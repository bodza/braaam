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
public class VimD
{
    /*
     * option.h: definition of global variables for settable options
     */

    /* Default values for "b_p_ff" 'fileformat' and "p_ffs" 'fileformats'. */
    /*private*/ static final Bytes FF_DOS       = u8("dos");
    /*private*/ static final Bytes FF_MAC       = u8("mac");
    /*private*/ static final Bytes FF_UNIX      = u8("unix");

    /* end-of-line style */
    /*private*/ static final int EOL_UNKNOWN    = -1;       /* not defined yet */
    /*private*/ static final int EOL_UNIX       = 0;        /* NL */
    /*private*/ static final int EOL_DOS        = 1;        /* CR NL */
    /*private*/ static final int EOL_MAC        = 2;        /* CR */

    /* Formatting options for "p_fo" 'formatoptions'. */
    /*private*/ static final byte
        FO_WRAP         = 't',
        FO_WRAP_COMS    = 'c',
        FO_RET_COMS     = 'r',
        FO_OPEN_COMS    = 'o',
        FO_Q_COMS       = 'q',
        FO_Q_NUMBER     = 'n',
        FO_Q_SECOND     = '2',
        FO_INS_VI       = 'v',
        FO_INS_LONG     = 'l',
        FO_INS_BLANK    = 'b',
        FO_MBYTE_BREAK  = 'm',  /* break before/after multi-byte char */
        FO_MBYTE_JOIN   = 'M',  /* no space before/after multi-byte char */
        FO_MBYTE_JOIN2  = 'B',  /* no space between multi-byte chars */
        FO_ONE_LETTER   = '1',
        FO_WHITE_PAR    = 'w',  /* trailing white space continues paragr. */
        FO_AUTO         = 'a',  /* automatic formatting */
        FO_REMOVE_COMS  = 'j';  /* remove comment leaders when joining lines */

    /*private*/ static final Bytes DFLT_FO_VIM  = u8("tcq");
    /*private*/ static final Bytes FO_ALL       = u8("tcroq2vlb1mMBn,awj");    /* for do_set() */

    /* characters for the "p_cpo" option: */
    /*private*/ static final byte
        CPO_ALTREAD     = 'a',  /* ":read" sets alternate file name */
        CPO_ALTWRITE    = 'A',  /* ":write" sets alternate file name */
        CPO_BAR         = 'b',  /* "\|" ends a mapping */
        CPO_BSLASH      = 'B',  /* backslash in mapping is not special */
        CPO_SEARCH      = 'c',
        CPO_CONCAT      = 'C',  /* don't concatenate sourced lines */
        CPO_DOTTAG      = 'd',  /* "./tags" in 'tags' is in current dir */
        CPO_DIGRAPH     = 'D',  /* no digraph after "r", "f", etc. */
        CPO_EXECBUF     = 'e',
        CPO_EMPTYREGION = 'E',  /* operating on empty region is an error */
        CPO_FNAMER      = 'f',  /* set file name for ":r file" */
        CPO_FNAMEW      = 'F',  /* set file name for ":w file" */
        CPO_GOTO1       = 'g',  /* goto line 1 for ":edit" */
        CPO_INSEND      = 'H',  /* "I" inserts before last blank in line */
        CPO_INTMOD      = 'i',  /* interrupt a read makes buffer modified */
        CPO_INDENT      = 'I',  /* remove auto-indent more often */
        CPO_JOINSP      = 'j',  /* only use two spaces for join after '.' */
        CPO_ENDOFSENT   = 'J',  /* need two spaces to detect end of sentence */
        CPO_KEYCODE     = 'k',  /* don't recognize raw key code in mappings */
        CPO_KOFFSET     = 'K',  /* don't wait for key code in mappings */
        CPO_LITERAL     = 'l',  /* take char after backslash in [] literal */
        CPO_LISTWM      = 'L',  /* 'list' changes wrapmargin */
        CPO_SHOWMATCH   = 'm',
        CPO_MATCHBSL    = 'M',  /* "%" ignores use of backslashes */
        CPO_NUMCOL      = 'n',  /* 'number' column also used for text */
        CPO_LINEOFF     = 'o',
        CPO_OVERNEW     = 'O',  /* silently overwrite new file */
        CPO_LISP        = 'p',  /* 'lisp' indenting */
        CPO_FNAMEAPP    = 'P',  /* set file name for ":w >>file" */
        CPO_JOINCOL     = 'q',  /* with "3J" use column after first join */
        CPO_REDO        = 'r',
        CPO_REMMARK     = 'R',  /* remove marks when filtering */
        CPO_BUFOPT      = 's',
        CPO_BUFOPTGLOB  = 'S',
        CPO_TAGPAT      = 't',
        CPO_UNDO        = 'u',  /* "u" undoes itself */
        CPO_BACKSPACE   = 'v',  /* "v" keep deleted text */
        CPO_CW          = 'w',  /* "cw" only changes one blank */
        CPO_FWRITE      = 'W',  /* "w!" doesn't overwrite readonly files */
        CPO_ESC         = 'x',
        CPO_REPLCNT     = 'X',  /* "R" with a count only deletes chars once */
        CPO_YANK        = 'y',
        CPO_KEEPRO      = 'Z',  /* don't reset 'readonly' on ":w!" */
        CPO_DOLLAR      = '$',
        CPO_FILTER      = '!',
        CPO_MATCH       = '%',
        CPO_STAR        = '*',  /* ":*" means ":@" */
        CPO_PLUS        = '+',  /* ":write file" resets 'modified' */
        CPO_MINUS       = '-',  /* "9-" fails at and before line 9 */
        CPO_SPECI       = '<',  /* don't recognize <> in mappings */
        CPO_REGAPPEND   = '>',  /* insert NL when appending to a register */

    /* POSIX flags */
        CPO_HASH        = '#',  /* "D", "o" and "O" do not use a count */
        CPO_PARA        = '{',  /* "{" is also a paragraph boundary */
        CPO_TSIZE       = '|',  /* $LINES and $COLUMNS overrule term size */
        CPO_PRESERVE    = '&',  /* keep swap file after :preserve */
        CPO_SUBPERCENT  = '/',  /* % in :s string uses previous one */
        CPO_BACKSL      = '\\', /* \ is not special in [] */
        CPO_CHDIR       = '.',  /* don't chdir if buffer is modified */
        CPO_SCOLON      = ';';  /* using "," and ";" will skip over char if cursor would not move */

    /* default values for Vim, Vi and POSIX */
    /*private*/ static final Bytes CPO_VIM  = u8("aABceFs");
    /*private*/ static final Bytes CPO_ALL  = u8("aAbBcCdDeEfFgHiIjJkKlLmMnoOpPqrRsStuvwWxXyZ$!%*-+<>#{|&/\\.;");

    /* characters for "p_ww" option: */
    /*private*/ static final Bytes WW_ALL   = u8("bshl<>[],~");

    /* characters for "p_mouse" option: */
    /*private*/ static final byte MOUSE_NORMAL  = 'n';              /* use mouse in Normal mode */
    /*private*/ static final byte MOUSE_VISUAL  = 'v';              /* use mouse in Visual/Select mode */
    /*private*/ static final byte MOUSE_INSERT  = 'i';              /* use mouse in Insert mode */
    /*private*/ static final byte MOUSE_COMMAND = 'c';              /* use mouse in Command-line mode */
    /*private*/ static final byte MOUSE_HELP    = 'h';              /* use mouse in help buffers */
    /*private*/ static final byte MOUSE_RETURN  = 'r';              /* use mouse for hit-return message */
    /*private*/ static final Bytes MOUSE_A      = u8("nvich");      /* used for 'a' flag */
    /*private*/ static final Bytes MOUSE_ALL    = u8("anvichr");    /* all possible characters */
    /*private*/ static final byte MOUSE_NONE    = ' ';              /* don't use Visual selection */
    /*private*/ static final byte MOUSE_NONEF   = 'x';              /* forced modeless selection */

    /*private*/ static final Bytes COCU_ALL     = u8("nvic");       /* flags for 'concealcursor' */

    /* characters for "p_shm" option: */
    /*private*/ static final byte SHM_RO        = 'r';              /* readonly */
    /*private*/ static final byte SHM_MOD       = 'm';              /* modified */
    /*private*/ static final byte SHM_FILE      = 'f';              /* (file 1 of 2) */
    /*private*/ static final byte SHM_LAST      = 'i';              /* last line incomplete */
    /*private*/ static final byte SHM_TEXT      = 'x';              /* tx instead of textmode */
    /*private*/ static final byte SHM_LINES     = 'l';              /* "L" instead of "lines" */
    /*private*/ static final byte SHM_NEW       = 'n';              /* "[New]" instead of "[New file]" */
    /*private*/ static final byte SHM_WRI       = 'w';              /* "[w]" instead of "written" */
    /*private*/ static final Bytes SHM_A        = u8("rmfixlnw");   /* represented by 'a' flag */
    /*private*/ static final byte SHM_WRITE     = 'W';              /* don't use "written" at all */
    /*private*/ static final byte SHM_TRUNC     = 't';              /* trunctate file messages */
    /*private*/ static final byte SHM_TRUNCALL  = 'T';              /* trunctate all messages */
    /*private*/ static final byte SHM_OVER      = 'o';              /* overwrite file messages */
    /*private*/ static final byte SHM_OVERALL   = 'O';              /* overwrite more messages */
    /*private*/ static final byte SHM_SEARCH    = 's';              /* no search hit bottom messages */
    /*private*/ static final byte SHM_ATTENTION = 'A';              /* no ATTENTION messages */
    /*private*/ static final byte SHM_INTRO     = 'I';              /* intro messages */
    /*private*/ static final byte SHM_COMPLETIONMENU = 'c';         /* completion menu messages */
    /*private*/ static final Bytes SHM_ALL      = u8("rmfixlnwaWtToOsAIc"); /* all possible flags for 'shm' */

    /* characters for p_go: */
    /*private*/ static final byte GO_ASEL       = 'a';              /* autoselect */
    /*private*/ static final byte GO_ASELML     = 'A';              /* autoselect modeless selection */
    /*private*/ static final byte GO_BOT        = 'b';              /* use bottom scrollbar */
    /*private*/ static final byte GO_CONDIALOG  = 'c';              /* use console dialog */
    /*private*/ static final byte GO_TABLINE    = 'e';              /* may show tabline */
    /*private*/ static final byte GO_FORG       = 'f';              /* start GUI in foreground */
    /*private*/ static final byte GO_GREY       = 'g';              /* use grey menu items */
    /*private*/ static final byte GO_HORSCROLL  = 'h';              /* flexible horizontal scrolling */
    /*private*/ static final byte GO_ICON       = 'i';              /* use Vim icon */
    /*private*/ static final byte GO_LEFT       = 'l';              /* use left scrollbar */
    /*private*/ static final byte GO_VLEFT      = 'L';              /* left scrollbar with vert split */
    /*private*/ static final byte GO_MENUS      = 'm';              /* use menu bar */
    /*private*/ static final byte GO_NOSYSMENU  = 'M';              /* don't source system menu */
    /*private*/ static final byte GO_POINTER    = 'p';              /* pointer enter/leave callbacks */
    /*private*/ static final byte GO_ASELPLUS   = 'P';              /* autoselectPlus */
    /*private*/ static final byte GO_RIGHT      = 'r';              /* use right scrollbar */
    /*private*/ static final byte GO_VRIGHT     = 'R';              /* right scrollbar with vert split */
    /*private*/ static final byte GO_TEAROFF    = 't';              /* add tear-off menu items */
    /*private*/ static final byte GO_TOOLBAR    = 'T';              /* add toolbar */
    /*private*/ static final byte GO_FOOTER     = 'F';              /* add footer */
    /*private*/ static final byte GO_VERTICAL   = 'v';              /* arrange dialog buttons vertically */
    /*private*/ static final Bytes GO_ALL       = u8("aAbcefFghilmMprtTv"); /* all possible flags for 'go' */

    /* flags for 'comments' option */
    /*private*/ static final byte COM_NEST      = 'n';              /* comments strings nest */
    /*private*/ static final byte COM_BLANK     = 'b';              /* needs blank after string */
    /*private*/ static final byte COM_START     = 's';              /* start of comment */
    /*private*/ static final byte COM_MIDDLE    = 'm';              /* middle of comment */
    /*private*/ static final byte COM_END       = 'e';              /* end of comment */
    /*private*/ static final byte COM_AUTO_END  = 'x';              /* last char of end closes comment */
    /*private*/ static final byte COM_FIRST     = 'f';              /* first line comment only */
    /*private*/ static final byte COM_LEFT      = 'l';              /* left adjusted */
    /*private*/ static final byte COM_RIGHT     = 'r';              /* right adjusted */
    /*private*/ static final byte COM_NOBACK    = 'O';              /* don't use for "O" command */
    /*private*/ static final Bytes COM_ALL      = u8("nbsmexflrO"); /* all flags for 'comments' option */
    /*private*/ static final int COM_MAX_LEN    = 50;               /* maximum length of a part */

    /* flags for 'statusline' option */
    /*private*/ static final byte
        STL_FILEPATH        = 'f',      /* path of file in buffer */
        STL_FULLPATH        = 'F',      /* full path of file in buffer */
        STL_FILENAME        = 't',      /* last part (tail) of file path */
        STL_COLUMN          = 'c',      /* column og cursor */
        STL_VIRTCOL         = 'v',      /* virtual column */
        STL_VIRTCOL_ALT     = 'V',      /* - with 'if different' display */
        STL_LINE            = 'l',      /* line number of cursor */
        STL_NUMLINES        = 'L',      /* number of lines in buffer */
        STL_BUFNO           = 'n',      /* current buffer number */
        STL_KEYMAP          = 'k',      /* 'keymap' when active */
        STL_OFFSET          = 'o',      /* offset of character under cursor */
        STL_OFFSET_X        = 'O',      /* - in hexadecimal */
        STL_BYTEVAL         = 'b',      /* byte value of character */
        STL_BYTEVAL_X       = 'B',      /* - in hexadecimal */
        STL_ROFLAG          = 'r',      /* readonly flag */
        STL_ROFLAG_ALT      = 'R',      /* - other display */
        STL_HELPFLAG        = 'h',      /* window is showing a help file */
        STL_HELPFLAG_ALT    = 'H',      /* - other display */
        STL_FILETYPE        = 'y',      /* 'filetype' */
        STL_FILETYPE_ALT    = 'Y',      /* - other display */
        STL_PREVIEWFLAG     = 'w',      /* window is showing the preview buf */
        STL_PREVIEWFLAG_ALT = 'W',      /* - other display */
        STL_MODIFIED        = 'm',      /* modified flag */
        STL_MODIFIED_ALT    = 'M',      /* - other display */
        STL_QUICKFIX        = 'q',      /* quickfix window description */
        STL_PERCENTAGE      = 'p',      /* percentage through file */
        STL_ALTPERCENT      = 'P',      /* percentage as TOP BOT ALL or NN% */
        STL_ARGLISTSTAT     = 'a',      /* argument list status as (x of y) */
        STL_PAGENUM         = 'N',      /* page number (when printing) */
        STL_VIM_EXPR        = '{',      /* start of expression to substitute */
        STL_MIDDLEMARK      = '=',      /* separation between left and right */
        STL_TRUNCMARK       = '<',      /* truncation mark if line is too long */
        STL_USER_HL         = '*',      /* highlight from (User)1..9 or 0 */
        STL_HIGHLIGHT       = '#',      /* highlight name */
        STL_TABPAGENR       = 'T',      /* tab page label nr */
        STL_TABCLOSENR      = 'X';      /* tab page close nr */
    /*private*/ static final Bytes STL_ALL = u8("fFtcvVlLknoObBrRhHmYyWwMqpPaN{#");

    /* flags used for parsed 'wildmode' */
    /*private*/ static final int WIM_FULL       = 1;
    /*private*/ static final int WIM_LONGEST    = 2;
    /*private*/ static final int WIM_LIST       = 4;

    /* arguments for can_bs() */
    /*private*/ static final byte BS_INDENT     = 'i';      /* "Indent" */
    /*private*/ static final byte BS_EOL        = 'o';      /* "eOl" */
    /*private*/ static final byte BS_START      = 's';      /* "Start" */

    /*private*/ static final Bytes LISPWORD_VALUE = u8("defun,define,defmacro,set!,lambda,if,case,let,flet,let*,letrec,do,do*,define-syntax,let-syntax,letrec-syntax,destructuring-bind,defpackage,defparameter,defstruct,deftype,defvar,do-all-symbols,do-external-symbols,do-symbols,dolist,dotimes,ecase,etypecase,eval-when,labels,macrolet,multiple-value-bind,multiple-value-call,multiple-value-prog1,multiple-value-setq,prog1,progv,typecase,unless,unwind-protect,when,with-input-from-string,with-open-file,with-open-stream,with-output-to-string,with-package-iterator,define-condition,handler-bind,handler-case,restart-bind,restart-case,with-simple-restart,store-value,use-value,muffle-warning,abort,continue,with-slots,with-slots*,with-accessors,with-accessors*,defclass,defmethod,print-unreadable-object");

    /*
     * The following are actual variables for the options:
     */

    /*private*/ static long[]    p_aleph     = new long[1];     /* 'aleph' */
    /*private*/ static Bytes[]   p_ambw      = new Bytes[1];    /* 'ambiwidth' */
    /*private*/ static boolean[] p_ar        = new boolean[1];  /* 'autoread' */
    /*private*/ static boolean[] p_aw        = new boolean[1];  /* 'autowrite' */
    /*private*/ static boolean[] p_awa       = new boolean[1];  /* 'autowriteall' */
    /*private*/ static Bytes[]   p_bs        = new Bytes[1];    /* 'backspace' */
    /*private*/ static Bytes[]   p_bg        = new Bytes[1];    /* 'background' */
    /*private*/ static Bytes[]   p_breakat   = new Bytes[1];    /* 'breakat' */
    /*private*/ static Bytes[]   p_cedit     = new Bytes[1];    /* 'cedit' */
    /*private*/ static long[]    p_cwh       = new long[1];     /* 'cmdwinheight' */
    /*private*/ static Bytes[]   p_cb        = new Bytes[1];    /* 'clipboard' */
    /*private*/ static long[]    p_ch        = new long[1];     /* 'cmdheight' */
    /*private*/ static boolean[] p_confirm   = new boolean[1];  /* 'confirm' */
    /*private*/ static Bytes[]   p_cpo       = new Bytes[1];    /* 'cpoptions' */
    /*private*/ static Bytes[]   p_debug     = new Bytes[1];    /* 'debug' */
    /*private*/ static boolean[] p_deco      = new boolean[1];  /* 'delcombine' */
    /*private*/ static boolean[] p_dg        = new boolean[1];  /* 'digraph' */
    /*private*/ static Bytes[]   p_dy        = new Bytes[1];    /* 'display' */

    /*private*/ static final int
        DY_LASTLINE = 0x001,
        DY_UHEX     = 0x002;
    /*private*/ static int[]    dy_flags    = new int[1];
    /*private*/ static final Bytes[] p_dy_values =
    {
        u8("lastline"), u8("uhex"), null
    };

    /*private*/ static Bytes[]   p_ead       = new Bytes[1];    /* 'eadirection' */
    /*private*/ static boolean[] p_ea        = new boolean[1];  /* 'equalalways' */
    /*private*/ static Bytes[]   p_ep        = new Bytes[1];    /* 'equalprg' */
    /*private*/ static boolean[] p_eb        = new boolean[1];  /* 'errorbells' */
    /*private*/ static Bytes[]   p_ei        = new Bytes[1];    /* 'eventignore' */
    /*private*/ static boolean[] p_ek        = new boolean[1];  /* 'esckeys' */
    /*private*/ static boolean[] p_exrc      = new boolean[1];  /* 'exrc' */
    /*private*/ static Bytes[]   p_fencs     = new Bytes[1];    /* 'fileencodings' */
    /*private*/ static Bytes[]   p_ffs       = new Bytes[1];    /* 'fileformats' */
    /*private*/ static Bytes[]   p_fp        = new Bytes[1];    /* 'formatprg' */
    /*private*/ static boolean[] p_fs        = new boolean[1];  /* 'fsync' */
    /*private*/ static boolean[] p_gd        = new boolean[1];  /* 'gdefault' */
    /*private*/ static boolean[] p_prompt    = new boolean[1];  /* 'prompt' */
    /*private*/ static boolean[] p_hid       = new boolean[1];  /* 'hidden' */
    /*private*/ static Bytes[]   p_hl        = new Bytes[1];    /* 'highlight' */
    /*private*/ static boolean[] p_hls       = new boolean[1];  /* 'hlsearch' */
    /*private*/ static long[]    p_hi        = new long[1];     /* 'history' */
    /*private*/ static boolean[] p_ic        = new boolean[1];  /* 'ignorecase' */
    /*private*/ static boolean[] p_is        = new boolean[1];  /* 'incsearch' */
    /*private*/ static boolean[] p_im        = new boolean[1];  /* 'insertmode' */
    /*private*/ static Bytes[]   p_isf       = new Bytes[1];    /* 'isfname' */
    /*private*/ static Bytes[]   p_isi       = new Bytes[1];    /* 'isident' */
    /*private*/ static Bytes[]   p_isp       = new Bytes[1];    /* 'isprint' */
    /*private*/ static boolean[] p_js        = new boolean[1];  /* 'joinspaces' */
    /*private*/ static Bytes[]   p_kp        = new Bytes[1];    /* 'keywordprg' */
    /*private*/ static Bytes[]   p_km        = new Bytes[1];    /* 'keymodel' */
    /*private*/ static Bytes[]   p_lispwords = new Bytes[1];    /* 'lispwords' */
    /*private*/ static long[]    p_ls        = new long[1];     /* 'laststatus' */
    /*private*/ static long[]    p_stal      = new long[1];     /* 'showtabline' */
    /*private*/ static Bytes[]   p_lcs       = new Bytes[1];    /* 'listchars' */
    /*private*/ static boolean[] p_lz        = new boolean[1];  /* 'lazyredraw' */
    /*private*/ static boolean[] p_magic     = new boolean[1];  /* 'magic' */
    /*private*/ static long[]    p_mat       = new long[1];     /* 'matchtime' */
    /*private*/ static long[]    p_mco       = new long[1];     /* 'maxcombine' */
    /*private*/ static long[]    p_mfd       = new long[1];     /* 'maxfuncdepth' */
    /*private*/ static long[]    p_mmd       = new long[1];     /* 'maxmapdepth' */
    /*private*/ static long[]    p_mmp       = new long[1];     /* 'maxmempattern' */
    /*private*/ static Bytes[]   p_mouse     = new Bytes[1];    /* 'mouse' */
    /*private*/ static Bytes[]   p_mousem    = new Bytes[1];    /* 'mousemodel' */
    /*private*/ static long[]    p_mouset    = new long[1];     /* 'mousetime' */
    /*private*/ static boolean[] p_more      = new boolean[1];  /* 'more' */
    /*private*/ static Bytes[]   p_opfunc    = new Bytes[1];    /* 'operatorfunc' */
    /*private*/ static Bytes[]   p_para      = new Bytes[1];    /* 'paragraphs' */
    /*private*/ static boolean[] p_paste     = new boolean[1];  /* 'paste' */
    /*private*/ static Bytes[]   p_pt        = new Bytes[1];    /* 'pastetoggle' */
    /*private*/ static long[]    p_rdt       = new long[1];     /* 'redrawtime' */
    /*private*/ static boolean[] p_remap     = new boolean[1];  /* 'remap' */
    /*private*/ static long[]    p_re        = new long[1];     /* 'regexpengine' */
    /*private*/ static long[]    p_report    = new long[1];     /* 'report' */
    /*private*/ static boolean[] p_ari       = new boolean[1];  /* 'allowrevins' */
    /*private*/ static boolean[] p_ri        = new boolean[1];  /* 'revins' */
    /*private*/ static boolean[] p_ru        = new boolean[1];  /* 'ruler' */
    /*private*/ static Bytes[]   p_ruf       = new Bytes[1];    /* 'rulerformat' */
    /*private*/ static Bytes[]   p_rtp       = new Bytes[1];    /* 'runtimepath' */
    /*private*/ static long[]    p_sj        = new long[1];     /* 'scrolljump' */
    /*private*/ static long[]    p_so        = new long[1];     /* 'scrolloff' */
    /*private*/ static Bytes[]   p_sbo       = new Bytes[1];    /* 'scrollopt' */
    /*private*/ static Bytes[]   p_sections  = new Bytes[1];    /* 'sections' */
    /*private*/ static boolean[] p_secure    = new boolean[1];  /* 'secure' */
    /*private*/ static Bytes[]   p_sel       = new Bytes[1];    /* 'selection' */
    /*private*/ static Bytes[]   p_slm       = new Bytes[1];    /* 'selectmode' */
    /*private*/ static Bytes[]   p_stl       = new Bytes[1];    /* 'statusline' */
    /*private*/ static boolean[] p_sr        = new boolean[1];  /* 'shiftround' */
    /*private*/ static Bytes[]   p_shm       = new Bytes[1];    /* 'shortmess' */
    /*private*/ static Bytes[]   p_sbr       = new Bytes[1];    /* 'showbreak' */
    /*private*/ static boolean[] p_sc        = new boolean[1];  /* 'showcmd' */
    /*private*/ static boolean[] p_sm        = new boolean[1];  /* 'showmatch' */
    /*private*/ static boolean[] p_smd       = new boolean[1];  /* 'showmode' */
    /*private*/ static long[]    p_ss        = new long[1];     /* 'sidescroll' */
    /*private*/ static long[]    p_siso      = new long[1];     /* 'sidescrolloff' */
    /*private*/ static boolean[] p_scs       = new boolean[1];  /* 'smartcase' */
    /*private*/ static boolean[] p_sta       = new boolean[1];  /* 'smarttab' */
    /*private*/ static boolean[] p_sb        = new boolean[1];  /* 'splitbelow' */
    /*private*/ static long[]    p_tpm       = new long[1];     /* 'tabpagemax' */
    /*private*/ static Bytes[]   p_tal       = new Bytes[1];    /* 'tabline' */
    /*private*/ static boolean[] p_spr       = new boolean[1];  /* 'splitright' */
    /*private*/ static boolean[] p_sol       = new boolean[1];  /* 'startofline' */
    /*private*/ static Bytes[]   p_swb       = new Bytes[1];    /* 'switchbuf' */

    /*private*/ static final int
        SWB_USEOPEN = 0x001,
        SWB_USETAB  = 0x002,
        SWB_SPLIT   = 0x004,
        SWB_NEWTAB  = 0x008;
    /*private*/ static int[]    swb_flags   = new int[1];
    /*private*/ static final Bytes[] p_swb_values =
    {
        u8("useopen"), u8("usetab"), u8("split"), u8("newtab"), null
    };

    /*private*/ static boolean[] p_terse     = new boolean[1];  /* 'terse' */
    /*private*/ static boolean[] p_to        = new boolean[1];  /* 'tildeop' */
    /*private*/ static boolean[] p_timeout   = new boolean[1];  /* 'timeout' */
    /*private*/ static long[]    p_tm        = new long[1];     /* 'timeoutlen' */
    /*private*/ static boolean[] p_ttimeout  = new boolean[1];  /* 'ttimeout' */
    /*private*/ static long[]    p_ttm       = new long[1];     /* 'ttimeoutlen' */
    /*private*/ static boolean[] p_tf        = new boolean[1];  /* 'ttyfast' */
    /*private*/ static long[]    p_ttyscroll = new long[1];     /* 'ttyscroll' */
    /*private*/ static Bytes[]   p_ttym      = new Bytes[1];    /* 'ttymouse' */

    /*private*/ static final int
        TTYM_XTERM  = 0x01,
        TTYM_XTERM2 = 0x02;
    /*private*/ static int[]    ttym_flags  = new int[1];
    /*private*/ static final Bytes[] p_ttym_values =
    {
        u8("xterm"), u8("xterm2"), null
    };

    /*private*/ static Bytes[]   p_udir      = new Bytes[1];    /* 'undodir' */
    /*private*/ static long[]    p_ul        = new long[1];     /* 'undolevels' */
    /*private*/ static long[]    p_ur        = new long[1];     /* 'undoreload' */
    /*private*/ static long[]    p_ut        = new long[1];     /* 'updatetime' */
    /*private*/ static Bytes[]   p_fcs       = new Bytes[1];    /* 'fillchar' */
    /*private*/ static boolean[] p_vb        = new boolean[1];  /* 'visualbell' */
    /*private*/ static Bytes[]   p_ve        = new Bytes[1];    /* 'virtualedit' */

    /*private*/ static final int
        VE_BLOCK   = 5,        /* includes "all" */
        VE_INSERT  = 6,        /* includes "all" */
        VE_ALL     = 4,
        VE_ONEMORE = 8;
    /*private*/ static int[]    ve_flags    = new int[1];
    /*private*/ static final Bytes[] p_ve_values =
    {
        u8("block"), u8("insert"), u8("all"), u8("onemore"), null
    };

    /*private*/ static long[]    p_verbose   = new long[1];     /* 'verbose' */

    /*private*/ static Bytes[]   p_vfile = { u8("") };  /* used before options are initialized */

    /*private*/ static boolean[] p_warn      = new boolean[1];  /* 'warn' */
    /*private*/ static long[]    p_window    = new long[1];     /* 'window' */
    /*private*/ static boolean[] p_wiv       = new boolean[1];  /* 'weirdinvert' */
    /*private*/ static Bytes[]   p_ww        = new Bytes[1];    /* 'whichwrap' */
    /*private*/ static long[]    p_wc        = new long[1];     /* 'wildchar' */
    /*private*/ static long[]    p_wcm       = new long[1];     /* 'wildcharm' */
    /*private*/ static boolean[] p_wic       = new boolean[1];  /* 'wildignorecase' */
    /*private*/ static Bytes[]   p_wim       = new Bytes[1];    /* 'wildmode' */
    /*private*/ static long[]    p_wh        = new long[1];     /* 'winheight' */
    /*private*/ static long[]    p_wmh       = new long[1];     /* 'winminheight' */
    /*private*/ static long[]    p_wmw       = new long[1];     /* 'winminwidth' */
    /*private*/ static long[]    p_wiw       = new long[1];     /* 'winwidth' */
    /*private*/ static boolean[] p_ws        = new boolean[1];  /* 'wrapscan' */
    /*private*/ static boolean[] p_write     = new boolean[1];  /* 'write' */
    /*private*/ static boolean[] p_wa        = new boolean[1];  /* 'writeany' */
    /*private*/ static long[]    p_wd        = new long[1];     /* 'writedelay' */

    /*
     * "indir" values for buffer-local opions.
     * These need to be defined globally, so that the BV_COUNT can be used with b_p_scriptID[].
     */
    /*private*/ static final int
        BV_AI    =  0,
        BV_AR    =  1,
        BV_BIN   =  2,
        BV_BL    =  3,
        BV_BOMB  =  4,
        BV_CI    =  5,
        BV_CIN   =  6,
        BV_CINK  =  7,
        BV_CINO  =  8,
        BV_CINW  =  9,
        BV_CM    = 10,
        BV_COM   = 11,
        BV_EOL   = 12,
        BV_EP    = 13,
        BV_ET    = 14,
        BV_FENC  = 15,
        BV_FEX   = 16,
        BV_FF    = 17,
        BV_FLP   = 18,
        BV_FO    = 19,
        BV_FT    = 20,
        BV_IMI   = 21,
        BV_IMS   = 22,
        BV_INDE  = 23,
        BV_INDK  = 24,
        BV_INF   = 25,
        BV_ISK   = 26,
        BV_KP    = 27,
        BV_LISP  = 28,
        BV_LW    = 29,
        BV_MA    = 30,
        BV_MOD   = 31,
        BV_MPS   = 32,
        BV_NF    = 33,
        BV_PI    = 34,
        BV_QE    = 35,
        BV_RO    = 36,
        BV_SI    = 37,
        BV_SMC   = 38,
        BV_SYN   = 39,
        BV_STS   = 40,
        BV_SW    = 41,
        BV_TS    = 42,
        BV_TW    = 43,
        BV_TX    = 44,
        BV_UDF   = 45,
        BV_UL    = 46,
        BV_WM    = 47,
        BV_COUNT = 48;  /* must be the last one */

    /*
     * "indir" values for window-local options.
     * These need to be defined globally, so that the WV_COUNT can be used in the window structure.
     */
    /*private*/ static final int
        WV_LIST   =  0,
        WV_COCU   =  1,
        WV_COLE   =  2,
        WV_CRBIND =  3,
        WV_BRI    =  4,
        WV_BRIOPT =  5,
        WV_LBR    =  6,
        WV_NU     =  7,
        WV_RNU    =  8,
        WV_NUW    =  9,
        WV_RL     = 10,
        WV_RLC    = 11,
        WV_SCBIND = 12,
        WV_SCROLL = 13,
        WV_CUC    = 14,
        WV_CUL    = 15,
        WV_CC     = 16,
        WV_STL    = 17,
        WV_WFH    = 18,
        WV_WFW    = 19,
        WV_WRAP   = 20,
        WV_COUNT  = 21; /* must be the last one */

    /* Value for "b_p_ul" indicating the global value must be used. */
    /*private*/ static final int NO_LOCAL_UNDOLEVEL = -123456;

    /* ----------------------------------------------------------------------- */

    /*
     * position in file or buffer
     */
    /*private*/ static final class pos_C
    {
        long        lnum;       /* line number */
        int         col;        /* column number */
        int         coladd;

        /*private*/ pos_C()
        {
        }
    }

    /*private*/ static final pos_C NOPOS = new pos_C();

    /*private*/ static pos_C new_pos(long lnum, int col, int coladd)
    {
        pos_C p = new pos_C();
        p.lnum = lnum;
        p.col = col;
        p.coladd = coladd;
        return p;
    }

    /*private*/ static void COPY_pos(pos_C p1, pos_C p0)
    {
        p1.lnum = p0.lnum;
        p1.col = p0.col;
        p1.coladd = p0.coladd;
    }

    /*private*/ static pos_C[] ARRAY_pos(int n)
    {
        pos_C[] a = new pos_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new pos_C();
        return a;
    }

    /*
     * Same, but without coladd.
     */
    /*private*/ static final class lpos_C
    {
        long        lnum;   /* line number */
        int         col;    /* column number */

        /*private*/ lpos_C()
        {
        }
    }

    /*private*/ static void MIN1_lpos(lpos_C lp)
    {
        lp.lnum = -1;
        lp.col = -1;
    }

    /*private*/ static void COPY_lpos(lpos_C lp1, lpos_C lp0)
    {
        lp1.lnum = lp0.lnum;
        lp1.col = lp0.col;
    }

    /*private*/ static lpos_C[] ARRAY_lpos(int n)
    {
        lpos_C[] a = new lpos_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new lpos_C();
        return a;
    }

    /*private*/ static void COPY__lpos(lpos_C[] a1, lpos_C[] a0, int n)
    {
        for (int i = 0; i < n; i++)
            COPY_lpos(a1[i], a0[i]);
    }

    /* ----------------------------------------------------------------------- */

    /*
     * Structure used for growing byte arrays.
     * This is used to store information that only grows, is deleted all at once,
     * and needs to be accessed by index.  See ba_clear() and ba_grow().
     */
    /*private*/ static final class barray_C
    {
        int     ba_len;                 /* current number of items used */
        int     ba_maxlen;              /* maximum number of items possible */
        int     ba_growsize;            /* number of items to grow each time */
        byte[]  ba_data;                /* pointer to the first item */

        /*private*/ barray_C(int growsize)
        {
            ba_growsize = growsize;
        }
    }

    /*private*/ static void COPY_barray(barray_C ba1, barray_C ba0)
    {
        ba1.ba_len = ba0.ba_len;
        ba1.ba_maxlen = ba0.ba_maxlen;
        ba1.ba_growsize = ba0.ba_growsize;
        ba1.ba_data = ba0.ba_data;
    }

    /*
     * Clear an allocated growing array.
     */
    /*private*/ static void ba_clear(barray_C bap)
    {
        bap.ba_data = null;
        bap.ba_maxlen = 0;
        bap.ba_len = 0;
    }

    /*
     * Make room in growing array "bap" for at least "n" items.
     */
    /*private*/ static byte[] ba_grow(barray_C bap, int n)
    {
        if (bap.ba_maxlen - bap.ba_len < n)
        {
            if (n < bap.ba_growsize)
                n = bap.ba_growsize;
            n += bap.ba_len;
            byte[] data = new byte[n];
            if (bap.ba_data != null)
                ACOPY(data, 0, bap.ba_data, 0, bap.ba_maxlen);
            bap.ba_maxlen = n;
            bap.ba_data = data;
        }
        return bap.ba_data;
    }

    /*
     * Concatenate a string to a growarray which contains characters.
     * Note: Does NOT copy the NUL at the end!
     */
    /*private*/ static void ba_concat(barray_C bap, Bytes s)
    {
        int len = strlen(s);

        ba_grow(bap, len);
        ACOPY(bap.ba_data, bap.ba_len, s.array, s.index, len);
        bap.ba_len += len;
    }

    /*
     * Append one byte to a growarray which contains bytes.
     */
    /*private*/ static void ba_append(barray_C bap, byte b)
    {
        ba_grow(bap, 1);
        bap.ba_data[bap.ba_len++] = b;
    }

    /* ----------------------------------------------------------------------- */

    /*
     * Structure used for growing int arrays.
     * This is used to store information that only grows, is deleted all at once,
     * and needs to be accessed by index.  See ia_clear() and ia_grow().
     */
    /*private*/ static final class iarray_C
    {
        int     ia_len;                 /* current number of items used */
        int     ia_maxlen;              /* maximum number of items possible */
        int     ia_growsize;            /* number of items to grow each time */
        int[]   ia_data;                /* pointer to the first item */

        /*private*/ iarray_C(int growsize)
        {
            ia_growsize = growsize;
        }
    }

    /*private*/ static void COPY_iarray(iarray_C ia1, iarray_C ia0)
    {
        ia1.ia_len = ia0.ia_len;
        ia1.ia_maxlen = ia0.ia_maxlen;
        ia1.ia_growsize = ia0.ia_growsize;
        ia1.ia_data = ia0.ia_data;
    }

    /*
     * Clear an allocated growing array.
     */
    /*private*/ static void ia_clear(iarray_C iap)
    {
        iap.ia_data = null;
        iap.ia_maxlen = 0;
        iap.ia_len = 0;
    }

    /*
     * Make room in growing array "iap" for at least "n" items.
     */
    /*private*/ static int[] ia_grow(iarray_C iap, int n)
    {
        if (iap.ia_maxlen - iap.ia_len < n)
        {
            if (n < iap.ia_growsize)
                n = iap.ia_growsize;
            n += iap.ia_len;
            int[] data = new int[n];
            if (iap.ia_data != null)
                ACOPY(data, 0, iap.ia_data, 0, iap.ia_maxlen);
            iap.ia_maxlen = n;
            iap.ia_data = data;
        }
        return iap.ia_data;
    }

    /* ----------------------------------------------------------------------- */

    /*
     * Structure used for growing Object arrays.
     * This is used to store information that only grows, is deleted all at once,
     * and needs to be accessed by index.  See ga_clear() and ga_grow().
     */
    /*private*/ static final class Growing<T>
    {
        Class<T> ga_class;

        int     ga_len;                 /* current number of items used */
        int     ga_maxlen;              /* maximum number of items possible */
        int     ga_growsize;            /* number of items to grow each time */
        T[]     ga_data;                /* pointer to the first item */

        /*private*/ Growing(Class<T> type, int growsize)
        {
            ga_class = type;
            ga_growsize = growsize;
        }

        /*
         * Make room in growing array "gap" for at least "n" items.
         */
        /*private*/ T[] ga_grow(int n)
        {
            if (ga_maxlen - ga_len < n)
            {
                if (n < ga_growsize)
                    n = ga_growsize;
                n += ga_len;
                @SuppressWarnings("unchecked")
                T[] data = (T[])Array.newInstance(ga_class, n);
                if (ga_data != null)
                    ACOPY(data, 0, ga_data, 0, ga_maxlen);
                ga_maxlen = n;
                ga_data = data;
            }
            return ga_data;
        }

        /*
         * Clear an allocated growing array.
         */
        /*private*/ void ga_clear()
        {
            ga_data = null;
            ga_maxlen = 0;
            ga_len = 0;
        }
    }

    /*private*/ static <T> void COPY_garray(Growing<T> ga1, Growing<T> ga0)
    {
        ga1.ga_len = ga0.ga_len;
        ga1.ga_maxlen = ga0.ga_maxlen;
        ga1.ga_growsize = ga0.ga_growsize;
        ga1.ga_data = ga0.ga_data;
    }

    /* ----------------------------------------------------------------------- */

    /*
     * The number of sub-matches is limited to 10.
     * The first one (index 0) is the whole match, referenced with "\0".
     * The second one (index 1) is the first sub-match, referenced with "\1".
     * This goes up to the tenth (index 9), referenced with "\9".
     */
    /*private*/ static final int NSUBEXP  = 10;

    /*
     * In the NFA engine: how many braces are allowed.
     * TODO(RE): Use dynamic memory allocation instead of static, like here.
     */
    /*private*/ static final int NFA_MAX_BRACES = 20;

    /*
     * In the NFA engine: how many states are allowed.
     */
    /*private*/ static final int NFA_MAX_STATES = 100000;
    /*private*/ static final int NFA_TOO_EXPENSIVE = -1;

    /* Which regexp engine to use? Needed for vim_regcomp().
     * Must match with 'regexpengine'. */
    /*private*/ static final int AUTOMATIC_ENGINE    = 0;
    /*private*/ static final int BACKTRACKING_ENGINE = 1;
    /*private*/ static final int NFA_ENGINE          = 2;

    /*
     * Structure returned by vim_regcomp() to pass on to vim_regexec().
     * This is the general structure.  For the actual matcher, two specific
     * structures are used.  See code below.
     */
    /*private*/ static abstract class regprog_C
    {
        regengine_C         engine;
        int                 regflags;
        int                 re_engine;      /* automatic, backtracking or nfa engine */
        int                 re_flags;       /* second argument for vim_regcomp() */

        protected regprog_C()
        {
        }
    }

    /*
     * Structure used by the back track matcher.
     * These fields are only to be used in regexp.c!
     * See regexp.c for an explanation.
     */
    /*private*/ static final class bt_regprog_C extends regprog_C
    {
        int                 reganch;
        int                 regstart;
        Bytes               regmust;
        int                 regmlen;
        int                 reghasz;
        Bytes               program;

        /*private*/ bt_regprog_C()
        {
        }
    }

    /*
     * Structure representing a NFA state.
     * A NFA state may have no outgoing edge, when it is a NFA_MATCH state.
     */
    /*private*/ static final class nfa_state_C
    {
        int         c;
        fragnode_C  out0 = new fragnode_C();
        fragnode_C  out1 = new fragnode_C();
        int         id;
        int[]       lastlist = new int[2];  /* 0: normal, 1: recursive */
        int         val;

        /*private*/ nfa_state_C()
        {
        }

        /*private*/ void out0(nfa_state_C out0)
        {
            this.out0.fn_next = out0;
        }

        /*private*/ void out1(nfa_state_C out1)
        {
            this.out1.fn_next = out1;
        }

        /*private*/ nfa_state_C out0()
        {
            return (nfa_state_C)out0.fn_next;
        }

        /*private*/ nfa_state_C out1()
        {
            return (nfa_state_C)out1.fn_next;
        }
    }

    /*
     * Structure used by the NFA matcher.
     */
    /*private*/ static final class nfa_regprog_C extends regprog_C
    {
        nfa_state_C         start;          /* points into state[] */

        int                 reganch;        /* pattern starts with ^ */
        int                 regstart;       /* char at start of pattern */
        Bytes               match_text;     /* plain text to match with */

        boolean             has_zend;       /* pattern contains \ze */
        boolean             has_backref;    /* pattern contains \1 .. \9 */
        int                 reghasz;
        Bytes               pattern;
        int                 nsubexp;        /* number of () */

        int                 nstate;         /* states.length */
        nfa_state_C[]       states;
        int                 istate;         /* index in states == number of states allocated */

        /*private*/ nfa_regprog_C()
        {
        }
    }

    /*
     * Structure to be used for single-line matching.
     * Sub-match "no" starts at "startp[no]" and ends just before "endp[no]".
     * When there is no match, the pointer is null.
     */
    /*private*/ static final class regmatch_C
    {
        regprog_C   regprog;
        Bytes[]   startp = new Bytes[NSUBEXP];
        Bytes[]   endp = new Bytes[NSUBEXP];
        boolean     rm_ic;

        /*private*/ regmatch_C()
        {
        }
    }

    /*
     * Structure to be used for multi-line matching.
     * Sub-match "no" starts in line "startpos[no].lnum" column "startpos[no].col"
     * and ends in line "endpos[no].lnum" just before column "endpos[no].col".
     * The line numbers are relative to the first line, thus startpos[0].lnum is always 0.
     * When there is no match, the line number is -1.
     */
    /*private*/ static final class regmmatch_C
    {
        regprog_C           regprog;
        lpos_C[]            startpos;
        lpos_C[]            endpos;
        boolean             rmm_ic;
        int                 rmm_maxcol;     /* when not zero: maximum column */

        /*private*/ regmmatch_C()
        {
            startpos = ARRAY_lpos(NSUBEXP);
            endpos = ARRAY_lpos(NSUBEXP);
        }
    }

    /*private*/ static void COPY_regmmatch(regmmatch_C rmm1, regmmatch_C rmm0)
    {
        rmm1.regprog = rmm0.regprog;
        COPY__lpos(rmm1.startpos, rmm0.startpos, NSUBEXP);
        COPY__lpos(rmm1.endpos, rmm0.endpos, NSUBEXP);
        rmm1.rmm_ic = rmm0.rmm_ic;
        rmm1.rmm_maxcol = rmm0.rmm_maxcol;
    }

    /*
     * Structure used to store external references: "\z\(\)" to "\z\1".
     * Use a reference count to avoid the need to copy this around.
     * When it goes from 1 to zero the matches need to be freed.
     */
    /*private*/ static final class reg_extmatch_C
    {
        short       refcnt;
        Bytes[]   matches = new Bytes[NSUBEXP];

        /*private*/ reg_extmatch_C()
        {
        }
    }

    /*private*/ static abstract class regengine_C
    {
        Bytes expr = u8("");

        protected regengine_C()
        {
        }

        public abstract regprog_C regcomp(Bytes expr, int re_flags);
        public abstract long regexec_nl(regmatch_C rmp, Bytes line, int col, boolean line_lbr);
        public abstract long regexec_multi(regmmatch_C rmp, window_C win, buffer_C buf, long lnum, int col, timeval_C tm);
    }

    /* ----------------------------------------------------------------------- */

    /*
     * marks: positions in a file
     * (a normal mark is a lnum/col pair, the same as a file position)
     */

    /*private*/ static final int NMARKS = ('z' - 'a' + 1);  /* max. # of named marks */
    /*private*/ static final int JUMPLISTSIZE = 100;        /* max. # of marks in jump list */

    /*private*/ static final class fmark_C
    {
        pos_C   mark;   /* cursor position */
        int     fnum;   /* file number */

        /*private*/ fmark_C()
        {
            mark = new pos_C();
        }
    }

    /*private*/ static void COPY_fmark(fmark_C fm1, fmark_C fm0)
    {
        COPY_pos(fm1.mark, fm0.mark);
        fm1.fnum = fm0.fnum;
    }

    /* Xtended file mark: also has a file name. */
    /*private*/ static final class xfmark_C
    {
        fmark_C     fmark;
        Bytes       fname;      /* file name, used when fnum == 0 */

        /*private*/ xfmark_C()
        {
            fmark = new fmark_C();
        }
    }

    /*private*/ static void COPY_xfmark(xfmark_C xfm1, xfmark_C xfm0)
    {
        COPY_fmark(xfm1.fmark, xfm0.fmark);
        xfm1.fname = xfm0.fname;
    }

    /*private*/ static xfmark_C[] ARRAY_xfmark(int n)
    {
        xfmark_C[] a = new xfmark_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new xfmark_C();
        return a;
    }

    /*
     * Structure that contains all options that are local to a window.
     * Used twice in a window: for the current buffer and for all buffers.
     * Also used in wininfo_C.
     */
    /*private*/ static final class winopt_C
    {
        boolean[] wo_bri    = new boolean[1];   /* 'breakindent' */
        Bytes[]   wo_briopt = new Bytes[1];     /* 'breakindentopt' */
        boolean[] wo_lbr    = new boolean[1];   /* 'linebreak' */
        boolean[] wo_list   = new boolean[1];   /* 'list' */
        boolean[] wo_nu     = new boolean[1];   /* 'number' */
        boolean[] wo_rnu    = new boolean[1];   /* 'relativenumber' */
        long[]    wo_nuw    = new long[1];      /* 'numberwidth' */
        boolean[] wo_wfh    = new boolean[1];   /* 'winfixheight' */
        boolean[] wo_wfw    = new boolean[1];   /* 'winfixwidth' */
        boolean[] wo_rl     = new boolean[1];   /* 'rightleft' */
        Bytes[]   wo_rlc    = new Bytes[1];     /* 'rightleftcmd' */
        long[]    wo_scr    = new long[1];      /* 'scroll' */
        boolean[] wo_cuc    = new boolean[1];   /* 'cursorcolumn' */
        boolean[] wo_cul    = new boolean[1];   /* 'cursorline' */
        Bytes[]   wo_cc     = new Bytes[1];     /* 'colorcolumn' */
        Bytes[]   wo_stl    = new Bytes[1];     /* 'statusline' */
        boolean[] wo_scb    = new boolean[1];   /* 'scrollbind' */
        boolean[] wo_wrap   = new boolean[1];   /* 'wrap' */
        Bytes[]   wo_cocu   = new Bytes[1];     /* 'concealcursor' */
        long[]    wo_cole   = new long[1];      /* 'conceallevel' */
        boolean[] wo_crb    = new boolean[1];   /* 'cursorbind' */

        int[]     wo_scriptID;          /* SIDs for window-local options */

        /*private*/ winopt_C()
        {
            wo_scriptID = new int[WV_COUNT];
        }
    }

    /*
     * Window info stored with a buffer.
     *
     * Two types of info are kept for a buffer which are associated with a specific window:
     * 1. Each window can have a different line number associated with a buffer.
     * 2. The window-local options for a buffer work in a similar way.
     * The window-info is kept in a list at b_wininfo.  It is kept in most-recently-used order.
     */
    /*private*/ static final class wininfo_C
    {
        wininfo_C   wi_next;        /* next entry or null for last entry */
        wininfo_C   wi_prev;        /* previous entry or null for first entry */
        window_C    wi_win;         /* pointer to window that did set wi_fpos */
        pos_C       wi_fpos;        /* last cursor position in the file */
        boolean     wi_optset;      /* true when wi_opt has useful values */
        winopt_C    wi_opt;         /* local window options */

        /*private*/ wininfo_C()
        {
            wi_fpos = new pos_C();
            wi_opt = new winopt_C();
        }
    }

    /* Structure to store info about the Visual area. */
    /*private*/ static final class visualinfo_C
    {
        pos_C       vi_start;       /* start pos of last VIsual */
        pos_C       vi_end;         /* end position of last VIsual */
        int         vi_mode;        /* VIsual_mode of last VIsual */
        int         vi_curswant;    /* MAXCOL from w_curswant */

        /*private*/ visualinfo_C()
        {
            vi_start = new pos_C();
            vi_end = new pos_C();
        }
    }

    /*private*/ static void COPY_visualinfo(visualinfo_C vi1, visualinfo_C vi0)
    {
        COPY_pos(vi1.vi_start, vi0.vi_start);
        COPY_pos(vi1.vi_end, vi0.vi_end);
        vi1.vi_mode = vi0.vi_mode;
        vi1.vi_curswant = vi0.vi_curswant;
    }

    /*
     * structures used for undo
     */

    /*private*/ static final class u_entry_C
    {
        u_entry_C   ue_next;        /* pointer to next entry in list */
        long        ue_top;         /* number of line above undo block */
        long        ue_bot;         /* number of line below undo block */
        long        ue_lcount;      /* linecount when u_save() called */
        Bytes[]   ue_array;       /* array of lines in undo block */
        long        ue_size;        /* number of lines in "ue_array" */

        /*private*/ u_entry_C()
        {
        }
    }

    /*private*/ static final class u_link_C
    {
        u_header_C  ptr;
        long        seq;

        /*private*/ u_link_C()
        {
        }
    }

    /*private*/ static final class u_header_C
    {
        /* The following have a pointer and a number.
         * The number is used when reading the undo file in u_read_undo(). */
        u_link_C    uh_next;            /* next undo header in list */
        u_link_C    uh_prev;            /* previous header in list */
        u_link_C    uh_alt_next;        /* next header for alt. redo */
        u_link_C    uh_alt_prev;        /* previous header for alt. redo */
        long        uh_seq;             /* sequence number, higher == newer undo */
        int         uh_walk;            /* used by undo_time() */
        u_entry_C   uh_entry;           /* pointer to first entry */
        u_entry_C   uh_getbot_entry;    /* pointer to where ue_bot must be set */
        pos_C       uh_cursor;          /* cursor position before saving */
        long        uh_cursor_vcol;
        int         uh_flags;           /* see below */
        pos_C[]     uh_namedm;          /* marks before undo/after redo */
        visualinfo_C uh_visual;         /* Visual areas before undo/after redo */
        long        uh_time;            /* timestamp when the change was made */
        long        uh_save_nr;         /* set when the file was saved after changes in this block */

        /*private*/ u_header_C()
        {
            uh_next = new u_link_C();
            uh_prev = new u_link_C();
            uh_alt_next = new u_link_C();
            uh_alt_prev = new u_link_C();
            uh_cursor = new pos_C();
            uh_namedm = ARRAY_pos(NMARKS);
            uh_visual = new visualinfo_C();
        }
    }

    /* values for uh_flags */
    /*private*/ static final int UH_CHANGED  = 0x01;            /* "b_changed" flag before undo/after redo */
    /*private*/ static final int UH_EMPTYBUF = 0x02;            /* buffer was empty */

    /*
     * mf_hashtab_C is a chained hashtable with blocknr_C key and arbitrary
     * structures as items.  This is an intrusive data structure: we require
     * that items begin with mf_hashitem_C which contains the key and linked
     * list pointers.  List of items in each bucket is doubly-linked.
     */

    /*private*/ static final class mf_hashitem_C
    {
        mf_hashitem_C   mhi_next;
        mf_hashitem_C   mhi_prev;
        long            mhi_key;
        Object          mhi_data;

        /*private*/ mf_hashitem_C()
        {
        }
    }

    /*private*/ static mf_hashitem_C[] ARRAY_mf_hashitem(int n)
    {
        mf_hashitem_C[] a = new mf_hashitem_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new mf_hashitem_C();
        return a;
    }

    /*private*/ static final int MHT_INIT_SIZE = 64;

    /*private*/ static final class mf_hashtab_C
    {
        int             mht_mask;       /* mask used for hash value (nr of items in array is "mht_mask" + 1) */
        int             mht_count;      /* number of items inserted into hashtable */
        mf_hashitem_C[] mht_buckets;

        /*private*/ mf_hashtab_C()
        {
        }
    }

    /*private*/ static void ZER0_mf_hashtab(mf_hashtab_C mht)
    {
        mht.mht_mask = 0;
        mht.mht_count = 0;
        mht.mht_buckets = null;
    }

    /*
     * For each (previously) used block in the memfile there is one block header.
     *
     * The block may be linked in the used list OR in the free list.
     * The used blocks are also kept in hash lists.
     *
     * The used list is a doubly linked list, most recently used block first.
     *      The blocks in the used list have a block of memory allocated.
     * The hash lists are used to quickly find a block in the used list.
     * The free list is a single linked list, not sorted.
     *      The blocks in the free list have no block of memory allocated and
     *      the contents of the block in the file (if any) is irrelevant.
     */

    /* 'bh_flags': */
    /*private*/ static final byte
        BH_DIRTY    = 1,
        BH_LOCKED   = 2;

    /*private*/ static final class block_hdr_C
    {
        mf_hashitem_C bh_hashitem;      /* header for hash table and key */

        block_hdr_C bh_next;            /* next block_hdr in free or used list */
        block_hdr_C bh_prev;            /* previous block_hdr in used list */
        Object      bh_data;           /* pointer to memory (for used block) */
        int         bh_page_count;      /* number of pages in this block */

        byte        bh_flags;           /* BH_DIRTY or BH_LOCKED */

        /*private*/ block_hdr_C()
        {
            bh_hashitem = new mf_hashitem_C();
            bh_hashitem.mhi_data = this;
        }

        long bh_bnum()
        {
            return bh_hashitem.mhi_key; /* block number, part of bh_hashitem */
        }

        void bh_bnum(long bnum)
        {
            bh_hashitem.mhi_key = bnum;
        }
    }

    /*
     * When a block with a negative number is flushed to the file, it gets a positive number.
     * Because the reference to the block is still the negative number,
     * we remember the translation to the new positive number in the double linked trans lists.
     * The structure is the same as the hash lists.
     */
    /*private*/ static final class nr_trans_C
    {
        mf_hashitem_C nt_hashitem;              /* header for hash table and key */

        long        nt_new_bnum;                /* new (positive) number */

        /*private*/ nr_trans_C()
        {
            nt_hashitem = new mf_hashitem_C();
            nt_hashitem.mhi_data = this;
        }

        void nt_old_bnum(long bnum)
        {
            nt_hashitem.mhi_key = bnum;     /* old (negative) number */
        }
    }

    /*
     * structure used to store one block of the stuff/redo/recording buffers
     */
    /*private*/ static final class buffblock_C
    {
        buffblock_C bb_next;        /* pointer to next buffblock */
        Bytes       bb_str;         /* contents */

        /*private*/ buffblock_C()
        {
        }
    }

    /*private*/ static void COPY_buffblock(buffblock_C bb1, buffblock_C bb0)
    {
        bb1.bb_next = bb0.bb_next;
        bb1.bb_str = bb0.bb_str;
    }

    /*
     * header used for the stuff buffer and the redo buffer
     */
    /*private*/ static final class buffheader_C
    {
        buffblock_C bh_first;       /* first (dummy) block of list */
        buffblock_C bh_curr;        /* buffblock for appending */
        int         bh_index;       /* index for reading */
        int         bh_space;       /* space in bh_curr for appending */

        /*private*/ buffheader_C()
        {
            bh_first = new buffblock_C();
        }
    }

    /*private*/ static void COPY_buffheader(buffheader_C bh1, buffheader_C bh0)
    {
        COPY_buffblock(bh1.bh_first, bh0.bh_first);
        bh1.bh_curr = bh0.bh_curr;
        bh1.bh_index = bh0.bh_index;
        bh1.bh_space = bh0.bh_space;
    }

    /*
     * used for completion on the command line
     */
    /*private*/ static final class expand_C
    {
        int         xp_context;             /* type of expansion */
        Bytes       xp_pattern;             /* start of item to expand */
        int         xp_pattern_len;         /* bytes in "xp_pattern" before cursor */
        Bytes       xp_arg;                 /* completion function */
        int         xp_scriptID;            /* SID for completion function */
        int         xp_backslash;           /* one of the XP_BS_ values */
        int         xp_numfiles;            /* number of files found by file name completion */
        Bytes[]     xp_files;               /* list of files */
        Bytes       xp_line;                /* text being completed */
        int         xp_col;                 /* cursor position in line */

        /*private*/ expand_C()
        {
        }
    }

    /* values for xp_backslash */
    /*private*/ static final int XP_BS_NONE = 0;    /* nothing special for backslashes */
    /*private*/ static final int XP_BS_ONE  = 1;    /* uses one backslash before a space */

    /*
     * Command modifiers ":vertical", ":browse", ":confirm" and ":hide" set a flag.
     * This needs to be saved for recursive commands, put them in a structure for easy manipulation.
     */
    /*private*/ static final class cmdmod_C
    {
        boolean     hide;                   /* true when ":hide" was used */
        int         split;                  /* flags for win_split() */
        int         tab;                    /* > 0 when ":tab" was used */
        boolean     confirm;                /* true to invoke yes/no dialog */
        boolean     keepalt;                /* true when ":keepalt" was used */
        boolean     keepmarks;              /* true when ":keepmarks" was used */
        boolean     keepjumps;              /* true when ":keepjumps" was used */
        boolean     lockmarks;              /* true when ":lockmarks" was used */
        boolean     keeppatterns;           /* true when ":keeppatterns" was used */
        Bytes       save_ei;                /* saved value of 'eventignore' */

        /*private*/ cmdmod_C()
        {
        }
    }

    /*private*/ static void ZER0_cmdmod(cmdmod_C cm)
    {
        cm.hide = false;
        cm.split = 0;
        cm.tab = 0;
        cm.confirm = false;
        cm.keepalt = false;
        cm.keepmarks = false;
        cm.keepjumps = false;
        cm.lockmarks = false;
        cm.keeppatterns = false;
        cm.save_ei = null;
    }

    /*private*/ static void COPY_cmdmod(cmdmod_C cm1, cmdmod_C cm0)
    {
        cm1.hide = cm0.hide;
        cm1.split = cm0.split;
        cm1.tab = cm0.tab;
        cm1.confirm = cm0.confirm;
        cm1.keepalt = cm0.keepalt;
        cm1.keepmarks = cm0.keepmarks;
        cm1.keepjumps = cm0.keepjumps;
        cm1.lockmarks = cm0.lockmarks;
        cm1.keeppatterns = cm0.keeppatterns;
        cm1.save_ei = cm0.save_ei;
    }

    /*private*/ static final class memfile_C
    {
        block_hdr_C     mf_used_first;          /* mru block_hdr in used list */
        block_hdr_C     mf_used_last;           /* lru block_hdr in used list */
        mf_hashtab_C    mf_hash;                /* hash lists */
        mf_hashtab_C    mf_trans;               /* trans lists */
        long            mf_blocknr_max;         /* highest positive block number + 1 */
        long            mf_blocknr_min;         /* lowest negative block number - 1 */
        long            mf_neg_count;           /* number of negative blocks numbers */

        /*private*/ memfile_C()
        {
            mf_hash = new mf_hashtab_C();
            mf_trans = new mf_hashtab_C();
        }
    }

    /*
     * things used in memline.c
     */
    /*
     * When searching for a specific line, we remember what blocks in the tree
     * are the branches leading to that block.  This is stored in ml_stack.  Each
     * entry is a pointer to info in a block (may be data block or pointer block)
     */
    /*private*/ static final class infoptr_C
    {
        long        ip_bnum;                /* block number */
        long        ip_low;                 /* lowest lnum in this block */
        long        ip_high;                /* highest lnum in this block */
        int         ip_index;               /* index for block with current lnum */

        /*private*/ infoptr_C()
        {
        }
    }

    /*private*/ static void COPY_infoptr(infoptr_C ip1, infoptr_C ip0)
    {
        ip1.ip_bnum = ip0.ip_bnum;
        ip1.ip_low = ip0.ip_low;
        ip1.ip_high = ip0.ip_high;
        ip1.ip_index = ip0.ip_index;
    }

    /*private*/ static infoptr_C[] ARRAY_infoptr(int n)
    {
        infoptr_C[] a = new infoptr_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new infoptr_C();
        return a;
    }

    /*private*/ static final class chunksize_C
    {
        int         mlcs_numlines;
        long        mlcs_totalsize;

        /*private*/ chunksize_C()
        {
        }
    }

    /*private*/ static void COPY_chunksize(chunksize_C cs1, chunksize_C cs0)
    {
        cs1.mlcs_numlines = cs0.mlcs_numlines;
        cs1.mlcs_totalsize = cs0.mlcs_totalsize;
    }

    /*private*/ static chunksize_C[] ARRAY_chunksize(int n)
    {
        chunksize_C[] a = new chunksize_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new chunksize_C();
        return a;
    }

    /* Flags when calling ml_updatechunk(). */
    /*private*/ static final int ML_CHNK_ADDLINE = 1;
    /*private*/ static final int ML_CHNK_DELLINE = 2;
    /*private*/ static final int ML_CHNK_UPDLINE = 3;

    /* 'ml_flags': */
    /*private*/ static final int ML_EMPTY        = 1;   /* empty buffer */
    /*private*/ static final int ML_LINE_DIRTY   = 2;   /* cached line was changed and allocated */
    /*private*/ static final int ML_LOCKED_DIRTY = 4;   /* ml_locked was changed */
    /*private*/ static final int ML_LOCKED_POS   = 8;   /* ml_locked needs positive block number */

    /*
     * The memline structure holds all the information about a memline.
     */
    /*private*/ static final class memline_C
    {
        long        ml_line_count;      /* number of lines in the buffer */

        memfile_C   ml_mfp;             /* pointer to associated memfile */

        int         ml_flags;

        infoptr_C[] ml_stack;           /* stack of pointer blocks (array of IPTRs) */
        int         ml_stack_top;       /* current top in ml_stack */
        int         ml_stack_size;      /* total number of entries in ml_stack */

        long        ml_line_lnum;       /* line number of cached line, 0 if not valid */
        Bytes       ml_line_ptr;        /* pointer to cached line */

        block_hdr_C ml_locked;          /* block used by last ml_get() */
        long        ml_locked_low;      /* first line in ml_locked */
        long        ml_locked_high;     /* last line in ml_locked */
        int         ml_locked_lineadd;  /* number of lines inserted in ml_locked */

        chunksize_C[] ml_chunksize;
        int         ml_numchunks;
        int         ml_usedchunks;

        /*private*/ memline_C()
        {
        }
    }

    /*
     * For each argument remember the file name as it was given, and the buffer
     * number that contains the expanded file name (required for when ":cd" is used.
     */
    /*private*/ static final class aentry_C
    {
        Bytes       ae_fname;           /* file name as specified */
        int         ae_fnum;            /* buffer number with expanded file name */

        /*private*/ aentry_C()
        {
        }
    }

    /*
     * Argument list: Array of file names.
     * Used for the global argument list and the argument lists local to a window.
     */
    /*private*/ static final class alist_C
    {
        Growing<aentry_C>    al_ga;              /* growarray with the array of file names */
        int         al_refcount;        /* number of windows using this arglist */
        int         id;                 /* id of this arglist */

        /*private*/ alist_C()
        {
            al_ga = new Growing<aentry_C>(aentry_C.class, 5);
        }
    }

    /*
     * A list used for saving values of "emsg_silent".
     * Used by ex_try() to save the value of "emsg_silent" if it was non-zero.
     * When this is done, the CSF_SILENT flag below is set.
     */
    /*private*/ static final class eslist_C
    {
        int         saved_emsg_silent;      /* saved value of "emsg_silent" */
        eslist_C    next;                   /* next element on the list */

        /*private*/ eslist_C()
        {
        }
    }

    /*
     * For conditional commands a stack is kept of nested conditionals.
     * When cs_idx < 0, there is no conditional command.
     */
    /*private*/ static final int CSTACK_LEN      = 50;

    /*private*/ static final class condstack_C
    {
        short[]     cs_flags;               /* CSF_ flags */
        byte[]      cs_pending;             /* CSTP_: what's pending in ":finally" */
                                            /* return typeval for pending return */
        Object[]    cs_rv_ex;               /*          -- union -- */
                                            /* exception for pending throw */
        forinfo_C[] cs_forinfo;             /* info used by ":for" */
        int[]       cs_line;                /* line nr of ":while"/":for" line */
        int         cs_idx;                 /* current entry, or -1 if none */
        int         cs_looplevel;           /* nr of nested ":while"s and ":for"s */
        int         cs_trylevel;            /* nr of nested ":try"s */
        eslist_C    cs_emsg_silent_list;    /* saved values of "emsg_silent" */
        byte        cs_lflags;              /* loop flags: CSL_ flags */

        /*private*/ condstack_C()
        {
            cs_flags = new short[CSTACK_LEN];
            cs_pending = new byte[CSTACK_LEN];
            cs_rv_ex = new Object[CSTACK_LEN];
            cs_forinfo = new forinfo_C[CSTACK_LEN];
            cs_line = new int[CSTACK_LEN];
        }
    }

    /* There is no CSF_IF, the lack of CSF_WHILE, CSF_FOR and CSF_TRY means ":if" was used. */
    /*private*/ static final int
        CSF_TRUE    = 0x0001,   /* condition was true */
        CSF_ACTIVE  = 0x0002,   /* current state is active */
        CSF_ELSE    = 0x0004,   /* ":else" has been passed */
        CSF_WHILE   = 0x0008,   /* is a ":while" */
        CSF_FOR     = 0x0010,   /* is a ":for" */

        CSF_TRY     = 0x0100,   /* is a ":try" */
        CSF_FINALLY = 0x0200,   /* ":finally" has been passed */
        CSF_THROWN  = 0x0400,   /* exception thrown to this try conditional */
        CSF_CAUGHT  = 0x0800,   /* exception caught by this try conditional */
        CSF_SILENT  = 0x1000;   /* "emsg_silent" reset by ":try" */
    /* Note that CSF_ELSE is only used when CSF_TRY and CSF_WHILE are unset
     * (an ":if"), and CSF_SILENT is only used when CSF_TRY is set. */

    /*
     * What's pending for being reactivated at the ":endtry" of this try conditional:
     */
    /*private*/ static final byte
        CSTP_NONE      =  0,    /* nothing pending in ":finally" clause */
        CSTP_ERROR     =  1,    /* an error is pending */
        CSTP_INTERRUPT =  2,    /* an interrupt is pending */
        CSTP_THROW     =  4,    /* a throw is pending */
        CSTP_BREAK     =  8,    /* ":break" is pending */
        CSTP_CONTINUE  = 16,    /* ":continue" is pending */
        CSTP_RETURN    = 24,    /* ":return" is pending */
        CSTP_FINISH    = 32;    /* ":finish" is pending */

    /*
     * Flags for the cs_lflags item in condstack_C.
     */
    /*private*/ static final byte
        CSL_HAD_LOOP    = 1,    /* just found ":while" or ":for" */
        CSL_HAD_ENDLOOP = 2,    /* just found ":endwhile" or ":endfor" */
        CSL_HAD_CONT    = 4,    /* just found ":continue" */
        CSL_HAD_FINA    = 8;    /* just found ":finally" */

    /*
     * A list of error messages that can be converted to an exception.  "throw_msg"
     * is only set in the first element of the list.  Usually, it points to the
     * original message stored in that element, but sometimes it points to a later
     * message in the list.  See cause_errthrow() below.
     */
    /*private*/ static final class msglist_C
    {
        Bytes       msg;        /* original message */
        Bytes       throw_msg;  /* "msg" to throw: usually original one */
        msglist_C   next;       /* next of several messages in a row */

        /*private*/ msglist_C()
        {
        }
    }

    /*
     * Structure describing an exception.
     * (don't use "struct exception", it's used by the math library).
     */
    /*private*/ static final class except_C
    {
        int         type;       /* exception type */
        Bytes       value;      /* exception value */
        msglist_C   messages;   /* message(s) causing error exception */
        Bytes       throw_name; /* name of the throw point */
        long        throw_lnum; /* line number of the throw point */
        except_C    caught;     /* next exception on the caught stack */

        /*private*/ except_C()
        {
        }
    }

    /*
     * The exception types.
     */
    /*private*/ static final int
        ET_USER      = 0,       /* exception caused by ":throw" command */
        ET_ERROR     = 1,       /* error exception */
        ET_INTERRUPT = 2;       /* interrupt exception triggered by Ctrl-C */

    /*
     * Structure to save the error/interrupt/exception state between calls to
     * enter_cleanup() and leave_cleanup().  Must be allocated as an automatic
     * variable by the (common) caller of these functions.
     */
    /*private*/ static final class cleanup_C
    {
        int pending;            /* error/interrupt/exception state */
        except_C exception;     /* exception value */

        /*private*/ cleanup_C()
        {
        }
    }

    /* struct passed to in_id_list() */
    /*private*/ static final class sp_syn_C
    {
        int         inc_tag;        /* ":syn include" unique tag */
        short       id;             /* highlight group ID of item */
        short[]     cont_in_list;   /* cont.in group IDs, if non-zero */

        /*private*/ sp_syn_C()
        {
        }
    }

    /*private*/ static void COPY_sp_syn(sp_syn_C ss1, sp_syn_C ss0)
    {
        ss1.inc_tag = ss0.inc_tag;
        ss1.id = ss0.id;
        ss1.cont_in_list = ss0.cont_in_list;
    }

    /*
     * Each keyword has one keyentry, which is linked in a hash list.
     */
    /*private*/ static final class keyentry_C
    {
        keyentry_C  ke_next;        /* next entry with identical "keyword[]" */
        sp_syn_C    ke_syn;         /* struct passed to in_id_list() */
        short[]     ke_next_list;   /* ID list for next match (if non-zero) */
        int         ke_flags;
        int         ke_char;        /* conceal substitute character */
        Bytes       ke_keyword;     /* actually longer */

        /*private*/ keyentry_C()
        {
            ke_syn = new sp_syn_C();
        }
    }

    /*
     * Struct used to store one state of the state stack.
     */
    /*private*/ static final class bufstate_C
    {
        int             bs_idx;         /* index of pattern */
        int             bs_flags;       /* flags for pattern */
        int             bs_seqnr;       /* stores si_seqnr */
        int             bs_cchar;       /* stores si_cchar */
        reg_extmatch_C  bs_extmatch;    /* external matches from start pattern */

        /*private*/ bufstate_C()
        {
        }
    }

    /*
     * syn_state contains the syntax state stack for the start of one line.
     * Used by b_sst_array[].
     */
    /*private*/ static final class synstate_C
    {
        synstate_C  sst_next;           /* next entry in used or free list */
        long        sst_lnum;           /* line number for this state */
        Growing<bufstate_C> sst_ga;     /* growarray for state stack */
        int         sst_next_flags;     /* flags for sst_next_list */
        int         sst_stacksize;      /* number of states on the stack */
        short[]     sst_next_list;      /* "nextgroup" list in this state (this is a copy, don't free it!) */
        short       sst_tick;           /* tick when last displayed */
        long        sst_change_lnum;    /* when non-zero, change in this line may have made the state invalid */

        /*private*/ synstate_C()
        {
            sst_ga = new Growing<bufstate_C>(bufstate_C.class, 1);
        }
    }

    /*private*/ static void COPY_synstate(synstate_C sst1, synstate_C sst0)
    {
        sst1.sst_next = sst0.sst_next;
        sst1.sst_lnum = sst0.sst_lnum;
        COPY_garray(sst1.sst_ga, sst0.sst_ga);
        sst1.sst_next_flags = sst0.sst_next_flags;
        sst1.sst_stacksize = sst0.sst_stacksize;
        sst1.sst_next_list = sst0.sst_next_list;
        sst1.sst_tick = sst0.sst_tick;
        sst1.sst_change_lnum = sst0.sst_change_lnum;
    }

    /*private*/ static synstate_C[] ARRAY_synstate(int n)
    {
        synstate_C[] a = new synstate_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new synstate_C();
        return a;
    }

    /*
     * Structure shared between syntax.c and screen.c.
     */
    /*private*/ static final class attrentry_C
    {
        int         ae_attr;            /* HL_BOLD, etc. */
        Bytes       ae_esc_start;       /* start escape sequence */
        Bytes       ae_esc_stop;        /* stop escape sequence */
        /* These colors need to be > 8 bits to hold 256. */
        int         ae_fg_color;        /* foreground color number */
        int         ae_bg_color;        /* background color number */

        /*private*/ attrentry_C()
        {
        }
    }

    /*private*/ static void ZER0_attrentry(attrentry_C ae)
    {
        ae.ae_attr = 0;
        ae.ae_esc_start = null;
        ae.ae_esc_stop = null;
        ae.ae_fg_color = 0;
        ae.ae_bg_color = 0;
    }

    /*private*/ static void COPY_attrentry(attrentry_C ae1, attrentry_C ae0)
    {
        ae1.ae_attr = ae0.ae_attr;
        ae1.ae_esc_start = ae0.ae_esc_start;
        ae1.ae_esc_stop = ae0.ae_esc_stop;
        ae1.ae_fg_color = ae0.ae_fg_color;
        ae1.ae_bg_color = ae0.ae_bg_color;
    }

    /*
     * Used for the typeahead buffer: typebuf.
     */
    /*private*/ static final class typebuf_C
    {
        Bytes       tb_buf;             /* buffer for typed characters */
        Bytes       tb_noremap;         /* mapping flags for characters in "tb_buf" */
        int         tb_buflen;          /* size of "tb_buf" */
        int         tb_off;             /* current position in "tb_buf" */
        int         tb_len;             /* number of valid bytes in "tb_buf" */
        int         tb_maplen;          /* nr of mapped bytes in "tb_buf" */
        int         tb_silent;          /* nr of silently mapped bytes in "tb_buf" */
        int         tb_no_abbr_cnt;     /* nr of bytes without abbrev. in "tb_buf" */
        int         tb_change_cnt;      /* nr of time "tb_buf" was changed; never zero */

        /*private*/ typebuf_C()
        {
        }
    }

    /*private*/ static void COPY_typebuf(typebuf_C tb1, typebuf_C tb0)
    {
        tb1.tb_buf = tb0.tb_buf;
        tb1.tb_noremap = tb0.tb_noremap;
        tb1.tb_buflen = tb0.tb_buflen;
        tb1.tb_off = tb0.tb_off;
        tb1.tb_len = tb0.tb_len;
        tb1.tb_maplen = tb0.tb_maplen;
        tb1.tb_silent = tb0.tb_silent;
        tb1.tb_no_abbr_cnt = tb0.tb_no_abbr_cnt;
        tb1.tb_change_cnt = tb0.tb_change_cnt;
    }

    /*private*/ static typebuf_C[] ARRAY_typebuf(int n)
    {
        typebuf_C[] a = new typebuf_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new typebuf_C();
        return a;
    }

    /* Struct to hold the saved typeahead for save_typeahead(). */
    /*private*/ static final class tasave_C
    {
        int             old_char;
        int             old_mod_mask;
        buffheader_C    save_readbuf1;
        buffheader_C    save_readbuf2;
        Bytes           save_inputbuf;
        int             save_inputlen;

        /*private*/ tasave_C()
        {
        }
    }

    /*private*/ static tasave_C new_tasave()
    {
        tasave_C tasp = new tasave_C();
        tasp.save_readbuf1 = new buffheader_C();
        tasp.save_readbuf2 = new buffheader_C();
        return tasp;
    }

    /*
     * Structure used for mappings and abbreviations.
     */
    /*private*/ static final class mapblock_C
    {
        mapblock_C  m_next;         /* next mapblock in list */
        Bytes       m_keys;         /* mapped from, lhs */
        Bytes       m_str;          /* mapped to, rhs */
        Bytes       m_orig_str;     /* rhs as entered by the user */
        int         m_keylen;       /* strlen(m_keys) */
        int         m_mode;         /* valid mode */
        int         m_noremap;      /* if non-zero no re-mapping for "m_str" */
        boolean     m_silent;       /* <silent> used, don't echo commands */
        boolean     m_nowait;       /* <nowait> used */
        boolean     m_expr;         /* <expr> used, "m_str" is an expression */
        int         m_script_ID;    /* ID of script where map was defined */

        /*private*/ mapblock_C()
        {
        }
    }

    /*
     * Used for highlighting in the status line.
     */
    /*private*/ static final class stl_hlrec_C
    {
        Bytes       start;
        int         userhl;         /* 0: no HL, 1-9: User HL, < 0 for syn ID */

        /*private*/ stl_hlrec_C()
        {
        }
    }

    /*private*/ static stl_hlrec_C[] ARRAY_stl_hlrec(int n)
    {
        stl_hlrec_C[] a = new stl_hlrec_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new stl_hlrec_C();
        return a;
    }

    /*
     * Item for a hashtable.  "hi_key" can be one of three values:
     * null:           Never been used.
     * HASH_REMOVED:   Entry was removed.
     * Otherwise:      Used item, pointer to the actual key; this usually is
     *                 inside the item, subtract an offset to locate the item.
     *                 This reduces the size of hashitem by 1/3.
     */
    /*private*/ static final class hashitem_C
    {
        long        hi_hash;        /* cached hash number of hi_key */
        Object      hi_data;
        Bytes       hi_key;

        /*private*/ hashitem_C()
        {
        }
    }

    /*private*/ static void COPY_hashitem(hashitem_C hi1, hashitem_C hi0)
    {
        hi1.hi_hash = hi0.hi_hash;
        hi1.hi_data = hi0.hi_data;
        hi1.hi_key = hi0.hi_key;
    }

    /*private*/ static hashitem_C[] ARRAY_hashitem(int n)
    {
        hashitem_C[] a = new hashitem_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new hashitem_C();
        return a;
    }

    /* Initial size for a hashtable.  Our items are relatively small and growing
     * is expensive, thus use 16 as a start.  Must be a power of 2. */
    /*private*/ static final int HT_INIT_SIZE = 16;

    /*private*/ static final class hashtab_C
    {
        long        ht_mask;        /* mask used for hash value (nr of items in array is "ht_mask" + 1) */
        long        ht_used;        /* number of items used */
        long        ht_filled;      /* number of items used + removed */
        int         ht_locked;      /* counter for hash_lock() */
        hashitem_C[] ht_buckets;

        /*private*/ hashtab_C()
        {
        }
    }

    /*private*/ static void ZER0_hashtab(hashtab_C ht)
    {
        ht.ht_mask = 0;
        ht.ht_used = 0;
        ht.ht_filled = 0;
        ht.ht_locked = 0;
        ht.ht_buckets = null;
    }

    /*
     * Structure to hold an internal variable without a name.
     */
    /*private*/ static final class typval_C
    {
        byte        tv_type;        /* see below: VAR_NUMBER, VAR_STRING, etc. */
        byte        tv_lock;        /* see below: VAR_LOCKED, VAR_FIXED */

        long        tv_number;      /* number value */
        Bytes       tv_string;      /* string value (can be null!) */
        list_C      tv_list;        /* list value (can be null!) */
        dict_C      tv_dict;        /* dict value (can be null!) */

        /*private*/ typval_C()
        {
        }
    }

    /*private*/ static void ZER0_typval(typval_C tv)
    {
        tv.tv_type = 0;
        tv.tv_lock = 0;
        tv.tv_number = 0;
        tv.tv_string = null;
        tv.tv_list = null;
        tv.tv_dict = null;
    }

    /*private*/ static void COPY_typval(typval_C tv1, typval_C tv0)
    {
        tv1.tv_type = tv0.tv_type;
        tv1.tv_lock = tv0.tv_lock;
        tv1.tv_number = tv0.tv_number;
        tv1.tv_string = tv0.tv_string;
        tv1.tv_list = tv0.tv_list;
        tv1.tv_dict = tv0.tv_dict;
    }

    /*private*/ static typval_C[] ARRAY_typval(int n)
    {
        typval_C[] a = new typval_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new typval_C();
        return a;
    }

    /* Values for "tv_type". */
    /*private*/ static final byte
        VAR_UNKNOWN = 0,
        VAR_NUMBER  = 1,        /* "tv_number" is used */
        VAR_STRING  = 2,        /* "tv_string" is used */
        VAR_FUNC    = 3,        /* "tv_string" is function name */
        VAR_LIST    = 4,        /* "tv_list" is used */
        VAR_DICT    = 5;        /* "tv_dict" is used */

    /* Values for "dv_scope". */
    /*private*/ static final byte
        VAR_SCOPE     = 1,      /* a:, v:, s:, etc. scope dictionaries */
        VAR_DEF_SCOPE = 2;      /* l:, g: scope dictionaries: here funcrefs are
                                 * not allowed to mask existing functions */

    /* Values for "tv_lock". */
    /*private*/ static final byte
        VAR_LOCKED  = 1,        /* locked with lock(), can use unlock() */
        VAR_FIXED   = 2;        /* locked forever */

    /*
     * Structure to hold an item of a list: an internal variable without a name.
     */
    /*private*/ static final class listitem_C
    {
        listitem_C  li_next;    /* next item in list */
        listitem_C  li_prev;    /* previous item in list */
        typval_C    li_tv;      /* type and value of the variable */

        /*private*/ listitem_C()
        {
            li_tv = new typval_C();
        }
    }

    /*private*/ static listitem_C[] ARRAY_listitem(int n)
    {
        listitem_C[] a = new listitem_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new listitem_C();
        return a;
    }

    /*
     * Struct used by those that are using an item in a list.
     */
    /*private*/ static final class listwatch_C
    {
        listitem_C  lw_item;    /* item being watched */
        listwatch_C lw_next;    /* next watcher */

        /*private*/ listwatch_C()
        {
        }
    }

    /*
     * Structure to hold info about a list.
     */
    /*private*/ static final class list_C
    {
        listitem_C  lv_first;       /* first item, null if none */
        listitem_C  lv_last;        /* last item, null if none */
        int         lv_refcount;    /* reference count */
        int         lv_len;         /* number of items */
        listwatch_C lv_watch;       /* first watcher, null if none */
        int         lv_idx;         /* cached index of an item */
        listitem_C  lv_idx_item;    /* when not null item at index "lv_idx" */
        int         lv_copyID;      /* ID used by deepcopy() */
        list_C      lv_copylist;    /* copied list used by deepcopy() */
        byte        lv_lock;        /* zero, VAR_LOCKED, VAR_FIXED */

        /*private*/ list_C()
        {
        }
    }

    /*private*/ static void ZER0_list(list_C lv)
    {
        lv.lv_first = null;
        lv.lv_last = null;
        lv.lv_refcount = 0;
        lv.lv_len = 0;
        lv.lv_watch = null;
        lv.lv_idx = 0;
        lv.lv_idx_item = null;
        lv.lv_copyID = 0;
        lv.lv_copylist = null;
        lv.lv_lock = 0;
    }

    /*
     * Structure to hold an item of a Dictionary.
     * Also used for a variable.
     */
    /*private*/ static final class dictitem_C
    {
        typval_C    di_tv;          /* type and value of the variable */
        byte        di_flags;       /* flags (only used for variable) */
        Bytes       di_key;         /* key (== name for variable) */

        /*private*/ dictitem_C()
        {
            di_tv = new typval_C();
        }
    }

    /*private*/ static final byte
        DI_FLAGS_RO     = 1,        /* "di_flags" value: read-only variable */
        DI_FLAGS_RO_SBX = 2,        /* "di_flags" value: read-only in the sandbox */
        DI_FLAGS_FIX    = 4,        /* "di_flags" value: fixed variable, not allocated */
        DI_FLAGS_LOCK   = 8;        /* "di_flags" value: locked variable */

    /*
     * Structure to hold info about a Dictionary.
     */
    /*private*/ static final class dict_C
    {
        byte        dv_lock;        /* zero, VAR_LOCKED, VAR_FIXED */
        byte        dv_scope;       /* zero, VAR_SCOPE, VAR_DEF_SCOPE */
        int         dv_refcount;    /* reference count */
        int         dv_copyID;      /* ID used by deepcopy() */
        hashtab_C   dv_hashtab;     /* hashtab that refers to the items */
        dict_C      dv_copydict;    /* copied dict used by deepcopy() */

        /*private*/ dict_C()
        {
            dv_hashtab = new hashtab_C();
        }
    }

    /* structure used for explicit stack while garbage collecting hash tables */
    /*private*/ static final class ht_stack_C
    {
        hashtab_C       ht;
        ht_stack_C      prev;

        /*private*/ ht_stack_C()
        {
        }
    }

    /* structure used for explicit stack while garbage collecting lists */
    /*private*/ static final class list_stack_C
    {
        list_C          list;
        list_stack_C    prev;

        /*private*/ list_stack_C()
        {
        }
    }

    /*
     * These are items normally related to a buffer.
     * But when using ":ownsyntax" a window may have its own instance.
     */
    /*private*/ static final class synblock_C
    {
        hashtab_C   b_keywtab;                  /* syntax keywords hash table */
        hashtab_C   b_keywtab_ic;               /* idem, ignore case */
        boolean     b_syn_error;                /* true when error occurred in HL */
        boolean     b_syn_ic;                   /* ignore case for :syn cmds */
        Growing<synpat_C> b_syn_patterns;       /* table for syntax patterns */
        Growing<syn_cluster_C> b_syn_clusters;  /* table for syntax clusters */
        int         b_spell_cluster_id;         /* @Spell cluster ID or 0 */
        int         b_nospell_cluster_id;       /* @NoSpell cluster ID or 0 */
        boolean     b_syn_containedin;          /* true when there is an item with a
                                                 * "containedin" argument */
        int         b_syn_sync_flags;           /* flags about how to sync */
        short       b_syn_sync_id;              /* group to sync on */
        long        b_syn_sync_minlines;        /* minimal sync lines offset */
        long        b_syn_sync_maxlines;        /* maximal sync lines offset */
        long        b_syn_sync_linebreaks;      /* offset for multi-line pattern */
        Bytes       b_syn_linecont_pat;         /* line continuation pattern */
        regprog_C   b_syn_linecont_prog;        /* line continuation program */
        boolean     b_syn_linecont_ic;          /* ignore-case flag for above */
        int         b_syn_topgrp;               /* for ":syntax include" */
        boolean     b_syn_conceal;              /* auto-conceal for :syn cmds */
        /*
         * b_sst_array[]        contains the state stack for a number of lines,
         *                      for the start of that line (col == 0).
         *                      This avoids having to recompute the syntax state too often.
         * b_sst_array[]        is allocated to hold the state for all displayed lines,
         *                      and states for 1 out of about 20 other lines.
         * b_sst_array          pointer to an array of synstate_C
         * b_sst_len            number of entries in b_sst_array[]
         * b_sst_first          pointer to first used entry in b_sst_array[] or null
         * b_sst_firstfree      pointer to first free entry in b_sst_array[] or null
         * b_sst_freecount      number of free entries in b_sst_array[]
         * b_sst_check_lnum     entries after this lnum need to be checked for validity
         *                      (MAXLNUM means no check needed)
         */
        synstate_C[] b_sst_array;
        int         b_sst_len;
        synstate_C  b_sst_first;
        synstate_C  b_sst_firstfree;
        int         b_sst_freecount;
        long        b_sst_check_lnum;
        short       b_sst_lasttick;         /* last display tick */

        /*private*/ synblock_C()
        {
            b_keywtab = new hashtab_C();
            b_keywtab_ic = new hashtab_C();
            b_syn_patterns = new Growing<synpat_C>(synpat_C.class, 10);
            b_syn_clusters = new Growing<syn_cluster_C>(syn_cluster_C.class, 10);
        }
    }

    /*
     * buffer: structure that holds information about one file
     *
     * Several windows can share a single Buffer
     * A buffer is unallocated if there is no memfile for it.
     * A buffer is new if the associated file has never been loaded yet.
     */

    /*private*/ static final int
        B_IMODE_USE_INSERT = -1,        /* use "b_p_iminsert" value for search */
        B_IMODE_NONE       =  0,        /* input via none */
        B_IMODE_LMAP       =  1,        /* input via langmap */
        B_IMODE_LAST       =  1;

    /*private*/ static final class buffer_C
    {
        memline_C   b_ml;               /* associated memline (also contains line count) */

        buffer_C    b_next;             /* links in list of buffers */
        buffer_C    b_prev;

        int         b_nwindows;         /* nr of windows open on this buffer */

        int         b_flags;            /* various BF_ flags */
        boolean     b_closing;          /* buffer is being closed, don't let autocommands close it too */

        /*
         * b_ffname has the full path of the file (null for no name).
         * b_sfname is the name as the user typed it (or null).
         * b_fname is the same as b_sfname, unless ":cd" has been done,
         *      then it is the same as b_ffname (null for no name).
         */
        Bytes       b_ffname;           /* full path file name */
        Bytes       b_sfname;           /* short file name */
        Bytes       b_fname;            /* current file name */

        boolean     b_dev_valid;        /* true when b_dev has a valid number */
        long        b_dev;              /* device number */
        long        b_ino;              /* inode number */

        int         b_fnum;             /* buffer number for this file. */

        boolean[]   b_changed = new boolean[1]; /* 'modified': Set to true if something in the
                                         * file has been changed and not written out. */
        int         b_changedtick;      /* incremented for each change, also for undo */

        boolean     b_saving;           /* Set to true if we are in the middle of saving the buffer. */

        /*
         * Changes to a buffer require updating of the display.
         * To minimize the work, remember changes made and update everything at once.
         */
        boolean     b_mod_set;          /* true when there are changes since the
                                         * last time the display was updated */
        long        b_mod_top;          /* topmost lnum that was changed */
        long        b_mod_bot;          /* lnum below last changed line, AFTER the change */
        long        b_mod_xlines;       /* number of extra buffer lines inserted;
                                         * negative when lines were deleted */

        wininfo_C   b_wininfo;          /* list of last used info for each window */

        long        b_mtime;            /* last change time of original file */
        long        b_mtime_read;       /* last change time when reading */
        long        b_orig_size;        /* size of original file in bytes */
        int         b_orig_mode;        /* mode of original file */

        pos_C[]     b_namedm;           /* current named marks (mark.c) */

        /* These variables are set when VIsual_active becomes false. */
        visualinfo_C b_visual;
        int         b_visual_mode_eval; /* b_visual.vi_mode for visualmode() */

        pos_C       b_last_cursor;      /* cursor position when last unloading this buffer */
        pos_C       b_last_insert;      /* where Insert mode was left */
        pos_C       b_last_change;      /* position of last change: '. mark */

        /*
         * the changelist contains old change positions
         */
        pos_C[]     b_changelist;
        int         b_changelistlen;    /* number of active entries */
        boolean     b_new_change;       /* set by u_savecommon() */

        /*
         * Character table, only used in charset.c for 'iskeyword'.
         * 8 bytes of 32 bits: 1 bit per character 0-255.
         */
        int[]       b_chartab;

        /* Table used for mappings local to a buffer. */
        mapblock_C[][] b_maphash;

        /* First abbreviation local to a buffer. */
        mapblock_C[] b_first_abbr = new mapblock_C[1];
        /* User commands local to the buffer. */
        Growing<ucmd_C>    b_ucmds;
        /*
         * start and end of an operator, also used for '[ and ']
         */
        pos_C       b_op_start;
        pos_C       b_op_start_orig;    /* used for insStart_orig */
        pos_C       b_op_end;

        /*
         * The following only used in undo.c.
         */
        u_header_C  b_u_oldhead;        /* pointer to oldest header */
        u_header_C  b_u_newhead;        /* pointer to newest header; may not be valid
                                         * if b_u_curhead is not null */
        u_header_C  b_u_curhead;        /* pointer to current header */
        int         b_u_numhead;        /* current number of headers */
        boolean     b_u_synced;         /* entry lists are synced */
        long        b_u_seq_last;       /* last used undo sequence number */
        long        b_u_save_nr_last;   /* counter for last file write */
        long        b_u_seq_cur;        /* hu_seq of header below which we are now */
        long        b_u_time_cur;       /* uh_time of header below which we are now */
        long        b_u_save_nr_cur;    /* file write nr after which we are now */

        /*
         * variables for "U" command in undo.c
         */
        Bytes       b_u_line_ptr;       /* saved line for "U" command */
        long        b_u_line_lnum;      /* line number of line in u_line */
        int         b_u_line_colnr;     /* optional column number */

        /* flags for use of ":lmap" and IM control */
        long[]      b_p_iminsert = new long[1]; /* input mode for insert */
        long[]      b_p_imsearch = new long[1]; /* input mode for search */

        /*
         * Options local to a buffer.
         * They are here because their value depends on the type of file
         * or contents of the file being edited.
         */
        boolean     b_p_initialized;        /* set when options initialized */

        int[]       b_p_scriptID;           /* SIDs for buffer-local options */

        boolean[]   b_p_ai   = new boolean[1];  /* 'autoindent' */
        boolean     b_p_ai_nopaste;             /* "b_p_ai" saved for paste mode */
        boolean[]   b_p_ci   = new boolean[1];  /* 'copyindent' */
        boolean[]   b_p_bin  = new boolean[1];  /* 'binary' */
        boolean[]   b_p_bomb = new boolean[1];  /* 'bomb' */
        boolean[]   b_p_bl   = new boolean[1];  /* 'buflisted' */
        boolean[]   b_p_cin  = new boolean[1];  /* 'cindent' */
        Bytes[]     b_p_cino = new Bytes[1];    /* 'cinoptions' */
        Bytes[]     b_p_cink = new Bytes[1];    /* 'cinkeys' */
        Bytes[]     b_p_cinw = new Bytes[1];    /* 'cinwords' */
        Bytes[]     b_p_com  = new Bytes[1];    /* 'comments' */
        boolean[]   b_p_eol  = new boolean[1];  /* 'endofline' */
        boolean[]   b_p_et   = new boolean[1];  /* 'expandtab' */
        boolean     b_p_et_nobin;               /* "b_p_et" saved for binary mode */
        Bytes[]     b_p_fenc = new Bytes[1];    /* 'fileencoding' */
        Bytes[]     b_p_ff   = new Bytes[1];    /* 'fileformat' */
        Bytes[]     b_p_ft   = new Bytes[1];    /* 'filetype' */
        Bytes[]     b_p_fo   = new Bytes[1];    /* 'formatoptions' */
        Bytes[]     b_p_flp  = new Bytes[1];    /* 'formatlistpat' */
        boolean[]   b_p_inf  = new boolean[1];  /* 'infercase' */
        Bytes[]     b_p_isk  = new Bytes[1];    /* 'iskeyword' */
        Bytes[]     b_p_inde = new Bytes[1];    /* 'indentexpr' */
        long[]      b_p_inde_flags = new long[1];             /* flags for 'indentexpr' */
        Bytes[]     b_p_indk = new Bytes[1];    /* 'indentkeys' */
        Bytes[]     b_p_fex  = new Bytes[1];    /* 'formatexpr' */
        long[]      b_p_fex_flags = new long[1];              /* flags for 'formatexpr' */
        Bytes[]     b_p_kp   = new Bytes[1];    /* 'keywordprg' */
        boolean[]   b_p_lisp = new boolean[1];  /* 'lisp' */
        Bytes[]     b_p_mps  = new Bytes[1];    /* 'matchpairs' */
        boolean[]   b_p_ma   = new boolean[1];  /* 'modifiable' */
        Bytes[]     b_p_nf   = new Bytes[1];    /* 'nrformats' */
        boolean[]   b_p_pi   = new boolean[1];  /* 'preserveindent' */
        Bytes[]     b_p_qe   = new Bytes[1];    /* 'quoteescape' */
        boolean[]   b_p_ro   = new boolean[1];  /* 'readonly' */
        long[]      b_p_sw   = new long[1];     /* 'shiftwidth' */
        boolean[]   b_p_si   = new boolean[1];  /* 'smartindent' */
        long[]      b_p_sts  = new long[1];     /* 'softtabstop' */
        long        b_p_sts_nopaste;            /* "b_p_sts" saved for paste mode */
        long[]      b_p_smc  = new long[1];     /* 'synmaxcol' */
        Bytes[]     b_p_syn  = new Bytes[1];    /* 'syntax' */
        long[]      b_p_ts   = new long[1];     /* 'tabstop' */
        boolean[]   b_p_tx   = new boolean[1];  /* 'textmode' */
        long[]      b_p_tw   = new long[1];     /* 'textwidth' */
        long        b_p_tw_nobin;               /* "b_p_tw" saved for binary mode */
        long        b_p_tw_nopaste;             /* "b_p_tw" saved for paste mode */
        long[]      b_p_wm   = new long[1];     /* 'wrapmargin' */
        long        b_p_wm_nobin;               /* "b_p_wm" saved for binary mode */
        long        b_p_wm_nopaste;             /* "b_p_wm" saved for paste mode */

        /* local values for options which are normally global */
        Bytes[]     b_p_ep   = new Bytes[1];    /* 'equalprg' local value */
        /*int*/long[]       b_p_ar   = new /*int*/long[1];      /* 'autoread' local value */
        long[]      b_p_ul   = new long[1];     /* 'undolevels' local value */
        boolean[]   b_p_udf  = new boolean[1];  /* 'undofile' */
        Bytes[]     b_p_lw   = new Bytes[1];    /* 'lispwords' local value */

        /* end of buffer options */

        /* values set from "b_p_cino" */
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

        long        b_no_eol_lnum;          /* non-zero lnum when last line of next binary
                                             * write should not have an end-of-line */

        boolean     b_start_eol;            /* last line had eol when it was read */
        int         b_start_ffc;            /* first char of 'ff' when edit started */
        Bytes       b_start_fenc;           /* 'fileencoding' when edit started or null */
        int         b_bad_char;             /* "++bad=" argument when edit started or 0 */
        boolean     b_start_bomb;           /* 'bomb' when it was read */

        dictitem_C  b_bufvar;               /* variable for "b:" Dictionary */
        dict_C      b_vars;                 /* internal variables, local to buffer */

        boolean     b_did_warn;             /* set to true if user has been warned on first
                                             * change of a read-only file */

        boolean     b_shortname;            /* this file has an 8.3 file name */

        synblock_C  b_s;                    /* Info related to syntax highlighting.  w_s
                                             * normally points to this, but some windows
                                             * may use a different synblock_C. */

        int         b_mapped_ctrl_c;        /* modes where CTRL-C is mapped */

        /*private*/ buffer_C()
        {
        }
    }

    /*private*/ static final int SNAP_AUCMD_IDX = 0;
    /*private*/ static final int SNAP_COUNT     = 1;

    /*
     * Tab pages point to the top frame of each tab page.
     * Note: Most values are NOT valid for the current tab page!  Use "curwin",
     * "firstwin", etc. for that.  "tp_topframe" is always valid and can be
     * compared against "topframe" to find the current tab page.
     */
    /*private*/ static final class tabpage_C
    {
        tabpage_C       tp_next;            /* next tabpage or null */
        frame_C         tp_topframe;        /* topframe for the windows */
        window_C        tp_curwin;          /* current window in this Tab page */
        window_C        tp_prevwin;         /* previous window in this Tab page */
        window_C        tp_firstwin;        /* first window in this Tab page */
        window_C        tp_lastwin;         /* last window in this Tab page */
        long            tp_old_Rows;        /* Rows when Tab page was left */
        long            tp_old_Columns;     /* Columns when Tab page was left */
        long            tp_ch_used;         /* value of 'cmdheight' when frame size was set */
        frame_C[]       tp_snapshot;        /* window layout snapshots */
        dictitem_C      tp_winvar;          /* variable for "t:" Dictionary */
        dict_C          tp_vars;            /* internal variables, local to tab page */

        /*private*/ tabpage_C()
        {
            tp_snapshot = new frame_C[SNAP_COUNT];
            tp_winvar = new dictitem_C();
        }
    }

    /*
     * Structure to cache info for displayed lines in w_lines[].
     * Each logical line has one entry.
     * The entry tells how the logical line is currently displayed in the window.
     * This is updated when displaying the window.
     * When the display is changed (e.g., when clearing the screen) w_lines_valid
     * is changed to exclude invalid entries.
     * When making changes to the buffer, wl_valid is reset to indicate wl_size
     * may not reflect what is actually in the buffer.  When wl_valid is false,
     * the entries can only be used to count the number of displayed lines used.
     * wl_lnum and wl_lastlnum are invalid too.
     */
    /*private*/ static final class wline_C
    {
        long        wl_lnum;        /* buffer line number for logical line */
        int         wl_size;        /* height in screen lines */
        boolean     wl_valid;       /* true values are valid for text in buffer */

        /*private*/ wline_C()
        {
        }
    }

    /*private*/ static void COPY_wline(wline_C wl1, wline_C wl0)
    {
        wl1.wl_lnum = wl0.wl_lnum;
        wl1.wl_size = wl0.wl_size;
        wl1.wl_valid = wl0.wl_valid;
    }

    /*private*/ static wline_C[] ARRAY_wline(int n)
    {
        wline_C[] a = new wline_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new wline_C();
        return a;
    }

    /*
     * Windows are kept in a tree of frames.  Each frame has a column (FR_COL)
     * or row (FR_ROW) layout or is a leaf, which has a window.
     */
    /*private*/ static final class frame_C
    {
        byte        fr_layout;      /* FR_LEAF, FR_COL or FR_ROW */
        int         fr_width;
        int         fr_newwidth;    /* new width used in win_equal_rec() */
        int         fr_height;
        int         fr_newheight;   /* new height used in win_equal_rec() */
        frame_C     fr_parent;      /* containing frame or null */
        frame_C     fr_next;        /* frame right or below in same parent, null for first */
        frame_C     fr_prev;        /* frame left or above in same parent, null for last */
                                    /* fr_child and fr_win are mutually exclusive */
        frame_C     fr_child;       /* first contained frame */
        window_C    fr_win;         /* window that fills this frame */

        /*private*/ frame_C()
        {
        }
    }

    /*private*/ static void COPY_frame(frame_C fr1, frame_C fr0)
    {
        fr1.fr_layout = fr0.fr_layout;
        fr1.fr_width = fr0.fr_width;
        fr1.fr_newwidth = fr0.fr_newwidth;
        fr1.fr_height = fr0.fr_height;
        fr1.fr_newheight = fr0.fr_newheight;
        fr1.fr_parent = fr0.fr_parent;
        fr1.fr_next = fr0.fr_next;
        fr1.fr_prev = fr0.fr_prev;
        fr1.fr_child = fr0.fr_child;
        fr1.fr_win = fr0.fr_win;
    }

    /*private*/ static final byte
        FR_LEAF = 0,            /* frame is a leaf */
        FR_ROW  = 1,            /* frame with a row of windows */
        FR_COL  = 2;            /* frame with a column of windows */

    /*
     * Struct used for highlighting 'hlsearch' matches, matches defined by ":match" and
     * matches defined by match functions.  For 'hlsearch' there is one pattern for all windows.
     * For ":match" and the match functions there is a different pattern for each window.
     */
    /*private*/ static final class match_C
    {
        regmmatch_C rmm;                    /* points to the regexp program; contains last
                                             * found match (may continue in next line) */
        buffer_C    buf;                    /* the buffer to search for a match */
        long        lnum;                   /* the line to search for a match */
        int         attr;                   /* attributes to be used for a match */
        int         attr_cur;               /* attributes currently active in win_line() */
        long        first_lnum;             /* first lnum to search for multi-line pat */
        int         startcol;               /* in win_line() points to char where HL starts */
        int         endcol;                 /* in win_line() points to char where HL ends */
        timeval_C   tm = new timeval_C();   /* for a time limit */

        /*private*/ match_C()
        {
            rmm = new regmmatch_C();
        }
    }

    /* number of positions supported by matchaddpos() */
    /*private*/ static final int MAXPOSMATCH = 8;

    /*
     * Same as lpos_C, but with additional field len.
     */
    /*private*/ static final class llpos_C
    {
        long        lnum;       /* line number */
        int         col;        /* column number */
        int         len;        /* length: 0 - to the end of line */

        /*private*/ llpos_C()
        {
        }
    }

    /*private*/ static void COPY_llpos(llpos_C llp1, llpos_C llp0)
    {
        llp1.lnum = llp0.lnum;
        llp1.col = llp0.col;
        llp1.len = llp0.len;
    }

    /*private*/ static llpos_C[] ARRAY_llpos(int n)
    {
        llpos_C[] a = new llpos_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new llpos_C();
        return a;
    }

    /*
     * posmatch_C provides an array for storing match items for matchaddpos() function.
     */
    /*private*/ static final class posmatch_C
    {
        llpos_C[]   pm_pos;         /* array of positions */
        int         cur;            /* internal position counter */
        long        toplnum;        /* top buffer line */
        long        botlnum;        /* bottom buffer line */

        /*private*/ posmatch_C()
        {
            pm_pos = ARRAY_llpos(MAXPOSMATCH);
        }
    }

    /*private*/ static posmatch_C[] ARRAY_posmatch(int n)
    {
        posmatch_C[] a = new posmatch_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new posmatch_C();
        return a;
    }

    /*
     * matchitem_C provides a linked list for storing match items for ":match" and the match functions.
     */
    /*private*/ static final class matchitem_C
    {
        matchitem_C next;
        int         id;                 /* match ID */
        int         priority;           /* match priority */
        Bytes       pattern;            /* pattern to highlight */
        int         hlg_id;             /* highlight group ID */
        regmmatch_C mi_match;           /* regexp program for "pattern" */
        posmatch_C  mi_pos;             /* position matches */
        match_C     mi_hl;              /* struct for doing the actual highlighting */

        /*private*/ matchitem_C()
        {
            mi_match = new regmmatch_C();
            mi_pos = new posmatch_C();
            mi_hl = new match_C();
        }
    }

    /*
     * Structure which contains all information that belongs to a window
     *
     * All row numbers are relative to the start of the window, except w_winrow.
     */
    /*private*/ static final class window_C
    {
        buffer_C    w_buffer;           /* buffer we are a window into (used
                                         * often, keep it the first item!) */

        synblock_C  w_s;                /* for :ownsyntax */

        window_C    w_prev;             /* link to previous window */
        window_C    w_next;             /* link to next window */
        boolean     w_closing;          /* window is being closed, don't let
                                         * autocommands close it too. */

        frame_C     w_frame;            /* frame containing this window */

        pos_C       w_cursor;           /* cursor position in buffer */

        int         w_curswant;         /* The column we'd like to be at.  This is
                                         * used to try to stay in the same column
                                         * for up/down cursor motions. */

        boolean     w_set_curswant;     /* If set, then update w_curswant the next
                                         * time through cursupdate() to the
                                         * current virtual column */

        /*
         * the next six are used to update the visual part
         */
        int         w_old_visual_mode;  /* last known VIsual_mode */
        long        w_old_cursor_lnum;  /* last known end of visual part */
        int         w_old_cursor_fcol;  /* first column for block visual part */
        int         w_old_cursor_lcol;  /* last column for block visual part */
        long        w_old_visual_lnum;  /* last known start of visual part */
        int         w_old_visual_col;   /* last known start of visual part */
        int         w_old_curswant;     /* last known value of Curswant */

        /*
         * "w_topline", "w_leftcol" and "w_skipcol" specify the offsets for displaying the buffer.
         */
        long        w_topline;          /* buffer line number of the line at the
                                         * top of the window */
        boolean     w_topline_was_set;  /* flag set to true when topline is set,
                                         * e.g. by winrestview() */
        int         w_leftcol;          /* window column number of the left most character
                                         * in the window; used when 'wrap' is off */
        int         w_skipcol;          /* starting column when a single line
                                         * doesn't fit in the window */

        /*
         * Layout of the window in the screen.
         * May need to add "msg_scrolled" to "w_winrow" in rare situations.
         */
        int         w_winrow;           /* first row of window in screen */
        int         w_height;           /* number of rows in window, excluding status/command line(s) */
        int         w_status_height;    /* number of status lines (0 or 1) */
        int         w_wincol;           /* leftmost column of window in screen */
        int         w_width;            /* width of window, excluding separation */
        int         w_vsep_width;       /* number of separator columns (0 or 1) */

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
        pos_C       w_valid_cursor;     /* last known position of w_cursor, used to adjust w_valid */
        int         w_valid_leftcol;    /* last known w_leftcol */

        /*
         * w_cline_height is the number of physical lines taken by the buffer line
         * that the cursor is on.  We use this to avoid extra calls to plines().
         */
        int         w_cline_height;     /* current size of cursor line */

        int         w_cline_row;        /* starting row of the cursor line */

        int         w_virtcol;          /* column number of the cursor in the buffer line,
                                         * as opposed to the column number we're at on the screen.
                                         * This makes a difference on lines which span more than
                                         * one screen line or when w_leftcol is non-zero */

        /*
         * w_wrow and w_wcol specify the cursor position in the window.
         * This is related to positions in the window, not in the display or
         * buffer, thus w_wrow is relative to w_winrow.
         */
        int         w_wrow, w_wcol;     /* cursor position in window */

        long        w_botline;          /* number of the line below the bottom of the screen */
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
        wline_C[]   w_lines;
        int         w_lines_len;

        int         w_nrwidth;          /* width of 'number' and 'relativenumber' column being used */

        /*
         * === end of cached values ===
         */

        int         w_redr_type;        /* type of redraw to be performed on win */
        int         w_upd_rows;         /* number of window lines to update when
                                         * w_redr_type is REDRAW_TOP */
        long        w_redraw_top;       /* when != 0: first line needing redraw */
        long        w_redraw_bot;       /* when != 0: last line needing redraw */
        boolean     w_redr_status;      /* if true status line must be redrawn */

        /* remember what is shown in the ruler for this window (if 'ruler' set) */
        pos_C       w_ru_cursor;        /* cursor position shown in ruler */
        int         w_ru_virtcol;       /* virtcol shown in ruler */
        long        w_ru_topline;       /* topline shown in ruler */
        long        w_ru_line_count;    /* line count used for ruler */
        boolean     w_ru_empty;         /* true if ruler shows 0-1 (empty line) */

        int         w_alt_fnum;         /* alternate file (for # and CTRL-^) */

        alist_C     w_alist;            /* pointer to arglist for this window */
        int         w_arg_idx;          /* current index in argument list (can be out of range!) */
        boolean     w_arg_idx_invalid;  /* editing another file than w_arg_idx */

        /*
         * Options local to a window.
         * They are local because they influence the layout of the window or depend on the window layout.
         * There are two values:
         * "w_onebuf_opt" is local to the buffer currently in this window,
         * "w_allbuf_opt" is for all buffers in this window.
         */
        winopt_C    w_onebuf_opt = new winopt_C();
        winopt_C    w_allbuf_opt = new winopt_C();

        /* A few options have local flags for P_INSECURE. */
        long[]      w_p_stl_flags = new long[1];      /* flags for 'statusline' */
        int[]       w_p_cc_cols;        /* array of columns to highlight or null */
        int         w_p_brimin;         /* minimum width for breakindent */
        int         w_p_brishift;       /* additional shift for breakindent */
        boolean     w_p_brisbr;         /* sbr in 'briopt' */

        long        w_scbind_pos;

        dictitem_C  w_winvar;           /* variable for "w:" Dictionary */
        dict_C      w_vars;             /* internal variables, local to window */

        /*
         * The w_prev_pcmark field is used to check whether we really did jump
         * to a new line after setting the w_pcmark.  If not, then we revert to
         * using the previous w_pcmark.
         */
        pos_C       w_pcmark;           /* previous context mark */
        pos_C       w_prev_pcmark;      /* previous w_pcmark */

        /*
         * the jumplist contains old cursor positions
         */
        xfmark_C[]  w_jumplist;
        int         w_jumplistlen;      /* number of active entries */
        int         w_jumplistidx;      /* current position */

        int         w_changelistidx;    /* current position in b_changelist */

        matchitem_C w_match_head;       /* head of match list */
        int         w_next_match_id;    /* next match ID */

        /*
         * w_fraction is the fractional row of the cursor within the window,
         * from 0 at the top row to FRACTION_MULT at the last row.
         * w_prev_fraction_row was the actual cursor row when w_fraction was last calculated.
         */
        int         w_fraction;
        int         w_prev_fraction_row;

        long        w_nrwidth_line_count;   /* line count when ml_nrwidth_width was computed. */
        long        w_nuw_cached;           /* 'numberwidth' option cached */
        int         w_nrwidth_width;        /* nr of chars to print line count. */

        /*private*/ window_C()
        {
            w_cursor = new pos_C();
            w_valid_cursor = new pos_C();
            w_ru_cursor = new pos_C();

            w_winvar = new dictitem_C();
            w_pcmark = new pos_C();
            w_prev_pcmark = new pos_C();
            w_jumplist = ARRAY_xfmark(JUMPLISTSIZE);
        }
    }

    /*
     * Arguments for operators.
     */
    /*private*/ static final class oparg_C
    {
        int         op_type;        /* current pending operator type */
        int         regname;        /* register to use for the operator */
        byte        motion_type;    /* type of the current cursor motion */
        int         motion_force;   /* force motion type: 'v', 'V' or CTRL-V */
        boolean     use_reg_one;    /* true if delete uses reg 1 even when not linewise */
        boolean     inclusive;      /* true if char motion is inclusive
                                     * (only valid when motion_type is MCHAR */
        boolean     end_adjusted;   /* backuped b_op_end one char (only used by do_format()) */
        pos_C       op_start;       /* start of the operator */
        pos_C       op_end;         /* end of the operator */
        pos_C       cursor_start;   /* cursor position before motion for "gw" */

        long        line_count;     /* number of lines from op_start to op_end (inclusive) */
        boolean     empty;          /* op_start and op_end the same (only used by do_change()) */
        boolean     is_VIsual;      /* operator on Visual area */
        boolean     block_mode;     /* current operator is Visual block mode */
        int         start_vcol;     /* start col for block mode operator */
        int         end_vcol;       /* end col for block mode operator */
        long        prev_opcount;   /* ca.opcount saved for K_CURSORHOLD */
        long        prev_count0;    /* ca.count0 saved for K_CURSORHOLD */

        /*private*/ oparg_C()
        {
            op_start = new pos_C();
            op_end = new pos_C();
            cursor_start = new pos_C();
        }
    }

    /*
     * Arguments for Normal mode commands.
     */
    /*private*/ static final class cmdarg_C
    {
        oparg_C     oap;                /* operator arguments */
        int         prechar;            /* prefix character (optional, always 'g') */
        int         cmdchar;            /* command character */
        int[]       nchar = new int[1]; /* next command character (optional) */
        int         ncharC1;            /* first composing character (optional) */
        int         ncharC2;            /* second composing character (optional) */
        int[]       extra_char = new int[1]; /* yet another character (optional) */
        long        opcount;            /* count before an operator */
        long        count0;             /* count before command, default 0 */
        long        count1;             /* count before command, default 1 */
        int         arg;                /* extra argument from nv_cmds[] */
        int         retval;             /* return: CA_* values */
        Bytes       searchbuf;          /* return: pointer to search pattern or null */

        /*private*/ cmdarg_C()
        {
        }
    }

    /* values for retval: */
    /*private*/ static final int CA_COMMAND_BUSY  = 1;  /* skip restarting edit() once */
    /*private*/ static final int CA_NO_ADJ_OP_END = 2;  /* don't adjust operator end */

    /*
     * Struct to save values in before executing autocommands for a buffer that is
     * not the current buffer.  Without FEAT_AUTOCMD only "curbuf" is remembered.
     */
    /*private*/ static final class aco_save_C
    {
        buffer_C    save_curbuf;    /* saved curbuf */
        boolean     use_aucmd_win;  /* using aucmd_win */
        window_C    save_curwin;    /* saved curwin */
        window_C    new_curwin;     /* new curwin */
        buffer_C    new_curbuf;     /* new curbuf */

        /*private*/ aco_save_C()
        {
        }
    }

    /*private*/ static final class context_sha256_C
    {
        long        total;
        int[]       state = new int[8];
        Bytes       buffer = new Bytes(64);

        /*private*/ context_sha256_C()
        {
        }
    }

    /* ----------------------------------------------------------------------- */

    /* Codes for mouse button events in lower three bits: */
    /*private*/ static final int MOUSE_LEFT     = 0x00;
    /*private*/ static final int MOUSE_MIDDLE   = 0x01;
    /*private*/ static final int MOUSE_RIGHT    = 0x02;
    /*private*/ static final int MOUSE_RELEASE  = 0x03;

    /* bit masks for modifiers: */
    /*private*/ static final int MOUSE_SHIFT    = 0x04;
    /*private*/ static final int MOUSE_ALT      = 0x08;
    /*private*/ static final int MOUSE_CTRL     = 0x10;

    /* mouse buttons that are handled like a key press (GUI only) */
    /* Note that the scroll wheel keys are inverted: MOUSE_5 scrolls lines up but
     * the result of this is that the window moves down, similarly MOUSE_6 scrolls
     * columns left but the window moves right. */
    /*private*/ static final int MOUSE_4    = 0x100;    /* scroll wheel down */
    /*private*/ static final int MOUSE_5    = 0x200;    /* scroll wheel up */

    /*private*/ static final int MOUSE_X1   = 0x300;    /* Mouse-button X1 (6th) */
    /*private*/ static final int MOUSE_X2   = 0x400;    /* Mouse-button X2 */

    /*private*/ static final int MOUSE_6    = 0x500;    /* scroll wheel left */
    /*private*/ static final int MOUSE_7    = 0x600;    /* scroll wheel right */

    /* 0x20 is reserved by xterm */
    /*private*/ static final int MOUSE_DRAG_XTERM = 0x40;

    /*private*/ static final int MOUSE_DRAG = (0x40 | MOUSE_RELEASE);

    /* Lowest button code for using the mouse wheel (xterm only). */
    /*private*/ static final int MOUSEWHEEL_LOW     = 0x60;

    /*private*/ static final int MOUSE_CLICK_MASK   = 0x03;

    /*
     * jump_to_mouse() returns one of first four these values, possibly with
     * some of the other three added.
     */
    /*private*/ static final int IN_UNKNOWN         = 0;
    /*private*/ static final int IN_BUFFER          = 1;
    /*private*/ static final int IN_STATUS_LINE     = 2;        /* on status or command line */
    /*private*/ static final int IN_SEP_LINE        = 4;        /* on vertical separator line */
    /*private*/ static final int IN_OTHER_WIN       = 8;        /* in other window but can't go there */
    /*private*/ static final int CURSOR_MOVED       = 0x100;

    /* flags for jump_to_mouse() */
    /*private*/ static final int MOUSE_FOCUS        = 0x01;     /* need to stay in this window */
    /*private*/ static final int MOUSE_MAY_VIS      = 0x02;     /* may start Visual mode */
    /*private*/ static final int MOUSE_DID_MOVE     = 0x04;     /* only act when mouse has moved */
    /*private*/ static final int MOUSE_SETPOS       = 0x08;     /* only set current mouse position */
    /*private*/ static final int MOUSE_MAY_STOP_VIS = 0x10;     /* may stop Visual mode */
    /*private*/ static final int MOUSE_RELEASED     = 0x20;     /* button was released */

    /* defines for eval_vars() */
    /*private*/ static final int VALID_PATH          = 1;
    /*private*/ static final int VALID_HEAD          = 2;

    /* Defines for Vim variables.  These must match vimvars[] in eval.c! */
    /*private*/ static final int
        VV_COUNT         =  0,
        VV_COUNT1        =  1,
        VV_PREVCOUNT     =  2,
        VV_ERRMSG        =  3,
        VV_WARNINGMSG    =  4,
        VV_STATUSMSG     =  5,
        VV_LNUM          =  6,
        VV_TERMRESPONSE  =  7,
        VV_CMDARG        =  8,
        VV_DYING         =  9,
        VV_EXCEPTION     = 10,
        VV_THROWPOINT    = 11,
        VV_REG           = 12,
        VV_CMDBANG       = 13,
        VV_INSERTMODE    = 14,
        VV_VAL           = 15,
        VV_KEY           = 16,
        VV_FCS_REASON    = 17,
        VV_FCS_CHOICE    = 18,
        VV_SCROLLSTART   = 19,
        VV_SWAPCOMMAND   = 20,
        VV_CHAR          = 21,
        VV_MOUSE_WIN     = 22,
        VV_MOUSE_LNUM    = 23,
        VV_MOUSE_COL     = 24,
        VV_OP            = 25,
        VV_SEARCHFORWARD = 26,
        VV_HLSEARCH      = 27,
        VV_OLDFILES      = 28,
        VV_LEN           = 29;      /* number of v: vars */

    /* Selection states for modeless selection. */
    /*private*/ static final int SELECT_CLEARED     = 0;
    /*private*/ static final int SELECT_IN_PROGRESS = 1;
    /*private*/ static final int SELECT_DONE        = 2;

    /*private*/ static final int SELECT_MODE_CHAR   = 0;
    /*private*/ static final int SELECT_MODE_WORD   = 1;
    /*private*/ static final int SELECT_MODE_LINE   = 2;

    /* Info about selected text. */
    /*private*/ static final class clipboard_C
    {
        boolean     available;      /* Is clipboard available? */
        boolean     owned;          /* Flag: do we own the selection? */
        pos_C       cbd_start;      /* start of selected area */
        pos_C       cbd_end;        /* end of selected area */
        int         vmode;          /* visual mode character */

        /* Fields for selection that doesn't use Visual mode. */
        int         origin_row;
        int         origin_start_col;
        int         origin_end_col;
        int         word_start_col;
        int         word_end_col;

        pos_C       cbd_prev;       /* previous position */
        short       state;          /* current selection state */
        short       mode;           /* select by char, word, or line */

        /*private*/ clipboard_C()
        {
            cbd_start = new pos_C();
            cbd_end = new pos_C();
            cbd_prev = new pos_C();
        }
    }
}
