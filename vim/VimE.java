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
public class VimE
{
    /*
     * ex_cmds.h --------------------------------------------------------------------------------------
     */

    /*private*/ static final int
        RANGE           = 0x001,   /* allow a linespecs */
        BANG            = 0x002,   /* allow a ! after the command name */
        EXTRA           = 0x004,   /* allow extra args after command name */
        XFILE           = 0x008,   /* expand wildcards in extra part */
        NOSPC           = 0x010,   /* no spaces allowed in the extra part */
        DFLALL          = 0x020,   /* default file range is 1,$ */

        NEEDARG         = 0x080,   /* argument required */
        TRLBAR          = 0x100,   /* check for trailing vertical bar */
        REGSTR          = 0x200,   /* allow "x for register designation */
        COUNT           = 0x400,   /* allow count in argument, after command */
        NOTRLCOM        = 0x800,   /* no trailing comment allowed */
        ZEROR          = 0x1000,   /* zero line number allowed */
        USECTRLV       = 0x2000,   /* do not remove CTRL-V from argument */
        NOTADR         = 0x4000,   /* number before command is not an address */
        EDITCMD        = 0x8000,   /* allow "+command" argument */
        BUFNAME       = 0x10000,   /* accepts buffer name */
        BUFUNL        = 0x20000,   /* accepts unlisted buffer too */
        ARGOPT        = 0x40000,   /* allow "++opt=val" argument */
        SBOXOK        = 0x80000,   /* allowed in the sandbox */
        CMDWIN       = 0x100000,   /* allowed in cmdline window */
        MODIFY       = 0x200000,   /* forbidden in non-'modifiable' buffer */
        EXFLAGS      = 0x400000,   /* allow flags after count in argument */

        FILES = (XFILE | EXTRA),   /* multiple extra files allowed */
        WORD1 = (EXTRA | NOSPC),   /* one extra word allowed */
        FILE1 = (FILES | NOSPC);   /* 1 file allowed, defaults to current file */

    /* values for cmd_addr_type */
    /*private*/ static final byte
        ADDR_LINES          = 0,
        ADDR_WINDOWS        = 1,
        ADDR_ARGUMENTS      = 2,
        ADDR_LOADED_BUFFERS = 3,
        ADDR_BUFFERS        = 4,
        ADDR_TABS           = 5;

    /*private*/ static final int
        CMD_append = 0,
        CMD_abbreviate = 1,
        CMD_abclear = 2,
        CMD_aboveleft = 3,
        CMD_all = 4,
        CMD_args = 5,
        CMD_argadd = 6,
        CMD_argdelete = 7,
        CMD_argdo = 8,
        CMD_argedit = 9,
        CMD_argglobal = 10,
        CMD_arglocal = 11,
        CMD_argument = 12,
        CMD_ascii = 13,
        CMD_autocmd = 14,
        CMD_augroup = 15,
        CMD_buffer = 16,
        CMD_bNext = 17,
        CMD_ball = 18,
        CMD_badd = 19,
        CMD_bdelete = 20,
        CMD_belowright = 21,
        CMD_bfirst = 22,
        CMD_blast = 23,
        CMD_bmodified = 24,
        CMD_bnext = 25,
        CMD_botright = 26,
        CMD_bprevious = 27,
        CMD_brewind = 28,
        CMD_break = 29,
        CMD_breakadd = 30,
        CMD_breakdel = 31,
        CMD_breaklist = 32,
        CMD_browse = 33,
        CMD_buffers = 34,
        CMD_bufdo = 35,
        CMD_bunload = 36,
        CMD_bwipeout = 37,
        CMD_change = 38,
        CMD_cabbrev = 39,
        CMD_cabclear = 40,
        CMD_call = 41,
        CMD_catch = 42,
        CMD_center = 43,
        CMD_changes = 44,
        CMD_checktime = 45,
        CMD_close = 46,
        CMD_cmap = 47,
        CMD_cmapclear = 48,
        CMD_cnoremap = 49,
        CMD_cnoreabbrev = 50,
        CMD_copy = 51,
        CMD_colorscheme = 52,
        CMD_command = 53,
        CMD_comclear = 54,
        CMD_continue = 55,
        CMD_confirm = 56,
        CMD_cquit = 57,
        CMD_cunmap = 58,
        CMD_cunabbrev = 59,
        CMD_delete = 60,
        CMD_delmarks = 61,
        CMD_debug = 62,
        CMD_debuggreedy = 63,
        CMD_delcommand = 64,
        CMD_delfunction = 65,
        CMD_display = 66,
        CMD_digraphs = 67,
        CMD_doautocmd = 68,
        CMD_doautoall = 69,
        CMD_edit = 70,
        CMD_earlier = 71,
        CMD_echo = 72,
        CMD_echoerr = 73,
        CMD_echohl = 74,
        CMD_echomsg = 75,
        CMD_echon = 76,
        CMD_else = 77,
        CMD_elseif = 78,
        CMD_endif = 79,
        CMD_endfunction = 80,
        CMD_endfor = 81,
        CMD_endtry = 82,
        CMD_endwhile = 83,
        CMD_enew = 84,
        CMD_ex = 85,
        CMD_execute = 86,
        CMD_exit = 87,
        CMD_files = 88,
        CMD_finally = 89,
        CMD_finish = 90,
        CMD_first = 91,
        CMD_fixdel = 92,
        CMD_for = 93,
        CMD_function = 94,
        CMD_global = 95,
        CMD_goto = 96,
        CMD_highlight = 97,
        CMD_hide = 98,
        CMD_history = 99,
        CMD_insert = 100,
        CMD_iabbrev = 101,
        CMD_iabclear = 102,
        CMD_if = 103,
        CMD_imap = 104,
        CMD_imapclear = 105,
        CMD_inoremap = 106,
        CMD_inoreabbrev = 107,
        CMD_iunmap = 108,
        CMD_iunabbrev = 109,
        CMD_join = 110,
        CMD_jumps = 111,
        CMD_k = 112,
        CMD_keepmarks = 113,
        CMD_keepjumps = 114,
        CMD_keeppatterns = 115,
        CMD_keepalt = 116,
        CMD_list = 117,
        CMD_last = 118,
        CMD_later = 119,
        CMD_left = 120,
        CMD_leftabove = 121,
        CMD_let = 122,
        CMD_lmap = 123,
        CMD_lmapclear = 124,
        CMD_lnoremap = 125,
        CMD_lockmarks = 126,
        CMD_lockvar = 127,
        CMD_lunmap = 128,
        CMD_ls = 129,
        CMD_move = 130,
        CMD_mark = 131,
        CMD_map = 132,
        CMD_mapclear = 133,
        CMD_marks = 134,
        CMD_match = 135,
        CMD_messages = 136,
        CMD_mode = 137,
        CMD_next = 138,
        CMD_new = 139,
        CMD_nmap = 140,
        CMD_nmapclear = 141,
        CMD_nnoremap = 142,
        CMD_noremap = 143,
        CMD_noautocmd = 144,
        CMD_nohlsearch = 145,
        CMD_noreabbrev = 146,
        CMD_normal = 147,
        CMD_number = 148,
        CMD_nunmap = 149,
        CMD_open = 150,
        CMD_omap = 151,
        CMD_omapclear = 152,
        CMD_only = 153,
        CMD_onoremap = 154,
        CMD_ounmap = 155,
        CMD_ownsyntax = 156,
        CMD_print = 157,
        CMD_previous = 158,
        CMD_profdel = 159,
        CMD_put = 160,
        CMD_quit = 161,
        CMD_quitall = 162,
        CMD_qall = 163,
        CMD_read = 164,
        CMD_redo = 165,
        CMD_redir = 166,
        CMD_redraw = 167,
        CMD_redrawstatus = 168,
        CMD_registers = 169,
        CMD_resize = 170,
        CMD_retab = 171,
        CMD_return = 172,
        CMD_rewind = 173,
        CMD_right = 174,
        CMD_rightbelow = 175,
        CMD_runtime = 176,
        CMD_rundo = 177,
        CMD_substitute = 178,
        CMD_sNext = 179,
        CMD_sargument = 180,
        CMD_sall = 181,
        CMD_sandbox = 182,
        CMD_saveas = 183,
        CMD_sbuffer = 184,
        CMD_sbNext = 185,
        CMD_sball = 186,
        CMD_sbfirst = 187,
        CMD_sblast = 188,
        CMD_sbmodified = 189,
        CMD_sbnext = 190,
        CMD_sbprevious = 191,
        CMD_sbrewind = 192,
        CMD_scriptnames = 193,
        CMD_set = 194,
        CMD_setfiletype = 195,
        CMD_setglobal = 196,
        CMD_setlocal = 197,
        CMD_sfirst = 198,
        CMD_silent = 199,
        CMD_sleep = 200,
        CMD_slast = 201,
        CMD_smagic = 202,
        CMD_smap = 203,
        CMD_smapclear = 204,
        CMD_snext = 205,
        CMD_snomagic = 206,
        CMD_snoremap = 207,
        CMD_source = 208,
        CMD_sort = 209,
        CMD_split = 210,
        CMD_sprevious = 211,
        CMD_srewind = 212,
        CMD_stop = 213,
        CMD_startinsert = 214,
        CMD_startgreplace = 215,
        CMD_startreplace = 216,
        CMD_stopinsert = 217,
        CMD_sunhide = 218,
        CMD_sunmap = 219,
        CMD_suspend = 220,
        CMD_sview = 221,
        CMD_syntax = 222,
        CMD_syncbind = 223,
        CMD_t = 224,
        CMD_tab = 225,
        CMD_tabclose = 226,
        CMD_tabdo = 227,
        CMD_tabedit = 228,
        CMD_tabfirst = 229,
        CMD_tabmove = 230,
        CMD_tablast = 231,
        CMD_tabnext = 232,
        CMD_tabnew = 233,
        CMD_tabonly = 234,
        CMD_tabprevious = 235,
        CMD_tabNext = 236,
        CMD_tabrewind = 237,
        CMD_tabs = 238,
        CMD_throw = 239,
        CMD_topleft = 240,
        CMD_try = 241,
        CMD_undo = 242,
        CMD_undojoin = 243,
        CMD_undolist = 244,
        CMD_unabbreviate = 245,
        CMD_unhide = 246,
        CMD_unlet = 247,
        CMD_unlockvar = 248,
        CMD_unmap = 249,
        CMD_unsilent = 250,
        CMD_update = 251,
        CMD_vglobal = 252,
        CMD_verbose = 253,
        CMD_vertical = 254,
        CMD_visual = 255,
        CMD_view = 256,
        CMD_vmap = 257,
        CMD_vmapclear = 258,
        CMD_vnoremap = 259,
        CMD_vnew = 260,
        CMD_vsplit = 261,
        CMD_vunmap = 262,
        CMD_write = 263,
        CMD_wNext = 264,
        CMD_wall = 265,
        CMD_while = 266,
        CMD_winsize = 267,
        CMD_wincmd = 268,
        CMD_windo = 269,
        CMD_winpos = 270,
        CMD_wnext = 271,
        CMD_wprevious = 272,
        CMD_wq = 273,
        CMD_wqall = 274,
        CMD_wundo = 275,
        CMD_xit = 276,
        CMD_xall = 277,
        CMD_xmap = 278,
        CMD_xmapclear = 279,
        CMD_xnoremap = 280,
        CMD_xunmap = 281,
        CMD_yank = 282,
        CMD_z = 283,

    /* commands that don't start with a lowercase letter */

        CMD_bang = 284,
        CMD_pound = 285,
        CMD_and = 286,
        CMD_star = 287,
        CMD_lshift = 288,
        CMD_equal = 289,
        CMD_rshift = 290,
        CMD_at = 291,
        CMD_Next = 292,
        CMD_Print = 293,
        CMD_tilde = 294,

        CMD_SIZE = 295,     /* MUST be after all real commands! */
        CMD_USER = -1,      /* user-defined command */
        CMD_USER_BUF = -2;  /* user-defined command local to buffer */

    /*private*/ static abstract class getline_C
    {
        public abstract Bytes getline(int c, Object cookie, int indent);
    }

    /*
     * Arguments used for Ex commands.
     */
    /*private*/ static final class exarg_C
    {
        Bytes       arg;            /* argument of the command */
        Bytes       nextcmd;        /* next command (null if none) */
        Bytes       cmd;            /* the name of the command (except for :make) */
        Bytes[]     cmdlinep;       /* pointer to pointer of allocated cmdline */
        int         cmdidx;         /* the index for the command */
        long        argt;           /* flags for the command */
        boolean     skip;           /* don't execute the command, only parse it */
        boolean     forceit;        /* true if ! present */
        int         addr_count;     /* the number of addresses given */
        long        line1;          /* the first line number */
        long        line2;          /* the second line number or count */
        int         addr_type;      /* type of the count/range */
        int         flags;          /* extra flags after count: EXFLAG_ */
        Bytes       do_ecmd_cmd;    /* +command arg to be used in edited file */
        long        do_ecmd_lnum;   /* the line number in an edited file */
        boolean     append;         /* true with ":w >>file" command */
        boolean     usefilter;      /* true with ":w !command" and ":r!command" */
        int         amount;         /* number of '>' or '<' for shift command */
        int         regname;        /* register name (NUL if none) */
        int         force_bin;      /* 0, FORCE_BIN or FORCE_NOBIN */
        boolean     read_edit;      /* ++edit argument */
        int         bad_char;       /* BAD_KEEP, BAD_DROP or replacement byte */
        int         useridx;        /* user command index */
        Bytes       errmsg;         /* returned error message */
        getline_C   getline;
        Object      cookie;         /* argument for getline() */
        condstack_C cstack;         /* condition stack for ":if" etc. */

        /*private*/ exarg_C()
        {
        }
    }

    /*private*/ static final int FORCE_BIN = 1;         /* ":edit ++bin file" */
    /*private*/ static final int FORCE_NOBIN = 2;       /* ":edit ++nobin file" */

    /* Values for "flags". */
    /*private*/ static final int EXFLAG_LIST  = 0x01;   /* 'l': list */
    /*private*/ static final int EXFLAG_NR    = 0x02;   /* '#': number */
    /*private*/ static final int EXFLAG_PRINT = 0x04;   /* 'p': print */

    /* ----------------------------------------------------------------------- */

    /*private*/ static boolean asc_islower(int c)
    {
        return ('a' <= c && c <= 'z');
    }

    /*private*/ static boolean asc_isupper(int c)
    {
        return ('A' <= c && c <= 'Z');
    }

    /*private*/ static boolean asc_isalpha(int c)
    {
        return (asc_isupper(c) || asc_islower(c));
    }

    /*private*/ static boolean asc_isalnum(int c)
    {
        return (asc_isalpha(c) || asc_isdigit(c));
    }

    /*private*/ static boolean asc_iscntrl(int c)
    {
        return (0x00 <= c && c <= 0x1f)
            || (c == 0x7f);
    }

    /*private*/ static boolean asc_isgraph(int c)
    {
        return (0x21 <= c && c <= 0x7e);
    }

    /*private*/ static boolean asc_isprint(int c)
    {
        return (0x20 <= c && c <= 0x7e);
    }

    /*private*/ static boolean asc_ispunct(int c)
    {
        return (0x21 <= c && c <= 0x2f)
            || (0x3a <= c && c <= 0x40)
            || (0x5b <= c && c <= 0x60)
            || (0x7b <= c && c <= 0x7e);
    }
}
