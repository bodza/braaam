/*
 * Handling of regular expressions: vim_regcomp(), vim_regexec(), vim_regsub()
 *
 * Beware that some of this code is subtly aware of the way operator
 * precedence is structured in regular expressions.  Serious changes in
 * regular-expression syntax might require a total rethink.
 */

#include "vim.h"

/*
 * The "internal use only" fields in regexp.h are present to pass info from
 * compile to execute that permits the execute phase to run lots faster on
 * simple cases.  They are:
 *
 * regstart     char that must begin a match; NUL if none obvious; Can be a
 *              multi-byte character.
 * reganch      is the match anchored (at beginning-of-line only)?
 * regmust      string (pointer into program) that match must include, or NULL
 * regmlen      length of regmust string
 * regflags     RF_ values or'ed together
 *
 * Regstart and reganch permit very fast decisions on suitable starting points
 * for a match, cutting down the work a lot.  Regmust permits fast rejection
 * of lines that cannot possibly match.  The regmust tests are costly enough
 * that vim_regcomp() supplies a regmust only if the r.e. contains something
 * potentially expensive (at present, the only such thing detected is * or +
 * at the start of the r.e., which can involve a lot of backup).  Regmlen is
 * supplied because the test in vim_regexec() needs it and vim_regcomp() is
 * computing it anyway.
 */

/*
 * Structure for regexp "program".  This is essentially a linear encoding
 * of a nondeterministic finite-state machine (aka syntax charts or
 * "railroad normal form" in parsing technology).  Each node is an opcode
 * plus a "next" pointer, possibly plus an operand.  "Next" pointers of
 * all nodes except BRANCH and BRACES_COMPLEX implement concatenation; a "next"
 * pointer with a BRANCH on both ends of it is connecting two alternatives.
 * (Here we have one of the subtle syntax dependencies: an individual BRANCH
 * (as opposed to a collection of them) is never concatenated with anything
 * because of operator precedence).  The "next" pointer of a BRACES_COMPLEX
 * node points to the node after the stuff to be repeated.
 * The operand of some types of node is a literal string; for others, it is a
 * node leading into a sub-FSM.  In particular, the operand of a BRANCH node
 * is the first node of the branch.
 * (NB this is *not* a tree structure: the tail of the branch connects to the
 * thing following the set of BRANCHes.)
 *
 * pattern      is coded like:
 *
 *                        +-----------------+
 *                        |                 V
 * <aa>\|<bb>   BRANCH <aa> BRANCH <bb> --> END
 *                   |      ^    |          ^
 *                   +------+    +----------+
 *
 *                     +------------------+
 *                     V                  |
 * <aa>*        BRANCH BRANCH <aa> --> BACK BRANCH --> NOTHING --> END
 *                   |      |               ^                      ^
 *                   |      +---------------+                      |
 *                   +---------------------------------------------+
 *
 *                     +----------------------+
 *                     V                      |
 * <aa>\+       BRANCH <aa> --> BRANCH --> BACK  BRANCH --> NOTHING --> END
 *                   |               |           ^                      ^
 *                   |               +-----------+                      |
 *                   +--------------------------------------------------+
 *
 *                                      +-------------------------+
 *                                      V                         |
 * <aa>\{}      BRANCH BRACE_LIMITS --> BRACE_COMPLEX <aa> --> BACK  END
 *                   |                              |                ^
 *                   |                              +----------------+
 *                   +-----------------------------------------------+
 *
 * <aa>\@!<bb>  BRANCH NOMATCH <aa> --> END  <bb> --> END
 *                   |       |                ^       ^
 *                   |       +----------------+       |
 *                   +--------------------------------+
 *
 *                                                    +---------+
 *                                                    |         V
 * \z[abc]      BRANCH BRANCH  a  BRANCH  b  BRANCH  c  BRANCH  NOTHING --> END
 *                   |      |          |          |     ^                   ^
 *                   |      |          |          +-----+                   |
 *                   |      |          +----------------+                   |
 *                   |      +---------------------------+                   |
 *                   +------------------------------------------------------+
 *
 * They all start with a BRANCH for "\|" alternatives, even when there is only
 * one alternative.
 */

/*
 * The opcodes are:
 */

/* definition   number             opnd?    meaning */
#define END             0       /*      End of program or NOMATCH operand. */
#define BOL             1       /*      Match "" at beginning of line. */
#define EOL             2       /*      Match "" at end of line. */
#define BRANCH          3       /* node Match this alternative, or the
                                 *      next... */
#define BACK            4       /*      Match "", "next" ptr points backward. */
#define EXACTLY         5       /* str  Match this string. */
#define NOTHING         6       /*      Match empty string. */
#define STAR            7       /* node Match this (simple) thing 0 or more
                                 *      times. */
#define PLUS            8       /* node Match this (simple) thing 1 or more
                                 *      times. */
#define MATCH           9       /* node match the operand zero-width */
#define NOMATCH         10      /* node check for no match with operand */
#define BEHIND          11      /* node look behind for a match with operand */
#define NOBEHIND        12      /* node look behind for no match with operand */
#define SUBPAT          13      /* node match the operand here */
#define BRACE_SIMPLE    14      /* node Match this (simple) thing between m and
                                 *      n times (\{m,n\}). */
#define BOW             15      /*      Match "" after [^a-zA-Z0-9_] */
#define EOW             16      /*      Match "" at    [^a-zA-Z0-9_] */
#define BRACE_LIMITS    17      /* nr nr  define the min & max for BRACE_SIMPLE
                                 *      and BRACE_COMPLEX. */
#define NEWL            18      /*      Match line-break */
#define BHPOS           19      /*      End position for BEHIND or NOBEHIND */

/* character classes: 20-48 normal, 50-78 include a line-break */
#define ADD_NL          30
#define FIRST_NL        ANY + ADD_NL
#define ANY             20      /*      Match any one character. */
#define ANYOF           21      /* str  Match any character in this string. */
#define ANYBUT          22      /* str  Match any character not in this
                                 *      string. */
#define IDENT           23      /*      Match identifier char */
#define SIDENT          24      /*      Match identifier char but no digit */
#define KWORD           25      /*      Match keyword char */
#define SKWORD          26      /*      Match word char but no digit */
#define FNAME           27      /*      Match file name char */
#define SFNAME          28      /*      Match file name char but no digit */
#define PRINT           29      /*      Match printable char */
#define SPRINT          30      /*      Match printable char but no digit */
#define WHITE           31      /*      Match whitespace char */
#define NWHITE          32      /*      Match non-whitespace char */
#define DIGIT           33      /*      Match digit char */
#define NDIGIT          34      /*      Match non-digit char */
#define HEX             35      /*      Match hex char */
#define NHEX            36      /*      Match non-hex char */
#define OCTAL           37      /*      Match octal char */
#define NOCTAL          38      /*      Match non-octal char */
#define WORD            39      /*      Match word char */
#define NWORD           40      /*      Match non-word char */
#define HEAD            41      /*      Match head char */
#define NHEAD           42      /*      Match non-head char */
#define ALPHA           43      /*      Match alpha char */
#define NALPHA          44      /*      Match non-alpha char */
#define LOWER           45      /*      Match lowercase char */
#define NLOWER          46      /*      Match non-lowercase char */
#define UPPER           47      /*      Match uppercase char */
#define NUPPER          48      /*      Match non-uppercase char */
#define LAST_NL         NUPPER + ADD_NL
#define WITH_NL(op)     ((op) >= FIRST_NL && (op) <= LAST_NL)

#define MOPEN           80  /* -89       Mark this point in input as start of
                                 *       \( subexpr.  MOPEN + 0 marks start of
                                 *       match. */
#define MCLOSE          90  /* -99       Analogous to MOPEN.  MCLOSE + 0 marks
                                 *       end of match. */
#define BACKREF         100 /* -109 node Match same string again \1-\9 */

#define ZOPEN          110 /* -119      Mark this point in input as start of
                                 *       \z( subexpr. */
#define ZCLOSE         120 /* -129      Analogous to ZOPEN. */
#define ZREF           130 /* -139 node Match external submatch \z1-\z9 */

#define BRACE_COMPLEX   140 /* -149 node Match nodes between m & n times */

#define NOPEN           150     /*      Mark this point in input as start of
                                        \%( subexpr. */
#define NCLOSE          151     /*      Analogous to NOPEN. */

#define MULTIBYTECODE   200     /* mbc  Match one multi-byte character */
#define RE_BOF          201     /*      Match "" at beginning of file. */
#define RE_EOF          202     /*      Match "" at end of file. */
#define CURSOR          203     /*      Match location of cursor. */

#define RE_LNUM         204     /* nr cmp  Match line number */
#define RE_COL          205     /* nr cmp  Match column number */
#define RE_VCOL         206     /* nr cmp  Match virtual column number */

#define RE_MARK         207     /* mark cmp  Match mark position */
#define RE_VISUAL       208     /*      Match Visual area */
#define RE_COMPOSING    209     /* any composing characters */

/*
 * Magic characters have a special meaning, they don't match literally.
 * Magic characters are negative.  This separates them from literal characters
 * (possibly multi-byte).  Only ASCII characters can be Magic.
 */
#define Magic(x)        ((int)(x) - 256)
#define un_Magic(x)     ((x) + 256)
#define is_Magic(x)     ((x) < 0)

static int no_Magic(int x);
static int toggle_Magic(int x);

    static int
no_Magic(x)
    int         x;
{
    if (is_Magic(x))
        return un_Magic(x);

    return x;
}

    static int
toggle_Magic(x)
    int         x;
{
    if (is_Magic(x))
        return un_Magic(x);

    return Magic(x);
}

/*
 * The first byte of the regexp internal "program" is actually this magic
 * number; the start node begins in the second byte.  It's used to catch the
 * most severe mutilation of the program by the caller.
 */

#define REGMAGIC        0234

/*
 * Opcode notes:
 *
 * BRANCH       The set of branches constituting a single choice are hooked
 *              together with their "next" pointers, since precedence prevents
 *              anything being concatenated to any individual branch.  The
 *              "next" pointer of the last BRANCH in a choice points to the
 *              thing following the whole choice.  This is also where the
 *              final "next" pointer of each individual branch points; each
 *              branch starts with the operand node of a BRANCH node.
 *
 * BACK         Normal "next" pointers all implicitly point forward; BACK
 *              exists to make loop structures possible.
 *
 * STAR,PLUS    '=', and complex '*' and '+', are implemented as circular
 *              BRANCH structures using BACK.  Simple cases (one character
 *              per match) are implemented with STAR and PLUS for speed
 *              and to minimize recursive plunges.
 *
 * BRACE_LIMITS This is always followed by a BRACE_SIMPLE or BRACE_COMPLEX
 *              node, and defines the min and max limits to be used for that
 *              node.
 *
 * MOPEN,MCLOSE ...are numbered at compile time.
 * ZOPEN,ZCLOSE ...ditto
 */

/*
 * A node is one char of opcode followed by two chars of "next" pointer.
 * "Next" pointers are stored as two 8-bit bytes, high order first.  The
 * value is a positive offset from the opcode of the node containing it.
 * An operand, if any, simply follows the node.  (Note that much of the
 * code generation knows about this implicit relationship.)
 *
 * Using two bytes for the "next" pointer is vast overkill for most things,
 * but allows patterns to get big without disasters.
 */
#define OP(p)           ((int)*(p))
#define NEXT(p)         (((*((p) + 1) & 0377) << 8) + (*((p) + 2) & 0377))
#define OPERAND(p)      ((p) + 3)
/* Obtain an operand that was stored as four bytes, MSB first. */
#define OPERAND_MIN(p)  (((long)(p)[3] << 24) + ((long)(p)[4] << 16) \
                        + ((long)(p)[5] << 8) + (long)(p)[6])
/* Obtain a second operand stored as four bytes. */
#define OPERAND_MAX(p)  OPERAND_MIN((p) + 4)
/* Obtain a second single-byte operand stored after a four bytes operand. */
#define OPERAND_CMP(p)  (p)[7]

/* Used for an error (down from) vim_regcomp():
 * give the error message, set rc_did_emsg and return NULL */
#define EMSG_RET_NULL(m) return (EMSG(m), rc_did_emsg = TRUE, (void *)NULL)
#define EMSG_RET_FAIL(m) return (EMSG(m), rc_did_emsg = TRUE, FAIL)
#define EMSG2_RET_NULL(m, c) return (EMSG2((m), (c) ? "" : "\\"), rc_did_emsg = TRUE, (void *)NULL)
#define EMSG2_RET_FAIL(m, c) return (EMSG2((m), (c) ? "" : "\\"), rc_did_emsg = TRUE, FAIL)

#define MAX_LIMIT       (32767L << 16L)

static int re_multi_type(int);
static int cstrncmp(char_u *s1, char_u *s2, int *n);
static char_u *cstrchr(char_u *, int);

static int re_mult_next(char *what);

static char_u e_missingbracket[] = "E769: Missing ] after %s[";
static char_u e_unmatchedpp[] = "E53: Unmatched %s%%(";
static char_u e_unmatchedp[] = "E54: Unmatched %s(";
static char_u e_unmatchedpar[] = "E55: Unmatched %s)";
static char_u e_z_not_allowed[] = "E66: \\z( not allowed here";
static char_u e_z1_not_allowed[] = "E67: \\z1 et al. not allowed here";
static char_u e_missing_sb[] = "E69: Missing ] after %s%%[";
static char_u e_empty_sb[]  = "E70: Empty %s%%[]";
#define NOT_MULTI       0
#define MULTI_ONE       1
#define MULTI_MULT      2
/*
 * Return NOT_MULTI if c is not a "multi" operator.
 * Return MULTI_ONE if c is a single "multi" operator.
 * Return MULTI_MULT if c is a multi "multi" operator.
 */
    static int
re_multi_type(c)
    int c;
{
    if (c == Magic('@') || c == Magic('=') || c == Magic('?'))
        return MULTI_ONE;
    if (c == Magic('*') || c == Magic('+') || c == Magic('{'))
        return MULTI_MULT;

    return NOT_MULTI;
}

/*
 * Flags to be passed up and down.
 */
#define HASWIDTH        0x1     /* Known never to match null string. */
#define SIMPLE          0x2     /* Simple enough to be STAR/PLUS operand. */
#define SPSTART         0x4     /* Starts with * or +. */
#define HASNL           0x8     /* Contains some \n. */
#define HASLOOKBH       0x10    /* Contains "\@<=" or "\@<!". */
#define WORST           0       /* Worst case. */

/*
 * When regcode is set to this value, code is not emitted and size is computed instead.
 */
#define JUST_CALC_SIZE  ((char_u *) -1)

static char_u           *reg_prev_sub = NULL;

/*
 * REGEXP_INRANGE contains all characters which are always special in a [] range after '\'.
 * REGEXP_ABBR contains all characters which act as abbreviations after '\'.
 * These are:
 *  \n  - New line (NL).
 *  \r  - Carriage Return (CR).
 *  \t  - Tab (TAB).
 *  \e  - Escape (ESC).
 *  \b  - Backspace (Ctrl_H).
 *  \d  - Character code in decimal, eg \d123
 *  \o  - Character code in octal, eg \o80
 *  \x  - Character code in hex, eg \x4a
 *  \u  - Multibyte character code, eg \u20ac
 *  \U  - Long multibyte character code, eg \U12345678
 */
static char_u REGEXP_INRANGE[] = "]^-n\\";
static char_u REGEXP_ABBR[] = "nrtebdoxuU";

static int      backslash_trans(int c);
static int      get_char_class(char_u **pp);
static int      get_equi_class(char_u **pp);
static void     reg_equi_class(int c);
static int      get_coll_element(char_u **pp);
static char_u   *skip_anyof(char_u *p);
static void     init_class_tab(void);

/*
 * Translate '\x' to its control character, except "\n", which is Magic.
 */
    static int
backslash_trans(c)
    int         c;
{
    switch (c)
    {
        case 'r':   return CAR;
        case 't':   return TAB;
        case 'e':   return ESC;
        case 'b':   return BS;
    }
    return c;
}

/*
 * Check for a character class name "[:name:]".  "pp" points to the '['.
 * Returns one of the CLASS_ items. CLASS_NONE means that no item was
 * recognized.  Otherwise "pp" is advanced to after the item.
 */
    static int
get_char_class(pp)
    char_u      **pp;
{
    static const char *(class_names[]) =
    {
        "alnum:]",
#define CLASS_ALNUM 0
        "alpha:]",
#define CLASS_ALPHA 1
        "blank:]",
#define CLASS_BLANK 2
        "cntrl:]",
#define CLASS_CNTRL 3
        "digit:]",
#define CLASS_DIGIT 4
        "graph:]",
#define CLASS_GRAPH 5
        "lower:]",
#define CLASS_LOWER 6
        "print:]",
#define CLASS_PRINT 7
        "punct:]",
#define CLASS_PUNCT 8
        "space:]",
#define CLASS_SPACE 9
        "upper:]",
#define CLASS_UPPER 10
        "xdigit:]",
#define CLASS_XDIGIT 11
        "tab:]",
#define CLASS_TAB 12
        "return:]",
#define CLASS_RETURN 13
        "backspace:]",
#define CLASS_BACKSPACE 14
        "escape:]",
#define CLASS_ESCAPE 15
    };
#define CLASS_NONE 99
    int i;

    if ((*pp)[1] == ':')
    {
        for (i = 0; i < (int)(sizeof(class_names) / sizeof(*class_names)); ++i)
            if (STRNCMP(*pp + 2, class_names[i], STRLEN(class_names[i])) == 0)
            {
                *pp += STRLEN(class_names[i]) + 2;
                return i;
            }
    }
    return CLASS_NONE;
}

/*
 * Specific version of character class functions.
 * Using a table to keep this fast.
 */
static short    class_tab[256];

#define RI_DIGIT    0x01
#define RI_HEX      0x02
#define RI_OCTAL    0x04
#define RI_WORD     0x08
#define RI_HEAD     0x10
#define RI_ALPHA    0x20
#define RI_LOWER    0x40
#define RI_UPPER    0x80
#define RI_WHITE    0x100

    static void
init_class_tab()
{
    int         i;
    static int  done = FALSE;

    if (done)
        return;

    for (i = 0; i < 256; ++i)
    {
        if (i >= '0' && i <= '7')
            class_tab[i] = RI_DIGIT + RI_HEX + RI_OCTAL + RI_WORD;
        else if (i >= '8' && i <= '9')
            class_tab[i] = RI_DIGIT + RI_HEX + RI_WORD;
        else if (i >= 'a' && i <= 'f')
            class_tab[i] = RI_HEX + RI_WORD + RI_HEAD + RI_ALPHA + RI_LOWER;
        else if (i >= 'g' && i <= 'z')
            class_tab[i] = RI_WORD + RI_HEAD + RI_ALPHA + RI_LOWER;
        else if (i >= 'A' && i <= 'F')
            class_tab[i] = RI_HEX + RI_WORD + RI_HEAD + RI_ALPHA + RI_UPPER;
        else if (i >= 'G' && i <= 'Z')
            class_tab[i] = RI_WORD + RI_HEAD + RI_ALPHA + RI_UPPER;
        else if (i == '_')
            class_tab[i] = RI_WORD + RI_HEAD;
        else
            class_tab[i] = 0;
    }
    class_tab[' '] |= RI_WHITE;
    class_tab['\t'] |= RI_WHITE;
    done = TRUE;
}

#define ri_digit(c)    (c < 0x100 && (class_tab[c] & RI_DIGIT))
#define ri_hex(c)      (c < 0x100 && (class_tab[c] & RI_HEX))
#define ri_octal(c)    (c < 0x100 && (class_tab[c] & RI_OCTAL))
#define ri_word(c)     (c < 0x100 && (class_tab[c] & RI_WORD))
#define ri_head(c)     (c < 0x100 && (class_tab[c] & RI_HEAD))
#define ri_alpha(c)    (c < 0x100 && (class_tab[c] & RI_ALPHA))
#define ri_lower(c)    (c < 0x100 && (class_tab[c] & RI_LOWER))
#define ri_upper(c)    (c < 0x100 && (class_tab[c] & RI_UPPER))
#define ri_white(c)    (c < 0x100 && (class_tab[c] & RI_WHITE))

/* flags for regflags */
#define RF_ICASE    1   /* ignore case */
#define RF_NOICASE  2   /* don't ignore case */
#define RF_HASNL    4   /* can match a NL */
#define RF_ICOMBINE 8   /* ignore combining characters */
#define RF_LOOKBH   16  /* uses "\@<=" or "\@<!" */

/*
 * Global work variables for vim_regcomp().
 */

static char_u   *regparse;      /* Input-scan pointer. */
static int      prevchr_len;    /* byte length of previous char */
static int      num_complex_braces; /* Complex \{...} count */
static int      regnpar;        /* () count. */
static int      regnzpar;       /* \z() count. */
static int      re_has_z;       /* \z item detected */
static char_u   *regcode;       /* Code-emit pointer, or JUST_CALC_SIZE */
static long     regsize;        /* Code size. */
static int      reg_toolong;    /* TRUE when offset out of range */
static char_u   had_endbrace[NSUBEXP];  /* flags, TRUE if end of () found */
static unsigned regflags;       /* RF_ flags for prog */
static long     brace_min[10];  /* Minimums for complex brace repeats */
static long     brace_max[10];  /* Maximums for complex brace repeats */
static int      brace_count[10]; /* Current counts for complex brace repeats */
static int      had_eol;        /* TRUE when EOL found by vim_regcomp() */
static int      one_exactly = FALSE;    /* only do one char for EXACTLY */

static int      reg_magic;      /* magicness of the pattern: */
#define MAGIC_NONE      1       /* "\V" very unmagic */
#define MAGIC_OFF       2       /* "\M" or 'magic' off */
#define MAGIC_ON        3       /* "\m" or 'magic' */
#define MAGIC_ALL       4       /* "\v" very magic */

static int      reg_string;     /* matching with a string instead of a buffer
                                   line */
static int      reg_strict;     /* "[abc" is illegal */

/*
 * META contains all characters that may be magic, except '^' and '$'.
 */

/* META[] is used often enough to justify turning it into a table. */
static char_u META_flags[] =
{
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
/*                 %  &     (  )  *  +        .    */
    0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0,
/*     1  2  3  4  5  6  7  8  9        <  =  >  ? */
    0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1,
/*  @  A     C  D     F     H  I     K  L  M     O */
    1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 1,
/*  P        S     U  V  W  X     Z  [           _ */
    1, 0, 0, 1, 0, 1, 1, 1, 1, 0, 1, 1, 0, 0, 0, 1,
/*     a     c  d     f     h  i     k  l  m  n  o */
    0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 1, 1,
/*  p        s     u  v  w  x     z  {  |     ~    */
    1, 0, 0, 1, 0, 1, 1, 1, 1, 0, 1, 1, 1, 0, 1
};

static int      curchr;         /* currently parsed character */
/* Previous character.  Note: prevchr is sometimes -1 when we are not at the
 * start, eg in /[ ^I]^ the pattern was never found even if it existed,
 * because ^ was taken to be magic -- webb */
static int      prevchr;
static int      prevprevchr;    /* previous-previous character */
static int      nextchr;        /* used for ungetchr() */

/* arguments for reg() */
#define REG_NOPAREN     0       /* toplevel reg() */
#define REG_PAREN       1       /* \(\) */
#define REG_ZPAREN      2       /* \z(\) */
#define REG_NPAREN      3       /* \%(\) */

typedef struct
{
     char_u     *regparse;
     int        prevchr_len;
     int        curchr;
     int        prevchr;
     int        prevprevchr;
     int        nextchr;
     int        at_start;
     int        prev_at_start;
     int        regnpar;
} parse_state_T;

/*
 * Forward declarations for vim_regcomp()'s friends.
 */
static void     initchr(char_u *);
static void     save_parse_state(parse_state_T *ps);
static void     restore_parse_state(parse_state_T *ps);
static int      getchr(void);
static void     skipchr_keepstart(void);
static int      peekchr(void);
static void     skipchr(void);
static void     ungetchr(void);
static int      gethexchrs(int maxinputlen);
static int      getoctchrs(void);
static int      getdecchrs(void);
static int      coll_get_char(void);
static void     regcomp_start(char_u *expr, int flags);
static char_u   *reg(int, int *);
static char_u   *regbranch(int *flagp);
static char_u   *regconcat(int *flagp);
static char_u   *regpiece(int *);
static char_u   *regatom(int *);
static char_u   *regnode(int);
static int      use_multibytecode(int c);
static int      prog_magic_wrong(void);
static char_u   *regnext(char_u *);
static void     regc(int b);
static void     regmbc(int c);
#define REGMBC(x) regmbc(x);
#define CASEMBC(x) case x:
static void     reginsert(int, char_u *);
static void     reginsert_nr(int op, long val, char_u *opnd);
static void     reginsert_limits(int, long, long, char_u *);
static char_u   *re_put_long(char_u *pr, long_u val);
static int      read_limits(long *, long *);
static void     regtail(char_u *, char_u *);
static void     regoptail(char_u *, char_u *);

static regengine_T bt_regengine;
static regengine_T nfa_regengine;

/*
 * Return TRUE if compiled regular expression "prog" can match a line break.
 */
    int
re_multiline(prog)
    regprog_T *prog;
{
    return (prog->regflags & RF_HASNL);
}

/*
 * Return TRUE if compiled regular expression "prog" looks before the start
 * position (pattern contains "\@<=" or "\@<!").
 */
    int
re_lookbehind(prog)
    regprog_T *prog;
{
    return (prog->regflags & RF_LOOKBH);
}

/*
 * Check for an equivalence class name "[=a=]".  "pp" points to the '['.
 * Returns a character representing the class. Zero means that no item was
 * recognized.  Otherwise "pp" is advanced to after the item.
 */
    static int
get_equi_class(pp)
    char_u      **pp;
{
    int         c;
    int         l = 1;
    char_u      *p = *pp;

    if (p[1] == '=')
    {
        l = utfc_ptr2len(p + 2);
        if (p[l + 2] == '=' && p[l + 3] == ']')
        {
            c = utf_ptr2char(p + 2);
            *pp += l + 4;
            return c;
        }
    }
    return 0;
}

/*
 * Produce the bytes for equivalence class "c".
 * Currently only handles latin1, latin9 and utf-8.
 *
 * NOTE! When changing this function, also change nfa_emit_equi_class()
 */
    static void
reg_equi_class(c)
    int     c;
{
    switch (c)
    {
        case 'A': case '\300': case '\301': case '\302':
        CASEMBC(0x100) CASEMBC(0x102) CASEMBC(0x104) CASEMBC(0x1cd)
        CASEMBC(0x1de) CASEMBC(0x1e0) CASEMBC(0x1ea2)
        case '\303': case '\304': case '\305':
                    regmbc('A'); regmbc('\300'); regmbc('\301');
                    regmbc('\302'); regmbc('\303'); regmbc('\304');
                    regmbc('\305');
                    REGMBC(0x100) REGMBC(0x102) REGMBC(0x104)
                    REGMBC(0x1cd) REGMBC(0x1de) REGMBC(0x1e0)
                    REGMBC(0x1ea2)
                    return;
        case 'B': CASEMBC(0x1e02) CASEMBC(0x1e06)
                    regmbc('B'); REGMBC(0x1e02) REGMBC(0x1e06)
                    return;
        case 'C': case '\307':
        CASEMBC(0x106) CASEMBC(0x108) CASEMBC(0x10a) CASEMBC(0x10c)
                    regmbc('C'); regmbc('\307');
                    REGMBC(0x106) REGMBC(0x108) REGMBC(0x10a)
                    REGMBC(0x10c)
                    return;
        case 'D': CASEMBC(0x10e) CASEMBC(0x110) CASEMBC(0x1e0a)
        CASEMBC(0x1e0e) CASEMBC(0x1e10)
                    regmbc('D'); REGMBC(0x10e) REGMBC(0x110)
                    REGMBC(0x1e0a) REGMBC(0x1e0e) REGMBC(0x1e10)
                    return;
        case 'E': case '\310': case '\311': case '\312': case '\313':
        CASEMBC(0x112) CASEMBC(0x114) CASEMBC(0x116) CASEMBC(0x118)
        CASEMBC(0x11a) CASEMBC(0x1eba) CASEMBC(0x1ebc)
                    regmbc('E'); regmbc('\310'); regmbc('\311');
                    regmbc('\312'); regmbc('\313');
                    REGMBC(0x112) REGMBC(0x114) REGMBC(0x116)
                    REGMBC(0x118) REGMBC(0x11a) REGMBC(0x1eba)
                    REGMBC(0x1ebc)
                    return;
        case 'F': CASEMBC(0x1e1e)
                    regmbc('F'); REGMBC(0x1e1e)
                    return;
        case 'G': CASEMBC(0x11c) CASEMBC(0x11e) CASEMBC(0x120)
        CASEMBC(0x122) CASEMBC(0x1e4) CASEMBC(0x1e6) CASEMBC(0x1f4)
        CASEMBC(0x1e20)
                    regmbc('G'); REGMBC(0x11c) REGMBC(0x11e)
                    REGMBC(0x120) REGMBC(0x122) REGMBC(0x1e4)
                    REGMBC(0x1e6) REGMBC(0x1f4) REGMBC(0x1e20)
                    return;
        case 'H': CASEMBC(0x124) CASEMBC(0x126) CASEMBC(0x1e22)
        CASEMBC(0x1e26) CASEMBC(0x1e28)
                    regmbc('H'); REGMBC(0x124) REGMBC(0x126)
                    REGMBC(0x1e22) REGMBC(0x1e26) REGMBC(0x1e28)
                    return;
        case 'I': case '\314': case '\315': case '\316': case '\317':
        CASEMBC(0x128) CASEMBC(0x12a) CASEMBC(0x12c) CASEMBC(0x12e)
        CASEMBC(0x130) CASEMBC(0x1cf) CASEMBC(0x1ec8)
                    regmbc('I'); regmbc('\314'); regmbc('\315');
                    regmbc('\316'); regmbc('\317');
                    REGMBC(0x128) REGMBC(0x12a) REGMBC(0x12c)
                    REGMBC(0x12e) REGMBC(0x130) REGMBC(0x1cf)
                    REGMBC(0x1ec8)
                    return;
        case 'J': CASEMBC(0x134)
                    regmbc('J'); REGMBC(0x134)
                    return;
        case 'K': CASEMBC(0x136) CASEMBC(0x1e8) CASEMBC(0x1e30)
        CASEMBC(0x1e34)
                    regmbc('K'); REGMBC(0x136) REGMBC(0x1e8)
                    REGMBC(0x1e30) REGMBC(0x1e34)
                    return;
        case 'L': CASEMBC(0x139) CASEMBC(0x13b) CASEMBC(0x13d)
        CASEMBC(0x13f) CASEMBC(0x141) CASEMBC(0x1e3a)
                    regmbc('L'); REGMBC(0x139) REGMBC(0x13b)
                    REGMBC(0x13d) REGMBC(0x13f) REGMBC(0x141)
                    REGMBC(0x1e3a)
                    return;
        case 'M': CASEMBC(0x1e3e) CASEMBC(0x1e40)
                    regmbc('M'); REGMBC(0x1e3e) REGMBC(0x1e40)
                    return;
        case 'N': case '\321':
        CASEMBC(0x143) CASEMBC(0x145) CASEMBC(0x147) CASEMBC(0x1e44)
        CASEMBC(0x1e48)
                    regmbc('N'); regmbc('\321');
                    REGMBC(0x143) REGMBC(0x145) REGMBC(0x147)
                    REGMBC(0x1e44) REGMBC(0x1e48)
                    return;
        case 'O': case '\322': case '\323': case '\324': case '\325':
        case '\326': case '\330':
        CASEMBC(0x14c) CASEMBC(0x14e) CASEMBC(0x150) CASEMBC(0x1a0)
        CASEMBC(0x1d1) CASEMBC(0x1ea) CASEMBC(0x1ec) CASEMBC(0x1ece)
                    regmbc('O'); regmbc('\322'); regmbc('\323');
                    regmbc('\324'); regmbc('\325'); regmbc('\326');
                    regmbc('\330');
                    REGMBC(0x14c) REGMBC(0x14e) REGMBC(0x150)
                    REGMBC(0x1a0) REGMBC(0x1d1) REGMBC(0x1ea)
                    REGMBC(0x1ec) REGMBC(0x1ece)
                    return;
        case 'P': case 0x1e54: case 0x1e56:
                    regmbc('P'); REGMBC(0x1e54) REGMBC(0x1e56)
                    return;
        case 'R': CASEMBC(0x154) CASEMBC(0x156) CASEMBC(0x158)
        CASEMBC(0x1e58) CASEMBC(0x1e5e)
                    regmbc('R'); REGMBC(0x154) REGMBC(0x156) REGMBC(0x158)
                    REGMBC(0x1e58) REGMBC(0x1e5e)
                    return;
        case 'S': CASEMBC(0x15a) CASEMBC(0x15c) CASEMBC(0x15e)
        CASEMBC(0x160) CASEMBC(0x1e60)
                    regmbc('S'); REGMBC(0x15a) REGMBC(0x15c)
                    REGMBC(0x15e) REGMBC(0x160) REGMBC(0x1e60)
                    return;
        case 'T': CASEMBC(0x162) CASEMBC(0x164) CASEMBC(0x166)
        CASEMBC(0x1e6a) CASEMBC(0x1e6e)
                    regmbc('T'); REGMBC(0x162) REGMBC(0x164)
                    REGMBC(0x166) REGMBC(0x1e6a) REGMBC(0x1e6e)
                    return;
        case 'U': case '\331': case '\332': case '\333': case '\334':
        CASEMBC(0x168) CASEMBC(0x16a) CASEMBC(0x16c) CASEMBC(0x16e)
        CASEMBC(0x170) CASEMBC(0x172) CASEMBC(0x1af) CASEMBC(0x1d3)
        CASEMBC(0x1ee6)
                    regmbc('U'); regmbc('\331'); regmbc('\332');
                    regmbc('\333'); regmbc('\334');
                    REGMBC(0x168) REGMBC(0x16a) REGMBC(0x16c)
                    REGMBC(0x16e) REGMBC(0x170) REGMBC(0x172)
                    REGMBC(0x1af) REGMBC(0x1d3) REGMBC(0x1ee6)
                    return;
        case 'V': CASEMBC(0x1e7c)
                    regmbc('V'); REGMBC(0x1e7c)
                    return;
        case 'W': CASEMBC(0x174) CASEMBC(0x1e80) CASEMBC(0x1e82)
        CASEMBC(0x1e84) CASEMBC(0x1e86)
                    regmbc('W'); REGMBC(0x174) REGMBC(0x1e80)
                    REGMBC(0x1e82) REGMBC(0x1e84) REGMBC(0x1e86)
                    return;
        case 'X': CASEMBC(0x1e8a) CASEMBC(0x1e8c)
                    regmbc('X'); REGMBC(0x1e8a) REGMBC(0x1e8c)
                    return;
        case 'Y': case '\335':
        CASEMBC(0x176) CASEMBC(0x178) CASEMBC(0x1e8e) CASEMBC(0x1ef2)
        CASEMBC(0x1ef6) CASEMBC(0x1ef8)
                    regmbc('Y'); regmbc('\335');
                    REGMBC(0x176) REGMBC(0x178) REGMBC(0x1e8e)
                    REGMBC(0x1ef2) REGMBC(0x1ef6) REGMBC(0x1ef8)
                    return;
        case 'Z': CASEMBC(0x179) CASEMBC(0x17b) CASEMBC(0x17d)
        CASEMBC(0x1b5) CASEMBC(0x1e90) CASEMBC(0x1e94)
                    regmbc('Z'); REGMBC(0x179) REGMBC(0x17b)
                    REGMBC(0x17d) REGMBC(0x1b5) REGMBC(0x1e90)
                    REGMBC(0x1e94)
                    return;
        case 'a': case '\340': case '\341': case '\342':
        case '\343': case '\344': case '\345':
        CASEMBC(0x101) CASEMBC(0x103) CASEMBC(0x105) CASEMBC(0x1ce)
        CASEMBC(0x1df) CASEMBC(0x1e1) CASEMBC(0x1ea3)
                    regmbc('a'); regmbc('\340'); regmbc('\341');
                    regmbc('\342'); regmbc('\343'); regmbc('\344');
                    regmbc('\345');
                    REGMBC(0x101) REGMBC(0x103) REGMBC(0x105)
                    REGMBC(0x1ce) REGMBC(0x1df) REGMBC(0x1e1)
                    REGMBC(0x1ea3)
                    return;
        case 'b': CASEMBC(0x1e03) CASEMBC(0x1e07)
                    regmbc('b'); REGMBC(0x1e03) REGMBC(0x1e07)
                    return;
        case 'c': case '\347':
        CASEMBC(0x107) CASEMBC(0x109) CASEMBC(0x10b) CASEMBC(0x10d)
                    regmbc('c'); regmbc('\347');
                    REGMBC(0x107) REGMBC(0x109) REGMBC(0x10b)
                    REGMBC(0x10d)
                    return;
        case 'd': CASEMBC(0x10f) CASEMBC(0x111) CASEMBC(0x1d0b)
        CASEMBC(0x1e11)
                    regmbc('d'); REGMBC(0x10f) REGMBC(0x111)
                    REGMBC(0x1e0b) REGMBC(0x01e0f) REGMBC(0x1e11)
                    return;
        case 'e': case '\350': case '\351': case '\352': case '\353':
        CASEMBC(0x113) CASEMBC(0x115) CASEMBC(0x117) CASEMBC(0x119)
        CASEMBC(0x11b) CASEMBC(0x1ebb) CASEMBC(0x1ebd)
                    regmbc('e'); regmbc('\350'); regmbc('\351');
                    regmbc('\352'); regmbc('\353');
                    REGMBC(0x113) REGMBC(0x115) REGMBC(0x117)
                    REGMBC(0x119) REGMBC(0x11b) REGMBC(0x1ebb)
                    REGMBC(0x1ebd)
                    return;
        case 'f': CASEMBC(0x1e1f)
                    regmbc('f'); REGMBC(0x1e1f)
                    return;
        case 'g': CASEMBC(0x11d) CASEMBC(0x11f) CASEMBC(0x121)
        CASEMBC(0x123) CASEMBC(0x1e5) CASEMBC(0x1e7) CASEMBC(0x1f5)
        CASEMBC(0x1e21)
                    regmbc('g'); REGMBC(0x11d) REGMBC(0x11f)
                    REGMBC(0x121) REGMBC(0x123) REGMBC(0x1e5)
                    REGMBC(0x1e7) REGMBC(0x1f5) REGMBC(0x1e21)
                    return;
        case 'h': CASEMBC(0x125) CASEMBC(0x127) CASEMBC(0x1e23)
        CASEMBC(0x1e27) CASEMBC(0x1e29) CASEMBC(0x1e96)
                    regmbc('h'); REGMBC(0x125) REGMBC(0x127)
                    REGMBC(0x1e23) REGMBC(0x1e27) REGMBC(0x1e29)
                    REGMBC(0x1e96)
                    return;
        case 'i': case '\354': case '\355': case '\356': case '\357':
        CASEMBC(0x129) CASEMBC(0x12b) CASEMBC(0x12d) CASEMBC(0x12f)
        CASEMBC(0x1d0) CASEMBC(0x1ec9)
                    regmbc('i'); regmbc('\354'); regmbc('\355');
                    regmbc('\356'); regmbc('\357');
                    REGMBC(0x129) REGMBC(0x12b) REGMBC(0x12d)
                    REGMBC(0x12f) REGMBC(0x1d0) REGMBC(0x1ec9)
                    return;
        case 'j': CASEMBC(0x135) CASEMBC(0x1f0)
                    regmbc('j'); REGMBC(0x135) REGMBC(0x1f0)
                    return;
        case 'k': CASEMBC(0x137) CASEMBC(0x1e9) CASEMBC(0x1e31)
        CASEMBC(0x1e35)
                    regmbc('k'); REGMBC(0x137) REGMBC(0x1e9)
                    REGMBC(0x1e31) REGMBC(0x1e35)
                    return;
        case 'l': CASEMBC(0x13a) CASEMBC(0x13c) CASEMBC(0x13e)
        CASEMBC(0x140) CASEMBC(0x142) CASEMBC(0x1e3b)
                    regmbc('l'); REGMBC(0x13a) REGMBC(0x13c)
                    REGMBC(0x13e) REGMBC(0x140) REGMBC(0x142)
                    REGMBC(0x1e3b)
                    return;
        case 'm': CASEMBC(0x1e3f) CASEMBC(0x1e41)
                    regmbc('m'); REGMBC(0x1e3f) REGMBC(0x1e41)
                    return;
        case 'n': case '\361':
        CASEMBC(0x144) CASEMBC(0x146) CASEMBC(0x148) CASEMBC(0x149)
        CASEMBC(0x1e45) CASEMBC(0x1e49)
                    regmbc('n'); regmbc('\361');
                    REGMBC(0x144) REGMBC(0x146) REGMBC(0x148)
                    REGMBC(0x149) REGMBC(0x1e45) REGMBC(0x1e49)
                    return;
        case 'o': case '\362': case '\363': case '\364': case '\365':
        case '\366': case '\370':
        CASEMBC(0x14d) CASEMBC(0x14f) CASEMBC(0x151) CASEMBC(0x1a1)
        CASEMBC(0x1d2) CASEMBC(0x1eb) CASEMBC(0x1ed) CASEMBC(0x1ecf)
                    regmbc('o'); regmbc('\362'); regmbc('\363');
                    regmbc('\364'); regmbc('\365'); regmbc('\366');
                    regmbc('\370');
                    REGMBC(0x14d) REGMBC(0x14f) REGMBC(0x151)
                    REGMBC(0x1a1) REGMBC(0x1d2) REGMBC(0x1eb)
                    REGMBC(0x1ed) REGMBC(0x1ecf)
                    return;
        case 'p': CASEMBC(0x1e55) CASEMBC(0x1e57)
                    regmbc('p'); REGMBC(0x1e55) REGMBC(0x1e57)
                    return;
        case 'r': CASEMBC(0x155) CASEMBC(0x157) CASEMBC(0x159)
        CASEMBC(0x1e59) CASEMBC(0x1e5f)
                    regmbc('r'); REGMBC(0x155) REGMBC(0x157) REGMBC(0x159)
                    REGMBC(0x1e59) REGMBC(0x1e5f)
                    return;
        case 's': CASEMBC(0x15b) CASEMBC(0x15d) CASEMBC(0x15f)
        CASEMBC(0x161) CASEMBC(0x1e61)
                    regmbc('s'); REGMBC(0x15b) REGMBC(0x15d)
                    REGMBC(0x15f) REGMBC(0x161) REGMBC(0x1e61)
                    return;
        case 't': CASEMBC(0x163) CASEMBC(0x165) CASEMBC(0x167)
        CASEMBC(0x1e6b) CASEMBC(0x1e6f) CASEMBC(0x1e97)
                    regmbc('t'); REGMBC(0x163) REGMBC(0x165) REGMBC(0x167)
                    REGMBC(0x1e6b) REGMBC(0x1e6f) REGMBC(0x1e97)
                    return;
        case 'u': case '\371': case '\372': case '\373': case '\374':
        CASEMBC(0x169) CASEMBC(0x16b) CASEMBC(0x16d) CASEMBC(0x16f)
        CASEMBC(0x171) CASEMBC(0x173) CASEMBC(0x1b0) CASEMBC(0x1d4)
        CASEMBC(0x1ee7)
                    regmbc('u'); regmbc('\371'); regmbc('\372');
                    regmbc('\373'); regmbc('\374');
                    REGMBC(0x169) REGMBC(0x16b) REGMBC(0x16d)
                    REGMBC(0x16f) REGMBC(0x171) REGMBC(0x173)
                    REGMBC(0x1b0) REGMBC(0x1d4) REGMBC(0x1ee7)
                    return;
        case 'v': CASEMBC(0x1e7d)
                    regmbc('v'); REGMBC(0x1e7d)
                    return;
        case 'w': CASEMBC(0x175) CASEMBC(0x1e81) CASEMBC(0x1e83)
        CASEMBC(0x1e85) CASEMBC(0x1e87) CASEMBC(0x1e98)
                    regmbc('w'); REGMBC(0x175) REGMBC(0x1e81)
                    REGMBC(0x1e83) REGMBC(0x1e85) REGMBC(0x1e87)
                    REGMBC(0x1e98)
                    return;
        case 'x': CASEMBC(0x1e8b) CASEMBC(0x1e8d)
                    regmbc('x'); REGMBC(0x1e8b) REGMBC(0x1e8d)
                    return;
        case 'y': case '\375': case '\377':
        CASEMBC(0x177) CASEMBC(0x1e8f) CASEMBC(0x1e99)
        CASEMBC(0x1ef3) CASEMBC(0x1ef7) CASEMBC(0x1ef9)
                    regmbc('y'); regmbc('\375'); regmbc('\377');
                    REGMBC(0x177) REGMBC(0x1e8f) REGMBC(0x1e99)
                    REGMBC(0x1ef3) REGMBC(0x1ef7) REGMBC(0x1ef9)
                    return;
        case 'z': CASEMBC(0x17a) CASEMBC(0x17c) CASEMBC(0x17e)
        CASEMBC(0x1b6) CASEMBC(0x1e91) CASEMBC(0x1e95)
                    regmbc('z'); REGMBC(0x17a) REGMBC(0x17c)
                    REGMBC(0x17e) REGMBC(0x1b6) REGMBC(0x1e91)
                    REGMBC(0x1e95)
                    return;
    }

    regmbc(c);
}

/*
 * Check for a collating element "[.a.]".  "pp" points to the '['.
 * Returns a character. Zero means that no item was recognized.  Otherwise
 * "pp" is advanced to after the item.
 * Currently only single characters are recognized!
 */
    static int
get_coll_element(pp)
    char_u      **pp;
{
    int         c;
    int         l = 1;
    char_u      *p = *pp;

    if (p[1] == '.')
    {
        l = utfc_ptr2len(p + 2);
        if (p[l + 2] == '.' && p[l + 3] == ']')
        {
            c = utf_ptr2char(p + 2);
            *pp += l + 4;
            return c;
        }
    }
    return 0;
}

static void get_cpo_flags(void);
static int reg_cpo_lit; /* 'cpoptions' contains 'l' flag */
static int reg_cpo_bsl; /* 'cpoptions' contains '\' flag */

    static void
get_cpo_flags()
{
    reg_cpo_lit = vim_strchr(p_cpo, CPO_LITERAL) != NULL;
    reg_cpo_bsl = vim_strchr(p_cpo, CPO_BACKSL) != NULL;
}

/*
 * Skip over a "[]" range.
 * "p" must point to the character after the '['.
 * The returned pointer is on the matching ']', or the terminating NUL.
 */
    static char_u *
skip_anyof(p)
    char_u      *p;
{
    int         l;

    if (*p == '^')      /* Complement of range. */
        ++p;
    if (*p == ']' || *p == '-')
        ++p;
    while (*p != NUL && *p != ']')
    {
        if ((l = utfc_ptr2len(p)) > 1)
            p += l;
        else if (*p == '-')
        {
            ++p;
            if (*p != ']' && *p != NUL)
                p += utfc_ptr2len(p);
        }
        else if (*p == '\\'
                && !reg_cpo_bsl
                && (vim_strchr(REGEXP_INRANGE, p[1]) != NULL
                    || (!reg_cpo_lit && vim_strchr(REGEXP_ABBR, p[1]) != NULL)))
            p += 2;
        else if (*p == '[')
        {
            if (get_char_class(&p) == CLASS_NONE
                    && get_equi_class(&p) == 0
                    && get_coll_element(&p) == 0)
                ++p; /* It was not a class name */
        }
        else
            ++p;
    }

    return p;
}

/*
 * Skip past regular expression.
 * Stop at end of "startp" or where "dirc" is found ('/', '?', etc).
 * Take care of characters with a backslash in front of it.
 * Skip strings inside [ and ].
 * When "newp" is not NULL and "dirc" is '?', make an allocated copy of the
 * expression and change "\?" to "?".  If "*newp" is not NULL the expression
 * is changed in-place.
 */
    char_u *
skip_regexp(startp, dirc, magic, newp)
    char_u      *startp;
    int         dirc;
    int         magic;
    char_u      **newp;
{
    int         mymagic;
    char_u      *p = startp;

    if (magic)
        mymagic = MAGIC_ON;
    else
        mymagic = MAGIC_OFF;
    get_cpo_flags();

    for (; p[0] != NUL; p += utfc_ptr2len(p))
    {
        if (p[0] == dirc)       /* found end of regexp */
            break;
        if ((p[0] == '[' && mymagic >= MAGIC_ON) || (p[0] == '\\' && p[1] == '[' && mymagic <= MAGIC_OFF))
        {
            p = skip_anyof(p + 1);
            if (p[0] == NUL)
                break;
        }
        else if (p[0] == '\\' && p[1] != NUL)
        {
            if (dirc == '?' && newp != NULL && p[1] == '?')
            {
                /* change "\?" to "?", make a copy first. */
                if (*newp == NULL)
                {
                    *newp = vim_strsave(startp);
                    if (*newp != NULL)
                        p = *newp + (p - startp);
                }
                if (*newp != NULL)
                    STRMOVE(p, p + 1);
                else
                    ++p;
            }
            else
                ++p;    /* skip next character */
            if (*p == 'v')
                mymagic = MAGIC_ALL;
            else if (*p == 'V')
                mymagic = MAGIC_NONE;
        }
    }
    return p;
}

static regprog_T  *bt_regcomp(char_u *expr, int re_flags);
static void bt_regfree(regprog_T *prog);

/*
 * bt_regcomp() - compile a regular expression into internal code for the
 * traditional back track matcher.
 * Returns the program in allocated space.  Returns NULL for an error.
 *
 * We can't allocate space until we know how big the compiled form will be,
 * but we can't compile it (and thus know how big it is) until we've got a
 * place to put the code.  So we cheat:  we compile it twice, once with code
 * generation turned off and size counting turned on, and once "for real".
 * This also means that we don't allocate space until we are sure that the
 * thing really will compile successfully, and we never have to move the
 * code and thus invalidate pointers into it.  (Note that it has to be in
 * one piece because vim_free() must be able to free it all.)
 *
 * Whether upper/lower case is to be ignored is decided when executing the
 * program, it does not matter here.
 *
 * Beware that the optimization-preparation code in here knows about some
 * of the structure of the compiled regexp.
 * "re_flags": RE_MAGIC and/or RE_STRING.
 */
    static regprog_T *
bt_regcomp(expr, re_flags)
    char_u      *expr;
    int         re_flags;
{
    bt_regprog_T    *r;
    char_u      *scan;
    char_u      *longest;
    int         len;
    int         flags;

    if (expr == NULL)
        EMSG_RET_NULL((char *)e_null);

    init_class_tab();

    /*
     * First pass: determine size, legality.
     */
    regcomp_start(expr, re_flags);
    regcode = JUST_CALC_SIZE;
    regc(REGMAGIC);
    if (reg(REG_NOPAREN, &flags) == NULL)
        return NULL;

    /* Small enough for pointer-storage convention? */
#if defined(SMALL_MALLOC) /* 16 bit storage allocation */
    if (regsize >= 65536L - 256L)
        EMSG_RET_NULL("E339: Pattern too long");
#endif

    /* Allocate space. */
    r = (bt_regprog_T *)lalloc(sizeof(bt_regprog_T) + regsize, TRUE);
    if (r == NULL)
        return NULL;

    /*
     * Second pass: emit code.
     */
    regcomp_start(expr, re_flags);
    regcode = r->program;
    regc(REGMAGIC);
    if (reg(REG_NOPAREN, &flags) == NULL || reg_toolong)
    {
        vim_free(r);
        if (reg_toolong)
            EMSG_RET_NULL("E339: Pattern too long");
        return NULL;
    }

    /* Dig out information for optimizations. */
    r->regstart = NUL;          /* Worst-case defaults. */
    r->reganch = 0;
    r->regmust = NULL;
    r->regmlen = 0;
    r->regflags = regflags;
    if (flags & HASNL)
        r->regflags |= RF_HASNL;
    if (flags & HASLOOKBH)
        r->regflags |= RF_LOOKBH;
    /* Remember whether this pattern has any \z specials in it. */
    r->reghasz = re_has_z;
    scan = r->program + 1;      /* First BRANCH. */
    if (OP(regnext(scan)) == END)   /* Only one top-level choice. */
    {
        scan = OPERAND(scan);

        /* Starting-point info. */
        if (OP(scan) == BOL || OP(scan) == RE_BOF)
        {
            r->reganch++;
            scan = regnext(scan);
        }

        if (OP(scan) == EXACTLY)
        {
            r->regstart = utf_ptr2char(OPERAND(scan));
        }
        else if ((OP(scan) == BOW
                    || OP(scan) == EOW
                    || OP(scan) == NOTHING
                    || OP(scan) == MOPEN + 0 || OP(scan) == NOPEN
                    || OP(scan) == MCLOSE + 0 || OP(scan) == NCLOSE)
                 && OP(regnext(scan)) == EXACTLY)
        {
            r->regstart = utf_ptr2char(OPERAND(regnext(scan)));
        }

        /*
         * If there's something expensive in the r.e., find the longest
         * literal string that must appear and make it the regmust.  Resolve
         * ties in favor of later strings, since the regstart check works
         * with the beginning of the r.e. and avoiding duplication
         * strengthens checking.  Not a strong reason, but sufficient in the
         * absence of others.
         */
        /*
         * When the r.e. starts with BOW, it is faster to look for a regmust
         * first. Used a lot for "#" and "*" commands. (Added by mool).
         */
        if ((flags & SPSTART || OP(scan) == BOW || OP(scan) == EOW) && !(flags & HASNL))
        {
            longest = NULL;
            len = 0;
            for (; scan != NULL; scan = regnext(scan))
                if (OP(scan) == EXACTLY && STRLEN(OPERAND(scan)) >= (size_t)len)
                {
                    longest = OPERAND(scan);
                    len = (int)STRLEN(OPERAND(scan));
                }
            r->regmust = longest;
            r->regmlen = len;
        }
    }
    r->engine = &bt_regengine;
    return (regprog_T *)r;
}

/*
 * Free a compiled regexp program, returned by bt_regcomp().
 */
    static void
bt_regfree(prog)
    regprog_T   *prog;
{
    vim_free(prog);
}

/*
 * Setup to parse the regexp.  Used once to get the length and once to do it.
 */
    static void
regcomp_start(expr, re_flags)
    char_u      *expr;
    int         re_flags;           /* see vim_regcomp() */
{
    initchr(expr);
    if (re_flags & RE_MAGIC)
        reg_magic = MAGIC_ON;
    else
        reg_magic = MAGIC_OFF;
    reg_string = (re_flags & RE_STRING);
    reg_strict = (re_flags & RE_STRICT);
    get_cpo_flags();

    num_complex_braces = 0;
    regnpar = 1;
    vim_memset(had_endbrace, 0, sizeof(had_endbrace));
    regnzpar = 1;
    re_has_z = 0;
    regsize = 0L;
    reg_toolong = FALSE;
    regflags = 0;
    had_eol = FALSE;
}

/*
 * Check if during the previous call to vim_regcomp the EOL item "$" has been
 * found.  This is messy, but it works fine.
 */
    int
vim_regcomp_had_eol()
{
    return had_eol;
}

/*
 * Parse regular expression, i.e. main body or parenthesized thing.
 *
 * Caller must absorb opening parenthesis.
 *
 * Combining parenthesis handling with the base level of regular expression
 * is a trifle forced, but the need to tie the tails of the branches to what
 * follows makes it hard to avoid.
 */
    static char_u *
reg(paren, flagp)
    int         paren;  /* REG_NOPAREN, REG_PAREN, REG_NPAREN or REG_ZPAREN */
    int         *flagp;
{
    char_u      *ret;
    char_u      *br;
    char_u      *ender;
    int         parno = 0;
    int         flags;

    *flagp = HASWIDTH;          /* Tentatively. */

    if (paren == REG_ZPAREN)
    {
        /* Make a ZOPEN node. */
        if (regnzpar >= NSUBEXP)
            EMSG_RET_NULL("E50: Too many \\z(");
        parno = regnzpar;
        regnzpar++;
        ret = regnode(ZOPEN + parno);
    }
    else if (paren == REG_PAREN)
    {
        /* Make a MOPEN node. */
        if (regnpar >= NSUBEXP)
            EMSG2_RET_NULL("E51: Too many %s(", reg_magic == MAGIC_ALL);
        parno = regnpar;
        ++regnpar;
        ret = regnode(MOPEN + parno);
    }
    else if (paren == REG_NPAREN)
    {
        /* Make a NOPEN node. */
        ret = regnode(NOPEN);
    }
    else
        ret = NULL;

    /* Pick up the branches, linking them together. */
    br = regbranch(&flags);
    if (br == NULL)
        return NULL;
    if (ret != NULL)
        regtail(ret, br);       /* [MZ]OPEN -> first. */
    else
        ret = br;
    /* If one of the branches can be zero-width, the whole thing can.
     * If one of the branches has * at start or matches a line-break, the
     * whole thing can. */
    if (!(flags & HASWIDTH))
        *flagp &= ~HASWIDTH;
    *flagp |= flags & (SPSTART | HASNL | HASLOOKBH);
    while (peekchr() == Magic('|'))
    {
        skipchr();
        br = regbranch(&flags);
        if (br == NULL || reg_toolong)
            return NULL;
        regtail(ret, br);       /* BRANCH -> BRANCH. */
        if (!(flags & HASWIDTH))
            *flagp &= ~HASWIDTH;
        *flagp |= flags & (SPSTART | HASNL | HASLOOKBH);
    }

    /* Make a closing node, and hook it on the end. */
    ender = regnode(
            (paren == REG_ZPAREN) ? ZCLOSE + parno :
            (paren == REG_PAREN) ? MCLOSE + parno :
            (paren == REG_NPAREN) ? NCLOSE : END);
    regtail(ret, ender);

    /* Hook the tails of the branches to the closing node. */
    for (br = ret; br != NULL; br = regnext(br))
        regoptail(br, ender);

    /* Check for proper termination. */
    if (paren != REG_NOPAREN && getchr() != Magic(')'))
    {
        if (paren == REG_ZPAREN)
            EMSG_RET_NULL("E52: Unmatched \\z(");
        else if (paren == REG_NPAREN)
            EMSG2_RET_NULL((char *)e_unmatchedpp, reg_magic == MAGIC_ALL);
        else
            EMSG2_RET_NULL((char *)e_unmatchedp, reg_magic == MAGIC_ALL);
    }
    else if (paren == REG_NOPAREN && peekchr() != NUL)
    {
        if (curchr == Magic(')'))
            EMSG2_RET_NULL((char *)e_unmatchedpar, reg_magic == MAGIC_ALL);
        else
            EMSG_RET_NULL((char *)e_trailing);       /* "Can't happen". */
        /* NOTREACHED */
    }
    /*
     * Here we set the flag allowing back references to this set of parentheses.
     */
    if (paren == REG_PAREN)
        had_endbrace[parno] = TRUE;     /* have seen the close paren */
    return ret;
}

/*
 * Parse one alternative of an | operator.
 * Implements the & operator.
 */
    static char_u *
regbranch(flagp)
    int         *flagp;
{
    char_u      *ret;
    char_u      *chain = NULL;
    char_u      *latest;
    int         flags;

    *flagp = WORST | HASNL;             /* Tentatively. */

    ret = regnode(BRANCH);
    for (;;)
    {
        latest = regconcat(&flags);
        if (latest == NULL)
            return NULL;
        /* If one of the branches has width, the whole thing has.  If one of
         * the branches anchors at start-of-line, the whole thing does.
         * If one of the branches uses look-behind, the whole thing does. */
        *flagp |= flags & (HASWIDTH | SPSTART | HASLOOKBH);
        /* If one of the branches doesn't match a line-break, the whole thing doesn't. */
        *flagp &= ~HASNL | (flags & HASNL);
        if (chain != NULL)
            regtail(chain, latest);
        if (peekchr() != Magic('&'))
            break;
        skipchr();
        regtail(latest, regnode(END)); /* operand ends */
        if (reg_toolong)
            break;
        reginsert(MATCH, latest);
        chain = latest;
    }

    return ret;
}

/*
 * Parse one alternative of an | or & operator.
 * Implements the concatenation operator.
 */
    static char_u *
regconcat(flagp)
    int         *flagp;
{
    char_u      *first = NULL;
    char_u      *chain = NULL;
    char_u      *latest;
    int         flags;
    int         cont = TRUE;

    *flagp = WORST;             /* Tentatively. */

    while (cont)
    {
        switch (peekchr())
        {
            case NUL:
            case Magic('|'):
            case Magic('&'):
            case Magic(')'):
                            cont = FALSE;
                            break;
            case Magic('Z'):
                            regflags |= RF_ICOMBINE;
                            skipchr_keepstart();
                            break;
            case Magic('c'):
                            regflags |= RF_ICASE;
                            skipchr_keepstart();
                            break;
            case Magic('C'):
                            regflags |= RF_NOICASE;
                            skipchr_keepstart();
                            break;
            case Magic('v'):
                            reg_magic = MAGIC_ALL;
                            skipchr_keepstart();
                            curchr = -1;
                            break;
            case Magic('m'):
                            reg_magic = MAGIC_ON;
                            skipchr_keepstart();
                            curchr = -1;
                            break;
            case Magic('M'):
                            reg_magic = MAGIC_OFF;
                            skipchr_keepstart();
                            curchr = -1;
                            break;
            case Magic('V'):
                            reg_magic = MAGIC_NONE;
                            skipchr_keepstart();
                            curchr = -1;
                            break;
            default:
                            latest = regpiece(&flags);
                            if (latest == NULL || reg_toolong)
                                return NULL;
                            *flagp |= flags & (HASWIDTH | HASNL | HASLOOKBH);
                            if (chain == NULL)  /* First piece. */
                                *flagp |= flags & SPSTART;
                            else
                                regtail(chain, latest);
                            chain = latest;
                            if (first == NULL)
                                first = latest;
                            break;
        }
    }
    if (first == NULL)          /* Loop ran zero times. */
        first = regnode(NOTHING);
    return first;
}

/*
 * Parse something followed by possible [*+=].
 *
 * Note that the branching code sequences used for = and the general cases
 * of * and + are somewhat optimized:  they use the same NOTHING node as
 * both the endmarker for their branch list and the body of the last branch.
 * It might seem that this node could be dispensed with entirely, but the
 * endmarker role is not redundant.
 */
    static char_u *
regpiece(flagp)
    int             *flagp;
{
    char_u          *ret;
    int             op;
    char_u          *next;
    int             flags;
    long            minval;
    long            maxval;

    ret = regatom(&flags);
    if (ret == NULL)
        return NULL;

    op = peekchr();
    if (re_multi_type(op) == NOT_MULTI)
    {
        *flagp = flags;
        return ret;
    }
    /* default flags */
    *flagp = (WORST | SPSTART | (flags & (HASNL | HASLOOKBH)));

    skipchr();
    switch (op)
    {
        case Magic('*'):
            if (flags & SIMPLE)
                reginsert(STAR, ret);
            else
            {
                /* Emit x* as (x&|), where & means "self". */
                reginsert(BRANCH, ret); /* Either x */
                regoptail(ret, regnode(BACK));  /* and loop */
                regoptail(ret, ret);    /* back */
                regtail(ret, regnode(BRANCH));  /* or */
                regtail(ret, regnode(NOTHING)); /* null. */
            }
            break;

        case Magic('+'):
            if (flags & SIMPLE)
                reginsert(PLUS, ret);
            else
            {
                /* Emit x+ as x(&|), where & means "self". */
                next = regnode(BRANCH); /* Either */
                regtail(ret, next);
                regtail(regnode(BACK), ret);    /* loop back */
                regtail(next, regnode(BRANCH)); /* or */
                regtail(ret, regnode(NOTHING)); /* null. */
            }
            *flagp = (WORST | HASWIDTH | (flags & (HASNL | HASLOOKBH)));
            break;

        case Magic('@'):
            {
                int     lop = END;
                int     nr;

                nr = getdecchrs();
                switch (no_Magic(getchr()))
                {
                    case '=': lop = MATCH; break;                 /* \@= */
                    case '!': lop = NOMATCH; break;               /* \@! */
                    case '>': lop = SUBPAT; break;                /* \@> */
                    case '<': switch (no_Magic(getchr()))
                              {
                                  case '=': lop = BEHIND; break;   /* \@<= */
                                  case '!': lop = NOBEHIND; break; /* \@<! */
                              }
                }
                if (lop == END)
                    EMSG2_RET_NULL("E59: invalid character after %s@", reg_magic == MAGIC_ALL);
                /* Look behind must match with behind_pos. */
                if (lop == BEHIND || lop == NOBEHIND)
                {
                    regtail(ret, regnode(BHPOS));
                    *flagp |= HASLOOKBH;
                }
                regtail(ret, regnode(END)); /* operand ends */
                if (lop == BEHIND || lop == NOBEHIND)
                {
                    if (nr < 0)
                        nr = 0; /* no limit is same as zero limit */
                    reginsert_nr(lop, nr, ret);
                }
                else
                    reginsert(lop, ret);
                break;
            }

        case Magic('?'):
        case Magic('='):
            /* Emit x= as (x|) */
            reginsert(BRANCH, ret);             /* Either x */
            regtail(ret, regnode(BRANCH));      /* or */
            next = regnode(NOTHING);            /* null. */
            regtail(ret, next);
            regoptail(ret, next);
            break;

        case Magic('{'):
            if (!read_limits(&minval, &maxval))
                return NULL;
            if (flags & SIMPLE)
            {
                reginsert(BRACE_SIMPLE, ret);
                reginsert_limits(BRACE_LIMITS, minval, maxval, ret);
            }
            else
            {
                if (num_complex_braces >= 10)
                    EMSG2_RET_NULL("E60: Too many complex %s{...}s", reg_magic == MAGIC_ALL);
                reginsert(BRACE_COMPLEX + num_complex_braces, ret);
                regoptail(ret, regnode(BACK));
                regoptail(ret, ret);
                reginsert_limits(BRACE_LIMITS, minval, maxval, ret);
                ++num_complex_braces;
            }
            if (minval > 0 && maxval > 0)
                *flagp = (HASWIDTH | (flags & (HASNL | HASLOOKBH)));
            break;
    }
    if (re_multi_type(peekchr()) != NOT_MULTI)
    {
        /* Can't have a multi follow a multi. */
        if (peekchr() == Magic('*'))
            sprintf((char *)IObuff, "E61: Nested %s*", reg_magic >= MAGIC_ON ? "" : "\\");
        else
            sprintf((char *)IObuff, "E62: Nested %s%c",
                reg_magic == MAGIC_ALL ? "" : "\\", no_Magic(peekchr()));
        EMSG_RET_NULL(IObuff);
    }

    return ret;
}

/* When making changes to classchars also change nfa_classcodes. */
static char_u   *classchars = (char_u *)".iIkKfFpPsSdDxXoOwWhHaAlLuU";
static int      classcodes[] =
{
    ANY, IDENT, SIDENT, KWORD, SKWORD,
    FNAME, SFNAME, PRINT, SPRINT,
    WHITE, NWHITE, DIGIT, NDIGIT,
    HEX, NHEX, OCTAL, NOCTAL,
    WORD, NWORD, HEAD, NHEAD,
    ALPHA, NALPHA, LOWER, NLOWER,
    UPPER, NUPPER
};

/*
 * Parse the lowest level.
 *
 * Optimization:  gobbles an entire sequence of ordinary characters so that
 * it can turn them into a single node, which is smaller to store and
 * faster to run.  Don't do this when one_exactly is set.
 */
    static char_u *
regatom(flagp)
    int            *flagp;
{
    char_u          *ret;
    int             flags;
    int             c;
    char_u          *p;
    int             extra = 0;

    *flagp = WORST;             /* Tentatively. */

    c = getchr();
    switch (c)
    {
      case Magic('^'):
        ret = regnode(BOL);
        break;

      case Magic('$'):
        ret = regnode(EOL);
        had_eol = TRUE;
        break;

      case Magic('<'):
        ret = regnode(BOW);
        break;

      case Magic('>'):
        ret = regnode(EOW);
        break;

      case Magic('_'):
        c = no_Magic(getchr());
        if (c == '^')           /* "\_^" is start-of-line */
        {
            ret = regnode(BOL);
            break;
        }
        if (c == '$')           /* "\_$" is end-of-line */
        {
            ret = regnode(EOL);
            had_eol = TRUE;
            break;
        }

        extra = ADD_NL;
        *flagp |= HASNL;

        /* "\_[" is character range plus newline */
        if (c == '[')
            goto collection;

        /* "\_x" is character class plus newline */
        /*FALLTHROUGH*/

        /*
         * Character classes.
         */
      case Magic('.'):
      case Magic('i'):
      case Magic('I'):
      case Magic('k'):
      case Magic('K'):
      case Magic('f'):
      case Magic('F'):
      case Magic('p'):
      case Magic('P'):
      case Magic('s'):
      case Magic('S'):
      case Magic('d'):
      case Magic('D'):
      case Magic('x'):
      case Magic('X'):
      case Magic('o'):
      case Magic('O'):
      case Magic('w'):
      case Magic('W'):
      case Magic('h'):
      case Magic('H'):
      case Magic('a'):
      case Magic('A'):
      case Magic('l'):
      case Magic('L'):
      case Magic('u'):
      case Magic('U'):
        p = vim_strchr(classchars, no_Magic(c));
        if (p == NULL)
            EMSG_RET_NULL("E63: invalid use of \\_");
        /* When '.' is followed by a composing char ignore the dot, so that
         * the composing char is matched here. */
        if (c == Magic('.') && utf_iscomposing(peekchr()))
        {
            c = getchr();
            goto do_multibyte;
        }
        ret = regnode(classcodes[p - classchars] + extra);
        *flagp |= HASWIDTH | SIMPLE;
        break;

      case Magic('n'):
        if (reg_string)
        {
            /* In a string "\n" matches a newline character. */
            ret = regnode(EXACTLY);
            regc(NL);
            regc(NUL);
            *flagp |= HASWIDTH | SIMPLE;
        }
        else
        {
            /* In buffer text "\n" matches the end of a line. */
            ret = regnode(NEWL);
            *flagp |= HASWIDTH | HASNL;
        }
        break;

      case Magic('('):
        if (one_exactly)
            EMSG2_RET_NULL("E369: invalid item in %s%%[]", reg_magic == MAGIC_ALL);
        ret = reg(REG_PAREN, &flags);
        if (ret == NULL)
            return NULL;
        *flagp |= flags & (HASWIDTH | SPSTART | HASNL | HASLOOKBH);
        break;

      case NUL:
      case Magic('|'):
      case Magic('&'):
      case Magic(')'):
        if (one_exactly)
            EMSG2_RET_NULL("E369: invalid item in %s%%[]", reg_magic == MAGIC_ALL);
        EMSG_RET_NULL((char *)e_internal);   /* Supposed to be caught earlier. */
        /* NOTREACHED */

      case Magic('='):
      case Magic('?'):
      case Magic('+'):
      case Magic('@'):
      case Magic('{'):
      case Magic('*'):
        c = no_Magic(c);
        sprintf((char *)IObuff, "E64: %s%c follows nothing",
                (c == '*' ? reg_magic >= MAGIC_ON : reg_magic == MAGIC_ALL)
                ? "" : "\\", c);
        EMSG_RET_NULL(IObuff);
        /* NOTREACHED */

      case Magic('~'):          /* previous substitute pattern */
            if (reg_prev_sub != NULL)
            {
                char_u      *lp;

                ret = regnode(EXACTLY);
                lp = reg_prev_sub;
                while (*lp != NUL)
                    regc(*lp++);
                regc(NUL);
                if (*reg_prev_sub != NUL)
                {
                    *flagp |= HASWIDTH;
                    if ((lp - reg_prev_sub) == 1)
                        *flagp |= SIMPLE;
                }
            }
            else
                EMSG_RET_NULL((char *)e_nopresub);
            break;

      case Magic('1'):
      case Magic('2'):
      case Magic('3'):
      case Magic('4'):
      case Magic('5'):
      case Magic('6'):
      case Magic('7'):
      case Magic('8'):
      case Magic('9'):
            {
                int                 refnum;

                refnum = c - Magic('0');
                /*
                 * Check if the back reference is legal. We must have seen the close brace.
                 * TODO: Should also check that we don't refer to something
                 * that is repeated (+*=): what instance of the repetition should we match?
                 */
                if (!had_endbrace[refnum])
                {
                    /* Trick: check if "@<=" or "@<!" follows, in which case
                     * the \1 can appear before the referenced match. */
                    for (p = regparse; *p != NUL; ++p)
                        if (p[0] == '@' && p[1] == '<' && (p[2] == '!' || p[2] == '='))
                            break;
                    if (*p == NUL)
                        EMSG_RET_NULL("E65: Illegal back reference");
                }
                ret = regnode(BACKREF + refnum);
            }
            break;

      case Magic('z'):
        {
            c = no_Magic(getchr());
            switch (c)
            {
                case '(': if (reg_do_extmatch != REX_SET)
                              EMSG_RET_NULL((char *)e_z_not_allowed);
                          if (one_exactly)
                              EMSG2_RET_NULL("E369: invalid item in %s%%[]", reg_magic == MAGIC_ALL);
                          ret = reg(REG_ZPAREN, &flags);
                          if (ret == NULL)
                              return NULL;
                          *flagp |= flags & (HASWIDTH|SPSTART|HASNL|HASLOOKBH);
                          re_has_z = REX_SET;
                          break;

                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9': if (reg_do_extmatch != REX_USE)
                              EMSG_RET_NULL((char *)e_z1_not_allowed);
                          ret = regnode(ZREF + c - '0');
                          re_has_z = REX_USE;
                          break;

                case 's': ret = regnode(MOPEN + 0);
                          if (re_mult_next("\\zs") == FAIL)
                              return NULL;
                          break;

                case 'e': ret = regnode(MCLOSE + 0);
                          if (re_mult_next("\\ze") == FAIL)
                              return NULL;
                          break;

                default:  EMSG_RET_NULL("E68: Invalid character after \\z");
            }
        }
        break;

      case Magic('%'):
        {
            c = no_Magic(getchr());
            switch (c)
            {
                /* () without a back reference */
                case '(':
                    if (one_exactly)
                        EMSG2_RET_NULL("E369: invalid item in %s%%[]", reg_magic == MAGIC_ALL);
                    ret = reg(REG_NPAREN, &flags);
                    if (ret == NULL)
                        return NULL;
                    *flagp |= flags & (HASWIDTH | SPSTART | HASNL | HASLOOKBH);
                    break;

                /* Catch \%^ and \%$ regardless of where they appear in the
                 * pattern -- regardless of whether or not it makes sense. */
                case '^':
                    ret = regnode(RE_BOF);
                    break;

                case '$':
                    ret = regnode(RE_EOF);
                    break;

                case '#':
                    ret = regnode(CURSOR);
                    break;

                case 'V':
                    ret = regnode(RE_VISUAL);
                    break;

                case 'C':
                    ret = regnode(RE_COMPOSING);
                    break;

                /* \%[abc]: Emit as a list of branches, all ending at the last
                 * branch which matches nothing. */
                case '[':
                          if (one_exactly)      /* doesn't nest */
                              EMSG2_RET_NULL("E369: invalid item in %s%%[]", reg_magic == MAGIC_ALL);
                          {
                              char_u    *lastbranch;
                              char_u    *lastnode = NULL;
                              char_u    *br;

                              ret = NULL;
                              while ((c = getchr()) != ']')
                              {
                                  if (c == NUL)
                                      EMSG2_RET_NULL((char *)e_missing_sb, reg_magic == MAGIC_ALL);
                                  br = regnode(BRANCH);
                                  if (ret == NULL)
                                      ret = br;
                                  else
                                      regtail(lastnode, br);

                                  ungetchr();
                                  one_exactly = TRUE;
                                  lastnode = regatom(flagp);
                                  one_exactly = FALSE;
                                  if (lastnode == NULL)
                                      return NULL;
                              }
                              if (ret == NULL)
                                  EMSG2_RET_NULL((char *)e_empty_sb, reg_magic == MAGIC_ALL);
                              lastbranch = regnode(BRANCH);
                              br = regnode(NOTHING);
                              if (ret != JUST_CALC_SIZE)
                              {
                                  regtail(lastnode, br);
                                  regtail(lastbranch, br);
                                  /* connect all branches to the NOTHING
                                   * branch at the end */
                                  for (br = ret; br != lastnode; )
                                  {
                                      if (OP(br) == BRANCH)
                                      {
                                          regtail(br, lastbranch);
                                          br = OPERAND(br);
                                      }
                                      else
                                          br = regnext(br);
                                  }
                              }
                              *flagp &= ~(HASWIDTH | SIMPLE);
                              break;
                          }

                case 'd':   /* %d123 decimal */
                case 'o':   /* %o123 octal */
                case 'x':   /* %xab hex 2 */
                case 'u':   /* %uabcd hex 4 */
                case 'U':   /* %U1234abcd hex 8 */
                          {
                              int i;

                              switch (c)
                              {
                                  case 'd': i = getdecchrs(); break;
                                  case 'o': i = getoctchrs(); break;
                                  case 'x': i = gethexchrs(2); break;
                                  case 'u': i = gethexchrs(4); break;
                                  case 'U': i = gethexchrs(8); break;
                                  default:  i = -1; break;
                              }

                              if (i < 0)
                                  EMSG2_RET_NULL("E678: Invalid character after %s%%[dxouU]",
                                        reg_magic == MAGIC_ALL);
                              if (use_multibytecode(i))
                                  ret = regnode(MULTIBYTECODE);
                              else
                                  ret = regnode(EXACTLY);
                              if (i == 0)
                                  regc(0x0a);
                              else
                                  regmbc(i);
                              regc(NUL);
                              *flagp |= HASWIDTH;
                              break;
                          }

                default:
                          if (VIM_ISDIGIT(c) || c == '<' || c == '>' || c == '\'')
                          {
                              long_u    n = 0;
                              int       cmp;

                              cmp = c;
                              if (cmp == '<' || cmp == '>')
                                  c = getchr();
                              while (VIM_ISDIGIT(c))
                              {
                                  n = n * 10 + (c - '0');
                                  c = getchr();
                              }
                              if (c == '\'' && n == 0)
                              {
                                  /* "\%'m", "\%<'m" and "\%>'m": Mark */
                                  c = getchr();
                                  ret = regnode(RE_MARK);
                                  if (ret == JUST_CALC_SIZE)
                                      regsize += 2;
                                  else
                                  {
                                      *regcode++ = c;
                                      *regcode++ = cmp;
                                  }
                                  break;
                              }
                              else if (c == 'l' || c == 'c' || c == 'v')
                              {
                                  if (c == 'l')
                                      ret = regnode(RE_LNUM);
                                  else if (c == 'c')
                                      ret = regnode(RE_COL);
                                  else
                                      ret = regnode(RE_VCOL);
                                  if (ret == JUST_CALC_SIZE)
                                      regsize += 5;
                                  else
                                  {
                                      /* put the number and the optional
                                       * comparator after the opcode */
                                      regcode = re_put_long(regcode, n);
                                      *regcode++ = cmp;
                                  }
                                  break;
                              }
                          }

                          EMSG2_RET_NULL("E71: Invalid character after %s%%",
                                                      reg_magic == MAGIC_ALL);
            }
        }
        break;

      case Magic('['):
collection:
        {
            char_u      *lp;

            /*
             * If there is no matching ']', we assume the '[' is a normal
             * character.  This makes 'incsearch' and ":help [" work.
             */
            lp = skip_anyof(regparse);
            if (*lp == ']')     /* there is a matching ']' */
            {
                int     startc = -1;    /* > 0 when next '-' is a range */
                int     endc;

                /*
                 * In a character class, different parsing rules apply.
                 * Not even \ is special anymore, nothing is.
                 */
                if (*regparse == '^')       /* Complement of range. */
                {
                    ret = regnode(ANYBUT + extra);
                    regparse++;
                }
                else
                    ret = regnode(ANYOF + extra);

                /* At the start ']' and '-' mean the literal character. */
                if (*regparse == ']' || *regparse == '-')
                {
                    startc = *regparse;
                    regc(*regparse++);
                }

                while (*regparse != NUL && *regparse != ']')
                {
                    if (*regparse == '-')
                    {
                        ++regparse;
                        /* The '-' is not used for a range at the end and
                         * after or before a '\n'. */
                        if (*regparse == ']' || *regparse == NUL
                                || startc == -1
                                || (regparse[0] == '\\' && regparse[1] == 'n'))
                        {
                            regc('-');
                            startc = '-';       /* [--x] is a range */
                        }
                        else
                        {
                            /* Also accept "a-[.z.]" */
                            endc = 0;
                            if (*regparse == '[')
                                endc = get_coll_element(&regparse);
                            if (endc == 0)
                                endc = mb_ptr2char_adv(&regparse);

                            /* Handle \o40, \x20 and \u20AC style sequences */
                            if (endc == '\\' && !reg_cpo_lit && !reg_cpo_bsl)
                                endc = coll_get_char();

                            if (startc > endc)
                                EMSG_RET_NULL((char *)e_invrange);
                            if (utf_char2len(startc) > 1 || utf_char2len(endc) > 1)
                            {
                                /* Limit to a range of 256 chars */
                                if (endc > startc + 256)
                                    EMSG_RET_NULL((char *)e_invrange);
                                while (++startc <= endc)
                                    regmbc(startc);
                            }
                            else
                            {
                                while (++startc <= endc)
                                    regc(startc);
                            }
                            startc = -1;
                        }
                    }
                    /*
                     * Only "\]", "\^", "\]" and "\\" are special in Vi.  Vim
                     * accepts "\t", "\e", etc., but only when the 'l' flag in
                     * 'cpoptions' is not included.
                     * Posix doesn't recognize backslash at all.
                     */
                    else if (*regparse == '\\'
                            && !reg_cpo_bsl
                            && (vim_strchr(REGEXP_INRANGE, regparse[1]) != NULL
                                || (!reg_cpo_lit
                                    && vim_strchr(REGEXP_ABBR, regparse[1]) != NULL)))
                    {
                        regparse++;
                        if (*regparse == 'n')
                        {
                            /* '\n' in range: also match NL */
                            if (ret != JUST_CALC_SIZE)
                            {
                                /* Using \n inside [^] does not change what
                                 * matches. "[^\n]" is the same as ".". */
                                if (*ret == ANYOF)
                                {
                                    *ret = ANYOF + ADD_NL;
                                    *flagp |= HASNL;
                                }
                                /* else: must have had a \n already */
                            }
                            regparse++;
                            startc = -1;
                        }
                        else if (*regparse == 'd'
                                || *regparse == 'o'
                                || *regparse == 'x'
                                || *regparse == 'u'
                                || *regparse == 'U')
                        {
                            startc = coll_get_char();
                            if (startc == 0)
                                regc(0x0a);
                            else
                                regmbc(startc);
                        }
                        else
                        {
                            startc = backslash_trans(*regparse++);
                            regc(startc);
                        }
                    }
                    else if (*regparse == '[')
                    {
                        int c_class;
                        int cu;

                        c_class = get_char_class(&regparse);
                        startc = -1;
                        /* Characters assumed to be 8 bits! */
                        switch (c_class)
                        {
                            case CLASS_NONE:
                                c_class = get_equi_class(&regparse);
                                if (c_class != 0)
                                {
                                    /* produce equivalence class */
                                    reg_equi_class(c_class);
                                }
                                else if ((c_class = get_coll_element(&regparse)) != 0)
                                {
                                    /* produce a collating element */
                                    regmbc(c_class);
                                }
                                else
                                {
                                    /* literal '[', allow [[-x] as a range */
                                    startc = *regparse++;
                                    regc(startc);
                                }
                                break;
                            case CLASS_ALNUM:
                                for (cu = 1; cu <= 255; cu++)
                                    if (isalnum(cu))
                                        regc(cu);
                                break;
                            case CLASS_ALPHA:
                                for (cu = 1; cu <= 255; cu++)
                                    if (isalpha(cu))
                                        regc(cu);
                                break;
                            case CLASS_BLANK:
                                regc(' ');
                                regc('\t');
                                break;
                            case CLASS_CNTRL:
                                for (cu = 1; cu <= 255; cu++)
                                    if (iscntrl(cu))
                                        regc(cu);
                                break;
                            case CLASS_DIGIT:
                                for (cu = 1; cu <= 255; cu++)
                                    if (VIM_ISDIGIT(cu))
                                        regc(cu);
                                break;
                            case CLASS_GRAPH:
                                for (cu = 1; cu <= 255; cu++)
                                    if (isgraph(cu))
                                        regc(cu);
                                break;
                            case CLASS_LOWER:
                                for (cu = 1; cu <= 255; cu++)
                                    if (vim_islower(cu))
                                        regc(cu);
                                break;
                            case CLASS_PRINT:
                                for (cu = 1; cu <= 255; cu++)
                                    if (vim_isprintc(cu))
                                        regc(cu);
                                break;
                            case CLASS_PUNCT:
                                for (cu = 1; cu <= 255; cu++)
                                    if (ispunct(cu))
                                        regc(cu);
                                break;
                            case CLASS_SPACE:
                                for (cu = 9; cu <= 13; cu++)
                                    regc(cu);
                                regc(' ');
                                break;
                            case CLASS_UPPER:
                                for (cu = 1; cu <= 255; cu++)
                                    if (vim_isupper(cu))
                                        regc(cu);
                                break;
                            case CLASS_XDIGIT:
                                for (cu = 1; cu <= 255; cu++)
                                    if (vim_isxdigit(cu))
                                        regc(cu);
                                break;
                            case CLASS_TAB:
                                regc('\t');
                                break;
                            case CLASS_RETURN:
                                regc('\r');
                                break;
                            case CLASS_BACKSPACE:
                                regc('\b');
                                break;
                            case CLASS_ESCAPE:
                                regc('\033');
                                break;
                        }
                    }
                    else
                    {
                        int len;

                        /* produce a multibyte character, including any
                         * following composing characters */
                        startc = utf_ptr2char(regparse);
                        len = utfc_ptr2len(regparse);
                        if (utf_char2len(startc) != len)
                            startc = -1;    /* composing chars */
                        while (--len >= 0)
                            regc(*regparse++);
                    }
                }
                regc(NUL);
                prevchr_len = 1;        /* last char was the ']' */
                if (*regparse != ']')
                    EMSG_RET_NULL((char *)e_toomsbra);       /* Cannot happen? */
                skipchr();          /* let's be friends with the lexer again */
                *flagp |= HASWIDTH | SIMPLE;
                break;
            }
            else if (reg_strict)
                EMSG2_RET_NULL((char *)e_missingbracket, reg_magic > MAGIC_OFF);
        }
        /* FALLTHROUGH */

      default:
        {
            int         len;

            /* A multi-byte character is handled as a separate atom if it's
             * before a multi and when it's a composing char. */
            if (use_multibytecode(c))
            {
do_multibyte:
                ret = regnode(MULTIBYTECODE);
                regmbc(c);
                *flagp |= HASWIDTH | SIMPLE;
                break;
            }

            ret = regnode(EXACTLY);

            /*
             * Append characters as long as:
             * - there is no following multi, we then need the character in
             *   front of it as a single character operand
             * - not running into a Magic character
             * - "one_exactly" is not set
             * But always emit at least one character.  Might be a Multi,
             * e.g., a "[" without matching "]".
             */
            for (len = 0; c != NUL && (len == 0
                        || (re_multi_type(peekchr()) == NOT_MULTI
                            && !one_exactly
                            && !is_Magic(c))); ++len)
            {
                c = no_Magic(c);

                regmbc(c);

                {
                    int     l;

                    /* Need to get composing character too. */
                    for (;;)
                    {
                        l = utf_ptr2len(regparse);
                        if (!UTF_COMPOSINGLIKE(regparse, regparse + l))
                            break;
                        regmbc(utf_ptr2char(regparse));
                        skipchr();
                    }
                }

                c = getchr();
            }
            ungetchr();

            regc(NUL);
            *flagp |= HASWIDTH;
            if (len == 1)
                *flagp |= SIMPLE;
        }
        break;
    }

    return ret;
}

/*
 * Return TRUE if MULTIBYTECODE should be used instead of EXACTLY for character "c".
 */
    static int
use_multibytecode(c)
    int c;
{
    return utf_char2len(c) > 1 && (re_multi_type(peekchr()) != NOT_MULTI || utf_iscomposing(c));
}

/*
 * Emit a node.
 * Return pointer to generated code.
 */
    static char_u *
regnode(op)
    int         op;
{
    char_u  *ret;

    ret = regcode;
    if (ret == JUST_CALC_SIZE)
        regsize += 3;
    else
    {
        *regcode++ = op;
        *regcode++ = NUL;               /* Null "next" pointer. */
        *regcode++ = NUL;
    }
    return ret;
}

/*
 * Emit (if appropriate) a byte of code
 */
    static void
regc(b)
    int         b;
{
    if (regcode == JUST_CALC_SIZE)
        regsize++;
    else
        *regcode++ = b;
}

/*
 * Emit (if appropriate) a multi-byte character of code
 */
    static void
regmbc(c)
    int         c;
{
    if (regcode == JUST_CALC_SIZE)
        regsize += utf_char2len(c);
    else
        regcode += utf_char2bytes(c, regcode);
}

/*
 * Insert an operator in front of already-emitted operand
 *
 * Means relocating the operand.
 */
    static void
reginsert(op, opnd)
    int         op;
    char_u     *opnd;
{
    char_u      *src;
    char_u      *dst;
    char_u      *place;

    if (regcode == JUST_CALC_SIZE)
    {
        regsize += 3;
        return;
    }
    src = regcode;
    regcode += 3;
    dst = regcode;
    while (src > opnd)
        *--dst = *--src;

    place = opnd;               /* Op node, where operand used to be. */
    *place++ = op;
    *place++ = NUL;
    *place = NUL;
}

/*
 * Insert an operator in front of already-emitted operand.
 * Add a number to the operator.
 */
    static void
reginsert_nr(op, val, opnd)
    int         op;
    long        val;
    char_u      *opnd;
{
    char_u      *src;
    char_u      *dst;
    char_u      *place;

    if (regcode == JUST_CALC_SIZE)
    {
        regsize += 7;
        return;
    }
    src = regcode;
    regcode += 7;
    dst = regcode;
    while (src > opnd)
        *--dst = *--src;

    place = opnd;               /* Op node, where operand used to be. */
    *place++ = op;
    *place++ = NUL;
    *place++ = NUL;
    place = re_put_long(place, (long_u)val);
}

/*
 * Insert an operator in front of already-emitted operand.
 * The operator has the given limit values as operands.  Also set next pointer.
 *
 * Means relocating the operand.
 */
    static void
reginsert_limits(op, minval, maxval, opnd)
    int         op;
    long        minval;
    long        maxval;
    char_u      *opnd;
{
    char_u      *src;
    char_u      *dst;
    char_u      *place;

    if (regcode == JUST_CALC_SIZE)
    {
        regsize += 11;
        return;
    }
    src = regcode;
    regcode += 11;
    dst = regcode;
    while (src > opnd)
        *--dst = *--src;

    place = opnd;               /* Op node, where operand used to be. */
    *place++ = op;
    *place++ = NUL;
    *place++ = NUL;
    place = re_put_long(place, (long_u)minval);
    place = re_put_long(place, (long_u)maxval);
    regtail(opnd, place);
}

/*
 * Write a long as four bytes at "p" and return pointer to the next char.
 */
    static char_u *
re_put_long(p, val)
    char_u      *p;
    long_u      val;
{
    *p++ = (char_u) ((val >> 24) & 0377);
    *p++ = (char_u) ((val >> 16) & 0377);
    *p++ = (char_u) ((val >> 8) & 0377);
    *p++ = (char_u) (val & 0377);
    return p;
}

/*
 * Set the next-pointer at the end of a node chain.
 */
    static void
regtail(p, val)
    char_u      *p;
    char_u      *val;
{
    char_u      *scan;
    char_u      *temp;
    int         offset;

    if (p == JUST_CALC_SIZE)
        return;

    /* Find last node. */
    scan = p;
    for (;;)
    {
        temp = regnext(scan);
        if (temp == NULL)
            break;
        scan = temp;
    }

    if (OP(scan) == BACK)
        offset = (int)(scan - val);
    else
        offset = (int)(val - scan);
    /* When the offset uses more than 16 bits it can no longer fit in the two
     * bytes available.  Use a global flag to avoid having to check return
     * values in too many places. */
    if (offset > 0xffff)
        reg_toolong = TRUE;
    else
    {
        *(scan + 1) = (char_u) (((unsigned)offset >> 8) & 0377);
        *(scan + 2) = (char_u) (offset & 0377);
    }
}

/*
 * Like regtail, on item after a BRANCH; nop if none.
 */
    static void
regoptail(p, val)
    char_u      *p;
    char_u      *val;
{
    /* When op is neither BRANCH nor BRACE_COMPLEX0-9, it is "operandless" */
    if (p == NULL || p == JUST_CALC_SIZE
            || (OP(p) != BRANCH && (OP(p) < BRACE_COMPLEX || OP(p) > BRACE_COMPLEX + 9)))
        return;
    regtail(OPERAND(p), val);
}

/*
 * Functions for getting characters from the regexp input.
 */

static int      at_start;       /* True when on the first character */
static int      prev_at_start;  /* True when on the second character */

/*
 * Start parsing at "str".
 */
    static void
initchr(str)
    char_u *str;
{
    regparse = str;
    prevchr_len = 0;
    curchr = prevprevchr = prevchr = nextchr = -1;
    at_start = TRUE;
    prev_at_start = FALSE;
}

/*
 * Save the current parse state, so that it can be restored and parsing
 * starts in the same state again.
 */
    static void
save_parse_state(ps)
    parse_state_T *ps;
{
    ps->regparse = regparse;
    ps->prevchr_len = prevchr_len;
    ps->curchr = curchr;
    ps->prevchr = prevchr;
    ps->prevprevchr = prevprevchr;
    ps->nextchr = nextchr;
    ps->at_start = at_start;
    ps->prev_at_start = prev_at_start;
    ps->regnpar = regnpar;
}

/*
 * Restore a previously saved parse state.
 */
    static void
restore_parse_state(ps)
    parse_state_T *ps;
{
    regparse = ps->regparse;
    prevchr_len = ps->prevchr_len;
    curchr = ps->curchr;
    prevchr = ps->prevchr;
    prevprevchr = ps->prevprevchr;
    nextchr = ps->nextchr;
    at_start = ps->at_start;
    prev_at_start = ps->prev_at_start;
    regnpar = ps->regnpar;
}

/*
 * Get the next character without advancing.
 */
    static int
peekchr()
{
    static int  after_slash = FALSE;

    if (curchr == -1)
    {
        switch (curchr = regparse[0])
        {
        case '.':
        case '[':
        case '~':
            /* magic when 'magic' is on */
            if (reg_magic >= MAGIC_ON)
                curchr = Magic(curchr);
            break;
        case '(':
        case ')':
        case '{':
        case '%':
        case '+':
        case '=':
        case '?':
        case '@':
        case '!':
        case '&':
        case '|':
        case '<':
        case '>':
        case '#':       /* future ext. */
        case '"':       /* future ext. */
        case '\'':      /* future ext. */
        case ',':       /* future ext. */
        case '-':       /* future ext. */
        case ':':       /* future ext. */
        case ';':       /* future ext. */
        case '`':       /* future ext. */
        case '/':       /* Can't be used in / command */
            /* magic only after "\v" */
            if (reg_magic == MAGIC_ALL)
                curchr = Magic(curchr);
            break;
        case '*':
            /* * is not magic as the very first character, eg "?*ptr", when
             * after '^', eg "/^*ptr" and when after "\(", "\|", "\&".  But
             * "\(\*" is not magic, thus must be magic if "after_slash" */
            if (reg_magic >= MAGIC_ON
                    && !at_start
                    && !(prev_at_start && prevchr == Magic('^'))
                    && (after_slash
                        || (prevchr != Magic('(')
                            && prevchr != Magic('&')
                            && prevchr != Magic('|'))))
                curchr = Magic('*');
            break;
        case '^':
            /* '^' is only magic as the very first character and if it's after
             * "\(", "\|", "\&' or "\n" */
            if (reg_magic >= MAGIC_OFF
                    && (at_start
                        || reg_magic == MAGIC_ALL
                        || prevchr == Magic('(')
                        || prevchr == Magic('|')
                        || prevchr == Magic('&')
                        || prevchr == Magic('n')
                        || (no_Magic(prevchr) == '('
                            && prevprevchr == Magic('%'))))
            {
                curchr = Magic('^');
                at_start = TRUE;
                prev_at_start = FALSE;
            }
            break;
        case '$':
            /* '$' is only magic as the very last char and if it's in front of
             * either "\|", "\)", "\&", or "\n" */
            if (reg_magic >= MAGIC_OFF)
            {
                char_u *p = regparse + 1;
                int is_magic_all = (reg_magic == MAGIC_ALL);

                /* ignore \c \C \m \M \v \V and \Z after '$' */
                while (p[0] == '\\' && (p[1] == 'c' || p[1] == 'C'
                                || p[1] == 'm' || p[1] == 'M'
                                || p[1] == 'v' || p[1] == 'V' || p[1] == 'Z'))
                {
                    if (p[1] == 'v')
                        is_magic_all = TRUE;
                    else if (p[1] == 'm' || p[1] == 'M' || p[1] == 'V')
                        is_magic_all = FALSE;
                    p += 2;
                }
                if (p[0] == NUL
                        || (p[0] == '\\'
                            && (p[1] == '|' || p[1] == '&' || p[1] == ')' || p[1] == 'n'))
                        || (is_magic_all
                               && (p[0] == '|' || p[0] == '&' || p[0] == ')'))
                        || reg_magic == MAGIC_ALL)
                    curchr = Magic('$');
            }
            break;
        case '\\':
            {
                int c = regparse[1];

                if (c == NUL)
                    curchr = '\\';      /* trailing '\' */
                else if (c <= '~' && META_flags[c])
                {
                    /*
                     * META contains everything that may be magic sometimes,
                     * except ^ and $ ("\^" and "\$" are only magic after
                     * "\v").  We now fetch the next character and toggle its
                     * magicness.  Therefore, \ is so meta-magic that it is not in META.
                     */
                    curchr = -1;
                    prev_at_start = at_start;
                    at_start = FALSE;   /* be able to say "/\*ptr" */
                    ++regparse;
                    ++after_slash;
                    peekchr();
                    --regparse;
                    --after_slash;
                    curchr = toggle_Magic(curchr);
                }
                else if (vim_strchr(REGEXP_ABBR, c))
                {
                    /*
                     * Handle abbreviations, like "\t" for TAB -- webb
                     */
                    curchr = backslash_trans(c);
                }
                else if (reg_magic == MAGIC_NONE && (c == '$' || c == '^'))
                    curchr = toggle_Magic(c);
                else
                {
                    /*
                     * Next character can never be (made) magic?
                     * Then backslashing it won't do anything.
                     */
                    curchr = utf_ptr2char(regparse + 1);
                }
                break;
            }

        default:
            curchr = utf_ptr2char(regparse);
        }
    }

    return curchr;
}

/*
 * Eat one lexed character.  Do this in a way that we can undo it.
 */
    static void
skipchr()
{
    /* peekchr() eats a backslash, do the same here */
    if (*regparse == '\\')
        prevchr_len = 1;
    else
        prevchr_len = 0;
    if (regparse[prevchr_len] != NUL)
    {
        /* exclude composing chars that utfc_ptr2len does include */
        prevchr_len += utf_ptr2len(regparse + prevchr_len);
    }
    regparse += prevchr_len;
    prev_at_start = at_start;
    at_start = FALSE;
    prevprevchr = prevchr;
    prevchr = curchr;
    curchr = nextchr;       /* use previously unget char, or -1 */
    nextchr = -1;
}

/*
 * Skip a character while keeping the value of prev_at_start for at_start.
 * prevchr and prevprevchr are also kept.
 */
    static void
skipchr_keepstart()
{
    int as = prev_at_start;
    int pr = prevchr;
    int prpr = prevprevchr;

    skipchr();
    at_start = as;
    prevchr = pr;
    prevprevchr = prpr;
}

/*
 * Get the next character from the pattern. We know about magic and such, so
 * therefore we need a lexical analyzer.
 */
    static int
getchr()
{
    int chr = peekchr();

    skipchr();
    return chr;
}

/*
 * put character back.  Works only once!
 */
    static void
ungetchr()
{
    nextchr = curchr;
    curchr = prevchr;
    prevchr = prevprevchr;
    at_start = prev_at_start;
    prev_at_start = FALSE;

    /* Backup regparse, so that it's at the same position as before the getchr(). */
    regparse -= prevchr_len;
}

/*
 * Get and return the value of the hex string at the current position.
 * Return -1 if there is no valid hex number.
 * The position is updated:
 *     blahblah\%x20asdf
 *         before-^ ^-after
 * The parameter controls the maximum number of input characters. This will be
 * 2 when reading a \%x20 sequence and 4 when reading a \%u20AC sequence.
 */
    static int
gethexchrs(maxinputlen)
    int         maxinputlen;
{
    int         nr = 0;
    int         i;

    for (i = 0; i < maxinputlen; ++i)
    {
        int c = regparse[0];
        if (!vim_isxdigit(c))
            break;
        nr <<= 4;
        nr |= hex2nr(c);
        ++regparse;
    }

    if (i == 0)
        return -1;

    return nr;
}

/*
 * Get and return the value of the decimal string immediately after the
 * current position. Return -1 for invalid.  Consumes all digits.
 */
    static int
getdecchrs()
{
    int         nr = 0;
    int         i;

    for (i = 0; ; ++i)
    {
        int c = regparse[0];
        if (c < '0' || c > '9')
            break;
        nr *= 10;
        nr += c - '0';
        ++regparse;
        curchr = -1; /* no longer valid */
    }

    if (i == 0)
        return -1;

    return nr;
}

/*
 * get and return the value of the octal string immediately after the current
 * position. Return -1 for invalid, or 0-255 for valid. Smart enough to handle
 * numbers > 377 correctly (for example, 400 is treated as 40) and doesn't
 * treat 8 or 9 as recognised characters. Position is updated:
 *     blahblah\%o210asdf
 *         before-^  ^-after
 */
    static int
getoctchrs()
{
    int         nr = 0;
    int         i;

    for (i = 0; i < 3 && nr < 040; ++i)
    {
        int c = regparse[0];
        if (c < '0' || c > '7')
            break;
        nr <<= 3;
        nr |= hex2nr(c);
        ++regparse;
    }

    if (i == 0)
        return -1;

    return nr;
}

/*
 * Get a number after a backslash that is inside [].
 * When nothing is recognized return a backslash.
 */
    static int
coll_get_char()
{
    int     nr = -1;

    switch (*regparse++)
    {
        case 'd': nr = getdecchrs(); break;
        case 'o': nr = getoctchrs(); break;
        case 'x': nr = gethexchrs(2); break;
        case 'u': nr = gethexchrs(4); break;
        case 'U': nr = gethexchrs(8); break;
    }
    if (nr < 0)
    {
        /* If getting the number fails be backwards compatible: the character is a backslash. */
        --regparse;
        nr = '\\';
    }
    return nr;
}

/*
 * read_limits - Read two integers to be taken as a minimum and maximum.
 * If the first character is '-', then the range is reversed.
 * Should end with 'end'.  If minval is missing, zero is default, if maxval is
 * missing, a very big number is the default.
 */
    static int
read_limits(minval, maxval)
    long        *minval;
    long        *maxval;
{
    int         reverse = FALSE;
    char_u      *first_char;
    long        tmp;

    if (*regparse == '-')
    {
        /* Starts with '-', so reverse the range later */
        regparse++;
        reverse = TRUE;
    }
    first_char = regparse;
    *minval = getdigits(&regparse);
    if (*regparse == ',')           /* There is a comma */
    {
        if (vim_isdigit(*++regparse))
            *maxval = getdigits(&regparse);
        else
            *maxval = MAX_LIMIT;
    }
    else if (VIM_ISDIGIT(*first_char))
        *maxval = *minval;          /* It was \{n} or \{-n} */
    else
        *maxval = MAX_LIMIT;        /* It was \{} or \{-} */
    if (*regparse == '\\')
        regparse++;     /* Allow either \{...} or \{...\} */
    if (*regparse != '}')
    {
        sprintf((char *)IObuff, "E554: Syntax error in %s{...}",
                                          reg_magic == MAGIC_ALL ? "" : "\\");
        EMSG_RET_FAIL(IObuff);
    }

    /*
     * Reverse the range if there was a '-', or make sure it is in the right order otherwise.
     */
    if ((!reverse && *minval > *maxval) || (reverse && *minval < *maxval))
    {
        tmp = *minval;
        *minval = *maxval;
        *maxval = tmp;
    }
    skipchr();          /* let's be friends with the lexer again */
    return OK;
}

/*
 * vim_regexec and friends
 */

/*
 * Global work variables for vim_regexec().
 */

/* The current match-position is remembered with these variables: */
static linenr_T reglnum;        /* line number, relative to first line */
static char_u   *regline;       /* start of current line */
static char_u   *reginput;      /* current input, points into "regline" */

static int      need_clear_subexpr;     /* subexpressions still need to be
                                         * cleared */
static int      need_clear_zsubexpr = FALSE;    /* extmatch subexpressions
                                                 * still need to be cleared */

/*
 * Structure used to save the current input state, when it needs to be
 * restored after trying a match.  Used by reg_save() and reg_restore().
 * Also stores the length of "backpos".
 */
typedef struct
{
    union
    {
        char_u  *ptr;   /* reginput pointer, for single-line regexp */
        lpos_T  pos;    /* reginput pos, for multi-line regexp */
    } rs_u;
    int         rs_len;
} regsave_T;

/* struct to save start/end pointer/position in for \(\) */
typedef struct
{
    union
    {
        char_u  *ptr;
        lpos_T  pos;
    } se_u;
} save_se_T;

/* used for BEHIND and NOBEHIND matching */
typedef struct regbehind_S
{
    regsave_T   save_after;
    regsave_T   save_behind;
    int         save_need_clear_subexpr;
    save_se_T   save_start[NSUBEXP];
    save_se_T   save_end[NSUBEXP];
} regbehind_T;

static char_u   *reg_getline(linenr_T lnum);
static long     bt_regexec_both(char_u *line, colnr_T col, proftime_T *tm);
static long     regtry(bt_regprog_T *prog, colnr_T col);
static void     cleanup_subexpr(void);
static void     cleanup_zsubexpr(void);
static void     save_subexpr(regbehind_T *bp);
static void     restore_subexpr(regbehind_T *bp);
static void     reg_nextline(void);
static void     reg_save(regsave_T *save, garray_T *gap);
static void     reg_restore(regsave_T *save, garray_T *gap);
static int      reg_save_equal(regsave_T *save);
static void     save_se_multi(save_se_T *savep, lpos_T *posp);
static void     save_se_one(save_se_T *savep, char_u **pp);

/* Save the sub-expressions before attempting a match. */
#define save_se(savep, posp, pp) \
    REG_MULTI ? save_se_multi((savep), (posp)) : save_se_one((savep), (pp))

/* After a failed match restore the sub-expressions. */
#define restore_se(savep, posp, pp) { \
    if (REG_MULTI) \
        *(posp) = (savep)->se_u.pos; \
    else \
        *(pp) = (savep)->se_u.ptr; }

static int      re_num_cmp(long_u val, char_u *scan);
static int      match_with_backref(linenr_T start_lnum, colnr_T start_col, linenr_T end_lnum, colnr_T end_col, int *bytelen);
static int      regmatch(char_u *prog);
static int      regrepeat(char_u *p, long maxcount);

/*
 * Internal copy of 'ignorecase'.  It is set at each call to vim_regexec().
 * Normally it gets the value of "rm_ic" or "rmm_ic", but when the pattern
 * contains '\c' or '\C' the value is overruled.
 */
static int      ireg_ic;

/*
 * Similar to ireg_ic, but only for 'combining' characters.  Set with \Z flag
 * in the regexp.  Defaults to false, always.
 */
static int      ireg_icombine;

/*
 * Copy of "rmm_maxcol": maximum column to search for a match.  Zero when
 * there is no maximum.
 */
static colnr_T  ireg_maxcol;

/*
 * Sometimes need to save a copy of a line.  Since alloc()/free() is very
 * slow, we keep one allocated piece of memory and only re-allocate it when
 * it's too small.  It's freed in bt_regexec_both() when finished.
 */
static char_u   *reg_tofree = NULL;
static unsigned reg_tofreelen;

/*
 * These variables are set when executing a regexp to speed up the execution.
 * Which ones are set depends on whether a single-line or multi-line match is
 * done:
 *                      single-line             multi-line
 * reg_match            &regmatch_T             NULL
 * reg_mmatch           NULL                    &regmmatch_T
 * reg_startp           reg_match->startp       <invalid>
 * reg_endp             reg_match->endp         <invalid>
 * reg_startpos         <invalid>               reg_mmatch->startpos
 * reg_endpos           <invalid>               reg_mmatch->endpos
 * reg_win              NULL                    window in which to search
 * reg_buf              curbuf                  buffer in which to search
 * reg_firstlnum        <invalid>               first line in which to search
 * reg_maxline          0                       last line nr
 * reg_line_lbr         FALSE or TRUE           FALSE
 */
static regmatch_T       *reg_match;
static regmmatch_T      *reg_mmatch;
static char_u           **reg_startp = NULL;
static char_u           **reg_endp = NULL;
static lpos_T           *reg_startpos = NULL;
static lpos_T           *reg_endpos = NULL;
static win_T            *reg_win;
static buf_T            *reg_buf;
static linenr_T         reg_firstlnum;
static linenr_T         reg_maxline;
static int              reg_line_lbr;       /* "\n" in string is line break */

/* Values for rs_state in regitem_T. */
typedef enum regstate_E
{
    RS_NOPEN = 0        /* NOPEN and NCLOSE */
    , RS_MOPEN          /* MOPEN + [0-9] */
    , RS_MCLOSE         /* MCLOSE + [0-9] */
    , RS_ZOPEN          /* ZOPEN + [0-9] */
    , RS_ZCLOSE         /* ZCLOSE + [0-9] */
    , RS_BRANCH         /* BRANCH */
    , RS_BRCPLX_MORE    /* BRACE_COMPLEX and trying one more match */
    , RS_BRCPLX_LONG    /* BRACE_COMPLEX and trying longest match */
    , RS_BRCPLX_SHORT   /* BRACE_COMPLEX and trying shortest match */
    , RS_NOMATCH        /* NOMATCH */
    , RS_BEHIND1        /* BEHIND / NOBEHIND matching rest */
    , RS_BEHIND2        /* BEHIND / NOBEHIND matching behind part */
    , RS_STAR_LONG      /* STAR/PLUS/BRACE_SIMPLE longest match */
    , RS_STAR_SHORT     /* STAR/PLUS/BRACE_SIMPLE shortest match */
} regstate_T;

/*
 * When there are alternatives a regstate_T is put on the regstack to remember what we are doing.
 * Before it may be another type of item, depending on rs_state, to remember more things.
 */
typedef struct regitem_S
{
    regstate_T  rs_state;       /* what we are doing, one of RS_ above */
    char_u      *rs_scan;       /* current node in program */
    union
    {
        save_se_T  sesave;
        regsave_T  regsave;
    } rs_un;                    /* room for saving reginput */
    short       rs_no;          /* submatch nr or BEHIND/NOBEHIND */
} regitem_T;

static regitem_T *regstack_push(regstate_T state, char_u *scan);
static void regstack_pop(char_u **scan);

/* used for STAR, PLUS and BRACE_SIMPLE matching */
typedef struct regstar_S
{
    int         nextb;          /* next byte */
    int         nextb_ic;       /* next byte reverse case */
    long        count;
    long        minval;
    long        maxval;
} regstar_T;

/* used to store input position when a BACK was encountered, so that we now if
 * we made any progress since the last time. */
typedef struct backpos_S
{
    char_u      *bp_scan;       /* "scan" where BACK was encountered */
    regsave_T   bp_pos;         /* last input position */
} backpos_T;

/*
 * "regstack" and "backpos" are used by regmatch().  They are kept over calls
 * to avoid invoking malloc() and free() often.
 * "regstack" is a stack with regitem_T items, sometimes preceded by regstar_T
 * or regbehind_T.
 * "backpos_T" is a table with backpos_T for BACK
 */
static garray_T regstack = {0, 0, 0, 0, NULL};
static garray_T backpos = {0, 0, 0, 0, NULL};

/*
 * Both for regstack and backpos tables we use the following strategy of
 * allocation (to reduce malloc/free calls):
 * - Initial size is fairly small.
 * - When needed, the tables are grown bigger (8 times at first, double after that).
 * - After executing the match we free the memory only if the array has grown.
 *   Thus the memory is kept allocated when it's at the initial size.
 * This makes it fast while not keeping a lot of memory allocated.
 * A three times speed increase was observed when using many simple patterns.
 */
#define REGSTACK_INITIAL        2048
#define BACKPOS_INITIAL         64

/*
 * Get pointer to the line "lnum", which is relative to "reg_firstlnum".
 */
    static char_u *
reg_getline(lnum)
    linenr_T    lnum;
{
    /* when looking behind for a match/no-match lnum is negative.  But we
     * can't go before line 1 */
    if (reg_firstlnum + lnum < 1)
        return NULL;
    if (lnum > reg_maxline)
        /* Must have matched the "\n" in the last line. */
        return (char_u *)"";

    return ml_get_buf(reg_buf, reg_firstlnum + lnum, FALSE);
}

static regsave_T behind_pos;

static char_u   *reg_startzp[NSUBEXP];  /* Workspace to mark beginning */
static char_u   *reg_endzp[NSUBEXP];    /*   and end of \z(...\) matches */
static lpos_T   reg_startzpos[NSUBEXP]; /* idem, beginning pos */
static lpos_T   reg_endzpos[NSUBEXP];   /* idem, end pos */

/* TRUE if using multi-line regexp. */
#define REG_MULTI       (reg_match == NULL)

static int  bt_regexec_nl(regmatch_T *rmp, char_u *line, colnr_T col, int line_lbr);

/*
 * Match a regexp against a string.
 * "rmp->regprog" is a compiled regexp as returned by vim_regcomp().
 * Uses curbuf for line count and 'iskeyword'.
 * if "line_lbr" is TRUE  consider a "\n" in "line" to be a line break.
 *
 * Returns 0 for failure, number of lines contained in the match otherwise.
 */
    static int
bt_regexec_nl(rmp, line, col, line_lbr)
    regmatch_T  *rmp;
    char_u      *line;  /* string to match against */
    colnr_T     col;    /* column to start looking for match */
    int         line_lbr;
{
    reg_match = rmp;
    reg_mmatch = NULL;
    reg_maxline = 0;
    reg_line_lbr = line_lbr;
    reg_buf = curbuf;
    reg_win = NULL;
    ireg_ic = rmp->rm_ic;
    ireg_icombine = FALSE;
    ireg_maxcol = 0;

    return bt_regexec_both(line, col, NULL);
}

static long bt_regexec_multi(regmmatch_T *rmp, win_T *win, buf_T *buf, linenr_T lnum, colnr_T col, proftime_T *tm);

/*
 * Match a regexp against multiple lines.
 * "rmp->regprog" is a compiled regexp as returned by vim_regcomp().
 * Uses curbuf for line count and 'iskeyword'.
 *
 * Return zero if there is no match.  Return number of lines contained in the match otherwise.
 */
    static long
bt_regexec_multi(rmp, win, buf, lnum, col, tm)
    regmmatch_T *rmp;
    win_T       *win;           /* window in which to search or NULL */
    buf_T       *buf;           /* buffer in which to search */
    linenr_T    lnum;           /* nr of line to start looking for match */
    colnr_T     col;            /* column to start looking for match */
    proftime_T  *tm;            /* timeout limit or NULL */
{
    reg_match = NULL;
    reg_mmatch = rmp;
    reg_buf = buf;
    reg_win = win;
    reg_firstlnum = lnum;
    reg_maxline = reg_buf->b_ml.ml_line_count - lnum;
    reg_line_lbr = FALSE;
    ireg_ic = rmp->rmm_ic;
    ireg_icombine = FALSE;
    ireg_maxcol = rmp->rmm_maxcol;

    return bt_regexec_both(NULL, col, tm);
}

/*
 * Match a regexp against a string ("line" points to the string) or multiple
 * lines ("line" is NULL, use reg_getline()).
 * Returns 0 for failure, number of lines contained in the match otherwise.
 */
    static long
bt_regexec_both(line, col, tm)
    char_u      *line;
    colnr_T     col;            /* column to start looking for match */
    proftime_T  *tm UNUSED;     /* timeout limit or NULL */
{
    bt_regprog_T    *prog;
    char_u          *s;
    long            retval = 0L;

    /* Create "regstack" and "backpos" if they are not allocated yet.
     * We allocate *_INITIAL amount of bytes first and then set the grow size
     * to much bigger value to avoid many malloc calls in case of deep regular
     * expressions. */
    if (regstack.ga_data == NULL)
    {
        /* Use an item size of 1 byte, since we push different things
         * onto the regstack. */
        ga_init2(&regstack, 1, REGSTACK_INITIAL);
        ga_grow(&regstack, REGSTACK_INITIAL);
        regstack.ga_growsize = REGSTACK_INITIAL * 8;
    }

    if (backpos.ga_data == NULL)
    {
        ga_init2(&backpos, sizeof(backpos_T), BACKPOS_INITIAL);
        ga_grow(&backpos, BACKPOS_INITIAL);
        backpos.ga_growsize = BACKPOS_INITIAL * 8;
    }

    if (REG_MULTI)
    {
        prog = (bt_regprog_T *)reg_mmatch->regprog;
        line = reg_getline((linenr_T)0);
        reg_startpos = reg_mmatch->startpos;
        reg_endpos = reg_mmatch->endpos;
    }
    else
    {
        prog = (bt_regprog_T *)reg_match->regprog;
        reg_startp = reg_match->startp;
        reg_endp = reg_match->endp;
    }

    /* Be paranoid... */
    if (prog == NULL || line == NULL)
    {
        EMSG((char *)e_null);
        goto theend;
    }

    /* Check validity of program. */
    if (prog_magic_wrong())
        goto theend;

    /* If the start column is past the maximum column: no need to try. */
    if (ireg_maxcol > 0 && col >= ireg_maxcol)
        goto theend;

    /* If pattern contains "\c" or "\C": overrule value of ireg_ic */
    if (prog->regflags & RF_ICASE)
        ireg_ic = TRUE;
    else if (prog->regflags & RF_NOICASE)
        ireg_ic = FALSE;

    /* If pattern contains "\Z" overrule value of ireg_icombine */
    if (prog->regflags & RF_ICOMBINE)
        ireg_icombine = TRUE;

    /* If there is a "must appear" string, look for it. */
    if (prog->regmust != NULL)
    {
        int c;

        c = utf_ptr2char(prog->regmust);
        s = line + col;

        /*
         * This is used very often, esp. for ":global".  Use three versions of
         * the loop to avoid overhead of conditions.
         */
        if (!ireg_ic)
            while ((s = vim_strchr(s, c)) != NULL)
            {
                if (cstrncmp(s, prog->regmust, &prog->regmlen) == 0)
                    break;              /* Found it. */
                s += utfc_ptr2len(s);
            }
        else
            while ((s = cstrchr(s, c)) != NULL)
            {
                if (cstrncmp(s, prog->regmust, &prog->regmlen) == 0)
                    break;              /* Found it. */
                s += utfc_ptr2len(s);
            }
        if (s == NULL)          /* Not present. */
            goto theend;
    }

    regline = line;
    reglnum = 0;
    reg_toolong = FALSE;

    /* Simplest case: Anchored match need be tried only once. */
    if (prog->reganch)
    {
        int     c;

        c = utf_ptr2char(regline + col);
        if (prog->regstart == NUL
                || prog->regstart == c
                || (ireg_ic && ((utf_fold(prog->regstart) == utf_fold(c))
                        || (c < 255 && prog->regstart < 255 &&
                            vim_tolower(prog->regstart) == vim_tolower(c)))))
            retval = regtry(prog, col);
        else
            retval = 0;
    }
    else
    {
        int tm_count = 0;
        /* Messy cases:  unanchored match. */
        while (!got_int)
        {
            if (prog->regstart != NUL)
            {
                /* Skip until the char we know it must start with.
                 * Used often, do some work to avoid call overhead. */
                s = cstrchr(regline + col, prog->regstart);
                if (s == NULL)
                {
                    retval = 0;
                    break;
                }
                col = (int)(s - regline);
            }

            /* Check for maximum column to try. */
            if (ireg_maxcol > 0 && col >= ireg_maxcol)
            {
                retval = 0;
                break;
            }

            retval = regtry(prog, col);
            if (retval > 0)
                break;

            /* if not currently on the first line, get it again */
            if (reglnum != 0)
            {
                reglnum = 0;
                regline = reg_getline((linenr_T)0);
            }
            if (regline[col] == NUL)
                break;
            col += utfc_ptr2len(regline + col);
            /* Check for timeout once in a twenty times to avoid overhead. */
            if (tm != NULL && ++tm_count == 20)
            {
                tm_count = 0;
                if (profile_passed_limit(tm))
                    break;
            }
        }
    }

theend:
    /* Free "reg_tofree" when it's a bit big.
     * Free regstack and backpos if they are bigger than their initial size. */
    if (reg_tofreelen > 400)
    {
        vim_free(reg_tofree);
        reg_tofree = NULL;
    }
    if (regstack.ga_maxlen > REGSTACK_INITIAL)
        ga_clear(&regstack);
    if (backpos.ga_maxlen > BACKPOS_INITIAL)
        ga_clear(&backpos);

    return retval;
}

static reg_extmatch_T *make_extmatch(void);

/*
 * Create a new extmatch and mark it as referenced once.
 */
    static reg_extmatch_T *
make_extmatch()
{
    reg_extmatch_T      *em;

    em = (reg_extmatch_T *)alloc_clear((unsigned)sizeof(reg_extmatch_T));
    if (em != NULL)
        em->refcnt = 1;
    return em;
}

/*
 * Add a reference to an extmatch.
 */
    reg_extmatch_T *
ref_extmatch(em)
    reg_extmatch_T      *em;
{
    if (em != NULL)
        em->refcnt++;
    return em;
}

/*
 * Remove a reference to an extmatch.  If there are no references left, free the info.
 */
    void
unref_extmatch(em)
    reg_extmatch_T      *em;
{
    int i;

    if (em != NULL && --em->refcnt <= 0)
    {
        for (i = 0; i < NSUBEXP; ++i)
            vim_free(em->matches[i]);
        vim_free(em);
    }
}

/*
 * regtry - try match of "prog" with at regline["col"].
 * Returns 0 for failure, number of lines contained in the match otherwise.
 */
    static long
regtry(prog, col)
    bt_regprog_T    *prog;
    colnr_T     col;
{
    reginput = regline + col;
    need_clear_subexpr = TRUE;
    /* Clear the external match subpointers if necessary. */
    if (prog->reghasz == REX_SET)
        need_clear_zsubexpr = TRUE;

    if (regmatch(prog->program + 1) == 0)
        return 0;

    cleanup_subexpr();
    if (REG_MULTI)
    {
        if (reg_startpos[0].lnum < 0)
        {
            reg_startpos[0].lnum = 0;
            reg_startpos[0].col = col;
        }
        if (reg_endpos[0].lnum < 0)
        {
            reg_endpos[0].lnum = reglnum;
            reg_endpos[0].col = (int)(reginput - regline);
        }
        else
            /* Use line number of "\ze". */
            reglnum = reg_endpos[0].lnum;
    }
    else
    {
        if (reg_startp[0] == NULL)
            reg_startp[0] = regline + col;
        if (reg_endp[0] == NULL)
            reg_endp[0] = reginput;
    }
    /* Package any found \z(...\) matches for export. Default is none. */
    unref_extmatch(re_extmatch_out);
    re_extmatch_out = NULL;

    if (prog->reghasz == REX_SET)
    {
        int             i;

        cleanup_zsubexpr();
        re_extmatch_out = make_extmatch();
        for (i = 0; i < NSUBEXP; i++)
        {
            if (REG_MULTI)
            {
                /* Only accept single line matches. */
                if (reg_startzpos[i].lnum >= 0
                        && reg_endzpos[i].lnum == reg_startzpos[i].lnum
                        && reg_endzpos[i].col >= reg_startzpos[i].col)
                    re_extmatch_out->matches[i] =
                        vim_strnsave(reg_getline(reg_startzpos[i].lnum) + reg_startzpos[i].col,
                                   reg_endzpos[i].col - reg_startzpos[i].col);
            }
            else
            {
                if (reg_startzp[i] != NULL && reg_endzp[i] != NULL)
                    re_extmatch_out->matches[i] =
                            vim_strnsave(reg_startzp[i], (int)(reg_endzp[i] - reg_startzp[i]));
            }
        }
    }
    return 1 + reglnum;
}

static int reg_prev_class(void);

/*
 * Get class of previous character.
 */
    static int
reg_prev_class()
{
    if (reginput > regline)
        return mb_get_class_buf(reginput - 1 - utf_head_off(regline, reginput - 1), reg_buf);

    return -1;
}

static int reg_match_visual(void);

/*
 * Return TRUE if the current reginput position matches the Visual area.
 */
    static int
reg_match_visual()
{
    pos_T       top, bot;
    linenr_T    lnum;
    colnr_T     col;
    win_T       *wp = (reg_win == NULL) ? curwin : reg_win;
    int         mode;
    colnr_T     start, end;
    colnr_T     start2, end2;
    colnr_T     cols;

    /* Check if the buffer is the current buffer. */
    if (reg_buf != curbuf || VIsual.lnum == 0)
        return FALSE;

    if (VIsual_active)
    {
        if (lt(VIsual, wp->w_cursor))
        {
            top = VIsual;
            bot = wp->w_cursor;
        }
        else
        {
            top = wp->w_cursor;
            bot = VIsual;
        }
        mode = VIsual_mode;
    }
    else
    {
        if (lt(curbuf->b_visual.vi_start, curbuf->b_visual.vi_end))
        {
            top = curbuf->b_visual.vi_start;
            bot = curbuf->b_visual.vi_end;
        }
        else
        {
            top = curbuf->b_visual.vi_end;
            bot = curbuf->b_visual.vi_start;
        }
        mode = curbuf->b_visual.vi_mode;
    }
    lnum = reglnum + reg_firstlnum;
    if (lnum < top.lnum || lnum > bot.lnum)
        return FALSE;

    if (mode == 'v')
    {
        col = (colnr_T)(reginput - regline);
        if ((lnum == top.lnum && col < top.col) || (lnum == bot.lnum && col >= bot.col + (*p_sel != 'e')))
            return FALSE;
    }
    else if (mode == Ctrl_V)
    {
        getvvcol(wp, &top, &start, NULL, &end);
        getvvcol(wp, &bot, &start2, NULL, &end2);
        if (start2 < start)
            start = start2;
        if (end2 > end)
            end = end2;
        if (top.col == MAXCOL || bot.col == MAXCOL)
            end = MAXCOL;
        cols = win_linetabsize(wp, regline, (colnr_T)(reginput - regline));
        if (cols < start || cols > end - (*p_sel == 'e'))
            return FALSE;
    }
    return TRUE;
}

/*
 * The arguments from BRACE_LIMITS are stored here.  They are actually local
 * to regmatch(), but they are here to reduce the amount of stack space used
 * (it can be called recursively many times).
 */
static long     bl_minval;
static long     bl_maxval;

/*
 * regmatch - main matching routine
 *
 * Conceptually the strategy is simple: Check to see whether the current node
 * matches, push an item onto the regstack and loop to see whether the rest
 * matches, and then act accordingly.  In practice we make some effort to
 * avoid using the regstack, in particular by going through "ordinary" nodes
 * (that don't need to know whether the rest of the match failed) by a nested loop.
 *
 * Returns TRUE when there is a match.  Leaves reginput and reglnum just after
 * the last matched character.
 * Returns FALSE when there is no match.  Leaves reginput and reglnum in an
 * undefined state!
 */
    static int
regmatch(scan)
    char_u      *scan;          /* Current node. */
{
  char_u        *next;          /* Next node. */
  int           op;
  int           c;
  regitem_T     *rp;
  int           no;
  int           status;         /* one of the RA_ values: */
#define RA_FAIL         1       /* something failed, abort */
#define RA_CONT         2       /* continue in inner loop */
#define RA_BREAK        3       /* break inner loop */
#define RA_MATCH        4       /* successful match */
#define RA_NOMATCH      5       /* didn't match */

  /* Make "regstack" and "backpos" empty.  They are allocated and freed in
   * bt_regexec_both() to reduce malloc()/free() calls. */
  regstack.ga_len = 0;
  backpos.ga_len = 0;

  /*
   * Repeat until "regstack" is empty.
   */
  for (;;)
  {
    /* Some patterns may take a long time to match, e.g., "\([a-z]\+\)\+Q".
     * Allow interrupting them with CTRL-C. */
    fast_breakcheck();

    /*
     * Repeat for items that can be matched sequentially, without using the regstack.
     */
    for (;;)
    {
        if (got_int || scan == NULL)
        {
            status = RA_FAIL;
            break;
        }
        status = RA_CONT;

        next = regnext(scan);

        op = OP(scan);
        /* Check for character class with NL added. */
        if (!reg_line_lbr && WITH_NL(op) && REG_MULTI && *reginput == NUL && reglnum <= reg_maxline)
        {
            reg_nextline();
        }
        else if (reg_line_lbr && WITH_NL(op) && *reginput == '\n')
        {
            reginput += utfc_ptr2len(reginput);
        }
        else
        {
            if (WITH_NL(op))
                op -= ADD_NL;
            c = utf_ptr2char(reginput);
            switch (op)
            {
            case BOL:
                if (reginput != regline)
                    status = RA_NOMATCH;
                break;

            case EOL:
                if (c != NUL)
                    status = RA_NOMATCH;
                break;

            case RE_BOF:
                /* We're not at the beginning of the file when below the first
                 * line where we started, not at the start of the line or we
                 * didn't start at the first line of the buffer. */
                if (reglnum != 0 || reginput != regline || (REG_MULTI && reg_firstlnum > 1))
                    status = RA_NOMATCH;
                break;

            case RE_EOF:
                if (reglnum != reg_maxline || c != NUL)
                    status = RA_NOMATCH;
                break;

            case CURSOR:
                /* Check if the buffer is in a window and compare the
                 * reg_win->w_cursor position to the match position. */
                if (reg_win == NULL
                        || (reglnum + reg_firstlnum != reg_win->w_cursor.lnum)
                        || ((colnr_T)(reginput - regline) != reg_win->w_cursor.col))
                    status = RA_NOMATCH;
                break;

            case RE_MARK:
                /* Compare the mark position to the match position. */
                {
                    int     mark = OPERAND(scan)[0];
                    int     cmp = OPERAND(scan)[1];
                    pos_T   *pos;

                    pos = getmark_buf(reg_buf, mark, FALSE);
                    if (pos == NULL              /* mark doesn't exist */
                            || pos->lnum <= 0    /* mark isn't set in reg_buf */
                            || (pos->lnum == reglnum + reg_firstlnum
                                    ? (pos->col == (colnr_T)(reginput - regline)
                                        ? (cmp == '<' || cmp == '>')
                                        : (pos->col < (colnr_T)(reginput - regline)
                                            ? cmp != '>'
                                            : cmp != '<'))
                                    : (pos->lnum < reglnum + reg_firstlnum
                                        ? cmp != '>'
                                        : cmp != '<')))
                        status = RA_NOMATCH;
                }
                break;

            case RE_VISUAL:
                if (!reg_match_visual())
                    status = RA_NOMATCH;
                break;

            case RE_LNUM:
                if (!REG_MULTI || !re_num_cmp((long_u)(reglnum + reg_firstlnum), scan))
                    status = RA_NOMATCH;
                break;

            case RE_COL:
                if (!re_num_cmp((long_u)(reginput - regline) + 1, scan))
                    status = RA_NOMATCH;
                break;

            case RE_VCOL:
                if (!re_num_cmp((long_u)win_linetabsize(
                                (reg_win == NULL) ? curwin : reg_win,
                                regline, (colnr_T)(reginput - regline)) + 1, scan))
                    status = RA_NOMATCH;
                break;

            case BOW:     /* \<word; reginput points to w */
                if (c == NUL)       /* Can't match at end of line */
                    status = RA_NOMATCH;
                else
                {
                    int this_class;

                    /* Get class of current and previous char (if it exists). */
                    this_class = mb_get_class_buf(reginput, reg_buf);
                    if (this_class <= 1)
                        status = RA_NOMATCH;  /* not on a word at all */
                    else if (reg_prev_class() == this_class)
                        status = RA_NOMATCH;  /* previous char is in same word */
                }
                break;

            case EOW:     /* word\>; reginput points after d */
                if (reginput == regline)    /* Can't match at start of line */
                    status = RA_NOMATCH;
                else
                {
                    int this_class, prev_class;

                    /* Get class of current and previous char (if it exists). */
                    this_class = mb_get_class_buf(reginput, reg_buf);
                    prev_class = reg_prev_class();
                    if (this_class == prev_class || prev_class == 0 || prev_class == 1)
                        status = RA_NOMATCH;
                }
                break; /* Matched with EOW */

            case ANY:
                /* ANY does not match new lines. */
                if (c == NUL)
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case IDENT:
                if (!vim_isIDc(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case SIDENT:
                if (VIM_ISDIGIT(*reginput) || !vim_isIDc(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case KWORD:
                if (!vim_iswordp_buf(reginput, reg_buf))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case SKWORD:
                if (VIM_ISDIGIT(*reginput) || !vim_iswordp_buf(reginput, reg_buf))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case FNAME:
                if (!vim_isfilec(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case SFNAME:
                if (VIM_ISDIGIT(*reginput) || !vim_isfilec(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case PRINT:
                if (!vim_isprintc(utf_ptr2char(reginput)))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case SPRINT:
                if (VIM_ISDIGIT(*reginput) || !vim_isprintc(utf_ptr2char(reginput)))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case WHITE:
                if (!vim_iswhite(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case NWHITE:
                if (c == NUL || vim_iswhite(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case DIGIT:
                if (!ri_digit(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case NDIGIT:
                if (c == NUL || ri_digit(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case HEX:
                if (!ri_hex(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case NHEX:
                if (c == NUL || ri_hex(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case OCTAL:
                if (!ri_octal(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case NOCTAL:
                if (c == NUL || ri_octal(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case WORD:
                if (!ri_word(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case NWORD:
                if (c == NUL || ri_word(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case HEAD:
                if (!ri_head(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case NHEAD:
                if (c == NUL || ri_head(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case ALPHA:
                if (!ri_alpha(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case NALPHA:
                if (c == NUL || ri_alpha(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case LOWER:
                if (!ri_lower(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case NLOWER:
                if (c == NUL || ri_lower(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case UPPER:
                if (!ri_upper(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case NUPPER:
                if (c == NUL || ri_upper(c))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case EXACTLY:
                {
                    int     len;
                    char_u  *opnd;

                    opnd = OPERAND(scan);
                    /* Inline the first byte, for speed. */
                    if (*opnd != *reginput && !ireg_ic)
                        status = RA_NOMATCH;
                    else if (*opnd == NUL)
                    {
                        /* match empty string always works; happens when "~" is empty. */
                    }
                    else
                    {
                        if (opnd[1] == NUL && !ireg_ic)
                        {
                            len = 1;        /* matched a single byte above */
                        }
                        else
                        {
                            /* Need to match first byte again for multi-byte. */
                            len = (int)STRLEN(opnd);
                            if (cstrncmp(opnd, reginput, &len) != 0)
                                status = RA_NOMATCH;
                        }
                        /* Check for following composing character, unless %C
                         * follows (skips over all composing chars). */
                        if (status != RA_NOMATCH
                                && UTF_COMPOSINGLIKE(reginput, reginput + len)
                                && !ireg_icombine
                                && OP(next) != RE_COMPOSING)
                        {
                            /* raaron: This code makes a composing character get
                             * ignored, which is the correct behavior (sometimes)
                             * for voweled Hebrew texts. */
                            status = RA_NOMATCH;
                        }
                        if (status != RA_NOMATCH)
                            reginput += len;
                    }
                }
                break;

            case ANYOF:
            case ANYBUT:
                if (c == NUL)
                    status = RA_NOMATCH;
                else if ((cstrchr(OPERAND(scan), c) == NULL) == (op == ANYOF))
                    status = RA_NOMATCH;
                else
                    reginput += utfc_ptr2len(reginput);
                break;

            case MULTIBYTECODE:
                {
                    int     i, len;
                    char_u  *opnd;
                    int     opndc = 0, inpc;

                    opnd = OPERAND(scan);
                    /* Safety check (just in case 'encoding' was changed since
                     * compiling the program). */
                    if ((len = utfc_ptr2len(opnd)) < 2)
                    {
                        status = RA_NOMATCH;
                        break;
                    }
                    opndc = utf_ptr2char(opnd);
                    if (utf_iscomposing(opndc))
                    {
                        /* When only a composing char is given match at any
                         * position where that composing char appears. */
                        status = RA_NOMATCH;
                        for (i = 0; reginput[i] != NUL; i += utf_ptr2len(reginput + i))
                        {
                            inpc = utf_ptr2char(reginput + i);
                            if (!utf_iscomposing(inpc))
                            {
                                if (i > 0)
                                    break;
                            }
                            else if (opndc == inpc)
                            {
                                /* Include all following composing chars. */
                                len = i + utfc_ptr2len(reginput + i);
                                status = RA_MATCH;
                                break;
                            }
                        }
                    }
                    else
                        for (i = 0; i < len; ++i)
                            if (opnd[i] != reginput[i])
                            {
                                status = RA_NOMATCH;
                                break;
                            }
                    reginput += len;
                }
                break;
            case RE_COMPOSING:
                {
                    /* Skip composing characters. */
                    while (utf_iscomposing(utf_ptr2char(reginput)))
                        reginput += utf_ptr2len(reginput);
                }
                break;

            case NOTHING:
                break;

            case BACK:
                {
                    int             i;
                    backpos_T       *bp;

                    /*
                     * When we run into BACK we need to check if we don't keep
                     * looping without matching any input.  The second and later
                     * times a BACK is encountered it fails if the input is still
                     * at the same position as the previous time.
                     * The positions are stored in "backpos" and found by the
                     * current value of "scan", the position in the RE program.
                     */
                    bp = (backpos_T *)backpos.ga_data;
                    for (i = 0; i < backpos.ga_len; ++i)
                        if (bp[i].bp_scan == scan)
                            break;
                    if (i == backpos.ga_len)
                    {
                        /* First time at this BACK, make room to store the pos. */
                        if (ga_grow(&backpos, 1) == FAIL)
                            status = RA_FAIL;
                        else
                        {
                            /* get "ga_data" again, it may have changed */
                            bp = (backpos_T *)backpos.ga_data;
                            bp[i].bp_scan = scan;
                            ++backpos.ga_len;
                        }
                    }
                    else if (reg_save_equal(&bp[i].bp_pos))
                        /* Still at same position as last time, fail. */
                        status = RA_NOMATCH;

                    if (status != RA_FAIL && status != RA_NOMATCH)
                        reg_save(&bp[i].bp_pos, &backpos);
                }
                break;

            case MOPEN + 0:   /* Match start: \zs */
            case MOPEN + 1:   /* \( */
            case MOPEN + 2:
            case MOPEN + 3:
            case MOPEN + 4:
            case MOPEN + 5:
            case MOPEN + 6:
            case MOPEN + 7:
            case MOPEN + 8:
            case MOPEN + 9:
                {
                    no = op - MOPEN;
                    cleanup_subexpr();
                    rp = regstack_push(RS_MOPEN, scan);
                    if (rp == NULL)
                        status = RA_FAIL;
                    else
                    {
                        rp->rs_no = no;
                        save_se(&rp->rs_un.sesave, &reg_startpos[no], &reg_startp[no]);
                        /* We simply continue and handle the result when done. */
                    }
                }
                break;

            case NOPEN:       /* \%( */
            case NCLOSE:      /* \) after \%( */
                    if (regstack_push(RS_NOPEN, scan) == NULL)
                        status = RA_FAIL;
                    /* We simply continue and handle the result when done. */
                    break;

            case ZOPEN + 1:
            case ZOPEN + 2:
            case ZOPEN + 3:
            case ZOPEN + 4:
            case ZOPEN + 5:
            case ZOPEN + 6:
            case ZOPEN + 7:
            case ZOPEN + 8:
            case ZOPEN + 9:
                {
                    no = op - ZOPEN;
                    cleanup_zsubexpr();
                    rp = regstack_push(RS_ZOPEN, scan);
                    if (rp == NULL)
                        status = RA_FAIL;
                    else
                    {
                        rp->rs_no = no;
                        save_se(&rp->rs_un.sesave, &reg_startzpos[no], &reg_startzp[no]);
                        /* We simply continue and handle the result when done. */
                    }
                }
                break;

            case MCLOSE + 0:  /* Match end: \ze */
            case MCLOSE + 1:  /* \) */
            case MCLOSE + 2:
            case MCLOSE + 3:
            case MCLOSE + 4:
            case MCLOSE + 5:
            case MCLOSE + 6:
            case MCLOSE + 7:
            case MCLOSE + 8:
            case MCLOSE + 9:
                {
                    no = op - MCLOSE;
                    cleanup_subexpr();
                    rp = regstack_push(RS_MCLOSE, scan);
                    if (rp == NULL)
                        status = RA_FAIL;
                    else
                    {
                        rp->rs_no = no;
                        save_se(&rp->rs_un.sesave, &reg_endpos[no], &reg_endp[no]);
                        /* We simply continue and handle the result when done. */
                    }
                }
                break;

            case ZCLOSE + 1:  /* \) after \z( */
            case ZCLOSE + 2:
            case ZCLOSE + 3:
            case ZCLOSE + 4:
            case ZCLOSE + 5:
            case ZCLOSE + 6:
            case ZCLOSE + 7:
            case ZCLOSE + 8:
            case ZCLOSE + 9:
                {
                    no = op - ZCLOSE;
                    cleanup_zsubexpr();
                    rp = regstack_push(RS_ZCLOSE, scan);
                    if (rp == NULL)
                        status = RA_FAIL;
                    else
                    {
                        rp->rs_no = no;
                        save_se(&rp->rs_un.sesave, &reg_endzpos[no], &reg_endzp[no]);
                        /* We simply continue and handle the result when done. */
                    }
                }
                break;

            case BACKREF + 1:
            case BACKREF + 2:
            case BACKREF + 3:
            case BACKREF + 4:
            case BACKREF + 5:
            case BACKREF + 6:
            case BACKREF + 7:
            case BACKREF + 8:
            case BACKREF + 9:
                {
                    int             len;

                    no = op - BACKREF;
                    cleanup_subexpr();
                    if (!REG_MULTI)         /* Single-line regexp */
                    {
                        if (reg_startp[no] == NULL || reg_endp[no] == NULL)
                        {
                            /* Backref was not set: Match an empty string. */
                            len = 0;
                        }
                        else
                        {
                            /* Compare current input with back-ref in the same line. */
                            len = (int)(reg_endp[no] - reg_startp[no]);
                            if (cstrncmp(reg_startp[no], reginput, &len) != 0)
                                status = RA_NOMATCH;
                        }
                    }
                    else                            /* Multi-line regexp */
                    {
                        if (reg_startpos[no].lnum < 0 || reg_endpos[no].lnum < 0)
                        {
                            /* Backref was not set: Match an empty string. */
                            len = 0;
                        }
                        else
                        {
                            if (reg_startpos[no].lnum == reglnum && reg_endpos[no].lnum == reglnum)
                            {
                                /* Compare back-ref within the current line. */
                                len = reg_endpos[no].col - reg_startpos[no].col;
                                if (cstrncmp(regline + reg_startpos[no].col, reginput, &len) != 0)
                                    status = RA_NOMATCH;
                            }
                            else
                            {
                                /* Messy situation: Need to compare between two lines. */
                                int r = match_with_backref(
                                                reg_startpos[no].lnum,
                                                reg_startpos[no].col,
                                                reg_endpos[no].lnum,
                                                reg_endpos[no].col,
                                                &len);

                                if (r != RA_MATCH)
                                    status = r;
                            }
                        }
                    }

                    /* Matched the backref, skip over it. */
                    reginput += len;
                }
                break;

            case ZREF + 1:
            case ZREF + 2:
            case ZREF + 3:
            case ZREF + 4:
            case ZREF + 5:
            case ZREF + 6:
            case ZREF + 7:
            case ZREF + 8:
            case ZREF + 9:
                {
                    int     len;

                    cleanup_zsubexpr();
                    no = op - ZREF;
                    if (re_extmatch_in != NULL && re_extmatch_in->matches[no] != NULL)
                    {
                        len = (int)STRLEN(re_extmatch_in->matches[no]);
                        if (cstrncmp(re_extmatch_in->matches[no], reginput, &len) != 0)
                            status = RA_NOMATCH;
                        else
                            reginput += len;
                    }
                    else
                    {
                        /* Backref was not set: Match an empty string. */
                    }
                }
                break;

            case BRANCH:
                {
                    if (OP(next) != BRANCH) /* No choice. */
                        next = OPERAND(scan);       /* Avoid recursion. */
                    else
                    {
                        rp = regstack_push(RS_BRANCH, scan);
                        if (rp == NULL)
                            status = RA_FAIL;
                        else
                            status = RA_BREAK;      /* rest is below */
                    }
                }
                break;

            case BRACE_LIMITS:
                {
                    if (OP(next) == BRACE_SIMPLE)
                    {
                        bl_minval = OPERAND_MIN(scan);
                        bl_maxval = OPERAND_MAX(scan);
                    }
                    else if (OP(next) >= BRACE_COMPLEX && OP(next) < BRACE_COMPLEX + 10)
                    {
                        no = OP(next) - BRACE_COMPLEX;
                        brace_min[no] = OPERAND_MIN(scan);
                        brace_max[no] = OPERAND_MAX(scan);
                        brace_count[no] = 0;
                    }
                    else
                    {
                        EMSG((char *)e_internal);            /* Shouldn't happen */
                        status = RA_FAIL;
                    }
                }
                break;

            case BRACE_COMPLEX + 0:
            case BRACE_COMPLEX + 1:
            case BRACE_COMPLEX + 2:
            case BRACE_COMPLEX + 3:
            case BRACE_COMPLEX + 4:
            case BRACE_COMPLEX + 5:
            case BRACE_COMPLEX + 6:
            case BRACE_COMPLEX + 7:
            case BRACE_COMPLEX + 8:
            case BRACE_COMPLEX + 9:
                {
                    no = op - BRACE_COMPLEX;
                    ++brace_count[no];

                    /* If not matched enough times yet, try one more */
                    if (brace_count[no] <= (brace_min[no] <= brace_max[no] ? brace_min[no] : brace_max[no]))
                    {
                        rp = regstack_push(RS_BRCPLX_MORE, scan);
                        if (rp == NULL)
                            status = RA_FAIL;
                        else
                        {
                            rp->rs_no = no;
                            reg_save(&rp->rs_un.regsave, &backpos);
                            next = OPERAND(scan);
                            /* We continue and handle the result when done. */
                        }
                        break;
                    }

                    /* If matched enough times, may try matching some more */
                    if (brace_min[no] <= brace_max[no])
                    {
                        /* Range is the normal way around, use longest match */
                        if (brace_count[no] <= brace_max[no])
                        {
                            rp = regstack_push(RS_BRCPLX_LONG, scan);
                            if (rp == NULL)
                                status = RA_FAIL;
                            else
                            {
                                rp->rs_no = no;
                                reg_save(&rp->rs_un.regsave, &backpos);
                                next = OPERAND(scan);
                                /* We continue and handle the result when done. */
                            }
                        }
                    }
                    else
                    {
                        /* Range is backwards, use shortest match first */
                        if (brace_count[no] <= brace_min[no])
                        {
                            rp = regstack_push(RS_BRCPLX_SHORT, scan);
                            if (rp == NULL)
                                status = RA_FAIL;
                            else
                            {
                                reg_save(&rp->rs_un.regsave, &backpos);
                                /* We continue and handle the result when done. */
                            }
                        }
                    }
                }
                break;

            case BRACE_SIMPLE:
            case STAR:
            case PLUS:
                {
                    regstar_T       rst;

                    /*
                     * Lookahead to avoid useless match attempts when we know
                     * what character comes next.
                     */
                    if (OP(next) == EXACTLY)
                    {
                        rst.nextb = *OPERAND(next);
                        if (ireg_ic)
                        {
                            if (vim_isupper(rst.nextb))
                                rst.nextb_ic = vim_tolower(rst.nextb);
                            else
                                rst.nextb_ic = vim_toupper(rst.nextb);
                        }
                        else
                            rst.nextb_ic = rst.nextb;
                    }
                    else
                    {
                        rst.nextb = NUL;
                        rst.nextb_ic = NUL;
                    }
                    if (op != BRACE_SIMPLE)
                    {
                        rst.minval = (op == STAR) ? 0 : 1;
                        rst.maxval = MAX_LIMIT;
                    }
                    else
                    {
                        rst.minval = bl_minval;
                        rst.maxval = bl_maxval;
                    }

                    /*
                     * When maxval > minval, try matching as much as possible, up
                     * to maxval.  When maxval < minval, try matching at least the
                     * minimal number (since the range is backwards, that's also maxval!).
                     */
                    rst.count = regrepeat(OPERAND(scan), rst.maxval);
                    if (got_int)
                    {
                        status = RA_FAIL;
                        break;
                    }
                    if (rst.minval <= rst.maxval ? rst.count >= rst.minval : rst.count >= rst.maxval)
                    {
                        /* It could match.  Prepare for trying to match what
                         * follows.  The code is below.  Parameters are stored in
                         * a regstar_T on the regstack. */
                        if ((long)((unsigned)regstack.ga_len >> 10) >= p_mmp)
                        {
                            EMSG((char *)e_maxmempat);
                            status = RA_FAIL;
                        }
                        else if (ga_grow(&regstack, sizeof(regstar_T)) == FAIL)
                            status = RA_FAIL;
                        else
                        {
                            regstack.ga_len += sizeof(regstar_T);
                            rp = regstack_push(rst.minval <= rst.maxval ? RS_STAR_LONG : RS_STAR_SHORT, scan);
                            if (rp == NULL)
                                status = RA_FAIL;
                            else
                            {
                                *(((regstar_T *)rp) - 1) = rst;
                                status = RA_BREAK;      /* skip the restore bits */
                            }
                        }
                    }
                    else
                        status = RA_NOMATCH;
                }
                break;

            case NOMATCH:
            case MATCH:
            case SUBPAT:
                rp = regstack_push(RS_NOMATCH, scan);
                if (rp == NULL)
                    status = RA_FAIL;
                else
                {
                    rp->rs_no = op;
                    reg_save(&rp->rs_un.regsave, &backpos);
                    next = OPERAND(scan);
                    /* We continue and handle the result when done. */
                }
                break;

            case BEHIND:
            case NOBEHIND:
                /* Need a bit of room to store extra positions. */
                if ((long)((unsigned)regstack.ga_len >> 10) >= p_mmp)
                {
                    EMSG((char *)e_maxmempat);
                    status = RA_FAIL;
                }
                else if (ga_grow(&regstack, sizeof(regbehind_T)) == FAIL)
                    status = RA_FAIL;
                else
                {
                    regstack.ga_len += sizeof(regbehind_T);
                    rp = regstack_push(RS_BEHIND1, scan);
                    if (rp == NULL)
                        status = RA_FAIL;
                    else
                    {
                        /* Need to save the subexpr to be able to restore them
                         * when there is a match but we don't use it. */
                        save_subexpr(((regbehind_T *)rp) - 1);

                        rp->rs_no = op;
                        reg_save(&rp->rs_un.regsave, &backpos);
                        /* First try if what follows matches.  If it does then we
                         * check the behind match by looping. */
                    }
                }
                break;

            case BHPOS:
                if (REG_MULTI)
                {
                    if (behind_pos.rs_u.pos.col != (colnr_T)(reginput - regline)
                            || behind_pos.rs_u.pos.lnum != reglnum)
                        status = RA_NOMATCH;
                }
                else if (behind_pos.rs_u.ptr != reginput)
                    status = RA_NOMATCH;
                break;

            case NEWL:
                if ((c != NUL || !REG_MULTI || reglnum > reg_maxline
                                || reg_line_lbr) && (c != '\n' || !reg_line_lbr))
                    status = RA_NOMATCH;
                else if (reg_line_lbr)
                    reginput += utfc_ptr2len(reginput);
                else
                    reg_nextline();
                break;

            case END:
                status = RA_MATCH;  /* Success! */
                break;

            default:
                EMSG((char *)e_re_corr);
                status = RA_FAIL;
                break;
            }
        }

        /* If we can't continue sequentially, break the inner loop. */
        if (status != RA_CONT)
            break;

        /* Continue in inner loop, advance to next item. */
        scan = next;
    }

    /*
     * If there is something on the regstack execute the code for the state.
     * If the state is popped then loop and use the older state.
     */
    while (regstack.ga_len > 0 && status != RA_FAIL)
    {
        rp = (regitem_T *)((char *)regstack.ga_data + regstack.ga_len) - 1;
        switch (rp->rs_state)
        {
          case RS_NOPEN:
            /* Result is passed on as-is, simply pop the state. */
            regstack_pop(&scan);
            break;

          case RS_MOPEN:
            /* Pop the state.  Restore pointers when there is no match. */
            if (status == RA_NOMATCH)
                restore_se(&rp->rs_un.sesave, &reg_startpos[rp->rs_no], &reg_startp[rp->rs_no]);
            regstack_pop(&scan);
            break;

          case RS_ZOPEN:
            /* Pop the state.  Restore pointers when there is no match. */
            if (status == RA_NOMATCH)
                restore_se(&rp->rs_un.sesave, &reg_startzpos[rp->rs_no], &reg_startzp[rp->rs_no]);
            regstack_pop(&scan);
            break;

          case RS_MCLOSE:
            /* Pop the state.  Restore pointers when there is no match. */
            if (status == RA_NOMATCH)
                restore_se(&rp->rs_un.sesave, &reg_endpos[rp->rs_no], &reg_endp[rp->rs_no]);
            regstack_pop(&scan);
            break;

          case RS_ZCLOSE:
            /* Pop the state.  Restore pointers when there is no match. */
            if (status == RA_NOMATCH)
                restore_se(&rp->rs_un.sesave, &reg_endzpos[rp->rs_no], &reg_endzp[rp->rs_no]);
            regstack_pop(&scan);
            break;

          case RS_BRANCH:
            if (status == RA_MATCH)
                /* this branch matched, use it */
                regstack_pop(&scan);
            else
            {
                if (status != RA_BREAK)
                {
                    /* After a non-matching branch: try next one. */
                    reg_restore(&rp->rs_un.regsave, &backpos);
                    scan = rp->rs_scan;
                }
                if (scan == NULL || OP(scan) != BRANCH)
                {
                    /* no more branches, didn't find a match */
                    status = RA_NOMATCH;
                    regstack_pop(&scan);
                }
                else
                {
                    /* Prepare to try a branch. */
                    rp->rs_scan = regnext(scan);
                    reg_save(&rp->rs_un.regsave, &backpos);
                    scan = OPERAND(scan);
                }
            }
            break;

          case RS_BRCPLX_MORE:
            /* Pop the state.  Restore pointers when there is no match. */
            if (status == RA_NOMATCH)
            {
                reg_restore(&rp->rs_un.regsave, &backpos);
                --brace_count[rp->rs_no];       /* decrement match count */
            }
            regstack_pop(&scan);
            break;

          case RS_BRCPLX_LONG:
            /* Pop the state.  Restore pointers when there is no match. */
            if (status == RA_NOMATCH)
            {
                /* There was no match, but we did find enough matches. */
                reg_restore(&rp->rs_un.regsave, &backpos);
                --brace_count[rp->rs_no];
                /* continue with the items after "\{}" */
                status = RA_CONT;
            }
            regstack_pop(&scan);
            if (status == RA_CONT)
                scan = regnext(scan);
            break;

          case RS_BRCPLX_SHORT:
            /* Pop the state.  Restore pointers when there is no match. */
            if (status == RA_NOMATCH)
                /* There was no match, try to match one more item. */
                reg_restore(&rp->rs_un.regsave, &backpos);
            regstack_pop(&scan);
            if (status == RA_NOMATCH)
            {
                scan = OPERAND(scan);
                status = RA_CONT;
            }
            break;

          case RS_NOMATCH:
            /* Pop the state.  If the operand matches for NOMATCH or
             * doesn't match for MATCH/SUBPAT, we fail.  Otherwise backup,
             * except for SUBPAT, and continue with the next item. */
            if (status == (rp->rs_no == NOMATCH ? RA_MATCH : RA_NOMATCH))
                status = RA_NOMATCH;
            else
            {
                status = RA_CONT;
                if (rp->rs_no != SUBPAT)        /* zero-width */
                    reg_restore(&rp->rs_un.regsave, &backpos);
            }
            regstack_pop(&scan);
            if (status == RA_CONT)
                scan = regnext(scan);
            break;

          case RS_BEHIND1:
            if (status == RA_NOMATCH)
            {
                regstack_pop(&scan);
                regstack.ga_len -= sizeof(regbehind_T);
            }
            else
            {
                /* The stuff after BEHIND/NOBEHIND matches.  Now try if
                 * the behind part does (not) match before the current
                 * position in the input.  This must be done at every
                 * position in the input and checking if the match ends at
                 * the current position. */

                /* save the position after the found match for next */
                reg_save(&(((regbehind_T *)rp) - 1)->save_after, &backpos);

                /* Start looking for a match with operand at the current
                 * position.  Go back one character until we find the
                 * result, hitting the start of the line or the previous
                 * line (for multi-line matching).
                 * Set behind_pos to where the match should end, BHPOS
                 * will match it.  Save the current value. */
                (((regbehind_T *)rp) - 1)->save_behind = behind_pos;
                behind_pos = rp->rs_un.regsave;

                rp->rs_state = RS_BEHIND2;

                reg_restore(&rp->rs_un.regsave, &backpos);
                scan = OPERAND(rp->rs_scan) + 4;
            }
            break;

          case RS_BEHIND2:
            /*
             * Looping for BEHIND / NOBEHIND match.
             */
            if (status == RA_MATCH && reg_save_equal(&behind_pos))
            {
                /* found a match that ends where "next" started */
                behind_pos = (((regbehind_T *)rp) - 1)->save_behind;
                if (rp->rs_no == BEHIND)
                    reg_restore(&(((regbehind_T *)rp) - 1)->save_after, &backpos);
                else
                {
                    /* But we didn't want a match.  Need to restore the
                     * subexpr, because what follows matched, so they have been set. */
                    status = RA_NOMATCH;
                    restore_subexpr(((regbehind_T *)rp) - 1);
                }
                regstack_pop(&scan);
                regstack.ga_len -= sizeof(regbehind_T);
            }
            else
            {
                long limit;

                /* No match or a match that doesn't end where we want it: Go
                 * back one character.  May go to previous line once. */
                no = OK;
                limit = OPERAND_MIN(rp->rs_scan);
                if (REG_MULTI)
                {
                    if (limit > 0
                            && ((rp->rs_un.regsave.rs_u.pos.lnum < behind_pos.rs_u.pos.lnum
                                    ? (colnr_T)STRLEN(regline)
                                    : behind_pos.rs_u.pos.col)
                                - rp->rs_un.regsave.rs_u.pos.col >= limit))
                        no = FAIL;
                    else if (rp->rs_un.regsave.rs_u.pos.col == 0)
                    {
                        if (rp->rs_un.regsave.rs_u.pos.lnum < behind_pos.rs_u.pos.lnum
                                || reg_getline(--rp->rs_un.regsave.rs_u.pos.lnum) == NULL)
                            no = FAIL;
                        else
                        {
                            reg_restore(&rp->rs_un.regsave, &backpos);
                            rp->rs_un.regsave.rs_u.pos.col = (colnr_T)STRLEN(regline);
                        }
                    }
                    else
                    {
                        rp->rs_un.regsave.rs_u.pos.col -= utf_head_off(regline, regline + rp->rs_un.regsave.rs_u.pos.col - 1) + 1;
                    }
                }
                else
                {
                    if (rp->rs_un.regsave.rs_u.ptr == regline)
                        no = FAIL;
                    else
                    {
                        mb_ptr_back(regline, rp->rs_un.regsave.rs_u.ptr);
                        if (limit > 0 && (long)(behind_pos.rs_u.ptr - rp->rs_un.regsave.rs_u.ptr) > limit)
                            no = FAIL;
                    }
                }
                if (no == OK)
                {
                    /* Advanced, prepare for finding match again. */
                    reg_restore(&rp->rs_un.regsave, &backpos);
                    scan = OPERAND(rp->rs_scan) + 4;
                    if (status == RA_MATCH)
                    {
                        /* We did match, so subexpr may have been changed,
                         * need to restore them for the next try. */
                        status = RA_NOMATCH;
                        restore_subexpr(((regbehind_T *)rp) - 1);
                    }
                }
                else
                {
                    /* Can't advance.  For NOBEHIND that's a match. */
                    behind_pos = (((regbehind_T *)rp) - 1)->save_behind;
                    if (rp->rs_no == NOBEHIND)
                    {
                        reg_restore(&(((regbehind_T *)rp) - 1)->save_after, &backpos);
                        status = RA_MATCH;
                    }
                    else
                    {
                        /* We do want a proper match.  Need to restore the
                         * subexpr if we had a match, because they may have been set. */
                        if (status == RA_MATCH)
                        {
                            status = RA_NOMATCH;
                            restore_subexpr(((regbehind_T *)rp) - 1);
                        }
                    }
                    regstack_pop(&scan);
                    regstack.ga_len -= sizeof(regbehind_T);
                }
            }
            break;

          case RS_STAR_LONG:
          case RS_STAR_SHORT:
            {
                regstar_T           *rst = ((regstar_T *)rp) - 1;

                if (status == RA_MATCH)
                {
                    regstack_pop(&scan);
                    regstack.ga_len -= sizeof(regstar_T);
                    break;
                }

                /* Tried once already, restore input pointers. */
                if (status != RA_BREAK)
                    reg_restore(&rp->rs_un.regsave, &backpos);

                /* Repeat until we found a position where it could match. */
                for (;;)
                {
                    if (status != RA_BREAK)
                    {
                        /* Tried first position already, advance. */
                        if (rp->rs_state == RS_STAR_LONG)
                        {
                            /* Trying for longest match, but couldn't or
                             * didn't match -- back up one char. */
                            if (--rst->count < rst->minval)
                                break;
                            if (reginput == regline)
                            {
                                /* backup to last char of previous line */
                                --reglnum;
                                regline = reg_getline(reglnum);
                                /* Just in case regrepeat() didn't count right. */
                                if (regline == NULL)
                                    break;
                                reginput = regline + STRLEN(regline);
                                fast_breakcheck();
                            }
                            else
                                mb_ptr_back(regline, reginput);
                        }
                        else
                        {
                            /* Range is backwards, use shortest match first.
                             * Careful: maxval and minval are exchanged!
                             * Couldn't or didn't match: try advancing one char. */
                            if (rst->count == rst->minval || regrepeat(OPERAND(rp->rs_scan), 1L) == 0)
                                break;
                            ++rst->count;
                        }
                        if (got_int)
                            break;
                    }
                    else
                        status = RA_NOMATCH;

                    /* If it could match, try it. */
                    if (rst->nextb == NUL || *reginput == rst->nextb || *reginput == rst->nextb_ic)
                    {
                        reg_save(&rp->rs_un.regsave, &backpos);
                        scan = regnext(rp->rs_scan);
                        status = RA_CONT;
                        break;
                    }
                }
                if (status != RA_CONT)
                {
                    /* Failed. */
                    regstack_pop(&scan);
                    regstack.ga_len -= sizeof(regstar_T);
                    status = RA_NOMATCH;
                }
            }
            break;
        }

        /* If we want to continue the inner loop or didn't pop a state
         * continue matching loop */
        if (status == RA_CONT || rp == (regitem_T *)((char *)regstack.ga_data + regstack.ga_len) - 1)
            break;
    }

    /* May need to continue with the inner loop, starting at "scan". */
    if (status == RA_CONT)
        continue;

    /*
     * If the regstack is empty or something failed we are done.
     */
    if (regstack.ga_len == 0 || status == RA_FAIL)
    {
        if (scan == NULL)
        {
            /*
             * We get here only if there's trouble -- normally "case END" is
             * the terminating point.
             */
            EMSG((char *)e_re_corr);
        }
        if (status == RA_FAIL)
            got_int = TRUE;
        return (status == RA_MATCH);
    }
  }

  /* NOTREACHED */
}

/*
 * Push an item onto the regstack.
 * Returns pointer to new item.  Returns NULL when out of memory.
 */
    static regitem_T *
regstack_push(state, scan)
    regstate_T  state;
    char_u      *scan;
{
    regitem_T   *rp;

    if ((long)((unsigned)regstack.ga_len >> 10) >= p_mmp)
    {
        EMSG((char *)e_maxmempat);
        return NULL;
    }
    if (ga_grow(&regstack, sizeof(regitem_T)) == FAIL)
        return NULL;

    rp = (regitem_T *)((char *)regstack.ga_data + regstack.ga_len);
    rp->rs_state = state;
    rp->rs_scan = scan;

    regstack.ga_len += sizeof(regitem_T);
    return rp;
}

/*
 * Pop an item from the regstack.
 */
    static void
regstack_pop(scan)
    char_u      **scan;
{
    regitem_T   *rp;

    rp = (regitem_T *)((char *)regstack.ga_data + regstack.ga_len) - 1;
    *scan = rp->rs_scan;

    regstack.ga_len -= sizeof(regitem_T);
}

/*
 * regrepeat - repeatedly match something simple, return how many.
 * Advances reginput (and reglnum) to just after the matched chars.
 */
    static int
regrepeat(p, maxcount)
    char_u      *p;
    long        maxcount;   /* maximum number of matches allowed */
{
    long        count = 0;
    char_u      *scan;
    char_u      *opnd;
    int         mask;
    int         testval = 0;

    scan = reginput;        /* Make local copy of reginput for speed. */
    opnd = OPERAND(p);
    switch (OP(p))
    {
      case ANY:
      case ANY + ADD_NL:
        while (count < maxcount)
        {
            /* Matching anything means we continue until end-of-line (or
             * end-of-file for ANY + ADD_NL), only limited by maxcount. */
            while (*scan != NUL && count < maxcount)
            {
                ++count;
                scan += utfc_ptr2len(scan);
            }
            if (!REG_MULTI || !WITH_NL(OP(p)) || reglnum > reg_maxline || reg_line_lbr || count == maxcount)
                break;
            ++count;            /* count the line-break */
            reg_nextline();
            scan = reginput;
            if (got_int)
                break;
        }
        break;

      case IDENT:
      case IDENT + ADD_NL:
        testval = TRUE;
        /*FALLTHROUGH*/
      case SIDENT:
      case SIDENT + ADD_NL:
        while (count < maxcount)
        {
            if (vim_isIDc(utf_ptr2char(scan)) && (testval || !VIM_ISDIGIT(*scan)))
            {
                scan += utfc_ptr2len(scan);
            }
            else if (*scan == NUL)
            {
                if (!REG_MULTI || !WITH_NL(OP(p)) || reglnum > reg_maxline || reg_line_lbr)
                    break;
                reg_nextline();
                scan = reginput;
                if (got_int)
                    break;
            }
            else if (reg_line_lbr && *scan == '\n' && WITH_NL(OP(p)))
                ++scan;
            else
                break;
            ++count;
        }
        break;

      case KWORD:
      case KWORD + ADD_NL:
        testval = TRUE;
        /*FALLTHROUGH*/
      case SKWORD:
      case SKWORD + ADD_NL:
        while (count < maxcount)
        {
            if (vim_iswordp_buf(scan, reg_buf) && (testval || !VIM_ISDIGIT(*scan)))
            {
                scan += utfc_ptr2len(scan);
            }
            else if (*scan == NUL)
            {
                if (!REG_MULTI || !WITH_NL(OP(p)) || reglnum > reg_maxline || reg_line_lbr)
                    break;
                reg_nextline();
                scan = reginput;
                if (got_int)
                    break;
            }
            else if (reg_line_lbr && *scan == '\n' && WITH_NL(OP(p)))
                ++scan;
            else
                break;
            ++count;
        }
        break;

      case FNAME:
      case FNAME + ADD_NL:
        testval = TRUE;
        /*FALLTHROUGH*/
      case SFNAME:
      case SFNAME + ADD_NL:
        while (count < maxcount)
        {
            if (vim_isfilec(utf_ptr2char(scan)) && (testval || !VIM_ISDIGIT(*scan)))
            {
                scan += utfc_ptr2len(scan);
            }
            else if (*scan == NUL)
            {
                if (!REG_MULTI || !WITH_NL(OP(p)) || reglnum > reg_maxline || reg_line_lbr)
                    break;
                reg_nextline();
                scan = reginput;
                if (got_int)
                    break;
            }
            else if (reg_line_lbr && *scan == '\n' && WITH_NL(OP(p)))
                ++scan;
            else
                break;
            ++count;
        }
        break;

      case PRINT:
      case PRINT + ADD_NL:
        testval = TRUE;
        /*FALLTHROUGH*/
      case SPRINT:
      case SPRINT + ADD_NL:
        while (count < maxcount)
        {
            if (*scan == NUL)
            {
                if (!REG_MULTI || !WITH_NL(OP(p)) || reglnum > reg_maxline || reg_line_lbr)
                    break;
                reg_nextline();
                scan = reginput;
                if (got_int)
                    break;
            }
            else if (vim_isprintc(utf_ptr2char(scan)) == 1 && (testval || !VIM_ISDIGIT(*scan)))
            {
                scan += utfc_ptr2len(scan);
            }
            else if (reg_line_lbr && *scan == '\n' && WITH_NL(OP(p)))
                ++scan;
            else
                break;
            ++count;
        }
        break;

      case WHITE:
      case WHITE + ADD_NL:
        testval = mask = RI_WHITE;
do_class:
        while (count < maxcount)
        {
            int         l;
            if (*scan == NUL)
            {
                if (!REG_MULTI || !WITH_NL(OP(p)) || reglnum > reg_maxline || reg_line_lbr)
                    break;
                reg_nextline();
                scan = reginput;
                if (got_int)
                    break;
            }
            else if ((l = utfc_ptr2len(scan)) > 1)
            {
                if (testval != 0)
                    break;
                scan += l;
            }
            else if ((class_tab[*scan] & mask) == testval)
                ++scan;
            else if (reg_line_lbr && *scan == '\n' && WITH_NL(OP(p)))
                ++scan;
            else
                break;
            ++count;
        }
        break;

      case NWHITE:
      case NWHITE + ADD_NL:
        mask = RI_WHITE;
        goto do_class;
      case DIGIT:
      case DIGIT + ADD_NL:
        testval = mask = RI_DIGIT;
        goto do_class;
      case NDIGIT:
      case NDIGIT + ADD_NL:
        mask = RI_DIGIT;
        goto do_class;
      case HEX:
      case HEX + ADD_NL:
        testval = mask = RI_HEX;
        goto do_class;
      case NHEX:
      case NHEX + ADD_NL:
        mask = RI_HEX;
        goto do_class;
      case OCTAL:
      case OCTAL + ADD_NL:
        testval = mask = RI_OCTAL;
        goto do_class;
      case NOCTAL:
      case NOCTAL + ADD_NL:
        mask = RI_OCTAL;
        goto do_class;
      case WORD:
      case WORD + ADD_NL:
        testval = mask = RI_WORD;
        goto do_class;
      case NWORD:
      case NWORD + ADD_NL:
        mask = RI_WORD;
        goto do_class;
      case HEAD:
      case HEAD + ADD_NL:
        testval = mask = RI_HEAD;
        goto do_class;
      case NHEAD:
      case NHEAD + ADD_NL:
        mask = RI_HEAD;
        goto do_class;
      case ALPHA:
      case ALPHA + ADD_NL:
        testval = mask = RI_ALPHA;
        goto do_class;
      case NALPHA:
      case NALPHA + ADD_NL:
        mask = RI_ALPHA;
        goto do_class;
      case LOWER:
      case LOWER + ADD_NL:
        testval = mask = RI_LOWER;
        goto do_class;
      case NLOWER:
      case NLOWER + ADD_NL:
        mask = RI_LOWER;
        goto do_class;
      case UPPER:
      case UPPER + ADD_NL:
        testval = mask = RI_UPPER;
        goto do_class;
      case NUPPER:
      case NUPPER + ADD_NL:
        mask = RI_UPPER;
        goto do_class;

      case EXACTLY:
        {
            int     cu, cl;

            /* This doesn't do a multi-byte character, because a MULTIBYTECODE
             * would have been used for it.  It does handle single-byte
             * characters, such as latin1. */
            if (ireg_ic)
            {
                cu = vim_toupper(*opnd);
                cl = vim_tolower(*opnd);
                while (count < maxcount && (*scan == cu || *scan == cl))
                {
                    count++;
                    scan++;
                }
            }
            else
            {
                cu = *opnd;
                while (count < maxcount && *scan == cu)
                {
                    count++;
                    scan++;
                }
            }
            break;
        }

      case MULTIBYTECODE:
        {
            int         i, len, cf = 0;

            /* Safety check (just in case 'encoding' was changed since
             * compiling the program). */
            if ((len = utfc_ptr2len(opnd)) > 1)
            {
                if (ireg_ic)
                    cf = utf_fold(utf_ptr2char(opnd));
                while (count < maxcount)
                {
                    for (i = 0; i < len; ++i)
                        if (opnd[i] != scan[i])
                            break;
                    if (i < len && (!ireg_ic || utf_fold(utf_ptr2char(scan)) != cf))
                        break;
                    scan += len;
                    ++count;
                }
            }
        }
        break;

      case ANYOF:
      case ANYOF + ADD_NL:
        testval = TRUE;
        /*FALLTHROUGH*/

      case ANYBUT:
      case ANYBUT + ADD_NL:
        while (count < maxcount)
        {
            int len;
            if (*scan == NUL)
            {
                if (!REG_MULTI || !WITH_NL(OP(p)) || reglnum > reg_maxline || reg_line_lbr)
                    break;
                reg_nextline();
                scan = reginput;
                if (got_int)
                    break;
            }
            else if (reg_line_lbr && *scan == '\n' && WITH_NL(OP(p)))
                ++scan;
            else if ((len = utfc_ptr2len(scan)) > 1)
            {
                if ((cstrchr(opnd, utf_ptr2char(scan)) == NULL) == testval)
                    break;
                scan += len;
            }
            else
            {
                if ((cstrchr(opnd, *scan) == NULL) == testval)
                    break;
                ++scan;
            }
            ++count;
        }
        break;

      case NEWL:
        while (count < maxcount
                && ((*scan == NUL && reglnum <= reg_maxline && !reg_line_lbr
                            && REG_MULTI) || (*scan == '\n' && reg_line_lbr)))
        {
            count++;
            if (reg_line_lbr)
                reginput += utfc_ptr2len(reginput);
            else
                reg_nextline();
            scan = reginput;
            if (got_int)
                break;
        }
        break;

      default:                  /* Oh dear.  Called inappropriately. */
        EMSG((char *)e_re_corr);
        break;
    }

    reginput = scan;

    return (int)count;
}

/*
 * regnext - dig the "next" pointer out of a node
 * Returns NULL when calculating size, when there is no next item and when there is an error.
 */
    static char_u *
regnext(p)
    char_u  *p;
{
    int     offset;

    if (p == JUST_CALC_SIZE || reg_toolong)
        return NULL;

    offset = NEXT(p);
    if (offset == 0)
        return NULL;

    if (OP(p) == BACK)
        return p - offset;
    else
        return p + offset;
}

/*
 * Check the regexp program for its magic number.
 * Return TRUE if it's wrong.
 */
    static int
prog_magic_wrong()
{
    regprog_T   *prog;

    prog = REG_MULTI ? reg_mmatch->regprog : reg_match->regprog;
    if (prog->engine == &nfa_regengine)
        /* For NFA matcher we don't check the magic */
        return FALSE;

    if (((int)*(char_u *)(((bt_regprog_T *)prog)->program)) != REGMAGIC)
    {
        EMSG((char *)e_re_corr);
        return TRUE;
    }
    return FALSE;
}

/*
 * Cleanup the subexpressions, if this wasn't done yet.
 * This construction is used to clear the subexpressions only when they are
 * used (to increase speed).
 */
    static void
cleanup_subexpr()
{
    if (need_clear_subexpr)
    {
        if (REG_MULTI)
        {
            /* Use 0xff to set lnum to -1 */
            vim_memset(reg_startpos, 0xff, sizeof(lpos_T) * NSUBEXP);
            vim_memset(reg_endpos, 0xff, sizeof(lpos_T) * NSUBEXP);
        }
        else
        {
            vim_memset(reg_startp, 0, sizeof(char_u *) * NSUBEXP);
            vim_memset(reg_endp, 0, sizeof(char_u *) * NSUBEXP);
        }
        need_clear_subexpr = FALSE;
    }
}

    static void
cleanup_zsubexpr()
{
    if (need_clear_zsubexpr)
    {
        if (REG_MULTI)
        {
            /* Use 0xff to set lnum to -1 */
            vim_memset(reg_startzpos, 0xff, sizeof(lpos_T) * NSUBEXP);
            vim_memset(reg_endzpos, 0xff, sizeof(lpos_T) * NSUBEXP);
        }
        else
        {
            vim_memset(reg_startzp, 0, sizeof(char_u *) * NSUBEXP);
            vim_memset(reg_endzp, 0, sizeof(char_u *) * NSUBEXP);
        }
        need_clear_zsubexpr = FALSE;
    }
}

/*
 * Save the current subexpr to "bp", so that they can be restored
 * later by restore_subexpr().
 */
    static void
save_subexpr(bp)
    regbehind_T *bp;
{
    int i;

    /* When "need_clear_subexpr" is set we don't need to save the values, only
     * remember that this flag needs to be set again when restoring. */
    bp->save_need_clear_subexpr = need_clear_subexpr;
    if (!need_clear_subexpr)
    {
        for (i = 0; i < NSUBEXP; ++i)
        {
            if (REG_MULTI)
            {
                bp->save_start[i].se_u.pos = reg_startpos[i];
                bp->save_end[i].se_u.pos = reg_endpos[i];
            }
            else
            {
                bp->save_start[i].se_u.ptr = reg_startp[i];
                bp->save_end[i].se_u.ptr = reg_endp[i];
            }
        }
    }
}

/*
 * Restore the subexpr from "bp".
 */
    static void
restore_subexpr(bp)
    regbehind_T *bp;
{
    int i;

    /* Only need to restore saved values when they are not to be cleared. */
    need_clear_subexpr = bp->save_need_clear_subexpr;
    if (!need_clear_subexpr)
    {
        for (i = 0; i < NSUBEXP; ++i)
        {
            if (REG_MULTI)
            {
                reg_startpos[i] = bp->save_start[i].se_u.pos;
                reg_endpos[i] = bp->save_end[i].se_u.pos;
            }
            else
            {
                reg_startp[i] = bp->save_start[i].se_u.ptr;
                reg_endp[i] = bp->save_end[i].se_u.ptr;
            }
        }
    }
}

/*
 * Advance reglnum, regline and reginput to the next line.
 */
    static void
reg_nextline()
{
    regline = reg_getline(++reglnum);
    reginput = regline;
    fast_breakcheck();
}

/*
 * Save the input line and position in a regsave_T.
 */
    static void
reg_save(save, gap)
    regsave_T   *save;
    garray_T    *gap;
{
    if (REG_MULTI)
    {
        save->rs_u.pos.col = (colnr_T)(reginput - regline);
        save->rs_u.pos.lnum = reglnum;
    }
    else
        save->rs_u.ptr = reginput;
    save->rs_len = gap->ga_len;
}

/*
 * Restore the input line and position from a regsave_T.
 */
    static void
reg_restore(save, gap)
    regsave_T   *save;
    garray_T    *gap;
{
    if (REG_MULTI)
    {
        if (reglnum != save->rs_u.pos.lnum)
        {
            /* only call reg_getline() when the line number changed to save a bit of time */
            reglnum = save->rs_u.pos.lnum;
            regline = reg_getline(reglnum);
        }
        reginput = regline + save->rs_u.pos.col;
    }
    else
        reginput = save->rs_u.ptr;
    gap->ga_len = save->rs_len;
}

/*
 * Return TRUE if current position is equal to saved position.
 */
    static int
reg_save_equal(save)
    regsave_T   *save;
{
    if (REG_MULTI)
        return reglnum == save->rs_u.pos.lnum && reginput == regline + save->rs_u.pos.col;

    return (reginput == save->rs_u.ptr);
}

/*
 * Tentatively set the sub-expression start to the current position (after
 * calling regmatch() they will have changed).  Need to save the existing
 * values for when there is no match.
 * Use se_save() to use pointer (save_se_multi()) or position (save_se_one()),
 * depending on REG_MULTI.
 */
    static void
save_se_multi(savep, posp)
    save_se_T   *savep;
    lpos_T      *posp;
{
    savep->se_u.pos = *posp;
    posp->lnum = reglnum;
    posp->col = (colnr_T)(reginput - regline);
}

    static void
save_se_one(savep, pp)
    save_se_T   *savep;
    char_u      **pp;
{
    savep->se_u.ptr = *pp;
    *pp = reginput;
}

/*
 * Compare a number with the operand of RE_LNUM, RE_COL or RE_VCOL.
 */
    static int
re_num_cmp(val, scan)
    long_u      val;
    char_u      *scan;
{
    long_u  n = OPERAND_MIN(scan);

    if (OPERAND_CMP(scan) == '>')
        return val > n;
    if (OPERAND_CMP(scan) == '<')
        return val < n;

    return val == n;
}

/*
 * Check whether a backreference matches.
 * Returns RA_FAIL, RA_NOMATCH or RA_MATCH.
 * If "bytelen" is not NULL, it is set to the byte length of the match in the last line.
 */
    static int
match_with_backref(start_lnum, start_col, end_lnum, end_col, bytelen)
    linenr_T start_lnum;
    colnr_T  start_col;
    linenr_T end_lnum;
    colnr_T  end_col;
    int      *bytelen;
{
    linenr_T    clnum = start_lnum;
    colnr_T     ccol = start_col;
    int         len;
    char_u      *p;

    if (bytelen != NULL)
        *bytelen = 0;
    for (;;)
    {
        /* Since getting one line may invalidate the other, need to make copy.
         * Slow! */
        if (regline != reg_tofree)
        {
            len = (int)STRLEN(regline);
            if (reg_tofree == NULL || len >= (int)reg_tofreelen)
            {
                len += 50;      /* get some extra */
                vim_free(reg_tofree);
                reg_tofree = alloc(len);
                if (reg_tofree == NULL)
                    return RA_FAIL; /* out of memory! */
                reg_tofreelen = len;
            }
            STRCPY(reg_tofree, regline);
            reginput = reg_tofree + (reginput - regline);
            regline = reg_tofree;
        }

        /* Get the line to compare with. */
        p = reg_getline(clnum);
        if (clnum == end_lnum)
            len = end_col - ccol;
        else
            len = (int)STRLEN(p + ccol);

        if (cstrncmp(p + ccol, reginput, &len) != 0)
            return RA_NOMATCH;  /* doesn't match */
        if (bytelen != NULL)
            *bytelen += len;
        if (clnum == end_lnum)
            break;              /* match and at end! */
        if (reglnum >= reg_maxline)
            return RA_NOMATCH;  /* text too short */

        /* Advance to next line. */
        reg_nextline();
        if (bytelen != NULL)
            *bytelen = 0;
        ++clnum;
        ccol = 0;
        if (got_int)
            return RA_FAIL;
    }

    /* found a match!  Note that regline may now point to a copy of the line,
     * that should not matter. */
    return RA_MATCH;
}

/*
 * Used in a place where no * or \+ can follow.
 */
    static int
re_mult_next(what)
    char *what;
{
    if (re_multi_type(peekchr()) == MULTI_MULT)
        EMSG2_RET_FAIL("E888: (NFA regexp) cannot repeat %s", what);
    return OK;
}

static void mb_decompose(int c, int *c1, int *c2, int *c3);

typedef struct
{
    int a, b, c;
} decomp_T;

/* 0xfb20 - 0xfb4f */
static decomp_T decomp_table[0xfb4f-0xfb20+1] =
{
    {0x5e2,0,0},                /* 0xfb20       alt ayin */
    {0x5d0,0,0},                /* 0xfb21       alt alef */
    {0x5d3,0,0},                /* 0xfb22       alt dalet */
    {0x5d4,0,0},                /* 0xfb23       alt he */
    {0x5db,0,0},                /* 0xfb24       alt kaf */
    {0x5dc,0,0},                /* 0xfb25       alt lamed */
    {0x5dd,0,0},                /* 0xfb26       alt mem-sofit */
    {0x5e8,0,0},                /* 0xfb27       alt resh */
    {0x5ea,0,0},                /* 0xfb28       alt tav */
    {'+', 0, 0},                /* 0xfb29       alt plus */
    {0x5e9, 0x5c1, 0},          /* 0xfb2a       shin+shin-dot */
    {0x5e9, 0x5c2, 0},          /* 0xfb2b       shin+sin-dot */
    {0x5e9, 0x5c1, 0x5bc},      /* 0xfb2c       shin+shin-dot+dagesh */
    {0x5e9, 0x5c2, 0x5bc},      /* 0xfb2d       shin+sin-dot+dagesh */
    {0x5d0, 0x5b7, 0},          /* 0xfb2e       alef+patah */
    {0x5d0, 0x5b8, 0},          /* 0xfb2f       alef+qamats */
    {0x5d0, 0x5b4, 0},          /* 0xfb30       alef+hiriq */
    {0x5d1, 0x5bc, 0},          /* 0xfb31       bet+dagesh */
    {0x5d2, 0x5bc, 0},          /* 0xfb32       gimel+dagesh */
    {0x5d3, 0x5bc, 0},          /* 0xfb33       dalet+dagesh */
    {0x5d4, 0x5bc, 0},          /* 0xfb34       he+dagesh */
    {0x5d5, 0x5bc, 0},          /* 0xfb35       vav+dagesh */
    {0x5d6, 0x5bc, 0},          /* 0xfb36       zayin+dagesh */
    {0xfb37, 0, 0},             /* 0xfb37 -- UNUSED */
    {0x5d8, 0x5bc, 0},          /* 0xfb38       tet+dagesh */
    {0x5d9, 0x5bc, 0},          /* 0xfb39       yud+dagesh */
    {0x5da, 0x5bc, 0},          /* 0xfb3a       kaf sofit+dagesh */
    {0x5db, 0x5bc, 0},          /* 0xfb3b       kaf+dagesh */
    {0x5dc, 0x5bc, 0},          /* 0xfb3c       lamed+dagesh */
    {0xfb3d, 0, 0},             /* 0xfb3d -- UNUSED */
    {0x5de, 0x5bc, 0},          /* 0xfb3e       mem+dagesh */
    {0xfb3f, 0, 0},             /* 0xfb3f -- UNUSED */
    {0x5e0, 0x5bc, 0},          /* 0xfb40       nun+dagesh */
    {0x5e1, 0x5bc, 0},          /* 0xfb41       samech+dagesh */
    {0xfb42, 0, 0},             /* 0xfb42 -- UNUSED */
    {0x5e3, 0x5bc, 0},          /* 0xfb43       pe sofit+dagesh */
    {0x5e4, 0x5bc,0},           /* 0xfb44       pe+dagesh */
    {0xfb45, 0, 0},             /* 0xfb45 -- UNUSED */
    {0x5e6, 0x5bc, 0},          /* 0xfb46       tsadi+dagesh */
    {0x5e7, 0x5bc, 0},          /* 0xfb47       qof+dagesh */
    {0x5e8, 0x5bc, 0},          /* 0xfb48       resh+dagesh */
    {0x5e9, 0x5bc, 0},          /* 0xfb49       shin+dagesh */
    {0x5ea, 0x5bc, 0},          /* 0xfb4a       tav+dagesh */
    {0x5d5, 0x5b9, 0},          /* 0xfb4b       vav+holam */
    {0x5d1, 0x5bf, 0},          /* 0xfb4c       bet+rafe */
    {0x5db, 0x5bf, 0},          /* 0xfb4d       kaf+rafe */
    {0x5e4, 0x5bf, 0},          /* 0xfb4e       pe+rafe */
    {0x5d0, 0x5dc, 0}           /* 0xfb4f       alef-lamed */
};

    static void
mb_decompose(c, c1, c2, c3)
    int c, *c1, *c2, *c3;
{
    decomp_T d;

    if (c >= 0xfb20 && c <= 0xfb4f)
    {
        d = decomp_table[c - 0xfb20];
        *c1 = d.a;
        *c2 = d.b;
        *c3 = d.c;
    }
    else
    {
        *c1 = c;
        *c2 = *c3 = 0;
    }
}

/*
 * Compare two strings, ignore case if ireg_ic set.
 * Return 0 if strings match, non-zero otherwise.
 * Correct the length "*n" when composing characters are ignored.
 */
    static int
cstrncmp(s1, s2, n)
    char_u      *s1, *s2;
    int         *n;
{
    int         result;

    if (!ireg_ic)
        result = STRNCMP(s1, s2, *n);
    else
        result = MB_STRNICMP(s1, s2, *n);

    /* if it failed and it's utf8 and we want to combineignore: */
    if (result != 0 && ireg_icombine)
    {
        char_u  *str1, *str2;
        int     c1, c2, c11, c12;
        int     junk;

        /* we have to handle the strcmp ourselves, since it is necessary to
         * deal with the composing characters by ignoring them: */
        str1 = s1;
        str2 = s2;
        c1 = c2 = 0;
        while ((int)(str1 - s1) < *n)
        {
            c1 = mb_ptr2char_adv(&str1);
            c2 = mb_ptr2char_adv(&str2);

            /* decompose the character if necessary, into 'base' characters
             * because I don't care about Arabic, I will hard-code the Hebrew
             * which I *do* care about!  So sue me... */
            if (c1 != c2 && (!ireg_ic || utf_fold(c1) != utf_fold(c2)))
            {
                /* decomposition necessary? */
                mb_decompose(c1, &c11, &junk, &junk);
                mb_decompose(c2, &c12, &junk, &junk);
                c1 = c11;
                c2 = c12;
                if (c11 != c12 && (!ireg_ic || utf_fold(c11) != utf_fold(c12)))
                    break;
            }
        }
        result = c2 - c1;
        if (result == 0)
            *n = (int)(str2 - s2);
    }

    return result;
}

/*
 * cstrchr: This function is used a lot for simple searches, keep it fast!
 */
    static char_u *
cstrchr(s, c)
    char_u      *s;
    int         c;
{
    char_u      *p;
    int         cc;

    if (!ireg_ic)
        return vim_strchr(s, c);

    /* tolower() and toupper() can be slow, comparing twice should be a lot
     * faster (esp. when using MS Visual C++!).
     * For UTF-8 need to use folded case. */
    if (c > 0x80)
        cc = utf_fold(c);
    else if (vim_isupper(c))
        cc = vim_tolower(c);
    else if (vim_islower(c))
        cc = vim_toupper(c);
    else
        return vim_strchr(s, c);

    for (p = s; *p != NUL; p += utfc_ptr2len(p))
    {
        if (c > 0x80)
        {
            if (utf_fold(utf_ptr2char(p)) == cc)
                return p;
        }
        else if (*p == c || *p == cc)
            return p;
    }

    return NULL;
}

/***************************************************************
 *                    regsub stuff                             *
 ***************************************************************/

/*
 * We should define ftpr as a pointer to a function returning a pointer to
 * a function returning a pointer to a function ...
 * This is impossible, so we declare a pointer to a function returning a
 * pointer to a function returning void. This should work for all compilers.
 */
typedef void (*(*fptr_T)(int *, int))();

static fptr_T do_upper(int *, int);
static fptr_T do_Upper(int *, int);
static fptr_T do_lower(int *, int);
static fptr_T do_Lower(int *, int);

static int vim_regsub_both(char_u *source, char_u *dest, int copy, int magic, int backslash);

    static fptr_T
do_upper(d, c)
    int         *d;
    int         c;
{
    *d = vim_toupper(c);

    return (fptr_T)NULL;
}

    static fptr_T
do_Upper(d, c)
    int         *d;
    int         c;
{
    *d = vim_toupper(c);

    return (fptr_T)do_Upper;
}

    static fptr_T
do_lower(d, c)
    int         *d;
    int         c;
{
    *d = vim_tolower(c);

    return (fptr_T)NULL;
}

    static fptr_T
do_Lower(d, c)
    int         *d;
    int         c;
{
    *d = vim_tolower(c);

    return (fptr_T)do_Lower;
}

/*
 * regtilde(): Replace tildes in the pattern by the old pattern.
 *
 * Short explanation of the tilde: It stands for the previous replacement
 * pattern.  If that previous pattern also contains a ~ we should go back a
 * step further...  But we insert the previous pattern into the current one
 * and remember that.
 * This still does not handle the case where "magic" changes.  So require the
 * user to keep his hands off of "magic".
 *
 * The tildes are parsed once before the first call to vim_regsub().
 */
    char_u *
regtilde(source, magic)
    char_u      *source;
    int         magic;
{
    char_u      *newsub = source;
    char_u      *tmpsub;
    char_u      *p;
    int         len;
    int         prevlen;

    for (p = newsub; *p; ++p)
    {
        if ((*p == '~' && magic) || (*p == '\\' && *(p + 1) == '~' && !magic))
        {
            if (reg_prev_sub != NULL)
            {
                /* length = len(newsub) - 1 + len(prev_sub) + 1 */
                prevlen = (int)STRLEN(reg_prev_sub);
                tmpsub = alloc((unsigned)(STRLEN(newsub) + prevlen));
                if (tmpsub != NULL)
                {
                    /* copy prefix */
                    len = (int)(p - newsub);    /* not including ~ */
                    mch_memmove(tmpsub, newsub, (size_t)len);
                    /* interpret tilde */
                    mch_memmove(tmpsub + len, reg_prev_sub, (size_t)prevlen);
                    /* copy postfix */
                    if (!magic)
                        ++p;                    /* back off \ */
                    STRCPY(tmpsub + len + prevlen, p + 1);

                    if (newsub != source)       /* already allocated newsub */
                        vim_free(newsub);
                    newsub = tmpsub;
                    p = newsub + len + prevlen;
                }
            }
            else if (magic)
                STRMOVE(p, p + 1);      /* remove '~' */
            else
                STRMOVE(p, p + 2);      /* remove '\~' */
            --p;
        }
        else
        {
            if (*p == '\\' && p[1])             /* skip escaped characters */
                ++p;
            p += utfc_ptr2len(p) - 1;
        }
    }

    vim_free(reg_prev_sub);
    if (newsub != source)       /* newsub was allocated, just keep it */
        reg_prev_sub = newsub;
    else                        /* no ~ found, need to save newsub */
        reg_prev_sub = vim_strsave(newsub);
    return newsub;
}

static int can_f_submatch = FALSE;      /* TRUE when submatch() can be used */

/* These pointers are used instead of reg_match and reg_mmatch for
 * reg_submatch().  Needed for when the substitution string is an expression
 * that contains a call to substitute() and submatch(). */
static regmatch_T       *submatch_match;
static regmmatch_T      *submatch_mmatch;
static linenr_T         submatch_firstlnum;
static linenr_T         submatch_maxline;
static int              submatch_line_lbr;

/*
 * vim_regsub() - perform substitutions after a vim_regexec() or
 * vim_regexec_multi() match.
 *
 * If "copy" is TRUE really copy into "dest".
 * If "copy" is FALSE nothing is copied, this is just to find out the length
 * of the result.
 *
 * If "backslash" is TRUE, a backslash will be removed later, need to double
 * them to keep them, and insert a backslash before a CR to avoid it being
 * replaced with a line break later.
 *
 * Note: The matched text must not change between the call of
 * vim_regexec()/vim_regexec_multi() and vim_regsub()!  It would make the back
 * references invalid!
 *
 * Returns the size of the replacement, including terminating NUL.
 */
    int
vim_regsub(rmp, source, dest, copy, magic, backslash)
    regmatch_T  *rmp;
    char_u      *source;
    char_u      *dest;
    int         copy;
    int         magic;
    int         backslash;
{
    reg_match = rmp;
    reg_mmatch = NULL;
    reg_maxline = 0;
    reg_buf = curbuf;
    reg_line_lbr = TRUE;
    return vim_regsub_both(source, dest, copy, magic, backslash);
}

    int
vim_regsub_multi(rmp, lnum, source, dest, copy, magic, backslash)
    regmmatch_T *rmp;
    linenr_T    lnum;
    char_u      *source;
    char_u      *dest;
    int         copy;
    int         magic;
    int         backslash;
{
    reg_match = NULL;
    reg_mmatch = rmp;
    reg_buf = curbuf;           /* always works on the current buffer! */
    reg_firstlnum = lnum;
    reg_maxline = curbuf->b_ml.ml_line_count - lnum;
    reg_line_lbr = FALSE;
    return vim_regsub_both(source, dest, copy, magic, backslash);
}

    static int
vim_regsub_both(source, dest, copy, magic, backslash)
    char_u      *source;
    char_u      *dest;
    int         copy;
    int         magic;
    int         backslash;
{
    char_u      *src;
    char_u      *dst;
    char_u      *s;
    int         c;
    int         cc;
    int         no = -1;
    fptr_T      func_all = (fptr_T)NULL;
    fptr_T      func_one = (fptr_T)NULL;
    linenr_T    clnum = 0;      /* init for GCC */
    int         len = 0;        /* init for GCC */
    static char_u *eval_result = NULL;

    /* Be paranoid... */
    if (source == NULL || dest == NULL)
    {
        EMSG((char *)e_null);
        return 0;
    }
    if (prog_magic_wrong())
        return 0;
    src = source;
    dst = dest;

    /*
     * When the substitute part starts with "\=" evaluate it as an expression.
     */
    if (source[0] == '\\' && source[1] == '=' && !can_f_submatch)   /* can't do this recursively */
    {
        /* To make sure that the length doesn't change between checking the
         * length and copying the string, and to speed up things, the
         * resulting string is saved from the call with "copy" == FALSE to the
         * call with "copy" == TRUE. */
        if (copy)
        {
            if (eval_result != NULL)
            {
                STRCPY(dest, eval_result);
                dst += STRLEN(eval_result);
                vim_free(eval_result);
                eval_result = NULL;
            }
        }
        else
        {
            win_T       *save_reg_win;
            int         save_ireg_ic;

            vim_free(eval_result);

            /* The expression may contain substitute(), which calls us
             * recursively.  Make sure submatch() gets the text from the first
             * level.  Don't need to save "reg_buf", because
             * vim_regexec_multi() can't be called recursively. */
            submatch_match = reg_match;
            submatch_mmatch = reg_mmatch;
            submatch_firstlnum = reg_firstlnum;
            submatch_maxline = reg_maxline;
            submatch_line_lbr = reg_line_lbr;
            save_reg_win = reg_win;
            save_ireg_ic = ireg_ic;
            can_f_submatch = TRUE;

            eval_result = eval_to_string(source + 2, NULL, TRUE);
            if (eval_result != NULL)
            {
                int had_backslash = FALSE;

                for (s = eval_result; *s != NUL; s += utfc_ptr2len(s))
                {
                    /* Change NL to CR, so that it becomes a line break,
                     * unless called from vim_regexec_nl().
                     * Skip over a backslashed character. */
                    if (*s == NL && !submatch_line_lbr)
                        *s = CAR;
                    else if (*s == '\\' && s[1] != NUL)
                    {
                        ++s;
                        /* Change NL to CR here too, so that this works:
                         * :s/abc\\\ndef/\="aaa\\\nbbb"/  on text:
                         *   abc\
                         *   def
                         * Not when called from vim_regexec_nl().
                         */
                        if (*s == NL && !submatch_line_lbr)
                            *s = CAR;
                        had_backslash = TRUE;
                    }
                }
                if (had_backslash && backslash)
                {
                    /* Backslashes will be consumed, need to double them. */
                    s = vim_strsave_escaped(eval_result, (char_u *)"\\");
                    if (s != NULL)
                    {
                        vim_free(eval_result);
                        eval_result = s;
                    }
                }

                dst += STRLEN(eval_result);
            }

            reg_match = submatch_match;
            reg_mmatch = submatch_mmatch;
            reg_firstlnum = submatch_firstlnum;
            reg_maxline = submatch_maxline;
            reg_line_lbr = submatch_line_lbr;
            reg_win = save_reg_win;
            ireg_ic = save_ireg_ic;
            can_f_submatch = FALSE;
        }
    }
    else
      while ((c = *src++) != NUL)
      {
        if (c == '&' && magic)
            no = 0;
        else if (c == '\\' && *src != NUL)
        {
            if (*src == '&' && !magic)
            {
                ++src;
                no = 0;
            }
            else if ('0' <= *src && *src <= '9')
            {
                no = *src++ - '0';
            }
            else if (vim_strchr((char_u *)"uUlLeE", *src))
            {
                switch (*src++)
                {
                case 'u':   func_one = (fptr_T)do_upper;
                            continue;
                case 'U':   func_all = (fptr_T)do_Upper;
                            continue;
                case 'l':   func_one = (fptr_T)do_lower;
                            continue;
                case 'L':   func_all = (fptr_T)do_Lower;
                            continue;
                case 'e':
                case 'E':   func_one = func_all = (fptr_T)NULL;
                            continue;
                }
            }
        }
        if (no < 0)           /* Ordinary character. */
        {
            if (c == K_SPECIAL && src[0] != NUL && src[1] != NUL)
            {
                /* Copy a special key as-is. */
                if (copy)
                {
                    *dst++ = c;
                    *dst++ = *src++;
                    *dst++ = *src++;
                }
                else
                {
                    dst += 3;
                    src += 2;
                }
                continue;
            }

            if (c == '\\' && *src != NUL)
            {
                /* Check for abbreviations -- webb */
                switch (*src)
                {
                    case 'r':   c = CAR;        ++src;  break;
                    case 'n':   c = NL;         ++src;  break;
                    case 't':   c = TAB;        ++src;  break;
                 /* Oh no!  \e already has meaning in subst pat :-( */
                 /* case 'e':   c = ESC;        ++src;  break; */
                    case 'b':   c = Ctrl_H;     ++src;  break;

                    /* If "backslash" is TRUE the backslash will be removed
                     * later.  Used to insert a literal CR. */
                    default:    if (backslash)
                                {
                                    if (copy)
                                        *dst = '\\';
                                    ++dst;
                                }
                                c = *src++;
                }
            }
            else
                c = utf_ptr2char(src - 1);

            /* Write to buffer, if copy is set. */
            if (func_one != (fptr_T)NULL)
                /* Turbo C complains without the typecast */
                func_one = (fptr_T)(func_one(&cc, c));
            else if (func_all != (fptr_T)NULL)
                /* Turbo C complains without the typecast */
                func_all = (fptr_T)(func_all(&cc, c));
            else /* just copy */
                cc = c;

            {
                int totlen = utfc_ptr2len(src - 1);

                if (copy)
                    utf_char2bytes(cc, dst);
                dst += utf_char2len(cc) - 1;

                {
                    int clen = utf_ptr2len(src - 1);

                    /* If the character length is shorter than "totlen", there
                     * are composing characters; copy them as-is. */
                    if (clen < totlen)
                    {
                        if (copy)
                            mch_memmove(dst + 1, src - 1 + clen, (size_t)(totlen - clen));
                        dst += totlen - clen;
                    }
                }

                src += totlen - 1;
            }

            dst++;
        }
        else
        {
            if (REG_MULTI)
            {
                clnum = reg_mmatch->startpos[no].lnum;
                if (clnum < 0 || reg_mmatch->endpos[no].lnum < 0)
                    s = NULL;
                else
                {
                    s = reg_getline(clnum) + reg_mmatch->startpos[no].col;
                    if (reg_mmatch->endpos[no].lnum == clnum)
                        len = reg_mmatch->endpos[no].col - reg_mmatch->startpos[no].col;
                    else
                        len = (int)STRLEN(s);
                }
            }
            else
            {
                s = reg_match->startp[no];
                if (reg_match->endp[no] == NULL)
                    s = NULL;
                else
                    len = (int)(reg_match->endp[no] - s);
            }
            if (s != NULL)
            {
                for (;;)
                {
                    if (len == 0)
                    {
                        if (REG_MULTI)
                        {
                            if (reg_mmatch->endpos[no].lnum == clnum)
                                break;
                            if (copy)
                                *dst = CAR;
                            ++dst;
                            s = reg_getline(++clnum);
                            if (reg_mmatch->endpos[no].lnum == clnum)
                                len = reg_mmatch->endpos[no].col;
                            else
                                len = (int)STRLEN(s);
                        }
                        else
                            break;
                    }
                    else if (*s == NUL) /* we hit NUL. */
                    {
                        if (copy)
                            EMSG((char *)e_re_damg);
                        goto exit;
                    }
                    else
                    {
                        if (backslash && (*s == CAR || *s == '\\'))
                        {
                            /*
                             * Insert a backslash in front of a CR, otherwise
                             * it will be replaced by a line break.
                             * Number of backslashes will be halved later, double them here.
                             */
                            if (copy)
                            {
                                dst[0] = '\\';
                                dst[1] = *s;
                            }
                            dst += 2;
                        }
                        else
                        {
                            c = utf_ptr2char(s);

                            if (func_one != (fptr_T)NULL)
                                /* Turbo C complains without the typecast */
                                func_one = (fptr_T)(func_one(&cc, c));
                            else if (func_all != (fptr_T)NULL)
                                /* Turbo C complains without the typecast */
                                func_all = (fptr_T)(func_all(&cc, c));
                            else /* just copy */
                                cc = c;

                            {
                                int l;

                                /* Copy composing characters separately, one at a time. */
                                l = utf_ptr2len(s) - 1;

                                s += l;
                                len -= l;
                                if (copy)
                                    utf_char2bytes(cc, dst);
                                dst += utf_char2len(cc) - 1;
                            }

                            dst++;
                        }

                        ++s;
                        --len;
                    }
                }
            }
            no = -1;
        }
      }
    if (copy)
        *dst = NUL;

exit:
    return (int)((dst - dest) + 1);
}

static char_u *reg_getline_submatch(linenr_T lnum);

/*
 * Call reg_getline() with the line numbers from the submatch.  If a
 * substitute() was used the reg_maxline and other values have been overwritten.
 */
    static char_u *
reg_getline_submatch(lnum)
    linenr_T    lnum;
{
    char_u *s;
    linenr_T save_first = reg_firstlnum;
    linenr_T save_max = reg_maxline;

    reg_firstlnum = submatch_firstlnum;
    reg_maxline = submatch_maxline;

    s = reg_getline(lnum);

    reg_firstlnum = save_first;
    reg_maxline = save_max;
    return s;
}

/*
 * Used for the submatch() function: get the string from the n'th submatch in allocated memory.
 * Returns NULL when not in a ":s" command and for a non-existing submatch.
 */
    char_u *
reg_submatch(no)
    int         no;
{
    char_u      *retval = NULL;
    char_u      *s;
    int         len;
    int         round;
    linenr_T    lnum;

    if (!can_f_submatch || no < 0)
        return NULL;

    if (submatch_match == NULL)
    {
        /*
         * First round: compute the length and allocate memory.
         * Second round: copy the text.
         */
        for (round = 1; round <= 2; ++round)
        {
            lnum = submatch_mmatch->startpos[no].lnum;
            if (lnum < 0 || submatch_mmatch->endpos[no].lnum < 0)
                return NULL;

            s = reg_getline_submatch(lnum) + submatch_mmatch->startpos[no].col;
            if (s == NULL)  /* anti-crash check, cannot happen? */
                break;
            if (submatch_mmatch->endpos[no].lnum == lnum)
            {
                /* Within one line: take form start to end col. */
                len = submatch_mmatch->endpos[no].col - submatch_mmatch->startpos[no].col;
                if (round == 2)
                    vim_strncpy(retval, s, len);
                ++len;
            }
            else
            {
                /* Multiple lines: take start line from start col, middle
                 * lines completely and end line up to end col. */
                len = (int)STRLEN(s);
                if (round == 2)
                {
                    STRCPY(retval, s);
                    retval[len] = '\n';
                }
                ++len;
                ++lnum;
                while (lnum < submatch_mmatch->endpos[no].lnum)
                {
                    s = reg_getline_submatch(lnum++);
                    if (round == 2)
                        STRCPY(retval + len, s);
                    len += (int)STRLEN(s);
                    if (round == 2)
                        retval[len] = '\n';
                    ++len;
                }
                if (round == 2)
                    STRNCPY(retval + len, reg_getline_submatch(lnum), submatch_mmatch->endpos[no].col);
                len += submatch_mmatch->endpos[no].col;
                if (round == 2)
                    retval[len] = NUL;
                ++len;
            }

            if (retval == NULL)
            {
                retval = lalloc((long_u)len, TRUE);
                if (retval == NULL)
                    return NULL;
            }
        }
    }
    else
    {
        s = submatch_match->startp[no];
        if (s == NULL || submatch_match->endp[no] == NULL)
            retval = NULL;
        else
            retval = vim_strnsave(s, (int)(submatch_match->endp[no] - s));
    }

    return retval;
}

/*
 * Used for the submatch() function with the optional non-zero argument: get
 * the list of strings from the n'th submatch in allocated memory with NULs
 * represented in NLs.
 * Returns a list of allocated strings.  Returns NULL when not in a ":s"
 * command, for a non-existing submatch and for any error.
 */
    list_T *
reg_submatch_list(no)
    int         no;
{
    char_u      *s;
    linenr_T    slnum;
    linenr_T    elnum;
    colnr_T     scol;
    colnr_T     ecol;
    int         i;
    list_T      *list;
    int         error = FALSE;

    if (!can_f_submatch || no < 0)
        return NULL;

    if (submatch_match == NULL)
    {
        slnum = submatch_mmatch->startpos[no].lnum;
        elnum = submatch_mmatch->endpos[no].lnum;
        if (slnum < 0 || elnum < 0)
            return NULL;

        scol = submatch_mmatch->startpos[no].col;
        ecol = submatch_mmatch->endpos[no].col;

        list = list_alloc();
        if (list == NULL)
            return NULL;

        s = reg_getline_submatch(slnum) + scol;
        if (slnum == elnum)
        {
            if (list_append_string(list, s, ecol - scol) == FAIL)
                error = TRUE;
        }
        else
        {
            if (list_append_string(list, s, -1) == FAIL)
                error = TRUE;
            for (i = 1; i < elnum - slnum; i++)
            {
                s = reg_getline_submatch(slnum + i);
                if (list_append_string(list, s, -1) == FAIL)
                    error = TRUE;
            }
            s = reg_getline_submatch(elnum);
            if (list_append_string(list, s, ecol) == FAIL)
                error = TRUE;
        }
    }
    else
    {
        s = submatch_match->startp[no];
        if (s == NULL || submatch_match->endp[no] == NULL)
            return NULL;
        list = list_alloc();
        if (list == NULL)
            return NULL;
        if (list_append_string(list, s, (int)(submatch_match->endp[no] - s)) == FAIL)
            error = TRUE;
    }

    if (error)
    {
        list_free(list, TRUE);
        return NULL;
    }
    return list;
}

static regengine_T bt_regengine =
{
    bt_regcomp,
    bt_regfree,
    bt_regexec_nl,
    bt_regexec_multi,
    (char_u *)""
};

/* ----------------------------------------------------------------------- */

/*
 * NFA regular expression implementation.
 *
 * This file is included in "regexp.c".
 */

/* Added to NFA_ANY - NFA_NUPPER_IC to include a NL. */
#define NFA_ADD_NL              31

enum
{
    NFA_SPLIT = -1024,
    NFA_MATCH,
    NFA_EMPTY,                      /* matches 0-length */

    NFA_START_COLL,                 /* [abc] start */
    NFA_END_COLL,                   /* [abc] end */
    NFA_START_NEG_COLL,             /* [^abc] start */
    NFA_END_NEG_COLL,               /* [^abc] end (postfix only) */
    NFA_RANGE,                      /* range of the two previous items
                                     * (postfix only) */
    NFA_RANGE_MIN,                  /* low end of a range */
    NFA_RANGE_MAX,                  /* high end of a range */

    NFA_CONCAT,                     /* concatenate two previous items (postfix
                                     * only) */
    NFA_OR,                         /* \| (postfix only) */
    NFA_STAR,                       /* greedy * (posfix only) */
    NFA_STAR_NONGREEDY,             /* non-greedy * (postfix only) */
    NFA_QUEST,                      /* greedy \? (postfix only) */
    NFA_QUEST_NONGREEDY,            /* non-greedy \? (postfix only) */

    NFA_BOL,                        /* ^    Begin line */
    NFA_EOL,                        /* $    End line */
    NFA_BOW,                        /* \<   Begin word */
    NFA_EOW,                        /* \>   End word */
    NFA_BOF,                        /* \%^  Begin file */
    NFA_EOF,                        /* \%$  End file */
    NFA_NEWL,
    NFA_ZSTART,                     /* Used for \zs */
    NFA_ZEND,                       /* Used for \ze */
    NFA_NOPEN,                      /* Start of subexpression marked with \%( */
    NFA_NCLOSE,                     /* End of subexpr. marked with \%( ... \) */
    NFA_START_INVISIBLE,
    NFA_START_INVISIBLE_FIRST,
    NFA_START_INVISIBLE_NEG,
    NFA_START_INVISIBLE_NEG_FIRST,
    NFA_START_INVISIBLE_BEFORE,
    NFA_START_INVISIBLE_BEFORE_FIRST,
    NFA_START_INVISIBLE_BEFORE_NEG,
    NFA_START_INVISIBLE_BEFORE_NEG_FIRST,
    NFA_START_PATTERN,
    NFA_END_INVISIBLE,
    NFA_END_INVISIBLE_NEG,
    NFA_END_PATTERN,
    NFA_COMPOSING,                  /* Next nodes in NFA are part of the
                                       composing multibyte char */
    NFA_END_COMPOSING,              /* End of a composing char in the NFA */
    NFA_ANY_COMPOSING,              /* \%C: Any composing characters. */
    NFA_OPT_CHARS,                  /* \%[abc] */

    /* The following are used only in the postfix form, not in the NFA */
    NFA_PREV_ATOM_NO_WIDTH,         /* Used for \@= */
    NFA_PREV_ATOM_NO_WIDTH_NEG,     /* Used for \@! */
    NFA_PREV_ATOM_JUST_BEFORE,      /* Used for \@<= */
    NFA_PREV_ATOM_JUST_BEFORE_NEG,  /* Used for \@<! */
    NFA_PREV_ATOM_LIKE_PATTERN,     /* Used for \@> */

    NFA_BACKREF1,                   /* \1 */
    NFA_BACKREF2,                   /* \2 */
    NFA_BACKREF3,                   /* \3 */
    NFA_BACKREF4,                   /* \4 */
    NFA_BACKREF5,                   /* \5 */
    NFA_BACKREF6,                   /* \6 */
    NFA_BACKREF7,                   /* \7 */
    NFA_BACKREF8,                   /* \8 */
    NFA_BACKREF9,                   /* \9 */
    NFA_ZREF1,                      /* \z1 */
    NFA_ZREF2,                      /* \z2 */
    NFA_ZREF3,                      /* \z3 */
    NFA_ZREF4,                      /* \z4 */
    NFA_ZREF5,                      /* \z5 */
    NFA_ZREF6,                      /* \z6 */
    NFA_ZREF7,                      /* \z7 */
    NFA_ZREF8,                      /* \z8 */
    NFA_ZREF9,                      /* \z9 */
    NFA_SKIP,                       /* Skip characters */

    NFA_MOPEN,
    NFA_MOPEN1,
    NFA_MOPEN2,
    NFA_MOPEN3,
    NFA_MOPEN4,
    NFA_MOPEN5,
    NFA_MOPEN6,
    NFA_MOPEN7,
    NFA_MOPEN8,
    NFA_MOPEN9,

    NFA_MCLOSE,
    NFA_MCLOSE1,
    NFA_MCLOSE2,
    NFA_MCLOSE3,
    NFA_MCLOSE4,
    NFA_MCLOSE5,
    NFA_MCLOSE6,
    NFA_MCLOSE7,
    NFA_MCLOSE8,
    NFA_MCLOSE9,

    NFA_ZOPEN,
    NFA_ZOPEN1,
    NFA_ZOPEN2,
    NFA_ZOPEN3,
    NFA_ZOPEN4,
    NFA_ZOPEN5,
    NFA_ZOPEN6,
    NFA_ZOPEN7,
    NFA_ZOPEN8,
    NFA_ZOPEN9,

    NFA_ZCLOSE,
    NFA_ZCLOSE1,
    NFA_ZCLOSE2,
    NFA_ZCLOSE3,
    NFA_ZCLOSE4,
    NFA_ZCLOSE5,
    NFA_ZCLOSE6,
    NFA_ZCLOSE7,
    NFA_ZCLOSE8,
    NFA_ZCLOSE9,

    /* NFA_FIRST_NL */
    NFA_ANY,            /*      Match any one character. */
    NFA_IDENT,          /*      Match identifier char */
    NFA_SIDENT,         /*      Match identifier char but no digit */
    NFA_KWORD,          /*      Match keyword char */
    NFA_SKWORD,         /*      Match word char but no digit */
    NFA_FNAME,          /*      Match file name char */
    NFA_SFNAME,         /*      Match file name char but no digit */
    NFA_PRINT,          /*      Match printable char */
    NFA_SPRINT,         /*      Match printable char but no digit */
    NFA_WHITE,          /*      Match whitespace char */
    NFA_NWHITE,         /*      Match non-whitespace char */
    NFA_DIGIT,          /*      Match digit char */
    NFA_NDIGIT,         /*      Match non-digit char */
    NFA_HEX,            /*      Match hex char */
    NFA_NHEX,           /*      Match non-hex char */
    NFA_OCTAL,          /*      Match octal char */
    NFA_NOCTAL,         /*      Match non-octal char */
    NFA_WORD,           /*      Match word char */
    NFA_NWORD,          /*      Match non-word char */
    NFA_HEAD,           /*      Match head char */
    NFA_NHEAD,          /*      Match non-head char */
    NFA_ALPHA,          /*      Match alpha char */
    NFA_NALPHA,         /*      Match non-alpha char */
    NFA_LOWER,          /*      Match lowercase char */
    NFA_NLOWER,         /*      Match non-lowercase char */
    NFA_UPPER,          /*      Match uppercase char */
    NFA_NUPPER,         /*      Match non-uppercase char */
    NFA_LOWER_IC,       /*      Match [a-z] */
    NFA_NLOWER_IC,      /*      Match [^a-z] */
    NFA_UPPER_IC,       /*      Match [A-Z] */
    NFA_NUPPER_IC,      /*      Match [^A-Z] */

    NFA_FIRST_NL = NFA_ANY + NFA_ADD_NL,
    NFA_LAST_NL = NFA_NUPPER_IC + NFA_ADD_NL,

    NFA_CURSOR,         /*      Match cursor pos */
    NFA_LNUM,           /*      Match line number */
    NFA_LNUM_GT,        /*      Match > line number */
    NFA_LNUM_LT,        /*      Match < line number */
    NFA_COL,            /*      Match cursor column */
    NFA_COL_GT,         /*      Match > cursor column */
    NFA_COL_LT,         /*      Match < cursor column */
    NFA_VCOL,           /*      Match cursor virtual column */
    NFA_VCOL_GT,        /*      Match > cursor virtual column */
    NFA_VCOL_LT,        /*      Match < cursor virtual column */
    NFA_MARK,           /*      Match mark */
    NFA_MARK_GT,        /*      Match > mark */
    NFA_MARK_LT,        /*      Match < mark */
    NFA_VISUAL,         /*      Match Visual area */

    /* Character classes [:alnum:] etc */
    NFA_CLASS_ALNUM,
    NFA_CLASS_ALPHA,
    NFA_CLASS_BLANK,
    NFA_CLASS_CNTRL,
    NFA_CLASS_DIGIT,
    NFA_CLASS_GRAPH,
    NFA_CLASS_LOWER,
    NFA_CLASS_PRINT,
    NFA_CLASS_PUNCT,
    NFA_CLASS_SPACE,
    NFA_CLASS_UPPER,
    NFA_CLASS_XDIGIT,
    NFA_CLASS_TAB,
    NFA_CLASS_RETURN,
    NFA_CLASS_BACKSPACE,
    NFA_CLASS_ESCAPE
};

/* Keep in sync with classchars. */
static int nfa_classcodes[] =
{
    NFA_ANY, NFA_IDENT, NFA_SIDENT, NFA_KWORD,NFA_SKWORD,
    NFA_FNAME, NFA_SFNAME, NFA_PRINT, NFA_SPRINT,
    NFA_WHITE, NFA_NWHITE, NFA_DIGIT, NFA_NDIGIT,
    NFA_HEX, NFA_NHEX, NFA_OCTAL, NFA_NOCTAL,
    NFA_WORD, NFA_NWORD, NFA_HEAD, NFA_NHEAD,
    NFA_ALPHA, NFA_NALPHA, NFA_LOWER, NFA_NLOWER,
    NFA_UPPER, NFA_NUPPER
};

static char_u e_nul_found[] = "E865: (NFA) Regexp end encountered prematurely";
static char_u e_misplaced[] = "E866: (NFA regexp) Misplaced %c";
static char_u e_ill_char_class[] = "E877: (NFA regexp) Invalid character class: %ld";

/* re_flags passed to nfa_regcomp() */
static int nfa_re_flags;

/* NFA regexp \ze operator encountered. */
static int nfa_has_zend;

/* NFA regexp \1 .. \9 encountered. */
static int nfa_has_backref;

/* NFA regexp has \z( ), set zsubexpr. */
static int nfa_has_zsubexpr;

/* Number of sub expressions actually being used during execution. 1 if only
 * the whole match (subexpr 0) is used. */
static int nfa_nsubexpr;

static int *post_start;  /* holds the postfix form of r.e. */
static int *post_end;
static int *post_ptr;

static int nstate;      /* Number of states in the NFA. Also used when
                         * executing. */
static int istate;      /* Index in the state vector, used in alloc_state() */

/* If not NULL match must end at this position */
static save_se_T *nfa_endp = NULL;

/* listid is global, so that it increases on recursive calls to
 * nfa_regmatch(), which means we don't have to clear the lastlist field of all the states. */
static int nfa_listid;
static int nfa_alt_listid;

/* 0 for first call to nfa_regmatch(), 1 for recursive call. */
static int nfa_ll_index = 0;

static int nfa_regcomp_start(char_u *expr, int re_flags);
static int nfa_get_reganch(nfa_state_T *start, int depth);
static int nfa_get_regstart(nfa_state_T *start, int depth);
static char_u *nfa_get_match_text(nfa_state_T *start);
static int realloc_post_list(void);
static int nfa_recognize_char_class(char_u *start, char_u *end, int extra_newl);
static int nfa_emit_equi_class(int c);
static int nfa_regatom(void);
static int nfa_regpiece(void);
static int nfa_regconcat(void);
static int nfa_regbranch(void);
static int nfa_reg(int paren);
static int *re2post(void);
static nfa_state_T *alloc_state(int c, nfa_state_T *out, nfa_state_T *out1);
static void st_error(int *postfix, int *end, int *p);
static int nfa_max_width(nfa_state_T *startstate, int depth);
static nfa_state_T *post2nfa(int *postfix, int *end, int nfa_calc_size);
static void nfa_postprocess(nfa_regprog_T *prog);
static int check_char_class(int class, int c);
static void nfa_save_listids(nfa_regprog_T *prog, int *list);
static void nfa_restore_listids(nfa_regprog_T *prog, int *list);
static int nfa_re_num_cmp(long_u val, int op, long_u pos);
static long nfa_regtry(nfa_regprog_T *prog, colnr_T col, proftime_T *tm);
static long nfa_regexec_both(char_u *line, colnr_T col, proftime_T *tm);
static regprog_T *nfa_regcomp(char_u *expr, int re_flags);
static void nfa_regfree(regprog_T *prog);
static int  nfa_regexec_nl(regmatch_T *rmp, char_u *line, colnr_T col, int line_lbr);
static long nfa_regexec_multi(regmmatch_T *rmp, win_T *win, buf_T *buf, linenr_T lnum, colnr_T col, proftime_T *tm);
static int match_follows(nfa_state_T *startstate, int depth);
static int failure_chance(nfa_state_T *state, int depth);

/* helper functions used when doing re2post() ... regatom() parsing */
#define EMIT(c) do {                            \
                    if (post_ptr >= post_end && realloc_post_list() == FAIL) \
                        return FAIL;            \
                    *post_ptr++ = c;            \
                } while (0)

/*
 * Initialize internal variables before NFA compilation.
 * Return OK on success, FAIL otherwise.
 */
    static int
nfa_regcomp_start(expr, re_flags)
    char_u      *expr;
    int         re_flags;           /* see vim_regcomp() */
{
    size_t      postfix_size;
    int         nstate_max;

    nstate = 0;
    istate = 0;
    /* A reasonable estimation for maximum size */
    nstate_max = (int)(STRLEN(expr) + 1) * 25;

    /* Some items blow up in size, such as [A-z].  Add more space for that.
     * When it is still not enough realloc_post_list() will be used. */
    nstate_max += 1000;

    /* Size for postfix representation of expr. */
    postfix_size = sizeof(int) * nstate_max;

    post_start = (int *)lalloc(postfix_size, TRUE);
    if (post_start == NULL)
        return FAIL;
    post_ptr = post_start;
    post_end = post_start + nstate_max;
    nfa_has_zend = FALSE;
    nfa_has_backref = FALSE;

    /* shared with BT engine */
    regcomp_start(expr, re_flags);

    return OK;
}

/*
 * Figure out if the NFA state list starts with an anchor, must match at start of the line.
 */
    static int
nfa_get_reganch(start, depth)
    nfa_state_T *start;
    int         depth;
{
    nfa_state_T *p = start;

    if (depth > 4)
        return 0;

    while (p != NULL)
    {
        switch (p->c)
        {
            case NFA_BOL:
            case NFA_BOF:
                return 1; /* yes! */

            case NFA_ZSTART:
            case NFA_ZEND:
            case NFA_CURSOR:
            case NFA_VISUAL:

            case NFA_MOPEN:
            case NFA_MOPEN1:
            case NFA_MOPEN2:
            case NFA_MOPEN3:
            case NFA_MOPEN4:
            case NFA_MOPEN5:
            case NFA_MOPEN6:
            case NFA_MOPEN7:
            case NFA_MOPEN8:
            case NFA_MOPEN9:
            case NFA_NOPEN:
            case NFA_ZOPEN:
            case NFA_ZOPEN1:
            case NFA_ZOPEN2:
            case NFA_ZOPEN3:
            case NFA_ZOPEN4:
            case NFA_ZOPEN5:
            case NFA_ZOPEN6:
            case NFA_ZOPEN7:
            case NFA_ZOPEN8:
            case NFA_ZOPEN9:
                p = p->out;
                break;

            case NFA_SPLIT:
                return nfa_get_reganch(p->out, depth + 1) && nfa_get_reganch(p->out1, depth + 1);

            default:
                return 0; /* noooo */
        }
    }
    return 0;
}

/*
 * Figure out if the NFA state list starts with a character which must match
 * at start of the match.
 */
    static int
nfa_get_regstart(start, depth)
    nfa_state_T *start;
    int         depth;
{
    nfa_state_T *p = start;

    if (depth > 4)
        return 0;

    while (p != NULL)
    {
        switch (p->c)
        {
            /* all kinds of zero-width matches */
            case NFA_BOL:
            case NFA_BOF:
            case NFA_BOW:
            case NFA_EOW:
            case NFA_ZSTART:
            case NFA_ZEND:
            case NFA_CURSOR:
            case NFA_VISUAL:
            case NFA_LNUM:
            case NFA_LNUM_GT:
            case NFA_LNUM_LT:
            case NFA_COL:
            case NFA_COL_GT:
            case NFA_COL_LT:
            case NFA_VCOL:
            case NFA_VCOL_GT:
            case NFA_VCOL_LT:
            case NFA_MARK:
            case NFA_MARK_GT:
            case NFA_MARK_LT:

            case NFA_MOPEN:
            case NFA_MOPEN1:
            case NFA_MOPEN2:
            case NFA_MOPEN3:
            case NFA_MOPEN4:
            case NFA_MOPEN5:
            case NFA_MOPEN6:
            case NFA_MOPEN7:
            case NFA_MOPEN8:
            case NFA_MOPEN9:
            case NFA_NOPEN:
            case NFA_ZOPEN:
            case NFA_ZOPEN1:
            case NFA_ZOPEN2:
            case NFA_ZOPEN3:
            case NFA_ZOPEN4:
            case NFA_ZOPEN5:
            case NFA_ZOPEN6:
            case NFA_ZOPEN7:
            case NFA_ZOPEN8:
            case NFA_ZOPEN9:
                p = p->out;
                break;

            case NFA_SPLIT:
            {
                int c1 = nfa_get_regstart(p->out, depth + 1);
                int c2 = nfa_get_regstart(p->out1, depth + 1);

                if (c1 == c2)
                    return c1; /* yes! */

                return 0;
            }

            default:
                if (p->c > 0)
                    return p->c; /* yes! */

                return 0;
        }
    }
    return 0;
}

/*
 * Figure out if the NFA state list contains just literal text and nothing
 * else.  If so return a string in allocated memory with what must match after
 * regstart.  Otherwise return NULL.
 */
    static char_u *
nfa_get_match_text(start)
    nfa_state_T *start;
{
    nfa_state_T *p = start;
    int         len = 0;
    char_u      *ret;
    char_u      *s;

    if (p->c != NFA_MOPEN)
        return NULL; /* just in case */
    p = p->out;
    while (p->c > 0)
    {
        len += utf_char2len(p->c);
        p = p->out;
    }
    if (p->c != NFA_MCLOSE || p->out->c != NFA_MATCH)
        return NULL;

    ret = alloc(len);
    if (ret != NULL)
    {
        len = 0;
        p = start->out->out; /* skip first char, it goes into regstart */
        s = ret;
        while (p->c > 0)
        {
            s += utf_char2bytes(p->c, s);
            p = p->out;
        }
        *s = NUL;
    }
    return ret;
}

/*
 * Allocate more space for post_start.  Called when
 * running above the estimated number of states.
 */
    static int
realloc_post_list()
{
    int   nstate_max = (int)(post_end - post_start);
    int   new_max = nstate_max + 1000;
    int   *new_start;
    int   *old_start;

    new_start = (int *)lalloc(new_max * sizeof(int), TRUE);
    if (new_start == NULL)
        return FAIL;
    mch_memmove(new_start, post_start, nstate_max * sizeof(int));
    old_start = post_start;
    post_start = new_start;
    post_ptr = new_start + (post_ptr - old_start);
    post_end = post_start + new_max;
    vim_free(old_start);
    return OK;
}

/*
 * Search between "start" and "end" and try to recognize a
 * character class in expanded form. For example [0-9].
 * On success, return the id the character class to be emitted.
 * On failure, return 0 (=FAIL)
 * Start points to the first char of the range, while end should point
 * to the closing brace.
 * Keep in mind that 'ignorecase' applies at execution time, thus [a-z] may
 * need to be interpreted as [a-zA-Z].
 */
    static int
nfa_recognize_char_class(start, end, extra_newl)
    char_u  *start;
    char_u  *end;
    int     extra_newl;
{
#define CLASS_not            0x80
#define CLASS_af             0x40
#define CLASS_AF             0x20
#define CLASS_az             0x10
#define CLASS_AZ             0x08
#define CLASS_o7             0x04
#define CLASS_o9             0x02
#define CLASS_underscore     0x01

    int         newl = FALSE;
    char_u      *p;
    int         config = 0;

    if (extra_newl == TRUE)
        newl = TRUE;

    if (*end != ']')
        return FAIL;
    p = start;
    if (*p == '^')
    {
        config |= CLASS_not;
        p++;
    }

    while (p < end)
    {
        if (p + 2 < end && *(p + 1) == '-')
        {
            switch (*p)
            {
                case '0':
                    if (*(p + 2) == '9')
                    {
                        config |= CLASS_o9;
                        break;
                    }
                    else if (*(p + 2) == '7')
                    {
                        config |= CLASS_o7;
                        break;
                    }
                case 'a':
                    if (*(p + 2) == 'z')
                    {
                        config |= CLASS_az;
                        break;
                    }
                    else if (*(p + 2) == 'f')
                    {
                        config |= CLASS_af;
                        break;
                    }
                case 'A':
                    if (*(p + 2) == 'Z')
                    {
                        config |= CLASS_AZ;
                        break;
                    }
                    else if (*(p + 2) == 'F')
                    {
                        config |= CLASS_AF;
                        break;
                    }
                /* FALLTHROUGH */
                default:
                    return FAIL;
            }
            p += 3;
        }
        else if (p + 1 < end && *p == '\\' && *(p + 1) == 'n')
        {
            newl = TRUE;
            p += 2;
        }
        else if (*p == '_')
        {
            config |= CLASS_underscore;
            p ++;
        }
        else if (*p == '\n')
        {
            newl = TRUE;
            p ++;
        }
        else
            return FAIL;
    }

    if (p != end)
        return FAIL;

    if (newl == TRUE)
        extra_newl = NFA_ADD_NL;

    switch (config)
    {
        case CLASS_o9:
            return extra_newl + NFA_DIGIT;
        case CLASS_not |  CLASS_o9:
            return extra_newl + NFA_NDIGIT;
        case CLASS_af | CLASS_AF | CLASS_o9:
            return extra_newl + NFA_HEX;
        case CLASS_not | CLASS_af | CLASS_AF | CLASS_o9:
            return extra_newl + NFA_NHEX;
        case CLASS_o7:
            return extra_newl + NFA_OCTAL;
        case CLASS_not | CLASS_o7:
            return extra_newl + NFA_NOCTAL;
        case CLASS_az | CLASS_AZ | CLASS_o9 | CLASS_underscore:
            return extra_newl + NFA_WORD;
        case CLASS_not | CLASS_az | CLASS_AZ | CLASS_o9 | CLASS_underscore:
            return extra_newl + NFA_NWORD;
        case CLASS_az | CLASS_AZ | CLASS_underscore:
            return extra_newl + NFA_HEAD;
        case CLASS_not | CLASS_az | CLASS_AZ | CLASS_underscore:
            return extra_newl + NFA_NHEAD;
        case CLASS_az | CLASS_AZ:
            return extra_newl + NFA_ALPHA;
        case CLASS_not | CLASS_az | CLASS_AZ:
            return extra_newl + NFA_NALPHA;
        case CLASS_az:
           return extra_newl + NFA_LOWER_IC;
        case CLASS_not | CLASS_az:
            return extra_newl + NFA_NLOWER_IC;
        case CLASS_AZ:
            return extra_newl + NFA_UPPER_IC;
        case CLASS_not | CLASS_AZ:
            return extra_newl + NFA_NUPPER_IC;
    }
    return FAIL;
}

/*
 * Produce the bytes for equivalence class "c".
 * Currently only handles latin1, latin9 and utf-8.
 * Emits bytes in postfix notation: 'a,b,NFA_OR,c,NFA_OR' is
 * equivalent to 'a OR b OR c'
 *
 * NOTE! When changing this function, also update reg_equi_class()
 */
    static int
nfa_emit_equi_class(c)
    int     c;
{
#define EMIT2(c)    EMIT(c); EMIT(NFA_CONCAT);
#define EMITMBC(c)  EMIT(c); EMIT(NFA_CONCAT);

    switch (c)
    {
        case 'A': case 0300: case 0301: case 0302:
        case 0303: case 0304: case 0305:
        CASEMBC(0x100) CASEMBC(0x102) CASEMBC(0x104) CASEMBC(0x1cd)
        CASEMBC(0x1de) CASEMBC(0x1e0) CASEMBC(0x1ea2)
                EMIT2('A'); EMIT2(0300); EMIT2(0301); EMIT2(0302);
                EMIT2(0303); EMIT2(0304); EMIT2(0305);
                EMITMBC(0x100) EMITMBC(0x102) EMITMBC(0x104)
                EMITMBC(0x1cd) EMITMBC(0x1de) EMITMBC(0x1e0)
                EMITMBC(0x1ea2)
                return OK;

        case 'B': CASEMBC(0x1e02) CASEMBC(0x1e06)
                EMIT2('B'); EMITMBC(0x1e02) EMITMBC(0x1e06)
                return OK;

        case 'C': case 0307:
        CASEMBC(0x106) CASEMBC(0x108) CASEMBC(0x10a) CASEMBC(0x10c)
                EMIT2('C'); EMIT2(0307); EMITMBC(0x106) EMITMBC(0x108)
                EMITMBC(0x10a) EMITMBC(0x10c)
                return OK;

        case 'D': CASEMBC(0x10e) CASEMBC(0x110) CASEMBC(0x1e0a)
        CASEMBC(0x1e0e) CASEMBC(0x1e10)
                EMIT2('D'); EMITMBC(0x10e) EMITMBC(0x110) EMITMBC(0x1e0a)
                EMITMBC(0x1e0e) EMITMBC(0x1e10)
                return OK;

        case 'E': case 0310: case 0311: case 0312: case 0313:
        CASEMBC(0x112) CASEMBC(0x114) CASEMBC(0x116) CASEMBC(0x118)
        CASEMBC(0x11a) CASEMBC(0x1eba) CASEMBC(0x1ebc)
                EMIT2('E'); EMIT2(0310); EMIT2(0311); EMIT2(0312);
                EMIT2(0313);
                EMITMBC(0x112) EMITMBC(0x114) EMITMBC(0x116)
                EMITMBC(0x118) EMITMBC(0x11a) EMITMBC(0x1eba)
                EMITMBC(0x1ebc)
                return OK;

        case 'F': CASEMBC(0x1e1e)
                EMIT2('F'); EMITMBC(0x1e1e)
                return OK;

        case 'G': CASEMBC(0x11c) CASEMBC(0x11e) CASEMBC(0x120)
        CASEMBC(0x122) CASEMBC(0x1e4) CASEMBC(0x1e6) CASEMBC(0x1f4)
        CASEMBC(0x1e20)
                EMIT2('G'); EMITMBC(0x11c) EMITMBC(0x11e) EMITMBC(0x120)
                EMITMBC(0x122) EMITMBC(0x1e4) EMITMBC(0x1e6)
                EMITMBC(0x1f4) EMITMBC(0x1e20)
                return OK;

        case 'H': CASEMBC(0x124) CASEMBC(0x126) CASEMBC(0x1e22)
        CASEMBC(0x1e26) CASEMBC(0x1e28)
                EMIT2('H'); EMITMBC(0x124) EMITMBC(0x126) EMITMBC(0x1e22)
                EMITMBC(0x1e26) EMITMBC(0x1e28)
                return OK;

        case 'I': case 0314: case 0315: case 0316: case 0317:
        CASEMBC(0x128) CASEMBC(0x12a) CASEMBC(0x12c) CASEMBC(0x12e)
        CASEMBC(0x130) CASEMBC(0x1cf) CASEMBC(0x1ec8)
                EMIT2('I'); EMIT2(0314); EMIT2(0315); EMIT2(0316);
                EMIT2(0317); EMITMBC(0x128) EMITMBC(0x12a)
                EMITMBC(0x12c) EMITMBC(0x12e) EMITMBC(0x130)
                EMITMBC(0x1cf) EMITMBC(0x1ec8)
                return OK;

        case 'J': CASEMBC(0x134)
                EMIT2('J'); EMITMBC(0x134)
                return OK;

        case 'K': CASEMBC(0x136) CASEMBC(0x1e8) CASEMBC(0x1e30)
        CASEMBC(0x1e34)
                EMIT2('K'); EMITMBC(0x136) EMITMBC(0x1e8) EMITMBC(0x1e30)
                EMITMBC(0x1e34)
                return OK;

        case 'L': CASEMBC(0x139) CASEMBC(0x13b) CASEMBC(0x13d)
        CASEMBC(0x13f) CASEMBC(0x141) CASEMBC(0x1e3a)
                EMIT2('L'); EMITMBC(0x139) EMITMBC(0x13b) EMITMBC(0x13d)
                EMITMBC(0x13f) EMITMBC(0x141) EMITMBC(0x1e3a)
                return OK;

        case 'M': CASEMBC(0x1e3e) CASEMBC(0x1e40)
                EMIT2('M'); EMITMBC(0x1e3e) EMITMBC(0x1e40)
                return OK;

        case 'N': case 0321:
        CASEMBC(0x143) CASEMBC(0x145) CASEMBC(0x147) CASEMBC(0x1e44)
        CASEMBC(0x1e48)
                EMIT2('N'); EMIT2(0321); EMITMBC(0x143) EMITMBC(0x145)
                EMITMBC(0x147) EMITMBC(0x1e44) EMITMBC(0x1e48)
                return OK;

        case 'O': case 0322: case 0323: case 0324: case 0325:
        case 0326: case 0330:
        CASEMBC(0x14c) CASEMBC(0x14e) CASEMBC(0x150) CASEMBC(0x1a0)
        CASEMBC(0x1d1) CASEMBC(0x1ea) CASEMBC(0x1ec) CASEMBC(0x1ece)
                EMIT2('O'); EMIT2(0322); EMIT2(0323); EMIT2(0324);
                EMIT2(0325); EMIT2(0326); EMIT2(0330);
                EMITMBC(0x14c) EMITMBC(0x14e) EMITMBC(0x150)
                EMITMBC(0x1a0) EMITMBC(0x1d1) EMITMBC(0x1ea)
                EMITMBC(0x1ec) EMITMBC(0x1ece)
                return OK;

        case 'P': case 0x1e54: case 0x1e56:
                EMIT2('P'); EMITMBC(0x1e54) EMITMBC(0x1e56)
                return OK;

        case 'R': CASEMBC(0x154) CASEMBC(0x156) CASEMBC(0x158)
        CASEMBC(0x1e58) CASEMBC(0x1e5e)
                EMIT2('R'); EMITMBC(0x154) EMITMBC(0x156) EMITMBC(0x158)
                EMITMBC(0x1e58) EMITMBC(0x1e5e)
                return OK;

        case 'S': CASEMBC(0x15a) CASEMBC(0x15c) CASEMBC(0x15e)
        CASEMBC(0x160) CASEMBC(0x1e60)
                EMIT2('S'); EMITMBC(0x15a) EMITMBC(0x15c) EMITMBC(0x15e)
                EMITMBC(0x160) EMITMBC(0x1e60)
                return OK;

        case 'T': CASEMBC(0x162) CASEMBC(0x164) CASEMBC(0x166)
        CASEMBC(0x1e6a) CASEMBC(0x1e6e)
                EMIT2('T'); EMITMBC(0x162) EMITMBC(0x164) EMITMBC(0x166)
                EMITMBC(0x1e6a) EMITMBC(0x1e6e)
                return OK;

        case 'U': case 0331: case 0332: case 0333: case 0334:
        CASEMBC(0x168) CASEMBC(0x16a) CASEMBC(0x16c) CASEMBC(0x16e)
        CASEMBC(0x170) CASEMBC(0x172) CASEMBC(0x1af) CASEMBC(0x1d3)
        CASEMBC(0x1ee6)
                EMIT2('U'); EMIT2(0331); EMIT2(0332); EMIT2(0333);
                EMIT2(0334); EMITMBC(0x168) EMITMBC(0x16a)
                EMITMBC(0x16c) EMITMBC(0x16e) EMITMBC(0x170)
                EMITMBC(0x172) EMITMBC(0x1af) EMITMBC(0x1d3)
                EMITMBC(0x1ee6)
                return OK;

        case 'V': CASEMBC(0x1e7c)
                EMIT2('V'); EMITMBC(0x1e7c)
                return OK;

        case 'W': CASEMBC(0x174) CASEMBC(0x1e80) CASEMBC(0x1e82)
        CASEMBC(0x1e84) CASEMBC(0x1e86)
                EMIT2('W'); EMITMBC(0x174) EMITMBC(0x1e80) EMITMBC(0x1e82)
                EMITMBC(0x1e84) EMITMBC(0x1e86)
                return OK;

        case 'X': CASEMBC(0x1e8a) CASEMBC(0x1e8c)
                EMIT2('X'); EMITMBC(0x1e8a) EMITMBC(0x1e8c)
                return OK;

        case 'Y': case 0335:
        CASEMBC(0x176) CASEMBC(0x178) CASEMBC(0x1e8e) CASEMBC(0x1ef2)
        CASEMBC(0x1ef6) CASEMBC(0x1ef8)
                EMIT2('Y'); EMIT2(0335); EMITMBC(0x176) EMITMBC(0x178)
                EMITMBC(0x1e8e) EMITMBC(0x1ef2) EMITMBC(0x1ef6)
                EMITMBC(0x1ef8)
                return OK;

        case 'Z': CASEMBC(0x179) CASEMBC(0x17b) CASEMBC(0x17d)
        CASEMBC(0x1b5) CASEMBC(0x1e90) CASEMBC(0x1e94)
                EMIT2('Z'); EMITMBC(0x179) EMITMBC(0x17b) EMITMBC(0x17d)
                EMITMBC(0x1b5) EMITMBC(0x1e90) EMITMBC(0x1e94)
                return OK;

        case 'a': case 0340: case 0341: case 0342:
        case 0343: case 0344: case 0345:
        CASEMBC(0x101) CASEMBC(0x103) CASEMBC(0x105) CASEMBC(0x1ce)
        CASEMBC(0x1df) CASEMBC(0x1e1) CASEMBC(0x1ea3)
                EMIT2('a'); EMIT2(0340); EMIT2(0341); EMIT2(0342);
                EMIT2(0343); EMIT2(0344); EMIT2(0345);
                EMITMBC(0x101) EMITMBC(0x103) EMITMBC(0x105)
                EMITMBC(0x1ce) EMITMBC(0x1df) EMITMBC(0x1e1)
                EMITMBC(0x1ea3)
                return OK;

        case 'b': CASEMBC(0x1e03) CASEMBC(0x1e07)
                EMIT2('b'); EMITMBC(0x1e03) EMITMBC(0x1e07)
                return OK;

        case 'c': case 0347:
        CASEMBC(0x107) CASEMBC(0x109) CASEMBC(0x10b) CASEMBC(0x10d)
                EMIT2('c'); EMIT2(0347); EMITMBC(0x107) EMITMBC(0x109)
                EMITMBC(0x10b) EMITMBC(0x10d)
                return OK;

        case 'd': CASEMBC(0x10f) CASEMBC(0x111) CASEMBC(0x1d0b)
        CASEMBC(0x1e11)
                EMIT2('d'); EMITMBC(0x10f) EMITMBC(0x111) EMITMBC(0x1e0b)
                EMITMBC(0x01e0f) EMITMBC(0x1e11)
                return OK;

        case 'e': case 0350: case 0351: case 0352: case 0353:
        CASEMBC(0x113) CASEMBC(0x115) CASEMBC(0x117) CASEMBC(0x119)
        CASEMBC(0x11b) CASEMBC(0x1ebb) CASEMBC(0x1ebd)
                EMIT2('e'); EMIT2(0350); EMIT2(0351); EMIT2(0352);
                EMIT2(0353); EMITMBC(0x113) EMITMBC(0x115)
                EMITMBC(0x117) EMITMBC(0x119) EMITMBC(0x11b)
                EMITMBC(0x1ebb) EMITMBC(0x1ebd)
                return OK;

        case 'f': CASEMBC(0x1e1f)
                EMIT2('f'); EMITMBC(0x1e1f)
                return OK;

        case 'g': CASEMBC(0x11d) CASEMBC(0x11f) CASEMBC(0x121)
        CASEMBC(0x123) CASEMBC(0x1e5) CASEMBC(0x1e7) CASEMBC(0x1f5)
        CASEMBC(0x1e21)
                EMIT2('g'); EMITMBC(0x11d) EMITMBC(0x11f) EMITMBC(0x121)
                EMITMBC(0x123) EMITMBC(0x1e5) EMITMBC(0x1e7)
                EMITMBC(0x1f5) EMITMBC(0x1e21)
                return OK;

        case 'h': CASEMBC(0x125) CASEMBC(0x127) CASEMBC(0x1e23)
        CASEMBC(0x1e27) CASEMBC(0x1e29) CASEMBC(0x1e96)
                EMIT2('h'); EMITMBC(0x125) EMITMBC(0x127) EMITMBC(0x1e23)
                EMITMBC(0x1e27) EMITMBC(0x1e29) EMITMBC(0x1e96)
                return OK;

        case 'i': case 0354: case 0355: case 0356: case 0357:
        CASEMBC(0x129) CASEMBC(0x12b) CASEMBC(0x12d) CASEMBC(0x12f)
        CASEMBC(0x1d0) CASEMBC(0x1ec9)
                EMIT2('i'); EMIT2(0354); EMIT2(0355); EMIT2(0356);
                EMIT2(0357); EMITMBC(0x129) EMITMBC(0x12b)
                EMITMBC(0x12d) EMITMBC(0x12f) EMITMBC(0x1d0)
                EMITMBC(0x1ec9)
                return OK;

        case 'j': CASEMBC(0x135) CASEMBC(0x1f0)
                EMIT2('j'); EMITMBC(0x135) EMITMBC(0x1f0)
                return OK;

        case 'k': CASEMBC(0x137) CASEMBC(0x1e9) CASEMBC(0x1e31)
        CASEMBC(0x1e35)
                EMIT2('k'); EMITMBC(0x137) EMITMBC(0x1e9) EMITMBC(0x1e31)
                EMITMBC(0x1e35)
                return OK;

        case 'l': CASEMBC(0x13a) CASEMBC(0x13c) CASEMBC(0x13e)
        CASEMBC(0x140) CASEMBC(0x142) CASEMBC(0x1e3b)
                EMIT2('l'); EMITMBC(0x13a) EMITMBC(0x13c) EMITMBC(0x13e)
                EMITMBC(0x140) EMITMBC(0x142) EMITMBC(0x1e3b)
                return OK;

        case 'm': CASEMBC(0x1e3f) CASEMBC(0x1e41)
                EMIT2('m'); EMITMBC(0x1e3f) EMITMBC(0x1e41)
                return OK;

        case 'n': case 0361:
        CASEMBC(0x144) CASEMBC(0x146) CASEMBC(0x148) CASEMBC(0x149)
        CASEMBC(0x1e45) CASEMBC(0x1e49)
                EMIT2('n'); EMIT2(0361); EMITMBC(0x144) EMITMBC(0x146)
                EMITMBC(0x148) EMITMBC(0x149) EMITMBC(0x1e45)
                EMITMBC(0x1e49)
                return OK;

        case 'o': case 0362: case 0363: case 0364: case 0365:
        case 0366: case 0370:
        CASEMBC(0x14d) CASEMBC(0x14f) CASEMBC(0x151) CASEMBC(0x1a1)
        CASEMBC(0x1d2) CASEMBC(0x1eb) CASEMBC(0x1ed) CASEMBC(0x1ecf)
                EMIT2('o'); EMIT2(0362); EMIT2(0363); EMIT2(0364);
                EMIT2(0365); EMIT2(0366); EMIT2(0370);
                EMITMBC(0x14d) EMITMBC(0x14f) EMITMBC(0x151)
                EMITMBC(0x1a1) EMITMBC(0x1d2) EMITMBC(0x1eb)
                EMITMBC(0x1ed) EMITMBC(0x1ecf)
                return OK;

        case 'p': CASEMBC(0x1e55) CASEMBC(0x1e57)
                EMIT2('p'); EMITMBC(0x1e55) EMITMBC(0x1e57)
                return OK;

        case 'r': CASEMBC(0x155) CASEMBC(0x157) CASEMBC(0x159)
        CASEMBC(0x1e59) CASEMBC(0x1e5f)
                EMIT2('r'); EMITMBC(0x155) EMITMBC(0x157) EMITMBC(0x159)
                EMITMBC(0x1e59) EMITMBC(0x1e5f)
                return OK;

        case 's': CASEMBC(0x15b) CASEMBC(0x15d) CASEMBC(0x15f)
        CASEMBC(0x161) CASEMBC(0x1e61)
                EMIT2('s'); EMITMBC(0x15b) EMITMBC(0x15d) EMITMBC(0x15f)
                EMITMBC(0x161) EMITMBC(0x1e61)
                return OK;

        case 't': CASEMBC(0x163) CASEMBC(0x165) CASEMBC(0x167)
        CASEMBC(0x1e6b) CASEMBC(0x1e6f) CASEMBC(0x1e97)
                EMIT2('t'); EMITMBC(0x163) EMITMBC(0x165) EMITMBC(0x167)
                EMITMBC(0x1e6b) EMITMBC(0x1e6f) EMITMBC(0x1e97)
                return OK;

        case 'u': case 0371: case 0372: case 0373: case 0374:
        CASEMBC(0x169) CASEMBC(0x16b) CASEMBC(0x16d) CASEMBC(0x16f)
        CASEMBC(0x171) CASEMBC(0x173) CASEMBC(0x1b0) CASEMBC(0x1d4)
        CASEMBC(0x1ee7)
                EMIT2('u'); EMIT2(0371); EMIT2(0372); EMIT2(0373);
                EMIT2(0374); EMITMBC(0x169) EMITMBC(0x16b)
                EMITMBC(0x16d) EMITMBC(0x16f) EMITMBC(0x171)
                EMITMBC(0x173) EMITMBC(0x1b0) EMITMBC(0x1d4)
                EMITMBC(0x1ee7)
                return OK;

        case 'v': CASEMBC(0x1e7d)
                EMIT2('v'); EMITMBC(0x1e7d)
                return OK;

        case 'w': CASEMBC(0x175) CASEMBC(0x1e81) CASEMBC(0x1e83)
        CASEMBC(0x1e85) CASEMBC(0x1e87) CASEMBC(0x1e98)
                EMIT2('w'); EMITMBC(0x175) EMITMBC(0x1e81) EMITMBC(0x1e83)
                EMITMBC(0x1e85) EMITMBC(0x1e87) EMITMBC(0x1e98)
                return OK;

        case 'x': CASEMBC(0x1e8b) CASEMBC(0x1e8d)
                EMIT2('x'); EMITMBC(0x1e8b) EMITMBC(0x1e8d)
                return OK;

        case 'y': case 0375: case 0377:
        CASEMBC(0x177) CASEMBC(0x1e8f) CASEMBC(0x1e99)
        CASEMBC(0x1ef3) CASEMBC(0x1ef7) CASEMBC(0x1ef9)
                EMIT2('y'); EMIT2(0375); EMIT2(0377); EMITMBC(0x177)
                EMITMBC(0x1e8f) EMITMBC(0x1e99) EMITMBC(0x1ef3)
                EMITMBC(0x1ef7) EMITMBC(0x1ef9)
                return OK;

        case 'z': CASEMBC(0x17a) CASEMBC(0x17c) CASEMBC(0x17e)
        CASEMBC(0x1b6) CASEMBC(0x1e91) CASEMBC(0x1e95)
                EMIT2('z'); EMITMBC(0x17a) EMITMBC(0x17c) EMITMBC(0x17e)
                EMITMBC(0x1b6) EMITMBC(0x1e91) EMITMBC(0x1e95)
                return OK;

        /* default: character itself */
    }

    EMIT2(c);
    return OK;
#undef EMIT2
#undef EMITMBC
}

/*
 * Code to parse regular expression.
 *
 * We try to reuse parsing functions in regexp.c to
 * minimize surprise and keep the syntax consistent.
 */

/*
 * Parse the lowest level.
 *
 * An atom can be one of a long list of items.  Many atoms match one character
 * in the text.  It is often an ordinary character or a character class.
 * Braces can be used to make a pattern into an atom.  The "\z(\)" construct
 * is only for syntax highlighting.
 *
 * atom    ::=     ordinary-atom
 *     or  \( pattern \)
 *     or  \%( pattern \)
 *     or  \z( pattern \)
 */
    static int
nfa_regatom()
{
    int         c;
    int         charclass;
    int         equiclass;
    int         collclass;
    int         got_coll_char;
    char_u      *p;
    char_u      *endp;
    char_u      *old_regparse = regparse;
    int         extra = 0;
    int         emit_range;
    int         negated;
    int         result;
    int         startc = -1;
    int         endc = -1;
    int         oldstartc = -1;

    c = getchr();
    switch (c)
    {
        case NUL:
            EMSG_RET_FAIL((char *)e_nul_found);

        case Magic('^'):
            EMIT(NFA_BOL);
            break;

        case Magic('$'):
            EMIT(NFA_EOL);
            had_eol = TRUE;
            break;

        case Magic('<'):
            EMIT(NFA_BOW);
            break;

        case Magic('>'):
            EMIT(NFA_EOW);
            break;

        case Magic('_'):
            c = no_Magic(getchr());
            if (c == NUL)
                EMSG_RET_FAIL((char *)e_nul_found);

            if (c == '^')       /* "\_^" is start-of-line */
            {
                EMIT(NFA_BOL);
                break;
            }
            if (c == '$')       /* "\_$" is end-of-line */
            {
                EMIT(NFA_EOL);
                had_eol = TRUE;
                break;
            }

            extra = NFA_ADD_NL;

            /* "\_[" is collection plus newline */
            if (c == '[')
                goto collection;

        /* "\_x" is character class plus newline */
        /*FALLTHROUGH*/

        /*
         * Character classes.
         */
        case Magic('.'):
        case Magic('i'):
        case Magic('I'):
        case Magic('k'):
        case Magic('K'):
        case Magic('f'):
        case Magic('F'):
        case Magic('p'):
        case Magic('P'):
        case Magic('s'):
        case Magic('S'):
        case Magic('d'):
        case Magic('D'):
        case Magic('x'):
        case Magic('X'):
        case Magic('o'):
        case Magic('O'):
        case Magic('w'):
        case Magic('W'):
        case Magic('h'):
        case Magic('H'):
        case Magic('a'):
        case Magic('A'):
        case Magic('l'):
        case Magic('L'):
        case Magic('u'):
        case Magic('U'):
            p = vim_strchr(classchars, no_Magic(c));
            if (p == NULL)
            {
                if (extra == NFA_ADD_NL)
                {
                    EMSGN((char *)e_ill_char_class, c);
                    rc_did_emsg = TRUE;
                    return FAIL;
                }
                EMSGN("INTERNAL: Unknown character class char: %ld", c);
                return FAIL;
            }
            /* When '.' is followed by a composing char ignore the dot, so that
             * the composing char is matched here. */
            if (c == Magic('.') && utf_iscomposing(peekchr()))
            {
                old_regparse = regparse;
                c = getchr();
                goto nfa_do_multibyte;
            }
            EMIT(nfa_classcodes[p - classchars]);
            if (extra == NFA_ADD_NL)
            {
                EMIT(NFA_NEWL);
                EMIT(NFA_OR);
                regflags |= RF_HASNL;
            }
            break;

        case Magic('n'):
            if (reg_string)
                /* In a string "\n" matches a newline character. */
                EMIT(NL);
            else
            {
                /* In buffer text "\n" matches the end of a line. */
                EMIT(NFA_NEWL);
                regflags |= RF_HASNL;
            }
            break;

        case Magic('('):
            if (nfa_reg(REG_PAREN) == FAIL)
                return FAIL;        /* cascaded error */
            break;

        case Magic('|'):
        case Magic('&'):
        case Magic(')'):
            EMSGN((char *)e_misplaced, no_Magic(c));
            return FAIL;

        case Magic('='):
        case Magic('?'):
        case Magic('+'):
        case Magic('@'):
        case Magic('*'):
        case Magic('{'):
            /* these should follow an atom, not form an atom */
            EMSGN((char *)e_misplaced, no_Magic(c));
            return FAIL;

        case Magic('~'):
            {
                char_u      *lp;

                /* Previous substitute pattern.
                 * Generated as "\%(pattern\)". */
                if (reg_prev_sub == NULL)
                {
                    EMSG((char *)e_nopresub);
                    return FAIL;
                }
                for (lp = reg_prev_sub; *lp != NUL; lp += utf_ptr2len(lp))
                {
                    EMIT(utf_ptr2char(lp));
                    if (lp != reg_prev_sub)
                        EMIT(NFA_CONCAT);
                }
                EMIT(NFA_NOPEN);
                break;
            }

        case Magic('1'):
        case Magic('2'):
        case Magic('3'):
        case Magic('4'):
        case Magic('5'):
        case Magic('6'):
        case Magic('7'):
        case Magic('8'):
        case Magic('9'):
            EMIT(NFA_BACKREF1 + (no_Magic(c) - '1'));
            nfa_has_backref = TRUE;
            break;

        case Magic('z'):
            c = no_Magic(getchr());
            switch (c)
            {
                case 's':
                    EMIT(NFA_ZSTART);
                    if (re_mult_next("\\zs") == FAIL)
                        return FAIL;
                    break;
                case 'e':
                    EMIT(NFA_ZEND);
                    nfa_has_zend = TRUE;
                    if (re_mult_next("\\ze") == FAIL)
                        return FAIL;
                    break;
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    /* \z1...\z9 */
                    if (reg_do_extmatch != REX_USE)
                        EMSG_RET_FAIL((char *)e_z1_not_allowed);
                    EMIT(NFA_ZREF1 + (no_Magic(c) - '1'));
                    /* No need to set nfa_has_backref, the sub-matches don't
                     * change when \z1 .. \z9 matches or not. */
                    re_has_z = REX_USE;
                    break;
                case '(':
                    /* \z( */
                    if (reg_do_extmatch != REX_SET)
                        EMSG_RET_FAIL((char *)e_z_not_allowed);
                    if (nfa_reg(REG_ZPAREN) == FAIL)
                        return FAIL;        /* cascaded error */
                    re_has_z = REX_SET;
                    break;
                default:
                    EMSGN("E867: (NFA) Unknown operator '\\z%c'", no_Magic(c));
                    return FAIL;
            }
            break;

        case Magic('%'):
            c = no_Magic(getchr());
            switch (c)
            {
                /* () without a back reference */
                case '(':
                    if (nfa_reg(REG_NPAREN) == FAIL)
                        return FAIL;
                    EMIT(NFA_NOPEN);
                    break;

                case 'd':   /* %d123 decimal */
                case 'o':   /* %o123 octal */
                case 'x':   /* %xab hex 2 */
                case 'u':   /* %uabcd hex 4 */
                case 'U':   /* %U1234abcd hex 8 */
                    {
                        int nr;

                        switch (c)
                        {
                            case 'd': nr = getdecchrs(); break;
                            case 'o': nr = getoctchrs(); break;
                            case 'x': nr = gethexchrs(2); break;
                            case 'u': nr = gethexchrs(4); break;
                            case 'U': nr = gethexchrs(8); break;
                            default:  nr = -1; break;
                        }

                        if (nr < 0)
                            EMSG2_RET_FAIL("E678: Invalid character after %s%%[dxouU]",
                                    reg_magic == MAGIC_ALL);
                        /* A NUL is stored in the text as NL */
                        /* TODO: what if a composing character follows? */
                        EMIT(nr == 0 ? 0x0a : nr);
                    }
                    break;

                /* Catch \%^ and \%$ regardless of where they appear in the
                 * pattern -- regardless of whether or not it makes sense. */
                case '^':
                    EMIT(NFA_BOF);
                    break;

                case '$':
                    EMIT(NFA_EOF);
                    break;

                case '#':
                    EMIT(NFA_CURSOR);
                    break;

                case 'V':
                    EMIT(NFA_VISUAL);
                    break;

                case 'C':
                    EMIT(NFA_ANY_COMPOSING);
                    break;

                case '[':
                    {
                        int         n;

                        /* \%[abc] */
                        for (n = 0; (c = peekchr()) != ']'; ++n)
                        {
                            if (c == NUL)
                                EMSG2_RET_FAIL((char *)e_missing_sb, reg_magic == MAGIC_ALL);
                            /* recursive call! */
                            if (nfa_regatom() == FAIL)
                                return FAIL;
                        }
                        getchr();  /* get the ] */
                        if (n == 0)
                            EMSG2_RET_FAIL((char *)e_empty_sb, reg_magic == MAGIC_ALL);
                        EMIT(NFA_OPT_CHARS);
                        EMIT(n);

                        /* Emit as "\%(\%[abc]\)" to be able to handle
                         * "\%[abc]*" which would cause the empty string to be
                         * matched an unlimited number of times. NFA_NOPEN is
                         * added only once at a position, while NFA_SPLIT is
                         * added multiple times.  This is more efficient than
                         * not allowing NFA_SPLIT multiple times, it is used a lot. */
                        EMIT(NFA_NOPEN);
                        break;
                    }

                default:
                    {
                        int     n = 0;
                        int     cmp = c;

                        if (c == '<' || c == '>')
                            c = getchr();
                        while (VIM_ISDIGIT(c))
                        {
                            n = n * 10 + (c - '0');
                            c = getchr();
                        }
                        if (c == 'l' || c == 'c' || c == 'v')
                        {
                            if (c == 'l')
                                /* \%{n}l  \%{n}<l  \%{n}>l */
                                EMIT(cmp == '<' ? NFA_LNUM_LT :
                                     cmp == '>' ? NFA_LNUM_GT : NFA_LNUM);
                            else if (c == 'c')
                                /* \%{n}c  \%{n}<c  \%{n}>c */
                                EMIT(cmp == '<' ? NFA_COL_LT :
                                     cmp == '>' ? NFA_COL_GT : NFA_COL);
                            else
                                /* \%{n}v  \%{n}<v  \%{n}>v */
                                EMIT(cmp == '<' ? NFA_VCOL_LT :
                                     cmp == '>' ? NFA_VCOL_GT : NFA_VCOL);
                            EMIT(n);
                            break;
                        }
                        else if (c == '\'' && n == 0)
                        {
                            /* \%'m  \%<'m  \%>'m */
                            EMIT(cmp == '<' ? NFA_MARK_LT :
                                 cmp == '>' ? NFA_MARK_GT : NFA_MARK);
                            EMIT(getchr());
                            break;
                        }
                    }
                    EMSGN("E867: (NFA) Unknown operator '\\%%%c'", no_Magic(c));
                    return FAIL;
            }
            break;

        case Magic('['):
collection:
            /*
             * [abc]  uses NFA_START_COLL - NFA_END_COLL
             * [^abc] uses NFA_START_NEG_COLL - NFA_END_NEG_COLL
             * Each character is produced as a regular state, using
             * NFA_CONCAT to bind them together.
             * Besides normal characters there can be:
             * - character classes  NFA_CLASS_*
             * - ranges, two characters followed by NFA_RANGE.
             */

            p = regparse;
            endp = skip_anyof(p);
            if (*endp == ']')
            {
                /*
                 * Try to reverse engineer character classes. For example,
                 * recognize that [0-9] stands for \d and [A-Za-z_] for \h,
                 * and perform the necessary substitutions in the NFA.
                 */
                result = nfa_recognize_char_class(regparse, endp, extra == NFA_ADD_NL);
                if (result != FAIL)
                {
                    if (result >= NFA_FIRST_NL && result <= NFA_LAST_NL)
                    {
                        EMIT(result - NFA_ADD_NL);
                        EMIT(NFA_NEWL);
                        EMIT(NFA_OR);
                    }
                    else
                        EMIT(result);
                    regparse = endp;
                    regparse += utfc_ptr2len(regparse);
                    return OK;
                }
                /*
                 * Failed to recognize a character class. Use the simple
                 * version that turns [abc] into 'a' OR 'b' OR 'c'
                 */
                startc = endc = oldstartc = -1;
                negated = FALSE;
                if (*regparse == '^')                   /* negated range */
                {
                    negated = TRUE;
                    regparse += utfc_ptr2len(regparse);
                    EMIT(NFA_START_NEG_COLL);
                }
                else
                    EMIT(NFA_START_COLL);
                if (*regparse == '-')
                {
                    startc = '-';
                    EMIT(startc);
                    EMIT(NFA_CONCAT);
                    regparse += utfc_ptr2len(regparse);
                }
                /* Emit the OR branches for each character in the [] */
                emit_range = FALSE;
                while (regparse < endp)
                {
                    oldstartc = startc;
                    startc = -1;
                    got_coll_char = FALSE;
                    if (*regparse == '[')
                    {
                        /* Check for [: :], [= =], [. .] */
                        equiclass = collclass = 0;
                        charclass = get_char_class(&regparse);
                        if (charclass == CLASS_NONE)
                        {
                            equiclass = get_equi_class(&regparse);
                            if (equiclass == 0)
                                collclass = get_coll_element(&regparse);
                        }

                        /* Character class like [:alpha:] */
                        if (charclass != CLASS_NONE)
                        {
                            switch (charclass)
                            {
                                case CLASS_ALNUM:
                                    EMIT(NFA_CLASS_ALNUM);
                                    break;
                                case CLASS_ALPHA:
                                    EMIT(NFA_CLASS_ALPHA);
                                    break;
                                case CLASS_BLANK:
                                    EMIT(NFA_CLASS_BLANK);
                                    break;
                                case CLASS_CNTRL:
                                    EMIT(NFA_CLASS_CNTRL);
                                    break;
                                case CLASS_DIGIT:
                                    EMIT(NFA_CLASS_DIGIT);
                                    break;
                                case CLASS_GRAPH:
                                    EMIT(NFA_CLASS_GRAPH);
                                    break;
                                case CLASS_LOWER:
                                    EMIT(NFA_CLASS_LOWER);
                                    break;
                                case CLASS_PRINT:
                                    EMIT(NFA_CLASS_PRINT);
                                    break;
                                case CLASS_PUNCT:
                                    EMIT(NFA_CLASS_PUNCT);
                                    break;
                                case CLASS_SPACE:
                                    EMIT(NFA_CLASS_SPACE);
                                    break;
                                case CLASS_UPPER:
                                    EMIT(NFA_CLASS_UPPER);
                                    break;
                                case CLASS_XDIGIT:
                                    EMIT(NFA_CLASS_XDIGIT);
                                    break;
                                case CLASS_TAB:
                                    EMIT(NFA_CLASS_TAB);
                                    break;
                                case CLASS_RETURN:
                                    EMIT(NFA_CLASS_RETURN);
                                    break;
                                case CLASS_BACKSPACE:
                                    EMIT(NFA_CLASS_BACKSPACE);
                                    break;
                                case CLASS_ESCAPE:
                                    EMIT(NFA_CLASS_ESCAPE);
                                    break;
                            }
                            EMIT(NFA_CONCAT);
                            continue;
                        }
                        /* Try equivalence class [=a=] and the like */
                        if (equiclass != 0)
                        {
                            result = nfa_emit_equi_class(equiclass);
                            if (result == FAIL)
                            {
                                /* should never happen */
                                EMSG_RET_FAIL("E868: Error building NFA with equivalence class!");
                            }
                            continue;
                        }
                        /* Try collating class like [. .] */
                        if (collclass != 0)
                        {
                            startc = collclass;  /* allow [.a.]-x as a range */
                            /* Will emit the proper atom at the end of the while loop. */
                        }
                    }
                    /* Try a range like 'a-x' or '\t-z'. Also allows '-' as a
                     * start character. */
                    if (*regparse == '-' && oldstartc != -1)
                    {
                        emit_range = TRUE;
                        startc = oldstartc;
                        regparse += utfc_ptr2len(regparse);
                        continue;           /* reading the end of the range */
                    }

                    /* Now handle simple and escaped characters.
                     * Only "\]", "\^", "\]" and "\\" are special in Vi.  Vim
                     * accepts "\t", "\e", etc., but only when the 'l' flag in
                     * 'cpoptions' is not included.
                     * Posix doesn't recognize backslash at all.
                     */
                    if (*regparse == '\\'
                            && !reg_cpo_bsl
                            && regparse + 1 <= endp
                            && (vim_strchr(REGEXP_INRANGE, regparse[1]) != NULL
                                || (!reg_cpo_lit && vim_strchr(REGEXP_ABBR, regparse[1]) != NULL)
                            )
                        )
                    {
                        regparse += utfc_ptr2len(regparse);

                        if (*regparse == 'n')
                            startc = reg_string ? NL : NFA_NEWL;
                        else if  (*regparse == 'd'
                                    || *regparse == 'o'
                                    || *regparse == 'x'
                                    || *regparse == 'u'
                                    || *regparse == 'U'
                                )
                        {
                            /* TODO(RE) This needs more testing */
                            startc = coll_get_char();
                            got_coll_char = TRUE;
                            mb_ptr_back(old_regparse, regparse);
                        }
                        else
                        {
                            /* \r,\t,\e,\b */
                            startc = backslash_trans(*regparse);
                        }
                    }

                    /* Normal printable char */
                    if (startc == -1)
                        startc = utf_ptr2char(regparse);

                    /* Previous char was '-', so this char is end of range. */
                    if (emit_range)
                    {
                        endc = startc;
                        startc = oldstartc;
                        if (startc > endc)
                            EMSG_RET_FAIL((char *)e_invrange);

                        if (endc > startc + 2)
                        {
                            /* Emit a range instead of the sequence of
                             * individual characters. */
                            if (startc == 0)
                                /* \x00 is translated to \x0a, start at \x01. */
                                EMIT(1);
                            else
                                --post_ptr; /* remove NFA_CONCAT */
                            EMIT(endc);
                            EMIT(NFA_RANGE);
                            EMIT(NFA_CONCAT);
                        }
                        else if (utf_char2len(startc) > 1 || utf_char2len(endc) > 1)
                        {
                            /* Emit the characters in the range.
                             * "startc" was already emitted, so skip it.
                             */
                            for (c = startc + 1; c <= endc; c++)
                            {
                                EMIT(c);
                                EMIT(NFA_CONCAT);
                            }
                        }
                        else
                        {
                            /* Emit the range. "startc" was already emitted, so skip it. */
                            for (c = startc + 1; c <= endc; c++)
                            {
                                EMIT(c);
                                EMIT(NFA_CONCAT);
                            }
                        }
                        emit_range = FALSE;
                        startc = -1;
                    }
                    else
                    {
                        /* This char (startc) is not part of a range. Just emit it.
                         * Normally, simply emit startc. But if we get char
                         * code=0 from a collating char, then replace it with 0x0a.
                         * This is needed to completely mimic the behaviour of
                         * the backtracking engine. */
                        if (startc == NFA_NEWL)
                        {
                            /* Line break can't be matched as part of the
                             * collection, add an OR below. But not for negated range. */
                            if (!negated)
                                extra = NFA_ADD_NL;
                        }
                        else
                        {
                            if (got_coll_char == TRUE && startc == 0)
                                EMIT(0x0a);
                            else
                                EMIT(startc);
                            EMIT(NFA_CONCAT);
                        }
                    }

                    regparse += utfc_ptr2len(regparse);
                }

                mb_ptr_back(old_regparse, regparse);
                if (*regparse == '-')       /* if last, '-' is just a char */
                {
                    EMIT('-');
                    EMIT(NFA_CONCAT);
                }

                /* skip the trailing ] */
                regparse = endp;
                regparse += utfc_ptr2len(regparse);

                /* Mark end of the collection. */
                if (negated == TRUE)
                    EMIT(NFA_END_NEG_COLL);
                else
                    EMIT(NFA_END_COLL);

                /* \_[] also matches \n but it's not negated */
                if (extra == NFA_ADD_NL)
                {
                    EMIT(reg_string ? NL : NFA_NEWL);
                    EMIT(NFA_OR);
                }

                return OK;
            }

            if (reg_strict)
                EMSG_RET_FAIL((char *)e_missingbracket);
            /* FALLTHROUGH */

        default:
            {
                int     plen;

nfa_do_multibyte:
                /* plen is length of current char with composing chars */
                if (utf_char2len(c) != (plen = utfc_ptr2len(old_regparse)) || utf_iscomposing(c))
                {
                    int i = 0;

                    /* A base character plus composing characters, or just one
                     * or more composing characters.
                     * This requires creating a separate atom as if enclosing
                     * the characters in (), where NFA_COMPOSING is the ( and
                     * NFA_END_COMPOSING is the ). Note that right now we are
                     * building the postfix form, not the NFA itself;
                     * a composing char could be: a, b, c, NFA_COMPOSING
                     * where 'b' and 'c' are chars with codes > 256. */
                    for (;;)
                    {
                        EMIT(c);
                        if (i > 0)
                            EMIT(NFA_CONCAT);
                        if ((i += utf_char2len(c)) >= plen)
                            break;
                        c = utf_ptr2char(old_regparse + i);
                    }
                    EMIT(NFA_COMPOSING);
                    regparse = old_regparse + plen;
                }
                else
                {
                    c = no_Magic(c);
                    EMIT(c);
                }
                return OK;
            }
    }

    return OK;
}

/*
 * Parse something followed by possible [*+=].
 *
 * A piece is an atom, possibly followed by a multi, an indication of how many
 * times the atom can be matched.  Example: "a*" matches any sequence of "a"
 * characters: "", "a", "aa", etc.
 *
 * piece   ::=      atom
 *      or  atom  multi
 */
    static int
nfa_regpiece()
{
    int         i;
    int         op;
    int         ret;
    long        minval, maxval;
    int         greedy = TRUE;      /* Braces are prefixed with '-' ? */
    parse_state_T old_state;
    parse_state_T new_state;
    int         c2;
    int         old_post_pos;
    int         my_post_start;
    int         quest;

    /* Save the current parse state, so that we can use it if <atom>{m,n} is next. */
    save_parse_state(&old_state);

    /* store current pos in the postfix form, for \{m,n} involving 0s */
    my_post_start = (int)(post_ptr - post_start);

    ret = nfa_regatom();
    if (ret == FAIL)
        return FAIL;        /* cascaded error */

    op = peekchr();
    if (re_multi_type(op) == NOT_MULTI)
        return OK;

    skipchr();
    switch (op)
    {
        case Magic('*'):
            EMIT(NFA_STAR);
            break;

        case Magic('+'):
            /*
             * Trick: Normally, (a*)\+ would match the whole input "aaa".  The
             * first and only submatch would be "aaa". But the backtracking
             * engine interprets the plus as "try matching one more time", and
             * a* matches a second time at the end of the input, the empty string.
             * The submatch will be the empty string.
             *
             * In order to be consistent with the old engine, we replace
             * <atom>+ with <atom><atom>*
             */
            restore_parse_state(&old_state);
            curchr = -1;
            if (nfa_regatom() == FAIL)
                return FAIL;
            EMIT(NFA_STAR);
            EMIT(NFA_CONCAT);
            skipchr();          /* skip the \+ */
            break;

        case Magic('@'):
            c2 = getdecchrs();
            op = no_Magic(getchr());
            i = 0;
            switch(op)
            {
                case '=':
                    /* \@= */
                    i = NFA_PREV_ATOM_NO_WIDTH;
                    break;
                case '!':
                    /* \@! */
                    i = NFA_PREV_ATOM_NO_WIDTH_NEG;
                    break;
                case '<':
                    op = no_Magic(getchr());
                    if (op == '=')
                        /* \@<= */
                        i = NFA_PREV_ATOM_JUST_BEFORE;
                    else if (op == '!')
                        /* \@<! */
                        i = NFA_PREV_ATOM_JUST_BEFORE_NEG;
                    break;
                case '>':
                    /* \@> */
                    i = NFA_PREV_ATOM_LIKE_PATTERN;
                    break;
            }
            if (i == 0)
            {
                EMSGN("E869: (NFA) Unknown operator '\\@%c'", op);
                return FAIL;
            }
            EMIT(i);
            if (i == NFA_PREV_ATOM_JUST_BEFORE || i == NFA_PREV_ATOM_JUST_BEFORE_NEG)
                EMIT(c2);
            break;

        case Magic('?'):
        case Magic('='):
            EMIT(NFA_QUEST);
            break;

        case Magic('{'):
            /* a{2,5} will expand to 'aaa?a?a?'
             * a{-1,3} will expand to 'aa??a??', where ?? is the nongreedy
             * version of '?'
             * \v(ab){2,3} will expand to '(ab)(ab)(ab)?', where all the
             * parenthesis have the same id
             */

            greedy = TRUE;
            c2 = peekchr();
            if (c2 == '-' || c2 == Magic('-'))
            {
                skipchr();
                greedy = FALSE;
            }
            if (!read_limits(&minval, &maxval))
                EMSG_RET_FAIL("E870: (NFA regexp) Error reading repetition limits");

            /*  <atom>{0,inf}, <atom>{0,} and <atom>{}  are equivalent to
             *  <atom>*  */
            if (minval == 0 && maxval == MAX_LIMIT)
            {
                if (greedy)             /* { { (match the braces) */
                    /* \{}, \{0,} */
                    EMIT(NFA_STAR);
                else                    /* { { (match the braces) */
                    /* \{-}, \{-0,} */
                    EMIT(NFA_STAR_NONGREEDY);
                break;
            }

            /* Special case: x{0} or x{-0} */
            if (maxval == 0)
            {
                /* Ignore result of previous call to nfa_regatom() */
                post_ptr = post_start + my_post_start;
                /* NFA_EMPTY is 0-length and works everywhere */
                EMIT(NFA_EMPTY);
                return OK;
            }

            /* The engine is very inefficient (uses too many states) when the
             * maximum is much larger than the minimum and when the maximum is
             * large.  Bail out if we can use the other engine. */
            if ((nfa_re_flags & RE_AUTO) && (maxval > minval + 200 || maxval > 500))
                return FAIL;

            /* Ignore previous call to nfa_regatom() */
            post_ptr = post_start + my_post_start;
            /* Save parse state after the repeated atom and the \{} */
            save_parse_state(&new_state);

            quest = (greedy == TRUE? NFA_QUEST : NFA_QUEST_NONGREEDY);
            for (i = 0; i < maxval; i++)
            {
                /* Goto beginning of the repeated atom */
                restore_parse_state(&old_state);
                old_post_pos = (int)(post_ptr - post_start);
                if (nfa_regatom() == FAIL)
                    return FAIL;
                /* after "minval" times, atoms are optional */
                if (i + 1 > minval)
                {
                    if (maxval == MAX_LIMIT)
                    {
                        if (greedy)
                            EMIT(NFA_STAR);
                        else
                            EMIT(NFA_STAR_NONGREEDY);
                    }
                    else
                        EMIT(quest);
                }
                if (old_post_pos != my_post_start)
                    EMIT(NFA_CONCAT);
                if (i + 1 > minval && maxval == MAX_LIMIT)
                    break;
            }

            /* Go to just after the repeated atom and the \{} */
            restore_parse_state(&new_state);
            curchr = -1;

            break;

        default:
            break;
    }

    if (re_multi_type(peekchr()) != NOT_MULTI)
        /* Can't have a multi follow a multi. */
        EMSG_RET_FAIL("E871: (NFA regexp) Can't have a multi follow a multi !");

    return OK;
}

/*
 * Parse one or more pieces, concatenated.  It matches a match for the
 * first piece, followed by a match for the second piece, etc.  Example:
 * "f[0-9]b", first matches "f", then a digit and then "b".
 *
 * concat  ::=      piece
 *      or  piece piece
 *      or  piece piece piece
 *      etc.
 */
    static int
nfa_regconcat()
{
    int         cont = TRUE;
    int         first = TRUE;

    while (cont)
    {
        switch (peekchr())
        {
            case NUL:
            case Magic('|'):
            case Magic('&'):
            case Magic(')'):
                cont = FALSE;
                break;

            case Magic('Z'):
                regflags |= RF_ICOMBINE;
                skipchr_keepstart();
                break;
            case Magic('c'):
                regflags |= RF_ICASE;
                skipchr_keepstart();
                break;
            case Magic('C'):
                regflags |= RF_NOICASE;
                skipchr_keepstart();
                break;
            case Magic('v'):
                reg_magic = MAGIC_ALL;
                skipchr_keepstart();
                curchr = -1;
                break;
            case Magic('m'):
                reg_magic = MAGIC_ON;
                skipchr_keepstart();
                curchr = -1;
                break;
            case Magic('M'):
                reg_magic = MAGIC_OFF;
                skipchr_keepstart();
                curchr = -1;
                break;
            case Magic('V'):
                reg_magic = MAGIC_NONE;
                skipchr_keepstart();
                curchr = -1;
                break;

            default:
                if (nfa_regpiece() == FAIL)
                    return FAIL;
                if (first == FALSE)
                    EMIT(NFA_CONCAT);
                else
                    first = FALSE;
                break;
        }
    }

    return OK;
}

/*
 * Parse a branch, one or more concats, separated by "\&".  It matches the
 * last concat, but only if all the preceding concats also match at the same
 * position.  Examples:
 *      "foobeep\&..." matches "foo" in "foobeep".
 *      ".*Peter\&.*Bob" matches in a line containing both "Peter" and "Bob"
 *
 * branch ::=       concat
 *              or  concat \& concat
 *              or  concat \& concat \& concat
 *              etc.
 */
    static int
nfa_regbranch()
{
    int         ch;
    int         old_post_pos;

    old_post_pos = (int)(post_ptr - post_start);

    /* First branch, possibly the only one */
    if (nfa_regconcat() == FAIL)
        return FAIL;

    ch = peekchr();
    /* Try next concats */
    while (ch == Magic('&'))
    {
        skipchr();
        EMIT(NFA_NOPEN);
        EMIT(NFA_PREV_ATOM_NO_WIDTH);
        old_post_pos = (int)(post_ptr - post_start);
        if (nfa_regconcat() == FAIL)
            return FAIL;
        /* if concat is empty do emit a node */
        if (old_post_pos == (int)(post_ptr - post_start))
            EMIT(NFA_EMPTY);
        EMIT(NFA_CONCAT);
        ch = peekchr();
    }

    /* if a branch is empty, emit one node for it */
    if (old_post_pos == (int)(post_ptr - post_start))
        EMIT(NFA_EMPTY);

    return OK;
}

/*
 *  Parse a pattern, one or more branches, separated by "\|".  It matches
 *  anything that matches one of the branches.  Example: "foo\|beep" matches
 *  "foo" and matches "beep".  If more than one branch matches, the first one is used.
 *
 *  pattern ::=     branch
 *      or  branch \| branch
 *      or  branch \| branch \| branch
 *      etc.
 */
    static int
nfa_reg(paren)
    int         paren;  /* REG_NOPAREN, REG_PAREN, REG_NPAREN or REG_ZPAREN */
{
    int         parno = 0;

    if (paren == REG_PAREN)
    {
        if (regnpar >= NSUBEXP) /* Too many `(' */
            EMSG_RET_FAIL("E872: (NFA regexp) Too many '('");
        parno = regnpar++;
    }
    else if (paren == REG_ZPAREN)
    {
        /* Make a ZOPEN node. */
        if (regnzpar >= NSUBEXP)
            EMSG_RET_FAIL("E879: (NFA regexp) Too many \\z(");
        parno = regnzpar++;
    }

    if (nfa_regbranch() == FAIL)
        return FAIL;        /* cascaded error */

    while (peekchr() == Magic('|'))
    {
        skipchr();
        if (nfa_regbranch() == FAIL)
            return FAIL;    /* cascaded error */
        EMIT(NFA_OR);
    }

    /* Check for proper termination. */
    if (paren != REG_NOPAREN && getchr() != Magic(')'))
    {
        if (paren == REG_NPAREN)
            EMSG2_RET_FAIL((char *)e_unmatchedpp, reg_magic == MAGIC_ALL);
        else
            EMSG2_RET_FAIL((char *)e_unmatchedp, reg_magic == MAGIC_ALL);
    }
    else if (paren == REG_NOPAREN && peekchr() != NUL)
    {
        if (peekchr() == Magic(')'))
            EMSG2_RET_FAIL((char *)e_unmatchedpar, reg_magic == MAGIC_ALL);
        else
            EMSG_RET_FAIL("E873: (NFA regexp) proper termination error");
    }
    /*
     * Here we set the flag allowing back references to this set of parentheses.
     */
    if (paren == REG_PAREN)
    {
        had_endbrace[parno] = TRUE;     /* have seen the close paren */
        EMIT(NFA_MOPEN + parno);
    }
    else if (paren == REG_ZPAREN)
        EMIT(NFA_ZOPEN + parno);

    return OK;
}

/*
 * Parse r.e. @expr and convert it into postfix form.
 * Return the postfix string on success, NULL otherwise.
 */
    static int *
re2post()
{
    if (nfa_reg(REG_NOPAREN) == FAIL)
        return NULL;
    EMIT(NFA_MOPEN);
    return post_start;
}

/* NB. Some of the code below is inspired by Russ's. */

/*
 * Represents an NFA state plus zero or one or two arrows exiting.
 * if c == MATCH, no arrows out; matching state.
 * If c == SPLIT, unlabeled arrows to out and out1 (if != NULL).
 * If c < 256, labeled arrow with character c to out.
 */

static nfa_state_T      *state_ptr; /* points to nfa_prog->state */

/*
 * Allocate and initialize nfa_state_T.
 */
    static nfa_state_T *
alloc_state(c, out, out1)
    int         c;
    nfa_state_T *out;
    nfa_state_T *out1;
{
    nfa_state_T *s;

    if (istate >= nstate)
        return NULL;

    s = &state_ptr[istate++];

    s->c    = c;
    s->out  = out;
    s->out1 = out1;
    s->val  = 0;

    s->id   = istate;
    s->lastlist[0] = 0;
    s->lastlist[1] = 0;

    return s;
}

/*
 * A partially built NFA without the matching state filled in.
 * Frag_T.start points at the start state.
 * Frag_T.out is a list of places that need to be set to the
 * next state for this fragment.
 */

/* Since the out pointers in the list are always uninitialized,
 * we use the pointers themselves as storage for the Ptrlists. */
typedef union Ptrlist Ptrlist;
union Ptrlist
{
    Ptrlist     *next;
    nfa_state_T *s;
};

typedef struct Frag
{
    nfa_state_T *start;
    Ptrlist     *out;
} Frag_T;

static Frag_T frag(nfa_state_T *start, Ptrlist *out);
static Ptrlist *list1(nfa_state_T **outp);
static void patch(Ptrlist *l, nfa_state_T *s);
static Ptrlist *append(Ptrlist *l1, Ptrlist *l2);
static void st_push(Frag_T s, Frag_T **p, Frag_T *stack_end);
static Frag_T st_pop(Frag_T **p, Frag_T *stack);

/*
 * Initialize a Frag_T struct and return it.
 */
    static Frag_T
frag(start, out)
    nfa_state_T *start;
    Ptrlist     *out;
{
    Frag_T n;

    n.start = start;
    n.out = out;
    return n;
}

/*
 * Create singleton list containing just outp.
 */
    static Ptrlist *
list1(outp)
    nfa_state_T **outp;
{
    Ptrlist *l;

    l = (Ptrlist *)outp;
    l->next = NULL;
    return l;
}

/*
 * Patch the list of states at out to point to start.
 */
    static void
patch(l, s)
    Ptrlist     *l;
    nfa_state_T *s;
{
    Ptrlist *next;

    for (; l; l = next)
    {
        next = l->next;
        l->s = s;
    }
}

/*
 * Join the two lists l1 and l2, returning the combination.
 */
    static Ptrlist *
append(l1, l2)
    Ptrlist *l1;
    Ptrlist *l2;
{
    Ptrlist *oldl1;

    oldl1 = l1;
    while (l1->next)
        l1 = l1->next;
    l1->next = l2;
    return oldl1;
}

/*
 * Stack used for transforming postfix form into NFA.
 */
static Frag_T empty;

    static void
st_error(postfix, end, p)
    int *postfix UNUSED;
    int *end UNUSED;
    int *p UNUSED;
{
    EMSG("E874: (NFA) Could not pop the stack !");
}

/*
 * Push an item onto the stack.
 */
    static void
st_push(s, p, stack_end)
    Frag_T s;
    Frag_T **p;
    Frag_T *stack_end;
{
    Frag_T *stackp = *p;

    if (stackp >= stack_end)
        return;
    *stackp = s;
    *p = *p + 1;
}

/*
 * Pop an item from the stack.
 */
    static Frag_T
st_pop(p, stack)
    Frag_T **p;
    Frag_T *stack;
{
    Frag_T *stackp;

    *p = *p - 1;
    stackp = *p;
    if (stackp < stack)
        return empty;

    return **p;
}

/*
 * Estimate the maximum byte length of anything matching "state".
 * When unknown or unlimited return -1.
 */
    static int
nfa_max_width(startstate, depth)
    nfa_state_T *startstate;
    int         depth;
{
    int             l, r;
    nfa_state_T     *state = startstate;
    int             len = 0;

    /* detect looping in a NFA_SPLIT */
    if (depth > 4)
        return -1;

    while (state != NULL)
    {
        switch (state->c)
        {
            case NFA_END_INVISIBLE:
            case NFA_END_INVISIBLE_NEG:
                /* the end, return what we have */
                return len;

            case NFA_SPLIT:
                /* two alternatives, use the maximum */
                l = nfa_max_width(state->out, depth + 1);
                r = nfa_max_width(state->out1, depth + 1);
                if (l < 0 || r < 0)
                    return -1;

                return len + (l > r ? l : r);

            case NFA_ANY:
            case NFA_START_COLL:
            case NFA_START_NEG_COLL:
                /* matches some character, including composing chars */
                len += MB_MAXBYTES;
                if (state->c != NFA_ANY)
                {
                    /* skip over the characters */
                    state = state->out1->out;
                    continue;
                }
                break;

            case NFA_DIGIT:
            case NFA_WHITE:
            case NFA_HEX:
            case NFA_OCTAL:
                /* ascii */
                ++len;
                break;

            case NFA_IDENT:
            case NFA_SIDENT:
            case NFA_KWORD:
            case NFA_SKWORD:
            case NFA_FNAME:
            case NFA_SFNAME:
            case NFA_PRINT:
            case NFA_SPRINT:
            case NFA_NWHITE:
            case NFA_NDIGIT:
            case NFA_NHEX:
            case NFA_NOCTAL:
            case NFA_WORD:
            case NFA_NWORD:
            case NFA_HEAD:
            case NFA_NHEAD:
            case NFA_ALPHA:
            case NFA_NALPHA:
            case NFA_LOWER:
            case NFA_NLOWER:
            case NFA_UPPER:
            case NFA_NUPPER:
            case NFA_LOWER_IC:
            case NFA_NLOWER_IC:
            case NFA_UPPER_IC:
            case NFA_NUPPER_IC:
            case NFA_ANY_COMPOSING:
                /* possibly non-ascii */
                len += 3;
                break;

            case NFA_START_INVISIBLE:
            case NFA_START_INVISIBLE_NEG:
            case NFA_START_INVISIBLE_BEFORE:
            case NFA_START_INVISIBLE_BEFORE_NEG:
                /* zero-width, out1 points to the END state */
                state = state->out1->out;
                continue;

            case NFA_BACKREF1:
            case NFA_BACKREF2:
            case NFA_BACKREF3:
            case NFA_BACKREF4:
            case NFA_BACKREF5:
            case NFA_BACKREF6:
            case NFA_BACKREF7:
            case NFA_BACKREF8:
            case NFA_BACKREF9:
            case NFA_ZREF1:
            case NFA_ZREF2:
            case NFA_ZREF3:
            case NFA_ZREF4:
            case NFA_ZREF5:
            case NFA_ZREF6:
            case NFA_ZREF7:
            case NFA_ZREF8:
            case NFA_ZREF9:
            case NFA_NEWL:
            case NFA_SKIP:
                /* unknown width */
                return -1;

            case NFA_BOL:
            case NFA_EOL:
            case NFA_BOF:
            case NFA_EOF:
            case NFA_BOW:
            case NFA_EOW:
            case NFA_MOPEN:
            case NFA_MOPEN1:
            case NFA_MOPEN2:
            case NFA_MOPEN3:
            case NFA_MOPEN4:
            case NFA_MOPEN5:
            case NFA_MOPEN6:
            case NFA_MOPEN7:
            case NFA_MOPEN8:
            case NFA_MOPEN9:
            case NFA_ZOPEN:
            case NFA_ZOPEN1:
            case NFA_ZOPEN2:
            case NFA_ZOPEN3:
            case NFA_ZOPEN4:
            case NFA_ZOPEN5:
            case NFA_ZOPEN6:
            case NFA_ZOPEN7:
            case NFA_ZOPEN8:
            case NFA_ZOPEN9:
            case NFA_ZCLOSE:
            case NFA_ZCLOSE1:
            case NFA_ZCLOSE2:
            case NFA_ZCLOSE3:
            case NFA_ZCLOSE4:
            case NFA_ZCLOSE5:
            case NFA_ZCLOSE6:
            case NFA_ZCLOSE7:
            case NFA_ZCLOSE8:
            case NFA_ZCLOSE9:
            case NFA_MCLOSE:
            case NFA_MCLOSE1:
            case NFA_MCLOSE2:
            case NFA_MCLOSE3:
            case NFA_MCLOSE4:
            case NFA_MCLOSE5:
            case NFA_MCLOSE6:
            case NFA_MCLOSE7:
            case NFA_MCLOSE8:
            case NFA_MCLOSE9:
            case NFA_NOPEN:
            case NFA_NCLOSE:

            case NFA_LNUM_GT:
            case NFA_LNUM_LT:
            case NFA_COL_GT:
            case NFA_COL_LT:
            case NFA_VCOL_GT:
            case NFA_VCOL_LT:
            case NFA_MARK_GT:
            case NFA_MARK_LT:
            case NFA_VISUAL:
            case NFA_LNUM:
            case NFA_CURSOR:
            case NFA_COL:
            case NFA_VCOL:
            case NFA_MARK:

            case NFA_ZSTART:
            case NFA_ZEND:
            case NFA_OPT_CHARS:
            case NFA_EMPTY:
            case NFA_START_PATTERN:
            case NFA_END_PATTERN:
            case NFA_COMPOSING:
            case NFA_END_COMPOSING:
                /* zero-width */
                break;

            default:
                if (state->c < 0)
                    /* don't know what this is */
                    return -1;
                /* normal character */
                len += utf_char2len(state->c);
                break;
        }

        /* normal way to continue */
        state = state->out;
    }

    /* unrecognized, "cannot happen" */
    return -1;
}

/*
 * Convert a postfix form into its equivalent NFA.
 * Return the NFA start state on success, NULL otherwise.
 */
    static nfa_state_T *
post2nfa(postfix, end, nfa_calc_size)
    int         *postfix;
    int         *end;
    int         nfa_calc_size;
{
    int         *p;
    int         mopen;
    int         mclose;
    Frag_T      *stack = NULL;
    Frag_T      *stackp = NULL;
    Frag_T      *stack_end = NULL;
    Frag_T      e1;
    Frag_T      e2;
    Frag_T      e;
    nfa_state_T *s;
    nfa_state_T *s1;
    nfa_state_T *matchstate;
    nfa_state_T *ret = NULL;

    if (postfix == NULL)
        return NULL;

#define PUSH(s)     st_push((s), &stackp, stack_end)
#define POP()       st_pop(&stackp, stack);             \
                    if (stackp < stack)                 \
                    {                                   \
                        st_error(postfix, end, p);      \
                        return NULL;                    \
                    }

    if (nfa_calc_size == FALSE)
    {
        /* Allocate space for the stack. Max states on the stack : nstate */
        stack = (Frag_T *)lalloc((nstate + 1) * sizeof(Frag_T), TRUE);
        stackp = stack;
        stack_end = stack + (nstate + 1);
    }

    for (p = postfix; p < end; ++p)
    {
        switch (*p)
        {
        case NFA_CONCAT:
            /* Concatenation.
             * Pay attention: this operator does not exist in the r.e. itself
             * (it is implicit, really).  It is added when r.e. is translated
             * to postfix form in re2post(). */
            if (nfa_calc_size == TRUE)
            {
                /* nstate += 0; */
                break;
            }
            e2 = POP();
            e1 = POP();
            patch(e1.out, e2.start);
            PUSH(frag(e1.start, e2.out));
            break;

        case NFA_OR:
            /* Alternation */
            if (nfa_calc_size == TRUE)
            {
                nstate++;
                break;
            }
            e2 = POP();
            e1 = POP();
            s = alloc_state(NFA_SPLIT, e1.start, e2.start);
            if (s == NULL)
                goto theend;
            PUSH(frag(s, append(e1.out, e2.out)));
            break;

        case NFA_STAR:
            /* Zero or more, prefer more */
            if (nfa_calc_size == TRUE)
            {
                nstate++;
                break;
            }
            e = POP();
            s = alloc_state(NFA_SPLIT, e.start, NULL);
            if (s == NULL)
                goto theend;
            patch(e.out, s);
            PUSH(frag(s, list1(&s->out1)));
            break;

        case NFA_STAR_NONGREEDY:
            /* Zero or more, prefer zero */
            if (nfa_calc_size == TRUE)
            {
                nstate++;
                break;
            }
            e = POP();
            s = alloc_state(NFA_SPLIT, NULL, e.start);
            if (s == NULL)
                goto theend;
            patch(e.out, s);
            PUSH(frag(s, list1(&s->out)));
            break;

        case NFA_QUEST:
            /* one or zero atoms=> greedy match */
            if (nfa_calc_size == TRUE)
            {
                nstate++;
                break;
            }
            e = POP();
            s = alloc_state(NFA_SPLIT, e.start, NULL);
            if (s == NULL)
                goto theend;
            PUSH(frag(s, append(e.out, list1(&s->out1))));
            break;

        case NFA_QUEST_NONGREEDY:
            /* zero or one atoms => non-greedy match */
            if (nfa_calc_size == TRUE)
            {
                nstate++;
                break;
            }
            e = POP();
            s = alloc_state(NFA_SPLIT, NULL, e.start);
            if (s == NULL)
                goto theend;
            PUSH(frag(s, append(e.out, list1(&s->out))));
            break;

        case NFA_END_COLL:
        case NFA_END_NEG_COLL:
            /* On the stack is the sequence starting with NFA_START_COLL or
             * NFA_START_NEG_COLL and all possible characters. Patch it to
             * add the output to the start. */
            if (nfa_calc_size == TRUE)
            {
                nstate++;
                break;
            }
            e = POP();
            s = alloc_state(NFA_END_COLL, NULL, NULL);
            if (s == NULL)
                goto theend;
            patch(e.out, s);
            e.start->out1 = s;
            PUSH(frag(e.start, list1(&s->out)));
            break;

        case NFA_RANGE:
            /* Before this are two characters, the low and high end of a
             * range.  Turn them into two states with MIN and MAX. */
            if (nfa_calc_size == TRUE)
            {
                /* nstate += 0; */
                break;
            }
            e2 = POP();
            e1 = POP();
            e2.start->val = e2.start->c;
            e2.start->c = NFA_RANGE_MAX;
            e1.start->val = e1.start->c;
            e1.start->c = NFA_RANGE_MIN;
            patch(e1.out, e2.start);
            PUSH(frag(e1.start, e2.out));
            break;

        case NFA_EMPTY:
            /* 0-length, used in a repetition with max/min count of 0 */
            if (nfa_calc_size == TRUE)
            {
                nstate++;
                break;
            }
            s = alloc_state(NFA_EMPTY, NULL, NULL);
            if (s == NULL)
                goto theend;
            PUSH(frag(s, list1(&s->out)));
            break;

        case NFA_OPT_CHARS:
          {
            int    n;

            /* \%[abc] implemented as:
             *    NFA_SPLIT
             *    +-CHAR(a)
             *    | +-NFA_SPLIT
             *    |   +-CHAR(b)
             *    |   | +-NFA_SPLIT
             *    |   |   +-CHAR(c)
             *    |   |   | +-next
             *    |   |   +- next
             *    |   +- next
             *    +- next
             */
            n = *++p; /* get number of characters */
            if (nfa_calc_size == TRUE)
            {
                nstate += n;
                break;
            }
            s = NULL; /* avoid compiler warning */
            e1.out = NULL; /* stores list with out1's */
            s1 = NULL; /* previous NFA_SPLIT to connect to */
            while (n-- > 0)
            {
                e = POP(); /* get character */
                s = alloc_state(NFA_SPLIT, e.start, NULL);
                if (s == NULL)
                    goto theend;
                if (e1.out == NULL)
                    e1 = e;
                patch(e.out, s1);
                append(e1.out, list1(&s->out1));
                s1 = s;
            }
            PUSH(frag(s, e1.out));
            break;
          }

        case NFA_PREV_ATOM_NO_WIDTH:
        case NFA_PREV_ATOM_NO_WIDTH_NEG:
        case NFA_PREV_ATOM_JUST_BEFORE:
        case NFA_PREV_ATOM_JUST_BEFORE_NEG:
        case NFA_PREV_ATOM_LIKE_PATTERN:
          {
            int before = (*p == NFA_PREV_ATOM_JUST_BEFORE || *p == NFA_PREV_ATOM_JUST_BEFORE_NEG);
            int pattern = (*p == NFA_PREV_ATOM_LIKE_PATTERN);
            int start_state;
            int end_state;
            int n = 0;
            nfa_state_T *zend;
            nfa_state_T *skip;

            switch (*p)
            {
                case NFA_PREV_ATOM_NO_WIDTH:
                    start_state = NFA_START_INVISIBLE;
                    end_state = NFA_END_INVISIBLE;
                    break;
                case NFA_PREV_ATOM_NO_WIDTH_NEG:
                    start_state = NFA_START_INVISIBLE_NEG;
                    end_state = NFA_END_INVISIBLE_NEG;
                    break;
                case NFA_PREV_ATOM_JUST_BEFORE:
                    start_state = NFA_START_INVISIBLE_BEFORE;
                    end_state = NFA_END_INVISIBLE;
                    break;
                case NFA_PREV_ATOM_JUST_BEFORE_NEG:
                    start_state = NFA_START_INVISIBLE_BEFORE_NEG;
                    end_state = NFA_END_INVISIBLE_NEG;
                    break;
                default: /* NFA_PREV_ATOM_LIKE_PATTERN: */
                    start_state = NFA_START_PATTERN;
                    end_state = NFA_END_PATTERN;
                    break;
            }

            if (before)
                n = *++p; /* get the count */

            /* The \@= operator: match the preceding atom with zero width.
             * The \@! operator: no match for the preceding atom.
             * The \@<= operator: match for the preceding atom.
             * The \@<! operator: no match for the preceding atom.
             * Surrounds the preceding atom with START_INVISIBLE and
             * END_INVISIBLE, similarly to MOPEN. */

            if (nfa_calc_size == TRUE)
            {
                nstate += pattern ? 4 : 2;
                break;
            }
            e = POP();
            s1 = alloc_state(end_state, NULL, NULL);
            if (s1 == NULL)
                goto theend;

            s = alloc_state(start_state, e.start, s1);
            if (s == NULL)
                goto theend;
            if (pattern)
            {
                /* NFA_ZEND -> NFA_END_PATTERN -> NFA_SKIP -> what follows. */
                skip = alloc_state(NFA_SKIP, NULL, NULL);
                zend = alloc_state(NFA_ZEND, s1, NULL);
                s1->out= skip;
                patch(e.out, zend);
                PUSH(frag(s, list1(&skip->out)));
            }
            else
            {
                patch(e.out, s1);
                PUSH(frag(s, list1(&s1->out)));
                if (before)
                {
                    if (n <= 0)
                        /* See if we can guess the maximum width, it avoids a
                         * lot of pointless tries. */
                        n = nfa_max_width(e.start, 0);
                    s->val = n; /* store the count */
                }
            }
            break;
          }

        case NFA_COMPOSING:     /* char with composing char */
            if (regflags & RF_ICOMBINE)
            {
                /* TODO: use the base character only */
            }
            /* FALLTHROUGH */

        case NFA_MOPEN: /* \( \) Submatch */
        case NFA_MOPEN1:
        case NFA_MOPEN2:
        case NFA_MOPEN3:
        case NFA_MOPEN4:
        case NFA_MOPEN5:
        case NFA_MOPEN6:
        case NFA_MOPEN7:
        case NFA_MOPEN8:
        case NFA_MOPEN9:
        case NFA_ZOPEN: /* \z( \) Submatch */
        case NFA_ZOPEN1:
        case NFA_ZOPEN2:
        case NFA_ZOPEN3:
        case NFA_ZOPEN4:
        case NFA_ZOPEN5:
        case NFA_ZOPEN6:
        case NFA_ZOPEN7:
        case NFA_ZOPEN8:
        case NFA_ZOPEN9:
        case NFA_NOPEN: /* \%( \) "Invisible Submatch" */
            if (nfa_calc_size == TRUE)
            {
                nstate += 2;
                break;
            }

            mopen = *p;
            switch (*p)
            {
                case NFA_NOPEN: mclose = NFA_NCLOSE; break;
                case NFA_ZOPEN: mclose = NFA_ZCLOSE; break;
                case NFA_ZOPEN1: mclose = NFA_ZCLOSE1; break;
                case NFA_ZOPEN2: mclose = NFA_ZCLOSE2; break;
                case NFA_ZOPEN3: mclose = NFA_ZCLOSE3; break;
                case NFA_ZOPEN4: mclose = NFA_ZCLOSE4; break;
                case NFA_ZOPEN5: mclose = NFA_ZCLOSE5; break;
                case NFA_ZOPEN6: mclose = NFA_ZCLOSE6; break;
                case NFA_ZOPEN7: mclose = NFA_ZCLOSE7; break;
                case NFA_ZOPEN8: mclose = NFA_ZCLOSE8; break;
                case NFA_ZOPEN9: mclose = NFA_ZCLOSE9; break;
                case NFA_COMPOSING: mclose = NFA_END_COMPOSING; break;
                default:
                    /* NFA_MOPEN, NFA_MOPEN1 .. NFA_MOPEN9 */
                    mclose = *p + NSUBEXP;
                    break;
            }

            /* Allow "NFA_MOPEN" as a valid postfix representation for
             * the empty regexp "". In this case, the NFA will be
             * NFA_MOPEN -> NFA_MCLOSE. Note that this also allows
             * empty groups of parenthesis, and empty mbyte chars */
            if (stackp == stack)
            {
                s = alloc_state(mopen, NULL, NULL);
                if (s == NULL)
                    goto theend;
                s1 = alloc_state(mclose, NULL, NULL);
                if (s1 == NULL)
                    goto theend;
                patch(list1(&s->out), s1);
                PUSH(frag(s, list1(&s1->out)));
                break;
            }

            /* At least one node was emitted before NFA_MOPEN, so
             * at least one node will be between NFA_MOPEN and NFA_MCLOSE */
            e = POP();
            s = alloc_state(mopen, e.start, NULL);   /* `(' */
            if (s == NULL)
                goto theend;

            s1 = alloc_state(mclose, NULL, NULL);   /* `)' */
            if (s1 == NULL)
                goto theend;
            patch(e.out, s1);

            if (mopen == NFA_COMPOSING)
                /* COMPOSING->out1 = END_COMPOSING */
                patch(list1(&s->out1), s1);

            PUSH(frag(s, list1(&s1->out)));
            break;

        case NFA_BACKREF1:
        case NFA_BACKREF2:
        case NFA_BACKREF3:
        case NFA_BACKREF4:
        case NFA_BACKREF5:
        case NFA_BACKREF6:
        case NFA_BACKREF7:
        case NFA_BACKREF8:
        case NFA_BACKREF9:
        case NFA_ZREF1:
        case NFA_ZREF2:
        case NFA_ZREF3:
        case NFA_ZREF4:
        case NFA_ZREF5:
        case NFA_ZREF6:
        case NFA_ZREF7:
        case NFA_ZREF8:
        case NFA_ZREF9:
            if (nfa_calc_size == TRUE)
            {
                nstate += 2;
                break;
            }
            s = alloc_state(*p, NULL, NULL);
            if (s == NULL)
                goto theend;
            s1 = alloc_state(NFA_SKIP, NULL, NULL);
            if (s1 == NULL)
                goto theend;
            patch(list1(&s->out), s1);
            PUSH(frag(s, list1(&s1->out)));
            break;

        case NFA_LNUM:
        case NFA_LNUM_GT:
        case NFA_LNUM_LT:
        case NFA_VCOL:
        case NFA_VCOL_GT:
        case NFA_VCOL_LT:
        case NFA_COL:
        case NFA_COL_GT:
        case NFA_COL_LT:
        case NFA_MARK:
        case NFA_MARK_GT:
        case NFA_MARK_LT:
          {
            int n = *++p; /* lnum, col or mark name */

            if (nfa_calc_size == TRUE)
            {
                nstate += 1;
                break;
            }
            s = alloc_state(p[-1], NULL, NULL);
            if (s == NULL)
                goto theend;
            s->val = n;
            PUSH(frag(s, list1(&s->out)));
            break;
          }

        case NFA_ZSTART:
        case NFA_ZEND:
        default:
            /* Operands */
            if (nfa_calc_size == TRUE)
            {
                nstate++;
                break;
            }
            s = alloc_state(*p, NULL, NULL);
            if (s == NULL)
                goto theend;
            PUSH(frag(s, list1(&s->out)));
            break;
        }
    }

    if (nfa_calc_size == TRUE)
    {
        nstate++;
        goto theend;    /* Return value when counting size is ignored anyway */
    }

    e = POP();
    if (stackp != stack)
        EMSG_RET_NULL("E875: (NFA regexp) (While converting from postfix to NFA), too many states left on stack");

    if (istate >= nstate)
        EMSG_RET_NULL("E876: (NFA regexp) Not enough space to store the whole NFA ");

    matchstate = &state_ptr[istate++]; /* the match state */
    matchstate->c = NFA_MATCH;
    matchstate->out = matchstate->out1 = NULL;
    matchstate->id = 0;

    patch(e.out, matchstate);
    ret = e.start;

theend:
    vim_free(stack);
    return ret;

#undef POP1
#undef PUSH1
#undef POP2
#undef PUSH2
#undef POP
#undef PUSH
}

/*
 * After building the NFA program, inspect it to add optimization hints.
 */
    static void
nfa_postprocess(prog)
    nfa_regprog_T   *prog;
{
    int i;
    int c;

    for (i = 0; i < prog->nstate; ++i)
    {
        c = prog->state[i].c;
        if (c == NFA_START_INVISIBLE
                || c == NFA_START_INVISIBLE_NEG
                || c == NFA_START_INVISIBLE_BEFORE
                || c == NFA_START_INVISIBLE_BEFORE_NEG)
        {
            int directly;

            /* Do it directly when what follows is possibly the end of the match. */
            if (match_follows(prog->state[i].out1->out, 0))
                directly = TRUE;
            else
            {
                int ch_invisible = failure_chance(prog->state[i].out, 0);
                int ch_follows = failure_chance(prog->state[i].out1->out, 0);

                /* Postpone when the invisible match is expensive or has a
                 * lower chance of failing. */
                if (c == NFA_START_INVISIBLE_BEFORE || c == NFA_START_INVISIBLE_BEFORE_NEG)
                {
                    /* "before" matches are very expensive when
                     * unbounded, always prefer what follows then,
                     * unless what follows will always match.
                     * Otherwise strongly prefer what follows. */
                    if (prog->state[i].val <= 0 && ch_follows > 0)
                        directly = FALSE;
                    else
                        directly = ch_follows * 10 < ch_invisible;
                }
                else
                {
                    /* normal invisible, first do the one with the
                     * highest failure chance */
                    directly = ch_follows < ch_invisible;
                }
            }
            if (directly)
                /* switch to the _FIRST state */
                ++prog->state[i].c;
        }
    }
}

/****************************************************************
 * NFA execution code.
 ****************************************************************/

typedef struct
{
    int     in_use; /* number of subexpr with useful info */

    /* When REG_MULTI is TRUE list.multi is used, otherwise list.line. */
    union
    {
        struct multipos
        {
            linenr_T    start_lnum;
            linenr_T    end_lnum;
            colnr_T     start_col;
            colnr_T     end_col;
        } multi[NSUBEXP];
        struct linepos
        {
            char_u      *start;
            char_u      *end;
        } line[NSUBEXP];
    } list;
} regsub_T;

typedef struct
{
    regsub_T    norm; /* \( .. \) matches */
    regsub_T    synt; /* \z( .. \) matches */
} regsubs_T;

/* nfa_pim_T stores a Postponed Invisible Match. */
typedef struct
{
    int         result;         /* NFA_PIM_*, see below */
    nfa_state_T *state;         /* the invisible match start state */
    regsubs_T   subs;           /* submatch info, only party used */
    union
    {
        lpos_T  pos;
        char_u  *ptr;
    } end;                      /* where the match must end */
} nfa_pim_T;

/* Values for done in nfa_pim_T. */
#define NFA_PIM_UNUSED   0      /* pim not used */
#define NFA_PIM_TODO     1      /* pim not done yet */
#define NFA_PIM_MATCH    2      /* pim executed, matches */
#define NFA_PIM_NOMATCH  3      /* pim executed, no match */

/* nfa_thread_T contains execution information of a NFA state */
typedef struct
{
    nfa_state_T *state;
    int         count;
    nfa_pim_T   pim;            /* if pim.result != NFA_PIM_UNUSED: postponed invisible match */
    regsubs_T   subs;           /* submatch info, only party used */
} nfa_thread_T;

/* nfa_list_T contains the alternative NFA execution states. */
typedef struct
{
    nfa_thread_T    *t;         /* allocated array of states */
    int             n;          /* nr of states currently in "t" */
    int             len;        /* max nr of states in "t" */
    int             id;         /* ID of the list */
    int             has_pim;    /* TRUE when any state has a PIM */
} nfa_list_T;

/* Used during execution: whether a match has been found. */
static int nfa_match;
static proftime_T  *nfa_time_limit;
static int         nfa_time_count;

static void copy_pim(nfa_pim_T *to, nfa_pim_T *from);
static void clear_sub(regsub_T *sub);
static void copy_sub(regsub_T *to, regsub_T *from);
static void copy_sub_off(regsub_T *to, regsub_T *from);
static void copy_ze_off(regsub_T *to, regsub_T *from);
static int sub_equal(regsub_T *sub1, regsub_T *sub2);
static int match_backref(regsub_T *sub, int subidx, int *bytelen);
static int has_state_with_pos(nfa_list_T *l, nfa_state_T *state, regsubs_T *subs, nfa_pim_T *pim);
static int pim_equal(nfa_pim_T *one, nfa_pim_T *two);
static int state_in_list(nfa_list_T *l, nfa_state_T *state, regsubs_T *subs);
static regsubs_T *addstate(nfa_list_T *l, nfa_state_T *state, regsubs_T *subs_arg, nfa_pim_T *pim, int off);
static void addstate_here(nfa_list_T *l, nfa_state_T *state, regsubs_T *subs, nfa_pim_T *pim, int *ip);

/*
 * Copy postponed invisible match info from "from" to "to".
 */
    static void
copy_pim(to, from)
    nfa_pim_T *to;
    nfa_pim_T *from;
{
    to->result = from->result;
    to->state = from->state;
    copy_sub(&to->subs.norm, &from->subs.norm);
    if (nfa_has_zsubexpr)
        copy_sub(&to->subs.synt, &from->subs.synt);
    to->end = from->end;
}

    static void
clear_sub(sub)
    regsub_T *sub;
{
    if (REG_MULTI)
        /* Use 0xff to set lnum to -1 */
        vim_memset(sub->list.multi, 0xff, sizeof(struct multipos) * nfa_nsubexpr);
    else
        vim_memset(sub->list.line, 0, sizeof(struct linepos) * nfa_nsubexpr);
    sub->in_use = 0;
}

/*
 * Copy the submatches from "from" to "to".
 */
    static void
copy_sub(to, from)
    regsub_T    *to;
    regsub_T    *from;
{
    to->in_use = from->in_use;
    if (from->in_use > 0)
    {
        /* Copy the match start and end positions. */
        if (REG_MULTI)
            mch_memmove(&to->list.multi[0],
                        &from->list.multi[0],
                        sizeof(struct multipos) * from->in_use);
        else
            mch_memmove(&to->list.line[0],
                        &from->list.line[0],
                        sizeof(struct linepos) * from->in_use);
    }
}

/*
 * Like copy_sub() but exclude the main match.
 */
    static void
copy_sub_off(to, from)
    regsub_T    *to;
    regsub_T    *from;
{
    if (to->in_use < from->in_use)
        to->in_use = from->in_use;
    if (from->in_use > 1)
    {
        /* Copy the match start and end positions. */
        if (REG_MULTI)
            mch_memmove(&to->list.multi[1],
                        &from->list.multi[1],
                        sizeof(struct multipos) * (from->in_use - 1));
        else
            mch_memmove(&to->list.line[1],
                        &from->list.line[1],
                        sizeof(struct linepos) * (from->in_use - 1));
    }
}

/*
 * Like copy_sub() but only do the end of the main match if \ze is present.
 */
    static void
copy_ze_off(to, from)
    regsub_T    *to;
    regsub_T    *from;
{
    if (nfa_has_zend)
    {
        if (REG_MULTI)
        {
            if (from->list.multi[0].end_lnum >= 0)
            {
                to->list.multi[0].end_lnum = from->list.multi[0].end_lnum;
                to->list.multi[0].end_col = from->list.multi[0].end_col;
            }
        }
        else
        {
            if (from->list.line[0].end != NULL)
                to->list.line[0].end = from->list.line[0].end;
        }
    }
}

/*
 * Return TRUE if "sub1" and "sub2" have the same start positions.
 * When using back-references also check the end position.
 */
    static int
sub_equal(sub1, sub2)
    regsub_T    *sub1;
    regsub_T    *sub2;
{
    int         i;
    int         todo;
    linenr_T    s1;
    linenr_T    s2;
    char_u      *sp1;
    char_u      *sp2;

    todo = sub1->in_use > sub2->in_use ? sub1->in_use : sub2->in_use;
    if (REG_MULTI)
    {
        for (i = 0; i < todo; ++i)
        {
            if (i < sub1->in_use)
                s1 = sub1->list.multi[i].start_lnum;
            else
                s1 = -1;
            if (i < sub2->in_use)
                s2 = sub2->list.multi[i].start_lnum;
            else
                s2 = -1;
            if (s1 != s2)
                return FALSE;
            if (s1 != -1 && sub1->list.multi[i].start_col != sub2->list.multi[i].start_col)
                return FALSE;

            if (nfa_has_backref)
            {
                if (i < sub1->in_use)
                    s1 = sub1->list.multi[i].end_lnum;
                else
                    s1 = -1;
                if (i < sub2->in_use)
                    s2 = sub2->list.multi[i].end_lnum;
                else
                    s2 = -1;
                if (s1 != s2)
                    return FALSE;
                if (s1 != -1 && sub1->list.multi[i].end_col != sub2->list.multi[i].end_col)
                    return FALSE;
            }
        }
    }
    else
    {
        for (i = 0; i < todo; ++i)
        {
            if (i < sub1->in_use)
                sp1 = sub1->list.line[i].start;
            else
                sp1 = NULL;
            if (i < sub2->in_use)
                sp2 = sub2->list.line[i].start;
            else
                sp2 = NULL;
            if (sp1 != sp2)
                return FALSE;
            if (nfa_has_backref)
            {
                if (i < sub1->in_use)
                    sp1 = sub1->list.line[i].end;
                else
                    sp1 = NULL;
                if (i < sub2->in_use)
                    sp2 = sub2->list.line[i].end;
                else
                    sp2 = NULL;
                if (sp1 != sp2)
                    return FALSE;
            }
        }
    }

    return TRUE;
}

/*
 * Return TRUE if the same state is already in list "l" with the same
 * positions as "subs".
 */
    static int
has_state_with_pos(l, state, subs, pim)
    nfa_list_T          *l;     /* runtime state list */
    nfa_state_T         *state; /* state to update */
    regsubs_T           *subs;  /* pointers to subexpressions */
    nfa_pim_T           *pim;   /* postponed match or NULL */
{
    nfa_thread_T        *thread;

    for (int i = 0; i < l->n; ++i)
    {
        thread = &l->t[i];
        if (thread->state->id == state->id
                && sub_equal(&thread->subs.norm, &subs->norm)
                && (!nfa_has_zsubexpr || sub_equal(&thread->subs.synt, &subs->synt))
                && pim_equal(&thread->pim, pim))
            return TRUE;
    }

    return FALSE;
}

/*
 * Return TRUE if "one" and "two" are equal.  That includes when both are not set.
 */
    static int
pim_equal(one, two)
    nfa_pim_T *one;
    nfa_pim_T *two;
{
    int one_unused = (one == NULL || one->result == NFA_PIM_UNUSED);
    int two_unused = (two == NULL || two->result == NFA_PIM_UNUSED);

    if (one_unused)
        /* one is unused: equal when two is also unused */
        return two_unused;
    if (two_unused)
        /* one is used and two is not: not equal */
        return FALSE;
    /* compare the state id */
    if (one->state->id != two->state->id)
        return FALSE;
    /* compare the position */
    if (REG_MULTI)
        return one->end.pos.lnum == two->end.pos.lnum && one->end.pos.col == two->end.pos.col;

    return (one->end.ptr == two->end.ptr);
}

/*
 * Return TRUE if "state" leads to a NFA_MATCH without advancing the input.
 */
    static int
match_follows(startstate, depth)
    nfa_state_T *startstate;
    int         depth;
{
    nfa_state_T     *state = startstate;

    /* avoid too much recursion */
    if (depth > 10)
        return FALSE;

    while (state != NULL)
    {
        switch (state->c)
        {
            case NFA_MATCH:
            case NFA_MCLOSE:
            case NFA_END_INVISIBLE:
            case NFA_END_INVISIBLE_NEG:
            case NFA_END_PATTERN:
                return TRUE;

            case NFA_SPLIT:
                return match_follows(state->out, depth + 1) || match_follows(state->out1, depth + 1);

            case NFA_START_INVISIBLE:
            case NFA_START_INVISIBLE_FIRST:
            case NFA_START_INVISIBLE_BEFORE:
            case NFA_START_INVISIBLE_BEFORE_FIRST:
            case NFA_START_INVISIBLE_NEG:
            case NFA_START_INVISIBLE_NEG_FIRST:
            case NFA_START_INVISIBLE_BEFORE_NEG:
            case NFA_START_INVISIBLE_BEFORE_NEG_FIRST:
            case NFA_COMPOSING:
                /* skip ahead to next state */
                state = state->out1->out;
                continue;

            case NFA_ANY:
            case NFA_ANY_COMPOSING:
            case NFA_IDENT:
            case NFA_SIDENT:
            case NFA_KWORD:
            case NFA_SKWORD:
            case NFA_FNAME:
            case NFA_SFNAME:
            case NFA_PRINT:
            case NFA_SPRINT:
            case NFA_WHITE:
            case NFA_NWHITE:
            case NFA_DIGIT:
            case NFA_NDIGIT:
            case NFA_HEX:
            case NFA_NHEX:
            case NFA_OCTAL:
            case NFA_NOCTAL:
            case NFA_WORD:
            case NFA_NWORD:
            case NFA_HEAD:
            case NFA_NHEAD:
            case NFA_ALPHA:
            case NFA_NALPHA:
            case NFA_LOWER:
            case NFA_NLOWER:
            case NFA_UPPER:
            case NFA_NUPPER:
            case NFA_LOWER_IC:
            case NFA_NLOWER_IC:
            case NFA_UPPER_IC:
            case NFA_NUPPER_IC:
            case NFA_START_COLL:
            case NFA_START_NEG_COLL:
            case NFA_NEWL:
                /* state will advance input */
                return FALSE;

            default:
                if (state->c > 0)
                    /* state will advance input */
                    return FALSE;

                /* Others: zero-width or possibly zero-width, might still find
                 * a match at the same position, keep looking. */
                break;
        }
        state = state->out;
    }
    return FALSE;
}

/*
 * Return TRUE if "state" is already in list "l".
 */
    static int
state_in_list(l, state, subs)
    nfa_list_T          *l;     /* runtime state list */
    nfa_state_T         *state; /* state to update */
    regsubs_T           *subs;  /* pointers to subexpressions */
{
    if (state->lastlist[nfa_ll_index] == l->id)
    {
        if (!nfa_has_backref || has_state_with_pos(l, state, subs, NULL))
            return TRUE;
    }
    return FALSE;
}

/*
 * Add "state" and possibly what follows to state list ".".
 * Returns "subs_arg", possibly copied into temp_subs.
 */
    static regsubs_T *
addstate(l, state, subs_arg, pim, off)
    nfa_list_T          *l;         /* runtime state list */
    nfa_state_T         *state;     /* state to update */
    regsubs_T           *subs_arg;  /* pointers to subexpressions */
    nfa_pim_T           *pim;       /* postponed look-behind match */
    int                 off;        /* byte offset, when -1 go to next line */
{
    int                 subidx;
    nfa_thread_T        *thread;
    lpos_T              save_lpos;
    int                 save_in_use;
    char_u              *save_ptr;
    int                 i;
    regsub_T            *sub;
    regsubs_T           *subs = subs_arg;
    static regsubs_T    temp_subs;

    switch (state->c)
    {
        case NFA_NCLOSE:
        case NFA_MCLOSE:
        case NFA_MCLOSE1:
        case NFA_MCLOSE2:
        case NFA_MCLOSE3:
        case NFA_MCLOSE4:
        case NFA_MCLOSE5:
        case NFA_MCLOSE6:
        case NFA_MCLOSE7:
        case NFA_MCLOSE8:
        case NFA_MCLOSE9:
        case NFA_ZCLOSE:
        case NFA_ZCLOSE1:
        case NFA_ZCLOSE2:
        case NFA_ZCLOSE3:
        case NFA_ZCLOSE4:
        case NFA_ZCLOSE5:
        case NFA_ZCLOSE6:
        case NFA_ZCLOSE7:
        case NFA_ZCLOSE8:
        case NFA_ZCLOSE9:
        case NFA_MOPEN:
        case NFA_ZEND:
        case NFA_SPLIT:
        case NFA_EMPTY:
            /* These nodes are not added themselves but their "out" and/or
             * "out1" may be added below. */
            break;

        case NFA_BOL:
        case NFA_BOF:
            /* "^" won't match past end-of-line, don't bother trying.
             * Except when at the end of the line, or when we are going to the
             * next line for a look-behind match. */
            if (reginput > regline
                    && *reginput != NUL
                    && (nfa_endp == NULL || !REG_MULTI || reglnum == nfa_endp->se_u.pos.lnum))
                goto skip_add;
            /* FALLTHROUGH */

        case NFA_MOPEN1:
        case NFA_MOPEN2:
        case NFA_MOPEN3:
        case NFA_MOPEN4:
        case NFA_MOPEN5:
        case NFA_MOPEN6:
        case NFA_MOPEN7:
        case NFA_MOPEN8:
        case NFA_MOPEN9:
        case NFA_ZOPEN:
        case NFA_ZOPEN1:
        case NFA_ZOPEN2:
        case NFA_ZOPEN3:
        case NFA_ZOPEN4:
        case NFA_ZOPEN5:
        case NFA_ZOPEN6:
        case NFA_ZOPEN7:
        case NFA_ZOPEN8:
        case NFA_ZOPEN9:
        case NFA_NOPEN:
        case NFA_ZSTART:
            /* These nodes need to be added so that we can bail out when it
             * was added to this list before at the same position to avoid an
             * endless loop for "\(\)*" */

        default:
            if (state->lastlist[nfa_ll_index] == l->id && state->c != NFA_SKIP)
            {
                /* This state is already in the list, don't add it again,
                 * unless it is an MOPEN that is used for a backreference or
                 * when there is a PIM. For NFA_MATCH check the position,
                 * lower position is preferred. */
                if (!nfa_has_backref && pim == NULL && !l->has_pim && state->c != NFA_MATCH)
                {
skip_add:
                    return subs;
                }

                /* Do not add the state again when it exists with the same positions. */
                if (has_state_with_pos(l, state, subs, pim))
                    goto skip_add;
            }

            /* When there are backreferences or PIMs the number of states may
             * be (a lot) bigger than anticipated. */
            if (l->n == l->len)
            {
                int newlen = l->len * 3 / 2 + 50;

                if (subs != &temp_subs)
                {
                    /* "subs" may point into the current array, need to make a
                     * copy before it becomes invalid. */
                    copy_sub(&temp_subs.norm, &subs->norm);
                    if (nfa_has_zsubexpr)
                        copy_sub(&temp_subs.synt, &subs->synt);
                    subs = &temp_subs;
                }

                /* TODO: check for realloc() returning NULL. */
                l->t = realloc(l->t, newlen * sizeof(nfa_thread_T));
                l->len = newlen;
            }

            /* add the state to the list */
            state->lastlist[nfa_ll_index] = l->id;
            thread = &l->t[l->n++];
            thread->state = state;
            if (pim == NULL)
                thread->pim.result = NFA_PIM_UNUSED;
            else
            {
                copy_pim(&thread->pim, pim);
                l->has_pim = TRUE;
            }
            copy_sub(&thread->subs.norm, &subs->norm);
            if (nfa_has_zsubexpr)
                copy_sub(&thread->subs.synt, &subs->synt);
    }

    switch (state->c)
    {
        case NFA_MATCH:
            break;

        case NFA_SPLIT:
            /* order matters here */
            subs = addstate(l, state->out, subs, pim, off);
            subs = addstate(l, state->out1, subs, pim, off);
            break;

        case NFA_EMPTY:
        case NFA_NOPEN:
        case NFA_NCLOSE:
            subs = addstate(l, state->out, subs, pim, off);
            break;

        case NFA_MOPEN:
        case NFA_MOPEN1:
        case NFA_MOPEN2:
        case NFA_MOPEN3:
        case NFA_MOPEN4:
        case NFA_MOPEN5:
        case NFA_MOPEN6:
        case NFA_MOPEN7:
        case NFA_MOPEN8:
        case NFA_MOPEN9:
        case NFA_ZOPEN:
        case NFA_ZOPEN1:
        case NFA_ZOPEN2:
        case NFA_ZOPEN3:
        case NFA_ZOPEN4:
        case NFA_ZOPEN5:
        case NFA_ZOPEN6:
        case NFA_ZOPEN7:
        case NFA_ZOPEN8:
        case NFA_ZOPEN9:
        case NFA_ZSTART:
            if (state->c == NFA_ZSTART)
            {
                subidx = 0;
                sub = &subs->norm;
            }
            else if (state->c >= NFA_ZOPEN && state->c <= NFA_ZOPEN9)
            {
                subidx = state->c - NFA_ZOPEN;
                sub = &subs->synt;
            }
            else
            {
                subidx = state->c - NFA_MOPEN;
                sub = &subs->norm;
            }

            /* avoid compiler warnings */
            save_ptr = NULL;
            save_lpos.lnum = 0;
            save_lpos.col = 0;

            /* Set the position (with "off" added) in the subexpression.  Save
             * and restore it when it was in use.  Otherwise fill any gap. */
            if (REG_MULTI)
            {
                if (subidx < sub->in_use)
                {
                    save_lpos.lnum = sub->list.multi[subidx].start_lnum;
                    save_lpos.col = sub->list.multi[subidx].start_col;
                    save_in_use = -1;
                }
                else
                {
                    save_in_use = sub->in_use;
                    for (i = sub->in_use; i < subidx; ++i)
                    {
                        sub->list.multi[i].start_lnum = -1;
                        sub->list.multi[i].end_lnum = -1;
                    }
                    sub->in_use = subidx + 1;
                }
                if (off == -1)
                {
                    sub->list.multi[subidx].start_lnum = reglnum + 1;
                    sub->list.multi[subidx].start_col = 0;
                }
                else
                {
                    sub->list.multi[subidx].start_lnum = reglnum;
                    sub->list.multi[subidx].start_col = (colnr_T)(reginput - regline + off);
                }
            }
            else
            {
                if (subidx < sub->in_use)
                {
                    save_ptr = sub->list.line[subidx].start;
                    save_in_use = -1;
                }
                else
                {
                    save_in_use = sub->in_use;
                    for (i = sub->in_use; i < subidx; ++i)
                    {
                        sub->list.line[i].start = NULL;
                        sub->list.line[i].end = NULL;
                    }
                    sub->in_use = subidx + 1;
                }
                sub->list.line[subidx].start = reginput + off;
            }

            subs = addstate(l, state->out, subs, pim, off);
            /* "subs" may have changed, need to set "sub" again */
            if (state->c >= NFA_ZOPEN && state->c <= NFA_ZOPEN9)
                sub = &subs->synt;
            else
                sub = &subs->norm;

            if (save_in_use == -1)
            {
                if (REG_MULTI)
                {
                    sub->list.multi[subidx].start_lnum = save_lpos.lnum;
                    sub->list.multi[subidx].start_col = save_lpos.col;
                }
                else
                    sub->list.line[subidx].start = save_ptr;
            }
            else
                sub->in_use = save_in_use;
            break;

        case NFA_MCLOSE:
            if (nfa_has_zend && (REG_MULTI
                        ? subs->norm.list.multi[0].end_lnum >= 0
                        : subs->norm.list.line[0].end != NULL))
            {
                /* Do not overwrite the position set by \ze. */
                subs = addstate(l, state->out, subs, pim, off);
                break;
            }
        case NFA_MCLOSE1:
        case NFA_MCLOSE2:
        case NFA_MCLOSE3:
        case NFA_MCLOSE4:
        case NFA_MCLOSE5:
        case NFA_MCLOSE6:
        case NFA_MCLOSE7:
        case NFA_MCLOSE8:
        case NFA_MCLOSE9:
        case NFA_ZCLOSE:
        case NFA_ZCLOSE1:
        case NFA_ZCLOSE2:
        case NFA_ZCLOSE3:
        case NFA_ZCLOSE4:
        case NFA_ZCLOSE5:
        case NFA_ZCLOSE6:
        case NFA_ZCLOSE7:
        case NFA_ZCLOSE8:
        case NFA_ZCLOSE9:
        case NFA_ZEND:
            if (state->c == NFA_ZEND)
            {
                subidx = 0;
                sub = &subs->norm;
            }
            else if (state->c >= NFA_ZCLOSE && state->c <= NFA_ZCLOSE9)
            {
                subidx = state->c - NFA_ZCLOSE;
                sub = &subs->synt;
            }
            else
            {
                subidx = state->c - NFA_MCLOSE;
                sub = &subs->norm;
            }

            /* We don't fill in gaps here, there must have been an MOPEN that has done that. */
            save_in_use = sub->in_use;
            if (sub->in_use <= subidx)
                sub->in_use = subidx + 1;
            if (REG_MULTI)
            {
                save_lpos.lnum = sub->list.multi[subidx].end_lnum;
                save_lpos.col = sub->list.multi[subidx].end_col;
                if (off == -1)
                {
                    sub->list.multi[subidx].end_lnum = reglnum + 1;
                    sub->list.multi[subidx].end_col = 0;
                }
                else
                {
                    sub->list.multi[subidx].end_lnum = reglnum;
                    sub->list.multi[subidx].end_col = (colnr_T)(reginput - regline + off);
                }
                /* avoid compiler warnings */
                save_ptr = NULL;
            }
            else
            {
                save_ptr = sub->list.line[subidx].end;
                sub->list.line[subidx].end = reginput + off;
                /* avoid compiler warnings */
                save_lpos.lnum = 0;
                save_lpos.col = 0;
            }

            subs = addstate(l, state->out, subs, pim, off);
            /* "subs" may have changed, need to set "sub" again */
            if (state->c >= NFA_ZCLOSE && state->c <= NFA_ZCLOSE9)
                sub = &subs->synt;
            else
                sub = &subs->norm;

            if (REG_MULTI)
            {
                sub->list.multi[subidx].end_lnum = save_lpos.lnum;
                sub->list.multi[subidx].end_col = save_lpos.col;
            }
            else
                sub->list.line[subidx].end = save_ptr;
            sub->in_use = save_in_use;
            break;
    }
    return subs;
}

/*
 * Like addstate(), but the new state(s) are put at position "*ip".
 * Used for zero-width matches, next state to use is the added one.
 * This makes sure the order of states to be tried does not change, which
 * matters for alternatives.
 */
    static void
addstate_here(l, state, subs, pim, ip)
    nfa_list_T          *l;     /* runtime state list */
    nfa_state_T         *state; /* state to update */
    regsubs_T           *subs;  /* pointers to subexpressions */
    nfa_pim_T           *pim;   /* postponed look-behind match */
    int                 *ip;
{
    int tlen = l->n;
    int count;
    int listidx = *ip;

    /* first add the state(s) at the end, so that we know how many there are */
    addstate(l, state, subs, pim, 0);

    /* when "*ip" was at the end of the list, nothing to do */
    if (listidx + 1 == tlen)
        return;

    /* re-order to put the new state at the current position */
    count = l->n - tlen;
    if (count == 0)
        return; /* no state got added */
    if (count == 1)
    {
        /* overwrite the current state */
        l->t[listidx] = l->t[l->n - 1];
    }
    else if (count > 1)
    {
        if (l->n + count - 1 >= l->len)
        {
            /* not enough space to move the new states, reallocate the list
             * and move the states to the right position */
            nfa_thread_T *newl;

            l->len = l->len * 3 / 2 + 50;
            newl = (nfa_thread_T *)alloc(l->len * sizeof(nfa_thread_T));
            if (newl == NULL)
                return;
            mch_memmove(&(newl[0]),
                    &(l->t[0]),
                    sizeof(nfa_thread_T) * listidx);
            mch_memmove(&(newl[listidx]),
                    &(l->t[l->n - count]),
                    sizeof(nfa_thread_T) * count);
            mch_memmove(&(newl[listidx + count]),
                    &(l->t[listidx + 1]),
                    sizeof(nfa_thread_T) * (l->n - count - listidx - 1));
            vim_free(l->t);
            l->t = newl;
        }
        else
        {
            /* make space for new states, then move them from the
             * end to the current position */
            mch_memmove(&(l->t[listidx + count]),
                    &(l->t[listidx + 1]),
                    sizeof(nfa_thread_T) * (l->n - listidx - 1));
            mch_memmove(&(l->t[listidx]),
                    &(l->t[l->n - 1]),
                    sizeof(nfa_thread_T) * count);
        }
    }
    --l->n;
    *ip = listidx - 1;
}

/*
 * Check character class "class" against current character c.
 */
    static int
check_char_class(class, c)
    int         class;
    int         c;
{
    switch (class)
    {
        case NFA_CLASS_ALNUM:
            if (c >= 1 && c <= 255 && isalnum(c))
                return OK;
            break;
        case NFA_CLASS_ALPHA:
            if (c >= 1 && c <= 255 && isalpha(c))
                return OK;
            break;
        case NFA_CLASS_BLANK:
            if (c == ' ' || c == '\t')
                return OK;
            break;
        case NFA_CLASS_CNTRL:
            if (c >= 1 && c <= 255 && iscntrl(c))
                return OK;
            break;
        case NFA_CLASS_DIGIT:
            if (VIM_ISDIGIT(c))
                return OK;
            break;
        case NFA_CLASS_GRAPH:
            if (c >= 1 && c <= 255 && isgraph(c))
                return OK;
            break;
        case NFA_CLASS_LOWER:
            if (vim_islower(c))
                return OK;
            break;
        case NFA_CLASS_PRINT:
            if (vim_isprintc(c))
                return OK;
            break;
        case NFA_CLASS_PUNCT:
            if (c >= 1 && c <= 255 && ispunct(c))
                return OK;
            break;
        case NFA_CLASS_SPACE:
            if ((c >= 9 && c <= 13) || (c == ' '))
                return OK;
            break;
        case NFA_CLASS_UPPER:
            if (vim_isupper(c))
                return OK;
            break;
        case NFA_CLASS_XDIGIT:
            if (vim_isxdigit(c))
                return OK;
            break;
        case NFA_CLASS_TAB:
            if (c == '\t')
                return OK;
            break;
        case NFA_CLASS_RETURN:
            if (c == '\r')
                return OK;
            break;
        case NFA_CLASS_BACKSPACE:
            if (c == '\b')
                return OK;
            break;
        case NFA_CLASS_ESCAPE:
            if (c == '\033')
                return OK;
            break;

        default:
            /* should not be here :P */
            EMSGN((char *)e_ill_char_class, class);
            return FAIL;
    }
    return FAIL;
}

/*
 * Check for a match with subexpression "subidx".
 * Return TRUE if it matches.
 */
    static int
match_backref(sub, subidx, bytelen)
    regsub_T    *sub;       /* pointers to subexpressions */
    int         subidx;
    int         *bytelen;   /* out: length of match in bytes */
{
    int         len;

    if (sub->in_use <= subidx)
    {
retempty:
        /* backref was not set, match an empty string */
        *bytelen = 0;
        return TRUE;
    }

    if (REG_MULTI)
    {
        if (sub->list.multi[subidx].start_lnum < 0 || sub->list.multi[subidx].end_lnum < 0)
            goto retempty;
        if (sub->list.multi[subidx].start_lnum == reglnum && sub->list.multi[subidx].end_lnum == reglnum)
        {
            len = sub->list.multi[subidx].end_col - sub->list.multi[subidx].start_col;
            if (cstrncmp(regline + sub->list.multi[subidx].start_col, reginput, &len) == 0)
            {
                *bytelen = len;
                return TRUE;
            }
        }
        else
        {
            if (match_with_backref(
                        sub->list.multi[subidx].start_lnum,
                        sub->list.multi[subidx].start_col,
                        sub->list.multi[subidx].end_lnum,
                        sub->list.multi[subidx].end_col,
                        bytelen) == RA_MATCH)
                return TRUE;
        }
    }
    else
    {
        if (sub->list.line[subidx].start == NULL || sub->list.line[subidx].end == NULL)
            goto retempty;
        len = (int)(sub->list.line[subidx].end - sub->list.line[subidx].start);
        if (cstrncmp(sub->list.line[subidx].start, reginput, &len) == 0)
        {
            *bytelen = len;
            return TRUE;
        }
    }
    return FALSE;
}

static int match_zref(int subidx, int *bytelen);

/*
 * Check for a match with \z subexpression "subidx".
 * Return TRUE if it matches.
 */
    static int
match_zref(subidx, bytelen)
    int         subidx;
    int         *bytelen;   /* out: length of match in bytes */
{
    int         len;

    cleanup_zsubexpr();
    if (re_extmatch_in == NULL || re_extmatch_in->matches[subidx] == NULL)
    {
        /* backref was not set, match an empty string */
        *bytelen = 0;
        return TRUE;
    }

    len = (int)STRLEN(re_extmatch_in->matches[subidx]);
    if (cstrncmp(re_extmatch_in->matches[subidx], reginput, &len) == 0)
    {
        *bytelen = len;
        return TRUE;
    }
    return FALSE;
}

/*
 * Save list IDs for all NFA states of "prog" into "list".
 * Also reset the IDs to zero.
 * Only used for the recursive value lastlist[1].
 */
    static void
nfa_save_listids(prog, list)
    nfa_regprog_T   *prog;
    int             *list;
{
    int             i;
    nfa_state_T     *p;

    /* Order in the list is reverse, it's a bit faster that way. */
    p = &prog->state[0];
    for (i = prog->nstate; --i >= 0; )
    {
        list[i] = p->lastlist[1];
        p->lastlist[1] = 0;
        ++p;
    }
}

/*
 * Restore list IDs from "list" to all NFA states.
 */
    static void
nfa_restore_listids(prog, list)
    nfa_regprog_T   *prog;
    int             *list;
{
    int             i;
    nfa_state_T     *p;

    p = &prog->state[0];
    for (i = prog->nstate; --i >= 0; )
    {
        p->lastlist[1] = list[i];
        ++p;
    }
}

    static int
nfa_re_num_cmp(val, op, pos)
    long_u      val;
    int         op;
    long_u      pos;
{
    if (op == 1) return pos > val;
    if (op == 2) return pos < val;
    return val == pos;
}

static int recursive_regmatch(nfa_state_T *state, nfa_pim_T *pim, nfa_regprog_T *prog, regsubs_T *submatch, regsubs_T *m, int **listids);
static int nfa_regmatch(nfa_regprog_T *prog, nfa_state_T *start, regsubs_T *submatch, regsubs_T *m);

/*
 * Recursively call nfa_regmatch()
 * "pim" is NULL or contains info about a Postponed Invisible Match (start position).
 */
    static int
recursive_regmatch(state, pim, prog, submatch, m, listids)
    nfa_state_T     *state;
    nfa_pim_T       *pim;
    nfa_regprog_T   *prog;
    regsubs_T       *submatch;
    regsubs_T       *m;
    int             **listids;
{
    int         save_reginput_col = (int)(reginput - regline);
    int         save_reglnum = reglnum;
    int         save_nfa_match = nfa_match;
    int         save_nfa_listid = nfa_listid;
    save_se_T   *save_nfa_endp = nfa_endp;
    save_se_T   endpos;
    save_se_T   *endposp = NULL;
    int         result;
    int         need_restore = FALSE;

    if (pim != NULL)
    {
        /* start at the position where the postponed match was */
        if (REG_MULTI)
            reginput = regline + pim->end.pos.col;
        else
            reginput = pim->end.ptr;
    }

    if (state->c == NFA_START_INVISIBLE_BEFORE
        || state->c == NFA_START_INVISIBLE_BEFORE_FIRST
        || state->c == NFA_START_INVISIBLE_BEFORE_NEG
        || state->c == NFA_START_INVISIBLE_BEFORE_NEG_FIRST)
    {
        /* The recursive match must end at the current position. When "pim" is
         * not NULL it specifies the current position. */
        endposp = &endpos;
        if (REG_MULTI)
        {
            if (pim == NULL)
            {
                endpos.se_u.pos.col = (int)(reginput - regline);
                endpos.se_u.pos.lnum = reglnum;
            }
            else
                endpos.se_u.pos = pim->end.pos;
        }
        else
        {
            if (pim == NULL)
                endpos.se_u.ptr = reginput;
            else
                endpos.se_u.ptr = pim->end.ptr;
        }

        /* Go back the specified number of bytes, or as far as the
         * start of the previous line, to try matching "\@<=" or
         * not matching "\@<!". This is very inefficient, limit the number of
         * bytes if possible. */
        if (state->val <= 0)
        {
            if (REG_MULTI)
            {
                regline = reg_getline(--reglnum);
                if (regline == NULL)
                    /* can't go before the first line */
                    regline = reg_getline(++reglnum);
            }
            reginput = regline;
        }
        else
        {
            if (REG_MULTI && (int)(reginput - regline) < state->val)
            {
                /* Not enough bytes in this line, go to end of previous line. */
                regline = reg_getline(--reglnum);
                if (regline == NULL)
                {
                    /* can't go before the first line */
                    regline = reg_getline(++reglnum);
                    reginput = regline;
                }
                else
                    reginput = regline + STRLEN(regline);
            }
            if ((int)(reginput - regline) >= state->val)
            {
                reginput -= state->val;
                reginput -= utf_head_off(regline, reginput);
            }
            else
                reginput = regline;
        }
    }

    /* Have to clear the lastlist field of the NFA nodes, so that
     * nfa_regmatch() and addstate() can run properly after recursion. */
    if (nfa_ll_index == 1)
    {
        /* Already calling nfa_regmatch() recursively.  Save the lastlist[1]
         * values and clear them. */
        if (*listids == NULL)
        {
            *listids = (int *)lalloc(sizeof(int) * nstate, TRUE);
            if (*listids == NULL)
            {
                EMSG("E878: (NFA) Could not allocate memory for branch traversal!");
                return 0;
            }
        }
        nfa_save_listids(prog, *listids);
        need_restore = TRUE;
        /* any value of nfa_listid will do */
    }
    else
    {
        /* First recursive nfa_regmatch() call, switch to the second lastlist
         * entry.  Make sure nfa_listid is different from a previous recursive
         * call, because some states may still have this ID. */
        ++nfa_ll_index;
        if (nfa_listid <= nfa_alt_listid)
            nfa_listid = nfa_alt_listid;
    }

    /* Call nfa_regmatch() to check if the current concat matches at this
     * position. The concat ends with the node NFA_END_INVISIBLE */
    nfa_endp = endposp;
    result = nfa_regmatch(prog, state->out, submatch, m);

    if (need_restore)
        nfa_restore_listids(prog, *listids);
    else
    {
        --nfa_ll_index;
        nfa_alt_listid = nfa_listid;
    }

    /* restore position in input text */
    reglnum = save_reglnum;
    if (REG_MULTI)
        regline = reg_getline(reglnum);
    reginput = regline + save_reginput_col;
    nfa_match = save_nfa_match;
    nfa_endp = save_nfa_endp;
    nfa_listid = save_nfa_listid;

    return result;
}

static int skip_to_start(int c, colnr_T *colp);
static long find_match_text(colnr_T startcol, int regstart, char_u *match_text);

/*
 * Estimate the chance of a match with "state" failing.
 * empty match: 0
 * NFA_ANY: 1
 * specific character: 99
 */
    static int
failure_chance(state, depth)
    nfa_state_T *state;
    int         depth;
{
    int c = state->c;
    int l, r;

    /* detect looping */
    if (depth > 4)
        return 1;

    switch (c)
    {
        case NFA_SPLIT:
            if (state->out->c == NFA_SPLIT || state->out1->c == NFA_SPLIT)
                /* avoid recursive stuff */
                return 1;
            /* two alternatives, use the lowest failure chance */
            l = failure_chance(state->out, depth + 1);
            r = failure_chance(state->out1, depth + 1);
            return l < r ? l : r;

        case NFA_ANY:
            /* matches anything, unlikely to fail */
            return 1;

        case NFA_MATCH:
        case NFA_MCLOSE:
        case NFA_ANY_COMPOSING:
            /* empty match works always */
            return 0;

        case NFA_START_INVISIBLE:
        case NFA_START_INVISIBLE_FIRST:
        case NFA_START_INVISIBLE_NEG:
        case NFA_START_INVISIBLE_NEG_FIRST:
        case NFA_START_INVISIBLE_BEFORE:
        case NFA_START_INVISIBLE_BEFORE_FIRST:
        case NFA_START_INVISIBLE_BEFORE_NEG:
        case NFA_START_INVISIBLE_BEFORE_NEG_FIRST:
        case NFA_START_PATTERN:
            /* recursive regmatch is expensive, use low failure chance */
            return 5;

        case NFA_BOL:
        case NFA_EOL:
        case NFA_BOF:
        case NFA_EOF:
        case NFA_NEWL:
            return 99;

        case NFA_BOW:
        case NFA_EOW:
            return 90;

        case NFA_MOPEN:
        case NFA_MOPEN1:
        case NFA_MOPEN2:
        case NFA_MOPEN3:
        case NFA_MOPEN4:
        case NFA_MOPEN5:
        case NFA_MOPEN6:
        case NFA_MOPEN7:
        case NFA_MOPEN8:
        case NFA_MOPEN9:
        case NFA_ZOPEN:
        case NFA_ZOPEN1:
        case NFA_ZOPEN2:
        case NFA_ZOPEN3:
        case NFA_ZOPEN4:
        case NFA_ZOPEN5:
        case NFA_ZOPEN6:
        case NFA_ZOPEN7:
        case NFA_ZOPEN8:
        case NFA_ZOPEN9:
        case NFA_ZCLOSE:
        case NFA_ZCLOSE1:
        case NFA_ZCLOSE2:
        case NFA_ZCLOSE3:
        case NFA_ZCLOSE4:
        case NFA_ZCLOSE5:
        case NFA_ZCLOSE6:
        case NFA_ZCLOSE7:
        case NFA_ZCLOSE8:
        case NFA_ZCLOSE9:
        case NFA_NOPEN:
        case NFA_MCLOSE1:
        case NFA_MCLOSE2:
        case NFA_MCLOSE3:
        case NFA_MCLOSE4:
        case NFA_MCLOSE5:
        case NFA_MCLOSE6:
        case NFA_MCLOSE7:
        case NFA_MCLOSE8:
        case NFA_MCLOSE9:
        case NFA_NCLOSE:
            return failure_chance(state->out, depth + 1);

        case NFA_BACKREF1:
        case NFA_BACKREF2:
        case NFA_BACKREF3:
        case NFA_BACKREF4:
        case NFA_BACKREF5:
        case NFA_BACKREF6:
        case NFA_BACKREF7:
        case NFA_BACKREF8:
        case NFA_BACKREF9:
        case NFA_ZREF1:
        case NFA_ZREF2:
        case NFA_ZREF3:
        case NFA_ZREF4:
        case NFA_ZREF5:
        case NFA_ZREF6:
        case NFA_ZREF7:
        case NFA_ZREF8:
        case NFA_ZREF9:
            /* backreferences don't match in many places */
            return 94;

        case NFA_LNUM_GT:
        case NFA_LNUM_LT:
        case NFA_COL_GT:
        case NFA_COL_LT:
        case NFA_VCOL_GT:
        case NFA_VCOL_LT:
        case NFA_MARK_GT:
        case NFA_MARK_LT:
        case NFA_VISUAL:
            /* before/after positions don't match very often */
            return 85;

        case NFA_LNUM:
            return 90;

        case NFA_CURSOR:
        case NFA_COL:
        case NFA_VCOL:
        case NFA_MARK:
            /* specific positions rarely match */
            return 98;

        case NFA_COMPOSING:
            return 95;

        default:
            if (c > 0)
                /* character match fails often */
                return 95;
    }

    /* something else, includes character classes */
    return 50;
}

/*
 * Skip until the char "c" we know a match must start with.
 */
    static int
skip_to_start(c, colp)
    int         c;
    colnr_T     *colp;
{
    char_u *s;

    /* Used often, do some work to avoid call overhead. */
    s = cstrchr(regline + *colp, c);
    if (s == NULL)
        return FAIL;
    *colp = (int)(s - regline);
    return OK;
}

/*
 * Check for a match with match_text.
 * Called after skip_to_start() has found regstart.
 * Returns zero for no match, 1 for a match.
 */
    static long
find_match_text(startcol, regstart, match_text)
    colnr_T startcol;
    int     regstart;
    char_u  *match_text;
{
    colnr_T col = startcol;
    int     c1, c2;
    int     len1, len2;
    int     match;

    for (;;)
    {
        match = TRUE;
        len2 = utf_char2len(regstart); /* skip regstart */
        for (len1 = 0; match_text[len1] != NUL; len1 += utf_char2len(c1))
        {
            c1 = utf_ptr2char(match_text + len1);
            c2 = utf_ptr2char(regline + col + len2);
            if (c1 != c2 && (!ireg_ic || vim_tolower(c1) != vim_tolower(c2)))
            {
                match = FALSE;
                break;
            }
            len2 += utf_char2len(c2);
        }
        if (match
                /* check that no composing char follows */
                && !utf_iscomposing(utf_ptr2char(regline + col + len2)))
        {
            cleanup_subexpr();
            if (REG_MULTI)
            {
                reg_startpos[0].lnum = reglnum;
                reg_startpos[0].col = col;
                reg_endpos[0].lnum = reglnum;
                reg_endpos[0].col = col + len2;
            }
            else
            {
                reg_startp[0] = regline + col;
                reg_endp[0] = regline + col + len2;
            }
            return 1L;
        }

        /* Try finding regstart after the current match. */
        col += utf_char2len(regstart); /* skip regstart */
        if (skip_to_start(regstart, &col) == FAIL)
            break;
    }
    return 0L;
}

/*
 * Main matching routine.
 *
 * Run NFA to determine whether it matches reginput.
 *
 * When "nfa_endp" is not NULL it is a required end-of-match position.
 *
 * Return TRUE if there is a match, FALSE otherwise.
 * When there is a match "submatch" contains the positions.
 * Note: Caller must ensure that: start != NULL.
 */
    static int
nfa_regmatch(prog, start, submatch, m)
    nfa_regprog_T       *prog;
    nfa_state_T         *start;
    regsubs_T           *submatch;
    regsubs_T           *m;
{
    int         result;
    size_t      size = 0;
    int         flag = 0;
    int         go_to_nextline = FALSE;
    nfa_thread_T *t;
    nfa_list_T  list[2];
    int         listidx;
    nfa_list_T  *thislist;
    nfa_list_T  *nextlist;
    int         *listids = NULL;
    nfa_state_T *add_state;
    int         add_here;
    int         add_count;
    int         add_off = 0;
    int         toplevel = start->c == NFA_MOPEN;
    /* Some patterns may take a long time to match, especially when using
     * recursive_regmatch(). Allow interrupting them with CTRL-C. */
    fast_breakcheck();
    if (got_int)
        return FALSE;
    if (nfa_time_limit != NULL && profile_passed_limit(nfa_time_limit))
        return FALSE;

    nfa_match = FALSE;

    /* Allocate memory for the lists of nodes. */
    size = (nstate + 1) * sizeof(nfa_thread_T);

    list[0].t = (nfa_thread_T *)lalloc(size, TRUE);
    list[0].len = nstate + 1;
    list[1].t = (nfa_thread_T *)lalloc(size, TRUE);
    list[1].len = nstate + 1;
    if (list[0].t == NULL || list[1].t == NULL)
        goto theend;

    thislist = &list[0];
    thislist->n = 0;
    thislist->has_pim = FALSE;
    nextlist = &list[1];
    nextlist->n = 0;
    nextlist->has_pim = FALSE;
    thislist->id = nfa_listid + 1;

    /* Inline optimized code for addstate(thislist, start, m, 0) if we know
     * it's the first MOPEN. */
    if (toplevel)
    {
        if (REG_MULTI)
        {
            m->norm.list.multi[0].start_lnum = reglnum;
            m->norm.list.multi[0].start_col = (colnr_T)(reginput - regline);
        }
        else
            m->norm.list.line[0].start = reginput;
        m->norm.in_use = 1;
        addstate(thislist, start->out, m, NULL, 0);
    }
    else
        addstate(thislist, start, m, NULL, 0);

#define ADD_STATE_IF_MATCH(state)   \
    if (result) {                   \
        add_state = state->out;     \
        add_off = clen;             \
    }

    /*
     * Run for each character.
     */
    for (;;)
    {
        int curc = utf_ptr2char(reginput);
        int clen = utfc_ptr2len(reginput);
        if (curc == NUL)
        {
            clen = 0;
            go_to_nextline = FALSE;
        }

        /* swap lists */
        thislist = &list[flag];
        nextlist = &list[flag ^= 1];
        nextlist->n = 0;            /* clear nextlist */
        nextlist->has_pim = FALSE;
        ++nfa_listid;
        if (prog->re_engine == AUTOMATIC_ENGINE && nfa_listid >= NFA_MAX_STATES)
        {
            /* too many states, retry with old engine */
            nfa_match = NFA_TOO_EXPENSIVE;
            goto theend;
        }

        thislist->id = nfa_listid;
        nextlist->id = nfa_listid + 1;

        /*
         * If the state lists are empty we can stop.
         */
        if (thislist->n == 0)
            break;

        /* compute nextlist */
        for (listidx = 0; listidx < thislist->n; ++listidx)
        {
            t = &thislist->t[listidx];

            /*
             * Handle the possible codes of the current state.
             * The most important is NFA_MATCH.
             */
            add_state = NULL;
            add_here = FALSE;
            add_count = 0;
            switch (t->state->c)
            {
            case NFA_MATCH:
              {
                /* If the match ends before a composing characters and
                 * ireg_icombine is not set, that is not really a match. */
                if (!ireg_icombine && utf_iscomposing(curc))
                    break;
                nfa_match = TRUE;
                copy_sub(&submatch->norm, &t->subs.norm);
                if (nfa_has_zsubexpr)
                    copy_sub(&submatch->synt, &t->subs.synt);
                /* Found the left-most longest match, do not look at any other
                 * states at this position.  When the list of states is going
                 * to be empty quit without advancing, so that "reginput" is
                 * correct. */
                if (nextlist->n == 0)
                    clen = 0;
                goto nextchar;
              }

            case NFA_END_INVISIBLE:
            case NFA_END_INVISIBLE_NEG:
            case NFA_END_PATTERN:
                /*
                 * This is only encountered after a NFA_START_INVISIBLE or
                 * NFA_START_INVISIBLE_BEFORE node.
                 * They surround a zero-width group, used with "\@=", "\&",
                 * "\@!", "\@<=" and "\@<!".
                 * If we got here, it means that the current "invisible" group
                 * finished successfully, so return control to the parent
                 * nfa_regmatch().  For a look-behind match only when it ends
                 * in the position in "nfa_endp".
                 * Submatches are stored in *m, and used in the parent call.
                 */
                /* If "nfa_endp" is set it's only a match if it ends at "nfa_endp" */
                if (nfa_endp != NULL && (REG_MULTI
                        ? (reglnum != nfa_endp->se_u.pos.lnum
                            || (int)(reginput - regline) != nfa_endp->se_u.pos.col)
                        : reginput != nfa_endp->se_u.ptr))
                    break;

                /* do not set submatches for \@! */
                if (t->state->c != NFA_END_INVISIBLE_NEG)
                {
                    copy_sub(&m->norm, &t->subs.norm);
                    if (nfa_has_zsubexpr)
                        copy_sub(&m->synt, &t->subs.synt);
                }
                nfa_match = TRUE;
                /* See comment above at "goto nextchar". */
                if (nextlist->n == 0)
                    clen = 0;
                goto nextchar;

            case NFA_START_INVISIBLE:
            case NFA_START_INVISIBLE_FIRST:
            case NFA_START_INVISIBLE_NEG:
            case NFA_START_INVISIBLE_NEG_FIRST:
            case NFA_START_INVISIBLE_BEFORE:
            case NFA_START_INVISIBLE_BEFORE_FIRST:
            case NFA_START_INVISIBLE_BEFORE_NEG:
            case NFA_START_INVISIBLE_BEFORE_NEG_FIRST:
                {
                    /* Do it directly if there already is a PIM or when
                     * nfa_postprocess() detected it will work better. */
                    if (t->pim.result != NFA_PIM_UNUSED
                         || t->state->c == NFA_START_INVISIBLE_FIRST
                         || t->state->c == NFA_START_INVISIBLE_NEG_FIRST
                         || t->state->c == NFA_START_INVISIBLE_BEFORE_FIRST
                         || t->state->c == NFA_START_INVISIBLE_BEFORE_NEG_FIRST)
                    {
                        int in_use = m->norm.in_use;

                        /* Copy submatch info for the recursive call, opposite
                         * of what happens on success below. */
                        copy_sub_off(&m->norm, &t->subs.norm);
                        if (nfa_has_zsubexpr)
                            copy_sub_off(&m->synt, &t->subs.synt);

                        /*
                         * First try matching the invisible match, then what follows.
                         */
                        result = recursive_regmatch(t->state, NULL, prog, submatch, m, &listids);
                        if (result == NFA_TOO_EXPENSIVE)
                        {
                            nfa_match = result;
                            goto theend;
                        }

                        /* for \@! and \@<! it is a match when the result is FALSE */
                        if (result != (t->state->c == NFA_START_INVISIBLE_NEG
                               || t->state->c == NFA_START_INVISIBLE_NEG_FIRST
                               || t->state->c == NFA_START_INVISIBLE_BEFORE_NEG
                               || t->state->c == NFA_START_INVISIBLE_BEFORE_NEG_FIRST))
                        {
                            /* Copy submatch info from the recursive call */
                            copy_sub_off(&t->subs.norm, &m->norm);
                            if (nfa_has_zsubexpr)
                                copy_sub_off(&t->subs.synt, &m->synt);
                            /* If the pattern has \ze and it matched in the
                             * sub pattern, use it. */
                            copy_ze_off(&t->subs.norm, &m->norm);

                            /* t->state->out1 is the corresponding
                             * END_INVISIBLE node; Add its out to the current
                             * list (zero-width match). */
                            add_here = TRUE;
                            add_state = t->state->out1->out;
                        }
                        m->norm.in_use = in_use;
                    }
                    else
                    {
                        nfa_pim_T pim;

                        /*
                         * First try matching what follows.  Only if a match
                         * is found verify the invisible match matches.  Add a
                         * nfa_pim_T to the following states, it contains info
                         * about the invisible match.
                         */
                        pim.state = t->state;
                        pim.result = NFA_PIM_TODO;
                        pim.subs.norm.in_use = 0;
                        pim.subs.synt.in_use = 0;
                        if (REG_MULTI)
                        {
                            pim.end.pos.col = (int)(reginput - regline);
                            pim.end.pos.lnum = reglnum;
                        }
                        else
                            pim.end.ptr = reginput;

                        /* t->state->out1 is the corresponding END_INVISIBLE
                         * node; Add its out to the current list (zero-width match). */
                        addstate_here(thislist, t->state->out1->out, &t->subs, &pim, &listidx);
                    }
                }
                break;

            case NFA_START_PATTERN:
              {
                nfa_state_T *skip = NULL;

                /* There is no point in trying to match the pattern if the
                 * output state is not going to be added to the list. */
                if (state_in_list(nextlist, t->state->out1->out, &t->subs))
                {
                    skip = t->state->out1->out;
                }
                else if (state_in_list(nextlist, t->state->out1->out->out, &t->subs))
                {
                    skip = t->state->out1->out->out;
                }
                else if (state_in_list(thislist, t->state->out1->out->out, &t->subs))
                {
                    skip = t->state->out1->out->out;
                }
                if (skip != NULL)
                {
                    break;
                }
                /* Copy submatch info to the recursive call, opposite of what
                 * happens afterwards. */
                copy_sub_off(&m->norm, &t->subs.norm);
                if (nfa_has_zsubexpr)
                    copy_sub_off(&m->synt, &t->subs.synt);

                /* First try matching the pattern. */
                result = recursive_regmatch(t->state, NULL, prog, submatch, m, &listids);
                if (result == NFA_TOO_EXPENSIVE)
                {
                    nfa_match = result;
                    goto theend;
                }
                if (result)
                {
                    int bytelen;

                    /* Copy submatch info from the recursive call */
                    copy_sub_off(&t->subs.norm, &m->norm);
                    if (nfa_has_zsubexpr)
                        copy_sub_off(&t->subs.synt, &m->synt);
                    /* Now we need to skip over the matched text and then
                     * continue with what follows. */
                    if (REG_MULTI)
                        /* TODO: multi-line match */
                        bytelen = m->norm.list.multi[0].end_col - (int)(reginput - regline);
                    else
                        bytelen = (int)(m->norm.list.line[0].end - reginput);

                    if (bytelen == 0)
                    {
                        /* empty match, output of corresponding
                         * NFA_END_PATTERN/NFA_SKIP to be used at current position */
                        add_here = TRUE;
                        add_state = t->state->out1->out->out;
                    }
                    else if (bytelen <= clen)
                    {
                        /* match current character, output of corresponding
                         * NFA_END_PATTERN to be used at next position. */
                        add_state = t->state->out1->out->out;
                        add_off = clen;
                    }
                    else
                    {
                        /* skip over the matched characters, set character
                         * count in NFA_SKIP */
                        add_state = t->state->out1->out;
                        add_off = bytelen;
                        add_count = bytelen - clen;
                    }
                }
                break;
              }

            case NFA_BOL:
                if (reginput == regline)
                {
                    add_here = TRUE;
                    add_state = t->state->out;
                }
                break;

            case NFA_EOL:
                if (curc == NUL)
                {
                    add_here = TRUE;
                    add_state = t->state->out;
                }
                break;

            case NFA_BOW:
                result = TRUE;

                if (curc == NUL)
                    result = FALSE;
                else
                {
                    int this_class;

                    /* Get class of current and previous char (if it exists). */
                    this_class = mb_get_class_buf(reginput, reg_buf);
                    if (this_class <= 1)
                        result = FALSE;
                    else if (reg_prev_class() == this_class)
                        result = FALSE;
                }
                if (result)
                {
                    add_here = TRUE;
                    add_state = t->state->out;
                }
                break;

            case NFA_EOW:
                result = TRUE;
                if (reginput == regline)
                    result = FALSE;
                else
                {
                    int this_class, prev_class;

                    /* Get class of current and previous char (if it exists). */
                    this_class = mb_get_class_buf(reginput, reg_buf);
                    prev_class = reg_prev_class();
                    if (this_class == prev_class || prev_class == 0 || prev_class == 1)
                        result = FALSE;
                }
                if (result)
                {
                    add_here = TRUE;
                    add_state = t->state->out;
                }
                break;

            case NFA_BOF:
                if (reglnum == 0 && reginput == regline && (!REG_MULTI || reg_firstlnum == 1))
                {
                    add_here = TRUE;
                    add_state = t->state->out;
                }
                break;

            case NFA_EOF:
                if (reglnum == reg_maxline && curc == NUL)
                {
                    add_here = TRUE;
                    add_state = t->state->out;
                }
                break;

            case NFA_COMPOSING:
            {
                int         mc = curc;
                int         len = 0;
                nfa_state_T *end;
                nfa_state_T *sta;
                int         cchars[MAX_MCO];
                int         ccount = 0;
                int         j;

                sta = t->state->out;
                len = 0;
                if (utf_iscomposing(sta->c))
                {
                    /* Only match composing character(s), ignore base
                     * character.  Used for ".{composing}" and "{composing}"
                     * (no preceding character). */
                    len += utf_char2len(mc);
                }
                if (ireg_icombine && len == 0)
                {
                    /* If \Z was present, then ignore composing characters.
                     * When ignoring the base character this always matches. */
                    if (len == 0 && sta->c != curc)
                        result = FAIL;
                    else
                        result = OK;
                    while (sta->c != NFA_END_COMPOSING)
                        sta = sta->out;
                }

                /* Check base character matches first, unless ignored. */
                else if (len > 0 || mc == sta->c)
                {
                    if (len == 0)
                    {
                        len += utf_char2len(mc);
                        sta = sta->out;
                    }

                    /* We don't care about the order of composing characters.
                     * Get them into cchars[] first. */
                    while (len < clen)
                    {
                        mc = utf_ptr2char(reginput + len);
                        cchars[ccount++] = mc;
                        len += utf_char2len(mc);
                        if (ccount == MAX_MCO)
                            break;
                    }

                    /* Check that each composing char in the pattern matches a
                     * composing char in the text.  We do not check if all
                     * composing chars are matched. */
                    result = OK;
                    while (sta->c != NFA_END_COMPOSING)
                    {
                        for (j = 0; j < ccount; ++j)
                            if (cchars[j] == sta->c)
                                break;
                        if (j == ccount)
                        {
                            result = FAIL;
                            break;
                        }
                        sta = sta->out;
                    }
                }
                else
                    result = FAIL;

                end = t->state->out1;       /* NFA_END_COMPOSING */
                ADD_STATE_IF_MATCH(end);
                break;
            }

            case NFA_NEWL:
                if (curc == NUL && !reg_line_lbr && REG_MULTI && reglnum <= reg_maxline)
                {
                    go_to_nextline = TRUE;
                    /* Pass -1 for the offset, which means taking the position
                     * at the start of the next line. */
                    add_state = t->state->out;
                    add_off = -1;
                }
                else if (curc == '\n' && reg_line_lbr)
                {
                    /* match \n as if it is an ordinary character */
                    add_state = t->state->out;
                    add_off = 1;
                }
                break;

            case NFA_START_COLL:
            case NFA_START_NEG_COLL:
              {
                /* What follows is a list of characters, until NFA_END_COLL.
                 * One of them must match or none of them must match. */
                nfa_state_T     *state;
                int             result_if_matched;
                int             c1, c2;

                /* Never match EOL. If it's part of the collection it is added
                 * as a separate state with an OR. */
                if (curc == NUL)
                    break;

                state = t->state->out;
                result_if_matched = (t->state->c == NFA_START_COLL);
                for (;;)
                {
                    if (state->c == NFA_END_COLL)
                    {
                        result = !result_if_matched;
                        break;
                    }
                    if (state->c == NFA_RANGE_MIN)
                    {
                        c1 = state->val;
                        state = state->out; /* advance to NFA_RANGE_MAX */
                        c2 = state->val;
                        if (curc >= c1 && curc <= c2)
                        {
                            result = result_if_matched;
                            break;
                        }
                        if (ireg_ic)
                        {
                            int curc_low = vim_tolower(curc);
                            int done = FALSE;

                            for ( ; c1 <= c2; ++c1)
                                if (vim_tolower(c1) == curc_low)
                                {
                                    result = result_if_matched;
                                    done = TRUE;
                                    break;
                                }
                            if (done)
                                break;
                        }
                    }
                    else if (state->c < 0 ? check_char_class(state->c, curc)
                                : (curc == state->c
                                   || (ireg_ic && vim_tolower(curc) == vim_tolower(state->c))))
                    {
                        result = result_if_matched;
                        break;
                    }
                    state = state->out;
                }
                if (result)
                {
                    /* next state is in out of the NFA_END_COLL, out1 of
                     * START points to the END state */
                    add_state = t->state->out1->out;
                    add_off = clen;
                }
                break;
              }

            case NFA_ANY:
                /* Any char except '\0', (end of input) does not match. */
                if (curc > 0)
                {
                    add_state = t->state->out;
                    add_off = clen;
                }
                break;

            case NFA_ANY_COMPOSING:
                /* On a composing character skip over it.  Otherwise do
                 * nothing.  Always matches. */
                if (utf_iscomposing(curc))
                {
                    add_off = clen;
                }
                else
                {
                    add_here = TRUE;
                    add_off = 0;
                }
                add_state = t->state->out;
                break;

            /*
             * Character classes like \a for alpha, \d for digit etc.
             */
            case NFA_IDENT:     /*  \i  */
                result = vim_isIDc(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_SIDENT:    /*  \I  */
                result = !VIM_ISDIGIT(curc) && vim_isIDc(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_KWORD:     /*  \k  */
                result = vim_iswordp_buf(reginput, reg_buf);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_SKWORD:    /*  \K  */
                result = !VIM_ISDIGIT(curc) && vim_iswordp_buf(reginput, reg_buf);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_FNAME:     /*  \f  */
                result = vim_isfilec(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_SFNAME:    /*  \F  */
                result = !VIM_ISDIGIT(curc) && vim_isfilec(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_PRINT:     /*  \p  */
                result = vim_isprintc(utf_ptr2char(reginput));
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_SPRINT:    /*  \P  */
                result = !VIM_ISDIGIT(curc) && vim_isprintc(utf_ptr2char(reginput));
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_WHITE:     /*  \s  */
                result = vim_iswhite(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_NWHITE:    /*  \S  */
                result = curc != NUL && !vim_iswhite(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_DIGIT:     /*  \d  */
                result = ri_digit(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_NDIGIT:    /*  \D  */
                result = curc != NUL && !ri_digit(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_HEX:       /*  \x  */
                result = ri_hex(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_NHEX:      /*  \X  */
                result = curc != NUL && !ri_hex(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_OCTAL:     /*  \o  */
                result = ri_octal(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_NOCTAL:    /*  \O  */
                result = curc != NUL && !ri_octal(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_WORD:      /*  \w  */
                result = ri_word(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_NWORD:     /*  \W  */
                result = curc != NUL && !ri_word(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_HEAD:      /*  \h  */
                result = ri_head(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_NHEAD:     /*  \H  */
                result = curc != NUL && !ri_head(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_ALPHA:     /*  \a  */
                result = ri_alpha(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_NALPHA:    /*  \A  */
                result = curc != NUL && !ri_alpha(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_LOWER:     /*  \l  */
                result = ri_lower(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_NLOWER:    /*  \L  */
                result = curc != NUL && !ri_lower(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_UPPER:     /*  \u  */
                result = ri_upper(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_NUPPER:    /*  \U  */
                result = curc != NUL && !ri_upper(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_LOWER_IC:  /* [a-z] */
                result = ri_lower(curc) || (ireg_ic && ri_upper(curc));
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_NLOWER_IC: /* [^a-z] */
                result = curc != NUL && !(ri_lower(curc) || (ireg_ic && ri_upper(curc)));
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_UPPER_IC:  /* [A-Z] */
                result = ri_upper(curc) || (ireg_ic && ri_lower(curc));
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_NUPPER_IC: /* ^[A-Z] */
                result = curc != NUL && !(ri_upper(curc) || (ireg_ic && ri_lower(curc)));
                ADD_STATE_IF_MATCH(t->state);
                break;

            case NFA_BACKREF1:
            case NFA_BACKREF2:
            case NFA_BACKREF3:
            case NFA_BACKREF4:
            case NFA_BACKREF5:
            case NFA_BACKREF6:
            case NFA_BACKREF7:
            case NFA_BACKREF8:
            case NFA_BACKREF9:
            case NFA_ZREF1:
            case NFA_ZREF2:
            case NFA_ZREF3:
            case NFA_ZREF4:
            case NFA_ZREF5:
            case NFA_ZREF6:
            case NFA_ZREF7:
            case NFA_ZREF8:
            case NFA_ZREF9:
                /* \1 .. \9  \z1 .. \z9 */
              {
                int subidx;
                int bytelen;

                if (t->state->c <= NFA_BACKREF9)
                {
                    subidx = t->state->c - NFA_BACKREF1 + 1;
                    result = match_backref(&t->subs.norm, subidx, &bytelen);
                }
                else
                {
                    subidx = t->state->c - NFA_ZREF1 + 1;
                    result = match_zref(subidx, &bytelen);
                }

                if (result)
                {
                    if (bytelen == 0)
                    {
                        /* empty match always works, output of NFA_SKIP to be used next */
                        add_here = TRUE;
                        add_state = t->state->out->out;
                    }
                    else if (bytelen <= clen)
                    {
                        /* match current character, jump ahead to out of NFA_SKIP */
                        add_state = t->state->out->out;
                        add_off = clen;
                    }
                    else
                    {
                        /* skip over the matched characters, set character
                         * count in NFA_SKIP */
                        add_state = t->state->out;
                        add_off = bytelen;
                        add_count = bytelen - clen;
                    }
                }
                break;
              }
            case NFA_SKIP:
              /* character of previous matching \1 .. \9  or \@> */
              if (t->count - clen <= 0)
              {
                  /* end of match, go to what follows */
                  add_state = t->state->out;
                  add_off = clen;
              }
              else
              {
                  /* add state again with decremented count */
                  add_state = t->state;
                  add_off = 0;
                  add_count = t->count - clen;
              }
              break;

            case NFA_LNUM:
            case NFA_LNUM_GT:
            case NFA_LNUM_LT:
                result = (REG_MULTI &&
                        nfa_re_num_cmp(t->state->val, t->state->c - NFA_LNUM,
                            (long_u)(reglnum + reg_firstlnum)));
                if (result)
                {
                    add_here = TRUE;
                    add_state = t->state->out;
                }
                break;

            case NFA_COL:
            case NFA_COL_GT:
            case NFA_COL_LT:
                result = nfa_re_num_cmp(t->state->val, t->state->c - NFA_COL,
                        (long_u)(reginput - regline) + 1);
                if (result)
                {
                    add_here = TRUE;
                    add_state = t->state->out;
                }
                break;

            case NFA_VCOL:
            case NFA_VCOL_GT:
            case NFA_VCOL_LT:
                {
                    int     op = t->state->c - NFA_VCOL;
                    colnr_T col = (colnr_T)(reginput - regline);
                    win_T   *wp = (reg_win == NULL) ? curwin : reg_win;

                    /* Bail out quickly when there can't be a match, avoid the
                     * overhead of win_linetabsize() on long lines. */
                    if (op != 1 && col > t->state->val * MB_MAXBYTES)
                        break;
                    result = FALSE;
                    if (op == 1 && col - 1 > t->state->val && col > 100)
                    {
                        int ts = wp->w_buffer->b_p_ts;

                        /* Guess that a character won't use more columns than
                         * 'tabstop', with a minimum of 4. */
                        if (ts < 4)
                            ts = 4;
                        result = col > t->state->val * ts;
                    }
                    if (!result)
                        result = nfa_re_num_cmp(t->state->val, op,
                                (long_u)win_linetabsize(wp, regline, col) + 1);
                    if (result)
                    {
                        add_here = TRUE;
                        add_state = t->state->out;
                    }
                }
                break;

            case NFA_MARK:
            case NFA_MARK_GT:
            case NFA_MARK_LT:
              {
                pos_T   *pos = getmark_buf(reg_buf, t->state->val, FALSE);

                /* Compare the mark position to the match position. */
                result = (pos != NULL                /* mark doesn't exist */
                        && pos->lnum > 0    /* mark isn't set in reg_buf */
                        && (pos->lnum == reglnum + reg_firstlnum
                                ? (pos->col == (colnr_T)(reginput - regline)
                                    ? t->state->c == NFA_MARK
                                    : (pos->col < (colnr_T)(reginput - regline)
                                        ? t->state->c == NFA_MARK_GT
                                        : t->state->c == NFA_MARK_LT))
                                : (pos->lnum < reglnum + reg_firstlnum
                                    ? t->state->c == NFA_MARK_GT
                                    : t->state->c == NFA_MARK_LT)));
                if (result)
                {
                    add_here = TRUE;
                    add_state = t->state->out;
                }
                break;
              }

            case NFA_CURSOR:
                result = (reg_win != NULL
                        && (reglnum + reg_firstlnum == reg_win->w_cursor.lnum)
                        && ((colnr_T)(reginput - regline) == reg_win->w_cursor.col));
                if (result)
                {
                    add_here = TRUE;
                    add_state = t->state->out;
                }
                break;

            case NFA_VISUAL:
                result = reg_match_visual();
                if (result)
                {
                    add_here = TRUE;
                    add_state = t->state->out;
                }
                break;

            case NFA_MOPEN1:
            case NFA_MOPEN2:
            case NFA_MOPEN3:
            case NFA_MOPEN4:
            case NFA_MOPEN5:
            case NFA_MOPEN6:
            case NFA_MOPEN7:
            case NFA_MOPEN8:
            case NFA_MOPEN9:
            case NFA_ZOPEN:
            case NFA_ZOPEN1:
            case NFA_ZOPEN2:
            case NFA_ZOPEN3:
            case NFA_ZOPEN4:
            case NFA_ZOPEN5:
            case NFA_ZOPEN6:
            case NFA_ZOPEN7:
            case NFA_ZOPEN8:
            case NFA_ZOPEN9:
            case NFA_NOPEN:
            case NFA_ZSTART:
                /* These states are only added to be able to bail out when
                 * they are added again, nothing is to be done. */
                break;

            default:    /* regular character */
              {
                int c = t->state->c;

                result = (c == curc);

                if (!result && ireg_ic)
                    result = vim_tolower(c) == vim_tolower(curc);
                /* If ireg_icombine is not set only skip over the character
                 * itself.  When it is set skip over composing characters. */
                if (result && !ireg_icombine)
                    clen = utf_char2len(curc);
                ADD_STATE_IF_MATCH(t->state);
                break;
              }
            }

            if (add_state != NULL)
            {
                nfa_pim_T *pim;
                nfa_pim_T pim_copy;

                if (t->pim.result == NFA_PIM_UNUSED)
                    pim = NULL;
                else
                    pim = &t->pim;

                /* Handle the postponed invisible match if the match might end
                 * without advancing and before the end of the line. */
                if (pim != NULL && (clen == 0 || match_follows(add_state, 0)))
                {
                    if (pim->result == NFA_PIM_TODO)
                    {
                        result = recursive_regmatch(pim->state, pim, prog, submatch, m, &listids);
                        pim->result = result ? NFA_PIM_MATCH : NFA_PIM_NOMATCH;
                        /* for \@! and \@<! it is a match when the result is FALSE */
                        if (result != (pim->state->c == NFA_START_INVISIBLE_NEG
                             || pim->state->c == NFA_START_INVISIBLE_NEG_FIRST
                             || pim->state->c == NFA_START_INVISIBLE_BEFORE_NEG
                             || pim->state->c == NFA_START_INVISIBLE_BEFORE_NEG_FIRST))
                        {
                            /* Copy submatch info from the recursive call */
                            copy_sub_off(&pim->subs.norm, &m->norm);
                            if (nfa_has_zsubexpr)
                                copy_sub_off(&pim->subs.synt, &m->synt);
                        }
                    }
                    else
                    {
                        result = (pim->result == NFA_PIM_MATCH);
                    }

                    /* for \@! and \@<! it is a match when result is FALSE */
                    if (result != (pim->state->c == NFA_START_INVISIBLE_NEG
                             || pim->state->c == NFA_START_INVISIBLE_NEG_FIRST
                             || pim->state->c == NFA_START_INVISIBLE_BEFORE_NEG
                             || pim->state->c == NFA_START_INVISIBLE_BEFORE_NEG_FIRST))
                    {
                        /* Copy submatch info from the recursive call */
                        copy_sub_off(&t->subs.norm, &pim->subs.norm);
                        if (nfa_has_zsubexpr)
                            copy_sub_off(&t->subs.synt, &pim->subs.synt);
                    }
                    else
                        /* look-behind match failed, don't add the state */
                        continue;

                    /* Postponed invisible match was handled, don't add it to
                     * following states. */
                    pim = NULL;
                }

                /* If "pim" points into l->t it will become invalid when
                 * adding the state causes the list to be reallocated.  Make a
                 * local copy to avoid that. */
                if (pim == &t->pim)
                {
                    copy_pim(&pim_copy, pim);
                    pim = &pim_copy;
                }

                if (add_here)
                    addstate_here(thislist, add_state, &t->subs, pim, &listidx);
                else
                {
                    addstate(nextlist, add_state, &t->subs, pim, add_off);
                    if (add_count > 0)
                        nextlist->t[nextlist->n - 1].count = add_count;
                }
            }
        }

        /* Look for the start of a match in the current position by adding the
         * start state to the list of states.
         * The first found match is the leftmost one, thus the order of states matters!
         * Do not add the start state in recursive calls of nfa_regmatch(),
         * because recursive calls should only start in the first position.
         * Unless "nfa_endp" is not NULL, then we match the end position.
         * Also don't start a match past the first line. */
        if (nfa_match == FALSE
                && ((toplevel
                        && reglnum == 0
                        && clen != 0
                        && (ireg_maxcol == 0 || (colnr_T)(reginput - regline) < ireg_maxcol))
                    || (nfa_endp != NULL
                        && (REG_MULTI
                            ? (reglnum < nfa_endp->se_u.pos.lnum
                               || (reglnum == nfa_endp->se_u.pos.lnum
                                   && (int)(reginput - regline) < nfa_endp->se_u.pos.col))
                            : reginput < nfa_endp->se_u.ptr))))
        {
            /* Inline optimized code for addstate() if we know the state is
             * the first MOPEN. */
            if (toplevel)
            {
                int add = TRUE;
                int c;

                if (prog->regstart != NUL && clen != 0)
                {
                    if (nextlist->n == 0)
                    {
                        colnr_T col = (colnr_T)(reginput - regline) + clen;

                        /* Nextlist is empty, we can skip ahead to the
                         * character that must appear at the start. */
                        if (skip_to_start(prog->regstart, &col) == FAIL)
                            break;
                        reginput = regline + col - clen;
                    }
                    else
                    {
                        /* Checking if the required start character matches is
                         * cheaper than adding a state that won't match. */
                        c = utf_ptr2char(reginput + clen);
                        if (c != prog->regstart && (!ireg_ic || vim_tolower(c) != vim_tolower(prog->regstart)))
                        {
                            add = FALSE;
                        }
                    }
                }

                if (add)
                {
                    if (REG_MULTI)
                        m->norm.list.multi[0].start_col = (colnr_T)(reginput - regline) + clen;
                    else
                        m->norm.list.line[0].start = reginput + clen;
                    addstate(nextlist, start->out, m, NULL, clen);
                }
            }
            else
                addstate(nextlist, start, m, NULL, clen);
        }

nextchar:
        /* Advance to the next character, or advance to the next line, or finish. */
        if (clen != 0)
            reginput += clen;
        else if (go_to_nextline || (nfa_endp != NULL && REG_MULTI && reglnum < nfa_endp->se_u.pos.lnum))
            reg_nextline();
        else
            break;

        /* Allow interrupting with CTRL-C. */
        line_breakcheck();
        if (got_int)
            break;
        /* Check for timeout once in a twenty times to avoid overhead. */
        if (nfa_time_limit != NULL && ++nfa_time_count == 20)
        {
            nfa_time_count = 0;
            if (profile_passed_limit(nfa_time_limit))
                break;
        }
    }

theend:
    /* Free memory */
    vim_free(list[0].t);
    vim_free(list[1].t);
    vim_free(listids);
#undef ADD_STATE_IF_MATCH

    return nfa_match;
}

/*
 * Try match of "prog" with at regline["col"].
 * Returns <= 0 for failure, number of lines contained in the match otherwise.
 */
    static long
nfa_regtry(prog, col, tm)
    nfa_regprog_T   *prog;
    colnr_T         col;
    proftime_T      *tm UNUSED; /* timeout limit or NULL */
{
    int         i;
    regsubs_T   subs, m;
    nfa_state_T *start = prog->start;
    int         result;

    reginput = regline + col;
    nfa_time_limit = tm;
    nfa_time_count = 0;

    clear_sub(&subs.norm);
    clear_sub(&m.norm);
    clear_sub(&subs.synt);
    clear_sub(&m.synt);

    result = nfa_regmatch(prog, start, &subs, &m);
    if (result == FALSE)
        return 0;
    else if (result == NFA_TOO_EXPENSIVE)
        return result;

    cleanup_subexpr();
    if (REG_MULTI)
    {
        for (i = 0; i < subs.norm.in_use; i++)
        {
            reg_startpos[i].lnum = subs.norm.list.multi[i].start_lnum;
            reg_startpos[i].col = subs.norm.list.multi[i].start_col;

            reg_endpos[i].lnum = subs.norm.list.multi[i].end_lnum;
            reg_endpos[i].col = subs.norm.list.multi[i].end_col;
        }

        if (reg_startpos[0].lnum < 0)
        {
            reg_startpos[0].lnum = 0;
            reg_startpos[0].col = col;
        }
        if (reg_endpos[0].lnum < 0)
        {
            /* pattern has a \ze but it didn't match, use current end */
            reg_endpos[0].lnum = reglnum;
            reg_endpos[0].col = (int)(reginput - regline);
        }
        else
            /* Use line number of "\ze". */
            reglnum = reg_endpos[0].lnum;
    }
    else
    {
        for (i = 0; i < subs.norm.in_use; i++)
        {
            reg_startp[i] = subs.norm.list.line[i].start;
            reg_endp[i] = subs.norm.list.line[i].end;
        }

        if (reg_startp[0] == NULL)
            reg_startp[0] = regline + col;
        if (reg_endp[0] == NULL)
            reg_endp[0] = reginput;
    }

    /* Package any found \z(...\) matches for export. Default is none. */
    unref_extmatch(re_extmatch_out);
    re_extmatch_out = NULL;

    if (prog->reghasz == REX_SET)
    {
        cleanup_zsubexpr();
        re_extmatch_out = make_extmatch();
        for (i = 0; i < subs.synt.in_use; i++)
        {
            if (REG_MULTI)
            {
                struct multipos *mpos = &subs.synt.list.multi[i];

                /* Only accept single line matches that are valid. */
                if (mpos->start_lnum >= 0
                        && mpos->start_lnum == mpos->end_lnum
                        && mpos->end_col >= mpos->start_col)
                    re_extmatch_out->matches[i] =
                        vim_strnsave(reg_getline(mpos->start_lnum) + mpos->start_col,
                                             mpos->end_col - mpos->start_col);
            }
            else
            {
                struct linepos *lpos = &subs.synt.list.line[i];

                if (lpos->start != NULL && lpos->end != NULL)
                    re_extmatch_out->matches[i] =
                            vim_strnsave(lpos->start, (int)(lpos->end - lpos->start));
            }
        }
    }

    return 1 + reglnum;
}

/*
 * Match a regexp against a string ("line" points to the string) or multiple
 * lines ("line" is NULL, use reg_getline()).
 *
 * Returns <= 0 for failure, number of lines contained in the match otherwise.
 */
    static long
nfa_regexec_both(line, startcol, tm)
    char_u      *line;
    colnr_T     startcol;       /* column to start looking for match */
    proftime_T  *tm;            /* timeout limit or NULL */
{
    nfa_regprog_T   *prog;
    long            retval = 0L;
    int             i;
    colnr_T         col = startcol;

    if (REG_MULTI)
    {
        prog = (nfa_regprog_T *)reg_mmatch->regprog;
        line = reg_getline((linenr_T)0);    /* relative to the cursor */
        reg_startpos = reg_mmatch->startpos;
        reg_endpos = reg_mmatch->endpos;
    }
    else
    {
        prog = (nfa_regprog_T *)reg_match->regprog;
        reg_startp = reg_match->startp;
        reg_endp = reg_match->endp;
    }

    /* Be paranoid... */
    if (prog == NULL || line == NULL)
    {
        EMSG((char *)e_null);
        goto theend;
    }

    /* If pattern contains "\c" or "\C": overrule value of ireg_ic */
    if (prog->regflags & RF_ICASE)
        ireg_ic = TRUE;
    else if (prog->regflags & RF_NOICASE)
        ireg_ic = FALSE;

    /* If pattern contains "\Z" overrule value of ireg_icombine */
    if (prog->regflags & RF_ICOMBINE)
        ireg_icombine = TRUE;

    regline = line;
    reglnum = 0;    /* relative to line */

    nfa_has_zend = prog->has_zend;
    nfa_has_backref = prog->has_backref;
    nfa_nsubexpr = prog->nsubexp;
    nfa_listid = 1;
    nfa_alt_listid = 2;
    nfa_regengine.expr = prog->pattern;

    if (prog->reganch && col > 0)
        return 0L;

    need_clear_subexpr = TRUE;
    /* Clear the external match subpointers if necessary. */
    if (prog->reghasz == REX_SET)
    {
        nfa_has_zsubexpr = TRUE;
        need_clear_zsubexpr = TRUE;
    }
    else
        nfa_has_zsubexpr = FALSE;

    if (prog->regstart != NUL)
    {
        /* Skip ahead until a character we know the match must start with.
         * When there is none there is no match. */
        if (skip_to_start(prog->regstart, &col) == FAIL)
            return 0L;

        /* If match_text is set it contains the full text that must match.
         * Nothing else to try. Doesn't handle combining chars well. */
        if (prog->match_text != NULL && !ireg_icombine)
            return find_match_text(col, prog->regstart, prog->match_text);
    }

    /* If the start column is past the maximum column: no need to try. */
    if (ireg_maxcol > 0 && col >= ireg_maxcol)
        goto theend;

    nstate = prog->nstate;
    for (i = 0; i < nstate; ++i)
    {
        prog->state[i].id = i;
        prog->state[i].lastlist[0] = 0;
        prog->state[i].lastlist[1] = 0;
    }

    retval = nfa_regtry(prog, col, tm);

    nfa_regengine.expr = NULL;

theend:
    return retval;
}

/*
 * Compile a regular expression into internal code for the NFA matcher.
 * Returns the program in allocated space.  Returns NULL for an error.
 */
    static regprog_T *
nfa_regcomp(expr, re_flags)
    char_u      *expr;
    int         re_flags;
{
    nfa_regprog_T       *prog = NULL;
    size_t              prog_size;
    int                 *postfix;

    if (expr == NULL)
        return NULL;

    nfa_regengine.expr = expr;
    nfa_re_flags = re_flags;

    init_class_tab();

    if (nfa_regcomp_start(expr, re_flags) == FAIL)
        return NULL;

    /* Build postfix form of the regexp. Needed to build the NFA
     * (and count its size). */
    postfix = re2post();
    if (postfix == NULL)
    {
        /* TODO: only give this error for debugging? */
        if (post_ptr >= post_end)
            EMSGN("Internal error: estimated max number of states insufficient: %ld", post_end - post_start);
        goto fail;          /* Cascaded (syntax?) error */
    }

    /*
     * In order to build the NFA, we parse the input regexp twice:
     * 1. first pass to count size (so we can allocate space)
     * 2. second to emit code
     */

    /*
     * PASS 1
     * Count number of NFA states in "nstate". Do not build the NFA.
     */
    post2nfa(postfix, post_ptr, TRUE);

    /* allocate the regprog with space for the compiled regexp */
    prog_size = sizeof(nfa_regprog_T) + sizeof(nfa_state_T) * (nstate - 1);
    prog = (nfa_regprog_T *)lalloc(prog_size, TRUE);
    if (prog == NULL)
        goto fail;
    state_ptr = prog->state;

    /*
     * PASS 2
     * Build the NFA
     */
    prog->start = post2nfa(postfix, post_ptr, FALSE);
    if (prog->start == NULL)
        goto fail;

    prog->regflags = regflags;
    prog->engine = &nfa_regengine;
    prog->nstate = nstate;
    prog->has_zend = nfa_has_zend;
    prog->has_backref = nfa_has_backref;
    prog->nsubexp = regnpar;

    nfa_postprocess(prog);

    prog->reganch = nfa_get_reganch(prog->start, 0);
    prog->regstart = nfa_get_regstart(prog->start, 0);
    prog->match_text = nfa_get_match_text(prog->start);

    /* Remember whether this pattern has any \z specials in it. */
    prog->reghasz = re_has_z;
    prog->pattern = vim_strsave(expr);
    nfa_regengine.expr = NULL;

out:
    vim_free(post_start);
    post_start = post_ptr = post_end = NULL;
    state_ptr = NULL;
    return (regprog_T *)prog;

fail:
    vim_free(prog);
    prog = NULL;
    nfa_regengine.expr = NULL;
    goto out;
}

/*
 * Free a compiled regexp program, returned by nfa_regcomp().
 */
    static void
nfa_regfree(prog)
    regprog_T   *prog;
{
    if (prog != NULL)
    {
        vim_free(((nfa_regprog_T *)prog)->match_text);
        vim_free(((nfa_regprog_T *)prog)->pattern);
        vim_free(prog);
    }
}

/*
 * Match a regexp against a string.
 * "rmp->regprog" is a compiled regexp as returned by nfa_regcomp().
 * Uses curbuf for line count and 'iskeyword'.
 * If "line_lbr" is TRUE consider a "\n" in "line" to be a line break.
 *
 * Returns <= 0 for failure, number of lines contained in the match otherwise.
 */
    static int
nfa_regexec_nl(rmp, line, col, line_lbr)
    regmatch_T  *rmp;
    char_u      *line;  /* string to match against */
    colnr_T     col;    /* column to start looking for match */
    int         line_lbr;
{
    reg_match = rmp;
    reg_mmatch = NULL;
    reg_maxline = 0;
    reg_line_lbr = line_lbr;
    reg_buf = curbuf;
    reg_win = NULL;
    ireg_ic = rmp->rm_ic;
    ireg_icombine = FALSE;
    ireg_maxcol = 0;
    return nfa_regexec_both(line, col, NULL);
}

/*
 * Match a regexp against multiple lines.
 * "rmp->regprog" is a compiled regexp as returned by vim_regcomp().
 * Uses curbuf for line count and 'iskeyword'.
 *
 * Return <= 0 if there is no match.  Return number of lines contained in the
 * match otherwise.
 *
 * Note: the body is the same as bt_regexec() except for nfa_regexec_both()
 *
 * ! Also NOTE : match may actually be in another line. e.g.:
 * when r.e. is \nc, cursor is at 'a' and the text buffer looks like
 *
 * +-------------------------+
 * |a                        |
 * |b                        |
 * |c                        |
 * |                         |
 * +-------------------------+
 *
 * then nfa_regexec_multi() returns 3. while the original
 * vim_regexec_multi() returns 0 and a second call at line 2 will return 2.
 *
 * FIXME if this behavior is not compatible.
 */
    static long
nfa_regexec_multi(rmp, win, buf, lnum, col, tm)
    regmmatch_T *rmp;
    win_T       *win;           /* window in which to search or NULL */
    buf_T       *buf;           /* buffer in which to search */
    linenr_T    lnum;           /* nr of line to start looking for match */
    colnr_T     col;            /* column to start looking for match */
    proftime_T  *tm;            /* timeout limit or NULL */
{
    reg_match = NULL;
    reg_mmatch = rmp;
    reg_buf = buf;
    reg_win = win;
    reg_firstlnum = lnum;
    reg_maxline = reg_buf->b_ml.ml_line_count - lnum;
    reg_line_lbr = FALSE;
    ireg_ic = rmp->rmm_ic;
    ireg_icombine = FALSE;
    ireg_maxcol = rmp->rmm_maxcol;

    return nfa_regexec_both(NULL, col, tm);
}

/* ----------------------------------------------------------------------- */

static regengine_T nfa_regengine =
{
    nfa_regcomp,
    nfa_regfree,
    nfa_regexec_nl,
    nfa_regexec_multi,
    (char_u *)""
};

/* Which regexp engine to use? Needed for vim_regcomp().
 * Must match with 'regexpengine'. */
static int regexp_engine = 0;

/*
 * Compile a regular expression into internal code.
 * Returns the program in allocated memory.
 * Use vim_regfree() to free the memory.
 * Returns NULL for an error.
 */
    regprog_T *
vim_regcomp(expr_arg, re_flags)
    char_u      *expr_arg;
    int         re_flags;
{
    regprog_T   *prog = NULL;
    char_u      *expr = expr_arg;

    regexp_engine = p_re;

    /* Check for prefix "\%#=", that sets the regexp engine */
    if (STRNCMP(expr, "\\%#=", 4) == 0)
    {
        int newengine = expr[4] - '0';

        if (newengine == AUTOMATIC_ENGINE
            || newengine == BACKTRACKING_ENGINE
            || newengine == NFA_ENGINE)
        {
            regexp_engine = expr[4] - '0';
            expr += 5;
        }
        else
        {
            EMSG("E864: \\%#= can only be followed by 0, 1, or 2. The automatic engine will be used ");
            regexp_engine = AUTOMATIC_ENGINE;
        }
    }
    bt_regengine.expr = expr;
    nfa_regengine.expr = expr;

    /*
     * First try the NFA engine, unless backtracking was requested.
     */
    if (regexp_engine != BACKTRACKING_ENGINE)
        prog = nfa_regengine.regcomp(expr,
                re_flags + (regexp_engine == AUTOMATIC_ENGINE ? RE_AUTO : 0));
    else
        prog = bt_regengine.regcomp(expr, re_flags);

    /* Check for error compiling regexp with initial engine. */
    if (prog == NULL)
    {
        /*
         * If the NFA engine failed, try the backtracking engine.
         * The NFA engine also fails for patterns that it can't handle well
         * but are still valid patterns, thus a retry should work.
         */
        if (regexp_engine == AUTOMATIC_ENGINE)
        {
            regexp_engine = BACKTRACKING_ENGINE;
            prog = bt_regengine.regcomp(expr, re_flags);
        }
    }

    if (prog != NULL)
    {
        /* Store the info needed to call regcomp() again when the engine turns
         * out to be very slow when executing it. */
        prog->re_engine = regexp_engine;
        prog->re_flags  = re_flags;
    }

    return prog;
}

/*
 * Free a compiled regexp program, returned by vim_regcomp().
 */
    void
vim_regfree(prog)
    regprog_T   *prog;
{
    if (prog != NULL)
        prog->engine->regfree(prog);
}

static void report_re_switch(char_u *pat);

    static void
report_re_switch(pat)
    char_u *pat;
{
    if (p_verbose > 0)
    {
        verbose_enter();
        MSG_PUTS("Switching to backtracking RE engine for pattern: ");
        MSG_PUTS(pat);
        verbose_leave();
    }
}

static int vim_regexec_both(regmatch_T *rmp, char_u *line, colnr_T col, int nl);

/*
 * Match a regexp against a string.
 * "rmp->regprog" is a compiled regexp as returned by vim_regcomp().
 * Note: "rmp->regprog" may be freed and changed.
 * Uses curbuf for line count and 'iskeyword'.
 * When "nl" is TRUE consider a "\n" in "line" to be a line break.
 *
 * Return TRUE if there is a match, FALSE if not.
 */
    static int
vim_regexec_both(rmp, line, col, nl)
    regmatch_T  *rmp;
    char_u      *line;  /* string to match against */
    colnr_T     col;    /* column to start looking for match */
    int         nl;
{
    int result = rmp->regprog->engine->regexec_nl(rmp, line, col, nl);

    /* NFA engine aborted because it's very slow. */
    if (rmp->regprog->re_engine == AUTOMATIC_ENGINE && result == NFA_TOO_EXPENSIVE)
    {
        int    save_p_re = p_re;
        int    re_flags = rmp->regprog->re_flags;
        char_u *pat = vim_strsave(((nfa_regprog_T *)rmp->regprog)->pattern);

        p_re = BACKTRACKING_ENGINE;
        vim_regfree(rmp->regprog);
        if (pat != NULL)
        {
            report_re_switch(pat);
            rmp->regprog = vim_regcomp(pat, re_flags);
            if (rmp->regprog != NULL)
                result = rmp->regprog->engine->regexec_nl(rmp, line, col, nl);
            vim_free(pat);
        }

        p_re = save_p_re;
    }
    return result > 0;
}

/*
 * Note: "*prog" may be freed and changed.
 * Return TRUE if there is a match, FALSE if not.
 */
    int
vim_regexec_prog(prog, ignore_case, line, col)
    regprog_T   **prog;
    int         ignore_case;
    char_u      *line;
    colnr_T     col;
{
    int r;
    regmatch_T regmatch;

    regmatch.regprog = *prog;
    regmatch.rm_ic = ignore_case;
    r = vim_regexec_both(&regmatch, line, col, FALSE);
    *prog = regmatch.regprog;
    return r;
}

/*
 * Note: "rmp->regprog" may be freed and changed.
 * Return TRUE if there is a match, FALSE if not.
 */
    int
vim_regexec(rmp, line, col)
    regmatch_T  *rmp;
    char_u      *line;
    colnr_T     col;
{
    return vim_regexec_both(rmp, line, col, FALSE);
}

/*
 * Like vim_regexec(), but consider a "\n" in "line" to be a line break.
 * Note: "rmp->regprog" may be freed and changed.
 * Return TRUE if there is a match, FALSE if not.
 */
    int
vim_regexec_nl(rmp, line, col)
    regmatch_T  *rmp;
    char_u      *line;
    colnr_T     col;
{
    return vim_regexec_both(rmp, line, col, TRUE);
}

/*
 * Match a regexp against multiple lines.
 * "rmp->regprog" is a compiled regexp as returned by vim_regcomp().
 * Note: "rmp->regprog" may be freed and changed.
 * Uses curbuf for line count and 'iskeyword'.
 *
 * Return zero if there is no match.  Return number of lines contained in the match otherwise.
 */
    long
vim_regexec_multi(rmp, win, buf, lnum, col, tm)
    regmmatch_T *rmp;
    win_T       *win;           /* window in which to search or NULL */
    buf_T       *buf;           /* buffer in which to search */
    linenr_T    lnum;           /* nr of line to start looking for match */
    colnr_T     col;            /* column to start looking for match */
    proftime_T  *tm;            /* timeout limit or NULL */
{
    int result = rmp->regprog->engine->regexec_multi(rmp, win, buf, lnum, col, tm);

    /* NFA engine aborted because it's very slow. */
    if (rmp->regprog->re_engine == AUTOMATIC_ENGINE && result == NFA_TOO_EXPENSIVE)
    {
        int    save_p_re = p_re;
        int    re_flags = rmp->regprog->re_flags;
        char_u *pat = vim_strsave(((nfa_regprog_T *)rmp->regprog)->pattern);

        p_re = BACKTRACKING_ENGINE;
        vim_regfree(rmp->regprog);
        if (pat != NULL)
        {
            report_re_switch(pat);
            rmp->regprog = vim_regcomp(pat, re_flags);
            if (rmp->regprog != NULL)
                result = rmp->regprog->engine->regexec_multi(rmp, win, buf, lnum, col, tm);
            vim_free(pat);
        }
        p_re = save_p_re;
    }

    return (result <= 0) ? 0 : result;
}
