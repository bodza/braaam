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
public class VimG
{
    /* Maximum number of commands from + or -c arguments. */
    /*private*/ static final int MAX_ARG_CMDS = 10;

    /* values for "window_layout" */
    /*private*/ static final int WIN_HOR     = 1;       /* "-o" horizontally split windows */
    /*private*/ static final int WIN_VER     = 2;       /* "-O" vertically split windows */
    /*private*/ static final int WIN_TABS    = 3;       /* "-p" windows on tab pages */

    /* Struct for various parameters passed between main() and other functions. */
    /*private*/ static final class mparm_C
    {
        int         argc;
        Bytes[]   argv;

        Bytes       use_vimrc;                  /* vimrc from -u argument */

        int         n_commands;                 /* no. of commands from + or -c */
        Bytes[]   commands;                   /* commands from + or -c arg. */
        int         n_pre_commands;             /* no. of commands from --cmd */
        Bytes[]   pre_commands;               /* commands from --cmd argument */

        int         edit_type;                  /* type of editing to do */

        boolean     stdout_isatty;              /* is stdout a terminal? */
        Bytes       term;                       /* specified terminal name */
        int         use_debug_break_level;
        int         window_count;               /* number of windows to use */
        int         window_layout;              /* 0, WIN_HOR, WIN_VER or WIN_TABS */

        /*private*/ mparm_C()
        {
            commands = new Bytes[MAX_ARG_CMDS];
            pre_commands = new Bytes[MAX_ARG_CMDS];
        }
    }

    /*
     * Values for edit_type.
     */
    /*private*/ static final int EDIT_NONE   = 0;       /* no edit type yet */
    /*private*/ static final int EDIT_FILE   = 1;       /* file name argument[s] given, use argument list */
    /*private*/ static final int EDIT_STDIN  = 2;       /* read file from stdin */

    /*
     * Different types of error messages.
     */
    /*private*/ static final int ME_UNKNOWN_OPTION       = 0;
    /*private*/ static final int ME_TOO_MANY_ARGS        = 1;
    /*private*/ static final int ME_ARG_MISSING          = 2;
    /*private*/ static final int ME_GARBAGE              = 3;
    /*private*/ static final int ME_EXTRA_CMD            = 4;
    /*private*/ static final int ME_INVALID_ARG          = 5;

    /*private*/ static Bytes[] main_errors =
    {
        u8("Unknown option argument"),
        u8("Too many edit arguments"),
        u8("Argument missing after"),
        u8("Garbage after option argument"),
        u8("Too many \"+command\", \"-c command\" or \"--cmd command\" arguments"),
        u8("Invalid argument for"),
    };

    public static int _main(int argc, Bytes[] argv)
    {
        starttime = libC._time();

        mparm_C params = new mparm_C();
        /* Many variables are in "params" so that we can pass them to invoked
         * functions without a lot of arguments.  "argc" and "argv" are also
         * copied, so that they can be changed. */
        params.argc = argc;
        params.argv = argv;
        params.use_debug_break_level = -1;
        params.window_count = -1;

        eval_init();                            /* init global variables */

        /* Init the table of Normal mode commands. */
        init_normal_cmds();

        /*
         * Allocate space for the generic buffers (needed for set_init_1() and emsg2()).
         */
        if ((ioBuff = new Bytes(IOSIZE)) == null || (nameBuff = new Bytes(MAXPATHL)) == null)
            mch_exit(0);

        clip_init(false);                       /* initialise clipboard stuff */

        /*
         * Check if we have an interactive window.
         */
        params.stdout_isatty = mch_output_isatty();

        /*
         * Allocate the first window and buffer.
         * Can't do anything without it, exit when it fails.
         */
        if (win_alloc_first() == false)
            mch_exit(0);

        init_yank();                            /* init yank buffers */

        /*
         * Set the default values for the options.
         */
        set_init_1();

        /*
         * Figure out the way to work from the command name argv[0].
         * "vimdiff" starts diff mode, "rvim" sets "restricted", etc.
         */
        parse_command_name(params);

        /*
         * Process the command line arguments.
         * File names are put in the global argument list "global_alist".
         */
        command_line_scan(params);

        Bytes fname = null;                /* file name from command line */
        if (0 < global_alist.al_ga.ga_len)
            fname = alist_name(global_alist.al_ga.ga_data[0]);

        /* Don't redraw until much later. */
        redrawingDisabled++;

        /*
         * mch_init() sets up the terminal (window) for use.
         * This must be done after resetting full_screen, otherwise it may move the cursor (MSDOS).
         * Note that we may use mch_exit() before mch_init()!
         */
        mch_init();

        /*
         * Print a warning if stdout is not a terminal.
         */
        check_tty(params);

        /* This message comes before term inits,
         * but after setting "silent_mode" when the input is not a tty. */
        if (1 < global_alist.al_ga.ga_len && !silent_mode)
            libC.fprintf(stdout, u8("%d files to edit\n"), global_alist.al_ga.ga_len);

        if (!silent_mode)
        {
            termcapinit(params.term);           /* set terminal name and get terminal
                                                 * capabilities (will set full_screen) */
            screen_start();                     /* don't know where cursor is now */
        }

        /*
         * Set the default values for the options that use Rows and Columns.
         */
        ui_get_shellsize();                     /* inits Rows and Columns */
        win_init_size();

        cmdline_row = (int)(Rows[0] - p_ch[0]);
        msg_row = cmdline_row;
        screenalloc(false);                     /* allocate screen buffers */
        set_init_2();

        msg_scroll = true;
        no_wait_return = TRUE;

        init_mappings();                        /* set up initial mappings */

        init_highlight(true, false);            /* set the default highlight groups */

        /* Set the break level after the terminal is initialized. */
        debug_break_level = params.use_debug_break_level;

        /* Execute --cmd arguments. */
        exe_pre_commands(params);

        /* Source startup scripts. */
        source_startup_scripts(params);

        /* It's better to make v:oldfiles an empty list than null. */
        if (get_vim_var_list(VV_OLDFILES) == null)
            set_vim_var_list(VV_OLDFILES, new list_C());

        /*
         * Start putting things on the screen.
         * Scroll screen down before drawing over it
         * Clear screen now, so file message will not be cleared.
         */
        starting = NO_BUFFERS;
        no_wait_return = FALSE;
        if (exmode_active == 0)
            msg_scroll = false;

        /*
         * If "-" argument given: Read file from stdin.
         * Do this before starting Raw mode, because it may change things that the
         * writing end of the pipe doesn't like, e.g., in case stdin and stderr
         * are the same terminal: "cat | vim -".
         * Using autocommands here may cause trouble...
         */
        if (params.edit_type == EDIT_STDIN)
            read_stdin();

        /* When switching screens and something caused a message from a vimrc script,
         * need to output an extra newline on exit. */
        if ((did_emsg || msg_didout) && T_TI[0].at(0) != NUL)
            newline_on_exit = true;

        /*
         * When done something that is not allowed or error message call wait_return.
         * This must be done before starttermcap(), because it may switch to another screen.
         * It must be done after settmode(TMODE_RAW), because we want to react on a single key stroke.
         * Call settmode and starttermcap here, so the T_KS and T_TI may be defined
         * by termcapinit and redefined in .exrc.
         */
        settmode(TMODE_RAW);

        if (need_wait_return || msg_didany)
        {
            wait_return(TRUE);
        }

        starttermcap();                         /* start termcap if not done by wait_return() */
        may_req_ambiguous_char_width();

        setmouse();                             /* may start using the mouse */
        if (scroll_region)
            scroll_region_reset();              /* in case Rows changed */
        scroll_start();                         /* may scroll the screen to the right position */

        /*
         * Don't clear the screen when starting in Ex mode, unless using the GUI.
         */
        if (exmode_active != 0)
            must_redraw = CLEAR;
        else
            screenclear();                      /* clear screen */

        no_wait_return = TRUE;

        /*
         * Create the requested number of windows and edit buffers in them.
         */
        create_windows(params);

        /* clear v:swapcommand */
        set_vim_var_string(VV_SWAPCOMMAND, null, -1);

        /* Ex starts at last line of the file. */
        if (exmode_active != 0)
            curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;

        apply_autocmds(EVENT_BUFENTER, null, null, false, curbuf);
        setpcmark();

        /*
         * If opened more than one window, start editing files in the other windows.
         */
        edit_buffers(params);

        /*
         * Shorten any of the filenames, but only when absolute.
         */
        shorten_fnames(false);

        /* Execute any "+" and "-c" arguments. */
        if (0 < params.n_commands)
            exe_commands(params);

        redrawingDisabled = 0;
        redraw_all_later(NOT_VALID);
        no_wait_return = FALSE;
        starting = 0;

        /* Requesting the termresponse is postponed until here, so that a "-c q"
         * argument doesn't make it appear in the shell Vim was started from. */
        may_req_termresponse();

        /* start in insert mode */
        if (p_im[0])
            need_start_insertmode = true;

        apply_autocmds(EVENT_VIMENTER, null, null, false, curbuf);

        /* Adjust default register name for "unnamed" in 'clipboard'.  Can only be done
         * after the clipboard is available and all initial commands that may modify
         * the 'clipboard' setting have run; i.e. just before entering the main loop. */
        {
            int default_regname = 0;
            default_regname = adjust_clip_reg(default_regname);
            set_reg_var(default_regname);
        }

        /* If ":startinsert" command used, stuff a dummy command to be
         * able to call normal_cmd(), which will then start Insert mode. */
        if (restart_edit != 0)
            stuffcharReadbuff(K_NOP);

        /*
         * Call the main command loop.  This never returns.
         */
        main_loop(false, false);

        return 0;
    }

    /*
     * Main loop: Execute Normal mode commands until exiting Vim.
     * Also used to handle commands in the command-line window, until the window is closed.
     * Also used to handle ":visual" command after ":global": execute Normal mode commands,
     * return when entering Ex mode.  "noexmode" is true then.
     */
    /*private*/ static void main_loop(boolean cmdwin, boolean noexmode)
        /* cmdwin: true when working in the command-line window */
        /* noexmode: true when return on entering Ex mode */
    {
        /*volatile*//*transient */boolean previous_got_int = false;      /* "got_int" was true */

        long conceal_old_cursor_line = 0;
        long conceal_new_cursor_line = 0;
        boolean conceal_update_lines = false;

        oparg_C oa = new oparg_C();                     /* operator arguments */

        while (!cmdwin || cmdwin_result == 0)
        {
            if (stuff_empty())
            {
                did_check_timestamps = false;
                if (need_check_timestamps)
                    check_timestamps(false);
                if (need_wait_return)                   /* if wait_return still needed ... */
                    wait_return(FALSE);                 /* ... call it now */
                if (need_start_insertmode && goto_im() && !VIsual_active)
                {
                    need_start_insertmode = false;
                    stuffReadbuff(u8("i"));                 /* start insert mode next */
                    /* skip the fileinfo message now,
                     * because it would be shown after insert mode finishes! */
                    need_fileinfo = false;
                }
            }

            /* Reset "got_int" now that we got back to the main loop.  Except when inside
             * a ":g/pat/cmd" command, then the "got_int" needs to abort the ":g" command.
             * For ":g/pat/vi" we reset "got_int" when used once.  When used
             * a second time we go back to Ex mode and abort the ":g" command. */
            if (got_int)
            {
                if (noexmode && global_busy != 0 && exmode_active == 0 && previous_got_int)
                {
                    /* Typed two CTRL-C in a row: go back to ex mode as if "Q" was used
                     * and keep "got_int" set, so that it aborts ":g". */
                    exmode_active = EXMODE_NORMAL;
                    State = NORMAL;
                }
                else if (global_busy == 0 || exmode_active == 0)
                {
                    if (!quit_more)
                        vgetc();                /* flush all buffers */
                    got_int = false;
                }
                previous_got_int = true;
            }
            else
                previous_got_int = false;

            if (exmode_active == 0)
                msg_scroll = false;
            quit_more = false;

            /*
             * If skip redraw is set (for ":" in wait_return()), don't redraw now.
             * If there is nothing in the stuff_buffer or do_redraw is true, update cursor and redraw.
             */
            if (skip_redraw || exmode_active != 0)
                skip_redraw = false;
            else if (do_redraw || stuff_empty())
            {
                /* Trigger CursorMoved if the cursor moved. */
                if (!finish_op && (has_cursormoved() || 0 < curwin.w_onebuf_opt.wo_cole[0])
                     && !eqpos(last_cursormoved, curwin.w_cursor))
                {
                    if (has_cursormoved())
                        apply_autocmds(EVENT_CURSORMOVED, null, null, false, curbuf);
                    if (0 < curwin.w_onebuf_opt.wo_cole[0])
                    {
                        conceal_old_cursor_line = last_cursormoved.lnum;
                        conceal_new_cursor_line = curwin.w_cursor.lnum;
                        conceal_update_lines = true;
                    }
                    COPY_pos(last_cursormoved, curwin.w_cursor);
                }

                /* Trigger TextChanged if b_changedtick differs. */
                if (!finish_op && has_textchanged() && last_changedtick != curbuf.b_changedtick)
                {
                    if (last_changedtick_buf == curbuf)
                        apply_autocmds(EVENT_TEXTCHANGED, null, null, false, curbuf);
                    last_changedtick_buf = curbuf;
                    last_changedtick = curbuf.b_changedtick;
                }

                /*
                 * Before redrawing, make sure w_topline is correct, and w_leftcol
                 * if lines don't wrap, and w_skipcol if lines wrap.
                 */
                update_topline();
                validate_cursor();

                if (VIsual_active)
                    update_curbuf(INVERTED); /* update inverted part */
                else if (must_redraw != 0)
                    update_screen(0);
                else if (redraw_cmdline || clear_cmdline)
                    showmode();
                redraw_statuslines();
                /* display message after redraw */
                if (keep_msg != null)
                {
                    /* msg_attr_keep() will set "keep_msg" to null, must free the string here.
                     * Don't reset "keep_msg", msg_attr_keep() uses it to check for duplicates. */
                    msg_attr(keep_msg, keep_msg_attr);
                }
                if (need_fileinfo)          /* show file info after redraw */
                {
                    fileinfo(0, false);
                    need_fileinfo = false;
                }

                emsg_on_display = false;    /* can delete error message now */
                did_emsg = false;
                msg_didany = false;         /* reset lines_left in msg_start() */
                may_clear_sb_text();        /* clear scroll-back text on next msg */
                showruler(false);

                if (conceal_update_lines
                        && (conceal_old_cursor_line != conceal_new_cursor_line
                            || conceal_cursor_line(curwin)
                            || need_cursor_line_redraw))
                {
                    if (conceal_old_cursor_line != conceal_new_cursor_line
                            && conceal_old_cursor_line <= curbuf.b_ml.ml_line_count)
                        update_single_line(curwin, conceal_old_cursor_line);
                    update_single_line(curwin, conceal_new_cursor_line);
                    curwin.w_valid &= ~VALID_CROW;
                }
                setcursor();
                cursor_on();

                do_redraw = false;
            }

            /*
             * Update w_curswant if w_set_curswant has been set.
             * Postponed until here to avoid computing w_virtcol too often.
             */
            update_curswant();

            /*
             * If we're invoked as ex, do a round of ex commands.
             * Otherwise, get and execute a normal mode command.
             */
            if (exmode_active != 0)
            {
                if (noexmode)   /* End of ":global/path/visual" commands */
                    return;
                do_exmode(exmode_active == EXMODE_VIM);
            }
            else
                normal_cmd(oa, true);
        }
    }

    /* Exit properly. */
    /*private*/ static void getout(int exitval)
    {
        exiting = true;

        /* When running in Ex mode an error causes us to exit with a non-zero exit code.
         * POSIX requires this, although it's not 100% clear from the standard. */
        if (exmode_active != 0)
            exitval += ex_exitval;

        /* Position the cursor on the last screen line, below all the text. */
        windgoto((int)Rows[0] - 1, 0);

        if (get_vim_var_nr(VV_DYING) <= 1)
        {
            /* Trigger BufWinLeave for all windows, but only once per buffer. */
            tabpage_C next_tp;
            for (tabpage_C tp = first_tabpage; tp != null; tp = next_tp)
            {
                next_tp = tp.tp_next;
                for (window_C wp = (tp == curtab) ? firstwin : tp.tp_firstwin; wp != null; wp = wp.w_next)
                {
                    if (wp.w_buffer == null)
                        /* Autocmd must have close the buffer already, skip. */
                        continue;
                    buffer_C buf = wp.w_buffer;
                    if (buf.b_changedtick != -1)
                    {
                        apply_autocmds(EVENT_BUFWINLEAVE, buf.b_fname, buf.b_fname, false, buf);
                        buf.b_changedtick = -1;     /* note that we did it already */
                        /* start all over, autocommands may mess up the lists */
                        next_tp = first_tabpage;
                        break;
                    }
                }
            }

            /* Trigger BufUnload for buffers that are loaded. */
            for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
                if (buf.b_ml.ml_mfp != null)
                {
                    apply_autocmds(EVENT_BUFUNLOAD, buf.b_fname, buf.b_fname, false, buf);
                    if (!buf_valid(buf))    /* autocmd may delete the buffer */
                        break;
                }
            apply_autocmds(EVENT_VIMLEAVEPRE, null, null, false, curbuf);
        }

        if (get_vim_var_nr(VV_DYING) <= 1)
            apply_autocmds(EVENT_VIMLEAVE, null, null, false, curbuf);

        if (did_emsg)
        {
            /* give the user a chance to read the (error) message */
            no_wait_return = FALSE;
            wait_return(FALSE);
        }

        /* Position the cursor again, the autocommands may have moved it. */
        windgoto((int)Rows[0] - 1, 0);

        mch_exit(exitval);
    }

    /*
     * Get a (optional) count for a Vim argument.
     */
    /*private*/ static int get_number_arg(Bytes p, int[] idx, int def)
        /* p: pointer to argument */
        /* idx: index in argument, is incremented */
        /* def: default value */
    {
        if (asc_isdigit(p.at(idx[0])))
        {
            def = libC.atoi(p.plus(idx[0]));
            while (asc_isdigit(p.at(idx[0])))
                idx[0] += 1;
        }
        return def;
    }

    /*
     * Check for: [r][e][vi|vim|view][ex[im]]
     * If the executable name starts with "r" we disable shell commands.
     * If the next character is "e" we run in Easy mode.
     * If the next characters are "view" we start in readonly mode.
     * If the next characters are "ex" we start in Ex mode.  If it's followed
     * by "im" use improved Ex mode.
     */
    /*private*/ static void parse_command_name(mparm_C parmp)
    {
        Bytes s = gettail(parmp.argv[0]);

        if (asc_tolower(s.at(0)) == 'r')
        {
            restricted = true;
            s = s.plus(1);
        }

        if (STRNCASECMP(s, u8("view"), 4) == 0)
        {
            readonlymode = true;
            curbuf.b_p_ro[0] = true;
            s = s.plus(4);
        }
        else if (STRNCASECMP(s, u8("vim"), 3) == 0)
            s = s.plus(3);

        if (STRNCASECMP(s, u8("ex"), 2) == 0)
        {
            if (STRNCASECMP(s.plus(2), u8("im"), 2) == 0)
                exmode_active = EXMODE_VIM;
            else
                exmode_active = EXMODE_NORMAL;
        }
    }

    /*
     * Scan the command line arguments.
     */
    /*private*/ static void command_line_scan(mparm_C parmp)
    {
        int argc = parmp.argc;
        Bytes[] argv = parmp.argv;
        --argc;
        int i = 1;

        boolean had_minmin = false;     /* found "--" argument */
        int[] ai = { 1 };               /* active option letter is argv[i].at(ai[0]) */

        while (0 < argc)
        {
            /*
             * "+" or "+{number}" or "+/{pat}" or "+{command}" argument.
             */
            if (argv[i].at(0) == (byte)'+' && !had_minmin)
            {
                if (MAX_ARG_CMDS <= parmp.n_commands)
                    mainerr(ME_EXTRA_CMD, null);
                ai[0] = -1;                      /* skip to next argument */
                if (argv[i].at(1) == NUL)
                    parmp.commands[parmp.n_commands++] = u8("$");
                else
                    parmp.commands[parmp.n_commands++] = argv[i].plus(1);
            }

            /*
             * Optional argument.
             */
            else if (argv[i].at(0) == (byte)'-' && !had_minmin)
            {
                boolean want_argument = false;
                int c = argv[i].at(ai[0]++);
                switch (c)
                {
                    case NUL:
                    {
                        if (exmode_active != 0)          /* "ex -" silent mode */
                            silent_mode = true;
                        else                        /* "vim -" read from stdin */
                        {
                            if (parmp.edit_type != EDIT_NONE)
                                mainerr(ME_TOO_MANY_ARGS, argv[i]);
                            parmp.edit_type = EDIT_STDIN;
                            read_cmd_fd = 2;        /* read from stderr instead of stdin */
                        }
                        ai[0] = -1;              /* skip to next argument */
                        break;
                    }

                    case '-':
                    {
                        /* "--" don't take any more option arguments
                         * "--help" give help message
                         * "--cmd <cmd>" execute cmd before vimrc */
                        if (STRCASECMP(argv[i].plus(ai[0]), u8("help")) == 0)
                            usage();
                        else if (STRNCASECMP(argv[i].plus(ai[0]), u8("cmd"), 3) == 0)
                        {
                            want_argument = true;
                            ai[0] += 3;
                        }
                        else
                        {
                            if (argv[i].at(ai[0]) != NUL)
                                mainerr(ME_UNKNOWN_OPTION, argv[i]);
                            had_minmin = true;
                        }
                        if (!want_argument)
                            ai[0] = -1;          /* skip to next argument */
                        break;
                    }

                    case 'b':                       /* "-b" binary mode */
                    {
                        /* Needs to be effective before expanding file names,
                         * because for Win32 this makes us edit a shortcut file itself,
                         * instead of the file it links to. */
                        set_options_bin(curbuf.b_p_bin[0], true, 0);
                        curbuf.b_p_bin[0] = true;         /* binary file I/O */
                        break;
                    }

                    case 'e':                       /* "-e" Ex mode */
                        exmode_active = EXMODE_NORMAL;
                        break;

                    case 'E':                       /* "-E" Improved Ex mode */
                        exmode_active = EXMODE_VIM;
                        break;

                    case 'h':                       /* "-h" give help message */
                        usage();
                        break;

                    case 'l':                       /* "-l" lisp mode, 'lisp' and 'showmatch' on */
                        set_option_value(u8("lisp"), 1L, null, 0);
                        p_sm[0] = true;
                        break;

                    case 'M':                       /* "-M" no changes or writing of files */
                        reset_modifiable();
                        /* FALLTHROUGH */
                    case 'm':                       /* "-m" no writing of files */
                        p_write[0] = false;
                        break;

                    case 'p':                       /* "-p[N]" open N tab pages */
                        /* default is 0: open window for each file */
                        parmp.window_count = get_number_arg(argv[i], ai, 0);
                        parmp.window_layout = WIN_TABS;
                        break;

                    case 'o':                       /* "-o[N]" open N horizontal split windows */
                        /* default is 0: open window for each file */
                        parmp.window_count = get_number_arg(argv[i], ai, 0);
                        parmp.window_layout = WIN_HOR;
                        break;

                    case 'O':                       /* "-O[N]" open N vertical split windows */
                        /* default is 0: open window for each file */
                        parmp.window_count = get_number_arg(argv[i], ai, 0);
                        parmp.window_layout = WIN_VER;
                        break;

                    case 'R':                       /* "-R" readonly mode */
                        readonlymode = true;
                        curbuf.b_p_ro[0] = true;
                        break;

                    case 's':
                        if (exmode_active != 0)          /* "-s" silent (batch) mode */
                            silent_mode = true;
                        else                        /* "-s {scriptin}" read from script file */
                            want_argument = true;
                        break;

                    case 'D':                       /* "-D" debugging */
                        parmp.use_debug_break_level = 9999;
                        break;

                    case 'V':                       /* "-V{N}" verbose level */
                    {
                        /* default is 10: a little bit verbose */
                        p_verbose[0] = get_number_arg(argv[i], ai, 10);
                        if (argv[i].at(ai[0]) != NUL)
                        {
                            set_option_value(u8("verbosefile"), 0L, argv[i].plus(ai[0]), 0);
                            ai[0] = strlen(argv[i]);
                        }
                        break;
                    }

                    case 'v':                       /* "-v" Vi-mode (as if called "vi") */
                        exmode_active = 0;
                        break;

                    case 'w':                       /* "-w{number}" set window height
                                                     * "-w {scriptout}" write to script */
                    {
                        if (asc_isdigit(argv[i].at(ai[0])))
                        {
                            long n = get_number_arg(argv[i], ai, 10);
                            set_option_value(u8("window"), n, null, 0);
                            break;
                        }
                        want_argument = true;
                        break;
                    }

                    case 'Z':                       /* "-Z" restricted mode */
                        restricted = true;
                        break;

                    case 'c':                   /* "-c{command}" or "-c {command}" execute command */
                        if (argv[i].at(ai[0]) != NUL)
                        {
                            if (MAX_ARG_CMDS <= parmp.n_commands)
                                mainerr(ME_EXTRA_CMD, null);
                            parmp.commands[parmp.n_commands++] = argv[i].plus(ai[0]);
                            ai[0] = -1;
                            break;
                        }
                        /* FALLTHROUGH */
                    case 'T':                       /* "-T {terminal}" terminal name */
                    case 'u':                       /* "-u {vimrc}" vim inits file */
                    case 'W':                       /* "-W {scriptout}" overwrite */
                        want_argument = true;
                        break;

                    default:
                        mainerr(ME_UNKNOWN_OPTION, argv[i]);
                        break;
                }

                /*
                 * Handle option arguments with argument.
                 */
                if (want_argument)
                {
                    /*
                     * Check for garbage immediately after the option letter.
                     */
                    if (argv[i].at(ai[0]) != NUL)
                        mainerr(ME_GARBAGE, argv[i]);

                    --argc;
                    if (argc < 1)
                        mainerr_arg_missing(argv[i]);
                    i++;
                    ai[0] = -1;

                    switch (c)
                    {
                        case 'c':                   /* "-c {command}" execute command */
                            if (MAX_ARG_CMDS <= parmp.n_commands)
                                mainerr(ME_EXTRA_CMD, null);
                            parmp.commands[parmp.n_commands++] = argv[i];
                            break;

                        case '-':
                            if (argv[i - 1].at(2) == (byte)'c')
                            {
                                /* "--cmd {command}" execute command */
                                if (MAX_ARG_CMDS <= parmp.n_pre_commands)
                                    mainerr(ME_EXTRA_CMD, null);
                                parmp.pre_commands[parmp.n_pre_commands++] = argv[i];
                            }
                            break;

                        case 's':                   /* "-s {scriptin}" read from script file */
                            if (scriptin[0] != null)
                            {
                                libC.fprintf(stderr, u8("Attempt to open script file again: \"%s %s\"\n"),
                                                                argv[i - 1], argv[i]);
                                mch_exit(2);
                            }
                            if ((scriptin[0] = libC.fopen(argv[i], u8("r"))) == null)
                            {
                                libC.fprintf(stderr, u8("Cannot open for reading: \"%s\"\n"), argv[i]);
                                mch_exit(2);
                            }
                            save_typebuf();
                            break;

                        case 'T':                   /* "-T {terminal}" terminal name */
                            /*
                             * The -T term argument is always available and when
                             * HAVE_TERMLIB is supported it overrides the environment variable TERM.
                             */
                            parmp.term = argv[i];
                            break;

                        case 'u':                   /* "-u {vimrc}" vim inits file */
                            parmp.use_vimrc = argv[i];
                            break;

                        case 'w':                   /* "-w {nr}" 'window' value */
                                                    /* "-w {scriptout}" append to script file */
                            if (asc_isdigit(argv[i].at(0)))
                            {
                                ai[0] = 0;
                                long n = get_number_arg(argv[i], ai, 10);
                                set_option_value(u8("window"), n, null, 0);
                                ai[0] = -1;
                                break;
                            }
                            /* FALLTHROUGH */
                        case 'W':                   /* "-W {scriptout}" overwrite script file */
                            if (scriptout != null)
                            {
                                libC.fprintf(stderr, u8("Attempt to open script file again: \"%s %s\"\n"),
                                                                argv[i - 1], argv[i]);
                                mch_exit(2);
                            }
                            if ((scriptout = libC.fopen(argv[i], (c == 'w') ? u8("a") : u8("w"))) == null)
                            {
                                libC.fprintf(stderr, u8("Cannot open for script output: \"%s\"\n"), argv[i]);
                                mch_exit(2);
                            }
                            break;
                    }
                }
            }

            /*
             * File name argument.
             */
            else
            {
                ai[0] = -1;                      /* skip to next argument */

                /* Check for only one type of editing. */
                if (parmp.edit_type != EDIT_NONE && parmp.edit_type != EDIT_FILE)
                    mainerr(ME_TOO_MANY_ARGS, argv[i]);
                parmp.edit_type = EDIT_FILE;

                /* Add the file to the global argument list. */
                alist_add(global_alist, STRDUP(argv[i]), 2); /* add buffer number now and use curbuf */
            }

            /*
             * If there are no more letters after the current "-", go to next argument.
             * "ai" is set to -1 when the current argument is to be skipped.
             */
            if (ai[0] <= 0 || argv[i].at(ai[0]) == NUL)
            {
                --argc;
                i++;
                ai[0] = 1;
            }
        }

        /* If there is a "+123" or "-c" command, set v:swapcommand to the first one. */
        if (0 < parmp.n_commands)
        {
            Bytes p = new Bytes(strlen(parmp.commands[0]) + 3);

            libC.sprintf(p, u8(":%s\r"), parmp.commands[0]);
            set_vim_var_string(VV_SWAPCOMMAND, p, -1);
        }
    }

    /*
     * Print a warning if stdout is not a terminal.
     * When starting in Ex mode and commands come from a file, set Silent mode.
     */
    /*private*/ static void check_tty(mparm_C parmp)
    {
        boolean input_isatty = mch_input_isatty();  /* is active input a terminal? */

        if (exmode_active != 0)
        {
            if (!input_isatty)
                silent_mode = true;
        }
        else if (!parmp.stdout_isatty || !input_isatty)
        {
            if (!parmp.stdout_isatty)
                libC.fprintf(stderr, u8("Vim: Warning: Output is not to a terminal\n"));
            if (!input_isatty)
                libC.fprintf(stderr, u8("Vim: Warning: Input is not from a terminal\n"));
            out_flush();
            if (scriptin[0] == null)
                ui_delay(2000L, true);
        }
    }

    /*
     * Read text from stdin.
     */
    /*private*/ static void read_stdin()
    {
        /* When getting the ATTENTION prompt here, use a dialog. */
        swap_exists_action = SEA_DIALOG;
        no_wait_return = TRUE;
        boolean b = msg_didany;
        set_buflisted(true);
        open_buffer(true, null, 0);     /* create memfile and read file */
        no_wait_return = FALSE;
        msg_didany = b;
        check_swap_exists_action();
        /*
         * Close stdin and dup it from stderr.
         * Required for GPM to work properly, and for running external commands.
         * Is there any other system that cannot do this?
         */
        libc.close(0);
        libc.dup(2);
    }

    /*
     * Create the requested number of windows and edit buffers in them.
     */
    /*private*/ static void create_windows(mparm_C parmp)
    {
        int done = 0;

        /*
         * Create the number of windows that was requested.
         */
        if (parmp.window_count == -1)   /* was not set */
            parmp.window_count = 1;
        if (parmp.window_count == 0)
            parmp.window_count = global_alist.al_ga.ga_len;

        if (1 < parmp.window_count)
        {
            /* Don't change the windows if there was a command in .vimrc that already split some windows. */
            if (parmp.window_layout == 0)
                parmp.window_layout = WIN_HOR;
            if (parmp.window_layout == WIN_TABS)
            {
                parmp.window_count = make_tabpages(parmp.window_count);
            }
            else if (firstwin.w_next == null)
            {
                parmp.window_count = make_windows(parmp.window_count, parmp.window_layout == WIN_VER);
            }
            else
                parmp.window_count = win_count();
        }
        else
            parmp.window_count = 1;

        /*
         * Open a buffer for windows that don't have one yet.
         * Commands in the .vimrc might have loaded a file or split the window.
         * Watch out for autocommands that delete a window.
         */
        /*
         * Don't execute Win/Buf Enter/Leave autocommands here
         */
        autocmd_no_enter++;
        autocmd_no_leave++;
        boolean dorewind = true;
        while (done++ < 1000)
        {
            if (dorewind)
            {
                if (parmp.window_layout == WIN_TABS)
                    goto_tabpage(1);
                else
                    curwin = firstwin;
            }
            else if (parmp.window_layout == WIN_TABS)
            {
                if (curtab.tp_next == null)
                    break;
                goto_tabpage(0);
            }
            else
            {
                if (curwin.w_next == null)
                    break;
                curwin = curwin.w_next;
            }
            dorewind = false;
            curbuf = curwin.w_buffer;
            if (curbuf.b_ml.ml_mfp == null)
            {
                /* When getting the ATTENTION prompt here, use a dialog. */
                swap_exists_action = SEA_DIALOG;
                set_buflisted(true);

                /* create memfile, read file */
                open_buffer(false, null, 0);

                if (swap_exists_action == SEA_QUIT)
                {
                    if (got_int || only_one_window())
                    {
                        /* abort selected or quit and only one window */
                        did_emsg = false;   /* avoid hit-enter prompt */
                        getout(1);
                    }
                    /* We can't close the window, it would disturb what happens next.
                     * Clear the file name and set the arg index to -1 to delete it later.
                     */
                    setfname(curbuf, null, null, false);
                    curwin.w_arg_idx = -1;
                    swap_exists_action = SEA_NONE;
                }
                else
                    handle_swap_exists(null);
                dorewind = true;                /* start again */
            }
            ui_breakcheck();
            if (got_int)
            {
                vgetc();    /* only break the file loading, not the rest */
                break;
            }
        }
        if (parmp.window_layout == WIN_TABS)
            goto_tabpage(1);
        else
            curwin = firstwin;
        curbuf = curwin.w_buffer;
        --autocmd_no_enter;
        --autocmd_no_leave;
    }

    /*
     * If opened more than one window, start editing files in the other windows.
     * make_windows() has already opened the windows.
     */
    /*private*/ static void edit_buffers(mparm_C parmp)
    {
        boolean advance = true;

        /*
         * Don't execute Win/Buf Enter/Leave autocommands here
         */
        autocmd_no_enter++;
        autocmd_no_leave++;

        /* When w_arg_idx is -1 remove the window (see create_windows()). */
        if (curwin.w_arg_idx == -1)
        {
            win_close(curwin, true);
            advance = false;
        }

        int arg_idx = 1;        /* index in argument list */

        for (int i = 1; i < parmp.window_count; i++)
        {
            /* When w_arg_idx is -1 remove the window (see create_windows()). */
            if (curwin.w_arg_idx == -1)
            {
                arg_idx++;
                win_close(curwin, true);
                advance = false;
                continue;
            }

            if (advance)
            {
                if (parmp.window_layout == WIN_TABS)
                {
                    if (curtab.tp_next == null)     /* just checking */
                        break;
                    goto_tabpage(0);
                }
                else
                {
                    if (curwin.w_next == null)      /* just checking */
                        break;
                    win_enter(curwin.w_next, false);
                }
            }
            advance = true;

            /* Only open the file if there is no file in this window yet
             * (that can happen when .vimrc contains ":sall"). */
            if (curbuf == firstwin.w_buffer || curbuf.b_ffname == null)
            {
                curwin.w_arg_idx = arg_idx;
                /* Edit file from arg list, if there is one.
                 * When "Quit" selected at the ATTENTION prompt close the window. */
                swap_exists_did_quit = false;
                do_ecmd(0, (arg_idx < global_alist.al_ga.ga_len)
                              ? alist_name(global_alist.al_ga.ga_data[arg_idx]) : null,
                              null, null, ECMD_LASTL, ECMD_HIDE, curwin);
                if (swap_exists_did_quit)
                {
                    /* abort or quit selected */
                    if (got_int || only_one_window())
                    {
                        /* abort selected and only one window */
                        did_emsg = false;   /* avoid hit-enter prompt */
                        getout(1);
                    }
                    win_close(curwin, true);
                    advance = false;
                }
                if (arg_idx == global_alist.al_ga.ga_len - 1)
                    arg_had_last = true;
                arg_idx++;
            }
            ui_breakcheck();
            if (got_int)
            {
                vgetc();        /* only break the file loading, not the rest */
                break;
            }
        }

        if (parmp.window_layout == WIN_TABS)
            goto_tabpage(1);
        --autocmd_no_enter;

        /* make the first window the current window */
        window_C win = firstwin;
        win_enter(win, false);

        --autocmd_no_leave;
        if (1 < parmp.window_count && parmp.window_layout != WIN_TABS)
            win_equal(curwin, false, 'b');  /* adjust heights */
    }

    /*
     * Execute the commands from --cmd arguments "cmds[cnt]".
     */
    /*private*/ static void exe_pre_commands(mparm_C parmp)
    {
        Bytes[] cmds = parmp.pre_commands;
        int cnt = parmp.n_pre_commands;

        if (0 < cnt)
        {
            curwin.w_cursor.lnum = 0; /* just in case.. */
            sourcing_name = u8("pre-vimrc command line");
            current_SID = SID_CMDARG;
            for (int i = 0; i < cnt; i++)
                do_cmdline_cmd(cmds[i]);
            sourcing_name = null;
            current_SID = 0;
        }
    }

    /*
     * Execute "+" and "-c" arguments.
     */
    /*private*/ static void exe_commands(mparm_C parmp)
    {
        /*
         * We start commands on line 0, make "vim +/pat file" match a pattern on line 1.
         * But don't move the cursor when an autocommand with g`" was used.
         */
        msg_scroll = true;
        if (curwin.w_cursor.lnum <= 1)
            curwin.w_cursor.lnum = 0;
        sourcing_name = u8("command line");
        current_SID = SID_CARG;

        for (int i = 0; i < parmp.n_commands; i++)
            do_cmdline_cmd(parmp.commands[i]);

        sourcing_name = null;
        current_SID = 0;
        if (curwin.w_cursor.lnum == 0)
            curwin.w_cursor.lnum = 1;

        if (exmode_active == 0)
            msg_scroll = false;
    }

    /*
     * Source startup scripts.
     */
    /*private*/ static void source_startup_scripts(mparm_C parmp)
    {
        /*
         * If -u argument given, use only the initializations from that file and nothing else.
         */
        if (parmp.use_vimrc != null)
        {
            if (STRCMP(parmp.use_vimrc, u8("NONE")) != 0 && STRCMP(parmp.use_vimrc, u8("NORC")) != 0)
            {
                if (do_source(parmp.use_vimrc, false) != true)
                    emsg2(u8("E282: Cannot read from \"%s\""), parmp.use_vimrc);
            }
        }
        else if (!silent_mode)
        {
            /*
             * Read initialization commands from ".vimrc" or ".exrc" in current directory.
             * This is only done if the 'exrc' option is set.
             * Because of security reasons we disallow shell and write commands
             * now, except for unix if the file is owned by the user or 'secure'
             * option has been reset in environment of global ".exrc" or ".vimrc".
             */
            if (p_exrc[0])
            {
                /* If ".vimrc" is not owned by user, set 'secure' mode. */
                if (!file_owned(u8(".vimrc")))
                    secure = p_secure[0] ? 1 : 0;

                if (do_source(u8(".vimrc"), true) == false)
                {
                    /* If ".exrc" is not owned by user, set 'secure' mode. */
                    if (!file_owned(u8(".exrc")))
                        secure = p_secure[0] ? 1 : 0;
                    else
                        secure = 0;

                    do_source(u8(".exrc"), false);
                }
            }
            if (secure == 2)
                need_wait_return = true;
            secure = 0;
        }
    }

    /*
     * Return true if we are certain the user owns the file "fname".
     * Used for ".vimrc" and ".exrc".
     * Use both stat() and lstat() for extra security.
     */
    /*private*/ static boolean file_owned(Bytes fname)
    {
        int uid = libc.getuid();

        stat_C st = new stat_C();
        return (libC.stat(fname, st) == 0 && st.st_uid() == uid
             && libC.lstat(fname, st) == 0 && st.st_uid() == uid);
    }

    /*
     * Give an error message main_errors[n] and exit.
     */
    /*private*/ static void mainerr(int n, Bytes str)
        /* n: one of the ME_ defines */
        /* str: extra argument or null */
    {
        reset_signals();            /* kill us with CTRL-C here, if you like */

        libC.fprintf(stderr, u8("%s\n"), longVersion);
        libC.fprintf(stderr, u8("%s"), main_errors[n]);
        if (str != null)
            libC.fprintf(stderr, u8(": \"%s\""), str);
        libC.fprintf(stderr, u8("\nMore info with: \"vim -h\"\n"));

        mch_exit(1);
    }

    /*private*/ static void mainerr_arg_missing(Bytes str)
    {
        mainerr(ME_ARG_MISSING, str);
    }

    /*
     * print a message with three spaces prepended and '\n' appended.
     */
    /*private*/ static void main_msg(Bytes s)
    {
        libC.fprintf(stdout, u8("   %s\n"), s);
    }

    /*private*/ static Bytes[] __usage =
    {
        u8("[file ..]       edit specified file(s)"),
        u8("-               read text from stdin"),
    };

    /*
     * Print messages for "vim -h" or "vim --help" and exit.
     */
    /*private*/ static void usage()
    {
        reset_signals();            /* kill us with CTRL-C here, if you like */

        libC.fprintf(stdout, u8("%s\n\nusage:"), longVersion);
        for (int i = 0; ; i++)
        {
            libC.fprintf(stdout, u8(" vim [arguments] %s"), __usage[i]);
            if (i == __usage.length - 1)
                break;
            libC.fprintf(stdout, u8("\n   or:"));
        }

        libC.fprintf(stdout, u8("\n\nArguments:\n"));
        main_msg(u8("--\t\t\tOnly file names after this"));
        main_msg(u8("-v\t\t\tVi mode (like \"vi\")"));
        main_msg(u8("-e\t\t\tEx mode (like \"ex\")"));
        main_msg(u8("-E\t\t\tImproved Ex mode"));
        main_msg(u8("-s\t\t\tSilent (batch) mode (only for \"ex\")"));
        main_msg(u8("-R\t\t\tReadonly mode (like \"view\")"));
        main_msg(u8("-Z\t\t\tRestricted mode (like \"rvim\")"));
        main_msg(u8("-m\t\t\tModifications (writing files) not allowed"));
        main_msg(u8("-M\t\t\tModifications in text not allowed"));
        main_msg(u8("-b\t\t\tBinary mode"));
        main_msg(u8("-l\t\t\tLisp mode"));
        main_msg(u8("-V[N][fname]\t\tBe verbose [level N] [log messages to fname]"));
        main_msg(u8("-D\t\t\tDebugging mode"));
        main_msg(u8("-T <terminal>\tSet terminal type to <terminal>"));
        main_msg(u8("-u <vimrc>\t\tUse <vimrc> instead of any .vimrc"));
        main_msg(u8("-p[N]\t\tOpen N tab pages (default: one for each file)"));
        main_msg(u8("-o[N]\t\tOpen N windows (default: one for each file)"));
        main_msg(u8("-O[N]\t\tLike -o but split vertically"));
        main_msg(u8("+\t\t\tStart at end of file"));
        main_msg(u8("+<lnum>\t\tStart at line <lnum>"));
        main_msg(u8("--cmd <command>\tExecute <command> before loading any vimrc file"));
        main_msg(u8("-c <command>\t\tExecute <command> after loading the first file"));
        main_msg(u8("-s <scriptin>\tRead Normal mode commands from file <scriptin>"));
        main_msg(u8("-w <scriptout>\tAppend all typed commands to file <scriptout>"));
        main_msg(u8("-W <scriptout>\tWrite all typed commands to file <scriptout>"));
        main_msg(u8("-h  or  --help\tPrint Help (this message) and exit"));

        mch_exit(0);
    }

    /*
     * Check the result of the ATTENTION dialog:
     * When "Quit" selected, exit Vim.
     * When "Recover" selected, recover the file.
     */
    /*private*/ static void check_swap_exists_action()
    {
        if (swap_exists_action == SEA_QUIT)
            getout(1);
        handle_swap_exists(null);
    }

    /*
     * Show the intro message when not editing a file.
     */
    /*private*/ static void maybe_intro_message()
    {
        if (bufempty()
                && curbuf.b_fname == null
                && firstwin.w_next == null
                && vim_strchr(p_shm[0], SHM_INTRO) == null)
            intro_message();
    }

    /*
     * Give an introductory message about Vim.
     * Only used when starting Vim on an empty file, without a file name.
     */
    /*private*/ static void intro_message()
    {
    }
}
