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
public class VimO
{
    /*
     * NFA regular expression implementation.
     */

    /* Added to NFA_ANY - NFA_NUPPER_IC to include a NL. */
    /*private*/ static final int NFA_ADD_NL = 31;

    /*private*/ static final int
        NFA_SPLIT = -1024,
        NFA_MATCH = -1023,
        NFA_EMPTY = -1022,                      /* matches 0-length */

        NFA_START_COLL = -1021,                 /* [abc] start */
        NFA_END_COLL = -1020,                   /* [abc] end */
        NFA_START_NEG_COLL = -1019,             /* [^abc] start */
        NFA_END_NEG_COLL = -1018,               /* [^abc] end (postfix only) */
        NFA_RANGE = -1017,                      /* range of the two previous items
                                                 * (postfix only) */
        NFA_RANGE_MIN = -1016,                  /* low end of a range */
        NFA_RANGE_MAX = -1015,                  /* high end of a range */

        NFA_CONCAT = -1014,                     /* concatenate two previous items (postfix only) */
        NFA_OR = -1013,                         /* \| (postfix only) */
        NFA_STAR = -1012,                       /* greedy * (posfix only) */
        NFA_STAR_NONGREEDY = -1011,             /* non-greedy * (postfix only) */
        NFA_QUEST = -1010,                      /* greedy \? (postfix only) */
        NFA_QUEST_NONGREEDY = -1009,            /* non-greedy \? (postfix only) */

        NFA_BOL = -1008,                        /* ^    Begin line */
        NFA_EOL = -1007,                        /* $    End line */
        NFA_BOW = -1006,                        /* \<   Begin word */
        NFA_EOW = -1005,                        /* \>   End word */
        NFA_BOF = -1004,                        /* \%^  Begin file */
        NFA_EOF = -1003,                        /* \%$  End file */
        NFA_NEWL = -1002,
        NFA_ZSTART = -1001,                     /* Used for \zs */
        NFA_ZEND = -1000,                       /* Used for \ze */
        NFA_NOPEN = -999,                       /* Start of subexpression marked with \%( */
        NFA_NCLOSE = -998,                      /* End of subexpr. marked with \%( ... \) */
        NFA_START_INVISIBLE = -997,
        NFA_START_INVISIBLE_FIRST = -996,
        NFA_START_INVISIBLE_NEG = -995,
        NFA_START_INVISIBLE_NEG_FIRST = -994,
        NFA_START_INVISIBLE_BEFORE = -993,
        NFA_START_INVISIBLE_BEFORE_FIRST = -992,
        NFA_START_INVISIBLE_BEFORE_NEG = -991,
        NFA_START_INVISIBLE_BEFORE_NEG_FIRST = -990,
        NFA_START_PATTERN = -989,
        NFA_END_INVISIBLE = -988,
        NFA_END_INVISIBLE_NEG = -987,
        NFA_END_PATTERN = -986,
        NFA_COMPOSING = -985,                   /* Next nodes in NFA are part of the
                                                 * composing multibyte char */
        NFA_END_COMPOSING = -984,               /* End of a composing char in the NFA */
        NFA_ANY_COMPOSING = -983,               /* \%C: Any composing characters. */
        NFA_OPT_CHARS = -982,                   /* \%[abc] */

        /* The following are used only in the postfix form, not in the NFA. */
        NFA_PREV_ATOM_NO_WIDTH = -981,          /* Used for \@= */
        NFA_PREV_ATOM_NO_WIDTH_NEG = -980,      /* Used for \@! */
        NFA_PREV_ATOM_JUST_BEFORE = -979,       /* Used for \@<= */
        NFA_PREV_ATOM_JUST_BEFORE_NEG = -978,   /* Used for \@<! */
        NFA_PREV_ATOM_LIKE_PATTERN = -977,      /* Used for \@> */

        NFA_BACKREF1 = -976,                    /* \1 */
        NFA_BACKREF2 = -975,                    /* \2 */
        NFA_BACKREF3 = -974,                    /* \3 */
        NFA_BACKREF4 = -973,                    /* \4 */
        NFA_BACKREF5 = -972,                    /* \5 */
        NFA_BACKREF6 = -971,                    /* \6 */
        NFA_BACKREF7 = -970,                    /* \7 */
        NFA_BACKREF8 = -969,                    /* \8 */
        NFA_BACKREF9 = -968,                    /* \9 */
        NFA_ZREF1 = -967,                       /* \z1 */
        NFA_ZREF2 = -966,                       /* \z2 */
        NFA_ZREF3 = -965,                       /* \z3 */
        NFA_ZREF4 = -964,                       /* \z4 */
        NFA_ZREF5 = -963,                       /* \z5 */
        NFA_ZREF6 = -962,                       /* \z6 */
        NFA_ZREF7 = -961,                       /* \z7 */
        NFA_ZREF8 = -960,                       /* \z8 */
        NFA_ZREF9 = -959,                       /* \z9 */
        NFA_SKIP = -958,                        /* Skip characters */

        NFA_MOPEN = -957,
        NFA_MOPEN1 = -956,
        NFA_MOPEN2 = -955,
        NFA_MOPEN3 = -954,
        NFA_MOPEN4 = -953,
        NFA_MOPEN5 = -952,
        NFA_MOPEN6 = -951,
        NFA_MOPEN7 = -950,
        NFA_MOPEN8 = -949,
        NFA_MOPEN9 = -948,

        NFA_MCLOSE = -947,
        NFA_MCLOSE1 = -946,
        NFA_MCLOSE2 = -945,
        NFA_MCLOSE3 = -944,
        NFA_MCLOSE4 = -943,
        NFA_MCLOSE5 = -942,
        NFA_MCLOSE6 = -941,
        NFA_MCLOSE7 = -940,
        NFA_MCLOSE8 = -939,
        NFA_MCLOSE9 = -938,

        NFA_ZOPEN = -937,
        NFA_ZOPEN1 = -936,
        NFA_ZOPEN2 = -935,
        NFA_ZOPEN3 = -934,
        NFA_ZOPEN4 = -933,
        NFA_ZOPEN5 = -932,
        NFA_ZOPEN6 = -931,
        NFA_ZOPEN7 = -930,
        NFA_ZOPEN8 = -929,
        NFA_ZOPEN9 = -928,

        NFA_ZCLOSE = -927,
        NFA_ZCLOSE1 = -926,
        NFA_ZCLOSE2 = -925,
        NFA_ZCLOSE3 = -924,
        NFA_ZCLOSE4 = -923,
        NFA_ZCLOSE5 = -922,
        NFA_ZCLOSE6 = -921,
        NFA_ZCLOSE7 = -920,
        NFA_ZCLOSE8 = -919,
        NFA_ZCLOSE9 = -918,

        /* NFA_FIRST_NL */
        NFA_ANY = -917,                        /* Match any one character. */
        NFA_IDENT = -916,                      /* Match identifier char */
        NFA_SIDENT = -915,                     /* Match identifier char but no digit */
        NFA_KWORD = -914,                      /* Match keyword char */
        NFA_SKWORD = -913,                     /* Match word char but no digit */
        NFA_FNAME = -912,                      /* Match file name char */
        NFA_SFNAME = -911,                     /* Match file name char but no digit */
        NFA_PRINT = -910,                      /* Match printable char */
        NFA_SPRINT = -909,                     /* Match printable char but no digit */
        NFA_WHITE = -908,                      /* Match whitespace char */
        NFA_NWHITE = -907,                     /* Match non-whitespace char */
        NFA_DIGIT = -906,                      /* Match digit char */
        NFA_NDIGIT = -905,                     /* Match non-digit char */
        NFA_HEX = -904,                        /* Match hex char */
        NFA_NHEX = -903,                       /* Match non-hex char */
        NFA_OCTAL = -902,                      /* Match octal char */
        NFA_NOCTAL = -901,                     /* Match non-octal char */
        NFA_WORD = -900,                       /* Match word char */
        NFA_NWORD = -899,                      /* Match non-word char */
        NFA_HEAD = -898,                       /* Match head char */
        NFA_NHEAD = -897,                      /* Match non-head char */
        NFA_ALPHA = -896,                      /* Match alpha char */
        NFA_NALPHA = -895,                     /* Match non-alpha char */
        NFA_LOWER = -894,                      /* Match lowercase char */
        NFA_NLOWER = -893,                     /* Match non-lowercase char */
        NFA_UPPER = -892,                      /* Match uppercase char */
        NFA_NUPPER = -891,                     /* Match non-uppercase char */
        NFA_LOWER_IC = -890,                   /* Match [a-z] */
        NFA_NLOWER_IC = -889,                  /* Match [^a-z] */
        NFA_UPPER_IC = -888,                   /* Match [A-Z] */
        NFA_NUPPER_IC = -887,                  /* Match [^A-Z] */

        NFA_FIRST_NL = NFA_ANY + NFA_ADD_NL,
        NFA_LAST_NL = NFA_NUPPER_IC + NFA_ADD_NL,

        NFA_CURSOR = -855,                     /* Match cursor pos */
        NFA_LNUM = -854,                       /* Match line number */
        NFA_LNUM_GT = -853,                    /* Match > line number */
        NFA_LNUM_LT = -852,                    /* Match < line number */
        NFA_COL = -851,                        /* Match cursor column */
        NFA_COL_GT = -850,                     /* Match > cursor column */
        NFA_COL_LT = -849,                     /* Match < cursor column */
        NFA_VCOL = -848,                       /* Match cursor virtual column */
        NFA_VCOL_GT = -847,                    /* Match > cursor virtual column */
        NFA_VCOL_LT = -846,                    /* Match < cursor virtual column */
        NFA_MARK = -845,                       /* Match mark */
        NFA_MARK_GT = -844,                    /* Match > mark */
        NFA_MARK_LT = -843,                    /* Match < mark */
        NFA_VISUAL = -842,                     /* Match Visual area */

        /* Character classes [:alnum:] etc. */
        NFA_CLASS_ALNUM = -841,
        NFA_CLASS_ALPHA = -840,
        NFA_CLASS_BLANK = -839,
        NFA_CLASS_CNTRL = -838,
        NFA_CLASS_DIGIT = -837,
        NFA_CLASS_GRAPH = -836,
        NFA_CLASS_LOWER = -835,
        NFA_CLASS_PRINT = -834,
        NFA_CLASS_PUNCT = -833,
        NFA_CLASS_SPACE = -832,
        NFA_CLASS_UPPER = -831,
        NFA_CLASS_XDIGIT = -830,
        NFA_CLASS_TAB = -829,
        NFA_CLASS_RETURN = -828,
        NFA_CLASS_BACKSPACE = -827,
        NFA_CLASS_ESCAPE = -826;

    /* Keep in sync with "classchars". */
    /*private*/ static int[] nfa_classcodes =
    {
        NFA_ANY, NFA_IDENT, NFA_SIDENT, NFA_KWORD, NFA_SKWORD,
        NFA_FNAME, NFA_SFNAME, NFA_PRINT, NFA_SPRINT,
        NFA_WHITE, NFA_NWHITE, NFA_DIGIT, NFA_NDIGIT,
        NFA_HEX, NFA_NHEX, NFA_OCTAL, NFA_NOCTAL,
        NFA_WORD, NFA_NWORD, NFA_HEAD, NFA_NHEAD,
        NFA_ALPHA, NFA_NALPHA, NFA_LOWER, NFA_NLOWER,
        NFA_UPPER, NFA_NUPPER
    };

    /*private*/ static Bytes e_nul_found       = u8("E865: (NFA) Regexp end encountered prematurely");
    /*private*/ static Bytes e_misplaced       = u8("E866: (NFA regexp) Misplaced %c");
    /*private*/ static Bytes e_ill_char_class  = u8("E877: (NFA regexp) Invalid character class: %ld");

    /* re_flags passed to nfa_regcomp() */
    /*private*/ static int nfa_re_flags;

    /* NFA regexp \ze operator encountered. */
    /*private*/ static boolean nfa_has_zend;

    /* NFA regexp \1 .. \9 encountered. */
    /*private*/ static boolean nfa_has_backref;

    /* NFA regexp has \z( ), set zsubexpr. */
    /*private*/ static boolean nfa_has_zsubexpr;

    /* Number of sub expressions actually being used during execution.
     * 1 if only the whole match (subexpr 0) is used. */
    /*private*/ static int nfa_nsubexpr;

    /*private*/ static int[] post_array;    /* holds the postfix form of r.e. */
    /*private*/ static int post_index;

    /* If not null match must end at this position. */
    /*private*/ static save_se_C nfa_endp;

    /* 'listid' is global, so that it increases on recursive calls to nfa_regmatch(),
     * which means we don't have to clear the lastlist field of all the states. */
    /*private*/ static int nfa_listid;
    /*private*/ static int nfa_alt_listid;

    /* 0 for first call to nfa_regmatch(), 1 for recursive call. */
    /*private*/ static int nfa_ll_index;

    /*
     * Initialize internal variables before NFA compilation.
     * Return true on success, false otherwise.
     */
    /*private*/ static void nfa_regcomp_start(Bytes expr, int re_flags)
        /* re_flags: see vim_regcomp() */
    {
        /* A reasonable estimation for maximum size. */
        int nstate_max = (strlen(expr) + 1) * 25;

        /* Some items blow up in size, such as [A-z].  Add more space for that.
         * When it is still not enough grow_post_array() will be used. */
        nstate_max += 1000;

        post_array = new int[nstate_max];
        post_index = 0;

        nfa_has_zend = false;
        nfa_has_backref = false;

        /* shared with BT engine */
        regcomp_start(expr, re_flags);
    }

    /*
     * Figure out if the NFA state list starts with an anchor, must match at start of the line.
     */
    /*private*/ static boolean nfa_get_reganch(nfa_state_C start, int depth)
    {
        if (4 < depth)
            return false;

        for (nfa_state_C p = start; p != null; )
        {
            switch (p.c)
            {
                case NFA_BOL:
                case NFA_BOF:
                    return true; /* yes! */

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
                    p = p.out0();
                    break;

                case NFA_SPLIT:
                    return nfa_get_reganch(p.out0(), depth + 1) && nfa_get_reganch(p.out1(), depth + 1);

                default:
                    return false; /* noooo! */
            }
        }

        return false;
    }

    /*
     * Figure out if the NFA state list starts with a character which must match at start of the match.
     */
    /*private*/ static int nfa_get_regstart(nfa_state_C start, int depth)
    {
        if (4 < depth)
            return 0;

        for (nfa_state_C p = start; p != null; )
        {
            switch (p.c)
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
                    p = p.out0();
                    break;

                case NFA_SPLIT:
                {
                    int c1 = nfa_get_regstart(p.out0(), depth + 1);
                    int c2 = nfa_get_regstart(p.out1(), depth + 1);

                    if (c1 == c2)
                        return c1;      /* yes! */

                    return 0;
                }

                default:
                    if (0 < p.c)
                        return p.c;     /* yes! */

                    return 0;
            }
        }

        return 0;
    }

    /*
     * Figure out if the NFA state list contains just literal text and nothing else.
     * If so return a string in allocated memory with what must match after regstart.
     * Otherwise return null.
     */
    /*private*/ static Bytes nfa_get_match_text(nfa_state_C start)
    {
        nfa_state_C p = start;
        if (p.c != NFA_MOPEN)
            return null;                /* just in case */
        p = p.out0();

        int len = 0;
        while (0 < p.c)
        {
            len += utf_char2len(p.c);
            p = p.out0();
        }

        if (p.c != NFA_MCLOSE || p.out0().c != NFA_MATCH)
            return null;

        Bytes ret = new Bytes(len);

        p = start.out0().out0();    /* skip first char, it goes into regstart */
        Bytes s = ret;
        while (0 < p.c)
        {
            s = s.plus(utf_char2bytes(p.c, s));
            p = p.out0();
        }
        s.be(0, NUL);

        return ret;
    }

    /*
     * Allocate more space for post_array.
     * Called when running above the estimated number of states.
     */
    /*private*/ static void grow_post_array(int more)
    {
        post_array = Arrays.copyOf(post_array, post_array.length + more);
    }

    /*
     * Search between "start" and "end" and try to recognize a character class in expanded form.
     * For example [0-9].
     * On success, return the id the character class to be emitted.
     * On failure, return 0 (=false).
     * Start points to the first char of the range, while end should point to the closing brace.
     * Keep in mind that 'ignorecase' applies at execution time,
     * thus [a-z] may need to be interpreted as [a-zA-Z].
     */
    /*private*/ static int nfa_recognize_char_class(Bytes start, Bytes end, boolean newl)
    {
        final int
            CLASS_not        = 0x80,
            CLASS_af         = 0x40,
            CLASS_AF         = 0x20,
            CLASS_az         = 0x10,
            CLASS_AZ         = 0x08,
            CLASS_o7         = 0x04,
            CLASS_o9         = 0x02,
            CLASS_underscore = 0x01;

        int config = 0;

        if (end.at(0) != (byte)']')
            return 0;

        Bytes p = start;
        if (p.at(0) == (byte)'^')
        {
            config |= CLASS_not;
            p = p.plus(1);
        }

        while (BLT(p, end))
        {
            if (BLT(p.plus(2), end) && p.at(1) == (byte)'-')
            {
                switch (p.at(0))
                {
                    case '0':
                        if (p.at(2) == (byte)'9')
                        {
                            config |= CLASS_o9;
                            break;
                        }
                        else if (p.at(2) == (byte)'7')
                        {
                            config |= CLASS_o7;
                            break;
                        }
                    case 'a':
                        if (p.at(2) == (byte)'z')
                        {
                            config |= CLASS_az;
                            break;
                        }
                        else if (p.at(2) == (byte)'f')
                        {
                            config |= CLASS_af;
                            break;
                        }
                    case 'A':
                        if (p.at(2) == (byte)'Z')
                        {
                            config |= CLASS_AZ;
                            break;
                        }
                        else if (p.at(2) == (byte)'F')
                        {
                            config |= CLASS_AF;
                            break;
                        }
                    /* FALLTHROUGH */
                    default:
                        return 0;
                }
                p = p.plus(3);
            }
            else if (BLT(p.plus(1), end) && p.at(0) == (byte)'\\' && p.at(1) == (byte)'n')
            {
                newl = true;
                p = p.plus(2);
            }
            else if (p.at(0) == (byte)'_')
            {
                config |= CLASS_underscore;
                p = p.plus(1);
            }
            else if (p.at(0) == (byte)'\n')
            {
                newl = true;
                p = p.plus(1);
            }
            else
                return 0;
        }

        if (BNE(p, end))
            return 0;

        int nfa_add_nl = (newl) ? NFA_ADD_NL : 0;

        switch (config)
        {
            case CLASS_o9:
                return nfa_add_nl + NFA_DIGIT;
            case CLASS_not | CLASS_o9:
                return nfa_add_nl + NFA_NDIGIT;
            case CLASS_af | CLASS_AF | CLASS_o9:
                return nfa_add_nl + NFA_HEX;
            case CLASS_not | CLASS_af | CLASS_AF | CLASS_o9:
                return nfa_add_nl + NFA_NHEX;
            case CLASS_o7:
                return nfa_add_nl + NFA_OCTAL;
            case CLASS_not | CLASS_o7:
                return nfa_add_nl + NFA_NOCTAL;
            case CLASS_az | CLASS_AZ | CLASS_o9 | CLASS_underscore:
                return nfa_add_nl + NFA_WORD;
            case CLASS_not | CLASS_az | CLASS_AZ | CLASS_o9 | CLASS_underscore:
                return nfa_add_nl + NFA_NWORD;
            case CLASS_az | CLASS_AZ | CLASS_underscore:
                return nfa_add_nl + NFA_HEAD;
            case CLASS_not | CLASS_az | CLASS_AZ | CLASS_underscore:
                return nfa_add_nl + NFA_NHEAD;
            case CLASS_az | CLASS_AZ:
                return nfa_add_nl + NFA_ALPHA;
            case CLASS_not | CLASS_az | CLASS_AZ:
                return nfa_add_nl + NFA_NALPHA;
            case CLASS_az:
                return nfa_add_nl + NFA_LOWER_IC;
            case CLASS_not | CLASS_az:
                return nfa_add_nl + NFA_NLOWER_IC;
            case CLASS_AZ:
                return nfa_add_nl + NFA_UPPER_IC;
            case CLASS_not | CLASS_AZ:
                return nfa_add_nl + NFA_NUPPER_IC;
        }

        return 0;
    }

    /*
     * helper functions used when doing re2post() ... regatom() parsing
     */
    /*private*/ static boolean emc1(int c)
    {
        if (post_array.length <= post_index)
            grow_post_array(1000);

        post_array[post_index++] = c;

        return true;
    }

    /*private*/ static boolean emc2(int c)
    {
        return emc1(c) && emc1(NFA_CONCAT);
    }

    /*
     * Produce the bytes for equivalence class "c".
     * Currently only handles latin1, latin9 and utf-8.
     * Emits bytes in postfix notation: 'a,b,NFA_OR,c,NFA_OR' is equivalent to 'a OR b OR c'.
     *
     * NOTE! When changing this function, also update reg_equi_class()
     */
    /*private*/ static boolean nfa_emit_equi_class(int c)
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
                return emc2('A')
                    && emc2(0xc0) && emc2(0xc1) && emc2(0xc2)
                    && emc2(0xc3) && emc2(0xc4) && emc2(0xc5)
                    && emc2(0x100) && emc2(0x102) && emc2(0x104)
                    && emc2(0x1cd) && emc2(0x1de) && emc2(0x1e0)
                    && emc2(0x1ea2);
            }
            case 'a':
            case 0xe0: case 0xe1: case 0xe2:
            case 0xe3: case 0xe4: case 0xe5:
            case 0x101: case 0x103: case 0x105:
            case 0x1ce: case 0x1df: case 0x1e1:
            case 0x1ea3:
            {
                return emc2('a')
                    && emc2(0xe0) && emc2(0xe1) && emc2(0xe2)
                    && emc2(0xe3) && emc2(0xe4) && emc2(0xe5)
                    && emc2(0x101) && emc2(0x103) && emc2(0x105)
                    && emc2(0x1ce) && emc2(0x1df) && emc2(0x1e1)
                    && emc2(0x1ea3);
            }

            case 'B':
            case 0x1e02: case 0x1e06:
            {
                return emc2('B')
                    && emc2(0x1e02) && emc2(0x1e06);
            }
            case 'b':
            case 0x1e03: case 0x1e07:
            {
                return emc2('b')
                    && emc2(0x1e03) && emc2(0x1e07);
            }

            case 'C':
            case 0xc7:
            case 0x106: case 0x108: case 0x10a: case 0x10c:
            {
                return emc2('C')
                    && emc2(0xc7)
                    && emc2(0x106) && emc2(0x108) && emc2(0x10a) && emc2(0x10c);
            }
            case 'c':
            case 0xe7:
            case 0x107: case 0x109: case 0x10b: case 0x10d:
            {
                return emc2('c')
                    && emc2(0xe7)
                    && emc2(0x107) && emc2(0x109) && emc2(0x10b) && emc2(0x10d);
            }

            case 'D':
            case 0x10e: case 0x110:
            case 0x1e0a: case 0x1e0c: case 0x1e0e: case 0x1e10: case 0x1e12:
            {
                return emc2('D')
                    && emc2(0x10e) && emc2(0x110)
                    && emc2(0x1e0a) && emc2(0x1e0c) && emc2(0x1e0e) && emc2(0x1e10) && emc2(0x1e12);
            }
            case 'd':
            case 0x10f: case 0x111:
            case 0x1e0b: case 0x1e0d: case 0x1e0f: case 0x1e11: case 0x1e13:
            {
                return emc2('d')
                    && emc2(0x10f) && emc2(0x111)
                    && emc2(0x1e0b) && emc2(0x1e0d) && emc2(0x1e0f) && emc2(0x1e11) && emc2(0x1e13);
            }

            case 'E':
            case 0xc8: case 0xc9: case 0xca: case 0xcb:
            case 0x112: case 0x114: case 0x116: case 0x118: case 0x11a:
            case 0x1eba: case 0x1ebc:
            {
                return emc2('E')
                    && emc2(0xc8) && emc2(0xc9) && emc2(0xca) && emc2(0xcb)
                    && emc2(0x112) && emc2(0x114) && emc2(0x116) && emc2(0x118) && emc2(0x11a)
                    && emc2(0x1eba) && emc2(0x1ebc);
            }
            case 'e':
            case 0xe8: case 0xe9: case 0xea: case 0xeb:
            case 0x113: case 0x115: case 0x117: case 0x119: case 0x11b:
            case 0x1ebb: case 0x1ebd:
            {
                return emc2('e')
                    && emc2(0xe8) && emc2(0xe9) && emc2(0xea) && emc2(0xeb)
                    && emc2(0x113) && emc2(0x115) && emc2(0x117) && emc2(0x119) && emc2(0x11b)
                    && emc2(0x1ebb) && emc2(0x1ebd);
            }

            case 'F':
            case 0x1e1e:
            {
                return emc2('F')
                    && emc2(0x1e1e);
            }
            case 'f':
            case 0x1e1f:
            {
                return emc2('f')
                    && emc2(0x1e1f);
            }

            case 'G':
            case 0x11c: case 0x11e: case 0x120: case 0x122:
            case 0x1e4: case 0x1e6: case 0x1f4:
            case 0x1e20:
            {
                return emc2('G')
                    && emc2(0x11c) && emc2(0x11e) && emc2(0x120) && emc2(0x122)
                    && emc2(0x1e4) && emc2(0x1e6) && emc2(0x1f4)
                    && emc2(0x1e20);
            }
            case 'g':
            case 0x11d: case 0x11f: case 0x121: case 0x123:
            case 0x1e5: case 0x1e7: case 0x1f5:
            case 0x1e21:
            {
                return emc2('g')
                    && emc2(0x11d) && emc2(0x11f) && emc2(0x121) && emc2(0x123)
                    && emc2(0x1e5) && emc2(0x1e7) && emc2(0x1f5)
                    && emc2(0x1e21);
            }

            case 'H':
            case 0x124: case 0x126:
            case 0x1e22: case 0x1e26: case 0x1e28:
            {
                return emc2('H')
                    && emc2(0x124) && emc2(0x126)
                    && emc2(0x1e22) && emc2(0x1e26) && emc2(0x1e28);
            }
            case 'h':
            case 0x125: case 0x127:
            case 0x1e23: case 0x1e27: case 0x1e29: case 0x1e96:
            {
                return emc2('h')
                    && emc2(0x125) && emc2(0x127)
                    && emc2(0x1e23) && emc2(0x1e27) && emc2(0x1e29) && emc2(0x1e96);
            }

            case 'I':
            case 0xcc: case 0xcd: case 0xce: case 0xcf:
            case 0x128: case 0x12a: case 0x12c: case 0x12e: case 0x130:
            case 0x1cf:
            case 0x1ec8:
            {
                return emc2('I')
                    && emc2(0xcc) && emc2(0xcd) && emc2(0xce) && emc2(0xcf)
                    && emc2(0x128) && emc2(0x12a) && emc2(0x12c) && emc2(0x12e) && emc2(0x130)
                    && emc2(0x1cf)
                    && emc2(0x1ec8);
            }
            case 'i':
            case 0xec: case 0xed: case 0xee: case 0xef:
            case 0x129: case 0x12b: case 0x12d: case 0x12f: case 0x131:
            case 0x1d0:
            case 0x1ec9:
            {
                return emc2('i')
                    && emc2(0xec) && emc2(0xed) && emc2(0xee) && emc2(0xef)
                    && emc2(0x129) && emc2(0x12b) && emc2(0x12d) && emc2(0x12f) && emc2(0x131)
                    && emc2(0x1d0)
                    && emc2(0x1ec9);
            }

            case 'J':
            case 0x134:
            {
                return emc2('J')
                    && emc2(0x134);
            }
            case 'j':
            case 0x135: case 0x1f0:
            {
                return emc2('j')
                    && emc2(0x135) && emc2(0x1f0);
            }

            case 'K':
            case 0x136: case 0x1e8:
            case 0x1e30: case 0x1e34:
            {
                return emc2('K')
                    && emc2(0x136) && emc2(0x1e8)
                    && emc2(0x1e30) && emc2(0x1e34);
            }
            case 'k':
            case 0x137: case 0x1e9:
            case 0x1e31: case 0x1e35:
            {
                return emc2('k')
                    && emc2(0x137) && emc2(0x1e9)
                    && emc2(0x1e31) && emc2(0x1e35);
            }

            case 'L':
            case 0x139: case 0x13b: case 0x13d: case 0x13f: case 0x141:
            case 0x1e3a:
            {
                return emc2('L')
                    && emc2(0x139) && emc2(0x13b) && emc2(0x13d) && emc2(0x13f) && emc2(0x141)
                    && emc2(0x1e3a);
            }
            case 'l':
            case 0x13a: case 0x13c: case 0x13e: case 0x140: case 0x142:
            case 0x1e3b:
            {
                return emc2('l')
                    && emc2(0x13a) && emc2(0x13c) && emc2(0x13e) && emc2(0x140) && emc2(0x142)
                    && emc2(0x1e3b);
            }

            case 'M':
            case 0x1e3e: case 0x1e40:
            {
                return emc2('M')
                    && emc2(0x1e3e) && emc2(0x1e40);
            }
            case 'm':
            case 0x1e3f: case 0x1e41:
            {
                return emc2('m')
                    && emc2(0x1e3f) && emc2(0x1e41);
            }

            case 'N':
            case 0xd1:
            case 0x143: case 0x145: case 0x147:
            case 0x1e44: case 0x1e48:
            {
                return emc2('N')
                    && emc2(0xd1)
                    && emc2(0x143) && emc2(0x145) && emc2(0x147)
                    && emc2(0x1e44) && emc2(0x1e48);
            }
            case 'n':
            case 0xf1:
            case 0x144: case 0x146: case 0x148: case 0x149:
            case 0x1e45: case 0x1e49:
            {
                return emc2('n')
                    && emc2(0xf1)
                    && emc2(0x144) && emc2(0x146) && emc2(0x148) && emc2(0x149)
                    && emc2(0x1e45) && emc2(0x1e49);
            }

            case 'O':
            case 0xd2: case 0xd3: case 0xd4:
            case 0xd5: case 0xd6: case 0xd8:
            case 0x14c: case 0x14e: case 0x150:
            case 0x1a0: case 0x1d1: case 0x1ea: case 0x1ec:
            case 0x1ece:
            {
                return emc2('O')
                    && emc2(0xd2) && emc2(0xd3) && emc2(0xd4)
                    && emc2(0xd5) && emc2(0xd6) && emc2(0xd8)
                    && emc2(0x14c) && emc2(0x14e) && emc2(0x150)
                    && emc2(0x1a0) && emc2(0x1d1) && emc2(0x1ea) && emc2(0x1ec)
                    && emc2(0x1ece);
            }
            case 'o':
            case 0xf2: case 0xf3: case 0xf4:
            case 0xf5: case 0xf6: case 0xf8:
            case 0x14d: case 0x14f: case 0x151:
            case 0x1a1: case 0x1d2: case 0x1eb: case 0x1ed:
            case 0x1ecf:
            {
                return emc2('o')
                    && emc2(0xf2) && emc2(0xf3) && emc2(0xf4)
                    && emc2(0xf5) && emc2(0xf6) && emc2(0xf8)
                    && emc2(0x14d) && emc2(0x14f) && emc2(0x151)
                    && emc2(0x1a1) && emc2(0x1d2) && emc2(0x1eb) && emc2(0x1ed)
                    && emc2(0x1ecf);
            }

            case 'P':
            case 0x1e54: case 0x1e56:
            {
                return emc2('P')
                    && emc2(0x1e54) && emc2(0x1e56);
            }
            case 'p':
            case 0x1e55: case 0x1e57:
            {
                return emc2('p')
                    && emc2(0x1e55) && emc2(0x1e57);
            }

            case 'R':
            case 0x154: case 0x156: case 0x158:
            case 0x1e58: case 0x1e5e:
            {
                return emc2('R')
                    && emc2(0x154) && emc2(0x156) && emc2(0x158)
                    && emc2(0x1e58) && emc2(0x1e5e);
            }
            case 'r':
            case 0x155: case 0x157: case 0x159:
            case 0x1e59: case 0x1e5f:
            {
                return emc2('r')
                    && emc2(0x155) && emc2(0x157) && emc2(0x159)
                    && emc2(0x1e59) && emc2(0x1e5f);
            }

            case 'S':
            case 0x15a: case 0x15c: case 0x15e: case 0x160:
            case 0x1e60:
            {
                return emc2('S')
                    && emc2(0x15a) && emc2(0x15c) && emc2(0x15e) && emc2(0x160)
                    && emc2(0x1e60);
            }
            case 's':
            case 0x15b: case 0x15d: case 0x15f: case 0x161:
            case 0x1e61:
            {
                return emc2('s')
                    && emc2(0x15b) && emc2(0x15d) && emc2(0x15f) && emc2(0x161)
                    && emc2(0x1e61);
            }

            case 'T':
            case 0x162: case 0x164: case 0x166:
            case 0x1e6a: case 0x1e6e:
            {
                return emc2('T')
                    && emc2(0x162) && emc2(0x164) && emc2(0x166)
                    && emc2(0x1e6a) && emc2(0x1e6e);
            }
            case 't':
            case 0x163: case 0x165: case 0x167:
            case 0x1e6b: case 0x1e6f: case 0x1e97:
            {
                return emc2('t')
                    && emc2(0x163) && emc2(0x165) && emc2(0x167)
                    && emc2(0x1e6b) && emc2(0x1e6f) && emc2(0x1e97);
            }

            case 'U':
            case 0xd9: case 0xda: case 0xdb: case 0xdc:
            case 0x168: case 0x16a: case 0x16c: case 0x16e:
            case 0x170: case 0x172: case 0x1af: case 0x1d3:
            case 0x1ee6:
            {
                return emc2('U')
                    && emc2(0xd9) && emc2(0xda) && emc2(0xdb) && emc2(0xdc)
                    && emc2(0x168) && emc2(0x16a) && emc2(0x16c) && emc2(0x16e)
                    && emc2(0x170) && emc2(0x172) && emc2(0x1af) && emc2(0x1d3)
                    && emc2(0x1ee6);
            }
            case 'u':
            case 0xf9: case 0xfa: case 0xfb: case 0xfc:
            case 0x169: case 0x16b: case 0x16d: case 0x16f:
            case 0x171: case 0x173: case 0x1b0: case 0x1d4:
            case 0x1ee7:
            {
                return emc2('u')
                    && emc2(0xf9) && emc2(0xfa) && emc2(0xfb) && emc2(0xfc)
                    && emc2(0x169) && emc2(0x16b) && emc2(0x16d) && emc2(0x16f)
                    && emc2(0x171) && emc2(0x173) && emc2(0x1b0) && emc2(0x1d4)
                    && emc2(0x1ee7);
            }

            case 'V':
            case 0x1e7c:
            {
                return emc2('V')
                    && emc2(0x1e7c);
            }
            case 'v':
            case 0x1e7d:
            {
                return emc2('v')
                    && emc2(0x1e7d);
            }

            case 'W':
            case 0x174:
            case 0x1e80: case 0x1e82: case 0x1e84: case 0x1e86:
            {
                return emc2('W')
                    && emc2(0x174)
                    && emc2(0x1e80) && emc2(0x1e82) && emc2(0x1e84) && emc2(0x1e86);
            }
            case 'w':
            case 0x175:
            case 0x1e81: case 0x1e83: case 0x1e85: case 0x1e87: case 0x1e98:
            {
                return emc2('w')
                    && emc2(0x175)
                    && emc2(0x1e81) && emc2(0x1e83) && emc2(0x1e85) && emc2(0x1e87) && emc2(0x1e98);
            }

            case 'X':
            case 0x1e8a: case 0x1e8c:
            {
                return emc2('X')
                    && emc2(0x1e8a) && emc2(0x1e8c);
            }
            case 'x':
            case 0x1e8b: case 0x1e8d:
            {
                return emc2('x')
                    && emc2(0x1e8b) && emc2(0x1e8d);
            }

            case 'Y':
            case 0xdd:
            case 0x176: case 0x178:
            case 0x1e8e: case 0x1ef2: case 0x1ef6: case 0x1ef8:
            {
                return emc2('Y')
                    && emc2(0xdd)
                    && emc2(0x176) && emc2(0x178)
                    && emc2(0x1e8e) && emc2(0x1ef2) && emc2(0x1ef6) && emc2(0x1ef8);
            }
            case 'y':
            case 0xfd: case 0xff:
            case 0x177:
            case 0x1e8f: case 0x1e99: case 0x1ef3: case 0x1ef7: case 0x1ef9:
            {
                return emc2('y')
                    && emc2(0xfd) && emc2(0xff)
                    && emc2(0x177)
                    && emc2(0x1e8f) && emc2(0x1e99) && emc2(0x1ef3) && emc2(0x1ef7) && emc2(0x1ef9);
            }

            case 'Z':
            case 0x179: case 0x17b: case 0x17d: case 0x1b5:
            case 0x1e90: case 0x1e94:
            {
                return emc2('Z')
                    && emc2(0x179) && emc2(0x17b) && emc2(0x17d) && emc2(0x1b5)
                    && emc2(0x1e90) && emc2(0x1e94);
            }
            case 'z':
            case 0x17a: case 0x17c: case 0x17e: case 0x1b6:
            case 0x1e91: case 0x1e95:
            {
                return emc2('z')
                    && emc2(0x17a) && emc2(0x17c) && emc2(0x17e) && emc2(0x1b6)
                    && emc2(0x1e91) && emc2(0x1e95);
            }

            /* default: character itself */
        }

        return emc2(c);
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
    /*private*/ static boolean nfa_regatom()
    {
        Bytes old_regparse = regparse;
        int extra = 0;
        int startc = -1;
        int endc = -1;
        int oldstartc = -1;

        int c = getchr();

        collection:
        {
            switch (c)
            {
                case NUL:
                    emsg(e_nul_found);
                    rc_did_emsg = true;
                    return false;

                case -162: // case Magic('^'):
                    emc1(NFA_BOL);
                    break;

                case -220: // case Magic('$'):
                    emc1(NFA_EOL);
                    had_eol = true;
                    break;

                case -196: // case Magic('<'):
                    emc1(NFA_BOW);
                    break;

                case -194: // case Magic('>'):
                    emc1(NFA_EOW);
                    break;

                case -161: // case Magic('_'):
                {
                    c = no_Magic(getchr());
                    if (c == NUL)
                    {
                        emsg(e_nul_found);
                        rc_did_emsg = true;
                        return false;
                    }

                    if (c == '^')       /* "\_^" is start-of-line */
                    {
                        emc1(NFA_BOL);
                        break;
                    }
                    if (c == '$')       /* "\_$" is end-of-line */
                    {
                        emc1(NFA_EOL);
                        had_eol = true;
                        break;
                    }

                    extra = NFA_ADD_NL;

                    /* "\_[" is collection plus newline */
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
                        if (extra == NFA_ADD_NL)
                        {
                            emsgn(e_ill_char_class, (long)c);
                            rc_did_emsg = true;
                            return false;
                        }
                        emsgn(u8("INTERNAL: Unknown character class char: %ld"), (long)c);
                        return false;
                    }
                    /* When '.' is followed by a composing char ignore the dot,
                     * so that the composing char is matched here. */
                    if (c == Magic('.') && utf_iscomposing(peekchr()))
                    {
                        old_regparse = regparse;
                        c = getchr();
                        return nfa_do_multibyte(c, old_regparse);
                    }
                    emc1(nfa_classcodes[BDIFF(p, classchars)]);
                    if (extra == NFA_ADD_NL)
                    {
                        emc1(NFA_NEWL);
                        emc1(NFA_OR);
                        regflags |= RF_HASNL;
                    }
                    break;
                }

                case -146: // case Magic('n'):
                {
                    if (reg_string)
                        /* In a string "\n" matches a newline character. */
                        emc1(NL);
                    else
                    {
                        /* In buffer text "\n" matches the end of a line. */
                        emc1(NFA_NEWL);
                        regflags |= RF_HASNL;
                    }
                    break;
                }

                case -216: // case Magic('('):
                    if (nfa_reg(REG_PAREN) == false)
                        return false;           /* cascaded error */
                    break;

                case -132: // case Magic('|'):
                case -218: // case Magic('&'):
                case -215: // case Magic(')'):
                    emsgn(e_misplaced, (long)no_Magic(c));
                    return false;

                case -195: // case Magic('='):
                case -193: // case Magic('?'):
                case -213: // case Magic('+'):
                case -192: // case Magic('@'):
                case -214: // case Magic('*'):
                case -133: // case Magic('{'):
                    /* these should follow an atom, not form an atom */
                    emsgn(e_misplaced, (long)no_Magic(c));
                    return false;

                case -130: // case Magic('~'):
                {
                    /* Previous substitute pattern.
                     * Generated as "\%(pattern\)". */
                    if (reg_prev_sub == null)
                    {
                        emsg(e_nopresub);
                        return false;
                    }
                    for (Bytes lp = reg_prev_sub; lp.at(0) != NUL; lp = lp.plus(us_ptr2len(lp)))
                    {
                        emc1(us_ptr2char(lp));
                        if (BNE(lp, reg_prev_sub))
                            emc1(NFA_CONCAT);
                    }
                    emc1(NFA_NOPEN);
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
                    emc1(NFA_BACKREF1 + (no_Magic(c) - '1'));
                    nfa_has_backref = true;
                    break;

                case -134: // case Magic('z'):
                {
                    c = no_Magic(getchr());
                    switch (c)
                    {
                        case 's':
                            emc1(NFA_ZSTART);
                            if (re_mult_next(u8("\\zs")) == false)
                                return false;
                            break;

                        case 'e':
                            emc1(NFA_ZEND);
                            nfa_has_zend = true;
                            if (re_mult_next(u8("\\ze")) == false)
                                return false;
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
                        {
                            /* \z1...\z9 */
                            if (reg_do_extmatch != REX_USE)
                            {
                                emsg(e_z1_not_allowed);
                                rc_did_emsg = true;
                                return false;
                            }
                            emc1(NFA_ZREF1 + (no_Magic(c) - '1'));
                            /* No need to set nfa_has_backref, the sub-matches
                             * don't change when \z1 .. \z9 matches or not. */
                            re_has_z = REX_USE;
                            break;
                        }

                        case '(':
                        {
                            /* \z( */
                            if (reg_do_extmatch != REX_SET)
                            {
                                emsg(e_z_not_allowed);
                                rc_did_emsg = true;
                                return false;
                            }
                            if (nfa_reg(REG_ZPAREN) == false)
                                return false;           /* cascaded error */
                            re_has_z = REX_SET;
                            break;
                        }

                        default:
                            emsgn(u8("E867: (NFA) Unknown operator '\\z%c'"), (long)no_Magic(c));
                            return false;
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
                            if (nfa_reg(REG_NPAREN) == false)
                                return false;
                            emc1(NFA_NOPEN);
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
                            {
                                emsg2(u8("E678: Invalid character after %s%%[dxouU]"), (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                                rc_did_emsg = true;
                                return false;
                            }
                            /* A NUL is stored in the text as NL. */
                            /* TODO: what if a composing character follows? */
                            emc1(nr == 0 ? 0x0a : nr);
                            break;
                        }

                        /* Catch \%^ and \%$ regardless of where they appear in the
                         * pattern -- regardless of whether or not it makes sense. */
                        case '^':
                            emc1(NFA_BOF);
                            break;

                        case '$':
                            emc1(NFA_EOF);
                            break;

                        case '#':
                            emc1(NFA_CURSOR);
                            break;

                        case 'V':
                            emc1(NFA_VISUAL);
                            break;

                        case 'C':
                            emc1(NFA_ANY_COMPOSING);
                            break;

                        case '[':
                        {
                            int n;

                            /* \%[abc] */
                            for (n = 0; (c = peekchr()) != ']'; n++)
                            {
                                if (c == NUL)
                                {
                                    emsg2(e_missing_sb, (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                                    rc_did_emsg = true;
                                    return false;
                                }
                                /* recursive call! */
                                if (nfa_regatom() == false)
                                    return false;
                            }
                            getchr();       /* get the ] */
                            if (n == 0)
                            {
                                emsg2(e_empty_sb, (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
                                rc_did_emsg = true;
                                return false;
                            }
                            emc1(NFA_OPT_CHARS);
                            emc1(n);

                            /* Emit as "\%(\%[abc]\)" to be able to handle "\%[abc]*" which would
                             * cause the empty string to be matched an unlimited number of times.
                             * NFA_NOPEN is added only once at a position, while NFA_SPLIT is added
                             * multiple times.  This is more efficient than not allowing NFA_SPLIT
                             * multiple times, it is used a lot.
                             */
                            emc1(NFA_NOPEN);
                            break;
                        }

                        default:
                        {
                            int n = 0;
                            int cmp = c;

                            if (c == '<' || c == '>')
                                c = getchr();
                            while (asc_isdigit(c))
                            {
                                n = n * 10 + (c - '0');
                                c = getchr();
                            }
                            if (c == 'l' || c == 'c' || c == 'v')
                            {
                                if (c == 'l')
                                    /* \%{n}l  \%{n}<l  \%{n}>l */
                                    emc1(cmp == '<' ? NFA_LNUM_LT : cmp == '>' ? NFA_LNUM_GT : NFA_LNUM);
                                else if (c == 'c')
                                    /* \%{n}c  \%{n}<c  \%{n}>c */
                                    emc1(cmp == '<' ? NFA_COL_LT : cmp == '>' ? NFA_COL_GT : NFA_COL);
                                else
                                    /* \%{n}v  \%{n}<v  \%{n}>v */
                                    emc1(cmp == '<' ? NFA_VCOL_LT : cmp == '>' ? NFA_VCOL_GT : NFA_VCOL);
                                emc1(n);
                                break;
                            }
                            else if (c == '\'' && n == 0)
                            {
                                /* \%'m  \%<'m  \%>'m */
                                emc1(cmp == '<' ? NFA_MARK_LT : cmp == '>' ? NFA_MARK_GT : NFA_MARK);
                                emc1(getchr());
                                break;
                            }
                            emsgn(u8("E867: (NFA) Unknown operator '\\%%%c'"), (long)no_Magic(c));
                            return false;
                        }
                    }
                    break;
                }

                case -165: // case Magic('['):
                    break collection;

                default:
                    return nfa_do_multibyte(c, old_regparse);
            }

            return true;
        }

        /*
         * [abc]  uses NFA_START_COLL - NFA_END_COLL
         * [^abc] uses NFA_START_NEG_COLL - NFA_END_NEG_COLL
         * Each character is produced as a regular state,
         * using NFA_CONCAT to bind them together.
         * Besides normal characters there can be:
         * - character classes  NFA_CLASS_*
         * - ranges, two characters followed by NFA_RANGE.
         */
        Bytes p = regparse;
        Bytes endp = skip_anyof(p);

        if (endp.at(0) == (byte)']')
        {
            /*
             * Try to reverse engineer character classes.  For example,
             * recognize that [0-9] stands for \d and [A-Za-z_] for \h,
             * and perform the necessary substitutions in the NFA.
             */
            int result = nfa_recognize_char_class(regparse, endp, extra == NFA_ADD_NL);
            if (result != 0)
            {
                if (NFA_FIRST_NL <= result && result <= NFA_LAST_NL)
                {
                    emc1(result - NFA_ADD_NL);
                    emc1(NFA_NEWL);
                    emc1(NFA_OR);
                }
                else
                    emc1(result);
                regparse = endp;
                regparse = regparse.plus(us_ptr2len_cc(regparse));
                return true;
            }

            /*
             * Failed to recognize a character class.
             * Use the simple version that turns [abc] into 'a' OR 'b' OR 'c'.
             */
            startc = endc = oldstartc = -1;
            boolean negated = false;
            if (regparse.at(0) == (byte)'^')                   /* negated range */
            {
                negated = true;
                regparse = regparse.plus(us_ptr2len_cc(regparse));
                emc1(NFA_START_NEG_COLL);
            }
            else
                emc1(NFA_START_COLL);
            if (regparse.at(0) == (byte)'-')
            {
                startc = '-';
                emc1(startc);
                emc1(NFA_CONCAT);
                regparse = regparse.plus(us_ptr2len_cc(regparse));
            }
            /* Emit the OR branches for each character in the []. */
            boolean emit_range = false;
            while (BLT(regparse, endp))
            {
                oldstartc = startc;
                startc = -1;
                boolean got_coll_char = false;
                if (regparse.at(0) == (byte)'[')
                {
                    /* Check for [: :], [= =], [. .]. */
                    int charclass, equiclass = 0, collclass = 0;
                    { Bytes[] __ = { regparse }; charclass = get_char_class(__); regparse = __[0]; }
                    if (charclass == CLASS_NONE)
                    {
                        { Bytes[] __ = { regparse }; equiclass = get_equi_class(__); regparse = __[0]; }
                        if (equiclass == 0)
                        {
                            { Bytes[] __ = { regparse }; collclass = get_coll_element(__); regparse = __[0]; }
                        }
                    }

                    /* Character class like [:alpha:]. */
                    if (charclass != CLASS_NONE)
                    {
                        switch (charclass)
                        {
                            case CLASS_ALNUM:
                                emc1(NFA_CLASS_ALNUM);
                                break;
                            case CLASS_ALPHA:
                                emc1(NFA_CLASS_ALPHA);
                                break;
                            case CLASS_BLANK:
                                emc1(NFA_CLASS_BLANK);
                                break;
                            case CLASS_CNTRL:
                                emc1(NFA_CLASS_CNTRL);
                                break;
                            case CLASS_DIGIT:
                                emc1(NFA_CLASS_DIGIT);
                                break;
                            case CLASS_GRAPH:
                                emc1(NFA_CLASS_GRAPH);
                                break;
                            case CLASS_LOWER:
                                emc1(NFA_CLASS_LOWER);
                                break;
                            case CLASS_PRINT:
                                emc1(NFA_CLASS_PRINT);
                                break;
                            case CLASS_PUNCT:
                                emc1(NFA_CLASS_PUNCT);
                                break;
                            case CLASS_SPACE:
                                emc1(NFA_CLASS_SPACE);
                                break;
                            case CLASS_UPPER:
                                emc1(NFA_CLASS_UPPER);
                                break;
                            case CLASS_XDIGIT:
                                emc1(NFA_CLASS_XDIGIT);
                                break;
                            case CLASS_TAB:
                                emc1(NFA_CLASS_TAB);
                                break;
                            case CLASS_RETURN:
                                emc1(NFA_CLASS_RETURN);
                                break;
                            case CLASS_BACKSPACE:
                                emc1(NFA_CLASS_BACKSPACE);
                                break;
                            case CLASS_ESCAPE:
                                emc1(NFA_CLASS_ESCAPE);
                                break;
                        }
                        emc1(NFA_CONCAT);
                        continue;
                    }
                    /* Try equivalence class [=a=] and the like. */
                    if (equiclass != 0)
                    {
                        if (nfa_emit_equi_class(equiclass) == false)
                        {
                            /* should never happen */
                            emsg(u8("E868: Error building NFA with equivalence class!"));
                            rc_did_emsg = true;
                            return false;
                        }
                        continue;
                    }
                    /* Try collating class like [. .]. */
                    if (collclass != 0)
                    {
                        startc = collclass;     /* allow [.a.]-x as a range */
                        /* Will emit the proper atom at the end of the while loop. */
                    }
                }
                /* Try a range like 'a-x' or '\t-z'.  Also allows '-' as a start character. */
                if (regparse.at(0) == (byte)'-' && oldstartc != -1)
                {
                    emit_range = true;
                    startc = oldstartc;
                    regparse = regparse.plus(us_ptr2len_cc(regparse));
                    continue;           /* reading the end of the range */
                }

                /* Now handle simple and escaped characters.
                 * Only "\]", "\^", "\]" and "\\" are special in Vi.
                 * Vim accepts "\t", "\e", etc., but only when the 'l' flag in 'cpoptions' is not included.
                 * Posix doesn't recognize backslash at all.
                 */
                if (regparse.at(0) == (byte)'\\'
                        && !reg_cpo_bsl
                        && BLE(regparse.plus(1), endp)
                        && (vim_strchr(REGEXP_INRANGE, regparse.at(1)) != null
                            || (!reg_cpo_lit && vim_strchr(REGEXP_ABBR, regparse.at(1)) != null)))
                {
                    regparse = regparse.plus(us_ptr2len_cc(regparse));

                    if (regparse.at(0) == (byte)'n')
                        startc = reg_string ? NL : NFA_NEWL;
                    else if (regparse.at(0) == (byte)'d'
                          || regparse.at(0) == (byte)'o'
                          || regparse.at(0) == (byte)'x'
                          || regparse.at(0) == (byte)'u'
                          || regparse.at(0) == (byte)'U')
                    {
                        /* TODO(RE) This needs more testing. */
                        startc = coll_get_char();
                        got_coll_char = true;
                        regparse = regparse.minus(us_ptr_back(old_regparse, regparse));
                    }
                    else
                    {
                        /* \r,\t,\e,\b */
                        startc = backslash_trans(regparse.at(0));
                    }
                }

                /* Normal printable char. */
                if (startc == -1)
                    startc = us_ptr2char(regparse);

                /* Previous char was '-', so this char is end of range. */
                if (emit_range)
                {
                    endc = startc;
                    startc = oldstartc;
                    if (endc < startc)
                    {
                        emsg(e_invrange);
                        rc_did_emsg = true;
                        return false;
                    }

                    if (startc + 2 < endc)
                    {
                        /* Emit a range instead of the sequence of individual characters. */
                        if (startc == 0)
                            /* \x00 is translated to \x0a, start at \x01. */
                            emc1(1);
                        else
                            --post_index; /* remove NFA_CONCAT */
                        emc1(endc);
                        emc1(NFA_RANGE);
                        emc1(NFA_CONCAT);
                    }
                    else if (1 < utf_char2len(startc) || 1 < utf_char2len(endc))
                    {
                        /* Emit the characters in the range.
                         * "startc" was already emitted, so skip it.
                         */
                        for (c = startc + 1; c <= endc; c++)
                        {
                            emc1(c);
                            emc1(NFA_CONCAT);
                        }
                    }
                    else
                    {
                        /* Emit the range. "startc" was already emitted, so skip it. */
                        for (c = startc + 1; c <= endc; c++)
                        {
                            emc1(c);
                            emc1(NFA_CONCAT);
                        }
                    }
                    emit_range = false;
                    startc = -1;
                }
                else
                {
                    /* This char (startc) is not part of a range.  Just emit it.
                     * Normally, simply emit startc.  But if we get char
                     * code=0 from a collating char, then replace it with 0x0a.
                     * This is needed to completely mimic the behaviour of
                     * the backtracking engine. */
                    if (startc == NFA_NEWL)
                    {
                        /* Line break can't be matched as part of the collection,
                         * add an OR below.  But not for negated range. */
                        if (!negated)
                            extra = NFA_ADD_NL;
                    }
                    else
                    {
                        if (got_coll_char == true && startc == 0)
                            emc1(0x0a);
                        else
                            emc1(startc);
                        emc1(NFA_CONCAT);
                    }
                }

                regparse = regparse.plus(us_ptr2len_cc(regparse));
            }

            regparse = regparse.minus(us_ptr_back(old_regparse, regparse));
            if (regparse.at(0) == (byte)'-')       /* if last, '-' is just a char */
            {
                emc1('-');
                emc1(NFA_CONCAT);
            }

            /* skip the trailing ] */
            regparse = endp;
            regparse = regparse.plus(us_ptr2len_cc(regparse));

            /* Mark end of the collection. */
            if (negated == true)
                emc1(NFA_END_NEG_COLL);
            else
                emc1(NFA_END_COLL);

            /* \_[] also matches \n but it's not negated */
            if (extra == NFA_ADD_NL)
            {
                emc1(reg_string ? NL : NFA_NEWL);
                emc1(NFA_OR);
            }

            return true;
        }

        if (reg_strict)
        {
            emsg(e_missingbracket);
            rc_did_emsg = true;
            return false;
        }

        return nfa_do_multibyte(c, old_regparse);
    }

    /*private*/ static final boolean nfa_do_multibyte(int c, Bytes old_regparse)
    {
        int plen;

        /* plen is length of current char with composing chars */
        if (utf_char2len(c) != (plen = us_ptr2len_cc(old_regparse)) || utf_iscomposing(c))
        {
            int i = 0;

            /* A base character plus composing characters, or just one or more
             * composing characters.  This requires creating a separate atom
             * as if enclosing the characters in (), where NFA_COMPOSING is
             * the ( and NFA_END_COMPOSING is the ).
             * Note that right now we are building the postfix form, not the
             * NFA itself; a composing char could be: a, b, c, NFA_COMPOSING
             * where 'b' and 'c' are chars with codes > 256. */
            for ( ; ; )
            {
                emc1(c);
                if (0 < i)
                    emc1(NFA_CONCAT);
                if (plen <= (i += utf_char2len(c)))
                    break;
                c = us_ptr2char(old_regparse.plus(i));
            }
            emc1(NFA_COMPOSING);
            regparse = old_regparse.plus(plen);
        }
        else
        {
            c = no_Magic(c);
            emc1(c);
        }

        return true;
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
    /*private*/ static boolean nfa_regpiece()
    {
        /* Save the current parse state, so that we can use it if <atom>{m,n} is next. */
        parse_state_C old_state = new parse_state_C();
        save_parse_state(old_state);

        /* store current pos in the postfix form, for \{m,n} involving 0s */
        int my_post_start = post_index;

        boolean ret = nfa_regatom();
        if (ret == false)
            return false;           /* cascaded error */

        int op = peekchr();
        if (re_multi_type(op) == NOT_MULTI)
            return true;

        skipchr();
        switch (op)
        {
            case -214: // case Magic('*'):
            {
                emc1(NFA_STAR);
                break;
            }

            case -213: // case Magic('+'):
            {
                /*
                 * Trick: Normally, (a*)\+ would match the whole input "aaa".  The first and
                 * only submatch would be "aaa".  But the backtracking engine interprets the
                 * plus as "try matching one more time", and a* matches a second time at the
                 * end of the input, the empty string.  The submatch will be the empty string.
                 *
                 * In order to be consistent with the old engine, we replace
                 * <atom>+ with <atom><atom>*
                 */
                restore_parse_state(old_state);
                curchr = -1;
                if (nfa_regatom() == false)
                    return false;
                emc1(NFA_STAR);
                emc1(NFA_CONCAT);
                skipchr();          /* skip the \+ */
                break;
            }

            case -192: // case Magic('@'):
            {
                int c2 = getdecchrs();
                op = no_Magic(getchr());
                int i = 0;
                switch (op)
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
                    emsgn(u8("E869: (NFA) Unknown operator '\\@%c'"), (long)op);
                    return false;
                }
                emc1(i);
                if (i == NFA_PREV_ATOM_JUST_BEFORE || i == NFA_PREV_ATOM_JUST_BEFORE_NEG)
                    emc1(c2);
                break;
            }

            case -193: // case Magic('?'):
            case -195: // case Magic('='):
            {
                emc1(NFA_QUEST);
                break;
            }

            case -133: // case Magic('{'):
            {
                /* a{2,5} will expand to 'aaa?a?a?'
                 * a{-1,3} will expand to 'aa??a??', where ?? is the nongreedy
                 * version of '?'
                 * \v(ab){2,3} will expand to '(ab)(ab)(ab)?', where all the
                 * parenthesis have the same id
                 */
                boolean greedy = true;      /* Braces are prefixed with '-' ? */
                int c2 = peekchr();
                if (c2 == '-' || c2 == Magic('-'))
                {
                    skipchr();
                    greedy = false;
                }
                long[] minval = new long[1];
                long[] maxval = new long[1];
                if (!read_limits(minval, maxval))
                {
                    emsg(u8("E870: (NFA regexp) Error reading repetition limits"));
                    rc_did_emsg = true;
                    return false;
                }

                /*  <atom>{0,inf}, <atom>{0,} and <atom>{}  are equivalent to
                 *  <atom>*  */
                if (minval[0] == 0 && maxval[0] == MAX_LIMIT)
                {
                    if (greedy)             /* { { (match the braces) */
                        /* \{}, \{0,} */
                        emc1(NFA_STAR);
                    else                    /* { { (match the braces) */
                        /* \{-}, \{-0,} */
                        emc1(NFA_STAR_NONGREEDY);
                    break;
                }

                /* Special case: x{0} or x{-0}. */
                if (maxval[0] == 0)
                {
                    /* Ignore result of previous call to nfa_regatom(). */
                    post_index = my_post_start;
                    /* NFA_EMPTY is 0-length and works everywhere. */
                    emc1(NFA_EMPTY);
                    return true;
                }

                /* The engine is very inefficient (uses too many states) when the
                 * maximum is much larger than the minimum and when the maximum is
                 * large.  Bail out if we can use the other engine. */
                if ((nfa_re_flags & RE_AUTO) != 0 && (minval[0] + 200 < maxval[0] || 500 < maxval[0]))
                    return false;

                /* Ignore previous call to nfa_regatom(). */
                post_index = my_post_start;
                /* Save parse state after the repeated atom and the \{}. */
                parse_state_C new_state = new parse_state_C();
                save_parse_state(new_state);

                int quest = (greedy == true) ? NFA_QUEST : NFA_QUEST_NONGREEDY;
                for (int i = 0; i < maxval[0]; i++)
                {
                    /* Goto beginning of the repeated atom. */
                    restore_parse_state(old_state);
                    int old_post_pos = post_index;
                    if (nfa_regatom() == false)
                        return false;

                    /* after "minval" times, atoms are optional */
                    if (minval[0] < i + 1)
                    {
                        if (maxval[0] == MAX_LIMIT)
                        {
                            if (greedy)
                                emc1(NFA_STAR);
                            else
                                emc1(NFA_STAR_NONGREEDY);
                        }
                        else
                            emc1(quest);
                    }
                    if (old_post_pos != my_post_start)
                        emc1(NFA_CONCAT);
                    if (minval[0] < i + 1 && maxval[0] == MAX_LIMIT)
                        break;
                }

                /* Go to just after the repeated atom and the \{}. */
                restore_parse_state(new_state);
                curchr = -1;
                break;
            }

            default:
                break;
        }

        if (re_multi_type(peekchr()) != NOT_MULTI)
        {
            emsg(u8("E871: (NFA regexp) Can't have a multi follow a multi !"));
            rc_did_emsg = true;
            return false;
        }

        return true;
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
    /*private*/ static boolean nfa_regconcat()
    {
        boolean cont = true;
        boolean first = true;

        while (cont)
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
                    if (nfa_regpiece() == false)
                        return false;
                    if (first == false)
                        emc1(NFA_CONCAT);
                    else
                        first = false;
                    break;
            }
        }

        return true;
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
    /*private*/ static boolean nfa_regbranch()
    {
        int old_post_pos = post_index;

        /* First branch, possibly the only one. */
        if (nfa_regconcat() == false)
            return false;

        int ch = peekchr();
        /* Try next concats. */
        while (ch == Magic('&'))
        {
            skipchr();
            emc1(NFA_NOPEN);
            emc1(NFA_PREV_ATOM_NO_WIDTH);
            old_post_pos = post_index;
            if (nfa_regconcat() == false)
                return false;
            /* if concat is empty do emit a node */
            if (old_post_pos == post_index)
                emc1(NFA_EMPTY);
            emc1(NFA_CONCAT);
            ch = peekchr();
        }

        /* if a branch is empty, emit one node for it */
        if (old_post_pos == post_index)
            emc1(NFA_EMPTY);

        return true;
    }

    /*
     *  Parse a pattern, one or more branches, separated by "\|".
     *  It matches anything that matches one of the branches.
     *  Example: "foo\|beep" matches "foo" and matches "beep".
     *  If more than one branch matches, the first one is used.
     *
     *  pattern ::=     branch
     *      or  branch \| branch
     *      or  branch \| branch \| branch
     *      etc.
     */
    /*private*/ static boolean nfa_reg(int paren)
        /* paren: REG_NOPAREN, REG_PAREN, REG_NPAREN or REG_ZPAREN */
    {
        int parno = 0;

        if (paren == REG_PAREN)
        {
            if (NSUBEXP <= regnpar)
            {
                emsg(u8("E872: (NFA regexp) Too many '('"));
                rc_did_emsg = true;
                return false;
            }
            parno = regnpar++;
        }
        else if (paren == REG_ZPAREN)
        {
            /* Make a ZOPEN node. */
            if (NSUBEXP <= regnzpar)
            {
                emsg(u8("E879: (NFA regexp) Too many \\z("));
                rc_did_emsg = true;
                return false;
            }
            parno = regnzpar++;
        }

        if (nfa_regbranch() == false)
            return false;                   /* cascaded error */

        while (peekchr() == Magic('|'))
        {
            skipchr();
            if (nfa_regbranch() == false)
                return false;               /* cascaded error */
            emc1(NFA_OR);
        }

        /* Check for proper termination. */
        if (paren != REG_NOPAREN && getchr() != Magic(')'))
        {
            if (paren == REG_NPAREN)
                emsg2(e_unmatchedpp, (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
            else
                emsg2(e_unmatchedp, (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
            rc_did_emsg = true;
            return false;
        }
        else if (paren == REG_NOPAREN && peekchr() != NUL)
        {
            if (peekchr() == Magic(')'))
                emsg2(e_unmatchedpar, (reg_magic == MAGIC_ALL) ? u8("") : u8("\\"));
            else
                emsg(u8("E873: (NFA regexp) proper termination error"));
            rc_did_emsg = true;
            return false;
        }
        /*
         * Here we set the flag allowing back references to this set of parentheses.
         */
        if (paren == REG_PAREN)
        {
            had_endbrace[parno] = true;     /* have seen the close paren */
            emc1(NFA_MOPEN + parno);
        }
        else if (paren == REG_ZPAREN)
            emc1(NFA_ZOPEN + parno);

        return true;
    }

    /*
     * Parse r.e. @expr and convert it into postfix form.
     * Return the postfix string on success, null otherwise.
     */
    /*private*/ static int[] re2post()
    {
        if (nfa_reg(REG_NOPAREN) == false)
            return null;

        emc1(NFA_MOPEN);
        return post_array;
    }

    /*
     * Represents an NFA state plus zero or one or two arrows exiting.
     * if c == MATCH, no arrows out; matching state.
     * If c == SPLIT, unlabeled arrows to out0 and out1 (if != null).
     * If c < 256, labeled arrow with character c to out0.
     */

    /*private*/ static nfa_state_C[] nfa_states;    /* points to nfa_prog.states */

    /*
     * Allocate and initialize nfa_state_C.
     */
    /*private*/ static nfa_state_C alloc_state(nfa_regprog_C prog, int c, nfa_state_C out0, nfa_state_C out1)
    {
        if (prog.nstate <= prog.istate)
            return null;

        nfa_state_C state = nfa_states[prog.istate++] = new nfa_state_C();

        state.c = c;
        state.out0(out0);
        state.out1(out1);
        state.val = 0;

        state.id = prog.istate;
        state.lastlist[0] = 0;
        state.lastlist[1] = 0;

        return state;
    }

    /*
     * Estimate the maximum byte length of anything matching "state".
     * When unknown or unlimited return -1.
     */
    /*private*/ static int nfa_max_width(nfa_state_C startstate, int depth)
    {
        /* detect looping in a NFA_SPLIT */
        if (4 < depth)
            return -1;

        int len = 0;

        for (nfa_state_C state = startstate; state != null; )
        {
            switch (state.c)
            {
                case NFA_END_INVISIBLE:
                case NFA_END_INVISIBLE_NEG:
                    /* the end, return what we have */
                    return len;

                case NFA_SPLIT:
                {
                    /* two alternatives, use the maximum */
                    int l = nfa_max_width(state.out0(), depth + 1);
                    int r = nfa_max_width(state.out1(), depth + 1);
                    if (l < 0 || r < 0)
                        return -1;

                    return len + (r < l ? l : r);
                }

                case NFA_ANY:
                case NFA_START_COLL:
                case NFA_START_NEG_COLL:
                    /* matches some character, including composing chars */
                    len += MB_MAXBYTES;
                    if (state.c != NFA_ANY)
                    {
                        /* skip over the characters */
                        state = state.out1().out0();
                        continue;
                    }
                    break;

                case NFA_DIGIT:
                case NFA_WHITE:
                case NFA_HEX:
                case NFA_OCTAL:
                    /* ascii */
                    len++;
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
                    state = state.out1().out0();
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
                    if (state.c < 0)
                        /* don't know what this is */
                        return -1;
                    /* normal character */
                    len += utf_char2len(state.c);
                    break;
            }

            /* normal way to continue */
            state = state.out0();
        }

        /* unrecognized, "cannot happen" */
        return -1;
    }

    /*
     * A partially built NFA without the matching state filled in.
     * frag_C.fr_start points at the start state.
     * frag_C.fr_out is a list of places that need to be set to the next state for this fragment.
     */

    /* Since the out pointers in the list are always uninitialized,
     * we use the pointers themselves as storage for the fragnode_C. */
    /*private*/ static final class fragnode_C
    {
        /*fragnode_C*/Object      fn_next;

        /*private*/ fragnode_C()
        {
        }
    }

    /*private*/ static final class frag_C
    {
        nfa_state_C     fr_start;
        fragnode_C      fr_out;

        /*private*/ frag_C()
        {
        }
    }

    /*private*/ static void COPY_frag(frag_C fr1, frag_C fr0)
    {
        fr1.fr_start = fr0.fr_start;
        fr1.fr_out = fr0.fr_out;
    }

    /*
     * Initialize a frag_C struct and return it.
     */
    /*private*/ static frag_C alloc_frag(nfa_state_C start, fragnode_C out)
    {
        frag_C frag = new frag_C();

        frag.fr_start = start;
        frag.fr_out = out;

        return frag;
    }

    /*
     * Create singleton list containing just outp.
     */
    /*private*/ static fragnode_C fr_single(fragnode_C node)
    {
        node.fn_next = null;
        return node;
    }

    /*
     * Patch the list of states at out to point to start.
     */
    /*private*/ static void fr_patch(fragnode_C node, nfa_state_C start)
    {
        for (fragnode_C next; node != null; node = next)
        {
            next = (fragnode_C)node.fn_next;
            node.fn_next = start;
        }
    }

    /*
     * Join the two lists returning the concatenation.
     */
    /*private*/ static fragnode_C fr_append(fragnode_C head, fragnode_C tail)
    {
        fragnode_C list = head;

        while (head.fn_next != null)
            head = (fragnode_C)head.fn_next;
        head.fn_next = tail;

        return list;
    }

    /*
     * Stack used for transforming postfix form into NFA.
     */
    /*private*/ static final class nfa_stack_C
    {
        frag_C[]    st_base;
        int         st_next;
        int         st_over;

        /*private*/ nfa_stack_C(int n)
        {
            this.st_base = new frag_C[n];
            this.st_next = 0;
            this.st_over = n;
        }
    }

    /*
     * Push an item onto the stack.
     */
    /*private*/ static boolean st_push(nfa_stack_C stack, frag_C frag)
    {
        if (stack.st_next < stack.st_over)
        {
            stack.st_base[stack.st_next++] = frag;
            return true;
        }

        return false;
    }

    /*
     * Pop an item from the stack.
     */
    /*private*/ static frag_C st_pop(nfa_stack_C stack)
    {
        if (--stack.st_next < 0)
            return null;

        frag_C frag = stack.st_base[stack.st_next];
        stack.st_base[stack.st_next] = null;
        return frag;
    }

    /*private*/ static nfa_state_C st_error(int[] _postfix, int _i, int _over)
    {
        emsg(u8("E874: (NFA) Could not pop the stack !"));
        return null;
    }

    /*
     * Convert a postfix form into its equivalent NFA.
     * Return the NFA start state on success, null otherwise.
     */
    /*private*/ static nfa_state_C post2nfa(int[] postfix, int over, nfa_regprog_C prog, boolean nfa_calc_size)
    {
        if (postfix == null)
            return null;

        nfa_stack_C stack = (nfa_calc_size) ? null : new nfa_stack_C(prog.nstate + 1);

        int i;
        for (i = 0; i < over; i++)
        {
            switch (postfix[i])
            {
                case NFA_CONCAT:
                {
                    /* Concatenation.
                     * Pay attention: this operator does not exist in the r.e. itself (it is implicit, really).
                     * It is added when r.e. is translated to postfix form in re2post(). */
                    if (nfa_calc_size)
                    {
                        /* prog.nstate += 0; */
                        break;
                    }

                    frag_C e2 = st_pop(stack);
                    if (e2 == null)
                        return st_error(postfix, i, over);
                    frag_C e1 = st_pop(stack);
                    if (e1 == null)
                        return st_error(postfix, i, over);
                    fr_patch(e1.fr_out, e2.fr_start);
                    st_push(stack, alloc_frag(e1.fr_start, e2.fr_out));
                    break;
                }

                case NFA_OR:
                {
                    /* Alternation. */
                    if (nfa_calc_size)
                    {
                        prog.nstate++;
                        break;
                    }

                    frag_C e2 = st_pop(stack);
                    if (e2 == null)
                        return st_error(postfix, i, over);
                    frag_C e1 = st_pop(stack);
                    if (e1 == null)
                        return st_error(postfix, i, over);
                    nfa_state_C s0 = alloc_state(prog, NFA_SPLIT, e1.fr_start, e2.fr_start);
                    if (s0 == null)
                        return null;
                    st_push(stack, alloc_frag(s0, fr_append(e1.fr_out, e2.fr_out)));
                    break;
                }

                case NFA_STAR:
                {
                    /* Zero or more, prefer more. */
                    if (nfa_calc_size)
                    {
                        prog.nstate++;
                        break;
                    }

                    frag_C e0 = st_pop(stack);
                    if (e0 == null)
                        return st_error(postfix, i, over);
                    nfa_state_C s0 = alloc_state(prog, NFA_SPLIT, e0.fr_start, null);
                    if (s0 == null)
                        return null;
                    fr_patch(e0.fr_out, s0);
                    st_push(stack, alloc_frag(s0, fr_single(s0.out1)));
                    break;
                }

                case NFA_STAR_NONGREEDY:
                {
                    /* Zero or more, prefer zero. */
                    if (nfa_calc_size)
                    {
                        prog.nstate++;
                        break;
                    }

                    frag_C e0 = st_pop(stack);
                    if (e0 == null)
                        return st_error(postfix, i, over);
                    nfa_state_C s0 = alloc_state(prog, NFA_SPLIT, null, e0.fr_start);
                    if (s0 == null)
                        return null;
                    fr_patch(e0.fr_out, s0);
                    st_push(stack, alloc_frag(s0, fr_single(s0.out0)));
                    break;
                }

                case NFA_QUEST:
                {
                    /* one or zero atoms=> greedy match */
                    if (nfa_calc_size)
                    {
                        prog.nstate++;
                        break;
                    }

                    frag_C e0 = st_pop(stack);
                    if (e0 == null)
                        return st_error(postfix, i, over);
                    nfa_state_C s0 = alloc_state(prog, NFA_SPLIT, e0.fr_start, null);
                    if (s0 == null)
                        return null;
                    st_push(stack, alloc_frag(s0, fr_append(e0.fr_out, fr_single(s0.out1))));
                    break;
                }

                case NFA_QUEST_NONGREEDY:
                {
                    /* zero or one atoms => non-greedy match */
                    if (nfa_calc_size)
                    {
                        prog.nstate++;
                        break;
                    }

                    frag_C e0 = st_pop(stack);
                    if (e0 == null)
                        return st_error(postfix, i, over);
                    nfa_state_C s0 = alloc_state(prog, NFA_SPLIT, null, e0.fr_start);
                    if (s0 == null)
                        return null;
                    st_push(stack, alloc_frag(s0, fr_append(e0.fr_out, fr_single(s0.out0))));
                    break;
                }

                case NFA_END_COLL:
                case NFA_END_NEG_COLL:
                {
                    /* On the stack is the sequence starting with NFA_START_COLL or
                     * NFA_START_NEG_COLL and all possible characters.  Patch it to
                     * add the output to the start. */
                    if (nfa_calc_size)
                    {
                        prog.nstate++;
                        break;
                    }

                    frag_C e0 = st_pop(stack);
                    if (e0 == null)
                        return st_error(postfix, i, over);
                    nfa_state_C s0 = alloc_state(prog, NFA_END_COLL, null, null);
                    if (s0 == null)
                        return null;
                    fr_patch(e0.fr_out, s0);
                    e0.fr_start.out1(s0);
                    st_push(stack, alloc_frag(e0.fr_start, fr_single(s0.out0)));
                    break;
                }

                case NFA_RANGE:
                {
                    /* Before this are two characters, the low and high end of a range.
                     * Turn them into two states with MIN and MAX. */
                    if (nfa_calc_size)
                    {
                        /* prog.nstate += 0; */
                        break;
                    }

                    frag_C e2 = st_pop(stack);
                    if (e2 == null)
                        return st_error(postfix, i, over);
                    frag_C e1 = st_pop(stack);
                    if (e1 == null)
                        return st_error(postfix, i, over);
                    e2.fr_start.val = e2.fr_start.c;
                    e2.fr_start.c = NFA_RANGE_MAX;
                    e1.fr_start.val = e1.fr_start.c;
                    e1.fr_start.c = NFA_RANGE_MIN;
                    fr_patch(e1.fr_out, e2.fr_start);
                    st_push(stack, alloc_frag(e1.fr_start, e2.fr_out));
                    break;
                }

                case NFA_EMPTY:
                {
                    /* 0-length, used in a repetition with max/min count of 0 */
                    if (nfa_calc_size)
                    {
                        prog.nstate++;
                        break;
                    }

                    nfa_state_C s0 = alloc_state(prog, NFA_EMPTY, null, null);
                    if (s0 == null)
                        return null;
                    st_push(stack, alloc_frag(s0, fr_single(s0.out0)));
                    break;
                }

                case NFA_OPT_CHARS:
                {
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
                    int n = postfix[++i];                           /* get number of characters */
                    if (nfa_calc_size)
                    {
                        prog.nstate += n;
                        break;
                    }

                    frag_C e1 = alloc_frag(null, null);     /* e1.fr_out: stores list with out1's */
                    nfa_state_C s0 = null;
                    for (nfa_state_C s1 = null; 0 < n--; s1 = s0)  /* s1: previous NFA_SPLIT to connect to */
                    {
                        frag_C e0 = st_pop(stack);          /* get character */
                        if (e0 == null)
                            return st_error(postfix, i, over);
                        s0 = alloc_state(prog, NFA_SPLIT, e0.fr_start, null);
                        if (s0 == null)
                            return null;
                        if (e1.fr_out == null)
                            COPY_frag(e1, e0);
                        fr_patch(e0.fr_out, s1);
                        fr_append(e1.fr_out, fr_single(s0.out1));
                    }
                    st_push(stack, alloc_frag(s0, e1.fr_out));
                    break;
                }

                case NFA_PREV_ATOM_NO_WIDTH:
                case NFA_PREV_ATOM_NO_WIDTH_NEG:
                case NFA_PREV_ATOM_JUST_BEFORE:
                case NFA_PREV_ATOM_JUST_BEFORE_NEG:
                case NFA_PREV_ATOM_LIKE_PATTERN:
                {
                    boolean before = (postfix[i] == NFA_PREV_ATOM_JUST_BEFORE
                                   || postfix[i] == NFA_PREV_ATOM_JUST_BEFORE_NEG);
                    boolean pattern = (postfix[i] == NFA_PREV_ATOM_LIKE_PATTERN);

                    int start_state, end_state;
                    switch (postfix[i])
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

                    int n = (before) ? postfix[++i] : 0;    /* get the count */

                    /*
                     * The \@= operator: match the preceding atom with zero width.
                     * The \@! operator: no match for the preceding atom.
                     * The \@<= operator: match for the preceding atom.
                     * The \@<! operator: no match for the preceding atom.
                     * Surrounds the preceding atom with START_INVISIBLE and END_INVISIBLE, similarly to MOPEN.
                     */

                    if (nfa_calc_size)
                    {
                        prog.nstate += pattern ? 4 : 2;
                        break;
                    }

                    frag_C e0 = st_pop(stack);
                    if (e0 == null)
                        return st_error(postfix, i, over);

                    nfa_state_C s1 = alloc_state(prog, end_state, null, null);
                    if (s1 == null)
                        return null;

                    nfa_state_C s0 = alloc_state(prog, start_state, e0.fr_start, s1);
                    if (s0 == null)
                        return null;

                    if (pattern)
                    {
                        /* NFA_ZEND -> NFA_END_PATTERN -> NFA_SKIP -> what follows. */
                        nfa_state_C skip = alloc_state(prog, NFA_SKIP, null, null);
                        nfa_state_C zend = alloc_state(prog, NFA_ZEND, s1, null);
                        s1.out0(skip);
                        fr_patch(e0.fr_out, zend);
                        st_push(stack, alloc_frag(s0, fr_single(skip.out0)));
                    }
                    else
                    {
                        fr_patch(e0.fr_out, s1);
                        st_push(stack, alloc_frag(s0, fr_single(s1.out0)));
                        if (before)
                        {
                            /* See if we can guess the maximum width, it avoids a lot of pointless tries. */
                            if (n <= 0)
                                n = nfa_max_width(e0.fr_start, 0);
                            s0.val = n; /* store the count */
                        }
                    }
                    break;
                }

                case NFA_COMPOSING:     /* char with composing char */
                    if ((regflags & RF_ICOMBINE) != 0)
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
                {
                    if (nfa_calc_size)
                    {
                        prog.nstate += 2;
                        break;
                    }

                    int mopen = postfix[i], mclose;
                    switch (postfix[i])
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
                            mclose = postfix[i] + NSUBEXP;
                            break;
                    }

                    /* Allow "NFA_MOPEN" as a valid postfix representation for the empty regexp "".
                     * In this case, the NFA will be NFA_MOPEN -> NFA_MCLOSE.  Note that this also
                     * allows empty groups of parenthesis, and empty mbyte chars. */
                    if (stack.st_next == 0)
                    {
                        nfa_state_C s0 = alloc_state(prog, mopen, null, null);
                        if (s0 == null)
                            return null;
                        nfa_state_C s1 = alloc_state(prog, mclose, null, null);
                        if (s1 == null)
                            return null;
                        fr_patch(fr_single(s0.out0), s1);
                        st_push(stack, alloc_frag(s0, fr_single(s1.out0)));
                        break;
                    }

                    /* At least one node was emitted before NFA_MOPEN, so
                     * at least one node will be between NFA_MOPEN and NFA_MCLOSE. */
                    frag_C e0 = st_pop(stack);
                    if (e0 == null)
                        return st_error(postfix, i, over);
                    nfa_state_C s0 = alloc_state(prog, mopen, e0.fr_start, null);   /* `(' */
                    if (s0 == null)
                        return null;

                    nfa_state_C s1 = alloc_state(prog, mclose, null, null);         /* `)' */
                    if (s1 == null)
                        return null;
                    fr_patch(e0.fr_out, s1);

                    if (mopen == NFA_COMPOSING)
                        /* COMPOSING.out1 = END_COMPOSING */
                        fr_patch(fr_single(s0.out1), s1);

                    st_push(stack, alloc_frag(s0, fr_single(s1.out0)));
                    break;
                }

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
                {
                    if (nfa_calc_size)
                    {
                        prog.nstate += 2;
                        break;
                    }

                    nfa_state_C s0 = alloc_state(prog, postfix[i], null, null);
                    if (s0 == null)
                        return null;
                    nfa_state_C s1 = alloc_state(prog, NFA_SKIP, null, null);
                    if (s1 == null)
                        return null;
                    fr_patch(fr_single(s0.out0), s1);
                    st_push(stack, alloc_frag(s0, fr_single(s1.out0)));
                    break;
                }

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
                    int n = postfix[++i]; /* lnum, col or mark name */

                    if (nfa_calc_size)
                    {
                        prog.nstate += 1;
                        break;
                    }

                    nfa_state_C s0 = alloc_state(prog, postfix[i - 1], null, null);
                    if (s0 == null)
                        return null;
                    s0.val = n;
                    st_push(stack, alloc_frag(s0, fr_single(s0.out0)));
                    break;
                }

                case NFA_ZSTART:
                case NFA_ZEND:
                default:
                {
                    /* Operands. */
                    if (nfa_calc_size)
                    {
                        prog.nstate++;
                        break;
                    }

                    nfa_state_C s0 = alloc_state(prog, postfix[i], null, null);
                    if (s0 == null)
                        return null;
                    st_push(stack, alloc_frag(s0, fr_single(s0.out0)));
                    break;
                }
            }
        }

        if (nfa_calc_size)
        {
            prog.nstate++;
            return null;  /* Return value when counting size is ignored anyway. */
        }

        frag_C e0 = st_pop(stack);
        if (e0 == null)
            return st_error(postfix, i, over);
        if (0 < stack.st_next)
        {
            emsg(u8("E875: (NFA regexp) (While converting from postfix to NFA), too many states left on stack"));
            rc_did_emsg = true;
            return null;
        }

        if (prog.nstate <= prog.istate)
        {
            emsg(u8("E876: (NFA regexp) Not enough space to store the whole NFA"));
            rc_did_emsg = true;
            return null;
        }

        nfa_state_C state = nfa_states[prog.istate++] = new nfa_state_C();
        state.c = NFA_MATCH;
        state.out0(null);
        state.out1(null);
        state.id = 0;

        fr_patch(e0.fr_out, state);
        return e0.fr_start;
    }

    /*
     * After building the NFA program, inspect it to add optimization hints.
     */
    /*private*/ static void nfa_postprocess(nfa_regprog_C prog)
    {
        for (int i = 0; i < prog.nstate; i++)
        {
            nfa_state_C state = prog.states[i];
            if (state == null)
                continue;

            int c = state.c;
            if (c == NFA_START_INVISIBLE
             || c == NFA_START_INVISIBLE_NEG
             || c == NFA_START_INVISIBLE_BEFORE
             || c == NFA_START_INVISIBLE_BEFORE_NEG)
            {
                boolean directly;

                /* Do it directly when what follows is possibly the end of the match. */
                if (match_follows(state.out1().out0(), 0))
                    directly = true;
                else
                {
                    int ch_invisible = failure_chance(state.out0(), 0);
                    int ch_follows = failure_chance(state.out1().out0(), 0);

                    /* Postpone when the invisible match is expensive or has a lower chance of failing. */
                    if (c == NFA_START_INVISIBLE_BEFORE || c == NFA_START_INVISIBLE_BEFORE_NEG)
                    {
                        /* "before" matches are very expensive when unbounded,
                         * always prefer what follows then, unless what follows will always match.
                         * Otherwise strongly prefer what follows. */
                        if (state.val <= 0 && 0 < ch_follows)
                            directly = false;
                        else
                            directly = (ch_follows * 10 < ch_invisible);
                    }
                    else
                    {
                        /* normal invisible, first do the one with the highest failure chance */
                        directly = (ch_follows < ch_invisible);
                    }
                }
                if (directly)
                    /* switch to the _FIRST state */
                    state.c++;
            }
        }
    }

    /****************************************************************
     * NFA execution code.
     ****************************************************************/

    /*private*/ static final class multipos_C
    {
        long        start_lnum;
        long        end_lnum;
        int         start_col;
        int         end_col;

        /*private*/ multipos_C()
        {
        }
    }

    /*private*/ static void MIN1_multipos(multipos_C mp)
    {
        mp.start_lnum = -1;
        mp.end_lnum = -1;
        mp.start_col = -1;
        mp.end_col = -1;
    }

    /*private*/ static void COPY_multipos(multipos_C mp1, multipos_C mp0)
    {
        mp1.start_lnum = mp0.start_lnum;
        mp1.end_lnum = mp0.end_lnum;
        mp1.start_col = mp0.start_col;
        mp1.end_col = mp0.end_col;
    }

    /*private*/ static multipos_C[] ARRAY_multipos(int n)
    {
        multipos_C[] a = new multipos_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new multipos_C();
        return a;
    }

    /*private*/ static void COPY__multipos(multipos_C[] a1, multipos_C[] a0, int n)
    {
        for (int i = 0; i < n; i++)
            COPY_multipos(a1[i], a0[i]);
    }

    /*private*/ static final class linepos_C
    {
        Bytes       start;
        Bytes       end;

        /*private*/ linepos_C()
        {
        }
    }

    /*private*/ static void ZER0_linepos(linepos_C lp)
    {
        lp.start = null;
        lp.end = null;
    }

    /*private*/ static void COPY_linepos(linepos_C lp1, linepos_C lp0)
    {
        lp1.start = lp0.start;
        lp1.end = lp0.end;
    }

    /*private*/ static linepos_C[] ARRAY_linepos(int n)
    {
        linepos_C[] a = new linepos_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new linepos_C();
        return a;
    }

    /*private*/ static void COPY__linepos(linepos_C[] a1, linepos_C[] a0, int n)
    {
        for (int i = 0; i < n; i++)
            COPY_linepos(a1[i], a0[i]);
    }

    /*private*/ static final class regsub_C
    {
        int         in_use;         /* number of subexpr with useful info */

        multipos_C[] rs_multi;      /* union: when reg_match == null */
        linepos_C[] rs_line;        /* union: when reg_match != null */

        /*private*/ regsub_C()
        {
            rs_multi = ARRAY_multipos(NSUBEXP);
            rs_line = ARRAY_linepos(NSUBEXP);
        }
    }

    /*private*/ static void COPY_regsub(regsub_C rs1, regsub_C rs0)
    {
        rs1.in_use = rs0.in_use;

        COPY__multipos(rs1.rs_multi, rs0.rs_multi, NSUBEXP);
        COPY__linepos(rs1.rs_line, rs0.rs_line, NSUBEXP);
    }

    /*private*/ static final class regsubs_C
    {
        regsub_C    rs_norm;        /* \( .. \) matches */
        regsub_C    rs_synt;        /* \z( .. \) matches */

        /*private*/ regsubs_C()
        {
            rs_norm = new regsub_C();
            rs_synt = new regsub_C();
        }
    }

    /*private*/ static void COPY_regsubs(regsubs_C rs1, regsubs_C rs0)
    {
        COPY_regsub(rs1.rs_norm, rs0.rs_norm);
        COPY_regsub(rs1.rs_synt, rs0.rs_synt);
    }

    /* nfa_pim_C stores a Postponed Invisible Match. */
    /*private*/ static final class nfa_pim_C
    {
        int         result;         /* NFA_PIM_*, see below */
        nfa_state_C state;          /* the invisible match start state */
        regsubs_C   np_subs;        /* submatch info, only party used */

        lpos_C      end_pos;        /* union upon reg_match: where the match must end */
        Bytes       end_ptr;        /* union upon reg_match: where the match must end */

        /*private*/ nfa_pim_C()
        {
            np_subs = new regsubs_C();
            end_pos = new lpos_C();
        }
    }

    /*private*/ static void COPY_nfa_pim(nfa_pim_C np1, nfa_pim_C np0)
    {
        np1.result = np0.result;
        np1.state = np0.state;
        COPY_regsubs(np1.np_subs, np0.np_subs);

        COPY_lpos(np1.end_pos, np0.end_pos);
        np1.end_ptr = np0.end_ptr;
    }

    /* Values for done in nfa_pim_C. */
    /*private*/ static final int NFA_PIM_UNUSED   = 0;      /* pim not used */
    /*private*/ static final int NFA_PIM_TODO     = 1;      /* pim not done yet */
    /*private*/ static final int NFA_PIM_MATCH    = 2;      /* pim executed, matches */
    /*private*/ static final int NFA_PIM_NOMATCH  = 3;      /* pim executed, no match */

    /* nfa_thread_C contains execution information of a NFA state */
    /*private*/ static final class nfa_thread_C
    {
        nfa_state_C state;
        int         count;
        nfa_pim_C   th_pim;         /* if pim.result != NFA_PIM_UNUSED: postponed invisible match */
        regsubs_C   th_subs;        /* submatch info, only party used */

        /*private*/ nfa_thread_C()
        {
            th_pim = new nfa_pim_C();
            th_subs = new regsubs_C();
        }
    }

    /*private*/ static void COPY_nfa_thread(nfa_thread_C th1, nfa_thread_C th0)
    {
        th1.state = th0.state;
        th1.count = th0.count;
        COPY_nfa_pim(th1.th_pim, th0.th_pim);
        COPY_regsubs(th1.th_subs, th0.th_subs);
    }

    /*private*/ static nfa_thread_C[] ARRAY_nfa_thread(int n)
    {
        nfa_thread_C[] a = new nfa_thread_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new nfa_thread_C();
        return a;
    }

    /* nfa_list_C contains the alternative NFA execution states. */
    /*private*/ static final class nfa_list_C
    {
        nfa_thread_C[]  threads;    /* allocated array of states */
        int             n;          /* nr of states currently in "t" */
        int             len;        /* max nr of states in "t" */
        int             id;         /* ID of the list */
        boolean         has_pim;    /* true when any state has a PIM */

        /*private*/ nfa_list_C()
        {
        }
    }

    /*private*/ static nfa_list_C[] ARRAY_nfa_list(int n)
    {
        nfa_list_C[] a = new nfa_list_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new nfa_list_C();
        return a;
    }

    /* Used during execution: whether a match has been found. */
    /*private*/ static int nfa_match;
    /*private*/ static timeval_C nfa_time_limit;
    /*private*/ static int nfa_time_count;

    /*
     * Copy postponed invisible match info from "from" to "to".
     */
    /*private*/ static void copy_pim(nfa_pim_C to, nfa_pim_C from)
    {
        to.result = from.result;
        to.state = from.state;
        copy_sub(to.np_subs.rs_norm, from.np_subs.rs_norm);
        if (nfa_has_zsubexpr)
            copy_sub(to.np_subs.rs_synt, from.np_subs.rs_synt);
        COPY_lpos(to.end_pos, from.end_pos);
        to.end_ptr = from.end_ptr;
    }

    /*private*/ static void clear_sub(regsub_C sub)
    {
        if (reg_match == null)
        {
            /* Use 0xff to set lnum to -1. */
            for (int i = 0; i < nfa_nsubexpr; i++)
                MIN1_multipos(sub.rs_multi[i]);
        }
        else
        {
            for (int i = 0; i < nfa_nsubexpr; i++)
                ZER0_linepos(sub.rs_line[i]);
        }
        sub.in_use = 0;
    }

    /*
     * Copy the submatches from "from" to "to".
     */
    /*private*/ static void copy_sub(regsub_C to, regsub_C from)
    {
        to.in_use = from.in_use;
        if (0 < from.in_use)
        {
            /* Copy the match start and end positions. */
            if (reg_match == null)
            {
                for (int i = 0; i < from.in_use; i++)
                    COPY_multipos(to.rs_multi[i], from.rs_multi[i]);
            }
            else
            {
                for (int i = 0; i < from.in_use; i++)
                    COPY_linepos(to.rs_line[i], from.rs_line[i]);
            }
        }
    }

    /*
     * Like copy_sub() but exclude the main match.
     */
    /*private*/ static void copy_sub_off(regsub_C to, regsub_C from)
    {
        if (to.in_use < from.in_use)
            to.in_use = from.in_use;
        if (1 < from.in_use)
        {
            /* Copy the match start and end positions. */
            if (reg_match == null)
            {
                for (int i = 1; i < from.in_use; i++)
                    COPY_multipos(to.rs_multi[i], from.rs_multi[i]);
            }
            else
            {
                for (int i = 1; i < from.in_use; i++)
                    COPY_linepos(to.rs_line[i], from.rs_line[i]);
            }
        }
    }

    /*
     * Like copy_sub() but only do the end of the main match if \ze is present.
     */
    /*private*/ static void copy_ze_off(regsub_C to, regsub_C from)
    {
        if (nfa_has_zend)
        {
            if (reg_match == null)
            {
                if (0 <= from.rs_multi[0].end_lnum)
                {
                    to.rs_multi[0].end_lnum = from.rs_multi[0].end_lnum;
                    to.rs_multi[0].end_col = from.rs_multi[0].end_col;
                }
            }
            else
            {
                if (from.rs_line[0].end != null)
                    to.rs_line[0].end = from.rs_line[0].end;
            }
        }
    }

    /*
     * Return true if "sub1" and "sub2" have the same start positions.
     * When using back-references also check the end position.
     */
    /*private*/ static boolean sub_equal(regsub_C sub1, regsub_C sub2)
    {
        int todo = (sub2.in_use < sub1.in_use) ? sub1.in_use : sub2.in_use;

        if (reg_match == null)
        {
            for (int i = 0; i < todo; i++)
            {
                long s1, s2;

                if (i < sub1.in_use)
                    s1 = sub1.rs_multi[i].start_lnum;
                else
                    s1 = -1;
                if (i < sub2.in_use)
                    s2 = sub2.rs_multi[i].start_lnum;
                else
                    s2 = -1;
                if (s1 != s2)
                    return false;
                if (s1 != -1 && sub1.rs_multi[i].start_col != sub2.rs_multi[i].start_col)
                    return false;

                if (nfa_has_backref)
                {
                    if (i < sub1.in_use)
                        s1 = sub1.rs_multi[i].end_lnum;
                    else
                        s1 = -1;
                    if (i < sub2.in_use)
                        s2 = sub2.rs_multi[i].end_lnum;
                    else
                        s2 = -1;
                    if (s1 != s2)
                        return false;
                    if (s1 != -1 && sub1.rs_multi[i].end_col != sub2.rs_multi[i].end_col)
                        return false;
                }
            }
        }
        else
        {
            for (int i = 0; i < todo; i++)
            {
                Bytes sp1, sp2;

                if (i < sub1.in_use)
                    sp1 = sub1.rs_line[i].start;
                else
                    sp1 = null;
                if (i < sub2.in_use)
                    sp2 = sub2.rs_line[i].start;
                else
                    sp2 = null;
                if (BNE(sp1, sp2))
                    return false;
                if (nfa_has_backref)
                {
                    if (i < sub1.in_use)
                        sp1 = sub1.rs_line[i].end;
                    else
                        sp1 = null;
                    if (i < sub2.in_use)
                        sp2 = sub2.rs_line[i].end;
                    else
                        sp2 = null;
                    if (BNE(sp1, sp2))
                        return false;
                }
            }
        }

        return true;
    }

    /*
     * Return true if the same state is already in list "nfl" with the same positions as "subs".
     */
    /*private*/ static boolean has_state_with_pos(nfa_list_C nfl, nfa_state_C state, regsubs_C subs, nfa_pim_C pim)
        /* nfl: runtime state list */
        /* state: state to update */
        /* subs: pointers to subexpressions */
        /* pim: postponed match or null */
    {
        for (int i = 0; i < nfl.n; i++)
        {
            nfa_thread_C thread = nfl.threads[i];

            if (thread.state.id == state.id
                    && sub_equal(thread.th_subs.rs_norm, subs.rs_norm)
                    && (!nfa_has_zsubexpr || sub_equal(thread.th_subs.rs_synt, subs.rs_synt))
                    && pim_equal(thread.th_pim, pim))
                return true;
        }

        return false;
    }

    /*
     * Return true if "one" and "two" are equal.  That includes when both are not set.
     */
    /*private*/ static boolean pim_equal(nfa_pim_C one, nfa_pim_C two)
    {
        boolean one_unused = (one == null || one.result == NFA_PIM_UNUSED);
        boolean two_unused = (two == null || two.result == NFA_PIM_UNUSED);

        if (one_unused)
            /* one is unused: equal when two is also unused */
            return two_unused;
        if (two_unused)
            /* one is used and two is not: not equal */
            return false;
        /* compare the state id */
        if (one.state.id != two.state.id)
            return false;
        /* compare the position */
        if (reg_match == null)
            return (one.end_pos.lnum == two.end_pos.lnum && one.end_pos.col == two.end_pos.col);

        return BEQ(one.end_ptr, two.end_ptr);
    }

    /*
     * Return true if "state" leads to a NFA_MATCH without advancing the input.
     */
    /*private*/ static boolean match_follows(nfa_state_C startstate, int depth)
    {
        /* avoid too much recursion */
        if (10 < depth)
            return false;

        for (nfa_state_C state = startstate; state != null; )
        {
            switch (state.c)
            {
                case NFA_MATCH:
                case NFA_MCLOSE:
                case NFA_END_INVISIBLE:
                case NFA_END_INVISIBLE_NEG:
                case NFA_END_PATTERN:
                    return true;

                case NFA_SPLIT:
                    return match_follows(state.out0(), depth + 1) || match_follows(state.out1(), depth + 1);

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
                    state = state.out1().out0();
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
                    return false;

                default:
                    if (0 < state.c)
                        /* state will advance input */
                        return false;

                    /* Others: zero-width or possibly zero-width,
                     * might still find a match at the same position, keep looking. */
                    break;
            }
            state = state.out0();
        }

        return false;
    }

    /*
     * Return true if "state" is already in list "nfl".
     */
    /*private*/ static boolean state_in_list(nfa_list_C nfl, nfa_state_C state, regsubs_C subs)
        /* nfl: runtime state list */
        /* state: state to update */
        /* subs: pointers to subexpressions */
    {
        if (state.lastlist[nfa_ll_index] == nfl.id)
            if (!nfa_has_backref || has_state_with_pos(nfl, state, subs, null))
                return true;

        return false;
    }

    /*private*/ static regsubs_C temp_subs = new regsubs_C();

    /*
     * Add "state" and possibly what follows to state list ".".
     * Returns "subs_arg", possibly copied into temp_subs.
     */
    /*private*/ static regsubs_C addstate(nfa_list_C nfl, nfa_state_C state, regsubs_C subs_arg, nfa_pim_C pim, int off)
        /* nfl: runtime state list */
        /* state: state to update */
        /* subs_arg: pointers to subexpressions */
        /* pim: postponed look-behind match */
        /* off: byte offset, when -1 go to next line */
    {
        regsubs_C subs = subs_arg;

        switch (state.c)
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
                /* These nodes are not added themselves
                 * but their "out0" and/or "out1" may be added below. */
                break;

            case NFA_BOL:
            case NFA_BOF:
                /* "^" won't match past end-of-line, don't bother trying.
                 * Except when at the end of the line, or when we are going
                 * to the next line for a look-behind match. */
                if (BLT(regline, reginput)
                        && reginput.at(0) != NUL
                        && (nfa_endp == null || !(reg_match == null) || reglnum == nfa_endp.se_pos.lnum))
                    return subs;
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
                /* These nodes need to be added so that we can bail out
                 * when it was added to this list before at the same
                 * position to avoid an endless loop for "\(\)*" */

            default:
            {
                if (state.lastlist[nfa_ll_index] == nfl.id && state.c != NFA_SKIP)
                {
                    /* This state is already in the list, don't add it again,
                     * unless it is an MOPEN that is used for a backreference or
                     * when there is a PIM.  For NFA_MATCH check the position,
                     * lower position is preferred. */
                    if (!nfa_has_backref && pim == null && !nfl.has_pim && state.c != NFA_MATCH)
                        return subs;

                    /* Do not add the state again when it exists with the same positions. */
                    if (has_state_with_pos(nfl, state, subs, pim))
                        return subs;
                }

                /* When there are backreferences or PIMs,
                 * the number of states may be (a lot) bigger than anticipated. */
                if (nfl.n == nfl.len)
                {
                    if (subs != temp_subs)
                    {
                        /* "subs" may point into the current array,
                         * need to make a copy before it becomes invalid. */
                        copy_sub(temp_subs.rs_norm, subs.rs_norm);
                        if (nfa_has_zsubexpr)
                            copy_sub(temp_subs.rs_synt, subs.rs_synt);
                        subs = temp_subs;
                    }

                    int newlen = nfl.len * 3 / 2 + 50;
                    nfa_thread_C[] a = ARRAY_nfa_thread(newlen);
                    for (int i = 0; i < nfl.n; i++)
                        COPY_nfa_thread(a[i], nfl.threads[i]);
                    nfl.threads = a;
                    nfl.len = newlen;
                }

                /* add the state to the list */
                state.lastlist[nfa_ll_index] = nfl.id;
                nfa_thread_C thread = nfl.threads[nfl.n++];
                thread.state = state;
                if (pim == null)
                    thread.th_pim.result = NFA_PIM_UNUSED;
                else
                {
                    copy_pim(thread.th_pim, pim);
                    nfl.has_pim = true;
                }
                copy_sub(thread.th_subs.rs_norm, subs.rs_norm);
                if (nfa_has_zsubexpr)
                    copy_sub(thread.th_subs.rs_synt, subs.rs_synt);

                break;
            }
        }

        switch (state.c)
        {
            case NFA_MATCH:
                break;

            case NFA_SPLIT:
                /* order matters here */
                subs = addstate(nfl, state.out0(), subs, pim, off);
                subs = addstate(nfl, state.out1(), subs, pim, off);
                break;

            case NFA_EMPTY:
            case NFA_NOPEN:
            case NFA_NCLOSE:
                subs = addstate(nfl, state.out0(), subs, pim, off);
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
            {
                int subidx;
                regsub_C sub;
                if (state.c == NFA_ZSTART)
                {
                    subidx = 0;
                    sub = subs.rs_norm;
                }
                else if (NFA_ZOPEN <= state.c && state.c <= NFA_ZOPEN9)
                {
                    subidx = state.c - NFA_ZOPEN;
                    sub = subs.rs_synt;
                }
                else
                {
                    subidx = state.c - NFA_MOPEN;
                    sub = subs.rs_norm;
                }

                lpos_C save_lpos = new lpos_C();
                save_lpos.lnum = 0;
                save_lpos.col = 0;
                Bytes save_ptr = null;

                int save_in_use;
                /* Set the position (with "off" added) in the subexpression.
                 * Save and restore it when it was in use.
                 * Otherwise fill any gap. */
                if (reg_match == null)
                {
                    if (subidx < sub.in_use)
                    {
                        save_lpos.lnum = sub.rs_multi[subidx].start_lnum;
                        save_lpos.col = sub.rs_multi[subidx].start_col;
                        save_in_use = -1;
                    }
                    else
                    {
                        save_in_use = sub.in_use;
                        for (int i = sub.in_use; i < subidx; i++)
                        {
                            sub.rs_multi[i].start_lnum = -1;
                            sub.rs_multi[i].end_lnum = -1;
                        }
                        sub.in_use = subidx + 1;
                    }
                    if (off == -1)
                    {
                        sub.rs_multi[subidx].start_lnum = reglnum + 1;
                        sub.rs_multi[subidx].start_col = 0;
                    }
                    else
                    {
                        sub.rs_multi[subidx].start_lnum = reglnum;
                        sub.rs_multi[subidx].start_col = BDIFF(reginput, regline) + off;
                    }
                }
                else
                {
                    if (subidx < sub.in_use)
                    {
                        save_ptr = sub.rs_line[subidx].start;
                        save_in_use = -1;
                    }
                    else
                    {
                        save_in_use = sub.in_use;
                        for (int i = sub.in_use; i < subidx; i++)
                        {
                            sub.rs_line[i].start = null;
                            sub.rs_line[i].end = null;
                        }
                        sub.in_use = subidx + 1;
                    }
                    sub.rs_line[subidx].start = reginput.plus(off);
                }

                subs = addstate(nfl, state.out0(), subs, pim, off);
                /* "subs" may have changed, need to set "sub" again */
                if (NFA_ZOPEN <= state.c && state.c <= NFA_ZOPEN9)
                    sub = subs.rs_synt;
                else
                    sub = subs.rs_norm;

                if (save_in_use == -1)
                {
                    if (reg_match == null)
                    {
                        sub.rs_multi[subidx].start_lnum = save_lpos.lnum;
                        sub.rs_multi[subidx].start_col = save_lpos.col;
                    }
                    else
                        sub.rs_line[subidx].start = save_ptr;
                }
                else
                    sub.in_use = save_in_use;

                break;
            }

            case NFA_MCLOSE:
                if (nfa_has_zend && ((reg_match == null)
                            ? 0 <= subs.rs_norm.rs_multi[0].end_lnum
                            : subs.rs_norm.rs_line[0].end != null))
                {
                    /* Do not overwrite the position set by \ze. */
                    subs = addstate(nfl, state.out0(), subs, pim, off);
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
            {
                int subidx;
                regsub_C sub;
                if (state.c == NFA_ZEND)
                {
                    subidx = 0;
                    sub = subs.rs_norm;
                }
                else if (NFA_ZCLOSE <= state.c && state.c <= NFA_ZCLOSE9)
                {
                    subidx = state.c - NFA_ZCLOSE;
                    sub = subs.rs_synt;
                }
                else
                {
                    subidx = state.c - NFA_MCLOSE;
                    sub = subs.rs_norm;
                }

                lpos_C save_lpos = new lpos_C();
                Bytes save_ptr;

                /* We don't fill in gaps here, there must have been an MOPEN that has done that. */
                int save_in_use = sub.in_use;
                if (sub.in_use <= subidx)
                    sub.in_use = subidx + 1;
                if (reg_match == null)
                {
                    save_lpos.lnum = sub.rs_multi[subidx].end_lnum;
                    save_lpos.col = sub.rs_multi[subidx].end_col;
                    if (off == -1)
                    {
                        sub.rs_multi[subidx].end_lnum = reglnum + 1;
                        sub.rs_multi[subidx].end_col = 0;
                    }
                    else
                    {
                        sub.rs_multi[subidx].end_lnum = reglnum;
                        sub.rs_multi[subidx].end_col = BDIFF(reginput, regline) + off;
                    }
                    save_ptr = null;
                }
                else
                {
                    save_ptr = sub.rs_line[subidx].end;
                    sub.rs_line[subidx].end = reginput.plus(off);

                    save_lpos.lnum = 0;
                    save_lpos.col = 0;
                }

                subs = addstate(nfl, state.out0(), subs, pim, off);
                /* "subs" may have changed, need to set "sub" again */
                if (NFA_ZCLOSE <= state.c && state.c <= NFA_ZCLOSE9)
                    sub = subs.rs_synt;
                else
                    sub = subs.rs_norm;

                if (reg_match == null)
                {
                    sub.rs_multi[subidx].end_lnum = save_lpos.lnum;
                    sub.rs_multi[subidx].end_col = save_lpos.col;
                }
                else
                    sub.rs_line[subidx].end = save_ptr;
                sub.in_use = save_in_use;

                break;
            }
        }

        return subs;
    }

    /*
     * Like addstate(), but the new state(s) are put at position "*ip".
     * Used for zero-width matches, next state to use is the added one.
     * This makes sure the order of states to be tried does not change,
     * which matters for alternatives.
     */
    /*private*/ static void addstate_here(nfa_list_C nfl, nfa_state_C state, regsubs_C subs, nfa_pim_C pim, int[] ip)
        /* nfl: runtime state list */
        /* state: state to update */
        /* subs: pointers to subexpressions */
        /* pim: postponed look-behind match */
    {
        int tlen = nfl.n;
        int lidx = ip[0];

        /* first add the state(s) at the end, so that we know how many there are */
        addstate(nfl, state, subs, pim, 0);

        /* when "*ip" was at the end of the list, nothing to do */
        if (lidx + 1 == tlen)
            return;

        /* re-order to put the new state at the current position */
        int count = nfl.n - tlen;
        if (count == 0)
            return; /* no state got added */

        if (count == 1)
        {
            /* overwrite the current state */
            COPY_nfa_thread(nfl.threads[lidx], nfl.threads[nfl.n - 1]);
        }
        else if (1 < count)
        {
            if (nfl.len <= nfl.n + count - 1)
            {
                /* not enough space to move the new states,
                 * reallocate the list and move the states to the right position */
                int newlen = nfl.len * 3 / 2 + 50;
                nfa_thread_C[] a = ARRAY_nfa_thread(newlen);
                for (int i = 0; i < lidx; i++)
                    COPY_nfa_thread(a[i], nfl.threads[i]);
                for (int i = 0; i < count; i++)
                    COPY_nfa_thread(a[lidx + i], nfl.threads[nfl.n - count + i]);
                for (int i = 0; i < nfl.n - count - lidx - 1; i++)
                    COPY_nfa_thread(a[lidx + count + i], nfl.threads[lidx + 1 + i]);
                nfl.threads = a;
                nfl.len = newlen;
            }
            else
            {
                /* make space for new states, then move them from the end to the current position */
                for (int i = nfl.n; lidx + 1 <= --i; )
                    COPY_nfa_thread(nfl.threads[i + count - 1], nfl.threads[i]);
                for (int i = 0; i < count; i++)
                    COPY_nfa_thread(nfl.threads[lidx + i], nfl.threads[nfl.n - 1 + i]);
            }
        }

        --nfl.n;
        ip[0] = lidx - 1;
    }

    /*
     * Check character class "class" against current character c.
     */
    /*private*/ static boolean check_char_class(int klass, int c)
    {
        switch (klass)
        {
            case NFA_CLASS_ALNUM:
                if (1 <= c && c <= 255 && asc_isalnum(c))
                    return true;
                break;

            case NFA_CLASS_ALPHA:
                if (1 <= c && c <= 255 && asc_isalpha(c))
                    return true;
                break;

            case NFA_CLASS_BLANK:
                if (c == ' ' || c == '\t')
                    return true;
                break;

            case NFA_CLASS_CNTRL:
                if (1 <= c && c <= 255 && asc_iscntrl(c))
                    return true;
                break;

            case NFA_CLASS_DIGIT:
                if (asc_isdigit(c))
                    return true;
                break;

            case NFA_CLASS_GRAPH:
                if (1 <= c && c <= 255 && asc_isgraph(c))
                    return true;
                break;

            case NFA_CLASS_LOWER:
                if (utf_islower(c))
                    return true;
                break;

            case NFA_CLASS_PRINT:
                if (vim_isprintc(c))
                    return true;
                break;

            case NFA_CLASS_PUNCT:
                if (1 <= c && c <= 255 && asc_ispunct(c))
                    return true;
                break;

            case NFA_CLASS_SPACE:
                if ((9 <= c && c <= 13) || (c == ' '))
                    return true;
                break;

            case NFA_CLASS_UPPER:
                if (utf_isupper(c))
                    return true;
                break;

            case NFA_CLASS_XDIGIT:
                if (asc_isxdigit(c))
                    return true;
                break;

            case NFA_CLASS_TAB:
                if (c == '\t')
                    return true;
                break;

            case NFA_CLASS_RETURN:
                if (c == '\r')
                    return true;
                break;

            case NFA_CLASS_BACKSPACE:
                if (c == '\b')
                    return true;
                break;

            case NFA_CLASS_ESCAPE:
                if (c == '\033')
                    return true;
                break;

            default:
                /* should not be here :P */
                emsgn(e_ill_char_class, (long)klass);
                return false;
        }

        return false;
    }

    /*
     * Check for a match with subexpression "subidx".
     * Return true if it matches.
     */
    /*private*/ static boolean match_backref(regsub_C sub, int subidx, int[] bytelen)
        /* sub: pointers to subexpressions */
        /* bytelen: out: length of match in bytes */
    {
        if (sub.in_use <= subidx)
        {
            /* backref was not set, match an empty string */
            bytelen[0] = 0;
            return true;
        }

        if (reg_match == null)
        {
            if (sub.rs_multi[subidx].start_lnum < 0 || sub.rs_multi[subidx].end_lnum < 0)
            {
                /* backref was not set, match an empty string */
                bytelen[0] = 0;
                return true;
            }
            if (sub.rs_multi[subidx].start_lnum == reglnum && sub.rs_multi[subidx].end_lnum == reglnum)
            {
                int[] len = { sub.rs_multi[subidx].end_col - sub.rs_multi[subidx].start_col };
                if (cstrncmp(regline.plus(sub.rs_multi[subidx].start_col), reginput, len) == 0)
                {
                    bytelen[0] = len[0];
                    return true;
                }
            }
            else
            {
                if (match_with_backref(
                            sub.rs_multi[subidx].start_lnum,
                            sub.rs_multi[subidx].start_col,
                            sub.rs_multi[subidx].end_lnum,
                            sub.rs_multi[subidx].end_col,
                            bytelen) == RA_MATCH)
                    return true;
            }
        }
        else
        {
            if (sub.rs_line[subidx].start == null || sub.rs_line[subidx].end == null)
            {
                /* backref was not set, match an empty string */
                bytelen[0] = 0;
                return true;
            }
            int[] len = { BDIFF(sub.rs_line[subidx].end, sub.rs_line[subidx].start) };
            if (cstrncmp(sub.rs_line[subidx].start, reginput, len) == 0)
            {
                bytelen[0] = len[0];
                return true;
            }
        }

        return false;
    }

    /*
     * Check for a match with \z subexpression "subidx".
     * Return true if it matches.
     */
    /*private*/ static boolean match_zref(int subidx, int[] bytelen)
        /* bytelen: out: length of match in bytes */
    {
        cleanup_zsubexpr();

        if (re_extmatch_in == null || re_extmatch_in.matches[subidx] == null)
        {
            /* backref was not set, match an empty string */
            bytelen[0] = 0;
            return true;
        }

        int[] len = { strlen(re_extmatch_in.matches[subidx]) };
        if (cstrncmp(re_extmatch_in.matches[subidx], reginput, len) == 0)
        {
            bytelen[0] = len[0];
            return true;
        }

        return false;
    }

    /*
     * Save list IDs for all NFA states of "prog" into "list".
     * Also reset the IDs to zero.
     * Only used for the recursive value lastlist[1].
     */
    /*private*/ static void nfa_save_listids(nfa_regprog_C prog, int[] list)
    {
        /* Order in the list is reverse, it's a bit faster that way. */
        for (int i = 0, n = prog.nstate; 0 <= --n; i++)
        {
            nfa_state_C state = prog.states[i];
            if (state == null)
            {
                list[n] = 0;
                continue;
            }

            list[n] = state.lastlist[1];
            state.lastlist[1] = 0;
        }
    }

    /*
     * Restore list IDs from "list" to all NFA states.
     */
    /*private*/ static void nfa_restore_listids(nfa_regprog_C prog, int[] list)
    {
        for (int i = 0, n = prog.nstate; 0 <= --n; i++)
        {
            nfa_state_C state = prog.states[i];
            if (state == null)
                continue;

            state.lastlist[1] = list[n];
        }
    }

    /*private*/ static boolean nfa_re_num_cmp(long val, int op, long pos)
    {
        if (op == 1)
            return (val < pos);
        if (op == 2)
            return (pos < val);

        return (val == pos);
    }

    /*
     * Recursively call nfa_regmatch()
     * "pim" is null or contains info about a Postponed Invisible Match (start position).
     */
    /*private*/ static int recursive_regmatch(nfa_state_C state, nfa_pim_C pim, nfa_regprog_C prog, regsubs_C submatch, regsubs_C m, int[][] listids)
    {
        int save_reginput_col = BDIFF(reginput, regline);
        long save_reglnum = reglnum;
        int save_nfa_match = nfa_match;
        int save_nfa_listid = nfa_listid;
        save_se_C save_nfa_endp = nfa_endp;
        save_se_C endpos = new save_se_C();
        save_se_C endposp = null;
        boolean need_restore = false;

        if (pim != null)
        {
            /* start at the position where the postponed match was */
            if (reg_match == null)
                reginput = regline.plus(pim.end_pos.col);
            else
                reginput = pim.end_ptr;
        }

        if (state.c == NFA_START_INVISIBLE_BEFORE
         || state.c == NFA_START_INVISIBLE_BEFORE_FIRST
         || state.c == NFA_START_INVISIBLE_BEFORE_NEG
         || state.c == NFA_START_INVISIBLE_BEFORE_NEG_FIRST)
        {
            /* The recursive match must end at the current position.
             * When "pim" is not null it specifies the current position. */
            endposp = endpos;
            if (reg_match == null)
            {
                if (pim == null)
                {
                    endpos.se_pos.col = BDIFF(reginput, regline);
                    endpos.se_pos.lnum = reglnum;
                }
                else
                    COPY_lpos(endpos.se_pos, pim.end_pos);
            }
            else
            {
                if (pim == null)
                    endpos.se_ptr = reginput;
                else
                    endpos.se_ptr = pim.end_ptr;
            }

            /* Go back the specified number of bytes, or as far as the start of
             * the previous line, to try matching "\@<=" or not matching "\@<!".
             * This is very inefficient, limit the number of bytes if possible.
             */
            if (state.val <= 0)
            {
                if (reg_match == null)
                {
                    regline = reg_getline(--reglnum);
                    if (regline == null)
                        /* can't go before the first line */
                        regline = reg_getline(++reglnum);
                }
                reginput = regline;
            }
            else
            {
                if (reg_match == null && BDIFF(reginput, regline) < state.val)
                {
                    /* Not enough bytes in this line, go to end of previous line. */
                    regline = reg_getline(--reglnum);
                    if (regline == null)
                    {
                        /* can't go before the first line */
                        regline = reg_getline(++reglnum);
                        reginput = regline;
                    }
                    else
                        reginput = regline.plus(strlen(regline));
                }
                if (state.val <= BDIFF(reginput, regline))
                {
                    reginput = reginput.minus(state.val);
                    reginput = reginput.minus(us_head_off(regline, reginput));
                }
                else
                    reginput = regline;
            }
        }

        /* Have to clear the lastlist field of the NFA nodes, so that
         * nfa_regmatch() and addstate() can run properly after recursion. */
        if (nfa_ll_index == 1)
        {
            /* Already calling nfa_regmatch() recursively.
             * Save the lastlist[1] values and clear them. */
            if (listids[0] == null)
                listids[0] = new int[prog.nstate];
            nfa_save_listids(prog, listids[0]);
            need_restore = true;
            /* any value of nfa_listid will do */
        }
        else
        {
            /* First recursive nfa_regmatch() call, switch to the second lastlist entry.
             * Make sure nfa_listid is different from a previous recursive call,
             * because some states may still have this ID. */
            nfa_ll_index++;
            if (nfa_listid <= nfa_alt_listid)
                nfa_listid = nfa_alt_listid;
        }

        /* Call nfa_regmatch() to check if the current concat matches at this position.
         * The concat ends with the node NFA_END_INVISIBLE. */
        nfa_endp = endposp;
        int result = nfa_regmatch(prog, state.out0(), submatch, m);

        if (need_restore)
            nfa_restore_listids(prog, listids[0]);
        else
        {
            --nfa_ll_index;
            nfa_alt_listid = nfa_listid;
        }

        /* restore position in input text */
        reglnum = save_reglnum;
        if (reg_match == null)
            regline = reg_getline(reglnum);
        reginput = regline.plus(save_reginput_col);
        nfa_match = save_nfa_match;
        nfa_endp = save_nfa_endp;
        nfa_listid = save_nfa_listid;

        return result;
    }

    /*
     * Estimate the chance of a match with "state" failing.
     * empty match: 0
     * NFA_ANY: 1
     * specific character: 99
     */
    /*private*/ static int failure_chance(nfa_state_C state, int depth)
    {
        /* detect looping */
        if (4 < depth)
            return 1;

        int c = state.c;

        switch (c)
        {
            case NFA_SPLIT:
            {
                if (state.out0().c == NFA_SPLIT || state.out1().c == NFA_SPLIT)
                    /* avoid recursive stuff */
                    return 1;

                /* two alternatives, use the lowest failure chance */
                int l = failure_chance(state.out0(), depth + 1);
                int r = failure_chance(state.out1(), depth + 1);
                return (l < r) ? l : r;
            }

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
                return failure_chance(state.out0(), depth + 1);

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
                if (0 < c)
                    /* character match fails often */
                    return 95;
        }

        /* something else, includes character classes */
        return 50;
    }

    /*
     * Skip until the char "c" we know a match must start with.
     */
    /*private*/ static boolean skip_to_start(int c, int[] colp)
    {
        /* Used often, do some work to avoid call overhead. */
        Bytes s = cstrchr(regline.plus(colp[0]), c);
        if (s == null)
            return false;

        colp[0] = BDIFF(s, regline);
        return true;
    }

    /*
     * Check for a match with match_text.
     * Called after skip_to_start() has found regstart.
     * Returns zero for no match, 1 for a match.
     */
    /*private*/ static long find_match_text(int startcol, int regstart, Bytes match_text)
    {
        for (int[] col = { startcol }; ; )
        {
            boolean match = true;
            int len2 = utf_char2len(regstart);                      /* skip regstart */

            int c1, c2;
            for (int len1 = 0; match_text.at(len1) != NUL; len1 += utf_char2len(c1))
            {
                c1 = us_ptr2char(match_text.plus(len1));
                c2 = us_ptr2char(regline.plus(col[0] + len2));
                if (c1 != c2 && (!ireg_ic || utf_tolower(c1) != utf_tolower(c2)))
                {
                    match = false;
                    break;
                }
                len2 += utf_char2len(c2);
            }

            /* check that no composing char follows */
            if (match && !utf_iscomposing(us_ptr2char(regline.plus(col[0] + len2))))
            {
                cleanup_subexpr();
                if (reg_match == null)
                {
                    reg_startpos[0].lnum = reglnum;
                    reg_startpos[0].col = col[0];
                    reg_endpos[0].lnum = reglnum;
                    reg_endpos[0].col = col[0] + len2;
                }
                else
                {
                    reg_startp[0] = regline.plus(col[0]);
                    reg_endp[0] = regline.plus(col[0] + len2);
                }
                return 1L;
            }

            /* Try finding regstart after the current match. */
            col[0] += utf_char2len(regstart);                          /* skip regstart */
            if (skip_to_start(regstart, col) == false)
                break;
        }

        return 0L;
    }

    /*
     * Main matching routine.
     *
     * Run NFA to determine whether it matches reginput.
     *
     * When "nfa_endp" is not null it is a required end-of-match position.
     *
     * Return true if there is a match, false otherwise.
     * When there is a match "submatch" contains the positions.
     * Note: Caller must ensure that: start != null.
     */
    /*private*/ static int nfa_regmatch(nfa_regprog_C prog, nfa_state_C start, regsubs_C submatch, regsubs_C m)
    {
        boolean toplevel = (start.c == NFA_MOPEN);

        /* Some patterns may take a long time to match, especially when using recursive_regmatch().
         * Allow interrupting them with CTRL-C. */
        fast_breakcheck();
        if (got_int)
            return FALSE;
        if (nfa_time_limit != null && profile_passed_limit(nfa_time_limit))
            return FALSE;

        nfa_match = FALSE;

        nfa_list_C[] list = ARRAY_nfa_list(2);
        list[0].threads = ARRAY_nfa_thread(list[0].len = prog.nstate + 1);
        list[1].threads = ARRAY_nfa_thread(list[1].len = prog.nstate + 1);

        nfa_list_C thislist = list[0];
        thislist.n = 0;
        thislist.has_pim = false;
        nfa_list_C nextlist = list[1];
        nextlist.n = 0;
        nextlist.has_pim = false;
        thislist.id = nfa_listid + 1;

        /* Inline optimized code for addstate(thislist, start, m, 0) if we know it's the first MOPEN. */
        if (toplevel)
        {
            if (reg_match == null)
            {
                m.rs_norm.rs_multi[0].start_lnum = reglnum;
                m.rs_norm.rs_multi[0].start_col = BDIFF(reginput, regline);
            }
            else
                m.rs_norm.rs_line[0].start = reginput;
            m.rs_norm.in_use = 1;
            addstate(thislist, start.out0(), m, null, 0);
        }
        else
            addstate(thislist, start, m, null, 0);

        boolean go_to_nextline = false;
        int flag = 0;
        int[][] listids = { null };
        int add_off = 0;

        /*
         * Run for each character.
         */
        for ( ; ; )
        {
            int curc = us_ptr2char(reginput);
            int clen = us_ptr2len_cc(reginput);
            if (curc == NUL)
            {
                clen = 0;
                go_to_nextline = false;
            }

            /* swap lists */
            thislist = list[flag];
            nextlist = list[flag ^= 1];
            nextlist.n = 0;                 /* clear nextlist */
            nextlist.has_pim = false;
            nfa_listid++;
            if (prog.re_engine == AUTOMATIC_ENGINE && NFA_MAX_STATES <= nfa_listid)
            {
                /* too many states, retry with old engine */
                nfa_match = NFA_TOO_EXPENSIVE;
                return nfa_match;
            }

            thislist.id = nfa_listid;
            nextlist.id = nfa_listid + 1;

            /*
             * If the state lists are empty we can stop.
             */
            if (thislist.n == 0)
                break;

            nextchar:
            {
                /* compute nextlist */
                for (int[] lidx = { 0 }; lidx[0] < thislist.n; lidx[0]++)
                {
                    nfa_thread_C thread = thislist.threads[lidx[0]];

                    /*
                     * Handle the possible codes of the current state.
                     * The most important is NFA_MATCH.
                     */
                    nfa_state_C add_state = null;
                    boolean add_here = false;
                    int add_count = 0;

                    switch (thread.state.c)
                    {
                        case NFA_MATCH:
                        {
                            /* If the match ends before a composing characters and
                             * ireg_icombine is not set, that is not really a match. */
                            if (!ireg_icombine && utf_iscomposing(curc))
                                break;
                            nfa_match = TRUE;
                            copy_sub(submatch.rs_norm, thread.th_subs.rs_norm);
                            if (nfa_has_zsubexpr)
                                copy_sub(submatch.rs_synt, thread.th_subs.rs_synt);
                            /* Found the left-most longest match, do not look at any other states
                             * at this position.  When the list of states is going to be empty
                             * quit without advancing, so that "reginput" is correct. */
                            if (nextlist.n == 0)
                                clen = 0;
                            break nextchar;
                        }

                        case NFA_END_INVISIBLE:
                        case NFA_END_INVISIBLE_NEG:
                        case NFA_END_PATTERN:
                        {
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
                            /* If "nfa_endp" is set it's only a match if it ends at "nfa_endp". */
                            if (nfa_endp != null && ((reg_match == null)
                                    ? (reglnum != nfa_endp.se_pos.lnum
                                        || BDIFF(reginput, regline) != nfa_endp.se_pos.col)
                                    : BNE(reginput, nfa_endp.se_ptr)))
                                break;

                            /* do not set submatches for \@! */
                            if (thread.state.c != NFA_END_INVISIBLE_NEG)
                            {
                                copy_sub(m.rs_norm, thread.th_subs.rs_norm);
                                if (nfa_has_zsubexpr)
                                    copy_sub(m.rs_synt, thread.th_subs.rs_synt);
                            }
                            nfa_match = TRUE;
                            /* See comment above at "goto nextchar". */
                            if (nextlist.n == 0)
                                clen = 0;
                            break nextchar;
                        }

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
                            if (thread.th_pim.result != NFA_PIM_UNUSED
                                || thread.state.c == NFA_START_INVISIBLE_FIRST
                                || thread.state.c == NFA_START_INVISIBLE_NEG_FIRST
                                || thread.state.c == NFA_START_INVISIBLE_BEFORE_FIRST
                                || thread.state.c == NFA_START_INVISIBLE_BEFORE_NEG_FIRST)
                            {
                                int in_use = m.rs_norm.in_use;

                                /* Copy submatch info for the recursive call,
                                 * opposite of what happens on success below. */
                                copy_sub_off(m.rs_norm, thread.th_subs.rs_norm);
                                if (nfa_has_zsubexpr)
                                    copy_sub_off(m.rs_synt, thread.th_subs.rs_synt);

                                /*
                                 * First try matching the invisible match, then what follows.
                                 */
                                int result = recursive_regmatch(thread.state, null, prog, submatch, m, listids);
                                if (result == NFA_TOO_EXPENSIVE)
                                {
                                    nfa_match = result;
                                    return nfa_match;
                                }

                                /* for \@! and \@<! it is a match when the result is false */
                                if ((result != FALSE) != (thread.state.c == NFA_START_INVISIBLE_NEG
                                    || thread.state.c == NFA_START_INVISIBLE_NEG_FIRST
                                    || thread.state.c == NFA_START_INVISIBLE_BEFORE_NEG
                                    || thread.state.c == NFA_START_INVISIBLE_BEFORE_NEG_FIRST))
                                {
                                    /* Copy submatch info from the recursive call. */
                                    copy_sub_off(thread.th_subs.rs_norm, m.rs_norm);
                                    if (nfa_has_zsubexpr)
                                        copy_sub_off(thread.th_subs.rs_synt, m.rs_synt);
                                    /* If the pattern has \ze and it matched in the sub pattern, use it. */
                                    copy_ze_off(thread.th_subs.rs_norm, m.rs_norm);

                                    /* thread.state.out1 is the corresponding END_INVISIBLE node.
                                     * Add its out0 to the current list (zero-width match). */
                                    add_here = true;
                                    add_state = thread.state.out1().out0();
                                }
                                m.rs_norm.in_use = in_use;
                            }
                            else
                            {
                                nfa_pim_C pim = new nfa_pim_C();

                                /*
                                 * First try matching what follows.  Only if a match
                                 * is found verify the invisible match matches.  Add a
                                 * nfa_pim_C to the following states, it contains info
                                 * about the invisible match.
                                 */
                                pim.state = thread.state;
                                pim.result = NFA_PIM_TODO;
                                pim.np_subs.rs_norm.in_use = 0;
                                pim.np_subs.rs_synt.in_use = 0;
                                if (reg_match == null)
                                {
                                    pim.end_pos.col = BDIFF(reginput, regline);
                                    pim.end_pos.lnum = reglnum;
                                }
                                else
                                    pim.end_ptr = reginput;

                                /* thread.state.out1 is the corresponding END_INVISIBLE node.
                                 * Add its out0 to the current list (zero-width match). */
                                addstate_here(thislist, thread.state.out1().out0(), thread.th_subs, pim, lidx);
                            }
                            break;
                        }

                        case NFA_START_PATTERN:
                        {
                            nfa_state_C skip = null;

                            /* There is no point in trying to match the pattern
                             * if the output state is not going to be added to the list. */
                            if (state_in_list(nextlist, thread.state.out1().out0(), thread.th_subs))
                            {
                                skip = thread.state.out1().out0();
                            }
                            else if (state_in_list(nextlist, thread.state.out1().out0().out0(), thread.th_subs))
                            {
                                skip = thread.state.out1().out0().out0();
                            }
                            else if (state_in_list(thislist, thread.state.out1().out0().out0(), thread.th_subs))
                            {
                                skip = thread.state.out1().out0().out0();
                            }

                            if (skip != null)
                                break;

                            /* Copy submatch info to the recursive call, opposite of what happens afterwards. */
                            copy_sub_off(m.rs_norm, thread.th_subs.rs_norm);
                            if (nfa_has_zsubexpr)
                                copy_sub_off(m.rs_synt, thread.th_subs.rs_synt);

                            /* First try matching the pattern. */
                            int result = recursive_regmatch(thread.state, null, prog, submatch, m, listids);
                            if (result == NFA_TOO_EXPENSIVE)
                            {
                                nfa_match = result;
                                return nfa_match;
                            }
                            if (result != FALSE)
                            {
                                int bytelen;

                                /* Copy submatch info from the recursive call. */
                                copy_sub_off(thread.th_subs.rs_norm, m.rs_norm);
                                if (nfa_has_zsubexpr)
                                    copy_sub_off(thread.th_subs.rs_synt, m.rs_synt);
                                /* Now we need to skip over the matched text and
                                 * then continue with what follows. */
                                if (reg_match == null)
                                    /* TODO: multi-line match */
                                    bytelen = m.rs_norm.rs_multi[0].end_col - BDIFF(reginput, regline);
                                else
                                    bytelen = BDIFF(m.rs_norm.rs_line[0].end, reginput);

                                if (bytelen == 0)
                                {
                                    /* Empty match: output of corresponding NFA_END_PATTERN/NFA_SKIP
                                     * to be used at current position. */
                                    add_here = true;
                                    add_state = thread.state.out1().out0().out0();
                                }
                                else if (bytelen <= clen)
                                {
                                    /* Match current character, output of corresponding
                                     * NFA_END_PATTERN to be used at next position. */
                                    add_state = thread.state.out1().out0().out0();
                                    add_off = clen;
                                }
                                else
                                {
                                    /* Skip over the matched characters, set character count in NFA_SKIP. */
                                    add_state = thread.state.out1().out0();
                                    add_off = bytelen;
                                    add_count = bytelen - clen;
                                }
                            }
                            break;
                        }

                        case NFA_BOL:
                            if (BEQ(reginput, regline))
                            {
                                add_here = true;
                                add_state = thread.state.out0();
                            }
                            break;

                        case NFA_EOL:
                            if (curc == NUL)
                            {
                                add_here = true;
                                add_state = thread.state.out0();
                            }
                            break;

                        case NFA_BOW:
                        {
                            boolean result = true;
                            if (curc == NUL)
                                result = false;
                            else
                            {
                                int this_class;

                                /* Get class of current and previous char (if it exists). */
                                this_class = us_get_class(reginput, reg_buf);
                                if (this_class <= 1)
                                    result = false;
                                else if (reg_prev_class() == this_class)
                                    result = false;
                            }
                            if (result)
                            {
                                add_here = true;
                                add_state = thread.state.out0();
                            }
                            break;
                        }

                        case NFA_EOW:
                        {
                            boolean result = true;
                            if (BEQ(reginput, regline))
                                result = false;
                            else
                            {
                                int this_class, prev_class;

                                /* Get class of current and previous char (if it exists). */
                                this_class = us_get_class(reginput, reg_buf);
                                prev_class = reg_prev_class();
                                if (this_class == prev_class || prev_class == 0 || prev_class == 1)
                                    result = false;
                            }
                            if (result)
                            {
                                add_here = true;
                                add_state = thread.state.out0();
                            }
                            break;
                        }

                        case NFA_BOF:
                            if (reglnum == 0 && BEQ(reginput, regline) && (reg_match != null || reg_firstlnum == 1))
                            {
                                add_here = true;
                                add_state = thread.state.out0();
                            }
                            break;

                        case NFA_EOF:
                            if (reglnum == reg_maxline && curc == NUL)
                            {
                                add_here = true;
                                add_state = thread.state.out0();
                            }
                            break;

                        case NFA_COMPOSING:
                        {
                            int mc = curc;
                            int[] cchars = new int[MAX_MCO];
                            int ccount = 0;

                            nfa_state_C sta = thread.state.out0();
                            int len = 0;
                            if (utf_iscomposing(sta.c))
                            {
                                /* Only match composing character(s), ignore base character.
                                 * Used for ".{composing}" and "{composing}" (no preceding character). */
                                len += utf_char2len(mc);
                            }

                            boolean result;
                            if (ireg_icombine && len == 0)
                            {
                                /* If \Z was present, then ignore composing characters.
                                 * When ignoring the base character this always matches. */
                                if (len == 0 && sta.c != curc)
                                    result = false;
                                else
                                    result = true;
                                while (sta.c != NFA_END_COMPOSING)
                                    sta = sta.out0();
                            }
                            /* Check base character matches first, unless ignored. */
                            else if (0 < len || mc == sta.c)
                            {
                                if (len == 0)
                                {
                                    len += utf_char2len(mc);
                                    sta = sta.out0();
                                }

                                /* We don't care about the order of composing characters.
                                 * Get them into cchars[] first. */
                                while (len < clen)
                                {
                                    mc = us_ptr2char(reginput.plus(len));
                                    cchars[ccount++] = mc;
                                    len += utf_char2len(mc);
                                    if (ccount == MAX_MCO)
                                        break;
                                }

                                /* Check that each composing char in the pattern matches
                                 * a composing char in the text.
                                 * We do not check if all composing chars are matched. */
                                result = true;
                                while (sta.c != NFA_END_COMPOSING)
                                {
                                    int j;
                                    for (j = 0; j < ccount; j++)
                                        if (cchars[j] == sta.c)
                                            break;
                                    if (j == ccount)
                                    {
                                        result = false;
                                        break;
                                    }
                                    sta = sta.out0();
                                }
                            }
                            else
                                result = false;

                            nfa_state_C end = thread.state.out1();    /* NFA_END_COMPOSING */

                            if (result)
                            {
                                add_state = end.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_NEWL:
                        {
                            if (curc == NUL && !reg_line_lbr && reg_match == null && reglnum <= reg_maxline)
                            {
                                go_to_nextline = true;
                                /* Pass -1 for the offset, which means
                                 * taking the position at the start of the next line. */
                                add_state = thread.state.out0();
                                add_off = -1;
                            }
                            else if (curc == '\n' && reg_line_lbr)
                            {
                                /* match \n as if it is an ordinary character */
                                add_state = thread.state.out0();
                                add_off = 1;
                            }
                            break;
                        }

                        case NFA_START_COLL:
                        case NFA_START_NEG_COLL:
                        {
                            /* What follows is a list of characters, until NFA_END_COLL.
                             * One of them must match or none of them must match. */

                            /* Never match EOL.
                             * If it's part of the collection it is added as a separate state with an OR. */
                            if (curc == NUL)
                                break;

                            boolean result_if_matched = (thread.state.c == NFA_START_COLL);

                            boolean result = false;	// %% anno dunno
                            for (nfa_state_C state = thread.state.out0(); ; state = state.out0())
                            {
                                if (state.c == NFA_END_COLL)
                                {
                                    result = !result_if_matched;
                                    break;
                                }
                                if (state.c == NFA_RANGE_MIN)
                                {
                                    int c1 = state.val;
                                    state = state.out0(); /* advance to NFA_RANGE_MAX */
                                    int c2 = state.val;
                                    if (c1 <= curc && curc <= c2)
                                    {
                                        result = result_if_matched;
                                        break;
                                    }
                                    if (ireg_ic)
                                    {
                                        int curc_low = utf_tolower(curc);
                                        boolean done = false;

                                        for ( ; c1 <= c2; ++c1)
                                            if (utf_tolower(c1) == curc_low)
                                            {
                                                result = result_if_matched;
                                                done = true;
                                                break;
                                            }
                                        if (done)
                                            break;
                                    }
                                }
                                else if (state.c < 0 ? check_char_class(state.c, curc) : (curc == state.c || (ireg_ic && utf_tolower(curc) == utf_tolower(state.c))))
                                {
                                    result = result_if_matched;
                                    break;
                                }
                            }
                            if (result)
                            {
                                /* next state is in out of the NFA_END_COLL,
                                 * out1 of START points to the END state */
                                add_state = thread.state.out1().out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_ANY:
                            /* Any char except NUL, (end of input) does not match. */
                            if (0 < curc)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;

                        case NFA_ANY_COMPOSING:
                            /* On a composing character skip over it.
                             * Otherwise do nothing.
                             * Always matches. */
                            if (utf_iscomposing(curc))
                            {
                                add_off = clen;
                            }
                            else
                            {
                                add_here = true;
                                add_off = 0;
                            }
                            add_state = thread.state.out0();
                            break;

                        /*
                         * Character classes like \a for alpha, \d for digit etc.
                         */
                        case NFA_IDENT:     /*  \i  */
                        {
                            boolean result = vim_isIDc(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_SIDENT:    /*  \I  */
                        {
                            boolean result = !asc_isdigit(curc) && vim_isIDc(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_KWORD:     /*  \k  */
                        {
                            boolean result = us_iswordp(reginput, reg_buf);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_SKWORD:    /*  \K  */
                        {
                            boolean result = !asc_isdigit(curc) && us_iswordp(reginput, reg_buf);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_FNAME:     /*  \f  */
                        {
                            boolean result = vim_isfilec(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_SFNAME:    /*  \F  */
                        {
                            boolean result = !asc_isdigit(curc) && vim_isfilec(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_PRINT:     /*  \p  */
                        {
                            boolean result = vim_isprintc(us_ptr2char(reginput));
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_SPRINT:    /*  \P  */
                        {
                            boolean result = !asc_isdigit(curc) && vim_isprintc(us_ptr2char(reginput));
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_WHITE:     /*  \s  */
                        {
                            boolean result = vim_iswhite(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_NWHITE:    /*  \S  */
                        {
                            boolean result = (curc != NUL) && !vim_iswhite(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_DIGIT:     /*  \d  */
                        {
                            boolean result = ri_digit(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_NDIGIT:    /*  \D  */
                        {
                            boolean result = (curc != NUL) && !ri_digit(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_HEX:       /*  \x  */
                        {
                            boolean result = ri_hex(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_NHEX:      /*  \X  */
                        {
                            boolean result = (curc != NUL) && !ri_hex(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_OCTAL:     /*  \o  */
                        {
                            boolean result = ri_octal(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_NOCTAL:    /*  \O  */
                        {
                            boolean result = (curc != NUL) && !ri_octal(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_WORD:      /*  \w  */
                        {
                            boolean result = ri_word(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_NWORD:     /*  \W  */
                        {
                            boolean result = (curc != NUL) && !ri_word(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_HEAD:      /*  \h  */
                        {
                            boolean result = ri_head(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_NHEAD:     /*  \H  */
                        {
                            boolean result = (curc != NUL) && !ri_head(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_ALPHA:     /*  \a  */
                        {
                            boolean result = ri_alpha(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_NALPHA:    /*  \A  */
                        {
                            boolean result = (curc != NUL) && !ri_alpha(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_LOWER:     /*  \l  */
                        {
                            boolean result = ri_lower(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_NLOWER:    /*  \L  */
                        {
                            boolean result = (curc != NUL) && !ri_lower(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_UPPER:     /*  \\u (sic!) */
                        {
                            boolean result = ri_upper(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_NUPPER:    /*  \U  */
                        {
                            boolean result = (curc != NUL) && !ri_upper(curc);
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_LOWER_IC:  /* [a-z] */
                        {
                            boolean result = ri_lower(curc) || (ireg_ic && ri_upper(curc));
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_NLOWER_IC: /* [^a-z] */
                        {
                            boolean result = (curc != NUL) && !(ri_lower(curc) || (ireg_ic && ri_upper(curc)));
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_UPPER_IC:  /* [A-Z] */
                        {
                            boolean result = ri_upper(curc) || (ireg_ic && ri_lower(curc));
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

                        case NFA_NUPPER_IC: /* ^[A-Z] */
                        {
                            boolean result = (curc != NUL) && !(ri_upper(curc) || (ireg_ic && ri_lower(curc)));
                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }

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
                        case NFA_ZREF9: /* \1 .. \9  \z1 .. \z9 */
                        {
                            int subidx;
                            int[] bytelen = new int[1];

                            boolean result;
                            if (thread.state.c <= NFA_BACKREF9)
                            {
                                subidx = thread.state.c - NFA_BACKREF1 + 1;
                                result = match_backref(thread.th_subs.rs_norm, subidx, bytelen);
                            }
                            else
                            {
                                subidx = thread.state.c - NFA_ZREF1 + 1;
                                result = match_zref(subidx, bytelen);
                            }

                            if (result)
                            {
                                if (bytelen[0] == 0)
                                {
                                    /* Empty match always works, output of NFA_SKIP to be used next. */
                                    add_here = true;
                                    add_state = thread.state.out0().out0();
                                }
                                else if (bytelen[0] <= clen)
                                {
                                    /* Match current character, jump ahead to out of NFA_SKIP. */
                                    add_state = thread.state.out0().out0();
                                    add_off = clen;
                                }
                                else
                                {
                                    /* Skip over the matched characters, set character count in NFA_SKIP. */
                                    add_state = thread.state.out0();
                                    add_off = bytelen[0];
                                    add_count = bytelen[0] - clen;
                                }
                            }
                            break;
                        }

                        case NFA_SKIP:
                        {
                            /* character of previous matching \1 .. \9  or \@> */
                            if (thread.count - clen <= 0)
                            {
                                /* end of match, go to what follows */
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            else
                            {
                                /* add state again with decremented count */
                                add_state = thread.state;
                                add_off = 0;
                                add_count = thread.count - clen;
                            }
                            break;
                        }

                        case NFA_LNUM:
                        case NFA_LNUM_GT:
                        case NFA_LNUM_LT:
                        {
                            boolean result = (reg_match == null
                                && nfa_re_num_cmp(thread.state.val, thread.state.c - NFA_LNUM,
                                            reglnum + reg_firstlnum));
                            if (result)
                            {
                                add_here = true;
                                add_state = thread.state.out0();
                            }
                            break;
                        }

                        case NFA_COL:
                        case NFA_COL_GT:
                        case NFA_COL_LT:
                        {
                            boolean result = nfa_re_num_cmp(thread.state.val, thread.state.c - NFA_COL,
                                                    BDIFF(reginput, regline) + 1);
                            if (result)
                            {
                                add_here = true;
                                add_state = thread.state.out0();
                            }
                            break;
                        }

                        case NFA_VCOL:
                        case NFA_VCOL_GT:
                        case NFA_VCOL_LT:
                        {
                            int op = thread.state.c - NFA_VCOL;
                            int col = BDIFF(reginput, regline);
                            window_C wp = (reg_win == null) ? curwin : reg_win;

                            /* Bail out quickly when there can't be a match,
                             * avoid the overhead of win_linetabsize() on long lines. */
                            if (op != 1 && thread.state.val * MB_MAXBYTES < col)
                                break;

                            boolean result = false;
                            if (op == 1 && thread.state.val < col - 1 && 100 < col)
                            {
                                int ts = (int)wp.w_buffer.b_p_ts[0];

                                /* Guess that a character won't use more columns than 'tabstop',
                                 * with a minimum of 4. */
                                if (ts < 4)
                                    ts = 4;
                                result = (thread.state.val * ts < col);
                            }
                            if (!result)
                                result = nfa_re_num_cmp(thread.state.val, op, (long)win_linetabsize(wp, regline, col) + 1);
                            if (result)
                            {
                                add_here = true;
                                add_state = thread.state.out0();
                            }
                            break;
                        }

                        case NFA_MARK:
                        case NFA_MARK_GT:
                        case NFA_MARK_LT:
                        {
                            pos_C pos = getmark_buf(reg_buf, thread.state.val, false);

                            /* Compare the mark position to the match position. */
                            boolean result = (pos != null           /* mark doesn't exist */
                                    && 0 < pos.lnum                 /* mark isn't set in reg_buf */
                                    && (pos.lnum == reglnum + reg_firstlnum
                                            ? (pos.col == BDIFF(reginput, regline)
                                                ? thread.state.c == NFA_MARK
                                                : (pos.col < BDIFF(reginput, regline)
                                                    ? thread.state.c == NFA_MARK_GT
                                                    : thread.state.c == NFA_MARK_LT))
                                            : (pos.lnum < reglnum + reg_firstlnum
                                                ? thread.state.c == NFA_MARK_GT
                                                : thread.state.c == NFA_MARK_LT)));
                            if (result)
                            {
                                add_here = true;
                                add_state = thread.state.out0();
                            }
                            break;
                        }

                        case NFA_CURSOR:
                        {
                            boolean result = (reg_win != null
                                    && reglnum + reg_firstlnum == reg_win.w_cursor.lnum
                                    && BDIFF(reginput, regline) == reg_win.w_cursor.col);
                            if (result)
                            {
                                add_here = true;
                                add_state = thread.state.out0();
                            }
                            break;
                        }

                        case NFA_VISUAL:
                        {
                            boolean result = reg_match_visual();
                            if (result)
                            {
                                add_here = true;
                                add_state = thread.state.out0();
                            }
                            break;
                        }

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
                            int c = thread.state.c;

                            boolean result = (c == curc);
                            if (!result && ireg_ic)
                                result = (utf_tolower(c) == utf_tolower(curc));

                            /* If ireg_icombine is not set only skip over the character itself.
                             * When it is set skip over composing characters. */
                            if (result && !ireg_icombine)
                                clen = utf_char2len(curc);

                            if (result)
                            {
                                add_state = thread.state.out0();
                                add_off = clen;
                            }
                            break;
                        }
                    }

                    if (add_state != null)
                    {
                        nfa_pim_C pim = (thread.th_pim.result != NFA_PIM_UNUSED) ? thread.th_pim : null;

                        /* Handle the postponed invisible match if the match might end
                         * without advancing and before the end of the line. */
                        if (pim != null && (clen == 0 || match_follows(add_state, 0)))
                        {
                            int result;
                            if (pim.result == NFA_PIM_TODO)
                            {
                                result = recursive_regmatch(pim.state, pim, prog, submatch, m, listids);
                                pim.result = (result != FALSE) ? NFA_PIM_MATCH : NFA_PIM_NOMATCH;
                                /* for \@! and \@<! it is a match when the result is false */
                                if ((result != FALSE) != (pim.state.c == NFA_START_INVISIBLE_NEG
                                            || pim.state.c == NFA_START_INVISIBLE_NEG_FIRST
                                            || pim.state.c == NFA_START_INVISIBLE_BEFORE_NEG
                                            || pim.state.c == NFA_START_INVISIBLE_BEFORE_NEG_FIRST))
                                {
                                    /* Copy submatch info from the recursive call. */
                                    copy_sub_off(pim.np_subs.rs_norm, m.rs_norm);
                                    if (nfa_has_zsubexpr)
                                        copy_sub_off(pim.np_subs.rs_synt, m.rs_synt);
                                }
                            }
                            else
                            {
                                result = (pim.result == NFA_PIM_MATCH) ? TRUE : FALSE;
                            }

                            /* for \@! and \@<! it is a match when result is false */
                            if ((result != FALSE) != (pim.state.c == NFA_START_INVISIBLE_NEG
                                        || pim.state.c == NFA_START_INVISIBLE_NEG_FIRST
                                        || pim.state.c == NFA_START_INVISIBLE_BEFORE_NEG
                                        || pim.state.c == NFA_START_INVISIBLE_BEFORE_NEG_FIRST))
                            {
                                /* Copy submatch info from the recursive call. */
                                copy_sub_off(thread.th_subs.rs_norm, pim.np_subs.rs_norm);
                                if (nfa_has_zsubexpr)
                                    copy_sub_off(thread.th_subs.rs_synt, pim.np_subs.rs_synt);
                            }
                            else
                            {
                                /* look-behind match failed, don't add the state */
                                continue;
                            }

                            /* Postponed invisible match was handled, don't add it to following states. */
                            pim = null;
                        }

                        nfa_pim_C pim_copy = new nfa_pim_C();

                        /* If "pim" points into nfl.threads,
                         * it will become invalid when adding the state causes the list to be reallocated.
                         * Make a local copy to avoid that. */
                        if (pim == thread.th_pim)
                        {
                            copy_pim(pim_copy, pim);
                            pim = pim_copy;
                        }

                        if (add_here)
                            addstate_here(thislist, add_state, thread.th_subs, pim, lidx);
                        else
                        {
                            addstate(nextlist, add_state, thread.th_subs, pim, add_off);
                            if (0 < add_count)
                                nextlist.threads[nextlist.n - 1].count = add_count;
                        }
                    }
                }

                /* Look for the start of a match in the current position
                 * by adding the start state to the list of states.
                 * The first found match is the leftmost one, thus the order of states matters!
                 * Do not add the start state in recursive calls of nfa_regmatch(),
                 * because recursive calls should only start in the first position.
                 * Unless "nfa_endp" is not null, then we match the end position.
                 * Also don't start a match past the first line. */
                if (nfa_match == FALSE
                        && ((toplevel
                                && reglnum == 0
                                && clen != 0
                                && (ireg_maxcol == 0 || BDIFF(reginput, regline) < ireg_maxcol))
                            || (nfa_endp != null
                                && ((reg_match == null)
                                    ? (reglnum < nfa_endp.se_pos.lnum
                                    || (reglnum == nfa_endp.se_pos.lnum
                                        && BDIFF(reginput, regline) < nfa_endp.se_pos.col))
                                    : BLT(reginput, nfa_endp.se_ptr)))))
                {
                    /* Inline optimized code for addstate() if we know the state is the first MOPEN. */
                    if (toplevel)
                    {
                        boolean add = true;

                        if (prog.regstart != NUL && clen != 0)
                        {
                            if (nextlist.n == 0)
                            {
                                int[] col = { BDIFF(reginput, regline) + clen };

                                /* Nextlist is empty, we can skip ahead to the
                                 * character that must appear at the start. */
                                if (skip_to_start(prog.regstart, col) == false)
                                    break;
                                reginput = regline.plus(col[0] - clen);
                            }
                            else
                            {
                                /* Checking if the required start character matches is
                                 * cheaper than adding a state that won't match. */
                                int c = us_ptr2char(reginput.plus(clen));
                                if (c != prog.regstart
                                    && (!ireg_ic || utf_tolower(c) != utf_tolower(prog.regstart)))
                                {
                                    add = false;
                                }
                            }
                        }

                        if (add)
                        {
                            if (reg_match == null)
                                m.rs_norm.rs_multi[0].start_col = BDIFF(reginput, regline) + clen;
                            else
                                m.rs_norm.rs_line[0].start = reginput.plus(clen);
                            addstate(nextlist, start.out0(), m, null, clen);
                        }
                    }
                    else
                        addstate(nextlist, start, m, null, clen);
                }
            }

            /* Advance to the next character, or advance to the next line, or finish. */
            if (clen != 0)
                reginput = reginput.plus(clen);
            else if (go_to_nextline || (nfa_endp != null && reg_match == null && reglnum < nfa_endp.se_pos.lnum))
                reg_nextline();
            else
                break;

            /* Allow interrupting with CTRL-C. */
            line_breakcheck();
            if (got_int)
                break;

            /* Check for timeout once in a twenty times to avoid overhead. */
            if (nfa_time_limit != null && ++nfa_time_count == 20)
            {
                nfa_time_count = 0;
                if (profile_passed_limit(nfa_time_limit))
                    break;
            }
        }

        return nfa_match;
    }

    /*
     * Try match of "prog" with at regline[col].
     * Returns <= 0 for failure, number of lines contained in the match otherwise.
     */
    /*private*/ static long nfa_regtry(nfa_regprog_C prog, int col, timeval_C tm)
        /* tm: timeout limit or null */
    {
        regsubs_C subs = new regsubs_C();
        regsubs_C m = new regsubs_C();
        nfa_state_C start = prog.start;

        reginput = regline.plus(col);
        nfa_time_limit = tm;
        nfa_time_count = 0;

        clear_sub(subs.rs_norm);
        clear_sub(m.rs_norm);
        clear_sub(subs.rs_synt);
        clear_sub(m.rs_synt);

        int result = nfa_regmatch(prog, start, subs, m);
        if (result == FALSE)
            return 0;
        else if (result == NFA_TOO_EXPENSIVE)
            return result;

        cleanup_subexpr();
        if (reg_match == null)
        {
            for (int i = 0; i < subs.rs_norm.in_use; i++)
            {
                reg_startpos[i].lnum = subs.rs_norm.rs_multi[i].start_lnum;
                reg_startpos[i].col = subs.rs_norm.rs_multi[i].start_col;

                reg_endpos[i].lnum = subs.rs_norm.rs_multi[i].end_lnum;
                reg_endpos[i].col = subs.rs_norm.rs_multi[i].end_col;
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
                reg_endpos[0].col = BDIFF(reginput, regline);
            }
            else
                /* Use line number of "\ze". */
                reglnum = reg_endpos[0].lnum;
        }
        else
        {
            for (int i = 0; i < subs.rs_norm.in_use; i++)
            {
                reg_startp[i] = subs.rs_norm.rs_line[i].start;
                reg_endp[i] = subs.rs_norm.rs_line[i].end;
            }

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
            for (int i = 0; i < subs.rs_synt.in_use; i++)
            {
                if (reg_match == null)
                {
                    multipos_C mp = subs.rs_synt.rs_multi[i];

                    /* Only accept single line matches that are valid. */
                    if (0 <= mp.start_lnum && mp.start_lnum == mp.end_lnum && mp.start_col <= mp.end_col)
                        re_extmatch_out.matches[i] = STRNDUP(reg_getline(mp.start_lnum).plus(mp.start_col), mp.end_col - mp.start_col);
                }
                else
                {
                    linepos_C lp = subs.rs_synt.rs_line[i];

                    if (lp.start != null && lp.end != null)
                        re_extmatch_out.matches[i] = STRNDUP(lp.start, BDIFF(lp.end, lp.start));
                }
            }
        }

        return 1 + reglnum;
    }

    /*
     * Match a regexp against
     *  a string ("line" points to the string)
     *  or multiple lines ("line" is null, use reg_getline()).
     *
     * Returns <= 0 for failure, number of lines contained in the match otherwise.
     */
    /*private*/ static long nfa_regexec_both(Bytes line, int startcol, timeval_C tm)
        /* startcol: column to start looking for match */
        /* tm: timeout limit or null */
    {
        int[] col = { startcol };

        nfa_regprog_C prog;
        if (reg_match == null)
        {
            prog = (nfa_regprog_C)reg_mmatch.regprog;
            line = reg_getline(0);              /* relative to the cursor */
            reg_startpos = reg_mmatch.startpos;
            reg_endpos = reg_mmatch.endpos;
        }
        else
        {
            prog = (nfa_regprog_C)reg_match.regprog;
            reg_startp = reg_match.startp;
            reg_endp = reg_match.endp;
        }

        /* Be paranoid... */
        if (prog == null || line == null)
        {
            emsg(e_null);
            return 0L;
        }

        /* If pattern contains "\c" or "\C": overrule value of ireg_ic. */
        if ((prog.regflags & RF_ICASE) != 0)
            ireg_ic = true;
        else if ((prog.regflags & RF_NOICASE) != 0)
            ireg_ic = false;

        /* If pattern contains "\Z" overrule value of ireg_icombine. */
        if ((prog.regflags & RF_ICOMBINE) != 0)
            ireg_icombine = true;

        regline = line;
        reglnum = 0;    /* relative to line */

        nfa_has_zend = prog.has_zend;
        nfa_has_backref = prog.has_backref;
        nfa_nsubexpr = prog.nsubexp;
        nfa_listid = 1;
        nfa_alt_listid = 2;
        nfa_regengine.expr = prog.pattern;

        if (prog.reganch != 0 && 0 < col[0])
            return 0L;

        need_clear_subexpr = true;
        /* Clear the external match subpointers if necessary. */
        if (prog.reghasz == REX_SET)
        {
            nfa_has_zsubexpr = true;
            need_clear_zsubexpr = true;
        }
        else
            nfa_has_zsubexpr = false;

        if (prog.regstart != NUL)
        {
            /* Skip ahead until a character we know the match must start with.
             * When there is none there is no match. */
            if (skip_to_start(prog.regstart, col) == false)
                return 0L;

            /* If match_text is set, it contains the full text that must match.
             * Nothing else to try.  Doesn't handle combining chars well. */
            if (prog.match_text != null && !ireg_icombine)
                return find_match_text(col[0], prog.regstart, prog.match_text);
        }

        /* If the start column is past the maximum column: no need to try. */
        if (0 < ireg_maxcol && ireg_maxcol <= col[0])
            return 0L;

        for (int i = 0; i < prog.nstate; i++)
        {
            nfa_state_C state = prog.states[i];
            if (state == null)
                continue;

            state.id = i;
            state.lastlist[0] = 0;
            state.lastlist[1] = 0;
        }

        long retval = nfa_regtry(prog, col[0], tm);

        nfa_regengine.expr = null;

        return retval;
    }

    /*
     * Compile a regular expression into internal code for the NFA matcher.
     * Returns the program in allocated space.  Returns null for an error.
     */
    /*private*/ static regprog_C nfa_regcomp(Bytes expr, int re_flags)
    {
        if (expr == null)
            return null;

        nfa_regprog_C prog;

        nfa_regengine.expr = expr;
        nfa_re_flags = re_flags;

        init_class_tab();

        nfa_regcomp_start(expr, re_flags);

        theend:
        {
            fail:
            {
                /* Build postfix form of the regexp.  Needed to build the NFA (and count its size). */
                int[] postfix = re2post();
                if (postfix == null)
                {
                    /* TODO: only give this error for debugging? */
                    if (post_array.length <= post_index)
                        emsgn(u8("Internal error: estimated max number of states insufficient: %d"), post_array.length);
                    break fail;          /* cascaded (syntax?) error */
                }

                /*
                 * In order to build the NFA, we parse the input regexp twice:
                 * 1. first pass to count size (so we can allocate space)
                 * 2. second to emit code
                 */

                prog = new nfa_regprog_C();
                prog.nstate = 0;

                /*
                 * PASS 1
                 * Count number of NFA states in "prog.nstate".  Do not build the NFA.
                 */
                post2nfa(postfix, post_index, prog, true);

                /* allocate space for the compiled regexp */
                nfa_states = prog.states = new nfa_state_C[prog.nstate];
                prog.istate = 0;

                /*
                 * PASS 2
                 * Build the NFA
                 */
                prog.start = post2nfa(postfix, post_index, prog, false);
                if (prog.start == null)
                    break fail;

                prog.regflags = regflags;
                prog.engine = nfa_regengine;
                prog.has_zend = nfa_has_zend;
                prog.has_backref = nfa_has_backref;
                prog.nsubexp = regnpar;

                nfa_postprocess(prog);

                prog.reganch = nfa_get_reganch(prog.start, 0) ? 1 : 0;
                prog.regstart = nfa_get_regstart(prog.start, 0);
                prog.match_text = nfa_get_match_text(prog.start);

                /* Remember whether this pattern has any \z specials in it. */
                prog.reghasz = re_has_z;
                prog.pattern = STRDUP(expr);
                nfa_regengine.expr = null;
                break theend;
            }

            prog = null;
            nfa_regengine.expr = null;
        }

        post_array = null;
        post_index = 0;
        nfa_states = null;

        return (regprog_C)prog;
    }

    /*
     * Match a regexp against a string.
     * "rmp.regprog" is a compiled regexp as returned by nfa_regcomp().
     * Uses curbuf for line count and 'iskeyword'.
     * If "line_lbr" is true, consider a "\n" in "line" to be a line break.
     *
     * Returns <= 0 for failure, number of lines contained in the match otherwise.
     */
    /*private*/ static long nfa_regexec_nl(regmatch_C rmp, Bytes line, int col, boolean line_lbr)
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

        return nfa_regexec_both(line, col, null);
    }

    /*
     * Match a regexp against multiple lines.
     * "rmp.regprog" is a compiled regexp as returned by vim_regcomp().
     * Uses curbuf for line count and 'iskeyword'.
     *
     * Return <= 0 if there is no match.  Return number of lines contained in the match otherwise.
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
    /*private*/ static long nfa_regexec_multi(regmmatch_C rmp, window_C win, buffer_C buf, long lnum, int col, timeval_C tm)
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

        return nfa_regexec_both(null, col, tm);
    }

    /* ----------------------------------------------------------------------- */

    /* Which regexp engine to use?  Needed for vim_regcomp().
     * Must match with 'regexpengine'. */
    /*private*/ static int regexp_engine;

    /*
     * Compile a regular expression into internal code.
     * Returns the program in allocated memory.
     * Returns null for an error.
     */
    /*private*/ static regprog_C vim_regcomp(Bytes expr_arg, int re_flags)
    {
        regprog_C prog = null;
        Bytes expr = expr_arg;

        regexp_engine = (int)p_re[0];

        /* Check for prefix "\%#=", that sets the regexp engine. */
        if (STRNCMP(expr, u8("\\%#="), 4) == 0)
        {
            int newengine = expr.at(4) - '0';

            if (newengine == AUTOMATIC_ENGINE || newengine == BACKTRACKING_ENGINE || newengine == NFA_ENGINE)
            {
                regexp_engine = expr.at(4) - '0';
                expr = expr.plus(5);
            }
            else
            {
                emsg(u8("E864: \\%#= can only be followed by 0, 1, or 2. The automatic engine will be used."));
                regexp_engine = AUTOMATIC_ENGINE;
            }
        }

        bt_regengine.expr = expr;
        nfa_regengine.expr = expr;

        /*
         * First try the NFA engine, unless backtracking was requested.
         */
        if (regexp_engine != BACKTRACKING_ENGINE)
            prog = nfa_regengine.regcomp(expr, re_flags + (regexp_engine == AUTOMATIC_ENGINE ? RE_AUTO : 0));
        else
            prog = bt_regengine.regcomp(expr, re_flags);

        /* Check for error compiling regexp with initial engine. */
        if (prog == null)
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

        if (prog != null)
        {
            /* Store the info needed to call regcomp() again when
             * the engine turns out to be very slow executing it. */
            prog.re_engine = regexp_engine;
            prog.re_flags  = re_flags;
        }

        return prog;
    }

    /*private*/ static void report_re_switch(Bytes pat)
    {
        if (0 < p_verbose[0])
        {
            verbose_enter();
            msg_puts(u8("Switching to backtracking RE engine for pattern: "));
            msg_puts(pat);
            verbose_leave();
        }
    }

    /*
     * Match a regexp against a string.
     * "rmp.regprog" is a compiled regexp as returned by vim_regcomp().
     * Note: "rmp.regprog" may be freed and changed.
     * Uses curbuf for line count and 'iskeyword'.
     * When "nl" is true, consider a "\n" in "line" to be a line break.
     *
     * Return true if there is a match, false if not.
     */
    /*private*/ static boolean vim_regexec_both(regmatch_C rmp, Bytes line, int col, boolean nl)
        /* line: string to match against */
        /* col: column to start looking for match */
    {
        long result = rmp.regprog.engine.regexec_nl(rmp, line, col, nl);

        /* NFA engine aborted because it's very slow. */
        if (rmp.regprog.re_engine == AUTOMATIC_ENGINE && result == NFA_TOO_EXPENSIVE)
        {
            long save_p_re = p_re[0];
            int re_flags = rmp.regprog.re_flags;
            Bytes pat = STRDUP(((nfa_regprog_C)rmp.regprog).pattern);

            p_re[0] = BACKTRACKING_ENGINE;
            rmp.regprog = null;
            if (pat != null)
            {
                report_re_switch(pat);
                rmp.regprog = vim_regcomp(pat, re_flags);
                if (rmp.regprog != null)
                    result = rmp.regprog.engine.regexec_nl(rmp, line, col, nl);
            }
            p_re[0] = save_p_re;
        }

        return (0 < result);
    }

    /*
     * Note: "*prog" may be freed and changed.
     * Return true if there is a match, false if not.
     */
    /*private*/ static boolean vim_regexec_prog(regprog_C[] prog, boolean ignore_case, Bytes line, int col)
    {
        regmatch_C regmatch = new regmatch_C();
        regmatch.regprog = prog[0];
        regmatch.rm_ic = ignore_case;

        boolean r = vim_regexec_both(regmatch, line, col, false);

        prog[0] = regmatch.regprog;
        return r;
    }

    /*
     * Note: "rmp.regprog" may be freed and changed.
     * Return true if there is a match, false if not.
     */
    /*private*/ static boolean vim_regexec(regmatch_C rmp, Bytes line, int col)
    {
        return vim_regexec_both(rmp, line, col, false);
    }

    /*
     * Like vim_regexec(), but consider a "\n" in "line" to be a line break.
     * Note: "rmp.regprog" may be freed and changed.
     * Return true if there is a match, false if not.
     */
    /*private*/ static boolean vim_regexec_nl(regmatch_C rmp, Bytes line, int col)
    {
        return vim_regexec_both(rmp, line, col, true);
    }

    /*
     * Match a regexp against multiple lines.
     * "rmp.regprog" is a compiled regexp as returned by vim_regcomp().
     * Note: "rmp.regprog" may be freed and changed.
     * Uses curbuf for line count and 'iskeyword'.
     *
     * Return zero if there is no match.  Return number of lines contained in the match otherwise.
     */
    /*private*/ static long vim_regexec_multi(regmmatch_C rmp, window_C win, buffer_C buf, long lnum, int col, timeval_C tm)
        /* win: window in which to search or null */
        /* buf: buffer in which to search */
        /* lnum: nr of line to start looking for match */
        /* col: column to start looking for match */
        /* tm: timeout limit or null */
    {
        long result = rmp.regprog.engine.regexec_multi(rmp, win, buf, lnum, col, tm);

        /* NFA engine aborted because it's very slow. */
        if (rmp.regprog.re_engine == AUTOMATIC_ENGINE && result == NFA_TOO_EXPENSIVE)
        {
            long save_p_re = p_re[0];
            int re_flags = rmp.regprog.re_flags;
            Bytes pat = STRDUP(((nfa_regprog_C)rmp.regprog).pattern);

            p_re[0] = BACKTRACKING_ENGINE;
            rmp.regprog = null;
            if (pat != null)
            {
                report_re_switch(pat);
                rmp.regprog = vim_regcomp(pat, re_flags);
                if (rmp.regprog != null)
                    result = rmp.regprog.engine.regexec_multi(rmp, win, buf, lnum, col, tm);
            }
            p_re[0] = save_p_re;
        }

        return Math.max(0, result);
    }
}
