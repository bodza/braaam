#if !defined(VIM__H)
#define VIM__H

/* ============ the header file puzzle (ca. 50-100 pieces) ========= */

/* Define when terminfo support found */
#define TERMINFO 1

/* Define when __attribute__((unused)) can be used */
#define HAVE_ATTRIBUTE_UNUSED 1

/* Defined to the size of an int */
#define VIM_SIZEOF_INT 4

/* Defined to the size of a long */
#define VIM_SIZEOF_LONG 8

/* Defined to the size of off_t */
#define SIZEOF_OFF_T 8

/* Defined to the size of time_t */
#define SIZEOF_TIME_T 8

/* Define if tgetent() returns zero for an error */
#define TGETENT_ZERO_ERR 0

#define HAVE_TGETENT 1

/* user ID of root is usually zero, but not for everybody */
#define ROOT_UID 0

/*
 * Message history is fixed at 200 message, 20 for the tiny version.
 */
#define MAX_MSG_HIST_LEN 200

/* Define this if you want to use 16 bit Unicode only, reduces memory used for
 * the screen structures. */
/* #define UNICODE16 */

/* Use iconv() when it's available. */
#define USE_ICONV

/*
 * +xterm_save          The t_ti and t_te entries for the builtin xterm will
 *                      be set to save the screen when starting Vim and
 *                      restoring it when exiting.
 */
/* #define FEAT_XTERM_SAVE */

/*
 * SESSION_FILE         Name of the default ":mksession" file.
 */
#define SESSION_FILE    "Session.vim"

/*
 * SYS_OPTWIN_FILE      Name of the default optwin.vim file.
 */
#define SYS_OPTWIN_FILE        "$VIMRUNTIME/optwin.vim"

/*
 * RUNTIME_DIRNAME      Generic name for the directory of the runtime files.
 */
#define RUNTIME_DIRNAME "runtime"

/*
 * +termresponse        send t_RV to obtain terminal response.  Used for xterm
 *                      to check if mouse dragging can be used and if term
 *                      codes can be obtained.
 */
#if defined(HAVE_TGETENT)
#define FEAT_TERMRESPONSE
#endif

/* ----------------------------------------------------------------------- */

/* #include "os_unix.h" */    /* bring lots of system header files */

#include <stdio.h>
#include <ctype.h>

#include <sys/types.h>
#include <sys/stat.h>

#include <stdlib.h>
#include <unistd.h>

#include <sys/param.h>

#define vim_mkdir(x, y) mkdir((char *)(x), y)
#define mch_rmdir(x) rmdir((char *)(x))
#define mch_remove(x) unlink((char *)(x))

#define SIGDEFARG(s)  (s) int s UNUSED;

#include <dirent.h>
#if !defined(NAMLEN)
#define NAMLEN(dirent) strlen((dirent)->d_name)
#endif

#include <time.h>      /* on some systems time.h should not be included together with sys/time.h */
#include <sys/time.h>

#include <signal.h>

#if defined(DIRSIZ) && !defined(MAXNAMLEN)
#define MAXNAMLEN DIRSIZ
#endif

#if defined(UFS_MAXNAMLEN) && !defined(MAXNAMLEN)
#define MAXNAMLEN UFS_MAXNAMLEN    /* for dynix/ptx */
#endif

#if defined(NAME_MAX) && !defined(MAXNAMLEN)
#define MAXNAMLEN NAME_MAX         /* for Linux before .99p3 */
#endif

/*
 * Note: if MAXNAMLEN has the wrong value, you will get error messages
 *       for not being able to open the swap file.
 */
#if !defined(MAXNAMLEN)
#define MAXNAMLEN 512              /* for all other Unix */
#endif

#define BASENAMELEN     (MAXNAMLEN - 5)

#include <pwd.h>

/*
 * Unix system-dependent file names
 */
#define SYS_VIMRC_FILE "$VIM/vimrc"

#define FILETYPE_FILE  "filetype.vim"
#define FTPLUGIN_FILE  "ftplugin.vim"
#define INDENT_FILE    "indent.vim"
#define FTOFF_FILE     "ftoff.vim"
#define FTPLUGOF_FILE  "ftplugof.vim"
#define INDOFF_FILE    "indoff.vim"

#define USR_EXRC_FILE "$HOME/.exrc"
#define USR_VIMRC_FILE "$HOME/.vimrc"

#define EVIM_FILE      "$VIMRUNTIME/evim.vim"

#define EXRC_FILE      ".exrc"
#define VIMRC_FILE     ".vimrc"

#define SYNTAX_FNAME   "$VIMRUNTIME/syntax/%s.vim"

#define DFLT_BDIR    ".,~/tmp,~/"    /* default for 'backupdir' */
#define DFLT_DIR     ".,~/tmp,/var/tmp,/tmp" /* default for 'directory' */

#define DFLT_ERRORFILE          "errors.err"

#define DFLT_RUNTIMEPATH     "~/.vim,$VIM/vimfiles,$VIMRUNTIME,$VIM/vimfiles/after,~/.vim/after"

#define TEMPDIRNAMES  "$TMPDIR", "/tmp", ".", "$HOME"
#define TEMPNAMELEN    256

/* Special wildcards that need to be handled by the shell */
#define SPECIAL_WILDCHAR    "`'{"

/*
 * Unix has plenty of memory, use large buffers
 */
#define CMDBUFFSIZE 1024        /* size of the command processing buffer */

/* Use the system path length if it makes sense. */
#if defined(PATH_MAX) && (PATH_MAX > 1000)
#define MAXPATHL       PATH_MAX
#else
#define MAXPATHL       1024
#endif

#define CHECK_INODE             /* used when checking if a swap file already exists for a file */
#if !defined(DFLT_MAXMEM)
#define DFLT_MAXMEM   (5*1024)         /* use up to 5 Mbyte for a buffer */
#endif
#if !defined(DFLT_MAXMEMTOT)
#define DFLT_MAXMEMTOT        (10*1024)    /* use up to 10 Mbyte for Vim */
#endif

/* memmove is not present on all systems, use memmove, bcopy, memcpy or our
 * own version */
/* Some systems have (void *) arguments, some (char *). If we use (char *) it
 * works for all */
#define mch_memmove(to, from, len) memmove((char *)(to), (char *)(from), len)

#define mch_rename(src, dst) rename(src, dst)
#define mch_getenv(x) (char_u *)getenv((char *)(x))
#define mch_setenv(name, val, x) setenv(name, val, x)

#if !defined(S_ISDIR) && defined(S_IFDIR)
#define S_ISDIR(m) (((m) & S_IFMT) == S_IFDIR)
#endif
#if !defined(S_ISREG) && defined(S_IFREG)
#define S_ISREG(m) (((m) & S_IFMT) == S_IFREG)
#endif
#if !defined(S_ISBLK) && defined(S_IFBLK)
#define S_ISBLK(m) (((m) & S_IFMT) == S_IFBLK)
#endif
#if !defined(S_ISSOCK) && defined(S_IFSOCK)
#define S_ISSOCK(m) (((m) & S_IFMT) == S_IFSOCK)
#endif
#if !defined(S_ISFIFO) && defined(S_IFIFO)
#define S_ISFIFO(m) (((m) & S_IFMT) == S_IFIFO)
#endif
#if !defined(S_ISCHR) && defined(S_IFCHR)
#define S_ISCHR(m) (((m) & S_IFMT) == S_IFCHR)
#endif

/* Note: Some systems need both string.h and strings.h (Savage).  However,
 * some systems can't handle both, only use string.h in that case. */
#include <string.h>
#include <strings.h>

#include <setjmp.h>
#define JMP_BUF jmp_buf
#define SETJMP(x) setjmp(x)
#define LONGJMP longjmp

/* ----------------------------------------------------------------------- */

/* Mark unused function arguments with UNUSED, so that gcc -Wunused-parameter
 * can be used to check for mistakes. */
#if defined(HAVE_ATTRIBUTE_UNUSED)
#define UNUSED __attribute__((unused))
#else
#define UNUSED
#endif

extern void (*sigset(int, void (*func)(int)))(int);

/*
 * Maximum length of a path (for non-unix systems) Make it a bit long, to stay
 * on the safe side.  But not too long to put on the stack.
 */
#if !defined(MAXPATHL)
#if defined(MAXPATHLEN)
#define MAXPATHL  MAXPATHLEN
#else
#define MAXPATHL  256
#endif
#endif
#define PATH_ESC_CHARS ((char_u *)" \t\n*?[{`$\\%#'\"|!<")
#define SHELL_ESC_CHARS ((char_u *)" \t\n*?[{`$\\%#'\"|!<>();&")

#define NUMBUFLEN 30        /* length of a buffer to store a number in ASCII */

/*
 * Shorthand for unsigned variables. Many systems, but not all, have u_char
 * already defined, so we use char_u to avoid trouble.
 */
typedef unsigned char   char_u;
typedef unsigned short  short_u;
typedef unsigned int    int_u;
/* Make sure long_u is big enough to hold a pointer.
 * On Win64, longs are 32 bits and pointers are 64 bits.
 * For printf() and scanf(), we need to take care of long_u specifically. */

  /* Microsoft-specific. The __w64 keyword should be specified on any typedefs
   * that change size between 32-bit and 64-bit platforms.  For any such type,
   * __w64 should appear only on the 32-bit definition of the typedef.
   * Define __w64 as an empty token for everything but MSVC 7.x or later.
   */
#define __w64
typedef unsigned long __w64     long_u;
typedef          long __w64     long_i;

/*
 * Only systems which use configure will have SIZEOF_OFF_T and VIM_SIZEOF_LONG
 * defined, which is ok since those are the same systems which can have
 * varying sizes for off_t.  The other systems will continue to use "%ld" to
 * print off_t since off_t is simply a typedef to long for them.
 */
#if defined(SIZEOF_OFF_T) && (SIZEOF_OFF_T > VIM_SIZEOF_LONG)
#define LONG_LONG_OFF_T
#endif

/*
 * The characters and attributes cached for the screen.
 */
typedef char_u schar_T;
typedef unsigned short sattr_T;
#define MAX_TYPENR 65535

/*
 * The u8char_T can hold one decoded UTF-8 character.
 * We normally use 32 bits now, since some Asian characters don't fit in 16
 * bits.  u8char_T is only used for displaying, it could be 16 bits to save memory.
 */
#if defined(UNICODE16)
typedef unsigned short u8char_T;    /* short should be 16 bits */
#else
typedef unsigned int u8char_T;      /* int is 32 bits */
#endif

/* ----------------------------------------------------------------------- */

/* #include "ascii.h" */
/*
 * Definitions of various common control characters.
 */

#define CharOrd(x)      ((x) < 'a' ? (x) - 'A' : (x) - 'a')
#define CharOrdLow(x)   ((x) - 'a')
#define CharOrdUp(x)    ((x) - 'A')
#define ROT13(c, a)     (((((c) - (a)) + 13) % 26) + (a))

#define NUL             '\000'
#define BELL            '\007'
#define BS              '\010'
#define TAB             '\011'
#define NL              '\012'
#define NL_STR          (char_u *)"\012"
#define FF              '\014'
#define CAR             '\015'  /* CR is used by Mac OS X */
#define ESC             '\033'
#define ESC_STR         (char_u *)"\033"
#define ESC_STR_nc      "\033"
#define DEL             0x7f
#define DEL_STR         (char_u *)"\177"
#define CSI             0x9b    /* Control Sequence Introducer */
#define CSI_STR         "\233"
#define DCS             0x90    /* Device Control String */
#define STERM           0x9c    /* String Terminator */

#define POUND           0xA3

#define Ctrl_chr(x)     (TOUPPER_ASC(x) ^ 0x40) /* '?' -> DEL, '@' -> ^@, etc. */
#define Meta(x)         ((x) | 0x80)

#define CTRL_F_STR      "\006"
#define CTRL_H_STR      "\010"
#define CTRL_V_STR      "\026"

#define Ctrl_AT         0   /* @ */
#define Ctrl_A          1
#define Ctrl_B          2
#define Ctrl_C          3
#define Ctrl_D          4
#define Ctrl_E          5
#define Ctrl_F          6
#define Ctrl_G          7
#define Ctrl_H          8
#define Ctrl_I          9
#define Ctrl_J          10
#define Ctrl_K          11
#define Ctrl_L          12
#define Ctrl_M          13
#define Ctrl_N          14
#define Ctrl_O          15
#define Ctrl_P          16
#define Ctrl_Q          17
#define Ctrl_R          18
#define Ctrl_S          19
#define Ctrl_T          20
#define Ctrl_U          21
#define Ctrl_V          22
#define Ctrl_W          23
#define Ctrl_X          24
#define Ctrl_Y          25
#define Ctrl_Z          26
                            /* CTRL- [ Left Square Bracket == ESC */
#define Ctrl_BSL        28  /* \ BackSLash */
#define Ctrl_RSB        29  /* ] Right Square Bracket */
#define Ctrl_HAT        30  /* ^ */
#define Ctrl__          31

/*
 * Character that separates dir names in a path.
 */
#define PATHSEP        '/'
#define PATHSEPSTR     "/"

/* ----------------------------------------------------------------------- */

/* #include "keymap.h" */
/*
 * Keycode definitions for special keys.
 *
 * Any special key code sequences are replaced by these codes.
 */

/*
 * For MSDOS some keys produce codes larger than 0xff. They are split into two
 * chars, the first one is K_NUL (same value used in term.h).
 */
#define K_NUL                   (0xce)  /* for MSDOS: special key follows */

/*
 * K_SPECIAL is the first byte of a special key code and is always followed by two bytes.
 * The second byte can have any value. ASCII is used for normal termcap
 * entries, 0x80 and higher for special keys, see below.
 * The third byte is guaranteed to be between 0x02 and 0x7f.
 */

#define K_SPECIAL               (0x80)

/*
 * Positive characters are "normal" characters.
 * Negative characters are special key codes.  Only characters below -0x200
 * are used to so that the absolute value can't be mistaken for a single-byte character.
 */
#define IS_SPECIAL(c)           ((c) < 0)

/*
 * Characters 0x0100 - 0x01ff have a special meaning for abbreviations.
 * Multi-byte characters also have ABBR_OFF added, thus are above 0x0200.
 */
#define ABBR_OFF                0x100

/*
 * NUL cannot be in the input string, therefore it is replaced by
 *      K_SPECIAL   KS_ZERO     KE_FILLER
 */
#define KS_ZERO                 255

/*
 * K_SPECIAL cannot be in the input string, therefore it is replaced by
 *      K_SPECIAL   KS_SPECIAL  KE_FILLER
 */
#define KS_SPECIAL              254

/*
 * KS_EXTRA is used for keys that have no termcap name
 *      K_SPECIAL   KS_EXTRA    KE_xxx
 */
#define KS_EXTRA                253

/*
 * KS_MODIFIER is used when a modifier is given for a (special) key
 *      K_SPECIAL   KS_MODIFIER bitmask
 */
#define KS_MODIFIER             252

/*
 * These are used for the GUI
 *      K_SPECIAL   KS_xxx      KE_FILLER
 */
#define KS_MOUSE                251
#define KS_MENU                 250
#define KS_VER_SCROLLBAR        249
#define KS_HOR_SCROLLBAR        248

/*
 * These are used for DEC mouse
 */
#define KS_NETTERM_MOUSE        247
#define KS_DEC_MOUSE            246

/*
 * Used for switching Select mode back on after a mapping or menu.
 */
#define KS_SELECT               245
#define K_SELECT_STRING         (char_u *)"\200\365X"

/*
 * Used for tearing off a menu.
 */
#define KS_TEAROFF              244

/* Used for JSB term mouse. */
#define KS_JSBTERM_MOUSE        243

/* Used a termcap entry that produces a normal character. */
#define KS_KEY                  242

/* Used for the qnx pterm mouse. */
#define KS_PTERM_MOUSE          241

/* Used for click in a tab pages label. */
#define KS_TABLINE              240

/* Used for menu in a tab pages line. */
#define KS_TABMENU              239

/* Used for the urxvt mouse. */
#define KS_URXVT_MOUSE          238

/* Used for the sgr mouse. */
#define KS_SGR_MOUSE            237

/*
 * Filler used after KS_SPECIAL and others
 */
#define KE_FILLER               ('X')

/*
 * translation of three byte code "K_SPECIAL a b" into int "K_xxx" and back
 */
#define TERMCAP2KEY(a, b)       (-((a) + ((int)(b) << 8)))
#define KEY2TERMCAP0(x)         ((-(x)) & 0xff)
#define KEY2TERMCAP1(x)         (((unsigned)(-(x)) >> 8) & 0xff)

/*
 * get second or third byte when translating special key code into three bytes
 */
#define K_SECOND(c)     ((c) == K_SPECIAL ? KS_SPECIAL : (c) == NUL ? KS_ZERO : KEY2TERMCAP0(c))

#define K_THIRD(c)      (((c) == K_SPECIAL || (c) == NUL) ? KE_FILLER : KEY2TERMCAP1(c))

/*
 * get single int code from second byte after K_SPECIAL
 */
#define TO_SPECIAL(a, b)    ((a) == KS_SPECIAL ? K_SPECIAL : (a) == KS_ZERO ? K_ZERO : TERMCAP2KEY(a, b))

/*
 * Codes for keys that do not have a termcap name.
 *
 * K_SPECIAL KS_EXTRA KE_xxx
 */
enum key_extra
{
    KE_NAME = 3         /* name of this terminal entry */

    , KE_S_UP           /* shift-up */
    , KE_S_DOWN         /* shift-down */

    , KE_S_F1           /* shifted function keys */
    , KE_S_F2
    , KE_S_F3
    , KE_S_F4
    , KE_S_F5
    , KE_S_F6
    , KE_S_F7
    , KE_S_F8
    , KE_S_F9
    , KE_S_F10

    , KE_S_F11
    , KE_S_F12
    , KE_S_F13
    , KE_S_F14
    , KE_S_F15
    , KE_S_F16
    , KE_S_F17
    , KE_S_F18
    , KE_S_F19
    , KE_S_F20

    , KE_S_F21
    , KE_S_F22
    , KE_S_F23
    , KE_S_F24
    , KE_S_F25
    , KE_S_F26
    , KE_S_F27
    , KE_S_F28
    , KE_S_F29
    , KE_S_F30

    , KE_S_F31
    , KE_S_F32
    , KE_S_F33
    , KE_S_F34
    , KE_S_F35
    , KE_S_F36
    , KE_S_F37

    , KE_MOUSE          /* mouse event start */

/*
 * Symbols for pseudo keys which are translated from the real key symbols above.
 */
    , KE_LEFTMOUSE      /* Left mouse button click */
    , KE_LEFTDRAG       /* Drag with left mouse button down */
    , KE_LEFTRELEASE    /* Left mouse button release */
    , KE_MIDDLEMOUSE    /* Middle mouse button click */
    , KE_MIDDLEDRAG     /* Drag with middle mouse button down */
    , KE_MIDDLERELEASE  /* Middle mouse button release */
    , KE_RIGHTMOUSE     /* Right mouse button click */
    , KE_RIGHTDRAG      /* Drag with right mouse button down */
    , KE_RIGHTRELEASE   /* Right mouse button release */

    , KE_IGNORE         /* Ignored mouse drag/release */

    , KE_TAB            /* unshifted TAB key */
    , KE_S_TAB_OLD      /* shifted TAB key (no longer used) */

    , KE_SNIFF          /* SNiFF+ input waiting */

    , KE_XF1            /* extra vt100 function keys for xterm */
    , KE_XF2
    , KE_XF3
    , KE_XF4
    , KE_XEND           /* extra (vt100) end key for xterm */
    , KE_ZEND           /* extra (vt100) end key for xterm */
    , KE_XHOME          /* extra (vt100) home key for xterm */
    , KE_ZHOME          /* extra (vt100) home key for xterm */
    , KE_XUP            /* extra vt100 cursor keys for xterm */
    , KE_XDOWN
    , KE_XLEFT
    , KE_XRIGHT

    , KE_LEFTMOUSE_NM   /* non-mappable Left mouse button click */
    , KE_LEFTRELEASE_NM /* non-mappable left mouse button release */

    , KE_S_XF1          /* extra vt100 shifted function keys for xterm */
    , KE_S_XF2
    , KE_S_XF3
    , KE_S_XF4

    /* NOTE: The scroll wheel events are inverted: i.e. UP is the same as
     * moving the actual scroll wheel down, LEFT is the same as moving the
     * scroll wheel right. */
    , KE_MOUSEDOWN      /* scroll wheel pseudo-button Down */
    , KE_MOUSEUP        /* scroll wheel pseudo-button Up */
    , KE_MOUSELEFT      /* scroll wheel pseudo-button Left */
    , KE_MOUSERIGHT     /* scroll wheel pseudo-button Right */

    , KE_KINS           /* keypad Insert key */
    , KE_KDEL           /* keypad Delete key */

    , KE_CSI            /* CSI typed directly */
    , KE_SNR            /* <SNR> */
    , KE_PLUG           /* <Plug> */
    , KE_CMDWIN         /* open command-line window from Command-line Mode */

    , KE_C_LEFT         /* control-left */
    , KE_C_RIGHT        /* control-right */
    , KE_C_HOME         /* control-home */
    , KE_C_END          /* control-end */

    , KE_X1MOUSE        /* X1/X2 mouse-buttons */
    , KE_X1DRAG
    , KE_X1RELEASE
    , KE_X2MOUSE
    , KE_X2DRAG
    , KE_X2RELEASE

    , KE_DROP           /* DnD data is available */
    , KE_CURSORHOLD     /* CursorHold event */
    , KE_NOP            /* doesn't do something */
    , KE_FOCUSGAINED    /* focus gained */
    , KE_FOCUSLOST      /* focus lost */
};

/*
 * the three byte codes are replaced with the following int when using vgetc()
 */
#define K_ZERO          TERMCAP2KEY(KS_ZERO, KE_FILLER)

#define K_UP            TERMCAP2KEY('k', 'u')
#define K_DOWN          TERMCAP2KEY('k', 'd')
#define K_LEFT          TERMCAP2KEY('k', 'l')
#define K_RIGHT         TERMCAP2KEY('k', 'r')
#define K_S_UP          TERMCAP2KEY(KS_EXTRA, KE_S_UP)
#define K_S_DOWN        TERMCAP2KEY(KS_EXTRA, KE_S_DOWN)
#define K_S_LEFT        TERMCAP2KEY('#', '4')
#define K_C_LEFT        TERMCAP2KEY(KS_EXTRA, KE_C_LEFT)
#define K_S_RIGHT       TERMCAP2KEY('%', 'i')
#define K_C_RIGHT       TERMCAP2KEY(KS_EXTRA, KE_C_RIGHT)
#define K_S_HOME        TERMCAP2KEY('#', '2')
#define K_C_HOME        TERMCAP2KEY(KS_EXTRA, KE_C_HOME)
#define K_S_END         TERMCAP2KEY('*', '7')
#define K_C_END         TERMCAP2KEY(KS_EXTRA, KE_C_END)
#define K_TAB           TERMCAP2KEY(KS_EXTRA, KE_TAB)
#define K_S_TAB         TERMCAP2KEY('k', 'B')

/* extra set of function keys F1-F4, for vt100 compatible xterm */
#define K_XF1           TERMCAP2KEY(KS_EXTRA, KE_XF1)
#define K_XF2           TERMCAP2KEY(KS_EXTRA, KE_XF2)
#define K_XF3           TERMCAP2KEY(KS_EXTRA, KE_XF3)
#define K_XF4           TERMCAP2KEY(KS_EXTRA, KE_XF4)

/* extra set of cursor keys for vt100 compatible xterm */
#define K_XUP           TERMCAP2KEY(KS_EXTRA, KE_XUP)
#define K_XDOWN         TERMCAP2KEY(KS_EXTRA, KE_XDOWN)
#define K_XLEFT         TERMCAP2KEY(KS_EXTRA, KE_XLEFT)
#define K_XRIGHT        TERMCAP2KEY(KS_EXTRA, KE_XRIGHT)

#define K_F1            TERMCAP2KEY('k', '1')   /* function keys */
#define K_F2            TERMCAP2KEY('k', '2')
#define K_F3            TERMCAP2KEY('k', '3')
#define K_F4            TERMCAP2KEY('k', '4')
#define K_F5            TERMCAP2KEY('k', '5')
#define K_F6            TERMCAP2KEY('k', '6')
#define K_F7            TERMCAP2KEY('k', '7')
#define K_F8            TERMCAP2KEY('k', '8')
#define K_F9            TERMCAP2KEY('k', '9')
#define K_F10           TERMCAP2KEY('k', ';')

#define K_F11           TERMCAP2KEY('F', '1')
#define K_F12           TERMCAP2KEY('F', '2')
#define K_F13           TERMCAP2KEY('F', '3')
#define K_F14           TERMCAP2KEY('F', '4')
#define K_F15           TERMCAP2KEY('F', '5')
#define K_F16           TERMCAP2KEY('F', '6')
#define K_F17           TERMCAP2KEY('F', '7')
#define K_F18           TERMCAP2KEY('F', '8')
#define K_F19           TERMCAP2KEY('F', '9')
#define K_F20           TERMCAP2KEY('F', 'A')

#define K_F21           TERMCAP2KEY('F', 'B')
#define K_F22           TERMCAP2KEY('F', 'C')
#define K_F23           TERMCAP2KEY('F', 'D')
#define K_F24           TERMCAP2KEY('F', 'E')
#define K_F25           TERMCAP2KEY('F', 'F')
#define K_F26           TERMCAP2KEY('F', 'G')
#define K_F27           TERMCAP2KEY('F', 'H')
#define K_F28           TERMCAP2KEY('F', 'I')
#define K_F29           TERMCAP2KEY('F', 'J')
#define K_F30           TERMCAP2KEY('F', 'K')

#define K_F31           TERMCAP2KEY('F', 'L')
#define K_F32           TERMCAP2KEY('F', 'M')
#define K_F33           TERMCAP2KEY('F', 'N')
#define K_F34           TERMCAP2KEY('F', 'O')
#define K_F35           TERMCAP2KEY('F', 'P')
#define K_F36           TERMCAP2KEY('F', 'Q')
#define K_F37           TERMCAP2KEY('F', 'R')

/* extra set of shifted function keys F1-F4, for vt100 compatible xterm */
#define K_S_XF1         TERMCAP2KEY(KS_EXTRA, KE_S_XF1)
#define K_S_XF2         TERMCAP2KEY(KS_EXTRA, KE_S_XF2)
#define K_S_XF3         TERMCAP2KEY(KS_EXTRA, KE_S_XF3)
#define K_S_XF4         TERMCAP2KEY(KS_EXTRA, KE_S_XF4)

#define K_S_F1          TERMCAP2KEY(KS_EXTRA, KE_S_F1)  /* shifted func. keys */
#define K_S_F2          TERMCAP2KEY(KS_EXTRA, KE_S_F2)
#define K_S_F3          TERMCAP2KEY(KS_EXTRA, KE_S_F3)
#define K_S_F4          TERMCAP2KEY(KS_EXTRA, KE_S_F4)
#define K_S_F5          TERMCAP2KEY(KS_EXTRA, KE_S_F5)
#define K_S_F6          TERMCAP2KEY(KS_EXTRA, KE_S_F6)
#define K_S_F7          TERMCAP2KEY(KS_EXTRA, KE_S_F7)
#define K_S_F8          TERMCAP2KEY(KS_EXTRA, KE_S_F8)
#define K_S_F9          TERMCAP2KEY(KS_EXTRA, KE_S_F9)
#define K_S_F10         TERMCAP2KEY(KS_EXTRA, KE_S_F10)

#define K_S_F11         TERMCAP2KEY(KS_EXTRA, KE_S_F11)
#define K_S_F12         TERMCAP2KEY(KS_EXTRA, KE_S_F12)
/* K_S_F13 to K_S_F37  are currently not used */

#define K_HELP          TERMCAP2KEY('%', '1')
#define K_UNDO          TERMCAP2KEY('&', '8')

#define K_BS            TERMCAP2KEY('k', 'b')

#define K_INS           TERMCAP2KEY('k', 'I')
#define K_KINS          TERMCAP2KEY(KS_EXTRA, KE_KINS)
#define K_DEL           TERMCAP2KEY('k', 'D')
#define K_KDEL          TERMCAP2KEY(KS_EXTRA, KE_KDEL)
#define K_HOME          TERMCAP2KEY('k', 'h')
#define K_KHOME         TERMCAP2KEY('K', '1')   /* keypad home (upper left) */
#define K_XHOME         TERMCAP2KEY(KS_EXTRA, KE_XHOME)
#define K_ZHOME         TERMCAP2KEY(KS_EXTRA, KE_ZHOME)
#define K_END           TERMCAP2KEY('@', '7')
#define K_KEND          TERMCAP2KEY('K', '4')   /* keypad end (lower left) */
#define K_XEND          TERMCAP2KEY(KS_EXTRA, KE_XEND)
#define K_ZEND          TERMCAP2KEY(KS_EXTRA, KE_ZEND)
#define K_PAGEUP        TERMCAP2KEY('k', 'P')
#define K_PAGEDOWN      TERMCAP2KEY('k', 'N')
#define K_KPAGEUP       TERMCAP2KEY('K', '3')   /* keypad pageup (upper R.) */
#define K_KPAGEDOWN     TERMCAP2KEY('K', '5')   /* keypad pagedown (lower R.) */

#define K_KPLUS         TERMCAP2KEY('K', '6')   /* keypad plus */
#define K_KMINUS        TERMCAP2KEY('K', '7')   /* keypad minus */
#define K_KDIVIDE       TERMCAP2KEY('K', '8')   /* keypad / */
#define K_KMULTIPLY     TERMCAP2KEY('K', '9')   /* keypad * */
#define K_KENTER        TERMCAP2KEY('K', 'A')   /* keypad Enter */
#define K_KPOINT        TERMCAP2KEY('K', 'B')   /* keypad . or , */

#define K_K0            TERMCAP2KEY('K', 'C')   /* keypad 0 */
#define K_K1            TERMCAP2KEY('K', 'D')   /* keypad 1 */
#define K_K2            TERMCAP2KEY('K', 'E')   /* keypad 2 */
#define K_K3            TERMCAP2KEY('K', 'F')   /* keypad 3 */
#define K_K4            TERMCAP2KEY('K', 'G')   /* keypad 4 */
#define K_K5            TERMCAP2KEY('K', 'H')   /* keypad 5 */
#define K_K6            TERMCAP2KEY('K', 'I')   /* keypad 6 */
#define K_K7            TERMCAP2KEY('K', 'J')   /* keypad 7 */
#define K_K8            TERMCAP2KEY('K', 'K')   /* keypad 8 */
#define K_K9            TERMCAP2KEY('K', 'L')   /* keypad 9 */

#define K_MOUSE         TERMCAP2KEY(KS_MOUSE, KE_FILLER)
#define K_MENU          TERMCAP2KEY(KS_MENU, KE_FILLER)
#define K_VER_SCROLLBAR TERMCAP2KEY(KS_VER_SCROLLBAR, KE_FILLER)
#define K_HOR_SCROLLBAR   TERMCAP2KEY(KS_HOR_SCROLLBAR, KE_FILLER)

#define K_NETTERM_MOUSE TERMCAP2KEY(KS_NETTERM_MOUSE, KE_FILLER)
#define K_DEC_MOUSE     TERMCAP2KEY(KS_DEC_MOUSE, KE_FILLER)
#define K_JSBTERM_MOUSE TERMCAP2KEY(KS_JSBTERM_MOUSE, KE_FILLER)
#define K_PTERM_MOUSE   TERMCAP2KEY(KS_PTERM_MOUSE, KE_FILLER)
#define K_URXVT_MOUSE   TERMCAP2KEY(KS_URXVT_MOUSE, KE_FILLER)
#define K_SGR_MOUSE     TERMCAP2KEY(KS_SGR_MOUSE, KE_FILLER)

#define K_SELECT        TERMCAP2KEY(KS_SELECT, KE_FILLER)
#define K_TEAROFF       TERMCAP2KEY(KS_TEAROFF, KE_FILLER)

#define K_TABLINE       TERMCAP2KEY(KS_TABLINE, KE_FILLER)
#define K_TABMENU       TERMCAP2KEY(KS_TABMENU, KE_FILLER)

/*
 * Symbols for pseudo keys which are translated from the real key symbols above.
 */
#define K_LEFTMOUSE     TERMCAP2KEY(KS_EXTRA, KE_LEFTMOUSE)
#define K_LEFTMOUSE_NM  TERMCAP2KEY(KS_EXTRA, KE_LEFTMOUSE_NM)
#define K_LEFTDRAG      TERMCAP2KEY(KS_EXTRA, KE_LEFTDRAG)
#define K_LEFTRELEASE   TERMCAP2KEY(KS_EXTRA, KE_LEFTRELEASE)
#define K_LEFTRELEASE_NM TERMCAP2KEY(KS_EXTRA, KE_LEFTRELEASE_NM)
#define K_MIDDLEMOUSE   TERMCAP2KEY(KS_EXTRA, KE_MIDDLEMOUSE)
#define K_MIDDLEDRAG    TERMCAP2KEY(KS_EXTRA, KE_MIDDLEDRAG)
#define K_MIDDLERELEASE TERMCAP2KEY(KS_EXTRA, KE_MIDDLERELEASE)
#define K_RIGHTMOUSE    TERMCAP2KEY(KS_EXTRA, KE_RIGHTMOUSE)
#define K_RIGHTDRAG     TERMCAP2KEY(KS_EXTRA, KE_RIGHTDRAG)
#define K_RIGHTRELEASE  TERMCAP2KEY(KS_EXTRA, KE_RIGHTRELEASE)
#define K_X1MOUSE       TERMCAP2KEY(KS_EXTRA, KE_X1MOUSE)
#define K_X1MOUSE       TERMCAP2KEY(KS_EXTRA, KE_X1MOUSE)
#define K_X1DRAG        TERMCAP2KEY(KS_EXTRA, KE_X1DRAG)
#define K_X1RELEASE     TERMCAP2KEY(KS_EXTRA, KE_X1RELEASE)
#define K_X2MOUSE       TERMCAP2KEY(KS_EXTRA, KE_X2MOUSE)
#define K_X2DRAG        TERMCAP2KEY(KS_EXTRA, KE_X2DRAG)
#define K_X2RELEASE     TERMCAP2KEY(KS_EXTRA, KE_X2RELEASE)

#define K_IGNORE        TERMCAP2KEY(KS_EXTRA, KE_IGNORE)
#define K_NOP           TERMCAP2KEY(KS_EXTRA, KE_NOP)

#define K_SNIFF         TERMCAP2KEY(KS_EXTRA, KE_SNIFF)

#define K_MOUSEDOWN     TERMCAP2KEY(KS_EXTRA, KE_MOUSEDOWN)
#define K_MOUSEUP       TERMCAP2KEY(KS_EXTRA, KE_MOUSEUP)
#define K_MOUSELEFT     TERMCAP2KEY(KS_EXTRA, KE_MOUSELEFT)
#define K_MOUSERIGHT    TERMCAP2KEY(KS_EXTRA, KE_MOUSERIGHT)

#define K_CSI           TERMCAP2KEY(KS_EXTRA, KE_CSI)
#define K_SNR           TERMCAP2KEY(KS_EXTRA, KE_SNR)
#define K_PLUG          TERMCAP2KEY(KS_EXTRA, KE_PLUG)
#define K_CMDWIN        TERMCAP2KEY(KS_EXTRA, KE_CMDWIN)

#define K_DROP          TERMCAP2KEY(KS_EXTRA, KE_DROP)
#define K_FOCUSGAINED   TERMCAP2KEY(KS_EXTRA, KE_FOCUSGAINED)
#define K_FOCUSLOST     TERMCAP2KEY(KS_EXTRA, KE_FOCUSLOST)

#define K_CURSORHOLD    TERMCAP2KEY(KS_EXTRA, KE_CURSORHOLD)

/* Bits for modifier mask */
/* 0x01 cannot be used, because the modifier must be 0x02 or higher */
#define MOD_MASK_SHIFT      0x02
#define MOD_MASK_CTRL       0x04
#define MOD_MASK_ALT        0x08        /* aka META */
#define MOD_MASK_META       0x10        /* META when it's different from ALT */
#define MOD_MASK_2CLICK     0x20        /* use MOD_MASK_MULTI_CLICK */
#define MOD_MASK_3CLICK     0x40        /* use MOD_MASK_MULTI_CLICK */
#define MOD_MASK_4CLICK     0x60        /* use MOD_MASK_MULTI_CLICK */

#define MOD_MASK_MULTI_CLICK    (MOD_MASK_2CLICK|MOD_MASK_3CLICK|MOD_MASK_4CLICK)

/*
 * The length of the longest special key name, including modifiers.
 * Current longest is <M-C-S-T-4-MiddleRelease> (length includes '<' and '>').
 */
#define MAX_KEY_NAME_LEN    25

/* Maximum length of a special key event as tokens.  This includes modifiers.
 * The longest event is something like <M-C-S-T-4-LeftDrag> which would be the
 * following string of tokens:
 *
 * <K_SPECIAL> <KS_MODIFIER> bitmask <K_SPECIAL> <KS_EXTRA> <KT_LEFTDRAG>.
 *
 * This is a total of 6 tokens, and is currently the longest one possible.
 */
#define MAX_KEY_CODE_LEN    6

/* ----------------------------------------------------------------------- */

/* #include "term.h" */
/*
 * This file contains the defines for the machine dependent escape sequences
 * that the editor needs to perform various operations. All of the sequences
 * here are optional, except "cm" (cursor motion).
 */

/*
 * Index of the termcap codes in the term_strings array.
 */
enum SpecialKey
{
    KS_NAME = 0,/* name of this terminal entry */
    KS_CE,      /* clear to end of line */
    KS_AL,      /* add new blank line */
    KS_CAL,     /* add number of blank lines */
    KS_DL,      /* delete line */
    KS_CDL,     /* delete number of lines */
    KS_CS,      /* scroll region */
    KS_CL,      /* clear screen */
    KS_CD,      /* clear to end of display */
    KS_UT,      /* clearing uses current background color */
    KS_DA,      /* text may be scrolled down from up */
    KS_DB,      /* text may be scrolled up from down */
    KS_VI,      /* cursor invisible */
    KS_VE,      /* cursor visible */
    KS_VS,      /* cursor very visible */
    KS_ME,      /* normal mode */
    KS_MR,      /* reverse mode */
    KS_MD,      /* bold mode */
    KS_SE,      /* normal mode */
    KS_SO,      /* standout mode */
    KS_CZH,     /* italic mode start */
    KS_CZR,     /* italic mode end */
    KS_UE,      /* exit underscore (underline) mode */
    KS_US,      /* underscore (underline) mode */
    KS_UCE,     /* exit undercurl mode */
    KS_UCS,     /* undercurl mode */
    KS_MS,      /* save to move cur in reverse mode */
    KS_CM,      /* cursor motion */
    KS_SR,      /* scroll reverse (backward) */
    KS_CRI,     /* cursor number of chars right */
    KS_VB,      /* visual bell */
    KS_KS,      /* put term in "keypad transmit" mode */
    KS_KE,      /* out of "keypad transmit" mode */
    KS_TI,      /* put terminal in termcap mode */
    KS_TE,      /* out of termcap mode */
    KS_BC,      /* backspace character (cursor left) */
    KS_CCS,     /* cur is relative to scroll region */
    KS_CCO,     /* number of colors */
    KS_CSF,     /* set foreground color */
    KS_CSB,     /* set background color */
    KS_XS,      /* standout not erased by overwriting (hpterm) */
    KS_XN,      /* newline glitch */
    KS_MB,      /* blink mode */
    KS_CAF,     /* set foreground color (ANSI) */
    KS_CAB,     /* set background color (ANSI) */
    KS_LE,      /* cursor left (mostly backspace) */
    KS_ND,      /* cursor right */
    KS_CIS,     /* set icon text start */
    KS_CIE,     /* set icon text end */
    KS_TS,      /* set window title start (to status line) */
    KS_FS,      /* set window title end (from status line) */
    KS_CWP,     /* set window position in pixels */
    KS_CWS,     /* set window size in characters */
    KS_CRV,     /* request version string */
    KS_CSI,     /* start insert mode (bar cursor) */
    KS_CEI,     /* end insert mode (block cursor) */
    KS_CSR,     /* start replace mode (underline cursor) */
    KS_CSV,     /* scroll region vertical */
    KS_OP,      /* original color pair */
    KS_U7       /* request cursor position */
};

#define KS_LAST     KS_U7

/*
 * the terminal capabilities are stored in this array
 * IMPORTANT: When making changes, note the following:
 * - there should be an entry for each code in the builtin termcaps
 * - there should be an option for each code in option.c
 * - there should be code in term.c to obtain the value from the termcap
 */

extern char_u *(term_strings[]);    /* current terminal strings */

/*
 * strings used for terminal
 */
#define T_NAME  (term_str(KS_NAME))     /* terminal name */
#define T_CE    (term_str(KS_CE))       /* clear to end of line */
#define T_AL    (term_str(KS_AL))       /* add new blank line */
#define T_CAL   (term_str(KS_CAL))      /* add number of blank lines */
#define T_DL    (term_str(KS_DL))       /* delete line */
#define T_CDL   (term_str(KS_CDL))      /* delete number of lines */
#define T_CS    (term_str(KS_CS))       /* scroll region */
#define T_CSV   (term_str(KS_CSV))      /* scroll region vertical */
#define T_CL    (term_str(KS_CL))       /* clear screen */
#define T_CD    (term_str(KS_CD))       /* clear to end of display */
#define T_UT    (term_str(KS_UT))       /* clearing uses background color */
#define T_DA    (term_str(KS_DA))       /* text may be scrolled down from up */
#define T_DB    (term_str(KS_DB))       /* text may be scrolled up from down */
#define T_VI    (term_str(KS_VI))       /* cursor invisible */
#define T_VE    (term_str(KS_VE))       /* cursor visible */
#define T_VS    (term_str(KS_VS))       /* cursor very visible */
#define T_ME    (term_str(KS_ME))       /* normal mode */
#define T_MR    (term_str(KS_MR))       /* reverse mode */
#define T_MD    (term_str(KS_MD))       /* bold mode */
#define T_SE    (term_str(KS_SE))       /* normal mode */
#define T_SO    (term_str(KS_SO))       /* standout mode */
#define T_CZH   (term_str(KS_CZH))      /* italic mode start */
#define T_CZR   (term_str(KS_CZR))      /* italic mode end */
#define T_UE    (term_str(KS_UE))       /* exit underscore (underline) mode */
#define T_US    (term_str(KS_US))       /* underscore (underline) mode */
#define T_UCE   (term_str(KS_UCE))      /* exit undercurl mode */
#define T_UCS   (term_str(KS_UCS))      /* undercurl mode */
#define T_MS    (term_str(KS_MS))       /* save to move cur in reverse mode */
#define T_CM    (term_str(KS_CM))       /* cursor motion */
#define T_SR    (term_str(KS_SR))       /* scroll reverse (backward) */
#define T_CRI   (term_str(KS_CRI))      /* cursor number of chars right */
#define T_VB    (term_str(KS_VB))       /* visual bell */
#define T_KS    (term_str(KS_KS))       /* put term in "keypad transmit" mode */
#define T_KE    (term_str(KS_KE))       /* out of "keypad transmit" mode */
#define T_TI    (term_str(KS_TI))       /* put terminal in termcap mode */
#define T_TE    (term_str(KS_TE))       /* out of termcap mode */
#define T_BC    (term_str(KS_BC))       /* backspace character */
#define T_CCS   (term_str(KS_CCS))      /* cur is relative to scroll region */
#define T_CCO   (term_str(KS_CCO))      /* number of colors */
#define T_CSF   (term_str(KS_CSF))      /* set foreground color */
#define T_CSB   (term_str(KS_CSB))      /* set background color */
#define T_XS    (term_str(KS_XS))       /* standout not erased by overwriting */
#define T_XN    (term_str(KS_XN))       /* newline glitch */
#define T_MB    (term_str(KS_MB))       /* blink mode */
#define T_CAF   (term_str(KS_CAF))      /* set foreground color (ANSI) */
#define T_CAB   (term_str(KS_CAB))      /* set background color (ANSI) */
#define T_LE    (term_str(KS_LE))       /* cursor left */
#define T_ND    (term_str(KS_ND))       /* cursor right */
#define T_CIS   (term_str(KS_CIS))      /* set icon text start */
#define T_CIE   (term_str(KS_CIE))      /* set icon text end */
#define T_TS    (term_str(KS_TS))       /* set window title start */
#define T_FS    (term_str(KS_FS))       /* set window title end */
#define T_CWP   (term_str(KS_CWP))      /* window position */
#define T_CWS   (term_str(KS_CWS))      /* window size */
#define T_CSI   (term_str(KS_CSI))      /* start insert mode */
#define T_CEI   (term_str(KS_CEI))      /* end insert mode */
#define T_CSR   (term_str(KS_CSR))      /* start replace mode */
#define T_CRV   (term_str(KS_CRV))      /* request version string */
#define T_OP    (term_str(KS_OP))       /* original color pair */
#define T_U7    (term_str(KS_U7))       /* request cursor position */

#define TMODE_COOK  0   /* terminal mode for external cmds and Ex mode */
#define TMODE_SLEEP 1   /* terminal mode for sleeping (cooked but no echo) */
#define TMODE_RAW   2   /* terminal mode for Normal and Insert mode */

/* ----------------------------------------------------------------------- */

/* #include "macros.h" */
/*
 * macros.h: macro definitions for often used code
 */

/*
 * pchar(lp, c) - put character 'c' at position 'lp'
 */
#define pchar(lp, c) (*(ml_get_buf(curbuf, (lp).lnum, TRUE) + (lp).col) = (c))

/*
 * Position comparisons
 */
#define lt(a, b) (((a).lnum != (b).lnum) \
                   ? (a).lnum < (b).lnum \
                   : (a).col != (b).col \
                       ? (a).col < (b).col \
                       : (a).coladd < (b).coladd)
#define ltp(a, b) (((a)->lnum != (b)->lnum) \
                   ? (a)->lnum < (b)->lnum \
                   : (a)->col != (b)->col \
                       ? (a)->col < (b)->col \
                       : (a)->coladd < (b)->coladd)
#define equalpos(a, b) (((a).lnum == (b).lnum) && ((a).col == (b).col) && ((a).coladd == (b).coladd))
#define clearpos(a) {(a)->lnum = 0; (a)->col = 0; (a)->coladd = 0;}

#define ltoreq(a, b) (lt(a, b) || equalpos(a, b))

/*
 * lineempty() - return TRUE if the line is empty
 */
#define lineempty(p) (*ml_get(p) == NUL)

/*
 * bufempty() - return TRUE if the current buffer is empty
 */
#define bufempty() (curbuf->b_ml.ml_line_count == 1 && *ml_get((linenr_T)1) == NUL)

/*
 * toupper() and tolower() that use the current locale.
 * On some systems toupper()/tolower() only work on lower/uppercase
 * characters, first use islower() or isupper() then.
 * Careful: Only call TOUPPER_LOC() and TOLOWER_LOC() with a character in the
 * range 0 - 255.  toupper()/tolower() on some systems can't handle others.
 * Note: It is often better to use MB_TOLOWER() and MB_TOUPPER(), because many
 * toupper() and tolower() implementations only work for ASCII.
 */
#define TOUPPER_LOC           toupper
#define TOLOWER_LOC           tolower

/* toupper() and tolower() for ASCII only and ignore the current locale. */
#define TOUPPER_ASC(c) (((c) < 'a' || (c) > 'z') ? (c) : (c) - ('a' - 'A'))
#define TOLOWER_ASC(c) (((c) < 'A' || (c) > 'Z') ? (c) : (c) + ('a' - 'A'))

/*
 * MB_ISLOWER() and MB_ISUPPER() are to be used on multi-byte characters.  But
 * don't use them for negative values!
 */
#define MB_ISLOWER(c)  vim_islower(c)
#define MB_ISUPPER(c)  vim_isupper(c)
#define MB_TOLOWER(c)  vim_tolower(c)
#define MB_TOUPPER(c)  vim_toupper(c)

/* Use our own isdigit() replacement, because on MS-Windows isdigit() returns
 * non-zero for superscript 1.  Also avoids that isdigit() crashes for numbers
 * below 0 and above 255. */
#define VIM_ISDIGIT(c) ((unsigned)(c) - '0' < 10)

/* Like isalpha() but reject non-ASCII characters.  Can't be used with a
 * special key (negative value). */
#define ASCII_ISLOWER(c) ((unsigned)(c) - 'a' < 26)
#define ASCII_ISUPPER(c) ((unsigned)(c) - 'A' < 26)
#define ASCII_ISALPHA(c) (ASCII_ISUPPER(c) || ASCII_ISLOWER(c))
#define ASCII_ISALNUM(c) (ASCII_ISALPHA(c) || VIM_ISDIGIT(c))

/* macro version of chartab().
 * Only works with values 0-255!
 * Doesn't work for UTF-8 mode with chars >= 0x80. */
#define CHARSIZE(c)     (chartab[c] & CT_CELL_MASK)

#define LANGMAP_ADJUST(c, condition) /* nop */

/*
 * vim_isbreak() is used very often if 'linebreak' is set, use a macro to make it work fast.
 */
#define vim_isbreak(c) (breakat_flags[(char_u)(c)])

/*
 * On VMS file names are different and require a translation.
 * On the Mac open() has only two arguments.
 */
#define mch_access(n, p)     access((n), (p))
#define mch_fopen(n, p)       fopen((n), (p))
#define mch_fstat(n, p)        fstat((n), (p))
#define mch_stat(n, p)       stat((n), (p))

#define mch_lstat(n, p)        lstat((n), (p))

#define mch_open(n, m, p)    open((n), (m), (p))

/* mch_open_rw(): invoke mch_open() with third argument for user R/W. */
#define mch_open_rw(n, f)      mch_open((n), (f), (mode_t)0600)

#define TIME_MSG(s)

#define REPLACE_NORMAL(s) (((s) & REPLACE_FLAG) && !((s) & VREPLACE_FLAG))

#define UTF_COMPOSINGLIKE(p1, p2)  utf_iscomposing(utf_ptr2char(p2))

/* Whether to draw the vertical bar on the right side of the cell. */
#define CURSOR_BAR_RIGHT (curwin->w_p_rl && (!(State & CMDLINE) || cmdmsg_rl))

/*
 * mb_ptr_adv(): advance a pointer to the next character, taking care of
 * multi-byte characters if needed.
 * mb_ptr_back(): backup a pointer to the previous character, taking care of
 * multi-byte characters if needed.
 * MB_COPY_CHAR(f, t): copy one char from "f" to "t" and advance the pointers.
 * PTR2CHAR(): get character from pointer.
 */
/* Get the length of the character p points to */
#define MB_PTR2LEN(p)          (has_mbyte ? (*mb_ptr2len)(p) : 1)
/* Advance multi-byte pointer, skip over composing chars. */
#define mb_ptr_adv(p)      p += has_mbyte ? (*mb_ptr2len)(p) : 1
/* Advance multi-byte pointer, do not skip over composing chars. */
#define mb_cptr_adv(p)     p += enc_utf8 ? utf_ptr2len(p) : has_mbyte ? (*mb_ptr2len)(p) : 1
/* Backup multi-byte pointer. Only use with "p" > "s" ! */
#define mb_ptr_back(s, p)  p -= has_mbyte ? ((*mb_head_off)(s, p - 1) + 1) : 1
/* get length of multi-byte char, not including composing chars */
#define mb_cptr2len(p)     (enc_utf8 ? utf_ptr2len(p) : (*mb_ptr2len)(p))

#define MB_COPY_CHAR(f, t) if (has_mbyte) mb_copy_char(&f, &t); else *t++ = *f++
#define MB_CHARLEN(p)      (has_mbyte ? mb_charlen(p) : (int)STRLEN(p))
#define MB_CHAR2LEN(c)     (has_mbyte ? mb_char2len(c) : 1)
#define PTR2CHAR(p)        (has_mbyte ? mb_ptr2char(p) : (int)*(p))

#define RESET_BINDING(wp)  (wp)->w_p_scb = FALSE; (wp)->w_p_crb = FALSE

/* ----------------------------------------------------------------------- */

#include <errno.h>
#include <assert.h>
#include <stdint.h>
#include <inttypes.h>
#include <wctype.h>
#include <stdarg.h>

#include <sys/select.h>

/* ================ end of the header file puzzle =============== */

/*
 * flags for update_screen()
 * The higher the value, the higher the priority
 */
#define VALID                   10  /* buffer not changed, or changes marked with b_mod_* */
#define INVERTED                20  /* redisplay inverted part that changed */
#define INVERTED_ALL            25  /* redisplay whole inverted part */
#define REDRAW_TOP              30  /* display first w_upd_rows screen lines */
#define SOME_VALID              35  /* like NOT_VALID but may scroll */
#define NOT_VALID               40  /* buffer needs complete redraw */
#define CLEAR                   50  /* screen messed up, clear it */

/*
 * Flags for w_valid.
 * These are set when something in a window structure becomes invalid, except
 * when the cursor is moved.  Call check_cursor_moved() before testing one of the flags.
 * These are reset when that thing has been updated and is valid again.
 *
 * Every function that invalidates one of these must call one of the
 * invalidate_* functions.
 *
 * w_valid is supposed to be used only in screen.c.  From other files, use the
 * functions that set or reset the flags.
 *
 * VALID_BOTLINE    VALID_BOTLINE_AP
 *     on               on              w_botline valid
 *     off              on              w_botline approximated
 *     off              off             w_botline not valid
 *     on               off             not possible
 */
#define VALID_WROW      0x01    /* w_wrow (window row) is valid */
#define VALID_WCOL      0x02    /* w_wcol (window col) is valid */
#define VALID_VIRTCOL   0x04    /* w_virtcol (file col) is valid */
#define VALID_CHEIGHT   0x08    /* w_cline_height and w_cline_folded valid */
#define VALID_CROW      0x10    /* w_cline_row is valid */
#define VALID_BOTLINE   0x20    /* w_botine and w_empty_rows are valid */
#define VALID_BOTLINE_AP 0x40   /* w_botine is approximated */
#define VALID_TOPLINE   0x80    /* w_topline is valid (for cursor position) */

/*
 * Terminal highlighting attribute bits.
 * Attributes above HL_ALL are used for syntax highlighting.
 */
#define HL_NORMAL               0x00
#define HL_INVERSE              0x01
#define HL_BOLD                 0x02
#define HL_ITALIC               0x04
#define HL_UNDERLINE            0x08
#define HL_UNDERCURL            0x10
#define HL_STANDOUT             0x20
#define HL_ALL                  0x3f

/* special attribute addition: Put message in history */
#define MSG_HIST                0x1000

/*
 * values for State
 *
 * The lower bits up to 0x20 are used to distinguish normal/visual/op_pending
 * and cmdline/insert+replace mode.  This is used for mapping.  If none of
 * these bits are set, no mapping is done.
 * The upper bits are used to distinguish between other states.
 */
#define NORMAL          0x01    /* Normal mode, command expected */
#define VISUAL          0x02    /* Visual mode - use get_real_state() */
#define OP_PENDING      0x04    /* Normal mode, operator is pending - use
                                   get_real_state() */
#define CMDLINE         0x08    /* Editing command line */
#define INSERT          0x10    /* Insert mode */
#define LANGMAP         0x20    /* Language mapping, can be combined with
                                   INSERT and CMDLINE */

#define REPLACE_FLAG    0x40    /* Replace mode flag */
#define REPLACE         (REPLACE_FLAG + INSERT)
#define VREPLACE_FLAG  0x80    /* Virtual-replace mode flag */
#define VREPLACE       (REPLACE_FLAG + VREPLACE_FLAG + INSERT)
#define LREPLACE        (REPLACE_FLAG + LANGMAP)

#define NORMAL_BUSY     (0x100 + NORMAL) /* Normal mode, busy with a command */
#define HITRETURN       (0x200 + NORMAL) /* waiting for return or command */
#define ASKMORE         0x300   /* Asking if you want --more-- */
#define SETWSIZE        0x400   /* window size has changed */
#define ABBREV          0x500   /* abbreviation instead of mapping */
#define EXTERNCMD       0x600   /* executing an external command */
#define SHOWMATCH       (0x700 + INSERT) /* show matching paren */
#define CONFIRM         0x800   /* ":confirm" prompt */
#define SELECTMODE      0x1000  /* Select mode, only for mappings */

#define MAP_ALL_MODES   (0x3f | SELECTMODE)     /* all mode bits used for mapping */

/* directions */
#define FORWARD                 1
#define BACKWARD                (-1)
#define FORWARD_FILE            3
#define BACKWARD_FILE           (-3)

/* return values for functions */
#define OK                      1
#define FAIL                    0
#define NOTDONE                 2   /* not OK or FAIL but skipped */

/* flags for b_flags */
#define BF_RECOVERED    0x01    /* buffer has been recovered */
#define BF_CHECK_RO     0x02    /* need to check readonly when loading file
                                   into buffer (set by ":e", may be reset by
                                   ":buf" */
#define BF_NEVERLOADED  0x04    /* file has never been loaded into buffer,
                                   many variables still need to be set */
#define BF_NOTEDITED    0x08    /* Set when file name is changed after
                                   starting to edit, reset when file is
                                   written out. */
#define BF_NEW          0x10    /* file didn't exist when editing started */
#define BF_NEW_W        0x20    /* Warned for BF_NEW and file created */
#define BF_READERR      0x40    /* got errors while reading the file */
#define BF_DUMMY        0x80    /* dummy buffer, only used internally */
#define BF_PRESERVED    0x100   /* ":preserve" was used */

/* Mask to check for flags that prevent normal writing */
#define BF_WRITE_MASK   (BF_NOTEDITED + BF_NEW + BF_READERR)

/*
 * values for xp_context when doing command line completion
 */
#define EXPAND_UNSUCCESSFUL     (-2)
#define EXPAND_OK               (-1)
#define EXPAND_NOTHING          0
#define EXPAND_COMMANDS         1
#define EXPAND_FILES            2
#define EXPAND_DIRECTORIES      3
#define EXPAND_SETTINGS         4
#define EXPAND_BOOL_SETTINGS    5
#define EXPAND_OLD_SETTING      7
#define EXPAND_BUFFERS          9
#define EXPAND_EVENTS           10
#define EXPAND_SYNTAX           12
#define EXPAND_HIGHLIGHT        13
#define EXPAND_AUGROUP          14
#define EXPAND_USER_VARS        15
#define EXPAND_MAPPINGS         16
#define EXPAND_FUNCTIONS        18
#define EXPAND_USER_FUNC        19
#define EXPAND_EXPRESSION       20
#define EXPAND_USER_COMMANDS    22
#define EXPAND_USER_CMD_FLAGS   23
#define EXPAND_USER_NARGS       24
#define EXPAND_USER_COMPLETE    25
#define EXPAND_ENV_VARS         26
#define EXPAND_COLORS           28
#define EXPAND_COMPILER         29
#define EXPAND_USER_DEFINED     30
#define EXPAND_USER_LIST        31
#define EXPAND_SHELLCMD         32
#define EXPAND_BEHAVE           36
#define EXPAND_FILETYPE         37
#define EXPAND_FILES_IN_PATH    38
#define EXPAND_OWNSYNTAX        39
#define EXPAND_HISTORY          41
#define EXPAND_USER             42
#define EXPAND_SYNTIME          43
#define EXPAND_USER_ADDR_TYPE   44

/* Values for exmode_active (0 is no exmode) */
#define EXMODE_NORMAL           1
#define EXMODE_VIM              2

/* Values for nextwild() and ExpandOne().  See ExpandOne() for meaning. */
#define WILD_FREE               1
#define WILD_EXPAND_FREE        2
#define WILD_EXPAND_KEEP        3
#define WILD_NEXT               4
#define WILD_PREV               5
#define WILD_ALL                6
#define WILD_LONGEST            7
#define WILD_ALL_KEEP           8

#define WILD_LIST_NOTFOUND      0x01
#define WILD_HOME_REPLACE       0x02
#define WILD_USE_NL             0x04
#define WILD_NO_BEEP            0x08
#define WILD_ADD_SLASH          0x10
#define WILD_KEEP_ALL           0x20
#define WILD_SILENT             0x40
#define WILD_ESCAPE             0x80
#define WILD_ICASE              0x100
#define WILD_ALLLINKS           0x200

/* Flags for expand_wildcards() */
#define EW_DIR          0x01    /* include directory names */
#define EW_FILE         0x02    /* include file names */
#define EW_NOTFOUND     0x04    /* include not found names */
#define EW_ADDSLASH     0x08    /* append slash to directory name */
#define EW_KEEPALL      0x10    /* keep all matches */
#define EW_SILENT       0x20    /* don't print "1 returned" from shell */
#define EW_EXEC         0x40    /* executable files */
#define EW_PATH         0x80    /* search in 'path' too */
#define EW_ICASE        0x100   /* ignore case */
#define EW_NOERROR      0x200   /* no error for bad regexp */
#define EW_NOTWILD      0x400   /* add match with literal name if exists */
#define EW_KEEPDOLLAR   0x800   /* do not escape $, $var is expanded */
/* Note: mostly EW_NOTFOUND and EW_SILENT are mutually exclusive: EW_NOTFOUND
 * is used when executing commands and EW_SILENT for interactive expanding. */
#define EW_ALLLINKS     0x1000  /* also links not pointing to existing file */
#define EW_SHELLCMD     0x2000  /* called from expand_shellcmd(), don't check
                                 * if executable is in $PATH */

#define W_WINCOL(wp)   (wp->w_wincol)
#define W_WIDTH(wp)    (wp->w_width)
#define W_ENDCOL(wp)   (wp->w_wincol + wp->w_width)
#define W_VSEP_WIDTH(wp) (wp->w_vsep_width)
#define W_STATUS_HEIGHT(wp) (wp->w_status_height)
#define W_WINROW(wp)   (wp->w_winrow)

/* Values for the find_pattern_in_path() function args 'type' and 'action': */
#define FIND_ANY        1
#define FIND_DEFINE     2
#define CHECK_PATH      3

#define ACTION_SHOW     1
#define ACTION_GOTO     2
#define ACTION_SPLIT    3
#define ACTION_SHOW_ALL 4

#define SST_MIN_ENTRIES 150    /* minimal size for state stack array */
#define SST_MAX_ENTRIES 1000  /* maximal size for state stack array */
#define SST_FIX_STATES  7      /* size of sst_stack[]. */
#define SST_DIST        16     /* normal distance between entries */
#define SST_INVALID    (synstate_T *)-1        /* invalid syn_state pointer */

#define HL_CONTAINED   0x01    /* not used on toplevel */
#define HL_TRANSP      0x02    /* has no highlighting */
#define HL_ONELINE     0x04    /* match within one line only */
#define HL_HAS_EOL     0x08    /* end pattern that matches with $ */
#define HL_SYNC_HERE   0x10    /* sync point after this item (syncing only) */
#define HL_SYNC_THERE  0x20    /* sync point at current line (syncing only) */
#define HL_MATCH       0x40    /* use match ID instead of item ID */
#define HL_SKIPNL      0x80    /* nextgroup can skip newlines */
#define HL_SKIPWHITE   0x100   /* nextgroup can skip white space */
#define HL_SKIPEMPTY   0x200   /* nextgroup can skip empty lines */
#define HL_KEEPEND     0x400   /* end match always kept */
#define HL_EXCLUDENL   0x800   /* exclude NL from match */
#define HL_DISPLAY     0x1000  /* only used for displaying, not syncing */
#define HL_FOLD        0x2000  /* define fold */
#define HL_EXTEND      0x4000  /* ignore a keepend */
#define HL_MATCHCONT   0x8000  /* match continued from previous line */
#define HL_TRANS_CONT  0x10000 /* transparent item without contains arg */
#define HL_CONCEAL     0x20000 /* can be concealed */
#define HL_CONCEALENDS 0x40000 /* can be concealed */

/* Values for 'options' argument in do_search() and searchit() */
#define SEARCH_REV    0x01  /* go in reverse of previous dir. */
#define SEARCH_ECHO   0x02  /* echo the search command and handle options */
#define SEARCH_MSG    0x0c  /* give messages (yes, it's not 0x04) */
#define SEARCH_NFMSG  0x08  /* give all messages except not found */
#define SEARCH_OPT    0x10  /* interpret optional flags */
#define SEARCH_HIS    0x20  /* put search pattern in history */
#define SEARCH_END    0x40  /* put cursor at end of match */
#define SEARCH_NOOF   0x80  /* don't add offset to position */
#define SEARCH_START 0x100  /* start search without col offset */
#define SEARCH_MARK  0x200  /* set previous context mark */
#define SEARCH_KEEP  0x400  /* keep previous search pattern */
#define SEARCH_PEEK  0x800  /* peek for typed char, cancel search */

/* Values for find_ident_under_cursor() */
#define FIND_IDENT      1       /* find identifier (word) */
#define FIND_STRING     2       /* find any string (WORD) */
#define FIND_EVAL       4       /* include "->", "[]" and "." */

/* Values for file_name_in_line() */
#define FNAME_MESS      1       /* give error message */
#define FNAME_EXP       2       /* expand to path */
#define FNAME_HYP       4       /* check for hypertext link */
#define FNAME_INCL      8       /* apply 'includeexpr' */
#define FNAME_REL       16      /* ".." and "./" are relative to the (current)
                                   file instead of the current directory */
#define FNAME_UNESC     32      /* remove backslashes used for escaping */

/* Values for buflist_getfile() */
#define GETF_SETMARK    0x01    /* set pcmark before jumping */
#define GETF_ALT        0x02    /* jumping to alternate file (not buf num) */
#define GETF_SWITCH     0x04    /* respect 'switchbuf' settings when jumping */

/* Values for buflist_new() flags */
#define BLN_CURBUF      1       /* May re-use curbuf for new buffer */
#define BLN_LISTED      2       /* Put new buffer in buffer list */
#define BLN_DUMMY       4       /* Allocating dummy buffer */

/* Values for in_cinkeys() */
#define KEY_OPEN_FORW   0x101
#define KEY_OPEN_BACK   0x102
#define KEY_COMPLETE    0x103   /* end of completion */

/* Values for "noremap" argument of ins_typebuf().  Also used for
 * map->m_noremap and menu->noremap[]. */
#define REMAP_YES       0       /* allow remapping */
#define REMAP_NONE      -1      /* no remapping */
#define REMAP_SCRIPT    -2      /* remap script-local mappings only */
#define REMAP_SKIP      -3      /* no remapping for first char */

/* Values for mch_call_shell() second argument */
#define SHELL_FILTER    1       /* filtering text */
#define SHELL_EXPAND    2       /* expanding wildcards */
#define SHELL_COOKED    4       /* set term to cooked mode */
#define SHELL_DOOUT     8       /* redirecting output */
#define SHELL_SILENT    16      /* don't print error returned by command */
#define SHELL_READ      32      /* read lines and insert into buffer */
#define SHELL_WRITE     64      /* write lines from buffer */

/* Values returned by mch_nodetype() */
#define NODE_NORMAL     0       /* file or directory, check with mch_isdir() */
#define NODE_WRITABLE   1       /* something we can write to (character
                                   device, fifo, socket, ..) */
#define NODE_OTHER      2       /* non-writable thing (e.g., block device) */

/* Values for readfile() flags */
#define READ_NEW        0x01    /* read a file into a new buffer */
#define READ_FILTER     0x02    /* read filter output */
#define READ_STDIN      0x04    /* read from stdin */
#define READ_BUFFER     0x08    /* read from curbuf (converting stdin) */
#define READ_DUMMY      0x10    /* reading into a dummy buffer */
#define READ_KEEP_UNDO  0x20    /* keep undo info */

/* Values for change_indent() */
#define INDENT_SET      1       /* set indent */
#define INDENT_INC      2       /* increase indent */
#define INDENT_DEC      3       /* decrease indent */

/* Values for flags argument for findmatchlimit() */
#define FM_BACKWARD     0x01    /* search backwards */
#define FM_FORWARD      0x02    /* search forwards */
#define FM_BLOCKSTOP    0x04    /* stop at start/end of block */
#define FM_SKIPCOMM     0x08    /* skip comments */

/* Values for action argument for do_buffer() */
#define DOBUF_GOTO      0       /* go to specified buffer */
#define DOBUF_SPLIT     1       /* split window and go to specified buffer */
#define DOBUF_UNLOAD    2       /* unload specified buffer(s) */
#define DOBUF_DEL       3       /* delete specified buffer(s) from buflist */
#define DOBUF_WIPE      4       /* delete specified buffer(s) really */

/* Values for start argument for do_buffer() */
#define DOBUF_CURRENT   0       /* "count" buffer from current buffer */
#define DOBUF_FIRST     1       /* "count" buffer from first buffer */
#define DOBUF_LAST      2       /* "count" buffer from last buffer */
#define DOBUF_MOD       3       /* "count" mod. buffer from current buffer */

/* Values for sub_cmd and which_pat argument for search_regcomp() */
/* Also used for which_pat argument for searchit() */
#define RE_SEARCH       0       /* save/use pat in/from search_pattern */
#define RE_SUBST        1       /* save/use pat in/from subst_pattern */
#define RE_BOTH         2       /* save pat in both patterns */
#define RE_LAST         2       /* use last used pattern if "pat" is NULL */

/* Second argument for vim_regcomp(). */
#define RE_MAGIC        1       /* 'magic' option */
#define RE_STRING       2       /* match in string instead of buffer text */
#define RE_STRICT       4       /* don't allow [abc] without ] */
#define RE_AUTO         8       /* automatic engine selection */

/* values for reg_do_extmatch */
#define REX_SET        1       /* to allow \z\(...\), */
#define REX_USE        2       /* to allow \z\1 et al. */

/* Return values for fullpathcmp() */
/* Note: can use (fullpathcmp() & FPC_SAME) to check for equal files */
#define FPC_SAME        1       /* both exist and are the same file. */
#define FPC_DIFF        2       /* both exist and are different files. */
#define FPC_NOTX        4       /* both don't exist. */
#define FPC_DIFFX       6       /* one of them doesn't exist. */
#define FPC_SAMEX       7       /* both don't exist and file names are same. */

/* flags for do_ecmd() */
#define ECMD_HIDE       0x01    /* don't free the current buffer */
#define ECMD_OLDBUF     0x04    /* use existing buffer if it exists */
#define ECMD_FORCEIT    0x08    /* ! used in Ex command */
#define ECMD_ADDBUF     0x10    /* don't edit, just add to buffer list */

/* for lnum argument in do_ecmd() */
#define ECMD_LASTL      (linenr_T)0     /* use last position in loaded file */
#define ECMD_LAST       (linenr_T)-1    /* use last position in all files */
#define ECMD_ONE        (linenr_T)1     /* use first line */

/* flags for do_cmdline() */
#define DOCMD_VERBOSE   0x01    /* included command in error message */
#define DOCMD_NOWAIT    0x02    /* don't call wait_return() and friends */
#define DOCMD_REPEAT    0x04    /* repeat exec. until getline() returns NULL */
#define DOCMD_KEYTYPED  0x08    /* don't reset KeyTyped */
#define DOCMD_EXCRESET  0x10    /* reset exception environment (for debugging) */
#define DOCMD_KEEPLINE  0x20    /* keep typed line for repeating with "." */

/* flags for beginline() */
#define BL_WHITE        1       /* cursor on first non-white in the line */
#define BL_SOL          2       /* use 'sol' option */
#define BL_FIX          4       /* don't leave cursor on a NUL */

/* flags for mf_sync() */
#define MFS_ALL         1       /* also sync blocks with negative numbers */
#define MFS_STOP        2       /* stop syncing when a character is available */
#define MFS_FLUSH       4       /* flushed file to disk */
#define MFS_ZERO        8       /* only write block 0 */

/* flags for buf_copy_options() */
#define BCO_ENTER       1       /* going to enter the buffer */
#define BCO_ALWAYS      2       /* always copy the options */

/* flags for do_put() */
#define PUT_FIXINDENT   1       /* make indent look nice */
#define PUT_CURSEND     2       /* leave cursor after end of new text */
#define PUT_CURSLINE    4       /* leave cursor on last line of new text */
#define PUT_LINE        8       /* put register as lines */
#define PUT_LINE_SPLIT  16      /* split line for linewise register */
#define PUT_LINE_FORWARD 32     /* put linewise register below Visual sel. */

/* flags for set_indent() */
#define SIN_CHANGED     1       /* call changed_bytes() when line changed */
#define SIN_INSERT      2       /* insert indent before existing text */
#define SIN_UNDO        4       /* save line for undo before changing it */

/* flags for insertchar() */
#define INSCHAR_FORMAT  1       /* force formatting */
#define INSCHAR_DO_COM  2       /* format comments */
#define INSCHAR_CTRLV   4       /* char typed just after CTRL-V */
#define INSCHAR_NO_FEX  8       /* don't use 'formatexpr' */
#define INSCHAR_COM_LIST 16     /* format comments with list/2nd line indent */

/* flags for open_line() */
#define OPENLINE_DELSPACES  1   /* delete spaces after cursor */
#define OPENLINE_DO_COM     2   /* format comments */
#define OPENLINE_KEEPTRAIL  4   /* keep trailing spaces */
#define OPENLINE_MARKFIX    8   /* fix mark positions */
#define OPENLINE_COM_LIST  16   /* format comments with list/2nd line indent */

/*
 * There are four history tables:
 */
#define HIST_CMD        0       /* colon commands */
#define HIST_SEARCH     1       /* search commands */
#define HIST_EXPR       2       /* expressions (from entering = register) */
#define HIST_INPUT      3       /* input() lines */
#define HIST_DEBUG      4       /* debug commands */
#define HIST_COUNT      5       /* number of history tables */

/*
 * Flags for chartab[].
 */
#define CT_CELL_MASK    0x07    /* mask: nr of display cells (1, 2 or 4) */
#define CT_PRINT_CHAR   0x10    /* flag: set for printable chars */
#define CT_ID_CHAR      0x20    /* flag: set for ID chars */
#define CT_FNAME_CHAR   0x40    /* flag: set for file name chars */

/*
 * Types of dialogs passed to do_vim_dialog().
 */
#define VIM_GENERIC     0
#define VIM_ERROR       1
#define VIM_WARNING     2
#define VIM_INFO        3
#define VIM_QUESTION    4
#define VIM_LAST_TYPE   4       /* sentinel value */

/*
 * Return values for functions like gui_yesnocancel()
 */
#define VIM_YES         2
#define VIM_NO          3
#define VIM_CANCEL      4
#define VIM_ALL         5
#define VIM_DISCARDALL  6

/*
 * arguments for win_split()
 */
#define WSP_ROOM        1       /* require enough room */
#define WSP_VERT        2       /* split vertically */
#define WSP_TOP         4       /* window at top-left of shell */
#define WSP_BOT         8       /* window at bottom-right of shell */
#define WSP_BELOW       32      /* put new window below/right */
#define WSP_ABOVE       64      /* put new window above/left */
#define WSP_NEWLOC      128     /* don't copy location list */

/*
 * arguments for gui_set_shellsize()
 */
#define RESIZE_VERT     1       /* resize vertically */
#define RESIZE_HOR      2       /* resize horizontally */
#define RESIZE_BOTH     15      /* resize in both directions */

/*
 * flags for check_changed()
 */
#define CCGD_AW         1       /* do autowrite if buffer was changed */
#define CCGD_MULTWIN    2       /* check also when several wins for the buf */
#define CCGD_FORCEIT    4       /* ! used */
#define CCGD_ALLBUF     8       /* may write all buffers */
#define CCGD_EXCMD      16      /* may suggest using ! */

/*
 * "flags" values for option-setting functions.
 * When OPT_GLOBAL and OPT_LOCAL are both missing, set both local and global
 * values, get local value.
 */
#define OPT_FREE        1       /* free old value if it was allocated */
#define OPT_GLOBAL      2       /* use global value */
#define OPT_LOCAL       4       /* use local value */
#define OPT_MODELINE    8       /* option in modeline */
#define OPT_WINONLY     16      /* only set window-local options */
#define OPT_NOWIN       32      /* don't set window-local options */

/* Magic chars used in confirm dialog strings */
#define DLG_BUTTON_SEP  '\n'
#define DLG_HOTKEY_CHAR '&'

/* Values for "starting" */
#define NO_SCREEN       2       /* no screen updating yet */
#define NO_BUFFERS      1       /* not all buffers loaded yet */
/*                      0          not starting anymore */

/* Values for swap_exists_action: what to do when swap file already exists */
#define SEA_NONE        0       /* don't use dialog */
#define SEA_DIALOG      1       /* use dialog when possible */
#define SEA_QUIT        2       /* quit editing the file */
#define SEA_RECOVER     3       /* recover the file */

/*
 * Minimal size for block 0 of a swap file.
 * NOTE: This depends on size of struct block0! It's not done with a sizeof(),
 * because struct block0 is defined in memline.c (Sorry).
 * The maximal block size is arbitrary.
 */
#define MIN_SWAP_PAGE_SIZE 1048
#define MAX_SWAP_PAGE_SIZE 50000

/* Special values for current_SID. */
#define SID_MODELINE    -1      /* when using a modeline */
#define SID_CMDARG      -2      /* for "--cmd" argument */
#define SID_CARG        -3      /* for "-c" argument */
#define SID_ENV         -4      /* for sourcing environment variable */
#define SID_ERROR       -5      /* option was reset because of an error */
#define SID_NONE        -6      /* don't set scriptID */

/*
 * Events for autocommands.
 */
enum auto_event
{
    EVENT_BUFADD = 0,           /* after adding a buffer to the buffer list */
    EVENT_BUFNEW,               /* after creating any buffer */
    EVENT_BUFDELETE,            /* deleting a buffer from the buffer list */
    EVENT_BUFWIPEOUT,           /* just before really deleting a buffer */
    EVENT_BUFENTER,             /* after entering a buffer */
    EVENT_BUFFILEPOST,          /* after renaming a buffer */
    EVENT_BUFFILEPRE,           /* before renaming a buffer */
    EVENT_BUFLEAVE,             /* before leaving a buffer */
    EVENT_BUFNEWFILE,           /* when creating a buffer for a new file */
    EVENT_BUFREADPOST,          /* after reading a buffer */
    EVENT_BUFREADPRE,           /* before reading a buffer */
    EVENT_BUFREADCMD,           /* read buffer using command */
    EVENT_BUFUNLOAD,            /* just before unloading a buffer */
    EVENT_BUFHIDDEN,            /* just after buffer becomes hidden */
    EVENT_BUFWINENTER,          /* after showing a buffer in a window */
    EVENT_BUFWINLEAVE,          /* just after buffer removed from window */
    EVENT_BUFWRITEPOST,         /* after writing a buffer */
    EVENT_BUFWRITEPRE,          /* before writing a buffer */
    EVENT_BUFWRITECMD,          /* write buffer using command */
    EVENT_CMDWINENTER,          /* after entering the cmdline window */
    EVENT_CMDWINLEAVE,          /* before leaving the cmdline window */
    EVENT_COLORSCHEME,          /* after loading a colorscheme */
    EVENT_COMPLETEDONE,         /* after finishing insert complete */
    EVENT_FILEAPPENDPOST,       /* after appending to a file */
    EVENT_FILEAPPENDPRE,        /* before appending to a file */
    EVENT_FILEAPPENDCMD,        /* append to a file using command */
    EVENT_FILECHANGEDSHELL,     /* after shell command that changed file */
    EVENT_FILECHANGEDSHELLPOST, /* after (not) reloading changed file */
    EVENT_FILECHANGEDRO,        /* before first change to read-only file */
    EVENT_FILEREADPOST,         /* after reading a file */
    EVENT_FILEREADPRE,          /* before reading a file */
    EVENT_FILEREADCMD,          /* read from a file using command */
    EVENT_FILETYPE,             /* new file type detected (user defined) */
    EVENT_FILEWRITEPOST,        /* after writing a file */
    EVENT_FILEWRITEPRE,         /* before writing a file */
    EVENT_FILEWRITECMD,         /* write to a file using command */
    EVENT_FILTERREADPOST,       /* after reading from a filter */
    EVENT_FILTERREADPRE,        /* before reading from a filter */
    EVENT_FILTERWRITEPOST,      /* after writing to a filter */
    EVENT_FILTERWRITEPRE,       /* before writing to a filter */
    EVENT_FOCUSGAINED,          /* got the focus */
    EVENT_FOCUSLOST,            /* lost the focus to another app */
    EVENT_GUIENTER,             /* after starting the GUI */
    EVENT_GUIFAILED,            /* after starting the GUI failed */
    EVENT_INSERTCHANGE,         /* when changing Insert/Replace mode */
    EVENT_INSERTENTER,          /* when entering Insert mode */
    EVENT_INSERTLEAVE,          /* when leaving Insert mode */
    EVENT_MENUPOPUP,            /* just before popup menu is displayed */
    EVENT_QUICKFIXCMDPOST,      /* after :make, :grep etc. */
    EVENT_QUICKFIXCMDPRE,       /* before :make, :grep etc. */
    EVENT_QUITPRE,              /* before :quit */
    EVENT_SESSIONLOADPOST,      /* after loading a session file */
    EVENT_STDINREADPOST,        /* after reading from stdin */
    EVENT_STDINREADPRE,         /* before reading from stdin */
    EVENT_SYNTAX,               /* syntax selected */
    EVENT_TERMCHANGED,          /* after changing 'term' */
    EVENT_TERMRESPONSE,         /* after setting "v:termresponse" */
    EVENT_USER,                 /* user defined autocommand */
    EVENT_VIMENTER,             /* after starting Vim */
    EVENT_VIMLEAVE,             /* before exiting Vim */
    EVENT_VIMLEAVEPRE,          /* before exiting Vim and writing .viminfo */
    EVENT_VIMRESIZED,           /* after Vim window was resized */
    EVENT_WINENTER,             /* after entering a window */
    EVENT_WINLEAVE,             /* before leaving a window */
    EVENT_ENCODINGCHANGED,      /* after changing the 'encoding' option */
    EVENT_INSERTCHARPRE,        /* before inserting a char */
    EVENT_CURSORHOLD,           /* cursor in same position for a while */
    EVENT_CURSORHOLDI,          /* idem, in Insert mode */
    EVENT_FUNCUNDEFINED,        /* if calling a function which doesn't exist */
    EVENT_REMOTEREPLY,          /* upon string reception from a remote vim */
    EVENT_SWAPEXISTS,           /* found existing swap file */
    EVENT_SOURCEPRE,            /* before sourcing a Vim script */
    EVENT_SOURCECMD,            /* sourcing a Vim script using command */
    EVENT_SPELLFILEMISSING,     /* spell file missing */
    EVENT_CURSORMOVED,          /* cursor was moved */
    EVENT_CURSORMOVEDI,         /* cursor was moved in Insert mode */
    EVENT_TABLEAVE,             /* before leaving a tab page */
    EVENT_TABENTER,             /* after entering a tab page */
    EVENT_SHELLCMDPOST,         /* after ":!cmd" */
    EVENT_SHELLFILTERPOST,      /* after ":1,2!cmd", ":w !cmd", ":r !cmd". */
    EVENT_TEXTCHANGED,          /* text was modified */
    EVENT_TEXTCHANGEDI,         /* text was modified in Insert mode */
    EVENT_CMDUNDEFINED,         /* command undefined */
    NUM_EVENTS                  /* MUST be the last one */
};

typedef enum auto_event event_T;

/*
 * Values for index in highlight_attr[].
 * When making changes, also update HL_FLAGS below!  And update the default
 * value of 'highlight' in option.c.
 */
typedef enum
{
    HLF_8 = 0       /* Meta & special keys listed with ":map", text that is
                       displayed different from what it is */
    , HLF_AT        /* @ and ~ characters at end of screen, characters that
                       don't really exist in the text */
    , HLF_D         /* directories in CTRL-D listing */
    , HLF_E         /* error messages */
    , HLF_H         /* obsolete, ignored */
    , HLF_I         /* incremental search */
    , HLF_L         /* last search string */
    , HLF_M         /* "--More--" message */
    , HLF_CM        /* Mode (e.g., "-- INSERT --") */
    , HLF_N         /* line number for ":number" and ":#" commands */
    , HLF_CLN       /* current line number */
    , HLF_R         /* return to continue message and yes/no questions */
    , HLF_S         /* status lines */
    , HLF_SNC       /* status lines of not-current windows */
    , HLF_C         /* column to separate vertically split windows */
    , HLF_T         /* Titles for output from ":set all", ":autocmd" etc. */
    , HLF_V         /* Visual mode */
    , HLF_VNC       /* Visual mode, autoselecting and not clipboard owner */
    , HLF_W         /* warning messages */
    , HLF_WM        /* Wildmenu highlight */
    , HLF_FL        /* Folded line */
    , HLF_FC        /* Fold column */
    , HLF_ADD       /* Added diff line */
    , HLF_CHD       /* Changed diff line */
    , HLF_DED       /* Deleted diff line */
    , HLF_TXD       /* Text Changed in diff line */
    , HLF_CONCEAL   /* Concealed text */
    , HLF_SC        /* Sign column */
    , HLF_SPB       /* SpellBad */
    , HLF_SPC       /* SpellCap */
    , HLF_SPR       /* SpellRare */
    , HLF_SPL       /* SpellLocal */
    , HLF_PNI       /* popup menu normal item */
    , HLF_PSI       /* popup menu selected item */
    , HLF_PSB       /* popup menu scrollbar */
    , HLF_PST       /* popup menu scrollbar thumb */
    , HLF_TP        /* tabpage line */
    , HLF_TPS       /* tabpage line selected */
    , HLF_TPF       /* tabpage line filler */
    , HLF_CUC       /* 'cursurcolumn' */
    , HLF_CUL       /* 'cursurline' */
    , HLF_MC        /* 'colorcolumn' */
    , HLF_COUNT     /* MUST be the last one */
} hlf_T;

/* The HL_FLAGS must be in the same order as the HLF_ enums!
 * When changing this also adjust the default for 'highlight'. */
#define HL_FLAGS {'8', '@', 'd', 'e', 'h', 'i', 'l', 'm', 'M', \
                  'n', 'N', 'r', 's', 'S', 'c', 't', 'v', 'V', 'w', 'W', \
                  'f', 'F', 'A', 'C', 'D', 'T', '-', '>', \
                  'B', 'P', 'R', 'L', \
                  '+', '=', 'x', 'X', '*', '#', '_', '!', '.', 'o'}

/*
 * Boolean constants
 */
#if !defined(TRUE)
#define FALSE  0           /* note: this is an int, not a long! */
#define TRUE   1
#endif

#define MAYBE   2           /* sometimes used for a variant on TRUE */

#if !defined(UINT32_T)
typedef uint32_t UINT32_T;
#endif

/*
 * Operator IDs; The order must correspond to opchars[] in ops.c!
 */
#define OP_NOP          0       /* no pending operation */
#define OP_DELETE       1       /* "d"  delete operator */
#define OP_YANK         2       /* "y"  yank operator */
#define OP_CHANGE       3       /* "c"  change operator */
#define OP_LSHIFT       4       /* "<"  left shift operator */
#define OP_RSHIFT       5       /* ">"  right shift operator */
#define OP_FILTER       6       /* "!"  filter operator */
#define OP_TILDE        7       /* "g~" switch case operator */
#define OP_INDENT       8       /* "="  indent operator */
#define OP_FORMAT       9       /* "gq" format operator */
#define OP_COLON        10      /* ":"  colon operator */
#define OP_UPPER        11      /* "gU" make upper case operator */
#define OP_LOWER        12      /* "gu" make lower case operator */
#define OP_JOIN         13      /* "J"  join operator, only for Visual mode */
#define OP_JOIN_NS      14      /* "gJ"  join operator, only for Visual mode */
#define OP_ROT13        15      /* "g?" rot-13 encoding */
#define OP_REPLACE      16      /* "r"  replace chars, only for Visual mode */
#define OP_INSERT       17      /* "I"  Insert column, only for Visual mode */
#define OP_APPEND       18      /* "A"  Append column, only for Visual mode */
#define OP_FOLD         19      /* "zf" define a fold */
#define OP_FOLDOPEN     20      /* "zo" open folds */
#define OP_FOLDOPENREC  21      /* "zO" open folds recursively */
#define OP_FOLDCLOSE    22      /* "zc" close folds */
#define OP_FOLDCLOSEREC 23      /* "zC" close folds recursively */
#define OP_FOLDDEL      24      /* "zd" delete folds */
#define OP_FOLDDELREC   25      /* "zD" delete folds recursively */
#define OP_FORMAT2      26      /* "gw" format operator, keeps cursor pos */
#define OP_FUNCTION     27      /* "g@" call 'operatorfunc' */

/*
 * Motion types, used for operators and for yank/delete registers.
 */
#define MCHAR   0               /* character-wise movement/register */
#define MLINE   1               /* line-wise movement/register */
#define MBLOCK  2               /* block-wise register */

#define MAUTO   0xff            /* Decide between MLINE/MCHAR */

/*
 * Minimum screen size
 */
#define MIN_COLUMNS     12      /* minimal columns for screen */
#define MIN_LINES       2       /* minimal lines for screen */
#define STATUS_HEIGHT   1       /* height of a status line under a window */
#define QF_WINHEIGHT    10      /* default height for quickfix window */

/*
 * Buffer sizes
 */
#if !defined(CMDBUFFSIZE)
#define CMDBUFFSIZE    256     /* size of the command processing buffer */
#endif

#define LSIZE       512         /* max. size of a line in the tags file */

#define IOSIZE     (1024+1)     /* file i/o and sprintf buffer size */

#define DIALOG_MSG_SIZE 1000    /* buffer size for dialog_msg() */

#define MSG_BUF_LEN 480        /* length of buffer for small messages */
#define MSG_BUF_CLEN  (MSG_BUF_LEN / 6)    /* cell length (worst case: utf-8
                                               takes 6 bytes for one cell) */

/* Size of the buffer used for tgetent().  Unfortunately this is largely
 * undocumented, some systems use 1024.  Using a buffer that is too small
 * causes a buffer overrun and a crash.  Use the maximum known value to stay
 * on the safe side. */
#define TBUFSZ 2048             /* buffer size for termcap entry */

/*
 * Maximum length of key sequence to be mapped.
 * Must be able to hold an Amiga resize report.
 */
#define MAXMAPLEN   50

/* Size in bytes of the hash used in the undo file. */
#define UNDO_HASH_SIZE 32

#include <fcntl.h>

#if defined(BINARY_FILE_IO)
#define WRITEBIN   "wb"        /* no CR-LF translation */
#define READBIN    "rb"
#define APPENDBIN  "ab"
#else
#define WRITEBIN   "w"
#define READBIN    "r"
#define APPENDBIN  "a"
#endif

/*
 * EMX doesn't have a global way of making open() use binary I/O.
 * Use O_BINARY for all open() calls.
 */
#define O_EXTRA    0

#if !defined(O_NOFOLLOW)
#define O_NOFOLLOW 0
#endif

#if !defined(W_OK)
#define W_OK 2         /* for systems that don't have W_OK in unistd.h */
#endif
#if !defined(R_OK)
#define R_OK 4         /* for systems that don't have R_OK in unistd.h */
#endif

/*
 * defines to avoid typecasts from (char_u *) to (char *) and back
 * (vim_strchr() and vim_strrchr() are now in alloc.c)
 */
#define STRLEN(s)           strlen((char *)(s))
#define STRCPY(d, s)        strcpy((char *)(d), (char *)(s))
#define STRNCPY(d, s, n)    strncpy((char *)(d), (char *)(s), (size_t)(n))
#define STRCMP(d, s)        strcmp((char *)(d), (char *)(s))
#define STRNCMP(d, s, n)    strncmp((char *)(d), (char *)(s), (size_t)(n))
#define STRICMP(d, s)      strcasecmp((char *)(d), (char *)(s))

/* Like strcpy() but allows overlapped source and destination. */
#define STRMOVE(d, s)       mch_memmove((d), (s), STRLEN(s) + 1)

#define STRNICMP(d, s, n)  strncasecmp((char *)(d), (char *)(s), (size_t)(n))

/* We need to call mb_stricmp() even when we aren't dealing with a multi-byte
 * encoding because mb_stricmp() takes care of all ascii and non-ascii
 * encodings, including characters with umlauts in latin1, etc., while
 * STRICMP() only handles the system locale version, which often does not
 * handle non-ascii properly. */

#define MB_STRICMP(d, s)       mb_strnicmp((char_u *)(d), (char_u *)(s), (int)MAXCOL)
#define MB_STRNICMP(d, s, n)   mb_strnicmp((char_u *)(d), (char_u *)(s), (int)(n))

#define STRCAT(d, s)        strcat((char *)(d), (char *)(s))
#define STRNCAT(d, s, n)    strncat((char *)(d), (char *)(s), (size_t)(n))

#define vim_strpbrk(s, cs) (char_u *)strpbrk((char *)(s), (char *)(cs))

#define MSG(s)                      msg((char_u *)(s))
#define MSG_ATTR(s, attr)           msg_attr((char_u *)(s), (attr))
#define EMSG(s)                     emsg((char_u *)(s))
#define EMSG2(s, p)                 emsg2((char_u *)(s), (char_u *)(p))
#define EMSG3(s, p, q)              emsg3((char_u *)(s), (char_u *)(p), (char_u *)(q))
#define EMSGN(s, n)                 emsgn((char_u *)(s), (long)(n))
#define EMSGU(s, n)                 emsgu((char_u *)(s), (long_u)(n))
#define OUT_STR(s)                  out_str((char_u *)(s))
#define OUT_STR_NF(s)               out_str_nf((char_u *)(s))
#define MSG_PUTS(s)                 msg_puts((char_u *)(s))
#define MSG_PUTS_ATTR(s, a)         msg_puts_attr((char_u *)(s), (a))
#define MSG_PUTS_TITLE(s)           msg_puts_title((char_u *)(s))
#define MSG_PUTS_LONG(s)            msg_puts_long_attr((char_u *)(s), 0)
#define MSG_PUTS_LONG_ATTR(s, a)    msg_puts_long_attr((char_u *)(s), (a))

/* Prefer using emsg3(), because perror() may send the output to the wrong
 * destination and mess up the screen. */
#define PERROR(msg)                (void)emsg3((char_u *)"%s: %s", (char_u *)msg, (char_u *)strerror(errno))

typedef long    linenr_T;               /* line number type */
typedef int     colnr_T;                /* column number type */
typedef unsigned short disptick_T;      /* display tick type */

#define MAXLNUM (0x7fffffffL)           /* maximum (invalid) line number */

/*
 * Well, you won't believe it, but some S/390 machines ("host", now also known
 * as zServer) use 31 bit pointers. There are also some newer machines, that
 * use 64 bit pointers. I don't know how to distinguish between 31 and 64 bit
 * machines, so the best way is to assume 31 bits whenever we detect OS/390 Unix.
 * With this we restrict the maximum line length to 1073741823. I guess this is
 * not a real problem. BTW:  Longer lines are split.
 */
#define MAXCOL (0x7fffffffL)          /* maximum column number, 31 bits */

#define SHOWCMD_COLS 10                 /* columns needed by shown command */
#define STL_MAX_ITEM 80                 /* max nr of %<flag> in statusline */

typedef void        *vim_acl_T;         /* dummy to pass an ACL to a function */

/*
 * Include a prototype for mch_memmove(), it may not be in alloc.pro.
 */
#if !defined(mch_memmove)
#define mch_memmove(to, from, len) memmove(to, from, len)
#endif

/*
 * fnamecmp() is used to compare file names.
 * On some systems case in a file name does not matter, on others it does.
 * (this does not account for maximum name lengths and things like "../dir",
 * thus it is not 100% accurate!)
 */
#define fnamecmp(x, y) vim_fnamecmp((char_u *)(x), (char_u *)(y))
#define fnamencmp(x, y, n) vim_fnamencmp((char_u *)(x), (char_u *)(y), (size_t)(n))

#define vim_memset(ptr, c, size)   memset((ptr), (c), (size))
#define vim_memcmp(p1, p2, len)   memcmp((p1), (p2), (len))

#if !defined(EINTR)
#define read_eintr(fd, buf, count) vim_read((fd), (buf), (count))
#define write_eintr(fd, buf, count) vim_write((fd), (buf), (count))
#endif

#define vim_read(fd, buf, count)   read((fd), (char *)(buf), (size_t) (count))
#define vim_write(fd, buf, count)  write((fd), (char *)(buf), (size_t) (count))

/*
 * Enums need a typecast to be used as array index (for Ultrix).
 */
#define hl_attr(n)      highlight_attr[(int)(n)]
#define term_str(n)     term_strings[(int)(n)]

/*
 * vim_iswhite() is used for "^" and the like. It differs from isspace()
 * because it doesn't include <CR> and <LF> and the like.
 */
#define vim_iswhite(x)  ((x) == ' ' || (x) == '\t')

/*
 * EXTERN is only defined in main.c.  That's where global variables are
 * actually defined and initialized.
 */
#if !defined(EXTERN)
#define EXTERN extern
#define INIT(x)
#else
#if !defined(INIT)
#define INIT(x) x
#define DO_INIT
#endif
#endif

#define MAX_MCO        6       /* maximum value for 'maxcombine' */

/* Maximum number of bytes in a multi-byte character.  It can be one 32-bit
 * character of up to 6 bytes, or one 16-bit character of up to three bytes
 * plus six following composing characters of three bytes each. */
#define MB_MAXBYTES    21

typedef struct timeval proftime_T;

/* ----------------------------------------------------------------------- */

/* Include option.h before structs.h, because the number of window-local and
 * buffer-local options is used there. */
/* #include "option.h" */      /* options and default values */
/*
 * option.h: definition of global variables for settable options
 */

/*
 * Default values for 'errorformat'.
 * The "%f|%l| %m" one is used for when the contents of the quickfix window is written to a file.
 */
#define DFLT_EFM        "%*[^\"]\"%f\"%*\\D%l: %m,\"%f\"%*\\D%l: %m,%-G%f:%l: (Each undeclared identifier is reported only once,%-G%f:%l: for each function it appears in.),%-GIn file included from %f:%l:%c:,%-GIn file included from %f:%l:%c\\,,%-GIn file included from %f:%l:%c,%-GIn file included from %f:%l,%-G%*[ ]from %f:%l:%c,%-G%*[ ]from %f:%l:,%-G%*[ ]from %f:%l\\,,%-G%*[ ]from %f:%l,%f:%l:%c:%m,%f(%l):%m,%f:%l:%m,\"%f\"\\, line %l%*\\D%c%*[^ ] %m,%D%*\\a[%*\\d]: Entering directory %*[`']%f',%X%*\\a[%*\\d]: Leaving directory %*[`']%f',%D%*\\a: Entering directory %*[`']%f',%X%*\\a: Leaving directory %*[`']%f',%DMaking %*\\a in %f,%f|%l| %m"

#define DFLT_GREPFORMAT "%f:%l:%m,%f:%l%m,%f  %l%m"

/* default values for b_p_ff 'fileformat' and p_ffs 'fileformats' */
#define FF_DOS          "dos"
#define FF_MAC          "mac"
#define FF_UNIX         "unix"

#define DFLT_FF       "unix"
#define DFLT_FFS_VIM  "unix,dos"
#define DFLT_FFS_VI  ""
#define DFLT_TEXTAUTO FALSE

/* Possible values for 'encoding' */
#define ENC_UCSBOM     "ucs-bom"       /* check for BOM at start of file */

/* default value for 'encoding' */
#define ENC_DFLT       "latin1"

/* end-of-line style */
#define EOL_UNKNOWN     -1      /* not defined yet */
#define EOL_UNIX        0       /* NL */
#define EOL_DOS         1       /* CR NL */
#define EOL_MAC         2       /* CR */

/* Formatting options for p_fo 'formatoptions' */
#define FO_WRAP         't'
#define FO_WRAP_COMS    'c'
#define FO_RET_COMS     'r'
#define FO_OPEN_COMS    'o'
#define FO_Q_COMS       'q'
#define FO_Q_NUMBER     'n'
#define FO_Q_SECOND     '2'
#define FO_INS_VI       'v'
#define FO_INS_LONG     'l'
#define FO_INS_BLANK    'b'
#define FO_MBYTE_BREAK  'm'     /* break before/after multi-byte char */
#define FO_MBYTE_JOIN   'M'     /* no space before/after multi-byte char */
#define FO_MBYTE_JOIN2  'B'     /* no space between multi-byte chars */
#define FO_ONE_LETTER   '1'
#define FO_WHITE_PAR    'w'     /* trailing white space continues paragr. */
#define FO_AUTO         'a'     /* automatic formatting */
#define FO_REMOVE_COMS  'j'     /* remove comment leaders when joining lines */

#define DFLT_FO_VI      "vt"
#define DFLT_FO_VIM     "tcq"
#define FO_ALL          "tcroq2vlb1mMBn,awj"    /* for do_set() */

/* characters for the p_cpo option: */
#define CPO_ALTREAD     'a'     /* ":read" sets alternate file name */
#define CPO_ALTWRITE    'A'     /* ":write" sets alternate file name */
#define CPO_BAR         'b'     /* "\|" ends a mapping */
#define CPO_BSLASH      'B'     /* backslash in mapping is not special */
#define CPO_SEARCH      'c'
#define CPO_CONCAT      'C'     /* Don't concatenate sourced lines */
#define CPO_DOTTAG      'd'     /* "./tags" in 'tags' is in current dir */
#define CPO_DIGRAPH     'D'     /* No digraph after "r", "f", etc. */
#define CPO_EXECBUF     'e'
#define CPO_EMPTYREGION 'E'     /* operating on empty region is an error */
#define CPO_FNAMER      'f'     /* set file name for ":r file" */
#define CPO_FNAMEW      'F'     /* set file name for ":w file" */
#define CPO_GOTO1       'g'     /* goto line 1 for ":edit" */
#define CPO_INSEND      'H'     /* "I" inserts before last blank in line */
#define CPO_INTMOD      'i'     /* interrupt a read makes buffer modified */
#define CPO_INDENT      'I'     /* remove auto-indent more often */
#define CPO_JOINSP      'j'     /* only use two spaces for join after '.' */
#define CPO_ENDOFSENT   'J'     /* need two spaces to detect end of sentence */
#define CPO_KEYCODE     'k'     /* don't recognize raw key code in mappings */
#define CPO_KOFFSET     'K'     /* don't wait for key code in mappings */
#define CPO_LITERAL     'l'     /* take char after backslash in [] literal */
#define CPO_LISTWM      'L'     /* 'list' changes wrapmargin */
#define CPO_SHOWMATCH   'm'
#define CPO_MATCHBSL    'M'     /* "%" ignores use of backslashes */
#define CPO_NUMCOL      'n'     /* 'number' column also used for text */
#define CPO_LINEOFF     'o'
#define CPO_OVERNEW     'O'     /* silently overwrite new file */
#define CPO_LISP        'p'     /* 'lisp' indenting */
#define CPO_FNAMEAPP    'P'     /* set file name for ":w >>file" */
#define CPO_JOINCOL     'q'     /* with "3J" use column after first join */
#define CPO_REDO        'r'
#define CPO_REMMARK     'R'     /* remove marks when filtering */
#define CPO_BUFOPT      's'
#define CPO_BUFOPTGLOB  'S'
#define CPO_TAGPAT      't'
#define CPO_UNDO        'u'     /* "u" undoes itself */
#define CPO_BACKSPACE   'v'     /* "v" keep deleted text */
#define CPO_CW          'w'     /* "cw" only changes one blank */
#define CPO_FWRITE      'W'     /* "w!" doesn't overwrite readonly files */
#define CPO_ESC         'x'
#define CPO_REPLCNT     'X'     /* "R" with a count only deletes chars once */
#define CPO_YANK        'y'
#define CPO_KEEPRO      'Z'     /* don't reset 'readonly' on ":w!" */
#define CPO_DOLLAR      '$'
#define CPO_FILTER      '!'
#define CPO_MATCH       '%'
#define CPO_STAR        '*'     /* ":*" means ":@" */
#define CPO_PLUS        '+'     /* ":write file" resets 'modified' */
#define CPO_MINUS       '-'     /* "9-" fails at and before line 9 */
#define CPO_SPECI       '<'     /* don't recognize <> in mappings */
#define CPO_REGAPPEND   '>'     /* insert NL when appending to a register */
/* POSIX flags */
#define CPO_HASH        '#'     /* "D", "o" and "O" do not use a count */
#define CPO_PARA        '{'     /* "{" is also a paragraph boundary */
#define CPO_TSIZE       '|'     /* $LINES and $COLUMNS overrule term size */
#define CPO_PRESERVE    '&'     /* keep swap file after :preserve */
#define CPO_SUBPERCENT  '/'     /* % in :s string uses previous one */
#define CPO_BACKSL      '\\'    /* \ is not special in [] */
#define CPO_CHDIR       '.'     /* don't chdir if buffer is modified */
#define CPO_SCOLON      ';'     /* using "," and ";" will skip over char if
                                 * cursor would not move */
/* default values for Vim, Vi and POSIX */
#define CPO_VIM         "aABceFs"
#define CPO_VI          "aAbBcCdDeEfFgHiIjJkKlLmMnoOpPqrRsStuvwWxXyZ$!%*-+<>;"
#define CPO_ALL         "aAbBcCdDeEfFgHiIjJkKlLmMnoOpPqrRsStuvwWxXyZ$!%*-+<>#{|&/\\.;"

/* characters for p_ww option: */
#define WW_ALL          "bshl<>[],~"

/* characters for p_mouse option: */
#define MOUSE_NORMAL    'n'             /* use mouse in Normal mode */
#define MOUSE_VISUAL    'v'             /* use mouse in Visual/Select mode */
#define MOUSE_INSERT    'i'             /* use mouse in Insert mode */
#define MOUSE_COMMAND   'c'             /* use mouse in Command-line mode */
#define MOUSE_HELP      'h'             /* use mouse in help buffers */
#define MOUSE_RETURN    'r'             /* use mouse for hit-return message */
#define MOUSE_A         "nvich"         /* used for 'a' flag */
#define MOUSE_ALL       "anvichr"       /* all possible characters */
#define MOUSE_NONE      ' '             /* don't use Visual selection */
#define MOUSE_NONEF     'x'             /* forced modeless selection */

#define COCU_ALL        "nvic"          /* flags for 'concealcursor' */

/* characters for p_shm option: */
#define SHM_RO          'r'             /* readonly */
#define SHM_MOD         'm'             /* modified */
#define SHM_FILE        'f'             /* (file 1 of 2) */
#define SHM_LAST        'i'             /* last line incomplete */
#define SHM_TEXT        'x'             /* tx instead of textmode */
#define SHM_LINES       'l'             /* "L" instead of "lines" */
#define SHM_NEW         'n'             /* "[New]" instead of "[New file]" */
#define SHM_WRI         'w'             /* "[w]" instead of "written" */
#define SHM_A           "rmfixlnw"      /* represented by 'a' flag */
#define SHM_WRITE       'W'             /* don't use "written" at all */
#define SHM_TRUNC       't'             /* trunctate file messages */
#define SHM_TRUNCALL    'T'             /* trunctate all messages */
#define SHM_OVER        'o'             /* overwrite file messages */
#define SHM_OVERALL     'O'             /* overwrite more messages */
#define SHM_SEARCH      's'             /* no search hit bottom messages */
#define SHM_ATTENTION   'A'             /* no ATTENTION messages */
#define SHM_INTRO       'I'             /* intro messages */
#define SHM_COMPLETIONMENU  'c'         /* completion menu messages */
#define SHM_ALL         "rmfixlnwaWtToOsAIc" /* all possible flags for 'shm' */

/* characters for p_go: */
#define GO_ASEL         'a'             /* autoselect */
#define GO_ASELML       'A'             /* autoselect modeless selection */
#define GO_BOT          'b'             /* use bottom scrollbar */
#define GO_CONDIALOG    'c'             /* use console dialog */
#define GO_TABLINE      'e'             /* may show tabline */
#define GO_FORG         'f'             /* start GUI in foreground */
#define GO_GREY         'g'             /* use grey menu items */
#define GO_HORSCROLL    'h'             /* flexible horizontal scrolling */
#define GO_ICON         'i'             /* use Vim icon */
#define GO_LEFT         'l'             /* use left scrollbar */
#define GO_VLEFT        'L'             /* left scrollbar with vert split */
#define GO_MENUS        'm'             /* use menu bar */
#define GO_NOSYSMENU    'M'             /* don't source system menu */
#define GO_POINTER      'p'             /* pointer enter/leave callbacks */
#define GO_ASELPLUS     'P'             /* autoselectPlus */
#define GO_RIGHT        'r'             /* use right scrollbar */
#define GO_VRIGHT       'R'             /* right scrollbar with vert split */
#define GO_TEAROFF      't'             /* add tear-off menu items */
#define GO_TOOLBAR      'T'             /* add toolbar */
#define GO_FOOTER       'F'             /* add footer */
#define GO_VERTICAL     'v'             /* arrange dialog buttons vertically */
#define GO_ALL          "aAbcefFghilmMprtTv" /* all possible flags for 'go' */

/* flags for 'comments' option */
#define COM_NEST        'n'             /* comments strings nest */
#define COM_BLANK       'b'             /* needs blank after string */
#define COM_START       's'             /* start of comment */
#define COM_MIDDLE      'm'             /* middle of comment */
#define COM_END         'e'             /* end of comment */
#define COM_AUTO_END    'x'             /* last char of end closes comment */
#define COM_FIRST       'f'             /* first line comment only */
#define COM_LEFT        'l'             /* left adjusted */
#define COM_RIGHT       'r'             /* right adjusted */
#define COM_NOBACK      'O'             /* don't use for "O" command */
#define COM_ALL         "nbsmexflrO"    /* all flags for 'comments' option */
#define COM_MAX_LEN     50              /* maximum length of a part */

/* flags for 'statusline' option */
#define STL_FILEPATH    'f'             /* path of file in buffer */
#define STL_FULLPATH    'F'             /* full path of file in buffer */
#define STL_FILENAME    't'             /* last part (tail) of file path */
#define STL_COLUMN      'c'             /* column og cursor */
#define STL_VIRTCOL     'v'             /* virtual column */
#define STL_VIRTCOL_ALT 'V'             /* - with 'if different' display */
#define STL_LINE        'l'             /* line number of cursor */
#define STL_NUMLINES    'L'             /* number of lines in buffer */
#define STL_BUFNO       'n'             /* current buffer number */
#define STL_KEYMAP      'k'             /* 'keymap' when active */
#define STL_OFFSET      'o'             /* offset of character under cursor */
#define STL_OFFSET_X    'O'             /* - in hexadecimal */
#define STL_BYTEVAL     'b'             /* byte value of character */
#define STL_BYTEVAL_X   'B'             /* - in hexadecimal */
#define STL_ROFLAG      'r'             /* readonly flag */
#define STL_ROFLAG_ALT  'R'             /* - other display */
#define STL_HELPFLAG    'h'             /* window is showing a help file */
#define STL_HELPFLAG_ALT 'H'            /* - other display */
#define STL_FILETYPE    'y'             /* 'filetype' */
#define STL_FILETYPE_ALT 'Y'            /* - other display */
#define STL_PREVIEWFLAG 'w'             /* window is showing the preview buf */
#define STL_PREVIEWFLAG_ALT 'W'         /* - other display */
#define STL_MODIFIED    'm'             /* modified flag */
#define STL_MODIFIED_ALT 'M'            /* - other display */
#define STL_QUICKFIX    'q'             /* quickfix window description */
#define STL_PERCENTAGE  'p'             /* percentage through file */
#define STL_ALTPERCENT  'P'             /* percentage as TOP BOT ALL or NN% */
#define STL_ARGLISTSTAT 'a'             /* argument list status as (x of y) */
#define STL_PAGENUM     'N'             /* page number (when printing) */
#define STL_VIM_EXPR    '{'             /* start of expression to substitute */
#define STL_MIDDLEMARK  '='             /* separation between left and right */
#define STL_TRUNCMARK   '<'             /* truncation mark if line is too long */
#define STL_USER_HL     '*'             /* highlight from (User)1..9 or 0 */
#define STL_HIGHLIGHT   '#'             /* highlight name */
#define STL_TABPAGENR   'T'             /* tab page label nr */
#define STL_TABCLOSENR  'X'             /* tab page close nr */
#define STL_ALL         ((char_u *)"fFtcvVlLknoObBrRhHmYyWwMqpPaN{#")

/* flags used for parsed 'wildmode' */
#define WIM_FULL        1
#define WIM_LONGEST     2
#define WIM_LIST        4

/* arguments for can_bs() */
#define BS_INDENT       'i'     /* "Indent" */
#define BS_EOL          'o'     /* "eOl" */
#define BS_START        's'     /* "Start" */

#define LISPWORD_VALUE  "defun,define,defmacro,set!,lambda,if,case,let,flet,let*,letrec,do,do*,define-syntax,let-syntax,letrec-syntax,destructuring-bind,defpackage,defparameter,defstruct,deftype,defvar,do-all-symbols,do-external-symbols,do-symbols,dolist,dotimes,ecase,etypecase,eval-when,labels,macrolet,multiple-value-bind,multiple-value-call,multiple-value-prog1,multiple-value-setq,prog1,progv,typecase,unless,unwind-protect,when,with-input-from-string,with-open-file,with-open-stream,with-output-to-string,with-package-iterator,define-condition,handler-bind,handler-case,restart-bind,restart-case,with-simple-restart,store-value,use-value,muffle-warning,abort,continue,with-slots,with-slots*,with-accessors,with-accessors*,defclass,defmethod,print-unreadable-object"

/*
 * The following are actual variables for the options
 */

EXTERN long     p_aleph;        /* 'aleph' */
EXTERN char_u   *p_ambw;        /* 'ambiwidth' */
EXTERN int      p_ar;           /* 'autoread' */
EXTERN int      p_aw;           /* 'autowrite' */
EXTERN int      p_awa;          /* 'autowriteall' */
EXTERN char_u   *p_bs;          /* 'backspace' */
EXTERN char_u   *p_bg;          /* 'background' */
EXTERN int      p_bk;           /* 'backup' */
EXTERN char_u   *p_bkc;         /* 'backupcopy' */
EXTERN unsigned bkc_flags;      /* flags from 'backupcopy' */
#if defined(IN_OPTION_C)
static char *(p_bkc_values[]) = {"yes", "auto", "no", "breaksymlink", "breakhardlink", NULL};
#endif
#define BKC_YES                0x001
#define BKC_AUTO               0x002
#define BKC_NO                 0x004
#define BKC_BREAKSYMLINK       0x008
#define BKC_BREAKHARDLINK      0x010
EXTERN char_u   *p_bdir;        /* 'backupdir' */
EXTERN char_u   *p_bex;         /* 'backupext' */
EXTERN char_u   *p_breakat;     /* 'breakat' */
EXTERN char_u   *p_cmp;         /* 'casemap' */
EXTERN unsigned cmp_flags;
#if defined(IN_OPTION_C)
static char *(p_cmp_values[]) = {"internal", "keepascii", NULL};
#endif
#define CMP_INTERNAL           0x001
#define CMP_KEEPASCII          0x002
EXTERN char_u   *p_enc;         /* 'encoding' */
EXTERN int      p_deco;         /* 'delcombine' */
EXTERN char_u   *p_ccv;         /* 'charconvert' */
EXTERN char_u   *p_cedit;       /* 'cedit' */
EXTERN long     p_cwh;          /* 'cmdwinheight' */
EXTERN char_u   *p_cb;          /* 'clipboard' */
EXTERN long     p_ch;           /* 'cmdheight' */
EXTERN int      p_confirm;      /* 'confirm' */
EXTERN int      p_cp;           /* 'compatible' */
EXTERN char_u   *p_cpo;         /* 'cpoptions' */
EXTERN char_u   *p_debug;       /* 'debug' */
EXTERN int      p_dg;           /* 'digraph' */
EXTERN char_u   *p_dir;         /* 'directory' */
EXTERN char_u   *p_dy;          /* 'display' */
EXTERN unsigned dy_flags;
#if defined(IN_OPTION_C)
static char *(p_dy_values[]) = {"lastline", "uhex", NULL};
#endif
#define DY_LASTLINE             0x001
#define DY_UHEX                 0x002
EXTERN int      p_ed;           /* 'edcompatible' */
EXTERN char_u   *p_ead;         /* 'eadirection' */
EXTERN int      p_ea;           /* 'equalalways' */
EXTERN char_u   *p_ep;          /* 'equalprg' */
EXTERN int      p_eb;           /* 'errorbells' */
EXTERN char_u   *p_ei;          /* 'eventignore' */
EXTERN int      p_ek;           /* 'esckeys' */
EXTERN int      p_exrc;         /* 'exrc' */
EXTERN char_u   *p_fencs;       /* 'fileencodings' */
EXTERN char_u   *p_ffs;         /* 'fileformats' */
EXTERN long     p_fic;          /* 'fileignorecase' */
EXTERN char_u   *p_fp;          /* 'formatprg' */
EXTERN int      p_fs;           /* 'fsync' */
EXTERN int      p_gd;           /* 'gdefault' */
EXTERN int      p_prompt;       /* 'prompt' */
EXTERN char_u   *p_guicursor;   /* 'guicursor' */
EXTERN int      p_hid;          /* 'hidden' */
/* Use P_HID to check if a buffer is to be hidden when it is no longer visible in a window. */
#define P_HID(dummy) (p_hid || cmdmod.hide)
EXTERN char_u   *p_hl;          /* 'highlight' */
EXTERN int      p_hls;          /* 'hlsearch' */
EXTERN long     p_hi;           /* 'history' */
EXTERN int      p_hkmap;        /* 'hkmap' */
EXTERN int      p_hkmapp;       /* 'hkmapp' */
EXTERN int      p_icon;         /* 'icon' */
EXTERN char_u   *p_iconstring;  /* 'iconstring' */
EXTERN int      p_ic;           /* 'ignorecase' */
EXTERN int      p_is;           /* 'incsearch' */
EXTERN int      p_im;           /* 'insertmode' */
EXTERN char_u   *p_isf;         /* 'isfname' */
EXTERN char_u   *p_isi;         /* 'isident' */
EXTERN char_u   *p_isp;         /* 'isprint' */
EXTERN int      p_js;           /* 'joinspaces' */
EXTERN char_u   *p_kp;          /* 'keywordprg' */
EXTERN char_u   *p_km;          /* 'keymodel' */
EXTERN char_u   *p_lispwords;   /* 'lispwords' */
EXTERN long     p_ls;           /* 'laststatus' */
EXTERN long     p_stal;         /* 'showtabline' */
EXTERN char_u   *p_lcs;         /* 'listchars' */

EXTERN int      p_lz;           /* 'lazyredraw' */
EXTERN int      p_lpl;          /* 'loadplugins' */
EXTERN int      p_magic;        /* 'magic' */
EXTERN char_u   *p_cc;          /* 'colorcolumn' */
EXTERN int      p_cc_cols[256]; /* array for 'colorcolumn' columns */
EXTERN long     p_mat;          /* 'matchtime' */
EXTERN long     p_mco;          /* 'maxcombine' */
EXTERN long     p_mfd;          /* 'maxfuncdepth' */
EXTERN long     p_mmd;          /* 'maxmapdepth' */
EXTERN long     p_mm;           /* 'maxmem' */
EXTERN long     p_mmp;          /* 'maxmempattern' */
EXTERN long     p_mmt;          /* 'maxmemtot' */
EXTERN long     p_mls;          /* 'modelines' */
EXTERN char_u   *p_mouse;       /* 'mouse' */
EXTERN char_u   *p_mousem;      /* 'mousemodel' */
EXTERN long     p_mouset;       /* 'mousetime' */
EXTERN int      p_more;         /* 'more' */
EXTERN char_u   *p_opfunc;      /* 'operatorfunc' */
EXTERN char_u   *p_para;        /* 'paragraphs' */
EXTERN int      p_paste;        /* 'paste' */
EXTERN char_u   *p_pt;          /* 'pastetoggle' */
EXTERN char_u   *p_pm;          /* 'patchmode' */
EXTERN char_u   *p_path;        /* 'path' */
EXTERN long     p_rdt;          /* 'redrawtime' */
EXTERN int      p_remap;        /* 'remap' */
EXTERN long     p_re;           /* 'regexpengine' */
EXTERN long     p_report;       /* 'report' */
EXTERN int      p_ari;          /* 'allowrevins' */
EXTERN int      p_ri;           /* 'revins' */
EXTERN int      p_ru;           /* 'ruler' */
EXTERN char_u   *p_ruf;         /* 'rulerformat' */
EXTERN char_u   *p_rtp;         /* 'runtimepath' */
EXTERN long     p_sj;           /* 'scrolljump' */
EXTERN long     p_so;           /* 'scrolloff' */
EXTERN char_u   *p_sbo;         /* 'scrollopt' */
EXTERN char_u   *p_sections;    /* 'sections' */
EXTERN int      p_secure;       /* 'secure' */
EXTERN char_u   *p_sel;         /* 'selection' */
EXTERN char_u   *p_slm;         /* 'selectmode' */
EXTERN char_u   *p_sh;          /* 'shell' */
EXTERN char_u   *p_shcf;        /* 'shellcmdflag' */
EXTERN char_u   *p_shq;         /* 'shellquote' */
EXTERN char_u   *p_sxq;         /* 'shellxquote' */
EXTERN char_u   *p_sxe;         /* 'shellxescape' */
EXTERN char_u   *p_srr;         /* 'shellredir' */
EXTERN int      p_stmp;         /* 'shelltemp' */
EXTERN char_u   *p_stl;         /* 'statusline' */
EXTERN int      p_sr;           /* 'shiftround' */
EXTERN char_u   *p_shm;         /* 'shortmess' */
EXTERN char_u   *p_sbr;         /* 'showbreak' */
EXTERN int      p_sc;           /* 'showcmd' */
EXTERN int      p_sm;           /* 'showmatch' */
EXTERN int      p_smd;          /* 'showmode' */
EXTERN long     p_ss;           /* 'sidescroll' */
EXTERN long     p_siso;         /* 'sidescrolloff' */
EXTERN int      p_scs;          /* 'smartcase' */
EXTERN int      p_sta;          /* 'smarttab' */
EXTERN int      p_sb;           /* 'splitbelow' */
EXTERN long     p_tpm;          /* 'tabpagemax' */
EXTERN char_u   *p_tal;         /* 'tabline' */
EXTERN int      p_spr;          /* 'splitright' */
EXTERN int      p_sol;          /* 'startofline' */
EXTERN char_u   *p_su;          /* 'suffixes' */
EXTERN char_u   *p_sws;         /* 'swapsync' */
EXTERN char_u   *p_swb;         /* 'switchbuf' */
EXTERN unsigned swb_flags;
#if defined(IN_OPTION_C)
static char *(p_swb_values[]) = {"useopen", "usetab", "split", "newtab", NULL};
#endif
#define SWB_USEOPEN             0x001
#define SWB_USETAB              0x002
#define SWB_SPLIT               0x004
#define SWB_NEWTAB              0x008
EXTERN char_u   *p_tenc;        /* 'termencoding' */
EXTERN int      p_terse;        /* 'terse' */
EXTERN int      p_ta;           /* 'textauto' */
EXTERN int      p_to;           /* 'tildeop' */
EXTERN int      p_timeout;      /* 'timeout' */
EXTERN long     p_tm;           /* 'timeoutlen' */
EXTERN int      p_title;        /* 'title' */
EXTERN long     p_titlelen;     /* 'titlelen' */
EXTERN char_u   *p_titleold;    /* 'titleold' */
EXTERN char_u   *p_titlestring; /* 'titlestring' */
EXTERN int      p_ttimeout;     /* 'ttimeout' */
EXTERN long     p_ttm;          /* 'ttimeoutlen' */
EXTERN int      p_tbi;          /* 'ttybuiltin' */
EXTERN int      p_tf;           /* 'ttyfast' */
EXTERN long     p_ttyscroll;    /* 'ttyscroll' */
EXTERN char_u   *p_ttym;        /* 'ttymouse' */
EXTERN unsigned ttym_flags;
#if defined(IN_OPTION_C)
static char *(p_ttym_values[]) = {"xterm", "xterm2", "dec", "netterm", "jsbterm", "pterm", "urxvt", "sgr", NULL};
#endif
#define TTYM_XTERM             0x01
#define TTYM_XTERM2            0x02
#define TTYM_DEC               0x04
#define TTYM_NETTERM           0x08
#define TTYM_JSBTERM           0x10
#define TTYM_PTERM             0x20
#define TTYM_URXVT             0x40
#define TTYM_SGR               0x80
EXTERN char_u   *p_udir;        /* 'undodir' */
EXTERN long     p_ul;           /* 'undolevels' */
EXTERN long     p_ur;           /* 'undoreload' */
EXTERN long     p_uc;           /* 'updatecount' */
EXTERN long     p_ut;           /* 'updatetime' */
EXTERN char_u   *p_fcs;         /* 'fillchar' */
EXTERN int      p_vb;           /* 'visualbell' */
EXTERN char_u   *p_ve;          /* 'virtualedit' */
EXTERN unsigned ve_flags;
#if defined(IN_OPTION_C)
static char *(p_ve_values[]) = {"block", "insert", "all", "onemore", NULL};
#endif
#define VE_BLOCK       5       /* includes "all" */
#define VE_INSERT      6       /* includes "all" */
#define VE_ALL         4
#define VE_ONEMORE     8
EXTERN long     p_verbose;      /* 'verbose' */
#if defined(IN_OPTION_C)
char_u  *p_vfile = (char_u *)""; /* used before options are initialized */
#else
extern char_u   *p_vfile;       /* 'verbosefile' */
#endif
EXTERN int      p_warn;         /* 'warn' */
EXTERN long     p_window;       /* 'window' */
EXTERN int      p_wiv;          /* 'weirdinvert' */
EXTERN char_u   *p_ww;          /* 'whichwrap' */
EXTERN long     p_wc;           /* 'wildchar' */
EXTERN long     p_wcm;          /* 'wildcharm' */
EXTERN long     p_wic;          /* 'wildignorecase' */
EXTERN char_u   *p_wim;         /* 'wildmode' */
EXTERN long     p_wh;           /* 'winheight' */
EXTERN long     p_wmh;          /* 'winminheight' */
EXTERN long     p_wmw;          /* 'winminwidth' */
EXTERN long     p_wiw;          /* 'winwidth' */
EXTERN int      p_ws;           /* 'wrapscan' */
EXTERN int      p_write;        /* 'write' */
EXTERN int      p_wa;           /* 'writeany' */
EXTERN int      p_wb;           /* 'writebackup' */
EXTERN long     p_wd;           /* 'writedelay' */

/*
 * "indir" values for buffer-local opions.
 * These need to be defined globally, so that the BV_COUNT can be used with b_p_scriptID[].
 */
enum
{
    BV_AI = 0
    , BV_AR
    , BV_BKC
    , BV_BIN
    , BV_BL
    , BV_BOMB
    , BV_CI
    , BV_CIN
    , BV_CINK
    , BV_CINO
    , BV_CINW
    , BV_CM
    , BV_COM
    , BV_EOL
    , BV_EP
    , BV_ET
    , BV_FENC
    , BV_BEXPR
    , BV_FEX
    , BV_FF
    , BV_FLP
    , BV_FO
    , BV_FT
    , BV_IMI
    , BV_IMS
    , BV_INDE
    , BV_INDK
    , BV_INF
    , BV_ISK
    , BV_KP
    , BV_LISP
    , BV_LW
    , BV_MA
    , BV_ML
    , BV_MOD
    , BV_MPS
    , BV_NF
    , BV_PATH
    , BV_PI
    , BV_QE
    , BV_RO
    , BV_SI
    , BV_SN
    , BV_SMC
    , BV_SYN
    , BV_STS
    , BV_SW
    , BV_SWF
    , BV_TS
    , BV_TW
    , BV_TX
    , BV_UDF
    , BV_UL
    , BV_WM
    , BV_COUNT      /* must be the last one */
};

/*
 * "indir" values for window-local options.
 * These need to be defined globally, so that the WV_COUNT can be used in the window structure.
 */
enum
{
    WV_LIST = 0
    , WV_COCU
    , WV_COLE
    , WV_CRBIND
    , WV_BRI
    , WV_BRIOPT
    , WV_LBR
    , WV_NU
    , WV_RNU
    , WV_NUW
    , WV_RL
    , WV_RLC
    , WV_SCBIND
    , WV_SCROLL
    , WV_CUC
    , WV_CUL
    , WV_CC
    , WV_STL
    , WV_WFH
    , WV_WFW
    , WV_WRAP
    , WV_COUNT      /* must be the last one */
};

/* Value for b_p_ul indicating the global value must be used. */
#define NO_LOCAL_UNDOLEVEL -123456

/* ----------------------------------------------------------------------- */

/* #include "structs.h" */     /* file that defines many structures */
/*
 * This file contains various definitions of structures that are used by Vim
 */

/*
 * position in file or buffer
 */
typedef struct
{
    linenr_T    lnum;   /* line number */
    colnr_T     col;    /* column number */
    colnr_T     coladd;
} pos_T;

#define INIT_POS_T(l, c, ca) {l, c, ca}

/*
 * Same, but without coladd.
 */
typedef struct
{
    linenr_T    lnum;   /* line number */
    colnr_T     col;    /* column number */
} lpos_T;

/*
 * Structure used for growing arrays.
 * This is used to store information that only grows, is deleted all at
 * once, and needs to be accessed by index.  See ga_clear() and ga_grow().
 */
typedef struct growarray
{
    int     ga_len;                 /* current number of items used */
    int     ga_maxlen;              /* maximum number of items possible */
    int     ga_itemsize;            /* sizeof(item) */
    int     ga_growsize;            /* number of items to grow each time */
    void    *ga_data;               /* pointer to the first item */
} garray_T;

#define GA_EMPTY    {0, 0, 0, 0, NULL}

typedef struct window_S         win_T;
typedef struct wininfo_S        wininfo_T;
typedef struct frame_S          frame_T;
typedef int                     scid_T;         /* script ID */
typedef struct file_buffer      buf_T;  /* forward declaration */

/* ----------------------------------------------------------------------- */

/*
 * This is here because regexp.h needs pos_T and below regprog_T is used.
 */
/* #include "regexp.h" */

/*
 * The number of sub-matches is limited to 10.
 * The first one (index 0) is the whole match, referenced with "\0".
 * The second one (index 1) is the first sub-match, referenced with "\1".
 * This goes up to the tenth (index 9), referenced with "\9".
 */
#define NSUBEXP  10

/*
 * In the NFA engine: how many braces are allowed.
 * TODO(RE): Use dynamic memory allocation instead of static, like here
 */
#define NFA_MAX_BRACES 20

/*
 * In the NFA engine: how many states are allowed
 */
#define NFA_MAX_STATES 100000
#define NFA_TOO_EXPENSIVE -1

/* Which regexp engine to use? Needed for vim_regcomp().
 * Must match with 'regexpengine'. */
#define AUTOMATIC_ENGINE    0
#define BACKTRACKING_ENGINE 1
#define NFA_ENGINE          2

typedef struct regengine regengine_T;

/*
 * Structure returned by vim_regcomp() to pass on to vim_regexec().
 * This is the general structure. For the actual matcher, two specific
 * structures are used. See code below.
 */
typedef struct regprog
{
    regengine_T         *engine;
    unsigned            regflags;
    unsigned            re_engine;   /* automatic, backtracking or nfa engine */
    unsigned            re_flags;    /* second argument for vim_regcomp() */
} regprog_T;

/*
 * Structure used by the back track matcher.
 * These fields are only to be used in regexp.c!
 * See regexp.c for an explanation.
 */
typedef struct
{
    /* These four members implement regprog_T */
    regengine_T         *engine;
    unsigned            regflags;
    unsigned            re_engine;
    unsigned            re_flags;    /* second argument for vim_regcomp() */

    int                 regstart;
    char_u              reganch;
    char_u              *regmust;
    int                 regmlen;
    char_u              reghasz;
    char_u              program[1];     /* actually longer.. */
} bt_regprog_T;

/*
 * Structure representing a NFA state.
 * A NFA state may have no outgoing edge, when it is a NFA_MATCH state.
 */
typedef struct nfa_state nfa_state_T;
struct nfa_state
{
    int                 c;
    nfa_state_T         *out;
    nfa_state_T         *out1;
    int                 id;
    int                 lastlist[2]; /* 0: normal, 1: recursive */
    int                 val;
};

/*
 * Structure used by the NFA matcher.
 */
typedef struct
{
    /* These three members implement regprog_T */
    regengine_T         *engine;
    unsigned            regflags;
    unsigned            re_engine;
    unsigned            re_flags;    /* second argument for vim_regcomp() */

    nfa_state_T         *start;         /* points into state[] */

    int                 reganch;        /* pattern starts with ^ */
    int                 regstart;       /* char at start of pattern */
    char_u              *match_text;    /* plain text to match with */

    int                 has_zend;       /* pattern contains \ze */
    int                 has_backref;    /* pattern contains \1 .. \9 */
    int                 reghasz;
    char_u              *pattern;
    int                 nsubexp;        /* number of () */
    int                 nstate;
    nfa_state_T         state[1];       /* actually longer.. */
} nfa_regprog_T;

/*
 * Structure to be used for single-line matching.
 * Sub-match "no" starts at "startp[no]" and ends just before "endp[no]".
 * When there is no match, the pointer is NULL.
 */
typedef struct
{
    regprog_T           *regprog;
    char_u              *startp[NSUBEXP];
    char_u              *endp[NSUBEXP];
    int                 rm_ic;
} regmatch_T;

/*
 * Structure to be used for multi-line matching.
 * Sub-match "no" starts in line "startpos[no].lnum" column "startpos[no].col"
 * and ends in line "endpos[no].lnum" just before column "endpos[no].col".
 * The line numbers are relative to the first line, thus startpos[0].lnum is always 0.
 * When there is no match, the line number is -1.
 */
typedef struct
{
    regprog_T           *regprog;
    lpos_T              startpos[NSUBEXP];
    lpos_T              endpos[NSUBEXP];
    int                 rmm_ic;
    colnr_T             rmm_maxcol;     /* when not zero: maximum column */
} regmmatch_T;

/*
 * Structure used to store external references: "\z\(\)" to "\z\1".
 * Use a reference count to avoid the need to copy this around.  When it goes
 * from 1 to zero the matches need to be freed.
 */
typedef struct
{
    short               refcnt;
    char_u              *matches[NSUBEXP];
} reg_extmatch_T;

struct regengine
{
    regprog_T   *(*regcomp)(char_u*, int);
    void        (*regfree)(regprog_T *);
    int         (*regexec_nl)(regmatch_T*, char_u*, colnr_T, int);
    long        (*regexec_multi)(regmmatch_T*, win_T*, buf_T*, linenr_T, colnr_T, proftime_T*);
    char_u      *expr;
};

/* ----------------------------------------------------------------------- */

/*
 * marks: positions in a file
 * (a normal mark is a lnum/col pair, the same as a file position)
 */

#define NMARKS          ('z' - 'a' + 1) /* max. # of named marks */
#define JUMPLISTSIZE    100             /* max. # of marks in jump list */

typedef struct filemark
{
    pos_T       mark;           /* cursor position */
    int         fnum;           /* file number */
} fmark_T;

/* Xtended file mark: also has a file name */
typedef struct xfilemark
{
    fmark_T     fmark;
    char_u      *fname;         /* file name, used when fnum == 0 */
} xfmark_T;

/*
 * Structure that contains all options that are local to a window.
 * Used twice in a window: for the current buffer and for all buffers.
 * Also used in wininfo_T.
 */
typedef struct
{
    int         wo_bri;
#define w_p_bri w_onebuf_opt.wo_bri    /* 'breakindent' */
    char_u      *wo_briopt;
#define w_p_briopt w_onebuf_opt.wo_briopt /* 'breakindentopt' */
    int         wo_lbr;
#define w_p_lbr w_onebuf_opt.wo_lbr    /* 'linebreak' */
    int         wo_list;
#define w_p_list w_onebuf_opt.wo_list   /* 'list' */
    int         wo_nu;
#define w_p_nu w_onebuf_opt.wo_nu       /* 'number' */
    int         wo_rnu;
#define w_p_rnu w_onebuf_opt.wo_rnu     /* 'relativenumber' */
    long        wo_nuw;
#define w_p_nuw w_onebuf_opt.wo_nuw    /* 'numberwidth' */
    int         wo_wfh;
#define w_p_wfh w_onebuf_opt.wo_wfh    /* 'winfixheight' */
    int         wo_wfw;
#define w_p_wfw w_onebuf_opt.wo_wfw    /* 'winfixwidth' */
    int         wo_rl;
#define w_p_rl w_onebuf_opt.wo_rl      /* 'rightleft' */
    char_u      *wo_rlc;
#define w_p_rlc w_onebuf_opt.wo_rlc    /* 'rightleftcmd' */
    long        wo_scr;
#define w_p_scr w_onebuf_opt.wo_scr     /* 'scroll' */
    int         wo_cuc;
#define w_p_cuc w_onebuf_opt.wo_cuc    /* 'cursorcolumn' */
    int         wo_cul;
#define w_p_cul w_onebuf_opt.wo_cul    /* 'cursorline' */
    char_u      *wo_cc;
#define w_p_cc w_onebuf_opt.wo_cc      /* 'colorcolumn' */
    char_u      *wo_stl;
#define w_p_stl w_onebuf_opt.wo_stl     /* 'statusline' */
    int         wo_scb;
#define w_p_scb w_onebuf_opt.wo_scb    /* 'scrollbind' */
    int         wo_diff_saved; /* options were saved for starting diff mode */
#define w_p_diff_saved w_onebuf_opt.wo_diff_saved
    int         wo_scb_save;    /* 'scrollbind' saved for diff mode */
#define w_p_scb_save w_onebuf_opt.wo_scb_save
    int         wo_wrap;
#define w_p_wrap w_onebuf_opt.wo_wrap   /* 'wrap' */
    char_u      *wo_cocu;               /* 'concealcursor' */
#define w_p_cocu w_onebuf_opt.wo_cocu
    long        wo_cole;                /* 'conceallevel' */
#define w_p_cole w_onebuf_opt.wo_cole
    int         wo_crb;
#define w_p_crb w_onebuf_opt.wo_crb    /* 'cursorbind' */
    int         wo_crb_save;    /* 'cursorbind' state saved for diff mode */
#define w_p_crb_save w_onebuf_opt.wo_crb_save

    int         wo_scriptID[WV_COUNT];  /* SIDs for window-local options */
#define w_p_scriptID w_onebuf_opt.wo_scriptID
} winopt_T;

/*
 * Window info stored with a buffer.
 *
 * Two types of info are kept for a buffer which are associated with a specific window:
 * 1. Each window can have a different line number associated with a buffer.
 * 2. The window-local options for a buffer work in a similar way.
 * The window-info is kept in a list at b_wininfo.  It is kept in
 * most-recently-used order.
 */
struct wininfo_S
{
    wininfo_T   *wi_next;       /* next entry or NULL for last entry */
    wininfo_T   *wi_prev;       /* previous entry or NULL for first entry */
    win_T       *wi_win;        /* pointer to window that did set wi_fpos */
    pos_T       wi_fpos;        /* last cursor position in the file */
    int         wi_optset;      /* TRUE when wi_opt has useful values */
    winopt_T    wi_opt;         /* local window options */
};

/* Structure to store info about the Visual area. */
typedef struct
{
    pos_T       vi_start;       /* start pos of last VIsual */
    pos_T       vi_end;         /* end position of last VIsual */
    int         vi_mode;        /* VIsual_mode of last VIsual */
    colnr_T     vi_curswant;    /* MAXCOL from w_curswant */
} visualinfo_T;

/*
 * structures used for undo
 */

typedef struct u_entry u_entry_T;
typedef struct u_header u_header_T;
struct u_entry
{
    u_entry_T   *ue_next;       /* pointer to next entry in list */
    linenr_T    ue_top;         /* number of line above undo block */
    linenr_T    ue_bot;         /* number of line below undo block */
    linenr_T    ue_lcount;      /* linecount when u_save called */
    char_u      **ue_array;     /* array of lines in undo block */
    long        ue_size;        /* number of lines in ue_array */
};

struct u_header
{
    /* The following have a pointer and a number. The number is used when
     * reading the undo file in u_read_undo() */
    union {
        u_header_T *ptr;        /* pointer to next undo header in list */
        long       seq;
    } uh_next;
    union {
        u_header_T *ptr;        /* pointer to previous header in list */
        long       seq;
    } uh_prev;
    union {
        u_header_T *ptr;        /* pointer to next header for alt. redo */
        long       seq;
    } uh_alt_next;
    union {
        u_header_T *ptr;        /* pointer to previous header for alt. redo */
        long       seq;
    } uh_alt_prev;
    long        uh_seq;         /* sequence number, higher == newer undo */
    int         uh_walk;        /* used by undo_time() */
    u_entry_T   *uh_entry;      /* pointer to first entry */
    u_entry_T   *uh_getbot_entry; /* pointer to where ue_bot must be set */
    pos_T       uh_cursor;      /* cursor position before saving */
    long        uh_cursor_vcol;
    int         uh_flags;       /* see below */
    pos_T       uh_namedm[NMARKS];      /* marks before undo/after redo */
    visualinfo_T uh_visual;     /* Visual areas before undo/after redo */
    time_t      uh_time;        /* timestamp when the change was made */
    long        uh_save_nr;     /* set when the file was saved after the
                                   changes in this block */
};

/* values for uh_flags */
#define UH_CHANGED  0x01        /* b_changed flag before undo/after redo */
#define UH_EMPTYBUF 0x02        /* buffer was empty */

/*
 * structures used in undo.c
 */
#define ALIGN_LONG     /* longword alignment and use filler byte */
#define ALIGN_SIZE (sizeof(long))

#define ALIGN_MASK (ALIGN_SIZE - 1)

typedef struct m_info minfo_T;

/*
 * structure used to link chunks in one of the free chunk lists.
 */
struct m_info
{
#if defined(ALIGN_LONG)
    long_u      m_size;         /* size of the chunk (including m_info) */
#else
    short_u     m_size;         /* size of the chunk (including m_info) */
#endif
    minfo_T     *m_next;        /* pointer to next free chunk in the list */
};

/*
 * things used in memfile.c
 */

typedef struct block_hdr    bhdr_T;
typedef struct memfile      memfile_T;
typedef long                blocknr_T;

/*
 * mf_hashtab_T is a chained hashtable with blocknr_T key and arbitrary
 * structures as items.  This is an intrusive data structure: we require
 * that items begin with mf_hashitem_T which contains the key and linked
 * list pointers.  List of items in each bucket is doubly-linked.
 */

typedef struct mf_hashitem_S mf_hashitem_T;

struct mf_hashitem_S
{
    mf_hashitem_T   *mhi_next;
    mf_hashitem_T   *mhi_prev;
    blocknr_T       mhi_key;
};

#define MHT_INIT_SIZE   64

typedef struct mf_hashtab_S
{
    long_u          mht_mask;       /* mask used for hash value (nr of items
                                     * in array is "mht_mask" + 1) */
    long_u          mht_count;      /* nr of items inserted into hashtable */
    mf_hashitem_T   **mht_buckets;  /* points to mht_small_buckets or
                                     *dynamically allocated array */
    mf_hashitem_T   *mht_small_buckets[MHT_INIT_SIZE];   /* initial buckets */
    char            mht_fixed;      /* non-zero value forbids growth */
} mf_hashtab_T;

/*
 * for each (previously) used block in the memfile there is one block header.
 *
 * The block may be linked in the used list OR in the free list.
 * The used blocks are also kept in hash lists.
 *
 * The used list is a doubly linked list, most recently used block first.
 *      The blocks in the used list have a block of memory allocated.
 *      mf_used_count is the number of pages in the used list.
 * The hash lists are used to quickly find a block in the used list.
 * The free list is a single linked list, not sorted.
 *      The blocks in the free list have no block of memory allocated and
 *      the contents of the block in the file (if any) is irrelevant.
 */

struct block_hdr
{
    mf_hashitem_T bh_hashitem;      /* header for hash table and key */
#define bh_bnum bh_hashitem.mhi_key /* block number, part of bh_hashitem */

    bhdr_T      *bh_next;           /* next block_hdr in free or used list */
    bhdr_T      *bh_prev;           /* previous block_hdr in used list */
    char_u      *bh_data;           /* pointer to memory (for used block) */
    int         bh_page_count;      /* number of pages in this block */

#define BH_DIRTY    1
#define BH_LOCKED   2
    char        bh_flags;           /* BH_DIRTY or BH_LOCKED */
};

/*
 * when a block with a negative number is flushed to the file, it gets
 * a positive number. Because the reference to the block is still the negative
 * number, we remember the translation to the new positive number in the
 * double linked trans lists. The structure is the same as the hash lists.
 */
typedef struct nr_trans NR_TRANS;

struct nr_trans
{
    mf_hashitem_T nt_hashitem;          /* header for hash table and key */
#define nt_old_bnum nt_hashitem.mhi_key /* old, negative, number */

    blocknr_T   nt_new_bnum;            /* new, positive, number */
};

typedef struct buffblock buffblock_T;
typedef struct buffheader buffheader_T;

/*
 * structure used to store one block of the stuff/redo/recording buffers
 */
struct buffblock
{
    buffblock_T *b_next;        /* pointer to next buffblock */
    char_u      b_str[1];       /* contents (actually longer) */
};

/*
 * header used for the stuff buffer and the redo buffer
 */
struct buffheader
{
    buffblock_T bh_first;       /* first (dummy) block of list */
    buffblock_T *bh_curr;       /* buffblock for appending */
    int         bh_index;       /* index for reading */
    int         bh_space;       /* space in bh_curr for appending */
};

/*
 * used for completion on the command line
 */
typedef struct expand
{
    int         xp_context;             /* type of expansion */
    char_u      *xp_pattern;            /* start of item to expand */
    int         xp_pattern_len;         /* bytes in xp_pattern before cursor */
    char_u      *xp_arg;                /* completion function */
    int         xp_scriptID;            /* SID for completion function */
    int         xp_backslash;           /* one of the XP_BS_ values */
    int         xp_shell;               /* TRUE for a shell command, more
                                           characters need to be escaped */
    int         xp_numfiles;            /* number of files found by
                                                    file name completion */
    char_u      **xp_files;             /* list of files */
    char_u      *xp_line;               /* text being completed */
    int         xp_col;                 /* cursor position in line */
} expand_T;

/* values for xp_backslash */
#define XP_BS_NONE      0       /* nothing special for backslashes */
#define XP_BS_ONE       1       /* uses one backslash before a space */
#define XP_BS_THREE     2       /* uses three backslashes before a space */

/*
 * Command modifiers ":vertical", ":browse", ":confirm" and ":hide" set a flag.
 * This needs to be saved for recursive commands, put them in a structure for easy manipulation.
 */
typedef struct
{
    int         hide;                   /* TRUE when ":hide" was used */
    int         split;                  /* flags for win_split() */
    int         tab;                    /* > 0 when ":tab" was used */
    int         confirm;                /* TRUE to invoke yes/no dialog */
    int         keepalt;                /* TRUE when ":keepalt" was used */
    int         keepmarks;              /* TRUE when ":keepmarks" was used */
    int         keepjumps;              /* TRUE when ":keepjumps" was used */
    int         lockmarks;              /* TRUE when ":lockmarks" was used */
    int         keeppatterns;           /* TRUE when ":keeppatterns" was used */
    int         noswapfile;             /* TRUE when ":noswapfile" was used */
    char_u      *save_ei;               /* saved value of 'eventignore' */
} cmdmod_T;

#define MF_SEED_LEN     8

struct memfile
{
    char_u      *mf_fname;              /* name of the file */
    char_u      *mf_ffname;             /* idem, full path */
    int         mf_fd;                  /* file descriptor */
    bhdr_T      *mf_free_first;         /* first block_hdr in free list */
    bhdr_T      *mf_used_first;         /* mru block_hdr in used list */
    bhdr_T      *mf_used_last;          /* lru block_hdr in used list */
    unsigned    mf_used_count;          /* number of pages in used list */
    unsigned    mf_used_count_max;      /* maximum number of pages in memory */
    mf_hashtab_T mf_hash;               /* hash lists */
    mf_hashtab_T mf_trans;              /* trans lists */
    blocknr_T   mf_blocknr_max;         /* highest positive block number + 1 */
    blocknr_T   mf_blocknr_min;         /* lowest negative block number - 1 */
    blocknr_T   mf_neg_count;           /* number of negative blocks numbers */
    blocknr_T   mf_infile_count;        /* number of pages in the file */
    unsigned    mf_page_size;           /* number of bytes in a page */
    int         mf_dirty;               /* TRUE if there are dirty blocks */
};

/*
 * things used in memline.c
 */
/*
 * When searching for a specific line, we remember what blocks in the tree
 * are the branches leading to that block. This is stored in ml_stack.  Each
 * entry is a pointer to info in a block (may be data block or pointer block)
 */
typedef struct info_pointer
{
    blocknr_T   ip_bnum;        /* block number */
    linenr_T    ip_low;         /* lowest lnum in this block */
    linenr_T    ip_high;        /* highest lnum in this block */
    int         ip_index;       /* index for block with current lnum */
} infoptr_T;    /* block/index pair */

typedef struct ml_chunksize
{
    int         mlcs_numlines;
    long        mlcs_totalsize;
} chunksize_T;

 /* Flags when calling ml_updatechunk() */

#define ML_CHNK_ADDLINE 1
#define ML_CHNK_DELLINE 2
#define ML_CHNK_UPDLINE 3

/*
 * the memline structure holds all the information about a memline
 */
typedef struct memline
{
    linenr_T    ml_line_count;  /* number of lines in the buffer */

    memfile_T   *ml_mfp;        /* pointer to associated memfile */

#define ML_EMPTY        1       /* empty buffer */
#define ML_LINE_DIRTY   2       /* cached line was changed and allocated */
#define ML_LOCKED_DIRTY 4       /* ml_locked was changed */
#define ML_LOCKED_POS   8       /* ml_locked needs positive block number */
    int         ml_flags;

    infoptr_T   *ml_stack;      /* stack of pointer blocks (array of IPTRs) */
    int         ml_stack_top;   /* current top if ml_stack */
    int         ml_stack_size;  /* total number of entries in ml_stack */

    linenr_T    ml_line_lnum;   /* line number of cached line, 0 if not valid */
    char_u      *ml_line_ptr;   /* pointer to cached line */

    bhdr_T      *ml_locked;     /* block used by last ml_get */
    linenr_T    ml_locked_low;  /* first line in ml_locked */
    linenr_T    ml_locked_high; /* last line in ml_locked */
    int         ml_locked_lineadd;  /* number of lines inserted in ml_locked */
    chunksize_T *ml_chunksize;
    int         ml_numchunks;
    int         ml_usedchunks;
} memline_T;

/*
 * Argument list: Array of file names.
 * Used for the global argument list and the argument lists local to a window.
 */
typedef struct arglist
{
    garray_T    al_ga;          /* growarray with the array of file names */
    int         al_refcount;    /* number of windows using this arglist */
    int         id;             /* id of this arglist */
} alist_T;

/*
 * For each argument remember the file name as it was given, and the buffer
 * number that contains the expanded file name (required for when ":cd" is used.
 */
typedef struct argentry
{
    char_u      *ae_fname;      /* file name as specified */
    int         ae_fnum;        /* buffer number with expanded file name */
} aentry_T;

#define ALIST(win) (win)->w_alist
#define GARGLIST        ((aentry_T *)global_alist.al_ga.ga_data)
#define ARGLIST         ((aentry_T *)ALIST(curwin)->al_ga.ga_data)
#define WARGLIST(wp)    ((aentry_T *)ALIST(wp)->al_ga.ga_data)
#define AARGLIST(al)    ((aentry_T *)((al)->al_ga.ga_data))
#define GARGCOUNT       (global_alist.al_ga.ga_len)
#define ARGCOUNT        (ALIST(curwin)->al_ga.ga_len)
#define WARGCOUNT(wp)   (ALIST(wp)->al_ga.ga_len)

/*
 * A list used for saving values of "emsg_silent".  Used by ex_try() to save the
 * value of "emsg_silent" if it was non-zero.  When this is done, the CSF_SILENT flag below is set.
 */

typedef struct eslist_elem eslist_T;
struct eslist_elem
{
    int         saved_emsg_silent;      /* saved value of "emsg_silent" */
    eslist_T    *next;                  /* next element on the list */
};

/*
 * For conditional commands a stack is kept of nested conditionals.
 * When cs_idx < 0, there is no conditional command.
 */
#define CSTACK_LEN      50

struct condstack
{
    short       cs_flags[CSTACK_LEN];   /* CSF_ flags */
    char        cs_pending[CSTACK_LEN]; /* CSTP_: what's pending in ":finally" */
    union {
        void    *csp_rv[CSTACK_LEN];    /* return typeval for pending return */
        void    *csp_ex[CSTACK_LEN];    /* exception for pending throw */
    }           cs_pend;
    void        *cs_forinfo[CSTACK_LEN]; /* info used by ":for" */
    int         cs_line[CSTACK_LEN];    /* line nr of ":while"/":for" line */
    int         cs_idx;                 /* current entry, or -1 if none */
    int         cs_looplevel;           /* nr of nested ":while"s and ":for"s */
    int         cs_trylevel;            /* nr of nested ":try"s */
    eslist_T    *cs_emsg_silent_list;   /* saved values of "emsg_silent" */
    char        cs_lflags;              /* loop flags: CSL_ flags */
};
#define cs_rettv       cs_pend.csp_rv
#define cs_exception   cs_pend.csp_ex

/* There is no CSF_IF, the lack of CSF_WHILE, CSF_FOR and CSF_TRY means ":if"
 * was used. */
#define CSF_TRUE       0x0001  /* condition was TRUE */
#define CSF_ACTIVE     0x0002  /* current state is active */
#define CSF_ELSE       0x0004  /* ":else" has been passed */
#define CSF_WHILE      0x0008  /* is a ":while" */
#define CSF_FOR        0x0010  /* is a ":for" */

#define CSF_TRY        0x0100  /* is a ":try" */
#define CSF_FINALLY    0x0200  /* ":finally" has been passed */
#define CSF_THROWN     0x0400  /* exception thrown to this try conditional */
#define CSF_CAUGHT     0x0800  /* exception caught by this try conditional */
#define CSF_SILENT     0x1000  /* "emsg_silent" reset by ":try" */
/* Note that CSF_ELSE is only used when CSF_TRY and CSF_WHILE are unset
 * (an ":if"), and CSF_SILENT is only used when CSF_TRY is set. */

/*
 * What's pending for being reactivated at the ":endtry" of this try conditional:
 */
#define CSTP_NONE      0       /* nothing pending in ":finally" clause */
#define CSTP_ERROR     1       /* an error is pending */
#define CSTP_INTERRUPT 2       /* an interrupt is pending */
#define CSTP_THROW     4       /* a throw is pending */
#define CSTP_BREAK     8       /* ":break" is pending */
#define CSTP_CONTINUE  16      /* ":continue" is pending */
#define CSTP_RETURN    24      /* ":return" is pending */
#define CSTP_FINISH    32      /* ":finish" is pending */

/*
 * Flags for the cs_lflags item in struct condstack.
 */
#define CSL_HAD_LOOP    1      /* just found ":while" or ":for" */
#define CSL_HAD_ENDLOOP 2      /* just found ":endwhile" or ":endfor" */
#define CSL_HAD_CONT    4      /* just found ":continue" */
#define CSL_HAD_FINA    8      /* just found ":finally" */

/*
 * A list of error messages that can be converted to an exception.  "throw_msg"
 * is only set in the first element of the list.  Usually, it points to the
 * original message stored in that element, but sometimes it points to a later
 * message in the list.  See cause_errthrow() below.
 */
struct msglist
{
    char_u              *msg;           /* original message */
    char_u              *throw_msg;     /* msg to throw: usually original one */
    struct msglist      *next;          /* next of several messages in a row */
};

/*
 * Structure describing an exception.
 * (don't use "struct exception", it's used by the math library).
 */
typedef struct vim_exception except_T;
struct vim_exception
{
    int                 type;           /* exception type */
    char_u              *value;         /* exception value */
    struct msglist      *messages;      /* message(s) causing error exception */
    char_u              *throw_name;    /* name of the throw point */
    linenr_T            throw_lnum;     /* line number of the throw point */
    except_T            *caught;        /* next exception on the caught stack */
};

/*
 * The exception types.
 */
#define ET_USER         0       /* exception caused by ":throw" command */
#define ET_ERROR        1       /* error exception */
#define ET_INTERRUPT    2       /* interrupt exception triggered by Ctrl-C */

/*
 * Structure to save the error/interrupt/exception state between calls to
 * enter_cleanup() and leave_cleanup().  Must be allocated as an automatic
 * variable by the (common) caller of these functions.
 */
typedef struct cleanup_stuff cleanup_T;
struct cleanup_stuff
{
    int pending;                /* error/interrupt/exception state */
    except_T *exception;        /* exception value */
};

/* struct passed to in_id_list() */
struct sp_syn
{
    int         inc_tag;        /* ":syn include" unique tag */
    short       id;             /* highlight group ID of item */
    short       *cont_in_list;  /* cont.in group IDs, if non-zero */
};

/*
 * Each keyword has one keyentry, which is linked in a hash list.
 */
typedef struct keyentry keyentry_T;

struct keyentry
{
    keyentry_T  *ke_next;       /* next entry with identical "keyword[]" */
    struct sp_syn k_syn;        /* struct passed to in_id_list() */
    short       *next_list;     /* ID list for next match (if non-zero) */
    int         flags;
    int         k_char;         /* conceal substitute character */
    char_u      keyword[1];     /* actually longer */
};

/*
 * Struct used to store one state of the state stack.
 */
typedef struct buf_state
{
    int             bs_idx;      /* index of pattern */
    int             bs_flags;    /* flags for pattern */
    int             bs_seqnr;    /* stores si_seqnr */
    int             bs_cchar;    /* stores si_cchar */
    reg_extmatch_T *bs_extmatch; /* external matches from start pattern */
} bufstate_T;

/*
 * syn_state contains the syntax state stack for the start of one line.
 * Used by b_sst_array[].
 */
typedef struct syn_state synstate_T;

struct syn_state
{
    synstate_T  *sst_next;      /* next entry in used or free list */
    linenr_T    sst_lnum;       /* line number for this state */
    union
    {
        bufstate_T      sst_stack[SST_FIX_STATES]; /* short state stack */
        garray_T        sst_ga; /* growarray for long state stack */
    } sst_union;
    int         sst_next_flags; /* flags for sst_next_list */
    int         sst_stacksize;  /* number of states on the stack */
    short       *sst_next_list; /* "nextgroup" list in this state
                                 * (this is a copy, don't free it! */
    disptick_T  sst_tick;       /* tick when last displayed */
    linenr_T    sst_change_lnum;/* when non-zero, change in this line
                                 * may have made the state invalid */
};

/*
 * Structure shared between syntax.c, screen.c and gui_x11.c.
 */
typedef struct attr_entry
{
    short           ae_attr;            /* HL_BOLD, etc. */
    union
    {
        struct
        {
            char_u          *start;     /* start escape sequence */
            char_u          *stop;      /* stop escape sequence */
        } term;
        struct
        {
            /* These colors need to be > 8 bits to hold 256. */
            short_u         fg_color;   /* foreground color number */
            short_u         bg_color;   /* background color number */
        } cterm;
    } ae_u;
} attrentry_T;

#if defined(USE_ICONV)
#include <iconv.h>
#endif

/*
 * Used for the typeahead buffer: typebuf.
 */
typedef struct
{
    char_u      *tb_buf;        /* buffer for typed characters */
    char_u      *tb_noremap;    /* mapping flags for characters in tb_buf[] */
    int         tb_buflen;      /* size of tb_buf[] */
    int         tb_off;         /* current position in tb_buf[] */
    int         tb_len;         /* number of valid bytes in tb_buf[] */
    int         tb_maplen;      /* nr of mapped bytes in tb_buf[] */
    int         tb_silent;      /* nr of silently mapped bytes in tb_buf[] */
    int         tb_no_abbr_cnt; /* nr of bytes without abbrev. in tb_buf[] */
    int         tb_change_cnt;  /* nr of time tb_buf was changed; never zero */
} typebuf_T;

/* Struct to hold the saved typeahead for save_typeahead(). */
typedef struct
{
    typebuf_T           save_typebuf;
    int                 typebuf_valid;      /* TRUE when save_typebuf valid */
    int                 old_char;
    int                 old_mod_mask;
    buffheader_T        save_readbuf1;
    buffheader_T        save_readbuf2;
    char_u              *save_inputbuf;
} tasave_T;

/*
 * Used for conversion of terminal I/O and script files.
 */
typedef struct
{
    int         vc_type;        /* zero or one of the CONV_ values */
    int         vc_factor;      /* max. expansion factor */
#if defined(USE_ICONV)
    iconv_t     vc_fd;          /* for CONV_ICONV */
#endif
    int         vc_fail;        /* fail for invalid char, don't use '?' */
} vimconv_T;

/*
 * Structure used for reading from the viminfo file.
 */
typedef struct
{
    char_u      *vir_line;      /* text of the current line */
    FILE        *vir_fd;        /* file descriptor */
    vimconv_T   vir_conv;       /* encoding conversion */
} vir_T;

#define CONV_NONE               0
#define CONV_TO_UTF8            1
#define CONV_9_TO_UTF8          2
#define CONV_TO_LATIN1          3
#define CONV_TO_LATIN9          4
#define CONV_ICONV              5

/*
 * Structure used for mappings and abbreviations.
 */
typedef struct mapblock mapblock_T;
struct mapblock
{
    mapblock_T  *m_next;        /* next mapblock in list */
    char_u      *m_keys;        /* mapped from, lhs */
    char_u      *m_str;         /* mapped to, rhs */
    char_u      *m_orig_str;    /* rhs as entered by the user */
    int         m_keylen;       /* strlen(m_keys) */
    int         m_mode;         /* valid mode */
    int         m_noremap;      /* if non-zero no re-mapping for m_str */
    char        m_silent;       /* <silent> used, don't echo commands */
    char        m_nowait;       /* <nowait> used */
    char        m_expr;         /* <expr> used, m_str is an expression */
    scid_T      m_script_ID;    /* ID of script where map was defined */
};

/*
 * Used for highlighting in the status line.
 */
struct stl_hlrec
{
    char_u      *start;
    int         userhl;         /* 0: no HL, 1-9: User HL, < 0 for syn ID */
};

/*
 * Syntax items - usually buffer-specific.
 */

/* Item for a hashtable.  "hi_key" can be one of three values:
 * NULL:           Never been used
 * HI_KEY_REMOVED: Entry was removed
 * Otherwise:      Used item, pointer to the actual key; this usually is
 *                 inside the item, subtract an offset to locate the item.
 *                 This reduces the size of hashitem by 1/3.
 */
typedef struct hashitem_S
{
    long_u      hi_hash;        /* cached hash number of hi_key */
    char_u      *hi_key;
} hashitem_T;

/* The address of "hash_removed" is used as a magic number for hi_key to
 * indicate a removed item. */
#define HI_KEY_REMOVED &hash_removed
#define HASHITEM_EMPTY(hi) ((hi)->hi_key == NULL || (hi)->hi_key == &hash_removed)

/* Initial size for a hashtable.  Our items are relatively small and growing
 * is expensive, thus use 16 as a start.  Must be a power of 2. */
#define HT_INIT_SIZE 16

typedef struct hashtable_S
{
    long_u      ht_mask;        /* mask used for hash value (nr of items in
                                 * array is "ht_mask" + 1) */
    long_u      ht_used;        /* number of items used */
    long_u      ht_filled;      /* number of items used + removed */
    int         ht_locked;      /* counter for hash_lock() */
    int         ht_error;       /* when set growing failed, can't add more
                                   items before growing works */
    hashitem_T  *ht_array;      /* points to the array, allocated when it's
                                   not "ht_smallarray" */
    hashitem_T  ht_smallarray[HT_INIT_SIZE];   /* initial array */
} hashtab_T;

typedef long_u hash_T;          /* Type for hi_hash */

typedef int     varnumber_T;
typedef double  float_T;

typedef struct listvar_S list_T;
typedef struct dictvar_S dict_T;

/*
 * Structure to hold an internal variable without a name.
 */
typedef struct
{
    char        v_type;     /* see below: VAR_NUMBER, VAR_STRING, etc. */
    char        v_lock;     /* see below: VAR_LOCKED, VAR_FIXED */
    union
    {
        varnumber_T     v_number;       /* number value */
        float_T         v_float;        /* floating number value */
        char_u          *v_string;      /* string value (can be NULL!) */
        list_T          *v_list;        /* list value (can be NULL!) */
        dict_T          *v_dict;        /* dict value (can be NULL!) */
    }           vval;
} typval_T;

/* Values for "v_type". */
#define VAR_UNKNOWN 0
#define VAR_NUMBER  1   /* "v_number" is used */
#define VAR_STRING  2   /* "v_string" is used */
#define VAR_FUNC    3   /* "v_string" is function name */
#define VAR_LIST    4   /* "v_list" is used */
#define VAR_DICT    5   /* "v_dict" is used */
#define VAR_FLOAT   6   /* "v_float" is used */

/* Values for "dv_scope". */
#define VAR_SCOPE     1 /* a:, v:, s:, etc. scope dictionaries */
#define VAR_DEF_SCOPE 2 /* l:, g: scope dictionaries: here funcrefs are not
                           allowed to mask existing functions */

/* Values for "v_lock". */
#define VAR_LOCKED  1   /* locked with lock(), can use unlock() */
#define VAR_FIXED   2   /* locked forever */

/*
 * Structure to hold an item of a list: an internal variable without a name.
 */
typedef struct listitem_S listitem_T;

struct listitem_S
{
    listitem_T  *li_next;       /* next item in list */
    listitem_T  *li_prev;       /* previous item in list */
    typval_T    li_tv;          /* type and value of the variable */
};

/*
 * Struct used by those that are using an item in a list.
 */
typedef struct listwatch_S listwatch_T;

struct listwatch_S
{
    listitem_T          *lw_item;       /* item being watched */
    listwatch_T         *lw_next;       /* next watcher */
};

/*
 * Structure to hold info about a list.
 */
struct listvar_S
{
    listitem_T  *lv_first;      /* first item, NULL if none */
    listitem_T  *lv_last;       /* last item, NULL if none */
    int         lv_refcount;    /* reference count */
    int         lv_len;         /* number of items */
    listwatch_T *lv_watch;      /* first watcher, NULL if none */
    int         lv_idx;         /* cached index of an item */
    listitem_T  *lv_idx_item;   /* when not NULL item at index "lv_idx" */
    int         lv_copyID;      /* ID used by deepcopy() */
    list_T      *lv_copylist;   /* copied list used by deepcopy() */
    char        lv_lock;        /* zero, VAR_LOCKED, VAR_FIXED */
    list_T      *lv_used_next;  /* next list in used lists list */
    list_T      *lv_used_prev;  /* previous list in used lists list */
};

/*
 * Structure to hold an item of a Dictionary.
 * Also used for a variable.
 * The key is copied into "di_key" to avoid an extra alloc/free for it.
 */
struct dictitem_S
{
    typval_T    di_tv;          /* type and value of the variable */
    char_u      di_flags;       /* flags (only used for variable) */
    char_u      di_key[1];      /* key (actually longer!) */
};

typedef struct dictitem_S dictitem_T;

#define DI_FLAGS_RO     1 /* "di_flags" value: read-only variable */
#define DI_FLAGS_RO_SBX 2 /* "di_flags" value: read-only in the sandbox */
#define DI_FLAGS_FIX    4 /* "di_flags" value: fixed variable, not allocated */
#define DI_FLAGS_LOCK   8 /* "di_flags" value: locked variable */

/*
 * Structure to hold info about a Dictionary.
 */
struct dictvar_S
{
    char        dv_lock;        /* zero, VAR_LOCKED, VAR_FIXED */
    char        dv_scope;       /* zero, VAR_SCOPE, VAR_DEF_SCOPE */
    int         dv_refcount;    /* reference count */
    int         dv_copyID;      /* ID used by deepcopy() */
    hashtab_T   dv_hashtab;     /* hashtab that refers to the items */
    dict_T      *dv_copydict;   /* copied dict used by deepcopy() */
    dict_T      *dv_used_next;  /* next dict in used dicts list */
    dict_T      *dv_used_prev;  /* previous dict in used dicts list */
};

/* structure used for explicit stack while garbage collecting hash tables */
typedef struct ht_stack_S
{
    hashtab_T           *ht;
    struct ht_stack_S   *prev;
} ht_stack_T;

/* structure used for explicit stack while garbage collecting lists */
typedef struct list_stack_S
{
    list_T              *list;
    struct list_stack_S *prev;
} list_stack_T;

/* values for b_syn_spell: what to do with toplevel text */
#define SYNSPL_DEFAULT  0       /* spell check if @Spell not defined */
#define SYNSPL_TOP      1       /* spell check toplevel text */
#define SYNSPL_NOTOP    2       /* don't spell check toplevel text */

/*
 * These are items normally related to a buffer.  But when using ":ownsyntax"
 * a window may have its own instance.
 */
typedef struct {
    hashtab_T   b_keywtab;              /* syntax keywords hash table */
    hashtab_T   b_keywtab_ic;           /* idem, ignore case */
    int         b_syn_error;            /* TRUE when error occurred in HL */
    int         b_syn_ic;               /* ignore case for :syn cmds */
    int         b_syn_spell;            /* SYNSPL_ values */
    garray_T    b_syn_patterns;         /* table for syntax patterns */
    garray_T    b_syn_clusters;         /* table for syntax clusters */
    int         b_spell_cluster_id;     /* @Spell cluster ID or 0 */
    int         b_nospell_cluster_id;   /* @NoSpell cluster ID or 0 */
    int         b_syn_containedin;      /* TRUE when there is an item with a
                                           "containedin" argument */
    int         b_syn_sync_flags;       /* flags about how to sync */
    short       b_syn_sync_id;          /* group to sync on */
    long        b_syn_sync_minlines;    /* minimal sync lines offset */
    long        b_syn_sync_maxlines;    /* maximal sync lines offset */
    long        b_syn_sync_linebreaks;  /* offset for multi-line pattern */
    char_u      *b_syn_linecont_pat;    /* line continuation pattern */
    regprog_T   *b_syn_linecont_prog;   /* line continuation program */
    int         b_syn_linecont_ic;      /* ignore-case flag for above */
    int         b_syn_topgrp;           /* for ":syntax include" */
    int         b_syn_conceal;          /* auto-conceal for :syn cmds */
    /*
     * b_sst_array[] contains the state stack for a number of lines, for the
     * start of that line (col == 0).  This avoids having to recompute the
     * syntax state too often.
     * b_sst_array[] is allocated to hold the state for all displayed lines,
     * and states for 1 out of about 20 other lines.
     * b_sst_array      pointer to an array of synstate_T
     * b_sst_len        number of entries in b_sst_array[]
     * b_sst_first      pointer to first used entry in b_sst_array[] or NULL
     * b_sst_firstfree  pointer to first free entry in b_sst_array[] or NULL
     * b_sst_freecount  number of free entries in b_sst_array[]
     * b_sst_check_lnum entries after this lnum need to be checked for
     *                  validity (MAXLNUM means no check needed)
     */
    synstate_T  *b_sst_array;
    int         b_sst_len;
    synstate_T  *b_sst_first;
    synstate_T  *b_sst_firstfree;
    int         b_sst_freecount;
    linenr_T    b_sst_check_lnum;
    short_u     b_sst_lasttick; /* last display tick */
} synblock_T;

/*
 * buffer: structure that holds information about one file
 *
 * Several windows can share a single Buffer
 * A buffer is unallocated if there is no memfile for it.
 * A buffer is new if the associated file has never been loaded yet.
 */

struct file_buffer
{
    memline_T   b_ml;           /* associated memline (also contains line
                                   count) */

    buf_T       *b_next;        /* links in list of buffers */
    buf_T       *b_prev;

    int         b_nwindows;     /* nr of windows open on this buffer */

    int         b_flags;        /* various BF_ flags */
    int         b_closing;      /* buffer is being closed, don't let
                                   autocommands close it too. */

    /*
     * b_ffname has the full path of the file (NULL for no name).
     * b_sfname is the name as the user typed it (or NULL).
     * b_fname is the same as b_sfname, unless ":cd" has been done,
     *          then it is the same as b_ffname (NULL for no name).
     */
    char_u      *b_ffname;      /* full path file name */
    char_u      *b_sfname;      /* short file name */
    char_u      *b_fname;       /* current file name */

    int         b_dev_valid;    /* TRUE when b_dev has a valid number */
    dev_t       b_dev;          /* device number */
    ino_t       b_ino;          /* inode number */

    int         b_fnum;         /* buffer number for this file. */

    int         b_changed;      /* 'modified': Set to TRUE if something in the
                                   file has been changed and not written out. */
    int         b_changedtick;  /* incremented for each change, also for undo */

    int         b_saving;       /* Set to TRUE if we are in the middle of
                                   saving the buffer. */

    /*
     * Changes to a buffer require updating of the display.  To minimize the
     * work, remember changes made and update everything at once.
     */
    int         b_mod_set;      /* TRUE when there are changes since the last
                                   time the display was updated */
    linenr_T    b_mod_top;      /* topmost lnum that was changed */
    linenr_T    b_mod_bot;      /* lnum below last changed line, AFTER the
                                   change */
    long        b_mod_xlines;   /* number of extra buffer lines inserted;
                                   negative when lines were deleted */

    wininfo_T   *b_wininfo;     /* list of last used info for each window */

    long        b_mtime;        /* last change time of original file */
    long        b_mtime_read;   /* last change time when reading */
    off_t       b_orig_size;    /* size of original file in bytes */
    int         b_orig_mode;    /* mode of original file */

    pos_T       b_namedm[NMARKS]; /* current named marks (mark.c) */

    /* These variables are set when VIsual_active becomes FALSE */
    visualinfo_T b_visual;
    int         b_visual_mode_eval;  /* b_visual.vi_mode for visualmode() */

    pos_T       b_last_cursor;  /* cursor position when last unloading this
                                   buffer */
    pos_T       b_last_insert;  /* where Insert mode was left */
    pos_T       b_last_change;  /* position of last change: '. mark */

    /*
     * the changelist contains old change positions
     */
    pos_T       b_changelist[JUMPLISTSIZE];
    int         b_changelistlen;        /* number of active entries */
    int         b_new_change;           /* set by u_savecommon() */

    /*
     * Character table, only used in charset.c for 'iskeyword'
     * 32 bytes of 8 bits: 1 bit per character 0-255.
     */
    char_u      b_chartab[32];

    /* Table used for mappings local to a buffer. */
    mapblock_T  *(b_maphash[256]);

    /* First abbreviation local to a buffer. */
    mapblock_T  *b_first_abbr;
    /* User commands local to the buffer. */
    garray_T    b_ucmds;
    /*
     * start and end of an operator, also used for '[ and ']
     */
    pos_T       b_op_start;
    pos_T       b_op_start_orig;  /* used for Insstart_orig */
    pos_T       b_op_end;

    /*
     * The following only used in undo.c.
     */
    u_header_T  *b_u_oldhead;   /* pointer to oldest header */
    u_header_T  *b_u_newhead;   /* pointer to newest header; may not be valid
                                   if b_u_curhead is not NULL */
    u_header_T  *b_u_curhead;   /* pointer to current header */
    int         b_u_numhead;    /* current number of headers */
    int         b_u_synced;     /* entry lists are synced */
    long        b_u_seq_last;   /* last used undo sequence number */
    long        b_u_save_nr_last; /* counter for last file write */
    long        b_u_seq_cur;    /* hu_seq of header below which we are now */
    time_t      b_u_time_cur;   /* uh_time of header below which we are now */
    long        b_u_save_nr_cur; /* file write nr after which we are now */

    /*
     * variables for "U" command in undo.c
     */
    char_u      *b_u_line_ptr;  /* saved line for "U" command */
    linenr_T    b_u_line_lnum;  /* line number of line in u_line */
    colnr_T     b_u_line_colnr; /* optional column number */

    /* flags for use of ":lmap" and IM control */
    long        b_p_iminsert;   /* input mode for insert */
    long        b_p_imsearch;   /* input mode for search */
#define B_IMODE_USE_INSERT -1   /*      Use b_p_iminsert value for search */
#define B_IMODE_NONE 0          /*      Input via none */
#define B_IMODE_LMAP 1          /*      Input via langmap */
#define B_IMODE_LAST 1

    /*
     * Options local to a buffer.
     * They are here because their value depends on the type of file
     * or contents of the file being edited.
     */
    int         b_p_initialized;        /* set when options initialized */

    int         b_p_scriptID[BV_COUNT]; /* SIDs for buffer-local options */

    int         b_p_ai;         /* 'autoindent' */
    int         b_p_ai_nopaste; /* b_p_ai saved for paste mode */
    char_u      *b_p_bkc;       /* 'backupcopy' */
    unsigned    b_bkc_flags;    /* flags for 'backupcopy' */
    int         b_p_ci;         /* 'copyindent' */
    int         b_p_bin;        /* 'binary' */
    int         b_p_bomb;       /* 'bomb' */
    int         b_p_bl;         /* 'buflisted' */
    int         b_p_cin;        /* 'cindent' */
    char_u      *b_p_cino;      /* 'cinoptions' */
    char_u      *b_p_cink;      /* 'cinkeys' */
    char_u      *b_p_cinw;      /* 'cinwords' */
    char_u      *b_p_com;       /* 'comments' */
    int         b_p_eol;        /* 'endofline' */
    int         b_p_et;         /* 'expandtab' */
    int         b_p_et_nobin;   /* b_p_et saved for binary mode */
    char_u      *b_p_fenc;      /* 'fileencoding' */
    char_u      *b_p_ff;        /* 'fileformat' */
    char_u      *b_p_ft;        /* 'filetype' */
    char_u      *b_p_fo;        /* 'formatoptions' */
    char_u      *b_p_flp;       /* 'formatlistpat' */
    int         b_p_inf;        /* 'infercase' */
    char_u      *b_p_isk;       /* 'iskeyword' */
    char_u      *b_p_inde;      /* 'indentexpr' */
    long_u      b_p_inde_flags; /* flags for 'indentexpr' */
    char_u      *b_p_indk;      /* 'indentkeys' */
    char_u      *b_p_fex;       /* 'formatexpr' */
    long_u      b_p_fex_flags;  /* flags for 'formatexpr' */
    char_u      *b_p_kp;        /* 'keywordprg' */
    int         b_p_lisp;       /* 'lisp' */
    char_u      *b_p_mps;       /* 'matchpairs' */
    int         b_p_ml;         /* 'modeline' */
    int         b_p_ml_nobin;   /* b_p_ml saved for binary mode */
    int         b_p_ma;         /* 'modifiable' */
    char_u      *b_p_nf;        /* 'nrformats' */
    int         b_p_pi;         /* 'preserveindent' */
    char_u      *b_p_qe;        /* 'quoteescape' */
    int         b_p_ro;         /* 'readonly' */
    long        b_p_sw;         /* 'shiftwidth' */
    int         b_p_sn;         /* 'shortname' */
    int         b_p_si;         /* 'smartindent' */
    long        b_p_sts;        /* 'softtabstop' */
    long        b_p_sts_nopaste; /* b_p_sts saved for paste mode */
    int         b_p_swf;        /* 'swapfile' */
    long        b_p_smc;        /* 'synmaxcol' */
    char_u      *b_p_syn;       /* 'syntax' */
    long        b_p_ts;         /* 'tabstop' */
    int         b_p_tx;         /* 'textmode' */
    long        b_p_tw;         /* 'textwidth' */
    long        b_p_tw_nobin;   /* b_p_tw saved for binary mode */
    long        b_p_tw_nopaste; /* b_p_tw saved for paste mode */
    long        b_p_wm;         /* 'wrapmargin' */
    long        b_p_wm_nobin;   /* b_p_wm saved for binary mode */
    long        b_p_wm_nopaste; /* b_p_wm saved for paste mode */

    /* local values for options which are normally global */
    char_u      *b_p_ep;        /* 'equalprg' local value */
    char_u      *b_p_path;      /* 'path' local value */
    int         b_p_ar;         /* 'autoread' local value */
    long        b_p_ul;         /* 'undolevels' local value */
    int         b_p_udf;        /* 'undofile' */
    char_u      *b_p_lw;        /* 'lispwords' local value */

    /* end of buffer options */

    /* values set from b_p_cino */
    int         b_ind_level;
    int         b_ind_open_imag;
    int         b_ind_no_brace;
    int         b_ind_first_open;
    int         b_ind_open_extra;
    int         b_ind_close_extra;
    int         b_ind_open_left_imag;
    int         b_ind_jump_label;
    int         b_ind_case;
    int         b_ind_case_code;
    int         b_ind_case_break;
    int         b_ind_param;
    int         b_ind_func_type;
    int         b_ind_comment;
    int         b_ind_in_comment;
    int         b_ind_in_comment2;
    int         b_ind_cpp_baseclass;
    int         b_ind_continuation;
    int         b_ind_unclosed;
    int         b_ind_unclosed2;
    int         b_ind_unclosed_noignore;
    int         b_ind_unclosed_wrapped;
    int         b_ind_unclosed_whiteok;
    int         b_ind_matching_paren;
    int         b_ind_paren_prev;
    int         b_ind_maxparen;
    int         b_ind_maxcomment;
    int         b_ind_scopedecl;
    int         b_ind_scopedecl_code;
    int         b_ind_java;
    int         b_ind_js;
    int         b_ind_keep_case_label;
    int         b_ind_hash_comment;
    int         b_ind_cpp_namespace;
    int         b_ind_if_for_while;

    linenr_T    b_no_eol_lnum;  /* non-zero lnum when last line of next binary
                                 * write should not have an end-of-line */

    int         b_start_eol;    /* last line had eol when it was read */
    int         b_start_ffc;    /* first char of 'ff' when edit started */
    char_u      *b_start_fenc;  /* 'fileencoding' when edit started or NULL */
    int         b_bad_char;     /* "++bad=" argument when edit started or 0 */
    int         b_start_bomb;   /* 'bomb' when it was read */

    dictitem_T  b_bufvar;       /* variable for "b:" Dictionary */
    dict_T      *b_vars;        /* internal variables, local to buffer */

    /* When a buffer is created, it starts without a swap file.  b_may_swap is
     * then set to indicate that a swap file may be opened later.  It is reset
     * if a swap file could not be opened.
     */
    int         b_may_swap;
    int         b_did_warn;     /* Set to 1 if user has been warned on first
                                   change of a read-only file */

    int         b_shortname;    /* this file has an 8.3 file name */

    synblock_T  b_s;            /* Info related to syntax highlighting.  w_s
                                 * normally points to this, but some windows
                                 * may use a different synblock_T. */

    int         b_mapped_ctrl_c; /* modes where CTRL-C is mapped */
};

#define SNAP_AUCMD_IDX 0
#define SNAP_COUNT     1

/*
 * Tab pages point to the top frame of each tab page.
 * Note: Most values are NOT valid for the current tab page!  Use "curwin",
 * "firstwin", etc. for that.  "tp_topframe" is always valid and can be
 * compared against "topframe" to find the current tab page.
 */
typedef struct tabpage_S tabpage_T;
struct tabpage_S
{
    tabpage_T       *tp_next;       /* next tabpage or NULL */
    frame_T         *tp_topframe;   /* topframe for the windows */
    win_T           *tp_curwin;     /* current window in this Tab page */
    win_T           *tp_prevwin;    /* previous window in this Tab page */
    win_T           *tp_firstwin;   /* first window in this Tab page */
    win_T           *tp_lastwin;    /* last window in this Tab page */
    long            tp_old_Rows;    /* Rows when Tab page was left */
    long            tp_old_Columns; /* Columns when Tab page was left */
    long            tp_ch_used;     /* value of 'cmdheight' when frame size
                                       was set */
    frame_T         *(tp_snapshot[SNAP_COUNT]);  /* window layout snapshots */
    dictitem_T      tp_winvar;      /* variable for "t:" Dictionary */
    dict_T          *tp_vars;       /* internal variables, local to tab page */
};

/*
 * Structure to cache info for displayed lines in w_lines[].
 * Each logical line has one entry.
 * The entry tells how the logical line is currently displayed in the window.
 * This is updated when displaying the window.
 * When the display is changed (e.g., when clearing the screen) w_lines_valid
 * is changed to exclude invalid entries.
 * When making changes to the buffer, wl_valid is reset to indicate wl_size
 * may not reflect what is actually in the buffer.  When wl_valid is FALSE,
 * the entries can only be used to count the number of displayed lines used.
 * wl_lnum and wl_lastlnum are invalid too.
 */
typedef struct w_line
{
    linenr_T    wl_lnum;        /* buffer line number for logical line */
    short_u     wl_size;        /* height in screen lines */
    char        wl_valid;       /* TRUE values are valid for text in buffer */
} wline_T;

/*
 * Windows are kept in a tree of frames.  Each frame has a column (FR_COL)
 * or row (FR_ROW) layout or is a leaf, which has a window.
 */
struct frame_S
{
    char        fr_layout;      /* FR_LEAF, FR_COL or FR_ROW */
    int         fr_width;
    int         fr_newwidth;    /* new width used in win_equal_rec() */
    int         fr_height;
    int         fr_newheight;   /* new height used in win_equal_rec() */
    frame_T     *fr_parent;     /* containing frame or NULL */
    frame_T     *fr_next;       /* frame right or below in same parent, NULL
                                   for first */
    frame_T     *fr_prev;       /* frame left or above in same parent, NULL
                                   for last */
    /* fr_child and fr_win are mutually exclusive */
    frame_T     *fr_child;      /* first contained frame */
    win_T       *fr_win;        /* window that fills this frame */
};

#define FR_LEAF 0       /* frame is a leaf */
#define FR_ROW  1       /* frame with a row of windows */
#define FR_COL  2       /* frame with a column of windows */

/*
 * Struct used for highlighting 'hlsearch' matches, matches defined by
 * ":match" and matches defined by match functions.
 * For 'hlsearch' there is one pattern for all windows.  For ":match" and the
 * match functions there is a different pattern for each window.
 */
typedef struct
{
    regmmatch_T rm;     /* points to the regexp program; contains last found
                           match (may continue in next line) */
    buf_T       *buf;   /* the buffer to search for a match */
    linenr_T    lnum;   /* the line to search for a match */
    int         attr;   /* attributes to be used for a match */
    int         attr_cur; /* attributes currently active in win_line() */
    linenr_T    first_lnum;     /* first lnum to search for multi-line pat */
    colnr_T     startcol; /* in win_line() points to char where HL starts */
    colnr_T     endcol;  /* in win_line() points to char where HL ends */
    proftime_T  tm;     /* for a time limit */
} match_T;

/* number of positions supported by matchaddpos() */
#define MAXPOSMATCH 8

/*
 * Same as lpos_T, but with additional field len.
 */
typedef struct
{
    linenr_T    lnum;   /* line number */
    colnr_T     col;    /* column number */
    int         len;    /* length: 0 - to the end of line */
} llpos_T;

/*
 * posmatch_T provides an array for storing match items for matchaddpos() function.
 */
typedef struct posmatch posmatch_T;
struct posmatch
{
    llpos_T     pos[MAXPOSMATCH];       /* array of positions */
    int         cur;                    /* internal position counter */
    linenr_T    toplnum;                /* top buffer line */
    linenr_T    botlnum;                /* bottom buffer line */
};

/*
 * matchitem_T provides a linked list for storing match items for ":match" and
 * the match functions.
 */
typedef struct matchitem matchitem_T;
struct matchitem
{
    matchitem_T *next;
    int         id;         /* match ID */
    int         priority;   /* match priority */
    char_u      *pattern;   /* pattern to highlight */
    int         hlg_id;     /* highlight group ID */
    regmmatch_T match;      /* regexp program for pattern */
    posmatch_T  pos;        /* position matches */
    match_T     hl;         /* struct for doing the actual highlighting */
};

/*
 * Structure which contains all information that belongs to a window
 *
 * All row numbers are relative to the start of the window, except w_winrow.
 */
struct window_S
{
    buf_T       *w_buffer;          /* buffer we are a window into (used
                                       often, keep it the first item!) */

    synblock_T  *w_s;               /* for :ownsyntax */

    win_T       *w_prev;            /* link to previous window */
    win_T       *w_next;            /* link to next window */
    int         w_closing;          /* window is being closed, don't let
                                       autocommands close it too. */

    frame_T     *w_frame;           /* frame containing this window */

    pos_T       w_cursor;           /* cursor position in buffer */

    colnr_T     w_curswant;         /* The column we'd like to be at.  This is
                                       used to try to stay in the same column
                                       for up/down cursor motions. */

    int         w_set_curswant;     /* If set, then update w_curswant the next
                                       time through cursupdate() to the
                                       current virtual column */

    /*
     * the next six are used to update the visual part
     */
    char        w_old_visual_mode;  /* last known VIsual_mode */
    linenr_T    w_old_cursor_lnum;  /* last known end of visual part */
    colnr_T     w_old_cursor_fcol;  /* first column for block visual part */
    colnr_T     w_old_cursor_lcol;  /* last column for block visual part */
    linenr_T    w_old_visual_lnum;  /* last known start of visual part */
    colnr_T     w_old_visual_col;   /* last known start of visual part */
    colnr_T     w_old_curswant;     /* last known value of Curswant */

    /*
     * "w_topline", "w_leftcol" and "w_skipcol" specify the offsets for
     * displaying the buffer.
     */
    linenr_T    w_topline;          /* buffer line number of the line at the
                                       top of the window */
    char        w_topline_was_set;  /* flag set to TRUE when topline is set,
                                       e.g. by winrestview() */
    colnr_T     w_leftcol;          /* window column number of the left most
                                       character in the window; used when
                                       'wrap' is off */
    colnr_T     w_skipcol;          /* starting column when a single line
                                       doesn't fit in the window */

    /*
     * Layout of the window in the screen.
     * May need to add "msg_scrolled" to "w_winrow" in rare situations.
     */
    int         w_winrow;           /* first row of window in screen */
    int         w_height;           /* number of rows in window, excluding
                                       status/command line(s) */
    int         w_status_height;    /* number of status lines (0 or 1) */
    int         w_wincol;           /* Leftmost column of window in screen.
                                       use W_WINCOL() */
    int         w_width;            /* Width of window, excluding separation.
                                       use W_WIDTH() */
    int         w_vsep_width;       /* Number of separator columns (0 or 1).
                                       use W_VSEP_WIDTH() */

    /*
     * === start of cached values ====
     */
    /*
     * Recomputing is minimized by storing the result of computations.
     * Use functions in screen.c to check if they are valid and to update.
     * w_valid is a bitfield of flags, which indicate if specific values are
     * valid or need to be recomputed.  See screen.c for values.
     */
    int         w_valid;
    pos_T       w_valid_cursor;     /* last known position of w_cursor, used
                                       to adjust w_valid */
    colnr_T     w_valid_leftcol;    /* last known w_leftcol */

    /*
     * w_cline_height is the number of physical lines taken by the buffer line
     * that the cursor is on.  We use this to avoid extra calls to plines().
     */
    int         w_cline_height;     /* current size of cursor line */

    int         w_cline_row;        /* starting row of the cursor line */

    colnr_T     w_virtcol;          /* column number of the cursor in the
                                       buffer line, as opposed to the column
                                       number we're at on the screen.  This
                                       makes a difference on lines which span
                                       more than one screen line or when
                                       w_leftcol is non-zero */

    /*
     * w_wrow and w_wcol specify the cursor position in the window.
     * This is related to positions in the window, not in the display or
     * buffer, thus w_wrow is relative to w_winrow.
     */
    int         w_wrow, w_wcol;     /* cursor position in window */

    linenr_T    w_botline;          /* number of the line below the bottom of
                                       the screen */
    int         w_empty_rows;       /* number of ~ rows in window */

    /*
     * Info about the lines currently in the window is remembered to avoid
     * recomputing it every time.  The allocated size of w_lines[] is Rows.
     * Only the w_lines_valid entries are actually valid.
     * When the display is up-to-date w_lines[0].wl_lnum is equal to w_topline
     * and w_lines[w_lines_valid - 1].wl_lnum is equal to w_botline.
     * Between changing text and updating the display w_lines[] represents
     * what is currently displayed.  wl_valid is reset to indicated this.
     * This is used for efficient redrawing.
     */
    int         w_lines_valid;      /* number of valid entries */
    wline_T     *w_lines;

    int         w_nrwidth;          /* width of 'number' and 'relativenumber' column being used */

    /*
     * === end of cached values ===
     */

    int         w_redr_type;        /* type of redraw to be performed on win */
    int         w_upd_rows;         /* number of window lines to update when
                                       w_redr_type is REDRAW_TOP */
    linenr_T    w_redraw_top;       /* when != 0: first line needing redraw */
    linenr_T    w_redraw_bot;       /* when != 0: last line needing redraw */
    int         w_redr_status;      /* if TRUE status line must be redrawn */

    /* remember what is shown in the ruler for this window (if 'ruler' set) */
    pos_T       w_ru_cursor;        /* cursor position shown in ruler */
    colnr_T     w_ru_virtcol;       /* virtcol shown in ruler */
    linenr_T    w_ru_topline;       /* topline shown in ruler */
    linenr_T    w_ru_line_count;    /* line count used for ruler */
    char        w_ru_empty;         /* TRUE if ruler shows 0-1 (empty line) */

    int         w_alt_fnum;         /* alternate file (for # and CTRL-^) */

    alist_T     *w_alist;           /* pointer to arglist for this window */
    int         w_arg_idx;          /* current index in argument list (can be
                                       out of range!) */
    int         w_arg_idx_invalid;  /* editing another file than w_arg_idx */

    char_u      *w_localdir;        /* absolute path of local directory or
                                       NULL */
    /*
     * Options local to a window.
     * They are local because they influence the layout of the window or
     * depend on the window layout.
     * There are two values: w_onebuf_opt is local to the buffer currently in
     * this window, w_allbuf_opt is for all buffers in this window.
     */
    winopt_T    w_onebuf_opt;
    winopt_T    w_allbuf_opt;

    /* A few options have local flags for P_INSECURE. */
    long_u      w_p_stl_flags;      /* flags for 'statusline' */
    long_u      w_p_fde_flags;      /* flags for 'foldexpr' */
    long_u      w_p_fdt_flags;      /* flags for 'foldtext' */
    int         *w_p_cc_cols;       /* array of columns to highlight or NULL */
    int         w_p_brimin;         /* minimum width for breakindent */
    int         w_p_brishift;       /* additional shift for breakindent */
    int         w_p_brisbr;         /* sbr in 'briopt' */

    /* transform a pointer to a "onebuf" option into a "allbuf" option */
#define GLOBAL_WO(p)    ((char *)p + sizeof(winopt_T))

    long        w_scbind_pos;

    dictitem_T  w_winvar;       /* variable for "w:" Dictionary */
    dict_T      *w_vars;        /* internal variables, local to window */

    /*
     * The w_prev_pcmark field is used to check whether we really did jump to
     * a new line after setting the w_pcmark.  If not, then we revert to
     * using the previous w_pcmark.
     */
    pos_T       w_pcmark;       /* previous context mark */
    pos_T       w_prev_pcmark;  /* previous w_pcmark */

    /*
     * the jumplist contains old cursor positions
     */
    xfmark_T    w_jumplist[JUMPLISTSIZE];
    int         w_jumplistlen;          /* number of active entries */
    int         w_jumplistidx;          /* current position */

    int         w_changelistidx;        /* current position in b_changelist */

    matchitem_T *w_match_head;          /* head of match list */
    int         w_next_match_id;        /* next match ID */

    /*
     * w_fraction is the fractional row of the cursor within the window, from
     * 0 at the top row to FRACTION_MULT at the last row.
     * w_prev_fraction_row was the actual cursor row when w_fraction was last calculated.
     */
    int         w_fraction;
    int         w_prev_fraction_row;

    linenr_T    w_nrwidth_line_count;   /* line count when ml_nrwidth_width was computed. */
    long        w_nuw_cached;           /* 'numberwidth' option cached */
    int         w_nrwidth_width;        /* nr of chars to print line count. */
};

/*
 * Arguments for operators.
 */
typedef struct oparg_S
{
    int         op_type;        /* current pending operator type */
    int         regname;        /* register to use for the operator */
    int         motion_type;    /* type of the current cursor motion */
    int         motion_force;   /* force motion type: 'v', 'V' or CTRL-V */
    int         use_reg_one;    /* TRUE if delete uses reg 1 even when not linewise */
    int         inclusive;      /* TRUE if char motion is inclusive (only
                                   valid when motion_type is MCHAR */
    int         end_adjusted;   /* backuped b_op_end one char (only used by do_format()) */
    pos_T       start;          /* start of the operator */
    pos_T       end;            /* end of the operator */
    pos_T       cursor_start;   /* cursor position before motion for "gw" */

    long        line_count;     /* number of lines from op_start to op_end (inclusive) */
    int         empty;          /* op_start and op_end the same (only used by do_change()) */
    int         is_VIsual;      /* operator on Visual area */
    int         block_mode;     /* current operator is Visual block mode */
    colnr_T     start_vcol;     /* start col for block mode operator */
    colnr_T     end_vcol;       /* end col for block mode operator */
    long        prev_opcount;   /* ca.opcount saved for K_CURSORHOLD */
    long        prev_count0;    /* ca.count0 saved for K_CURSORHOLD */
} oparg_T;

/*
 * Arguments for Normal mode commands.
 */
typedef struct cmdarg_S
{
    oparg_T     *oap;           /* Operator arguments */
    int         prechar;        /* prefix character (optional, always 'g') */
    int         cmdchar;        /* command character */
    int         nchar;          /* next command character (optional) */
    int         ncharC1;        /* first composing character (optional) */
    int         ncharC2;        /* second composing character (optional) */
    int         extra_char;     /* yet another character (optional) */
    long        opcount;        /* count before an operator */
    long        count0;         /* count before command, default 0 */
    long        count1;         /* count before command, default 1 */
    int         arg;            /* extra argument from nv_cmds[] */
    int         retval;         /* return: CA_* values */
    char_u      *searchbuf;     /* return: pointer to search pattern or NULL */
} cmdarg_T;

/* values for retval: */
#define CA_COMMAND_BUSY     1   /* skip restarting edit() once */
#define CA_NO_ADJ_OP_END    2   /* don't adjust operator end */

/*
 * struct to store values from 'guicursor' and 'mouseshape'
 */
/* Indexes in shape_table[] */
#define SHAPE_IDX_N     0       /* Normal mode */
#define SHAPE_IDX_V     1       /* Visual mode */
#define SHAPE_IDX_I     2       /* Insert mode */
#define SHAPE_IDX_R     3       /* Replace mode */
#define SHAPE_IDX_C     4       /* Command line Normal mode */
#define SHAPE_IDX_CI    5       /* Command line Insert mode */
#define SHAPE_IDX_CR    6       /* Command line Replace mode */
#define SHAPE_IDX_O     7       /* Operator-pending mode */
#define SHAPE_IDX_VE    8       /* Visual mode with 'selection' exclusive */
#define SHAPE_IDX_CLINE 9       /* On command line */
#define SHAPE_IDX_STATUS 10     /* A status line */
#define SHAPE_IDX_SDRAG 11      /* dragging a status line */
#define SHAPE_IDX_VSEP  12      /* A vertical separator line */
#define SHAPE_IDX_VDRAG 13      /* dragging a vertical separator line */
#define SHAPE_IDX_MORE  14      /* Hit-return or More */
#define SHAPE_IDX_MOREL 15      /* Hit-return or More in last line */
#define SHAPE_IDX_SM    16      /* showing matching paren */
#define SHAPE_IDX_COUNT 17

#define SHAPE_BLOCK     0       /* block cursor */
#define SHAPE_HOR       1       /* horizontal bar cursor */
#define SHAPE_VER       2       /* vertical bar cursor */

#define MSHAPE_NUMBERED 1000    /* offset for shapes identified by number */
#define MSHAPE_HIDE     1       /* hide mouse pointer */

#define SHAPE_MOUSE     1       /* used for mouse pointer shape */
#define SHAPE_CURSOR    2       /* used for text cursor shape */

typedef struct cursor_entry
{
    int         shape;          /* one of the SHAPE_ defines */
    int         mshape;         /* one of the MSHAPE defines */
    int         percentage;     /* percentage of cell for bar */
    long        blinkwait;      /* blinking, wait time before blinking starts */
    long        blinkon;        /* blinking, on time */
    long        blinkoff;       /* blinking, off time */
    int         id;             /* highlight group ID */
    int         id_lm;          /* highlight group ID for :lmap mode */
    char        *name;          /* mode name (fixed) */
    char        used_for;       /* SHAPE_MOUSE and/or SHAPE_CURSOR */
} cursorentry_T;

/* For generating prototypes when FEAT_MENU isn't defined. */
typedef int vimmenu_T;

/*
 * Struct to save values in before executing autocommands for a buffer that is
 * not the current buffer.  Without FEAT_AUTOCMD only "curbuf" is remembered.
 */
typedef struct
{
    buf_T       *save_curbuf;   /* saved curbuf */
    int         use_aucmd_win;  /* using aucmd_win */
    win_T       *save_curwin;   /* saved curwin */
    win_T       *new_curwin;    /* new curwin */
    buf_T       *new_curbuf;    /* new curbuf */
    char_u      *globaldir;     /* saved value of globaldir */
} aco_save_T;

/*
 * Generic option table item, only used for printer at the moment.
 */
typedef struct
{
    const char  *name;
    int         hasnum;
    long        number;
    char_u      *string;        /* points into option string */
    int         strlen;
    int         present;
} option_table_T;

typedef struct {
  UINT32_T total[2];
  UINT32_T state[8];
  char_u   buffer[64];
} context_sha256_T;

/* ----------------------------------------------------------------------- */

/* Codes for mouse button events in lower three bits: */
#define MOUSE_LEFT     0x00
#define MOUSE_MIDDLE   0x01
#define MOUSE_RIGHT    0x02
#define MOUSE_RELEASE  0x03

/* bit masks for modifiers: */
#define MOUSE_SHIFT    0x04
#define MOUSE_ALT      0x08
#define MOUSE_CTRL     0x10

/* mouse buttons that are handled like a key press (GUI only) */
/* Note that the scroll wheel keys are inverted: MOUSE_5 scrolls lines up but
 * the result of this is that the window moves down, similarly MOUSE_6 scrolls
 * columns left but the window moves right. */
#define MOUSE_4        0x100   /* scroll wheel down */
#define MOUSE_5        0x200   /* scroll wheel up */

#define MOUSE_X1       0x300 /* Mouse-button X1 (6th) */
#define MOUSE_X2       0x400 /* Mouse-button X2 */

#define MOUSE_6        0x500   /* scroll wheel left */
#define MOUSE_7        0x600   /* scroll wheel right */

/* 0x20 is reserved by xterm */
#define MOUSE_DRAG_XTERM   0x40

#define MOUSE_DRAG     (0x40 | MOUSE_RELEASE)

/* Lowest button code for using the mouse wheel (xterm only) */
#define MOUSEWHEEL_LOW         0x60

#define MOUSE_CLICK_MASK       0x03

#define NUM_MOUSE_CLICKS(code) (((unsigned)((code) & 0xC0) >> 6) + 1)

#define SET_NUM_MOUSE_CLICKS(code, num) (code) = ((code) & 0x3f) | ((((num) - 1) & 3) << 6)

/* Added to mouse column for GUI when 'mousefocus' wants to give focus to a
 * window by simulating a click on its status line.  We could use up to 128 *
 * 128 = 16384 columns, now it's reduced to 10000. */
#define MOUSE_COLOFF 10000

/*
 * jump_to_mouse() returns one of first four these values, possibly with
 * some of the other three added.
 */
#define IN_UNKNOWN             0
#define IN_BUFFER              1
#define IN_STATUS_LINE         2       /* on status or command line */
#define IN_SEP_LINE            4       /* on vertical separator line */
#define IN_OTHER_WIN           8       /* in other window but can't go there */
#define CURSOR_MOVED           0x100
#define MOUSE_FOLD_CLOSE       0x200   /* clicked on '-' in fold column */
#define MOUSE_FOLD_OPEN        0x400   /* clicked on '+' in fold column */

/* flags for jump_to_mouse() */
#define MOUSE_FOCUS            0x01    /* need to stay in this window */
#define MOUSE_MAY_VIS          0x02    /* may start Visual mode */
#define MOUSE_DID_MOVE         0x04    /* only act when mouse has moved */
#define MOUSE_SETPOS           0x08    /* only set current mouse position */
#define MOUSE_MAY_STOP_VIS     0x10    /* may stop Visual mode */
#define MOUSE_RELEASED         0x20    /* button was released */

/* defines for eval_vars() */
#define VALID_PATH              1
#define VALID_HEAD              2

/* Defines for Vim variables.  These must match vimvars[] in eval.c! */
#define VV_COUNT        0
#define VV_COUNT1       1
#define VV_PREVCOUNT    2
#define VV_ERRMSG       3
#define VV_WARNINGMSG   4
#define VV_STATUSMSG    5
#define VV_SHELL_ERROR  6
#define VV_THIS_SESSION 7
#define VV_VERSION      8
#define VV_LNUM         9
#define VV_TERMRESPONSE 10
#define VV_FNAME        11
#define VV_LANG         12
#define VV_LC_TIME      13
#define VV_CTYPE        14
#define VV_CC_FROM      15
#define VV_CC_TO        16
#define VV_FNAME_IN     17
#define VV_FNAME_OUT    18
#define VV_FNAME_NEW    19
#define VV_FNAME_DIFF   20
#define VV_CMDARG       21
#define VV_FOLDSTART    22
#define VV_FOLDEND      23
#define VV_FOLDDASHES   24
#define VV_FOLDLEVEL    25
#define VV_PROGNAME     26
#define VV_SEND_SERVER  27
#define VV_DYING        28
#define VV_EXCEPTION    29
#define VV_THROWPOINT   30
#define VV_REG          31
#define VV_CMDBANG      32
#define VV_INSERTMODE   33
#define VV_VAL          34
#define VV_KEY          35
#define VV_PROFILING    36
#define VV_FCS_REASON   37
#define VV_FCS_CHOICE   38
#define VV_BEVAL_BUFNR  39
#define VV_BEVAL_WINNR  40
#define VV_BEVAL_LNUM   41
#define VV_BEVAL_COL    42
#define VV_BEVAL_TEXT   43
#define VV_SCROLLSTART  44
#define VV_SWAPNAME     45
#define VV_SWAPCHOICE   46
#define VV_SWAPCOMMAND  47
#define VV_CHAR         48
#define VV_MOUSE_WIN    49
#define VV_MOUSE_LNUM   50
#define VV_MOUSE_COL    51
#define VV_OP           52
#define VV_SEARCHFORWARD 53
#define VV_HLSEARCH     54
#define VV_OLDFILES     55
#define VV_WINDOWID     56
#define VV_PROGPATH     57
#define VV_LEN          58      /* number of v: vars */

/* VIM_ATOM_NAME is the older Vim-specific selection type for X11.  Still
 * supported for when a mix of Vim versions is used. VIMENC_ATOM_NAME includes
 * the encoding to support Vims using different 'encoding' values. */
#define VIM_ATOM_NAME "_VIM_TEXT"
#define VIMENC_ATOM_NAME "_VIMENC_TEXT"

/* Selection states for modeless selection */
#define SELECT_CLEARED         0
#define SELECT_IN_PROGRESS     1
#define SELECT_DONE            2

#define SELECT_MODE_CHAR       0
#define SELECT_MODE_WORD       1
#define SELECT_MODE_LINE       2

/* Info about selected text */
typedef struct VimClipboard
{
    int         available;      /* Is clipboard available? */
    int         owned;          /* Flag: do we own the selection? */
    pos_T       start;          /* Start of selected area */
    pos_T       end;            /* End of selected area */
    int         vmode;          /* Visual mode character */

    /* Fields for selection that doesn't use Visual mode */
    short_u     origin_row;
    short_u     origin_start_col;
    short_u     origin_end_col;
    short_u     word_start_col;
    short_u     word_end_col;

    pos_T       prev;           /* Previous position */
    short_u     state;          /* Current selection state */
    short_u     mode;           /* Select by char, word, or line. */
} VimClipboard;

#include "ex_cmds.h"        /* Ex command defines */
#include "proto.h"          /* function prototypes */

/* This has to go after the include of proto.h, as proto/gui.pro declares
 * functions of these names. The declarations would break if the defines had
 * been seen at that stage.  But it must be before globals.h, where error_ga
 * is declared. */
#define mch_errmsg(str)        fprintf(stderr, "%s", (str))
#define display_errors()       fflush(stderr)
#define mch_msg(str)           printf("%s", (str))

/* ----------------------------------------------------------------------- */

/* #include "globals.h" */     /* global variables and messages */
/*
 * definition of global variables
 */

/*
 * Number of Rows and Columns in the screen.
 * Must be long to be able to use them as options in option.c.
 * Note: Use screen_Rows and screen_Columns to access items in ScreenLines[].
 * They may have different values when the screen wasn't (re)allocated yet
 * after setting Rows or Columns (e.g., when starting up).
 */
EXTERN long     Rows                    /* nr of rows in the screen */
#if defined(DO_INIT)
                            = 24L
#endif
                            ;
EXTERN long     Columns INIT(= 80);     /* nr of columns in the screen */

/*
 * The characters that are currently on the screen are kept in ScreenLines[].
 * It is a single block of characters, the size of the screen plus one line.
 * The attributes for those characters are kept in ScreenAttrs[].
 *
 * "LineOffset[n]" is the offset from ScreenLines[] for the start of line 'n'.
 * The same value is used for ScreenLinesUC[] and ScreenAttrs[].
 *
 * Note: before the screen is initialized and when out of memory these can be NULL.
 */
EXTERN schar_T  *ScreenLines INIT(= NULL);
EXTERN sattr_T  *ScreenAttrs INIT(= NULL);
EXTERN unsigned *LineOffset INIT(= NULL);
EXTERN char_u   *LineWraps INIT(= NULL);        /* line wraps to next line */

/*
 * When using Unicode characters (in UTF-8 encoding) the character in
 * ScreenLinesUC[] contains the Unicode for the character at this position, or
 * NUL when the character in ScreenLines[] is to be used (ASCII char).
 * The composing characters are to be drawn on top of the original character.
 * ScreenLinesC[0][off] is only to be used when ScreenLinesUC[off] != 0.
 * Note: These three are only allocated when enc_utf8 is set!
 */
EXTERN u8char_T *ScreenLinesUC INIT(= NULL);    /* decoded UTF-8 characters */
EXTERN u8char_T *ScreenLinesC[MAX_MCO];         /* composing characters */
EXTERN int      Screen_mco INIT(= 0);           /* value of p_mco used when
                                                   allocating ScreenLinesC[] */

/*
 * Indexes for tab page line:
 *      N > 0 for label of tab page N
 *      N == 0 for no label
 *      N < 0 for closing tab page -N
 *      N == -999 for closing current tab page
 */
EXTERN short    *TabPageIdxs INIT(= NULL);

EXTERN int      screen_Rows INIT(= 0);      /* actual size of ScreenLines[] */
EXTERN int      screen_Columns INIT(= 0);   /* actual size of ScreenLines[] */

/*
 * When vgetc() is called, it sets mod_mask to the set of modifiers that are
 * held down based on the MOD_MASK_* symbols that are read first.
 */
EXTERN int      mod_mask INIT(= 0x0);           /* current key modifiers */

/*
 * Cmdline_row is the row where the command line starts, just below the last window.
 * When the cmdline gets longer than the available space the screen gets
 * scrolled up. After a CTRL-D (show matches), after hitting ':' after
 * "hit return", and for the :global command, the command line is
 * temporarily moved.  The old position is restored with the next call to update_screen().
 */
EXTERN int      cmdline_row;

EXTERN int      redraw_cmdline INIT(= FALSE);   /* cmdline must be redrawn */
EXTERN int      clear_cmdline INIT(= FALSE);    /* cmdline must be cleared */
EXTERN int      mode_displayed INIT(= FALSE);   /* mode is being displayed */
EXTERN int      cmdline_star INIT(= FALSE);     /* cmdline is crypted */

EXTERN int      exec_from_reg INIT(= FALSE);    /* executing register */

EXTERN int      screen_cleared INIT(= FALSE);   /* screen has been cleared */

/*
 * When '$' is included in 'cpoptions' option set:
 * When a change command is given that deletes only part of a line, a dollar
 * is put at the end of the changed text. dollar_vcol is set to the virtual
 * column of this '$'.  -1 is used to indicate no $ is being displayed.
 */
EXTERN colnr_T  dollar_vcol INIT(= -1);

/*
 * Functions for putting characters in the command line,
 * while keeping ScreenLines[] updated.
 */
EXTERN int      cmdmsg_rl INIT(= FALSE);    /* cmdline is drawn right to left */
EXTERN int      msg_col;
EXTERN int      msg_row;
EXTERN int      msg_scrolled;   /* Number of screen lines that windows have
                                 * scrolled because of printing messages. */
EXTERN int      msg_scrolled_ign INIT(= FALSE);
                                /* when TRUE don't set need_wait_return in
                                   msg_puts_attr() when msg_scrolled is
                                   non-zero */

EXTERN char_u   *keep_msg INIT(= NULL);     /* msg to be shown after redraw */
EXTERN int      keep_msg_attr INIT(= 0);    /* highlight attr for keep_msg */
EXTERN int      keep_msg_more INIT(= FALSE); /* keep_msg was set by msgmore() */
EXTERN int      need_fileinfo INIT(= FALSE);/* do fileinfo() after redraw */
EXTERN int      msg_scroll INIT(= FALSE);   /* msg_start() will scroll */
EXTERN int      msg_didout INIT(= FALSE);   /* msg_outstr() was used in line */
EXTERN int      msg_didany INIT(= FALSE);   /* msg_outstr() was used at all */
EXTERN int      msg_nowait INIT(= FALSE);   /* don't wait for this msg */
EXTERN int      emsg_off INIT(= 0);         /* don't display errors for now,
                                               unless 'debug' is set. */
EXTERN int      info_message INIT(= FALSE); /* printing informative message */
EXTERN int      msg_hist_off INIT(= FALSE); /* don't add messages to history */
EXTERN int      need_clr_eos INIT(= FALSE); /* need to clear text before
                                               displaying a message. */
EXTERN int      emsg_skip INIT(= 0);        /* don't display errors for
                                               expression that is skipped */
EXTERN int      emsg_severe INIT(= FALSE);   /* use message of next of several
                                               emsg() calls for throw */
EXTERN int      did_endif INIT(= FALSE);    /* just had ":endif" */
EXTERN dict_T   vimvardict;                 /* Dictionary with v: variables */
EXTERN dict_T   globvardict;                /* Dictionary with g: variables */
EXTERN int      did_emsg;                   /* set by emsg() when the message
                                               is displayed or thrown */
EXTERN int      did_emsg_syntax;            /* did_emsg set because of a
                                               syntax error */
EXTERN int      called_emsg;                /* always set by emsg() */
EXTERN int      ex_exitval INIT(= 0);       /* exit value for ex mode */
EXTERN int      emsg_on_display INIT(= FALSE);  /* there is an error message */
EXTERN int      rc_did_emsg INIT(= FALSE);  /* vim_regcomp() called emsg() */

EXTERN int      no_wait_return INIT(= 0);   /* don't wait for return for now */
EXTERN int      need_wait_return INIT(= 0); /* need to wait for return later */
EXTERN int      did_wait_return INIT(= FALSE);  /* wait_return() was used and
                                                   nothing written since then */
EXTERN int      need_maketitle INIT(= TRUE); /* call maketitle() soon */

EXTERN int      quit_more INIT(= FALSE);    /* 'q' hit at "--more--" msg */
EXTERN int      newline_on_exit INIT(= FALSE);  /* did msg in altern. screen */
EXTERN int      intr_char INIT(= 0);        /* extra interrupt character */
EXTERN int      ex_keep_indent INIT(= FALSE); /* getexmodeline(): keep indent */
EXTERN int      vgetc_busy INIT(= 0);       /* when inside vgetc() then > 0 */

EXTERN int      didset_vim INIT(= FALSE);   /* did set $VIM ourselves */
EXTERN int      didset_vimruntime INIT(= FALSE);   /* idem for $VIMRUNTIME */

/*
 * Lines left before a "more" message.  Ex mode needs to be able to reset this
 * after you type something.
 */
EXTERN int      lines_left INIT(= -1);      /* lines left for listing */
EXTERN int      msg_no_more INIT(= FALSE);  /* don't use more prompt, truncate
                                               messages */

EXTERN char_u   *sourcing_name INIT( = NULL);/* name of error message source */
EXTERN linenr_T sourcing_lnum INIT(= 0);    /* line number of the source file */

EXTERN int      ex_nesting_level INIT(= 0);     /* nesting level */
EXTERN int      debug_break_level INIT(= -1);   /* break below this level */
EXTERN int      debug_did_msg INIT(= FALSE);    /* did "debug mode" message */
EXTERN int      debug_tick INIT(= 0);           /* breakpoint change count */

/*
 * The exception currently being thrown.  Used to pass an exception to
 * a different cstack.  Also used for discarding an exception before it is
 * caught or made pending.  Only valid when did_throw is TRUE.
 */
EXTERN except_T *current_exception;

/*
 * did_throw: An exception is being thrown.  Reset when the exception is caught
 * or as long as it is pending in a finally clause.
 */
EXTERN int did_throw INIT(= FALSE);

/*
 * need_rethrow: set to TRUE when a throw that cannot be handled in do_cmdline()
 * must be propagated to the cstack of the previously called do_cmdline().
 */
EXTERN int need_rethrow INIT(= FALSE);

/*
 * check_cstack: set to TRUE when a ":finish" or ":return" that cannot be
 * handled in do_cmdline() must be propagated to the cstack of the previously
 * called do_cmdline().
 */
EXTERN int check_cstack INIT(= FALSE);

/*
 * Number of nested try conditionals (across function calls and ":source" commands).
 */
EXTERN int trylevel INIT(= 0);

/*
 * When "force_abort" is TRUE, always skip commands after an error message,
 * even after the outermost ":endif", ":endwhile" or ":endfor" or for a
 * function without the "abort" flag.  It is set to TRUE when "trylevel" is
 * non-zero (and ":silent!" was not used) or an exception is being thrown at
 * the time an error is detected.  It is set to FALSE when "trylevel" gets
 * zero again and there was no error or interrupt or throw.
 */
EXTERN int force_abort INIT(= FALSE);

/*
 * "msg_list" points to a variable in the stack of do_cmdline() which keeps
 * the list of arguments of several emsg() calls, one of which is to be
 * converted to an error exception immediately after the failing command
 * returns.  The message to be used for the exception value is pointed to by
 * the "throw_msg" field of the first element in the list.  It is usually the
 * same as the "msg" field of that element, but can be identical to the "msg"
 * field of a later list element, when the "emsg_severe" flag was set when the
 * emsg() call was made.
 */
EXTERN struct msglist **msg_list INIT(= NULL);

/*
 * suppress_errthrow: When TRUE, don't convert an error to an exception.  Used
 * when displaying the interrupt message or reporting an exception that is still
 * uncaught at the top level (which has already been discarded then).  Also used
 * for the error message when no exception can be thrown.
 */
EXTERN int suppress_errthrow INIT(= FALSE);

/*
 * The stack of all caught and not finished exceptions.  The exception on the
 * top of the stack is the one got by evaluation of v:exception.  The complete
 * stack of all caught and pending exceptions is embedded in the various
 * cstacks; the pending exceptions, however, are not on the caught stack.
 */
EXTERN except_T *caught_stack INIT(= NULL);

/*
 * Garbage collection can only take place when we are sure there are no Lists
 * or Dictionaries being used internally.  This is flagged with
 * "may_garbage_collect" when we are at the toplevel.
 * "want_garbage_collect" is set by the garbagecollect() function, which means
 * we do garbage collection before waiting for a char at the toplevel.
 * "garbage_collect_at_exit" indicates garbagecollect(1) was called.
 */
EXTERN int      may_garbage_collect INIT(= FALSE);
EXTERN int      want_garbage_collect INIT(= FALSE);
EXTERN int      garbage_collect_at_exit INIT(= FALSE);

/* ID of script being sourced or was sourced to define the current function. */
EXTERN scid_T   current_SID INIT(= 0);

/* Magic number used for hashitem "hi_key" value indicating a deleted item.
 * Only the address is used. */
EXTERN char_u   hash_removed;

EXTERN int      scroll_region INIT(= FALSE); /* term supports scroll region */
EXTERN int      t_colors INIT(= 0);         /* int value of T_CCO */

/*
 * When highlight_match is TRUE, highlight a match, starting at the cursor
 * position.  Search_match_lines is the number of lines after the match (0 for
 * a match within one line), search_match_endcol the column number of the
 * character just after the match in the last line.
 */
EXTERN int      highlight_match INIT(= FALSE);  /* show search match pos */
EXTERN linenr_T search_match_lines;             /* lines of of matched string */
EXTERN colnr_T  search_match_endcol;            /* col nr of match end */

EXTERN int      no_smartcase INIT(= FALSE);     /* don't use 'smartcase' once */

EXTERN int      need_check_timestamps INIT(= FALSE); /* need to check file
                                                        timestamps asap */
EXTERN int      did_check_timestamps INIT(= FALSE); /* did check timestamps
                                                       recently */
EXTERN int      no_check_timestamps INIT(= 0);  /* Don't check timestamps */

EXTERN int      highlight_attr[HLF_COUNT];  /* Highl. attr for each context. */
EXTERN int      highlight_user[9];              /* User[1-9] attributes */
EXTERN int      highlight_stlnc[9];             /* On top of user */
EXTERN int      cterm_normal_fg_color INIT(= 0);
EXTERN int      cterm_normal_fg_bold INIT(= 0);
EXTERN int      cterm_normal_bg_color INIT(= 0);

EXTERN int      autocmd_busy INIT(= FALSE);     /* Is apply_autocmds() busy? */
EXTERN int      autocmd_no_enter INIT(= FALSE); /* *Enter autocmds disabled */
EXTERN int      autocmd_no_leave INIT(= FALSE); /* *Leave autocmds disabled */
EXTERN int      modified_was_set;               /* did ":set modified" */
EXTERN int      did_filetype INIT(= FALSE);     /* FileType event found */
EXTERN int      keep_filetype INIT(= FALSE);    /* value for did_filetype when
                                                   starting to execute
                                                   autocommands */

/* When deleting the current buffer, another one must be loaded.  If we know
 * which one is preferred, au_new_curbuf is set to it */
EXTERN buf_T    *au_new_curbuf INIT(= NULL);

/* When deleting a buffer/window and autocmd_busy is TRUE, do not free the
 * buffer/window. but link it in the list starting with
 * au_pending_free_buf/ap_pending_free_win, using b_next/w_next.
 * Free the buffer/window when autocmd_busy is being set to FALSE. */
EXTERN buf_T    *au_pending_free_buf INIT(= NULL);
EXTERN win_T    *au_pending_free_win INIT(= NULL);

/*
 * Mouse coordinates, set by check_termcode()
 */
EXTERN int      mouse_row;
EXTERN int      mouse_col;
EXTERN int      mouse_past_bottom INIT(= FALSE);/* mouse below last line */
EXTERN int      mouse_past_eol INIT(= FALSE);   /* mouse right of line */
EXTERN int      mouse_dragging INIT(= 0);       /* extending Visual area with
                                                   mouse dragging */

/* While redrawing the screen this flag is set.  It means the screen size
 * ('lines' and 'rows') must not be changed. */
EXTERN int      updating_screen INIT(= FALSE);

EXTERN VimClipboard clip_star;  /* PRIMARY selection in X11 */
#define clip_plus clip_star   /* there is only one clipboard */
#define ONE_CLIPBOARD

#define CLIP_UNNAMED      1
#define CLIP_UNNAMED_PLUS 2
EXTERN int      clip_unnamed INIT(= 0); /* above two values or'ed */

EXTERN int      clip_autoselect_star INIT(= FALSE);
EXTERN int      clip_autoselect_plus INIT(= FALSE);
EXTERN int      clip_autoselectml INIT(= FALSE);
EXTERN int      clip_html INIT(= FALSE);
EXTERN regprog_T *clip_exclude_prog INIT(= NULL);
EXTERN int      clip_did_set_selection INIT(= TRUE);
EXTERN int      clip_unnamed_saved INIT(= 0);

/*
 * All windows are linked in a list. firstwin points to the first entry,
 * lastwin to the last entry (can be the same as firstwin) and curwin to the
 * currently active window.
 */
EXTERN win_T    *firstwin;              /* first window */
EXTERN win_T    *lastwin;               /* last window */
EXTERN win_T    *prevwin INIT(= NULL);  /* previous window */
#define W_NEXT(wp) ((wp)->w_next)
#define FOR_ALL_WINDOWS(wp) for (wp = firstwin; wp != NULL; wp = wp->w_next)
/*
 * When using this macro "break" only breaks out of the inner loop. Use "goto"
 * to break out of the tabpage loop.
 */
#define FOR_ALL_TAB_WINDOWS(tp, wp) \
    for ((tp) = first_tabpage; (tp) != NULL; (tp) = (tp)->tp_next) \
        for ((wp) = ((tp) == curtab) \
                ? firstwin : (tp)->tp_firstwin; (wp); (wp) = (wp)->w_next)

EXTERN win_T    *curwin;        /* currently active window */

EXTERN win_T    *aucmd_win;     /* window used in aucmd_prepbuf() */
EXTERN int      aucmd_win_used INIT(= FALSE);   /* aucmd_win is being used */

/*
 * The window layout is kept in a tree of frames.  topframe points to the top of the tree.
 */
EXTERN frame_T  *topframe;      /* top of the window frame tree */

/*
 * Tab pages are alternative topframes.  "first_tabpage" points to the first
 * one in the list, "curtab" is the current one.
 */
EXTERN tabpage_T    *first_tabpage;
EXTERN tabpage_T    *curtab;
EXTERN int          redraw_tabline INIT(= FALSE);  /* need to redraw tabline */

/*
 * All buffers are linked in a list. 'firstbuf' points to the first entry,
 * 'lastbuf' to the last entry and 'curbuf' to the currently active buffer.
 */
EXTERN buf_T    *firstbuf INIT(= NULL); /* first buffer */
EXTERN buf_T    *lastbuf INIT(= NULL);  /* last buffer */
EXTERN buf_T    *curbuf INIT(= NULL);   /* currently active buffer */

/* Flag that is set when switching off 'swapfile'.  It means that all blocks
 * are to be loaded into memory.  Shouldn't be global... */
EXTERN int      mf_dont_release INIT(= FALSE);  /* don't release blocks */

/*
 * List of files being edited (global argument list).  curwin->w_alist points
 * to this when the window is using the global argument list.
 */
EXTERN alist_T  global_alist;   /* global argument list */
EXTERN int      max_alist_id INIT(= 0);     /* the previous argument list id */
EXTERN int      arg_had_last INIT(= FALSE); /* accessed last file in
                                               global_alist */

EXTERN int      ru_col;         /* column for ruler */
EXTERN int      ru_wid;         /* 'rulerfmt' width of ruler when non-zero */
EXTERN int      sc_col;         /* column for shown command */

EXTERN char_u   *vim_tempdir INIT(= NULL); /* Name of Vim's own temp dir.
                                              Ends in a slash. */

/*
 * When starting or exiting some things are done differently (e.g. screen updating).
 */
EXTERN int      starting INIT(= NO_SCREEN);
                                /* first NO_SCREEN, then NO_BUFFERS and then
                                 * set to 0 when starting up finished */
EXTERN int      exiting INIT(= FALSE);
                                /* TRUE when planning to exit Vim.  Might
                                 * still keep on running if there is a changed buffer. */
EXTERN int      really_exiting INIT(= FALSE);
                                /* TRUE when we are sure to exit, e.g., after a deadly signal */
/* volatile because it is used in signal handler deathtrap(). */
EXTERN volatile int full_screen INIT(= FALSE);
                                /* TRUE when doing full-screen output
                                 * otherwise only writing some messages */

EXTERN int      restricted INIT(= FALSE);
                                /* TRUE when started as "rvim" */
EXTERN int      secure INIT(= FALSE);
                                /* non-zero when only "safe" commands are
                                 * allowed, e.g. when sourcing .exrc or .vimrc
                                 * in current directory */

EXTERN int      textlock INIT(= 0);
                                /* non-zero when changing text and jumping to
                                 * another window or buffer is not allowed */

EXTERN int      curbuf_lock INIT(= 0);
                                /* non-zero when the current buffer can't be
                                 * changed.  Used for FileChangedRO. */
EXTERN int      allbuf_lock INIT(= 0);
                                /* non-zero when no buffer name can be
                                 * changed, no buffer can be deleted and
                                 * current directory can't be changed.
                                 * Used for SwapExists et al. */
EXTERN int      sandbox INIT(= 0);
                                /* Non-zero when evaluating an expression in a
                                 * "sandbox".  Several things are not allowed then. */

EXTERN int      silent_mode INIT(= FALSE);
                                /* set to TRUE when "-s" commandline argument used for ex */

EXTERN pos_T    VIsual;         /* start position of active Visual selection */
EXTERN int      VIsual_active INIT(= FALSE);
                                /* whether Visual mode is active */
EXTERN int      VIsual_select INIT(= FALSE);
                                /* whether Select mode is active */
EXTERN int      VIsual_reselect;
                                /* whether to restart the selection after a
                                 * Select mode mapping or menu */

EXTERN int      VIsual_mode INIT(= 'v');
                                /* type of Visual mode */

EXTERN int      redo_VIsual_busy INIT(= FALSE);
                                /* TRUE when redoing Visual */

/*
 * When pasting text with the middle mouse button in visual mode with
 * restart_edit set, remember where it started so we can set Insstart.
 */
EXTERN pos_T    where_paste_started;

/*
 * This flag is used to make auto-indent work right on lines where only a
 * <RETURN> or <ESC> is typed. It is set when an auto-indent is done, and
 * reset when any other editing is done on the line. If an <ESC> or <RETURN>
 * is received, and did_ai is TRUE, the line is truncated.
 */
EXTERN int     did_ai INIT(= FALSE);

/*
 * Column of first char after autoindent.  0 when no autoindent done.  Used
 * when 'backspace' is 0, to avoid backspacing over autoindent.
 */
EXTERN colnr_T  ai_col INIT(= 0);

/*
 * This is a character which will end a start-middle-end comment when typed as
 * the first character on a new line.  It is taken from the last character of
 * the "end" comment leader when the COM_AUTO_END flag is given for that
 * comment end in 'comments'.  It is only valid when did_ai is TRUE.
 */
EXTERN int     end_comment_pending INIT(= NUL);

/*
 * This flag is set after a ":syncbind" to let the check_scrollbind() function
 * know that it should not attempt to perform scrollbinding due to the scroll
 * that was a result of the ":syncbind." (Otherwise, check_scrollbind() will
 * undo some of the work done by ":syncbind.")  -ralston
 */
EXTERN int     did_syncbind INIT(= FALSE);

/*
 * This flag is set when a smart indent has been performed. When the next typed
 * character is a '{' the inserted tab will be deleted again.
 */
EXTERN int      did_si INIT(= FALSE);

/*
 * This flag is set after an auto indent. If the next typed character is a '}'
 * one indent will be removed.
 */
EXTERN int      can_si INIT(= FALSE);

/*
 * This flag is set after an "O" command. If the next typed character is a '{'
 * one indent will be removed.
 */
EXTERN int      can_si_back INIT(= FALSE);

EXTERN pos_T    saved_cursor            /* w_cursor before formatting text. */
#if defined(DO_INIT)
        = INIT_POS_T(0, 0, 0)
#endif
        ;

/*
 * Stuff for insert mode.
 */
EXTERN pos_T    Insstart;               /* This is where the latest
                                         * insert/append mode started. */

/* This is where the latest insert/append mode started. In contrast to
 * Insstart, this won't be reset by certain keys and is needed for
 * op_insert(), to detect correctly where inserting by the user started. */
EXTERN pos_T    Insstart_orig;

/*
 * Stuff for VREPLACE mode.
 */
EXTERN int      orig_line_count INIT(= 0);  /* Line count when "gR" started */
EXTERN int      vr_lines_changed INIT(= 0); /* #Lines changed by "gR" so far */

/*
 * Stuff for setjmp() and longjmp().
 * Used to protect areas where we could crash.
 */
EXTERN JMP_BUF lc_jump_env;     /* argument to SETJMP() */
/* volatile because it is used in signal handler deathtrap(). */
EXTERN volatile int lc_active INIT(= FALSE); /* TRUE when lc_jump_env is valid. */

EXTERN int      enc_unicode INIT(= 0);          /* 2: UCS-2 or UTF-16, 4: UCS-4 */
EXTERN int      enc_utf8 INIT(= FALSE);         /* UTF-8 encoded Unicode */
EXTERN int      enc_latin1like INIT(= TRUE);    /* 'encoding' is latin1 comp. */
EXTERN int      has_mbyte INIT(= 0);            /* any multi-byte encoding */

/*
 * To speed up BYTELEN() we fill a table with the byte lengths whenever enc_utf8 changes.
 */
EXTERN char     mb_bytelen_tab[256];

/* Variables that tell what conversion is used for keyboard input and display
 * output. */
EXTERN vimconv_T input_conv;                    /* type of input conversion */
EXTERN vimconv_T output_conv;                   /* type of output conversion */

/*
 * Function pointers, used to quickly get to the right function.  Each has
 * two possible values: latin_ (8-bit), utfc_ or utf_ (utf-8).
 * The value is set in mb_init();
 */
/* length of char in bytes, including following composing chars */
EXTERN int (*mb_ptr2len)(char_u *p) INIT(= latin_ptr2len);
/* idem, with limit on string length */
EXTERN int (*mb_ptr2len_len)(char_u *p, int size) INIT(= latin_ptr2len_len);
/* byte length of char */
EXTERN int (*mb_char2len)(int c) INIT(= latin_char2len);
/* convert char to bytes, return the length */
EXTERN int (*mb_char2bytes)(int c, char_u *buf) INIT(= latin_char2bytes);
EXTERN int (*mb_ptr2cells)(char_u *p) INIT(= latin_ptr2cells);
EXTERN int (*mb_ptr2cells_len)(char_u *p, int size) INIT(= latin_ptr2cells_len);
EXTERN int (*mb_char2cells)(int c) INIT(= latin_char2cells);
EXTERN int (*mb_off2cells)(unsigned off, unsigned max_off) INIT(= latin_off2cells);
EXTERN int (*mb_ptr2char)(char_u *p) INIT(= latin_ptr2char);
EXTERN int (*mb_head_off)(char_u *base, char_u *p) INIT(= latin_head_off);

/*
 * "State" is the main state of Vim.
 * There are other variables that modify the state:
 * "Visual_mode"    When State is NORMAL or INSERT.
 * "finish_op"      When State is NORMAL, after typing the operator and before
 *                  typing the motion command.
 */
EXTERN int      State INIT(= NORMAL);   /* This is the current state of the
                                         * command interpreter. */

EXTERN int      finish_op INIT(= FALSE);/* TRUE while an operator is pending */
EXTERN int      opcount INIT(= 0);      /* count for pending operator */

/*
 * ex mode (Q) state
 */
EXTERN int exmode_active INIT(= 0);     /* zero, EXMODE_NORMAL or EXMODE_VIM */
EXTERN int ex_no_reprint INIT(= FALSE); /* no need to print after z or p */

EXTERN int Recording INIT(= FALSE);     /* TRUE when recording into a reg. */
EXTERN int Exec_reg INIT(= FALSE);      /* TRUE when executing a register */

EXTERN int no_mapping INIT(= FALSE);    /* currently no mapping allowed */
EXTERN int no_zero_mapping INIT(= 0);   /* mapping zero not allowed */
EXTERN int allow_keys INIT(= FALSE);    /* allow key codes when no_mapping
                                         * is set */
EXTERN int no_u_sync INIT(= 0);         /* Don't call u_sync() */
EXTERN int u_sync_once INIT(= 0);       /* Call u_sync() once when evaluating
                                           an expression. */

EXTERN int restart_edit INIT(= 0);      /* call edit when next cmd finished */
EXTERN int arrow_used;                  /* Normally FALSE, set to TRUE after
                                         * hitting cursor key in insert mode.
                                         * Used by vgetorpeek() to decide when
                                         * to call u_sync() */
EXTERN int      ins_at_eol INIT(= FALSE); /* put cursor after eol when
                                           restarting edit after CTRL-O */

EXTERN int      no_abbr INIT(= TRUE);   /* TRUE when no abbreviations loaded */

#if defined(USE_ON_FLY_SCROLL)
EXTERN int      dont_scroll INIT(= FALSE);/* don't use scrollbars when TRUE */
#endif
EXTERN int      mapped_ctrl_c INIT(= FALSE); /* modes where CTRL-C is mapped */
EXTERN int      ctrl_c_interrupts INIT(= TRUE); /* CTRL-C sets got_int */

EXTERN cmdmod_T cmdmod;                 /* Ex command modifiers */

EXTERN int      msg_silent INIT(= 0);   /* don't print messages */
EXTERN int      emsg_silent INIT(= 0);  /* don't print error messages */
EXTERN int      cmd_silent INIT(= FALSE); /* don't echo the command line */

EXTERN int      swap_exists_action INIT(= SEA_NONE); /* For dialog when swap file already exists. */
EXTERN int      swap_exists_did_quit INIT(= FALSE);  /* Selected "quit" at the dialog. */

EXTERN char_u   *IObuff;        /* sprintf's are done in this buffer, size is IOSIZE */
EXTERN char_u   *NameBuff;      /* file names are expanded in this buffer, size is MAXPATHL */
EXTERN char_u   msg_buf[MSG_BUF_LEN];   /* small buffer for messages */

/* When non-zero, postpone redrawing. */
EXTERN int      RedrawingDisabled INIT(= 0);

EXTERN int      readonlymode INIT(= FALSE); /* Set to TRUE for "view" */
EXTERN int      recoverymode INIT(= FALSE); /* Set to TRUE for "-r" option */

EXTERN typebuf_T typebuf                /* typeahead buffer */
#if defined(DO_INIT)
                    = {NULL, NULL, 0, 0, 0, 0, 0, 0, 0}
#endif
                    ;
EXTERN int      ex_normal_busy INIT(= 0); /* recursiveness of ex_normal() */
EXTERN int      ex_normal_lock INIT(= 0); /* forbid use of ex_normal() */
EXTERN int      ignore_script INIT(= FALSE);  /* ignore script input */
EXTERN int      stop_insert_mode;       /* for ":stopinsert" and 'insertmode' */

EXTERN int      KeyTyped;               /* TRUE if user typed current char */
EXTERN int      KeyStuffed;             /* TRUE if current char from stuffbuf */
EXTERN int      maptick INIT(= 0);      /* tick for each non-mapped char */

EXTERN char_u   chartab[256];           /* table used in charset.c; See
                                           init_chartab() for explanation */

EXTERN int      must_redraw INIT(= 0);      /* type of redraw necessary */
EXTERN int      skip_redraw INIT(= FALSE);  /* skip redraw once */
EXTERN int      do_redraw INIT(= FALSE);    /* extra redraw once */

EXTERN int      need_highlight_changed INIT(= TRUE);
EXTERN char_u   *use_viminfo INIT(= NULL);  /* name of viminfo file to use */

#define NSCRIPT 15
EXTERN FILE     *scriptin[NSCRIPT];         /* streams to read script from */
EXTERN int      curscript INIT(= 0);        /* index in scriptin[] */
EXTERN FILE     *scriptout  INIT(= NULL);   /* stream to write script to */
EXTERN int      read_cmd_fd INIT(= 0);      /* fd to read commands from */

/* volatile because it is used in signal handler catch_sigint(). */
EXTERN volatile int got_int INIT(= FALSE);    /* set to TRUE when interrupt
                                                signal occurred */
EXTERN int      termcap_active INIT(= FALSE);   /* set by starttermcap() */
EXTERN int      cur_tmode INIT(= TMODE_COOK);   /* input terminal mode */
EXTERN int      bangredo INIT(= FALSE);     /* set to TRUE with ! command */
EXTERN int      searchcmdlen;               /* length of previous search cmd */
EXTERN int      reg_do_extmatch INIT(= 0);  /* Used when compiling regexp:
                                             * REX_SET to allow \z\(...\),
                                             * REX_USE to allow \z\1 et al. */
EXTERN reg_extmatch_T *re_extmatch_in INIT(= NULL); /* Used by vim_regexec():
                                             * strings for \z\1...\z\9 */
EXTERN reg_extmatch_T *re_extmatch_out INIT(= NULL); /* Set by vim_regexec()
                                             * to store \z\(...\) matches */

EXTERN int      did_outofmem_msg INIT(= FALSE);
                                            /* set after out of memory msg */
EXTERN int      did_swapwrite_msg INIT(= FALSE);
                                            /* set after swap write error msg */
EXTERN int      undo_off INIT(= FALSE);     /* undo switched off for now */
EXTERN int      global_busy INIT(= 0);      /* set when :global is executing */
EXTERN int      listcmd_busy INIT(= FALSE); /* set when :argdo, :windo or
                                               :bufdo is executing */
EXTERN int      need_start_insertmode INIT(= FALSE);
                                            /* start insert mode soon */
EXTERN char_u   *last_cmdline INIT(= NULL); /* last command line (for ":) */
EXTERN char_u   *repeat_cmdline INIT(= NULL); /* command line for "." */
EXTERN char_u   *new_last_cmdline INIT(= NULL); /* new value for last_cmdline */
EXTERN char_u   *autocmd_fname INIT(= NULL); /* fname for <afile> on cmdline */
EXTERN int      autocmd_fname_full;          /* autocmd_fname is full path */
EXTERN int      autocmd_bufnr INIT(= 0);     /* fnum for <abuf> on cmdline */
EXTERN char_u   *autocmd_match INIT(= NULL); /* name for <amatch> on cmdline */
EXTERN int      did_cursorhold INIT(= FALSE); /* set when CursorHold t'gerd */
EXTERN pos_T    last_cursormoved              /* for CursorMoved event */
#if defined(DO_INIT)
                        = INIT_POS_T(0, 0, 0)
#endif
                        ;
EXTERN int      last_changedtick INIT(= 0);   /* for TextChanged event */
EXTERN buf_T    *last_changedtick_buf INIT(= NULL);

EXTERN int      postponed_split INIT(= 0);  /* for CTRL-W CTRL-] command */
EXTERN int      postponed_split_flags INIT(= 0);  /* args for win_split() */
EXTERN int      postponed_split_tab INIT(= 0);  /* cmdmod.tab */
EXTERN int      replace_offset INIT(= 0);   /* offset for replace_push() */

EXTERN char_u   *escape_chars INIT(= (char_u *)" \t\\\"|"); /* need backslash in cmd line */

/*
 * When a string option is NULL (which only happens in out-of-memory situations),
 * it is set to empty_option, to avoid having to check for NULL everywhere.
 */
EXTERN char_u   *empty_option INIT(= (char_u *)"");

EXTERN int  redir_off INIT(= FALSE);    /* no redirection for a moment */
EXTERN FILE *redir_fd INIT(= NULL);     /* message redirection file */
EXTERN int  redir_reg INIT(= 0);        /* message redirection register */
EXTERN int  redir_vname INIT(= 0);      /* message redirection variable */

EXTERN char     breakat_flags[256];     /* which characters are in 'breakat' */

/* these are in version.c */
extern char *shortVersion;
extern char *longVersion;

/* When a window has a local directory, the absolute path of the global
 * current directory is stored here (in allocated memory).  If the current
 * directory is not a local directory, globaldir is NULL. */
EXTERN char_u   *globaldir INIT(= NULL);

/* Characters from 'listchars' option */
EXTERN int      lcs_eol INIT(= '$');
EXTERN int      lcs_ext INIT(= NUL);
EXTERN int      lcs_prec INIT(= NUL);
EXTERN int      lcs_nbsp INIT(= NUL);
EXTERN int      lcs_tab1 INIT(= NUL);
EXTERN int      lcs_tab2 INIT(= NUL);
EXTERN int      lcs_trail INIT(= NUL);
EXTERN int      lcs_conceal INIT(= ' ');

/* Characters from 'fillchars' option */
EXTERN int      fill_stl INIT(= ' ');
EXTERN int      fill_stlnc INIT(= ' ');
EXTERN int      fill_vert INIT(= ' ');
EXTERN int      fill_fold INIT(= '-');
EXTERN int      fill_diff INIT(= '-');

/* Whether 'keymodel' contains "stopsel" and "startsel". */
EXTERN int      km_stopsel INIT(= FALSE);
EXTERN int      km_startsel INIT(= FALSE);

EXTERN int      cedit_key INIT(= -1);   /* key value of 'cedit' option */
EXTERN int      cmdwin_type INIT(= 0);  /* type of cmdline window or 0 */
EXTERN int      cmdwin_result INIT(= 0); /* result of cmdline window or 0 */

EXTERN char_u no_lines_msg[]    INIT(= "--No lines in buffer--");

/*
 * When ":global" is used to number of substitutions and changed lines is
 * accumulated until it's finished.
 * Also used for ":spellrepall".
 */
EXTERN long     sub_nsubs;      /* total number of substitutions */
EXTERN linenr_T sub_nlines;     /* total number of lines changed */

/* table to store parsed 'wildmode' */
EXTERN char_u   wim_flags[4];

/* whether titlestring and iconstring contains statusline syntax */
#define STL_IN_ICON    1
#define STL_IN_TITLE   2
EXTERN int      stl_syntax INIT(= 0);

/* don't use 'hlsearch' temporarily */
EXTERN int      no_hlsearch INIT(= FALSE);

/* the table is in misc2.c, because of initializations */
extern cursorentry_T shape_table[SHAPE_IDX_COUNT];

EXTERN int      typebuf_was_filled INIT(= FALSE); /* received text from client
                                                     or from feedkeys() */

EXTERN int      term_is_xterm INIT(= FALSE);    /* xterm-like 'term' */

/* Set to TRUE when an operator is being executed with virtual editing, MAYBE
 * when no operator is being executed, FALSE otherwise. */
EXTERN int      virtual_op INIT(= MAYBE);

/* Display tick, incremented for each call to update_screen() */
EXTERN disptick_T       display_tick INIT(= 0);

/* Set when the cursor line needs to be redrawn. */
EXTERN int              need_cursor_line_redraw INIT(= FALSE);

/*
 * The error messages that can be shared are included here.
 * Excluded are errors that are only used once and debugging messages.
 */
EXTERN char_u e_abort[]         INIT(= "E470: Command aborted");
EXTERN char_u e_argreq[]        INIT(= "E471: Argument required");
EXTERN char_u e_backslash[]     INIT(= "E10: \\ should be followed by /, ? or &");
EXTERN char_u e_cmdwin[]        INIT(= "E11: Invalid in command-line window; <CR> executes, CTRL-C quits");
EXTERN char_u e_curdir[]        INIT(= "E12: Command not allowed from exrc/vimrc in current dir");
EXTERN char_u e_endif[]         INIT(= "E171: Missing :endif");
EXTERN char_u e_endtry[]        INIT(= "E600: Missing :endtry");
EXTERN char_u e_endwhile[]      INIT(= "E170: Missing :endwhile");
EXTERN char_u e_endfor[]        INIT(= "E170: Missing :endfor");
EXTERN char_u e_while[]         INIT(= "E588: :endwhile without :while");
EXTERN char_u e_for[]           INIT(= "E588: :endfor without :for");
EXTERN char_u e_exists[]        INIT(= "E13: File exists (add ! to override)");
EXTERN char_u e_failed[]        INIT(= "E472: Command failed");
EXTERN char_u e_internal[]      INIT(= "E473: Internal error");
EXTERN char_u e_interr[]        INIT(= "Interrupted");
EXTERN char_u e_invaddr[]       INIT(= "E14: Invalid address");
EXTERN char_u e_invarg[]        INIT(= "E474: Invalid argument");
EXTERN char_u e_invarg2[]       INIT(= "E475: Invalid argument: %s");
EXTERN char_u e_invexpr2[]      INIT(= "E15: Invalid expression: %s");
EXTERN char_u e_invrange[]      INIT(= "E16: Invalid range");
EXTERN char_u e_invcmd[]        INIT(= "E476: Invalid command");
EXTERN char_u e_isadir2[]       INIT(= "E17: \"%s\" is a directory");
EXTERN char_u e_markinval[]     INIT(= "E19: Mark has invalid line number");
EXTERN char_u e_marknotset[]    INIT(= "E20: Mark not set");
EXTERN char_u e_modifiable[]    INIT(= "E21: Cannot make changes, 'modifiable' is off");
EXTERN char_u e_nesting[]       INIT(= "E22: Scripts nested too deep");
EXTERN char_u e_noalt[]         INIT(= "E23: No alternate file");
EXTERN char_u e_noabbr[]        INIT(= "E24: No such abbreviation");
EXTERN char_u e_nobang[]        INIT(= "E477: No ! allowed");
EXTERN char_u e_nogvim[]        INIT(= "E25: GUI cannot be used: Not enabled at compile time");
EXTERN char_u e_nofarsi[]       INIT(= "E27: Farsi cannot be used: Not enabled at compile time\n");
EXTERN char_u e_noarabic[]      INIT(= "E800: Arabic cannot be used: Not enabled at compile time\n");
EXTERN char_u e_nogroup[]       INIT(= "E28: No such highlight group name: %s");
EXTERN char_u e_noinstext[]     INIT(= "E29: No inserted text yet");
EXTERN char_u e_nolastcmd[]     INIT(= "E30: No previous command line");
EXTERN char_u e_nomap[]         INIT(= "E31: No such mapping");
EXTERN char_u e_nomatch[]       INIT(= "E479: No match");
EXTERN char_u e_nomatch2[]      INIT(= "E480: No match: %s");
EXTERN char_u e_noname[]        INIT(= "E32: No file name");
EXTERN char_u e_nopresub[]      INIT(= "E33: No previous substitute regular expression");
EXTERN char_u e_noprev[]        INIT(= "E34: No previous command");
EXTERN char_u e_noprevre[]      INIT(= "E35: No previous regular expression");
EXTERN char_u e_norange[]       INIT(= "E481: No range allowed");
EXTERN char_u e_noroom[]        INIT(= "E36: Not enough room");
EXTERN char_u e_notcreate[]     INIT(= "E482: Can't create file %s");
EXTERN char_u e_notmp[]         INIT(= "E483: Can't get temp file name");
EXTERN char_u e_notopen[]       INIT(= "E484: Can't open file %s");
EXTERN char_u e_notread[]       INIT(= "E485: Can't read file %s");
EXTERN char_u e_nowrtmsg[]      INIT(= "E37: No write since last change (add ! to override)");
EXTERN char_u e_nowrtmsg_nobang[]   INIT(= "E37: No write since last change");
EXTERN char_u e_null[]          INIT(= "E38: Null argument");
EXTERN char_u e_number_exp[]    INIT(= "E39: Number expected");
EXTERN char_u e_outofmem[]      INIT(= "E41: Out of memory!");
EXTERN char_u e_patnotf2[]      INIT(= "E486: Pattern not found: %s");
EXTERN char_u e_positive[]      INIT(= "E487: Argument must be positive");
EXTERN char_u e_prev_dir[]      INIT(= "E459: Cannot go back to previous directory");

EXTERN char_u e_re_damg[]       INIT(= "E43: Damaged match string");
EXTERN char_u e_re_corr[]       INIT(= "E44: Corrupted regexp program");
EXTERN char_u e_readonly[]      INIT(= "E45: 'readonly' option is set (add ! to override)");
EXTERN char_u e_readonlyvar[]   INIT(= "E46: Cannot change read-only variable \"%s\"");
EXTERN char_u e_readonlysbx[]   INIT(= "E794: Cannot set variable in the sandbox: \"%s\"");
EXTERN char_u e_sandbox[]       INIT(= "E48: Not allowed in sandbox");
EXTERN char_u e_secure[]        INIT(= "E523: Not allowed here");
EXTERN char_u e_screenmode[]    INIT(= "E359: Screen mode setting not supported");
EXTERN char_u e_scroll[]        INIT(= "E49: Invalid scroll size");
EXTERN char_u e_shellempty[]    INIT(= "E91: 'shell' option is empty");
EXTERN char_u e_swapclose[]     INIT(= "E72: Close error on swap file");
EXTERN char_u e_toocompl[]      INIT(= "E74: Command too complex");
EXTERN char_u e_longname[]      INIT(= "E75: Name too long");
EXTERN char_u e_toomsbra[]      INIT(= "E76: Too many [");
EXTERN char_u e_toomany[]       INIT(= "E77: Too many file names");
EXTERN char_u e_trailing[]      INIT(= "E488: Trailing characters");
EXTERN char_u e_umark[]         INIT(= "E78: Unknown mark");
EXTERN char_u e_wildexpand[]    INIT(= "E79: Cannot expand wildcards");
EXTERN char_u e_winheight[]     INIT(= "E591: 'winheight' cannot be smaller than 'winminheight'");
EXTERN char_u e_winwidth[]      INIT(= "E592: 'winwidth' cannot be smaller than 'winminwidth'");
EXTERN char_u e_write[]         INIT(= "E80: Error while writing");
EXTERN char_u e_zerocount[]     INIT(= "Zero count");
EXTERN char_u e_usingsid[]      INIT(= "E81: Using <SID> not in a script context");
EXTERN char_u e_intern2[]       INIT(= "E685: Internal error: %s");
EXTERN char_u e_maxmempat[]     INIT(= "E363: pattern uses more memory than 'maxmempattern'");
EXTERN char_u e_emptybuf[]      INIT(= "E749: empty buffer");
EXTERN char_u e_nobufnr[]       INIT(= "E86: Buffer %ld does not exist");

EXTERN char_u e_invalpat[]      INIT(= "E682: Invalid search pattern or delimiter");
EXTERN char_u e_bufloaded[]     INIT(= "E139: File is loaded in another buffer");
EXTERN char_u e_notset[]        INIT(= "E764: Option '%s' is not set");

EXTERN char top_bot_msg[] INIT(= "search hit TOP, continuing at BOTTOM");
EXTERN char bot_top_msg[] INIT(= "search hit BOTTOM, continuing at TOP");

/* For undo we need to know the lowest time possible. */
EXTERN time_t starttime;

/*
 * Some compilers warn for not using a return value, but in some situations we
 * can't do anything useful with the value.  Assign to this variable to avoid the warning.
 */
EXTERN int ignored;
EXTERN char *ignoredp;

/* ----------------------------------------------------------------------- */

/* Note: a NULL argument for vim_realloc() is not portable, don't use it. */
#define vim_realloc(ptr, size)  realloc((ptr), (size))

/*
 * Return byte length of character that starts with byte "b".
 * Returns 1 for a single-byte character.
 * MB_BYTE2LEN_CHECK() can be used to count a special key as one byte.
 * Don't call MB_BYTE2LEN(b) with b < 0 or b > 255!
 */
#define MB_BYTE2LEN(b)         mb_bytelen_tab[b]
#define MB_BYTE2LEN_CHECK(b)   (((b) < 0 || (b) > 255) ? 1 : mb_bytelen_tab[b])

/* properties used in enc_canon_table[] (first three mutually exclusive) */
#define ENC_8BIT       0x01
#define ENC_UNICODE    0x04

#define ENC_LATIN1     0x200       /* Latin1 */

#if defined(USE_ICONV)
#if !defined(EILSEQ)
#define EILSEQ 123
#endif
#define ICONV_ERRNO errno
#define ICONV_E2BIG  E2BIG
#define ICONV_EINVAL EINVAL
#define ICONV_EILSEQ EILSEQ
#endif

/* ISSYMLINK(mode) tests if a file is a symbolic link. */
#if (defined(S_IFMT) && defined(S_IFLNK)) || defined(S_ISLNK)
#define HAVE_ISSYMLINK
#if defined(S_IFMT) && defined(S_IFLNK)
#define ISSYMLINK(mode) (((mode) & S_IFMT) == S_IFLNK)
#else
#define ISSYMLINK(mode) S_ISLNK(mode)
#endif
#endif

/* values for vim_handle_signal() that are not a signal */
#define SIGNAL_BLOCK    -1
#define SIGNAL_UNBLOCK  -2

/* behavior for bad character, "++bad=" argument */
#define BAD_REPLACE     '?'     /* replace it with '?' (default) */
#define BAD_KEEP        -1      /* leave it */
#define BAD_DROP        -2      /* erase it */

/* last argument for do_source() */
#define DOSO_NONE       0
#define DOSO_VIMRC      1       /* loading vimrc file */
#define DOSO_GVIMRC     2       /* loading gvimrc file */

/* flags for buf_freeall() */
#define BFA_DEL         1       /* buffer is going to be deleted */
#define BFA_WIPE        2       /* buffer is going to be wiped out */
#define BFA_KEEP_UNDO   4       /* do not free undo information */

/* direction for nv_mousescroll() and ins_mousescroll() */
#define MSCR_DOWN       0       /* DOWN must be FALSE */
#define MSCR_UP         1
#define MSCR_LEFT       -1
#define MSCR_RIGHT      -2

#define KEYLEN_PART_KEY -1      /* keylen value for incomplete key-code */
#define KEYLEN_PART_MAP -2      /* keylen value for incomplete mapping */
#define KEYLEN_REMOVED  9999    /* keylen value for removed sequence */

/* Return value from get_option_value_strict */
#define SOPT_BOOL       0x01    /* Boolean option */
#define SOPT_NUM        0x02    /* Number option */
#define SOPT_STRING     0x04    /* String option */
#define SOPT_GLOBAL     0x08    /* Option has global value */
#define SOPT_WIN        0x10    /* Option has window-local value */
#define SOPT_BUF        0x20    /* Option has buffer-local value */
#define SOPT_UNSET      0x40    /* Option does not have local value set */

/* Option types for various functions in option.c */
#define SREQ_GLOBAL     0       /* Request global option */
#define SREQ_WIN        1       /* Request window-local option */
#define SREQ_BUF        2       /* Request buffer-local option */

/* Flags for get_reg_contents */
#define GREG_NO_EXPR    1       /* Do not allow expression register */
#define GREG_EXPR_SRC   2       /* Return expression itself for "=" register */
#define GREG_LIST       4       /* Return list */

/* Character used as separated in autoload function/variable names. */
#define AUTOLOAD_CHAR '#'

#define SET_NO_HLSEARCH(flag) no_hlsearch = (flag); set_vim_var_nr(VV_HLSEARCH, !no_hlsearch && p_hls)

#endif
