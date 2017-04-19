#define EXTERN
#include "vim.h"

/* Maximum number of commands from + or -c arguments. */
#define MAX_ARG_CMDS 10

/* values for "window_layout" */
#define WIN_HOR     1       /* "-o" horizontally split windows */
#define WIN_VER     2       /* "-O" vertically split windows */
#define WIN_TABS    3       /* "-p" windows on tab pages */

/* Struct for various parameters passed between main() and other functions. */
typedef struct
{
    int     argc;
    char    **argv;

    int     evim_mode;                      /* started as "evim" */
    char_u  *use_vimrc;                     /* vimrc from -u argument */

    int     n_commands;                     /* no. of commands from + or -c */
    char_u  *commands[MAX_ARG_CMDS];        /* commands from + or -c arg. */
    char_u  cmds_tofree[MAX_ARG_CMDS];      /* commands that need free() */
    int     n_pre_commands;                 /* no. of commands from --cmd */
    char_u  *pre_commands[MAX_ARG_CMDS];    /* commands from --cmd argument */

    int     edit_type;                      /* type of editing to do */

    int     want_full_screen;
    int     stdout_isatty;                  /* is stdout a terminal? */
    char_u  *term;                          /* specified terminal name */
    int     no_swap_file;                   /* "-n" argument used */
    int     use_debug_break_level;
    int     window_count;                   /* number of windows to use */
    int     window_layout;                  /* 0, WIN_HOR, WIN_VER or WIN_TABS */
} mparm_T;

/* Values for edit_type. */
#define EDIT_NONE   0       /* no edit type yet */
#define EDIT_FILE   1       /* file name argument[s] given, use argument list */
#define EDIT_STDIN  2       /* read file from stdin */

static int file_owned(char *fname);
static void mainerr(int, char_u *);
static void main_msg(char *s);
static void usage(void);
static int get_number_arg(char_u *p, int *idx, int def);
static void parse_command_name(mparm_T *parmp);
static void command_line_scan(mparm_T *parmp);
static void check_tty(mparm_T *parmp);
static void read_stdin(void);
static void create_windows(mparm_T *parmp);
static void edit_buffers(mparm_T *parmp, char_u *cwd);
static void exe_pre_commands(mparm_T *parmp);
static void exe_commands(mparm_T *parmp);
static void source_startup_scripts(mparm_T *parmp);
static void check_swap_exists_action(void);

/*
 * Different types of error messages.
 */
static char *(main_errors[]) =
{
    "Unknown option argument",
#define ME_UNKNOWN_OPTION       0
    "Too many edit arguments",
#define ME_TOO_MANY_ARGS        1
    "Argument missing after",
#define ME_ARG_MISSING          2
    "Garbage after option argument",
#define ME_GARBAGE              3
    "Too many \"+command\", \"-c command\" or \"--cmd command\" arguments",
#define ME_EXTRA_CMD            4
    "Invalid argument for",
#define ME_INVALID_ARG          5
};

static char_u *start_dir = NULL;        /* current working dir on startup */

    int
main(argc, argv)
    int         argc;
    char        **argv;
{
    char_u      *fname = NULL;          /* file name from command line */
    mparm_T     params;                 /* various parameters passed between
                                         * main() and other functions. */

    /*
     * Do any system-specific initialisations.  These can NOT use IObuff or
     * NameBuff.  Thus emsg2() cannot be called!
     */
    mch_early_init();

    /* Many variables are in "params" so that we can pass them to invoked
     * functions without a lot of arguments.  "argc" and "argv" are also
     * copied, so that they can be changed. */
    vim_memset(&params, 0, sizeof(params));
    params.argc = argc;
    params.argv = argv;
    params.want_full_screen = TRUE;
    params.use_debug_break_level = -1;
    params.window_count = -1;

    starttime = time(NULL);

    mb_init(TRUE);  /* init mb_bytelen_tab[] to ones */
    eval_init();    /* init global variables */

    /* Init the table of Normal mode commands. */
    init_normal_cmds();

    /*
     * Allocate space for the generic buffers (needed for set_init_1() and EMSG2()).
     */
    if ((IObuff = alloc(IOSIZE)) == NULL || (NameBuff = alloc(MAXPATHL)) == NULL)
        mch_exit(0);

    clip_init(FALSE);           /* Initialise clipboard stuff */

    /*
     * Check if we have an interactive window.
     * On the Amiga: If there is no window, we open one with a newcli command
     * (needed for :! to * work). mch_check_win() will also handle the -d or
     * -dev argument.
     */
    params.stdout_isatty = (mch_check_win(params.argc, params.argv) != FAIL);

    /*
     * Allocate the first window and buffer.
     * Can't do anything without it, exit when it fails.
     */
    if (win_alloc_first() == FAIL)
        mch_exit(0);

    init_yank();                /* init yank buffers */

    alist_init(&global_alist);  /* Init the argument list to empty. */
    global_alist.id = 0;

    /*
     * Set the default values for the options.
     * First find out the home directory, needed to expand "~" in options.
     */
    init_homedir();             /* find real value of $HOME */
    set_init_1();

    /*
     * Figure out the way to work from the command name argv[0].
     * "vimdiff" starts diff mode, "rvim" sets "restricted", etc.
     */
    parse_command_name(&params);

    /*
     * Process the command line arguments.  File names are put in the global
     * argument list "global_alist".
     */
    command_line_scan(&params);

    /*
     * On some systems, when we compile with the GUI, we always use it.  On Mac
     * there is no terminal version, and on Windows we can't fork one off with :gui.
     */

    if (GARGCOUNT > 0)
    {
        fname = alist_name(&GARGLIST[0]);
    }

    /* Don't redraw until much later. */
    ++RedrawingDisabled;

    /*
     * When listing swap file names, don't do cursor positioning et. al.
     */
    if (recoverymode && fname == NULL)
        params.want_full_screen = FALSE;

    /*
     * When certain to start the GUI, don't check capabilities of terminal.
     * For GTK we can't be sure, but when started from the desktop it doesn't
     * make sense to try using a terminal.
     */

    /*
     * mch_init() sets up the terminal (window) for use.  This must be
     * done after resetting full_screen, otherwise it may move the cursor (MSDOS).
     * Note that we may use mch_exit() before mch_init()!
     */
    mch_init();

    /*
     * Print a warning if stdout is not a terminal.
     */
    check_tty(&params);

    /* This message comes before term inits, but after setting "silent_mode"
     * when the input is not a tty. */
    if (GARGCOUNT > 1 && !silent_mode)
        printf("%d files to edit\n", GARGCOUNT);

    if (params.want_full_screen && !silent_mode)
    {
        termcapinit(params.term);       /* set terminal name and get terminal
                                           capabilities (will set full_screen) */
        screen_start();                 /* don't know where cursor is now */
    }

    /*
     * Set the default values for the options that use Rows and Columns.
     */
    ui_get_shellsize();         /* inits Rows and Columns */
    win_init_size();

    cmdline_row = Rows - p_ch;
    msg_row = cmdline_row;
    screenalloc(FALSE);         /* allocate screen buffers */
    set_init_2();

    msg_scroll = TRUE;
    no_wait_return = TRUE;

    init_mappings();            /* set up initial mappings */

    init_highlight(TRUE, FALSE); /* set the default highlight groups */

    /* Set the break level after the terminal is initialized. */
    debug_break_level = params.use_debug_break_level;

    /* Execute --cmd arguments. */
    exe_pre_commands(&params);

    /* Source startup scripts. */
    source_startup_scripts(&params);

    /*
     * Read all the plugin files.
     * Only when compiled with +eval, since most plugins need it.
     */
    if (p_lpl)
    {
        source_runtime((char_u *)"plugin/**/*.vim", TRUE);
    }

    /*
     * Recovery mode without a file name: List swap files.
     * This uses the 'dir' option, therefore it must be after the
     * initializations.
     */
    if (recoverymode && fname == NULL)
    {
        recover_names(NULL, TRUE, 0, NULL);
        mch_exit(0);
    }

    /*
     * Set a few option defaults after reading .vimrc files:
     * 'title' and 'icon', Unix: 'shellpipe' and 'shellredir'.
     */
    set_init_3();

    /*
     * "-n" argument: Disable swap file by setting 'updatecount' to 0.
     * Note that this overrides anything from a vimrc file.
     */
    if (params.no_swap_file)
        p_uc = 0;

    /* It's better to make v:oldfiles an empty list than NULL. */
    if (get_vim_var_list(VV_OLDFILES) == NULL)
        set_vim_var_list(VV_OLDFILES, list_alloc());

    /*
     * Start putting things on the screen.
     * Scroll screen down before drawing over it
     * Clear screen now, so file message will not be cleared.
     */
    starting = NO_BUFFERS;
    no_wait_return = FALSE;
    if (!exmode_active)
        msg_scroll = FALSE;

    /*
     * If "-" argument given: Read file from stdin.
     * Do this before starting Raw mode, because it may change things that the
     * writing end of the pipe doesn't like, e.g., in case stdin and stderr
     * are the same terminal: "cat | vim -".
     * Using autocommands here may cause trouble...
     */
    if (params.edit_type == EDIT_STDIN && !recoverymode)
        read_stdin();

    /* When switching screens and something caused a message from a vimrc
     * script, need to output an extra newline on exit. */
    if ((did_emsg || msg_didout) && *T_TI != NUL)
        newline_on_exit = TRUE;

    /*
     * When done something that is not allowed or error message call
     * wait_return.  This must be done before starttermcap(), because it may
     * switch to another screen. It must be done after settmode(TMODE_RAW),
     * because we want to react on a single key stroke.
     * Call settmode and starttermcap here, so the T_KS and T_TI may be
     * defined by termcapinit and redefined in .exrc.
     */
    settmode(TMODE_RAW);

    if (need_wait_return || msg_didany)
    {
        wait_return(TRUE);
    }

    starttermcap();         /* start termcap if not done by wait_return() */
    may_req_ambiguous_char_width();

    setmouse();                         /* may start using the mouse */
    if (scroll_region)
        scroll_region_reset();          /* In case Rows changed */
    scroll_start();     /* may scroll the screen to the right position */

    /*
     * Don't clear the screen when starting in Ex mode, unless using the GUI.
     */
    if (exmode_active)
        must_redraw = CLEAR;
    else
        screenclear();                  /* clear screen */

    no_wait_return = TRUE;

    /*
     * Create the requested number of windows and edit buffers in them.
     * Also does recovery if "recoverymode" set.
     */
    create_windows(&params);

    /* clear v:swapcommand */
    set_vim_var_string(VV_SWAPCOMMAND, NULL, -1);

    /* Ex starts at last line of the file */
    if (exmode_active)
        curwin->w_cursor.lnum = curbuf->b_ml.ml_line_count;

    apply_autocmds(EVENT_BUFENTER, NULL, NULL, FALSE, curbuf);
    setpcmark();

    /*
     * If opened more than one window, start editing files in the other windows.
     */
    edit_buffers(&params, start_dir);
    vim_free(start_dir);

    /*
     * Shorten any of the filenames, but only when absolute.
     */
    shorten_fnames(FALSE);

    /* Execute any "+" and "-c" arguments. */
    if (params.n_commands > 0)
        exe_commands(&params);

    RedrawingDisabled = 0;
    redraw_all_later(NOT_VALID);
    no_wait_return = FALSE;
    starting = 0;

    /* Requesting the termresponse is postponed until here, so that a "-c q"
     * argument doesn't make it appear in the shell Vim was started from. */
    may_req_termresponse();

    /* start in insert mode */
    if (p_im)
        need_start_insertmode = TRUE;

    apply_autocmds(EVENT_VIMENTER, NULL, NULL, FALSE, curbuf);

    /* Adjust default register name for "unnamed" in 'clipboard'. Can only be
     * done after the clipboard is available and all initial commands that may
     * modify the 'clipboard' setting have run; i.e. just before entering the
     * main loop. */
    {
        int default_regname = 0;
        adjust_clip_reg(&default_regname);
        set_reg_var(default_regname);
    }

    /* If ":startinsert" command used, stuff a dummy command to be able to
     * call normal_cmd(), which will then start Insert mode. */
    if (restart_edit != 0)
        stuffcharReadbuff(K_NOP);

    /*
     * Call the main command loop.  This never returns.
     */
    main_loop(FALSE, FALSE);

    return 0;
}

/*
 * Main loop: Execute Normal mode commands until exiting Vim.
 * Also used to handle commands in the command-line window, until the window is closed.
 * Also used to handle ":visual" command after ":global": execute Normal mode
 * commands, return when entering Ex mode.  "noexmode" is TRUE then.
 */
    void
main_loop(cmdwin, noexmode)
    int         cmdwin;     /* TRUE when working in the command-line window */
    int         noexmode;   /* TRUE when return on entering Ex mode */
{
    oparg_T     oa;                             /* operator arguments */
    volatile int previous_got_int = FALSE;      /* "got_int" was TRUE */
    linenr_T    conceal_old_cursor_line = 0;
    linenr_T    conceal_new_cursor_line = 0;
    int         conceal_update_lines = FALSE;

    clear_oparg(&oa);
    while (!cmdwin
            || cmdwin_result == 0
            )
    {
        if (stuff_empty())
        {
            did_check_timestamps = FALSE;
            if (need_check_timestamps)
                check_timestamps(FALSE);
            if (need_wait_return)       /* if wait_return still needed ... */
                wait_return(FALSE);     /* ... call it now */
            if (need_start_insertmode && goto_im() && !VIsual_active)
            {
                need_start_insertmode = FALSE;
                stuffReadbuff((char_u *)"i");   /* start insert mode next */
                /* skip the fileinfo message now, because it would be shown
                 * after insert mode finishes! */
                need_fileinfo = FALSE;
            }
        }

        /* Reset "got_int" now that we got back to the main loop.  Except when
         * inside a ":g/pat/cmd" command, then the "got_int" needs to abort the ":g" command.
         * For ":g/pat/vi" we reset "got_int" when used once.  When used
         * a second time we go back to Ex mode and abort the ":g" command. */
        if (got_int)
        {
            if (noexmode && global_busy && !exmode_active && previous_got_int)
            {
                /* Typed two CTRL-C in a row: go back to ex mode as if "Q" was
                 * used and keep "got_int" set, so that it aborts ":g". */
                exmode_active = EXMODE_NORMAL;
                State = NORMAL;
            }
            else if (!global_busy || !exmode_active)
            {
                if (!quit_more)
                    (void)vgetc();              /* flush all buffers */
                got_int = FALSE;
            }
            previous_got_int = TRUE;
        }
        else
            previous_got_int = FALSE;

        if (!exmode_active)
            msg_scroll = FALSE;
        quit_more = FALSE;

        /*
         * If skip redraw is set (for ":" in wait_return()), don't redraw now.
         * If there is nothing in the stuff_buffer or do_redraw is TRUE,
         * update cursor and redraw.
         */
        if (skip_redraw || exmode_active)
            skip_redraw = FALSE;
        else if (do_redraw || stuff_empty())
        {
            /* Trigger CursorMoved if the cursor moved. */
            if (!finish_op && (has_cursormoved() || curwin->w_p_cole > 0)
                 && !equalpos(last_cursormoved, curwin->w_cursor))
            {
                if (has_cursormoved())
                    apply_autocmds(EVENT_CURSORMOVED, NULL, NULL, FALSE, curbuf);
                if (curwin->w_p_cole > 0)
                {
                    conceal_old_cursor_line = last_cursormoved.lnum;
                    conceal_new_cursor_line = curwin->w_cursor.lnum;
                    conceal_update_lines = TRUE;
                }
                last_cursormoved = curwin->w_cursor;
            }

            /* Trigger TextChanged if b_changedtick differs. */
            if (!finish_op && has_textchanged() && last_changedtick != curbuf->b_changedtick)
            {
                if (last_changedtick_buf == curbuf)
                    apply_autocmds(EVENT_TEXTCHANGED, NULL, NULL,
                                                               FALSE, curbuf);
                last_changedtick_buf = curbuf;
                last_changedtick = curbuf->b_changedtick;
            }

            /*
             * Before redrawing, make sure w_topline is correct, and w_leftcol
             * if lines don't wrap, and w_skipcol if lines wrap.
             */
            update_topline();
            validate_cursor();

            if (VIsual_active)
                update_curbuf(INVERTED);/* update inverted part */
            else if (must_redraw)
                update_screen(0);
            else if (redraw_cmdline || clear_cmdline)
                showmode();
            redraw_statuslines();
            if (need_maketitle)
                maketitle();
            /* display message after redraw */
            if (keep_msg != NULL)
            {
                char_u *p;

                /* msg_attr_keep() will set keep_msg to NULL, must free the
                 * string here. Don't reset keep_msg, msg_attr_keep() uses it
                 * to check for duplicates. */
                p = keep_msg;
                msg_attr(p, keep_msg_attr);
                vim_free(p);
            }
            if (need_fileinfo)          /* show file info after redraw */
            {
                fileinfo(FALSE, FALSE);
                need_fileinfo = FALSE;
            }

            emsg_on_display = FALSE;    /* can delete error message now */
            did_emsg = FALSE;
            msg_didany = FALSE;         /* reset lines_left in msg_start() */
            may_clear_sb_text();        /* clear scroll-back text on next msg */
            showruler(FALSE);

            if (conceal_update_lines
                    && (conceal_old_cursor_line != conceal_new_cursor_line
                        || conceal_cursor_line(curwin)
                        || need_cursor_line_redraw))
            {
                if (conceal_old_cursor_line != conceal_new_cursor_line
                        && conceal_old_cursor_line <= curbuf->b_ml.ml_line_count)
                    update_single_line(curwin, conceal_old_cursor_line);
                update_single_line(curwin, conceal_new_cursor_line);
                curwin->w_valid &= ~VALID_CROW;
            }
            setcursor();
            cursor_on();

            do_redraw = FALSE;
        }

        /*
         * Update w_curswant if w_set_curswant has been set.
         * Postponed until here to avoid computing w_virtcol too often.
         */
        update_curswant();

        /*
         * May perform garbage collection when waiting for a character, but
         * only at the very toplevel.  Otherwise we may be using a List or
         * Dict internally somewhere.
         * "may_garbage_collect" is reset in vgetc() which is invoked through
         * do_exmode() and normal_cmd().
         */
        may_garbage_collect = (!cmdwin && !noexmode);
        /*
         * If we're invoked as ex, do a round of ex commands.
         * Otherwise, get and execute a normal mode command.
         */
        if (exmode_active)
        {
            if (noexmode)   /* End of ":global/path/visual" commands */
                return;
            do_exmode(exmode_active == EXMODE_VIM);
        }
        else
            normal_cmd(&oa, TRUE);
    }
}

/* Exit properly */
    void
getout(exitval)
    int         exitval;
{
    buf_T       *buf;
    win_T       *wp;
    tabpage_T   *tp, *next_tp;

    exiting = TRUE;

    /* When running in Ex mode an error causes us to exit with a non-zero exit
     * code.  POSIX requires this, although it's not 100% clear from the standard. */
    if (exmode_active)
        exitval += ex_exitval;

    /* Position the cursor on the last screen line, below all the text */
        windgoto((int)Rows - 1, 0);

    /* Optionally print hashtable efficiency. */
    hash_debug_results();

    if (get_vim_var_nr(VV_DYING) <= 1)
    {
        /* Trigger BufWinLeave for all windows, but only once per buffer. */
        for (tp = first_tabpage; tp != NULL; tp = next_tp)
        {
            next_tp = tp->tp_next;
            for (wp = (tp == curtab) ? firstwin : tp->tp_firstwin; wp != NULL; wp = wp->w_next)
            {
                if (wp->w_buffer == NULL)
                    /* Autocmd must have close the buffer already, skip. */
                    continue;
                buf = wp->w_buffer;
                if (buf->b_changedtick != -1)
                {
                    apply_autocmds(EVENT_BUFWINLEAVE, buf->b_fname, buf->b_fname, FALSE, buf);
                    buf->b_changedtick = -1;  /* note that we did it already */
                    /* start all over, autocommands may mess up the lists */
                    next_tp = first_tabpage;
                    break;
                }
            }
        }

        /* Trigger BufUnload for buffers that are loaded */
        for (buf = firstbuf; buf != NULL; buf = buf->b_next)
            if (buf->b_ml.ml_mfp != NULL)
            {
                apply_autocmds(EVENT_BUFUNLOAD, buf->b_fname, buf->b_fname, FALSE, buf);
                if (!buf_valid(buf))    /* autocmd may delete the buffer */
                    break;
            }
        apply_autocmds(EVENT_VIMLEAVEPRE, NULL, NULL, FALSE, curbuf);
    }

    if (get_vim_var_nr(VV_DYING) <= 1)
        apply_autocmds(EVENT_VIMLEAVE, NULL, NULL, FALSE, curbuf);

    if (did_emsg)
    {
        /* give the user a chance to read the (error) message */
        no_wait_return = FALSE;
        wait_return(FALSE);
    }

    /* Position the cursor again, the autocommands may have moved it */
        windgoto((int)Rows - 1, 0);

    if (garbage_collect_at_exit)
        garbage_collect();

    mch_exit(exitval);
}

/*
 * Get a (optional) count for a Vim argument.
 */
    static int
get_number_arg(p, idx, def)
    char_u      *p;         /* pointer to argument */
    int         *idx;       /* index in argument, is incremented */
    int         def;        /* default value */
{
    if (vim_isdigit(p[*idx]))
    {
        def = atoi((char *)&(p[*idx]));
        while (vim_isdigit(p[*idx]))
            *idx = *idx + 1;
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
    static void
parse_command_name(parmp)
    mparm_T     *parmp;
{
    char_u      *initstr;

    initstr = gettail((char_u *)parmp->argv[0]);

    set_vim_var_string(VV_PROGNAME, initstr, -1);
    set_vim_var_string(VV_PROGPATH, (char_u *)parmp->argv[0], -1);

    if (TOLOWER_ASC(initstr[0]) == 'r')
    {
        restricted = TRUE;
        ++initstr;
    }

    /* Use evim mode for "evim", not for "editor". */
    if (STRNICMP(initstr, "ev", 2) == 0)
    {
        parmp->evim_mode = TRUE;
        ++initstr;
    }

    if (STRNICMP(initstr, "view", 4) == 0)
    {
        readonlymode = TRUE;
        curbuf->b_p_ro = TRUE;
        p_uc = 10000;                   /* don't update very often */
        initstr += 4;
    }
    else if (STRNICMP(initstr, "vim", 3) == 0)
        initstr += 3;

    if (STRNICMP(initstr, "ex", 2) == 0)
    {
        if (STRNICMP(initstr + 2, "im", 2) == 0)
            exmode_active = EXMODE_VIM;
        else
            exmode_active = EXMODE_NORMAL;
    }
}

/*
 * Scan the command line arguments.
 */
    static void
command_line_scan(parmp)
    mparm_T     *parmp;
{
    int         argc = parmp->argc;
    char        **argv = parmp->argv;
    int         argv_idx;               /* index in argv[n][] */
    int         had_minmin = FALSE;     /* found "--" argument */
    int         want_argument;          /* option argument with argument */
    int         c;
    char_u      *p = NULL;
    long        n;

    --argc;
    ++argv;
    argv_idx = 1;           /* active option letter is argv[0][argv_idx] */
    while (argc > 0)
    {
        /*
         * "+" or "+{number}" or "+/{pat}" or "+{command}" argument.
         */
        if (argv[0][0] == '+' && !had_minmin)
        {
            if (parmp->n_commands >= MAX_ARG_CMDS)
                mainerr(ME_EXTRA_CMD, NULL);
            argv_idx = -1;          /* skip to next argument */
            if (argv[0][1] == NUL)
                parmp->commands[parmp->n_commands++] = (char_u *)"$";
            else
                parmp->commands[parmp->n_commands++] = (char_u *)&(argv[0][1]);
        }

        /*
         * Optional argument.
         */
        else if (argv[0][0] == '-' && !had_minmin)
        {
            want_argument = FALSE;
            c = argv[0][argv_idx++];
            switch (c)
            {
            case NUL:           /* "vim -"  read from stdin */
                                /* "ex -" silent mode */
                if (exmode_active)
                    silent_mode = TRUE;
                else
                {
                    if (parmp->edit_type != EDIT_NONE)
                        mainerr(ME_TOO_MANY_ARGS, (char_u *)argv[0]);
                    parmp->edit_type = EDIT_STDIN;
                    read_cmd_fd = 2;    /* read from stderr instead of stdin */
                }
                argv_idx = -1;          /* skip to next argument */
                break;

            case '-':           /* "--" don't take any more option arguments */
                                /* "--help" give help message */
                                /* "--noplugin[s]" skip plugins */
                                /* "--cmd <cmd>" execute cmd before vimrc */
                if (STRICMP(argv[0] + argv_idx, "help") == 0)
                    usage();
                else if (STRNICMP(argv[0] + argv_idx, "noplugin", 8) == 0)
                    p_lpl = FALSE;
                else if (STRNICMP(argv[0] + argv_idx, "cmd", 3) == 0)
                {
                    want_argument = TRUE;
                    argv_idx += 3;
                }
                else if (STRNICMP(argv[0] + argv_idx, "startuptime", 11) == 0)
                {
                    want_argument = TRUE;
                    argv_idx += 11;
                }
                else
                {
                    if (argv[0][argv_idx])
                        mainerr(ME_UNKNOWN_OPTION, (char_u *)argv[0]);
                    had_minmin = TRUE;
                }
                if (!want_argument)
                    argv_idx = -1;      /* skip to next argument */
                break;

            case 'b':           /* "-b" binary mode */
                /* Needs to be effective before expanding file names, because
                 * for Win32 this makes us edit a shortcut file itself,
                 * instead of the file it links to. */
                set_options_bin(curbuf->b_p_bin, 1, 0);
                curbuf->b_p_bin = 1;        /* binary file I/O */
                break;

            case 'e':           /* "-e" Ex mode */
                exmode_active = EXMODE_NORMAL;
                break;

            case 'E':           /* "-E" Improved Ex mode */
                exmode_active = EXMODE_VIM;
                break;

            case 'h':           /* "-h" give help message */
                usage();
                break;

            case 'H':           /* "-H" start in Hebrew mode: rl + hkmap set */
                p_hkmap = TRUE;
                set_option_value((char_u *)"rl", 1L, NULL, 0);
                break;

            case 'l':           /* "-l" lisp mode, 'lisp' and 'showmatch' on */
                set_option_value((char_u *)"lisp", 1L, NULL, 0);
                p_sm = TRUE;
                break;

            case 'M':           /* "-M"  no changes or writing of files */
                reset_modifiable();
                /* FALLTHROUGH */

            case 'm':           /* "-m"  no writing of files */
                p_write = FALSE;
                break;

            case 'y':           /* "-y"  easy mode */
                parmp->evim_mode = TRUE;
                break;

            case 'n':           /* "-n" no swap file */
                parmp->no_swap_file = TRUE;
                break;

            case 'p':           /* "-p[N]" open N tab pages */
                /* default is 0: open window for each file */
                parmp->window_count = get_number_arg((char_u *)argv[0], &argv_idx, 0);
                parmp->window_layout = WIN_TABS;
                break;

            case 'o':           /* "-o[N]" open N horizontal split windows */
                /* default is 0: open window for each file */
                parmp->window_count = get_number_arg((char_u *)argv[0], &argv_idx, 0);
                parmp->window_layout = WIN_HOR;
                break;

            case 'O':           /* "-O[N]" open N vertical split windows */
                /* default is 0: open window for each file */
                parmp->window_count = get_number_arg((char_u *)argv[0], &argv_idx, 0);
                parmp->window_layout = WIN_VER;
                break;

            case 'R':           /* "-R" readonly mode */
                readonlymode = TRUE;
                curbuf->b_p_ro = TRUE;
                p_uc = 10000;                   /* don't update very often */
                break;

            case 'r':           /* "-r" recovery mode */
                recoverymode = 1;
                break;

            case 's':
                if (exmode_active)      /* "-s" silent (batch) mode */
                    silent_mode = TRUE;
                else            /* "-s {scriptin}" read from script file */
                    want_argument = TRUE;
                break;

            case 'D':           /* "-D"         Debugging */
                parmp->use_debug_break_level = 9999;
                break;
            case 'V':           /* "-V{N}"      Verbose level */
                /* default is 10: a little bit verbose */
                p_verbose = get_number_arg((char_u *)argv[0], &argv_idx, 10);
                if (argv[0][argv_idx] != NUL)
                {
                    set_option_value((char_u *)"verbosefile", 0L, (char_u *)argv[0] + argv_idx, 0);
                    argv_idx = (int)STRLEN(argv[0]);
                }
                break;

            case 'v':           /* "-v"  Vi-mode (as if called "vi") */
                exmode_active = 0;
                break;

            case 'w':           /* "-w{number}" set window height */
                                /* "-w {scriptout}"     write to script */
                if (vim_isdigit(((char_u *)argv[0])[argv_idx]))
                {
                    n = get_number_arg((char_u *)argv[0], &argv_idx, 10);
                    set_option_value((char_u *)"window", n, NULL, 0);
                    break;
                }
                want_argument = TRUE;
                break;

            case 'Z':           /* "-Z"  restricted mode */
                restricted = TRUE;
                break;

            case 'c':           /* "-c{command}" or "-c {command}" execute command */
                if (argv[0][argv_idx] != NUL)
                {
                    if (parmp->n_commands >= MAX_ARG_CMDS)
                        mainerr(ME_EXTRA_CMD, NULL);
                    parmp->commands[parmp->n_commands++] = (char_u *)argv[0] + argv_idx;
                    argv_idx = -1;
                    break;
                }
                /*FALLTHROUGH*/
            case 'i':           /* "-i {viminfo}" use for viminfo */
            case 'T':           /* "-T {terminal}" terminal name */
            case 'u':           /* "-u {vimrc}" vim inits file */
            case 'W':           /* "-W {scriptout}" overwrite */
                want_argument = TRUE;
                break;

            default:
                mainerr(ME_UNKNOWN_OPTION, (char_u *)argv[0]);
            }

            /*
             * Handle option arguments with argument.
             */
            if (want_argument)
            {
                /*
                 * Check for garbage immediately after the option letter.
                 */
                if (argv[0][argv_idx] != NUL)
                    mainerr(ME_GARBAGE, (char_u *)argv[0]);

                --argc;
                if (argc < 1)
                    mainerr_arg_missing((char_u *)argv[0]);
                ++argv;
                argv_idx = -1;

                switch (c)
                {
                case 'c':       /* "-c {command}" execute command */
                    if (parmp->n_commands >= MAX_ARG_CMDS)
                        mainerr(ME_EXTRA_CMD, NULL);
                    parmp->commands[parmp->n_commands++] = (char_u *)argv[0];
                    break;

                case '-':
                    if (argv[-1][2] == 'c')
                    {
                        /* "--cmd {command}" execute command */
                        if (parmp->n_pre_commands >= MAX_ARG_CMDS)
                            mainerr(ME_EXTRA_CMD, NULL);
                        parmp->pre_commands[parmp->n_pre_commands++] = (char_u *)argv[0];
                    }
                    /* "--startuptime <file>" already handled */
                    break;

                case 'i':       /* "-i {viminfo}" use for viminfo */
                    use_viminfo = (char_u *)argv[0];
                    break;

                case 's':       /* "-s {scriptin}" read from script file */
                    if (scriptin[0] != NULL)
                    {
scripterror:
                        fprintf(stderr, "Attempt to open script file again: \"%s %s\"\n", argv[-1], argv[0]);
                        mch_exit(2);
                    }
                    if ((scriptin[0] = mch_fopen(argv[0], READBIN)) == NULL)
                    {
                        fprintf(stderr, "Cannot open for reading: \"%s\"\n", argv[0]);
                        mch_exit(2);
                    }
                    if (save_typebuf() == FAIL)
                        mch_exit(2);    /* out of memory */
                    break;

                case 'T':       /* "-T {terminal}" terminal name */
                    /*
                     * The -T term argument is always available and when
                     * HAVE_TERMLIB is supported it overrides the environment variable TERM.
                     */
                        parmp->term = (char_u *)argv[0];
                    break;

                case 'u':       /* "-u {vimrc}" vim inits file */
                    parmp->use_vimrc = (char_u *)argv[0];
                    break;

                case 'w':       /* "-w {nr}" 'window' value */
                                /* "-w {scriptout}" append to script file */
                    if (vim_isdigit(*((char_u *)argv[0])))
                    {
                        argv_idx = 0;
                        n = get_number_arg((char_u *)argv[0], &argv_idx, 10);
                        set_option_value((char_u *)"window", n, NULL, 0);
                        argv_idx = -1;
                        break;
                    }
                    /*FALLTHROUGH*/
                case 'W':       /* "-W {scriptout}" overwrite script file */
                    if (scriptout != NULL)
                        goto scripterror;
                    if ((scriptout = mch_fopen(argv[0], (c == 'w') ? APPENDBIN : WRITEBIN)) == NULL)
                    {
                        fprintf(stderr, "Cannot open for script output: \"%s\"\n", argv[0]);
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
            argv_idx = -1;          /* skip to next argument */

            /* Check for only one type of editing. */
            if (parmp->edit_type != EDIT_NONE && parmp->edit_type != EDIT_FILE)
                mainerr(ME_TOO_MANY_ARGS, (char_u *)argv[0]);
            parmp->edit_type = EDIT_FILE;

            /* Add the file to the global argument list. */
            if (ga_grow(&global_alist.al_ga, 1) == FAIL
                    || (p = vim_strsave((char_u *)argv[0])) == NULL)
                mch_exit(2);

            alist_add(&global_alist, p, 2);           /* add buffer number now and use curbuf */
        }

        /*
         * If there are no more letters after the current "-", go to next
         * argument.  argv_idx is set to -1 when the current argument is to be skipped.
         */
        if (argv_idx <= 0 || argv[0][argv_idx] == NUL)
        {
            --argc;
            ++argv;
            argv_idx = 1;
        }
    }

    /* If there is a "+123" or "-c" command, set v:swapcommand to the first one. */
    if (parmp->n_commands > 0)
    {
        p = alloc((unsigned)STRLEN(parmp->commands[0]) + 3);
        if (p != NULL)
        {
            sprintf((char *)p, ":%s\r", parmp->commands[0]);
            set_vim_var_string(VV_SWAPCOMMAND, p, -1);
            vim_free(p);
        }
    }
}

/*
 * Print a warning if stdout is not a terminal.
 * When starting in Ex mode and commands come from a file, set Silent mode.
 */
    static void
check_tty(parmp)
    mparm_T     *parmp;
{
    int         input_isatty;           /* is active input a terminal? */

    input_isatty = mch_input_isatty();
    if (exmode_active)
    {
        if (!input_isatty)
            silent_mode = TRUE;
    }
    else if (parmp->want_full_screen && (!parmp->stdout_isatty || !input_isatty))
    {
        if (!parmp->stdout_isatty)
            fprintf(stderr, "Vim: Warning: Output is not to a terminal\n");
        if (!input_isatty)
            fprintf(stderr, "Vim: Warning: Input is not from a terminal\n");
        out_flush();
        if (scriptin[0] == NULL)
            ui_delay(2000L, TRUE);
    }
}

/*
 * Read text from stdin.
 */
    static void
read_stdin()
{
    int     i;

    /* When getting the ATTENTION prompt here, use a dialog */
    swap_exists_action = SEA_DIALOG;
    no_wait_return = TRUE;
    i = msg_didany;
    set_buflisted(TRUE);
    (void)open_buffer(TRUE, NULL, 0);   /* create memfile and read file */
    no_wait_return = FALSE;
    msg_didany = i;
    check_swap_exists_action();
    /*
     * Close stdin and dup it from stderr.  Required for GPM to work
     * properly, and for running external commands.
     * Is there any other system that cannot do this?
     */
    close(0);
    ignored = dup(2);
}

/*
 * Create the requested number of windows and edit buffers in them.
 * Also does recovery if "recoverymode" set.
 */
    static void
create_windows(parmp)
    mparm_T     *parmp UNUSED;
{
    int         dorewind;
    int         done = 0;

    /*
     * Create the number of windows that was requested.
     */
    if (parmp->window_count == -1)      /* was not set */
        parmp->window_count = 1;
    if (parmp->window_count == 0)
        parmp->window_count = GARGCOUNT;
    if (parmp->window_count > 1)
    {
        /* Don't change the windows if there was a command in .vimrc that
         * already split some windows */
        if (parmp->window_layout == 0)
            parmp->window_layout = WIN_HOR;
        if (parmp->window_layout == WIN_TABS)
        {
            parmp->window_count = make_tabpages(parmp->window_count);
        }
        else if (firstwin->w_next == NULL)
        {
            parmp->window_count = make_windows(parmp->window_count, parmp->window_layout == WIN_VER);
        }
        else
            parmp->window_count = win_count();
    }
    else
        parmp->window_count = 1;

    if (recoverymode)                   /* do recover */
    {
        msg_scroll = TRUE;              /* scroll message up */
        ml_recover();
        if (curbuf->b_ml.ml_mfp == NULL) /* failed */
            getout(1);
        do_modelines(0);                /* do modelines */
    }
    else
    {
        /*
         * Open a buffer for windows that don't have one yet.
         * Commands in the .vimrc might have loaded a file or split the window.
         * Watch out for autocommands that delete a window.
         */
        /*
         * Don't execute Win/Buf Enter/Leave autocommands here
         */
        ++autocmd_no_enter;
        ++autocmd_no_leave;
        dorewind = TRUE;
        while (done++ < 1000)
        {
            if (dorewind)
            {
                if (parmp->window_layout == WIN_TABS)
                    goto_tabpage(1);
                else
                    curwin = firstwin;
            }
            else if (parmp->window_layout == WIN_TABS)
            {
                if (curtab->tp_next == NULL)
                    break;
                goto_tabpage(0);
            }
            else
            {
                if (curwin->w_next == NULL)
                    break;
                curwin = curwin->w_next;
            }
            dorewind = FALSE;
            curbuf = curwin->w_buffer;
            if (curbuf->b_ml.ml_mfp == NULL)
            {
                /* When getting the ATTENTION prompt here, use a dialog */
                swap_exists_action = SEA_DIALOG;
                set_buflisted(TRUE);

                /* create memfile, read file */
                (void)open_buffer(FALSE, NULL, 0);

                if (swap_exists_action == SEA_QUIT)
                {
                    if (got_int || only_one_window())
                    {
                        /* abort selected or quit and only one window */
                        did_emsg = FALSE;   /* avoid hit-enter prompt */
                        getout(1);
                    }
                    /* We can't close the window, it would disturb what
                     * happens next.  Clear the file name and set the arg
                     * index to -1 to delete it later. */
                    setfname(curbuf, NULL, NULL, FALSE);
                    curwin->w_arg_idx = -1;
                    swap_exists_action = SEA_NONE;
                }
                else
                    handle_swap_exists(NULL);
                dorewind = TRUE;                /* start again */
            }
            ui_breakcheck();
            if (got_int)
            {
                (void)vgetc();  /* only break the file loading, not the rest */
                break;
            }
        }
        if (parmp->window_layout == WIN_TABS)
            goto_tabpage(1);
        else
            curwin = firstwin;
        curbuf = curwin->w_buffer;
        --autocmd_no_enter;
        --autocmd_no_leave;
    }
}

    /*
     * If opened more than one window, start editing files in the other
     * windows.  make_windows() has already opened the windows.
     */
    static void
edit_buffers(parmp, cwd)
    mparm_T     *parmp;
    char_u      *cwd;                   /* current working dir */
{
    int         arg_idx;                /* index in argument list */
    int         i;
    int         advance = TRUE;
    win_T       *win;

    /*
     * Don't execute Win/Buf Enter/Leave autocommands here
     */
    ++autocmd_no_enter;
    ++autocmd_no_leave;

    /* When w_arg_idx is -1 remove the window (see create_windows()). */
    if (curwin->w_arg_idx == -1)
    {
        win_close(curwin, TRUE);
        advance = FALSE;
    }

    arg_idx = 1;
    for (i = 1; i < parmp->window_count; ++i)
    {
        if (cwd != NULL)
            mch_chdir((char *)cwd);
        /* When w_arg_idx is -1 remove the window (see create_windows()). */
        if (curwin->w_arg_idx == -1)
        {
            ++arg_idx;
            win_close(curwin, TRUE);
            advance = FALSE;
            continue;
        }

        if (advance)
        {
            if (parmp->window_layout == WIN_TABS)
            {
                if (curtab->tp_next == NULL)    /* just checking */
                    break;
                goto_tabpage(0);
            }
            else
            {
                if (curwin->w_next == NULL)     /* just checking */
                    break;
                win_enter(curwin->w_next, FALSE);
            }
        }
        advance = TRUE;

        /* Only open the file if there is no file in this window yet (that can
         * happen when .vimrc contains ":sall"). */
        if (curbuf == firstwin->w_buffer || curbuf->b_ffname == NULL)
        {
            curwin->w_arg_idx = arg_idx;
            /* Edit file from arg list, if there is one.  When "Quit" selected
             * at the ATTENTION prompt close the window. */
            swap_exists_did_quit = FALSE;
            (void)do_ecmd(0, arg_idx < GARGCOUNT
                          ? alist_name(&GARGLIST[arg_idx]) : NULL,
                          NULL, NULL, ECMD_LASTL, ECMD_HIDE, curwin);
            if (swap_exists_did_quit)
            {
                /* abort or quit selected */
                if (got_int || only_one_window())
                {
                    /* abort selected and only one window */
                    did_emsg = FALSE;   /* avoid hit-enter prompt */
                    getout(1);
                }
                win_close(curwin, TRUE);
                advance = FALSE;
            }
            if (arg_idx == GARGCOUNT - 1)
                arg_had_last = TRUE;
            ++arg_idx;
        }
        ui_breakcheck();
        if (got_int)
        {
            (void)vgetc();      /* only break the file loading, not the rest */
            break;
        }
    }

    if (parmp->window_layout == WIN_TABS)
        goto_tabpage(1);
    --autocmd_no_enter;

    /* make the first window the current window */
    win = firstwin;
    win_enter(win, FALSE);

    --autocmd_no_leave;
    if (parmp->window_count > 1 && parmp->window_layout != WIN_TABS)
        win_equal(curwin, FALSE, 'b');  /* adjust heights */
}

/*
 * Execute the commands from --cmd arguments "cmds[cnt]".
 */
    static void
exe_pre_commands(parmp)
    mparm_T     *parmp;
{
    char_u      **cmds = parmp->pre_commands;
    int         cnt = parmp->n_pre_commands;
    int         i;

    if (cnt > 0)
    {
        curwin->w_cursor.lnum = 0; /* just in case.. */
        sourcing_name = (char_u *)"pre-vimrc command line";
        current_SID = SID_CMDARG;
        for (i = 0; i < cnt; ++i)
            do_cmdline_cmd(cmds[i]);
        sourcing_name = NULL;
        current_SID = 0;
    }
}

/*
 * Execute "+" and "-c" arguments.
 */
    static void
exe_commands(parmp)
    mparm_T     *parmp;
{
    int         i;

    /*
     * We start commands on line 0, make "vim +/pat file" match a
     * pattern on line 1.  But don't move the cursor when an autocommand
     * with g`" was used.
     */
    msg_scroll = TRUE;
    if (curwin->w_cursor.lnum <= 1)
        curwin->w_cursor.lnum = 0;
    sourcing_name = (char_u *)"command line";
    current_SID = SID_CARG;
    for (i = 0; i < parmp->n_commands; ++i)
    {
        do_cmdline_cmd(parmp->commands[i]);
        if (parmp->cmds_tofree[i])
            vim_free(parmp->commands[i]);
    }
    sourcing_name = NULL;
    current_SID = 0;
    if (curwin->w_cursor.lnum == 0)
        curwin->w_cursor.lnum = 1;

    if (!exmode_active)
        msg_scroll = FALSE;
}

/*
 * Source startup scripts.
 */
    static void
source_startup_scripts(parmp)
    mparm_T     *parmp;
{
    int         i;

    /*
     * For "evim" source evim.vim first of all, so that the user can overrule
     * any things he doesn't like.
     */
    if (parmp->evim_mode)
        (void)do_source((char_u *)EVIM_FILE, FALSE);

    /*
     * If -u argument given, use only the initializations from that file and nothing else.
     */
    if (parmp->use_vimrc != NULL)
    {
        if (STRCMP(parmp->use_vimrc, "NONE") == 0 || STRCMP(parmp->use_vimrc, "NORC") == 0)
        {
            if (parmp->use_vimrc[2] == 'N')
                p_lpl = FALSE;              /* don't load plugins either */
        }
        else
        {
            if (do_source(parmp->use_vimrc, FALSE) != OK)
                EMSG2("E282: Cannot read from \"%s\"", parmp->use_vimrc);
        }
    }
    else if (!silent_mode)
    {
        /*
         * Get system wide defaults, if the file name is defined.
         */
        (void)do_source((char_u *)SYS_VIMRC_FILE, FALSE);

        /*
         * Try to read initialization commands from the following places:
         * - environment variable VIMINIT
         * - user vimrc file (s:.vimrc for Amiga, ~/.vimrc otherwise)
         * - second user vimrc file ($VIM/.vimrc for Dos)
         * - environment variable EXINIT
         * - user exrc file (s:.exrc for Amiga, ~/.exrc otherwise)
         * - second user exrc file ($VIM/.exrc for Dos)
         * The first that exists is used, the rest is ignored.
         */
        if (process_env((char_u *)"VIMINIT") != OK)
        {
            if (do_source((char_u *)USR_VIMRC_FILE, TRUE) == FAIL
                && process_env((char_u *)"EXINIT") == FAIL
                && do_source((char_u *)USR_EXRC_FILE, FALSE) == FAIL)
            {
            }
        }

        /*
         * Read initialization commands from ".vimrc" or ".exrc" in current
         * directory.  This is only done if the 'exrc' option is set.
         * Because of security reasons we disallow shell and write commands
         * now, except for unix if the file is owned by the user or 'secure'
         * option has been reset in environment of global ".exrc" or ".vimrc".
         * Only do this if VIMRC_FILE is not the same as USR_VIMRC_FILE or SYS_VIMRC_FILE.
         */
        if (p_exrc)
        {
            /* If ".vimrc" file is not owned by user, set 'secure' mode. */
            if (!file_owned(VIMRC_FILE))
                secure = p_secure;

            i = FAIL;
            if (fullpathcmp((char_u *)USR_VIMRC_FILE, (char_u *)VIMRC_FILE, FALSE) != FPC_SAME
                && fullpathcmp((char_u *)SYS_VIMRC_FILE, (char_u *)VIMRC_FILE, FALSE) != FPC_SAME)
                i = do_source((char_u *)VIMRC_FILE, TRUE);

            if (i == FAIL)
            {
                /* if ".exrc" is not owned by user set 'secure' mode */
                if (!file_owned(EXRC_FILE))
                    secure = p_secure;
                else
                    secure = 0;
                if (fullpathcmp((char_u *)USR_EXRC_FILE, (char_u *)EXRC_FILE, FALSE) != FPC_SAME)
                    (void)do_source((char_u *)EXRC_FILE, FALSE);
            }
        }
        if (secure == 2)
            need_wait_return = TRUE;
        secure = 0;
    }
}

/*
 * Get an environment variable, and execute it as Ex commands.
 * Returns FAIL if the environment variable was not executed, OK otherwise.
 */
    int
process_env(env)
    char_u      *env;
{
    char_u      *initstr;
    char_u      *save_sourcing_name;
    linenr_T    save_sourcing_lnum;
    scid_T      save_sid;

    if ((initstr = mch_getenv(env)) != NULL && *initstr != NUL)
    {
        save_sourcing_name = sourcing_name;
        save_sourcing_lnum = sourcing_lnum;
        sourcing_name = env;
        sourcing_lnum = 0;
        save_sid = current_SID;
        current_SID = SID_ENV;
        do_cmdline_cmd(initstr);
        sourcing_name = save_sourcing_name;
        sourcing_lnum = save_sourcing_lnum;
        current_SID = save_sid;;
        return OK;
    }
    return FAIL;
}

/*
 * Return TRUE if we are certain the user owns the file "fname".
 * Used for ".vimrc" and ".exrc".
 * Use both stat() and lstat() for extra security.
 */
    static int
file_owned(fname)
    char        *fname;
{
    struct stat s;
    uid_t       uid = getuid();

    return !(mch_stat(fname, &s) != 0 || s.st_uid != uid
          || mch_lstat(fname, &s) != 0 || s.st_uid != uid);
}

/*
 * Give an error message main_errors["n"] and exit.
 */
    static void
mainerr(n, str)
    int         n;      /* one of the ME_ defines */
    char_u      *str;   /* extra argument or NULL */
{
    reset_signals();            /* kill us with CTRL-C here, if you like */

    fprintf(stderr, "%s\n", longVersion);
    fprintf(stderr, "%s", (char *)main_errors[n]);
    if (str != NULL)
        fprintf(stderr, ": \"%s\"", (char *)str);
    fprintf(stderr, "\nMore info with: \"vim -h\"\n");

    mch_exit(1);
}

    void
mainerr_arg_missing(str)
    char_u      *str;
{
    mainerr(ME_ARG_MISSING, str);
}

/*
 * print a message with three spaces prepended and '\n' appended.
 */
    static void
main_msg(s)
    char *s;
{
    printf("   %s\n", s);
}

/*
 * Print messages for "vim -h" or "vim --help" and exit.
 */
    static void
usage()
{
    static char *(use[]) =
    {
        "[file ..]       edit specified file(s)",
        "-               read text from stdin",
    };

    reset_signals();            /* kill us with CTRL-C here, if you like */

    printf("%s\n\nusage:", longVersion);
    for (int i = 0; ; ++i)
    {
        printf(" vim [arguments] %s", use[i]);
        if (i == (sizeof(use) / sizeof(char *)) - 1)
            break;
        printf("\n   or:");
    }

    printf("\n\nArguments:\n");
    main_msg("--\t\t\tOnly file names after this");
    main_msg("-v\t\t\tVi mode (like \"vi\")");
    main_msg("-e\t\t\tEx mode (like \"ex\")");
    main_msg("-E\t\t\tImproved Ex mode");
    main_msg("-s\t\t\tSilent (batch) mode (only for \"ex\")");
    main_msg("-y\t\t\tEasy mode (like \"evim\", modeless)");
    main_msg("-R\t\t\tReadonly mode (like \"view\")");
    main_msg("-Z\t\t\tRestricted mode (like \"rvim\")");
    main_msg("-m\t\t\tModifications (writing files) not allowed");
    main_msg("-M\t\t\tModifications in text not allowed");
    main_msg("-b\t\t\tBinary mode");
    main_msg("-l\t\t\tLisp mode");
    main_msg("-V[N][fname]\t\tBe verbose [level N] [log messages to fname]");
    main_msg("-D\t\t\tDebugging mode");
    main_msg("-n\t\t\tNo swap file, use memory only");
    main_msg("-r\t\t\tList swap files and exit");
    main_msg("-r (with file name)\tRecover crashed session");
    main_msg("-H\t\t\tStart in Hebrew mode");
    main_msg("-T <terminal>\tSet terminal type to <terminal>");
    main_msg("-u <vimrc>\t\tUse <vimrc> instead of any .vimrc");
    main_msg("--noplugin\t\tDon't load plugin scripts");
    main_msg("-p[N]\t\tOpen N tab pages (default: one for each file)");
    main_msg("-o[N]\t\tOpen N windows (default: one for each file)");
    main_msg("-O[N]\t\tLike -o but split vertically");
    main_msg("+\t\t\tStart at end of file");
    main_msg("+<lnum>\t\tStart at line <lnum>");
    main_msg("--cmd <command>\tExecute <command> before loading any vimrc file");
    main_msg("-c <command>\t\tExecute <command> after loading the first file");
    main_msg("-s <scriptin>\tRead Normal mode commands from file <scriptin>");
    main_msg("-w <scriptout>\tAppend all typed commands to file <scriptout>");
    main_msg("-W <scriptout>\tWrite all typed commands to file <scriptout>");
    main_msg("-h  or  --help\tPrint Help (this message) and exit");

    mch_exit(0);
}

/*
 * Check the result of the ATTENTION dialog:
 * When "Quit" selected, exit Vim.
 * When "Recover" selected, recover the file.
 */
    static void
check_swap_exists_action()
{
    if (swap_exists_action == SEA_QUIT)
        getout(1);
    handle_swap_exists(NULL);
}

char    *shortVersion = VIM_VERSION_SHORT;
char    *longVersion = VIM_VERSION_LONG;

static int included_patches[] =
{
    692, 691, 690,
    689, 688, 687, 686, 685, 684, 683, 682, 681, 680,
    679, 678, 677, 676, 675, 674, 673, 672, 671, 670,
    669, 668, 667, 666, 665, 664, 663, 662, 661, 660,
    659, 658, 657, 656, 655, 654, 653, 652, 651, 650,
    649, 648, 647, 646, 645, 644, 643, 642, 641, 640,
    639, 638, 637, 636, 635, 634, 633, 632, 631, 630,
    629, 628, 627, 626, 625, 624, 623, 622, 621, 620,
    619, 618, 617, 616, 615, 614, 613, 612, 611, 610,
    609, 608, 607, 606, 605, 604, 603, 602, 601, 600,
    599, 598, 597, 596, 595, 594, 593, 592, 591, 590,
    589, 588, 587, 586, 585, 584, 583, 582, 581, 580,
    579, 578, 577, 576, 575, 574, 573, 572, 571, 570,
    569, 568, 567, 566, 565, 564, 563, 562, 561, 560,
    559, 558, 557, 556, 555, 554, 553, 552, 551, 550,
    549, 548, 547, 546, 545, 544, 543, 542, 541, 540,
    539, 538, 537, 536, 535, 534, 533, 532, 531, 530,
    529, 528, 527, 526, 525, 524, 523, 522, 521, 520,
    519, 518, 517, 516, 515, 514, 513, 512, 511, 510,
    509, 508, 507, 506, 505, 504, 503, 502, 501, 500,
    499, 498, 497, 496, 495, 494, 493, 492, 491, 490,
    489, 488, 487, 486, 485, 484, 483, 482, 481, 480,
    479, 478, 477, 476, 475, 474, 473, 472, 471, 470,
    469, 468, 467, 466, 465, 464, 463, 462, 461, 460,
    459, 458, 457, 456, 455, 454, 453, 452, 451, 450,
    449, 448, 447, 446, 445, 444, 443, 442, 441, 440,
    439, 438, 437, 436, 435, 434, 433, 432, 431, 430,
    429, 428, 427, 426, 425, 424, 423, 422, 421, 420,
    419, 418, 417, 416, 415, 414, 413, 412, 411, 410,
    409, 408, 407, 406, 405, 404, 403, 402, 401, 400,
    399, 398, 397, 396, 395, 394, 393, 392, 391, 390,
    389, 388, 387, 386, 385, 384, 383, 382, 381, 380,
    379, 378, 377, 376, 375, 374, 373, 372, 371, 370,
    369, 368, 367, 366, 365, 364, 363, 362, 361, 360,
    359, 358, 357, 356, 355, 354, 353, 352, 351, 350,
    349, 348, 347, 346, 345, 344, 343, 342, 341, 340,
    339, 338, 337, 336, 335, 334, 333, 332, 331, 330,
    329, 328, 327, 326, 325, 324, 323, 322, 321, 320,
    319, 318, 317, 316, 315, 314, 313, 312, 311, 310,
    309, 308, 307, 306, 305, 304, 303, 302, 301, 300,
    299, 298, 297, 296, 295, 294, 293, 292, 291, 290,
    289, 288, 287, 286, 285, 284, 283, 282, 281, 280,
    279, 278, 277, 276, 275, 274, 273, 272, 271, 270,
    269, 268, 267, 266, 265, 264, 263, 262, 261, 260,
    259, 258, 257, 256, 255, 254, 253, 252, 251, 250,
    249, 248, 247, 246, 245, 244, 243, 242, 241, 240,
    239, 238, 237, 236, 235, 234, 233, 232, 231, 230,
    229, 228, 227, 226, 225, 224, 223, 222, 221, 220,
    219, 218, 217, 216, 215, 214, 213, 212, 211, 210,
    209, 208, 207, 206, 205, 204, 203, 202, 201, 200,
    199, 198, 197, 196, 195, 194, 193, 192, 191, 190,
    189, 188, 187, 186, 185, 184, 183, 182, 181, 180,
    179, 178, 177, 176, 175, 174, 173, 172, 171, 170,
    169, 168, 167, 166, 165, 164, 163, 162, 161, 160,
    159, 158, 157, 156, 155, 154, 153, 152, 151, 150,
    149, 148, 147, 146, 145, 144, 143, 142, 141, 140,
    139, 138, 137, 136, 135, 134, 133, 132, 131, 130,
    129, 128, 127, 126, 125, 124, 123, 122, 121, 120,
    119, 118, 117, 116, 115, 114, 113, 112, 111, 110,
    109, 108, 107, 106, 105, 104, 103, 102, 101, 100,
    99, 98, 97, 96, 95, 94, 93, 92, 91, 90,
    89, 88, 87, 86, 85, 84, 83, 82, 81, 80,
    79, 78, 77, 76, 75, 74, 73, 72, 71, 70,
    69, 68, 67, 66, 65, 64, 63, 62, 61, 60,
    59, 58, 57, 56, 55, 54, 53, 52, 51, 50,
    49, 48, 47, 46, 45, 44, 43, 42, 41, 40,
    39, 38, 37, 36, 35, 34, 33, 32, 31, 30,
    29, 28, 27, 26, 25, 24, 23, 22, 21, 20,
    19, 18, 17, 16, 15, 14, 13, 12, 11, 10,
    9, 8, 7, 6, 5, 4, 3, 2, 1, 0
};

/*
 * Return TRUE if patch "n" has been included.
 */
    int
has_patch(n)
    int         n;
{
    for (int i = 0; included_patches[i] != 0; ++i)
        if (included_patches[i] == n)
            return TRUE;

    return FALSE;
}

/*
 * Show the intro message when not editing a file.
 */
    void
maybe_intro_message()
{
    if (bufempty()
            && curbuf->b_fname == NULL
            && firstwin->w_next == NULL
            && vim_strchr(p_shm, SHM_INTRO) == NULL)
        intro_message();
}

/*
 * Give an introductory message about Vim.
 * Only used when starting Vim on an empty file, without a file name.
 */
    void
intro_message()
{
}
