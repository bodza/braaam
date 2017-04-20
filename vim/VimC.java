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
public class VimC
{
    /*private*/ static final int BASENAMELEN = MAXNAMLEN - 5;

    /*private*/ static final int MAXPATHL = 4096;

    /*private*/ static final int NUMBUFLEN = 30;        /* length of a buffer to store a number in ASCII */

    /* ----------------------------------------------------------------------- */

    /*private*/ static int charOrd(int x)
    {
        return (x < 'a') ? x - 'A' : x - 'a';
    }

    /*private*/ static int charOrdLow(int x)
    {
        return x - 'a';
    }

    /*private*/ static int charOrdUp(int x)
    {
        return x - 'A';
    }

    /*private*/ static int rot13(int c, int a)
    {
        return (((c - a) + 13) % 26) + a;
    }

    /*
     * Definitions of various common control characters.
     */
    /*private*/ static final byte BELL      = '\007';
    /*private*/ static final byte BS        = '\010';
    /*private*/ static final byte TAB       = '\011';
    /*private*/ static final byte NL        = '\012';
    /*private*/ static final byte FF        = '\014';
    /*private*/ static final byte CAR       = '\015';       /* CR is used by Mac OS X */
    /*private*/ static final byte ESC       = '\033';
    /*private*/ static final byte DEL       = (byte)0x7f;

    /*private*/ static final byte CSI       = (byte)0x9b;   /* Control Sequence Introducer */
    /*private*/ static final byte DCS       = (byte)0x90;   /* Device Control String */
    /*private*/ static final byte STERM     = (byte)0x9c;   /* String Terminator */

    /*private*/ static final byte POUND     = (byte)0xa3;

    /*private*/ static final Bytes NL_STR   = u8("\012");
    /*private*/ static final Bytes ESC_STR  = u8("\033");
    /*private*/ static final Bytes DEL_STR  = u8("\177");
    /*private*/ static final Bytes CSI_STR  = u8("\233");

    /*private*/ static int ctrl_key(byte c)
    {
        if (c < NUL)
            return char_u(c);

        return asc_toupper(c) ^ 0x40;       /* '?' -> DEL, '@' -> ^@, etc. */
    }

    /*private*/ static final Bytes CTRL_F_STR = u8("\006");
    /*private*/ static final Bytes CTRL_H_STR = u8("\010");
    /*private*/ static final Bytes CTRL_V_STR = u8("\026");

    /*private*/ static final byte
        Ctrl_AT  =  0,          /* @ */
        Ctrl_A   =  1,
        Ctrl_B   =  2,
        Ctrl_C   =  3,
        Ctrl_D   =  4,
        Ctrl_E   =  5,
        Ctrl_F   =  6,
        Ctrl_G   =  7,
        Ctrl_H   =  8,
        Ctrl_I   =  9,
        Ctrl_J   = 10,
        Ctrl_K   = 11,
        Ctrl_L   = 12,
        Ctrl_M   = 13,
        Ctrl_N   = 14,
        Ctrl_O   = 15,
        Ctrl_P   = 16,
        Ctrl_Q   = 17,
        Ctrl_R   = 18,
        Ctrl_S   = 19,
        Ctrl_T   = 20,
        Ctrl_U   = 21,
        Ctrl_V   = 22,
        Ctrl_W   = 23,
        Ctrl_X   = 24,
        Ctrl_Y   = 25,
        Ctrl_Z   = 26,
                          /* CTRL- [ Left Square Bracket == ESC */
        Ctrl_BSL = 28,          /* \ BackSLash */
        Ctrl_RSB = 29,          /* ] Right Square Bracket */
        Ctrl_HAT = 30,          /* ^ */
        Ctrl__   = 31;

    /* ----------------------------------------------------------------------- */

    /*
     * Keycode definitions for special keys.
     *
     * Any special key code sequences are replaced by these codes.
     */

    /*
     * KB_SPECIAL is the first byte of a special key code and is always followed by two bytes.
     * The second byte can have any value.  ASCII is used for normal termcap entries,
     * 0x80 and higher for special keys, see below.
     * The third byte is guaranteed to be between 0x02 and 0x7f.
     */

    /*private*/ static final byte KB_SPECIAL            = (byte)0x80;

    /*
     * Positive characters are "normal" characters.
     * Negative characters are special key codes.  Only characters below -0x200 are used,
     * so that the absolute value can't be mistaken for a single-byte character.
     */
    /*private*/ static boolean is_special(int c)
    {
        return (c < 0);
    }

    /*
     * Characters 0x0100 - 0x01ff have a special meaning for abbreviations.
     * Multi-byte characters also have ABBR_OFF added, thus are above 0x0200.
     */
    /*private*/ static final int ABBR_OFF               = 0x100;

    /*
     * NUL cannot be in the input string, therefore it is replaced by
     *      KB_SPECIAL  KS_ZERO     KE_FILLER
     */
    /*private*/ static final byte KS_ZERO               = (byte)255;

    /*
     * KB_SPECIAL cannot be in the input string, therefore it is replaced by
     *      KB_SPECIAL  KS_SPECIAL  KE_FILLER
     */
    /*private*/ static final byte KS_SPECIAL            = (byte)254;

    /*
     * KS_EXTRA is used for keys that have no termcap name
     *      KB_SPECIAL  KS_EXTRA    KE_xxx
     */
    /*private*/ static final byte KS_EXTRA              = (byte)253;

    /*
     * KS_MODIFIER is used when a modifier is given for a (special) key
     *      KB_SPECIAL  KS_MODIFIER bitmask
     */
    /*private*/ static final byte KS_MODIFIER           = (byte)252;

    /*
     * These are used for the GUI
     *      KB_SPECIAL  KS_xxx      KE_FILLER
     */
    /*private*/ static final byte KS_MOUSE              = (byte)251;
    /*private*/ static final byte KS_VER_SCROLLBAR      = (byte)249;
    /*private*/ static final byte KS_HOR_SCROLLBAR      = (byte)248;

    /*
     * Used for switching Select mode back on after a mapping or menu.
     */
    /*private*/ static final byte KS_SELECT             = (byte)245;
    /*private*/ static final Bytes K_SELECT_STRING      = u8("\200\365X");

    /*
     * Used a termcap entry that produces a normal character.
     */
    /*private*/ static final byte KS_KEY                = (byte)242;

    /*
     * Filler used after KS_SPECIAL and others.
     */
    /*private*/ static final byte KE_FILLER             = 'X';

    /*
     * Translation of three byte code "KB_SPECIAL a b" into int "K_xxx" and back.
     */
    /*private*/ static int TERMCAP2KEY(byte a, byte b)
    {
        return (int)(-(int_u(a) + (int_u(b) << 8)));
    }

    /*private*/ static byte KEY2TERMCAP0(int x)
    {
        return (byte)((-(x)) & 0xff);
    }

    /*private*/ static byte KEY2TERMCAP1(int x)
    {
        return (byte)((int_u(-(x)) >>> 8) & 0xff);
    }

    /*
     * Get second or third byte when translating special key code into three bytes.
     */
    /*private*/ static byte KB_SECOND(int c)
    {
        return (c == char_u(KB_SPECIAL)) ? KS_SPECIAL : (c == NUL) ? KS_ZERO : KEY2TERMCAP0(c);
    }

    /*private*/ static byte KB_THIRD(int c)
    {
        return (c == char_u(KB_SPECIAL) || c == NUL) ? KE_FILLER : KEY2TERMCAP1(c);
    }

    /*
     * Codes for keys that do not have a termcap name.
     *
     * KB_SPECIAL KS_EXTRA KE_xxx
     */
    /*private*/ static final byte
        KE_NAME = 3,        /* name of this terminal entry */

        KE_S_UP = 4,            /* shift-up */
        KE_S_DOWN = 5,          /* shift-down */

        KE_S_F1 = 6,            /* shifted function keys */
        KE_S_F2 = 7,
        KE_S_F3 = 8,
        KE_S_F4 = 9,
        KE_S_F5 = 10,
        KE_S_F6 = 11,
        KE_S_F7 = 12,
        KE_S_F8 = 13,
        KE_S_F9 = 14,
        KE_S_F10 = 15,

        KE_S_F11 = 16,
        KE_S_F12 = 17,
        KE_S_F13 = 18,
        KE_S_F14 = 19,
        KE_S_F15 = 20,
        KE_S_F16 = 21,
        KE_S_F17 = 22,
        KE_S_F18 = 23,
        KE_S_F19 = 24,
        KE_S_F20 = 25,

        KE_S_F21 = 26,
        KE_S_F22 = 27,
        KE_S_F23 = 28,
        KE_S_F24 = 29,
        KE_S_F25 = 30,
        KE_S_F26 = 31,
        KE_S_F27 = 32,
        KE_S_F28 = 33,
        KE_S_F29 = 34,
        KE_S_F30 = 35,

        KE_S_F31 = 36,
        KE_S_F32 = 37,
        KE_S_F33 = 38,
        KE_S_F34 = 39,
        KE_S_F35 = 40,
        KE_S_F36 = 41,
        KE_S_F37 = 42,

        KE_MOUSE = 43,          /* mouse event start */

    /*
     * Symbols for pseudo keys which are translated from the real key symbols above.
     */
        KE_LEFTMOUSE = 44,      /* left mouse button click */
        KE_LEFTDRAG = 45,       /* drag with left mouse button down */
        KE_LEFTRELEASE = 46,    /* left mouse button release */
        KE_MIDDLEMOUSE = 47,    /* middle mouse button click */
        KE_MIDDLEDRAG = 48,     /* drag with middle mouse button down */
        KE_MIDDLERELEASE = 49,  /* middle mouse button release */
        KE_RIGHTMOUSE = 50,     /* right mouse button click */
        KE_RIGHTDRAG = 51,      /* drag with right mouse button down */
        KE_RIGHTRELEASE = 52,   /* right mouse button release */

        KE_IGNORE = 53,         /* ignored mouse drag/release */

        KE_TAB = 54,            /* unshifted TAB key */
        KE_S_TAB_OLD = 55,      /* shifted TAB key (no longer used) */

        KE_XF1 = 56,            /* extra vt100 function keys for xterm */
        KE_XF2 = 57,
        KE_XF3 = 58,
        KE_XF4 = 59,
        KE_XEND = 60,           /* extra (vt100) end key for xterm */
        KE_ZEND = 61,           /* extra (vt100) end key for xterm */
        KE_XHOME = 62,          /* extra (vt100) home key for xterm */
        KE_ZHOME = 63,          /* extra (vt100) home key for xterm */
        KE_XUP = 64,            /* extra vt100 cursor keys for xterm */
        KE_XDOWN = 65,
        KE_XLEFT = 66,
        KE_XRIGHT = 67,

        KE_LEFTMOUSE_NM = 68,   /* non-mappable left mouse button click */
        KE_LEFTRELEASE_NM = 69, /* non-mappable left mouse button release */

        KE_S_XF1 = 70,          /* extra vt100 shifted function keys for xterm */
        KE_S_XF2 = 71,
        KE_S_XF3 = 72,
        KE_S_XF4 = 73,

        /* NOTE: The scroll wheel events are inverted: i.e. UP is the same as moving the
         * actual scroll wheel down, LEFT is the same as moving the scroll wheel right.
         */
        KE_MOUSEDOWN = 74,      /* scroll wheel pseudo-button Down */
        KE_MOUSEUP = 75,        /* scroll wheel pseudo-button Up */
        KE_MOUSELEFT = 76,      /* scroll wheel pseudo-button Left */
        KE_MOUSERIGHT = 77,     /* scroll wheel pseudo-button Right */

        KE_KINS = 78,           /* keypad Insert key */
        KE_KDEL = 79,           /* keypad Delete key */

        KE_CSI = 80,            /* CSI typed directly */
        KE_SNR = 81,            /* <SNR> */
        KE_PLUG = 82,           /* <Plug> */
        KE_CMDWIN = 83,         /* open command-line window from Command-line Mode */

        KE_C_LEFT = 84,         /* control-left */
        KE_C_RIGHT = 85,        /* control-right */
        KE_C_HOME = 86,         /* control-home */
        KE_C_END = 87,          /* control-end */

        KE_X1MOUSE = 88,        /* X1/X2 mouse-buttons */
        KE_X1DRAG = 89,
        KE_X1RELEASE = 90,
        KE_X2MOUSE = 91,
        KE_X2DRAG = 92,
        KE_X2RELEASE = 93,

        KE_DROP = 94,           /* DnD data is available */
        KE_CURSORHOLD = 95,     /* CursorHold event */
        KE_FOCUSGAINED = 96,    /* focus gained */
        KE_FOCUSLOST = 97,      /* focus lost */

        KE_NOP = 98;            /* doesn't do anything */

    /*
     * the three byte codes are replaced with the following int when using vgetc()
     */
    /*private*/ static final int
        K_ZERO          = -22527,   // = TERMCAP2KEY(KS_ZERO, KE_FILLER),

        K_UP            = -30059,   // = TERMCAP2KEY((byte)'k', (byte)'u'),
        K_DOWN          = -25707,   // = TERMCAP2KEY((byte)'k', (byte)'d'),
        K_LEFT          = -27755,   // = TERMCAP2KEY((byte)'k', (byte)'l'),
        K_RIGHT         = -29291,   // = TERMCAP2KEY((byte)'k', (byte)'r'),
        K_S_UP          = -1021,    // = TERMCAP2KEY(KS_EXTRA, KE_S_UP),
        K_S_DOWN        = -1277,    // = TERMCAP2KEY(KS_EXTRA, KE_S_DOWN),
        K_S_LEFT        = -13347,   // = TERMCAP2KEY((byte)'#', (byte)'4'),
        K_C_LEFT        = -21501,   // = TERMCAP2KEY(KS_EXTRA, KE_C_LEFT),
        K_S_RIGHT       = -26917,   // = TERMCAP2KEY((byte)'%', (byte)'i'),
        K_C_RIGHT       = -21757,   // = TERMCAP2KEY(KS_EXTRA, KE_C_RIGHT),
        K_S_HOME        = -12835,   // = TERMCAP2KEY((byte)'#', (byte)'2'),
        K_C_HOME        = -22013,   // = TERMCAP2KEY(KS_EXTRA, KE_C_HOME),
        K_S_END         = -14122,   // = TERMCAP2KEY((byte)'*', (byte)'7'),
        K_C_END         = -22269,   // = TERMCAP2KEY(KS_EXTRA, KE_C_END),
        K_TAB           = -13821,   // = TERMCAP2KEY(KS_EXTRA, KE_TAB),
        K_S_TAB         = -17003,   // = TERMCAP2KEY((byte)'k', (byte)'B'),

    /* extra set of function keys F1-F4, for vt100 compatible xterm */
        K_XF1           = -14333,   // = TERMCAP2KEY(KS_EXTRA, KE_XF1),
        K_XF2           = -14589,   // = TERMCAP2KEY(KS_EXTRA, KE_XF2),
        K_XF3           = -14845,   // = TERMCAP2KEY(KS_EXTRA, KE_XF3),
        K_XF4           = -15101,   // = TERMCAP2KEY(KS_EXTRA, KE_XF4),

    /* extra set of cursor keys for vt100 compatible xterm */
        K_XUP           = -16381,   // = TERMCAP2KEY(KS_EXTRA, KE_XUP),
        K_XDOWN         = -16637,   // = TERMCAP2KEY(KS_EXTRA, KE_XDOWN),
        K_XLEFT         = -16893,   // = TERMCAP2KEY(KS_EXTRA, KE_XLEFT),
        K_XRIGHT        = -17149,   // = TERMCAP2KEY(KS_EXTRA, KE_XRIGHT),

        K_F0            = -12395,   // = TERMCAP2KEY((byte)'k', (byte)'0'),

        K_F1            = -12651,   // = TERMCAP2KEY((byte)'k', (byte)'1'),   /* function keys */
        K_F2            = -12907,   // = TERMCAP2KEY((byte)'k', (byte)'2'),
        K_F3            = -13163,   // = TERMCAP2KEY((byte)'k', (byte)'3'),
        K_F4            = -13419,   // = TERMCAP2KEY((byte)'k', (byte)'4'),
        K_F5            = -13675,   // = TERMCAP2KEY((byte)'k', (byte)'5'),
        K_F6            = -13931,   // = TERMCAP2KEY((byte)'k', (byte)'6'),
        K_F7            = -14187,   // = TERMCAP2KEY((byte)'k', (byte)'7'),
        K_F8            = -14443,   // = TERMCAP2KEY((byte)'k', (byte)'8'),
        K_F9            = -14699,   // = TERMCAP2KEY((byte)'k', (byte)'9'),
        K_F10           = -15211,   // = TERMCAP2KEY((byte)'k', (byte)';'),

        K_F11           = -12614,   // = TERMCAP2KEY((byte)'F', (byte)'1'),
        K_F12           = -12870,   // = TERMCAP2KEY((byte)'F', (byte)'2'),
        K_F13           = -13126,   // = TERMCAP2KEY((byte)'F', (byte)'3'),
        K_F14           = -13382,   // = TERMCAP2KEY((byte)'F', (byte)'4'),
        K_F15           = -13638,   // = TERMCAP2KEY((byte)'F', (byte)'5'),
        K_F16           = -13894,   // = TERMCAP2KEY((byte)'F', (byte)'6'),
        K_F17           = -14150,   // = TERMCAP2KEY((byte)'F', (byte)'7'),
        K_F18           = -14406,   // = TERMCAP2KEY((byte)'F', (byte)'8'),
        K_F19           = -14662,   // = TERMCAP2KEY((byte)'F', (byte)'9'),
        K_F20           = -16710,   // = TERMCAP2KEY((byte)'F', (byte)'A'),

        K_F21           = -16966,   // = TERMCAP2KEY((byte)'F', (byte)'B'),
        K_F22           = -17222,   // = TERMCAP2KEY((byte)'F', (byte)'C'),
        K_F23           = -17478,   // = TERMCAP2KEY((byte)'F', (byte)'D'),
        K_F24           = -17734,   // = TERMCAP2KEY((byte)'F', (byte)'E'),
        K_F25           = -17990,   // = TERMCAP2KEY((byte)'F', (byte)'F'),
        K_F26           = -18246,   // = TERMCAP2KEY((byte)'F', (byte)'G'),
        K_F27           = -18502,   // = TERMCAP2KEY((byte)'F', (byte)'H'),
        K_F28           = -18758,   // = TERMCAP2KEY((byte)'F', (byte)'I'),
        K_F29           = -19014,   // = TERMCAP2KEY((byte)'F', (byte)'J'),
        K_F30           = -19270,   // = TERMCAP2KEY((byte)'F', (byte)'K'),

        K_F31           = -19526,   // = TERMCAP2KEY((byte)'F', (byte)'L'),
        K_F32           = -19782,   // = TERMCAP2KEY((byte)'F', (byte)'M'),
        K_F33           = -20038,   // = TERMCAP2KEY((byte)'F', (byte)'N'),
        K_F34           = -20294,   // = TERMCAP2KEY((byte)'F', (byte)'O'),
        K_F35           = -20550,   // = TERMCAP2KEY((byte)'F', (byte)'P'),
        K_F36           = -20806,   // = TERMCAP2KEY((byte)'F', (byte)'Q'),
        K_F37           = -21062,   // = TERMCAP2KEY((byte)'F', (byte)'R'),

    /* extra set of shifted function keys F1-F4, for vt100 compatible xterm */
        K_S_XF1         = -17917,   // = TERMCAP2KEY(KS_EXTRA, KE_S_XF1),
        K_S_XF2         = -18173,   // = TERMCAP2KEY(KS_EXTRA, KE_S_XF2),
        K_S_XF3         = -18429,   // = TERMCAP2KEY(KS_EXTRA, KE_S_XF3),
        K_S_XF4         = -18685,   // = TERMCAP2KEY(KS_EXTRA, KE_S_XF4),

        K_S_F1          = -1533,    // = TERMCAP2KEY(KS_EXTRA, KE_S_F1),  /* shifted func. keys */
        K_S_F2          = -1789,    // = TERMCAP2KEY(KS_EXTRA, KE_S_F2),
        K_S_F3          = -2045,    // = TERMCAP2KEY(KS_EXTRA, KE_S_F3),
        K_S_F4          = -2301,    // = TERMCAP2KEY(KS_EXTRA, KE_S_F4),
        K_S_F5          = -2557,    // = TERMCAP2KEY(KS_EXTRA, KE_S_F5),
        K_S_F6          = -2813,    // = TERMCAP2KEY(KS_EXTRA, KE_S_F6),
        K_S_F7          = -3069,    // = TERMCAP2KEY(KS_EXTRA, KE_S_F7),
        K_S_F8          = -3325,    // = TERMCAP2KEY(KS_EXTRA, KE_S_F8),
        K_S_F9          = -3581,    // = TERMCAP2KEY(KS_EXTRA, KE_S_F9),
        K_S_F10         = -3837,    // = TERMCAP2KEY(KS_EXTRA, KE_S_F10),

        K_S_F11         = -4093,    // = TERMCAP2KEY(KS_EXTRA, KE_S_F11),
        K_S_F12         = -4349,    // = TERMCAP2KEY(KS_EXTRA, KE_S_F12),
    /* K_S_F13 to K_S_F37 are currently not used */

        K_HELP          = -12581,   // = TERMCAP2KEY((byte)'%', (byte)'1'),
        K_UNDO          = -14374,   // = TERMCAP2KEY((byte)'&', (byte)'8'),

        K_BS            = -25195,   // = TERMCAP2KEY((byte)'k', (byte)'b'),

        K_INS           = -18795,   // = TERMCAP2KEY((byte)'k', (byte)'I'),
        K_KINS          = -19965,   // = TERMCAP2KEY(KS_EXTRA, KE_KINS),
        K_DEL           = -17515,   // = TERMCAP2KEY((byte)'k', (byte)'D'),
        K_KDEL          = -20221,   // = TERMCAP2KEY(KS_EXTRA, KE_KDEL),
        K_HOME          = -26731,   // = TERMCAP2KEY((byte)'k', (byte)'h'),
        K_KHOME         = -12619,   // = TERMCAP2KEY((byte)'K', (byte)'1'),   /* keypad home (upper left) */
        K_XHOME         = -15869,   // = TERMCAP2KEY(KS_EXTRA, KE_XHOME),
        K_ZHOME         = -16125,   // = TERMCAP2KEY(KS_EXTRA, KE_ZHOME),
        K_END           = -14144,   // = TERMCAP2KEY((byte)'@', (byte)'7'),
        K_KEND          = -13387,   // = TERMCAP2KEY((byte)'K', (byte)'4'),   /* keypad end (lower left) */
        K_XEND          = -15357,   // = TERMCAP2KEY(KS_EXTRA, KE_XEND),
        K_ZEND          = -15613,   // = TERMCAP2KEY(KS_EXTRA, KE_ZEND),
        K_PAGEUP        = -20587,   // = TERMCAP2KEY((byte)'k', (byte)'P'),
        K_PAGEDOWN      = -20075,   // = TERMCAP2KEY((byte)'k', (byte)'N'),
        K_KPAGEUP       = -13131,   // = TERMCAP2KEY((byte)'K', (byte)'3'),   /* keypad pageup (upper R.) */
        K_KPAGEDOWN     = -13643,   // = TERMCAP2KEY((byte)'K', (byte)'5'),   /* keypad pagedown (lower R.) */

        K_KPLUS         = -13899,   // = TERMCAP2KEY((byte)'K', (byte)'6'),   /* keypad plus */
        K_KMINUS        = -14155,   // = TERMCAP2KEY((byte)'K', (byte)'7'),   /* keypad minus */
        K_KDIVIDE       = -14411,   // = TERMCAP2KEY((byte)'K', (byte)'8'),   /* keypad / */
        K_KMULTIPLY     = -14667,   // = TERMCAP2KEY((byte)'K', (byte)'9'),   /* keypad * */
        K_KENTER        = -16715,   // = TERMCAP2KEY((byte)'K', (byte)'A'),   /* keypad Enter */
        K_KPOINT        = -16971,   // = TERMCAP2KEY((byte)'K', (byte)'B'),   /* keypad . or , */

        K_K0            = -17227,   // = TERMCAP2KEY((byte)'K', (byte)'C'),   /* keypad 0 */
        K_K1            = -17483,   // = TERMCAP2KEY((byte)'K', (byte)'D'),   /* keypad 1 */
        K_K2            = -17739,   // = TERMCAP2KEY((byte)'K', (byte)'E'),   /* keypad 2 */
        K_K3            = -17995,   // = TERMCAP2KEY((byte)'K', (byte)'F'),   /* keypad 3 */
        K_K4            = -18251,   // = TERMCAP2KEY((byte)'K', (byte)'G'),   /* keypad 4 */
        K_K5            = -18507,   // = TERMCAP2KEY((byte)'K', (byte)'H'),   /* keypad 5 */
        K_K6            = -18763,   // = TERMCAP2KEY((byte)'K', (byte)'I'),   /* keypad 6 */
        K_K7            = -19019,   // = TERMCAP2KEY((byte)'K', (byte)'J'),   /* keypad 7 */
        K_K8            = -19275,   // = TERMCAP2KEY((byte)'K', (byte)'K'),   /* keypad 8 */
        K_K9            = -19531,   // = TERMCAP2KEY((byte)'K', (byte)'L'),   /* keypad 9 */

        K_MOUSE         = -22523,   // = TERMCAP2KEY(KS_MOUSE, KE_FILLER),
        K_VER_SCROLLBAR = -22521,   // = TERMCAP2KEY(KS_VER_SCROLLBAR, KE_FILLER),
        K_HOR_SCROLLBAR = -22520,   // = TERMCAP2KEY(KS_HOR_SCROLLBAR, KE_FILLER),

        K_SELECT        = -22517,   // = TERMCAP2KEY(KS_SELECT, KE_FILLER),

    /*
     * Symbols for pseudo keys which are translated from the real key symbols above.
     */
        K_LEFTMOUSE     = -11261,   // = TERMCAP2KEY(KS_EXTRA, KE_LEFTMOUSE),
        K_LEFTMOUSE_NM  = -17405,   // = TERMCAP2KEY(KS_EXTRA, KE_LEFTMOUSE_NM),
        K_LEFTDRAG      = -11517,   // = TERMCAP2KEY(KS_EXTRA, KE_LEFTDRAG),
        K_LEFTRELEASE   = -11773,   // = TERMCAP2KEY(KS_EXTRA, KE_LEFTRELEASE),
        K_LEFTRELEASE_NM = -17661,  // = TERMCAP2KEY(KS_EXTRA, KE_LEFTRELEASE_NM),
        K_MIDDLEMOUSE   = -12029,   // = TERMCAP2KEY(KS_EXTRA, KE_MIDDLEMOUSE),
        K_MIDDLEDRAG    = -12285,   // = TERMCAP2KEY(KS_EXTRA, KE_MIDDLEDRAG),
        K_MIDDLERELEASE = -12541,   // = TERMCAP2KEY(KS_EXTRA, KE_MIDDLERELEASE),
        K_RIGHTMOUSE    = -12797,   // = TERMCAP2KEY(KS_EXTRA, KE_RIGHTMOUSE),
        K_RIGHTDRAG     = -13053,   // = TERMCAP2KEY(KS_EXTRA, KE_RIGHTDRAG),
        K_RIGHTRELEASE  = -13309,   // = TERMCAP2KEY(KS_EXTRA, KE_RIGHTRELEASE),
        K_X1MOUSE       = -22525,   // = TERMCAP2KEY(KS_EXTRA, KE_X1MOUSE),
        K_X1DRAG        = -22781,   // = TERMCAP2KEY(KS_EXTRA, KE_X1DRAG),
        K_X1RELEASE     = -23037,   // = TERMCAP2KEY(KS_EXTRA, KE_X1RELEASE),
        K_X2MOUSE       = -23293,   // = TERMCAP2KEY(KS_EXTRA, KE_X2MOUSE),
        K_X2DRAG        = -23549,   // = TERMCAP2KEY(KS_EXTRA, KE_X2DRAG),
        K_X2RELEASE     = -23805,   // = TERMCAP2KEY(KS_EXTRA, KE_X2RELEASE),

        K_IGNORE        = -13565,   // = TERMCAP2KEY(KS_EXTRA, KE_IGNORE),
        K_NOP           = -25085,   // = TERMCAP2KEY(KS_EXTRA, KE_NOP),

        K_MOUSEDOWN     = -18941,   // = TERMCAP2KEY(KS_EXTRA, KE_MOUSEDOWN),
        K_MOUSEUP       = -19197,   // = TERMCAP2KEY(KS_EXTRA, KE_MOUSEUP),
        K_MOUSELEFT     = -19453,   // = TERMCAP2KEY(KS_EXTRA, KE_MOUSELEFT),
        K_MOUSERIGHT    = -19709,   // = TERMCAP2KEY(KS_EXTRA, KE_MOUSERIGHT),

        K_CSI           = -20477,   // = TERMCAP2KEY(KS_EXTRA, KE_CSI),
        K_SNR           = -20733,   // = TERMCAP2KEY(KS_EXTRA, KE_SNR),
        K_PLUG          = -20989,   // = TERMCAP2KEY(KS_EXTRA, KE_PLUG),
        K_CMDWIN        = -21245,   // = TERMCAP2KEY(KS_EXTRA, KE_CMDWIN),

        K_DROP          = -24061,   // = TERMCAP2KEY(KS_EXTRA, KE_DROP),
        K_FOCUSGAINED   = -24573,   // = TERMCAP2KEY(KS_EXTRA, KE_FOCUSGAINED),
        K_FOCUSLOST     = -24829,   // = TERMCAP2KEY(KS_EXTRA, KE_FOCUSLOST),

        K_CURSORHOLD    = -24317;   // = TERMCAP2KEY(KS_EXTRA, KE_CURSORHOLD);

    /* Bits for modifier mask. */
    /* 0x01 cannot be used, because the modifier must be 0x02 or higher */
    /*private*/ static final int MOD_MASK_SHIFT      = 0x02;
    /*private*/ static final int MOD_MASK_CTRL       = 0x04;
    /*private*/ static final int MOD_MASK_ALT        = 0x08;        /* aka META */
    /*private*/ static final int MOD_MASK_META       = 0x10;        /* META when it's different from ALT */
    /*private*/ static final int MOD_MASK_2CLICK     = 0x20;        /* use MOD_MASK_MULTI_CLICK */
    /*private*/ static final int MOD_MASK_3CLICK     = 0x40;        /* use MOD_MASK_MULTI_CLICK */
    /*private*/ static final int MOD_MASK_4CLICK     = 0x60;        /* use MOD_MASK_MULTI_CLICK */

    /*private*/ static final int MOD_MASK_MULTI_CLICK = (MOD_MASK_2CLICK|MOD_MASK_3CLICK|MOD_MASK_4CLICK);

    /*
     * The length of the longest special key name, including modifiers.
     * Current longest is <M-C-S-T-4-MiddleRelease> (length includes '<' and '>').
     */
    /*private*/ static final int MAX_KEY_NAME_LEN    = 25;

    /* Maximum length of a special key event as tokens.  This includes modifiers.
     * The longest event is something like <M-C-S-T-4-LeftDrag> which would be the
     * following string of tokens:
     *
     * <KB_SPECIAL> <KS_MODIFIER> bitmask <KB_SPECIAL> <KS_EXTRA> <KE_LEFTDRAG>.
     *
     * This is a total of 6 tokens, and is currently the longest one possible.
     */
    /*private*/ static final int MAX_KEY_CODE_LEN    = 6;

    /*
     * Get single int code from second byte after KB_SPECIAL.
     */
    /*private*/ static int toSpecial(byte a, byte b)
    {
        return (a == KS_SPECIAL) ? char_u(KB_SPECIAL) : (a == KS_ZERO) ? K_ZERO : TERMCAP2KEY(a, b);
    }

    /* ----------------------------------------------------------------------- */

    /*
     * This file contains the defines for the machine dependent escape sequences
     * that the editor needs to perform various operations.  All of the sequences
     * here are optional, except "cm" (cursor motion).
     */

    /*
     * Index of the termcap codes in the 'term_strings' array.
     */
    /*private*/ static final int
        KS_NAME = 0,    /* name of this terminal entry */
        KS_CE   = 1,    /* clear to end of line */
        KS_AL   = 2,    /* add new blank line */
        KS_CAL  = 3,    /* add number of blank lines */
        KS_DL   = 4,    /* delete line */
        KS_CDL  = 5,    /* delete number of lines */
        KS_CS   = 6,    /* scroll region */
        KS_CL   = 7,    /* clear screen */
        KS_CD   = 8,    /* clear to end of display */
        KS_UT   = 9,    /* clearing uses current background color */
        KS_DA   = 10,   /* text may be scrolled down from up */
        KS_DB   = 11,   /* text may be scrolled up from down */
        KS_VI   = 12,   /* cursor invisible */
        KS_VE   = 13,   /* cursor visible */
        KS_VS   = 14,   /* cursor very visible */
        KS_ME   = 15,   /* normal mode */
        KS_MR   = 16,   /* reverse mode */
        KS_MD   = 17,   /* bold mode */
        KS_SE   = 18,   /* normal mode */
        KS_SO   = 19,   /* standout mode */
        KS_CZH  = 20,   /* italic mode start */
        KS_CZR  = 21,   /* italic mode end */
        KS_UE   = 22,   /* exit underscore (underline) mode */
        KS_US   = 23,   /* underscore (underline) mode */
        KS_UCE  = 24,   /* exit undercurl mode */
        KS_UCS  = 25,   /* undercurl mode */
        KS_MS   = 26,   /* save to move cur in reverse mode */
        KS_CM   = 27,   /* cursor motion */
        KS_SR   = 28,   /* scroll reverse (backward) */
        KS_CRI  = 29,   /* cursor number of chars right */
        KS_VB   = 30,   /* visual bell */
        KS_KS   = 31,   /* put term in "keypad transmit" mode */
        KS_KE   = 32,   /* out of "keypad transmit" mode */
        KS_TI   = 33,   /* put terminal in termcap mode */
        KS_TE   = 34,   /* out of termcap mode */
        KS_BC   = 35,   /* backspace character (cursor left) */
        KS_CCS  = 36,   /* cur is relative to scroll region */
        KS_CCO  = 37,   /* number of colors */
        KS_CSF  = 38,   /* set foreground color */
        KS_CSB  = 39,   /* set background color */
        KS_XS   = 40,   /* standout not erased by overwriting (hpterm) */
        KS_XN   = 41,   /* newline glitch */
        KS_MB   = 42,   /* blink mode */
        KS_CAF  = 43,   /* set foreground color (ANSI) */
        KS_CAB  = 44,   /* set background color (ANSI) */
        KS_LE   = 45,   /* cursor left (mostly backspace) */
        KS_ND   = 46,   /* cursor right */
        KS_CIS  = 47,   /* set icon text start */
        KS_CIE  = 48,   /* set icon text end */
        KS_TS   = 49,   /* set window title start (to status line) */
        KS_FS   = 50,   /* set window title end (from status line) */
        KS_CWP  = 51,   /* set window position in pixels */
        KS_CWS  = 52,   /* set window size in characters */
        KS_CRV  = 53,   /* request version string */
        KS_CSI  = 54,   /* start insert mode (bar cursor) */
        KS_CEI  = 55,   /* end insert mode (block cursor) */
        KS_CSR  = 56,   /* start replace mode (underline cursor) */
        KS_CSV  = 57,   /* scroll region vertical */
        KS_OP   = 58,   /* original color pair */
        KS_U7   = 59;   /* request cursor position */

    /*private*/ static final int KS_LAST = KS_U7;

    /*
     * The terminal capabilities are stored in this array.
     * IMPORTANT: When making changes, note the following:
     * - there should be an entry for each code in the builtin termcaps
     * - there should be an option for each code in option.c
     * - there should be code in term.c to obtain the value from the termcap
     */

    /*
     * 'term_strings' contains currently used terminal output strings.
     * It is initialized with the default values by parse_builtin_tcap().
     * The values can be changed by setting the option with the same name.
     */
    /*private*/ static Bytes[][] term_strings = new Bytes[KS_LAST + 1][1];

    /*
     * strings used for terminal
     */
    /*private*/ static Bytes[/*1*/]
        T_NAME = term_strings[KS_NAME],         /* terminal name */
        T_CE   = term_strings[KS_CE],           /* clear to end of line */
        T_AL   = term_strings[KS_AL],           /* add new blank line */
        T_CAL  = term_strings[KS_CAL],          /* add number of blank lines */
        T_DL   = term_strings[KS_DL],           /* delete line */
        T_CDL  = term_strings[KS_CDL],          /* delete number of lines */
        T_CS   = term_strings[KS_CS],           /* scroll region */
        T_CSV  = term_strings[KS_CSV],          /* scroll region vertical */
        T_CL   = term_strings[KS_CL],           /* clear screen */
        T_CD   = term_strings[KS_CD],           /* clear to end of display */
        T_UT   = term_strings[KS_UT],           /* clearing uses background color */
        T_DA   = term_strings[KS_DA],           /* text may be scrolled down from up */
        T_DB   = term_strings[KS_DB],           /* text may be scrolled up from down */
        T_VI   = term_strings[KS_VI],           /* cursor invisible */
        T_VE   = term_strings[KS_VE],           /* cursor visible */
        T_VS   = term_strings[KS_VS],           /* cursor very visible */
        T_ME   = term_strings[KS_ME],           /* normal mode */
        T_MR   = term_strings[KS_MR],           /* reverse mode */
        T_MD   = term_strings[KS_MD],           /* bold mode */
        T_SE   = term_strings[KS_SE],           /* normal mode */
        T_SO   = term_strings[KS_SO],           /* standout mode */
        T_CZH  = term_strings[KS_CZH],          /* italic mode start */
        T_CZR  = term_strings[KS_CZR],          /* italic mode end */
        T_UE   = term_strings[KS_UE],           /* exit underscore (underline) mode */
        T_US   = term_strings[KS_US],           /* underscore (underline) mode */
        T_UCE  = term_strings[KS_UCE],          /* exit undercurl mode */
        T_UCS  = term_strings[KS_UCS],          /* undercurl mode */
        T_MS   = term_strings[KS_MS],           /* save to move cur in reverse mode */
        T_CM   = term_strings[KS_CM],           /* cursor motion */
        T_SR   = term_strings[KS_SR],           /* scroll reverse (backward) */
        T_CRI  = term_strings[KS_CRI],          /* cursor number of chars right */
        T_VB   = term_strings[KS_VB],           /* visual bell */
        T_KS   = term_strings[KS_KS],           /* put term in "keypad transmit" mode */
        T_KE   = term_strings[KS_KE],           /* out of "keypad transmit" mode */
        T_TI   = term_strings[KS_TI],           /* put terminal in termcap mode */
        T_TE   = term_strings[KS_TE],           /* out of termcap mode */
        T_BC   = term_strings[KS_BC],           /* backspace character */
        T_CCS  = term_strings[KS_CCS],          /* cur is relative to scroll region */
        T_CCO  = term_strings[KS_CCO],          /* number of colors */
        T_CSF  = term_strings[KS_CSF],          /* set foreground color */
        T_CSB  = term_strings[KS_CSB],          /* set background color */
        T_XS   = term_strings[KS_XS],           /* standout not erased by overwriting */
        T_XN   = term_strings[KS_XN],           /* newline glitch */
        T_MB   = term_strings[KS_MB],           /* blink mode */
        T_CAF  = term_strings[KS_CAF],          /* set foreground color (ANSI) */
        T_CAB  = term_strings[KS_CAB],          /* set background color (ANSI) */
        T_LE   = term_strings[KS_LE],           /* cursor left */
        T_ND   = term_strings[KS_ND],           /* cursor right */
        T_CIS  = term_strings[KS_CIS],          /* set icon text start */
        T_CIE  = term_strings[KS_CIE],          /* set icon text end */
        T_TS   = term_strings[KS_TS],           /* set window title start */
        T_FS   = term_strings[KS_FS],           /* set window title end */
        T_CWP  = term_strings[KS_CWP],          /* window position */
        T_CWS  = term_strings[KS_CWS],          /* window size */
        T_CSI  = term_strings[KS_CSI],          /* start insert mode */
        T_CEI  = term_strings[KS_CEI],          /* end insert mode */
        T_CSR  = term_strings[KS_CSR],          /* start replace mode */
        T_CRV  = term_strings[KS_CRV],          /* request version string */
        T_OP   = term_strings[KS_OP],           /* original color pair */
        T_U7   = term_strings[KS_U7];           /* request cursor position */

    /*private*/ static final int TMODE_COOK  = 0;   /* terminal mode for external cmds and Ex mode */
    /*private*/ static final int TMODE_SLEEP = 1;   /* terminal mode for sleeping (cooked but no echo) */
    /*private*/ static final int TMODE_RAW   = 2;   /* terminal mode for Normal and Insert mode */

    /* ----------------------------------------------------------------------- */

    /*
     * flags for update_screen()
     * The higher the value, the higher the priority
     */
    /*private*/ static final int VALID        = 10; /* buffer not changed, or changes marked with b_mod_* */
    /*private*/ static final int INVERTED     = 20; /* redisplay inverted part that changed */
    /*private*/ static final int INVERTED_ALL = 25; /* redisplay whole inverted part */
    /*private*/ static final int REDRAW_TOP   = 30; /* display first w_upd_rows screen lines */
    /*private*/ static final int SOME_VALID   = 35; /* like NOT_VALID but may scroll */
    /*private*/ static final int NOT_VALID    = 40; /* buffer needs complete redraw */
    /*private*/ static final int CLEAR        = 50; /* screen messed up, clear it */

    /*
     * Flags for w_valid.
     * These are set when something in a window structure becomes invalid, except when the cursor is moved.
     * Call check_cursor_moved() before testing one of the flags.
     * These are reset when that thing has been updated and is valid again.
     *
     * Every function that invalidates one of these must call one of the invalidate_* functions.
     *
     * w_valid is supposed to be encapsulated: use the functions that set or reset the flags, instead.
     *
     * VALID_BOTLINE    VALID_BOTLINE_AP
     *     on               on              w_botline valid
     *     off              on              w_botline approximated
     *     off              off             w_botline not valid
     *     on               off             not possible
     */
    /*private*/ static final int VALID_WROW         = 0x01;     /* w_wrow (window row) is valid */
    /*private*/ static final int VALID_WCOL         = 0x02;     /* w_wcol (window col) is valid */
    /*private*/ static final int VALID_VIRTCOL      = 0x04;     /* w_virtcol (file col) is valid */
    /*private*/ static final int VALID_CHEIGHT      = 0x08;     /* w_cline_height and w_cline_folded valid */
    /*private*/ static final int VALID_CROW         = 0x10;     /* w_cline_row is valid */
    /*private*/ static final int VALID_BOTLINE      = 0x20;     /* w_botine and w_empty_rows are valid */
    /*private*/ static final int VALID_BOTLINE_AP   = 0x40;     /* w_botine is approximated */
    /*private*/ static final int VALID_TOPLINE      = 0x80;     /* w_topline is valid (for cursor position) */

    /*
     * Terminal highlighting attribute bits.
     * Attributes above HL_ALL are used for syntax highlighting.
     */
    /*private*/ static final int HL_NORMAL          = 0x00;
    /*private*/ static final int HL_INVERSE         = 0x01;
    /*private*/ static final int HL_BOLD            = 0x02;
    /*private*/ static final int HL_ITALIC          = 0x04;
    /*private*/ static final int HL_UNDERLINE       = 0x08;
    /*private*/ static final int HL_UNDERCURL       = 0x10;
    /*private*/ static final int HL_STANDOUT        = 0x20;
    /*private*/ static final int HL_ALL             = 0x3f;

    /* special attribute addition: put message in history */
    /*private*/ static final int MSG_HIST           = 0x1000;

    /*
     * values for State
     *
     * The lower bits up to 0x20 are used to distinguish normal/visual/op_pending
     * and cmdline/insert+replace mode.  This is used for mapping.
     * If none of these bits are set, no mapping is done.
     * The upper bits are used to distinguish between other states.
     */
    /*private*/ static final int
        NORMAL          = 0x01,             /* normal mode, command expected */
        VISUAL          = 0x02,             /* visual mode - use get_real_state() */
        OP_PENDING      = 0x04,             /* normal mode, operator is pending - use get_real_state() */
        CMDLINE         = 0x08,             /* editing command line */
        INSERT          = 0x10,             /* insert mode */
        LANGMAP         = 0x20,             /* language mapping, can be combined with INSERT and CMDLINE */

        REPLACE_FLAG    = 0x40,             /* replace mode flag */
        REPLACE         = REPLACE_FLAG + INSERT,
        LREPLACE        = REPLACE_FLAG + LANGMAP,
        VREPLACE_FLAG   = 0x80,             /* virtual-replace mode flag */
        VREPLACE        = REPLACE_FLAG + VREPLACE_FLAG + INSERT,

        NORMAL_BUSY     = 0x100 + NORMAL,   /* normal mode, busy with a command */
        HITRETURN       = 0x200 + NORMAL,   /* waiting for return or command */
        ASKMORE         = 0x300,            /* asking if you want --more-- */
        SETWSIZE        = 0x400,            /* window size has changed */
        ABBREV          = 0x500,            /* abbreviation instead of mapping */
        EXTERNCMD       = 0x600,            /* executing an external command */
        SHOWMATCH       = 0x700 + INSERT,   /* show matching paren */
        CONFIRM         = 0x800,            /* ":confirm" prompt */
        SELECTMODE      = 0x1000;           /* select mode, only for mappings */

    /*private*/ static final int MAP_ALL_MODES = 0x3f | SELECTMODE; /* all mode bits used for mapping */

    /* directions */
    /*private*/ static final int FORWARD        = 1;
    /*private*/ static final int BACKWARD       = -1;
    /*private*/ static final int FORWARD_FILE   = 3;
    /*private*/ static final int BACKWARD_FILE  = -3;

    /* flags for b_flags */
    /*private*/ static final int BF_RECOVERED   = 0x01;     /* buffer has been recovered */
    /*private*/ static final int BF_CHECK_RO    = 0x02;     /* need to check readonly when loading file
                                                         * into buffer (set by ":e", may be reset by ":buf" */
    /*private*/ static final int BF_NEVERLOADED = 0x04;     /* file has never been loaded into buffer,
                                                         * many variables still need to be set */
    /*private*/ static final int BF_NOTEDITED   = 0x08;     /* Set when file name is changed after
                                                         * starting to edit, reset when file is written out. */
    /*private*/ static final int BF_NEW         = 0x10;     /* file didn't exist when editing started */
    /*private*/ static final int BF_NEW_W       = 0x20;     /* Warned for BF_NEW and file created */
    /*private*/ static final int BF_READERR     = 0x40;     /* got errors while reading the file */
    /*private*/ static final int BF_DUMMY       = 0x80;     /* dummy buffer, only used internally */

    /* Mask to check for flags that prevent normal writing. */
    /*private*/ static final int BF_WRITE_MASK  = BF_NOTEDITED + BF_NEW + BF_READERR;

    /*
     * values for xp_context when doing command line completion
     */
    /*private*/ static final int
        EXPAND_UNSUCCESSFUL     = -2,
        EXPAND_OK               = -1,
        EXPAND_NOTHING          =  0,
        EXPAND_COMMANDS         =  1,
        EXPAND_SETTINGS         =  4,
        EXPAND_BOOL_SETTINGS    =  5,
        EXPAND_OLD_SETTING      =  7,
        EXPAND_BUFFERS          =  9,
        EXPAND_EVENTS           = 10,
        EXPAND_SYNTAX           = 12,
        EXPAND_HIGHLIGHT        = 13,
        EXPAND_AUGROUP          = 14,
        EXPAND_USER_VARS        = 15,
        EXPAND_MAPPINGS         = 16,
        EXPAND_FUNCTIONS        = 18,
        EXPAND_USER_FUNC        = 19,
        EXPAND_EXPRESSION       = 20,
        EXPAND_USER_COMMANDS    = 22,
        EXPAND_USER_CMD_FLAGS   = 23,
        EXPAND_USER_NARGS       = 24,
        EXPAND_USER_COMPLETE    = 25,
        EXPAND_USER_DEFINED     = 30,
        EXPAND_USER_LIST        = 31,
        EXPAND_HISTORY          = 41,
        EXPAND_SYNTIME          = 43,
        EXPAND_USER_ADDR_TYPE   = 44;

    /* Values for exmode_active (0 is no exmode). */
    /*private*/ static final int EXMODE_NORMAL      = 1;
    /*private*/ static final int EXMODE_VIM         = 2;

    /* Values for nextwild() and expandOne().  See expandOne() for meaning. */
    /*private*/ static final int WILD_FREE          = 1;
    /*private*/ static final int WILD_EXPAND_FREE   = 2;
    /*private*/ static final int WILD_EXPAND_KEEP   = 3;
    /*private*/ static final int WILD_NEXT          = 4;
    /*private*/ static final int WILD_PREV          = 5;
    /*private*/ static final int WILD_ALL           = 6;
    /*private*/ static final int WILD_LONGEST       = 7;
    /*private*/ static final int WILD_ALL_KEEP      = 8;

    /*private*/ static final int WILD_LIST_NOTFOUND = 0x01;
    /*private*/ static final int WILD_USE_NL        = 0x04;
    /*private*/ static final int WILD_NO_BEEP       = 0x08;
    /*private*/ static final int WILD_ADD_SLASH     = 0x10;
    /*private*/ static final int WILD_SILENT        = 0x40;
    /*private*/ static final int WILD_ESCAPE        = 0x80;
    /*private*/ static final int WILD_ICASE         = 0x100;
    /*private*/ static final int WILD_ALLLINKS      = 0x200;

    /* Flags for expand_wildcards(). */
    /*private*/ static final int EW_DIR          = 0x01;    /* include directory names */
    /*private*/ static final int EW_FILE         = 0x02;    /* include file names */
    /*private*/ static final int EW_NOTFOUND     = 0x04;    /* include not found names */
    /*private*/ static final int EW_ADDSLASH     = 0x08;    /* append slash to directory name */
    /*private*/ static final int EW_SILENT       = 0x20;    /* don't print "1 returned" from shell */
    /*private*/ static final int EW_ICASE        = 0x100;   /* ignore case */
    /*private*/ static final int EW_NOERROR      = 0x200;   /* no error for bad regexp */
    /*private*/ static final int EW_NOTWILD      = 0x400;   /* add match with literal name if exists */
    /*private*/ static final int EW_KEEPDOLLAR   = 0x800;   /* do not escape $, $var is expanded */
    /* Note: mostly EW_NOTFOUND and EW_SILENT are mutually exclusive: EW_NOTFOUND
     * is used when executing commands and EW_SILENT for interactive expanding. */
    /*private*/ static final int EW_ALLLINKS     = 0x1000;  /* also links not pointing to existing file */

    /*private*/ static final int SST_MIN_ENTRIES = 150;     /* minimal size for state stack array */
    /*private*/ static final int SST_MAX_ENTRIES = 1000;    /* maximal size for state stack array */
    /*private*/ static final int SST_DIST        = 16;      /* normal distance between entries */

    /*private*/ static final int HL_CONTAINED    = 0x01;    /* not used on toplevel */
    /*private*/ static final int HL_TRANSP       = 0x02;    /* has no highlighting */
    /*private*/ static final int HL_ONELINE      = 0x04;    /* match within one line only */
    /*private*/ static final int HL_HAS_EOL      = 0x08;    /* end pattern that matches with $ */
    /*private*/ static final int HL_SYNC_HERE    = 0x10;    /* sync point after this item (syncing only) */
    /*private*/ static final int HL_SYNC_THERE   = 0x20;    /* sync point at current line (syncing only) */
    /*private*/ static final int HL_MATCH        = 0x40;    /* use match ID instead of item ID */
    /*private*/ static final int HL_SKIPNL       = 0x80;    /* nextgroup can skip newlines */
    /*private*/ static final int HL_SKIPWHITE    = 0x100;   /* nextgroup can skip white space */
    /*private*/ static final int HL_SKIPEMPTY    = 0x200;   /* nextgroup can skip empty lines */
    /*private*/ static final int HL_KEEPEND      = 0x400;   /* end match always kept */
    /*private*/ static final int HL_EXCLUDENL    = 0x800;   /* exclude NL from match */
    /*private*/ static final int HL_DISPLAY      = 0x1000;  /* only used for displaying, not syncing */
    /*private*/ static final int HL_FOLD         = 0x2000;  /* define fold */
    /*private*/ static final int HL_EXTEND       = 0x4000;  /* ignore a keepend */
    /*private*/ static final int HL_MATCHCONT    = 0x8000;  /* match continued from previous line */
    /*private*/ static final int HL_TRANS_CONT   = 0x10000; /* transparent item without contains arg */
    /*private*/ static final int HL_CONCEAL      = 0x20000; /* can be concealed */
    /*private*/ static final int HL_CONCEALENDS  = 0x40000; /* can be concealed */

    /* Values for 'options' argument in do_search() and searchit(). */
    /*private*/ static final int SEARCH_REV      = 0x01;    /* go in reverse of previous dir. */
    /*private*/ static final int SEARCH_ECHO     = 0x02;    /* echo the search command and handle options */
    /*private*/ static final int SEARCH_MSG      = 0x0c;    /* give messages (yes, it's not 0x04) */
    /*private*/ static final int SEARCH_NFMSG    = 0x08;    /* give all messages except not found */
    /*private*/ static final int SEARCH_OPT      = 0x10;    /* interpret optional flags */
    /*private*/ static final int SEARCH_HIS      = 0x20;    /* put search pattern in history */
    /*private*/ static final int SEARCH_END      = 0x40;    /* put cursor at end of match */
    /*private*/ static final int SEARCH_NOOF     = 0x80;    /* don't add offset to position */
    /*private*/ static final int SEARCH_START    = 0x100;   /* start search without col offset */
    /*private*/ static final int SEARCH_MARK     = 0x200;   /* set previous context mark */
    /*private*/ static final int SEARCH_KEEP     = 0x400;   /* keep previous search pattern */
    /*private*/ static final int SEARCH_PEEK     = 0x800;   /* peek for typed char, cancel search */

    /* Values for find_ident_under_cursor(). */
    /*private*/ static final int FIND_IDENT      = 1;       /* find identifier (word) */
    /*private*/ static final int FIND_STRING     = 2;       /* find any string (WORD) */
    /*private*/ static final int FIND_EVAL       = 4;       /* include "->", "[]" and "." */

    /* Values for buflist_getfile(). */
    /*private*/ static final int GETF_SETMARK    = 0x01;    /* set pcmark before jumping */
    /*private*/ static final int GETF_ALT        = 0x02;    /* jumping to alternate file (not buf num) */
    /*private*/ static final int GETF_SWITCH     = 0x04;    /* respect 'switchbuf' settings when jumping */

    /* Values for buflist_new() flags. */
    /*private*/ static final int BLN_CURBUF      = 1;       /* may re-use curbuf for new buffer */
    /*private*/ static final int BLN_LISTED      = 2;       /* put new buffer in buffer list */
    /*private*/ static final int BLN_DUMMY       = 4;       /* allocating dummy buffer */

    /* Values for in_cinkeys(). */
    /*private*/ static final int KEY_OPEN_FORW   = 0x101;
    /*private*/ static final int KEY_OPEN_BACK   = 0x102;
    /*private*/ static final int KEY_COMPLETE    = 0x103;   /* end of completion */

    /* Values for "noremap" argument of ins_typebuf().
     * Also used for map.m_noremap and menu.noremap[]. */
    /*private*/ static final int REMAP_YES       = 0;       /* allow remapping */
    /*private*/ static final int REMAP_NONE      = -1;      /* no remapping */
    /*private*/ static final int REMAP_SCRIPT    = -2;      /* remap script-local mappings only */
    /*private*/ static final int REMAP_SKIP      = -3;      /* no remapping for first char */

    /* Values for mch_call_shell() second argument. */
    /*private*/ static final int SHELL_FILTER    = 1;       /* filtering text */
    /*private*/ static final int SHELL_EXPAND    = 2;       /* expanding wildcards */
    /*private*/ static final int SHELL_COOKED    = 4;       /* set term to cooked mode */
    /*private*/ static final int SHELL_DOOUT     = 8;       /* redirecting output */
    /*private*/ static final int SHELL_SILENT    = 16;      /* don't print error returned by command */
    /*private*/ static final int SHELL_READ      = 32;      /* read lines and insert into buffer */
    /*private*/ static final int SHELL_WRITE     = 64;      /* write lines from buffer */

    /* Values returned by mch_nodetype(). */
    /*private*/ static final int NODE_NORMAL     = 0;       /* file or directory, check with mch_isdir() */
    /*private*/ static final int NODE_WRITABLE   = 1;       /* something we can write to (character
                                                         * device, fifo, socket, ..) */
    /*private*/ static final int NODE_OTHER      = 2;       /* non-writable thing (e.g., block device) */

    /* Values for readfile() flags. */
    /*private*/ static final int READ_NEW        = 0x01;    /* read a file into a new buffer */
    /*private*/ static final int READ_FILTER     = 0x02;    /* read filter output */
    /*private*/ static final int READ_STDIN      = 0x04;    /* read from stdin */
    /*private*/ static final int READ_BUFFER     = 0x08;    /* read from curbuf (converting stdin) */
    /*private*/ static final int READ_DUMMY      = 0x10;    /* reading into a dummy buffer */
    /*private*/ static final int READ_KEEP_UNDO  = 0x20;    /* keep undo info */

    /* Values for change_indent(). */
    /*private*/ static final int INDENT_SET      = 1;       /* set indent */
    /*private*/ static final int INDENT_INC      = 2;       /* increase indent */
    /*private*/ static final int INDENT_DEC      = 3;       /* decrease indent */

    /* Values for flags argument for findmatchlimit(). */
    /*private*/ static final int FM_BACKWARD     = 0x01;    /* search backwards */
    /*private*/ static final int FM_FORWARD      = 0x02;    /* search forwards */
    /*private*/ static final int FM_BLOCKSTOP    = 0x04;    /* stop at start/end of block */
    /*private*/ static final int FM_SKIPCOMM     = 0x08;    /* skip comments */

    /* Values for action argument for do_buffer(). */
    /*private*/ static final int DOBUF_GOTO      = 0;       /* go to specified buffer */
    /*private*/ static final int DOBUF_SPLIT     = 1;       /* split window and go to specified buffer */
    /*private*/ static final int DOBUF_UNLOAD    = 2;       /* unload specified buffer(s) */
    /*private*/ static final int DOBUF_DEL       = 3;       /* delete specified buffer(s) from buflist */
    /*private*/ static final int DOBUF_WIPE      = 4;       /* delete specified buffer(s) really */

    /* Values for start argument for do_buffer(). */
    /*private*/ static final int DOBUF_CURRENT   = 0;       /* "count" buffer from current buffer */
    /*private*/ static final int DOBUF_FIRST     = 1;       /* "count" buffer from first buffer */
    /*private*/ static final int DOBUF_LAST      = 2;       /* "count" buffer from last buffer */
    /*private*/ static final int DOBUF_MOD       = 3;       /* "count" mod. buffer from current buffer */

    /* Values for sub_cmd and which_pat argument for search_regcomp(). */
    /* Also used for which_pat argument for searchit(). */
    /*private*/ static final int RE_SEARCH       = 0;       /* save/use pat in/from search_pattern */
    /*private*/ static final int RE_SUBST        = 1;       /* save/use pat in/from subst_pattern */
    /*private*/ static final int RE_BOTH         = 2;       /* save pat in both patterns */
    /*private*/ static final int RE_LAST         = 2;       /* use last used pattern if "pat" is null */

    /* Second argument for vim_regcomp(). */
    /*private*/ static final int RE_MAGIC        = 1;       /* 'magic' option */
    /*private*/ static final int RE_STRING       = 2;       /* match in string instead of buffer text */
    /*private*/ static final int RE_STRICT       = 4;       /* don't allow [abc] without ] */
    /*private*/ static final int RE_AUTO         = 8;       /* automatic engine selection */

    /* values for reg_do_extmatch */
    /*private*/ static final int REX_SET         = 1;       /* to allow \z\(...\), */
    /*private*/ static final int REX_USE         = 2;       /* to allow \z\1 et al. */

    /* Return values for fullpathcmp(). */
    /* Note: can use (fullpathcmp() & FPC_SAME) to check for equal files. */
    /*private*/ static final int FPC_SAME        = 1;       /* both exist and are the same file. */
    /*private*/ static final int FPC_DIFF        = 2;       /* both exist and are different files. */
    /*private*/ static final int FPC_NOTX        = 4;       /* both don't exist. */
    /*private*/ static final int FPC_DIFFX       = 6;       /* one of them doesn't exist. */
    /*private*/ static final int FPC_SAMEX       = 7;       /* both don't exist and file names are same. */

    /* flags for do_ecmd() */
    /*private*/ static final int ECMD_HIDE       = 0x01;    /* don't free the current buffer */
    /*private*/ static final int ECMD_OLDBUF     = 0x04;    /* use existing buffer if it exists */
    /*private*/ static final int ECMD_FORCEIT    = 0x08;    /* ! used in Ex command */
    /*private*/ static final int ECMD_ADDBUF     = 0x10;    /* don't edit, just add to buffer list */

    /* for lnum argument in do_ecmd() */
    /*private*/ static final int ECMD_LASTL      = 0;       /* use last position in loaded file */
    /*private*/ static final int ECMD_LAST       = -1;      /* use last position in all files */
    /*private*/ static final int ECMD_ONE        = 1;       /* use first line */

    /* flags for do_cmdline() */
    /*private*/ static final int DOCMD_VERBOSE   = 0x01;    /* included command in error message */
    /*private*/ static final int DOCMD_NOWAIT    = 0x02;    /* don't call wait_return() and friends */
    /*private*/ static final int DOCMD_REPEAT    = 0x04;    /* repeat exec. until getline() returns null */
    /*private*/ static final int DOCMD_KEYTYPED  = 0x08;    /* don't reset keyTyped */
    /*private*/ static final int DOCMD_EXCRESET  = 0x10;    /* reset exception environment (for debugging) */
    /*private*/ static final int DOCMD_KEEPLINE  = 0x20;    /* keep typed line for repeating with "." */

    /* flags for beginline() */
    /*private*/ static final int BL_WHITE        = 1;       /* cursor on first non-white in the line */
    /*private*/ static final int BL_SOL          = 2;       /* use 'sol' option */
    /*private*/ static final int BL_FIX          = 4;       /* don't leave cursor on a NUL */

    /* flags for buf_copy_options() */
    /*private*/ static final int BCO_ENTER       = 1;       /* going to enter the buffer */
    /*private*/ static final int BCO_ALWAYS      = 2;       /* always copy the options */

    /* flags for do_put() */
    /*private*/ static final int PUT_FIXINDENT   = 1;       /* make indent look nice */
    /*private*/ static final int PUT_CURSEND     = 2;       /* leave cursor after end of new text */
    /*private*/ static final int PUT_CURSLINE    = 4;       /* leave cursor on last line of new text */
    /*private*/ static final int PUT_LINE        = 8;       /* put register as lines */
    /*private*/ static final int PUT_LINE_SPLIT  = 16;      /* split line for linewise register */
    /*private*/ static final int PUT_LINE_FORWARD = 32;     /* put linewise register below Visual sel. */

    /* flags for set_indent() */
    /*private*/ static final int SIN_CHANGED     = 1;       /* call changed_bytes() when line changed */
    /*private*/ static final int SIN_INSERT      = 2;       /* insert indent before existing text */
    /*private*/ static final int SIN_UNDO        = 4;       /* save line for undo before changing it */

    /* flags for insertchar() */
    /*private*/ static final int INSCHAR_FORMAT  = 1;       /* force formatting */
    /*private*/ static final int INSCHAR_DO_COM  = 2;       /* format comments */
    /*private*/ static final int INSCHAR_CTRLV   = 4;       /* char typed just after CTRL-V */
    /*private*/ static final int INSCHAR_NO_FEX  = 8;       /* don't use 'formatexpr' */
    /*private*/ static final int INSCHAR_COM_LIST = 16;     /* format comments with list/2nd line indent */

    /* flags for open_line() */
    /*private*/ static final int OPENLINE_DELSPACES  = 1;   /* delete spaces after cursor */
    /*private*/ static final int OPENLINE_DO_COM     = 2;   /* format comments */
    /*private*/ static final int OPENLINE_KEEPTRAIL  = 4;   /* keep trailing spaces */
    /*private*/ static final int OPENLINE_MARKFIX    = 8;   /* fix mark positions */
    /*private*/ static final int OPENLINE_COM_LIST  = 16;   /* format comments with list/2nd line indent */

    /*
     * There are five history tables:
     */
    /*private*/ static final int HIST_CMD        = 0;       /* colon commands */
    /*private*/ static final int HIST_SEARCH     = 1;       /* search commands */
    /*private*/ static final int HIST_EXPR       = 2;       /* expressions (from entering = register) */
    /*private*/ static final int HIST_INPUT      = 3;       /* input() lines */
    /*private*/ static final int HIST_DEBUG      = 4;       /* debug commands */
    /*private*/ static final int HIST_COUNT      = 5;       /* number of history tables */

    /*
     * Flags for chartab[].
     */
    /*private*/ static final int CT_CELL_MASK    = 0x07;    /* mask: nr of display cells (1, 2 or 4) */
    /*private*/ static final int CT_PRINT_CHAR   = 0x10;    /* flag: set for printable chars */
    /*private*/ static final int CT_ID_CHAR      = 0x20;    /* flag: set for ID chars */
    /*private*/ static final int CT_FNAME_CHAR   = 0x40;    /* flag: set for file name chars */

    /*
     * Return values for functions like gui_yesnocancel()
     */
    /*private*/ static final int VIM_YES         = 2;
    /*private*/ static final int VIM_NO          = 3;
    /*private*/ static final int VIM_CANCEL      = 4;
    /*private*/ static final int VIM_ALL         = 5;
    /*private*/ static final int VIM_DISCARDALL  = 6;

    /*
     * arguments for win_split()
     */
    /*private*/ static final int WSP_ROOM        = 1;       /* require enough room */
    /*private*/ static final int WSP_VERT        = 2;       /* split vertically */
    /*private*/ static final int WSP_TOP         = 4;       /* window at top-left of shell */
    /*private*/ static final int WSP_BOT         = 8;       /* window at bottom-right of shell */
    /*private*/ static final int WSP_BELOW       = 32;      /* put new window below/right */
    /*private*/ static final int WSP_ABOVE       = 64;      /* put new window above/left */
    /*private*/ static final int WSP_NEWLOC      = 128;     /* don't copy location list */

    /*
     * arguments for gui_set_shellsize()
     */
    /*private*/ static final int RESIZE_VERT     = 1;       /* resize vertically */
    /*private*/ static final int RESIZE_HOR      = 2;       /* resize horizontally */
    /*private*/ static final int RESIZE_BOTH     = 15;      /* resize in both directions */

    /*
     * flags for check_changed()
     */
    /*private*/ static final int CCGD_AW         = 1;       /* do autowrite if buffer was changed */
    /*private*/ static final int CCGD_MULTWIN    = 2;       /* check also when several wins for the buf */
    /*private*/ static final int CCGD_FORCEIT    = 4;       /* ! used */
    /*private*/ static final int CCGD_ALLBUF     = 8;       /* may write all buffers */
    /*private*/ static final int CCGD_EXCMD      = 16;      /* may suggest using ! */

    /*
     * "flags" values for option-setting functions.
     * When OPT_GLOBAL and OPT_LOCAL are both missing, set both local and global
     * values, get local value.
     */
    /*private*/ static final int OPT_FREE        = 1;       /* free old value if it was allocated */
    /*private*/ static final int OPT_GLOBAL      = 2;       /* use global value */
    /*private*/ static final int OPT_LOCAL       = 4;       /* use local value */
    /*private*/ static final int OPT_MODELINE    = 8;       /* option in modeline */
    /*private*/ static final int OPT_WINONLY     = 16;      /* only set window-local options */
    /*private*/ static final int OPT_NOWIN       = 32;      /* don't set window-local options */

    /* Magic chars used in confirm dialog strings. */
    /*private*/ static final byte DLG_BUTTON_SEP  = '\n';
    /*private*/ static final byte DLG_HOTKEY_CHAR = '&';

    /* Values for "starting". */
    /*private*/ static final int NO_SCREEN       = 2;       /* no screen updating yet */
    /*private*/ static final int NO_BUFFERS      = 1;       /* not all buffers loaded yet */
    /*                                         0           not starting anymore */

    /* Values for swap_exists_action: what to do when swap file already exists. */
    /*private*/ static final int SEA_NONE        = 0;       /* don't use dialog */
    /*private*/ static final int SEA_DIALOG      = 1;       /* use dialog when possible */
    /*private*/ static final int SEA_QUIT        = 2;       /* quit editing the file */

    /* Special values for current_SID. */
    /*private*/ static final int SID_CMDARG      = -2;      /* for "--cmd" argument */
    /*private*/ static final int SID_CARG        = -3;      /* for "-c" argument */
    /*private*/ static final int SID_ENV         = -4;      /* for sourcing environment variable */
    /*private*/ static final int SID_ERROR       = -5;      /* option was reset because of an error */
    /*private*/ static final int SID_NONE        = -6;      /* don't set scriptID */

    /*
     * Events for autocommands.
     */
    /*private*/ static final int
        EVENT_BUFADD = 0,                /* after adding a buffer to the buffer list */
        EVENT_BUFNEW = 1,                /* after creating any buffer */
        EVENT_BUFDELETE = 2,             /* deleting a buffer from the buffer list */
        EVENT_BUFWIPEOUT = 3,            /* just before really deleting a buffer */
        EVENT_BUFENTER = 4,              /* after entering a buffer */
        EVENT_BUFFILEPOST = 5,           /* after renaming a buffer */
        EVENT_BUFFILEPRE = 6,            /* before renaming a buffer */
        EVENT_BUFLEAVE = 7,              /* before leaving a buffer */
        EVENT_BUFNEWFILE = 8,            /* when creating a buffer for a new file */
        EVENT_BUFREADPOST = 9,           /* after reading a buffer */
        EVENT_BUFREADPRE = 10,           /* before reading a buffer */
        EVENT_BUFREADCMD = 11,           /* read buffer using command */
        EVENT_BUFUNLOAD = 12,            /* just before unloading a buffer */
        EVENT_BUFHIDDEN = 13,            /* just after buffer becomes hidden */
        EVENT_BUFWINENTER = 14,          /* after showing a buffer in a window */
        EVENT_BUFWINLEAVE = 15,          /* just after buffer removed from window */
        EVENT_BUFWRITEPOST = 16,         /* after writing a buffer */
        EVENT_BUFWRITEPRE = 17,          /* before writing a buffer */
        EVENT_BUFWRITECMD = 18,          /* write buffer using command */
        EVENT_CMDWINENTER = 19,          /* after entering the cmdline window */
        EVENT_CMDWINLEAVE = 20,          /* before leaving the cmdline window */
        EVENT_COLORSCHEME = 21,          /* after loading a colorscheme */
        EVENT_FILEAPPENDPOST = 22,       /* after appending to a file */
        EVENT_FILEAPPENDPRE = 23,        /* before appending to a file */
        EVENT_FILEAPPENDCMD = 24,        /* append to a file using command */
        EVENT_FILECHANGEDSHELL = 25,     /* after shell command that changed file */
        EVENT_FILECHANGEDSHELLPOST = 26, /* after (not) reloading changed file */
        EVENT_FILECHANGEDRO = 27,        /* before first change to read-only file */
        EVENT_FILEREADPOST = 28,         /* after reading a file */
        EVENT_FILEREADPRE = 29,          /* before reading a file */
        EVENT_FILEREADCMD = 30,          /* read from a file using command */
        EVENT_FILETYPE = 31,             /* new file type detected (user defined) */
        EVENT_FILEWRITEPOST = 32,        /* after writing a file */
        EVENT_FILEWRITEPRE = 33,         /* before writing a file */
        EVENT_FILEWRITECMD = 34,         /* write to a file using command */
        EVENT_FILTERREADPOST = 35,       /* after reading from a filter */
        EVENT_FILTERREADPRE = 36,        /* before reading from a filter */
        EVENT_FILTERWRITEPOST = 37,      /* after writing to a filter */
        EVENT_FILTERWRITEPRE = 38,       /* before writing to a filter */
        EVENT_FOCUSGAINED = 39,          /* got the focus */
        EVENT_FOCUSLOST = 40,            /* lost the focus to another app */
        EVENT_INSERTCHANGE = 41,         /* when changing Insert/Replace mode */
        EVENT_INSERTENTER = 42,          /* when entering Insert mode */
        EVENT_INSERTLEAVE = 43,          /* when leaving Insert mode */
        EVENT_QUITPRE = 44,              /* before :quit */
        EVENT_STDINREADPOST = 45,        /* after reading from stdin */
        EVENT_STDINREADPRE = 46,         /* before reading from stdin */
        EVENT_SYNTAX = 47,               /* syntax selected */
        EVENT_TERMCHANGED = 48,          /* after changing 'term' */
        EVENT_TERMRESPONSE = 49,         /* after setting "v:termresponse" */
        EVENT_USER = 50,                 /* user defined autocommand */
        EVENT_VIMENTER = 51,             /* after starting Vim */
        EVENT_VIMLEAVE = 52,             /* before exiting Vim */
        EVENT_VIMLEAVEPRE = 53,          /* before exiting Vim and writing .viminfo */
        EVENT_VIMRESIZED = 54,           /* after Vim window was resized */
        EVENT_WINENTER = 55,             /* after entering a window */
        EVENT_WINLEAVE = 56,             /* before leaving a window */
        EVENT_INSERTCHARPRE = 57,        /* before inserting a char */
        EVENT_CURSORHOLD = 58,           /* cursor in same position for a while */
        EVENT_CURSORHOLDI = 59,          /* idem, in Insert mode */
        EVENT_FUNCUNDEFINED = 60,        /* if calling a function which doesn't exist */
        EVENT_REMOTEREPLY = 61,          /* upon string reception from a remote vim */
        EVENT_SWAPEXISTS = 62,           /* found existing swap file */
        EVENT_SOURCEPRE = 63,            /* before sourcing a Vim script */
        EVENT_SOURCECMD = 64,            /* sourcing a Vim script using command */
        EVENT_CURSORMOVED = 65,          /* cursor was moved */
        EVENT_CURSORMOVEDI = 66,         /* cursor was moved in Insert mode */
        EVENT_TABLEAVE = 67,             /* before leaving a tab page */
        EVENT_TABENTER = 68,             /* after entering a tab page */
        EVENT_SHELLCMDPOST = 69,         /* after ":!cmd" */
        EVENT_SHELLFILTERPOST = 70,      /* after ":1,2!cmd", ":w !cmd", ":r !cmd". */
        EVENT_TEXTCHANGED = 71,          /* text was modified */
        EVENT_TEXTCHANGEDI = 72,         /* text was modified in Insert mode */
        EVENT_CMDUNDEFINED = 73,         /* command undefined */
        NUM_EVENTS = 74;                 /* MUST be the last one */

    /*
     * Values for index in highlight_attr[].
     * When making changes, also update HL_FLAGS below!
     * And update the default value of 'highlight' in option.c.
     */
    /*private*/ static final int
        HLF_8 = 0,        /* Meta & special keys listed with ":map", text that is
                           * displayed different from what it is */
        HLF_AT = 1,       /* @ and ~ characters at end of screen, characters that
                           * don't really exist in the text */
        HLF_D = 2,        /* directories in CTRL-D listing */
        HLF_E = 3,        /* error messages */
        HLF_H = 4,        /* obsolete, ignored */
        HLF_I = 5,        /* incremental search */
        HLF_L = 6,        /* last search string */
        HLF_M = 7,        /* "--More--" message */
        HLF_CM = 8,       /* Mode (e.g., "-- INSERT --") */
        HLF_N = 9,        /* line number for ":number" and ":#" commands */
        HLF_CLN = 10,     /* current line number */
        HLF_R = 11,       /* return to continue message and yes/no questions */
        HLF_S = 12,       /* status lines */
        HLF_SNC = 13,     /* status lines of not-current windows */
        HLF_C = 14,       /* column to separate vertically split windows */
        HLF_T = 15,       /* Titles for output from ":set all", ":autocmd" etc. */
        HLF_V = 16,       /* Visual mode */
        HLF_VNC = 17,     /* Visual mode, autoselecting and not clipboard owner */
        HLF_W = 18,       /* warning messages */
        HLF_WM = 19,      /* Wildmenu highlight */
        HLF_FL = 20,      /* Folded line */
        HLF_FC = 21,      /* Fold column */
        HLF_ADD = 22,     /* Added diff line */
        HLF_CHD = 23,     /* Changed diff line */
        HLF_DED = 24,     /* Deleted diff line */
        HLF_TXD = 25,     /* Text Changed in diff line */
        HLF_CONCEAL = 26, /* Concealed text */
        HLF_SC = 27,      /* Sign column */
        HLF_SPB = 28,     /* SpellBad */
        HLF_SPC = 29,     /* SpellCap */
        HLF_SPR = 30,     /* SpellRare */
        HLF_SPL = 31,     /* SpellLocal */
        HLF_PNI = 32,     /* popup menu normal item */
        HLF_PSI = 33,     /* popup menu selected item */
        HLF_PSB = 34,     /* popup menu scrollbar */
        HLF_PST = 35,     /* popup menu scrollbar thumb */
        HLF_TP = 36,      /* tabpage line */
        HLF_TPS = 37,     /* tabpage line selected */
        HLF_TPF = 38,     /* tabpage line filler */
        HLF_CUC = 39,     /* 'cursorcolumn' */
        HLF_CUL = 40,     /* 'cursorline' */
        HLF_MC = 41,      /* 'colorcolumn' */
        HLF_COUNT = 42;   /* MUST be the last one */

    /*
     * Operator IDs; The order must correspond to opchars[] in ops.c!
     */
    /*private*/ static final int
        OP_NOP      =  0,       /* no pending operation */
        OP_DELETE   =  1,       /* "d"  delete operator */
        OP_YANK     =  2,       /* "y"  yank operator */
        OP_CHANGE   =  3,       /* "c"  change operator */
        OP_LSHIFT   =  4,       /* "<"  left shift operator */
        OP_RSHIFT   =  5,       /* ">"  right shift operator */
        OP_FILTER   =  6,       /* "!"  filter operator */
        OP_TILDE    =  7,       /* "g~" switch case operator */
        OP_INDENT   =  8,       /* "="  indent operator */
        OP_FORMAT   =  9,       /* "gq" format operator */
        OP_COLON    = 10,       /* ":"  colon operator */
        OP_UPPER    = 11,       /* "gU" make upper case operator */
        OP_LOWER    = 12,       /* "gu" make lower case operator */
        OP_JOIN     = 13,       /* "J"  join operator, only for Visual mode */
        OP_JOIN_NS  = 14,       /* "gJ"  join operator, only for Visual mode */
        OP_ROT13    = 15,       /* "g?" rot-13 encoding */
        OP_REPLACE  = 16,       /* "r"  replace chars, only for Visual mode */
        OP_INSERT   = 17,       /* "I"  Insert column, only for Visual mode */
        OP_APPEND   = 18,       /* "A"  Append column, only for Visual mode */
        OP_FORMAT2  = 19,       /* "gw" format operator, keeps cursor pos */
        OP_FUNCTION = 20;       /* "g@" call 'operatorfunc' */

    /*
     * Motion types, used for operators and for yank/delete registers.
     */
    /*private*/ static final byte
        MCHAR   = 0,            /* character-wise movement/register */
        MLINE   = 1,            /* line-wise movement/register */
        MBLOCK  = 2,            /* block-wise register */
        MAUTO   = (byte)0xff;   /* decide between MLINE/MCHAR */

    /*
     * Minimum screen size.
     */
    /*private*/ static final int MIN_COLUMNS     = 12;          /* minimal columns for screen */
    /*private*/ static final int MIN_LINES       = 2;           /* minimal lines for screen */
    /*private*/ static final int STATUS_HEIGHT   = 1;           /* height of a status line under a window */

    /*private*/ static final int IOSIZE          = 1024 + 1;    /* file i/o and sprintf buffer size */

    /*private*/ static final int DIALOG_MSG_SIZE = 1000;        /* buffer size for dialog_msg() */

    /*private*/ static final int
        MSG_BUF_LEN     = 480,              /* length of buffer for small messages */
        MSG_BUF_CLEN    = MSG_BUF_LEN / 6;  /* cell length (worst case: utf-8 takes 6 bytes for one cell) */

    /*
     * Maximum length of key sequence to be mapped.
     * Must be able to hold an Amiga resize report.
     */
    /*private*/ static final int MAXMAPLEN = 50;

    /* Size in bytes of the hash used in the undo file. */
    /*private*/ static final int UNDO_HASH_SIZE = 32;

    /*private*/ static final long MAXLNUM = 0x7fffffffL;           /* maximum (invalid) line number */

    /*
     * Well, you won't believe it, but some S/390 machines ("host", now also known
     * as zServer) use 31 bit pointers.  There are also some newer machines, that
     * use 64 bit pointers.  I don't know how to distinguish between 31 and 64 bit
     * machines, so the best way is to assume 31 bits whenever we detect OS/390 Unix.
     * With this we restrict the maximum line length to 1073741823.  I guess this is
     * not a real problem.  BTW:  Longer lines are split.
     */
    /*private*/ static final int MAXCOL = 0x7fffffff;           /* maximum column number, 31 bits */

    /*private*/ static final int SHOWCMD_COLS = 10;                 /* columns needed by shown command */
    /*private*/ static final int STL_MAX_ITEM = 80;                 /* max nr of %<flag> in statusline */

    /*
     * vim_iswhite() is used for "^" and the like.  It differs from isspace()
     * because it doesn't include <CR> and <LF> and the like.
     */
    /*private*/ static boolean vim_iswhite(int x)
    {
        return (x == ' ' || x == '\t');
    }

    /*private*/ static final int MAX_MCO        = 6;        /* maximum value for 'maxcombine' */

    /* Maximum number of bytes in a multi-byte character.  It can be one 32-bit
     * character of up to 6 bytes, or one 16-bit character of up to three bytes
     * plus six following composing characters of three bytes each. */
    /*private*/ static final int MB_MAXBYTES    = 21;
}
