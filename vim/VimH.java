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
public class VimH
{
    /*
     * os_unix.c --------------------------------------------------------------------------------------
     */

    /* volatile because it is used in signal handler sig_winch(). */
    /*private*/ static /*volatile*/transient boolean do_resize;
    /* volatile because it is used in signal handler deathtrap(). */
    /*private*/ static /*volatile*/transient int deadly_signal;                  /* the signal we caught */
    /* volatile because it is used in signal handler deathtrap(). */
    /*private*/ static /*volatile*/transient boolean in_mch_delay;                   /* sleeping in mch_delay() */

    /*private*/ static int curr_tmode = TMODE_COOK;                 /* contains current terminal mode */

    /*private*/ static final class signalinfo_C
    {
        int     sig;        /* Signal number, eg. SIGSEGV etc. */
        Bytes name;       /* Signal name. */
        boolean deadly;     /* Catch as a deadly signal? */

        /*private*/ signalinfo_C(int sig, Bytes name, boolean deadly)
        {
            this.sig = sig;
            this.name = name;
            this.deadly = deadly;
        }
    }

    /*private*/ static signalinfo_C[] signal_info = new signalinfo_C[]
    {
        new signalinfo_C(SIGHUP,    u8("HUP"),      true ),
        new signalinfo_C(SIGQUIT,   u8("QUIT"),     true ),
        new signalinfo_C(SIGILL,    u8("ILL"),      true ),
        new signalinfo_C(SIGTRAP,   u8("TRAP"),     true ),
        new signalinfo_C(SIGABRT,   u8("ABRT"),     true ),
        new signalinfo_C(SIGFPE,    u8("FPE"),      true ),
        new signalinfo_C(SIGBUS,    u8("BUS"),      true ),
        new signalinfo_C(SIGSEGV,   u8("SEGV"),     true ),
        new signalinfo_C(SIGSYS,    u8("SYS"),      true ),
        new signalinfo_C(SIGALRM,   u8("ALRM"),     false),
        new signalinfo_C(SIGTERM,   u8("TERM"),     true ),
        new signalinfo_C(SIGVTALRM, u8("VTALRM"),   true ),
        new signalinfo_C(SIGPROF,   u8("PROF"),     true ),
        new signalinfo_C(SIGXCPU,   u8("XCPU"),     true ),
        new signalinfo_C(SIGXFSZ,   u8("XFSZ"),     true ),
        new signalinfo_C(SIGUSR1,   u8("USR1"),     true ),
        new signalinfo_C(SIGUSR2,   u8("USR2"),     true ),
        new signalinfo_C(SIGINT,    u8("INT"),      false),
        new signalinfo_C(SIGWINCH,  u8("WINCH"),    false),
        new signalinfo_C(SIGTSTP,   u8("TSTP"),     false),
        new signalinfo_C(SIGPIPE,   u8("PIPE"),     false),

        new signalinfo_C(-1,        u8("Unknown!"), false)
    };

    /*private*/ static int mch_chdir(Bytes path)
    {
        if (5 <= p_verbose[0])
        {
            verbose_enter();
            smsg(u8("chdir(%s)"), path);
            verbose_leave();
        }
        return libC.chdir(path);
    }

    /*
     * Write s[len] to the screen.
     */
    /*private*/ static void mch_write(Bytes s, int len)
    {
        libC.write(1, s, len);
        if (p_wd[0] != 0)           /* Unix is too fast, slow down a bit more */
            realWaitForChar(read_cmd_fd, p_wd[0]);
    }

    /*
     * mch_inchar(): low level input function.
     * Get a characters from the keyboard.
     * Return the number of characters that are available.
     * If wtime == 0 do not wait for characters.
     * If wtime == n wait a short time for characters.
     * If wtime == -1 wait forever for characters.
     */
    /*private*/ static int mch_inchar(Bytes buf, int maxlen, long wtime, int tb_change_cnt)
        /* wtime: don't use "time", MIPS cannot handle it */
    {
        /* Check if window changed size while we were busy, perhaps the ":set columns=99" command was used. */
        while (do_resize)
            handle_resize();

        if (0 <= wtime)
        {
            while (waitForChar(wtime) == false)         /* no character available */
            {
                if (!do_resize)                     /* return if not interrupted by resize */
                    return 0;
                handle_resize();
            }
        }
        else        /* wtime == -1 */
        {
            /*
             * If there is no character available within 'updatetime' seconds
             * flush all the swap files to disk.
             * Also done when interrupted by SIGWINCH.
             */
            if (waitForChar(p_ut[0]) == false)
            {
                if (trigger_cursorhold() && 3 <= maxlen && !typebuf_changed(tb_change_cnt))
                {
                    buf.be(0, KB_SPECIAL);
                    buf.be(1, KS_EXTRA);
                    buf.be(2, KE_CURSORHOLD);
                    return 3;
                }
                before_blocking();
            }
        }

        for ( ; ; )                                 /* repeat until we got a character */
        {
            while (do_resize)                       /* window changed size */
                handle_resize();

            /*
             * We want to be interrupted by the winch signal
             * or by an event on the monitored file descriptors.
             */
            if (waitForChar(-1L) == false)
            {
                if (do_resize)                      /* interrupted by SIGWINCH signal */
                    handle_resize();
                return 0;
            }

            /* If input was put directly in typeahead buffer bail out here. */
            if (typebuf_changed(tb_change_cnt))
                return 0;

            /*
             * For some terminals we only get one character at a time.
             * We want the get all available characters, so we could keep on trying until none is available
             * For some other terminals this is quite slow, that's why we don't do it.
             */
            int len = read_from_input_buf(buf, maxlen);
            if (0 < len)
                return len;
        }
    }

    /*private*/ static void handle_resize()
    {
        do_resize = false;
        shell_resized();
    }

    /*private*/ static void mch_delay(long msec, boolean ignoreinput)
    {
        if (ignoreinput)
        {
            /* Go to cooked mode without echo, to allow SIGINT interrupting us here.
             * But we don't want QUIT to kill us (CTRL-\ used in a shell may produce SIGQUIT). */
            in_mch_delay = true;
            int old_tmode = curr_tmode;
            if (curr_tmode == TMODE_RAW)
                settmode(TMODE_SLEEP);

            /*
             * Everybody sleeps in a different way...
             * Prefer nanosleep(), some versions of usleep() can only sleep up to one second.
             */
            {
                timespec_C ts = new timespec_C();

                ts.tv_sec(msec / 1000);
                ts.tv_nsec((msec % 1000) * 1000000);
                libc.nanosleep(ts, null);
            }

            settmode(old_tmode);
            in_mch_delay = false;
        }
        else
            waitForChar(msec);
    }

    /*
     * We need correct prototypes for a signal function, otherwise mean compilers
     * will barf when the second argument to sigset() is ``wrong''.
     */
    /*private*/ static void sig_winch(int _sigarg)
    {
        /* this is not required on all systems, but it doesn't hurt anybody */
        libC.sigset(SIGWINCH, /*(void (*)())sig_winch*/null);
        do_resize = true;
    }

    /*private*/ static void catch_sigint(int _sigarg)
    {
        /* this is not required on all systems, but it doesn't hurt anybody */
        libC.sigset(SIGINT, /*(void (*)())catch_sigint*/null);
        got_int = true;
    }

    /*private*/ static void catch_sigpwr(int _sigarg)
    {
        /* this is not required on all systems, but it doesn't hurt anybody */
        libC.sigset(SIGPWR, /*(void (*)())catch_sigpwr*/null);
    }

    /*private*/ static int trap__entered;       /* Count the number of times we got here.
                                             * Note: when memory has been corrupted
                                             * this may get an arbitrary value! */

    /*private*/ static void may_core_dump()
    {
        if (deadly_signal != 0)
        {
            libC.sigset(deadly_signal, /*SIG_DFL*/null);
            libc.kill(libc.getpid(), deadly_signal);          /* Die using the signal we caught */
        }
    }

    /*
     * This function handles deadly signals.
     * It tries to preserve any swap files and exit properly.
     * NOTE: Avoid unsafe functions, such as allocating memory, they can result in a deadlock.
     */
    /*private*/ static void deathtrap(int sigarg)
    {
        /* While in mch_delay() we go to cooked mode to allow a CTRL-C to interrupt us.
         * But in cooked mode we may also get SIGQUIT, e.g., when pressing CTRL-\,
         * but we don't want Vim to exit then. */
        if (in_mch_delay && sigarg == SIGQUIT)
            return;

        /* When SIGHUP, SIGQUIT, etc. are blocked: postpone the effect and return here.
         * This avoids that a non-reentrant function is interrupted, e.g. free().
         * Calling free() again may then cause a crash. */
        if (trap__entered == 0
                && (sigarg == SIGHUP
                 || sigarg == SIGQUIT
                 || sigarg == SIGTERM
                 || sigarg == SIGPWR
                 || sigarg == SIGUSR1
                 || sigarg == SIGUSR2)
                && !vim_handle_signal(sigarg))
            return;

        /* Remember how often we have been called. */
        trap__entered++;

        /* Set the v:dying variable. */
        set_vim_var_nr(VV_DYING, trap__entered);

        int i;

        /* try to find the name of this signal */
        for (i = 0; signal_info[i].sig != -1; i++)
            if (sigarg == signal_info[i].sig)
                break;
        deadly_signal = sigarg;

        full_screen = false; /* don't write message to the GUI, it might be part of the problem... */
        /*
         * If something goes wrong after entering here, we may get here again.
         * When this happens, give a message and try to exit nicely (resetting the terminal mode, etc.)
         * When this happens twice, just exit, don't even try to give a message,
         * stack may be corrupt or something weird.
         * When this still happens again (or memory was corrupted in such a way
         * that "trap__entered" was clobbered) use _exit(), don't try freeing resources.
         */
        if (3 <= trap__entered)
        {
            reset_signals();        /* don't catch any signals anymore */
            may_core_dump();
            if (4 <= trap__entered)
                libc._exit(8);
            libc.exit(7);
        }
        if (trap__entered == 2)
        {
            out_str(u8("Vim: Double signal, exiting\n"));
            out_flush();
            getout(1);
        }

        libC.sprintf(ioBuff, u8("Vim: Caught deadly signal %s\n"), signal_info[i].name);

        /* Preserve files and exit. */
        preserve_exit();
    }

    /*
     * If the machine has job control, use it to suspend the program,
     * otherwise fake it by starting a new shell.
     */
    /*private*/ static void mch_suspend()
    {
        out_flush();                        /* needed to make cursor visible on some systems */
        settmode(TMODE_COOK);
        out_flush();                        /* needed to disable mouse on some systems */

        libc.kill(0, SIGTSTP);                   /* send ourselves a STOP signal */

        settmode(TMODE_RAW);
        need_check_timestamps = true;
        did_check_timestamps = false;
    }

    /*private*/ static void mch_init()
    {
        Columns[0] = 80;
        Rows[0] = 24;

        out_flush();
        set_signals();
    }

    /*private*/ static void set_signals()
    {
        /*
         * WINDOW CHANGE signal is handled with sig_winch().
         */
        libC.sigset(SIGWINCH, /*(void (*)())sig_winch*/null);

        /*
         * We want the STOP signal to work, to make mch_suspend() work.
         * For "rvim" the STOP signal is ignored.
         */
        libC.sigset(SIGTSTP, /*restricted ? SIG_IGN : SIG_DFL*/null);

        /*
         * We want to ignore breaking of PIPEs.
         */
        libC.sigset(SIGPIPE, /*SIG_IGN*/null);

        catch_int_signal();

        /*
         * Ignore alarm signals (Perl's alarm() generates it).
         */
        libC.sigset(SIGALRM, /*SIG_IGN*/null);

        /*
         * Catch SIGPWR (power failure?) to preserve the swap files, so that no work will be lost.
         */
        libC.sigset(SIGPWR, /*(void (*)())catch_sigpwr*/null);

        /*
         * Arrange for other signals to gracefully shutdown Vim.
         */
        catch_signals(/*deathtrap*//*null*/0, SIG_ERR);
    }

    /*
     * Catch CTRL-C (only works while in Cooked mode).
     */
    /*private*/ static void catch_int_signal()
    {
        libC.sigset(SIGINT, /*(void (*)())catch_sigint*/null);
    }

    /*private*/ static void reset_signals()
    {
        catch_signals(SIG_DFL, SIG_DFL);
    }

    /*private*/ static void catch_signals/*(void (*func_deadly)(), void (*func_other)())*/(@sighandler_t long func_deadly, @sighandler_t long func_other)
    {
        for (int i = 0; signal_info[i].sig != -1; i++)
        {
            if (signal_info[i].deadly)
            {
             // sigaction_C sa = new sigaction_C();

                /* Setup to use the alternate stack for the signal function. */
             // sa.sa_handler(func_deadly);
                libC.sigemptyset(/*&sa.sa_mask*/null);
             // sa.sa_flags(SA_ONSTACK);
                libC.sigaction(signal_info[i].sig, /*sa*/null, null);
            }
            else if (func_other != SIG_ERR)
                libC.sigset(signal_info[i].sig, /*func_other*/null);
        }
    }

    /*private*/ static int got_signal;
    /*private*/ static boolean __blocked = true;

    /*
     * Handling of SIGHUP, SIGQUIT and SIGTERM:
     * "when" == a signal:       when busy, postpone and return false, otherwise return true
     * "when" == SIGNAL_BLOCK:   Going to be busy, block signals
     * "when" == SIGNAL_UNBLOCK: Going to wait, unblock signals, use postponed signal
     * Returns true when Vim should exit.
     */
    /*private*/ static boolean vim_handle_signal(int sig)
    {
        switch (sig)
        {
            case SIGNAL_BLOCK:
            {
                __blocked = true;
                break;
            }
            case SIGNAL_UNBLOCK:
            {
                __blocked = false;
                if (got_signal != 0)
                {
                    libc.kill(libc.getpid(), got_signal);
                    got_signal = 0;
                }
                break;
            }
            default:
            {
                if (!__blocked)
                    return true;            /* exit! */
                got_signal = sig;
                if (sig != SIGPWR)
                    got_int = true;         /* break any loops */
                break;
            }
        }
        return false;
    }

    /*
     * Check_win checks whether we have an interactive stdout.
     */
    /*private*/ static boolean mch_output_isatty()
    {
        return (libc.isatty(1) != 0);
    }

    /*
     * Return true if the input comes from a terminal, false otherwise.
     */
    /*private*/ static boolean mch_input_isatty()
    {
        return (libc.isatty(read_cmd_fd) != 0);
    }

    /*
     * Return true if "name" looks like some xterm name.
     */
    /*private*/ static boolean vim_is_xterm(Bytes name)
    {
        if (name == null)
            return false;

        return (STRNCASECMP(name, u8("xterm"), 5) == 0
             || STRNCASECMP(name, u8("rxvt"), 4) == 0
             || STRCMP(name, u8("builtin_xterm")) == 0);
    }

    /*
     * Return true if "name" appears to be that of a terminal
     * known to support the xterm-style mouse protocol.
     * Relies on term_is_xterm having been set to its correct value.
     */
    /*private*/ static boolean use_xterm_like_mouse(Bytes name)
    {
        return (name != null && (term_is_xterm || STRNCASECMP(name, u8("screen"), 6) == 0));
    }

    /*
     * Return non-zero when using an xterm mouse, according to 'ttymouse'.
     * Return 1 for "xterm".
     * Return 2 for "xterm2".
     */
    /*private*/ static int use_xterm_mouse()
    {
        if (ttym_flags[0] == TTYM_XTERM2)
            return 2;
        if (ttym_flags[0] == TTYM_XTERM)
            return 1;

        return 0;
    }

    /*private*/ static boolean vim_is_vt300(Bytes name)
    {
        if (name == null)
            return false;           /* actually all ANSI comp. terminals should be here */

        /* catch VT100 - VT5xx */
        return ((STRNCASECMP(name, u8("vt"), 2) == 0 && vim_strbyte(u8("12345"), name.at(2)) != null)
            || STRCMP(name, u8("builtin_vt320")) == 0);
    }

    /*
     * Return true if "name" is a terminal for which 'ttyfast' should be set.
     * This should include all windowed terminal emulators.
     */
    /*private*/ static boolean vim_is_fastterm(Bytes name)
    {
        if (name == null)
            return false;
        if (vim_is_xterm(name) || vim_is_vt300(name))
            return true;

        return (STRNCASECMP(name, u8("screen"), 6) == 0);
    }

    /*
     * Get name of current directory into buffer 'buf' of length 'len' bytes.
     * Return true for success, false for failure.
     */
    /*private*/ static boolean mch_dirname(Bytes buf, int len)
    {
        if (!libC._getcwd(buf, len))
        {
            STRCPY(buf, libC.strerror(libC.errno()));
            return false;
        }
        return true;
    }

    /*private*/ static boolean dont_fchdir;     /* true when fchdir() doesn't work */

    /*
     * Get absolute file name into "buf[len]".
     *
     * return false for failure, true for success
     */
    /*private*/ static boolean mch_fullName(Bytes fname, Bytes buf, int len, boolean force)
        /* force: also expand when already absolute path */
    {
        boolean retval = true;

        /* expand it if forced or not an absolute path */
        if (force || !mch_isFullName(fname))
        {
            int fd = -1;
            Bytes olddir = new Bytes(MAXPATHL);

            /*
             * If the file name has a path, change to that directory for a moment,
             * and then do the getwd() (and get back to where we were).
             * This will get the correct path name with "../" things.
             */
            Bytes p = vim_strrchr(fname, (byte)'/');
            if (p != null)
            {
                /*
                 * Use fchdir() if possible, it's said to be faster and more reliable.
                 * But on SunOS 4 it might not work.  Check this by doing a fchdir() right now.
                 */
                if (!dont_fchdir)
                {
                    fd = libC.open(u8("."), O_RDONLY, 0);
                    if (0 <= fd && libc.fchdir(fd) < 0)
                    {
                        libc.close(fd);
                        fd = -1;
                        dont_fchdir = true;     /* don't try again */
                    }
                }

                /* Only change directory when we are sure we can return to where we are now.
                 * After doing "su" chdir(".") might not work. */
                if (fd < 0 && (mch_dirname(olddir, MAXPATHL) == false || mch_chdir(olddir) != 0))
                {
                    p = null;       /* can't get current dir: don't chdir */
                    retval = false;
                }
                else
                {
                    /* The directory is copied into buf[], to be able to remove the file name
                     * without changing it (could be a string in read-only memory) */
                    if (len <= BDIFF(p, fname))
                        retval = false;
                    else
                    {
                        vim_strncpy(buf, fname, BDIFF(p, fname));
                        if (mch_chdir(buf) != 0)
                            retval = false;
                        else
                            fname = p.plus(1);
                        buf.be(0, NUL);
                    }
                }
            }
            if (mch_dirname(buf, len) == false)
            {
                retval = false;
                buf.be(0, NUL);
            }
            if (p != null)
            {
                int l;
                if (0 <= fd)
                {
                    if (5 <= p_verbose[0])
                    {
                        verbose_enter();
                        msg(u8("fchdir() to previous dir"));
                        verbose_leave();
                    }
                    l = libc.fchdir(fd);
                    libc.close(fd);
                }
                else
                    l = mch_chdir(olddir);
                if (l != 0)
                    emsg(e_prev_dir);
            }

            int l = strlen(buf);
            if (len - 1 <= l)
                retval = false; /* no space for trailing "/" */
            else if (0 < l && buf.at(l - 1) != (byte)'/' && fname.at(0) != NUL && STRCMP(fname, u8(".")) != 0)
                STRCAT(buf, u8("/"));
        }

        /* Catch file names which are too long. */
        if (retval == false || len <= strlen(buf) + strlen(fname))
            return false;

        /* Do not append ".", "/dir/." is equal to "/dir". */
        if (STRCMP(fname, u8(".")) != 0)
            STRCAT(buf, fname);

        return true;
    }

    /*
     * Return true if "fname" does not depend on the current directory.
     */
    /*private*/ static boolean mch_isFullName(Bytes fname)
    {
        return (fname.at(0) == (byte)'/' || fname.at(0) == (byte)'~');
    }

    /*
     * Get file permissions for 'name'.
     * Returns -1 when it doesn't exist.
     */
    /*private*/ static int mch_getperm(Bytes name)
    {
        stat_C st = new stat_C();

        if (libC.stat(name, st) != 0)
            return -1;

        return st.st_mode();
    }

    /*
     * set file permission for 'name' to 'perm'
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean mch_setperm(Bytes name, int perm)
    {
        return (libC.chmod(name, perm) == 0);
    }

    /*
     * return true if "name" is a directory
     * return false if "name" is not a directory
     * return false for error
     */
    /*private*/ static boolean mch_isdir(Bytes name)
    {
        if (name.at(0) == NUL)       /* Some stat()s don't flag "" as an error. */
            return false;

        stat_C st = new stat_C();
        if (libC.stat(name, st) != 0)
            return false;

        return S_ISDIR(st.st_mode());
    }

    /*
     * Check what "name" is:
     * NODE_NORMAL: file or directory (or doesn't exist)
     * NODE_WRITABLE: writable device, socket, fifo, etc.
     * NODE_OTHER: non-writable things
     */
    /*private*/ static int mch_nodetype(Bytes name)
    {
        stat_C st = new stat_C();

        if (libC.stat(name, st) != 0)
            return NODE_NORMAL;
        if (S_ISREG(st.st_mode()) || S_ISDIR(st.st_mode()))
            return NODE_NORMAL;
        if (S_ISBLK(st.st_mode()))  /* block device isn't writable */
            return NODE_OTHER;
        /* Everything else is writable? */
        return NODE_WRITABLE;
    }

    /*
     * Output a newline when exiting.
     * Make sure the newline goes to the same stream as the text.
     */
    /*private*/ static void exit_scroll()
    {
        if (silent_mode)
            return;
        if (newline_on_exit || msg_didout)
        {
            if (msg_use_printf())
            {
                if (info_message)
                    libC.fprintf(stdout, u8("\n"));
                else
                    libC.fprintf(stderr, u8("\r\n"));
            }
            else
                out_char((byte)'\n');
        }
        else
        {
            restore_cterm_colors();         /* get original colors back */
            msg_clr_eos_force();            /* clear the rest of the display */
            windgoto((int)Rows[0] - 1, 0);     /* may have moved the cursor */
        }
    }

    /*private*/ static void mch_exit(int r)
    {
        exiting = true;

        settmode(TMODE_COOK);

        /*
         * When t_ti is not empty, but it doesn't cause swapping terminal pages,
         * need to output a newline when msg_didout is set.
         * But when t_ti does swap pages, it should not go to the shell page.
         * Do this before stoptermcap().
         */
        if (swapping_screen() && !newline_on_exit)
            exit_scroll();

        /* Stop termcap: may need to check for T_CRV response,
         * which requires RAW mode. */
        stoptermcap();

        /* A newline is only required after a message in the alternate screen.
         * This is set to true by wait_return(). */
        if (!swapping_screen() || newline_on_exit)
            exit_scroll();

        /* Cursor may have been switched off without calling starttermcap()
         * when doing "vim -u vimrc" and vimrc contains ":q". */
        if (full_screen)
            cursor_on();

        out_flush();
        ml_close_all();
        may_core_dump();

        throw new Error("exit! " + r);
     // libc.exit(r);
    }

    /*
     * for "new" tty systems
     */
    /*private*/ static termios_C stm__told;

    /*private*/ static void mch_settmode(int tmode)
    {
        if (stm__told == null)
        {
            stm__told = new termios_C();
            libc.tcgetattr(read_cmd_fd, stm__told);
        }

        termios_C tnew = new termios_C();
        COPY_termios(tnew, stm__told);

        if (tmode == TMODE_RAW)
        {
            /*
             * ~ICRNL enables typing ^V^M
             */
            tnew.c_iflag(tnew.c_iflag() & ~(ICRNL));
            tnew.c_lflag(tnew.c_lflag() & ~(ICANON | ECHO | ISIG | ECHOE | IEXTEN));
            tnew.c_oflag(tnew.c_oflag() & ~(ONLCR));
            tnew.c_vmin((short)1);      /* return after 1 char */
            tnew.c_vtime((short)0);     /* don't wait */
        }
        else if (tmode == TMODE_SLEEP)
            tnew.c_lflag(tnew.c_lflag() & ~(ECHO));

        /* A signal may cause tcsetattr() to fail (e.g., SIGCONT).  Retry a few times. */
        for (int n = 10; libc.tcsetattr(read_cmd_fd, TCSANOW, tnew) == -1 && libC.errno() == EINTR && 0 < n; )
            --n;

        curr_tmode = tmode;
    }

    /*
     * Try to get the code for "t_kb" from the stty setting
     *
     * Even if termcap claims a backspace key, the user's setting *should*
     * prevail.  stty knows more about reality than termcap does, and if
     * somebody's usual erase key is DEL (which, for most BSD users, it will
     * be), they're going to get really annoyed if their erase key starts
     * doing forward deletes for no reason.
     */
    /*private*/ static void get_stty()
    {
        /* for "new" tty systems */
        termios_C keys = new termios_C();

        if (libc.tcgetattr(read_cmd_fd, keys) != -1)
        {
            Bytes buf = new Bytes(2);

            buf.be(0, keys.c_verase());
            intr_char = keys.c_vintr();
            buf.be(1, NUL);
            add_termcode(u8("kb"), buf, FALSE);

            /*
             * If <BS> and <DEL> are now the same, redefine <DEL>.
             */
            Bytes p = find_termcode(u8("kD"));
            if (p != null && p.at(0) == buf.at(0) && p.at(1) == buf.at(1))
                ex_fixdel.ex(null);
        }
    }

    /*private*/ static boolean sm__ison;

    /*
     * Set mouse clicks on or off.
     */
    /*private*/ static void mch_setmouse(boolean on)
    {
        if (on == sm__ison)     /* return quickly if nothing to do */
            return;

        int xterm_mouse_vers = use_xterm_mouse();

        if (0 < xterm_mouse_vers)
        {
            if (on) /* enable mouse events, use mouse tracking if available */
                out_str_nf((1 < xterm_mouse_vers) ? u8("\033[?1002h") : u8("\033[?1000h"));
            else    /* disable mouse events, could probably always send the same */
                out_str_nf((1 < xterm_mouse_vers) ? u8("\033[?1002l") : u8("\033[?1000l"));
            sm__ison = on;
        }
    }

    /*
     * Set the mouse termcode, depending on the 'term' and 'ttymouse' options.
     */
    /*private*/ static void check_mouse_termcode()
    {
        if (use_xterm_mouse() != 0)
        {
            set_mouse_termcode(KS_MOUSE, term_is_8bit(T_NAME[0]) ? u8("\233M") : u8("\033[M"));
            if (p_mouse[0].at(0) != NUL)
            {
                /* Force mouse off and maybe on to send possibly new mouse
                 * activation sequence to the xterm, with(out) drag tracing. */
                mch_setmouse(false);
                setmouse();
            }
        }
        else
            del_mouse_termcode(KS_MOUSE);
    }

    /*
     * set screen mode, always fails.
     */
    /*private*/ static boolean mch_screenmode(Bytes _arg)
    {
        emsg(e_screenmode);
        return false;
    }

    /*
     * Try to get the current window size:
     * 1. with an ioctl(), most accurate method
     * 2. from the environment variables LINES and COLUMNS
     * 3. from the termcap
     * 4. keep using the old values
     * Return true when size could be determined, false otherwise.
     */
    /*private*/ static boolean mch_get_shellsize()
    {
        long rows = 0;
        long columns = 0;

        /*
         * 1. try using an ioctl.  It is the most accurate method.
         *
         * Try using TIOCGWINSZ first, some systems that have it also define
         * TIOCGSIZE but don't have a struct ttysize.
         */
        {
            winsize_C ws = new winsize_C();
            int fd = 1;

            /* When stdout is not a tty, use stdin for the ioctl(). */
            if (libc.isatty(fd) == 0 && libc.isatty(read_cmd_fd) != 0)
                fd = read_cmd_fd;
            if (libc.ioctl(fd, TIOCGWINSZ, ws) == 0)
            {
                rows = ws.ws_row();
                columns = ws.ws_col();
            }
        }

        /*
         * 2. get size from environment
         *    When being POSIX compliant ('|' flag in 'cpoptions') this overrules
         *    the ioctl() values!
         */
        if (columns == 0 || rows == 0 || vim_strbyte(p_cpo[0], CPO_TSIZE) != null)
        {
            Bytes p;
            if ((p = libC.getenv(u8("LINES"))) != null)
                rows = libC.atoi(p);
            if ((p = libC.getenv(u8("COLUMNS"))) != null)
                columns = libC.atoi(p);
        }

        /*
         * 4. If everything fails, use the old values
         */
        if (columns <= 0 || rows <= 0)
            return false;

        Rows[0] = rows;
        Columns[0] = columns;
        limit_screen_size();
        return true;
    }

    /*
     * Try to set the window size to Rows and Columns.
     */
    /*private*/ static void mch_set_shellsize()
    {
        if (T_CWS[0].at(0) != NUL)
        {
            /*
             * NOTE: if you get an error here that term_set_winsize() is undefined,
             * check the output of configure.  It could probably not find a ncurses,
             * termcap or termlib library.
             */
            term_set_winsize((int)Rows[0], (int)Columns[0]);
            out_flush();
            screen_start();                 /* don't know where cursor is now */
        }
    }

    /*
     * Rows and/or Columns has changed.
     */
    /*private*/ static void mch_new_shellsize()
    {
        /* Nothing to do. */
    }

    /*
     * Check for CTRL-C typed by reading all available characters.
     * In cooked mode we should get SIGINT, no need to check.
     */
    /*private*/ static void mch_breakcheck()
    {
        if (curr_tmode == TMODE_RAW && realWaitForChar(read_cmd_fd, 0L))
            fill_input_buf(false);
    }

    /*
     * Wait "msec" msec until a character is available from the keyboard or from inbuf[].
     * "msec" == -1 will block forever.
     */
    /*private*/ static boolean waitForChar(long msec)
    {
        if (input_available())          /* something in inbuf[] */
            return true;

        return realWaitForChar(read_cmd_fd, msec);
    }

    /*
     * Wait "msec" msec until a character is available from file descriptor "fd".
     * "msec" == 0 will check for characters once.
     * "msec" == -1 will block until a character is available.
     */
    /*private*/ static boolean realWaitForChar(int fd, long msec)
    {
        timeval_C tv = new timeval_C();
        if (0 <= msec)
        {
            tv.tv_sec(msec / 1000);
            tv.tv_usec((msec % 1000) * 1000);
        }

        /*
         * Select on ready for reading and exceptional condition (end of file).
         */
        for (long[] rfds = new long[FD_SET_LENGTH], efds = new long[FD_SET_LENGTH]; ; )
        {
            FD_ZERO(rfds);
            FD_ZERO(efds);
            FD_SET(fd, rfds);
            FD_SET(fd, efds);

            int ret = libc.select(fd + 1, rfds, null, efds, (msec < 0) ? null : tv);

            if (ret == -1 && libC.errno() == EINTR)
            {
                /* Check whether window has been resized, EINTR may be caused by SIGWINCH. */
                if (do_resize)
                    handle_resize();

                /* Interrupted by a signal, need to try again.  We ignore msec
                 * here, because we do want to check even after a timeout if
                 * characters are available.  Needed for reading output of an
                 * external command after the process has finished. */
                continue;
            }

            return (0 < ret);
        }

        /* NOTREACHED */
    }

    /*
     * message.c: functions for displaying messages on the command line -------------------------------
     */

    /*private*/ static int      confirm_msg_used;       /* displaying "confirm_msg" */
    /*private*/ static Bytes    confirm_msg;            /* ":confirm" message */
    /*private*/ static Bytes    confirm_msg_tail;       /* tail of "confirm_msg" */

    /*private*/ static final class msg_hist_C
    {
        msg_hist_C      next;
        Bytes           msg;
        int             attr;

        /*private*/ msg_hist_C()
        {
        }
    }

    /*private*/ static msg_hist_C first_msg_hist;
    /*private*/ static msg_hist_C last_msg_hist;
    /*private*/ static int msg_hist_len;

    /*private*/ static file_C verbose_fd;
    /*private*/ static boolean verbose_did_open;

    /*
     * When writing messages to the screen, there are many different situations.
     * A number of variables is used to remember the current state:
     * msg_didany       true when messages were written since the last time the user reacted to a prompt.
     *                  Reset: After hitting a key for the hit-return prompt,
     *                  hitting <CR> for the command line or input().
     *                  Set: When any message is written to the screen.
     * msg_didout       true when something was written to the current line.
     *                  Reset: When advancing to the next line, when the current text can be overwritten.
     *                  Set: When any message is written to the screen.
     * msg_nowait       No extra delay for the last drawn message.
     *                  Used in normal_cmd() before the mode message is drawn.
     * emsg_on_display  There was an error message recently.
     *                  Indicates that there should be a delay before redrawing.
     * msg_scroll       The next message should not overwrite the current one.
     * msg_scrolled     How many lines the screen has been scrolled (because of messages).
     *                  Used in update_screen() to scroll the screen back.
     *                  Incremented each time the screen scrolls a line.
     * msg_scrolled_ign true when msg_scrolled is non-zero and msg_puts_attr() writes something
     *                  without scrolling should not make need_wait_return to be set.
     *                  This is a hack to make ":ts" work without an extra prompt.
     * lines_left       Number of lines available for messages before the more-prompt is to be given.
     *                  -1 when not set.
     * need_wait_return true when the hit-return prompt is needed.
     *                  Reset: After giving the hit-return prompt, when the user has answered some other prompt.
     *                  Set: When the ruler or typeahead display is overwritten,
     *                  scrolling the screen for some message.
     * keep_msg         Message to be displayed after redrawing the screen, in main_loop().
     *                  This is an allocated string or null when not used.
     */

    /*
     * msg(s) - displays the string 's' on the status line.
     * Return true if wait_return not called.
     */
    /*private*/ static boolean msg(Bytes s)
    {
        return msg_attr_keep(s, 0, false);
    }

    /*
     * Like msg() but keep it silent when 'verbosefile' is set.
     */
    /*private*/ static boolean verb_msg(Bytes s)
    {
        boolean b;

        verbose_enter();
        b = msg_attr_keep(s, 0, false);
        verbose_leave();

        return b;
    }

    /*private*/ static boolean msg_attr(Bytes s, int attr)
    {
        return msg_attr_keep(s, attr, false);
    }

    /*private*/ static int msg__entered;

    /*private*/ static boolean msg_attr_keep(Bytes s, int attr, boolean keep)
        /* keep: true: set "keep_msg" if it doesn't scroll */
    {
        if (attr == 0)
            set_vim_var_string(VV_STATUSMSG, s, -1);

        /*
         * It is possible that displaying a messages causes a problem
         * (e.g. when redrawing the window), which causes another message, etc.
         * To break this loop, limit the recursiveness to 3 levels.
         */
        if (3 <= msg__entered)
            return true;
        msg__entered++;

        /* Add message to history (unless it's a repeated kept message or a truncated message) */
        if (BNE(s, keep_msg)
                || (s.at(0) != (byte)'<'
                    && last_msg_hist != null
                    && last_msg_hist.msg != null
                    && STRCMP(s, last_msg_hist.msg) != 0))
            add_msg_hist(s, -1, attr);

        /* When displaying "keep_msg", don't let msg_start() free it, caller must do that. */
        if (BEQ(s, keep_msg))
            keep_msg = null;

        /* Truncate the message if needed. */
        msg_start();

        Bytes buf = msg_strtrunc(s, false);
        if (buf != null)
            s = buf;

        msg_outtrans_attr(s, attr);
        msg_clr_eos();

        boolean retval = msg_end();

        if (keep && retval && mb_string2cells(s, -1) < (int)(Rows[0] - cmdline_row - 1) * (int)Columns[0] + sc_col)
            set_keep_msg(s, 0);

        --msg__entered;
        return retval;
    }

    /*
     * Truncate a string such that it can be printed without causing a scroll.
     * Returns an allocated string or null when no truncating is done.
     */
    /*private*/ static Bytes msg_strtrunc(Bytes s, boolean force)
        /* force: always truncate */
    {
        Bytes buf = null;
        int len;
        int room;

        /* May truncate message to avoid a hit-return prompt. */
        if ((!msg_scroll && !need_wait_return && shortmess(SHM_TRUNCALL)
                                   && exmode_active == 0 && msg_silent == 0) || force)
        {
            len = mb_string2cells(s, -1);
            if (msg_scrolled != 0)
                /* Use all the columns. */
                room = (int)(Rows[0] - msg_row) * (int)Columns[0] - 1;
            else
                /* Use up to 'showcmd' column. */
                room = (int)(Rows[0] - msg_row - 1) * (int)Columns[0] + sc_col - 1;
            if (room < len && 0 < room)
            {
                /* may have up to 18 bytes per cell (6 per char, up to two composing chars) */
                len = (room + 2) * 18;
                buf = new Bytes(len);
                trunc_string(s, buf, room, len);
            }
        }
        return buf;
    }

    /*
     * Truncate a string "s" to "buf" with cell width "room".
     * "s" and "buf" may be equal.
     */
    /*private*/ static void trunc_string(Bytes s, Bytes buf, int room, int buflen)
    {
        room -= 3;

        int half = room / 2;
        int len = 0;

        int e;

        /* First part: Start of the string. */
        for (e = 0; len < half && e < buflen; e++)
        {
            if (s.at(e) == NUL)
            {
                /* text fits without truncating! */
                buf.be(e, NUL);
                return;
            }

            int n = mb_ptr2cells(s.plus(e));
            if (half <= len + n)
                break;
            len += n;
            buf.be(e, s.at(e));

            for (n = us_ptr2len_cc(s.plus(e)); 0 < --n; )
            {
                if (++e == buflen)
                    break;
                buf.be(e, s.at(e));
            }
        }

        /* Last part: End of the string. */
        int i = e;

        /* For UTF-8 we can go backwards easily. */
        half = i = strlen(s);
        for ( ; ; )
        {
            do
            {
                half = half - us_head_off(s, s.plus(half - 1)) - 1;
            } while (utf_iscomposing(us_ptr2char(s.plus(half))) && 0 < half);
            int n = mb_ptr2cells(s.plus(half));
            if (room < len + n)
                break;
            len += n;
            i = half;
        }

        /* Set the middle and copy the last part. */
        if (e + 3 < buflen)
        {
            BCOPY(buf, e, u8("..."), 0, 3);
            len = strlen(s, i) + 1;
            if (buflen - e - 3 <= len)
                len = buflen - e - 3 - 1;
            BCOPY(buf, e + 3, s, i, len);
            buf.be(e + 3 + len - 1, NUL);
        }
        else
        {
            buf.be(e - 1, NUL);   /* make sure it is truncated */
        }
    }

    /*private*/ static final boolean smsg(Bytes s, Object... args)
    {
        return smsg_attr(0, s, args);
    }

    /*private*/ static boolean smsg_attr(int attr, Bytes s, Object... args)
    {
        vim_snprintf(ioBuff, IOSIZE, s, args);

        return msg_attr(ioBuff, attr);
    }

    /*
     * Remember the last sourcing name/lnum used in an error message, so that it
     * isn't printed each time when it didn't change.
     */
    /*private*/ static Bytes    last_sourcing_name;
    /*private*/ static long     last_sourcing_lnum;

    /*
     * Reset the last used sourcing name/lnum.  Makes sure it is displayed again
     * for the next error message;
     */
    /*private*/ static void reset_last_sourcing()
    {
        last_sourcing_name = null;
        last_sourcing_lnum = 0;
    }

    /*
     * Return true if "sourcing_name" differs from "last_sourcing_name".
     */
    /*private*/ static boolean other_sourcing_name()
    {
        if (sourcing_name != null)
        {
            if (last_sourcing_name != null)
                return (STRCMP(sourcing_name, last_sourcing_name) != 0);

            return true;
        }
        return false;
    }

    /*
     * Get the message about the source, as used for an error message.
     * Returns an allocated string with room for one more character.
     * Returns null when no message is to be given.
     */
    /*private*/ static Bytes get_emsg_source()
    {
        if (sourcing_name != null && other_sourcing_name())
        {
            Bytes p = u8("Error detected while processing %s:");
            Bytes buf = new Bytes(strlen(sourcing_name) + strlen(p));
            libC.sprintf(buf, p, sourcing_name);
            return buf;
        }

        return null;
    }

    /*
     * Get the message about the source lnum, as used for an error message.
     * Returns an allocated string with room for one more character.
     * Returns null when no message is to be given.
     */
    /*private*/ static Bytes get_emsg_lnum()
    {
        /* lnum is 0 when executing a command from the command line argument,
         * we don't want a line number then */
        if (sourcing_name != null
                && (other_sourcing_name() || sourcing_lnum != last_sourcing_lnum)
                && sourcing_lnum != 0)
        {
            Bytes p = u8("line %4ld:");
            Bytes buf = new Bytes(strlen(p) + 20);
            libC.sprintf(buf, p, sourcing_lnum);
            return buf;
        }

        return null;
    }

    /*
     * Display name and line number for the source of an error.
     * Remember the file name and line number, so that for the next error the info
     * is only displayed if it changed.
     */
    /*private*/ static void msg_source(int attr)
    {
        no_wait_return++;

        Bytes p = get_emsg_source();
        if (p != null)
            msg_attr(p, attr);

        p = get_emsg_lnum();
        if (p != null)
        {
            msg_attr(p, hl_attr(HLF_N));
            last_sourcing_lnum = sourcing_lnum;     /* only once for each line */
        }

        /* remember the last sourcing name printed, also when it's empty */
        if (sourcing_name == null || other_sourcing_name())
        {
            if (sourcing_name == null)
                last_sourcing_name = null;
            else
                last_sourcing_name = STRDUP(sourcing_name);
        }

        --no_wait_return;
    }

    /*
     * Return true if not giving error messages right now:
     * If "emsg_off" is set: no error messages at the moment.
     * If "msg" is in 'debug': do error message but without side effects.
     * If "emsg_skip" is set: never do error messages.
     */
    /*private*/ static boolean emsg_not_now()
    {
        if ((0 < emsg_off && vim_strchr(p_debug[0], 'm') == null && vim_strchr(p_debug[0], 't') == null)
          || 0 < emsg_skip)
            return true;

        return false;
    }

    /*
     * emsg() - display an error message.
     *
     * Rings the bell, if appropriate, and calls message() to do the real work.
     *
     * Return true if wait_return not called.
     */
    /*private*/ static boolean emsg(Bytes s)
    {
        boolean[] ignore = { false };

        /* Skip this if not giving error messages at the moment. */
        if (emsg_not_now())
            return true;

        called_emsg = true;
        ex_exitval = 1;

        /*
         * If "emsg_severe" is true: When an error exception is to be thrown,
         * prefer this message over previous messages for the same command.
         */
        boolean severe = emsg_severe;
        emsg_severe = false;

        if (emsg_off == 0 || vim_strchr(p_debug[0], 't') != null)
        {
            /*
             * Cause a throw of an error exception if appropriate.
             * Don't display the error message in this case.
             * (If no matching catch clause will be found, the message will be displayed later on.)
             * "ignore" is set when the message should be ignored completely
             * (used for the interrupt message).
             */
            if (cause_errthrow(s, severe, ignore) == true)
            {
                if (!ignore[0])
                    did_emsg = true;
                return true;
            }

            /* set "v:errmsg", also when using ":silent! cmd" */
            set_vim_var_string(VV_ERRMSG, s, -1);

            /*
             * When using ":silent! cmd" ignore error messages.
             * But do write it to the redirection file.
             */
            if (emsg_silent != 0)
            {
                msg_start();
                Bytes p = get_emsg_source();
                if (p != null)
                {
                    STRCAT(p, u8("\n"));
                    redir_write(p, -1);
                }
                p = get_emsg_lnum();
                if (p != null)
                {
                    STRCAT(p, u8("\n"));
                    redir_write(p, -1);
                }
                redir_write(s, -1);
                return true;
            }

            /* Reset msg_silent, an error causes messages to be switched back on. */
            msg_silent = 0;
            cmd_silent = false;

            if (global_busy != 0)                /* break :global command */
                global_busy++;

            if (p_eb[0])
                beep_flush();               /* also includes flush_buffers() */
            else
                flush_buffers(false);       /* flush internal buffers */
            did_emsg = true;                /* flag for DoOneCmd() */
        }

        emsg_on_display = true;             /* remember there is an error message */
        msg_scroll = true;                       /* don't overwrite a previous message */
        int attr = hl_attr(HLF_E);          /* set highlight mode for error messages */
        if (msg_scrolled != 0)
            need_wait_return = true;        /* needed in case emsg() is called after
                                             * wait_return has reset need_wait_return
                                             * and a redraw is expected because
                                             * msg_scrolled is non-zero */

        /*
         * Display name and line number for the source of the error.
         */
        msg_source(attr);

        /*
         * Display the error message itself.
         */
        msg_nowait = false;                 /* wait for this msg */
        return msg_attr(s, attr);
    }

    /*
     * Print an error message with one "%s" and one string argument.
     */
    /*private*/ static boolean emsg2(Bytes s, Bytes a1)
    {
        return emsg3(s, a1, null);
    }

    /* emsg3() and emsgn() are in misc.c to avoid warnings for the prototypes. */

    /*private*/ static void emsg_invreg(int name)
    {
        emsg2(u8("E354: Invalid register name: '%s'"), transchar(name));
    }

    /*
     * Like msg(), but truncate to a single line if "p_shm" contains 't', or when "force" is true.
     * This truncates in another way as for normal messages.
     * Careful: The string may be changed by msg_may_trunc()!
     * Returns a pointer to the printed message, if wait_return() not called.
     */
    /*private*/ static Bytes msg_trunc_attr(Bytes s, boolean force, int attr)
    {
        /* Add message to history before truncating. */
        add_msg_hist(s, -1, attr);

        s = msg_may_trunc(force, s);

        msg_hist_off = true;
        boolean b = msg_attr(s, attr);
        msg_hist_off = false;

        return (b) ? s : null;
    }

    /*
     * Check if message "s" should be truncated at the start (for filenames).
     * Return a pointer to where the truncated message starts.
     * Note: May change the message by replacing a character with '<'.
     */
    /*private*/ static Bytes msg_may_trunc(boolean force, Bytes s)
    {
        int room = (int)(Rows[0] - cmdline_row - 1) * (int)Columns[0] + sc_col - 1;

        if ((force || (shortmess(SHM_TRUNC) && exmode_active == 0)) && 0 < strlen(s) - room)
        {
            int cells = mb_string2cells(s, -1);

            /* There may be room anyway when there are multibyte chars. */
            if (cells <= room)
                return s;

            int n;
            for (n = 0; room <= cells; n += us_ptr2len_cc(s.plus(n)))
                cells -= us_ptr2cells(s.plus(n));
            --n;

            s = s.plus(n);
            s.be(0, (byte)'<');
        }

        return s;
    }

    /*private*/ static void add_msg_hist(Bytes s, int len, int attr)
        /* len: -1 for undetermined length */
    {
        final int MAX_MSG_HIST_LEN = 200;

        if (msg_hist_off || msg_silent != 0)
            return;

        /* don't let the message history get too big */
        while (MAX_MSG_HIST_LEN < msg_hist_len)
            delete_first_msg();

        /* allocate an entry and add the message at the end of the history */
        msg_hist_C p = new msg_hist_C();

        if (len < 0)
            len = strlen(s);
        /* remove leading and trailing newlines */
        while (0 < len && s.at(0) == (byte)'\n')
        {
            s = s.plus(1);
            --len;
        }
        while (0 < len && s.at(len - 1) == (byte)'\n')
            --len;
        p.msg = STRNDUP(s, len);
        p.next = null;
        p.attr = attr;
        if (last_msg_hist != null)
            last_msg_hist.next = p;
        last_msg_hist = p;
        if (first_msg_hist == null)
            first_msg_hist = last_msg_hist;
        msg_hist_len++;
    }

    /*
     * Delete the first (oldest) message from the history.
     * Returns false if there are no messages.
     */
    /*private*/ static boolean delete_first_msg()
    {
        if (msg_hist_len <= 0)
            return false;

        msg_hist_C p = first_msg_hist;
        first_msg_hist = p.next;
        if (first_msg_hist == null)
            last_msg_hist = null;       /* history is empty */

        --msg_hist_len;
        return true;
    }

    /*
     * ":messages" command.
     */
    /*private*/ static final ex_func_C ex_messages = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            msg_hist_off = true;

            for (msg_hist_C p = first_msg_hist; p != null && !got_int; p = p.next)
                if (p.msg != null)
                    msg_attr(p.msg, p.attr);

            msg_hist_off = false;
        }
    };

    /*
     * Call this after prompting the user.  This will avoid a hit-return message and a delay.
     */
    /*private*/ static void msg_end_prompt()
    {
        need_wait_return = false;
        emsg_on_display = false;
        cmdline_row = msg_row;
        msg_col = 0;
        msg_clr_eos();
        lines_left = -1;
    }

    /*
     * wait for the user to hit a key (normally a return)
     * if 'redraw' is true, clear and redraw the screen
     * if 'redraw' is false, just redraw the screen
     * if 'redraw' is -1, don't redraw at all
     */
    /*private*/ static void wait_return(int redraw)
    {
        if (redraw == TRUE)
            must_redraw = CLEAR;

        /* If using ":silent cmd", don't wait for a return.
         * Also don't set need_wait_return to do it later. */
        if (msg_silent != 0)
            return;

        /*
         * When inside vgetc(), we can't wait for a typed character at all.
         * With the global command (and some others) we only need one return at
         * the end.  Adjust cmdline_row to avoid the next message overwriting the last one.
         */
        if (0 < vgetc_busy)
            return;
        need_wait_return = true;
        if (no_wait_return != 0)
        {
            if (exmode_active == 0)
                cmdline_row = msg_row;
            return;
        }

        int c;

        redir_off = true;               /* don't redirect this message */
        int oldState = State;
        if (quit_more)
        {
            c = CAR;                    /* just pretend CR was hit */
            quit_more = false;
            got_int = false;
        }
        else if (exmode_active != 0)
        {
            msg_puts(u8(" "));              /* make sure the cursor is on the right line */
            c = CAR;                    /* no need for a return in ex mode */
            got_int = false;
        }
        else
        {
            /* Make sure the hit-return prompt is on screen when 'guioptions' was just changed. */
            screenalloc(false);

            State = HITRETURN;
            setmouse();
            /* Avoid the sequence that the user types ":" at the hit-return prompt
             * to start an Ex command, but the file-changed dialog gets in the way. */
            if (need_check_timestamps)
                check_timestamps(false);

            hit_return_msg();

            boolean had_got_int;
            do
            {
                /* Remember "got_int", if it is set vgetc() probably returns a CTRL-C,
                 * but we need to loop then. */
                had_got_int = got_int;

                /* Don't do mappings here, we put the character back in the typeahead buffer. */
                no_mapping++;
                allow_keys++;

                /* Temporarily disable Recording.
                 * If Recording is active, the character will be recorded later,
                 * since it will be added to the typebuf after the loop. */
                boolean save_Recording = Recording;
                file_C save_scriptout = scriptout;
                Recording = false;
                scriptout = null;
                c = safe_vgetc();
                if (had_got_int && global_busy == 0)
                    got_int = false;
                --no_mapping;
                --allow_keys;
                Recording = save_Recording;
                scriptout = save_scriptout;

                /* Strange way to allow copying (yanking) a modeless selection at
                 * the hit-enter prompt.  Use CTRL-Y, because the same is used in
                 * Cmdline-mode and it's harmless when there is no selection. */
                if (c == Ctrl_Y && clip_star.state == SELECT_DONE)
                {
                    clip_copy_modeless_selection(true);
                    c = K_IGNORE;
                }

                /*
                 * Allow scrolling back in the messages.
                 * Also accept scroll-down commands when messages fill the screen,
                 * to avoid that typing one 'j' too many makes the messages disappear.
                 */
                if (p_more[0])
                {
                    if (c == 'b' || c == 'k' || c == 'u' || c == 'g' || c == K_UP || c == K_PAGEUP)
                    {
                        if (Rows[0] < msg_scrolled)
                            /* scroll back to show older messages */
                            do_more_prompt(c);
                        else
                        {
                            msg_didout = false;
                            c = K_IGNORE;
                            msg_col = cmdmsg_rl ? (int)Columns[0] - 1 : 0;
                        }
                        if (quit_more)
                        {
                            c = CAR;                /* just pretend CR was hit */
                            quit_more = false;
                            got_int = false;
                        }
                        else if (c != K_IGNORE)
                        {
                            c = K_IGNORE;
                            hit_return_msg();
                        }
                    }
                    else if (Rows[0] - 2 < msg_scrolled
                             && (c == 'j' || c == 'd' || c == 'f' || c == K_DOWN || c == K_PAGEDOWN))
                        c = K_IGNORE;
                }
            } while ((had_got_int && c == Ctrl_C)
                                  || c == K_IGNORE
                                  || c == K_LEFTDRAG   || c == K_LEFTRELEASE
                                  || c == K_MIDDLEDRAG || c == K_MIDDLERELEASE
                                  || c == K_RIGHTDRAG  || c == K_RIGHTRELEASE
                                  || c == K_MOUSELEFT  || c == K_MOUSERIGHT
                                  || c == K_MOUSEDOWN  || c == K_MOUSEUP
                                  || (!mouse_has(MOUSE_RETURN)
                                      && mouse_row < msg_row
                                      && (c == K_LEFTMOUSE
                                       || c == K_MIDDLEMOUSE
                                       || c == K_RIGHTMOUSE
                                       || c == K_X1MOUSE
                                       || c == K_X2MOUSE)));
            ui_breakcheck();
            /*
             * Avoid that the mouse-up event causes visual mode to start.
             */
            if (c == K_LEFTMOUSE || c == K_MIDDLEMOUSE || c == K_RIGHTMOUSE || c == K_X1MOUSE || c == K_X2MOUSE)
                jump_to_mouse(MOUSE_SETPOS, null, 0);
            else if (vim_strchr(u8("\r\n "), c) == null && c != Ctrl_C)
            {
                /* Put the character back in the typeahead buffer.
                 * Don't use the stuff buffer, because lmaps wouldn't work. */
                ins_char_typebuf(c);
                do_redraw = true;       /* need a redraw even though there is typeahead */
            }
        }
        redir_off = false;

        /*
         * If the user hits ':', '?' or '/' we get a command line from the next line.
         */
        if (c == ':' || c == '?' || c == '/')
        {
            if (exmode_active == 0)
                cmdline_row = msg_row;
            skip_redraw = true;         /* skip redraw once */
            do_redraw = false;
        }

        /*
         * If the window size changed set_shellsize() will redraw the screen.
         * Otherwise the screen is only redrawn if 'redraw' is set and no ':' typed.
         */
        int tmpState = State;
        State = oldState;               /* restore State before set_shellsize */
        setmouse();
        msg_check();

        /*
         * When switching screens, we need to output an extra newline on exit.
         */
        if (swapping_screen() && !termcap_active)
            newline_on_exit = true;

        need_wait_return = false;
        did_wait_return = true;
        emsg_on_display = false;    /* can delete error message now */
        lines_left = -1;            /* reset lines_left at next msg_start() */
        reset_last_sourcing();
        if (keep_msg != null && (int)(Rows[0] - cmdline_row - 1) * (int)Columns[0] + sc_col <= mb_string2cells(keep_msg, -1))
        {
            keep_msg = null;            /* don't redisplay message, it's too long */
        }

        if (tmpState == SETWSIZE)       /* got resize event while in vgetc() */
        {
            starttermcap();             /* start termcap before redrawing */
            shell_resized();
        }
        else if (!skip_redraw && (redraw == TRUE || (msg_scrolled != 0 && redraw != -1)))
        {
            starttermcap();             /* start termcap before redrawing */
            redraw_later(VALID);
        }
    }

    /*
     * Write the hit-return prompt.
     */
    /*private*/ static void hit_return_msg()
    {
        boolean save_p_more = p_more[0];

        p_more[0] = false;             /* don't want see this message when scrolling back */
        if (msg_didout)             /* start on a new line */
            msg_putchar('\n');
        if (got_int)
            msg_puts(u8("Interrupt: "));

        msg_puts_attr(u8("Press ENTER or type command to continue"), hl_attr(HLF_R));
        if (!msg_use_printf())
            msg_clr_eos();
        p_more[0] = save_p_more;
    }

    /*
     * Set "keep_msg" to "s".  Free the old value and check for null pointer.
     */
    /*private*/ static void set_keep_msg(Bytes s, int attr)
    {
        if (s != null && msg_silent == 0)
            keep_msg = STRDUP(s);
        else
            keep_msg = null;
        keep_msg_more = false;
        keep_msg_attr = attr;
    }

    /*
     * If there currently is a message being displayed, set "keep_msg" to it, so
     * that it will be displayed again after redraw.
     */
    /*private*/ static void set_keep_msg_from_hist()
    {
        if (keep_msg == null && last_msg_hist != null && msg_scrolled == 0 && (State & NORMAL) != 0)
            set_keep_msg(last_msg_hist.msg, last_msg_hist.attr);
    }

    /*
     * Prepare for outputting characters in the command line.
     */
    /*private*/ static void msg_start()
    {
        boolean did_return = false;

        if (msg_silent == 0)
        {
            keep_msg = null;                    /* don't display old message now */
        }

        if (need_clr_eos)
        {
            /* Halfway an ":echo" command and getting an (error) message:
             * clear any text from the command. */
            need_clr_eos = false;
            msg_clr_eos();
        }

        if (!msg_scroll && full_screen)         /* overwrite last message */
        {
            msg_row = cmdline_row;
            msg_col = cmdmsg_rl ? (int)Columns[0] - 1 : 0;
        }
        else if (msg_didout)                    /* start message on next line */
        {
            msg_putchar('\n');
            did_return = true;
            if (exmode_active != EXMODE_NORMAL)
                cmdline_row = msg_row;
        }
        if (!msg_didany || lines_left < 0)
            msg_starthere();
        if (msg_silent == 0)
        {
            msg_didout = false;                 /* no output on current line yet */
            cursor_off();
        }

        /* when redirecting, may need to start a new line. */
        if (!did_return)
            redir_write(u8("\n"), -1);
    }

    /*
     * Note that the current msg position is where messages start.
     */
    /*private*/ static void msg_starthere()
    {
        lines_left = cmdline_row;
        msg_didany = false;
    }

    /*private*/ static void msg_putchar(int c)
    {
        msg_putchar_attr(c, 0);
    }

    /*private*/ static void msg_putchar_attr(int c, int attr)
    {
        Bytes buf = new Bytes(MB_MAXBYTES + 1);

        if (is_special(c))
        {
            buf.be(0, KB_SPECIAL);
            buf.be(1, KB_SECOND(c));
            buf.be(2, KB_THIRD(c));
            buf.be(3, NUL);
        }
        else
        {
            buf.be(utf_char2bytes(c, buf), NUL);
        }
        msg_puts_attr(buf, attr);
    }

    /*private*/ static void msg_outnum(long n)
    {
        Bytes buf = new Bytes(20);

        libC.sprintf(buf, u8("%ld"), n);
        msg_puts(buf);
    }

    /*
     * Output 'len' characters in 'p' (including NULs) with translation
     * if 'len' is -1, output upto a NUL character.
     * Use attributes 'attr'.
     * Return the number of characters it takes on the screen.
     */
    /*private*/ static int msg_outtrans(Bytes p)
    {
        return msg_outtrans_attr(p, 0);
    }

    /*private*/ static int msg_outtrans_attr(Bytes p, int attr)
    {
        return msg_outtrans_len_attr(p, strlen(p), attr);
    }

    /*private*/ static int msg_outtrans_len(Bytes p, int len)
    {
        return msg_outtrans_len_attr(p, len, 0);
    }

    /*private*/ static int msg_outtrans_len_attr(Bytes p, int len, int attr)
    {
        int cells = 0;

        /* if MSG_HIST flag set, add message to history */
        if ((attr & MSG_HIST) != 0)
        {
            add_msg_hist(p, len, attr);
            attr &= ~MSG_HIST;
        }

        /* If the string starts with a composing character,
         * first draw a space on which the composing char can be drawn. */
        if (utf_iscomposing(us_ptr2char(p)))
            msg_puts_attr(u8(" "), attr);

        Bytes q = p;

        /*
         * Go over the string.  Special characters are translated and printed.
         * Normal characters are printed several at a time.
         */
        while (0 <= --len)
        {
            /* Don't include composing chars after the end. */
            int l = us_ptr2len_cc_len(p, len + 1);
            if (1 < l)
            {
                int c = us_ptr2char(p);
                if (vim_isprintc(c))
                    /* printable multi-byte char: count the cells. */
                    cells += us_ptr2cells(p);
                else
                {
                    /* unprintable multi-byte char: print the printable chars
                     * so far and the translation of the unprintable char */
                    if (BLT(q, p))
                        msg_puts_attr_len(q, BDIFF(p, q), attr);
                    q = p.plus(l);
                    msg_puts_attr(transchar(c), (attr == 0) ? hl_attr(HLF_8) : attr);
                    cells += mb_char2cells(c);
                }
                len -= l - 1;
                p = p.plus(l);
            }
            else
            {
                Bytes s = transchar_byte(p.at(0));
                if (s.at(1) != NUL)
                {
                    /* unprintable char: print the printable chars so far
                     * and the translation of the unprintable char */
                    if (BLT(q, p))
                        msg_puts_attr_len(q, BDIFF(p, q), attr);
                    q = p.plus(1);
                    msg_puts_attr(s, (attr == 0) ? hl_attr(HLF_8) : attr);
                    cells += strlen(s);
                }
                else
                    cells++;
                p = p.plus(1);
            }
        }

        if (BLT(q, p))
            /* print the printable chars at the end */
            msg_puts_attr_len(q, BDIFF(p, q), attr);

        return cells;
    }

    /*
     * Output the string 's' upto a NUL character.
     * Return the number of characters it takes on the screen.
     *
     * If KB_SPECIAL is encountered, then it is taken in conjunction with the following character
     * and shown as <F1>, <S-Up> etc.  Any other character which is not printable shown in <> form.
     * If 'from' is true (lhs of a mapping), a space is shown as <Space>.
     * If a character is displayed in one of these special ways, is also highlighted
     * (its highlight name is '8' in the "p_hl" variable).  Otherwise characters are not highlighted.
     * This function is used to show mappings, where we want to see how to type the character/string.
     */
    /*private*/ static int msg_outtrans_special(Bytes _p, boolean is_lhs)
        /* is_lhs: true for lhs of a mapping */
    {
        Bytes[] p = { _p };
        int cells = 0;

        int attr = hl_attr(HLF_8);
        for (Bytes q = p[0]; p[0].at(0) != NUL; )
        {
            Bytes s;

            /* Leading and trailing spaces need to be displayed in <> form. */
            if ((BEQ(p[0], q) || p[0].at(1) == NUL) && p[0].at(0) == (byte)' ')
            {
                s = u8("<Space>");
                p[0] = p[0].plus(1);
            }
            else
                s = str2special(p, is_lhs);

            int len = mb_string2cells(s, -1);
            /* Highlight special keys. */
            msg_puts_attr(s, (1 < len && us_ptr2len_cc(s) <= 1) ? attr : 0);
            cells += len;
        }

        return cells;
    }

    /*
     * Return the lhs or rhs of a mapping, with the key codes
     * turned into printable strings, in an allocated string.
     */
    /*private*/ static Bytes str2special_save(Bytes _s, boolean is_lhs)
        /* is_lhs: true for lhs, false for rhs */
    {
        Bytes[] s = { _s };
        barray_C ba = new barray_C(40);

        while (s[0].at(0) != NUL)
            ba_concat(ba, str2special(s, is_lhs));
        ba_append(ba, NUL);

        return new Bytes(ba.ba_data);
    }

    /*private*/ static Bytes buf7 = new Bytes(7);

    /*
     * Return the printable string for the key codes at "*sp".
     * Used for translating the lhs or rhs of a mapping to printable chars.
     * Advances "*sp" to the next code.
     */
    /*private*/ static Bytes str2special(Bytes[] sp, boolean is_lhs)
        /* is_lhs: true for lhs of mapping */
    {
        Bytes s = sp[0];
        int modifiers = 0;
        boolean special = false;

        /* Try to un-escape a multi-byte character.
         * Return the un-escaped string if it is a multi-byte character. */
        Bytes p = mb_unescape(sp);
        if (p != null)
            return p;

        int c = char_u(s.at(0));
        if (c == char_u(KB_SPECIAL) && s.at(1) != NUL && s.at(2) != NUL)
        {
            if (s.at(1) == KS_MODIFIER)
            {
                modifiers = char_u(s.at(2));
                s = s.plus(3);
                c = char_u(s.at(0));
            }
            if (c == char_u(KB_SPECIAL) && s.at(1) != NUL && s.at(2) != NUL)
            {
                c = toSpecial(s.at(1), s.at(2));
                s = s.plus(2);
                if (c == char_u(KS_ZERO))   /* display <Nul> as ^@ or <Nul> */
                    c = NUL;
            }
            if (is_special(c) || modifiers != 0)    /* special key */
                special = true;
        }

        if (!is_special(c))
        {
            int len = us_ptr2len_cc(s);

            /* For multi-byte characters check for an illegal byte. */
            if (len < us_byte2len(s.at(0), false))
            {
                transchar_nonprint(buf7, c);
                sp[0] = s.plus(1);
                return buf7;
            }
            /* Since 'special' is true, the multi-byte character 'c'
             * will be processed by get_special_key_name(). */
            c = us_ptr2char(s);
            sp[0] = s.plus(len);
        }
        else
            sp[0] = s.plus(1);

        /* Make unprintable characters in <> form, also <M-Space> and <Tab>.
         * Use <Space> only for lhs of a mapping. */
        if (special || 1 < mb_char2cells(c) || (is_lhs && c == ' '))
            return get_special_key_name(c, modifiers);

        buf7.be(0, c);
        buf7.be(1, NUL);
        return buf7;
    }

    /*
     * Translate a key sequence into special key names.
     */
    /*private*/ static void str2specialbuf(Bytes _sp, Bytes buf, int len)
    {
        Bytes[] sp = { _sp };
        buf.be(0, NUL);
        while (sp[0].at(0) != NUL)
        {
            Bytes s = str2special(sp, false);
            if (strlen(s) + strlen(buf) < len)
                STRCAT(buf, s);
        }
    }

    /*
     * print line for :print or :list command
     */
    /*private*/ static void msg_prt_line(Bytes s, boolean list)
    {
        if (curwin.w_onebuf_opt.wo_list[0])
            list = true;

        /* find start of trailing whitespace */
        Bytes trail = null;
        if (list && lcs_trail[0] != NUL)
        {
            trail = s.plus(strlen(s));
            while (BLT(s, trail) && vim_iswhite(trail.at(-1)))
                trail = trail.minus(1);
        }

        /* output a space for an empty line, otherwise the line will be overwritten */
        if (s.at(0) == NUL && !(list && lcs_eol[0] != NUL))
            msg_putchar(' ');

        int col = 0;
        int n_extra = 0;
        int c_extra = 0;
        Bytes p_extra = null;
        int attr = 0;

        for (int c; !got_int; )
        {
            int len;

            if (0 < n_extra)
            {
                --n_extra;
                if (c_extra != 0)
                    c = c_extra;
                else
                    c = char_u((p_extra = p_extra.plus(1)).at(-1));
            }
            else if (1 < (len = us_ptr2len_cc(s)))
            {
                Bytes buf = new Bytes(MB_MAXBYTES + 1);

                col += us_ptr2cells(s);
                if (lcs_nbsp[0] != NUL && list && us_ptr2char(s) == 0xa0)
                {
                    utf_char2bytes(lcs_nbsp[0], buf);
                    buf.be(us_ptr2len_cc(buf), NUL);
                }
                else
                {
                    BCOPY(buf, s, len);
                    buf.be(len, NUL);
                }
                msg_puts(buf);
                s = s.plus(len);
                continue;
            }
            else
            {
                int n;

                attr = 0;
                c = char_u((s = s.plus(1)).at(-1));
                if (c == TAB && (!list || lcs_tab1[0] != NUL))
                {
                    /* tab amount depends on current column */
                    n_extra = (int)curbuf.b_p_ts[0] - col % (int)curbuf.b_p_ts[0] - 1;
                    if (!list)
                    {
                        c = ' ';
                        c_extra = ' ';
                    }
                    else
                    {
                        c = lcs_tab1[0];
                        c_extra = lcs_tab2[0];
                        attr = hl_attr(HLF_8);
                    }
                }
                else if (c == 0xa0 && list && lcs_nbsp[0] != NUL)
                {
                    c = lcs_nbsp[0];
                    attr = hl_attr(HLF_8);
                }
                else if (c == NUL && list && lcs_eol[0] != NUL)
                {
                    p_extra = u8("");
                    c_extra = NUL;
                    n_extra = 1;
                    c = lcs_eol[0];
                    attr = hl_attr(HLF_AT);
                    s = s.minus(1);
                }
                else if (c != NUL && 1 < (n = mb_byte2cells((byte)c)))
                {
                    n_extra = n - 1;
                    p_extra = transchar_byte((byte)c);
                    c_extra = NUL;
                    c = char_u((p_extra = p_extra.plus(1)).at(-1));
                    /* Use special coloring to be able to distinguish <hex> from the same in plain text. */
                    attr = hl_attr(HLF_8);
                }
                else if (c == ' ' && trail != null && BLT(trail, s))
                {
                    c = lcs_trail[0];
                    attr = hl_attr(HLF_8);
                }
            }

            if (c == NUL)
                break;

            msg_putchar_attr(c, attr);
            col++;
        }

        msg_clr_eos();
    }

    /*
     * Use screen_puts() to output one multi-byte character.
     * Return the pointer "s" advanced to the next character.
     */
    /*private*/ static Bytes screen_puts_mbyte(Bytes s, int len, int attr)
    {
        msg_didout = true;          /* remember that line is not empty */

        int cells = us_ptr2cells(s);
        if (1 < cells && (cmdmsg_rl ? msg_col <= 1 : msg_col == (int)Columns[0] - 1))
        {
            /* Doesn't fit, print a highlighted '>' to fill it up. */
            msg_screen_putchar('>', hl_attr(HLF_AT));
            return s;
        }

        screen_puts_len(s, len, msg_row, msg_col, attr);
        if (cmdmsg_rl)
        {
            msg_col -= cells;
            if (msg_col == 0)
            {
                msg_col = (int)Columns[0];
                msg_row++;
            }
        }
        else
        {
            msg_col += cells;
            if ((int)Columns[0] <= msg_col)
            {
                msg_col = 0;
                msg_row++;
            }
        }
        return s.plus(len);
    }

    /*
     * Output a string to the screen at position msg_row, msg_col.
     * Update msg_row and msg_col for the next message.
     */
    /*private*/ static void msg_puts(Bytes s)
    {
        msg_puts_attr(s, 0);
    }

    /*private*/ static void msg_puts_title(Bytes s)
    {
        msg_puts_attr(s, hl_attr(HLF_T));
    }

    /*
     * Basic function for writing a message with highlight attributes.
     */
    /*private*/ static void msg_puts_attr(Bytes s, int attr)
    {
        msg_puts_attr_len(s, -1, attr);
    }

    /*
     * Like msg_puts_attr(), but with a maximum length "maxlen" (in bytes).
     * When "maxlen" is -1 there is no maximum length.
     * When "maxlen" is >= 0 the message is not put in the history.
     */
    /*private*/ static void msg_puts_attr_len(Bytes str, int maxlen, int attr)
    {
        /*
         * If redirection is on, also write to the redirection file.
         */
        redir_write(str, maxlen);

        /*
         * Don't print anything when using ":silent cmd".
         */
        if (msg_silent != 0)
            return;

        /* if MSG_HIST flag set, add message to history */
        if ((attr & MSG_HIST) != 0 && maxlen < 0)
        {
            add_msg_hist(str, -1, attr);
            attr &= ~MSG_HIST;
        }

        /*
         * When writing something to the screen after it has scrolled, requires
         * a wait-return prompt later.  Needed when scrolling, resetting need_wait_return
         * after some prompt, and then outputting something without scrolling
         */
        if (msg_scrolled != 0 && !msg_scrolled_ign)
            need_wait_return = true;
        msg_didany = true;          /* remember that something was outputted */

        /*
         * If there is no valid screen, use fprintf so we can see error messages.
         * If termcap is not active, we may be writing in an alternate console window,
         * cursor positioning may not work correctly (window size may be different,
         * e.g. for Win32 console) or we just don't know where the cursor is.
         */
        if (msg_use_printf())
            msg_puts_printf(str, maxlen);
        else
            msg_puts_display(str, maxlen, attr, false);
    }

    /*
     * The display part of msg_puts_attr_len().
     * May be called recursively to display scroll-back text.
     */
    /*private*/ static void msg_puts_display(Bytes str, int maxlen, int attr, boolean recurse)
    {
        Bytes s = str;
        Bytes t_s = str;       /* string from "t_s" to "s" is still todo */
        int t_col = 0;          /* screen cells todo, 0 when "t_s" not used */
        Bytes[] sb_str = { str };
        int[] sb_col = { msg_col };

        did_wait_return = false;
        while ((maxlen < 0 || BDIFF(s, str) < maxlen) && s.at(0) != NUL)
        {
            /*
             * We are at the end of the screen line when:
             * - When outputting a newline.
             * - When outputting a character in the last column.
             */
            if (!recurse && Rows[0] - 1 <= msg_row && (s.at(0) == (byte)'\n' || (
                        cmdmsg_rl
                        ? (msg_col <= 1
                            || (s.at(0) == TAB && msg_col <= 7)
                            || (1 < us_ptr2cells(s) && msg_col <= 2))
                        : ((int)Columns[0] - 1 <= msg_col + t_col
                            || (s.at(0) == TAB && (((int)Columns[0] - 1) & ~7) <= msg_col + t_col)
                            || (1 < us_ptr2cells(s) && (int)Columns[0] - 2 <= msg_col + t_col)))))
            {
                /*
                 * The screen is scrolled up when at the last row (some terminals scroll
                 * automatically, some don't.  To avoid problems we scroll ourselves).
                 */
                if (0 < t_col)
                    /* output postponed text */
                    t_col = t_puts(t_col, t_s, s, attr);

                /* When no more prompt and no more room, truncate here. */
                if (msg_no_more && lines_left == 0)
                    break;

                /* Scroll the screen up one line. */
                msg_scroll_up();

                msg_row = (int)Rows[0] - 2;
                if ((int)Columns[0] <= msg_col)     /* can happen after screen resize */
                    msg_col = (int)Columns[0] - 1;

                boolean did_last_char;

                /* Display char in last column before showing more-prompt. */
                if (' ' <= s.at(0) && !cmdmsg_rl)
                {
                    int len;
                    if (0 <= maxlen)
                        /* avoid including composing chars after the end */
                        len = us_ptr2len_cc_len(s, BDIFF(str.plus(maxlen), s));
                    else
                        len = us_ptr2len_cc(s);
                    s = screen_puts_mbyte(s, len, attr);

                    did_last_char = true;
                }
                else
                    did_last_char = false;

                if (p_more[0])
                    /* store text for scrolling back */
                    store_sb_text(sb_str, s, attr, sb_col, true);

                inc_msg_scrolled();
                need_wait_return = true;    /* may need wait_return in main() */
                if (must_redraw < VALID)
                    must_redraw = VALID;
                redraw_cmdline = true;
                if (0 < cmdline_row && exmode_active == 0)
                    --cmdline_row;

                /*
                 * If screen is completely filled and 'more' is set then wait for a character.
                 */
                if (0 < lines_left)
                    --lines_left;
                if (p_more[0] && lines_left == 0 && State != HITRETURN && !msg_no_more && exmode_active == 0)
                {
                    if (do_more_prompt(NUL))
                        s = confirm_msg_tail;
                    if (quit_more)
                        return;
                }

                /* When we displayed a char in last column need to check if there is still more. */
                if (did_last_char)
                    continue;
            }

            boolean wrap = (s.at(0) == (byte)'\n')
                        || (int)Columns[0] <= msg_col + t_col
                        || (1 < us_ptr2cells(s) && (int)Columns[0] - 1 <= msg_col + t_col);
            if (0 < t_col && (wrap || s.at(0) == (byte)'\r' || s.at(0) == (byte)'\b' || s.at(0) == (byte)'\t' || s.at(0) == BELL))
                /* output any postponed text */
                t_col = t_puts(t_col, t_s, s, attr);

            if (wrap && p_more[0] && !recurse)
                /* store text for scrolling back */
                store_sb_text(sb_str, s, attr, sb_col, true);

            if (s.at(0) == (byte)'\n')                 /* go to next line */
            {
                msg_didout = false;         /* remember that line is empty */
                if (cmdmsg_rl)
                    msg_col = (int)Columns[0] - 1;
                else
                    msg_col = 0;
                if (Rows[0] <= ++msg_row)      /* safety check */
                    msg_row = (int)Rows[0] - 1;
            }
            else if (s.at(0) == (byte)'\r')            /* go to column 0 */
            {
                msg_col = 0;
            }
            else if (s.at(0) == (byte)'\b')            /* go to previous char */
            {
                if (0 < msg_col)
                    --msg_col;
            }
            else if (s.at(0) == TAB)             /* translate Tab into spaces */
            {
                do
                {
                    msg_screen_putchar(' ', attr);
                } while ((msg_col & 7) != 0);
            }
            else if (s.at(0) == BELL)            /* beep (from ":sh") */
                vim_beep();
            else
            {
                int cells = us_ptr2cells(s);

                int len;
                if (0 <= maxlen)
                    /* avoid including composing chars after the end */
                    len = us_ptr2len_cc_len(s, BDIFF(str.plus(maxlen), s));
                else
                    len = us_ptr2len_cc(s);

                /* When drawing from right to left or when a double-wide character
                 * doesn't fit, draw a single character here.  Otherwise collect
                 * characters and draw them all at once later. */
                if (cmdmsg_rl || (1 < cells && (int)Columns[0] - 1 <= msg_col + t_col))
                {
                    if (1 < len)
                        s = screen_puts_mbyte(s, len, attr).minus(1);
                    else
                        msg_screen_putchar(s.at(0), attr);
                }
                else
                {
                    /* postpone this character until later */
                    if (t_col == 0)
                        t_s = s;
                    t_col += cells;
                    s = s.plus(len - 1);
                }
            }
            s = s.plus(1);
        }

        /* output any postponed text */
        if (0 < t_col)
            t_col = t_puts(t_col, t_s, s, attr);
        if (p_more[0] && !recurse)
            store_sb_text(sb_str, s, attr, sb_col, false);

        msg_check();
    }

    /*
     * Scroll the screen up one line for displaying the next message line.
     */
    /*private*/ static void msg_scroll_up()
    {
        /* scrolling up always works */
        screen_del_lines(0, 0, 1, (int)Rows[0], true, null);

        if (!can_clear(u8(" ")))
        {
            /* Scrolling up doesn't result in the right background.
             * Set the background here.  It's not efficient,
             * but avoids that we have to do it all over the code. */
            screen_fill((int)Rows[0] - 1, (int)Rows[0], 0, (int)Columns[0], ' ', ' ', 0);

            /* Also clear the last char of the last but one line
             * if it was not cleared before to avoid a scroll-up. */
            if (screenAttrs[lineOffset[(int)Rows[0] - 2] + (int)Columns[0] - 1] == -1)
                screen_fill((int)Rows[0] - 2, (int)Rows[0] - 1, (int)Columns[0] - 1, (int)Columns[0], ' ', ' ', 0);
        }
    }

    /*
     * Increment "msg_scrolled".
     */
    /*private*/ static void inc_msg_scrolled()
    {
        if (get_vim_var_str(VV_SCROLLSTART).at(0) == NUL)
        {
            Bytes p = sourcing_name;

            /* v:scrollstart is empty, set it to the script/function name and line number */
            if (p == null)
                p = u8("Unknown");
            else
            {
                int len = strlen(p) + 40;
                Bytes tofree = new Bytes(len);

                vim_snprintf(tofree, len, u8("%s line %ld"), p, sourcing_lnum);

                p = tofree;
            }
            set_vim_var_string(VV_SCROLLSTART, p, -1);
        }
        msg_scrolled++;
    }

    /*
     * To be able to scroll back at the "more" and "hit-enter" prompts we need to
     * store the displayed text and remember where screen lines start.
     */
    /*private*/ static final class msgchunk_C
    {
        msgchunk_C  sb_next;
        msgchunk_C  sb_prev;
        boolean     sb_eol;         /* true when line ends after this text */
        int         sb_msg_col;     /* column in which text starts */
        int         sb_attr;        /* text attributes */
        Bytes       sb_text;        /* text to be displayed, actually longer */

        /*private*/ msgchunk_C()
        {
        }
    }

    /*private*/ static msgchunk_C last_msgchunk;    /* last displayed text */

    /*private*/ static boolean do_clear_sb_text;    /* clear text on next msg */

    /*
     * Store part of a printed message for displaying when scrolling back.
     */
    /*private*/ static void store_sb_text(Bytes[] sb_str, Bytes s, int attr, int[] sb_col, boolean finish)
        /* sb_str: start of string */
        /* s: just after string */
        /* finish: line ends */
    {
        if (do_clear_sb_text)
        {
            clear_sb_text();
            do_clear_sb_text = false;
        }

        if (BLT(sb_str[0], s))
        {
            msgchunk_C mp = new msgchunk_C();

            mp.sb_eol = finish;
            mp.sb_msg_col = sb_col[0];
            mp.sb_attr = attr;
            mp.sb_text = STRNDUP(sb_str[0], BDIFF(s, sb_str[0]));

            if (last_msgchunk == null)
            {
                last_msgchunk = mp;
                mp.sb_prev = null;
            }
            else
            {
                mp.sb_prev = last_msgchunk;
                last_msgchunk.sb_next = mp;
                last_msgchunk = mp;
            }
            mp.sb_next = null;
        }
        else if (finish && last_msgchunk != null)
            last_msgchunk.sb_eol = true;

        sb_str[0] = s;
        sb_col[0] = 0;
    }

    /*
     * Finished showing messages, clear the scroll-back text on the next message.
     */
    /*private*/ static void may_clear_sb_text()
    {
        do_clear_sb_text = true;
    }

    /*
     * Clear any text remembered for scrolling back.
     * Called when redrawing the screen.
     */
    /*private*/ static void clear_sb_text()
    {
        last_msgchunk = null;
    }

    /*
     * "g<" command.
     */
    /*private*/ static void show_sb_text()
    {
        /* Only show something if there is more than one line, otherwise it looks weird,
         * typing a command without output results in one line. */
        msgchunk_C mp = msg_sb_start(last_msgchunk);
        if (mp == null || mp.sb_prev == null)
            vim_beep();
        else
        {
            do_more_prompt('G');
            wait_return(FALSE);
        }
    }

    /*
     * Move to the start of screen line in already displayed text.
     */
    /*private*/ static msgchunk_C msg_sb_start(msgchunk_C mps)
    {
        msgchunk_C mp = mps;

        while (mp != null && mp.sb_prev != null && !mp.sb_prev.sb_eol)
            mp = mp.sb_prev;

        return mp;
    }

    /*
     * Mark the last message chunk as finishing the line.
     */
    /*private*/ static void msg_sb_eol()
    {
        if (last_msgchunk != null)
            last_msgchunk.sb_eol = true;
    }

    /*
     * Display a screen line from previously displayed text at row "row".
     * Returns a pointer to the text for the next line (can be null).
     */
    /*private*/ static msgchunk_C disp_sb_line(int row, msgchunk_C smp)
    {
        msgchunk_C mp = smp;

        for ( ; ; )
        {
            msg_row = row;
            msg_col = mp.sb_msg_col;
            Bytes p = mp.sb_text;
            if (p.at(0) == (byte)'\n')             /* don't display the line break */
                p = p.plus(1);
            msg_puts_display(p, -1, mp.sb_attr, true);
            if (mp.sb_eol || mp.sb_next == null)
                break;
            mp = mp.sb_next;
        }

        return mp.sb_next;
    }

    /*
     * Output any postponed text for msg_puts_attr_len().
     */
    /*private*/ static int t_puts(int t_col, Bytes t_s, Bytes s, int attr)
    {
        /* output postponed text */
        msg_didout = true;          /* remember that line is not empty */
        screen_puts_len(t_s, BDIFF(s, t_s), msg_row, msg_col, attr);
        msg_col += t_col;
        t_col = 0;
        /* If the string starts with a composing character,
         * don't increment the column position for it. */
        if (utf_iscomposing(us_ptr2char(t_s)))
            --msg_col;
        if ((int)Columns[0] <= msg_col)
        {
            msg_col = 0;
            msg_row++;
        }
        return t_col;
    }

    /*
     * Returns true when messages should be printed with mch_errmsg().
     * This is used when there is no valid screen, so we can see error messages.
     * If termcap is not active, we may be writing in an alternate console window,
     * cursor positioning may not work correctly (window size may be different)
     * or we just don't know where the cursor is.
     */
    /*private*/ static boolean msg_use_printf()
    {
        return (!msg_check_screen() || (swapping_screen() && !termcap_active));
    }

    /*
     * Print a message when there is no valid screen.
     */
    /*private*/ static void msg_puts_printf(Bytes str, int maxlen)
    {
        for (Bytes s = str; s.at(0) != NUL && (maxlen < 0 || BDIFF(s, str) < maxlen); s = s.plus(1))
        {
            if (!(silent_mode && p_verbose[0] == 0))
            {
                Bytes buf = new Bytes(4);
                Bytes p = buf;

                /* NL --> CR NL translation (for Unix, not for "--version") */
                if (s.at(0) == (byte)'\n' && !info_message)
                    (p = p.plus(1)).be(-1, (byte)'\r');
                (p = p.plus(1)).be(-1, s.at(0));
                p.be(0, NUL);
                if (info_message)   /* informative message, not an error */
                    libC.fprintf(stdout, u8("%s"), buf);
                else
                    libC.fprintf(stderr, u8("%s"), buf);
            }

            /* primitive way to compute the current column */
            if (cmdmsg_rl)
            {
                if (s.at(0) == (byte)'\r' || s.at(0) == (byte)'\n')
                    msg_col = (int)Columns[0] - 1;
                else
                    --msg_col;
            }
            else
            {
                if (s.at(0) == (byte)'\r' || s.at(0) == (byte)'\n')
                    msg_col = 0;
                else
                    msg_col++;
            }
        }

        msg_didout = true;      /* assume that line is not empty */
    }

    /*
     * Show the more-prompt and handle the user response.
     * This takes care of scrolling back and displaying previously displayed text.
     * When at hit-enter prompt "typed_char" is the already typed character,
     * otherwise it's NUL.
     * Returns true when jumping ahead to "confirm_msg_tail".
     */
    /*private*/ static boolean do_more_prompt(int typed_char)
    {
        boolean retval = false;

        int used_typed_char = typed_char;
        int oldState = State;

        msgchunk_C mp_last = null;
        if (typed_char == 'G')
        {
            /* "g<": Find first line on the last page. */
            mp_last = msg_sb_start(last_msgchunk);
            for (int i = 0; i < Rows[0] - 2 && mp_last != null && mp_last.sb_prev != null; i++)
                mp_last = msg_sb_start(mp_last.sb_prev);
        }

        State = ASKMORE;
        setmouse();
        if (typed_char == NUL)
            msg_moremsg(false);

        for ( ; ; )
        {
            int c;
            /*
             * Get a typed character directly from the user.
             */
            if (used_typed_char != NUL)
            {
                c = used_typed_char;        /* was typed at hit-enter prompt */
                used_typed_char = NUL;
            }
            else
                c = get_keystroke();

            int toscroll = 0;
            switch (c)
            {
                case BS:                    /* scroll one line back */
                case K_BS:
                case 'k':
                case K_UP:
                    toscroll = -1;
                    break;

                case CAR:                   /* one extra line */
                case NL:
                case 'j':
                case K_DOWN:
                    toscroll = 1;
                    break;

                case 'u':                   /* Up half a page */
                    toscroll = -((int)Rows[0] / 2);
                    break;

                case 'd':                   /* Down half a page */
                    toscroll = (int)Rows[0] / 2;
                    break;

                case 'b':                   /* one page back */
                case K_PAGEUP:
                    toscroll = -((int)Rows[0] - 1);
                    break;

                case ' ':                   /* one extra page */
                case 'f':
                case K_PAGEDOWN:
                case K_LEFTMOUSE:
                    toscroll = (int)Rows[0] - 1;
                    break;

                case 'g':                   /* all the way back to the start */
                    toscroll = -999999;
                    break;

                case 'G':                   /* all the way to the end */
                    toscroll = 999999;
                    lines_left = 999999;
                    break;

                case ':':                   /* start new command line */
                    if (confirm_msg_used == 0)
                    {
                        /* Since got_int is set all typeahead will be flushed, but we
                         * want to keep this ':', remember that in a special way. */
                        typeahead_noflush(':');
                        cmdline_row = (int)Rows[0] - 1;         /* put ':' on this line */
                        skip_redraw = true;             /* skip redraw once */
                        need_wait_return = false;       /* don't wait in main() */
                    }
                    /* FALLTHROUGH */
                case 'q':                   /* quit */
                case Ctrl_C:
                case ESC:
                    if (confirm_msg_used != 0)
                    {
                        /* Jump to the choices of the dialog. */
                        retval = true;
                    }
                    else
                    {
                        got_int = true;
                        quit_more = true;
                    }
                    /* When there is some more output (wrapping line)
                     * display that without another prompt. */
                    lines_left = (int)Rows[0] - 1;
                    break;

                case Ctrl_Y:
                    /* Strange way to allow copying (yanking) a modeless
                     * selection at the more prompt.  Use CTRL-Y,
                     * because the same is used in Cmdline-mode and at the
                     * hit-enter prompt.  However, scrolling one line up
                     * might be expected... */
                    if (clip_star.state == SELECT_DONE)
                        clip_copy_modeless_selection(true);
                    continue;

                default:                    /* no valid response */
                    msg_moremsg(true);
                    continue;
            }

            if (toscroll != 0)
            {
                if (toscroll < 0)
                {
                    msgchunk_C mp;
                    /* go to start of last line */
                    if (mp_last == null)
                        mp = msg_sb_start(last_msgchunk);
                    else if (mp_last.sb_prev != null)
                        mp = msg_sb_start(mp_last.sb_prev);
                    else
                        mp = null;

                    /* go to start of line at top of the screen */
                    for (int i = 0; i < Rows[0] - 2 && mp != null && mp.sb_prev != null; i++)
                        mp = msg_sb_start(mp.sb_prev);

                    if (mp != null && mp.sb_prev != null)
                    {
                        /* Find line to be displayed at top. */
                        for (int i = 0; toscroll < i; --i)
                        {
                            if (mp == null || mp.sb_prev == null)
                                break;
                            mp = msg_sb_start(mp.sb_prev);
                            if (mp_last == null)
                                mp_last = msg_sb_start(last_msgchunk);
                            else
                                mp_last = msg_sb_start(mp_last.sb_prev);
                        }

                        if (toscroll == -1 && screen_ins_lines(0, 0, 1, (int)Rows[0], null) == true)
                        {
                            /* display line at top */
                            disp_sb_line(0, mp);
                        }
                        else
                        {
                            /* redisplay all lines */
                            screenclear();
                            for (int i = 0; mp != null && i < Rows[0] - 1; i++)
                            {
                                mp = disp_sb_line(i, mp);
                                msg_scrolled++;
                            }
                        }
                        toscroll = 0;
                    }
                }
                else
                {
                    /* First display any text that we scrolled back. */
                    while (0 < toscroll && mp_last != null)
                    {
                        /* scroll up, display line at bottom */
                        msg_scroll_up();
                        inc_msg_scrolled();
                        screen_fill((int)Rows[0] - 2, (int)Rows[0] - 1, 0, (int)Columns[0], ' ', ' ', 0);
                        mp_last = disp_sb_line((int)Rows[0] - 2, mp_last);
                        --toscroll;
                    }
                }

                if (toscroll <= 0)
                {
                    /* displayed the requested text, more prompt again */
                    screen_fill((int)Rows[0] - 1, (int)Rows[0], 0, (int)Columns[0], ' ', ' ', 0);
                    msg_moremsg(false);
                    continue;
                }

                /* display more text, return to caller */
                lines_left = toscroll;
            }

            break;
        }

        /* clear the --more-- message */
        screen_fill((int)Rows[0] - 1, (int)Rows[0], 0, (int)Columns[0], ' ', ' ', 0);
        State = oldState;
        setmouse();
        if (quit_more)
        {
            msg_row = (int)Rows[0] - 1;
            msg_col = 0;
        }
        else if (cmdmsg_rl)
            msg_col = (int)Columns[0] - 1;

        return retval;
    }

    /*
     * Put a character on the screen at the current message position and advance
     * to the next position.  Only for printable ASCII!
     */
    /*private*/ static void msg_screen_putchar(int c, int attr)
    {
        msg_didout = true;          /* remember that line is not empty */
        screen_putchar(c, msg_row, msg_col, attr);
        if (cmdmsg_rl)
        {
            if (--msg_col == 0)
            {
                msg_col = (int)Columns[0];
                msg_row++;
            }
        }
        else
        {
            if ((int)Columns[0] <= ++msg_col)
            {
                msg_col = 0;
                msg_row++;
            }
        }
    }

    /*private*/ static void msg_moremsg(boolean full)
    {
        Bytes s = u8("-- More --");

        int attr = hl_attr(HLF_M);
        screen_puts(s, (int)Rows[0] - 1, 0, attr);
        if (full)
            screen_puts(u8(" SPACE/d/j: screen/page/line down, b/u/k: up, q: quit "),
                    (int)Rows[0] - 1, mb_string2cells(s, -1), attr);
    }

    /*
     * Repeat the message for the current mode: ASKMORE, EXTERNCMD, CONFIRM or exmode_active.
     */
    /*private*/ static void repeat_message()
    {
        if (State == ASKMORE)
        {
            msg_moremsg(true);      /* display --more-- message again */
            msg_row = (int)Rows[0] - 1;
        }
        else if (State == CONFIRM)
        {
            display_confirm_msg();  /* display ":confirm" message again */
            msg_row = (int)Rows[0] - 1;
        }
        else if (State == EXTERNCMD)
        {
            windgoto(msg_row, msg_col); /* put cursor back */
        }
        else if (State == HITRETURN || State == SETWSIZE)
        {
            if (msg_row == (int)Rows[0] - 1)
            {
                /* Avoid drawing the "hit-enter" prompt below the previous one,
                 * overwrite it.  Esp. useful when regaining focus and a
                 * FocusGained autocmd exists but didn't draw anything. */
                msg_didout = false;
                msg_col = 0;
                msg_clr_eos();
            }
            hit_return_msg();
            msg_row = (int)Rows[0] - 1;
        }
    }

    /*
     * msg_check_screen - check if the screen is initialized.
     * Also check msg_row and msg_col, if they are too big it may cause a crash.
     * While starting the GUI the terminal codes will be set for the GUI, but the
     * output goes to the terminal.  Don't use the terminal codes then.
     */
    /*private*/ static boolean msg_check_screen()
    {
        if (!full_screen || !screen_valid(false))
            return false;

        if ((int)Rows[0] <= msg_row)
            msg_row = (int)Rows[0] - 1;
        if ((int)Columns[0] <= msg_col)
            msg_col = (int)Columns[0] - 1;
        return true;
    }

    /*
     * Clear from current message position to end of screen.
     * Skip this when ":silent" was used, no need to clear for redirection.
     */
    /*private*/ static void msg_clr_eos()
    {
        if (msg_silent == 0)
            msg_clr_eos_force();
    }

    /*
     * Clear from current message position to end of screen.
     * Note: msg_col is not updated, so we remember the end of the message for msg_check().
     */
    /*private*/ static void msg_clr_eos_force()
    {
        if (msg_use_printf())
        {
            if (full_screen)        /* only when termcap codes are valid */
            {
                if (T_CD[0].at(0) != NUL)
                    out_str(T_CD[0]);  /* clear to end of display */
                else if (T_CE[0].at(0) != NUL)
                    out_str(T_CE[0]);  /* clear to end of line */
            }
        }
        else
        {
            if (cmdmsg_rl)
            {
                screen_fill(msg_row, msg_row + 1, 0, msg_col + 1, ' ', ' ', 0);
                screen_fill(msg_row + 1, (int)Rows[0], 0, (int)Columns[0], ' ', ' ', 0);
            }
            else
            {
                screen_fill(msg_row, msg_row + 1, msg_col, (int)Columns[0], ' ', ' ', 0);
                screen_fill(msg_row + 1, (int)Rows[0], 0, (int)Columns[0], ' ', ' ', 0);
            }
        }
    }

    /*
     * Clear the command line.
     */
    /*private*/ static void msg_clr_cmdline()
    {
        msg_row = cmdline_row;
        msg_col = 0;
        msg_clr_eos_force();
    }

    /*
     * end putting a message on the screen
     * call wait_return if the message does not fit in the available space
     * return true if wait_return not called.
     */
    /*private*/ static boolean msg_end()
    {
        /*
         * If the string is larger than the window,
         * or the ruler option is set and we run into it,
         * we have to redraw the window.
         * Do not do this if we are abandoning the file or editing the command line.
         */
        if (!exiting && need_wait_return && (State & CMDLINE) == 0)
        {
            wait_return(FALSE);
            return false;
        }
        out_flush();
        return true;
    }

    /*
     * If the written message runs into the shown command or ruler, we have to
     * wait for hit-return and redraw the window later.
     */
    /*private*/ static void msg_check()
    {
        if (msg_row == (int)Rows[0] - 1 && sc_col <= msg_col)
        {
            need_wait_return = true;
            redraw_cmdline = true;
        }
    }

    /*private*/ static int rw_cur_col;

    /*
     * May write a string to the redirection file.
     * When "maxlen" is -1 write the whole string, otherwise up to "maxlen" bytes.
     */
    /*private*/ static void redir_write(Bytes str, int maxlen)
    {
        /* Don't do anything for displaying prompts and the like. */
        if (redir_off)
            return;

        /* If 'verbosefile' is set prepare for writing in that file. */
        if (p_vfile[0].at(0) != NUL && verbose_fd == null)
            verbose_open();

        if (redirecting())
        {
            Bytes s = str;

            /* If the string doesn't start with CR or NL, go to msg_col. */
            if (s.at(0) != (byte)'\n' && s.at(0) != (byte)'\r')
            {
                while (rw_cur_col < msg_col)
                {
                    if (redir_reg != 0)
                        write_reg_contents(redir_reg, u8(" "), -1, true);
                    else if (redir_vname)
                        var_redir_str(u8(" "), -1);
                    else if (redir_fd != null)
                        libC.fputs(u8(" "), redir_fd);
                    if (verbose_fd != null)
                        libC.fputs(u8(" "), verbose_fd);
                    rw_cur_col++;
                }
            }

            if (redir_reg != 0)
                write_reg_contents(redir_reg, s, maxlen, true);
            if (redir_vname)
                var_redir_str(s, maxlen);

            /* Write and adjust the current column. */
            while (s.at(0) != NUL && (maxlen < 0 || BDIFF(s, str) < maxlen))
            {
                if (redir_reg == 0 && !redir_vname)
                    if (redir_fd != null)
                        libc.putc(s.at(0), redir_fd);
                if (verbose_fd != null)
                    libc.putc(s.at(0), verbose_fd);
                if (s.at(0) == (byte)'\r' || s.at(0) == (byte)'\n')
                    rw_cur_col = 0;
                else if (s.at(0) == (byte)'\t')
                    rw_cur_col += (8 - rw_cur_col % 8);
                else
                    rw_cur_col++;
                s = s.plus(1);
            }

            if (msg_silent != 0)    /* should update msg_col */
                msg_col = rw_cur_col;
        }
    }

    /*private*/ static boolean redirecting()
    {
        return (redir_fd != null || p_vfile[0].at(0) != NUL || redir_reg != 0 || redir_vname);
    }

    /*
     * Before giving verbose message.
     * Must always be called paired with verbose_leave()!
     */
    /*private*/ static void verbose_enter()
    {
        if (p_vfile[0].at(0) != NUL)
            msg_silent++;
    }

    /*
     * After giving verbose message.
     * Must always be called paired with verbose_enter()!
     */
    /*private*/ static void verbose_leave()
    {
        if (p_vfile[0].at(0) != NUL)
            if (--msg_silent < 0)
                msg_silent = 0;
    }

    /*
     * Like verbose_enter() and set msg_scroll when displaying the message.
     */
    /*private*/ static void verbose_enter_scroll()
    {
        if (p_vfile[0].at(0) != NUL)
            msg_silent++;
        else
            /* always scroll up, don't overwrite */
            msg_scroll = true;
    }

    /*
     * Like verbose_leave() and set cmdline_row when displaying the message.
     */
    /*private*/ static void verbose_leave_scroll()
    {
        if (p_vfile[0].at(0) != NUL)
        {
            if (--msg_silent < 0)
                msg_silent = 0;
        }
        else
            cmdline_row = msg_row;
    }

    /*
     * Called when 'verbosefile' is set: stop writing to the file.
     */
    /*private*/ static void verbose_stop()
    {
        if (verbose_fd != null)
        {
            libc.fclose(verbose_fd);
            verbose_fd = null;
        }
        verbose_did_open = false;
    }

    /*
     * Open the file 'verbosefile'.
     * Return false or true.
     */
    /*private*/ static boolean verbose_open()
    {
        if (verbose_fd == null && !verbose_did_open)
        {
            /* Only give the error message once. */
            verbose_did_open = true;

            verbose_fd = libC.fopen(p_vfile[0], u8("a"));
            if (verbose_fd == null)
            {
                emsg2(e_notopen, p_vfile[0]);
                return false;
            }
        }
        return true;
    }

    /*
     * Give a warning message (for searching).
     * Use 'w' highlighting and may repeat the message after redrawing
     */
    /*private*/ static void give_warning(Bytes message, boolean hl)
    {
        /* Don't do this for ":silent". */
        if (msg_silent != 0)
            return;

        /* Don't want a hit-enter prompt here. */
        no_wait_return++;

        set_vim_var_string(VV_WARNINGMSG, message, -1);
        keep_msg = null;
        if (hl)
            keep_msg_attr = hl_attr(HLF_W);
        else
            keep_msg_attr = 0;
        if (msg_attr(message, keep_msg_attr) && msg_scrolled == 0)
            set_keep_msg(message, keep_msg_attr);
        msg_didout = false;     /* overwrite this message */
        msg_nowait = true;      /* don't wait for this message */
        msg_col = 0;

        --no_wait_return;
    }

    /*
     * Advance msg cursor to column "col".
     */
    /*private*/ static void msg_advance(int col)
    {
        if (msg_silent != 0)        /* nothing to advance to */
        {
            msg_col = col;          /* for redirection, may fill it up later */
            return;
        }
        if ((int)Columns[0] <= col)         /* not enough room */
            col = (int)Columns[0] - 1;
        if (cmdmsg_rl)
        {
            while ((int)Columns[0] - col < msg_col)
                msg_putchar(' ');
        }
        else
        {
            while (msg_col < col)
                msg_putchar(' ');
        }
    }

    /*
     * Used for "confirm()" function, and the :confirm command prefix.
     * Versions which haven't got flexible dialogs yet, and console
     * versions, get this generic handler which uses the command line.
     *
     * Format of the "buttons" string:
     * "Button1Name\nButton2Name\nButton3Name"
     * The first button should normally be the default/accept
     * The second button should be the 'Cancel' button
     * Other buttons- use your imagination!
     * A '&' in a button name becomes a shortcut, so each '&' should be before a different letter.
     */
    /*private*/ static int do_dialog(Bytes message, Bytes buttons, int dfltbutton, boolean ex_cmd)
        /* ex_cmd: when true pressing : accepts default and starts Ex command */
    {
        int retval = 0;

        /* Don't output anything in silent mode ("ex -s"). */
        if (silent_mode)
            return dfltbutton;      /* return default option */

        int oldState = State;
        State = CONFIRM;
        setmouse();

        /*
         * Since we wait for a keypress,
         * don't make the user press RETURN as well afterwards.
         */
        no_wait_return++;
        Bytes hotkeys = msg_show_console_dialog(message, buttons, dfltbutton);

        if (hotkeys != null)
        {
            for ( ; ; )
            {
                /* Get a typed character directly from the user. */
                int c = get_keystroke();
                switch (c)
                {
                    case CAR:           /* User accepts default option */
                    case NL:
                        retval = dfltbutton;
                        break;

                    case Ctrl_C:        /* User aborts/cancels */
                    case ESC:
                        retval = 0;
                        break;

                    default:            /* Could be a hotkey? */
                        if (c < 0)      /* special keys are ignored here */
                            continue;
                        if (c == ':' && ex_cmd)
                        {
                            retval = dfltbutton;
                            ins_char_typebuf(':');
                            break;
                        }

                        /* Make the character lowercase, as chars in "hotkeys" are. */
                        c = utf_tolower(c);
                        retval = 1;
                        int i;
                        for (i = 0; hotkeys.at(i) != NUL; i++)
                        {
                            if (us_ptr2char(hotkeys.plus(i)) == c)
                                break;
                            i += us_ptr2len_cc(hotkeys.plus(i)) - 1;
                            retval++;
                        }
                        if (hotkeys.at(i) != NUL)
                            break;
                        /* No hotkey match, so keep waiting. */
                        continue;
                }
                break;
            }
        }

        State = oldState;
        setmouse();
        --no_wait_return;
        msg_end_prompt();

        return retval;
    }

    /*
     * Copy one character from "*from" to "*to", taking care of multi-byte characters.
     * Return the length of the character in bytes.
     */
    /*private*/ static int copy_char(Bytes from, Bytes to, boolean lowercase)
        /* lowercase: make character lower case */
    {
        if (lowercase)
        {
            int c = utf_tolower(us_ptr2char(from));
            return utf_char2bytes(c, to);
        }
        else
        {
            int len = us_ptr2len_cc(from);
            BCOPY(to, from, len);
            return len;
        }
    }

    /*
     * Format the dialog string, and display it at the bottom of the screen.
     * Return a string of hotkey chars (if defined) for each 'button'.
     * If a button has no hotkey defined, the first character of the button is used.
     * The hotkeys can be multi-byte characters, but without combining chars.
     *
     * Returns an allocated string with hotkeys, or null for error.
     */
    /*private*/ static Bytes msg_show_console_dialog(Bytes message, Bytes buttons, int dfltbutton)
    {
        Bytes hotk = null;

        final int HOTK_LEN = MB_MAXBYTES;
        int lenhotkey = HOTK_LEN;           /* count first button */
        int len = 0;

        Bytes msgp = null;
        Bytes hotkp = null;

        final int HAS_HOTKEY_LEN = 30;

        boolean[] has_hotkey = new boolean[HAS_HOTKEY_LEN];
        has_hotkey[0] = false;
        boolean first_hotkey = false;       /* first char of button is hotkey */

        /*
         * First loop: compute the size of memory to allocate.
         * Second loop: copy to the allocated memory.
         */
        for (int copy = 0; copy <= 1; copy++)
        {
            Bytes r = buttons;
            int idx = 0;
            while (r.at(0) != NUL)
            {
                if (r.at(0) == DLG_BUTTON_SEP)
                {
                    if (copy != 0)
                    {
                        (msgp = msgp.plus(1)).be(-1, (byte)',');
                        (msgp = msgp.plus(1)).be(-1, (byte)' ');          /* '\n' -> ', ' */

                        /* advance to next hotkey and set default hotkey */
                        hotkp = hotkp.plus(strlen(hotkp));
                        hotkp.be(copy_char(r.plus(1), hotkp, true), NUL);
                        if (dfltbutton != 0)
                            --dfltbutton;

                        /* If no hotkey is specified first char is used. */
                        if (idx < HAS_HOTKEY_LEN - 1 && !has_hotkey[++idx])
                            first_hotkey = true;
                    }
                    else
                    {
                        len += 3;               /* '\n' -> ', '; 'x' -> '(x)' */
                        lenhotkey += HOTK_LEN;  /* each button needs a hotkey */
                        if (idx < HAS_HOTKEY_LEN - 1)
                            has_hotkey[++idx] = false;
                    }
                }
                else if (r.at(0) == DLG_HOTKEY_CHAR || first_hotkey)
                {
                    if (r.at(0) == DLG_HOTKEY_CHAR)
                        r = r.plus(1);
                    first_hotkey = false;
                    if (copy != 0)
                    {
                        if (r.at(0) == DLG_HOTKEY_CHAR)          /* '&&a' -> '&a' */
                            (msgp = msgp.plus(1)).be(-1, r.at(0));
                        else
                        {
                            /* '&a' -> '[a]' */
                            (msgp = msgp.plus(1)).be(-1, (dfltbutton == 1) ? (byte)'[' : (byte)'(');
                            msgp = msgp.plus(copy_char(r, msgp, false));
                            (msgp = msgp.plus(1)).be(-1, (dfltbutton == 1) ? (byte)']' : (byte)')');

                            /* redefine hotkey */
                            hotkp.be(copy_char(r, hotkp, true), NUL);
                        }
                    }
                    else
                    {
                        len++;          /* '&a' -> '[a]' */
                        if (idx < HAS_HOTKEY_LEN - 1)
                            has_hotkey[idx] = true;
                    }
                }
                else
                {
                    /* everything else copy literally */
                    if (copy != 0)
                        msgp = msgp.plus(copy_char(r, msgp, false));
                }

                /* advance to the next character */
                r = r.plus(us_ptr2len_cc(r));
            }

            if (copy != 0)
            {
                (msgp = msgp.plus(1)).be(-1, (byte)':');
                (msgp = msgp.plus(1)).be(-1, (byte)' ');
                msgp.be(0, NUL);
            }
            else
            {
                len += strlen(message)
                           + 2                      /* for the NL's */
                           + strlen(buttons)
                           + 3;                    /* for the ": " and NUL */
                lenhotkey++;                        /* for the NUL */

                /* If no hotkey is specified first char is used. */
                if (!has_hotkey[0])
                {
                    first_hotkey = true;
                    len += 2;               /* "x" -> "[x]" */
                }

                /*
                 * Now allocate and load the strings
                 */
                confirm_msg = new Bytes(len);
                confirm_msg.be(0, (byte)'\n');
                STRCPY(confirm_msg.plus(1), message);

                msgp = confirm_msg.plus(1 + strlen(message));

                hotk = new Bytes(lenhotkey);
                hotkp = hotk;

                /* Define first default hotkey.
                 * Keep the hotkey string NUL terminated to avoid reading past the end. */
                hotkp.be(copy_char(buttons, hotkp, true), NUL);

                /* Remember where the choices start,
                 * displaying starts here when "hotkp" typed at the more prompt. */
                confirm_msg_tail = msgp;
                (msgp = msgp.plus(1)).be(-1, (byte)'\n');
            }
        }

        display_confirm_msg();

        return hotk;
    }

    /*
     * Display the ":confirm" message.  Also called when screen resized.
     */
    /*private*/ static void display_confirm_msg()
    {
        /* avoid that 'q' at the more prompt truncates the message here */
        confirm_msg_used++;
        if (confirm_msg != null)
            msg_puts_attr(confirm_msg, hl_attr(HLF_M));
        --confirm_msg_used;
    }

    /*private*/ static int vim_dialog_yesno(Bytes message, int dflt)
    {
        if (do_dialog(message, u8("&Yes\n&No"), dflt, false) == 1)
            return VIM_YES;

        return VIM_NO;
    }

    /*private*/ static int vim_dialog_yesnocancel(Bytes message, int dflt)
    {
        switch (do_dialog(message, u8("&Yes\n&No\n&Cancel"), dflt, false))
        {
            case 1: return VIM_YES;
            case 2: return VIM_NO;
        }
        return VIM_CANCEL;
    }

    /*private*/ static int vim_dialog_yesnoallcancel(Bytes message, int dflt)
    {
        switch (do_dialog(message, u8("&Yes\n&No\nSave &All\n&Discard All\n&Cancel"), dflt, false))
        {
            case 1: return VIM_YES;
            case 2: return VIM_NO;
            case 3: return VIM_ALL;
            case 4: return VIM_DISCARDALL;
        }
        return VIM_CANCEL;
    }

    /*
     * This code was included to provide a portable snprintf().
     * Some systems may provide their own, but we always use this one for consistency.
     *
     * This code is based on snprintf.c - a portable implementation of snprintf
     * by Mark Martinec <mark.martinec@ijs.si>, Version 2.2, 2000-10-06.
     * Included with permission.  It was heavily modified to fit in Vim.
     * The original code, including useful comments, can be found here:
     *      http://www.ijs.si/software/snprintf/
     *
     * This snprintf() only supports the following conversion specifiers:
     * s, c, d, u, o, x, X, p  (and synonyms: i, D, U, O - see below)
     * with flags: '-', '+', ' ', '0' and '#'.
     * An asterisk is supported for field width as well as precision.
     *
     * Limited support for floating point was added: 'f', 'e', 'E', 'g', 'G'.
     *
     * Length modifiers 'h' (short int) and 'l' (long int) are supported.
     * 'll' (long long int) is not supported.
     *
     * The locale is not used, the string is used as a byte string.  This is only
     * relevant for double-byte encodings where the second byte may be '%'.
     *
     * It is permitted for "str_m" to be zero, and it is permitted to specify null
     * pointer for resulting string argument if "str_m" is zero (as per ISO C99).
     *
     * The return value is the number of characters which would be generated
     * for the given input, excluding the trailing NUL.  If this value
     * is greater or equal to "str_m", not all characters from the result
     * have been stored in str, output bytes beyond the ("str_m"-1) -th character
     * are discarded.  If "str_m" is greater than zero it is guaranteed
     * the resulting string will be NUL-terminated.
     */

    /* Like vim_snprintf() but append to the string. */
    /*private*/ static int vim_snprintf_add(Bytes str, int str_m, Bytes fmt, Object... args)
    {
        int len = strlen(str), space = (len < str_m) ? str_m - len : 0;

        return vim_snprintf(str.plus(len), space, fmt, args);
    }

    /*private*/ static int vim_snprintf(Bytes str, int str_m, Bytes fmt, Object... args)
    {
        int str_l = 0;
        int ai = 0;

        Bytes p = fmt;
        if (p == null)
            p = u8("");

        while (p.at(0) != NUL)
        {
            if (p.at(0) != (byte)'%')
            {
                Bytes q = STRCHR(p.plus(1), (byte)'%');
                int n = (q == null) ? strlen(p) : BDIFF(q, p);

                /* Copy up to the next '%' or NUL without any changes. */
                if (str_l < str_m)
                {
                    int avail = str_m - str_l;

                    BCOPY(str, str_l, p, 0, (avail < n) ? avail : n);
                }
                p = p.plus(n);
                str_l += n;
            }
            else
            {
                int min_field_width = 0, precision = 0;
                boolean precision_specified = false;
                boolean zero_padding = false, justify_left = false;
                boolean alternate_form = false, force_sign = false;

                /* If both the ' ' and '+' flags appear, the ' ' flag should be ignored. */
                boolean space_for_positive = true;

                /* allowed values: \0, h, l, L */
                byte length_modifier = NUL;

                /* On my system 1e308 is the biggest number possible.
                 * That sounds reasonable to use as the maximum printable.
                 */
                final int TMP_LEN = 350;
                /* temporary buffer for simple numeric -> string conversion */
                Bytes tmp = new Bytes(TMP_LEN);

                /* natural field width of arg without padding and sign */
                int str_arg_l;

                /* unsigned char argument value - only defined for c conversion.
                 * N.B. standard explicitly states the char argument for the c conversion is unsigned */
                Bytes uchar_arg = new Bytes(1);

                /* number of zeros to be inserted for numeric conversions
                 * as required by the precision or minimal field width */
                int number_of_zeros_to_pad = 0;

                /* index into 'tmp' where zero padding is to be inserted */
                int zero_padding_insertion_ind = 0;

                /* current conversion specifier character */
                byte fmt_spec = NUL;

                /* string address in case of string argument */
                Bytes str_arg = null;

                p = p.plus(1);                /* skip '%' */

                /* parse flags */
                while (p.at(0) == (byte)'0' || p.at(0) == (byte)'-' || p.at(0) == (byte)'+' || p.at(0) == (byte)' ' || p.at(0) == (byte)'#' || p.at(0) == (byte)'\'')
                {
                    switch (p.at(0))
                    {
                        case '0': zero_padding = true; break;
                        case '-': justify_left = true; break;
                        case '+': force_sign = true; space_for_positive = false; break;
                        case ' ': force_sign = true;
                                  /* If both the ' ' and '+' flags appear,
                                   * the ' ' flag should be ignored. */
                                  break;
                        case '#': alternate_form = true; break;
                        case '\'': break;
                    }
                    p = p.plus(1);
                }
                /* If the '0' and '-' flags both appear, the '0' flag should be ignored. */

                /* parse field width */
                if (p.at(0) == (byte)'*')
                {
                    p = p.plus(1);
                    int j = (int)args[ai++];
                    if (0 <= j)
                        min_field_width = j;
                    else
                    {
                        min_field_width = -j;
                        justify_left = true;
                    }
                }
                else if (asc_isdigit((int)p.at(0)))
                {
                    /* size_t could be wider than unsigned int;
                     * make sure we treat argument like common implementations do */
                    long uj = (p = p.plus(1)).at(-1) - '0';

                    while (asc_isdigit((int)p.at(0)))
                        uj = 10 * uj + (long)((p = p.plus(1)).at(-1) - '0');

                    min_field_width = (int)(uj & 0x7fffffffL);
                }

                /* parse precision */
                if (p.at(0) == (byte)'.')
                {
                    p = p.plus(1);
                    precision_specified = true;
                    if (p.at(0) == (byte)'*')
                    {
                        int j = (int)args[ai++];
                        p = p.plus(1);
                        if (0 <= j)
                            precision = j;
                        else
                        {
                            precision_specified = false;
                            precision = 0;
                        }
                    }
                    else if (asc_isdigit((int)p.at(0)))
                    {
                        /* size_t could be wider than unsigned int;
                         * make sure we treat argument like common implementations do */
                        long uj = (p = p.plus(1)).at(-1) - '0';

                        while (asc_isdigit((int)p.at(0)))
                            uj = 10 * uj + (long)((p = p.plus(1)).at(-1) - '0');

                        precision = (int)(uj & 0x7fffffffL);
                    }
                }

                /* parse 'h', 'l' and 'll' length modifiers */
                if (p.at(0) == (byte)'h' || p.at(0) == (byte)'l')
                {
                    length_modifier = p.at(0);
                    p = p.plus(1);
                    if (length_modifier == 'l' && p.at(0) == (byte)'l')
                    {
                        /* double l = long long */
                        length_modifier = 'l';      /* treat it as a single 'l' */
                        p = p.plus(1);
                    }
                }
                fmt_spec = p.at(0);

                /* common synonyms: */
                switch (fmt_spec)
                {
                    case 'i': fmt_spec = 'd'; break;
                    case 'D': fmt_spec = 'd'; length_modifier = 'l'; break;
                    case 'U': fmt_spec = 'u'; length_modifier = 'l'; break;
                    case 'O': fmt_spec = 'o'; length_modifier = 'l'; break;
                    case 'F': fmt_spec = 'f'; break;
                    default: break;
                }

                /* get parameter value, do initial processing */
                switch (fmt_spec)
                {
                    /* '%' and 'c' behave similar to 's' regarding flags and field widths */
                    case '%':
                    case 'c':
                    case 's':
                    case 'S':
                    {
                        length_modifier = NUL;
                        str_arg_l = 1;
                        switch (fmt_spec)
                        {
                            case '%':
                                str_arg = p;
                                break;

                            case 'c':
                            {
                                int c = (int)args[ai++];

                                /* standard demands unsigned char */
                                uchar_arg.be(0, /*char_u(*/c/*)*/);
                                str_arg = uchar_arg;
                                break;
                            }

                            case 's':
                            case 'S':
                            {
                                str_arg = (Bytes)args[ai++];
                                if (str_arg == null)
                                {
                                    str_arg = u8("[NULL]");
                                    str_arg_l = 6;
                                }
                                /* make sure not to address string beyond the specified precision !!! */
                                else if (!precision_specified)
                                    str_arg_l = strlen(str_arg);
                                /* truncate string if necessary as requested by precision */
                                else if (precision == 0)
                                    str_arg_l = 0;
                                else
                                {
                                    final int roof = 0x7fffffff;
                                    /* memchr() on HP does not like n > 2^31 !!! */
                                    Bytes q = MEMCHR(str_arg, NUL, (precision <= roof) ? precision : roof);
                                    str_arg_l = (q == null) ? precision : BDIFF(q, str_arg);
                                }
                                if (fmt_spec == 'S')
                                {
                                    if (min_field_width != 0)
                                        min_field_width += strlen(str_arg) - us_string2cells(str_arg, -1);
                                    if (precision != 0)
                                    {
                                        Bytes q = str_arg;

                                        for (int i = 0; i < precision && q.at(0) != NUL; i++)
                                            q = q.plus(us_ptr2len_cc(q));

                                        str_arg_l = precision = BDIFF(q, str_arg);
                                    }
                                }
                                break;
                            }

                            default:
                                break;
                        }
                        break;
                    }

                    case 'd': case 'u': case 'o': case 'x': case 'X': case 'p':
                    {
                        /* u, o, x, X and p conversion specifiers imply the value is unsigned;
                         * d implies a signed value */

                        /* 0 if numeric argument is zero (or if pointer is null for 'p'),
                         * +1 if greater than zero (or nonzero for unsigned arguments),
                         * -1 if negative (unsigned argument is never negative) */
                        int arg_sign = 0;

                        /* only defined for length modifier h, or for no length modifiers */
                        int int_arg = 0;

                        /* only defined for length modifier l */
                        long long_arg = 0;

                        if (fmt_spec == 'd')
                        {
                            /* signed */
                            switch (length_modifier)
                            {
                                case NUL:
                                case 'h':
                                {
                                    /* char and short arguments are passed as int. */
                                    int_arg = (int)args[ai++];
                                    if (0 < int_arg)
                                        arg_sign =  1;
                                    else if (int_arg < 0)
                                        arg_sign = -1;
                                    break;
                                }
                                case 'l':
                                {
                                    long_arg = (long)args[ai++];
                                    if (0 < long_arg)
                                        arg_sign =  1;
                                    else if (long_arg < 0)
                                        arg_sign = -1;
                                    break;
                                }
                            }
                        }
                        else
                        {
                            /* unsigned */
                            switch (length_modifier)
                            {
                                case NUL:
                                case 'h':
                                {
                                    int_arg = (int)args[ai++];
                                    if (int_arg != 0)
                                        arg_sign = 1;
                                    break;
                                }
                                case 'l':
                                {
                                    long_arg = (long)args[ai++];
                                    if (long_arg != 0)
                                        arg_sign = 1;
                                    break;
                                }
                            }
                        }

                        str_arg = tmp;
                        str_arg_l = 0;

                        /* NOTE:
                         *   For d, i, u, o, x, and X conversions, if precision is
                         *   specified, the '0' flag should be ignored.  This is so
                         *   with Solaris 2.6, Digital UNIX 4.0, HPUX 10, Linux,
                         *   FreeBSD, NetBSD; but not with Perl.
                         */
                        if (precision_specified)
                            zero_padding = false;
                        if (fmt_spec == 'd')
                        {
                            if (force_sign && 0 <= arg_sign)
                                tmp.be(str_arg_l++, space_for_positive ? (byte)' ' : (byte)'+');
                            /* leave negative numbers for sprintf to handle, to
                             * avoid handling tricky cases like (short int)-32768 */
                        }
                        else if (alternate_form)
                        {
                            if (arg_sign != 0 && (fmt_spec == 'x' || fmt_spec == 'X') )
                            {
                                tmp.be(str_arg_l++, (byte)'0');
                                tmp.be(str_arg_l++, fmt_spec);
                            }
                            /* alternate form should have no effect for 'p' conversion, but ... */
                        }

                        zero_padding_insertion_ind = str_arg_l;
                        if (!precision_specified)
                            precision = 1;      /* default precision is 1 */
                        if (precision == 0 && arg_sign == 0)
                        {
                            /* When zero value is formatted with an explicit precision 0,
                             * the resulting formatted string is empty (d, i, u, o, x, X, p).
                             */
                        }
                        else
                        {
                            Bytes f = new Bytes(5);
                            int f_l = 0;

                            /* construct a simple format string for sprintf */
                            f.be(f_l++, (byte)'%');
                            if (length_modifier == NUL)
                                ;
                            else if (length_modifier == '2')
                            {
                                f.be(f_l++, (byte)'l');
                                f.be(f_l++, (byte)'l');
                            }
                            else
                                f.be(f_l++, length_modifier);
                            f.be(f_l++, fmt_spec);
                            f.be(f_l++, NUL);

                            if (fmt_spec == 'd')
                            {
                                /* signed */
                                switch (length_modifier)
                                {
                                    case NUL:
                                    case 'h':
                                        str_arg_l += libC.sprintf(tmp.plus(str_arg_l), f, int_arg);
                                        break;
                                    case 'l':
                                        str_arg_l += libC.sprintf(tmp.plus(str_arg_l), f, long_arg);
                                        break;
                                }
                            }
                            else
                            {
                                /* unsigned */
                                switch (length_modifier)
                                {
                                    case NUL:
                                    case 'h':
                                        str_arg_l += libC.sprintf(tmp.plus(str_arg_l), f, int_arg);
                                        break;
                                    case 'l':
                                        str_arg_l += libC.sprintf(tmp.plus(str_arg_l), f, long_arg);
                                        break;
                                }
                            }

                            /* include the optional minus sign and possible "0x"
                             * in the region before the zero padding insertion point */
                            if (zero_padding_insertion_ind < str_arg_l
                                    && tmp.at(zero_padding_insertion_ind) == (byte)'-')
                                zero_padding_insertion_ind++;
                            if (zero_padding_insertion_ind + 1 < str_arg_l
                                    && tmp.at(zero_padding_insertion_ind) == (byte)'0'
                                    && (tmp.at(zero_padding_insertion_ind + 1) == (byte)'x'
                                            || tmp.at(zero_padding_insertion_ind + 1) == (byte)'X'))
                                zero_padding_insertion_ind += 2;
                        }

                        int num_of_digits = str_arg_l - zero_padding_insertion_ind;

                        if (alternate_form && fmt_spec == 'o'
                            /* unless zero is already the first character */
                            && !(zero_padding_insertion_ind < str_arg_l && tmp.at(zero_padding_insertion_ind) == (byte)'0'))
                        {
                            /* assure leading zero for alternate-form octal numbers */
                            if (!precision_specified || precision < num_of_digits + 1)
                            {
                                /* precision is increased to force the first character to be zero,
                                 * except if a zero value is formatted with an explicit precision of zero
                                 */
                                precision = num_of_digits + 1;
                                precision_specified = true;
                            }
                        }
                        /* zero padding to specified precision? */
                        if (num_of_digits < precision)
                            number_of_zeros_to_pad = precision - num_of_digits;

                        /* zero padding to specified minimal field width? */
                        if (!justify_left && zero_padding)
                        {
                            int n = min_field_width - (str_arg_l + number_of_zeros_to_pad);
                            if (0 < n)
                                number_of_zeros_to_pad += n;
                        }
                        break;
                    }

                    default:
                    {
                        /* unrecognized conversion specifier, keep format string as-is */
                        zero_padding = false;       /* turn zero padding off for non-numeric conversion */
                        justify_left = true;
                        min_field_width = 0;                /* reset flags */

                        /* discard the unrecognized conversion,
                         * just keep the unrecognized conversion character */
                        str_arg = p;
                        str_arg_l = 0;
                        if (p.at(0) != NUL)
                            str_arg_l++;        /* include invalid conversion specifier
                                                 * unchanged if not at end-of-string */
                        break;
                    }
                }

                if (p.at(0) != NUL)
                    p = p.plus(1);                /* step over the just processed conversion specifier */

                /* insert padding to the left as requested by min_field_width;
                 * this does not include the zero padding in case of numerical conversions */
                if (!justify_left)
                {
                    /* left padding with blank or zero */
                    int pn = min_field_width - (str_arg_l + number_of_zeros_to_pad);
                    if (0 < pn)
                    {
                        if (str_l < str_m)
                        {
                            int avail = str_m - str_l;

                            BFILL(str, str_l, zero_padding ? (byte)'0' : (byte)' ', (avail < pn) ? avail : pn);
                        }
                        str_l += pn;
                    }
                }

                /* zero padding as requested by the precision or by the minimal
                 * field width for numeric conversions required? */
                if (number_of_zeros_to_pad == 0)
                {
                    /* will not copy first part of numeric right now,
                     * force it to be copied later in its entirety */
                    zero_padding_insertion_ind = 0;
                }
                else
                {
                    /* insert first part of numerics (sign or '0x') before zero padding */
                    int zn = zero_padding_insertion_ind;
                    if (0 < zn)
                    {
                        if (str_l < str_m)
                        {
                            int avail = str_m - str_l;

                            BCOPY(str, str_l, str_arg, 0, (avail < zn) ? avail : zn);
                        }
                        str_l += zn;
                    }

                    /* insert zero padding as requested by the precision or min field width */
                    zn = number_of_zeros_to_pad;
                    if (0 < zn)
                    {
                        if (str_l < str_m)
                        {
                            int avail = str_m - str_l;

                            BFILL(str, str_l, (byte)'0', (avail < zn) ? avail : zn);
                        }
                        str_l += zn;
                    }
                }

                /* insert formatted string (or as-is conversion specifier for unknown conversions) */
                {
                    int sn = str_arg_l - zero_padding_insertion_ind;
                    if (0 < sn)
                    {
                        if (str_l < str_m)
                        {
                            int avail = str_m - str_l;

                            BCOPY(str, str_l, str_arg, zero_padding_insertion_ind, (avail < sn) ? avail : sn);
                        }
                        str_l += sn;
                    }
                }

                /* insert right padding */
                if (justify_left)
                {
                    /* right blank padding to the field width */
                    int pn = min_field_width - (str_arg_l + number_of_zeros_to_pad);
                    if (0 < pn)
                    {
                        if (str_l < str_m)
                        {
                            int avail = str_m - str_l;

                            BFILL(str, str_l, (byte)' ', (avail < pn) ? avail : pn);
                        }
                        str_l += pn;
                    }
                }
            }
        }

        if (0 < str_m)
        {
            /* make sure the string is nul-terminated even at the expense of
             * overwriting the last character (shouldn't happen, but just in case)
             */
            str.be((str_l <= str_m - 1) ? str_l : str_m - 1, NUL);
        }

        /* Return the number of characters formatted (excluding trailing nul character), that is,
         * the number of characters that would have been written to the buffer if it were large enough.
         */
        return str_l;
    }

    /*
     * Code to handle user-settable options.  This is all pretty much table-
     * driven.  Checklist for adding a new option:
     * - Put it in the options array below (copy an existing entry).
     * - For a global option: Add a variable for it in option.h.
     * - For a buffer or window local option:
     *   - Add a PV_XX entry to the enum below.
     *   - Add a variable to the window or buffer struct in structs.h.
     *   - For a window option, add some code to copy_winopt().
     *   - For a buffer option, add some code to buf_copy_options().
     *   - For a buffer string option, add code to check_buf_options().
     * - If it's a numeric option, add any necessary bounds checks to do_set().
     * - If it's a list of flags, add some code in do_set(), search for WW_ALL.
     * When making changes:
     * - When an entry has the P_VIM flag, or is lacking the P_VI_DEF flag,
     *   add a comment at the help for the 'compatible' option.
     */

    /*
     * The options that are local to a window or buffer have "indir" set to
     * one of these values.  Special values:
     * PV_NONE: global option.
     * PV_WIN is added: window-local option
     * PV_BUF is added: buffer-local option
     * PV_BOTH is added: global option which also has a local value.
     */
    /*private*/ static final int
        PV_NONE = 0,
        PV_BOTH = 0x1000,
        PV_WIN  = 0x2000,
        PV_BUF  = 0x4000,
        PV_MASK = 0x0fff;

    /*private*/ static final int opt_win(int x)
    {
        return PV_WIN + x;
    }

    /*private*/ static final int opt_buf(int x)
    {
        return PV_BUF + x;
    }

    /*private*/ static final int opt_both(int x)
    {
        return PV_BOTH + x;
    }

    /*
     * Definition of the PV_ values for buffer-local options.
     * The BV_ values are defined in option.h.
     */
    /*private*/ static final int
        PV_AI   = 16384,    // = opt_buf(BV_AI),
        PV_AR   = 20481,    // = opt_both(opt_buf(BV_AR)),
        PV_BIN  = 16386,    // = opt_buf(BV_BIN),
        PV_BL   = 16387,    // = opt_buf(BV_BL),
        PV_BOMB = 16388,    // = opt_buf(BV_BOMB),
        PV_CI   = 16389,    // = opt_buf(BV_CI),
        PV_CIN  = 16390,    // = opt_buf(BV_CIN),
        PV_CINK = 16391,    // = opt_buf(BV_CINK),
        PV_CINO = 16392,    // = opt_buf(BV_CINO),
        PV_CINW = 16393,    // = opt_buf(BV_CINW),
        PV_CM   = 20490,    // = opt_both(opt_buf(BV_CM)),
        PV_COM  = 16395,    // = opt_buf(BV_COM),
        PV_EOL  = 16396,    // = opt_buf(BV_EOL),
        PV_EP   = 20493,    // = opt_both(opt_buf(BV_EP)),
        PV_ET   = 16398,    // = opt_buf(BV_ET),
        PV_FENC = 16399,    // = opt_buf(BV_FENC),
        PV_FEX  = 16400,    // = opt_buf(BV_FEX),
        PV_FF   = 16401,    // = opt_buf(BV_FF),
        PV_FLP  = 16402,    // = opt_buf(BV_FLP),
        PV_FO   = 16403,    // = opt_buf(BV_FO),
        PV_FT   = 16404,    // = opt_buf(BV_FT),
        PV_IMI  = 16405,    // = opt_buf(BV_IMI),
        PV_IMS  = 16406,    // = opt_buf(BV_IMS),
        PV_INDE = 16407,    // = opt_buf(BV_INDE),
        PV_INDK = 16408,    // = opt_buf(BV_INDK),
        PV_INF  = 16409,    // = opt_buf(BV_INF),
        PV_ISK  = 16410,    // = opt_buf(BV_ISK),
        PV_KP   = 20507,    // = opt_both(opt_buf(BV_KP)),
        PV_LISP = 16412,    // = opt_buf(BV_LISP),
        PV_LW   = 20509,    // = opt_both(opt_buf(BV_LW)),
        PV_MA   = 16414,    // = opt_buf(BV_MA),
        PV_MOD  = 16415,    // = opt_buf(BV_MOD),
        PV_MPS  = 16416,    // = opt_buf(BV_MPS),
        PV_NF   = 16417,    // = opt_buf(BV_NF),
        PV_PI   = 16418,    // = opt_buf(BV_PI),
        PV_QE   = 16419,    // = opt_buf(BV_QE),
        PV_RO   = 16420,    // = opt_buf(BV_RO),
        PV_SI   = 16421,    // = opt_buf(BV_SI),
        PV_SMC  = 16422,    // = opt_buf(BV_SMC),
        PV_SYN  = 16423,    // = opt_buf(BV_SYN),
        PV_STS  = 16424,    // = opt_buf(BV_STS),
        PV_SW   = 16425,    // = opt_buf(BV_SW),
        PV_TS   = 16426,    // = opt_buf(BV_TS),
        PV_TW   = 16427,    // = opt_buf(BV_TW),
        PV_TX   = 16428,    // = opt_buf(BV_TX),
        PV_UDF  = 16429,    // = opt_buf(BV_UDF),
        PV_UL   = 20526,    // = opt_both(opt_buf(BV_UL)),
        PV_WM   = 16431;    // = opt_buf(BV_WM);

    /*
     * Definition of the PV_ values for window-local options.
     * The WV_ values are defined in option.h.
     */
    /*private*/ static final int
        PV_LIST   = 8192,   // = opt_win(WV_LIST),
        PV_COCU   = 8193,   // = opt_win(WV_COCU),
        PV_COLE   = 8194,   // = opt_win(WV_COLE),
        PV_CRBIND = 8195,   // = opt_win(WV_CRBIND),
        PV_BRI    = 8196,   // = opt_win(WV_BRI),
        PV_BRIOPT = 8197,   // = opt_win(WV_BRIOPT),
        PV_LBR    = 8198,   // = opt_win(WV_LBR),
        PV_NU     = 8199,   // = opt_win(WV_NU),
        PV_RNU    = 8200,   // = opt_win(WV_RNU),
        PV_NUW    = 8201,   // = opt_win(WV_NUW),
        PV_RL     = 8202,   // = opt_win(WV_RL),
        PV_RLC    = 8203,   // = opt_win(WV_RLC),
        PV_SCBIND = 8204,   // = opt_win(WV_SCBIND),
        PV_SCROLL = 8205,   // = opt_win(WV_SCROLL),
        PV_CUC    = 8206,   // = opt_win(WV_CUC),
        PV_CUL    = 8207,   // = opt_win(WV_CUL),
        PV_CC     = 8208,   // = opt_win(WV_CC),
        PV_STL    = 12305,  // = opt_both(opt_win(WV_STL)),
        PV_WFH    = 8210,   // = opt_win(WV_WFH),
        PV_WFW    = 8211,   // = opt_win(WV_WFW),
        PV_WRAP   = 8212;   // = opt_win(WV_WRAP);

    /*
     * Options local to a window have a value local to a buffer and global to all buffers.
     * Indicate this by setting "var" to VAR_WIN.
     */
    /*private*/ static final Object VAR_WIN = new Object();

    /*
     * These are the global values for options which are also local to a buffer.
     * Only to be used in option.c!
     */
    /*private*/ static boolean[] p_ai       = new boolean[1];
    /*private*/ static boolean[] p_bin      = new boolean[1];
    /*private*/ static boolean[] p_bomb     = new boolean[1];
    /*private*/ static boolean[] p_bl       = new boolean[1];
    /*private*/ static boolean[] p_ci       = new boolean[1];
    /*private*/ static boolean[] p_cin      = new boolean[1];
    /*private*/ static Bytes[]   p_cink     = new Bytes[1];
    /*private*/ static Bytes[]   p_cino     = new Bytes[1];
    /*private*/ static Bytes[]   p_cinw     = new Bytes[1];
    /*private*/ static Bytes[]   p_com      = new Bytes[1];
    /*private*/ static boolean[] p_eol      = new boolean[1];
    /*private*/ static boolean[] p_et       = new boolean[1];
    /*private*/ static Bytes[]   p_fenc     = new Bytes[1];
    /*private*/ static Bytes[]   p_ff       = new Bytes[1];
    /*private*/ static Bytes[]   p_fo       = new Bytes[1];
    /*private*/ static Bytes[]   p_flp      = new Bytes[1];
    /*private*/ static Bytes[]   p_ft       = new Bytes[1];
    /*private*/ static long[]    p_iminsert = new long[1];
    /*private*/ static long[]    p_imsearch = new long[1];
    /*private*/ static Bytes[]   p_inde     = new Bytes[1];
    /*private*/ static Bytes[]   p_indk     = new Bytes[1];
    /*private*/ static Bytes[]   p_fex      = new Bytes[1];
    /*private*/ static boolean[] p_inf      = new boolean[1];
    /*private*/ static Bytes[]   p_isk      = new Bytes[1];
    /*private*/ static boolean[] p_lisp     = new boolean[1];
    /*private*/ static boolean[] p_ma       = new boolean[1];
    /*private*/ static boolean[] p_mod      = new boolean[1];
    /*private*/ static Bytes[]   p_mps      = new Bytes[1];
    /*private*/ static Bytes[]   p_nf       = new Bytes[1];
    /*private*/ static boolean[] p_pi       = new boolean[1];
    /*private*/ static Bytes[]   p_qe       = new Bytes[1];
    /*private*/ static boolean[] p_ro       = new boolean[1];
    /*private*/ static boolean[] p_si       = new boolean[1];
    /*private*/ static long[]    p_sts      = new long[1];
    /*private*/ static long[]    p_sw       = new long[1];
    /*private*/ static long[]    p_smc      = new long[1];
    /*private*/ static Bytes[]   p_syn      = new Bytes[1];
    /*private*/ static long[]    p_ts       = new long[1];
    /*private*/ static long[]    p_tw       = new long[1];
    /*private*/ static boolean[] p_tx       = new boolean[1];
    /*private*/ static boolean[] p_udf      = new boolean[1];
    /*private*/ static long[]    p_wm       = new long[1];

    /* Saved values for when 'bin' is set. */
    /*private*/ static boolean  p_et_nobin;
    /*private*/ static long     p_tw_nobin;
    /*private*/ static long     p_wm_nobin;

    /* Saved values for when 'paste' is set. */
    /*private*/ static long     p_tw_nopaste;
    /*private*/ static long     p_wm_nopaste;
    /*private*/ static long     p_sts_nopaste;
    /*private*/ static boolean  p_ai_nopaste;

    /*private*/ static final class vimoption_C
    {
        Bytes       fullname;       /* full option name */
        Bytes       shortname;      /* permissible abbreviation */
        long[]      flags = new long[1];          /* see below */
        Object      var;            /* global option: pointer to variable;
                                     * window-local option: VAR_WIN;
                                     * buffer-local option: global value */
        int         indir;          /* global option: PV_NONE;
                                     * local option: indirect option index */
        Object      def_val;        /* default value for variable */
        int         scriptID;       /* script in which the option was last set */

        /*private*/ vimoption_C()
        {
        }
    }

    /*private*/ static vimoption_C new_vimoption(Bytes fullname, Bytes shortname, long flags, Object var, int indir, Object def_val)
    {
        vimoption_C v = new vimoption_C();

        v.fullname = fullname;
        v.shortname = shortname;
        v.flags[0] = flags;
        v.var = var;
        v.indir = indir;
        v.def_val = def_val;

        return v;
    }

    /*
     * Flags
     */
    /*private*/ static final int P_BOOL               = 0x01;   /* the option is boolean */
    /*private*/ static final int P_NUM                = 0x02;   /* the option is numeric */
    /*private*/ static final int P_STRING             = 0x04;   /* the option is a string */

    /*private*/ static final int P_NODEFAULT          = 0x40;   /* don't set to default value */
    /*private*/ static final int P_WAS_SET           = 0x100;   /* option has been set/reset */

                                        /* when option changed, what to display: */
    /*private*/ static final int P_RSTAT            = 0x1000;   /* redraw status lines */
    /*private*/ static final int P_RWIN             = 0x2000;   /* redraw current window */
    /*private*/ static final int P_RBUF             = 0x4000;   /* redraw current buffer */
    /*private*/ static final int P_RALL             = 0x6000;   /* redraw all windows */
    /*private*/ static final int P_RCLR             = 0x7000;   /* clear and redraw all */

    /*private*/ static final int P_COMMA            = 0x8000;   /* comma separated list */
    /*private*/ static final int P_NODUP           = 0x10000;   /* don't allow duplicate strings */
    /*private*/ static final int P_FLAGLIST        = 0x20000;   /* list of single-char flags */

    /*private*/ static final int P_SECURE          = 0x40000;   /* cannot change in modeline or secure mode */
    /*private*/ static final int P_GETTEXT         = 0x80000;   /* expand default value with _() */
    /*private*/ static final int P_NOGLOB         = 0x100000;   /* do not use local value for global vimrc */
    /*private*/ static final int P_NFNAME         = 0x200000;   /* only normal file name chars allowed */
    /*private*/ static final int P_INSECURE       = 0x400000;   /* option was set from a modeline */
    /*private*/ static final int P_NO_ML         = 0x1000000;   /* not allowed in modeline */
    /*private*/ static final int P_CURSWANT      = 0x2000000;   /* update curswant required; not needed when there is a redraw flag */

    /*private*/ static final Bytes COMMENTS_INIT = u8("s1:/*,mb:*,ex:*/,://,b:#,:%,:XCOMM,n:>,fb:-");

    /*private*/ static final Bytes HIGHLIGHT_INIT = u8("8:SpecialKey,@:NonText,d:Directory,e:ErrorMsg,i:IncSearch,l:Search,m:MoreMsg,M:ModeMsg,n:LineNr,N:CursorLineNr,r:Question,s:StatusLine,S:StatusLineNC,c:VertSplit,t:Title,v:Visual,V:VisualNOS,w:WarningMsg,W:WildMenu,f:Folded,F:FoldColumn,A:DiffAdd,C:DiffChange,D:DiffDelete,T:DiffText,>:SignColumn,-:Conceal,B:SpellBad,P:SpellCap,R:SpellRare,L:SpellLocal,+:Pmenu,=:PmenuSel,x:PmenuSbar,X:PmenuThumb,*:TabLine,#:TabLineSel,_:TabLineFill,!:CursorColumn,.:CursorLine,o:ColorColumn");

    /*private*/ static vimoption_C bool_opt(Bytes fname, Bytes sname, long flags, /*boolean[]*/Object var, int indir, boolean def)
    {
        return new_vimoption(fname, sname, P_BOOL | flags, var, indir, def);
    }

    /*private*/ static vimoption_C long_opt(Bytes fname, Bytes sname, long flags, /*long[]*/Object var, int indir, long def)
    {
        return new_vimoption(fname, sname, P_NUM | flags, var, indir, def);
    }

    /*private*/ static vimoption_C utf8_opt(Bytes fname, Bytes sname, long flags, /*Bytes[]*/Object var, int indir, Bytes def)
    {
        return new_vimoption(fname, sname, P_STRING | flags, var, indir, def);
    }

    /*private*/ static vimoption_C term_opt(Bytes fname, Bytes[] var)
    {
        return utf8_opt(fname, null, P_RALL|P_SECURE, var, PV_NONE, u8(""));
    }

    /*
     * vimoptions[] are initialized here.
     *
     * The order of the options MUST be alphabetic for ":set all" and findoption().
     * All option names MUST start with a lowercase letter (for findoption()).
     * Exception: "t_" options are at the end.
     * The options with a null variable are 'hidden': a set command for them is
     * ignored and they are not printed.
     */
    /*private*/ static vimoption_C[] vimoptions = new vimoption_C[]
    {
        long_opt
        (
            u8("aleph"), u8("al"), P_CURSWANT, p_aleph, PV_NONE, 224L
        ),
        bool_opt
        (
            u8("allowrevins"), u8("ari"), 0, p_ari, PV_NONE, false
        ),
        utf8_opt
        (
            u8("ambiwidth"), u8("ambw"), P_RCLR, p_ambw, PV_NONE, u8("single")
        ),
        bool_opt
        (
            u8("autoindent"), u8("ai"), 0, p_ai, PV_AI, false
        ),
        bool_opt
        (
            u8("autoread"), u8("ar"), 0, p_ar, PV_AR, false
        ),
        bool_opt
        (
            u8("autowrite"), u8("aw"), 0, p_aw, PV_NONE, false
        ),
        bool_opt
        (
            u8("autowriteall"), u8("awa"), 0, p_awa, PV_NONE, false
        ),
        utf8_opt
        (
            u8("background"), u8("bg"), P_RCLR, p_bg, PV_NONE, u8("light")
        ),
        utf8_opt
        (
            u8("backspace"), u8("bs"), P_COMMA|P_NODUP, p_bs, PV_NONE, u8("")
        ),
        bool_opt
        (
            u8("binary"), u8("bin"), P_RSTAT, p_bin, PV_BIN, false
        ),
        bool_opt
        (
            u8("bomb"), null, P_RSTAT, p_bomb, PV_BOMB, false
        ),
        utf8_opt
        (
            u8("breakat"), u8("brk"), P_RALL|P_FLAGLIST, p_breakat, PV_NONE, u8(" \t!@*-+;:,./?")
        ),
        bool_opt
        (
            u8("breakindent"), u8("bri"), P_RWIN, VAR_WIN, PV_BRI, false
        ),
        utf8_opt
        (
            u8("breakindentopt"), u8("briopt"), P_RBUF|P_COMMA|P_NODUP, VAR_WIN, PV_BRIOPT, u8("")
        ),
        bool_opt
        (
            u8("buflisted"), u8("bl"), P_NOGLOB, p_bl, PV_BL, true
        ),
        utf8_opt
        (
            u8("cedit"), null, 0, p_cedit, PV_NONE, CTRL_F_STR
        ),
        bool_opt
        (
            u8("cindent"), u8("cin"), 0, p_cin, PV_CIN, false
        ),
        utf8_opt
        (
            u8("cinkeys"), u8("cink"), P_COMMA|P_NODUP, p_cink, PV_CINK, u8("0{,0},0),:,0#,!^F,o,O,e")
        ),
        utf8_opt
        (
            u8("cinoptions"), u8("cino"), P_COMMA|P_NODUP, p_cino, PV_CINO, u8("")
        ),
        utf8_opt
        (
            u8("cinwords"), u8("cinw"), P_COMMA|P_NODUP, p_cinw, PV_CINW, u8("if,else,while,do,for,switch")
        ),
        utf8_opt
        (
            u8("clipboard"), u8("cb"), P_COMMA|P_NODUP, p_cb, PV_NONE, u8("")
        ),
        long_opt
        (
            u8("cmdheight"), u8("ch"), P_RALL, p_ch, PV_NONE, 1L
        ),
        long_opt
        (
            u8("cmdwinheight"), u8("cwh"), 0, p_cwh, PV_NONE, 7L
        ),
        utf8_opt
        (
            u8("colorcolumn"), u8("cc"), P_COMMA|P_NODUP|P_RWIN, VAR_WIN, PV_CC, u8("")
        ),
        long_opt
        (
            u8("columns"), u8("co"), P_NODEFAULT|P_RCLR, Columns, PV_NONE, 80L
        ),
        utf8_opt
        (
            u8("comments"), u8("com"), P_COMMA|P_NODUP|P_CURSWANT, p_com, PV_COM, COMMENTS_INIT
        ),
        utf8_opt
        (
            u8("concealcursor"), u8("cocu"), P_RWIN, VAR_WIN, PV_COCU, u8("")
        ),
        long_opt
        (
            u8("conceallevel"), u8("cole"), P_RWIN, VAR_WIN, PV_COLE, 0L
        ),
        bool_opt
        (
            u8("confirm"), u8("cf"), 0, p_confirm, PV_NONE, false
        ),
        bool_opt
        (
            u8("copyindent"), u8("ci"), 0, p_ci, PV_CI, false
        ),
        utf8_opt
        (
            u8("cpoptions"), u8("cpo"), P_RALL|P_FLAGLIST, p_cpo, PV_NONE, CPO_VIM
        ),
        bool_opt
        (
            u8("cursorbind"), u8("crb"), 0, VAR_WIN, PV_CRBIND, false
        ),
        bool_opt
        (
            u8("cursorcolumn"), u8("cuc"), P_RWIN, VAR_WIN, PV_CUC, false
        ),
        bool_opt
        (
            u8("cursorline"), u8("cul"), P_RWIN, VAR_WIN, PV_CUL, false
        ),
        utf8_opt
        (
            u8("debug"), null, 0, p_debug, PV_NONE, u8("")
        ),
        bool_opt
        (
            u8("delcombine"), u8("deco"), 0, p_deco, PV_NONE, false
        ),
        bool_opt
        (
            u8("digraph"), u8("dg"), 0, p_dg, PV_NONE, false
        ),
        utf8_opt
        (
            u8("display"), u8("dy"), P_COMMA|P_RALL|P_NODUP, p_dy, PV_NONE, u8("")
        ),
        utf8_opt
        (
            u8("eadirection"), u8("ead"), 0, p_ead, PV_NONE, u8("both")
        ),
        bool_opt
        (
            u8("endofline"), u8("eol"), P_RSTAT, p_eol, PV_EOL, true
        ),
        bool_opt
        (
            u8("equalalways"), u8("ea"), P_RALL, p_ea, PV_NONE, true
        ),
        utf8_opt
        (
            u8("equalprg"), u8("ep"), P_SECURE, p_ep, PV_EP, u8("")
        ),
        bool_opt
        (
            u8("errorbells"), u8("eb"), 0, p_eb, PV_NONE, false
        ),
        bool_opt
        (
            u8("esckeys"), u8("ek"), 0, p_ek, PV_NONE, true
        ),
        utf8_opt
        (
            u8("eventignore"), u8("ei"), P_COMMA|P_NODUP, p_ei, PV_NONE, u8("")
        ),
        bool_opt
        (
            u8("expandtab"), u8("et"), 0, p_et, PV_ET, false
        ),
        bool_opt
        (
            u8("exrc"), u8("ex"), P_SECURE, p_exrc, PV_NONE, false
        ),
        utf8_opt
        (
            u8("fileencoding"), u8("fenc"), P_RSTAT|P_RBUF, p_fenc, PV_FENC, u8("")
        ),
        utf8_opt
        (
            u8("fileencodings"), u8("fencs"), P_COMMA, p_fencs, PV_NONE, u8("ucs-bom,utf-8")
        ),
        utf8_opt
        (
            u8("fileformat"), u8("ff"), P_RSTAT|P_CURSWANT, p_ff, PV_FF, u8("unix")
        ),
        utf8_opt
        (
            u8("fileformats"), u8("ffs"), P_COMMA|P_NODUP, p_ffs, PV_NONE, u8("unix,dos")
        ),
        utf8_opt
        (
            u8("filetype"), u8("ft"), P_NOGLOB|P_NFNAME, p_ft, PV_FT, u8("")
        ),
        utf8_opt
        (
            u8("fillchars"), u8("fcs"), P_RALL|P_COMMA|P_NODUP, p_fcs, PV_NONE, u8("vert:|,fold:-")
        ),
        utf8_opt
        (
            u8("formatexpr"), u8("fex"), 0, p_fex, PV_FEX, u8("")
        ),
        utf8_opt
        (
            u8("formatoptions"), u8("fo"), P_FLAGLIST, p_fo, PV_FO, DFLT_FO_VIM
        ),
        utf8_opt
        (
            u8("formatlistpat"), u8("flp"), 0, p_flp, PV_FLP, u8("^\\s*\\d\\+[\\]:.)}\\t ]\\s*")
        ),
        utf8_opt
        (
            u8("formatprg"), u8("fp"), P_SECURE, p_fp, PV_NONE, u8("")
        ),
        bool_opt
        (
            u8("fsync"), u8("fs"), P_SECURE, p_fs, PV_NONE, true
        ),
        bool_opt
        (
            u8("gdefault"), u8("gd"), 0, p_gd, PV_NONE, false
        ),
        bool_opt
        (
            u8("hidden"), u8("hid"), 0, p_hid, PV_NONE, false
        ),
        utf8_opt
        (
            u8("highlight"), u8("hl"), P_RCLR|P_COMMA|P_NODUP, p_hl, PV_NONE, HIGHLIGHT_INIT
        ),
        long_opt
        (
            u8("history"), u8("hi"), 0, p_hi, PV_NONE, 50L
        ),
        bool_opt
        (
            u8("hlsearch"), u8("hls"), P_RALL, p_hls, PV_NONE, false
        ),
        bool_opt
        (
            u8("ignorecase"), u8("ic"), 0, p_ic, PV_NONE, false
        ),
        long_opt
        (
            u8("iminsert"), u8("imi"), 0, p_iminsert, PV_IMI, B_IMODE_NONE
        ),
        long_opt
        (
            u8("imsearch"), u8("ims"), 0, p_imsearch, PV_IMS, B_IMODE_NONE
        ),
        bool_opt
        (
            u8("incsearch"), u8("is"), 0, p_is, PV_NONE, false
        ),
        utf8_opt
        (
            u8("indentexpr"), u8("inde"), 0, p_inde, PV_INDE, u8("")
        ),
        utf8_opt
        (
            u8("indentkeys"), u8("indk"), P_COMMA|P_NODUP, p_indk, PV_INDK, u8("0{,0},:,0#,!^F,o,O,e")
        ),
        bool_opt
        (
            u8("infercase"), u8("inf"), 0, p_inf, PV_INF, false
        ),
        bool_opt
        (
            u8("insertmode"), u8("im"), 0, p_im, PV_NONE, false
        ),
        utf8_opt
        (
            u8("isfname"), u8("isf"), P_COMMA|P_NODUP, p_isf, PV_NONE, u8("@,48-57,/,.,-,_,+,,,#,$,%,~,=")
        ),
        utf8_opt
        (
            u8("isident"), u8("isi"), P_COMMA|P_NODUP, p_isi, PV_NONE, u8("@,48-57,_,192-255")
        ),
        utf8_opt
        (
            u8("iskeyword"), u8("isk"), P_COMMA|P_NODUP, p_isk, PV_ISK, u8("@,48-57,_,192-255")
        ),
        utf8_opt
        (
            u8("isprint"), u8("isp"), P_RALL|P_COMMA|P_NODUP, p_isp, PV_NONE, u8("@,161-255")
        ),
        bool_opt
        (
            u8("joinspaces"), u8("js"), 0, p_js, PV_NONE, true
        ),
        utf8_opt
        (
            u8("keymodel"), u8("km"), P_COMMA|P_NODUP, p_km, PV_NONE, u8("")
        ),
        utf8_opt
        (
            u8("keywordprg"), u8("kp"), P_SECURE, p_kp, PV_KP, u8(":echo")
        ),
        long_opt
        (
            u8("laststatus"), u8("ls"), P_RALL, p_ls, PV_NONE, 1L
        ),
        bool_opt
        (
            u8("lazyredraw"), u8("lz"), 0, p_lz, PV_NONE, false
        ),
        bool_opt
        (
            u8("linebreak"), u8("lbr"), P_RWIN, VAR_WIN, PV_LBR, false
        ),
        long_opt
        (
            u8("lines"), null, P_NODEFAULT|P_RCLR, Rows, PV_NONE, 24L
        ),
        bool_opt
        (
            u8("lisp"), null, 0, p_lisp, PV_LISP, false
        ),
        utf8_opt
        (
            u8("lispwords"), u8("lw"), P_COMMA|P_NODUP, p_lispwords, PV_LW, LISPWORD_VALUE
        ),
        bool_opt
        (
            u8("list"), null, P_RWIN, VAR_WIN, PV_LIST, false
        ),
        utf8_opt
        (
            u8("listchars"), u8("lcs"), P_RALL|P_COMMA|P_NODUP, p_lcs, PV_NONE, u8("eol:$")
        ),
        bool_opt
        (
            u8("magic"), null, 0, p_magic, PV_NONE, true
        ),
        utf8_opt
        (
            u8("matchpairs"), u8("mps"), P_COMMA|P_NODUP, p_mps, PV_MPS, u8("(:),{:},[:]")
        ),
        long_opt
        (
            u8("matchtime"), u8("mat"), 0, p_mat, PV_NONE, 5L
        ),
        long_opt
        (
            u8("maxcombine"), u8("mco"), P_CURSWANT, p_mco, PV_NONE, 2L
        ),
        long_opt
        (
            u8("maxfuncdepth"), u8("mfd"), 0, p_mfd, PV_NONE, 100L
        ),
        long_opt
        (
            u8("maxmapdepth"), u8("mmd"), 0, p_mmd, PV_NONE, 1000L
        ),
        long_opt
        (
            u8("maxmempattern"), u8("mmp"), 0, p_mmp, PV_NONE, 1000L
        ),
        bool_opt
        (
            u8("modifiable"), u8("ma"), P_NOGLOB, p_ma, PV_MA, true
        ),
        bool_opt
        (
            u8("modified"), u8("mod"), P_RSTAT, p_mod, PV_MOD, false
        ),
        bool_opt
        (
            u8("more"), null, 0, p_more, PV_NONE, true
        ),
        utf8_opt
        (
            u8("mouse"), null, P_FLAGLIST, p_mouse, PV_NONE, u8("")
        ),
        utf8_opt
        (
            u8("mousemodel"), u8("mousem"), 0, p_mousem, PV_NONE, u8("extend")
        ),
        long_opt
        (
            u8("mousetime"), u8("mouset"), 0, p_mouset, PV_NONE, 500L
        ),
        utf8_opt
        (
            u8("nrformats"), u8("nf"), P_COMMA|P_NODUP, p_nf, PV_NF, u8("octal,hex")
        ),
        bool_opt
        (
            u8("number"), u8("nu"), P_RWIN, VAR_WIN, PV_NU, false
        ),
        long_opt
        (
            u8("numberwidth"), u8("nuw"), P_RWIN, VAR_WIN, PV_NUW, 4L
        ),
        utf8_opt
        (
            u8("operatorfunc"), u8("opfunc"), P_SECURE, p_opfunc, PV_NONE, u8("")
        ),
        utf8_opt
        (
            u8("paragraphs"), u8("para"), 0, p_para, PV_NONE, u8("IPLPPPQPP TPHPLIPpLpItpplpipbp")
        ),
        bool_opt
        (
            u8("paste"), null, 0, p_paste, PV_NONE, false
        ),
        utf8_opt
        (
            u8("pastetoggle"), u8("pt"), 0, p_pt, PV_NONE, u8("")
        ),
        bool_opt
        (
            u8("preserveindent"), u8("pi"), 0, p_pi, PV_PI, false
        ),
        bool_opt
        (
            u8("prompt"), null, 0, p_prompt, PV_NONE, true
        ),
        utf8_opt
        (
            u8("quoteescape"), u8("qe"), 0, p_qe, PV_QE, u8("\\")
        ),
        bool_opt
        (
            u8("readonly"), u8("ro"), P_RSTAT|P_NOGLOB, p_ro, PV_RO, false
        ),
        long_opt
        (
            u8("redrawtime"), u8("rdt"), 0, p_rdt, PV_NONE, 2000L
        ),
        long_opt
        (
            u8("regexpengine"), u8("re"), 0, p_re, PV_NONE, 0L
        ),
        bool_opt
        (
            u8("relativenumber"), u8("rnu"), P_RWIN, VAR_WIN, PV_RNU, false
        ),
        bool_opt
        (
            u8("remap"), null, 0, p_remap, PV_NONE, true
        ),
        long_opt
        (
            u8("report"), null, 0, p_report, PV_NONE, 2L
        ),
        bool_opt
        (
            u8("revins"), u8("ri"), 0, p_ri, PV_NONE, false
        ),
        bool_opt
        (
            u8("rightleft"), u8("rl"), P_RWIN, VAR_WIN, PV_RL, false
        ),
        utf8_opt
        (
            u8("rightleftcmd"), u8("rlc"), P_RWIN, VAR_WIN, PV_RLC, u8("search")
        ),
        bool_opt
        (
            u8("ruler"), u8("ru"), P_RSTAT, p_ru, PV_NONE, false
        ),
        utf8_opt
        (
            u8("rulerformat"), u8("ruf"), P_RSTAT, p_ruf, PV_NONE, u8("")
        ),
        utf8_opt
        (
            u8("runtimepath"), u8("rtp"), P_COMMA|P_NODUP|P_SECURE, p_rtp, PV_NONE, VIMRUNTIME
        ),
        long_opt
        (
            u8("scroll"), u8("scr"), 0, VAR_WIN, PV_SCROLL, 12L
        ),
        bool_opt
        (
            u8("scrollbind"), u8("scb"), 0, VAR_WIN, PV_SCBIND, false
        ),
        long_opt
        (
            u8("scrolljump"), u8("sj"), 0, p_sj, PV_NONE, 1L
        ),
        long_opt
        (
            u8("scrolloff"), u8("so"), P_RALL, p_so, PV_NONE, 0L
        ),
        utf8_opt
        (
            u8("scrollopt"), u8("sbo"), P_COMMA|P_NODUP, p_sbo, PV_NONE, u8("ver,jump")
        ),
        utf8_opt
        (
            u8("sections"), u8("sect"), 0, p_sections, PV_NONE, u8("SHNHH HUnhsh")
        ),
        bool_opt
        (
            u8("secure"), null, P_SECURE, p_secure, PV_NONE, false
        ),
        utf8_opt
        (
            u8("selection"), u8("sel"), 0, p_sel, PV_NONE, u8("inclusive")
        ),
        utf8_opt
        (
            u8("selectmode"), u8("slm"), P_COMMA|P_NODUP, p_slm, PV_NONE, u8("")
        ),
        bool_opt
        (
            u8("shiftround"), u8("sr"), 0, p_sr, PV_NONE, false
        ),
        long_opt
        (
            u8("shiftwidth"), u8("sw"), 0, p_sw, PV_SW, 8L
        ),
        utf8_opt
        (
            u8("shortmess"), u8("shm"), P_FLAGLIST, p_shm, PV_NONE, u8("filnxtToO")
        ),
        utf8_opt
        (
            u8("showbreak"), u8("sbr"), P_RALL, p_sbr, PV_NONE, u8("")
        ),
        bool_opt
        (
            u8("showcmd"), u8("sc"), 0, p_sc, PV_NONE, false
        ),
        bool_opt
        (
            u8("showmatch"), u8("sm"), 0, p_sm, PV_NONE, false
        ),
        bool_opt
        (
            u8("showmode"), u8("smd"), 0, p_smd, PV_NONE, true
        ),
        long_opt
        (
            u8("showtabline"), u8("stal"), P_RALL, p_stal, PV_NONE, 1L
        ),
        long_opt
        (
            u8("sidescroll"), u8("ss"), 0, p_ss, PV_NONE, 0L
        ),
        long_opt
        (
            u8("sidescrolloff"), u8("siso"), P_RBUF, p_siso, PV_NONE, 0L
        ),
        bool_opt
        (
            u8("smartcase"), u8("scs"), 0, p_scs, PV_NONE, false
        ),
        bool_opt
        (
            u8("smartindent"), u8("si"), 0, p_si, PV_SI, false
        ),
        bool_opt
        (
            u8("smarttab"), u8("sta"), 0, p_sta, PV_NONE, false
        ),
        long_opt
        (
            u8("softtabstop"), u8("sts"), 0, p_sts, PV_STS, 0L
        ),
        bool_opt
        (
            u8("splitbelow"), u8("sb"), 0, p_sb, PV_NONE, false
        ),
        bool_opt
        (
            u8("splitright"), u8("spr"), 0, p_spr, PV_NONE, false
        ),
        bool_opt
        (
            u8("startofline"), u8("sol"), 0, p_sol, PV_NONE, true
        ),
        utf8_opt
        (
            u8("statusline"), u8("stl"), P_RSTAT, p_stl, PV_STL, u8("")
        ),
        utf8_opt
        (
            u8("switchbuf"), u8("swb"), P_COMMA|P_NODUP, p_swb, PV_NONE, u8("")
        ),
        long_opt
        (
            u8("synmaxcol"), u8("smc"), P_RBUF, p_smc, PV_SMC, 3000L
        ),
        utf8_opt
        (
            u8("syntax"), u8("syn"), P_NOGLOB|P_NFNAME, p_syn, PV_SYN, u8("")
        ),
        utf8_opt
        (
            u8("tabline"), u8("tal"), P_RALL, p_tal, PV_NONE, u8("")
        ),
        long_opt
        (
            u8("tabpagemax"), u8("tpm"), 0, p_tpm, PV_NONE, 10L
        ),
        long_opt
        (
            u8("tabstop"), u8("ts"), P_RBUF, p_ts, PV_TS, 8L
        ),
        utf8_opt
        (
            u8("term"), null, P_NODEFAULT|P_RALL, T_NAME, PV_NONE, u8("")
        ),
        bool_opt
        (
            u8("terse"), null, 0, p_terse, PV_NONE, false
        ),
        bool_opt
        (
            u8("textmode"), u8("tx"), 0, p_tx, PV_TX, false
        ),
        long_opt
        (
            u8("textwidth"), u8("tw"), P_RBUF, p_tw, PV_TW, 0L
        ),
        bool_opt
        (
            u8("tildeop"), u8("top"), 0, p_to, PV_NONE, false
        ),
        bool_opt
        (
            u8("timeout"), u8("to"), 0, p_timeout, PV_NONE, true
        ),
        long_opt
        (
            u8("timeoutlen"), u8("tm"), 0, p_tm, PV_NONE, 1000L
        ),
        bool_opt
        (
            u8("ttimeout"), null, 0, p_ttimeout, PV_NONE, false
        ),
        long_opt
        (
            u8("ttimeoutlen"), u8("ttm"), 0, p_ttm, PV_NONE, -1L
        ),
        bool_opt
        (
            u8("ttyfast"), u8("tf"), 0, p_tf, PV_NONE, false
        ),
        utf8_opt
        (
            u8("ttymouse"), u8("ttym"), P_NODEFAULT, p_ttym, PV_NONE, u8("")
        ),
        long_opt
        (
            u8("ttyscroll"), u8("tsl"), 0, p_ttyscroll, PV_NONE, 999L
        ),
        utf8_opt
        (
            u8("ttytype"), u8("tty"), P_NODEFAULT|P_RALL, T_NAME, PV_NONE, u8("")
        ),
        utf8_opt
        (
            u8("undodir"), u8("udir"), P_COMMA|P_NODUP|P_SECURE, p_udir, PV_NONE, u8(".")
        ),
        bool_opt
        (
            u8("undofile"), u8("udf"), 0, p_udf, PV_UDF, false
        ),
        long_opt
        (
            u8("undolevels"), u8("ul"), 0, p_ul, PV_UL, 1000L
        ),
        long_opt
        (
            u8("undoreload"), u8("ur"), 0, p_ur, PV_NONE, 10000L
        ),
        long_opt
        (
            u8("updatetime"), u8("ut"), 0, p_ut, PV_NONE, 4000L
        ),
        long_opt
        (
            u8("verbose"), u8("vbs"), 0, p_verbose, PV_NONE, 0L
        ),
        utf8_opt
        (
            u8("verbosefile"), u8("vfile"), P_SECURE, p_vfile, PV_NONE, u8("")
        ),
        utf8_opt
        (
            u8("virtualedit"), u8("ve"), P_COMMA|P_NODUP|P_CURSWANT, p_ve, PV_NONE, u8("")
        ),
        bool_opt
        (
            u8("visualbell"), u8("vb"), 0, p_vb, PV_NONE, false
        ),
        bool_opt
        (
            u8("warn"), null, 0, p_warn, PV_NONE, true
        ),
        bool_opt
        (
            u8("weirdinvert"), u8("wiv"), P_RCLR, p_wiv, PV_NONE, false
        ),
        utf8_opt
        (
            u8("whichwrap"), u8("ww"), P_COMMA|P_FLAGLIST, p_ww, PV_NONE, u8("b,s")
        ),
        long_opt
        (
            u8("wildchar"), u8("wc"), 0, p_wc, PV_NONE, (long)TAB
        ),
        long_opt
        (
            u8("wildcharm"), u8("wcm"), 0, p_wcm, PV_NONE, 0L
        ),
        bool_opt
        (
            u8("wildignorecase"), u8("wic"), 0, p_wic, PV_NONE, false
        ),
        utf8_opt
        (
            u8("wildmode"), u8("wim"), P_COMMA|P_NODUP, p_wim, PV_NONE, u8("full")
        ),
        long_opt
        (
            u8("window"), u8("wi"), 0, p_window, PV_NONE, 0L
        ),
        long_opt
        (
            u8("winheight"), u8("wh"), 0, p_wh, PV_NONE, 1L
        ),
        bool_opt
        (
            u8("winfixheight"), u8("wfh"), P_RSTAT, VAR_WIN, PV_WFH, false
        ),
        bool_opt
        (
            u8("winfixwidth"), u8("wfw"), P_RSTAT, VAR_WIN, PV_WFW, false
        ),
        long_opt
        (
            u8("winminheight"), u8("wmh"), 0, p_wmh, PV_NONE, 1L
        ),
        long_opt
        (
            u8("winminwidth"), u8("wmw"), 0, p_wmw, PV_NONE, 1L
        ),
        long_opt
        (
            u8("winwidth"), u8("wiw"), 0, p_wiw, PV_NONE, 20L
        ),
        bool_opt
        (
            u8("wrap"), null, P_RWIN, VAR_WIN, PV_WRAP, true
        ),
        long_opt
        (
            u8("wrapmargin"), u8("wm"), 0, p_wm, PV_WM, 0L
        ),
        bool_opt
        (
            u8("wrapscan"), u8("ws"), 0, p_ws, PV_NONE, true
        ),
        bool_opt
        (
            u8("write"), null, 0, p_write, PV_NONE, true
        ),
        bool_opt
        (
            u8("writeany"), u8("wa"), 0, p_wa, PV_NONE, false
        ),
        long_opt
        (
            u8("writedelay"), u8("wd"), 0, p_wd, PV_NONE, 0L
        ),

        /* terminal output codes */

        term_opt(u8("t_AB"), T_CAB),
        term_opt(u8("t_AF"), T_CAF),
        term_opt(u8("t_AL"), T_CAL),
        term_opt(u8("t_al"), T_AL),
        term_opt(u8("t_bc"), T_BC),
        term_opt(u8("t_cd"), T_CD),
        term_opt(u8("t_ce"), T_CE),
        term_opt(u8("t_cl"), T_CL),
        term_opt(u8("t_cm"), T_CM),
        term_opt(u8("t_Co"), T_CCO),
        term_opt(u8("t_CS"), T_CCS),
        term_opt(u8("t_cs"), T_CS),
        term_opt(u8("t_CV"), T_CSV),
        term_opt(u8("t_ut"), T_UT),
        term_opt(u8("t_da"), T_DA),
        term_opt(u8("t_db"), T_DB),
        term_opt(u8("t_DL"), T_CDL),
        term_opt(u8("t_dl"), T_DL),
        term_opt(u8("t_fs"), T_FS),
        term_opt(u8("t_IE"), T_CIE),
        term_opt(u8("t_IS"), T_CIS),
        term_opt(u8("t_ke"), T_KE),
        term_opt(u8("t_ks"), T_KS),
        term_opt(u8("t_le"), T_LE),
        term_opt(u8("t_mb"), T_MB),
        term_opt(u8("t_md"), T_MD),
        term_opt(u8("t_me"), T_ME),
        term_opt(u8("t_mr"), T_MR),
        term_opt(u8("t_ms"), T_MS),
        term_opt(u8("t_nd"), T_ND),
        term_opt(u8("t_op"), T_OP),
        term_opt(u8("t_RI"), T_CRI),
        term_opt(u8("t_RV"), T_CRV),
        term_opt(u8("t_u7"), T_U7),
        term_opt(u8("t_Sb"), T_CSB),
        term_opt(u8("t_Sf"), T_CSF),
        term_opt(u8("t_se"), T_SE),
        term_opt(u8("t_so"), T_SO),
        term_opt(u8("t_sr"), T_SR),
        term_opt(u8("t_ts"), T_TS),
        term_opt(u8("t_te"), T_TE),
        term_opt(u8("t_ti"), T_TI),
        term_opt(u8("t_ue"), T_UE),
        term_opt(u8("t_us"), T_US),
        term_opt(u8("t_vb"), T_VB),
        term_opt(u8("t_ve"), T_VE),
        term_opt(u8("t_vi"), T_VI),
        term_opt(u8("t_vs"), T_VS),
        term_opt(u8("t_WP"), T_CWP),
        term_opt(u8("t_WS"), T_CWS),
        term_opt(u8("t_SI"), T_CSI),
        term_opt(u8("t_EI"), T_CEI),
        term_opt(u8("t_SR"), T_CSR),
        term_opt(u8("t_xn"), T_XN),
        term_opt(u8("t_xs"), T_XS),
        term_opt(u8("t_ZH"), T_CZH),
        term_opt(u8("t_ZR"), T_CZR),

        /* terminal key codes are not in here */

        new_vimoption(null, null, 0, null, PV_NONE, null)
    };

    /*private*/ static Bytes[]
        p_ambw_values = { u8("single"), u8("double"), null },
        p_bg_values = { u8("light"), u8("dark"), null },
        p_nf_values = { u8("octal"), u8("hex"), u8("alpha"), null },
        p_ff_values = { FF_UNIX, FF_DOS, FF_MAC, null },
        p_mousem_values = { u8("extend"), u8("popup"), u8("popup_setpos"), u8("mac"), null },
        p_sel_values = { u8("inclusive"), u8("exclusive"), u8("old"), null },
        p_slm_values = { u8("mouse"), u8("key"), u8("cmd"), null },
        p_km_values = { u8("startsel"), u8("stopsel"), null },
        p_scbopt_values = { u8("ver"), u8("hor"), u8("jump"), null },
        p_debug_values = { u8("msg"), u8("throw"), u8("beep"), null },
        p_ead_values = { u8("both"), u8("ver"), u8("hor"), null },
        p_bs_values = { u8("indent"), u8("eol"), u8("start"), null };

    /*
     * Initialize the options, first part.
     *
     * Called only once from main(), just after creating the first buffer.
     */
    /*private*/ static void set_init_1()
    {
        /*
         * Set all the options (except the terminal options) to their default value.
         * Also set the global value for local options.
         */
        set_options_default(0);

        curbuf.b_p_initialized = true;
        curbuf.b_p_ar[0] = -1;     /* no local 'autoread' value */
        curbuf.b_p_ul[0] = NO_LOCAL_UNDOLEVEL;
        check_buf_options(curbuf);
        check_win_options(curwin);
        check_options();

        didset_options();

        /*
         * initialize the table for 'breakat'.
         */
        fill_breakat_flags();

        /* Initialize the highlight_attr[] table. */
        highlight_changed();

        save_file_ff(curbuf);       /* Buffer is unchanged */

        /* Parse default for 'wildmode'. */
        check_opt_wim();

        /* Parse default for 'fillchars'. */
        set_chars_option(p_fcs);

        /* Parse default for 'clipboard'. */
        check_clipboard_option();

        /* The cell width depends on the type of multi-byte characters. */
        init_chartab();

        /* When enc_utf8 is set or reset, (de)allocate screenLinesUC[]. */
        screenalloc(false);
    }

    /*
     * Set an option to its default value.
     * This does not take care of side effects!
     */
    /*private*/ static void set_option_default(int opt_idx, int opt_flags)
        /* opt_flags: OPT_FREE, OPT_LOCAL and/or OPT_GLOBAL */
    {
        boolean both = (opt_flags & (OPT_LOCAL | OPT_GLOBAL)) == 0;

        Object varp = get_varp_scope(vimoptions[opt_idx], both ? OPT_LOCAL : opt_flags);
        long flags = vimoptions[opt_idx].flags[0];

        if (varp != null)       /* skip hidden option, nothing to do for it */
        {
            if ((flags & P_STRING) != 0)
            {
                /* Use set_string_option_direct() for local options
                 * to handle freeing and allocating the value. */
                if (vimoptions[opt_idx].indir != PV_NONE)
                    set_string_option_direct(null, opt_idx, (Bytes)vimoptions[opt_idx].def_val, opt_flags, 0);
                else
                    ((Bytes[])varp)[0] = (Bytes)vimoptions[opt_idx].def_val;
            }
            else if ((flags & P_NUM) != 0)
            {
                if (vimoptions[opt_idx].indir == PV_SCROLL)
                    win_comp_scroll(curwin);
                else
                {
                    ((long[])varp)[0] = (long)vimoptions[opt_idx].def_val;
                    /* May also set global value for local option. */
                    if (both)
                        ((long[])get_varp_scope(vimoptions[opt_idx], OPT_GLOBAL))[0] = ((long[])varp)[0];
                }
            }
            else    /* P_BOOL */
            {
                /* For 'autoread' -1 means to use global value. */
                if (varp == curbuf.b_p_ar)
                    ((long[])varp)[0] = ((boolean)vimoptions[opt_idx].def_val) ? TRUE : FALSE;
                else
                    ((boolean[])varp)[0] = (boolean)vimoptions[opt_idx].def_val;
                /* May also set global value for local option. */
                if (both)
                {
                    /* For 'autoread' -1 means to use global value. */
                    if (varp == curbuf.b_p_ar)
                        ((boolean[])get_varp_scope(vimoptions[opt_idx], OPT_GLOBAL))[0] = (((long[])varp)[0] != 0);
                    else
                        ((boolean[])get_varp_scope(vimoptions[opt_idx], OPT_GLOBAL))[0] = ((boolean[])varp)[0];
                }
            }

            /* The default value is not insecure. */
            long[] flagsp = insecure_flag(opt_idx, opt_flags);
            flagsp[0] &= ~P_INSECURE;
        }

        set_option_scriptID_idx(opt_idx, opt_flags, current_SID);
    }

    /*
     * Set all options (except terminal options) to their default value.
     */
    /*private*/ static void set_options_default(int opt_flags)
        /* opt_flags: OPT_FREE, OPT_LOCAL and/or OPT_GLOBAL */
    {
        for (int i = 0; !istermoption(vimoptions[i]); i++)
            if ((vimoptions[i].flags[0] & P_NODEFAULT) == 0)
                set_option_default(i, opt_flags);

        /* The 'scroll' option must be computed for all windows. */
        for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
            for (window_C wp = (tp == curtab) ? firstwin : tp.tp_firstwin; wp != null; wp = wp.w_next)
                win_comp_scroll(wp);

        parse_cino(curbuf);
    }

    /*
     * Set the Vi-default value of a string option.
     * Used for 'term'.
     */
    /*private*/ static void set_string_default(Bytes name, Bytes val)
    {
        int opt_idx = findoption(name);
        if (0 <= opt_idx)
            vimoptions[opt_idx].def_val = STRDUP(val);
    }

    /*
     * Set the Vi-default value of a number option.
     * Used for 'lines' and 'columns'.
     */
    /*private*/ static void set_number_default(Bytes name, long val)
    {
        int opt_idx = findoption(name);
        if (0 <= opt_idx)
            vimoptions[opt_idx].def_val = val;
    }

    /*
     * Initialize the options, part two: After getting Rows and Columns and setting 'term'.
     */
    /*private*/ static void set_init_2()
    {
        /*
         * 'scroll' defaults to half the window height.
         * Note that this default is wrong when the window height changes.
         */
        set_number_default(u8("scroll"), Rows[0] >>> 1);
        int idx = findoption(u8("scroll"));
        if (0 <= idx && (vimoptions[idx].flags[0] & P_WAS_SET) == 0)
            set_option_default(idx, OPT_LOCAL);
        comp_col();

        /*
         * 'window' is only for backwards compatibility with Vi.
         * Default is Rows - 1.
         */
        if (!option_was_set(u8("window")))
            p_window[0] = Rows[0] - 1;
        set_number_default(u8("window"), Rows[0] - 1);

        /*
         * If 'background' wasn't set by the user, try guessing the value,
         * depending on the terminal name.  Only need to check for terminals
         * with a dark background, that can handle color.
         */
        idx = findoption(u8("bg"));
        if (0 <= idx && (vimoptions[idx].flags[0] & P_WAS_SET) == 0 && term_bg_default().at(0) == (byte)'d')
        {
            set_string_option_direct(null, idx, u8("dark"), OPT_FREE, 0);
            /* don't mark it as set, when starting the GUI it may be changed again */
            vimoptions[idx].flags[0] &= ~P_WAS_SET;
        }
    }

    /*
     * Return "dark" or "light" depending on the kind of terminal.
     * This is just guessing!  Recognized are:
     * "linux"          Linux console
     * "screen.linux"   Linux console with screen
     */
    /*private*/ static Bytes term_bg_default()
    {
        if (STRCMP(T_NAME[0], u8("linux")) == 0 || STRCMP(T_NAME[0], u8("screen.linux")) == 0)
            return u8("dark");

        return u8("light");
    }

    /*
     * Parse 'arg' for option settings.
     *
     * 'arg' may be ioBuff, but only when no errors can be present
     * and option does not need to be expanded with option_expand().
     * "opt_flags":
     * 0 for ":set"
     * OPT_GLOBAL   for ":setglobal"
     * OPT_LOCAL    for ":setlocal" and a modeline
     * OPT_MODELINE for a modeline
     * OPT_WINONLY  to only set window-local options
     * OPT_NOWIN    to skip setting window-local options
     *
     * returns false if an error is detected, true otherwise
     */
    /*private*/ static boolean do_set(Bytes arg, int opt_flags)
        /* arg: option string (may be written to!) */
    {
        boolean did_show = false;       /* already showed one value */
        if (arg.at(0) == NUL)
        {
            showoptions(0, opt_flags);
            did_show = true;
        }
        else
        {
            Bytes errbuf = new Bytes(80);
            Bytes key_name = new Bytes(2);

            Object varp = null;              /* pointer to variable for current option */

            for ( ; arg.at(0) != NUL; arg = skipwhite(arg))             /* loop to process all options */
            {
                Bytes errmsg = null;
                Bytes startarg = arg;      /* remember for error message */

                if (STRNCMP(arg, u8("all"), 3) == 0 && !asc_isalpha(arg.at(3)) && (opt_flags & OPT_MODELINE) == 0)
                {
                    /*
                     * ":set all"  show all options.
                     * ":set all&" set all options to their default value.
                     */
                    arg = arg.plus(3);
                    if (arg.at(0) == (byte)'&')
                    {
                        arg = arg.plus(1);
                        /* Only for :set command set global value of local options. */
                        set_options_default(OPT_FREE | opt_flags);
                    }
                    else
                    {
                        showoptions(1, opt_flags);
                        did_show = true;
                    }
                }
                else if (STRNCMP(arg, u8("termcap"), 7) == 0 && (opt_flags & OPT_MODELINE) == 0)
                {
                    showoptions(2, opt_flags);
                    show_termcodes();
                    did_show = true;
                    arg = arg.plus(7);
                }
                else
                {
                    int prefix = 1;     /* 1: nothing, 0: "no", 2: "inv" in front of name */
                    if (STRNCMP(arg, u8("no"), 2) == 0 && STRNCMP(arg, u8("novice"), 6) != 0)
                    {
                        prefix = 0;
                        arg = arg.plus(2);
                    }
                    else if (STRNCMP(arg, u8("inv"), 3) == 0)
                    {
                        prefix = 2;
                        arg = arg.plus(3);
                    }

                    skip:
                    {
                        int nextchar;           /* next non-white char after option name */
                        int opt_idx;
                        int len;

                        /* find end of name */
                        int key = 0;
                        if (arg.at(0) == (byte)'<')
                        {
                            nextchar = 0;
                            opt_idx = -1;
                            /* look out for <t_>;> */
                            if (arg.at(1) == (byte)'t' && arg.at(2) == (byte)'_' && arg.at(3) != NUL && arg.at(4) != NUL)
                                len = 5;
                            else
                            {
                                len = 1;
                                while (arg.at(len) != NUL && arg.at(len) != (byte)'>')
                                    len++;
                            }
                            if (arg.at(len) != (byte)'>')
                            {
                                errmsg = e_invarg;
                                break skip;
                            }
                            arg.be(len, NUL);                     /* put NUL after name */
                            if (arg.at(1) == (byte)'t' && arg.at(2) == (byte)'_') /* could be term code */
                                opt_idx = findoption(arg.plus(1));
                            arg.be(len++, (byte)'>');                   /* restore '>' */
                            if (opt_idx == -1)
                                key = find_key_option(arg.plus(1));
                        }
                        else
                        {
                            len = 0;
                            /*
                             * The two characters after "t_" may not be alphanumeric.
                             */
                            if (arg.at(0) == (byte)'t' && arg.at(1) == (byte)'_' && arg.at(2) != NUL && arg.at(3) != NUL)
                                len = 4;
                            else
                                while (asc_isalnum(arg.at(len)) || arg.at(len) == (byte)'_')
                                    len++;
                            nextchar = arg.at(len);
                            arg.be(len, NUL);                     /* put NUL after name */
                            opt_idx = findoption(arg);
                            arg.be(len, nextchar);                /* restore nextchar */
                            if (opt_idx == -1)
                                key = find_key_option(arg);
                        }

                        /* remember character after option name */
                        byte afterchar = arg.at(len);

                        /* skip white space, allow ":set ai  ?" */
                        while (vim_iswhite(arg.at(len)))
                            len++;

                        boolean adding = false;             /* "opt+=arg" */
                        boolean prepending = false;         /* "opt^=arg" */
                        boolean removing = false;           /* "opt-=arg" */

                        if (arg.at(len) != NUL && arg.at(len + 1) == (byte)'=')
                        {
                            if (arg.at(len) == (byte)'+')
                            {
                                adding = true;              /* "+=" */
                                len++;
                            }
                            else if (arg.at(len) == (byte)'^')
                            {
                                prepending = true;          /* "^=" */
                                len++;
                            }
                            else if (arg.at(len) == (byte)'-')
                            {
                                removing = true;            /* "-=" */
                                len++;
                            }
                        }
                        nextchar = arg.at(len);

                        if (opt_idx == -1 && key == 0)      /* found a mismatch: skip */
                        {
                            errmsg = u8("E518: Unknown option");
                            break skip;
                        }

                        long flags;             /* flags for current option */
                        if (0 <= opt_idx)
                        {
                            if (vimoptions[opt_idx].var == null)   /* hidden option: skip */
                            {
                                /* Only give an error message when requesting
                                 * the value of a hidden option, ignore setting it. */
                                if (vim_strchr(u8("=:!&<"), nextchar) == null
                                        && ((vimoptions[opt_idx].flags[0] & P_BOOL) == 0 || nextchar == '?'))
                                    errmsg = u8("E519: Option not supported");
                                break skip;
                            }

                            flags = vimoptions[opt_idx].flags[0];
                            varp = get_varp_scope(vimoptions[opt_idx], opt_flags);
                        }
                        else
                        {
                            flags = P_STRING;
                            if (key < 0)
                            {
                                key_name.be(0, KEY2TERMCAP0(key));
                                key_name.be(1, KEY2TERMCAP1(key));
                            }
                            else
                            {
                                key_name.be(0, KS_KEY);
                                key_name.be(1, (byte)(key & 0xff));
                            }
                        }

                        /* Skip all options that are not window-local
                         * (used when showing an already loaded buffer in a window). */
                        if ((opt_flags & OPT_WINONLY) != 0 && (opt_idx < 0 || vimoptions[opt_idx].var != VAR_WIN))
                            break skip;

                        /* Skip all options that are window-local (used for :vimgrep). */
                        if ((opt_flags & OPT_NOWIN) != 0 && 0 <= opt_idx && vimoptions[opt_idx].var == VAR_WIN)
                            break skip;

                        /* Disallow changing some options from modelines. */
                        if ((opt_flags & OPT_MODELINE) != 0)
                        {
                            if ((flags & (P_SECURE | P_NO_ML)) != 0)
                            {
                                errmsg = u8("E520: Not allowed in a modeline");
                                break skip;
                            }
                        }

                        /* Disallow changing some options in the sandbox. */
                        if (sandbox != 0 && (flags & P_SECURE) != 0)
                        {
                            errmsg = e_sandbox;
                            break skip;
                        }

                        if (vim_strchr(u8("?=:!&<"), nextchar) != null)
                        {
                            arg = arg.plus(len);
                            if (nextchar == '&' && arg.at(1) == (byte)'v' && arg.at(2) == (byte)'i')
                            {
                                if (arg.at(3) == (byte)'m')  /* "opt&vim": set to Vim default */
                                    arg = arg.plus(3);
                                else                /* "opt&vi": set to Vi default */
                                    arg = arg.plus(2);
                            }
                            if (vim_strchr(u8("?!&<"), nextchar) != null && arg.at(1) != NUL && !vim_iswhite(arg.at(1)))
                            {
                                errmsg = e_trailing;
                                break skip;
                            }
                        }

                        /*
                         * allow '=' and ':' as MSDOS command.com allows
                         * only one '=' character per "set" command line
                         */
                        if (nextchar == '?' || (prefix == 1 && vim_strchr(u8("=:&<"), nextchar) == null && (flags & P_BOOL) == 0))
                        {
                            /* print value */
                            if (did_show)
                                msg_putchar('\n');      /* cursor below last one */
                            else
                            {
                                gotocmdline(true);      /* cursor at status line */
                                did_show = true;        /* remember that we did a line */
                            }
                            if (0 <= opt_idx)
                            {
                                showoneopt(vimoptions[opt_idx], opt_flags);
                                if (0 < p_verbose[0])
                                {
                                    int mask = (vimoptions[opt_idx].indir & PV_MASK);

                                    /* Mention where the option was last set. */
                                    if (varp == vimoptions[opt_idx].var)
                                        last_set_msg(vimoptions[opt_idx].scriptID);
                                    else if ((vimoptions[opt_idx].indir & PV_WIN) != 0)
                                        last_set_msg(curwin.w_onebuf_opt.wo_scriptID[mask]);
                                    else if ((vimoptions[opt_idx].indir & PV_BUF) != 0)
                                        last_set_msg(curbuf.b_p_scriptID[mask]);
                                }
                            }
                            else
                            {
                                Bytes p = find_termcode(key_name);
                                if (p == null)
                                {
                                    errmsg = u8("E846: Key code not set");
                                    break skip;
                                }
                                else
                                    show_one_termcode(key_name, p, true);
                            }
                            if (nextchar != '?' && nextchar != NUL && !vim_iswhite(afterchar))
                                errmsg = e_trailing;
                        }
                        else
                        {
                            if ((flags & P_BOOL) != 0)          /* boolean */
                            {
                                if (nextchar == '=' || nextchar == ':')
                                {
                                    errmsg = e_invarg;
                                    break skip;
                                }

                                boolean value;

                                /*
                                 * ":set opt!": invert
                                 * ":set opt&": reset to default value
                                 * ":set opt<": reset to global value
                                 */
                                if (nextchar == '!')
                                    value = !((boolean[])varp)[0];
                                else if (nextchar == '&')
                                    value = (boolean)vimoptions[opt_idx].def_val;
                                else if (nextchar == '<')
                                {
                                    /* For 'autoread' -1 means to use global value. */
                                 /* if (varp == curbuf.b_p_ar && opt_flags == OPT_LOCAL)
                                        value = -1;
                                    else */
                                        value = ((boolean[])get_varp_scope(vimoptions[opt_idx], OPT_GLOBAL))[0];
                                }
                                else
                                {
                                    /*
                                     * ":set invopt": invert
                                     * ":set opt" or ":set noopt": set or reset
                                     */
                                    if (nextchar != NUL && !vim_iswhite(afterchar))
                                    {
                                        errmsg = e_trailing;
                                        break skip;
                                    }
                                    if (prefix == 2)            /* inv */
                                        value = !((boolean[])varp)[0];
                                    else
                                        value = (prefix != 0);
                                }

                                errmsg = set_bool_option(opt_idx, (boolean[])varp, value, opt_flags);
                            }
                            else                                /* numeric or string */
                            {
                                if (vim_strchr(u8("=:&<"), nextchar) == null || prefix != 1)
                                {
                                    errmsg = e_invarg;
                                    break skip;
                                }

                                if ((flags & P_NUM) != 0)       /* numeric */
                                {
                                    long value;

                                    /*
                                     * Different ways to set a number option:
                                     * &        set to default value
                                     * <        set to global value
                                     * <xx>     accept special key codes for 'wildchar'
                                     * c        accept any non-digit for 'wildchar'
                                     * [-]0-9   set number
                                     * other    error
                                     */
                                    arg = arg.plus(1);
                                    if (nextchar == '&')
                                        value = (long)vimoptions[opt_idx].def_val;
                                    else if (nextchar == '<')
                                    {
                                        /* For 'autoread' -1 means to use global value. */
                                        if (varp == curbuf.b_p_ar && opt_flags == OPT_LOCAL)
                                            value = -1;
                                        else
                                        /* For 'undolevels' NO_LOCAL_UNDOLEVEL means to use the global value. */
                                        if (varp == curbuf.b_p_ul && opt_flags == OPT_LOCAL)
                                            value = NO_LOCAL_UNDOLEVEL;
                                        else
                                            value = ((long[])get_varp_scope(vimoptions[opt_idx], OPT_GLOBAL))[0];
                                    }
                                    else if ((varp == p_wc || varp == p_wcm)
                                            && (arg.at(0) == (byte)'<' || arg.at(0) == (byte)'^'
                                                || ((arg.at(1) == NUL || vim_iswhite(arg.at(1)))
                                                    && !asc_isdigit(arg.at(0)))))
                                    {
                                        value = parse_key(arg);
                                        if (value == 0 && varp != p_wcm)
                                        {
                                            errmsg = e_invarg;
                                            break skip;
                                        }
                                    }
                                    else if (arg.at(0) == (byte)'-' || asc_isdigit(arg.at(0)))
                                    {
                                        int[] ip = new int[1];

                                        /* Allow negative (for 'undolevels'), octal and hex numbers. */
                                        { long[] __ = new long[1]; vim_str2nr(arg, null, ip, TRUE, TRUE, __); value = __[0]; }
                                        if (arg.at(ip[0]) != NUL && !vim_iswhite(arg.at(ip[0])))
                                        {
                                            errmsg = e_invarg;
                                            break skip;
                                        }
                                    }
                                    else
                                    {
                                        errmsg = u8("E521: Number required after =");
                                        break skip;
                                    }

                                    if (adding)
                                        value = ((long[])varp)[0] + value;
                                    if (prepending)
                                        value = ((long[])varp)[0] * value;
                                    if (removing)
                                        value = ((long[])varp)[0] - value;
                                    errmsg = set_num_option(opt_idx, (long[])varp, value, errbuf, errbuf.size(), opt_flags);
                                }
                                else if (0 <= opt_idx)                  /* string */
                                {
                                    Bytes save_arg = null;

                                    /* When using ":set opt=val" for a global option with a local value,
                                     * the local value will be reset, use the global value here. */
                                    if ((opt_flags & (OPT_LOCAL | OPT_GLOBAL)) == 0
                                            && (vimoptions[opt_idx].indir & PV_BOTH) != 0)
                                        varp = vimoptions[opt_idx].var;

                                    Bytes newval;
                                    /* The old value is kept until we are sure that the new value is valid. */
                                    Bytes oldval = ((Bytes[])varp)[0];
                                    if (nextchar == '&')    /* set to default val */
                                    {
                                        newval = (Bytes)vimoptions[opt_idx].def_val;
                                        if (varp == p_bg)
                                        {
                                            /* guess the value of 'background' */
                                            newval = term_bg_default();
                                        }

                                        if (newval == null)
                                            newval = EMPTY_OPTION;
                                        else
                                            newval = STRDUP(newval);
                                    }
                                    else if (nextchar == '<')       /* set to global val */
                                    {
                                        newval = STRDUP(((Bytes[])get_varp_scope(vimoptions[opt_idx], OPT_GLOBAL))[0]);
                                    }
                                    else
                                    {
                                        arg = arg.plus(1);      /* jump to after the '=' or ':' */

                                        /*
                                         * Set 'keywordprg' to ":echo" if an empty
                                         * value was passed to :set by the user.
                                         * Misuse errbuf[] for the resulting string.
                                         */
                                        if (varp == p_kp && (arg.at(0) == NUL || arg.at(0) == (byte)' '))
                                        {
                                            STRCPY(errbuf, u8(":echo"));
                                            save_arg = arg;
                                            arg = errbuf;
                                        }
                                        /*
                                         * Convert 'backspace' number to string, for
                                         * adding, prepending and removing string.
                                         */
                                        else if (varp == p_bs && asc_isdigit(p_bs[0].at(0)))
                                        {
                                            int i = (int)getdigits(p_bs);
                                            switch (i)
                                            {
                                                case 0:
                                                    p_bs[0] = EMPTY_OPTION;
                                                    break;
                                                case 1:
                                                    p_bs[0] = STRDUP(u8("indent,eol"));
                                                    break;
                                                case 2:
                                                    p_bs[0] = STRDUP(u8("indent,eol,start"));
                                                    break;
                                            }
                                            oldval = p_bs[0];
                                        }
                                        /*
                                         * Convert 'whichwrap' number to string, for
                                         * backwards compatibility with Vim 3.0.
                                         * Misuse errbuf[] for the resulting string.
                                         */
                                        else if (varp == p_ww && asc_isdigit(arg.at(0)))
                                        {
                                            errbuf.be(0, NUL);
                                            int i;
                                            { Bytes[] __ = { arg }; i = (int)getdigits(__); arg = __[0]; }
                                            if ((i & 1) != 0)
                                                STRCAT(errbuf, u8("b,"));
                                            if ((i & 2) != 0)
                                                STRCAT(errbuf, u8("s,"));
                                            if ((i & 4) != 0)
                                                STRCAT(errbuf, u8("h,l,"));
                                            if ((i & 8) != 0)
                                                STRCAT(errbuf, u8("<,>,"));
                                            if ((i & 16) != 0)
                                                STRCAT(errbuf, u8("[,],"));
                                            if (errbuf.at(0) != NUL)     /* remove trailing , */
                                                errbuf.be(strlen(errbuf) - 1, NUL);
                                            save_arg = arg;
                                            arg = errbuf;
                                        }

                                        /* When setting the local value of a global option,
                                         * the old value may be the global value. */
                                        Bytes origval;
                                        if ((vimoptions[opt_idx].indir & PV_BOTH) != 0 && (opt_flags & OPT_LOCAL) != 0)
                                            origval = ((Bytes[])get_varp(vimoptions[opt_idx], false))[0];
                                        else
                                            origval = oldval;

                                        /*
                                         * Copy the new string into allocated memory.
                                         * Can't use set_string_option_direct(),
                                         * because we need to remove the backslashes.
                                         */
                                        /* get a bit too much */
                                        int newlen = strlen(arg) + 1;
                                        if (adding || prepending || removing)
                                            newlen += strlen(origval) + 1;
                                        newval = new Bytes(newlen);
                                        Bytes s = newval;

                                        /*
                                         * Copy the string, skip over escaped chars.
                                         * For MS-DOS and WIN32 backslashes before normal
                                         * file name characters are not removed, and keep
                                         * backslash at start, for "\\machine\path", but
                                         * do remove it for "\\\\machine\\path".
                                         * The reverse is found in expandOldSetting().
                                         */
                                        while (arg.at(0) != NUL && !vim_iswhite(arg.at(0)))
                                        {
                                            if (arg.at(0) == (byte)'\\' && arg.at(1) != NUL)
                                                arg = arg.plus(1);      /* remove backslash */
                                            int i = us_ptr2len_cc(arg);
                                            if (1 < i)
                                            {
                                                /* copy multibyte char */
                                                BCOPY(s, arg, i);
                                                arg = arg.plus(i);
                                                s = s.plus(i);
                                            }
                                            else
                                                (s = s.plus(1)).be(-1, (arg = arg.plus(1)).at(-1));
                                        }
                                        s.be(0, NUL);

                                        /* Locate newval[] in origval[] when removing it
                                         * and when adding to avoid duplicates. */
                                        int i = 0;
                                        if (removing || (flags & P_NODUP) != 0)
                                        {
                                            i = strlen(newval);
                                            int bs = 0;
                                            for (s = origval; s.at(0) != NUL; s = s.plus(1))
                                            {
                                                if (((flags & P_COMMA) == 0
                                                            || BEQ(s, origval)
                                                            || (s.at(-1) == (byte)',' && (bs & 1) == 0))
                                                        && STRNCMP(s, newval, i) == 0
                                                        && ((flags & P_COMMA) == 0
                                                            || s.at(i) == (byte)','
                                                            || s.at(i) == NUL))
                                                    break;
                                                /* Count backslashes.  Only a comma with an
                                                 * even number of backslashes before it is
                                                 * recognized as a separator */
                                                if (BLT(origval, s) && s.at(-1) == (byte)'\\')
                                                    bs++;
                                                else
                                                    bs = 0;
                                            }

                                            /* do not add if already there */
                                            if ((adding || prepending) && s.at(0) != NUL)
                                            {
                                                prepending = false;
                                                adding = false;
                                                STRCPY(newval, origval);
                                            }
                                        }

                                        /* concatenate the two strings; add a ',' if needed */
                                        if (adding || prepending)
                                        {
                                            boolean comma = ((flags & P_COMMA) != 0 && origval.at(0) != NUL && newval.at(0) != NUL);
                                            if (adding)
                                            {
                                                i = strlen(origval);
                                                BCOPY(newval, i + (comma ? 1 : 0), newval, 0, strlen(newval) + 1);
                                                BCOPY(newval, origval, i);
                                            }
                                            else
                                            {
                                                i = strlen(newval);
                                                BCOPY(newval, i + (comma ? 1 : 0), origval, 0, strlen(origval) + 1);
                                            }
                                            if (comma)
                                                newval.be(i, (byte)',');
                                        }

                                        /* Remove newval[] from origval[].
                                         * (Note: "i" has been set above and is used here). */
                                        if (removing)
                                        {
                                            STRCPY(newval, origval);
                                            if (s.at(0) != NUL)
                                            {
                                                /* may need to remove a comma */
                                                if ((flags & P_COMMA) != 0)
                                                {
                                                    if (BEQ(s, origval))
                                                    {
                                                        /* include comma after string */
                                                        if (s.at(i) == (byte)',')
                                                            i++;
                                                    }
                                                    else
                                                    {
                                                        /* include comma before string */
                                                        s = s.minus(1);
                                                        i++;
                                                    }
                                                }
                                                BCOPY(newval, BDIFF(s, origval), s, i, strlen(s, i) + 1);
                                            }
                                        }

                                        if ((flags & P_FLAGLIST) != 0)
                                        {
                                            /* Remove flags that appear twice. */
                                            for (s = newval; s.at(0) != NUL; s = s.plus(1))
                                                if (((flags & P_COMMA) == 0 || s.at(0) != (byte)',') && vim_strbyte(s.plus(1), s.at(0)) != null)
                                                {
                                                    BCOPY(s, 0, s, 1, strlen(s, 1) + 1);
                                                    s = s.minus(1);
                                                }
                                        }

                                        if (save_arg != null)   /* number for 'whichwrap' */
                                            arg = save_arg;
                                    }

                                    /* Set the new value. */
                                    ((Bytes[])varp)[0] = newval;

                                    /* Handle side effects, and set the global value for ":set" on local options. */
                                    errmsg = did_set_string_option(opt_idx, (Bytes[])varp, oldval, errbuf, opt_flags);

                                    /* If error detected, print the error message. */
                                    if (errmsg != null)
                                        break skip;
                                }
                                else            /* key code option */
                                {
                                    if (nextchar == '&')
                                    {
                                        if (add_termcap_entry(key_name, true) == false)
                                            errmsg = u8("E522: Not found in termcap");
                                    }
                                    else
                                    {
                                        arg = arg.plus(1); /* jump to after the '=' or ':' */
                                        Bytes p;
                                        for (p = arg; p.at(0) != NUL && !vim_iswhite(p.at(0)); p = p.plus(1))
                                            if (p.at(0) == (byte)'\\' && p.at(1) != NUL)
                                                p = p.plus(1);
                                        nextchar = p.at(0);
                                        p.be(0, NUL);
                                        add_termcode(key_name, arg, FALSE);
                                        p.be(0, nextchar);
                                    }
                                    if (full_screen)
                                        ttest(false);
                                    redraw_all_later(CLEAR);
                                }
                            }

                            if (0 <= opt_idx)
                                did_set_option(opt_idx, opt_flags, !prepending && !adding && !removing);
                        }
                    }

                    /*
                     * Advance to next argument.
                     * - skip until a blank found, taking care of backslashes
                     * - skip blanks
                     * - skip one "=val" argument (for hidden options ":set gfn =xx")
                     */
                    for (int i = 0; i < 2 ; i++)
                    {
                        while (arg.at(0) != NUL && !vim_iswhite(arg.at(0)))
                            if ((arg = arg.plus(1)).at(-1) == (byte)'\\' && arg.at(0) != NUL)
                                arg = arg.plus(1);
                        arg = skipwhite(arg);
                        if (arg.at(0) != (byte)'=')
                            break;
                    }
                }

                if (errmsg != null)
                {
                    vim_strncpy(ioBuff, errmsg, IOSIZE - 1);
                    int i = strlen(ioBuff) + 2;
                    if (i + BDIFF(arg, startarg) < IOSIZE)
                    {
                        /* append the argument with the error */
                        STRCAT(ioBuff, u8(": "));
                        BCOPY(ioBuff, i, startarg, 0, BDIFF(arg, startarg));
                        ioBuff.be(i + BDIFF(arg, startarg), NUL);
                    }
                    /* make sure all characters are printable */
                    trans_characters(ioBuff, IOSIZE);

                    no_wait_return++;       /* wait_return done later */
                    emsg(ioBuff);           /* show error highlighted */
                    --no_wait_return;

                    return false;
                }
            }
        }

        if (silent_mode && did_show)
        {
            /* After displaying option values in silent mode. */
            silent_mode = false;
            info_message = true;
            msg_putchar('\n');
            cursor_on();                /* msg_start() switches it off */
            out_flush();
            silent_mode = true;
            info_message = false;
        }

        return true;
    }

    /*
     * Call this when an option has been given a new value through a user command.
     * Sets the P_WAS_SET flag and takes care of the P_INSECURE flag.
     */
    /*private*/ static void did_set_option(int opt_idx, int opt_flags, boolean new_value)
        /* opt_flags: possibly with OPT_MODELINE */
        /* new_value: value was replaced completely */
    {
        vimoptions[opt_idx].flags[0] |= P_WAS_SET;

        /* When an option is set in the sandbox from a modeline or in secure mode,
         * set the P_INSECURE flag.  Otherwise, if a new value is stored reset the flag. */
        long[] p = insecure_flag(opt_idx, opt_flags);
        if (secure != 0 || sandbox != 0 || (opt_flags & OPT_MODELINE) != 0)
            p[0] |= P_INSECURE;
        else if (new_value)
            p[0] &= ~P_INSECURE;
    }

    /*private*/ static Bytes illegal_char(Bytes errbuf, int c)
    {
        if (errbuf == null)
            return u8("");

        libC.sprintf(errbuf, u8("E539: Illegal character <%s>"), transchar(c));
        return errbuf;
    }

    /*
     * Convert a key name or ctrl into a key value.
     * Used for 'wildchar' and 'cedit' options.
     */
    /*private*/ static int parse_key(Bytes arg)
    {
        if (arg.at(0) == (byte)'<')
            return find_key_option(arg.plus(1));
        if (arg.at(0) == (byte)'^')
            return ctrl_key(arg.at(1));

        return char_u(arg.at(0));
    }

    /*
     * Check value of 'cedit' and set cedit_key.
     * Returns null if value is OK, error message otherwise.
     */
    /*private*/ static Bytes check_cedit()
    {
        if (p_cedit[0].at(0) == NUL)
            cedit_key = -1;
        else
        {
            int key = parse_key(p_cedit[0]);
            if (vim_isprintc(key))
                return e_invarg;
            cedit_key = key;
        }

        return null;
    }

    /*
     * set_options_bin -- called when 'bin' changes value.
     */
    /*private*/ static void set_options_bin(boolean oldval, boolean newval, int opt_flags)
        /* opt_flags: OPT_LOCAL and/or OPT_GLOBAL */
    {
        /*
         * The option values that are changed when 'bin' changes are
         * copied when 'bin is set and restored when 'bin' is reset.
         */
        if (newval)
        {
            if (!oldval)                    /* switched on */
            {
                if ((opt_flags & OPT_GLOBAL) == 0)
                {
                    curbuf.b_p_tw_nobin = curbuf.b_p_tw[0];
                    curbuf.b_p_wm_nobin = curbuf.b_p_wm[0];
                    curbuf.b_p_et_nobin = curbuf.b_p_et[0];
                }
                if ((opt_flags & OPT_LOCAL) == 0)
                {
                    p_tw_nobin = p_tw[0];
                    p_wm_nobin = p_wm[0];
                    p_et_nobin = p_et[0];
                }
            }

            if ((opt_flags & OPT_GLOBAL) == 0)
            {
                curbuf.b_p_tw[0] = 0;          /* no automatic line wrap */
                curbuf.b_p_wm[0] = 0;          /* no automatic line wrap */
                curbuf.b_p_et[0] = false;          /* no expandtab */
            }
            if ((opt_flags & OPT_LOCAL) == 0)
            {
                p_tw[0] = 0;
                p_wm[0] = 0;
                p_et[0] = false;
                p_bin[0] = true;               /* needed when called for the "-b" argument */
            }
        }
        else if (oldval)                    /* switched off */
        {
            if ((opt_flags & OPT_GLOBAL) == 0)
            {
                curbuf.b_p_tw[0] = curbuf.b_p_tw_nobin;
                curbuf.b_p_wm[0] = curbuf.b_p_wm_nobin;
                curbuf.b_p_et[0] = curbuf.b_p_et_nobin;
            }
            if ((opt_flags & OPT_LOCAL) == 0)
            {
                p_tw[0] = p_tw_nobin;
                p_wm[0] = p_wm_nobin;
                p_et[0] = p_et_nobin;
            }
        }
    }

    /*
     * After setting various option values: recompute variables that depend on option values.
     */
    /*private*/ static void didset_options()
    {
        /* initialize the table for 'iskeyword' et. al. */
        init_chartab();

        opt_strings_flags(p_dy[0], p_dy_values, dy_flags, true);
        opt_strings_flags(p_ve[0], p_ve_values, ve_flags, true);
        opt_strings_flags(p_ttym[0], p_ttym_values, ttym_flags, false);
        /* set cedit_key */
        check_cedit();
        briopt_check(curwin);
    }

    /*
     * Check for string options that are null (normally only termcap options).
     */
    /*private*/ static void check_options()
    {
        for (int opt_idx = 0; vimoptions[opt_idx].fullname != null; opt_idx++)
            if ((vimoptions[opt_idx].flags[0] & P_STRING) != 0 && vimoptions[opt_idx].var != null)
                check_string_option((Bytes[])get_varp(vimoptions[opt_idx], false));
    }

    /*
     * Check string options in a buffer for null value.
     */
    /*private*/ static void check_buf_options(buffer_C buf)
    {
        check_string_option(buf.b_p_fenc);
        check_string_option(buf.b_p_ff);
        check_string_option(buf.b_p_inde);
        check_string_option(buf.b_p_indk);
        check_string_option(buf.b_p_fex);
        check_string_option(buf.b_p_kp);
        check_string_option(buf.b_p_mps);
        check_string_option(buf.b_p_fo);
        check_string_option(buf.b_p_flp);
        check_string_option(buf.b_p_isk);
        check_string_option(buf.b_p_com);
        check_string_option(buf.b_p_nf);
        check_string_option(buf.b_p_qe);
        check_string_option(buf.b_p_syn);
        check_string_option(buf.b_p_cink);
        check_string_option(buf.b_p_cino);
        parse_cino(buf);
        check_string_option(buf.b_p_ft);
        check_string_option(buf.b_p_cinw);
        check_string_option(buf.b_p_ep);
        check_string_option(buf.b_p_lw);
    }

    /*private*/ static void clear_string_option(Bytes[] pp)
    {
        pp[0] = EMPTY_OPTION;
    }

    /*private*/ static void check_string_option(Bytes[] pp)
    {
        if (pp[0] == null)
            pp[0] = EMPTY_OPTION;
    }

    /*
     * Return true when option "opt" was set from a modeline or in secure mode.
     * Return false when it wasn't.
     * Return -1 for an unknown option.
     */
    /*private*/ static boolean was_set_insecurely(Bytes opt, int opt_flags)
    {
        int idx = findoption(opt);
        if (0 <= idx)
        {
            long[] flagp = insecure_flag(idx, opt_flags);
            return ((flagp[0] & P_INSECURE) != 0);
        }

        emsg2(e_intern2, u8("was_set_insecurely()"));
        return /*-1*/true;
    }

    /*
     * Get a pointer to the flags used for the P_INSECURE flag of option
     * "opt_idx".  For some local options a local flags field is used.
     */
    /*private*/ static long[] insecure_flag(int opt_idx, int opt_flags)
    {
        if ((opt_flags & OPT_LOCAL) != 0)
            switch (vimoptions[opt_idx].indir)
            {
                case PV_STL:        return curwin.w_p_stl_flags;
                case PV_INDE:       return curbuf.b_p_inde_flags;
                case PV_FEX:        return curbuf.b_p_fex_flags;
            }

        /* Nothing special, return global flags field. */
        return vimoptions[opt_idx].flags;
    }

    /*
     * Redraw the tab page text later.
     */
    /*private*/ static void redraw_titles()
    {
        redraw_tabline = true;
    }

    /*
     * Set a string option to a new value (without checking the effect).
     * The string is copied into allocated memory.
     * If ("opt_idx" == -1) "name" is used, otherwise "opt_idx" is used.
     * When "set_sid" is zero set the scriptID to current_SID.  When "set_sid" is
     * SID_NONE don't set the scriptID.  Otherwise set the scriptID to "set_sid".
     */
    /*private*/ static void set_string_option_direct(Bytes name, int opt_idx, Bytes val, int opt_flags, int set_sid)
        /* opt_flags: OPT_FREE, OPT_LOCAL and/or OPT_GLOBAL */
    {
        boolean both = (opt_flags & (OPT_LOCAL | OPT_GLOBAL)) == 0;
        int idx = opt_idx;

        if (idx == -1)                      /* use name */
        {
            idx = findoption(name);
            if (idx < 0)                    /* not found (should not happen) */
            {
                emsg2(e_intern2, u8("set_string_option_direct()"));
                emsg2(u8("For option %s"), name);
                return;
            }
        }

        if (vimoptions[idx].var == null)       /* can't set hidden option */
            return;

        Bytes[] varp = (Bytes[])get_varp_scope(vimoptions[idx], both ? OPT_LOCAL : opt_flags);
        varp[0] = STRDUP(val);

        /* For buffer/window local option may also set the global value. */
        if (both)
            set_string_option_global(idx, varp);

        /* When setting both values of a global option with a local value,
         * make the local value empty, so that the global value is used. */
        if ((vimoptions[idx].indir & PV_BOTH) != 0 && both)
            varp[0] = EMPTY_OPTION;
        if (set_sid != SID_NONE)
            set_option_scriptID_idx(idx, opt_flags, (set_sid == 0) ? current_SID : set_sid);
    }

    /*
     * Set global value for string option when it's a local option.
     */
    /*private*/ static void set_string_option_global(int opt_idx, Bytes[] varp)
        /* opt_idx: option index */
        /* varp: pointer to option variable */
    {
        vimoption_C v = vimoptions[opt_idx];

        /* the global value is always allocated */
        Bytes[] pp;
        if (v.var == VAR_WIN)
        {
            /* transform a pointer to a "w_onebuf_opt" option into a "w_allbuf_opt" option */
            pp = (Bytes[])get_varp(v, true);
        }
        else
            pp = (Bytes[])v.var;

        if (v.indir != PV_NONE && pp != varp)
            pp[0] = STRDUP(varp[0]);
    }

    /*
     * Set a string option to a new value, and handle the effects.
     *
     * Returns null on success or error message on error.
     */
    /*private*/ static Bytes set_string_option(int opt_idx, Bytes value, int opt_flags)
        /* opt_flags: OPT_LOCAL and/or OPT_GLOBAL */
    {
        if (vimoptions[opt_idx].var == null)   /* don't set hidden option */
            return null;

        Bytes[] varp = (Bytes[])get_varp_scope(vimoptions[opt_idx],
                (opt_flags & (OPT_LOCAL | OPT_GLOBAL)) == 0
                    ? ((vimoptions[opt_idx].indir & PV_BOTH) != 0 ? OPT_GLOBAL : OPT_LOCAL)
                    : opt_flags);

        Bytes oldval = varp[0];
        varp[0] = STRDUP(value);

        Bytes r = did_set_string_option(opt_idx, varp, oldval, null, opt_flags);
        if (r == null)
            did_set_option(opt_idx, opt_flags, true);

        return r;
    }

    /*
     * Handle string options that need some action to perform when changed.
     * Returns null for success, or an error message for an error.
     */
    /*private*/ static Bytes did_set_string_option(int opt_idx, Bytes[] varp, Bytes oldval, Bytes errbuf, int opt_flags)
        /* opt_idx: index in vimoptions[] table */
        /* varp: pointer to the option variable */
        /* oldval: previous value of the option */
        /* errbuf: buffer for errors, or null */
        /* opt_flags: OPT_LOCAL and/or OPT_GLOBAL */
    {
        Bytes errmsg = null;
        boolean did_chartab = false;

        /* Get the global option to compare with,
         * otherwise we would have to check two values for all local options. */
        Object gvarp = get_varp_scope(vimoptions[opt_idx], OPT_GLOBAL);

        /* Disallow changing some options from secure mode. */
        if ((secure != 0 || sandbox != 0) && (vimoptions[opt_idx].flags[0] & P_SECURE) != 0)
        {
            errmsg = e_secure;
        }

        /* Check for a "normal" file name in some options.
         * Disallow a path separator (slash and/or backslash),
         * wildcards and characters that are often illegal in a file name. */
        else if ((vimoptions[opt_idx].flags[0] & P_NFNAME) != 0 && STRPBRK(varp[0], u8("/\\*?[|<>")) != null)
        {
            errmsg = e_invarg;
        }

        /* 'term' */
        else if (varp == T_NAME)
        {
            if (T_NAME[0].at(0) == NUL)
                errmsg = u8("E529: Cannot set 'term' to empty string");
            else if (set_termname(T_NAME[0]) == false)
                errmsg = u8("E522: Not found in termcap");
            else
                /* Screen colors may have changed. */
                redraw_later_clear();
        }

        /* 'breakindentopt' */
        else if (varp == curwin.w_onebuf_opt.wo_briopt)
        {
            if (briopt_check(curwin) == false)
                errmsg = e_invarg;
        }

        /*
         * 'isident', 'iskeyword', 'isprint' or 'isfname' option: refill chartab[]
         * If the new option is invalid, use old value.
         * 'lisp' option: refill chartab[] for '-' char
         */
        else if (varp == p_isi || varp == curbuf.b_p_isk || varp == p_isp || varp == p_isf)
        {
            if (init_chartab() == false)
            {
                did_chartab = true;     /* need to restore it below */
                errmsg = e_invarg;      /* error in value */
            }
        }

        /* 'colorcolumn' */
        else if (varp == curwin.w_onebuf_opt.wo_cc)
            errmsg = check_colorcolumn(curwin);

        /* 'highlight' */
        else if (varp == p_hl)
        {
            if (highlight_changed() == false)
                errmsg = e_invarg;  /* invalid flags */
        }

        /* 'nrformats' */
        else if (gvarp == p_nf)
        {
            if (check_opt_strings(varp[0], p_nf_values, true) != true)
                errmsg = e_invarg;
        }

        /* 'scrollopt' */
        else if (varp == p_sbo)
        {
            if (check_opt_strings(p_sbo[0], p_scbopt_values, true) != true)
                errmsg = e_invarg;
        }

        /* 'ambiwidth' */
        else if (varp == p_ambw)
        {
            if (check_opt_strings(p_ambw[0], p_ambw_values, false) != true)
                errmsg = e_invarg;
            else if (set_chars_option(p_lcs) != null)
                errmsg = u8("E834: Conflicts with value of 'listchars'");
            else if (set_chars_option(p_fcs) != null)
                errmsg = u8("E835: Conflicts with value of 'fillchars'");
        }

        /* 'background' */
        else if (varp == p_bg)
        {
            if (check_opt_strings(p_bg[0], p_bg_values, false) == true)
            {
                boolean dark = (p_bg[0].at(0) == (byte)'d');

                init_highlight(false, false);

                if (dark != (p_bg[0].at(0) == (byte)'d') && get_var_value(u8("g:colors_name")) != null)
                {
                    /* The color scheme must have set 'background' back to another
                     * value, that's not what we want here.  Disable the color
                     * scheme and set the colors again. */
                    do_unlet(u8("g:colors_name"), true);
                    p_bg[0] = STRDUP(dark ? u8("dark") : u8("light"));
                    check_string_option(p_bg);
                    init_highlight(false, false);
                }
            }
            else
                errmsg = e_invarg;
        }

        /* 'wildmode' */
        else if (varp == p_wim)
        {
            if (check_opt_wim() == false)
                errmsg = e_invarg;
        }

        /* 'eventignore' */
        else if (varp == p_ei)
        {
            if (check_ei() == false)
                errmsg = e_invarg;
        }

        /* 'fileencoding' */
        else if (gvarp == p_fenc)
        {
            if (!curbuf.b_p_ma[0] && opt_flags != OPT_GLOBAL)
                errmsg = e_modifiable;
            else if (vim_strchr(varp[0], ',') != null)
                /* No comma allowed in 'fileencoding'; catches confusing it with 'fileencodings'. */
                errmsg = e_invarg;
            else
            {
                /* May show a "+" in the title now. */
                redraw_titles();
                /* Add 'fileencoding' to the swap file. */
                ml_setflags(curbuf);
            }

            if (errmsg == null)
            {
                /* canonize the value, so that strcmp() can be used on it */
                varp[0] = enc_canonize(varp[0]);
            }
        }

        /* 'fileformat' */
        else if (gvarp == p_ff)
        {
            if (!curbuf.b_p_ma[0] && (opt_flags & OPT_GLOBAL) == 0)
                errmsg = e_modifiable;
            else if (check_opt_strings(varp[0], p_ff_values, false) != true)
                errmsg = e_invarg;
            else
            {
                /* may also change 'textmode' */
                if (get_fileformat(curbuf) == EOL_DOS)
                    curbuf.b_p_tx[0] = true;
                else
                    curbuf.b_p_tx[0] = false;
                redraw_titles();
                /* update flag in swap file */
                ml_setflags(curbuf);
                /* Redraw needed when switching to/from "mac":
                 * a CR in the text will be displayed differently. */
                if (get_fileformat(curbuf) == EOL_MAC || oldval.at(0) == (byte)'m')
                    redraw_curbuf_later(NOT_VALID);
            }
        }

        /* 'fileformats' */
        else if (varp == p_ffs)
        {
            if (check_opt_strings(p_ffs[0], p_ff_values, true) != true)
                errmsg = e_invarg;
        }

        /* 'matchpairs' */
        else if (gvarp == p_mps)
        {
            for (Bytes p = varp[0]; p.at(0) != NUL; p = p.plus(1))
            {
                int x2 = -1;
                int x3 = -1;

                if (p.at(0) != NUL)
                    p = p.plus(us_ptr2len_cc(p));
                if (p.at(0) != NUL)
                    x2 = (p = p.plus(1)).at(-1);
                if (p.at(0) != NUL)
                {
                    x3 = us_ptr2char(p);
                    p = p.plus(us_ptr2len_cc(p));
                }
                if (x2 != ':' || x3 == -1 || (p.at(0) != NUL && p.at(0) != (byte)','))
                {
                    errmsg = e_invarg;
                    break;
                }
                if (p.at(0) == NUL)
                    break;
            }
        }

        /* 'comments' */
        else if (gvarp == p_com)
        {
            for (Bytes s = varp[0]; s.at(0) != NUL; )
            {
                while (s.at(0) != NUL && s.at(0) != (byte)':')
                {
                    if (vim_strchr(COM_ALL, s.at(0)) == null && !asc_isdigit(s.at(0)) && s.at(0) != (byte)'-')
                    {
                        errmsg = illegal_char(errbuf, s.at(0));
                        break;
                    }
                    s = s.plus(1);
                }
                if ((s = s.plus(1)).at(-1) == NUL)
                    errmsg = u8("E524: Missing colon");
                else if (s.at(0) == (byte)',' || s.at(0) == NUL)
                    errmsg = u8("E525: Zero length string");
                if (errmsg != null)
                    break;
                while (s.at(0) != NUL && s.at(0) != (byte)',')
                {
                    if (s.at(0) == (byte)'\\' && s.at(1) != NUL)
                        s = s.plus(1);
                    s = s.plus(1);
                }
                s = skip_to_option_part(s);
            }
        }

        /* 'listchars' */
        else if (varp == p_lcs)
        {
            errmsg = set_chars_option(varp);
        }

        /* 'fillchars' */
        else if (varp == p_fcs)
        {
            errmsg = set_chars_option(varp);
        }

        /* 'cedit' */
        else if (varp == p_cedit)
        {
            errmsg = check_cedit();
        }

        /* 'verbosefile' */
        else if (varp == p_vfile)
        {
            verbose_stop();
            if (p_vfile[0].at(0) != NUL && verbose_open() == false)
                errmsg = e_invarg;
        }

        /* terminal options */
        else if (istermoption(vimoptions[opt_idx]) && full_screen)
        {
            /* ":set t_Co=0" and ":set t_Co=1" do ":set t_Co=" */
            if (varp == T_CCO)
            {
                int colors = libC.atoi(T_CCO[0]);

                /* Only reinitialize colors if t_Co value has really changed to
                 * avoid expensive reload of colorscheme if t_Co is set to the
                 * same value multiple times. */
                if (colors != t_colors)
                {
                    t_colors = colors;
                    if (t_colors <= 1)
                        T_CCO[0] = EMPTY_OPTION;

                    /* We now have a different color setup, initialize it again. */
                    init_highlight(true, false);
                }
            }
            ttest(false);
            if (varp == T_ME)
            {
                out_str(T_ME[0]);
                redraw_later(CLEAR);
            }
        }

        /* 'showbreak' */
        else if (varp == p_sbr)
        {
            for (Bytes s = p_sbr[0]; s.at(0) != NUL; )
            {
                if (mb_ptr2cells(s) != 1)
                    errmsg = u8("E595: contains unprintable or wide character");
                s = s.plus(us_ptr2len_cc(s));
            }
        }

        /* 'breakat' */
        else if (varp == p_breakat)
            fill_breakat_flags();

        /* 'ttymouse' */
        else if (varp == p_ttym)
        {
            /* Switch the mouse off before changing the escape sequences used for that. */
            mch_setmouse(false);
            if (opt_strings_flags(p_ttym[0], p_ttym_values, ttym_flags, false) != true)
                errmsg = e_invarg;
            else
                check_mouse_termcode();
            if (termcap_active)
                setmouse();         /* may switch it on again */
        }

        /* 'selection' */
        else if (varp == p_sel)
        {
            if (p_sel[0].at(0) == NUL || check_opt_strings(p_sel[0], p_sel_values, false) != true)
                errmsg = e_invarg;
        }

        /* 'selectmode' */
        else if (varp == p_slm)
        {
            if (check_opt_strings(p_slm[0], p_slm_values, true) != true)
                errmsg = e_invarg;
        }

        /* 'keymodel' */
        else if (varp == p_km)
        {
            if (check_opt_strings(p_km[0], p_km_values, true) != true)
                errmsg = e_invarg;
            else
            {
                km_stopsel = (vim_strchr(p_km[0], 'o') != null);
                km_startsel = (vim_strchr(p_km[0], 'a') != null);
            }
        }

        /* 'mousemodel' */
        else if (varp == p_mousem)
        {
            if (check_opt_strings(p_mousem[0], p_mousem_values, false) != true)
                errmsg = e_invarg;
        }

        /* 'switchbuf' */
        else if (varp == p_swb)
        {
            if (opt_strings_flags(p_swb[0], p_swb_values, swb_flags, true) != true)
                errmsg = e_invarg;
        }

        /* 'debug' */
        else if (varp == p_debug)
        {
            if (check_opt_strings(p_debug[0], p_debug_values, true) != true)
                errmsg = e_invarg;
        }

        /* 'display' */
        else if (varp == p_dy)
        {
            if (opt_strings_flags(p_dy[0], p_dy_values, dy_flags, true) != true)
                errmsg = e_invarg;
            else
                init_chartab();
        }

        /* 'eadirection' */
        else if (varp == p_ead)
        {
            if (check_opt_strings(p_ead[0], p_ead_values, false) != true)
                errmsg = e_invarg;
        }

        /* 'clipboard' */
        else if (varp == p_cb)
            errmsg = check_clipboard_option();

        /* 'statusline' or 'rulerformat' */
        else if (gvarp == p_stl || varp == p_ruf)
        {
            if (varp == p_ruf)     /* reset ru_wid first */
                ru_wid = 0;
            Bytes s = varp[0];
            if (varp == p_ruf && s.at(0) == (byte)'%')
            {
                /* set ru_wid if 'ruf' starts with "%99(" */
                if ((s = s.plus(1)).at(0) == (byte)'-')    /* ignore a '-' */
                    s = s.plus(1);
                int wid;
                { Bytes[] __ = { s }; wid = (int)getdigits(__); s = __[0]; }
                if (wid != 0 && s.at(0) == (byte)'(' && (errmsg = check_stl_option(p_ruf[0])) == null)
                    ru_wid = wid;
                else
                    errmsg = check_stl_option(p_ruf[0]);
            }
            /* check 'statusline' only if it doesn't start with "%!" */
            else if (varp == p_ruf || s.at(0) != (byte)'%' || s.at(1) != (byte)'!')
                errmsg = check_stl_option(s);
            if (varp == p_ruf && errmsg == null)
                comp_col();
        }

        /* 'pastetoggle': translate key codes like in a mapping */
        else if (varp == p_pt)
        {
            if (p_pt[0].at(0) != NUL)
                p_pt[0] = replace_termcodes(p_pt[0], true, true, false);
        }

        /* 'backspace' */
        else if (varp == p_bs)
        {
            if (asc_isdigit(p_bs[0].at(0)))
            {
                if ('2' < p_bs[0].at(0) || p_bs[0].at(1) != NUL)
                    errmsg = e_invarg;
            }
            else if (check_opt_strings(p_bs[0], p_bs_values, true) != true)
                errmsg = e_invarg;
        }

        /* 'virtualedit' */
        else if (varp == p_ve)
        {
            if (opt_strings_flags(p_ve[0], p_ve_values, ve_flags, true) != true)
                errmsg = e_invarg;
            else if (STRCMP(p_ve[0], oldval) != 0)
            {
                /* Recompute cursor position in case the new 've' setting changes something. */
                validate_virtcol();
                coladvance(curwin.w_virtcol);
            }
        }

        /* 'cinoptions' */
        else if (gvarp == p_cino)
        {
            /* TODO: recognize errors */
            parse_cino(curbuf);
        }

        /* Options that are a list of flags. */
        else
        {
            Bytes p = null;
            if (varp == p_ww)
                p = WW_ALL;
            else if (varp == p_shm)
                p = SHM_ALL;
            else if (varp == p_cpo)
                p = CPO_ALL;
            else if (varp == curbuf.b_p_fo)
                p = FO_ALL;
            else if (varp == curwin.w_onebuf_opt.wo_cocu)
                p = COCU_ALL;
            else if (varp == p_mouse)
                p = MOUSE_ALL;
            if (p != null)
            {
                for (Bytes s = varp[0]; s.at(0) != NUL; s = s.plus(1))
                    if (vim_strchr(p, s.at(0)) == null)
                    {
                        errmsg = illegal_char(errbuf, s.at(0));
                        break;
                    }
            }
        }

        /*
         * If error detected, restore the previous value.
         */
        if (errmsg != null)
        {
            varp[0] = oldval;
            /*
             * When resetting some values, need to act on it.
             */
            if (did_chartab)
                init_chartab();
            if (varp == p_hl)
                highlight_changed();
        }
        else
        {
            /* Remember where the option was set. */
            set_option_scriptID_idx(opt_idx, opt_flags, current_SID);

            if ((opt_flags & (OPT_LOCAL | OPT_GLOBAL)) == 0 && (vimoptions[opt_idx].indir & PV_BOTH) != 0)
            {
                /* Global option with local value set to use global value;
                 * free the local value and make it empty. */
                Object v = get_varp_scope(vimoptions[opt_idx], OPT_LOCAL);
                ((Bytes[])v)[0] = EMPTY_OPTION;
            }
            else if ((opt_flags & OPT_LOCAL) == 0 && opt_flags != OPT_GLOBAL)
            {
                /* May set global value for local option. */
                set_string_option_global(opt_idx, varp);
            }

            /*
             * Trigger the autocommand only after setting the flags.
             */
            /* When 'syntax' is set, load the syntax of that name. */
            if (varp == curbuf.b_p_syn)
            {
                apply_autocmds(EVENT_SYNTAX, curbuf.b_p_syn[0], curbuf.b_fname, true, curbuf);
            }
            else if (varp == curbuf.b_p_ft)
            {
                /* 'filetype' is set, trigger the FileType autocommand */
                did_filetype = true;
                apply_autocmds(EVENT_FILETYPE, curbuf.b_p_ft[0], curbuf.b_fname, true, curbuf);
            }
        }

        if (varp == p_mouse)
        {
            if (p_mouse[0].at(0) == NUL)
                mch_setmouse(false);    /* switch mouse off */
            else
                setmouse();             /* in case 'mouse' changed */
        }

        if (curwin.w_curswant != MAXCOL && (vimoptions[opt_idx].flags[0] & (P_CURSWANT | P_RALL)) != 0)
            curwin.w_set_curswant = true;
        check_redraw(vimoptions[opt_idx].flags[0]);

        return errmsg;
    }

    /*
     * Handle setting 'colorcolumn' or 'textwidth' in window "wp".
     * Returns error message, null if it's OK.
     */
    /*private*/ static Bytes check_colorcolumn(window_C wp)
    {
        int count = 0;
        int[] color_cols = new int[256];

        if (wp.w_buffer == null)
            return null;    /* buffer was closed */

        for (Bytes s = wp.w_onebuf_opt.wo_cc[0]; s.at(0) != NUL && count < 255; )
        {
            skip:
            {
                int col;
                if (s.at(0) == (byte)'-' || s.at(0) == (byte)'+')
                {
                    /* -N and +N: add to 'textwidth' */
                    col = (s.at(0) == (byte)'-') ? -1 : 1;
                    s = s.plus(1);
                    if (!asc_isdigit(s.at(0)))
                        return e_invarg;
                    { Bytes[] __ = { s }; col *= getdigits(__); s = __[0]; }
                    if (wp.w_buffer.b_p_tw[0] == 0)
                        break skip;  /* 'textwidth' not set, skip this item */
                    col += wp.w_buffer.b_p_tw[0];
                    if (col < 0)
                        break skip;
                }
                else if (asc_isdigit(s.at(0)))
                {
                    Bytes[] __ = { s }; col = (int)getdigits(__); s = __[0];
                }
                else
                    return e_invarg;
                color_cols[count++] = col - 1;  /* 1-based to 0-based */
            }

            if (s.at(0) == NUL)
                break;
            if (s.at(0) != (byte)',')
                return e_invarg;
            if ((s = s.plus(1)).at(0) == NUL)
                return e_invarg;    /* illegal trailing comma as in "set cc=80," */
        }

        if (count == 0)
            wp.w_p_cc_cols = null;
        else
        {
            wp.w_p_cc_cols = new int[count + 1];

            /* sort the columns for faster usage on screen redraw inside win_line() */
            Arrays.sort(color_cols, 0, count);

            int j = 0;
            for (int i = 0; i < count; i++)
            {
                /* skip duplicates */
                if (j == 0 || wp.w_p_cc_cols[j - 1] != color_cols[i])
                    wp.w_p_cc_cols[j++] = color_cols[i];
            }
            wp.w_p_cc_cols[j] = -1;     /* end marker */
        }

        return null;    /* no error */
    }

    /*private*/ static final class charstab_C
    {
        int[]   cp;
        Bytes name;

        /*private*/ charstab_C(int[] cp, Bytes name)
        {
            this.cp = cp;
            this.name = name;
        }
    }

    /*private*/ static charstab_C[] filltab = new charstab_C[]
    {
        new charstab_C(fill_stl,    u8("stl")     ),
        new charstab_C(fill_stlnc,  u8("stlnc")   ),
        new charstab_C(fill_vert,   u8("vert")    ),
        new charstab_C(fill_fold,   u8("fold")    ),
        new charstab_C(fill_diff,   u8("diff")    ),
    };

    /*private*/ static charstab_C[] lcstab = new charstab_C[]
    {
        new charstab_C(lcs_eol,     u8("eol")     ),
        new charstab_C(lcs_ext,     u8("extends") ),
        new charstab_C(lcs_nbsp,    u8("nbsp")    ),
        new charstab_C(lcs_prec,    u8("precedes")),
        new charstab_C(lcs_tab2,    u8("tab")     ),
        new charstab_C(lcs_trail,   u8("trail")   ),
        new charstab_C(lcs_conceal, u8("conceal") ),
    };

    /*
     * Handle setting 'listchars' or 'fillchars'.
     * Returns error message, null if it's OK.
     */
    /*private*/ static Bytes set_chars_option(Bytes[] varp)
    {
        int c1, c2 = 0;

        charstab_C[] tab;
        int entries;
        if (varp == p_lcs)
        {
            tab = lcstab;
            entries = lcstab.length;
        }
        else
        {
            tab = filltab;
            entries = filltab.length;
        }

        /* first round: check for valid value, second round: assign values */
        for (int round = 0; round <= 1; round++)
        {
            if (0 < round)
            {
                /* After checking that the value is valid: set defaults:
                 * space for 'fillchars', NUL for 'listchars' */
                for (int i = 0; i < entries; i++)
                    if (tab[i].cp != null)
                        tab[i].cp[0] = (varp == p_lcs) ? NUL : ' ';
                if (varp == p_lcs)
                    lcs_tab1[0] = NUL;
                else
                    fill_diff[0] = '-';
            }

            for (Bytes p = varp[0]; p.at(0) != NUL; )
            {
                int i;
                for (i = 0; i < entries; i++)
                {
                    int len = strlen(tab[i].name);
                    if (STRNCMP(p, tab[i].name, len) == 0 && p.at(len) == (byte)':' && p.at(len + 1) != NUL)
                    {
                        Bytes[] s = { p.plus(len + 1) };
                        c1 = us_ptr2char_adv(s, true);
                        if (1 < utf_char2cells(c1))
                            continue;
                        if (tab[i].cp == lcs_tab2)
                        {
                            if (s[0].at(0) == NUL)
                                continue;
                            c2 = us_ptr2char_adv(s, true);
                            if (1 < utf_char2cells(c2))
                                continue;
                        }
                        if (s[0].at(0) == (byte)',' || s[0].at(0) == NUL)
                        {
                            if (round != 0)
                            {
                                if (tab[i].cp == lcs_tab2)
                                {
                                    lcs_tab1[0] = c1;
                                    lcs_tab2[0] = c2;
                                }
                                else if (tab[i].cp != null)
                                    tab[i].cp[0] = c1;
                            }
                            p = s[0];
                            break;
                        }
                    }
                }

                if (i == entries)
                    return e_invarg;
                if (p.at(0) == (byte)',')
                    p = p.plus(1);
            }
        }

        return null;        /* no error */
    }

    /*private*/ static Bytes stl__errbuf = new Bytes(80);

    /*
     * Check validity of options with the 'statusline' format.
     * Return error message or null.
     */
    /*private*/ static Bytes check_stl_option(Bytes s)
    {
        int itemcnt = 0;
        int groupdepth = 0;

        while (s.at(0) != NUL && itemcnt < STL_MAX_ITEM)
        {
            /* Check for valid keys after % sequences. */
            while (s.at(0) != NUL && s.at(0) != (byte)'%')
                s = s.plus(1);
            if (s.at(0) == NUL)
                break;
            s = s.plus(1);
            if (s.at(0) != (byte)'%' && s.at(0) != (byte)')')
                itemcnt++;
            if (s.at(0) == (byte)'%' || s.at(0) == STL_TRUNCMARK || s.at(0) == STL_MIDDLEMARK)
            {
                s = s.plus(1);
                continue;
            }
            if (s.at(0) == (byte)')')
            {
                s = s.plus(1);
                if (--groupdepth < 0)
                    break;
                continue;
            }
            if (s.at(0) == (byte)'-')
                s = s.plus(1);
            while (asc_isdigit(s.at(0)))
                s = s.plus(1);
            if (s.at(0) == STL_USER_HL)
                continue;
            if (s.at(0) == (byte)'.')
            {
                s = s.plus(1);
                while (s.at(0) != NUL && asc_isdigit(s.at(0)))
                    s = s.plus(1);
            }
            if (s.at(0) == (byte)'(')
            {
                groupdepth++;
                continue;
            }
            if (vim_strchr(STL_ALL, s.at(0)) == null)
                return illegal_char(stl__errbuf, s.at(0));
            if (s.at(0) == (byte)'{')
            {
                s = s.plus(1);
                while (s.at(0) != (byte)'}' && s.at(0) != NUL)
                    s = s.plus(1);
                if (s.at(0) != (byte)'}')
                    return u8("E540: Unclosed expression sequence");
            }
        }
        if (STL_MAX_ITEM <= itemcnt)
            return u8("E541: too many items");
        if (groupdepth != 0)
            return u8("E542: unbalanced groups");

        return null;
    }

    /*
     * Extract the items in the 'clipboard' option and set global values.
     */
    /*private*/ static Bytes check_clipboard_option()
    {
        int new_unnamed = 0;
        boolean new_autoselect_star = false;
        boolean new_autoselect_plus = false;
        boolean new_autoselectml = false;
        boolean new_html = false;
        regprog_C new_exclude_prog = null;
        Bytes errmsg = null;

        for (Bytes p = p_cb[0]; p.at(0) != NUL; )
        {
            if (STRNCMP(p, u8("unnamed"), 7) == 0 && (p.at(7) == (byte)',' || p.at(7) == NUL))
            {
                new_unnamed |= CLIP_UNNAMED;
                p = p.plus(7);
            }
            else if (STRNCMP(p, u8("unnamedplus"), 11) == 0 && (p.at(11) == (byte)',' || p.at(11) == NUL))
            {
                new_unnamed |= CLIP_UNNAMED_PLUS;
                p = p.plus(11);
            }
            else if (STRNCMP(p, u8("autoselect"), 10) == 0 && (p.at(10) == (byte)',' || p.at(10) == NUL))
            {
                new_autoselect_star = true;
                p = p.plus(10);
            }
            else if (STRNCMP(p, u8("autoselectplus"), 14) == 0 && (p.at(14) == (byte)',' || p.at(14) == NUL))
            {
                new_autoselect_plus = true;
                p = p.plus(14);
            }
            else if (STRNCMP(p, u8("autoselectml"), 12) == 0 && (p.at(12) == (byte)',' || p.at(12) == NUL))
            {
                new_autoselectml = true;
                p = p.plus(12);
            }
            else if (STRNCMP(p, u8("html"), 4) == 0 && (p.at(4) == (byte)',' || p.at(4) == NUL))
            {
                new_html = true;
                p = p.plus(4);
            }
            else if (STRNCMP(p, u8("exclude:"), 8) == 0 && new_exclude_prog == null)
            {
                p = p.plus(8);
                new_exclude_prog = vim_regcomp(p, RE_MAGIC);
                if (new_exclude_prog == null)
                    errmsg = e_invarg;
                break;
            }
            else
            {
                errmsg = e_invarg;
                break;
            }
            if (p.at(0) == (byte)',')
                p = p.plus(1);
        }

        if (errmsg == null)
        {
            clip_unnamed = new_unnamed;
            clip_autoselect_star = new_autoselect_star;
            clip_autoselect_plus = new_autoselect_plus;
            clip_autoselectml = new_autoselectml;
            clip_html = new_html;
            clip_exclude_prog = new_exclude_prog;
        }

        return errmsg;
    }

    /*
     * Set the scriptID for an option, taking care of setting the buffer- or window-local value.
     */
    /*private*/ static void set_option_scriptID_idx(int opt_idx, int opt_flags, int id)
    {
        boolean both = (opt_flags & (OPT_LOCAL | OPT_GLOBAL)) == 0;
        int indir = vimoptions[opt_idx].indir;

        /* Remember where the option was set.
         * For local options need to do that in the buffer or window structure. */
        if (both || (opt_flags & OPT_GLOBAL) != 0 || (indir & (PV_BUF|PV_WIN)) == 0)
            vimoptions[opt_idx].scriptID = id;
        if (both || (opt_flags & OPT_LOCAL) != 0)
        {
            if ((indir & PV_BUF) != 0)
                curbuf.b_p_scriptID[indir & PV_MASK] = id;
            else if ((indir & PV_WIN) != 0)
                curwin.w_onebuf_opt.wo_scriptID[indir & PV_MASK] = id;
        }
    }

    /*
     * Set the value of a boolean option, and take care of side effects.
     * Returns null for success, or an error message for an error.
     */
    /*private*/ static Bytes set_bool_option(int opt_idx, boolean[] varp, boolean value, int opt_flags)
        /* opt_idx: index in vimoptions[] table */
        /* varp: pointer to the option variable */
        /* value: new value */
        /* opt_flags: OPT_LOCAL and/or OPT_GLOBAL */
    {
        /* Disallow changing some options from secure mode. */
        if ((secure != 0 || sandbox != 0) && (vimoptions[opt_idx].flags[0] & P_SECURE) != 0)
            return e_secure;

        boolean old_value = varp[0];
        varp[0] = value;              /* set the new value */

        /* Remember where the option was set. */
        set_option_scriptID_idx(opt_idx, opt_flags, current_SID);

        /* May set global value for local option. */
        if ((opt_flags & (OPT_LOCAL | OPT_GLOBAL)) == 0)
            ((boolean[])get_varp_scope(vimoptions[opt_idx], OPT_GLOBAL))[0] = value;

        /*
         * Handle side effects of changing a bool option.
         */

        /* 'undofile' */
        if (varp == curbuf.b_p_udf || varp == p_udf)
        {
            /* Only take action when the option was set.  When reset we do not
             * delete the undo file, the option may be set again without making
             * any changes in between. */
            if (curbuf.b_p_udf[0] || p_udf[0])
            {
                Bytes hash = new Bytes(UNDO_HASH_SIZE);
                buffer_C save_curbuf = curbuf;

                for (curbuf = firstbuf; curbuf != null; curbuf = curbuf.b_next)
                {
                    /* When 'undofile' is set globally: for every buffer, otherwise
                     * only for the current buffer: Try to read in the undofile,
                     * if one exists, the buffer wasn't changed and the buffer was loaded */
                    if ((curbuf == save_curbuf || (opt_flags & OPT_GLOBAL) != 0 || opt_flags == 0)
                            && !curbufIsChanged() && curbuf.b_ml.ml_mfp != null)
                    {
                        u_compute_hash(hash);
                        u_read_undo(null, hash, curbuf.b_fname);
                    }
                }
                curbuf = save_curbuf;
            }
        }

        else if (varp == curbuf.b_p_ro)
        {
            /* when 'readonly' is reset globally, also reset readonlymode */
            if (!curbuf.b_p_ro[0] && (opt_flags & OPT_LOCAL) == 0)
                readonlymode = false;

            /* when 'readonly' is set may give W10 again */
            if (curbuf.b_p_ro[0])
                curbuf.b_did_warn = false;

            redraw_titles();
        }

        /* when 'modifiable' is changed, redraw the window title */
        else if (varp == curbuf.b_p_ma)
        {
            redraw_titles();
        }
        /* when 'endofline' is changed, redraw the window title */
        else if (varp == curbuf.b_p_eol)
        {
            redraw_titles();
        }
        /* when 'bomb' is changed, redraw the window title and tab page text */
        else if (varp == curbuf.b_p_bomb)
        {
            redraw_titles();
        }

        /* when 'bin' is set also set some other options */
        else if (varp == curbuf.b_p_bin)
        {
            set_options_bin(old_value, curbuf.b_p_bin[0], opt_flags);
            redraw_titles();
        }

        /* when 'buflisted' changes, trigger autocommands */
        else if (varp == curbuf.b_p_bl && old_value != curbuf.b_p_bl[0])
        {
            apply_autocmds(curbuf.b_p_bl[0] ? EVENT_BUFADD : EVENT_BUFDELETE, null, null, true, curbuf);
        }

        /* when 'terse' is set change 'shortmess' */
        else if (varp == p_terse)
        {
            Bytes p = vim_strchr(p_shm[0], SHM_SEARCH);

            /* insert 's' in "p_shm" */
            if (p_terse[0] && p == null)
            {
                STRCPY(ioBuff, p_shm[0]);
                STRCAT(ioBuff, u8("s"));
                set_string_option_direct(u8("shm"), -1, ioBuff, OPT_FREE, 0);
            }
            /* remove 's' from "p_shm" */
            else if (!p_terse[0] && p != null)
                BCOPY(p, 0, p, 1, strlen(p, 1) + 1);
        }

        /* when 'paste' is set or reset also change other options */
        else if (varp == p_paste)
        {
            paste_option_changed();
        }

        /* when 'insertmode' is set from an autocommand need to do work here */
        else if (varp == p_im)
        {
            if (p_im[0])
            {
                if ((State & INSERT) == 0)
                    need_start_insertmode = true;
                stop_insert_mode = false;
            }
            else
            {
                need_start_insertmode = false;
                stop_insert_mode = true;
                if (restart_edit != 0 && mode_displayed)
                    clear_cmdline = true;   /* remove "(insert)" */
                restart_edit = 0;
            }
        }

        /* when 'ignorecase' is set or reset and 'hlsearch' is set, redraw */
        else if (varp == p_ic && p_hls[0])
        {
            redraw_all_later(SOME_VALID);
        }

        /* when 'hlsearch' is set or reset: reset no_hlsearch */
        else if (varp == p_hls)
        {
            no_hlsearch = false;
            set_vim_var_nr(VV_HLSEARCH, (!no_hlsearch && p_hls[0]) ? 1 : 0);
        }

        /* when 'scrollbind' is set:
         * snapshot the current position to avoid a jump at the end of normal_cmd() */
        else if (varp == curwin.w_onebuf_opt.wo_scb)
        {
            if (curwin.w_onebuf_opt.wo_scb[0])
            {
                do_check_scrollbind(false);
                curwin.w_scbind_pos = curwin.w_topline;
            }
        }

        /* when 'textmode' is set or reset also change 'fileformat' */
        else if (varp == curbuf.b_p_tx)
        {
            set_fileformat(curbuf.b_p_tx[0] ? EOL_DOS : EOL_UNIX, opt_flags);
        }

        /*
         * When 'lisp' option changes include/exclude '-' in
         * keyword characters.
         */
        else if (varp == curbuf.b_p_lisp)
        {
            buf_init_chartab(curbuf, false);        /* ignore errors */
        }

        else if (varp == curbuf.b_changed)
        {
            if (!value)
                save_file_ff(curbuf);               /* buffer is unchanged */
            redraw_titles();
            modified_was_set = value;
        }

        /* If 'wrap' is set, set w_leftcol to zero. */
        else if (varp == curwin.w_onebuf_opt.wo_wrap)
        {
            if (curwin.w_onebuf_opt.wo_wrap[0])
                curwin.w_leftcol = 0;
        }

        else if (varp == p_ea)
        {
            if (p_ea[0] && !old_value)
                win_equal(curwin, false, 0);
        }

        else if (varp == p_wiv)
        {
            /*
             * When 'weirdinvert' changed, set/reset 't_xs'.
             * Then set 'weirdinvert' according to value of 't_xs'.
             */
            if (p_wiv[0] && !old_value)
                T_XS[0] = u8("y");
            else if (!p_wiv[0] && old_value)
                T_XS[0] = EMPTY_OPTION;
            p_wiv[0] = (T_XS[0].at(0) != NUL);
        }

        /*
         * End of handling side effects for bool options.
         */

        vimoptions[opt_idx].flags[0] |= P_WAS_SET;

        comp_col();                     /* in case 'ruler' or 'showcmd' changed */
        if (curwin.w_curswant != MAXCOL && (vimoptions[opt_idx].flags[0] & (P_CURSWANT | P_RALL)) != 0)
            curwin.w_set_curswant = true;
        check_redraw(vimoptions[opt_idx].flags[0]);

        return null;
    }

    /*
     * Set the value of a number option, and take care of side effects.
     * Returns null for success, or an error message for an error.
     */
    /*private*/ static Bytes set_num_option(int opt_idx, long[] varp, long value, Bytes errbuf, int errbuflen, int opt_flags)
        /* opt_idx: index in vimoptions[] table */
        /* varp: pointer to the option variable */
        /* value: new value */
        /* errbuf: buffer for error messages */
        /* errbuflen: length of "errbuf" */
        /* opt_flags: OPT_LOCAL, OPT_GLOBAL and OPT_MODELINE */
    {
        Bytes errmsg = null;
        long old_value = varp[0];
        long old_Rows = Rows[0];               /* remember old Rows */
        long old_Columns = Columns[0];         /* remember old Columns */

        /* Disallow changing some options from secure mode. */
        if ((secure != 0 || sandbox != 0) && (vimoptions[opt_idx].flags[0] & P_SECURE) != 0)
            return e_secure;

        varp[0] = value;
        /* Remember where the option was set. */
        set_option_scriptID_idx(opt_idx, opt_flags, current_SID);

        if (curbuf.b_p_sw[0] < 0)
        {
            errmsg = e_positive;
            curbuf.b_p_sw[0] = curbuf.b_p_ts[0];
        }

        /*
         * Number options that need some action when changed.
         */
        if (varp == p_wh)
        {
            if (p_wh[0] < 1)
            {
                errmsg = e_positive;
                p_wh[0] = 1;
            }
            if (p_wh[0] < p_wmh[0])
            {
                errmsg = e_winheight;
                p_wh[0] = p_wmh[0];
            }

            /* Change window height NOW. */
            if (lastwin != firstwin)
            {
                if (varp == p_wh && curwin.w_height < p_wh[0])
                    win_setheight((int)p_wh[0]);
            }
        }

        /* 'winminheight' */
        else if (varp == p_wmh)
        {
            if (p_wmh[0] < 0)
            {
                errmsg = e_positive;
                p_wmh[0] = 0;
            }
            if (p_wh[0] < p_wmh[0])
            {
                errmsg = e_winheight;
                p_wmh[0] = p_wh[0];
            }
            win_setminheight();
        }

        else if (varp == p_wiw)
        {
            if (p_wiw[0] < 1)
            {
                errmsg = e_positive;
                p_wiw[0] = 1;
            }
            if (p_wiw[0] < p_wmw[0])
            {
                errmsg = e_winwidth;
                p_wiw[0] = p_wmw[0];
            }

            /* Change window width NOW. */
            if (lastwin != firstwin && curwin.w_width < p_wiw[0])
                win_setwidth((int)p_wiw[0]);
        }

        /* 'winminwidth' */
        else if (varp == p_wmw)
        {
            if (p_wmw[0] < 0)
            {
                errmsg = e_positive;
                p_wmw[0] = 0;
            }
            if (p_wiw[0] < p_wmw[0])
            {
                errmsg = e_winwidth;
                p_wmw[0] = p_wiw[0];
            }
            win_setminheight();
        }

        /* (re)set last window status line */
        else if (varp == p_ls)
        {
            last_status(false);
        }

        /* (re)set tab page line */
        else if (varp == p_stal)
        {
            shell_new_rows();       /* recompute window positions and heights */
        }

        /* 'shiftwidth' or 'tabstop' */
        else if (varp == curbuf.b_p_sw || varp == curbuf.b_p_ts)
        {
            /* When 'shiftwidth' changes, or it's zero and 'tabstop' changes:
             * parse 'cinoptions'. */
            if (varp == curbuf.b_p_sw || curbuf.b_p_sw[0] == 0)
                parse_cino(curbuf);
        }

        /* 'maxcombine' */
        else if (varp == p_mco)
        {
            if (MAX_MCO < p_mco[0])
                p_mco[0] = MAX_MCO;
            else if (p_mco[0] < 0)
                p_mco[0] = 0;
            screenclear();      /* will re-allocate the screen */
        }

        else if (varp == curbuf.b_p_iminsert)
        {
            if (curbuf.b_p_iminsert[0] < 0 || B_IMODE_LAST < curbuf.b_p_iminsert[0])
            {
                errmsg = e_invarg;
                curbuf.b_p_iminsert[0] = B_IMODE_NONE;
            }
            p_iminsert[0] = curbuf.b_p_iminsert[0];
            if (termcap_active)     /* don't do this in the alternate screen */
                showmode();
        }

        else if (varp == p_window)
        {
            if (p_window[0] < 1)
                p_window[0] = 1;
            else if (Rows[0] <= p_window[0])
                p_window[0] = Rows[0] - 1;
        }

        else if (varp == curbuf.b_p_imsearch)
        {
            if (curbuf.b_p_imsearch[0] < -1 || B_IMODE_LAST < curbuf.b_p_imsearch[0])
            {
                errmsg = e_invarg;
                curbuf.b_p_imsearch[0] = B_IMODE_NONE;
            }
            p_imsearch[0] = curbuf.b_p_imsearch[0];
        }

        /* if "p_ch" changed value, change the command line height */
        else if (varp == p_ch)
        {
            if (p_ch[0] < 1)
            {
                errmsg = e_positive;
                p_ch[0] = 1;
            }
            int min = min_rows();
            if (p_ch[0] > Rows[0] - min + 1)
                p_ch[0] = Rows[0] - min + 1;

            /* Only compute the new window layout when startup has been completed,
             * otherwise the frame sizes may be wrong. */
            if (p_ch[0] != old_value && full_screen)
                command_height();
        }

        else if (varp == curwin.w_onebuf_opt.wo_cole)
        {
            if (curwin.w_onebuf_opt.wo_cole[0] < 0)
            {
                errmsg = e_positive;
                curwin.w_onebuf_opt.wo_cole[0] = 0;
            }
            else if (3 < curwin.w_onebuf_opt.wo_cole[0])
            {
                errmsg = e_invarg;
                curwin.w_onebuf_opt.wo_cole[0] = 3;
            }
        }

        /* sync undo before 'undolevels' changes */
        else if (varp == p_ul)
        {
            /* use the old value, otherwise u_sync() may not work properly */
            p_ul[0] = old_value;
            u_sync(true);
            p_ul[0] = value;
        }
        else if (varp == curbuf.b_p_ul)
        {
            /* use the old value, otherwise u_sync() may not work properly */
            curbuf.b_p_ul[0] = old_value;
            u_sync(true);
            curbuf.b_p_ul[0] = value;
        }

        /* 'numberwidth' must be positive */
        else if (varp == curwin.w_onebuf_opt.wo_nuw)
        {
            if (curwin.w_onebuf_opt.wo_nuw[0] < 1)
            {
                errmsg = e_positive;
                curwin.w_onebuf_opt.wo_nuw[0] = 1;
            }
            if (10 < curwin.w_onebuf_opt.wo_nuw[0])
            {
                errmsg = e_invarg;
                curwin.w_onebuf_opt.wo_nuw[0] = 10;
            }
            curwin.w_nrwidth_line_count = 0; /* trigger a redraw */
        }

        else if (varp == curbuf.b_p_tw)
        {
            if (curbuf.b_p_tw[0] < 0)
            {
                errmsg = e_positive;
                curbuf.b_p_tw[0] = 0;
            }

            for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
                for (window_C wp = (tp == curtab) ? firstwin : tp.tp_firstwin; wp != null; wp = wp.w_next)
                    check_colorcolumn(wp);
        }

        /*
         * Check the bounds for numeric options here.
         */
        if (full_screen)
        {
            int min = min_rows();
            if (Rows[0] < min)
            {
                if (errbuf != null)
                {
                    vim_snprintf(errbuf, errbuflen, u8("E593: Need at least %d lines"), min);
                    errmsg = errbuf;
                }
                Rows[0] = min;
            }
            if ((int)Columns[0] < MIN_COLUMNS)
            {
                if (errbuf != null)
                {
                    vim_snprintf(errbuf, errbuflen, u8("E594: Need at least %d columns"), MIN_COLUMNS);
                    errmsg = errbuf;
                }
                Columns[0] = MIN_COLUMNS;
            }
        }
        limit_screen_size();

        /*
         * If the screen (shell) height has been changed, assume it is the physical screenheight.
         */
        if (old_Rows != Rows[0] || old_Columns != Columns[0])
        {
            /* Changing the screen size is not allowed while updating the screen. */
            if (updating_screen)
                varp[0] = old_value;
            else if (full_screen)
                set_shellsize((int)Columns[0], (int)Rows[0], true);
            else
            {
                /* Postpone the resizing; check the size and cmdline position for messages. */
                check_shellsize();
                if (cmdline_row > Rows[0] - p_ch[0] && p_ch[0] < Rows[0])
                    cmdline_row = (int)(Rows[0] - p_ch[0]);
            }
            if (Rows[0] <= p_window[0] || !option_was_set(u8("window")))
                p_window[0] = Rows[0] - 1;
        }

        if (curbuf.b_p_ts[0] <= 0)
        {
            errmsg = e_positive;
            curbuf.b_p_ts[0] = 8;
        }
        if (p_tm[0] < 0)
        {
            errmsg = e_positive;
            p_tm[0] = 0;
        }
        if ((curwin.w_onebuf_opt.wo_scr[0] <= 0 || (curwin.w_height < curwin.w_onebuf_opt.wo_scr[0] && 0 < curwin.w_height))
                && full_screen)
        {
            if (varp == curwin.w_onebuf_opt.wo_scr)
            {
                if (curwin.w_onebuf_opt.wo_scr[0] != 0)
                    errmsg = e_scroll;
                win_comp_scroll(curwin);
            }
            /* If 'scroll' became invalid because of a side effect silently adjust it. */
            else if (curwin.w_onebuf_opt.wo_scr[0] <= 0)
                curwin.w_onebuf_opt.wo_scr[0] = 1;
            else /* curwin.w_onebuf_opt.wo_scr[0] > curwin.w_height */
                curwin.w_onebuf_opt.wo_scr[0] = curwin.w_height;
        }
        if (p_hi[0] < 0)
        {
            errmsg = e_positive;
            p_hi[0] = 0;
        }
        else if (10000 < p_hi[0])
        {
            errmsg = e_invarg;
            p_hi[0] = 10000;
        }
        if (p_re[0] < 0 || 2 < p_re[0])
        {
            errmsg = e_invarg;
            p_re[0] = 0;
        }
        if (p_report[0] < 0)
        {
            errmsg = e_positive;
            p_report[0] = 1;
        }
        if ((p_sj[0] < -100 || Rows[0] <= p_sj[0]) && full_screen)
        {
            if (Rows[0] != old_Rows)   /* Rows changed, just adjust "p_sj" */
                p_sj[0] = Rows[0] / 2;
            else
            {
                errmsg = e_scroll;
                p_sj[0] = 1;
            }
        }
        if (p_so[0] < 0 && full_screen)
        {
            errmsg = e_scroll;
            p_so[0] = 0;
        }
        if (p_siso[0] < 0 && full_screen)
        {
            errmsg = e_positive;
            p_siso[0] = 0;
        }
        if (p_cwh[0] < 1)
        {
            errmsg = e_positive;
            p_cwh[0] = 1;
        }
        if (p_ut[0] < 0)
        {
            errmsg = e_positive;
            p_ut[0] = 2000;
        }
        if (p_ss[0] < 0)
        {
            errmsg = e_positive;
            p_ss[0] = 0;
        }

        /* May set global value for local option. */
        if ((opt_flags & (OPT_LOCAL | OPT_GLOBAL)) == 0)
            ((long[])get_varp_scope(vimoptions[opt_idx], OPT_GLOBAL))[0] = varp[0];

        vimoptions[opt_idx].flags[0] |= P_WAS_SET;

        comp_col();                     /* in case 'columns' or 'ls' changed */
        if (curwin.w_curswant != MAXCOL && (vimoptions[opt_idx].flags[0] & (P_CURSWANT | P_RALL)) != 0)
            curwin.w_set_curswant = true;
        check_redraw(vimoptions[opt_idx].flags[0]);

        return errmsg;
    }

    /*
     * Called after an option changed: check if something needs to be redrawn.
     */
    /*private*/ static void check_redraw(long flags)
    {
        /* Careful: P_RCLR and P_RALL are a combination of other P_ flags. */
        boolean doclear = ((flags & P_RCLR) == P_RCLR);
        boolean all = ((flags & P_RALL) == P_RALL || doclear);

        if ((flags & P_RSTAT) != 0 || all)      /* mark all status lines dirty */
            status_redraw_all();

        if ((flags & P_RBUF) != 0 || (flags & P_RWIN) != 0 || all)
            changed_window_setting();
        if ((flags & P_RBUF) != 0)
            redraw_curbuf_later(NOT_VALID);
        if (doclear)
            redraw_all_later(CLEAR);
        else if (all)
            redraw_all_later(NOT_VALID);
    }

    /*private*/ static short[] quick_tab = new short[27];   /* quick access table */

    /*
     * Find index for option 'arg'.
     * Return -1 if not found.
     */
    /*private*/ static int findoption(Bytes arg)
    {
        Bytes s;

        /*
         * For first call: Initialize the quick-access table.
         * It contains the index for the first option that starts with a certain letter.
         * There are 26 letters, plus the first "t_" option.
         */
        if (quick_tab[1] == 0)
        {
            Bytes p = vimoptions[0].fullname;
            for (int opt_idx = 1; (s = vimoptions[opt_idx].fullname) != null; opt_idx++)
            {
                if (s.at(0) != p.at(0))
                {
                    if (s.at(0) == (byte)'t' && s.at(1) == (byte)'_')
                        quick_tab[26] = (short)opt_idx;
                    else
                        quick_tab[charOrdLow(s.at(0))] = (short)opt_idx;
                }
                p = s;
            }
        }

        /*
         * Check for name starting with an illegal character.
         */
        if (arg.at(0) < 'a' || 'z' < arg.at(0))
            return -1;

        int opt_idx;
        boolean is_term_opt = (arg.at(0) == (byte)'t' && arg.at(1) == (byte)'_');
        if (is_term_opt)
            opt_idx = quick_tab[26];
        else
            opt_idx = quick_tab[charOrdLow(arg.at(0))];
        for ( ; (s = vimoptions[opt_idx].fullname) != null; opt_idx++)
        {
            if (STRCMP(arg, s) == 0)                    /* match full name */
                break;
        }
        if (s == null && !is_term_opt)
        {
            opt_idx = quick_tab[charOrdLow(arg.at(0))];
            for ( ; vimoptions[opt_idx].fullname != null; opt_idx++)
            {
                s = vimoptions[opt_idx].shortname;
                if (s != null && STRCMP(arg, s) == 0)   /* match short name */
                    break;
                s = null;
            }
        }
        if (s == null)
            opt_idx = -1;
        return opt_idx;
    }

    /*
     * Get the value for an option.
     *
     * Returns:
     * Number or Toggle option: 1, numval[0] gets value.
     *           String option: 0, stringval[0] gets allocated string.
     * Hidden Number or Toggle option: -1.
     *           hidden String option: -2.
     *                 unknown option: -3.
     */
    /*private*/ static int get_option_value(Bytes name, long[] numval, Bytes[] stringval, int opt_flags)
        /* stringval: null when only checking existence */
    {
        int opt_idx = findoption(name);
        if (opt_idx < 0)                /* unknown option */
            return -3;

        Object varp = get_varp_scope(vimoptions[opt_idx], opt_flags);

        if ((vimoptions[opt_idx].flags[0] & P_STRING) != 0)
        {
            if (varp == null)           /* hidden option */
                return -2;
            if (stringval != null)
                stringval[0] = STRDUP(((Bytes[])varp)[0]);
            return 0;
        }

        if (varp == null)               /* hidden option */
            return -1;
        if ((vimoptions[opt_idx].flags[0] & P_NUM) != 0)
            numval[0] = ((long[])varp)[0];
        else
        {
            /* Special case: 'modified' is "b_changed",
             * but we also want to consider it set when 'ff' or 'fenc' changed. */
            if (varp == curbuf.b_changed)
                numval[0] = curbufIsChanged() ? TRUE : FALSE;
            else
                numval[0] = ((int[])varp)[0];
        }
        return 1;
    }

    /*
     * Set the value of option "name".
     * Use "string" for string options, use "number" for other options.
     *
     * Returns null on success or error message on error.
     */
    /*private*/ static Bytes set_option_value(Bytes name, long number, Bytes string, int opt_flags)
        /* opt_flags: OPT_LOCAL or 0 (both) */
    {
        int opt_idx = findoption(name);
        if (opt_idx < 0)
        {
            emsg2(u8("E355: Unknown option: %s"), name);
            return null;
        }

        long flags = vimoptions[opt_idx].flags[0];

        /* Disallow changing some options in the sandbox. */
        if (0 < sandbox && (flags & P_SECURE) != 0)
        {
            emsg(e_sandbox);
            return null;
        }

        if ((flags & P_STRING) != 0)
            return set_string_option(opt_idx, string, opt_flags);

        Object varp = get_varp_scope(vimoptions[opt_idx], opt_flags);
        if (varp != null)   /* hidden option is not changed */
        {
            if (number == 0 && string != null)
            {
                int idx;

                /* Either we are given a string or we are setting option to zero. */
                for (idx = 0; string.at(idx) == (byte)'0'; idx++)
                    ;
                if (string.at(idx) != NUL || idx == 0)
                {
                    /* There's another character after zeros or the string is empty.
                     * In both cases, we are trying to set a num option using a string. */
                    emsg3(u8("E521: Number required: &%s = '%s'"), name, string);
                    return null;        /* do nothing as we hit an error */
                }
            }

            if ((flags & P_NUM) != 0)
                return set_num_option(opt_idx, (long[])varp, number, null, 0, opt_flags);
            else
                return set_bool_option(opt_idx, (boolean[])varp, (number != 0), opt_flags);
        }

        return null;
    }

    /*
     * Get the terminal code for a terminal option.
     * Returns null when not found.
     */
    /*private*/ static Bytes get_term_code(Bytes tname)
    {
        if (tname.at(0) != (byte)'t' || tname.at(1) != (byte)'_' || tname.at(2) == NUL || tname.at(3) == NUL)
            return null;
        int opt_idx = findoption(tname);
        if (0 <= opt_idx)
        {
            Object varp = get_varp(vimoptions[opt_idx], false);
            return (varp != null) ? ((Bytes[])varp)[0] : null;
        }
        return find_termcode(tname.plus(2));
    }

    /*private*/ static Bytes get_highlight_default()
    {
        int i = findoption(u8("hl"));
        if (i < 0)
            return null;

        return (Bytes)vimoptions[i].def_val;
    }

    /*
     * Translate a string like "t_xx", "<t_xx>" or "<S-Tab>" to a key number.
     */
    /*private*/ static int find_key_option(Bytes _arg)
    {
        Bytes[] arg = { _arg };
        int key;

        /*
         * Don't use get_special_key_code() for t_xx, we don't want it to call add_termcap_entry().
         */
        if (arg[0].at(0) == (byte)'t' && arg[0].at(1) == (byte)'_' && arg[0].at(2) != NUL && arg[0].at(3) != NUL)
            key = TERMCAP2KEY(arg[0].at(2), arg[0].at(3));
        else
        {
            arg[0] = arg[0].minus(1);                      /* put "arg" at the '<' */
            int[] modifiers = { 0 };
            key = find_special_key(arg, modifiers, true, true);
            if (modifiers[0] != 0)              /* can't handle modifiers here */
                key = 0;
        }

        return key;
    }

    /*
     * if 'all' == 0: show changed options
     * if 'all' == 1: show all normal options
     * if 'all' == 2: show all terminal options
     */
    /*private*/ static void showoptions(int all, int opt_flags)
        /* opt_flags: OPT_LOCAL and/or OPT_GLOBAL */
    {
        final int INC = 20, GAP = 3;

        vimoption_C[] items = new vimoption_C[vimoptions.length];

        /* Highlight title. */
        if (all == 2)
            msg_puts_title(u8("\n--- Terminal codes ---"));
        else if ((opt_flags & OPT_GLOBAL) != 0)
            msg_puts_title(u8("\n--- Global option values ---"));
        else if ((opt_flags & OPT_LOCAL) != 0)
            msg_puts_title(u8("\n--- Local option values ---"));
        else
            msg_puts_title(u8("\n--- Options ---"));

        /*
         * do the loop two times:
         * 1. display the short items
         * 2. display the long items (only strings and numbers)
         */
        for (int run = 1; run <= 2 && !got_int; run++)
        {
            /*
             * collect the items in items[]
             */
            int item_count = 0;
            for (int i = 0; vimoptions[i].fullname != null; i++)
            {
                boolean isterm = istermoption(vimoptions[i]);

                Object varp = null;
                if (opt_flags != 0)
                {
                    if (vimoptions[i].indir != PV_NONE && !isterm)
                        varp = get_varp_scope(vimoptions[i], opt_flags);
                }
                else
                    varp = get_varp(vimoptions[i], false);

                if (varp != null
                        && ((all == 2 && isterm)
                            || (all == 1 && !isterm)
                            || (all == 0 && !optval_default(vimoptions[i], varp))))
                {
                    int len;
                    if ((vimoptions[i].flags[0] & P_BOOL) != 0)
                        len = 1;            /* a toggle option fits always */
                    else
                    {
                        option_value2string(vimoptions[i], opt_flags);
                        len = strlen(vimoptions[i].fullname) + mb_string2cells(nameBuff, -1) + 1;
                    }
                    if ((len <= INC - GAP && run == 1) || (INC - GAP < len && run == 2))
                        items[item_count++] = vimoptions[i];
                }
            }

            /*
             * display the items
             */
            int rows;
            if (run == 1)
            {
                int cols = ((int)Columns[0] + GAP - 3) / INC;
                if (cols == 0)
                    cols = 1;
                rows = (item_count + cols - 1) / cols;
            }
            else    /* run == 2 */
                rows = item_count;

            for (int row = 0; row < rows && !got_int; row++)
            {
                msg_putchar('\n');                      /* go to next line */
                if (got_int)                            /* 'q' typed in more */
                    break;
                int col = 0;
                for (int i = row; i < item_count; i += rows)
                {
                    msg_col = col;                      /* make columns */
                    showoneopt(items[i], opt_flags);
                    col += INC;
                }
                out_flush();
                ui_breakcheck();
            }
        }
    }

    /*
     * Return true if option "p" has its default value.
     */
    /*private*/ static boolean optval_default(vimoption_C v, Object varp)
    {
        if (varp == null)
            return true;        /* hidden option is always at default */
        if ((v.flags[0] & P_NUM) != 0)
            return (((long[])varp)[0] == (long)v.def_val);
        if ((v.flags[0] & P_BOOL) != 0)
            return (((boolean[])varp)[0] == (boolean)v.def_val);
        if ((v.flags[0] & P_STRING) != 0)
            return (STRCMP(((Bytes[])varp)[0], (Bytes)v.def_val) == 0);

        throw new IllegalArgumentException("invalid option type");
    }

    /*
     * showoneopt: show the value of one option
     * must not be called with a hidden option!
     */
    /*private*/ static void showoneopt(vimoption_C v, int opt_flags)
        /* opt_flags: OPT_LOCAL or OPT_GLOBAL */
    {
        boolean save_silent = silent_mode;

        silent_mode = false;
        info_message = true;

        Object varp = get_varp_scope(v, opt_flags);

        /* for 'modified' we also need to check if 'ff' or 'fenc' changed. */
        if ((v.flags[0] & P_BOOL) != 0 && ((varp == curbuf.b_changed) ? !curbufIsChanged() : !((boolean[])varp)[0]))
            msg_puts(u8("no"));
     /* else if ((v.flags[0] & P_BOOL) != 0 && ((boolean[])varp)[0] < 0)
            msg_puts(u8("--")); */
        else
            msg_puts(u8("  "));
        msg_puts(v.fullname);
        if ((v.flags[0] & P_BOOL) == 0)
        {
            msg_putchar('=');
            /* put value string in nameBuff */
            option_value2string(v, opt_flags);
            msg_outtrans(nameBuff);
        }

        silent_mode = save_silent;
        info_message = false;
    }

    /*
     * Clear all the terminal options.
     * If the option has been allocated, free the memory.
     * Terminal options are never hidden or indirect.
     */
    /*private*/ static void clear_termoptions()
    {
        /*
         * Reset a few things before clearing the old options.  This may cause
         * outputting a few things that the terminal doesn't understand, but the
         * screen will be cleared later, so this is OK.
         */
        mch_setmouse(false);            /* switch mouse off */

        stoptermcap();                  /* stop termcap mode */

        free_termoptions();
    }

    /*private*/ static void free_termoptions()
    {
        for (int i = 0; vimoptions[i].fullname != null; i++)
            if (istermoption(vimoptions[i]))
            {
                ((Bytes[])vimoptions[i].var)[0] = EMPTY_OPTION;
                vimoptions[i].def_val = EMPTY_OPTION;
            }

        clear_termcodes();
    }

    /*
     * Free the string for one term option, if it was allocated.
     * Set the string to EMPTY_OPTION and clear allocated flag.
     * "var" points to the option value.
     */
    /*private*/ static void free_one_termoption(Bytes[] varp)
    {
        for (int i = 0; vimoptions[i].fullname != null; i++)
            if (vimoptions[i].var == varp)
            {
                ((Bytes[])vimoptions[i].var)[0] = EMPTY_OPTION;
                break;
            }
    }

    /*
     * Set the terminal option defaults to the current value.
     * Used after setting the terminal name.
     */
    /*private*/ static void set_term_defaults()
    {
        for (int i = 0; vimoptions[i].fullname != null; i++)
        {
            if (istermoption(vimoptions[i]) && vimoptions[i].def_val != ((Bytes[])vimoptions[i].var)[0])
                vimoptions[i].def_val = ((Bytes[])vimoptions[i].var)[0];
        }
    }

    /*
     * return true if 'v' starts with 't_'
     */
    /*private*/ static boolean istermoption(vimoption_C v)
    {
        return (v.fullname.at(0) == (byte)'t' && v.fullname.at(1) == (byte)'_');
    }

    /*
     * Compute columns for ruler and shown command. 'sc_col' is also used to
     * decide what the maximum length of a message on the status line can be.
     * If there is a status line for the last window, 'sc_col' is independent
     * of 'ru_col'.
     */

    /*private*/ static final int COL_RULER = 17;        /* columns needed by standard ruler */

    /*private*/ static void comp_col()
    {
        boolean last_has_status = (p_ls[0] == 2 || (p_ls[0] == 1 && firstwin != lastwin));

        sc_col = 0;
        ru_col = 0;
        if (p_ru[0])
        {
            ru_col = (ru_wid != 0 ? ru_wid : COL_RULER) + 1;
            /* no last status line, adjust sc_col */
            if (!last_has_status)
                sc_col = ru_col;
        }
        if (p_sc[0])
        {
            sc_col += SHOWCMD_COLS;
            if (!p_ru[0] || last_has_status)       /* no need for separating space */
                sc_col++;
        }
        sc_col = (int)Columns[0] - sc_col;
        ru_col = (int)Columns[0] - ru_col;
        if (sc_col <= 0)            /* screen too narrow, will become a mess */
            sc_col = 1;
        if (ru_col <= 0)
            ru_col = 1;
    }

    /*
     * Get pointer to option variable, depending on local or global scope.
     */
    /*private*/ static Object get_varp_scope(vimoption_C v, int opt_flags)
    {
        if ((opt_flags & OPT_GLOBAL) != 0 && v.indir != PV_NONE)
        {
            if (v.var == VAR_WIN)
            {
                /* transform a pointer to a "w_onebuf_opt" option into a "w_allbuf_opt" option */
                return get_varp(v, true);
            }

            return v.var;
        }

        if ((opt_flags & OPT_LOCAL) != 0 && (v.indir & PV_BOTH) != 0)
        {
            switch (v.indir)
            {
                case PV_AR:   return curbuf.b_p_ar;
                case PV_EP:   return curbuf.b_p_ep;
                case PV_KP:   return curbuf.b_p_kp;
                case PV_LW:   return curbuf.b_p_lw;
                case PV_STL:  return curwin.w_onebuf_opt.wo_stl;
                case PV_UL:   return curbuf.b_p_ul;
            }
            return null;        /* "cannot happen" */
        }

        return get_varp(v, false);
    }

    /*
     * Get pointer to option variable.
     */
    /*private*/ static Object get_varp(vimoption_C v, boolean all)
    {
        /* hidden option, always return null */
        if (v.var == null)
            return null;

        winopt_C wop = (all) ? curwin.w_allbuf_opt : curwin.w_onebuf_opt;

        switch (v.indir)
        {
            case PV_NONE:   return v.var;

            /* global option with local value: use local value if it's been set */
            case PV_AR:     return (-1 < curbuf.b_p_ar[0])                  ? curbuf.b_p_ar : v.var;
            case PV_EP:     return (curbuf.b_p_ep[0].at(0) != NUL)          ? curbuf.b_p_ep : v.var;
            case PV_KP:     return (curbuf.b_p_kp[0].at(0) != NUL)          ? curbuf.b_p_kp : v.var;
            case PV_LW:     return (curbuf.b_p_lw[0].at(0) != NUL)          ? curbuf.b_p_lw : v.var;
            case PV_STL:    return (wop.wo_stl[0].at(0) != NUL)             ? wop.wo_stl    : v.var;
            case PV_UL:     return (curbuf.b_p_ul[0] != NO_LOCAL_UNDOLEVEL) ? curbuf.b_p_ul : v.var;

            case PV_BRI:    return wop.wo_bri;
            case PV_BRIOPT: return wop.wo_briopt;
            case PV_CC:     return wop.wo_cc;
            case PV_COCU:   return wop.wo_cocu;
            case PV_COLE:   return wop.wo_cole;
            case PV_CRBIND: return wop.wo_crb;
            case PV_CUC:    return wop.wo_cuc;
            case PV_CUL:    return wop.wo_cul;
            case PV_LBR:    return wop.wo_lbr;
            case PV_LIST:   return wop.wo_list;
            case PV_NU:     return wop.wo_nu;
            case PV_NUW:    return wop.wo_nuw;
            case PV_RL:     return wop.wo_rl;
            case PV_RLC:    return wop.wo_rlc;
            case PV_RNU:    return wop.wo_rnu;
            case PV_SCBIND: return wop.wo_scb;
            case PV_SCROLL: return wop.wo_scr;
            case PV_WFH:    return wop.wo_wfh;
            case PV_WFW:    return wop.wo_wfw;
            case PV_WRAP:   return wop.wo_wrap;

            case PV_AI:     return curbuf.b_p_ai;
            case PV_BIN:    return curbuf.b_p_bin;
            case PV_BL:     return curbuf.b_p_bl;
            case PV_BOMB:   return curbuf.b_p_bomb;
            case PV_CI:     return curbuf.b_p_ci;
            case PV_CIN:    return curbuf.b_p_cin;
            case PV_CINK:   return curbuf.b_p_cink;
            case PV_CINO:   return curbuf.b_p_cino;
            case PV_CINW:   return curbuf.b_p_cinw;
            case PV_COM:    return curbuf.b_p_com;
            case PV_EOL:    return curbuf.b_p_eol;
            case PV_ET:     return curbuf.b_p_et;
            case PV_FENC:   return curbuf.b_p_fenc;
            case PV_FEX:    return curbuf.b_p_fex;
            case PV_FF:     return curbuf.b_p_ff;
            case PV_FLP:    return curbuf.b_p_flp;
            case PV_FO:     return curbuf.b_p_fo;
            case PV_FT:     return curbuf.b_p_ft;
            case PV_IMI:    return curbuf.b_p_iminsert;
            case PV_IMS:    return curbuf.b_p_imsearch;
            case PV_INDE:   return curbuf.b_p_inde;
            case PV_INDK:   return curbuf.b_p_indk;
            case PV_INF:    return curbuf.b_p_inf;
            case PV_ISK:    return curbuf.b_p_isk;
            case PV_LISP:   return curbuf.b_p_lisp;
            case PV_MA:     return curbuf.b_p_ma;
            case PV_MOD:    return curbuf.b_changed;
            case PV_MPS:    return curbuf.b_p_mps;
            case PV_NF:     return curbuf.b_p_nf;
            case PV_PI:     return curbuf.b_p_pi;
            case PV_QE:     return curbuf.b_p_qe;
            case PV_RO:     return curbuf.b_p_ro;
            case PV_SI:     return curbuf.b_p_si;
            case PV_SMC:    return curbuf.b_p_smc;
            case PV_STS:    return curbuf.b_p_sts;
            case PV_SW:     return curbuf.b_p_sw;
            case PV_SYN:    return curbuf.b_p_syn;
            case PV_TS:     return curbuf.b_p_ts;
            case PV_TW:     return curbuf.b_p_tw;
            case PV_TX:     return curbuf.b_p_tx;
            case PV_UDF:    return curbuf.b_p_udf;
            case PV_WM:     return curbuf.b_p_wm;

            default:
                emsg(u8("E356: get_varp() ERROR"));
                break;
        }

        return null;
    }

    /*
     * Get the value of 'equalprg', either the buffer-local one or the global one.
     */
    /*private*/ static Bytes get_equalprg()
    {
        if (curbuf.b_p_ep[0].at(0) == NUL)
            return p_ep[0];

        return curbuf.b_p_ep[0];
    }

    /*
     * Copy options from one window to another.
     * Used when splitting a window.
     */
    /*private*/ static void win_copy_options(window_C wp_from, window_C wp_to)
    {
        copy_winopt(wp_from.w_onebuf_opt, wp_to.w_onebuf_opt);
        copy_winopt(wp_from.w_allbuf_opt, wp_to.w_allbuf_opt);
        briopt_check(wp_to);
    }

    /*
     * Copy the options from one winopt_C to another.
     * Doesn't free the old option values in "to", use clear_winopt() for that.
     * The 'scroll' option is not copied, because it depends on the window height.
     * The 'previewwindow' option is reset, there can be only one preview window.
     */
    /*private*/ static void copy_winopt(winopt_C from, winopt_C to)
    {
        to.wo_list[0] = from.wo_list[0];
        to.wo_nu[0] = from.wo_nu[0];
        to.wo_rnu[0] = from.wo_rnu[0];
        to.wo_nuw[0] = from.wo_nuw[0];
        to.wo_rl[0]  = from.wo_rl[0];
        to.wo_rlc[0] = STRDUP(from.wo_rlc[0]);
        to.wo_stl[0] = STRDUP(from.wo_stl[0]);
        to.wo_wrap[0] = from.wo_wrap[0];
        to.wo_lbr[0] = from.wo_lbr[0];
        to.wo_bri[0] = from.wo_bri[0];
        to.wo_briopt[0] = STRDUP(from.wo_briopt[0]);
        to.wo_scb[0] = from.wo_scb[0];
        to.wo_crb[0] = from.wo_crb[0];
        to.wo_cuc[0] = from.wo_cuc[0];
        to.wo_cul[0] = from.wo_cul[0];
        to.wo_cc[0] = STRDUP(from.wo_cc[0]);
        to.wo_cocu[0] = STRDUP(from.wo_cocu[0]);
        to.wo_cole[0] = from.wo_cole[0];
        check_winopt(to);           /* don't want null pointers */
    }

    /*
     * Check string options in a window for a null value.
     */
    /*private*/ static void check_win_options(window_C win)
    {
        check_winopt(win.w_onebuf_opt);
        check_winopt(win.w_allbuf_opt);
    }

    /*
     * Check for null pointers in a winopt_C and replace them with EMPTY_OPTION.
     */
    /*private*/ static void check_winopt(winopt_C wop)
    {
        check_string_option(wop.wo_rlc);
        check_string_option(wop.wo_stl);
        check_string_option(wop.wo_cc);
        check_string_option(wop.wo_cocu);
        check_string_option(wop.wo_briopt);
    }

    /*
     * Free the allocated memory inside a winopt_C.
     */
    /*private*/ static void clear_winopt(winopt_C wop)
    {
        clear_string_option(wop.wo_briopt);
        clear_string_option(wop.wo_rlc);
        clear_string_option(wop.wo_stl);
        clear_string_option(wop.wo_cc);
        clear_string_option(wop.wo_cocu);
    }

    /*
     * Copy global option values to local options for one buffer.
     * Used when creating a new buffer and sometimes when entering a buffer.
     * flags:
     * BCO_ENTER    We will enter the buf buffer.
     * BCO_ALWAYS   Always copy the options, but only set b_p_initialized when appropriate.
     */
    /*private*/ static void buf_copy_options(buffer_C buf, int flags)
    {
        boolean should_copy = true;
        Bytes save_p_isk = null;
        boolean did_isk = false;

        /*
         * Don't do anything if the buffer is invalid.
         */
        if (buf == null || !buf_valid(buf))
            return;

        /*
         * Skip this when the option defaults have not been set yet.
         * Happens when main() allocates the first buffer.
         */
        if (p_cpo[0] != null)
        {
            /*
             * Always copy when entering and 'cpo' contains 'S'.
             * Don't copy when already initialized.
             * Don't copy when 'cpo' contains 's' and not entering.
             * 'S'  BCO_ENTER  initialized  's'  should_copy
             * yes    yes          X         X      true
             * yes    no          yes        X      false
             * no      X          yes        X      false
             *  X     no          no        yes     false
             *  X     no          no        no      true
             * no     yes         no         X      true
             */
            if ((vim_strbyte(p_cpo[0], CPO_BUFOPTGLOB) == null || (flags & BCO_ENTER) == 0)
                    && (buf.b_p_initialized
                        || ((flags & BCO_ENTER) == 0
                            && vim_strbyte(p_cpo[0], CPO_BUFOPT) != null)))
                should_copy = false;

            if (should_copy || (flags & BCO_ALWAYS) != 0)
            {
                /* Don't copy the options specific to a help buffer when the options were already initialized
                 * (jumping back to a help file with CTRL-T or CTRL-O). */
                boolean dont_do_help = buf.b_p_initialized;
                if (dont_do_help)           /* don't free "b_p_isk" */
                {
                    save_p_isk = buf.b_p_isk[0];
                    buf.b_p_isk[0] = null;
                }
                /*
                 * Always free the allocated strings.
                 * If not already initialized, set 'readonly' and copy 'fileformat'.
                 */
                if (!buf.b_p_initialized)
                {
                    free_buf_options(buf, true);
                    buf.b_p_ro[0] = false;             /* don't copy readonly */
                    buf.b_p_tx[0] = p_tx[0];
                    buf.b_p_fenc[0] = STRDUP(p_fenc[0]);
                    buf.b_p_ff[0] = STRDUP(p_ff[0]);
                }
                else
                    free_buf_options(buf, false);

                buf.b_p_ai[0] = p_ai[0];
                buf.b_p_ai_nopaste = p_ai_nopaste;
                buf.b_p_sw[0] = p_sw[0];
                buf.b_p_tw[0] = p_tw[0];
                buf.b_p_tw_nopaste = p_tw_nopaste;
                buf.b_p_tw_nobin = p_tw_nobin;
                buf.b_p_wm[0] = p_wm[0];
                buf.b_p_wm_nopaste = p_wm_nopaste;
                buf.b_p_wm_nobin = p_wm_nobin;
                buf.b_p_bin[0] = p_bin[0];
                buf.b_p_bomb[0] = p_bomb[0];
                buf.b_p_et[0] = p_et[0];
                buf.b_p_et_nobin = p_et_nobin;
                buf.b_p_inf[0] = p_inf[0];
                buf.b_p_sts[0] = p_sts[0];
                buf.b_p_sts_nopaste = p_sts_nopaste;
                buf.b_p_com[0] = STRDUP(p_com[0]);
                buf.b_p_fo[0] = STRDUP(p_fo[0]);
                buf.b_p_flp[0] = STRDUP(p_flp[0]);
                buf.b_p_nf[0] = STRDUP(p_nf[0]);
                buf.b_p_mps[0] = STRDUP(p_mps[0]);
                buf.b_p_si[0] = p_si[0];
                buf.b_p_ci[0] = p_ci[0];
                buf.b_p_cin[0] = p_cin[0];
                buf.b_p_cink[0] = STRDUP(p_cink[0]);
                buf.b_p_cino[0] = STRDUP(p_cino[0]);
                /* Don't copy 'filetype', it must be detected. */
                buf.b_p_ft[0] = EMPTY_OPTION;
                buf.b_p_pi[0] = p_pi[0];
                buf.b_p_cinw[0] = STRDUP(p_cinw[0]);
                buf.b_p_lisp[0] = p_lisp[0];
                /* Don't copy 'syntax', it must be set. */
                buf.b_p_syn[0] = EMPTY_OPTION;
                buf.b_p_smc[0] = p_smc[0];
                buf.b_p_inde[0] = STRDUP(p_inde[0]);
                buf.b_p_indk[0] = STRDUP(p_indk[0]);
                buf.b_p_fex[0] = STRDUP(p_fex[0]);
                /* This isn't really an option, but copying the langmap and IME
                 * state from the current buffer is better than resetting it. */
                buf.b_p_iminsert[0] = p_iminsert[0];
                buf.b_p_imsearch[0] = p_imsearch[0];

                /* Options that are normally global but also have a local value
                 * are not copied: start using the global value. */
                buf.b_p_ar[0] = -1;
                buf.b_p_ul[0] = NO_LOCAL_UNDOLEVEL;
                buf.b_p_ep[0] = EMPTY_OPTION;
                buf.b_p_kp[0] = EMPTY_OPTION;
                buf.b_p_qe[0] = STRDUP(p_qe[0]);
                buf.b_p_udf[0] = p_udf[0];
                buf.b_p_lw[0] = EMPTY_OPTION;

                /*
                 * Don't copy the options set by ex_help(), use the saved values,
                 * when going from a help buffer to a non-help buffer.
                 */
                if (dont_do_help)
                    buf.b_p_isk[0] = save_p_isk;
                else
                {
                    buf.b_p_isk[0] = STRDUP(p_isk[0]);
                    did_isk = true;
                    buf.b_p_ts[0] = p_ts[0];
                    buf.b_p_ma[0] = p_ma[0];
                }
            }

            /*
             * When the options should be copied (ignoring BCO_ALWAYS), set the
             * flag that indicates that the options have been initialized.
             */
            if (should_copy)
                buf.b_p_initialized = true;
        }

        check_buf_options(buf);         /* make sure we don't have NULLs */
        if (did_isk)
            buf_init_chartab(buf, false);
    }

    /*
     * Reset the 'modifiable' option and its default value.
     */
    /*private*/ static void reset_modifiable()
    {
        curbuf.b_p_ma[0] = false;
        p_ma[0] = false;
        int opt_idx = findoption(u8("ma"));
        if (0 <= opt_idx)
            vimoptions[opt_idx].def_val = false;
    }

    /*
     * Set the global value for 'iminsert' to the local value.
     */
    /*private*/ static void set_iminsert_global()
    {
        p_iminsert[0] = curbuf.b_p_iminsert[0];
    }

    /*
     * Set the global value for 'imsearch' to the local value.
     */
    /*private*/ static void set_imsearch_global()
    {
        p_imsearch[0] = curbuf.b_p_imsearch[0];
    }

    /*private*/ static int expand_option_idx = -1;
    /*private*/ static Bytes expand_option_name = new Bytes(new byte[] { 't', '_', NUL, NUL, NUL });
    /*private*/ static int expand_option_flags;

    /*private*/ static void set_context_in_set_cmd(expand_C xp, Bytes arg, int opt_flags)
        /* opt_flags: OPT_GLOBAL and/or OPT_LOCAL */
    {
        expand_option_flags = opt_flags;

        xp.xp_context = EXPAND_SETTINGS;
        if (arg.at(0) == NUL)
        {
            xp.xp_pattern = arg;
            return;
        }
        Bytes p = arg.plus(strlen(arg) - 1);
        if (p.at(0) == (byte)' ' && p.at(-1) != (byte)'\\')
        {
            xp.xp_pattern = p.plus(1);
            return;
        }
        while (BLT(arg, p))
        {
            Bytes s = p;
            /* count number of backslashes before ' ' or ',' */
            if (p.at(0) == (byte)' ' || p.at(0) == (byte)',')
            {
                while (BLT(arg, s) && s.at(-1) == (byte)'\\')
                    s = s.minus(1);
            }
            /* break at a space with an even number of backslashes */
            if (p.at(0) == (byte)' ' && (BDIFF(p, s) & 1) == 0)
            {
                p = p.plus(1);
                break;
            }
            p = p.minus(1);
        }
        if (STRNCMP(p, u8("no"), 2) == 0 && STRNCMP(p, u8("novice"), 6) != 0)
        {
            xp.xp_context = EXPAND_BOOL_SETTINGS;
            p = p.plus(2);
        }
        if (STRNCMP(p, u8("inv"), 3) == 0)
        {
            xp.xp_context = EXPAND_BOOL_SETTINGS;
            p = p.plus(3);
        }
        xp.xp_pattern = arg = p;

        int nextchar;
        boolean is_term_option = false;
        int opt_idx = 0;
        long flags = 0;

        if (arg.at(0) == (byte)'<')
        {
            while (p.at(0) != (byte)'>')
                if ((p = p.plus(1)).at(-1) == NUL)        /* expand terminal option name */
                    return;
            int key = get_special_key_code(arg.plus(1));
            if (key == 0)               /* unknown name */
            {
                xp.xp_context = EXPAND_NOTHING;
                return;
            }
            nextchar = (p = p.plus(1)).at(0);
            is_term_option = true;
            expand_option_name.be(2, KEY2TERMCAP0(key));
            expand_option_name.be(3, KEY2TERMCAP1(key));
        }
        else
        {
            if (p.at(0) == (byte)'t' && p.at(1) == (byte)'_')
            {
                p = p.plus(2);
                if (p.at(0) != NUL)
                    p = p.plus(1);
                if (p.at(0) == NUL)
                    return;             /* expand option name */
                nextchar = (p = p.plus(1)).at(0);
                is_term_option = true;
                expand_option_name.be(2, p.at(-2));
                expand_option_name.be(3, p.at(-1));
            }
            else
            {
                /* Allow * wildcard. */
                while (asc_isalnum(p.at(0)) || p.at(0) == (byte)'_' || p.at(0) == (byte)'*')
                    p = p.plus(1);
                if (p.at(0) == NUL)
                    return;
                nextchar = p.at(0);
                p.be(0, NUL);
                opt_idx = findoption(arg);
                p.be(0, nextchar);
                if (opt_idx == -1 || vimoptions[opt_idx].var == null)
                {
                    xp.xp_context = EXPAND_NOTHING;
                    return;
                }
                flags = vimoptions[opt_idx].flags[0];
                if ((flags & P_BOOL) != 0)
                {
                    xp.xp_context = EXPAND_NOTHING;
                    return;
                }
            }
        }
        /* handle "-=" and "+=" */
        if ((nextchar == '-' || nextchar == '+' || nextchar == '^') && p.at(1) == (byte)'=')
        {
            p = p.plus(1);
            nextchar = '=';
        }
        if ((nextchar != '=' && nextchar != ':') || xp.xp_context == EXPAND_BOOL_SETTINGS)
        {
            xp.xp_context = EXPAND_UNSUCCESSFUL;
            return;
        }
        if (xp.xp_context != EXPAND_BOOL_SETTINGS && p.at(1) == NUL)
        {
            xp.xp_context = EXPAND_OLD_SETTING;
            if (is_term_option)
                expand_option_idx = -1;
            else
                expand_option_idx = opt_idx;
            xp.xp_pattern = p.plus(1);
            return;
        }
        xp.xp_context = EXPAND_NOTHING;
        if (is_term_option || (flags & P_NUM) != 0)
            return;

        xp.xp_pattern = p.plus(1);

        /* For an option that is a list of file names, find the start of the last file name. */
        for (p = arg.plus(strlen(arg) - 1); BLT(xp.xp_pattern, p); p = p.minus(1))
        {
            /* count number of backslashes before ' ' or ',' */
            if (p.at(0) == (byte)' ' || p.at(0) == (byte)',')
            {
                Bytes s = p;
                while (BLT(xp.xp_pattern, s) && s.at(-1) == (byte)'\\')
                    s = s.minus(1);
                if (p.at(0) == (byte)',' && (flags & P_COMMA) != 0 && (BDIFF(p, s) & 1) == 0)
                {
                    xp.xp_pattern = p.plus(1);
                    break;
                }
            }
        }
    }

    /*private*/ static final Bytes[] es__names = { u8("all"), u8("termcap") };

    /*private*/ static boolean expandSettings(expand_C xp, regmatch_C regmatch, int[] num_file, Bytes[][] file)
    {
        int num_normal = 0;     /* Nr of matching non-term-code settings */
        int num_term = 0;       /* Nr of matching terminal code settings */
        int count = 0;
        Bytes name_buf = new Bytes(MAX_KEY_NAME_LEN);

        boolean ic = regmatch.rm_ic;    /* remember the ignore-case flag */

        /* do this loop twice:
         * loop == 0: count the number of matching options
         * loop == 1: copy the matching options into allocated memory
         */
        for (int loop = 0; loop <= 1; loop++)
        {
            Bytes str;

            regmatch.rm_ic = ic;
            if (xp.xp_context != EXPAND_BOOL_SETTINGS)
            {
                for (int match = 0; match < es__names.length; match++)
                    if (vim_regexec(regmatch, es__names[match], 0))
                    {
                        if (loop == 0)
                            num_normal++;
                        else
                            file[0][count++] = STRDUP(es__names[match]);
                    }
            }
            for (int opt_idx = 0; (str = vimoptions[opt_idx].fullname) != null; opt_idx++)
            {
                if (vimoptions[opt_idx].var == null)
                    continue;
                if (xp.xp_context == EXPAND_BOOL_SETTINGS && (vimoptions[opt_idx].flags[0] & P_BOOL) == 0)
                    continue;
                boolean is_term_opt = istermoption(vimoptions[opt_idx]);
                if (is_term_opt && 0 < num_normal)
                    continue;
                boolean match = false;
                if (vim_regexec(regmatch, str, 0)
                        || (vimoptions[opt_idx].shortname != null
                                && vim_regexec(regmatch, vimoptions[opt_idx].shortname, 0)))
                    match = true;
                else if (is_term_opt)
                {
                    name_buf.be(0, (byte)'<');
                    name_buf.be(1, (byte)'t');
                    name_buf.be(2, (byte)'_');
                    name_buf.be(3, str.at(2));
                    name_buf.be(4, str.at(3));
                    name_buf.be(5, (byte)'>');
                    name_buf.be(6, NUL);
                    if (vim_regexec(regmatch, name_buf, 0))
                    {
                        match = true;
                        str = name_buf;
                    }
                }
                if (match)
                {
                    if (loop == 0)
                    {
                        if (is_term_opt)
                            num_term++;
                        else
                            num_normal++;
                    }
                    else
                        file[0][count++] = STRDUP(str);
                }
            }
            /*
             * Check terminal key codes, these are not in the option table.
             */
            if (xp.xp_context != EXPAND_BOOL_SETTINGS  && num_normal == 0)
            {
                for (int opt_idx = 0; (str = get_termcode(opt_idx)) != null; opt_idx++)
                {
                    if (!asc_isprint(str.at(0)) || !asc_isprint(str.at(1)))
                        continue;

                    name_buf.be(0, (byte)'t');
                    name_buf.be(1, (byte)'_');
                    name_buf.be(2, str.at(0));
                    name_buf.be(3, str.at(1));
                    name_buf.be(4, NUL);

                    boolean match = false;
                    if (vim_regexec(regmatch, name_buf, 0))
                        match = true;
                    else
                    {
                        name_buf.be(0, (byte)'<');
                        name_buf.be(1, (byte)'t');
                        name_buf.be(2, (byte)'_');
                        name_buf.be(3, str.at(0));
                        name_buf.be(4, str.at(1));
                        name_buf.be(5, (byte)'>');
                        name_buf.be(6, NUL);

                        if (vim_regexec(regmatch, name_buf, 0))
                            match = true;
                    }
                    if (match)
                    {
                        if (loop == 0)
                            num_term++;
                        else
                            file[0][count++] = STRDUP(name_buf);
                    }
                }

                /*
                 * Check special key names.
                 */
                regmatch.rm_ic = true;          /* ignore case here */
                for (int opt_idx = 0; (str = get_key_name(opt_idx)) != null; opt_idx++)
                {
                    name_buf.be(0, (byte)'<');
                    STRCPY(name_buf.plus(1), str);
                    STRCAT(name_buf, u8(">"));

                    if (vim_regexec(regmatch, name_buf, 0))
                    {
                        if (loop == 0)
                            num_term++;
                        else
                            file[0][count++] = STRDUP(name_buf);
                    }
                }
            }
            if (loop == 0)
            {
                if (0 < num_normal)
                    num_file[0] = num_normal;
                else if (0 < num_term)
                    num_file[0] = num_term;
                else
                    return true;
                file[0] = new Bytes[num_file[0]];
            }
        }

        return true;
    }

    /*private*/ static boolean expandOldSetting(int[] num_file, Bytes[][] file)
    {
        num_file[0] = 0;
        file[0] = new Bytes[1];

        Bytes var = null;

        /*
         * For a terminal key code expand_option_idx is < 0.
         */
        if (expand_option_idx < 0)
        {
            var = find_termcode(expand_option_name.plus(2));
            if (var == null)
                expand_option_idx = findoption(expand_option_name);
        }

        if (0 <= expand_option_idx)
        {
            /* put string of option value in nameBuff */
            option_value2string(vimoptions[expand_option_idx], expand_option_flags);
            var = nameBuff;
        }
        else if (var == null)
            var = u8("");

        /*
         * A backslash is required before some characters.
         * This is the reverse of what happens in do_set().
         */
        Bytes buf = vim_strsave_escaped(var, escape_chars);

        file[0][0] = buf;
        num_file[0] = 1;

        return true;
    }

    /*
     * Get the value for the numeric or string option *opp in a nice format into nameBuff[].
     * Must not be called with a hidden option!
     */
    /*private*/ static void option_value2string(vimoption_C v, int opt_flags)
        /* opt_flags: OPT_GLOBAL and/or OPT_LOCAL */
    {
        Object varp = get_varp_scope(v, opt_flags);

        if ((v.flags[0] & P_NUM) != 0)
        {
            long[] wc = { 0 };

            if (wc_use_keyname((long[])varp, wc))
                STRCPY(nameBuff, get_special_key_name((int)wc[0], 0));
            else if (wc[0] != 0)
                STRCPY(nameBuff, transchar((int)wc[0]));
            else
                libC.sprintf(nameBuff, u8("%ld"), ((long[])varp)[0]);
        }
        else    /* P_STRING */
        {
            Bytes s = ((Bytes[])varp)[0];

            if (s == null)              /* just in case */
                nameBuff.be(0, NUL);
            /* translate 'pastetoggle' into special key names */
            else if (v.var == p_pt)
                str2specialbuf(p_pt[0], nameBuff, MAXPATHL);
            else
                vim_strncpy(nameBuff, s, MAXPATHL - 1);
        }
    }

    /*
     * Return true if "varp" points to 'wildchar' or 'wildcharm' and it can be printed as a keyname.
     * "*wcp" is set to the value of the option if it's 'wildchar' or 'wildcharm'.
     */
    /*private*/ static boolean wc_use_keyname(long[] varp, long[] wcp)
    {
        if (varp == p_wc || varp == p_wcm)
        {
            wcp[0] = varp[0];
            if (is_special((int)wcp[0]) || 0 <= find_special_key_in_table((int)wcp[0]))
                return true;
        }
        return false;
    }

    /*
     * Return true if format option 'x' is in effect.
     * Take care of no formatting when 'paste' is set.
     */
    /*private*/ static boolean has_format_option(int x)
    {
        if (p_paste[0])
            return false;

        return (vim_strchr(curbuf.b_p_fo[0], x) != null);
    }

    /*
     * Return true if "x" is present in 'shortmess' option, or
     * 'shortmess' contains 'a' and "x" is present in SHM_A.
     */
    /*private*/ static boolean shortmess(int x)
    {
        return (p_shm[0] != null
            && (vim_strchr(p_shm[0], x) != null || (vim_strchr(p_shm[0], 'a') != null && vim_strchr(SHM_A, x) != null)));
    }

    /*private*/ static boolean old_p_paste;
    /*private*/ static boolean save_sm;
    /*private*/ static boolean save_ru;
    /*private*/ static boolean save_ri;

    /*
     * paste_option_changed() - Called after "p_paste" was set or reset.
     */
    /*private*/ static void paste_option_changed()
    {
        if (p_paste[0])
        {
            /*
             * Paste switched from off to on.
             * Save the current values, so they can be restored later.
             */
            if (!old_p_paste)
            {
                /* save options for each buffer */
                for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
                {
                    buf.b_p_tw_nopaste = buf.b_p_tw[0];
                    buf.b_p_wm_nopaste = buf.b_p_wm[0];
                    buf.b_p_sts_nopaste = buf.b_p_sts[0];
                    buf.b_p_ai_nopaste = buf.b_p_ai[0];
                }

                /* save global options */
                save_sm = p_sm[0];
                save_ru = p_ru[0];
                save_ri = p_ri[0];
                /* save global values for local buffer options */
                p_tw_nopaste = p_tw[0];
                p_wm_nopaste = p_wm[0];
                p_sts_nopaste = p_sts[0];
                p_ai_nopaste = p_ai[0];
            }

            /*
             * Always set the option values, also when 'paste' is set when it is already on.
             */
            /* set options for each buffer */
            for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
            {
                buf.b_p_tw[0] = 0;         /* textwidth is 0 */
                buf.b_p_wm[0] = 0;         /* wrapmargin is 0 */
                buf.b_p_sts[0] = 0;        /* softtabstop is 0 */
                buf.b_p_ai[0] = false;         /* no auto-indent */
            }

            /* set global options */
            p_sm[0] = false;                   /* no showmatch */
            if (p_ru[0])
                status_redraw_all();    /* redraw to remove the ruler */
            p_ru[0] = false;                   /* no ruler */
            p_ri[0] = false;                   /* no reverse insert */
            /* set global values for local buffer options */
            p_tw[0] = 0;
            p_wm[0] = 0;
            p_sts[0] = 0;
            p_ai[0] = false;
        }

        /*
         * Paste switched from on to off: Restore saved values.
         */
        else if (old_p_paste)
        {
            /* restore options for each buffer */
            for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
            {
                buf.b_p_tw[0] = buf.b_p_tw_nopaste;
                buf.b_p_wm[0] = buf.b_p_wm_nopaste;
                buf.b_p_sts[0] = buf.b_p_sts_nopaste;
                buf.b_p_ai[0] = buf.b_p_ai_nopaste;
            }

            /* restore global options */
            p_sm[0] = save_sm;
            if (p_ru[0] != save_ru)
                status_redraw_all();    /* redraw to draw the ruler */
            p_ru[0] = save_ru;
            p_ri[0] = save_ri;
            /* set global values for local buffer options */
            p_tw[0] = p_tw_nopaste;
            p_wm[0] = p_wm_nopaste;
            p_sts[0] = p_sts_nopaste;
            p_ai[0] = p_ai_nopaste;
        }

        old_p_paste = p_paste[0];
    }

    /*
     * Return true when option "name" has been set.
     * Only works correctly for global options.
     */
    /*private*/ static boolean option_was_set(Bytes name)
    {
        int idx = findoption(name);
        if (idx < 0)        /* unknown option */
            return false;
        if ((vimoptions[idx].flags[0] & P_WAS_SET) != 0)
            return true;

        return false;
    }

    /*
     * Reset the flag indicating option "name" was set.
     */
    /*private*/ static void reset_option_was_set(Bytes name)
    {
        int idx = findoption(name);

        if (0 <= idx)
            vimoptions[idx].flags[0] &= ~P_WAS_SET;
    }

    /*
     * fill_breakat_flags() -- called when 'breakat' changes value.
     */
    /*private*/ static void fill_breakat_flags()
    {
        for (int i = 0; i < 256; i++)
            breakat_flags[i] = false;

        if (p_breakat[0] != null)
            for (Bytes p = p_breakat[0]; p.at(0) != NUL; p = p.plus(1))
                breakat_flags[char_u(p.at(0))] = true;
    }

    /*
     * Check an option that can be a range of string values.
     *
     * Return true for correct value, false otherwise.
     * Empty is always OK.
     */
    /*private*/ static boolean check_opt_strings(Bytes val, Bytes[] values, boolean list)
        /* list: when true: accept a list of values */
    {
        return opt_strings_flags(val, values, null, list);
    }

    /*
     * Handle an option that can be a range of string values.
     * Set a flag in "*flagp" for each string present.
     *
     * Return true for correct value, false otherwise.
     * Empty is always OK.
     */
    /*private*/ static boolean opt_strings_flags(Bytes val, Bytes[] values, int[] flagp, boolean list)
        /* val: new value */
        /* values: array of valid string values */
        /* list: when true: accept a list of values */
    {
        int new_flags = 0;

        while (val.at(0) != NUL)
        {
            for (int i = 0; ; i++)
            {
                if (values[i] == null)      /* "val" not found in values[] */
                    return false;

                int len = strlen(values[i]);
                if (STRNCMP(values[i], val, len) == 0 && ((list && val.at(len) == (byte)',') || val.at(len) == NUL))
                {
                    val = val.plus(len + ((val.at(len) == (byte)',') ? 1 : 0));
                    new_flags |= (1 << i);
                    break;                  /* check next item in "val" list */
                }
            }
        }
        if (flagp != null)
            flagp[0] = new_flags;

        return true;
    }

    /*
     * Read the 'wildmode' option, fill wim_flags[].
     */
    /*private*/ static boolean check_opt_wim()
    {
        byte[] new_wim_flags = new byte[4];
        for (int i = 0; i < 4; i++)
            new_wim_flags[i] = 0;

        int idx = 0;
        for (Bytes p = p_wim[0]; p.at(0) != NUL; p = p.plus(1))
        {
            int i;
            for (i = 0; asc_isalpha(p.at(i)); i++)
                ;
            if (p.at(i) != NUL && p.at(i) != (byte)',' && p.at(i) != (byte)':')
                return false;
            if (i == 7 && STRNCMP(p, u8("longest"), 7) == 0)
                new_wim_flags[idx] |= WIM_LONGEST;
            else if (i == 4 && STRNCMP(p, u8("full"), 4) == 0)
                new_wim_flags[idx] |= WIM_FULL;
            else if (i == 4 && STRNCMP(p, u8("list"), 4) == 0)
                new_wim_flags[idx] |= WIM_LIST;
            else
                return false;
            p = p.plus(i);
            if (p.at(0) == NUL)
                break;
            if (p.at(0) == (byte)',')
            {
                if (idx == 3)
                    return false;
                idx++;
            }
        }

        /* fill remaining entries with last flag */
        while (idx < 3)
        {
            new_wim_flags[idx + 1] = new_wim_flags[idx];
            idx++;
        }

        /* only when there are no errors, wim_flags[] is changed */
        for (int i = 0; i < 4; i++)
            wim_flags[i] = new_wim_flags[i];

        return true;
    }

    /*
     * Check if backspacing over something is allowed.
     */
    /*private*/ static boolean can_bs(int what)
        /* what: BS_INDENT, BS_EOL or BS_START */
    {
        switch (p_bs[0].at(0))
        {
            case '2': return true;
            case '1': return (what != BS_START);
            case '0': return false;
        }

        return (vim_strchr(p_bs[0], what) != null);
    }

    /*
     * Save the current values of 'fileformat' and 'fileencoding', so that we know
     * the file must be considered changed when the value is different.
     */
    /*private*/ static void save_file_ff(buffer_C buf)
    {
        buf.b_start_ffc = buf.b_p_ff[0].at(0);
        buf.b_start_eol = buf.b_p_eol[0];
        buf.b_start_bomb = buf.b_p_bomb[0];

        /* Only use free/alloc when necessary, they take time. */
        if (buf.b_start_fenc == null || STRCMP(buf.b_start_fenc, buf.b_p_fenc[0]) != 0)
            buf.b_start_fenc = STRDUP(buf.b_p_fenc[0]);
    }

    /*
     * Return true if 'fileformat' and/or 'fileencoding' has a different value
     * from when editing started (save_file_ff() called).
     * Also when 'endofline' was changed and 'binary' is set, or when 'bomb' was
     * changed and 'binary' is not set.
     * When "ignore_empty" is true don't consider a new, empty buffer to be changed.
     */
    /*private*/ static boolean file_ff_differs(buffer_C buf, boolean ignore_empty)
    {
        /* In a buffer that was never loaded the options are not valid. */
        if ((buf.b_flags & BF_NEVERLOADED) != 0)
            return false;
        if (ignore_empty
                && (buf.b_flags & BF_NEW) != 0
                && buf.b_ml.ml_line_count == 1
                && ml_get_buf(buf, 1, false).at(0) == NUL)
            return false;
        if (buf.b_start_ffc != buf.b_p_ff[0].at(0))
            return true;
        if (buf.b_p_bin[0] && buf.b_start_eol != buf.b_p_eol[0])
            return true;
        if (!buf.b_p_bin[0] && buf.b_start_bomb != buf.b_p_bomb[0])
            return true;
        if (buf.b_start_fenc == null)
            return (buf.b_p_fenc[0].at(0) != NUL);

        return (STRCMP(buf.b_start_fenc, buf.b_p_fenc[0]) != 0);
    }

    /*
     * Return the effective shiftwidth value for current buffer,
     * using the 'tabstop' value when 'shiftwidth' is zero.
     */
    /*private*/ static long get_sw_value(buffer_C buf)
    {
        return (buf.b_p_sw[0] != 0) ? buf.b_p_sw[0] : buf.b_p_ts[0];
    }

    /*
     * Return the effective softtabstop value for the current buffer,
     * using the 'tabstop' value when 'softtabstop' is negative.
     */
    /*private*/ static long get_sts_value()
    {
        return (curbuf.b_p_sts[0] < 0) ? get_sw_value(curbuf) : curbuf.b_p_sts[0];
    }

    /*
     * Check matchpairs option for "*initc".
     * If there is a match set "*initc" to the matching character and "*findc" to the opposite.
     * Set "*backwards" to the direction.
     * When "switchit" is true, swap the direction.
     */
    /*private*/ static void find_mps_values(int[] initc, int[] findc, boolean[] backwards, boolean switchit)
    {
        Bytes ptr = curbuf.b_p_mps[0];
        while (ptr.at(0) != NUL)
        {
            Bytes prev;

            if (us_ptr2char(ptr) == initc[0])
            {
                if (switchit)
                {
                    findc[0] = initc[0];
                    initc[0] = us_ptr2char(ptr.plus(us_ptr2len_cc(ptr) + 1));
                    backwards[0] = true;
                }
                else
                {
                    findc[0] = us_ptr2char(ptr.plus(us_ptr2len_cc(ptr) + 1));
                    backwards[0] = false;
                }
                return;
            }
            prev = ptr;
            ptr = ptr.plus(us_ptr2len_cc(ptr) + 1);
            if (us_ptr2char(ptr) == initc[0])
            {
                if (switchit)
                {
                    findc[0] = initc[0];
                    initc[0] = us_ptr2char(prev);
                    backwards[0] = false;
                }
                else
                {
                    findc[0] = us_ptr2char(prev);
                    backwards[0] = true;
                }
                return;
            }
            ptr = ptr.plus(us_ptr2len_cc(ptr));

            if (ptr.at(0) == (byte)',')
                ptr = ptr.plus(1);
        }
    }

    /*
     * This is called when 'breakindentopt' is changed and when a window is initialized.
     */
    /*private*/ static boolean briopt_check(window_C wp)
    {
        int bri_shift = 0;
        int bri_min = 20;
        boolean bri_sbr = false;

        Bytes p = wp.w_onebuf_opt.wo_briopt[0];
        while (p.at(0) != NUL)
        {
            if (STRNCMP(p, u8("shift:"), 6) == 0 && ((p.at(6) == (byte)'-' && asc_isdigit(p.at(7))) || asc_isdigit(p.at(6))))
            {
                p = p.plus(6);
                { Bytes[] __ = { p }; bri_shift = (int)getdigits(__); p = __[0]; }
            }
            else if (STRNCMP(p, u8("min:"), 4) == 0 && asc_isdigit(p.at(4)))
            {
                p = p.plus(4);
                { Bytes[] __ = { p }; bri_min = (int)getdigits(__); p = __[0]; }
            }
            else if (STRNCMP(p, u8("sbr"), 3) == 0)
            {
                p = p.plus(3);
                bri_sbr = true;
            }
            if (p.at(0) != (byte)',' && p.at(0) != NUL)
                return false;
            if (p.at(0) == (byte)',')
                p = p.plus(1);
        }

        wp.w_p_brishift = bri_shift;
        wp.w_p_brimin = bri_min;
        wp.w_p_brisbr = bri_sbr;

        return true;
    }

    /*
     * ex_cmds.c: some functions for command line commands --------------------------------------------
     */

    /*
     * ":ascii" and "ga".
     */
    /*private*/ static final ex_func_C ex_ascii = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            int[] cc = new int[MAX_MCO];

            int c = us_ptr2char_cc(ml_get_cursor(), cc);
            if (c == NUL)
            {
                msg(u8("NUL"));
                return;
            }

            int ci = 0;

            ioBuff.be(0, NUL);
            if (c < 0x80)
            {
                if (c == NL)        /* NUL is stored as NL */
                    c = NUL;

                int cval;
                if (c == CAR && get_fileformat(curbuf) == EOL_MAC)
                    cval = NL;      /* NL is stored as CR */
                else
                    cval = c;

                Bytes buf1 = new Bytes(20);
                if (vim_isprintc(c) && (c < ' ' || '~' < c))
                {
                    Bytes buf3 = new Bytes(7);
                    transchar_nonprint(buf3, c);
                    vim_snprintf(buf1, buf1.size(), u8("  <%s>"), buf3);
                }
                else
                    buf1.be(0, NUL);

                Bytes buf2 = new Bytes(20);
                if (0x80 <= c)
                    vim_snprintf(buf2, buf2.size(), u8("  <M-%s>"), transchar(c & 0x7f));
                else
                    buf2.be(0, NUL);

                vim_snprintf(ioBuff, IOSIZE, u8("<%s>%s%s  %d,  Hex %02x,  Octal %03o"),
                                        transchar(c), buf1, buf2, cval, cval, cval);
                c = cc[ci++];
            }

            /* Repeat for combining characters. */
            while (0x100 <= c || 0x80 <= c)
            {
                int len = strlen(ioBuff);
                /* This assumes every multi-byte char is printable... */
                if (0 < len)
                    ioBuff.be(len++, (byte)' ');
                ioBuff.be(len++, (byte)'<');
                if (utf_iscomposing(c))
                    ioBuff.be(len++, (byte)' ');                /* draw composing char on top of a space */
                len += utf_char2bytes(c, ioBuff.plus(len));
                vim_snprintf(ioBuff.plus(len), IOSIZE - len,
                            (c < 0x10000) ? u8("> %d, Hex %04x, Octal %o")
                                            : u8("> %d, Hex %08x, Octal %o"), c, c, c);
                if (ci == MAX_MCO)
                    break;
                c = cc[ci++];
            }

            msg(ioBuff);
        }
    };

    /*
     * ":left", ":center" and ":right": align text.
     */
    /*private*/ static final ex_func_C ex_align = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int indent = 0;

            if (curwin.w_onebuf_opt.wo_rl[0])
            {
                /* switch left and right aligning */
                if (eap.cmdidx == CMD_right)
                    eap.cmdidx = CMD_left;
                else if (eap.cmdidx == CMD_left)
                    eap.cmdidx = CMD_right;
            }

            int width = libC.atoi(eap.arg);
            pos_C save_curpos = new pos_C();
            COPY_pos(save_curpos, curwin.w_cursor);
            if (eap.cmdidx == CMD_left)     /* width is used for new indent */
            {
                if (0 <= width)
                    indent = width;
            }
            else
            {
                /*
                 * if 'textwidth' set, use it
                 * else if 'wrapmargin' set, use it
                 * if invalid value, use 80
                 */
                if (width <= 0)
                    width = (int)curbuf.b_p_tw[0];
                if (width == 0 && 0 < curbuf.b_p_wm[0])
                    width = curwin.w_width - (int)curbuf.b_p_wm[0];
                if (width <= 0)
                    width = 80;
            }

            if (!u_save(eap.line1 - 1, eap.line2 + 1))
                return;

            for (curwin.w_cursor.lnum = eap.line1; curwin.w_cursor.lnum <= eap.line2; curwin.w_cursor.lnum++)
            {
                int new_indent;
                if (eap.cmdidx == CMD_left)             /* left align */
                    new_indent = indent;
                else
                {
                    boolean[] has_tab = { false };            /* avoid uninit warnings */

                    int len = linelen(eap.cmdidx == CMD_right ? has_tab : null) - get_indent();
                    if (len <= 0)                       /* skip blank lines */
                        continue;

                    if (eap.cmdidx == CMD_center)
                        new_indent = (width - len) / 2;
                    else
                    {
                        new_indent = width - len;       /* right align */

                        /*
                         * Make sure that embedded TABs don't make the text go too far to the right.
                         */
                        if (has_tab[0])
                            while (0 < new_indent)
                            {
                                set_indent(new_indent, 0);
                                if (linelen(null) <= width)
                                {
                                    /*
                                     * Now try to move the line as much as possible to the right.
                                     * Stop when it moves too far.
                                     */
                                    do
                                    {
                                        set_indent(++new_indent, 0);
                                    } while (linelen(null) <= width);
                                    --new_indent;
                                    break;
                                }
                                --new_indent;
                            }
                    }
                }
                if (new_indent < 0)
                    new_indent = 0;
                set_indent(new_indent, 0);                  /* set indent */
            }
            changed_lines(eap.line1, 0, eap.line2 + 1, 0L);
            COPY_pos(curwin.w_cursor, save_curpos);
            beginline(BL_WHITE | BL_FIX);
        }
    };

    /*
     * Get the length of the current line, excluding trailing white space.
     */
    /*private*/ static int linelen(boolean[] has_tab)
    {
        /* find the first non-blank character */
        Bytes line = ml_get_curline();
        Bytes first = skipwhite(line);

        /* find the character after the last non-blank character */
        Bytes last;
        for (last = first.plus(strlen(first)); BLT(first, last) && vim_iswhite(last.at(-1)); last = last.minus(1))
            ;

        byte save = last.at(0);
        last.be(0, NUL);
        int len = linetabsize(line);        /* get line length */
        if (has_tab != null)                /* check for embedded TAB */
            has_tab[0] = (vim_strrchr(first, TAB) != null);
        last.be(0, save);

        return len;
    }

    /* Buffer for two lines used during sorting.
     * They are allocated to contain the longest line being sorted. */
    /*private*/ static Bytes    sortbuf1;
    /*private*/ static Bytes    sortbuf2;

    /*private*/ static boolean  sort_ic;            /* ignore case */
    /*private*/ static int      sort_nr;            /* sort on number */
    /*private*/ static boolean  sort_rx;            /* sort on regex instead of skipping it */

    /*private*/ static boolean  sort_abort;         /* flag to indicate if sorting has been interrupted */

    /* Struct to store info to be sorted. */
    /*private*/ static final class sorti_C
    {
        long        lnum;                   /* line number */
        int         start_col_nr;           /* starting column number or number */
        int         end_col_nr;             /* ending column number */

        /*private*/ sorti_C()
        {
        }
    }

    /*private*/ static sorti_C[] ARRAY_sorti(int n)
    {
        sorti_C[] a = new sorti_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new sorti_C();
        return a;
    }

    /*private*/ static final Comparator<sorti_C> sort_compare = new Comparator<sorti_C>()
    {
        public int compare(sorti_C s1, sorti_C s2)
        {
            /* If the user interrupts, there's no way to stop qsort() immediately, but
             * if we return 0 every time, qsort will assume it's done sorting and exit. */
            if (sort_abort)
                return 0;

            fast_breakcheck();
            if (got_int)
                sort_abort = true;

            int cmp = 0;

            /* When sorting numbers "start_col_nr" is the number, not the column number. */
            if (sort_nr != 0)
                cmp = (s1.start_col_nr == s2.start_col_nr) ? 0 : (s2.start_col_nr < s1.start_col_nr) ? 1 : -1;
            else
            {
                /* We need to copy one line into "sortbuf1", because there is no guarantee
                 * that the first pointer becomes invalid when obtaining the second one. */
                STRNCPY(sortbuf1, ml_get(s1.lnum).plus(s1.start_col_nr), s1.end_col_nr - s1.start_col_nr + 1);
                sortbuf1.be(s1.end_col_nr - s1.start_col_nr, NUL);
                STRNCPY(sortbuf2, ml_get(s2.lnum).plus(s2.start_col_nr), s2.end_col_nr - s2.start_col_nr + 1);
                sortbuf2.be(s2.end_col_nr - s2.start_col_nr, NUL);

                cmp = (sort_ic) ? STRCASECMP(sortbuf1, sortbuf2) : STRCMP(sortbuf1, sortbuf2);
            }

            /* If two lines have the same value, preserve the original line order. */
            if (cmp == 0)
                cmp = (int)(s1.lnum - s2.lnum);

            return cmp;
        }
    };

    /*
     * ":sort".
     */
    /*private*/ static final ex_func_C ex_sort = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int count = (int)(eap.line2 - eap.line1 + 1);

            /* Sorting one line is really quick! */
            if (count <= 1)
                return;

            if (!u_save(eap.line1 - 1, eap.line2 + 1))
                return;

            sortbuf1 = null;
            sortbuf2 = null;

            sortend:
            {
                regmatch_C regmatch = new regmatch_C();
                regmatch.regprog = null;

                sorti_C[] nrs = ARRAY_sorti(count);

                int sort_oct = 0;               /* sort on octal number */
                int sort_hex = 0;               /* sort on hex number */

                sort_abort = sort_ic = sort_rx = false;
                sort_nr = 0;

                boolean unique = false;

                for (Bytes p = eap.arg; p.at(0) != NUL; p = p.plus(1))
                {
                    if (vim_iswhite(p.at(0)))
                        ;
                    else if (p.at(0) == (byte)'i')
                        sort_ic = true;
                    else if (p.at(0) == (byte)'r')
                        sort_rx = true;
                    else if (p.at(0) == (byte)'n')
                        sort_nr = 2;
                    else if (p.at(0) == (byte)'o')
                        sort_oct = 2;
                    else if (p.at(0) == (byte)'x')
                        sort_hex = 2;
                    else if (p.at(0) == (byte)'u')
                        unique = true;
                    else if (p.at(0) == (byte)'"')     /* comment start */
                        break;
                    else if (check_nextcmd(p) != null)
                    {
                        eap.nextcmd = check_nextcmd(p);
                        break;
                    }
                    else if (!asc_isalpha(p.at(0)) && regmatch.regprog == null)
                    {
                        Bytes s = skip_regexp(p.plus(1), p.at(0), true, null);
                        if (s.at(0) != p.at(0))
                        {
                            emsg(e_invalpat);
                            break sortend;
                        }
                        s.be(0, NUL);
                        /* Use last search pattern if sort pattern is empty. */
                        if (BEQ(s, p.plus(1)))
                        {
                            if (last_search_pat() == null)
                            {
                                emsg(e_noprevre);
                                break sortend;
                            }
                            regmatch.regprog = vim_regcomp(last_search_pat(), RE_MAGIC);
                        }
                        else
                            regmatch.regprog = vim_regcomp(p.plus(1), RE_MAGIC);
                        if (regmatch.regprog == null)
                            break sortend;
                        p = s;              /* continue after the regexp */
                        regmatch.rm_ic = p_ic[0];
                    }
                    else
                    {
                        emsg2(e_invarg2, p);
                        break sortend;
                    }
                }

                /* Can only have one of 'n', 'o' and 'x'. */
                if (2 < sort_nr + sort_oct + sort_hex)
                {
                    emsg(e_invarg);
                    break sortend;
                }

                /* From here on "sort_nr" is used as a flag for any number sorting. */
                sort_nr += sort_oct + sort_hex;

                int maxlen = 0;
                /*
                 * Make an array with all line numbers.
                 * This avoids having to copy all the lines into allocated memory.
                 * When sorting on strings "start_col_nr" is the offset in the line,
                 * for numbers sorting it's the number to sort on.
                 * This means the pattern matching and number conversion only has to be done once per line.
                 * Also get the longest line length for allocating "sortbuf".
                 */
                long lnum;
                for (lnum = eap.line1; lnum <= eap.line2; lnum++)
                {
                    Bytes s = ml_get(lnum);
                    int len = strlen(s);
                    if (maxlen < len)
                        maxlen = len;

                    int start_col = 0;
                    int end_col = len;
                    if (regmatch.regprog != null && vim_regexec(regmatch, s, 0))
                    {
                        if (sort_rx)
                        {
                            start_col = BDIFF(regmatch.startp[0], s);
                            end_col = BDIFF(regmatch.endp[0], s);
                        }
                        else
                            start_col = BDIFF(regmatch.endp[0], s);
                    }
                    else if (regmatch.regprog != null)
                        end_col = 0;

                    if (sort_nr != 0)
                    {
                        /* Make sure vim_str2nr() doesn't read any digits past the end
                         * of the match, by temporarily terminating the string there. */
                        Bytes s2 = s.plus(end_col);
                        byte c = s2.at(0);
                        s2.be(0, NUL);
                        /* Sorting on number: Store the number itself. */
                        Bytes p = s.plus(start_col);
                        if (sort_hex != 0)
                            s = skiptohex(p);
                        else
                            s = skiptodigit(p);
                        if (BLT(p, s) && s.at(-1) == (byte)'-')
                            s = s.minus(1);    /* include preceding negative sign */
                        if (s.at(0) == NUL)
                            /* empty line should sort before any number */
                            nrs[(int)(lnum - eap.line1)].start_col_nr = -MAXCOL;
                        else
                        {
                            long[] __ = { nrs[(int)(lnum - eap.line1)].start_col_nr };
                            vim_str2nr(s, null, null, sort_oct, sort_hex, __);
                            nrs[(int)(lnum - eap.line1)].start_col_nr = (int)__[0];
                        }
                        s2.be(0, c);
                    }
                    else
                    {
                        /* Store the column to sort at. */
                        nrs[(int)(lnum - eap.line1)].start_col_nr = start_col;
                        nrs[(int)(lnum - eap.line1)].end_col_nr = end_col;
                    }

                    nrs[(int)(lnum - eap.line1)].lnum = lnum;

                    if (regmatch.regprog != null)
                        fast_breakcheck();
                    if (got_int)
                        break sortend;
                }

                /* Allocate a buffer that can hold the longest line. */
                sortbuf1 = new Bytes(maxlen + 1);
                sortbuf2 = new Bytes(maxlen + 1);

                /* Sort the array of line numbers.  Note: can't be interrupted! */
                Arrays.sort(nrs, 0, count, sort_compare);

                if (sort_abort)
                    break sortend;

                /* Insert the lines in the sorted order below the last one. */
                lnum = eap.line2;

                int i;
                for (i = 0; i < count; i++)
                {
                    Bytes s = ml_get(nrs[eap.forceit ? count - i - 1 : i].lnum);
                    if (!unique || i == 0 || (sort_ic ? STRCASECMP(s, sortbuf1) : STRCMP(s, sortbuf1)) != 0)
                    {
                        if (!ml_append(lnum++, s, 0, false))
                            break;
                        if (unique)
                            STRCPY(sortbuf1, s);
                    }
                    fast_breakcheck();
                    if (got_int)
                        break sortend;
                }

                /* delete the original lines if appending worked */
                if (i == count)
                {
                    for (i = 0; i < count; i++)
                        ml_delete(eap.line1, false);
                }
                else
                    count = 0;

                /* Adjust marks for deleted (or added) lines and prepare for displaying. */
                int deleted = count - (int)(lnum - eap.line2);
                if (0 < deleted)
                    mark_adjust(eap.line2 - deleted, eap.line2, MAXLNUM, -deleted);
                else if (deleted < 0)
                    mark_adjust(eap.line2, MAXLNUM, -deleted, 0L);
                changed_lines(eap.line1, 0, eap.line2 + 1, -deleted);

                curwin.w_cursor.lnum = eap.line1;
                beginline(BL_WHITE | BL_FIX);
            }

            sortbuf2 = null;
            sortbuf1 = null;

            if (got_int)
                emsg(e_interr);
        }
    };

    /*
     * ":retab".
     */
    /*private*/ static final ex_func_C ex_retab = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            boolean got_tab = false;
            int num_spaces = 0;
            int start_col = 0;                     /* for start of white-space string */
            int start_vcol = 0;                    /* for start of white-space string */
            long first_line = 0;                    /* first changed line */
            long last_line = 0;                     /* last changed line */

            boolean save_list = curwin.w_onebuf_opt.wo_list[0];
            curwin.w_onebuf_opt.wo_list[0] = false;       /* don't want list mode here */

            int new_ts;
            { Bytes[] __ = { eap.arg }; new_ts = (int)getdigits(__); eap.arg = __[0]; }
            if (new_ts < 0)
            {
                emsg(e_positive);
                return;
            }
            if (new_ts == 0)
                new_ts = (int)curbuf.b_p_ts[0];

            loop:
            for (long lnum = eap.line1; !got_int && lnum <= eap.line2; lnum++)
            {
                Bytes ptr = ml_get(lnum);
                int col = 0;
                int vcol = 0;
                boolean did_undo = false;           /* called u_save() for current line */
                for ( ; ; )
                {
                    if (vim_iswhite(ptr.at(col)))
                    {
                        if (!got_tab && num_spaces == 0)
                        {
                            /* First consecutive white-space. */
                            start_vcol = vcol;
                            start_col = col;
                        }
                        if (ptr.at(col) == (byte)' ')
                            num_spaces++;
                        else
                            got_tab = true;
                    }
                    else
                    {
                        if (got_tab || (eap.forceit && 1 < num_spaces))
                        {
                            /* Retabulate this string of white-space. */

                            /* len is virtual length of white string */
                            int len = num_spaces = vcol - start_vcol;
                            int num_tabs = 0;
                            if (!curbuf.b_p_et[0])
                            {
                                int temp = new_ts - (start_vcol % new_ts);
                                if (temp <= num_spaces)
                                {
                                    num_spaces -= temp;
                                    num_tabs++;
                                }
                                num_tabs += num_spaces / new_ts;
                                num_spaces -= (num_spaces / new_ts) * new_ts;
                            }
                            if (curbuf.b_p_et[0] || got_tab || (num_spaces + num_tabs < len))
                            {
                                if (did_undo == false)
                                {
                                    did_undo = true;
                                    if (!u_save(lnum - 1, lnum + 1))
                                        break loop;        /* out-of-memory ??? */
                                }

                                /* len is actual number of white characters used */
                                len = num_spaces + num_tabs;
                                int old_len = strlen(ptr);
                                Bytes new_line = new Bytes(old_len - col + start_col + len + 1);

                                if (0 < start_col)
                                    BCOPY(new_line, ptr, start_col);
                                BCOPY(new_line, start_col + len, ptr, col, old_len - col + 1);
                                ptr = new_line.plus(start_col);
                                for (col = 0; col < len; col++)
                                    ptr.be(col, (col < num_tabs) ? (byte)'\t' : (byte)' ');
                                ml_replace(lnum, new_line, false);
                                if (first_line == 0)
                                    first_line = lnum;
                                last_line = lnum;
                                ptr = new_line;
                                col = start_col + len;
                            }
                        }
                        got_tab = false;
                        num_spaces = 0;
                    }
                    if (ptr.at(col) == NUL)
                        break;
                    vcol += chartabsize(ptr.plus(col), vcol);
                    col += us_ptr2len_cc(ptr.plus(col));
                }
                line_breakcheck();
            }
            if (got_int)
                emsg(e_interr);

            if (curbuf.b_p_ts[0] != new_ts)
                redraw_curbuf_later(NOT_VALID);
            if (first_line != 0)
                changed_lines(first_line, 0, last_line + 1, 0L);

            curwin.w_onebuf_opt.wo_list[0] = save_list;       /* restore 'list' */

            curbuf.b_p_ts[0] = new_ts;
            coladvance(curwin.w_curswant);

            u_clearline();
        }
    };

    /*
     * :move command - move lines line1-line2 to line dest
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean do_move(long line1, long line2, long dest)
    {
        if (line1 <= dest && dest < line2)
        {
            emsg(u8("E134: Move lines into themselves"));
            return false;
        }

        long num_lines = line2 - line1 + 1;

        /*
         * First we copy the old text to its new location.
         * Also copy the flag that ":global" command uses.
         */
        if (!u_save(dest, dest + 1))
            return false;

        long l;

        long extra; /* num lines added before line1 */
        for (extra = 0, l = line1; l <= line2; l++)
        {
            Bytes str = STRDUP(ml_get(l + extra));
            ml_append(dest + l - line1, str, 0, false);
            if (dest < line1)
                extra++;
        }

        /*
         * Now we must be careful adjusting our marks so that we don't overlap our
         * mark_adjust() calls.
         *
         * We adjust the marks within the old text so that they refer to the last lines
         * of the file (temporarily), because we know no other marks will be set there
         * since these line numbers did not exist until we added our new lines.
         *
         * Then we adjust the marks on lines between the old and new text positions
         * (either forwards or backwards).
         *
         * And finally we adjust the marks we put at the end of the file back to
         * their final destination at the new text position.
         */
        long last_line = curbuf.b_ml.ml_line_count; /* last line in file after adding new text */
        mark_adjust(line1, line2, last_line - line2, 0L);
        changed_lines(last_line - num_lines + 1, 0, last_line + 1, num_lines);
        if (line2 <= dest)
        {
            mark_adjust(line2 + 1, dest, -num_lines, 0L);
            curbuf.b_op_start.lnum = dest - num_lines + 1;
            curbuf.b_op_end.lnum = dest;
        }
        else
        {
            mark_adjust(dest + 1, line1 - 1, num_lines, 0L);
            curbuf.b_op_start.lnum = dest + 1;
            curbuf.b_op_end.lnum = dest + num_lines;
        }
        curbuf.b_op_start.col = curbuf.b_op_end.col = 0;
        mark_adjust(last_line - num_lines + 1, last_line, -(last_line - dest - extra), 0L);
        changed_lines(last_line - num_lines + 1, 0, last_line + 1, -extra);

        /*
         * Now we delete the original text.
         */
        if (!u_save(line1 + extra - 1, line2 + extra + 1))
            return false;

        for (l = line1; l <= line2; l++)
            ml_delete(line1 + extra, true);

        if (global_busy == 0 && p_report[0] < num_lines)
        {
            if (num_lines == 1)
                msg(u8("1 line moved"));
            else
                smsg(u8("%ld lines moved"), num_lines);
        }

        /*
         * Leave the cursor on the last of the moved lines.
         */
        if (line1 <= dest)
            curwin.w_cursor.lnum = dest;
        else
            curwin.w_cursor.lnum = dest + (line2 - line1) + 1;

        if (line1 < dest)
        {
            dest += num_lines + 1;
            last_line = curbuf.b_ml.ml_line_count;
            if (dest > last_line + 1)
                dest = last_line + 1;
            changed_lines(line1, 0, dest, 0L);
        }
        else
            changed_lines(dest + 1, 0, line1 + num_lines, 0L);

        return true;
    }

    /*
     * ":copy"
     */
    /*private*/ static void ex_copy(long line1, long line2, long n)
    {
        long count = line2 - line1 + 1;

        curbuf.b_op_start.lnum = n + 1;
        curbuf.b_op_end.lnum = n + count;
        curbuf.b_op_start.col = curbuf.b_op_end.col = 0;

        /*
         * there are three situations:
         * 1. destination is above line1
         * 2. destination is between line1 and line2
         * 3. destination is below line2
         *
         * n = destination (when starting)
         * curwin.w_cursor.lnum = destination (while copying)
         * line1 = start of source (while copying)
         * line2 = end of source (while copying)
         */
        if (!u_save(n, n + 1))
            return;

        curwin.w_cursor.lnum = n;
        while (line1 <= line2)
        {
            /* need to use STRDUP() because the line will be unlocked within ml_append() */
            Bytes p = STRDUP(ml_get(line1));
            ml_append(curwin.w_cursor.lnum, p, 0, false);
            /* situation 2: skip already copied lines */
            if (line1 == n)
                line1 = curwin.w_cursor.lnum;
            line1++;
            if (curwin.w_cursor.lnum < line1)
                line1++;
            if (curwin.w_cursor.lnum < line2)
                line2++;
            curwin.w_cursor.lnum++;
        }

        appended_lines_mark(n, count);

        msgmore(count);
    }

    /*private*/ static Bytes prevcmd;          /* the previous command */

    /*
     * Handle the ":!cmd" command.  Also for ":r !cmd" and ":w !cmd"
     * Bangs in the argument are replaced with the previously entered command.
     * Remember the argument.
     */
    /*private*/ static void do_bang(int addr_count, exarg_C eap, boolean forceit, boolean do_in, boolean do_out)
    {
        Bytes arg = eap.arg;               /* command */
        long line1 = eap.line1;             /* start of range */
        long line2 = eap.line2;             /* end of range */
        Bytes newcmd = null;               /* the new command */
        boolean scroll_save = msg_scroll;

        /*
         * Disallow shell commands for "rvim".
         * Disallow shell commands from .exrc and .vimrc in current directory for security reasons.
         */
        if (check_restricted() || check_secure())
            return;

        if (addr_count == 0)                /* :! */
        {
            msg_scroll = false;         /* don't scroll here */
            autowrite_all();
            msg_scroll = scroll_save;
        }

        /*
         * Try to find an embedded bang, like in :!<cmd> ! [args]
         * (:!! is indicated by the 'forceit' variable)
         */
        boolean ins_prevcmd = forceit;
        Bytes trailarg = arg;
        do
        {
            int len = strlen(trailarg) + 1;
            if (newcmd != null)
                len += strlen(newcmd);
            if (ins_prevcmd)
            {
                if (prevcmd == null)
                {
                    emsg(e_noprev);
                    return;
                }
                len += strlen(prevcmd);
            }
            Bytes t = new Bytes(len);
            t.be(0, NUL);
            if (newcmd != null)
                STRCAT(t, newcmd);
            if (ins_prevcmd)
                STRCAT(t, prevcmd);
            Bytes p = t.plus(strlen(t));
            STRCAT(t, trailarg);
            newcmd = t;

            /*
             * Scan the rest of the argument for '!', which is replaced by the
             * previous command.  "\!" is replaced by "!" (this is vi compatible).
             */
            trailarg = null;
            while (p.at(0) != NUL)
            {
                if (p.at(0) == (byte)'!')
                {
                    if (BLT(newcmd, p) && p.at(-1) == (byte)'\\')
                        BCOPY(p, -1, p, 0, strlen(p) + 1);
                    else
                    {
                        trailarg = p;
                        (trailarg = trailarg.plus(1)).be(-1, NUL);
                        ins_prevcmd = true;
                        break;
                    }
                }
                p = p.plus(1);
            }
        } while (trailarg != null);

        prevcmd = newcmd;

        if (bangredo)           /* put cmd in redo buffer for ! command */
        {
            /* If % or # appears in the command, it must have been escaped.
             * Reescape them, so that redoing them does not substitute them by the buffername. */
            Bytes cmd = vim_strsave_escaped(prevcmd, u8("%#"));

            appendToRedobuffLit(cmd, -1);
            appendToRedobuff(u8("\n"));

            bangredo = false;
        }

        if (addr_count == 0)                /* :! */
        {
            /* echo the command */
            msg_start();
            msg_putchar(':');
            msg_putchar('!');
            msg_outtrans(newcmd);
            msg_clr_eos();
            windgoto(msg_row, msg_col);

            do_shell(newcmd, 0);
        }
        else                                /* :range! */
        {
            /* Careful: This may recursively call do_bang() again! (because of autocommands). */
            do_filter(line1, line2, newcmd, do_in, do_out);
            apply_autocmds(EVENT_SHELLFILTERPOST, null, null, false, curbuf);
        }
    }

    /*
     * do_filter: filter lines through a command given by the user
     *
     * We mostly use temp files and the call_shell() routine here.  This would
     * normally be done using pipes on a UNIX machine, but this is more portable
     * to non-unix machines.  The call_shell() routine needs to be able
     * to deal with redirection somehow, and should handle things like looking
     * at the PATH env. variable, and adding reasonable extensions to the
     * command name given by the user.  All reasonable versions of call_shell() do this.
     * Alternatively, if on Unix and redirecting input or output, but not both,
     * and the 'shelltemp' option isn't set, use pipes.
     * We use input redirection if do_in is true.
     * We use output redirection if do_out is true.
     */
    /*private*/ static void do_filter(long line1, long line2, Bytes cmd, boolean do_in, boolean do_out)
    {
        buffer_C old_curbuf = curbuf;

        if (cmd.at(0) == NUL)        /* no filter command */
            return;

        pos_C cursor_save = new pos_C();
        COPY_pos(cursor_save, curwin.w_cursor);

        long linecount = line2 - line1 + 1;
        curwin.w_cursor.lnum = line1;
        curwin.w_cursor.col = 0;
        changed_line_abv_curs();
        invalidate_botline();

        boolean error = false;

        filterend:
        {
            /*
             * When using temp files:
             * 1. * Form temp file names
             * 2. * Write the lines to a temp file
             * 3.   Run the filter command on the temp file
             * 4. * Read the output of the command into the buffer
             * 5. * Delete the original lines to be filtered
             * 6. * Remove the temp files
             *
             * When writing the input with a pipe or when catching the output with a
             * pipe only need to do 3.
             */

            int shell_flags = 0;
            if (do_out)
                shell_flags |= SHELL_DOOUT;

            if (!do_in && do_out)
            {
                /* Use a pipe to fetch stdout of the command, do not use a temp file. */
                shell_flags |= SHELL_READ;
                curwin.w_cursor.lnum = line2;
            }
            else if (do_in && !do_out)
            {
                /* Use a pipe to write stdin of the command, do not use a temp file. */
                shell_flags |= SHELL_WRITE;
                curbuf.b_op_start.lnum = line1;
                curbuf.b_op_end.lnum = line2;
            }
            else if (do_in && do_out)
            {
                /* Use a pipe to write stdin and fetch stdout of the command, do not use a temp file. */
                shell_flags |= SHELL_READ|SHELL_WRITE;
                curbuf.b_op_start.lnum = line1;
                curbuf.b_op_end.lnum = line2;
                curwin.w_cursor.lnum = line2;
            }
            else if (do_in || do_out)
            {
                emsg(e_notmp);
                break filterend;
            }

            no_wait_return++;           /* don't call wait_return() while busy */

            if (curbuf != old_curbuf)
                break filterend;

            if (!do_out)
                msg_putchar('\n');

            /* Create the shell command in allocated memory. */
            Bytes cmd_buf = new Bytes(strlen(cmd) + 3);         /* "()" + NUL */
            STRCPY(cmd_buf, cmd);

            windgoto((int)Rows[0] - 1, 0);
            cursor_on();

            /*
             * When not redirecting the output,
             * the command can write anything to the screen.
             * If p_srr is equal to ">", screen may be messed up by stderr.
             * If do_in is false, this could be something like ":r !cat",
             * which may also mess up the screen.
             * Clear the screen later.
             */
            if (!do_out || /*STRCMP(p_srr, u8(">")) == 0*/false || !do_in)
                redraw_later_clear();

            if (do_out)
            {
                if (!u_save(line2, line2 + 1))
                {
                    error = true;
                    break filterend;
                }

                redraw_curbuf_later(VALID);
            }

            long read_linecount = curbuf.b_ml.ml_line_count;

            /*
             * When call_shell() fails, wait_return() is called to give the user a chance
             * to read the error messages.  Otherwise errors are ignored, so you can see
             * the error messages from the command that appear on stdout.
             * Use 'u' to fix the text.
             * Switch to cooked mode when not redirecting stdin, avoids that something
             * like ":r !cat" hangs.
             * Pass on the SHELL_DOOUT flag when the output is being redirected.
             */
            if (call_shell(cmd_buf, SHELL_FILTER | SHELL_COOKED | shell_flags) != 0)
            {
                redraw_later_clear();
                wait_return(FALSE);
            }

            did_check_timestamps = false;
            need_check_timestamps = true;

            /* When interrupting the shell command, it may still have produced some useful output.
             * Reset got_int here, so that readfile() won't cancel reading. */
            ui_breakcheck();
            got_int = false;

            if (do_out)
            {
                read_linecount = curbuf.b_ml.ml_line_count - read_linecount;

                if ((shell_flags & SHELL_READ) != 0)
                {
                    curbuf.b_op_start.lnum = line2 + 1;
                    curbuf.b_op_end.lnum = curwin.w_cursor.lnum;
                    appended_lines_mark(line2, read_linecount);
                }

                if (do_in)
                {
                    if (cmdmod.keepmarks || vim_strbyte(p_cpo[0], CPO_REMMARK) == null)
                    {
                        if (linecount <= read_linecount)
                            /* move all marks from old lines to new lines */
                            mark_adjust(line1, line2, linecount, 0L);
                        else
                        {
                            /* move marks from old lines to new lines,
                             * delete marks that are in deleted lines */
                            mark_adjust(line1, line1 + read_linecount - 1, linecount, 0L);
                            mark_adjust(line1 + read_linecount, line2, MAXLNUM, 0L);
                        }
                    }

                    /*
                     * Put cursor on first filtered line for ":range!cmd".
                     * Adjust '[ and '] (set by buf_write()).
                     */
                    curwin.w_cursor.lnum = line1;
                    del_lines(linecount, true);
                    curbuf.b_op_start.lnum -= linecount;    /* adjust '[ */
                    curbuf.b_op_end.lnum -= linecount;      /* adjust '] */
                    write_lnum_adjust(-linecount);          /* adjust last line for next write */
                }
                else
                {
                    /*
                     * Put cursor on last new line for ":r !cmd".
                     */
                    linecount = curbuf.b_op_end.lnum - curbuf.b_op_start.lnum + 1;
                    curwin.w_cursor.lnum = curbuf.b_op_end.lnum;
                }

                beginline(BL_WHITE | BL_FIX);       /* cursor on first non-blank */
                --no_wait_return;

                if (p_report[0] < linecount)
                {
                    if (do_in)
                    {
                        vim_snprintf(msg_buf, MSG_BUF_LEN, u8("%ld lines filtered"), linecount);
                        if (msg(msg_buf) && !msg_scroll)
                            /* save message to display it after redraw */
                            set_keep_msg(msg_buf, 0);
                    }
                    else
                        msgmore(linecount);
                }
            }
            else
            {
                error = true;
                break filterend;
            }
        }

        if (error)
        {
            /* put cursor back in same position for ":w !cmd" */
            COPY_pos(curwin.w_cursor, cursor_save);
            --no_wait_return;
            wait_return(FALSE);
        }

        if (curbuf != old_curbuf)
        {
            --no_wait_return;
            emsg(u8("E135: *Filter* Autocommands must not change current buffer"));
        }
    }

    /*
     * Call a shell to execute a command.
     * When "cmd" is null start an interactive shell.
     */
    /*private*/ static void do_shell(Bytes cmd, int flags)
        /* flags: may be SHELL_DOOUT when output is redirected */
    {
        /*
         * Disallow shell commands for "rvim".
         * Disallow shell commands from .exrc and .vimrc in current directory for security reasons.
         */
        if (check_restricted() || check_secure())
        {
            msg_end();
            return;
        }

        /*
         * For autocommands we want to get the output on the current screen,
         * to avoid having to type return below.
         */
        msg_putchar('\r');              /* put cursor at start of line */
        if (!autocmd_busy)
        {
            stoptermcap();
        }
        msg_putchar('\n');              /* may shift screen one line up */

        /* warning message before calling the shell */
        if (p_warn[0] && !autocmd_busy && msg_silent == 0)
            for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
                if (bufIsChanged(buf))
                {
                    msg_puts(u8("[No write since last change]\n"));
                    break;
                }

        /* This windgoto is required for when the '\n' resulted
         * in a "delete line 1" command to the terminal. */
        if (!swapping_screen())
            windgoto(msg_row, msg_col);
        cursor_on();
        call_shell(cmd, SHELL_COOKED | flags);
        did_check_timestamps = false;
        need_check_timestamps = true;

        /*
         * put the message cursor at the end of the screen, avoids wait_return()
         * to overwrite the text that the external command showed
         */
        if (!swapping_screen())
        {
            msg_row = (int)Rows[0] - 1;
            msg_col = 0;
        }

        if (autocmd_busy)
        {
            if (msg_silent == 0)
                redraw_later_clear();
        }
        else
        {
            /*
             * For ":sh" there is no need to call wait_return(), just redraw.
             * Otherwise there is probably text on the screen that the user wants
             * to read before redrawing, so call wait_return().
             */
            if (cmd == null)
            {
                if (msg_silent == 0)
                    redraw_later_clear();
                need_wait_return = false;
            }
            else
            {
                /*
                 * If we switch screens when starttermcap() is called, we really
                 * want to wait for "hit return to continue".
                 */
                int save_nwr = no_wait_return;
                if (swapping_screen())
                    no_wait_return = FALSE;
                wait_return((msg_silent == 0) ? TRUE : FALSE);
                no_wait_return = save_nwr;
            }

            starttermcap();     /* start termcap if not done by wait_return() */
        }

        /* display any error messages now */
        libc.fflush(stderr);

        apply_autocmds(EVENT_SHELLCMDPOST, null, null, false, curbuf);
    }

    /*
     * Implementation of ":fixdel", also used by get_stty().
     *  <BS>    resulting <Del>
     *   ^?         ^H
     * not ^?       ^?
     */
    /*private*/ static final ex_func_C ex_fixdel = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            Bytes p = find_termcode(u8("kb"));
            add_termcode(u8("kD"), (p != null && p.at(0) == DEL) ? CTRL_H_STR : DEL_STR, FALSE);
        }
    };

    /*private*/ static void print_line_no_prefix(long lnum, boolean use_number, boolean list)
    {
        if (curwin.w_onebuf_opt.wo_nu[0] || use_number)
        {
            Bytes numbuf = new Bytes(30);

            vim_snprintf(numbuf, numbuf.size(), u8("%*ld "), number_width(curwin), lnum);
            msg_puts_attr(numbuf, hl_attr(HLF_N));      /* Highlight line nrs */
        }
        msg_prt_line(ml_get(lnum), list);
    }

    /*
     * Print a text line.  Also in silent mode ("ex -s").
     */
    /*private*/ static void print_line(long lnum, boolean use_number, boolean list)
    {
        boolean save_silent = silent_mode;

        msg_start();
        silent_mode = false;
        info_message = true;
        print_line_no_prefix(lnum, use_number, list);
        if (save_silent)
        {
            msg_putchar('\n');
            cursor_on();            /* msg_start() switches it off */
            out_flush();
            silent_mode = save_silent;
        }
        info_message = false;
    }

    /*
     * ":update".
     */
    /*private*/ static final ex_func_C ex_update = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (curbufIsChanged())
                do_write(eap);
        }
    };

    /*
     * ":write" and ":saveas".
     */
    /*private*/ static final ex_func_C ex_write = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.usefilter)      /* input lines to shell command */
                do_bang(1, eap, false, true, false);
            else
                do_write(eap);
        }
    };

    /*
     * write current buffer to file 'eap.arg'
     * if 'eap.append' is true, append to the file
     *
     * if eap.arg[0] == NUL write to current file
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean do_write(exarg_C eap)
    {
        boolean retval = false;

        Bytes fname = null;
        Bytes free_fname = null;
        buffer_C alt_buf = null;

        if (not_writing())              /* check 'write' option */
            return false;

        boolean other;

        Bytes ffname = eap.arg;
        if (ffname.at(0) == NUL)
        {
            if (eap.cmdidx == CMD_saveas)
            {
                emsg(e_argreq);
                return retval;
            }
            other = false;
        }
        else
        {
            fname = ffname;
            free_fname = fullName_save(ffname, true);
            /*
             * When out-of-memory, keep unexpanded file name,
             * because we MUST be able to write the file in this situation.
             */
            if (free_fname != null)
                ffname = free_fname;
            other = otherfile(ffname);
        }

        /*
         * If we have a new file, put its name in the list of alternate file names.
         */
        if (other)
        {
            if (vim_strbyte(p_cpo[0], CPO_ALTWRITE) != null || eap.cmdidx == CMD_saveas)
                alt_buf = setaltfname(ffname, fname, 1);
            else
                alt_buf = buflist_findname(ffname);
            if (alt_buf != null && alt_buf.b_ml.ml_mfp != null)
            {
                /* Overwriting a file that is loaded in another buffer is not a good idea. */
                emsg(e_bufloaded);
                return retval;
            }
        }

        /*
         * Writing to the current file is not allowed in readonly mode and a file name is required.
         * "nofile" and "nowrite" buffers cannot be written implicitly either.
         */
        if (!other)
        {
            if (check_fname() == false)
                return retval;
            boolean b;
            { boolean[] __ = { eap.forceit }; b = check_readonly(__, curbuf); eap.forceit = __[0]; }
            if (b)
                return retval;
        }

        if (!other)
        {
            ffname = curbuf.b_ffname;
            fname = curbuf.b_fname;
            /*
             * Not writing the whole file is only allowed with '!'.
             */
            if ((eap.line1 != 1 || eap.line2 != curbuf.b_ml.ml_line_count)
                    && !eap.forceit
                    && !eap.append
                    && !p_wa[0])
            {
                if (p_confirm[0] || cmdmod.confirm)
                {
                    if (vim_dialog_yesno(u8("Write partial file?"), 2) != VIM_YES)
                        return retval;
                    eap.forceit = true;
                }
                else
                {
                    emsg(u8("E140: Use ! to write partial buffer"));
                    return retval;
                }
            }
        }

        if (check_overwrite(eap, curbuf, fname, ffname, other) == true)
        {
            if (eap.cmdidx == CMD_saveas && alt_buf != null)
            {
                buffer_C was_curbuf = curbuf;

                apply_autocmds(EVENT_BUFFILEPRE, null, null, false, curbuf);
                apply_autocmds(EVENT_BUFFILEPRE, null, null, false, alt_buf);
                if (curbuf != was_curbuf || aborting())
                {
                    /* buffer changed, don't change name now */
                    return false;
                }
                /* Exchange the file names for the current and the alternate buffer.
                 * This makes it look like we are now editing the buffer under the new name.
                 * Must be done before buf_write(), because if there is no file name
                 * and 'cpo' contains 'F', it will set the file name. */
                fname = alt_buf.b_fname;
                alt_buf.b_fname = curbuf.b_fname;
                curbuf.b_fname = fname;
                fname = alt_buf.b_ffname;
                alt_buf.b_ffname = curbuf.b_ffname;
                curbuf.b_ffname = fname;
                fname = alt_buf.b_sfname;
                alt_buf.b_sfname = curbuf.b_sfname;
                curbuf.b_sfname = fname;
                buf_name_changed(curbuf);
                apply_autocmds(EVENT_BUFFILEPOST, null, null, false, curbuf);
                apply_autocmds(EVENT_BUFFILEPOST, null, null, false, alt_buf);
                if (!alt_buf.b_p_bl[0])
                {
                    alt_buf.b_p_bl[0] = true;
                    apply_autocmds(EVENT_BUFADD, null, null, false, alt_buf);
                }
                if (curbuf != was_curbuf || aborting())
                {
                    /* buffer changed, don't write the file */
                    return false;
                }

                /* If 'filetype' was empty try detecting it now. */
                if (curbuf.b_p_ft[0].at(0) == NUL)
                {
                    if (au_has_group(u8("filetypedetect")))
                        do_doautocmd(u8("filetypedetect BufRead"), true);
                }

                /* Autocommands may have changed buffer names, esp. when 'autochdir' is set. */
                fname = curbuf.b_sfname;
            }

            retval = buf_write(curbuf, ffname, fname, eap.line1, eap.line2,
                                     eap, eap.append, eap.forceit, true, false);

            /* After ":saveas fname" reset 'readonly'. */
            if (eap.cmdidx == CMD_saveas)
            {
                if (retval == true)
                {
                    curbuf.b_p_ro[0] = false;
                    redraw_tabline = true;
                }
            }
        }

        return retval;
    }

    /*
     * Check if it is allowed to overwrite a file.  If b_flags has BF_NOTEDITED,
     * BF_NEW or BF_READERR, check for overwriting current file.
     * May set eap.forceit if a dialog says it's OK to overwrite.
     * Return true if it's OK, false if it is not.
     */
    /*private*/ static boolean check_overwrite(exarg_C eap, buffer_C buf, Bytes fname, Bytes ffname, boolean other)
        /* fname: file name to be used (can differ from buf.ffname) */
        /* ffname: full path version of fname */
        /* other: writing under other name */
    {
        /*
         * write to other file or b_flags set or not writing the whole file:
         * overwriting only allowed with '!'
         */
        if ((other
                   || (buf.b_flags & BF_NOTEDITED) != 0
                   || ((buf.b_flags & BF_NEW) != 0 && vim_strbyte(p_cpo[0], CPO_OVERNEW) == null)
                   || (buf.b_flags & BF_READERR) != 0)
                && !p_wa[0]
                && vim_fexists(ffname))
        {
            if (!eap.forceit && !eap.append)
            {
                /* with UNIX it is possible to open a directory */
                if (mch_isdir(ffname))
                {
                    emsg2(e_isadir2, ffname);
                    return false;
                }
                if (p_confirm[0] || cmdmod.confirm)
                {
                    Bytes buff = new Bytes(DIALOG_MSG_SIZE);

                    dialog_msg(buff, u8("Overwrite existing file \"%s\"?"), fname);
                    if (vim_dialog_yesno(buff, 2) != VIM_YES)
                        return false;
                    eap.forceit = true;
                }
                else
                {
                    emsg(e_exists);
                    return false;
                }
            }
        }

        return true;
    }

    /*
     * Handle ":wnext", ":wNext" and ":wprevious" commands.
     */
    /*private*/ static final ex_func_C ex_wnext = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int i;
            if (eap.cmd.at(1) == (byte)'n')
                i = curwin.w_arg_idx + (int)eap.line2;
            else
                i = curwin.w_arg_idx - (int)eap.line2;

            eap.line1 = 1;
            eap.line2 = curbuf.b_ml.ml_line_count;

            if (do_write(eap) != false)
                do_argfile(eap, i);
        }
    };

    /*
     * ":wall", ":wqall" and ":xall": Write all changed files (and exit).
     */
    /*private*/ static final ex_func_C ex_wqall = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int error = 0;
            boolean save_forceit = eap.forceit;

            if (eap.cmdidx == CMD_xall || eap.cmdidx == CMD_wqall)
                exiting = true;

            for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
            {
                if (bufIsChanged(buf))
                {
                    /*
                     * Check if there is a reason the buffer cannot be written:
                     * 1. if the 'write' option is set
                     * 2. if there is no file name (even after browsing)
                     * 3. if the 'readonly' is set (even after a dialog)
                     * 4. if overwriting is allowed (even after a dialog)
                     */
                    if (not_writing())
                    {
                        error++;
                        break;
                    }
                    if (buf.b_ffname == null)
                    {
                        emsgn(u8("E141: No file name for buffer %ld"), (long)buf.b_fnum);
                        error++;
                    }
                    else
                    {
                        boolean b;
                        { boolean[] __ = { eap.forceit }; b = check_readonly(__, buf); eap.forceit = __[0]; }
                        if (b || check_overwrite(eap, buf, buf.b_fname, buf.b_ffname, false) == false)
                        {
                            error++;
                        }
                        else
                        {
                            if (buf_write_all(buf, eap.forceit) == false)
                                error++;
                            /* an autocommand may have deleted the buffer */
                            if (!buf_valid(buf))
                                buf = firstbuf;
                        }
                    }
                    eap.forceit = save_forceit; /* check_overwrite() may set it */
                }
            }

            if (exiting)
            {
                if (error == 0)
                    getout(0);          /* exit Vim */
                not_exiting();
            }
        }
    };

    /*
     * Check the 'write' option.
     * Return true and give a message when it's not st.
     */
    /*private*/ static boolean not_writing()
    {
        if (p_write[0])
            return false;

        emsg(u8("E142: File not written: Writing is disabled by 'write' option"));
        return true;
    }

    /*
     * Check if a buffer is read-only (either 'readonly' option is set or file is read-only).
     * Ask for overruling in a dialog.
     * Return true and give an error message when the buffer is readonly.
     */
    /*private*/ static boolean check_readonly(boolean[] forceit, buffer_C buf)
    {
        stat_C st = new stat_C();

        /* Handle a file being readonly when the 'readonly' option is set or when the file exists
         * and permissions are read-only.  We will send 0777 to check_file_readonly(),
         * as the "perm" variable is important for device checks but not here. */
        if (!forceit[0] && (buf.b_p_ro[0] || (0 <= libC.stat(buf.b_ffname, st) && check_file_readonly(buf.b_ffname, 0777))))
        {
            if ((p_confirm[0] || cmdmod.confirm) && buf.b_fname != null)
            {
                Bytes buff = new Bytes(DIALOG_MSG_SIZE);

                if (buf.b_p_ro[0])
                    dialog_msg(buff, u8("'readonly' option is set for \"%s\".\nDo you wish to write anyway?"), buf.b_fname);
                else
                    dialog_msg(buff, u8("File permissions of \"%s\" are read-only.\nIt may still be possible to write it.\nDo you wish to try?"), buf.b_fname);

                if (vim_dialog_yesno(buff, 2) == VIM_YES)
                {
                    /* Set forceit, to force the writing of a readonly file. */
                    forceit[0] = true;
                    return false;
                }
                else
                    return true;
            }
            else if (buf.b_p_ro[0])
                emsg(e_readonly);
            else
                emsg2(u8("E505: \"%s\" is read-only (add ! to override)"), buf.b_fname);
            return true;
        }

        return false;
    }

    /* Use P_HID to check if a buffer is to be hidden when it is no longer visible in a window. */
    /*private*/ static boolean P_HID(buffer_C _dummy)
    {
        return (p_hid[0] || cmdmod.hide);
    }

    /*
     * Try to abandon current file and edit a new or existing file.
     * 'fnum' is the number of the file, if zero use ffname/sfname.
     *
     * Return 1 for "normal" error, 2 for "not written" error, 0 for success
     * -1 for successfully opening another file.
     * 'lnum' is the line number for the cursor in the new file (if non-zero).
     */
    /*private*/ static int getfile(int fnum, Bytes _ffname, Bytes _sfname, boolean setpm, long lnum, boolean forceit)
    {
        Bytes[] ffname = { _ffname };
        Bytes[] sfname = { _sfname };
        int retval;
        Bytes free_me = null;

        if (text_locked())
            return 1;
        if (curbuf_locked())
            return 1;

        boolean other;
        if (fnum == 0)
        {
            fname_expand(ffname, sfname);     /* make "ffname" full path, set "sfname" */
            other = otherfile(ffname[0]);
            free_me = ffname[0];                   /* has been allocated, free() later */
        }
        else
            other = (fnum != curbuf.b_fnum);

        if (other)
            no_wait_return++;                   /* don't wait for autowrite message */
        if (other && !forceit && curbuf.b_nwindows == 1 && !P_HID(curbuf)
                       && curbufIsChanged() && autowrite(curbuf, forceit) == false)
        {
            if (p_confirm[0] && p_write[0])
                dialog_changed(curbuf, false);
            if (curbufIsChanged())
            {
                if (other)
                    --no_wait_return;
                emsg(e_nowrtmsg);
                return 2; /* file has been changed */
            }
        }
        if (other)
            --no_wait_return;
        if (setpm)
            setpcmark();
        if (!other)
        {
            if (lnum != 0)
                curwin.w_cursor.lnum = lnum;
            check_cursor_lnum();
            beginline(BL_SOL | BL_FIX);
            retval = 0;     /* it's in the same file */
        }
        else if (do_ecmd(fnum, ffname[0], sfname[0], null, lnum,
                    (P_HID(curbuf) ? ECMD_HIDE : 0) + (forceit ? ECMD_FORCEIT : 0), curwin) == true)
            retval = -1;    /* opened another file */
        else
            retval = 1;     /* error encountered */

        return retval;
    }

    /*
     * start editing a new file
     *
     *     fnum: file number; if zero use ffname/sfname
     *   ffname: the file name
     *              - full path if sfname used,
     *              - any file name if sfname is null
     *              - empty string to re-edit with the same file name (but may be
     *                  in a different directory)
     *              - null to start an empty buffer
     *   sfname: the short file name (or null)
     *      eap: contains the command to be executed after loading the file and
     *           forced 'ff' and 'fenc'
     *  newlnum: if > 0: put cursor on this line number (if possible)
     *           if ECMD_LASTL: use last position in loaded file
     *           if ECMD_LAST: use last position in all files
     *           if ECMD_ONE: use first line
     *    flags:
     *         ECMD_HIDE: if true don't free the current buffer
     *       ECMD_OLDBUF: use existing buffer if it exists
     *      ECMD_FORCEIT: ! used for Ex command
     *       ECMD_ADDBUF: don't edit, just add to buffer list
     *   oldwin: Should be "curwin" when editing a new buffer in the current
     *           window, null when splitting the window first.  When not null info
     *           of the previous buffer for "oldwin" is stored.
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean do_ecmd(int fnum, Bytes ffname, Bytes sfname, exarg_C eap, long newlnum, int flags, window_C oldwin)
        /* eap: can be null! */
    {
        boolean[] retval = { false };

        boolean other_file;         /* true if editing another file */
        boolean oldbuf;             /* true if using existing buffer */
        boolean auto_buf = false;   /* true if autocommands brought us into the buffer unexpectedly */

        boolean did_set_swapcommand = false;
        buffer_C old_curbuf = curbuf;
        Bytes free_fname = null;
        long topline = 0;
        int newcol = -1;
        int solcol = -1;
        int readfile_flags = 0;

        Bytes command = null;
        if (eap != null)
            command = eap.do_ecmd_cmd;

        theend:
        {
            if (fnum != 0)
            {
                if (fnum == curbuf.b_fnum)              /* file is already being edited */
                    return true;                        /* nothing to do */
                other_file = true;
            }
            else
            {
                /* if no short name given, use "ffname" for short name */
                if (sfname == null)
                    sfname = ffname;

                if ((flags & ECMD_ADDBUF) != 0 && (ffname == null || ffname.at(0) == NUL))
                    break theend;

                if (ffname == null)
                    other_file = true;
                                                                /* there is no file name */
                else if (ffname.at(0) == NUL && curbuf.b_ffname == null)
                    other_file = false;
                else
                {
                    if (ffname.at(0) == NUL)                         /* re-edit with same file name */
                    {
                        ffname = curbuf.b_ffname;
                        sfname = curbuf.b_fname;
                    }
                    free_fname = fullName_save(ffname, true);   /* may expand to full path name */
                    if (free_fname != null)
                        ffname = free_fname;
                    other_file = otherfile(ffname);
                }
            }

            /*
             * if the file was changed we may not be allowed to abandon it
             * - if we are going to re-edit the same file
             * - or if we are the only window on this file and if ECMD_HIDE is false
             */
            if (((!other_file && (flags & ECMD_OLDBUF) == 0)
                    || (curbuf.b_nwindows == 1
                        && (flags & (ECMD_HIDE | ECMD_ADDBUF)) == 0))
                && check_changed(curbuf, (p_awa[0] ? CCGD_AW : 0)
                                    | (other_file ? 0 : CCGD_MULTWIN)
                                    | ((flags & ECMD_FORCEIT) != 0 ? CCGD_FORCEIT : 0)
                                    | (eap == null ? 0 : CCGD_EXCMD)))
            {
                if (fnum == 0 && other_file && ffname != null)
                    setaltfname(ffname, sfname, newlnum < 0 ? 0 : newlnum);
                break theend;
            }

            /*
             * End Visual mode before switching to another buffer, so the text can be
             * copied into the GUI selection buffer.
             */
            reset_VIsual();

            if ((command != null || 0 < newlnum) && get_vim_var_str(VV_SWAPCOMMAND).at(0) == NUL)
            {
                /* Set v:swapcommand for the SwapExists autocommands. */
                int len = (command != null) ? strlen(command) + 3 : 30;

                Bytes p = new Bytes(len);

                if (command != null)
                    vim_snprintf(p, len, u8(":%s\r"), command);
                else
                    vim_snprintf(p, len, u8("%ldG"), newlnum);
                set_vim_var_string(VV_SWAPCOMMAND, p, -1);
                did_set_swapcommand = true;
            }

            /*
             * If we are starting to edit another file, open a (new) buffer.
             * Otherwise we re-use the current buffer.
             */
            if (other_file)
            {
                if ((flags & ECMD_ADDBUF) == 0)
                {
                    if (!cmdmod.keepalt)
                        curwin.w_alt_fnum = curbuf.b_fnum;
                    if (oldwin != null)
                        buflist_altfpos(oldwin);
                }

                buffer_C buf;
                if (fnum != 0)
                    buf = buflist_findnr(fnum);
                else
                {
                    if ((flags & ECMD_ADDBUF) != 0)
                    {
                        long tlnum = 1L;

                        if (command != null)
                        {
                            tlnum = libC.atol(command);
                            if (tlnum <= 0)
                                tlnum = 1L;
                        }
                        buflist_new(ffname, sfname, tlnum, BLN_LISTED);
                        break theend;
                    }
                    buf = buflist_new(ffname, sfname, 0L, BLN_CURBUF | BLN_LISTED);
                    /* autocommands may change curwin and curbuf */
                    if (oldwin != null)
                        oldwin = curwin;
                    old_curbuf = curbuf;
                }
                if (buf == null)
                    break theend;
                if (buf.b_ml.ml_mfp == null)            /* no memfile yet */
                {
                    oldbuf = false;
                }
                else                                    /* existing memfile */
                {
                    oldbuf = true;
                    buf_check_timestamp(buf);
                    /* Check if autocommands made buffer invalid or changed the current buffer. */
                    if (!buf_valid(buf) || curbuf != old_curbuf)
                        break theend;
                    if (aborting())                     /* autocmds may abort script processing */
                        break theend;
                }

                /* May jump to last used line number for a loaded buffer or when asked for explicitly. */
                if ((oldbuf && newlnum == ECMD_LASTL) || newlnum == ECMD_LAST)
                {
                    pos_C pos = buflist_findfpos(buf);
                    newlnum = pos.lnum;
                    solcol = pos.col;
                }

                /*
                 * Make the (new) buffer the one used by the current window.
                 * If the old buffer becomes unused, free it if ECMD_HIDE is false.
                 * If the current buffer was empty and has no file name, curbuf
                 * is returned by buflist_new(), nothing to do here.
                 */
                if (buf != curbuf)
                {
                    /*
                     * Be careful: The autocommands may delete any buffer and
                     * change the current buffer.
                     * - If the buffer we are going to edit is deleted, give up.
                     * - If the current buffer is deleted, prefer to load the new
                     *   buffer when loading a buffer is required.  This avoids
                     *   loading another buffer which then must be closed again.
                     * - If we ended up in the new buffer already, need to skip
                     *   a few things, set auto_buf.
                     */
                    Bytes new_name = null;
                    if (buf.b_fname != null)
                        new_name = STRDUP(buf.b_fname);

                    au_new_curbuf = buf;
                    apply_autocmds(EVENT_BUFLEAVE, null, null, false, curbuf);
                    if (!buf_valid(buf))                /* new buffer has been deleted */
                    {
                        delbuf_msg(new_name);
                        break theend;
                    }
                    if (aborting())                     /* autocmds may abort script processing */
                        break theend;
                    if (buf == curbuf)                  /* already in new buffer */
                        auto_buf = true;
                    else
                    {
                        if (curbuf == old_curbuf)
                            buf_copy_options(buf, BCO_ENTER);

                        /* close the link to the current buffer */
                        u_sync(false);
                        close_buffer(oldwin, curbuf, (flags & ECMD_HIDE) != 0 ? 0 : DOBUF_UNLOAD, false);

                        /* Autocommands may open a new window and leave oldwin open
                         * which leads to crashes since the above call sets oldwin.w_buffer to null. */
                        if (curwin != oldwin && oldwin != aucmd_win
                                    && win_valid(oldwin) && oldwin.w_buffer == null)
                            win_close(oldwin, false);

                        if (aborting())                 /* autocmds may abort script processing */
                            break theend;
                        /* Be careful again, like above. */
                        if (!buf_valid(buf))            /* new buffer has been deleted */
                        {
                            delbuf_msg(new_name);
                            break theend;
                        }
                        if (buf == curbuf)              /* already in new buffer */
                            auto_buf = true;
                        else
                        {
                            /*
                             * <VN> We could instead free the synblock
                             * and re-attach to buffer, perhaps.
                             */
                            if (curwin.w_s == curwin.w_buffer.b_s)
                                curwin.w_s = buf.b_s;
                            curwin.w_buffer = buf;
                            curbuf = buf;
                            curbuf.b_nwindows++;

                            /* Set 'fileformat' and 'binary' when forced. */
                            if (!oldbuf && eap != null)
                                set_file_options(true, eap);
                        }

                        /* May get the window options from the last time this buffer was in this
                         * (or another) window.
                         * If not used before, reset the local window options to the global values.
                         * Also restores old folding stuff. */
                        get_winopts(curbuf);
                    }
                    au_new_curbuf = null;
                }

                curwin.w_pcmark.lnum = 1;
                curwin.w_pcmark.col = 0;
            }
            else /* !other_file */
            {
                if ((flags & ECMD_ADDBUF) != 0 || check_fname() == false)
                    break theend;

                oldbuf = ((flags & ECMD_OLDBUF) != 0);
            }

            buffer_C buf = curbuf;
            set_buflisted(true);

            /* If autocommands change buffers under our fingers, forget about editing the file. */
            if (buf != curbuf)
                break theend;
            if (aborting())         /* autocmds may abort script processing */
                break theend;

            /* Since we are starting to edit a file, consider the filetype to be unset.
             * Helps for when an autocommand changes files and expects syntax highlighting
             * to work in the other file. */
            did_filetype = false;

            /*
             * other_file   oldbuf
             *  false       false       re-edit same file, buffer is re-used
             *  false       true        re-edit same file, nothing changes
             *  true        false       start editing new file, new buffer
             *  true        true        start editing in existing buffer (nothing to do)
             */
            if (!other_file && !oldbuf)         /* re-use the buffer */
            {
                set_last_cursor(curwin);        /* may set b_last_cursor */
                if (newlnum == ECMD_LAST || newlnum == ECMD_LASTL)
                {
                    newlnum = curwin.w_cursor.lnum;
                    solcol = curwin.w_cursor.col;
                }
                buf = curbuf;

                Bytes new_name = null;
                if (buf.b_fname != null)
                    new_name = STRDUP(buf.b_fname);

                if (p_ur[0] < 0 || curbuf.b_ml.ml_line_count <= p_ur[0])
                {
                    /* Save all the text, so that the reload can be undone.
                     * Sync first so that this is a separate undo-able action. */
                    u_sync(false);
                    if (u_savecommon(0, curbuf.b_ml.ml_line_count + 1, 0, true) == false)
                        break theend;

                    u_unchanged(curbuf);
                    buf_freeall(curbuf, BFA_KEEP_UNDO);

                    /* tell readfile() not to clear or reload undo info */
                    readfile_flags = READ_KEEP_UNDO;
                }
                else
                    buf_freeall(curbuf, 0);     /* free all things for buffer */

                /* If autocommands deleted the buffer we were going to re-edit,
                 * give up and jump to the end. */
                if (!buf_valid(buf))
                {
                    delbuf_msg(new_name);
                    break theend;
                }

                /* If autocommands change buffers under our fingers, forget about re-editing the file.
                 * Should do the buf_clear_file(), but perhaps the autocommands changed the buffer... */
                if (buf != curbuf)
                    break theend;
                if (aborting())         /* autocommands may abort script processing */
                    break theend;

                buf_clear_file(curbuf);
                curbuf.b_op_start.lnum = 0;     /* clear '[ and '] marks */
                curbuf.b_op_end.lnum = 0;
            }

        /*
         * If we get here we are sure to start editing
         */
            /* don't redraw until the cursor is in the right line */
            redrawingDisabled++;

            /* Assume success now. */
            retval[0] = true;

            /*
             * Reset cursor position, could be used by autocommands.
             */
            check_cursor();

            /*
             * Check if we are editing the w_arg_idx file in the argument list.
             */
            check_arg_idx(curwin);

            if (!auto_buf)
            {
                /*
                 * Set cursor and init window before reading the file and executing autocommands.
                 * This allows for the autocommands to position the cursor.
                 */
                curwin_init();

                /*
                 * Careful: open_buffer() and apply_autocmds() may change the current buffer and window.
                 */
                pos_C orig_pos = new pos_C();
                COPY_pos(orig_pos, curwin.w_cursor);
                topline = curwin.w_topline;
                if (!oldbuf)                        /* need to read the file */
                {
                    swap_exists_action = SEA_DIALOG;
                    curbuf.b_flags |= BF_CHECK_RO; /* set/reset 'ro' flag */

                    /*
                     * Open the buffer and read the file.
                     */
                    if (should_abort(open_buffer(false, eap, readfile_flags)))
                        retval[0] = false;

                    if (swap_exists_action == SEA_QUIT)
                        retval[0] = false;
                    handle_swap_exists(old_curbuf);
                }
                else
                {
                    apply_autocmds_retval(EVENT_BUFENTER, null, null, false, curbuf, retval);
                    apply_autocmds_retval(EVENT_BUFWINENTER, null, null, false, curbuf, retval);
                }
                check_arg_idx(curwin);

                /* If autocommands change the cursor position or topline,
                 * we should keep it.  Also when it moves within a line. */
                if (!eqpos(curwin.w_cursor, orig_pos))
                {
                    newlnum = curwin.w_cursor.lnum;
                    newcol = curwin.w_cursor.col;
                }
                if (curwin.w_topline == topline)
                    topline = 0;

                /* Even when cursor didn't move we need to recompute topline. */
                changed_line_abv_curs();
            }

            if (command == null)
            {
                if (0 <= newcol)        /* position set by autocommands */
                {
                    curwin.w_cursor.lnum = newlnum;
                    curwin.w_cursor.col = newcol;
                    check_cursor();
                }
                else if (0 < newlnum)   /* line number from caller or old position */
                {
                    curwin.w_cursor.lnum = newlnum;
                    check_cursor_lnum();
                    if (0 <= solcol && !p_sol[0])
                    {
                        /* 'sol' is off: Use last known column. */
                        curwin.w_cursor.col = solcol;
                        check_cursor_col();
                        curwin.w_cursor.coladd = 0;
                        curwin.w_set_curswant = true;
                    }
                    else
                        beginline(BL_SOL | BL_FIX);
                }
                else                    /* no line number, go to last line in Ex mode */
                {
                    if (exmode_active != 0)
                        curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
                    beginline(BL_WHITE | BL_FIX);
                }
            }

            /* Check if cursors in other windows on the same buffer are still valid. */
            check_lnums(false);

            /*
             * Did not read the file, need to show some info about the file.
             * Do this after setting the cursor.
             */
            if (oldbuf && !auto_buf)
            {
                boolean msg_scroll_save = msg_scroll;

                /* Obey the 'O' flag in 'cpoptions': overwrite any previous file message. */
                if (shortmess(SHM_OVERALL) && !exiting && p_verbose[0] == 0)
                    msg_scroll = false;
                if (!msg_scroll)        /* wait a bit when overwriting an error msg */
                    check_for_delay(false);
                msg_start();
                msg_scroll = msg_scroll_save;
                msg_scrolled_ign = true;

                fileinfo(0, false);

                msg_scrolled_ign = false;
            }

            if (command != null)
                do_cmdline(command, null, null, DOCMD_VERBOSE);

            --redrawingDisabled;
            if (!skip_redraw)
            {
                long n = p_so[0];
                if (topline == 0 && command == null)
                    p_so[0] = 999;                 /* force cursor halfway the window */
                update_topline();
                curwin.w_scbind_pos = curwin.w_topline;
                p_so[0] = n;
                redraw_curbuf_later(NOT_VALID); /* redraw this buffer later */
            }

            if (p_im[0])
                need_start_insertmode = true;
        }

        if (did_set_swapcommand)
            set_vim_var_string(VV_SWAPCOMMAND, null, -1);

        return retval[0];
    }

    /*private*/ static void delbuf_msg(Bytes name)
    {
        emsg2(u8("E143: Autocommands unexpectedly deleted new buffer %s"), (name == null) ? u8("") : name);
        au_new_curbuf = null;
    }

    /*private*/ static int append_indent;       /* autoindent for first line */

    /*
     * ":insert" and ":append", also used by ":change"
     */
    /*private*/ static final ex_func_C ex_append = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            boolean did_undo = false;
            long lnum = eap.line2;
            int indent = 0;
            boolean empty = ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0);

            /* the ! flag toggles autoindent */
            if (eap.forceit)
                curbuf.b_p_ai[0] = !curbuf.b_p_ai[0];

            /* First autoindent comes from the line we start on. */
            if (eap.cmdidx != CMD_change && curbuf.b_p_ai[0] && 0 < lnum)
                append_indent = get_indent_lnum(lnum);

            if (eap.cmdidx != CMD_append)
                --lnum;

            /* when the buffer is empty append to line 0 and delete the dummy line */
            if (empty && lnum == 1)
                lnum = 0;

            State = INSERT;                 /* behave like in Insert mode */
            if (curbuf.b_p_iminsert[0] == B_IMODE_LMAP)
                State |= LANGMAP;

            for ( ; ; )
            {
                msg_scroll = true;
                need_wait_return = false;
                if (curbuf.b_p_ai[0])
                {
                    if (0 <= append_indent)
                    {
                        indent = append_indent;
                        append_indent = -1;
                    }
                    else if (0 < lnum)
                        indent = get_indent_lnum(lnum);
                }
                ex_keep_indent = false;
                Bytes theline;
                if (eap.getline == null)
                {
                    /* No getline() function, use the lines that follow.
                     * This ends when there is no more. */
                    if (eap.nextcmd == null || eap.nextcmd.at(0) == NUL)
                        break;
                    Bytes p = vim_strchr(eap.nextcmd, NL);
                    if (p == null)
                        p = eap.nextcmd.plus(strlen(eap.nextcmd));
                    theline = STRNDUP(eap.nextcmd, BDIFF(p, eap.nextcmd));
                    if (p.at(0) != NUL)
                        p = p.plus(1);
                    eap.nextcmd = p;
                }
                else
                {
                    int save_State = State;

                    /* Set State to avoid the cursor shape to be set to INSERT mode
                     * when getline() returns. */
                    State = CMDLINE;
                    theline = eap.getline.getline((0 < eap.cstack.cs_looplevel) ? -1 : NUL, eap.cookie, indent);
                    State = save_State;
                }
                lines_left = (int)Rows[0] - 1;
                if (theline == null)
                    break;

                /* Using ^ CTRL-D in getexmodeline() makes us repeat the indent. */
                if (ex_keep_indent)
                    append_indent = indent;

                /* Look for the "." after automatic indent. */
                int vcol = 0;
                Bytes p;
                for (p = theline; vcol < indent; p = p.plus(1))
                {
                    if (p.at(0) == (byte)' ')
                        vcol++;
                    else if (p.at(0) == TAB)
                        vcol += 8 - vcol % 8;
                    else
                        break;
                }
                if ((p.at(0) == (byte)'.' && p.at(1) == NUL) || (!did_undo && !u_save(lnum, lnum + 1 + (empty ? 1 : 0))))
                    break;

                /* don't use autoindent if nothing was typed. */
                if (p.at(0) == NUL)
                    theline.be(0, NUL);

                did_undo = true;
                ml_append(lnum, theline, 0, false);
                appended_lines_mark(lnum, 1L);

                lnum++;

                if (empty)
                {
                    ml_delete(2L, false);
                    empty = false;
                }
            }
            State = NORMAL;

            if (eap.forceit)
                curbuf.b_p_ai[0] = !curbuf.b_p_ai[0];

            /* "start" is set to eap.line2+1 unless that position is invalid
             * (when eap.line2 pointed to the end of the buffer and nothing was appended)
             * "end" is set to lnum when something has been appended,
             * otherwise it is the same than "start" */
            curbuf.b_op_start.lnum =
                    (eap.line2 < curbuf.b_ml.ml_line_count) ? eap.line2 + 1 : curbuf.b_ml.ml_line_count;
            if (eap.cmdidx != CMD_append)
                --curbuf.b_op_start.lnum;
            curbuf.b_op_end.lnum = (eap.line2 < lnum) ? lnum : curbuf.b_op_start.lnum;
            curbuf.b_op_start.col = curbuf.b_op_end.col = 0;
            curwin.w_cursor.lnum = lnum;
            check_cursor_lnum();
            beginline(BL_SOL | BL_FIX);

            need_wait_return = false;   /* don't use wait_return() now */
            ex_no_reprint = true;
        }
    };

    /*
     * ":change"
     */
    /*private*/ static final ex_func_C ex_change = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.line1 <= eap.line2 && !u_save(eap.line1 - 1, eap.line2 + 1))
                return;

            /* the ! flag toggles autoindent */
            if (eap.forceit ? !curbuf.b_p_ai[0] : curbuf.b_p_ai[0])
                append_indent = get_indent_lnum(eap.line1);

            long lnum;
            for (lnum = eap.line2; eap.line1 <= lnum; --lnum)
            {
                if ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0) /* nothing to delete */
                    break;
                ml_delete(eap.line1, false);
            }

            /* make sure the cursor is not beyond the end of the file now */
            check_cursor_lnum();
            deleted_lines_mark(eap.line1, eap.line2 - lnum);

            /* ":append" on the line above the deleted lines. */
            eap.line2 = eap.line1;
            ex_append.ex(eap);
        }
    };

    /*private*/ static final ex_func_C ex_z = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            long lnum = eap.line2;

            /* Vi compatible: ":z!" uses display height, without a count uses 'scroll'. */
            int bigness;
            if (eap.forceit)
                bigness = curwin.w_height;
            else if (firstwin != lastwin)
                bigness = curwin.w_height - 3;
            else
                bigness = (int)curwin.w_onebuf_opt.wo_scr[0] * 2;
            if (bigness < 1)
                bigness = 1;

            Bytes x = eap.arg;
            Bytes kind = x;
            if (kind.at(0) == (byte)'-' || kind.at(0) == (byte)'+' || kind.at(0) == (byte)'=' || kind.at(0) == (byte)'^' || kind.at(0) == (byte)'.')
                x = x.plus(1);
            while (x.at(0) == (byte)'-' || x.at(0) == (byte)'+')
                x = x.plus(1);

            if (x.at(0) != 0)
            {
                if (!asc_isdigit(x.at(0)))
                {
                    emsg(u8("E144: non-numeric argument to :z"));
                    return;
                }
                else
                {
                    bigness = libC.atoi(x);
                    p_window[0] = bigness;
                    if (kind.at(0) == (byte)'=')
                        bigness += 2;
                }
            }

            /* the number of '-' and '+' multiplies the distance */
            if (kind.at(0) == (byte)'-' || kind.at(0) == (byte)'+')
                for (x = kind.plus(1); x.at(0) == kind.at(0); x = x.plus(1))
                    ;

            long start, end, curs;
            boolean minus = false;

            switch (kind.at(0))
            {
                case '-':
                    start = lnum - bigness * BDIFF(x, kind) + 1;
                    end = start + bigness - 1;
                    curs = end;
                    break;

                case '=':
                    start = lnum - (bigness + 1) / 2 + 1;
                    end = lnum + (bigness + 1) / 2 - 1;
                    curs = lnum;
                    minus = true;
                    break;

                case '^':
                    start = lnum - bigness * 2;
                    end = lnum - bigness;
                    curs = lnum - bigness;
                    break;

                case '.':
                    start = lnum - (bigness + 1) / 2 + 1;
                    end = lnum + (bigness + 1) / 2 - 1;
                    curs = end;
                    break;

                default: /* '+' */
                    start = lnum;
                    if (kind.at(0) == (byte)'+')
                        start += bigness * (BDIFF(x, kind) - 1) + 1;
                    else if (eap.addr_count == 0)
                        start++;
                    end = start + bigness - 1;
                    curs = end;
                    break;
            }

            if (start < 1)
                start = 1;

            if (end > curbuf.b_ml.ml_line_count)
                end = curbuf.b_ml.ml_line_count;

            if (curs > curbuf.b_ml.ml_line_count)
                curs = curbuf.b_ml.ml_line_count;

            for (long i = start; i <= end; i++)
            {
                if (minus && i == lnum)
                {
                    msg_putchar('\n');

                    for (int j = 1; j < (int)Columns[0]; j++)
                        msg_putchar('-');
                }

                print_line(i, (eap.flags & EXFLAG_NR) != 0, (eap.flags & EXFLAG_LIST) != 0);

                if (minus && i == lnum)
                {
                    msg_putchar('\n');

                    for (int j = 1; j < (int)Columns[0]; j++)
                        msg_putchar('-');
                }
            }

            curwin.w_cursor.lnum = curs;
            ex_no_reprint = true;
        }
    };

    /*
     * Check if the restricted flag is set.
     * If so, give an error message and return true.
     * Otherwise, return false.
     */
    /*private*/ static boolean check_restricted()
    {
        if (restricted)
        {
            emsg(u8("E145: Shell commands not allowed in rvim"));
            return true;
        }
        return false;
    }

    /*
     * Check if the secure flag is set (.exrc or .vimrc in current directory).
     * If so, give an error message and return true.
     * Otherwise, return false.
     */
    /*private*/ static boolean check_secure()
    {
        if (secure != 0)
        {
            secure = 2;
            emsg(e_curdir);
            return true;
        }
        /*
         * In the sandbox more things are not allowed, including the things
         * disallowed in secure mode.
         */
        if (sandbox != 0)
        {
            emsg(e_sandbox);
            return true;
        }
        return false;
    }

    /*private*/ static Bytes old_sub;              /* previous substitute pattern */
    /*private*/ static boolean global_need_beginline;   /* call beginline() after ":g" */

    /*private*/ static boolean do__all;                 /* do multiple substitutions per line */
    /*private*/ static boolean do__ask;                 /* ask for confirmation */
    /*private*/ static boolean do__count;               /* count only */
    /*private*/ static boolean do__error = true;        /* if false, ignore errors */
    /*private*/ static boolean do__print;               /* print last line with subs. */
    /*private*/ static boolean do__list;                /* list last line with subs. */
    /*private*/ static boolean do__number;              /* list last line with line nr */
    /*private*/ static int do__ic;                      /* ignore case flag */

    /*
     * Perform a substitution from line eap.line1 to line eap.line2 using
     * the command pointed to by eap.arg which should be of the form:
     *
     * /pattern/substitution/{flags}
     *
     * The usual escapes are supported as described in the regexp docs.
     */
    /*private*/ static final ex_func_C ex_sub = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            long i = 0;

            Bytes pat = null, sub = null;
            boolean got_quit = false;
            boolean got_match = false;
            long first_line = 0;                        /* first changed line */
            long last_line= 0;                          /* below last changed line AFTER the change */
            long old_line_count = curbuf.b_ml.ml_line_count;
            boolean endcolumn = false;                  /* cursor in last column when done */
            pos_C old_cursor = new pos_C();
            COPY_pos(old_cursor, curwin.w_cursor);
            boolean save_ma = false;

            Bytes cmd = eap.arg;
            if (global_busy == 0)
            {
                sub_nsubs = 0;
                sub_nlines = 0;
            }
            long start_nsubs = sub_nsubs;

            int which_pat;
            if (eap.cmdidx == CMD_tilde)
                which_pat = RE_LAST;            /* use last used regexp */
            else
                which_pat = RE_SUBST;           /* use last substitute regexp */

                                                /* new pattern and substitution */
            if (eap.cmd.at(0) == (byte)'s' && cmd.at(0) != NUL && !vim_iswhite(cmd.at(0))
                        && vim_strbyte(u8("0123456789cegriIp|\""), cmd.at(0)) == null)
            {
                                                /* don't accept alphanumeric for separator */
                if (asc_isalpha(cmd.at(0)))
                {
                    emsg(u8("E146: Regular expressions can't be delimited by letters"));
                    return;
                }

                byte delimiter;
                /*
                 * undocumented vi feature:
                 *  "\/sub/" and "\?sub?" use last used search pattern (almost like
                 *  //sub/r).  "\&sub&" use last substitute pattern (like //sub/).
                 */
                if (cmd.at(0) == (byte)'\\')
                {
                    cmd = cmd.plus(1);
                    if (vim_strbyte(u8("/?&"), cmd.at(0)) == null)
                    {
                        emsg(e_backslash);
                        return;
                    }
                    if (cmd.at(0) != (byte)'&')
                        which_pat = RE_SEARCH;      /* use last '/' pattern */
                    pat = u8("");                       /* empty search pattern */
                    delimiter = (cmd = cmd.plus(1)).at(-1);             /* remember delimiter character */
                }
                else            /* find the end of the regexp */
                {
                    which_pat = RE_LAST;            /* use last used regexp */
                    delimiter = (cmd = cmd.plus(1)).at(-1);             /* remember delimiter character */
                    pat = cmd;                      /* remember start of search pattern */
                    { Bytes[] __ = { eap.arg }; cmd = skip_regexp(cmd, delimiter, p_magic[0], __); eap.arg = __[0]; }
                    if (cmd.at(0) == delimiter)        /* end delimiter found */
                        (cmd = cmd.plus(1)).be(-1, NUL);               /* replace it with a NUL */
                }

                /*
                 * Small incompatibility: vi sees '\n' as end of the command, but in
                 * Vim we want to use '\n' to find/substitute a NUL.
                 */
                sub = cmd;          /* remember the start of the substitution */

                while (cmd.at(0) != NUL)
                {
                    if (cmd.at(0) == delimiter)            /* end delimiter found */
                    {
                        (cmd = cmd.plus(1)).be(-1, NUL);                   /* replace it with a NUL */
                        break;
                    }
                    if (cmd.at(0) == (byte)'\\' && cmd.at(1) != 0)  /* skip escaped characters */
                        cmd = cmd.plus(1);
                    cmd = cmd.plus(us_ptr2len_cc(cmd));
                }

                if (!eap.skip)
                {
                    /* In POSIX vi ":s/pat/%/" uses the previous subst. string. */
                    if (STRCMP(sub, u8("%")) == 0 && vim_strbyte(p_cpo[0], CPO_SUBPERCENT) != null)
                    {
                        if (old_sub == null)    /* there is no previous command */
                        {
                            emsg(e_nopresub);
                            return;
                        }
                        sub = old_sub;
                    }
                    else
                    {
                        old_sub = STRDUP(sub);
                    }
                }
            }
            else if (!eap.skip)         /* use previous pattern and substitution */
            {
                if (old_sub == null)    /* there is no previous command */
                {
                    emsg(e_nopresub);
                    return;
                }
                pat = null;             /* search_regcomp() will use previous pattern */
                sub = old_sub;

                /* Vi compatibility quirk:
                 * repeating with ":s" keeps the cursor in the last column after using "$". */
                endcolumn = (curwin.w_curswant == MAXCOL);
            }

            /* Recognize ":%s/\n//" and turn it into a join command, which is much more efficient.
             * TODO: find a generic solution to make line-joining operations more
             * efficient, avoid allocating a string that grows in size.
             */
            if (pat != null && STRCMP(pat, u8("\\n")) == 0 && sub.at(0) == NUL && (cmd.at(0) == NUL
                    || (cmd.at(1) == NUL
                            && (cmd.at(0) == (byte)'g' || cmd.at(0) == (byte)'l' || cmd.at(0) == (byte)'p' || cmd.at(0) == (byte)'#'))))
            {
                curwin.w_cursor.lnum = eap.line1;
                if (cmd.at(0) == (byte)'l')
                    eap.flags = EXFLAG_LIST;
                else if (cmd.at(0) == (byte)'#')
                    eap.flags = EXFLAG_NR;
                else if (cmd.at(0) == (byte)'p')
                    eap.flags = EXFLAG_PRINT;

                /* The number of lines joined is the number of lines in the range plus one.
                 * One less when the last line is included. */
                int joined_lines_count = (int)(eap.line2 - eap.line1 + 1);
                if (eap.line2 < curbuf.b_ml.ml_line_count)
                    joined_lines_count++;
                if (1 < joined_lines_count)
                {
                    do_join(joined_lines_count, false, true, false, true);
                    sub_nsubs = joined_lines_count - 1;
                    sub_nlines = 1;
                    do_sub_msg(false);
                    ex_may_print(eap);
                }

                if (!cmdmod.keeppatterns)
                    save_re_pat(RE_SUBST, pat, p_magic[0]);
                /* put pattern in history */
                add_to_history(HIST_SEARCH, pat, true, NUL);

                return;
            }

            /*
             * Find trailing options.  When '&' is used, keep old options.
             */
            if (cmd.at(0) == (byte)'&')
                cmd = cmd.plus(1);
            else
            {
                do__all = p_gd[0];         /* default is global on */
                do__ask = false;
                do__error = true;
                do__print = false;
                do__count = false;
                do__number = false;
                do__ic = 0;
            }
            while (cmd.at(0) != NUL)
            {
                /*
                 * Note that 'g' and 'c' are always inverted.
                 * 'r' is never inverted.
                 */
                if (cmd.at(0) == (byte)'g')
                    do__all = !do__all;
                else if (cmd.at(0) == (byte)'c')
                    do__ask = !do__ask;
                else if (cmd.at(0) == (byte)'n')
                    do__count = true;
                else if (cmd.at(0) == (byte)'e')
                    do__error = !do__error;
                else if (cmd.at(0) == (byte)'r')       /* use last used regexp */
                    which_pat = RE_LAST;
                else if (cmd.at(0) == (byte)'p')
                    do__print = true;
                else if (cmd.at(0) == (byte)'#')
                {
                    do__print = true;
                    do__number = true;
                }
                else if (cmd.at(0) == (byte)'l')
                {
                    do__print = true;
                    do__list = true;
                }
                else if (cmd.at(0) == (byte)'i')       /* ignore case */
                    do__ic = 'i';
                else if (cmd.at(0) == (byte)'I')       /* don't ignore case */
                    do__ic = 'I';
                else
                    break;
                cmd = cmd.plus(1);
            }
            if (do__count)
                do__ask = false;

            /*
             * check for a trailing count
             */
            cmd = skipwhite(cmd);
            if (asc_isdigit(cmd.at(0)))
            {
                { Bytes[] __ = { cmd }; i = getdigits(__); cmd = __[0]; }
                if (i <= 0 && !eap.skip && do__error)
                {
                    emsg(e_zerocount);
                    return;
                }
                eap.line1 = eap.line2;
                eap.line2 += i - 1;
                if (eap.line2 > curbuf.b_ml.ml_line_count)
                    eap.line2 = curbuf.b_ml.ml_line_count;
            }

            /*
             * check for trailing command or garbage
             */
            cmd = skipwhite(cmd);
            if (cmd.at(0) != NUL && cmd.at(0) != (byte)'"')        /* if not end-of-line or comment */
            {
                eap.nextcmd = check_nextcmd(cmd);
                if (eap.nextcmd == null)
                {
                    emsg(e_trailing);
                    return;
                }
            }

            if (eap.skip)       /* not executing commands, only parsing */
                return;

            if (!do__count && !curbuf.b_p_ma[0])
            {
                /* Substitution is not allowed in non-'modifiable' buffer. */
                emsg(e_modifiable);
                return;
            }

            regmmatch_C regmatch = new regmmatch_C();
            if (search_regcomp(pat, RE_SUBST, which_pat, SEARCH_HIS, regmatch) == false)
            {
                if (do__error)
                    emsg(e_invcmd);
                return;
            }

            /* the 'i' or 'I' flag overrules 'ignorecase' and 'smartcase' */
            if (do__ic == 'i')
                regmatch.rmm_ic = true;
            else if (do__ic == 'I')
                regmatch.rmm_ic = false;

            Bytes sub_firstline = null;        /* allocated copy of first sub line */

            /*
             * ~ in the substitute pattern is replaced with the old pattern.
             * We do it here once to avoid it to be replaced over and over again.
             * But don't do it when it starts with "\=", then it's an expression.
             */
            if (!(sub.at(0) == (byte)'\\' && sub.at(1) == (byte)'='))
                sub = regtilde(sub, p_magic[0]);

            /*
             * Check for a match on each line.
             */
            long line2 = eap.line2;
            for (long lnum = eap.line1; lnum <= line2 && !(got_quit || aborting()); lnum++)
            {
                long nmatch = vim_regexec_multi(regmatch, curwin, curbuf, lnum, 0, null);
                if (nmatch != 0)                        /* number of lines in match */
                {
                    /*
                     * The new text is build up step by step, to avoid too much copying.
                     * There are these pieces:
                     *
                     * sub_firstline    The old text, unmodified.
                     * copycol          Column in the old text where we started looking for a match;
                     *                  from here old text still needs to be copied to the new text.
                     * matchcol         Column number of the old text where to look for the next match.
                     *                  It's just after the previous match or one further.
                     * prev_matchcol    Column just after the previous match (if any).
                     *                  Mostly equal to matchcol, except for the first
                     *                  match and after skipping an empty match.
                     * regmatch.*pos    Where the pattern matched in the old text.
                     * new_start        The new text, all that has been produced so far.
                     * new_end          The new text, where to append new text.
                     *
                     * lnum             The line number where we found the start of the match.
                     *                  Can be below the line we searched when there is a \n
                     *                  before a \zs in the pattern.
                     * sub_firstlnum    The line number in the buffer where to look for a match.
                     *                  Can be different from "lnum" when the pattern or substitute
                     *                  string contains line breaks.
                     *
                     * Special situations:
                     * - When the substitute string contains a line break, the part up to the line
                     *   break is inserted in the text, but the copy of the original line is kept.
                     *   "sub_firstlnum" is adjusted for the inserted lines.
                     * - When the matched pattern contains a line break, the old line is taken from
                     *   the line at the end of the pattern.  The lines in the match are deleted
                     *   later, "sub_firstlnum" is adjusted accordingly.
                     *
                     * The new text is built up in new_start[].  It has some extra room to avoid
                     * using calloc()/free() too often.  new_start_len is the length of the allocated
                     * memory at new_start.
                     *
                     * Make a copy of the old line, so it won't be taken away when updating the screen
                     * or handling a multi-line match.  The "old_" pointers point into this copy.
                     */
                    int prev_matchcol = MAXCOL;
                    Bytes new_start = null;
                    int new_start_len = 0;
                    boolean did_sub = false;
                    long nmatch_tl = 0;                     /* nr of lines matched below lnum */
                    boolean skip_match = false;

                    long sub_firstlnum = lnum;              /* nr of first sub line */
                    int copycol = 0;
                    int matchcol = 0;

                    /* At first match, remember current cursor position. */
                    if (!got_match)
                    {
                        setpcmark();
                        got_match = true;
                    }

                    /*
                     * Loop until nothing more to replace in this line.
                     * 1. Handle match with empty string.
                     * 2. If do__ask is set, ask for confirmation.
                     * 3. Substitute the string.
                     * 4. If do__all is set, find next match.
                     * 5. Break if there isn't another match in this line.
                     */
                    for ( ; ; )
                    {
                        /* Advance "lnum" to the line where the match starts.
                         * The match does not start in the first line when there is a line break before \zs. */
                        if (0 < regmatch.startpos[0].lnum)
                        {
                            lnum += regmatch.startpos[0].lnum;
                            sub_firstlnum += regmatch.startpos[0].lnum;
                            nmatch -= regmatch.startpos[0].lnum;
                            sub_firstline = null;
                        }

                        if (sub_firstline == null)
                            sub_firstline = STRDUP(ml_get(sub_firstlnum));

                        /* Save the line number of the last change for the final cursor position (just like Vi). */
                        curwin.w_cursor.lnum = lnum;
                        boolean do_again = false;               /* do it again after joining lines */

                        skip:
                        {
                            /*
                             * 1. Match empty string does not count, except for first match.
                             * This reproduces the strange vi behaviour.
                             * This also catches endless loops.
                             */
                            if (matchcol == prev_matchcol
                                        && regmatch.endpos[0].lnum == 0 && matchcol == regmatch.endpos[0].col)
                            {
                                if (sub_firstline.at(matchcol) == NUL)
                                    /* We already were at the end of the line.
                                     * Don't look for a match in this line again. */
                                    skip_match = true;
                                else
                                {
                                    /* search for a match at next column */
                                    matchcol += us_ptr2len_cc(sub_firstline.plus(matchcol));
                                }
                                break skip;
                            }

                            /* Normally we continue searching for a match just after the previous match. */
                            matchcol = regmatch.endpos[0].col;
                            prev_matchcol = matchcol;

                            /*
                             * 2. If do__count is set only increase the counter.
                             *    If do__ask is set, ask for confirmation.
                             */
                            if (do__count)
                            {
                                /* For a multi-line match, put matchcol at the NUL at
                                 * the end of the line and set nmatch to one, so that
                                 * we continue looking for a match on the next line.
                                 * Avoids that ":s/\nB\@=//gc" get stuck. */
                                if (1 < nmatch)
                                {
                                    matchcol = strlen(sub_firstline);
                                    nmatch = 1;
                                    skip_match = true;
                                }
                                sub_nsubs++;
                                did_sub = true;
                                /* Skip the substitution, unless an expression is used,
                                 * then it is evaluated in the sandbox. */
                                if (!(sub.at(0) == (byte)'\\' && sub.at(1) == (byte)'='))
                                    break skip;
                            }

                            if (do__ask)
                            {
                                int typed = 0;

                                /* change State to CONFIRM, so that the mouse works properly */
                                int save_State = State;
                                State = CONFIRM;
                                setmouse();         /* disable mouse in xterm */
                                curwin.w_cursor.col = regmatch.startpos[0].col;

                                /* When 'cpoptions' contains "u" don't sync undo when asking for confirmation. */
                                if (vim_strbyte(p_cpo[0], CPO_UNDO) != null)
                                    no_u_sync++;

                                /*
                                 * Loop until 'y', 'n', 'q', CTRL-E or CTRL-Y typed.
                                 */
                                while (do__ask)
                                {
                                    if (exmode_active != 0)
                                    {
                                        int[] sc = new int[1];
                                        int[] ec = new int[1];

                                        print_line_no_prefix(lnum, do__number, do__list);

                                        getvcol(curwin, curwin.w_cursor, sc, null, null);
                                        curwin.w_cursor.col = regmatch.endpos[0].col - 1;
                                        getvcol(curwin, curwin.w_cursor, null, null, ec);
                                        if (do__number || curwin.w_onebuf_opt.wo_nu[0])
                                        {
                                            int numw = number_width(curwin) + 1;
                                            sc[0] += numw;
                                            ec[0] += numw;
                                        }
                                        msg_start();
                                        for (i = 0; i < (long)sc[0]; i++)
                                            msg_putchar(' ');
                                        for ( ; i <= (long)ec[0]; i++)
                                            msg_putchar('^');

                                        Bytes resp = getexmodeline.getline('?', null, 0);
                                        if (resp != null)
                                            typed = resp.at(0);
                                    }
                                    else
                                    {
                                        Bytes orig_line = null;
                                        int len_change = 0;

                                        /* Invert the matched string.  Remove the inversion afterwards. */
                                        int temp = redrawingDisabled;
                                        redrawingDisabled = 0;

                                        if (new_start != null)
                                        {
                                            /* There already was a substitution, we would like to show this
                                             * to the user.
                                             * We cannot really update the line, it would change what matches.
                                             * Temporarily replace the line and change it back afterwards. */
                                            orig_line = STRDUP(ml_get(lnum));

                                            Bytes new_line = concat_str(new_start, sub_firstline.plus(copycol));

                                            /* Position the cursor relative to the end of the line,
                                             * the previous substitute may have inserted or deleted
                                             * characters before the cursor. */
                                            len_change = strlen(new_line) - strlen(orig_line);
                                            curwin.w_cursor.col += len_change;
                                            ml_replace(lnum, new_line, false);
                                        }

                                        search_match_lines = regmatch.endpos[0].lnum - regmatch.startpos[0].lnum;
                                        search_match_endcol = regmatch.endpos[0].col + len_change;
                                        highlight_match = true;

                                        update_topline();
                                        validate_cursor();
                                        update_screen(SOME_VALID);
                                        highlight_match = false;
                                        redraw_later(SOME_VALID);

                                        if (msg_row == (int)Rows[0] - 1)
                                            msg_didout = false;         /* avoid a scroll-up */
                                        msg_starthere();
                                        boolean b = msg_scroll;
                                        msg_scroll = false;             /* truncate msg when needed */
                                        msg_no_more = true;
                                        /* write message same highlighting as for wait_return */
                                        smsg_attr(hl_attr(HLF_R), u8("replace with %s (y/n/a/q/l/^E/^Y)?"), sub);
                                        msg_no_more = false;
                                        msg_scroll = b;
                                        showruler(true);
                                        windgoto(msg_row, msg_col);
                                        redrawingDisabled = temp;

                                        no_mapping++;                   /* don't map this key */
                                        allow_keys++;                   /* allow special keys */
                                        typed = plain_vgetc();
                                        --allow_keys;
                                        --no_mapping;

                                        /* clear the question */
                                        msg_didout = false;             /* don't scroll up */
                                        msg_col = 0;
                                        gotocmdline(true);

                                        /* restore the line */
                                        if (orig_line != null)
                                            ml_replace(lnum, orig_line, false);
                                    }

                                    need_wait_return = false;           /* no hit-return prompt */
                                    if (typed == 'q' || typed == ESC || typed == Ctrl_C || typed == intr_char)
                                    {
                                        got_quit = true;
                                        break;
                                    }
                                    if (typed == 'n')
                                        break;
                                    if (typed == 'y')
                                        break;
                                    if (typed == 'l')
                                    {
                                        /* last: replace and then stop */
                                        do__all = false;
                                        line2 = lnum;
                                        break;
                                    }
                                    if (typed == 'a')
                                    {
                                        do__ask = false;
                                        break;
                                    }
                                }
                                State = save_State;
                                setmouse();
                                if (vim_strbyte(p_cpo[0], CPO_UNDO) != null)
                                    --no_u_sync;

                                if (typed == 'n')
                                {
                                    /* For a multi-line match, put matchcol at the NUL at the end of the line and
                                     * set nmatch to one, so that we continue looking for a match on the next line.
                                     * Avoids that ":%s/\nB\@=//gc" and ":%s/\n/,\r/gc" get stuck when pressing 'n'.
                                     */
                                    if (1 < nmatch)
                                    {
                                        matchcol = strlen(sub_firstline);
                                        skip_match = true;
                                    }
                                    break skip;
                                }
                                if (got_quit)
                                    break skip;
                            }

                            /* Move the cursor to the start of the match, so that we can use "\=col("."). */
                            curwin.w_cursor.col = regmatch.startpos[0].col;

                            /*
                             * 3. substitute the string.
                             */
                            if (do__count)
                            {
                                /* prevent accidentally changing the buffer by a function */
                                save_ma = curbuf.b_p_ma[0];
                                curbuf.b_p_ma[0] = false;
                                sandbox++;
                            }
                            /* get length of substitution part */
                            int sublen = vim_regsub_multi(regmatch,
                                                sub_firstlnum - regmatch.startpos[0].lnum,
                                                sub, sub_firstline, false, p_magic[0], true);
                            if (do__count)
                            {
                                curbuf.b_p_ma[0] = save_ma;
                                sandbox--;
                                break skip;
                            }

                            /* When the match included the "$" of the last line it may
                             * go beyond the last line of the buffer. */
                            if (nmatch > curbuf.b_ml.ml_line_count - sub_firstlnum + 1)
                            {
                                nmatch = curbuf.b_ml.ml_line_count - sub_firstlnum + 1;
                                skip_match = true;
                            }

                            /* Need room for:
                             * - result so far in "new_start" (not for first sub in line)
                             * - original text up to match
                             * - length of substituted part
                             * - original text after match
                             */
                            Bytes p1;
                            if (nmatch == 1)
                                p1 = sub_firstline;
                            else
                            {
                                p1 = ml_get(sub_firstlnum + nmatch - 1);
                                nmatch_tl += nmatch - 1;
                            }
                            int copy_len = regmatch.startpos[0].col - copycol;
                            int needed_len = copy_len + (strlen(p1) - regmatch.endpos[0].col) + sublen + 1;
                            Bytes new_end;
                            if (new_start == null)
                            {
                                /*
                                 * Get some space for a temporary buffer to do the substitution into
                                 * (and some extra space to avoid too many calls to calloc()/free()).
                                 */
                                new_start_len = needed_len + 50;
                                new_start = new Bytes(new_start_len);
                                new_start.be(0, NUL);
                                new_end = new_start;
                            }
                            else
                            {
                                /*
                                 * Check if the temporary buffer is long enough to do the
                                 * substitution into.  If not, make it larger (with a bit
                                 * extra to avoid too many calls to calloc()/free()).
                                 */
                                int len = strlen(new_start);
                                needed_len += len;
                                if (new_start_len < needed_len)
                                {
                                    new_start_len = needed_len + 50;
                                    p1 = new Bytes(new_start_len);
                                    BCOPY(p1, new_start, len + 1);
                                    new_start = p1;
                                }
                                new_end = new_start.plus(len);
                            }

                            /*
                             * copy the text up to the part that matched
                             */
                            BCOPY(new_end, 0, sub_firstline, copycol, copy_len);
                            new_end = new_end.plus(copy_len);

                            vim_regsub_multi(regmatch, sub_firstlnum - regmatch.startpos[0].lnum,
                                                    sub, new_end, true, p_magic[0], true);
                            sub_nsubs++;
                            did_sub = true;

                            /* Move the cursor to the start of the line, to avoid that
                             * it is beyond the end of the line after the substitution. */
                            curwin.w_cursor.col = 0;

                            /* For a multi-line match, make a copy of the last matched
                             * line and continue in that one. */
                            if (1 < nmatch)
                            {
                                sub_firstlnum += nmatch - 1;
                                sub_firstline = STRDUP(ml_get(sub_firstlnum));
                                /* When going beyond the last line, stop substituting. */
                                if (sub_firstlnum <= line2)
                                    do_again = true;
                                else
                                    do__all = false;
                            }

                            /* Remember next character to be copied. */
                            copycol = regmatch.endpos[0].col;

                            if (skip_match)
                            {
                                /* Already hit end of the buffer,
                                 * sub_firstlnum is one less than what it ought to be. */
                                sub_firstline = STRDUP(u8(""));
                                copycol = 0;
                            }

                            /*
                             * Now the trick is to replace CTRL-M chars with a real line break.
                             * This would make it impossible to insert a CTRL-M in the text.
                             * The line break can be avoided by preceding the CTRL-M with a backslash.
                             * To be able to insert a backslash, they must be doubled in the string
                             * and are halved here.
                             * That is Vi compatible.
                             */
                            for (p1 = new_end; p1.at(0) != NUL; p1 = p1.plus(1))
                            {
                                if (p1.at(0) == (byte)'\\' && p1.at(1) != NUL)  /* remove backslash */
                                    BCOPY(p1, 0, p1, 1, strlen(p1, 1) + 1);
                                else if (p1.at(0) == CAR)
                                {
                                    if (u_inssub(lnum) == true)     /* prepare for undo */
                                    {
                                        p1.be(0, NUL);                  /* truncate up to the CR */
                                        ml_append(lnum - 1, new_start, BDIFF(p1, new_start) + 1, false);
                                        mark_adjust(lnum + 1, MAXLNUM, 1L, 0L);
                                        if (do__ask)
                                            appended_lines(lnum - 1, 1L);
                                        else
                                        {
                                            if (first_line == 0)
                                                first_line = lnum;
                                            last_line = lnum + 1;
                                        }
                                        /* All line numbers increase. */
                                        sub_firstlnum++;
                                        lnum++;
                                        line2++;
                                        /* move the cursor to the new line, like Vi */
                                        curwin.w_cursor.lnum++;
                                        /* copy the rest */
                                        BCOPY(new_start, 0, p1, 1, strlen(p1, 1) + 1);
                                        p1 = new_start.minus(1);
                                    }
                                }
                                else
                                    p1 = p1.plus(us_ptr2len_cc(p1) - 1);
                            }
                        }

                        /*
                         * 4. If do__all is set, find next match.
                         * Prevent endless loop with patterns that match empty
                         * strings, e.g. :s/$/pat/g or :s/[a-z]* /(&)/g.
                         * But ":s/\n/#/" is OK.
                         */
                        /*
                         * We already know that we did the last subst when we are at the end of the line,
                         * except that a pattern like "bar\|\nfoo" may match at the NUL.
                         * "lnum" can be below "line2" when there is a \zs in the pattern after a line break.
                         */
                        boolean lastone = (skip_match
                                        || got_int
                                        || got_quit
                                        || line2 < lnum
                                        || !(do__all || do_again)
                                        || (sub_firstline.at(matchcol) == NUL && nmatch <= 1
                                                && !re_multiline(regmatch.regprog)));
                        nmatch = -1;

                        /*
                         * Replace the line in the buffer when needed.
                         * This is skipped when there are more matches.
                         * The check for nmatch_tl is needed for when multi-line matching must replace
                         * the lines before trying to do another match, otherwise "\@<=" won't work.
                         * When the match starts below where we start searching
                         * also need to replace the line first (using \zs after \n).
                         */
                        if (lastone
                            || 0 < nmatch_tl
                            || (nmatch = vim_regexec_multi(regmatch, curwin, curbuf, sub_firstlnum, matchcol, null)) == 0
                            || 0 < regmatch.startpos[0].lnum)
                        {
                            if (new_start != null)
                            {
                                /*
                                 * Copy the rest of the line, that didn't match.
                                 * "matchcol" has to be adjusted, we use the end of the line as reference,
                                 * because the substitute may have changed the number of characters.
                                 * Same for "prev_matchcol".
                                 */
                                STRCAT(new_start, sub_firstline.plus(copycol));
                                matchcol = strlen(sub_firstline) - matchcol;
                                prev_matchcol = strlen(sub_firstline) - prev_matchcol;

                                if (u_savesub(lnum) != true)
                                    break;
                                ml_replace(lnum, new_start, true);

                                if (0 < nmatch_tl)
                                {
                                    /*
                                     * Matched lines have now been substituted and are useless, delete them.
                                     * The part after the match has been appended to "new_start", we don't need
                                     * it in the buffer.
                                     */
                                    lnum++;
                                    if (u_savedel(lnum, nmatch_tl) != true)
                                        break;
                                    for (i = 0; i < nmatch_tl; i++)
                                        ml_delete(lnum, false);
                                    mark_adjust(lnum, lnum + nmatch_tl - 1, MAXLNUM, -nmatch_tl);
                                    if (do__ask)
                                        deleted_lines(lnum, nmatch_tl);
                                    --lnum;
                                    line2 -= nmatch_tl; /* nr of lines decreases */
                                    nmatch_tl = 0;
                                }

                                /* When asking, undo is saved each time, must also set changed flag each time. */
                                if (do__ask)
                                    changed_bytes(lnum, 0);
                                else
                                {
                                    if (first_line == 0)
                                        first_line = lnum;
                                    last_line = lnum + 1;
                                }

                                sub_firstlnum = lnum;
                                sub_firstline = new_start;
                                new_start = null;
                                matchcol = strlen(sub_firstline) - matchcol;
                                prev_matchcol = strlen(sub_firstline) - prev_matchcol;
                                copycol = 0;
                            }
                            if (nmatch == -1 && !lastone)
                                nmatch = vim_regexec_multi(regmatch, curwin, curbuf, sub_firstlnum, matchcol, null);

                            /*
                             * 5. break if there isn't another match in this line
                             */
                            if (nmatch <= 0)
                            {
                                /* If the match found didn't start where we were
                                 * searching, do the next search in the line where we
                                 * found the match. */
                                if (nmatch == -1)
                                    lnum -= regmatch.startpos[0].lnum;
                                break;
                            }
                        }

                        line_breakcheck();
                    }

                    if (did_sub)
                        sub_nlines++;
                    sub_firstline = null;
                }

                line_breakcheck();
            }

            if (first_line != 0)
            {
                /* Need to subtract the number of added lines from "last_line" to get
                 * the line number before the change (same as adding the number of deleted lines).
                 */
                i = curbuf.b_ml.ml_line_count - old_line_count;
                changed_lines(first_line, 0, last_line - i, i);
            }

            /* ":s/pat//n" doesn't move the cursor */
            if (do__count)
                COPY_pos(curwin.w_cursor, old_cursor);

            if (start_nsubs < sub_nsubs)
            {
                /* Set the '[ and '] marks. */
                curbuf.b_op_start.lnum = eap.line1;
                curbuf.b_op_end.lnum = line2;
                curbuf.b_op_start.col = curbuf.b_op_end.col = 0;

                if (global_busy == 0)
                {
                    if (!do__ask)       /* when interactive leave cursor on the match */
                    {
                        if (endcolumn)
                            coladvance(MAXCOL);
                        else
                            beginline(BL_WHITE | BL_FIX);
                    }
                    if (!do_sub_msg(do__count) && do__ask)
                        msg(u8(""));
                }
                else
                    global_need_beginline = true;
                if (do__print)
                    print_line(curwin.w_cursor.lnum, do__number, do__list);
            }
            else if (global_busy == 0)
            {
                if (got_int)                /* interrupted */
                    emsg(e_interr);
                else if (got_match)         /* did find something but nothing substituted */
                    msg(u8(""));
                else if (do__error)         /* nothing found */
                    emsg2(e_patnotf2, get_search_pat());
            }
        }
    };

    /*
     * Give message for number of substitutions.
     * Can also be used after a ":global" command.
     * Return true if a message was given.
     */
    /*private*/ static boolean do_sub_msg(boolean count_only)
        /* count_only: used 'n' flag for ":s" */
    {
        /*
         * Only report substitutions when:
         * - more than 'report' substitutions
         * - command was typed by user, or number of changed lines > 'report'
         * - giving messages is not disabled by 'lazyredraw'
         */
        if (((p_report[0] < sub_nsubs && (keyTyped || 1 < sub_nlines || p_report[0] < 1)) || count_only)
                && messaging())
        {
            if (got_int)
                STRCPY(msg_buf, u8("(Interrupted) "));
            else
                msg_buf.be(0, NUL);
            if (sub_nsubs == 1)
                vim_snprintf_add(msg_buf, MSG_BUF_LEN, u8("%s"), (count_only) ? u8("1 match") : u8("1 substitution"));
            else
                vim_snprintf_add(msg_buf, MSG_BUF_LEN, (count_only) ? u8("%ld matches") : u8("%ld substitutions"), sub_nsubs);
            if (sub_nlines == 1)
                vim_snprintf_add(msg_buf, MSG_BUF_LEN, u8("%s"), u8(" on 1 line"));
            else
                vim_snprintf_add(msg_buf, MSG_BUF_LEN, u8(" on %ld lines"), sub_nlines);
            if (msg(msg_buf))
                /* save message to display it after redraw */
                set_keep_msg(msg_buf, 0);
            return true;
        }
        if (got_int)
        {
            emsg(e_interr);
            return true;
        }
        return false;
    }

    /*
     * Execute a global command of the form:
     *
     * g/pattern/X : execute X on all lines where pattern matches
     * v/pattern/X : execute X on all lines where pattern does not match
     *
     * where 'X' is an EX command
     *
     * The command character (as well as the trailing slash) is optional, and
     * is assumed to be 'p' if missing.
     *
     * This is implemented in two passes: first we scan the file for the pattern and
     * set a mark for each line that (not) matches.  Secondly we execute the command
     * for each line that has a mark.  This is required because after deleting
     * lines we do not know where to search for the next match.
     */
    /*private*/ static final ex_func_C ex_global = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (global_busy != 0)
            {
                emsg(u8("E147: Cannot do :global recursive")); /* will increment global_busy */
                return;
            }

            int type;                               /* first char of cmd: 'v' or 'g' */
            if (eap.forceit)                        /* ":global!" is like ":vglobal" */
                type = 'v';
            else
                type = eap.cmd.at(0);
            Bytes cmd = eap.arg;
            int which_pat = RE_LAST;                /* default: use last used regexp */

            /*
             * undocumented vi feature:
             *  "\/" and "\?": use previous search pattern.
             *           "\&": use previous substitute pattern.
             */
            Bytes pat;
            if (cmd.at(0) == (byte)'\\')
            {
                cmd = cmd.plus(1);
                if (vim_strbyte(u8("/?&"), cmd.at(0)) == null)
                {
                    emsg(e_backslash);
                    return;
                }
                if (cmd.at(0) == (byte)'&')
                    which_pat = RE_SUBST;           /* use previous substitute pattern */
                else
                    which_pat = RE_SEARCH;          /* use previous search pattern */
                cmd = cmd.plus(1);
                pat = u8("");
            }
            else if (cmd.at(0) == NUL)
            {
                emsg(u8("E148: Regular expression missing from global"));
                return;
            }
            else
            {
                byte delim = cmd.at(0);                  /* delimiter, normally '/' */
                if (delim != NUL)
                    cmd = cmd.plus(1);                          /* skip delimiter if there is one */
                pat = cmd;                          /* remember start of pattern */
                { Bytes[] __ = { eap.arg }; cmd = skip_regexp(cmd, delim, p_magic[0], __); eap.arg = __[0]; }
                if (cmd.at(0) == delim)                /* end delimiter found */
                    (cmd = cmd.plus(1)).be(-1, NUL);                   /* replace it with a NUL */
            }

            regmmatch_C regmatch = new regmmatch_C();
            if (search_regcomp(pat, RE_BOTH, which_pat, SEARCH_HIS, regmatch) == false)
            {
                emsg(e_invcmd);
                return;
            }

            int ndone = 0;

            /*
             * pass 1: set marks for each (not) matching line
             */
            for (long lnum = eap.line1; lnum <= eap.line2 && !got_int; lnum++)
            {
                /* a match on this line? */
                long match = vim_regexec_multi(regmatch, curwin, curbuf, lnum, 0, null);
                if ((type == 'g' && match != 0) || (type == 'v' && match == 0))
                {
                    ml_setmarked(lnum);
                    ndone++;
                }
                line_breakcheck();
            }

            /*
             * pass 2: execute the command for each line that has been marked
             */
            if (got_int)
                msg(e_interr);
            else if (ndone == 0)
            {
                if (type == 'v')
                    smsg(u8("Pattern found in every line: %s"), pat);
                else
                    smsg(u8("Pattern not found: %s"), pat);
            }
            else
            {
                start_global_changes();
                global_exe(cmd);
                end_global_changes();
            }

            ml_clearmarked();                   /* clear rest of the marks */
        }
    };

    /*
     * Execute "cmd" on lines marked with ml_setmarked().
     */
    /*private*/ static void global_exe(Bytes cmd)
    {
        buffer_C old_buf = curbuf;  /* remember what buffer we started in */

        /*
         * Set current position only once for a global command.
         * If global_busy is set, setpcmark() will not do anything.
         * If there is an error, global_busy will be incremented.
         */
        setpcmark();

        /* When the command writes a message, don't overwrite the command. */
        msg_didout = true;

        sub_nsubs = 0;
        sub_nlines = 0;
        global_need_beginline = false;
        global_busy = 1;

        long old_lcount = curbuf.b_ml.ml_line_count;

        long lnum;                  /* line number according to old situation */
        while (!got_int && (lnum = ml_firstmarked()) != 0 && global_busy == 1)
        {
            curwin.w_cursor.lnum = lnum;
            curwin.w_cursor.col = 0;
            if (cmd.at(0) == NUL || cmd.at(0) == (byte)'\n')
                do_cmdline(u8("p"), null, null, DOCMD_NOWAIT);
            else
                do_cmdline(cmd, null, null, DOCMD_NOWAIT);
            ui_breakcheck();
        }

        global_busy = 0;
        if (global_need_beginline)
            beginline(BL_WHITE | BL_FIX);
        else
            check_cursor(); /* cursor may be beyond the end of the line */

        /* The cursor may have not moved in the text but a change
         * in a previous line may have moved it on the screen. */
        changed_line_abv_curs();

        /* If it looks like no message was written,
         * allow overwriting the command with the report for number of changes. */
        if (msg_col == 0 && msg_scrolled == 0)
            msg_didout = false;

        /* If substitutes done, report number of substitutes,
         * otherwise report number of extra or deleted lines.
         * Don't report extra or deleted lines in the edge case where the buffer
         * we are in after execution is different from the buffer we started in. */
        if (!do_sub_msg(false) && curbuf == old_buf)
            msgmore(curbuf.b_ml.ml_line_count - old_lcount);
    }

    /*
     * ex_getln.c: Functions for entering and editing an Ex command line ------------------------------
     */

    /*
     * Variables shared between getcmdline(), redrawcmdline() and others.
     * These need to be saved when using CTRL-R |, that's why they are in a structure.
     */
    /*private*/ static final class cmdline_info_C
    {
        Bytes       cmdbuff;        /* pointer to command line buffer */
        int         cmdbufflen;     /* length of "cmdbuff" */
        int         cmdlen;         /* number of chars in command line */
        int         cmdpos;         /* current cursor position */
        int         cmdspos;        /* cursor column on screen */
        int         cmdfirstc;      /* ':', '/', '?', '=', '>' or NUL */
        int         cmdindent;      /* number of spaces before cmdline */
        Bytes       cmdprompt;      /* message in front of cmdline */
        int         cmdattr;        /* attributes for prompt */
        boolean     overstrike;     /* Typing mode on the command line.
                                     * Shared by getcmdline() and put_on_cmdline(). */
        expand_C    xpc;            /* struct being used for expansion,
                                     * "xp_pattern" may point into "cmdbuff" */
        int         xp_context;     /* type of expansion */
        Bytes       xp_arg;         /* user-defined expansion arg */
        boolean     input_fn;       /* when true Invoked for input() function */

        /*private*/ cmdline_info_C()
        {
        }
    }

    /*private*/ static void COPY_cmdline_info(cmdline_info_C cli1, cmdline_info_C cli0)
    {
        cli1.cmdbuff = cli0.cmdbuff;
        cli1.cmdbufflen = cli0.cmdbufflen;
        cli1.cmdlen = cli0.cmdlen;
        cli1.cmdpos = cli0.cmdpos;
        cli1.cmdspos = cli0.cmdspos;
        cli1.cmdfirstc = cli0.cmdfirstc;
        cli1.cmdindent = cli0.cmdindent;
        cli1.cmdprompt = cli0.cmdprompt;
        cli1.cmdattr = cli0.cmdattr;
        cli1.overstrike = cli0.overstrike;
        cli1.xpc = cli0.xpc;
        cli1.xp_context = cli0.xp_context;
        cli1.xp_arg = cli0.xp_arg;
        cli1.input_fn = cli0.input_fn;
    }

    /* The current cmdline_info.  It is initialized in getcmdline() and after that
     * used by other functions.  When invoking getcmdline() recursively it needs
     * to be saved with save_cmdline() and restored with restore_cmdline().
     * TODO: make it local to getcmdline() and pass it around. */
    /*private*/ static cmdline_info_C ccline = new cmdline_info_C();

    /*private*/ static int new_cmdpos;  /* position set by set_cmdline_pos() */

    /*private*/ static final class histentry_C
    {
        int         hisnum;         /* identifying number */
        Bytes       hisstr;         /* actual entry, separator char after the NUL */

        /*private*/ histentry_C()
        {
        }
    }

    /*private*/ static void COPY_histentry(histentry_C he1, histentry_C he0)
    {
        he1.hisnum = he0.hisnum;
        he1.hisstr = he0.hisstr;
    }

    /*private*/ static histentry_C[] ARRAY_histentry(int n)
    {
        histentry_C[] a = new histentry_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new histentry_C();
        return a;
    }

    /*private*/ static histentry_C[/*HIST_COUNT*/][] history = { null, null, null, null, null };
    /*private*/ static int[/*HIST_COUNT*/] hisidx = { -1, -1, -1, -1, -1 }; /* lastused entry */
    /* identifying (unique) number of newest history entry */
    /*private*/ static int[/*HIST_COUNT*/] hisnum = { 0, 0, 0, 0, 0 };
    /*private*/ static int hislen;                                      /* actual length of history tables */

    /*
     * getcmdline() - accept a command line starting with firstc.
     *
     * firstc == ':'            get ":" command line.
     * firstc == '/' or '?'     get search pattern
     * firstc == '='            get expression
     * firstc == '@'            get text for input() function
     * firstc == '>'            get text for debug mode
     * firstc == NUL            get text for :insert command
     * firstc == -1             like NUL, and break on CTRL-C
     *
     * The line is collected in ccline.cmdbuff, which is reallocated to fit the command line.
     *
     * Careful: getcmdline() can be called recursively!
     *
     * Return pointer to allocated string if there is a commandline, null otherwise.
     */
    /*private*/ static Bytes getcmdline(int firstc, long count, int indent)
        /* count: only used for incremental search */
        /* indent: indent for inside conditionals */
    {
        boolean gotesc = false;                 /* true when <ESC> just typed */
        Bytes lookfor = null;                  /* string to match */
        boolean did_incsearch = false;
        boolean incsearch_postponed = false;
        boolean did_wild_list = false;          /* did wild_list() recently */
        int wim_index = 0;                      /* index in wim_flags[] */
        boolean save_msg_scroll = msg_scroll;
        int save_State = State;                 /* remember State when called */
        boolean some_key_typed = false;         /* one of the keys was typed */
        /*
         * mouse drag and release events are ignored,
         * unless they are preceded with a mouse down event
         */
        boolean ignore_drag_release = true;
        boolean break_ctrl_c = false;
        long[] b_im_ptr = null;

        /*
         * Everything that may work recursively should save and restore the current command line in save_cli.
         * That includes update_screen(), a custom status line may invoke ":normal".
         */
        cmdline_info_C save_cli = new cmdline_info_C();

        if (firstc == -1)
        {
            firstc = NUL;
            break_ctrl_c = true;
        }

        ccline.overstrike = false;                  /* always start in insert mode */
        pos_C old_cursor = new pos_C();
        COPY_pos(old_cursor, curwin.w_cursor);      /* needs to be restored later */
        int old_curswant = curwin.w_curswant;
        int old_leftcol = curwin.w_leftcol;
        long old_topline = curwin.w_topline;
        long old_botline = curwin.w_botline;

        /*
         * set some variables for redrawcmd()
         */
        ccline.cmdfirstc = (firstc == '@') ? 0 : firstc;
        ccline.cmdindent = (0 < firstc) ? indent : 0;

        /* alloc initial ccline.cmdbuff */
        alloc_cmdbuff((exmode_active != 0) ? 250 : indent + 1);
        ccline.cmdlen = ccline.cmdpos = 0;
        ccline.cmdbuff.be(0, NUL);

        /* autoindent for :insert and :append */
        if (firstc <= 0)
        {
            copy_spaces(ccline.cmdbuff, indent);
            ccline.cmdbuff.be(indent, NUL);
            ccline.cmdpos = indent;
            ccline.cmdspos = indent;
            ccline.cmdlen = indent;
        }

        expand_C xpc = new expand_C();
        expandInit(xpc);
        ccline.xpc = xpc;

        if (curwin.w_onebuf_opt.wo_rl[0] && curwin.w_onebuf_opt.wo_rlc[0].at(0) == (byte)'s' && (firstc == '/' || firstc == '?'))
            cmdmsg_rl = true;
        else
            cmdmsg_rl = false;

        redir_off = true;                   /* don't redirect the typed command */
        if (!cmd_silent)
        {
            int i = msg_scrolled;
            msg_scrolled = 0;               /* avoid wait_return message */
            gotocmdline(true);
            msg_scrolled += i;
            redrawcmdprompt();              /* draw prompt or indent */
            set_cmdspos();
        }
        xpc.xp_context = EXPAND_NOTHING;
        xpc.xp_backslash = XP_BS_NONE;

        if (ccline.input_fn)
        {
            xpc.xp_context = ccline.xp_context;
            xpc.xp_pattern = ccline.cmdbuff;
            xpc.xp_arg = ccline.xp_arg;
        }

        /*
         * Avoid scrolling when called by a recursive do_cmdline(),
         * e.g. when doing ":@0" when register 0 doesn't contain a CR.
         */
        msg_scroll = false;

        State = CMDLINE;

        if (firstc == '/' || firstc == '?' || firstc == '@')
        {
            /* Use ":lmap" mappings for search pattern and input(). */
            if (curbuf.b_p_imsearch[0] == B_IMODE_USE_INSERT)
                b_im_ptr = curbuf.b_p_iminsert;
            else
                b_im_ptr = curbuf.b_p_imsearch;
            if (b_im_ptr[0] == B_IMODE_LMAP)
                State |= LANGMAP;
        }

        setmouse();
        ui_cursor_shape();          /* may show different cursor shape */

        /* When inside an autocommand for writing "exiting" may be set and terminal mode set to cooked.
         * Need to set raw mode here then. */
        settmode(TMODE_RAW);

        init_history();
        int hiscnt = hislen;        /* current history line in use; set to impossible history value */
        int histype = hist_char2type(firstc);   /* history type to be used */

        do_digraph(-1);             /* init digraph typeahead */

        /* If something above caused an error, reset the flags, we do want to type and execute commands.
         * Display may be messed up a bit. */
        if (did_emsg)
            redrawcmd();
        did_emsg = false;
        got_int = false;

        /*
         * Collect the command string, handling editing keys.
         */
        returncmd:
        for ( ; ; )
        {
            redir_off = true;   /* Don't redirect the typed command.
                                 * Repeated, because a ":redir" inside completion may switch it on. */
            quit_more = false;  /* reset after CTRL-D which had a more-prompt */

            cursorcmd();        /* set the cursor on the right spot */

            /* Get a character.
             * Ignore K_IGNORE, it should not do anything, such as stop completion. */
            int c;
            do
            {
                c = safe_vgetc();
            } while (c == K_IGNORE);

            if (keyTyped)
            {
                some_key_typed = true;

                if (cmdmsg_rl && !keyStuffed)
                {
                    /* Invert horizontal movements and operations.
                     * Only when typed by the user directly, not when the result of a mapping. */
                    switch (c)
                    {
                        case K_RIGHT:   c = K_LEFT; break;
                        case K_S_RIGHT: c = K_S_LEFT; break;
                        case K_C_RIGHT: c = K_C_LEFT; break;
                        case K_LEFT:    c = K_RIGHT; break;
                        case K_S_LEFT:  c = K_S_RIGHT; break;
                        case K_C_LEFT:  c = K_C_RIGHT; break;
                    }
                }
            }

            /*
             * Ignore got_int when CTRL-C was typed here.
             * Don't ignore it in :global, we really need to break then,
             * e.g. for ":g/pat/normal /pat" (without the <CR>).
             * Don't ignore it for the input() function.
             */
            if ((c == Ctrl_C || c == intr_char)
                    && firstc != '@'
                    && !break_ctrl_c
                    && global_busy == 0)
                got_int = false;

            /* free old command line when finished moving around in the history list */
            if (lookfor != null
                    && c != K_S_DOWN && c != K_S_UP
                    && c != K_DOWN && c != K_UP
                    && c != K_PAGEDOWN && c != K_PAGEUP
                    && c != K_KPAGEDOWN && c != K_KPAGEUP
                    && c != K_LEFT && c != K_RIGHT
                    && (0 < xpc.xp_numfiles || (c != Ctrl_P && c != Ctrl_N)))
            {
                lookfor = null;
            }

            /*
             * When there are matching completions to select <S-Tab> works like
             * CTRL-P (unless 'wc' is <S-Tab>).
             */
            if (c != p_wc[0] && c == K_S_TAB && 0 < xpc.xp_numfiles)
                c = Ctrl_P;

            /* free expanded names when finished walking through matches */
            if (xpc.xp_numfiles != -1
                    && !(c == p_wc[0] && keyTyped) && c != p_wcm[0]
                    && c != Ctrl_N && c != Ctrl_P && c != Ctrl_A
                    && c != Ctrl_L)
            {
                expandOne(xpc, null, null, 0, WILD_FREE);
                did_wild_list = false;
                    xpc.xp_context = EXPAND_NOTHING;
                wim_index = 0;
            }

            cmdline_changed:
            {
                cmdline_not_changed:
                {
                    /* CTRL-\ CTRL-N goes to Normal mode,
                     * CTRL-\ CTRL-G goes to Insert mode when 'insertmode' is set,
                     * CTRL-\ e prompts for an expression. */
                    if (c == Ctrl_BSL)
                    {
                        no_mapping++;
                        allow_keys++;
                        c = plain_vgetc();
                        --no_mapping;
                        --allow_keys;
                        /* CTRL-\ e doesn't work when obtaining an expression, unless it is in a mapping. */
                        if (c != Ctrl_N && c != Ctrl_G && (c != 'e' || (ccline.cmdfirstc == '=' && keyTyped)))
                        {
                            vungetc(c);
                            c = Ctrl_BSL;
                        }
                        else if (c == 'e')
                        {
                            /*
                             * Replace the command line with the result of an expression.
                             * Need to save and restore the current command line, to be
                             * able to enter a new one...
                             */
                            if (ccline.cmdpos == ccline.cmdlen)
                                new_cmdpos = 99999; /* keep it at the end */
                            else
                                new_cmdpos = ccline.cmdpos;

                            save_cmdline(save_cli);
                            c = get_expr_register();
                            restore_cmdline(save_cli);

                            if (c == '=')
                            {
                                /* Need to save and restore ccline, and set "textlock" to avoid nasty things
                                 * like going to another buffer when evaluating an expression. */
                                save_cmdline(save_cli);
                                textlock++;
                                Bytes p = get_expr_line();
                                --textlock;
                                restore_cmdline(save_cli);

                                if (p != null)
                                {
                                    int len = strlen(p);
                                    realloc_cmdbuff(len + 1);

                                    ccline.cmdlen = len;
                                    STRCPY(ccline.cmdbuff, p);

                                    /* Restore the cursor or use the position set with set_cmdline_pos(). */
                                    if (ccline.cmdlen < new_cmdpos)
                                        ccline.cmdpos = ccline.cmdlen;
                                    else
                                        ccline.cmdpos = new_cmdpos;

                                    keyTyped = false;   /* Don't do "p_wc" completion. */
                                    redrawcmd();
                                    break cmdline_changed;
                                }
                            }

                            beep_flush();
                            got_int = false;        /* don't abandon the command line */
                            did_emsg = false;
                            emsg_on_display = false;
                            redrawcmd();
                            break cmdline_not_changed;
                        }
                        else
                        {
                            if (c == Ctrl_G && p_im[0] && restart_edit == 0)
                                restart_edit = 'a';
                            gotesc = true;          /* will free ccline.cmdbuff after putting it in history */
                            break returncmd;         /* back to Normal mode */
                        }
                    }

                    if (c == cedit_key || c == K_CMDWIN)
                    {
                        if (ex_normal_busy == 0 && got_int == false)
                        {
                            /* Open a window to edit the command line (and history). */
                            c = ex_window();
                            some_key_typed = true;
                        }
                    }
                    else
                        c = do_digraph(c);

                    if (c == '\n' || c == '\r' || c == K_KENTER || (c == ESC
                                    && (!keyTyped || vim_strbyte(p_cpo[0], CPO_ESC) != null)))
                    {
                        /* In Ex mode a backslash escapes a newline. */
                        if (exmode_active != 0
                                && c != ESC
                                && ccline.cmdpos == ccline.cmdlen
                                && 0 < ccline.cmdpos
                                && ccline.cmdbuff.at(ccline.cmdpos - 1) == (byte)'\\')
                        {
                            if (c == K_KENTER)
                                c = '\n';
                        }
                        else
                        {
                            gotesc = false;                     /* Might have typed ESC previously,
                                                                * don't truncate the cmdline now. */
                            if (ccheck_abbr(c + ABBR_OFF))
                                break cmdline_changed;
                            if (!cmd_silent)
                            {
                                windgoto(msg_row, 0);
                                out_flush();
                            }
                            break;
                        }
                    }

                    /*
                     * Completion for 'wildchar' or 'wildcharm' key.
                     * - hitting <ESC> twice means: abandon command line.
                     * - wildcard expansion is only done when the 'wildchar' key is really
                     *   typed, not when it comes from a macro
                     */
                    if ((c == p_wc[0] && !gotesc && keyTyped) || c == p_wcm[0])
                    {
                        boolean res;
                        if (0 < xpc.xp_numfiles)    /* typed "p_wc" at least twice */
                        {
                            /* if 'wildmode' contains "list" may still need to list */
                            if (1 < xpc.xp_numfiles
                                    && !did_wild_list
                                    && (wim_flags[wim_index] & WIM_LIST) != 0)
                            {
                                showmatches(xpc);
                                redrawcmd();
                                did_wild_list = true;
                            }
                            if ((wim_flags[wim_index] & WIM_LONGEST) != 0)
                                res = nextwild(xpc, WILD_LONGEST, WILD_NO_BEEP, firstc != '@');
                            else if ((wim_flags[wim_index] & WIM_FULL) != 0)
                                res = nextwild(xpc, WILD_NEXT, WILD_NO_BEEP, firstc != '@');
                            else
                                res = true;         /* don't insert 'wildchar' now */
                        }
                        else                        /* typed "p_wc" first time */
                        {
                            wim_index = 0;
                            int j = ccline.cmdpos;
                            /* if 'wildmode' first contains "longest", get longest common part */
                            if ((wim_flags[0] & WIM_LONGEST) != 0)
                                res = nextwild(xpc, WILD_LONGEST, WILD_NO_BEEP, firstc != '@');
                            else
                                res = nextwild(xpc, WILD_EXPAND_KEEP, WILD_NO_BEEP, firstc != '@');

                            /* if interrupted while completing, behave like it failed */
                            if (got_int)
                            {
                                vpeekc();           /* remove <C-C> from input stream */
                                got_int = false;    /* don't abandon the command line */
                                expandOne(xpc, null, null, 0, WILD_FREE);
                                break cmdline_changed;
                            }

                            /* when more than one match, and 'wildmode' first contains "list",
                             * or no change and 'wildmode' contains "longest,list", list all matches */
                            if (res == true && 1 < xpc.xp_numfiles)
                            {
                                /* a "longest" that didn't do anything is skipped (but not "list:longest") */
                                if (wim_flags[0] == WIM_LONGEST && ccline.cmdpos == j)
                                    wim_index = 1;
                                if ((wim_flags[wim_index] & WIM_LIST) != 0)
                                {
                                    if ((wim_flags[0] & WIM_LONGEST) == 0)
                                    {
                                        /* remove match */
                                        nextwild(xpc, WILD_PREV, 0, firstc != '@');
                                    }
                                    showmatches(xpc);
                                    redrawcmd();
                                    did_wild_list = true;
                                    if ((wim_flags[wim_index] & WIM_LONGEST) != 0)
                                        nextwild(xpc, WILD_LONGEST, WILD_NO_BEEP, firstc != '@');
                                    else if ((wim_flags[wim_index] & WIM_FULL) != 0)
                                        nextwild(xpc, WILD_NEXT, WILD_NO_BEEP, firstc != '@');
                                }
                                else
                                    vim_beep();
                            }
                        }
                        if (wim_index < 3)
                            wim_index++;
                        if (c == ESC)
                            gotesc = true;
                        if (res == true)
                            break cmdline_changed;
                    }

                    gotesc = false;

                    /* <S-Tab> goes to last match, in a clumsy way */
                    if (c == K_S_TAB && keyTyped)
                    {
                        if (nextwild(xpc, WILD_EXPAND_KEEP, 0, firstc != '@') == true
                                && nextwild(xpc, WILD_PREV, 0, firstc != '@') == true
                                && nextwild(xpc, WILD_PREV, 0, firstc != '@') == true)
                            break cmdline_changed;
                    }

                    if (c == NUL || c == K_ZERO)        /* NUL is stored as NL */
                        c = NL;

                    boolean do_abbr = true;             /* default: check for abbreviation */

                    /*
                     * Big switch for a typed command line character.
                     */
                    switch (c)
                    {
                        case K_BS:
                        case Ctrl_H:
                        case K_DEL:
                        case K_KDEL:
                        case Ctrl_W:
                        {
                            if (c == K_KDEL)
                                c = K_DEL;

                            /*
                             * delete current character is the same as backspace on next
                             * character, except at end of line
                             */
                            if (c == K_DEL && ccline.cmdpos != ccline.cmdlen)
                                ccline.cmdpos++;
                            if (c == K_DEL)
                                ccline.cmdpos += us_off_next(ccline.cmdbuff, ccline.cmdbuff.plus(ccline.cmdpos));
                            if (0 < ccline.cmdpos)
                            {
                                int j = ccline.cmdpos;
                                Bytes p = us_prevptr(ccline.cmdbuff, ccline.cmdbuff.plus(j));

                                if (c == Ctrl_W)
                                {
                                    while (BLT(ccline.cmdbuff, p) && vim_isspace(p.at(0)))
                                        p = us_prevptr(ccline.cmdbuff, p);
                                    int i = us_get_class(p, curbuf);
                                    while (BLT(ccline.cmdbuff, p) && us_get_class(p, curbuf) == i)
                                        p = us_prevptr(ccline.cmdbuff, p);
                                    if (us_get_class(p, curbuf) != i)
                                        p = p.plus(us_ptr2len_cc(p));
                                }

                                ccline.cmdpos = BDIFF(p, ccline.cmdbuff);
                                ccline.cmdlen -= j - ccline.cmdpos;
                                int i = ccline.cmdpos;
                                while (i < ccline.cmdlen)
                                    ccline.cmdbuff.be(i++, ccline.cmdbuff.at(j++));

                                /* Truncate at the end, required for multi-byte chars. */
                                ccline.cmdbuff.be(ccline.cmdlen, NUL);
                                redrawcmd();
                            }
                            else if (ccline.cmdlen == 0 && c != Ctrl_W && ccline.cmdprompt == null && indent == 0)
                            {
                                /* In ex and debug mode it doesn't make sense to return. */
                                if (exmode_active != 0 || ccline.cmdfirstc == '>')
                                    break cmdline_not_changed;

                                ccline.cmdbuff = null;      /* no commandline to return */
                                if (!cmd_silent)
                                {
                                    if (cmdmsg_rl)
                                        msg_col = (int)Columns[0];
                                    else
                                        msg_col = 0;
                                    msg_putchar(' ');       /* delete ':' */
                                }
                                redraw_cmdline = true;
                                break returncmd;             /* back to cmd mode */
                            }
                            break cmdline_changed;
                        }

                        case K_INS:
                        case K_KINS:
                        {
                            ccline.overstrike = !ccline.overstrike;
                            ui_cursor_shape();              /* may show different cursor shape */
                            break cmdline_not_changed;
                        }

                        case Ctrl_HAT:
                        {
                            if (map_to_exists_mode(u8(""), LANGMAP, false))
                            {
                                /* ":lmap" mappings exists, toggle use of mappings. */
                                State ^= LANGMAP;
                                if (b_im_ptr != null)
                                {
                                    if ((State & LANGMAP) != 0)
                                        b_im_ptr[0] = B_IMODE_LMAP;
                                    else
                                        b_im_ptr[0] = B_IMODE_NONE;
                                }
                            }
                            if (b_im_ptr != null)
                            {
                                if (b_im_ptr == curbuf.b_p_iminsert)
                                    set_iminsert_global();
                                else
                                    set_imsearch_global();
                            }
                            ui_cursor_shape();      /* may show different cursor shape */
                            break cmdline_not_changed;
                        }

                     /* case '@':   only in very old vi */
                        case Ctrl_U:
                        {
                            /* delete all characters left of the cursor */
                            int j = ccline.cmdpos;
                            ccline.cmdlen -= j;
                            int i = ccline.cmdpos = 0;
                            while (i < ccline.cmdlen)
                                ccline.cmdbuff.be(i++, ccline.cmdbuff.at(j++));
                            /* Truncate at the end, required for multi-byte chars. */
                            ccline.cmdbuff.be(ccline.cmdlen, NUL);
                            redrawcmd();
                            break cmdline_changed;
                        }

                        case Ctrl_Y:
                        {
                            /* Copy the modeless selection, if there is one. */
                            if (clip_star.state != SELECT_CLEARED)
                            {
                                if (clip_star.state == SELECT_DONE)
                                    clip_copy_modeless_selection(true);
                                break cmdline_not_changed;
                            }
                            break;
                        }

                        case ESC:       /* get here if p_wc != ESC or when ESC typed twice */
                        case Ctrl_C:
                        {
                            /* In exmode it doesn't make sense to return.
                             * Except when ":normal" runs out of characters. */
                            if (exmode_active != 0 && (ex_normal_busy == 0 || 0 < typebuf.tb_len))
                                break cmdline_not_changed;

                            gotesc = true;          /* will free ccline.cmdbuff after putting it in history */
                            break returncmd;         /* back to cmd mode */
                        }

                        case Ctrl_R:                    /* insert register */
                        {
                            putcmdline('"', true);
                            no_mapping++;
                            int i = c = plain_vgetc();  /* CTRL-R <char> */
                            if (i == Ctrl_O)
                                i = Ctrl_R;             /* CTRL-R CTRL-O == CTRL-R CTRL-R */
                            if (i == Ctrl_R)
                                c = plain_vgetc();      /* CTRL-R CTRL-R <char> */
                            --no_mapping;
                            /*
                             * Insert the result of an expression.
                             * Need to save the current command line, to be able to enter a new one...
                             */
                            new_cmdpos = -1;
                            if (c == '=')
                            {
                                if (ccline.cmdfirstc == '=')    /* can't do this recursively */
                                {
                                    beep_flush();
                                    c = ESC;
                                }
                                else
                                {
                                    save_cmdline(save_cli);
                                    c = get_expr_register();
                                    restore_cmdline(save_cli);
                                }
                            }
                            if (c != ESC)           /* use ESC to cancel inserting register */
                            {
                                cmdline_paste(c, i == Ctrl_R, false);

                                /* When there was a serious error abort getting the command line. */
                                if (aborting())
                                {
                                    gotesc = true;  /* will free ccline.cmdbuff after putting it in history */
                                    break returncmd; /* back to cmd mode */
                                }
                                keyTyped = false;   /* Don't do "p_wc" completion. */
                                if (0 <= new_cmdpos)
                                {
                                    /* set_cmdline_pos() was used */
                                    if (ccline.cmdlen < new_cmdpos)
                                        ccline.cmdpos = ccline.cmdlen;
                                    else
                                        ccline.cmdpos = new_cmdpos;
                                }
                            }
                            redrawcmd();
                            break cmdline_changed;
                        }

                        case Ctrl_D:
                        {
                            if (showmatches(xpc) == EXPAND_NOTHING)
                                break;      /* Use ^D as normal char instead */

                            redrawcmd();
                            continue;       /* don't do incremental search now */
                        }

                        case K_RIGHT:
                        case K_S_RIGHT:
                        case K_C_RIGHT:
                        {
                            do
                            {
                                if (ccline.cmdlen <= ccline.cmdpos)
                                    break;
                                int i = cmdline_charsize(ccline.cmdpos);
                                if (keyTyped && (int)Columns[0] * (int)Rows[0] <= ccline.cmdspos + i)
                                    break;
                                ccline.cmdspos += i;
                                ccline.cmdpos += us_ptr2len_cc(ccline.cmdbuff.plus(ccline.cmdpos));
                            }
                            while ((c == K_S_RIGHT || c == K_C_RIGHT || (mod_mask & (MOD_MASK_SHIFT|MOD_MASK_CTRL)) != 0)
                                    && ccline.cmdbuff.at(ccline.cmdpos) != (byte)' ');
                            set_cmdspos_cursor();
                            break cmdline_not_changed;
                        }

                        case K_LEFT:
                        case K_S_LEFT:
                        case K_C_LEFT:
                        {
                            if (ccline.cmdpos == 0)
                                break cmdline_not_changed;
                            do
                            {
                                --ccline.cmdpos;
                                /* move to first byte of char */
                                ccline.cmdpos -= us_head_off(ccline.cmdbuff, ccline.cmdbuff.plus(ccline.cmdpos));
                                ccline.cmdspos -= cmdline_charsize(ccline.cmdpos);
                            }
                            while (0 < ccline.cmdpos
                                    && (c == K_S_LEFT || c == K_C_LEFT
                                        || (mod_mask & (MOD_MASK_SHIFT|MOD_MASK_CTRL)) != 0)
                                    && ccline.cmdbuff.at(ccline.cmdpos - 1) != (byte)' ');
                            set_cmdspos_cursor();
                            break cmdline_not_changed;
                        }

                        case K_IGNORE:
                            break cmdline_not_changed;       /* Ignore mouse event or ex_window() result. */

                        case K_MIDDLEDRAG:
                        case K_MIDDLERELEASE:
                            break cmdline_not_changed;       /* Ignore mouse */

                        case K_MIDDLEMOUSE:
                        {
                            if (!mouse_has(MOUSE_COMMAND))
                                break cmdline_not_changed;   /* Ignore mouse */
                            if (clip_star.available)
                                cmdline_paste('*', true, true);
                            else
                                cmdline_paste(0, true, true);
                            redrawcmd();
                            break cmdline_changed;
                        }

                        case K_DROP:
                        {
                            cmdline_paste('~', true, false);
                            redrawcmd();
                            break cmdline_changed;
                        }

                        case K_LEFTDRAG:
                        case K_LEFTRELEASE:
                        case K_RIGHTDRAG:
                        case K_RIGHTRELEASE:
                        {
                            /* Ignore drag and release events when the button-down wasn't seen before. */
                            if (ignore_drag_release)
                                break cmdline_not_changed;
                            /* FALLTHROUGH */
                        }

                        case K_LEFTMOUSE:
                        case K_RIGHTMOUSE:
                        {
                            ignore_drag_release = (c == K_LEFTRELEASE || c == K_RIGHTRELEASE);

                            if (!mouse_has(MOUSE_COMMAND))
                                break cmdline_not_changed;   /* Ignore mouse. */

                            if (mouse_row < cmdline_row && clip_star.available)
                            {
                                /* Handle modeless selection. */
                                boolean[] is_click = new boolean[1];
                                boolean[] is_drag = new boolean[1];
                                int button = get_mouse_button(KEY2TERMCAP1(c), is_click, is_drag);
                                if (mouse_model_popup() && button == MOUSE_LEFT && (mod_mask & MOD_MASK_SHIFT) != 0)
                                {
                                    /* Translate shift-left to right button. */
                                    button = MOUSE_RIGHT;
                                    mod_mask &= ~MOD_MASK_SHIFT;
                                }
                                clip_modeless(button, is_click[0], is_drag[0]);
                                break cmdline_not_changed;
                            }

                            set_cmdspos();
                            for (ccline.cmdpos = 0; ccline.cmdpos < ccline.cmdlen; ccline.cmdpos++)
                            {
                                int i = cmdline_charsize(ccline.cmdpos);
                                if (mouse_row <= cmdline_row + ccline.cmdspos / (int)Columns[0]
                                            && mouse_col < ccline.cmdspos % (int)Columns[0] + i)
                                    break;

                                /* Count ">" for double-wide char that doesn't fit. */
                                correct_cmdspos(ccline.cmdpos, i);
                                ccline.cmdpos += us_ptr2len_cc(ccline.cmdbuff.plus(ccline.cmdpos)) - 1;

                                ccline.cmdspos += i;
                            }
                            break cmdline_not_changed;
                        }

                        /* Mouse scroll wheel: ignored here. */
                        case K_MOUSEDOWN:
                        case K_MOUSEUP:
                        case K_MOUSELEFT:
                        case K_MOUSERIGHT:
                        /* Alternate buttons ignored here. */
                        case K_X1MOUSE:
                        case K_X1DRAG:
                        case K_X1RELEASE:
                        case K_X2MOUSE:
                        case K_X2DRAG:
                        case K_X2RELEASE:
                            break cmdline_not_changed;

                        case K_SELECT:      /* end of Select mode mapping - ignore */
                            break cmdline_not_changed;

                        case Ctrl_B:        /* begin of command line */
                        case K_HOME:
                        case K_KHOME:
                        case K_S_HOME:
                        case K_C_HOME:
                        {
                            ccline.cmdpos = 0;
                            set_cmdspos();
                            break cmdline_not_changed;
                        }

                        case Ctrl_E:        /* end of command line */
                        case K_END:
                        case K_KEND:
                        case K_S_END:
                        case K_C_END:
                        {
                            ccline.cmdpos = ccline.cmdlen;
                            set_cmdspos_cursor();
                            break cmdline_not_changed;
                        }

                        case Ctrl_A:        /* all matches */
                        {
                            if (nextwild(xpc, WILD_ALL, 0, firstc != '@') == false)
                                break;
                            break cmdline_changed;
                        }

                        case Ctrl_L:
                        {
                            if (p_is[0] && !cmd_silent && (firstc == '/' || firstc == '?'))
                            {
                                /* Add a character from under the cursor for 'incsearch'. */
                                if (did_incsearch && !eqpos(curwin.w_cursor, old_cursor))
                                {
                                    c = gchar_cursor();
                                    /* If 'ignorecase' and 'smartcase' are set and the
                                     * command line has no uppercase characters, convert
                                     * the character to lowercase */
                                    if (p_ic[0] && p_scs[0] && !pat_has_uppercase(ccline.cmdbuff))
                                        c = utf_tolower(c);
                                    if (c != NUL)
                                    {
                                        if (c == firstc || vim_strchr(p_magic[0] ? u8("\\^$.*[") : u8("\\^$"), c) != null)
                                        {
                                            /* put a backslash before special characters */
                                            stuffcharReadbuff(c);
                                            c = '\\';
                                        }
                                        break;
                                    }
                                }
                                break cmdline_not_changed;
                            }

                            /* completion: longest common part */
                            if (nextwild(xpc, WILD_LONGEST, 0, firstc != '@') == false)
                                break;
                            break cmdline_changed;
                        }

                        case Ctrl_N:        /* next match */
                        case Ctrl_P:        /* previous match */
                            if (0 < xpc.xp_numfiles)
                            {
                                if (nextwild(xpc, (c == Ctrl_P) ? WILD_PREV : WILD_NEXT, 0, firstc != '@') == false)
                                    break;
                                break cmdline_changed;
                            }

                        case K_UP:
                        case K_DOWN:
                        case K_S_UP:
                        case K_S_DOWN:
                        case K_PAGEUP:
                        case K_KPAGEUP:
                        case K_PAGEDOWN:
                        case K_KPAGEDOWN:
                        {
                            if (hislen == 0 || firstc == NUL)       /* no history */
                                break cmdline_not_changed;

                            int i = hiscnt;

                            /* save current command string so it can be restored later */
                            if (lookfor == null)
                            {
                                lookfor = STRDUP(ccline.cmdbuff);
                                if (lookfor == null)
                                    break cmdline_not_changed;
                                lookfor.be(ccline.cmdpos, NUL);
                            }

                            for (int n = strlen(lookfor); ; )
                            {
                                /* one step backwards */
                                if (c == K_UP|| c == K_S_UP || c == Ctrl_P || c == K_PAGEUP || c == K_KPAGEUP)
                                {
                                    if (hiscnt == hislen)   /* first time */
                                        hiscnt = hisidx[histype];
                                    else if (hiscnt == 0 && hisidx[histype] != hislen - 1)
                                        hiscnt = hislen - 1;
                                    else if (hiscnt != hisidx[histype] + 1)
                                        --hiscnt;
                                    else                    /* at top of list */
                                    {
                                        hiscnt = i;
                                        break;
                                    }
                                }
                                else    /* one step forwards */
                                {
                                    /* on last entry, clear the line */
                                    if (hiscnt == hisidx[histype])
                                    {
                                        hiscnt = hislen;
                                        break;
                                    }

                                    /* not on a history line, nothing to do */
                                    if (hiscnt == hislen)
                                        break;
                                    if (hiscnt == hislen - 1)   /* wrap around */
                                        hiscnt = 0;
                                    else
                                        hiscnt++;
                                }
                                if (hiscnt < 0 || history[histype][hiscnt].hisstr == null)
                                {
                                    hiscnt = i;
                                    break;
                                }
                                if ((c != K_UP && c != K_DOWN)
                                        || hiscnt == i
                                        || STRNCMP(history[histype][hiscnt].hisstr, lookfor, n) == 0)
                                    break;
                            }

                            if (hiscnt != i)        /* jumped to other entry */
                            {
                                ccline.cmdbuff = null;
                                xpc.xp_context = EXPAND_NOTHING;

                                Bytes p;
                                if (hiscnt == hislen)
                                    p = lookfor;    /* back to the old one */
                                else
                                    p = history[histype][hiscnt].hisstr;

                                int old_firstc;
                                if (histype == HIST_SEARCH && BNE(p, lookfor) && (old_firstc = p.at(strlen(p) + 1)) != firstc)
                                {
                                    /* Correct for the separator character used when
                                     * adding the history entry vs the one used now.
                                     * First loop: count length.
                                     * Second loop: copy the characters. */
                                    for (int round = 0; round <= 1; round++)
                                    {
                                        int len = 0;

                                        for (/*int */i = 0; p.at(i) != NUL; i++)
                                        {
                                            /* Replace old sep with new sep, unless it is escaped. */
                                            if (p.at(i) == old_firstc && (i == 0 || p.at(i - 1) != (byte)'\\'))
                                            {
                                                if (0 < round)
                                                    ccline.cmdbuff.be(len, firstc);
                                            }
                                            else
                                            {
                                                /* Escape new sep, unless it is already escaped. */
                                                if (p.at(i) == firstc && (i == 0 || p.at(i - 1) != (byte)'\\'))
                                                {
                                                    if (0 < round)
                                                        ccline.cmdbuff.be(len, (byte)'\\');
                                                    len++;
                                                }
                                                if (0 < round)
                                                    ccline.cmdbuff.be(len, p.at(i));
                                            }
                                            len++;
                                        }

                                        if (0 < round)
                                            ccline.cmdbuff.be(len, NUL);
                                        else
                                            alloc_cmdbuff(len);
                                    }
                                }
                                else
                                {
                                    alloc_cmdbuff(strlen(p));
                                    STRCPY(ccline.cmdbuff, p);
                                }

                                ccline.cmdpos = ccline.cmdlen = strlen(ccline.cmdbuff);
                                redrawcmd();
                                break cmdline_changed;
                            }
                            beep_flush();
                            break cmdline_not_changed;
                        }

                        case Ctrl_V:
                        case Ctrl_Q:
                        {
                            ignore_drag_release = true;
                            putcmdline('^', true);
                            c = get_literal();          /* get next (two) character(s) */
                            do_abbr = false;            /* don't do abbreviation now */
                            /* may need to remove ^ when composing char was typed */
                            if (utf_iscomposing(c) && !cmd_silent)
                            {
                                draw_cmdline(ccline.cmdpos, ccline.cmdlen - ccline.cmdpos);
                                msg_putchar(' ');
                                cursorcmd();
                            }
                            break;
                        }

                        case Ctrl_K:
                        {
                            ignore_drag_release = true;
                            putcmdline('?', true);
                            c = get_digraph(true);
                            if (c != NUL)
                                break;

                            redrawcmd();
                            break cmdline_not_changed;
                        }

                        case Ctrl__:        /* CTRL-_: switch language mode */
                        {
                            if (!p_ari[0])
                                break;

                            break cmdline_not_changed;
                        }

                        default:
                        {
                            if (c == intr_char)
                            {
                                gotesc = true;      /* will free ccline.cmdbuff after putting it in history */
                                break returncmd;     /* back to Normal mode */
                            }
                            /*
                             * Normal character with no special meaning.  Just set mod_mask
                             * to 0x0 so that typing Shift-Space in the GUI doesn't enter
                             * the string <S-Space>.  This should only happen after ^V.
                             */
                            if (!is_special(c))
                                mod_mask = 0x0;
                            break;
                        }
                    }
                    /*
                     * End of switch on command line character.
                     * We come here if we have a normal character.
                     */

                    if (do_abbr && (is_special(c) || !vim_iswordc(c, curbuf))
                            /* Add ABBR_OFF for characters above 0x100, this is what check_abbr() expects. */
                            && (ccheck_abbr((0x100 <= c) ? (c + ABBR_OFF) : c) || c == Ctrl_RSB))
                        break cmdline_changed;

                    /*
                     * put the character in the command line
                     */
                    if (is_special(c) || mod_mask != 0)
                        put_on_cmdline(get_special_key_name(c, mod_mask), -1, true);
                    else
                    {
                        int j = utf_char2bytes(c, ioBuff);
                        ioBuff.be(j, NUL);        /* exclude composing chars */
                        put_on_cmdline(ioBuff, j, true);
                    }
                    break cmdline_changed;
                }

                /*
                 * This part implements incremental searches for "/" and "?"
                 * Jump to cmdline_not_changed when a character has been read but the command
                 * line did not change.  Then we only search and redraw if something changed in the past.
                 * Jump to cmdline_changed when the command line did change.
                 * (Sorry for the goto's, I know it is ugly).
                 */
                if (!incsearch_postponed)
                    continue;
            }

            /*
             * 'incsearch' highlighting.
             */
            if (p_is[0] && !cmd_silent && (firstc == '/' || firstc == '?'))
            {
                /* if there is a character waiting, search and redraw later */
                if (char_avail())
                {
                    incsearch_postponed = true;
                    continue;
                }
                incsearch_postponed = false;
                COPY_pos(curwin.w_cursor, old_cursor); /* start at old position */

                /* If there is no command line, don't do anything. */
                int i;
                if (ccline.cmdlen == 0)
                    i = 0;
                else
                {
                    cursor_off();               /* so the user knows we're busy */
                    out_flush();
                    emsg_off++;                 /* so it doesn't beep if bad expr */
                    /* Set the time limit to half a second. */
                    timeval_C tm = new timeval_C();
                    profile_setlimit(500L, tm);
                    i = do_search(null, (byte)firstc, ccline.cmdbuff, count,
                            SEARCH_KEEP + SEARCH_OPT + SEARCH_NOOF + SEARCH_PEEK, tm);
                    --emsg_off;
                    /* if interrupted while searching, behave like it failed */
                    if (got_int)
                    {
                        vpeekc();               /* remove <C-C> from input stream */
                        got_int = false;        /* don't abandon the command line */
                        i = 0;
                    }
                    else if (char_avail())
                        /* cancelled searching because a char was typed */
                        incsearch_postponed = true;
                }
                if (i != 0)
                    highlight_match = true;         /* highlight position */
                else
                    highlight_match = false;        /* remove highlight */

                /* First restore the old curwin values, so the screen is
                 * positioned in the same way as the actual search command. */
                curwin.w_leftcol = old_leftcol;
                curwin.w_topline = old_topline;
                curwin.w_botline = old_botline;
                changed_cline_bef_curs();
                update_topline();

                pos_C end_pos = new pos_C();
                if (i != 0)
                {
                    pos_C save_pos = new pos_C();
                    COPY_pos(save_pos, curwin.w_cursor);

                    /*
                     * First move the cursor to the end of the match, then to the start.
                     * This moves the whole match onto the screen when 'nowrap' is set.
                     */
                    curwin.w_cursor.lnum += search_match_lines;
                    curwin.w_cursor.col = search_match_endcol;
                    if (curwin.w_cursor.lnum > curbuf.b_ml.ml_line_count)
                    {
                        curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
                        coladvance(MAXCOL);
                    }
                    validate_cursor();
                    COPY_pos(end_pos, curwin.w_cursor);
                    COPY_pos(curwin.w_cursor, save_pos);
                }
                else
                    COPY_pos(end_pos, curwin.w_cursor);

                validate_cursor();
                /* May redraw the status line to show the cursor position. */
                if (p_ru[0] && 0 < curwin.w_status_height)
                    curwin.w_redr_status = true;

                save_cmdline(save_cli);
                update_screen(SOME_VALID);
                restore_cmdline(save_cli);

                /* Leave it at the end to make CTRL-R CTRL-W work. */
                if (i != 0)
                    COPY_pos(curwin.w_cursor, end_pos);

                msg_starthere();
                redrawcmdline();
                did_incsearch = true;
            }

            if (cmdmsg_rl)
                /* Always redraw the whole command line to fix shaping and right-left typing.
                 * Not efficient, but it works.  Do it only when there are no characters left
                 * to read to avoid useless intermediate redraws. */
                if (vpeekc() == NUL)
                    redrawcmd();
        }

        cmdmsg_rl = false;

        expandCleanup(xpc);
        ccline.xpc = null;

        if (did_incsearch)
        {
            COPY_pos(curwin.w_cursor, old_cursor);
            curwin.w_curswant = old_curswant;
            curwin.w_leftcol = old_leftcol;
            curwin.w_topline = old_topline;
            curwin.w_botline = old_botline;
            highlight_match = false;
            validate_cursor();      /* needed for TAB */
            redraw_later(SOME_VALID);
        }

        if (ccline.cmdbuff != null)
        {
            /*
             * Put line in history buffer (":" and "=" only when it was typed).
             */
            if (ccline.cmdlen != 0 && firstc != NUL && (some_key_typed || histype == HIST_SEARCH))
            {
                add_to_history(histype, ccline.cmdbuff, true, histype == HIST_SEARCH ? firstc : NUL);
                if (firstc == ':')
                    new_last_cmdline = STRDUP(ccline.cmdbuff);
            }

            if (gotesc)         /* abandon command line */
            {
                ccline.cmdbuff = null;
                if (msg_scrolled == 0)
                    compute_cmdrow();
                msg(u8(""));
                redraw_cmdline = true;
            }
        }

        /*
         * If the screen was shifted up, redraw the whole screen (later).
         * If the line is too long, clear it, so ruler and shown command
         * do not get printed in the middle of it.
         */
        msg_check();
        msg_scroll = save_msg_scroll;
        redir_off = false;

        /* When the command line was typed, no need for a wait-return prompt. */
        if (some_key_typed)
            need_wait_return = false;

        State = save_State;
        setmouse();
        ui_cursor_shape();          /* may show different cursor shape */

        Bytes p = ccline.cmdbuff;
        /* Make ccline empty, getcmdline() may try to use it. */
        ccline.cmdbuff = null;
        return p;
    }

    /*
     * Get a command line with a prompt.
     * This is prepared to be called recursively from getcmdline()
     * (e.g. by f_input() when evaluating an expression from CTRL-R =).
     * Returns the command line in allocated memory, or null.
     */
    /*private*/ static Bytes getcmdline_prompt(int firstc, Bytes prompt, int attr, int xp_context, Bytes xp_arg)
        /* prompt: command line prompt */
        /* attr: attributes for prompt */
        /* xp_context: type of expansion */
        /* xp_arg: user-defined expansion argument */
    {
        Bytes s;

        int msg_col_save = msg_col;

        cmdline_info_C save_cli = save_cmdline_alloc();

        ccline.cmdprompt = prompt;
        ccline.cmdattr = attr;
        ccline.xp_context = xp_context;
        ccline.xp_arg = xp_arg;
        ccline.input_fn = (firstc == '@');

        s = getcmdline(firstc, 1L, 0);

        restore_cmdline_alloc(save_cli);
        /* Restore msg_col, the prompt from input() may have changed it.
         * But only if called recursively and the commandline is therefore being
         * restored to an old one; if not, the input() prompt stays on the screen,
         * so we need its modified msg_col left intact. */
        if (ccline.cmdbuff != null)
            msg_col = msg_col_save;

        return s;
    }

    /*
     * Return true when the text must not be changed and we can't switch to
     * another window or buffer.  Used when editing the command line, evaluating
     * 'balloonexpr', etc.
     */
    /*private*/ static boolean text_locked()
    {
        if (cmdwin_type != 0)
            return true;

        return textlock != 0;
    }

    /*
     * Give an error message for a command that isn't allowed while the cmdline
     * window is open or editing the cmdline in another way.
     */
    /*private*/ static void text_locked_msg()
    {
        if (cmdwin_type != 0)
            emsg(e_cmdwin);
        else
            emsg(e_secure);
    }

    /*
     * Check if "curbuf_lock" or "allbuf_lock" is set and return true when it is
     * and give an error message.
     */
    /*private*/ static boolean curbuf_locked()
    {
        if (0 < curbuf_lock)
        {
            emsg(u8("E788: Not allowed to edit another buffer now"));
            return true;
        }
        return allbuf_locked();
    }

    /*
     * Check if "allbuf_lock" is set and return true when it is and give an error message.
     */
    /*private*/ static boolean allbuf_locked()
    {
        if (0 < allbuf_lock)
        {
            emsg(u8("E811: Not allowed to change buffer information now"));
            return true;
        }
        return false;
    }

    /*private*/ static int cmdline_charsize(int idx)
    {
        if (0 < cmdline_star)           /* showing '*', always 1 position */
            return 1;

        return mb_ptr2cells(ccline.cmdbuff.plus(idx));
    }

    /*
     * Compute the offset of the cursor on the command line for the prompt and indent.
     */
    /*private*/ static void set_cmdspos()
    {
        if (ccline.cmdfirstc != NUL)
            ccline.cmdspos = 1 + ccline.cmdindent;
        else
            ccline.cmdspos = 0 + ccline.cmdindent;
    }

    /*
     * Compute the screen position for the cursor on the command line.
     */
    /*private*/ static void set_cmdspos_cursor()
    {
        int i, m, c;

        set_cmdspos();
        if (keyTyped)
        {
            m = (int)Columns[0] * (int)Rows[0];
            if (m < 0)      /* overflow, Columns or Rows at weird value */
                m = MAXCOL;
        }
        else
            m = MAXCOL;
        for (i = 0; i < ccline.cmdlen && i < ccline.cmdpos; i++)
        {
            c = cmdline_charsize(i);
            /* Count ">" for double-wide multi-byte char that doesn't fit. */
            correct_cmdspos(i, c);
            /* If the cmdline doesn't fit, show cursor on last visible char.
             * Don't move the cursor itself, so we can still append. */
            if (m <= (ccline.cmdspos += c))
            {
                ccline.cmdspos -= c;
                break;
            }
            i += us_ptr2len_cc(ccline.cmdbuff.plus(i)) - 1;
        }
    }

    /*
     * Check if the character at "idx", which is "cells" wide, is a multi-byte
     * character that doesn't fit, so that a ">" must be displayed.
     */
    /*private*/ static void correct_cmdspos(int idx, int cells)
    {
        if (1 < us_ptr2len_cc(ccline.cmdbuff.plus(idx))
                    && 1 < us_ptr2cells(ccline.cmdbuff.plus(idx))
                    && (int)Columns[0] < ccline.cmdspos % (int)Columns[0] + cells)
            ccline.cmdspos++;
    }

    /*
     * Get an Ex command line for the ":" command.
     */
    /*private*/ static final getline_C getexline = new getline_C()
    {
        public Bytes getline(int c, Object _cookie, int indent)
            /* c: normally ':', NUL for ":append" */
            /* indent: indent for inside conditionals */
        {
            /* When executing a register, remove ':' that's in front of each line. */
            if (exec_from_reg && vpeekc() == ':')
                vgetc();
            return getcmdline(c, 1L, indent);
        }
    };

    /*
     * Get an Ex command line for Ex mode.
     * In Ex mode we only use the OS supplied line editing features and no mappings or abbreviations.
     * Returns a string in allocated memory or null.
     */
    /*private*/ static final getline_C getexmodeline = new getline_C()
    {
        public Bytes getline(int promptc, Object _cookie, int indent)
            /* promptc: normally ':', NUL for ":append" and '?' for :s prompt */
            /* indent: indent for inside conditionals */
        {
            /* Switch cursor on now.  This avoids that it happens after the "\n",
             * which confuses the system function that computes tabstops. */
            cursor_on();

            /* always start in column 0; write a newline if necessary */
            compute_cmdrow();
            if ((msg_col != 0 || msg_didout) && promptc != '?')
                msg_putchar('\n');
            int startcol = 0;
            if (promptc == ':')
            {
                /* indent that is only displayed, not in the line itself */
                if (p_prompt[0])
                    msg_putchar(':');
                while (0 < indent--)
                    msg_putchar(' ');
                startcol = msg_col;
            }

            barray_C line_ba = new barray_C(30);

            /* autoindent for :insert and :append is in the line itself */
            int vcol = 0;
            if (promptc <= 0)
            {
                vcol = indent;
                while (8 <= indent)
                {
                    ba_append(line_ba, TAB);
                    msg_puts(u8("        "));
                    indent -= 8;
                }
                while (0 < indent--)
                {
                    ba_append(line_ba, (byte)' ');
                    msg_putchar(' ');
                }
            }
            no_mapping++;
            allow_keys++;

            int c1 = 0;
            boolean escaped = false;        /* CTRL-V typed */
            /*
             * Get the line, one character at a time.
             */
            got_int = false;
            loop:
            while (!got_int)
            {
                /* Get one character at a time.
                 * Don't use inchar(), it can't handle special characters. */
                int prev_char = c1;
                c1 = vgetc();

                /*
                 * Handle line editing.
                 * Previously this was left to the system, putting the terminal in
                 * cooked mode, but then CTRL-D and CTRL-T can't be used properly.
                 */
                if (got_int)
                {
                    msg_putchar('\n');
                    break loop;
                }

                ba_grow(line_ba, 40);

                redraw:
                {
                    Bytes p;

                    add_indent:
                    {
                        if (!escaped)
                        {
                            /* CR typed means "enter", which is NL */
                            if (c1 == '\r')
                                c1 = '\n';

                            if (c1 == BS || c1 == K_BS || c1 == DEL || c1 == K_DEL || c1 == K_KDEL)
                            {
                                if (0 < line_ba.ba_len)
                                {
                                    p = new Bytes(line_ba.ba_data);
                                    p.be(line_ba.ba_len, NUL);
                                    int len = us_head_off(p, p.plus(line_ba.ba_len - 1)) + 1;
                                    line_ba.ba_len -= len;
                                    break redraw;
                                }
                                continue loop;
                            }

                            if (c1 == Ctrl_U)
                            {
                                msg_col = startcol;
                                msg_clr_eos();
                                line_ba.ba_len = 0;
                                break redraw;
                            }

                            if (c1 == Ctrl_T)
                            {
                                long sw = get_sw_value(curbuf);
                                p = new Bytes(line_ba.ba_data);
                                p.be(line_ba.ba_len, NUL);
                                indent = get_indent_str(p, 8, false);
                                indent += sw - indent % sw;
                                break add_indent;
                            }

                            if (c1 == Ctrl_D)
                            {
                                /* Delete one shiftwidth. */
                                p = new Bytes(line_ba.ba_data);
                                if (prev_char == '0' || prev_char == '^')
                                {
                                    if (prev_char == '^')
                                        ex_keep_indent = true;
                                    indent = 0;
                                    p.be(--line_ba.ba_len, NUL);
                                }
                                else
                                {
                                    p.be(line_ba.ba_len, NUL);
                                    indent = get_indent_str(p, 8, false);
                                    if (0 < indent)
                                    {
                                        --indent;
                                        indent -= indent % get_sw_value(curbuf);
                                    }
                                }
                                while (indent < get_indent_str(p, 8, false))
                                {
                                    Bytes s = skipwhite(p);
                                    BCOPY(s, -1, s, 0, line_ba.ba_len - BDIFF(s, p) + 1);
                                    --line_ba.ba_len;
                                }
                                break add_indent;
                            }

                            if (c1 == Ctrl_V || c1 == Ctrl_Q)
                            {
                                escaped = true;
                                continue loop;
                            }

                            /* Ignore special key codes: mouse movement, K_IGNORE, etc. */
                            if (is_special(c1))
                                continue loop;
                        }

                        if (is_special(c1))
                            c1 = '?';
                        int len = utf_char2bytes(c1, new Bytes(line_ba.ba_data, line_ba.ba_len));
                        if (c1 == '\n')
                            msg_putchar('\n');
                        else if (c1 == TAB)
                        {
                            /* Don't use chartabsize(), 'ts' can be different. */
                            do
                            {
                                msg_putchar(' ');
                            } while (++vcol % 8 != 0);
                        }
                        else
                        {
                            msg_outtrans_len(new Bytes(line_ba.ba_data, line_ba.ba_len), len);
                            vcol += mb_char2cells(c1);
                        }
                        line_ba.ba_len += len;
                        escaped = false;

                        windgoto(msg_row, msg_col);
                        Bytes pend = new Bytes(line_ba.ba_data, line_ba.ba_len);

                        /* We are done when a NL is entered, but not when it comes after
                         * an odd number of backslashes, that results in a NUL. */
                        if (0 < line_ba.ba_len && pend.at(-1) == (byte)'\n')
                        {
                            int bcount = 0;

                            while (bcount <= line_ba.ba_len - 2 && pend.at(-2 - bcount) == (byte)'\\')
                                bcount++;

                            if (0 < bcount)
                            {
                                /* Halve the number of backslashes: "\NL" -> "NUL", "\\NL" -> "\NL", etc. */
                                line_ba.ba_len -= (bcount + 1) / 2;
                                pend = pend.minus((bcount + 1) / 2);
                                pend.be(-1, (byte)'\n');
                            }

                            if ((bcount & 1) == 0)
                            {
                                --line_ba.ba_len;
                                pend = pend.minus(1);
                                pend.be(0, NUL);
                                break loop;
                            }
                        }

                        continue loop;
                    }

                    while (get_indent_str(p, 8, false) < indent)
                    {
                        p = new Bytes(ba_grow(line_ba, 2)); /* one more for the NUL */
                        Bytes s = skipwhite(p);
                        BCOPY(s, 1, s, 0, line_ba.ba_len - BDIFF(s, p) + 1);
                        s.be(0, (byte)' ');
                        line_ba.ba_len++;
                    }
                }

                /* redraw the line */
                msg_col = startcol;
                vcol = 0;
                Bytes p = new Bytes(line_ba.ba_data);
                p.be(line_ba.ba_len, NUL);
                for (int i = 0; i < line_ba.ba_len; )
                {
                    if (p.at(0) == TAB)
                    {
                        do
                        {
                            msg_putchar(' ');
                        } while (++vcol % 8 != 0);
                        p = p.plus(1);
                        i++;
                    }
                    else
                    {
                        int len = us_ptr2len_cc(p);
                        msg_outtrans_len(p, len);
                        vcol += mb_ptr2cells(p);
                        p = p.plus(len);
                        i += len;
                    }
                }
                msg_clr_eos();
                windgoto(msg_row, msg_col);
            }

            --no_mapping;
            --allow_keys;

            /* make following messages go to the next line */
            msg_didout = false;
            msg_col = 0;
            if (msg_row < Rows[0] - 1)
                msg_row++;
            emsg_on_display = false;            /* don't want ui_delay() */

            if (got_int)
                ba_clear(line_ba);

            return new Bytes(line_ba.ba_data);
        }
    };

    /*
     * Allocate a new command line buffer.
     * Assigns the new buffer to ccline.cmdbuff and ccline.cmdbufflen.
     * Returns the new value of ccline.cmdbuff and ccline.cmdbufflen.
     */
    /*private*/ static void alloc_cmdbuff(int len)
    {
        /*
         * give some extra space to avoid having to allocate all the time
         */
        if (len < 80)
            len = 100;
        else
            len += 20;

        ccline.cmdbuff = new Bytes(len);
        ccline.cmdbufflen = len;
    }

    /*
     * Re-allocate the command line to length len + something extra.
     */
    /*private*/ static void realloc_cmdbuff(int len)
    {
        if (len < ccline.cmdbufflen)
            return;                             /* no need to resize */

        Bytes p = ccline.cmdbuff;
        alloc_cmdbuff(len);                     /* will get some more */
        /* There isn't always a NUL after the command, but it may need to be there,
         * thus copy up to the NUL and add a NUL. */
        BCOPY(ccline.cmdbuff, p, ccline.cmdlen);
        ccline.cmdbuff.be(ccline.cmdlen, NUL);

        if (ccline.xpc != null
                && ccline.xpc.xp_pattern != null
                && ccline.xpc.xp_context != EXPAND_NOTHING
                && ccline.xpc.xp_context != EXPAND_UNSUCCESSFUL)
        {
            int i = BDIFF(ccline.xpc.xp_pattern, p);

            /* If xp_pattern points inside the old cmdbuff,
             * it needs to be adjusted to point into the newly allocated memory. */
            if (0 <= i && i <= ccline.cmdlen)
                ccline.xpc.xp_pattern = ccline.cmdbuff.plus(i);
        }
    }

    /*
     * Draw part of the cmdline at the current cursor position;
     * but draw stars when cmdline_star is true.
     */
    /*private*/ static void draw_cmdline(int start, int len)
    {
        if (0 < cmdline_star)
            for (int i = 0; i < len; i++)
            {
                msg_putchar('*');
                i += us_ptr2len_cc(ccline.cmdbuff.plus(start + i)) - 1;
            }
        else
            msg_outtrans_len(ccline.cmdbuff.plus(start), len);
    }

    /*
     * Put a character on the command line.  Shifts the following text to the
     * right when "shift" is true.  Used for CTRL-V, CTRL-K, etc.
     * "c" must be printable (fit in one display cell)!
     */
    /*private*/ static void putcmdline(int c, boolean shift)
    {
        if (cmd_silent)
            return;
        msg_no_more = true;
        msg_putchar(c);
        if (shift)
            draw_cmdline(ccline.cmdpos, ccline.cmdlen - ccline.cmdpos);
        msg_no_more = false;
        cursorcmd();
    }

    /*
     * Undo a putcmdline(c, false).
     */
    /*private*/ static void unputcmdline()
    {
        if (cmd_silent)
            return;
        msg_no_more = true;
        if (ccline.cmdlen == ccline.cmdpos)
            msg_putchar(' ');
        else
            draw_cmdline(ccline.cmdpos, us_ptr2len_cc(ccline.cmdbuff.plus(ccline.cmdpos)));
        msg_no_more = false;
        cursorcmd();
    }

    /*
     * Put the given string, of the given length, onto the command line.
     * If len is -1, then strlen() is used to calculate the length.
     * If 'redraw' is true then the new part of the command line, and the remaining part
     * will be redrawn, otherwise it will not.  If this function is called twice in a row,
     * then 'redraw' should be false and redrawcmd() should be called afterwards.
     */
    /*private*/ static void put_on_cmdline(Bytes str, int len, boolean redraw)
    {
        if (len < 0)
            len = strlen(str);

        /* Check if ccline.cmdbuff needs to be longer. */
        if (ccline.cmdbufflen <= ccline.cmdlen + len + 1)
            realloc_cmdbuff(ccline.cmdlen + len + 1);

        if (!ccline.overstrike)
        {
            BCOPY(ccline.cmdbuff, ccline.cmdpos + len, ccline.cmdbuff, ccline.cmdpos, ccline.cmdlen - ccline.cmdpos);
            ccline.cmdlen += len;
        }
        else
        {
            /* Count nr of characters in the new string. */
            int m = 0, n;
            for (n = 0; n < len; n += us_ptr2len_cc(str.plus(n)))
                m++;
            /* Count nr of bytes in cmdline that are overwritten by these characters. */
            for (n = ccline.cmdpos; n < ccline.cmdlen && 0 < m; n += us_ptr2len_cc(ccline.cmdbuff.plus(n)))
                --m;
            if (n < ccline.cmdlen)
            {
                BCOPY(ccline.cmdbuff, ccline.cmdpos + len, ccline.cmdbuff, n, ccline.cmdlen - n);
                ccline.cmdlen += ccline.cmdpos + len - n;
            }
            else
                ccline.cmdlen = ccline.cmdpos + len;
        }
        BCOPY(ccline.cmdbuff, ccline.cmdpos, str, 0, len);
        ccline.cmdbuff.be(ccline.cmdlen, NUL);

        /* When the inserted text starts with a composing character,
         * backup to the character before it.  There could be two of them.
         */
        int i = 0;
        int c = us_ptr2char(ccline.cmdbuff.plus(ccline.cmdpos));
        while (0 < ccline.cmdpos && utf_iscomposing(c))
        {
            i = us_head_off(ccline.cmdbuff, ccline.cmdbuff.plus(ccline.cmdpos - 1)) + 1;
            ccline.cmdpos -= i;
            len += i;
            c = us_ptr2char(ccline.cmdbuff.plus(ccline.cmdpos));
        }
        if (i != 0)
        {
            /* Also backup the cursor position. */
            i = mb_ptr2cells(ccline.cmdbuff.plus(ccline.cmdpos));
            ccline.cmdspos -= i;
            msg_col -= i;
            if (msg_col < 0)
            {
                msg_col += (int)Columns[0];
                --msg_row;
            }
        }

        if (redraw && !cmd_silent)
        {
            msg_no_more = true;
            i = cmdline_row;
            cursorcmd();
            draw_cmdline(ccline.cmdpos, ccline.cmdlen - ccline.cmdpos);
            /* Avoid clearing the rest of the line too often. */
            if (cmdline_row != i || ccline.overstrike)
                msg_clr_eos();
            msg_no_more = false;
        }

        int m;
        if (keyTyped)
        {
            m = (int)Columns[0] * (int)Rows[0];
            if (m < 0)      /* overflow, Columns or Rows at weird value */
                m = MAXCOL;
        }
        else
            m = MAXCOL;

        for (i = 0; i < len; i++)
        {
            c = cmdline_charsize(ccline.cmdpos);
            /* count ">" for a double-wide char that doesn't fit. */
            correct_cmdspos(ccline.cmdpos, c);
            /* Stop cursor at the end of the screen, but do increment the insert position,
             * so that entering a very long command works, even though you can't see it. */
            if (ccline.cmdspos + c < m)
                ccline.cmdspos += c;

            c = us_ptr2len_cc(ccline.cmdbuff.plus(ccline.cmdpos)) - 1;
            if (c > len - i - 1)
                c = len - i - 1;
            ccline.cmdpos += c;
            i += c;

            ccline.cmdpos++;
        }

        if (redraw)
            msg_check();
    }

    /*private*/ static cmdline_info_C prev_ccline;

    /*
     * Save ccline, because obtaining the "=" register may execute "normal :cmd" and overwrite it.
     * But get_cmdline_str() may need it, thus make it available globally in prev_ccline.
     */
    /*private*/ static void save_cmdline(cmdline_info_C cli)
    {
        if (prev_ccline == null)
            prev_ccline = new cmdline_info_C();

        COPY_cmdline_info(cli, prev_ccline);
        COPY_cmdline_info(prev_ccline, ccline);

        ccline.cmdbuff = null;
        ccline.cmdprompt = null;
        ccline.xpc = null;
    }

    /*
     * Restore ccline after it has been saved with save_cmdline().
     */
    /*private*/ static void restore_cmdline(cmdline_info_C cli)
    {
        COPY_cmdline_info(ccline, prev_ccline);
        COPY_cmdline_info(prev_ccline, cli);
    }

    /*
     * Save the command line into allocated memory.
     * Returns a pointer to be passed to restore_cmdline_alloc() later.
     */
    /*private*/ static cmdline_info_C save_cmdline_alloc()
    {
        cmdline_info_C cli = new cmdline_info_C();
        save_cmdline(cli);
        return cli;
    }

    /*
     * Restore the command line from the return value of save_cmdline_alloc().
     */
    /*private*/ static void restore_cmdline_alloc(cmdline_info_C cli)
    {
        if (cli != null)
            restore_cmdline(cli);
    }

    /*
     * Paste a yank register into the command line.
     * Used by CTRL-R command in command-line mode.
     * insert_reg() can't be used here, because special characters
     * from the register contents will be interpreted as commands.
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean cmdline_paste(int regname, boolean literally, boolean remcr)
        /* literally: Insert text literally instead of "as typed" */
        /* remcr: remove trailing CR */
    {
        /* check for valid regname; also accept special characters for CTRL-R in the command line */
        if (regname != Ctrl_F && regname != Ctrl_P && regname != Ctrl_W && regname != Ctrl_A
                && !valid_yank_reg(regname, false))
            return false;

        /* A register containing CTRL-R can cause an endless loop.
         * Allow using CTRL-C to break the loop. */
        line_breakcheck();
        if (got_int)
            return false;

        regname = may_get_selection(regname);

        /* Need to save and restore ccline.  And set "textlock" to avoid nasty
         * things like going to another buffer when evaluating an expression. */
        cmdline_info_C save_cli = save_cmdline_alloc();
        textlock++;

        Bytes[] arg = new Bytes[1];
        boolean[] allocated = new boolean[1];
        boolean got = get_spec_reg(regname, arg, allocated, true);

        --textlock;
        restore_cmdline_alloc(save_cli);

        if (got)
        {
            /* Got the value of a special register in "arg". */
            if (arg[0] == null)
                return false;

            /* When 'incsearch' is set and CTRL-R CTRL-W used: skip the duplicate part of the word. */
            Bytes p = arg[0];

            if (p_is[0] && regname == Ctrl_W)
            {
                /* Locate start of last word in the cmd buffer. */
                Bytes w;
                for (w = ccline.cmdbuff.plus(ccline.cmdpos); BLT(ccline.cmdbuff, w); )
                {
                    int len = us_head_off(ccline.cmdbuff, w.minus(1)) + 1;
                    if (!vim_iswordc(us_ptr2char(w.minus(len)), curbuf))
                        break;
                    w = w.minus(len);
                }
                int len = BDIFF(ccline.cmdbuff.plus(ccline.cmdpos), w);
                if (p_ic[0] ? STRNCASECMP(w, arg[0], len) == 0 : STRNCMP(w, arg[0], len) == 0)
                    p = p.plus(len);
            }

            cmdline_paste_str(p, literally);

            return true;
        }

        return cmdline_paste_reg(regname, literally, remcr);
    }

    /*
     * Put a string on the command line.
     * When "literally" is true, insert literally.
     * When "literally" is false, insert as typed, but don't leave the command line.
     */
    /*private*/ static void cmdline_paste_str(Bytes _s, boolean literally)
    {
        Bytes[] s = { _s };
        if (literally)
            put_on_cmdline(s[0], -1, true);
        else
            while (s[0].at(0) != NUL)
            {
                int cv = s[0].at(0);
                if (cv == Ctrl_V && s[0].at(1) != NUL)
                    s[0] = s[0].plus(1);
                int c = us_ptr2char_adv(s, false);
                if (cv == Ctrl_V || c == ESC || c == Ctrl_C
                        || c == CAR || c == NL || c == Ctrl_L
                        || c == intr_char
                        || (c == Ctrl_BSL && s[0].at(0) == Ctrl_N))
                    stuffcharReadbuff(Ctrl_V);
                stuffcharReadbuff(c);
            }
    }

    /*
     * this function is called when the screen size changes and with incremental search
     */
    /*private*/ static void redrawcmdline()
    {
        if (cmd_silent)
            return;

        need_wait_return = false;
        compute_cmdrow();
        redrawcmd();
        cursorcmd();
    }

    /*private*/ static void redrawcmdprompt()
    {
        if (cmd_silent)
            return;

        if (ccline.cmdfirstc != NUL)
            msg_putchar(ccline.cmdfirstc);
        if (ccline.cmdprompt != null)
        {
            msg_puts_attr(ccline.cmdprompt, ccline.cmdattr);
            ccline.cmdindent = msg_col + (msg_row - cmdline_row) * (int)Columns[0];
            /* do the reverse of set_cmdspos() */
            if (ccline.cmdfirstc != NUL)
                --ccline.cmdindent;
        }
        else
            for (int i = ccline.cmdindent; 0 < i; --i)
                msg_putchar(' ');
    }

    /*
     * Redraw what is currently on the command line.
     */
    /*private*/ static void redrawcmd()
    {
        if (cmd_silent)
            return;

        /* when 'incsearch' is set there may be no command line while redrawing */
        if (ccline.cmdbuff == null)
        {
            windgoto(cmdline_row, 0);
            msg_clr_eos();
            return;
        }

        msg_start();
        redrawcmdprompt();

        /* Don't use more prompt, truncate the cmdline if it doesn't fit. */
        msg_no_more = true;
        draw_cmdline(0, ccline.cmdlen);
        msg_clr_eos();
        msg_no_more = false;

        set_cmdspos_cursor();

        /*
         * An emsg() before may have set msg_scroll.  This is used in normal mode,
         * in cmdline mode we can reset them now.
         */
        msg_scroll = false;         /* next message overwrites cmdline */

        /* Typing ':' at the more prompt may set skip_redraw.  We don't want this in cmdline mode. */
        skip_redraw = false;
    }

    /*private*/ static void compute_cmdrow()
    {
        if (exmode_active != 0 || msg_scrolled != 0)
            cmdline_row = (int)Rows[0] - 1;
        else
            cmdline_row = lastwin.w_winrow + lastwin.w_height + lastwin.w_status_height;
    }

    /*private*/ static void cursorcmd()
    {
        if (cmd_silent)
            return;

        if (cmdmsg_rl)
        {
            msg_row = cmdline_row  + (ccline.cmdspos / ((int)Columns[0] - 1));
            msg_col = (int)Columns[0] - (ccline.cmdspos % ((int)Columns[0] - 1)) - 1;
            if (msg_row <= 0)
                msg_row = (int)Rows[0] - 1;
        }
        else
        {
            msg_row = cmdline_row + (ccline.cmdspos / (int)Columns[0]);
            msg_col = ccline.cmdspos % (int)Columns[0];
            if (Rows[0] <= msg_row)
                msg_row = (int)Rows[0] - 1;
        }

        windgoto(msg_row, msg_col);
    }

    /*private*/ static void gotocmdline(boolean clr)
    {
        msg_start();
        if (cmdmsg_rl)
            msg_col = (int)Columns[0] - 1;
        else
            msg_col = 0;        /* always start in column 0 */
        if (clr)                /* clear the bottom line(s) */
            msg_clr_eos();      /* will reset clear_cmdline */
        windgoto(cmdline_row, 0);
    }

    /*
     * Check the word in front of the cursor for an abbreviation.
     * Called when the non-id character "c" has been entered.
     * When an abbreviation is recognized it is removed from the text with
     * backspaces and the replacement string is inserted, followed by "c".
     */
    /*private*/ static boolean ccheck_abbr(int c)
    {
        if (p_paste[0] || no_abbr)         /* no abbreviations or in paste mode */
            return false;

        return check_abbr(c, ccline.cmdbuff, ccline.cmdpos, 0);
    }

    /*
     * Return false if this is not an appropriate context in which to do
     * completion of anything, return true if it is (even if there are no matches).
     * For the caller, this means that the character is just passed through like a
     * normal character (instead of being expanded).  This allows :s/^I^D etc.
     */
    /*private*/ static boolean nextwild(expand_C xp, int type, int options, boolean escape)
        /* options: extra options for expandOne() */
        /* escape: if true, escape the returned matches */
    {
        if (xp.xp_numfiles == -1)
            set_expand_context(xp);

        if (xp.xp_context == EXPAND_UNSUCCESSFUL)
        {
            beep_flush();
            return true;        /* something illegal on command line */
        }
        if (xp.xp_context == EXPAND_NOTHING)
        {
            /* Caller can use the character as a normal char instead. */
            return false;
        }

        msg_puts(u8("..."));            /* show that we are busy */
        out_flush();

        int i = BDIFF(xp.xp_pattern, ccline.cmdbuff);
        xp.xp_pattern_len = ccline.cmdpos - i;

        Bytes p2;
        if (type == WILD_NEXT || type == WILD_PREV)
        {
            /*
             * Get next/previous match for a previous expanded pattern.
             */
            p2 = expandOne(xp, null, null, 0, type);
        }
        else
        {
            /*
             * Translate string into pattern and expand it.
             */
            Bytes p1 = addstar(xp.xp_pattern, xp.xp_pattern_len, xp.xp_context);
            if (p1 == null)
                p2 = null;
            else
            {
                int use_options = options | WILD_ADD_SLASH|WILD_SILENT;
                if (escape)
                    use_options |= WILD_ESCAPE;

                if (p_wic[0])
                    use_options += WILD_ICASE;
                p2 = expandOne(xp, p1, STRNDUP(ccline.cmdbuff.plus(i), xp.xp_pattern_len), use_options, type);
                /* longest match: make sure it is not shorter, happens with :help */
                if (p2 != null && type == WILD_LONGEST)
                {
                    int j;
                    for (j = 0; j < xp.xp_pattern_len; j++)
                         if (ccline.cmdbuff.at(i + j) == (byte)'*' || ccline.cmdbuff.at(i + j) == (byte)'?')
                             break;
                    if (strlen(p2) < j)
                        p2 = null;
                }
            }
        }

        if (p2 != null && !got_int)
        {
            int difflen = strlen(p2) - xp.xp_pattern_len;
            if (ccline.cmdbufflen < ccline.cmdlen + difflen + 4)
            {
                realloc_cmdbuff(ccline.cmdlen + difflen + 4);
                xp.xp_pattern = ccline.cmdbuff.plus(i);
            }

            BCOPY(ccline.cmdbuff, ccline.cmdpos + difflen, ccline.cmdbuff, ccline.cmdpos, ccline.cmdlen - ccline.cmdpos + 1);
            BCOPY(ccline.cmdbuff, i, p2, 0, strlen(p2));
            ccline.cmdlen += difflen;
            ccline.cmdpos += difflen;
        }

        redrawcmd();
        cursorcmd();

        /* When expanding a ":map" command and no matches are found,
         * assume that the key is supposed to be inserted literally. */
        if (xp.xp_context == EXPAND_MAPPINGS && p2 == null)
            return false;

        if (xp.xp_numfiles <= 0 && p2 == null)
            beep_flush();
        else if (xp.xp_numfiles == 1)
            /* free expanded pattern */
            expandOne(xp, null, null, 0, WILD_FREE);

        return true;
    }

    /*private*/ static int findex;
    /*private*/ static Bytes orig_save;    /* kept value of orig */

    /*
     * Do wildcard expansion on the string 'str'.
     * Chars that should not be expanded must be preceded with a backslash.
     * Return a pointer to allocated memory containing the new string.
     * Return null for failure.
     *
     * "orig" is the originally expanded string, copied to allocated memory.
     * It should either be kept in "orig_save" or freed.  When "mode" is WILD_NEXT
     * or WILD_PREV "orig" should be null.
     *
     * Results are cached in xp.xp_files and xp.xp_numfiles, except when "mode"
     * is WILD_EXPAND_FREE or WILD_ALL.
     *
     * mode = WILD_FREE:        just free previously expanded matches
     * mode = WILD_EXPAND_FREE: normal expansion, do not keep matches
     * mode = WILD_EXPAND_KEEP: normal expansion, keep matches
     * mode = WILD_NEXT:        use next match in multiple match, wrap to first
     * mode = WILD_PREV:        use previous match in multiple match, wrap to first
     * mode = WILD_ALL:         return all matches concatenated
     * mode = WILD_LONGEST:     return longest matched part
     * mode = WILD_ALL_KEEP:    get all matches, keep matches
     *
     * options = WILD_LIST_NOTFOUND:    list entries without a match
     * options = WILD_USE_NL:           Use '\n' for WILD_ALL
     * options = WILD_NO_BEEP:          Don't beep for multiple matches
     * options = WILD_ADD_SLASH:        add a slash after directory names
     * options = WILD_SILENT:           don't print warning messages
     * options = WILD_ESCAPE:           put backslash before special chars
     * options = WILD_ICASE:            ignore case for files
     *
     * The variables xp.xp_context and xp.xp_backslash must have been set!
     */
    /*private*/ static Bytes expandOne(expand_C xp, Bytes str, Bytes orig, int options, int mode)
        /* orig: allocated copy of original of expanded string */
    {
        Bytes ss = null;

        /*
         * first handle the case of using an old match
         */
        if (mode == WILD_NEXT || mode == WILD_PREV)
        {
            if (0 < xp.xp_numfiles)
            {
                if (mode == WILD_PREV)
                {
                    if (findex == -1)
                        findex = xp.xp_numfiles;
                    --findex;
                }
                else    /* mode == WILD_NEXT */
                    findex++;

                /*
                 * When wrapping around, return the original string, set findex to -1.
                 */
                if (findex < 0)
                {
                    if (orig_save == null)
                        findex = xp.xp_numfiles - 1;
                    else
                        findex = -1;
                }
                if (xp.xp_numfiles <= findex)
                {
                    if (orig_save == null)
                        findex = 0;
                    else
                        findex = -1;
                }
                if (findex == -1)
                    return STRDUP(orig_save);

                return STRDUP(xp.xp_files[findex]);
            }
            else
                return null;
        }

        /* free old names */
        if (xp.xp_numfiles != -1 && mode != WILD_ALL && mode != WILD_LONGEST)
        {
            xp.xp_files = null;
            xp.xp_numfiles = -1;
            orig_save = null;
        }
        findex = 0;

        if (mode == WILD_FREE)      /* only release file name */
            return null;

        if (xp.xp_numfiles == -1)
        {
            orig_save = orig;

            /*
             * Do the expansion.
             */
            boolean b;
            {
                int[] _1 = { xp.xp_numfiles };
                Bytes[][] _2 = { xp.xp_files };
                b = expandFromContext(xp, str, _1, _2, options);
                xp.xp_numfiles = _1[0];
                xp.xp_files = _2[0];
            }
            if (b == false)
            {
            }
            else if (xp.xp_numfiles == 0)
            {
                if ((options & WILD_SILENT) == 0)
                    emsg2(e_nomatch2, str);
            }
            else
            {
                /* Escape the matches for use on the command line. */
                expandEscape(xp, str, xp.xp_numfiles, xp.xp_files, options);

                /*
                 * Check for matching suffixes in file names.
                 */
                if (mode != WILD_ALL && mode != WILD_ALL_KEEP && mode != WILD_LONGEST)
                {
                    int non_suf_match = 1;          /* number without matching suffix */
                    if (xp.xp_numfiles != 0)
                        non_suf_match = xp.xp_numfiles;
                    if (non_suf_match != 1)
                    {
                        /* Can we ever get here unless it's while expanding
                         * interactively?  If not, we can get rid of this all
                         * together.  Don't really want to wait for this message
                         * (and possibly have to hit return to continue!).
                         */
                        if ((options & WILD_SILENT) == 0)
                            emsg(e_toomany);
                        else if ((options & WILD_NO_BEEP) == 0)
                            beep_flush();
                    }
                    if (!(non_suf_match != 1 && mode == WILD_EXPAND_FREE))
                        ss = STRDUP(xp.xp_files[0]);
                }
            }
        }

        /* Find longest common part. */
        if (mode == WILD_LONGEST && 0 < xp.xp_numfiles)
        {
            int len;
            for (len = 0; xp.xp_files[0].at(len) != NUL; len++)
            {
                int i;
                for (i = 0; i < xp.xp_numfiles; i++)
                {
                    if (xp.xp_files[i].at(len) != xp.xp_files[0].at(len))
                        break;
                }
                if (i < xp.xp_numfiles)
                {
                    if ((options & WILD_NO_BEEP) == 0)
                        vim_beep();
                    break;
                }
            }
            ss = STRNDUP(xp.xp_files[0], len);
            findex = -1;                        /* next "p_wc" gets first one */
        }

        /* Concatenate all matching names. */
        if (mode == WILD_ALL && 0 < xp.xp_numfiles)
        {
            int len = 0;
            for (int i = 0; i < xp.xp_numfiles; i++)
                len += strlen(xp.xp_files[i]) + 1;

            ss = new Bytes(len);
            ss.be(0, NUL);

            for (int i = 0; i < xp.xp_numfiles; i++)
            {
                STRCAT(ss, xp.xp_files[i]);
                if (i != xp.xp_numfiles - 1)
                    STRCAT(ss, (options & WILD_USE_NL) != 0 ? u8("\n") : u8(" "));
            }
        }

        if (mode == WILD_EXPAND_FREE || mode == WILD_ALL)
            expandCleanup(xp);

        return ss;
    }

    /*
     * Prepare an expand structure for use.
     */
    /*private*/ static void expandInit(expand_C xp)
    {
        xp.xp_pattern = null;
        xp.xp_pattern_len = 0;
        xp.xp_backslash = XP_BS_NONE;
        xp.xp_numfiles = -1;
        xp.xp_files = null;
        xp.xp_arg = null;
        xp.xp_line = null;
    }

    /*
     * Cleanup an expand structure after use.
     */
    /*private*/ static void expandCleanup(expand_C xp)
    {
        if (0 <= xp.xp_numfiles)
        {
            xp.xp_files = null;
            xp.xp_numfiles = -1;
        }
    }

    /*private*/ static void expandEscape(expand_C xp, Bytes str, int numfiles, Bytes[] files, int options)
    {
        if ((options & WILD_ESCAPE) != 0)
        {
            if (xp.xp_context == EXPAND_BUFFERS)
            {
                /*
                 * Insert a backslash into a file name before a space, \, %, #
                 * and wildmatch characters, except '~'.
                 */
                for (int i = 0; i < numfiles; i++)
                {
                    Bytes p = vim_strsave_fnameescape(files[i]);
                    if (p != null)
                        files[i] = p;

                    /* If 'str' starts with "\~", replace "~" at start of files[i] with "\~". */
                    if (str.at(0) == (byte)'\\' && str.at(1) == (byte)'~' && files[i].at(0) == (byte)'~')
                        files[i] = escape_fname(files[i]);
                }
                xp.xp_backslash = XP_BS_NONE;

                /* If the first file starts with a '+' escape it.
                 * Otherwise it could be seen as "+cmd". */
                if (files[0].at(0) == (byte)'+')
                    files[0] = escape_fname(files[0]);
            }
        }
    }

    /*
     * Escape special characters in "fname" for when used as a file name argument
     * after a Vim command, or, when "shell" is non-zero, a shell command.
     * Returns the result in allocated memory.
     */
    /*private*/ static Bytes vim_strsave_fnameescape(Bytes fname)
    {
        Bytes p = vim_strsave_escaped(fname, u8(" \t\n*?[{`$\\%#'\"|!<"));

        /* '>' and '+' are special at the start of some commands, e.g. ":edit" and ":write".
         * "cd -" has a special meaning. */
        if (p.at(0) == (byte)'>' || p.at(0) == (byte)'+' || (p.at(0) == (byte)'-' && p.at(1) == NUL))
            p = escape_fname(p);

        return p;
    }

    /*
     * Put a backslash before the file name.
     */
    /*private*/ static Bytes escape_fname(Bytes fname)
    {
        Bytes p = new Bytes(strlen(fname) + 2);

        p.be(0, (byte)'\\');
        STRCPY(p.plus(1), fname);

        return p;
    }

    /*
     * Show all matches for completion on the command line.
     * Returns EXPAND_NOTHING when the character that triggered expansion
     * should be inserted like a normal character.
     */
    /*private*/ static int showmatches(expand_C xp)
    {
        int[] num_files = new int[1];
        Bytes[][] files_found = new Bytes[1][];

        if (xp.xp_numfiles == -1)
        {
            set_expand_context(xp);
            int i = expand_cmdline(xp, ccline.cmdbuff, ccline.cmdpos, num_files, files_found);
            if (i != EXPAND_OK)
                return i;
        }
        else
        {
            num_files[0] = xp.xp_numfiles;
            files_found[0] = xp.xp_files;
        }

        msg_didany = false;             /* lines_left will be set */
        msg_start();                    /* prepare for paging */
        msg_putchar('\n');
        out_flush();
        cmdline_row = msg_row;
        msg_didany = false;             /* lines_left will be set again */
        msg_start();                    /* prepare for paging */

        if (got_int)
            got_int = false;        /* only int. the completion, not the cmd line */
        else
        {
            /* find the length of the longest file name */
            int maxlen = 0;
            for (int i = 0; i < num_files[0]; i++)
            {
                int len = mb_string2cells(files_found[0][i], -1);
                if (maxlen < len)
                    maxlen = len;
            }
            maxlen += 2;                                        /* two spaces between file names */

            /* compute the number of columns and lines for the listing */
            int columns = ((int)Columns[0] + 2) / maxlen;
            if (columns < 1)
                columns = 1;
            int lines = (num_files[0] + columns - 1) / columns;

            int attr = hl_attr(HLF_D);                          /* find out highlighting for directories */

            /* list the files line by line */
            for (int i = 0; i < lines; i++)
            {
                int lastlen = 999;
                for (int j = i; j < num_files[0]; j += lines)
                {
                    for (int k = maxlen - lastlen; 0 <= --k; )
                        msg_putchar(' ');
                    boolean isdir = false;
                    if (xp.xp_context == EXPAND_BUFFERS)
                    {
                        /* highlight directories */
                        if (xp.xp_numfiles != -1)
                        {
                            /* Expansion was done before and special characters were escaped,
                             * need to halve backslashes.
                             */
                            Bytes halved_slash = backslash_halve_save(files_found[0][j]);
                            isdir = mch_isdir((halved_slash != null) ? halved_slash : files_found[0][j]);
                        }
                        else
                            /* Expansion was done here, file names are literal. */
                            isdir = mch_isdir(files_found[0][j]);
                    }
                    lastlen = msg_outtrans_attr(files_found[0][j], isdir ? attr : 0);
                }
                if (0 < msg_col)                                /* when not wrapped around */
                {
                    msg_clr_eos();
                    msg_putchar('\n');
                }
                out_flush();                                    /* show one line at a time */
                if (got_int)
                {
                    got_int = false;
                    break;
                }
            }

            /*
             * We redraw the command below the lines that we have just listed.
             * This is a bit tricky, but it saves a lot of screen updating.
             */
            cmdline_row = msg_row;                              /* will put it back later */
        }

        return EXPAND_OK;
    }

    /*
     * Prepare a string for expansion.
     * When expanding file names: The string will be used with expand_wildcards().
     * Copy "fname[len]" into allocated memory and add a '*' at the end.
     * When expanding other names: The string will be used with regcomp().
     * Copy the name into allocated memory and prepend "^".
     */
    /*private*/ static Bytes addstar(Bytes fname, int len, int context)
    {
        int new_len = len + 2;                  /* +2 for '^' at start, NUL at end */
        for (int i = 0; i < len; i++)
        {
            if (fname.at(i) == (byte)'*' || fname.at(i) == (byte)'~')
                new_len++;                      /* '*' needs to be replaced by ".*"
                                                 * '~' needs to be replaced by "\~" */

            /* Buffer names are like file names.  "." should be literal. */
            if (context == EXPAND_BUFFERS && fname.at(i) == (byte)'.')
                new_len++;                      /* "." becomes "\." */

            /* Custom expansion takes care of special things, match
             * backslashes literally (perhaps also for other types?) */
            if ((context == EXPAND_USER_DEFINED || context == EXPAND_USER_LIST) && fname.at(i) == (byte)'\\')
                new_len++;                      /* '\' becomes "\\" */
        }

        Bytes retval = new Bytes(new_len);

        retval.be(0, (byte)'^');
        int j = 1;
        for (int i = 0; i < len; i++, j++)
        {
            /* Skip backslash.  But why?  At least keep it for custom expansion. */
            if (context != EXPAND_USER_DEFINED
                    && context != EXPAND_USER_LIST
                    && fname.at(i) == (byte)'\\'
                    && ++i == len)
                break;

            switch (fname.at(i))
            {
                case '*':   retval.be(j++, (byte)'.');
                            break;
                case '~':   retval.be(j++, (byte)'\\');
                            break;
                case '?':   retval.be(j, (byte)'.');
                            continue;
                case '.':   if (context == EXPAND_BUFFERS)
                                retval.be(j++, (byte)'\\');
                            break;
                case '\\':  if (context == EXPAND_USER_DEFINED || context == EXPAND_USER_LIST)
                                retval.be(j++, (byte)'\\');
                            break;
            }
            retval.be(j, fname.at(i));
        }
        retval.be(j, NUL);

        return retval;
    }

    /*
     * Must parse the command line so far to work out what context we are in.
     * Completion can then be done based on that context.
     * This routine sets the variables:
     *  xp.xp_pattern          The start of the pattern to be expanded within
     *                          the command line (ends at the cursor).
     *  xp.xp_context          The type of thing to expand.  Will be one of:
     *
     *  EXPAND_UNSUCCESSFUL     Used sometimes when there is something illegal
     *                          on the command line, like an unknown command.
     *                          Caller should beep.
     *  EXPAND_NOTHING          Unrecognised context for completion, use char
     *                          like a normal char, rather than for completion.
     *                          e.g. :s/^I/
     *  EXPAND_COMMANDS         Cursor is still touching the command, so complete it.
     *  EXPAND_BUFFERS          Complete file names for :buf and :sbuf commands.
     *  EXPAND_SETTINGS         Complete variable names.  e.g. :set d^I
     *  EXPAND_BOOL_SETTINGS    Complete boolean variables only,  e.g. :set no^I
     *  EXPAND_EVENTS           Complete event names
     *  EXPAND_SYNTAX           Complete :syntax command arguments
     *  EXPAND_HIGHLIGHT        Complete highlight (syntax) group names
     *  EXPAND_AUGROUP          Complete autocommand group names
     *  EXPAND_USER_VARS        Complete user defined variable names, e.g. :unlet a^I
     *  EXPAND_MAPPINGS         Complete mapping and abbreviation names,
     *                          e.g. :unmap a^I , :cunab x^I
     *  EXPAND_FUNCTIONS        Complete internal or user defined function names,
     *                          e.g. :call sub^I
     *  EXPAND_USER_FUNC        Complete user defined function names, e.g. :delf F^I
     *  EXPAND_EXPRESSION       Complete internal or user defined function/variable
     *                          names in expressions, e.g. :while s^I
     */
    /*private*/ static void set_expand_context(expand_C xp)
    {
        /* only expansion for ':', '>' and '=' command-lines */
        if (ccline.cmdfirstc != ':' && ccline.cmdfirstc != '>' && ccline.cmdfirstc != '=' && !ccline.input_fn)
        {
            xp.xp_context = EXPAND_NOTHING;
            return;
        }
        set_cmd_context(xp, ccline.cmdbuff, ccline.cmdlen, ccline.cmdpos);
    }

    /*private*/ static void set_cmd_context(expand_C xp, Bytes str, int len, int col)
        /* str: start of command line */
        /* len: length of command line (excl. NUL) */
        /* col: position of cursor */
    {
        int old_char = NUL;

        /*
         * Avoid a UMR warning from Purify, only save the character if it has been written before.
         */
        if (col < len)
            old_char = str.at(col);
        str.be(col, NUL);
        Bytes nextcomm = str;

        if (ccline.cmdfirstc == '=')
        {
            /* pass CMD_SIZE because there is no real command */
            set_context_for_expression(xp, str, CMD_SIZE);
        }
        else if (ccline.input_fn)
        {
            xp.xp_context = ccline.xp_context;
            xp.xp_pattern = ccline.cmdbuff;
            xp.xp_arg = ccline.xp_arg;
        }
        else
            while (nextcomm != null)
                nextcomm = set_one_cmd_context(xp, nextcomm);

        /* Store the string here so that call_user_expand_func() can get to them easily. */
        xp.xp_line = str;
        xp.xp_col = col;

        str.be(col, old_char);
    }

    /*
     * Expand the command line "str" from context "xp".
     * "xp" must have been set by set_cmd_context().
     * xp.xp_pattern points into "str", to where the text that is to be expanded starts.
     * Returns EXPAND_UNSUCCESSFUL when there is something illegal before the cursor.
     * Returns EXPAND_NOTHING when there is nothing to expand, might insert the
     * key that triggered expansion literally.
     * Returns EXPAND_OK otherwise.
     */
    /*private*/ static int expand_cmdline(expand_C xp, Bytes str, int col, int[] matchcount, Bytes[][] matches)
        /* str: start of command line */
        /* col: position of cursor */
        /* matchcount: return: nr of matches */
        /* matches: return: array of pointers to matches */
    {
        Bytes file_str = null;
        int options = WILD_ADD_SLASH|WILD_SILENT;

        if (xp.xp_context == EXPAND_UNSUCCESSFUL)
        {
            beep_flush();
            return EXPAND_UNSUCCESSFUL;     /* Something illegal on command line */
        }
        if (xp.xp_context == EXPAND_NOTHING)
        {
            /* Caller can use the character as a normal char instead. */
            return EXPAND_NOTHING;
        }

        /* add star to file name, or convert to regexp if not exp. files. */
        xp.xp_pattern_len = BDIFF(str.plus(col), xp.xp_pattern);
        file_str = addstar(xp.xp_pattern, xp.xp_pattern_len, xp.xp_context);
        if (file_str == null)
            return EXPAND_UNSUCCESSFUL;

        if (p_wic[0])
            options += WILD_ICASE;

        /* find all files that match the description */
        if (expandFromContext(xp, file_str, matchcount, matches, options) == false)
        {
            matchcount[0] = 0;
            matches[0] = null;
        }

        return EXPAND_OK;
    }

    /*private*/ static abstract class expfun_C
    {
        public abstract Bytes expand(expand_C xp, int idx);
    }

    /*private*/ static final class expgen_C
    {
        int         context;
        expfun_C    func;
        boolean     ic;
        boolean     escaped;

        /*private*/ expgen_C(int context, expfun_C func, boolean ic, boolean escaped)
        {
            this.context = context;
            this.func = func;
            this.ic = ic;
            this.escaped = escaped;
        }
    }

    /*
     * Do the expansion based on xp.xp_context and "pat".
     */
    /*private*/ static boolean expandFromContext(expand_C xp, Bytes pat, int[] num_file, Bytes[][] file, int options)
        /* options: EW_ flags */
    {
        boolean ret;

        int flags = EW_DIR;                     /* include directories */
        if ((options & WILD_LIST_NOTFOUND) != 0)
            flags |= EW_NOTFOUND;
        if ((options & WILD_ADD_SLASH) != 0)
            flags |= EW_ADDSLASH;
        if ((options & WILD_SILENT) != 0)
            flags |= EW_SILENT;
        if ((options & WILD_ALLLINKS) != 0)
            flags |= EW_ALLLINKS;

        file[0] = new Bytes[] { u8("") };
        num_file[0] = 0;

        if (xp.xp_context == EXPAND_OLD_SETTING)
            return expandOldSetting(num_file, file);
        if (xp.xp_context == EXPAND_BUFFERS)
            return expandBufnames(pat, num_file, file, options);
        if (xp.xp_context == EXPAND_USER_LIST)
            return expandUserList(xp, num_file, file);

        regmatch_C regmatch = new regmatch_C();
        regmatch.regprog = vim_regcomp(pat, p_magic[0] ? RE_MAGIC : 0);
        if (regmatch.regprog == null)
            return false;

        /* set ignore-case according to "p_ic", "p_scs" and "pat" */
        regmatch.rm_ic = ignorecase(pat);

        if (xp.xp_context == EXPAND_SETTINGS || xp.xp_context == EXPAND_BOOL_SETTINGS)
            ret = expandSettings(xp, regmatch, num_file, file);
        else if (xp.xp_context == EXPAND_MAPPINGS)
            ret = expandMappings(regmatch, num_file, file);
        else if (xp.xp_context == EXPAND_USER_DEFINED)
            ret = expandUserDefined(xp, regmatch, num_file, file);
        else
        {
            /*
             * Find a context in the table and call the expandGeneric()
             * with the right function to do the expansion.
             */
            ret = false;
            for (int i = 0; i < expgen_tab.length; i++)
                if (expgen_tab[i].context == xp.xp_context)
                {
                    if (expgen_tab[i].ic)
                        regmatch.rm_ic = true;
                    ret = expandGeneric(xp, regmatch, num_file, file, expgen_tab[i].func, expgen_tab[i].escaped);
                    break;
                }
        }

        return ret;
    }

    /*
     * Expand a list of names.
     *
     * Generic function for command line completion.  It calls a function to
     * obtain strings, one by one.  The strings are matched against a regexp
     * program.  Matching strings are copied into an array, which is returned.
     *
     * Returns true.
     */
    /*private*/ static boolean expandGeneric(expand_C xp, regmatch_C regmatch, int[] num_file, Bytes[][] file, expfun_C func, boolean escaped)
        /* func: returns a string from the list */
    {
        int count = 0;

        /* do this loop twice:
         * round == 0: count the number of matching names
         * round == 1: copy the matching names into allocated memory
         */
        for (int round = 0; round <= 1; round++)
        {
            for (int i = 0; ; i++)
            {
                Bytes str = func.expand(xp, i);
                if (str == null)        /* end of list */
                    break;
                if (str.at(0) == NUL)        /* skip empty strings */
                    continue;

                if (vim_regexec(regmatch, str, 0))
                {
                    if (round != 0)
                    {
                        if (escaped)
                            str = vim_strsave_escaped(str, u8(" \t\\."));
                        else
                            str = STRDUP(str);
                        file[0][count] = str;
                    }
                    count++;
                }
            }
            if (round == 0)
            {
                if (count == 0)
                    return true;
                num_file[0] = count;
                file[0] = new Bytes[count];
                count = 0;
            }
        }

        /* Sort the results. */
        if (xp.xp_context == EXPAND_EXPRESSION
         || xp.xp_context == EXPAND_FUNCTIONS
         || xp.xp_context == EXPAND_USER_FUNC)
            /* <SNR> functions should be sorted to the end. */
            Arrays.sort(file[0], 0, num_file[0], sort_func_compare);
        else
            sort_strings(file[0], num_file[0]);

        /* Reset the variables used for special highlight names expansion, so that
         * they don't show up when getting normal highlight names by ID. */
        reset_expand_highlight();

        return true;
    }

    /*private*/ static final Comparator<Bytes> sort_func_compare = new Comparator<Bytes>()
    {
        public int compare(Bytes s1, Bytes s2)
        {
            if (s1.at(0) != (byte)'<' && s2.at(0) == (byte)'<')
                return -1;
            if (s1.at(0) == (byte)'<' && s2.at(0) != (byte)'<')
                return 1;
            return STRCMP(s1, s2);
        }
    };

    /*
     * Call "user_expand_func()" to invoke a user defined VimL function and
     * return the result (either a string or a List).
     */
    /*private*/ static /*Bytes|list_C*/Object call_user_expand_func(boolean retlist, expand_C xp, int[] num_file, Bytes[][] file)
    {
        int save_current_SID = current_SID;

        if (xp.xp_arg == null || xp.xp_arg.at(0) == NUL || xp.xp_line == null)
            return null;

        num_file[0] = 0;
        file[0] = null;

        byte keep = NUL;
        if (ccline.cmdbuff != null)
        {
            keep = ccline.cmdbuff.at(ccline.cmdlen);
            ccline.cmdbuff.be(ccline.cmdlen, NUL);
        }

        Bytes[] args = new Bytes[3];
        args[0] = STRNDUP(xp.xp_pattern, xp.xp_pattern_len);
        args[1] = xp.xp_line;
        Bytes num = new Bytes(50);
        libC.sprintf(num, u8("%d"), xp.xp_col);
        args[2] = num;

        /* Save the cmdline, we don't know what the function may do. */
        cmdline_info_C save_cli = new cmdline_info_C();
        COPY_cmdline_info(save_cli, ccline);
        ccline.cmdbuff = null;
        ccline.cmdprompt = null;
        current_SID = xp.xp_scriptID;

        /*Bytes|list_C*/Object ret = retlist ? call_func_retlist(xp.xp_arg, 3, args, false) : call_func_retstr(xp.xp_arg, 3, args, false);

        COPY_cmdline_info(ccline, save_cli);
        current_SID = save_current_SID;
        if (ccline.cmdbuff != null)
            ccline.cmdbuff.be(ccline.cmdlen, keep);

        return ret;
    }

    /*
     * Expand names with a function defined by the user.
     */
    /*private*/ static boolean expandUserDefined(expand_C xp, regmatch_C regmatch, int[] num_file, Bytes[][] file)
    {
        Bytes retstr = (Bytes)call_user_expand_func(false, xp, num_file, file);
        if (retstr == null)
            return false;

        Growing<Bytes> ga = new Growing<Bytes>(Bytes.class, 3);

        Bytes e;
        for (Bytes s = retstr; s.at(0) != NUL; s = e)
        {
            e = vim_strchr(s, '\n');
            if (e == null)
                e = s.plus(strlen(s));
            byte keep = e.at(0);
            e.be(0, NUL);

            if (xp.xp_pattern.at(0) != NUL && !vim_regexec(regmatch, s, 0))
            {
                e.be(0, keep);
                if (e.at(0) != NUL)
                    e = e.plus(1);
                continue;
            }

            ga.ga_grow(1);
            ga.ga_data[ga.ga_len++] = STRNDUP(s, BDIFF(e, s));

            e.be(0, keep);
            if (e.at(0) != NUL)
                e = e.plus(1);
        }

        file[0] = ga.ga_data;
        num_file[0] = ga.ga_len;

        return true;
    }

    /*
     * Expand names with a list returned by a function defined by the user.
     */
    /*private*/ static boolean expandUserList(expand_C xp, int[] num_file, Bytes[][] file)
    {
        list_C retlist = (list_C)call_user_expand_func(true, xp, num_file, file);
        if (retlist == null)
            return false;

        Growing<Bytes> ga = new Growing<Bytes>(Bytes.class, 3);

        /* Loop over the items in the list. */
        for (listitem_C li = retlist.lv_first; li != null; li = li.li_next)
        {
            if (li.li_tv.tv_type != VAR_STRING || li.li_tv.tv_string == null)
                continue; /* skip non-string items and empty strings */

            ga.ga_grow(1);
            ga.ga_data[ga.ga_len++] = STRDUP(li.li_tv.tv_string);
        }
        list_unref(retlist);

        file[0] = ga.ga_data;
        num_file[0] = ga.ga_len;

        return true;
    }

    /*********************************
     *  Command line history stuff   *
     *********************************/

    /*
     * Translate a history character to the associated type number.
     */
    /*private*/ static int hist_char2type(int c)
    {
        if (c == ':')
            return HIST_CMD;
        if (c == '=')
            return HIST_EXPR;
        if (c == '@')
            return HIST_INPUT;
        if (c == '>')
            return HIST_DEBUG;

        return HIST_SEARCH;     /* must be '?' or '/' */
    }

    /*
     * Table of history names.
     * These names are used in :history and various hist...() functions.
     * It is sufficient to give the significant prefix of a history name.
     */

    /*private*/ static Bytes[] history_names =
    {
        u8("cmd"), u8("search"), u8("expr"), u8("input"), u8("debug"), null
    };

    /*private*/ static Bytes ha__compl = new Bytes(2);

    /*
     * Function given to expandGeneric() to obtain the possible
     * first arguments of the ":history command.
     */
    /*private*/ static final expfun_C get_history_arg = new expfun_C()
    {
        public Bytes expand(expand_C _xp, int idx)
        {
            Bytes short_names = u8(":=@>?/");
            int slen = strlen(short_names);
            int hlen = history_names.length - 1;

            if (idx < slen)
            {
                ha__compl.be(0, short_names.at(idx));
                return ha__compl;
            }
            if (idx < slen + hlen)
                return history_names[idx - slen];
            if (idx == slen + hlen)
                return u8("all");

            return null;
        }
    };

    /*
     * init_history() - Initialize the command line history.
     * Also used to re-allocate the history when the size changes.
     */
    /*private*/ static void init_history()
    {
        /*
         * If size of history table changed, reallocate it.
         */
        int newlen = (int)p_hi[0];
        if (newlen != hislen)                                   /* history length changed */
        {
            for (int type = 0; type < HIST_COUNT; type++)       /* adjust the tables */
            {
                histentry_C[] temp = null;

                if (newlen != 0)
                    temp = ARRAY_histentry(newlen);

                if (newlen == 0 || temp != null)
                {
                    if (hisidx[type] < 0)                       /* there are no entries yet */
                    {
                        for (int i = 0; i < newlen; i++)
                            clear_hist_entry(temp[i]);
                    }
                    else if (hislen < newlen)                   /* array becomes bigger */
                    {
                        int i;
                        for (i = 0; i <= hisidx[type]; i++)
                            COPY_histentry(temp[i], history[type][i]);
                        int j = i;
                        for ( ; i <= newlen - (hislen - hisidx[type]); i++)
                            clear_hist_entry(temp[i]);
                        for ( ; j < hislen; i++, j++)
                            COPY_histentry(temp[i], history[type][j]);
                    }
                    else                                        /* array becomes smaller or 0 */
                    {
                        int j = hisidx[type];
                        for (int i = newlen - 1; ; --i)
                        {
                            if (0 <= i)                         /* copy newest entries */
                                COPY_histentry(temp[i], history[type][j]);
                            else                                /* remove older entries */
                                history[type][j].hisstr = null;
                            if (--j < 0)
                                j = hislen - 1;
                            if (j == hisidx[type])
                                break;
                        }
                        hisidx[type] = newlen - 1;
                    }
                    history[type] = temp;
                }
            }
            hislen = newlen;
        }
    }

    /*private*/ static void clear_hist_entry(histentry_C hisptr)
    {
        hisptr.hisnum = 0;
        hisptr.hisstr = null;
    }

    /*
     * Check if command line 'str' is already in history.
     * If 'move_to_front' is true, matching entry is moved to end of history.
     */
    /*private*/ static boolean in_history(int type, Bytes str, boolean move_to_front, int sep)
        /* move_to_front: Move the entry to the front if it exists */
    {
        if (hisidx[type] < 0)
            return false;

        int last_i = -1;

        int i = hisidx[type];
        do
        {
            if (history[type][i].hisstr == null)
                return false;

            /* For search history, check that the separator character matches as well. */
            Bytes p = history[type][i].hisstr;
            if (STRCMP(str, p) == 0 && (type != HIST_SEARCH || sep == p.at(strlen(p) + 1)))
            {
                if (!move_to_front)
                    return true;
                last_i = i;
                break;
            }
            if (--i < 0)
                i = hislen - 1;
        } while (i != hisidx[type]);

        if (0 <= last_i)
        {
            str = history[type][i].hisstr;
            while (i != hisidx[type])
            {
                if (hislen <= ++i)
                    i = 0;
                COPY_histentry(history[type][last_i], history[type][i]);
                last_i = i;
            }
            history[type][i].hisnum = ++hisnum[type];
            history[type][i].hisstr = str;
            return true;
        }

        return false;
    }

    /*
     * Convert history name (from table above) to its HIST_ equivalent.
     * When "name" is empty, return "cmd" history.
     * Returns -1 for unknown history name.
     */
    /*private*/ static int get_histtype(Bytes name)
    {
        int len = strlen(name);

        /* No argument: use current history. */
        if (len == 0)
            return hist_char2type(ccline.cmdfirstc);

        for (int i = 0; history_names[i] != null; i++)
            if (STRNCASECMP(name, history_names[i], len) == 0)
                return i;

        if (vim_strbyte(u8(":=@>?/"), name.at(0)) != null && name.at(1) == NUL)
            return hist_char2type(name.at(0));

        return -1;
    }

    /*private*/ static int      last_maptick = -1;      /* last seen maptick */

    /*
     * Add the given string to the given history.  If the string is already in the
     * history then it is moved to the front.  "histype" may be one of he HIST_ values.
     */
    /*private*/ static void add_to_history(int histype, Bytes new_entry, boolean in_map, int sep)
        /* in_map: consider maptick when inside a mapping */
        /* sep: separator character used (search hist) */
    {
        if (hislen == 0)            /* no history */
            return;

        if (cmdmod.keeppatterns && histype == HIST_SEARCH)
            return;

        /*
         * Searches inside the same mapping overwrite each other, so that only
         * the last line is kept.  Be careful not to remove a line that was moved
         * down, only lines that were added.
         */
        if (histype == HIST_SEARCH && in_map)
        {
            if (maptick == last_maptick)
            {
                /* Current line is from the same mapping, remove it. */
                histentry_C hisptr = history[HIST_SEARCH][hisidx[HIST_SEARCH]];
                hisptr.hisstr = null;
                clear_hist_entry(hisptr);
                --hisnum[histype];
                if (--hisidx[HIST_SEARCH] < 0)
                    hisidx[HIST_SEARCH] = hislen - 1;
            }
            last_maptick = -1;
        }

        if (!in_history(histype, new_entry, true, sep))
        {
            if (++hisidx[histype] == hislen)
                hisidx[histype] = 0;

            histentry_C hisptr = history[histype][hisidx[histype]];

            /* Store the separator after the NUL of the string. */
            int len = strlen(new_entry);
            hisptr.hisstr = STRNDUP(new_entry, len + 2);
            hisptr.hisstr.be(len + 1, sep);

            hisptr.hisnum = ++hisnum[histype];
            if (histype == HIST_SEARCH && in_map)
                last_maptick = maptick;
        }
    }

    /*
     * Get identifier of newest history entry.
     * "histype" may be one of the HIST_ values.
     */
    /*private*/ static int get_history_idx(int histype)
    {
        if (hislen == 0 || histype < 0 || HIST_COUNT <= histype || hisidx[histype] < 0)
            return -1;

        return history[histype][hisidx[histype]].hisnum;
    }

    /*
     * Get pointer to the command line info to use.
     * cmdline_paste() may clear ccline and put the previous value in prev_ccline.
     */
    /*private*/ static cmdline_info_C get_ccline_ptr()
    {
        if ((State & CMDLINE) == 0)
            return null;
        if (ccline.cmdbuff != null)
            return ccline;
        if (prev_ccline != null && prev_ccline.cmdbuff != null)
            return prev_ccline;

        return null;
    }

    /*
     * Get the current command line in allocated memory.
     * Only works when the command line is being edited.
     * Returns null when something is wrong.
     */
    /*private*/ static Bytes get_cmdline_str()
    {
        cmdline_info_C p = get_ccline_ptr();
        if (p == null)
            return null;

        return STRNDUP(p.cmdbuff, p.cmdlen);
    }

    /*
     * Get the current command line position, counted in bytes.
     * Zero is the first position.
     * Only works when the command line is being edited.
     * Returns -1 when something is wrong.
     */
    /*private*/ static int get_cmdline_pos()
    {
        cmdline_info_C p = get_ccline_ptr();
        if (p == null)
            return -1;

        return p.cmdpos;
    }

    /*
     * Set the command line byte position to "pos".
     * Zero is the first position.
     * Only works when the command line is being edited.
     * Returns 1 when failed, 0 when OK.
     */
    /*private*/ static int set_cmdline_pos(int pos)
    {
        cmdline_info_C p = get_ccline_ptr();
        if (p == null)
            return 1;

        /* The position is not set directly but after CTRL-\ e or CTRL-R = has changed the command line. */
        if (pos < 0)
            new_cmdpos = 0;
        else
            new_cmdpos = pos;
        return 0;
    }

    /*
     * Get the current command-line type.
     * Returns ':' or '/' or '?' or '@' or '>' or '-'
     * Only works when the command line is being edited.
     * Returns NUL when something is wrong.
     */
    /*private*/ static int get_cmdline_type()
    {
        cmdline_info_C p = get_ccline_ptr();

        if (p == null)
            return NUL;
        if (p.cmdfirstc == NUL)
            return (p.input_fn) ? '@' : '-';

        return p.cmdfirstc;
    }

    /*
     * Calculate history index from a number:
     *   num > 0: seen as identifying number of a history entry
     *   num < 0: relative position in history wrt newest entry
     * "histype" may be one of the HIST_ values.
     */
    /*private*/ static int calc_hist_idx(int histype, int num)
    {
        int i;
        if (hislen == 0 || histype < 0 || HIST_COUNT <= histype || (i = hisidx[histype]) < 0 || num == 0)
            return -1;

        boolean wrapped = false;

        histentry_C[] hist = history[histype];
        if (0 < num)
        {
            while (num < hist[i].hisnum)
                if (--i < 0)
                {
                    if (wrapped)
                        break;
                    i += hislen;
                    wrapped = true;
                }
            if (hist[i].hisnum == num && hist[i].hisstr != null)
                return i;
        }
        else if (-num <= hislen)
        {
            i += num + 1;
            if (i < 0)
                i += hislen;
            if (hist[i].hisstr != null)
                return i;
        }

        return -1;
    }

    /*
     * Get a history entry by its index.
     * "histype" may be one of the HIST_ values.
     */
    /*private*/ static Bytes get_history_entry(int histype, int idx)
    {
        idx = calc_hist_idx(histype, idx);
        if (0 <= idx)
            return history[histype][idx].hisstr;

        return u8("");
    }

    /*
     * Clear all entries of a history.
     * "histype" may be one of the HIST_ values.
     */
    /*private*/ static boolean clr_history(int histype)
    {
        if (hislen != 0 && 0 <= histype && histype < HIST_COUNT)
        {
            histentry_C[] hist = history[histype];
            for (int i = hislen; 0 < i--; )
            {
                hist[i].hisstr = null;
                clear_hist_entry(hist[i]);
            }
            hisidx[histype] = -1;   /* mark history as cleared */
            hisnum[histype] = 0;    /* reset identifier counter */
            return true;
        }

        return false;
    }

    /*
     * Remove all entries matching {str} from a history.
     * "histype" may be one of the HIST_ values.
     */
    /*private*/ static boolean del_history_entry(int histype, Bytes str)
    {
        boolean found = false;

        regmatch_C regmatch = new regmatch_C();
        regmatch.regprog = null;
        regmatch.rm_ic = false;     /* always match case */

        int idx;
        if (hislen != 0
                && 0 <= histype
                && histype < HIST_COUNT
                && str.at(0) != NUL
                && 0 <= (idx = hisidx[histype])
                && (regmatch.regprog = vim_regcomp(str, RE_MAGIC + RE_STRING)) != null)
        {
            int last = idx;
            int i = last;
            do
            {
                histentry_C hisptr = history[histype][i];
                if (hisptr.hisstr == null)
                    break;
                if (vim_regexec(regmatch, hisptr.hisstr, 0))
                {
                    found = true;
                    hisptr.hisstr = null;
                    clear_hist_entry(hisptr);
                }
                else
                {
                    if (i != last)
                    {
                        COPY_histentry(history[histype][last], hisptr);
                        clear_hist_entry(hisptr);
                    }
                    if (--last < 0)
                        last += hislen;
                }
                if (--i < 0)
                    i += hislen;
            } while (i != idx);

            if (history[histype][idx].hisstr == null)
                hisidx[histype] = -1;
        }

        return found;
    }

    /*
     * Remove an indexed entry from a history.
     * "histype" may be one of the HIST_ values.
     */
    /*private*/ static boolean del_history_idx(int histype, int idx)
    {
        int i = calc_hist_idx(histype, idx);
        if (i < 0)
            return false;

        idx = hisidx[histype];
        history[histype][i].hisstr = null;

        /* When deleting the last added search string in a mapping, reset
         * last_maptick, so that the last added search string isn't deleted again.
         */
        if (histype == HIST_SEARCH && maptick == last_maptick && i == idx)
            last_maptick = -1;

        while (i != idx)
        {
            int j = (i + 1) % hislen;
            COPY_histentry(history[histype][i], history[histype][j]);
            i = j;
        }
        clear_hist_entry(history[histype][i]);
        if (--i < 0)
            i += hislen;
        hisidx[histype] = i;

        return true;
    }

    /*
     * Get indices "num1,num2" that specify a range within a list (not a range of
     * text lines in a buffer!) from a string.  Used for ":history" and ":clist".
     * Returns true if parsed successfully, otherwise false.
     */
    /*private*/ static boolean get_list_range(Bytes[] str, int[] num1, int[] num2)
    {
        boolean first = false;

        str[0] = skipwhite(str[0]);
        if (str[0].at(0) == (byte)'-' || asc_isdigit(str[0].at(0)))     /* parse "from" part of range */
        {
            int[] len = new int[1];
            long[] num = new long[1];
            vim_str2nr(str[0], null, len, FALSE, FALSE, num);
            str[0] = str[0].plus(len[0]);
            num1[0] = (int)num[0];
            first = true;
        }
        str[0] = skipwhite(str[0]);
        if (str[0].at(0) == (byte)',')                   /* parse "to" part of range */
        {
            str[0] = skipwhite(str[0].plus(1));
            int[] len = new int[1];
            long[] num = new long[1];
            vim_str2nr(str[0], null, len, FALSE, FALSE, num);
            if (0 < len[0])
            {
                num2[0] = (int)num[0];
                str[0] = skipwhite(str[0].plus(len[0]));
            }
            else if (!first)                /* no number given at all */
                return false;
        }
        else if (first)                     /* only one number given */
            num2[0] = num1[0];

        return true;
    }

    /*
     * :history command - print a history
     */
    /*private*/ static final ex_func_C ex_history = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int histype1 = HIST_CMD;
            int histype2 = HIST_CMD;
            int[] hisidx1 = { 1 };
            int[] hisidx2 = { -1 };

            Bytes arg = eap.arg;

            if (hislen == 0)
            {
                msg(u8("'history' option is zero"));
                return;
            }

            Bytes[] end = new Bytes[1];
            if (!(asc_isdigit(arg.at(0)) || arg.at(0) == (byte)'-' || arg.at(0) == (byte)','))
            {
                end[0] = arg;
                while (asc_isalpha(end[0].at(0)) || vim_strbyte(u8(":=@>/?"), end[0].at(0)) != null)
                    end[0] = end[0].plus(1);
                byte save_end = end[0].at(0);
                end[0].be(0, NUL);
                histype1 = get_histtype(arg);
                if (histype1 == -1)
                {
                    if (STRNCASECMP(arg, u8("all"), strlen(arg)) == 0)
                    {
                        histype1 = 0;
                        histype2 = HIST_COUNT-1;
                    }
                    else
                    {
                        end[0].be(0, save_end);
                        emsg(e_trailing);
                        return;
                    }
                }
                else
                    histype2 = histype1;
                end[0].be(0, save_end);
            }
            else
                end[0] = arg;
            if (!get_list_range(end, hisidx1, hisidx2) || end[0].at(0) != NUL)
            {
                emsg(e_trailing);
                return;
            }

            for ( ; !got_int && histype1 <= histype2; ++histype1)
            {
                STRCPY(ioBuff, u8("\n      #  "));
                STRCAT(STRCAT(ioBuff, history_names[histype1]), u8(" history"));
                msg_puts_title(ioBuff);
                int idx = hisidx[histype1];
                histentry_C[] hist = history[histype1];
                int j = hisidx1[0];
                if (j < 0)
                    j = (hislen < -j) ? 0 : hist[(hislen+j+idx+1) % hislen].hisnum;
                int k = hisidx2[0];
                if (k < 0)
                    k = (hislen < -k) ? 0 : hist[(hislen+k+idx+1) % hislen].hisnum;
                if (0 <= idx && j <= k)
                    for (int i = idx + 1; !got_int; i++)
                    {
                        if (i == hislen)
                            i = 0;
                        if (hist[i].hisstr != null && j <= hist[i].hisnum && hist[i].hisnum <= k)
                        {
                            msg_putchar('\n');
                            libC.sprintf(ioBuff, u8("%c%6d  "), (i == idx) ? (byte)'>' : (byte)' ', hist[i].hisnum);
                            if ((int)Columns[0] - 10 < mb_string2cells(hist[i].hisstr, -1))
                                trunc_string(hist[i].hisstr, ioBuff.plus(strlen(ioBuff)),
                                    (int)Columns[0] - 10, IOSIZE - strlen(ioBuff));
                            else
                                STRCAT(ioBuff, hist[i].hisstr);
                            msg_outtrans(ioBuff);
                            out_flush();
                        }
                        if (i == idx)
                            break;
                    }
            }
        }
    };

    /*
     * Open a window on the current command line and history.  Allow editing in
     * the window.  Returns when the window is closed.
     * Returns:
     *      CR       if the command is to be executed
     *      Ctrl_C   if it is to be abandoned
     *      K_IGNORE if editing continues
     */
    /*private*/ static int ex_window()
    {
        buffer_C old_curbuf = curbuf;
        window_C old_curwin = curwin;

        Bytes typestr = new Bytes(2);

        int save_restart_edit = restart_edit;
        int save_State = State;
        int save_exmode = exmode_active;
        boolean save_cmdmsg_rl = cmdmsg_rl;

        /* Can't do this recursively.  Can't do it when typing a password. */
        if (cmdwin_type != 0 || 0 < cmdline_star)
        {
            beep_flush();
            return K_IGNORE;
        }

        /* Save current window sizes. */
        iarray_C winsizes = new iarray_C(1);
        win_size_save(winsizes);

        /* Don't execute autocommands while creating the window. */
        block_autocmds();
        /* don't use a new tab page */
        cmdmod.tab = 0;

        /* Create a window for the command-line buffer. */
        if (win_split((int)p_cwh[0], WSP_BOT) == false)
        {
            beep_flush();
            unblock_autocmds();
            return K_IGNORE;
        }
        cmdwin_type = get_cmdline_type();

        /* Create the command-line buffer empty. */
        do_ecmd(0, null, null, null, ECMD_ONE, ECMD_HIDE, null);
        setfname(curbuf, u8("[Command Line]"), null, true);
        set_option_value(u8("bt"), 0L, u8("nofile"), OPT_LOCAL);
        curbuf.b_p_ma[0] = true;
        curwin.w_onebuf_opt.wo_rl[0] = cmdmsg_rl;
        cmdmsg_rl = false;
        curwin.w_onebuf_opt.wo_scb[0] = false;
        curwin.w_onebuf_opt.wo_crb[0] = false;

        /* Do execute autocommands for setting the filetype (load syntax). */
        unblock_autocmds();

        /* Showing the prompt may have set need_wait_return, reset it. */
        need_wait_return = false;

        int histtype = hist_char2type(cmdwin_type);
        if (histtype == HIST_CMD || histtype == HIST_DEBUG)
        {
            if (p_wc[0] == TAB)
            {
                add_map(u8("<buffer> <Tab> <C-X><C-V>"), INSERT);
                add_map(u8("<buffer> <Tab> a<C-X><C-V>"), NORMAL);
            }
            set_option_value(u8("ft"), 0L, u8("vim"), OPT_LOCAL);
        }

        /* Reset 'textwidth' after setting 'filetype' (the Vim filetype plugin sets 'textwidth' to 78). */
        curbuf.b_p_tw[0] = 0;

        /* Fill the buffer with the history. */
        init_history();
        if (0 < hislen)
        {
            int i = hisidx[histtype];
            if (0 <= i)
            {
                long lnum = 0;
                do
                {
                    if (++i == hislen)
                        i = 0;
                    if (history[histtype][i].hisstr != null)
                        ml_append(lnum++, history[histtype][i].hisstr, 0, false);
                } while (i != hisidx[histtype]);
            }
        }

        /* Replace the empty last line with the current command-line and put the cursor there. */
        ml_replace(curbuf.b_ml.ml_line_count, ccline.cmdbuff, true);
        curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
        curwin.w_cursor.col = ccline.cmdpos;
        changed_line_abv_curs();
        invalidate_botline();
        redraw_later(SOME_VALID);

        /* Save the command line info, can be used recursively. */
        cmdline_info_C save_cli = new cmdline_info_C();
        COPY_cmdline_info(save_cli, ccline);
        ccline.cmdbuff = null;
        ccline.cmdprompt = null;

        /* No Ex mode here! */
        exmode_active = 0;

        State = NORMAL;
        setmouse();

        /* Trigger CmdwinEnter autocommands. */
        typestr.be(0, cmdwin_type);
        typestr.be(1, NUL);
        apply_autocmds(EVENT_CMDWINENTER, typestr, typestr, false, curbuf);
        if (restart_edit != 0)      /* autocmd with ":startinsert" */
            stuffcharReadbuff(K_NOP);

        int i = redrawingDisabled;
        redrawingDisabled = 0;

        /*
         * Call the main loop until <CR> or CTRL-C is typed.
         */
        cmdwin_result = 0;
        main_loop(true, false);

        redrawingDisabled = i;

        /* Trigger CmdwinLeave autocommands. */
        apply_autocmds(EVENT_CMDWINLEAVE, typestr, typestr, false, curbuf);

        /* Restore the command line info. */
        COPY_cmdline_info(ccline, save_cli);
        cmdwin_type = 0;

        exmode_active = save_exmode;

        /* Safety check: The old window or buffer was deleted: It's a bug when this happens! */
        if (!win_valid(old_curwin) || !buf_valid(old_curbuf))
        {
            cmdwin_result = Ctrl_C;
            emsg(u8("E199: Active window or buffer deleted"));
        }
        else
        {
            /* autocmds may abort script processing */
            if (aborting() && cmdwin_result != K_IGNORE)
                cmdwin_result = Ctrl_C;

            /* Set the new command line from the cmdline buffer. */
            if (cmdwin_result == K_XF1 || cmdwin_result == K_XF2) /* :qa[!] typed */
            {
                Bytes p = (cmdwin_result == K_XF2) ? u8("qa") : u8("qa!");

                if (histtype == HIST_CMD)
                {
                    /* Execute the command directly. */
                    ccline.cmdbuff = STRDUP(p);
                    cmdwin_result = CAR;
                }
                else
                {
                    /* First need to cancel what we were doing. */
                    ccline.cmdbuff = null;
                    stuffcharReadbuff(':');
                    stuffReadbuff(p);
                    stuffcharReadbuff(CAR);
                }
            }
            else if (cmdwin_result == K_XF2)        /* :qa typed */
            {
                ccline.cmdbuff = STRDUP(u8("qa"));
                cmdwin_result = CAR;
            }
            else if (cmdwin_result == Ctrl_C)
            {
                /* :q or :close, don't execute any command and don't modify the cmd window. */
                ccline.cmdbuff = null;
            }
            else
                ccline.cmdbuff = STRDUP(ml_get_curline());

            if (ccline.cmdbuff == null)
                cmdwin_result = Ctrl_C;
            else
            {
                ccline.cmdlen = strlen(ccline.cmdbuff);
                ccline.cmdbufflen = ccline.cmdlen + 1;
                ccline.cmdpos = curwin.w_cursor.col;
                if (ccline.cmdlen < ccline.cmdpos)
                    ccline.cmdpos = ccline.cmdlen;
                if (cmdwin_result == K_IGNORE)
                {
                    set_cmdspos_cursor();
                    redrawcmd();
                }
            }

            /* Don't execute autocommands while deleting the window. */
            block_autocmds();
            window_C wp = curwin;
            buffer_C bp = curbuf;
            win_goto(old_curwin);
            win_close(wp, true);

            /* win_close() may have already wiped the buffer when 'bh' is set to 'wipe' */
            if (buf_valid(bp))
                close_buffer(null, bp, DOBUF_WIPE, false);

            /* Restore window sizes. */
            win_size_restore(winsizes);

            unblock_autocmds();
        }

        restart_edit = save_restart_edit;
        cmdmsg_rl = save_cmdmsg_rl;

        State = save_State;
        setmouse();

        return cmdwin_result;
    }

    /*
     * Used for commands that either take a simple command string argument, or:
     *      cmd << endmarker
     *        {script}
     *      endmarker
     * Returns a pointer to allocated memory with {script} or null.
     */
    /*private*/ static Bytes script_get(exarg_C eap, Bytes cmd)
    {
        if (cmd.at(0) != (byte)'<' || cmd.at(1) != (byte)'<' || eap.getline == null)
            return null;

        barray_C ba = new barray_C(0x400);

        Bytes dot = u8(".");

        Bytes end_pattern;
        if (cmd.at(2) != NUL)
            end_pattern = skipwhite(cmd.plus(2));
        else
            end_pattern = dot;

        for ( ; ; )
        {
            Bytes line = eap.getline.getline(0 < eap.cstack.cs_looplevel ? -1 : NUL, eap.cookie, 0);

            if (line == null || STRCMP(end_pattern, line) == 0)
                break;

            ba_concat(ba, line);
            ba_append(ba, (byte)'\n');
        }
        ba_append(ba, NUL);

        return new Bytes(ba.ba_data);
    }

    /*
     * ex_docmd.c: functions for executing an Ex command line -----------------------------------------
     */

    /*private*/ static int      quitmore;
    /*private*/ static boolean  ex_pressedreturn;

    /*private*/ static final class ucmd_C
    {
        Bytes       uc_name;        /* The command name */
        long        uc_argt;        /* The argument type */
        Bytes       uc_rep;         /* The command's replacement string */
        long        uc_def;         /* The default value for a range/count */
        int         uc_compl;       /* completion type */
        int         uc_addr_type;   /* The command's address type */
        int         uc_scriptID;    /* SID where the command was defined */
        Bytes       uc_compl_arg;   /* completion argument if any */

        /*private*/ ucmd_C()
        {
        }
    }

    /*private*/ static final int UC_BUFFER       = 1;       /* -buffer: local to current buffer */

    /*private*/ static Growing<ucmd_C> ucmds = new Growing<ucmd_C>(ucmd_C.class, 4);

    /* Wether a command index indicates a user command. */
    /*private*/ static boolean is_user_cmdidx(int idx)
    {
        return (idx < 0);
    }
}
