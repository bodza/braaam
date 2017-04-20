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
public class VimF
{
    /* #include "globals.h" */          /* global variables and messages */

    /*
     * Number of Rows and Columns in the screen.
     * Must be long to be able to use them as options in option.c.
     * Note: Use screenRows and screenColumns to access items in screenLines[].
     * They may have different values when the screen wasn't (re)allocated yet
     * after setting Rows or Columns (e.g., when starting up).
     */
    /*private*/ static long[]   Rows = { 24L };     /* nr of rows in the screen */
    /*private*/ static long[]   Columns = { 80L };  /* nr of columns in the screen */

    /*
     * The characters that are currently on the screen are kept in screenLines[].
     * It is a single block of characters, the size of the screen plus one line.
     * The attributes for those characters are kept in screenAttrs[].
     *
     * "lineOffset[n]" is the offset from screenLines[] for the start of line 'n'.
     * The same value is used for screenLinesUC[] and screenAttrs[].
     *
     * Note: before the screen is initialized and when out of memory these can be null.
     */
    /*private*/ static Bytes    screenLines;
    /*private*/ static int[]    screenAttrs;
    /*private*/ static int[]    lineOffset;
    /*private*/ static boolean[] lineWraps;         /* line wraps to next line */

    /*
     * When using Unicode characters (in UTF-8 encoding) the character in
     * screenLinesUC[] contains the Unicode for the character at this position,
     * or NUL when the character in screenLines[] is to be used (ASCII char).
     * The composing characters are to be drawn on top of the original character.
     * screenLinesC[0][off] is only to be used when screenLinesUC[off] != 0.
     */
    /*private*/ static int[]    screenLinesUC;                      /* decoded UTF-8 characters */
    /*private*/ static int[][]  screenLinesC = new int[MAX_MCO][];  /* composing characters */
    /*private*/ static int      screen_mco;         /* value of "p_mco" used when allocating screenLinesC[] */

    /*
     * Indexes for tab page line:
     *      N > 0 for label of tab page N
     *      N == 0 for no label
     *      N < 0 for closing tab page -N
     *      N == -999 for closing current tab page
     */
    /*private*/ static short[]  tabPageIdxs;

    /*private*/ static int      screenRows;         /* actual size of screenLines[] */
    /*private*/ static int      screenColumns;      /* actual size of screenLines[] */

    /*
     * When vgetc() is called, it sets mod_mask to the set of modifiers that are
     * held down based on the MOD_MASK_* symbols that are read first.
     */
    /*private*/ static int      mod_mask = 0x0;     /* current key modifiers */

    /*
     * Cmdline_row is the row where the command line starts, just below the last window.
     * When the cmdline gets longer than the available space the screen gets scrolled up.
     * After a CTRL-D (show matches), after hitting ':' after "hit return",
     * and for the :global command, the command line is temporarily moved.
     * The old position is restored with the next call to update_screen().
     */
    /*private*/ static int      cmdline_row;

    /*private*/ static boolean  redraw_cmdline;     /* cmdline must be redrawn */
    /*private*/ static boolean  clear_cmdline;      /* cmdline must be cleared */
    /*private*/ static boolean  mode_displayed;     /* mode is being displayed */
    /*private*/ static int      cmdline_star;       /* cmdline is crypted */

    /*private*/ static boolean  exec_from_reg;      /* executing register */

    /*private*/ static /*MAYBEAN*/int  screen_cleared = FALSE;     /* screen has been cleared */

    /*
     * When '$' is included in 'cpoptions' option set:
     * When a change command is given that deletes only part of a line, a dollar
     * is put at the end of the changed text. dollar_vcol is set to the virtual
     * column of this '$'.  -1 is used to indicate no $ is being displayed.
     */
    /*private*/ static int      dollar_vcol = -1;

    /*
     * Functions for putting characters in the command line,
     * while keeping screenLines[] updated.
     */
    /*private*/ static boolean  cmdmsg_rl;          /* cmdline is drawn right to left */
    /*private*/ static int      msg_col;
    /*private*/ static int      msg_row;
    /*private*/ static int      msg_scrolled;       /* number of screen lines that windows have
                                                 * scrolled because of printing messages */
    /*private*/ static boolean  msg_scrolled_ign;   /* when true don't set need_wait_return in
                                                 * msg_puts_attr() when msg_scrolled is non-zero */

    /*private*/ static Bytes    keep_msg;           /* msg to be shown after redraw */
    /*private*/ static int      keep_msg_attr;      /* highlight attr for "keep_msg" */
    /*private*/ static boolean  keep_msg_more;      /* "keep_msg" was set by msgmore() */
    /*private*/ static boolean  need_fileinfo;      /* do fileinfo() after redraw */
    /*private*/ static boolean  msg_scroll;         /* msg_start() will scroll */
    /*private*/ static boolean  msg_didout;         /* msg_outstr() was used in line */
    /*private*/ static boolean  msg_didany;         /* msg_outstr() was used at all */
    /*private*/ static boolean  msg_nowait;         /* don't wait for this msg */
    /*private*/ static int      emsg_off;           /* don't display errors for now, unless 'debug' is set */
    /*private*/ static boolean  info_message;       /* printing informative message */
    /*private*/ static boolean  msg_hist_off;       /* don't add messages to history */
    /*private*/ static boolean  need_clr_eos;       /* need to clear text before displaying a message */
    /*private*/ static int      emsg_skip;          /* don't display errors for expression that is skipped */
    /*private*/ static boolean  emsg_severe;        /* use message of next of several emsg() calls for throw */
    /*private*/ static boolean  did_endif;          /* just had ":endif" */
    /*private*/ static dict_C   globvardict;        /* dictionary with g: variables */
    /*private*/ static dict_C   vimvardict;         /* dictionary with v: variables */
    /*private*/ static boolean  did_emsg;           /* set by emsg() when the message is displayed or thrown */
    /*private*/ static boolean  did_emsg_syntax;    /* did_emsg set because of a syntax error */
    /*private*/ static boolean  called_emsg;        /* always set by emsg() */
    /*private*/ static int      ex_exitval;         /* exit value for ex mode */
    /*private*/ static boolean  emsg_on_display;    /* there is an error message */
    /*private*/ static boolean  rc_did_emsg;        /* vim_regcomp() called emsg() */

    /*private*/ static int      no_wait_return;     /* don't wait for return for now */
    /*private*/ static boolean  need_wait_return;   /* need to wait for return later */
    /*private*/ static boolean  did_wait_return;    /* wait_return() was used and nothing written since then */

    /*private*/ static boolean  quit_more;          /* 'q' hit at "--more--" msg */
    /*private*/ static boolean  newline_on_exit;    /* did msg in altern. screen */
    /*private*/ static int      intr_char;          /* extra interrupt character */
    /*private*/ static boolean  ex_keep_indent;     /* getexmodeline(): keep indent */
    /*private*/ static int      vgetc_busy;         /* when inside vgetc() then > 0 */

    /*
     * Lines left before a "more" message.
     * Ex mode needs to be able to reset this after you type something.
     */
    /*private*/ static int      lines_left = -1;        /* lines left for listing */
    /*private*/ static boolean  msg_no_more;            /* don't use more prompt, truncate messages */

    /*private*/ static Bytes    sourcing_name;          /* name of error message source */
    /*private*/ static long     sourcing_lnum;          /* line number of the source file */

    /*private*/ static int      ex_nesting_level;       /* nesting level */
    /*private*/ static int      debug_break_level = -1; /* break below this level */
    /*private*/ static boolean  debug_did_msg;          /* did "debug mode" message */
    /*private*/ static int      debug_tick;             /* breakpoint change count */

    /*
     * The exception currently being thrown.  Used to pass an exception to
     * a different cstack.  Also used for discarding an exception before it is
     * caught or made pending.  Only valid when did_throw is true.
     */
    /*private*/ static except_C current_exception;

    /*
     * did_throw: An exception is being thrown.  Reset when the exception is caught
     * or as long as it is pending in a finally clause.
     */
    /*private*/ static boolean did_throw;

    /*
     * need_rethrow: set to true when a throw that cannot be handled in do_cmdline()
     * must be propagated to the cstack of the previously called do_cmdline().
     */
    /*private*/ static boolean need_rethrow;

    /*
     * check_cstack: set to true when a ":finish" or ":return" that cannot be
     * handled in do_cmdline() must be propagated to the cstack of the previously
     * called do_cmdline().
     */
    /*private*/ static boolean check_cstack;

    /*
     * Number of nested try conditionals (across function calls and ":source" commands).
     */
    /*private*/ static int trylevel;

    /*
     * When "force_abort" is true, always skip commands after an error message,
     * even after the outermost ":endif", ":endwhile" or ":endfor" or for a
     * function without the "abort" flag.  It is set to true when "trylevel" is
     * non-zero (and ":silent!" was not used) or an exception is being thrown at
     * the time an error is detected.  It is set to false when "trylevel" gets
     * zero again and there was no error or interrupt or throw.
     */
    /*private*/ static boolean force_abort;

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
    /*private*/ static msglist_C[] msg_list;

    /*
     * suppress_errthrow: When true, don't convert an error to an exception.  Used
     * when displaying the interrupt message or reporting an exception that is still
     * uncaught at the top level (which has already been discarded then).  Also used
     * for the error message when no exception can be thrown.
     */
    /*private*/ static boolean suppress_errthrow;

    /*
     * The stack of all caught and not finished exceptions.  The exception on the
     * top of the stack is the one got by evaluation of v:exception.  The complete
     * stack of all caught and pending exceptions is embedded in the various
     * cstacks; the pending exceptions, however, are not on the caught stack.
     */
    /*private*/ static except_C caught_stack;

    /* ID of script being sourced or was sourced to define the current function. */
    /*private*/ static int      current_SID;

    /* Magic number used for hashitem "hi_key" value indicating a deleted item.
     * Only the address is used. */
    /*private*/ static final Bytes HASH_REMOVED = u8("");

    /*private*/ static boolean hashitem_empty(hashitem_C hi)
    {
        return (hi.hi_key == null || hi.hi_key == HASH_REMOVED);
    }

    /*private*/ static boolean  scroll_region;                  /* term supports scroll region */
    /*private*/ static int      t_colors;                       /* int value of T_CCO */

    /*
     * When highlight_match is true, highlight a match, starting at the cursor position.
     * Search_match_lines is the number of lines after the match (0 for a match within one line),
     * search_match_endcol the column number of the character just after the match in the last line.
     */
    /*private*/ static boolean  highlight_match;                /* show search match pos */
    /*private*/ static long     search_match_lines;             /* lines of of matched string */
    /*private*/ static int      search_match_endcol;            /* col nr of match end */

    /*private*/ static boolean  no_smartcase;                   /* don't use 'smartcase' once */

    /*private*/ static boolean  need_check_timestamps;          /* need to check file timestamps asap */
    /*private*/ static boolean  did_check_timestamps;           /* did check timestamps recently */
    /*private*/ static int      no_check_timestamps;            /* don't check timestamps */

    /*private*/ static int[]    highlight_attr = new int[HLF_COUNT]; /* highl. attr. for each context */
    /*private*/ static int[]    highlight_user = new int[9];    /* user[1-9] attributes */
    /*private*/ static int[]    highlight_stlnc = new int[9];   /* on top of user */
    /*private*/ static int      cterm_normal_fg_color;
    /*private*/ static int      cterm_normal_fg_bold;
    /*private*/ static int      cterm_normal_bg_color;

    /*private*/ static int hl_attr(int n)
    {
        return highlight_attr[n];
    }

    /*private*/ static boolean  autocmd_busy;           /* Is apply_autocmds() busy? */
    /*private*/ static int      autocmd_no_enter;       /* *Enter autocmds disabled */
    /*private*/ static int      autocmd_no_leave;       /* *Leave autocmds disabled */
    /*private*/ static boolean  modified_was_set;       /* did ":set modified" */
    /*private*/ static boolean  did_filetype;           /* FileType event found */
    /*private*/ static boolean  keep_filetype;          /* value for did_filetype when
                                                     * starting to execute autocommands */

    /* When deleting the current buffer, another one must be loaded.
     * If we know which one is preferred, au_new_curbuf is set to it */
    /*private*/ static buffer_C au_new_curbuf;

    /* When deleting a buffer/window and autocmd_busy is true, do not free
     * the buffer/window, but link it in the list starting with
     * au_pending_free_buf/ap_pending_free_win, using b_next/w_next.
     * Free the buffer/window when autocmd_busy is being set to false. */
    /*private*/ static buffer_C au_pending_free_buf;
    /*private*/ static window_C au_pending_free_win;

    /*
     * Mouse coordinates, set by check_termcode()
     */
    /*private*/ static int      mouse_row;
    /*private*/ static int      mouse_col;
    /*private*/ static boolean  mouse_past_bottom;      /* mouse below last line */
    /*private*/ static boolean  mouse_past_eol;         /* mouse right of line */
    /*private*/ static int      mouse_dragging;         /* extending Visual area with mouse dragging */

    /* While redrawing the screen this flag is set.
     * It means the screen size ('lines' and 'rows') must not be changed. */
    /*private*/ static boolean  updating_screen;

    /*private*/ static clipboard_C clip_star = new clipboard_C(), clip_plus = new clipboard_C();

    /*private*/ static final int
        CLIP_UNNAMED      = 1,
        CLIP_UNNAMED_PLUS = 2;
    /*private*/ static int      clip_unnamed;   /* above two values or'ed */
    /*private*/ static int      clip_unnamed_saved;

    /*private*/ static boolean  clip_autoselect_star;
    /*private*/ static boolean  clip_autoselect_plus;
    /*private*/ static boolean  clip_autoselectml;
    /*private*/ static boolean  clip_html;
    /*private*/ static regprog_C clip_exclude_prog;
    /*private*/ static boolean  clip_did_set_selection = true;

    /*
     * All windows are linked in a list.  "firstwin" points to the first entry,
     * "lastwin" to the last entry (can be the same as "firstwin") and "curwin"
     * to the currently active window.
     */
    /*private*/ static window_C firstwin;               /* first window */
    /*private*/ static window_C lastwin;                /* last window */
    /*private*/ static window_C prevwin;                /* previous window */
    /*private*/ static window_C curwin;                 /* currently active window */

    /*private*/ static window_C aucmd_win;              /* window used in aucmd_prepbuf() */
    /*private*/ static boolean  aucmd_win_used;         /* aucmd_win is being used */

    /*
     * The window layout is kept in a tree of frames.  "topframe" points to the top of the tree.
     */
    /*private*/ static frame_C  topframe;               /* top of the window frame tree */

    /*
     * Tab pages are alternative topframes.  "first_tabpage" points to the first
     * one in the list, "curtab" is the current one.
     */
    /*private*/ static tabpage_C    first_tabpage;
    /*private*/ static tabpage_C    curtab;
    /*private*/ static boolean      redraw_tabline;     /* need to redraw tabline */

    /*
     * All buffers are linked in a list.  'firstbuf' points to the first entry,
     * 'lastbuf' to the last entry and 'curbuf' to the currently active buffer.
     */
    /*private*/ static buffer_C firstbuf;               /* first buffer */
    /*private*/ static buffer_C lastbuf;                /* last buffer */
    /*private*/ static buffer_C curbuf;                 /* currently active buffer */

    /*
     * List of files being edited (global argument list).
     * curwin.w_alist points to this when the window is using the global argument list.
     */
    /*private*/ static alist_C  global_alist = new alist_C(); /* global argument list */
    /*private*/ static int      max_alist_id;           /* the previous argument list id */
    /*private*/ static boolean  arg_had_last;           /* accessed last file in global_alist */

    /*private*/ static int      ru_col;                 /* column for ruler */
    /*private*/ static int      ru_wid;                 /* 'rulerfmt' width of ruler when non-zero */
    /*private*/ static int      sc_col;                 /* column for shown command */

    /*
     * When starting or exiting some things are done differently (e.g. screen updating).
     */
    /*private*/ static int      starting = NO_SCREEN;   /* first NO_SCREEN, then NO_BUFFERS and then
                                                     * set to 0 when starting up finished */
    /*private*/ static boolean  exiting;                /* true when planning to exit Vim.  Might still
                                                     * keep on running if there is a changed buffer. */

    /* volatile because it is used in signal handler deathtrap(). */
    /*private*/ static /*volatile*/transient boolean full_screen;    /* true when doing full-screen output
                                                     * otherwise only writing some messages */

    /*private*/ static boolean  restricted;         /* true when started as "rvim" */
    /*private*/ static int      secure;             /* non-zero when only "safe" commands are allowed,
                                                 * e.g. when sourcing .exrc or .vimrc in current directory */

    /*private*/ static int      textlock;           /* non-zero when changing text and jumping to
                                                 * another window or buffer is not allowed */

    /*private*/ static int      curbuf_lock;        /* non-zero when the current buffer can't be
                                                 * changed.  Used for FileChangedRO. */
    /*private*/ static int      allbuf_lock;        /* non-zero when no buffer name can be
                                                 * changed, no buffer can be deleted and
                                                 * current directory can't be changed.
                                                 * Used for SwapExists et al. */
    /*private*/ static int      sandbox;            /* Non-zero when evaluating an expression in a "sandbox".
                                                 * Several things are not allowed then. */

    /*private*/ static boolean  silent_mode;        /* set to true when "-s" commandline argument used for ex */

    /*private*/ static pos_C    VIsual = new pos_C();   /* start position of active Visual selection */
    /*private*/ static boolean  VIsual_active;          /* whether Visual mode is active */
    /*private*/ static boolean  VIsual_select;          /* whether Select mode is active */
    /*private*/ static boolean  VIsual_reselect;        /* whether to restart the selection
                                                     * after a Select mode mapping or menu */

    /*private*/ static int      VIsual_mode = 'v';      /* type of Visual mode */

    /*private*/ static boolean  redo_VIsual_busy;       /* true when redoing Visual */

    /*
     * When pasting text with the middle mouse button in visual mode with
     * restart_edit set, remember where it started so we can set insStart.
     */
    /*private*/ static pos_C    where_paste_started = new pos_C();

    /*
     * This flag is used to make auto-indent work right on lines where only a
     * <RETURN> or <ESC> is typed.  It is set when an auto-indent is done, and
     * reset when any other editing is done on the line.  If an <ESC> or <RETURN>
     * is received, and did_ai is true, the line is truncated.
     */
    /*private*/ static boolean did_ai;

    /*
     * Column of first char after autoindent.  0 when no autoindent done.
     * Used when 'backspace' is 0, to avoid backspacing over autoindent.
     */
    /*private*/ static int      ai_col;

    /*
     * This is a character which will end a start-middle-end comment when typed as
     * the first character on a new line.  It is taken from the last character of
     * the "end" comment leader when the COM_AUTO_END flag is given for that
     * comment end in 'comments'.  It is only valid when did_ai is true.
     */
    /*private*/ static int     end_comment_pending = NUL;

    /*
     * This flag is set after a ":syncbind" to let the check_scrollbind() function
     * know that it should not attempt to perform scrollbinding due to the scroll
     * that was a result of the ":syncbind." (Otherwise, check_scrollbind() will
     * undo some of the work done by ":syncbind.")  -ralston
     */
    /*private*/ static boolean did_syncbind;

    /*
     * This flag is set when a smart indent has been performed.
     * When the next typed character is a '{' the inserted tab will be deleted again.
     */
    /*private*/ static boolean  did_si;

    /*
     * This flag is set after an auto indent.
     * If the next typed character is a '}' one indent will be removed.
     */
    /*private*/ static boolean  can_si;

    /*
     * This flag is set after an "O" command.
     * If the next typed character is a '{' one indent will be removed.
     */
    /*private*/ static boolean  can_si_back;

    /*private*/ static pos_C    saved_cursor = new pos_C(); /* w_cursor before formatting text. */

    /*
     * Stuff for insert mode.
     * This is where the latest insert/append mode started.
     */
    /*private*/ static pos_C    insStart = new pos_C();

    /* This is where the latest insert/append mode started.  In contrast to
     * insStart, this won't be reset by certain keys and is needed for
     * op_insert(), to detect correctly where inserting by the user started. */
    /*private*/ static pos_C    insStart_orig = new pos_C();

    /*
     * Stuff for VREPLACE mode.
     */
    /*private*/ static long     orig_line_count;    /* Line count when "gR" started */
    /*private*/ static int      vr_lines_changed;   /* #Lines changed by "gR" so far */

    /*
     * "State" is the main state of Vim.
     * There are other variables that modify the state:
     * "Visual_mode"    When State is NORMAL or INSERT.
     * "finish_op"      When State is NORMAL, after typing the operator and before typing the motion command.
     */
    /*private*/ static int      State = NORMAL;     /* This is the current state of the command interpreter. */

    /*private*/ static boolean  finish_op;          /* true while an operator is pending */
    /*private*/ static long     opcount;            /* count for pending operator */

    /*
     * ex mode (Q) state
     */
    /*private*/ static int exmode_active;           /* zero, EXMODE_NORMAL or EXMODE_VIM */
    /*private*/ static boolean ex_no_reprint;       /* no need to print after z or p */

    /*private*/ static boolean Recording;           /* true when recording into a reg. */
    /*private*/ static boolean execReg;             /* true when executing a register */

    /*private*/ static int no_mapping;              /* currently no mapping allowed */
    /*private*/ static int no_zero_mapping;         /* mapping zero not allowed */
    /*private*/ static int allow_keys;              /* allow key codes when no_mapping is set */
    /*private*/ static int no_u_sync;               /* Don't call u_sync() */
    /*private*/ static int u_sync_once;             /* Call u_sync() once when evaluating an expression. */

    /*private*/ static int restart_edit;            /* call edit when next cmd finished */
    /*private*/ static boolean arrow_used;          /* Normally false, set to true after
                                                 * hitting cursor key in insert mode.
                                                 * Used by vgetorpeek() to decide when to call u_sync() */
    /*private*/ static boolean  ins_at_eol;         /* put cursor after eol when restarting edit after CTRL-O */

    /*private*/ static boolean  no_abbr = true;     /* true when no abbreviations loaded */

    /*private*/ static int      mapped_ctrl_c;              /* modes where CTRL-C is mapped */
    /*private*/ static boolean  ctrl_c_interrupts = true;   /* CTRL-C sets got_int */

    /*private*/ static cmdmod_C cmdmod = new cmdmod_C();    /* Ex command modifiers */

    /*private*/ static int      msg_silent;                 /* don't print messages */
    /*private*/ static int      emsg_silent;                /* don't print error messages */
    /*private*/ static boolean  cmd_silent;                 /* don't echo the command line */

    /*private*/ static int      swap_exists_action = SEA_NONE;  /* For dialog when swap file already exists. */
    /*private*/ static boolean  swap_exists_did_quit;           /* Selected "quit" at the dialog. */

    /*private*/ static Bytes    ioBuff;             /* sprintf's are done in this buffer, size is IOSIZE */
    /*private*/ static Bytes    nameBuff;           /* file names are expanded in this buffer, size is MAXPATHL */
    /*private*/ static Bytes    msg_buf = new Bytes(MSG_BUF_LEN); /* small buffer for messages */

    /*private*/ static int      redrawingDisabled;          /* When non-zero, postpone redrawing. */

    /*private*/ static boolean  readonlymode;               /* set to true for "view" */

    /*private*/ static typebuf_C typebuf = new typebuf_C(); /* typeahead buffer */
    /*private*/ static int      ex_normal_busy;             /* recursiveness of ex_normal() */
    /*private*/ static int      ex_normal_lock;             /* forbid use of ex_normal() */
    /*private*/ static boolean  ignore_script;              /* ignore script input */
    /*private*/ static boolean  stop_insert_mode;           /* for ":stopinsert" and 'insertmode' */

    /*private*/ static boolean  keyTyped;                   /* true if user typed current char */
    /*private*/ static boolean  keyStuffed;                 /* true if current char from stuffbuf */
    /*private*/ static int      maptick;                    /* tick for each non-mapped char */

    /*private*/ static byte[]   chartab = new byte[256];    /* table used in charset.c; see init_chartab() */

    /*private*/ static int      must_redraw;                /* type of redraw necessary */
    /*private*/ static boolean  skip_redraw;                /* skip redraw once */
    /*private*/ static boolean  do_redraw;                  /* extra redraw once */

    /*private*/ static boolean  need_highlight_changed = true;

    /*private*/ static final int NSCRIPT = 15;
    /*private*/ static file_C[] scriptin = new file_C[NSCRIPT]; /* streams to read script from */
    /*private*/ static int      curscript;                  /* index in scriptin[] */
    /*private*/ static file_C   scriptout;                  /* stream to write script to */
    /*private*/ static int      read_cmd_fd;                /* fd to read commands from */

    /* volatile because it is used in signal handler catch_sigint(). */
    /*private*/ static /*volatile*/transient boolean got_int; /* set to true when interrupt signal occurred */

    /*private*/ static boolean  termcap_active;             /* set by starttermcap() */
    /*private*/ static int      cur_tmode = TMODE_COOK;     /* input terminal mode */
    /*private*/ static boolean  bangredo;                   /* set to true with ! command */
    /*private*/ static int      searchcmdlen;               /* length of previous search cmd */
    /*private*/ static int      reg_do_extmatch;            /* Used when compiling regexp:
                                                         * REX_SET to allow \z\(...\),
                                                         * REX_USE to allow \z\1 et al. */
    /*private*/ static reg_extmatch_C re_extmatch_in;       /* Used by vim_regexec():
                                                         * strings for \z\1...\z\9 */
    /*private*/ static reg_extmatch_C re_extmatch_out;      /* Set by vim_regexec()
                                                         * to store \z\(...\) matches */

    /*private*/ static boolean  undo_off;                   /* undo switched off for now */
    /*private*/ static int      global_busy;                /* set when :global is executing */
    /*private*/ static boolean  listcmd_busy;               /* set when :argdo, :windo or :bufdo is executing */
    /*private*/ static boolean  need_start_insertmode;      /* start insert mode soon */
    /*private*/ static Bytes    last_cmdline;               /* last command line (for ":) */
    /*private*/ static Bytes    repeat_cmdline;             /* command line for "." */
    /*private*/ static Bytes    new_last_cmdline;           /* new value for "last_cmdline" */
    /*private*/ static Bytes    autocmd_fname;              /* fname for <afile> on cmdline */
    /*private*/ static boolean  autocmd_fname_full;         /* "autocmd_fname" is full path */
    /*private*/ static int      autocmd_bufnr;              /* fnum for <abuf> on cmdline */
    /*private*/ static Bytes    autocmd_match;              /* name for <amatch> on cmdline */
    /*private*/ static boolean  did_cursorhold;             /* set when CursorHold t'gerd */
    /*private*/ static pos_C    last_cursormoved = new pos_C(); /* for CursorMoved event */
    /*private*/ static int      last_changedtick;           /* for TextChanged event */
    /*private*/ static buffer_C last_changedtick_buf;

    /*private*/ static int      postponed_split;            /* for CTRL-W CTRL-] command */
    /*private*/ static int      postponed_split_flags;      /* args for win_split() */
    /*private*/ static int      postponed_split_tab;        /* cmdmod.tab */
    /*private*/ static int      replace_offset;             /* offset for replace_push() */

    /*private*/ static Bytes    escape_chars = u8(" \t\\\"|"); /* need backslash in cmd line */

    /*
     * When a string option is null (which only happens in out-of-memory situations),
     * it is set to EMPTY_OPTION to avoid having to check for null everywhere.
     */
    /*private*/ static final Bytes EMPTY_OPTION = u8("");

    /*private*/ static boolean  redir_off;          /* no redirection for a moment */
    /*private*/ static file_C   redir_fd;           /* message redirection file */
    /*private*/ static int      redir_reg;          /* message redirection register */
    /*private*/ static boolean  redir_vname;        /* message redirection variable */

    /*private*/ static boolean[] breakat_flags = new boolean[256]; /* which characters are in 'breakat' */

    /* Characters from 'fillchars' option. */
    /*private*/ static int[]
        fill_stl    = { ' ' },
        fill_stlnc  = { ' ' },
        fill_vert   = { ' ' },
        fill_fold   = { '-' },
        fill_diff   = { '-' };

    /* Characters from 'listchars' option. */
    /*private*/ static int[]
        lcs_eol     = { '$' },
        lcs_ext     = { NUL },
        lcs_prec    = { NUL },
        lcs_nbsp    = { NUL },
        lcs_tab1    = { NUL },
        lcs_tab2    = { NUL },
        lcs_trail   = { NUL },
        lcs_conceal = { ' ' };

    /* Whether 'keymodel' contains "stopsel" and "startsel". */
    /*private*/ static boolean  km_stopsel;
    /*private*/ static boolean  km_startsel;

    /*private*/ static int      cedit_key = -1;         /* key value of 'cedit' option */
    /*private*/ static int      cmdwin_type;            /* type of cmdline window or 0 */
    /*private*/ static int      cmdwin_result;          /* result of cmdline window or 0 */

    /*private*/ static Bytes    no_lines_msg = u8("--No lines in buffer--");

    /*
     * When ":global" is used to number of substitutions and changed lines is
     * accumulated until it's finished.
     */
    /*private*/ static long     sub_nsubs;              /* total number of substitutions */
    /*private*/ static long     sub_nlines;             /* total number of lines changed */

    /* table to store parsed 'wildmode' */
    /*private*/ static byte[]   wim_flags = new byte[4];

    /* don't use 'hlsearch' temporarily */
    /*private*/ static boolean  no_hlsearch;

    /*private*/ static boolean  typebuf_was_filled;     /* received text from client or from feedkeys() */

    /*private*/ static boolean  term_is_xterm;          /* xterm-like 'term' */

    /* Set to true when an operator is being executed with virtual editing,
     * MAYBE when no operator is being executed, false otherwise. */
    /*private*/ static /*MAYBEAN*/int  virtual_op = MAYBE;

    /* Display tick, incremented for each call to update_screen(). */
    /*private*/ static short            display_tick;

    /* Set when the cursor line needs to be redrawn. */
    /*private*/ static boolean          need_cursor_line_redraw;

    /*
     * The error messages that can be shared are included here.
     * Excluded are errors that are only used once and debugging messages.
     */
    /*private*/ static final Bytes
        e_abort           = u8("E470: Command aborted"),
        e_argreq          = u8("E471: Argument required"),
        e_backslash       = u8("E10: \\ should be followed by /, ? or &"),
        e_cmdwin          = u8("E11: Invalid in command-line window; <CR> executes, CTRL-C quits"),
        e_curdir          = u8("E12: Command not allowed from exrc/vimrc in current dir"),
        e_endif           = u8("E171: Missing :endif"),
        e_endtry          = u8("E600: Missing :endtry"),
        e_endwhile        = u8("E170: Missing :endwhile"),
        e_endfor          = u8("E170: Missing :endfor"),
        e_while           = u8("E588: :endwhile without :while"),
        e_for             = u8("E588: :endfor without :for"),
        e_exists          = u8("E13: File exists (add ! to override)"),
        e_internal        = u8("E473: Internal error"),
        e_interr          = u8("Interrupted"),
        e_invaddr         = u8("E14: Invalid address"),
        e_invarg          = u8("E474: Invalid argument"),
        e_invarg2         = u8("E475: Invalid argument: %s"),
        e_invexpr2        = u8("E15: Invalid expression: %s"),
        e_invrange        = u8("E16: Invalid range"),
        e_invcmd          = u8("E476: Invalid command"),
        e_isadir2         = u8("E17: \"%s\" is a directory"),
        e_markinval       = u8("E19: Mark has invalid line number"),
        e_marknotset      = u8("E20: Mark not set"),
        e_modifiable      = u8("E21: Cannot make changes, 'modifiable' is off"),
        e_nesting         = u8("E22: Scripts nested too deep"),
        e_noalt           = u8("E23: No alternate file"),
        e_noabbr          = u8("E24: No such abbreviation"),
        e_nobang          = u8("E477: No ! allowed"),
        e_nogroup         = u8("E28: No such highlight group name: %s"),
        e_noinstext       = u8("E29: No inserted text yet"),
        e_nolastcmd       = u8("E30: No previous command line"),
        e_nomap           = u8("E31: No such mapping"),
        e_nomatch         = u8("E479: No match"),
        e_nomatch2        = u8("E480: No match: %s"),
        e_noname          = u8("E32: No file name"),
        e_nopresub        = u8("E33: No previous substitute regular expression"),
        e_noprev          = u8("E34: No previous command"),
        e_noprevre        = u8("E35: No previous regular expression"),
        e_norange         = u8("E481: No range allowed"),
        e_noroom          = u8("E36: Not enough room"),
        e_notcreate       = u8("E482: Can't create file %s"),
        e_notmp           = u8("E483: Can't get temp file name"),
        e_notopen         = u8("E484: Can't open file %s"),
        e_nowrtmsg        = u8("E37: No write since last change (add ! to override)"),
        e_nowrtmsg_nobang = u8("E37: No write since last change"),
        e_null            = u8("E38: Null argument"),
        e_number_exp      = u8("E39: Number expected"),
        e_patnotf2        = u8("E486: Pattern not found: %s"),
        e_positive        = u8("E487: Argument must be positive"),
        e_prev_dir        = u8("E459: Cannot go back to previous directory"),
        e_re_damg         = u8("E43: Damaged match string"),
        e_re_corr         = u8("E44: Corrupted regexp program"),
        e_readonly        = u8("E45: 'readonly' option is set (add ! to override)"),
        e_readonlyvar     = u8("E46: Cannot change read-only variable \"%s\""),
        e_readonlysbx     = u8("E794: Cannot set variable in the sandbox: \"%s\""),
        e_sandbox         = u8("E48: Not allowed in sandbox"),
        e_secure          = u8("E523: Not allowed here"),
        e_screenmode      = u8("E359: Screen mode setting not supported"),
        e_scroll          = u8("E49: Invalid scroll size"),
        e_toocompl        = u8("E74: Command too complex"),
        e_longname        = u8("E75: Name too long"),
        e_toomsbra        = u8("E76: Too many ["),
        e_toomany         = u8("E77: Too many file names"),
        e_trailing        = u8("E488: Trailing characters"),
        e_umark           = u8("E78: Unknown mark"),
        e_winheight       = u8("E591: 'winheight' cannot be smaller than 'winminheight'"),
        e_winwidth        = u8("E592: 'winwidth' cannot be smaller than 'winminwidth'"),
        e_write           = u8("E80: Error while writing"),
        e_zerocount       = u8("Zero count"),
        e_usingsid        = u8("E81: Using <SID> not in a script context"),
        e_intern2         = u8("E685: Internal error: %s"),
        e_maxmempat       = u8("E363: pattern uses more memory than 'maxmempattern'"),
        e_emptybuf        = u8("E749: empty buffer"),
        e_nobufnr         = u8("E86: Buffer %ld does not exist"),

        e_invalpat        = u8("E682: Invalid search pattern or delimiter"),
        e_bufloaded       = u8("E139: File is loaded in another buffer");

    /* For undo we need to know the lowest time possible. */
    /*private*/ static long starttime;

    /* ----------------------------------------------------------------------- */

    /* values for vim_handle_signal() that are not a signal */
    /*private*/ static final int SIGNAL_BLOCK    = -1;
    /*private*/ static final int SIGNAL_UNBLOCK  = -2;

    /* behavior for bad character, "++bad=" argument */
    /*private*/ static final byte BAD_REPLACE     = '?';     /* replace it with '?' (default) */
    /*private*/ static final int BAD_KEEP        = -1;      /* leave it */
    /*private*/ static final int BAD_DROP        = -2;      /* erase it */

    /* flags for buf_freeall() */
    /*private*/ static final int BFA_DEL         = 1;       /* buffer is going to be deleted */
    /*private*/ static final int BFA_WIPE        = 2;       /* buffer is going to be wiped out */
    /*private*/ static final int BFA_KEEP_UNDO   = 4;       /* do not free undo information */

    /* direction for nv_mousescroll() and ins_mousescroll() */
    /*private*/ static final int MSCR_DOWN       = 0;       /* DOWN must be false */
    /*private*/ static final int MSCR_UP         = 1;
    /*private*/ static final int MSCR_LEFT       = -1;
    /*private*/ static final int MSCR_RIGHT      = -2;

    /*private*/ static final int KEYLEN_PART_KEY = -1;      /* keylen value for incomplete key-code */
    /*private*/ static final int KEYLEN_PART_MAP = -2;      /* keylen value for incomplete mapping */
    /*private*/ static final int KEYLEN_REMOVED  = 9999;    /* keylen value for removed sequence */

    /* Option types for various functions in option.c. */
    /*private*/ static final int SREQ_GLOBAL     = 0;       /* Request global option */
    /*private*/ static final int SREQ_WIN        = 1;       /* Request window-local option */
    /*private*/ static final int SREQ_BUF        = 2;       /* Request buffer-local option */

    /* Flags for get_reg_contents(). */
    /*private*/ static final int GREG_NO_EXPR    = 1;       /* Do not allow expression register */
    /*private*/ static final int GREG_EXPR_SRC   = 2;       /* Return expression itself for "=" register */
    /*private*/ static final int GREG_LIST       = 4;       /* Return list */

    /* Character used as separated in autoload function/variable names. */
    /*private*/ static final byte AUTOLOAD_CHAR = '#';

    /*
     * Position comparisons
     */
    /*private*/ static boolean ltpos(pos_C a, pos_C b)
    {
        return (a.lnum != b.lnum) ? (a.lnum < b.lnum) : (a.col != b.col) ? (a.col < b.col) : (a.coladd < b.coladd);
    }

    /*private*/ static boolean eqpos(pos_C a, pos_C b)
    {
        return (a.lnum == b.lnum) && (a.col == b.col) && (a.coladd == b.coladd);
    }

    /*private*/ static boolean ltoreq(pos_C a, pos_C b)
    {
        return ltpos(a, b) || eqpos(a, b);
    }

    /*private*/ static void clearpos(pos_C a)
    {
        a.lnum = 0;
        a.col = 0;
        a.coladd = 0;
    }

    /*
     * lineempty() - return true if the line is empty
     */
    /*private*/ static boolean lineempty(long lnum)
    {
        return (ml_get(lnum).at(0) == NUL);
    }

    /*
     * bufempty() - return true if the current buffer is empty
     */
    /*private*/ static boolean bufempty()
    {
        return (curbuf.b_ml.ml_line_count == 1 && ml_get(1).at(0) == NUL);
    }

    /* ----------------------------------------------------------------------- */
}
