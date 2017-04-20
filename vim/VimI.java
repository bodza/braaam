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
public class VimI
{
    /*
     * This file defines the Ex commands.
     *
     * When adding an Ex command:
     * 1. Add an entry in the table below.  Keep it sorted on the shortest
     *    version of the command name that works.  If it doesn't start with a
     *    lower case letter, add it at the end.
     * 2. Add a "case: CMD_xxx" in the big switch in ex_docmd.c.
     */

    /*
     * This array maps ex command names to command codes.
     * The order in which command names are listed below is significant --
     * ambiguous abbreviations are always resolved to be the first possible match
     * (e.g. "r" is taken to mean "read", not "rewind", because "read" comes before "rewind").
     * Not supported commands are included to avoid ambiguities.
     */

    /*private*/ static abstract class ex_func_C
    {
        public abstract void ex(exarg_C eap);
    }

    /*private*/ static final class cmdname_C
    {
        Bytes       cmd_name;       /* name of the command */
        ex_func_C   cmd_func;       /* function for this command */
        long        cmd_argt;       /* flags declared above */
        int         cmd_addr_type;  /* flag for address type */

        /*private*/ cmdname_C(Bytes cmd_name, ex_func_C cmd_func, long cmd_argt, int cmd_addr_type)
        {
            this.cmd_name = cmd_name;
            this.cmd_func = cmd_func;
            this.cmd_argt = cmd_argt;
            this.cmd_addr_type = cmd_addr_type;
        }
    }

    /* ----------------------------------------------------------------------- */

    /*
     * Table used to quickly search for a command, based on its first character.
     */
    /*private*/ static int cmdidxs[/*27*/] =
    {
        CMD_append,
        CMD_buffer,
        CMD_change,
        CMD_delete,
        CMD_edit,
        CMD_files,
        CMD_global,
        CMD_highlight,
        CMD_insert,
        CMD_join,
        CMD_k,
        CMD_list,
        CMD_move,
        CMD_next,
        CMD_open,
        CMD_print,
        CMD_quit,
        CMD_read,
        CMD_substitute,
        CMD_t,
        CMD_undo,
        CMD_vglobal,
        CMD_write,
        CMD_xit,
        CMD_yank,
        CMD_z,
        CMD_bang
    };

    /*private*/ static Bytes dollar_command = new Bytes(new byte[] { '$', 0 });

    /* Struct for storing a line inside a while/for loop. */
    /*private*/ static final class wcmd_C
    {
        Bytes       line;           /* command line */
        long        lnum;           /* "sourcing_lnum" of the line */

        /*private*/ wcmd_C()
        {
        }
    }

    /*
     * Structure used to store info for line position in a while or for loop.
     * This is required, because do_one_cmd() may invoke ex_function(), which
     * reads more lines that may come from the while/for loop.
     */
    /*private*/ static final class loop_cookie_C
    {
        Growing<wcmd_C>    lines_gap;              /* growarray with line info */
        int         current_line;           /* last read line from growarray */
        boolean     repeating;              /* true when looping a second time */
        /* When "repeating" is false use "getline" and "cookie" to get lines. */
        getline_C   getline;
        Object      cookie;

        /*private*/ loop_cookie_C()
        {
        }
    }

    /* Struct to save a few things while debugging.  Used in do_cmdline() only. */
    /*private*/ static final class dbg_stuff_C
    {
        int         trylevel;
        boolean     force_abort;
        except_C    caught_stack;
        Bytes       vv_exception;
        Bytes       vv_throwpoint;
        boolean     did_emsg;
        boolean     got_int;
        boolean     did_throw;
        boolean     need_rethrow;
        boolean     check_cstack;
        except_C    current_exception;

        /*private*/ dbg_stuff_C()
        {
        }
    }

    /*private*/ static void save_dbg_stuff(dbg_stuff_C dsp)
    {
        dsp.trylevel       = trylevel;             trylevel = 0;
        dsp.force_abort    = force_abort;          force_abort = false;
        dsp.caught_stack   = caught_stack;         caught_stack = null;
        dsp.vv_exception   = v_exception(null);
        dsp.vv_throwpoint  = v_throwpoint(null);

        /* Necessary for debugging an inactive ":catch", ":finally", ":endtry". */
        dsp.did_emsg       = did_emsg;             did_emsg     = false;
        dsp.got_int        = got_int;              got_int      = false;
        dsp.did_throw      = did_throw;            did_throw    = false;
        dsp.need_rethrow   = need_rethrow;         need_rethrow = false;
        dsp.check_cstack   = check_cstack;         check_cstack = false;
        dsp.current_exception = current_exception; current_exception = null;
    }

    /*private*/ static void restore_dbg_stuff(dbg_stuff_C dsp)
    {
        suppress_errthrow = false;
        trylevel = dsp.trylevel;
        force_abort = dsp.force_abort;
        caught_stack = dsp.caught_stack;
        v_exception(dsp.vv_exception);
        v_throwpoint(dsp.vv_throwpoint);
        did_emsg = dsp.did_emsg;
        got_int = dsp.got_int;
        did_throw = dsp.did_throw;
        need_rethrow = dsp.need_rethrow;
        check_cstack = dsp.check_cstack;
        current_exception = dsp.current_exception;
    }

    /*
     * do_exmode(): Repeatedly get commands for the "Ex" mode, until the ":vi" command is given.
     */
    /*private*/ static void do_exmode(boolean improved)
        /* improved: true for "improved Ex" mode */
    {
        if (improved)
            exmode_active = EXMODE_VIM;
        else
            exmode_active = EXMODE_NORMAL;
        State = NORMAL;

        /* When using ":global /pat/ visual" and then "Q" we return to continue the :global command. */
        if (global_busy != 0)
            return;

        boolean save_msg_scroll = msg_scroll;
        redrawingDisabled++;            /* don't redisplay the window */
        no_wait_return++;               /* don't wait for return */

        msg(u8("Entering Ex mode.  Type \"visual\" to go to Normal mode."));
        while (exmode_active != 0)
        {
            /* Check for a ":normal" command and no more characters left. */
            if (0 < ex_normal_busy && typebuf.tb_len == 0)
            {
                exmode_active = 0;
                break;
            }
            msg_scroll = true;
            need_wait_return = false;
            ex_pressedreturn = false;
            ex_no_reprint = false;
            int changedtick = curbuf.b_changedtick;
            int prev_msg_row = msg_row;
            long prev_line = curwin.w_cursor.lnum;
            if (improved)
            {
                cmdline_row = msg_row;
                do_cmdline(null, getexline, null, 0);
            }
            else
                do_cmdline(null, getexmodeline, null, DOCMD_NOWAIT);
            lines_left = (int)Rows[0] - 1;

            if ((prev_line != curwin.w_cursor.lnum || changedtick != curbuf.b_changedtick) && !ex_no_reprint)
            {
                if ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0)
                    emsg(e_emptybuf);
                else
                {
                    if (ex_pressedreturn)
                    {
                        /* Go up one line, to overwrite the ":<CR>" line,
                         * so the output doesn't contain empty lines. */
                        msg_row = prev_msg_row;
                        if (prev_msg_row == (int)Rows[0] - 1)
                            msg_row--;
                    }
                    msg_col = 0;
                    print_line_no_prefix(curwin.w_cursor.lnum, false, false);
                    msg_clr_eos();
                }
            }
            else if (ex_pressedreturn && !ex_no_reprint)    /* must be at EOF */
            {
                if ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0)
                    emsg(e_emptybuf);
                else
                    emsg(u8("E501: At end-of-file"));
            }
        }

        --redrawingDisabled;
        --no_wait_return;
        update_screen(CLEAR);
        need_wait_return = false;
        msg_scroll = save_msg_scroll;
    }

    /*
     * Execute a simple command line.  Used for translated commands like "*".
     */
    /*private*/ static boolean do_cmdline_cmd(Bytes cmd)
    {
        return do_cmdline(cmd, null, null, DOCMD_VERBOSE|DOCMD_NOWAIT|DOCMD_KEYTYPED);
    }

    /*private*/ static int _0_recurse;              /* recursive depth */
    /*private*/ static int call_depth;              /* recursiveness */

    /*
     * do_cmdline(): execute one Ex command line
     *
     * 1. Execute "cmdline" when it is not null.
     *    If "cmdline" is null, or more lines are needed, fgetline() is used.
     * 2. Split up in parts separated with '|'.
     *
     * This function can be called recursively!
     *
     * flags:
     * DOCMD_VERBOSE  - The command will be included in the error message.
     * DOCMD_NOWAIT   - Don't call wait_return() and friends.
     * DOCMD_REPEAT   - Repeat execution until fgetline() returns null.
     * DOCMD_KEYTYPED - Don't reset keyTyped.
     * DOCMD_EXCRESET - Reset the exception environment (used for debugging).
     * DOCMD_KEEPLINE - Store first typed line (for repeating with ".").
     *
     * return false if cmdline could not be executed, true otherwise
     */
    /*private*/ static boolean do_cmdline(Bytes cmdline, getline_C fgetline, Object cookie, int flags)
        /* cookie: argument for fgetline() */
    {
        boolean retval = true;

        boolean used_getline = false;               /* used "fgetline" to obtain command */

        boolean msg_didout_before_start = false;
        int count = 0;                              /* line number count */
        boolean did_inc = false;                    /* incremented redrawingDisabled */
        int current_line = 0;                       /* active line in lines_ga */

        /* "fgetline" and "cookie" passed to do_one_cmd() */
        getline_C cmd_getline;
        Object cmd_cookie;
        loop_cookie_C cmd_loop_cookie = new loop_cookie_C();

        /* For every pair of do_cmdline()/do_one_cmd() calls, use an extra memory
         * location for storing error messages to be converted to an exception.
         * This ensures that the do_errthrow() call in do_one_cmd() does not
         * combine the messages stored by an earlier invocation of do_one_cmd()
         * with the command name of the later one.  This would happen when
         * BufWritePost autocommands are executed after a write error. */
        msglist_C[] saved_msg_list = msg_list;
        msglist_C[] private_msg_list = { null };
        msg_list = private_msg_list;

        /* It's possible to create an endless loop with ":execute", catch that here.
         * The value of 200 allows nested function calls, ":source", etc. */
        if (call_depth == 200)
        {
            emsg(u8("E169: Command too recursive"));
            /* When converting to an exception, we do not include the command name
             * since this is not an error of the specific command. */
            do_errthrow((condstack_C)null, null);
            msg_list = saved_msg_list;
            return false;
        }
        call_depth++;

        condstack_C cstack = new condstack_C();     /* conditional stack */
        cstack.cs_idx = -1;
        cstack.cs_looplevel = 0;
        cstack.cs_trylevel = 0;
        cstack.cs_emsg_silent_list = null;
        cstack.cs_lflags = 0;

        Growing<wcmd_C> lines_ga = new Growing<wcmd_C>(wcmd_C.class, 10); /* keep lines for ":while"/":for" */

        Object real_cookie = getline_cookie(fgetline, cookie);

        /* Inside a function use a higher nesting level. */
        boolean getline_is_func = getline_equal(fgetline, cookie, get_func_line);
        if (getline_is_func && ex_nesting_level == func_level((funccall_C)real_cookie))
            ex_nesting_level++;

        Bytes fname = null;                /* function or script name */
        long[] breakpoint = null;           /* ptr to breakpoint field in cookie */
        int[] dbg_tick = null;              /* ptr to dbg_tick field in cookie */

        /* Get the function or script name and the address where the next breakpoint
         * line and the debug tick for a function or script are stored. */
        if (getline_is_func)
        {
            fname = func_name((funccall_C)real_cookie);
            breakpoint = func_breakpoint((funccall_C)real_cookie);
            dbg_tick = func_dbg_tick((funccall_C)real_cookie);
        }
        else if (getline_equal(fgetline, cookie, getsourceline))
        {
            fname = sourcing_name;
            breakpoint = source_breakpoint((source_cookie_C)real_cookie);
            dbg_tick = source_dbg_tick((source_cookie_C)real_cookie);
        }

        /*
         * Initialize "force_abort" and "suppress_errthrow" at the top level.
         */
        if (_0_recurse == 0)
        {
            force_abort = false;
            suppress_errthrow = false;
        }

        /*
         * If requested, store and reset the global values controlling the
         * exception handling (used when debugging).  Otherwise clear it to avoid
         * a bogus compiler warning when the optimizer uses inline functions...
         */
        dbg_stuff_C debug_saved = new dbg_stuff_C();    /* saved things for debug mode */
        if ((flags & DOCMD_EXCRESET) != 0)
            save_dbg_stuff(debug_saved);

        int initial_trylevel = trylevel;

        /*
         * "did_throw" will be set to true when an exception is being thrown.
         */
        did_throw = false;
        /*
         * "did_emsg" will be set to true when emsg() is used,
         * in which case we cancel the whole command line, and any if/endif or loop.
         * If force_abort is set, we cancel everything.
         */
        did_emsg = false;

        /*
         * keyTyped is only set when calling vgetc().
         * Reset it here when not calling vgetc() (sourced command lines).
         */
        if ((flags & DOCMD_KEYTYPED) == 0 && !getline_equal(fgetline, cookie, getexline))
            keyTyped = false;

        Bytes[] cmdline_copy = { null };                 /* copy of cmd line */

        /*
         * Continue executing command lines:
         * - when inside an ":if", ":while" or ":for"
         * - for multiple commands on one line, separated with '|'
         * - when repeating until there are no more lines (for ":source")
         */
        Bytes next_cmdline = cmdline;
        do
        {
            getline_is_func = getline_equal(fgetline, cookie, get_func_line);

            /* stop skipping cmds for an error msg after all endif/while/for */
            if (next_cmdline == null
                    && !force_abort
                    && cstack.cs_idx < 0
                    && !(getline_is_func && func_has_abort((funccall_C)real_cookie)))
                did_emsg = false;

            /*
             * 1. If repeating a line in a loop, get a line from lines_ga.
             * 2. If no line given: get an allocated line with fgetline().
             * 3. If a line is given: make a copy, so we can mess with it.
             */

            /* 1. If repeating, get a previous line from lines_ga. */
            if (0 < cstack.cs_looplevel && current_line < lines_ga.ga_len)
            {
                /* Each '|' separated command is stored separately in lines_ga, to be able to jump to it.
                 * Don't use "next_cmdline" now. */
                cmdline_copy[0] = null;

                /* Check if a function has returned or, unless it has an unclosed try conditional, aborted. */
                if (getline_is_func && func_has_ended((funccall_C)real_cookie))
                {
                    retval = false;
                    break;
                }

                /* Check if a sourced file hit a ":finish" command. */
                if (source_finished(fgetline, cookie))
                {
                    retval = false;
                    break;
                }

                /* If breakpoints have been added/deleted need to check for it. */
                if (breakpoint != null && dbg_tick != null && dbg_tick[0] != debug_tick)
                {
                    breakpoint[0] = dbg_find_breakpoint(
                                    getline_equal(fgetline, cookie, getsourceline), fname, sourcing_lnum);
                    dbg_tick[0] = debug_tick;
                }

                next_cmdline = lines_ga.ga_data[current_line].line;
                sourcing_lnum = lines_ga.ga_data[current_line].lnum;

                /* Did we encounter a breakpoint? */
                if (breakpoint != null && breakpoint[0] != 0 && breakpoint[0] <= sourcing_lnum)
                {
                    dbg_breakpoint(fname, sourcing_lnum);
                    /* Find next breakpoint. */
                    breakpoint[0] = dbg_find_breakpoint(
                                   getline_equal(fgetline, cookie, getsourceline), fname, sourcing_lnum);
                    dbg_tick[0] = debug_tick;
                }
            }

            if (0 < cstack.cs_looplevel)
            {
                /* Inside a while/for loop we need to store the lines and use them again.
                 * Pass a different "fgetline" function to do_one_cmd() below,
                 * so that it stores lines in or reads them from "lines_ga".
                 * Makes it possible to define a function inside a while/for loop. */
                cmd_getline = get_loop_line;
                cmd_cookie = cmd_loop_cookie;
                cmd_loop_cookie.lines_gap = lines_ga;
                cmd_loop_cookie.current_line = current_line;
                cmd_loop_cookie.getline = fgetline;
                cmd_loop_cookie.cookie = cookie;
                cmd_loop_cookie.repeating = (current_line < lines_ga.ga_len);
            }
            else
            {
                cmd_getline = fgetline;
                cmd_cookie = cookie;
            }

            /* 2. If no line given, get an allocated line with fgetline(). */
            if (next_cmdline == null)
            {
                /*
                 * Need to set msg_didout for the first line after an ":if",
                 * otherwise the ":if" will be overwritten.
                 */
                if (count == 1 && getline_equal(fgetline, cookie, getexline))
                    msg_didout = true;
                if (fgetline == null || (next_cmdline = fgetline.getline(':', cookie,
                                                (cstack.cs_idx < 0) ? 0 : (cstack.cs_idx + 1) * 2)) == null)
                {
                    /* Don't call wait_return for aborted command line.  The null
                     * returned for the end of a sourced file or executed function
                     * doesn't do this. */
                    if (keyTyped && (flags & DOCMD_REPEAT) == 0)
                        need_wait_return = false;
                    retval = false;
                    break;
                }
                used_getline = true;

                /*
                 * Keep the first typed line.  Clear it when more lines are typed.
                 */
                if ((flags & DOCMD_KEEPLINE) != 0)
                {
                    if (count == 0)
                        repeat_cmdline = STRDUP(next_cmdline);
                    else
                        repeat_cmdline = null;
                }
            }

            /* 3. Make a copy of the command so we can mess with it. */
            else if (cmdline_copy[0] == null)
            {
                next_cmdline = STRDUP(next_cmdline);
            }
            cmdline_copy[0] = next_cmdline;

            /*
             * Save the current line when inside a ":while" or ":for", and when
             * the command looks like a ":while" or ":for", because we may need it
             * later.  When there is a '|' and another command, it is stored
             * separately, because we need to be able to jump back to it from an
             * :endwhile/:endfor.
             */
            if (current_line == lines_ga.ga_len && (cstack.cs_looplevel != 0 || has_loop_cmd(next_cmdline)))
            {
                if (store_loop_line(lines_ga, next_cmdline) == false)
                {
                    retval = false;
                    break;
                }
            }
            did_endif = false;

            if (count++ == 0)
            {
                /*
                 * All output from the commands is put below each other, without waiting for a return.
                 * Don't do this when executing commands from a script or when being called recursive
                 * (e.g. for ":e +command file").
                 */
                if ((flags & DOCMD_NOWAIT) == 0 && _0_recurse == 0)
                {
                    msg_didout_before_start = msg_didout;
                    msg_didany = false; /* no output yet */
                    msg_start();
                    msg_scroll = true;  /* put messages below each other */
                    no_wait_return++;   /* don't wait for return until finished */
                    redrawingDisabled++;
                    did_inc = true;
                }
            }

            if (15 <= p_verbose[0] && sourcing_name != null)
            {
                no_wait_return++;
                verbose_enter_scroll();

                smsg(u8("line %ld: %s"), sourcing_lnum, cmdline_copy[0]);
                if (msg_silent == 0)
                    msg_puts(u8("\n"));     /* don't overwrite this */

                verbose_leave_scroll();
                --no_wait_return;
            }

            /*
             * 2. Execute one '|' separated command.
             *    do_one_cmd() will return null if there is no trailing '|'.
             *    "cmdline_copy" can change, e.g. for '%' and '#' expansion.
             */
            _0_recurse++;
            next_cmdline = do_one_cmd(cmdline_copy, (flags & DOCMD_VERBOSE) != 0, cstack, cmd_getline, cmd_cookie);
            --_0_recurse;

            if (cmd_cookie == cmd_loop_cookie)
                /* Use "current_line" from "cmd_loop_cookie",
                 * it may have been incremented when defining a function. */
                current_line = cmd_loop_cookie.current_line;

            if (next_cmdline == null)
            {
                cmdline_copy[0] = null;
                /*
                 * If the command was typed, remember it for the ':' register.
                 * Do this AFTER executing the command to make :@: work.
                 */
                if (getline_equal(fgetline, cookie, getexline) && new_last_cmdline != null)
                {
                    last_cmdline = new_last_cmdline;
                    new_last_cmdline = null;
                }
            }
            else
            {
                /* Need to copy the command after the '|' to 'cmdline_copy',
                 * for the next do_one_cmd(). */
                BCOPY(cmdline_copy[0], next_cmdline, strlen(next_cmdline) + 1);
                next_cmdline = cmdline_copy[0];
            }

            /* reset did_emsg for a function that is not aborted by an error */
            if (did_emsg && !force_abort
                    && getline_equal(fgetline, cookie, get_func_line)
                            && !func_has_abort((funccall_C)real_cookie))
                did_emsg = false;

            if (0 < cstack.cs_looplevel)
            {
                current_line++;

                /*
                 * An ":endwhile", ":endfor" and ":continue" is handled here.
                 * If we were executing commands, jump back to the ":while" or ":for".
                 * If we were not executing commands, decrement cs_looplevel.
                 */
                if ((cstack.cs_lflags & (CSL_HAD_CONT | CSL_HAD_ENDLOOP)) != 0)
                {
                    cstack.cs_lflags &= ~(CSL_HAD_CONT | CSL_HAD_ENDLOOP);

                    /* Jump back to the matching ":while" or ":for".  Be careful
                     * not to use a cs_line[] from an entry that isn't a ":while"
                     * or ":for": It would make "current_line" invalid and can
                     * cause a crash. */
                    if (!did_emsg && !got_int && !did_throw
                            && 0 <= cstack.cs_idx
                            && (cstack.cs_flags[cstack.cs_idx] & (CSF_WHILE | CSF_FOR)) != 0
                            && 0 <= cstack.cs_line[cstack.cs_idx]
                            && (cstack.cs_flags[cstack.cs_idx] & CSF_ACTIVE) != 0)
                    {
                        current_line = cstack.cs_line[cstack.cs_idx];
                                                    /* remember we jumped there */
                        cstack.cs_lflags |= CSL_HAD_LOOP;
                        line_breakcheck();          /* check if CTRL-C typed */

                        /* Check for the next breakpoint at or after the ":while" or ":for". */
                        if (breakpoint != null)
                        {
                            breakpoint[0] = dbg_find_breakpoint(
                                    getline_equal(fgetline, cookie, getsourceline), fname,
                                            lines_ga.ga_data[current_line].lnum - 1);
                            dbg_tick[0] = debug_tick;
                        }
                    }
                    else
                    {
                        /* can only get here with ":endwhile" or ":endfor" */
                        if (0 <= cstack.cs_idx)
                        {
                            int[] __ = { cstack.cs_looplevel };
                            rewind_conditionals(cstack, cstack.cs_idx - 1, CSF_WHILE | CSF_FOR, __);
                            cstack.cs_looplevel = __[0];
                        }
                    }
                }

                /*
                 * For a ":while" or ":for" we need to remember the line number.
                 */
                else if ((cstack.cs_lflags & CSL_HAD_LOOP) != 0)
                {
                    cstack.cs_lflags &= ~CSL_HAD_LOOP;
                    cstack.cs_line[cstack.cs_idx] = current_line - 1;
                }
            }

            /*
             * When not inside any ":while" loop, clear remembered lines.
             */
            if (cstack.cs_looplevel == 0)
            {
                if (0 < lines_ga.ga_len)
                {
                    sourcing_lnum = lines_ga.ga_data[lines_ga.ga_len - 1].lnum;
                    free_cmdlines(lines_ga);
                }
                current_line = 0;
            }

            /*
             * A ":finally" makes did_emsg, got_int, and did_throw pending for being restored
             * at the ":endtry".  Reset them here and set the ACTIVE and FINALLY flags, so that
             * the finally clause gets executed.  This includes the case where a missing ":endif",
             * ":endwhile" or ":endfor" was detected by the ":finally" itself.
             */
            if ((cstack.cs_lflags & CSL_HAD_FINA) != 0)
            {
                cstack.cs_lflags &= ~CSL_HAD_FINA;
                report_make_pending(cstack.cs_pending[cstack.cs_idx] & (CSTP_ERROR | CSTP_INTERRUPT | CSTP_THROW),
                                                did_throw ? current_exception : null);
                did_emsg = got_int = did_throw = false;
                cstack.cs_flags[cstack.cs_idx] |= CSF_ACTIVE | CSF_FINALLY;
            }

            /* Update global "trylevel" for recursive calls to do_cmdline() from within this loop. */
            trylevel = initial_trylevel + cstack.cs_trylevel;

            /*
             * If the outermost try conditional (across function calls and sourced files)
             * is aborted because of an error, an interrupt, or an uncaught exception,
             * cancel everything.  If it is left normally, reset force_abort to get
             * the non-EH compatible abortion behavior for the rest of the script.
             */
            if (trylevel == 0 && !did_emsg && !got_int && !did_throw)
                force_abort = false;

            /* Convert an interrupt to an exception if appropriate. */
            do_intthrow(cstack);
        }
        /*
         * Continue executing command lines when:
         * - no CTRL-C typed, no aborting error, no exception thrown or try conditionals need
         *   to be checked for executing finally clauses or catching an interrupt exception
         * - didn't get an error message or lines are not typed
         * - there is a command after '|', inside a :if, :while, :for or :try, or looping
         *   for ":source" command or function call.
         */
        while (!((got_int || (did_emsg && force_abort) || did_throw) && cstack.cs_trylevel == 0)
                && !(did_emsg
                    /* Keep going when inside try/catch, so that the error can be deal with,
                     * except when it is a syntax error, it may cause the :endtry to be missed. */
                    && (cstack.cs_trylevel == 0 || did_emsg_syntax)
                    && used_getline
                            && (getline_equal(fgetline, cookie, getexmodeline)
                                   || getline_equal(fgetline, cookie, getexline)))
                && (next_cmdline != null
                            || 0 <= cstack.cs_idx
                            || (flags & DOCMD_REPEAT) != 0));

        did_emsg_syntax = false;
        free_cmdlines(lines_ga);
        lines_ga.ga_clear();

        if (0 <= cstack.cs_idx)
        {
            /*
             * If a sourced file or executed function ran to its end, report the unclosed conditional.
             */
            if (!got_int && !did_throw
                    && ((getline_equal(fgetline, cookie, getsourceline)
                            && !source_finished(fgetline, cookie))
                     || (getline_equal(fgetline, cookie, get_func_line)
                            && !func_has_ended((funccall_C)real_cookie))))
            {
                if ((cstack.cs_flags[cstack.cs_idx] & CSF_TRY) != 0)
                    emsg(e_endtry);
                else if ((cstack.cs_flags[cstack.cs_idx] & CSF_WHILE) != 0)
                    emsg(e_endwhile);
                else if ((cstack.cs_flags[cstack.cs_idx] & CSF_FOR) != 0)
                    emsg(e_endfor);
                else
                    emsg(e_endif);
            }

            /*
             * Reset "trylevel" in case of a ":finish" or ":return" or a missing
             * ":endtry" in a sourced file or executed function.  If the try
             * conditional is in its finally clause, ignore anything pending.
             * If it is in a catch clause, finish the caught exception.
             * Also cleanup any "cs_forinfo" structures.
             */
            do
            {
                int idx = cleanup_conditionals(cstack, 0, true);
                if (0 <= idx)
                    --idx;      /* remove try block not in its finally clause */

                { int[] __ = { cstack.cs_looplevel }; rewind_conditionals(cstack, idx, CSF_WHILE | CSF_FOR, __); cstack.cs_looplevel = __[0]; }
            } while (0 <= cstack.cs_idx);
            trylevel = initial_trylevel;
        }

        /* If a missing ":endtry", ":endwhile", ":endfor", or ":endif" or a memory
         * lack was reported above and the error message is to be converted to an
         * exception, do this now after rewinding the cstack. */
        do_errthrow(cstack, getline_equal(fgetline, cookie, get_func_line) ? u8("endfunction") : null);

        if (trylevel == 0)
        {
            /*
             * When an exception is being thrown out of the outermost try conditional,
             * discard the uncaught exception, disable the conversion of interrupts
             * or errors to exceptions, and ensure that no more commands are executed.
             */
            if (did_throw)
            {
                Bytes p = null;
                msglist_C messages = null;

                /*
                 * If the uncaught exception is a user exception, report it as an error.
                 * If it is an error exception, display the saved error message now.
                 * For an interrupt exception, do nothing; the interrupt message is given elsewhere.
                 */
                switch (current_exception.type)
                {
                    case ET_USER:
                        vim_snprintf(ioBuff, IOSIZE, u8("E605: Exception not caught: %s"), current_exception.value);
                        p = STRDUP(ioBuff);
                        break;
                    case ET_ERROR:
                        messages = current_exception.messages;
                        current_exception.messages = null;
                        break;
                    case ET_INTERRUPT:
                        break;
                    default:
                        p = STRDUP(e_internal);
                        break;
                }

                Bytes saved_sourcing_name = sourcing_name;
                long saved_sourcing_lnum = sourcing_lnum;
                sourcing_name = current_exception.throw_name;
                sourcing_lnum = current_exception.throw_lnum;
                current_exception.throw_name = null;

                discard_current_exception();        /* uses ioBuff if 'verbose' */
                suppress_errthrow = true;
                force_abort = true;

                if (messages != null)
                {
                    do
                    {
                        msglist_C next = messages.next;
                        emsg(messages.msg);
                        messages = next;
                    } while (messages != null);
                }
                else if (p != null)
                {
                    emsg(p);
                }

                sourcing_name = saved_sourcing_name;
                sourcing_lnum = saved_sourcing_lnum;
            }

            /*
             * On an interrupt or an aborting error not converted to an exception,
             * disable the conversion of errors to exceptions.  (Interrupts are not
             * converted any more, here.) This enables also the interrupt message
             * when force_abort is set and did_emsg unset in case of an interrupt
             * from a finally clause after an error.
             */
            else if (got_int || (did_emsg && force_abort))
                suppress_errthrow = true;
        }

        /*
         * The current cstack will be freed when do_cmdline() returns.  An uncaught
         * exception will have to be rethrown in the previous cstack.  If a function
         * has just returned or a script file was just finished and the previous
         * cstack belongs to the same function or, respectively, script file, it
         * will have to be checked for finally clauses to be executed due to the
         * ":return" or ":finish".  This is done in do_one_cmd().
         */
        if (did_throw)
            need_rethrow = true;
        if ((getline_equal(fgetline, cookie, getsourceline)
                && source_level((source_cookie_C)real_cookie) < ex_nesting_level)
         || (getline_equal(fgetline, cookie, get_func_line)
                && func_level((funccall_C)real_cookie) + 1 < ex_nesting_level))
        {
            if (!did_throw)
                check_cstack = true;
        }
        else
        {
            /* When leaving a function, reduce nesting level. */
            if (getline_equal(fgetline, cookie, get_func_line))
                --ex_nesting_level;
            /*
             * Go to debug mode when returning from a function in which we are single-stepping.
             */
            if ((getline_equal(fgetline, cookie, getsourceline)
              || getline_equal(fgetline, cookie, get_func_line))
                    && ex_nesting_level + 1 <= debug_break_level)
                do_debug(getline_equal(fgetline, cookie, getsourceline) ? u8("End of sourced file") : u8("End of function"));
        }

        /*
         * Restore the exception environment (done after returning from the debugger).
         */
        if ((flags & DOCMD_EXCRESET) != 0)
            restore_dbg_stuff(debug_saved);

        msg_list = saved_msg_list;

        /*
         * If there was too much output to fit on the command line, ask the user to
         * hit return before redrawing the screen.  With the ":global" command we do
         * this only once after the command is finished.
         */
        if (did_inc)
        {
            --redrawingDisabled;
            --no_wait_return;
            msg_scroll = false;

            /*
             * When just finished an ":if"-":else" which was typed, no need to
             * wait for hit-return.  Also for an error situation.
             */
            if (retval == false || (did_endif && keyTyped && !did_emsg))
            {
                need_wait_return = false;
                msg_didany = false;         /* don't wait when restarting edit */
            }
            else if (need_wait_return)
            {
                /*
                 * The msg_start() above clears msg_didout.  The wait_return we do here
                 * should not overwrite the command that may be shown before doing that.
                 */
                msg_didout |= msg_didout_before_start;
                wait_return(FALSE);
            }
        }

        did_endif = false;          /* in case do_cmdline used recursively */

        --call_depth;

        return retval;
    }

    /*
     * Obtain a line when inside a ":while" or ":for" loop.
     */
    /*private*/ static final getline_C get_loop_line = new getline_C()
    {
        public Bytes getline(int c, Object cookie, int indent)
        {
            loop_cookie_C cp = (loop_cookie_C)cookie;

            if (cp.lines_gap.ga_len <= cp.current_line + 1)
            {
                if (cp.repeating)
                    return null;        /* trying to read past ":endwhile"/":endfor" */

                /* First time inside the ":while"/":for": get line normally. */
                Bytes line;
                if (cp.getline == null)
                    line = getcmdline(c, 0L, indent);
                else
                    line = cp.getline.getline(c, cp.cookie, indent);

                if (line != null && store_loop_line(cp.lines_gap, line) == true)
                    cp.current_line++;

                return line;
            }

            keyTyped = false;
            cp.current_line++;
            wcmd_C wp = cp.lines_gap.ga_data[cp.current_line];
            sourcing_lnum = wp.lnum;

            return STRDUP(wp.line);
        }
    };

    /*
     * Store a line in "gap" so that a ":while" loop can execute it again.
     */
    /*private*/ static boolean store_loop_line(Growing<wcmd_C> gap, Bytes line)
    {
        gap.ga_grow(1);
        wcmd_C wp = gap.ga_data[gap.ga_len++] = new wcmd_C();
        wp.line = STRDUP(line);
        wp.lnum = sourcing_lnum;

        return true;
    }

    /*
     * Free the lines stored for a ":while" or ":for" loop.
     */
    /*private*/ static void free_cmdlines(Growing<wcmd_C> gap)
    {
        for (wcmd_C[] wpp = gap.ga_data; 0 < gap.ga_len; )
            wpp[--gap.ga_len] = null;
    }

    /*
     * If "fgetline" is get_loop_line(), return true if the getline it uses equals "func".
     * Otherwise return true when "fgetline" equals "func".
     */
    /*private*/ static boolean getline_equal(getline_C fgetline, Object cookie, getline_C func)
        /* cookie: argument for fgetline() */
    {
        getline_C gl = fgetline;

        /* When "fgetline" is "get_loop_line()",
         * use the "cookie" to find the function that's originally used to obtain the lines.
         * This may be nested several levels. */
        for (loop_cookie_C lc = (loop_cookie_C)cookie; gl == get_loop_line; lc = (loop_cookie_C)lc.cookie)
            gl = lc.getline;

        return (gl == func);
    }

    /*
     * If "fgetline" is get_loop_line(), return the cookie used by the original getline function.
     * Otherwise return "cookie".
     */
    /*private*/ static Object getline_cookie(getline_C fgetline, Object cookie)
        /* cookie: argument for fgetline() */
    {
        getline_C gl = fgetline;

        /* When "fgetline" is "get_loop_line()",
         * use the "cookie" to find the cookie that's originally used to obtain the lines.
         * This may be nested several levels. */
        loop_cookie_C lc;
        for (lc = (loop_cookie_C)cookie; gl == get_loop_line; lc = (loop_cookie_C)lc.cookie)
            gl = lc.getline;

        return lc;
    }

    /*
     * Helper function to apply an offset for buffer commands, i.e. ":bdelete", ":bwipeout", etc.
     * Returns the buffer number.
     */
    /*private*/ static int compute_buffer_local_count(int addr_type, int fnum, int offset)
    {
        int count = offset;

        buffer_C buf = firstbuf;
        while (buf.b_next != null && buf.b_fnum < fnum)
            buf = buf.b_next;
        while (count != 0)
        {
            count += (offset < 0) ? 1 : -1;
            buffer_C nextbuf = (offset < 0) ? buf.b_prev : buf.b_next;
            if (nextbuf == null)
                break;
            buf = nextbuf;
            if (addr_type == ADDR_LOADED_BUFFERS)
                /* skip over unloaded buffers */
                while (buf.b_ml.ml_mfp == null)
                {
                    nextbuf = (offset < 0) ? buf.b_prev : buf.b_next;
                    if (nextbuf == null)
                        break;
                    buf = nextbuf;
                }
        }
        /* we might have gone too far, last buffer is not loadedd */
        if (addr_type == ADDR_LOADED_BUFFERS)
            while (buf.b_ml.ml_mfp == null)
            {
                buffer_C nextbuf = (0 <= offset) ? buf.b_prev : buf.b_next;
                if (nextbuf == null)
                    break;
                buf = nextbuf;
            }
        return buf.b_fnum;
    }

    /*private*/ static int current_win_nr(window_C win)
    {
        int nr = 0;

        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
        {
            nr++;
            if (wp == win)
                break;
        }

        return nr;
    }

    /*private*/ static int current_tab_nr(tabpage_C tab)
    {
        int nr = 0;

        for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
        {
            nr++;
            if (tp == tab)
                break;
        }

        return nr;
    }

    /*
     * Function called for command which is Not Implemented.  NI!
     */
    /*private*/ static final ex_func_C ex_ni = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (!eap.skip)
                eap.errmsg = u8("E319: Sorry, the command is not available in this version");
        }
    };

    /*
     * Function called for script command which is Not Implemented.  NI!
     * Skips over ":perl <<EOF" constructs.
     */
    /*private*/ static final ex_func_C ex_script_ni = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (!eap.skip)
                ex_ni.ex(eap);
            else
                script_get(eap, eap.arg);
        }
    };

    /*
     * Execute one Ex command.
     *
     * If 'sourcing' is true, the command will be included in the error message.
     *
     * 1. skip comment lines and leading space
     * 2. handle command modifiers
     * 3. find the command
     * 4. parse range
     * 5. parse the command
     * 6. parse arguments
     * 7. switch on command name
     *
     * Note: "fgetline" can be null.
     *
     * This function may be called recursively!
     */
    /*private*/ static Bytes do_one_cmd(Bytes[] cmdlinep, boolean sourcing, condstack_C cstack, getline_C fgetline, Object cookie)
        /* cookie: argument for fgetline() */
    {
        Bytes errormsg = null;         /* error message */
        long verbose_save = -1;
        boolean save_msg_scroll = msg_scroll;
        int save_msg_silent = -1;
        int did_esilent = 0;
        boolean did_sandbox = false;

        exarg_C ea = new exarg_C();     /* Ex command arguments */
        ea.line1 = 1;
        ea.line2 = 1;
        ex_nesting_level++;

        /* When the last file has not been edited :q has to be typed twice. */
        if (quitmore != 0
                /* avoid that a function call in 'statusline' does this */
                && !getline_equal(fgetline, cookie, get_func_line)
                /* avoid that an autocommand, e.g. QuitPre, does this */
                && !getline_equal(fgetline, cookie, getnextac)
                )
            --quitmore;

        /*
         * Reset browse, confirm, etc..  They are restored when returning, for recursive calls.
         */
        cmdmod_C save_cmdmod = new cmdmod_C();
        COPY_cmdmod(save_cmdmod, cmdmod);
        ZER0_cmdmod(cmdmod);

        doend:
        {
            /* "#!anything" is handled like a comment. */
            if (cmdlinep[0].at(0) == (byte)'#' && cmdlinep[0].at(1) == (byte)'!')
                break doend;

            /*
             * Repeat until no more command modifiers are found.
             */
            ea.cmd = cmdlinep[0];
            for ( ; ; )
            {
                /*
                 * 1. Skip comment lines and leading white space and colons.
                 */
                while (ea.cmd.at(0) == (byte)' ' || ea.cmd.at(0) == (byte)'\t' || ea.cmd.at(0) == (byte)':')
                    ea.cmd = ea.cmd.plus(1);

                /* in ex mode, an empty line works like :+ */
                if (ea.cmd.at(0) == NUL && exmode_active != 0
                        && (getline_equal(fgetline, cookie, getexmodeline)
                         || getline_equal(fgetline, cookie, getexline))
                        && curwin.w_cursor.lnum < curbuf.b_ml.ml_line_count)
                {
                    ea.cmd = u8("+");
                    ex_pressedreturn = true;
                }

                /* ignore comment and empty lines */
                if (ea.cmd.at(0) == (byte)'"')
                    break doend;
                if (ea.cmd.at(0) == NUL)
                {
                    ex_pressedreturn = true;
                    break doend;
                }

                /*
                 * 2. Handle command modifiers.
                 */
                Bytes[] p = { ea.cmd };
                if (asc_isdigit(p[0].at(0)))
                    p[0] = skipwhite(skipdigits(p[0]));
                switch (p[0].at(0))
                {
                    /* When adding an entry, also modify cmd_exists(). */
                    case 'a':
                    {
                        if (checkforcmd(p, u8("aboveleft"), 3))
                        {
                            ea.cmd = p[0];
                            cmdmod.split |= WSP_ABOVE;
                            continue;
                        }
                        break;
                    }

                    case 'b':
                    {
                        if (checkforcmd(p, u8("belowright"), 3))
                        {
                            ea.cmd = p[0];
                            cmdmod.split |= WSP_BELOW;
                            continue;
                        }
                        if (checkforcmd(p, u8("browse"), 3))
                        {
                            ea.cmd = p[0];
                            continue;
                        }
                        if (checkforcmd(p, u8("botright"), 2))
                        {
                            ea.cmd = p[0];
                            cmdmod.split |= WSP_BOT;
                            continue;
                        }
                        break;
                    }

                    case 'c':
                    {
                        if (checkforcmd(p, u8("confirm"), 4))
                        {
                            ea.cmd = p[0];
                            cmdmod.confirm = true;
                            continue;
                        }
                        break;
                    }

                    case 'k':
                    {
                        if (checkforcmd(p, u8("keepmarks"), 3))
                        {
                            ea.cmd = p[0];
                            cmdmod.keepmarks = true;
                            continue;
                        }
                        if (checkforcmd(p, u8("keepalt"), 5))
                        {
                            ea.cmd = p[0];
                            cmdmod.keepalt = true;
                            continue;
                        }
                        if (checkforcmd(p, u8("keeppatterns"), 5))
                        {
                            ea.cmd = p[0];
                            cmdmod.keeppatterns = true;
                            continue;
                        }
                        if (checkforcmd(p, u8("keepjumps"), 5))
                        {
                            ea.cmd = p[0];
                            cmdmod.keepjumps = true;
                            continue;
                        }
                        break;
                    }

                    case 'h':
                    {
                        /* ":hide" and ":hide | cmd" are not modifiers */
                        if (BEQ(p[0], ea.cmd) && checkforcmd(p, u8("hide"), 3) && p[0].at(0) != NUL && !ends_excmd(p[0].at(0)))
                        {
                            ea.cmd = p[0];
                            cmdmod.hide = true;
                            continue;
                        }
                        break;
                    }

                    case 'l':
                    {
                        if (checkforcmd(p, u8("lockmarks"), 3))
                        {
                            ea.cmd = p[0];
                            cmdmod.lockmarks = true;
                            continue;
                        }
                        if (checkforcmd(p, u8("leftabove"), 5))
                        {
                            ea.cmd = p[0];
                            cmdmod.split |= WSP_ABOVE;
                            continue;
                        }
                        break;
                    }

                    case 'n':
                    {
                        if (checkforcmd(p, u8("noautocmd"), 3))
                        {
                            ea.cmd = p[0];
                            if (cmdmod.save_ei == null)
                            {
                                /* Set 'eventignore' to "all".
                                 * Restore the existing option value later. */
                                cmdmod.save_ei = STRDUP(p_ei[0]);
                                set_string_option_direct(u8("ei"), -1, u8("all"), OPT_FREE, SID_NONE);
                            }
                            continue;
                        }
                        break;
                    }

                    case 'r':
                    {
                        if (checkforcmd(p, u8("rightbelow"), 6))
                        {
                            ea.cmd = p[0];
                            cmdmod.split |= WSP_BELOW;
                            continue;
                        }
                        break;
                    }

                    case 's':
                    {
                        if (checkforcmd(p, u8("sandbox"), 3))
                        {
                            ea.cmd = p[0];
                            if (!did_sandbox)
                                sandbox++;
                            did_sandbox = true;
                            continue;
                        }
                        if (checkforcmd(p, u8("silent"), 3))
                        {
                            ea.cmd = p[0];
                            if (save_msg_silent == -1)
                                save_msg_silent = msg_silent;
                            msg_silent++;
                            if (ea.cmd.at(0) == (byte)'!' && !vim_iswhite(ea.cmd.at(-1)))
                            {
                                /* ":silent!", but not "silent !cmd" */
                                ea.cmd = skipwhite(ea.cmd.plus(1));
                                emsg_silent++;
                                did_esilent++;
                            }
                            continue;
                        }
                        break;
                    }

                    case 't':
                    {
                        if (checkforcmd(p, u8("tab"), 3))
                        {
                            if (asc_isdigit(ea.cmd.at(0)))
                                cmdmod.tab = libC.atoi(ea.cmd) + 1;
                            else
                                cmdmod.tab = tabpage_index(curtab) + 1;
                            ea.cmd = p[0];
                            continue;
                        }
                        if (checkforcmd(p, u8("topleft"), 2))
                        {
                            ea.cmd = p[0];
                            cmdmod.split |= WSP_TOP;
                            continue;
                        }
                        break;
                    }

                    case 'u':
                    {
                        if (checkforcmd(p, u8("unsilent"), 3))
                        {
                            ea.cmd = p[0];
                            if (save_msg_silent == -1)
                                save_msg_silent = msg_silent;
                            msg_silent = 0;
                            continue;
                        }
                        break;
                    }

                    case 'v':
                    {
                        if (checkforcmd(p, u8("vertical"), 4))
                        {
                            ea.cmd = p[0];
                            cmdmod.split |= WSP_VERT;
                            continue;
                        }
                        if (checkforcmd(p, u8("verbose"), 4))
                        {
                            if (verbose_save < 0)
                                verbose_save = p_verbose[0];
                            if (asc_isdigit(ea.cmd.at(0)))
                                p_verbose[0] = libC.atoi(ea.cmd);
                            else
                                p_verbose[0] = 1;
                            ea.cmd = p[0];
                            continue;
                        }
                        break;
                    }
                }
                break;
            }

            ea.skip = (did_emsg || got_int || did_throw
                            || (0 <= cstack.cs_idx && (cstack.cs_flags[cstack.cs_idx] & CSF_ACTIVE) == 0));

            /* May go to debug mode.  If this happens and the ">quit" debug command
             * is used, throw an interrupt exception and skip the next command. */
            dbg_check_breakpoint(ea);
            if (!ea.skip && got_int)
            {
                ea.skip = true;
                do_intthrow(cstack);
            }

            /*
             * 3. Skip over the range to find the command.  Let "p" point to after it.
             *
             * We need the command to know what kind of range it uses.
             */
            Bytes cmd = ea.cmd;
            ea.cmd = skip_range(ea.cmd, null);
            if (ea.cmd.at(0) == (byte)'*' && vim_strbyte(p_cpo[0], CPO_STAR) == null)
                ea.cmd = skipwhite(ea.cmd.plus(1));
            Bytes p = find_command(ea, null);

            /*
             * 4. parse a range specifier of the form: addr [,addr] [;addr] ..
             *
             * where 'addr' is:
             *
             * %          (entire file)
             * $  [+-NUM]
             * 'x [+-NUM] (where x denotes a currently defined mark)
             * .  [+-NUM]
             * [+-NUM]..
             * NUM
             *
             * The ea.cmd pointer is updated to point to the first character following the
             * range spec.  If an initial address is found, but no second, the upper bound
             * is equal to the lower.
             */

            /* ea.addr_type for user commands is set by find_ucmd() */
            if (!is_user_cmdidx(ea.cmdidx))
            {
                if (ea.cmdidx != CMD_SIZE)
                    ea.addr_type = cmdnames[ea.cmdidx].cmd_addr_type;
                else
                    ea.addr_type = ADDR_LINES;

                /* :wincmd range depends on the argument. */
                if (ea.cmdidx == CMD_wincmd && p != null)
                    get_wincmd_addr_type(skipwhite(p), ea);
            }

            long lnum;

            /* repeat for all ',' or ';' separated addresses */
            ea.cmd = cmd;
            for ( ; ; )
            {
                ea.line1 = ea.line2;
                switch (ea.addr_type)
                {
                    case ADDR_LINES:
                        /* default is current line number */
                        ea.line2 = curwin.w_cursor.lnum;
                        break;

                    case ADDR_WINDOWS:
                        lnum = current_win_nr(curwin);
                        ea.line2 = lnum;
                        break;

                    case ADDR_ARGUMENTS:
                        ea.line2 = curwin.w_arg_idx + 1;
                        if (ea.line2 > curwin.w_alist.al_ga.ga_len)
                            ea.line2 = curwin.w_alist.al_ga.ga_len;
                        break;

                    case ADDR_LOADED_BUFFERS:
                    case ADDR_BUFFERS:
                        ea.line2 = curbuf.b_fnum;
                        break;

                    case ADDR_TABS:
                        lnum = current_tab_nr(curtab);
                        ea.line2 = lnum;
                        break;
                }
                ea.cmd = skipwhite(ea.cmd);
                { Bytes[] __ = { ea.cmd }; lnum = get_address(__, ea.addr_type, ea.skip, ea.addr_count == 0); ea.cmd = __[0]; }
                if (ea.cmd == null)                 /* error detected */
                    break doend;
                if (lnum == MAXLNUM)
                {
                    if (ea.cmd.at(0) == (byte)'%')             /* '%' - all lines */
                    {
                        ea.cmd = ea.cmd.plus(1);
                        switch (ea.addr_type)
                        {
                            case ADDR_LINES:
                                ea.line1 = 1;
                                ea.line2 = curbuf.b_ml.ml_line_count;
                                break;

                            case ADDR_LOADED_BUFFERS:
                            {
                                buffer_C buf = firstbuf;

                                while (buf.b_next != null && buf.b_ml.ml_mfp == null)
                                    buf = buf.b_next;
                                ea.line1 = buf.b_fnum;
                                buf = lastbuf;
                                while (buf.b_prev != null && buf.b_ml.ml_mfp == null)
                                    buf = buf.b_prev;
                                ea.line2 = buf.b_fnum;
                                break;
                            }

                            case ADDR_BUFFERS:
                                ea.line1 = firstbuf.b_fnum;
                                ea.line2 = lastbuf.b_fnum;
                                break;

                            case ADDR_WINDOWS:
                            case ADDR_TABS:
                                if (is_user_cmdidx(ea.cmdidx))
                                {
                                    ea.line1 = 1;
                                    ea.line2 = ea.addr_type == ADDR_WINDOWS ? current_win_nr(null) : current_tab_nr(null);
                                }
                                else
                                {
                                    /* There is no Vim command which uses '%' and ADDR_WINDOWS or ADDR_TABS. */
                                    errormsg = e_invrange;
                                    break doend;
                                }
                                break;

                            case ADDR_ARGUMENTS:
                                if (curwin.w_alist.al_ga.ga_len == 0)
                                    ea.line1 = ea.line2 = 0;
                                else
                                {
                                    ea.line1 = 1;
                                    ea.line2 = curwin.w_alist.al_ga.ga_len;
                                }
                                break;
                        }
                        ea.addr_count++;
                    }
                                                    /* '*' - visual area */
                    else if (ea.cmd.at(0) == (byte)'*' && vim_strbyte(p_cpo[0], CPO_STAR) == null)
                    {
                        if (ea.addr_type != ADDR_LINES)
                        {
                            errormsg = e_invrange;
                            break doend;
                        }

                        ea.cmd = ea.cmd.plus(1);
                        if (!ea.skip)
                        {
                            pos_C fp = getmark('<', false);
                            if (check_mark(fp) == false)
                                break doend;
                            ea.line1 = fp.lnum;
                            fp = getmark('>', false);
                            if (check_mark(fp) == false)
                                break doend;
                            ea.line2 = fp.lnum;
                            ea.addr_count++;
                        }
                    }
                }
                else
                    ea.line2 = lnum;
                ea.addr_count++;

                if (ea.cmd.at(0) == (byte)';')
                {
                    if (!ea.skip)
                        curwin.w_cursor.lnum = ea.line2;
                }
                else if (ea.cmd.at(0) != (byte)',')
                    break;
                ea.cmd = ea.cmd.plus(1);
            }

            /* One address given: set start and end lines. */
            if (ea.addr_count == 1)
            {
                ea.line1 = ea.line2;
                    /* ... but only implicit: really no address given */
                if (lnum == MAXLNUM)
                    ea.addr_count = 0;
            }

            /* Don't leave the cursor on an illegal line (caused by ';'). */
            check_cursor_lnum();

            /*
             * 5. Parse the command.
             */

            /*
             * Skip ':' and any white space
             */
            ea.cmd = skipwhite(ea.cmd);
            while (ea.cmd.at(0) == (byte)':')
                ea.cmd = skipwhite(ea.cmd.plus(1));

            /*
             * If we got a line, but no command, then go to the line.
             * If we find a '|' or '\n' we set ea.nextcmd.
             */
            if (ea.cmd.at(0) == NUL || ea.cmd.at(0) == (byte)'"' || (ea.nextcmd = check_nextcmd(ea.cmd)) != null)
            {
                /*
                 * strange vi behaviour:
                 * ":3"         jumps to line 3
                 * ":3|..."     prints line 3
                 * ":|"         prints current line
                 */
                if (ea.skip)    /* skip this if inside :if */
                    break doend;
                if (ea.cmd.at(0) == (byte)'|' || (exmode_active != 0 && ea.line1 != ea.line2))
                {
                    ea.cmdidx = CMD_print;
                    ea.argt = RANGE + COUNT + TRLBAR;
                    if ((errormsg = invalid_range(ea)) == null)
                    {
                        correct_range(ea);
                        ex_print.ex(ea);
                    }
                }
                else if (ea.addr_count != 0)
                {
                    if (curbuf.b_ml.ml_line_count < ea.line2)
                    {
                        /* With '-' in 'cpoptions' a line number past the file is an error,
                         * otherwise put it at the end of the file. */
                        if (vim_strbyte(p_cpo[0], CPO_MINUS) != null)
                            ea.line2 = -1;
                        else
                            ea.line2 = curbuf.b_ml.ml_line_count;
                    }

                    if (ea.line2 < 0)
                        errormsg = e_invrange;
                    else
                    {
                        if (ea.line2 == 0)
                            curwin.w_cursor.lnum = 1;
                        else
                            curwin.w_cursor.lnum = ea.line2;
                        beginline(BL_SOL | BL_FIX);
                    }
                }
                break doend;
            }

            /* If this looks like an undefined user command and there are CmdUndefined
             * autocommands defined, trigger the matching autocommands. */
            if (p != null && ea.cmdidx == CMD_SIZE && !ea.skip && asc_isupper(ea.cmd.at(0)) && has_cmdundefined())
            {
                p = ea.cmd;
                while (asc_isalnum(p.at(0)))
                    p = p.plus(1);
                p = STRNDUP(ea.cmd, BDIFF(p, ea.cmd));
                boolean b = apply_autocmds(EVENT_CMDUNDEFINED, p, p, true, null);
                p = null;
                if (b && !aborting())
                    p = find_command(ea, null);
            }

            if (p == null)
            {
                if (!ea.skip)
                    errormsg = u8("E464: Ambiguous use of user-defined command");
                break doend;
            }
            /* Check for wrong commands. */
            if (p.at(0) == (byte)'!' && ea.cmd.at(1) == 0151 && ea.cmd.at(0) == 78)
            {
                errormsg = uc_fun_cmd();
                break doend;
            }
            if (ea.cmdidx == CMD_SIZE)
            {
                if (!ea.skip)
                {
                    STRCPY(ioBuff, u8("E492: Not an editor command"));
                    if (!sourcing)
                        append_command(cmdlinep[0]);
                    errormsg = ioBuff;
                    did_emsg_syntax = true;
                }
                break doend;
            }

            /* set when Not Implemented */
            boolean ni = (!is_user_cmdidx(ea.cmdidx)
                                && (cmdnames[ea.cmdidx].cmd_func == ex_ni
                                 || cmdnames[ea.cmdidx].cmd_func == ex_script_ni));

            /* forced commands */
            if (p.at(0) == (byte)'!' && ea.cmdidx != CMD_substitute
                    && ea.cmdidx != CMD_smagic && ea.cmdidx != CMD_snomagic)
            {
                p = p.plus(1);
                ea.forceit = true;
            }
            else
                ea.forceit = false;

            /*
             * 6. Parse arguments.
             */
            if (!is_user_cmdidx(ea.cmdidx))
                ea.argt = cmdnames[ea.cmdidx].cmd_argt;

            if (!ea.skip)
            {
                if (sandbox != 0 && (ea.argt & SBOXOK) == 0)
                {
                    /* Command not allowed in sandbox. */
                    errormsg = e_sandbox;
                    break doend;
                }
                if (!curbuf.b_p_ma[0] && (ea.argt & MODIFY) != 0)
                {
                    /* Command not allowed in non-'modifiable' buffer. */
                    errormsg = e_modifiable;
                    break doend;
                }

                if (text_locked() && (ea.argt & CMDWIN) == 0 && !is_user_cmdidx(ea.cmdidx))
                {
                    /* Command not allowed when editing the command line. */
                    if (cmdwin_type != 0)
                        errormsg = e_cmdwin;
                    else
                        errormsg = e_secure;
                    break doend;
                }
                /* Disallow editing another buffer when "curbuf_lock" is set.
                 * Do allow ":edit" (check for argument later).
                 * Do allow ":checktime" (it's postponed). */
                if ((ea.argt & CMDWIN) == 0
                        && ea.cmdidx != CMD_edit
                        && ea.cmdidx != CMD_checktime
                        && !is_user_cmdidx(ea.cmdidx)
                        && curbuf_locked())
                    break doend;

                if (!ni && (ea.argt & RANGE) == 0 && 0 < ea.addr_count)
                {
                    /* no range allowed */
                    errormsg = e_norange;
                    break doend;
                }
            }

            if (!ni && (ea.argt & BANG) == 0 && ea.forceit) /* no <!> allowed */
            {
                errormsg = e_nobang;
                break doend;
            }

            /*
             * Don't complain about the range if it is not used
             * (could happen if line_count is accidentally set to 0).
             */
            if (!ea.skip && !ni)
            {
                /*
                 * If the range is backwards, ask for confirmation and, if given,
                 * swap ea.line1 & ea.line2 so it's forwards again.
                 * When global command is busy, don't ask, will fail below.
                 */
                if (global_busy == 0 && ea.line2 < ea.line1)
                {
                    if (msg_silent == 0)
                    {
                        if (sourcing || exmode_active != 0)
                        {
                            errormsg = u8("E493: Backwards range given");
                            break doend;
                        }
                        if (ask_yesno(u8("Backwards range given, OK to swap"), false) != 'y')
                            break doend;
                    }
                    lnum = ea.line1;
                    ea.line1 = ea.line2;
                    ea.line2 = lnum;
                }
                if ((errormsg = invalid_range(ea)) != null)
                    break doend;
            }

            if ((ea.argt & NOTADR) != 0 && ea.addr_count == 0) /* default is 1, not cursor */
                ea.line2 = 1;

            correct_range(ea);

            /*
             * Skip to start of argument.
             * Don't do this for the ":!" command, because ":!! -l" needs the space.
             */
            if (ea.cmdidx == CMD_bang)
                ea.arg = p;
            else
                ea.arg = skipwhite(p);

            /*
             * Check for "++opt=val" argument.
             * Must be first, allow ":w ++enc=utf8 !cmd"
             */
            if ((ea.argt & ARGOPT) != 0)
                while (ea.arg.at(0) == (byte)'+' && ea.arg.at(1) == (byte)'+')
                    if (getargopt(ea) == false && !ni)
                    {
                        errormsg = e_invarg;
                        break doend;
                    }

            if (ea.cmdidx == CMD_write || ea.cmdidx == CMD_update)
            {
                if (ea.arg.at(0) == (byte)'>')                 /* append */
                {
                    if ((ea.arg = ea.arg.plus(1)).at(0) != (byte)'>')           /* typed wrong */
                    {
                        errormsg = u8("E494: Use w or w>>");
                        break doend;
                    }
                    ea.arg = skipwhite(ea.arg.plus(1));
                    ea.append = true;
                }
                else if (ea.arg.at(0) == (byte)'!' && ea.cmdidx == CMD_write)  /* :w !filter */
                {
                    ea.arg = ea.arg.plus(1);
                    ea.usefilter = true;
                }
            }

            if (ea.cmdidx == CMD_read)
            {
                if (ea.forceit)
                {
                    ea.usefilter = true;            /* :r! filter if ea.forceit */
                    ea.forceit = false;
                }
                else if (ea.arg.at(0) == (byte)'!')            /* :r !filter */
                {
                    ea.arg = ea.arg.plus(1);
                    ea.usefilter = true;
                }
            }

            if (ea.cmdidx == CMD_lshift || ea.cmdidx == CMD_rshift)
            {
                ea.amount = 1;
                while (ea.arg.at(0) == ea.cmd.at(0))              /* count number of '>' or '<' */
                {
                    ea.arg = ea.arg.plus(1);
                    ea.amount++;
                }
                ea.arg = skipwhite(ea.arg);
            }

            /*
             * Check for "+command" argument, before checking for next command.
             * Don't do this for ":read !cmd" and ":write !cmd".
             */
            if ((ea.argt & EDITCMD) != 0 && !ea.usefilter)
            {
                Bytes[] __ = { ea.arg };
                ea.do_ecmd_cmd = getargcmd(__);
                ea.arg = __[0];
            }

            /*
             * Check for '|' to separate commands and '"' to start comments.
             * Don't do this for ":read !cmd" and ":write !cmd".
             */
            if ((ea.argt & TRLBAR) != 0 && !ea.usefilter)
                separate_nextcmd(ea);

            /*
             * Check for <newline> to end a shell command.
             * Also do this for ":read !cmd", ":write !cmd" and ":global".
             * Any others?
             */
            else if (ea.cmdidx == CMD_bang
                    || ea.cmdidx == CMD_global
                    || ea.cmdidx == CMD_vglobal
                    || ea.usefilter)
            {
                for (p = ea.arg; p.at(0) != NUL; p = p.plus(1))
                {
                    /* Remove one backslash before a newline, so that it's possible to
                     * pass a newline to the shell and also a newline that is preceded
                     * with a backslash.  This makes it impossible to end a shell
                     * command in a backslash, but that doesn't appear useful.
                     * Halving the number of backslashes is incompatible with previous
                     * versions. */
                    if (p.at(0) == (byte)'\\' && p.at(1) == (byte)'\n')
                        BCOPY(p, 0, p, 1, strlen(p, 1) + 1);
                    else if (p.at(0) == (byte)'\n')
                    {
                        ea.nextcmd = p.plus(1);
                        p.be(0, NUL);
                        break;
                    }
                }
            }

            if ((ea.argt & DFLALL) != 0 && ea.addr_count == 0)
            {
                ea.line1 = 1;
                switch (ea.addr_type)
                {
                    case ADDR_LINES:
                        ea.line2 = curbuf.b_ml.ml_line_count;
                        break;

                    case ADDR_LOADED_BUFFERS:
                    {
                        buffer_C buf = firstbuf;
                        while (buf.b_next != null && buf.b_ml.ml_mfp == null)
                            buf = buf.b_next;
                        ea.line1 = buf.b_fnum;
                        buf = lastbuf;
                        while (buf.b_prev != null && buf.b_ml.ml_mfp == null)
                            buf = buf.b_prev;
                        ea.line2 = buf.b_fnum;
                        break;
                    }

                    case ADDR_BUFFERS:
                        ea.line1 = firstbuf.b_fnum;
                        ea.line2 = lastbuf.b_fnum;
                        break;

                    case ADDR_WINDOWS:
                        ea.line2 = current_win_nr(null);
                        break;

                    case ADDR_TABS:
                        ea.line2 = current_tab_nr(null);
                        break;

                    case ADDR_ARGUMENTS:
                        if (curwin.w_alist.al_ga.ga_len == 0)
                            ea.line1 = ea.line2 = 0;
                        else
                            ea.line2 = curwin.w_alist.al_ga.ga_len;
                        break;
                }
            }

            /* accept numbered register only when no count allowed (:put) */
            if ((ea.argt & REGSTR) != 0
                    && ea.arg.at(0) != NUL
                    /* Do not allow register = for user commands. */
                    && (!is_user_cmdidx(ea.cmdidx) || ea.arg.at(0) != (byte)'=')
                    && !((ea.argt & COUNT) != 0 && asc_isdigit(ea.arg.at(0))))
            {
                if (valid_yank_reg(ea.arg.at(0), (ea.cmdidx != CMD_put && !is_user_cmdidx(ea.cmdidx))))
                {
                    ea.regname = (ea.arg = ea.arg.plus(1)).at(-1);
                    /* for '=' register: accept the rest of the line as an expression */
                    if (ea.arg.at(-1) == (byte)'=' && ea.arg.at(0) != NUL)
                    {
                        set_expr_line(STRDUP(ea.arg));
                        ea.arg = ea.arg.plus(strlen(ea.arg));
                    }
                    ea.arg = skipwhite(ea.arg);
                }
            }

            /*
             * Check for a count.
             * When accepting a BUFNAME, don't use "123foo" as a count, it's a buffer name.
             */
            if ((ea.argt & COUNT) != 0 && asc_isdigit(ea.arg.at(0))
                && ((ea.argt & BUFNAME) == 0 || (p = skipdigits(ea.arg)).at(0) == NUL || vim_iswhite(p.at(0))))
            {
                long n;
                { Bytes[] __ = { ea.arg }; n = getdigits(__); ea.arg = __[0]; }
                ea.arg = skipwhite(ea.arg);
                if (n <= 0 && !ni && (ea.argt & ZEROR) == 0)
                {
                    errormsg = e_zerocount;
                    break doend;
                }
                if ((ea.argt & NOTADR) != 0)    /* e.g. :buffer 2, :sleep 3 */
                {
                    ea.line2 = n;
                    if (ea.addr_count == 0)
                        ea.addr_count = 1;
                }
                else
                {
                    ea.line1 = ea.line2;
                    ea.line2 += n - 1;
                    ea.addr_count++;
                    /*
                     * Be vi compatible: no error message for out of range.
                     */
                    if (ea.line2 > curbuf.b_ml.ml_line_count)
                        ea.line2 = curbuf.b_ml.ml_line_count;
                }
            }

            /*
             * Check for flags: 'l', 'p' and '#'.
             */
            if ((ea.argt & EXFLAGS) != 0)
                get_flags(ea);
                                                        /* no arguments allowed */
            if (!ni && (ea.argt & EXTRA) == 0 && ea.arg.at(0) != NUL
                    && ea.arg.at(0) != (byte)'"' && (ea.arg.at(0) != (byte)'|' || (ea.argt & TRLBAR) == 0))
            {
                errormsg = e_trailing;
                break doend;
            }

            if (!ni && (ea.argt & NEEDARG) != 0 && ea.arg.at(0) == NUL)
            {
                errormsg = e_argreq;
                break doend;
            }

            /*
             * Skip the command when it's not going to be executed.
             * The commands like :if, :endif, etc. always need to be executed.
             * Also make an exception for commands that handle a trailing command themselves.
             */
            if (ea.skip)
            {
                switch (ea.cmdidx)
                {
                    /* commands that need evaluation */
                    case CMD_while:
                    case CMD_endwhile:
                    case CMD_for:
                    case CMD_endfor:
                    case CMD_if:
                    case CMD_elseif:
                    case CMD_else:
                    case CMD_endif:
                    case CMD_try:
                    case CMD_catch:
                    case CMD_finally:
                    case CMD_endtry:
                    case CMD_function:
                        break;

                    /* Commands that handle '|' themselves.  Check: A command should
                     * either have the TRLBAR flag, appear in this list or appear in
                     * the list at ":help :bar". */
                    case CMD_aboveleft:
                    case CMD_and:
                    case CMD_belowright:
                    case CMD_botright:
                    case CMD_browse:
                    case CMD_call:
                    case CMD_confirm:
                    case CMD_delfunction:
                    case CMD_echo:
                    case CMD_echoerr:
                    case CMD_echomsg:
                    case CMD_echon:
                    case CMD_execute:
                    case CMD_hide:
                    case CMD_keepalt:
                    case CMD_keepjumps:
                    case CMD_keepmarks:
                    case CMD_keeppatterns:
                    case CMD_leftabove:
                    case CMD_let:
                    case CMD_lockmarks:
                    case CMD_match:
                    case CMD_noautocmd:
                    case CMD_return:
                    case CMD_rightbelow:
                    case CMD_silent:
                    case CMD_smagic:
                    case CMD_snomagic:
                    case CMD_substitute:
                    case CMD_syntax:
                    case CMD_tab:
                    case CMD_throw:
                    case CMD_tilde:
                    case CMD_topleft:
                    case CMD_unlet:
                    case CMD_verbose:
                    case CMD_vertical:
                    case CMD_wincmd:
                        break;

                    default:
                        break doend;
                }
            }

            /*
             * Accept buffer name.  Cannot be used at the same time with a buffer number.
             * Don't do this for a user command.
             */
            if ((ea.argt & BUFNAME) != 0 && ea.arg.at(0) != NUL && ea.addr_count == 0 && !is_user_cmdidx(ea.cmdidx))
            {
                /*
                 * :bdelete, :bwipeout and :bunload take several arguments, separated
                 * by spaces: find next space (skipping over escaped characters).
                 * The others take one argument: ignore trailing spaces.
                 */
                if (ea.cmdidx == CMD_bdelete || ea.cmdidx == CMD_bwipeout || ea.cmdidx == CMD_bunload)
                    p = skiptowhite_esc(ea.arg);
                else
                {
                    p = ea.arg.plus(strlen(ea.arg));
                    while (BLT(ea.arg, p) && vim_iswhite(p.at(-1)))
                        p = p.minus(1);
                }
                ea.line2 = buflist_findpat(ea.arg, p, (ea.argt & BUFUNL) != 0, false);
                if (ea.line2 < 0)       /* failed */
                    break doend;
                ea.addr_count = 1;
                ea.arg = skipwhite(p);
            }

            /*
             * 7. Switch on command name.
             *
             * The "ea" structure holds the arguments that can be used.
             */
            ea.cmdlinep = cmdlinep;
            ea.getline = fgetline;
            ea.cookie = cookie;
            ea.cstack = cstack;

            if (is_user_cmdidx(ea.cmdidx))
            {
                /*
                 * Execute a user-defined command.
                 */
                do_ucmd(ea);
            }
            else
            {
                /*
                 * Call the function to execute the command.
                 */
                ea.errmsg = null;
                cmdnames[ea.cmdidx].cmd_func.ex(ea);
                if (ea.errmsg != null)
                    errormsg = ea.errmsg;
            }

            /*
             * If the command just executed called do_cmdline(), any throw or ":return"
             * or ":finish" encountered there must also check the cstack of the still
             * active do_cmdline() that called this do_one_cmd().  Rethrow an uncaught
             * exception, or reanimate a returned function or finished script file and
             * return or finish it again.
             */
            if (need_rethrow)
                do_throw(cstack);
            else if (check_cstack)
            {
                if (source_finished(fgetline, cookie))
                    do_finish(ea, true);
                else if (getline_equal(fgetline, cookie, get_func_line) && current_func_returned())
                    do_return(ea, true, false, null);
            }
            need_rethrow = check_cstack = false;
        }

        if (curwin.w_cursor.lnum == 0)  /* can happen with zero line number */
            curwin.w_cursor.lnum = 1;

        if (errormsg != null && errormsg.at(0) != NUL && !did_emsg)
        {
            if (sourcing)
            {
                if (BNE(errormsg, ioBuff))
                {
                    STRCPY(ioBuff, errormsg);
                    errormsg = ioBuff;
                }
                append_command(cmdlinep[0]);
            }
            emsg(errormsg);
        }

        do_errthrow(cstack,
            (ea.cmdidx != CMD_SIZE && !is_user_cmdidx(ea.cmdidx)) ? cmdnames[ea.cmdidx].cmd_name : null);

        if (0 <= verbose_save)
            p_verbose[0] = verbose_save;

        if (cmdmod.save_ei != null)
        {
            /* Restore 'eventignore' to the value before ":noautocmd". */
            set_string_option_direct(u8("ei"), -1, cmdmod.save_ei, OPT_FREE, SID_NONE);
            cmdmod.save_ei = null;
        }
        COPY_cmdmod(cmdmod, save_cmdmod);

        if (save_msg_silent != -1)
        {
            /* Messages could be enabled for a serious error,
             * need to check if the counters don't become negative. */
            if (!did_emsg || save_msg_silent < msg_silent)
                msg_silent = save_msg_silent;
            emsg_silent -= did_esilent;
            if (emsg_silent < 0)
                emsg_silent = 0;
            /* Restore msg_scroll, it's set by file I/O commands,
             * even when no message is actually displayed. */
            msg_scroll = save_msg_scroll;

            /* "silent reg" or "silent echo x" inside "redir" leaves msg_col
             * somewhere in the line.  Put it back in the first column. */
            if (redirecting())
                msg_col = 0;
        }

        if (did_sandbox)
            --sandbox;

        if (ea.nextcmd != null && ea.nextcmd.at(0) == NUL)       /* not really a next command */
            ea.nextcmd = null;

        --ex_nesting_level;

        return ea.nextcmd;
    }

    /*
     * Check for an Ex command with optional tail.
     * If there is a match advance "pp" to the argument and return true.
     */
    /*private*/ static boolean checkforcmd(Bytes[] pp, Bytes cmd, int len)
        /* pp: start of command */
        /* cmd: name of command */
        /* len: required length */
    {
        int i;

        for (i = 0; cmd.at(i) != NUL; i++)
            if (cmd.at(i) != pp[0].at(i))
                break;
        if (len <= i && !asc_isalpha(pp[0].at(i)))
        {
            pp[0] = skipwhite(pp[0].plus(i));
            return true;
        }

        return false;
    }

    /*
     * Append "cmd" to the error message in ioBuff.
     * Takes care of limiting the length and handling 0xa0,
     * which would be invisible otherwise.
     */
    /*private*/ static void append_command(Bytes cmd)
    {
        STRCAT(ioBuff, u8(": "));

        Bytes d = ioBuff.plus(strlen(ioBuff));
        for (Bytes s = cmd; s.at(0) != NUL && BDIFF(d, ioBuff) < IOSIZE - 7; )
        {
            if (char_u(s.at(0)) == 0xc2 && char_u(s.at(1)) == 0xa0)
            {
                s = s.plus(2);
                STRCPY(d, u8("<a0>"));
                d = d.plus(4);
            }
            else
            {
                int len = us_ptr2len_cc(s);
                BCOPY(d, s, len);
                d = d.plus(len);
                s = s.plus(len);
            }
        }
        d.be(0, NUL);
    }

    /*
     * Find an Ex command by its name, either built-in or user.
     * Start of the name can be found at eap.cmd.
     * Returns pointer to char after the command name.
     * "full" is set to true if the whole command name matched.
     * Returns null for an ambiguous user command.
     */
    /*private*/ static Bytes find_command(exarg_C eap, boolean[] full)
    {
        /*
         * Isolate the command and search for it in the command table.
         * Exceptions:
         * - the 'k' command can directly be followed by any character.
         * - the 's' command can be followed directly by 'c', 'g', 'i', 'I' or 'r'
         *      but :sre[wind] is another command, as are :scr[iptnames],
         *      :scs[cope], :sim[alt], :sig[ns] and :sil[ent].
         * - the "d" command can directly be followed by 'l' or 'p' flag.
         */
        Bytes p = eap.cmd;
        if (p.at(0) == (byte)'k')
        {
            eap.cmdidx = CMD_k;
            p = p.plus(1);
        }
        else if (p.at(0) == (byte)'s'
                && ((p.at(1) == (byte)'c' && p.at(2) != (byte)'s' && p.at(2) != (byte)'r' && p.at(3) != (byte)'i' && p.at(4) != (byte)'p')
                    || p.at(1) == (byte)'g'
                    || (p.at(1) == (byte)'i' && p.at(2) != (byte)'m' && p.at(2) != (byte)'l' && p.at(2) != (byte)'g')
                    || p.at(1) == (byte)'I'
                    || (p.at(1) == (byte)'r' && p.at(2) != (byte)'e')))
        {
            eap.cmdidx = CMD_substitute;
            p = p.plus(1);
        }
        else
        {
            while (asc_isalpha(p.at(0)))
                p = p.plus(1);
            /* for python 3.x support ":py3", ":python3", ":py3file", etc. */
            if (eap.cmd.at(0) == (byte)'p' && eap.cmd.at(1) == (byte)'y')
                while (asc_isalnum(p.at(0)))
                    p = p.plus(1);

            /* check for non-alpha command */
            if (BEQ(p, eap.cmd) && vim_strbyte(u8("@*!=><&~#"), p.at(0)) != null)
                p = p.plus(1);

            int len = BDIFF(p, eap.cmd);

            if (eap.cmd.at(0) == (byte)'d' && (p.at(-1) == (byte)'l' || p.at(-1) == (byte)'p'))
            {
                /* Check for ":dl", ":dell", etc. to ":deletel": that's
                 * :delete with the 'l' flag.  Same for 'p'. */
                int i;
                for (i = 0; i < len; i++)
                    if (eap.cmd.at(i) != u8("delete").at(i))
                        break;
                if (i == len - 1)
                {
                    --len;
                    if (p.at(-1) == (byte)'l')
                        eap.flags |= EXFLAG_LIST;
                    else
                        eap.flags |= EXFLAG_PRINT;
                }
            }

            if (asc_islower(eap.cmd.at(0)))
                eap.cmdidx = cmdidxs[charOrdLow(eap.cmd.at(0))];
            else
                eap.cmdidx = cmdidxs[26];

            for ( ; eap.cmdidx < CMD_SIZE; eap.cmdidx++)
                if (STRNCMP(cmdnames[eap.cmdidx].cmd_name, eap.cmd, len) == 0)
                {
                    if (full != null && cmdnames[eap.cmdidx].cmd_name.at(len) == NUL)
                        full[0] = true;
                    break;
                }

            /* Look for a user defined command as a last resort.
             * Let ":Print" be overruled by a user defined command. */
            if ((eap.cmdidx == CMD_SIZE || eap.cmdidx == CMD_Print) && 'A' <= eap.cmd.at(0) && eap.cmd.at(0) <= 'Z')
            {
                /* User defined commands may contain digits. */
                while (asc_isalnum(p.at(0)))
                    p = p.plus(1);
                p = find_ucmd(eap, p, full, null, null);
            }
            if (BEQ(p, eap.cmd))
                eap.cmdidx = CMD_SIZE;
        }

        return p;
    }

    /*
     * Search for a user command that matches "eap.cmd".
     * Return cmdidx in "eap.cmdidx", flags in "eap.argt", idx in "eap.useridx".
     * Return a pointer to just after the command.
     * Return null if there is no matching command.
     */
    /*private*/ static Bytes find_ucmd(exarg_C eap, Bytes p, boolean[] full, expand_C xp, int[] compl)
        /* p: end of the command (possibly including count) */
        /* full: set to true for a full match */
        /* xp: used for completion, null otherwise */
        /* compl: completion flags or null */
    {
        int len = BDIFF(p, eap.cmd);
        int matchlen = 0;

        boolean found = false;
        boolean possible = false;
        boolean amb_local = false;  /* Found ambiguous buffer-local command,
                                     * only full match global is accepted. */

        /*
         * Look for buffer-local user commands first, then global ones.
         */
        for (Growing<ucmd_C> gap = curbuf.b_ucmds; ; gap = ucmds)
        {
            int j;
            for (j = 0; j < gap.ga_len; j++)
            {
                ucmd_C uc = gap.ga_data[j];
                Bytes cp = eap.cmd;
                Bytes np = uc.uc_name;
                int k = 0;
                while (k < len && np.at(0) != NUL && (cp = cp.plus(1)).at(-1) == (np = np.plus(1)).at(-1))
                    k++;
                if (k == len || (np.at(0) == NUL && asc_isdigit(eap.cmd.at(k))))
                {
                    /* If finding a second match, the command is ambiguous.
                     * But not if a buffer-local command wasn't a full match
                     * and a global command is a full match. */
                    if (k == len && found && np.at(0) != NUL)
                    {
                        if (gap == ucmds)
                            return null;
                        amb_local = true;
                    }

                    if (!found || (k == len && np.at(0) == NUL))
                    {
                        /* If we matched up to a digit, then there could
                         * be another command including the digit that we
                         * should use instead.
                         */
                        if (k == len)
                            found = true;
                        else
                            possible = true;

                        if (gap == ucmds)
                            eap.cmdidx = CMD_USER;
                        else
                            eap.cmdidx = CMD_USER_BUF;
                        eap.argt = uc.uc_argt;
                        eap.useridx = j;
                        eap.addr_type = uc.uc_addr_type;

                        if (compl != null)
                            compl[0] = uc.uc_compl;
                        if (xp != null)
                        {
                            xp.xp_arg = uc.uc_compl_arg;
                            xp.xp_scriptID = uc.uc_scriptID;
                        }
                        /* Do not search for further abbreviations
                         * if this is an exact match. */
                        matchlen = k;
                        if (k == len && np.at(0) == NUL)
                        {
                            if (full != null)
                                full[0] = true;
                            amb_local = false;
                            break;
                        }
                    }
                }
            }

            /* Stop if we found a full match or searched all. */
            if (j < gap.ga_len || gap == ucmds)
                break;
        }

        /* Only found ambiguous matches. */
        if (amb_local)
        {
            if (xp != null)
                xp.xp_context = EXPAND_UNSUCCESSFUL;
            return null;
        }

        /* The match we found may be followed immediately by a number.
         * Move "p" back to point to it. */
        if (found || possible)
            return p.plus(matchlen - len);

        return p;
    }

    /*private*/ static final class cmdmods_C
    {
        Bytes       name;
        int         minlen;
        boolean     has_count;  /* :123verbose  :3tab */

        /*private*/ cmdmods_C(Bytes name, int minlen, boolean has_count)
        {
            this.name = name;
            this.minlen = minlen;
            this.has_count = has_count;
        }
    }

    /*private*/ static cmdmods_C[] cmdmods = new cmdmods_C[]
    {
        new cmdmods_C(u8("aboveleft"),    3, false),
        new cmdmods_C(u8("belowright"),   3, false),
        new cmdmods_C(u8("botright"),     2, false),
        new cmdmods_C(u8("browse"),       3, false),
        new cmdmods_C(u8("confirm"),      4, false),
        new cmdmods_C(u8("hide"),         3, false),
        new cmdmods_C(u8("keepalt"),      5, false),
        new cmdmods_C(u8("keepjumps"),    5, false),
        new cmdmods_C(u8("keepmarks"),    3, false),
        new cmdmods_C(u8("keeppatterns"), 5, false),
        new cmdmods_C(u8("leftabove"),    5, false),
        new cmdmods_C(u8("lockmarks"),    3, false),
        new cmdmods_C(u8("noautocmd"),    3, false),
        new cmdmods_C(u8("rightbelow"),   6, false),
        new cmdmods_C(u8("sandbox"),      3, false),
        new cmdmods_C(u8("silent"),       3, false),
        new cmdmods_C(u8("tab"),          3, true ),
        new cmdmods_C(u8("topleft"),      2, false),
        new cmdmods_C(u8("unsilent"),     3, false),
        new cmdmods_C(u8("verbose"),      4, true ),
        new cmdmods_C(u8("vertical"),     4, false),
    };

    /*
     * Return length of a command modifier (including optional count).
     * Return zero when it's not a modifier.
     */
    /*private*/ static int modifier_len(Bytes cmd)
    {
        Bytes p = cmd;

        if (asc_isdigit(cmd.at(0)))
            p = skipwhite(skipdigits(cmd));
        for (int i = 0; i < cmdmods.length; i++)
        {
            int j;
            for (j = 0; p.at(j) != NUL; j++)
                if (p.at(j) != cmdmods[i].name.at(j))
                    break;
            if (!asc_isalpha(p.at(j)) && cmdmods[i].minlen <= j && (BEQ(p, cmd) || cmdmods[i].has_count))
                return j + BDIFF(p, cmd);
        }
        return 0;
    }

    /*
     * Return > 0 if an Ex command "name" exists.
     * Return 2 if there is an exact match.
     * Return 3 if there is an ambiguous match.
     */
    /*private*/ static int cmd_exists(Bytes name)
    {
        /* Check command modifiers. */
        for (int i = 0; i < cmdmods.length; i++)
        {
            int j;
            for (j = 0; name.at(j) != NUL; j++)
                if (name.at(j) != cmdmods[i].name.at(j))
                    break;
            if (name.at(j) == NUL && cmdmods[i].minlen <= j)
                return (cmdmods[i].name.at(j) == NUL) ? 2 : 1;
        }

        /* Check built-in commands and user defined commands.
         * For ":2match" and ":3match" we need to skip the number. */
        exarg_C ea = new exarg_C();
        ea.cmd = (name.at(0) == (byte)'2' || name.at(0) == (byte)'3') ? name.plus(1) : name;
        ea.cmdidx = 0;
        boolean[] full = { false };
        Bytes p = find_command(ea, full);
        if (p == null)
            return 3;
        if (asc_isdigit(name.at(0)) && ea.cmdidx != CMD_match)
            return 0;
        if (skipwhite(p).at(0) != NUL)
            return 0;       /* trailing garbage */

        return (ea.cmdidx == CMD_SIZE) ? 0 : (full[0] ? 2 : 1);
    }

    /*
     * This is all pretty much copied from do_one_cmd(), with all the extra stuff
     * we don't need/want deleted.  Maybe this could be done better if we didn't
     * repeat all this stuff.  The only problem is that they may not stay
     * perfectly compatible with each other, but then the command line syntax
     * probably won't change that much.
     */
    /*private*/ static Bytes set_one_cmd_context(expand_C xp, Bytes buff)
        /* buff: buffer for command string */
    {
        Bytes retval = null;

        int len = 0;
        boolean forceit = false;
        boolean usefilter = false;          /* filter instead of file name */

        expandInit(xp);
        xp.xp_pattern = buff;
        xp.xp_context = EXPAND_COMMANDS;    /* default until we get past command */

    /*
     * 1. Skip comment lines and leading space, colons or bars.
     */
        Bytes cmd;
        for (cmd = buff; vim_strbyte(u8(" \t:|"), cmd.at(0)) != null; cmd = cmd.plus(1))
            ;
        xp.xp_pattern = cmd;

        if (cmd.at(0) == NUL)
            return null;
        if (cmd.at(0) == (byte)'"')                    /* ignore comment lines */
        {
            xp.xp_context = EXPAND_NOTHING;
            return null;
        }

    /*
     * 3. Skip over the range to find the command.
     */
        { int[] __ = { xp.xp_context }; cmd = skip_range(cmd, __); xp.xp_context = __[0]; }
        xp.xp_pattern = cmd;
        if (cmd.at(0) == NUL)
            return null;
        if (cmd.at(0) == (byte)'"')
        {
            xp.xp_context = EXPAND_NOTHING;
            return null;
        }

        if (cmd.at(0) == (byte)'|' || cmd.at(0) == (byte)'\n')
            return cmd.plus(1);                 /* there's another command */

        exarg_C ea = new exarg_C();
        ea.argt = 0;

        Bytes p;

        /*
         * Isolate the command and search for it in the command table.
         * Exceptions:
         * - the 'k' command can directly be followed by any character,
         *   but do accept "keepmarks", "keepalt" and "keepjumps".
         * - the 's' command can be followed directly by 'c', 'g', 'i', 'I' or 'r'.
         */
        if (cmd.at(0) == (byte)'k' && cmd.at(1) != (byte)'e')
        {
            ea.cmdidx = CMD_k;
            p = cmd.plus(1);
        }
        else
        {
            p = cmd;
            while (asc_isalpha(p.at(0)) || p.at(0) == (byte)'*')        /* allow * wild card */
                p = p.plus(1);
            /* check for non-alpha command */
            if (BEQ(p, cmd) && vim_strbyte(u8("@*!=><&~#"), p.at(0)) != null)
                p = p.plus(1);
            /* for python 3.x: ":py3*" commands completion */
            if (cmd.at(0) == (byte)'p' && cmd.at(1) == (byte)'y' && BEQ(p, cmd.plus(2)) && p.at(0) == (byte)'3')
            {
                p = p.plus(1);
                while (asc_isalpha(p.at(0)) || p.at(0) == (byte)'*')
                    p = p.plus(1);
            }
            len = BDIFF(p, cmd);

            if (len == 0)
            {
                xp.xp_context = EXPAND_UNSUCCESSFUL;
                return retval;
            }
            for (ea.cmdidx = 0; ea.cmdidx < CMD_SIZE; ea.cmdidx++)
                if (STRNCMP(cmdnames[ea.cmdidx].cmd_name, cmd, len) == 0)
                    break;

            if ('A' <= cmd.at(0) && cmd.at(0) <= 'Z')
                while (asc_isalnum(p.at(0)) || p.at(0) == (byte)'*')    /* allow * wild card */
                    p = p.plus(1);
        }

        int[] compl = { EXPAND_NOTHING };

        /*
         * If the cursor is touching the command, and it ends in an alpha-numeric
         * character, complete the command name.
         */
        if (p.at(0) == NUL && asc_isalnum(p.at(-1)))
            return retval;

        if (ea.cmdidx == CMD_SIZE)
        {
            if (cmd.at(0) == (byte)'s' && vim_strbyte(u8("cgriI"), cmd.at(1)) != null)
            {
                ea.cmdidx = CMD_substitute;
                p = cmd.plus(1);
            }
            else if ('A' <= cmd.at(0) && cmd.at(0) <= 'Z')
            {
                ea.cmd = cmd;
                p = find_ucmd(ea, p, null, xp, compl);
                if (p == null)
                    ea.cmdidx = CMD_SIZE;   /* ambiguous user command */
            }
        }
        if (ea.cmdidx == CMD_SIZE)
        {
            /* Not still touching the command and it was an illegal one. */
            xp.xp_context = EXPAND_UNSUCCESSFUL;
            return retval;
        }

        xp.xp_context = EXPAND_NOTHING;     /* default now that we're past command */

        if (p.at(0) == (byte)'!')                      /* forced commands */
        {
            forceit = true;
            p = p.plus(1);
        }

    /*
     * 6. parse arguments
     */
        if (!is_user_cmdidx(ea.cmdidx))
            ea.argt = cmdnames[ea.cmdidx].cmd_argt;

        Bytes arg = skipwhite(p);

        if (ea.cmdidx == CMD_write || ea.cmdidx == CMD_update)
        {
            if (arg.at(0) == (byte)'>')                        /* append */
            {
                if ((arg = arg.plus(1)).at(0) == (byte)'>')
                    arg = arg.plus(1);
                arg = skipwhite(arg);
            }
            else if (arg.at(0) == (byte)'!' && ea.cmdidx == CMD_write) /* :w !filter */
            {
                arg = arg.plus(1);
                usefilter = true;
            }
        }

        if (ea.cmdidx == CMD_read)
        {
            usefilter = forceit;                    /* :r! filter if forced */
            if (arg.at(0) == (byte)'!')                        /* :r !filter */
            {
                arg = arg.plus(1);
                usefilter = true;
            }
        }

        if (ea.cmdidx == CMD_lshift || ea.cmdidx == CMD_rshift)
        {
            while (arg.at(0) == cmd.at(0))        /* allow any number of '>' or '<' */
                arg = arg.plus(1);
            arg = skipwhite(arg);
        }

        /* Does command allow "+command"? */
        if ((ea.argt & EDITCMD) != 0 && !usefilter && arg.at(0) == (byte)'+')
        {
            /* Check if we're in the +command. */
            p = arg.plus(1);
            arg = skip_cmd_arg(arg, false);

            /* Still touching the command after '+'? */
            if (arg.at(0) == NUL)
                return p;

            /* Skip space(s) after +command to get to the real argument. */
            arg = skipwhite(arg);
        }

        /*
         * Check for '|' to separate commands and '"' to start comments.
         * Don't do this for ":read !cmd" and ":write !cmd".
         */
        if ((ea.argt & TRLBAR) != 0 && !usefilter)
        {
            p = arg;
            /* ":redir @" is not the start of a comment */
            if (ea.cmdidx == CMD_redir && p.at(0) == (byte)'@' && p.at(1) == (byte)'"')
                p = p.plus(2);
            while (p.at(0) != NUL)
            {
                if (p.at(0) == Ctrl_V)
                {
                    if (p.at(1) != NUL)
                        p = p.plus(1);
                }
                else if ((p.at(0) == (byte)'"' && (ea.argt & NOTRLCOM) == 0) || p.at(0) == (byte)'|' || p.at(0) == (byte)'\n')
                {
                    if (p.at(-1) != (byte)'\\')
                    {
                        if (p.at(0) == (byte)'|' || p.at(0) == (byte)'\n')
                            retval = p.plus(1);

                        return retval;  /* It's a comment. */
                    }
                }
                p = p.plus(us_ptr2len_cc(p));
            }
        }

        if ((ea.argt & EXTRA) == 0 && arg.at(0) != NUL && vim_strbyte(u8("|\""), arg.at(0)) == null) /* no arguments allowed */
            return retval;

        /* Find start of last argument (argument just before cursor): */
        p = buff;
        xp.xp_pattern = p;
        len = strlen(buff);
        while (p.at(0) != NUL && BLT(p, buff.plus(len)))
        {
            if (p.at(0) == (byte)' ' || p.at(0) == TAB)
            {
                /* argument starts after a space */
                xp.xp_pattern = (p = p.plus(1));
            }
            else
            {
                if (p.at(0) == (byte)'\\' && p.at(1) != NUL)
                    p = p.plus(1); /* skip over escaped character */
                p = p.plus(us_ptr2len_cc(p));
            }
        }

    /*
     * 6. Switch on command name.
     */
        switch (ea.cmdidx)
        {
            /* Command modifiers: return the argument.
             * Also for commands with an argument that is a command. */
            case CMD_aboveleft:
            case CMD_argdo:
            case CMD_belowright:
            case CMD_botright:
            case CMD_browse:
            case CMD_bufdo:
            case CMD_confirm:
            case CMD_debug:
            case CMD_hide:
            case CMD_keepalt:
            case CMD_keepjumps:
            case CMD_keepmarks:
            case CMD_keeppatterns:
            case CMD_leftabove:
            case CMD_lockmarks:
            case CMD_noautocmd:
            case CMD_rightbelow:
            case CMD_sandbox:
            case CMD_silent:
            case CMD_tab:
            case CMD_tabdo:
            case CMD_topleft:
            case CMD_verbose:
            case CMD_vertical:
            case CMD_windo:
                return arg;

            case CMD_match:
                if (arg.at(0) == NUL || !ends_excmd(arg.at(0)))
                {
                    /* also complete "None" */
                    set_context_in_echohl_cmd(xp, arg);
                    arg = skipwhite(skiptowhite(arg));
                    if (arg.at(0) != NUL)
                    {
                        xp.xp_context = EXPAND_NOTHING;
                        arg = skip_regexp(arg.plus(1), arg.at(0), p_magic[0], null);
                    }
                }
                return find_nextcmd(arg);

    /*
     * All completion for the +cmdline_compl feature goes here.
     */

            case CMD_command:
                /* Check for attributes. */
                while (arg.at(0) == (byte)'-')
                {
                    arg = arg.plus(1);      /* Skip "-" */
                    p = skiptowhite(arg);
                    if (p.at(0) == NUL)
                    {
                        /* Cursor is still in the attribute. */
                        p = vim_strchr(arg, '=');
                        if (p == null)
                        {
                            /* No "=", so complete attribute names. */
                            xp.xp_context = EXPAND_USER_CMD_FLAGS;
                            xp.xp_pattern = arg;
                            return retval;
                        }

                        /* For the -complete, -nargs and -addr attributes,
                         * we complete their arguments as well.
                         */
                        if (STRNCASECMP(arg, u8("complete"), BDIFF(p, arg)) == 0)
                        {
                            xp.xp_context = EXPAND_USER_COMPLETE;
                            xp.xp_pattern = p.plus(1);
                            return retval;
                        }
                        else if (STRNCASECMP(arg, u8("nargs"), BDIFF(p, arg)) == 0)
                        {
                            xp.xp_context = EXPAND_USER_NARGS;
                            xp.xp_pattern = p.plus(1);
                            return retval;
                        }
                        else if (STRNCASECMP(arg, u8("addr"), BDIFF(p, arg)) == 0)
                        {
                            xp.xp_context = EXPAND_USER_ADDR_TYPE;
                            xp.xp_pattern = p.plus(1);
                            return retval;
                        }
                        return retval;
                    }
                    arg = skipwhite(p);
                }

                /* After the attributes comes the new command name. */
                p = skiptowhite(arg);
                if (p.at(0) == NUL)
                {
                    xp.xp_context = EXPAND_USER_COMMANDS;
                    xp.xp_pattern = arg;
                    break;
                }

                /* And finally comes a normal command. */
                return skipwhite(p);

            case CMD_delcommand:
                xp.xp_context = EXPAND_USER_COMMANDS;
                xp.xp_pattern = arg;
                break;

            case CMD_global:
            case CMD_vglobal:
            {
                byte delim = arg.at(0);
                if (delim != NUL)
                    arg = arg.plus(1);
                while (arg.at(0) != NUL && arg.at(0) != delim)
                {
                    if (arg.at(0) == (byte)'\\' && arg.at(1) != NUL)
                        arg = arg.plus(1);
                    arg = arg.plus(1);
                }
                if (arg.at(0) != NUL)
                    return arg.plus(1);
                break;
            }

            case CMD_and:
            case CMD_substitute:
            {
                byte delim = arg.at(0);
                if (delim != NUL)
                {
                    /* skip "from" part */
                    arg = arg.plus(1);
                    arg = skip_regexp(arg, delim, p_magic[0], null);
                }
                /* skip "to" part */
                while (arg.at(0) != NUL && arg.at(0) != delim)
                {
                    if (arg.at(0) == (byte)'\\' && arg.at(1) != NUL)
                        arg = arg.plus(1);
                    arg = arg.plus(1);
                }
                if (arg.at(0) != NUL)  /* skip delimiter */
                    arg = arg.plus(1);
                while (arg.at(0) != NUL && vim_strbyte(u8("|\"#"), arg.at(0)) == null)
                    arg = arg.plus(1);
                if (arg.at(0) != NUL)
                    return arg;
                break;
            }

            case CMD_autocmd:
            case CMD_doautocmd:
            case CMD_doautoall:
                return set_context_in_autocmd(xp, arg);

            case CMD_set:
                set_context_in_set_cmd(xp, arg, 0);
                break;

            case CMD_setglobal:
                set_context_in_set_cmd(xp, arg, OPT_GLOBAL);
                break;

            case CMD_setlocal:
                set_context_in_set_cmd(xp, arg, OPT_LOCAL);
                break;

            case CMD_augroup:
                xp.xp_context = EXPAND_AUGROUP;
                xp.xp_pattern = arg;
                break;

            case CMD_syntax:
                set_context_in_syntax_cmd(xp, arg);
                break;

            case CMD_let:
            case CMD_if:
            case CMD_elseif:
            case CMD_while:
            case CMD_for:
            case CMD_echo:
            case CMD_echon:
            case CMD_execute:
            case CMD_echomsg:
            case CMD_echoerr:
            case CMD_call:
            case CMD_return:
                set_context_for_expression(xp, arg, ea.cmdidx);
                break;

            case CMD_unlet:
                while ((xp.xp_pattern = vim_strchr(arg, ' ')) != null)
                    arg = xp.xp_pattern.plus(1);
                xp.xp_context = EXPAND_USER_VARS;
                xp.xp_pattern = arg;
                break;

            case CMD_function:
            case CMD_delfunction:
                xp.xp_context = EXPAND_USER_FUNC;
                xp.xp_pattern = arg;
                break;

            case CMD_echohl:
                set_context_in_echohl_cmd(xp, arg);
                break;

            case CMD_highlight:
                set_context_in_highlight_cmd(xp, arg);
                break;

            case CMD_bdelete:
            case CMD_bwipeout:
            case CMD_bunload:
                while ((xp.xp_pattern = vim_strchr(arg, ' ')) != null)
                    arg = xp.xp_pattern.plus(1);
                /* FALLTHROUGH */

            case CMD_buffer:
            case CMD_sbuffer:
            case CMD_checktime:
                xp.xp_context = EXPAND_BUFFERS;
                xp.xp_pattern = arg;
                break;

            case CMD_USER:
            case CMD_USER_BUF:
                if (compl[0] != EXPAND_NOTHING)
                {
                    /* XFILE: file names are handled above. */
                    if ((ea.argt & XFILE) == 0)
                    {
                        if (compl[0] == EXPAND_COMMANDS)
                            return arg;
                        if (compl[0] == EXPAND_MAPPINGS)
                            return set_context_in_map_cmd(xp, u8("map"), arg, forceit, false, false, CMD_map);

                        /* Find start of last argument. */
                        p = arg;
                        while (p.at(0) != NUL)
                        {
                            if (p.at(0) == (byte)' ')
                                /* argument starts after a space */
                                arg = p.plus(1);
                            else if (p.at(0) == (byte)'\\' && p.at(1) != NUL)
                                p = p.plus(1); /* skip over escaped character */
                            p = p.plus(us_ptr2len_cc(p));
                        }
                        xp.xp_pattern = arg;
                    }
                    xp.xp_context = compl[0];
                }
                break;

            case CMD_map:       case CMD_noremap:
            case CMD_nmap:      case CMD_nnoremap:
            case CMD_vmap:      case CMD_vnoremap:
            case CMD_omap:      case CMD_onoremap:
            case CMD_imap:      case CMD_inoremap:
            case CMD_cmap:      case CMD_cnoremap:
            case CMD_lmap:      case CMD_lnoremap:
            case CMD_smap:      case CMD_snoremap:
            case CMD_xmap:      case CMD_xnoremap:
                return set_context_in_map_cmd(xp, cmd, arg, forceit, false, false, ea.cmdidx);

            case CMD_unmap:
            case CMD_nunmap:
            case CMD_vunmap:
            case CMD_ounmap:
            case CMD_iunmap:
            case CMD_cunmap:
            case CMD_lunmap:
            case CMD_sunmap:
            case CMD_xunmap:
                return set_context_in_map_cmd(xp, cmd, arg, forceit, false, true, ea.cmdidx);

            case CMD_abbreviate:    case CMD_noreabbrev:
            case CMD_cabbrev:       case CMD_cnoreabbrev:
            case CMD_iabbrev:       case CMD_inoreabbrev:
                return set_context_in_map_cmd(xp, cmd, arg, forceit, true, false, ea.cmdidx);

            case CMD_unabbreviate:
            case CMD_cunabbrev:
            case CMD_iunabbrev:
                return set_context_in_map_cmd(xp, cmd, arg, forceit, true, true, ea.cmdidx);

            case CMD_history:
                xp.xp_context = EXPAND_HISTORY;
                xp.xp_pattern = arg;
                break;

            default:
                break;
        }

        return retval;
    }

    /*
     * skip a range specifier of the form: addr [,addr] [;addr] ..
     *
     * Backslashed delimiters after / or ? will be skipped, and commands will
     * not be expanded between /'s and ?'s or after "'".
     *
     * Also skip white space and ":" characters.
     * Returns the "cmd" pointer advanced to beyond the range.
     */
    /*private*/ static Bytes skip_range(Bytes cmd, int[] ctx)
        /* ctx: pointer to xp_context or null */
    {
        while (vim_strbyte(u8(" \t0123456789.$%'/?-+,;"), cmd.at(0)) != null)
        {
            if (cmd.at(0) == (byte)'\'')
            {
                if ((cmd = cmd.plus(1)).at(0) == NUL && ctx != null)
                    ctx[0] = EXPAND_NOTHING;
            }
            else if (cmd.at(0) == (byte)'/' || cmd.at(0) == (byte)'?')
            {
                byte delim = (cmd = cmd.plus(1)).at(-1);
                while (cmd.at(0) != NUL && cmd.at(0) != delim)
                    if ((cmd = cmd.plus(1)).at(-1) == (byte)'\\' && cmd.at(0) != NUL)
                        cmd = cmd.plus(1);
                if (cmd.at(0) == NUL && ctx != null)
                    ctx[0] = EXPAND_NOTHING;
            }
            if (cmd.at(0) != NUL)
                cmd = cmd.plus(1);
        }

        /* Skip ":" and white space. */
        while (cmd.at(0) == (byte)':')
            cmd = skipwhite(cmd.plus(1));

        return cmd;
    }

    /*
     * get a single EX address
     *
     * Set ptr to the next character after the part that was interpreted.
     * Set ptr to null when an error is encountered.
     *
     * Return MAXLNUM when no Ex address was found.
     */
    /*private*/ static long get_address(Bytes[] ptr, int addr_type, boolean skip, boolean to_other_file)
        /* addr_type: flag: one of ADDR_LINES, ... */
        /* skip: only skip the address, don't use it */
        /* to_other_file: flag: may jump to other file */
    {
        long lnum = MAXLNUM;
        Bytes cmd = skipwhite(ptr[0]);

        error:
        do
        {
            switch (cmd.at(0))
            {
                case '.':                       /* '.' - Cursor position */
                {
                    cmd = cmd.plus(1);
                    switch (addr_type)
                    {
                        case ADDR_LINES:
                            lnum = curwin.w_cursor.lnum;
                            break;

                        case ADDR_WINDOWS:
                            lnum = current_win_nr(curwin);
                            break;

                        case ADDR_ARGUMENTS:
                            lnum = curwin.w_arg_idx + 1;
                            break;

                        case ADDR_LOADED_BUFFERS:
                        case ADDR_BUFFERS:
                            lnum = curbuf.b_fnum;
                            break;

                        case ADDR_TABS:
                            lnum = current_tab_nr(curtab);
                            break;
                    }
                    break;
                }

                case '$':                       /* '$' - last line */
                {
                    cmd = cmd.plus(1);
                    switch (addr_type)
                    {
                        case ADDR_LINES:
                            lnum = curbuf.b_ml.ml_line_count;
                            break;

                        case ADDR_WINDOWS:
                            lnum = current_win_nr(null);
                            break;

                        case ADDR_ARGUMENTS:
                            lnum = curwin.w_alist.al_ga.ga_len;
                            break;

                        case ADDR_LOADED_BUFFERS:
                        {
                            buffer_C buf = lastbuf;
                            while (buf.b_ml.ml_mfp == null)
                            {
                                if (buf.b_prev == null)
                                    break;
                                buf = buf.b_prev;
                            }
                            lnum = buf.b_fnum;
                            break;
                        }

                        case ADDR_BUFFERS:
                            lnum = lastbuf.b_fnum;
                            break;

                        case ADDR_TABS:
                            lnum = current_tab_nr(null);
                            break;
                    }
                    break;
                }

                case '\'':                      /* ''' - mark */
                {
                    if ((cmd = cmd.plus(1)).at(0) == NUL)
                    {
                        cmd = null;
                        break error;
                    }
                    if (addr_type != ADDR_LINES)
                    {
                        emsg(e_invaddr);
                        cmd = null;
                        break error;
                    }
                    if (skip)
                        cmd = cmd.plus(1);
                    else
                    {
                        /* Only accept a mark in another file when it is used by itself: ":'M". */
                        pos_C fp = getmark(cmd.at(0), to_other_file && cmd.at(1) == NUL);
                        cmd = cmd.plus(1);
                        if (fp == NOPOS)
                            /* Jumped to another file. */
                            lnum = curwin.w_cursor.lnum;
                        else
                        {
                            if (check_mark(fp) == false)
                            {
                                cmd = null;
                                break error;
                            }
                            lnum = fp.lnum;
                        }
                    }
                    break;
                }

                case '/':
                case '?':                   /* '/' or '?' - search */
                {
                    byte c = (cmd = cmd.plus(1)).at(-1);
                    if (addr_type != ADDR_LINES)
                    {
                        emsg(e_invaddr);
                        cmd = null;
                        break error;
                    }
                    if (skip)       /* skip "/pat/" */
                    {
                        cmd = skip_regexp(cmd, c, p_magic[0], null);
                        if (cmd.at(0) == c)
                            cmd = cmd.plus(1);
                    }
                    else
                    {
                        pos_C save_pos = new pos_C();
                        COPY_pos(save_pos, curwin.w_cursor); /* save curwin.w_cursor */
                        /*
                         * When '/' or '?' follows another address, start from there.
                         */
                        if (lnum != MAXLNUM)
                            curwin.w_cursor.lnum = lnum;
                        /*
                         * Start a forward search at the end of the line.
                         * Start a backward search at the start of the line.
                         * This makes sure we never match in the current line,
                         * and can match anywhere in the next/previous line.
                         */
                        curwin.w_cursor.col = (c == '/') ? MAXCOL : 0;
                        searchcmdlen = 0;
                        if (do_search(null, c, cmd, 1L, SEARCH_HIS | SEARCH_MSG, null) == 0)
                        {
                            COPY_pos(curwin.w_cursor, save_pos);
                            cmd = null;
                            break error;
                        }
                        lnum = curwin.w_cursor.lnum;
                        COPY_pos(curwin.w_cursor, save_pos);
                        /* adjust command string pointer */
                        cmd = cmd.plus(searchcmdlen);
                    }
                    break;
                }

                case '\\':              /* "\?", "\/" or "\&", repeat search */
                {
                    cmd = cmd.plus(1);
                    if (addr_type != ADDR_LINES)
                    {
                        emsg(e_invaddr);
                        cmd = null;
                        break error;
                    }
                    int i;
                    if (cmd.at(0) == (byte)'&')
                        i = RE_SUBST;
                    else if (cmd.at(0) == (byte)'?' || cmd.at(0) == (byte)'/')
                        i = RE_SEARCH;
                    else
                    {
                        emsg(e_backslash);
                        cmd = null;
                        break error;
                    }

                    if (!skip)
                    {
                        pos_C pos = new pos_C();
                        /*
                         * When search follows another address, start from there.
                         */
                        pos.lnum = (lnum != MAXLNUM) ? lnum : curwin.w_cursor.lnum;
                        /*
                         * Start the search just like for the above do_search().
                         */
                        pos.col = (cmd.at(0) != (byte)'?') ? MAXCOL : 0;

                        if (searchit(curwin, curbuf, pos,
                                (cmd.at(0) == (byte)'?') ? BACKWARD : FORWARD, u8(""), 1L, SEARCH_MSG, i, 0, null) != 0)
                            lnum = pos.lnum;
                        else
                        {
                            cmd = null;
                            break error;
                        }
                    }
                    cmd = cmd.plus(1);
                    break;
                }

                default:
                {
                    if (asc_isdigit(cmd.at(0)))  /* absolute line number */
                    {
                        Bytes[] __ = { cmd }; lnum = getdigits(__); cmd = __[0];
                    }
                    break;
                }
            }

            for ( ; ; )
            {
                cmd = skipwhite(cmd);
                if (cmd.at(0) != (byte)'-' && cmd.at(0) != (byte)'+' && !asc_isdigit(cmd.at(0)))
                    break;

                if (lnum == MAXLNUM)
                {
                    switch (addr_type)
                    {
                        case ADDR_LINES:
                            /* "+1" is same as ".+1" */
                            lnum = curwin.w_cursor.lnum;
                            break;

                        case ADDR_WINDOWS:
                            lnum = current_win_nr(curwin);
                            break;

                        case ADDR_ARGUMENTS:
                            lnum = curwin.w_arg_idx + 1;
                            break;

                        case ADDR_LOADED_BUFFERS:
                        case ADDR_BUFFERS:
                            lnum = curbuf.b_fnum;
                            break;

                        case ADDR_TABS:
                            lnum = current_tab_nr(curtab);
                            break;
                    }
                }

                int m;
                if (asc_isdigit(cmd.at(0)))
                    m = '+';                /* "number" is same as "+number" */
                else
                    m = (cmd = cmd.plus(1)).at(-1);
                int n;
                if (!asc_isdigit(cmd.at(0)))     /* '+' is '+1', but '+0' is not '+1' */
                    n = 1;
                else
                {
                    Bytes[] __ = { cmd }; n = (int)getdigits(__); cmd = __[0];
                }
                if (addr_type == ADDR_LOADED_BUFFERS || addr_type == ADDR_BUFFERS)
                    lnum = compute_buffer_local_count(addr_type, (int)lnum, (m == '-') ? -n : n);
                else if (m == '-')
                    lnum -= n;
                else
                    lnum += n;
            }
        } while (cmd.at(0) == (byte)'/' || cmd.at(0) == (byte)'?');

        ptr[0] = cmd;
        return lnum;
    }

    /*
     * Get flags from an Ex command argument.
     */
    /*private*/ static void get_flags(exarg_C eap)
    {
        while (vim_strbyte(u8("lp#"), eap.arg.at(0)) != null)
        {
            if (eap.arg.at(0) == (byte)'l')
                eap.flags |= EXFLAG_LIST;
            else if (eap.arg.at(0) == (byte)'p')
                eap.flags |= EXFLAG_PRINT;
            else
                eap.flags |= EXFLAG_NR;
            eap.arg = skipwhite(eap.arg.plus(1));
        }
    }

    /*
     * Check range in Ex command for validity.
     * Return null when valid, error message when invalid.
     */
    /*private*/ static Bytes invalid_range(exarg_C eap)
    {
        if (eap.line1 < 0 || eap.line2 < 0 || eap.line2 < eap.line1)
            return e_invrange;

        if ((eap.argt & RANGE) != 0)
        {
            switch (eap.addr_type)
            {
                case ADDR_LINES:
                    if ((eap.argt & NOTADR) == 0 && curbuf.b_ml.ml_line_count < eap.line2)
                        return e_invrange;
                    break;

                case ADDR_ARGUMENTS:
                    /* add 1 if ARGCOUNT is 0 */
                    if (curwin.w_alist.al_ga.ga_len + ((curwin.w_alist.al_ga.ga_len == 0) ? 1 : 0) < eap.line2)
                        return e_invrange;
                    break;

                case ADDR_BUFFERS:
                    if (eap.line1 < firstbuf.b_fnum || lastbuf.b_fnum < eap.line2)
                        return e_invrange;
                    break;

                case ADDR_LOADED_BUFFERS:
                {
                    buffer_C buf = firstbuf;
                    while (buf.b_ml.ml_mfp == null)
                    {
                        if (buf.b_next == null)
                            return e_invrange;
                        buf = buf.b_next;
                    }
                    if (eap.line1 < buf.b_fnum)
                        return e_invrange;
                    buf = lastbuf;
                    while (buf.b_ml.ml_mfp == null)
                    {
                        if (buf.b_prev == null)
                            return e_invrange;
                        buf = buf.b_prev;
                    }
                    if (buf.b_fnum < eap.line2)
                        return e_invrange;
                    break;
                }

                case ADDR_WINDOWS:
                    if (current_win_nr(null) < eap.line2)
                        return e_invrange;
                    break;

                case ADDR_TABS:
                    if (current_tab_nr(null) < eap.line2)
                        return e_invrange;
                    break;
            }
        }

        return null;
    }

    /*
     * Correct the range for zero line number, if required.
     */
    /*private*/ static void correct_range(exarg_C eap)
    {
        if ((eap.argt & ZEROR) == 0)    /* zero in range not allowed */
        {
            if (eap.line1 == 0)
                eap.line1 = 1;
            if (eap.line2 == 0)
                eap.line2 = 1;
        }
    }

    /*
     * Check for '|' to separate commands and '"' to start comments.
     */
    /*private*/ static void separate_nextcmd(exarg_C eap)
    {
        for (Bytes p = eap.arg; p.at(0) != NUL; p = p.plus(us_ptr2len_cc(p)))
        {
            if (p.at(0) == Ctrl_V)
            {
                if ((eap.argt & (USECTRLV | XFILE)) != 0)
                    p = p.plus(1);                                    /* skip CTRL-V and next char */
                else
                                                            /* remove CTRL-V and skip next char */
                    BCOPY(p, 0, p, 1, strlen(p, 1) + 1);
                if (p.at(0) == NUL)                              /* stop at NUL after CTRL-V */
                    break;
            }

            /* Skip over `=expr` when wildcards are expanded. */
            else if (p.at(0) == (byte)'`' && p.at(1) == (byte)'=' && (eap.argt & XFILE) != 0)
            {
                p = p.plus(2);
                { Bytes[] __ = { p }; skip_expr(__); p = __[0]; }
            }

            /* Check for '"': start of comment or '|': next command.
             * :@" and :*" do not start a comment!
             * :redir @" doesn't either. */
            else if ((p.at(0) == (byte)'"' && (eap.argt & NOTRLCOM) == 0
                        && ((eap.cmdidx != CMD_at && eap.cmdidx != CMD_star) || BNE(p, eap.arg))
                        && (eap.cmdidx != CMD_redir || BNE(p, eap.arg.plus(1)) || p.at(-1) != (byte)'@'))
                    || p.at(0) == (byte)'|' || p.at(0) == (byte)'\n')
            {
                /*
                 * We remove the '\' before the '|', unless USECTRLV is used AND 'b' is present in 'cpoptions'.
                 */
                if ((vim_strbyte(p_cpo[0], CPO_BAR) == null || (eap.argt & USECTRLV) == 0) && p.at(-1) == (byte)'\\')
                {
                    BCOPY(p, -1, p, 0, strlen(p) + 1);       /* remove the '\' */
                    p = p.minus(1);
                }
                else
                {
                    eap.nextcmd = check_nextcmd(p);
                    p.be(0, NUL);
                    break;
                }
            }
        }

        if ((eap.argt & NOTRLCOM) == 0)                     /* remove trailing spaces */
            del_trailing_spaces(eap.arg);
    }

    /*
     * get + command from ex argument
     */
    /*private*/ static Bytes getargcmd(Bytes[] argp)
    {
        Bytes arg = argp[0];
        Bytes command = null;

        if (arg.at(0) == (byte)'+')                                /* +[command] */
        {
            arg = arg.plus(1);
            if (vim_isspace(arg.at(0)) || arg.at(0) == NUL)
                command = dollar_command;
            else
            {
                command = arg;
                arg = skip_cmd_arg(command, true);
                if (arg.at(0) != NUL)
                    (arg = arg.plus(1)).be(-1, NUL);                       /* terminate command with NUL */
            }

            arg = skipwhite(arg);                       /* skip over spaces */
            argp[0] = arg;
        }
        return command;
    }

    /*
     * Find end of "+command" argument.  Skip over "\ " and "\\".
     */
    /*private*/ static Bytes skip_cmd_arg(Bytes p, boolean rembs)
        /* rembs: true to halve the number of backslashes */
    {
        while (p.at(0) != NUL && !vim_isspace(p.at(0)))
        {
            if (p.at(0) == (byte)'\\' && p.at(1) != NUL)
            {
                if (rembs)
                    BCOPY(p, 0, p, 1, strlen(p, 1) + 1);
                else
                    p = p.plus(1);
            }
            p = p.plus(us_ptr2len_cc(p));
        }
        return p;
    }

    /*
     * Get "++opt=arg" argument.
     * Return false or true.
     */
    /*private*/ static boolean getargopt(exarg_C eap)
    {
        Bytes arg = eap.arg.plus(2);

        /* ":edit ++[no]bin[ary] file" */
        if (STRNCMP(arg, u8("bin"), 3) == 0 || STRNCMP(arg, u8("nobin"), 5) == 0)
        {
            if (arg.at(0) == (byte)'n')
            {
                arg = arg.plus(2);
                eap.force_bin = FORCE_NOBIN;
            }
            else
                eap.force_bin = FORCE_BIN;
            Bytes[] p = { arg };
            if (!checkforcmd(p, u8("binary"), 3))
                return false;
            eap.arg = skipwhite(p[0]);
            return true;
        }

        /* ":read ++edit file" */
        if (STRNCMP(arg, u8("edit"), 4) == 0)
        {
            eap.read_edit = true;
            eap.arg = skipwhite(arg.plus(4));
            return true;
        }

        int[] bad_char_idx = new int[1];
        int[] pp = null;

        if (STRNCMP(arg, u8("bad"), 3) == 0)
        {
            arg = arg.plus(3);
            pp = bad_char_idx;
        }

        if (pp == null || arg.at(0) != (byte)'=')
            return false;

        arg = arg.plus(1);
        pp[0] = BDIFF(arg, eap.cmd);
        arg = skip_cmd_arg(arg, false);
        eap.arg = skipwhite(arg);
        arg.be(0, NUL);

        /* Check ++bad= argument.  Must be a single-byte character, "keep" or "drop". */
        Bytes p = eap.cmd.plus(bad_char_idx[0]);
        if (STRCASECMP(p, u8("keep")) == 0)
            eap.bad_char = BAD_KEEP;
        else if (STRCASECMP(p, u8("drop")) == 0)
            eap.bad_char = BAD_DROP;
        else if (us_byte2len(p.at(0), false) == 1 && p.at(1) == NUL)
            eap.bad_char = p.at(0);
        else
            return false;

        return true;
    }

    /*
     * ":abbreviate" and friends.
     */
    /*private*/ static final ex_func_C ex_abbreviate = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            do_exmap(eap, true);        /* almost the same as mapping */
        }
    };

    /*
     * ":map" and friends.
     */
    /*private*/ static final ex_func_C ex_map = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            /*
             * If we are sourcing .exrc or .vimrc in current directory
             * we print the mappings for security reasons.
             */
            if (secure != 0)
            {
                secure = 2;
                msg_outtrans(eap.cmd);
                msg_putchar('\n');
            }
            do_exmap(eap, false);
        }
    };

    /*
     * ":unmap" and friends.
     */
    /*private*/ static final ex_func_C ex_unmap = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            do_exmap(eap, false);
        }
    };

    /*
     * ":mapclear" and friends.
     */
    /*private*/ static final ex_func_C ex_mapclear = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            map_clear(eap.cmd, eap.arg, eap.forceit, false);
        }
    };

    /*
     * ":abclear" and friends.
     */
    /*private*/ static final ex_func_C ex_abclear = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            map_clear(eap.cmd, eap.arg, true, true);
        }
    };

    /*private*/ static final ex_func_C ex_autocmd = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            /*
             * Disallow auto commands from .exrc and .vimrc in current directory for security reasons.
             */
            if (secure != 0)
            {
                secure = 2;
                eap.errmsg = e_curdir;
            }
            else if (eap.cmdidx == CMD_autocmd)
                do_autocmd(eap.arg, eap.forceit);
            else
                do_augroup(eap.arg, eap.forceit);
        }
    };

    /*
     * ":doautocmd": Apply the automatic commands to the current buffer.
     */
    /*private*/ static final ex_func_C ex_doautocmd = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            do_doautocmd(eap.arg, true);
        }
    };

    /*
     * :[N]bunload[!] [N] [bufname] unload buffer
     * :[N]bdelete[!] [N] [bufname] delete buffer from buffer list
     * :[N]bwipeout[!] [N] [bufname] delete buffer really
     */
    /*private*/ static final ex_func_C ex_bunload = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            eap.errmsg = do_bufdel(
                    (eap.cmdidx == CMD_bdelete) ? DOBUF_DEL
                        : (eap.cmdidx == CMD_bwipeout) ? DOBUF_WIPE
                        : DOBUF_UNLOAD, eap.arg,
                    eap.addr_count, (int)eap.line1, (int)eap.line2, eap.forceit);
        }
    };

    /*
     * :[N]buffer [N]       to buffer N
     * :[N]sbuffer [N]      to buffer N
     */
    /*private*/ static final ex_func_C ex_buffer = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.arg.at(0) != NUL)
                eap.errmsg = e_trailing;
            else
            {
                if (eap.addr_count == 0)    /* default is current buffer */
                    goto_buffer(eap, DOBUF_CURRENT, FORWARD, 0);
                else
                    goto_buffer(eap, DOBUF_FIRST, FORWARD, (int)eap.line2);
                if (eap.do_ecmd_cmd != null)
                    do_cmdline_cmd(eap.do_ecmd_cmd);
            }
        }
    };

    /*
     * :[N]bmodified [N]    to next mod. buffer
     * :[N]sbmodified [N]   to next mod. buffer
     */
    /*private*/ static final ex_func_C ex_bmodified = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            goto_buffer(eap, DOBUF_MOD, FORWARD, (int)eap.line2);
            if (eap.do_ecmd_cmd != null)
                do_cmdline_cmd(eap.do_ecmd_cmd);
        }
    };

    /*
     * :[N]bnext [N]        to next buffer
     * :[N]sbnext [N]       split and to next buffer
     */
    /*private*/ static final ex_func_C ex_bnext = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            goto_buffer(eap, DOBUF_CURRENT, FORWARD, (int)eap.line2);
            if (eap.do_ecmd_cmd != null)
                do_cmdline_cmd(eap.do_ecmd_cmd);
        }
    };

    /*
     * :[N]bNext [N]        to previous buffer
     * :[N]bprevious [N]    to previous buffer
     * :[N]sbNext [N]       split and to previous buffer
     * :[N]sbprevious [N]   split and to previous buffer
     */
    /*private*/ static final ex_func_C ex_bprevious = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            goto_buffer(eap, DOBUF_CURRENT, BACKWARD, (int)eap.line2);
            if (eap.do_ecmd_cmd != null)
                do_cmdline_cmd(eap.do_ecmd_cmd);
        }
    };

    /*
     * :brewind             to first buffer
     * :bfirst              to first buffer
     * :sbrewind            split and to first buffer
     * :sbfirst             split and to first buffer
     */
    /*private*/ static final ex_func_C ex_brewind = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            goto_buffer(eap, DOBUF_FIRST, FORWARD, 0);
            if (eap.do_ecmd_cmd != null)
                do_cmdline_cmd(eap.do_ecmd_cmd);
        }
    };

    /*
     * :blast               to last buffer
     * :sblast              split and to last buffer
     */
    /*private*/ static final ex_func_C ex_blast = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            goto_buffer(eap, DOBUF_LAST, BACKWARD, 0);
            if (eap.do_ecmd_cmd != null)
                do_cmdline_cmd(eap.do_ecmd_cmd);
        }
    };

    /*private*/ static boolean ends_excmd(int c)
    {
        return (c == NUL || c == '|' || c == '"' || c == '\n');
    }

    /*
     * Return the next command, after the first '|' or '\n'.
     * Return null if not found.
     */
    /*private*/ static Bytes find_nextcmd(Bytes p)
    {
        while (p.at(0) != (byte)'|' && p.at(0) != (byte)'\n')
        {
            if (p.at(0) == NUL)
                return null;
            p = p.plus(1);
        }
        return p.plus(1);
    }

    /*
     * Check if *p is a separator between Ex commands.
     * Return null if it isn't, (p + 1) if it is.
     */
    /*private*/ static Bytes check_nextcmd(Bytes p)
    {
        p = skipwhite(p);
        if (p.at(0) == (byte)'|' || p.at(0) == (byte)'\n')
            return p.plus(1);
        else
            return null;
    }

    /*
     * - if there are more files to edit
     * - and this is the last window
     * - and forceit not used
     * - and not repeated twice on a row
     *    return false and give error message if 'message' true
     * return true otherwise
     */
    /*private*/ static boolean check_more(boolean message, boolean forceit)
        /* message: when false check only, no messages */
    {
        int n = curwin.w_alist.al_ga.ga_len - curwin.w_arg_idx - 1;

        if (!forceit && only_one_window() && 1 < curwin.w_alist.al_ga.ga_len && !arg_had_last && 0 <= n && quitmore == 0)
        {
            if (message)
            {
                if ((p_confirm[0] || cmdmod.confirm) && curbuf.b_fname != null)
                {
                    Bytes buff = new Bytes(DIALOG_MSG_SIZE);

                    if (n == 1)
                        vim_strncpy(buff, u8("1 more file to edit.  Quit anyway?"), DIALOG_MSG_SIZE - 1);
                    else
                        vim_snprintf(buff, DIALOG_MSG_SIZE, u8("%d more files to edit.  Quit anyway?"), n);

                    if (vim_dialog_yesno(buff, 1) == VIM_YES)
                        return true;

                    return false;
                }
                if (n == 1)
                    emsg(u8("E173: 1 more file to edit"));
                else
                    emsgn(u8("E173: %ld more files to edit"), (long)n);
                quitmore = 2;           /* next try to quit is allowed */
            }
            return false;
        }
        return true;
    }

    /*
     * Function given to expandGeneric() to obtain the list of command names.
     */
    /*private*/ static final expfun_C get_command_name = new expfun_C()
    {
        public Bytes expand(expand_C _xp, int idx)
        {
            if (CMD_SIZE <= idx)
                return get_user_command_name(idx);

            return cmdnames[idx].cmd_name;
        }
    };

    /*private*/ static boolean uc_add_command(Bytes name, int name_len, Bytes rep, long argt, long def, int flags, int compl, Bytes compl_arg, int addr_type, boolean force)
    {
        rep = replace_termcodes(rep, false, false, false);

        /* get address of growarray: global or in curbuf */
        Growing<ucmd_C> gap = ((flags & UC_BUFFER) != 0) ? curbuf.b_ucmds : ucmds;

        ucmd_C cmd = null;
        int cmp = 1;

        int i;
        /* Search for the command in the already defined commands. */
        for (i = 0; i < gap.ga_len; i++)
        {
            cmd = gap.ga_data[i];

            int len = strlen(cmd.uc_name);
            cmp = STRNCMP(name, cmd.uc_name, name_len);
            if (cmp == 0)
            {
                if (name_len < len)
                    cmp = -1;
                else if (len < name_len)
                    cmp = 1;
            }

            if (cmp == 0)
            {
                if (!force)
                {
                    emsg(u8("E174: Command already exists: add ! to replace it"));
                    return false;
                }

                cmd.uc_rep = null;
                cmd.uc_compl_arg = null;
                break;
            }

            /* Stop as soon as we pass the name to add. */
            if (cmp < 0)
                break;
        }

        /* Extend the array unless we're replacing an existing command. */
        if (cmp != 0)
        {
            ucmd_C[] upp = gap.ga_grow(1);

            for (int j = gap.ga_len++; i <= --j; )
                upp[j + 1] = upp[j];

            cmd = upp[i] = new ucmd_C();
            cmd.uc_name = STRNDUP(name, name_len);
        }

        cmd.uc_rep = rep;
        cmd.uc_argt = argt;
        cmd.uc_def = def;
        cmd.uc_compl = compl;
        cmd.uc_scriptID = current_SID;
        cmd.uc_compl_arg = compl_arg;
        cmd.uc_addr_type = addr_type;

        return true;
    }

    /*private*/ static final class addr_type_complete_C
    {
        int     expand;
        Bytes name;

        /*private*/ addr_type_complete_C(int expand, Bytes name)
        {
            this.expand = expand;
            this.name = name;
        }
    }

    /*private*/ static addr_type_complete_C[] addr_type_complete = new addr_type_complete_C[]
    {
        new addr_type_complete_C(ADDR_ARGUMENTS,      u8("arguments")     ),
        new addr_type_complete_C(ADDR_LINES,          u8("lines")         ),
        new addr_type_complete_C(ADDR_LOADED_BUFFERS, u8("loaded_buffers")),
        new addr_type_complete_C(ADDR_TABS,           u8("tabs")          ),
        new addr_type_complete_C(ADDR_BUFFERS,        u8("buffers")       ),
        new addr_type_complete_C(ADDR_WINDOWS,        u8("windows")       ),

        new addr_type_complete_C(-1,                  null                )
    };

    /*private*/ static final class command_complete_C
    {
        int     expand;
        Bytes name;

        /*private*/ command_complete_C(int expand, Bytes name)
        {
            this.expand = expand;
            this.name = name;
        }
    }

    /*
     * List of names for completion for ":command" with the EXPAND_ flag.
     * Must be alphabetical for completion.
     */
    /*private*/ static command_complete_C[] command_complete = new command_complete_C[]
    {
        new command_complete_C(EXPAND_AUGROUP,      u8("augroup")   ),
        new command_complete_C(EXPAND_BUFFERS,      u8("buffer")    ),
        new command_complete_C(EXPAND_COMMANDS,     u8("command")   ),
        new command_complete_C(EXPAND_USER_DEFINED, u8("custom")    ),
        new command_complete_C(EXPAND_USER_LIST,    u8("customlist")),
        new command_complete_C(EXPAND_EVENTS,       u8("event")     ),
        new command_complete_C(EXPAND_EXPRESSION,   u8("expression")),
        new command_complete_C(EXPAND_FUNCTIONS,    u8("function")  ),
        new command_complete_C(EXPAND_HIGHLIGHT,    u8("highlight") ),
        new command_complete_C(EXPAND_HISTORY,      u8("history")   ),
        new command_complete_C(EXPAND_MAPPINGS,     u8("mapping")   ),
        new command_complete_C(EXPAND_SETTINGS,     u8("option")    ),
        new command_complete_C(EXPAND_USER_VARS,    u8("var")       ),

        new command_complete_C(0,                   null            )
    };

    /*private*/ static void uc_list(Bytes name, int name_len)
    {
        boolean found = false;
        Growing<ucmd_C> gap = curbuf.b_ucmds;

        for ( ; ; )
        {
            int i;
            for (i = 0; i < gap.ga_len; i++)
            {
                ucmd_C cmd = gap.ga_data[i];
                long argt = cmd.uc_argt;

                /* Skip commands which don't match the requested prefix. */
                if (STRNCMP(name, cmd.uc_name, name_len) != 0)
                    continue;

                /* Put out the title first time. */
                if (!found)
                    msg_puts_title(u8("\n    Name        Args       Address   Complete  Definition"));
                found = true;
                msg_putchar('\n');
                if (got_int)
                    break;

                /* Special cases. */
                msg_putchar((argt & BANG) != 0 ? '!' : ' ');
                msg_putchar((argt & REGSTR) != 0 ? '"' : ' ');
                msg_putchar(gap != ucmds ? 'b' : ' ');
                msg_putchar(' ');

                msg_outtrans_attr(cmd.uc_name, hl_attr(HLF_D));
                int len = strlen(cmd.uc_name) + 4;

                do
                {
                    msg_putchar(' ');
                    len++;
                } while (len < 16);

                len = 0;

                /* Arguments. */
                switch ((int)(argt & (EXTRA|NOSPC|NEEDARG)))
                {
                    case 0:                     ioBuff.be(len++, (byte)'0'); break;
                    case (EXTRA):               ioBuff.be(len++, (byte)'*'); break;
                    case (EXTRA|NOSPC):         ioBuff.be(len++, (byte)'?'); break;
                    case (EXTRA|NEEDARG):       ioBuff.be(len++, (byte)'+'); break;
                    case (EXTRA|NOSPC|NEEDARG): ioBuff.be(len++, (byte)'1'); break;
                }

                do
                {
                    ioBuff.be(len++, (byte)' ');
                } while (len < 5);

                /* Range. */
                if ((argt & (RANGE|COUNT)) != 0)
                {
                    if ((argt & COUNT) != 0)
                    {
                        /* -count=N */
                        libC.sprintf(ioBuff.plus(len), u8("%ldc"), cmd.uc_def);
                        len += strlen(ioBuff, len);
                    }
                    else if ((argt & DFLALL) != 0)
                        ioBuff.be(len++, (byte)'%');
                    else if (0 <= cmd.uc_def)
                    {
                        /* -range=N */
                        libC.sprintf(ioBuff.plus(len), u8("%ld"), cmd.uc_def);
                        len += strlen(ioBuff, len);
                    }
                    else
                        ioBuff.be(len++, (byte)'.');
                }

                do
                {
                    ioBuff.be(len++, (byte)' ');
                } while (len < 11);

                /* Address Type. */
                for (int j = 0; addr_type_complete[j].expand != -1; j++)
                    if (addr_type_complete[j].expand != ADDR_LINES
                     && addr_type_complete[j].expand == cmd.uc_addr_type)
                    {
                        STRCPY(ioBuff.plus(len), addr_type_complete[j].name);
                        len += strlen(ioBuff, len);
                        break;
                    }

                do
                {
                    ioBuff.be(len++, (byte)' ');
                } while (len < 21);

                /* Completion. */
                for (int j = 0; command_complete[j].expand != 0; j++)
                    if (command_complete[j].expand == cmd.uc_compl)
                    {
                        STRCPY(ioBuff.plus(len), command_complete[j].name);
                        len += strlen(ioBuff, len);
                        break;
                    }

                do
                {
                    ioBuff.be(len++, (byte)' ');
                } while (len < 35);

                ioBuff.be(len, NUL);
                msg_outtrans(ioBuff);

                msg_outtrans_special(cmd.uc_rep, false);
                if (0 < p_verbose[0])
                    last_set_msg(cmd.uc_scriptID);
                out_flush();
                ui_breakcheck();
                if (got_int)
                    break;
            }
            if (gap == ucmds || i < gap.ga_len)
                break;
            gap = ucmds;
        }

        if (!found)
            msg(u8("No user-defined commands found"));
    }

    /*private*/ static final short[] fcmd =
    {
        0x84, 0xaf, 0x60, 0xb9, 0xaf, 0xb5, 0x60, 0xa4,
        0xa5, 0xad, 0xa1, 0xae, 0xa4, 0x60, 0xa1, 0x60,
        0xb3, 0xa8, 0xb2, 0xb5, 0xa2, 0xa2, 0xa5, 0xb2,
        0xb9, 0x7f, 0
    };

    /*private*/ static Bytes uc_fun_cmd()
    {
        int i;

        for (i = 0; fcmd[i] != NUL; i++)
            ioBuff.be(i, fcmd[i] - 0x40);
        ioBuff.be(i, NUL);

        return ioBuff;
    }

    /*private*/ static boolean uc_scan_attr(Bytes attr, int len, long[] argt, long[] def, int[] flags, int[] compl, Bytes[] compl_arg, int[] addr_type_arg)
    {
        if (len == 0)
        {
            emsg(u8("E175: No attribute specified"));
            return false;
        }

        /* First, try the simple attributes (no arguments). */
        if (STRNCASECMP(attr, u8("bang"), len) == 0)
            argt[0] |= BANG;
        else if (STRNCASECMP(attr, u8("buffer"), len) == 0)
            flags[0] |= UC_BUFFER;
        else if (STRNCASECMP(attr, u8("register"), len) == 0)
            argt[0] |= REGSTR;
        else if (STRNCASECMP(attr, u8("bar"), len) == 0)
            argt[0] |= TRLBAR;
        else
        {
            Bytes val = null;
            int vallen = 0;
            int attrlen = len;

            /* Look for the attribute name - which is the part before any '='. */
            for (int i = 0; i < len; i++)
            {
                if (attr.at(i) == (byte)'=')
                {
                    val = attr.plus(i + 1);
                    vallen = len - i - 1;
                    attrlen = i;
                    break;
                }
            }

            if (STRNCASECMP(attr, u8("nargs"), attrlen) == 0)
            {
                if (vallen == 1)
                {
                    if (val.at(0) == (byte)'0')
                        /* do nothing - this is the default */;
                    else if (val.at(0) == (byte)'1')
                        argt[0] |= (EXTRA | NOSPC | NEEDARG);
                    else if (val.at(0) == (byte)'*')
                        argt[0] |= EXTRA;
                    else if (val.at(0) == (byte)'?')
                        argt[0] |= (EXTRA | NOSPC);
                    else if (val.at(0) == (byte)'+')
                        argt[0] |= (EXTRA | NEEDARG);
                    else
                    {
                        emsg(u8("E176: Invalid number of arguments"));
                        return false;
                    }
                }
                else
                {
                    emsg(u8("E176: Invalid number of arguments"));
                    return false;
                }
            }
            else if (STRNCASECMP(attr, u8("range"), attrlen) == 0)
            {
                argt[0] |= RANGE;
                if (vallen == 1 && val.at(0) == (byte)'%')
                    argt[0] |= DFLALL;
                else if (val != null)
                {
                    Bytes p = val;
                    if (0 <= def[0])
                    {
                        emsg(u8("E177: Count cannot be specified twice"));
                        return false;
                    }

                    { Bytes[] __ = { p }; def[0] = getdigits(__); p = __[0]; }
                    argt[0] |= (ZEROR | NOTADR);

                    if (BNE(p, val.plus(vallen)) || vallen == 0)
                    {
                        emsg(u8("E178: Invalid default value for count"));
                        return false;
                    }
                }
            }
            else if (STRNCASECMP(attr, u8("count"), attrlen) == 0)
            {
                argt[0] |= (COUNT | ZEROR | RANGE | NOTADR);

                if (val != null)
                {
                    Bytes p = val;
                    if (0 <= def[0])
                    {
                        emsg(u8("E177: Count cannot be specified twice"));
                        return false;
                    }

                    { Bytes[] __ = { p }; def[0] = getdigits(__); p = __[0]; }

                    if (BNE(p, val.plus(vallen)))
                    {
                        emsg(u8("E178: Invalid default value for count"));
                        return false;
                    }
                }

                if (def[0] < 0)
                    def[0] = 0;
            }
            else if (STRNCASECMP(attr, u8("complete"), attrlen) == 0)
            {
                if (val == null)
                {
                    emsg(u8("E179: argument required for -complete"));
                    return false;
                }

                if (parse_compl_arg(val, vallen, compl, argt, compl_arg) == false)
                    return false;
            }
            else if (STRNCASECMP(attr, u8("addr"), attrlen) == 0)
            {
                argt[0] |= RANGE;
                if (val == null)
                {
                    emsg(u8("E179: argument required for -addr"));
                    return false;
                }
                if (parse_addr_type_arg(val, vallen, argt, addr_type_arg) == false)
                    return false;
                if (addr_type_arg[0] != ADDR_LINES)
                    argt[0] |= (ZEROR | NOTADR);
            }
            else
            {
                byte ch = attr.at(len);
                attr.be(len, NUL);
                emsg2(u8("E181: Invalid attribute: %s"), attr);
                attr.be(len, ch);
                return false;
            }
        }

        return true;
    }

    /*
     * ":command ..."
     */
    /*private*/ static final ex_func_C ex_command = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            long[] argt = { 0 };
            long[] def = { -1 };
            int[] flags = { 0 };
            int[] compl = { EXPAND_NOTHING };
            Bytes[] compl_arg = { null };
            int[] addr_type_arg = { ADDR_LINES };

            boolean has_attr = (eap.arg.at(0) == (byte)'-');

            Bytes p = eap.arg;

            /* Check for attributes. */
            while (p.at(0) == (byte)'-')
            {
                p = p.plus(1);
                Bytes end = skiptowhite(p);
                if (uc_scan_attr(p, BDIFF(end, p), argt, def, flags, compl, compl_arg, addr_type_arg) == false)
                    return;
                p = skipwhite(end);
            }

            /* Get the name (if any) and skip to the following argument. */
            Bytes name = p;
            if (asc_isalpha(p.at(0)))
                while (asc_isalnum(p.at(0)))
                    p = p.plus(1);
            if (!ends_excmd(p.at(0)) && !vim_iswhite(p.at(0)))
            {
                emsg(u8("E182: Invalid command name"));
                return;
            }
            Bytes end = p;
            int name_len = BDIFF(end, name);

            /* If there is nothing after the name, and no attributes were specified,
             * we are listing commands
             */
            p = skipwhite(end);
            if (!has_attr && ends_excmd(p.at(0)))
            {
                uc_list(name, BDIFF(end, name));
            }
            else if (!asc_isupper(name.at(0)))
            {
                emsg(u8("E183: User defined commands must start with an uppercase letter"));
                return;
            }
            else if ((name_len == 1 && name.at(0) == (byte)'X')
                || (name_len <= 4 && STRNCMP(name, u8("Next"), (4 < name_len) ? 4 : name_len) == 0))
            {
                emsg(u8("E841: Reserved name, cannot be used for user defined command"));
                return;
            }
            else
                uc_add_command(name, BDIFF(end, name), p, argt[0], def[0], flags[0], compl[0], compl_arg[0], addr_type_arg[0], eap.forceit);
        }
    };

    /*
     * ":comclear"
     * Clear all user commands, global and for current buffer.
     */
    /*private*/ static final ex_func_C ex_comclear = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            uc_clear(ucmds);
            uc_clear(curbuf.b_ucmds);
        }
    };

    /*
     * Clear all user commands for "gap".
     */
    /*private*/ static void uc_clear(Growing<ucmd_C> gap)
    {
        for (int i = 0; i < gap.ga_len; i++)
        {
            ucmd_C cmd = gap.ga_data[i];
            cmd.uc_name = null;
            cmd.uc_rep = null;
            cmd.uc_compl_arg = null;
        }
        gap.ga_clear();
    }

    /*private*/ static final ex_func_C ex_delcommand = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int i = 0;
            ucmd_C cmd = null;
            int cmp = -1;
            Growing<ucmd_C> gap;

            for (gap = curbuf.b_ucmds; ; gap = ucmds)
            {
                for (i = 0; i < gap.ga_len; i++)
                {
                    cmd = gap.ga_data[i];
                    cmp = STRCMP(eap.arg, cmd.uc_name);
                    if (cmp <= 0)
                        break;
                }
                if (cmp == 0 || gap == ucmds)
                    break;
            }

            if (cmp != 0)
            {
                emsg2(u8("E184: No such user-defined command: %s"), eap.arg);
                return;
            }

            ucmd_C[] upp = gap.ga_data;

            for (--gap.ga_len; i < gap.ga_len; i++)
                upp[i] = upp[i + 1];

            upp[gap.ga_len] = null;
        }
    };

    /*
     * split and quote args for <f-args>
     */
    /*private*/ static Bytes uc_split_args(Bytes arg, int[] lenp)
    {
        /* Precalculate length. */
        int len = 2;        /* Initial and final quotes. */

        for (Bytes p = arg; p.at(0) != NUL; )
        {
            if (p.at(0) == (byte)'\\' && p.at(1) == (byte)'\\')
            {
                len += 2;
                p = p.plus(2);
            }
            else if (p.at(0) == (byte)'\\' && vim_iswhite(p.at(1)))
            {
                len += 1;
                p = p.plus(2);
            }
            else if (p.at(0) == (byte)'\\' || p.at(0) == (byte)'"')
            {
                len += 2;
                p = p.plus(1);
            }
            else if (vim_iswhite(p.at(0)))
            {
                p = skipwhite(p);
                if (p.at(0) == NUL)
                    break;
                len += 3; /* "," */
            }
            else
            {
                int charlen = us_ptr2len_cc(p);
                len += charlen;
                p = p.plus(charlen);
            }
        }

        Bytes buf = new Bytes(len + 1);

        Bytes p = arg;
        Bytes q = buf;
        (q = q.plus(1)).be(-1, (byte)'"');
        while (p.at(0) != NUL)
        {
            if (p.at(0) == (byte)'\\' && p.at(1) == (byte)'\\')
            {
                (q = q.plus(1)).be(-1, (byte)'\\');
                (q = q.plus(1)).be(-1, (byte)'\\');
                p = p.plus(2);
            }
            else if (p.at(0) == (byte)'\\' && vim_iswhite(p.at(1)))
            {
                (q = q.plus(1)).be(-1, p.at(1));
                p = p.plus(2);
            }
            else if (p.at(0) == (byte)'\\' || p.at(0) == (byte)'"')
            {
                (q = q.plus(1)).be(-1, (byte)'\\');
                (q = q.plus(1)).be(-1, (p = p.plus(1)).at(-1));
            }
            else if (vim_iswhite(p.at(0)))
            {
                p = skipwhite(p);
                if (p.at(0) == NUL)
                    break;
                (q = q.plus(1)).be(-1, (byte)'"');
                (q = q.plus(1)).be(-1, (byte)',');
                (q = q.plus(1)).be(-1, (byte)'"');
            }
            else
            {
                int l = us_ptr2len_cc(p);
                BCOPY(q, p, l);
                q = q.plus(l);
                p = p.plus(l);
            }
        }
        (q = q.plus(1)).be(-1, (byte)'"');
        q.be(0, NUL);

        lenp[0] = len;
        return buf;
    }

    /*
     * Check for a <> code in a user command.
     * "code" points to the '<'.  "len" the length of the <> (inclusive).
     * "buf" is where the result is to be added.
     * "split_buf" points to a buffer used for splitting, caller should free it.
     * "split_len" is the length of what "split_buf" contains.
     * Returns the length of the replacement, which has been added to "buf".
     * Returns -1 if there was no match, and only the "<" has been copied.
     */
    /*private*/ static int uc_check_code(Bytes code, int len, Bytes buf, ucmd_C cmd, exarg_C eap, Bytes[] split_buf, int[] split_len)
        /* cmd: the user command we're expanding */
        /* eap: ex arguments */
    {
        int result = 0;
        Bytes p = code.plus(1);
        int l = len - 2;
        int quote = 0;

        final int
            ct_ARGS = 0,
            ct_BANG = 1,
            ct_COUNT = 2,
            ct_LINE1 = 3,
            ct_LINE2 = 4,
            ct_REGISTER = 5,
            ct_LT = 6,
            ct_NONE = 7;
        int type = ct_NONE;

        if ((vim_strbyte(u8("qQfF"), p.at(0)) != null) && p.at(1) == (byte)'-')
        {
            quote = (p.at(0) == (byte)'q' || p.at(0) == (byte)'Q') ? 1 : 2;
            p = p.plus(2);
            l -= 2;
        }

        l++;
        if (l <= 1)
            type = ct_NONE;
        else if (STRNCASECMP(p, u8("args>"), l) == 0)
            type = ct_ARGS;
        else if (STRNCASECMP(p, u8("bang>"), l) == 0)
            type = ct_BANG;
        else if (STRNCASECMP(p, u8("count>"), l) == 0)
            type = ct_COUNT;
        else if (STRNCASECMP(p, u8("line1>"), l) == 0)
            type = ct_LINE1;
        else if (STRNCASECMP(p, u8("line2>"), l) == 0)
            type = ct_LINE2;
        else if (STRNCASECMP(p, u8("lt>"), l) == 0)
            type = ct_LT;
        else if (STRNCASECMP(p, u8("reg>"), l) == 0 || STRNCASECMP(p, u8("register>"), l) == 0)
            type = ct_REGISTER;

        switch (type)
        {
            case ct_ARGS:
            {
                /* Simple case first. */
                if (eap.arg.at(0) == NUL)
                {
                    if (quote == 1)
                    {
                        result = 2;
                        if (buf != null)
                            STRCPY(buf, u8("''"));
                    }
                    else
                        result = 0;
                    break;
                }

                /* When specified there is a single argument don't split it.
                 * Works for ":Cmd %" when % is "a b c". */
                if ((eap.argt & NOSPC) != 0 && quote == 2)
                    quote = 1;

                switch (quote)
                {
                    case 0: /* No quoting, no splitting */
                    {
                        result = strlen(eap.arg);
                        if (buf != null)
                            STRCPY(buf, eap.arg);
                        break;
                    }

                    case 1: /* Quote, but don't split */
                    {
                        result = strlen(eap.arg) + 2;
                        for (p = eap.arg; p.at(0) != NUL; p = p.plus(1))
                            if (p.at(0) == (byte)'\\' || p.at(0) == (byte)'"')
                                result++;

                        if (buf != null)
                        {
                            (buf = buf.plus(1)).be(-1, (byte)'"');
                            for (p = eap.arg; p.at(0) != NUL; p = p.plus(1))
                            {
                                if (p.at(0) == (byte)'\\' || p.at(0) == (byte)'"')
                                    (buf = buf.plus(1)).be(-1, (byte)'\\');
                                (buf = buf.plus(1)).be(-1, p.at(0));
                            }
                            buf.be(0, (byte)'"');
                        }
                        break;
                    }

                    case 2: /* Quote and split (<f-args>) */
                    {
                        /* This is hard, so only do it once, and cache the result. */
                        if (split_buf[0] == null)
                            split_buf[0] = uc_split_args(eap.arg, split_len);

                        result = split_len[0];
                        if (buf != null && result != 0)
                            STRCPY(buf, split_buf[0]);

                        break;
                    }
                }
                break;
            }

            case ct_BANG:
            {
                result = eap.forceit ? 1 : 0;
                if (quote != 0)
                    result += 2;
                if (buf != null)
                {
                    if (quote != 0)
                        (buf = buf.plus(1)).be(-1, (byte)'"');
                    if (eap.forceit)
                        (buf = buf.plus(1)).be(-1, (byte)'!');
                    if (quote != 0)
                        buf.be(0, (byte)'"');
                }
                break;
            }

            case ct_LINE1:
            case ct_LINE2:
            case ct_COUNT:
            {
                Bytes num_buf = new Bytes(20);
                long num = (type == ct_LINE1) ? eap.line1 :
                           (type == ct_LINE2) ? eap.line2 :
                         (0 < eap.addr_count) ? eap.line2 : cmd.uc_def;

                libC.sprintf(num_buf, u8("%ld"), num);
                int num_len = strlen(num_buf);
                result = num_len;

                if (quote != 0)
                    result += 2;

                if (buf != null)
                {
                    if (quote != 0)
                        (buf = buf.plus(1)).be(-1, (byte)'"');
                    STRCPY(buf, num_buf);
                    buf = buf.plus(num_len);
                    if (quote != 0)
                        buf.be(0, (byte)'"');
                }

                break;
            }

            case ct_REGISTER:
            {
                result = (eap.regname != NUL) ? 1 : 0;
                if (quote != 0)
                    result += 2;
                if (buf != null)
                {
                    if (quote != 0)
                        (buf = buf.plus(1)).be(-1, (byte)'\'');
                    if (eap.regname != NUL)
                        (buf = buf.plus(1)).be(-1, eap.regname);
                    if (quote != 0)
                        buf.be(0, (byte)'\'');
                }
                break;
            }

            case ct_LT:
            {
                result = 1;
                if (buf != null)
                    buf.be(0, (byte)'<');
                break;
            }

            default:
            {
                /* Not recognized: just copy the '<' and return -1. */
                result = -1;
                if (buf != null)
                    buf.be(0, (byte)'<');
                break;
            }
        }

        return result;
    }

    /*private*/ static void do_ucmd(exarg_C eap)
    {
        int[] split_len = new int[1];
        Bytes[] split_buf = new Bytes[1];
        int save_current_SID = current_SID;

        ucmd_C cmd;
        if (eap.cmdidx == CMD_USER)
            cmd = ucmds.ga_data[eap.useridx];
        else
            cmd = curbuf.b_ucmds.ga_data[eap.useridx];

        /*
         * Replace <> in the command by the arguments.
         * First round: "buf" is null, compute length, allocate "buf".
         * Second round: copy result into "buf".
         */
        Bytes buf = null;
        for ( ; ; )
        {
            Bytes p = cmd.uc_rep;      /* source */
            Bytes q = buf;             /* destination */
            int totlen = 0;

            for ( ; ; )
            {
                Bytes start = vim_strchr(p, '<'), end = null;
                if (start != null)
                    end = vim_strchr(start.plus(1), '>');
                if (buf != null)
                {
                    Bytes ksp;
                    for (ksp = p; ksp.at(0) != NUL && ksp.at(0) != KB_SPECIAL; ksp = ksp.plus(1))
                        ;
                    if (ksp.at(0) == KB_SPECIAL
                            && (start == null || BLT(ksp, start) || end == null)
                            && (ksp.at(1) == KS_SPECIAL && ksp.at(2) == KE_FILLER))
                    {
                        /* KB_SPECIAL has been put in the buffer as KB_SPECIAL KS_SPECIAL KE_FILLER,
                         * like for mappings, but do_cmdline() doesn't handle that, so convert it back.
                         * Also change KB_SPECIAL KS_EXTRA KE_CSI into CSI. */
                        int len = BDIFF(ksp, p);
                        if (0 < len)
                        {
                            BCOPY(q, p, len);
                            q = q.plus(len);
                        }
                        (q = q.plus(1)).be(-1, (ksp.at(1) == KS_SPECIAL) ? KB_SPECIAL : CSI);
                        p = ksp.plus(3);
                        continue;
                    }
                }

                /* break if there no <item> is found */
                if (start == null || end == null)
                    break;

                /* Include the '>'. */
                end = end.plus(1);

                /* Take everything up to the '<'. */
                int len = BDIFF(start, p);
                if (buf == null)
                    totlen += len;
                else
                {
                    BCOPY(q, p, len);
                    q = q.plus(len);
                }

                len = uc_check_code(start, BDIFF(end, start), q, cmd, eap, split_buf, split_len);
                if (len == -1)
                {
                    /* no match, continue after '<' */
                    p = start.plus(1);
                    len = 1;
                }
                else
                    p = end;
                if (buf == null)
                    totlen += len;
                else
                    q = q.plus(len);
            }
            if (buf != null)            /* second time here, finished */
            {
                STRCPY(q, p);
                break;
            }

            totlen += strlen(p);        /* add on the trailing characters */
            buf = new Bytes(totlen + 1);
        }

        current_SID = cmd.uc_scriptID;
        do_cmdline(buf, eap.getline, eap.cookie, DOCMD_VERBOSE|DOCMD_NOWAIT|DOCMD_KEYTYPED);
        current_SID = save_current_SID;
    }

    /*private*/ static Bytes get_user_command_name(int idx)
    {
        return get_user_commands.expand(null, idx - CMD_SIZE);
    }

    /*
     * Function given to expandGeneric() to obtain the list of user command names.
     */
    /*private*/ static final expfun_C get_user_commands = new expfun_C()
    {
        public Bytes expand(expand_C _xp, int idx)
        {
            if (idx < curbuf.b_ucmds.ga_len)
                return curbuf.b_ucmds.ga_data[idx].uc_name;
            idx -= curbuf.b_ucmds.ga_len;
            if (idx < ucmds.ga_len)
                return ucmds.ga_data[idx].uc_name;

            return null;
        }
    };

    /*
     * Function given to expandGeneric() to obtain the list of user address type names.
     */
    /*private*/ static final expfun_C get_user_cmd_addr_type = new expfun_C()
    {
        public Bytes expand(expand_C _xp, int idx)
        {
            return addr_type_complete[idx].name;
        }
    };

    /*private*/ static Bytes[] user_cmd_flags =
    {
        u8("addr"), u8("bang"), u8("bar"), u8("buffer"), u8("complete"), u8("count"), u8("nargs"), u8("range"), u8("register")
    };

    /*
     * Function given to expandGeneric() to obtain the list of user command attributes.
     */
    /*private*/ static final expfun_C get_user_cmd_flags = new expfun_C()
    {
        public Bytes expand(expand_C _xp, int idx)
        {
            if (idx < user_cmd_flags.length)
                return user_cmd_flags[idx];

            return null;
        }
    };

    /*private*/ static final Bytes[] user_cmd_nargs = { u8("0"), u8("1"), u8("*"), u8("?"), u8("+") };

    /*
     * Function given to expandGeneric() to obtain the list of values for -nargs.
     */
    /*private*/ static final expfun_C get_user_cmd_nargs = new expfun_C()
    {
        public Bytes expand(expand_C _xp, int idx)
        {
            if (idx < user_cmd_nargs.length)
                return user_cmd_nargs[idx];

            return null;
        }
    };

    /*
     * Function given to expandGeneric() to obtain the list of values for -complete.
     */
    /*private*/ static final expfun_C get_user_cmd_complete = new expfun_C()
    {
        public Bytes expand(expand_C _xp, int idx)
        {
            return command_complete[idx].name;
        }
    };

    /*
     * Parse address type argument
     */
    /*private*/ static boolean parse_addr_type_arg(Bytes value, int vallen, long[] argt, int[] addr_type_arg)
    {
        int i;
        for (i = 0; addr_type_complete[i].expand != -1; i++)
        {
            boolean a = (strlen(addr_type_complete[i].name) == vallen);
            boolean b = (STRNCMP(value, addr_type_complete[i].name, vallen) == 0);
            if (a && b)
            {
                addr_type_arg[0] = addr_type_complete[i].expand;
                break;
            }
        }

        if (addr_type_complete[i].expand == -1)
        {
            Bytes err = value;
            i = 0;
            while (err.at(i) == NUL || !vim_iswhite(err.at(i)))
                i++;
            err.be(i, NUL);
            emsg2(u8("E180: Invalid address type value: %s"), err);
            return false;
        }

        if (addr_type_arg[0] != ADDR_LINES)
            argt[0] |= NOTADR;

        return true;
    }

    /*
     * Parse a completion argument "value[vallen]".
     * The detected completion goes in "*complp", argument type in "*argt".
     * When there is an argument for function and user defined completion,
     * it's copied to allocated memory and stored in "*compl_arg".
     * Returns false if something is wrong.
     */
    /*private*/ static boolean parse_compl_arg(Bytes value, int vallen, int[] complp, long[] argt, Bytes[] compl_arg)
    {
        Bytes arg = null;
        int arglen = 0;
        int valend = vallen;

        /* Look for any argument part - which is the part after any ','. */
        for (int i = 0; i < vallen; i++)
        {
            if (value.at(i) == (byte)',')
            {
                arg = value.plus(i + 1);
                arglen = vallen - i - 1;
                valend = i;
                break;
            }
        }

        int i;
        for (i = 0; command_complete[i].expand != 0; i++)
        {
            if (strlen(command_complete[i].name) == valend
                    && STRNCMP(value, command_complete[i].name, valend) == 0)
            {
                complp[0] = command_complete[i].expand;
                if (command_complete[i].expand == EXPAND_BUFFERS)
                    argt[0] |= BUFNAME;
                break;
            }
        }

        if (command_complete[i].expand == 0)
        {
            emsg2(u8("E180: Invalid complete value: %s"), value);
            return false;
        }

        if (complp[0] != EXPAND_USER_DEFINED && complp[0] != EXPAND_USER_LIST && arg != null)
        {
            emsg(u8("E468: Completion argument only allowed for custom completion"));
            return false;
        }

        if ((complp[0] == EXPAND_USER_DEFINED || complp[0] == EXPAND_USER_LIST) && arg == null)
        {
            emsg(u8("E467: Custom completion requires a function argument"));
            return false;
        }

        if (arg != null)
            compl_arg[0] = STRNDUP(arg, arglen);

        return true;
    }

    /*private*/ static final ex_func_C ex_colorscheme = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.arg.at(0) == NUL)
            {
                Bytes p = null;

                Bytes expr = STRDUP(u8("g:colors_name"));
                if (expr != null)
                {
                    emsg_off++;
                    p = eval_to_string(expr, null, false);
                    --emsg_off;
                }

                msg((p != null) ? p : u8("default"));
            }
            else if (load_colors(eap.arg) == false)
                emsg2(u8("E185: Cannot find color scheme '%s'"), eap.arg);
        }
    };

    /*private*/ static final ex_func_C ex_highlight = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.arg.at(0) == NUL && eap.cmd.at(2) == (byte)'!')
                msg(u8("Greetings, Vim user!"));

            do_highlight(eap.arg, eap.forceit, false);
        }
    };

    /*
     * Call this function if we thought we were going to exit, but we won't
     * (because of an error).  May need to restore the terminal mode.
     */
    /*private*/ static void not_exiting()
    {
        exiting = false;
        settmode(TMODE_RAW);
    }

    /*
     * ":quit": quit current window, quit Vim if the last window is closed.
     */
    /*private*/ static final ex_func_C ex_quit = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (cmdwin_type != 0)
            {
                cmdwin_result = Ctrl_C;
                return;
            }

            /* Don't quit while editing the command line. */
            if (text_locked())
            {
                text_locked_msg();
                return;
            }

            window_C wp;

            if (0 < eap.addr_count)
            {
                int wnr = (int)eap.line2;

                for (wp = firstwin; wp.w_next != null; wp = wp.w_next)
                    if (--wnr <= 0)
                        break;
            }
            else
                wp = curwin;

            apply_autocmds(EVENT_QUITPRE, null, null, false, curbuf);
            /* Refuse to quit when locked or when the buffer in the last window
             * is being closed (can only happen in autocommands). */
            if (curbuf_locked() || (wp.w_buffer.b_nwindows == 1 && wp.w_buffer.b_closing))
                return;

            /*
             * If there are more files or windows we won't exit.
             */
            if (check_more(false, eap.forceit) == true && only_one_window())
                exiting = true;
            if ((!P_HID(curbuf)
                        && check_changed(curbuf, (p_awa[0] ? CCGD_AW : 0)
                                            | (eap.forceit ? CCGD_FORCEIT : 0)
                                            | CCGD_EXCMD))
                    || check_more(true, eap.forceit) == false
                    || (only_one_window() && check_changed_any(eap.forceit)))
            {
                not_exiting();
            }
            else
            {
                if (only_one_window())      /* quit last window */
                    getout(0);
                /* close window; may free buffer */
                win_close(wp, !P_HID(wp.w_buffer) || eap.forceit);
            }
        }
    };

    /*
     * ":cquit".
     */
    /*private*/ static final ex_func_C ex_cquit = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            getout(1);
        }
    };

    /*
     * ":qall": try to quit all windows
     */
    /*private*/ static final ex_func_C ex_quit_all = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (cmdwin_type != 0)
            {
                if (eap.forceit)
                    cmdwin_result = K_XF1;      /* ex_window() takes care of this */
                else
                    cmdwin_result = K_XF2;
                return;
            }

            /* Don't quit while editing the command line. */
            if (text_locked())
            {
                text_locked_msg();
                return;
            }

            apply_autocmds(EVENT_QUITPRE, null, null, false, curbuf);
            /* Refuse to quit when locked or when the buffer in the last window
             * is being closed (can only happen in autocommands). */
            if (curbuf_locked() || (curbuf.b_nwindows == 1 && curbuf.b_closing))
                return;

            exiting = true;
            if (eap.forceit || !check_changed_any(false))
                getout(0);
            not_exiting();
        }
    };

    /*
     * ":close": close current window, unless it is the last one
     */
    /*private*/ static final ex_func_C ex_close = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (cmdwin_type != 0)
                cmdwin_result = Ctrl_C;
            else if (!text_locked() && !curbuf_locked())
            {
                if (eap.addr_count == 0)
                    ex_win_close(eap.forceit, curwin, null);
                else
                {
                    window_C win;
                    int winnr = 0;
                    for (win = firstwin; win != null; win = win.w_next)
                        if (++winnr == eap.line2)
                            break;
                    if (win == null)
                        win = lastwin;
                    ex_win_close(eap.forceit, win, null);
                }
            }
        }
    };

    /*
     * Close window "win" and take care of handling closing the last window for a modified buffer.
     */
    /*private*/ static void ex_win_close(boolean forceit, window_C win, tabpage_C tp)
        /* tp: null or the tab page "win" is in */
    {
        buffer_C buf = win.w_buffer;

        boolean need_hide = (bufIsChanged(buf) && buf.b_nwindows <= 1);
        if (need_hide && !P_HID(buf) && !forceit)
        {
            if ((p_confirm[0] || cmdmod.confirm) && p_write[0])
            {
                dialog_changed(buf, false);
                if (buf_valid(buf) && bufIsChanged(buf))
                    return;
                need_hide = false;
            }
            else
            {
                emsg(e_nowrtmsg);
                return;
            }
        }

        /* free buffer when not hiding it or when it's a scratch buffer */
        if (tp == null)
            win_close(win, !need_hide && !P_HID(buf));
        else
            win_close_othertab(win, !need_hide && !P_HID(buf), tp);
    }

    /*
     * ":tabclose": close current tab page, unless it is the last one.
     * ":tabclose N": close tab page N.
     */
    /*private*/ static final ex_func_C ex_tabclose = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (cmdwin_type != 0)
                cmdwin_result = K_IGNORE;
            else if (first_tabpage.tp_next == null)
                emsg(u8("E784: Cannot close last tab page"));
            else
            {
                if (0 < eap.addr_count)
                {
                    tabpage_C tp = find_tabpage((int)eap.line2);
                    if (tp == null)
                    {
                        beep_flush();
                        return;
                    }
                    if (tp != curtab)
                    {
                        tabpage_close_other(tp, eap.forceit);
                        return;
                    }
                }
                if (!text_locked() && !curbuf_locked())
                    tabpage_close(eap.forceit);
            }
        }
    };

    /*
     * ":tabonly": close all tab pages except the current one
     */
    /*private*/ static final ex_func_C ex_tabonly = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (cmdwin_type != 0)
                cmdwin_result = K_IGNORE;
            else if (first_tabpage.tp_next == null)
                msg(u8("Already only one tab page"));
            else
            {
                if (0 < eap.addr_count)
                    goto_tabpage((int)eap.line2);
                /* Repeat this up to a 1000 times, because autocommands may mess up the lists. */
                for (int done = 0; done < 1000; done++)
                {
                    for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
                        if (tp.tp_topframe != topframe)
                        {
                            tabpage_close_other(tp, eap.forceit);
                            /* if we failed to close it quit */
                            if (valid_tabpage(tp))
                                done = 1000;
                            /* start over, "tp" is now invalid */
                            break;
                        }
                    if (first_tabpage.tp_next == null)
                        break;
                }
            }
        }
    };

    /*
     * Close the current tab page.
     */
    /*private*/ static void tabpage_close(boolean forceit)
    {
        /* First close all the windows but the current one.
         * If that worked then close the last window in this tab, that will close it. */
        if (lastwin != firstwin)
            close_others(true, forceit);
        if (lastwin == firstwin)
            ex_win_close(forceit, curwin, null);
    }

    /*
     * Close tab page "tp", which is not the current tab page.
     * Note that autocommands may make "tp" invalid.
     * Also takes care of the tab pages line disappearing when closing the last-but-one tab page.
     */
    /*private*/ static void tabpage_close_other(tabpage_C tp, boolean forceit)
    {
        int h = tabline_height();

        /* Limit to 1000 windows, autocommands may add a window while we close one.
         * OK, so I'm paranoid... */
        for (int done = 0; ++done < 1000; )
        {
            window_C wp = tp.tp_firstwin;
            ex_win_close(forceit, wp, tp);

            /* Autocommands may delete the tab page under our fingers and
             * we may fail to close a window with a modified buffer. */
            if (!valid_tabpage(tp) || tp.tp_firstwin == wp)
                break;
        }

        redraw_tabline = true;
        if (h != tabline_height())
            shell_new_rows();
    }

    /*
     * ":only".
     */
    /*private*/ static final ex_func_C ex_only = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (0 < eap.addr_count)
            {
                int wnr = (int)eap.line2;
                window_C wp;
                for (wp = firstwin; 0 < --wnr; )
                {
                    if (wp.w_next == null)
                        break;
                    else
                        wp = wp.w_next;
                }
                win_goto(wp);
            }
            close_others(true, eap.forceit);
        }
    };

    /*
     * ":all" and ":sall".
     * Also used for ":tab drop file ..." after setting the argument list.
     */
    /*private*/ static final ex_func_C ex_all = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.addr_count == 0)
                eap.line2 = 9999;
            do_arg_all((int)eap.line2, eap.forceit, false);
        }
    };

    /*private*/ static final ex_func_C ex_hide = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.arg.at(0) != NUL && check_nextcmd(eap.arg) == null)
                eap.errmsg = e_invarg;
            else
            {
                /* ":hide" or ":hide | cmd": hide current window */
                eap.nextcmd = check_nextcmd(eap.arg);
                if (!eap.skip)
                {
                    if (eap.addr_count == 0)
                        win_close(curwin, false);       /* don't free buffer */
                    else
                    {
                        int winnr = 0;
                        window_C win;
                        for (win = firstwin; win != null; win = win.w_next)
                            if (++winnr == eap.line2)
                                break;
                        if (win == null)
                            win = lastwin;
                        win_close(win, false);
                    }
                }
            }
        }
    };

    /*
     * ":stop" and ":suspend": Suspend Vim.
     */
    /*private*/ static final ex_func_C ex_stop = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            /*
             * Disallow suspending for "rvim".
             */
            if (!check_restricted())
            {
                if (!eap.forceit)
                    autowrite_all();
                windgoto((int)Rows[0] - 1, 0);
                out_char((byte)'\n');
                out_flush();
                stoptermcap();
                out_flush();            /* needed for SUN to restore xterm buffer */
                ui_suspend();           /* call machine specific function */
                starttermcap();
                scroll_start();         /* scroll screen before redrawing */
                redraw_later_clear();
                shell_resized();        /* may have resized window */
            }
        }
    };

    /*
     * ":exit", ":xit" and ":wq": Write file and exit Vim.
     */
    /*private*/ static final ex_func_C ex_exit = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (cmdwin_type != 0)
            {
                cmdwin_result = Ctrl_C;
                return;
            }

            /* Don't quit while editing the command line. */
            if (text_locked())
            {
                text_locked_msg();
                return;
            }

            apply_autocmds(EVENT_QUITPRE, null, null, false, curbuf);
            /* Refuse to quit when locked or when the buffer in the last window
             * is being closed (can only happen in autocommands). */
            if (curbuf_locked() || (curbuf.b_nwindows == 1 && curbuf.b_closing))
                return;

            /*
             * if more files or windows we won't exit
             */
            if (check_more(false, eap.forceit) == true && only_one_window())
                exiting = true;
            if (((eap.cmdidx == CMD_wq || curbufIsChanged()) && do_write(eap) == false)
                    || check_more(true, eap.forceit) == false
                    || (only_one_window() && check_changed_any(eap.forceit)))
            {
                not_exiting();
            }
            else
            {
                if (only_one_window())      /* quit last window, exit Vim */
                    getout(0);
                /* Quit current window, may free the buffer. */
                win_close(curwin, !P_HID(curwin.w_buffer));
            }
        }
    };

    /*
     * ":print", ":list", ":number".
     */
    /*private*/ static final ex_func_C ex_print = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0)
                emsg(e_emptybuf);
            else
            {
                for ( ; !got_int; ui_breakcheck())
                {
                    print_line(eap.line1,
                            eap.cmdidx == CMD_number || eap.cmdidx == CMD_pound || (eap.flags & EXFLAG_NR) != 0,
                            eap.cmdidx == CMD_list || (eap.flags & EXFLAG_LIST) != 0);
                    if (eap.line2 < ++eap.line1)
                        break;
                    out_flush();            /* show one line at a time */
                }
                setpcmark();
                /* put cursor at last line */
                curwin.w_cursor.lnum = eap.line2;
                beginline(BL_SOL | BL_FIX);
            }

            ex_no_reprint = true;
        }
    };

    /*private*/ static final ex_func_C ex_goto = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            goto_byte(eap.line2);
        }
    };

    /*
     * Clear an argument list: free all file names and reset it to zero entries.
     */
    /*private*/ static void alist_clear(alist_C al)
    {
        for (aentry_C[] aep = al.al_ga.ga_data; 0 < al.al_ga.ga_len; )
            aep[--al.al_ga.ga_len] = null;
        al.al_ga.ga_clear();
    }

    /*
     * Remove a reference from an argument list.
     * Ignored when the argument list is the global one.
     * If the argument list is no longer used by any window, free it.
     */
    /*private*/ static void alist_unlink(alist_C al)
    {
        if (al != global_alist && --al.al_refcount <= 0)
            alist_clear(al);
    }

    /*
     * Create a new argument list and use it for the current window.
     */
    /*private*/ static alist_C alist_new()
    {
        alist_C al = new alist_C();

        al.al_refcount = 1;
        al.id = ++max_alist_id;

        return al;
    }

    /*
     * Set the argument list for the current window.
     * Takes over the allocated files[] and the allocated fnames in it.
     */
    /*private*/ static void alist_set(alist_C al, int count, Bytes[] files, boolean use_curbuf, int[] fnum_list, int fnum_len)
    {
        alist_clear(al);

        for (int i = 0; i < count; i++)
        {
            if (got_int)
            {
                /* When adding many buffers this can take a long time.
                 * Allow interrupting here. */
                while (i < count)
                    files[i++] = null;
                break;
            }

            /* May set buffer name of a buffer previously used for the argument list,
             * so that it's re-used by alist_add(). */
            if (fnum_list != null && i < fnum_len)
                buf_set_name(fnum_list[i], files[i]);

            alist_add(al, files[i], use_curbuf ? 2 : 1);

            ui_breakcheck();
        }

        if (al == global_alist)
            arg_had_last = false;
    }

    /*
     * Add file "fname" to argument list "al".
     * "fname" must have been allocated and "al" must have been checked for room.
     */
    /*private*/ static void alist_add(alist_C al, Bytes fname, int set_fnum)
        /* set_fnum: 1: set buffer number; 2: re-use curbuf */
    {
        if (fname != null)          /* don't add null file names */
        {
            aentry_C[] aep = al.al_ga.ga_grow(1);

            aep[al.al_ga.ga_len] = new aentry_C();
            aep[al.al_ga.ga_len].ae_fname = fname;
            if (0 < set_fnum)
                aep[al.al_ga.ga_len].ae_fnum = buflist_add(fname, BLN_LISTED | (set_fnum == 2 ? BLN_CURBUF : 0));

            al.al_ga.ga_len++;
        }
    }

    /*
     * Command modifier used in a wrong way.
     */
    /*private*/ static final ex_func_C ex_wrongmodifier = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            eap.errmsg = e_invcmd;
        }
    };

    /*
     * :sview [+command] file       split window with new file, read-only
     * :split [[+command] file]     split window with current or new file
     * :vsplit [[+command] file]    split window vertically with current or new file
     * :new [[+command] file]       split window with no or new file
     * :vnew [[+command] file]      split vertically window with no or new file
     *
     * :tabedit                     open new Tab page with empty window
     * :tabedit [+command] file     open new Tab page and edit "file"
     * :tabnew [[+command] file]    just like :tabedit
     */
    /*private*/ static final ex_func_C ex_splitview = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            window_C old_curwin = curwin;

            /*
             * Either open new tab page or split the window.
             */
            if (eap.cmdidx == CMD_tabedit || eap.cmdidx == CMD_tabnew)
            {
                if (win_new_tabpage((cmdmod.tab != 0) ? cmdmod.tab : (eap.addr_count == 0) ? 0 : (int)eap.line2 + 1) != false)
                {
                    do_exedit(eap, old_curwin);

                    /* set the alternate buffer for the window we came from */
                    if (curwin != old_curwin
                            && win_valid(old_curwin)
                            && old_curwin.w_buffer != curbuf
                            && !cmdmod.keepalt)
                        old_curwin.w_alt_fnum = curbuf.b_fnum;
                }
            }
            else if (win_split((0 < eap.addr_count) ? (int)eap.line2 : 0, (eap.cmd.at(0) == (byte)'v') ? WSP_VERT : 0) != false)
            {
                /* Reset 'scrollbind' when editing another file,
                 * but keep it when doing ":split" without arguments. */
                if (eap.arg.at(0) != NUL)
                {
                    curwin.w_onebuf_opt.wo_scb[0] = false;
                    curwin.w_onebuf_opt.wo_crb[0] = false;
                }
                else
                    do_check_scrollbind(false);
                do_exedit(eap, old_curwin);
            }
        }
    };

    /*
     * Open a new tab page.
     */
    /*private*/ static void tabpage_new()
    {
        exarg_C ea = new exarg_C();

        ea.cmdidx = CMD_tabnew;
        ea.cmd = u8("tabn");
        ea.arg = u8("");

        ex_splitview.ex(ea);
    }

    /*
     * :tabnext command
     */
    /*private*/ static final ex_func_C ex_tabnext = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            switch (eap.cmdidx)
            {
                case CMD_tabfirst:
                case CMD_tabrewind:
                    goto_tabpage(1);
                    break;
                case CMD_tablast:
                    goto_tabpage(9999);
                    break;
                case CMD_tabprevious:
                case CMD_tabNext:
                    goto_tabpage((eap.addr_count == 0) ? -1 : -(int)eap.line2);
                    break;
                default: /* CMD_tabnext */
                    goto_tabpage((eap.addr_count == 0) ? 0 : (int)eap.line2);
                    break;
            }
        }
    };

    /*
     * :tabmove command
     */
    /*private*/ static final ex_func_C ex_tabmove = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int tab_number = 9999;

            if (eap.arg != null && eap.arg.at(0) != NUL)
            {
                Bytes p = eap.arg;
                int relative = 0;           /* argument +N/-N means: move N places to the
                                             * right/left relative to the current position. */

                if (eap.arg.at(0) == (byte)'-')
                {
                    relative = -1;
                    p = eap.arg.plus(1);
                }
                else if (eap.arg.at(0) == (byte)'+')
                {
                    relative = 1;
                    p = eap.arg.plus(1);
                }
                else
                    p = eap.arg;

                if (BEQ(p, skipdigits(p)))
                {
                    /* No numbers as argument. */
                    eap.errmsg = e_invarg;
                    return;
                }

                { Bytes[] __ = { p }; tab_number = (int)getdigits(__); p = __[0]; }
                if (relative != 0)
                    tab_number = tab_number * relative + tabpage_index(curtab) - 1;
            }
            else if (eap.addr_count != 0)
                tab_number = (int)eap.line2;

            tabpage_move(tab_number);
        }
    };

    /*
     * :tabs command: List tabs and their contents.
     */
    /*private*/ static final ex_func_C ex_tabs = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            msg_start();
            msg_scroll = true;

            int tabcount = 1;

            for (tabpage_C tp = first_tabpage; tp != null && !got_int; tp = tp.tp_next)
            {
                msg_putchar('\n');
                vim_snprintf(ioBuff, IOSIZE, u8("Tab page %d"), tabcount++);
                msg_outtrans_attr(ioBuff, hl_attr(HLF_T));
                out_flush();        /* output one line at a time */
                ui_breakcheck();

                for (window_C wp = (tp == curtab) ? firstwin : tp.tp_firstwin; wp != null && !got_int; wp = wp.w_next)
                {
                    msg_putchar('\n');
                    msg_putchar(wp == curwin ? '>' : ' ');
                    msg_putchar(' ');
                    msg_putchar(bufIsChanged(wp.w_buffer) ? '+' : ' ');
                    msg_putchar(' ');
                    msg_outtrans(buf_spname(wp.w_buffer, false));
                    out_flush();            /* output one line at a time */
                    ui_breakcheck();
                }
            }
        }
    };

    /*
     * ":mode": Set screen mode.
     * If no argument given, just get the screen size and redraw.
     */
    /*private*/ static final ex_func_C ex_mode = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.arg.at(0) == NUL)
                shell_resized();
            else
                mch_screenmode(eap.arg);
        }
    };

    /*
     * ":resize".
     * set, increment or decrement current window height
     */
    /*private*/ static final ex_func_C ex_resize = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            window_C wp = curwin;

            if (0 < eap.addr_count)
            {
                int n = (int)eap.line2;
                for (wp = firstwin; wp.w_next != null && 0 < --n; wp = wp.w_next)
                    ;
            }

            int n = libC.atoi(eap.arg);
            if ((cmdmod.split & WSP_VERT) != 0)
            {
                if (eap.arg.at(0) == (byte)'-' || eap.arg.at(0) == (byte)'+')
                    n += curwin.w_width;
                else if (n == 0 && eap.arg.at(0) == NUL)   /* default is very wide */
                    n = 9999;
                win_setwidth_win(n, wp);
            }
            else
            {
                if (eap.arg.at(0) == (byte)'-' || eap.arg.at(0) == (byte)'+')
                    n += curwin.w_height;
                else if (n == 0 && eap.arg.at(0) == NUL)   /* default is very wide */
                    n = 9999;
                win_setheight_win(n, wp);
            }
        }
    };

    /*
     * ":open" simulation: for now just work like ":visual".
     */
    /*private*/ static final ex_func_C ex_open = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            curwin.w_cursor.lnum = eap.line2;
            beginline(BL_SOL | BL_FIX);
            if (eap.arg.at(0) == (byte)'/')
            {
                /* ":open /pattern/": put cursor in column found with pattern */
                eap.arg = eap.arg.plus(1);
                Bytes p = skip_regexp(eap.arg, (byte)'/', p_magic[0], null);
                p.be(0, NUL);
                regmatch_C regmatch = new regmatch_C();
                regmatch.regprog = vim_regcomp(eap.arg, p_magic[0] ? RE_MAGIC : 0);
                if (regmatch.regprog != null)
                {
                    regmatch.rm_ic = p_ic[0];
                    p = ml_get_curline();
                    if (vim_regexec(regmatch, p, 0))
                        curwin.w_cursor.col = BDIFF(regmatch.startp[0], p);
                    else
                        emsg(e_nomatch);
                }
                /* Move to the NUL, ignore any other arguments. */
                eap.arg = eap.arg.plus(strlen(eap.arg));
            }
            check_cursor();

            eap.cmdidx = CMD_visual;
            do_exedit(eap, null);
        }
    };

    /*
     * ":edit", ":badd", ":visual".
     */
    /*private*/ static final ex_func_C ex_edit = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            do_exedit(eap, null);
        }
    };

    /*
     * ":edit <file>" command and alikes.
     */
    /*private*/ static void do_exedit(exarg_C eap, window_C old_curwin)
        /* old_curwin: curwin before doing a split or null */
    {
        int exmode_was = exmode_active;

        /*
         * ":vi" command ends Ex mode.
         */
        if (exmode_active != 0 && (eap.cmdidx == CMD_visual || eap.cmdidx == CMD_view))
        {
            exmode_active = 0;
            if (eap.arg.at(0) == NUL)
            {
                /* Special case: ":global/pat/visual\NLvi-commands". */
                if (global_busy != 0)
                {
                    int rd = redrawingDisabled;
                    int nwr = no_wait_return;
                    boolean ms = msg_scroll;

                    if (eap.nextcmd != null)
                    {
                        stuffReadbuff(eap.nextcmd);
                        eap.nextcmd = null;
                    }

                    if (exmode_was != EXMODE_VIM)
                        settmode(TMODE_RAW);
                    redrawingDisabled = 0;
                    no_wait_return = FALSE;
                    need_wait_return = false;
                    msg_scroll = false;
                    must_redraw = CLEAR;

                    main_loop(false, true);

                    redrawingDisabled = rd;
                    no_wait_return = nwr;
                    msg_scroll = ms;
                }
                return;
            }
        }

        if ((eap.cmdidx == CMD_new
                    || eap.cmdidx == CMD_tabnew
                    || eap.cmdidx == CMD_tabedit
                    || eap.cmdidx == CMD_vnew
                    ) && eap.arg.at(0) == NUL)
        {
            /* ":new" or ":tabnew" without argument: edit an new empty buffer */
            setpcmark();
            do_ecmd(0, null, null, eap, ECMD_ONE,
                          ECMD_HIDE + (eap.forceit ? ECMD_FORCEIT : 0),
                          old_curwin == null ? curwin : null);
        }
        else if ((eap.cmdidx != CMD_split && eap.cmdidx != CMD_vsplit) || eap.arg.at(0) != NUL)
        {
            /* Can't edit another file when "curbuf_lock" is set.
             * Only ":edit" can bring us here, others are stopped earlier. */
            if (eap.arg.at(0) != NUL && curbuf_locked())
                return;
            boolean rom = readonlymode;
            if (eap.cmdidx == CMD_view || eap.cmdidx == CMD_sview)
                readonlymode = true;
            else if (eap.cmdidx == CMD_enew)
                readonlymode = false;   /* 'readonly' doesn't make sense in an empty buffer */
            setpcmark();
            if (do_ecmd(0, (eap.cmdidx == CMD_enew) ? null : eap.arg,
                        null, eap,
                        /* ":edit" goes to first line if Vi compatible */
                        (eap.arg.at(0) == NUL && eap.do_ecmd_lnum == 0
                                && vim_strbyte(p_cpo[0], CPO_GOTO1) != null) ? ECMD_ONE : eap.do_ecmd_lnum,
                        (P_HID(curbuf) ? ECMD_HIDE : 0)
                            + (eap.forceit ? ECMD_FORCEIT : 0)
                            /* after a split we can use an existing buffer */
                            + (old_curwin != null ? ECMD_OLDBUF : 0)
                            + (eap.cmdidx == CMD_badd ? ECMD_ADDBUF : 0),
                        (old_curwin == null) ? curwin : null) == false)
            {
                /* Editing the file failed.  If the window was split, close it. */
                if (old_curwin != null)
                {
                    boolean need_hide = (curbufIsChanged() && curbuf.b_nwindows <= 1);
                    if (!need_hide || P_HID(curbuf))
                    {
                        cleanup_C cs = new cleanup_C();

                        /* Reset the error/interrupt/exception state here so that
                         * aborting() returns false when closing a window. */
                        enter_cleanup(cs);
                        win_close(curwin, !need_hide && !P_HID(curbuf));

                        /* Restore the error/interrupt/exception state if not discarded
                         * by a new aborting error, interrupt, or uncaught exception. */
                        leave_cleanup(cs);
                    }
                }
            }
            else if (readonlymode && curbuf.b_nwindows == 1)
            {
                /* When editing an already visited buffer, 'readonly' won't be set
                 * but the previous value is kept.  With ":view" and ":sview" we
                 * want the  file to be readonly, except when another window is
                 * editing the same buffer. */
                curbuf.b_p_ro[0] = true;
            }
            readonlymode = rom;
        }
        else
        {
            if (eap.do_ecmd_cmd != null)
                do_cmdline_cmd(eap.do_ecmd_cmd);
            check_arg_idx(curwin);
        }

        /*
         * if ":split file" worked, set alternate file name in old window to new file
         */
        if (old_curwin != null
                && eap.arg.at(0) != NUL
                && curwin != old_curwin
                && win_valid(old_curwin)
                && old_curwin.w_buffer != curbuf
                && !cmdmod.keepalt)
            old_curwin.w_alt_fnum = curbuf.b_fnum;

        ex_no_reprint = true;
    }

    /*
     * ":syncbind" forces all 'scrollbind' windows to have the same relative offset.
     */
    /*private*/ static final ex_func_C ex_syncbind = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            window_C save_curwin = curwin;
            buffer_C save_curbuf = curbuf;
            long old_linenr = curwin.w_cursor.lnum;

            setpcmark();

            long topline;
            /* determine max topline */
            if (curwin.w_onebuf_opt.wo_scb[0])
            {
                topline = curwin.w_topline;
                for (window_C wp = firstwin; wp != null; wp = wp.w_next)
                {
                    if (wp.w_onebuf_opt.wo_scb[0] && wp.w_buffer != null)
                    {
                        long y = wp.w_buffer.b_ml.ml_line_count - p_so[0];
                        if (y < topline)
                            topline = y;
                    }
                }
                if (topline < 1)
                    topline = 1;
            }
            else
            {
                topline = 1;
            }

            /*
             * Set all scrollbind windows to the same topline.
             */
            for (curwin = firstwin; curwin != null; curwin = curwin.w_next)
            {
                if (curwin.w_onebuf_opt.wo_scb[0])
                {
                    curbuf = curwin.w_buffer;
                    long y = topline - curwin.w_topline;
                    if (0 < y)
                        scrollup(y);
                    else
                        scrolldown(-y);
                    curwin.w_scbind_pos = topline;
                    redraw_later(VALID);
                    cursor_correct();
                    curwin.w_redr_status = true;
                }
            }
            curwin = save_curwin;
            curbuf = save_curbuf;
            if (curwin.w_onebuf_opt.wo_scb[0])
            {
                did_syncbind = true;
                checkpcmark();
                if (old_linenr != curwin.w_cursor.lnum)
                {
                    Bytes ctrl_o = new Bytes(2);

                    ctrl_o.be(0, Ctrl_O);
                    ctrl_o.be(1, NUL);
                    ins_typebuf(ctrl_o, REMAP_NONE, 0, true, false);
                }
            }
        }
    };

    /*private*/ static final ex_func_C ex_read = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            boolean empty = ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0);

            if (eap.usefilter)                      /* :r!cmd */
                do_bang(1, eap, false, false, true);
            else
            {
                if (!u_save(eap.line2, eap.line2 + 1))
                    return;

                boolean i;
                if (eap.arg.at(0) == NUL)
                {
                    if (check_fname() == false)     /* check for no file name */
                        return;
                    i = readfile(curbuf.b_ffname, curbuf.b_fname, eap.line2, 0, MAXLNUM, eap, 0);
                }
                else
                {
                    if (vim_strbyte(p_cpo[0], CPO_ALTREAD) != null)
                        setaltfname(eap.arg, eap.arg, 1);
                    i = readfile(eap.arg, null, eap.line2, 0, MAXLNUM, eap, 0);
                }
                if (i == false)
                {
                    if (!aborting())
                        emsg2(e_notopen, eap.arg);
                }
                else
                {
                    if (empty && exmode_active != 0)
                    {
                        long lnum;
                        /* Delete the empty line that remains.
                         * Historically ex does this but vi doesn't. */
                        if (eap.line2 == 0)
                            lnum = curbuf.b_ml.ml_line_count;
                        else
                            lnum = 1;
                        if (ml_get(lnum).at(0) == NUL && u_savedel(lnum, 1L) == true)
                        {
                            ml_delete(lnum, false);
                            if (1 < curwin.w_cursor.lnum && lnum <= curwin.w_cursor.lnum)
                                --curwin.w_cursor.lnum;
                            deleted_lines_mark(lnum, 1L);
                        }
                    }
                    redraw_curbuf_later(VALID);
                }
            }
        }
    };

    /*
     * ":=".
     */
    /*private*/ static final ex_func_C ex_equal = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            smsg(u8("%ld"), eap.line2);
            ex_may_print(eap);
        }
    };

    /*private*/ static final ex_func_C ex_sleep = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (cursor_valid())
            {
                int n = curwin.w_winrow + curwin.w_wrow - msg_scrolled;
                if (0 <= n)
                    windgoto(n, curwin.w_wincol + curwin.w_wcol);
            }

            long len = eap.line2;
            switch (eap.arg.at(0))
            {
                case 'm': break;
                case NUL: len *= 1000L; break;
                default: emsg2(e_invarg2, eap.arg); return;
            }
            do_sleep(len);
        }
    };

    /*
     * Sleep for "msec" milliseconds, but keep checking for a CTRL-C every second.
     */
    /*private*/ static void do_sleep(long msec)
    {
        cursor_on();
        out_flush();
        for (long done = 0; !got_int && done < msec; done += 1000L)
        {
            ui_delay(1000L < msec - done ? 1000L : msec - done, true);
            ui_breakcheck();
        }
    }

    /*private*/ static void do_exmap(exarg_C eap, boolean isabbrev)
    {
        Bytes[] cmdp = { eap.cmd };
        int mode = get_map_mode(cmdp, eap.forceit || isabbrev);

        switch (do_map((cmdp[0].at(0) == (byte)'n') ? 2 : (cmdp[0].at(0) == (byte)'u') ? 1 : 0, eap.arg, mode, isabbrev))
        {
            case 1: emsg(e_invarg);
                    break;
            case 2: emsg(isabbrev ? e_noabbr : e_nomap);
                    break;
        }
    }

    /*
     * ":winsize" command (obsolete).
     */
    /*private*/ static final ex_func_C ex_winsize = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes[] arg = { eap.arg };

            int w = (int)getdigits(arg);
            arg[0] = skipwhite(arg[0]);
            Bytes p = arg[0];
            int h = (int)getdigits(arg);
            if (p.at(0) != NUL && arg[0].at(0) == NUL)
                set_shellsize(w, h, true);
            else
                emsg(u8("E465: :winsize requires two number arguments"));
        }
    };

    /*private*/ static final ex_func_C ex_wincmd = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int xchar = NUL;
            Bytes p;

            if (eap.arg.at(0) == (byte)'g' || eap.arg.at(0) == Ctrl_G)
            {
                /* CTRL-W g and CTRL-W CTRL-G have an extra command character. */
                if (eap.arg.at(1) == NUL)
                {
                    emsg(e_invarg);
                    return;
                }
                xchar = eap.arg.at(1);
                p = eap.arg.plus(2);
            }
            else
                p = eap.arg.plus(1);

            eap.nextcmd = check_nextcmd(p);
            p = skipwhite(p);
            if (p.at(0) != NUL && p.at(0) != (byte)'"' && eap.nextcmd == null)
                emsg(e_invarg);
            else if (!eap.skip)
            {
                /* Pass flags on for ":vertical wincmd ]". */
                postponed_split_flags = cmdmod.split;
                postponed_split_tab = cmdmod.tab;
                do_window(eap.arg.at(0), (0 < eap.addr_count) ? eap.line2 : 0L, xchar);
                postponed_split_flags = 0;
                postponed_split_tab = 0;
            }
        }
    };

    /*
     * ":winpos".
     */
    /*private*/ static final ex_func_C ex_winpos = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes[] arg = { eap.arg };

            if (arg[0].at(0) == NUL)
            {
                emsg(u8("E188: Obtaining window position not implemented for this platform"));
                return;
            }

            int x = (int)getdigits(arg);
            arg[0] = skipwhite(arg[0]);
            Bytes p = arg[0];
            int y = (int)getdigits(arg);
            if (p.at(0) == NUL || arg[0].at(0) != NUL)
            {
                emsg(u8("E466: :winpos requires two number arguments"));
                return;
            }

            if (T_CWP[0].at(0) != NUL)
                term_set_winpos(x, y);
        }
    };

    /*
     * Handle command that work like operators: ":delete", ":yank", ":>" and ":<".
     */
    /*private*/ static final ex_func_C ex_operators = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            oparg_C oa = new oparg_C();

            oa.regname = eap.regname;
            oa.op_start.lnum = eap.line1;
            oa.op_end.lnum = eap.line2;
            oa.line_count = eap.line2 - eap.line1 + 1;
            oa.motion_type = MLINE;
            virtual_op = FALSE;

            if (eap.cmdidx != CMD_yank)     /* position cursor for undo */
            {
                setpcmark();
                curwin.w_cursor.lnum = eap.line1;
                beginline(BL_SOL | BL_FIX);
            }

            if (VIsual_active)
                end_visual_mode();

            switch (eap.cmdidx)
            {
                case CMD_delete:
                    oa.op_type = OP_DELETE;
                    op_delete(oa);
                    break;

                case CMD_yank:
                    oa.op_type = OP_YANK;
                    op_yank(oa, false, true);
                    break;

                default:    /* CMD_rshift or CMD_lshift */
                    if ((eap.cmdidx == CMD_rshift) ^ curwin.w_onebuf_opt.wo_rl[0])
                        oa.op_type = OP_RSHIFT;
                    else
                        oa.op_type = OP_LSHIFT;
                    op_shift(oa, false, eap.amount);
                    break;
            }
            virtual_op = MAYBE;
            ex_may_print(eap);
        }
    };

    /*
     * ":put".
     */
    /*private*/ static final ex_func_C ex_put = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            /* ":0put" works like ":1put!". */
            if (eap.line2 == 0)
            {
                eap.line2 = 1;
                eap.forceit = true;
            }
            curwin.w_cursor.lnum = eap.line2;
            do_put(eap.regname, eap.forceit ? BACKWARD : FORWARD, 1, PUT_LINE|PUT_CURSLINE);
        }
    };

    /*
     * Handle ":copy" and ":move".
     */
    /*private*/ static final ex_func_C ex_copymove = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            long n;
            { Bytes[] __ = { eap.arg }; n = get_address(__, eap.addr_type, false, false); eap.arg = __[0]; }
            if (eap.arg == null)        /* error detected */
            {
                eap.nextcmd = null;
                return;
            }
            get_flags(eap);

            /* move or copy lines from 'eap.line1'-'eap.line2' to below line 'n' */
            if (n == MAXLNUM || n < 0 || curbuf.b_ml.ml_line_count < n)
            {
                emsg(e_invaddr);
                return;
            }

            if (eap.cmdidx == CMD_move)
            {
                if (do_move(eap.line1, eap.line2, n) == false)
                    return;
            }
            else
                ex_copy(eap.line1, eap.line2, n);
            u_clearline();
            beginline(BL_SOL | BL_FIX);
            ex_may_print(eap);
        }
    };

    /*
     * Print the current line if flags were given to the Ex command.
     */
    /*private*/ static void ex_may_print(exarg_C eap)
    {
        if (eap.flags != 0)
        {
            print_line(curwin.w_cursor.lnum, (eap.flags & EXFLAG_NR) != 0, (eap.flags & EXFLAG_LIST) != 0);
            ex_no_reprint = true;
        }
    }

    /*
     * ":smagic" and ":snomagic".
     */
    /*private*/ static final ex_func_C ex_submagic = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            boolean magic_save = p_magic[0];

            p_magic[0] = (eap.cmdidx == CMD_smagic);
            ex_sub.ex(eap);
            p_magic[0] = magic_save;
        }
    };

    /*
     * ":join".
     */
    /*private*/ static final ex_func_C ex_join = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            curwin.w_cursor.lnum = eap.line1;
            if (eap.line1 == eap.line2)
            {
                if (2 <= eap.addr_count)    /* :2,2join does nothing */
                    return;
                if (eap.line2 == curbuf.b_ml.ml_line_count)
                {
                    beep_flush();
                    return;
                }
                eap.line2++;
            }
            do_join((int)(eap.line2 - eap.line1 + 1), !eap.forceit, true, true, true);
            beginline(BL_WHITE | BL_FIX);
            ex_may_print(eap);
        }
    };

    /*
     * ":[addr]@r" or ":[addr]*r": execute register
     */
    /*private*/ static final ex_func_C ex_at = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int prev_len = typebuf.tb_len;

            curwin.w_cursor.lnum = eap.line2;

            /* Get the register name.  No name means to use the previous one. */
            byte c = eap.arg.at(0);
            if (c == NUL || (c == '*' && eap.cmd.at(0) == (byte)'*'))
                c = '@';
            /* Put the register in the typeahead buffer with the "silent" flag. */
            if (do_execreg(c, true, vim_strbyte(p_cpo[0], CPO_EXECBUF) != null, true) == false)
            {
                beep_flush();
            }
            else
            {
                boolean save_efr = exec_from_reg;

                exec_from_reg = true;

                /*
                 * Execute from the typeahead buffer.
                 * Continue until the stuff buffer is empty and all added characters have been consumed.
                 */
                while (!stuff_empty() || prev_len < typebuf.tb_len)
                    do_cmdline(null, getexline, null, DOCMD_NOWAIT|DOCMD_VERBOSE);

                exec_from_reg = save_efr;
            }
        }
    };

    /*
     * ":!".
     */
    /*private*/ static final ex_func_C ex_bang = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            do_bang(eap.addr_count, eap, eap.forceit, true, true);
        }
    };

    /*
     * ":undo".
     */
    /*private*/ static final ex_func_C ex_undo = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.addr_count == 1)    /* :undo 123 */
                undo_time(eap.line2, false, false, true);
            else
                u_undo(1);
        }
    };

    /*private*/ static final ex_func_C ex_wundo = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes hash = new Bytes(UNDO_HASH_SIZE);

            u_compute_hash(hash);
            u_write_undo(eap.arg, eap.forceit, curbuf, hash);
        }
    };

    /*private*/ static final ex_func_C ex_rundo = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes hash = new Bytes(UNDO_HASH_SIZE);

            u_compute_hash(hash);
            u_read_undo(eap.arg, hash, null);
        }
    };

    /*
     * ":redo".
     */
    /*private*/ static final ex_func_C ex_redo = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            u_redo(1);
        }
    };

    /*
     * ":earlier" and ":later".
     */
    /*private*/ static final ex_func_C ex_later = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            long count = 0;
            boolean sec = false;
            boolean file = false;
            Bytes p = eap.arg;

            if (p.at(0) == NUL)
                count = 1;
            else if (asc_isdigit(p.at(0)))
            {
                { Bytes[] __ = { p }; count = getdigits(__); p = __[0]; }
                switch (p.at(0))
                {
                    case 's': p = p.plus(1); sec = true; break;
                    case 'm': p = p.plus(1); sec = true; count *= 60; break;
                    case 'h': p = p.plus(1); sec = true; count *= 60 * 60; break;
                    case 'd': p = p.plus(1); sec = true; count *= 24 * 60 * 60; break;
                    case 'f': p = p.plus(1); file = true; break;
                }
            }

            if (p.at(0) != NUL)
                emsg2(e_invarg2, eap.arg);
            else
                undo_time(eap.cmdidx == CMD_earlier ? -count : count, sec, file, false);
        }
    };

    /*
     * ":redir": start/stop redirection.
     */
    /*private*/ static final ex_func_C ex_redir = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes arg = eap.arg;

            if (STRCASECMP(eap.arg, u8("END")) == 0)
                close_redir();
            else
            {
                if (arg.at(0) == (byte)'>')
                {
                    arg = arg.plus(1);

                    close_redir();

                    Bytes mode;
                    if (arg.at(0) == (byte)'>')
                    {
                        arg = arg.plus(1);
                        mode = u8("a");
                    }
                    else
                        mode = u8("w");

                    arg = skipwhite(arg);

                    redir_fd = open_exfile(arg, eap.forceit, mode);
                }
                else if (arg.at(0) == (byte)'@')
                {
                    arg = arg.plus(1);

                    close_redir();

                    /* redirect to a register a-z (resp. A-Z for appending) */
                    if (asc_isalpha(arg.at(0))
                            || arg.at(0) == (byte)'*'
                            || arg.at(0) == (byte)'+'
                            || arg.at(0) == (byte)'"')
                    {
                        redir_reg = (arg = arg.plus(1)).at(-1);
                        if (arg.at(0) == (byte)'>' && arg.at(1) == (byte)'>') /* append */
                            arg = arg.plus(2);
                        else
                        {
                            /* Can use both "@a" and "@a>". */
                            if (arg.at(0) == (byte)'>')
                                arg = arg.plus(1);
                            /* Make register empty when not using @A-@Z and the command is valid. */
                            if (arg.at(0) == NUL && !asc_isupper(redir_reg))
                                write_reg_contents(redir_reg, u8(""), -1, false);
                        }
                    }
                    if (arg.at(0) != NUL)
                    {
                        redir_reg = 0;
                        emsg2(e_invarg2, eap.arg);
                    }
                }
                else if (arg.at(0) == (byte)'=' && arg.at(1) == (byte)'>')
                {
                    arg = arg.plus(2);

                    close_redir();

                    /* redirect to a variable */
                    boolean append;
                    if (arg.at(0) == (byte)'>')
                    {
                        arg = arg.plus(1);
                        append = true;
                    }
                    else
                        append = false;

                    if (var_redir_start(skipwhite(arg), append) == true)
                        redir_vname = true;
                }

                /* TODO: redirect to a buffer */

                else
                    emsg2(e_invarg2, eap.arg);
            }

            /* Make sure redirection is not off.  Can happen for cmdline completion
             * that indirectly invokes a command to catch its output. */
            if (redir_fd != null || redir_reg != 0 || redir_vname)
                redir_off = false;
        }
    };

    /*
     * ":redraw": force redraw
     */
    /*private*/ static final ex_func_C ex_redraw = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int r = redrawingDisabled;
            boolean p = p_lz[0];

            redrawingDisabled = 0;
            p_lz[0] = false;
            update_topline();
            update_screen(eap.forceit ? CLEAR : VIsual_active ? INVERTED : 0);
            redrawingDisabled = r;
            p_lz[0] = p;

            /* Reset msg_didout, so that a message that's there is overwritten. */
            msg_didout = false;
            msg_col = 0;

            /* No need to wait after an intentional redraw. */
            need_wait_return = false;

            out_flush();
        }
    };

    /*
     * ":redrawstatus": force redraw of status line(s)
     */
    /*private*/ static final ex_func_C ex_redrawstatus = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int r = redrawingDisabled;
            boolean p = p_lz[0];

            redrawingDisabled = 0;
            p_lz[0] = false;
            if (eap.forceit)
                status_redraw_all();
            else
                status_redraw_curbuf();
            update_screen(VIsual_active ? INVERTED : 0);
            redrawingDisabled = r;
            p_lz[0] = p;
            out_flush();
        }
    };

    /*private*/ static void close_redir()
    {
        if (redir_fd != null)
        {
            libc.fclose(redir_fd);
            redir_fd = null;
        }
        redir_reg = 0;
        if (redir_vname)
        {
            var_redir_stop();
            redir_vname = false;
        }
    }

    /*
     * Open a file for writing for an Ex command, with some checks.
     * Return file descriptor, or null on failure.
     */
    /*private*/ static file_C open_exfile(Bytes fname, boolean forceit, Bytes mode)
        /* mode: "w" for create new file or "a" for append */
    {
        /* with Unix it is possible to open a directory */
        if (mch_isdir(fname))
        {
            emsg2(e_isadir2, fname);
            return null;
        }

        if (!forceit && mode.at(0) != (byte)'a' && vim_fexists(fname))
        {
            emsg2(u8("E189: \"%s\" exists (add ! to override)"), fname);
            return null;
        }

        file_C fd = libC.fopen(fname, mode);

        if (fd == null)
            emsg2(u8("E190: Cannot open \"%s\" for writing"), fname);

        return fd;
    }

    /*
     * ":mark" and ":k".
     */
    /*private*/ static final ex_func_C ex_mark = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.arg.at(0) == NUL)                    /* No argument? */
                emsg(e_argreq);
            else if (eap.arg.at(1) != NUL)             /* more than one character? */
                emsg(e_trailing);
            else
            {
                pos_C pos = new pos_C();
                COPY_pos(pos, curwin.w_cursor);     /* save curwin.w_cursor */
                curwin.w_cursor.lnum = eap.line2;
                beginline(BL_WHITE | BL_FIX);
                if (setmark(eap.arg.at(0)) == false)     /* set mark */
                    emsg(u8("E191: Argument must be a letter or forward/backward quote"));
                COPY_pos(curwin.w_cursor, pos);     /* restore curwin.w_cursor */
            }
        }
    };

    /*
     * Update w_topline, w_leftcol and the cursor position.
     */
    /*private*/ static void update_topline_cursor()
    {
        check_cursor();             /* put cursor on valid line */
        update_topline();
        if (!curwin.w_onebuf_opt.wo_wrap[0])
            validate_cursor();
        update_curswant();
    }

    /*
     * ":normal[!] {commands}": Execute normal mode commands.
     */
    /*private*/ static final ex_func_C ex_normal = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            boolean save_msg_didout = msg_didout;
            int save_State = State;
            boolean save_finish_op = finish_op;
            long save_opcount = opcount;

            if (0 < ex_normal_lock)
            {
                emsg(e_secure);
                return;
            }
            if (p_mmd[0] <= ex_normal_busy)
            {
                emsg(u8("E192: Recursive use of :normal too deep"));
                return;
            }

            ex_normal_busy++;

            boolean save_msg_scroll = msg_scroll;
            int save_restart_edit = restart_edit;
            boolean save_insertmode = p_im[0];

            msg_scroll = false;     /* no msg scrolling in Normal mode */
            restart_edit = 0;       /* don't go to Insert mode */
            p_im[0] = false;           /* don't use 'insertmode' */

            /*
             * vgetc() expects a CSI and KB_SPECIAL to have been escaped.
             * Don't do this for the KB_SPECIAL leading byte, otherwise special keys will not work.
             */

            /* Count the number of characters to be escaped. */
            int len = 0;
            for (Bytes p = eap.arg; p.at(0) != NUL; p = p.plus(1))
            {
                for (int l = us_ptr2len_cc(p) - 1; 0 < l; --l)
                    if ((p = p.plus(1)).at(0) == KB_SPECIAL)                     /* trailbyte KB_SPECIAL or CSI */
                        len += 2;
            }
            Bytes arg = null;
            if (0 < len)
            {
                arg = new Bytes(strlen(eap.arg) + len + 1);

                len = 0;
                for (Bytes p = eap.arg; p.at(0) != NUL; p = p.plus(1))
                {
                    arg.be(len++, p.at(0));
                    for (int l = us_ptr2len_cc(p) - 1; 0 < l; --l)
                    {
                        arg.be(len++, (p = p.plus(1)).at(0));
                        if (p.at(0) == KB_SPECIAL)
                        {
                            arg.be(len++, KS_SPECIAL);
                            arg.be(len++, KE_FILLER);
                        }
                    }
                    arg.be(len, NUL);
                }
            }

            /*
             * Save the current typeahead.  This is required to allow using ":normal"
             * from an event handler and makes sure we don't hang when the argument
             * ends with half a command.
             */
            tasave_C tabuf = new_tasave();
            save_typeahead(tabuf);

            /*
             * Repeat the :normal command for each line in the range.
             * When no range given, execute it just once, without positioning the cursor first.
             */
            do
            {
                if (eap.addr_count != 0)
                {
                    curwin.w_cursor.lnum = eap.line1++;
                    curwin.w_cursor.col = 0;
                }

                exec_normal_cmd((arg != null) ? arg : eap.arg, eap.forceit ? REMAP_NONE : REMAP_YES, false);
            } while (0 < eap.addr_count && eap.line1 <= eap.line2 && !got_int);

            /* Might not return to the main loop when in an event handler. */
            update_topline_cursor();

            /* Restore the previous typeahead. */
            restore_typeahead(tabuf);

            --ex_normal_busy;

            msg_scroll = save_msg_scroll;
            restart_edit = save_restart_edit;
            p_im[0] = save_insertmode;

            finish_op = save_finish_op;
            opcount = save_opcount;
            msg_didout |= save_msg_didout;      /* don't reset msg_didout now */

            /* Restore the state (needed when called from a function executed for
             * 'indentexpr').  Update the mouse and cursor, they may have changed. */
            State = save_State;
            setmouse();
            ui_cursor_shape();          /* may show different cursor shape */
        }
    };

    /*
     * ":startinsert", ":startreplace" and ":startgreplace"
     */
    /*private*/ static final ex_func_C ex_startinsert = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.forceit)
            {
                coladvance(MAXCOL);
                curwin.w_curswant = MAXCOL;
                curwin.w_set_curswant = false;
            }

            /* Ignore the command when already in Insert mode.  Inserting an
             * expression register that invokes a function can do this. */
            if ((State & INSERT) != 0)
                return;

            if (eap.cmdidx == CMD_startinsert)
                restart_edit = 'a';
            else if (eap.cmdidx == CMD_startreplace)
                restart_edit = 'R';
            else
                restart_edit = 'V';

            if (!eap.forceit)
            {
                if (eap.cmdidx == CMD_startinsert)
                    restart_edit = 'i';
                curwin.w_curswant = 0;  /* avoid MAXCOL */
            }
        }
    };

    /*
     * ":stopinsert"
     */
    /*private*/ static final ex_func_C ex_stopinsert = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            restart_edit = 0;
            stop_insert_mode = true;
        }
    };

    /*
     * Execute normal mode command "cmd".
     * "remap" can be REMAP_NONE or REMAP_YES.
     */
    /*private*/ static void exec_normal_cmd(Bytes cmd, int remap, boolean silent)
    {
        oparg_C oa = new oparg_C();

        /*
         * Stuff the argument into the typeahead buffer.
         * Execute normal_cmd() until there is no typeahead left.
         */
        finish_op = false;
        ins_typebuf(cmd, remap, 0, true, silent);
        while ((!stuff_empty() || (!typebuf_typed() && 0 < typebuf.tb_len)) && !got_int)
        {
            update_topline_cursor();
            normal_cmd(oa, true);   /* execute a Normal mode cmd */
        }
    }

    /*private*/ static final int SPEC_PERC   = 0;
    /*private*/ static final int SPEC_HASH   = 1;
    /*private*/ static final int SPEC_CWORD  = 2;
    /*private*/ static final int SPEC_CCWORD = 3;
    /*private*/ static final int SPEC_CFILE  = 4;
    /*private*/ static final int SPEC_SFILE  = 5;
    /*private*/ static final int SPEC_SLNUM  = 6;
    /*private*/ static final int SPEC_AFILE  = 7;
    /*private*/ static final int SPEC_ABUF   = 8;
    /*private*/ static final int SPEC_AMATCH = 9;

    /*private*/ static Bytes[] spec_str =
    {
        u8("%"),
        u8("#"),
        u8("<cword>"),      /* cursor word */
        u8("<cWORD>"),      /* cursor WORD */
        u8("<cfile>"),      /* cursor path name */
        u8("<sfile>"),      /* ":so" file name */
        u8("<slnum>"),      /* ":so" file line number */
        u8("<afile>"),      /* autocommand file name */
        u8("<abuf>"),       /* autocommand buffer number */
        u8("<amatch>"),     /* autocommand match name */
    };

    /*
     * Check "str" for starting with a special cmdline variable.
     * If found, return one of the SPEC_ values and set "*usedlen" to the length of the variable.
     * Otherwise return -1 and "*usedlen" is unchanged.
     */
    /*private*/ static int find_cmdline_var(Bytes src, int[] usedlen)
    {
        for (int i = 0; i < spec_str.length; i++)
        {
            int len = strlen(spec_str[i]);
            if (STRNCMP(src, spec_str[i], len) == 0)
            {
                usedlen[0] = len;
                return i;
            }
        }

        return -1;
    }

    /*
     * Evaluate cmdline variables.
     *
     * change '%'       to curbuf.b_ffname
     *        '#'       to curwin.w_altfile
     *        '<cword>' to word under the cursor
     *        '<cWORD>' to WORD under the cursor
     *        '<cfile>' to path name under the cursor
     *        '<sfile>' to sourced file name
     *        '<slnum>' to sourced file line number
     *        '<afile>' to file name for autocommand
     *        '<abuf>'  to buffer number for autocommand
     *        '<amatch>' to matching name for autocommand
     *
     * When an error is detected, "errormsg" is set to a non-null pointer
     * (may be "" for error without a message) and null is returned.
     * Returns an allocated string if a valid match was found.
     * Returns null if no match was found.
     * "usedlen" then still contains the number of characters to skip.
     */
    /*private*/ static Bytes eval_vars(Bytes src, Bytes srcstart, int[] usedlen, long[] lnump, Bytes[] errormsg, boolean[] escaped)
        /* src: pointer into commandline */
        /* srcstart: beginning of valid memory for src */
        /* usedlen: characters after src that are used */
        /* lnump: line number for :e command, or null */
        /* errormsg: pointer to error message */
        /* escaped: return value has escaped white space (can be null) */
    {
        int valid = VALID_HEAD + VALID_PATH;    /* assume valid result */
        boolean skip_mod = false;
        Bytes strbuf = new Bytes(30);

        errormsg[0] = null;
        if (escaped != null)
            escaped[0] = false;

        /*
         * Check if there is something to do.
         */
        int spec_idx = find_cmdline_var(src, usedlen);
        if (spec_idx < 0)   /* no match */
        {
            usedlen[0] = 1;
            return null;
        }

        /*
         * Skip when preceded with a backslash "\%" and "\#".
         * Note: In "\\%" the % is also not recognized!
         */
        if (BLT(srcstart, src) && src.at(-1) == (byte)'\\')
        {
            usedlen[0] = 0;
            BCOPY(src, -1, src, 0, strlen(src) + 1);         /* remove backslash */
            return null;
        }

        int[] resultlen = new int[1];
        Bytes[] result = new Bytes[1];

        /*
         * word or WORD under cursor
         */
        if (spec_idx == SPEC_CWORD || spec_idx == SPEC_CCWORD)
        {
            resultlen[0] = find_ident_under_cursor(result,
                                        (spec_idx == SPEC_CWORD) ? (FIND_IDENT|FIND_STRING) : FIND_STRING);
            if (resultlen[0] == 0)
            {
                errormsg[0] = u8("");
                return null;
            }
        }
        /*
         * '#': Alternate file name
         * '%': Current file name
         *      File name under the cursor
         *      File name for autocommand
         *  and following modifiers
         */
        else
        {
            Bytes[] resultbuf = { null };

            switch (spec_idx)
            {
                case SPEC_PERC:                 /* '%': current file */
                {
                    if (curbuf.b_fname == null)
                    {
                        result[0] = u8("");
                        valid = 0;              /* Must have ":p:h" to be valid */
                    }
                    else
                        result[0] = curbuf.b_fname;
                    break;
                }

                case SPEC_HASH:                 /* '#' or "#99": alternate file */
                {
                    if (src.at(1) == (byte)'#')          /* "##": the argument list */
                    {
                        result[0] = arg_all();
                        resultbuf[0] = result[0];
                        usedlen[0] = 2;
                        if (escaped != null)
                            escaped[0] = true;
                        skip_mod = true;
                        break;
                    }
                    Bytes s = src.plus(1);
                    if (s.at(0) == (byte)'<')              /* "#<99" uses v:oldfiles */
                        s = s.plus(1);
                    int i;
                    { Bytes[] __ = { s }; i = (int)getdigits(__); s = __[0]; }
                    usedlen[0] = BDIFF(s, src);  /* length of what we expand */

                    if (src.at(1) == (byte)'<')
                    {
                        if (usedlen[0] < 2)
                        {
                            /* Should we give an error message for #<text? */
                            usedlen[0] = 1;
                            return null;
                        }
                        result[0] = list_find_str(get_vim_var_list(VV_OLDFILES), (long)i);
                        if (result[0] == null)
                        {
                            errormsg[0] = u8("");
                            return null;
                        }
                    }
                    else
                    {
                        buffer_C buf = buflist_findnr(i);
                        if (buf == null)
                        {
                            errormsg[0] = u8("E194: No alternate file name to substitute for '#'");
                            return null;
                        }
                        if (lnump != null)
                            lnump[0] = ECMD_LAST;
                        if (buf.b_fname == null)
                        {
                            result[0] = u8("");
                            valid = 0;          /* Must have ":p:h" to be valid */
                        }
                        else
                            result[0] = buf.b_fname;
                    }
                    break;
                }

                case SPEC_AFILE:                /* file name for autocommand */
                {
                    result[0] = autocmd_fname;
                    if (result[0] != null && !autocmd_fname_full)
                    {
                        /* Still need to turn the fname into a full path.
                         * It is postponed to avoid a delay when <afile> is not used. */
                        autocmd_fname_full = true;
                        result[0] = fullName_save(autocmd_fname, false);
                        autocmd_fname = result[0];
                    }
                    if (result[0] == null)
                    {
                        errormsg[0] = u8("E495: no autocommand file name to substitute for \"<afile>\"");
                        return null;
                    }
                    result[0] = shorten_fname1(result[0]);
                    break;
                }

                case SPEC_ABUF:                 /* buffer number for autocommand */
                {
                    if (autocmd_bufnr <= 0)
                    {
                        errormsg[0] = u8("E496: no autocommand buffer number to substitute for \"<abuf>\"");
                        return null;
                    }
                    libC.sprintf(strbuf, u8("%d"), autocmd_bufnr);
                    result[0] = strbuf;
                    break;
                }

                case SPEC_AMATCH:               /* match name for autocommand */
                {
                    result[0] = autocmd_match;
                    if (result[0] == null)
                    {
                        errormsg[0] = u8("E497: no autocommand match name to substitute for \"<amatch>\"");
                        return null;
                    }
                    break;
                }

                case SPEC_SFILE:                /* file name for ":so" command */
                {
                    result[0] = sourcing_name;
                    if (result[0] == null)
                    {
                        errormsg[0] = u8("E498: no :source file name to substitute for \"<sfile>\"");
                        return null;
                    }
                    break;
                }

                case SPEC_SLNUM:                /* line in file for ":so" command */
                {
                    if (sourcing_name == null || sourcing_lnum == 0)
                    {
                        errormsg[0] = u8("E842: no line number to use for \"<slnum>\"");
                        return null;
                    }
                    libC.sprintf(strbuf, u8("%ld"), sourcing_lnum);
                    result[0] = strbuf;
                    break;
                }
            }

            resultlen[0] = strlen(result[0]);    /* length of new string */

            if (src.at(usedlen[0]) == (byte)'<')           /* remove the file name extension */
            {
                ++usedlen[0];
                Bytes s = vim_strrchr(result[0], (byte)'.');
                if (s != null && BLE(gettail(result[0]), s))
                    resultlen[0] = BDIFF(s, result[0]);
            }
            else if (!skip_mod)
            {
                valid |= modify_fname(src, usedlen, result, resultbuf, resultlen);
                if (result[0] == null)
                {
                    errormsg[0] = u8("");
                    return null;
                }
            }
        }

        if (resultlen[0] == 0 || valid != VALID_HEAD + VALID_PATH)
        {
            if (valid != VALID_HEAD + VALID_PATH)
                /* xgettext:no-c-format */
                errormsg[0] = u8("E499: Empty file name for '%' or '#', only works with \":p:h\"");
            else
                errormsg[0] = u8("E500: Evaluates to an empty string");
            result[0] = null;
        }
        else
            result[0] = STRNDUP(result[0], resultlen[0]);

        return result[0];
    }

    /*
     * Concatenate all files in the argument list, separated by spaces, and return
     * it in one allocated string.
     * Spaces and backslashes in the file names are escaped with a backslash.
     * Returns null when out of memory.
     */
    /*private*/ static Bytes arg_all()
    {
        Bytes retval = null;

        /*
         * Do this loop two times:
         * first time: compute the total length
         * second time: concatenate the names
         */
        for ( ; ; )
        {
            aentry_C[] waep = curwin.w_alist.al_ga.ga_data;

            int len = 0;
            for (int idx = 0; idx < curwin.w_alist.al_ga.ga_len; idx++)
            {
                Bytes p = alist_name(waep[idx]);
                if (p != null)
                {
                    if (0 < len)
                    {
                        /* insert a space in between names */
                        if (retval != null)
                            retval.be(len, (byte)' ');
                        len++;
                    }
                    for ( ; p.at(0) != NUL; p = p.plus(1))
                    {
                        if (p.at(0) == (byte)' ' || p.at(0) == (byte)'\\')
                        {
                            /* insert a backslash */
                            if (retval != null)
                                retval.be(len, (byte)'\\');
                            len++;
                        }
                        if (retval != null)
                            retval.be(len, p.at(0));
                        len++;
                    }
                }
            }

            /* second time: break here */
            if (retval != null)
            {
                retval.be(len, NUL);
                break;
            }

            /* allocate memory */
            retval = new Bytes(len + 1);
        }

        return retval;
    }

    /*
     * Expand the <sfile> string in "arg".
     *
     * Returns an allocated string, or null for any error.
     */
    /*private*/ static Bytes expand_sfile(Bytes arg)
    {
        Bytes result = STRDUP(arg);

        for (Bytes p = result; p.at(0) != NUL; )
        {
            if (STRNCMP(p, u8("<sfile>"), 7) != 0)
                p = p.plus(1);
            else
            {
                /* replace "<sfile>" with the sourced file name, and do ":" stuff */
                int[] srclen = new int[1];
                Bytes[] errormsg = new Bytes[1];
                Bytes repl = eval_vars(p, result, srclen, null, errormsg, null);
                if (errormsg[0] != null)
                {
                    if (errormsg[0].at(0) != NUL)
                        emsg(errormsg[0]);
                    return null;
                }
                if (repl == null)           /* no match (cannot happen) */
                {
                    p = p.plus(srclen[0]);
                    continue;
                }
                int len = strlen(result) - srclen[0] + strlen(repl) + 1;
                Bytes newres = new Bytes(len);
                BCOPY(newres, result, BDIFF(p, result));
                STRCPY(newres.plus(BDIFF(p, result)), repl);
                len = strlen(newres);
                STRCAT(newres, p.plus(srclen[0]));
                result = newres;
                p = newres.plus(len);           /* continue after the match */
            }
        }

        return result;
    }

    /*
     * Make a dialog message in "buff[DIALOG_MSG_SIZE]".
     * "format" must contain "%s".
     */
    /*private*/ static void dialog_msg(Bytes buff, Bytes format, Bytes fname)
    {
        if (fname == null)
            fname = u8("Untitled");
        vim_snprintf(buff, DIALOG_MSG_SIZE, format, fname);
    }

    /*
     * ":setfiletype {name}"
     */
    /*private*/ static final ex_func_C ex_setfiletype = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (!did_filetype)
                set_option_value(u8("filetype"), 0L, eap.arg, OPT_LOCAL);
        }
    };

    /*private*/ static final ex_func_C ex_digraphs = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.arg.at(0) != NUL)
                putdigraph(eap.arg);
            else
                listdigraphs();
        }
    };

    /*private*/ static final ex_func_C ex_set = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int flags = 0;

            if (eap.cmdidx == CMD_setlocal)
                flags = OPT_LOCAL;
            else if (eap.cmdidx == CMD_setglobal)
                flags = OPT_GLOBAL;
            do_set(eap.arg, flags);
        }
    };

    /*
     * ":nohlsearch"
     */
    /*private*/ static final ex_func_C ex_nohlsearch = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            no_hlsearch = true;
            set_vim_var_nr(VV_HLSEARCH, (!no_hlsearch && p_hls[0]) ? 1 : 0);
            redraw_all_later(SOME_VALID);
        }
    };

    /*
     * ":[N]match {group} {pattern}"
     * Sets nextcmd to the start of the next command, if any.  Also called when
     * skipping commands to find the next command.
     */
    /*private*/ static final ex_func_C ex_match = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int id;
            if (eap.line2 <= 3)
                id = (int)eap.line2;
            else
            {
                emsg(e_invcmd);
                return;
            }

            /* First clear any old pattern. */
            if (!eap.skip)
                match_delete(curwin, id, false);

            Bytes end;

            if (ends_excmd(eap.arg.at(0)))
                end = eap.arg;
            else if ((STRNCASECMP(eap.arg, u8("none"), 4) == 0 && (vim_iswhite(eap.arg.at(4)) || ends_excmd(eap.arg.at(4)))))
                end = eap.arg.plus(4);
            else
            {
                Bytes g = null;
                Bytes p = skiptowhite(eap.arg);
                if (!eap.skip)
                    g = STRNDUP(eap.arg, BDIFF(p, eap.arg));
                p = skipwhite(p);
                if (p.at(0) == NUL)
                {
                    /* There must be two arguments. */
                    emsg2(e_invarg2, eap.arg);
                    return;
                }
                end = skip_regexp(p.plus(1), p.at(0), true, null);
                if (!eap.skip)
                {
                    if (end.at(0) != NUL && !ends_excmd(skipwhite(end.plus(1)).at(0)))
                    {
                        eap.errmsg = e_trailing;
                        return;
                    }
                    if (end.at(0) != p.at(0))
                    {
                        emsg2(e_invarg2, p);
                        return;
                    }

                    int c = end.at(0);
                    end.be(0, NUL);
                    match_add(curwin, g, p.plus(1), 10, id, null);
                    end.be(0, c);
                }
            }
            eap.nextcmd = find_nextcmd(end);
        }
    };

    /*
     * ex_eval.c: functions for Ex command line for the +eval feature ---------------------------------
     */

    /*
     * Exception handling terms:
     *
     *      :try            ":try" command          \
     *          ...         try block               |
     *      :catch RE       ":catch" command        |
     *          ...         catch clause            |- try conditional
     *      :finally        ":finally" command      |
     *          ...         finally clause          |
     *      :endtry         ":endtry" command       /
     *
     * The try conditional may have any number of catch clauses and at most one
     * finally clause.  A ":throw" command can be inside the try block, a catch
     * clause, the finally clause, or in a function called or script sourced from
     * there or even outside the try conditional.  Try conditionals may be nested.
     */

    /*
     * Configuration whether an exception is thrown on error or interrupt.  When
     * the preprocessor macros below evaluate to false, an error (did_emsg) or
     * interrupt (got_int) under an active try conditional terminates the script
     * after the non-active finally clauses of all active try conditionals have been
     * executed.  Otherwise, errors and/or interrupts are converted into catchable
     * exceptions (did_throw additionally set), which terminate the script only if
     * not caught.  For user exceptions, only did_throw is set.  (Note: got_int can
     * be set asynchronously afterwards by a SIGINT, so did_throw && got_int is not
     * a reliant test that the exception currently being thrown is an interrupt
     * exception.  Similarly, did_emsg can be set afterwards on an error in an
     * (unskipped) conditional command inside an inactive conditional, so did_throw
     * && did_emsg is not a reliant test that the exception currently being thrown
     * is an error exception.)  -  The macros can be defined as expressions checking
     * for a variable that is allowed to be changed during execution of a script.
     */

    /*
     * When several errors appear in a row, setting "force_abort" is delayed until
     * the failing command returned.  "cause_abort" is set to true meanwhile, in
     * order to indicate that situation.  This is useful when "force_abort" was set
     * during execution of a function call from an expression: the aborting of the
     * expression evaluation is done without producing any error messages, but all
     * error messages on parsing errors during the expression evaluation are given
     * (even if a try conditional is active).
     */
    /*private*/ static boolean cause_abort;

    /*
     * Return true when immediately aborting on error, or when an interrupt
     * occurred or an exception was thrown but not caught.  Use for ":{range}call"
     * to check whether an aborted function that does not handle a range itself
     * should be called again for the next line in the range.  Also used for
     * cancelling expression evaluation after a function call caused an immediate
     * abort.  Note that the first emsg() call temporarily resets "force_abort"
     * until the throw point for error messages has been reached.  That is, during
     * cancellation of an expression evaluation after an aborting function call or
     * due to a parsing error, aborting() always returns the same value.
     */
    /*private*/ static boolean aborting()
    {
        return (did_emsg && force_abort) || got_int || did_throw;
    }

    /*
     * The value of "force_abort" is temporarily reset by the first emsg() call
     * during an expression evaluation, and "cause_abort" is used instead.  It might
     * be necessary to restore "force_abort" even before the throw point for the
     * error message has been reached.  update_force_abort() should be called then.
     */
    /*private*/ static void update_force_abort()
    {
        if (cause_abort)
            force_abort = true;
    }

    /*
     * Return true if a command with a subcommand resulting in "retcode" should
     * abort the script processing.  Can be used to suppress an autocommand after
     * execution of a failing subcommand as long as the error message has not been
     * displayed and actually caused the abortion.
     */
    /*private*/ static boolean should_abort(boolean retcode)
    {
        return ((retcode == false && trylevel != 0 && emsg_silent == 0) || aborting());
    }

    /*
     * Return true if a function with the "abort" flag should not be considered
     * ended on an error.  This means that parsing commands is continued in order
     * to find finally clauses to be executed, and that some errors in skipped
     * commands are still reported.
     */
    /*private*/ static boolean aborted_in_try()
    {
        /* This function is only called after an error.  In this case, "force_abort"
         * determines whether searching for finally clauses is necessary. */
        return force_abort;
    }

    /*
     * cause_errthrow(): Cause a throw of an error exception if appropriate.
     * Return true if the error message should not be displayed by emsg().
     * Sets "ignore", if the emsg() call should be ignored completely.
     *
     * When several messages appear in the same command, the first is usually the
     * most specific one and used as the exception value.  The "severe" flag can be
     * set to true, if a later but severer message should be used instead.
     */
    /*private*/ static boolean cause_errthrow(Bytes mesg, boolean severe, boolean[] ignore)
    {
        /*
         * Do nothing when displaying the interrupt message or reporting an
         * uncaught exception (which has already been discarded then) at the top level.
         * Also when no exception can be thrown.  The message will be displayed by emsg().
         */
        if (suppress_errthrow)
            return false;

        /*
         * If emsg() has not been called previously, temporarily reset "force_abort" until
         * the throw point for error messages has been reached.  This ensures that aborting()
         * returns the same value for all errors that appear in the same command.
         * This means particularly that for parsing errors during expression evaluation
         * emsg() will be called multiply, even when the expression is evaluated from a finally
         * clause that was activated due to an aborting error, interrupt, or exception.
         */
        if (!did_emsg)
        {
            cause_abort = force_abort;
            force_abort = false;
        }

        /*
         * If no try conditional is active and no exception is being thrown and
         * there has not been an error in a try conditional or a throw so far, do
         * nothing (for compatibility of non-EH scripts).  The message will then
         * be displayed by emsg().  When ":silent!" was used and we are not
         * currently throwing an exception, do nothing.  The message text will
         * then be stored to v:errmsg by emsg() without displaying it.
         */
        if (((trylevel == 0 && !cause_abort) || emsg_silent != 0) && !did_throw)
            return false;

        /*
         * Ignore an interrupt message when inside a try conditional or when an
         * exception is being thrown or when an error in a try conditional or
         * throw has been detected previously.  This is important in order that an
         * interrupt exception is catchable by the innermost try conditional and
         * not replaced by an interrupt message error exception.
         */
        if (BEQ(mesg, e_interr))
        {
            ignore[0] = true;
            return true;
        }

        /*
         * Ensure that all commands in nested function calls and sourced files
         * are aborted immediately.
         */
        cause_abort = true;

        /*
         * When an exception is being thrown, some commands (like conditionals) are
         * not skipped.  Errors in those commands may affect what of the subsequent
         * commands are regarded part of catch and finally clauses.  Catching the
         * exception would then cause execution of commands not intended by the
         * user, who wouldn't even get aware of the problem.  Therefor, discard the
         * exception currently being thrown to prevent it from being caught.  Just
         * execute finally clauses and terminate.
         */
        if (did_throw)
        {
            /* When discarding an interrupt exception, reset got_int to prevent the
             * same interrupt being converted to an exception again and discarding
             * the error exception we are about to throw here. */
            if (current_exception.type == ET_INTERRUPT)
                got_int = false;
            discard_current_exception();
        }

        /*
         * Prepare the throw of an error exception, so that everything will be aborted
         * (except for executing finally clauses), until the error exception is caught;
         * if still uncaught at the top level, the error message will be displayed and
         * the script processing terminated then.  This function has no access to the
         * conditional stack.  Thus, the actual throw is made after the failing command
         * has returned.  Throw only the first of several errors in a row, except
         * a severe error is following.
         */
        if (msg_list != null)
        {
            msglist_C last = null;
            for (msglist_C list = msg_list[0]; list != null; list = list.next)
                last = list;

            msglist_C elem = new msglist_C();

            elem.msg = STRDUP(mesg);
            elem.next = null;
            elem.throw_msg = null;
            if (last == null)
                msg_list[0] = elem;
            else
                last.next = elem;
            if (last == null || severe)
            {
                /* Skip the extra "Vim " prefix for message "E458". */
                Bytes tmsg = elem.msg;
                if (STRNCMP(tmsg, u8("Vim E"), 5) == 0
                        && asc_isdigit(tmsg.at(5))
                        && asc_isdigit(tmsg.at(6))
                        && asc_isdigit(tmsg.at(7))
                        && tmsg.at(8) == (byte)':'
                        && tmsg.at(9) == (byte)' ')
                    msg_list[0].throw_msg = tmsg.plus(4);
                else
                    msg_list[0].throw_msg = tmsg;
            }
        }

        return true;
    }

    /*
     * Free global "*msg_list" and the messages it contains, then set "*msg_list" to null.
     */
    /*private*/ static void free_global_msglist()
    {
        msg_list[0] = null;
    }

    /*
     * Throw the message specified in the call to cause_errthrow() above as an
     * error exception.  If cstack is null, postpone the throw until do_cmdline()
     * has returned (see do_one_cmd()).
     */
    /*private*/ static void do_errthrow(condstack_C cstack, Bytes cmdname)
    {
        /*
         * Ensure that all commands in nested function calls and sourced files
         * are aborted immediately.
         */
        if (cause_abort)
        {
            cause_abort = false;
            force_abort = true;
        }

        /* If no exception is to be thrown or the conversion should be done after
         * returning to a previous invocation of do_one_cmd(), do nothing. */
        if (msg_list == null || msg_list[0] == null)
            return;

        if (throw_exception(msg_list[0], ET_ERROR, cmdname) != false)
        {
            if (cstack != null)
                do_throw(cstack);
            else
                need_rethrow = true;
        }
        msg_list[0] = null;
    }

    /*
     * do_intthrow(): Replace the current exception by an interrupt or interrupt
     * exception if appropriate.  Return true if the current exception is discarded,
     * false otherwise.
     */
    /*private*/ static boolean do_intthrow(condstack_C cstack)
    {
        /*
         * If no interrupt occurred or no try conditional is active and no exception
         * is being thrown, do nothing (for compatibility of non-EH scripts).
         */
        if (!got_int || (trylevel == 0 && !did_throw))
            return false;

        /*
         * Throw an interrupt exception, so that everything will be aborted
         * (except for executing finally clauses), until the interrupt exception
         * is caught; if still uncaught at the top level, the script processing
         * will be terminated then.  -  If an interrupt exception is already
         * being thrown, do nothing.
         *
         */
        if (did_throw)
        {
            if (current_exception.type == ET_INTERRUPT)
                return false;

            /* An interrupt exception replaces any user or error exception. */
            discard_current_exception();
        }
        if (throw_exception(u8("Vim:Interrupt"), ET_INTERRUPT, null) != false)
            do_throw(cstack);

        return true;
    }

    /*
     * Get an exception message that is to be stored in current_exception.value.
     */
    /*private*/ static Bytes get_exception_string(/*msglist_C|Bytes*/Object value, int type, Bytes cmdname)
    {
        Bytes ret;

        if (type == ET_ERROR)
        {
            Bytes mesg = ((msglist_C)value).throw_msg;

            Bytes val;
            if (cmdname != null && cmdname.at(0) != NUL)
            {
                int cmdlen = strlen(cmdname);
                ret = STRNDUP(u8("Vim("), 4 + cmdlen + 2 + strlen(mesg));
                STRCPY(ret.plus(4), cmdname);
                STRCPY(ret.plus(4 + cmdlen), u8("):"));
                val = ret.plus(4 + cmdlen + 2);
            }
            else
            {
                ret = STRNDUP(u8("Vim:"), 4 + strlen(mesg));
                val = ret.plus(4);
            }

            /* msg_add_fname() may have been used to prefix the message with a file name in quotes.
             * In the exception value, put the file name in parentheses and move it to the end. */
            for (Bytes p = mesg; ; p = p.plus(1))
            {
                if (p.at(0) == NUL
                        || (p.at(0) == (byte)'E'
                            && asc_isdigit(p.at(1))
                            && (p.at(2) == (byte)':'
                                || (asc_isdigit(p.at(2))
                                    && (p.at(3) == (byte)':'
                                        || (asc_isdigit(p.at(3))
                                            && p.at(4) == (byte)':'))))))
                {
                    if (p.at(0) == NUL || BEQ(p, mesg))
                        STRCAT(val, mesg);          /* 'E123' missing or at beginning */
                    else
                    {
                        /* '"filename" E123: message text' */
                        if (mesg.at(0) != (byte)'"' || BLT(p.minus(2), mesg.plus(1)) || p.at(-2) != (byte)'"' || p.at(-1) != (byte)' ')
                            /* "E123:" is part of the file name. */
                            continue;

                        STRCAT(val, p);
                        p.be(-2, NUL);
                        libC.sprintf(val.plus(strlen(p)), u8(" (%s)"), mesg.plus(1));
                        p.be(-2, (byte)'"');
                    }
                    break;
                }
            }
        }
        else
        {
            ret = (Bytes)value;
        }

        return ret;
    }

    /*
     * Throw a new exception.  Return false when out of memory or it was tried to throw
     * an illegal user exception.  "value" is the exception string for a user or
     * interrupt exception, or points to a message list in case of an error exception.
     */
    /*private*/ static boolean throw_exception(/*msglist_C|Bytes*/Object value, int type, Bytes cmdname)
    {
        /*
         * Disallow faking Interrupt or error exceptions as user exceptions.
         * They would be treated differently from real interrupt or error exceptions
         * when no active try block is found, see do_cmdline().
         */
        if (type == ET_USER)
        {
            Bytes v = (Bytes)value;
            if (STRNCMP(v, u8("Vim"), 3) == 0 && (v.at(3) == NUL || v.at(3) == (byte)':' || v.at(3) == (byte)'('))
            {
                emsg(u8("E608: Cannot :throw exceptions with 'Vim' prefix"));
                current_exception = null;
                return false;
            }
        }

        except_C excp = new except_C();

        if (type == ET_ERROR)
            /* Store the original message and prefix the exception value with
             * "Vim:" or, if a command name is given, "Vim(cmdname):". */
            excp.messages = (msglist_C)value;

        excp.value = get_exception_string(value, type, cmdname);
        excp.type = type;
        excp.throw_name = STRDUP(sourcing_name == null ? u8("") : sourcing_name);
        excp.throw_lnum = sourcing_lnum;

        if (13 <= p_verbose[0] || 0 < debug_break_level)
        {
            int save_msg_silent = msg_silent;

            if (0 < debug_break_level)
                msg_silent = FALSE;         /* display messages */
            else
                verbose_enter();
            no_wait_return++;
            if (0 < debug_break_level || p_vfile[0].at(0) == NUL)
                msg_scroll = true;          /* always scroll up, don't overwrite */

            smsg(u8("Exception thrown: %s"), excp.value);
            msg_puts(u8("\n"));                 /* don't overwrite this either */

            if (0 < debug_break_level || p_vfile[0].at(0) == NUL)
                cmdline_row = msg_row;
            --no_wait_return;
            if (0 < debug_break_level)
                msg_silent = save_msg_silent;
            else
                verbose_leave();
        }

        current_exception = excp;
        return true;
    }

    /*
     * Discard an exception.  "was_finished" is set when the exception
     * has been caught and the catch clause has been ended normally.
     */
    /*private*/ static void discard_exception(except_C excp, boolean was_finished)
    {
        if (excp == null)
        {
            emsg(e_internal);
            return;
        }

        if (13 <= p_verbose[0] || 0 < debug_break_level)
        {
            int save_msg_silent = msg_silent;

            Bytes saved_ioBuff = STRDUP(ioBuff);
            if (0 < debug_break_level)
                msg_silent = FALSE;         /* display messages */
            else
                verbose_enter();
            no_wait_return++;
            if (0 < debug_break_level || p_vfile[0].at(0) == NUL)
                msg_scroll = true;          /* always scroll up, don't overwrite */
            smsg(was_finished ? u8("Exception finished: %s") : u8("Exception discarded: %s"), excp.value);
            msg_puts(u8("\n"));                 /* don't overwrite this either */
            if (0 < debug_break_level || p_vfile[0].at(0) == NUL)
                cmdline_row = msg_row;
            --no_wait_return;
            if (0 < debug_break_level)
                msg_silent = save_msg_silent;
            else
                verbose_leave();
            STRCPY(ioBuff, saved_ioBuff);
        }
    }

    /*
     * Discard the exception currently being thrown.
     */
    /*private*/ static void discard_current_exception()
    {
        discard_exception(current_exception, false);
        current_exception = null;
        did_throw = false;
        need_rethrow = false;
    }

    /*
     * Put an exception on the caught stack.
     */
    /*private*/ static void catch_exception(except_C excp)
    {
        excp.caught = caught_stack;
        caught_stack = excp;
        set_vim_var_string(VV_EXCEPTION, excp.value, -1);
        if (excp.throw_name.at(0) != NUL)
        {
            if (excp.throw_lnum != 0)
                vim_snprintf(ioBuff, IOSIZE, u8("%s, line %ld"), excp.throw_name, excp.throw_lnum);
            else
                vim_snprintf(ioBuff, IOSIZE, u8("%s"), excp.throw_name);
            set_vim_var_string(VV_THROWPOINT, ioBuff, -1);
        }
        else
            /* throw_name not set on an exception from a command that was typed. */
            set_vim_var_string(VV_THROWPOINT, null, -1);

        if (13 <= p_verbose[0] || 0 < debug_break_level)
        {
            int save_msg_silent = msg_silent;

            if (0 < debug_break_level)
                msg_silent = FALSE;         /* display messages */
            else
                verbose_enter();
            no_wait_return++;
            if (0 < debug_break_level || p_vfile[0].at(0) == NUL)
                msg_scroll = true;          /* always scroll up, don't overwrite */

            smsg(u8("Exception caught: %s"), excp.value);
            msg_puts(u8("\n"));                 /* don't overwrite this either */

            if (0 < debug_break_level || p_vfile[0].at(0) == NUL)
                cmdline_row = msg_row;
            --no_wait_return;
            if (0 < debug_break_level)
                msg_silent = save_msg_silent;
            else
                verbose_leave();
        }
    }

    /*
     * Remove an exception from the caught stack.
     */
    /*private*/ static void finish_exception(except_C excp)
    {
        if (excp != caught_stack)
            emsg(e_internal);
        caught_stack = caught_stack.caught;
        if (caught_stack != null)
        {
            set_vim_var_string(VV_EXCEPTION, caught_stack.value, -1);
            if (caught_stack.throw_name.at(0) != NUL)
            {
                if (caught_stack.throw_lnum != 0)
                    vim_snprintf(ioBuff, IOSIZE, u8("%s, line %ld"), caught_stack.throw_name,
                            caught_stack.throw_lnum);
                else
                    vim_snprintf(ioBuff, IOSIZE, u8("%s"), caught_stack.throw_name);
                set_vim_var_string(VV_THROWPOINT, ioBuff, -1);
            }
            else
                /* throw_name not set on an exception from a command that was typed. */
                set_vim_var_string(VV_THROWPOINT, null, -1);
        }
        else
        {
            set_vim_var_string(VV_EXCEPTION, null, -1);
            set_vim_var_string(VV_THROWPOINT, null, -1);
        }

        /* Discard the exception, but use the finish message for 'verbose'. */
        discard_exception(excp, true);
    }

    /*
     * Flags specifying the message displayed by report_pending.
     */
    /*private*/ static final int RP_MAKE         = 0;
    /*private*/ static final int RP_RESUME       = 1;
    /*private*/ static final int RP_DISCARD      = 2;

    /*
     * Report information about something pending in a finally clause if required by
     * the 'verbose' option or when debugging.  "action" tells whether something is
     * made pending or something pending is resumed or discarded.  "pending" tells
     * what is pending.  "value" specifies the return value for a pending ":return"
     * or the exception value for a pending exception.
     */
    /*private*/ static void report_pending(int action, int pending, Object value)
    {
        Bytes mesg;

        switch (action)
        {
            case RP_MAKE:
                mesg = u8("%s made pending");
                break;
            case RP_RESUME:
                mesg = u8("%s resumed");
                break;
         /* case RP_DISCARD: */
            default:
                mesg = u8("%s discarded");
                break;
        }

        Bytes s;

        switch (pending)
        {
            case CSTP_NONE:
                return;

            case CSTP_CONTINUE:
                s = u8(":continue");
                break;
            case CSTP_BREAK:
                s = u8(":break");
                break;
            case CSTP_FINISH:
                s = u8(":finish");
                break;
            case CSTP_RETURN:
                /* ":return" command producing value, allocated */
                s = get_return_cmd((typval_C)value);
                break;

            default:
                if ((pending & CSTP_THROW) != 0)
                {
                    vim_snprintf(ioBuff, IOSIZE, mesg, u8("Exception"));
                    mesg = STRNDUP(ioBuff, strlen(ioBuff) + 4);
                    STRCAT(mesg, u8(": %s"));
                    s = ((except_C)value).value;
                }
                else if ((pending & CSTP_ERROR) != 0 && (pending & CSTP_INTERRUPT) != 0)
                    s = u8("Error and interrupt");
                else if ((pending & CSTP_ERROR) != 0)
                    s = u8("Error");
                else /* if ((pending & CSTP_INTERRUPT) != 0) */
                    s = u8("Interrupt");
        }

        int save_msg_silent = msg_silent;
        if (0 < debug_break_level)
            msg_silent = FALSE;         /* display messages */
        no_wait_return++;
        msg_scroll = true;              /* always scroll up, don't overwrite */
        smsg(mesg, s);
        msg_puts(u8("\n"));                 /* don't overwrite this either */
        cmdline_row = msg_row;
        --no_wait_return;
        if (0 < debug_break_level)
            msg_silent = save_msg_silent;
    }

    /*
     * If something is made pending in a finally clause, report it if required by
     * the 'verbose' option or when debugging.
     */
    /*private*/ static void report_make_pending(int pending, Object value)
    {
        if (14 <= p_verbose[0] || 0 < debug_break_level)
        {
            if (debug_break_level <= 0)
                verbose_enter();
            report_pending(RP_MAKE, pending, value);
            if (debug_break_level <= 0)
                verbose_leave();
        }
    }

    /*
     * If something pending in a finally clause is resumed at the ":endtry", report
     * it if required by the 'verbose' option or when debugging.
     */
    /*private*/ static void report_resume_pending(int pending, Object value)
    {
        if (14 <= p_verbose[0] || 0 < debug_break_level)
        {
            if (debug_break_level <= 0)
                verbose_enter();
            report_pending(RP_RESUME, pending, value);
            if (debug_break_level <= 0)
                verbose_leave();
        }
    }

    /*
     * If something pending in a finally clause is discarded, report it if required
     * by the 'verbose' option or when debugging.
     */
    /*private*/ static void report_discard_pending(int pending, Object value)
    {
        if (14 <= p_verbose[0] || 0 < debug_break_level)
        {
            if (debug_break_level <= 0)
                verbose_enter();
            report_pending(RP_DISCARD, pending, value);
            if (debug_break_level <= 0)
                verbose_leave();
        }
    }

    /*
     * ":if".
     */
    /*private*/ static final ex_func_C ex_if = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            condstack_C cstack = eap.cstack;

            if (cstack.cs_idx == CSTACK_LEN - 1)
                eap.errmsg = u8("E579: :if nesting too deep");
            else
            {
                cstack.cs_idx++;
                cstack.cs_flags[cstack.cs_idx] = 0;

                /*
                 * Don't do something after an error, interrupt, or throw, or when
                 * there is a surrounding conditional and it was not active.
                 */
                boolean skip = (did_emsg || got_int || did_throw
                                || (0 < cstack.cs_idx && (cstack.cs_flags[cstack.cs_idx - 1] & CSF_ACTIVE) == 0));

                boolean[] error = new boolean[1];
                boolean result;
                { Bytes[] __ = { eap.nextcmd }; result = eval_to_bool(eap.arg, error, __, skip); eap.nextcmd = __[0]; }

                if (!skip && !error[0])
                {
                    if (result)
                        cstack.cs_flags[cstack.cs_idx] = CSF_ACTIVE | CSF_TRUE;
                }
                else
                    /* set true, so this conditional will never get active */
                    cstack.cs_flags[cstack.cs_idx] = CSF_TRUE;
            }
        }
    };

    /*
     * ":endif".
     */
    /*private*/ static final ex_func_C ex_endif = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            did_endif = true;
            if (eap.cstack.cs_idx < 0
                    || (eap.cstack.cs_flags[eap.cstack.cs_idx] & (CSF_WHILE | CSF_FOR | CSF_TRY)) != 0)
                eap.errmsg = u8("E580: :endif without :if");
            else
            {
                /*
                 * When debugging or a breakpoint was encountered, display the debug
                 * prompt (if not already done).  This shows the user that an ":endif"
                 * is executed when the ":if" or a previous ":elseif" was not true.
                 * Handle a ">quit" debug command as if an interrupt had occurred before
                 * the ":endif".  That is, throw an interrupt exception if appropriate.
                 * Doing this here prevents an exception for a parsing error being
                 * discarded by throwing the interrupt exception later on.
                 */
                if ((eap.cstack.cs_flags[eap.cstack.cs_idx] & CSF_TRUE) == 0 && dbg_check_skipped(eap))
                    do_intthrow(eap.cstack);

                --eap.cstack.cs_idx;
            }
        }
    };

    /*
     * ":else" and ":elseif".
     */
    /*private*/ static final ex_func_C ex_else = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            condstack_C cstack = eap.cstack;

            /*
             * Don't do something after an error, interrupt, or throw, or when
             * there is a surrounding conditional and it was not active.
             */
            boolean skip = (did_emsg || got_int || did_throw
                                || (0 < cstack.cs_idx && (cstack.cs_flags[cstack.cs_idx - 1] & CSF_ACTIVE) == 0));

            if (cstack.cs_idx < 0 || (cstack.cs_flags[cstack.cs_idx] & (CSF_WHILE | CSF_FOR | CSF_TRY)) != 0)
            {
                if (eap.cmdidx == CMD_else)
                {
                    eap.errmsg = u8("E581: :else without :if");
                    return;
                }
                eap.errmsg = u8("E582: :elseif without :if");
                skip = true;
            }
            else if ((cstack.cs_flags[cstack.cs_idx] & CSF_ELSE) != 0)
            {
                if (eap.cmdidx == CMD_else)
                {
                    eap.errmsg = u8("E583: multiple :else");
                    return;
                }
                eap.errmsg = u8("E584: :elseif after :else");
                skip = true;
            }

            /* if skipping or the ":if" was true, reset ACTIVE, otherwise set it */
            if (skip || (cstack.cs_flags[cstack.cs_idx] & CSF_TRUE) != 0)
            {
                if (eap.errmsg == null)
                    cstack.cs_flags[cstack.cs_idx] = CSF_TRUE;
                skip = true;    /* don't evaluate an ":elseif" */
            }
            else
                cstack.cs_flags[cstack.cs_idx] = CSF_ACTIVE;

            /*
             * When debugging, or a breakpoint was encountered, display the debug prompt
             * (if not already done).  This shows the user that an ":else" or ":elseif"
             * is executed when the ":if" or previous ":elseif" was not true.
             * Handle a ">quit" debug command as if an interrupt had occurred
             * before the ":else" or ":elseif".  That is, set "skip" and throw an interrupt
             * exception if appropriate.  Doing this here prevents that an exception for
             * a parsing error is discarded when throwing the interrupt exception later on.
             */
            if (!skip && dbg_check_skipped(eap) && got_int)
            {
                do_intthrow(cstack);
                skip = true;
            }

            if (eap.cmdidx == CMD_elseif)
            {
                boolean[] error = new boolean[1];
                boolean result;
                { Bytes[] __ = { eap.nextcmd }; result = eval_to_bool(eap.arg, error, __, skip); eap.nextcmd = __[0]; }

                /*
                 * When throwing error exceptions, we want to throw always the first
                 * of several errors in a row.  This is what actually happens when
                 * a conditional error was detected above and there is another failure
                 * when parsing the expression.  Since the skip flag is set in this
                 * case, the parsing error will be ignored by emsg().
                 */
                if (!skip && !error[0])
                {
                    if (result)
                        cstack.cs_flags[cstack.cs_idx] = CSF_ACTIVE | CSF_TRUE;
                    else
                        cstack.cs_flags[cstack.cs_idx] = 0;
                }
                else if (eap.errmsg == null)
                    /* set true, so this conditional will never get active */
                    cstack.cs_flags[cstack.cs_idx] = CSF_TRUE;
            }
            else
                cstack.cs_flags[cstack.cs_idx] |= CSF_ELSE;
        }
    };

    /*
     * Handle ":while" and ":for".
     */
    /*private*/ static final ex_func_C ex_while = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            condstack_C cstack = eap.cstack;

            if (cstack.cs_idx == CSTACK_LEN - 1)
                eap.errmsg = u8("E585: :while/:for nesting too deep");
            else
            {
                /*
                 * The loop flag is set when we have jumped back from the matching ":endwhile" or ":endfor".
                 * When not set, need to initialise this cstack entry.
                 */
                if ((cstack.cs_lflags & CSL_HAD_LOOP) == 0)
                {
                    cstack.cs_idx++;
                    cstack.cs_looplevel++;
                    cstack.cs_line[cstack.cs_idx] = -1;
                }
                cstack.cs_flags[cstack.cs_idx] = (eap.cmdidx == CMD_while) ? (short)CSF_WHILE : (short)CSF_FOR;

                /*
                 * Don't do something after an error, interrupt, or throw, or when
                 * there is a surrounding conditional and it was not active.
                 */
                boolean skip = (did_emsg || got_int || did_throw
                                || (0 < cstack.cs_idx && (cstack.cs_flags[cstack.cs_idx - 1] & CSF_ACTIVE) == 0));

                boolean result;
                boolean[] error = new boolean[1];

                if (eap.cmdidx == CMD_while)
                {
                    /*
                     * ":while bool-expr"
                     */
                    { Bytes[] __ = { eap.nextcmd }; result = eval_to_bool(eap.arg, error, __, skip); eap.nextcmd = __[0]; }
                }
                else
                {
                    forinfo_C fi;

                    /*
                     * ":for var in list-expr"
                     */
                    if ((cstack.cs_lflags & CSL_HAD_LOOP) != 0)
                    {
                        /* Jumping here from a ":continue" or ":endfor":
                         * use the previously evaluated list. */
                        fi = cstack.cs_forinfo[cstack.cs_idx];
                        error[0] = false;
                    }
                    else
                    {
                        /* Evaluate the argument and get the info in a structure. */
                        { Bytes[] __ = { eap.nextcmd }; fi = eval_for_line(eap.arg, error, __, skip); eap.nextcmd = __[0]; }
                        cstack.cs_forinfo[cstack.cs_idx] = fi;
                    }

                    /* use the element at the start of the list and advance */
                    if (!error[0] && fi != null && !skip)
                        result = next_for_item(fi, eap.arg);
                    else
                        result = false;

                    if (!result)
                    {
                        free_for_info(fi);
                        cstack.cs_forinfo[cstack.cs_idx] = null;
                    }
                }

                /*
                 * If this cstack entry was just initialised and is active, set the
                 * loop flag, so do_cmdline() will set the line number in cs_line[].
                 * If executing the command a second time, clear the loop flag.
                 */
                if (!skip && !error[0] && result)
                {
                    cstack.cs_flags[cstack.cs_idx] |= (CSF_ACTIVE | CSF_TRUE);
                    cstack.cs_lflags ^= CSL_HAD_LOOP;
                }
                else
                {
                    cstack.cs_lflags &= ~CSL_HAD_LOOP;
                    /*
                     * If the ":while" evaluates to false or ":for" is past the end of
                     * the list, show the debug prompt at the ":endwhile"/":endfor" as
                     * if there was a ":break" in a ":while"/":for" evaluating to true.
                     */
                    if (!skip && !error[0])
                        cstack.cs_flags[cstack.cs_idx] |= CSF_TRUE;
                }
            }
        }
    };

    /*
     * ":continue"
     */
    /*private*/ static final ex_func_C ex_continue = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            condstack_C cstack = eap.cstack;

            if (cstack.cs_looplevel <= 0 || cstack.cs_idx < 0)
                eap.errmsg = u8("E586: :continue without :while or :for");
            else
            {
                /* Try to find the matching ":while".  This might stop at a try conditional
                 * not in its finally clause (which is then to be executed next).  Therefore,
                 * inactivate all conditionals except the ":while" itself (if reached).
                 */
                int idx = cleanup_conditionals(cstack, CSF_WHILE | CSF_FOR, false);
                if (0 <= idx && (cstack.cs_flags[idx] & (CSF_WHILE | CSF_FOR)) != 0)
                {
                    { int[] __ = { cstack.cs_trylevel }; rewind_conditionals(cstack, idx, CSF_TRY, __); cstack.cs_trylevel = __[0]; }

                    /*
                     * Set CSL_HAD_CONT, so do_cmdline() will jump back to the matching ":while".
                     */
                    cstack.cs_lflags |= CSL_HAD_CONT;   /* let do_cmdline() handle it */
                }
                else
                {
                    /* If a try conditional not in its finally clause is reached first,
                     * make the ":continue" pending for execution at the ":endtry". */
                    cstack.cs_pending[idx] = CSTP_CONTINUE;
                    report_make_pending(CSTP_CONTINUE, null);
                }
            }
        }
    };

    /*
     * ":break"
     */
    /*private*/ static final ex_func_C ex_break = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            condstack_C cstack = eap.cstack;

            if (cstack.cs_looplevel <= 0 || cstack.cs_idx < 0)
                eap.errmsg = u8("E587: :break without :while or :for");
            else
            {
                /* Inactivate conditionals until the matching ":while" or a try
                 * conditional not in its finally clause (which is then to be
                 * executed next) is found.  In the latter case, make the ":break"
                 * pending for execution at the ":endtry".
                 */
                int idx = cleanup_conditionals(cstack, CSF_WHILE | CSF_FOR, true);
                if (0 <= idx && (cstack.cs_flags[idx] & (CSF_WHILE | CSF_FOR)) == 0)
                {
                    cstack.cs_pending[idx] = CSTP_BREAK;
                    report_make_pending(CSTP_BREAK, null);
                }
            }
        }
    };

    /*
     * ":endwhile" and ":endfor"
     */
    /*private*/ static final ex_func_C ex_endwhile = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            condstack_C cstack = eap.cstack;

            Bytes err;
            int csf;
            if (eap.cmdidx == CMD_endwhile)
            {
                err = e_while;
                csf = CSF_WHILE;
            }
            else
            {
                err = e_for;
                csf = CSF_FOR;
            }

            if (cstack.cs_looplevel <= 0 || cstack.cs_idx < 0)
                eap.errmsg = err;
            else
            {
                int fl = cstack.cs_flags[cstack.cs_idx];
                if ((fl & csf) == 0)
                {
                    /* If we are in a ":while" or ":for" but used the wrong endloop command,
                     * do not rewind to the next enclosing ":for"/":while". */
                    if ((fl & CSF_WHILE) != 0)
                        eap.errmsg = u8("E732: Using :endfor with :while");
                    else if ((fl & CSF_FOR) != 0)
                        eap.errmsg = u8("E733: Using :endwhile with :for");
                }
                if ((fl & (CSF_WHILE | CSF_FOR)) == 0)
                {
                    if ((fl & CSF_TRY) == 0)
                        eap.errmsg = e_endif;
                    else if ((fl & CSF_FINALLY) != 0)
                        eap.errmsg = e_endtry;
                    /* Try to find the matching ":while" and report what's missing. */
                    int idx;
                    for (idx = cstack.cs_idx; 0 < idx; --idx)
                    {
                        fl = cstack.cs_flags[idx];
                        if ((fl & CSF_TRY) != 0 && (fl & CSF_FINALLY) == 0)
                        {
                            /* Give up at a try conditional not in its finally clause.
                             * Ignore the ":endwhile"/":endfor". */
                            eap.errmsg = err;
                            return;
                        }
                        if ((fl & csf) != 0)
                            break;
                    }
                    /* Cleanup and rewind all contained (and unclosed) conditionals. */
                    cleanup_conditionals(cstack, CSF_WHILE | CSF_FOR, false);
                    { int[] __ = { cstack.cs_trylevel }; rewind_conditionals(cstack, idx, CSF_TRY, __); cstack.cs_trylevel = __[0]; }
                }

                /*
                 * When debugging or a breakpoint was encountered, display the debug
                 * prompt (if not already done).  This shows the user that an
                 * ":endwhile"/":endfor" is executed when the ":while" was not true or
                 * after a ":break".  Handle a ">quit" debug command as if an
                 * interrupt had occurred before the ":endwhile"/":endfor".  That is,
                 * throw an interrupt exception if appropriate.  Doing this here
                 * prevents that an exception for a parsing error is discarded when
                 * throwing the interrupt exception later on.
                 */
                else if ((cstack.cs_flags[cstack.cs_idx] & CSF_TRUE) != 0
                      && (cstack.cs_flags[cstack.cs_idx] & CSF_ACTIVE) == 0
                            && dbg_check_skipped(eap))
                    do_intthrow(cstack);

                /*
                 * Set loop flag, so do_cmdline() will jump back to the matching
                 * ":while" or ":for".
                 */
                cstack.cs_lflags |= CSL_HAD_ENDLOOP;
            }
        }
    };

    /*
     * ":throw expr"
     */
    /*private*/ static final ex_func_C ex_throw = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes arg = eap.arg;
            Bytes value;

            if (arg.at(0) != NUL && arg.at(0) != (byte)'|' && arg.at(0) != (byte)'\n')
            {
                Bytes[] __ = { eap.nextcmd };
                value = eval_to_string_skip(arg, __, eap.skip);
                eap.nextcmd = __[0];
            }
            else
            {
                emsg(e_argreq);
                value = null;
            }

            /* On error or when an exception is thrown during argument evaluation, do not throw. */
            if (!eap.skip && value != null)
            {
                if (throw_exception(value, ET_USER, null) != false)
                    do_throw(eap.cstack);
            }
        }
    };

    /*
     * Throw the current exception through the specified cstack.
     * Common routine for ":throw" (user exception) and error and interrupt exceptions.
     * Also used for rethrowing an uncaught exception.
     */
    /*private*/ static void do_throw(condstack_C cstack)
    {
        boolean inactivate_try = false;

        /*
         * Cleanup and inactivate up to the next surrounding try conditional that
         * is not in its finally clause.  Normally, do not inactivate the try
         * conditional itself, so that its ACTIVE flag can be tested below.  But
         * if a previous error or interrupt has not been converted to an exception,
         * inactivate the try conditional, too, as if the conversion had been done,
         * and reset the did_emsg or got_int flag, so this won't happen again at
         * the next surrounding try conditional.
         */
        int idx = cleanup_conditionals(cstack, 0, inactivate_try);
        if (0 <= idx)
        {
            /*
             * If this try conditional is active and we are before its first
             * ":catch", set THROWN so that the ":catch" commands will check
             * whether the exception matches.  When the exception came from any of
             * the catch clauses, it will be made pending at the ":finally" (if
             * present) and rethrown at the ":endtry".  This will also happen if
             * the try conditional is inactive.  This is the case when we are
             * throwing an exception due to an error or interrupt on the way from
             * a preceding ":continue", ":break", ":return", ":finish", error or
             * interrupt (not converted to an exception) to the finally clause or
             * from a preceding throw of a user or error or interrupt exception to
             * the matching catch clause or the finally clause.
             */
            if ((cstack.cs_flags[idx] & CSF_CAUGHT) == 0)
            {
                if ((cstack.cs_flags[idx] & CSF_ACTIVE) != 0)
                    cstack.cs_flags[idx] |= CSF_THROWN;
                else
                    /* THROWN may have already been set for a catchable exception that has been discarded.
                     * Ensure it is reset for the new exception. */
                    cstack.cs_flags[idx] &= ~CSF_THROWN;
            }
            cstack.cs_flags[idx] &= ~CSF_ACTIVE;
            cstack.cs_rv_ex[idx] = current_exception;
        }

        did_throw = true;
    }

    /*
     * ":try"
     */
    /*private*/ static final ex_func_C ex_try = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            condstack_C cstack = eap.cstack;

            if (cstack.cs_idx == CSTACK_LEN - 1)
            {
                eap.errmsg = u8("E601: :try nesting too deep");
                return;
            }

            cstack.cs_idx++;
            cstack.cs_trylevel++;
            cstack.cs_flags[cstack.cs_idx] = CSF_TRY;
            cstack.cs_pending[cstack.cs_idx] = CSTP_NONE;

            /*
             * Don't do something after an error, interrupt, or throw,
             * or when there is a surrounding conditional and it was not active.
             */
            boolean skip = (did_emsg || got_int || did_throw
                                || (0 < cstack.cs_idx && (cstack.cs_flags[cstack.cs_idx - 1] & CSF_ACTIVE) == 0));

            if (!skip)
            {
                /* Set ACTIVE and true.  true means that the corresponding ":catch"
                 * commands should check for a match if an exception is thrown and
                 * that the finally clause needs to be executed. */
                cstack.cs_flags[cstack.cs_idx] |= CSF_ACTIVE | CSF_TRUE;

                /*
                 * ":silent!", even when used in a try conditional, disables
                 * displaying of error messages and conversion of errors to
                 * exceptions.  When the silent commands again open a try
                 * conditional, save "emsg_silent" and reset it so that errors are
                 * again converted to exceptions.  The value is restored when that
                 * try conditional is left.  If it is left normally, the commands
                 * following the ":endtry" are again silent.  If it is left by
                 * a ":continue", ":break", ":return", or ":finish", the commands
                 * executed next are again silent.  If it is left due to an
                 * aborting error, an interrupt, or an exception, restoring
                 * "emsg_silent" does not matter since we are already in the
                 * aborting state and/or the exception has already been thrown.
                 * The effect is then just freeing the memory that was allocated
                 * to save the value.
                 */
                if (emsg_silent != 0)
                {
                    eslist_C elem = new eslist_C();

                    elem.saved_emsg_silent = emsg_silent;
                    elem.next = cstack.cs_emsg_silent_list;
                    cstack.cs_emsg_silent_list = elem;
                    cstack.cs_flags[cstack.cs_idx] |= CSF_SILENT;
                    emsg_silent = 0;
                }
            }
        }
    };

    /*
     * ":catch /{pattern}/" and ":catch"
     */
    /*private*/ static final ex_func_C ex_catch = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            condstack_C cstack = eap.cstack;

            boolean give_up = false;
            boolean skip = false;
            int idx = 0;

            if (cstack.cs_trylevel <= 0 || cstack.cs_idx < 0)
            {
                eap.errmsg = u8("E603: :catch without :try");
                give_up = true;
            }
            else
            {
                if ((cstack.cs_flags[cstack.cs_idx] & CSF_TRY) == 0)
                {
                    /* Report what's missing if the matching ":try" is not in its finally clause. */
                    eap.errmsg = get_end_emsg(cstack);
                    skip = true;
                }
                for (idx = cstack.cs_idx; 0 < idx; --idx)
                    if ((cstack.cs_flags[idx] & CSF_TRY) != 0)
                        break;
                if ((cstack.cs_flags[idx] & CSF_FINALLY) != 0)
                {
                    /* Give up for a ":catch" after ":finally" and ignore it.  Just parse. */
                    eap.errmsg = u8("E604: :catch after :finally");
                    give_up = true;
                }
                else
                {
                    int[] __ = { cstack.cs_looplevel };
                    rewind_conditionals(cstack, idx, CSF_WHILE | CSF_FOR, __);
                    cstack.cs_looplevel = __[0];
                }
            }

            Bytes pat, end;
            if (ends_excmd(eap.arg.at(0)))   /* no argument, catch all errors */
            {
                pat = u8(".*");
                end = null;
                eap.nextcmd = find_nextcmd(eap.arg);
            }
            else
            {
                pat = eap.arg.plus(1);
                end = skip_regexp(pat, eap.arg.at(0), true, null);
            }

            boolean caught = false;

            if (!give_up)
            {
                /*
                 * Don't do something when no exception has been thrown or when the
                 * corresponding try block never got active (because of an inactive
                 * surrounding conditional or after an error or interrupt or throw).
                 */
                if (!did_throw || (cstack.cs_flags[idx] & CSF_TRUE) == 0)
                    skip = true;

                /*
                 * Check for a match only if an exception is thrown but not caught by a previous ":catch".
                 * An exception that has replaced a discarded exception is not checked (THROWN is not set then).
                 */
                if (!skip && (cstack.cs_flags[idx] & CSF_THROWN) != 0 && (cstack.cs_flags[idx] & CSF_CAUGHT) == 0)
                {
                    if (end != null && end.at(0) != NUL && !ends_excmd(skipwhite(end.plus(1)).at(0)))
                    {
                        emsg(e_trailing);
                        return;
                    }

                    /* When debugging or a breakpoint was encountered, display the
                     * debug prompt (if not already done) before checking for a match.
                     * This is a helpful hint for the user when the regular expression
                     * matching fails.  Handle a ">quit" debug command as if an
                     * interrupt had occurred before the ":catch".  That is, discard
                     * the original exception, replace it by an interrupt exception,
                     * and don't catch it in this try block. */
                    if (!dbg_check_skipped(eap) || !do_intthrow(cstack))
                    {
                        /* Terminate the pattern and avoid the 'l' flag in 'cpoptions' while compiling it. */
                        byte save_char = NUL;
                        if (end != null)
                        {
                            save_char = end.at(0);
                            end.be(0, NUL);
                        }
                        Bytes save_cpo = p_cpo[0];
                        p_cpo[0] = u8("");
                        regmatch_C regmatch = new regmatch_C();
                        regmatch.regprog = vim_regcomp(pat, RE_MAGIC + RE_STRING);
                        regmatch.rm_ic = false;
                        if (end != null)
                            end.be(0, save_char);
                        p_cpo[0] = save_cpo;
                        if (regmatch.regprog == null)
                            emsg2(e_invarg2, pat);
                        else
                        {
                            /*
                             * Save the value of got_int and reset it.  We don't want
                             * a previous interruption cancel matching, only hitting
                             * CTRL-C while matching should abort it.
                             */
                            boolean prev_got_int = got_int;
                            got_int = false;
                            caught = vim_regexec_nl(regmatch, current_exception.value, 0);
                            got_int |= prev_got_int;
                        }
                    }
                }

                if (caught)
                {
                    /* Make this ":catch" clause active and reset did_emsg, got_int,
                     * and did_throw.  Put the exception on the caught stack. */
                    cstack.cs_flags[idx] |= CSF_ACTIVE | CSF_CAUGHT;
                    did_emsg = got_int = did_throw = false;
                    catch_exception((except_C)cstack.cs_rv_ex[idx]);
                    /* It's mandatory that the current exception is stored in the cstack
                     * so that it can be discarded at the next ":catch", ":finally", or
                     * ":endtry" or when the catch clause is left by a ":continue",
                     * ":break", ":return", ":finish", error, interrupt, or another
                     * exception. */
                    if (cstack.cs_rv_ex[cstack.cs_idx] != current_exception)
                        emsg(e_internal);
                }
                else
                {
                    /*
                     * If there is a preceding catch clause and it caught the exception,
                     * finish the exception now.  This happens also after errors except
                     * when this ":catch" was after the ":finally" or not within
                     * a ":try".  Make the try conditional inactive so that the
                     * following catch clauses are skipped.  On an error or interrupt
                     * after the preceding try block or catch clause was left by
                     * a ":continue", ":break", ":return", or ":finish", discard the
                     * pending action.
                     */
                    cleanup_conditionals(cstack, CSF_TRY, true);
                }
            }

            if (end != null)
                eap.nextcmd = find_nextcmd(end);
        }
    };

    /*
     * ":finally"
     */
    /*private*/ static final ex_func_C ex_finally = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            condstack_C cstack = eap.cstack;

            if (cstack.cs_trylevel <= 0 || cstack.cs_idx < 0)
            {
                eap.errmsg = u8("E606: :finally without :try");
                return;
            }

            int idx = cstack.cs_idx;
            int pending = CSTP_NONE;

            if ((cstack.cs_flags[idx] & CSF_TRY) == 0)
            {
                eap.errmsg = get_end_emsg(cstack);
                for (idx = cstack.cs_idx - 1; 0 < idx; --idx)
                    if ((cstack.cs_flags[idx] & CSF_TRY) != 0)
                        break;
                /* Make this error pending, so that the commands in the following
                 * finally clause can be executed.  This overrules also a pending
                 * ":continue", ":break", ":return", or ":finish". */
                pending = CSTP_ERROR;
            }

            if ((cstack.cs_flags[idx] & CSF_FINALLY) != 0)
            {
                /* Give up for a multiple ":finally" and ignore it. */
                eap.errmsg = u8("E607: multiple :finally");
                return;
            }

            { int[] __ = { cstack.cs_looplevel }; rewind_conditionals(cstack, idx, CSF_WHILE | CSF_FOR, __); cstack.cs_looplevel = __[0]; }

            /*
             * Don't do something when the corresponding try block never got active
             * (because of an inactive surrounding conditional or after an error or
             * interrupt or throw) or for a ":finally" without ":try" or a multiple
             * ":finally".  After every other error (did_emsg or the conditional
             * errors detected above) or after an interrupt (got_int) or an
             * exception (did_throw), the finally clause must be executed.
             */
            boolean skip = ((cstack.cs_flags[cstack.cs_idx] & CSF_TRUE) == 0);

            if (!skip)
            {
                /* When debugging or a breakpoint was encountered, display the
                 * debug prompt (if not already done).  The user then knows that
                 * the finally clause is executed. */
                if (dbg_check_skipped(eap))
                {
                    /* Handle a ">quit" debug command as if an interrupt had
                     * occurred before the ":finally".  That is, discard the
                     * original exception and replace it by an interrupt
                     * exception. */
                    do_intthrow(cstack);
                }

                /*
                 * If there is a preceding catch clause and it caught the exception,
                 * finish the exception now.  This happens also after errors except
                 * when this is a multiple ":finally" or one not within a ":try".
                 * After an error or interrupt, this also discards a pending
                 * ":continue", ":break", ":finish", or ":return" from the preceding
                 * try block or catch clause.
                 */
                cleanup_conditionals(cstack, CSF_TRY, false);

                /*
                 * Make did_emsg, got_int, did_throw pending.  If set, they overrule
                 * a pending ":continue", ":break", ":return", or ":finish".  Then
                 * we have particularly to discard a pending return value (as done
                 * by the call to cleanup_conditionals() above when did_emsg or
                 * got_int is set).  The pending values are restored by the
                 * ":endtry", except if there is a new error, interrupt, exception,
                 * ":continue", ":break", ":return", or ":finish" in the following
                 * finally clause.  A missing ":endwhile", ":endfor" or ":endif"
                 * detected here is treated as if did_emsg and did_throw had
                 * already been set, respectively in case that the error is not
                 * converted to an exception, did_throw had already been unset.
                 * We must not set did_emsg here since that would suppress the
                 * error message.
                 */
                if (pending == CSTP_ERROR || did_emsg || got_int || did_throw)
                {
                    if (cstack.cs_pending[cstack.cs_idx] == CSTP_RETURN)
                    {
                        report_discard_pending(CSTP_RETURN, cstack.cs_rv_ex[cstack.cs_idx]);
                        discard_pending_return((typval_C)cstack.cs_rv_ex[cstack.cs_idx]);
                    }
                    if (pending == CSTP_ERROR && !did_emsg)
                        pending |= CSTP_THROW;
                    else
                        pending |= did_throw ? CSTP_THROW : 0;
                    pending |= did_emsg  ? CSTP_ERROR     : 0;
                    pending |= got_int   ? CSTP_INTERRUPT : 0;
                    cstack.cs_pending[cstack.cs_idx] = (byte)pending;

                    /* It's mandatory that the current exception is stored in the
                     * cstack so that it can be rethrown at the ":endtry" or be
                     * discarded if the finally clause is left by a ":continue",
                     * ":break", ":return", ":finish", error, interrupt, or another
                     * exception.  When emsg() is called for a missing ":endif" or
                     * a missing ":endwhile"/":endfor" detected here, the
                     * exception will be discarded. */
                    if (did_throw && cstack.cs_rv_ex[cstack.cs_idx] != current_exception)
                        emsg(e_internal);
                }

                /*
                 * Set CSL_HAD_FINA, so do_cmdline() will reset did_emsg,
                 * got_int, and did_throw and make the finally clause active.
                 * This will happen after emsg() has been called for a missing
                 * ":endif" or a missing ":endwhile"/":endfor" detected here, so
                 * that the following finally clause will be executed even then.
                 */
                cstack.cs_lflags |= CSL_HAD_FINA;
            }
        }
    };

    /*
     * ":endtry"
     */
    /*private*/ static final ex_func_C ex_endtry = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            condstack_C cstack = eap.cstack;

            if (cstack.cs_trylevel <= 0 || cstack.cs_idx < 0)
            {
                eap.errmsg = u8("E602: :endtry without :try");
                return;
            }

            int idx = cstack.cs_idx;
            boolean rethrow = false;

            /*
             * Don't do something after an error, interrupt or throw in the try
             * block, catch clause, or finally clause preceding this ":endtry" or
             * when an error or interrupt occurred after a ":continue", ":break",
             * ":return", or ":finish" in a try block or catch clause preceding this
             * ":endtry" or when the try block never got active (because of an
             * inactive surrounding conditional or after an error or interrupt or
             * throw) or when there is a surrounding conditional and it has been
             * made inactive by a ":continue", ":break", ":return", or ":finish" in
             * the finally clause.  The latter case need not be tested since then
             * anything pending has already been discarded.
             */
            boolean skip = (did_emsg || got_int || did_throw || (cstack.cs_flags[idx] & CSF_TRUE) == 0);

            if ((cstack.cs_flags[idx] & CSF_TRY) == 0)
            {
                eap.errmsg = get_end_emsg(cstack);

                /* Find the matching ":try" and report what's missing. */
                idx = cstack.cs_idx;
                do
                {
                    --idx;
                } while (0 < idx && (cstack.cs_flags[idx] & CSF_TRY) == 0);
                { int[] __ = { cstack.cs_looplevel }; rewind_conditionals(cstack, idx, CSF_WHILE | CSF_FOR, __); cstack.cs_looplevel = __[0]; }
                skip = true;

                /*
                 * If an exception is being thrown, discard it to prevent it
                 * from being rethrown at the end of this function.  It would be
                 * discarded by the error message, anyway.  Resets did_throw.
                 * This does not affect the script termination due to the error
                 * since "trylevel" is decremented after emsg() has been called.
                 */
                if (did_throw)
                    discard_current_exception();
            }
            else
            {
                /*
                 * If we stopped with the exception currently being thrown at this
                 * try conditional since we didn't know that it doesn't have
                 * a finally clause, we need to rethrow it after closing the try
                 * conditional.
                 */
                if (did_throw && (cstack.cs_flags[idx] & CSF_TRUE) != 0 && (cstack.cs_flags[idx] & CSF_FINALLY) == 0)
                    rethrow = true;
            }

            /* If there was no finally clause, show the user when debugging or
             * a breakpoint was encountered that the end of the try conditional has
             * been reached: display the debug prompt (if not already done).  Do
             * this on normal control flow or when an exception was thrown, but not
             * on an interrupt or error not converted to an exception or when
             * a ":break", ":continue", ":return", or ":finish" is pending.  These
             * actions are carried out immediately.
             */
            if ((rethrow || (!skip
                            && (cstack.cs_flags[idx] & CSF_FINALLY) == 0
                            && cstack.cs_pending[idx] == 0))
                    && dbg_check_skipped(eap))
            {
                /* Handle a ">quit" debug command as if an interrupt had occurred before the ":endtry".
                 * That is, throw an interrupt exception and set "skip" and "rethrow". */
                if (got_int)
                {
                    skip = true;
                    do_intthrow(cstack);
                    /* The do_intthrow() call may have reset did_throw or cstack.cs_pending[idx]. */
                    rethrow = false;
                    if (did_throw && (cstack.cs_flags[idx] & CSF_FINALLY) == 0)
                        rethrow = true;
                }
            }

            int pending = CSTP_NONE;
            typval_C rtv = null;

            /*
             * If a ":return" is pending, we need to resume it after closing the
             * try conditional; remember the return value.  If there was a finally
             * clause making an exception pending, we need to rethrow it.  Make it
             * the exception currently being thrown.
             */
            if (!skip)
            {
                pending = cstack.cs_pending[idx];
                cstack.cs_pending[idx] = CSTP_NONE;
                if (pending == CSTP_RETURN)
                    rtv = (typval_C)cstack.cs_rv_ex[idx];
                else if ((pending & CSTP_THROW) != 0)
                    current_exception = (except_C)cstack.cs_rv_ex[idx];
            }

            /*
             * Discard anything pending on an error, interrupt, or throw in the
             * finally clause.  If there was no ":finally", discard a pending
             * ":continue", ":break", ":return", or ":finish" if an error or
             * interrupt occurred afterwards, but before the ":endtry" was reached.
             * If an exception was caught by the last of the catch clauses and there
             * was no finally clause, finish the exception now.  This happens also
             * after errors except when this ":endtry" is not within a ":try".
             * Restore "emsg_silent" if it has been reset by this try conditional.
             */
            cleanup_conditionals(cstack, CSF_TRY | CSF_SILENT, true);

            --cstack.cs_idx;
            --cstack.cs_trylevel;

            if (!skip)
            {
                report_resume_pending(pending, (pending == CSTP_RETURN) ? rtv : (pending & CSTP_THROW) != 0 ? current_exception : null);

                switch (pending)
                {
                    case CSTP_NONE:
                        break;

                    /* Reactivate a pending ":continue", ":break", ":return",
                     * ":finish" from the try block or a catch clause of this try
                     * conditional.  This is skipped, if there was an error in an
                     * (unskipped) conditional command or an interrupt afterwards
                     * or if the finally clause is present and executed a new error,
                     * interrupt, throw, ":continue", ":break", ":return", or ":finish".
                     */
                    case CSTP_CONTINUE:
                        ex_continue.ex(eap);
                        break;
                    case CSTP_BREAK:
                        ex_break.ex(eap);
                        break;
                    case CSTP_RETURN:
                        do_return(eap, false, false, rtv);
                        break;
                    case CSTP_FINISH:
                        do_finish(eap, false);
                        break;

                    /* When the finally clause was entered due to an error,
                     * interrupt or throw (as opposed to a ":continue", ":break",
                     * ":return", or ":finish"), restore the pending values of
                     * did_emsg, got_int, and did_throw.  This is skipped, if there
                     * was a new error, interrupt, throw, ":continue", ":break",
                     * ":return", or ":finish".  in the finally clause.
                     */
                    default:
                        if ((pending & CSTP_ERROR) != 0)
                            did_emsg = true;
                        if ((pending & CSTP_INTERRUPT) != 0)
                            got_int = true;
                        if ((pending & CSTP_THROW) != 0)
                            rethrow = true;
                        break;
                }
            }

            if (rethrow)
                /* Rethrow the current exception (within this cstack). */
                do_throw(cstack);
        }
    };

    /*
     * enter_cleanup() and leave_cleanup()
     *
     * Functions to be called before/after invoking a sequence of autocommands for
     * cleanup for a failed command.  (Failure means here that a call to emsg()
     * has been made, an interrupt occurred, or there is an uncaught exception
     * from a previous autocommand execution of the same command.)
     *
     * Call enter_cleanup() with a pointer to a cleanup_C and pass the same
     * pointer to leave_cleanup().  The cleanup_C structure stores the pending
     * error/interrupt/exception state.
     */

    /*
     * This function works a bit like ex_finally() except that there was not
     * actually an extra try block around the part that failed and an error or
     * interrupt has not (yet) been converted to an exception.  This function
     * saves the error/interrupt/ exception state and prepares for the call to
     * do_cmdline() that is going to be made for the cleanup autocommand execution.
     */
    /*private*/ static void enter_cleanup(cleanup_C csp)
    {
        int pending = CSTP_NONE;

        /*
         * Postpone did_emsg, got_int, did_throw.  The pending values will be
         * restored by leave_cleanup() except if there was an aborting error,
         * interrupt, or uncaught exception after this function ends.
         */
        if (did_emsg || got_int || did_throw || need_rethrow)
        {
            csp.pending = (did_emsg     ? CSTP_ERROR     : 0)
                        | (got_int      ? CSTP_INTERRUPT : 0)
                        | (did_throw    ? CSTP_THROW     : 0)
                        | (need_rethrow ? CSTP_THROW     : 0);

            /* If we are currently throwing an exception (did_throw), save it as
             * well.  On an error not yet converted to an exception, update
             * "force_abort" and reset "cause_abort" (as do_errthrow() would do).
             * This is needed for the do_cmdline() call that is going to be made
             * for autocommand execution.  We need not save "*msg_list", because
             * there is an extra instance for every call of do_cmdline(), anyway.
             */
            if (did_throw || need_rethrow)
                csp.exception = current_exception;
            else
            {
                csp.exception = null;
                if (did_emsg)
                {
                    force_abort |= cause_abort;
                    cause_abort = false;
                }
            }
            did_emsg = got_int = did_throw = need_rethrow = false;

            /* Report if required by the 'verbose' option or when debugging. */
            report_make_pending(pending, csp.exception);
        }
        else
        {
            csp.pending = CSTP_NONE;
            csp.exception = null;
        }
    }

    /*
     * See comment above enter_cleanup() for how this function is used.
     *
     * This function is a bit like ex_endtry() except that there was not actually
     * an extra try block around the part that failed and an error or interrupt
     * had not (yet) been converted to an exception when the cleanup autocommand
     * sequence was invoked.
     *
     * This function has to be called with the address of the cleanup_C structure
     * filled by enter_cleanup() as an argument; it restores the error/interrupt/
     * exception state saved by that function - except there was an aborting
     * error, an interrupt or an uncaught exception during execution of the
     * cleanup autocommands.  In the latter case, the saved error/interrupt/
     * exception state is discarded.
     */
    /*private*/ static void leave_cleanup(cleanup_C csp)
    {
        int pending = csp.pending;

        if (pending == CSTP_NONE)   /* nothing to do */
            return;

        /* If there was an aborting error, an interrupt, or an uncaught exception
         * after the corresponding call to enter_cleanup(), discard what has been
         * made pending by it.  Report this to the user if required by the
         * 'verbose' option or when debugging. */
        if (aborting() || need_rethrow)
        {
            if ((pending & CSTP_THROW) != 0)
                /* Cancel the pending exception (includes report). */
                discard_exception(csp.exception, false);
            else
                report_discard_pending(pending, null);

            /* If an error was about to be converted to an exception
             * when enter_cleanup() was called, free the message list. */
            if (msg_list != null)
                free_global_msglist();
        }

        /*
         * If there was no new error, interrupt, or throw between the calls
         * to enter_cleanup() and leave_cleanup(), restore the pending
         * error/interrupt/exception state.
         */
        else
        {
            /*
             * If there was an exception being thrown when enter_cleanup() was
             * called, we need to rethrow it.  Make it the exception currently being thrown.
             */
            if ((pending & CSTP_THROW) != 0)
                current_exception = csp.exception;

            /*
             * If an error was about to be converted to an exception when
             * enter_cleanup() was called, let "cause_abort" take the part of
             * "force_abort" (as done by cause_errthrow()).
             */
            else if ((pending & CSTP_ERROR) != 0)
            {
                cause_abort = force_abort;
                force_abort = false;
            }

            /*
             * Restore the pending values of did_emsg, got_int, and did_throw.
             */
            if ((pending & CSTP_ERROR) != 0)
                did_emsg = true;
            if ((pending & CSTP_INTERRUPT) != 0)
                got_int = true;
            if ((pending & CSTP_THROW) != 0)
                need_rethrow = true;    /* did_throw will be set by do_one_cmd() */

            /* Report if required by the 'verbose' option or when debugging. */
            report_resume_pending(pending, (pending & CSTP_THROW) != 0 ? current_exception : null);
        }
    }

    /*
     * Make conditionals inactive and discard what's pending in finally clauses
     * until the conditional type searched for or a try conditional not in its
     * finally clause is reached.  If this is in an active catch clause, finish
     * the caught exception.
     * Return the cstack index where the search stopped.
     * Values used for "searched_cond" are (CSF_WHILE | CSF_FOR) or CSF_TRY or 0,
     * the latter meaning the innermost try conditional not in its finally clause.
     * "inclusive" tells whether the conditional searched for should be made
     * inactive itself (a try conditional not in its finally clause possibly find
     * before is always made inactive).  If "inclusive" is true and
     * "searched_cond" is CSF_TRY|CSF_SILENT, the saved former value of
     * "emsg_silent", if reset when the try conditional finally reached was
     * entered, is restored (used by ex_endtry()).  This is normally done only
     * when such a try conditional is left.
     */
    /*private*/ static int cleanup_conditionals(condstack_C cstack, int searched_cond, boolean inclusive)
    {
        int idx;
        boolean stop = false;

        for (idx = cstack.cs_idx; 0 <= idx; --idx)
        {
            if ((cstack.cs_flags[idx] & CSF_TRY) != 0)
            {
                /*
                 * Discard anything pending in a finally clause and continue the
                 * search.  There may also be a pending ":continue", ":break",
                 * ":return", or ":finish" before the finally clause.  We must not
                 * discard it, unless an error or interrupt occurred afterwards.
                 */
                if (did_emsg || got_int || (cstack.cs_flags[idx] & CSF_FINALLY) != 0)
                {
                    switch (cstack.cs_pending[idx])
                    {
                        case CSTP_NONE:
                            break;

                        case CSTP_CONTINUE:
                        case CSTP_BREAK:
                        case CSTP_FINISH:
                            report_discard_pending(cstack.cs_pending[idx], null);
                            cstack.cs_pending[idx] = CSTP_NONE;
                            break;

                        case CSTP_RETURN:
                            report_discard_pending(CSTP_RETURN, cstack.cs_rv_ex[idx]);
                            discard_pending_return((typval_C)cstack.cs_rv_ex[idx]);
                            cstack.cs_pending[idx] = CSTP_NONE;
                            break;

                        default:
                            if ((cstack.cs_flags[idx] & CSF_FINALLY) != 0)
                            {
                                if ((cstack.cs_pending[idx] & CSTP_THROW) != 0)
                                {
                                    /* Cancel the pending exception.  This is in the finally clause,
                                     * so that the stack of the caught exceptions is not involved. */
                                    discard_exception((except_C)cstack.cs_rv_ex[idx], false);
                                }
                                else
                                    report_discard_pending(cstack.cs_pending[idx], null);
                                cstack.cs_pending[idx] = CSTP_NONE;
                            }
                            break;
                    }
                }

                /*
                 * Stop at a try conditional not in its finally clause.  If this try
                 * conditional is in an active catch clause, finish the caught exception.
                 */
                if ((cstack.cs_flags[idx] & CSF_FINALLY) == 0)
                {
                    if ((cstack.cs_flags[idx] & CSF_ACTIVE) != 0 && (cstack.cs_flags[idx] & CSF_CAUGHT) != 0)
                        finish_exception((except_C)cstack.cs_rv_ex[idx]);
                    /* Stop at this try conditional - except the try block never
                     * got active (because of an inactive surrounding conditional
                     * or when the ":try" appeared after an error or interrupt or throw). */
                    if ((cstack.cs_flags[idx] & CSF_TRUE) != 0)
                    {
                        if (searched_cond == 0 && !inclusive)
                            break;
                        stop = true;
                    }
                }
            }

            /* Stop on the searched conditional type (even when the surrounding
             * conditional is not active or something has been made pending).
             * If "inclusive" is true and "searched_cond" is CSF_TRY|CSF_SILENT,
             * check first whether "emsg_silent" needs to be restored. */
            if ((cstack.cs_flags[idx] & searched_cond) != 0)
            {
                if (!inclusive)
                    break;
                stop = true;
            }
            cstack.cs_flags[idx] &= ~CSF_ACTIVE;
            if (stop && searched_cond != (CSF_TRY | CSF_SILENT))
                break;

            /*
             * When leaving a try conditional that reset "emsg_silent" on its
             * entry after saving the original value, restore that value here and
             * free the memory used to store it.
             */
            if ((cstack.cs_flags[idx] & CSF_TRY) != 0 && (cstack.cs_flags[idx] & CSF_SILENT) != 0)
            {
                eslist_C elem = cstack.cs_emsg_silent_list;
                cstack.cs_emsg_silent_list = elem.next;
                emsg_silent = elem.saved_emsg_silent;
                cstack.cs_flags[idx] &= ~CSF_SILENT;
            }
            if (stop)
                break;
        }

        return idx;
    }

    /*
     * Return an appropriate error message for a missing endwhile/endfor/endif.
     */
    /*private*/ static Bytes get_end_emsg(condstack_C cstack)
    {
        if ((cstack.cs_flags[cstack.cs_idx] & CSF_WHILE) != 0)
            return e_endwhile;
        if ((cstack.cs_flags[cstack.cs_idx] & CSF_FOR) != 0)
            return e_endfor;

        return e_endif;
    }

    /*
     * Rewind conditionals until index "idx" is reached.  "cond_type" and
     * "cond_level" specify a conditional type and the address of a level variable
     * which is to be decremented with each skipped conditional of the specified type.
     * Also free "for info" structures where needed.
     */
    /*private*/ static void rewind_conditionals(condstack_C cstack, int idx, int cond_type, int[] cond_level)
    {
        while (idx < cstack.cs_idx)
        {
            if ((cstack.cs_flags[cstack.cs_idx] & cond_type) != 0)
                --cond_level[0];
            if ((cstack.cs_flags[cstack.cs_idx] & CSF_FOR) != 0)
                free_for_info(cstack.cs_forinfo[cstack.cs_idx]);
            --cstack.cs_idx;
        }
    }

    /*
     * ":endfunction" when not after a ":function"
     */
    /*private*/ static final ex_func_C ex_endfunction = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            emsg(u8("E193: :endfunction not inside a function"));
        }
    };

    /*
     * Return true if the string "p" looks like a ":while" or ":for" command.
     */
    /*private*/ static boolean has_loop_cmd(Bytes p)
    {
        /* skip modifiers, white space and ':' */
        for ( ; ; )
        {
            while (p.at(0) == (byte)' ' || p.at(0) == (byte)'\t' || p.at(0) == (byte)':')
                p = p.plus(1);
            int len = modifier_len(p);
            if (len == 0)
                break;
            p = p.plus(len);
        }
        if ((p.at(0) == (byte)'w' && p.at(1) == (byte)'h') || (p.at(0) == (byte)'f' && p.at(1) == (byte)'o' && p.at(2) == (byte)'r'))
            return true;

        return false;
    }
}
