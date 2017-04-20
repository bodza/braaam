package vim;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;

import jnr.ffi.LibraryLoader;
import jnr.ffi.mapper.DataConverter;
import jnr.ffi.mapper.FromNativeContext;
import jnr.ffi.mapper.ToNativeContext;
import jnr.ffi.Memory;
import jnr.ffi.NativeType;
import jnr.ffi.Pointer;
import jnr.ffi.StructLayout;
import jnr.ffi.Variable;
import jnr.ffi.types.*;

import static vim.VimA.*;
import static vim.VimB.*;
import static vim.VimC.*;
import static vim.VimD.*;
import static vim.VimE.*;
import static vim.VimF.*;
import static vim.VimG.*;
import static vim.VimH.*;
import static vim.VimI.*;
import static vim.VimJ.*;
import static vim.VimK.*;
import static vim.VimL.*;
import static vim.VimM.*;
import static vim.VimN.*;
import static vim.VimO.*;
import static vim.VimP.*;
import static vim.VimQ.*;
import static vim.VimR.*;
import static vim.VimS.*;
import static vim.VimT.*;
import static vim.VimU.*;
import static vim.VimV.*;
import static vim.VimW.*;
import static vim.VimX.*;
import static vim.VimY.*;

/*
 * Supported Types
 * ------
 *
 * All java primitives are mapped simply to the equivalent C types.
 *
 * - byte - 8 bit signed integer
 * - short - 16 bit signed integer
 * - int - 32 bit signed integer
 * - long - natural long (i.e. 32 bits wide on 32 bit systems, 64 bit wide on 64 bit systems)
 * - float - 32 bit float
 * - double - 64 bit float
 *
 * The width and/or signed-ness of these basic types can be specified using one of the type alias annotations.
 *  e.g.
 *
 *     // Use the correct width for the result from getpid(3)
 *     @pid_t long getpid();
 *
 *     // read(2) returns a signed long result, and its length parameter is an unsigned long
 *     @ssize_t long read(int fd, Pointer data, @size_t long len);
 *
 *
 * In addition, the following java types are mapped to a C pointer
 *
 * - String - equivalent to "const char *"
 * - Pointer - equivalent to "void *"
 * - Buffer - equivalent to "void *"
 */
public class VimN
{
    /*
     * regexp.c ---------------------------------------------------------------------------------------
     */

    /*
     * Handling of regular expressions: vim_regcomp(), vim_regexec(), vim_regsub()
     *
     * Beware that some of this code is subtly aware of the way operator
     * precedence is structured in regular expressions.  Serious changes in
     * regular-expression syntax might require a total rethink.
     */

    /*
     * The "internal use only" fields in regexp.h are present to pass info from
     * compile to execute that permits the execute phase to run lots faster on
     * simple cases.  They are:
     *
     * regstart     char that must begin a match; NUL if none obvious; Can be a
     *              multi-byte character.
     * reganch      is the match anchored (at beginning-of-line only)?
     * regmust      string (pointer into program) that match must include, or null
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
    /*private*/ static final int END             = 0;       /*      End of program or NOMATCH operand. */
    /*private*/ static final int BOL             = 1;       /*      Match "" at beginning of line. */
    /*private*/ static final int EOL             = 2;       /*      Match "" at end of line. */
    /*private*/ static final int BRANCH          = 3;       /* node Match this alternative, or the next... */
    /*private*/ static final int BACK            = 4;       /*      Match "", "next" ptr points backward. */
    /*private*/ static final int EXACTLY         = 5;       /* str  Match this string. */
    /*private*/ static final int NOTHING         = 6;       /*      Match empty string. */
    /*private*/ static final int STAR            = 7;       /* node Match this (simple) thing 0 or more times. */
    /*private*/ static final int PLUS            = 8;       /* node Match this (simple) thing 1 or more times. */
    /*private*/ static final int MATCH           = 9;       /* node match the operand zero-width */
    /*private*/ static final int NOMATCH         = 10;      /* node check for no match with operand */
    /*private*/ static final int BEHIND          = 11;      /* node look behind for a match with operand */
    /*private*/ static final int NOBEHIND        = 12;      /* node look behind for no match with operand */
    /*private*/ static final int SUBPAT          = 13;      /* node match the operand here */
    /*private*/ static final int BRACE_SIMPLE    = 14;      /* node Match this (simple) thing between m and
                                     *      n times (\{m,n\}). */
    /*private*/ static final int BOW             = 15;      /*      Match "" after [^a-zA-Z0-9_] */
    /*private*/ static final int EOW             = 16;      /*      Match "" at    [^a-zA-Z0-9_] */
    /*private*/ static final int BRACE_LIMITS    = 17;      /* nr nr  define the min & max for BRACE_SIMPLE
                                     *      and BRACE_COMPLEX. */
    /*private*/ static final int NEWL            = 18;      /*      Match line-break */
    /*private*/ static final int BHPOS           = 19;      /*      End position for BEHIND or NOBEHIND */

    /* character classes: 20-48 normal, 50-78 include a line-break */
    /*private*/ static final int ADD_NL          = 30;

    /*private*/ static final int ANY             = 20;      /*      Match any one character. */
    /*private*/ static final int ANYOF           = 21;      /* str  Match any character in this string. */
    /*private*/ static final int ANYBUT          = 22;      /* str  Match any character not in this string. */
    /*private*/ static final int IDENT           = 23;      /*      Match identifier char */
    /*private*/ static final int SIDENT          = 24;      /*      Match identifier char but no digit */
    /*private*/ static final int KWORD           = 25;      /*      Match keyword char */
    /*private*/ static final int SKWORD          = 26;      /*      Match word char but no digit */
    /*private*/ static final int FNAME           = 27;      /*      Match file name char */
    /*private*/ static final int SFNAME          = 28;      /*      Match file name char but no digit */
    /*private*/ static final int PRINT           = 29;      /*      Match printable char */
    /*private*/ static final int SPRINT          = 30;      /*      Match printable char but no digit */
    /*private*/ static final int WHITE           = 31;      /*      Match whitespace char */
    /*private*/ static final int NWHITE          = 32;      /*      Match non-whitespace char */
    /*private*/ static final int DIGIT           = 33;      /*      Match digit char */
    /*private*/ static final int NDIGIT          = 34;      /*      Match non-digit char */
    /*private*/ static final int HEX             = 35;      /*      Match hex char */
    /*private*/ static final int NHEX            = 36;      /*      Match non-hex char */
    /*private*/ static final int OCTAL           = 37;      /*      Match octal char */
    /*private*/ static final int NOCTAL          = 38;      /*      Match non-octal char */
    /*private*/ static final int WORD            = 39;      /*      Match word char */
    /*private*/ static final int NWORD           = 40;      /*      Match non-word char */
    /*private*/ static final int HEAD            = 41;      /*      Match head char */
    /*private*/ static final int NHEAD           = 42;      /*      Match non-head char */
    /*private*/ static final int ALPHA           = 43;      /*      Match alpha char */
    /*private*/ static final int NALPHA          = 44;      /*      Match non-alpha char */
    /*private*/ static final int LOWER           = 45;      /*      Match lowercase char */
    /*private*/ static final int NLOWER          = 46;      /*      Match non-lowercase char */
    /*private*/ static final int UPPER           = 47;      /*      Match uppercase char */
    /*private*/ static final int NUPPER          = 48;      /*      Match non-uppercase char */

    /*private*/ static final int FIRST_NL        = ANY + ADD_NL;
    /*private*/ static final int LAST_NL         = NUPPER + ADD_NL;

    /*private*/ static boolean with_nl(int op)
    {
        return (FIRST_NL <= op && op <= LAST_NL);
    }

    /*private*/ static final int MOPEN           = 80;  /* -89       Mark this point in input as start of
                                     *       \( subexpr.  MOPEN + 0 marks start of match. */
    /*private*/ static final int MCLOSE          = 90;  /* -99       Analogous to MOPEN.  MCLOSE + 0 marks
                                     *       end of match. */
    /*private*/ static final int BACKREF         = 100; /* -109 node Match same string again \1-\9 */

    /*private*/ static final int ZOPEN           = 110; /* -119      Mark this point in input as start of \z( subexpr. */
    /*private*/ static final int ZCLOSE          = 120; /* -129      Analogous to ZOPEN. */
    /*private*/ static final int ZREF            = 130; /* -139 node Match external submatch \z1-\z9 */

    /*private*/ static final int BRACE_COMPLEX   = 140; /* -149 node Match nodes between m & n times */

    /*private*/ static final int NOPEN           = 150;     /*      Mark this point in input as start of \%( subexpr. */
    /*private*/ static final int NCLOSE          = 151;     /*      Analogous to NOPEN. */

    /*private*/ static final int MULTIBYTECODE   = 200;     /* mbc  Match one multi-byte character */
    /*private*/ static final int RE_BOF          = 201;     /*      Match "" at beginning of file. */
    /*private*/ static final int RE_EOF          = 202;     /*      Match "" at end of file. */
    /*private*/ static final int CURSOR          = 203;     /*      Match location of cursor. */

    /*private*/ static final int RE_LNUM         = 204;     /* nr cmp  Match line number */
    /*private*/ static final int RE_COL          = 205;     /* nr cmp  Match column number */
    /*private*/ static final int RE_VCOL         = 206;     /* nr cmp  Match virtual column number */

    /*private*/ static final int RE_MARK         = 207;     /* mark cmp  Match mark position */
    /*private*/ static final int RE_VISUAL       = 208;     /*      Match Visual area */
    /*private*/ static final int RE_COMPOSING    = 209;     /* any composing characters */

    /*
     * Magic characters have a special meaning, they don't match literally.
     * Magic characters are negative.  This separates them from literal characters
     * (possibly multi-byte).  Only ASCII characters can be Magic.
     */
    /*private*/ static final int Magic(int x)
    {
        return x - 256;
    }

    /*private*/ static int un_Magic(int x)
    {
        return x + 256;
    }

    /*private*/ static boolean is_Magic(int x)
    {
        return (x < 0);
    }

    /*private*/ static int no_Magic(int x)
    {
        if (is_Magic(x))
            return un_Magic(x);

        return x;
    }

    /*private*/ static int toggle_Magic(int x)
    {
        if (is_Magic(x))
            return un_Magic(x);

        return Magic(x);
    }

    /*
     * The first byte of the regexp internal "program" is actually this magic number;
     * the start node begins in the second byte.
     * It's used to catch the most severe mutilation of the program by the caller.
     */

    /*private*/ static final byte REGMAGIC = (byte)0234;

    /*
     * Opcode notes:
     *
     * BRANCH           The set of branches constituting a single choice are hooked
     *                  together with their "next" pointers, since precedence prevents
     *                  anything being concatenated to any individual branch.  The
     *                  "next" pointer of the last BRANCH in a choice points to the
     *                  thing following the whole choice.  This is also where the
     *                  final "next" pointer of each individual branch points; each
     *                  branch starts with the operand node of a BRANCH node.
     *
     * BACK             Normal "next" pointers all implicitly point forward; BACK
     *                  exists to make loop structures possible.
     *
     * STAR,PLUS        '=', and complex '*' and '+', are implemented as circular
     *                  BRANCH structures using BACK.  Simple cases (one character
     *                  per match) are implemented with STAR and PLUS for speed
     *                  and to minimize recursive plunges.
     *
     * BRACE_LIMITS     This is always followed by a BRACE_SIMPLE or BRACE_COMPLEX
     *                  node, and defines the min and max limits to be used for that node.
     *
     * MOPEN, MCLOSE    ... are numbered at compile time.
     * ZOPEN, ZCLOSE    ... ditto
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
    /*private*/ static int re_op(Bytes p)
    {
        return (int)p.at(0);
    }

    /*private*/ static int re_next(Bytes p)
    {
        return ((int)p.at(1) << 8) + (int)p.at(2);
    }

    /*private*/ static Bytes operand(Bytes p)
    {
        return p.plus(3);
    }

    /* Obtain an operand that was stored as four bytes, MSB first. */
    /*private*/ static long operand_min(Bytes p)
    {
        return ((long)p.at(3) << 24) + ((long)p.at(4) << 16) + ((long)p.at(5) << 8) + (long)p.at(6);
    }

    /* Obtain a second operand stored as four bytes. */
    /*private*/ static long operand_max(Bytes p)
    {
        return operand_min(p.plus(4));
    }

    /* Obtain a second single-byte operand stored after a four bytes operand. */
    /*private*/ static byte operand_cmp(Bytes p)
    {
        return p.at(7);
    }

    /*private*/ static final long MAX_LIMIT       = (32767L << 16L);

    /*private*/ static Bytes e_missingbracket  = u8("E769: Missing ] after %s[");
    /*private*/ static Bytes e_unmatchedpp     = u8("E53: Unmatched %s%%(");
    /*private*/ static Bytes e_unmatchedp      = u8("E54: Unmatched %s(");
    /*private*/ static Bytes e_unmatchedpar    = u8("E55: Unmatched %s)");
    /*private*/ static Bytes e_z_not_allowed   = u8("E66: \\z( not allowed here");
    /*private*/ static Bytes e_z1_not_allowed  = u8("E67: \\z1 et al. not allowed here");
    /*private*/ static Bytes e_missing_sb      = u8("E69: Missing ] after %s%%[");
    /*private*/ static Bytes e_empty_sb        = u8("E70: Empty %s%%[]");

    /*private*/ static final int NOT_MULTI       = 0;
    /*private*/ static final int MULTI_ONE       = 1;
    /*private*/ static final int MULTI_MULT      = 2;
    /*
     * Return NOT_MULTI if c is not a "multi" operator.
     * Return MULTI_ONE if c is a single "multi" operator.
     * Return MULTI_MULT if c is a multi "multi" operator.
     */
    /*private*/ static int re_multi_type(int c)
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
    /*private*/ static final int HASWIDTH        = 0x1;     /* Known never to match null string. */
    /*private*/ static final int SIMPLE          = 0x2;     /* Simple enough to be STAR/PLUS operand. */
    /*private*/ static final int SPSTART         = 0x4;     /* Starts with * or +. */
    /*private*/ static final int HASNL           = 0x8;     /* Contains some \n. */
    /*private*/ static final int HASLOOKBH       = 0x10;    /* Contains "\@<=" or "\@<!". */
    /*private*/ static final int WORST           = 0;       /* Worst case. */

    /*
     * When regcode is set to this value, code is not emitted and size is computed instead.
     */
    /*private*/ static final Bytes JUST_CALC_SIZE = u8("");

    /*private*/ static Bytes reg_prev_sub;

    /*
     * REGEXP_INRANGE contains all characters which are always special in a [] range after '\'.
     * REGEXP_ABBR contains all characters which act as abbreviations after '\'.
     * These are:
     *  \n  - New line (NL).
     *  \r  - Carriage Return (CR).
     *  \t  - Tab (TAB).
     *  \e  - Escape (ESC).
     *  \b  - Backspace (Ctrl_H).
     *  \d  - Character code in decimal, e.g. \d123
     *  \o  - Character code in octal, e.g. \o80
     *  \x  - Character code in hex, e.g. \x4a
     *  \\u (sic!) - Multibyte character code, e.g. \u20ac
     *  \U  - Long multibyte character code, e.g. \U12345678
     */
    /*private*/ static Bytes REGEXP_INRANGE = u8("]^-n\\");
    /*private*/ static Bytes REGEXP_ABBR = u8("nrtebdoxuU");

    /*
     * Translate '\x' to its control character, except "\n", which is Magic.
     */
    /*private*/ static int backslash_trans(int c)
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

    /*private*/ static final int CLASS_ALNUM = 0;
    /*private*/ static final int CLASS_ALPHA = 1;
    /*private*/ static final int CLASS_BLANK = 2;
    /*private*/ static final int CLASS_CNTRL = 3;
    /*private*/ static final int CLASS_DIGIT = 4;
    /*private*/ static final int CLASS_GRAPH = 5;
    /*private*/ static final int CLASS_LOWER = 6;
    /*private*/ static final int CLASS_PRINT = 7;
    /*private*/ static final int CLASS_PUNCT = 8;
    /*private*/ static final int CLASS_SPACE = 9;
    /*private*/ static final int CLASS_UPPER = 10;
    /*private*/ static final int CLASS_XDIGIT = 11;
    /*private*/ static final int CLASS_TAB = 12;
    /*private*/ static final int CLASS_RETURN = 13;
    /*private*/ static final int CLASS_BACKSPACE = 14;
    /*private*/ static final int CLASS_ESCAPE = 15;
    /*private*/ static final int CLASS_NONE = 99;

    /*private*/ static Bytes[] class_names =
    {
        u8("alnum:]"),
        u8("alpha:]"),
        u8("blank:]"),
        u8("cntrl:]"),
        u8("digit:]"),
        u8("graph:]"),
        u8("lower:]"),
        u8("print:]"),
        u8("punct:]"),
        u8("space:]"),
        u8("upper:]"),
        u8("xdigit:]"),
        u8("tab:]"),
        u8("return:]"),
        u8("backspace:]"),
        u8("escape:]"),
    };

    /*
     * Check for a character class name "[:name:]".  "pp" points to the '['.
     * Returns one of the CLASS_ items.  CLASS_NONE means that no item was
     * recognized.  Otherwise "pp" is advanced to after the item.
     */
    /*private*/ static int get_char_class(Bytes[] pp)
    {
        if (pp[0].at(1) == (byte)':')
        {
            for (int i = 0; i < class_names.length; i++)
            {
                int len = strlen(class_names[i]);
                if (STRNCMP(pp[0].plus(2), class_names[i], len) == 0)
                {
                    pp[0] = pp[0].plus(len + 2);
                    return i;
                }
            }
        }
        return CLASS_NONE;
    }

    /*
     * Specific version of character class functions.
     * Using a table to keep this fast.
     */
    /*private*/ static short[] class_tab = new short[256];

    /*private*/ static final int RI_DIGIT    = 0x01;
    /*private*/ static final int RI_HEX      = 0x02;
    /*private*/ static final int RI_OCTAL    = 0x04;
    /*private*/ static final int RI_WORD     = 0x08;
    /*private*/ static final int RI_HEAD     = 0x10;
    /*private*/ static final int RI_ALPHA    = 0x20;
    /*private*/ static final int RI_LOWER    = 0x40;
    /*private*/ static final int RI_UPPER    = 0x80;
    /*private*/ static final int RI_WHITE    = 0x100;

    /*private*/ static boolean class_tab_done;

    /*private*/ static void init_class_tab()
    {
        if (class_tab_done)
            return;

        for (int i = 0; i < 256; i++)
        {
            if ('0' <= i && i <= '7')
                class_tab[i] = RI_DIGIT + RI_HEX + RI_OCTAL + RI_WORD;
            else if ('8' <= i && i <= '9')
                class_tab[i] = RI_DIGIT + RI_HEX + RI_WORD;
            else if ('a' <= i && i <= 'f')
                class_tab[i] = RI_HEX + RI_WORD + RI_HEAD + RI_ALPHA + RI_LOWER;
            else if ('g' <= i && i <= 'z')
                class_tab[i] = RI_WORD + RI_HEAD + RI_ALPHA + RI_LOWER;
            else if ('A' <= i && i <= 'F')
                class_tab[i] = RI_HEX + RI_WORD + RI_HEAD + RI_ALPHA + RI_UPPER;
            else if ('G' <= i && i <= 'Z')
                class_tab[i] = RI_WORD + RI_HEAD + RI_ALPHA + RI_UPPER;
            else if (i == '_')
                class_tab[i] = RI_WORD + RI_HEAD;
            else
                class_tab[i] = 0;
        }
        class_tab[' '] |= RI_WHITE;
        class_tab['\t'] |= RI_WHITE;

        class_tab_done = true;
    }

    /*private*/ static boolean ri_digit(int c)
    {
        return (c < 0x100 && (class_tab[c] & RI_DIGIT) != 0);
    }

    /*private*/ static boolean ri_hex(int c)
    {
        return (c < 0x100 && (class_tab[c] & RI_HEX) != 0);
    }

    /*private*/ static boolean ri_octal(int c)
    {
        return (c < 0x100 && (class_tab[c] & RI_OCTAL) != 0);
    }

    /*private*/ static boolean ri_word(int c)
    {
        return (c < 0x100 && (class_tab[c] & RI_WORD) != 0);
    }

    /*private*/ static boolean ri_head(int c)
    {
        return (c < 0x100 && (class_tab[c] & RI_HEAD) != 0);
    }

    /*private*/ static boolean ri_alpha(int c)
    {
        return (c < 0x100 && (class_tab[c] & RI_ALPHA) != 0);
    }

    /*private*/ static boolean ri_lower(int c)
    {
        return (c < 0x100 && (class_tab[c] & RI_LOWER) != 0);
    }

    /*private*/ static boolean ri_upper(int c)
    {
        return (c < 0x100 && (class_tab[c] & RI_UPPER) != 0);
    }

    /*private*/ static boolean ri_white(int c)
    {
        return (c < 0x100 && (class_tab[c] & RI_WHITE) != 0);
    }

    /* flags for regflags */
    /*private*/ static final int RF_ICASE    = 1;   /* ignore case */
    /*private*/ static final int RF_NOICASE  = 2;   /* don't ignore case */
    /*private*/ static final int RF_HASNL    = 4;   /* can match a NL */
    /*private*/ static final int RF_ICOMBINE = 8;   /* ignore combining characters */
    /*private*/ static final int RF_LOOKBH   = 16;  /* uses "\@<=" or "\@<!" */

    /*
     * Global work variables for vim_regcomp().
     */

    /*private*/ static Bytes    regparse;                   /* input-scan pointer */
    /*private*/ static int      prevchr_len;                /* byte length of previous char */
    /*private*/ static int      num_complex_braces;         /* complex \{...} count */
    /*private*/ static int      regnpar;                    /* () count */
    /*private*/ static int      regnzpar;                   /* \z() count */
    /*private*/ static int      re_has_z;                   /* \z item detected */
    /*private*/ static Bytes    regcode;                    /* code-emit pointer, or JUST_CALC_SIZE */
    /*private*/ static int      regsize;                    /* code size */
    /*private*/ static boolean  reg_toolong;                /* true when offset out of range */
    /*private*/ static boolean[] had_endbrace = new boolean[NSUBEXP]; /* flags, true if end of () found */
    /*private*/ static int      regflags;                   /* RF_ flags for prog */
    /*private*/ static long[]   brace_min = new long[10];   /* minimums for complex brace repeats */
    /*private*/ static long[]   brace_max = new long[10];   /* maximums for complex brace repeats */
    /*private*/ static int[]    brace_count = new int[10];  /* current counts for complex brace repeats */
    /*private*/ static boolean  had_eol;                    /* true when EOL found by vim_regcomp() */
    /*private*/ static boolean  one_exactly;                /* only do one char for EXACTLY */

    /*private*/ static int      reg_magic;                  /* magicness of the pattern: */
    /*private*/ static final int MAGIC_NONE      = 1;                           /* "\V" very unmagic */
    /*private*/ static final int MAGIC_OFF       = 2;                           /* "\M" or 'magic' off */
    /*private*/ static final int MAGIC_ON        = 3;                           /* "\m" or 'magic' */
    /*private*/ static final int MAGIC_ALL       = 4;                           /* "\v" very magic */

    /*private*/ static boolean  reg_string;                 /* matching with a string instead of a buffer line */
    /*private*/ static boolean  reg_strict;                 /* "[abc" is illegal */

    /*
     * META contains all characters that may be magic, except '^' and '$'.
     */

    /* META[] is used often enough to justify turning it into a table. */
    /*private*/ static final byte[] META_flags =
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

    /*private*/ static int      curchr;             /* currently parsed character */
    /* Previous character.  Note: prevchr is sometimes -1 when we are not at the start,
     * e.g. in /[ ^I]^ the pattern was never found even if it existed,
     * because ^ was taken to be magic. */
    /*private*/ static int      prevchr;
    /*private*/ static int      prevprevchr;        /* previous-previous character */
    /*private*/ static int      nextchr;            /* used for ungetchr() */

    /* arguments for reg() */
    /*private*/ static final int REG_NOPAREN     = 0;           /* toplevel reg() */
    /*private*/ static final int REG_PAREN       = 1;           /* \(\) */
    /*private*/ static final int REG_ZPAREN      = 2;           /* \z(\) */
    /*private*/ static final int REG_NPAREN      = 3;           /* \%(\) */

    /*private*/ static final class parse_state_C
    {
        Bytes       regparse;
        int         prevchr_len;
        int         curchr;
        int         prevchr;
        int         prevprevchr;
        int         nextchr;
        boolean     at_start;
        boolean     prev_at_start;
        int         regnpar;

        /*private*/ parse_state_C()
        {
        }
    }

    /*
     * Forward declarations for vim_regcomp()'s friends.
     */

    /*private*/ static regengine_C bt_regengine = new regengine_C()
    {
        public regprog_C regcomp(Bytes expr, int re_flags)
        {
            return bt_regcomp(expr, re_flags);
        }

        public long regexec_nl(regmatch_C rmp, Bytes line, int col, boolean line_lbr)
        {
            return bt_regexec_nl(rmp, line, col, line_lbr);
        }

        public long regexec_multi(regmmatch_C rmp, window_C win, buffer_C buf, long lnum, int col, timeval_C tm)
        {
            return bt_regexec_multi(rmp, win, buf, lnum, col, tm);
        }
    };

    /*private*/ static regengine_C nfa_regengine = new regengine_C()
    {
        public regprog_C regcomp(Bytes expr, int re_flags)
        {
            return nfa_regcomp(expr, re_flags);
        }

        public long regexec_nl(regmatch_C rmp, Bytes line, int col, boolean line_lbr)
        {
            return nfa_regexec_nl(rmp, line, col, line_lbr);
        }

        public long regexec_multi(regmmatch_C rmp, window_C win, buffer_C buf, long lnum, int col, timeval_C tm)
        {
            return nfa_regexec_multi(rmp, win, buf, lnum, col, tm);
        }
    };

    /*
     * Return true if compiled regular expression "prog" can match a line break.
     */
    /*private*/ static boolean re_multiline(regprog_C prog)
    {
        return ((prog.regflags & RF_HASNL) != 0);
    }

    /*
     * Return true if compiled regular expression "prog" looks before the start
     * position (pattern contains "\@<=" or "\@<!").
     */
    /*private*/ static boolean re_lookbehind(regprog_C prog)
    {
        return ((prog.regflags & RF_LOOKBH) != 0);
    }

    /*
     * Check for an equivalence class name "[=a=]".  "pp" points to the '['.
     * Returns a character representing the class.  Zero means that no item was
     * recognized.  Otherwise "pp" is advanced to after the item.
     */
    /*private*/ static int get_equi_class(Bytes[] pp)
    {
        Bytes p = pp[0];
        if (p.at(1) == (byte)'=')
        {
            int len = us_ptr2len_cc(p.plus(2));
            if (p.at(len + 2) == (byte)'=' && p.at(len + 3) == (byte)']')
            {
                int c = us_ptr2char(p.plus(2));
                pp[0] = pp[0].plus(len + 4);
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
    /*private*/ static void reg_equi_class(int c)
    {
        switch (c)
        {
            case 'A':
            case 0xc0: case 0xc1: case 0xc2:
            case 0xc3: case 0xc4: case 0xc5:
            case 0x100: case 0x102: case 0x104:
            case 0x1cd: case 0x1de: case 0x1e0:
            case 0x1ea2:
            {
                regmbc('A');
                regmbc(0xc0); regmbc(0xc1); regmbc(0xc2);
                regmbc(0xc3); regmbc(0xc4); regmbc(0xc5);
                regmbc(0x100); regmbc(0x102); regmbc(0x104);
                regmbc(0x1cd); regmbc(0x1de); regmbc(0x1e0);
                regmbc(0x1ea2);
                return;
            }
            case 'a':
            case 0xe0: case 0xe1: case 0xe2:
            case 0xe3: case 0xe4: case 0xe5:
            case 0x101: case 0x103: case 0x105:
            case 0x1ce: case 0x1df: case 0x1e1:
            case 0x1ea3:
            {
                regmbc('a');
                regmbc(0xe0); regmbc(0xe1); regmbc(0xe2);
                regmbc(0xe3); regmbc(0xe4); regmbc(0xe5);
                regmbc(0x101); regmbc(0x103); regmbc(0x105);
                regmbc(0x1ce); regmbc(0x1df); regmbc(0x1e1);
                regmbc(0x1ea3);
                return;
            }

            case 'B':
            case 0x1e02: case 0x1e06:
            {
                regmbc('B');
                regmbc(0x1e02); regmbc(0x1e06);
                return;
            }
            case 'b':
            case 0x1e03: case 0x1e07:
            {
                regmbc('b');
                regmbc(0x1e03); regmbc(0x1e07);
                return;
            }

            case 'C':
            case 0xc7:
            case 0x106: case 0x108: case 0x10a: case 0x10c:
            {
                regmbc('C');
                regmbc(0xc7);
                regmbc(0x106); regmbc(0x108); regmbc(0x10a); regmbc(0x10c);
                return;
            }
            case 'c':
            case 0xe7:
            case 0x107: case 0x109: case 0x10b: case 0x10d:
            {
                regmbc('c');
                regmbc(0xe7);
                regmbc(0x107); regmbc(0x109); regmbc(0x10b); regmbc(0x10d);
                return;
            }

            case 'D':
            case 0x10e: case 0x110:
            case 0x1e0a: case 0x1e0c: case 0x1e0e: case 0x1e10: case 0x1e12:
            {
                regmbc('D');
                regmbc(0x10e); regmbc(0x110);
                regmbc(0x1e0a); regmbc(0x1e0c); regmbc(0x1e0e); regmbc(0x1e10); regmbc(0x1e12);
                return;
            }
            case 'd':
            case 0x10f: case 0x111:
            case 0x1e0b: case 0x1e0d: case 0x1e0f: case 0x1e11: case 0x1e13:
            {
                regmbc('d');
                regmbc(0x10f); regmbc(0x111);
                regmbc(0x1e0b); regmbc(0x1e0d); regmbc(0x1e0f); regmbc(0x1e11); regmbc(0x1e13);
                return;
            }

            case 'E':
            case 0xc8: case 0xc9: case 0xca: case 0xcb:
            case 0x112: case 0x114: case 0x116: case 0x118: case 0x11a:
            case 0x1eba: case 0x1ebc:
            {
                regmbc('E');
                regmbc(0xc8); regmbc(0xc9); regmbc(0xca); regmbc(0xcb);
                regmbc(0x112); regmbc(0x114); regmbc(0x116); regmbc(0x118); regmbc(0x11a);
                regmbc(0x1eba); regmbc(0x1ebc);
                return;
            }
            case 'e':
            case 0xe8: case 0xe9: case 0xea: case 0xeb:
            case 0x113: case 0x115: case 0x117: case 0x119: case 0x11b:
            case 0x1ebb: case 0x1ebd:
            {
                regmbc('e');
                regmbc(0xe8); regmbc(0xe9); regmbc(0xea); regmbc(0xeb);
                regmbc(0x113); regmbc(0x115); regmbc(0x117); regmbc(0x119); regmbc(0x11b);
                regmbc(0x1ebb); regmbc(0x1ebd);
                return;
            }

            case 'F':
            case 0x1e1e:
            {
                regmbc('F');
                regmbc(0x1e1e);
                return;
            }
            case 'f':
            case 0x1e1f:
            {
                regmbc('f');
                regmbc(0x1e1f);
                return;
            }

            case 'G':
            case 0x11c: case 0x11e: case 0x120: case 0x122:
            case 0x1e4: case 0x1e6: case 0x1f4:
            case 0x1e20:
            {
                regmbc('G');
                regmbc(0x11c); regmbc(0x11e); regmbc(0x120); regmbc(0x122);
                regmbc(0x1e4); regmbc(0x1e6); regmbc(0x1f4);
                regmbc(0x1e20);
                return;
            }
            case 'g':
            case 0x11d: case 0x11f: case 0x121: case 0x123:
            case 0x1e5: case 0x1e7: case 0x1f5:
            case 0x1e21:
            {
                regmbc('g');
                regmbc(0x11d); regmbc(0x11f); regmbc(0x121); regmbc(0x123);
                regmbc(0x1e5); regmbc(0x1e7); regmbc(0x1f5);
                regmbc(0x1e21);
                return;
            }

            case 'H':
            case 0x124: case 0x126:
            case 0x1e22: case 0x1e26: case 0x1e28:
            {
                regmbc('H');
                regmbc(0x124); regmbc(0x126);
                regmbc(0x1e22); regmbc(0x1e26); regmbc(0x1e28);
                return;
            }
            case 'h':
            case 0x125: case 0x127:
            case 0x1e23: case 0x1e27: case 0x1e29: case 0x1e96:
            {
                regmbc('h');
                regmbc(0x125); regmbc(0x127);
                regmbc(0x1e23); regmbc(0x1e27); regmbc(0x1e29); regmbc(0x1e96);
                return;
            }

            case 'I':
            case 0xcc: case 0xcd: case 0xce: case 0xcf:
            case 0x128: case 0x12a: case 0x12c: case 0x12e: case 0x130:
            case 0x1cf:
            case 0x1ec8:
            {
                regmbc('I');
                regmbc(0xcc); regmbc(0xcd); regmbc(0xce); regmbc(0xcf);
                regmbc(0x128); regmbc(0x12a); regmbc(0x12c); regmbc(0x12e); regmbc(0x130);
                regmbc(0x1cf);
                regmbc(0x1ec8);
                return;
            }
            case 'i':
            case 0xec: case 0xed: case 0xee: case 0xef:
            case 0x129: case 0x12b: case 0x12d: case 0x12f: case 0x131:
            case 0x1d0:
            case 0x1ec9:
            {
                regmbc('i');
                regmbc(0xec); regmbc(0xed); regmbc(0xee); regmbc(0xef);
                regmbc(0x129); regmbc(0x12b); regmbc(0x12d); regmbc(0x12f); regmbc(0x131);
                regmbc(0x1d0);
                regmbc(0x1ec9);
                return;
            }

            case 'J':
            case 0x134:
            {
                regmbc('J');
                regmbc(0x134);
                return;
            }
            case 'j':
            case 0x135: case 0x1f0:
            {
                regmbc('j');
                regmbc(0x135); regmbc(0x1f0);
                return;
            }

            case 'K':
            case 0x136: case 0x1e8:
            case 0x1e30: case 0x1e34:
            {
                regmbc('K');
                regmbc(0x136); regmbc(0x1e8);
                regmbc(0x1e30); regmbc(0x1e34);
                return;
            }
            case 'k':
            case 0x137: case 0x1e9:
            case 0x1e31: case 0x1e35:
            {
                regmbc('k');
                regmbc(0x137); regmbc(0x1e9);
                regmbc(0x1e31); regmbc(0x1e35);
                return;
            }

            case 'L':
            case 0x139: case 0x13b: case 0x13d: case 0x13f: case 0x141:
            case 0x1e3a:
            {
                regmbc('L');
                regmbc(0x139); regmbc(0x13b); regmbc(0x13d); regmbc(0x13f); regmbc(0x141);
                regmbc(0x1e3a);
                return;
            }
            case 'l':
            case 0x13a: case 0x13c: case 0x13e: case 0x140: case 0x142:
            case 0x1e3b:
            {
                regmbc('l');
                regmbc(0x13a); regmbc(0x13c); regmbc(0x13e); regmbc(0x140); regmbc(0x142);
                regmbc(0x1e3b);
                return;
            }

            case 'M':
            case 0x1e3e: case 0x1e40:
            {
                regmbc('M');
                regmbc(0x1e3e); regmbc(0x1e40);
                return;
            }
            case 'm':
            case 0x1e3f: case 0x1e41:
            {
                regmbc('m');
                regmbc(0x1e3f); regmbc(0x1e41);
                return;
            }

            case 'N':
            case 0xd1:
            case 0x143: case 0x145: case 0x147:
            case 0x1e44: case 0x1e48:
            {
                regmbc('N');
                regmbc(0xd1);
                regmbc(0x143); regmbc(0x145); regmbc(0x147);
                regmbc(0x1e44); regmbc(0x1e48);
                return;
            }
            case 'n':
            case 0xf1:
            case 0x144: case 0x146: case 0x148: case 0x149:
            case 0x1e45: case 0x1e49:
            {
                regmbc('n');
                regmbc(0xf1);
                regmbc(0x144); regmbc(0x146); regmbc(0x148); regmbc(0x149);
                regmbc(0x1e45); regmbc(0x1e49);
                return;
            }

            case 'O':
            case 0xd2: case 0xd3: case 0xd4:
            case 0xd5: case 0xd6: case 0xd8:
            case 0x14c: case 0x14e: case 0x150:
            case 0x1a0: case 0x1d1: case 0x1ea: case 0x1ec:
            case 0x1ece:
            {
                regmbc('O');
                regmbc(0xd2); regmbc(0xd3); regmbc(0xd4);
                regmbc(0xd5); regmbc(0xd6); regmbc(0xd8);
                regmbc(0x14c); regmbc(0x14e); regmbc(0x150);
                regmbc(0x1a0); regmbc(0x1d1); regmbc(0x1ea); regmbc(0x1ec);
                regmbc(0x1ece);
                return;
            }
            case 'o':
            case 0xf2: case 0xf3: case 0xf4:
            case 0xf5: case 0xf6: case 0xf8:
            case 0x14d: case 0x14f: case 0x151:
            case 0x1a1: case 0x1d2: case 0x1eb: case 0x1ed:
            case 0x1ecf:
            {
                regmbc('o');
                regmbc(0xf2); regmbc(0xf3); regmbc(0xf4);
                regmbc(0xf5); regmbc(0xf6); regmbc(0xf8);
                regmbc(0x14d); regmbc(0x14f); regmbc(0x151);
                regmbc(0x1a1); regmbc(0x1d2); regmbc(0x1eb); regmbc(0x1ed);
                regmbc(0x1ecf);
                return;
            }

            case 'P':
            case 0x1e54: case 0x1e56:
            {
                regmbc('P');
                regmbc(0x1e54); regmbc(0x1e56);
                return;
            }
            case 'p':
            case 0x1e55: case 0x1e57:
            {
                regmbc('p');
                regmbc(0x1e55); regmbc(0x1e57);
                return;
            }

            case 'R':
            case 0x154: case 0x156: case 0x158:
            case 0x1e58: case 0x1e5e:
            {
                regmbc('R');
                regmbc(0x154); regmbc(0x156); regmbc(0x158);
                regmbc(0x1e58); regmbc(0x1e5e);
                return;
            }
            case 'r':
            case 0x155: case 0x157: case 0x159:
            case 0x1e59: case 0x1e5f:
            {
                regmbc('r');
                regmbc(0x155); regmbc(0x157); regmbc(0x159);
                regmbc(0x1e59); regmbc(0x1e5f);
                return;
            }

            case 'S':
            case 0x15a: case 0x15c: case 0x15e: case 0x160:
            case 0x1e60:
            {
                regmbc('S');
                regmbc(0x15a); regmbc(0x15c); regmbc(0x15e); regmbc(0x160);
                regmbc(0x1e60);
                return;
            }
            case 's':
            case 0x15b: case 0x15d: case 0x15f: case 0x161:
            case 0x1e61:
            {
                regmbc('s');
                regmbc(0x15b); regmbc(0x15d); regmbc(0x15f); regmbc(0x161);
                regmbc(0x1e61);
                return;
            }

            case 'T':
            case 0x162: case 0x164: case 0x166:
            case 0x1e6a: case 0x1e6e:
            {
                regmbc('T');
                regmbc(0x162); regmbc(0x164); regmbc(0x166);
                regmbc(0x1e6a); regmbc(0x1e6e);
                return;
            }
            case 't':
            case 0x163: case 0x165: case 0x167:
            case 0x1e6b: case 0x1e6f: case 0x1e97:
            {
                regmbc('t');
                regmbc(0x163); regmbc(0x165); regmbc(0x167);
                regmbc(0x1e6b); regmbc(0x1e6f); regmbc(0x1e97);
                return;
            }

            case 'U':
            case 0xd9: case 0xda: case 0xdb: case 0xdc:
            case 0x168: case 0x16a: case 0x16c: case 0x16e:
            case 0x170: case 0x172: case 0x1af: case 0x1d3:
            case 0x1ee6:
            {
                regmbc('U');
                regmbc(0xd9); regmbc(0xda); regmbc(0xdb); regmbc(0xdc);
                regmbc(0x168); regmbc(0x16a); regmbc(0x16c); regmbc(0x16e);
                regmbc(0x170); regmbc(0x172); regmbc(0x1af); regmbc(0x1d3);
                regmbc(0x1ee6);
                return;
            }
            case 'u':
            case 0xf9: case 0xfa: case 0xfb: case 0xfc:
            case 0x169: case 0x16b: case 0x16d: case 0x16f:
            case 0x171: case 0x173: case 0x1b0: case 0x1d4:
            case 0x1ee7:
            {
                regmbc('u');
                regmbc(0xf9); regmbc(0xfa); regmbc(0xfb); regmbc(0xfc);
                regmbc(0x169); regmbc(0x16b); regmbc(0x16d); regmbc(0x16f);
                regmbc(0x171); regmbc(0x173); regmbc(0x1b0); regmbc(0x1d4);
                regmbc(0x1ee7);
                return;
            }

            case 'V':
            case 0x1e7c:
            {
                regmbc('V');
                regmbc(0x1e7c);
                return;
            }
            case 'v':
            case 0x1e7d:
            {
                regmbc('v');
                regmbc(0x1e7d);
                return;
            }

            case 'W':
            case 0x174:
            case 0x1e80: case 0x1e82: case 0x1e84: case 0x1e86:
            {
                regmbc('W');
                regmbc(0x174);
                regmbc(0x1e80); regmbc(0x1e82); regmbc(0x1e84); regmbc(0x1e86);
                return;
            }
            case 'w':
            case 0x175:
            case 0x1e81: case 0x1e83: case 0x1e85: case 0x1e87: case 0x1e98:
            {
                regmbc('w');
                regmbc(0x175);
                regmbc(0x1e81); regmbc(0x1e83); regmbc(0x1e85); regmbc(0x1e87); regmbc(0x1e98);
                return;
            }

            case 'X':
            case 0x1e8a: case 0x1e8c:
            {
                regmbc('X');
                regmbc(0x1e8a); regmbc(0x1e8c);
                return;
            }
            case 'x':
            case 0x1e8b: case 0x1e8d:
            {
                regmbc('x');
                regmbc(0x1e8b); regmbc(0x1e8d);
                return;
            }

            case 'Y':
            case 0xdd:
            case 0x176: case 0x178:
            case 0x1e8e: case 0x1ef2: case 0x1ef6: case 0x1ef8:
            {
                regmbc('Y');
                regmbc(0xdd);
                regmbc(0x176); regmbc(0x178);
                regmbc(0x1e8e); regmbc(0x1ef2); regmbc(0x1ef6); regmbc(0x1ef8);
                return;
            }
            case 'y':
            case 0xfd: case 0xff:
            case 0x177:
            case 0x1e8f: case 0x1e99: case 0x1ef3: case 0x1ef7: case 0x1ef9:
            {
                regmbc('y');
                regmbc(0xfd); regmbc(0xff);
                regmbc(0x177);
                regmbc(0x1e8f); regmbc(0x1e99); regmbc(0x1ef3); regmbc(0x1ef7); regmbc(0x1ef9);
                return;
            }

            case 'Z':
            case 0x179: case 0x17b: case 0x17d: case 0x1b5:
            case 0x1e90: case 0x1e94:
            {
                regmbc('Z');
                regmbc(0x179); regmbc(0x17b); regmbc(0x17d); regmbc(0x1b5);
                regmbc(0x1e90); regmbc(0x1e94);
                return;
            }
            case 'z':
            case 0x17a: case 0x17c: case 0x17e: case 0x1b6:
            case 0x1e91: case 0x1e95:
            {
                regmbc('z');
                regmbc(0x17a); regmbc(0x17c); regmbc(0x17e); regmbc(0x1b6);
                regmbc(0x1e91); regmbc(0x1e95);
                return;
            }
        }

        regmbc(c);
    }

    /*
     * Check for a collating element "[.a.]".  "pp" points to the '['.
     * Returns a character.  Zero means that no item was recognized.
     * Otherwise "pp" is advanced to after the item.
     * Currently only single characters are recognized!
     */
    /*private*/ static int get_coll_element(Bytes[] pp)
    {
        Bytes p = pp[0];

        if (p.at(1) == (byte)'.')
        {
            int len = us_ptr2len_cc(p.plus(2));
            if (p.at(len + 2) == (byte)'.' && p.at(len + 3) == (byte)']')
            {
                int c = us_ptr2char(p.plus(2));
                pp[0] = pp[0].plus(len + 4);
                return c;
            }
        }

        return 0;
    }

    /*private*/ static boolean reg_cpo_lit;     /* 'cpoptions' contains 'l' flag */
    /*private*/ static boolean reg_cpo_bsl;     /* 'cpoptions' contains '\' flag */

    /*private*/ static void get_cpo_flags()
    {
        reg_cpo_lit = (vim_strbyte(p_cpo[0], CPO_LITERAL) != null);
        reg_cpo_bsl = (vim_strbyte(p_cpo[0], CPO_BACKSL) != null);
    }

    /*
     * Skip over a "[]" range.
     * "p" must point to the character after the '['.
     * The returned pointer is on the matching ']', or the terminating NUL.
     */
    /*private*/ static Bytes skip_anyof(Bytes p)
    {
        if (p.at(0) == (byte)'^')      /* Complement of range. */
            p = p.plus(1);
        if (p.at(0) == (byte)']' || p.at(0) == (byte)'-')
            p = p.plus(1);
        while (p.at(0) != NUL && p.at(0) != (byte)']')
        {
            int l = us_ptr2len_cc(p);
            if (1 < l)
                p = p.plus(l);
            else if (p.at(0) == (byte)'-')
            {
                p = p.plus(1);
                if (p.at(0) != (byte)']' && p.at(0) != NUL)
                    p = p.plus(us_ptr2len_cc(p));
            }
            else if (p.at(0) == (byte)'\\'
                    && !reg_cpo_bsl
                    && (vim_strchr(REGEXP_INRANGE, p.at(1)) != null
                        || (!reg_cpo_lit && vim_strchr(REGEXP_ABBR, p.at(1)) != null)))
                p = p.plus(2);
            else if (p.at(0) == (byte)'[')
            {
                boolean b;
                { Bytes[] __ = { p }; b = (get_char_class(__) == CLASS_NONE && get_equi_class(__) == 0 && get_coll_element(__) == 0); p = __[0]; }
                if (b)
                    p = p.plus(1); /* not a class name */
            }
            else
                p = p.plus(1);
        }

        return p;
    }

    /*
     * Skip past regular expression.
     * Stop at end of "startp" or where "dirc" is found ('/', '?', etc).
     * Take care of characters with a backslash in front of it.
     * Skip strings inside [ and ].
     * When "newp" is not null and "dirc" is '?', make an allocated copy of the expression
     * and change "\?" to "?".  If "*newp" is not null the expression is changed in-place.
     */
    /*private*/ static Bytes skip_regexp(Bytes startp, byte dirc, boolean magic, Bytes[] newp)
    {
        Bytes p = startp;

        int mymagic;
        if (magic)
            mymagic = MAGIC_ON;
        else
            mymagic = MAGIC_OFF;

        get_cpo_flags();

        for ( ; p.at(0) != NUL; p = p.plus(us_ptr2len_cc(p)))
        {
            if (p.at(0) == dirc)       /* found end of regexp */
                break;
            if ((p.at(0) == (byte)'[' && MAGIC_ON <= mymagic) || (p.at(0) == (byte)'\\' && p.at(1) == (byte)'[' && mymagic <= MAGIC_OFF))
            {
                p = skip_anyof(p.plus(1));
                if (p.at(0) == NUL)
                    break;
            }
            else if (p.at(0) == (byte)'\\' && p.at(1) != NUL)
            {
                if (dirc == '?' && newp != null && p.at(1) == (byte)'?')
                {
                    /* change "\?" to "?", make a copy first. */
                    if (newp[0] == null)
                    {
                        newp[0] = STRDUP(startp);
                        p = newp[0].plus(BDIFF(p, startp));
                    }
                    if (newp[0] != null)
                        BCOPY(p, 0, p, 1, strlen(p, 1) + 1);
                    else
                        p = p.plus(1);
                }
                else
                    p = p.plus(1);    /* skip next character */
                if (p.at(0) == (byte)'v')
                    mymagic = MAGIC_ALL;
                else if (p.at(0) == (byte)'V')
                    mymagic = MAGIC_NONE;
            }
        }

        return p;
    }

    /*
     * bt_regcomp() - compile a regular expression into internal code for the
     * traditional back track matcher.
     * Returns the program in allocated space.  Returns null for an error.
     *
     * We can't allocate space until we know how big the compiled form will be,
     * but we can't compile it (and thus know how big it is) until we've got a
     * place to put the code.  So we cheat:  we compile it twice, once with code
     * generation turned off and size counting turned on, and once "for real".
     * This also means that we don't allocate space until we are sure that the
     * thing really will compile successfully, and we never have to move the
     * code and thus invalidate pointers into it.
     *
     * Whether upper/lower case is to be ignored is decided when executing the
     * program, it does not matter here.
     *
     * Beware that the optimization-preparation code in here knows about some
     * of the structure of the compiled regexp.
     * "re_flags": RE_MAGIC and/or RE_STRING.
     */
    /*private*/ static regprog_C bt_regcomp(Bytes expr, int re_flags)
    {
        if (expr == null)
        {
            emsg(e_null);
            rc_did_emsg = true;
            return null;
        }

        init_class_tab();

        /*
         * First pass: determine size, legality.
         */
        regcomp_start(expr, re_flags);
        regcode = JUST_CALC_SIZE;
        regc(REGMAGIC);
        int[] flags = new int[1];
        if (reg(REG_NOPAREN, flags) == null)
            return null;

        /* Allocate space. */
        bt_regprog_C r = new bt_regprog_C();
        r.program = new Bytes(regsize + 1);

        /*
         * Second pass: emit code.
         */
        regcomp_start(expr, re_flags);
        regcode = r.program;
        regc(REGMAGIC);
        if (reg(REG_NOPAREN, flags) == null || reg_toolong)
        {
            if (reg_toolong)
            {
                emsg(u8("E339: Pattern too long"));
                rc_did_emsg = true;
            }
            return null;
        }

        /* Dig out information for optimizations. */
        r.regstart = NUL;                   /* Worst-case defaults. */
        r.reganch = 0;
        r.regmust = null;
        r.regmlen = 0;
        r.regflags = regflags;
        if ((flags[0] & HASNL) != 0)
            r.regflags |= RF_HASNL;
        if ((flags[0] & HASLOOKBH) != 0)
            r.regflags |= RF_LOOKBH;
        /* Remember whether this pattern has any \z specials in it. */
        r.reghasz = re_has_z;

        Bytes scan = r.program.plus(1);        /* First BRANCH. */
        if (re_op(regnext(scan)) == END)    /* Only one top-level choice. */
        {
            scan = operand(scan);

            /* Starting-point info. */
            if (re_op(scan) == BOL || re_op(scan) == RE_BOF)
            {
                r.reganch++;
                scan = regnext(scan);
            }

            if (re_op(scan) == EXACTLY)
            {
                r.regstart = us_ptr2char(operand(scan));
            }
            else if ((re_op(scan) == BOW
                        || re_op(scan) == EOW
                        || re_op(scan) == NOTHING
                        || re_op(scan) == MOPEN + 0 || re_op(scan) == NOPEN
                        || re_op(scan) == MCLOSE + 0 || re_op(scan) == NCLOSE)
                     && re_op(regnext(scan)) == EXACTLY)
            {
                r.regstart = us_ptr2char(operand(regnext(scan)));
            }

            /*
             * If there's something expensive in the r.e., find the longest literal string
             * that must appear and make it the regmust.  Resolve ties in favor of later strings,
             * since the regstart check works with the beginning of the r.e. and avoiding duplication
             * strengthens checking.  Not a strong reason, but sufficient in the absence of others.
             */
            /*
             * When the r.e. starts with BOW, it is faster to look for a regmust first.
             * Used a lot for "#" and "*" commands.
             */
            if (((flags[0] & SPSTART) != 0 || re_op(scan) == BOW || re_op(scan) == EOW) && (flags[0] & HASNL) == 0)
            {
                Bytes longest = null;
                int len = 0;
                for ( ; scan != null; scan = regnext(scan))
                    if (re_op(scan) == EXACTLY && len <= strlen(operand(scan)))
                    {
                        longest = operand(scan);
                        len = strlen(operand(scan));
                    }
                r.regmust = longest;
                r.regmlen = len;
            }
        }

        r.engine = bt_regengine;
        return (regprog_C)r;
    }

    /*
     * Setup to parse the regexp.  Used once to get the length and once to do it.
     */
    /*private*/ static void regcomp_start(Bytes expr, int re_flags)
        /* re_flags: see vim_regcomp() */
    {
        initchr(expr);
        if ((re_flags & RE_MAGIC) != 0)
            reg_magic = MAGIC_ON;
        else
            reg_magic = MAGIC_OFF;
        reg_string = ((re_flags & RE_STRING) != 0);
        reg_strict = ((re_flags & RE_STRICT) != 0);
        get_cpo_flags();

        num_complex_braces = 0;
        regnpar = 1;
        AFILL(had_endbrace, false);
        regnzpar = 1;
        re_has_z = 0;
        regsize = 0;
        reg_toolong = false;
        regflags = 0;
        had_eol = false;
    }

    /*
     * Check if during the previous call to vim_regcomp the EOL item "$" has been found.
     * This is messy, but it works fine.
     */
    /*private*/ static boolean vim_regcomp_had_eol()
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
    /*private*/ static Bytes reg(int paren, int[] flagp)
        /* paren: REG_NOPAREN, REG_PAREN, REG_NPAREN or REG_ZPAREN */
    {
        Bytes ret;

        flagp[0] = HASWIDTH;          /* Tentatively. */

        int parno = 0;
        if (paren == REG_ZPAREN)
        {
            /* Make a ZOPEN node. */
            if (NSUBEXP <= regnzpar)
            {
                emsg(u8("E50: Too many \\z("));
                rc_did_emsg = true;
                return null;
            }
            parno = regnzpar;
            regnzpar++;
            ret = regnode(ZOPEN + parno);
        }
        else if (paren == REG_PAREN)
        {
            /* Make a MOPEN node. */
            if (NSUBEXP <= regnpar)
            {
                emsg2(u8("E51: Too many %s("), (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                rc_did_emsg = true;
                return null;
            }
            parno = regnpar;
            regnpar++;
            ret = regnode(MOPEN + parno);
        }
        else if (paren == REG_NPAREN)
        {
            /* Make a NOPEN node. */
            ret = regnode(NOPEN);
        }
        else
            ret = null;

        /* Pick up the branches, linking them together. */
        int[] flags = new int[1];
        Bytes br = regbranch(flags);
        if (br == null)
            return null;
        if (ret != null)
            regtail(ret, br);       /* [MZ]OPEN -> first. */
        else
            ret = br;
        /* If one of the branches can be zero-width, the whole thing can.
         * If one of the branches has * at start or matches a line-break, the whole thing can. */
        if ((flags[0] & HASWIDTH) == 0)
            flagp[0] &= ~HASWIDTH;
        flagp[0] |= flags[0] & (SPSTART | HASNL | HASLOOKBH);
        while (peekchr() == Magic('|'))
        {
            skipchr();
            br = regbranch(flags);
            if (br == null || reg_toolong)
                return null;
            regtail(ret, br);       /* BRANCH -> BRANCH. */
            if ((flags[0] & HASWIDTH) == 0)
                flagp[0] &= ~HASWIDTH;
            flagp[0] |= flags[0] & (SPSTART | HASNL | HASLOOKBH);
        }

        /* Make a closing node, and hook it on the end. */
        Bytes ender = regnode(
                (paren == REG_ZPAREN) ? ZCLOSE + parno :
                    (paren == REG_PAREN) ? MCLOSE + parno :
                        (paren == REG_NPAREN) ? NCLOSE : END);
        regtail(ret, ender);

        /* Hook the tails of the branches to the closing node. */
        for (br = ret; br != null; br = regnext(br))
            regoptail(br, ender);

        /* Check for proper termination. */
        if (paren != REG_NOPAREN && getchr() != Magic(')'))
        {
            if (paren == REG_ZPAREN)
                emsg(u8("E52: Unmatched \\z("));
            else if (paren == REG_NPAREN)
                emsg2(e_unmatchedpp, (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
            else
                emsg2(e_unmatchedp, (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
            rc_did_emsg = true;
            return null;
        }
        else if (paren == REG_NOPAREN && peekchr() != NUL)
        {
            if (curchr == Magic(')'))
                emsg2(e_unmatchedpar, (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
            else
                emsg(e_trailing);
            rc_did_emsg = true;
            return null;                    /* "Can't happen". */
        }
        /*
         * Here we set the flag allowing back references to this set of parentheses.
         */
        if (paren == REG_PAREN)
            had_endbrace[parno] = true;     /* have seen the close paren */
        return ret;
    }

    /*
     * Parse one alternative of an | operator.
     * Implements the & operator.
     */
    /*private*/ static Bytes regbranch(int[] flagp)
    {
        flagp[0] = (WORST | HASNL);           /* Tentatively. */

        Bytes ret = regnode(BRANCH);

        for (Bytes chain = null; ; )
        {
            int[] flags = new int[1];
            Bytes latest = regconcat(flags);
            if (latest == null)
                return null;

            /* If one of the branches has width, the whole thing has.
             * If one of the branches anchors at start-of-line, the whole thing does.
             * If one of the branches uses look-behind, the whole thing does. */
            flagp[0] |= flags[0] & (HASWIDTH | SPSTART | HASLOOKBH);
            /* If one of the branches doesn't match a line-break, the whole thing doesn't. */
            flagp[0] &= ~HASNL | (flags[0] & HASNL);
            if (chain != null)
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
    /*private*/ static Bytes regconcat(int[] flagp)
    {
        Bytes first = null;
        Bytes chain = null;

        flagp[0] = WORST;             /* Tentatively. */

        for (boolean cont = true; cont; )
        {
            switch (peekchr())
            {
                case NUL:
                case -132: // case Magic('|'):
                case -218: // case Magic('&'):
                case -215: // case Magic(')'):
                    cont = false;
                    break;

                case -166: // case Magic('Z'):
                    regflags |= RF_ICOMBINE;
                    skipchr_keepstart();
                    break;

                case -157: // case Magic('c'):
                    regflags |= RF_ICASE;
                    skipchr_keepstart();
                    break;

                case -189: // case Magic('C'):
                    regflags |= RF_NOICASE;
                    skipchr_keepstart();
                    break;

                case -138: // case Magic('v'):
                    reg_magic = MAGIC_ALL;
                    skipchr_keepstart();
                    curchr = -1;
                    break;

                case -147: // case Magic('m'):
                    reg_magic = MAGIC_ON;
                    skipchr_keepstart();
                    curchr = -1;
                    break;

                case -179: // case Magic('M'):
                    reg_magic = MAGIC_OFF;
                    skipchr_keepstart();
                    curchr = -1;
                    break;

                case -170: // case Magic('V'):
                    reg_magic = MAGIC_NONE;
                    skipchr_keepstart();
                    curchr = -1;
                    break;

                default:
                {
                    int[] flags = new int[1];
                    Bytes latest = regpiece(flags);
                    if (latest == null || reg_toolong)
                        return null;

                    flagp[0] |= flags[0] & (HASWIDTH | HASNL | HASLOOKBH);
                    if (chain == null)  /* First piece. */
                        flagp[0] |= flags[0] & SPSTART;
                    else
                        regtail(chain, latest);
                    chain = latest;
                    if (first == null)
                        first = latest;
                    break;
                }
            }
        }

        if (first == null)          /* Loop ran zero times. */
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
    /*private*/ static Bytes regpiece(int[] flagp)
    {
        int[] flags = new int[1];
        Bytes ret = regatom(flags);
        if (ret == null)
            return null;

        int op = peekchr();
        if (re_multi_type(op) == NOT_MULTI)
        {
            flagp[0] = flags[0];
            return ret;
        }

        /* default flags */
        flagp[0] = (WORST | SPSTART | (flags[0] & (HASNL | HASLOOKBH)));

        skipchr();
        switch (op)
        {
            case -214: // case Magic('*'):
            {
                if ((flags[0] & SIMPLE) != 0)
                    reginsert(STAR, ret);
                else
                {
                    /* Emit x* as (x&|), where & means "self". */
                    reginsert(BRANCH, ret);             /* Either x */
                    regoptail(ret, regnode(BACK));      /* and loop */
                    regoptail(ret, ret);                /* back */
                    regtail(ret, regnode(BRANCH));      /* or */
                    regtail(ret, regnode(NOTHING));     /* null. */
                }
                break;
            }

            case -213: // case Magic('+'):
            {
                if ((flags[0] & SIMPLE) != 0)
                    reginsert(PLUS, ret);
                else
                {
                    /* Emit x+ as x(&|), where & means "self". */
                    Bytes next = regnode(BRANCH);      /* Either */
                    regtail(ret, next);
                    regtail(regnode(BACK), ret);        /* loop back */
                    regtail(next, regnode(BRANCH));     /* or */
                    regtail(ret, regnode(NOTHING));     /* null. */
                }
                flagp[0] = (WORST | HASWIDTH | (flags[0] & (HASNL | HASLOOKBH)));
                break;
            }

            case -192: // case Magic('@'):
            {
                int lop = END;
                int nr = getdecchrs();

                switch (no_Magic(getchr()))
                {
                    case '=': lop = MATCH; break;                   /* \@= */
                    case '!': lop = NOMATCH; break;                 /* \@! */
                    case '>': lop = SUBPAT; break;                  /* \@> */
                    case '<': switch (no_Magic(getchr()))
                              {
                                  case '=': lop = BEHIND; break;    /* \@<= */
                                  case '!': lop = NOBEHIND; break;  /* \@<! */
                              }
                              break;
                }
                if (lop == END)
                {
                    emsg2(u8("E59: invalid character after %s@"), (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                    rc_did_emsg = true;
                    return null;
                }
                /* Look behind must match with behind_pos. */
                if (lop == BEHIND || lop == NOBEHIND)
                {
                    regtail(ret, regnode(BHPOS));
                    flagp[0] |= HASLOOKBH;
                }
                regtail(ret, regnode(END));                 /* operand ends */
                if (lop == BEHIND || lop == NOBEHIND)
                {
                    if (nr < 0)
                        nr = 0;                             /* no limit is same as zero limit */
                    reginsert_nr(lop, nr, ret);
                }
                else
                    reginsert(lop, ret);
                break;
            }

            case -193: // case Magic('?'):
            case -195: // case Magic('='):
            {
                /* Emit x= as (x|). */
                reginsert(BRANCH, ret);                 /* Either x */
                regtail(ret, regnode(BRANCH));          /* or */
                Bytes next = regnode(NOTHING);         /* null. */
                regtail(ret, next);
                regoptail(ret, next);
                break;
            }

            case -133: // case Magic('{'):
            {
                long[] minval = new long[1];
                long[] maxval = new long[1];
                if (!read_limits(minval, maxval))
                    return null;

                if ((flags[0] & SIMPLE) != 0)
                {
                    reginsert(BRACE_SIMPLE, ret);
                    reginsert_limits(BRACE_LIMITS, minval[0], maxval[0], ret);
                }
                else
                {
                    if (10 <= num_complex_braces)
                    {
                        emsg2(u8("E60: Too many complex %s{...}s"), (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                        rc_did_emsg = true;
                        return null;
                    }
                    reginsert(BRACE_COMPLEX + num_complex_braces, ret);
                    regoptail(ret, regnode(BACK));
                    regoptail(ret, ret);
                    reginsert_limits(BRACE_LIMITS, minval[0], maxval[0], ret);
                    num_complex_braces++;
                }
                if (0 < minval[0] && 0 < maxval[0])
                    flagp[0] = (HASWIDTH | (flags[0] & (HASNL | HASLOOKBH)));
                break;
            }
        }

        if (re_multi_type(peekchr()) != NOT_MULTI)
        {
            /* Can't have a multi follow a multi. */
            if (peekchr() == Magic('*'))
                libC.sprintf(ioBuff, u8("E61: Nested %s*"), (MAGIC_ON <= reg_magic) ? u8("") : u8("\\"));
            else
                libC.sprintf(ioBuff, u8("E62: Nested %s%c"), (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"), no_Magic(peekchr()));

            emsg(ioBuff);
            rc_did_emsg = true;
            return null;
        }

        return ret;
    }

    /* When making changes to "classchars" also change "nfa_classcodes". */
    /*private*/ static Bytes classchars = u8(".iIkKfFpPsSdDxXoOwWhHaAlLuU");
    /*private*/ static int[] classcodes =
    {
        ANY,
        IDENT, SIDENT,
        KWORD, SKWORD,
        FNAME, SFNAME,
        PRINT, SPRINT,
        WHITE, NWHITE,
        DIGIT, NDIGIT,
        HEX,   NHEX,
        OCTAL, NOCTAL,
        WORD,  NWORD,
        HEAD,  NHEAD,
        ALPHA, NALPHA,
        LOWER, NLOWER,
        UPPER, NUPPER
    };

    /*
     * Parse the lowest level.
     *
     * Optimization:  gobbles an entire sequence of ordinary characters so that
     * it can turn them into a single node, which is smaller to store and
     * faster to run.  Don't do this when one_exactly is set.
     */
    /*private*/ static Bytes regatom(int[] flagp)
    {
        Bytes ret;

        int extra = 0;

        flagp[0] = WORST;             /* Tentatively. */

        int c = getchr();

        collection:
        {
            switch (c)
            {
                case -162: // case Magic('^'):
                    ret = regnode(BOL);
                    break;

                case -220: // case Magic('$'):
                    ret = regnode(EOL);
                    had_eol = true;
                    break;

                case -196: // case Magic('<'):
                    ret = regnode(BOW);
                    break;

                case -194: // case Magic('>'):
                    ret = regnode(EOW);
                    break;

                case -161: // case Magic('_'):
                {
                    c = no_Magic(getchr());
                    if (c == '^')           /* "\_^" is start-of-line */
                    {
                        ret = regnode(BOL);
                        break;
                    }
                    if (c == '$')           /* "\_$" is end-of-line */
                    {
                        ret = regnode(EOL);
                        had_eol = true;
                        break;
                    }

                    extra = ADD_NL;
                    flagp[0] |= HASNL;

                    /* "\_[" is character range plus newline */
                    if (c == '[')
                        break collection;

                    /* "\_x" is character class plus newline */
                    /* FALLTHROUGH */
                }
                /*
                * Character classes.
                */
                case -210: // case Magic('.'):
                case -151: // case Magic('i'):
                case -183: // case Magic('I'):
                case -149: // case Magic('k'):
                case -181: // case Magic('K'):
                case -154: // case Magic('f'):
                case -186: // case Magic('F'):
                case -144: // case Magic('p'):
                case -176: // case Magic('P'):
                case -141: // case Magic('s'):
                case -173: // case Magic('S'):
                case -156: // case Magic('d'):
                case -188: // case Magic('D'):
                case -136: // case Magic('x'):
                case -168: // case Magic('X'):
                case -145: // case Magic('o'):
                case -177: // case Magic('O'):
                case -137: // case Magic('w'):
                case -169: // case Magic('W'):
                case -152: // case Magic('h'):
                case -184: // case Magic('H'):
                case -159: // case Magic('a'):
                case -191: // case Magic('A'):
                case -148: // case Magic('l'):
                case -180: // case Magic('L'):
                case -139: // case Magic('u'):
                case -171: // case Magic('U'):
                {
                    Bytes p = vim_strchr(classchars, no_Magic(c));
                    if (p == null)
                    {
                        emsg(u8("E63: invalid use of \\_"));
                        rc_did_emsg = true;
                        return null;
                    }
                    /* When '.' is followed by a composing char ignore the dot,
                     * so that the composing char is matched here. */
                    if (c == Magic('.') && utf_iscomposing(peekchr()))
                    {
                        c = getchr();
                        ret = regnode(MULTIBYTECODE);
                        regmbc(c);
                        flagp[0] |= HASWIDTH | SIMPLE;
                        break;
                    }
                    ret = regnode(classcodes[BDIFF(p, classchars)] + extra);
                    flagp[0] |= HASWIDTH | SIMPLE;
                    break;
                }

                case -146: // case Magic('n'):
                {
                    if (reg_string)
                    {
                        /* In a string "\n" matches a newline character. */
                        ret = regnode(EXACTLY);
                        regc(NL);
                        regc(NUL);
                        flagp[0] |= HASWIDTH | SIMPLE;
                    }
                    else
                    {
                        /* In buffer text "\n" matches the end of a line. */
                        ret = regnode(NEWL);
                        flagp[0] |= HASWIDTH | HASNL;
                    }
                    break;
                }

                case -216: // case Magic('('):
                {
                    if (one_exactly)
                    {
                        emsg2(u8("E369: invalid item in %s%%[]"), (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                        rc_did_emsg = true;
                        return null;
                    }
                    int[] flags = new int[1];
                    ret = reg(REG_PAREN, flags);
                    if (ret == null)
                        return null;
                    flagp[0] |= flags[0] & (HASWIDTH | SPSTART | HASNL | HASLOOKBH);
                    break;
                }

                case NUL:
                case -132: // case Magic('|'):
                case -218: // case Magic('&'):
                case -215: // case Magic(')'):
                {
                    if (one_exactly)
                        emsg2(u8("E369: invalid item in %s%%[]"), (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                    else
                        emsg(e_internal);       /* Supposed to be caught earlier. */
                    rc_did_emsg = true;
                    return null;
                }

                case -195: // case Magic('='):
                case -193: // case Magic('?'):
                case -213: // case Magic('+'):
                case -192: // case Magic('@'):
                case -133: // case Magic('{'):
                case -214: // case Magic('*'):
                {
                    c = no_Magic(c);
                    libC.sprintf(ioBuff, u8("E64: %s%c follows nothing"),
                            (c == '*' ? MAGIC_ON <= reg_magic : reg_magic == MAGIC_ALL) ? u8("") : u8("\\"), c);

                    emsg(ioBuff);
                    rc_did_emsg = true;
                    return null;
                }

                case -130: // case Magic('~'):                    /* previous substitute pattern */
                {
                    if (reg_prev_sub != null)
                    {
                        ret = regnode(EXACTLY);
                        Bytes lp = reg_prev_sub;
                        while (lp.at(0) != NUL)
                            regc((lp = lp.plus(1)).at(-1));
                        regc(NUL);
                        if (reg_prev_sub.at(0) != NUL)
                        {
                            flagp[0] |= HASWIDTH;
                            if (BDIFF(lp, reg_prev_sub) == 1)
                                flagp[0] |= SIMPLE;
                        }
                    }
                    else
                    {
                        emsg(e_nopresub);
                        rc_did_emsg = true;
                        return null;
                    }
                    break;
                }

                case -207: // case Magic('1'):
                case -206: // case Magic('2'):
                case -205: // case Magic('3'):
                case -204: // case Magic('4'):
                case -203: // case Magic('5'):
                case -202: // case Magic('6'):
                case -201: // case Magic('7'):
                case -200: // case Magic('8'):
                case -199: // case Magic('9'):
                {
                    int refnum = c - Magic('0');

                    /*
                     * Check if the back reference is legal.  We must have seen the close brace.
                     * TODO: Should also check that we don't refer to something
                     * that is repeated (+*=): what instance of the repetition should we match?
                     */
                    if (!had_endbrace[refnum])
                    {
                        /* Trick: check if "@<=" or "@<!" follows, in which case
                         * the \1 can appear before the referenced match. */
                        Bytes p;
                        for (p = regparse; p.at(0) != NUL; p = p.plus(1))
                            if (p.at(0) == (byte)'@' && p.at(1) == (byte)'<' && (p.at(2) == (byte)'!' || p.at(2) == (byte)'='))
                                break;
                        if (p.at(0) == NUL)
                        {
                            emsg(u8("E65: Illegal back reference"));
                            rc_did_emsg = true;
                            return null;
                        }
                    }
                    ret = regnode(BACKREF + refnum);
                    break;
                }

                case -134: // case Magic('z'):
                {
                    c = no_Magic(getchr());
                    switch (c)
                    {
                        case '(':
                        {
                            if (reg_do_extmatch != REX_SET)
                            {
                                emsg(e_z_not_allowed);
                                rc_did_emsg = true;
                                return null;
                            }
                            if (one_exactly)
                            {
                                emsg2(u8("E369: invalid item in %s%%[]"), (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                                rc_did_emsg = true;
                                return null;
                            }
                            int[] flags = new int[1];
                            ret = reg(REG_ZPAREN, flags);
                            if (ret == null)
                                return null;
                            flagp[0] |= flags[0] & (HASWIDTH|SPSTART|HASNL|HASLOOKBH);
                            re_has_z = REX_SET;
                            break;
                        }
                        case '1':
                        case '2':
                        case '3':
                        case '4':
                        case '5':
                        case '6':
                        case '7':
                        case '8':
                        case '9':
                        {
                            if (reg_do_extmatch != REX_USE)
                            {
                                emsg(e_z1_not_allowed);
                                rc_did_emsg = true;
                                return null;
                            }
                            ret = regnode(ZREF + c - '0');
                            re_has_z = REX_USE;
                            break;
                        }
                        case 's':
                        {
                            ret = regnode(MOPEN + 0);
                            if (re_mult_next(u8("\\zs")) == false)
                                return null;
                            break;
                        }
                        case 'e':
                        {
                            ret = regnode(MCLOSE + 0);
                            if (re_mult_next(u8("\\ze")) == false)
                                return null;
                            break;
                        }
                        default:
                        {
                            emsg(u8("E68: Invalid character after \\z"));
                            rc_did_emsg = true;
                            return null;
                        }
                    }
                    break;
                }

                case -219: // case Magic('%'):
                {
                    c = no_Magic(getchr());
                    switch (c)
                    {
                        /* () without a back reference */
                        case '(':
                        {
                            if (one_exactly)
                            {
                                emsg2(u8("E369: invalid item in %s%%[]"), (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                                rc_did_emsg = true;
                                return null;
                            }
                            int[] flags = new int[1];
                            ret = reg(REG_NPAREN, flags);
                            if (ret == null)
                                return null;
                            flagp[0] |= flags[0] & (HASWIDTH | SPSTART | HASNL | HASLOOKBH);
                            break;
                        }
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

                        /* \%[abc]: Emit as a list of branches,
                         * all ending at the last branch which matches nothing. */
                        case '[':
                        {
                            if (one_exactly)        /* doesn't nest */
                            {
                                emsg2(u8("E369: invalid item in %s%%[]"), (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                                rc_did_emsg = true;
                                return null;
                            }

                            Bytes lastbranch;
                            Bytes lastnode = null;
                            Bytes br;

                            ret = null;
                            while ((c = getchr()) != ']')
                            {
                                if (c == NUL)
                                {
                                    emsg2(e_missing_sb, (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                                    rc_did_emsg = true;
                                    return null;
                                }
                                br = regnode(BRANCH);
                                if (ret == null)
                                    ret = br;
                                else
                                    regtail(lastnode, br);

                                ungetchr();
                                one_exactly = true;
                                lastnode = regatom(flagp);
                                one_exactly = false;
                                if (lastnode == null)
                                    return null;
                            }
                            if (ret == null)
                            {
                                emsg2(e_empty_sb, (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                                rc_did_emsg = true;
                                return null;
                            }
                            lastbranch = regnode(BRANCH);
                            br = regnode(NOTHING);
                            if (ret != JUST_CALC_SIZE)
                            {
                                regtail(lastnode, br);
                                regtail(lastbranch, br);
                                /* connect all branches to the NOTHING branch at the end */
                                for (br = ret; BNE(br, lastnode); )
                                {
                                    if (re_op(br) == BRANCH)
                                    {
                                        regtail(br, lastbranch);
                                        br = operand(br);
                                    }
                                    else
                                        br = regnext(br);
                                }
                            }
                            flagp[0] &= ~(HASWIDTH | SIMPLE);
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
                            {
                                emsg2(u8("E678: Invalid character after %s%%[dxouU]"), (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                                rc_did_emsg = true;
                                return null;
                            }
                            if (use_multibytecode(i))
                                ret = regnode(MULTIBYTECODE);
                            else
                                ret = regnode(EXACTLY);
                            if (i == 0)
                                regc(0x0a);
                            else
                                regmbc(i);
                            regc(NUL);
                            flagp[0] |= HASWIDTH;
                            break;
                        }
                        default:
                        {
                            if (asc_isdigit(c) || c == '<' || c == '>' || c == '\'')
                            {
                                int cmp = c;
                                if (cmp == '<' || cmp == '>')
                                    c = getchr();

                                long n = 0;
                                while (asc_isdigit(c))
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
                                        (regcode = regcode.plus(1)).be(-1, c);
                                        (regcode = regcode.plus(1)).be(-1, cmp);
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
                                        (regcode = regcode.plus(1)).be(-1, cmp);
                                    }
                                    break;
                                }
                            }

                            emsg2(u8("E71: Invalid character after %s%%"), (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                            rc_did_emsg = true;
                            return null;
                        }
                    }
                    break;
                }

                case -165: // case Magic('['):
                    break collection;

                default:
                    return do_multibyte(c, flagp);
            }

            return ret;
        }

        /*
         * If there is no matching ']', we assume the '[' is a normal character.
         * This makes 'incsearch' and ":help [" work.
         */
        Bytes lp = skip_anyof(regparse);

        if (lp.at(0) == (byte)']')     /* there is a matching ']' */
        {
            int startc = -1;    /* > 0 when next '-' is a range */
            int endc;

            /*
             * In a character class, different parsing rules apply.
             * Not even \ is special anymore, nothing is.
             */
            if (regparse.at(0) == (byte)'^')       /* Complement of range. */
            {
                ret = regnode(ANYBUT + extra);
                regparse = regparse.plus(1);
            }
            else
                ret = regnode(ANYOF + extra);

            /* At the start ']' and '-' mean the literal character. */
            if (regparse.at(0) == (byte)']' || regparse.at(0) == (byte)'-')
            {
                startc = regparse.at(0);
                regc((regparse = regparse.plus(1)).at(-1));
            }

            while (regparse.at(0) != NUL && regparse.at(0) != (byte)']')
            {
                if (regparse.at(0) == (byte)'-')
                {
                    regparse = regparse.plus(1);
                    /* The '-' is not used for a range at the end and after or before a '\n'. */
                    if (regparse.at(0) == (byte)']' || regparse.at(0) == NUL
                            || startc == -1
                            || (regparse.at(0) == (byte)'\\' && regparse.at(1) == (byte)'n'))
                    {
                        regc('-');
                        startc = '-';       /* [--x] is a range */
                    }
                    else
                    {
                        /* Also accept "a-[.z.]". */
                        endc = 0;
                        if (regparse.at(0) == (byte)'[')
                        {
                            Bytes[] __ = { regparse };
                            endc = get_coll_element(__);
                            regparse = __[0];
                        }
                        if (endc == 0)
                        {
                            Bytes[] __ = { regparse };
                            endc = us_ptr2char_adv(__, true);
                            regparse = __[0];
                        }

                        /* Handle \o40, \x20 and \u20AC style sequences. */
                        if (endc == '\\' && !reg_cpo_lit && !reg_cpo_bsl)
                            endc = coll_get_char();

                        if (endc < startc)
                        {
                            emsg(e_invrange);
                            rc_did_emsg = true;
                            return null;
                        }
                        if (1 < utf_char2len(startc) || 1 < utf_char2len(endc))
                        {
                            /* Limit to a range of 256 chars. */
                            if (startc + 256 < endc)
                            {
                                emsg(e_invrange);
                                rc_did_emsg = true;
                                return null;
                            }
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
                else if (regparse.at(0) == (byte)'\\'
                        && !reg_cpo_bsl
                        && (vim_strchr(REGEXP_INRANGE, regparse.at(1)) != null
                            || (!reg_cpo_lit && vim_strchr(REGEXP_ABBR, regparse.at(1)) != null)))
                {
                    regparse = regparse.plus(1);
                    if (regparse.at(0) == (byte)'n')
                    {
                        /* '\n' in range: also match NL */
                        if (ret != JUST_CALC_SIZE)
                        {
                            /* Using \n inside [^] does not change what
                             * matches.  "[^\n]" is the same as ".". */
                            if (ret.at(0) == ANYOF)
                            {
                                ret.be(0, ANYOF + ADD_NL);
                                flagp[0] |= HASNL;
                            }
                            /* else: must have had a \n already */
                        }
                        regparse = regparse.plus(1);
                        startc = -1;
                    }
                    else if (regparse.at(0) == (byte)'d'
                          || regparse.at(0) == (byte)'o'
                          || regparse.at(0) == (byte)'x'
                          || regparse.at(0) == (byte)'u'
                          || regparse.at(0) == (byte)'U')
                    {
                        startc = coll_get_char();
                        if (startc == 0)
                            regc(0x0a);
                        else
                            regmbc(startc);
                    }
                    else
                    {
                        startc = backslash_trans((regparse = regparse.plus(1)).at(-1));
                        regc(startc);
                    }
                }
                else if (regparse.at(0) == (byte)'[')
                {
                    int cu;

                    int c_class;
                    { Bytes[] __ = { regparse }; c_class = get_char_class(__); regparse = __[0]; }
                    startc = -1;
                    /* Characters assumed to be 8 bits! */
                    switch (c_class)
                    {
                        case CLASS_NONE:
                        {
                            { Bytes[] __ = { regparse }; c_class = get_equi_class(__); regparse = __[0]; }
                            if (c_class != 0)
                            {
                                /* produce equivalence class */
                                reg_equi_class(c_class);
                            }
                            else
                            {
                                { Bytes[] __ = { regparse }; c_class = get_coll_element(__); regparse = __[0]; }
                                if (c_class != 0)
                                {
                                    /* produce a collating element */
                                    regmbc(c_class);
                                }
                                else
                                {
                                    /* literal '[', allow [[-x] as a range */
                                    startc = (regparse = regparse.plus(1)).at(-1);
                                    regc(startc);
                                }
                            }
                            break;
                        }
                        case CLASS_ALNUM:
                            for (cu = 1; cu <= 255; cu++)
                                if (asc_isalnum(cu))
                                    regc(cu);
                            break;
                        case CLASS_ALPHA:
                            for (cu = 1; cu <= 255; cu++)
                                if (asc_isalpha(cu))
                                    regc(cu);
                            break;
                        case CLASS_BLANK:
                            regc(' ');
                            regc('\t');
                            break;
                        case CLASS_CNTRL:
                            for (cu = 1; cu <= 255; cu++)
                                if (asc_iscntrl(cu))
                                    regc(cu);
                            break;
                        case CLASS_DIGIT:
                            for (cu = 1; cu <= 255; cu++)
                                if (asc_isdigit(cu))
                                    regc(cu);
                            break;
                        case CLASS_GRAPH:
                            for (cu = 1; cu <= 255; cu++)
                                if (asc_isgraph(cu))
                                    regc(cu);
                            break;
                        case CLASS_LOWER:
                            for (cu = 1; cu <= 255; cu++)
                                if (utf_islower(cu))
                                    regc(cu);
                            break;
                        case CLASS_PRINT:
                            for (cu = 1; cu <= 255; cu++)
                                if (vim_isprintc(cu))
                                    regc(cu);
                            break;
                        case CLASS_PUNCT:
                            for (cu = 1; cu <= 255; cu++)
                                if (asc_ispunct(cu))
                                    regc(cu);
                            break;
                        case CLASS_SPACE:
                            for (cu = 9; cu <= 13; cu++)
                                regc(cu);
                            regc(' ');
                            break;
                        case CLASS_UPPER:
                            for (cu = 1; cu <= 255; cu++)
                                if (utf_isupper(cu))
                                    regc(cu);
                            break;
                        case CLASS_XDIGIT:
                            for (cu = 1; cu <= 255; cu++)
                                if (asc_isxdigit(cu))
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
                    /* produce a multibyte character,
                     * including any following composing characters */
                    startc = us_ptr2char(regparse);
                    int len = us_ptr2len_cc(regparse);
                    if (utf_char2len(startc) != len)
                        startc = -1;        /* composing chars */
                    while (0 <= --len)
                        regc((regparse = regparse.plus(1)).at(-1));
                }
            }
            regc(NUL);
            prevchr_len = 1;                /* last char was the ']' */
            if (regparse.at(0) != (byte)']')
            {
                emsg(e_toomsbra);
                rc_did_emsg = true;
                return null; /* Cannot happen? */
            }
            skipchr();                      /* let's be friends with the lexer again */
            flagp[0] |= HASWIDTH | SIMPLE;
            return ret;
        }

        if (reg_strict)
        {
            emsg2(e_missingbracket, (MAGIC_OFF < reg_magic) ? u8("") : u8("\\"));
            rc_did_emsg = true;
            return null;
        }

        return do_multibyte(c, flagp);
    }

    /*private*/ static final Bytes do_multibyte(int c, int[] flagp)
    {
        Bytes ret;

        /* A multi-byte character is handled as a separate atom
         * if it's before a multi and when it's a composing char. */
        if (use_multibytecode(c))
        {
            ret = regnode(MULTIBYTECODE);
            regmbc(c);
            flagp[0] |= HASWIDTH | SIMPLE;
            return ret;
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
        int len;
        for (len = 0; c != NUL && (len == 0 || (re_multi_type(peekchr()) == NOT_MULTI && !one_exactly && !is_Magic(c))); len++)
        {
            c = no_Magic(c);

            regmbc(c);

            /* Need to get composing character too. */
            for ( ; ; )
            {
                int l = us_ptr2len(regparse);
                if (!utf_iscomposing(us_ptr2char(regparse.plus(l))))
                    break;
                regmbc(us_ptr2char(regparse));
                skipchr();
            }

            c = getchr();
        }
        ungetchr();

        regc(NUL);

        flagp[0] |= HASWIDTH;
        if (len == 1)
            flagp[0] |= SIMPLE;

        return ret;
    }

    /*
     * Return true if MULTIBYTECODE should be used instead of EXACTLY for character "c".
     */
    /*private*/ static boolean use_multibytecode(int c)
    {
        return (1 < utf_char2len(c)) && (re_multi_type(peekchr()) != NOT_MULTI || utf_iscomposing(c));
    }

    /*
     * Emit a node.
     * Return pointer to generated code.
     */
    /*private*/ static Bytes regnode(int op)
    {
        Bytes ret = regcode;

        if (ret == JUST_CALC_SIZE)
            regsize += 3;
        else
        {
            (regcode = regcode.plus(1)).be(-1, op);
            (regcode = regcode.plus(1)).be(-1, NUL);           /* Null "next" pointer. */
            (regcode = regcode.plus(1)).be(-1, NUL);
        }

        return ret;
    }

    /*
     * Emit (if appropriate) a byte of code.
     */
    /*private*/ static void regc(int b)
    {
        if (regcode == JUST_CALC_SIZE)
            regsize++;
        else
            (regcode = regcode.plus(1)).be(-1, b);
    }

    /*
     * Emit (if appropriate) a multi-byte character of code.
     */
    /*private*/ static void regmbc(int c)
    {
        if (regcode == JUST_CALC_SIZE)
            regsize += utf_char2len(c);
        else
            regcode = regcode.plus(utf_char2bytes(c, regcode));
    }

    /*
     * Insert an operator in front of already-emitted operand.
     *
     * Means relocating the operand.
     */
    /*private*/ static void reginsert(int op, Bytes opnd)
    {
        if (regcode == JUST_CALC_SIZE)
        {
            regsize += 3;
            return;
        }

        Bytes src = regcode;
        regcode = regcode.plus(3);
        Bytes dst = regcode;
        while (BLT(opnd, src))
            (dst = dst.minus(1)).be(0, (src = src.minus(1)).at(0));

        Bytes place = opnd; /* Op node, where operand used to be. */
        (place = place.plus(1)).be(-1, op);
        (place = place.plus(1)).be(-1, NUL);
        place.be(0, NUL);
    }

    /*
     * Insert an operator in front of already-emitted operand.
     * Add a number to the operator.
     */
    /*private*/ static void reginsert_nr(int op, long val, Bytes opnd)
    {
        if (regcode == JUST_CALC_SIZE)
        {
            regsize += 7;
            return;
        }

        Bytes src = regcode;
        regcode = regcode.plus(7);
        Bytes dst = regcode;
        while (BLT(opnd, src))
            (dst = dst.minus(1)).be(0, (src = src.minus(1)).at(0));

        Bytes place = opnd; /* Op node, where operand used to be. */
        (place = place.plus(1)).be(-1, op);
        (place = place.plus(1)).be(-1, NUL);
        (place = place.plus(1)).be(-1, NUL);
        place = re_put_long(place, val);
    }

    /*
     * Insert an operator in front of already-emitted operand.
     * The operator has the given limit values as operands.
     * Also set next pointer.
     *
     * Means relocating the operand.
     */
    /*private*/ static void reginsert_limits(int op, long minval, long maxval, Bytes opnd)
    {
        if (regcode == JUST_CALC_SIZE)
        {
            regsize += 11;
            return;
        }

        Bytes src = regcode;
        regcode = regcode.plus(11);
        Bytes dst = regcode;
        while (BLT(opnd, src))
            (dst = dst.minus(1)).be(0, (src = src.minus(1)).at(0));

        Bytes place = opnd; /* Op node, where operand used to be. */
        (place = place.plus(1)).be(-1, op);
        (place = place.plus(1)).be(-1, NUL);
        (place = place.plus(1)).be(-1, NUL);
        place = re_put_long(place, minval);
        place = re_put_long(place, maxval);
        regtail(opnd, place);
    }

    /*
     * Write a long as four bytes at "p" and return pointer to the next char.
     */
    /*private*/ static Bytes re_put_long(Bytes p, long val)
    {
        (p = p.plus(1)).be(-1, (byte)((val >>> 24) & 0xff));
        (p = p.plus(1)).be(-1, (byte)((val >>> 16) & 0xff));
        (p = p.plus(1)).be(-1, (byte)((val >>>  8) & 0xff));
        (p = p.plus(1)).be(-1, (byte)((val       ) & 0xff));
        return p;
    }

    /*
     * Set the next-pointer at the end of a node chain.
     */
    /*private*/ static void regtail(Bytes p, Bytes val)
    {
        if (p == JUST_CALC_SIZE)
            return;

        /* Find last node. */
        Bytes scan = p;
        for ( ; ; )
        {
            Bytes temp = regnext(scan);
            if (temp == null)
                break;
            scan = temp;
        }

        int offset;
        if (re_op(scan) == BACK)
            offset = BDIFF(scan, val);
        else
            offset = BDIFF(val, scan);
        /* When the offset uses more than 16 bits it can no longer fit in the two bytes available.
         * Use a global flag to avoid having to check return values in too many places. */
        if (0xffff < offset)
            reg_toolong = true;
        else
        {
            scan.be(1, (byte)((offset >>> 8) & 0xff));
            scan.be(2, (byte)((offset      ) & 0xff));
        }
    }

    /*
     * Like regtail, on item after a BRANCH; nop if none.
     */
    /*private*/ static void regoptail(Bytes p, Bytes val)
    {
        /* When op is neither BRANCH nor BRACE_COMPLEX0-9, it is "operandless". */
        if (p == null || p == JUST_CALC_SIZE
                || (re_op(p) != BRANCH && (re_op(p) < BRACE_COMPLEX || BRACE_COMPLEX + 9 < re_op(p))))
            return;

        regtail(operand(p), val);
    }

    /*
     * Functions for getting characters from the regexp input.
     */

    /*private*/ static boolean at_start;        /* true when on the first character */
    /*private*/ static boolean prev_at_start;   /* true when on the second character */

    /*
     * Start parsing at "str".
     */
    /*private*/ static void initchr(Bytes str)
    {
        regparse = str;
        prevchr_len = 0;
        curchr = prevprevchr = prevchr = nextchr = -1;
        at_start = true;
        prev_at_start = false;
    }

    /*
     * Save the current parse state, so that it can be restored and parsing
     * starts in the same state again.
     */
    /*private*/ static void save_parse_state(parse_state_C ps)
    {
        ps.regparse = regparse;
        ps.prevchr_len = prevchr_len;
        ps.curchr = curchr;
        ps.prevchr = prevchr;
        ps.prevprevchr = prevprevchr;
        ps.nextchr = nextchr;
        ps.at_start = at_start;
        ps.prev_at_start = prev_at_start;
        ps.regnpar = regnpar;
    }

    /*
     * Restore a previously saved parse state.
     */
    /*private*/ static void restore_parse_state(parse_state_C ps)
    {
        regparse = ps.regparse;
        prevchr_len = ps.prevchr_len;
        curchr = ps.curchr;
        prevchr = ps.prevchr;
        prevprevchr = ps.prevprevchr;
        nextchr = ps.nextchr;
        at_start = ps.at_start;
        prev_at_start = ps.prev_at_start;
        regnpar = ps.regnpar;
    }

    /*private*/ static int after_slash;

    /*
     * Get the next character without advancing.
     */
    /*private*/ static int peekchr()
    {
        if (curchr == -1)
        {
            switch (curchr = regparse.at(0))
            {
                case '.':
                case '[':
                case '~':
                    /* magic when 'magic' is on */
                    if (MAGIC_ON <= reg_magic)
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
                case '/':       /* can't be used in / command */
                    /* magic only after "\v" */
                    if (reg_magic == MAGIC_ALL)
                        curchr = Magic(curchr);
                    break;

                case '*':
                    /* * is not magic as the very first character, e.g. "?*ptr",
                     * when after '^', e.g. "/^*ptr" and when after "\(", "\|", "\&".
                     * But "\(\*" is not magic, thus must be magic if "after_slash" */
                    if (MAGIC_ON <= reg_magic
                            && !at_start
                            && !(prev_at_start && prevchr == Magic('^'))
                            && (after_slash != 0
                                || (prevchr != Magic('(')
                                    && prevchr != Magic('&')
                                    && prevchr != Magic('|'))))
                        curchr = Magic('*');
                    break;

                case '^':
                    /* '^' is only magic as the very first character
                     * and if it's after "\(", "\|", "\&' or "\n" */
                    if (MAGIC_OFF <= reg_magic
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
                        at_start = true;
                        prev_at_start = false;
                    }
                    break;

                case '$':
                    /* '$' is only magic as the very last char
                     * and if it's in front of either "\|", "\)", "\&", or "\n" */
                    if (MAGIC_OFF <= reg_magic)
                    {
                        Bytes p = regparse.plus(1);
                        boolean is_magic_all = (reg_magic == MAGIC_ALL);

                        /* ignore \c \C \m \M \v \V and \Z after '$' */
                        while (p.at(0) == (byte)'\\' && (p.at(1) == (byte)'c' || p.at(1) == (byte)'C'
                                        || p.at(1) == (byte)'m' || p.at(1) == (byte)'M'
                                        || p.at(1) == (byte)'v' || p.at(1) == (byte)'V' || p.at(1) == (byte)'Z'))
                        {
                            if (p.at(1) == (byte)'v')
                                is_magic_all = true;
                            else if (p.at(1) == (byte)'m' || p.at(1) == (byte)'M' || p.at(1) == (byte)'V')
                                is_magic_all = false;
                            p = p.plus(2);
                        }
                        if (p.at(0) == NUL
                                || (p.at(0) == (byte)'\\'
                                    && (p.at(1) == (byte)'|' || p.at(1) == (byte)'&' || p.at(1) == (byte)')' || p.at(1) == (byte)'n'))
                                || (is_magic_all
                                    && (p.at(0) == (byte)'|' || p.at(0) == (byte)'&' || p.at(0) == (byte)')'))
                                || reg_magic == MAGIC_ALL)
                            curchr = Magic('$');
                    }
                    break;

                case '\\':
                {
                    int c = regparse.at(1);

                    if (c == NUL)
                        curchr = '\\';      /* trailing '\' */
                    else if (char_u((byte)c) <= '~' && META_flags[c] != 0)
                    {
                        /*
                         * META contains everything that may be magic sometimes,
                         * except ^ and $ ("\^" and "\$" are only magic after "\v").
                         * We now fetch the next character and toggle its magicness.
                         * Therefore, \ is so meta-magic that it is not in META.
                         */
                        curchr = -1;
                        prev_at_start = at_start;
                        at_start = false;   /* be able to say "/\*ptr" */
                        regparse = regparse.plus(1);
                        after_slash++;
                        peekchr();
                        regparse = regparse.minus(1);
                        --after_slash;
                        curchr = toggle_Magic(curchr);
                    }
                    else if (vim_strchr(REGEXP_ABBR, c) != null)
                    {
                        /*
                         * Handle abbreviations, like "\t" for TAB.
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
                        curchr = us_ptr2char(regparse.plus(1));
                    }
                    break;
                }

                default:
                    curchr = us_ptr2char(regparse);
                    break;
            }
        }

        return curchr;
    }

    /*
     * Eat one lexed character.  Do this in a way that we can undo it.
     */
    /*private*/ static void skipchr()
    {
        /* peekchr() eats a backslash, do the same here */
        if (regparse.at(0) == (byte)'\\')
            prevchr_len = 1;
        else
            prevchr_len = 0;
        if (regparse.at(prevchr_len) != NUL)
        {
            /* exclude composing chars that us_ptr2len_cc does include */
            prevchr_len += us_ptr2len(regparse.plus(prevchr_len));
        }
        regparse = regparse.plus(prevchr_len);
        prev_at_start = at_start;
        at_start = false;
        prevprevchr = prevchr;
        prevchr = curchr;
        curchr = nextchr;       /* use previously unget char, or -1 */
        nextchr = -1;
    }

    /*
     * Skip a character while keeping the value of prev_at_start for at_start.
     * prevchr and prevprevchr are also kept.
     */
    /*private*/ static void skipchr_keepstart()
    {
        boolean as = prev_at_start;
        int pr = prevchr;
        int prpr = prevprevchr;

        skipchr();

        at_start = as;
        prevchr = pr;
        prevprevchr = prpr;
    }

    /*
     * Get the next character from the pattern.  We know about magic and such, so
     * therefore we need a lexical analyzer.
     */
    /*private*/ static int getchr()
    {
        int chr = peekchr();

        skipchr();

        return chr;
    }

    /*
     * put character back.  Works only once!
     */
    /*private*/ static void ungetchr()
    {
        nextchr = curchr;
        curchr = prevchr;
        prevchr = prevprevchr;
        at_start = prev_at_start;
        prev_at_start = false;

        /* Backup "regparse", so that it's at the same position as before the getchr(). */
        regparse = regparse.minus(prevchr_len);
    }

    /*
     * Get and return the value of the hex string at the current position.
     * Return -1 if there is no valid hex number.
     * The position is updated:
     *     blahblah\%x20asdf
     *         before-^ ^-after
     * The parameter controls the maximum number of input characters.  This will be
     * 2 when reading a \%x20 sequence and 4 when reading a \%u20AC sequence.
     */
    /*private*/ static int gethexchrs(int maxinputlen)
    {
        int nr = 0;
        int i;

        for (i = 0; i < maxinputlen; i++)
        {
            int c = regparse.at(0);
            if (!asc_isxdigit(c))
                break;
            nr <<= 4;
            nr |= hex2nr(c);
            regparse = regparse.plus(1);
        }

        if (i == 0)
            return -1;

        return nr;
    }

    /*
     * Get and return the value of the decimal string immediately after the
     * current position.  Return -1 for invalid.  Consumes all digits.
     */
    /*private*/ static int getdecchrs()
    {
        int nr = 0;
        int i;

        for (i = 0; ; i++)
        {
            int c = regparse.at(0);
            if (c < '0' || '9' < c)
                break;
            nr *= 10;
            nr += c - '0';
            regparse = regparse.plus(1);
            curchr = -1;    /* no longer valid */
        }

        if (i == 0)
            return -1;

        return nr;
    }

    /*
     * get and return the value of the octal string immediately after the current
     * position. Return -1 for invalid, or 0-255 for valid.  Smart enough to handle
     * numbers > 377 correctly (for example, 400 is treated as 40) and doesn't
     * treat 8 or 9 as recognised characters.  Position is updated:
     *     blahblah\%o210asdf
     *         before-^  ^-after
     */
    /*private*/ static int getoctchrs()
    {
        int nr = 0;
        int i;

        for (i = 0; i < 3 && nr < 040; i++)
        {
            int c = regparse.at(0);
            if (c < '0' || '7' < c)
                break;
            nr <<= 3;
            nr |= hex2nr(c);
            regparse = regparse.plus(1);
        }

        if (i == 0)
            return -1;

        return nr;
    }

    /*
     * Get a number after a backslash that is inside [].
     * When nothing is recognized return a backslash.
     */
    /*private*/ static int coll_get_char()
    {
        int nr = -1;

        switch ((regparse = regparse.plus(1)).at(-1))
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
            regparse = regparse.minus(1);
            nr = '\\';
        }

        return nr;
    }

    /*
     * read_limits - Read two integers to be taken as a minimum and maximum.
     * If the first character is '-', then the range is reversed.
     * Should end with 'end'.  If minval is missing, zero is default,
     * if maxval is missing, a very big number is the default.
     */
    /*private*/ static boolean read_limits(long[] minval, long[] maxval)
    {
        boolean reverse = false;

        if (regparse.at(0) == (byte)'-')
        {
            /* starts with '-', so reverse the range later */
            regparse = regparse.plus(1);
            reverse = true;
        }
        Bytes first_char = regparse;
        { Bytes[] __ = { regparse }; minval[0] = getdigits(__); regparse = __[0]; }
        if (regparse.at(0) == (byte)',')                       /* there is a comma */
        {
            if (asc_isdigit((regparse = regparse.plus(1)).at(0)))
            {
                Bytes[] __ = { regparse }; maxval[0] = getdigits(__); regparse = __[0];
            }
            else
                maxval[0] = MAX_LIMIT;
        }
        else if (asc_isdigit(first_char.at(0)))
            maxval[0] = minval[0];                      /* it was \{n} or \{-n} */
        else
            maxval[0] = MAX_LIMIT;                    /* it was \{} or \{-} */
        if (regparse.at(0) == (byte)'\\')
            regparse = regparse.plus(1);                             /* allow either \{...} or \{...\} */
        if (regparse.at(0) != (byte)'}')
        {
            libC.sprintf(ioBuff, u8("E554: Syntax error in %s{...}"), (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));

            emsg(ioBuff);
            rc_did_emsg = true;
            return false;
        }

        /*
         * Reverse the range if there was a '-', or make sure it is in the right order otherwise.
         */
        if ((!reverse && maxval[0] < minval[0]) || (reverse && minval[0] < maxval[0]))
        {
            long tmp = minval[0];
            minval[0] = maxval[0];
            maxval[0] = tmp;
        }
        skipchr();          /* let's be friends with the lexer again */
        return true;
    }

    /*
     * Global work variables for vim_regexec().
     */

    /* The current match-position is remembered with these variables: */
    /*private*/ static long     reglnum;                /* line number, relative to first line */
    /*private*/ static Bytes    regline;                /* start of current line */
    /*private*/ static Bytes    reginput;               /* current input, points into "regline" */

    /*private*/ static boolean  need_clear_subexpr;     /* subexpressions still need to be cleared */
    /*private*/ static boolean  need_clear_zsubexpr;    /* extmatch subexpressions still need to be cleared */

    /*
     * Structure used to save the current input state, when it needs to be
     * restored after trying a match.  Used by reg_save() and reg_restore().
     * Also stores the length of "backpos".
     */
    /*private*/ static final class regsave_C
    {
        Bytes       rs_ptr;     /* reginput pointer, for single-line regexp */
        lpos_C      rs_pos;     /* reginput pos, for multi-line regexp */
        int         rs_len;

        /*private*/ regsave_C()
        {
            rs_pos = new lpos_C();
        }
    }

    /*private*/ static void COPY_regsave(regsave_C rs1, regsave_C rs0)
    {
        rs1.rs_ptr = rs0.rs_ptr;
        COPY_lpos(rs1.rs_pos, rs0.rs_pos);
        rs1.rs_len = rs0.rs_len;
    }

    /* struct to save start/end pointer/position in for \(\) */
    /*private*/ static final class save_se_C
    {
        Bytes       se_ptr;
        lpos_C      se_pos;

        /*private*/ save_se_C()
        {
            se_pos = new lpos_C();
        }
    }

    /*private*/ static save_se_C[] ARRAY_save_se(int n)
    {
        save_se_C[] a = new save_se_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new save_se_C();
        return a;
    }

    /* used for BEHIND and NOBEHIND matching */
    /*private*/ static final class regbehind_C
    {
        regsave_C   save_after;
        regsave_C   save_behind;
        boolean     save_need_clear_subexpr;
        save_se_C[] save_start;
        save_se_C[] save_end;

        /*private*/ regbehind_C()
        {
            save_after = new regsave_C();
            save_behind = new regsave_C();
            save_start = ARRAY_save_se(NSUBEXP);
            save_end = ARRAY_save_se(NSUBEXP);
        }
    }

    /*
     * Internal copy of 'ignorecase'.  It is set at each call to vim_regexec().
     * Normally it gets the value of "rm_ic" or "rmm_ic", but when the pattern
     * contains '\c' or '\C' the value is overruled.
     */
    /*private*/ static boolean ireg_ic;

    /*
     * Similar to ireg_ic, but only for 'combining' characters.  Set with \Z flag
     * in the regexp.  Defaults to false, always.
     */
    /*private*/ static boolean ireg_icombine;

    /*
     * Copy of "rmm_maxcol": maximum column to search for a match.  Zero when
     * there is no maximum.
     */
    /*private*/ static int     ireg_maxcol;

    /*
     * Sometimes need to save a copy of a line.  Since calloc()/free() is very
     * slow, we keep one allocated piece of memory and only re-allocate it when
     * it's too small.  It's freed in bt_regexec_both() when finished.
     */
    /*private*/ static Bytes reg_tofree;
    /*private*/ static int reg_tofree_len;

    /*
     * These variables are set when executing a regexp to speed up the execution.
     * Which ones are set depends on whether a single-line or multi-line match is
     * done:
     *                      single-line             multi-line
     * reg_match            regmatch_C              null
     * reg_mmatch           null                    regmmatch_C
     * reg_startp           reg_match.startp        <invalid>
     * reg_endp             reg_match.endp          <invalid>
     * reg_startpos         <invalid>               reg_mmatch.startpos
     * reg_endpos           <invalid>               reg_mmatch.endpos
     * reg_win              null                    window in which to search
     * reg_buf              curbuf                  buffer in which to search
     * reg_firstlnum        <invalid>               first line in which to search
     * reg_maxline          0                       last line nr
     * reg_line_lbr         false or true           false
     */
    /*private*/ static regmatch_C   reg_match;
    /*private*/ static regmmatch_C  reg_mmatch;
    /*private*/ static Bytes[]      reg_startp;
    /*private*/ static Bytes[]      reg_endp;
    /*private*/ static lpos_C[]     reg_startpos;
    /*private*/ static lpos_C[]     reg_endpos;
    /*private*/ static window_C     reg_win;
    /*private*/ static buffer_C     reg_buf;
    /*private*/ static long         reg_firstlnum;
    /*private*/ static long         reg_maxline;
    /*private*/ static boolean      reg_line_lbr;       /* "\n" in string is line break */

    /* Values for rs_state in regitem_C. */
    /*private*/ static final int
        RS_NOPEN = 0,           /* NOPEN and NCLOSE */
        RS_MOPEN = 1,           /* MOPEN + [0-9] */
        RS_MCLOSE = 2,          /* MCLOSE + [0-9] */
        RS_ZOPEN = 3,           /* ZOPEN + [0-9] */
        RS_ZCLOSE = 4,          /* ZCLOSE + [0-9] */
        RS_BRANCH = 5,          /* BRANCH */
        RS_BRCPLX_MORE = 6,     /* BRACE_COMPLEX and trying one more match */
        RS_BRCPLX_LONG = 7,     /* BRACE_COMPLEX and trying longest match */
        RS_BRCPLX_SHORT = 8,    /* BRACE_COMPLEX and trying shortest match */
        RS_NOMATCH = 9,         /* NOMATCH */
        RS_BEHIND1 = 10,        /* BEHIND / NOBEHIND matching rest */
        RS_BEHIND2 = 11,        /* BEHIND / NOBEHIND matching behind part */
        RS_STAR_LONG = 12,      /* STAR/PLUS/BRACE_SIMPLE longest match */
        RS_STAR_SHORT = 13;     /* STAR/PLUS/BRACE_SIMPLE shortest match */

    /*
     * When there are alternatives, a RS_ is put on the regstack to remember what we are doing.
     * Before it may be another type of item, depending on "rs_state", to remember more things.
     */
    /*private*/ static final class regitem_C
    {
        int         rs_state;       /* what we are doing, one of RS_ above */
        Bytes       rs_scan;        /* current node in program */
        save_se_C   rs_sesave;      /* union room for saving reginput */
        regsave_C   rs_regsave;     /* union room for saving reginput */
        int         rs_no;          /* submatch nr or BEHIND/NOBEHIND */

        /*private*/ regitem_C()
        {
            rs_sesave = new save_se_C();
            rs_regsave = new regsave_C();
        }
    }

    /* Used for STAR, PLUS and BRACE_SIMPLE matching. */
    /*private*/ static final class regstar_C
    {
        int         nextb;          /* next byte */
        int         nextb_ic;       /* next byte reverse case */
        long        count;
        long        minval;
        long        maxval;

        /*private*/ regstar_C()
        {
        }
    }

    /* Used to store input position when a BACK was encountered,
     * so that we now if we made any progress since the last time. */
    /*private*/ static final class backpos_C
    {
        Bytes       bp_scan;        /* "scan" where BACK was encountered */
        regsave_C   bp_pos;         /* last input position */

        /*private*/ backpos_C()
        {
            bp_pos = new regsave_C();
        }
    }

    /*
     * "regstack" and "backpos" are used by regmatch().
     * They are kept over calls to avoid invoking calloc() and free() often.
     * "regstack" is a stack with regitem_C items, sometimes preceded by regstar_C or regbehind_C.
     * "backpos" is a table with backpos_C items for BACK.
     */
    /*private*/ static Growing<Object> regstack;
    /*private*/ static Growing<backpos_C> backpos;

    /*
     * Both for regstack and backpos tables we use the following strategy of allocation
     * (to reduce calloc/free calls):
     * - Initial size is fairly small.
     * - When needed, the tables are grown bigger (8 times at first, double after that).
     * - After executing the match we free the memory only if the array has grown.
     *   Thus the memory is kept allocated when it's at the initial size.
     * This makes it fast while not keeping a lot of memory allocated.
     * A three times speed increase was observed when using many simple patterns.
     */
    /*private*/ static final int REGSTACK_INITIAL        = 2048;
    /*private*/ static final int BACKPOS_INITIAL         = 64;

    /*
     * Create "regstack" and "backpos".
     * We allocate *_INITIAL amount of bytes first and then set the grow size to much bigger value
     * to avoid many calloc calls in case of deep regular expressions.
     */
    /*private*/ static void create_regstack()
    {
        /* Use Object item, since we push different things onto the regstack. */
        regstack = new Growing<Object>(Object.class, REGSTACK_INITIAL);
        regstack.ga_grow(REGSTACK_INITIAL);
        regstack.ga_growsize = REGSTACK_INITIAL * 8;
    }

    /*private*/ static void create_backpos()
    {
        backpos = new Growing<backpos_C>(backpos_C.class, BACKPOS_INITIAL);
        backpos.ga_grow(BACKPOS_INITIAL);
        backpos.ga_growsize = BACKPOS_INITIAL * 8;
    }

    /*
     * Get pointer to the line "lnum", which is relative to "reg_firstlnum".
     */
    /*private*/ static Bytes reg_getline(long lnum)
    {
        /* When looking behind for a match/no-match, lnum is negative, but we can't go before line 1. */
        if (reg_firstlnum + lnum < 1)
            return null;
        if (reg_maxline < lnum)
            /* Must have matched the "\n" in the last line. */
            return u8("");

        return ml_get_buf(reg_buf, reg_firstlnum + lnum, false);
    }

    /*private*/ static regsave_C behind_pos = new regsave_C();

    /*private*/ static Bytes[] reg_startzp = new Bytes[NSUBEXP];      /* Workspace to mark beginning */
    /*private*/ static Bytes[] reg_endzp = new Bytes[NSUBEXP];        /*   and end of \z(...\) matches */
    /*private*/ static lpos_C[] reg_startzpos = ARRAY_lpos(NSUBEXP);    /* idem, beginning pos */
    /*private*/ static lpos_C[] reg_endzpos = ARRAY_lpos(NSUBEXP);      /* idem, end pos */

    /*
     * Match a regexp against a string.
     * "rmp.regprog" is a compiled regexp as returned by vim_regcomp().
     * Uses curbuf for line count and 'iskeyword'.
     * If "line_lbr" is true, consider a "\n" in "line" to be a line break.
     *
     * Returns 0 for failure, number of lines contained in the match otherwise.
     */
    /*private*/ static long bt_regexec_nl(regmatch_C rmp, Bytes line, int col, boolean line_lbr)
        /* line: string to match against */
        /* col: column to start looking for match */
    {
        reg_match = rmp;
        reg_mmatch = null;
        reg_maxline = 0;
        reg_line_lbr = line_lbr;
        reg_buf = curbuf;
        reg_win = null;
        ireg_ic = rmp.rm_ic;
        ireg_icombine = false;
        ireg_maxcol = 0;

        return bt_regexec_both(line, col, null);
    }

    /*
     * Match a regexp against multiple lines.
     * "rmp.regprog" is a compiled regexp as returned by vim_regcomp().
     * Uses curbuf for line count and 'iskeyword'.
     *
     * Return zero if there is no match.  Return number of lines contained in the match otherwise.
     */
    /*private*/ static long bt_regexec_multi(regmmatch_C rmp, window_C win, buffer_C buf, long lnum, int col, timeval_C tm)
        /* win: window in which to search or null */
        /* buf: buffer in which to search */
        /* lnum: nr of line to start looking for match */
        /* col: column to start looking for match */
        /* tm: timeout limit or null */
    {
        reg_match = null;
        reg_mmatch = rmp;
        reg_buf = buf;
        reg_win = win;
        reg_firstlnum = lnum;
        reg_maxline = reg_buf.b_ml.ml_line_count - lnum;
        reg_line_lbr = false;
        ireg_ic = rmp.rmm_ic;
        ireg_icombine = false;
        ireg_maxcol = rmp.rmm_maxcol;

        return bt_regexec_both(null, col, tm);
    }

    /*
     * Match a regexp against a string ("line" points to the string)
     * or multiple lines ("line" is null, use reg_getline()).
     * Returns 0 for failure, number of lines contained in the match otherwise.
     */
    /*private*/ static long bt_regexec_both(Bytes line, int col, timeval_C tm)
        /* col: column to start looking for match */
        /* tm: timeout limit or null */
    {
        long retval = 0L;

        if (regstack == null)
            create_regstack();
        if (backpos == null)
            create_backpos();

        bt_regprog_C prog;
        if (reg_match == null)
        {
            prog = (bt_regprog_C)reg_mmatch.regprog;
            line = reg_getline(0);
            reg_startpos = reg_mmatch.startpos;
            reg_endpos = reg_mmatch.endpos;
        }
        else
        {
            prog = (bt_regprog_C)reg_match.regprog;
            reg_startp = reg_match.startp;
            reg_endp = reg_match.endp;
        }

        theend:
        {
            /* Be paranoid... */
            if (prog == null || line == null)
            {
                emsg(e_null);
                break theend;
            }

            /* Check validity of program. */
            if (prog_magic_wrong())
                break theend;

            /* If the start column is past the maximum column: no need to try. */
            if (0 < ireg_maxcol && ireg_maxcol <= col)
                break theend;

            /* If pattern contains "\c" or "\C": overrule value of ireg_ic. */
            if ((prog.regflags & RF_ICASE) != 0)
                ireg_ic = true;
            else if ((prog.regflags & RF_NOICASE) != 0)
                ireg_ic = false;

            /* If pattern contains "\Z" overrule value of ireg_icombine. */
            if ((prog.regflags & RF_ICOMBINE) != 0)
                ireg_icombine = true;

            /* If there is a "must appear" string, look for it. */
            if (prog.regmust != null)
            {
                int c = us_ptr2char(prog.regmust);
                Bytes s = line.plus(col);

                /*
                 * This is used very often, esp. for ":global".
                 * Use three versions of the loop to avoid overhead of conditions.
                 */
                if (!ireg_ic)
                    while ((s = vim_strchr(s, c)) != null)
                    {
                        int cmp;
                        { int[] __ = { prog.regmlen }; cmp = cstrncmp(s, prog.regmust, __); prog.regmlen = __[0]; }
                        if (cmp == 0)
                            break;              /* Found it. */
                        s = s.plus(us_ptr2len_cc(s));
                    }
                else
                    while ((s = cstrchr(s, c)) != null)
                    {
                        int cmp;
                        { int[] __ = { prog.regmlen }; cmp = cstrncmp(s, prog.regmust, __); prog.regmlen = __[0]; }
                        if (cmp == 0)
                            break;              /* Found it. */
                        s = s.plus(us_ptr2len_cc(s));
                    }
                if (s == null)          /* Not present. */
                    break theend;
            }

            regline = line;
            reglnum = 0;
            reg_toolong = false;

            /* Simplest case: Anchored match need be tried only once. */
            if (prog.reganch != 0)
            {
                int c = us_ptr2char(regline.plus(col));
                if (prog.regstart == NUL || prog.regstart == c
                    || (ireg_ic && ((utf_fold(prog.regstart) == utf_fold(c))
                        || (c < 255 && prog.regstart < 255 && utf_tolower(prog.regstart) == utf_tolower(c)))))
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
                    if (prog.regstart != NUL)
                    {
                        /* Skip until the char we know it must start with.
                         * Used often, do some work to avoid call overhead. */
                        Bytes s = cstrchr(regline.plus(col), prog.regstart);
                        if (s == null)
                        {
                            retval = 0;
                            break;
                        }
                        col = BDIFF(s, regline);
                    }

                    /* Check for maximum column to try. */
                    if (0 < ireg_maxcol && ireg_maxcol <= col)
                    {
                        retval = 0;
                        break;
                    }

                    retval = regtry(prog, col);
                    if (0 < retval)
                        break;

                    /* if not currently on the first line, get it again */
                    if (reglnum != 0)
                    {
                        reglnum = 0;
                        regline = reg_getline(0);
                    }
                    if (regline.at(col) == NUL)
                        break;
                    col += us_ptr2len_cc(regline.plus(col));
                    /* Check for timeout once in a twenty times to avoid overhead. */
                    if (tm != null && ++tm_count == 20)
                    {
                        tm_count = 0;
                        if (profile_passed_limit(tm))
                            break;
                    }
                }
            }
        }

        /* Free "reg_tofree" when it's a bit big. */
        if (400 < reg_tofree_len)
            reg_tofree = null;

        /* Free backpos and regstack if they are bigger than their initial size. */
        if (BACKPOS_INITIAL < backpos.ga_maxlen)
        {
            backpos_C[] bpp = backpos.ga_data;
            while (0 < backpos.ga_len--)
                bpp[backpos.ga_len] = null;
            backpos.ga_clear();
            backpos = null;
        }
        if (REGSTACK_INITIAL < regstack.ga_maxlen)
        {
            Object[] rpp = regstack.ga_data;
            while (0 < regstack.ga_len--)
                rpp[regstack.ga_len] = null;
            regstack.ga_clear();
            regstack = null;
        }

        return retval;
    }

    /*
     * Create a new extmatch and mark it as referenced once.
     */
    /*private*/ static reg_extmatch_C make_extmatch()
    {
        reg_extmatch_C em = new reg_extmatch_C();
        em.refcnt = 1;
        return em;
    }

    /*
     * Add a reference to an extmatch.
     */
    /*private*/ static reg_extmatch_C ref_extmatch(reg_extmatch_C em)
    {
        if (em != null)
            em.refcnt++;
        return em;
    }

    /*
     * Try match of "prog" with at regline[col].
     * Returns 0 for failure, number of lines contained in the match otherwise.
     */
    /*private*/ static long regtry(bt_regprog_C prog, int col)
    {
        reginput = regline.plus(col);
        need_clear_subexpr = true;
        /* Clear the external match subpointers if necessary. */
        if (prog.reghasz == REX_SET)
            need_clear_zsubexpr = true;

        if (regmatch(prog.program.plus(1)) == false)
            return 0;

        cleanup_subexpr();
        if (reg_match == null)
        {
            if (reg_startpos[0].lnum < 0)
            {
                reg_startpos[0].lnum = 0;
                reg_startpos[0].col = col;
            }
            if (reg_endpos[0].lnum < 0)
            {
                reg_endpos[0].lnum = reglnum;
                reg_endpos[0].col = BDIFF(reginput, regline);
            }
            else
                /* Use line number of "\ze". */
                reglnum = reg_endpos[0].lnum;
        }
        else
        {
            if (reg_startp[0] == null)
                reg_startp[0] = regline.plus(col);
            if (reg_endp[0] == null)
                reg_endp[0] = reginput;
        }
        /* Package any found \z(...\) matches for export.  Default is none. */
        re_extmatch_out = null;

        if (prog.reghasz == REX_SET)
        {
            cleanup_zsubexpr();
            re_extmatch_out = make_extmatch();
            for (int i = 0; i < NSUBEXP; i++)
            {
                if (reg_match == null)
                {
                    /* Only accept single line matches. */
                    if (0 <= reg_startzpos[i].lnum
                            && reg_endzpos[i].lnum == reg_startzpos[i].lnum
                             && reg_endzpos[i].col >= reg_startzpos[i].col)
                        re_extmatch_out.matches[i] =
                            STRNDUP(reg_getline(reg_startzpos[i].lnum).plus(reg_startzpos[i].col),
                                       reg_endzpos[i].col - reg_startzpos[i].col);
                }
                else
                {
                    if (reg_startzp[i] != null && reg_endzp[i] != null)
                        re_extmatch_out.matches[i] =
                                STRNDUP(reg_startzp[i], BDIFF(reg_endzp[i], reg_startzp[i]));
                }
            }
        }
        return 1 + reglnum;
    }

    /*
     * Get class of previous character.
     */
    /*private*/ static int reg_prev_class()
    {
        if (BLT(regline, reginput))
            return us_get_class(reginput.minus(1 + us_head_off(regline, reginput.minus(1))), reg_buf);

        return -1;
    }

    /*
     * Return true if the current reginput position matches the Visual area.
     */
    /*private*/ static boolean reg_match_visual()
    {
        window_C wp = (reg_win == null) ? curwin : reg_win;

        /* Check if the buffer is the current buffer. */
        if (reg_buf != curbuf || VIsual.lnum == 0)
            return false;

        pos_C top = new pos_C();
        pos_C bot = new pos_C();
        int mode;
        if (VIsual_active)
        {
            if (ltpos(VIsual, wp.w_cursor))
            {
                COPY_pos(top, VIsual);
                COPY_pos(bot, wp.w_cursor);
            }
            else
            {
                COPY_pos(top, wp.w_cursor);
                COPY_pos(bot, VIsual);
            }
            mode = VIsual_mode;
        }
        else
        {
            if (ltpos(curbuf.b_visual.vi_start, curbuf.b_visual.vi_end))
            {
                COPY_pos(top, curbuf.b_visual.vi_start);
                COPY_pos(bot, curbuf.b_visual.vi_end);
            }
            else
            {
                COPY_pos(top, curbuf.b_visual.vi_end);
                COPY_pos(bot, curbuf.b_visual.vi_start);
            }
            mode = curbuf.b_visual.vi_mode;
        }
        long lnum = reglnum + reg_firstlnum;
        if (lnum < top.lnum || bot.lnum < lnum)
            return false;

        if (mode == 'v')
        {
            int col = BDIFF(reginput, regline);
            if ((lnum == top.lnum && col < top.col)
             || (lnum == bot.lnum && col >= bot.col + ((p_sel[0].at(0) != (byte)'e') ? 1 : 0)))
                return false;
        }
        else if (mode == Ctrl_V)
        {
            int[] start1 = new int[1];
            int[] end1 = new int[1];
            getvvcol(wp, top, start1, null, end1);
            int[] start2 = new int[1];
            int[] end2 = new int[1];
            getvvcol(wp, bot, start2, null, end2);
            if (start2[0] < start1[0])
                start1[0] = start2[0];
            if (end1[0] < end2[0])
                end1[0] = end2[0];
            if (top.col == MAXCOL || bot.col == MAXCOL)
                end1[0] = MAXCOL;
            int cols = win_linetabsize(wp, regline, BDIFF(reginput, regline));
            if (cols < start1[0] || end1[0] - (p_sel[0].at(0) == (byte)'e' ? 1 : 0) < cols)
                return false;
        }

        return true;
    }

    /*
     * The arguments from BRACE_LIMITS are stored here.  They are actually local
     * to regmatch(), but they are here to reduce the amount of stack space used
     * (it can be called recursively many times).
     */
    /*private*/ static long     bl_minval;
    /*private*/ static long     bl_maxval;

    /*private*/ static final int
        RA_FAIL = 1,            /* something failed, abort */
        RA_CONT = 2,            /* continue in inner loop */
        RA_BREAK = 3,           /* break inner loop */
        RA_MATCH = 4,           /* successful match */
        RA_NOMATCH = 5;         /* didn't match */

    /*
     * regmatch - main matching routine
     *
     * Conceptually the strategy is simple:
     * check to see whether the current node matches, push an item onto the regstack
     * and loop to see whether the rest matches, and then act accordingly.
     *
     * In practice we make some effort to avoid using the regstack,
     * in particular by going through "ordinary" nodes (that don't need to know
     * whether the rest of the match failed) by a nested loop.
     *
     * Returns true when there is a match.
     * Leaves reginput and reglnum just after the last matched character.
     *
     * Returns false when there is no match.
     * Leaves reginput and reglnum in an undefined state!
     */
    /*private*/ static boolean regmatch(Bytes scan)
        /* scan: Current node. */
    {
        int status;                 /* one of the RA_ values: */

        /* Make "regstack" and "backpos" empty.
         * They are allocated and freed in bt_regexec_both() to reduce calloc()/free() calls. */
        regstack.ga_len = 0;
        backpos.ga_len = 0;

        /*
         * Repeat until "regstack" is empty.
         */
        for ( ; ; )
        {
            /* Some patterns may take a long time to match, e.g., "\([a-z]\+\)\+Q".
             * Allow interrupting them with CTRL-C. */
            fast_breakcheck();

            /*
             * Repeat for items that can be matched sequentially, without using the regstack.
             */
            for ( ; ; )
            {
                if (got_int || scan == null)
                {
                    status = RA_FAIL;
                    break;
                }
                status = RA_CONT;

                Bytes next = regnext(scan);        /* Next node. */

                int op = re_op(scan);
                /* Check for character class with NL added. */
                if (!reg_line_lbr && with_nl(op) && reg_match == null && reginput.at(0) == NUL && reglnum <= reg_maxline)
                {
                    reg_nextline();
                }
                else if (reg_line_lbr && with_nl(op) && reginput.at(0) == (byte)'\n')
                {
                    reginput = reginput.plus(us_ptr2len_cc(reginput));
                }
                else
                {
                    if (with_nl(op))
                        op -= ADD_NL;
                    int c = us_ptr2char(reginput);
                    switch (op)
                    {
                        case BOL:
                            if (BNE(reginput, regline))
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
                            if (reglnum != 0 || BNE(reginput, regline) || (reg_match == null && 1 < reg_firstlnum))
                                status = RA_NOMATCH;
                            break;

                        case RE_EOF:
                            if (reglnum != reg_maxline || c != NUL)
                                status = RA_NOMATCH;
                            break;

                        case CURSOR:
                            /* Check if the buffer is in a window and compare the
                             * reg_win.w_cursor position to the match position. */
                            if (reg_win == null
                                    || reglnum + reg_firstlnum != reg_win.w_cursor.lnum
                                    || BDIFF(reginput, regline) != reg_win.w_cursor.col)
                                status = RA_NOMATCH;
                            break;

                        case RE_MARK:
                        {
                            /* Compare the mark position to the match position. */
                            int mark = operand(scan).at(0);
                            int cmp = operand(scan).at(1);

                            pos_C pos = getmark_buf(reg_buf, mark, false);
                            if (pos == null                 /* mark doesn't exist */
                                    || pos.lnum <= 0        /* mark isn't set in reg_buf */
                                    || (pos.lnum == reglnum + reg_firstlnum
                                            ? (pos.col == BDIFF(reginput, regline)
                                                ? (cmp == '<' || cmp == '>')
                                                : (pos.col < BDIFF(reginput, regline)
                                                    ? cmp != '>'
                                                    : cmp != '<'))
                                            : (pos.lnum < reglnum + reg_firstlnum
                                                ? cmp != '>'
                                                : cmp != '<')))
                                status = RA_NOMATCH;
                            break;
                        }

                        case RE_VISUAL:
                            if (!reg_match_visual())
                                status = RA_NOMATCH;
                            break;

                        case RE_LNUM:
                            if (!(reg_match == null) || !re_num_cmp(reglnum + reg_firstlnum, scan))
                                status = RA_NOMATCH;
                            break;

                        case RE_COL:
                            if (!re_num_cmp(BDIFF(reginput, regline) + 1, scan))
                                status = RA_NOMATCH;
                            break;

                        case RE_VCOL:
                            if (!re_num_cmp((long)win_linetabsize((reg_win == null) ? curwin : reg_win,
                                            regline, BDIFF(reginput, regline)) + 1, scan))
                                status = RA_NOMATCH;
                            break;

                        case BOW:                           /* \<word; reginput points to w */
                        {
                            if (c == NUL)                   /* Can't match at end of line */
                                status = RA_NOMATCH;
                            else
                            {
                                int this_class;

                                /* Get class of current and previous char (if it exists). */
                                this_class = us_get_class(reginput, reg_buf);
                                if (this_class <= 1)
                                    status = RA_NOMATCH;    /* not on a word at all */
                                else if (reg_prev_class() == this_class)
                                    status = RA_NOMATCH;    /* previous char is in same word */
                            }
                            break;
                        }

                        case EOW:                           /* word\>; reginput points after d */
                        {
                            if (BEQ(reginput, regline))        /* Can't match at start of line */
                                status = RA_NOMATCH;
                            else
                            {
                                int this_class, prev_class;

                                /* Get class of current and previous char (if it exists). */
                                this_class = us_get_class(reginput, reg_buf);
                                prev_class = reg_prev_class();
                                if (this_class == prev_class || prev_class == 0 || prev_class == 1)
                                    status = RA_NOMATCH;
                            }
                            break; /* Matched with EOW */
                        }

                        case ANY:
                            /* ANY does not match new lines. */
                            if (c == NUL)
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case IDENT:
                            if (!vim_isIDc(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case SIDENT:
                            if (asc_isdigit(reginput.at(0)) || !vim_isIDc(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case KWORD:
                            if (!us_iswordp(reginput, reg_buf))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case SKWORD:
                            if (asc_isdigit(reginput.at(0)) || !us_iswordp(reginput, reg_buf))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case FNAME:
                            if (!vim_isfilec(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case SFNAME:
                            if (asc_isdigit(reginput.at(0)) || !vim_isfilec(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case PRINT:
                            if (!vim_isprintc(us_ptr2char(reginput)))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case SPRINT:
                            if (asc_isdigit(reginput.at(0)) || !vim_isprintc(us_ptr2char(reginput)))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case WHITE:
                            if (!vim_iswhite(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case NWHITE:
                            if (c == NUL || vim_iswhite(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case DIGIT:
                            if (!ri_digit(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case NDIGIT:
                            if (c == NUL || ri_digit(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case HEX:
                            if (!ri_hex(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case NHEX:
                            if (c == NUL || ri_hex(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case OCTAL:
                            if (!ri_octal(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case NOCTAL:
                            if (c == NUL || ri_octal(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case WORD:
                            if (!ri_word(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case NWORD:
                            if (c == NUL || ri_word(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case HEAD:
                            if (!ri_head(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case NHEAD:
                            if (c == NUL || ri_head(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case ALPHA:
                            if (!ri_alpha(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case NALPHA:
                            if (c == NUL || ri_alpha(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case LOWER:
                            if (!ri_lower(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case NLOWER:
                            if (c == NUL || ri_lower(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case UPPER:
                            if (!ri_upper(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case NUPPER:
                            if (c == NUL || ri_upper(c))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case EXACTLY:
                        {
                            Bytes opnd = operand(scan);
                            /* Inline the first byte, for speed. */
                            if (opnd.at(0) != reginput.at(0) && !ireg_ic)
                                status = RA_NOMATCH;
                            else if (opnd.at(0) == NUL)
                            {
                                /* match empty string always works; happens when "~" is empty. */
                            }
                            else
                            {
                                int[] len = new int[1];
                                if (opnd.at(1) == NUL && !ireg_ic)
                                {
                                    len[0] = 1;        /* matched a single byte above */
                                }
                                else
                                {
                                    /* Need to match first byte again for multi-byte. */
                                    len[0] = strlen(opnd);
                                    if (cstrncmp(opnd, reginput, len) != 0)
                                        status = RA_NOMATCH;
                                }
                                /* Check for following composing character, unless %C
                                 * follows (skips over all composing chars). */
                                if (status != RA_NOMATCH
                                        && utf_iscomposing(us_ptr2char(reginput.plus(len[0])))
                                        && !ireg_icombine
                                        && re_op(next) != RE_COMPOSING)
                                {
                                    /* This code makes a composing character get ignored,
                                     * which is the correct behavior (sometimes)
                                     * for voweled Hebrew texts. */
                                    status = RA_NOMATCH;
                                }
                                if (status != RA_NOMATCH)
                                    reginput = reginput.plus(len[0]);
                            }
                            break;
                        }

                        case ANYOF:
                        case ANYBUT:
                            if (c == NUL)
                                status = RA_NOMATCH;
                            else if ((cstrchr(operand(scan), c) == null) == (op == ANYOF))
                                status = RA_NOMATCH;
                            else
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            break;

                        case MULTIBYTECODE:
                        {
                            Bytes opnd = operand(scan);
                            /* Safety check (just in case 'encoding' was changed since compiling the program). */
                            int len = us_ptr2len_cc(opnd);
                            if (len < 2)
                            {
                                status = RA_NOMATCH;
                                break;
                            }
                            int opndc = us_ptr2char(opnd);
                            if (utf_iscomposing(opndc))
                            {
                                /* When only a composing char is given match at any
                                 * position where that composing char appears. */
                                status = RA_NOMATCH;
                                for (int i = 0; reginput.at(i) != NUL; i += us_ptr2len(reginput.plus(i)))
                                {
                                    int inpc = us_ptr2char(reginput.plus(i));
                                    if (!utf_iscomposing(inpc))
                                    {
                                        if (0 < i)
                                            break;
                                    }
                                    else if (opndc == inpc)
                                    {
                                        /* Include all following composing chars. */
                                        len = i + us_ptr2len_cc(reginput.plus(i));
                                        status = RA_MATCH;
                                        break;
                                    }
                                }
                            }
                            else
                            {
                                for (int i = 0; i < len; i++)
                                    if (opnd.at(i) != reginput.at(i))
                                    {
                                        status = RA_NOMATCH;
                                        break;
                                    }
                            }

                            reginput = reginput.plus(len);
                            break;
                        }

                        case RE_COMPOSING:
                        {
                            /* Skip composing characters. */
                            while (utf_iscomposing(us_ptr2char(reginput)))
                                reginput = reginput.plus(us_ptr2len(reginput));
                            break;
                        }

                        case NOTHING:
                            break;

                        case BACK:
                        {
                            /*
                             * When we run into BACK we need to check if we don't keep
                             * looping without matching any input.  The second and later
                             * times a BACK is encountered it fails if the input is still
                             * at the same position as the previous time.
                             * The positions are stored in "backpos" and found by the
                             * current value of "scan", the position in the RE program.
                             */
                            backpos_C[] bpp = backpos.ga_data;

                            int i;
                            for (i = 0; i < backpos.ga_len; i++)
                                if (BEQ(bpp[i].bp_scan, scan))
                                    break;
                            if (i == backpos.ga_len)
                            {
                                /* First time at this BACK, make room to store the pos. */
                                bpp = backpos.ga_grow(1);
                                bpp[i] = new backpos_C();
                                bpp[i].bp_scan = scan;
                                backpos.ga_len++;
                            }
                            else if (reg_save_equal(bpp[i].bp_pos))
                                /* Still at same position as last time, fail. */
                                status = RA_NOMATCH;

                            if (status != RA_FAIL && status != RA_NOMATCH)
                                reg_save(bpp[i].bp_pos, backpos);

                            break;
                        }

                        case MOPEN + 0:     /* Match start: \zs */
                        case MOPEN + 1:     /* \( */
                        case MOPEN + 2:
                        case MOPEN + 3:
                        case MOPEN + 4:
                        case MOPEN + 5:
                        case MOPEN + 6:
                        case MOPEN + 7:
                        case MOPEN + 8:
                        case MOPEN + 9:
                        {
                            int no = op - MOPEN;
                            cleanup_subexpr();
                            regitem_C rip = push_regitem(RS_MOPEN, scan);
                            if (rip == null)
                                status = RA_FAIL;
                            else
                            {
                                rip.rs_no = no;
                                reg_startp[no] = save_se(rip.rs_sesave, reg_startpos[no], reg_startp[no]);
                                /* We simply continue and handle the result when done. */
                            }
                            break;
                        }

                        case NOPEN:         /* \%( */
                        case NCLOSE:        /* \) after \%( */
                            if (push_regitem(RS_NOPEN, scan) == null)
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
                            int no = op - ZOPEN;
                            cleanup_zsubexpr();
                            regitem_C rip = push_regitem(RS_ZOPEN, scan);
                            if (rip == null)
                                status = RA_FAIL;
                            else
                            {
                                rip.rs_no = no;
                                reg_startzp[no] = save_se(rip.rs_sesave, reg_startzpos[no], reg_startzp[no]);
                                /* We simply continue and handle the result when done. */
                            }
                            break;
                        }

                        case MCLOSE + 0:    /* Match end: \ze */
                        case MCLOSE + 1:    /* \) */
                        case MCLOSE + 2:
                        case MCLOSE + 3:
                        case MCLOSE + 4:
                        case MCLOSE + 5:
                        case MCLOSE + 6:
                        case MCLOSE + 7:
                        case MCLOSE + 8:
                        case MCLOSE + 9:
                        {
                            int no = op - MCLOSE;
                            cleanup_subexpr();
                            regitem_C rip = push_regitem(RS_MCLOSE, scan);
                            if (rip == null)
                                status = RA_FAIL;
                            else
                            {
                                rip.rs_no = no;
                                reg_endp[no] = save_se(rip.rs_sesave, reg_endpos[no], reg_endp[no]);
                                /* We simply continue and handle the result when done. */
                            }
                            break;
                        }

                        case ZCLOSE + 1:    /* \) after \z( */
                        case ZCLOSE + 2:
                        case ZCLOSE + 3:
                        case ZCLOSE + 4:
                        case ZCLOSE + 5:
                        case ZCLOSE + 6:
                        case ZCLOSE + 7:
                        case ZCLOSE + 8:
                        case ZCLOSE + 9:
                        {
                            int no = op - ZCLOSE;
                            cleanup_zsubexpr();
                            regitem_C rip = push_regitem(RS_ZCLOSE, scan);
                            if (rip == null)
                                status = RA_FAIL;
                            else
                            {
                                rip.rs_no = no;
                                reg_endzp[no] = save_se(rip.rs_sesave, reg_endzpos[no], reg_endzp[no]);
                                /* We simply continue and handle the result when done. */
                            }
                            break;
                        }

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
                            int[] len = new int[1];

                            int no = op - BACKREF;
                            cleanup_subexpr();
                            if (!(reg_match == null))       /* Single-line regexp */
                            {
                                if (reg_startp[no] == null || reg_endp[no] == null)
                                {
                                    /* Backref was not set: Match an empty string. */
                                    len[0] = 0;
                                }
                                else
                                {
                                    /* Compare current input with back-ref in the same line. */
                                    len[0] = BDIFF(reg_endp[no], reg_startp[no]);
                                    if (cstrncmp(reg_startp[no], reginput, len) != 0)
                                        status = RA_NOMATCH;
                                }
                            }
                            else                            /* Multi-line regexp */
                            {
                                if (reg_startpos[no].lnum < 0 || reg_endpos[no].lnum < 0)
                                {
                                    /* Backref was not set: Match an empty string. */
                                    len[0] = 0;
                                }
                                else
                                {
                                    if (reg_startpos[no].lnum == reglnum && reg_endpos[no].lnum == reglnum)
                                    {
                                        /* Compare back-ref within the current line. */
                                        len[0] = reg_endpos[no].col - reg_startpos[no].col;
                                        if (cstrncmp(regline.plus(reg_startpos[no].col), reginput, len) != 0)
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
                                                        len);

                                        if (r != RA_MATCH)
                                            status = r;
                                    }
                                }
                            }

                            /* Matched the backref, skip over it. */
                            reginput = reginput.plus(len[0]);
                            break;
                        }

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
                            cleanup_zsubexpr();
                            int no = op - ZREF;
                            if (re_extmatch_in != null && re_extmatch_in.matches[no] != null)
                            {
                                int[] len = { strlen(re_extmatch_in.matches[no]) };
                                if (cstrncmp(re_extmatch_in.matches[no], reginput, len) != 0)
                                    status = RA_NOMATCH;
                                else
                                    reginput = reginput.plus(len[0]);
                            }
                            else
                            {
                                /* Backref was not set: Match an empty string. */
                            }
                            break;
                        }

                        case BRANCH:
                        {
                            if (re_op(next) != BRANCH) /* No choice. */
                                next = operand(scan);       /* Avoid recursion. */
                            else
                            {
                                regitem_C rip = push_regitem(RS_BRANCH, scan);
                                if (rip == null)
                                    status = RA_FAIL;
                                else
                                    status = RA_BREAK;      /* rest is below */
                            }
                            break;
                        }

                        case BRACE_LIMITS:
                        {
                            if (re_op(next) == BRACE_SIMPLE)
                            {
                                bl_minval = operand_min(scan);
                                bl_maxval = operand_max(scan);
                            }
                            else if (BRACE_COMPLEX <= re_op(next) && re_op(next) < BRACE_COMPLEX + 10)
                            {
                                int no = re_op(next) - BRACE_COMPLEX;
                                brace_min[no] = operand_min(scan);
                                brace_max[no] = operand_max(scan);
                                brace_count[no] = 0;
                            }
                            else
                            {
                                emsg(e_internal);       /* Shouldn't happen. */
                                status = RA_FAIL;
                            }
                            break;
                        }

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
                            int no = op - BRACE_COMPLEX;
                            ++brace_count[no];

                            /* If not matched enough times yet, try one more. */
                            if (brace_count[no] <= (brace_min[no] <= brace_max[no] ? brace_min[no] : brace_max[no]))
                            {
                                regitem_C rip = push_regitem(RS_BRCPLX_MORE, scan);
                                if (rip == null)
                                    status = RA_FAIL;
                                else
                                {
                                    rip.rs_no = no;
                                    reg_save(rip.rs_regsave, backpos);
                                    next = operand(scan);
                                    /* We continue and handle the result when done. */
                                }
                                break;
                            }

                            /* If matched enough times, may try matching some more. */
                            if (brace_min[no] <= brace_max[no])
                            {
                                /* Range is the normal way around, use longest match. */
                                if (brace_count[no] <= brace_max[no])
                                {
                                    regitem_C rip = push_regitem(RS_BRCPLX_LONG, scan);
                                    if (rip == null)
                                        status = RA_FAIL;
                                    else
                                    {
                                        rip.rs_no = no;
                                        reg_save(rip.rs_regsave, backpos);
                                        next = operand(scan);
                                        /* We continue and handle the result when done. */
                                    }
                                }
                            }
                            else
                            {
                                /* Range is backwards, use shortest match first. */
                                if (brace_count[no] <= brace_min[no])
                                {
                                    regitem_C rip = push_regitem(RS_BRCPLX_SHORT, scan);
                                    if (rip == null)
                                        status = RA_FAIL;
                                    else
                                    {
                                        reg_save(rip.rs_regsave, backpos);
                                        /* We continue and handle the result when done. */
                                    }
                                }
                            }
                            break;
                        }

                        case BRACE_SIMPLE:
                        case STAR:
                        case PLUS:
                        {
                            regstar_C rst = new regstar_C();

                            /*
                             * Lookahead to avoid useless match attempts when we know
                             * what character comes next.
                             */
                            if (re_op(next) == EXACTLY)
                            {
                                rst.nextb = operand(next).at(0);
                                if (ireg_ic)
                                {
                                    if (utf_isupper(rst.nextb))
                                        rst.nextb_ic = utf_tolower(rst.nextb);
                                    else
                                        rst.nextb_ic = utf_toupper(rst.nextb);
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
                             * When maxval > minval, try matching as much as possible, up to maxval.
                             * When maxval < minval, try matching at least the minimal number
                             * (since the range is backwards, that's also maxval!).
                             */
                            rst.count = regrepeat(operand(scan), rst.maxval);
                            if (got_int)
                            {
                                status = RA_FAIL;
                                break;
                            }

                            if (rst.minval <= rst.maxval ? rst.minval <= rst.count : rst.maxval <= rst.count)
                            {
                                /* It could match.  Prepare for trying to match
                                 * what follows.  The code is below.  Parameters
                                 * are stored in a regstar_C on the regstack. */
                                if (p_mmp[0] <= (regstack.ga_len >>> 10))
                                {
                                    emsg(e_maxmempat);
                                    status = RA_FAIL;
                                }
                                else
                                {
                                    regstack.ga_grow(1);
                                    regstack.ga_data[regstack.ga_len++] = rst;
                                    rst = null;

                                    regitem_C rip = push_regitem(rst.minval <= rst.maxval ? RS_STAR_LONG : RS_STAR_SHORT, scan);
                                    if (rip == null)
                                        status = RA_FAIL;
                                    else
                                        status = RA_BREAK;      /* skip the restore bits */
                                }
                            }
                            else
                                status = RA_NOMATCH;

                            break;
                        }

                        case NOMATCH:
                        case MATCH:
                        case SUBPAT:
                        {
                            regitem_C rip = push_regitem(RS_NOMATCH, scan);
                            if (rip == null)
                                status = RA_FAIL;
                            else
                            {
                                rip.rs_no = op;
                                reg_save(rip.rs_regsave, backpos);
                                next = operand(scan);
                                /* We continue and handle the result when done. */
                            }
                            break;
                        }

                        case BEHIND:
                        case NOBEHIND:
                        {
                            /* Need a bit of room to store extra positions. */
                            if (p_mmp[0] <= (regstack.ga_len >>> 10))
                            {
                                emsg(e_maxmempat);
                                status = RA_FAIL;
                            }
                            else
                            {
                                regbehind_C rbp = new regbehind_C();

                                regstack.ga_grow(1);
                                regstack.ga_data[regstack.ga_len++] = rbp;

                                regitem_C rip = push_regitem(RS_BEHIND1, scan);
                                if (rip == null)
                                    status = RA_FAIL;
                                else
                                {
                                    /* Need to save the subexpr to be able to restore them
                                     * when there is a match but we don't use it. */
                                    save_subexpr(rbp);

                                    rip.rs_no = op;
                                    reg_save(rip.rs_regsave, backpos);
                                    /* First try if what follows matches.
                                     * If it does, then we check the behind match by looping. */
                                }
                            }
                            break;
                        }

                        case BHPOS:
                        {
                            if (reg_match == null)
                            {
                                if (behind_pos.rs_pos.col != BDIFF(reginput, regline)
                                        || behind_pos.rs_pos.lnum != reglnum)
                                    status = RA_NOMATCH;
                            }
                            else if (BNE(behind_pos.rs_ptr, reginput))
                                status = RA_NOMATCH;
                            break;
                        }

                        case NEWL:
                        {
                            if ((c != NUL || !(reg_match == null) || reg_maxline < reglnum || reg_line_lbr)
                                    && (c != '\n' || !reg_line_lbr))
                                status = RA_NOMATCH;
                            else if (reg_line_lbr)
                                reginput = reginput.plus(us_ptr2len_cc(reginput));
                            else
                                reg_nextline();
                            break;
                        }

                        case END:
                            status = RA_MATCH;  /* Success! */
                            break;

                        default:
                            emsg(e_re_corr);
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
             * If there is something on the regstack, execute the code for the state.
             * If the state is popped then loop and use the older state.
             */
            while (0 < regstack.ga_len && status != RA_FAIL)
            {
                Object vip = (1 < regstack.ga_len) ? regstack.ga_data[regstack.ga_len - 2] : null;
                regitem_C rip = (regitem_C)regstack.ga_data[regstack.ga_len - 1];

                switch (rip.rs_state)
                {
                    case RS_NOPEN:
                        /* Result is passed on as-is, simply pop the state. */
                        scan = pop_regitem();
                        break;

                    case RS_MOPEN:
                        /* Pop the state.  Restore pointers when there is no match. */
                        if (status == RA_NOMATCH)
                        {
                            reg_startp[rip.rs_no] = restore_se(rip.rs_sesave, reg_startpos[rip.rs_no], reg_startp[rip.rs_no]);
                        }
                        scan = pop_regitem();
                        break;

                    case RS_ZOPEN:
                        /* Pop the state.  Restore pointers when there is no match. */
                        if (status == RA_NOMATCH)
                        {
                            reg_startzp[rip.rs_no] = restore_se(rip.rs_sesave, reg_startzpos[rip.rs_no], reg_startzp[rip.rs_no]);
                        }
                        scan = pop_regitem();
                        break;

                    case RS_MCLOSE:
                        /* Pop the state.  Restore pointers when there is no match. */
                        if (status == RA_NOMATCH)
                        {
                            reg_endp[rip.rs_no] = restore_se(rip.rs_sesave, reg_endpos[rip.rs_no], reg_endp[rip.rs_no]);
                        }
                        scan = pop_regitem();
                        break;

                    case RS_ZCLOSE:
                        /* Pop the state.  Restore pointers when there is no match. */
                        if (status == RA_NOMATCH)
                        {
                            reg_endzp[rip.rs_no] = restore_se(rip.rs_sesave, reg_endzpos[rip.rs_no], reg_endzp[rip.rs_no]);
                        }
                        scan = pop_regitem();
                        break;

                    case RS_BRANCH:
                    {
                        if (status == RA_MATCH)
                            /* this branch matched, use it */
                            scan = pop_regitem();
                        else
                        {
                            if (status != RA_BREAK)
                            {
                                /* After a non-matching branch: try next one. */
                                reg_restore(rip.rs_regsave, backpos);
                                scan = rip.rs_scan;
                            }
                            if (scan == null || re_op(scan) != BRANCH)
                            {
                                /* no more branches, didn't find a match */
                                status = RA_NOMATCH;
                                scan = pop_regitem();
                            }
                            else
                            {
                                /* Prepare to try a branch. */
                                rip.rs_scan = regnext(scan);
                                reg_save(rip.rs_regsave, backpos);
                                scan = operand(scan);
                            }
                        }
                        break;
                    }

                    case RS_BRCPLX_MORE:
                        /* Pop the state.  Restore pointers when there is no match. */
                        if (status == RA_NOMATCH)
                        {
                            reg_restore(rip.rs_regsave, backpos);
                            --brace_count[rip.rs_no];       /* decrement match count */
                        }
                        scan = pop_regitem();
                        break;

                    case RS_BRCPLX_LONG:
                        /* Pop the state.  Restore pointers when there is no match. */
                        if (status == RA_NOMATCH)
                        {
                            /* There was no match, but we did find enough matches. */
                            reg_restore(rip.rs_regsave, backpos);
                            --brace_count[rip.rs_no];
                            /* continue with the items after "\{}" */
                            status = RA_CONT;
                        }
                        scan = pop_regitem();
                        if (status == RA_CONT)
                            scan = regnext(scan);
                        break;

                    case RS_BRCPLX_SHORT:
                        /* Pop the state.  Restore pointers when there is no match. */
                        if (status == RA_NOMATCH)
                            /* There was no match, try to match one more item. */
                            reg_restore(rip.rs_regsave, backpos);
                        scan = pop_regitem();
                        if (status == RA_NOMATCH)
                        {
                            scan = operand(scan);
                            status = RA_CONT;
                        }
                        break;

                    case RS_NOMATCH:
                        /* Pop the state.  If the operand matches for NOMATCH or
                         * doesn't match for MATCH/SUBPAT, we fail.  Otherwise backup,
                         * except for SUBPAT, and continue with the next item. */
                        if (status == (rip.rs_no == NOMATCH ? RA_MATCH : RA_NOMATCH))
                            status = RA_NOMATCH;
                        else
                        {
                            status = RA_CONT;
                            if (rip.rs_no != SUBPAT)        /* zero-width */
                                reg_restore(rip.rs_regsave, backpos);
                        }
                        scan = pop_regitem();
                        if (status == RA_CONT)
                            scan = regnext(scan);
                        break;

                    case RS_BEHIND1:
                        if (status == RA_NOMATCH)
                        {
                            scan = pop_regitem();
                            drop_regbehind();
                        }
                        else
                        {
                            /* The stuff after BEHIND/NOBEHIND matches.
                             * Now try if the behind part does (not) match before the current
                             * position in the input.  This must be done at every position in the
                             * input and checking if the match ends at the current position.
                             */

                            /* save the position after the found match for next */
                            reg_save(((regbehind_C)vip).save_after, backpos);

                            /* Start looking for a match with operand at the current position.
                             * Go back one character until we find the result, hitting the start
                             * of the line or the previous line (for multi-line matching).
                             * Set behind_pos to where the match should end, BHPOS will match it.
                             * Save the current value. */
                            COPY_regsave(((regbehind_C)vip).save_behind, behind_pos);
                            COPY_regsave(behind_pos, rip.rs_regsave);

                            rip.rs_state = RS_BEHIND2;

                            reg_restore(rip.rs_regsave, backpos);
                            scan = operand(rip.rs_scan).plus(4);
                        }
                        break;

                    case RS_BEHIND2:
                        /*
                         * Looping for BEHIND / NOBEHIND match.
                         */
                        if (status == RA_MATCH && reg_save_equal(behind_pos))
                        {
                            /* found a match that ends where "next" started */
                            COPY_regsave(behind_pos, ((regbehind_C)vip).save_behind);
                            if (rip.rs_no == BEHIND)
                                reg_restore(((regbehind_C)vip).save_after, backpos);
                            else
                            {
                                /* But we didn't want a match.  Need to restore the subexpr,
                                 * because what follows matched, so they have been set. */
                                status = RA_NOMATCH;
                                restore_subexpr((regbehind_C)vip);
                            }
                            scan = pop_regitem();
                            drop_regbehind();
                        }
                        else
                        {
                            /* No match or a match that doesn't end where we want it:
                             * go back one character.  May go to previous line once. */
                            boolean no = true;
                            long limit = operand_min(rip.rs_scan);
                            if (reg_match == null)
                            {
                                if (0 < limit
                                        && ((rip.rs_regsave.rs_pos.lnum < behind_pos.rs_pos.lnum
                                            ? strlen(regline)
                                            : behind_pos.rs_pos.col)
                                        - rip.rs_regsave.rs_pos.col >= limit))
                                    no = false;
                                else if (rip.rs_regsave.rs_pos.col == 0)
                                {
                                    if (rip.rs_regsave.rs_pos.lnum < behind_pos.rs_pos.lnum
                                            || reg_getline(--rip.rs_regsave.rs_pos.lnum) == null)
                                        no = false;
                                    else
                                    {
                                        reg_restore(rip.rs_regsave, backpos);
                                        rip.rs_regsave.rs_pos.col = strlen(regline);
                                    }
                                }
                                else
                                {
                                    rip.rs_regsave.rs_pos.col -= us_head_off(regline, regline.plus(rip.rs_regsave.rs_pos.col - 1)) + 1;
                                }
                            }
                            else
                            {
                                if (BEQ(rip.rs_regsave.rs_ptr, regline))
                                    no = false;
                                else
                                {
                                    rip.rs_regsave.rs_ptr = rip.rs_regsave.rs_ptr.minus(us_ptr_back(regline, rip.rs_regsave.rs_ptr));
                                    if (0 < limit && limit < BDIFF(behind_pos.rs_ptr, rip.rs_regsave.rs_ptr))
                                        no = false;
                                }
                            }
                            if (no == true)
                            {
                                /* Advanced, prepare for finding match again. */
                                reg_restore(rip.rs_regsave, backpos);
                                scan = operand(rip.rs_scan).plus(4);
                                if (status == RA_MATCH)
                                {
                                    /* We did match, so subexpr may have been changed,
                                     * need to restore them for the next try. */
                                    status = RA_NOMATCH;
                                    restore_subexpr((regbehind_C)vip);
                                }
                            }
                            else
                            {
                                /* Can't advance.  For NOBEHIND that's a match. */
                                COPY_regsave(behind_pos, ((regbehind_C)vip).save_behind);
                                if (rip.rs_no == NOBEHIND)
                                {
                                    reg_restore(((regbehind_C)vip).save_after, backpos);
                                    status = RA_MATCH;
                                }
                                else
                                {
                                    /* We do want a proper match.  Need to restore the subexpr
                                     * if we had a match, because they may have been set. */
                                    if (status == RA_MATCH)
                                    {
                                        status = RA_NOMATCH;
                                        restore_subexpr((regbehind_C)vip);
                                    }
                                }
                                scan = pop_regitem();
                                drop_regbehind();
                            }
                        }
                        break;

                    case RS_STAR_LONG:
                    case RS_STAR_SHORT:
                    {
                        regstar_C rst = (regstar_C)vip;

                        if (status == RA_MATCH)
                        {
                            scan = pop_regitem();
                            drop_regstar();
                            break;
                        }

                        /* Tried once already, restore input pointers. */
                        if (status != RA_BREAK)
                            reg_restore(rip.rs_regsave, backpos);

                        /* Repeat until we found a position where it could match. */
                        for ( ; ; )
                        {
                            if (status != RA_BREAK)
                            {
                                /* Tried first position already, advance. */
                                if (rip.rs_state == RS_STAR_LONG)
                                {
                                    /* Trying for longest match, but couldn't
                                     * or didn't match -- back up one char. */
                                    if (--rst.count < rst.minval)
                                        break;
                                    if (BEQ(reginput, regline))
                                    {
                                        /* backup to last char of previous line */
                                        --reglnum;
                                        regline = reg_getline(reglnum);
                                        /* Just in case regrepeat() didn't count right. */
                                        if (regline == null)
                                            break;
                                        reginput = regline.plus(strlen(regline));
                                        fast_breakcheck();
                                    }
                                    else
                                        reginput = reginput.minus(us_ptr_back(regline, reginput));
                                }
                                else
                                {
                                    /* Range is backwards, use shortest match first.
                                     * Careful: maxval and minval are exchanged!
                                     * Couldn't or didn't match: try advancing one char. */
                                    if (rst.count == rst.minval || regrepeat(operand(rip.rs_scan), 1L) == 0)
                                        break;
                                    rst.count++;
                                }
                                if (got_int)
                                    break;
                            }
                            else
                                status = RA_NOMATCH;

                            /* If it could match, try it. */
                            if (rst.nextb == NUL || reginput.at(0) == rst.nextb || reginput.at(0) == rst.nextb_ic)
                            {
                                reg_save(rip.rs_regsave, backpos);
                                scan = regnext(rip.rs_scan);
                                status = RA_CONT;
                                break;
                            }
                        }
                        if (status != RA_CONT)
                        {
                            /* Failed. */
                            scan = pop_regitem();
                            drop_regstar();
                            status = RA_NOMATCH;
                        }
                    }
                    break;
                }

                /* If we want to continue the inner loop or didn't pop a state continue matching loop. */
                if (status == RA_CONT || rip == (regitem_C)regstack.ga_data[regstack.ga_len - 1])
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
                if (scan == null)
                {
                    /*
                     * We get here only if there's trouble -- normally
                     * "case END" is the terminating point.
                     */
                    emsg(e_re_corr);
                }
                if (status == RA_FAIL)
                    got_int = true;
                return (status == RA_MATCH);
            }
        }

        /* NOTREACHED */
    }

    /*
     * Push an item onto the regstack.
     * Returns pointer to new item.  Returns null when out of memory.
     */
    /*private*/ static regitem_C push_regitem(int state, Bytes scan)
    {
        if (p_mmp[0] <= (regstack.ga_len >>> 10))
        {
            emsg(e_maxmempat);
            return null;
        }

        regitem_C rip = new regitem_C();

        regstack.ga_grow(1);
        regstack.ga_data[regstack.ga_len++] = rip;

        rip.rs_state = state;
        rip.rs_scan = scan;

        return rip;
    }

    /*
     * Pop an item from the regstack.
     */
    /*private*/ static Bytes pop_regitem()
    {
        regitem_C rip = (regitem_C)regstack.ga_data[--regstack.ga_len];
        regstack.ga_data[regstack.ga_len] = null;

        return rip.rs_scan;
    }

    /*private*/ static void drop_regbehind()
    {
        regstack.ga_data[--regstack.ga_len] = null;
    }

    /*private*/ static void drop_regstar()
    {
        regstack.ga_data[--regstack.ga_len] = null;
    }

    /*
     * regrepeat - repeatedly match something simple, return how many.
     * Advances reginput (and reglnum) to just after the matched chars.
     */
    /*private*/ static int regrepeat(Bytes p, long maxcount)
        /* maxcount: maximum number of matches allowed */
    {
        long count = 0;
        int testval = 0;
        int mask;

        Bytes scan = reginput;     /* Make local copy of reginput for speed. */
        Bytes opnd = operand(p);

        do_class:
        {
            switch (re_op(p))
            {
                case ANY:
                case ANY + ADD_NL:
                    while (count < maxcount)
                    {
                        /* Matching anything means we continue until end-of-line (or
                         * end-of-file for ANY + ADD_NL), only limited by maxcount. */
                        while (scan.at(0) != NUL && count < maxcount)
                        {
                            count++;
                            scan = scan.plus(us_ptr2len_cc(scan));
                        }
                        if (reg_match != null || !with_nl(re_op(p)) || reg_maxline < reglnum || reg_line_lbr || count == maxcount)
                            break;
                        count++;                /* count the line-break */
                        reg_nextline();
                        scan = reginput;
                        if (got_int)
                            break;
                    }
                    break do_class;

                case IDENT:
                case IDENT + ADD_NL:
                    testval = TRUE;
                    /* FALLTHROUGH */
                case SIDENT:
                case SIDENT + ADD_NL:
                    while (count < maxcount)
                    {
                        if (vim_isIDc(us_ptr2char(scan)) && (testval != 0 || !asc_isdigit(scan.at(0))))
                        {
                            scan = scan.plus(us_ptr2len_cc(scan));
                        }
                        else if (scan.at(0) == NUL)
                        {
                            if (!(reg_match == null) || !with_nl(re_op(p)) || reg_maxline < reglnum || reg_line_lbr)
                                break;
                            reg_nextline();
                            scan = reginput;
                            if (got_int)
                                break;
                        }
                        else if (reg_line_lbr && scan.at(0) == (byte)'\n' && with_nl(re_op(p)))
                            scan = scan.plus(1);
                        else
                            break;
                        count++;
                    }
                    break do_class;

                case KWORD:
                case KWORD + ADD_NL:
                    testval = TRUE;
                    /* FALLTHROUGH */
                case SKWORD:
                case SKWORD + ADD_NL:
                    while (count < maxcount)
                    {
                        if (us_iswordp(scan, reg_buf) && (testval != 0 || !asc_isdigit(scan.at(0))))
                        {
                            scan = scan.plus(us_ptr2len_cc(scan));
                        }
                        else if (scan.at(0) == NUL)
                        {
                            if (!(reg_match == null) || !with_nl(re_op(p)) || reg_maxline < reglnum || reg_line_lbr)
                                break;
                            reg_nextline();
                            scan = reginput;
                            if (got_int)
                                break;
                        }
                        else if (reg_line_lbr && scan.at(0) == (byte)'\n' && with_nl(re_op(p)))
                            scan = scan.plus(1);
                        else
                            break;
                        count++;
                    }
                    break do_class;

                case FNAME:
                case FNAME + ADD_NL:
                    testval = TRUE;
                    /* FALLTHROUGH */
                case SFNAME:
                case SFNAME + ADD_NL:
                    while (count < maxcount)
                    {
                        if (vim_isfilec(us_ptr2char(scan)) && (testval != 0 || !asc_isdigit(scan.at(0))))
                        {
                            scan = scan.plus(us_ptr2len_cc(scan));
                        }
                        else if (scan.at(0) == NUL)
                        {
                            if (!(reg_match == null) || !with_nl(re_op(p)) || reg_maxline < reglnum || reg_line_lbr)
                                break;
                            reg_nextline();
                            scan = reginput;
                            if (got_int)
                                break;
                        }
                        else if (reg_line_lbr && scan.at(0) == (byte)'\n' && with_nl(re_op(p)))
                            scan = scan.plus(1);
                        else
                            break;
                        count++;
                    }
                    break do_class;

                case PRINT:
                case PRINT + ADD_NL:
                    testval = TRUE;
                    /* FALLTHROUGH */
                case SPRINT:
                case SPRINT + ADD_NL:
                    while (count < maxcount)
                    {
                        if (scan.at(0) == NUL)
                        {
                            if (!(reg_match == null) || !with_nl(re_op(p)) || reg_maxline < reglnum || reg_line_lbr)
                                break;
                            reg_nextline();
                            scan = reginput;
                            if (got_int)
                                break;
                        }
                        else if (vim_isprintc(us_ptr2char(scan)) && (testval != 0 || !asc_isdigit(scan.at(0))))
                        {
                            scan = scan.plus(us_ptr2len_cc(scan));
                        }
                        else if (reg_line_lbr && scan.at(0) == (byte)'\n' && with_nl(re_op(p)))
                            scan = scan.plus(1);
                        else
                            break;
                        count++;
                    }
                    break do_class;

                case WHITE:
                case WHITE + ADD_NL:
                    testval = mask = RI_WHITE;
                    break;

                case NWHITE:
                case NWHITE + ADD_NL:
                    mask = RI_WHITE;
                    break;

                case DIGIT:
                case DIGIT + ADD_NL:
                    testval = mask = RI_DIGIT;
                    break;

                case NDIGIT:
                case NDIGIT + ADD_NL:
                    mask = RI_DIGIT;
                    break;

                case HEX:
                case HEX + ADD_NL:
                    testval = mask = RI_HEX;
                    break;

                case NHEX:
                case NHEX + ADD_NL:
                    mask = RI_HEX;
                    break;

                case OCTAL:
                case OCTAL + ADD_NL:
                    testval = mask = RI_OCTAL;
                    break;

                case NOCTAL:
                case NOCTAL + ADD_NL:
                    mask = RI_OCTAL;
                    break;

                case WORD:
                case WORD + ADD_NL:
                    testval = mask = RI_WORD;
                    break;

                case NWORD:
                case NWORD + ADD_NL:
                    mask = RI_WORD;
                    break;

                case HEAD:
                case HEAD + ADD_NL:
                    testval = mask = RI_HEAD;
                    break;

                case NHEAD:
                case NHEAD + ADD_NL:
                    mask = RI_HEAD;
                    break;

                case ALPHA:
                case ALPHA + ADD_NL:
                    testval = mask = RI_ALPHA;
                    break;

                case NALPHA:
                case NALPHA + ADD_NL:
                    mask = RI_ALPHA;
                    break;

                case LOWER:
                case LOWER + ADD_NL:
                    testval = mask = RI_LOWER;
                    break;

                case NLOWER:
                case NLOWER + ADD_NL:
                    mask = RI_LOWER;
                    break;

                case UPPER:
                case UPPER + ADD_NL:
                    testval = mask = RI_UPPER;
                    break;

                case NUPPER:
                case NUPPER + ADD_NL:
                    mask = RI_UPPER;
                    break;

                case EXACTLY:
                {
                    /* This doesn't do a multi-byte character, because a MULTIBYTECODE would have
                     * been used for it.  It does handle single-byte characters, such as latin1. */
                    if (ireg_ic)
                    {
                        int cu = utf_toupper(opnd.at(0));
                        int cl = utf_tolower(opnd.at(0));
                        while (count < maxcount && (scan.at(0) == cu || scan.at(0) == cl))
                        {
                            count++;
                            scan = scan.plus(1);
                        }
                    }
                    else
                    {
                        int cu = opnd.at(0);
                        while (count < maxcount && scan.at(0) == cu)
                        {
                            count++;
                            scan = scan.plus(1);
                        }
                    }
                    break do_class;
                }

                case MULTIBYTECODE:
                {
                    /* Safety check (just in case 'encoding' was changed since compiling the program). */
                    int len = us_ptr2len_cc(opnd);
                    if (1 < len)
                    {
                        int cf = 0;
                        if (ireg_ic)
                            cf = utf_fold(us_ptr2char(opnd));
                        while (count < maxcount)
                        {
                            int i;
                            for (i = 0; i < len; i++)
                                if (opnd.at(i) != scan.at(i))
                                    break;
                            if (i < len && (!ireg_ic || utf_fold(us_ptr2char(scan)) != cf))
                                break;
                            scan = scan.plus(len);
                            count++;
                        }
                    }
                    break do_class;
                }

                case ANYOF:
                case ANYOF + ADD_NL:
                    testval = TRUE;
                    /* FALLTHROUGH */
                case ANYBUT:
                case ANYBUT + ADD_NL:
                    while (count < maxcount)
                    {
                        int len;
                        if (scan.at(0) == NUL)
                        {
                            if (!(reg_match == null) || !with_nl(re_op(p)) || reg_maxline < reglnum || reg_line_lbr)
                                break;
                            reg_nextline();
                            scan = reginput;
                            if (got_int)
                                break;
                        }
                        else if (reg_line_lbr && scan.at(0) == (byte)'\n' && with_nl(re_op(p)))
                            scan = scan.plus(1);
                        else if (1 < (len = us_ptr2len_cc(scan)))
                        {
                            if ((cstrchr(opnd, us_ptr2char(scan)) == null) == (testval != 0))
                                break;
                            scan = scan.plus(len);
                        }
                        else
                        {
                            if ((cstrchr(opnd, scan.at(0)) == null) == (testval != 0))
                                break;
                            scan = scan.plus(1);
                        }
                        count++;
                    }
                    break do_class;

                case NEWL:
                    while (count < maxcount
                        && ((scan.at(0) == NUL && reglnum <= reg_maxline && !reg_line_lbr && reg_match == null)
                            || (scan.at(0) == (byte)'\n' && reg_line_lbr)))
                    {
                        count++;
                        if (reg_line_lbr)
                            reginput = reginput.plus(us_ptr2len_cc(reginput));
                        else
                            reg_nextline();
                        scan = reginput;
                        if (got_int)
                            break;
                    }
                    break do_class;

                default:                /* Oh dear.  Called inappropriately. */
                    emsg(e_re_corr);
                    break do_class;
            }

            while (count < maxcount)
            {
                int l;
                if (scan.at(0) == NUL)
                {
                    if (reg_match != null || !with_nl(re_op(p)) || reg_maxline < reglnum || reg_line_lbr)
                        break;
                    reg_nextline();
                    scan = reginput;
                    if (got_int)
                        break;
                }
                else if (1 < (l = us_ptr2len_cc(scan)))
                {
                    if (testval != 0)
                        break;
                    scan = scan.plus(l);
                }
                else if ((class_tab[char_u(scan.at(0))] & mask) == testval)
                    scan = scan.plus(1);
                else if (reg_line_lbr && scan.at(0) == (byte)'\n' && with_nl(re_op(p)))
                    scan = scan.plus(1);
                else
                    break;
                count++;
            }
        }

        reginput = scan;

        return (int)count;
    }

    /*
     * Dig the "next" pointer out of a node.
     * Returns null when calculating size, when there is no next item and when there is an error.
     */
    /*private*/ static Bytes regnext(Bytes p)
    {
        if (p == JUST_CALC_SIZE || reg_toolong)
            return null;

        int offset = re_next(p);
        if (offset == 0)
            return null;

        if (re_op(p) == BACK)
            return p.minus(offset);
        else
            return p.plus(offset);
    }

    /*
     * Check the regexp program for its magic number.
     * Return true if it's wrong.
     */
    /*private*/ static boolean prog_magic_wrong()
    {
        regprog_C prog = (reg_match == null) ? reg_mmatch.regprog : reg_match.regprog;
        if (prog.engine == nfa_regengine)
            /* For NFA matcher we don't check the magic. */
            return false;

        if (((bt_regprog_C)prog).program.at(0) != REGMAGIC)
        {
            emsg(e_re_corr);
            return true;
        }

        return false;
    }

    /*
     * Cleanup the subexpressions, if this wasn't done yet.
     * This construction is used to clear the subexpressions
     * only when they are used (to increase speed).
     */
    /*private*/ static void cleanup_subexpr()
    {
        if (need_clear_subexpr)
        {
            if (reg_match == null)
            {
                for (int i = 0; i < NSUBEXP; i++)
                {
                    /* Use 0xff to set lnum to -1. */
                    MIN1_lpos(reg_startpos[i]);
                    MIN1_lpos(reg_endpos[i]);
                }
            }
            else
            {
                for (int i = 0; i < NSUBEXP; i++)
                {
                    reg_startp[i] = null;
                    reg_endp[i] = null;
                }
            }
            need_clear_subexpr = false;
        }
    }

    /*private*/ static void cleanup_zsubexpr()
    {
        if (need_clear_zsubexpr)
        {
            if (reg_match == null)
            {
                for (int i = 0; i < NSUBEXP; i++)
                {
                    /* Use 0xff to set lnum to -1. */
                    MIN1_lpos(reg_startzpos[i]);
                    MIN1_lpos(reg_endzpos[i]);
                }
            }
            else
            {
                for (int i = 0; i < NSUBEXP; i++)
                {
                    reg_startzp[i] = null;
                    reg_endzp[i] = null;
                }
            }
            need_clear_zsubexpr = false;
        }
    }

    /*
     * Save the current subexpr to "bp", so that they can be restored later by restore_subexpr().
     */
    /*private*/ static void save_subexpr(regbehind_C bp)
    {
        /* When "need_clear_subexpr" is set we don't need to save the values,
         * only remember that this flag needs to be set again when restoring. */
        bp.save_need_clear_subexpr = need_clear_subexpr;
        if (!need_clear_subexpr)
        {
            for (int i = 0; i < NSUBEXP; i++)
            {
                if (reg_match == null)
                {
                    COPY_lpos(bp.save_start[i].se_pos, reg_startpos[i]);
                    COPY_lpos(bp.save_end[i].se_pos, reg_endpos[i]);
                }
                else
                {
                    bp.save_start[i].se_ptr = reg_startp[i];
                    bp.save_end[i].se_ptr = reg_endp[i];
                }
            }
        }
    }

    /*
     * Restore the subexpr from "bp".
     */
    /*private*/ static void restore_subexpr(regbehind_C bp)
    {
        /* Only need to restore saved values when they are not to be cleared. */
        need_clear_subexpr = bp.save_need_clear_subexpr;
        if (!need_clear_subexpr)
        {
            for (int i = 0; i < NSUBEXP; i++)
            {
                if (reg_match == null)
                {
                    COPY_lpos(reg_startpos[i], bp.save_start[i].se_pos);
                    COPY_lpos(reg_endpos[i], bp.save_end[i].se_pos);
                }
                else
                {
                    reg_startp[i] = bp.save_start[i].se_ptr;
                    reg_endp[i] = bp.save_end[i].se_ptr;
                }
            }
        }
    }

    /*
     * Advance "reglnum", "regline" and "reginput" to the next line.
     */
    /*private*/ static void reg_nextline()
    {
        regline = reg_getline(++reglnum);
        reginput = regline;
        fast_breakcheck();
    }

    /*
     * Save the input line and position in a regsave_C.
     */
    /*private*/ static void reg_save(regsave_C save, Growing<backpos_C> gap)
    {
        if (reg_match == null)
        {
            save.rs_pos.col = BDIFF(reginput, regline);
            save.rs_pos.lnum = reglnum;
        }
        else
            save.rs_ptr = reginput;
        save.rs_len = gap.ga_len;
    }

    /*
     * Restore the input line and position from a regsave_C.
     */
    /*private*/ static void reg_restore(regsave_C save, Growing<backpos_C> gap)
    {
        if (reg_match == null)
        {
            if (reglnum != save.rs_pos.lnum)
            {
                /* only call reg_getline() when the line number changed to save a bit of time */
                reglnum = save.rs_pos.lnum;
                regline = reg_getline(reglnum);
            }
            reginput = regline.plus(save.rs_pos.col);
        }
        else
            reginput = save.rs_ptr;
        gap.ga_len = save.rs_len;
    }

    /*
     * Return true if current position is equal to saved position.
     */
    /*private*/ static boolean reg_save_equal(regsave_C save)
    {
        if (reg_match == null)
            return (reglnum == save.rs_pos.lnum && BEQ(reginput, regline.plus(save.rs_pos.col)));

        return BEQ(reginput, save.rs_ptr);
    }

    /*
     * Tentatively set the sub-expression start to the current position (after calling regmatch()
     * they will have changed).  Need to save the existing values for when there is no match.
     * Use se_save() to use pointer (save_se_multi()) or position (save_se_one()),
     * depending on REG_MULTI.
     */
    /*private*/ static void save_se_multi(save_se_C savep, lpos_C posp)
    {
        COPY_lpos(savep.se_pos, posp);
        posp.lnum = reglnum;
        posp.col = BDIFF(reginput, regline);
    }

    /*private*/ static Bytes save_se_one(save_se_C savep, Bytes pp)
    {
        savep.se_ptr = pp;
        pp = reginput;
        return pp;
    }

    /* Save the sub-expressions before attempting a match. */
    /*private*/ static Bytes save_se(save_se_C savep, lpos_C posp, Bytes pp)
    {
        if (reg_match == null)
            save_se_multi(savep, posp);
        else
            pp = save_se_one(savep, pp);

        return pp;
    }

    /* After a failed match restore the sub-expressions. */
    /*private*/ static Bytes restore_se(save_se_C savep, lpos_C posp, Bytes pp)
    {
        if (reg_match == null)
            COPY_lpos(posp, savep.se_pos);
        else
            pp = savep.se_ptr;

        return pp;
    }

    /*
     * Compare a number with the operand of RE_LNUM, RE_COL or RE_VCOL.
     */
    /*private*/ static boolean re_num_cmp(long val, Bytes scan)
    {
        long n = operand_min(scan);

        if (operand_cmp(scan) == '>')
            return (val > n);
        if (operand_cmp(scan) == '<')
            return (val < n);

        return (val == n);
    }

    /*
     * Check whether a backreference matches.
     * Returns RA_FAIL, RA_NOMATCH or RA_MATCH.
     * If "bytelen" is not null, it is set to the byte length of the match in the last line.
     */
    /*private*/ static int match_with_backref(long start_lnum, int start_col, long end_lnum, int end_col, int[] bytelen)
    {
        long clnum = start_lnum;
        int ccol = start_col;

        if (bytelen != null)
            bytelen[0] = 0;

        for ( ; ; )
        {
            /* Since getting one line may invalidate the other, need to make copy.
             * Slow! */
            if (BNE(regline, reg_tofree))
            {
                int len = strlen(regline);
                if (reg_tofree == null || reg_tofree_len <= len)
                {
                    len += 50;                              /* get some extra */
                    reg_tofree = new Bytes(len);
                    reg_tofree_len = len;
                }
                STRCPY(reg_tofree, regline);
                reginput = reg_tofree.plus(BDIFF(reginput, regline));
                regline = reg_tofree;
            }

            /* Get the line to compare with. */
            Bytes p = reg_getline(clnum);
            int[] len = new int[1];
            if (clnum == end_lnum)
                len[0] = end_col - ccol;
            else
                len[0] = strlen(p, ccol);

            if (cstrncmp(p.plus(ccol), reginput, len) != 0)
                return RA_NOMATCH;                          /* doesn't match */
            if (bytelen != null)
                bytelen[0] += len[0];
            if (clnum == end_lnum)
                break;                                      /* match and at end! */
            if (reg_maxline <= reglnum)
                return RA_NOMATCH;                          /* text too short */

            /* Advance to next line. */
            reg_nextline();
            if (bytelen != null)
                bytelen[0] = 0;
            clnum++;
            ccol = 0;
            if (got_int)
                return RA_FAIL;
        }

        /* Found a match!
         * Note that regline may now point to a copy of the line, that should not matter. */
        return RA_MATCH;
    }

    /*
     * Used in a place where no * or \+ can follow.
     */
    /*private*/ static boolean re_mult_next(Bytes what)
    {
        if (re_multi_type(peekchr()) == MULTI_MULT)
        {
            emsg2(u8("E888: (NFA regexp) cannot repeat %s"), what);
            rc_did_emsg = true;
            return false;
        }
        return true;
    }

    /*private*/ static final class decomp_C
    {
        int a, b, c;

        /*private*/ decomp_C(int a, int b, int c)
        {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    /* 0xfb20 - 0xfb4f */
    /*private*/ static decomp_C[/*0xfb4f - 0xfb20 + 1*/] decomp_table =
    {
        new decomp_C(0x5e2, 0, 0),          /* 0xfb20   alt ayin */
        new decomp_C(0x5d0, 0, 0),          /* 0xfb21   alt alef */
        new decomp_C(0x5d3, 0, 0),          /* 0xfb22   alt dalet */
        new decomp_C(0x5d4, 0, 0),          /* 0xfb23   alt he */
        new decomp_C(0x5db, 0, 0),          /* 0xfb24   alt kaf */
        new decomp_C(0x5dc, 0, 0),          /* 0xfb25   alt lamed */
        new decomp_C(0x5dd, 0, 0),          /* 0xfb26   alt mem-sofit */
        new decomp_C(0x5e8, 0, 0),          /* 0xfb27   alt resh */
        new decomp_C(0x5ea, 0, 0),          /* 0xfb28   alt tav */
        new decomp_C('+', 0, 0),            /* 0xfb29   alt plus */
        new decomp_C(0x5e9, 0x5c1, 0),      /* 0xfb2a   shin+shin-dot */
        new decomp_C(0x5e9, 0x5c2, 0),      /* 0xfb2b   shin+sin-dot */
        new decomp_C(0x5e9, 0x5c1, 0x5bc),  /* 0xfb2c   shin+shin-dot+dagesh */
        new decomp_C(0x5e9, 0x5c2, 0x5bc),  /* 0xfb2d   shin+sin-dot+dagesh */
        new decomp_C(0x5d0, 0x5b7, 0),      /* 0xfb2e   alef+patah */
        new decomp_C(0x5d0, 0x5b8, 0),      /* 0xfb2f   alef+qamats */
        new decomp_C(0x5d0, 0x5b4, 0),      /* 0xfb30   alef+hiriq */
        new decomp_C(0x5d1, 0x5bc, 0),      /* 0xfb31   bet+dagesh */
        new decomp_C(0x5d2, 0x5bc, 0),      /* 0xfb32   gimel+dagesh */
        new decomp_C(0x5d3, 0x5bc, 0),      /* 0xfb33   dalet+dagesh */
        new decomp_C(0x5d4, 0x5bc, 0),      /* 0xfb34   he+dagesh */
        new decomp_C(0x5d5, 0x5bc, 0),      /* 0xfb35   vav+dagesh */
        new decomp_C(0x5d6, 0x5bc, 0),      /* 0xfb36   zayin+dagesh */
        new decomp_C(0xfb37, 0, 0),         /* 0xfb37 - UNUSED */
        new decomp_C(0x5d8, 0x5bc, 0),      /* 0xfb38   tet+dagesh */
        new decomp_C(0x5d9, 0x5bc, 0),      /* 0xfb39   yud+dagesh */
        new decomp_C(0x5da, 0x5bc, 0),      /* 0xfb3a   kaf sofit+dagesh */
        new decomp_C(0x5db, 0x5bc, 0),      /* 0xfb3b   kaf+dagesh */
        new decomp_C(0x5dc, 0x5bc, 0),      /* 0xfb3c   lamed+dagesh */
        new decomp_C(0xfb3d, 0, 0),         /* 0xfb3d - UNUSED */
        new decomp_C(0x5de, 0x5bc, 0),      /* 0xfb3e   mem+dagesh */
        new decomp_C(0xfb3f, 0, 0),         /* 0xfb3f - UNUSED */
        new decomp_C(0x5e0, 0x5bc, 0),      /* 0xfb40   nun+dagesh */
        new decomp_C(0x5e1, 0x5bc, 0),      /* 0xfb41   samech+dagesh */
        new decomp_C(0xfb42, 0, 0),         /* 0xfb42 - UNUSED */
        new decomp_C(0x5e3, 0x5bc, 0),      /* 0xfb43   pe sofit+dagesh */
        new decomp_C(0x5e4, 0x5bc, 0),      /* 0xfb44   pe+dagesh */
        new decomp_C(0xfb45, 0, 0),         /* 0xfb45 - UNUSED */
        new decomp_C(0x5e6, 0x5bc, 0),      /* 0xfb46   tsadi+dagesh */
        new decomp_C(0x5e7, 0x5bc, 0),      /* 0xfb47   qof+dagesh */
        new decomp_C(0x5e8, 0x5bc, 0),      /* 0xfb48   resh+dagesh */
        new decomp_C(0x5e9, 0x5bc, 0),      /* 0xfb49   shin+dagesh */
        new decomp_C(0x5ea, 0x5bc, 0),      /* 0xfb4a   tav+dagesh */
        new decomp_C(0x5d5, 0x5b9, 0),      /* 0xfb4b   vav+holam */
        new decomp_C(0x5d1, 0x5bf, 0),      /* 0xfb4c   bet+rafe */
        new decomp_C(0x5db, 0x5bf, 0),      /* 0xfb4d   kaf+rafe */
        new decomp_C(0x5e4, 0x5bf, 0),      /* 0xfb4e   pe+rafe */
        new decomp_C(0x5d0, 0x5dc, 0)       /* 0xfb4f   alef-lamed */
    };

    /*private*/ static void mb_decompose(int c, int[] c1, int[] c2, int[] c3)
    {
        if (0xfb20 <= c && c <= 0xfb4f)
        {
            decomp_C d = decomp_table[c - 0xfb20];
            c1[0] = d.a;
            c2[0] = d.b;
            c3[0] = d.c;
        }
        else
        {
            c1[0] = c;
            c2[0] = c3[0] = 0;
        }
    }

    /*
     * Compare two strings, ignore case if ireg_ic set.
     * Return 0 if strings match, non-zero otherwise.
     * Correct the length "*n" when composing characters are ignored.
     */
    /*private*/ static int cstrncmp(Bytes s1, Bytes s2, int[] n)
    {
        int result;

        if (!ireg_ic)
            result = STRNCMP(s1, s2, n[0]);
        else
            result = us_strnicmp(s1, s2, n[0]);

        /* if it failed and it's utf8 and we want to combineignore */
        if (result != 0 && ireg_icombine)
        {
            Bytes[] str1 = { s1 };
            Bytes[] str2 = { s2 };
            int c1 = 0, c2 = 0;

            /* We have to handle the strcmp() ourselves, since it is necessary
             * to deal with the composing characters by ignoring them. */
            while (BDIFF(str1[0], s1) < n[0])
            {
                c1 = us_ptr2char_adv(str1, true);
                c2 = us_ptr2char_adv(str2, true);

                /* Decompose the character if necessary into 'base' characters,
                 * because I don't care about Arabic, I will hard-code the Hebrew
                 * which I *do* care about!  So sue me... */
                if (c1 != c2 && (!ireg_ic || utf_fold(c1) != utf_fold(c2)))
                {
                    int[] c11 = new int[1];
                    int[] c12 = new int[1];
                    int[] junk = new int[1];

                    /* decomposition necessary? */
                    mb_decompose(c1, c11, junk, junk);
                    mb_decompose(c2, c12, junk, junk);
                    c1 = c11[0];
                    c2 = c12[0];
                    if (c11[0] != c12[0] && (!ireg_ic || utf_fold(c11[0]) != utf_fold(c12[0])))
                        break;
                }
            }
            result = c2 - c1;
            if (result == 0)
                n[0] = BDIFF(str2[0], s2);
        }

        return result;
    }

    /*
     * This function is used a lot for simple searches, keep it fast!
     */
    /*private*/ static Bytes cstrchr(Bytes s, int c)
    {
        if (!ireg_ic)
            return vim_strchr(s, c);

        int cc;
        if (0x80 < c)
            cc = utf_fold(c);
        else if (utf_isupper(c))
            cc = utf_tolower(c);
        else if (utf_islower(c))
            cc = utf_toupper(c);
        else
            return vim_strchr(s, c);

        for (Bytes p = s; p.at(0) != NUL; p = p.plus(us_ptr2len_cc(p)))
        {
            if (0x80 < c)
            {
                if (utf_fold(us_ptr2char(p)) == cc)
                    return p;
            }
            else if (p.at(0) == c || p.at(0) == cc)
                return p;
        }

        return null;
    }

    /***************************************************************
     *                    regsub stuff                             *
     ***************************************************************/

    /*
     * We should define ftpr as a pointer to a function returning
     * a pointer to a function returning a pointer to a function ...
     * This is impossible, so we declare a pointer to a function
     * returning a pointer to a function returning void.
     */
    /*private*/ static abstract class fptr_C
    {
        public abstract fptr_C flip(int[] d, int c);
    }

    /*private*/ static final fptr_C do_upper = new fptr_C()
    {
        public fptr_C flip(int[] d, int c) { d[0] = utf_toupper(c); return null; }
    };

    /*private*/ static final fptr_C do_Upper = new fptr_C()
    {
        public fptr_C flip(int[] d, int c) { d[0] = utf_toupper(c); return this; }
    };

    /*private*/ static final fptr_C do_lower = new fptr_C()
    {
        public fptr_C flip(int[] d, int c) { d[0] = utf_tolower(c); return null; }
    };

    /*private*/ static final fptr_C do_Lower = new fptr_C()
    {
        public fptr_C flip(int[] d, int c) { d[0] = utf_tolower(c); return this; }
    };

    /*
     * regtilde(): Replace tildes in the pattern by the old pattern.
     *
     * Short explanation of the tilde: It stands for the previous replacement pattern.
     * If that previous pattern also contains a ~ we should go back a step further...
     * But we insert the previous pattern into the current one and remember that.
     * This still does not handle the case where "magic" changes.  So require the
     * user to keep his hands off of "magic".
     *
     * The tildes are parsed once before the first call to vim_regsub().
     */
    /*private*/ static Bytes regtilde(Bytes source, boolean magic)
    {
        Bytes newsub = source;

        for (Bytes p = newsub; p.at(0) != NUL; p = p.plus(1))
        {
            if ((p.at(0) == (byte)'~' && magic) || (p.at(0) == (byte)'\\' && p.at(1) == (byte)'~' && !magic))
            {
                if (reg_prev_sub != null)
                {
                    /* length = len(newsub) - 1 + len(prev_sub) + 1 */
                    int prevlen = strlen(reg_prev_sub);
                    Bytes tmpsub = new Bytes(strlen(newsub) + prevlen);

                    /* copy prefix */
                    int len = BDIFF(p, newsub);            /* not including ~ */
                    BCOPY(tmpsub, newsub, len);
                    /* interpret tilde */
                    BCOPY(tmpsub, len, reg_prev_sub, 0, prevlen);
                    /* copy postfix */
                    if (!magic)
                        p = p.plus(1);                                /* back off \ */
                    STRCPY(tmpsub.plus(len + prevlen), p.plus(1));

                    newsub = tmpsub;
                    p = newsub.plus(len + prevlen);
                }
                else if (magic)
                    BCOPY(p, 0, p, 1, strlen(p, 1) + 1);   /* remove '~' */
                else
                    BCOPY(p, 0, p, 2, strlen(p, 2) + 1);   /* remove '\~' */
                p = p.minus(1);
            }
            else
            {
                if (p.at(0) == (byte)'\\' && p.at(1) != NUL)             /* skip escaped characters */
                    p = p.plus(1);
                p = p.plus(us_ptr2len_cc(p) - 1);
            }
        }

        if (BNE(newsub, source))                       /* "newsub" was allocated, just keep it */
            reg_prev_sub = newsub;
        else                                        /* no ~ found, need to save "newsub" */
            reg_prev_sub = STRDUP(newsub);

        return newsub;
    }

    /*private*/ static boolean can_f_submatch;          /* true when submatch() can be used */

    /* These pointers are used instead of reg_match and reg_mmatch for reg_submatch().
     * Needed when the substitution string is an expression
     * that contains a call to substitute() and submatch().
     */
    /*private*/ static regmatch_C       submatch_match;
    /*private*/ static regmmatch_C      submatch_mmatch;
    /*private*/ static long             submatch_firstlnum;
    /*private*/ static long             submatch_maxline;
    /*private*/ static boolean          submatch_line_lbr;

    /*
     * vim_regsub() - perform substitutions after a vim_regexec() or vim_regexec_multi() match.
     *
     * If "copy" is true really copy into "dest".
     * If "copy" is false nothing is copied, this is just to find out the length of the result.
     *
     * If "backslash" is true, a backslash will be removed later, need to double them to keep them,
     * and insert a backslash before a CR to avoid it being replaced with a line break later.
     *
     * Note: The matched text must not change between the call of vim_regexec()/vim_regexec_multi()
     * and vim_regsub()!  It would make the back references invalid!
     *
     * Returns the size of the replacement, including terminating NUL.
     */
    /*private*/ static int vim_regsub(regmatch_C rmp, Bytes source, Bytes dest, boolean copy, boolean magic, boolean backslash)
    {
        reg_match = rmp;
        reg_mmatch = null;
        reg_maxline = 0;
        reg_buf = curbuf;
        reg_line_lbr = true;
        return vim_regsub_both(source, dest, copy, magic, backslash);
    }

    /*private*/ static int vim_regsub_multi(regmmatch_C rmp, long lnum, Bytes source, Bytes dest, boolean copy, boolean magic, boolean backslash)
    {
        reg_match = null;
        reg_mmatch = rmp;
        reg_buf = curbuf;           /* always works on the current buffer! */
        reg_firstlnum = lnum;
        reg_maxline = curbuf.b_ml.ml_line_count - lnum;
        reg_line_lbr = false;
        return vim_regsub_both(source, dest, copy, magic, backslash);
    }

    /*private*/ static Bytes eval_result;

    /*private*/ static int vim_regsub_both(Bytes source, Bytes dest, boolean copy, boolean magic, boolean backslash)
    {
        /* Be paranoid... */
        if (source == null || dest == null)
        {
            emsg(e_null);
            return 0;
        }
        if (prog_magic_wrong())
            return 0;

        Bytes dst = dest;

        /*
         * When the substitute part starts with "\=" evaluate it as an expression.
         */
        if (source.at(0) == (byte)'\\' && source.at(1) == (byte)'=' && !can_f_submatch)   /* can't do this recursively */
        {
            /* To make sure that the length doesn't change between checking the length
             * and copying the string, and to speed up things, the resulting string is saved
             * from the call with "copy" == false to the call with "copy" == true. */
            if (copy)
            {
                if (eval_result != null)
                {
                    STRCPY(dest, eval_result);
                    dst = dst.plus(strlen(eval_result));
                    eval_result = null;
                }
            }
            else
            {
                /* The expression may contain substitute(), which calls us recursively.
                 * Make sure submatch() gets the text from the first level.
                 * Don't need to save "reg_buf", because vim_regexec_multi() can't be called recursively.
                 */
                submatch_match = reg_match;
                submatch_mmatch = reg_mmatch;
                submatch_firstlnum = reg_firstlnum;
                submatch_maxline = reg_maxline;
                submatch_line_lbr = reg_line_lbr;

                window_C save_reg_win = reg_win;
                boolean save_ireg_ic = ireg_ic;
                can_f_submatch = true;

                eval_result = eval_to_string(source.plus(2), null, true);
                if (eval_result != null)
                {
                    boolean had_backslash = false;

                    for (Bytes s = eval_result; s.at(0) != NUL; s = s.plus(us_ptr2len_cc(s)))
                    {
                        /* Change NL to CR, so that it becomes a line break,
                         * unless called from vim_regexec_nl().
                         * Skip over a backslashed character. */
                        if (s.at(0) == NL && !submatch_line_lbr)
                            s.be(0, CAR);
                        else if (s.at(0) == (byte)'\\' && s.at(1) != NUL)
                        {
                            s = s.plus(1);
                            /* Change NL to CR here too, so that this works:
                             * :s/abc\\\ndef/\="aaa\\\nbbb"/  on text:
                             *   abc\
                             *   def
                             * Not when called from vim_regexec_nl().
                             */
                            if (s.at(0) == NL && !submatch_line_lbr)
                                s.be(0, CAR);
                            had_backslash = true;
                        }
                    }
                    if (had_backslash && backslash)
                    {
                        /* Backslashes will be consumed, need to double them. */
                        eval_result = vim_strsave_escaped(eval_result, u8("\\"));
                    }

                    dst = dst.plus(strlen(eval_result));
                }

                reg_match = submatch_match;
                reg_mmatch = submatch_mmatch;
                reg_firstlnum = submatch_firstlnum;
                reg_maxline = submatch_maxline;
                reg_line_lbr = submatch_line_lbr;
                reg_win = save_reg_win;
                ireg_ic = save_ireg_ic;
                can_f_submatch = false;
            }
        }
        else
        {
            fptr_C func_one = null;
            fptr_C func_all = null;

            int no = -1;
            long clnum = 0;
            int len = 0;

            Bytes src = source;

            for (byte b; (b = (src = src.plus(1)).at(-1)) != NUL; )
            {
                if (b == '&' && magic)
                    no = 0;
                else if (b == '\\' && src.at(0) != NUL)
                {
                    if (src.at(0) == (byte)'&' && !magic)
                    {
                        src = src.plus(1);
                        no = 0;
                    }
                    else if ('0' <= src.at(0) && src.at(0) <= '9')
                    {
                        no = (src = src.plus(1)).at(-1) - '0';
                    }
                    else if (vim_strbyte(u8("uUlLeE"), src.at(0)) != null)
                    {
                        switch ((src = src.plus(1)).at(-1))
                        {
                            case 'u':   func_one = do_upper;
                                        continue;
                            case 'U':   func_all = do_Upper;
                                        continue;
                            case 'l':   func_one = do_lower;
                                        continue;
                            case 'L':   func_all = do_Lower;
                                        continue;
                            case 'e':
                            case 'E':   func_one = func_all = null;
                                        continue;
                        }
                    }
                }
                if (no < 0)             /* Ordinary character. */
                {
                    if (b == KB_SPECIAL && src.at(0) != NUL && src.at(1) != NUL)
                    {
                        /* Copy a special key as-is. */
                        if (copy)
                        {
                            (dst = dst.plus(1)).be(-1, b);
                            (dst = dst.plus(1)).be(-1, (src = src.plus(1)).at(-1));
                            (dst = dst.plus(1)).be(-1, (src = src.plus(1)).at(-1));
                        }
                        else
                        {
                            dst = dst.plus(3);
                            src = src.plus(2);
                        }
                        continue;
                    }

                    int c;
                    if (b == '\\' && src.at(0) != NUL)
                    {
                        /* Check for abbreviations. */
                        switch (src.at(0))
                        {
                            case 'r': b = CAR;    src = src.plus(1); break;
                            case 'n': b = NL;     src = src.plus(1); break;
                            case 't': b = TAB;    src = src.plus(1); break;
                         /* Oh no!  \e already has meaning in subst pat :-( */
                         /* case 'e': b = ESC;    src = src.plus(1); break; */
                            case 'b': b = Ctrl_H; src = src.plus(1); break;

                            /* If "backslash" is true the backslash will be removed later.
                             * Used to insert a literal CR. */
                            default: if (backslash)
                                     {
                                         if (copy)
                                             dst.be(0, (byte)'\\');
                                         dst = dst.plus(1);
                                     }
                                     b = (src = src.plus(1)).at(-1);
                        }
                        c = char_u(b);
                    }
                    else
                        c = us_ptr2char(src.minus(1));

                    /* Write to buffer, if copy is set. */
                    int[] cc = new int[1];
                    if (func_one != null)
                        func_one = func_one.flip(cc, c);
                    else if (func_all != null)
                        func_all = func_all.flip(cc, c);
                    else /* just copy */
                        cc[0] = c;

                    int totlen = us_ptr2len_cc(src.minus(1));

                    if (copy)
                        utf_char2bytes(cc[0], dst);
                    dst = dst.plus(utf_char2len(cc[0]) - 1);

                    int clen = us_ptr2len(src.minus(1));

                    /* If the character length is shorter than "totlen",
                     * there are composing characters; copy them as-is. */
                    if (clen < totlen)
                    {
                        if (copy)
                            BCOPY(dst, 1, src, -1 + clen, totlen - clen);
                        dst = dst.plus(totlen - clen);
                    }

                    src = src.plus(totlen - 1);

                    dst = dst.plus(1);
                }
                else
                {
                    Bytes s;
                    if (reg_match == null)
                    {
                        clnum = reg_mmatch.startpos[no].lnum;
                        if (clnum < 0 || reg_mmatch.endpos[no].lnum < 0)
                            s = null;
                        else
                        {
                            s = reg_getline(clnum).plus(reg_mmatch.startpos[no].col);
                            if (reg_mmatch.endpos[no].lnum == clnum)
                                len = reg_mmatch.endpos[no].col - reg_mmatch.startpos[no].col;
                            else
                                len = strlen(s);
                        }
                    }
                    else
                    {
                        s = reg_match.startp[no];
                        if (reg_match.endp[no] == null)
                            s = null;
                        else
                            len = BDIFF(reg_match.endp[no], s);
                    }
                    if (s != null)
                    {
                        for ( ; ; )
                        {
                            if (len == 0)
                            {
                                if (reg_match == null)
                                {
                                    if (reg_mmatch.endpos[no].lnum == clnum)
                                        break;
                                    if (copy)
                                        dst.be(0, CAR);
                                    dst = dst.plus(1);
                                    s = reg_getline(++clnum);
                                    if (reg_mmatch.endpos[no].lnum == clnum)
                                        len = reg_mmatch.endpos[no].col;
                                    else
                                        len = strlen(s);
                                }
                                else
                                    break;
                            }
                            else if (s.at(0) == NUL)
                            {
                                if (copy)
                                    emsg(e_re_damg);
                                return BDIFF(dst, dest) + 1;
                            }
                            else
                            {
                                if (backslash && (s.at(0) == CAR || s.at(0) == (byte)'\\'))
                                {
                                    /*
                                     * Insert a backslash in front of a CR,
                                     * otherwise it will be replaced by a line break.
                                     * Number of backslashes will be halved later, double them here.
                                     */
                                    if (copy)
                                    {
                                        dst.be(0, (byte)'\\');
                                        dst.be(1, s.at(0));
                                    }
                                    dst = dst.plus(2);
                                }
                                else
                                {
                                    int c = us_ptr2char(s);

                                    int[] cc = new int[1];
                                    if (func_one != null)
                                        func_one = func_one.flip(cc, c);
                                    else if (func_all != null)
                                        func_all = func_all.flip(cc, c);
                                    else /* just copy */
                                        cc[0] = c;

                                    /* Copy composing characters separately, one at a time. */
                                    int l = us_ptr2len(s) - 1;

                                    s = s.plus(l);
                                    len -= l;
                                    if (copy)
                                        utf_char2bytes(cc[0], dst);
                                    dst = dst.plus(utf_char2len(cc[0]) - 1);

                                    dst = dst.plus(1);
                                }

                                s = s.plus(1);
                                --len;
                            }
                        }
                    }
                    no = -1;
                }
            }
        }

        if (copy)
            dst.be(0, NUL);

        return BDIFF(dst, dest) + 1;
    }

    /*
     * Call reg_getline() with the line numbers from the submatch.
     * If a substitute() was used the reg_maxline and other values have been overwritten.
     */
    /*private*/ static Bytes reg_getline_submatch(long lnum)
    {
        long save_first = reg_firstlnum;
        long save_max = reg_maxline;

        reg_firstlnum = submatch_firstlnum;
        reg_maxline = submatch_maxline;

        Bytes s = reg_getline(lnum);

        reg_firstlnum = save_first;
        reg_maxline = save_max;

        return s;
    }

    /*
     * Used for the submatch() function: get the string from the n'th submatch in allocated memory.
     * Returns null when not in a ":s" command and for a non-existing submatch.
     */
    /*private*/ static Bytes reg_submatch(int no)
    {
        Bytes retval = null;

        if (!can_f_submatch || no < 0)
            return null;

        if (submatch_match == null)
        {
            /*
             * First round: compute the length and allocate memory.
             * Second round: copy the text.
             */
            for (int round = 1; round <= 2; round++)
            {
                long lnum = submatch_mmatch.startpos[no].lnum;
                if (lnum < 0 || submatch_mmatch.endpos[no].lnum < 0)
                    return null;

                Bytes s = reg_getline_submatch(lnum).plus(submatch_mmatch.startpos[no].col);
                if (s == null)  /* anti-crash check, cannot happen? */
                    break;

                int len;
                if (submatch_mmatch.endpos[no].lnum == lnum)
                {
                    /* Within one line: take form start to end col. */
                    len = submatch_mmatch.endpos[no].col - submatch_mmatch.startpos[no].col;
                    if (round == 2)
                        vim_strncpy(retval, s, len);
                    len++;
                }
                else
                {
                    /* Multiple lines: take start line from start col,
                     * middle lines completely, and end line up to end col. */
                    len = strlen(s);
                    if (round == 2)
                    {
                        STRCPY(retval, s);
                        retval.be(len, (byte)'\n');
                    }
                    len++;
                    lnum++;
                    while (lnum < submatch_mmatch.endpos[no].lnum)
                    {
                        s = reg_getline_submatch(lnum++);
                        if (round == 2)
                            STRCPY(retval.plus(len), s);
                        len += strlen(s);
                        if (round == 2)
                            retval.be(len, (byte)'\n');
                        len++;
                    }
                    if (round == 2)
                        STRNCPY(retval.plus(len), reg_getline_submatch(lnum), submatch_mmatch.endpos[no].col);
                    len += submatch_mmatch.endpos[no].col;
                    if (round == 2)
                        retval.be(len, NUL);
                    len++;
                }

                if (retval == null)
                    retval = new Bytes(len);
            }
        }
        else
        {
            Bytes s = submatch_match.startp[no];
            if (s == null || submatch_match.endp[no] == null)
                retval = null;
            else
                retval = STRNDUP(s, BDIFF(submatch_match.endp[no], s));
        }

        return retval;
    }

    /*
     * Used for the submatch() function with the optional non-zero argument:
     * get the list of strings from the n'th submatch in allocated memory with
     * NULs represented in NLs.
     * Returns a list of allocated strings.  Returns null when not in a ":s"
     * command, for a non-existing submatch and for any error.
     */
    /*private*/ static list_C reg_submatch_list(int no)
    {
        list_C list;

        boolean error = false;

        if (!can_f_submatch || no < 0)
            return null;

        if (submatch_match == null)
        {
            long slnum = submatch_mmatch.startpos[no].lnum;
            long elnum = submatch_mmatch.endpos[no].lnum;
            if (slnum < 0 || elnum < 0)
                return null;

            int scol = submatch_mmatch.startpos[no].col;
            int ecol = submatch_mmatch.endpos[no].col;

            list = new list_C();

            Bytes s = reg_getline_submatch(slnum).plus(scol);
            if (slnum == elnum)
            {
                if (list_append_string(list, s, ecol - scol) == false)
                    error = true;
            }
            else
            {
                if (list_append_string(list, s, -1) == false)
                    error = true;
                for (int i = 1; i < elnum - slnum; i++)
                {
                    s = reg_getline_submatch(slnum + i);
                    if (list_append_string(list, s, -1) == false)
                        error = true;
                }
                s = reg_getline_submatch(elnum);
                if (list_append_string(list, s, ecol) == false)
                    error = true;
            }
        }
        else
        {
            Bytes s = submatch_match.startp[no];
            if (s == null || submatch_match.endp[no] == null)
                return null;

            list = new list_C();

            if (list_append_string(list, s, BDIFF(submatch_match.endp[no], s)) == false)
                error = true;
        }

        if (error)
        {
            list_free(list, true);
            return null;
        }

        return list;
    }
}
