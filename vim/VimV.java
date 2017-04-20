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
public class VimV
{
    /*
     * term.c: functions for controlling the terminal -------------------------------------------------
     *
     * NOTE: padding and variable substitution is not performed.
     */

    /*
     * Here are the builtin termcap entries.  They are not stored as complete
     * structures with all entries, as such a structure is too big.
     *
     * The entries are compact, therefore they normally are included even when
     * HAVE_TGETENT is defined.  When HAVE_TGETENT is defined, the builtin entries
     * can be accessed with "builtin_ansi", etc.
     *
     * Each termcap is a list of btcap_C structures.  It always starts with KS_NAME,
     * which separates the entries.  See parse_builtin_tcap() for all details.
     * 'bt_key' is either a KS_xxx code (>= 0), or a K_xxx code.
     *
     * Entries marked with "guessed" may be wrong.
     */
    /*private*/ static final class btcap_C
    {
        int     bt_key;
        Bytes bt_seq;

        /*private*/ btcap_C(int key, Bytes seq)
        {
            bt_key = key;
            bt_seq = seq;
        }
    }

    /* start of keys that are not directly used by Vim but can be mapped */
    /*private*/ static final int BT_EXTRA_KEYS   = 0x101;

    /* Request Terminal Version status: */
    /*private*/ static final int CRV_GET       = 1;         /* send T_CRV when switched to RAW mode */
    /*private*/ static final int CRV_SENT      = 2;         /* did send T_CRV, waiting for answer */
    /*private*/ static final int CRV_GOT       = 3;         /* received T_CRV response */
    /*private*/ static int crv_status = CRV_GET;

    /* Request Cursor position report: */
    /*private*/ static final int U7_GET        = 1;         /* send T_U7 when switched to RAW mode */
    /*private*/ static final int U7_SENT       = 2;         /* did send T_U7, waiting for answer */
    /*private*/ static final int U7_GOT        = 3;         /* received T_U7 response */
    /*private*/ static int u7_status = U7_GET;

    /*private*/ static boolean detected_8bit;   /* detected 8-bit terminal */

    /*private*/ static final btcap_C tcap(int key, Bytes seq)
    {
        return new btcap_C(key, seq);
    }

    /*private*/ static btcap_C[] builtin_termcaps =
    {
        /*
         * standard ANSI terminal, default for unix
         */
        tcap(KS_NAME,       u8("ansi")                 ),
        tcap(KS_CE,         u8("\033[K")               ),
        tcap(KS_AL,         u8("\033[L")               ),
        tcap(KS_CAL,        u8("\033[%p1%dL")          ),
        tcap(KS_DL,         u8("\033[M")               ),
        tcap(KS_CDL,        u8("\033[%p1%dM")          ),
        tcap(KS_CL,         u8("\033[H\033[2J")        ),
        tcap(KS_ME,         u8("\033[0m")              ),
        tcap(KS_MR,         u8("\033[7m")              ),
        tcap(KS_MS,         u8("y")                    ),
        tcap(KS_UT,         u8("y")                    ),      /* guessed */
        tcap(KS_LE,         u8("\b")                   ),
        tcap(KS_CM,         u8("\033[%i%p1%d;%p2%dH")  ),
        tcap(KS_CRI,        u8("\033[%p1%dC")          ),

        /*
         */
        tcap(KS_NAME,       u8("xterm")                ),
        tcap(KS_CE,         u8("\033[K")               ),
        tcap(KS_AL,         u8("\033[L")               ),
        tcap(KS_CAL,        u8("\033[%p1%dL")          ),
        tcap(KS_DL,         u8("\033[M")               ),
        tcap(KS_CDL,        u8("\033[%p1%dM")          ),
        tcap(KS_CS,         u8("\033[%i%p1%d;%p2%dr")  ),
        tcap(KS_CL,         u8("\033[H\033[2J")        ),
        tcap(KS_CD,         u8("\033[J")               ),
        tcap(KS_ME,         u8("\033[m")               ),
        tcap(KS_MR,         u8("\033[7m")              ),
        tcap(KS_MD,         u8("\033[1m")              ),
        tcap(KS_UE,         u8("\033[m")               ),
        tcap(KS_US,         u8("\033[4m")              ),
        tcap(KS_MS,         u8("y")                    ),
        tcap(KS_UT,         u8("y")                    ),
        tcap(KS_LE,         u8("\b")                   ),
        tcap(KS_CM,         u8("\033[%i%p1%d;%p2%dH")  ),
        tcap(KS_SR,         u8("\033M")                ),
        tcap(KS_CRI,        u8("\033[%p1%dC")          ),
        tcap(KS_KS,         u8("\033[?1h\033=")        ),
        tcap(KS_KE,         u8("\033[?1l\033>")        ),
        tcap(KS_TI,         u8("\0337\033[?47h")       ),
        tcap(KS_TE,         u8("\033[2J\033[?47l\0338")),
        tcap(KS_CIS,        u8("\033]1;")              ),
        tcap(KS_CIE,        u8("\007")                 ),
        tcap(KS_TS,         u8("\033]2;")              ),
        tcap(KS_FS,         u8("\007")                 ),
        tcap(KS_CWS,        u8("\033[8;%p1%d;%p2%dt")  ),
        tcap(KS_CWP,        u8("\033[3;%p1%d;%p2%dt")  ),
        tcap(KS_CRV,        u8("\033[>c")              ),
        tcap(KS_U7,         u8("\033[6n")              ),

        tcap(K_UP,          u8("\033O*A")              ),
        tcap(K_DOWN,        u8("\033O*B")              ),
        tcap(K_RIGHT,       u8("\033O*C")              ),
        tcap(K_LEFT,        u8("\033O*D")              ),
        /* An extra set of cursor keys for vt100 mode. */
        tcap(K_XUP,         u8("\033[1;*A")            ),
        tcap(K_XDOWN,       u8("\033[1;*B")            ),
        tcap(K_XRIGHT,      u8("\033[1;*C")            ),
        tcap(K_XLEFT,       u8("\033[1;*D")            ),
        /* An extra set of function keys for vt100 mode. */
        tcap(K_XF1,         u8("\033O*P")              ),
        tcap(K_XF2,         u8("\033O*Q")              ),
        tcap(K_XF3,         u8("\033O*R")              ),
        tcap(K_XF4,         u8("\033O*S")              ),
        tcap(K_F1,          u8("\033[11;*~")           ),
        tcap(K_F2,          u8("\033[12;*~")           ),
        tcap(K_F3,          u8("\033[13;*~")           ),
        tcap(K_F4,          u8("\033[14;*~")           ),
        tcap(K_F5,          u8("\033[15;*~")           ),
        tcap(K_F6,          u8("\033[17;*~")           ),
        tcap(K_F7,          u8("\033[18;*~")           ),
        tcap(K_F8,          u8("\033[19;*~")           ),
        tcap(K_F9,          u8("\033[20;*~")           ),
        tcap(K_F10,         u8("\033[21;*~")           ),
        tcap(K_F11,         u8("\033[23;*~")           ),
        tcap(K_F12,         u8("\033[24;*~")           ),
        tcap(K_S_TAB,       u8("\033[Z")               ),
        tcap(K_HELP,        u8("\033[28;*~")           ),
        tcap(K_UNDO,        u8("\033[26;*~")           ),
        tcap(K_INS,         u8("\033[2;*~")            ),
        tcap(K_HOME,        u8("\033[1;*H")            ),
     /* tcap(K_S_HOME,      u8("\033O2H")              ), */
     /* tcap(K_C_HOME,      u8("\033O5H")              ), */
        tcap(K_KHOME,       u8("\033[1;*~")            ),
        tcap(K_XHOME,       u8("\033O*H")              ),      /* other Home */
        tcap(K_ZHOME,       u8("\033[7;*~")            ),      /* other Home */
        tcap(K_END,         u8("\033[1;*F")            ),
     /* tcap(K_S_END,       u8("\033O2F")              ), */
     /* tcap(K_C_END,       u8("\033O5F")              ), */
        tcap(K_KEND,        u8("\033[4;*~")            ),
        tcap(K_XEND,        u8("\033O*F")              ),      /* other End */
        tcap(K_ZEND,        u8("\033[8;*~")            ),
        tcap(K_PAGEUP,      u8("\033[5;*~")            ),
        tcap(K_PAGEDOWN,    u8("\033[6;*~")            ),
        tcap(K_KPLUS,       u8("\033O*k")              ),      /* keypad plus */
        tcap(K_KMINUS,      u8("\033O*m")              ),      /* keypad minus */
        tcap(K_KDIVIDE,     u8("\033O*o")              ),      /* keypad / */
        tcap(K_KMULTIPLY,   u8("\033O*j")              ),      /* keypad * */
        tcap(K_KENTER,      u8("\033O*M")              ),      /* keypad Enter */
        tcap(K_KPOINT,      u8("\033O*n")              ),      /* keypad . */
        tcap(K_KDEL,        u8("\033[3;*~")            ),      /* keypad Del */

        tcap(BT_EXTRA_KEYS, u8("")                     ),

        tcap(K_F0,          u8("\033[10;*~")           ),
        tcap(K_F13,         u8("\033[25;*~")           ),
        /* F14 and F15 are missing, because they send the same codes as the undo and help key,
         * although they don't work on all keyboards. */
        tcap(K_F16,         u8("\033[29;*~")           ),
        tcap(K_F17,         u8("\033[31;*~")           ),
        tcap(K_F18,         u8("\033[32;*~")           ),
        tcap(K_F19,         u8("\033[33;*~")           ),
        tcap(K_F20,         u8("\033[34;*~")           ),

        tcap(K_F21,         u8("\033[42;*~")           ),
        tcap(K_F22,         u8("\033[43;*~")           ),
        tcap(K_F23,         u8("\033[44;*~")           ),
        tcap(K_F24,         u8("\033[45;*~")           ),
        tcap(K_F25,         u8("\033[46;*~")           ),
        tcap(K_F26,         u8("\033[47;*~")           ),
        tcap(K_F27,         u8("\033[48;*~")           ),
        tcap(K_F28,         u8("\033[49;*~")           ),
        tcap(K_F29,         u8("\033[50;*~")           ),
        tcap(K_F30,         u8("\033[51;*~")           ),

        tcap(K_F31,         u8("\033[52;*~")           ),
        tcap(K_F32,         u8("\033[53;*~")           ),
        tcap(K_F33,         u8("\033[54;*~")           ),
        tcap(K_F34,         u8("\033[55;*~")           ),
        tcap(K_F35,         u8("\033[56;*~")           ),
        tcap(K_F36,         u8("\033[57;*~")           ),
        tcap(K_F37,         u8("\033[58;*~")           ),

        /*
         * The most minimal terminal: only clear screen and cursor positioning; always included.
         */
        tcap(KS_NAME,       u8("dumb")                 ),
        tcap(KS_CL,         u8("\014")                 ),
        tcap(KS_CM,         u8("\033[%i%p1%d;%p2%dH")  ),

        /*
         * end marker
         */
        tcap(KS_NAME,       null                       )
    };

    /*
     * DEFAULT_TERM is used, when no terminal is specified with -T option or $TERM.
     */
    /*private*/ static final Bytes DEFAULT_TERM = u8("ansi");

    /*private*/ static boolean need_gather;                     /* need to fill termleader[] */
    /*private*/ static Bytes termleader = new Bytes(256 + 1);   /* for check_termcode() */
    /*private*/ static boolean check_for_codes;                 /* check for key code response */

    /*private*/ static int find_builtin_term(Bytes term)
    {
        if (vim_is_xterm(term))
            term = u8("xterm");

        btcap_C[] bts = builtin_termcaps;
        for (int i = 0; bts[i].bt_seq != null; i++)
            if (bts[i].bt_key == KS_NAME && STRCMP(bts[i].bt_seq, term) == 0)
                return i;

        return -1;
    }

    /*
     * Parsing of the builtin termcap entries.
     * Caller should check if 'name' is a valid builtin term.
     * The terminal's name is not set, as this is already done in termcapinit().
     */
    /*private*/ static void parse_builtin_tcap(Bytes term)
    {
        btcap_C[] bts = builtin_termcaps;
        int i = find_builtin_term(term);
        if (i < 0)
            return;

        boolean term_8bit = term_is_8bit(term);

        for (++i; bts[i].bt_key != KS_NAME && bts[i].bt_key != BT_EXTRA_KEYS; i++)
        {
            if (bts[i].bt_key < 0)
            {
                Bytes name = new Bytes(2);
                name.be(0, KEY2TERMCAP0(bts[i].bt_key));
                name.be(1, KEY2TERMCAP1(bts[i].bt_key));

                if (find_termcode(name) == null)
                    add_termcode(name, bts[i].bt_seq, term_8bit ? TRUE : FALSE);
            }
            else /* KS_xx entry */
            {
                /* Only set the value if it wasn't set yet. */
                if (term_strings[bts[i].bt_key][0] == null || term_strings[bts[i].bt_key][0] == EMPTY_OPTION)
                {
                    /* 8bit terminal: use CSI instead of <Esc>[ */
                    if (term_8bit && term_7to8bit(bts[i].bt_seq) != NUL)
                    {
                        Bytes s = STRDUP(bts[i].bt_seq);
                        for (Bytes t = s; t.at(0) != NUL; t = t.plus(1))
                        {
                            byte b = term_7to8bit(t);
                            if (b != NUL)
                            {
                                t.be(0, b);
                                STRCPY(t.plus(1), t.plus(2));
                            }
                        }
                        term_strings[bts[i].bt_key][0] = s;
                    }
                    else
                        term_strings[bts[i].bt_key][0] = bts[i].bt_seq;
                }
            }
        }
    }

    /*
     * Set number of colors.
     * Store it as a number in t_colors.
     * Store it as a string in T_CCO (using nr_colors[]).
     */
    /*private*/ static void set_color_count(int nr)
    {
        t_colors = nr;

        Bytes nr_colors = new Bytes(20);
        if (1 < t_colors)
            libC.sprintf(nr_colors, u8("%d"), t_colors);
        else
            nr_colors.be(0, NUL);

        set_string_option_direct(u8("t_Co"), -1, nr_colors, OPT_FREE, 0);
    }

    /*private*/ static Bytes[] key_names =
    {
        /* Do this one first, it may cause a screen redraw. */
        u8("Co"),
        u8("ku"), u8("kd"), u8("kr"), u8("kl"),
        u8("#2"), u8("#4"), u8("%i"), u8("*7"),
        u8("k1"), u8("k2"), u8("k3"), u8("k4"), u8("k5"), u8("k6"),
        u8("k7"), u8("k8"), u8("k9"), u8("k;"), u8("F1"), u8("F2"),
        u8("%1"), u8("&8"), u8("kb"), u8("kI"), u8("kD"), u8("kh"),
        u8("@7"), u8("kP"), u8("kN"), u8("K1"), u8("K3"), u8("K4"), u8("K5"), u8("kB"),
        null
    };

    /*private*/ static final class tcname_C
    {
        int dest;           /* index in term_strings[] */
        Bytes name;        /* termcap name for string */

        /*private*/ tcname_C(int dest, Bytes name)
        {
            this.dest = dest;
            this.name = name;
        }
    }

    /*private*/ static final tcname_C tcname(int dest, Bytes name)
    {
        return new tcname_C(dest, name);
    }

    /*private*/ static tcname_C[] tcap_names =
    {
        tcname(KS_CE,  u8("ce")), tcname(KS_AL,  u8("al")), tcname(KS_CAL, u8("AL")),
        tcname(KS_DL,  u8("dl")), tcname(KS_CDL, u8("DL")), tcname(KS_CS,  u8("cs")),
        tcname(KS_CL,  u8("cl")), tcname(KS_CD,  u8("cd")),
        tcname(KS_VI,  u8("vi")), tcname(KS_VE,  u8("ve")), tcname(KS_MB,  u8("mb")),
        tcname(KS_VS,  u8("vs")), tcname(KS_ME,  u8("me")), tcname(KS_MR,  u8("mr")),
        tcname(KS_MD,  u8("md")), tcname(KS_SE,  u8("se")), tcname(KS_SO,  u8("so")),
        tcname(KS_CZH, u8("ZH")), tcname(KS_CZR, u8("ZR")), tcname(KS_UE,  u8("ue")),
        tcname(KS_US,  u8("us")), tcname(KS_UCE, u8("Ce")), tcname(KS_UCS, u8("Cs")),
        tcname(KS_CM,  u8("cm")), tcname(KS_SR,  u8("sr")),
        tcname(KS_CRI, u8("RI")), tcname(KS_VB,  u8("vb")), tcname(KS_KS,  u8("ks")),
        tcname(KS_KE,  u8("ke")), tcname(KS_TI,  u8("ti")), tcname(KS_TE,  u8("te")),
        tcname(KS_BC,  u8("bc")), tcname(KS_CSB, u8("Sb")), tcname(KS_CSF, u8("Sf")),
        tcname(KS_CAB, u8("AB")), tcname(KS_CAF, u8("AF")), tcname(KS_LE,  u8("le")),
        tcname(KS_ND,  u8("nd")), tcname(KS_OP,  u8("op")), tcname(KS_CRV, u8("RV")),
        tcname(KS_CIS, u8("IS")), tcname(KS_CIE, u8("IE")),
        tcname(KS_TS,  u8("ts")), tcname(KS_FS,  u8("fs")),
        tcname(KS_CWP, u8("WP")), tcname(KS_CWS, u8("WS")),
        tcname(KS_CSI, u8("SI")), tcname(KS_CEI, u8("EI")),
        tcname(KS_U7,  u8("u7"))
    };

    /*
     * Set terminal options for terminal "term".
     * Return true if terminal 'term' was found in a termcap, false otherwise.
     *
     * While doing this, until ttest(), some options may be null, be careful.
     */
    /*private*/ static boolean set_termname(Bytes term)
    {
        /* In silent mode (ex -s) we don't use the 'term' option. */
        if (silent_mode)
            return true;

        detected_8bit = false;                  /* reset 8-bit detection */

        if (STRNCMP(term, u8("builtin_"), 8) == 0)
            term = term.plus(8);

        /*
         * If HAVE_TGETENT is not defined, only the builtin termcap is used.
         */
        {
            if (find_builtin_term(term) < 0)
            {
                libC.fprintf(stderr, u8("\r\n"));
                libC.fprintf(stderr, u8("'%s' not known. Available builtin terminals are:\r\n"), term);
                btcap_C[] bts = builtin_termcaps;
                for (int i = 0; bts[i].bt_seq != null; i++)
                {
                    if (bts[i].bt_key == KS_NAME)
                        libC.fprintf(stderr, u8("    builtin_%s\r\n"), bts[i].bt_seq);
                }
                /* when user typed :set term=xxx, quit here */
                if (starting != NO_SCREEN)
                {
                    screen_start();         /* don't know where cursor is now */
                    wait_return(TRUE);
                    return false;
                }
                term = DEFAULT_TERM;
                libC.fprintf(stderr, u8("defaulting to '%s'\r\n"), term);
                if (emsg_silent == 0)
                {
                    screen_start();         /* don't know where cursor is now */
                    out_flush();
                    ui_delay(2000L, true);
                }
                set_string_option_direct(u8("term"), -1, term, OPT_FREE, 0);
                libc.fflush(stderr);
            }
            out_flush();
            clear_termoptions();            /* clear old options */
            parse_builtin_tcap(term);
        }

        /*
         * special: There is no info in the termcap about whether the cursor positioning
         * is relative to the start of the screen or to the start of the scrolling region.
         * We just guess here.  Only msdos pcterm is known to do it relative.
         */
        if (STRCMP(term, u8("pcterm")) == 0)
            T_CCS[0] = u8("yes");
        else
            T_CCS[0] = EMPTY_OPTION;

        /*
         * Any "stty" settings override the default for t_kb from the termcap.
         * Don't do this when the GUI is active, it uses "t_kb" and "t_kD" directly.
         */
        get_stty();

        /*
         * If the termcap has no entry for 'bs' and/or 'del' and the ioctl()
         * also didn't work, use the default CTRL-H.
         * The default for t_kD is DEL, unless t_kb is DEL.
         * The vim_strsave'd strings are probably lost forever, well it's only two bytes.
         * Don't do this when the GUI is active, it uses "t_kb" and "t_kD" directly.
         */
        Bytes bs_p = find_termcode(u8("kb"));
        if (bs_p == null || bs_p.at(0) == NUL)
            add_termcode(u8("kb"), (bs_p = CTRL_H_STR), FALSE);
        Bytes del_p = find_termcode(u8("kD"));
        if ((del_p == null || del_p.at(0) == NUL) && (bs_p == null || bs_p.at(0) != DEL))
            add_termcode(u8("kD"), DEL_STR, FALSE);

        term_is_xterm = vim_is_xterm(term);

        /*
         * For Unix, set the 'ttymouse' option to the type of mouse to be used.
         * The termcode for the mouse is added as a side effect in option.c.
         */
        {
            clip_init(false);

            Bytes ttym;
            if (use_xterm_like_mouse(term))
            {
                if (use_xterm_mouse() != 0)
                    ttym = null;        /* keep existing value, might be "xterm2" */
                else
                    ttym = u8("xterm");
            }
            else
                ttym = u8("");

            if (ttym != null)
            {
                set_option_value(u8("ttym"), 0L, ttym, 0);
                /* Reset the WAS_SET flag, 'ttymouse' can be set to "xterm2" in check_termcode(). */
                reset_option_was_set(u8("ttym"));
            }
            else
                check_mouse_termcode();     /* set mouse termcode anyway */
        }

        /*
         * 'ttyfast' is default on for xterm and a few others.
         */
        if (vim_is_fastterm(term))
            p_tf[0] = true;

        ttest(true);                /* make sure we have a valid set of terminal codes */

        full_screen = true;             /* we can use termcap codes from now on */
        set_term_defaults();            /* use current values as defaults */
        crv_status = CRV_GET;           /* get terminal version later */

        /*
         * Initialize the terminal with the appropriate termcap codes.
         * Set the mouse and window title if possible.
         * Don't do this when starting, need to parse the .vimrc first,
         * because it may redefine t_TI etc.
         */
        if (starting != NO_SCREEN)
        {
            starttermcap();             /* may change terminal mode */
            setmouse();                 /* may start using the mouse */
        }

        int width = 80, height = 24;    /* most terminals are 24 lines */

        set_shellsize(width, height, false);    /* may change Rows */

        if (starting != NO_SCREEN)
        {
            if (scroll_region)
                scroll_region_reset();          /* in case Rows changed */

            check_map_keycodes();       /* check mappings for terminal codes used */

            /*
             * Execute the TermChanged autocommands for each buffer that is loaded.
             */
            buffer_C oldbuf = curbuf;
            for (curbuf = firstbuf; curbuf != null; curbuf = curbuf.b_next)
            {
                if (curbuf.b_ml.ml_mfp != null)
                    apply_autocmds(EVENT_TERMCHANGED, null, null, false, curbuf);
            }
            if (buf_valid(oldbuf))
                curbuf = oldbuf;
        }

        may_req_termresponse();

        return true;
    }

    /*private*/ static boolean has_mouse_termcode;

    /*private*/ static void set_mouse_termcode(byte n, Bytes s)
        /* n: KS_MOUSE */
    {
        Bytes name = new Bytes(2);

        name.be(0, n);
        name.be(1, KE_FILLER);
        add_termcode(name, s, FALSE);
        has_mouse_termcode = true;
    }

    /*private*/ static void del_mouse_termcode(byte n)
        /* n: KS_MOUSE */
    {
        Bytes name = new Bytes(2);

        name.be(0, n);
        name.be(1, KE_FILLER);
        del_termcode(name);
        has_mouse_termcode = false;
    }

    /*
     * Get a string entry from the termcap and add it to the list of termcodes.
     * Used for <t_xx> special keys.
     * Give an error message for failure when not sourcing.
     * If force given, replace an existing entry.
     * Return false if the entry was not found, true if the entry was added.
     */
    /*private*/ static boolean add_termcap_entry(Bytes name, boolean force)
    {
        if (!force && find_termcode(name) != null)      /* it's already there */
            return true;

        Bytes term = T_NAME[0];
        if (term == null || term.at(0) == NUL)               /* 'term' not defined yet */
            return false;

        if (STRNCMP(term, u8("builtin_"), 8) == 0)
            term = term.plus(8);

        /*
         * Search in builtin termcap.
         */
        btcap_C[] bts = builtin_termcaps;
        int i = find_builtin_term(term);
        if (0 <= i)
        {
            int key = TERMCAP2KEY(name.at(0), name.at(1));

            for (++i; bts[i].bt_key != KS_NAME; i++)
                if (bts[i].bt_key == key)
                {
                    add_termcode(name, bts[i].bt_seq, term_is_8bit(term) ? TRUE : FALSE);
                    return true;
                }
        }

        if (sourcing_name == null)
            emsg2(u8("E436: No \"%s\" entry in termcap"), name);

        return false;
    }

    /*
     * Return true if terminal "name" uses CSI instead of <Esc>[.
     * Assume that the terminal is using 8-bit controls when the name contains "8bit", like in "xterm-8bit".
     */
    /*private*/ static boolean term_is_8bit(Bytes name)
    {
        return (detected_8bit || STRSTR(name, u8("8bit")) != null);
    }

    /*
     * Translate terminal control chars from 7-bit to 8-bit:
     * <Esc>[ -> CSI
     * <Esc>] -> <M-C-]>
     * <Esc>O -> <M-C-O>
     */
    /*private*/ static byte term_7to8bit(Bytes p)
    {
        if (p.at(0) == ESC)
            switch (p.at(1))
            {
                case '[': return CSI;
                case ']': return (byte)0x9d;
                case 'O': return (byte)0x8f;
            }
        return NUL;
    }

    /*
     * Set the terminal name and initialize the terminal options.
     * If "name" is null or empty, get the terminal name from the environment.
     * If that fails, use the default terminal name.
     */
    /*private*/ static void termcapinit(Bytes name)
    {
        if (name != null && name.at(0) == NUL)
            name = null;        /* empty name is equal to no name */

        Bytes term = name;
        if (term == null)
            term = libC.getenv(u8("TERM"));
        if (term == null || term.at(0) == NUL)
            term = DEFAULT_TERM;

        set_string_option_direct(u8("term"), -1, term, OPT_FREE, 0);

        /* Set the default terminal name. */
        set_string_default(u8("term"), term);
        set_string_default(u8("ttytype"), term);

        /*
         * Avoid using "term" here, because the next mch_getenv() may overwrite it.
         */
        set_termname(T_NAME[0] != null ? T_NAME[0] : term);
    }

    /*
     * the number of calls to ui_write is reduced by using the buffer "out_buf"
     */
    /*private*/ static final int OUT_SIZE      = 2047;

    /*private*/ static Bytes out_buf = new Bytes(OUT_SIZE + 1);
    /*private*/ static int out_pos;         /* number of chars in "out_buf" */

    /*
     * out_flush(): flush the output buffer
     */
    /*private*/ static void out_flush()
    {
        if (out_pos != 0)
        {
            /* set out_pos to 0 before ui_write, to avoid recursiveness */
            int len = out_pos;
            out_pos = 0;
            ui_write(out_buf, len);
        }
    }

    /*
     * Sometimes a byte out of a multi-byte character is written with out_char().
     * To avoid flushing half of the character, call this function first.
     */
    /*private*/ static void out_flush_check()
    {
    }

    /*
     * out_char(c): put a byte into the output buffer.
     *              Flush it if it becomes full.
     * This should not be used for outputting text on the screen
     * (use functions like msg_puts() and screen_putchar() for that).
     */
    /*private*/ static void out_char(byte c)
    {
        if (c == '\n')      /* turn LF into CR-LF (CRMOD doesn't seem to do this) */
            out_char((byte)'\r');

        out_buf.be(out_pos++, c);

        /* For testing we flush each time. */
        if (OUT_SIZE <= out_pos || p_wd[0] != 0)
            out_flush();
    }

    /*
     * out_char_nf(c): like out_char(), but don't flush when "p_wd" is set
     */
    /*private*/ static void out_char_nf(byte c)
    {
        if (c == '\n')      /* turn LF into CR-LF (CRMOD doesn't seem to do this) */
            out_char_nf((byte)'\r');

        out_buf.be(out_pos++, c);

        if (OUT_SIZE <= out_pos)
            out_flush();
    }

    /*private*/ static Bytes _addfmt(Bytes buf, Bytes fmt, int val)
    {
        libC.sprintf(buf, fmt, val);
        while (buf.at(0) != NUL)
            buf = buf.plus(1);
        return buf;
    }

    /*private*/ static Bytes tgoto_UP, tgoto_BC;
    /*private*/ static Bytes tgoto_buffer = new Bytes(32);

    /*
     * Decode cm cursor motion string.
     * cm is cursor motion string, row and col are the desired destination.
     * Returns a pointer to the decoded string, or "OOPS" if it cannot be decoded.
     *
     * Accepted escapes are:
     *      %d       as in printf, 0 origin.
     *      %2, %3   like %02d, %03d in printf.
     *      %.       like %c
     *      %+x      adds <x> to value, then %.
     *      %>xy     if value > x, adds y. No output.
     *      %i       increments row & col. No output.
     *      %r       reverses order of row & col. No output.
     *      %%       prints as a single %.
     *      %n       exclusive or row & col with 0140.
     *      %B       BCD, no output.
     *      %D       reverse coding (x-2*(x%16)), no output.
     */
    /*private*/ static Bytes _tgoto(Bytes cm, int col, int row)
        /* cm: string, from termcap */
        /* col: x position */
        /* row: y position */
    {
        if (cm == null)
            return u8("OOPS");                          /* kludge, but standard */

        boolean reverse = false;                    /* reverse flag */
        boolean addup = false;                      /* add upline */
        boolean addbak = false;                     /* add backup */

        Bytes p = tgoto_buffer;                    /* pointer in returned string */

        while (cm.at(0) != NUL)
        {
            byte b = (cm = cm.plus(1)).at(-1);
            if (b != '%')                           /* normal char */
            {
                (p = p.plus(1)).be(-1, b);
                continue;
            }

            b = (cm = cm.plus(1)).at(-1);
            switch (b)                              /* % escape */
            {
                case 'd':                           /* decimal */
                    p = _addfmt(p, u8("%d"), row);
                    row = col;
                    break;

                case '2':                           /* 2 digit decimal */
                    p = _addfmt(p, u8("%02d"), row);
                    row = col;
                    break;

                case '3':                           /* 3 digit decimal */
                    p = _addfmt(p, u8("%03d"), row);
                    row = col;
                    break;

                case '>':                           /* %>xy: if >x, add y */
                {
                    byte x = (cm = cm.plus(1)).at(-1), y = (cm = cm.plus(1)).at(-1);
                    if (col > x)
                        col += y;
                    if (row > x)
                        row += y;
                    break;
                }

                case '+':                           /* %+c: add c */
                    row += (cm = cm.plus(1)).at(-1);

                case '.':                           /* print x/y */
                    if (row == '\t'                 /* these are */
                     || row == '\n'                 /* chars that */
                     || row == '\004'               /* UNIX hates */
                     || row == '\000')
                    {
                        row++;                      /* so go to next pos */
                        if (reverse == (row == col))
                            addup = true;           /* and mark UP */
                        else
                            addbak = true;          /* or BC */
                    }
                    (p = p.plus(1)).be(-1, row);
                    row = col;
                    break;

                case 'r':                           /* r: reverse */
                {
                    int r = row;
                    row = col;
                    col = r;
                    reverse = true;
                    break;
                }

                case 'i':                           /* increment (1-origin screen) */
                    col++;
                    row++;
                    break;

                case '%':                           /* %%=% literally */
                    (p = p.plus(1)).be(-1, (byte)'%');
                    break;

                case 'n':                           /* magic DM2500 code */
                    row ^= 0140;
                    col ^= 0140;
                    break;

                case 'B':                           /* bcd encoding */
                    row = ((row / 10) << 4) + (row % 10);
                    col = ((col / 10) << 4) + (col % 10);
                    break;

                case 'D':                           /* magic Delta Data code */
                    row -= 2 * (row & 15);
                    col -= 2 * (col & 15);
                    break;

                case 'p':                           /* so, what? */
                {
                    byte d = (cm = cm.plus(1)).at(-1);
                    if (d == '1' || d == '2')       /* ignore %p1 and %p2 */
                        break;
                    /* FALLTHROUGH */
                }

                default:                            /* unknown escape */
                    return u8("OOPS");
            }
        }

        if (addup)                                  /* add upline */
        {
            if (tgoto_UP != null)
            {
                cm = tgoto_UP;
                while (asc_isdigit(cm.at(0)) || cm.at(0) == (byte)'.')
                    cm = cm.plus(1);
                if (cm.at(0) == (byte)'*')
                    cm = cm.plus(1);
                while (cm.at(0) != NUL)
                    (p = p.plus(1)).be(-1, (cm = cm.plus(1)).at(-1));
            }
        }

        if (addbak)                                 /* add backspace */
        {
            if (tgoto_BC != null)
            {
                cm = tgoto_BC;
                while (asc_isdigit(cm.at(0)) || cm.at(0) == (byte)'.')
                    cm = cm.plus(1);
                if (cm.at(0) == (byte)'*')
                    cm = cm.plus(1);
                while (cm.at(0) != NUL)
                    (p = p.plus(1)).be(-1, (cm = cm.plus(1)).at(-1));
            }
            else
                (p = p.plus(1)).be(-1, (byte)'\b');
        }

        p.be(0, NUL);

        return tgoto_buffer;
    }

    /*
     * Note: "s" may have padding information ahead of it, in the form of nnnTEXT or nnn*TEXT.
     *  nnn is the number of milliseconds to delay, and may be a decimal fraction (nnn.mmm).
     *  In case an asterisk is given, the delay is to be multiplied by "_affcnt".
     */
    /*private*/ static int _tputs(Bytes s, int _affcnt)
        /* s: string to print */
        /* affcnt: number of lines affected */
    {
        int i = 0;

        if (asc_isdigit(s.at(i)))
        {
            while (asc_isdigit(s.at(++i)))
                ;
            if (s.at(i) == (byte)'.')
                while (asc_isdigit(s.at(++i)))
                    ;
            if (s.at(i) == (byte)'*')
                i++;
        }

        while (s.at(i) != NUL)
            out_char_nf(s.at(i++));

        return 0;
    }

    /*
     * A never-padding out_str:
     * use this whenever you don't want to run the string through tputs.
     * tputs above is harmless,
     * but tputs from the termcap library is likely to strip off leading digits,
     * that it mistakes for padding information, and "%i", "%d", etc.
     * This should only be used for writing terminal codes, not for outputting
     * normal text: use functions like msg_puts() and screen_putchar() for that.
     */
    /*private*/ static void out_str_nf(Bytes s)
    {
        if (OUT_SIZE - 20 < out_pos)    /* avoid terminal strings being split up */
            out_flush();

        for (int i = 0; s.at(i) != NUL; )
            out_char_nf(s.at(i++));

        /* For testing we write one string at a time. */
        if (p_wd[0] != 0)
            out_flush();
    }

    /*
     * out_str(s): Put a character string a byte at a time into the output buffer.
     * This should only be used for writing terminal codes, not for outputting
     * normal text: use functions like msg_puts() and screen_putchar() for that.
     */
    /*private*/ static void out_str(Bytes s)
    {
        if (s != null && s.at(0) != NUL)
        {
            /* avoid terminal strings being split up */
            if (OUT_SIZE - 20 < out_pos)
                out_flush();

            _tputs(s, 1);

            /* For testing we write one string at a time. */
            if (p_wd[0] != 0)
                out_flush();
        }
    }

    /*
     * cursor positioning using termcap parser
     */
    /*private*/ static void term_windgoto(int row, int col)
    {
        out_str(_tgoto(T_CM[0], col, row));
    }

    /*private*/ static void term_cursor_right(int i)
    {
        out_str(_tgoto(T_CRI[0], 0, i));
    }

    /*private*/ static void term_append_lines(int line_count)
    {
        out_str(_tgoto(T_CAL[0], 0, line_count));
    }

    /*private*/ static void term_delete_lines(int line_count)
    {
        out_str(_tgoto(T_CDL[0], 0, line_count));
    }

    /*private*/ static void term_set_winpos(int x, int y)
    {
        /* Can't handle a negative value here. */
        if (x < 0)
            x = 0;
        if (y < 0)
            y = 0;
        out_str(_tgoto(T_CWP[0], y, x));
    }

    /*private*/ static void term_set_winsize(int width, int height)
    {
        out_str(_tgoto(T_CWS[0], height, width));
    }

    /*private*/ static void term_fg_color(int n)
    {
        /* Use "AF" termcap entry if present, "Sf" entry otherwise. */
        if (T_CAF[0].at(0) != NUL)
            term_color(T_CAF[0], n);
        else if (T_CSF[0].at(0) != NUL)
            term_color(T_CSF[0], n);
    }

    /*private*/ static void term_bg_color(int n)
    {
        /* Use "AB" termcap entry if present, "Sb" entry otherwise. */
        if (T_CAB[0].at(0) != NUL)
            term_color(T_CAB[0], n);
        else if (T_CSB[0].at(0) != NUL)
            term_color(T_CSB[0], n);
    }

    /*private*/ static void term_color(Bytes s, int n)
    {
        int i = 2;  /* index in s[] just after <Esc>[ or CSI */

        /* Special handling of 16 colors, because termcap can't handle it.
         * Also accept "\e[3%dm" for TERMINFO, it is sometimes used.
         * Also accept CSI instead of <Esc>[. */
        if (8 <= n && 16 <= t_colors
                  && ((s.at(0) == ESC && s.at(1) == (byte)'[') || (s.at(0) == CSI && (i = 1) == 1))
                  && s.at(i) != NUL
                  && (STRCMP(s.plus(i + 1), u8("%p1%dm")) == 0 || STRCMP(s.plus(i + 1), u8("%dm")) == 0)
                  && (s.at(i) == (byte)'3' || s.at(i) == (byte)'4'))
        {
            Bytes buf = new Bytes(20);
            libC.sprintf(buf, u8("%s%s%%p1%%dm"),
                            (i == 2) ? u8("\033[") : u8("\233"),
                            (s.at(i) == (byte)'3') ? (16 <= n ? u8("38;5;") : u8("9")) : (16 <= n ? u8("48;5;") : u8("10")));
            s = buf;
            n = (16 <= n) ? n : n - 8;
        }

        out_str(_tgoto(s, 0, n));
    }

    /*
     * Make sure we have a valid set or terminal options.
     * Replace all entries that are null by EMPTY_OPTION.
     */
    /*private*/ static void ttest(boolean pairs)
    {
        check_options();            /* make sure no options are null */

        /*
         * MUST have "cm": cursor motion.
         */
        if (T_CM[0].at(0) == NUL)
            emsg(u8("E437: terminal capability \"cm\" required"));

        /*
         * If "cs" defined, use a scroll region, it's faster.
         */
        scroll_region = (T_CS[0].at(0) != NUL);

        /*
         * optional pairs
         */
        if (pairs)
        {
            /* TP goes to normal mode for TI (invert) and TB (bold). */
            if (T_ME[0].at(0) == NUL)
                T_ME[0] = T_MR[0] = T_MD[0] = T_MB[0] = EMPTY_OPTION;
            if (T_SO[0].at(0) == NUL || T_SE[0].at(0) == NUL)
                T_SO[0] = T_SE[0] = EMPTY_OPTION;
            if (T_US[0].at(0) == NUL || T_UE[0].at(0) == NUL)
                T_US[0] = T_UE[0] = EMPTY_OPTION;
            if (T_CZH[0].at(0) == NUL || T_CZR[0].at(0) == NUL)
                T_CZH[0] = T_CZR[0] = EMPTY_OPTION;

            /* T_VE is needed even though T_VI is not defined. */
            if (T_VE[0].at(0) == NUL)
                T_VI[0] = EMPTY_OPTION;

            /* If 'mr' or 'me' is not defined use 'so' and 'se'. */
            if (T_ME[0].at(0) == NUL)
            {
                T_ME[0] = T_SE[0];
                T_MR[0] = T_MD[0] = T_SO[0];
            }

            /* If 'so' or 'se' is not defined use 'mr' and 'me'. */
            if (T_SO[0].at(0) == NUL)
            {
                T_SE[0] = T_ME[0];
                T_SO[0] = (T_MR[0].at(0) == NUL) ? T_MD[0] : T_MR[0];
            }

            /* If 'ZH' or 'ZR' is not defined use 'mr' and 'me'. */
            if (T_CZH[0].at(0) == NUL)
            {
                T_CZR[0] = T_ME[0];
                T_CZH[0] = (T_MR[0].at(0) == NUL) ? T_MD[0] : T_MR[0];
            }

            /* "Sb" and "Sf" come in pairs. */
            if (T_CSB[0].at(0) == NUL || T_CSF[0].at(0) == NUL)
            {
                T_CSB[0] = EMPTY_OPTION;
                T_CSF[0] = EMPTY_OPTION;
            }

            /* "AB" and "AF" come in pairs. */
            if (T_CAB[0].at(0) == NUL || T_CAF[0].at(0) == NUL)
            {
                T_CAB[0] = EMPTY_OPTION;
                T_CAF[0] = EMPTY_OPTION;
            }

            /* If 'Sb' and 'AB' are not defined, reset "Co". */
            if (T_CSB[0].at(0) == NUL && T_CAB[0].at(0) == NUL)
                free_one_termoption(T_CCO);

            /* Set 'weirdinvert' according to value of 't_xs'. */
            p_wiv[0] = (T_XS[0].at(0) != NUL);
        }

        need_gather = true;

        /* Set t_colors to the value of t_Co. */
        t_colors = libC.atoi(T_CCO[0]);
    }

    /*
     * Read the next num_bytes bytes from buf, and store them in bytes.
     * Assume that buf has been through inchar().
     * Returns the actual number of bytes used from buf (between num_bytes and num_bytes*2),
     * or -1 if not enough bytes were available.
     */
    /*private*/ static int get_bytes_from_buf(Bytes buf, Bytes bytes, int num_bytes)
    {
        int len = 0;

        for (int i = 0; i < num_bytes; i++)
        {
            byte b = buf.at(len++);

            if (b == NUL)
                return -1;
            if (b == KB_SPECIAL)
            {
                if (buf.at(len) == NUL || buf.at(len + 1) == NUL)     /* cannot happen? */
                    return -1;
                if (buf.at(len++) == KS_ZERO)
                    b = NUL;
                /* else it should be KS_SPECIAL; when followed by KE_FILLER
                 * b is KB_SPECIAL, or followed by KE_CSI and b must be CSI. */
                if (buf.at(len++) == KE_CSI)
                    b = CSI;
            }
            else if (b == CSI && buf.at(len) == KS_EXTRA && buf.at(len + 1) == KE_CSI)
                /* CSI is stored as CSI KS_SPECIAL KE_CSI to avoid confusion
                 * with the start of a special key. */
                len += 2;

            bytes.be(i, b);
        }

        return len;
    }

    /*
     * Check if the new shell size is valid, correct it if it's too small or way too big.
     */
    /*private*/ static void check_shellsize()
    {
        int min = min_rows();
        if (Rows[0] < min)         /* need room for one window and command line */
            Rows[0] = min;
        limit_screen_size();
    }

    /*
     * Limit Rows and Columns to avoid an overflow in Rows * Columns.
     */
    /*private*/ static void limit_screen_size()
    {
        if (Columns[0] < MIN_COLUMNS)
            Columns[0] = MIN_COLUMNS;
        else if (10000 < Columns[0])
            Columns[0] = 10000;

        if (1000 < Rows[0])
            Rows[0] = 1000;
    }

    /*private*/ static long old__Rows;
    /*private*/ static long old__Columns;

    /*
     * Invoked just before the screen structures are going to be (re)allocated.
     */
    /*private*/ static void win_new_shellsize()
    {
        if (old__Rows != Rows[0] || old__Columns != Columns[0])
            ui_new_shellsize();
        if (old__Rows != Rows[0])
        {
            /* if 'window' uses the whole screen, keep it using that */
            if (p_window[0] == old__Rows - 1 || old__Rows == 0)
                p_window[0] = Rows[0] - 1;
            old__Rows = Rows[0];
            shell_new_rows();       /* update window sizes */
        }
        if (old__Columns != Columns[0])
        {
            old__Columns = Columns[0];
            shell_new_columns();    /* update window sizes */
        }
    }

    /*
     * Call this function when the Vim shell has been resized in any way.
     * Will obtain the current size and redraw (also when size didn't change).
     */
    /*private*/ static void shell_resized()
    {
        set_shellsize(0, 0, false);
    }

    /*private*/ static int _2_busy;

    /*
     * Set size of the Vim shell.
     * If 'mustset' is true, we must set Rows and Columns,
     * do not get the real window size (this is used for the :win command).
     * If 'mustset' is false, we may try to get the real window size
     * and if it fails, use 'width' and 'height'.
     */
    /*private*/ static void set_shellsize(int width, int height, boolean mustset)
    {
        /*
         * Avoid recursiveness, can happen when setting the window size causes
         * another window-changed signal.
         */
        if (_2_busy != 0)
            return;

        if (width < 0 || height < 0)    /* just checking... */
            return;

        if (State == HITRETURN || State == SETWSIZE)
        {
            /* postpone the resizing */
            State = SETWSIZE;
            return;
        }

        /* curwin.w_buffer can be null when we are closing a window and the buffer
         * has already been closed and removing a scrollbar causes a resize event.
         * Don't resize then, it will happen after entering another buffer.
         */
        if (curwin.w_buffer == null)
            return;

        _2_busy++;

        if (mustset || (!ui_get_shellsize() && height != 0))
        {
            Rows[0] = height;
            Columns[0] = width;
            check_shellsize();
            ui_set_shellsize(mustset);
        }
        else
            check_shellsize();

        /* The window layout used to be adjusted here, but it now happens in
         * screenalloc() (also invoked from screenclear()).  That is because
         * the "_2_busy" check above may skip this, but not screenalloc(). */

        if (State != ASKMORE && State != EXTERNCMD && State != CONFIRM)
            screenclear();
        else
            screen_start();     /* don't know where cursor is now */

        if (starting != NO_SCREEN)
        {
            changed_line_abv_curs();
            invalidate_botline();

            /*
             * We only redraw when it's needed:
             * - While at the more prompt or executing an external command, don't
             *   redraw, but position the cursor.
             * - While editing the command line, only redraw that.
             * - in Ex mode, don't redraw anything.
             * - Otherwise, redraw right now, and position the cursor.
             * Always need to call update_screen() or screenalloc(), to make
             * sure Rows/Columns and the size of screenLines[] is correct!
             */
            if (State == ASKMORE || State == EXTERNCMD || State == CONFIRM || exmode_active != 0)
            {
                screenalloc(false);
                repeat_message();
            }
            else
            {
                if (curwin.w_onebuf_opt.wo_scb[0])
                    do_check_scrollbind(true);
                if ((State & CMDLINE) != 0)
                {
                    update_screen(NOT_VALID);
                    redrawcmdline();
                }
                else
                {
                    update_topline();
                    update_screen(NOT_VALID);
                    if (redrawing())
                        setcursor();
                }
            }
            cursor_on();        /* redrawing may have switched it off */
        }

        out_flush();

        --_2_busy;
    }

    /*
     * Set the terminal to TMODE_RAW (for Normal mode)
     * or TMODE_COOK (for external commands and Ex mode).
     */
    /*private*/ static void settmode(int tmode)
    {
        if (full_screen)
        {
            /*
             * When returning after calling a shell we want to really set the
             * terminal to raw mode, even though we think it already is, because
             * the shell program may have reset the terminal mode.
             * When we think the terminal is normal, don't try to set it to
             * normal again, because that causes problems (logout!) on some machines.
             */
            if (tmode != TMODE_COOK || cur_tmode != TMODE_COOK)
            {
                /* May need to check for T_CRV response and termcodes,
                 * it doesn't work in Cooked mode, an external program may get them. */
                if (tmode != TMODE_RAW && (crv_status == CRV_SENT || u7_status == U7_SENT))
                    vpeekc_nomap();
                check_for_codes_from_term();

                if (tmode != TMODE_RAW)
                    mch_setmouse(false);            /* switch mouse off */
                out_flush();
                mch_settmode(tmode);                /* machine specific function */
                cur_tmode = tmode;
                if (tmode == TMODE_RAW)
                    setmouse();                     /* may switch mouse on */
                out_flush();
            }
            may_req_termresponse();
        }
    }

    /*private*/ static void starttermcap()
    {
        if (full_screen && !termcap_active)
        {
            out_str(T_TI[0]);                  /* start termcap mode */
            out_str(T_KS[0]);                  /* start "keypad transmit" mode */
            out_flush();
            termcap_active = true;
            screen_start();                 /* don't know where cursor is now */

            may_req_termresponse();
            /* Immediately check for a response.
             * If t_Co changes, we don't want to redraw with wrong colors first. */
            if (crv_status == CRV_SENT)
                check_for_codes_from_term();
        }
    }

    /*private*/ static void stoptermcap()
    {
        screen_stop_highlight();
        reset_cterm_colors();
        if (termcap_active)
        {
            /* May need to discard T_CRV or T_U7 response. */
            if (crv_status == CRV_SENT || u7_status == U7_SENT)
            {
                /* Give the terminal a chance to respond. */
                mch_delay(100L, false);
                /* Discard data received but not read. */
                if (exiting)
                    libc.tcflush(libc.fileno(stdin), TCIFLUSH);
            }
            /* Check for termcodes first, otherwise an external program may get them. */
            check_for_codes_from_term();

            out_str(T_KE[0]);                  /* stop "keypad transmit" mode */
            out_flush();
            termcap_active = false;
            cursor_on();                    /* just in case it is still off */
            out_str(T_TE[0]);                  /* stop termcap mode */
            screen_start();                 /* don't know where cursor is now */
            out_flush();
        }
    }

    /*
     * Request version string (for xterm) when needed.
     * Only do this after switching to raw mode, otherwise the result will be echoed.
     * Only do this after startup has finished, to avoid that the response comes
     * while executing "-c !cmd" or even after "-c quit".
     * Only do this after termcap mode has been started, otherwise the codes for
     * the cursor keys may be wrong.
     * Only do this when 'esckeys' is on, otherwise the response causes trouble in Insert mode.
     * On Unix only do it when both output and input are a tty (avoid writing
     * request to terminal while reading from a file).
     * The result is caught in check_termcode().
     */
    /*private*/ static void may_req_termresponse()
    {
        if (crv_status == CRV_GET
                && cur_tmode == TMODE_RAW
                && starting == 0
                && termcap_active
                && p_ek[0]
                && libc.isatty(1) != 0
                && libc.isatty(read_cmd_fd) != 0
                && T_CRV[0].at(0) != NUL)
        {
            out_str(T_CRV[0]);
            crv_status = CRV_SENT;
            /* check for the characters now, otherwise they might be eaten by get_keystroke() */
            out_flush();
            vpeekc_nomap();
        }
    }

    /*
     * Check how the terminal treats ambiguous character width (UAX #11).
     * First, we move the cursor to (1, 0) and print a test ambiguous character
     * \u25bd (WHITE DOWN-POINTING TRIANGLE) and query current cursor position.
     * If the terminal treats \u25bd as single width, the position is (1, 1),
     * or if it is treated as double width, that will be (1, 2).
     * This function has the side effect that changes cursor position, so
     * it must be called immediately after entering termcap mode.
     */
    /*private*/ static void may_req_ambiguous_char_width()
    {
        if (u7_status == U7_GET
                && cur_tmode == TMODE_RAW
                && termcap_active
                && p_ek[0]
                && libc.isatty(1) != 0
                && libc.isatty(read_cmd_fd) != 0
                && T_U7[0].at(0) != NUL
                && !option_was_set(u8("ambiwidth")))
        {
            Bytes buf = new Bytes(16);

            /* Do this in the second row.
             * In the first row the returned sequence may be CSI 1;2R, which is the same as <S-F3>. */
            term_windgoto(1, 0);
            buf.be(utf_char2bytes(0x25bd, buf), 0);
            out_str(buf);
            out_str(T_U7[0]);
            u7_status = U7_SENT;
            out_flush();
            term_windgoto(1, 0);
            out_str(u8("  "));
            term_windgoto(0, 0);
            /* check for the characters now, otherwise they might be eaten by get_keystroke() */
            out_flush();
            vpeekc_nomap();
        }
    }

    /*
     * Return true when saving and restoring the screen.
     */
    /*private*/ static boolean swapping_screen()
    {
        return (full_screen && T_TI[0].at(0) != NUL);
    }

    /*
     * setmouse() - switch mouse on/off depending on current mode and 'mouse'
     */
    /*private*/ static void setmouse()
    {
        /* be quick when mouse is off */
        if (p_mouse[0].at(0) == NUL || !has_mouse_termcode)
            return;

        /* don't switch mouse on when not in raw mode (Ex mode) */
        if (cur_tmode != TMODE_RAW)
        {
            mch_setmouse(false);
            return;
        }

        byte checkfor;
        if (VIsual_active)
            checkfor = MOUSE_VISUAL;
        else if (State == HITRETURN || State == ASKMORE || State == SETWSIZE)
            checkfor = MOUSE_RETURN;
        else if ((State & INSERT) != 0)
            checkfor = MOUSE_INSERT;
        else if ((State & CMDLINE) != 0)
            checkfor = MOUSE_COMMAND;
        else if (State == CONFIRM || State == EXTERNCMD)
            checkfor = ' ';     /* don't use mouse for ":confirm" or ":!cmd" */
        else
            checkfor = MOUSE_NORMAL;    /* assume normal mode */

        mch_setmouse(mouse_has(checkfor));
    }

    /*
     * Return true if
     * - "b" is in 'mouse', or
     * - 'a' is in 'mouse' and "b" is in MOUSE_A, or ...
     */
    /*private*/ static boolean mouse_has(byte b)
    {
        for (Bytes p = p_mouse[0]; p.at(0) != NUL; p = p.plus(1))
            switch (p.at(0))
            {
                case 'a':
                    if (vim_strchr(MOUSE_A, b) != null)
                        return true;
                    break;
                case MOUSE_HELP:
                    break;
                default:
                    if (p.at(0) == b)
                        return true;
                    break;
            }
        return false;
    }

    /*
     * Return true when 'mousemodel' is set to "popup" or "popup_setpos".
     */
    /*private*/ static boolean mouse_model_popup()
    {
        return (p_mousem[0].at(0) == (byte)'p');
    }

    /*
     * By outputting the 'cursor very visible' termcap code, for some windowed
     * terminals this makes the screen scrolled to the correct position.
     * Used when starting Vim or returning from a shell.
     */
    /*private*/ static void scroll_start()
    {
        if (T_VS[0].at(0) != NUL)
        {
            out_str(T_VS[0]);
            out_str(T_VE[0]);
            screen_start();                 /* don't know where cursor is now */
        }
    }

    /*private*/ static boolean cursor_is_off;

    /*
     * Enable the cursor.
     */
    /*private*/ static void cursor_on()
    {
        if (cursor_is_off)
        {
            out_str(T_VE[0]);
            cursor_is_off = false;
        }
    }

    /*
     * Disable the cursor.
     */
    /*private*/ static void cursor_off()
    {
        if (full_screen)
        {
            if (!cursor_is_off)
                out_str(T_VI[0]);          /* disable cursor */
            cursor_is_off = true;
        }
    }

    /*private*/ static int showing_mode = NORMAL;

    /*
     * Set cursor shape to match Insert or Replace mode.
     */
    /*private*/ static void term_cursor_shape()
    {
        /* Only do something when redrawing the screen and we can restore the mode. */
        if (!full_screen || T_CEI[0].at(0) == NUL)
            return;

        if ((State & REPLACE) == REPLACE)
        {
            if (showing_mode != REPLACE)
            {
                Bytes p;
                if (T_CSR[0].at(0) != NUL)
                    p = T_CSR[0];                  /* Replace mode cursor */
                else
                    p = T_CSI[0];                  /* fall back to Insert mode cursor */
                if (p.at(0) != NUL)
                {
                    out_str(p);
                    showing_mode = REPLACE;
                }
            }
        }
        else if ((State & INSERT) != 0)
        {
            if (showing_mode != INSERT && T_CSI[0].at(0) != NUL)
            {
                out_str(T_CSI[0]);                 /* Insert mode cursor */
                showing_mode = INSERT;
            }
        }
        else if (showing_mode != NORMAL)
        {
            out_str(T_CEI[0]);                     /* non-Insert mode cursor */
            showing_mode = NORMAL;
        }
    }

    /*
     * Set scrolling region for window 'wp'.
     * The region starts 'off' lines from the start of the window.
     * Also set the vertical scroll region for a vertically split window.
     * Always the full width of the window, excluding the vertical separator.
     */
    /*private*/ static void scroll_region_set(window_C wp, int off)
    {
        out_str(_tgoto(T_CS[0], wp.w_winrow + wp.w_height - 1, wp.w_winrow + off));

        if (T_CSV[0].at(0) != NUL && wp.w_width != (int)Columns[0])
            out_str(_tgoto(T_CSV[0], wp.w_wincol + wp.w_width - 1, wp.w_wincol));

        screen_start();                 /* don't know where cursor is now */
    }

    /*
     * Reset scrolling region to the whole screen.
     */
    /*private*/ static void scroll_region_reset()
    {
        out_str(_tgoto(T_CS[0], (int)Rows[0] - 1, 0));

        if (T_CSV[0].at(0) != NUL)
            out_str(_tgoto(T_CSV[0], (int)Columns[0] - 1, 0));

        screen_start();                 /* don't know where cursor is now */
    }

    /*
     * List of terminal codes that are currently recognized.
     */

    /*private*/ static final class termcode_C
    {
        Bytes name = new Bytes(2);     /* termcap name of entry */
        Bytes code;                   /* terminal code (in allocated memory) */
        int     len;                    /* strlen(code) */
        int     modlen;                 /* length of part before ";*~". */

        /*private*/ termcode_C()
        {
        }
    }

    /*private*/ static void COPY_termcode(termcode_C tc1, termcode_C tc0)
    {
        tc1.name = tc0.name;
        tc1.code = tc0.code;
        tc1.len = tc0.len;
        tc1.modlen = tc0.modlen;
    }

    /*private*/ static termcode_C[] ARRAY_termcode(int n)
    {
        termcode_C[] a = new termcode_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new termcode_C();
        return a;
    }

    /*private*/ static termcode_C[] termcodes;

    /*private*/ static int tc_max_len;  /* number of entries that termcodes[] can hold */
    /*private*/ static int tc_len;      /* current number of entries in termcodes[] */

    /*private*/ static void clear_termcodes()
    {
        tc_len = 0;
        tc_max_len = 0;
        termcodes = null;

        need_gather = true;         /* need to fill termleader[] */
    }

    /*private*/ static final int ATC_FROM_TERM = 55;

    /*
     * Add a new entry to the list of terminal codes.
     * The list is kept alphabetical for ":set termcap"
     * "flags" is true when replacing 7-bit by 8-bit controls is desired.
     * "flags" can also be ATC_FROM_TERM for got_code_from_term().
     */
    /*private*/ static void add_termcode(Bytes name, Bytes string, int flags)
    {
        if (string == null || string.at(0) == NUL)
        {
            del_termcode(name);
            return;
        }

        Bytes code = STRDUP(string);

        /* Change leading <Esc>[ to CSI, change <Esc>O to <M-O>. */
        if (flags != 0 && flags != ATC_FROM_TERM)
        {
            byte esc = term_7to8bit(code);
            if (esc != NUL)
            {
                code.be(0, esc);
                BCOPY(code, 1, code, 2, strlen(code, 2) + 1);
            }
        }

        int len = strlen(code);

        need_gather = true;         /* need to fill termleader[] */

        /*
         * need to make space for more entries
         */
        if (tc_len == tc_max_len)
        {
            tc_max_len += 20;
            termcode_C[] new_tc = ARRAY_termcode(tc_max_len);
            for (int i = 0; i < tc_len; i++)
                COPY_termcode(new_tc[i], termcodes[i]);
            termcodes = new_tc;
        }

        /*
         * Look for existing entry with the same name, it is replaced.
         * Look for an existing entry that is alphabetical higher, the new entry is inserted in front of it.
         */
        int i;
        for (i = 0; i < tc_len; i++)
        {
            if (char_u(termcodes[i].name.at(0)) < char_u(name.at(0)))
                continue;
            if (termcodes[i].name.at(0) == name.at(0))
            {
                if (char_u(termcodes[i].name.at(1)) < char_u(name.at(1)))
                    continue;
                /*
                 * Exact match: May replace old code.
                 */
                if (termcodes[i].name.at(1) == name.at(1))
                {
                    int j;
                    if (flags == ATC_FROM_TERM && 0 < (j = termcode_star(termcodes[i].code, termcodes[i].len)))
                    {
                        /* Don't replace ESC[123;*X or ESC O*X with another
                         * when invoked from got_code_from_term(). */
                        if (len == termcodes[i].len - j
                                && STRNCMP(code, termcodes[i].code, len - 1) == 0
                                && code.at(len - 1) == termcodes[i].code.at(termcodes[i].len - 1))
                        {
                            /* They are equal but for the ";*": don't add it. */
                            return;
                        }
                    }
                    else
                    {
                        /* Replace old code. */
                        termcodes[i].code = null;
                        --tc_len;
                        break;
                    }
                }
            }
            /*
             * Found alphabetical larger entry, move rest to insert new entry
             */
            for (int j = tc_len; i < j; --j)
                COPY_termcode(termcodes[j], termcodes[j - 1]);
            break;
        }

        termcodes[i].name.be(0, name.at(0));
        termcodes[i].name.be(1, name.at(1));
        termcodes[i].code = code;
        termcodes[i].len = len;

        /* For xterm we recognize special codes like "ESC[42;*X" and "ESC O*X" that accept modifiers. */
        termcodes[i].modlen = 0;
        int j = termcode_star(code, len);
        if (0 < j)
            termcodes[i].modlen = len - 1 - j;
        tc_len++;
    }

    /*
     * Check termcode "code[len]" for ending in ;*X, <Esc>O*X or <M-O>*X.
     * The "X" can be any character.
     * Return 0 if not found, 2 for ;*X and 1 for O*X and <M-O>*X.
     */
    /*private*/ static int termcode_star(Bytes code, int len)
    {
        /* Shortest is <M-O>*X.  With ; shortest is <CSI>1;*X. */
        if (3 <= len && code.at(len - 2) == (byte)'*')
        {
            if (5 <= len && code.at(len - 3) == (byte)';')
                return 2;
            if ((4 <= len && code.at(len - 3) == (byte)'O') || char_u(code.at(len - 3)) == 'O' + 0x80)
                return 1;
        }
        return 0;
    }

    /*private*/ static Bytes find_termcode(Bytes name)
    {
        for (int i = 0; i < tc_len; i++)
            if (termcodes[i].name.at(0) == name.at(0) && termcodes[i].name.at(1) == name.at(1))
                return termcodes[i].code;

        return null;
    }

    /*private*/ static Bytes get_termcode(int i)
    {
        if (i < tc_len)
            return termcodes[i].name;

        return null;
    }

    /*private*/ static void del_termcode(Bytes name)
    {
        if (termcodes == null)      /* nothing there yet */
            return;

        need_gather = true;         /* need to fill termleader[] */

        for (int i = 0; i < tc_len; i++)
            if (termcodes[i].name.at(0) == name.at(0) && termcodes[i].name.at(1) == name.at(1))
            {
                del_termcode_idx(i);
                return;
            }
        /* Not found.  Give error message? */
    }

    /*private*/ static void del_termcode_idx(int idx)
    {
        termcodes[idx].code = null;
        --tc_len;
        for (int i = idx; i < tc_len; i++)
            COPY_termcode(termcodes[i], termcodes[i + 1]);
    }

    /*
     * Called when detected that the terminal sends 8-bit codes.
     * Convert all 7-bit codes to their 8-bit equivalent.
     */
    /*private*/ static void switch_to_8bit()
    {
        /* Only need to do something when not already using 8-bit codes. */
        if (!term_is_8bit(T_NAME[0]))
        {
            for (int i = 0; i < tc_len; i++)
            {
                Bytes code = termcodes[i].code;
                byte esc = term_7to8bit(code);
                if (esc != NUL)
                {
                    code.be(0, esc);
                    BCOPY(code, 1, code, 2, strlen(code, 2) + 1);
                }
            }

            need_gather = true;             /* need to fill termleader[] */
        }

        detected_8bit = true;
    }

    /*
     * Checking for double clicks ourselves.
     * "orig_topline" is used to avoid detecting a double-click when the window contents scrolled
     * (e.g., when 'scrolloff' is non-zero).
     */
    /*private*/ static long orig_topline;

    /*
     * Set orig_topline.  Used when jumping to another window, so that a double click still works.
     */
    /*private*/ static void set_mouse_topline(window_C wp)
    {
        orig_topline = wp.w_topline;
    }

    /*private*/ static int held_button = MOUSE_RELEASE;
    /*private*/ static int orig_num_clicks = 1;
    /*private*/ static int orig_mouse_code;
    /*private*/ static int orig_mouse_col;
    /*private*/ static int orig_mouse_row;
    /*private*/ static timeval_C orig_mouse_time = new timeval_C();   /* time of previous mouse click */

    /*
     * Check if typebuf.tb_buf[] contains a terminal key code.
     * Check from typebuf.tb_buf[typebuf.tb_off] to typebuf.tb_buf[typebuf.tb_off + max_offset].
     * Return 0 for no match, -1 for partial match, > 0 for full match.
     * Return KEYLEN_REMOVED when a key code was deleted.
     * With a match, the match is removed, the replacement code is inserted in
     * typebuf.tb_buf[] and the number of characters in typebuf.tb_buf[] is returned.
     * When "buf" is not null, buf[bufsize] is used instead of typebuf.tb_buf[].
     * "buflen" is then the length of the string in buf[] and is updated for inserts and deletes.
     */
    /*private*/ static int check_termcode(int max_offset, Bytes buf, int bufsize, int[] buflen)
    {
        int retval = 0;

        boolean cpo_koffset = (vim_strbyte(p_cpo[0], CPO_KOFFSET) != null);

        /*
         * Speed up the checks for terminal codes by gathering all first bytes used in termleader[].
         * Often this is just a single <Esc>.
         */
        if (need_gather)
            gather_termleader();

        int slen = 0;
        int mouse_code = 0;
        int wheel_code = 0;

        /*
         * Check at several positions in typebuf.tb_buf[], to catch something like "x<Up>"
         * that can be mapped.  Stop at max_offset, because characters after that cannot be
         * used for mapping, and with @r commands typebuf.tb_buf[] can become very long.
         * This is used often, KEEP IT FAST!
         */
        for (int offset = 0; offset < max_offset; offset++)
        {
            Bytes tp;
            int len;

            if (buf == null)
            {
                if (typebuf.tb_len <= offset)
                    break;
                tp = typebuf.tb_buf.plus(typebuf.tb_off + offset);
                len = typebuf.tb_len - offset;      /* length of the input */
            }
            else
            {
                if (buflen[0] <= offset)
                    break;
                tp = buf.plus(offset);
                len = buflen[0] - offset;
            }

            /*
             * Don't check characters after KB_SPECIAL, those are already
             * translated terminal chars (avoid translating ~@^Hx).
             */
            if (tp.at(0) == KB_SPECIAL)
            {
                offset += 2;        /* there are always 2 extra characters */
                continue;
            }

            /*
             * Skip this position if the character does not appear as the first character in 'term_strings'.
             * This speeds up a lot, since most termcodes start with the same character (ESC or CSI).
             */
            Bytes q;
            for (q = termleader; q.at(0) != NUL && q.at(0) != tp.at(0); q = q.plus(1))
                ;
            if (q.at(0) == NUL)
                continue;

            /*
             * Skip this position if "p_ek" is not set and *tp is an ESC and we are in Insert mode.
             */
            if (tp.at(0) == ESC && !p_ek[0] && (State & INSERT) != 0)
                continue;

            Bytes key_name = new Bytes(2);
            int[] modifiers = { 0 };      /* no modifiers yet */

            int idx;
            for (idx = 0; idx < tc_len; idx++)
            {
                /*
                 * Ignore the entry if we are not at the start of typebuf.tb_buf[]
                 * and there are not enough characters to make a match.
                 * But only when the 'K' flag is in 'cpoptions'.
                 */
                slen = termcodes[idx].len;
                if (cpo_koffset && offset != 0 && len < slen)
                    continue;

                if (STRNCMP(termcodes[idx].code, tp, (len < slen) ? len : slen) == 0)
                {
                    if (len < slen)             /* got a partial sequence */
                        return -1;              /* need to get more chars */

                    /*
                     * When found a keypad key, check if there is another key that matches and use that one.
                     * This makes <Home> to be found instead of <kHome> when they produce the same key code.
                     */
                    if (termcodes[idx].name.at(0) == (byte)'K' && asc_isdigit(termcodes[idx].name.at(1)))
                    {
                        for (int j = idx + 1; j < tc_len; j++)
                            if (termcodes[j].len == slen
                                && STRNCMP(termcodes[idx].code, termcodes[j].code, slen) == 0)
                            {
                                idx = j;
                                break;
                            }
                    }

                    key_name.be(0, termcodes[idx].name.at(0));
                    key_name.be(1, termcodes[idx].name.at(1));
                    break;
                }

                /*
                 * Check for code with modifier, like xterm uses:
                 * <Esc>[123;*X (modslen == slen - 3), also <Esc>O*X and <M-O>*X (modslen == slen - 2).
                 * When there is a modifier the * matches a number.
                 * When there is no modifier the ;* or * is omitted.
                 */
                if (0 < termcodes[idx].modlen)
                {
                    int modslen = termcodes[idx].modlen;
                    if (cpo_koffset && offset != 0 && len < modslen)
                        continue;

                    if (STRNCMP(termcodes[idx].code, tp, (len < modslen) ? len : modslen) == 0)
                    {
                        if (len <= modslen)     /* got a partial sequence */
                            return -1;          /* need to get more chars */

                        if (tp.at(modslen) == termcodes[idx].code.at(slen - 1))
                            slen = modslen + 1; /* no modifiers */
                        else if (tp.at(modslen) != (byte)';' && modslen == slen - 3)
                            continue;   /* no match */
                        else
                        {
                            /* Skip over the digits, the final char must follow. */
                            int j;
                            for (j = slen - 2; j < len && asc_isdigit(tp.at(j)); j++)
                                ;
                            j++;
                            if (len < j)        /* got a partial sequence */
                                return -1;      /* need to get more chars */
                            if (tp.at(j - 1) != termcodes[idx].code.at(slen - 1))
                                continue;       /* no match */

                            /* Match!  Convert modifier bits. */
                            int n = libC.atoi(tp.plus(slen - 2)) - 1;
                            if ((n & 1) != 0)
                                modifiers[0] |= MOD_MASK_SHIFT;
                            if ((n & 2) != 0)
                                modifiers[0] |= MOD_MASK_ALT;
                            if ((n & 4) != 0)
                                modifiers[0] |= MOD_MASK_CTRL;
                            if ((n & 8) != 0)
                                modifiers[0] |= MOD_MASK_META;

                            slen = j;
                        }

                        key_name.be(0, termcodes[idx].name.at(0));
                        key_name.be(1, termcodes[idx].name.at(1));
                        break;
                    }
                }
            }

            /* Mouse codes of DEC, pterm, and URXVT start with <ESC>[.
             * When detecting the start of these mouse codes they might
             * as well be another key code or terminal response. */
            if (key_name.at(0) == NUL)
            {
                /* Check for some responses from the terminal starting with "<Esc>[" or CSI:
                 *
                 * - Xterm version string: <Esc>[>{x};{vers};{y}c
                 *   Also eat other possible responses to t_RV, rxvt returns "<Esc>[?1;2c".
                 *   Also accept CSI instead of <Esc>[.
                 *   mrxvt has been reported to have "+" in the version.
                 *   Assume the escape sequence ends with a letter or one of "{|}~".
                 *
                 * - Cursor position report: <Esc>[{row};{col}R
                 *   The final byte must be 'R'.
                 *   It is used for checking the ambiguous-width character state.
                 */
                Bytes p = (tp.at(0) == CSI) ? tp.plus(1) : tp.plus(2);
                if ((T_CRV[0].at(0) != NUL || T_U7[0].at(0) != NUL)
                            && ((tp.at(0) == ESC && tp.at(1) == (byte)'[' && 3 <= len) || (tp.at(0) == CSI && 2 <= len))
                            && (asc_isdigit(p.at(0)) || p.at(0) == (byte)'>' || p.at(0) == (byte)'?'))
                {
                    int j = 0;
                    int extra = 0;
                    byte row_char = NUL;

                    int i;
                    for (i = 2 + ((tp.at(0) != CSI) ? 1 : 0); i < len && !('{' <= tp.at(i) && tp.at(i) <= '~') && !asc_isalpha(tp.at(i)); i++)
                        if (tp.at(i) == (byte)';' && ++j == 1)
                        {
                            extra = i + 1;
                            row_char = tp.at(i - 1);
                        }
                    if (i == len)
                        return -1;

                    int col = (0 < extra) ? libC.atoi(tp.plus(extra)) : 0;

                    /* Eat it when it has 2 arguments and ends in 'R'.  Also when u7_status is not
                     * "sent", it may be from a previous Vim that just exited.  But not for <S-F3>,
                     * it sends something similar, check for row and column to make sense. */
                    if (j == 1 && tp.at(i) == (byte)'R')
                    {
                        if (row_char == '2' && 2 <= col)
                        {
                            u7_status = U7_GOT;
                            did_cursorhold = true;

                            Bytes aw = null;
                            if (col == 2)
                                aw = u8("single");
                            else if (col == 3)
                                aw = u8("double");
                            if (aw != null && STRCMP(aw, p_ambw[0]) != 0)
                            {
                                /* Setting the option causes a screen redraw.
                                 * Do that right away if possible, keeping any messages. */
                                set_option_value(u8("ambw"), 0L, aw, 0);
                                redraw_asap(CLEAR);
                            }
                        }
                        key_name.be(0, KS_EXTRA);
                        key_name.be(1, KE_IGNORE);
                        slen = i + 1;
                    }

                    /* Eat it when at least one digit and ending in 'c'. */
                    else if (T_CRV[0].at(0) != NUL && 2 + ((tp.at(0) != CSI) ? 1 : 0) < i && tp.at(i) == (byte)'c')
                    {
                        crv_status = CRV_GOT;
                        did_cursorhold = true;

                        /* If this code starts with CSI, you can bet that the terminal uses 8-bit codes. */
                        if (tp.at(0) == CSI)
                            switch_to_8bit();

                        /* rxvt sends its version number: "20703" is 2.7.3.
                         * Ignore it for when the user has set 'term' to xterm, even though it's an rxvt. */
                        if (0 < extra)
                            extra = libC.atoi(tp.plus(extra));
                        if (20000 < extra)
                            extra = 0;

                        if (tp.at(1 + ((tp.at(0) != CSI) ? 1 : 0)) == '>' && j == 2)
                        {
                            /* Only set 'ttymouse' automatically if it was not set by the user already. */
                            if (!option_was_set(u8("ttym")))
                            {
                                /* if xterm version >= 95 use mouse dragging */
                                if (95 <= extra)
                                    set_option_value(u8("ttym"), 0L, u8("xterm2"), 0);
                            }

                            /* if xterm version >= 141 try to get termcap codes */
                            if (141 <= extra)
                            {
                                check_for_codes = true;
                                need_gather = true;
                                req_codes_from_term();
                            }
                        }
                        set_vim_var_string(VV_TERMRESPONSE, tp, i + 1);
                        apply_autocmds(EVENT_TERMRESPONSE, null, null, false, curbuf);
                        key_name.be(0, KS_EXTRA);
                        key_name.be(1, KE_IGNORE);
                        slen = i + 1;
                    }
                }

                /* Check for '<Esc>P1+r<hex bytes><Esc>\'.
                 * A "0" instead of the "1" means an invalid request. */
                else if (check_for_codes && ((tp.at(0) == ESC && tp.at(1) == (byte)'P' && 2 <= len) || tp.at(0) == DCS))
                {
                    int i;

                    int j = 1 + ((tp.at(0) != DCS) ? 1 : 0);
                    for (i = j; i < len; i++)
                        if ((tp.at(i) == ESC && tp.at(i + 1) == (byte)'\\' && i + 1 < len) || tp.at(i) == STERM)
                        {
                            if (3 <= i - j && tp.at(j + 1) == (byte)'+' && tp.at(j + 2) == (byte)'r')
                                got_code_from_term(tp.plus(j), i);
                            key_name.be(0, KS_EXTRA);
                            key_name.be(1, KE_IGNORE);
                            slen = i + 1 + (tp.at(i) == ESC ? 1 : 0);
                            break;
                        }

                    if (i == len)
                        return -1;          /* not enough characters */
                }
            }

            if (key_name.at(0) == NUL)
                continue;           /* no match at this position, try next one */

            /* We only get here when we have a complete termcode match. */

            /*
             * If it is a mouse click, get the coordinates.
             */
            if (key_name.at(0) == KS_MOUSE)
            {
                /*
                 * For xterm and MSDOS we get "<t_mouse>scr", where
                 *  s == encoded button state:
                 *         0x20 = left button down
                 *         0x21 = middle button down
                 *         0x22 = right button down
                 *         0x23 = any button release
                 *         0x60 = button 4 down (scroll wheel down)
                 *         0x61 = button 5 down (scroll wheel up)
                 *      add 0x04 for SHIFT
                 *      add 0x08 for ALT
                 *      add 0x10 for CTRL
                 *      add 0x20 for mouse drag (0x40 is drag with left button)
                 *  c == column + ' ' + 1 == column + 33
                 *  r == row + ' ' + 1 == row + 33
                 *
                 * The coordinates are passed on through global variables.
                 * Ugly, but this avoids trouble with mouse clicks at an
                 * unexpected moment and allows for mapping them.
                 */
                for ( ; ; )
                {
                    Bytes bytes = new Bytes(6);
                    int n = get_bytes_from_buf(tp.plus(slen), bytes, 3);
                    if (n == -1)    /* not enough coordinates */
                        return -1;

                    mouse_code = bytes.at(0);
                    mouse_col = bytes.at(1) - ' ' - 1;
                    mouse_row = bytes.at(2) - ' ' - 1;

                    slen += n;

                    /* If the following bytes is also a mouse code
                     * and it has the same code, dump this one and get the next.
                     * This makes dragging a whole lot faster. */
                    n = termcodes[idx].len;
                    if (STRNCMP(tp, tp.plus(slen), n) == 0
                            && tp.at(slen + n) == mouse_code
                            && tp.at(slen + n + 1) != NUL
                            && tp.at(slen + n + 2) != NUL)
                        slen += n;
                    else
                        break;
                }

                /*
                 * Handle mouse events.
                 * Recognize the xterm mouse wheel, but not in the GUI, the Linux console
                 * with GPM and the MS-DOS or Win32 console (multi-clicks use >= 0x60).
                 */
                if (MOUSEWHEEL_LOW <= mouse_code)
                {
                    /* Keep the mouse_code before it's changed,
                     * so that we remember that it was a mouse wheel click. */
                    wheel_code = mouse_code;
                }
                else if (held_button == MOUSE_RELEASE && (mouse_code == 0x23 || mouse_code == 0x24))
                {
                    /* Apparently used by rxvt scroll wheel. */
                    wheel_code = mouse_code - 0x23 + MOUSEWHEEL_LOW;
                }
                else if (1 < use_xterm_mouse())
                {
                    if ((mouse_code & MOUSE_DRAG_XTERM) != 0)
                        mouse_code |= MOUSE_DRAG;
                }

                boolean is_click = false, is_drag = false;

                /* Interpret the mouse code. */
                int current_button = (mouse_code & MOUSE_CLICK_MASK);
                if (current_button == MOUSE_RELEASE && wheel_code == 0)
                {
                    /*
                     * If we get a mouse drag or release event when there is no mouse button
                     * held down (held_button == MOUSE_RELEASE), produce a K_IGNORE below.
                     * (can happen when you hold down two buttons and then let them go, or
                     * click in the menu bar, but not on a menu, and drag into the text).
                     */
                    if ((mouse_code & MOUSE_DRAG) == MOUSE_DRAG)
                        is_drag = true;
                    current_button = held_button;
                }
                else if (wheel_code == 0)
                {
                    timeval_C mouse_time = new timeval_C();          /* time of current mouse click */
                    long timediff;                      /* elapsed time in msec */

                    /*
                     * Compute the time elapsed since the previous mouse click.
                     */
                    libC._gettimeofday(mouse_time);
                    timediff = (mouse_time.tv_usec() - orig_mouse_time.tv_usec()) / 1000;
                    if (timediff < 0)
                        orig_mouse_time.tv_sec(orig_mouse_time.tv_sec() - 1);
                    timediff += (mouse_time.tv_sec() - orig_mouse_time.tv_sec()) * 1000;
                    COPY_timeval(orig_mouse_time, mouse_time);
                    if (mouse_code == orig_mouse_code
                            && timediff < p_mouset[0]
                            && orig_num_clicks != 4
                            && orig_mouse_col == mouse_col
                            && orig_mouse_row == mouse_row
                            && (orig_topline == curwin.w_topline
                                /* Double click in tab pages line also works when window contents changes. */
                                || (mouse_row == 0 && 0 < firstwin.w_winrow)))
                        orig_num_clicks++;
                    else
                        orig_num_clicks = 1;
                    orig_mouse_col = mouse_col;
                    orig_mouse_row = mouse_row;
                    orig_topline = curwin.w_topline;

                    is_click = true;
                    orig_mouse_code = mouse_code;
                }
                if (!is_drag)
                    held_button = (mouse_code & MOUSE_CLICK_MASK);

                /*
                 * Translate the actual mouse event into a pseudo mouse event.
                 * First work out what modifiers are to be used.
                 */
                if ((orig_mouse_code & MOUSE_SHIFT) != 0)
                    modifiers[0] |= MOD_MASK_SHIFT;
                if ((orig_mouse_code & MOUSE_CTRL) != 0)
                    modifiers[0] |= MOD_MASK_CTRL;
                if ((orig_mouse_code & MOUSE_ALT) != 0)
                    modifiers[0] |= MOD_MASK_ALT;
                if (orig_num_clicks == 2)
                    modifiers[0] |= MOD_MASK_2CLICK;
                else if (orig_num_clicks == 3)
                    modifiers[0] |= MOD_MASK_3CLICK;
                else if (orig_num_clicks == 4)
                    modifiers[0] |= MOD_MASK_4CLICK;

                /* Work out our pseudo mouse event. */
                key_name.be(0, KS_EXTRA);
                if (wheel_code != 0)
                {
                    if ((wheel_code & MOUSE_CTRL) != 0)
                        modifiers[0] |= MOD_MASK_CTRL;
                    if ((wheel_code & MOUSE_ALT) != 0)
                        modifiers[0] |= MOD_MASK_ALT;
                    key_name.be(1, (wheel_code & 1) != 0 ? KE_MOUSEUP : KE_MOUSEDOWN);
                }
                else
                    key_name.be(1, get_pseudo_mouse_code(current_button, is_click, is_drag));
            }

            /*
             * Change <xHome> to <Home>, <xUp> to <Up>, etc.
             */
            int key = handle_x_keys(TERMCAP2KEY(key_name.at(0), key_name.at(1)));

            Bytes string = new Bytes(MAX_KEY_CODE_LEN + 1);
            /*
             * Add any modifier codes to our string.
             */
            int new_slen = 0;           /* length of what will replace the termcode */
            if (modifiers[0] != 0)
            {
                /* Some keys have the modifier included.
                 * Need to handle that here to make mappings work. */
                key = simplify_key(key, modifiers);
                if (modifiers[0] != 0)
                {
                    string.be(new_slen++, KB_SPECIAL);
                    string.be(new_slen++, KS_MODIFIER);
                    string.be(new_slen++, modifiers[0]);
                }
            }

            /* Finally, add the special key code to our string. */
            key_name.be(0, KEY2TERMCAP0(key));
            key_name.be(1, KEY2TERMCAP1(key));
            if (key_name.at(0) == KS_KEY)
            {
                /* from ":set <M-b>=xx" */
                new_slen += utf_char2bytes(char_u(key_name.at(1)), string.plus(new_slen));
            }
            else if (new_slen == 0 && key_name.at(0) == KS_EXTRA && key_name.at(1) == KE_IGNORE)
            {
                /* Do not put K_IGNORE into the buffer, do return KEYLEN_REMOVED to indicate what happened. */
                retval = KEYLEN_REMOVED;
            }
            else
            {
                string.be(new_slen++, KB_SPECIAL);
                string.be(new_slen++, key_name.at(0));
                string.be(new_slen++, key_name.at(1));
            }
            string.be(new_slen, NUL);

            int extra = new_slen - slen;
            if (buf == null)
            {
                if (extra < 0)
                    /* remove matched chars, taking care of noremap */
                    del_typebuf(-extra, offset);
                else if (0 < extra)
                    /* insert the extra space we need */
                    ins_typebuf(string.plus(slen), REMAP_YES, offset, false, false);

                /*
                 * Careful: del_typebuf() and ins_typebuf() may have reallocated typebuf.tb_buf[]!
                 */
                BCOPY(typebuf.tb_buf, typebuf.tb_off + offset, string, 0, new_slen);
            }
            else
            {
                if (extra < 0)
                    /* remove matched characters */
                    BCOPY(buf, offset, buf, offset - extra, buflen[0] + offset + extra);
                else if (0 < extra)
                {
                    /* Insert the extra space we need.  If there is insufficient space return -1. */
                    if (bufsize <= buflen[0] + extra + new_slen)
                        return -1;
                    BCOPY(buf, offset + extra, buf, offset, buflen[0] - offset);
                }
                BCOPY(buf, offset, string, 0, new_slen);
                buflen[0] += extra + new_slen;
            }

            return (retval != 0) ? retval : len + extra + offset;
        }

        return 0;                       /* no match found */
    }

    /*
     * Replace any terminal code strings in from[] with the equivalent internal vim representation.
     * This is used for the "from" and "to" part of a mapping, and the "to" part of a menu command.
     * Any strings like "<C-UP>" are also replaced, unless 'cpoptions' contains '<'.
     * KB_SPECIAL by itself is replaced by KB_SPECIAL KS_SPECIAL KE_FILLER.
     *
     * CTRL-V characters are removed.  When "from_part" is true, a trailing CTRL-V is included,
     * otherwise it is removed (for ":map xx ^V", maps xx to nothing).
     * When 'cpoptions' does not contain 'B', a backslash can be used instead of a CTRL-V.
     */
    /*private*/ static Bytes replace_termcodes(Bytes from, boolean from_part, boolean do_lt, boolean special)
        /* do_lt: also translate <lt> */
        /* special: always accept <key> notation */
    {
        boolean do_backslash = (vim_strbyte(p_cpo[0], CPO_BSLASH) == null);         /* backslash is special */
        boolean do_special = (vim_strbyte(p_cpo[0], CPO_SPECI) == null) || special; /* recognize <> key codes */
        boolean do_key_code = (vim_strbyte(p_cpo[0], CPO_KEYCODE) == null);         /* recognize raw key codes */

        /*
         * Allocate space for the translation.  Worst case a single character
         * is replaced by 6 bytes (shifted special key), plus a NUL at the end.
         */
        Bytes dest = new Bytes(strlen(from) * 6 + 1);

        Bytes[] src = { from };
        int dlen = 0;

        /*
         * Check for #n at start only: function key n.
         */
        if (from_part && src[0].at(0) == (byte)'#' && asc_isdigit(src[0].at(1)))  /* function key */
        {
            dest.be(dlen++, KB_SPECIAL);
            dest.be(dlen++, (byte)'k');
            if (src[0].at(1) == (byte)'0')
                dest.be(dlen++, (byte)';');         /* #0 is F10 is "k;" */
            else
                dest.be(dlen++, src[0].at(1));      /* #3 is F3 is "k3" */
            src[0] = src[0].plus(2);
        }

        /*
         * Copy each byte from *from to dest[dlen].
         */
        while (src[0].at(0) != NUL)
        {
            /*
             * If 'cpoptions' does not contain '<', check for special key codes,
             * like "<C-S-LeftMouse>".
             */
            if (do_special && (do_lt || STRNCMP(src[0], u8("<lt>"), 4) != 0))
            {
                /*
                 * Replace <SID> by K_SNR <script-nr> _.
                 * (room: 5 * 6 = 30 bytes; needed: 3 + <nr> + 1 <= 14)
                 */
                if (STRNCASECMP(src[0], u8("<SID>"), 5) == 0)
                {
                    if (current_SID <= 0)
                        emsg(e_usingsid);
                    else
                    {
                        src[0] = src[0].plus(5);
                        dest.be(dlen++, KB_SPECIAL);
                        dest.be(dlen++, KS_EXTRA);
                        dest.be(dlen++, KE_SNR);
                        libC.sprintf(dest.plus(dlen), u8("%ld"), (long)current_SID);
                        dlen += strlen(dest, dlen);
                        dest.be(dlen++, (byte)'_');
                        continue;
                    }
                }

                int slen = trans_special(src, dest.plus(dlen), true);
                if (slen != 0)
                {
                    dlen += slen;
                    continue;
                }
            }

            /*
             * If 'cpoptions' does not contain 'k', see if it's an actual key-code.
             * Note that this is also checked after replacing the <> form.
             * Single character codes are NOT replaced (e.g. ^H or DEL), because
             * it could be a character in the file.
             */
            if (do_key_code)
            {
                int i = find_term_bykeys(src[0]);
                if (0 <= i)
                {
                    dest.be(dlen++, KB_SPECIAL);
                    dest.be(dlen++, termcodes[i].name.at(0));
                    dest.be(dlen++, termcodes[i].name.at(1));
                    src[0] = src[0].plus(termcodes[i].len);
                    /* If terminal code matched, continue after it. */
                    continue;
                }
            }

            if (do_special)
            {
                Bytes p = null;
                int n = 0;

                /*
                 * Replace <Leader> by the value of "mapleader".
                 * Replace <LocalLeader> by the value of "maplocalleader".
                 * If "mapleader" or "maplocalleader" isn't set use a backslash.
                 */
                if (STRNCASECMP(src[0], u8("<Leader>"), 8) == 0)
                {
                    p = get_var_value(u8("g:mapleader"));
                    n = 8;
                }
                else if (STRNCASECMP(src[0], u8("<LocalLeader>"), 13) == 0)
                {
                    p = get_var_value(u8("g:maplocalleader"));
                    n = 13;
                }

                if (n != 0)
                {
                    /* Allow up to 8 * 6 characters for "mapleader". */
                    if (p == null || p.at(0) == NUL || 8 * 6 < strlen(p))
                        p = u8("\\");
                    while (p.at(0) != NUL)
                        dest.be(dlen++, (p = p.plus(1)).at(-1));
                    src[0] = src[0].plus(n);
                    continue;
                }
            }

            /*
             * Remove CTRL-V and ignore the next character.
             * For "from" side the CTRL-V at the end is included, for the "to" part it is removed.
             * If 'cpoptions' does not contain 'B', also accept a backslash.
             */
            byte key = src[0].at(0);
            if (key == Ctrl_V || (do_backslash && key == '\\'))
            {
                src[0] = src[0].plus(1);                              /* skip CTRL-V or backslash */
                if (src[0].at(0) == NUL)
                {
                    if (from_part)
                        dest.be(dlen++, key);
                    break;
                }
            }

            /* skip multibyte char correctly */
            for (int i = 0, n = us_ptr2len_cc(src[0]); i < n; i++)
            {
                /*
                 * If the character is KB_SPECIAL, replace it with KB_SPECIAL KS_SPECIAL KE_FILLER.
                 * If compiled with the GUI replace CSI with K_CSI.
                 */
                if (src[0].at(i) == KB_SPECIAL)
                {
                    dest.be(dlen++, KB_SPECIAL);
                    dest.be(dlen++, KS_SPECIAL);
                    dest.be(dlen++, KE_FILLER);
                }
                else
                    dest.be(dlen++, src[0].at(i));
            }
        }
        dest.be(dlen, NUL);

        /*
         * Copy the new string to allocated memory.
         */
        return STRDUP(dest);
    }

    /*
     * Find a termcode with keys 'src' (must be NUL terminated).
     * Return the index in termcodes[], or -1 if not found.
     */
    /*private*/ static int find_term_bykeys(Bytes src)
    {
        int slen = strlen(src);

        for (int i = 0; i < tc_len; i++)
        {
            if (slen == termcodes[i].len && STRNCMP(termcodes[i].code, src, slen) == 0)
                return i;
        }
        return -1;
    }

    /*
     * Gather the first characters in the terminal key codes into a string.
     * Used to speed up check_termcode().
     */
    /*private*/ static void gather_termleader()
    {
        int len = 0;

        if (check_for_codes)
            termleader.be(len++, DCS);    /* the termcode response starts with DCS in 8-bit mode */
        termleader.be(len, NUL);

        for (int i = 0; i < tc_len; i++)
            if (vim_strchr(termleader, termcodes[i].code.at(0)) == null)
            {
                termleader.be(len++, termcodes[i].code.at(0));
                termleader.be(len, NUL);
            }

        need_gather = false;
    }

    /*
     * Show all termcodes (for ":set termcap").
     * This code looks a lot like showoptions(), but is different.
     */
    /*private*/ static void show_termcodes()
    {
        if (tc_len == 0)        /* no terminal codes (must be GUI) */
            return;

        final int INC3 = 27;    /* try to make three columns */
        final int INC2 = 40;    /* try to make two columns */
        final int GAP2 = 2;     /* spaces between columns */

        int[] items = new int[tc_len];

        /* Highlight title. */
        msg_puts_title(u8("\n--- Terminal keys ---"));

        /*
         * do the loop two times:
         * 1. display the short items (non-strings and short strings)
         * 2. display the medium items (medium length strings)
         * 3. display the long items (remaining strings)
         */
        for (int run = 1; run <= 3 && !got_int; run++)
        {
            /*
             * collect the items in items[]
             */
            int n = 0;
            for (int i = 0; i < tc_len; i++)
            {
                int len = show_one_termcode(termcodes[i].name, termcodes[i].code, false);
                if (len <= INC3 - GAP2 ? run == 1 : len <= INC2 - GAP2 ? run == 2 : run == 3)
                    items[n++] = i;
            }

            /*
             * display the items
             */
            int rows;
            if (run <= 2)
            {
                int cols = ((int)Columns[0] + GAP2) / (run == 1 ? INC3 : INC2);
                if (cols == 0)
                    cols = 1;
                rows = (n + cols - 1) / cols;
            }
            else    /* run == 3 */
                rows = n;

            for (int row = 0; row < rows && !got_int; row++)
            {
                msg_putchar('\n');                  /* go to next line */
                if (got_int)                        /* 'q' typed in more */
                    break;

                for (int i = row, col = 0; i < n; i += rows, col += (run == 2) ? INC2 : INC3)
                {
                    msg_col = col;                  /* make columns */
                    show_one_termcode(termcodes[items[i]].name, termcodes[items[i]].code, true);
                }
                out_flush();
                ui_breakcheck();
            }
        }
    }

    /*
     * Show one termcode entry.
     * Output goes into ioBuff[].
     */
    /*private*/ static int show_one_termcode(Bytes name, Bytes code, boolean printit)
    {
        if ('~' < char_u(name.at(0)))
        {
            ioBuff.be(0, (byte)' ');
            ioBuff.be(1, (byte)' ');
            ioBuff.be(2, (byte)' ');
            ioBuff.be(3, (byte)' ');
        }
        else
        {
            ioBuff.be(0, (byte)'t');
            ioBuff.be(1, (byte)'_');
            ioBuff.be(2, name.at(0));
            ioBuff.be(3, name.at(1));
        }
        ioBuff.be(4, (byte)' ');

        Bytes p = get_special_key_name(TERMCAP2KEY(name.at(0), name.at(1)), 0);
        if (p.at(1) != (byte)'t')
            STRCPY(ioBuff.plus(5), p);
        else
            ioBuff.be(5, NUL);

        int len = strlen(ioBuff);
        do
        {
            ioBuff.be(len++, (byte)' ');
        } while (len < 17);
        ioBuff.be(len, NUL);
        len += (code != null) ? mb_string2cells(code, -1) : 4;

        if (printit)
        {
            msg_puts(ioBuff);
            if (code != null)
                msg_outtrans(code);
            else
                msg_puts(u8("NULL"));
        }

        return len;
    }

    /*
     * For Xterm >= 140 compiled with OPT_TCAP_QUERY:
     * obtain the actually used termcap codes from the terminal itself.
     * We get them one by one to avoid a very long response string.
     */
    /*private*/ static int xt_index_in;
    /*private*/ static int xt_index_out;

    /*private*/ static void req_codes_from_term()
    {
        xt_index_in = 0;
        xt_index_out = 0;
        req_more_codes_from_term();
    }

    /*private*/ static void req_more_codes_from_term()
    {
        /* Don't do anything when going to exit. */
        if (exiting)
            return;

        int old_idx = xt_index_out;

        /* Send up to 10 more requests out than we received.
         * Avoid sending too many, there can be a buffer overflow somewhere. */
        for ( ; xt_index_out < xt_index_in + 10 && key_names[xt_index_out] != null; xt_index_out++)
        {
            Bytes buf = new Bytes(10 + 1);

            libC.sprintf(buf, u8("\033P+q%02x%02x\033\\"), key_names[xt_index_out].at(0), key_names[xt_index_out].at(1));
            out_str_nf(buf);
        }

        /* Send the codes out right away. */
        if (xt_index_out != old_idx)
            out_flush();
    }

    /*
     * Decode key code response from xterm: '<Esc>P1+r<name>=<string><Esc>\'.
     * A "0" instead of the "1" indicates a code that isn't supported.
     * Both <name> and <string> are encoded in hex.
     * "code" points to the "0" or "1".
     */
    /*private*/ static void got_code_from_term(Bytes code, int len)
    {
        final int XT_LEN = 100;
        Bytes str = new Bytes(XT_LEN);

        /* A '1' means the code is supported, a '0' means it isn't.
         * When half the length is > XT_LEN we can't use it.
         * Our names are currently all 2 characters. */
        if (code.at(0) == (byte)'1' && code.at(7) == (byte)'=' && len / 2 < XT_LEN)
        {
            Bytes name = new Bytes(3);

            /* Get the name from the response and find it in the table. */
            name.be(0, hexhex2nr(code.plus(3)));
            name.be(1, hexhex2nr(code.plus(5)));
            name.be(2, NUL);

            int k;
            for (k = 0; key_names[k] != null; k++)
                if (STRCMP(key_names[k], name) == 0)
                {
                    xt_index_in = k;
                    break;
                }

            if (key_names[k] != null)
            {
                int j = 0;
                for (int i = 8; ; i += 2)
                {
                    int x = hexhex2nr(code.plus(i));
                    if (x < 0)
                        break;
                    str.be(j++, x);
                }
                str.be(j, NUL);

                if (name.at(0) == (byte)'C' && name.at(1) == (byte)'o')
                {
                    /* Color count is not a key code. */
                    int i = libC.atoi(str);
                    if (i != t_colors)
                    {
                        /* Nr of colors changed, initialize highlighting and redraw everything.
                         * This causes a redraw, which usually clears the message.
                         * Try keeping the message if it might work. */
                        set_keep_msg_from_hist();
                        set_color_count(i);
                        init_highlight(true, false);
                        redraw_asap(CLEAR);
                    }
                }
                else
                {
                    /* First delete any existing entry with the same code. */
                    int i = find_term_bykeys(str);
                    if (0 <= i)
                        del_termcode_idx(i);
                    add_termcode(name, str, ATC_FROM_TERM);
                }
            }
        }

        /* May request more codes now that we received one. */
        xt_index_in++;
        req_more_codes_from_term();
    }

    /*
     * Check if there are any unanswered requests and deal with them.
     * This is called before starting an external program or getting direct keyboard input.
     * We don't want responses to be send to that program or handled as typed text.
     */
    /*private*/ static void check_for_codes_from_term()
    {
        /* If no codes requested or all are answered, no need to wait. */
        if (xt_index_out == 0 || xt_index_out == xt_index_in)
            return;

        /* vgetc() will check for and handle any response.
         * Keep calling vpeekc() until we don't get any responses. */
        no_mapping++;
        allow_keys++;
        for ( ; ; )
        {
            int c = vpeekc();
            if (c == NUL)       /* nothing available */
                break;
            /*
             * If a response is recognized it's replaced with K_IGNORE, must read it from the input stream.
             * If there is no K_IGNORE we can't do anything, break here
             * (there might be some responses further on, but we don't want to throw away any typed chars).
             */
            if (c != char_u(KB_SPECIAL) && c != K_IGNORE)
                break;
            c = vgetc();
            if (c != K_IGNORE)
            {
                vungetc(c);
                break;
            }
        }
        --no_mapping;
        --allow_keys;
    }

    /*
     * Translate an internal mapping/abbreviation representation into the
     * corresponding external one recognized by :map/:abbrev commands;
     * respects the current B/k/< settings of 'cpoption'.
     *
     * This function is called when expanding mappings/abbreviations on the
     * command-line, and for building the "Ambiguous mapping..." error message.
     *
     * It uses a growarray to build the translation string since the latter
     * can be wider than the original description.  The caller has to free
     * the string afterwards.
     *
     * Returns null when there is a problem.
     */
    /*private*/ static Bytes translate_mapping(Bytes s)
    {
        barray_C ba = new barray_C(40);

        boolean cpo_bslash = (vim_strbyte(p_cpo[0], CPO_BSLASH) != null);
        boolean cpo_special = (vim_strbyte(p_cpo[0], CPO_SPECI) != null);
        boolean cpo_keycode = (vim_strbyte(p_cpo[0], CPO_KEYCODE) == null);

        for ( ; s.at(0) != NUL; s = s.plus(1))
        {
            int c = char_u(s.at(0));
            if (c == char_u(KB_SPECIAL) && s.at(1) != NUL && s.at(2) != NUL)
            {
                int modifiers = 0;
                if (s.at(1) == KS_MODIFIER)
                {
                    s = s.plus(1);
                    modifiers = char_u((s = s.plus(1)).at(0));
                    c = char_u((s = s.plus(1)).at(0));
                }
                if (cpo_special && cpo_keycode && c == char_u(KB_SPECIAL) && modifiers == 0)
                {
                    /* try to find special key in termcodes */
                    int i;
                    for (i = 0; i < tc_len; i++)
                        if (termcodes[i].name.at(0) == s.at(1) && termcodes[i].name.at(1) == s.at(2))
                            break;
                    if (i < tc_len)
                    {
                        ba_concat(ba, termcodes[i].code);
                        s = s.plus(2);
                        continue;
                    }
                }
                if (c == char_u(KB_SPECIAL) && s.at(1) != NUL && s.at(2) != NUL)
                {
                    if (cpo_special)
                    {
                        ba_clear(ba);
                        return null;
                    }
                    c = toSpecial(s.at(1), s.at(2));
                    if (c == K_ZERO)                /* display <Nul> as ^@ */
                        c = NUL;
                    s = s.plus(2);
                }
                if (is_special(c) || modifiers != 0)    /* special key */
                {
                    if (cpo_special)
                    {
                        ba_clear(ba);
                        return null;
                    }
                    ba_concat(ba, get_special_key_name(c, modifiers));
                    continue;
                }
            }
            if (c == ' ' || c == '\t' || c == Ctrl_J || c == Ctrl_V
                    || (c == '<' && !cpo_special)
                    || (c == '\\' && !cpo_bslash))
                ba_append(ba, cpo_bslash ? Ctrl_V : (byte)'\\');
            if (c != NUL)
                ba_append(ba, (byte)c);
        }

        ba_append(ba, NUL);
        return new Bytes(ba.ba_data);
    }

    /*
     * ui.c: functions that handle the user interface.
     * 1. Keyboard input stuff, and a bit of windowing stuff.  These are called
     *    before the machine specific stuff (mch_*) so that we can call the GUI
     *    stuff instead if the GUI is running.
     * 2. Clipboard stuff.
     * 3. Input buffer stuff.
     */

    /*private*/ static void ui_write(Bytes s, int len)
    {
        /* Don't output anything in silent mode ("ex -s") unless 'verbose' set. */
        if (!(silent_mode && p_verbose[0] == 0))
        {
            mch_write(s, len);
        }
    }

    /*
     * ui_inchar(): low level input function.
     * Get characters from the keyboard.
     * Return the number of characters that are available.
     * If "wtime" == 0 do not wait for characters.
     * If "wtime" == -1 wait forever for characters.
     * If "wtime" > 0 wait "wtime" milliseconds for a character.
     *
     * "tb_change_cnt" is the value of typebuf.tb_change_cnt if "buf" points into it.
     * When typebuf.tb_change_cnt changes (e.g., when a message is received from a remote client)
     * "buf" can no longer be used.  "tb_change_cnt" is null otherwise.
     */
    /*private*/ static int ui_inchar(Bytes buf, int maxlen, long wtime, int tb_change_cnt)
        /* wtime: don't use "time", MIPS cannot handle it */
    {
        /* If we are going to wait for some time or block... */
        if (wtime == -1 || 100L < wtime)
        {
            /* ... allow signals to kill us. */
            vim_handle_signal(SIGNAL_UNBLOCK);

            /* ... there is no need for CTRL-C to interrupt something,
             * don't let it set got_int when it was mapped. */
            if (((mapped_ctrl_c | curbuf.b_mapped_ctrl_c) & get_real_state()) != 0)
                ctrl_c_interrupts = false;
        }

        int len = mch_inchar(buf, maxlen, wtime, tb_change_cnt);

        if (wtime == -1 || 100L < wtime)
            /* block SIGHUP et al. */
            vim_handle_signal(SIGNAL_BLOCK);

        ctrl_c_interrupts = true;

        return len;
    }

    /*
     * Delay for the given number of milliseconds.
     * If ignoreinput is false then we cancel the delay if a key is hit.
     */
    /*private*/ static void ui_delay(long msec, boolean ignoreinput)
    {
        mch_delay(msec, ignoreinput);
    }

    /*
     * If the machine has job control, use it to suspend the program,
     * otherwise fake it by starting a new shell.
     * When running the GUI iconify the window.
     */
    /*private*/ static void ui_suspend()
    {
        mch_suspend();
    }

    /*
     * Try to get the current Vim shell size.
     * Put the result in Rows and Columns.
     * Use the new sizes as defaults for 'columns' and 'lines'.
     * Return true when size could be determined, false otherwise.
     */
    /*private*/ static boolean ui_get_shellsize()
    {
        boolean got = mch_get_shellsize();

        check_shellsize();

        /* adjust the default for 'lines' and 'columns' */
        if (got)
        {
            set_number_default(u8("lines"), Rows[0]);
            set_number_default(u8("columns"), Columns[0]);
        }

        return got;
    }

    /*
     * Set the size of the Vim shell according to Rows and Columns, if possible.
     * The gui_set_shellsize() or mch_set_shellsize() function will try to set the new size.
     * If this is not possible, it will adjust Rows and Columns.
     */
    /*private*/ static void ui_set_shellsize(boolean _mustset)
        /* mustset: set by the user */
    {
        mch_set_shellsize();
    }

    /*
     * Called when Rows and/or Columns changed.  Adjust scroll region and mouse region.
     */
    /*private*/ static void ui_new_shellsize()
    {
        if (full_screen && !exiting)
            mch_new_shellsize();
    }

    /*private*/ static void ui_breakcheck()
    {
        mch_breakcheck();
    }

    /*****************************************************************************
     * Functions for copying and pasting text between applications.
     *
     * This is always included in a GUI version, but may also be included when
     * the clipboard and mouse is available to a terminal version such as xterm.
     * Note: there are some more functions in ops.c that handle selection stuff.
     *
     * Also note that the majority of functions here deal with the X 'primary'
     * (visible - for Visual mode use) selection, and only that.  There are no versions
     * of these for the 'clipboard' selection, as Visual mode has no use for them.
     */

    /*
     * Selection stuff using Visual mode, for cutting and pasting text to other windows.
     */

    /*
     * Call this to initialise the clipboard.  Pass it false if the clipboard code
     * is included, but the clipboard can not be used, or true if the clipboard can be used.
     * Eg unix may call this with false, then call it again with true if the GUI starts.
     */
    /*private*/ static void clip_init(boolean can_use)
    {
        for (clipboard_C cbd = clip_star; ; )
        {
            cbd.available  = can_use;
            cbd.owned      = false;
            cbd.cbd_start.lnum = 0;
            cbd.cbd_start.col  = 0;
            cbd.cbd_end.lnum   = 0;
            cbd.cbd_end.col    = 0;
            cbd.state      = SELECT_CLEARED;

            if (cbd == clip_plus)
                break;
            cbd = clip_plus;
        }
    }

    /*
     * Check whether the VIsual area has changed, and if so try to become the owner
     * of the selection, and free any old converted selection we may still have
     * lying around.  If the VIsual mode has ended, make a copy of what was
     * selected so we can still give it to others.  Will probably have to make sure
     * this is called whenever VIsual mode is ended.
     */
    /*private*/ static void clip_update_selection(clipboard_C cbd)
    {
        /* If visual mode is only due to a redo command ("."), then ignore it. */
        if (!redo_VIsual_busy && VIsual_active && (State & NORMAL) != 0)
        {
            pos_C start = new pos_C();
            pos_C end = new pos_C();
            if (ltpos(VIsual, curwin.w_cursor))
            {
                COPY_pos(start, VIsual);
                COPY_pos(end, curwin.w_cursor);
                end.col += us_ptr2len_cc(ml_get_cursor()) - 1;
            }
            else
            {
                COPY_pos(start, curwin.w_cursor);
                COPY_pos(end, VIsual);
            }

            if (!eqpos(cbd.cbd_start, start) || !eqpos(cbd.cbd_end, end) || cbd.vmode != VIsual_mode)
            {
                clip_clear_selection(cbd);
                COPY_pos(cbd.cbd_start, start);
                COPY_pos(cbd.cbd_end, end);
                cbd.vmode = VIsual_mode;
                clip_free_selection(cbd);
                clip_own_selection(cbd);
                clip_gen_set_selection(cbd);
            }
        }
    }

    /*private*/ static void clip_own_selection(clipboard_C cbd)
    {
        /*
         * Also want to check somehow that we are reading from the keyboard rather than a mapping etc.
         */
        /* Only own the clipboard when we didn't own it yet. */
        if (!cbd.owned && cbd.available)
            cbd.owned = (clip_gen_own_selection(cbd) == true);
    }

    /*private*/ static void clip_lose_selection(clipboard_C cbd)
    {
        boolean visual_selection = false;

        if (cbd == clip_star || cbd == clip_plus)
            visual_selection = true;

        clip_free_selection(cbd);
        cbd.owned = false;
        if (visual_selection)
            clip_clear_selection(cbd);
        clip_gen_lose_selection(cbd);
    }

    /*private*/ static void clip_copy_selection(clipboard_C cbd)
    {
        if (VIsual_active && (State & NORMAL) != 0 && cbd.available)
        {
            clip_update_selection(cbd);
            clip_free_selection(cbd);
            clip_own_selection(cbd);
            if (cbd.owned)
                clip_get_selection(cbd);
            clip_gen_set_selection(cbd);
        }
    }

    /*
     * Save and restore "clip_unnamed" before doing possibly many changes.
     * This prevents accessing the clipboard very often which might slow down Vim considerably.
     */
    /*private*/ static int global_change_count;         /* if set, inside a start_global_changes */
    /*private*/ static boolean clipboard_needs_update;  /* clipboard needs to be updated */

    /*
     * Save "clip_unnamed" and reset it.
     */
    /*private*/ static void start_global_changes()
    {
        if (1 < ++global_change_count)
            return;

        clip_unnamed_saved = clip_unnamed;
        clipboard_needs_update = false;

        if (clip_did_set_selection)
        {
            clip_unnamed = 0;
            clip_did_set_selection = false;
        }
    }

    /*
     * Restore "clip_unnamed" and set the selection when needed.
     */
    /*private*/ static void end_global_changes()
    {
        if (0 < --global_change_count)  /* recursive */
            return;

        if (!clip_did_set_selection)
        {
            clip_did_set_selection = true;
            clip_unnamed = clip_unnamed_saved;
            clip_unnamed_saved = 0;
            if (clipboard_needs_update)
            {
                /* only store something in the clipboard,
                 * if we have yanked anything to it */
                if ((clip_unnamed & CLIP_UNNAMED) != 0)
                {
                    clip_own_selection(clip_star);
                    clip_gen_set_selection(clip_star);
                }
                if ((clip_unnamed & CLIP_UNNAMED_PLUS) != 0)
                {
                    clip_own_selection(clip_plus);
                    clip_gen_set_selection(clip_plus);
                }
            }
        }
    }

    /*
     * Called when Visual mode is ended: update the selection.
     */
    /*private*/ static void clip_auto_select()
    {
        if (clip_isautosel_star())
            clip_copy_selection(clip_star);
        if (clip_isautosel_plus())
            clip_copy_selection(clip_plus);
    }

    /*
     * Return true if automatic selection of Visual area is desired for the * register.
     */
    /*private*/ static boolean clip_isautosel_star()
    {
        return clip_autoselect_star;
    }

    /*
     * Return true if automatic selection of Visual area is desired for the + register.
     */
    /*private*/ static boolean clip_isautosel_plus()
    {
        return clip_autoselect_plus;
    }

    /*
     * Stuff for general mouse selection, without using Visual mode.
     */

    /* flags for clip_invert_area() */
    /*private*/ static final int CLIP_CLEAR      = 1;
    /*private*/ static final int CLIP_SET        = 2;
    /*private*/ static final int CLIP_TOGGLE     = 3;

    /*
     * Start, continue or end a modeless selection.
     * Used when editing the command-line and in the cmdline window.
     */
    /*private*/ static void clip_modeless(int button, boolean is_click, boolean is_drag)
    {
        boolean repeat = ((clip_star.mode == SELECT_MODE_CHAR || clip_star.mode == SELECT_MODE_LINE)
                                                    && (mod_mask & MOD_MASK_2CLICK) != 0)
                       || (clip_star.mode == SELECT_MODE_WORD
                                                    && (mod_mask & MOD_MASK_3CLICK) != 0);
        if (is_click && button == MOUSE_RIGHT)
        {
            /* Right mouse button: if there was no selection, start one;
             * otherwise extend the existing selection. */
            if (clip_star.state == SELECT_CLEARED)
                clip_start_selection(mouse_col, mouse_row, false);
            clip_process_selection(button, mouse_col, mouse_row, repeat);
        }
        else if (is_click)
            clip_start_selection(mouse_col, mouse_row, repeat);
        else if (is_drag)
        {
            /* Don't try extending a selection if there isn't one.  Happens when
             * button-down is in the cmdline and them moving mouse upwards. */
            if (clip_star.state != SELECT_CLEARED)
                clip_process_selection(button, mouse_col, mouse_row, repeat);
        }
        else /* release */
            clip_process_selection(MOUSE_RELEASE, mouse_col, mouse_row, false);
    }

    /*
     * Compare two screen positions ala strcmp()
     */
    /*private*/ static int clip_compare_pos(int row1, int col1, int row2, int col2)
    {
        if (row1 > row2) return 1;
        if (row1 < row2) return -1;
        if (col1 > col2) return 1;
        if (col1 < col2) return -1;

        return 0;
    }

    /*
     * Start the selection
     */
    /*private*/ static void clip_start_selection(int col, int row, boolean repeated_click)
    {
        clipboard_C cbd = clip_star;

        if (cbd.state == SELECT_DONE)
            clip_clear_selection(cbd);

        row = check_row(row);
        col = check_col(col);
        col = mb_fix_col(col, row);

        cbd.cbd_start.lnum = row;
        cbd.cbd_start.col = col;
        COPY_pos(cbd.cbd_end, cbd.cbd_start);
        cbd.origin_row = (int)cbd.cbd_start.lnum;
        cbd.state = SELECT_IN_PROGRESS;

        if (repeated_click)
        {
            if (SELECT_MODE_LINE < ++cbd.mode)
                cbd.mode = SELECT_MODE_CHAR;
        }
        else
            cbd.mode = SELECT_MODE_CHAR;

        switch (cbd.mode)
        {
            case SELECT_MODE_CHAR:
                cbd.origin_start_col = cbd.cbd_start.col;
                cbd.word_end_col = clip_get_line_end((int)cbd.cbd_start.lnum);
                break;

            case SELECT_MODE_WORD:
                clip_get_word_boundaries(cbd, (int)cbd.cbd_start.lnum, cbd.cbd_start.col);
                cbd.origin_start_col = cbd.word_start_col;
                cbd.origin_end_col   = cbd.word_end_col;

                clip_invert_area((int)cbd.cbd_start.lnum, cbd.word_start_col,
                                 (int)cbd.cbd_end.lnum, cbd.word_end_col, CLIP_SET);
                cbd.cbd_start.col = cbd.word_start_col;
                cbd.cbd_end.col   = cbd.word_end_col;
                break;

            case SELECT_MODE_LINE:
                clip_invert_area((int)cbd.cbd_start.lnum, 0,
                                 (int)cbd.cbd_start.lnum, (int)Columns[0], CLIP_SET);
                cbd.cbd_start.col = 0;
                cbd.cbd_end.col   = (int)Columns[0];
                break;
        }

        COPY_pos(cbd.cbd_prev, cbd.cbd_start);
    }

    /*
     * Continue processing the selection
     */
    /*private*/ static void clip_process_selection(int button, int col, int row, boolean repeated_click)
    {
        clipboard_C cbd = clip_star;

        if (button == MOUSE_RELEASE)
        {
            /* Check to make sure we have something selected. */
            if (cbd.cbd_start.lnum == cbd.cbd_end.lnum && cbd.cbd_start.col == cbd.cbd_end.col)
            {
                cbd.state = SELECT_CLEARED;
                return;
            }

            if (clip_isautosel_star() || (clip_autoselectml))
                clip_copy_modeless_selection(false);

            cbd.state = SELECT_DONE;
            return;
        }

        row = check_row(row);
        col = check_col(col);
        col = mb_fix_col(col, row);

        if (col == cbd.cbd_prev.col && row == cbd.cbd_prev.lnum && !repeated_click)
            return;

        /*
         * When extending the selection with the right mouse button, swap the
         * start and end if the position is before half the selection
         */
        if (cbd.state == SELECT_DONE && button == MOUSE_RIGHT)
        {
            /*
             * If the click is before the start, or the click is inside the
             * selection and the start is the closest side, set the origin to the
             * end of the selection.
             */
            int diff;
            if (clip_compare_pos(row, col, (int)cbd.cbd_start.lnum, cbd.cbd_start.col) < 0
                    || (clip_compare_pos(row, col, (int)cbd.cbd_end.lnum, cbd.cbd_end.col) < 0
                        && (((cbd.cbd_start.lnum == cbd.cbd_end.lnum
                                && col - cbd.cbd_start.col < cbd.cbd_end.col - col))
                            || (0 < (diff = ((int)cbd.cbd_end.lnum - row) - (row - (int)cbd.cbd_start.lnum))
                                    || (diff == 0 && col < (cbd.cbd_start.col + cbd.cbd_end.col) / 2)))))
            {
                cbd.origin_row = (int)cbd.cbd_end.lnum;
                cbd.origin_start_col = cbd.cbd_end.col - 1;
                cbd.origin_end_col = cbd.cbd_end.col;
            }
            else
            {
                cbd.origin_row = (int)cbd.cbd_start.lnum;
                cbd.origin_start_col = cbd.cbd_start.col;
                cbd.origin_end_col = cbd.cbd_start.col;
            }
            if (cbd.mode == SELECT_MODE_WORD && !repeated_click)
                cbd.mode = SELECT_MODE_CHAR;
        }

        /* set state, for when using the right mouse button */
        cbd.state = SELECT_IN_PROGRESS;

        if (repeated_click && SELECT_MODE_LINE < ++cbd.mode)
            cbd.mode = SELECT_MODE_CHAR;

        switch (cbd.mode)
        {
            case SELECT_MODE_CHAR:
                /* If we're on a different line, find where the line ends. */
                if (row != cbd.cbd_prev.lnum)
                    cbd.word_end_col = clip_get_line_end(row);

                /* See if we are before or after the origin of the selection. */
                if (0 <= clip_compare_pos(row, col, cbd.origin_row, cbd.origin_start_col))
                {
                    if (cbd.word_end_col <= col)
                        clip_update_modeless_selection(cbd,
                            cbd.origin_row, cbd.origin_start_col, row, (int)Columns[0]);
                    else
                    {
                        int slen = 1;       /* cursor shape width */
                        if (mb_lefthalve(row, col))
                            slen = 2;
                        clip_update_modeless_selection(cbd,
                            cbd.origin_row, cbd.origin_start_col, row, col + slen);
                    }
                }
                else
                {
                    int slen = 1;       /* cursor shape width */
                    if (mb_lefthalve(cbd.origin_row, cbd.origin_start_col))
                        slen = 2;
                    if (cbd.word_end_col <= col)
                        clip_update_modeless_selection(cbd,
                            row, cbd.word_end_col, cbd.origin_row, cbd.origin_start_col + slen);
                    else
                        clip_update_modeless_selection(cbd,
                            row, col, cbd.origin_row, cbd.origin_start_col + slen);
                }
                break;

            case SELECT_MODE_WORD:
                /* If we are still within the same word, do nothing. */
                if (row == cbd.cbd_prev.lnum
                        && cbd.word_start_col <= col && col < cbd.word_end_col && !repeated_click)
                    return;

                /* Get new word boundaries. */
                clip_get_word_boundaries(cbd, row, col);

                /* Handle being after the origin point of selection. */
                if (0 <= clip_compare_pos(row, col, cbd.origin_row, cbd.origin_start_col))
                    clip_update_modeless_selection(cbd,
                        cbd.origin_row, cbd.origin_start_col, row, cbd.word_end_col);
                else
                    clip_update_modeless_selection(cbd,
                        row, cbd.word_start_col, cbd.origin_row, cbd.origin_end_col);
                break;

            case SELECT_MODE_LINE:
                if (row == cbd.cbd_prev.lnum && !repeated_click)
                    return;

                if (0 <= clip_compare_pos(row, col, cbd.origin_row, cbd.origin_start_col))
                    clip_update_modeless_selection(cbd, cbd.origin_row, 0, row, (int)Columns[0]);
                else
                    clip_update_modeless_selection(cbd, row, 0, cbd.origin_row, (int)Columns[0]);
                break;
        }

        cbd.cbd_prev.lnum = row;
        cbd.cbd_prev.col  = col;
    }

    /*
     * Called from outside to clear selected region from the display
     */
    /*private*/ static void clip_clear_selection(clipboard_C cbd)
    {
        if (cbd.state == SELECT_CLEARED)
            return;

        clip_invert_area((int)cbd.cbd_start.lnum, cbd.cbd_start.col, (int)cbd.cbd_end.lnum, cbd.cbd_end.col, CLIP_CLEAR);
        cbd.state = SELECT_CLEARED;
    }

    /*
     * Clear the selection if any lines from "row1" to "row2" are inside of it.
     */
    /*private*/ static void clip_may_clear_selection(int row1, int row2)
    {
        if (clip_star.state == SELECT_DONE && clip_star.cbd_start.lnum <= row2 && row1 <= clip_star.cbd_end.lnum)
            clip_clear_selection(clip_star);
    }

    /*
     * Called before the screen is scrolled up or down.  Adjusts the line numbers
     * of the selection.  Call with big number when clearing the screen.
     */
    /*private*/ static void clip_scroll_selection(int rows)
        /* rows: negative for scroll down */
    {
        if (clip_star.state == SELECT_CLEARED)
            return;

        long lnum = clip_star.cbd_start.lnum - rows;
        if (lnum <= 0)
            clip_star.cbd_start.lnum = 0;
        else if (screenRows <= lnum)                /* scrolled off of the screen */
            clip_star.state = SELECT_CLEARED;
        else
            clip_star.cbd_start.lnum = lnum;

        lnum = clip_star.cbd_end.lnum - rows;
        if (lnum < 0)                               /* scrolled off of the screen */
            clip_star.state = SELECT_CLEARED;
        else if (screenRows <= lnum)
            clip_star.cbd_end.lnum = screenRows - 1;
        else
            clip_star.cbd_end.lnum = lnum;
    }

    /*
     * Invert a region of the display between a starting and ending row and column
     * Values for "how":
     * CLIP_CLEAR:  undo inversion
     * CLIP_SET:    set inversion
     * CLIP_TOGGLE: set inversion if pos1 < pos2, undo inversion otherwise.
     * 0: invert (GUI only).
     */
    /*private*/ static void clip_invert_area(int row1, int col1, int row2, int col2, int how)
    {
        boolean invert = false;

        if (how == CLIP_SET)
            invert = true;

        /* Swap the from and to positions so the from is always before. */
        if (0 < clip_compare_pos(row1, col1, row2, col2))
        {
            int _row, _col;

            _row = row1;
            _col = col1;
            row1 = row2;
            col1 = col2;
            row2 = _row;
            col2 = _col;
        }
        else if (how == CLIP_TOGGLE)
            invert = true;

        /* If all on the same line, do it the easy way. */
        if (row1 == row2)
        {
            clip_invert_rectangle(row1, col1, 1, col2 - col1, invert);
        }
        else
        {
            /* Handle a piece of the first line. */
            if (0 < col1)
            {
                clip_invert_rectangle(row1, col1, 1, (int)Columns[0] - col1, invert);
                row1++;
            }

            /* Handle a piece of the last line. */
            if (col2 < (int)Columns[0] - 1)
            {
                clip_invert_rectangle(row2, 0, 1, col2, invert);
                row2--;
            }

            /* Handle the rectangle thats left. */
            if (row1 <= row2)
                clip_invert_rectangle(row1, 0, row2 - row1 + 1, (int)Columns[0], invert);
        }
    }

    /*
     * Invert or un-invert a rectangle of the screen.
     * "invert" is true if the result is inverted.
     */
    /*private*/ static void clip_invert_rectangle(int row, int col, int height, int width, boolean invert)
    {
        screen_draw_rectangle(row, col, height, width, invert);
    }

    /*
     * Copy the currently selected area into the '*' register so it will be available for pasting.
     * When "both" is true also copy to the '+' register.
     */
    /*private*/ static void clip_copy_modeless_selection(boolean _both)
    {
        /* Can't use "screenLines" unless initialized. */
        if (screenLines == null)
            return;

        int row1 = (int)clip_star.cbd_start.lnum;
        int col1 = clip_star.cbd_start.col;
        int row2 = (int)clip_star.cbd_end.lnum;
        int col2 = clip_star.cbd_end.col;

        /*
         * Make sure row1 <= row2, and if row1 == row2 that col1 <= col2.
         */
        int row;
        if (row2 < row1)
        {
            row = row1; row1 = row2; row2 = row;
            row = col1; col1 = col2; col2 = row;
        }
        else if (row1 == row2 && col2 < col1)
        {
            row = col1; col1 = col2; col2 = row;
        }
        /* Correct starting point for being on right halve of double-wide char. */
        Bytes p = screenLines.plus(lineOffset[row1]);
        if (p.at(col1) == 0)
            --col1;

        /* Create a temporary buffer for storing the text. */
        int len = (row2 - row1 + 1) * (int)Columns[0] + 1;
        len *= MB_MAXBYTES;
        Bytes buffer = new Bytes(len);

        boolean add_newline_flag = false;

        /* Process each row in the selection. */
        Bytes bufp;
        for (bufp = buffer, row = row1; row <= row2; row++)
        {
            int start_col;
            if (row == row1)
                start_col = col1;
            else
                start_col = 0;

            int end_col;
            if (row == row2)
                end_col = col2;
            else
                end_col = (int)Columns[0];

            int line_end_col = clip_get_line_end(row);

            /* See if we need to nuke some trailing whitespace. */
            if ((int)Columns[0] <= end_col && (row < row2 || line_end_col < end_col))
            {
                /* Get rid of trailing whitespace. */
                end_col = line_end_col;
                if (end_col < start_col)
                    end_col = start_col;

                /* If the last line extended to the end, add an extra newline. */
                if (row == row2)
                    add_newline_flag = true;
            }

            /* If after the first row, we need to always add a newline. */
            if (row1 < row && !lineWraps[row - 1])
                (bufp = bufp.plus(1)).be(-1, NL);

            if (row < screenRows && end_col <= screenColumns)
            {
                int off = lineOffset[row];

                for (int i = start_col; i < end_col; i++)
                {
                    /* The base character is either in screenLinesUC[] or screenLines[]. */
                    if (screenLinesUC[off + i] == 0)
                        (bufp = bufp.plus(1)).be(-1, screenLines.at(off + i));
                    else
                    {
                        bufp = bufp.plus(utf_char2bytes(screenLinesUC[off + i], bufp));
                        for (int ci = 0; ci < screen_mco; ci++)
                        {
                            /* Add a composing character. */
                            if (screenLinesC[ci][off + i] == 0)
                                break;
                            bufp = bufp.plus(utf_char2bytes(screenLinesC[ci][off + i], bufp));
                        }
                    }
                    /* Skip right halve of double-wide character. */
                    if (screenLines.at(off + i + 1) == 0)
                        i++;
                }
            }
        }

        /* Add a newline at the end if the selection ended there. */
        if (add_newline_flag)
            (bufp = bufp.plus(1)).be(-1, NL);

        /* First cleanup any old selection and become the owner. */
        clip_free_selection(clip_star);
        clip_own_selection(clip_star);

        /* Yank the text into the '*' register. */
        clip_yank_selection(MCHAR, buffer, BDIFF(bufp, buffer), clip_star);

        /* Make the register contents available to the outside world. */
        clip_gen_set_selection(clip_star);
    }

    /*
     * Find the starting and ending positions of the word at the given row and column.
     * Only white-separated words are recognized here.
     */
    /*private*/ static int __char_class(int c)
    {
        return (c <= ' ') ? ' ' : vim_iswordc(c, curbuf) ? TRUE : FALSE;
    }

    /*private*/ static void clip_get_word_boundaries(clipboard_C cbd, int row, int col)
    {
        if (screenRows <= row || screenColumns <= col || screenLines == null)
            return;

        Bytes p = screenLines.plus(lineOffset[row]);
        /* Correct for starting in the right halve of a double-wide char. */
        if (p.at(col) == 0)
            --col;
        int start_class = __char_class(p.at(col));

        int temp_col = col;
        for ( ; 0 < temp_col; temp_col--)
            if (__char_class(p.at(temp_col - 1)) != start_class && p.at(temp_col - 1) != 0)
                break;
        cbd.word_start_col = temp_col;

        temp_col = col;
        for ( ; temp_col < screenColumns; temp_col++)
            if (__char_class(p.at(temp_col)) != start_class && p.at(temp_col) != 0)
                break;
        cbd.word_end_col = temp_col;
    }

    /*
     * Find the column position for the last non-whitespace character on the given line.
     */
    /*private*/ static int clip_get_line_end(int row)
    {
        if (screenRows <= row || screenLines == null)
            return 0;

        int i;
        for (i = screenColumns; 0 < i; i--)
            if (screenLines.at(lineOffset[row] + i - 1) != (byte)' ')
                break;
        return i;
    }

    /*
     * Update the currently selected region by adding and/or subtracting from the
     * beginning or end and inverting the changed area(s).
     */
    /*private*/ static void clip_update_modeless_selection(clipboard_C cbd, int row1, int col1, int row2, int col2)
    {
        /* See if we changed at the beginning of the selection. */
        if (row1 != cbd.cbd_start.lnum || col1 != cbd.cbd_start.col)
        {
            clip_invert_area(row1, col1, (int)cbd.cbd_start.lnum, cbd.cbd_start.col, CLIP_TOGGLE);
            cbd.cbd_start.lnum = row1;
            cbd.cbd_start.col  = col1;
        }

        /* See if we changed at the end of the selection. */
        if (row2 != cbd.cbd_end.lnum || col2 != cbd.cbd_end.col)
        {
            clip_invert_area((int)cbd.cbd_end.lnum, cbd.cbd_end.col, row2, col2, CLIP_TOGGLE);
            cbd.cbd_end.lnum = row2;
            cbd.cbd_end.col  = col2;
        }
    }

    /*private*/ static boolean clip_gen_own_selection(clipboard_C _cbd)
    {
        return true;
    }

    /*private*/ static void clip_gen_lose_selection(clipboard_C _cbd)
    {
    }

    /*private*/ static void clip_gen_set_selection(clipboard_C cbd)
    {
        if (!clip_did_set_selection)
        {
            /* Updating postponed, so that accessing the system clipboard won't
             * hang Vim when accessing it many times (e.g. on a :g comand). */
            if ((cbd == clip_plus && (clip_unnamed_saved & CLIP_UNNAMED_PLUS) != 0)
             || (cbd == clip_star && (clip_unnamed_saved & CLIP_UNNAMED) != 0))
            {
                clipboard_needs_update = true;
                return;
            }
        }
        /* TODO */
    }

    /*private*/ static void clip_gen_request_selection(clipboard_C _cbd)
    {
    }

    /*private*/ static boolean clip_gen_owner_exists(clipboard_C _cbd)
    {
        return true;
    }

    /*****************************************************************************
     * Functions that handle the input buffer.
     * This is used for any GUI version, and the unix terminal version.
     *
     * For Unix, the input characters are buffered to be able to check for a CTRL-C.
     * This should be done with signals, but I don't know how to do that in a portable way
     * for a tty in RAW mode.
     *
     * For the client-server code in the console the received keys are put in the input buffer.
     */

    /*
     * Internal typeahead buffer.
     * Includes extra space for long key code descriptions which would otherwise overflow.
     * The buffer is considered full when only this extra space (or part of it) remains.
     */
    /*private*/ static final int INBUFLEN = 250;

    /*private*/ static Bytes    inbuf = new Bytes(INBUFLEN + MAX_KEY_CODE_LEN);
    /*private*/ static int      inbufcount;     /* number of chars in inbuf[] */

    /* Remove everything from the input buffer.  Called when ^C is found. */
    /*private*/ static void trash_input_buf()
    {
        inbufcount = 0;
    }

    /*private*/ static boolean is_input_buf_full()
    {
        return (INBUFLEN <= inbufcount);
    }

    /*private*/ static boolean is_input_buf_empty()
    {
        return (inbufcount == 0);
    }

    /*
     * Save current contents of the input buffer and make it empty.
     */
    /*private*/ static void save_input_buf(tasave_C tp)
    {
        /* Add one to avoid a zero size. */
        tp.save_inputbuf = new Bytes(inbufcount + 1);
        BCOPY(tp.save_inputbuf, inbuf, inbufcount);
        tp.save_inputlen = inbufcount;

        trash_input_buf();
    }

    /*
     * Restore prior contents of the input buffer saved by save_input_buf().
     */
    /*private*/ static void restore_input_buf(tasave_C tp)
    {
        if (tp.save_inputbuf != null)
        {
            inbufcount = tp.save_inputlen;
            BCOPY(inbuf, tp.save_inputbuf, inbufcount);

            tp.save_inputbuf = null;
            tp.save_inputlen = 0;
        }
    }

    /*
     * Read as much data from the input buffer as possible up to maxlen, and store it in buf.
     * Note: this function used to be Read() in unix.c
     */
    /*private*/ static int read_from_input_buf(Bytes buf, int maxlen)
    {
        if (inbufcount == 0)            /* if the buffer is empty, fill it */
            fill_input_buf(true);
        if (inbufcount < maxlen)
            maxlen = inbufcount;
        BCOPY(buf, inbuf, maxlen);
        inbufcount -= maxlen;
        if (inbufcount != 0)
            BCOPY(inbuf, 0, inbuf, maxlen, inbufcount);
        return maxlen;
    }

    /*private*/ static boolean did_read_something;
    /*private*/ static Bytes fib__rest;    /* unconverted rest of previous read */
    /*private*/ static int fib__restlen;

    /*private*/ static void fill_input_buf(boolean exit_on_error)
    {
        if (is_input_buf_full())
            return;

        /*
         * Fill_input_buf() is only called when we really need a character.
         * If we can't get any, but there is some in the buffer, just return.
         * If we can't get any, and there isn't any in the buffer, we give up and exit Vim.
         */

        int unconverted;

        if (fib__rest != null)
        {
            /* Use remainder of previous call, starts with an invalid character
             * that may become valid when reading more. */
            if (fib__restlen > INBUFLEN - inbufcount)
                unconverted = INBUFLEN - inbufcount;
            else
                unconverted = fib__restlen;
            BCOPY(inbuf, inbufcount, fib__rest, 0, unconverted);
            if (unconverted == fib__restlen)
            {
                fib__rest = null;
            }
            else
            {
                fib__restlen -= unconverted;
                BCOPY(fib__rest, 0, fib__rest, unconverted, fib__restlen);
            }
            inbufcount += unconverted;
        }
        else
            unconverted = 0;

        int len = 0;

        for (int round = 0; round < 100; round++)
        {
            len = (int)libC.read(read_cmd_fd, inbuf.plus(inbufcount), INBUFLEN - inbufcount);

            if (0 < len || got_int)
                break;
            /*
             * If reading stdin results in an error, continue reading stderr.
             * This helps when using "foo | xargs vim".
             */
            if (!did_read_something && libc.isatty(read_cmd_fd) == 0 && read_cmd_fd == 0)
            {
                int m = cur_tmode;

                /* We probably set the wrong file descriptor to raw mode.
                 * Switch back to cooked mode, use another descriptor
                 * and set the mode to what it was. */
                settmode(TMODE_COOK);
                /* Use stderr for stdin, also works for shell commands. */
                libc.close(0);
                libc.dup(2);
                settmode(m);
            }
            if (!exit_on_error)
                return;
        }
        if (len <= 0 && !got_int)
            read_error_exit();
        if (0 < len)
            did_read_something = true;
        if (got_int)
        {
            /* Interrupted, pretend a CTRL-C was typed. */
            inbuf.be(0, 3);
            inbufcount = 1;
        }
        else
        {
            /*
             * May perform conversion on the input characters.
             * Include the unconverted rest of the previous call.
             * If there is an incomplete char at the end it is kept for the next
             * time, reading more bytes should make conversion possible.
             * Don't do this in the unlikely event that the input buffer is too
             * small ("fib__rest" still contains more bytes).
             */
            while (0 < len--)
            {
                /*
                 * if a CTRL-C was typed, remove it from the buffer and set got_int
                 */
                if (inbuf.at(inbufcount) == 3 && ctrl_c_interrupts)
                {
                    /* remove everything typed before the CTRL-C */
                    BCOPY(inbuf, 0, inbuf, inbufcount, len + 1);
                    inbufcount = 0;
                    got_int = true;
                }
                inbufcount++;
            }
        }
    }

    /*
     * Exit because of an input read error.
     */
    /*private*/ static void read_error_exit()
    {
        if (silent_mode)    /* Normal way to exit for "ex -s" */
            getout(0);

        STRCPY(ioBuff, u8("Vim: Error reading input, exiting...\n"));
        preserve_exit();
    }

    /*
     * May update the shape of the cursor.
     */
    /*private*/ static void ui_cursor_shape()
    {
        term_cursor_shape();

        conceal_check_cursor_line();
    }

    /*
     * Check bounds for column number
     */
    /*private*/ static int check_col(int col)
    {
        if (col < 0)
            return 0;
        if (col >= screenColumns)
            return screenColumns - 1;

        return col;
    }

    /*
     * Check bounds for row number
     */
    /*private*/ static int check_row(int row)
    {
        if (row < 0)
            return 0;
        if (row >= screenRows)
            return screenRows - 1;

        return row;
    }

    /*private*/ static int on_status_line;      /* #lines below bottom of window */
    /*private*/ static int on_sep_line;         /* on separator right of window */

    /*private*/ static int jm__prev_row = -1;
    /*private*/ static int jm__prev_col = -1;

    /*private*/ static window_C jm__dragwin;    /* window being dragged */
    /*private*/ static boolean jm__did_drag;    /* drag was noticed */

    /*
     * Move the cursor to the specified row and column on the screen.
     * Change current window if necessary.  Returns an integer with the
     * CURSOR_MOVED bit set if the cursor has moved or unset otherwise.
     *
     * If flags has MOUSE_FOCUS, then the current window will not be changed, and
     * if the mouse is outside the window then the text will scroll, or if the
     * mouse was previously on a status line, then the status line may be dragged.
     *
     * If flags has MOUSE_MAY_VIS, then VIsual mode will be started before the
     * cursor is moved unless the cursor was on a status line.
     * This function returns one of IN_UNKNOWN, IN_BUFFER, IN_STATUS_LINE or
     * IN_SEP_LINE depending on where the cursor was clicked.
     *
     * If flags has MOUSE_MAY_STOP_VIS, then Visual mode will be stopped, unless
     * the mouse is on the status line of the same window.
     *
     * If flags has MOUSE_DID_MOVE, nothing is done if the mouse didn't move since
     * the last call.
     *
     * If flags has MOUSE_SETPOS, nothing is done, only the current position is
     * remembered.
     */
    /*private*/ static int jump_to_mouse(int flags, boolean[] inclusive, int which_button)
        /* inclusive: used for inclusive operator, can be null */
        /* which_button: MOUSE_LEFT, MOUSE_RIGHT, MOUSE_MIDDLE */
    {
        int count;

        int[] row = { mouse_row };
        int[] col = { mouse_col };

        mouse_past_bottom = false;
        mouse_past_eol = false;

        if ((flags & MOUSE_RELEASED) != 0)
        {
            /* On button release we may change window focus if positioned
             * on a status line and no dragging happened. */
            if (jm__dragwin != null && !jm__did_drag)
                flags &= ~(MOUSE_FOCUS | MOUSE_DID_MOVE);
            jm__dragwin = null;
            jm__did_drag = false;
        }

        if ((flags & MOUSE_DID_MOVE) != 0 && jm__prev_row == mouse_row && jm__prev_col == mouse_col)
        {
            /* Before moving the cursor for a left click which is NOT in a status line,
             * stop Visual mode. */
            if (on_status_line != 0)
                return IN_STATUS_LINE;
            if (on_sep_line != 0)
                return IN_SEP_LINE;
            if ((flags & MOUSE_MAY_STOP_VIS) != 0)
            {
                end_visual_mode();
                redraw_curbuf_later(INVERTED);      /* delete the inversion */
            }
            /* Continue a modeless selection in another window. */
            if (cmdwin_type != 0 && row[0] < curwin.w_winrow)
                return IN_OTHER_WIN;

            return IN_BUFFER;
        }

        jm__prev_row = mouse_row;
        jm__prev_col = mouse_col;

        if ((flags & MOUSE_SETPOS) != 0)
        {
            /* Before moving the cursor for a left click which is NOT in a status line,
             * stop Visual mode. */
            if (on_status_line != 0)
                return IN_STATUS_LINE;
            if (on_sep_line != 0)
                return IN_SEP_LINE;
            if ((flags & MOUSE_MAY_STOP_VIS) != 0)
            {
                end_visual_mode();
                redraw_curbuf_later(INVERTED);      /* delete the inversion */
            }
            /* Continue a modeless selection in another window. */
            if (cmdwin_type != 0 && row[0] < curwin.w_winrow)
                return IN_OTHER_WIN;

            return IN_BUFFER;
        }

        window_C old_curwin = curwin;
        pos_C old_cursor = new pos_C();
        COPY_pos(old_cursor, curwin.w_cursor);

        if ((flags & MOUSE_FOCUS) == 0)
        {
            if (row[0] < 0 || col[0] < 0)                 /* check if it makes sense */
                return IN_UNKNOWN;

            /* find the window where the row is in */
            window_C wp = mouse_find_win(row, col);
            jm__dragwin = null;
            /*
             * winpos and height may change in win_enter()!
             */
            if (wp.w_height <= row[0])                 /* in (or below) status line */
            {
                on_status_line = row[0] - wp.w_height + 1;
                jm__dragwin = wp;
            }
            else
                on_status_line = 0;
            if (wp.w_width <= col[0])                  /* in separator line */
            {
                on_sep_line = col[0] - wp.w_width + 1;
                jm__dragwin = wp;
            }
            else
                on_sep_line = 0;

            /* The rightmost character of the status line might be a vertical
             * separator character if there is no connecting window to the right. */
            if (on_status_line != 0 && on_sep_line != 0)
            {
                if (stl_connected(wp))
                    on_sep_line = 0;
                else
                    on_status_line = 0;
            }

            /* Before jumping to another buffer, or moving the cursor for a left click,
             * stop Visual mode. */
            if (VIsual_active
                    && (wp.w_buffer != curwin.w_buffer
                        || (on_status_line == 0 && on_sep_line == 0 && (flags & MOUSE_MAY_STOP_VIS) != 0)))
            {
                end_visual_mode();
                redraw_curbuf_later(INVERTED);      /* delete the inversion */
            }
            if (cmdwin_type != 0 && wp != curwin)
            {
                /* A click outside the command-line window: use modeless selection if possible.
                 * Allow dragging the status lines. */
                on_sep_line = 0;
                if (on_status_line != 0)
                    return IN_STATUS_LINE;

                return IN_OTHER_WIN;
            }
            /* Only change window focus when not clicking on or dragging the
             * status line.  Do change focus when releasing the mouse button
             * (MOUSE_FOCUS was set above if we dragged first). */
            if (jm__dragwin == null || (flags & MOUSE_RELEASED) != 0)
                win_enter(wp, true);                /* can make wp invalid! */
            /* set topline, to be able to check for double click ourselves */
            if (curwin != old_curwin)
                set_mouse_topline(curwin);
            if (on_status_line != 0)                     /* in (or below) status line */
            {
                /* Don't use start_arrow() if we're in the same window. */
                if (curwin == old_curwin)
                    return IN_STATUS_LINE;
                else
                    return IN_STATUS_LINE | CURSOR_MOVED;
            }
            if (on_sep_line != 0)                        /* in (or below) status line */
            {
                /* Don't use start_arrow() if we're in the same window. */
                if (curwin == old_curwin)
                    return IN_SEP_LINE;
                else
                    return IN_SEP_LINE | CURSOR_MOVED;
            }

            curwin.w_cursor.lnum = curwin.w_topline;
        }
        else if (on_status_line != 0 && which_button == MOUSE_LEFT)
        {
            if (jm__dragwin != null)
            {
                /* Drag the status line. */
                count = row[0] - jm__dragwin.w_winrow - jm__dragwin.w_height + 1 - on_status_line;
                win_drag_status_line(jm__dragwin, count);
                jm__did_drag |= (count != 0);
            }
            return IN_STATUS_LINE;                  /* Cursor didn't move */
        }
        else if (on_sep_line != 0 && which_button == MOUSE_LEFT)
        {
            if (jm__dragwin != null)
            {
                /* Drag the separator column. */
                count = col[0] - jm__dragwin.w_wincol - jm__dragwin.w_width + 1 - on_sep_line;
                win_drag_vsep_line(jm__dragwin, count);
                jm__did_drag |= (count != 0);
            }
            return IN_SEP_LINE;                     /* Cursor didn't move */
        }
        else /* keep_window_focus must be true */
        {
            /* before moving the cursor for a left click, stop Visual mode */
            if ((flags & MOUSE_MAY_STOP_VIS) != 0)
            {
                end_visual_mode();
                redraw_curbuf_later(INVERTED);      /* delete the inversion */
            }

            /* Continue a modeless selection in another window. */
            if (cmdwin_type != 0 && row[0] < curwin.w_winrow)
                return IN_OTHER_WIN;

            row[0] -= curwin.w_winrow;
            col[0] -= curwin.w_wincol;

            /*
             * When clicking beyond the end of the window, scroll the screen.
             * Scroll by however many rows outside the window we are.
             */
            if (row[0] < 0)
            {
                count = 0;
                for (boolean first = true; 1 < curwin.w_topline; )
                {
                    count += plines(curwin.w_topline - 1);
                    if (!first && -row[0] < count)
                        break;
                    first = false;
                    --curwin.w_topline;
                }
                curwin.w_valid &= ~(VALID_WROW|VALID_CROW|VALID_BOTLINE|VALID_BOTLINE_AP);
                redraw_later(VALID);
                row[0] = 0;
            }
            else if (curwin.w_height <= row[0])
            {
                count = 0;
                for (boolean first = true; curwin.w_topline < curbuf.b_ml.ml_line_count; )
                {
                    count += plines(curwin.w_topline);
                    if (!first && row[0] - curwin.w_height + 1 < count)
                        break;
                    first = false;
                    curwin.w_topline++;
                }
                redraw_later(VALID);
                curwin.w_valid &= ~(VALID_WROW|VALID_CROW|VALID_BOTLINE|VALID_BOTLINE_AP);
                row[0] = curwin.w_height - 1;
            }
            else if (row[0] == 0)
            {
                /* When dragging the mouse, while the text has been scrolled up as
                 * far as it goes, moving the mouse in the top line should scroll
                 * the text down (done later when recomputing w_topline). */
                if (0 < mouse_dragging
                        && curwin.w_cursor.lnum == curwin.w_buffer.b_ml.ml_line_count
                        && curwin.w_cursor.lnum == curwin.w_topline)
                    curwin.w_valid &= ~(VALID_TOPLINE);
            }
        }

        /* compute the position in the buffer line from the posn on the screen */
        boolean b;
        { long[] __ = { curwin.w_cursor.lnum }; b = mouse_comp_pos(curwin, row, col, __); curwin.w_cursor.lnum = __[0]; }
        if (b)
            mouse_past_bottom = true;

        /* Start Visual mode before coladvance(), for when 'sel' != "old". */
        if ((flags & MOUSE_MAY_VIS) != 0 && !VIsual_active)
        {
            check_visual_highlight();
            COPY_pos(VIsual, old_cursor);
            VIsual_active = true;
            VIsual_reselect = true;
            /* if 'selectmode' contains "mouse", start Select mode */
            may_start_select('o');
            setmouse();
            if (p_smd[0] && msg_silent == 0)
                redraw_cmdline = true;          /* show visual mode later */
        }

        curwin.w_curswant = col[0];
        curwin.w_set_curswant = false;          /* May still have been true */
        if (coladvance(col[0]) == false)           /* Mouse click beyond end of line */
        {
            if (inclusive != null)
                inclusive[0] = true;
            mouse_past_eol = true;
        }
        else if (inclusive != null)
            inclusive[0] = false;

        count = IN_BUFFER;
        if (curwin != old_curwin || curwin.w_cursor.lnum != old_cursor.lnum
                || curwin.w_cursor.col != old_cursor.col)
            count |= CURSOR_MOVED;              /* Cursor has moved */

        return count;
    }

    /*
     * Compute the position in the buffer line from the posn on the screen in window "win".
     * Returns true if the position is below the last line.
     */
    /*private*/ static boolean mouse_comp_pos(window_C win, int[] rowp, int[] colp, long[] lnump)
    {
        boolean retval = false;

        int col = colp[0];
        int row = rowp[0];

        if (win.w_onebuf_opt.wo_rl[0])
            col = win.w_width - 1 - col;

        long lnum = win.w_topline;

        while (0 < row)
        {
            int count = plines_win(win, lnum, true);
            if (row < count)
                break;          /* position is in this buffer line */
            if (lnum == win.w_buffer.b_ml.ml_line_count)
            {
                retval = true;
                break;              /* past end of file */
            }
            row -= count;
            lnum++;
        }

        if (!retval)
        {
            /* Compute the column without wrapping. */
            int off = win_col_off(win) - win_col_off2(win);
            if (col < off)
                col = off;
            col += row * (win.w_width - off);
            /* add skip column (for long wrapping line) */
            col += win.w_skipcol;
        }

        if (!win.w_onebuf_opt.wo_wrap[0])
            col += win.w_leftcol;

        /* skip line number and fold column in front of the line */
        col -= win_col_off(win);
        if (col < 0)
        {
            col = 0;
        }

        colp[0] = col;
        rowp[0] = row;
        lnump[0] = lnum;
        return retval;
    }

    /*
     * Find the window at screen position "*rowp" and "*colp".  The positions are
     * updated to become relative to the top-left of the window.
     */
    /*private*/ static window_C mouse_find_win(int[] rowp, int[] colp)
    {
        frame_C fp = topframe;
        rowp[0] -= firstwin.w_winrow;
        for ( ; ; )
        {
            if (fp.fr_layout == FR_LEAF)
                break;
            if (fp.fr_layout == FR_ROW)
            {
                for (fp = fp.fr_child; fp.fr_next != null; fp = fp.fr_next)
                {
                    if (colp[0] < fp.fr_width)
                        break;
                    colp[0] -= fp.fr_width;
                }
            }
            else    /* fr_layout == FR_COL */
            {
                for (fp = fp.fr_child; fp.fr_next != null; fp = fp.fr_next)
                {
                    if (rowp[0] < fp.fr_height)
                        break;
                    rowp[0] -= fp.fr_height;
                }
            }
        }
        return fp.fr_win;
    }

    /*
     * screen.c: code for displaying on the screen
     *
     * Output to the screen (console, terminal emulator or GUI window) is minimized
     * by remembering what is already on the screen, and only updating the parts
     * that changed.
     *
     * screenLines[off]  Contains a copy of the whole screen, as it is currently
     *                   displayed (excluding text written by external commands).
     * screenAttrs[off]  Contains the associated attributes.
     * lineOffset[row]   Contains the offset into screenLines*[] and screenAttrs[]
     *                   for each line.
     * lineWraps[row]    Flag for each line whether it wraps to the next line.
     *
     * For double-byte characters, two consecutive bytes in screenLines[] can form one character
     * which occupies two display cells.  For UTF-8 a multi-byte character is converted to Unicode
     * and stored in screenLinesUC[].  screenLines[] contains the first byte only.  For an ASCII
     * character without composing chars screenLinesUC[] will be 0 and screenLinesC[][] is not used.
     * When the character occupies two display cells the next byte in screenLines[] is 0.
     * screenLinesC[][] contain up to 'maxcombine' composing characters
     * (drawn on top of the first character).  There is 0 after the last one used.
     *
     * The screen_*() functions write to the screen and handle updating screenLines[].
     *
     * update_screen() is the function that updates all windows and status lines.
     * It is called form the main loop when must_redraw is non-zero.  It may be
     * called from other places when an immediate screen update is needed.
     *
     * The part of the buffer that is displayed in a window is set with:
     * - w_topline (first buffer line in window)
     * - w_topfill (filler lines above the first line)
     * - w_leftcol (leftmost window cell in window),
     * - w_skipcol (skipped window cells of first line)
     *
     * Commands that only move the cursor around in a window, do not need to take
     * action to update the display.  The main loop will check if w_topline is
     * valid and update it (scroll the window) when needed.
     *
     * Commands that scroll a window change w_topline and must call
     * check_cursor() to move the cursor into the visible part of the window, and
     * call redraw_later(VALID) to have the window displayed by update_screen() later.
     *
     * Commands that change text in the buffer must call changed_bytes() or
     * changed_lines() to mark the area that changed and will require updating
     * later.  The main loop will call update_screen(), which will update each
     * window that shows the changed buffer.  This assumes text above the change
     * can remain displayed as it is.  Text after the change may need updating for
     * scrolling, folding and syntax highlighting.
     *
     * Commands that change how a window is displayed (e.g., setting 'list') or
     * invalidate the contents of a window in another way (e.g., change fold
     * settings), must call redraw_later(NOT_VALID) to have the whole window
     * redisplayed by update_screen() later.
     *
     * Commands that change how a buffer is displayed (e.g., setting 'tabstop')
     * must call redraw_curbuf_later(NOT_VALID) to have all the windows for the
     * buffer redisplayed by update_screen() later.
     *
     * Commands that change highlighting and possibly cause a scroll too must call
     * redraw_later(SOME_VALID) to update the whole window but still use scrolling
     * to avoid redrawing everything.  But the length of displayed lines must not
     * change, use NOT_VALID then.
     *
     * Commands that move the window position must call redraw_later(NOT_VALID).
     * TODO: should minimize redrawing by scrolling when possible.
     *
     * Commands that change everything (e.g., resizing the screen) must call
     * redraw_all_later(NOT_VALID) or redraw_all_later(CLEAR).
     *
     * Things that are handled indirectly:
     * - When messages scroll the screen up, msg_scrolled will be set and
     *   update_screen() called to redraw.
     */

    /*private*/ static final byte MB_FILLER_CHAR = '<';  /* character used when a double-width character doesn't fit. */

    /*
     * The attributes that are actually active for writing to the screen.
     */
    /*private*/ static int screen_attr;

    /*
     * Positioning the cursor is reduced by remembering the last position.
     * Mostly used by windgoto() and screen_char().
     */
    /*private*/ static int screen_cur_row, screen_cur_col;  /* last known cursor position */

    /*private*/ static match_C search_hl = new match_C();   /* used for 'hlsearch' highlight matching */

    /*
     * Buffer for one screen line (characters and attributes).
     */
    /*private*/ static Bytes current_ScreenLine;

    /*private*/ static final int SEARCH_HL_PRIORITY = 0;

    /* Ugly global: overrule attribute used by screen_char(). */
    /*private*/ static int screen_char_attr;

    /*
     * Redraw the current window later, with update_screen(type).
     * Set must_redraw only if not already set to a higher value.
     * e.g. if must_redraw is CLEAR, type NOT_VALID will do nothing.
     */
    /*private*/ static void redraw_later(int type)
    {
        redraw_win_later(curwin, type);
    }

    /*private*/ static void redraw_win_later(window_C wp, int type)
    {
        if (wp.w_redr_type < type)
        {
            wp.w_redr_type = type;
            if (NOT_VALID <= type)
                wp.w_lines_valid = 0;
            if (must_redraw < type) /* must_redraw is the maximum of all windows */
                must_redraw = type;
        }
    }

    /*
     * Force a complete redraw later.  Also resets the highlighting.
     * To be used after executing a shell command that messes up the screen.
     */
    /*private*/ static void redraw_later_clear()
    {
        redraw_all_later(CLEAR);
        /* Use attributes that is very unlikely to appear in text. */
        screen_attr = HL_BOLD | HL_UNDERLINE | HL_INVERSE;
    }

    /*
     * Mark all windows to be redrawn later.
     */
    /*private*/ static void redraw_all_later(int type)
    {
        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            redraw_win_later(wp, type);
    }

    /*
     * Mark all windows that are editing the current buffer to be updated later.
     */
    /*private*/ static void redraw_curbuf_later(int type)
    {
        redraw_buf_later(curbuf, type);
    }

    /*private*/ static void redraw_buf_later(buffer_C buf, int type)
    {
        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            if (wp.w_buffer == buf)
                redraw_win_later(wp, type);
    }

    /*
     * Redraw as soon as possible.  When the command line is not scrolled
     * redraw right away and restore what was on the command line.
     * Return a code indicating what happened.
     */
    /*private*/ static int redraw_asap(int type)
    {
        int ret = 0;

        redraw_later(type);
        if (msg_scrolled != 0 || (State != NORMAL && State != NORMAL_BUSY))
            return ret;

        /* Allocate space to save the text displayed in the command line area. */
        int rows = (int)Rows[0] - cmdline_row;
        Bytes slis = new Bytes(rows * (int)Columns[0]);
        int[] sats = new int[rows * (int)Columns[0]];

        int[] sluc = new int[rows * (int)Columns[0]];
        int[][] smco = new int[MAX_MCO][];
        for (int i = 0; i < p_mco[0]; i++)
            smco[i] = new int[rows * (int)Columns[0]];

        /* Save the text displayed in the command line area. */
        for (int r = 0; r < rows; r++)
        {
            int off = lineOffset[cmdline_row + r];

            BCOPY(slis, r * (int)Columns[0], screenLines, off, (int)Columns[0]);
            ACOPY(sats, r * (int)Columns[0], screenAttrs, off, (int)Columns[0]);
            ACOPY(sluc, r * (int)Columns[0], screenLinesUC, off, (int)Columns[0]);
            for (int i = 0; i < p_mco[0]; i++)
                ACOPY(smco[i], r * (int)Columns[0], screenLinesC[i], off, (int)Columns[0]);
        }

        update_screen(0);
        ret = 3;

        if (must_redraw == 0)
        {
            int off = BDIFF(current_ScreenLine, screenLines);

            /* Restore the text displayed in the command line area. */
            for (int r = 0; r < rows; r++)
            {
                BCOPY(current_ScreenLine, 0, slis, r * (int)Columns[0], (int)Columns[0]);
                ACOPY(screenAttrs, off, sats, r * (int)Columns[0], (int)Columns[0]);
                ACOPY(screenLinesUC, off, sluc, r * (int)Columns[0], (int)Columns[0]);
                for (int i = 0; i < p_mco[0]; i++)
                    ACOPY(screenLinesC[i], off, smco[i], r * (int)Columns[0], (int)Columns[0]);
                screen_line(cmdline_row + r, 0, (int)Columns[0], (int)Columns[0], false);
            }
            ret = 4;
        }

        /* Show the intro message when appropriate. */
        maybe_intro_message();

        setcursor();

        return ret;
    }

    /*
     * Changed something in the current window, at buffer line "lnum", that
     * requires that line and possibly other lines to be redrawn.
     * Used when entering/leaving Insert mode with the cursor on a folded line.
     * Used to remove the "$" from a change command.
     * Note that when also inserting/deleting lines w_redraw_top and w_redraw_bot
     * may become invalid and the whole window will have to be redrawn.
     */
    /*private*/ static void redrawWinline(long lnum)
    {
        if (curwin.w_redraw_top == 0 || curwin.w_redraw_top > lnum)
            curwin.w_redraw_top = lnum;
        if (curwin.w_redraw_bot == 0 || curwin.w_redraw_bot < lnum)
            curwin.w_redraw_bot = lnum;

        redraw_later(VALID);
    }

    /*
     * update all windows that are editing the current buffer
     */
    /*private*/ static void update_curbuf(int type)
    {
        redraw_curbuf_later(type);
        update_screen(type);
    }

    /*private*/ static boolean did_intro;

    /*
     * update_screen()
     *
     * Based on the current value of curwin.w_topline, transfer a screenfull
     * of stuff from Filemem to screenLines[], and update curwin.w_botline.
     */
    /*private*/ static void update_screen(int type)
    {
        /* Don't do anything if the screen structures are (not yet) valid. */
        if (!screen_valid(true))
            return;

        if (must_redraw != 0)
        {
            if (type < must_redraw)             /* use maximal type */
                type = must_redraw;

            /* must_redraw is reset here, so that when we run into some weird
             * reason to redraw while busy redrawing (e.g., asynchronous
             * scrolling), or update_topline() in win_update() will cause a
             * scroll, the screen will be redrawn later or in win_update(). */
            must_redraw = 0;
        }

        /* Need to update w_lines[]. */
        if (curwin.w_lines_valid == 0 && type < NOT_VALID)
            type = NOT_VALID;

        /* Postpone the redrawing when it's not needed and when being called recursively. */
        if (!redrawing() || updating_screen)
        {
            redraw_later(type);                 /* remember type for next time */
            must_redraw = type;
            if (INVERTED_ALL < type)
                curwin.w_lines_valid = 0;       /* don't use w_lines[].wl_size now */
            return;
        }

        updating_screen = true;
        /* let syntax code know we're in a next round of display updating */
        if (++display_tick < 0)
            display_tick = 0;

        /*
         * if the screen was scrolled up when displaying a message, scroll it down
         */
        if (msg_scrolled != 0)
        {
            clear_cmdline = true;
            if (Rows[0] - 5 < msg_scrolled)        /* clearing is faster */
                type = CLEAR;
            else if (type != CLEAR)
            {
                check_for_delay(false);
                if (screen_ins_lines(0, 0, msg_scrolled, (int)Rows[0], null) == false)
                    type = CLEAR;
                for (window_C wp = firstwin; wp != null; wp = wp.w_next)
                {
                    if (wp.w_winrow < msg_scrolled)
                    {
                        if (msg_scrolled < wp.w_winrow + wp.w_height
                                && wp.w_redr_type < REDRAW_TOP
                                && 0 < wp.w_lines_valid
                                && wp.w_topline == wp.w_lines[0].wl_lnum)
                        {
                            wp.w_upd_rows = msg_scrolled - wp.w_winrow;
                            wp.w_redr_type = REDRAW_TOP;
                        }
                        else
                        {
                            wp.w_redr_type = NOT_VALID;
                            if (wp.w_winrow + wp.w_height + wp.w_status_height <= msg_scrolled)
                                wp.w_redr_status = true;
                        }
                    }
                }
                redraw_cmdline = true;
                redraw_tabline = true;
            }
            msg_scrolled = 0;
            need_wait_return = false;
        }

        /* reset cmdline_row now (may have been changed temporarily) */
        compute_cmdrow();

        /* Check for changed highlighting. */
        if (need_highlight_changed)
            highlight_changed();

        if (type == CLEAR)              /* first clear screen */
        {
            screenclear();              /* will reset clear_cmdline */
            type = NOT_VALID;
        }

        if (clear_cmdline)              /* going to clear cmdline (done below) */
            check_for_delay(false);

        /* Force redraw when width of 'number' or 'relativenumber' column changes. */
        if (curwin.w_redr_type < NOT_VALID
               && curwin.w_nrwidth != ((curwin.w_onebuf_opt.wo_nu[0] || curwin.w_onebuf_opt.wo_rnu[0]) ? number_width(curwin) : 0))
            curwin.w_redr_type = NOT_VALID;

        /*
         * Only start redrawing if there is really something to do.
         */
        if (type == INVERTED)
            update_curswant();
        if (curwin.w_redr_type < type
                && !((type == VALID
                        && curwin.w_lines[0].wl_valid
                        && curwin.w_topline == curwin.w_lines[0].wl_lnum)
                    || (type == INVERTED
                        && VIsual_active
                        && curwin.w_old_cursor_lnum == curwin.w_cursor.lnum
                        && curwin.w_old_visual_mode == VIsual_mode
                        && (curwin.w_valid & VALID_VIRTCOL) != 0
                        && curwin.w_old_curswant == curwin.w_curswant)
                    ))
            curwin.w_redr_type = type;

        /* Redraw the tab pages line if needed. */
        if (redraw_tabline || NOT_VALID <= type)
            draw_tabline();

        /*
         * Correct stored syntax highlighting info for changes in each displayed buffer.
         * Each buffer must only be done once.
         */
        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
        {
            if (wp.w_buffer.b_mod_set)
            {
                window_C wwp;

                /* Check if we already did this buffer. */
                for (wwp = firstwin; wwp != wp; wwp = wwp.w_next)
                    if (wwp.w_buffer == wp.w_buffer)
                        break;
                if (wwp == wp && syntax_present(wp))
                    syn_stack_apply_changes(wp.w_buffer);
            }
        }

        /*
         * Go from top to bottom through the windows, redrawing the ones that need it.
         */
        boolean did_one = false;
        search_hl.rmm.regprog = null;

        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
        {
            if (wp.w_redr_type != 0)
            {
                cursor_off();
                if (!did_one)
                {
                    did_one = true;
                    start_search_hl();
                    /* When Visual area changed, may have to update selection. */
                    if (clip_star.available && clip_isautosel_star())
                        clip_update_selection(clip_star);
                    if (clip_plus.available && clip_isautosel_plus())
                        clip_update_selection(clip_plus);
                }
                win_update(wp);
            }

            /* redraw status line after the window to minimize cursor movement */
            if (wp.w_redr_status)
            {
                cursor_off();
                win_redr_status(wp);
            }
        }
        end_search_hl();

        /* Reset b_mod_set flags.  Going through all windows is probably faster
         * than going through all buffers (there could be many buffers). */
        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            wp.w_buffer.b_mod_set = false;

        updating_screen = false;

        /* Clear or redraw the command line.
         * Done last, because scrolling may mess up the command line. */
        if (clear_cmdline || redraw_cmdline)
            showmode();

        /* May put up an introductory message when not editing a file. */
        if (!did_intro)
        {
            maybe_intro_message();
            did_intro = true;
        }
    }

    /*
     * Return true if the cursor line in window "wp" may be concealed, according
     * to the 'concealcursor' option.
     */
    /*private*/ static boolean conceal_cursor_line(window_C wp)
    {
        if (wp.w_onebuf_opt.wo_cocu[0].at(0) == NUL)
            return false;

        int c;
        if ((get_real_state() & VISUAL) != 0)
            c = 'v';
        else if ((State & INSERT) != 0)
            c = 'i';
        else if ((State & NORMAL) != 0)
            c = 'n';
        else if ((State & CMDLINE) != 0)
            c = 'c';
        else
            return false;

        return (vim_strchr(wp.w_onebuf_opt.wo_cocu[0], c) != null);
    }

    /*
     * Check if the cursor line needs to be redrawn because of 'concealcursor'.
     */
    /*private*/ static void conceal_check_cursor_line()
    {
        if (0 < curwin.w_onebuf_opt.wo_cole[0] && conceal_cursor_line(curwin))
        {
            need_cursor_line_redraw = true;
            /* Need to recompute cursor column, e.g., when starting Visual mode without concealing. */
            curs_columns(true);
        }
    }

    /*private*/ static void update_single_line(window_C wp, long lnum)
    {
        if (wp.w_topline <= lnum && lnum < wp.w_botline)
        {
            int row = 0;
            for (int j = 0; j < wp.w_lines_valid; j++)
            {
                if (lnum == wp.w_lines[j].wl_lnum)
                {
                    screen_start(); /* not sure of screen cursor */
                    init_search_hl(wp);
                    start_search_hl();
                    prepare_search_hl(wp, lnum);
                    win_line(wp, lnum, row, row + wp.w_lines[j].wl_size, false);
                    end_search_hl();
                    break;
                }
                row += wp.w_lines[j].wl_size;
            }
        }

        need_cursor_line_redraw = false;
    }

    /*private*/ static boolean _2_recursive;    /* being called recursively */

    /*
     * Update a single window.
     *
     * This may cause the windows below it also to be redrawn (when clearing the
     * screen or scrolling lines).
     *
     * How the window is redrawn depends on wp.w_redr_type.  Each type also
     * implies the one below it.
     * NOT_VALID    redraw the whole window
     * SOME_VALID   redraw the whole window but do scroll when possible
     * REDRAW_TOP   redraw the top w_upd_rows window lines, otherwise like VALID
     * INVERTED     redraw the changed part of the Visual area
     * INVERTED_ALL redraw the whole Visual area
     * VALID        1. scroll up/down to adjust for a changed w_topline
     *              2. update lines at the top when scrolled down
     *              3. redraw changed text:
     *                 - if wp.w_buffer.b_mod_set set, update lines between
     *                   b_mod_top and b_mod_bot.
     *                 - if wp.w_redraw_top non-zero, redraw lines between
     *                   wp.w_redraw_top and wp.w_redr_bot.
     *                 - continue redrawing when syntax status is invalid.
     *              4. if scrolled up, update lines at the bottom.
     * This results in three areas that may need updating:
     * top: from first row to top_end (when scrolled down)
     * mid: from mid_start to mid_end (update inversion or changed text)
     * bot: from bot_start to last row (when scrolled up)
     */
    /*private*/ static void win_update(window_C wp)
    {
        buffer_C buf = wp.w_buffer;

        int top_end = 0;                    /* Below last row of the top area that needs
                                             * updating.  0 when no top area updating. */
        int mid_start = 999;                /* First row of the mid area that needs
                                             * updating.  999 when no mid area updating. */
        int mid_end = 0;                    /* Below last row of the mid area that needs
                                             * updating.  0 when no mid area updating. */
        int bot_start = 999;                /* First row of the bot area that needs
                                             * updating.  999 when no bot area updating. */
        boolean scrolled_down = false;      /* true when scrolled down when
                                             * w_topline got smaller a bit */
        boolean top_to_mod = false;         /* redraw above mod_top */

        boolean eof = false;                /* if true, we hit the end of the file */
        boolean didline = false;            /* if true, we finished the last line */

        long old_botline = wp.w_botline;

        /* remember what happened to the previous line,
         * to know if check_visual_highlight() can be used
         */
        final int
            DID_NONE = 1,                   /* didn't update a line */
            DID_LINE = 2,                   /* updated a normal line */
            DID_FOLD = 3;                   /* updated a folded line */
        int did_update = DID_NONE;

        long syntax_last_parsed = 0;        /* last parsed text line */
        long mod_top = 0;
        long mod_bot = 0;

        int type = wp.w_redr_type;

        if (type == NOT_VALID)
        {
            wp.w_redr_status = true;
            wp.w_lines_valid = 0;
        }

        /* Window is zero-height: nothing to draw. */
        if (wp.w_height == 0)
        {
            wp.w_redr_type = 0;
            return;
        }

        /* Window is zero-width: Only need to draw the separator. */
        if (wp.w_width == 0)
        {
            /* draw the vertical separator right of this window */
            draw_vsep_win(wp, 0);
            wp.w_redr_type = 0;
            return;
        }

        init_search_hl(wp);

        /* Force redraw when width of 'number' or 'relativenumber' column changes. */
        int i = (wp.w_onebuf_opt.wo_nu[0] || wp.w_onebuf_opt.wo_rnu[0]) ? number_width(wp) : 0;
        if (wp.w_nrwidth != i)
        {
            type = NOT_VALID;
            wp.w_nrwidth = i;
        }
        else if (buf.b_mod_set && buf.b_mod_xlines != 0 && wp.w_redraw_top != 0)
        {
            /*
             * When there are both inserted/deleted lines and specific lines to be
             * redrawn, w_redraw_top and w_redraw_bot may be invalid, just redraw
             * everything (only happens when redrawing is off for while).
             */
            type = NOT_VALID;
        }
        else
        {
            /*
             * Set mod_top to the first line that needs displaying because of changes.
             * Set mod_bot to the first line after the changes.
             */
            mod_top = wp.w_redraw_top;
            if (wp.w_redraw_bot != 0)
                mod_bot = wp.w_redraw_bot + 1;
            else
                mod_bot = 0;
            wp.w_redraw_top = 0;    /* reset for next time */
            wp.w_redraw_bot = 0;
            if (buf.b_mod_set)
            {
                if (mod_top == 0 || buf.b_mod_top < mod_top)
                {
                    mod_top = buf.b_mod_top;
                    /* Need to redraw lines above the change that may be included in a pattern match. */
                    if (syntax_present(wp))
                    {
                        mod_top -= buf.b_s.b_syn_sync_linebreaks;
                        if (mod_top < 1)
                            mod_top = 1;
                    }
                }
                if (mod_bot == 0 || mod_bot < buf.b_mod_bot)
                    mod_bot = buf.b_mod_bot;

                /* When 'hlsearch' is on and using a multi-line search pattern,
                 * a change in one line may make the Search highlighting in a previous line invalid.
                 * Simple solution: redraw all visible lines above the change.
                 * Same for a match pattern.
                 */
                if (search_hl.rmm.regprog != null && re_multiline(search_hl.rmm.regprog))
                    top_to_mod = true;
                else
                {
                    for (matchitem_C mi = wp.w_match_head; mi != null; mi = mi.next)
                        if (mi.mi_match.regprog != null && re_multiline(mi.mi_match.regprog))
                        {
                            top_to_mod = true;
                            break;
                        }
                }
            }

            /* When a change starts above w_topline and the end is below w_topline,
             * start redrawing at w_topline.
             * If the end of the change is above w_topline: do like no change was made,
             * but redraw the first line to find changes in syntax. */
            if (mod_top != 0 && mod_top < wp.w_topline)
            {
                if (mod_bot > wp.w_topline)
                    mod_top = wp.w_topline;
                else if (syntax_present(wp))
                    top_end = 1;
            }

            /* When line numbers are displayed, need to redraw all lines below
             * inserted/deleted lines. */
            if (mod_top != 0 && buf.b_mod_xlines != 0 && wp.w_onebuf_opt.wo_nu[0])
                mod_bot = MAXLNUM;
        }

        /*
         * When only displaying the lines at the top, set top_end.
         * Used when window has scrolled down for msg_scrolled.
         */
        if (type == REDRAW_TOP)
        {
            int j = 0;
            for (i = 0; i < wp.w_lines_valid; i++)
            {
                j += wp.w_lines[i].wl_size;
                if (wp.w_upd_rows <= j)
                {
                    top_end = j;
                    break;
                }
            }
            if (top_end == 0)
                /* not found (cannot happen?): redraw everything */
                type = NOT_VALID;
            else
                /* top area defined, the rest is VALID */
                type = VALID;
        }

        /* Trick: we want to avoid clearing the screen twice.  screenclear() will set
         * "screen_cleared" to true.  The special value MAYBE (which is still non-zero
         * and thus not false) will indicate that screenclear() was not called. */
        if (screen_cleared != FALSE)
            screen_cleared = MAYBE;

        /*
         * If there are no changes on the screen that require a complete redraw,
         * handle three cases:
         * 1: we are off the top of the screen by a few lines: scroll down
         * 2: wp.w_topline is below wp.w_lines[0].wl_lnum: may scroll up
         * 3: wp.w_topline is wp.w_lines[0].wl_lnum: find first entry in w_lines[] that needs updating.
         */
        if ((type == VALID || type == SOME_VALID || type == INVERTED || type == INVERTED_ALL))
        {
            if (mod_top != 0 && wp.w_topline == mod_top)
            {
                /* w_topline is the first changed line, the scrolling will be done further down. */
            }
            else if (wp.w_lines[0].wl_valid && (wp.w_topline < wp.w_lines[0].wl_lnum))
            {
                /* New topline is above old topline: may scroll down. */
                int j = (int)(wp.w_lines[0].wl_lnum - wp.w_topline);

                if (j < wp.w_height - 2)        /* not too far off */
                {
                    i = plines_m_win(wp, wp.w_topline, wp.w_lines[0].wl_lnum - 1);

                    if (i < wp.w_height - 2)    /* less than a screen off */
                    {
                        /*
                         * Try to insert the correct number of lines.
                         * If not the last window, delete the lines at the bottom.
                         * win_ins_lines may fail when the terminal can't do it.
                         */
                        if (0 < i)
                            check_for_delay(false);
                        if (win_ins_lines(wp, 0, i, false, wp == firstwin) == true)
                        {
                            if (wp.w_lines_valid != 0)
                            {
                                /* Need to update rows that are new,
                                 * stop at the first one that scrolled down. */
                                top_end = i;
                                scrolled_down = true;

                                /* Move the entries that were scrolled,
                                 * disable the entries for the lines to be redrawn. */
                                if ((wp.w_lines_valid += j) > wp.w_height)
                                    wp.w_lines_valid = wp.w_height;
                                int idx;
                                for (idx = wp.w_lines_valid; 0 <= idx - j; --idx)
                                    COPY_wline(wp.w_lines[idx], wp.w_lines[idx - j]);
                                while (0 <= idx)
                                    wp.w_lines[idx--].wl_valid = false;
                            }
                        }
                        else
                            mid_start = 0;          /* redraw all lines */
                    }
                    else
                        mid_start = 0;              /* redraw all lines */
                }
                else
                    mid_start = 0;                  /* redraw all lines */
            }
            else
            {
                /*
                 * New topline is at or below old topline: May scroll up.
                 * When topline didn't change, find first entry in w_lines[] that needs updating.
                 */

                /* try to find wp.w_topline in wp.w_lines[].wl_lnum */
                int j = -1;
                int row = 0;
                for (i = 0; i < wp.w_lines_valid; i++)
                {
                    if (wp.w_lines[i].wl_valid && wp.w_lines[i].wl_lnum == wp.w_topline)
                    {
                        j = i;
                        break;
                    }
                    row += wp.w_lines[i].wl_size;
                }
                if (j == -1)
                {
                    /* if wp.w_topline is not in wp.w_lines[].wl_lnum redraw all lines */
                    mid_start = 0;
                }
                else
                {
                    /*
                     * Try to delete the correct number of lines.
                     * wp.w_topline is at wp.w_lines[i].wl_lnum.
                     */
                    if (0 < row)
                    {
                        check_for_delay(false);
                        if (win_del_lines(wp, 0, row, false, wp == firstwin) == true)
                            bot_start = wp.w_height - row;
                        else
                            mid_start = 0;          /* redraw all lines */
                    }
                    if ((row == 0 || bot_start < 999) && wp.w_lines_valid != 0)
                    {
                        /*
                         * Skip the lines (below the deleted lines) that are still valid and
                         * don't need redrawing.  Copy their info upwards, to compensate for the
                         * deleted lines.  Set bot_start to the first row that needs redrawing.
                         */
                        bot_start = 0;
                        int idx = 0;                /* current index in w_lines[] */
                        for ( ; ; )
                        {
                            COPY_wline(wp.w_lines[idx], wp.w_lines[j]);
                            /* stop at line that didn't fit,
                             * unless it is still valid (no lines deleted) */
                            if (0 < row && wp.w_height < bot_start + row + wp.w_lines[j].wl_size)
                            {
                                wp.w_lines_valid = idx + 1;
                                break;
                            }
                            bot_start += wp.w_lines[idx++].wl_size;

                            /* stop at the last valid entry in w_lines[].wl_size */
                            if (wp.w_lines_valid <= ++j)
                            {
                                wp.w_lines_valid = idx;
                                break;
                            }
                        }
                    }
                }
            }

            /* When starting redraw in the first line, redraw all lines.
             * When there is only one window it's probably faster to clear the screen first. */
            if (mid_start == 0)
            {
                mid_end = wp.w_height;
                if (lastwin == firstwin)
                {
                    /* Clear the screen when it was not done by win_del_lines() or
                     * win_ins_lines() above, "screen_cleared" is false or MAYBE then. */
                    if (screen_cleared != TRUE)
                        screenclear();
                    /* The screen was cleared, redraw the tab pages line. */
                    if (redraw_tabline)
                        draw_tabline();
                }
            }

            /* When win_del_lines() or win_ins_lines() caused the screen to be
             * cleared (only happens for the first window) or when screenclear()
             * was called directly above, "must_redraw" will have been set to
             * NOT_VALID, need to reset it here to avoid redrawing twice. */
            if (screen_cleared == TRUE)
                must_redraw = 0;
        }
        else
        {
            /* Not VALID or INVERTED: redraw all lines. */
            mid_start = 0;
            mid_end = wp.w_height;
        }

        if (type == SOME_VALID)
        {
            /* SOME_VALID: redraw all lines. */
            mid_start = 0;
            mid_end = wp.w_height;
            type = NOT_VALID;
        }

        /* check if we are updating or removing the inverted part */
        if ((VIsual_active && buf == curwin.w_buffer)
            || (wp.w_old_cursor_lnum != 0 && type != NOT_VALID))
        {
            long from, to;

            if (VIsual_active)
            {
                if (VIsual_active && (VIsual_mode != wp.w_old_visual_mode || type == INVERTED_ALL))
                {
                    /*
                     * If the type of Visual selection changed, redraw the whole selection.
                     * Also when the ownership of the X selection is gained or lost.
                     */
                    if (curwin.w_cursor.lnum < VIsual.lnum)
                    {
                        from = curwin.w_cursor.lnum;
                        to = VIsual.lnum;
                    }
                    else
                    {
                        from = VIsual.lnum;
                        to = curwin.w_cursor.lnum;
                    }
                    /* redraw more when the cursor moved as well */
                    if (wp.w_old_cursor_lnum < from)
                        from = wp.w_old_cursor_lnum;
                    if (to < wp.w_old_cursor_lnum)
                        to = wp.w_old_cursor_lnum;
                    if (wp.w_old_visual_lnum < from)
                        from = wp.w_old_visual_lnum;
                    if (to < wp.w_old_visual_lnum)
                        to = wp.w_old_visual_lnum;
                }
                else
                {
                    /*
                     * Find the line numbers that need to be updated: The lines
                     * between the old cursor position and the current cursor
                     * position.  Also check if the Visual position changed.
                     */
                    if (curwin.w_cursor.lnum < wp.w_old_cursor_lnum)
                    {
                        from = curwin.w_cursor.lnum;
                        to = wp.w_old_cursor_lnum;
                    }
                    else
                    {
                        from = wp.w_old_cursor_lnum;
                        to = curwin.w_cursor.lnum;
                        if (from == 0)              /* Visual mode just started */
                            from = to;
                    }

                    if (VIsual.lnum != wp.w_old_visual_lnum || VIsual.col != wp.w_old_visual_col)
                    {
                        if (wp.w_old_visual_lnum < from && wp.w_old_visual_lnum != 0)
                            from = wp.w_old_visual_lnum;
                        if (to < wp.w_old_visual_lnum)
                            to = wp.w_old_visual_lnum;
                        if (VIsual.lnum < from)
                            from = VIsual.lnum;
                        if (to < VIsual.lnum)
                            to = VIsual.lnum;
                    }
                }

                /*
                 * If in block mode and changed column or curwin.w_curswant: update all lines.
                 * First compute the actual start and end column.
                 */
                if (VIsual_mode == Ctrl_V)
                {
                    int[] fromc = new int[1];
                    int[] toc = new int[1];
                    int save_ve_flags = ve_flags[0];

                    if (curwin.w_onebuf_opt.wo_lbr[0])
                        ve_flags[0] = VE_ALL;
                    getvcols(wp, VIsual, curwin.w_cursor, fromc, toc);
                    ve_flags[0] = save_ve_flags;
                    toc[0]++;
                    if (curwin.w_curswant == MAXCOL)
                        toc[0] = MAXCOL;

                    if (fromc[0] != wp.w_old_cursor_fcol || toc[0] != wp.w_old_cursor_lcol)
                    {
                        if (VIsual.lnum < from)
                            from = VIsual.lnum;
                        if (to < VIsual.lnum)
                            to = VIsual.lnum;
                    }
                    wp.w_old_cursor_fcol = fromc[0];
                    wp.w_old_cursor_lcol = toc[0];
                }
            }
            else
            {
                /* Use the line numbers of the old Visual area. */
                if (wp.w_old_cursor_lnum < wp.w_old_visual_lnum)
                {
                    from = wp.w_old_cursor_lnum;
                    to = wp.w_old_visual_lnum;
                }
                else
                {
                    from = wp.w_old_visual_lnum;
                    to = wp.w_old_cursor_lnum;
                }
            }

            /*
             * There is no need to update lines above the top of the window.
             */
            if (from < wp.w_topline)
                from = wp.w_topline;

            /*
             * If we know the value of w_botline,
             * use it to restrict the update to the lines that are visible in the window.
             */
            if ((wp.w_valid & VALID_BOTLINE) != 0)
            {
                if (from >= wp.w_botline)
                    from = wp.w_botline - 1;
                if (to >= wp.w_botline)
                    to = wp.w_botline - 1;
            }

            /*
             * Find the minimal part to be updated.
             * Watch out for scrolling that made entries in w_lines[] invalid.
             * E.g., CTRL-U makes the first half of w_lines[] invalid and sets top_end;
             * need to redraw from top_end to the "to" line.
             * A middle mouse click with a Visual selection may change the text above
             * the Visual area and reset wl_valid, do count these for mid_end (in srow).
             */
            if (0 < mid_start)
            {
                long lnum = wp.w_topline;       /* current buffer lnum to display */
                int idx = 0;                    /* current index in w_lines[] */
                int srow = 0;                   /* starting row of the current line */
                if (scrolled_down)
                    mid_start = top_end;
                else
                    mid_start = 0;
                while (lnum < from && idx < wp.w_lines_valid)   /* find start */
                {
                    if (wp.w_lines[idx].wl_valid)
                        mid_start += wp.w_lines[idx].wl_size;
                    else if (!scrolled_down)
                        srow += wp.w_lines[idx].wl_size;
                    idx++;
                    lnum++;
                }
                srow += mid_start;
                mid_end = wp.w_height;
                for ( ; idx < wp.w_lines_valid; idx++)          /* find end */
                {
                    if (wp.w_lines[idx].wl_valid && to + 1 <= wp.w_lines[idx].wl_lnum)
                    {
                        /* Only update until first row of this line. */
                        mid_end = srow;
                        break;
                    }
                    srow += wp.w_lines[idx].wl_size;
                }
            }
        }

        if (VIsual_active && buf == curwin.w_buffer)
        {
            wp.w_old_visual_mode = VIsual_mode;
            wp.w_old_cursor_lnum = curwin.w_cursor.lnum;
            wp.w_old_visual_lnum = VIsual.lnum;
            wp.w_old_visual_col = VIsual.col;
            wp.w_old_curswant = curwin.w_curswant;
        }
        else
        {
            wp.w_old_visual_mode = 0;
            wp.w_old_cursor_lnum = 0;
            wp.w_old_visual_lnum = 0;
            wp.w_old_visual_col = 0;
        }

        /* reset got_int, otherwise regexp won't work */
        boolean save_got_int = got_int;
        got_int = false;

        /*
         * Update all the window rows.
         */
        int idx = 0;                    /* first entry in w_lines[].wl_size */
        int row = 0;                    /* current window row to display */
        int srow = 0;                   /* starting row of the current line */
        long lnum = wp.w_topline;       /* first line shown in window */
        for ( ; ; )
        {
            /* stop updating when reached the end of the window
             * (check for _past_ the end of the window is at the end of the loop) */
            if (row == wp.w_height)
            {
                didline = true;
                break;
            }

            /* stop updating when hit the end of the file */
            if (buf.b_ml.ml_line_count < lnum)
            {
                eof = true;
                break;
            }

            /* Remember the starting row of the line that is going to be dealt with.
             * It is used further down when the line doesn't fit. */
            srow = row;

            /*
             * Update a line when it is in an area that needs updating,
             * when it has changes or w_lines[idx] is invalid.
             * bot_start may be halfway a wrapped line after using win_del_lines(),
             * check if the current line includes it.
             * When syntax folding is being used, the saved syntax states will
             * already have been updated, we can't see where the syntax state is
             * the same again, just update until the end of the window.
             */
            if (row < top_end
                    || (mid_start <= row && row < mid_end)
                    || top_to_mod
                    || wp.w_lines_valid <= idx
                    || (bot_start < row + wp.w_lines[idx].wl_size)
                    || (mod_top != 0
                        && (lnum == mod_top
                            || (mod_top <= lnum
                                && (lnum < mod_bot
                                    || did_update == DID_FOLD
                                    || (did_update == DID_LINE
                                        && syntax_present(wp)
                                        && syntax_check_changed(lnum))
                                    /* match in fixed position might need redraw
                                     * if lines were inserted or deleted */
                                    || (wp.w_match_head != null && buf.b_mod_xlines != 0)
                                    )))))
            {
                if (lnum == mod_top)
                    top_to_mod = false;

                /*
                 * When at start of changed lines:
                 * may scroll following lines up or down to minimize redrawing.
                 * Don't do this when the change continues until the end.
                 * Don't scroll when dollar_vcol >= 0, keep the "$".
                 */
                if (lnum == mod_top
                        && mod_bot != MAXLNUM
                        && !(0 <= dollar_vcol && mod_bot == mod_top + 1))
                {
                    int old_rows = 0;
                    int new_rows = 0;
                    int xtra_rows;
                    long l;

                    /* Count the old number of window rows, using w_lines[], which should
                     * still contain the sizes for the lines as they are currently displayed.
                     */
                    for (i = idx; i < wp.w_lines_valid; i++)
                    {
                        /* Only valid lines have a meaningful wl_lnum.
                         * Invalid lines are part of the changed area. */
                        if (wp.w_lines[i].wl_valid && wp.w_lines[i].wl_lnum == mod_bot)
                            break;
                        old_rows += wp.w_lines[i].wl_size;
                    }

                    if (wp.w_lines_valid <= i)
                    {
                        /* We can't find a valid line below the changed lines,
                         * need to redraw until the end of the window.
                         * Inserting/deleting lines has no use. */
                        bot_start = 0;
                    }
                    else
                    {
                        /* Able to count old number of rows:
                         * count new window rows, and may insert/delete lines. */
                        int j = idx;
                        for (l = lnum; l < mod_bot; l++)
                        {
                            new_rows += plines_win(wp, l, true);
                            j++;
                            if (wp.w_height - row - 2 < new_rows)
                            {
                                /* it's getting too much, must redraw the rest */
                                new_rows = 9999;
                                break;
                            }
                        }
                        xtra_rows = new_rows - old_rows;
                        if (xtra_rows < 0)
                        {
                            /* May scroll text up.
                             * If there is not enough remaining text or scrolling fails,
                             * must redraw the rest.
                             * If scrolling works,
                             * must redraw the text below the scrolled text. */
                            if (wp.w_height - 2 <= row - xtra_rows)
                                mod_bot = MAXLNUM;
                            else
                            {
                                check_for_delay(false);
                                if (win_del_lines(wp, row, -xtra_rows, false, false) == false)
                                    mod_bot = MAXLNUM;
                                else
                                    bot_start = wp.w_height + xtra_rows;
                            }
                        }
                        else if (0 < xtra_rows)
                        {
                            /* May scroll text down.
                             * If there is not enough remaining text of scrolling fails,
                             * must redraw the rest. */
                            if (wp.w_height - 2 <= row + xtra_rows)
                                mod_bot = MAXLNUM;
                            else
                            {
                                check_for_delay(false);
                                if (win_ins_lines(wp, row + old_rows, xtra_rows, false, false) == false)
                                    mod_bot = MAXLNUM;
                                else if (row + old_rows < top_end)
                                    /* Scrolled the part at the top that requires updating down. */
                                    top_end += xtra_rows;
                            }
                        }

                        /* When not updating the rest, may need to move w_lines[] entries. */
                        if (mod_bot != MAXLNUM && i != j)
                        {
                            if (j < i)
                            {
                                int x = row + new_rows;

                                /* move entries in w_lines[] upwards */
                                for ( ; ; )
                                {
                                    /* stop at last valid entry in w_lines[] */
                                    if (wp.w_lines_valid <= i)
                                    {
                                        wp.w_lines_valid = j;
                                        break;
                                    }
                                    COPY_wline(wp.w_lines[j], wp.w_lines[i]);
                                    /* stop at a line that won't fit */
                                    if (wp.w_height < x + wp.w_lines[j].wl_size)
                                    {
                                        wp.w_lines_valid = j + 1;
                                        break;
                                    }
                                    x += wp.w_lines[j++].wl_size;
                                    i++;
                                }
                                if (x < bot_start)
                                    bot_start = x;
                            }
                            else /* j > i */
                            {
                                /* move entries in w_lines[] downwards */
                                j -= i;
                                wp.w_lines_valid += j;
                                if (wp.w_lines_valid > wp.w_height)
                                    wp.w_lines_valid = wp.w_height;
                                for (i = wp.w_lines_valid; idx <= i - j; --i)
                                    COPY_wline(wp.w_lines[i], wp.w_lines[i - j]);

                                /* The w_lines[] entries for inserted lines are now invalid,
                                 * but wl_size may be used above.
                                 * Reset to zero. */
                                while (idx <= i)
                                {
                                    wp.w_lines[i].wl_size = 0;
                                    wp.w_lines[i--].wl_valid = false;
                                }
                            }
                        }
                    }
                }

                if (idx < wp.w_lines_valid
                        && wp.w_lines[idx].wl_valid
                        && wp.w_lines[idx].wl_lnum == lnum
                        && wp.w_topline < lnum
                        && (dy_flags[0] & DY_LASTLINE) == 0
                        && wp.w_height < srow + wp.w_lines[idx].wl_size)
                {
                    /* This line is not going to fit.
                     * Don't draw anything here, will draw "@  " lines below. */
                    row = wp.w_height + 1;
                }
                else
                {
                    prepare_search_hl(wp, lnum);
                    /* Let the syntax stuff know we skipped a few lines. */
                    if (syntax_last_parsed != 0 && syntax_last_parsed + 1 < lnum && syntax_present(wp))
                        syntax_end_parsing(syntax_last_parsed + 1);

                    /*
                     * Display one line.
                     */
                    row = win_line(wp, lnum, srow, wp.w_height, mod_top == 0);

                    did_update = DID_LINE;
                    syntax_last_parsed = lnum;
                }

                wp.w_lines[idx].wl_lnum = lnum;
                wp.w_lines[idx].wl_valid = true;
                if (wp.w_height < row)              /* past end of screen */
                {
                    /* we may need the size of that too long line later on */
                    if (dollar_vcol == -1)
                        wp.w_lines[idx].wl_size = plines_win(wp, lnum, true);
                    idx++;
                    break;
                }
                if (dollar_vcol == -1)
                    wp.w_lines[idx].wl_size = row - srow;
                idx++;
                lnum++;
            }
            else
            {
                /* This line does not need updating, advance to the next one. */
                row += wp.w_lines[idx++].wl_size;
                if (wp.w_height < row)              /* past end of screen */
                    break;
                lnum++;
                did_update = DID_NONE;
            }

            if (buf.b_ml.ml_line_count < lnum)
            {
                eof = true;
                break;
            }
        }
        /*
         * End of loop over all window lines.
         */

        if (wp.w_lines_valid < idx)
            wp.w_lines_valid = idx;

        /*
         * Let the syntax stuff know we stop parsing here.
         */
        if (syntax_last_parsed != 0 && syntax_present(wp))
            syntax_end_parsing(syntax_last_parsed + 1);

        /*
         * If we didn't hit the end of the file, and we didn't finish the last
         * line we were working on, then the line didn't fit.
         */
        wp.w_empty_rows = 0;
        if (!eof && !didline)
        {
            if (lnum == wp.w_topline)
            {
                /*
                 * Single line that does not fit!
                 * Don't overwrite it, it can be edited.
                 */
                wp.w_botline = lnum + 1;
            }
            else if ((dy_flags[0] & DY_LASTLINE) != 0)     /* 'display' has "lastline" */
            {
                /*
                 * Last line isn't finished: Display "@@@" at the end.
                 */
                screen_fill(wp.w_winrow + wp.w_height - 1, wp.w_winrow + wp.w_height,
                            wp.w_wincol + wp.w_width - 3, wp.w_wincol + wp.w_width,
                            '@', '@', hl_attr(HLF_AT));
                set_empty_rows(wp, srow);
                wp.w_botline = lnum;
            }
            else
            {
                win_draw_end(wp, '@', ' ', srow, wp.w_height, HLF_AT);
                wp.w_botline = lnum;
            }
        }
        else
        {
            draw_vsep_win(wp, row);
            if (eof)                                /* we hit the end of the file */
            {
                wp.w_botline = buf.b_ml.ml_line_count + 1;
            }
            else if (dollar_vcol == -1)
                wp.w_botline = lnum;

            /* Make sure the rest of the screen is blank,
             * put '~'s on rows that aren't part of the file. */
            win_draw_end(wp, '~', ' ', row, wp.w_height, HLF_AT);
        }

        /* Reset the type of redrawing required, the window has been updated. */
        wp.w_redr_type = 0;

        if (dollar_vcol == -1)
        {
            /*
             * There is a trick with w_botline.  If we invalidate it on each
             * change that might modify it, this will cause a lot of expensive
             * calls to plines() in update_topline() each time.  Therefore the
             * value of w_botline is often approximated, and this value is used to
             * compute the value of w_topline.  If the value of w_botline was
             * wrong, check that the value of w_topline is correct (cursor is on
             * the visible part of the text).  If it's not, we need to redraw
             * again.  Mostly this just means scrolling up a few lines, so it
             * doesn't look too bad.  Only do this for the current window (where
             * changes are relevant).
             */
            wp.w_valid |= VALID_BOTLINE;
            if (wp == curwin && wp.w_botline != old_botline && !_2_recursive)
            {
                _2_recursive = true;
                curwin.w_valid &= ~VALID_TOPLINE;
                update_topline();   /* may invalidate w_botline again */
                if (must_redraw != 0)
                {
                    /* Don't update for changes in buffer again. */
                    boolean b = curbuf.b_mod_set;
                    curbuf.b_mod_set = false;
                    win_update(curwin);
                    must_redraw = 0;
                    curbuf.b_mod_set = b;
                }
                _2_recursive = false;
            }
        }

        /* restore got_int, unless CTRL-C was hit while redrawing */
        if (!got_int)
            got_int = save_got_int;
    }

    /*
     * Clear the rest of the window and mark the unused lines with "c1".
     * Use "c2" as the filler character.
     */
    /*private*/ static void win_draw_end(window_C wp, int c1, int c2, int row, int endrow, int hl)
    {
        int n = 0;

        if (wp.w_onebuf_opt.wo_rl[0])
        {
            /* No check for cmdline window: should never be right-left. */
            screen_fill(wp.w_winrow + row, wp.w_winrow + endrow,
                        wp.w_wincol, wp.w_wincol + wp.w_width - 1 - n,
                        c2, c2, hl_attr(hl));
            screen_fill(wp.w_winrow + row, wp.w_winrow + endrow,
                        wp.w_wincol + wp.w_width - 1 - n, wp.w_wincol + wp.w_width - n,
                        c1, c2, hl_attr(hl));
        }
        else
        {
            if (cmdwin_type != 0 && wp == curwin)
            {
                /* draw the cmdline character in the leftmost column */
                n = 1;
                if (n > wp.w_width)
                    n = wp.w_width;
                screen_fill(wp.w_winrow + row, wp.w_winrow + endrow,
                            wp.w_wincol, wp.w_wincol + n,
                            cmdwin_type, ' ', hl_attr(HLF_AT));
            }
            screen_fill(wp.w_winrow + row, wp.w_winrow + endrow,
                        wp.w_wincol + n, wp.w_wincol + wp.w_width,
                        c1, c2, hl_attr(hl));
        }

        set_empty_rows(wp, row);
    }

    /*
     * Advance **color_cols and return true when there are columns to draw.
     */
    /*private*/ static boolean advance_color_col(int vcol, int[] color_cols, int[] cci)
    {
        while (0 <= color_cols[cci[0]] && color_cols[cci[0]] < vcol)
            cci[0]++;

        return (0 <= color_cols[cci[0]]);
    }

    /* used for p_extra when displaying lcs_eol at end-of-line */
    /*private*/ static Bytes at_end_str = u8("");

    /*
     * Display line "lnum" of window 'wp' on the screen.
     * Start at row "startrow", stop when "endrow" is reached.
     * wp.w_virtcol needs to be valid.
     *
     * Return the number of last row the line occupies.
     */
    /*private*/ static int win_line(window_C wp, long lnum, int startrow, int endrow, boolean _nochange)
        /* nochange: not updating for changed text */
    {
        int c = 0;
        int vcol = 0;                          /* virtual column (for tabs) */
        int vcol_sbr = -1;                     /* virtual column after showbreak */
        int vcol_prev = -1;                    /* "vcol" of previous character */

        Bytes extra = new Bytes(18);            /* "%ld" and 'fdc' must fit in here */
        int n_extra = 0;                        /* number of extra chars */
        Bytes p_extra = null;                  /* string of extra chars, plus NUL */
        Bytes p_extra_free = null;             /* "p_extra" needs to be freed */
        int c_extra = NUL;                      /* extra chars, all the same */
        int extra_attr = 0;                     /* attributes when n_extra != 0 */

        int lcs_eol_one = lcs_eol[0];              /* lcs_eol until it's been used */
        int lcs_prec_todo = lcs_prec[0];           /* lcs_prec until it's been used */

        int saved_n_extra = 0;  /* saved "extra" items for when draw_state becomes WL_LINE (again) */
        Bytes saved_p_extra = null;
        int saved_c_extra = 0;
        int saved_char_attr = 0;

        int n_attr = 0;                         /* chars with special attr */
        int saved_attr2 = 0;                    /* char_attr saved for n_attr */
        int n_attr3 = 0;                        /* chars with overruling special attr */
        int saved_attr3 = 0;                    /* char_attr saved for n_attr3 */

        int n_skip = 0;                         /* nr of chars to skip for 'nowrap' */

        int fromcol_prev = -2;                  /* start of inverting after cursor */
        boolean noinvcur = false;               /* don't invert the cursor */
        pos_C top, bot;
        boolean lnum_in_visual_area = false;

        int char_attr = 0;                      /* attributes for next character */
        boolean attr_pri = false;               /* char_attr has priority */
        boolean area_highlighting = false;      /* Visual or incsearch highlighting in this line */
        int attr = 0;                           /* attributes for area highlighting */
        int area_attr = 0;                      /* attributes desired by highlighting */
        int search_attr = 0;                    /* attributes desired by 'hlsearch' */
        int vcol_save_attr = 0;                 /* saved attr for 'cursorcolumn' */
        int syntax_attr = 0;                    /* attributes desired by syntax */
        boolean has_syntax = false;             /* this buffer has syntax highl. */
        int eol_hl_off = 0;                     /* 1 if highlighted char after EOL */
        int multi_attr = 0;                     /* attributes desired by multibyte */
        int mb_l = 1;                           /* multi-byte byte length */
        int mb_c = 0;                           /* decoded multi-byte character */
        boolean mb_utf8 = false;                /* screen char is UTF-8 char */
        int[] u8cc = new int[MAX_MCO];          /* composing UTF-8 chars */
        int trailcol = MAXCOL;                  /* start of trailing spaces */
        boolean need_showbreak = false;

        int line_attr = 0;                      /* attribute for the whole line */
        int did_line_attr = 0;

        /* draw_state: items that are drawn in sequence: */
        final int
            WL_START = 0,                       /* nothing done yet */
            WL_CMDLINE = WL_START + 1,          /* cmdline window column */
            WL_NR = WL_CMDLINE + 1,             /* line number */
            WL_BRI = WL_NR + 1,                 /* 'breakindent' */
            WL_SBR = WL_BRI + 1,                /* 'showbreak' or 'diff' */
            WL_LINE = WL_SBR + 1;               /* text in the line */

        int draw_state = WL_START;              /* what to draw next */

        int syntax_flags = 0;
        int[] syntax_seqnr = { 0 };
        int prev_syntax_id = 0;
        int conceal_attr = hl_attr(HLF_CONCEAL);
        boolean is_concealing = false;
        int boguscols = 0;                      /* nonexistent columns added to force wrapping */
        int vcol_off = 0;                       /* offset for concealed characters */
        boolean did_wcol = false;
        int old_boguscols = 0;

        if (endrow < startrow)                  /* past the end already! */
            return startrow;

        int row = startrow;                     /* row in the window, excl w_winrow */
        int screen_row = row + wp.w_winrow;     /* row on the screen, incl w_winrow */

        /*
         * To speed up the loop below, set extra_check when there is linebreak,
         * trailing white space and/or syntax processing to be done.
         */
        boolean extra_check = wp.w_onebuf_opt.wo_lbr[0];  /* has syntax or linebreak */
        if (syntax_present(wp) && !wp.w_s.b_syn_error)
        {
            /* Prepare for syntax highlighting in this line.
             * When there is an error, stop syntax highlighting. */
            boolean save_did_emsg = did_emsg;
            did_emsg = false;
            syntax_start(wp, lnum);
            if (did_emsg)
                wp.w_s.b_syn_error = true;
            else
            {
                did_emsg = save_did_emsg;
                has_syntax = true;
                extra_check = true;
            }
        }

        /* Check for columns to display for 'colorcolumn'. */
        int[] color_cols = wp.w_p_cc_cols, cci = { 0 };                /* pointer to according columns array */
        boolean draw_color_col = false;         /* highlight colorcolumn */
        if (color_cols != null)
            draw_color_col = advance_color_col(vcol - vcol_off, color_cols, cci);

        /*
         * handle visual active in this window
         */
        int[] fromcol = { -10 }, tocol = { MAXCOL };                     /* start/end of inverting */
        if (VIsual_active && wp.w_buffer == curwin.w_buffer)
        {
            if (ltoreq(curwin.w_cursor, VIsual))        /* Visual is after curwin.w_cursor */
            {
                top = curwin.w_cursor;
                bot = VIsual;
            }
            else                                        /* Visual is before curwin.w_cursor */
            {
                top = VIsual;
                bot = curwin.w_cursor;
            }

            lnum_in_visual_area = (top.lnum <= lnum && lnum <= bot.lnum);

            if (VIsual_mode == Ctrl_V)                  /* block mode */
            {
                if (lnum_in_visual_area)
                {
                    fromcol[0] = wp.w_old_cursor_fcol;
                    tocol[0] = wp.w_old_cursor_lcol;
                }
            }
            else                                        /* non-block mode */
            {
                if (top.lnum < lnum && lnum <= bot.lnum)
                    fromcol[0] = 0;
                else if (lnum == top.lnum)
                {
                    if (VIsual_mode == 'V')             /* linewise */
                        fromcol[0] = 0;
                    else
                    {
                        getvvcol(wp, top, fromcol, null, null);
                        if (gchar_pos(top) == NUL)
                            tocol[0] = fromcol[0] + 1;
                    }
                }
                if (VIsual_mode != 'V' && lnum == bot.lnum)
                {
                    if (p_sel[0].at(0) == (byte)'e' && bot.col == 0 && bot.coladd == 0)
                    {
                        fromcol[0] = -10;
                        tocol[0] = MAXCOL;
                    }
                    else if (bot.col == MAXCOL)
                        tocol[0] = MAXCOL;
                    else
                    {
                        pos_C pos = new pos_C();
                        COPY_pos(pos, bot);
                        if (p_sel[0].at(0) == (byte)'e')
                            getvvcol(wp, pos, tocol, null, null);
                        else
                        {
                            getvvcol(wp, pos, null, null, tocol);
                            tocol[0]++;
                        }
                    }
                }
            }

            /* Check if the character under the cursor should not be inverted. */
            if (!highlight_match && lnum == curwin.w_cursor.lnum && wp == curwin)
                noinvcur = true;

            /* if inverting in this line set area_highlighting */
            if (0 <= fromcol[0])
            {
                area_highlighting = true;
                attr = hl_attr(HLF_V);
            }
        }
        /*
         * handle 'incsearch' and ":s///c" highlighting
         */
        else if (highlight_match
                && wp == curwin
                && curwin.w_cursor.lnum <= lnum
                && lnum <= curwin.w_cursor.lnum + search_match_lines)
        {
            if (lnum == curwin.w_cursor.lnum)
                getvcol(curwin, curwin.w_cursor, fromcol, null, null);
            else
                fromcol[0] = 0;
            if (lnum == curwin.w_cursor.lnum + search_match_lines)
            {
                pos_C pos = new pos_C();
                pos.lnum = lnum;
                pos.col = search_match_endcol;

                getvcol(curwin, pos, tocol, null, null);
            }
            else
                tocol[0] = MAXCOL;
            /* do at least one character; happens when past end of line */
            if (fromcol[0] == tocol[0])
                tocol[0] = fromcol[0] + 1;
            area_highlighting = true;
            attr = hl_attr(HLF_I);
        }

        if (line_attr != 0)
            area_highlighting = true;

        Bytes line = ml_get_buf(wp.w_buffer, lnum, false); /* current line */
        Bytes ptr = line;                                  /* current position in "line" */

        /* find start of trailing whitespace */
        if (wp.w_onebuf_opt.wo_list[0] && lcs_trail[0] != NUL)
        {
            trailcol = strlen(ptr);
            while (0 < trailcol && vim_iswhite(ptr.at(trailcol - 1)))
                --trailcol;
            trailcol += BDIFF(ptr, line);
            extra_check = true;
        }

        /*
         * 'nowrap' or 'wrap' and a single line that doesn't fit:
         * advance to the first character to be displayed.
         */
        int v;
        if (wp.w_onebuf_opt.wo_wrap[0])
            v = wp.w_skipcol;
        else
            v = wp.w_leftcol;
        if (0 < v)
        {
            Bytes prev_ptr = ptr;
            while (vcol < v && ptr.at(0) != NUL)
            {
                c = win_lbr_chartabsize(wp, line, ptr, vcol, null);
                vcol += c;
                prev_ptr = ptr;
                ptr = ptr.plus(us_ptr2len_cc(ptr));
            }

            /* When:
             * - 'cuc' is set, or
             * - 'colorcolumn' is set, or
             * - 'virtualedit' is set, or
             * - the visual mode is active,
             * the end of the line may be before the start of the displayed part.
             */
            if (vcol < v && (wp.w_onebuf_opt.wo_cuc[0] || draw_color_col || virtual_active()
                                || (VIsual_active && wp.w_buffer == curwin.w_buffer)))
            {
                vcol = v;
            }

            /* Handle a character that's not completely on the screen:
             * put 'ptr' at that character, but skip the first few screen characters. */
            if (v < vcol)
            {
                vcol -= c;
                ptr = prev_ptr;
                n_skip = v - vcol;
            }

            /*
             * Adjust for when the inverted text is before the screen,
             * and when the start of the inverted text is before the screen.
             */
            if (tocol[0] <= vcol)
                fromcol[0] = 0;
            else if (0 <= fromcol[0] && fromcol[0] < vcol)
                fromcol[0] = vcol;

            /* When w_skipcol is non-zero, first line needs 'showbreak'. */
            if (wp.w_onebuf_opt.wo_wrap[0])
                need_showbreak = true;
        }

        /*
         * Correct highlighting for cursor that can't be disabled.
         * Avoids having to check this for each character.
         */
        if (0 <= fromcol[0])
        {
            if (noinvcur)
            {
                if (fromcol[0] == wp.w_virtcol)
                {
                    /* highlighting starts at cursor, let it start just after the cursor */
                    fromcol_prev = fromcol[0];
                    fromcol[0] = -1;
                }
                else if (fromcol[0] < wp.w_virtcol)
                    /* restart highlighting after the cursor */
                    fromcol_prev = wp.w_virtcol;
            }
            if (tocol[0] <= fromcol[0])
                fromcol[0] = -1;
        }

        /*
         * Handle highlighting the last used search pattern and matches.
         * Do this for both search_hl and the match list.
         */
        matchitem_C mi = wp.w_match_head;   /* points to the match list */
        boolean shl_flag = false;           /* whether search_hl has been processed */
        while (mi != null || !shl_flag)
        {
            match_C shl;                    /* points to search_hl or a match */
            if (!shl_flag)
            {
                shl = search_hl;
                shl_flag = true;
            }
            else
                shl = mi.mi_hl;
            shl.startcol = MAXCOL;
            shl.endcol = MAXCOL;
            shl.attr_cur = 0;
            v = BDIFF(ptr, line);
            if (mi != null)
                mi.mi_pos.cur = 0;
            next_search_hl(wp, shl, lnum, v, mi);

            /* Need to get the line again, a multi-line regexp may have made it invalid. */
            line = ml_get_buf(wp.w_buffer, lnum, false);
            ptr = line.plus(v);

            if (shl.lnum != 0 && shl.lnum <= lnum)
            {
                if (shl.lnum == lnum)
                    shl.startcol = shl.rmm.startpos[0].col;
                else
                    shl.startcol = 0;
                if (lnum == shl.lnum + shl.rmm.endpos[0].lnum - shl.rmm.startpos[0].lnum)
                    shl.endcol = shl.rmm.endpos[0].col;
                else
                    shl.endcol = MAXCOL;
                /* Highlight one character for an empty match. */
                if (shl.startcol == shl.endcol)
                {
                    if (line.at(shl.endcol) != NUL)
                        shl.endcol += us_ptr2len_cc(line.plus(shl.endcol));
                    else
                        shl.endcol++;
                }
                if ((long)shl.startcol < v) /* match at leftcol */
                {
                    shl.attr_cur = shl.attr;
                    search_attr = shl.attr;
                }
                area_highlighting = true;
            }
            if (shl != search_hl && mi != null)
                mi = mi.next;
        }

        /* Cursor line highlighting for 'cursorline' in the current window.
         * Not when Visual mode is active, because it's not clear what is selected then. */
        if (wp.w_onebuf_opt.wo_cul[0] && lnum == wp.w_cursor.lnum && !(wp == curwin && VIsual_active))
        {
            line_attr = hl_attr(HLF_CUL);
            area_highlighting = true;
        }

        int col = 0;                                        /* visual column on screen */
        int off = BDIFF(current_ScreenLine, screenLines);  /* offset in screenLines/screenAttrs */
        if (wp.w_onebuf_opt.wo_rl[0])
        {
            /* Rightleft window: process the text in the normal direction,
             * but put it in current_ScreenLine[] from right to left.
             * Start at the rightmost column of the window. */
            col = wp.w_width - 1;
            off += col;
        }

        /*
         * Repeat for the whole displayed line.
         */
        for ( ; ; )
        {
            /* Skip this quickly when working on the text. */
            if (draw_state != WL_LINE)
            {
                if (draw_state == WL_CMDLINE - 1 && n_extra == 0)
                {
                    draw_state = WL_CMDLINE;
                    if (cmdwin_type != 0 && wp == curwin)
                    {
                        /* Draw the cmdline character. */
                        n_extra = 1;
                        c_extra = cmdwin_type;
                        char_attr = hl_attr(HLF_AT);
                    }
                }

                if (draw_state == WL_NR - 1 && n_extra == 0)
                {
                    draw_state = WL_NR;
                    /* Display the absolute or relative line number.
                     * After the first fill with blanks when the 'n' flag isn't in 'cpo'. */
                    if ((wp.w_onebuf_opt.wo_nu[0] || wp.w_onebuf_opt.wo_rnu[0])
                            && (row == startrow || vim_strbyte(p_cpo[0], CPO_NUMCOL) == null))
                    {
                        /* Draw the line number (empty space after wrapping). */
                        if (row == startrow)
                        {
                            long num;
                            Bytes fmt = u8("%*ld ");

                            if (wp.w_onebuf_opt.wo_nu[0] && !wp.w_onebuf_opt.wo_rnu[0])
                                /* 'number' + 'norelativenumber' */
                                num = lnum;
                            else
                            {
                                /* 'relativenumber', don't use negative numbers */
                                num = Math.abs(get_cursor_rel_lnum(wp, lnum));
                                if (num == 0 && wp.w_onebuf_opt.wo_nu[0] && wp.w_onebuf_opt.wo_rnu[0])
                                {
                                    /* 'number' + 'relativenumber' */
                                    num = lnum;
                                    fmt = u8("%-*ld ");
                                }
                            }

                            libC.sprintf(extra, fmt, number_width(wp), num);
                            if (0 < wp.w_skipcol)
                                for (p_extra = extra; p_extra.at(0) == (byte)' '; p_extra = p_extra.plus(1))
                                    p_extra.be(0, (byte)'-');
                            if (wp.w_onebuf_opt.wo_rl[0])             /* reverse line numbers */
                                rl_mirror(extra);
                            p_extra = extra;
                            c_extra = NUL;
                        }
                        else
                            c_extra = ' ';
                        n_extra = number_width(wp) + 1;
                        char_attr = hl_attr(HLF_N);
                        /* When 'cursorline' is set, highlight the line number of the current line differently.
                         * TODO: Can we use CursorLine instead of CursorLineNr when CursorLineNr isn't set? */
                        if ((wp.w_onebuf_opt.wo_cul[0] || wp.w_onebuf_opt.wo_rnu[0]) && lnum == wp.w_cursor.lnum)
                            char_attr = hl_attr(HLF_CLN);
                    }
                }

                if (wp.w_p_brisbr && draw_state == WL_BRI - 1 && n_extra == 0 && p_sbr[0].at(0) != NUL)
                    /* draw indent after showbreak value */
                    draw_state = WL_BRI;
                else if (wp.w_p_brisbr && draw_state == WL_SBR && n_extra == 0)
                    /* After the showbreak, draw the breakindent. */
                    draw_state = WL_BRI - 1;

                /* draw 'breakindent': indent wrapped text accordingly */
                if (draw_state == WL_BRI - 1 && n_extra == 0)
                {
                    draw_state = WL_BRI;
                    if (wp.w_onebuf_opt.wo_bri[0] && n_extra == 0 && row != startrow)
                    {
                        char_attr = 0; /* was: hl_attr(HLF_AT); */
                        p_extra = null;
                        c_extra = ' ';
                        n_extra = get_breakindent_win(wp, ml_get_buf(wp.w_buffer, lnum, false));
                        /* Correct end of highlighted area for 'breakindent',
                         * required when 'linebreak' is also set. */
                        if (tocol[0] == vcol)
                            tocol[0] += n_extra;
                    }
                }

                if (draw_state == WL_SBR - 1 && n_extra == 0)
                {
                    draw_state = WL_SBR;
                    if (p_sbr[0].at(0) != NUL && need_showbreak)
                    {
                        /* Draw 'showbreak' at the start of each broken line. */
                        p_extra = p_sbr[0];
                        c_extra = NUL;
                        n_extra = strlen(p_sbr[0]);
                        char_attr = hl_attr(HLF_AT);
                        need_showbreak = false;
                        vcol_sbr = vcol + us_charlen(p_sbr[0]);
                        /* Correct end of highlighted area for 'showbreak',
                         * required when 'linebreak' is also set. */
                        if (tocol[0] == vcol)
                            tocol[0] += n_extra;
                        /* combine 'showbreak' with 'cursorline' */
                        if (wp.w_onebuf_opt.wo_cul[0] && lnum == wp.w_cursor.lnum)
                            char_attr = hl_combine_attr(char_attr, hl_attr(HLF_CUL));
                    }
                }

                if (draw_state == WL_LINE - 1 && n_extra == 0)
                {
                    draw_state = WL_LINE;
                    if (saved_n_extra != 0)
                    {
                        /* Continue item from end of wrapped line. */
                        n_extra = saved_n_extra;
                        c_extra = saved_c_extra;
                        p_extra = saved_p_extra;
                        char_attr = saved_char_attr;
                    }
                    else
                        char_attr = 0;
                }
            }

            /* When still displaying '$' of change command, stop at cursor. */
            if (0 <= dollar_vcol && wp == curwin && lnum == wp.w_cursor.lnum && wp.w_virtcol <= vcol)
            {
                screen_line(screen_row, wp.w_wincol, col, -wp.w_width, wp.w_onebuf_opt.wo_rl[0]);
                /* Pretend we have finished updating the window,
                 * except when 'cursorcolumn' is set. */
                if (wp.w_onebuf_opt.wo_cuc[0])
                    row = wp.w_cline_row + wp.w_cline_height;
                else
                    row = wp.w_height;
                break;
            }

            if (draw_state == WL_LINE && area_highlighting)
            {
                /* handle Visual or match highlighting in this line */
                if (vcol == fromcol[0]
                        || (vcol + 1 == fromcol[0] && n_extra == 0
                            && 1 < us_ptr2cells(ptr))
                        || (vcol_prev == fromcol_prev
                            && vcol_prev < vcol             /* not at margin */
                            && vcol < tocol[0]))
                    area_attr = attr;                       /* start highlighting */
                else if (area_attr != 0
                        && (vcol == tocol[0] || (noinvcur && vcol == wp.w_virtcol)))
                    area_attr = 0;                          /* stop highlighting */

                if (n_extra == 0)
                {
                    /*
                     * Check for start/end of search pattern match.
                     * After end, check for start/end of next match.
                     * When another match, have to check for start again.
                     * Watch out for matching an empty string!
                     * Do this for 'search_hl' and the match list (ordered by priority).
                     */
                    v = BDIFF(ptr, line);
                    mi = wp.w_match_head;
                    shl_flag = false;
                    while (mi != null || !shl_flag)
                    {
                        match_C shl;        /* points to search_hl or a match */
                        if (!shl_flag && ((mi != null && SEARCH_HL_PRIORITY < mi.priority) || mi == null))
                        {
                            shl = search_hl;
                            shl_flag = true;
                        }
                        else
                            shl = mi.mi_hl;
                        if (mi != null)
                            mi.mi_pos.cur = 0;
                        /* marks that position match search is in progress */
                        boolean pos_inprogress = true;
                        while (shl.rmm.regprog != null || (mi != null && pos_inprogress))
                        {
                            if (shl.startcol != MAXCOL && shl.startcol <= v && v < shl.endcol)
                            {
                                int tmp_col = v + us_ptr2len_cc(ptr);

                                if (shl.endcol < tmp_col)
                                    shl.endcol = tmp_col;
                                shl.attr_cur = shl.attr;
                            }
                            else if (v == shl.endcol)
                            {
                                shl.attr_cur = 0;
                                next_search_hl(wp, shl, lnum, v, mi);
                                pos_inprogress = (mi != null && mi.mi_pos.cur != 0);

                                /* Need to get the line again, a multi-line regexp may have made it invalid. */
                                line = ml_get_buf(wp.w_buffer, lnum, false);
                                ptr = line.plus(v);

                                if (shl.lnum == lnum)
                                {
                                    shl.startcol = shl.rmm.startpos[0].col;
                                    if (shl.rmm.endpos[0].lnum == 0)
                                        shl.endcol = shl.rmm.endpos[0].col;
                                    else
                                        shl.endcol = MAXCOL;

                                    if (shl.startcol == shl.endcol)
                                    {
                                        /* highlight empty match, try again after it */
                                        shl.endcol += us_ptr2len_cc(line.plus(shl.endcol));
                                    }

                                    /* Loop to check if the match starts at the current position. */
                                    continue;
                                }
                            }
                            break;
                        }
                        if (shl != search_hl && mi != null)
                            mi = mi.next;
                    }

                    /* Use attributes from match with highest priority
                     * among 'search_hl' and the match list. */
                    search_attr = search_hl.attr_cur;
                    mi = wp.w_match_head;
                    shl_flag = false;
                    while (mi != null || !shl_flag)
                    {
                        match_C shl;        /* points to search_hl or a match */
                        if (!shl_flag && ((mi != null && SEARCH_HL_PRIORITY < mi.priority) || mi == null))
                        {
                            shl = search_hl;
                            shl_flag = true;
                        }
                        else
                            shl = mi.mi_hl;
                        if (shl.attr_cur != 0)
                            search_attr = shl.attr_cur;
                        if (shl != search_hl && mi != null)
                            mi = mi.next;
                    }
                }

                /* Decide which of the highlight attributes to use. */
                attr_pri = true;

                if (area_attr != 0)
                    char_attr = hl_combine_attr(line_attr, area_attr);
                else if (search_attr != 0)
                    char_attr = hl_combine_attr(line_attr, search_attr);
                    /* Use line_attr when not in the Visual or 'incsearch' area
                     * (area_attr may be 0 when "noinvcur" is set). */
                else if (line_attr != 0 && ((fromcol[0] == -10 && tocol[0] == MAXCOL)
                                        || vcol < fromcol[0] || vcol_prev < fromcol_prev || tocol[0] <= vcol))
                    char_attr = line_attr;
                else
                {
                    attr_pri = false;
                    if (has_syntax)
                        char_attr = syntax_attr;
                    else
                        char_attr = 0;
                }
            }

            /*
             * Get the next character to put on the screen.
             */
            /*
             * The "p_extra" points to the extra stuff that is inserted to represent
             * special characters (non-printable stuff) and other things.
             * When all characters are the same, c_extra is used.
             * "p_extra" must end in a NUL to avoid us_ptr2len_cc() reads past "p_extra[n_extra]".
             * For the '$' of the 'list' option, n_extra == 1, p_extra == "".
             */
            if (0 < n_extra)
            {
                if (c_extra != NUL)
                {
                    c = c_extra;
                    mb_c = c;       /* doesn't handle non-utf-8 multi-byte! */
                    if (1 < utf_char2len(c))
                    {
                        mb_utf8 = true;
                        u8cc[0] = 0;
                        c = 0xc0;
                    }
                    else
                        mb_utf8 = false;
                }
                else
                {
                    c = p_extra.at(0);

                    mb_c = c;

                    /* If the UTF-8 character is more than one byte, decode it into "mb_c". */
                    mb_l = us_ptr2len_cc(p_extra);
                    mb_utf8 = false;
                    if (n_extra < mb_l)
                        mb_l = 1;
                    else if (1 < mb_l)
                    {
                        mb_c = us_ptr2char_cc(p_extra, u8cc);
                        mb_utf8 = true;
                        c = 0xc0;
                    }

                    if (mb_l == 0)  /* at the NUL at end-of-line */
                        mb_l = 1;

                    /* If a double-width char doesn't fit display a '>' in the last column. */
                    if ((wp.w_onebuf_opt.wo_rl[0] ? (col <= 0) : (wp.w_width - 1 <= col)) && utf_char2cells(mb_c) == 2)
                    {
                        c = '>';
                        mb_c = c;
                        mb_l = 1;
                        mb_utf8 = false;
                        multi_attr = hl_attr(HLF_AT);
                        /* put the pointer back to output the double-width
                         * character at the start of the next line */
                        n_extra++;
                        p_extra = p_extra.minus(1);
                    }
                    else
                    {
                        n_extra -= mb_l - 1;
                        p_extra = p_extra.plus(mb_l - 1);
                    }

                    p_extra = p_extra.plus(1);
                }
                --n_extra;
            }
            else
            {
                if (p_extra_free != null)
                    p_extra_free = null;

                /*
                 * Get a character from the line itself.
                 */
                c = ptr.at(0);

                mb_c = c;

                /* If the UTF-8 character is more than one byte, decode it into "mb_c". */
                mb_l = us_ptr2len_cc(ptr);
                mb_utf8 = false;
                if (1 < mb_l)
                {
                    mb_c = us_ptr2char_cc(ptr, u8cc);
                    /* Overlong encoded ASCII or ASCII with composing char
                     * is displayed normally, except a NUL. */
                    if (mb_c < 0x80)
                        c = mb_c;
                    mb_utf8 = true;

                    /* At start of the line we can have a composing char.
                     * Draw it as a space with a composing char. */
                    if (utf_iscomposing(mb_c))
                    {
                        for (int i = screen_mco - 1; 0 < i; --i)
                            u8cc[i] = u8cc[i - 1];
                        u8cc[0] = mb_c;
                        mb_c = ' ';
                    }
                }

                if ((mb_l == 1 && 0x80 <= c)
                        || (1 <= mb_l && mb_c == 0)
                        || (1 < mb_l && !vim_isprintc(mb_c)))
                {
                    /*
                     * Illegal UTF-8 byte: display as <xx>.
                     * Non-BMP character : display as ? or fullwidth ?.
                     */
                    {
                        transchar_hex(extra, mb_c);
                        if (wp.w_onebuf_opt.wo_rl[0])             /* reverse */
                            rl_mirror(extra);
                    }

                    p_extra = extra;
                    c = p_extra.at(0);
                    { Bytes[] __ = { p_extra }; mb_c = us_ptr2char_adv(__, true); p_extra = __[0]; }
                    mb_utf8 = (0x80 <= c);
                    n_extra = strlen(p_extra);
                    c_extra = NUL;
                    if (area_attr == 0 && search_attr == 0)
                    {
                        n_attr = n_extra + 1;
                        extra_attr = hl_attr(HLF_8);
                        saved_attr2 = char_attr;    /* save current attr */
                    }
                }
                else if (mb_l == 0)                 /* at the NUL at end-of-line */
                    mb_l = 1;

                /* If a double-width char doesn't fit, display a '>' in the last column;
                 * the character is displayed at the start of the next line. */
                if ((wp.w_onebuf_opt.wo_rl[0] ? (col <= 0) : (wp.w_width - 1 <= col)) && utf_char2cells(mb_c) == 2)
                {
                    c = '>';
                    mb_c = c;
                    mb_utf8 = false;
                    mb_l = 1;
                    multi_attr = hl_attr(HLF_AT);
                    /* Put pointer back so that the character will be
                     * displayed at the start of the next line. */
                    ptr = ptr.minus(1);
                }
                else if (ptr.at(0) != NUL)
                    ptr = ptr.plus(mb_l - 1);

                /* If a double-width char doesn't fit at the left side, display a '<'
                 * in the first column.  Don't do this for unprintable characters. */
                if (0 < n_skip && 1 < mb_l && n_extra == 0)
                {
                    n_extra = 1;
                    c_extra = MB_FILLER_CHAR;
                    c = ' ';
                    if (area_attr == 0 && search_attr == 0)
                    {
                        n_attr = n_extra + 1;
                        extra_attr = hl_attr(HLF_AT);
                        saved_attr2 = char_attr;    /* save current attr */
                    }
                    mb_c = c;
                    mb_utf8 = false;
                    mb_l = 1;
                }

                ptr = ptr.plus(1);

                /* 'list' : change char 0xa0 to lcs_nbsp. */
                if (wp.w_onebuf_opt.wo_list[0] && (c == 0xa0 || (mb_utf8 && mb_c == 0xa0)) && lcs_nbsp[0] != NUL)
                {
                    c = lcs_nbsp[0];
                    if (area_attr == 0 && search_attr == 0)
                    {
                        n_attr = 1;
                        extra_attr = hl_attr(HLF_8);
                        saved_attr2 = char_attr;    /* save current attr */
                    }
                    mb_c = c;
                    if (1 < utf_char2len(c))
                    {
                        mb_utf8 = true;
                        u8cc[0] = 0;
                        c = 0xc0;
                    }
                    else
                        mb_utf8 = false;
                }

                if (extra_check)
                {
                    /* Get syntax attribute, unless still at the start of the line
                     * (double-wide char that doesn't fit). */
                    v = BDIFF(ptr, line);
                    if (has_syntax && 0 < v)
                    {
                        /* Get the syntax attribute for the character.
                         * If there is an error, disable syntax highlighting. */
                        boolean save_did_emsg = did_emsg;
                        did_emsg = false;

                        syntax_attr = get_syntax_attr(v - 1, false);

                        if (did_emsg)
                        {
                            wp.w_s.b_syn_error = true;
                            has_syntax = false;
                        }
                        else
                            did_emsg = save_did_emsg;

                        /* Need to get the line again,
                         * a multi-line regexp may have made it invalid. */
                        line = ml_get_buf(wp.w_buffer, lnum, false);
                        ptr = line.plus(v);

                        if (!attr_pri)
                            char_attr = syntax_attr;
                        else
                            char_attr = hl_combine_attr(syntax_attr, char_attr);
                        /* no concealing past the end of the line,
                         * it interferes with line highlighting */
                        if (c == NUL)
                            syntax_flags = 0;
                        else
                            syntax_flags = get_syntax_info(syntax_seqnr);
                    }

                    /*
                     * Found last space before word: check for line break.
                     */
                    if (wp.w_onebuf_opt.wo_lbr[0] && breakat_flags[char_u((byte)c)] && !breakat_flags[char_u(ptr.at(0))])
                    {
                        int mb_off = us_head_off(line, ptr.minus(1));
                        Bytes p = ptr.minus(mb_off + 1);

                        /* TODO: is passing 'p' for start of the line OK? */
                        n_extra = win_lbr_chartabsize(wp, line, p, vcol, null) - 1;
                        if (c == TAB && wp.w_width < n_extra + col)
                            n_extra = (int)wp.w_buffer.b_p_ts[0] - vcol % (int)wp.w_buffer.b_p_ts[0] - 1;

                        c_extra = (0 < mb_off) ? MB_FILLER_CHAR : ' ';
                        if (vim_iswhite(c))
                        {
                            if (c == TAB)       /* See "Tab alignment" below. */
                            {
                                n_extra += vcol_off;
                                vcol -= vcol_off;
                                vcol_off = 0;
                                col -= boguscols;
                                old_boguscols = boguscols;
                                boguscols = 0;
                            }
                            if (!wp.w_onebuf_opt.wo_list[0])
                                c = ' ';
                        }
                    }

                    if (trailcol != MAXCOL && BLT(line.plus(trailcol), ptr) && c == ' ')
                    {
                        c = lcs_trail[0];
                        if (!attr_pri)
                        {
                            n_attr = 1;
                            extra_attr = hl_attr(HLF_8);
                            saved_attr2 = char_attr;    /* save current attr */
                        }
                        mb_c = c;
                        if (1 < utf_char2len(c))
                        {
                            mb_utf8 = true;
                            u8cc[0] = 0;
                            c = 0xc0;
                        }
                        else
                            mb_utf8 = false;
                    }
                }

                /*
                 * Handling of non-printable characters.
                 */
                if ((chartab[c & 0xff] & CT_PRINT_CHAR) == 0)
                {
                    /*
                     * When getting a character from the file, we may have to turn it
                     * into something else on the way to putting it into "screenLines".
                     */
                    if (c == TAB && (!wp.w_onebuf_opt.wo_list[0] || lcs_tab1[0] != NUL))
                    {
                        int tab_len = 0;
                        int vcol_adjusted = vcol; /* removed showbreak length */
                        /* Only adjust the "tab_len" when at the first column
                         * after the showbreak value was drawn. */
                        if (p_sbr[0].at(0) != NUL && vcol == vcol_sbr && wp.w_onebuf_opt.wo_wrap[0])
                            vcol_adjusted = vcol - us_charlen(p_sbr[0]);
                        /* tab amount depends on current column */
                        tab_len = (int)wp.w_buffer.b_p_ts[0] - vcol_adjusted % (int)wp.w_buffer.b_p_ts[0] - 1;

                        if (!wp.w_onebuf_opt.wo_lbr[0] || !wp.w_onebuf_opt.wo_list[0])
                            /* tab amount depends on current column */
                            n_extra = tab_len;
                        else
                        {
                            int len = n_extra;
                            int saved_nextra = n_extra;

                            if (0 < vcol_off)
                                /* there are characters to conceal */
                                tab_len += vcol_off;
                            /* boguscols before FIX_FOR_BOGUSCOLS macro from above */
                            if (wp.w_onebuf_opt.wo_list[0]
                                        && lcs_tab1[0] != NUL && 0 < old_boguscols && tab_len < n_extra)
                                tab_len += n_extra - tab_len;

                            /* if n_extra > 0, it gives the number of chars, to use for a tab,
                             * else we need to calculate the width for a tab */
                            len = tab_len * utf_char2len(lcs_tab2[0]);
                            if (0 < n_extra)
                                len += n_extra - tab_len;
                            c = lcs_tab1[0];
                            Bytes p = new Bytes(len + 1);
                            BFILL(p, 0, (byte)' ', len);
                            p.be(len, NUL);
                            p_extra_free = p;
                            for (int i = 0; i < tab_len; i++)
                            {
                                utf_char2bytes(lcs_tab2[0], p);
                                p = p.plus(utf_char2len(lcs_tab2[0]));
                                n_extra += utf_char2len(lcs_tab2[0]) - (0 < saved_nextra ? 1 : 0);
                            }
                            p_extra = p_extra_free;
                            /* n_extra will be increased by FIX_FOX_BOGUSCOLS
                             * macro below, so need to adjust for that here */
                            if (0 < vcol_off)
                                n_extra -= vcol_off;
                        }

                        {
                            int vc_saved = vcol_off;

                            /* Tab alignment should be identical regardless of
                             * 'conceallevel' value.  So tab compensates of all
                             * previous concealed characters, and thus resets
                             * vcol_off and boguscols accumulated so far in the
                             * line.  Note that the tab can be longer than
                             * 'tabstop' when there are concealed characters. */
                            {
                                n_extra += vcol_off;
                                vcol -= vcol_off;
                                vcol_off = 0;
                                col -= boguscols;
                                old_boguscols = boguscols;
                                boguscols = 0;
                            }

                            /* Make sure, the highlighting for the tab char will be
                             * correctly set further below (effectively reverts the
                             * FIX_FOR_BOGUSCOLS macro */
                            if (n_extra == tab_len + vc_saved && wp.w_onebuf_opt.wo_list[0] && lcs_tab1[0] != NUL)
                                tab_len += vc_saved;
                        }

                        mb_utf8 = false;                    /* don't draw as UTF-8 */
                        if (wp.w_onebuf_opt.wo_list[0])
                        {
                            c = lcs_tab1[0];
                            if (wp.w_onebuf_opt.wo_lbr[0])
                                c_extra = NUL;              /* using "p_extra" from above */
                            else
                                c_extra = lcs_tab2[0];
                            n_attr = tab_len + 1;
                            extra_attr = hl_attr(HLF_8);
                            saved_attr2 = char_attr;        /* save current attr */
                            mb_c = c;
                            if (1 < utf_char2len(c))
                            {
                                mb_utf8 = true;
                                u8cc[0] = 0;
                                c = 0xc0;
                            }
                        }
                        else
                        {
                            c_extra = ' ';
                            c = ' ';
                        }
                    }
                    else if (c == NUL
                            && ((wp.w_onebuf_opt.wo_list[0] && 0 < lcs_eol[0])
                                || ((0 <= fromcol[0] || 0 <= fromcol_prev)
                                    && vcol < tocol[0]
                                    && VIsual_mode != Ctrl_V
                                    && (wp.w_onebuf_opt.wo_rl[0] ? (0 <= col) : (col < wp.w_width))
                                    && !(noinvcur
                                        && lnum == wp.w_cursor.lnum
                                        && vcol == wp.w_virtcol)))
                            && 0 <= lcs_eol_one)
                    {
                        /* Display a '$' after the line or highlight an extra character
                         * if the line break is included. */

                        /* For a diff line the highlighting continues after the "$". */
                        if (line_attr == 0)
                        {
                            /* In virtualedit, visual selections may extend beyond end of line. */
                            if (area_highlighting && virtual_active() && tocol[0] != MAXCOL && vcol < tocol[0])
                                n_extra = 0;
                            else
                            {
                                p_extra = at_end_str;
                                n_extra = 1;
                                c_extra = NUL;
                            }
                        }
                        if (wp.w_onebuf_opt.wo_list[0])
                            c = lcs_eol[0];
                        else
                            c = ' ';
                        lcs_eol_one = -1;
                        ptr = ptr.minus(1);                              /* put it back at the NUL */
                        if (!attr_pri)
                        {
                            extra_attr = hl_attr(HLF_AT);
                            n_attr = 1;
                        }
                        mb_c = c;
                        if (1 < utf_char2len(c))
                        {
                            mb_utf8 = true;
                            u8cc[0] = 0;
                            c = 0xc0;
                        }
                        else
                            mb_utf8 = false;                /* don't draw as UTF-8 */
                    }
                    else if (c != NUL)
                    {
                        p_extra = transchar(c);
                        if (n_extra == 0)
                            n_extra = mb_byte2cells((byte)c) - 1;
                        if ((dy_flags[0] & DY_UHEX) != 0 && wp.w_onebuf_opt.wo_rl[0])
                            rl_mirror(p_extra);             /* reverse "<12>" */
                        c_extra = NUL;
                        if (wp.w_onebuf_opt.wo_lbr[0])
                        {
                            Bytes p;

                            c = p_extra.at(0);
                            p = new Bytes(n_extra + 1);
                            BFILL(p, 0, (byte)' ', n_extra);
                            STRNCPY(p, p_extra.plus(1), strlen(p_extra) - 1);
                            p.be(n_extra, NUL);
                            p_extra_free = p_extra = p;
                        }
                        else
                        {
                            n_extra = mb_byte2cells((byte)c) - 1;
                            c = (p_extra = p_extra.plus(1)).at(-1);
                        }
                        if (!attr_pri)
                        {
                            n_attr = n_extra + 1;
                            extra_attr = hl_attr(HLF_8);
                            saved_attr2 = char_attr;        /* save current attr */
                        }
                        mb_utf8 = false;                    /* don't draw as UTF-8 */
                    }
                    else if (VIsual_active
                             && (VIsual_mode == Ctrl_V || VIsual_mode == 'v')
                             && virtual_active()
                             && tocol[0] != MAXCOL
                             && vcol < tocol[0]
                             && (wp.w_onebuf_opt.wo_rl[0] ? (0 <= col) : (col < wp.w_width)))
                    {
                        c = ' ';
                        ptr = ptr.minus(1);                              /* put it back at the NUL */
                    }
                    else if ((line_attr != 0)
                        && (wp.w_onebuf_opt.wo_rl[0] ? (0 <= col) : (col - boguscols < wp.w_width)))
                    {
                        /* Highlight until the right side of the window. */
                        c = ' ';
                        ptr = ptr.minus(1);                              /* put it back at the NUL */

                        /* Remember we do the char for line highlighting. */
                        did_line_attr++;

                        /* don't do search HL for the rest of the line */
                        if (line_attr != 0 && char_attr == search_attr && 0 < col)
                            char_attr = line_attr;
                    }
                }

                if (0 < wp.w_onebuf_opt.wo_cole[0]
                    && (wp != curwin || lnum != wp.w_cursor.lnum || conceal_cursor_line(wp))
                    && (syntax_flags & HL_CONCEAL) != 0
                    && !(lnum_in_visual_area && vim_strchr(wp.w_onebuf_opt.wo_cocu[0], 'v') == null))
                {
                    char_attr = conceal_attr;
                    if (prev_syntax_id != syntax_seqnr[0]
                            && (syn_get_sub_char() != NUL || wp.w_onebuf_opt.wo_cole[0] == 1)
                            && wp.w_onebuf_opt.wo_cole[0] != 3)
                    {
                        /* First time at this concealed item: display one character. */
                        if (syn_get_sub_char() != NUL)
                            c = syn_get_sub_char();
                        else if (lcs_conceal[0] != NUL)
                            c = lcs_conceal[0];
                        else
                            c = ' ';

                        prev_syntax_id = syntax_seqnr[0];

                        if (0 < n_extra)
                            vcol_off += n_extra;
                        vcol += n_extra;
                        if (wp.w_onebuf_opt.wo_wrap[0] && 0 < n_extra)
                        {
                            if (wp.w_onebuf_opt.wo_rl[0])
                            {
                                col -= n_extra;
                                boguscols -= n_extra;
                            }
                            else
                            {
                                boguscols += n_extra;
                                col += n_extra;
                            }
                        }
                        n_extra = 0;
                        n_attr = 0;
                    }
                    else if (n_skip == 0)
                    {
                        is_concealing = true;
                        n_skip = 1;
                    }
                    mb_c = c;
                    if (1 < utf_char2len(c))
                    {
                        mb_utf8 = true;
                        u8cc[0] = 0;
                        c = 0xc0;
                    }
                    else
                        mb_utf8 = false;    /* don't draw as UTF-8 */
                }
                else
                {
                    prev_syntax_id = 0;
                    is_concealing = false;
                }
            }

            /* In the cursor line and we may be concealing characters:
             * correct the cursor column when we reach its position. */
            if (!did_wcol && draw_state == WL_LINE
                    && wp == curwin && lnum == wp.w_cursor.lnum
                    && conceal_cursor_line(wp)
                    && wp.w_virtcol <= vcol + n_skip)
            {
                wp.w_wcol = col - boguscols;
                wp.w_wrow = row;
                did_wcol = true;
            }

            /* Don't override visual selection highlighting. */
            if (0 < n_attr
                    && draw_state == WL_LINE
                    && !attr_pri)
                char_attr = extra_attr;

            /*
             * Handle the case where we are in column 0 but not on the first
             * character of the line and the user wants us to show us a
             * special character (via 'listchars' option "precedes:<char>".
             */
            if (lcs_prec_todo != NUL
                    && wp.w_onebuf_opt.wo_list[0]
                    && (wp.w_onebuf_opt.wo_wrap[0] ? 0 < wp.w_skipcol : 0 < wp.w_leftcol)
                    && WL_NR < draw_state
                    && c != NUL)
            {
                c = lcs_prec[0];
                lcs_prec_todo = NUL;
                if (1 < utf_char2cells(mb_c))
                {
                    /* Double-width character being overwritten by the "precedes"
                     * character, need to fill up half the character. */
                    c_extra = MB_FILLER_CHAR;
                    n_extra = 1;
                    n_attr = 2;
                    extra_attr = hl_attr(HLF_AT);
                }
                mb_c = c;
                if (1 < utf_char2len(c))
                {
                    mb_utf8 = true;
                    u8cc[0] = 0;
                    c = 0xc0;
                }
                else
                    mb_utf8 = false;                /* don't draw as UTF-8 */
                if (!attr_pri)
                {
                    saved_attr3 = char_attr;        /* save current attr */
                    char_attr = hl_attr(HLF_AT);    /* later copied to char_attr */
                    n_attr3 = 1;
                }
            }

            /*
             * At end of the text line or just after the last character.
             */
            if (c == NUL || did_line_attr == 1)
            {
                long prevcol = BDIFF(ptr, line) - ((c == NUL) ? 1 : 0);

                /* we're not really at that column when skipping some text */
                if (prevcol < (long)(wp.w_onebuf_opt.wo_wrap[0] ? wp.w_skipcol : wp.w_leftcol))
                    prevcol++;

                /* Invert at least one char, used for Visual and empty line or highlight
                 * match at end of line.  If it's beyond the last char on the screen,
                 * just overwrite that one (tricky!)
                 * Not needed when a '$' was displayed for 'list'.
                 */
                boolean prevcol_hl_flag = false;                /* whether prevcol equals startcol of
                                                                 * search_hl or one of the matches */
                if (prevcol == (long)search_hl.startcol)
                    prevcol_hl_flag = true;
                else
                {
                    for (mi = wp.w_match_head; mi != null; mi = mi.next)
                        if (prevcol == (long)mi.mi_hl.startcol)
                        {
                            prevcol_hl_flag = true;
                            break;
                        }
                }
                if (lcs_eol[0] == lcs_eol_one
                        && ((area_attr != 0 && vcol == fromcol[0]
                            && (VIsual_mode != Ctrl_V || lnum == VIsual.lnum || lnum == curwin.w_cursor.lnum)
                            && c == NUL)
                                /* highlight 'hlsearch' match at end of line */
                                || (prevcol_hl_flag == true && did_line_attr <= 1)))
                {
                    int n = 0;

                    if (wp.w_onebuf_opt.wo_rl[0])
                    {
                        if (col < 0)
                            n = 1;
                    }
                    else
                    {
                        if (wp.w_width <= col)
                            n = -1;
                    }
                    if (n != 0)
                    {
                        /* At the window boundary, highlight the last character
                         * instead (better than nothing). */
                        off += n;
                        col += n;
                    }
                    else
                    {
                        /* Add a blank character to highlight. */
                        screenLines.be(off, (byte)' ');
                        screenLinesUC[off] = 0;
                    }
                    if (area_attr == 0)
                    {
                        /* Use attributes from match with highest priority
                         * among 'search_hl' and the match list. */
                        char_attr = search_hl.attr;
                        mi = wp.w_match_head;
                        shl_flag = false;
                        while (mi != null || !shl_flag)
                        {
                            match_C shl;        /* points to search_hl or a match */
                            if (!shl_flag && ((mi != null && SEARCH_HL_PRIORITY < mi.priority) || mi == null))
                            {
                                shl = search_hl;
                                shl_flag = true;
                            }
                            else
                                shl = mi.mi_hl;
                            if (BDIFF(ptr, line) - 1 == shl.startcol)
                                char_attr = shl.attr;
                            if (shl != search_hl && mi != null)
                                mi = mi.next;
                        }
                    }
                    screenAttrs[off] = char_attr;
                    if (wp.w_onebuf_opt.wo_rl[0])
                    {
                        --col;
                        --off;
                    }
                    else
                    {
                        col++;
                        off++;
                    }
                    vcol++;
                    eol_hl_off = 1;
                }
            }

            /*
             * At end of the text line.
             */
            if (c == NUL)
            {
                if (0 < eol_hl_off
                    && vcol - eol_hl_off == wp.w_virtcol
                    && lnum == wp.w_cursor.lnum)
                {
                    /* highlight last char after line */
                    --col;
                    --off;
                    --vcol;
                }

                /* Highlight 'cursorcolumn' & 'colorcolumn' past end of the line. */
                if (wp.w_onebuf_opt.wo_wrap[0])
                    v = wp.w_skipcol;
                else
                    v = wp.w_leftcol;

                /* check if line ends before left margin */
                if (vcol < v + col - win_col_off(wp))
                    vcol = v + col - win_col_off(wp);
                /* Get rid of the boguscols now,
                 * we want to draw until the right edge for 'cursorcolumn'. */
                col -= boguscols;
                boguscols = 0;

                if (draw_color_col)
                    draw_color_col = advance_color_col(vcol - vcol_off, color_cols, cci);

                if (((wp.w_onebuf_opt.wo_cuc[0]
                          && vcol - vcol_off - eol_hl_off <= wp.w_virtcol
                          && wp.w_virtcol < wp.w_width * (row - startrow + 1) + v
                          && lnum != wp.w_cursor.lnum)
                        || draw_color_col)
                        && !wp.w_onebuf_opt.wo_rl[0])
                {
                    int rightmost_vcol = 0;

                    if (wp.w_onebuf_opt.wo_cuc[0])
                        rightmost_vcol = wp.w_virtcol;
                    if (draw_color_col)
                        /* determine rightmost colorcolumn to possibly draw */
                        for (int i = 0; 0 <= color_cols[cci[0] + i]; i++)
                            if (rightmost_vcol < color_cols[cci[0] + i])
                                rightmost_vcol = color_cols[cci[0] + i];

                    while (col < wp.w_width)
                    {
                        screenLines.be(off, (byte)' ');
                        screenLinesUC[off] = 0;
                        col++;
                        if (draw_color_col)
                            draw_color_col = advance_color_col(vcol - vcol_off, color_cols, cci);

                        if (wp.w_onebuf_opt.wo_cuc[0] && vcol - vcol_off == wp.w_virtcol)
                            screenAttrs[off++] = hl_attr(HLF_CUC);
                        else if (draw_color_col && vcol - vcol_off == color_cols[cci[0]])
                            screenAttrs[off++] = hl_attr(HLF_MC);
                        else
                            screenAttrs[off++] = 0;

                        if (rightmost_vcol <= vcol - vcol_off)
                            break;

                        vcol++;
                    }
                }

                screen_line(screen_row, wp.w_wincol, col, wp.w_width, wp.w_onebuf_opt.wo_rl[0]);
                row++;

                /*
                 * Update w_cline_height and w_cline_folded if the cursor line was
                 * updated (saves a call to plines() later).
                 */
                if (wp == curwin && lnum == curwin.w_cursor.lnum)
                {
                    curwin.w_cline_row = startrow;
                    curwin.w_cline_height = row - startrow;
                    curwin.w_valid |= (VALID_CHEIGHT|VALID_CROW);
                }

                break;
            }

            /* line continues beyond line end */
            if (lcs_ext[0] != NUL
                    && !wp.w_onebuf_opt.wo_wrap[0]
                    && (wp.w_onebuf_opt.wo_rl[0] ? col == 0 : col == wp.w_width - 1)
                    && (ptr.at(0) != NUL
                        || (wp.w_onebuf_opt.wo_list[0] && 0 < lcs_eol_one)
                        || (n_extra != 0 && (c_extra != NUL || p_extra.at(0) != NUL))))
            {
                c = lcs_ext[0];
                char_attr = hl_attr(HLF_AT);
                mb_c = c;
                if (1 < utf_char2len(c))
                {
                    mb_utf8 = true;
                    u8cc[0] = 0;
                    c = 0xc0;
                }
                else
                    mb_utf8 = false;
            }

            /* advance to the next 'colorcolumn' */
            if (draw_color_col)
                draw_color_col = advance_color_col(vcol - vcol_off, color_cols, cci);

            /* Highlight the cursor column if 'cursorcolumn' is set.
             * But don't highlight the cursor position itself.
             * Also highlight the 'colorcolumn' if it is different than 'cursorcolumn'. */
            vcol_save_attr = -1;
            if (draw_state == WL_LINE && !lnum_in_visual_area)
            {
                if (wp.w_onebuf_opt.wo_cuc[0] && vcol - vcol_off == wp.w_virtcol && lnum != wp.w_cursor.lnum)
                {
                    vcol_save_attr = char_attr;
                    char_attr = hl_combine_attr(char_attr, hl_attr(HLF_CUC));
                }
                else if (draw_color_col && vcol - vcol_off == color_cols[cci[0]])
                {
                    vcol_save_attr = char_attr;
                    char_attr = hl_combine_attr(char_attr, hl_attr(HLF_MC));
                }
            }

            /*
             * Store character to be displayed.
             * Skip characters that are left of the screen for 'nowrap'.
             */
            vcol_prev = vcol;
            if (draw_state < WL_LINE || n_skip <= 0)
            {
                /*
                 * Store the character.
                 */
                if (wp.w_onebuf_opt.wo_rl[0] && 1 < utf_char2cells(mb_c))
                {
                    /* A double-wide character is: put first halve in left cell. */
                    --off;
                    --col;
                }
                screenLines.be(off, c);
                if (mb_utf8)
                {
                    screenLinesUC[off] = mb_c;
                    if ((c & 0xff) == 0)
                        screenLines.be(off, 0x80);    /* avoid storing zero */
                    for (int i = 0; i < screen_mco; i++)
                    {
                        screenLinesC[i][off] = u8cc[i];
                        if (u8cc[i] == 0)
                            break;
                    }
                }
                else
                    screenLinesUC[off] = 0;
                if (multi_attr != 0)
                {
                    screenAttrs[off] = multi_attr;
                    multi_attr = 0;
                }
                else
                    screenAttrs[off] = char_attr;

                if (1 < utf_char2cells(mb_c))
                {
                    /* Need to fill two screen columns. */
                    off++;
                    col++;
                    /* UTF-8: Put a 0 in the second screen char. */
                    screenLines.be(off, NUL);
                    vcol++;
                    /* When "tocol" is halfway a character, set it to the end
                     * of the character, otherwise highlighting won't stop. */
                    if (tocol[0] == vcol)
                        tocol[0]++;
                    if (wp.w_onebuf_opt.wo_rl[0])
                    {
                        /* now it's time to backup one cell */
                        --off;
                        --col;
                    }
                }
                if (wp.w_onebuf_opt.wo_rl[0])
                {
                    --off;
                    --col;
                }
                else
                {
                    off++;
                    col++;
                }
            }
            else if (0 < wp.w_onebuf_opt.wo_cole[0] && is_concealing)
            {
                --n_skip;
                vcol_off++;
                if (0 < n_extra)
                    vcol_off += n_extra;
                if (wp.w_onebuf_opt.wo_wrap[0])
                {
                    /*
                     * Special voodoo required if 'wrap' is on.
                     *
                     * Advance the column indicator to force the line drawing to wrap early.
                     * This will make the line take up the same screen space when parts are concealed,
                     * so that cursor line computations aren't messed up.
                     *
                     * To avoid the fictitious advance of 'col' causing trailing junk to be written
                     * out of the screen line we are building, 'boguscols' keeps track of the number
                     * of bad columns we have advanced.
                     */
                    if (0 < n_extra)
                    {
                        vcol += n_extra;
                        if (wp.w_onebuf_opt.wo_rl[0])
                        {
                            col -= n_extra;
                            boguscols -= n_extra;
                        }
                        else
                        {
                            col += n_extra;
                            boguscols += n_extra;
                        }
                        n_extra = 0;
                        n_attr = 0;
                    }

                    if (1 < utf_char2cells(mb_c))
                    {
                        /* Need to fill two screen columns. */
                        if (wp.w_onebuf_opt.wo_rl[0])
                        {
                            --boguscols;
                            --col;
                        }
                        else
                        {
                            boguscols++;
                            col++;
                        }
                    }

                    if (wp.w_onebuf_opt.wo_rl[0])
                    {
                        --boguscols;
                        --col;
                    }
                    else
                    {
                        boguscols++;
                        col++;
                    }
                }
                else
                {
                    if (0 < n_extra)
                    {
                        vcol += n_extra;
                        n_extra = 0;
                        n_attr = 0;
                    }
                }
            }
            else
                --n_skip;

            /* Only advance the "vcol" when after the 'number' or 'relativenumber' column. */
            if (WL_NR < draw_state)
                vcol++;

            if (0 <= vcol_save_attr)
                char_attr = vcol_save_attr;

            /* restore attributes after "predeces" in 'listchars' */
            if (WL_NR < draw_state && 0 < n_attr3 && --n_attr3 == 0)
                char_attr = saved_attr3;

            /* restore attributes after last 'listchars' or 'number' char */
            if (0 < n_attr && draw_state == WL_LINE && --n_attr == 0)
                char_attr = saved_attr2;

            /*
             * At end of screen line and there is more to come:
             * display the line so far.
             * If there is no more to display it is caught above.
             */
            if ((wp.w_onebuf_opt.wo_rl[0] ? (col < 0) : (wp.w_width <= col))
                    && (ptr.at(0) != NUL
                        || (wp.w_onebuf_opt.wo_list[0] && lcs_eol[0] != NUL && BNE(p_extra, at_end_str))
                        || (n_extra != 0 && (c_extra != NUL || p_extra.at(0) != NUL))))
            {
                screen_line(screen_row, wp.w_wincol, col - boguscols, wp.w_width, wp.w_onebuf_opt.wo_rl[0]);
                boguscols = 0;
                row++;
                screen_row++;

                /* When not wrapping and finished diff lines, or when displayed
                 * '$' and highlighting until last column, break here. */
                if ((!wp.w_onebuf_opt.wo_wrap[0]) || lcs_eol_one == -1)
                    break;

                /* When the window is too narrow draw all "@" lines. */
                if (draw_state != WL_LINE)
                {
                    win_draw_end(wp, '@', ' ', row, wp.w_height, HLF_AT);
                    draw_vsep_win(wp, row);
                    row = endrow;
                }

                /* When line got too long for screen break here. */
                if (row == endrow)
                {
                    row++;
                    break;
                }

                if (screen_cur_row == screen_row - 1 && wp.w_width == (int)Columns[0])
                {
                    /* Remember that the line wraps, used for modeless copy. */
                    lineWraps[screen_row - 1] = true;

                    /*
                     * Special trick to make copy/paste of wrapped lines work with xterm/screen:
                     * write an extra character beyond the end of the line.
                     * This will work with all terminal types (regardless of the xn,am settings).
                     * Only do this on a fast tty.
                     * Only do this if the cursor is on the current line
                     * (something has been written in it).
                     * Don't do this for the GUI.
                     * Don't do this for double-width characters.
                     * Don't do this for a window not at the right screen border.
                     */
                    if (p_tf[0]
                             && !(utf_off2cells(lineOffset[screen_row],
                                         lineOffset[screen_row] + screenColumns) == 2
                                     || utf_off2cells(lineOffset[screen_row - 1] + ((int)Columns[0] - 2),
                                         lineOffset[screen_row] + screenColumns) == 2))
                    {
                        int eoff = lineOffset[screen_row - 1] + ((int)Columns[0] - 1);

                        /* First make sure we are at the end of the screen line,
                         * then output the same character again to let the terminal know about the wrap.
                         * If the terminal doesn't auto-wrap, we overwrite the character. */
                        if (screen_cur_col != wp.w_width)
                            screen_char(eoff, screen_row - 1, ((int)Columns[0] - 1));

                        /* When there is a multi-byte character,
                         * just output a space to keep it simple. */
                        if (1 < us_byte2len(screenLines.at(eoff), false))
                            out_char((byte)' ');
                        else
                            out_char(screenLines.at(eoff));
                        /* force a redraw of the first char on the next line */
                        screenAttrs[lineOffset[screen_row]] = -1;
                        screen_start();     /* don't know where cursor is now */
                    }
                }

                col = 0;
                off = BDIFF(current_ScreenLine, screenLines);
                if (wp.w_onebuf_opt.wo_rl[0])
                {
                    col = wp.w_width - 1;   /* col is not used if breaking! */
                    off += col;
                }

                /* reset the drawing state for the start of a wrapped line */
                draw_state = WL_START;
                saved_n_extra = n_extra;
                saved_p_extra = p_extra;
                saved_c_extra = c_extra;
                saved_char_attr = char_attr;
                n_extra = 0;
                lcs_prec_todo = lcs_prec[0];
                    need_showbreak = true;
            }
        }

        return row;
    }

    /*
     * Return if the composing characters at "off_from" and "off_to" differ.
     * Only to be used when screenLinesUC[off_from] != 0.
     */
    /*private*/ static boolean comp_char_differs(int off_from, int off_to)
    {
        for (int i = 0; i < screen_mco; i++)
        {
            if (screenLinesC[i][off_from] != screenLinesC[i][off_to])
                return true;
            if (screenLinesC[i][off_from] == 0)
                break;
        }
        return false;
    }

    /*
     * Check whether the given character needs redrawing:
     * - the (first byte of the) character is different
     * - the attributes are different
     * - the character is multi-byte and the next byte is different
     * - the character is two cells wide and the second cell differs.
     */
    /*private*/ static boolean char_needs_redraw(int from, int to, int cols)
    {
        return (0 < cols
            && ((screenLines.at(from) != screenLines.at(to) || screenAttrs[from] != screenAttrs[to])
                || (screenLinesUC[from] != screenLinesUC[to]
                    || (screenLinesUC[from] != 0 && comp_char_differs(from, to))
                    || (1 < utf_off2cells(from, from + cols)
                        && screenLines.at(from + 1) != screenLines.at(to + 1)))));
    }

    /*
     * Move one "cooked" screen line to the screen, but only the characters that
     * have actually changed.  Handle insert/delete character.
     * "coloff" gives the first column on the screen for this line.
     * "endcol" gives the columns where valid characters are.
     * "clear_width" is the width of the window.  It's > 0 if the rest of the line
     * needs to be cleared, negative otherwise.
     * "rlflag" is true in a rightleft window:
     *    When true and "clear_width" > 0, clear columns 0 to "endcol"
     *    When false and "clear_width" > 0, clear columns "endcol" to "clear_width"
     */
    /*private*/ static void screen_line(int row, int coloff, int endcol, int clear_width, boolean rlflag)
    {
        int col = 0;
        boolean force = false;              /* force update rest of the line */
        boolean clear_next = false;

        /* Check for illegal row and col, just in case. */
        if (Rows[0] <= row)
            row = (int)Rows[0] - 1;
        if (Columns[0] < endcol)
            endcol = (int)Columns[0];

        clip_may_clear_selection(row, row);

        int off_from = BDIFF(current_ScreenLine, screenLines);
        int off_to = lineOffset[row] + coloff;
        int max_off_from = off_from + screenColumns;
        int max_off_to = lineOffset[row] + screenColumns;

        if (rlflag)
        {
            /* Clear rest first, because it's left of the text. */
            if (0 < clear_width)
            {
                while (col <= endcol && screenLines.at(off_to) == (byte)' '
                                     && screenAttrs[off_to] == 0
                                     && screenLinesUC[off_to] == 0)
                {
                    off_to++;
                    col++;
                }
                if (col <= endcol)
                    screen_fill(row, row + 1, col + coloff, endcol + coloff + 1, ' ', ' ', 0);
            }
            col = endcol + 1;
            off_to = lineOffset[row] + col + coloff;
            off_from += col;
            endcol = (0 < clear_width) ? clear_width : -clear_width;
        }

        boolean redraw_next = char_needs_redraw(off_from, off_to, endcol - col);

        while (col < endcol)
        {
            int char_cells;             /* 1: normal char; 2: occupies two display cells */
            if (col + 1 < endcol)
                char_cells = utf_off2cells(off_from, max_off_from);
            else
                char_cells = 1;

            /* bool: does character need redraw? */
            boolean redraw_this = redraw_next;
            /* redraw_this for next character */
            redraw_next = force || char_needs_redraw(off_from + char_cells,
                                                     off_to + char_cells,
                                                     endcol - col - char_cells);

            if (redraw_this)
            {
                /*
                 * Special handling when 'xs' termcap flag set (hpterm):
                 * Attributes for characters are stored at the position where the
                 * cursor is when writing the highlighting code.  The
                 * start-highlighting code must be written with the cursor on the
                 * first highlighted character.  The stop-highlighting code must
                 * be written with the cursor just after the last highlighted character.
                 * Overwriting a character doesn't remove it's highlighting.  Need
                 * to clear the rest of the line, and force redrawing it completely.
                 */
                if (p_wiv[0]
                        && !force
                        && screenAttrs[off_to] != 0
                        && screenAttrs[off_from] != screenAttrs[off_to])
                {
                    /*
                     * Need to remove highlighting attributes here.
                     */
                    windgoto(row, col + coloff);
                    out_str(T_CE[0]);          /* clear rest of this screen line */
                    screen_start();         /* don't know where cursor is now */
                    force = true;           /* force redraw of rest of the line */
                    redraw_next = true;     /* or else next char would miss out */

                    /*
                     * If the previous character was highlighted, need to stop
                     * highlighting at this character.
                     */
                    if (0 < col + coloff && screenAttrs[off_to - 1] != 0)
                    {
                        screen_attr = screenAttrs[off_to - 1];
                        term_windgoto(row, col + coloff);
                        screen_stop_highlight();
                    }
                    else
                        screen_attr = 0;        /* highlighting has stopped */
                }
                /* When writing a single-width character over a double-width
                 * character and at the end of the redrawn text, need to clear out
                 * the right halve of the old character.
                 * Also required when writing the right halve of a double-width
                 * char over the left halve of an existing one. */
                if (col + char_cells == endcol
                        && ((char_cells == 1
                                && 1 < utf_off2cells(off_to, max_off_to))
                            || (char_cells == 2
                                && utf_off2cells(off_to, max_off_to) == 1
                                && 1 < utf_off2cells(off_to + 1, max_off_to))))
                    clear_next = true;

                screenLines.be(off_to, screenLines.at(off_from));
                screenLinesUC[off_to] = screenLinesUC[off_from];
                if (screenLinesUC[off_from] != 0)
                {
                    for (int i = 0; i < screen_mco; i++)
                        screenLinesC[i][off_to] = screenLinesC[i][off_from];
                }
                if (char_cells == 2)
                    screenLines.be(off_to + 1, screenLines.at(off_from + 1));

                /* The bold trick makes a single column of pixels appear in the
                 * next character.  When a bold character is removed, the next
                 * character should be redrawn too.  This happens for our own GUI
                 * and for some xterms. */
                if (term_is_xterm)
                {
                    int hl = screenAttrs[off_to];
                    if (HL_ALL < hl)
                        hl = syn_attr2attr(hl);
                    if ((hl & HL_BOLD) != 0)
                        redraw_next = true;
                }
                screenAttrs[off_to] = screenAttrs[off_from];
                /* For simplicity, set the attributes of second half
                 * of a double-wide character equal to the first half. */
                if (char_cells == 2)
                    screenAttrs[off_to + 1] = screenAttrs[off_from];

                screen_char(off_to, row, col + coloff);
            }
            else if (p_wiv[0] && 0 < col + coloff)
            {
                if (screenAttrs[off_to] == screenAttrs[off_to - 1])
                {
                    /*
                     * Don't output stop-highlight when moving the cursor,
                     * it will stop the highlighting when it should continue.
                     */
                    screen_attr = 0;
                }
                else if (screen_attr != 0)
                    screen_stop_highlight();
            }

            off_to += char_cells;
            off_from += char_cells;
            col += char_cells;
        }

        if (clear_next)
        {
            /* Clear the second half of a double-wide character of which
             * the left half was overwritten with a single-wide character. */
            screenLines.be(off_to, (byte)' ');
            screenLinesUC[off_to] = 0;
            screen_char(off_to, row, col + coloff);
        }

        if (0 < clear_width && !rlflag)
        {
            /* blank out the rest of the line */
            while (col < clear_width && screenLines.at(off_to) == (byte)' '
                                     && screenAttrs[off_to] == 0
                                     && screenLinesUC[off_to] == 0)
            {
                off_to++;
                col++;
            }
            if (col < clear_width)
            {
                screen_fill(row, row + 1, col + coloff, clear_width + coloff, ' ', ' ', 0);
                off_to += clear_width - col;
                col = clear_width;
            }
        }

        if (0 < clear_width)
        {
            /* For a window that's left of another, draw the separator char. */
            if (col + coloff < (int)Columns[0])
            {
                int[] hl = new int[1];
                int c = fillchar_vsep(hl);
                if (screenLines.at(off_to) != c
                        || screenLinesUC[off_to] != (0x80 <= c ? c : 0)
                        || screenAttrs[off_to] != hl[0])
                {
                    screenLines.be(off_to, c);
                    screenAttrs[off_to] = hl[0];
                    if (0x80 <= c)
                    {
                        screenLinesUC[off_to] = c;
                        screenLinesC[0][off_to] = 0;
                    }
                    else
                        screenLinesUC[off_to] = 0;
                    screen_char(off_to, row, col + coloff);
                }
            }
            else
                lineWraps[row] = false;
        }
    }

    /*
     * Mirror text "str" for right-left displaying.
     * Only works for single-byte characters (e.g., numbers).
     */
    /*private*/ static void rl_mirror(Bytes str)
    {
        for (Bytes p1 = str, p2 = str.plus(strlen(str) - 1); BLT(p1, p2); p1 = p1.plus(1), p2 = p2.minus(1))
        {
            byte b = p1.at(0);
            p1.be(0, p2.at(0));
            p2.be(0, b);
        }
    }

    /*
     * mark all status lines for redraw; used after first :cd
     */
    /*private*/ static void status_redraw_all()
    {
        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            if (wp.w_status_height != 0)
            {
                wp.w_redr_status = true;
                redraw_later(VALID);
            }
    }

    /*
     * mark all status lines of the current buffer for redraw
     */
    /*private*/ static void status_redraw_curbuf()
    {
        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            if (wp.w_status_height != 0 && wp.w_buffer == curbuf)
            {
                wp.w_redr_status = true;
                redraw_later(VALID);
            }
    }

    /*
     * Redraw all status lines that need to be redrawn.
     */
    /*private*/ static void redraw_statuslines()
    {
        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            if (wp.w_redr_status)
                win_redr_status(wp);
        if (redraw_tabline)
            draw_tabline();
    }

    /*
     * Draw the verticap separator right of window "wp" starting with line "row".
     */
    /*private*/ static void draw_vsep_win(window_C wp, int row)
    {
        if (wp.w_vsep_width != 0)
        {
            /* draw the vertical separator right of this window */
            int[] hl = new int[1];
            int c = fillchar_vsep(hl);
            screen_fill(wp.w_winrow + row, wp.w_winrow + wp.w_height,
                        wp.w_wincol + wp.w_width, wp.w_wincol + wp.w_width + 1,
                        c, ' ', hl[0]);
        }
    }

    /*private*/ static boolean _3_busy;

    /*
     * Redraw the status line of window wp.
     *
     * If inversion is possible we use it.  Else '=' characters are used.
     */
    /*private*/ static void win_redr_status(window_C wp)
    {
        /* It's possible to get here recursively when 'statusline' (indirectly)
         * invokes ":redrawstatus".  Simply ignore the call then. */
        if (_3_busy)
            return;
        _3_busy = true;

        wp.w_redr_status = false;
        if (wp.w_status_height == 0)
        {
            /* no status line, can only be last window */
            redraw_cmdline = true;
        }
        else if (!redrawing())
        {
            /* Don't redraw right now, do it later. */
            wp.w_redr_status = true;
        }
        else if (p_stl[0].at(0) != NUL || wp.w_onebuf_opt.wo_stl[0].at(0) != NUL)
        {
            /* redraw custom status line */
            redraw_custom_statusline(wp);
        }
        else
        {
            int[] attr = new int[1];
            int fillchar = fillchar_status(attr, wp == curwin);

            get_trans_bufname(wp.w_buffer);
            Bytes p = nameBuff;
            int len = strlen(p);

            if (bufIsChanged(wp.w_buffer) || wp.w_buffer.b_p_ro[0])
                p.be(len++, (byte)' ');
            if (bufIsChanged(wp.w_buffer))
            {
                STRCPY(p.plus(len), u8("[+]"));
                len += 3;
            }
            if (wp.w_buffer.b_p_ro[0])
            {
                STRCPY(p.plus(len), u8("[RO]"));
                len += 4;
            }

            int this_ru_col = ru_col - ((int)Columns[0] - wp.w_width);
            if (this_ru_col < (wp.w_width + 1) / 2)
                this_ru_col = (wp.w_width + 1) / 2;
            if (this_ru_col <= 1)
            {
                p = u8("<");                    /* No room for file name! */
                len = 1;
            }
            else
            {
                /* Count total number of display cells. */
                len = us_string2cells(p, -1);

                /* Find first character that will fit.
                 * Going from start to end is much faster for DBCS. */
                int i;
                for (i = 0; p.at(i) != NUL && this_ru_col - 1 <= len; i += us_ptr2len_cc(p.plus(i)))
                    len -= us_ptr2cells(p.plus(i));
                if (0 < i)
                {
                    p = p.plus(i - 1);
                    p.be(0, (byte)'<');
                    len++;
                }
            }

            int row = wp.w_winrow + wp.w_height;
            screen_puts(p, row, wp.w_wincol, attr[0]);
            screen_fill(row, row + 1, len + wp.w_wincol, this_ru_col + wp.w_wincol, fillchar, fillchar, attr[0]);

            if (get_keymap_str(wp, nameBuff, MAXPATHL) && strlen(nameBuff) + 1 < this_ru_col - len)
                screen_puts(nameBuff, row, this_ru_col - strlen(nameBuff) - 1 + wp.w_wincol, attr[0]);

            win_redr_ruler(wp, true);
        }

        /*
         * May need to draw the character below the vertical separator.
         */
        if (wp.w_vsep_width != 0 && wp.w_status_height != 0 && redrawing())
        {
            int fillchar;
            int[] attr = new int[1];
            if (stl_connected(wp))
                fillchar = fillchar_status(attr, wp == curwin);
            else
                fillchar = fillchar_vsep(attr);
            screen_putchar(fillchar, wp.w_winrow + wp.w_height, wp.w_wincol + wp.w_width, attr[0]);
        }
        _3_busy = false;
    }

    /*private*/ static boolean _2_entered;

    /*
     * Redraw the status line according to 'statusline' and take care of any errors encountered.
     */
    /*private*/ static void redraw_custom_statusline(window_C wp)
    {
        boolean save_called_emsg = called_emsg;

        /* When called recursively return.
         * This can happen when the statusline contains an expression that triggers a redraw. */
        if (_2_entered)
            return;
        _2_entered = true;

        called_emsg = false;
        win_redr_custom(wp, false);
        if (called_emsg)
        {
            /* When there is an error disable the statusline, otherwise the display is messed up
             * with errors and a redraw triggers the problem again and again. */
            set_string_option_direct(u8("statusline"), -1, u8(""),
                OPT_FREE | (wp.w_onebuf_opt.wo_stl[0].at(0) != NUL ? OPT_LOCAL : OPT_GLOBAL), SID_ERROR);
        }
        called_emsg |= save_called_emsg;
        _2_entered = false;
    }

    /*
     * Return true if the status line of window "wp" is connected to the status
     * line of the window right of it.  If not, then it's a vertical separator.
     * Only call if (wp.w_vsep_width != 0).
     */
    /*private*/ static boolean stl_connected(window_C wp)
    {
        for (frame_C fr = wp.w_frame; fr.fr_parent != null; fr = fr.fr_parent)
        {
            if (fr.fr_parent.fr_layout == FR_COL)
            {
                if (fr.fr_next != null)
                    break;
            }
            else
            {
                if (fr.fr_next != null)
                    return true;
            }
        }
        return false;
    }

    /*
     * Get the value to show for the language mappings, active 'keymap'.
     */
    /*private*/ static boolean get_keymap_str(window_C wp, Bytes buf, int len)
        /* buf: buffer for the result */
        /* len: length of buffer */
    {
        if (wp.w_buffer.b_p_iminsert[0] != B_IMODE_LMAP)
            return false;

        buffer_C old_curbuf = curbuf;
        window_C old_curwin = curwin;

        curbuf = wp.w_buffer;
        curwin = wp;
        STRCPY(buf, u8("b:keymap_name"));               /* must be writable */
        emsg_skip++;
        Bytes p = eval_to_string(buf, null, false);
        --emsg_skip;
        curbuf = old_curbuf;
        curwin = old_curwin;
        if (p == null || p.at(0) == NUL)
            p = u8("lang");
        if (strlen(p) + 3 < len)
            libC.sprintf(buf, u8("<%s>"), p);
        else
            buf.be(0, NUL);

        return (buf.at(0) != NUL);
    }

    /*private*/ static boolean _3_entered;

    /*
     * Redraw the status line or ruler of window "wp".
     * When "wp" is null redraw the tab pages line from 'tabline'.
     */
    /*private*/ static void win_redr_custom(window_C wp, boolean draw_ruler)
        /* draw_ruler: true or false */
    {
        /* There is a tiny chance that this gets called recursively:
         * when redrawing a status line triggers redrawing the ruler or tabline.
         * Avoid trouble by not allowing recursion. */
        if (_3_entered)
            return;
        _3_entered = true;

        stl_hlrec_C[] hltab = ARRAY_stl_hlrec(STL_MAX_ITEM);
        stl_hlrec_C[] tabtab = ARRAY_stl_hlrec(STL_MAX_ITEM);

        int col = 0;

        /* setup environment for the task at hand */
        Bytes stl;
        int row;
        int fillchar;
        int[] attr = new int[1];
        int maxwidth;
        boolean use_sandbox = false;
        if (wp == null)
        {
            /* Use 'tabline'.  Always at the first line of the screen. */
            stl = p_tal[0];
            row = 0;
            fillchar = ' ';
            attr[0] = hl_attr(HLF_TPF);
            maxwidth = (int)Columns[0];
            use_sandbox = was_set_insecurely(u8("tabline"), 0);
        }
        else
        {
            row = wp.w_winrow + wp.w_height;
            fillchar = fillchar_status(attr, wp == curwin);
            maxwidth = wp.w_width;

            if (draw_ruler)
            {
                stl = p_ruf[0];
                /* advance past any leading group spec - implicit in ru_col */
                if (stl.at(0) == (byte)'%')
                {
                    if ((stl = stl.plus(1)).at(0) == (byte)'-')
                        stl = stl.plus(1);
                    if (libC.atoi(stl) != 0)
                        while (asc_isdigit(stl.at(0)))
                            stl = stl.plus(1);
                    if ((stl = stl.plus(1)).at(-1) != (byte)'(')
                        stl = p_ruf[0];
                }
                col = ru_col - ((int)Columns[0] - wp.w_width);
                if (col < (wp.w_width + 1) / 2)
                    col = (wp.w_width + 1) / 2;
                maxwidth = wp.w_width - col;
                if (wp.w_status_height == 0)
                {
                    row = (int)Rows[0] - 1;
                    --maxwidth;     /* writing in last column may cause scrolling */
                    fillchar = ' ';
                    attr[0] = 0;
                }

                use_sandbox = was_set_insecurely(u8("rulerformat"), 0);
            }
            else
            {
                if (wp.w_onebuf_opt.wo_stl[0].at(0) != NUL)
                    stl = wp.w_onebuf_opt.wo_stl[0];
                else
                    stl = p_stl[0];
                use_sandbox = was_set_insecurely(u8("statusline"), (wp.w_onebuf_opt.wo_stl[0].at(0) == NUL) ? 0 : OPT_LOCAL);
            }

            col += wp.w_wincol;
        }

        if (0 < maxwidth)
        {
            /* Temporarily reset 'cursorbind',
             * we don't want a side effect from moving the cursor away and back. */
            window_C ewp = (wp == null) ? curwin : wp;
            boolean p_crb_save = ewp.w_onebuf_opt.wo_crb[0];
            ewp.w_onebuf_opt.wo_crb[0] = false;

            Bytes buf = new Bytes(MAXPATHL);
            /* Make a copy, because the statusline may include a function call that
             * might change the option value and free the memory. */
            stl = STRDUP(stl);
            int width = build_stl_str_hl(ewp, buf, buf.size(), stl, use_sandbox, fillchar, maxwidth, hltab, tabtab);
            ewp.w_onebuf_opt.wo_crb[0] = p_crb_save;

            /* Make all characters printable. */
            vim_strncpy(buf, transstr(buf), buf.size() - 1);

            /* fill up with "fillchar" */
            int len = strlen(buf);
            while (width < maxwidth && len < buf.size() - 1)
            {
                len += utf_char2bytes(fillchar, buf.plus(len));
                width++;
            }
            buf.be(len, NUL);

            /*
             * Draw each snippet with the specified highlighting.
             */
            int curattr = attr[0];
            Bytes p = buf;
            for (int n = 0; hltab[n].start != null; n++)
            {
                len = BDIFF(hltab[n].start, p);
                screen_puts_len(p, len, row, col, curattr);
                col += mb_string2cells(p, len);
                p = hltab[n].start;

                if (hltab[n].userhl == 0)
                    curattr = attr[0];
                else if (hltab[n].userhl < 0)
                    curattr = syn_id2attr(-hltab[n].userhl);
                else if (wp != null && wp != curwin && wp.w_status_height != 0)
                    curattr = highlight_stlnc[hltab[n].userhl - 1];
                else
                    curattr = highlight_user[hltab[n].userhl - 1];
            }
            screen_puts(p, row, col, curattr);

            if (wp == null)
            {
                /* Fill the tabPageIdxs[] array for clicking in the tab pagesline. */
                col = 0;
                len = 0;
                p = buf;
                fillchar = 0;
                for (int n = 0; tabtab[n].start != null; n++)
                {
                    len += mb_string2cells(p, BDIFF(tabtab[n].start, p));
                    while (col < len)
                        tabPageIdxs[col++] = (short)fillchar;
                    p = tabtab[n].start;
                    fillchar = tabtab[n].userhl;
                }
                while (col < (int)Columns[0])
                    tabPageIdxs[col++] = (short)fillchar;
            }
        }

        _3_entered = false;
    }

    /*
     * Output a single character directly to the screen and update "screenLines".
     */
    /*private*/ static void screen_putchar(int c, int row, int col, int attr)
    {
        Bytes buf = new Bytes(MB_MAXBYTES + 1);

        buf.be(utf_char2bytes(c, buf), NUL);
        screen_puts(buf, row, col, attr);
    }

    /*
     * Get a single character directly from "screenLines" into "bytes[]".
     * Also return its attribute in "*attrp".
     */
    /*private*/ static void screen_getbytes(int row, int col, Bytes bytes, int[] attrp)
    {
        /* safety check */
        if (screenLines != null && row < screenRows && col < screenColumns)
        {
            int off = lineOffset[row] + col;

            attrp[0] = screenAttrs[off];
            bytes.be(0, screenLines.at(off));
            bytes.be(1, NUL);

            if (screenLinesUC[off] != 0)
                bytes.be(utfc_char2bytes(off, bytes), NUL);
        }
    }

    /*
     * Return true if composing characters for screen posn "off"
     * differs from composing characters in "u8cc".
     * Only to be used when screenLinesUC[off] != 0.
     */
    /*private*/ static boolean screen_comp_differs(int off, int[] u8cc)
    {
        for (int i = 0; i < screen_mco; i++)
        {
            if (screenLinesC[i][off] != u8cc[i])
                return true;
            if (u8cc[i] == 0)
                break;
        }
        return false;
    }

    /*
     * Put string '*text' on the screen at position 'row' and 'col', with
     * attributes 'attr', and update screenLines[] and screenAttrs[].
     * Note: only outputs within one row, message is truncated at screen boundary!
     * Note: if screenLines[], row and/or col is invalid, nothing is done.
     */
    /*private*/ static void screen_puts(Bytes text, int row, int col, int attr)
    {
        screen_puts_len(text, -1, row, col, attr);
    }

    /*
     * Like screen_puts(), but output "text[len]".  When "len" is -1 output up to a NUL.
     */
    /*private*/ static void screen_puts_len(Bytes text, int textlen, int row, int col, int attr)
    {
        boolean clear_next_cell = false;
        boolean force_redraw_next = false;

        if (screenLines == null || screenRows <= row)       /* safety check */
            return;

        int off = lineOffset[row] + col;

        /* When drawing over the right halve of a double-wide char clear out the left halve.
         * Only needed in a terminal. */
        if (0 < col && col < screenColumns && mb_fix_col(col, row) != col)
        {
            screenLines.be(off - 1, (byte)' ');
            screenAttrs[off - 1] = 0;
            screenLinesUC[off - 1] = 0;
            screenLinesC[0][off - 1] = 0;
            /* redraw the previous cell, make it empty */
            screen_char(off - 1, row, col - 1);
            /* force the cell at "col" to be redrawn */
            force_redraw_next = true;
        }

        Bytes ptr = text;
        int len = textlen;
        int[] u8cc = new int[MAX_MCO];

        int max_off = lineOffset[row] + screenColumns;
        while (col < screenColumns && (len < 0 || BDIFF(ptr, text) < len) && ptr.at(0) != NUL)
        {
            byte c = ptr.at(0);

            int mbyte_blen;
            /* check if this is the first byte of a multibyte */
            if (0 < len)
                mbyte_blen = us_ptr2len_cc_len(ptr, BDIFF(text.plus(len), ptr));
            else
                mbyte_blen = us_ptr2len_cc(ptr);

            int u8c;
            if (0 <= len)
                u8c = us_ptr2char_cc_len(ptr, u8cc, BDIFF(text.plus(len), ptr));
            else
                u8c = us_ptr2char_cc(ptr, u8cc);

            int mbyte_cells = utf_char2cells(u8c);

            if (screenColumns < col + mbyte_cells)
            {
                /* Only 1 cell left, but character requires 2 cells:
                 * display a '>' in the last column to avoid wrapping. */
                c = '>';
                mbyte_cells = 1;
            }

            boolean force_redraw_this = force_redraw_next;
            force_redraw_next = false;

            boolean need_redraw = (screenLines.at(off) != c)
                    || (mbyte_cells == 2 && screenLines.at(off + 1) != 0)
                    || (screenLinesUC[off] != ((char_u(c) < 0x80 && u8cc[0] == 0) ? 0 : u8c)
                            || (screenLinesUC[off] != 0 && screen_comp_differs(off, u8cc)))
                    || (screenAttrs[off] != attr)
                    || (exmode_active != 0);

            if (need_redraw || force_redraw_this)
            {
                /* The bold trick makes a single row of pixels appear in the next character.
                 * When a bold character is removed, the next character should be redrawn too.
                 * This happens for our own GUI and for some xterms. */
                if (need_redraw && screenLines.at(off) != (byte)' ' && term_is_xterm)
                {
                    int n = screenAttrs[off];

                    if (HL_ALL < n)
                        n = syn_attr2attr(n);
                    if ((n & HL_BOLD) != 0)
                        force_redraw_next = true;
                }
                /* When at the end of the text and overwriting a two-cell character with
                 * a one-cell character, need to clear the next cell.  Also when overwriting
                 * the left halve of a two-cell char with the right halve of a two-cell char.
                 * Do this only once (utf_off2cells() may return 2 on the right halve). */
                if (clear_next_cell)
                    clear_next_cell = false;
                else if ((len < 0 ? ptr.at(mbyte_blen) == NUL : BLE(text.plus(len), ptr.plus(mbyte_blen)))
                        && ((mbyte_cells == 1 && 1 < utf_off2cells(off, max_off))
                            || (mbyte_cells == 2
                                && utf_off2cells(off, max_off) == 1
                                && 1 < utf_off2cells(off + 1, max_off))))
                    clear_next_cell = true;

                screenLines.be(off, c);
                screenAttrs[off] = attr;

                if (char_u(c) < 0x80 && u8cc[0] == 0)
                    screenLinesUC[off] = 0;
                else
                {
                    screenLinesUC[off] = u8c;
                    for (int i = 0; i < screen_mco; i++)
                    {
                        screenLinesC[i][off] = u8cc[i];
                        if (u8cc[i] == 0)
                            break;
                    }
                }
                if (mbyte_cells == 2)
                {
                    screenLines.be(off + 1, NUL);
                    screenAttrs[off + 1] = attr;
                }
                screen_char(off, row, col);
            }

            off += mbyte_cells;
            col += mbyte_cells;
            ptr = ptr.plus(mbyte_blen);
            if (clear_next_cell)
            {
                /* This only happens at the end, display one space next. */
                ptr = u8(" ");
                len = -1;
            }
        }

        /* If we detected the next character needs to be redrawn,
         * but the text doesn't extend up to there, update the character here. */
        if (force_redraw_next && col < screenColumns)
            screen_char(off, row, col);
    }

    /*
     * Prepare for 'hlsearch' highlighting.
     */
    /*private*/ static void start_search_hl()
    {
        if (p_hls[0] && !no_hlsearch)
        {
            last_pat_prog(search_hl.rmm);
            search_hl.attr = hl_attr(HLF_L);
            /* Set the time limit to 'redrawtime'. */
            profile_setlimit(p_rdt[0], search_hl.tm);
        }
    }

    /*
     * Clean up for 'hlsearch' highlighting.
     */
    /*private*/ static void end_search_hl()
    {
        if (search_hl.rmm.regprog != null)
            search_hl.rmm.regprog = null;
    }

    /*
     * Init for calling prepare_search_hl().
     */
    /*private*/ static void init_search_hl(window_C wp)
    {
        /* Setup for match and 'hlsearch' highlighting.  Disable any previous match. */
        for (matchitem_C mi = wp.w_match_head; mi != null; mi = mi.next)
        {
            COPY_regmmatch(mi.mi_hl.rmm, mi.mi_match);
            mi.mi_hl.attr = (mi.hlg_id != 0) ? syn_id2attr(mi.hlg_id) : 0;
            mi.mi_hl.buf = wp.w_buffer;
            mi.mi_hl.lnum = 0;
            mi.mi_hl.first_lnum = 0;
            /* Set the time limit to 'redrawtime'. */
            profile_setlimit(p_rdt[0], mi.mi_hl.tm);
        }
        search_hl.buf = wp.w_buffer;
        search_hl.lnum = 0;
        search_hl.first_lnum = 0;
        /* time limit is set at the toplevel, for all windows */
    }

    /*
     * Advance to the match in window "wp" line "lnum" or past it.
     */
    /*private*/ static void prepare_search_hl(window_C wp, long lnum)
    {
        /*
         * When using a multi-line pattern, start searching at the top
         * of the window or just after a closed fold.
         * Do this both for search_hl and the match list.
         */
        matchitem_C mi = wp.w_match_head;
        boolean shl_flag = false;               /* whether search_hl has been processed */
        while (mi != null || !shl_flag)
        {
            match_C shl;                        /* points to search_hl or a match */
            if (!shl_flag)
            {
                shl = search_hl;
                shl_flag = true;
            }
            else
                shl = mi.mi_hl;
            if (shl.rmm.regprog != null && shl.lnum == 0 && re_multiline(shl.rmm.regprog))
            {
                if (shl.first_lnum == 0)
                    shl.first_lnum = wp.w_topline;
                if (mi != null)
                    mi.mi_pos.cur = 0;
                boolean pos_inprogress = true;  /* marks that position match search is in progress */
                int n = 0;
                while (shl.first_lnum < lnum && (shl.rmm.regprog != null || (mi != null && pos_inprogress)))
                {
                    next_search_hl(wp, shl, shl.first_lnum, n, mi);
                    pos_inprogress = (mi != null && mi.mi_pos.cur != 0);
                    if (shl.lnum != 0)
                    {
                        shl.first_lnum = shl.lnum + shl.rmm.endpos[0].lnum - shl.rmm.startpos[0].lnum;
                        n = shl.rmm.endpos[0].col;
                    }
                    else
                    {
                        shl.first_lnum++;
                        n = 0;
                    }
                }
            }
            if (shl != search_hl && mi != null)
                mi = mi.next;
        }
    }

    /*
     * Search for a next 'hlsearch' or match.
     * Uses shl.buf.
     * Sets shl.lnum and shl.rmm contents.
     * Note: Assumes a previous match is always before "lnum", unless shl.lnum is zero.
     * Careful: Any pointers for buffer lines will become invalid.
     */
    /*private*/ static void next_search_hl(window_C win, match_C shl, long lnum, int mincol, matchitem_C mi)
        /* shl: points to search_hl or a match */
        /* mincol: minimal column for a match */
        /* mi: to retrieve match positions if any */
    {
        if (shl.lnum != 0)
        {
            /* Check for three situations:
             * 1. If the "lnum" is below a previous match, start a new search.
             * 2. If the previous match includes "mincol", use it.
             * 3. Continue after the previous match.
             */
            long l = shl.lnum + shl.rmm.endpos[0].lnum - shl.rmm.startpos[0].lnum;
            if (l < lnum)
                shl.lnum = 0;
            else if (lnum < l || mincol < shl.rmm.endpos[0].col)
                return;
        }

        /*
         * Repeat searching for a match until one is found that includes "mincol"
         * or none is found in this line.
         */
        called_emsg = false;
        for ( ; ; )
        {
            /* Stop searching after passing the time limit. */
            if (profile_passed_limit(shl.tm))
            {
                shl.lnum = 0;   /* no match found in time */
                break;
            }
            /* Three situations:
             * 1. No useful previous match: search from start of line.
             * 2. Not Vi compatible or empty match: continue at next character.
             *    Break the loop if this is beyond the end of the line.
             * 3. Vi compatible searching: continue at end of previous match.
             */
            int matchcol;
            if (shl.lnum == 0)
                matchcol = 0;
            else if (vim_strbyte(p_cpo[0], CPO_SEARCH) == null
                    || (shl.rmm.endpos[0].lnum == 0 && shl.rmm.endpos[0].col <= shl.rmm.startpos[0].col))
            {
                matchcol = shl.rmm.startpos[0].col;
                Bytes ml = ml_get_buf(shl.buf, lnum, false).plus(matchcol);
                if (ml.at(0) == NUL)
                {
                    matchcol++;
                    shl.lnum = 0;
                    break;
                }
                matchcol += us_ptr2len_cc(ml);
            }
            else
                matchcol = shl.rmm.endpos[0].col;

            long nmatched;

            shl.lnum = lnum;
            if (shl.rmm.regprog != null)
            {
                /* Remember whether shl.rmm is using a copy of the regprog in mi.mi_match. */
                boolean regprog_is_copy = (shl != search_hl && mi != null
                                        && shl == mi.mi_hl
                                        && mi.mi_match.regprog == mi.mi_hl.rmm.regprog);

                nmatched = vim_regexec_multi(shl.rmm, win, shl.buf, lnum, matchcol, shl.tm);
                /* Copy the regprog, in case it got freed and recompiled. */
                if (regprog_is_copy)
                    mi.mi_match.regprog = mi.mi_hl.rmm.regprog;

                if (called_emsg || got_int)
                {
                    /* Error while handling regexp: stop using this regexp. */
                    if (shl == search_hl)
                    {
                        no_hlsearch = true;
                        set_vim_var_nr(VV_HLSEARCH, (!no_hlsearch && p_hls[0]) ? 1 : 0);
                    }
                    shl.rmm.regprog = null;
                    shl.lnum = 0;
                    got_int = false;    /* avoid the "Type :quit to exit Vim" message */
                    break;
                }
            }
            else if (mi != null)
                nmatched = next_search_hl_pos(shl, lnum, mi.mi_pos, matchcol) ? 1 : 0;
            else
                nmatched = 0;
            if (nmatched == 0)
            {
                shl.lnum = 0;           /* no match found */
                break;
            }
            if (0 < shl.rmm.startpos[0].lnum
                    || mincol <= shl.rmm.startpos[0].col
                    || 1 < nmatched
                    || mincol < shl.rmm.endpos[0].col)
            {
                shl.lnum += shl.rmm.startpos[0].lnum;
                break;                  /* useful match found */
            }
        }
    }

    /*private*/ static boolean next_search_hl_pos(match_C shl, long lnum, posmatch_C posmatch, int mincol)
        /* shl: points to a match */
        /* posmatch: match positions */
        /* mincol: minimal column for a match */
    {
        int bot = -1;

        llpos_C tmp = new llpos_C();

        shl.lnum = 0;
        for (int i = posmatch.cur; i < MAXPOSMATCH; i++)
        {
            if (posmatch.pm_pos[i].lnum == 0)
                break;
            if (posmatch.pm_pos[i].col < mincol)
                continue;
            if (posmatch.pm_pos[i].lnum == lnum)
            {
                if (shl.lnum == lnum)
                {
                    /* partially sort positions by column numbers on the same line */
                    if (posmatch.pm_pos[i].col < posmatch.pm_pos[bot].col)
                    {
                        COPY_llpos(tmp, posmatch.pm_pos[i]);
                        COPY_llpos(posmatch.pm_pos[i], posmatch.pm_pos[bot]);
                        COPY_llpos(posmatch.pm_pos[bot], tmp);
                    }
                }
                else
                {
                    bot = i;
                    shl.lnum = lnum;
                }
            }
        }
        posmatch.cur = 0;

        if (shl.lnum == lnum)
        {
            int start = (posmatch.pm_pos[bot].col == 0) ? 0 : posmatch.pm_pos[bot].col - 1;
            int end = (posmatch.pm_pos[bot].col == 0) ? MAXCOL : start + posmatch.pm_pos[bot].len;

            shl.rmm.startpos[0].lnum = 0;
            shl.rmm.startpos[0].col = start;
            shl.rmm.endpos[0].lnum = 0;
            shl.rmm.endpos[0].col = end;
            posmatch.cur = bot + 1;

            return true;
        }

        return false;
    }

    /*private*/ static void screen_start_highlight(int attr)
    {
        attrentry_C aep = null;

        screen_attr = attr;
        if (full_screen)
        {
            if (HL_ALL < attr)                                  /* special HL attr. */
            {
                if (1 < t_colors)
                    aep = syn_cterm_attr2entry(attr);
                else
                    aep = syn_term_attr2entry(attr);
                if (aep == null)                                /* did ":syntax clear" */
                    attr = 0;
                else
                    attr = aep.ae_attr;
            }
            if ((attr & HL_BOLD) != 0 && T_MD[0] != null)          /* bold */
                out_str(T_MD[0]);
            else if (aep != null && 1 < t_colors && aep.ae_fg_color != 0 && cterm_normal_fg_bold != 0)
                /* If the Normal FG color has BOLD attribute
                 * and the new HL has a FG color defined, clear BOLD. */
                out_str(T_ME[0]);
            if ((attr & HL_STANDOUT) != 0 && T_SO[0] != null)      /* standout */
                out_str(T_SO[0]);
            if ((attr & (HL_UNDERLINE | HL_UNDERCURL)) != 0 && T_US[0] != null) /* underline or undercurl */
                out_str(T_US[0]);
            if ((attr & HL_ITALIC) != 0 && T_CZH[0] != null)       /* italic */
                out_str(T_CZH[0]);
            if ((attr & HL_INVERSE) != 0 && T_MR[0] != null)       /* inverse (reverse) */
                out_str(T_MR[0]);

            /*
             * Output the color or start string after bold etc.,
             * in case the bold etc. override the color setting.
             */
            if (aep != null)
            {
                if (1 < t_colors)
                {
                    if (aep.ae_fg_color != 0)
                        term_fg_color(aep.ae_fg_color - 1);
                    if (aep.ae_bg_color != 0)
                        term_bg_color(aep.ae_bg_color - 1);
                }
                else
                {
                    if (aep.ae_esc_start != null)
                        out_str(aep.ae_esc_start);
                }
            }
        }
    }

    /*private*/ static void screen_stop_highlight()
    {
        boolean do_ME = false;                          /* output T_ME code */

        if (screen_attr != 0)
        {
            if (HL_ALL < screen_attr)                   /* special HL attr. */
            {
                attrentry_C aep;

                if (1 < t_colors)
                {
                    /*
                     * Assume that t_me restores the original colors!
                     */
                    aep = syn_cterm_attr2entry(screen_attr);
                    if (aep != null && (aep.ae_fg_color != 0 || aep.ae_bg_color != 0))
                        do_ME = true;
                }
                else
                {
                    aep = syn_term_attr2entry(screen_attr);
                    if (aep != null && aep.ae_esc_stop != null)
                    {
                        if (STRCMP(aep.ae_esc_stop, T_ME[0]) == 0)
                            do_ME = true;
                        else
                            out_str(aep.ae_esc_stop);
                    }
                }
                if (aep == null)                        /* did ":syntax clear" */
                    screen_attr = 0;
                else
                    screen_attr = aep.ae_attr;
            }

            /*
             * Often all ending-codes are equal to T_ME.
             * Avoid outputting the same sequence several times.
             */
            if ((screen_attr & HL_STANDOUT) != 0)
            {
                if (STRCMP(T_SE[0], T_ME[0]) == 0)
                    do_ME = true;
                else
                    out_str(T_SE[0]);
            }
            if ((screen_attr & (HL_UNDERLINE | HL_UNDERCURL)) != 0)
            {
                if (STRCMP(T_UE[0], T_ME[0]) == 0)
                    do_ME = true;
                else
                    out_str(T_UE[0]);
            }
            if ((screen_attr & HL_ITALIC) != 0)
            {
                if (STRCMP(T_CZR[0], T_ME[0]) == 0)
                    do_ME = true;
                else
                    out_str(T_CZR[0]);
            }
            if (do_ME || (screen_attr & (HL_BOLD | HL_INVERSE)) != 0)
                out_str(T_ME[0]);

            if (1 < t_colors)
            {
                /* set Normal cterm colors */
                if (cterm_normal_fg_color != 0)
                    term_fg_color(cterm_normal_fg_color - 1);
                if (cterm_normal_bg_color != 0)
                    term_bg_color(cterm_normal_bg_color - 1);
                if (cterm_normal_fg_bold != 0)
                    out_str(T_MD[0]);
            }
        }
        screen_attr = 0;
    }

    /*
     * Reset the colors for a cterm.  Used when leaving Vim.
     * The machine specific code may override this again.
     */
    /*private*/ static void reset_cterm_colors()
    {
        if (1 < t_colors)
        {
            /* set Normal cterm colors */
            if (0 < cterm_normal_fg_color || 0 < cterm_normal_bg_color)
            {
                out_str(T_OP[0]);
                screen_attr = -1;
            }
            if (cterm_normal_fg_bold != 0)
            {
                out_str(T_ME[0]);
                screen_attr = -1;
            }
        }
    }

    /*
     * Put character screenLines["off"] on the screen at position "row" and "col",
     * using the attributes from screenAttrs["off"].
     */
    /*private*/ static void screen_char(int off, int row, int col)
    {
        /* Check for illegal values, just in case (could happen just after resizing). */
        if (screenRows <= row || screenColumns <= col)
            return;

        /* Outputting a character in the last cell on the screen may scroll the
         * screen up.  Only do it when the "xn" termcap property is set, otherwise
         * mark the character invalid (update it when scrolled up). */
        if (T_XN[0].at(0) == NUL
                && row == screenRows - 1 && col == screenColumns - 1
                /* account for first command-line character in rightleft mode */
                && !cmdmsg_rl)
        {
            screenAttrs[off] = -1;
            return;
        }

        /*
         * Stop highlighting first, so it's easier to move the cursor.
         */
        int attr;
        if (screen_char_attr != 0)
            attr = screen_char_attr;
        else
            attr = screenAttrs[off];
        if (screen_attr != attr)
            screen_stop_highlight();

        windgoto(row, col);

        if (screen_attr != attr)
            screen_start_highlight(attr);

        if (screenLinesUC[off] != 0)
        {
            Bytes buf = new Bytes(MB_MAXBYTES + 1);

            /* Convert UTF-8 character to bytes and write it. */
            buf.be(utfc_char2bytes(off, buf), NUL);

            out_str(buf);
            if (1 < utf_char2cells(screenLinesUC[off]))
                screen_cur_col++;
        }
        else
        {
            out_flush_check();
            out_char(screenLines.at(off));
        }

        screen_cur_col++;
    }

    /*
     * Draw a rectangle of the screen, inverted when "invert" is true.
     * This uses the contents of screenLines[] and doesn't change it.
     */
    /*private*/ static void screen_draw_rectangle(int row, int col, int height, int width, boolean invert)
    {
        /* Can't use "screenLines" unless initialized. */
        if (screenLines == null)
            return;

        if (invert)
            screen_char_attr = HL_INVERSE;
        for (int r = row; r < row + height; r++)
        {
            int off = lineOffset[r];
            int max_off = off + screenColumns;
            for (int c = col; c < col + width; c++)
            {
                screen_char(off + c, r, c);
                if (1 < utf_off2cells(off + c, max_off))
                    c++;
            }
        }
        screen_char_attr = 0;
    }

    /*
     * Redraw the characters for a vertically split window.
     */
    /*private*/ static void redraw_block(int row, int end, window_C wp)
    {
        clip_may_clear_selection(row, end - 1);

        int col;
        int width;

        if (wp == null)
        {
            col = 0;
            width = (int)Columns[0];
        }
        else
        {
            col = wp.w_wincol;
            width = wp.w_width;
        }

        screen_draw_rectangle(row, col, end - row, width, false);
    }

    /*
     * Fill the screen from 'start_row' to 'end_row', from 'start_col' to 'end_col'
     * with character 'c1' in first column followed by 'c2' in the other columns.
     * Use attributes 'attr'.
     */
    /*private*/ static void screen_fill(int start_row, int end_row, int start_col, int end_col, int c1, int c2, int attr)
    {
        boolean force_next = false;

        if (screenRows < end_row)               /* safety check */
            end_row = screenRows;
        if (screenColumns < end_col)            /* safety check */
            end_col = screenColumns;
        if (screenLines == null
                || end_row <= start_row
                || end_col <= start_col)        /* nothing to do */
            return;

        /* it's a "normal" terminal when not in a GUI or cterm */
        boolean norm_term = (t_colors <= 1);

        for (int row = start_row; row < end_row; row++)
        {
            /* When drawing over the right halve of a double-wide char clear out the left halve.
             * When drawing over the left halve of a double wide-char clear out the right halve.
             * Only needed in a terminal. */
            if (0 < start_col && mb_fix_col(start_col, row) != start_col)
                screen_puts_len(u8(" "), 1, row, start_col - 1, 0);
            if (end_col < screenColumns && mb_fix_col(end_col, row) != end_col)
                screen_puts_len(u8(" "), 1, row, end_col, 0);

            /*
             * Try to use delete-line termcap code, when no attributes or in a
             * "normal" terminal, where a bold/italic space is just a space.
             */
            boolean did_delete = false;
            if (c2 == ' '
                    && end_col == (int)Columns[0]
                    && can_clear(T_CE[0])
                    && (attr == 0
                        || (norm_term
                            && attr <= HL_ALL
                            && ((attr & ~(HL_BOLD | HL_ITALIC)) == 0))))
            {
                /*
                 * check if we really need to clear something
                 */
                int col = start_col;
                if (c1 != ' ')                      /* don't clear first char */
                    col++;

                int off = lineOffset[row] + col;
                int end_off = lineOffset[row] + end_col;

                /* skip blanks (used often, keep it fast!) */
                while (off < end_off && screenLines.at(off) == (byte)' '
                                     && screenAttrs[off] == 0
                                     && screenLinesUC[off] == 0)
                    off++;
                if (off < end_off)                  /* something to be cleared */
                {
                    col = off - lineOffset[row];
                    screen_stop_highlight();
                    term_windgoto(row, col);        /* clear rest of this screen line */
                    out_str(T_CE[0]);
                    screen_start();                 /* don't know where cursor is now */
                    col = end_col - col;
                    while (0 < col--)                   /* clear chars in "screenLines" */
                    {
                        screenLines.be(off, (byte)' ');
                        screenLinesUC[off] = 0;
                        screenAttrs[off] = 0;
                        off++;
                    }
                }
                did_delete = true;                  /* the chars are cleared now */
            }

            int off = lineOffset[row] + start_col;
            int c = c1;
            for (int col = start_col; col < end_col; col++)
            {
                if (screenLines.at(off) != c
                        || screenLinesUC[off] != (0x80 <= c ? c : 0)
                        || screenAttrs[off] != attr
                        || force_next)
                {
                    /* The bold trick may make a single row of pixels appear in
                     * the next character.  When a bold character is removed, the
                     * next character should be redrawn too.  This happens for our
                     * own GUI and for some xterms. */
                    if (term_is_xterm)
                    {
                        if (screenLines.at(off) != (byte)' '
                                && (HL_ALL < screenAttrs[off] || (screenAttrs[off] & HL_BOLD) != 0))
                            force_next = true;
                        else
                            force_next = false;
                    }
                    screenLines.be(off, c);
                    if (0x80 <= c)
                    {
                        screenLinesUC[off] = c;
                        screenLinesC[0][off] = 0;
                    }
                    else
                        screenLinesUC[off] = 0;
                    screenAttrs[off] = attr;
                    if (!did_delete || c != ' ')
                        screen_char(off, row, col);
                }
                off++;
                if (col == start_col)
                {
                    if (did_delete)
                        break;
                    c = c2;
                }
            }
            if (end_col == (int)Columns[0])
                lineWraps[row] = false;
            if (row == (int)Rows[0] - 1)                /* overwritten the command line */
            {
                redraw_cmdline = true;
                if (c1 == ' ' && c2 == ' ')
                    clear_cmdline = false;      /* command line has been cleared */
                if (start_col == 0)
                    mode_displayed = false;     /* mode cleared or overwritten */
            }
        }
    }

    /*
     * Check if there should be a delay.
     * Used before clearing or redrawing the screen or the command line.
     */
    /*private*/ static void check_for_delay(boolean check_msg_scroll)
    {
        if ((emsg_on_display || (check_msg_scroll && msg_scroll)) && !did_wait_return && emsg_silent == 0)
        {
            out_flush();
            ui_delay(1000L, true);
            emsg_on_display = false;
            if (check_msg_scroll)
                msg_scroll = false;
        }
    }

    /*
     * screen_valid -  allocate screen buffers if size changed
     *   If "doclear" is true: clear screen if it has been resized.
     *      Returns true if there is a valid screen to write to.
     *      Returns false when starting up and screen not initialized yet.
     */
    /*private*/ static boolean screen_valid(boolean doclear)
    {
        screenalloc(doclear);           /* allocate screen buffers if size changed */

        return (screenLines != null);
    }

    /*private*/ static boolean _4_entered;  /* avoid recursiveness */

    /*
     * Resize the shell to Rows and Columns.
     * Allocate screenLines[] and associated items.
     *
     * There may be some time between setting Rows and Columns and (re)allocating
     * screenLines[].  This happens when starting up and when (manually) changing
     * the shell size.  Always use screenRows and screenColumns to access items
     * in screenLines[].  Use Rows and Columns for positioning text etc. where the
     * final size of the shell is needed.
     */
    /*private*/ static void screenalloc(boolean doclear)
    {
        int[][] smco = new int[MAX_MCO][];

        int retry_count = 0;

        retry:
        for ( ; ; )
        {
            /*
             * Allocation of the screen buffers is done only when the size changes and
             * when Rows and Columns have been set and we have started doing full screen stuff.
             */
            if ((screenLines != null
                        && (int)Rows[0] == screenRows
                        && (int)Columns[0] == screenColumns
                        && screenLinesUC != null
                        && p_mco[0] == screen_mco)
                    || (int)Rows[0] == 0
                    || (int)Columns[0] == 0
                    || (!full_screen && screenLines == null))
                return;

            /*
             * It's possible that we produce an out-of-memory message below, which
             * will cause this function to be called again.  To break the loop, just return here.
             */
            if (_4_entered)
                return;
            _4_entered = true;

            /*
             * Note that the window sizes are updated before reallocating the arrays,
             * thus we must not redraw here!
             */
            redrawingDisabled++;

            win_new_shellsize();    /* fit the windows in the new sized shell */

            comp_col();             /* recompute columns for shown command and ruler */

            /*
             * We're changing the size of the screen.
             * - Allocate new arrays for "screenLines" and "screenAttrs".
             * - Move lines from the old arrays into the new arrays, clear extra
             *   lines (unless the screen is going to be cleared).
             * - Free the old arrays.
             *
             * If anything fails, make "screenLines" null, so we don't do anything!
             * Continuing with the old "screenLines" may result in a crash, because the size is wrong.
             */
            for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
                for (window_C wp = (tp == curtab) ? firstwin : tp.tp_firstwin; wp != null; wp = wp.w_next)
                    win_free_lines(wp);
            if (aucmd_win != null)
                win_free_lines(aucmd_win);

            Bytes slis = new Bytes((int)(Rows[0] + 1) * (int)Columns[0]);
            for (int i = 0; i < MAX_MCO; i++)
                smco[i] = null;
            int[] sluc = new int[(int)(Rows[0] + 1) * (int)Columns[0]];
            for (int i = 0; i < p_mco[0]; i++)
                smco[i] = new int[(int)(Rows[0] + 1) * (int)Columns[0]];

            int[] sats = new int[(int)(Rows[0] + 1) * (int)Columns[0]];
            int[] lofs = new int[(int)Rows[0]];
            boolean[] lwrs = new boolean[(int)Rows[0]];
            short[] tpis = new short[(int)Columns[0]];

            for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
                for (window_C wp = (tp == curtab) ? firstwin : tp.tp_firstwin; wp != null; wp = wp.w_next)
                    win_alloc_lines(wp);
            if (aucmd_win != null && aucmd_win.w_lines == null)
                win_alloc_lines(aucmd_win);

            for (int r = 0; r < Rows[0]; r++)
            {
                lofs[r] = r * (int)Columns[0];
                lwrs[r] = false;

                /*
                 * If the screen is not going to be cleared, copy as much as
                 * possible from the old screen to the new one and clear the rest
                 * (used when resizing the window at the "--more--" prompt or
                 * when executing an external command, for the GUI).
                 */
                if (!doclear)
                {
                    BFILL(slis, lofs[r], (byte)' ', (int)Columns[0]);
                    AFILL(sluc, lofs[r], 0, (int)Columns[0]);
                    for (int i = 0; i < p_mco[0]; i++)
                        AFILL(smco[i], lofs[r], 0, (int)Columns[0]);
                    AFILL(sats, lofs[r], 0, (int)Columns[0]);

                    int r0 = r + (screenRows - (int)Rows[0]);
                    if (0 <= r0 && screenLines != null)
                    {
                        int off = lineOffset[r0], len = (screenColumns < (int)Columns[0]) ? screenColumns : (int)Columns[0];

                        /* When switching to utf-8 don't copy characters, they
                         * may be invalid now.  Also when "p_mco" changes. */
                        if (screenLinesUC != null && p_mco[0] == screen_mco)
                        {
                            BCOPY(slis, lofs[r], screenLines, off, len);
                            ACOPY(sluc, lofs[r], screenLinesUC, off, len);
                            for (int i = 0; i < p_mco[0]; i++)
                                ACOPY(smco[i], lofs[r], screenLinesC[i], off, len);
                        }
                        ACOPY(sats, lofs[r], screenAttrs, off, len);
                    }
                }
            }

            /* Use the last line of the screen for the current line. */
            current_ScreenLine = slis.plus((int)Rows[0] * (int)Columns[0]);

            screenLines = slis;
            screenLinesUC = sluc;
            for (int i = 0; i < p_mco[0]; i++)
                screenLinesC[i] = smco[i];
            screen_mco = (int)p_mco[0];
            screenAttrs = sats;
            lineOffset = lofs;
            lineWraps = lwrs;
            tabPageIdxs = tpis;

            /* It's important that screenRows and screenColumns reflect the actual
             * size of screenLines[].  Set them before calling anything. */
            screenRows = (int)Rows[0];
            screenColumns = (int)Columns[0];

            must_redraw = CLEAR;        /* need to clear the screen later */
            if (doclear)
                screenclear2();

            _4_entered = false;
            --redrawingDisabled;

            /*
             * Do not apply autocommands more than 3 times to avoid an endless loop
             * in case applying autocommands always changes Rows or Columns.
             */
            if (starting == 0 && ++retry_count <= 3)
            {
                apply_autocmds(EVENT_VIMRESIZED, null, null, false, curbuf);
                /* In rare cases, autocommands may have altered Rows or Columns,
                 * jump back to check if we need to allocate the screen again. */
                continue retry;
            }

            break;
        }
    }

    /*private*/ static void screenclear()
    {
        check_for_delay(false);
        screenalloc(false);             /* allocate screen buffers if size changed */
        screenclear2();                 /* clear the screen */
    }

    /*private*/ static void screenclear2()
    {
        if (starting == NO_SCREEN || screenLines == null)
            return;

        screen_attr = -1;               /* force setting the Normal colors */
        screen_stop_highlight();        /* don't want highlighting here */

        /* disable selection without redrawing it */
        clip_scroll_selection(9999);

        /* blank out "screenLines" */
        for (int i = 0; i < Rows[0]; i++)
        {
            lineclear(lineOffset[i], (int)Columns[0]);
            lineWraps[i] = false;
        }

        if (can_clear(T_CL[0]))
        {
            out_str(T_CL[0]);              /* clear the display */
            clear_cmdline = false;
            mode_displayed = false;
        }
        else
        {
            /* can't clear the screen, mark all chars with invalid attributes */
            for (int i = 0; i < Rows[0]; i++)
                lineinvalid(lineOffset[i], (int)Columns[0]);
            clear_cmdline = true;
        }

        screen_cleared = TRUE;          /* can use contents of "screenLines" now */

        win_rest_invalid(firstwin);
        redraw_cmdline = true;
        redraw_tabline = true;
        if (must_redraw == CLEAR)       /* no need to clear again */
            must_redraw = NOT_VALID;
        compute_cmdrow();
        msg_row = cmdline_row;          /* put cursor on last line for messages */
        msg_col = 0;
        screen_start();                 /* don't know where cursor is now */
        msg_scrolled = 0;               /* can't scroll back */
        msg_didany = false;
        msg_didout = false;
    }

    /*
     * Clear one line in "screenLines".
     */
    /*private*/ static void lineclear(int off, int width)
    {
        BFILL(screenLines, off, (byte)' ', width);
        AFILL(screenLinesUC, off, 0, width);
        AFILL(screenAttrs, off, 0, width);
    }

    /*
     * Mark one line in "screenLines" invalid by setting the attributes to an invalid value.
     */
    /*private*/ static void lineinvalid(int off, int width)
    {
        AFILL(screenAttrs, off, -1, width);
    }

    /*
     * Copy part of a Screenline for vertically split window "wp".
     */
    /*private*/ static void linecopy(int to, int from, window_C wp)
    {
        int off_to = lineOffset[to] + wp.w_wincol;
        int off_from = lineOffset[from] + wp.w_wincol;

        BCOPY(screenLines, off_to, screenLines, off_from, wp.w_width);
        ACOPY(screenLinesUC, off_to, screenLinesUC, off_from, wp.w_width);
        for (int i = 0; i < p_mco[0]; i++)
            ACOPY(screenLinesC[i], off_to, screenLinesC[i], off_from, wp.w_width);
        ACOPY(screenAttrs, off_to, screenAttrs, off_from, wp.w_width);
    }

    /*
     * Return true if clearing with term string "p" would work.
     * It can't work when the string is empty or it won't set the right background.
     */
    /*private*/ static boolean can_clear(Bytes p)
    {
        return (p.at(0) != NUL && (t_colors <= 1 || cterm_normal_bg_color == 0 || T_UT[0].at(0) != NUL));
    }

    /*
     * Reset cursor position.  Use whenever cursor was moved because of outputting
     * something directly to the screen (shell commands) or a terminal control code.
     */
    /*private*/ static void screen_start()
    {
        screen_cur_row = screen_cur_col = 9999;
    }

    /*
     * Move the cursor to position "row","col" in the screen.
     * This tries to find the most efficient way to move, minimizing the number of
     * characters sent to the terminal.
     */
    /*private*/ static void windgoto(int row, int col)
    {
        final int
            GOTO_COST = 7,      /* assume a term_windgoto() takes about 7 chars */
            HIGHL_COST = 5;     /* assume unhighlight takes 5 chars */

        final int
            PLAN_LE = 1,
            PLAN_CR = 2,
            PLAN_NL = 3,
            PLAN_WRITE = 4;

        /* Can't use "screenLines" unless initialized. */
        if (screenLines == null)
            return;

        if (col != screen_cur_col || row != screen_cur_row)
        {
            int cost;

            /* Check for valid position. */
            if (row < 0)    /* window without text lines? */
                row = 0;
            if (screenRows <= row)
                row = screenRows - 1;
            if (screenColumns <= col)
                col = screenColumns - 1;

            /* check if no cursor movement is allowed in highlight mode */
            int noinvcurs;
            if (screen_attr != 0 && T_MS[0].at(0) == NUL)
                noinvcurs = HIGHL_COST;
            else
                noinvcurs = 0;
            int goto_cost = GOTO_COST + noinvcurs;

            /*
             * Plan how to do the positioning:
             *
             * 1. Use CR to move it to column 0, same row.
             * 2. Use T_LE to move it a few columns to the left.
             * 3. Use NL to move a few lines down, column 0.
             * 4. Move a few columns to the right with T_ND or by writing chars.
             *
             * Don't do this if the cursor went beyond the last column,
             * the cursor position is unknown then (some terminals wrap, some don't).
             *
             * First check if the highlighting attributes allow us to write
             * characters to move the cursor to the right.
             */
            if (screen_cur_row <= row && screen_cur_col < (int)Columns[0])
            {
                int plan;
                int wouldbe_col;

                /*
                 * If the cursor is in the same row, bigger col, we can use CR or T_LE.
                 */
                Bytes bs = null;
                int attr = screen_attr;
                if (row == screen_cur_row && col < screen_cur_col)
                {
                    /* "le" is preferred over "bc", because "bc" is obsolete */
                    if (T_LE[0].at(0) != NUL)
                        bs = T_LE[0];              /* "cursor left" */
                    else
                        bs = T_BC[0];              /* "backspace character (old) */
                    if (bs.at(0) != NUL)
                        cost = (screen_cur_col - col) * strlen(bs);
                    else
                        cost = 999;
                    if (col + 1 < cost)         /* using CR is less characters */
                    {
                        plan = PLAN_CR;
                        wouldbe_col = 0;
                        cost = 1;               /* CR is just one character */
                    }
                    else
                    {
                        plan = PLAN_LE;
                        wouldbe_col = col;
                    }
                    if (noinvcurs != 0)              /* will stop highlighting */
                    {
                        cost += noinvcurs;
                        attr = 0;
                    }
                }

                /*
                 * If the cursor is above where we want to be, we can use CR LF.
                 */
                else if (screen_cur_row < row)
                {
                    plan = PLAN_NL;
                    wouldbe_col = 0;
                    cost = (row - screen_cur_row) * 2;  /* CR LF */
                    if (noinvcurs != 0)              /* will stop highlighting */
                    {
                        cost += noinvcurs;
                        attr = 0;
                    }
                }

                /*
                 * If the cursor is in the same row, smaller col, just use write.
                 */
                else
                {
                    plan = PLAN_WRITE;
                    wouldbe_col = screen_cur_col;
                    cost = 0;
                }

                /*
                 * Check if any characters that need to be written have the
                 * correct attributes.  Also avoid UTF-8 characters.
                 */
                int i = col - wouldbe_col;
                if (0 < i)
                    cost += i;
                if (cost < goto_cost && 0 < i)
                {
                    /*
                     * Check if the attributes are correct without additionally
                     * stopping highlighting.
                     */
                    int ai = lineOffset[row] + wouldbe_col;
                    while (i != 0 && screenAttrs[ai++] == attr)
                        --i;
                    if (i != 0)
                    {
                        /*
                         * Try if it works when highlighting is stopped here.
                         */
                        if (screenAttrs[--ai] == 0)
                        {
                            cost += noinvcurs;
                            while (i != 0 && screenAttrs[ai++] == 0)
                                --i;
                        }
                        if (i != 0)
                            cost = 999;     /* different attributes, don't do it */
                    }

                    /* Don't use an UTF-8 char for positioning, it's slow. */
                    for (i = wouldbe_col; i < col; i++)
                        if (screenLinesUC[lineOffset[row] + i] != 0)
                        {
                            cost = 999;
                            break;
                        }
                }

                /*
                 * We can do it without term_windgoto()!
                 */
                if (cost < goto_cost)
                {
                    if (plan == PLAN_LE)
                    {
                        if (noinvcurs != 0)
                            screen_stop_highlight();
                        while (col < screen_cur_col)
                        {
                            out_str(bs);
                            --screen_cur_col;
                        }
                    }
                    else if (plan == PLAN_CR)
                    {
                        if (noinvcurs != 0)
                            screen_stop_highlight();
                        out_char((byte)'\r');
                        screen_cur_col = 0;
                    }
                    else if (plan == PLAN_NL)
                    {
                        if (noinvcurs != 0)
                            screen_stop_highlight();
                        while (screen_cur_row < row)
                        {
                            out_char((byte)'\n');
                            screen_cur_row++;
                        }
                        screen_cur_col = 0;
                    }

                    i = col - screen_cur_col;
                    if (0 < i)
                    {
                        /*
                         * Use cursor-right if it's one character only.
                         * Avoids removing a line of pixels from the last bold char,
                         * when using the bold trick in the GUI.
                         */
                        if (T_ND[0].at(0) != NUL && T_ND[0].at(1) == NUL)
                        {
                            while (0 < i--)
                                out_char(T_ND[0].at(0));
                        }
                        else
                        {
                            int off = lineOffset[row] + screen_cur_col;

                            while (0 < i--)
                            {
                                if (screenAttrs[off] != screen_attr)
                                    screen_stop_highlight();
                                out_flush_check();
                                out_char(screenLines.at(off));
                                off++;
                            }
                        }
                    }
                }
            }
            else
                cost = 999;

            if (goto_cost <= cost)
            {
                if (noinvcurs != 0)
                    screen_stop_highlight();
                if (row == screen_cur_row && (screen_cur_col < col) && T_CRI[0].at(0) != NUL)
                    term_cursor_right(col - screen_cur_col);
                else
                    term_windgoto(row, col);
            }
            screen_cur_row = row;
            screen_cur_col = col;
        }
    }

    /*
     * Set cursor to its position in the current window.
     */
    /*private*/ static void setcursor()
    {
        if (redrawing())
        {
            validate_cursor();

            /* With 'rightleft' set and the cursor on a double-wide character,
             * position it on the leftmost column. */
            int wcol = curwin.w_onebuf_opt.wo_rl[0]
                ? (curwin.w_width - curwin.w_wcol
                    - ((us_ptr2cells(ml_get_cursor()) == 2 && vim_isprintc(gchar_cursor())) ? 2 : 1))
                : curwin.w_wcol;

            windgoto(curwin.w_winrow + curwin.w_wrow, curwin.w_wincol + wcol);
        }
    }

    /*
     * insert 'line_count' lines at 'row' in window 'wp'
     * if 'invalid' is true the wp.w_lines[].wl_lnum is invalidated.
     * if 'mayclear' is true the screen will be cleared if it is faster than scrolling.
     * Returns false if the lines are not inserted, true for success.
     */
    /*private*/ static boolean win_ins_lines(window_C wp, int row, int line_count, boolean invalid, boolean mayclear)
    {
        if (invalid)
            wp.w_lines_valid = 0;

        if (wp.w_height < 5)
            return false;

        if (line_count > wp.w_height - row)
            line_count = wp.w_height - row;

        /*MAYBEAN*/int maybe = win_do_lines(wp, row, line_count, mayclear, false);
        if (maybe != MAYBE)
            return (maybe != FALSE);

        /*
         * If there is a next window or a status line, we first try to delete the
         * lines at the bottom to avoid messing what is after the window.
         * If this fails and there are following windows, don't do anything to avoid
         * messing up those windows, better just redraw.
         */
        boolean did_delete = false;
        if (wp.w_next != null || wp.w_status_height != 0)
        {
            if (screen_del_lines(0, wp.w_winrow + wp.w_height - line_count,
                                        line_count, (int)Rows[0], false, null) == true)
                did_delete = true;
            else if (wp.w_next != null)
                return false;
        }
        /*
         * if no lines deleted, blank the lines that will end up below the window
         */
        if (!did_delete)
        {
            wp.w_redr_status = true;
            redraw_cmdline = true;
            int nextrow = wp.w_winrow + wp.w_height + wp.w_status_height;
            int lastrow = nextrow + line_count;
            if (Rows[0] < lastrow)
                lastrow = (int)Rows[0];
            screen_fill(nextrow - line_count, lastrow - line_count,
                        wp.w_wincol, wp.w_wincol + wp.w_width,
                        ' ', ' ', 0);
        }

        if (screen_ins_lines(0, wp.w_winrow + row, line_count, (int)Rows[0], null) == false)
        {
            /* deletion will have messed up other windows */
            if (did_delete)
            {
                wp.w_redr_status = true;
                win_rest_invalid(wp.w_next);
            }
            return false;
        }

        return true;
    }

    /*
     * delete "line_count" window lines at "row" in window "wp"
     * If "invalid" is true curwin.w_lines[] is invalidated.
     * If "mayclear" is true the screen will be cleared if it is faster than scrolling
     * Return true for success, false if the lines are not deleted.
     */
    /*private*/ static boolean win_del_lines(window_C wp, int row, int line_count, boolean invalid, boolean mayclear)
    {
        if (invalid)
            wp.w_lines_valid = 0;

        if (line_count > wp.w_height - row)
            line_count = wp.w_height - row;

        /*MAYBEAN*/int maybe = win_do_lines(wp, row, line_count, mayclear, true);
        if (maybe != MAYBE)
            return (maybe != FALSE);

        if (screen_del_lines(0, wp.w_winrow + row, line_count, (int)Rows[0], false, null) == false)
            return false;

        /*
         * If there are windows or status lines below, try to put them at the
         * correct place.  If we can't do that, they have to be redrawn.
         */
        if (wp.w_next != null || wp.w_status_height != 0 || cmdline_row < Rows[0] - 1)
        {
            if (screen_ins_lines(0, wp.w_winrow + wp.w_height - line_count,
                                             line_count, (int)Rows[0], null) == false)
            {
                wp.w_redr_status = true;
                win_rest_invalid(wp.w_next);
            }
        }
        /* If this is the last window and there is no status line, redraw the command line later. */
        else
            redraw_cmdline = true;

        return true;
    }

    /*
     * Common code for win_ins_lines() and win_del_lines().
     * Returns true or false when the work has been done.
     * Returns MAYBE when not finished yet.
     */
    /*private*/ static /*MAYBEAN*/int win_do_lines(window_C wp, int row, int line_count, boolean mayclear, boolean del)
    {
        if (!redrawing() || line_count <= 0)
            return FALSE;

        /* only a few lines left: redraw is faster */
        if (mayclear && Rows[0] - line_count < 5 && wp.w_width == (int)Columns[0])
        {
            screenclear();      /* will set wp.w_lines_valid to 0 */
            return FALSE;
        }

        /*
         * Delete all remaining lines
         */
        if (wp.w_height <= row + line_count)
        {
            screen_fill(wp.w_winrow + row, wp.w_winrow + wp.w_height,
                        wp.w_wincol, wp.w_wincol + wp.w_width,
                        ' ', ' ', 0);
            return TRUE;
        }

        /*
         * when scrolling, the message on the command line should be cleared,
         * otherwise it will stay there forever.
         */
        clear_cmdline = true;

        /*
         * If the terminal can set a scroll region, use that.
         * Always do this in a vertically split window.  This will redraw from
         * screenLines[] when t_CV isn't defined.  That's faster than using win_line().
         * Don't use a scroll region when we are going to redraw the text, writing
         * a character in the lower right corner of the scroll region causes a
         * scroll-up in the DJGPP version.
         */
        if (scroll_region || wp.w_width != (int)Columns[0])
        {
            if (scroll_region && (wp.w_width == (int)Columns[0] || T_CSV[0].at(0) != NUL))
                scroll_region_set(wp, row);

            boolean r;
            if (del)
                r = screen_del_lines(wp.w_winrow + row, 0, line_count, wp.w_height - row, false, wp);
            else
                r = screen_ins_lines(wp.w_winrow + row, 0, line_count, wp.w_height - row, wp);

            if (scroll_region && (wp.w_width == (int)Columns[0] || T_CSV[0].at(0) != NUL))
                scroll_region_reset();

            return r ? TRUE : FALSE;
        }

        if (wp.w_next != null && p_tf[0]) /* don't delete/insert on fast terminal */
            return FALSE;

        return MAYBE;
    }

    /*
     * window 'wp' and everything after it is messed up, mark it for redraw
     */
    /*private*/ static void win_rest_invalid(window_C wp)
    {
        while (wp != null)
        {
            redraw_win_later(wp, NOT_VALID);
            wp.w_redr_status = true;
            wp = wp.w_next;
        }
        redraw_cmdline = true;
    }

    /*
     * The rest of the routines in this file perform screen manipulations.
     * The given operation is performed physically on the screen.
     * The corresponding change is also made to the internal screen image.
     * In this way, the editor anticipates the effect of editing changes
     * on the appearance of the screen.
     * That way, when we call screenupdate a complete redraw isn't usually necessary.
     * Another advantage is that we can keep adding code to anticipate screen changes,
     * and in the meantime, everything still works.
     */

    /*
     * types for inserting or deleting lines
     */
    /*private*/ static final int USE_T_CAL   = 1;
    /*private*/ static final int USE_T_CDL   = 2;
    /*private*/ static final int USE_T_AL    = 3;
    /*private*/ static final int USE_T_CE    = 4;
    /*private*/ static final int USE_T_DL    = 5;
    /*private*/ static final int USE_T_SR    = 6;
    /*private*/ static final int USE_NL      = 7;
    /*private*/ static final int USE_T_CD    = 8;
    /*private*/ static final int USE_REDRAW  = 9;

    /*
     * insert lines on the screen and update screenLines[]
     * 'end' is the line after the scrolled part.  Normally it is Rows.
     * When scrolling region used 'off' is the offset from the top for the region.
     * 'row' and 'end' are relative to the start of the region.
     *
     * return false for failure, true for success.
     */
    /*private*/ static boolean screen_ins_lines(int off, int row, int line_count, int end, window_C wp)
        /* wp: null or window to use width from */
    {
        boolean can_ce = can_clear(T_CE[0]);

        /*
         * FAIL if
         * - there is no valid screen
         * - the screen has to be redrawn completely
         * - the line count is less than one
         * - the line count is more than 'ttyscroll'
         */
        if (!screen_valid(true) || line_count <= 0 || p_ttyscroll[0] < line_count)
            return false;

        /*
         * There are seven ways to insert lines:
         *
         * 0. When in a vertically split window and t_CV isn't set, redraw the characters from screenLines[].
         * 1. Use T_CD (clear to end of display) if it exists and the result of the insert is just empty lines.
         * 2. Use T_CAL (insert multiple lines) if it exists and T_AL is not present or line_count > 1.
         *    It looks better if we do all the inserts at once.
         * 3. Use T_CDL (delete multiple lines) if it exists and the result of the
         *    insert is just empty lines and T_CE is not present or line_count > 1.
         * 4. Use T_AL (insert line) if it exists.
         * 5. Use T_CE (erase line) if it exists and the result of the insert is just empty lines.
         * 6. Use T_DL (delete line) if it exists and the result of the insert is just empty lines.
         * 7. Use T_SR (scroll reverse) if it exists and inserting at row 0
         *    and the 'da' flag is not set or we have clear line capability.
         * 8. Redraw the characters from screenLines[].
         *
         * Careful: In a hpterm scroll reverse doesn't work as expected, it moves
         * the scrollbar for the window.  It does have insert line, use that if it exists.
         */
        boolean result_empty = (end <= row + line_count);

        int type;
        if (wp != null && wp.w_width != (int)Columns[0] && T_CSV[0].at(0) == NUL)
            type = USE_REDRAW;
        else if (can_clear(T_CD[0]) && result_empty)
            type = USE_T_CD;
        else if (T_CAL[0].at(0) != NUL && (1 < line_count || T_AL[0].at(0) == NUL))
            type = USE_T_CAL;
        else if (T_CDL[0].at(0) != NUL && result_empty && (1 < line_count || !can_ce))
            type = USE_T_CDL;
        else if (T_AL[0].at(0) != NUL)
            type = USE_T_AL;
        else if (can_ce && result_empty)
            type = USE_T_CE;
        else if (T_DL[0].at(0) != NUL && result_empty)
            type = USE_T_DL;
        else if (T_SR[0].at(0) != NUL && row == 0 && (T_DA[0].at(0) == NUL || can_ce))
            type = USE_T_SR;
        else
            return false;

        /*
         * For clearing the lines screen_del_lines() is used.  This will also take
         * care of t_db if necessary.
         */
        if (type == USE_T_CD || type == USE_T_CDL || type == USE_T_CE || type == USE_T_DL)
            return screen_del_lines(off, row, line_count, end, false, wp);

        /*
         * If text is retained below the screen, first clear or delete as many
         * lines at the bottom of the window as are about to be inserted so that
         * the deleted lines won't later surface during a screen_del_lines.
         */
        if (T_DB[0].at(0) != NUL)
            screen_del_lines(off, end - line_count, line_count, end, false, wp);

        /* Remove a modeless selection when inserting lines halfway the screen
         * or not the full width of the screen. */
        if (0 < off + row || (wp != null && wp.w_width != (int)Columns[0]))
            clip_clear_selection(clip_star);
        else
            clip_scroll_selection(-line_count);

        int cursor_row;
        if (T_CCS[0].at(0) != NUL)      /* cursor relative to region */
            cursor_row = row;
        else
            cursor_row = row + off;

        /*
         * Shift lineOffset[] line_count down to reflect the inserted lines.
         * Clear the inserted lines in screenLines[].
         */
        row += off;
        end += off;
        for (int i = 0; i < line_count; i++)
        {
            if (wp != null && wp.w_width != (int)Columns[0])
            {
                /* need to copy part of a line */
                int j = end - 1 - i;
                while (row <= (j -= line_count))
                    linecopy(j + line_count, j, wp);
                j += line_count;
                if (can_clear(u8(" ")))
                    lineclear(lineOffset[j] + wp.w_wincol, wp.w_width);
                else
                    lineinvalid(lineOffset[j] + wp.w_wincol, wp.w_width);
                lineWraps[j] = false;
            }
            else
            {
                int j = end - 1 - i;
                int temp = lineOffset[j];
                while (row <= (j -= line_count))
                {
                    lineOffset[j + line_count] = lineOffset[j];
                    lineWraps[j + line_count] = lineWraps[j];
                }
                lineOffset[j + line_count] = temp;
                lineWraps[j + line_count] = false;
                if (can_clear(u8(" ")))
                    lineclear(temp, (int)Columns[0]);
                else
                    lineinvalid(temp, (int)Columns[0]);
            }
        }

        screen_stop_highlight();
        windgoto(cursor_row, 0);

        /* redraw the characters */
        if (type == USE_REDRAW)
            redraw_block(row, end, wp);
        else if (type == USE_T_CAL)
        {
            term_append_lines(line_count);
            screen_start();         /* don't know where cursor is now */
        }
        else
        {
            for (int i = 0; i < line_count; i++)
            {
                if (type == USE_T_AL)
                {
                    if (i != 0 && cursor_row != 0)
                        windgoto(cursor_row, 0);
                    out_str(T_AL[0]);
                }
                else /* type == USE_T_SR */
                    out_str(T_SR[0]);
                screen_start();         /* don't know where cursor is now */
            }
        }

        /*
         * With scroll-reverse and 'da' flag set we need to clear the lines that
         * have been scrolled down into the region.
         */
        if (type == USE_T_SR && T_DA[0].at(0) != NUL)
        {
            for (int i = 0; i < line_count; i++)
            {
                windgoto(off + i, 0);
                out_str(T_CE[0]);
                screen_start();         /* don't know where cursor is now */
            }
        }

        return true;
    }

    /*
     * delete lines on the screen and update screenLines[]
     * 'end' is the line after the scrolled part.  Normally it is Rows.
     * When scrolling region used 'off' is the offset from the top for the region.
     * 'row' and 'end' are relative to the start of the region.
     *
     * Return true for success, false if the lines are not deleted.
     */
    /*private*/ static boolean screen_del_lines(int off, int row, int line_count, int end, boolean force, window_C wp)
        /* force: even when line_count > p_ttyscroll */
        /* wp: null or window to use width from */
    {
        /*
         * FAIL if
         * - there is no valid screen
         * - the screen has to be redrawn completely
         * - the line count is less than one
         * - the line count is more than 'ttyscroll'
         */
        if (!screen_valid(true) || line_count <= 0 || (!force && p_ttyscroll[0] < line_count))
            return false;

        /*
         * Check if the rest of the current region will become empty.
         */
        boolean result_empty = (end <= row + line_count);

        /*
         * We can delete lines only when 'db' flag not set or when 'ce' option available.
         */
        boolean can_delete = (T_DB[0].at(0) == NUL || can_clear(T_CE[0]));

        int type;
        /*
         * There are six ways to delete lines:
         *
         * 0. When in a vertically split window and t_CV isn't set, redraw the characters from screenLines[].
         * 1. Use T_CD if it exists and the result is empty.
         * 2. Use newlines if row == 0 and count == 1 or T_CDL does not exist.
         * 3. Use T_CDL (delete multiple lines) if it exists and line_count > 1 or none of the other ways work.
         * 4. Use T_CE (erase line) if the result is empty.
         * 5. Use T_DL (delete line) if it exists.
         * 6. Redraw the characters from screenLines[].
         */
        if (wp != null && wp.w_width != (int)Columns[0] && T_CSV[0].at(0) == NUL)
            type = USE_REDRAW;
        else if (can_clear(T_CD[0]) && result_empty)
            type = USE_T_CD;
        else if (row == 0 && (line_count == 1 || T_CDL[0].at(0) == NUL))
            type = USE_NL;
        else if (T_CDL[0].at(0) != NUL && 1 < line_count && can_delete)
            type = USE_T_CDL;
        else if (can_clear(T_CE[0]) && result_empty && (wp == null || wp.w_width == (int)Columns[0]))
            type = USE_T_CE;
        else if (T_DL[0].at(0) != NUL && can_delete)
            type = USE_T_DL;
        else if (T_CDL[0].at(0) != NUL && can_delete)
            type = USE_T_CDL;
        else
            return false;

        /* Remove a modeless selection when deleting lines halfway the screen
         * or not the full width of the screen. */
        if (0 < off + row || (wp != null && wp.w_width != (int)Columns[0]))
            clip_clear_selection(clip_star);
        else
            clip_scroll_selection(line_count);

        int cursor_row;
        int cursor_end;
        if (T_CCS[0].at(0) != NUL)      /* cursor relative to region */
        {
            cursor_row = row;
            cursor_end = end;
        }
        else
        {
            cursor_row = row + off;
            cursor_end = end + off;
        }

        /*
         * Now shift lineOffset[] line_count up to reflect the deleted lines.
         * Clear the inserted lines in screenLines[].
         */
        row += off;
        end += off;
        for (int i = 0; i < line_count; i++)
        {
            if (wp != null && wp.w_width != (int)Columns[0])
            {
                /* need to copy part of a line */
                int j = row + i;
                while ((j += line_count) <= end - 1)
                    linecopy(j - line_count, j, wp);
                j -= line_count;
                if (can_clear(u8(" ")))
                    lineclear(lineOffset[j] + wp.w_wincol, wp.w_width);
                else
                    lineinvalid(lineOffset[j] + wp.w_wincol, wp.w_width);
                lineWraps[j] = false;
            }
            else
            {
                /* whole width, moving the line pointers is faster */
                int j = row + i;
                int temp = lineOffset[j];
                while ((j += line_count) <= end - 1)
                {
                    lineOffset[j - line_count] = lineOffset[j];
                    lineWraps[j - line_count] = lineWraps[j];
                }
                lineOffset[j - line_count] = temp;
                lineWraps[j - line_count] = false;
                if (can_clear(u8(" ")))
                    lineclear(temp, (int)Columns[0]);
                else
                    lineinvalid(temp, (int)Columns[0]);
            }
        }

        screen_stop_highlight();

        /* redraw the characters */
        if (type == USE_REDRAW)
            redraw_block(row, end, wp);
        else if (type == USE_T_CD)          /* delete the lines */
        {
            windgoto(cursor_row, 0);
            out_str(T_CD[0]);
            screen_start();                 /* don't know where cursor is now */
        }
        else if (type == USE_T_CDL)
        {
            windgoto(cursor_row, 0);
            term_delete_lines(line_count);
            screen_start();                 /* don't know where cursor is now */
        }
        /*
         * Deleting lines at top of the screen or scroll region: Just scroll
         * the whole screen (scroll region) up by outputting newlines on the last line.
         */
        else if (type == USE_NL)
        {
            windgoto(cursor_end - 1, 0);
            for (int i = line_count; 0 <= --i; )
                out_char((byte)'\n');             /* cursor will remain on same line */
        }
        else
        {
            for (int i = line_count; 0 <= --i; )
            {
                if (type == USE_T_DL)
                {
                    windgoto(cursor_row, 0);
                    out_str(T_DL[0]);          /* delete a line */
                }
                else /* type == USE_T_CE */
                {
                    windgoto(cursor_row + i, 0);
                    out_str(T_CE[0]);          /* erase a line */
                }
                screen_start();             /* don't know where cursor is now */
            }
        }

        /*
         * If the 'db' flag is set, we need to clear the lines that have been
         * scrolled up at the bottom of the region.
         */
        if (T_DB[0].at(0) != NUL && (type == USE_T_DL || type == USE_T_CDL))
        {
            for (int i = line_count; 0 < i; --i)
            {
                windgoto(cursor_end - i, 0);
                out_str(T_CE[0]);              /* erase a line */
                screen_start();             /* don't know where cursor is now */
            }
        }

        return true;
    }

    /*
     * show the current mode and ruler
     *
     * If clear_cmdline is true, clear the rest of the cmdline.
     * If clear_cmdline is false there may be a message there that needs to be
     * cleared only if a mode is shown.
     * Return the length of the message (0 if no message).
     */
    /*private*/ static int showmode()
    {
        int length = 0;

        boolean do_mode = ((p_smd[0] && msg_silent == 0) && ((State & INSERT) != 0 || restart_edit != 0 || VIsual_active));
        if (do_mode || Recording)
        {
            /*
             * Don't show mode right now, when not redrawing or inside a mapping.
             * Call char_avail() only when we are going to show something, because
             * it takes a bit of time.
             */
            if (!redrawing() || (char_avail() && !keyTyped) || msg_silent != 0)
            {
                redraw_cmdline = true;              /* show mode later */
                return 0;
            }

            boolean nwr_save = need_wait_return;

            /* wait a bit before overwriting an important message */
            check_for_delay(false);

            /* if the cmdline is more than one line high, erase top lines */
            boolean need_clear = clear_cmdline;
            if (clear_cmdline && cmdline_row < Rows[0] - 1)
                msg_clr_cmdline();                  /* will reset clear_cmdline */

            /* Position on the last line in the window, column 0. */
            msg_pos_mode();
            cursor_off();
            int attr = hl_attr(HLF_CM);                 /* Highlight mode */
            if (do_mode)
            {
                msg_puts_attr(u8("--"), attr);

                if ((State & VREPLACE_FLAG) != 0)
                    msg_puts_attr(u8(" VREPLACE"), attr);
                else if ((State & REPLACE_FLAG) != 0)
                    msg_puts_attr(u8(" REPLACE"), attr);
                else if ((State & INSERT) != 0)
                {
                    if (p_ri[0])
                        msg_puts_attr(u8(" REVERSE"), attr);
                    msg_puts_attr(u8(" INSERT"), attr);
                }
                else if (restart_edit == 'I')
                    msg_puts_attr(u8(" (insert)"), attr);
                else if (restart_edit == 'R')
                    msg_puts_attr(u8(" (replace)"), attr);
                else if (restart_edit == 'V')
                    msg_puts_attr(u8(" (vreplace)"), attr);

                if ((State & INSERT) != 0 && p_paste[0])
                    msg_puts_attr(u8(" (paste)"), attr);

                if (VIsual_active)
                {
                    Bytes p;

                    /* Don't concatenate separate words to avoid translation problems. */
                    switch ((VIsual_select ? 4 : 0) + ((VIsual_mode == Ctrl_V) ? 2 : 0) + ((VIsual_mode == 'V') ? 1 : 0))
                    {
                        case 0: p = u8(" VISUAL"); break;
                        case 1: p = u8(" VISUAL LINE"); break;
                        case 2: p = u8(" VISUAL BLOCK"); break;
                        case 4: p = u8(" SELECT"); break;
                        case 5: p = u8(" SELECT LINE"); break;
                        default: p = u8(" SELECT BLOCK"); break;
                    }
                    msg_puts_attr(p, attr);
                }
                msg_puts_attr(u8(" --"), attr);

                need_clear = true;
            }
            if (Recording)
            {
                msg_puts_attr(u8("recording"), attr);
                need_clear = true;
            }

            mode_displayed = true;
            if (need_clear || clear_cmdline)
                msg_clr_eos();
            msg_didout = false;             /* overwrite this message */
            length = msg_col;
            msg_col = 0;
            need_wait_return = nwr_save;    /* never ask for hit-return for this */
        }
        else if (clear_cmdline && msg_silent == 0)
            /* Clear the whole command line.  Will reset "clear_cmdline". */
            msg_clr_cmdline();

        /* In Visual mode the size of the selected area must be redrawn. */
        if (VIsual_active)
            clear_showcmd();

        /* If the last window has no status line,
         * the ruler is after the mode message and must be redrawn. */
        if (redrawing() && lastwin.w_status_height == 0)
            win_redr_ruler(lastwin, true);
        redraw_cmdline = false;
        clear_cmdline = false;

        return length;
    }

    /*
     * Position for a mode message.
     */
    /*private*/ static void msg_pos_mode()
    {
        msg_col = 0;
        msg_row = (int)Rows[0] - 1;
    }

    /*
     * Delete mode message.  Used when ESC is typed which is expected to end
     * Insert mode (but Insert mode didn't end yet!).
     * Caller should check "mode_displayed".
     */
    /*private*/ static void unshowmode(boolean force)
    {
        /*
         * Don't delete it right now, when not redrawing or inside a mapping.
         */
        if (!redrawing() || (!force && char_avail() && !keyTyped))
            redraw_cmdline = true;          /* delete mode later */
        else
        {
            msg_pos_mode();
            if (Recording)
                msg_puts_attr(u8("recording"), hl_attr(HLF_CM));
            msg_clr_eos();
        }
    }

    /*
     * Draw the tab pages line at the top of the Vim window.
     */
    /*private*/ static void draw_tabline()
    {
        int tabcount = 0;
        int col = 0;
        int attr_sel = hl_attr(HLF_TPS);
        int attr_nosel = hl_attr(HLF_TP);
        int attr_fill = hl_attr(HLF_TPF);
        boolean use_sep_chars = (t_colors < 8);

        redraw_tabline = false;

        if (tabline_height() < 1)
            return;

        /* Init tabPageIdxs[] to zero: Clicking outside of tabs has no effect. */
        for (int scol = 0; scol < (int)Columns[0]; scol++)
            tabPageIdxs[scol] = 0;

        /* Use the 'tabline' option if it's set. */
        if (p_tal[0].at(0) != NUL)
        {
            boolean save_called_emsg = called_emsg;

            /* Check for an error.
             * If there is one we would loop in redrawing the screen.
             * Avoid that by making 'tabline' empty. */
            called_emsg = false;
            win_redr_custom(null, false);
            if (called_emsg)
                set_string_option_direct(u8("tabline"), -1, u8(""), OPT_FREE, SID_ERROR);
            called_emsg |= save_called_emsg;
        }
        else
        {
            for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
                tabcount++;

            int tabwidth = ((int)Columns[0] - 1 + tabcount / 2) / tabcount;
            if (tabwidth < 6)
                tabwidth = 6;

            int attr = attr_nosel;
            tabcount = 0;
            int scol = 0;
            for (tabpage_C tp = first_tabpage; tp != null && col < (int)Columns[0] - 4; tp = tp.tp_next)
            {
                scol = col;

                if (tp.tp_topframe == topframe)
                    attr = attr_sel;
                if (use_sep_chars && 0 < col)
                    screen_putchar('|', 0, col++, attr);

                if (tp.tp_topframe != topframe)
                    attr = attr_nosel;

                screen_putchar(' ', 0, col++, attr);

                window_C cwp, wp;
                if (tp == curtab)
                {
                    cwp = curwin;
                    wp = firstwin;
                }
                else
                {
                    cwp = tp.tp_curwin;
                    wp = tp.tp_firstwin;
                }

                boolean modified = false;
                int wincount;
                for (wincount = 0; wp != null; wp = wp.w_next, ++wincount)
                    if (bufIsChanged(wp.w_buffer))
                        modified = true;
                if (modified || 1 < wincount)
                {
                    if (1 < wincount)
                    {
                        vim_snprintf(nameBuff, MAXPATHL, u8("%d"), wincount);
                        int len = strlen(nameBuff);
                        if ((int)Columns[0] - 3 <= col + len)
                            break;
                        screen_puts_len(nameBuff, len, 0, col, hl_combine_attr(attr, hl_attr(HLF_T)));
                        col += len;
                    }
                    if (modified)
                        screen_puts_len(u8("+"), 1, 0, col++, attr);
                    screen_putchar(' ', 0, col++, attr);
                }

                int room = scol - col + tabwidth - 1;
                if (0 < room)
                {
                    /* Get buffer name in nameBuff[]. */
                    get_trans_bufname(cwp.w_buffer);
                    shorten_dir(nameBuff);
                    int len = mb_string2cells(nameBuff, -1);
                    Bytes p = nameBuff;

                    while (room < len)
                    {
                        len -= mb_ptr2cells(p);
                        p = p.plus(us_ptr2len_cc(p));
                    }

                    if (len > (int)Columns[0] - col - 1)
                        len = (int)Columns[0] - col - 1;

                    screen_puts_len(p, strlen(p), 0, col, attr);
                    col += len;
                }
                screen_putchar(' ', 0, col++, attr);

                /* Store the tab page number in tabPageIdxs[],
                 * so that jump_to_mouse() knows where each one is. */
                tabcount++;
                while (scol < col)
                    tabPageIdxs[scol++] = (short)tabcount;
            }

            int c;
            if (use_sep_chars)
                c = '_';
            else
                c = ' ';
            screen_fill(0, 1, col, (int)Columns[0], c, c, attr_fill);

            /* Put an "X" for closing the current tab if there are several. */
            if (first_tabpage.tp_next != null)
            {
                screen_putchar('X', 0, (int)Columns[0] - 1, attr_nosel);
                tabPageIdxs[(int)Columns[0] - 1] = -999;
            }
        }

        /* Reset the flag here again, in case evaluating 'tabline' causes it to be set. */
        redraw_tabline = false;
    }

    /*
     * Get buffer name for "buf" into nameBuff[].
     * Takes care of special buffer names and translates special characters.
     */
    /*private*/ static void get_trans_bufname(buffer_C buf)
    {
        vim_strncpy(nameBuff, buf_spname(buf, false), MAXPATHL - 1);
        trans_characters(nameBuff, MAXPATHL);
    }

    /*
     * Get the character to use in a status line.  Get its attributes in "*attr".
     */
    /*private*/ static int fillchar_status(int[] attr, boolean is_curwin)
    {
        int fill;
        if (is_curwin)
        {
            attr[0] = hl_attr(HLF_S);
            fill = fill_stl[0];
        }
        else
        {
            attr[0] = hl_attr(HLF_SNC);
            fill = fill_stlnc[0];
        }

        /* Use fill when there is highlighting, and highlighting of current window differs,
         * or the fillchars differ, or this is not the current window. */
        if (attr[0] != 0 && ((hl_attr(HLF_S) != hl_attr(HLF_SNC) || !is_curwin || firstwin == lastwin)
                                                                        || fill_stl[0] != fill_stlnc[0]))
            return fill;
        if (is_curwin)
            return '^';

        return '=';
    }

    /*
     * Get the character to use in a separator between vertically split windows.
     * Get its attributes in "*attr".
     */
    /*private*/ static int fillchar_vsep(int[] attr)
    {
        attr[0] = hl_attr(HLF_C);
        if (attr[0] == 0 && fill_vert[0] == ' ')
            return '|';
        else
            return fill_vert[0];
    }

    /*
     * Return true if redrawing should currently be done.
     */
    /*private*/ static boolean redrawing()
    {
        return (redrawingDisabled == 0 && !(p_lz[0] && char_avail() && !keyTyped && !do_redraw));
    }

    /*
     * Return true if printing messages should currently be done.
     */
    /*private*/ static boolean messaging()
    {
        return !(p_lz[0] && char_avail() && !keyTyped);
    }

    /*
     * Show current status info in ruler and various other places.
     * If always is false, only show ruler if position has changed.
     */
    /*private*/ static void showruler(boolean always)
    {
        if (!always && !redrawing())
            return;

        if ((p_stl[0].at(0) != NUL || curwin.w_onebuf_opt.wo_stl[0].at(0) != NUL) && curwin.w_status_height != 0)
            redraw_custom_statusline(curwin);
        else
            win_redr_ruler(curwin, always);

        /* Redraw the tab pages line if needed. */
        if (redraw_tabline)
            draw_tabline();
    }

    /*private*/ static void win_redr_ruler(window_C wp, boolean always)
    {
        /* If 'ruler' off or redrawing disabled, don't do anything. */
        if (!p_ru[0])
            return;

        /*
         * Check if cursor.lnum is valid, since win_redr_ruler() may be called
         * after deleting lines, before cursor.lnum is corrected.
         */
        if (wp.w_buffer.b_ml.ml_line_count < wp.w_cursor.lnum)
            return;

        if (p_ruf[0].at(0) != NUL)
        {
            boolean save_called_emsg = called_emsg;

            called_emsg = false;
            win_redr_custom(wp, true);
            if (called_emsg)
                set_string_option_direct(u8("rulerformat"), -1, u8(""), OPT_FREE, SID_ERROR);
            called_emsg |= save_called_emsg;
            return;
        }

        /*
         * Check if not in Insert mode and the line is empty (will show "0-1").
         */
        boolean empty_line = false;
        if ((State & INSERT) == 0 && ml_get_buf(wp.w_buffer, wp.w_cursor.lnum, false).at(0) == NUL)
            empty_line = true;

        /*
         * Only draw the ruler when something changed.
         */
        validate_virtcol_win(wp);
        if (redraw_cmdline
                || always
                || wp.w_cursor.lnum != wp.w_ru_cursor.lnum
                || wp.w_cursor.col != wp.w_ru_cursor.col
                || wp.w_virtcol != wp.w_ru_virtcol
                || wp.w_cursor.coladd != wp.w_ru_cursor.coladd
                || wp.w_topline != wp.w_ru_topline
                || wp.w_buffer.b_ml.ml_line_count != wp.w_ru_line_count
                || empty_line != wp.w_ru_empty)
        {
            cursor_off();

            int row, fillchar, off, width;
            int[] attr = new int[1];
            if (wp.w_status_height != 0)
            {
                row = wp.w_winrow + wp.w_height;
                fillchar = fillchar_status(attr, wp == curwin);
                off = wp.w_wincol;
                width = wp.w_width;
            }
            else
            {
                row = (int)Rows[0] - 1;
                fillchar = ' ';
                attr[0] = 0;
                off = 0;
                width = (int)Columns[0];
            }

            /* In list mode virtcol needs to be recomputed. */
            int[] virtcol = { wp.w_virtcol };
            if (wp.w_onebuf_opt.wo_list[0] && lcs_tab1[0] == NUL)
            {
                wp.w_onebuf_opt.wo_list[0] = false;
                getvvcol(wp, wp.w_cursor, null, virtcol, null);
                wp.w_onebuf_opt.wo_list[0] = true;
            }

            final int RULER_BUF_LEN = 70;
            Bytes buffer = new Bytes(RULER_BUF_LEN);

            /*
             * Some sprintfs return the length, some return a pointer.
             * To avoid portability problems we use strlen() here.
             */
            vim_snprintf(buffer, RULER_BUF_LEN, u8("%ld,"),
                    ((wp.w_buffer.b_ml.ml_flags & ML_EMPTY) != 0) ? 0L : wp.w_cursor.lnum);
            int len = strlen(buffer);
            col_print(buffer.plus(len), RULER_BUF_LEN - len, (empty_line) ? 0 : wp.w_cursor.col + 1, virtcol[0] + 1);

            /*
             * Add a "50%" if there is room for it.
             * On the last line, don't print in the last column
             * (scrolls the screen up on some terminals).
             */
            int ii = strlen(buffer);
            get_rel_pos(wp, buffer.plus(ii + 1), RULER_BUF_LEN - ii - 1);
            int oo = ii + mb_string2cells(buffer.plus(ii + 1), -1);
            if (wp.w_status_height == 0)    /* can't use last char of screen */
                oo++;
            int this_ru_col = ru_col - ((int)Columns[0] - width);
            if (this_ru_col < 0)
                this_ru_col = 0;
            /* Never use more than half the window/screen width,
             * leave the other half for the filename. */
            if (this_ru_col < (width + 1) / 2)
                this_ru_col = (width + 1) / 2;
            if (this_ru_col + oo < width)
            {
                /* need at least 3 chars left for get_rel_pos() + NUL */
                while (this_ru_col + oo < width && ii + 4 < RULER_BUF_LEN)
                {
                    ii += utf_char2bytes(fillchar, buffer.plus(ii));
                    oo++;
                }
                get_rel_pos(wp, buffer.plus(ii), RULER_BUF_LEN - ii);
            }

            /* Truncate at window boundary. */
            int ooo = 0;
            for (int i = 0; buffer.at(i) != NUL; i += us_ptr2len_cc(buffer.plus(i)))
            {
                ooo += us_ptr2cells(buffer.plus(i));
                if (width < this_ru_col + ooo)
                {
                    buffer.be(i, NUL);
                    break;
                }
            }

            screen_puts(buffer, row, this_ru_col + off, attr[0]);
            boolean iii = redraw_cmdline;
            screen_fill(row, row + 1,
                        this_ru_col + off + strlen(buffer), off + width,
                        fillchar, fillchar, attr[0]);
            /* don't redraw the cmdline because of showing the ruler */
            redraw_cmdline = iii;
            COPY_pos(wp.w_ru_cursor, wp.w_cursor);
            wp.w_ru_virtcol = wp.w_virtcol;
            wp.w_ru_empty = empty_line;
            wp.w_ru_topline = wp.w_topline;
            wp.w_ru_line_count = wp.w_buffer.b_ml.ml_line_count;
        }
    }

    /*
     * Return the width of the 'number' and 'relativenumber' column.
     * Caller may need to check if 'number' or 'relativenumber' is set.
     * Otherwise it depends on 'numberwidth' and the line count.
     */
    /*private*/ static int number_width(window_C wp)
    {
        long lnum;
        if (wp.w_onebuf_opt.wo_rnu[0] && !wp.w_onebuf_opt.wo_nu[0])
            /* cursor line shows "0" */
            lnum = wp.w_height;
        else
            /* cursor line shows absolute line number */
            lnum = wp.w_buffer.b_ml.ml_line_count;

        if (lnum == wp.w_nrwidth_line_count && wp.w_nuw_cached == wp.w_onebuf_opt.wo_nuw[0])
            return wp.w_nrwidth_width;

        wp.w_nrwidth_line_count = lnum;

        int n = 0;
        do
        {
            lnum /= 10;
            n++;
        } while (0 < lnum);

        /* 'numberwidth' gives the minimal width plus one */
        if (n < wp.w_onebuf_opt.wo_nuw[0] - 1)
            n = (int)wp.w_onebuf_opt.wo_nuw[0] - 1;

        wp.w_nrwidth_width = n;
        wp.w_nuw_cached = wp.w_onebuf_opt.wo_nuw[0];
        return n;
    }

    /*
     * Return the current cursor column.  This is the actual position on the screen.
     * First column is 0.
     */
    /*private*/ static int screen_screencol()
    {
        return screen_cur_col;
    }

    /*
     * Return the current cursor row.  This is the actual position on the screen.
     * First row is 0.
     */
    /*private*/ static int screen_screenrow()
    {
        return screen_cur_row;
    }
}
