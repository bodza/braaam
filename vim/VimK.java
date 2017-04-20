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
public class VimK
{
    /*
     * normal.c ---------------------------------------------------------------------------------------
     * Contains the main routine for processing characters in command mode.
     * Communicates closely with the code in ops.c to handle the operators.
     */

    /*
     * The Visual area is remembered for reselection.
     */
    /*private*/ static int      resel_VIsual_mode = NUL;        /* 'v', 'V', or Ctrl-V */
    /*private*/ static long     resel_VIsual_line_count;        /* number of lines */
    /*private*/ static int      resel_VIsual_vcol;              /* nr of cols or end col */
    /*private*/ static int      VIsual_mode_orig = NUL;         /* saved Visual mode */

    /*private*/ static int      restart_VIsual_select;

    /*
     * nv_*(): functions called to handle Normal and Visual mode commands.
     * n_*(): functions called to handle Normal mode commands.
     * v_*(): functions called to handle Visual mode commands.
     */

    /*private*/ static Bytes e_noident = u8("E349: No identifier under cursor");

    /* Values for cmd_flags. */
    /*private*/ static final int NV_NCH      = 0x01;            /* may need to get a second char */
    /*private*/ static final int NV_NCH_NOP  = 0x02 | NV_NCH;   /* get second char when no operator pending */
    /*private*/ static final int NV_NCH_ALW  = 0x04 | NV_NCH;   /* always get a second char */
    /*private*/ static final int NV_LANG     = 0x08;            /* second char needs language adjustment */

    /*private*/ static final int NV_SS       = 0x10;            /* may start selection */
    /*private*/ static final int NV_SSS      = 0x20;            /* may start selection with shift modifier */
    /*private*/ static final int NV_STS      = 0x40;            /* may stop selection without shift modif. */
    /*private*/ static final int NV_RL       = 0x80;            /* 'rightleft' modifies command */
    /*private*/ static final int NV_KEEPREG  = 0x100;           /* don't clear regname */
    /*private*/ static final int NV_NCW      = 0x200;           /* not allowed in command-line window */

    /*
     * Generally speaking, every Normal mode command should either clear any
     * pending operator (with *clearop*()), or set the motion type variable
     * oap.motion_type.
     *
     * When a cursor motion command is made, it is marked as being a character or
     * line oriented motion.  Then, if an operator is in effect, the operation
     * becomes character or line oriented accordingly.
     */

    /*
     * Function to be called for a Normal or Visual mode command.
     * The argument is a cmdarg_C.
     */
    /*private*/ static abstract class nv_func_C
    {
        public abstract void nv(cmdarg_C cap);
    }

    /*private*/ static final class nv_cmd_C
    {
        int         cmd_char;       /* (first) command character */
        nv_func_C   cmd_func;       /* function for this command */
        int         cmd_flags;      /* NV_ flags */
        int         cmd_arg;        /* value for ca.arg */

        /*private*/ nv_cmd_C(int cmd_char, nv_func_C cmd_func, int cmd_flags, int cmd_arg)
        {
            this.cmd_char = cmd_char;
            this.cmd_func = cmd_func;
            this.cmd_flags = cmd_flags;
            this.cmd_arg = cmd_arg;
        }
    }

    /*
     * Compare function for qsort() below, that checks the command
     * character through the index in nv_cmd_idx[].
     */
    /*private*/ static final Comparator<Short> nv_compare = new Comparator<Short>()
    {
        public int compare(Short s1, Short s2)
        {
            /* The commands are sorted on absolute value. */
            int c1 = nv_cmds[s1].cmd_char;
            int c2 = nv_cmds[s2].cmd_char;
            if (c1 < 0)
                c1 = -c1;
            if (c2 < 0)
                c2 = -c2;
            return c1 - c2;
        }
    };

    /*
     * Initialize the nv_cmd_idx[] table.
     */
    /*private*/ static void init_normal_cmds()
    {
        /* Fill the index table with a one to one relation. */
        for (int i = 0; i < nv_cmds.length; i++)
            nv_cmd_idx[i] = (short)i;

        /* Sort the commands by the command character. */
        Arrays.sort(nv_cmd_idx, nv_compare);

        /* Find the first entry that can't be indexed by the command character. */
        int i;
        for (i = 0; i < nv_cmds.length; i++)
            if (nv_cmds[nv_cmd_idx[i]].cmd_char != i)
                break;
        nv_max_linear = i - 1;
    }

    /*
     * Search for a command in the commands table.
     * Returns -1 for invalid command.
     */
    /*private*/ static int find__command(int cmdchar)
    {
        /* A multi-byte character is never a command. */
        if (0x100 <= cmdchar)
            return -1;

        /* We use the absolute value of the character.
         * Special keys have a negative value, but are sorted on their absolute value. */
        if (cmdchar < 0)
            cmdchar = -cmdchar;

        /* If the character is in the first part: The character is the index into nv_cmd_idx[]. */
        if (cmdchar <= nv_max_linear)
            return nv_cmd_idx[cmdchar];

        /* Perform a binary search. */
        int bot = nv_max_linear + 1;
        int top = nv_cmds.length - 1;
        int idx = -1;
        while (bot <= top)
        {
            int i = (top + bot) / 2;
            int c = nv_cmds[nv_cmd_idx[i]].cmd_char;
            if (c < 0)
                c = -c;
            if (cmdchar == c)
            {
                idx = nv_cmd_idx[i];
                break;
            }
            if (c < cmdchar)
                bot = i + 1;
            else
                top = i - 1;
        }
        return idx;
    }

    /*private*/ static int old_mapped_len;

    /*
     * Execute a command in Normal mode.
     */
    /*private*/ static void normal_cmd(oparg_C oap, boolean toplevel)
        /* toplevel: true when called from main() */
    {
        int old_col = curwin.w_curswant;

        cmdarg_C ca = new cmdarg_C();   /* command arguments */
        ca.oap = oap;

        /* Use a count remembered from before entering an operator.
         * After typing "3d" we return from normal_cmd() and come back here,
         * the "3" is remembered in "opcount". */
        ca.opcount = opcount;

        /*
         * If there is an operator pending, then the command we take this time
         * will terminate it.  finish_op tells us to finish the operation before
         * returning this time (unless the operation was cancelled).
         */
        boolean save_finish_op = finish_op;
        finish_op = (oap.op_type != OP_NOP);
        if (finish_op != save_finish_op)
        {
            ui_cursor_shape();              /* may show different cursor shape */
        }

        boolean[] set_prevcount = { false };
        /* When not finishing an operator and no register name typed, reset the count. */
        if (!finish_op && oap.regname == 0)
        {
            ca.opcount = 0;
            set_prevcount[0] = true;
        }

        /* Restore counts from before receiving K_CURSORHOLD.
         * This means after typing "3", handling K_CURSORHOLD
         * and then typing "2" we get "32", not "3 * 2". */
        if (0 < oap.prev_opcount || 0 < oap.prev_count0)
        {
            ca.opcount = oap.prev_opcount;
            ca.count0 = oap.prev_count0;
            oap.prev_opcount = 0;
            oap.prev_count0 = 0;
        }

        int mapped_len = typebuf_maplen();

        State = NORMAL_BUSY;

        /* Set v:count here, when called from main() and not a stuffed
         * command, so that v:count can be used in an expression mapping
         * when there is no count.  Do set it for redo. */
        if (toplevel && readbuf1_empty())
            set_vcount_ca(ca, set_prevcount);

        /*
         * Get the command character from the user.
         */
        int c = safe_vgetc();

        /*
         * If a mapping was started in Visual or Select mode, remember the length
         * of the mapping.  This is used below to not return to Insert mode for as
         * long as the mapping is being executed.
         */
        if (restart_edit == 0)
            old_mapped_len = 0;
        else if (old_mapped_len != 0 || (VIsual_active && mapped_len == 0 && 0 < typebuf_maplen()))
            old_mapped_len = typebuf_maplen();

        if (c == NUL)
            c = K_ZERO;

        /*
         * In Select mode, typed text replaces the selection.
         */
        if (VIsual_active && VIsual_select && (vim_isprintc(c) || c == NL || c == CAR || c == K_KENTER))
        {
            /* Fake a "c"hange command.
             * When "restart_edit" is set (e.g., because 'insertmode' is set)
             * fake a "d"elete command, Insert mode will restart automatically.
             * Insert the typed character in the typeahead buffer, so it can be
             * mapped in Insert mode.  Required for ":lmap" to work. */
            ins_char_typebuf(c);
            if (restart_edit != 0)
                c = 'd';
            else
                c = 'c';
            msg_nowait = true;      /* don't delay going to insert mode */
            old_mapped_len = 0;     /* do go to Insert mode */
        }

        boolean need_flushbuf = add_to_showcmd(c);      /* need to call out_flush() */
        boolean ctrl_w = false;                         /* got CTRL-W command */

        getcount:
        for ( ; ; )
        {
            if (!(VIsual_active && VIsual_select))
            {
                /*
                 * Handle a count before a command and compute ca.count0.
                 * Note that '0' is a command and not the start of a count,
                 * but it's part of a count after other digits.
                 */
                while (('1' <= c && c <= '9') || (ca.count0 != 0 && (c == K_DEL || c == K_KDEL || c == '0')))
                {
                    if (c == K_DEL || c == K_KDEL)
                    {
                        ca.count0 /= 10;
                        del_from_showcmd(4);    /* delete the digit and ~@% */
                    }
                    else
                        ca.count0 = ca.count0 * 10 + (c - '0');
                    if (ca.count0 < 0)          /* got too large! */
                        ca.count0 = 999999999L;
                    /* Set v:count here, when called from main() and not a stuffed
                     * command, so that v:count can be used in an expression mapping
                     * right after the count.  Do set it for redo. */
                    if (toplevel && readbuf1_empty())
                        set_vcount_ca(ca, set_prevcount);
                    if (ctrl_w)
                    {
                        no_mapping++;
                        allow_keys++;           /* no mapping for nchar, but keys */
                    }
                    no_zero_mapping++;          /* don't map zero here */
                    c = plain_vgetc();
                    --no_zero_mapping;
                    if (ctrl_w)
                    {
                        --no_mapping;
                        --allow_keys;
                    }
                    need_flushbuf |= add_to_showcmd(c);
                }

                /*
                 * If we got CTRL-W there may be a/another count
                 */
                if (c == Ctrl_W && !ctrl_w && oap.op_type == OP_NOP)
                {
                    ctrl_w = true;
                    ca.opcount = ca.count0;     /* remember first count */
                    ca.count0 = 0;
                    no_mapping++;
                    allow_keys++;               /* no mapping for nchar, but keys */
                    c = plain_vgetc();          /* get next character */
                    --no_mapping;
                    --allow_keys;
                    need_flushbuf |= add_to_showcmd(c);
                    continue getcount;              /* jump back */
                }
            }

            break;
        }

        if (c == K_CURSORHOLD)
        {
            /* Save the count values so that ca.opcount and ca.count0 are exactly
             * the same when coming back here after handling K_CURSORHOLD. */
            oap.prev_opcount = ca.opcount;
            oap.prev_count0 = ca.count0;
        }
        else if (ca.opcount != 0)
        {
            /*
             * If we're in the middle of an operator (including after entering a
             * yank buffer with '"') AND we had a count before the operator, then
             * that count overrides the current value of ca.count0.
             * What this means effectively, is that commands like "3dw" get turned
             * into "d3w" which makes things fall into place pretty neatly.
             * If you give a count before AND after the operator, they are multiplied.
             */
            if (ca.count0 != 0)
                ca.count0 *= ca.opcount;
            else
                ca.count0 = ca.opcount;
        }

        /*
         * Always remember the count.
         * It will be set to zero (on the next call, above) when there is no pending operator.
         * When called from main(), save the count for use by the "count" built-in variable.
         */
        ca.opcount = ca.count0;
        ca.count1 = (ca.count0 == 0) ? 1 : ca.count0;

        /*
         * Only set v:count when called from main() and not a stuffed command.
         * Do set it for redo.
         */
        if (toplevel && readbuf1_empty())
            set_vcount(ca.count0, ca.count1, set_prevcount[0]);

        /*
         * Find the command character in the table of commands.
         * For CTRL-W we already got nchar when looking for a count.
         */
        if (ctrl_w)
        {
            ca.nchar[0] = c;
            ca.cmdchar = Ctrl_W;
        }
        else
            ca.cmdchar = c;

        normal_end:
        {
            int idx = find__command(ca.cmdchar);
            if (idx < 0)
            {
                /* Not a known command: beep. */
                clearopbeep(oap);
                break normal_end;
            }

            if (text_locked() && (nv_cmds[idx].cmd_flags & NV_NCW) != 0)
            {
                /* This command is not allowed while editing a cmdline: beep. */
                clearopbeep(oap);
                text_locked_msg();
                break normal_end;
            }
            if ((nv_cmds[idx].cmd_flags & NV_NCW) != 0 && curbuf_locked())
                break normal_end;

            /*
            * In Visual/Select mode, a few keys are handled in a special way.
            */
            if (VIsual_active)
            {
                /* when 'keymodel' contains "stopsel" may stop Select/Visual mode */
                if (km_stopsel
                        && (nv_cmds[idx].cmd_flags & NV_STS) != 0
                        && (mod_mask & MOD_MASK_SHIFT) == 0)
                {
                    end_visual_mode();
                    redraw_curbuf_later(INVERTED);
                }

                /* Keys that work different when 'keymodel' contains "startsel". */
                if (km_startsel)
                {
                    if ((nv_cmds[idx].cmd_flags & NV_SS) != 0)
                    {
                        unshift_special(ca);
                        idx = find__command(ca.cmdchar);
                        if (idx < 0)
                        {
                            /* Just in case. */
                            clearopbeep(oap);
                            break normal_end;
                        }
                    }
                    else if ((nv_cmds[idx].cmd_flags & NV_SSS) != 0 && (mod_mask & MOD_MASK_SHIFT) != 0)
                    {
                        mod_mask &= ~MOD_MASK_SHIFT;
                    }
                }
            }

            if (curwin.w_onebuf_opt.wo_rl[0] && keyTyped && !keyStuffed && (nv_cmds[idx].cmd_flags & NV_RL) != 0)
            {
                /* Invert horizontal movements and operations.
                 * Only when typed by the user directly,
                 * not when the result of a mapping or "x" translated to "dl".
                 */
                switch (ca.cmdchar)
                {
                    case 'l':       ca.cmdchar = 'h'; break;
                    case K_RIGHT:   ca.cmdchar = K_LEFT; break;
                    case K_S_RIGHT: ca.cmdchar = K_S_LEFT; break;
                    case K_C_RIGHT: ca.cmdchar = K_C_LEFT; break;
                    case 'h':       ca.cmdchar = 'l'; break;
                    case K_LEFT:    ca.cmdchar = K_RIGHT; break;
                    case K_S_LEFT:  ca.cmdchar = K_S_RIGHT; break;
                    case K_C_LEFT:  ca.cmdchar = K_C_RIGHT; break;
                    case '>':       ca.cmdchar = '<'; break;
                    case '<':       ca.cmdchar = '>'; break;
                }
                idx = find__command(ca.cmdchar);
            }

            /*
             * Get an additional character if we need one.
             */
            if ((nv_cmds[idx].cmd_flags & NV_NCH) != 0
                    && (((nv_cmds[idx].cmd_flags & NV_NCH_NOP) == NV_NCH_NOP
                            && oap.op_type == OP_NOP)
                        || (nv_cmds[idx].cmd_flags & NV_NCH_ALW) == NV_NCH_ALW
                        || (ca.cmdchar == 'q'
                            && oap.op_type == OP_NOP
                            && !Recording
                            && !execReg)
                        || ((ca.cmdchar == 'a' || ca.cmdchar == 'i')
                            && (oap.op_type != OP_NOP || VIsual_active))))
            {
                boolean repl = false;                   /* get character for replace mode */
                boolean lit = false;                    /* get extra character literally */
                boolean langmap_active = false;         /* using :lmap mappings */

                no_mapping++;
                allow_keys++;                           /* no mapping for nchar, but allow key codes */
                /* Don't generate a CursorHold event here,
                 * most commands can't handle it, e.g. nv_replace(), nv_csearch(). */
                did_cursorhold = true;
                int[] cp;
                if (ca.cmdchar == 'g')
                {
                    /*
                     * For 'g' get the next character now, so that we can check for "gr", "g'" and "g`".
                     */
                    ca.nchar[0] = plain_vgetc();
                    need_flushbuf |= add_to_showcmd(ca.nchar[0]);
                    if (ca.nchar[0] == 'r' || ca.nchar[0] == '\'' || ca.nchar[0] == '`' || ca.nchar[0] == Ctrl_BSL)
                    {
                        cp = ca.extra_char;            /* need to get a third character */
                        if (ca.nchar[0] != 'r')
                            lit = true;                 /* get it literally */
                        else
                            repl = true;                /* get it in replace mode */
                    }
                    else
                        cp = null;                      /* no third character needed */
                }
                else
                {
                    if (ca.cmdchar == 'r')              /* get it in replace mode */
                        repl = true;
                    cp = ca.nchar;
                }

                boolean lang = (repl || (nv_cmds[idx].cmd_flags & NV_LANG) != 0);

                /*
                 * Get a second or third character.
                 */
                if (cp != null)
                {
                    if (repl)
                    {
                        State = REPLACE;                /* pretend Replace mode */
                        ui_cursor_shape();              /* show different cursor shape */
                    }
                    if (lang && curbuf.b_p_iminsert[0] == B_IMODE_LMAP)
                    {
                        /* Allow mappings defined with ":lmap". */
                        --no_mapping;
                        --allow_keys;
                        if (repl)
                            State = LREPLACE;
                        else
                            State = LANGMAP;
                        langmap_active = true;
                    }

                    cp[0] = plain_vgetc();

                    if (langmap_active)
                    {
                        /* Undo the decrement done above. */
                        no_mapping++;
                        allow_keys++;
                        State = NORMAL_BUSY;
                    }
                    State = NORMAL_BUSY;
                    need_flushbuf |= add_to_showcmd(cp[0]);

                    if (!lit)
                    {
                        /* Typing CTRL-K gets a digraph. */
                        if (cp[0] == Ctrl_K
                                && ((nv_cmds[idx].cmd_flags & NV_LANG) != 0 || cp == ca.extra_char)
                                && vim_strbyte(p_cpo[0], CPO_DIGRAPH) == null)
                        {
                            c = get_digraph(false);
                            if (0 < c)
                            {
                                cp[0] = c;
                                /* Guessing how to update showcmd here... */
                                del_from_showcmd(3);
                                need_flushbuf |= add_to_showcmd(cp[0]);
                            }
                        }
                    }

                    /*
                     * When the next character is CTRL-\ a following CTRL-N means
                     * the command is aborted and we go to Normal mode.
                     */
                    if (cp == ca.extra_char
                            && ca.nchar[0] == Ctrl_BSL
                            && (ca.extra_char[0] == Ctrl_N || ca.extra_char[0] == Ctrl_G))
                    {
                        ca.cmdchar = Ctrl_BSL;
                        ca.nchar[0] = ca.extra_char[0];
                        idx = find__command(ca.cmdchar);
                    }
                    else if ((ca.nchar[0] == 'n' || ca.nchar[0] == 'N') && ca.cmdchar == 'g')
                        ca.oap.op_type = get_op_type(cp[0], NUL);
                    else if (cp[0] == Ctrl_BSL)
                    {
                        long towait = (0 <= p_ttm[0]) ? p_ttm[0] : p_tm[0];

                        /* There is a busy wait here when typing "f<C-\>" and then
                         * something different from CTRL-N.  Can't be avoided. */
                        while ((c = vpeekc()) <= 0 && 0L < towait)
                        {
                            do_sleep(50L < towait ? 50L : towait);
                            towait -= 50L;
                        }
                        if (0 < c)
                        {
                            c = plain_vgetc();
                            if (c != Ctrl_N && c != Ctrl_G)
                                vungetc(c);
                            else
                            {
                                ca.cmdchar = Ctrl_BSL;
                                ca.nchar[0] = c;
                                idx = find__command(ca.cmdchar);
                            }
                        }
                    }

                    /* When getting a text character and the next character is a multi-byte character,
                     * it could be a composing character.  However, don't wait for it to arrive.
                     * Also, do enable mapping, because if it's put back with vungetc() it's too late
                     * to apply mapping. */
                    --no_mapping;
                    while (lang && 0 < (c = vpeekc()) && (0x100 <= c || 1 < mb_byte2len(vpeekc())))
                    {
                        c = plain_vgetc();
                        if (!utf_iscomposing(c))
                        {
                            vungetc(c);         /* it wasn't, put it back */
                            break;
                        }
                        else if (ca.ncharC1 == 0)
                            ca.ncharC1 = c;
                        else
                            ca.ncharC2 = c;
                    }
                    no_mapping++;
                }
                --no_mapping;
                --allow_keys;
            }

            /*
             * Flush the showcmd characters onto the screen so we can see them while the command
             * is being executed.  Only do this when the shown command was actually displayed,
             * otherwise this will slow down a lot when executing mappings.
             */
            if (need_flushbuf)
                out_flush();
            if (ca.cmdchar != K_IGNORE)
                did_cursorhold = false;

            State = NORMAL;

            if (ca.nchar[0] == ESC)
            {
                clearop(oap);
                if (restart_edit == 0 && goto_im())
                    restart_edit = 'a';
                break normal_end;
            }

            if (ca.cmdchar != K_IGNORE)
            {
                msg_didout = false;         /* don't scroll screen up for normal command */
                msg_col = 0;
            }

            pos_C old_pos = new pos_C();
            COPY_pos(old_pos, curwin.w_cursor);     /* remember where cursor was */

            /* When 'keymodel' contains "startsel" some keys start Select/Visual mode. */
            if (!VIsual_active && km_startsel)
            {
                if ((nv_cmds[idx].cmd_flags & NV_SS) != 0)
                {
                    start_selection();
                    unshift_special(ca);
                    idx = find__command(ca.cmdchar);
                }
                else if ((nv_cmds[idx].cmd_flags & NV_SSS) != 0 && (mod_mask & MOD_MASK_SHIFT) != 0)
                {
                    start_selection();
                    mod_mask &= ~MOD_MASK_SHIFT;
                }
            }

            /*
             * Execute the command!
             * Call the command function found in the commands table.
             */
            ca.arg = nv_cmds[idx].cmd_arg;
            nv_cmds[idx].cmd_func.nv(ca);

            /*
             * If we didn't start or finish an operator, reset oap.regname, unless we need it later.
             */
            if (!finish_op && oap.op_type == OP_NOP && (idx < 0 || (nv_cmds[idx].cmd_flags & NV_KEEPREG) == 0))
            {
                clearop(oap);

                int regname = 0;

                /* Adjust the register according to 'clipboard', so that when
                 * "unnamed" is present, it becomes '*' or '+' instead of '"'. */
                regname = adjust_clip_reg(regname);
                set_reg_var(regname);
            }

            /* Get the length of mapped chars again after typing a count,
             * second character or "z333<cr>". */
            if (0 < old_mapped_len)
                old_mapped_len = typebuf_maplen();

            /*
             * If an operation is pending, handle it...
             */
            do_pending_operator(ca, old_col, false);

            /*
             * Wait for a moment when a message is displayed that will be overwritten by the mode message.
             * In Visual mode and with "^O" in Insert mode, a short message will be
             * overwritten by the mode message.  Wait a bit, until a key is hit.
             * In Visual mode, it's more important to keep the Visual area updated
             * than keeping a message (e.g. from a /pat search).
             * Only do this if the command was typed, not from a mapping.
             * Don't wait when emsg_silent is non-zero.
             * Also wait a bit after an error message, e.g. for "^O:".
             * Don't redraw the screen, it would remove the message.
             */
            if (((p_smd[0]
                            && msg_silent == 0
                            && (restart_edit != 0
                                || (VIsual_active
                                    && old_pos.lnum == curwin.w_cursor.lnum
                                    && old_pos.col == curwin.w_cursor.col))
                            && (clear_cmdline || redraw_cmdline)
                            && (msg_didout || (msg_didany && msg_scroll))
                            && !msg_nowait
                            && keyTyped)
                        || (restart_edit != 0
                            && !VIsual_active
                            && (msg_scroll || emsg_on_display)))
                    && oap.regname == 0
                    && (ca.retval & CA_COMMAND_BUSY) == 0
                    && stuff_empty()
                    && typebuf_typed()
                    && emsg_silent == 0
                    && !did_wait_return
                    && oap.op_type == OP_NOP)
            {
                int save_State = State;

                /* Draw the cursor with the right shape here. */
                if (restart_edit != 0)
                    State = INSERT;

                /* If need to redraw, and there is a "keep_msg", redraw before the delay. */
                if (must_redraw != 0 && keep_msg != null && !emsg_on_display)
                {
                    Bytes kmsg = keep_msg;
                    keep_msg = null;
                    /* showmode() will clear "keep_msg", but we want to use it anyway */
                    update_screen(0);
                    /* now reset it, otherwise it's put in the history again */
                    keep_msg = kmsg;
                    msg_attr(kmsg, keep_msg_attr);
                }
                setcursor();
                cursor_on();
                out_flush();
                if (msg_scroll || emsg_on_display)
                    ui_delay(1000L, true);      /* wait at least one second */
                ui_delay(3000L, false);         /* wait up to three seconds */
                State = save_State;

                msg_scroll = false;
                emsg_on_display = false;
            }
        }

        /*
         * Finish up after executing a Normal mode command.
         */
        msg_nowait = false;

        /* Reset finish_op, in case it was set. */
        save_finish_op = finish_op;
        finish_op = false;
        /* Redraw the cursor with another shape,
         * if we were in Operator-pending mode or did a replace command. */
        if (save_finish_op || ca.cmdchar == 'r')
        {
            ui_cursor_shape();              /* may show different cursor shape */
        }

        if (oap.op_type == OP_NOP && oap.regname == 0 && ca.cmdchar != K_CURSORHOLD)
            clear_showcmd();

        checkpcmark();                      /* check if we moved since setting pcmark */
        ca.searchbuf = null;

        mb_adjust_pos(curbuf, curwin.w_cursor);

        if (curwin.w_onebuf_opt.wo_scb[0] && toplevel)
        {
            validate_cursor();              /* may need to update w_leftcol */
            do_check_scrollbind(true);
        }

        if (curwin.w_onebuf_opt.wo_crb[0] && toplevel)
        {
            validate_cursor();              /* may need to update w_leftcol */
            do_check_cursorbind();
        }

        /*
         * May restart edit(), if we got here with CTRL-O in Insert mode
         * (but not if still inside a mapping that started in Visual mode).
         * May switch from Visual to Select mode after CTRL-O command.
         */
        if (oap.op_type == OP_NOP
                && ((restart_edit != 0 && !VIsual_active && old_mapped_len == 0)
                    || restart_VIsual_select == 1)
                && (ca.retval & CA_COMMAND_BUSY) == 0
                && stuff_empty()
                && oap.regname == 0)
        {
            if (restart_VIsual_select == 1)
            {
                VIsual_select = true;
                showmode();
                restart_VIsual_select = 0;
            }
            if (restart_edit != 0 && !VIsual_active && old_mapped_len == 0)
                edit(restart_edit, false, 1L);
        }

        if (restart_VIsual_select == 2)
            restart_VIsual_select = 1;

        /* Save count before an operator for next time. */
        opcount = ca.opcount;
    }

    /*
     * Set v:count and v:count1 according to "cap".
     * Set v:prevcount only when "set_prevcount" is true.
     */
    /*private*/ static void set_vcount_ca(cmdarg_C cap, boolean[] set_prevcount)
    {
        long count = cap.count0;

        /* multiply with cap.opcount the same way as above */
        if (cap.opcount != 0)
            count = cap.opcount * (count == 0 ? 1 : count);
        set_vcount(count, (count == 0) ? 1 : count, set_prevcount[0]);
        set_prevcount[0] = false;     /* only set v:prevcount once */
    }

    /* The visual area is remembered for redo. */
    /*private*/ static int      redo_VIsual_mode = NUL;     /* 'v', 'V', or Ctrl-V */
    /*private*/ static long     redo_VIsual_line_count;     /* number of lines */
    /*private*/ static int      redo_VIsual_vcol;           /* number of cols or end column */
    /*private*/ static long     redo_VIsual_count;          /* count for Visual operator */

    /*
     * Handle an operator after visual mode or when the movement is finished.
     */
    /*private*/ static void do_pending_operator(cmdarg_C cap, int old_col, boolean gui_yank)
    {
        oparg_C oap = cap.oap;

        boolean lbr_saved = curwin.w_onebuf_opt.wo_lbr[0];
        boolean include_line_break = false;

        /*
         * Yank the visual area into the GUI selection register
         * before we operate on it and lose it forever.
         * Don't do it if a specific register was specified, so that ""x"*P works.
         * This could call do_pending_operator() recursively, but that's OK,
         * because gui_yank will be true for the nested call.
         */
        if ((clip_star.available || clip_plus.available)
                && oap.op_type != OP_NOP
                && !gui_yank
                && VIsual_active
                && !redo_VIsual_busy
                && oap.regname == 0)
            clip_auto_select();

        pos_C old_cursor = new pos_C();
        COPY_pos(old_cursor, curwin.w_cursor);

        /*
         * If an operation is pending, handle it...
         */
        if ((finish_op || VIsual_active) && oap.op_type != OP_NOP)
        {
            /* Avoid a problem with unwanted linebreaks in block mode. */
            curwin.w_onebuf_opt.wo_lbr[0] = false;
            oap.is_VIsual = VIsual_active;
            if (oap.motion_force == 'V')
                oap.motion_type = MLINE;
            else if (oap.motion_force == 'v')
            {
                /* If the motion was linewise, "inclusive" will not have been set.
                 * Use "exclusive" to be consistent.  Makes "dvj" work nice. */
                if (oap.motion_type == MLINE)
                    oap.inclusive = false;
                /* If the motion already was characterwise, toggle "inclusive". */
                else if (oap.motion_type == MCHAR)
                    oap.inclusive = !oap.inclusive;
                oap.motion_type = MCHAR;
            }
            else if (oap.motion_force == Ctrl_V)
            {
                /* Change line- or characterwise motion into Visual block mode. */
                VIsual_active = true;
                COPY_pos(VIsual, oap.op_start);
                VIsual_mode = Ctrl_V;
                VIsual_select = false;
                VIsual_reselect = false;
            }

            /* Only redo yank when 'y' flag is in 'cpoptions'. */
            if ((vim_strbyte(p_cpo[0], CPO_YANK) != null || oap.op_type != OP_YANK)
                    && ((!VIsual_active || oap.motion_force != 0)
                        /* Also redo Operator-pending Visual mode mappings. */
                        || (VIsual_active && cap.cmdchar == ':' && oap.op_type != OP_COLON))
                    && cap.cmdchar != 'D')
            {
                prep_redo(oap.regname, cap.count0,
                        get_op_char(oap.op_type), get_extra_op_char(oap.op_type),
                        oap.motion_force, cap.cmdchar, cap.nchar[0]);
                if (cap.cmdchar == '/' || cap.cmdchar == '?') /* was a search */
                {
                    /* If 'cpoptions' does not contain 'r',
                     * insert the search pattern to really repeat the same command.
                     */
                    if (vim_strbyte(p_cpo[0], CPO_REDO) == null)
                        appendToRedobuffLit(cap.searchbuf, -1);
                    appendToRedobuff(NL_STR);
                }
                else if (cap.cmdchar == ':')
                {
                    /* do_cmdline() has stored the first typed line in "repeat_cmdline".
                     * When several lines are typed repeating won't be possible.
                     */
                    if (repeat_cmdline == null)
                        resetRedobuff();
                    else
                    {
                        appendToRedobuffLit(repeat_cmdline, -1);
                        appendToRedobuff(NL_STR);
                        repeat_cmdline = null;
                    }
                }
            }

            if (redo_VIsual_busy)
            {
                /* Redo of an operation on a Visual area.
                 * Use the same size from redo_VIsual_line_count and redo_VIsual_vcol.
                 */
                COPY_pos(oap.op_start, curwin.w_cursor);
                curwin.w_cursor.lnum += redo_VIsual_line_count - 1;
                if (curwin.w_cursor.lnum > curbuf.b_ml.ml_line_count)
                    curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
                VIsual_mode = redo_VIsual_mode;
                if (redo_VIsual_vcol == MAXCOL || VIsual_mode == 'v')
                {
                    if (VIsual_mode == 'v')
                    {
                        if (redo_VIsual_line_count <= 1)
                        {
                            validate_virtcol();
                            curwin.w_curswant = curwin.w_virtcol + redo_VIsual_vcol - 1;
                        }
                        else
                            curwin.w_curswant = redo_VIsual_vcol;
                    }
                    else
                    {
                        curwin.w_curswant = MAXCOL;
                    }
                    coladvance(curwin.w_curswant);
                }
                cap.count0 = redo_VIsual_count;
                if (redo_VIsual_count != 0)
                    cap.count1 = redo_VIsual_count;
                else
                    cap.count1 = 1;
            }
            else if (VIsual_active)
            {
                if (!gui_yank)
                {
                    /* Save the current VIsual area for '< and '> marks, and "gv". */
                    COPY_pos(curbuf.b_visual.vi_start, VIsual);
                    COPY_pos(curbuf.b_visual.vi_end, curwin.w_cursor);
                    curbuf.b_visual.vi_mode = VIsual_mode;
                    if (VIsual_mode_orig != NUL)
                    {
                        curbuf.b_visual.vi_mode = VIsual_mode_orig;
                        VIsual_mode_orig = NUL;
                    }
                    curbuf.b_visual.vi_curswant = curwin.w_curswant;
                    curbuf.b_visual_mode_eval = VIsual_mode;
                }

                /* In Select mode,
                 * a linewise selection is operated upon like a characterwise selection. */
                if (VIsual_select && VIsual_mode == 'V')
                {
                    if (ltpos(VIsual, curwin.w_cursor))
                    {
                        VIsual.col = 0;
                        curwin.w_cursor.col = strlen(ml_get(curwin.w_cursor.lnum));
                    }
                    else
                    {
                        curwin.w_cursor.col = 0;
                        VIsual.col = strlen(ml_get(VIsual.lnum));
                    }
                    VIsual_mode = 'v';
                }
                /* If 'selection' is "exclusive", backup one character for charwise selections. */
                else if (VIsual_mode == 'v')
                {
                    include_line_break = unadjust_for_sel();
                }

                COPY_pos(oap.op_start, VIsual);
                if (VIsual_mode == 'V')
                    oap.op_start.col = 0;
            }

            /*
             * Set oap.op_start to the first position of the operated text, oap.op_end
             * to the end of the operated text.  w_cursor is equal to oap.op_start.
             */
            if (ltpos(oap.op_start, curwin.w_cursor))
            {
                COPY_pos(oap.op_end, curwin.w_cursor);
                COPY_pos(curwin.w_cursor, oap.op_start);

                /* w_virtcol may have been updated; if the cursor goes back to its previous
                 * position, w_virtcol becomes invalid and isn't updated automatically. */
                curwin.w_valid &= ~VALID_VIRTCOL;
            }
            else
            {
                COPY_pos(oap.op_end, oap.op_start);
                COPY_pos(oap.op_start, curwin.w_cursor);
            }

            oap.line_count = oap.op_end.lnum - oap.op_start.lnum + 1;

            /* Set "virtual_op" before resetting VIsual_active. */
            virtual_op = virtual_active() ? TRUE : FALSE;

            if (VIsual_active || redo_VIsual_busy)
            {
                if (VIsual_mode == Ctrl_V)  /* block mode */
                {
                    oap.block_mode = true;

                    int[] start = { oap.start_vcol };
                    int[] end = { oap.end_vcol };
                    getvvcol(curwin, oap.op_start, start, null, end);
                    oap.start_vcol = start[0];
                    oap.end_vcol = end[0];

                    if (!redo_VIsual_busy)
                    {
                        getvvcol(curwin, oap.op_end, start, null, end);

                        if (start[0] < oap.start_vcol)
                            oap.start_vcol = start[0];
                        if (oap.end_vcol < end[0])
                        {
                            if (p_sel[0].at(0) == (byte)'e' && 1 <= start[0] && oap.end_vcol <= start[0] - 1)
                                oap.end_vcol = start[0] - 1;
                            else
                                oap.end_vcol = end[0];
                        }
                    }

                    /* if '$' was used, get oap.end_vcol from longest line */
                    if (curwin.w_curswant == MAXCOL)
                    {
                        curwin.w_cursor.col = MAXCOL;
                        oap.end_vcol = 0;
                        for (curwin.w_cursor.lnum = oap.op_start.lnum;
                             curwin.w_cursor.lnum <= oap.op_end.lnum;
                             curwin.w_cursor.lnum++)
                        {
                            getvvcol(curwin, curwin.w_cursor, null, null, end);
                            if (oap.end_vcol < end[0])
                                oap.end_vcol = end[0];
                        }
                    }
                    else if (redo_VIsual_busy)
                        oap.end_vcol = oap.start_vcol + redo_VIsual_vcol - 1;

                    /*
                     * Correct oap.op_end.col and oap.op_start.col to be the
                     * upper-left and lower-right corner of the block area.
                     *
                     * (Actually, this does convert column positions into character positions.)
                     */
                    curwin.w_cursor.lnum = oap.op_end.lnum;
                    coladvance(oap.end_vcol);
                    COPY_pos(oap.op_end, curwin.w_cursor);

                    COPY_pos(curwin.w_cursor, oap.op_start);
                    coladvance(oap.start_vcol);
                    COPY_pos(oap.op_start, curwin.w_cursor);
                }

                if (!redo_VIsual_busy && !gui_yank)
                {
                    /*
                     * Prepare to reselect and redo Visual:
                     * this is based on the size of the Visual text
                     */
                    resel_VIsual_mode = VIsual_mode;
                    if (curwin.w_curswant == MAXCOL)
                        resel_VIsual_vcol = MAXCOL;
                    else
                    {
                        if (VIsual_mode != Ctrl_V)
                        {
                            int[] __ = { oap.end_vcol };
                            getvvcol(curwin, oap.op_end, null, null, __);
                            oap.end_vcol = __[0];
                        }
                        if (VIsual_mode == Ctrl_V || oap.line_count <= 1)
                        {
                            if (VIsual_mode != Ctrl_V)
                            {
                                int[] __ = { oap.start_vcol };
                                getvvcol(curwin, oap.op_start, __, null, null);
                                oap.start_vcol = __[0];
                            }
                            resel_VIsual_vcol = oap.end_vcol - oap.start_vcol + 1;
                        }
                        else
                            resel_VIsual_vcol = oap.end_vcol;
                    }
                    resel_VIsual_line_count = oap.line_count;
                }

                /* can't redo yank (unless 'y' is in 'cpoptions') and ":" */
                if ((vim_strbyte(p_cpo[0], CPO_YANK) != null || oap.op_type != OP_YANK)
                        && oap.op_type != OP_COLON
                        && oap.motion_force == NUL)
                {
                    /* Prepare for redoing.  Only use the nchar field for "r",
                     * otherwise it might be the second char of the operator.
                     */
                    if (cap.cmdchar == 'g' && (cap.nchar[0] == 'n' || cap.nchar[0] == 'N'))
                        prep_redo(oap.regname, cap.count0,
                                get_op_char(oap.op_type), get_extra_op_char(oap.op_type),
                                oap.motion_force, cap.cmdchar, cap.nchar[0]);
                    else if (cap.cmdchar != ':')
                        prep_redo(oap.regname, 0L, NUL, 'v',
                                            get_op_char(oap.op_type),
                                            get_extra_op_char(oap.op_type),
                                            oap.op_type == OP_REPLACE ? cap.nchar[0] : NUL);
                    if (!redo_VIsual_busy)
                    {
                        redo_VIsual_mode = resel_VIsual_mode;
                        redo_VIsual_vcol = resel_VIsual_vcol;
                        redo_VIsual_line_count = resel_VIsual_line_count;
                        redo_VIsual_count = cap.count0;
                    }
                }

                /*
                 * oap.inclusive defaults to true.
                 * If oap.op_end is on a NUL (empty line) oap.inclusive becomes false.
                 * This makes "d}P" and "v}dP" work the same.
                 */
                if (oap.motion_force == NUL || oap.motion_type == MLINE)
                    oap.inclusive = true;
                if (VIsual_mode == 'V')
                    oap.motion_type = MLINE;
                else
                {
                    oap.motion_type = MCHAR;
                    if (VIsual_mode != Ctrl_V && ml_get_pos(oap.op_end).at(0) == NUL
                            && (include_line_break || virtual_op == FALSE))
                    {
                        oap.inclusive = false;
                        /* Try to include the newline,
                         * unless it's an operator that works on lines only. */
                        if (p_sel[0].at(0) != (byte)'o' && !op_on_lines(oap.op_type))
                        {
                            if (oap.op_end.lnum < curbuf.b_ml.ml_line_count)
                            {
                                oap.op_end.lnum++;
                                oap.op_end.col = 0;
                                oap.op_end.coladd = 0;
                                oap.line_count++;
                            }
                            else
                            {
                                /* Cannot move below the last line, make the op inclusive
                                 * to tell the operation to include the line break. */
                                oap.inclusive = true;
                            }
                        }
                    }
                }

                redo_VIsual_busy = false;

                /*
                 * Switch Visual off now, so screen updating does
                 * not show inverted text when the screen is redrawn.
                 * With OP_YANK and sometimes with OP_COLON and OP_FILTER there is
                 * no screen redraw, so it is done here to remove the inverted part.
                 */
                if (!gui_yank)
                {
                    VIsual_active = false;
                    setmouse();
                    mouse_dragging = 0;
                    if (mode_displayed)
                        clear_cmdline = true;   /* unshow visual mode later */
                    else
                        clear_showcmd();
                    if ((oap.op_type == OP_YANK
                                || oap.op_type == OP_COLON
                                || oap.op_type == OP_FUNCTION
                                || oap.op_type == OP_FILTER)
                            && oap.motion_force == NUL)
                    {
                        /* make sure redrawing is correct */
                        curwin.w_onebuf_opt.wo_lbr[0] = lbr_saved;
                        redraw_curbuf_later(INVERTED);
                    }
                }
            }

            /* Include the trailing byte of a multi-byte char. */
            if (oap.inclusive)
            {
                int l = us_ptr2len_cc(ml_get_pos(oap.op_end));
                if (1 < l)
                    oap.op_end.col += l - 1;
            }
            curwin.w_set_curswant = true;

            /*
             * oap.empty is set when start and end are the same.
             * The inclusive flag affects this too, unless yanking and the end is on a NUL.
             */
            oap.empty = (oap.motion_type == MCHAR
                        && (!oap.inclusive || (oap.op_type == OP_YANK && gchar_pos(oap.op_end) == NUL))
                        && eqpos(oap.op_start, oap.op_end)
                        && !(virtual_op != FALSE && oap.op_start.coladd != oap.op_end.coladd));
            /*
             * For delete, change and yank, it's an error to operate on an
             * empty region, when 'E' included in 'cpoptions' (Vi compatible).
             */
            boolean empty_region_error = (oap.empty && vim_strbyte(p_cpo[0], CPO_EMPTYREGION) != null);

            /* Force a redraw when operating on an empty Visual region,
             * when 'modifiable' is off or creating a fold. */
            if (oap.is_VIsual && (oap.empty || !curbuf.b_p_ma[0]))
            {
                curwin.w_onebuf_opt.wo_lbr[0] = lbr_saved;
                redraw_curbuf_later(INVERTED);
            }

            /*
             * If the end of an operator is in column one while oap.motion_type
             * is MCHAR and oap.inclusive is false, we put op_end after the last
             * character in the previous line.  If op_start is on or before the
             * first non-blank in the line, the operator becomes linewise
             * (strange, but that's the way vi does it).
             */
            if (       oap.motion_type == MCHAR
                    && oap.inclusive == false
                    && (cap.retval & CA_NO_ADJ_OP_END) == 0
                    && oap.op_end.col == 0
                    && (!oap.is_VIsual || p_sel[0].at(0) == (byte)'o')
                    && !oap.block_mode
                    && 1 < oap.line_count)
            {
                oap.end_adjusted = true;    /* remember that we did this */
                --oap.line_count;
                --oap.op_end.lnum;
                if (inindent(0))
                    oap.motion_type = MLINE;
                else
                {
                    oap.op_end.col = strlen(ml_get(oap.op_end.lnum));
                    if (0 < oap.op_end.col)
                    {
                        --oap.op_end.col;
                        oap.inclusive = true;
                    }
                }
            }
            else
                oap.end_adjusted = false;

            switch (oap.op_type)
            {
                case OP_LSHIFT:
                case OP_RSHIFT:
                    op_shift(oap, true, oap.is_VIsual ? (int)cap.count1 : 1);
                    auto_format(false, true);
                    break;

                case OP_JOIN_NS:
                case OP_JOIN:
                    if (oap.line_count < 2)
                        oap.line_count = 2;
                    if (curbuf.b_ml.ml_line_count < curwin.w_cursor.lnum + oap.line_count - 1)
                        beep_flush();
                    else
                    {
                        do_join((int)oap.line_count, oap.op_type == OP_JOIN, true, true, true);
                        auto_format(false, true);
                    }
                    break;

                case OP_DELETE:
                    VIsual_reselect = false;        /* don't reselect now */
                    if (empty_region_error)
                    {
                        vim_beep();
                        cancelRedo();
                    }
                    else
                    {
                        op_delete(oap);
                        if (oap.motion_type == MLINE && has_format_option(FO_AUTO))
                            u_save_cursor();        /* cursor line wasn't saved yet */
                        auto_format(false, true);
                    }
                    break;

                case OP_YANK:
                    if (empty_region_error)
                    {
                        if (!gui_yank)
                        {
                            vim_beep();
                            cancelRedo();
                        }
                    }
                    else
                    {
                        curwin.w_onebuf_opt.wo_lbr[0] = lbr_saved;
                        op_yank(oap, false, !gui_yank);
                    }
                    check_cursor_col();
                    break;

                case OP_CHANGE:
                    VIsual_reselect = false;        /* don't reselect now */
                    if (empty_region_error)
                    {
                        vim_beep();
                        cancelRedo();
                    }
                    else
                    {
                        /* This is a new edit command, not a restart.
                         * Need to remember it to make 'insertmode' work with mappings for Visual mode.
                         * But do this only once and not when typed and 'insertmode' isn't set. */
                        int restart_edit_save;
                        if (p_im[0] || !keyTyped)
                            restart_edit_save = restart_edit;
                        else
                            restart_edit_save = 0;
                        restart_edit = 0;
                        /* Restore linebreak, so that when the user edits it looks as before. */
                        curwin.w_onebuf_opt.wo_lbr[0] = lbr_saved;
                        /* Reset finish_op now, don't want it set inside edit(). */
                        finish_op = false;
                        if (op_change(oap))         /* will call edit() */
                            cap.retval |= CA_COMMAND_BUSY;
                        if (restart_edit == 0)
                            restart_edit = restart_edit_save;
                    }
                    break;

                case OP_FILTER:
                    if (vim_strbyte(p_cpo[0], CPO_FILTER) != null)
                        appendToRedobuff(u8("!\r"));    /* use any last used !cmd */
                    else
                        bangredo = true;            /* do_bang() will put cmd in redo buffer */
                    /* FALLTHROUGH */

                case OP_INDENT:
                case OP_COLON:
                    /*
                     * If 'equalprg' is empty, do the indenting internally.
                     */
                    if (oap.op_type == OP_INDENT && get_equalprg().at(0) == NUL)
                    {
                        if (curbuf.b_p_lisp[0])
                        {
                            op_reindent(oap, get_lisp_indent);
                            break;
                        }
                        op_reindent(oap, (curbuf.b_p_inde[0].at(0) != NUL) ? get_expr_indent : get_c_indent);
                        break;
                    }

                    op_colon(oap);
                    break;

                case OP_TILDE:
                case OP_UPPER:
                case OP_LOWER:
                case OP_ROT13:
                    if (empty_region_error)
                    {
                        vim_beep();
                        cancelRedo();
                    }
                    else
                        op_tilde(oap);
                    check_cursor_col();
                    break;

                case OP_FORMAT:
                    if (curbuf.b_p_fex[0].at(0) != NUL)
                        op_formatexpr(oap);             /* use expression */
                    else if (p_fp[0].at(0) != NUL)
                        op_colon(oap);                  /* use external command */
                    else
                        op_format(oap, false);          /* use internal function */
                    break;

                case OP_FORMAT2:
                    op_format(oap, true);               /* use internal function */
                    break;

                case OP_FUNCTION:
                    op_function(oap);                   /* call 'operatorfunc' */
                    break;

                case OP_INSERT:
                case OP_APPEND:
                    VIsual_reselect = false;            /* don't reselect now */
                    if (empty_region_error)
                    {
                        vim_beep();
                        cancelRedo();
                    }
                    else
                    {
                        /* This is a new edit command, not a restart.
                         * Need to remember it to make 'insertmode' work with mappings for Visual mode.
                         * But do this only once. */
                        int restart_edit_save = restart_edit;
                        restart_edit = 0;
                        /* Restore linebreak, so that when the user edits it looks as before. */
                        curwin.w_onebuf_opt.wo_lbr[0] = lbr_saved;
                        op_insert(oap, cap.count1);
                        /* Reset linebreak, so that formatting works correctly. */
                        curwin.w_onebuf_opt.wo_lbr[0] = false;

                        /* TODO: when inserting in several lines, should format all the lines. */
                        auto_format(false, true);

                        if (restart_edit == 0)
                            restart_edit = restart_edit_save;
                    }
                    break;

                case OP_REPLACE:
                    VIsual_reselect = false;    /* don't reselect now */
                    if (empty_region_error)
                    {
                        vim_beep();
                        cancelRedo();
                    }
                    else
                    {
                        /* Restore linebreak, so that when the user edits it looks as before. */
                        curwin.w_onebuf_opt.wo_lbr[0] = lbr_saved;
                        op_replace(oap, cap.nchar[0]);
                    }
                    break;

                default:
                    clearopbeep(oap);
                    break;
            }

            virtual_op = MAYBE;

            if (!gui_yank)
            {
                /*
                 * if 'sol' not set, go back to old column for some commands
                 */
                if (!p_sol[0] && oap.motion_type == MLINE && !oap.end_adjusted
                        && (oap.op_type == OP_LSHIFT
                         || oap.op_type == OP_RSHIFT
                         || oap.op_type == OP_DELETE))
                {
                    curwin.w_onebuf_opt.wo_lbr[0] = false;
                    coladvance(curwin.w_curswant = old_col);
                }
            }
            else
            {
                COPY_pos(curwin.w_cursor, old_cursor);
            }
            oap.block_mode = false;
            clearop(oap);
        }

        curwin.w_onebuf_opt.wo_lbr[0] = lbr_saved;
    }

    /*
     * Handle indent and format operators and visual mode ":".
     */
    /*private*/ static void op_colon(oparg_C oap)
    {
        stuffcharReadbuff(':');
        if (oap.is_VIsual)
            stuffReadbuff(u8("'<,'>"));
        else
        {
            /*
             * Make the range look nice, so it can be repeated.
             */
            if (oap.op_start.lnum == curwin.w_cursor.lnum)
                stuffcharReadbuff('.');
            else
                stuffnumReadbuff(oap.op_start.lnum);
            if (oap.op_end.lnum != oap.op_start.lnum)
            {
                stuffcharReadbuff(',');
                if (oap.op_end.lnum == curwin.w_cursor.lnum)
                    stuffcharReadbuff('.');
                else if (oap.op_end.lnum == curbuf.b_ml.ml_line_count)
                    stuffcharReadbuff('$');
                else if (oap.op_start.lnum == curwin.w_cursor.lnum)
                {
                    stuffReadbuff(u8(".+"));
                    stuffnumReadbuff(oap.line_count - 1);
                }
                else
                    stuffnumReadbuff(oap.op_end.lnum);
            }
        }
        if (oap.op_type != OP_COLON)
            stuffReadbuff(u8("!"));
        if (oap.op_type == OP_INDENT)
        {
            stuffReadbuff(get_equalprg());
            stuffReadbuff(u8("\n"));
        }
        else if (oap.op_type == OP_FORMAT)
        {
            if (p_fp[0].at(0) == NUL)
                stuffReadbuff(u8("fmt"));
            else
                stuffReadbuff(p_fp[0]);
            stuffReadbuff(u8("\n']"));
        }

        /*
         * do_cmdline() does the rest
         */
    }

    /*
     * Handle the "g@" operator: call 'operatorfunc'.
     */
    /*private*/ static void op_function(oparg_C oap)
    {
        Bytes[] argv = new Bytes[1];
        /*MAYBEAN*/int save_virtual_op = virtual_op;

        if (p_opfunc[0].at(0) == NUL)
            emsg(u8("E774: 'operatorfunc' is empty"));
        else
        {
            /* Set '[ and '] marks to text to be operated on. */
            COPY_pos(curbuf.b_op_start, oap.op_start);
            COPY_pos(curbuf.b_op_end, oap.op_end);
            if (oap.motion_type != MLINE && !oap.inclusive)
                /* Exclude the end position. */
                decl(curbuf.b_op_end);

            if (oap.block_mode)
                argv[0] = u8("block");
            else if (oap.motion_type == MLINE)
                argv[0] = u8("line");
            else
                argv[0] = u8("char");

            /* Reset virtual_op so that 'virtualedit' can be changed in the function. */
            virtual_op = MAYBE;

            call_func_retnr(p_opfunc[0], 1, argv, false);

            virtual_op = save_virtual_op;
        }
    }

    /*private*/ static boolean do_always;       /* ignore 'mouse' setting next time */
    /*private*/ static boolean got_click;       /* got a click some time back */
    /*private*/ static boolean in_tab_line;     /* mouse clicked in tab line */
    /*private*/ static pos_C orig_cursor = new pos_C();

    /*
     * Do the appropriate action for the current mouse click in the current mode.
     * Not used for Command-line mode.
     *
     * Normal Mode:
     * event         modi-  position      visual       change   action
     *               fier   cursor                     window
     * left press     -     yes         end             yes
     * left press     C     yes         end             yes     "^]" (2)
     * left press     S     yes         end             yes     "*" (2)
     * left drag      -     yes     start if moved      no
     * left relse     -     yes     start if moved      no
     * middle press   -     yes      if not active      no      put register
     * middle press   -     yes      if active          no      yank and put
     * right press    -     yes     start or extend     yes
     * right press    S     yes     no change           yes     "#" (2)
     * right drag     -     yes     extend              no
     * right relse    -     yes     extend              no
     *
     * Insert or Replace Mode:
     * event         modi-  position      visual       change   action
     *               fier   cursor                     window
     * left press     -     yes     (cannot be active)  yes
     * left press     C     yes     (cannot be active)  yes     "CTRL-O^]" (2)
     * left press     S     yes     (cannot be active)  yes     "CTRL-O*" (2)
     * left drag      -     yes     start or extend (1) no      CTRL-O (1)
     * left relse     -     yes     start or extend (1) no      CTRL-O (1)
     * middle press   -     no      (cannot be active)  no      put register
     * right press    -     yes     start or extend     yes     CTRL-O
     * right press    S     yes     (cannot be active)  yes     "CTRL-O#" (2)
     *
     * (1) only if mouse pointer moved since press
     * (2) only if click is in same buffer
     *
     * Return true if start_arrow() should be called for edit mode.
     */
    /*private*/ static boolean do_mouse(oparg_C oap, int c, int dir, int count, int fixindent)
        /* oap: operator argument, can be null */
        /* c: K_LEFTMOUSE, etc */
        /* dir: Direction to 'put' if necessary */
        /* fixindent: PUT_FIXINDENT if fixing indent necessary */
    {
        boolean[] is_click = new boolean[1];    /* if false it's a drag or release event */
        boolean[] is_drag = new boolean[1];     /* if true it's a drag event */
        int jump_flags = 0;                     /* flags for jump_to_mouse() */

        window_C old_curwin = curwin;

        boolean old_active = VIsual_active;
        int old_mode = VIsual_mode;

        /*
         * When GUI is active, always recognize mouse events, otherwise:
         * - Ignore mouse event in normal mode if 'mouse' doesn't include 'n'.
         * - Ignore mouse event in visual mode if 'mouse' doesn't include 'v'.
         * - For command line and insert mode 'mouse' is checked before calling do_mouse().
         */
        if (do_always)
            do_always = false;
        else
        {
            if (VIsual_active)
            {
                if (!mouse_has(MOUSE_VISUAL))
                    return false;
            }
            else if (State == NORMAL && !mouse_has(MOUSE_NORMAL))
                return false;
        }

        int which_button;                   /* MOUSE_LEFT, _MIDDLE or _RIGHT */

        for ( ; ; )
        {
            which_button = get_mouse_button(KEY2TERMCAP1(c), is_click, is_drag);
            if (is_drag[0])
            {
                /* If the next character is the same mouse event then use that one.
                 * Speeds up dragging the status line. */
                if (vpeekc() != NUL)
                {
                    int save_mouse_row = mouse_row;
                    int save_mouse_col = mouse_col;

                    /* Need to get the character, peeking doesn't get the actual one. */
                    int nc = safe_vgetc();
                    if (c == nc)
                        continue;
                    vungetc(nc);
                    mouse_row = save_mouse_row;
                    mouse_col = save_mouse_col;
                }
            }
            break;
        }

        /*
         * Ignore drag and release events if we didn't get a click.
         */
        if (is_click[0])
            got_click = true;
        else
        {
            if (!got_click)                 /* didn't get click, ignore */
                return false;
            if (!is_drag[0])                   /* release, reset got_click */
            {
                got_click = false;
                if (in_tab_line)
                {
                    in_tab_line = false;
                    return false;
                }
            }
        }

        /*
         * CTRL right mouse button does CTRL-T
         */
        if (is_click[0] && (mod_mask & MOD_MASK_CTRL) != 0 && which_button == MOUSE_RIGHT)
        {
            if ((State & INSERT) != 0)
                stuffcharReadbuff(Ctrl_O);
            if (1 < count)
                stuffnumReadbuff(count);
            stuffcharReadbuff(Ctrl_T);
            got_click = false;              /* ignore drag&release now */
            return false;
        }

        /*
         * CTRL only works with left mouse button
         */
        if ((mod_mask & MOD_MASK_CTRL) != 0 && which_button != MOUSE_LEFT)
            return false;

        /*
         * When a modifier is down, ignore drag and release events, as well as
         * multiple clicks and the middle mouse button.
         * Accept shift-leftmouse drags when 'mousemodel' is "popup.*".
         */
        if ((mod_mask & (MOD_MASK_SHIFT | MOD_MASK_CTRL | MOD_MASK_ALT | MOD_MASK_META)) != 0
                && (!is_click[0]
                    || (mod_mask & MOD_MASK_MULTI_CLICK) != 0
                    || which_button == MOUSE_MIDDLE)
                && !((mod_mask & (MOD_MASK_SHIFT | MOD_MASK_ALT)) != 0
                    && mouse_model_popup()
                    && which_button == MOUSE_LEFT)
                && !((mod_mask & MOD_MASK_ALT) != 0
                    && !mouse_model_popup()
                    && which_button == MOUSE_RIGHT)
                )
            return false;

        /*
         * If the button press was used as the movement command for an operator
         * (eg "d<MOUSE>"), or it is the middle button that is held down, ignore
         * drag/release events.
         */
        if (!is_click[0] && which_button == MOUSE_MIDDLE)
            return false;

        int regname;
        if (oap != null)
            regname = oap.regname;
        else
            regname = 0;

        /*
         * Middle mouse button does a 'put' of the selected text
         */
        if (which_button == MOUSE_MIDDLE)
        {
            if (State == NORMAL)
            {
                /*
                 * If an operator was pending, we don't know what the user wanted
                 * to do.  Go back to normal mode: Clear the operator and beep().
                 */
                if (oap != null && oap.op_type != OP_NOP)
                {
                    clearopbeep(oap);
                    return false;
                }

                /*
                 * If visual was active, yank the highlighted text and put it
                 * before the mouse pointer position.
                 * In Select mode replace the highlighted text with the clipboard.
                 */
                if (VIsual_active)
                {
                    if (VIsual_select)
                    {
                        stuffcharReadbuff(Ctrl_G);
                        stuffReadbuff(u8("\"+p"));
                    }
                    else
                    {
                        stuffcharReadbuff('y');
                        stuffcharReadbuff(K_MIDDLEMOUSE);
                    }
                    do_always = true;       /* ignore 'mouse' setting next time */
                    return false;
                }
                /*
                 * the rest is below jump_to_mouse()
                 */
            }

            else if ((State & INSERT) == 0)
                return false;

            /*
             * Middle click in insert mode doesn't move the mouse, just insert the
             * contents of a register.  '.' register is special, can't insert that with do_put().
             * Also paste at the cursor if the current mode isn't in 'mouse'.
             */
            if ((State & INSERT) != 0 || !mouse_has(MOUSE_NORMAL))
            {
                if (regname == '.')
                    insert_reg(regname, true);
                else
                {
                    if (clip_star.available && regname == 0)
                        regname = '*';
                    if ((State & REPLACE_FLAG) != 0 && !yank_register_mline(regname))
                        insert_reg(regname, true);
                    else
                    {
                        do_put(regname, BACKWARD, 1, fixindent | PUT_CURSEND);

                        /* Repeat it with CTRL-R CTRL-O r or CTRL-R CTRL-P r. */
                        appendCharToRedobuff(Ctrl_R);
                        appendCharToRedobuff((fixindent != 0) ? Ctrl_P : Ctrl_O);
                        appendCharToRedobuff((regname == 0) ? '"' : regname);
                    }
                }
                return false;
            }
        }

        /* When dragging or button-up stay in the same window. */
        if (!is_click[0])
            jump_flags |= MOUSE_FOCUS | MOUSE_DID_MOVE;

        /* Check for clicking in the tab page line. */
        if (mouse_row == 0 && 0 < firstwin.w_winrow)
        {
            if (is_drag[0])
            {
                if (in_tab_line)
                {
                    int c1 = tabPageIdxs[mouse_col];
                    tabpage_move(c1 <= 0 ? 9999 : c1 - 1);
                }
                return false;
            }

            /* click in a tab selects that tab page */
            if (is_click[0] && cmdwin_type == 0 && mouse_col < (int)Columns[0])
            {
                in_tab_line = true;
                int c1 = tabPageIdxs[mouse_col];
                if (0 <= c1)
                {
                    if ((mod_mask & MOD_MASK_MULTI_CLICK) == MOD_MASK_2CLICK)
                    {
                        /* double click opens new page */
                        end_visual_mode();
                        tabpage_new();
                        tabpage_move(c1 == 0 ? 9999 : c1 - 1);
                    }
                    else
                    {
                        /* Go to specified tab page, or next one if not clicking on a label. */
                        goto_tabpage(c1);

                        /* It's like clicking on the status line of a window. */
                        if (curwin != old_curwin)
                            end_visual_mode();
                    }
                }
                else if (c1 < 0)
                {
                    tabpage_C tp;

                    /* Close the current or specified tab page. */
                    if (c1 == -999)
                        tp = curtab;
                    else
                        tp = find_tabpage(-c1);
                    if (tp == curtab)
                    {
                        if (first_tabpage.tp_next != null)
                            tabpage_close(false);
                    }
                    else if (tp != null)
                        tabpage_close_other(tp, false);
                }
            }
            return true;
        }
        else if (is_drag[0] && in_tab_line)
        {
            int c1 = tabPageIdxs[mouse_col];
            tabpage_move(c1 <= 0 ? 9999 : c1 - 1);
            return false;
        }

        /*
         * When 'mousemodel' is "popup" or "popup_setpos", translate mouse events:
         * right button up   -> pop-up menu
         * shift-left button -> right button
         * alt-left button   -> alt-right button
         */
        if (mouse_model_popup())
        {
            if (which_button == MOUSE_RIGHT && (mod_mask & (MOD_MASK_SHIFT | MOD_MASK_CTRL)) == 0)
            {
                /*
                 * NOTE: Ignore right button down and drag mouse events.
                 * Windows only shows the popup menu on the button up event.
                 */
                return false;
            }
            if (which_button == MOUSE_LEFT && (mod_mask & (MOD_MASK_SHIFT|MOD_MASK_ALT)) != 0)
            {
                which_button = MOUSE_RIGHT;
                mod_mask &= ~MOD_MASK_SHIFT;
            }
        }

        pos_C start_visual = new pos_C();
        start_visual.lnum = 0;

        pos_C end_visual = new pos_C();

        if ((State & (NORMAL | INSERT)) != 0 && (mod_mask & (MOD_MASK_SHIFT | MOD_MASK_CTRL)) == 0)
        {
            if (which_button == MOUSE_LEFT)
            {
                if (is_click[0])
                {
                    /* stop Visual mode for a left click in a window, but not when on a status line */
                    if (VIsual_active)
                        jump_flags |= MOUSE_MAY_STOP_VIS;
                }
                else if (mouse_has(MOUSE_VISUAL))
                    jump_flags |= MOUSE_MAY_VIS;
            }
            else if (which_button == MOUSE_RIGHT)
            {
                if (is_click[0] && VIsual_active)
                {
                    /*
                     * Remember the start and end of visual before moving the cursor.
                     */
                    if (ltpos(curwin.w_cursor, VIsual))
                    {
                        COPY_pos(start_visual, curwin.w_cursor);
                        COPY_pos(end_visual, VIsual);
                    }
                    else
                    {
                        COPY_pos(start_visual, VIsual);
                        COPY_pos(end_visual, curwin.w_cursor);
                    }
                }
                jump_flags |= MOUSE_FOCUS;
                if (mouse_has(MOUSE_VISUAL))
                    jump_flags |= MOUSE_MAY_VIS;
            }
        }

        /*
         * If an operator is pending, ignore all drags and releases until the next mouse click.
         */
        if (!is_drag[0] && oap != null && oap.op_type != OP_NOP)
        {
            got_click = false;
            oap.motion_type = MCHAR;
        }

        /* When releasing the button let jump_to_mouse() know. */
        if (!is_click[0] && !is_drag[0])
            jump_flags |= MOUSE_RELEASED;

        /*
         * JUMP!
         */
        {
            boolean[] __ = (oap != null) ? new boolean[] { oap.inclusive } : null;
            jump_flags = jump_to_mouse(jump_flags, __, which_button);
            if (oap != null)
                oap.inclusive = __[0];
        }

        boolean moved = ((jump_flags & CURSOR_MOVED) != 0);                    /* Has cursor moved? */
        boolean in_status_line = ((jump_flags & IN_STATUS_LINE) != 0);         /* mouse in status line */
        boolean in_sep_line = ((jump_flags & IN_SEP_LINE) != 0);               /* mouse in vertical separator line */

        /* When jumping to another window, clear a pending operator.
         * That's a bit friendlier than beeping and not jumping to that window. */
        if (curwin != old_curwin && oap != null && oap.op_type != OP_NOP)
            clearop(oap);

        if ((jump_flags & IN_OTHER_WIN) != 0 && !VIsual_active && clip_star.available)
        {
            clip_modeless(which_button, is_click[0], is_drag[0]);
            return false;
        }

        /* Set global flag that we are extending the Visual area with mouse dragging;
         * temporarily minimize 'scrolloff'. */
        if (VIsual_active && is_drag[0] && p_so[0] != 0)
        {
            /* In the very first line, allow scrolling one line. */
            if (mouse_row == 0)
                mouse_dragging = 2;
            else
                mouse_dragging = 1;
        }

        /* When dragging the mouse above the window, scroll down. */
        if (is_drag[0] && mouse_row < 0 && !in_status_line)
        {
            scroll_redraw(false, 1L);
            mouse_row = 0;
        }

        if (start_visual.lnum != 0)          /* right click in visual mode */
        {
            /* When ALT is pressed make Visual mode blockwise. */
            if ((mod_mask & MOD_MASK_ALT) != 0)
                VIsual_mode = Ctrl_V;

            /*
             * In Visual-block mode, divide the area in four,
             * pick up the corner that is in the quarter that the cursor is in.
             */
            if (VIsual_mode == Ctrl_V)
            {
                int[] leftcol = new int[1];
                int[] rightcol = new int[1];
                getvcols(curwin, start_visual, end_visual, leftcol, rightcol);
                if ((leftcol[0] + rightcol[0]) / 2 < curwin.w_curswant)
                    end_visual.col = leftcol[0];
                else
                    end_visual.col = rightcol[0];
                if (curwin.w_cursor.lnum < (start_visual.lnum + end_visual.lnum) / 2)
                    end_visual.lnum = end_visual.lnum;
                else
                    end_visual.lnum = start_visual.lnum;

                /* move VIsual to the right column */
                COPY_pos(start_visual, curwin.w_cursor);    /* save the cursor pos */
                COPY_pos(curwin.w_cursor, end_visual);
                coladvance(end_visual.col);
                COPY_pos(VIsual, curwin.w_cursor);
                COPY_pos(curwin.w_cursor, start_visual);    /* restore the cursor */
            }
            else
            {
                /*
                 * If the click is before the start of visual, change the start.
                 * If the click is after the end of visual, change the end.
                 * If the click is inside the visual, change the closest side.
                 */
                if (ltpos(curwin.w_cursor, start_visual))
                    COPY_pos(VIsual, end_visual);
                else if (ltpos(end_visual, curwin.w_cursor))
                    COPY_pos(VIsual, start_visual);
                else
                {
                    /* In the same line, compare column number. */
                    if (end_visual.lnum == start_visual.lnum)
                    {
                        if (end_visual.col - curwin.w_cursor.col < curwin.w_cursor.col - start_visual.col)
                            COPY_pos(VIsual, start_visual);
                        else
                            COPY_pos(VIsual, end_visual);
                    }
                    /* In different lines, compare line number. */
                    else
                    {
                        long diff = (curwin.w_cursor.lnum - start_visual.lnum) - (end_visual.lnum - curwin.w_cursor.lnum);

                        if (0 < diff)               /* closest to end */
                            COPY_pos(VIsual, start_visual);
                        else if (diff < 0)          /* closest to start */
                            COPY_pos(VIsual, end_visual);
                        else                        /* in the middle line */
                        {
                            if (curwin.w_cursor.col < (start_visual.col + end_visual.col) / 2)
                                COPY_pos(VIsual, end_visual);
                            else
                                COPY_pos(VIsual, start_visual);
                        }
                    }
                }
            }
        }
        /*
         * If Visual mode started in insert mode, execute "CTRL-O"
         */
        else if ((State & INSERT) != 0 && VIsual_active)
            stuffcharReadbuff(Ctrl_O);

        /*
         * Middle mouse click: Put text before cursor.
         */
        if (which_button == MOUSE_MIDDLE)
        {
            if (clip_star.available && regname == 0)
                regname = '*';
            if (yank_register_mline(regname))
            {
                if (mouse_past_bottom)
                    dir = FORWARD;
            }
            else if (mouse_past_eol)
                dir = FORWARD;

            int c1, c2;
            if (fixindent != 0)
            {
                c1 = (dir == BACKWARD) ? '[' : ']';
                c2 = 'p';
            }
            else
            {
                c1 = (dir == FORWARD) ? 'p' : 'P';
                c2 = NUL;
            }
            prep_redo(regname, count, NUL, c1, NUL, c2, NUL);

            /*
             * Remember where the paste started, so in edit() insStart can be set to this position.
             */
            if (restart_edit != 0)
                COPY_pos(where_paste_started, curwin.w_cursor);
            do_put(regname, dir, count, fixindent | PUT_CURSEND);
        }

        /*
         * Ctrl-Mouse click jumps to the tag under the mouse pointer.
         */
        else if ((mod_mask & MOD_MASK_CTRL) != 0)
        {
            if ((State & INSERT) != 0)
                stuffcharReadbuff(Ctrl_O);
            stuffcharReadbuff(Ctrl_RSB);
            got_click = false;              /* ignore drag&release now */
        }

        /*
         * Shift-Mouse click searches for the next occurrence of the word under the mouse pointer
         */
        else if ((mod_mask & MOD_MASK_SHIFT) != 0)
        {
            if ((State & INSERT) != 0 || (VIsual_active && VIsual_select))
                stuffcharReadbuff(Ctrl_O);
            if (which_button == MOUSE_LEFT)
                stuffcharReadbuff('*');
            else    /* MOUSE_RIGHT */
                stuffcharReadbuff('#');
        }

        /* Handle double clicks, unless on status line. */
        else if (in_status_line)
        {
        }
        else if (in_sep_line)
        {
        }
        else if ((mod_mask & MOD_MASK_MULTI_CLICK) != 0 && (State & (NORMAL | INSERT)) != 0 && mouse_has(MOUSE_VISUAL))
        {
            if (is_click[0] || !VIsual_active)
            {
                if (VIsual_active)
                    COPY_pos(orig_cursor, VIsual);
                else
                {
                    check_visual_highlight();
                    COPY_pos(VIsual, curwin.w_cursor);
                    COPY_pos(orig_cursor, VIsual);
                    VIsual_active = true;
                    VIsual_reselect = true;
                    /* start Select mode if 'selectmode' contains "mouse" */
                    may_start_select('o');
                    setmouse();
                }
                if ((mod_mask & MOD_MASK_MULTI_CLICK) == MOD_MASK_2CLICK)
                {
                    /* Double click with ALT pressed makes it blockwise. */
                    if ((mod_mask & MOD_MASK_ALT) != 0)
                        VIsual_mode = Ctrl_V;
                    else
                        VIsual_mode = 'v';
                }
                else if ((mod_mask & MOD_MASK_MULTI_CLICK) == MOD_MASK_3CLICK)
                    VIsual_mode = 'V';
                else if ((mod_mask & MOD_MASK_MULTI_CLICK) == MOD_MASK_4CLICK)
                    VIsual_mode = Ctrl_V;
                /* Make sure the clipboard gets updated.  Needed because start and
                 * end may still be the same, and the selection needs to be owned. */
                clip_star.vmode = NUL;
            }

            /*
             * A double click selects a word or a block.
             */
            if ((mod_mask & MOD_MASK_MULTI_CLICK) == MOD_MASK_2CLICK)
            {
                pos_C pos = null;

                if (is_click[0])
                {
                    /* If the character under the cursor (skipping white space) is not a word character,
                     * try finding a match and select a (), {}, [], #if/#endif, etc. block. */
                    COPY_pos(end_visual, curwin.w_cursor);
                    while (vim_iswhite(gchar_pos(end_visual)))
                        inc(end_visual);
                    if (oap != null)
                        oap.motion_type = MCHAR;
                    if (oap != null
                            && VIsual_mode == 'v'
                            && !vim_iswordc(gchar_pos(end_visual), curbuf)
                            && eqpos(curwin.w_cursor, VIsual)
                            && (pos = findmatch(oap, NUL)) != null)
                    {
                        COPY_pos(curwin.w_cursor, pos);
                        if (oap.motion_type == MLINE)
                            VIsual_mode = 'V';
                        else if (p_sel[0].at(0) == (byte)'e')
                        {
                            if (ltpos(curwin.w_cursor, VIsual))
                                VIsual.col++;
                            else
                                curwin.w_cursor.col++;
                        }
                    }
                }

                if (pos == null && (is_click[0] || is_drag[0]))
                {
                    /* When not found a match or when dragging: extend to include a word. */
                    if (ltpos(curwin.w_cursor, orig_cursor))
                    {
                        find_start_of_word(curwin.w_cursor);
                        find_end_of_word(VIsual);
                    }
                    else
                    {
                        find_start_of_word(VIsual);
                        if (p_sel[0].at(0) == (byte)'e' && ml_get_cursor().at(0) != NUL)
                            curwin.w_cursor.col += us_ptr2len_cc(ml_get_cursor());
                        find_end_of_word(curwin.w_cursor);
                    }
                }
                curwin.w_set_curswant = true;
            }
            if (is_click[0])
                redraw_curbuf_later(INVERTED);      /* update the inversion */
        }
        else if (VIsual_active && !old_active)
        {
            if ((mod_mask & MOD_MASK_ALT) != 0)
                VIsual_mode = Ctrl_V;
            else
                VIsual_mode = 'v';
        }

        /* If Visual mode changed show it later. */
        if ((!VIsual_active && old_active && mode_displayed)
                || (VIsual_active && p_smd[0] && msg_silent == 0 && (!old_active || VIsual_mode != old_mode)))
            redraw_cmdline = true;

        return moved;
    }

    /*
     * Move "pos" back to the start of the word it's in.
     */
    /*private*/ static void find_start_of_word(pos_C pos)
    {
        Bytes line = ml_get(pos.lnum);
        int cclass = get_mouse_class(line.plus(pos.col));

        while (0 < pos.col)
        {
            int col = pos.col - 1;
            col -= us_head_off(line, line.plus(col));
            if (get_mouse_class(line.plus(col)) != cclass)
                break;
            pos.col = col;
        }
    }

    /*
     * Move "pos" forward to the end of the word it's in.
     * When 'selection' is "exclusive", the position is just after the word.
     */
    /*private*/ static void find_end_of_word(pos_C pos)
    {
        Bytes line = ml_get(pos.lnum);
        if (p_sel[0].at(0) == (byte)'e' && 0 < pos.col)
        {
            --pos.col;
            pos.col -= us_head_off(line, line.plus(pos.col));
        }

        int cclass = get_mouse_class(line.plus(pos.col));
        while (line.at(pos.col) != NUL)
        {
            int col = pos.col + us_ptr2len_cc(line.plus(pos.col));
            if (get_mouse_class(line.plus(col)) != cclass)
            {
                if (p_sel[0].at(0) == (byte)'e')
                    pos.col = col;
                break;
            }
            pos.col = col;
        }
    }

    /*
     * Get class of a character for selection: same class means same word.
     *  0: blank
     *  1: punctuation groups
     *  2: normal word character
     * >2: multi-byte word character.
     */
    /*private*/ static int get_mouse_class(Bytes p)
    {
        if (1 < us_byte2len(p.at(0), false))
            return us_get_class(p, curbuf);

        if (p.at(0) == (byte)' ' || p.at(0) == (byte)'\t')
            return 0;

        if (us_iswordb(p.at(0), curbuf))
            return 2;

        /*
         * There are a few special cases where we want certain combinations of
         * characters to be considered as a single word.  These are things like
         * "->", "/ *", "*=", "+=", "&=", "<=", ">=", "!=" etc.  Otherwise, each
         * character is in its own class.
         */
        if (p.at(0) != NUL && vim_strbyte(u8("-+*/%<>&|^!="), p.at(0)) != null)
            return 1;

        return char_u(p.at(0));
    }

    /*private*/ static boolean did_check_visual_highlight;

    /*
     * Check if highlighting for visual mode is possible, give a warning message if not.
     */
    /*private*/ static void check_visual_highlight()
    {
        if (full_screen)
        {
            if (!did_check_visual_highlight && hl_attr(HLF_V) == 0)
                msg(u8("Warning: terminal cannot highlight"));
            did_check_visual_highlight = true;
        }
    }

    /*
     * End Visual mode.
     * This function should ALWAYS be called to end Visual mode, except from do_pending_operator().
     */
    /*private*/ static void end_visual_mode()
    {
        /*
         * If we are using the clipboard, then remember what was selected in case
         * we need to paste it somewhere while we still own the selection.
         * Only do this when the clipboard is already owned.  Don't want to grab
         * the selection when hitting ESC.
         */
        if (clip_star.available && clip_star.owned)
            clip_auto_select();

        VIsual_active = false;
        setmouse();
        mouse_dragging = 0;

        /* Save the current VIsual area for '< and '> marks, and "gv". */
        curbuf.b_visual.vi_mode = VIsual_mode;
        COPY_pos(curbuf.b_visual.vi_start, VIsual);
        COPY_pos(curbuf.b_visual.vi_end, curwin.w_cursor);
        curbuf.b_visual.vi_curswant = curwin.w_curswant;
        curbuf.b_visual_mode_eval = VIsual_mode;
        if (!virtual_active())
            curwin.w_cursor.coladd = 0;

        if (mode_displayed)
            clear_cmdline = true;           /* unshow visual mode later */
        else
            clear_showcmd();

        adjust_cursor_eol();
    }

    /*
     * Reset VIsual_active and VIsual_reselect.
     */
    /*private*/ static void reset_VIsual_and_resel()
    {
        if (VIsual_active)
        {
            end_visual_mode();
            redraw_curbuf_later(INVERTED);  /* delete the inversion later */
        }
        VIsual_reselect = false;
    }

    /*
     * Reset VIsual_active and VIsual_reselect if it's set.
     */
    /*private*/ static void reset_VIsual()
    {
        if (VIsual_active)
        {
            end_visual_mode();
            redraw_curbuf_later(INVERTED);  /* delete the inversion later */
            VIsual_reselect = false;
        }
    }

    /*
     * Find the identifier under or to the right of the cursor.
     * "find_type" can have one of three values:
     * FIND_IDENT:   find an identifier (keyword)
     * FIND_STRING:  find any non-white string
     * FIND_IDENT + FIND_STRING: find any non-white string, identifier preferred.
     * FIND_EVAL:    find text useful for C program debugging
     *
     * There are three steps:
     * 1. Search forward for the start of an identifier/string.
     *    Doesn't move if already on one.
     * 2. Search backward for the start of this identifier/string.
     *    This doesn't match the real Vi but I like it a little better
     *    and it shouldn't bother anyone.
     * 3. Search forward to the end of this identifier/string.
     *    When FIND_IDENT isn't defined, we backup until a blank.
     *
     * Returns the length of the string, or zero if no string is found.
     * If a string is found, a pointer to the string is put in "*string".
     * This string is not always NUL terminated.
     */
    /*private*/ static int find_ident_under_cursor(Bytes[] string, int find_type)
    {
        return find_ident_at_pos(curwin, curwin.w_cursor.lnum, curwin.w_cursor.col, string, find_type);
    }

    /*
     * Like find_ident_under_cursor(), but for any window and any position.
     * However: Uses 'iskeyword' from the current window!.
     */
    /*private*/ static int find_ident_at_pos(window_C wp, long lnum, int startcol, Bytes[] string, int find_type)
    {
        Bytes p = ml_get_buf(wp.w_buffer, lnum, false);

        int col = 0;
        int this_class = 0;

        int round;
        /*
         * if round == 0: try to find an identifier
         * if round == 1: try to find any non-white string
         */
        for (round = (find_type & FIND_IDENT) != 0 ? 0 : 1; round < 2; round++)
        {
            /*
             * 1. Skip to start of identifier/string.
             */
            col = startcol;
            while (p.at(col) != NUL)
            {
                this_class = us_get_class(p.plus(col), curbuf);
                if (this_class != 0 && (round == 1 || this_class != 1))
                    break;
                col += us_ptr2len_cc(p.plus(col));
            }

            /*
             * 2. Back up to start of identifier/string.
             */
            /* Remember class of character under cursor. */
            this_class = us_get_class(p.plus(col), curbuf);
            while (0 < col && this_class != 0)
            {
                int prevcol = col - 1 - us_head_off(p, p.plus(col - 1));
                int prev_class = us_get_class(p.plus(prevcol), curbuf);
                if (this_class != prev_class && (round == 0 || prev_class == 0 || (find_type & FIND_IDENT) != 0))
                    break;
                col = prevcol;
            }

            /* If we don't want just any old string, or we've found an identifier, stop searching. */
            if (2 < this_class)
                this_class = 2;
            if ((find_type & FIND_STRING) == 0 || this_class == 2)
                break;
        }

        if (p.at(col) == NUL || (round == 0 && this_class != 2))
        {
            /*
             * didn't find an identifier or string
             */
            if ((find_type & FIND_STRING) != 0)
                emsg(u8("E348: No string under cursor"));
            else
                emsg(e_noident);
            return 0;
        }
        p = p.plus(col);
        string[0] = p;

        /*
         * 3. Find the end if the identifier/string.
         */
        col = 0;
        /* Search for point of changing multibyte character class. */
        this_class = us_get_class(p, curbuf);
        while (p.at(col) != NUL
            && ((round == 0 ? us_get_class(p.plus(col), curbuf) == this_class : us_get_class(p.plus(col), curbuf) != 0)))
            col += us_ptr2len_cc(p.plus(col));

        return col;
    }

    /*
     * Prepare for redo of a normal command.
     */
    /*private*/ static void prep_redo_cmd(cmdarg_C cap)
    {
        prep_redo(cap.oap.regname, cap.count0, NUL, cap.cmdchar, NUL, NUL, cap.nchar[0]);
    }

    /*
     * Prepare for redo of any command.
     * Note that only the last argument can be a multi-byte char.
     */
    /*private*/ static void prep_redo(int regname, long num, int cmd1, int cmd2, int cmd3, int cmd4, int cmd5)
    {
        resetRedobuff();
        if (regname != 0)   /* yank from specified buffer */
        {
            appendCharToRedobuff('"');
            appendCharToRedobuff(regname);
        }
        if (num != 0)
            appendNumberToRedobuff(num);

        if (cmd1 != NUL)
            appendCharToRedobuff(cmd1);
        if (cmd2 != NUL)
            appendCharToRedobuff(cmd2);
        if (cmd3 != NUL)
            appendCharToRedobuff(cmd3);
        if (cmd4 != NUL)
            appendCharToRedobuff(cmd4);
        if (cmd5 != NUL)
            appendCharToRedobuff(cmd5);
    }

    /*
     * check for operator active and clear it
     *
     * return true if operator was active
     */
    /*private*/ static boolean checkclearop(oparg_C oap)
    {
        if (oap.op_type == OP_NOP)
            return false;
        clearopbeep(oap);
        return true;
    }

    /*
     * Check for operator or Visual active.  Clear active operator.
     *
     * Return true if operator or Visual was active.
     */
    /*private*/ static boolean checkclearopq(oparg_C oap)
    {
        if (oap.op_type == OP_NOP && !VIsual_active)
            return false;
        clearopbeep(oap);
        return true;
    }

    /*private*/ static void clearop(oparg_C oap)
    {
        oap.op_type = OP_NOP;
        oap.regname = 0;
        oap.motion_force = NUL;
        oap.use_reg_one = false;
    }

    /*private*/ static void clearopbeep(oparg_C oap)
    {
        clearop(oap);
        beep_flush();
    }

    /*
     * Remove the shift modifier from a special key.
     */
    /*private*/ static void unshift_special(cmdarg_C cap)
    {
        switch (cap.cmdchar)
        {
            case K_S_RIGHT: cap.cmdchar = K_RIGHT; break;
            case K_S_LEFT:  cap.cmdchar = K_LEFT; break;
            case K_S_UP:    cap.cmdchar = K_UP; break;
            case K_S_DOWN:  cap.cmdchar = K_DOWN; break;
            case K_S_HOME:  cap.cmdchar = K_HOME; break;
            case K_S_END:   cap.cmdchar = K_END; break;
        }
        { int[] __ = { mod_mask }; cap.cmdchar = simplify_key(cap.cmdchar, __); mod_mask = __[0]; }
    }

    /*
     * Routines for displaying a partly typed command
     */

    /*private*/ static final int SHOWCMD_BUFLEN = SHOWCMD_COLS + 1 + 30;
    /*private*/ static Bytes    showcmd_buf = new Bytes(SHOWCMD_BUFLEN);
    /*private*/ static Bytes    old_showcmd_buf = new Bytes(SHOWCMD_BUFLEN); /* for push_showcmd() */
    /*private*/ static boolean  showcmd_is_clear = true;
    /*private*/ static boolean  showcmd_visual;

    /*private*/ static void clear_showcmd()
    {
        if (!p_sc[0])
            return;

        if (VIsual_active && !char_avail())
        {
            boolean cursor_bot = ltpos(VIsual, curwin.w_cursor);
            long lines;
            long top, bot;

            /* Show the size of the Visual area. */
            if (cursor_bot)
            {
                top = VIsual.lnum;
                bot = curwin.w_cursor.lnum;
            }
            else
            {
                top = curwin.w_cursor.lnum;
                bot = VIsual.lnum;
            }
            lines = bot - top + 1;

            if (VIsual_mode == Ctrl_V)
            {
                Bytes saved_sbr = p_sbr[0];
                int[] leftcol = new int[1];
                int[] rightcol = new int[1];

                /* Make 'sbr' empty for a moment to get the correct size. */
                p_sbr[0] = EMPTY_OPTION;
                getvcols(curwin, curwin.w_cursor, VIsual, leftcol, rightcol);
                p_sbr[0] = saved_sbr;
                libC.sprintf(showcmd_buf, u8("%ldx%ld"), lines, (long)(rightcol[0] - leftcol[0] + 1));
            }
            else if (VIsual_mode == 'V' || VIsual.lnum != curwin.w_cursor.lnum)
                libC.sprintf(showcmd_buf, u8("%ld"), lines);
            else
            {
                int bytes = 0;
                int chars = 0;

                Bytes s, e;
                if (cursor_bot)
                {
                    s = ml_get_pos(VIsual);
                    e = ml_get_cursor();
                }
                else
                {
                    s = ml_get_cursor();
                    e = ml_get_pos(VIsual);
                }
                while ((p_sel[0].at(0) != (byte)'e') ? BLE(s, e) : BLT(s, e))
                {
                    int l = us_ptr2len_cc(s);
                    if (l == 0)
                    {
                        bytes++;
                        chars++;
                        break;  /* end of line */
                    }
                    bytes += l;
                    chars++;
                    s = s.plus(l);
                }
                if (bytes == chars)
                    libC.sprintf(showcmd_buf, u8("%d"), chars);
                else
                    libC.sprintf(showcmd_buf, u8("%d-%d"), chars, bytes);
            }
            showcmd_buf.be(SHOWCMD_COLS, NUL);        /* truncate */
            showcmd_visual = true;
        }
        else
        {
            showcmd_buf.be(0, NUL);
            showcmd_visual = false;

            /* Don't actually display something if there is nothing to clear. */
            if (showcmd_is_clear)
                return;
        }

        display_showcmd();
    }

    /*private*/ static int[] ignore_showcmd =
    {
        K_IGNORE,
        K_LEFTMOUSE, K_LEFTDRAG, K_LEFTRELEASE,
        K_MIDDLEMOUSE, K_MIDDLEDRAG, K_MIDDLERELEASE,
        K_RIGHTMOUSE, K_RIGHTDRAG, K_RIGHTRELEASE,
        K_MOUSEDOWN, K_MOUSEUP, K_MOUSELEFT, K_MOUSERIGHT,
        K_X1MOUSE, K_X1DRAG, K_X1RELEASE, K_X2MOUSE, K_X2DRAG, K_X2RELEASE,
        K_CURSORHOLD,
        0
    };

    /*
     * Add 'c' to string of shown command chars.
     * Return true if output has been written (and setcursor() has been called).
     */
    /*private*/ static boolean add_to_showcmd(int c)
    {
        if (!p_sc[0] || msg_silent != 0)
            return false;

        if (showcmd_visual)
        {
            showcmd_buf.be(0, NUL);
            showcmd_visual = false;
        }

        /* Ignore keys that are scrollbar updates and mouse clicks. */
        if (is_special(c))
            for (int i = 0; ignore_showcmd[i] != 0; i++)
                if (ignore_showcmd[i] == c)
                    return false;

        Bytes p = transchar(c);
        if (p.at(0) == (byte)' ')
            STRCPY(p, u8("<20>"));

        int old_len = strlen(showcmd_buf);
        int extra_len = strlen(p);
        int overflow = old_len + extra_len - SHOWCMD_COLS;
        if (0 < overflow)
            BCOPY(showcmd_buf, 0, showcmd_buf, overflow, old_len - overflow + 1);
        STRCAT(showcmd_buf, p);

        if (char_avail())
            return false;

        display_showcmd();

        return true;
    }

    /*private*/ static void add_to_showcmd_c(int c)
    {
        if (!add_to_showcmd(c))
            setcursor();
    }

    /*
     * Delete 'len' characters from the end of the shown command.
     */
    /*private*/ static void del_from_showcmd(int len)
    {
        if (!p_sc[0])
            return;

        int old_len = strlen(showcmd_buf);
        if (old_len < len)
            len = old_len;
        showcmd_buf.be(old_len - len, NUL);

        if (!char_avail())
            display_showcmd();
    }

    /*
     * push_showcmd() and pop_showcmd() are used when waiting for the user
     * to type something and there is a partial mapping.
     */
    /*private*/ static void push_showcmd()
    {
        if (p_sc[0])
            STRCPY(old_showcmd_buf, showcmd_buf);
    }

    /*private*/ static void pop_showcmd()
    {
        if (!p_sc[0])
            return;

        STRCPY(showcmd_buf, old_showcmd_buf);

        display_showcmd();
    }

    /*private*/ static void display_showcmd()
    {
        cursor_off();

        int len = strlen(showcmd_buf);
        if (len == 0)
            showcmd_is_clear = true;
        else
        {
            screen_puts(showcmd_buf, (int)Rows[0] - 1, sc_col, 0);
            showcmd_is_clear = false;
        }

        /*
         * clear the rest of an old message by outputting up to SHOWCMD_COLS spaces
         */
        screen_puts(u8("          ").plus(len), (int)Rows[0] - 1, sc_col + len, 0);

        setcursor();            /* put cursor back where it belongs */
    }

    /*private*/ static window_C scr_old_curwin;
    /*private*/ static long scr_old_topline;
    /*private*/ static buffer_C scr_old_buf;
    /*private*/ static int scr_old_leftcol;

    /*
     * When "check" is false, prepare for commands that scroll the window.
     * When "check" is true, take care of scroll-binding after the window has scrolled.
     * Called from normal_cmd() and edit().
     */
    /*private*/ static void do_check_scrollbind(boolean check)
    {
        if (check && curwin.w_onebuf_opt.wo_scb[0])
        {
            /* If a ":syncbind" command was just used, don't scroll, only reset the values. */
            if (did_syncbind)
                did_syncbind = false;
            else if (curwin == scr_old_curwin)
            {
                /*
                 * Synchronize other windows, as necessary according to 'scrollbind'.
                 * Don't do this after an ":edit" command, except when 'diff' is set.
                 */
                if ((curwin.w_buffer == scr_old_buf)
                    && (curwin.w_topline != scr_old_topline || curwin.w_leftcol != scr_old_leftcol))
                {
                    check_scrollbind(curwin.w_topline - scr_old_topline,
                            (long)(curwin.w_leftcol - scr_old_leftcol));
                }
            }
            else if (vim_strchr(p_sbo[0], 'j') != null) /* jump flag set in 'scrollopt' */
            {
                /*
                 * When switching between windows, make sure that the relative
                 * vertical offset is valid for the new window.  The relative
                 * offset is invalid whenever another 'scrollbind' window has
                 * scrolled to a point that would force the current window to
                 * scroll past the beginning or end of its buffer.  When the
                 * resync is performed, some of the other 'scrollbind' windows may
                 * need to jump so that the current window's relative position is
                 * visible on-screen.
                 */
                check_scrollbind(curwin.w_topline - curwin.w_scbind_pos, 0L);
            }
            curwin.w_scbind_pos = curwin.w_topline;
        }

        scr_old_curwin = curwin;
        scr_old_topline = curwin.w_topline;
        scr_old_buf = curwin.w_buffer;
        scr_old_leftcol = curwin.w_leftcol;
    }

    /*
     * Synchronize any windows that have "scrollbind" set,
     * based on the number of rows by which the current window has changed.
     */
    /*private*/ static void check_scrollbind(long topline_diff, long leftcol_diff)
    {
        window_C old_curwin = curwin;
        buffer_C old_curbuf = curbuf;
        boolean old_VIsual_select = VIsual_select;
        boolean old_VIsual_active = VIsual_active;
        int tgt_leftcol = curwin.w_leftcol;

        /*
         * check 'scrollopt' string for vertical and horizontal scroll options
         */
        boolean want_ver = (vim_strchr(p_sbo[0], 'v') != null && topline_diff != 0);
        boolean want_hor = (vim_strchr(p_sbo[0], 'h') != null && (leftcol_diff != 0 || topline_diff != 0));

        /*
         * loop through the scrollbound windows and scroll accordingly
         */
        VIsual_select = VIsual_active = false;
        for (curwin = firstwin; curwin != null; curwin = curwin.w_next)
        {
            curbuf = curwin.w_buffer;
            /* skip original window  and windows with 'noscrollbind' */
            if (curwin != old_curwin && curwin.w_onebuf_opt.wo_scb[0])
            {
                /*
                 * do the vertical scroll
                 */
                if (want_ver)
                {
                    curwin.w_scbind_pos += topline_diff;
                    long topline = curwin.w_scbind_pos;
                    if (topline > curbuf.b_ml.ml_line_count)
                        topline = curbuf.b_ml.ml_line_count;
                    if (topline < 1)
                        topline = 1;

                    long y = topline - curwin.w_topline;
                    if (0 < y)
                        scrollup(y);
                    else
                        scrolldown(-y);

                    redraw_later(VALID);
                    cursor_correct();
                    curwin.w_redr_status = true;
                }

                /*
                 * do the horizontal scroll
                 */
                if (want_hor && curwin.w_leftcol != tgt_leftcol)
                {
                    curwin.w_leftcol = tgt_leftcol;
                    leftcol_changed();
                }
            }
        }

        /*
         * reset current-window
         */
        VIsual_select = old_VIsual_select;
        VIsual_active = old_VIsual_active;
        curwin = old_curwin;
        curbuf = old_curbuf;
    }

    /*
     * Command character that's ignored.
     * Used for CTRL-Q and CTRL-S to avoid problems with terminals that use xon/xoff.
     */
    /*private*/ static final nv_func_C nv_ignore = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            cap.retval |= CA_COMMAND_BUSY;      /* don't call edit() now */
        }
    };

    /*
     * Command character that doesn't do anything, but unlike nv_ignore()
     * does start edit().  Used for "startinsert" executed while starting up.
     */
    /*private*/ static final nv_func_C nv_nop = new nv_func_C()
    {
        public void nv(cmdarg_C _cap)
        {
        }
    };

    /*
     * Command character doesn't exist.
     */
    /*private*/ static final nv_func_C nv_error = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            clearopbeep(cap.oap);
        }
    };

    /*
     * CTRL-A and CTRL-X: Add or subtract from letter or number under cursor.
     */
    /*private*/ static final nv_func_C nv_addsub = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (!checkclearopq(cap.oap) && do_addsub(cap.cmdchar, cap.count1) == true)
                prep_redo_cmd(cap);
        }
    };

    /*
     * CTRL-F, CTRL-B, etc: Scroll page up or down.
     */
    /*private*/ static final nv_func_C nv_page = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (!checkclearop(cap.oap))
            {
                if ((mod_mask & MOD_MASK_CTRL) != 0)
                {
                    /* <C-PageUp>: tab page back; <C-PageDown>: tab page forward */
                    if (cap.arg == BACKWARD)
                        goto_tabpage(-(int)cap.count1);
                    else
                        goto_tabpage((int)cap.count0);
                }
                else
                onepage(cap.arg, cap.count1);
            }
        }
    };

    /*
     * Implementation of "gd" and "gD" command.
     */
    /*private*/ static void nv_gd(oparg_C oap, int nchar, boolean thisblock)
        /* thisblock: true for "1gd" and "1gD" */
    {
        Bytes[] ptr = new Bytes[1];
        int len = find_ident_under_cursor(ptr, FIND_IDENT);
        if (len == 0 || find_decl(ptr[0], len, nchar == 'd', thisblock, 0) == false)
            clearopbeep(oap);
    }

    /*
     * Search for variable declaration of "ptr[len]".
     * When "locally" is true in the current function ("gd"), otherwise in the current file ("gD").
     * When "thisblock" is true check the {} block scope.
     * Return false when not found.
     */
    /*private*/ static boolean find_decl(Bytes ptr, int len, boolean locally, boolean thisblock, int searchflags)
        /* searchflags: flags passed to searchit() */
    {
        boolean retval = true;

        Bytes pat = new Bytes(len + 7);

        /* Put "\V" before the pattern
         * to avoid that the special meaning of "." and "~" causes trouble. */
        libC.sprintf(pat, us_iswordp(ptr, curbuf) ? u8("\\V\\<%.*s\\>") : u8("\\V%.*s"), len, ptr);
        pos_C old_pos = new pos_C();
        COPY_pos(old_pos, curwin.w_cursor);
        boolean save_p_ws = p_ws[0];
        boolean save_p_scs = p_scs[0];
        p_ws[0] = false;       /* don't wrap around end of file now */
        p_scs[0] = false;      /* don't switch ignorecase off now */

        pos_C par_pos = new pos_C();
        /*
         * With "gD" go to line 1.
         * With "gd" Search back for the start of the current function,
         * then go back until a blank line.  If this fails go to line 1.
         */
        boolean[] incll = new boolean[1];
        if (!locally || !findpar(incll, BACKWARD, 1L, '{', false))
        {
            setpcmark();                    /* set in findpar() otherwise */
            curwin.w_cursor.lnum = 1;
            COPY_pos(par_pos, curwin.w_cursor);
        }
        else
        {
            COPY_pos(par_pos, curwin.w_cursor);
            while (1 < curwin.w_cursor.lnum && skipwhite(ml_get_curline()).at(0) != NUL)
                --curwin.w_cursor.lnum;
        }
        curwin.w_cursor.col = 0;

        boolean found;
        /* Search forward for the identifier, ignore comment lines. */
        pos_C found_pos = new pos_C();
        for ( ; ; )
        {
            int i = searchit(curwin, curbuf, curwin.w_cursor, FORWARD, pat, 1L, searchflags, RE_LAST, 0, null);

            found = (i != 0 && curwin.w_cursor.lnum < old_pos.lnum); /* match after start is failure too */

            if (thisblock && found)
            {
                /* Check that the block the match is in doesn't end before
                 * the position where we started the search from. */
                pos_C pos = findmatchlimit(null, '}', FM_FORWARD, (int)(old_pos.lnum - curwin.w_cursor.lnum + 1));
                if (pos != null && pos.lnum < old_pos.lnum)
                    continue;
            }

            if (!found)
            {
                /* If we previously found a valid position, use it. */
                if (found_pos.lnum != 0)
                {
                    COPY_pos(curwin.w_cursor, found_pos);
                    found = true;
                }
                break;
            }
            if (0 < get_leader_len(ml_get_curline(), null, false, true))
            {
                /* Ignore this line, continue at start of next line. */
                curwin.w_cursor.lnum++;
                curwin.w_cursor.col = 0;
                continue;
            }
            if (!locally)   /* global search: use first match found */
                break;
            if (par_pos.lnum <= curwin.w_cursor.lnum)
            {
                /* If we previously found a valid position, use it. */
                if (found_pos.lnum != 0)
                    COPY_pos(curwin.w_cursor, found_pos);
                break;
            }

            /* For finding a local variable and the match is before the "{" search
             * to find a later match.  For K&R style function declarations this
             * skips the function header without types. */
            COPY_pos(found_pos, curwin.w_cursor);
        }

        if (!found)
        {
            retval = false;
            COPY_pos(curwin.w_cursor, old_pos);
        }
        else
        {
            curwin.w_set_curswant = true;
            /* "n" searches forward now */
            reset_search_dir();
        }

        p_ws[0] = save_p_ws;
        p_scs[0] = save_p_scs;

        return retval;
    }

    /*
     * Move 'dist' lines in direction 'dir',
     * counting lines by *screen* lines rather than lines in the file.
     * 'dist' must be positive.
     *
     * Return true if able to move cursor, false otherwise.
     */
    /*private*/ static boolean nv_screengo(oparg_C oap, int dir, long dist)
    {
        boolean retval = true;
        boolean atend = false;

        int linelen = linetabsize(ml_get_curline());

        oap.motion_type = MCHAR;
        oap.inclusive = (curwin.w_curswant == MAXCOL);

        int col_off1 = curwin_col_off();                /* margin offset for first screen line */
        int col_off2 = col_off1 - curwin_col_off2()     /* margin offset for wrapped screen line */;
        int width1 = curwin.w_width - col_off1;         /* text width for first screen line */
        int width2 = curwin.w_width - col_off2;         /* test width for wrapped screen line */
        if (width2 == 0)
            width2 = 1;                                 /* avoid divide by zero */

        if (curwin.w_width != 0)
        {
            /*
             * Instead of sticking at the last character of the buffer line we
             * try to stick in the last column of the screen.
             */
            if (curwin.w_curswant == MAXCOL)
            {
                atend = true;
                validate_virtcol();
                if (width1 <= 0)
                    curwin.w_curswant = 0;
                else
                {
                    curwin.w_curswant = width1 - 1;
                    if (curwin.w_curswant < curwin.w_virtcol)
                        curwin.w_curswant += ((curwin.w_virtcol - curwin.w_curswant - 1) / width2 + 1) * width2;
                }
            }
            else
            {
                int n;
                if (width1 < linelen)
                    n = ((linelen - width1 - 1) / width2 + 1) * width2 + width1;
                else
                    n = width1;
                if (curwin.w_curswant > n + 1)
                    curwin.w_curswant -= ((curwin.w_curswant - n) / width2 + 1) * width2;
            }

            while (0 < dist--)
            {
                if (dir == BACKWARD)
                {
                    if (width2 <= (long)curwin.w_curswant)
                        /* move back within line */
                        curwin.w_curswant -= width2;
                    else
                    {
                        /* to previous line */
                        if (curwin.w_cursor.lnum == 1)
                        {
                            retval = false;
                            break;
                        }
                        --curwin.w_cursor.lnum;
                        linelen = linetabsize(ml_get_curline());
                        if (width1 < linelen)
                            curwin.w_curswant += (((linelen - width1 - 1) / width2) + 1) * width2;
                    }
                }
                else /* dir == FORWARD */
                {
                    int n;
                    if (width1 < linelen)
                        n = ((linelen - width1 - 1) / width2 + 1) * width2 + width1;
                    else
                        n = width1;
                    if (curwin.w_curswant + width2 < n)
                        /* move forward within line */
                        curwin.w_curswant += width2;
                    else
                    {
                        /* to next line */
                        if (curwin.w_cursor.lnum == curbuf.b_ml.ml_line_count)
                        {
                            retval = false;
                            break;
                        }
                        curwin.w_cursor.lnum++;
                        curwin.w_curswant %= width2;
                        linelen = linetabsize(ml_get_curline());
                    }
                }
            }
        }

        if (virtual_active() && atend)
            coladvance(MAXCOL);
        else
            coladvance(curwin.w_curswant);

        if (0 < curwin.w_cursor.col && curwin.w_onebuf_opt.wo_wrap[0])
        {
            int virtcol;

            /*
             * Check for landing on a character that got split at the end of the
             * last line.  We want to advance a screenline, not end up in the same
             * screenline or move two screenlines.
             */
            validate_virtcol();
            virtcol = curwin.w_virtcol;
            if (virtcol > width1 && p_sbr[0].at(0) != NUL)
                virtcol -= mb_string2cells(p_sbr[0], -1);

            if (curwin.w_curswant < virtcol
                    && (curwin.w_curswant < width1
                        ? (curwin.w_curswant > width1 / 2)
                        : ((curwin.w_curswant - width1) % width2 > width2 / 2)))
                --curwin.w_cursor.col;
        }

        if (atend)
            curwin.w_curswant = MAXCOL;     /* stick in the last column */

        return retval;
    }

    /*
     * Mouse scroll wheel: Default action is to scroll three lines,
     * or one page when Shift or Ctrl is used.
     * K_MOUSEUP (cap.arg == 1) or K_MOUSEDOWN (cap.arg == 0) or
     * K_MOUSELEFT (cap.arg == -1) or K_MOUSERIGHT (cap.arg == -2)
     */
    /*private*/ static final nv_func_C nv_mousescroll = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            window_C old_curwin = curwin;

            if (0 <= mouse_row && 0 <= mouse_col)
            {
                int[] row = { mouse_row };
                int[] col = { mouse_col };

                /* find the window at the pointer coordinates */
                curwin = mouse_find_win(row, col);
                curbuf = curwin.w_buffer;
            }

            if (cap.arg == MSCR_UP || cap.arg == MSCR_DOWN)
            {
                if ((mod_mask & (MOD_MASK_SHIFT | MOD_MASK_CTRL)) != 0)
                {
                    onepage((cap.arg != 0) ? FORWARD : BACKWARD, 1L);
                }
                else
                {
                    cap.count1 = 3;
                    cap.count0 = 3;
                    nv_scroll_line.nv(cap);
                }
            }

            curwin.w_redr_status = true;

            curwin = old_curwin;
            curbuf = curwin.w_buffer;
        }
    };

    /*
     * Mouse clicks and drags.
     */
    /*private*/ static final nv_func_C nv_mouse = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            do_mouse(cap.oap, cap.cmdchar, BACKWARD, (int)cap.count1, 0);
        }
    };

    /*
     * Handle CTRL-E and CTRL-Y commands: scroll a line up or down.
     * cap.arg must be TRUE for CTRL-E.
     */
    /*private*/ static final nv_func_C nv_scroll_line = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (!checkclearop(cap.oap))
                scroll_redraw(cap.arg != 0, cap.count1);
        }
    };

    /*
     * Scroll "count" lines up or down, and redraw.
     */
    /*private*/ static void scroll_redraw(boolean up, long count)
    {
        long prev_topline = curwin.w_topline;
        long prev_lnum = curwin.w_cursor.lnum;

        if (up)
            scrollup(count);
        else
            scrolldown(count);
        if (p_so[0] != 0)
        {
            /* Adjust the cursor position for 'scrolloff'.  Mark w_topline as valid,
             * otherwise the screen jumps back at the end of the file. */
            cursor_correct();
            check_cursor_moved(curwin);
            curwin.w_valid |= VALID_TOPLINE;

            /* If moved back to where we were, at least move the cursor, otherwise
             * we get stuck at one position.  Don't move the cursor up if the
             * first line of the buffer is already on the screen. */
            while (curwin.w_topline == prev_topline)
            {
                if (up)
                {
                    if (prev_lnum < curwin.w_cursor.lnum || cursor_down(1L, false) == false)
                        break;
                }
                else
                {
                    if (curwin.w_cursor.lnum < prev_lnum || prev_topline == 1L || cursor_up(1L, false) == false)
                        break;
                }
                /* Mark w_topline as valid, otherwise the screen jumps back at the end of the file. */
                check_cursor_moved(curwin);
                curwin.w_valid |= VALID_TOPLINE;
            }
        }
        if (curwin.w_cursor.lnum != prev_lnum)
            coladvance(curwin.w_curswant);
        redraw_later(VALID);
    }

    /*
     * Commands that start with "z".
     */
    /*private*/ static final nv_func_C nv_zet = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            int nchar = cap.nchar[0];

            dozet:
            if (asc_isdigit(nchar))
            {
                /*
                 * "z123{nchar}": edit the count before obtaining {nchar}
                 */
                if (checkclearop(cap.oap))
                    return;

                for (long n = nchar - '0'; ; )
                {
                    no_mapping++;
                    allow_keys++;   /* no mapping for nchar, but allow key codes */
                    nchar = plain_vgetc();
                    --no_mapping;
                    --allow_keys;

                    add_to_showcmd(nchar);

                    if (nchar == K_DEL || nchar == K_KDEL)
                        n /= 10;
                    else if (asc_isdigit(nchar))
                        n = n * 10 + (nchar - '0');
                    else if (nchar == CAR)
                    {
                        win_setheight((int)n);
                        break;
                    }
                    else if (nchar == 'l' || nchar == 'h' || nchar == K_LEFT || nchar == K_RIGHT)
                    {
                        cap.count1 = (n != 0) ? n * cap.count1 : cap.count1;
                        break dozet;
                    }
                    else
                    {
                        clearopbeep(cap.oap);
                        break;
                    }
                }

                cap.oap.op_type = OP_NOP;
                return;
            }

            if (checkclearop(cap.oap))
                return;

            /*
             * For "z+", "z<CR>", "zt", "z.", "zz", "z^", "z-", "zb":
             * If line number given, set cursor.
             */
            if (vim_strchr(u8("+\r\nt.z^-b"), nchar) != null && cap.count0 != 0 && cap.count0 != curwin.w_cursor.lnum)
            {
                setpcmark();
                if (curbuf.b_ml.ml_line_count < cap.count0)
                    curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
                else
                    curwin.w_cursor.lnum = cap.count0;
                check_cursor_col();
            }

            switch (nchar)
            {
                case '+':   /* "z+", "z<CR>" and "zt": put cursor at top of screen */
                    if (cap.count0 == 0)
                    {
                        /* No count given: put cursor at the line below screen. */
                        validate_botline(); /* make sure w_botline is valid */
                        if (curbuf.b_ml.ml_line_count < curwin.w_botline)
                            curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
                        else
                            curwin.w_cursor.lnum = curwin.w_botline;
                    }
                    /* FALLTHROUGH */

                case NL:
                case CAR:
                case K_KENTER:
                    beginline(BL_WHITE | BL_FIX);
                    /* FALLTHROUGH */

                case 't':
                    scroll_cursor_top(0, true);
                    redraw_later(VALID);
                    break;

                case '.':   /* "z." and "zz": put cursor in middle of screen */
                    beginline(BL_WHITE | BL_FIX);
                    /* FALLTHROUGH */

                case 'z':
                    scroll_cursor_halfway(true);
                    redraw_later(VALID);
                    break;

                case '^':   /* "z^", "z-" and "zb": put cursor at bottom of screen */
                    /* Strange Vi behavior:
                     * <count>z^ finds line at top of window when <count> is at bottom of window,
                     * and puts that one at bottom of window. */
                    if (cap.count0 != 0)
                    {
                        scroll_cursor_bot(0, true);
                        curwin.w_cursor.lnum = curwin.w_topline;
                    }
                    else if (curwin.w_topline == 1)
                        curwin.w_cursor.lnum = 1;
                    else
                        curwin.w_cursor.lnum = curwin.w_topline - 1;
                    /* FALLTHROUGH */

                case '-':
                    beginline(BL_WHITE | BL_FIX);
                    /* FALLTHROUGH */

                case 'b':
                    scroll_cursor_bot(0, true);
                    redraw_later(VALID);
                    break;

                case 'H':   /* "zH" - scroll screen right half-page */
                    cap.count1 *= curwin.w_width / 2;
                    /* FALLTHROUGH */

                case 'h':   /* "zh" - scroll screen to the right */
                case K_LEFT:
                    if (!curwin.w_onebuf_opt.wo_wrap[0])
                    {
                        if (curwin.w_leftcol < (int)cap.count1)
                            curwin.w_leftcol = 0;
                        else
                            curwin.w_leftcol -= (int)cap.count1;
                        leftcol_changed();
                    }
                    break;

                case 'L':   /* "zL" - scroll screen left half-page */
                    cap.count1 *= curwin.w_width / 2;
                    /* FALLTHROUGH */

                case 'l':   /* "zl" - scroll screen to the left */
                case K_RIGHT:
                    if (!curwin.w_onebuf_opt.wo_wrap[0])
                    {
                        /* scroll the window left */
                        curwin.w_leftcol += (int)cap.count1;
                        leftcol_changed();
                    }
                    break;

                case 's':   /* "zs" - scroll screen, cursor at the start */
                    if (!curwin.w_onebuf_opt.wo_wrap[0])
                    {
                        int[] col = new int[1];
                        getvcol(curwin, curwin.w_cursor, col, null, null);
                        if (p_siso[0] < (long)col[0])
                            col[0] -= p_siso[0];
                        else
                            col[0] = 0;
                        if (curwin.w_leftcol != col[0])
                        {
                            curwin.w_leftcol = col[0];
                            redraw_later(NOT_VALID);
                        }
                    }
                    break;

                case 'e':   /* "ze" - scroll screen, cursor at the end */
                    if (!curwin.w_onebuf_opt.wo_wrap[0])
                    {
                        int[] col = new int[1];
                        getvcol(curwin, curwin.w_cursor, null, null, col);
                        long n = curwin.w_width - curwin_col_off();
                        if ((long)col[0] + p_siso[0] < n)
                            col[0] = 0;
                        else
                            col[0] += p_siso[0] - n + 1;
                        if (curwin.w_leftcol != col[0])
                        {
                            curwin.w_leftcol = col[0];
                            redraw_later(NOT_VALID);
                        }
                    }
                    break;

                default:
                    clearopbeep(cap.oap);
                    break;
            }
        }
    };

    /*
     * "Q" command.
     */
    /*private*/ static final nv_func_C nv_exmode = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            /*
             * Ignore 'Q' in Visual mode, just give a beep.
             */
            if (VIsual_active)
                vim_beep();
            else if (!checkclearop(cap.oap))
                do_exmode(false);
        }
    };

    /*
     * Handle a ":" command.
     */
    /*private*/ static final nv_func_C nv_colon = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (VIsual_active)
                nv_operator.nv(cap);
            else
            {
                if (cap.oap.op_type != OP_NOP)
                {
                    /* Using ":" as a movement is characterwise exclusive. */
                    cap.oap.motion_type = MCHAR;
                    cap.oap.inclusive = false;
                }
                else if (cap.count0 != 0)
                {
                    /* translate "count:" into ":.,.+(count - 1)" */
                    stuffcharReadbuff('.');
                    if (1 < cap.count0)
                    {
                        stuffReadbuff(u8(",.+"));
                        stuffnumReadbuff(cap.count0 - 1);
                    }
                }

                /* When typing, don't type below an old message. */
                if (keyTyped)
                    compute_cmdrow();

                boolean old_p_im = p_im[0];

                /* get a command line and execute it */
                boolean cmd_result = do_cmdline(null, getexline, null, (cap.oap.op_type != OP_NOP) ? DOCMD_KEEPLINE : 0);

                /* If 'insertmode' changed, enter or exit Insert mode. */
                if (p_im[0] != old_p_im)
                {
                    if (p_im[0])
                        restart_edit = 'i';
                    else
                        restart_edit = 0;
                }

                if (cmd_result == false)
                    /* The Ex command failed, do not execute the operator. */
                    clearop(cap.oap);
                else if (cap.oap.op_type != OP_NOP
                        && (curbuf.b_ml.ml_line_count < cap.oap.op_start.lnum
                            || strlen(ml_get(cap.oap.op_start.lnum)) < cap.oap.op_start.col
                            || did_emsg))
                    /* The start of the operator has become invalid by the Ex command. */
                    clearopbeep(cap.oap);
            }
        }
    };

    /*
     * Handle CTRL-G command.
     */
    /*private*/ static final nv_func_C nv_ctrlg = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (VIsual_active)  /* toggle Selection/Visual mode */
            {
                VIsual_select = !VIsual_select;
                showmode();
            }
            else if (!checkclearop(cap.oap))
                /* print full name if count given or :cd used */
                fileinfo((int)cap.count0, true);
        }
    };

    /*
     * Handle CTRL-H <Backspace> command.
     */
    /*private*/ static final nv_func_C nv_ctrlh = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (VIsual_active && VIsual_select)
            {
                cap.cmdchar = 'x';  /* BS key behaves like 'x' in Select mode */
                v_visop(cap);
            }
            else
                nv_left.nv(cap);
        }
    };

    /*
     * CTRL-L: clear screen and redraw.
     */
    /*private*/ static final nv_func_C nv_clear = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (!checkclearop(cap.oap))
            {
                /* Clear all syntax states to force resyncing. */
                syn_stack_free_all(curwin.w_s);
                redraw_later(CLEAR);
            }
        }
    };

    /*
     * CTRL-O: In Select mode: switch to Visual mode for one command.
     * Otherwise: Go to older pcmark.
     */
    /*private*/ static final nv_func_C nv_ctrlo = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (VIsual_active && VIsual_select)
            {
                VIsual_select = false;
                showmode();
                restart_VIsual_select = 2;      /* restart Select mode later */
            }
            else
            {
                cap.count1 = -cap.count1;
                nv_pcmark.nv(cap);
            }
        }
    };

    /*
     * CTRL-^ command, short for ":e #"
     */
    /*private*/ static final nv_func_C nv_hat = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (!checkclearopq(cap.oap))
                buflist_getfile((int)cap.count0, 0, GETF_SETMARK|GETF_ALT, false);
        }
    };

    /*
     * "Z" commands.
     */
    /*private*/ static final nv_func_C nv_Zet = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (!checkclearopq(cap.oap))
            {
                switch (cap.nchar[0])
                {
                                /* "ZZ": equivalent to ":x". */
                    case 'Z':   do_cmdline_cmd(u8("x"));
                                break;

                                /* "ZQ": equivalent to ":q!" (Elvis compatible). */
                    case 'Q':   do_cmdline_cmd(u8("q!"));
                                break;

                    default:    clearopbeep(cap.oap);
                }
            }
        }
    };

    /*
     * Call nv_ident() as if "c1" was used, with "c2" as next character.
     */
    /*private*/ static void do_nv_ident(int c1, int c2)
    {
        oparg_C oa = new oparg_C();
        cmdarg_C ca = new cmdarg_C();

        ca.oap = oa;
        ca.cmdchar = c1;
        ca.nchar[0] = c2;
        nv_ident.nv(ca);
    }

    /*
     * Handle the commands that use the word under the cursor.
     * [g] CTRL-]   :ta to current identifier
     * [g] 'K'      run program for current identifier
     * [g] '*'      / to current identifier or string
     * [g] '#'      ? to current identifier or string
     *  g  ']'      :tselect for current identifier
     */
    /*private*/ static final nv_func_C nv_ident = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            int cmdchar = cap.cmdchar;

            boolean g_cmd = (cmdchar == 'g');
            if (g_cmd)                          /* "g*", "g#", "g]" and "gCTRL-]" */
                cmdchar = cap.nchar[0];

            if (cmdchar == char_u(POUND))       /* the pound sign, '#' for English keyboards */
                cmdchar = '#';

            Bytes[] ident = { null };
            int[] n = { 0 };

            /*
             * The "]", "CTRL-]" and "K" commands accept an argument in Visual mode.
             */
            if (cmdchar == ']' || cmdchar == Ctrl_RSB || cmdchar == 'K')
            {
                if (VIsual_active && get_visual_text(cap, ident, n) == false)
                    return;
                if (checkclearopq(cap.oap))
                    return;
            }

            if (ident[0] == null)
            {
                int type = (cmdchar == '*' || cmdchar == '#') ? FIND_IDENT|FIND_STRING : FIND_IDENT;
                n[0] = find_ident_under_cursor(ident, type);
                if (n[0] == 0)
                {
                    clearop(cap.oap);
                    return;
                }
            }

            /* Allocate buffer to put the command in.
             * Inserting backslashes can double the length of the word.
             * "p_kp" / "curbuf.b_p_kp" could be added and some numbers.
             */
            Bytes kp = (curbuf.b_p_kp[0].at(0) == NUL) ? p_kp[0] : curbuf.b_p_kp[0]; /* value of 'keywordprg' */

            Bytes buf = new Bytes(n[0] * 2 + 30 + strlen(kp));
            buf.be(0, NUL);

            boolean tag_cmd = false;

            switch (cmdchar)
            {
                case '*':
                case '#':
                {
                    /* Put cursor at start of word, makes search skip the word under the cursor.
                     * Call setpcmark() first, so "*``" puts the cursor back where it was.
                     */
                    setpcmark();
                    curwin.w_cursor.col = BDIFF(ident[0], ml_get_curline());

                    if (!g_cmd && us_iswordp(ident[0], curbuf))
                        STRCPY(buf, u8("\\<"));
                    no_smartcase = true;        /* don't use 'smartcase' now */
                    break;
                }

                case 'K':
                {
                    /* An external command will probably use an argument starting
                     * with "-" as an option.  To avoid trouble we skip the "-".
                     */
                    for ( ; 0 < n[0] && ident[0].at(0) == (byte)'-'; --n[0])
                        ident[0] = ident[0].plus(1);
                    if (n[0] == 0)
                    {
                        emsg(e_noident);        /* found dashes only */
                        return;
                    }

                    /* When a count is given, turn it into a range.  Is this really what we want? */
                    if (cap.count0 != 0)
                        libC.sprintf(buf, u8(".,.+%ld"), cap.count0 - 1);

                    if (kp.at(0) != (byte)':')
                        STRCAT(buf, u8("!"));
                    STRCAT(buf, (kp.at(0) != (byte)':') ? kp : kp.plus(1));
                    STRCAT(buf, u8(" "));
                    break;
                }

                case ']':
                {
                    tag_cmd = true;
                    STRCPY(buf, u8("ts "));
                    break;
                }

                default:
                {
                    tag_cmd = true;
                    if (g_cmd)
                        STRCPY(buf, u8("tj "));
                    else
                        libC.sprintf(buf, u8("%ldta "), cap.count0);
                    break;
                }
            }

            /*
             * Now grab the chars in the identifier
             */
            Bytes aux;
            if (cmdchar == '*')
                aux = p_magic[0] ? u8("/.*~[^$\\") : u8("/^$\\");
            else if (cmdchar == '#')
                aux = p_magic[0] ? u8("/?.*~[^$\\") : u8("/?^$\\");
            else if (tag_cmd)
                aux = u8("\\|\"\n[");
            else
                aux = u8("\\|\"\n*?[");

            Bytes p = buf.plus(strlen(buf));
            while (0 < n[0]--)
            {
                /* put a backslash before \ and some others */
                if (vim_strchr(aux, ident[0].at(0)) != null)
                    (p = p.plus(1)).be(-1, (byte)'\\');

                /* When current byte is part of multibyte character, copy all bytes of the character. */
                for (int i = 0, len = us_ptr2len_cc(ident[0]) - 1; i < len && 1 <= n[0]; ++i, --n[0])
                    (p = p.plus(1)).be(-1, (ident[0] = ident[0].plus(1)).at(-1));

                (p = p.plus(1)).be(-1, (ident[0] = ident[0].plus(1)).at(-1));
            }
            p.be(0, NUL);

            /*
             * Execute the command.
             */
            if (cmdchar == '*' || cmdchar == '#')
            {
                if (!g_cmd && us_iswordp(us_prevptr(ml_get_curline(), ident[0]), curbuf))
                    STRCAT(buf, u8("\\>"));
                /* put pattern in search history */
                init_history();
                add_to_history(HIST_SEARCH, buf, true, NUL);
                normal_search(cap, (cmdchar == '*') ? (byte)'/' : (byte)'?', buf, 0);
            }
            else
                do_cmdline_cmd(buf);
        }
    };

    /*
     * Get visually selected text, within one line only.
     * Returns false if more than one line selected.
     */
    /*private*/ static boolean get_visual_text(cmdarg_C cap, Bytes[] pp, int[] lenp)
        /* pp: return: start of selected text */
        /* lenp: return: length of selected text */
    {
        if (VIsual_mode != 'V')
            unadjust_for_sel();
        if (VIsual.lnum != curwin.w_cursor.lnum)
        {
            if (cap != null)
                clearopbeep(cap.oap);
            return false;
        }
        if (VIsual_mode == 'V')
        {
            pp[0] = ml_get_curline();
            lenp[0] = strlen(pp[0]);
        }
        else
        {
            if (ltpos(curwin.w_cursor, VIsual))
            {
                pp[0] = ml_get_pos(curwin.w_cursor);
                lenp[0] = VIsual.col - curwin.w_cursor.col + 1;
            }
            else
            {
                pp[0] = ml_get_pos(VIsual);
                lenp[0] = curwin.w_cursor.col - VIsual.col + 1;
            }

            /* Correct the length to include the whole last character. */
            lenp[0] += us_ptr2len_cc(pp[0].plus(lenp[0] - 1)) - 1;
        }
        reset_VIsual_and_resel();
        return true;
    }

    /*
     * Handle scrolling command 'H', 'L' and 'M'.
     */
    /*private*/ static final nv_func_C nv_scroll = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            cap.oap.motion_type = MLINE;
            setpcmark();

            if (cap.cmdchar == 'L')
            {
                validate_botline();         /* make sure curwin.w_botline is valid */
                curwin.w_cursor.lnum = curwin.w_botline - 1;
                if (curwin.w_cursor.lnum <= cap.count1 - 1)
                    curwin.w_cursor.lnum = 1;
                else
                {
                    curwin.w_cursor.lnum -= cap.count1 - 1;
                }
            }
            else
            {
                long n;
                if (cap.cmdchar == 'M')
                {
                    validate_botline();     /* make sure w_empty_rows is valid */

                    int half = (curwin.w_height - curwin.w_empty_rows + 1) / 2;
                    int used = 0;
                    for (n = 0; curwin.w_topline + n < curbuf.b_ml.ml_line_count; n++)
                    {
                        used += plines(curwin.w_topline + n);
                        if (half <= used)
                            break;
                    }
                    if (0 < n && curwin.w_height < used)
                        --n;
                }
                else /* (cap.cmdchar == 'H') */
                {
                    n = cap.count1 - 1;
                }
                curwin.w_cursor.lnum = curwin.w_topline + n;
                if (curwin.w_cursor.lnum > curbuf.b_ml.ml_line_count)
                    curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
            }

            cursor_correct();   /* correct for 'so' */
            beginline(BL_SOL | BL_FIX);
        }
    };

    /*
     * Cursor right commands.
     */
    /*private*/ static final nv_func_C nv_right = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if ((mod_mask & (MOD_MASK_SHIFT | MOD_MASK_CTRL)) != 0)
            {
                /* <C-Right> and <S-Right> move a word or WORD right */
                if ((mod_mask & MOD_MASK_CTRL) != 0)
                    cap.arg = TRUE;
                nv_wordcmd.nv(cap);
                return;
            }

            cap.oap.motion_type = MCHAR;
            cap.oap.inclusive = false;
            boolean past_line = (VIsual_active && p_sel[0].at(0) != (byte)'o');

            /*
             * In virtual edit mode, there's no such thing as "past_line",
             * as lines are (theoretically) infinitely long.
             */
            if (virtual_active())
                past_line = false;

            for (long n = cap.count1; 0 < n; --n)
            {
                if ((!past_line && oneright() == false) || (past_line && ml_get_cursor().at(0) == NUL))
                {
                    /*
                     *    <Space> wraps to next line if 'whichwrap' has 's'.
                     *        'l' wraps to next line if 'whichwrap' has 'l'.
                     * CURS_RIGHT wraps to next line if 'whichwrap' has '>'.
                     */
                    if (((cap.cmdchar == ' ' && vim_strchr(p_ww[0], 's') != null)
                        || (cap.cmdchar == 'l' && vim_strchr(p_ww[0], 'l') != null)
                        || (cap.cmdchar == K_RIGHT && vim_strchr(p_ww[0], '>') != null))
                            && curwin.w_cursor.lnum < curbuf.b_ml.ml_line_count)
                    {
                        /* When deleting we also count the NL as a character.
                         * Set cap.oap.inclusive when last char in the line is
                         * included, move to next line after that */
                        if (cap.oap.op_type != OP_NOP
                                && !cap.oap.inclusive
                                && !lineempty(curwin.w_cursor.lnum))
                            cap.oap.inclusive = true;
                        else
                        {
                            curwin.w_cursor.lnum++;
                            curwin.w_cursor.col = 0;
                            curwin.w_cursor.coladd = 0;
                            curwin.w_set_curswant = true;
                            cap.oap.inclusive = false;
                        }
                        continue;
                    }
                    if (cap.oap.op_type == OP_NOP)
                    {
                        /* Only beep and flush if not moved at all. */
                        if (n == cap.count1)
                            beep_flush();
                    }
                    else
                    {
                        if (!lineempty(curwin.w_cursor.lnum))
                            cap.oap.inclusive = true;
                    }
                    break;
                }
                else if (past_line)
                {
                    curwin.w_set_curswant = true;
                    if (virtual_active())
                        oneright();
                    else
                        curwin.w_cursor.col += us_ptr2len_cc(ml_get_cursor());
                }
            }
        }
    };

    /*
     * Cursor left commands.
     *
     * Returns true when operator end should not be adjusted.
     */
    /*private*/ static final nv_func_C nv_left = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if ((mod_mask & (MOD_MASK_SHIFT | MOD_MASK_CTRL)) != 0)
            {
                /* <C-Left> and <S-Left> move a word or WORD left */
                if ((mod_mask & MOD_MASK_CTRL) != 0)
                    cap.arg = 1;
                nv_bck_word.nv(cap);
                return;
            }

            cap.oap.motion_type = MCHAR;
            cap.oap.inclusive = false;

            for (long n = cap.count1; 0 < n; --n)
            {
                if (oneleft() == false)
                {
                    /* <BS> and <Del> wrap to previous line if 'whichwrap' has 'b'.
                     *           'h' wraps to previous line if 'whichwrap' has 'h'.
                     *     CURS_LEFT wraps to previous line if 'whichwrap' has '<'.
                     */
                    if ((((cap.cmdchar == K_BS || cap.cmdchar == Ctrl_H) && vim_strchr(p_ww[0], 'b') != null)
                        || (cap.cmdchar == 'h' && vim_strchr(p_ww[0], 'h') != null)
                        || (cap.cmdchar == K_LEFT && vim_strchr(p_ww[0], '<') != null))
                            && 1 < curwin.w_cursor.lnum)
                    {
                        --curwin.w_cursor.lnum;
                        coladvance(MAXCOL);
                        curwin.w_set_curswant = true;

                        /* When the NL before the first char has to be deleted we
                         * put the cursor on the NUL after the previous line.
                         * This is a very special case, be careful!
                         * Don't adjust op_end now, otherwise it won't work. */
                        if ((cap.oap.op_type == OP_DELETE || cap.oap.op_type == OP_CHANGE)
                                && !lineempty(curwin.w_cursor.lnum))
                        {
                            Bytes cp = ml_get_cursor();

                            if (cp.at(0) != NUL)
                                curwin.w_cursor.col += us_ptr2len_cc(cp);
                            cap.retval |= CA_NO_ADJ_OP_END;
                        }
                        continue;
                    }
                    /* Only beep and flush if not moved at all. */
                    else if (cap.oap.op_type == OP_NOP && n == cap.count1)
                        beep_flush();
                    break;
                }
            }
        }
    };

    /*
     * Cursor up commands.
     * cap.arg is TRUE for "-": Move cursor to first non-blank.
     */
    /*private*/ static final nv_func_C nv_up = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if ((mod_mask & MOD_MASK_SHIFT) != 0)
            {
                /* <S-Up> is page up */
                cap.arg = BACKWARD;
                nv_page.nv(cap);
            }
            else
            {
                cap.oap.motion_type = MLINE;
                if (cursor_up(cap.count1, cap.oap.op_type == OP_NOP) == false)
                    clearopbeep(cap.oap);
                else if (cap.arg != 0)
                    beginline(BL_WHITE | BL_FIX);
            }
        }
    };

    /*
     * Cursor down commands.
     * cap.arg is TRUE for CR and "+": Move cursor to first non-blank.
     */
    /*private*/ static final nv_func_C nv_down = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if ((mod_mask & MOD_MASK_SHIFT) != 0)
            {
                /* <S-Down> is page down */
                cap.arg = FORWARD;
                nv_page.nv(cap);
            }
            else
            {
                /* In the cmdline window a <CR> executes the command. */
                if (cmdwin_type != 0 && cap.cmdchar == CAR)
                    cmdwin_result = CAR;
                else
                {
                    cap.oap.motion_type = MLINE;
                    if (cursor_down(cap.count1, cap.oap.op_type == OP_NOP) == false)
                        clearopbeep(cap.oap);
                    else if (cap.arg != 0)
                        beginline(BL_WHITE | BL_FIX);
                }
            }
        }
    };

    /*
     * <End> command: to end of current line or last line.
     */
    /*private*/ static final nv_func_C nv_end = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (cap.arg != 0 || (mod_mask & MOD_MASK_CTRL) != 0) /* CTRL-END = goto last line */
            {
                cap.arg = TRUE;
                nv_goto.nv(cap);
                cap.count1 = 1;             /* to end of current line */
            }
            nv_dollar.nv(cap);
        }
    };

    /*
     * Handle the "$" command.
     */
    /*private*/ static final nv_func_C nv_dollar = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            cap.oap.motion_type = MCHAR;
            cap.oap.inclusive = true;
            /* In virtual mode when off the edge of a line and an operator
             * is pending (whew!) keep the cursor where it is.
             * Otherwise, send it to the end of the line. */
            if (!virtual_active() || gchar_cursor() != NUL || cap.oap.op_type == OP_NOP)
                curwin.w_curswant = MAXCOL;     /* so we stay at the end */
            if (cursor_down(cap.count1 - 1, cap.oap.op_type == OP_NOP) == false)
                clearopbeep(cap.oap);
        }
    };

    /*
     * Implementation of '?' and '/' commands.
     * If cap.arg is TRUE, don't set PC mark.
     */
    /*private*/ static final nv_func_C nv_search = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            oparg_C oap = cap.oap;

            if (cap.cmdchar == '?' && cap.oap.op_type == OP_ROT13)
            {
                /* Translate "g??" to "g?g?". */
                cap.cmdchar = 'g';
                cap.nchar[0] = '?';
                nv_operator.nv(cap);
                return;
            }

            cap.searchbuf = getcmdline(cap.cmdchar, cap.count1, 0);

            if (cap.searchbuf == null)
            {
                clearop(oap);
                return;
            }

            normal_search(cap, (byte)cap.cmdchar, cap.searchbuf, (cap.arg != 0) ? 0 : SEARCH_MARK);
        }
    };

    /*
     * Handle "N" and "n" commands.
     * cap.arg is SEARCH_REV for "N", 0 for "n".
     */
    /*private*/ static final nv_func_C nv_next = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            pos_C old = new pos_C();
            COPY_pos(old, curwin.w_cursor);
            int i = normal_search(cap, NUL, null, SEARCH_MARK | cap.arg);

            if (i == 1 && eqpos(old, curwin.w_cursor))
            {
                /* Avoid getting stuck on the current cursor position, which can
                 * happen when an offset is given and the cursor is on the last char
                 * in the buffer: Repeat with count + 1. */
                cap.count1 += 1;
                normal_search(cap, NUL, null, SEARCH_MARK | cap.arg);
                cap.count1 -= 1;
            }
        }
    };

    /*
     * Search for "pat" in direction "dirc" ('/' or '?', 0 for repeat).
     * Uses only cap.count1 and cap.oap from "cap".
     * Return 0 for failure, 1 for found, 2 for found and line offset added.
     */
    /*private*/ static int normal_search(cmdarg_C cap, byte dirc, Bytes pat, int opt)
        /* opt: extra flags for do_search() */
    {
        cap.oap.motion_type = MCHAR;
        cap.oap.inclusive = false;
        cap.oap.use_reg_one = true;
        curwin.w_set_curswant = true;

        int i = do_search(cap.oap, dirc, pat, cap.count1, opt | SEARCH_OPT | SEARCH_ECHO | SEARCH_MSG, null);
        if (i == 0)
            clearop(cap.oap);
        else
        {
            if (i == 2)
                cap.oap.motion_type = MLINE;
            curwin.w_cursor.coladd = 0;
        }

        /* "/$" will put the cursor after the end of the line, may need to correct that here */
        check_cursor();
        return i;
    }

    /*
     * Character search commands.
     * cap.arg is BACKWARD for 'F' and 'T', FORWARD for 'f' and 't', TRUE for ',' and FALSE for ';'.
     * cap.nchar is NUL for ',' and ';' (repeat the search).
     */
    /*private*/ static final nv_func_C nv_csearch = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            boolean t_cmd = (cap.cmdchar == 't' || cap.cmdchar == 'T');

            cap.oap.motion_type = MCHAR;
            if (is_special(cap.nchar[0]) || searchc(cap, t_cmd) == false)
                clearopbeep(cap.oap);
            else
            {
                curwin.w_set_curswant = true;
                /* Include a Tab for "tx" and for "dfx". */
                if (gchar_cursor() == TAB && virtual_active() && cap.arg == FORWARD
                        && (t_cmd || cap.oap.op_type != OP_NOP))
                {
                    int[] scol = new int[1];
                    int[] ecol = new int[1];

                    getvcol(curwin, curwin.w_cursor, scol, null, ecol);
                    curwin.w_cursor.coladd = ecol[0] - scol[0];
                }
                else
                    curwin.w_cursor.coladd = 0;
                adjust_for_sel(cap);
            }
        }
    };

    /*
     * "[" and "]" commands.
     * cap.arg is BACKWARD for "[" and FORWARD for "]".
     */
    /*private*/ static final nv_func_C nv_brackets = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            cap.oap.motion_type = MCHAR;
            cap.oap.inclusive = false;
            pos_C old_pos = new pos_C();
            COPY_pos(old_pos, curwin.w_cursor);     /* cursor position before command */
            curwin.w_cursor.coladd = 0;             /* TODO: don't do this for an error */

            pos_C prev_pos = new pos_C();

            /*
             * "[{", "[(", "]}" or "])": go to Nth unclosed '{', '(', '}' or ')'
             * "[#", "]#": go to start/end of Nth innermost #if..#endif construct.
             * "[/", "[*", "]/", "]*": go to Nth comment start/end.
             * "[m" or "]m" search for prev/next start of (Java) method.
             * "[M" or "]M" search for prev/next end of (Java) method.
             */
            if ((cap.cmdchar == '[' && vim_strchr(u8("{(*/#mM"), cap.nchar[0]) != null)
             || (cap.cmdchar == ']' && vim_strchr(u8("})*/#mM"), cap.nchar[0]) != null))
            {
                if (cap.nchar[0] == '*')
                    cap.nchar[0] = '/';

                int findc;
                long n;
                if (cap.nchar[0] == 'm' || cap.nchar[0] == 'M')
                {
                    if (cap.cmdchar == '[')
                        findc = '{';
                    else
                        findc = '}';
                    n = 9999;
                }
                else
                {
                    findc = cap.nchar[0];
                    n = cap.count1;
                }

                pos_C pos = null;
                prev_pos.lnum = 0;
                pos_C new_pos = new pos_C();

                for ( ; 0 < n; --n)
                {
                    pos = findmatchlimit(cap.oap, findc, (cap.cmdchar == '[') ? FM_BACKWARD : FM_FORWARD, 0);
                    if (pos == null)
                    {
                        if (new_pos.lnum == 0) /* nothing found */
                        {
                            if (cap.nchar[0] != 'm' && cap.nchar[0] != 'M')
                                clearopbeep(cap.oap);
                        }
                        else
                            pos = new_pos;      /* use last one found */
                        break;
                    }
                    COPY_pos(prev_pos, new_pos);
                    COPY_pos(curwin.w_cursor, pos);
                    COPY_pos(new_pos, pos);
                }
                COPY_pos(curwin.w_cursor, old_pos);

                /*
                 * Handle "[m", "]m", "[M" and "[M".  The findmatchlimit() only
                 * brought us to the match for "[m" and "]M" when inside a method.
                 * Try finding the '{' or '}' we want to be at.
                 * Also repeat for the given count.
                 */
                if (cap.nchar[0] == 'm' || cap.nchar[0] == 'M')
                {
                    /* norm is true for "]M" and "[m" */
                    boolean norm = ((findc == '{') == (cap.nchar[0] == 'm'));

                    n = cap.count1;
                    /* found a match: we were inside a method */
                    if (prev_pos.lnum != 0)
                    {
                        pos = prev_pos;
                        COPY_pos(curwin.w_cursor, prev_pos);
                        if (norm)
                            --n;
                    }
                    else
                        pos = null;
                    for ( ; 0 < n; --n)
                    {
                        for ( ; ; )
                        {
                            if ((findc == '{' ? dec_cursor() : inc_cursor()) < 0)
                            {
                                /* if not found anything, that's an error */
                                if (pos == null)
                                    clearopbeep(cap.oap);
                                n = 0;
                                break;
                            }
                            int c = gchar_cursor();
                            if (c == '{' || c == '}')
                            {
                                /* Must have found end/start of class: use it.
                                 * Or found the place to be at. */
                                if ((c == findc && norm) || (n == 1 && !norm))
                                {
                                    COPY_pos(new_pos, curwin.w_cursor);
                                    pos = new_pos;
                                    n = 0;
                                }
                                /* If no match found at all, we started outside of the class
                                 * and we're inside now.  Just go on. */
                                else if (new_pos.lnum == 0)
                                {
                                    COPY_pos(new_pos, curwin.w_cursor);
                                    pos = new_pos;
                                }
                                /* found start/end of other method: go to match */
                                else if ((pos = findmatchlimit(cap.oap, findc,
                                    (cap.cmdchar == '[') ? FM_BACKWARD : FM_FORWARD, 0)) == null)
                                    n = 0;
                                else
                                    COPY_pos(curwin.w_cursor, pos);
                                break;
                            }
                        }
                    }
                    COPY_pos(curwin.w_cursor, old_pos);
                    if (pos == null && new_pos.lnum != 0)
                        clearopbeep(cap.oap);
                }
                if (pos != null)
                {
                    setpcmark();
                    COPY_pos(curwin.w_cursor, pos);
                    curwin.w_set_curswant = true;
                }
            }

            /*
             * "[[", "[]", "]]" and "][": move to start or end of function
             */
            else if (cap.nchar[0] == '[' || cap.nchar[0] == ']')
            {
                int flag;
                if (cap.nchar[0] == cap.cmdchar)   /* "]]" or "[[" */
                    flag = '{';
                else
                    flag = '}';                 /* "][" or "[]" */

                curwin.w_set_curswant = true;
                /*
                 * Imitate strange Vi behaviour: When using "]]" with an operator we also stop at '}'.
                 */
                boolean b;
                { boolean[] __ = { cap.oap.inclusive }; b = findpar(__, cap.arg, cap.count1, flag, (cap.oap.op_type != OP_NOP && cap.arg == FORWARD && flag == '{')); cap.oap.inclusive = __[0]; }
                if (!b)
                    clearopbeep(cap.oap);
                else
                {
                    if (cap.oap.op_type == OP_NOP)
                        beginline(BL_WHITE | BL_FIX);
                }
            }

            /*
             * "[p", "[P", "]P" and "]p": put with indent adjustment
             */
            else if (cap.nchar[0] == 'p' || cap.nchar[0] == 'P')
            {
                if (!checkclearop(cap.oap))
                {
                    int dir = (cap.cmdchar == ']' && cap.nchar[0] == 'p') ? FORWARD : BACKWARD;
                    int regname = cap.oap.regname;
                    boolean was_visual = VIsual_active;
                    long line_count = curbuf.b_ml.ml_line_count;

                    pos_C start = new pos_C();
                    pos_C end = new pos_C();
                    if (VIsual_active)
                    {
                        COPY_pos(start, ltoreq(VIsual, curwin.w_cursor) ? VIsual : curwin.w_cursor);
                        COPY_pos(end, eqpos(start, VIsual) ? curwin.w_cursor : VIsual);
                        COPY_pos(curwin.w_cursor, (dir == BACKWARD) ? start : end);
                    }

                    regname = adjust_clip_reg(regname);
                    prep_redo_cmd(cap);

                    do_put(regname, dir, (int)cap.count1, PUT_FIXINDENT);

                    if (was_visual)
                    {
                        COPY_pos(VIsual, start);
                        COPY_pos(curwin.w_cursor, end);
                        if (dir == BACKWARD)
                        {
                            /* adjust lines */
                            VIsual.lnum += curbuf.b_ml.ml_line_count - line_count;
                            curwin.w_cursor.lnum += curbuf.b_ml.ml_line_count - line_count;
                        }

                        VIsual_active = true;
                        if (VIsual_mode == 'V')
                        {
                            /* delete visually selected lines */
                            cap.cmdchar = 'd';
                            cap.nchar[0] = NUL;
                            cap.oap.regname = regname;
                            nv_operator.nv(cap);
                            do_pending_operator(cap, 0, false);
                        }
                        if (VIsual_active)
                        {
                            end_visual_mode();
                            redraw_later(SOME_VALID);
                        }
                    }
                }
            }

            /*
             * "['", "[`", "]'" and "]`": jump to next mark
             */
            else if (cap.nchar[0] == '\'' || cap.nchar[0] == '`')
            {
                pos_C pos = curwin.w_cursor;
                for (long n = cap.count1; 0 < n; --n)
                {
                    COPY_pos(prev_pos, pos);
                    pos = getnextmark(pos, (cap.cmdchar == '[') ? BACKWARD : FORWARD, cap.nchar[0] == '\'');
                    if (pos == null)
                        break;
                }
                if (pos == null)
                    pos = prev_pos;
                nv_cursormark(cap, cap.nchar[0] == '\'', pos);
            }

            /*
             * [ or ] followed by a middle mouse click: put selected text with indent adjustment.
             * Any other button just does as usual.
             */
            else if (K_RIGHTRELEASE <= cap.nchar[0] && cap.nchar[0] <= K_LEFTMOUSE)
            {
                do_mouse(cap.oap, cap.nchar[0], (cap.cmdchar == ']') ? FORWARD : BACKWARD, (int)cap.count1, PUT_FIXINDENT);
            }

            /* Not a valid cap.nchar. */
            else
                clearopbeep(cap.oap);
        }
    };

    /*
     * Handle Normal mode "%" command.
     */
    /*private*/ static final nv_func_C nv_percent = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            cap.oap.inclusive = true;
            if (cap.count0 != 0)                 /* {cnt}% : goto {cnt} percentage in file */
            {
                if (100 < cap.count0)
                    clearopbeep(cap.oap);
                else
                {
                    cap.oap.motion_type = MLINE;
                    setpcmark();
                    /* Round up, so CTRL-G will give same value.
                     * Watch out for a large line count, the line number must not go negative! */
                    if (1000000 < curbuf.b_ml.ml_line_count)
                        curwin.w_cursor.lnum = (curbuf.b_ml.ml_line_count + 99L) / 100L * cap.count0;
                    else
                        curwin.w_cursor.lnum = (curbuf.b_ml.ml_line_count * cap.count0 + 99L) / 100L;
                    if (curwin.w_cursor.lnum > curbuf.b_ml.ml_line_count)
                        curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
                    beginline(BL_SOL | BL_FIX);
                }
            }
            else                            /* "%" : go to matching paren */
            {
                cap.oap.motion_type = MCHAR;
                cap.oap.use_reg_one = true;

                pos_C pos = findmatch(cap.oap, NUL);
                if (pos == null)
                    clearopbeep(cap.oap);
                else
                {
                    setpcmark();
                    COPY_pos(curwin.w_cursor, pos);
                    curwin.w_set_curswant = true;
                    curwin.w_cursor.coladd = 0;
                    adjust_for_sel(cap);
                }
            }
        }
    };

    /*
     * Handle "(" and ")" commands.
     * cap.arg is BACKWARD for "(" and FORWARD for ")".
     */
    /*private*/ static final nv_func_C nv_brace = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            cap.oap.motion_type = MCHAR;
            cap.oap.use_reg_one = true;
            /* The motion used to be inclusive for "(", but that is not what Vi does. */
            cap.oap.inclusive = false;
            curwin.w_set_curswant = true;

            if (findsent(cap.arg, cap.count1) == false)
                clearopbeep(cap.oap);
            else
            {
                /* Don't leave the cursor on the NUL past end of line. */
                adjust_cursor(cap.oap);
                curwin.w_cursor.coladd = 0;
            }
        }
    };

    /*
     * "m" command: Mark a position.
     */
    /*private*/ static final nv_func_C nv_mark = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (!checkclearop(cap.oap))
            {
                if (setmark(cap.nchar[0]) == false)
                    clearopbeep(cap.oap);
            }
        }
    };

    /*
     * "{" and "}" commands.
     * cmd.arg is BACKWARD for "{" and FORWARD for "}".
     */
    /*private*/ static final nv_func_C nv_findpar = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            cap.oap.motion_type = MCHAR;
            cap.oap.inclusive = false;
            cap.oap.use_reg_one = true;
            curwin.w_set_curswant = true;
            boolean b;
            { boolean[] __ = { cap.oap.inclusive }; b = findpar(__, cap.arg, cap.count1, NUL, false); cap.oap.inclusive = __[0]; }
            if (!b)
                clearopbeep(cap.oap);
            else
                curwin.w_cursor.coladd = 0;
        }
    };

    /*
     * "u" command: Undo or make lower case.
     */
    /*private*/ static final nv_func_C nv_undo = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (cap.oap.op_type == OP_LOWER || VIsual_active)
            {
                /* translate "<Visual>u" to "<Visual>gu" and "guu" to "gugu" */
                cap.cmdchar = 'g';
                cap.nchar[0] = 'u';
                nv_operator.nv(cap);
            }
            else
                nv_kundo.nv(cap);
        }
    };

    /*
     * <Undo> command.
     */
    /*private*/ static final nv_func_C nv_kundo = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (!checkclearopq(cap.oap))
            {
                u_undo((int)cap.count1);
                curwin.w_set_curswant = true;
            }
        }
    };

    /*
     * Handle the "r" command.
     */
    /*private*/ static final nv_func_C nv_replace = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (checkclearop(cap.oap))
                return;

            /* get another character */
            int had_ctrl_v;
            if (cap.nchar[0] == Ctrl_V)
            {
                had_ctrl_v = Ctrl_V;
                cap.nchar[0] = get_literal();
                /* Don't redo a multibyte character with CTRL-V. */
                if (DEL < cap.nchar[0])
                    had_ctrl_v = NUL;
            }
            else
                had_ctrl_v = NUL;

            /* Abort if the character is a special key. */
            if (is_special(cap.nchar[0]))
            {
                clearopbeep(cap.oap);
                return;
            }

            /* Visual mode "r". */
            if (VIsual_active)
            {
                if (got_int)
                    reset_VIsual();
                if (had_ctrl_v != NUL)
                {
                    if (cap.nchar[0] == '\r')
                        cap.nchar[0] = -1;
                    else if (cap.nchar[0] == '\n')
                        cap.nchar[0] = -2;
                }
                nv_operator.nv(cap);
                return;
            }

            /* Break tabs, etc. */
            if (virtual_active())
            {
                if (!u_save_cursor())
                    return;
                if (gchar_cursor() == NUL)
                {
                    /* Add extra space and put the cursor on the first one. */
                    coladvance_force((int)(getviscol() + cap.count1));
                    curwin.w_cursor.col -= cap.count1;
                }
                else if (gchar_cursor() == TAB)
                    coladvance_force(getviscol());
            }

            /* Abort if not enough characters to replace. */
            Bytes ptr = ml_get_cursor();
            if (strlen(ptr) < cap.count1 || us_charlen(ptr) < cap.count1)
            {
                clearopbeep(cap.oap);
                return;
            }

            /*
             * Replacing with a TAB is done by edit() when it is complicated because
             * 'expandtab' or 'smarttab' is set.  CTRL-V TAB inserts a literal TAB.
             * Other characters are done below to avoid problems with things like
             * CTRL-V 048 (for edit() this would be R CTRL-V 0 ESC).
             */
            if (had_ctrl_v != Ctrl_V && cap.nchar[0] == '\t' && (curbuf.b_p_et[0] || p_sta[0]))
            {
                stuffnumReadbuff(cap.count1);
                stuffcharReadbuff('R');
                stuffcharReadbuff('\t');
                stuffcharReadbuff(ESC);
                return;
            }

            /* save line for undo */
            if (!u_save_cursor())
                return;

            if (had_ctrl_v != Ctrl_V && (cap.nchar[0] == '\r' || cap.nchar[0] == '\n'))
            {
                /*
                 * Replace character(s) by a single newline.
                 * Strange vi behaviour: Only one newline is inserted.
                 * Delete the characters here.
                 * Insert the newline with an insert command, takes care of
                 * autoindent.  The insert command depends on being on the last
                 * character of a line or not.
                 */
                del_chars((int)cap.count1, false);   /* delete the characters */
                stuffcharReadbuff('\r');
                stuffcharReadbuff(ESC);

                /* Give 'r' to edit(), to get the redo command right. */
                invoke_edit(cap, true, 'r', false);
            }
            else
            {
                prep_redo(cap.oap.regname, cap.count1, NUL, 'r', NUL, had_ctrl_v, cap.nchar[0]);

                COPY_pos(curbuf.b_op_start, curwin.w_cursor);

                {
                    int old_State = State;

                    if (cap.ncharC1 != 0)
                        appendCharToRedobuff(cap.ncharC1);
                    if (cap.ncharC2 != 0)
                        appendCharToRedobuff(cap.ncharC2);

                    /* This is slow, but it handles replacing a single-byte with a
                     * multi-byte and the other way around.  Also handles adding
                     * composing characters for utf-8. */
                    for (long n = cap.count1; 0 < n; --n)
                    {
                        State = REPLACE;
                        if (cap.nchar[0] == Ctrl_E || cap.nchar[0] == Ctrl_Y)
                        {
                            int c = ins_copychar(curwin.w_cursor.lnum + (cap.nchar[0] == Ctrl_Y ? -1 : 1));
                            if (c != NUL)
                                ins_char(c);
                            else
                                /* will be decremented further down */
                                curwin.w_cursor.col++;
                        }
                        else
                            ins_char(cap.nchar[0]);
                        State = old_State;
                        if (cap.ncharC1 != 0)
                            ins_char(cap.ncharC1);
                        if (cap.ncharC2 != 0)
                            ins_char(cap.ncharC2);
                    }
                }

                --curwin.w_cursor.col;      /* cursor on the last replaced char */

                /* If the character on the left of the current cursor
                 * is a multi-byte character, move two characters left. */
                mb_adjust_pos(curbuf, curwin.w_cursor);
                COPY_pos(curbuf.b_op_end, curwin.w_cursor);
                curwin.w_set_curswant = true;
                set_last_insert(cap.nchar[0]);
            }
        }
    };

    /*
     * 'o': Exchange start and end of Visual area.
     * 'O': same, but in block mode exchange left and right corners.
     */
    /*private*/ static void v_swap_corners(int cmdchar)
    {
        if (cmdchar == 'O' && VIsual_mode == Ctrl_V)
        {
            pos_C old_cursor = new pos_C();
            COPY_pos(old_cursor, curwin.w_cursor);
            int[] left = new int[1];
            int[] right = new int[1];
            getvcols(curwin, old_cursor, VIsual, left, right);
            curwin.w_cursor.lnum = VIsual.lnum;
            coladvance(left[0]);
            COPY_pos(VIsual, curwin.w_cursor);

            curwin.w_cursor.lnum = old_cursor.lnum;
            curwin.w_curswant = right[0];
            /* 'selection "exclusive" and cursor at right-bottom corner: move it right one column */
            if (VIsual.lnum <= old_cursor.lnum && p_sel[0].at(0) == (byte)'e')
                curwin.w_curswant++;
            coladvance(curwin.w_curswant);
            if (curwin.w_cursor.col == old_cursor.col
                    && (!virtual_active() || curwin.w_cursor.coladd == old_cursor.coladd))
            {
                curwin.w_cursor.lnum = VIsual.lnum;
                if (old_cursor.lnum <= VIsual.lnum && p_sel[0].at(0) == (byte)'e')
                    right[0]++;
                coladvance(right[0]);
                COPY_pos(VIsual, curwin.w_cursor);

                curwin.w_cursor.lnum = old_cursor.lnum;
                coladvance(left[0]);
                curwin.w_curswant = left[0];
            }
        }
        else
        {
            pos_C old_cursor = new pos_C();
            COPY_pos(old_cursor, curwin.w_cursor);
            COPY_pos(curwin.w_cursor, VIsual);
            COPY_pos(VIsual, old_cursor);
            curwin.w_set_curswant = true;
        }
    }

    /*
     * "R" (cap.arg is FALSE) and "gR" (cap.arg is TRUE).
     */
    /*private*/ static final nv_func_C nv_Replace = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (VIsual_active)          /* "R" is replace lines */
            {
                cap.cmdchar = 'c';
                cap.nchar[0] = NUL;
                VIsual_mode_orig = VIsual_mode; /* remember original area for gv */
                VIsual_mode = 'V';
                nv_operator.nv(cap);
            }
            else if (!checkclearopq(cap.oap))
            {
                if (!curbuf.b_p_ma[0])
                    emsg(e_modifiable);
                else
                {
                    if (virtual_active())
                        coladvance(getviscol());
                    invoke_edit(cap, false, (cap.arg != 0) ? 'V' : 'R', false);
                }
            }
        }
    };

    /*
     * "gr".
     */
    /*private*/ static void nv_vreplace(cmdarg_C cap)
    {
        if (VIsual_active)
        {
            cap.cmdchar = 'r';
            cap.nchar[0] = cap.extra_char[0];
            nv_replace.nv(cap);        /* Do same as "r" in Visual mode for now */
        }
        else if (!checkclearopq(cap.oap))
        {
            if (!curbuf.b_p_ma[0])
                emsg(e_modifiable);
            else
            {
                if (cap.extra_char[0] == Ctrl_V)       /* get another character */
                    cap.extra_char[0] = get_literal();
                stuffcharReadbuff(cap.extra_char[0]);
                stuffcharReadbuff(ESC);
                if (virtual_active())
                    coladvance(getviscol());
                invoke_edit(cap, true, 'v', false);
            }
        }
    }

    /*
     * Swap case for "~" command, when it does not work like an operator.
     */
    /*private*/ static void n_swapchar(cmdarg_C cap)
    {
        if (checkclearopq(cap.oap))
            return;

        if (lineempty(curwin.w_cursor.lnum) && vim_strchr(p_ww[0], '~') == null)
        {
            clearopbeep(cap.oap);
            return;
        }

        prep_redo_cmd(cap);

        if (!u_save_cursor())
            return;

        boolean did_change = false;

        pos_C startpos = new pos_C();
        COPY_pos(startpos, curwin.w_cursor);

        for (long n = cap.count1; 0 < n; --n)
        {
            did_change |= swapchar(cap.oap.op_type, curwin.w_cursor);
            inc_cursor();
            if (gchar_cursor() == NUL)
            {
                if (vim_strchr(p_ww[0], '~') != null && curwin.w_cursor.lnum < curbuf.b_ml.ml_line_count)
                {
                    curwin.w_cursor.lnum++;
                    curwin.w_cursor.col = 0;
                    if (1 < n)
                    {
                        if (u_savesub(curwin.w_cursor.lnum) == false)
                            break;
                        u_clearline();
                    }
                }
                else
                    break;
            }
        }

        check_cursor();
        curwin.w_set_curswant = true;

        if (did_change)
        {
            changed_lines(startpos.lnum, startpos.col, curwin.w_cursor.lnum + 1, 0L);
            COPY_pos(curbuf.b_op_start, startpos);
            COPY_pos(curbuf.b_op_end, curwin.w_cursor);
            if (0 < curbuf.b_op_end.col)
                --curbuf.b_op_end.col;
        }
    }

    /*
     * Move cursor to mark.
     */
    /*private*/ static void nv_cursormark(cmdarg_C cap, boolean flag, pos_C pos)
    {
        if (check_mark(pos) == false)
            clearop(cap.oap);
        else
        {
            if (cap.cmdchar == '\'' || cap.cmdchar == '`' || cap.cmdchar == '[' || cap.cmdchar == ']')
                setpcmark();
            COPY_pos(curwin.w_cursor, pos);
            if (flag)
                beginline(BL_WHITE | BL_FIX);
            else
                check_cursor();
        }
        cap.oap.motion_type = flag ? MLINE : MCHAR;
        if (cap.cmdchar == '`')
            cap.oap.use_reg_one = true;
        cap.oap.inclusive = false;              /* ignored if not MCHAR */
        curwin.w_set_curswant = true;
    }

    /*private*/ static Bytes visop_trans = u8("YyDdCcxdXdAAIIrr");

    /*
     * Handle commands that are operators in Visual mode.
     */
    /*private*/ static void v_visop(cmdarg_C cap)
    {
        /* Uppercase means linewise, except in block mode,
         * then "D" deletes, and "C" replaces till EOL. */
        if (asc_isupper(cap.cmdchar))
        {
            if (VIsual_mode != Ctrl_V)
            {
                VIsual_mode_orig = VIsual_mode;
                VIsual_mode = 'V';
            }
            else if (cap.cmdchar == 'C' || cap.cmdchar == 'D')
                curwin.w_curswant = MAXCOL;
        }
        cap.cmdchar = vim_strchr(visop_trans, cap.cmdchar).at(1);
        nv_operator.nv(cap);
    }

    /*
     * "s" and "S" commands.
     */
    /*private*/ static final nv_func_C nv_subst = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (VIsual_active)  /* "vs" and "vS" are the same as "vc" */
            {
                if (cap.cmdchar == 'S')
                {
                    VIsual_mode_orig = VIsual_mode;
                    VIsual_mode = 'V';
                }
                cap.cmdchar = 'c';
                nv_operator.nv(cap);
            }
            else
                nv_optrans.nv(cap);
        }
    };

    /*
     * Abbreviated commands.
     */
    /*private*/ static final nv_func_C nv_abbrev = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (cap.cmdchar == K_DEL || cap.cmdchar == K_KDEL)
                cap.cmdchar = 'x';          /* DEL key behaves like 'x' */

            /* in Visual mode these commands are operators */
            if (VIsual_active)
                v_visop(cap);
            else
                nv_optrans.nv(cap);
        }
    };

    /*private*/ static Bytes[/*8*/] optrans_ar =
    {
        u8("dl"), u8("dh"),
        u8("d$"), u8("c$"),
        u8("cl"), u8("cc"),
        u8("yy"), u8(":s\r")
    };
    /*private*/ static Bytes optrans_str = u8("xXDCsSY&");

    /*
     * Translate a command into another command.
     */
    /*private*/ static final nv_func_C nv_optrans = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (!checkclearopq(cap.oap))
            {
                /* In Vi "2D" doesn't delete the next line.
                 * Can't translate it either, because "2." should also not use the count. */
                if (cap.cmdchar == 'D' && vim_strbyte(p_cpo[0], CPO_HASH) != null)
                {
                    COPY_pos(cap.oap.op_start, curwin.w_cursor);
                    cap.oap.op_type = OP_DELETE;
                    set_op_var(OP_DELETE);
                    cap.count1 = 1;
                    nv_dollar.nv(cap);
                    finish_op = true;
                    resetRedobuff();
                    appendCharToRedobuff('D');
                }
                else
                {
                    if (cap.count0 != 0)
                        stuffnumReadbuff(cap.count0);
                    stuffReadbuff(optrans_ar[BDIFF(vim_strchr(optrans_str, cap.cmdchar), optrans_str)]);
                }
            }
            cap.opcount = 0;
        }
    };

    /*
     * "'" and "`" commands.  Also for "g'" and "g`".
     * cap.arg is TRUE for "'" and "g'".
     */
    /*private*/ static final nv_func_C nv_gomark = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            int c;
            if (cap.cmdchar == 'g')
                c = cap.extra_char[0];
            else
                c = cap.nchar[0];

            pos_C pos = getmark(c, (cap.oap.op_type == OP_NOP));
            if (pos == NOPOS)         /* jumped to other file */
            {
                if (cap.arg != 0)
                {
                    check_cursor_lnum();
                    beginline(BL_WHITE | BL_FIX);
                }
                else
                    check_cursor();
            }
            else
                nv_cursormark(cap, cap.arg != 0, pos);

            /* May need to clear the coladd that a mark includes. */
            if (!virtual_active())
                curwin.w_cursor.coladd = 0;
        }
    };

    /*
     * Handle CTRL-O, CTRL-I, "g;" and "g," commands.
     */
    /*private*/ static final nv_func_C nv_pcmark = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            pos_C pos;

            if (!checkclearopq(cap.oap))
            {
                if (cap.cmdchar == 'g')
                    pos = movechangelist((int)cap.count1);
                else
                    pos = movemark((int)cap.count1);
                if (pos == NOPOS)           /* jump to other file */
                {
                    curwin.w_set_curswant = true;
                    check_cursor();
                }
                else if (pos != null)               /* can jump */
                    nv_cursormark(cap, false, pos);
                else if (cap.cmdchar == 'g')
                {
                    if (curbuf.b_changelistlen == 0)
                        emsg(u8("E664: changelist is empty"));
                    else if (cap.count1 < 0)
                        emsg(u8("E662: At start of changelist"));
                    else
                        emsg(u8("E663: At end of changelist"));
                }
                else
                    clearopbeep(cap.oap);
            }
        }
    };

    /*
     * Handle '"' command.
     */
    /*private*/ static final nv_func_C nv_regname = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (checkclearop(cap.oap))
                return;
            if (cap.nchar[0] == '=')
                cap.nchar[0] = get_expr_register();
            if (cap.nchar[0] != NUL && valid_yank_reg(cap.nchar[0], false))
            {
                cap.oap.regname = cap.nchar[0];
                cap.opcount = cap.count0;       /* remember count before '"' */
                set_reg_var(cap.oap.regname);
            }
            else
                clearopbeep(cap.oap);
        }
    };

    /*
     * Handle "v", "V" and "CTRL-V" commands.
     * Also for "gh", "gH" and "g^H" commands: Always start Select mode, cap.arg is TRUE.
     * Handle CTRL-Q just like CTRL-V.
     */
    /*private*/ static final nv_func_C nv_visual = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (cap.cmdchar == Ctrl_Q)
                cap.cmdchar = Ctrl_V;

            /* 'v', 'V' and CTRL-V can be used while an operator is pending to make it
             * characterwise, linewise, or blockwise. */
            if (cap.oap.op_type != OP_NOP)
            {
                cap.oap.motion_force = cap.cmdchar;
                finish_op = false;      /* operator doesn't finish now but later */
                return;
            }

            VIsual_select = (cap.arg != 0);
            if (VIsual_active)      /* change Visual mode */
            {
                if (VIsual_mode == cap.cmdchar)     /* stop visual mode */
                    end_visual_mode();
                else                                /* toggle char/block mode */
                {                                   /*     or char/line mode */
                    VIsual_mode = cap.cmdchar;
                    showmode();
                }
                redraw_curbuf_later(INVERTED);      /* update the inversion */
            }
            else                    /* start Visual mode */
            {
                check_visual_highlight();
                if (0 < cap.count0 && resel_VIsual_mode != NUL)
                {
                    /* use previously selected part */
                    COPY_pos(VIsual, curwin.w_cursor);

                    VIsual_active = true;
                    VIsual_reselect = true;
                    if (cap.arg == 0)
                        /* start Select mode when 'selectmode' contains "cmd" */
                        may_start_select('c');
                    setmouse();
                    if (p_smd[0] && msg_silent == 0)
                        redraw_cmdline = true;      /* show visual mode later */
                    /*
                     * For V and ^V, we multiply the number of lines even if there was only one.
                     */
                    if (resel_VIsual_mode != 'v' || 1 < resel_VIsual_line_count)
                    {
                        curwin.w_cursor.lnum += resel_VIsual_line_count * cap.count0 - 1;
                        if (curwin.w_cursor.lnum > curbuf.b_ml.ml_line_count)
                            curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
                    }
                    VIsual_mode = resel_VIsual_mode;
                    if (VIsual_mode == 'v')
                    {
                        if (resel_VIsual_line_count <= 1)
                        {
                            validate_virtcol();
                            curwin.w_curswant = curwin.w_virtcol + resel_VIsual_vcol * (int)cap.count0 - 1;
                        }
                        else
                            curwin.w_curswant = resel_VIsual_vcol;
                        coladvance(curwin.w_curswant);
                    }
                    if (resel_VIsual_vcol == MAXCOL)
                    {
                        curwin.w_curswant = MAXCOL;
                        coladvance(MAXCOL);
                    }
                    else if (VIsual_mode == Ctrl_V)
                    {
                        validate_virtcol();
                        curwin.w_curswant = curwin.w_virtcol + resel_VIsual_vcol * (int)cap.count0 - 1;
                        coladvance(curwin.w_curswant);
                    }
                    else
                        curwin.w_set_curswant = true;
                    redraw_curbuf_later(INVERTED);      /* show the inversion */
                }
                else
                {
                    if (cap.arg == 0)
                        /* start Select mode when 'selectmode' contains "cmd" */
                        may_start_select('c');
                    n_start_visual_mode(cap.cmdchar);
                    if (VIsual_mode != 'V' && p_sel[0].at(0) == (byte)'e')
                        cap.count1++;   /* include one more char */
                    if (0 < cap.count0 && 0 < --cap.count1)
                    {
                        /* With a count select that many characters or lines. */
                        if (VIsual_mode == 'v' || VIsual_mode == Ctrl_V)
                            nv_right.nv(cap);
                        else if (VIsual_mode == 'V')
                            nv_down.nv(cap);
                    }
                }
            }
        }
    };

    /*
     * Start selection for Shift-movement keys.
     */
    /*private*/ static void start_selection()
    {
        /* if 'selectmode' contains "key", start Select mode */
        may_start_select('k');
        n_start_visual_mode('v');
    }

    /*
     * Start Select mode, if "c" is in 'selectmode' and not in a mapping or menu.
     */
    /*private*/ static void may_start_select(int c)
    {
        VIsual_select = (stuff_empty() && typebuf_typed() && vim_strchr(p_slm[0], c) != null);
    }

    /*
     * Start Visual mode "c".
     * Should set VIsual_select before calling this.
     */
    /*private*/ static void n_start_visual_mode(int c)
    {
        /* Check for redraw before changing the state. */
        conceal_check_cursor_line();

        VIsual_mode = c;
        VIsual_active = true;
        VIsual_reselect = true;
        /* Corner case: the 0 position in a tab may change when going into
         * virtualedit.  Recalculate curwin.w_cursor to avoid bad hilighting.
         */
        if (c == Ctrl_V && (ve_flags[0] & VE_BLOCK) != 0 && gchar_cursor() == TAB)
        {
            validate_virtcol();
            coladvance(curwin.w_virtcol);
        }
        COPY_pos(VIsual, curwin.w_cursor);

        setmouse();
        /* Check for redraw after changing the state. */
        conceal_check_cursor_line();

        if (p_smd[0] && msg_silent == 0)
            redraw_cmdline = true;  /* show visual mode later */
        /* Make sure the clipboard gets updated.  Needed because start and
         * end may still be the same, and the selection needs to be owned. */
        clip_star.vmode = NUL;

        /* Only need to redraw this line, unless still need to redraw
         * an old Visual area (when 'lazyredraw' is set). */
        if (curwin.w_redr_type < INVERTED)
        {
            curwin.w_old_cursor_lnum = curwin.w_cursor.lnum;
            curwin.w_old_visual_lnum = curwin.w_cursor.lnum;
        }
    }

    /*
     * CTRL-W: Window commands
     */
    /*private*/ static final nv_func_C nv_window = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (!checkclearop(cap.oap))
                do_window(cap.nchar[0], cap.count0, NUL);
        }
    };

    /*
     * CTRL-Z: Suspend
     */
    /*private*/ static final nv_func_C nv_suspend = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            clearop(cap.oap);
            if (VIsual_active)
                end_visual_mode();              /* stop Visual mode */
            do_cmdline_cmd(u8("st"));
        }
    };

    /*
     * Commands starting with "g".
     */
    /*private*/ static final nv_func_C nv_g_cmd = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            oparg_C oap = cap.oap;
            boolean flag = false;

            switch (cap.nchar[0])
            {
                /*
                 * "gR": Enter virtual replace mode.
                 */
                case 'R':
                    cap.arg = TRUE;
                    nv_Replace.nv(cap);
                    break;

                case 'r':
                    nv_vreplace(cap);
                    break;

                case '&':
                    do_cmdline_cmd(u8("%s//~/&"));
                    break;

                /*
                 * "gv": Reselect the previous Visual area.
                 *       If Visual already active, exchange previous and current Visual area.
                 */
                case 'v':
                    if (checkclearop(oap))
                        break;

                    if (curbuf.b_visual.vi_start.lnum == 0
                            || curbuf.b_ml.ml_line_count < curbuf.b_visual.vi_start.lnum
                            || curbuf.b_visual.vi_end.lnum == 0)
                        beep_flush();
                    else
                    {
                        /* set w_cursor to the start of the Visual area, tpos to the end */
                        pos_C tpos = new pos_C();
                        if (VIsual_active)
                        {
                            int i = VIsual_mode;
                            VIsual_mode = curbuf.b_visual.vi_mode;
                            curbuf.b_visual.vi_mode = i;
                            curbuf.b_visual_mode_eval = i;
                            i = curwin.w_curswant;
                            curwin.w_curswant = curbuf.b_visual.vi_curswant;
                            curbuf.b_visual.vi_curswant = i;

                            COPY_pos(tpos, curbuf.b_visual.vi_end);
                            COPY_pos(curbuf.b_visual.vi_end, curwin.w_cursor);
                            COPY_pos(curwin.w_cursor, curbuf.b_visual.vi_start);
                            COPY_pos(curbuf.b_visual.vi_start, VIsual);
                        }
                        else
                        {
                            VIsual_mode = curbuf.b_visual.vi_mode;
                            curwin.w_curswant = curbuf.b_visual.vi_curswant;
                            COPY_pos(tpos, curbuf.b_visual.vi_end);
                            COPY_pos(curwin.w_cursor, curbuf.b_visual.vi_start);
                        }

                        VIsual_active = true;
                        VIsual_reselect = true;

                        /* Set Visual to the start and w_cursor to the end of the Visual area.
                         * Make sure they are on an existing character. */
                        check_cursor();
                        COPY_pos(VIsual, curwin.w_cursor);
                        COPY_pos(curwin.w_cursor, tpos);
                        check_cursor();
                        update_topline();
                        /*
                         * When called from normal "g" command: start Select mode when 'selectmode'
                         * contains "cmd".  When called for K_SELECT, always start Select mode.
                         */
                        if (cap.arg != 0)
                            VIsual_select = true;
                        else
                            may_start_select('c');
                        setmouse();
                        /* Make sure the clipboard gets updated.  Needed because start and
                         * end are still the same, and the selection needs to be owned. */
                        clip_star.vmode = NUL;
                        redraw_curbuf_later(INVERTED);
                        showmode();
                    }
                    break;

                /*
                 * "gV": Don't reselect the previous Visual area after a Select mode mapping of menu.
                 */
                case 'V':
                    VIsual_reselect = false;
                    break;

                /*
                 * "gh":  start Select mode.
                 * "gH":  start Select line mode.
                 * "g^H": start Select block mode.
                 */
                case K_BS:
                    cap.nchar[0] = Ctrl_H;
                    /* FALLTHROUGH */

                case 'h':
                case 'H':
                case Ctrl_H:
                    cap.cmdchar = cap.nchar[0] + ('v' - 'h');
                    cap.arg = TRUE;
                    nv_visual.nv(cap);
                    break;

                /* "gn", "gN" visually select next/previous search match
                 * "gn" selects next match
                 * "gN" selects previous match
                 */
                case 'N':
                case 'n':
                    if (!current_search(cap.count1, cap.nchar[0] == 'n'))
                        clearopbeep(oap);
                    break;

                /*
                 * "gj" and "gk" two new funny movement keys -- up and down
                 * movement based on *screen* line rather than *file* line.
                 */
                case 'j':
                case K_DOWN:
                {
                    boolean i;

                    /* with 'nowrap' it works just like the normal "j" command;
                     * also when in a closed fold */
                    if (!curwin.w_onebuf_opt.wo_wrap[0])
                    {
                        oap.motion_type = MLINE;
                        i = cursor_down(cap.count1, oap.op_type == OP_NOP);
                    }
                    else
                        i = nv_screengo(oap, FORWARD, cap.count1);
                    if (i == false)
                        clearopbeep(oap);
                    break;
                }

                case 'k':
                case K_UP:
                {
                    boolean i;

                    /* with 'nowrap' it works just like the normal "k" command;
                     * also when in a closed fold */
                    if (!curwin.w_onebuf_opt.wo_wrap[0])
                    {
                        oap.motion_type = MLINE;
                        i = cursor_up(cap.count1, oap.op_type == OP_NOP);
                    }
                    else
                        i = nv_screengo(oap, BACKWARD, cap.count1);
                    if (i == false)
                        clearopbeep(oap);
                    break;
                }

                /*
                 * "gJ": join two lines without inserting a space.
                 */
                case 'J':
                    nv_join.nv(cap);
                    break;

                /*
                 * "g0", "g^" and "g$": Like "0", "^" and "$" but for screen lines.
                 * "gm": middle of "g0" and "g$".
                 */
                case '^':
                    flag = true;
                    /* FALLTHROUGH */

                case '0':
                case 'm':
                case K_HOME:
                case K_KHOME:
                {
                    int i;

                    oap.motion_type = MCHAR;
                    oap.inclusive = false;
                    if (curwin.w_onebuf_opt.wo_wrap[0] && curwin.w_width != 0)
                    {
                        int width1 = curwin.w_width - curwin_col_off();
                        int width2 = width1 + curwin_col_off2();

                        validate_virtcol();
                        i = 0;
                        if (width1 <= curwin.w_virtcol && 0 < width2)
                            i = (curwin.w_virtcol - width1) / width2 * width2 + width1;
                    }
                    else
                        i = curwin.w_leftcol;
                    /* Go to the middle of the screen line.  When 'number' or 'relativenumber' is on
                     * and lines are wrapping the middle can be more to the left. */
                    if (cap.nchar[0] == 'm')
                        i += (curwin.w_width - curwin_col_off()
                                + ((curwin.w_onebuf_opt.wo_wrap[0] && 0 < i) ? curwin_col_off2() : 0)) / 2;
                    coladvance(i);
                    if (flag)
                    {
                        do
                        {
                            i = gchar_cursor();
                        } while (vim_iswhite(i) && oneright() == true);
                    }
                    curwin.w_set_curswant = true;
                    break;
                }

                case '_':
                    /* "g_": to the last non-blank character in the line or <count> lines downward. */
                    cap.oap.motion_type = MCHAR;
                    cap.oap.inclusive = true;
                    curwin.w_curswant = MAXCOL;
                    if (cursor_down(cap.count1 - 1, cap.oap.op_type == OP_NOP) == false)
                        clearopbeep(cap.oap);
                    else
                    {
                        Bytes ptr = ml_get_curline();

                        /* In Visual mode we may end up after the line. */
                        if (0 < curwin.w_cursor.col && ptr.at(curwin.w_cursor.col) == NUL)
                            --curwin.w_cursor.col;

                        /* Decrease the cursor column until it's on a non-blank. */
                        while (0 < curwin.w_cursor.col && vim_iswhite(ptr.at(curwin.w_cursor.col)))
                            --curwin.w_cursor.col;
                        curwin.w_set_curswant = true;
                        adjust_for_sel(cap);
                    }
                    break;

                case '$':
                case K_END:
                case K_KEND:
                {
                    int col_off = curwin_col_off();

                    oap.motion_type = MCHAR;
                    oap.inclusive = true;
                    if (curwin.w_onebuf_opt.wo_wrap[0] && curwin.w_width != 0)
                    {
                        curwin.w_curswant = MAXCOL; /* so we stay at the end */
                        if (cap.count1 == 1)
                        {
                            int width1 = curwin.w_width - col_off;
                            int width2 = width1 + curwin_col_off2();

                            validate_virtcol();
                            int i = width1 - 1;
                            if (width1 <= curwin.w_virtcol)
                                i += ((curwin.w_virtcol - width1) / width2 + 1) * width2;
                            coladvance(i);

                            /* Make sure we stick in this column. */
                            validate_virtcol();
                            curwin.w_curswant = curwin.w_virtcol;
                            curwin.w_set_curswant = false;
                            if (0 < curwin.w_cursor.col && curwin.w_onebuf_opt.wo_wrap[0])
                            {
                                /*
                                 * Check for landing on a character that got split at the end of the line.
                                 * We do not want to advance to the next screen line.
                                 */
                                if (i < curwin.w_virtcol)
                                    --curwin.w_cursor.col;
                            }
                        }
                        else if (nv_screengo(oap, FORWARD, cap.count1 - 1) == false)
                            clearopbeep(oap);
                    }
                    else
                    {
                        int i = curwin.w_leftcol + curwin.w_width - col_off - 1;
                        coladvance(i);

                        /* Make sure we stick in this column. */
                        validate_virtcol();
                        curwin.w_curswant = curwin.w_virtcol;
                        curwin.w_set_curswant = false;
                    }
                    break;
                }

                /*
                 * "g*" and "g#", like "*" and "#" but without using "\<" and "\>"
                 */
                case '*':
                case '#':
                case 163: // case char_u(POUND):         /* pound sign (sometimes equal to '#') */
                case Ctrl_RSB:              /* :tag or :tselect for current identifier */
                case ']':                   /* :tselect for current identifier */
                    nv_ident.nv(cap);
                    break;

                /*
                 * ge and gE: go back to end of word
                 */
                case 'e':
                case 'E':
                    oap.motion_type = MCHAR;
                    curwin.w_set_curswant = true;
                    oap.inclusive = true;
                    if (bckend_word(cap.count1, cap.nchar[0] == 'E', false) == false)
                        clearopbeep(oap);
                    break;

                /*
                 * "g CTRL-G": display info about cursor position.
                 */
                case Ctrl_G:
                    cursor_pos_info();
                    break;

                /*
                 * "gi": start Insert at the last position.
                 */
                case 'i':
                    if (curbuf.b_last_insert.lnum != 0)
                    {
                        COPY_pos(curwin.w_cursor, curbuf.b_last_insert);
                        check_cursor_lnum();
                        int i = strlen(ml_get_curline());
                        if (i < curwin.w_cursor.col)
                        {
                            if (virtual_active())
                                curwin.w_cursor.coladd += curwin.w_cursor.col - i;
                            curwin.w_cursor.col = i;
                        }
                    }
                    cap.cmdchar = 'i';
                    nv_edit.nv(cap);
                    break;

                /*
                 * "gI": Start insert in column 1.
                 */
                case 'I':
                    beginline(0);
                    if (!checkclearopq(oap))
                        invoke_edit(cap, false, 'g', false);
                    break;

                /*
                 * "g'm" and "g`m": jump to mark without setting pcmark.
                 */
                case '\'':
                    cap.arg = TRUE;
                    /* FALLTHROUGH */

                case '`':
                    nv_gomark.nv(cap);
                    break;

                /*
                 * "gs": Goto sleep.
                 */
                case 's':
                    do_sleep(cap.count1 * 1000L);
                    break;

                /*
                 * "ga": Display the ascii value of the character under the cursor.
                 *       It is displayed in decimal, hex, and octal.
                 */
                case 'a':
                    ex_ascii.ex(null);
                    break;

                /*
                 * "g8": Display the bytes used for the UTF-8 character under the cursor.
                 *       It is displayed in hex.
                 * "8g8" finds illegal byte sequence.
                 */
                case '8':
                    if (cap.count0 == 8)
                        utf_find_illegal();
                    else
                        show_utf8();
                    break;

                case '<':
                    show_sb_text();
                    break;

                /*
                 * "gg": Goto the first line in file.
                 *       With a count it goes to that line number like for "G".
                 */
                case 'g':
                    cap.arg = FALSE;
                    nv_goto.nv(cap);
                    break;

                /*
                 * Two-character operators:
                 *  "gq"    Format text.
                 *  "gw"    Format text and keep cursor position.
                 *  "g~"    Toggle the case of the text.
                 *  "gu"    Change text to lower case.
                 *  "gU"    Change text to upper case.
                 *  "g?"    rot13 encoding
                 *  "g@"    call 'operatorfunc'
                 */
                case 'q':
                case 'w':
                    COPY_pos(oap.cursor_start, curwin.w_cursor);
                    /* FALLTHROUGH */

                case '~':
                case 'u':
                case 'U':
                case '?':
                case '@':
                    nv_operator.nv(cap);
                    break;

                /*
                 * "gd": Find first occurrence of pattern under the cursor in the current function;
                 * "gD": idem, but in the current file.
                 */
                case 'd':
                case 'D':
                    nv_gd(oap, cap.nchar[0], cap.count0 == 1);
                    break;

                /*
                 * g<*Mouse> : <C-*mouse>
                 */
                case K_MIDDLEMOUSE:
                case K_MIDDLEDRAG:
                case K_MIDDLERELEASE:
                case K_LEFTMOUSE:
                case K_LEFTDRAG:
                case K_LEFTRELEASE:
                case K_RIGHTMOUSE:
                case K_RIGHTDRAG:
                case K_RIGHTRELEASE:
                case K_X1MOUSE:
                case K_X1DRAG:
                case K_X1RELEASE:
                case K_X2MOUSE:
                case K_X2DRAG:
                case K_X2RELEASE:
                    mod_mask = MOD_MASK_CTRL;
                    do_mouse(oap, cap.nchar[0], BACKWARD, (int)cap.count1, 0);
                    break;

                case K_IGNORE:
                    break;

                /*
                 * "gP" and "gp": same as "P" and "p" but leave cursor just after new text.
                 */
                case 'p':
                case 'P':
                    nv_put.nv(cap);
                    break;

                /* "go": goto byte count from start of buffer */
                case 'o':
                    goto_byte(cap.count0);
                    break;

                /* "gQ": improved Ex mode */
                case 'Q':
                    if (text_locked())
                    {
                        clearopbeep(cap.oap);
                        text_locked_msg();
                        break;
                    }

                    if (!checkclearopq(oap))
                        do_exmode(true);
                    break;

                case ',':
                    nv_pcmark.nv(cap);
                    break;

                case ';':
                    cap.count1 = -cap.count1;
                    nv_pcmark.nv(cap);
                    break;

                case 't':
                    if (!checkclearop(oap))
                        goto_tabpage((int)cap.count0);
                    break;

                case 'T':
                    if (!checkclearop(oap))
                        goto_tabpage(-(int)cap.count1);
                    break;

                /*
                 * "g+" and "g-": undo or redo along the timeline.
                 */
                case '+':
                case '-':
                    if (!checkclearopq(oap))
                        undo_time(cap.nchar[0] == '-' ? -cap.count1 : cap.count1, false, false, false);
                    break;

                default:
                    clearopbeep(oap);
                    break;
            }
        }
    };

    /*
     * Handle "o" and "O" commands.
     */
    /*private*/ static void n_opencmd(cmdarg_C cap)
    {
        long oldline = curwin.w_cursor.lnum;

        if (!checkclearopq(cap.oap))
        {
            if (u_save(curwin.w_cursor.lnum - (cap.cmdchar == 'O' ? 1 : 0),
                       curwin.w_cursor.lnum + (cap.cmdchar == 'o' ? 1 : 0))
                    && open_line(cap.cmdchar == 'O' ? BACKWARD : FORWARD,
                        has_format_option(FO_OPEN_COMS) ? OPENLINE_DO_COM : 0, 0))
            {
                if (0 < curwin.w_onebuf_opt.wo_cole[0] && oldline != curwin.w_cursor.lnum)
                    update_single_line(curwin, oldline);
                /* When '#' is in 'cpoptions' ignore the count. */
                if (vim_strbyte(p_cpo[0], CPO_HASH) != null)
                    cap.count1 = 1;
                invoke_edit(cap, false, cap.cmdchar, true);
            }
        }
    }

    /*
     * "." command: redo last change.
     */
    /*private*/ static final nv_func_C nv_dot = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (!checkclearopq(cap.oap))
            {
                /*
                 * If "restart_edit" is true, the last but one command is repeated
                 * instead of the last command (inserting text).  This is used for
                 * CTRL-O <.> in insert mode.
                 */
                if (!start_redo(cap.count0, restart_edit != 0 && !arrow_used))
                    clearopbeep(cap.oap);
            }
        }
    };

    /*
     * CTRL-R: undo undo
     */
    /*private*/ static final nv_func_C nv_redo = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (!checkclearopq(cap.oap))
            {
                u_redo((int)cap.count1);
                curwin.w_set_curswant = true;
            }
        }
    };

    /*
     * Handle "U" command.
     */
    /*private*/ static final nv_func_C nv_Undo = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            /* In Visual mode and typing "gUU" triggers an operator. */
            if (cap.oap.op_type == OP_UPPER || VIsual_active)
            {
                /* translate "gUU" to "gUgU" */
                cap.cmdchar = 'g';
                cap.nchar[0] = 'U';
                nv_operator.nv(cap);
            }
            else if (!checkclearopq(cap.oap))
            {
                u_undoline();
                curwin.w_set_curswant = true;
            }
        }
    };

    /*
     * '~' command: If tilde is not an operator and Visual is off: swap case of a single character.
     */
    /*private*/ static final nv_func_C nv_tilde = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (!p_to[0] && !VIsual_active && cap.oap.op_type != OP_TILDE)
                n_swapchar(cap);
            else
                nv_operator.nv(cap);
        }
    };

    /*
     * Handle an operator command.
     * The actual work is done by do_pending_operator().
     */
    /*private*/ static final nv_func_C nv_operator = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            int op_type = get_op_type(cap.cmdchar, cap.nchar[0]);

            if (op_type == cap.oap.op_type)     /* double operator works on lines */
                nv_lineop.nv(cap);
            else if (!checkclearop(cap.oap))
            {
                COPY_pos(cap.oap.op_start, curwin.w_cursor);
                cap.oap.op_type = op_type;
                set_op_var(op_type);
            }
        }
    };

    /*
     * Set v:operator to the characters for "optype".
     */
    /*private*/ static void set_op_var(int optype)
    {
        Bytes opchars = new Bytes(3);

        if (optype == OP_NOP)
            set_vim_var_string(VV_OP, null, 0);
        else
        {
            opchars.be(0, get_op_char(optype));
            opchars.be(1, get_extra_op_char(optype));
            opchars.be(2, NUL);
            set_vim_var_string(VV_OP, opchars, -1);
        }
    }

    /*
     * Handle linewise operator "dd", "yy", etc.
     *
     * "_" is is a strange motion command that helps make operators more logical.
     * It is actually implemented, but not documented in the real Vi.  This motion
     * command actually refers to "the current line".  Commands like "dd" and "yy"
     * are really an alternate form of "d_" and "y_".  It does accept a count, so
     * "d3_" works to delete 3 lines.
     */
    /*private*/ static final nv_func_C nv_lineop = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            cap.oap.motion_type = MLINE;
            if (cursor_down(cap.count1 - 1L, cap.oap.op_type == OP_NOP) == false)
                clearopbeep(cap.oap);
            else if (  (cap.oap.op_type == OP_DELETE    /* only with linewise motions */
                        && cap.oap.motion_force != 'v'
                        && cap.oap.motion_force != Ctrl_V)
                    || cap.oap.op_type == OP_LSHIFT
                    || cap.oap.op_type == OP_RSHIFT)
                beginline(BL_SOL | BL_FIX);
            else if (cap.oap.op_type != OP_YANK)        /* 'Y' does not move cursor */
                beginline(BL_WHITE | BL_FIX);
        }
    };

    /*
     * <Home> command.
     */
    /*private*/ static final nv_func_C nv_home = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            /* CTRL-HOME is like "gg" */
            if ((mod_mask & MOD_MASK_CTRL) != 0)
                nv_goto.nv(cap);
            else
            {
                cap.count0 = 1;
                nv_pipe.nv(cap);
            }
            ins_at_eol = false;     /* Don't move cursor past eol
                                    * (only necessary in a one-character line). */
        }
    };

    /*
     * "|" command.
     */
    /*private*/ static final nv_func_C nv_pipe = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            cap.oap.motion_type = MCHAR;
            cap.oap.inclusive = false;
            beginline(0);
            if (0 < cap.count0)
            {
                coladvance((int)(cap.count0 - 1));
                curwin.w_curswant = (int)(cap.count0 - 1);
            }
            else
                curwin.w_curswant = 0;
            /* Keep curswant at the column where we wanted to go,
             * not where we ended; differs if line is too short. */
            curwin.w_set_curswant = false;
        }
    };

    /*
     * Handle back-word command "b" and "B".
     * cap.arg is 1 for "B"
     */
    /*private*/ static final nv_func_C nv_bck_word = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            cap.oap.motion_type = MCHAR;
            cap.oap.inclusive = false;
            curwin.w_set_curswant = true;
            if (bck_word(cap.count1, cap.arg != 0, false) == false)
                clearopbeep(cap.oap);
        }
    };

    /*
     * Handle word motion commands "e", "E", "w" and "W".
     * cap.arg is TRUE for "E" and "W".
     */
    /*private*/ static final nv_func_C nv_wordcmd = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            pos_C startpos = new pos_C();
            COPY_pos(startpos, curwin.w_cursor);

            /*
             * Set inclusive for the "E" and "e" command.
             */
            boolean word_end = (cap.cmdchar == 'e' || cap.cmdchar == 'E');
            cap.oap.inclusive = word_end;

            boolean flag = false;
            /*
             * "cw" and "cW" are a special case.
             */
            if (!word_end && cap.oap.op_type == OP_CHANGE)
            {
                int n = gchar_cursor();
                if (n != NUL)                   /* not an empty line */
                {
                    if (vim_iswhite(n))
                    {
                        /*
                         * Reproduce a funny Vi behaviour: "cw" on a blank only
                         * changes one character, not all blanks until the start of
                         * the next word.  Only do this when the 'w' flag is included
                         * in 'cpoptions'.
                         */
                        if (cap.count1 == 1 && vim_strbyte(p_cpo[0], CPO_CW) != null)
                        {
                            cap.oap.inclusive = true;
                            cap.oap.motion_type = MCHAR;
                            return;
                        }
                    }
                    else
                    {
                        /*
                         * This is a little strange.  To match what the real Vi does,
                         * we effectively map 'cw' to 'ce', and 'cW' to 'cE', provided
                         * that we are not on a space or a TAB.  This seems impolite
                         * at first, but it's really more what we mean when we say 'cw'.
                         * Another strangeness: When standing on the end of a word
                         * "ce" will change until the end of the next word, but "cw"
                         * will change only one character! This is done by setting flag.
                         */
                        cap.oap.inclusive = true;
                        word_end = true;
                        flag = true;
                    }
                }
            }

            cap.oap.motion_type = MCHAR;
            curwin.w_set_curswant = true;
            boolean n;
            if (word_end)
                n = end_word(cap.count1, cap.arg != 0, flag, false);
            else
                n = fwd_word(cap.count1, cap.arg != 0, cap.oap.op_type != OP_NOP);

            /* Don't leave the cursor on the NUL past the end of line.
             * Unless we didn't move it forward. */
            if (ltpos(startpos, curwin.w_cursor))
                adjust_cursor(cap.oap);

            if (n == false && cap.oap.op_type == OP_NOP)
                clearopbeep(cap.oap);
            else
                adjust_for_sel(cap);
        }
    };

    /*
     * Used after a movement command: if the cursor ends up on the NUL after the end of the line,
     * may move it back to the last character and make the motion inclusive.
     */
    /*private*/ static void adjust_cursor(oparg_C oap)
    {
        /* The cursor cannot remain on the NUL when:
         * - the column is > 0
         * - not in Visual mode or 'selection' is "o"
         * - 'virtualedit' is not "all" and not "onemore".
         */
        if (0 < curwin.w_cursor.col && gchar_cursor() == NUL
                    && (!VIsual_active || p_sel[0].at(0) == (byte)'o')
                    && !virtual_active() && (ve_flags[0] & VE_ONEMORE) == 0)
        {
            --curwin.w_cursor.col;
            /* prevent cursor from moving on the trail byte */
            mb_adjust_pos(curbuf, curwin.w_cursor);
            oap.inclusive = true;
        }
    }

    /*
     * "0" and "^" commands.
     * cap.arg is the argument for beginline().
     */
    /*private*/ static final nv_func_C nv_beginline = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            cap.oap.motion_type = MCHAR;
            cap.oap.inclusive = false;
            beginline(cap.arg);
            ins_at_eol = false;     /* Don't move cursor past eol
                                    * (only necessary in a one-character line). */
        }
    };

    /*
     * In exclusive Visual mode, may include the last character.
     */
    /*private*/ static void adjust_for_sel(cmdarg_C cap)
    {
        if (VIsual_active && cap.oap.inclusive && p_sel[0].at(0) == (byte)'e'
                && gchar_cursor() != NUL && ltpos(VIsual, curwin.w_cursor))
        {
            inc_cursor();
            cap.oap.inclusive = false;
        }
    }

    /*
     * Exclude last character at end of Visual area for 'selection' == "exclusive".
     * Should check VIsual_mode before calling this.
     * Returns true when backed up to the previous line.
     */
    /*private*/ static boolean unadjust_for_sel()
    {
        if (p_sel[0].at(0) == (byte)'e' && !eqpos(VIsual, curwin.w_cursor))
        {
            pos_C pp;
            if (ltpos(VIsual, curwin.w_cursor))
                pp = curwin.w_cursor;
            else
                pp = VIsual;
            if (0 < pp.coladd)
                --pp.coladd;
            else if (0 < pp.col)
            {
                --pp.col;
                mb_adjust_pos(curbuf, pp);
            }
            else if (1 < pp.lnum)
            {
                --pp.lnum;
                pp.col = strlen(ml_get(pp.lnum));
                return true;
            }
        }
        return false;
    }

    /*
     * SELECT key in Normal or Visual mode: end of Select mode mapping.
     */
    /*private*/ static final nv_func_C nv_select = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (VIsual_active)
                VIsual_select = true;
            else if (VIsual_reselect)
            {
                cap.nchar[0] = 'v';        /* fake "gv" command */
                cap.arg = TRUE;
                nv_g_cmd.nv(cap);
            }
        }
    };

    /*
     * "G", "gg", CTRL-END, CTRL-HOME.
     * cap.arg is TRUE for "G".
     */
    /*private*/ static final nv_func_C nv_goto = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            long lnum;
            if (cap.arg != 0)
                lnum = curbuf.b_ml.ml_line_count;
            else
                lnum = 1L;

            cap.oap.motion_type = MLINE;
            setpcmark();

            /* When a count is given, use it instead of the default lnum. */
            if (cap.count0 != 0)
                lnum = cap.count0;
            if (lnum < 1L)
                lnum = 1L;
            else if (curbuf.b_ml.ml_line_count < lnum)
                lnum = curbuf.b_ml.ml_line_count;
            curwin.w_cursor.lnum = lnum;
            beginline(BL_SOL | BL_FIX);
        }
    };

    /*
     * CTRL-\ in Normal mode.
     */
    /*private*/ static final nv_func_C nv_normal = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (cap.nchar[0] == Ctrl_N || cap.nchar[0] == Ctrl_G)
            {
                clearop(cap.oap);
                if (restart_edit != 0 && mode_displayed)
                    clear_cmdline = true;               /* unshow mode later */
                restart_edit = 0;
                if (cmdwin_type != 0)
                    cmdwin_result = Ctrl_C;
                if (VIsual_active)
                {
                    end_visual_mode();          /* stop Visual */
                    redraw_curbuf_later(INVERTED);
                }
                /* CTRL-\ CTRL-G restarts Insert mode when 'insertmode' is set. */
                if (cap.nchar[0] == Ctrl_G && p_im[0])
                    restart_edit = 'a';
            }
            else
                clearopbeep(cap.oap);
        }
    };

    /*
     * ESC in Normal mode: beep, but don't flush buffers.
     * Don't even beep if we are canceling a command.
     */
    /*private*/ static final nv_func_C nv_esc = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            boolean no_reason = (cap.oap.op_type == OP_NOP
                        && cap.opcount == 0
                        && cap.count0 == 0
                        && cap.oap.regname == 0
                        && !p_im[0]);

            if (cap.arg != 0)                   /* true for CTRL-C */
            {
                if (restart_edit == 0 && cmdwin_type == 0 && !VIsual_active && no_reason)
                    msg(u8("Type  :quit<Enter>  to exit Vim"));

                /* Don't reset "restart_edit" when 'insertmode' is set,
                 * it won't be set again below when halfway a mapping. */
                if (!p_im[0])
                    restart_edit = 0;
                if (cmdwin_type != 0)
                {
                    cmdwin_result = K_IGNORE;
                    got_int = false;            /* don't stop executing autocommands et al. */
                    return;
                }
            }

            if (VIsual_active)
            {
                end_visual_mode();              /* stop Visual */
                check_cursor_col();             /* make sure cursor is not beyond EOL */
                curwin.w_set_curswant = true;
                redraw_curbuf_later(INVERTED);
            }
            else if (no_reason)
                vim_beep();
            clearop(cap.oap);

            /* A CTRL-C is often used at the start of a menu.
             * When 'insertmode' is set, return to Insert mode afterwards. */
            if (restart_edit == 0 && goto_im() && ex_normal_busy == 0)
                restart_edit = 'a';
        }
    };

    /*
     * Handle "A", "a", "I", "i" and <Insert> commands.
     */
    /*private*/ static final nv_func_C nv_edit = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            /* <Insert> is equal to "i" */
            if (cap.cmdchar == K_INS || cap.cmdchar == K_KINS)
                cap.cmdchar = 'i';

            /* in Visual mode "A" and "I" are an operator */
            if (VIsual_active && (cap.cmdchar == 'A' || cap.cmdchar == 'I'))
                v_visop(cap);

            /* in Visual mode and after an operator "a" and "i" are for text objects */
            else if ((cap.cmdchar == 'a' || cap.cmdchar == 'i')
                    && (cap.oap.op_type != OP_NOP || VIsual_active))
            {
                nv_object(cap);
            }
            else if (!curbuf.b_p_ma[0] && !p_im[0])
            {
                /* Only give this error when 'insertmode' is off. */
                emsg(e_modifiable);
                clearop(cap.oap);
            }
            else if (!checkclearopq(cap.oap))
            {
                switch (cap.cmdchar)
                {
                    case 'A':   /* "A"ppend after the line */
                        curwin.w_set_curswant = true;
                        if (ve_flags[0] == VE_ALL)
                        {
                            int save_State = State;

                            /* Pretend Insert mode here to allow the cursor
                             * on the character past the end of the line. */
                            State = INSERT;
                            coladvance(MAXCOL);
                            State = save_State;
                        }
                        else
                            curwin.w_cursor.col += strlen(ml_get_cursor());
                        break;

                    case 'I':   /* "I"nsert before the first non-blank */
                        if (vim_strbyte(p_cpo[0], CPO_INSEND) == null)
                            beginline(BL_WHITE);
                        else
                            beginline(BL_WHITE|BL_FIX);
                        break;

                    case 'a':   /* "a"ppend is like "i"nsert on the next character */
                        /* Increment coladd when in virtual space, increment the
                         * column otherwise, also to append after an unprintable char. */
                        if (virtual_active()
                                && (0 < curwin.w_cursor.coladd
                                    || ml_get_cursor().at(0) == NUL
                                    || ml_get_cursor().at(0) == TAB))
                            curwin.w_cursor.coladd++;
                        else if (ml_get_cursor().at(0) != NUL)
                            inc_cursor();
                        break;
                }

                if (curwin.w_cursor.coladd != 0 && cap.cmdchar != 'A')
                {
                    int save_State = State;

                    /* Pretend Insert mode here to allow the cursor
                     * on the character past the end of the line. */
                    State = INSERT;
                    coladvance(getviscol());
                    State = save_State;
                }

                invoke_edit(cap, false, cap.cmdchar, false);
            }
        }
    };

    /*
     * Invoke edit() and take care of "restart_edit" and the return value.
     */
    /*private*/ static void invoke_edit(cmdarg_C cap, boolean repl, int cmd, boolean startln)
        /* repl: "r" or "gr" command */
    {
        int restart_edit_save = 0;

        /* Complicated: when the user types "a<C-O>a", we don't want to do Insert mode recursively.
         * But when doing "a<C-O>." or "a<C-O>rx", we do allow it. */
        if (repl || !stuff_empty())
            restart_edit_save = restart_edit;
        else
            restart_edit_save = 0;

        /* Always reset "restart_edit", this is not a restarted edit. */
        restart_edit = 0;

        if (edit(cmd, startln, cap.count1))
            cap.retval |= CA_COMMAND_BUSY;

        if (restart_edit == 0)
            restart_edit = restart_edit_save;
    }

    /*
     * "a" or "i" while an operator is pending or in Visual mode: object motion.
     */
    /*private*/ static void nv_object(cmdarg_C cap)
    {
        boolean include;
        if (cap.cmdchar == 'i')
            include = false;    /* "ix" = inner object: exclude white space */
        else
            include = true;     /* "ax" = an object: include white space */

        /* Make sure (), [], {} and <> are in 'matchpairs'. */
        Bytes mps_save = curbuf.b_p_mps[0];
        curbuf.b_p_mps[0] = u8("(:),{:},[:],<:>");

        boolean flag;
        switch (cap.nchar[0])
        {
            case 'w': /* "aw" = a word */
                    flag = current_word(cap.oap, cap.count1, include, false);
                    break;
            case 'W': /* "aW" = a WORD */
                    flag = current_word(cap.oap, cap.count1, include, true);
                    break;
            case 'b': /* "ab" = a braces block */
            case '(':
            case ')':
                    flag = current_block(cap.oap, cap.count1, include, '(', ')');
                    break;
            case 'B': /* "aB" = a Brackets block */
            case '{':
            case '}':
                    flag = current_block(cap.oap, cap.count1, include, '{', '}');
                    break;
            case '[': /* "a[" = a [] block */
            case ']':
                    flag = current_block(cap.oap, cap.count1, include, '[', ']');
                    break;
            case '<': /* "a<" = a <> block */
            case '>':
                    flag = current_block(cap.oap, cap.count1, include, '<', '>');
                    break;
            case 't': /* "at" = a tag block (xml and html) */
                    /* Do not adjust oap.op_end in do_pending_operator()
                     * otherwise there are different results for 'dit'
                     * (note leading whitespace in last line):
                     * 1) <b>      2) <b>
                     *    foobar      foobar
                     *    </b>            </b>
                     */
                    cap.retval |= CA_NO_ADJ_OP_END;
                    flag = current_tagblock(cap.oap, cap.count1, include);
                    break;
            case 'p': /* "ap" = a paragraph */
                    flag = current_par(cap.oap, cap.count1, include, 'p');
                    break;
            case 's': /* "as" = a sentence */
                    flag = current_sent(cap.oap, cap.count1, include);
                    break;
            case '"': /* "a"" = a double quoted string */
            case '\'': /* "a'" = a single quoted string */
            case '`': /* "a`" = a backtick quoted string */
                    flag = current_quote(cap.oap, cap.count1, include, cap.nchar[0]);
                    break;
            default:
                    flag = false;
                    break;
        }

        curbuf.b_p_mps[0] = mps_save;
        if (flag == false)
            clearopbeep(cap.oap);
        adjust_cursor_col();
        curwin.w_set_curswant = true;
    }

    /*
     * "q" command: Start/stop recording.
     * "q:", "q/", "q?": edit command-line in command-line window.
     */
    /*private*/ static final nv_func_C nv_record = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (cap.oap.op_type == OP_FORMAT)
            {
                /* "gqq" is the same as "gqgq": format line */
                cap.cmdchar = 'g';
                cap.nchar[0] = 'q';
                nv_operator.nv(cap);
            }
            else if (!checkclearop(cap.oap))
            {
                if (cap.nchar[0] == ':' || cap.nchar[0] == '/' || cap.nchar[0] == '?')
                {
                    stuffcharReadbuff(cap.nchar[0]);
                    stuffcharReadbuff(K_CMDWIN);
                }
                else
                    /* (stop) recording into a named register, unless executing a register */
                    if (!execReg && do_record(cap.nchar[0]) == false)
                        clearopbeep(cap.oap);
            }
        }
    };

    /*
     * Handle the "@r" command.
     */
    /*private*/ static final nv_func_C nv_at = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (checkclearop(cap.oap))
                return;
            if (cap.nchar[0] == '=')
            {
                if (get_expr_register() == NUL)
                    return;
            }
            while (0 < cap.count1-- && !got_int)
            {
                if (do_execreg(cap.nchar[0], false, false, false) == false)
                {
                    clearopbeep(cap.oap);
                    break;
                }
                line_breakcheck();
            }
        }
    };

    /*
     * Handle the CTRL-U and CTRL-D commands.
     */
    /*private*/ static final nv_func_C nv_halfpage = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if ((cap.cmdchar == Ctrl_U && curwin.w_cursor.lnum == 1)
                    || (cap.cmdchar == Ctrl_D && curwin.w_cursor.lnum == curbuf.b_ml.ml_line_count))
                clearopbeep(cap.oap);
            else if (!checkclearop(cap.oap))
                halfpage(cap.cmdchar == Ctrl_D, cap.count0);
        }
    };

    /*
     * Handle "J" or "gJ" command.
     */
    /*private*/ static final nv_func_C nv_join = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (VIsual_active)  /* join the visual lines */
                nv_operator.nv(cap);
            else if (!checkclearop(cap.oap))
            {
                if (cap.count0 <= 1)
                    cap.count0 = 2;         /* default for join is two lines! */
                if (curbuf.b_ml.ml_line_count < curwin.w_cursor.lnum + cap.count0 - 1)
                    clearopbeep(cap.oap);   /* beyond last line */
                else
                {
                    prep_redo(cap.oap.regname, cap.count0, NUL, cap.cmdchar, NUL, NUL, cap.nchar[0]);
                    do_join((int)cap.count0, cap.nchar[0] == NUL, true, true, true);
                }
            }
        }
    };

    /*
     * "P", "gP", "p" and "gp" commands.
     */
    /*private*/ static final nv_func_C nv_put = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            int regname = 0;
            boolean empty = false;
            boolean was_visual = false;
            int flags = 0;

            if (cap.oap.op_type != OP_NOP)
            {
                clearopbeep(cap.oap);
            }
            else
            {
                int dir = (cap.cmdchar == 'P' || (cap.cmdchar == 'g' && cap.nchar[0] == 'P')) ? BACKWARD : FORWARD;
                prep_redo_cmd(cap);
                if (cap.cmdchar == 'g')
                    flags |= PUT_CURSEND;

                yankreg_C reg1 = null, reg2 = null;

                if (VIsual_active)
                {
                    /* Putting in Visual mode: The put text replaces the selected
                     * text.  First delete the selected text, then put the new text.
                     * Need to save and restore the registers that the delete
                     * overwrites if the old contents is being put.
                     */
                    was_visual = true;
                    regname = cap.oap.regname;
                    regname = adjust_clip_reg(regname);
                    if (regname == 0 || regname == '"' || asc_isdigit(regname) || regname == '-'
                            || (clip_unnamed != 0 && (regname == '*' || regname == '+')))
                    {
                        /* The delete is going to overwrite the register we want to put, save it first. */
                        reg1 = get_register(regname, true);
                    }

                    /* Now delete the selected text. */
                    cap.cmdchar = 'd';
                    cap.nchar[0] = NUL;
                    cap.oap.regname = NUL;
                    nv_operator.nv(cap);
                    do_pending_operator(cap, 0, false);
                    empty = ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0);

                    /* delete PUT_LINE_BACKWARD; */
                    cap.oap.regname = regname;

                    if (reg1 != null)
                    {
                        /* Delete probably changed the register we want to put, save it first.
                         * Then put back what was there before the delete. */
                        reg2 = get_register(regname, false);
                        put_register(regname, reg1);
                    }

                    /* When deleted a linewise Visual area,
                     * put the register as lines to avoid it joined with the next line.
                     * When deletion was characterwise, split a line when putting lines. */
                    if (VIsual_mode == 'V')
                        flags |= PUT_LINE;
                    else if (VIsual_mode == 'v')
                        flags |= PUT_LINE_SPLIT;
                    if (VIsual_mode == Ctrl_V && dir == FORWARD)
                        flags |= PUT_LINE_FORWARD;
                    dir = BACKWARD;
                    if ((VIsual_mode != 'V' && curwin.w_cursor.col < curbuf.b_op_start.col)
                    || (VIsual_mode == 'V' && curwin.w_cursor.lnum < curbuf.b_op_start.lnum))
                        /* cursor is at the end of the line or end of file, put forward. */
                        dir = FORWARD;
                    /* May have been reset in do_put(). */
                    VIsual_active = true;
                }

                do_put(cap.oap.regname, dir, (int)cap.count1, flags);

                /* If a register was saved, put it back now. */
                if (reg2 != null)
                    put_register(regname, reg2);

                /* What to reselect with "gv"?
                 * Selecting the just put text seems to be the most useful, since the original was removed. */
                if (was_visual)
                {
                    COPY_pos(curbuf.b_visual.vi_start, curbuf.b_op_start);
                    COPY_pos(curbuf.b_visual.vi_end, curbuf.b_op_end);
                }

                /* When all lines were selected and deleted do_put() leaves
                 * an empty line that needs to be deleted now. */
                if (empty && ml_get(curbuf.b_ml.ml_line_count).at(0) == NUL)
                {
                    ml_delete(curbuf.b_ml.ml_line_count, true);

                    /* If the cursor was in that line, move it to the end of the last line. */
                    if (curwin.w_cursor.lnum > curbuf.b_ml.ml_line_count)
                    {
                        curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
                        coladvance(MAXCOL);
                    }
                }
                auto_format(false, true);
            }
        }
    };

    /*
     * "o" and "O" commands.
     */
    /*private*/ static final nv_func_C nv_open = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            if (VIsual_active)  /* switch start and end of visual */
                v_swap_corners(cap.cmdchar);
            else
                n_opencmd(cap);
        }
    };

    /*private*/ static final nv_func_C nv_drop = new nv_func_C()
    {
        public void nv(cmdarg_C _cap)
        {
            do_put('~', BACKWARD, 1, PUT_CURSEND);
        }
    };

    /*
     * Trigger CursorHold event.
     * When waiting for a character for 'updatetime' K_CURSORHOLD is put in the
     * input buffer.  "did_cursorhold" is set to avoid retriggering.
     */
    /*private*/ static final nv_func_C nv_cursorhold = new nv_func_C()
    {
        public void nv(cmdarg_C cap)
        {
            apply_autocmds(EVENT_CURSORHOLD, null, null, false, curbuf);
            did_cursorhold = true;
            cap.retval |= CA_COMMAND_BUSY;  /* don't call edit() now */
        }
    };

    /*
     * This table contains one entry for every Normal or Visual mode command.
     * The order doesn't matter, init_normal_cmds() will create a sorted index.
     * It is faster when all keys from zero to '~' are present.
     */
    /*private*/ static nv_cmd_C[] nv_cmds = new nv_cmd_C[]
    {
        new nv_cmd_C(NUL,              nv_error,       0,                     0              ),
        new nv_cmd_C(Ctrl_A,           nv_addsub,      0,                     0              ),
        new nv_cmd_C(Ctrl_B,           nv_page,        NV_STS,                BACKWARD       ),
        new nv_cmd_C(Ctrl_C,           nv_esc,         0,                     TRUE           ),
        new nv_cmd_C(Ctrl_D,           nv_halfpage,    0,                     0              ),
        new nv_cmd_C(Ctrl_E,           nv_scroll_line, 0,                     TRUE           ),
        new nv_cmd_C(Ctrl_F,           nv_page,        NV_STS,                FORWARD        ),
        new nv_cmd_C(Ctrl_G,           nv_ctrlg,       0,                     0              ),
        new nv_cmd_C(Ctrl_H,           nv_ctrlh,       0,                     0              ),
        new nv_cmd_C(Ctrl_I,           nv_pcmark,      0,                     0              ),
        new nv_cmd_C(NL,               nv_down,        0,                     FALSE          ),
        new nv_cmd_C(Ctrl_K,           nv_error,       0,                     0              ),
        new nv_cmd_C(Ctrl_L,           nv_clear,       0,                     0              ),
        new nv_cmd_C(Ctrl_M,           nv_down,        0,                     TRUE           ),
        new nv_cmd_C(Ctrl_N,           nv_down,        NV_STS,                FALSE          ),
        new nv_cmd_C(Ctrl_O,           nv_ctrlo,       0,                     0              ),
        new nv_cmd_C(Ctrl_P,           nv_up,          NV_STS,                FALSE          ),
        new nv_cmd_C(Ctrl_Q,           nv_visual,      0,                     FALSE          ),
        new nv_cmd_C(Ctrl_R,           nv_redo,        0,                     0              ),
        new nv_cmd_C(Ctrl_S,           nv_ignore,      0,                     0              ),
        new nv_cmd_C(Ctrl_T,           nv_error,       0,                     0              ),
        new nv_cmd_C(Ctrl_U,           nv_halfpage,    0,                     0              ),
        new nv_cmd_C(Ctrl_V,           nv_visual,      0,                     FALSE          ),
        new nv_cmd_C('V',              nv_visual,      0,                     FALSE          ),
        new nv_cmd_C('v',              nv_visual,      0,                     FALSE          ),
        new nv_cmd_C(Ctrl_W,           nv_window,      0,                     0              ),
        new nv_cmd_C(Ctrl_X,           nv_addsub,      0,                     0              ),
        new nv_cmd_C(Ctrl_Y,           nv_scroll_line, 0,                     FALSE          ),
        new nv_cmd_C(Ctrl_Z,           nv_suspend,     0,                     0              ),
        new nv_cmd_C(ESC,              nv_esc,         0,                     FALSE          ),
        new nv_cmd_C(Ctrl_BSL,         nv_normal,      NV_NCH_ALW,            0              ),
        new nv_cmd_C(Ctrl_RSB,         nv_ident,       NV_NCW,                0              ),
        new nv_cmd_C(Ctrl_HAT,         nv_hat,         NV_NCW,                0              ),
        new nv_cmd_C(Ctrl__,           nv_error,       0,                     0              ),
        new nv_cmd_C(' ',              nv_right,       0,                     0              ),
        new nv_cmd_C('!',              nv_operator,    0,                     0              ),
        new nv_cmd_C('"',              nv_regname,     NV_NCH_NOP|NV_KEEPREG, 0              ),
        new nv_cmd_C('#',              nv_ident,       0,                     0              ),
        new nv_cmd_C('$',              nv_dollar,      0,                     0              ),
        new nv_cmd_C('%',              nv_percent,     0,                     0              ),
        new nv_cmd_C('&',              nv_optrans,     0,                     0              ),
        new nv_cmd_C('\'',             nv_gomark,      NV_NCH_ALW,            TRUE           ),
        new nv_cmd_C('(',              nv_brace,       0,                     BACKWARD       ),
        new nv_cmd_C(')',              nv_brace,       0,                     FORWARD        ),
        new nv_cmd_C('*',              nv_ident,       0,                     0              ),
        new nv_cmd_C('+',              nv_down,        0,                     TRUE           ),
        new nv_cmd_C(',',              nv_csearch,     0,                     TRUE           ),
        new nv_cmd_C('-',              nv_up,          0,                     TRUE           ),
        new nv_cmd_C('.',              nv_dot,         NV_KEEPREG,            0              ),
        new nv_cmd_C('/',              nv_search,      0,                     FALSE          ),
        new nv_cmd_C('0',              nv_beginline,   0,                     0              ),
        new nv_cmd_C('1',              nv_ignore,      0,                     0              ),
        new nv_cmd_C('2',              nv_ignore,      0,                     0              ),
        new nv_cmd_C('3',              nv_ignore,      0,                     0              ),
        new nv_cmd_C('4',              nv_ignore,      0,                     0              ),
        new nv_cmd_C('5',              nv_ignore,      0,                     0              ),
        new nv_cmd_C('6',              nv_ignore,      0,                     0              ),
        new nv_cmd_C('7',              nv_ignore,      0,                     0              ),
        new nv_cmd_C('8',              nv_ignore,      0,                     0              ),
        new nv_cmd_C('9',              nv_ignore,      0,                     0              ),
        new nv_cmd_C(':',              nv_colon,       0,                     0              ),
        new nv_cmd_C(';',              nv_csearch,     0,                     FALSE          ),
        new nv_cmd_C('<',              nv_operator,    NV_RL,                 0              ),
        new nv_cmd_C('=',              nv_operator,    0,                     0              ),
        new nv_cmd_C('>',              nv_operator,    NV_RL,                 0              ),
        new nv_cmd_C('?',              nv_search,      0,                     FALSE          ),
        new nv_cmd_C('@',              nv_at,          NV_NCH_NOP,            FALSE          ),
        new nv_cmd_C('A',              nv_edit,        0,                     0              ),
        new nv_cmd_C('B',              nv_bck_word,    0,                     1              ),
        new nv_cmd_C('C',              nv_abbrev,      NV_KEEPREG,            0              ),
        new nv_cmd_C('D',              nv_abbrev,      NV_KEEPREG,            0              ),
        new nv_cmd_C('E',              nv_wordcmd,     0,                     TRUE           ),
        new nv_cmd_C('F',              nv_csearch,     NV_NCH_ALW|NV_LANG,    BACKWARD       ),
        new nv_cmd_C('G',              nv_goto,        0,                     TRUE           ),
        new nv_cmd_C('H',              nv_scroll,      0,                     0              ),
        new nv_cmd_C('I',              nv_edit,        0,                     0              ),
        new nv_cmd_C('J',              nv_join,        0,                     0              ),
        new nv_cmd_C('K',              nv_ident,       0,                     0              ),
        new nv_cmd_C('L',              nv_scroll,      0,                     0              ),
        new nv_cmd_C('M',              nv_scroll,      0,                     0              ),
        new nv_cmd_C('N',              nv_next,        0,                     SEARCH_REV     ),
        new nv_cmd_C('O',              nv_open,        0,                     0              ),
        new nv_cmd_C('P',              nv_put,         0,                     0              ),
        new nv_cmd_C('Q',              nv_exmode,      NV_NCW,                0              ),
        new nv_cmd_C('R',              nv_Replace,     0,                     FALSE          ),
        new nv_cmd_C('S',              nv_subst,       NV_KEEPREG,            0              ),
        new nv_cmd_C('T',              nv_csearch,     NV_NCH_ALW|NV_LANG,    BACKWARD       ),
        new nv_cmd_C('U',              nv_Undo,        0,                     0              ),
        new nv_cmd_C('W',              nv_wordcmd,     0,                     TRUE           ),
        new nv_cmd_C('X',              nv_abbrev,      NV_KEEPREG,            0              ),
        new nv_cmd_C('Y',              nv_abbrev,      NV_KEEPREG,            0              ),
        new nv_cmd_C('Z',              nv_Zet,         NV_NCH_NOP|NV_NCW,     0              ),
        new nv_cmd_C('[',              nv_brackets,    NV_NCH_ALW,            BACKWARD       ),
        new nv_cmd_C('\\',             nv_error,       0,                     0              ),
        new nv_cmd_C(']',              nv_brackets,    NV_NCH_ALW,            FORWARD        ),
        new nv_cmd_C('^',              nv_beginline,   0,                     BL_WHITE|BL_FIX),
        new nv_cmd_C('_',              nv_lineop,      0,                     0              ),
        new nv_cmd_C('`',              nv_gomark,      NV_NCH_ALW,            FALSE          ),
        new nv_cmd_C('a',              nv_edit,        NV_NCH,                0              ),
        new nv_cmd_C('b',              nv_bck_word,    0,                     0              ),
        new nv_cmd_C('c',              nv_operator,    0,                     0              ),
        new nv_cmd_C('d',              nv_operator,    0,                     0              ),
        new nv_cmd_C('e',              nv_wordcmd,     0,                     FALSE          ),
        new nv_cmd_C('f',              nv_csearch,     NV_NCH_ALW|NV_LANG,    FORWARD        ),
        new nv_cmd_C('g',              nv_g_cmd,       NV_NCH_ALW,            FALSE          ),
        new nv_cmd_C('h',              nv_left,        NV_RL,                 0              ),
        new nv_cmd_C('i',              nv_edit,        NV_NCH,                0              ),
        new nv_cmd_C('j',              nv_down,        0,                     FALSE          ),
        new nv_cmd_C('k',              nv_up,          0,                     FALSE          ),
        new nv_cmd_C('l',              nv_right,       NV_RL,                 0              ),
        new nv_cmd_C('m',              nv_mark,        NV_NCH_NOP,            0              ),
        new nv_cmd_C('n',              nv_next,        0,                     0              ),
        new nv_cmd_C('o',              nv_open,        0,                     0              ),
        new nv_cmd_C('p',              nv_put,         0,                     0              ),
        new nv_cmd_C('q',              nv_record,      NV_NCH,                0              ),
        new nv_cmd_C('r',              nv_replace,     NV_NCH_NOP|NV_LANG,    0              ),
        new nv_cmd_C('s',              nv_subst,       NV_KEEPREG,            0              ),
        new nv_cmd_C('t',              nv_csearch,     NV_NCH_ALW|NV_LANG,    FORWARD        ),
        new nv_cmd_C('u',              nv_undo,        0,                     0              ),
        new nv_cmd_C('w',              nv_wordcmd,     0,                     FALSE          ),
        new nv_cmd_C('x',              nv_abbrev,      NV_KEEPREG,            0              ),
        new nv_cmd_C('y',              nv_operator,    0,                     0              ),
        new nv_cmd_C('z',              nv_zet,         NV_NCH_ALW,            0              ),
        new nv_cmd_C('{',              nv_findpar,     0,                     BACKWARD       ),
        new nv_cmd_C('|',              nv_pipe,        0,                     0              ),
        new nv_cmd_C('}',              nv_findpar,     0,                     FORWARD        ),
        new nv_cmd_C('~',              nv_tilde,       0,                     0              ),

        /* pound sign */
        new nv_cmd_C(char_u(POUND),    nv_ident,       0,                     0              ),
        new nv_cmd_C(K_MOUSEUP,        nv_mousescroll, 0,                     MSCR_UP        ),
        new nv_cmd_C(K_MOUSEDOWN,      nv_mousescroll, 0,                     MSCR_DOWN      ),
        new nv_cmd_C(K_MOUSELEFT,      nv_mousescroll, 0,                     MSCR_LEFT      ),
        new nv_cmd_C(K_MOUSERIGHT,     nv_mousescroll, 0,                     MSCR_RIGHT     ),
        new nv_cmd_C(K_LEFTMOUSE,      nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_LEFTMOUSE_NM,   nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_LEFTDRAG,       nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_LEFTRELEASE,    nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_LEFTRELEASE_NM, nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_MIDDLEMOUSE,    nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_MIDDLEDRAG,     nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_MIDDLERELEASE,  nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_RIGHTMOUSE,     nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_RIGHTDRAG,      nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_RIGHTRELEASE,   nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_X1MOUSE,        nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_X1DRAG,         nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_X1RELEASE,      nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_X2MOUSE,        nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_X2DRAG,         nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_X2RELEASE,      nv_mouse,       0,                     0              ),
        new nv_cmd_C(K_IGNORE,         nv_ignore,      NV_KEEPREG,            0              ),
        new nv_cmd_C(K_NOP,            nv_nop,         0,                     0              ),
        new nv_cmd_C(K_INS,            nv_edit,        0,                     0              ),
        new nv_cmd_C(K_KINS,           nv_edit,        0,                     0              ),
        new nv_cmd_C(K_BS,             nv_ctrlh,       0,                     0              ),
        new nv_cmd_C(K_UP,             nv_up,          NV_SSS|NV_STS,         FALSE          ),
        new nv_cmd_C(K_S_UP,           nv_page,        NV_SS,                 BACKWARD       ),
        new nv_cmd_C(K_DOWN,           nv_down,        NV_SSS|NV_STS,         FALSE          ),
        new nv_cmd_C(K_S_DOWN,         nv_page,        NV_SS,                 FORWARD        ),
        new nv_cmd_C(K_LEFT,           nv_left,        NV_SSS|NV_STS|NV_RL,   0              ),
        new nv_cmd_C(K_S_LEFT,         nv_bck_word,    NV_SS|NV_RL,           0              ),
        new nv_cmd_C(K_C_LEFT,         nv_bck_word,    NV_SSS|NV_RL|NV_STS,   1              ),
        new nv_cmd_C(K_RIGHT,          nv_right,       NV_SSS|NV_STS|NV_RL,   0              ),
        new nv_cmd_C(K_S_RIGHT,        nv_wordcmd,     NV_SS|NV_RL,           FALSE          ),
        new nv_cmd_C(K_C_RIGHT,        nv_wordcmd,     NV_SSS|NV_RL|NV_STS,   TRUE           ),
        new nv_cmd_C(K_PAGEUP,         nv_page,        NV_SSS|NV_STS,         BACKWARD       ),
        new nv_cmd_C(K_KPAGEUP,        nv_page,        NV_SSS|NV_STS,         BACKWARD       ),
        new nv_cmd_C(K_PAGEDOWN,       nv_page,        NV_SSS|NV_STS,         FORWARD        ),
        new nv_cmd_C(K_KPAGEDOWN,      nv_page,        NV_SSS|NV_STS,         FORWARD        ),
        new nv_cmd_C(K_END,            nv_end,         NV_SSS|NV_STS,         FALSE          ),
        new nv_cmd_C(K_KEND,           nv_end,         NV_SSS|NV_STS,         FALSE          ),
        new nv_cmd_C(K_S_END,          nv_end,         NV_SS,                 FALSE          ),
        new nv_cmd_C(K_C_END,          nv_end,         NV_SSS|NV_STS,         TRUE           ),
        new nv_cmd_C(K_HOME,           nv_home,        NV_SSS|NV_STS,         0              ),
        new nv_cmd_C(K_KHOME,          nv_home,        NV_SSS|NV_STS,         0              ),
        new nv_cmd_C(K_S_HOME,         nv_home,        NV_SS,                 0              ),
        new nv_cmd_C(K_C_HOME,         nv_goto,        NV_SSS|NV_STS,         FALSE          ),
        new nv_cmd_C(K_DEL,            nv_abbrev,      0,                     0              ),
        new nv_cmd_C(K_KDEL,           nv_abbrev,      0,                     0              ),
        new nv_cmd_C(K_UNDO,           nv_kundo,       0,                     0              ),
        new nv_cmd_C(K_SELECT,         nv_select,      0,                     0              ),
        new nv_cmd_C(K_DROP,           nv_drop,        NV_STS,                0              ),
        new nv_cmd_C(K_CURSORHOLD,     nv_cursorhold,  NV_KEEPREG,            0              ),
    };

    /* Sorted index of commands in nv_cmds[]. */
    /*private*/ static Short[] nv_cmd_idx = new Short[nv_cmds.length];

    /* The highest index for which
     * nv_cmds[idx].cmd_char == nv_cmd_idx[nv_cmds[idx].cmd_char]. */
    /*private*/ static int nv_max_linear;

    /*
     * ops.c: implementation of op_shift, op_delete, op_tilde, op_change, op_yank, do_put and do_join
     */

    /*
     * Number of registers.
     *      0 = unnamed register, for normal yanks and puts
     *   1..9 = registers '1' to '9', for deletes
     * 10..35 = registers 'a' to 'z'
     *     36 = delete register '-'
     *     37 = Selection register '*'.  Only if FEAT_CLIPBOARD defined
     *     38 = Clipboard register '+'.  Only if FEAT_CLIPBOARD and FEAT_X11 defined
     */
    /*
     * Symbolic names for some registers.
     */
    /*private*/ static final int DELETION_REGISTER       = 36;
    /*private*/ static final int STAR_REGISTER           = 37;
    /*private*/ static final int PLUS_REGISTER           = STAR_REGISTER;           /* there is only one */
    /*private*/ static final int TILDE_REGISTER          = PLUS_REGISTER + 1;

    /*private*/ static final int NUM_REGISTERS           = TILDE_REGISTER + 1;

    /*
     * Each yank register is an array of pointers to lines.
     */
    /*private*/ static final class yankreg_C
    {
        Bytes[]   y_array;            /* pointer to array of line pointers */
        int         y_size;             /* number of lines in "y_array" */
        byte        y_type;             /* MLINE, MCHAR or MBLOCK */
        int         y_width;            /* only set if y_type == MBLOCK */

        /*private*/ yankreg_C()
        {
        }
    }

    /*private*/ static void COPY_yankreg(yankreg_C y1, yankreg_C y0)
    {
        y1.y_array = y0.y_array;
        y1.y_size = y0.y_size;
        y1.y_type = y0.y_type;
        y1.y_width = y0.y_width;
    }

    /*private*/ static yankreg_C[] ARRAY_yankreg(int n)
    {
        yankreg_C[] a = new yankreg_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new yankreg_C();
        return a;
    }

    /*private*/ static yankreg_C[] y_regs = ARRAY_yankreg(NUM_REGISTERS);

    /*private*/ static yankreg_C   y_current;       /* ptr to current yankreg */
    /*private*/ static boolean     y_append;        /* true when appending */
    /*private*/ static yankreg_C   y_previous;      /* ptr to last written yankreg */

    /*
     * structure used by block_prep, op_delete and op_yank for blockwise operators
     * also op_change, op_shift, op_insert, op_replace
     */
    /*private*/ static final class block_def_C
    {
        int         startspaces;        /* 'extra' cols before first char */
        int         endspaces;          /* 'extra' cols after last char */
        int         textlen;            /* chars in block */
        Bytes       textstart;          /* pointer to 1st char (partially) in block */
        int         textcol;            /* index of chars (partially) in block */
        int         start_vcol;         /* start col of 1st char wholly inside block */
        int         end_vcol;           /* start col of 1st char wholly after block */
        boolean     is_short;           /* true if line is too short to fit in block */
        boolean     is_MAX;             /* true if curswant == MAXCOL when starting */
        boolean     is_oneChar;         /* true if block within one character */
        int         pre_whitesp;        /* screen cols of ws before block */
        int         pre_whitesp_c;      /* chars of ws before block */
        int         end_char_vcols;     /* number of vcols of post-block char */
        int         start_char_vcols;   /* number of vcols of pre-block char */

        /*private*/ block_def_C()
        {
        }
    }

    /*
     * The names of operators.
     * IMPORTANT: Index must correspond with defines in vim.h!!!
     * The third field indicates whether the operator always works on lines.
     */
    /*private*/ static byte[][/*3*/] opchars =
    {
        { NUL, NUL, FALSE },    /* OP_NOP */
        { 'd', NUL, FALSE },    /* OP_DELETE */
        { 'y', NUL, FALSE },    /* OP_YANK */
        { 'c', NUL, FALSE },    /* OP_CHANGE */
        { '<', NUL, TRUE  },    /* OP_LSHIFT */
        { '>', NUL, TRUE  },    /* OP_RSHIFT */
        { '!', NUL, TRUE  },    /* OP_FILTER */
        { 'g', '~', FALSE },    /* OP_TILDE */
        { '=', NUL, TRUE  },    /* OP_INDENT */
        { 'g', 'q', TRUE  },    /* OP_FORMAT */
        { ':', NUL, TRUE  },    /* OP_COLON */
        { 'g', 'U', FALSE },    /* OP_UPPER */
        { 'g', 'u', FALSE },    /* OP_LOWER */
        { 'J', NUL, TRUE  },    /* DO_JOIN */
        { 'g', 'J', TRUE  },    /* DO_JOIN_NS */
        { 'g', '?', FALSE },    /* OP_ROT13 */
        { 'r', NUL, FALSE },    /* OP_REPLACE */
        { 'I', NUL, FALSE },    /* OP_INSERT */
        { 'A', NUL, FALSE },    /* OP_APPEND */
        { 'g', 'w', TRUE  },    /* OP_FORMAT2 */
        { 'g', '@', FALSE },    /* OP_FUNCTION */
    };

    /*
     * Translate a command name into an operator type.
     * Must only be called with a valid operator name!
     */
    /*private*/ static int get_op_type(int char1, int char2)
    {
        if (char1 == 'r')           /* ignore second character */
            return OP_REPLACE;
        if (char1 == '~')           /* when tilde is an operator */
            return OP_TILDE;

        int i;
        for (i = 0; ; i++)
            if (opchars[i][0] == char1 && opchars[i][1] == char2)
                break;
        return i;
    }

    /*
     * Return true if operator "op" always works on whole lines.
     */
    /*private*/ static boolean op_on_lines(int op)
    {
        return (opchars[op][2] != FALSE);
    }

    /*
     * Get first operator command character.
     * Returns 'g' or 'z' if there is another command character.
     */
    /*private*/ static int get_op_char(int optype)
    {
        return opchars[optype][0];
    }

    /*
     * Get second operator command character.
     */
    /*private*/ static int get_extra_op_char(int optype)
    {
        return opchars[optype][1];
    }

    /*
     * op_shift - handle a shift operation
     */
    /*private*/ static void op_shift(oparg_C oap, boolean curs_top, int amount)
    {
        int block_col = 0;

        if (!u_save(oap.op_start.lnum - 1, oap.op_end.lnum + 1))
            return;

        if (oap.block_mode)
            block_col = curwin.w_cursor.col;

        for (long n = oap.line_count; 0 <= --n; curwin.w_cursor.lnum++)
        {
            byte c0 = ml_get_curline().at(0);
            if (c0 == NUL)                      /* empty line */
                curwin.w_cursor.col = 0;
            else if (oap.block_mode)
                shift_block(oap, amount);
            else if (c0 != '#' || !preprocs_left())
            {
                /* Move the line right if it doesn't start with '#',
                 * 'smartindent' isn't set or 'cindent' isn't set or '#' isn't in 'cino'. */
                shift_line(oap.op_type == OP_LSHIFT, p_sr[0], amount, false);
            }
        }

        changed_lines(oap.op_start.lnum, 0, oap.op_end.lnum + 1, 0L);

        if (oap.block_mode)
        {
            curwin.w_cursor.lnum = oap.op_start.lnum;
            curwin.w_cursor.col = block_col;
        }
        else if (curs_top)      /* put cursor on first line, for ">>" */
        {
            curwin.w_cursor.lnum = oap.op_start.lnum;
            beginline(BL_SOL | BL_FIX);     /* shift_line() may have set cursor.col */
        }
        else
            --curwin.w_cursor.lnum;         /* put cursor on last line, for ":>" */

        if (p_report[0] < oap.line_count)
        {
            Bytes s = (oap.op_type == OP_RSHIFT) ? u8(">") : u8("<");
            if (oap.line_count == 1)
            {
                if (amount == 1)
                    libC.sprintf(ioBuff, u8("1 line %sed 1 time"), s);
                else
                    libC.sprintf(ioBuff, u8("1 line %sed %d times"), s, amount);
            }
            else
            {
                if (amount == 1)
                    libC.sprintf(ioBuff, u8("%ld lines %sed 1 time"), oap.line_count, s);
                else
                    libC.sprintf(ioBuff, u8("%ld lines %sed %d times"), oap.line_count, s, amount);
            }
            msg(ioBuff);
        }

        /*
         * Set "'[" and "']" marks.
         */
        COPY_pos(curbuf.b_op_start, oap.op_start);
        curbuf.b_op_end.lnum = oap.op_end.lnum;
        curbuf.b_op_end.col = strlen(ml_get(oap.op_end.lnum));
        if (0 < curbuf.b_op_end.col)
            --curbuf.b_op_end.col;
    }

    /*
     * shift the current line one shiftwidth left (if left != 0) or right
     * leaves cursor on first blank in the line
     */
    /*private*/ static void shift_line(boolean left, boolean round, int amount, boolean call_changed_bytes)
        /* call_changed_bytes: call changed_bytes() */
    {
        int q_sw = (int)get_sw_value(curbuf);

        int count = get_indent();       /* get current indent */

        if (round)                      /* round off indent */
        {
            int i = count / q_sw;       /* number of "p_sw" rounded down */
            int j = count % q_sw;       /* extra spaces */
            if (j != 0 && left)              /* first remove extra spaces */
                --amount;
            if (left)
            {
                i -= amount;
                if (i < 0)
                    i = 0;
            }
            else
                i += amount;
            count = i * q_sw;
        }
        else                            /* original vi indent */
        {
            if (left)
            {
                count -= q_sw * amount;
                if (count < 0)
                    count = 0;
            }
            else
                count += q_sw * amount;
        }

        /* Set new indent. */
        if ((State & VREPLACE_FLAG) != 0)
            change_indent(INDENT_SET, count, false, NUL, call_changed_bytes);
        else
            set_indent(count, call_changed_bytes ? SIN_CHANGED : 0);
    }

    /*
     * Shift one line of the current block one shiftwidth right or left.
     * Leaves cursor on first character in block.
     */
    /*private*/ static void shift_block(oparg_C oap, int amount)
    {
        boolean left = (oap.op_type == OP_LSHIFT);

        int oldcol = curwin.w_cursor.col;
        int q_sw = (int)get_sw_value(curbuf);
        int q_ts = (int)curbuf.b_p_ts[0];

        boolean old_p_ri = p_ri[0];
        p_ri[0] = false;                   /* don't want revins in indent */

        int oldstate = State;
        State = INSERT;             /* don't want REPLACE for State */

        block_def_C bd = new block_def_C();
        block_prep(oap, bd, curwin.w_cursor.lnum, true);
        if (bd.is_short)
            return;

        /* total is number of screen columns to be inserted/removed */
        int total = amount * q_sw;
        Bytes oldp = ml_get_curline();
        Bytes newp;

        if (!left)
        {
            /*
             *  1. Get start vcol
             *  2. Total ws vcols
             *  3. Divvy into TABs & spp
             *  4. Construct new string
             */
            total += bd.pre_whitesp; /* all virtual WS upto & incl a split TAB */
            int ws_vcol = bd.start_vcol - bd.pre_whitesp;
            if (bd.startspaces != 0)
                bd.textstart = bd.textstart.plus(us_ptr2len_cc(bd.textstart));
            while (vim_iswhite(bd.textstart.at(0)))
            {
                /* TODO: is passing bd.textstart for start of the line OK? */
                int incr;
                { Bytes[] __ = { bd.textstart }; incr = lbr_chartabsize_adv(bd.textstart, __, bd.start_vcol); bd.textstart = __[0]; }
                total += incr;
                bd.start_vcol += incr;
            }
            /* OK, now total=all the VWS reqd, and textstart
             * points at the 1st non-ws char in the block. */
            int i = 0, j = total;
            if (!curbuf.b_p_et[0])
                i = ((ws_vcol % q_ts) + total) / q_ts; /* number of tabs */
            if (i != 0)
                j = ((ws_vcol % q_ts) + total) % q_ts; /* number of spp */
            /* if we're splitting a TAB, allow for it */
            bd.textcol -= bd.pre_whitesp_c - ((bd.startspaces != 0) ? 1 : 0);
            int len = strlen(bd.textstart) + 1;
            newp = new Bytes(bd.textcol + i + j + len);

            BCOPY(newp, oldp, bd.textcol);
            copy_chars(newp.plus(bd.textcol), i, TAB);
            copy_spaces(newp.plus(bd.textcol + i), j);
            BCOPY(newp, bd.textcol + i + j, bd.textstart, 0, len);
        }
        else /* left */
        {
            /*
             * Firstly, let's find the first non-whitespace character that is
             * displayed after the block's start column and the character's column
             * number.  Also, let's calculate the width of all the whitespace
             * characters that are displayed in the block and precede the searched
             * non-whitespace character.
             */

            /* If "bd.startspaces" is set, "bd.textstart" points to the character,
             * the part of which is displayed at the block's beginning.  Let's start
             * searching from the next character. */
            Bytes[] non_white = { bd.textstart };
            if (bd.startspaces != 0)
                non_white[0] = non_white[0].plus(us_ptr2len_cc(non_white[0]));

            /* The character's column is in "bd.start_vcol". */
            int non_white_col = bd.start_vcol;

            while (vim_iswhite(non_white[0].at(0)))
            {
                int incr = lbr_chartabsize_adv(bd.textstart, non_white, non_white_col);
                non_white_col += incr;
            }

            int block_space_width = non_white_col - oap.start_vcol;
            /* We will shift by "total" or "block_space_width", whichever is less. */
            int shift_amount = (block_space_width < total) ? block_space_width : total;

            /* The column to which we will shift the text. */
            int destination_col = non_white_col - shift_amount;

            /* Now let's find out how much of the beginning
             * of the line we can reuse without modification. */

            /* end of the part of the line which is copied verbatim */
            Bytes verbatim_copy_end = bd.textstart;
            /* the (displayed) width of this part of line */
            int verbatim_copy_width = bd.start_vcol;

            /* If "bd.startspaces" is set, "bd.textstart" points to the character preceding the block.
             * We have to subtract its width to obtain its column number. */
            if (bd.startspaces != 0)
                verbatim_copy_width -= bd.start_char_vcols;
            while (verbatim_copy_width < destination_col)
            {
                Bytes line = verbatim_copy_end;

                /* TODO: is passing "verbatim_copy_end" for start of the line OK? */
                int incr = lbr_chartabsize(line, verbatim_copy_end, verbatim_copy_width);
                if (destination_col < verbatim_copy_width + incr)
                    break;
                verbatim_copy_width += incr;
                verbatim_copy_end = verbatim_copy_end.plus(us_ptr2len_cc(verbatim_copy_end));
            }

            /* If "destination_col" is different from the width of the initial
             * part of the line that will be copied, it means we encountered a tab
             * character, which we will have to partly replace with spaces. */
            int fill = destination_col - verbatim_copy_width;

            /* The replacement line will consist of:
             * - the beginning of the original line up to "verbatim_copy_end",
             * - "fill" number of spaces,
             * - the rest of the line, pointed to by "non_white". */

            /* the length of the line after the block shift */
            int diff = BDIFF(verbatim_copy_end, oldp);
            int new_line_len = diff + fill + strlen(non_white[0]) + 1;

            newp = new Bytes(new_line_len);

            BCOPY(newp, oldp, diff);
            copy_spaces(newp.plus(diff), fill);
            BCOPY(newp, diff + fill, non_white[0], 0, strlen(non_white[0]) + 1);
        }

        /* replace the line */
        ml_replace(curwin.w_cursor.lnum, newp, false);
        changed_bytes(curwin.w_cursor.lnum, bd.textcol);
        State = oldstate;
        curwin.w_cursor.col = oldcol;
        p_ri[0] = old_p_ri;
    }

    /*
     * Insert string "s" (b_insert ? before : after) block.
     * Caller must prepare for undo.
     */
    /*private*/ static void block_insert(oparg_C oap, Bytes s, boolean b_insert, block_def_C bdp)
    {
        int count = 0;                                  /* extra spaces to replace a cut TAB */
        int spaces = 0;                                 /* non-zero if cutting a TAB */

        int oldstate = State;
        State = INSERT;                                 /* don't want REPLACE for State */

        int s_len = strlen(s);

        for (long lnum = oap.op_start.lnum + 1; lnum <= oap.op_end.lnum; lnum++)
        {
            block_prep(oap, bdp, lnum, true);
            if (bdp.is_short && b_insert)
                continue;                               /* OP_INSERT, line ends before block start */

            Bytes oldp = ml_get(lnum);

            int q_ts;
            int offset;
            if (b_insert)
            {
                q_ts = bdp.start_char_vcols;
                spaces = bdp.startspaces;
                if (spaces != 0)
                    count = q_ts - 1;                   /* we're cutting a TAB */
                offset = bdp.textcol;
            }
            else                                        /* append */
            {
                q_ts = bdp.end_char_vcols;
                if (!bdp.is_short)                      /* spaces = padding after block */
                {
                    spaces = (bdp.endspaces != 0) ? q_ts - bdp.endspaces : 0;
                    if (spaces != 0)
                        count = q_ts - 1;               /* we're cutting a TAB */
                    offset = bdp.textcol + bdp.textlen - ((spaces != 0) ? 1 : 0);
                }
                else                                    /* spaces = padding to block edge */
                {
                    /* if $ used, just append to EOL (ie spaces==0) */
                    if (!bdp.is_MAX)
                        spaces = (oap.end_vcol - bdp.end_vcol) + 1;
                    count = spaces;
                    offset = bdp.textcol + bdp.textlen;
                }
            }

            if (0 < spaces)
            {
                int off;

                /* Avoid starting halfway a multi-byte character. */
                if (b_insert)
                {
                    off = us_head_off(oldp, oldp.plus(offset + spaces));
                }
                else
                {
                    off = us_off_next(oldp, oldp.plus(offset));
                    offset += off;
                }
                spaces -= off;
                count -= off;
            }

            Bytes newp = new Bytes(strlen(oldp) + s_len + count + 1);

            /* copy up to shifted part */
            BCOPY(newp, oldp, offset);
            oldp = oldp.plus(offset);

            /* insert pre-padding */
            copy_spaces(newp.plus(offset), spaces);

            /* copy the new text */
            BCOPY(newp, offset + spaces, s, 0, s_len);
            offset += s_len;

            if (0 < spaces && !bdp.is_short)
            {
                /* insert post-padding */
                copy_spaces(newp.plus(offset + spaces), q_ts - spaces);
                /* We're splitting a TAB, don't copy it. */
                oldp = oldp.plus(1);
                /* We allowed for that TAB, remember this now. */
                count++;
            }

            if (0 < spaces)
                offset += count;
            BCOPY(newp, offset, oldp, 0, strlen(oldp) + 1);

            ml_replace(lnum, newp, false);

            if (lnum == oap.op_end.lnum)
            {
                /* Set "']" mark to the end of the block instead of the end of the insert in the first line. */
                curbuf.b_op_end.lnum = oap.op_end.lnum;
                curbuf.b_op_end.col = offset;
            }
        }

        changed_lines(oap.op_start.lnum + 1, 0, oap.op_end.lnum + 1, 0L);

        State = oldstate;
    }

    /*
     * op_reindent - handle reindenting a block of lines.
     */
    /*private*/ static void op_reindent(oparg_C oap, getindent_C how)
    {
        long first_changed = 0;
        long last_changed = 0;
        long start_lnum = curwin.w_cursor.lnum;

        /* Don't even try when 'modifiable' is off. */
        if (!curbuf.b_p_ma[0])
        {
            emsg(e_modifiable);
            return;
        }

        long i;
        for (i = oap.line_count; 0 <= --i && !got_int; )
        {
            /* It's a slow thing to do, so give feedback,
             * so there's no worry that the computer's just hung.
             */
            if (1 < i
                    && (i % 50 == 0 || i == oap.line_count - 1)
                    && p_report[0] < oap.line_count)
                smsg(u8("%ld lines to indent... "), i);

            /*
             * Be vi-compatible: for lisp indenting the first line is not indented,
             * unless there is only one line.
             */
            if (i != oap.line_count - 1 || oap.line_count == 1 || how != get_lisp_indent)
            {
                int count = 0;
                Bytes l = skipwhite(ml_get_curline());
                if (l.at(0) != NUL)                  /* empty or blank line */
                    count = how.getindent();              /* get the indent for this line */

                if (set_indent(count, SIN_UNDO))
                {
                    /* did change the indent, call changed_lines() later */
                    if (first_changed == 0)
                        first_changed = curwin.w_cursor.lnum;
                    last_changed = curwin.w_cursor.lnum;
                }
            }
            curwin.w_cursor.lnum++;
            curwin.w_cursor.col = 0;            /* make sure it's valid */
        }

        /* put cursor on first non-blank of indented line */
        curwin.w_cursor.lnum = start_lnum;
        beginline(BL_SOL | BL_FIX);

        /* Mark changed lines so that they will be redrawn.
         * When Visual highlighting was present, need to continue until the last line.
         * When there is no change still need to remove the Visual highlighting. */
        if (last_changed != 0)
            changed_lines(first_changed, 0,
                    oap.is_VIsual ? start_lnum + oap.line_count : last_changed + 1, 0L);
        else if (oap.is_VIsual)
            redraw_curbuf_later(INVERTED);

        if (p_report[0] < oap.line_count)
        {
            i = oap.line_count - (i + 1);
            if (i == 1)
                msg(u8("1 line indented "));
            else
                smsg(u8("%ld lines indented "), i);
        }
        /* set '[ and '] marks */
        COPY_pos(curbuf.b_op_start, oap.op_start);
        COPY_pos(curbuf.b_op_end, oap.op_end);
    }

    /*
     * Keep the last expression line here, for repeating.
     */
    /*private*/ static Bytes expr_line;

    /*
     * Get an expression for the "\"=expr1" or "CTRL-R =expr1"
     * Returns '=' when OK, NUL otherwise.
     */
    /*private*/ static int get_expr_register()
    {
        Bytes new_line = getcmdline('=', 0L, 0);
        if (new_line == null)
            return NUL;

        if (new_line.at(0) == NUL)
            ; /* use previous line */
        else
            set_expr_line(new_line);

        return '=';
    }

    /*
     * Set the expression for the '=' register.
     * Argument must be an allocated string.
     */
    /*private*/ static void set_expr_line(Bytes new_line)
    {
        expr_line = new_line;
    }

    /*private*/ static int __nested;

    /*
     * Get the result of the '=' register expression.
     * Returns a pointer to allocated memory, or null for failure.
     */
    /*private*/ static Bytes get_expr_line()
    {
        if (expr_line == null)
            return null;

        /* Make a copy of the expression, because evaluating it may cause it to be changed. */
        Bytes expr_copy = STRDUP(expr_line);

        /* When we are invoked recursively limit the evaluation to 10 levels.
         * Then return the string as-is. */
        if (10 <= __nested)
            return expr_copy;

        __nested++;
        Bytes rv = eval_to_string(expr_copy, null, true);
        --__nested;
        return rv;
    }

    /*
     * Get the '=' register expression itself, without evaluating it.
     */
    /*private*/ static Bytes get_expr_line_src()
    {
        if (expr_line == null)
            return null;

        return STRDUP(expr_line);
    }

    /*
     * Check if 'regname' is a valid name of a yank register.
     * Note: There is no check for 0 (default register), caller should do this
     */
    /*private*/ static boolean valid_yank_reg(int regname, boolean writing)
        /* writing: if true check for writable registers */
    {
        if ((0 < regname && asc_isalnum(regname))
                || (!writing && vim_strchr(u8("/.%:="), regname) != null)
                || regname == '#'
                || regname == '"'
                || regname == '-'
                || regname == '_'
                || regname == '*'
                || regname == '+'
                || (!writing && regname == '~'))
            return true;

        return false;
    }

    /*
     * Set y_current and y_append, according to the value of "regname".
     * Cannot handle the '_' register.
     * Must only be called with a valid register name!
     *
     * If regname is 0 and writing, use register 0.
     * If regname is 0 and reading, use previous register.
     */
    /*private*/ static void get_yank_register(int regname, boolean writing)
    {
        y_append = false;

        if ((regname == 0 || regname == '"') && !writing && y_previous != null)
        {
            y_current = y_previous;
            return;
        }

        int i = regname;
        if (asc_isdigit(i))
            i -= '0';
        else if (asc_islower(i))
            i = charOrdLow(i) + 10;
        else if (asc_isupper(i))
        {
            i = charOrdUp(i) + 10;
            y_append = true;
        }
        else if (regname == '-')
            i = DELETION_REGISTER;
        /* When selection is not available, use register 0 instead of '*'. */
        else if (clip_star.available && regname == '*')
            i = STAR_REGISTER;
        /* When clipboard is not available, use register 0 instead of '+'. */
        else if (clip_plus.available && regname == '+')
            i = PLUS_REGISTER;
        else if (!writing && regname == '~')
            i = TILDE_REGISTER;
        else                /* not 0-9, a-z, A-Z or '-': use register 0 */
            i = 0;

        y_current = y_regs[i];

        if (writing)        /* remember the register we write into for do_put() */
            y_previous = y_current;
    }

    /*
     * When "regname" is a clipboard register, obtain the selection.
     * If it's not available return zero, otherwise return "regname".
     */
    /*private*/ static int may_get_selection(int regname)
    {
        if (regname == '*')
        {
            if (!clip_star.available)
                regname = 0;
            else
                clip_get_selection(clip_star);
        }
        else if (regname == '+')
        {
            if (!clip_plus.available)
                regname = 0;
            else
                clip_get_selection(clip_plus);
        }
        return regname;
    }

    /*
     * Obtain the contents of a "normal" register.  The register is made empty.
     * The returned pointer has allocated memory, use put_register() later.
     */
    /*private*/ static yankreg_C get_register(int name, boolean copy)
        /* copy: make a copy, if false make register empty. */
    {
        /* When Visual area changed, may have to update selection.  Obtain the selection too. */
        if (name == '*' && clip_star.available)
        {
            if (clip_isautosel_star())
                clip_update_selection(clip_star);
            may_get_selection(name);
        }
        if (name == '+' && clip_plus.available)
        {
            if (clip_isautosel_plus())
                clip_update_selection(clip_plus);
            may_get_selection(name);
        }

        get_yank_register(name, false);

        yankreg_C reg = new yankreg_C();

        COPY_yankreg(reg, y_current);
        if (copy)
        {
            /* If we run out of memory some or all of the lines are empty. */
            if (reg.y_size == 0)
                reg.y_array = null;
            else
            {
                reg.y_array = new Bytes[reg.y_size];

                for (int i = 0; i < reg.y_size; i++)
                    reg.y_array[i] = STRDUP(y_current.y_array[i]);
            }
        }
        else
            y_current.y_array = null;

        return reg;
    }

    /*
     * Put "reg" into register "name".  Free any previous contents and "reg".
     */
    /*private*/ static void put_register(int name, yankreg_C reg)
    {
        get_yank_register(name, false);
        y_current.y_array = null;
        COPY_yankreg(y_current, reg);

        /* Send text written to clipboard register to the clipboard. */
        may_set_selection();
    }

    /*
     * return true if the current yank register has type MLINE
     */
    /*private*/ static boolean yank_register_mline(int regname)
    {
        if (regname != 0 && !valid_yank_reg(regname, false))
            return false;
        if (regname == '_')         /* black hole is always empty */
            return false;
        get_yank_register(regname, false);
        return (y_current.y_type == MLINE);
    }

    /*private*/ static int rec__regname;

    /*
     * Start or stop recording into a yank register.
     *
     * Return false for failure, true otherwise.
     */
    /*private*/ static boolean do_record(int c)
    {
        boolean retval;

        if (Recording == false)         /* start recording */
        {
            /* registers 0-9, a-z and " are allowed */
            if (c < 0 || (!asc_isalnum(c) && c != '"'))
                retval = false;
            else
            {
                Recording = true;
                showmode();
                rec__regname = c;
                retval = true;
            }
        }
        else                            /* stop recording */
        {
            /*
             * Get the recorded key hits.
             * KB_SPECIAL and CSI will be escaped, this needs to be removed again to put it in a register.
             * exec_reg then adds the escaping back later.
             */
            Recording = false;
            msg(u8(""));
            Bytes p = get_recorded();
            if (p == null)
                retval = false;
            else
            {
                /* Remove escaping for CSI and KB_SPECIAL in multi-byte chars. */
                vim_unescape_csi(p);

                /*
                 * We don't want to change the default register here,
                 * so save and restore the current register name.
                 */
                yankreg_C old_y_previous = y_previous;
                yankreg_C old_y_current = y_current;

                retval = stuff_yank(rec__regname, p);

                y_previous = old_y_previous;
                y_current = old_y_current;
            }
        }

        return retval;
    }

    /*
     * Stuff string "p" into yank register "regname" as a single line (append if uppercase).
     * "p" must have been alloced.
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean stuff_yank(int regname, Bytes p)
    {
        /* check for read-only register */
        if (regname != 0 && !valid_yank_reg(regname, true))
            return false;
        if (regname == '_')             /* black hole: don't do anything */
            return true;

        get_yank_register(regname, true);

        if (y_append && y_current.y_array != null)
        {
            Bytes[] a = y_current.y_array;
            int i = y_current.y_size - 1;

            Bytes lp = new Bytes(strlen(a[i]) + strlen(p) + 1);
            STRCPY(lp, a[i]);
            STRCAT(lp, p);
            a[i] = lp;
        }
        else
        {
            y_current.y_array = new Bytes[1];
            y_current.y_array[0] = p;
            y_current.y_size = 1;
            y_current.y_type = MCHAR;   /* used to be MLINE, why? */
        }

        return true;
    }

    /*private*/ static int execreg_lastc = NUL;

    /*
     * execute a yank register: copy it into the stuff buffer
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean do_execreg(int regname, boolean colon, boolean addcr, boolean silent)
        /* colon: insert ':' before each line */
        /* addcr: always add '\n' to end of line */
        /* silent: set "silent" flag in typeahead buffer */
    {
        boolean retval = true;

        if (regname == '@')                 /* repeat previous one */
        {
            if (execreg_lastc == NUL)
            {
                emsg(u8("E748: No previously used register"));
                return false;
            }
            regname = execreg_lastc;
        }
                                            /* check for valid regname */
        if (regname == '%' || regname == '#' || !valid_yank_reg(regname, false))
        {
            emsg_invreg(regname);
            return false;
        }
        execreg_lastc = regname;

        regname = may_get_selection(regname);

        if (regname == '_')                 /* black hole: don't stuff anything */
            return true;

        if (regname == ':')                 /* use last command line */
        {
            if (last_cmdline == null)
            {
                emsg(e_nolastcmd);
                return false;
            }
            new_last_cmdline = null;        /* don't keep the cmdline containing @: */
            /* Escape all control characters with a CTRL-V. */
            Bytes p = vim_strsave_escaped_ext(last_cmdline, u8("\001\002\003\004\005\006\007\010\011\012\013\014\015\016\017\020\021\022\023\024\025\026\027\030\031\032\033\034\035\036\037"), Ctrl_V);

            /* When in Visual mode "'<,'>" will be prepended to the command.
             * Remove it when it's already there. */
            if (VIsual_active && STRNCMP(p, u8("'<,'>"), 5) == 0)
                retval = put_in_typebuf(p.plus(5), true, true, silent);
            else
                retval = put_in_typebuf(p, true, true, silent);
        }
        else if (regname == '=')
        {
            Bytes p = get_expr_line();
            if (p == null)
                return false;

            retval = put_in_typebuf(p, true, colon, silent);
        }
        else if (regname == '.')            /* use last inserted text */
        {
            Bytes p = get_last_insert_save();
            if (p == null)
            {
                emsg(e_noinstext);
                return false;
            }
            retval = put_in_typebuf(p, false, colon, silent);
        }
        else
        {
            get_yank_register(regname, false);
            if (y_current.y_array == null)
                return false;

            /* Disallow remaping for ":@r". */
            int remap = colon ? REMAP_NONE : REMAP_YES;

            /*
             * Insert lines into typeahead buffer, from last one to first one.
             */
            put_reedit_in_typebuf(silent);

            for (int i = y_current.y_size; 0 <= --i; )
            {
                /* insert NL between lines and after last line if type is MLINE */
                if (y_current.y_type == MLINE || i < y_current.y_size - 1 || addcr)
                    if (!ins_typebuf(u8("\n"), remap, 0, true, silent))
                        return false;

                Bytes escaped = vim_strsave_escape_csi(y_current.y_array[i]);

                retval = ins_typebuf(escaped, remap, 0, true, silent);

                if (!retval)
                    return false;

                if (colon && !ins_typebuf(u8(":"), remap, 0, true, silent))
                    return false;
            }
            execReg = true;         /* disable the 'q' command */
        }

        return retval;
    }

    /*
     * If "restart_edit" is not zero, put it in the typeahead buffer, so that it's
     * used only after other typeahead has been processed.
     */
    /*private*/ static void put_reedit_in_typebuf(boolean silent)
    {
        Bytes buf = new Bytes(3);

        if (restart_edit != NUL)
        {
            if (restart_edit == 'V')
            {
                buf.be(0, (byte)'g');
                buf.be(1, (byte)'R');
                buf.be(2, NUL);
            }
            else
            {
                buf.be(0, (restart_edit == 'I') ? (byte)'i' : restart_edit);
                buf.be(1, NUL);
            }
            if (ins_typebuf(buf, REMAP_NONE, 0, true, silent))
                restart_edit = NUL;
        }
    }

    /*
     * Insert register contents "s" into the typeahead buffer, so that it will be executed again.
     * When "esc" is true it is to be taken literally: escape CSI characters and no remapping.
     */
    /*private*/ static boolean put_in_typebuf(Bytes s, boolean esc, boolean colon, boolean silent)
        /* colon: add ':' before the line */
    {
        boolean retval = true;

        put_reedit_in_typebuf(silent);

        if (colon)
            retval = ins_typebuf(u8("\n"), REMAP_NONE, 0, true, silent);
        if (retval)
        {
            Bytes p = (esc) ? vim_strsave_escape_csi(s) : s;

            if (p == null)
                retval = false;
            else
                retval = ins_typebuf(p, esc ? REMAP_NONE : REMAP_YES, 0, true, silent);
        }
        if (colon && retval)
            retval = ins_typebuf(u8(":"), REMAP_NONE, 0, true, silent);

        return retval;
    }

    /*
     * Insert a yank register: copy it into the Read buffer.
     * Used by CTRL-R command and middle mouse button in insert mode.
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean insert_reg(int regname, boolean literally)
        /* literally: insert literally, not as if typed */
    {
        boolean retval = true;

        /*
         * It is possible to get into an endless loop by having CTRL-R a in
         * register a and then, in insert mode, doing CTRL-R a.
         * If you hit CTRL-C, the loop will be broken here.
         */
        ui_breakcheck();
        if (got_int)
            return false;

        /* check for valid regname */
        if (regname != NUL && !valid_yank_reg(regname, false))
            return false;

        regname = may_get_selection(regname);

        Bytes[] arg = new Bytes[1];
        boolean[] allocated = new boolean[1];

        if (regname == '.')                 /* insert last inserted text */
            retval = stuff_inserted(NUL, 1L, true);
        else if (get_spec_reg(regname, arg, allocated, true))
        {
            if (arg[0] == null)
                return false;
            stuffescaped(arg[0], literally);
        }
        else                                /* name or number register */
        {
            get_yank_register(regname, false);
            if (y_current.y_array == null)
                retval = false;
            else
            {
                for (int i = 0; i < y_current.y_size; i++)
                {
                    stuffescaped(y_current.y_array[i], literally);
                    /*
                     * Insert a newline between lines and after last line if y_type is MLINE.
                     */
                    if (y_current.y_type == MLINE || i < y_current.y_size - 1)
                        stuffcharReadbuff('\n');
                }
            }
        }

        return retval;
    }

    /*
     * Stuff a string into the typeahead buffer, such that edit() will insert it
     * literally ("literally" true) or interpret is as typed characters.
     */
    /*private*/ static void stuffescaped(Bytes _arg, boolean literally)
    {
        Bytes[] arg = { _arg };
        while (arg[0].at(0) != NUL)
        {
            /* Stuff a sequence of normal ASCII characters, that's fast.
             * Also stuff KB_SPECIAL to get the effect of a special key when "literally" is true. */
            Bytes start = arg[0];
            while ((' ' <= arg[0].at(0) && arg[0].at(0) < DEL) || (arg[0].at(0) == KB_SPECIAL && !literally))
                arg[0] = arg[0].plus(1);
            if (BLT(start, arg[0]))
                stuffReadbuffLen(start, BDIFF(arg[0], start));

            /* stuff a single special character */
            if (arg[0].at(0) != NUL)
            {
                int c = us_ptr2char_adv(arg, false);
                if (literally && ((c < ' ' && c != TAB) || c == DEL))
                    stuffcharReadbuff(Ctrl_V);
                stuffcharReadbuff(c);
            }
        }
    }

    /*
     * If "regname" is a special register, return true and store a pointer to its value in "argp".
     */
    /*private*/ static boolean get_spec_reg(int regname, Bytes[] argp, boolean[] allocated, boolean errmsg)
        /* allocated: return: true when value was allocated */
        /* errmsg: give error message when failing */
    {
        argp[0] = null;
        allocated[0] = false;

        switch (regname)
        {
            case '%':                                       /* file name */
                if (errmsg)
                    check_fname();                          /* will give emsg if not set */
                argp[0] = curbuf.b_fname;
                return true;

            case '#':                                       /* alternate file name */
                argp[0] = getaltfname(errmsg);                /* may give emsg if not set */
                return true;

            case '=':                                       /* result of expression */
                argp[0] = get_expr_line();
                allocated[0] = true;
                return true;

            case ':':                                       /* last command line */
                if (last_cmdline == null && errmsg)
                    emsg(e_nolastcmd);
                argp[0] = last_cmdline;
                return true;

            case '/':                                       /* last search-pattern */
                if (last_search_pat() == null && errmsg)
                    emsg(e_noprevre);
                argp[0] = last_search_pat();
                return true;

            case '.':                                       /* last inserted text */
                argp[0] = get_last_insert_save();
                allocated[0] = true;
                if (argp[0] == null && errmsg)
                    emsg(e_noinstext);
                return true;

            case Ctrl_W:                                    /* word under cursor */
            case Ctrl_A:                                    /* WORD (mnemonic All) under cursor */
            {
                if (!errmsg)
                    return false;
                int cnt = find_ident_under_cursor(argp, regname == Ctrl_W ? (FIND_IDENT|FIND_STRING) : FIND_STRING);
                argp[0] = (cnt != 0) ? STRNDUP(argp[0], cnt) : null;
                allocated[0] = true;
                return true;
            }

            case '_':               /* black hole: always empty */
                argp[0] = u8("");
                return true;
        }

        return false;
    }

    /*
     * Paste a yank register into the command line.
     * Only for non-special registers.
     * Used by CTRL-R command in command-line mode
     * insert_reg() can't be used here, because special characters from the
     * register contents will be interpreted as commands.
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean cmdline_paste_reg(int regname, boolean literally, boolean remcr)
        /* literally: Insert text literally instead of "as typed" */
        /* remcr: don't add trailing CR */
    {
        get_yank_register(regname, false);
        if (y_current.y_array == null)
            return false;

        for (int i = 0; i < y_current.y_size; i++)
        {
            cmdline_paste_str(y_current.y_array[i], literally);

            /* Insert ^M between lines and after last line if type is MLINE.
             * Don't do this when "remcr" is true and the next line is empty. */
            if (y_current.y_type == MLINE
                    || (i < y_current.y_size - 1
                        && !(remcr
                            && i == y_current.y_size - 2
                            && y_current.y_array[i + 1].at(0) == NUL)))
                cmdline_paste_str(u8("\r"), literally);

            /* Check for CTRL-C in case someone tries to paste
             * a few thousand lines and gets bored. */
            ui_breakcheck();
            if (got_int)
                return false;
        }

        return true;
    }

    /*
     * Adjust the register name "reg" for the clipboard being used always and the clipboard being available.
     */
    /*private*/ static int adjust_clip_reg(int reg)
    {
        /* If no reg. specified, and "unnamed" or "unnamedplus" is in 'clipboard',
         * use '*' or '+' reg, respectively.  "unnamedplus" prevails. */
        if (reg == 0 && (clip_unnamed != 0 || clip_unnamed_saved != 0))
        {
            if (clip_unnamed != 0)
                reg = ((clip_unnamed & CLIP_UNNAMED_PLUS) != 0 && clip_plus.available) ? '+' : '*';
            else
                reg = ((clip_unnamed_saved & CLIP_UNNAMED_PLUS) != 0 && clip_plus.available) ? '+' : '*';
        }
        if (!clip_star.available && reg == '*')
            reg = 0;
        if (!clip_plus.available && reg == '+')
            reg = 0;

        return reg;
    }

    /*
     * Handle a delete operation.
     *
     * Return false if undo failed, true otherwise.
     */
    /*private*/ static boolean op_delete(oparg_C oap)
    {
        long old_lcount = curbuf.b_ml.ml_line_count;
        boolean did_yank = false;
        int orig_regname = oap.regname;

        if ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0) /* nothing to do */
            return true;

        /* Nothing to delete, return here.  Do prepare undo, for op_change(). */
        if (oap.empty)
            return u_save_cursor();

        if (!curbuf.b_p_ma[0])
        {
            emsg(e_modifiable);
            return false;
        }

        oap.regname = adjust_clip_reg(oap.regname);

        mb_adjust_opend(oap);

        /*
         * Imitate the strange Vi behaviour: If the delete spans more than one
         * line and motion_type == MCHAR and the result is a blank line, make the
         * delete linewise.  Don't do this for the change command or Visual mode.
         */
        if (       oap.motion_type == MCHAR
                && !oap.is_VIsual
                && !oap.block_mode
                && 1 < oap.line_count
                && oap.motion_force == NUL
                && oap.op_type == OP_DELETE)
        {
            Bytes ptr = ml_get(oap.op_end.lnum).plus(oap.op_end.col);
            if (ptr.at(0) != NUL && oap.inclusive)
                ptr = ptr.plus(1);
            ptr = skipwhite(ptr);
            if (ptr.at(0) == NUL && inindent(0))
                oap.motion_type = MLINE;
        }

        setmarks:
        {
            /*
             * Check for trying to delete (e.g. "D") in an empty line.
             * Note: For the change operator it is ok.
             */
            if (oap.motion_type == MCHAR
                    && oap.line_count == 1
                    && oap.op_type == OP_DELETE
                    && ml_get(oap.op_start.lnum).at(0) == NUL)
            {
                /*
                 * It's an error to operate on an empty region,
                 * when 'E' included in 'cpoptions' (Vi compatible).
                 */
                if (virtual_op != FALSE)
                    /* Virtual editing: nothing gets deleted,
                     * but we set the '[ and '] marks as if it happened. */
                    break setmarks;
                if (vim_strbyte(p_cpo[0], CPO_EMPTYREGION) != null)
                    beep_flush();
                return true;
            }

            /*
             * Do a yank of whatever we're about to delete.
             * If a yank register was specified, put the deleted text into that register.
             * For the black hole register '_' don't yank anything.
             */
            if (oap.regname != '_')
            {
                if (oap.regname != 0)
                {
                    /* check for read-only register */
                    if (!valid_yank_reg(oap.regname, true))
                    {
                        beep_flush();
                        return true;
                    }
                    get_yank_register(oap.regname, true);       /* yank into specif'd reg. */
                    if (op_yank(oap, true, false) == true)      /* yank without message */
                        did_yank = true;
                }

                /*
                 * Put deleted text into register 1 and shift number registers if the
                 * delete contains a line break, or when a regname has been specified.
                 * Use the register name from before adjust_clip_reg() may have changed it.
                 */
                if (orig_regname != 0 || oap.motion_type == MLINE || 1 < oap.line_count || oap.use_reg_one)
                {
                    y_current = y_regs[9];
                    y_current.y_array = null;               /* free register nine */
                    for (int n = 9; 1 < n; --n)
                        COPY_yankreg(y_regs[n], y_regs[n - 1]);
                    y_previous = y_current = y_regs[1];
                    y_regs[1].y_array = null;               /* set register one to empty */
                    if (op_yank(oap, true, false) == true)
                        did_yank = true;
                }

                /* Yank into small delete register when no named register specified
                 * and the delete is within one line. */
                if ((((clip_unnamed & CLIP_UNNAMED) != 0 && oap.regname == '*')
                    || ((clip_unnamed & CLIP_UNNAMED_PLUS) != 0 && oap.regname == '+')
                    || oap.regname == 0)
                        && oap.motion_type != MLINE && oap.line_count == 1)
                {
                    oap.regname = '-';
                    get_yank_register(oap.regname, true);
                    if (op_yank(oap, true, false) == true)
                        did_yank = true;
                    oap.regname = 0;
                }

                /*
                 * If there's too much stuff to fit in the yank register, then get a
                 * confirmation before doing the delete.  This is crude, but simple.
                 * And it avoids doing a delete of something we can't put back if we want.
                 */
                if (!did_yank)
                {
                    int msg_silent_save = msg_silent;

                    msg_silent = 0;     /* must display the prompt */
                    int n = ask_yesno(u8("cannot yank; delete anyway"), true);
                    msg_silent = msg_silent_save;
                    if (n != 'y')
                    {
                        emsg(e_abort);
                        return false;
                    }
                }
            }

            /*
             * block mode delete
             */
            if (oap.block_mode)
            {
                if (!u_save(oap.op_start.lnum - 1, oap.op_end.lnum + 1))
                    return false;

                block_def_C bd = new block_def_C();
                for (long lnum = curwin.w_cursor.lnum; lnum <= oap.op_end.lnum; lnum++)
                {
                    block_prep(oap, bd, lnum, true);
                    if (bd.textlen == 0)    /* nothing to delete */
                        continue;

                    /* Adjust cursor position for tab replaced by spaces and 'lbr'. */
                    if (lnum == curwin.w_cursor.lnum)
                    {
                        curwin.w_cursor.col = bd.textcol + bd.startspaces;
                        curwin.w_cursor.coladd = 0;
                    }

                    /* n == number of chars deleted
                     * If we delete a TAB, it may be replaced by several characters.
                     * Thus the number of characters may increase!
                     */
                    int n = bd.textlen - bd.startspaces - bd.endspaces;
                    Bytes oldp = ml_get(lnum);
                    Bytes newp = new Bytes(strlen(oldp) + 1 - n);

                    /* copy up to deleted part */
                    BCOPY(newp, oldp, bd.textcol);
                    /* insert spaces */
                    copy_spaces(newp.plus(bd.textcol), bd.startspaces + bd.endspaces);
                    /* copy the part after the deleted part */
                    oldp = oldp.plus(bd.textcol + bd.textlen);
                    BCOPY(newp, bd.textcol + bd.startspaces + bd.endspaces, oldp, 0, strlen(oldp) + 1);
                    /* replace the line */
                    ml_replace(lnum, newp, false);
                }

                check_cursor_col();
                changed_lines(curwin.w_cursor.lnum, curwin.w_cursor.col, oap.op_end.lnum + 1, 0L);
                oap.line_count = 0;     /* no lines deleted */
            }
            else if (oap.motion_type == MLINE)
            {
                if (oap.op_type == OP_CHANGE)
                {
                    /* Delete the lines except the first one.  Temporarily move the
                     * cursor to the next line.  Save the current line number, if the
                     * last line is deleted it may be changed.
                     */
                    if (1 < oap.line_count)
                    {
                        long lnum = curwin.w_cursor.lnum;
                        curwin.w_cursor.lnum++;
                        del_lines(oap.line_count - 1, true);
                        curwin.w_cursor.lnum = lnum;
                    }
                    if (!u_save_cursor())
                        return false;
                    if (curbuf.b_p_ai[0])                  /* don't delete indent */
                    {
                        beginline(BL_WHITE);            /* cursor on first non-white */
                        did_ai = true;                  /* delete the indent when ESC hit */
                        ai_col = curwin.w_cursor.col;
                    }
                    else
                        beginline(0);                   /* cursor in column 0 */
                    truncate_line(false);               /* delete the rest of the line */
                                                        /* leave cursor past last char in line */
                    if (1 < oap.line_count)
                        u_clearline();                  /* "U" command not possible after "2cc" */
                }
                else
                {
                    del_lines(oap.line_count, true);
                    beginline(BL_WHITE | BL_FIX);
                    u_clearline();                      /* "U" command not possible after "dd" */
                }
            }
            else
            {
                if (virtual_op != FALSE)
                {
                    int endcol = 0;

                    /* For virtualedit: break the tabs that are partly included. */
                    if (gchar_pos(oap.op_start) == '\t')
                    {
                        if (!u_save_cursor())       /* save first line for undo */
                            return false;
                        if (oap.line_count == 1)
                            endcol = getviscol2(oap.op_end.col, oap.op_end.coladd);
                        coladvance_force(getviscol2(oap.op_start.col, oap.op_start.coladd));
                        COPY_pos(oap.op_start, curwin.w_cursor);
                        if (oap.line_count == 1)
                        {
                            coladvance(endcol);
                            oap.op_end.col = curwin.w_cursor.col;
                            oap.op_end.coladd = curwin.w_cursor.coladd;
                            COPY_pos(curwin.w_cursor, oap.op_start);
                        }
                    }

                    /* Break a tab only when it's included in the area. */
                    if (gchar_pos(oap.op_end) == '\t' && oap.op_end.coladd < (oap.inclusive ? 1 : 0))
                    {
                        /* save last line for undo */
                        if (!u_save(oap.op_end.lnum - 1, oap.op_end.lnum + 1))
                            return false;
                        COPY_pos(curwin.w_cursor, oap.op_end);
                        coladvance_force(getviscol2(oap.op_end.col, oap.op_end.coladd));
                        COPY_pos(oap.op_end, curwin.w_cursor);
                        COPY_pos(curwin.w_cursor, oap.op_start);
                    }
                }

                if (oap.line_count == 1)            /* delete characters within one line */
                {
                    if (!u_save_cursor())           /* save line for undo */
                        return false;

                    /* if 'cpoptions' contains '$', display '$' at end of change */
                    if (vim_strbyte(p_cpo[0], CPO_DOLLAR) != null
                            && oap.op_type == OP_CHANGE
                            && oap.op_end.lnum == curwin.w_cursor.lnum
                            && !oap.is_VIsual)
                        display_dollar(oap.op_end.col - (!oap.inclusive ? 1 : 0));

                    int n = oap.op_end.col - oap.op_start.col + 1 - (!oap.inclusive ? 1 : 0);

                    if (virtual_op != FALSE)
                    {
                        /* fix up things for virtualedit-delete:
                         * break the tabs which are going to get in our way
                         */
                        Bytes curline = ml_get_curline();
                        int len = strlen(curline);

                        if (oap.op_end.coladd != 0
                                && len - 1 <= oap.op_end.col
                                && !(oap.op_start.coladd != 0 && len - 1 <= oap.op_end.col))
                            n++;
                        /* Delete at least one char (e.g, when on a control char). */
                        if (n == 0 && oap.op_start.coladd != oap.op_end.coladd)
                            n = 1;

                        /* When deleted a char in the line, reset coladd. */
                        if (gchar_cursor() != NUL)
                            curwin.w_cursor.coladd = 0;
                    }
                    if (oap.op_type == OP_DELETE
                            && oap.inclusive
                            && oap.op_end.lnum == curbuf.b_ml.ml_line_count
                            && strlen(ml_get(oap.op_end.lnum)) < n)
                    {
                        /* Special case: gH<Del> deletes the last line. */
                        del_lines(1L, false);
                    }
                    else
                    {
                        del_bytes(n, virtual_op == FALSE, oap.op_type == OP_DELETE && !oap.is_VIsual);
                    }
                }
                else                                    /* delete characters between lines */
                {
                    /* save deleted and changed lines for undo */
                    if (!u_save(curwin.w_cursor.lnum - 1, curwin.w_cursor.lnum + oap.line_count))
                        return false;

                    boolean delete_last_line = (oap.op_end.lnum == curbuf.b_ml.ml_line_count);
                    truncate_line(true);                /* delete from cursor to end of line */

                    pos_C curpos = new pos_C();
                    COPY_pos(curpos, curwin.w_cursor); /* remember curwin.w_cursor */

                    curwin.w_cursor.lnum++;
                    del_lines(oap.line_count - 2, false);

                    if (delete_last_line)
                        oap.op_end.lnum = curbuf.b_ml.ml_line_count;

                    int n = (oap.op_end.col + 1 - (!oap.inclusive ? 1 : 0));
                    if (oap.inclusive && delete_last_line && strlen(ml_get(oap.op_end.lnum)) < n)
                    {
                        /* Special case: gH<Del> deletes the last line. */
                        del_lines(1L, false);
                        COPY_pos(curwin.w_cursor, curpos); /* restore curwin.w_cursor */
                        if (curwin.w_cursor.lnum > curbuf.b_ml.ml_line_count)
                            curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
                    }
                    else
                    {
                        /* delete from start of line until op_end */
                        curwin.w_cursor.col = 0;
                        del_bytes(n, virtual_op == FALSE, oap.op_type == OP_DELETE && !oap.is_VIsual);
                        COPY_pos(curwin.w_cursor, curpos); /* restore curwin.w_cursor */
                    }
                    if (curwin.w_cursor.lnum < curbuf.b_ml.ml_line_count)
                        do_join(2, false, false, false, false);
                }
            }

            msgmore(curbuf.b_ml.ml_line_count - old_lcount);
        }

        if (oap.block_mode)
        {
            curbuf.b_op_end.lnum = oap.op_end.lnum;
            curbuf.b_op_end.col = oap.op_start.col;
        }
        else
            COPY_pos(curbuf.b_op_end, oap.op_start);
        COPY_pos(curbuf.b_op_start, oap.op_start);

        return true;
    }

    /*
     * Adjust end of operating area for ending on a multi-byte character.
     * Used for deletion.
     */
    /*private*/ static void mb_adjust_opend(oparg_C oap)
    {
        if (oap.inclusive)
        {
            Bytes p = ml_get(oap.op_end.lnum);
            oap.op_end.col += us_tail_off(p, p.plus(oap.op_end.col));
        }
    }

    /*
     * Replace a whole area with one character.
     */
    /*private*/ static boolean op_replace(oparg_C oap, int c)
    {
        Bytes after_p = null;
        boolean had_ctrl_v_cr = (c == -1 || c == -2);

        if ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0 || oap.empty)
            return true;            /* nothing to do */

        if (had_ctrl_v_cr)
            c = (c == -1) ? '\r' : '\n';

        mb_adjust_opend(oap);

        if (!u_save(oap.op_start.lnum - 1, oap.op_end.lnum + 1))
            return false;

        /*
         * block mode replace
         */
        if (oap.block_mode)
        {
            block_def_C bd = new block_def_C();

            bd.is_MAX = (curwin.w_curswant == MAXCOL);
            for ( ; curwin.w_cursor.lnum <= oap.op_end.lnum; curwin.w_cursor.lnum++)
            {
                curwin.w_cursor.col = 0;    /* make sure cursor position is valid */
                block_prep(oap, bd, curwin.w_cursor.lnum, true);
                if (bd.textlen == 0 && (virtual_op == FALSE || bd.is_MAX))
                    continue;               /* nothing to replace */

                /* n == number of extra chars required
                 * If we split a TAB, it may be replaced by several characters.
                 * Thus the number of characters may increase!
                 */
                /* If the range starts in virtual space,
                 * count the initial coladd offset as part of "startspaces". */
                int n;
                if (virtual_op != FALSE && bd.is_short && bd.textstart.at(0) == NUL)
                {
                    pos_C vpos = new pos_C();

                    vpos.lnum = curwin.w_cursor.lnum;
                    getvpos(vpos, oap.start_vcol);
                    bd.startspaces += vpos.coladd;
                    n = bd.startspaces;
                }
                else
                    /* allow for pre spaces */
                    n = (bd.startspaces != 0) ? bd.start_char_vcols - 1 : 0;

                /* allow for post spp */
                n += (bd.endspaces != 0 && !bd.is_oneChar && 0 < bd.end_char_vcols)
                        ? bd.end_char_vcols - 1 : 0;
                /* Figure out how many characters to replace. */
                int numc = oap.end_vcol - oap.start_vcol + 1;
                if (bd.is_short && (virtual_op == FALSE || bd.is_MAX))
                    numc -= (oap.end_vcol - bd.end_vcol) + 1;

                /* A double-wide character can be replaced only up to half the times. */
                if (1 < utf_char2cells(c))
                {
                    if ((numc & 1) != 0 && !bd.is_short)
                    {
                        bd.endspaces++;
                        n++;
                    }
                    numc = numc / 2;
                }

                /* Compute bytes needed, move character count to num_chars. */
                int num_chars = numc;
                numc *= utf_char2len(c);
                /* oldlen includes textlen, so don't double count */
                n += numc - bd.textlen;

                Bytes oldp = ml_get_curline();
                int oldlen = strlen(oldp);
                Bytes newp = new Bytes(oldlen + 1 + n);

                /* copy up to deleted part */
                BCOPY(newp, oldp, bd.textcol);
                oldp = oldp.plus(bd.textcol + bd.textlen);
                /* insert pre-spaces */
                copy_spaces(newp.plus(bd.textcol), bd.startspaces);
                /* insert replacement chars CHECK FOR ALLOCATED SPACE */
                /* -1/-2 is used for entering CR literally. */
                if (had_ctrl_v_cr || (c != '\r' && c != '\n'))
                {
                    n = strlen(newp);
                    while (0 <= --num_chars)
                        n += utf_char2bytes(c, newp.plus(n));

                    if (!bd.is_short)
                    {
                        /* insert post-spaces */
                        copy_spaces(newp.plus(strlen(newp)), bd.endspaces);
                        /* copy the part after the changed part */
                        BCOPY(newp, strlen(newp), oldp, 0, strlen(oldp) + 1);
                    }
                }
                else
                {
                    /* Replacing with \r or \n means splitting the line. */
                    after_p = new Bytes(oldlen + 1 + n - strlen(newp));
                    BCOPY(after_p, oldp, strlen(oldp) + 1);
                }
                /* replace the line */
                ml_replace(curwin.w_cursor.lnum, newp, false);
                if (after_p != null)
                {
                    ml_append(curwin.w_cursor.lnum++, after_p, 0, false);
                    appended_lines_mark(curwin.w_cursor.lnum, 1L);
                    oap.op_end.lnum++;
                }
            }
        }
        else
        {
            /*
             * MCHAR and MLINE motion replace.
             */
            if (oap.motion_type == MLINE)
            {
                oap.op_start.col = 0;
                curwin.w_cursor.col = 0;
                oap.op_end.col = strlen(ml_get(oap.op_end.lnum));
                if (oap.op_end.col != 0)
                    --oap.op_end.col;
            }
            else if (!oap.inclusive)
                dec(oap.op_end);

            while (ltoreq(curwin.w_cursor, oap.op_end))
            {
                int n = gchar_cursor();
                if (n != NUL)
                {
                    if (1 < utf_char2len(c) || 1 < utf_char2len(n))
                    {
                        /* This is slow, but it handles replacing a single-byte
                         * with a multi-byte and the other way around. */
                        if (curwin.w_cursor.lnum == oap.op_end.lnum)
                            oap.op_end.col += utf_char2len(c) - utf_char2len(n);
                        n = State;
                        State = REPLACE;
                        ins_char(c);
                        State = n;
                        /* Backup to the replaced character. */
                        dec_cursor();
                    }
                    else
                    {
                        if (n == TAB)
                        {
                            int end_vcol = 0;

                            if (curwin.w_cursor.lnum == oap.op_end.lnum)
                            {
                                /* oap.op_end has to be recalculated when the tab breaks */
                                end_vcol = getviscol2(oap.op_end.col, oap.op_end.coladd);
                            }
                            coladvance_force(getviscol());
                            if (curwin.w_cursor.lnum == oap.op_end.lnum)
                                getvpos(oap.op_end, end_vcol);
                        }
                        ml_get_buf(curbuf, curwin.w_cursor.lnum, true).be(curwin.w_cursor.col, c);
                    }
                }
                else if (virtual_op != FALSE && curwin.w_cursor.lnum == oap.op_end.lnum)
                {
                    int virtcols = oap.op_end.coladd;

                    if (curwin.w_cursor.lnum == oap.op_start.lnum
                            && oap.op_start.col == oap.op_end.col && oap.op_start.coladd != 0)
                        virtcols -= oap.op_start.coladd;

                    /* 'oap.op_end' has been trimmed, so it's effectively inclusive;
                     * as a result, an extra +1 must be counted, so we don't trample the NUL byte. */
                    coladvance_force(getviscol2(oap.op_end.col, oap.op_end.coladd) + 1);
                    curwin.w_cursor.col -= (virtcols + 1);
                    for ( ; 0 <= virtcols; virtcols--)
                    {
                        ml_get_buf(curbuf, curwin.w_cursor.lnum, true).be(curwin.w_cursor.col, c);
                        if (inc(curwin.w_cursor) == -1)
                            break;
                    }
                }

                /* Advance to next character, stop at the end of the file. */
                if (inc_cursor() == -1)
                    break;
            }
        }

        COPY_pos(curwin.w_cursor, oap.op_start);
        check_cursor();
        changed_lines(oap.op_start.lnum, oap.op_start.col, oap.op_end.lnum + 1, 0L);

        /* Set "'[" and "']" marks. */
        COPY_pos(curbuf.b_op_start, oap.op_start);
        COPY_pos(curbuf.b_op_end, oap.op_end);

        return true;
    }

    /*
     * Handle the (non-standard vi) tilde operator.  Also for "gu", "gU" and "g?".
     */
    /*private*/ static void op_tilde(oparg_C oap)
    {
        boolean did_change = false;

        if (!u_save(oap.op_start.lnum - 1, oap.op_end.lnum + 1))
            return;

        pos_C pos = new pos_C();
        COPY_pos(pos, oap.op_start);

        if (oap.block_mode)                     /* Visual block mode */
        {
            for ( ; pos.lnum <= oap.op_end.lnum; pos.lnum++)
            {
                block_def_C bd = new block_def_C();
                block_prep(oap, bd, pos.lnum, false);
                pos.col = bd.textcol;
                boolean one_change = swapchars(oap.op_type, pos, bd.textlen);
                did_change |= one_change;
            }
            if (did_change)
                changed_lines(oap.op_start.lnum, 0, oap.op_end.lnum + 1, 0L);
        }
        else                                    /* not block mode */
        {
            if (oap.motion_type == MLINE)
            {
                oap.op_start.col = 0;
                pos.col = 0;
                oap.op_end.col = strlen(ml_get(oap.op_end.lnum));
                if (oap.op_end.col != 0)
                    --oap.op_end.col;
            }
            else if (!oap.inclusive)
                dec(oap.op_end);

            if (pos.lnum == oap.op_end.lnum)
                did_change = swapchars(oap.op_type, pos, oap.op_end.col - pos.col + 1);
            else
                for ( ; ; )
                {
                    did_change |= swapchars(oap.op_type, pos,
                                    (pos.lnum == oap.op_end.lnum) ? oap.op_end.col + 1 : strlen(ml_get_pos(pos)));
                    if (ltoreq(oap.op_end, pos) || inc(pos) == -1)
                        break;
                }

            if (did_change)
            {
                changed_lines(oap.op_start.lnum, oap.op_start.col, oap.op_end.lnum + 1, 0L);
            }
        }

        if (!did_change && oap.is_VIsual)
            /* No change: need to remove the Visual selection. */
            redraw_curbuf_later(INVERTED);

        /*
         * Set '[ and '] marks.
         */
        COPY_pos(curbuf.b_op_start, oap.op_start);
        COPY_pos(curbuf.b_op_end, oap.op_end);

        if (p_report[0] < oap.line_count)
        {
            if (oap.line_count == 1)
                msg(u8("1 line changed"));
            else
                smsg(u8("%ld lines changed"), oap.line_count);
        }
    }

    /*
     * Invoke swapchar() on "length" bytes at position "pos".
     * "pos" is advanced to just after the changed characters.
     * "length" is rounded up to include the whole last multi-byte character.
     * Also works correctly when the number of bytes changes.
     * Returns true if some character was changed.
     */
    /*private*/ static boolean swapchars(int op_type, pos_C pos, int length)
    {
        boolean did_change = false;

        for (int todo = length; 0 < todo; --todo)
        {
            int len = us_ptr2len_cc(ml_get_pos(pos));

            /* we're counting bytes, not characters */
            if (0 < len)
                todo -= len - 1;

            did_change |= swapchar(op_type, pos);
            if (inc(pos) == -1)     /* at end of file */
                break;
        }

        return did_change;
    }

    /*
     * If op_type == OP_UPPER: make uppercase,
     * if op_type == OP_LOWER: make lowercase,
     * if op_type == OP_ROT13: do rot13 encoding,
     * else swap case of character at 'pos'
     * returns true when something actually changed.
     */
    /*private*/ static boolean swapchar(int op_type, pos_C pos)
    {
        int c = gchar_pos(pos);

        /* Only do rot13 encoding for ASCII characters. */
        if (0x80 <= c && op_type == OP_ROT13)
            return false;

        if (op_type == OP_UPPER && c == 0xdf)
        {
            pos_C sp = new pos_C();
            COPY_pos(sp, curwin.w_cursor);

            /* Special handling of German sharp s: change to "SS". */
            COPY_pos(curwin.w_cursor, pos);
            del_char(false);
            ins_char('S');
            ins_char('S');
            COPY_pos(curwin.w_cursor, sp);
            inc(pos);
        }

        int nc = c;
        if (utf_islower(c))
        {
            if (op_type == OP_ROT13)
                nc = rot13(c, 'a');
            else if (op_type != OP_LOWER)
                nc = utf_toupper(c);
        }
        else if (utf_isupper(c))
        {
            if (op_type == OP_ROT13)
                nc = rot13(c, 'A');
            else if (op_type != OP_UPPER)
                nc = utf_tolower(c);
        }
        if (nc != c)
        {
            if (0x80 <= c || 0x80 <= nc)
            {
                pos_C sp = new pos_C();
                COPY_pos(sp, curwin.w_cursor);

                COPY_pos(curwin.w_cursor, pos);
                /* don't use del_char(), it also removes composing chars */
                del_bytes(us_ptr2len(ml_get_cursor()), false, false);
                ins_char(nc);
                COPY_pos(curwin.w_cursor, sp);
            }
            else
                ml_get_buf(curbuf, pos.lnum, true).be(pos.col, nc);
            return true;
        }
        return false;
    }

    /*
     * op_insert - Insert and append operators for Visual mode.
     */
    /*private*/ static void op_insert(oparg_C oap, long count1)
    {
        int pre_textlen = 0;

        /* edit() changes this - record it for OP_APPEND */
        block_def_C bd = new block_def_C();
        bd.is_MAX = (curwin.w_curswant == MAXCOL);

        /* vis block is still marked.  Get rid of it now. */
        curwin.w_cursor.lnum = oap.op_start.lnum;
        update_screen(INVERTED);

        if (oap.block_mode)
        {
            /* When 'virtualedit' is used, need to insert the extra spaces before doing block_prep().
             * When only "block" is used, virtual edit is already disabled,
             * but still need it when calling coladvance_force(). */
            if (0 < curwin.w_cursor.coladd)
            {
                int old_ve_flags = ve_flags[0];

                ve_flags[0] = VE_ALL;
                if (!u_save_cursor())
                    return;

                coladvance_force(oap.op_type == OP_APPEND ? oap.end_vcol + 1 : getviscol());
                if (oap.op_type == OP_APPEND)
                    --curwin.w_cursor.col;
                ve_flags[0] = old_ve_flags;
            }
            /* Get the info about the block before entering the text. */
            block_prep(oap, bd, oap.op_start.lnum, true);
            Bytes firstline = ml_get(oap.op_start.lnum).plus(bd.textcol);
            if (oap.op_type == OP_APPEND)
                firstline = firstline.plus(bd.textlen);
            pre_textlen = strlen(firstline);
        }

        if (oap.op_type == OP_APPEND)
        {
            if (oap.block_mode && curwin.w_cursor.coladd == 0)
            {
                /* Move the cursor to the character right of the block. */
                curwin.w_set_curswant = true;
                while (ml_get_cursor().at(0) != NUL && curwin.w_cursor.col < bd.textcol + bd.textlen)
                    curwin.w_cursor.col++;
                if (bd.is_short && !bd.is_MAX)
                {
                    /* First line was too short, make it longer and adjust the values in "bd". */
                    if (!u_save_cursor())
                        return;

                    for (int i = 0; i < bd.endspaces; i++)
                        ins_char(' ');
                    bd.textlen += bd.endspaces;
                }
            }
            else
            {
                COPY_pos(curwin.w_cursor, oap.op_end);
                check_cursor_col();

                /* Works just like an 'i'nsert on the next character. */
                if (!lineempty(curwin.w_cursor.lnum) && oap.start_vcol != oap.end_vcol)
                    inc_cursor();
            }
        }

        pos_C t1 = new pos_C();
        COPY_pos(t1, oap.op_start);

        edit(NUL, false, count1);

        /* When a tab was inserted, and the characters in front of the tab
         * have been converted to a tab as well, the column of the cursor
         * might have actually been reduced, so need to adjust here. */
        if (t1.lnum == curbuf.b_op_start_orig.lnum && ltpos(curbuf.b_op_start_orig, t1))
            COPY_pos(oap.op_start, curbuf.b_op_start_orig);

        /* If user has moved off this line, we don't know what to do, so do nothing.
         * Also don't repeat the insert when Insert mode ended with CTRL-C. */
        if (curwin.w_cursor.lnum != oap.op_start.lnum || got_int)
            return;

        if (oap.block_mode)
        {
            block_def_C bd2 = new block_def_C();

            /* The user may have moved the cursor before inserting something,
             * try to adjust the block for that. */
            if (oap.op_start.lnum == curbuf.b_op_start_orig.lnum && !bd.is_MAX)
            {
                if (oap.op_type == OP_INSERT && oap.op_start.col + oap.op_start.coladd != curbuf.b_op_start_orig.col + curbuf.b_op_start_orig.coladd)
                {
                    int t = getviscol2(curbuf.b_op_start_orig.col, curbuf.b_op_start_orig.coladd);
                    oap.op_start.col = curbuf.b_op_start_orig.col;
                    pre_textlen -= t - oap.start_vcol;
                    oap.start_vcol = t;
                }
                else if (oap.op_type == OP_APPEND && curbuf.b_op_start_orig.col + curbuf.b_op_start_orig.coladd <= oap.op_end.col + oap.op_end.coladd)
                {
                    int t = getviscol2(curbuf.b_op_start_orig.col, curbuf.b_op_start_orig.coladd);
                    oap.op_start.col = curbuf.b_op_start_orig.col;
                    /* reset pre_textlen to the value of OP_INSERT */
                    pre_textlen += bd.textlen;
                    pre_textlen -= t - oap.start_vcol;
                    oap.start_vcol = t;
                    oap.op_type = OP_INSERT;
                }
            }

            /*
             * Spaces and tabs in the indent may have changed to other spaces and tabs.
             * Get the starting column again and correct the length.
             * Don't do this when "$" used, end-of-line will have changed.
             */
            block_prep(oap, bd2, oap.op_start.lnum, true);
            if (!bd.is_MAX || bd2.textlen < bd.textlen)
            {
                if (oap.op_type == OP_APPEND)
                {
                    pre_textlen += bd2.textlen - bd.textlen;
                    if (bd2.endspaces != 0)
                        --bd2.textlen;
                }
                bd.textcol = bd2.textcol;
                bd.textlen = bd2.textlen;
            }

            /*
             * Subsequent calls to ml_get() flush the firstline data
             * - take a copy of the required string.
             */
            Bytes firstline = ml_get(oap.op_start.lnum).plus(bd.textcol);
            if (oap.op_type == OP_APPEND)
                firstline = firstline.plus(bd.textlen);

            int ins_len;
            if (0 <= pre_textlen && 0 < (ins_len = strlen(firstline) - pre_textlen))
            {
                Bytes ins_text = STRNDUP(firstline, ins_len);

                /* block handled here */
                if (u_save(oap.op_start.lnum, oap.op_end.lnum + 1))
                    block_insert(oap, ins_text, (oap.op_type == OP_INSERT), bd);

                curwin.w_cursor.col = oap.op_start.col;
                check_cursor();
            }
        }
    }

    /*
     * op_change - handle a change operation
     *
     * return true if edit() returns because of a CTRL-O command
     */
    /*private*/ static boolean op_change(oparg_C oap)
    {
        int pre_textlen = 0;
        int pre_indent = 0;

        int l = oap.op_start.col;
        if (oap.motion_type == MLINE)
        {
            l = 0;
            if (!p_paste[0] && curbuf.b_p_si[0] && !curbuf.b_p_cin[0])
                can_si = true;      /* It's like opening a new line, do si */
        }

        /* First delete the text in the region.  In an empty buffer only need to save for undo. */
        if ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0)
        {
            if (!u_save_cursor())
                return false;
        }
        else if (op_delete(oap) == false)
            return false;

        if ((curwin.w_cursor.col < l) && !lineempty(curwin.w_cursor.lnum) && virtual_op == FALSE)
            inc_cursor();

        block_def_C bd = new block_def_C();
        /* check for still on same line (<CR> in inserted text meaningless); skip blank lines too */
        if (oap.block_mode)
        {
            /* Add spaces before getting the current line length. */
            if (virtual_op != FALSE && (0 < curwin.w_cursor.coladd || gchar_cursor() == NUL))
                coladvance_force(getviscol());
            Bytes firstline = ml_get(oap.op_start.lnum);
            pre_textlen = strlen(firstline);
            pre_indent = BDIFF(skipwhite(firstline), firstline);
            bd.textcol = curwin.w_cursor.col;
        }

        if (oap.motion_type == MLINE)
            fix_indent();

        boolean retval = edit(NUL, false, 1);

        /*
         * In Visual block mode, handle copying the new text to all lines of the block.
         * Don't repeat the insert when Insert mode ended with CTRL-C.
         */
        if (oap.block_mode && oap.op_start.lnum != oap.op_end.lnum && !got_int)
        {
            /* Auto-indenting may have changed the indent.  If the cursor was past
             * the indent, exclude that indent change from the inserted text. */
            Bytes firstline = ml_get(oap.op_start.lnum);
            if (pre_indent < bd.textcol)
            {
                int new_indent = BDIFF(skipwhite(firstline), firstline);

                pre_textlen += new_indent - pre_indent;
                bd.textcol += new_indent - pre_indent;
            }

            int ins_len = strlen(firstline) - pre_textlen;
            if (0 < ins_len)
            {
                /* Subsequent calls to ml_get() flush the "firstline" data
                 * -- take a copy of the inserted text. */
                Bytes ins_text = STRNDUP(firstline.plus(bd.textcol), ins_len);

                for (long linenr = oap.op_start.lnum + 1; linenr <= oap.op_end.lnum; linenr++)
                {
                    block_prep(oap, bd, linenr, true);
                    if (!bd.is_short || virtual_op != FALSE)
                    {
                        pos_C vpos = new pos_C();

                        /* If the block starts in virtual space, count the
                         * initial coladd offset as part of "startspaces". */
                        if (bd.is_short)
                        {
                            vpos.lnum = linenr;
                            getvpos(vpos, oap.start_vcol);
                        }
                        else
                            vpos.coladd = 0;
                        Bytes oldp = ml_get(linenr);
                        Bytes newp = new Bytes(strlen(oldp) + vpos.coladd + ins_len + 1);

                        /* copy up to block start */
                        BCOPY(newp, oldp, bd.textcol);
                        int offset = bd.textcol;
                        copy_spaces(newp.plus(offset), vpos.coladd);
                        offset += vpos.coladd;
                        BCOPY(newp, offset, ins_text, 0, ins_len);
                        offset += ins_len;
                        oldp = oldp.plus(bd.textcol);
                        BCOPY(newp, offset, oldp, 0, strlen(oldp) + 1);
                        ml_replace(linenr, newp, false);
                    }
                }
                check_cursor();

                changed_lines(oap.op_start.lnum + 1, 0, oap.op_end.lnum + 1, 0L);
            }
        }

        return retval;
    }

    /*
     * set all the yank registers to empty (called from main())
     */
    /*private*/ static void init_yank()
    {
        for (int i = 0; i < NUM_REGISTERS; i++)
            y_regs[i].y_array = null;
    }

    /*
     * Yank the text between "oap.op_start" and "oap.op_end" into a yank register.
     * If we are to append (uppercase register), we first yank into a new yank
     * register and then concatenate the old and the new one (so we keep the old
     * one in case of out-of-memory).
     *
     * Return false for failure, true otherwise.
     */
    /*private*/ static boolean op_yank(oparg_C oap, boolean deleting, boolean mess)
    {
        byte yanktype = oap.motion_type;
        int yanklines = (int)oap.line_count;
        long yankendlnum = oap.op_end.lnum;

                                                        /* check for read-only register */
        if (oap.regname != 0 && !valid_yank_reg(oap.regname, true))
        {
            beep_flush();
            return false;
        }
        if (oap.regname == '_')                         /* black hole: nothing to do */
            return true;

        if (!clip_star.available && oap.regname == '*')
            oap.regname = 0;
        else if (!clip_plus.available && oap.regname == '+')
            oap.regname = 0;

        if (!deleting)                                  /* op_delete() already set y_current */
            get_yank_register(oap.regname, true);

        yankreg_C curr = y_current;                     /* copy of y_current */
        yankreg_C newreg = new yankreg_C();             /* new yank register when appending */
        if (y_append && y_current.y_array != null)      /* append to existing contents */
            y_current = newreg;
        else
            y_current.y_array = null;                   /* free previously yanked lines */

        /*
         * If the cursor was in column 1 before and after the movement,
         * and the operator is not inclusive, the yank is always linewise.
         */
        if (       oap.motion_type == MCHAR
                && oap.op_start.col == 0
                && !oap.inclusive
                && (!oap.is_VIsual || p_sel[0].at(0) == (byte)'o')
                && !oap.block_mode
                && oap.op_end.col == 0
                && 1 < yanklines)
        {
            yanktype = MLINE;
            --yankendlnum;
            --yanklines;
        }

        y_current.y_size = yanklines;
        y_current.y_type = yanktype;                    /* set the yank register type */
        y_current.y_width = 0;
        y_current.y_array = new Bytes[yanklines];

        int y_idx = 0;                                 /* index in y_array[] */
        long lnum = oap.op_start.lnum;                  /* current line number */

        if (oap.block_mode)
        {
            /* Visual block mode. */
            y_current.y_type = MBLOCK;                  /* set the yank register type */
            y_current.y_width = oap.end_vcol - oap.start_vcol;

            if (curwin.w_curswant == MAXCOL && 0 < y_current.y_width)
                y_current.y_width--;
        }

        fail:
        {
            block_def_C bd = new block_def_C();

            for ( ; lnum <= yankendlnum; lnum++, y_idx++)
            {
                switch (y_current.y_type)
                {
                    case MBLOCK:
                        block_prep(oap, bd, lnum, false);
                        if (yank_copy_line(bd, y_idx) == false)
                            break fail;
                        break;

                    case MLINE:
                        if ((y_current.y_array[y_idx] = STRDUP(ml_get(lnum))) == null)
                            break fail;
                        break;

                    case MCHAR:
                    {
                        int startcol = 0, endcol = MAXCOL;
                        boolean is_oneChar = false;
                        Bytes p = ml_get(lnum);
                        bd.startspaces = 0;
                        bd.endspaces = 0;

                        if (lnum == oap.op_start.lnum)
                        {
                            startcol = oap.op_start.col;
                            if (virtual_op != FALSE)
                            {
                                int[] cs = new int[1];
                                int[] ce = new int[1];
                                getvcol(curwin, oap.op_start, cs, null, ce);
                                if (ce[0] != cs[0] && 0 < oap.op_start.coladd)
                                {
                                    /* Part of a tab selected -- but don't double-count it. */
                                    bd.startspaces = (ce[0] - cs[0] + 1) - oap.op_start.coladd;
                                    startcol++;
                                }
                            }
                        }

                        if (lnum == oap.op_end.lnum)
                        {
                            endcol = oap.op_end.col;
                            if (virtual_op != FALSE)
                            {
                                int[] cs = new int[1];
                                int[] ce = new int[1];
                                getvcol(curwin, oap.op_end, cs, null, ce);
                                if (p.at(endcol) == NUL || (cs[0] + oap.op_end.coladd < ce[0]
                                            /* Don't add space for double-wide char;
                                             * endcol will be on last byte of multi-byte char. */
                                            && us_head_off(p, p.plus(endcol)) == 0))
                                {
                                    if (oap.op_start.lnum == oap.op_end.lnum && oap.op_start.col == oap.op_end.col)
                                    {
                                        /* Special case: inside a single char. */
                                        is_oneChar = true;
                                        bd.startspaces = oap.op_end.coladd - oap.op_start.coladd + (oap.inclusive ? 1 : 0);
                                        endcol = startcol;
                                    }
                                    else
                                    {
                                        bd.endspaces = oap.op_end.coladd + (oap.inclusive ? 1 : 0);
                                        endcol -= (oap.inclusive ? 1 : 0);
                                    }
                                }
                            }
                        }
                        if (endcol == MAXCOL)
                            endcol = strlen(p);
                        if (endcol < startcol || is_oneChar)
                            bd.textlen = 0;
                        else
                            bd.textlen = endcol - startcol + (oap.inclusive ? 1 : 0);
                        bd.textstart = p.plus(startcol);
                        if (yank_copy_line(bd, y_idx) == false)
                            break fail;
                        break;
                    }
                }
            }

            if (curr != y_current)      /* append the new block to the old block */
            {
                Bytes[] new_ptr = new Bytes[curr.y_size + y_current.y_size];

                int j;
                for (j = 0; j < curr.y_size; j++)
                    new_ptr[j] = curr.y_array[j];
                curr.y_array = new_ptr;

                if (yanktype == MLINE)  /* MLINE overrides MCHAR and MBLOCK */
                    curr.y_type = MLINE;

                /* Concatenate the last line of the old block with the first line of the new block,
                 * unless being Vi compatible. */
                if (curr.y_type == MCHAR && vim_strbyte(p_cpo[0], CPO_REGAPPEND) == null)
                {
                    Bytes p = new Bytes(strlen(curr.y_array[curr.y_size - 1]) + strlen(y_current.y_array[0]) + 1);

                    STRCPY(p, curr.y_array[--j]);
                    STRCAT(p, y_current.y_array[0]);
                    y_current.y_array[0] = null;
                    curr.y_array[j++] = p;
                    y_idx = 1;
                }
                else
                    y_idx = 0;
                while (y_idx < y_current.y_size)
                    curr.y_array[j++] = y_current.y_array[y_idx++];
                curr.y_size = j;
                y_current.y_array = null;
                y_current = curr;
            }

            if (curwin.w_onebuf_opt.wo_rnu[0])
                redraw_later(SOME_VALID);       /* cursor moved to start */

            if (mess)                   /* Display message about yank? */
            {
                if (yanktype == MCHAR && !oap.block_mode && yanklines == 1)
                    yanklines = 0;
                /* Some versions of Vi use ">=" here, some don't... */
                if (p_report[0] < yanklines)
                {
                    /* redisplay now, so message is not deleted */
                    update_topline_redraw();
                    if (yanklines == 1)
                    {
                        if (oap.block_mode)
                            msg(u8("block of 1 line yanked"));
                        else
                            msg(u8("1 line yanked"));
                    }
                    else if (oap.block_mode)
                        smsg(u8("block of %ld lines yanked"), yanklines);
                    else
                        smsg(u8("%ld lines yanked"), yanklines);
                }
            }

            /*
             * Set "'[" and "']" marks.
             */
            COPY_pos(curbuf.b_op_start, oap.op_start);
            COPY_pos(curbuf.b_op_end, oap.op_end);
            if (yanktype == MLINE && !oap.block_mode)
            {
                curbuf.b_op_start.col = 0;
                curbuf.b_op_end.col = MAXCOL;
            }

            /*
             * If we were yanking to the '*' register, send result to clipboard.
             * If no register was specified, and "unnamed" in 'clipboard',
             * make a copy to the '*' register.
             */
            if (clip_star.available
                    && (curr == y_regs[STAR_REGISTER]
                        || (!deleting && oap.regname == 0
                        && ((clip_unnamed | clip_unnamed_saved) & CLIP_UNNAMED) != 0)))
            {
                if (curr != y_regs[STAR_REGISTER])
                    /* Copy the text from register 0 to the clipboard register. */
                    copy_yank_reg(y_regs[STAR_REGISTER]);

                clip_own_selection(clip_star);
                clip_gen_set_selection(clip_star);
            }

            return true;
        }

        y_current.y_array = null;
        y_current = curr;

        return false;
    }

    /*private*/ static boolean yank_copy_line(block_def_C bd, int y_idx)
    {
        Bytes pnew = new Bytes(bd.startspaces + bd.endspaces + bd.textlen + 1);

        y_current.y_array[y_idx] = pnew;
        copy_spaces(pnew, bd.startspaces);
        pnew = pnew.plus(bd.startspaces);
        BCOPY(pnew, bd.textstart, bd.textlen);
        pnew = pnew.plus(bd.textlen);
        copy_spaces(pnew, bd.endspaces);
        pnew = pnew.plus(bd.endspaces);
        pnew.be(0, NUL);

        return true;
    }

    /*
     * Make a copy of the y_current register to register "reg".
     */
    /*private*/ static void copy_yank_reg(yankreg_C reg)
    {
        yankreg_C curr = y_current;

        y_current = reg;
        COPY_yankreg(y_current, curr);
        y_current.y_array = new Bytes[y_current.y_size];

        for (int i = 0; i < y_current.y_size; i++)
        {
            Bytes s = STRDUP(curr.y_array[i]);
            if (s == null)
            {
                y_current.y_array = null;
                y_current.y_size = 0;
                break;
            }
            y_current.y_array[i] = s;
        }

        y_current = curr;
    }

    /*
     * Put contents of register "regname" into the text.
     * Caller must check "regname" to be valid!
     * "flags": PUT_FIXINDENT       make indent look nice
     *          PUT_CURSEND         leave cursor after end of new text
     *          PUT_LINE            force linewise put (":put")
     */
    /*private*/ static void do_put(int regname, int dir, int count, int flags)
        /* dir: BACKWARD for 'P', FORWARD for 'p' */
    {
        int totlen = 0;
        int y_width = 0;
        int incr = 0;
        long nr_lines = 0;
        int orig_indent = 0;
        int indent_diff = 0;
        boolean first_indent = true;
        int lendiff = 0;

        /* Adjust register name for "unnamed" in 'clipboard'. */
        regname = adjust_clip_reg(regname);
        may_get_selection(regname);

        if ((flags & PUT_FIXINDENT) != 0)
            orig_indent = get_indent();

        COPY_pos(curbuf.b_op_start, curwin.w_cursor);   /* default for '[ mark */
        COPY_pos(curbuf.b_op_end, curwin.w_cursor);     /* default for '] mark */

        /*
         * Using inserted text works differently, because the register includes
         * special characters (newlines, etc.).
         */
        if (regname == '.')
        {
            stuff_inserted((dir == FORWARD) ? (count == -1 ? 'o' : 'a') : (count == -1 ? 'O' : 'i'), count, false);
            /* Putting the text is done later, so can't really move the cursor
             * to the next character.  Use "l" to simulate it. */
            if ((flags & PUT_CURSEND) != 0 && gchar_cursor() != NUL)
                stuffcharReadbuff('l');
            return;
        }

        /*
         * For special registers '%' (file name), '#' (alternate file name) and
         * ':' (last command line), etc. we have to create a fake yank register.
         */
        Bytes[] insert_string = { null };
        boolean[] allocated = { false };
        if (get_spec_reg(regname, insert_string, allocated, true))
        {
            if (insert_string[0] == null)
                return;
        }

        /* Autocommands may be executed when saving lines for undo,
         * which may make "y_array" invalid.  Start undo now to avoid that. */
        u_save(curwin.w_cursor.lnum, curwin.w_cursor.lnum + 1);

        int y_type;
        int y_size;
        Bytes[] y_array = null;

        if (insert_string[0] != null)
        {
            y_type = MCHAR;
            if (regname == '=')
            {
                /* For the = register we need to split the string at NL characters.
                 * Loop twice: count the number of lines and save them. */
                for ( ; ; )
                {
                    y_size = 0;
                    for (Bytes p = insert_string[0]; p != null; )
                    {
                        if (y_array != null)
                            y_array[y_size] = p;
                        y_size++;
                        p = vim_strchr(p, '\n');
                        if (p != null)
                        {
                            if (y_array != null)
                                p.be(0, NUL);
                            p = p.plus(1);
                            /* A trailing '\n' makes the register linewise. */
                            if (p.at(0) == NUL)
                            {
                                y_type = MLINE;
                                break;
                            }
                        }
                    }
                    if (y_array != null)
                        break;
                    y_array = new Bytes[y_size];
                }
            }
            else
            {
                y_size = 1;         /* use fake one-line yank register */
                y_array = new Bytes[] { insert_string[0] };
            }
        }
        else
        {
            get_yank_register(regname, false);

            y_type = y_current.y_type;
            y_width = y_current.y_width;
            y_size = y_current.y_size;
            y_array = y_current.y_array;
        }

        theend:
        {
            if (y_type == MLINE)
            {
                if ((flags & PUT_LINE_SPLIT) != 0)
                {
                    /* "p" or "P" in Visual mode: split the lines to put the text in between. */
                    if (!u_save_cursor())
                        break theend;

                    Bytes p = STRDUP(ml_get_cursor());
                    ml_append(curwin.w_cursor.lnum, p, 0, false);
                    p = STRNDUP(ml_get_curline(), curwin.w_cursor.col);
                    ml_replace(curwin.w_cursor.lnum, p, false);
                    nr_lines++;
                    dir = FORWARD;
                }
                if ((flags & PUT_LINE_FORWARD) != 0)
                {
                    /* Must be "p" for a Visual block, put lines below the block. */
                    COPY_pos(curwin.w_cursor, curbuf.b_visual.vi_end);
                    dir = FORWARD;
                }
                COPY_pos(curbuf.b_op_start, curwin.w_cursor);   /* default for '[ mark */
                COPY_pos(curbuf.b_op_end, curwin.w_cursor);     /* default for '] mark */
            }

            if ((flags & PUT_LINE) != 0)        /* :put command or "p" in Visual line mode. */
                y_type = MLINE;

            if (y_size == 0 || y_array == null)
            {
                emsg2(u8("E353: Nothing in register %s"), (regname == 0) ? u8("\"") : transchar(regname));
                break theend;
            }

            long lnum;
            if (y_type == MBLOCK)
            {
                lnum = curwin.w_cursor.lnum + y_size + 1;
                if (lnum > curbuf.b_ml.ml_line_count)
                    lnum = curbuf.b_ml.ml_line_count + 1;
                if (!u_save(curwin.w_cursor.lnum - 1, lnum))
                    break theend;
            }
            else if (y_type == MLINE)
            {
                lnum = curwin.w_cursor.lnum;
                if (dir == FORWARD)
                    lnum++;
                /* In an empty buffer the empty line is going to be replaced,
                 * include it in the saved lines. */
                if (bufempty() ? !u_save(0, 2) : !u_save(lnum - 1, lnum))
                    break theend;
            }
            else if (!u_save_cursor())
                break theend;

            int yanklen = strlen(y_array[0]);

            if (ve_flags[0] == VE_ALL && y_type == MCHAR)
            {
                if (gchar_cursor() == TAB)
                {
                    /* Don't need to insert spaces when "p" on the last position
                     * of a tab or "P" on the first position. */
                    if (dir == FORWARD
                            ? curwin.w_cursor.coladd < curbuf.b_p_ts[0] - 1 : 0 < curwin.w_cursor.coladd)
                        coladvance_force(getviscol());
                    else
                        curwin.w_cursor.coladd = 0;
                }
                else if (0 < curwin.w_cursor.coladd || gchar_cursor() == NUL)
                    coladvance_force(getviscol() + (dir == FORWARD ? 1 : 0));
            }

            lnum = curwin.w_cursor.lnum;
            int[] col = { curwin.w_cursor.col };

            /*
             * Block mode
             */
            if (y_type == MBLOCK)
            {
                block_def_C bd = new block_def_C();
                int c = gchar_cursor();
                int[] endcol2 = { 0 };

                if (dir == FORWARD && c != NUL)
                {
                    if (ve_flags[0] == VE_ALL)
                        getvcol(curwin, curwin.w_cursor, col, null, endcol2);
                    else
                        getvcol(curwin, curwin.w_cursor, null, null, col);

                    /* move to start of next multi-byte character */
                    curwin.w_cursor.col += us_ptr2len_cc(ml_get_cursor());

                    col[0]++;
                }
                else
                    getvcol(curwin, curwin.w_cursor, col, null, endcol2);

                col[0] += curwin.w_cursor.coladd;
                if (ve_flags[0] == VE_ALL && (0 < curwin.w_cursor.coladd || endcol2[0] == curwin.w_cursor.col))
                {
                    if (dir == FORWARD && c == NUL)
                        col[0]++;
                    if (dir != FORWARD && c != NUL)
                        curwin.w_cursor.col++;
                    if (c == TAB)
                    {
                        if (dir == BACKWARD && 0 < curwin.w_cursor.col)
                            curwin.w_cursor.col--;
                        if (dir == FORWARD && col[0] - 1 == endcol2[0])
                            curwin.w_cursor.col++;
                    }
                }
                curwin.w_cursor.coladd = 0;
                bd.textcol = 0;
                for (int i = 0; i < y_size; i++)
                {
                    bd.startspaces = 0;
                    bd.endspaces = 0;
                    int vcol = 0;
                    int delcount = 0;

                    /* add a new line */
                    if (curbuf.b_ml.ml_line_count < curwin.w_cursor.lnum)
                    {
                        if (!ml_append(curbuf.b_ml.ml_line_count, u8(""), 1, false))
                            break;
                        nr_lines++;
                    }
                    /* get the old line and advance to the position to insert at */
                    Bytes oldp = ml_get_curline();
                    int oldlen = strlen(oldp);
                    Bytes[] pp = new Bytes[1];
                    for (pp[0] = oldp; vcol < col[0] && pp[0].at(0) != NUL; )
                    {
                        /* Count a tab for what it's worth (if list mode not on). */
                        incr = lbr_chartabsize_adv(oldp, pp, vcol);
                        vcol += incr;
                    }
                    bd.textcol = BDIFF(pp[0], oldp);

                    boolean shortline = (vcol < col[0]) || (vcol == col[0] && pp[0].at(0) == NUL);

                    if (vcol < col[0]) /* line too short, padd with spaces */
                        bd.startspaces = col[0] - vcol;
                    else if (col[0] < vcol)
                    {
                        bd.endspaces = vcol - col[0];
                        bd.startspaces = incr - bd.endspaces;
                        --bd.textcol;
                        delcount = 1;
                        bd.textcol -= us_head_off(oldp, oldp.plus(bd.textcol));
                        if (oldp.at(bd.textcol) != TAB)
                        {
                            /* Only a Tab can be split into spaces.
                             * Other characters will have to be moved to after the block,
                             * causing misalignment. */
                            delcount = 0;
                            bd.endspaces = 0;
                        }
                    }

                    yanklen = strlen(y_array[i]);

                    /* calculate number of spaces required to fill right side of block */
                    int spaces = y_width + 1;
                    for (int j = 0; j < yanklen; j++)
                        spaces -= lbr_chartabsize(null, y_array[i].plus(j), 0);
                    if (spaces < 0)
                        spaces = 0;

                    /* insert the new text */
                    totlen = count * (yanklen + spaces) + bd.startspaces + bd.endspaces;
                    Bytes newp = new Bytes(totlen + oldlen + 1);

                    /* copy part up to cursor to new line */
                    Bytes p = newp;
                    BCOPY(p, oldp, bd.textcol);
                    p = p.plus(bd.textcol);
                    /* may insert some spaces before the new text */
                    copy_spaces(p, bd.startspaces);
                    p = p.plus(bd.startspaces);
                    /* insert the new text */
                    for (int j = 0; j < count; j++)
                    {
                        BCOPY(p, y_array[i], yanklen);
                        p = p.plus(yanklen);

                        /* insert block's trailing spaces only if there's text behind */
                        if ((j < count - 1 || !shortline) && spaces != 0)
                        {
                            copy_spaces(p, spaces);
                            p = p.plus(spaces);
                        }
                    }
                    /* may insert some spaces after the new text */
                    copy_spaces(p, bd.endspaces);
                    p = p.plus(bd.endspaces);
                    /* move the text after the cursor to the end of the line. */
                    BCOPY(p, 0, oldp, bd.textcol + delcount, oldlen - bd.textcol - delcount + 1);
                    ml_replace(curwin.w_cursor.lnum, newp, false);

                    curwin.w_cursor.lnum++;
                    if (i == 0)
                        curwin.w_cursor.col += bd.startspaces;
                }

                changed_lines(lnum, 0, curwin.w_cursor.lnum, nr_lines);

                /* Set '[ mark. */
                COPY_pos(curbuf.b_op_start, curwin.w_cursor);
                curbuf.b_op_start.lnum = lnum;

                /* Adjust '] mark. */
                curbuf.b_op_end.lnum = curwin.w_cursor.lnum - 1;
                curbuf.b_op_end.col = bd.textcol + totlen - 1;
                curbuf.b_op_end.coladd = 0;
                if ((flags & PUT_CURSEND) != 0)
                {
                    COPY_pos(curwin.w_cursor, curbuf.b_op_end);
                    curwin.w_cursor.col++;

                    /* in Insert mode we might be after the NUL, correct for that */
                    int len = strlen(ml_get_curline());
                    if (curwin.w_cursor.col > len)
                        curwin.w_cursor.col = len;
                }
                else
                    curwin.w_cursor.lnum = lnum;
            }
            else
            {
                /*
                 * Character or Line mode
                 */
                if (y_type == MCHAR)
                {
                    /* if type is MCHAR, FORWARD is the same as BACKWARD on the next char */
                    if (dir == FORWARD && gchar_cursor() != NUL)
                    {
                        int bytelen = us_ptr2len_cc(ml_get_cursor());

                        /* put it on the next of the multi-byte character. */
                        col[0] += bytelen;
                        if (yanklen != 0)
                        {
                            curwin.w_cursor.col += bytelen;
                            curbuf.b_op_end.col += bytelen;
                        }
                    }
                    COPY_pos(curbuf.b_op_start, curwin.w_cursor);
                }
                /*
                 * Line mode: BACKWARD is the same as FORWARD on the previous line
                 */
                else if (dir == BACKWARD)
                    --lnum;

                pos_C new_cursor = new pos_C();
                COPY_pos(new_cursor, curwin.w_cursor);

                /*
                 * simple case: insert into current line
                 */
                if (y_type == MCHAR && y_size == 1)
                {
                    do
                    {
                        totlen = count * yanklen;
                        if (0 < totlen)
                        {
                            Bytes oldp = ml_get(lnum);
                            Bytes newp = new Bytes(strlen(oldp) + totlen + 1);

                            BCOPY(newp, oldp, col[0]);
                            Bytes p = newp.plus(col[0]);
                            for (long i = 0; i < count; i++)
                            {
                                BCOPY(p, y_array[0], yanklen);
                                p = p.plus(yanklen);
                            }
                            BCOPY(p, 0, oldp, col[0], strlen(oldp, col[0]) + 1);
                            ml_replace(lnum, newp, false);
                            /* Place cursor on last putted char. */
                            if (lnum == curwin.w_cursor.lnum)
                            {
                                /* make sure curwin.w_virtcol is updated */
                                changed_cline_bef_curs();
                                curwin.w_cursor.col += totlen - 1;
                            }
                        }
                        if (VIsual_active)
                            lnum++;
                    } while (VIsual_active && lnum <= curbuf.b_visual.vi_end.lnum);

                    if (VIsual_active) /* reset lnum to the last visual line */
                        lnum--;

                    COPY_pos(curbuf.b_op_end, curwin.w_cursor);
                    /* For "CTRL-O p" in Insert mode, put cursor after last char. */
                    if (totlen != 0 && (restart_edit != 0 || (flags & PUT_CURSEND) != 0))
                        curwin.w_cursor.col++;
                    changed_bytes(lnum, col[0]);
                }
                else
                {
                    /*
                    * Insert at least one line.  When y_type is MCHAR, break the first line in two.
                    */
                    error:
                    for (long cnt = 1; cnt <= count; cnt++)
                    {
                        int i = 0;
                        if (y_type == MCHAR)
                        {
                            /*
                             * Split the current line in two at the insert position.
                             * First insert y_array[size - 1] in front of second line.
                             * Then append y_array[0] to first line.
                             */
                            lnum = new_cursor.lnum;
                            Bytes p = ml_get(lnum).plus(col[0]);
                            totlen = strlen(y_array[y_size - 1]);
                            Bytes newp = new Bytes(strlen(p) + totlen + 1);
                            STRCPY(newp, y_array[y_size - 1]);
                            STRCAT(newp, p);
                            /* insert second line */
                            ml_append(lnum, newp, 0, false);

                            Bytes oldp = ml_get(lnum);
                            newp = new Bytes(col[0] + yanklen + 1);
                            /* copy first part of line */
                            BCOPY(newp, oldp, col[0]);
                            /* append to first line */
                            BCOPY(newp, col[0], y_array[0], 0, yanklen + 1);
                            ml_replace(lnum, newp, false);

                            curwin.w_cursor.lnum = lnum;
                            i = 1;
                        }

                        for ( ; i < y_size; i++)
                        {
                            if ((y_type != MCHAR || i < y_size - 1) && !ml_append(lnum, y_array[i], 0, false))
                                break error;
                            lnum++;
                            nr_lines++;
                            if ((flags & PUT_FIXINDENT) != 0)
                            {
                                pos_C old_pos = new pos_C();
                                COPY_pos(old_pos, curwin.w_cursor);
                                curwin.w_cursor.lnum = lnum;
                                Bytes p = ml_get(lnum);
                                if (cnt == count && i == y_size - 1)
                                    lendiff = strlen(p);
                                int indent;
                                if (p.at(0) == (byte)'#' && preprocs_left())
                                    indent = 0;     /* leave # lines at start */
                                else if (p.at(0) == NUL)
                                    indent = 0;     /* ignore empty lines */
                                else if (first_indent)
                                {
                                    indent_diff = orig_indent - get_indent();
                                    indent = orig_indent;
                                    first_indent = false;
                                }
                                else if ((indent = get_indent() + indent_diff) < 0)
                                    indent = 0;
                                set_indent(indent, 0);
                                COPY_pos(curwin.w_cursor, old_pos);
                                /* remember how many chars were removed */
                                if (cnt == count && i == y_size - 1)
                                    lendiff -= strlen(ml_get(lnum));
                            }
                        }
                    }

                    /* Adjust marks. */
                    if (y_type == MLINE)
                    {
                        curbuf.b_op_start.col = 0;
                        if (dir == FORWARD)
                            curbuf.b_op_start.lnum++;
                    }
                    mark_adjust(curbuf.b_op_start.lnum + (y_type == MCHAR ? 1 : 0), MAXLNUM, nr_lines, 0L);

                    /* note changed text for displaying and folding */
                    if (y_type == MCHAR)
                        changed_lines(curwin.w_cursor.lnum, col[0], curwin.w_cursor.lnum + 1, nr_lines);
                    else
                        changed_lines(curbuf.b_op_start.lnum, 0, curbuf.b_op_start.lnum, nr_lines);

                    /* put '] mark at last inserted character */
                    curbuf.b_op_end.lnum = lnum;
                    /* correct length for change in indent */
                    col[0] = strlen(y_array[y_size - 1]) - lendiff;
                    if (1 < col[0])
                        curbuf.b_op_end.col = col[0] - 1;
                    else
                        curbuf.b_op_end.col = 0;

                    if ((flags & PUT_CURSLINE) != 0)
                    {
                        /* ":put": put cursor on last inserted line */
                        curwin.w_cursor.lnum = lnum;
                        beginline(BL_WHITE | BL_FIX);
                    }
                    else if ((flags & PUT_CURSEND) != 0)
                    {
                        /* put cursor after inserted text */
                        if (y_type == MLINE)
                        {
                            if (curbuf.b_ml.ml_line_count <= lnum)
                                curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
                            else
                                curwin.w_cursor.lnum = lnum + 1;
                            curwin.w_cursor.col = 0;
                        }
                        else
                        {
                            curwin.w_cursor.lnum = lnum;
                            curwin.w_cursor.col = col[0];
                        }
                    }
                    else if (y_type == MLINE)
                    {
                        /* put cursor on first non-blank in first inserted line */
                        curwin.w_cursor.col = 0;
                        if (dir == FORWARD)
                            curwin.w_cursor.lnum++;
                        beginline(BL_WHITE | BL_FIX);
                    }
                    else        /* put cursor on first inserted character */
                        COPY_pos(curwin.w_cursor, new_cursor);
                }
            }

            msgmore(nr_lines);
            curwin.w_set_curswant = true;
        }

        VIsual_active = false;

        /* If the cursor is past the end of the line put it at the end. */
        adjust_cursor_eol();
    }

    /*
     * When the cursor is on the NUL past the end of the line
     * and it should not be there, move it left.
     */
    /*private*/ static void adjust_cursor_eol()
    {
        if (0 < curwin.w_cursor.col
                && gchar_cursor() == NUL
                && (ve_flags[0] & VE_ONEMORE) == 0
                && restart_edit == 0 && (State & INSERT) == 0)
        {
            /* Put the cursor on the last character in the line. */
            dec_cursor();

            if (ve_flags[0] == VE_ALL)
            {
                int[] scol = new int[1];
                int[] ecol = new int[1];

                /* Coladd is set to the width of the last character. */
                getvcol(curwin, curwin.w_cursor, scol, null, ecol);
                curwin.w_cursor.coladd = ecol[0] - scol[0] + 1;
            }
        }
    }

    /*
     * Return true if lines starting with '#' should be left aligned.
     */
    /*private*/ static boolean preprocs_left()
    {
        return (curbuf.b_p_si[0] && !curbuf.b_p_cin[0])
            || (curbuf.b_p_cin[0] && in_cinkeys('#', ' ', true) && curbuf.b_ind_hash_comment == 0);
    }

    /* Return the character name of the register with the given number. */
    /*private*/ static int get_register_name(int num)
    {
        if (num == -1)
            return '"';
        else if (num < 10)
            return num + '0';
        else if (num == DELETION_REGISTER)
            return '-';
        else if (num == STAR_REGISTER)
            return '*';
        else if (num == PLUS_REGISTER)
            return '+';
        else
            return num + 'a' - 10;
    }

    /*
     * ":dis" and ":registers": Display the contents of the yank registers.
     */
    /*private*/ static final ex_func_C ex_display = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes arg = eap.arg;
            if (arg != null && arg.at(0) == NUL)
                arg = null;
            int attr = hl_attr(HLF_8);

            /* Highlight title. */
            msg_puts_title(u8("\n--- Registers ---"));

            for (int i = -1; i < NUM_REGISTERS && !got_int; i++)
            {
                int name = get_register_name(i);
                if (arg != null && vim_strchr(arg, name) == null)
                    continue;       /* did not ask for this register */

                /* Adjust register name for "unnamed" in 'clipboard'.
                 * When it's a clipboard register, fill it with the current contents of the clipboard. */
                name = adjust_clip_reg(name);
                may_get_selection(name);

                yankreg_C yb;
                if (i == -1)
                {
                    if (y_previous != null)
                        yb = y_previous;
                    else
                        yb = y_regs[0];
                }
                else
                    yb = y_regs[i];

                if (name == utf_tolower(redir_reg) || (redir_reg == '"' && yb == y_previous))
                    continue;       /* do not list register being written to, the pointer can be freed */

                if (yb.y_array != null)
                {
                    msg_putchar('\n');
                    msg_putchar('"');
                    msg_putchar(name);
                    msg_puts(u8("   "));

                    int n = (int)Columns[0] - 6;
                    for (int j = 0; j < yb.y_size && 1 < n; j++)
                    {
                        if (j != 0)
                        {
                            msg_puts_attr(u8("^J"), attr);
                            n -= 2;
                        }

                        for (Bytes p = yb.y_array[j]; p.at(0) != NUL && 0 <= (n -= mb_ptr2cells(p)); p = p.plus(1))
                        {
                            int clen = us_ptr2len_cc(p);
                            msg_outtrans_len(p, clen);
                            p = p.plus(clen - 1);
                        }
                    }
                    if (1 < n && yb.y_type == MLINE)
                        msg_puts_attr(u8("^J"), attr);
                    out_flush();                    /* show one line at a time */
                }
                ui_breakcheck();
            }

            /*
             * display last inserted text
             */
            Bytes p = get_last_insert();
            if (p != null && (arg == null || vim_strchr(arg, '.') != null) && !got_int)
            {
                msg_puts(u8("\n\".   "));
                dis_msg(p, true);
            }

            /*
             * display last command line
             */
            if (last_cmdline != null && (arg == null || vim_strchr(arg, ':') != null) && !got_int)
            {
                msg_puts(u8("\n\":   "));
                dis_msg(last_cmdline, false);
            }

            /*
             * display current file name
             */
            if (curbuf.b_fname != null && (arg == null || vim_strchr(arg, '%') != null) && !got_int)
            {
                msg_puts(u8("\n\"%   "));
                dis_msg(curbuf.b_fname, false);
            }

            /*
             * display alternate file name
             */
            if ((arg == null || vim_strchr(arg, '%') != null) && !got_int)
            {
                Bytes[] fname = new Bytes[1];
                long[] dummy = new long[1];

                if (buflist_name_nr(0, fname, dummy) != false)
                {
                    msg_puts(u8("\n\"#   "));
                    dis_msg(fname[0], false);
                }
            }

            /*
             * display last search pattern
             */
            if (last_search_pat() != null && (arg == null || vim_strchr(arg, '/') != null) && !got_int)
            {
                msg_puts(u8("\n\"/   "));
                dis_msg(last_search_pat(), false);
            }

            /*
             * display last used expression
             */
            if (expr_line != null && (arg == null || vim_strchr(arg, '=') != null) && !got_int)
            {
                msg_puts(u8("\n\"=   "));
                dis_msg(expr_line, false);
            }
        }
    };

    /*
     * display a string for do_dis()
     * truncate at end of screen line
     */
    /*private*/ static void dis_msg(Bytes p, boolean skip_esc)
        /* skip_esc: if true, ignore trailing ESC */
    {
        int n = (int)Columns[0] - 6;
        while (p.at(0) != NUL && !(p.at(0) == ESC && skip_esc && p.at(1) == NUL) && 0 <= (n -= mb_ptr2cells(p)))
        {
            int len = us_ptr2len_cc(p);
            if (1 < len)
            {
                msg_outtrans_len(p, len);
                p = p.plus(len);
            }
            else
            {
                msg_outtrans_len(p, 1);
                p = p.plus(1);
            }
        }
        ui_breakcheck();
    }

    /*
     * If "process" is true and the line begins with a comment leader (possibly
     * after some white space), return a pointer to the text after it.  Put a boolean
     * value indicating whether the line ends with an unclosed comment in "is_comment".
     * line - line to be processed,
     * process - if false, will only check whether the line ends with an unclosed comment,
     * include_space - whether to also skip space following the comment leader,
     * is_comment - will indicate whether the current line ends with an unclosed comment.
     */
    /*private*/ static Bytes skip_comment(Bytes line, boolean process, boolean include_space, boolean[] is_comment)
    {
        Bytes[] flags = { null };
        int leader_offset = get_last_leader_offset(line, flags);

        is_comment[0] = false;
        if (leader_offset != -1)
        {
            /* Let's check whether the line ends with an unclosed comment.
             * If the last comment leader has COM_END in flags, there's no comment.
             */
            while (flags[0].at(0) != NUL)
            {
                if (flags[0].at(0) == COM_END || flags[0].at(0) == (byte)':')
                    break;
                flags[0] = flags[0].plus(1);
            }
            if (flags[0].at(0) != COM_END)
                is_comment[0] = true;
        }

        if (process == false)
            return line;

        int lead_len = get_leader_len(line, flags, false, include_space);
        if (lead_len == 0)
            return line;

        /* Find:
         * - COM_END,
         * - colon,
         * whichever comes first.
         */
        while (flags[0].at(0) != NUL)
        {
            if (flags[0].at(0) == COM_END || flags[0].at(0) == (byte)':')
                break;
            flags[0] = flags[0].plus(1);
        }

        /* If we found a colon, it means that we are not processing a line
         * starting with a closing part of a three-part comment.  That's good,
         * because we don't want to remove those as this would be annoying.
         */
        if (flags[0].at(0) == (byte)':' || flags[0].at(0) == NUL)
            line = line.plus(lead_len);

        return line;
    }

    /*
     * Join 'count' lines (minimal 2) at cursor position.
     * When "save_undo" is true save lines for undo first.
     * Set "use_formatoptions" to false when e.g. processing backspace and comment
     * leaders should not be removed.
     * When setmark is true, sets the '[ and '] mark, else, the caller is expected
     * to set those marks.
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean do_join(int count, boolean insert_space, boolean save_undo, boolean use_formatoptions, boolean setmark)
    {
        boolean retval = true;

        Bytes curr = null;
        Bytes curr_start = null;
        int endcurr1 = NUL;
        int endcurr2 = NUL;
        int currsize = 0;               /* size of the current line */
        int sumsize = 0;                /* size of the long new line */
        int col = 0;

        boolean remove_comments = (use_formatoptions == true) && has_format_option(FO_REMOVE_COMS);

        if (save_undo && !u_save(curwin.w_cursor.lnum - 1, curwin.w_cursor.lnum + count))
            return false;

        /* Allocate an array to store the number of spaces inserted before each line.
         * We will use it to pre-compute the length of the new line and the
         * proper placement of each original line in the new one. */
        int[] spaces = new int[count];

        int[] comments = null;
        if (remove_comments)
            comments = new int[count];

        /*
         * Don't move anything, just compute the final line length
         * and setup the array of space strings lengths.
         */
        for (int t = 0; t < count; t++)
        {
            curr = curr_start = ml_get(curwin.w_cursor.lnum + t);
            if (t == 0 && setmark)
            {
                /* Set the '[ mark. */
                curwin.w_buffer.b_op_start.lnum = curwin.w_cursor.lnum;
                curwin.w_buffer.b_op_start.col  = strlen(curr);
            }
            if (remove_comments)
            {
                boolean[] prev_was_comment = new boolean[1];

                /* We don't want to remove the comment leader if the previous line is not a comment. */
                if (0 < t && prev_was_comment[0])
                {
                    Bytes new_curr = skip_comment(curr, true, insert_space, prev_was_comment);
                    comments[t] = BDIFF(new_curr, curr);
                    curr = new_curr;
                }
                else
                    curr = skip_comment(curr, false, insert_space, prev_was_comment);
            }

            if (insert_space && 0 < t)
            {
                curr = skipwhite(curr);
                if (curr.at(0) != (byte)')' && currsize != 0 && endcurr1 != TAB
                        && (!has_format_option(FO_MBYTE_JOIN)
                            || (us_ptr2char(curr) < 0x100 && endcurr1 < 0x100))
                        && (!has_format_option(FO_MBYTE_JOIN2)
                            || us_ptr2char(curr) < 0x100 || endcurr1 < 0x100))
                {
                    /* don't add a space if the line is ending in a space */
                    if (endcurr1 == ' ')
                        endcurr1 = endcurr2;
                    else
                        ++spaces[t];
                    /* extra space when 'joinspaces' set and line ends in '.' */
                    if (p_js[0]
                            && (endcurr1 == '.'
                                || (vim_strbyte(p_cpo[0], CPO_JOINSP) == null
                                    && (endcurr1 == '?' || endcurr1 == '!'))))
                        ++spaces[t];
                }
            }
            currsize = strlen(curr);
            sumsize += currsize + spaces[t];
            endcurr1 = endcurr2 = NUL;
            if (insert_space && 0 < currsize)
            {
                Bytes cend = curr.plus(currsize);
                cend = cend.minus(us_ptr_back(curr, cend));
                endcurr1 = us_ptr2char(cend);
                if (BLT(curr, cend))
                {
                    cend = cend.minus(us_ptr_back(curr, cend));
                    endcurr2 = us_ptr2char(cend);
                }
            }
            line_breakcheck();
            if (got_int)
                return false;
        }

        /* store the column position before last line */
        col = sumsize - currsize - spaces[count - 1];

        /* allocate the space for the new line */
        Bytes newp = new Bytes(sumsize + 1);
        Bytes cend = newp.plus(sumsize);
        cend.be(0, NUL);

        /*
         * Move affected lines to the new long one.
         *
         * Move marks from each deleted line to the joined line, adjusting the
         * column.  This is not Vi compatible, but Vi deletes the marks, thus that
         * should not really be a problem.
         */
        for (int t = count - 1; ; --t)
        {
            cend = cend.minus(currsize);
            BCOPY(cend, curr, currsize);
            if (0 < spaces[t])
            {
                cend = cend.minus(spaces[t]);
                copy_spaces(cend, spaces[t]);
            }
            mark_col_adjust(curwin.w_cursor.lnum + t, 0, (long)-t,
                             (long)(BDIFF(cend, newp) + spaces[t] - BDIFF(curr, curr_start)));
            if (t == 0)
                break;
            curr = curr_start = ml_get(curwin.w_cursor.lnum + t - 1);
            if (remove_comments)
                curr = curr.plus(comments[t - 1]);
            if (insert_space && 1 < t)
                curr = skipwhite(curr);
            currsize = strlen(curr);
        }
        ml_replace(curwin.w_cursor.lnum, newp, false);

        if (setmark)
        {
            /* Set the '] mark. */
            curwin.w_buffer.b_op_end.lnum = curwin.w_cursor.lnum;
            curwin.w_buffer.b_op_end.col  = strlen(newp);
        }

        /* Only report the change in the first line here,
         * del_lines() will report the deleted line. */
        changed_lines(curwin.w_cursor.lnum, currsize, curwin.w_cursor.lnum + 1, 0L);

        /*
         * Delete following lines.  To do this we move the cursor there
         * briefly, and then move it back.  After del_lines() the cursor may
         * have moved up (last line deleted), so the current lnum is kept in t.
         */
        long t = curwin.w_cursor.lnum;
        curwin.w_cursor.lnum++;
        del_lines(count - 1, false);
        curwin.w_cursor.lnum = t;

        /*
         * Set the cursor column:
         * Vi compatible: use the column of the first join
         * vim:           use the column of the last join
         */
        curwin.w_cursor.col = (vim_strbyte(p_cpo[0], CPO_JOINCOL) != null) ? currsize : col;
        check_cursor_col();

        curwin.w_cursor.coladd = 0;
        curwin.w_set_curswant = true;

        return retval;
    }

    /*
     * Return true if the two comment leaders given are the same.
     * "lnum" is the first line.  White-space is ignored.
     * Note that the whole of 'leader1' must match 'leader2_len' characters from 'leader2'.
     */
    /*private*/ static boolean same_leader(long lnum, int leader1_len, Bytes leader1_flags, int leader2_len, Bytes leader2_flags)
    {
        int idx1 = 0, idx2 = 0;

        if (leader1_len == 0)
            return (leader2_len == 0);

        /*
         * If first leader has 'f' flag, the lines can be joined only
         * if the second line does not have a leader.
         * If first leader has 'e' flag, the lines can never be joined.
         * If first leader has 's' flag, the lines can only be joined
         * if there is some text after it and the second line has the 'm' flag.
         */
        if (leader1_flags != null)
        {
            for (Bytes p = leader1_flags; p.at(0) != NUL && p.at(0) != (byte)':'; p = p.plus(1))
            {
                if (p.at(0) == COM_FIRST)
                    return (leader2_len == 0);
                if (p.at(0) == COM_END)
                    return false;
                if (p.at(0) == COM_START)
                {
                    if (ml_get(lnum).at(leader1_len) == NUL)
                        return false;
                    if (leader2_flags == null || leader2_len == 0)
                        return false;
                    for (p = leader2_flags; p.at(0) != NUL && p.at(0) != (byte)':'; p = p.plus(1))
                        if (p.at(0) == COM_MIDDLE)
                            return true;

                    return false;
                }
            }
        }

        /*
         * Get current line and next line, compare the leaders.
         * The first line has to be saved, only one line can be locked at a time.
         */
        Bytes line1 = STRDUP(ml_get(lnum));

        for (idx1 = 0; vim_iswhite(line1.at(idx1)); ++idx1)
            ;
        Bytes line2 = ml_get(lnum + 1);
        for (idx2 = 0; idx2 < leader2_len; ++idx2)
        {
            if (!vim_iswhite(line2.at(idx2)))
            {
                if (line1.at(idx1++) != line2.at(idx2))
                    break;
            }
            else
                while (vim_iswhite(line1.at(idx1)))
                    idx1++;
        }

        return (idx2 == leader2_len && idx1 == leader1_len);
    }

    /*
     * Implementation of the format operator 'gq'.
     */
    /*private*/ static void op_format(oparg_C oap, boolean keep_cursor)
        /* keep_cursor: keep cursor on same text char */
    {
        long old_line_count = curbuf.b_ml.ml_line_count;

        /* Place the cursor where the "gq" or "gw" command was given, so that "u" can put it back there. */
        COPY_pos(curwin.w_cursor, oap.cursor_start);

        if (!u_save(oap.op_start.lnum - 1, oap.op_end.lnum + 1))
            return;
        COPY_pos(curwin.w_cursor, oap.op_start);

        if (oap.is_VIsual)
            /* When there is no change: need to remove the Visual selection. */
            redraw_curbuf_later(INVERTED);

        /* Set '[ mark at the start of the formatted area. */
        COPY_pos(curbuf.b_op_start, oap.op_start);

        /* For "gw" remember the cursor position and put it back below (adjusted for joined and split lines). */
        if (keep_cursor)
            COPY_pos(saved_cursor, oap.cursor_start);

        format_lines(oap.line_count, keep_cursor);

        /*
         * Leave the cursor at the first non-blank of the last formatted line.
         * If the cursor was moved one line back (e.g. with "Q}") go to the next line,
         * so "." will do the next lines.
         */
        if (oap.end_adjusted && curwin.w_cursor.lnum < curbuf.b_ml.ml_line_count)
            curwin.w_cursor.lnum++;
        beginline(BL_WHITE | BL_FIX);
        old_line_count = curbuf.b_ml.ml_line_count - old_line_count;
        msgmore(old_line_count);

        /* put '] mark on the end of the formatted area */
        COPY_pos(curbuf.b_op_end, curwin.w_cursor);

        if (keep_cursor)
        {
            COPY_pos(curwin.w_cursor, saved_cursor);
            saved_cursor.lnum = 0;
        }

        if (oap.is_VIsual)
        {
            for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            {
                if (wp.w_old_cursor_lnum != 0)
                {
                    /* When lines have been inserted or deleted,
                     * adjust the end of the Visual area to be redrawn. */
                    if (wp.w_old_visual_lnum < wp.w_old_cursor_lnum)
                        wp.w_old_cursor_lnum += old_line_count;
                    else
                        wp.w_old_visual_lnum += old_line_count;
                }
            }
        }
    }

    /*
     * Implementation of the format operator 'gq' for when using 'formatexpr'.
     */
    /*private*/ static void op_formatexpr(oparg_C oap)
    {
        if (oap.is_VIsual)
            /* When there is no change: need to remove the Visual selection. */
            redraw_curbuf_later(INVERTED);

        if (fex_format(oap.op_start.lnum, oap.line_count, NUL) != 0)
            /* As documented:
             * when 'formatexpr' returns non-zero fall back to internal formatting. */
            op_format(oap, false);
    }

    /*private*/ static int fex_format(long lnum, long count, int c)
        /* c: character to be inserted */
    {
        boolean use_sandbox = was_set_insecurely(u8("formatexpr"), OPT_LOCAL);

        /*
         * Set v:lnum to the first line number and v:count to the number of lines.
         * Set v:char to the character to be inserted (can be NUL).
         */
        set_vim_var_nr(VV_LNUM, lnum);
        set_vim_var_nr(VV_COUNT, count);
        set_vim_var_char(c);

        /*
         * Evaluate the function.
         */
        if (use_sandbox)
            sandbox++;
        int r = eval_to_number(curbuf.b_p_fex[0]);
        if (use_sandbox)
            --sandbox;

        set_vim_var_string(VV_CHAR, null, -1);

        return r;
    }

    /*
     * Format "line_count" lines, starting at the cursor position.
     * When "line_count" is negative, format until the end of the paragraph.
     * Lines after the cursor line are saved for undo, caller must have saved the first line.
     */
    /*private*/ static void format_lines(long line_count, boolean avoid_fex)
        /* avoid_fex: don't use 'formatexpr' */
    {
        boolean prev_is_end_par = false;        /* prev. line not part of parag. */
        boolean next_is_start_par = false;
        boolean do_comments_list = false;               /* format comments with 'n' or '2' */
        boolean advance = true;
        int second_indent = -1;                 /* indent for second line (comment aware) */
        boolean first_par_line = true;
        boolean need_set_indent = true;         /* set indent of next paragraph */
        boolean force_format = false;

        int old_State = State;

        /* length of a line to force formatting: 3 * 'tw' */
        int max_len = comp_textwidth(true) * 3;

        /* check for 'q', '2' and '1' in 'formatoptions' */
        boolean do_comments = has_format_option(FO_Q_COMS);         /* format comments */
        boolean do_second_indent = has_format_option(FO_Q_SECOND);
        boolean do_number_indent = has_format_option(FO_Q_NUMBER);
        boolean do_trail_white = has_format_option(FO_WHITE_PAR);

        /*
         * Get info about the previous and current line.
         */
        boolean is_not_par = true;              /* current line not part of parag. */
        int[] leader_len = { 0 };                     /* leader len of current line */
        Bytes[] leader_flags = { null };             /* flags for leader of current line */
        if (1 < curwin.w_cursor.lnum)
            is_not_par = fmt_check_par(curwin.w_cursor.lnum - 1, leader_len, leader_flags, do_comments);

        boolean next_is_not_par;                /* next line not part of paragraph */
        int[] next_leader_len = new int[1];                    /* leader len of next line */
        Bytes[] next_leader_flags = new Bytes[1];               /* flags for leader of next line */
        next_is_not_par = fmt_check_par(curwin.w_cursor.lnum, next_leader_len, next_leader_flags, do_comments);

        boolean is_end_par = (is_not_par || next_is_not_par);       /* at end of paragraph */
        if (!is_end_par && do_trail_white)
            is_end_par = !ends_in_white(curwin.w_cursor.lnum - 1);

        --curwin.w_cursor.lnum;
        for (long count = line_count; count != 0 && !got_int; --count)
        {
            /*
             * Advance to next paragraph.
             */
            if (advance)
            {
                curwin.w_cursor.lnum++;
                prev_is_end_par = is_end_par;
                is_not_par = next_is_not_par;
                leader_len[0] = next_leader_len[0];
                leader_flags[0] = next_leader_flags[0];
            }

            /*
             * The last line to be formatted.
             */
            if (count == 1 || curwin.w_cursor.lnum == curbuf.b_ml.ml_line_count)
            {
                next_is_not_par = true;
                next_leader_len[0] = 0;
                next_leader_flags[0] = null;
            }
            else
            {
                next_is_not_par = fmt_check_par(curwin.w_cursor.lnum + 1,
                                        next_leader_len, next_leader_flags, do_comments);
                if (do_number_indent)
                    next_is_start_par = (0 < get_number_indent(curwin.w_cursor.lnum + 1));
            }
            advance = true;
            is_end_par = (is_not_par || next_is_not_par || next_is_start_par);
            if (!is_end_par && do_trail_white)
                is_end_par = !ends_in_white(curwin.w_cursor.lnum);

            /*
             * Skip lines that are not in a paragraph.
             */
            if (is_not_par)
            {
                if (line_count < 0)
                    break;
            }
            else
            {
                /*
                 * For the first line of a paragraph, check indent of second line.
                 * Don't do this for comments and empty lines.
                 */
                if (first_par_line
                        && (do_second_indent || do_number_indent)
                        && prev_is_end_par
                        && curwin.w_cursor.lnum < curbuf.b_ml.ml_line_count)
                {
                    if (do_second_indent && !lineempty(curwin.w_cursor.lnum + 1))
                    {
                        if (leader_len[0] == 0 && next_leader_len[0] == 0)
                        {
                            /* no comment found */
                            second_indent = get_indent_lnum(curwin.w_cursor.lnum + 1);
                        }
                        else
                        {
                            second_indent = next_leader_len[0];
                            do_comments_list = true;
                        }
                    }
                    else if (do_number_indent)
                    {
                        if (leader_len[0] == 0 && next_leader_len[0] == 0)
                        {
                            /* no comment found */
                            second_indent = get_number_indent(curwin.w_cursor.lnum);
                        }
                        else
                        {
                            /* get_number_indent() is now "comment aware"... */
                            second_indent = get_number_indent(curwin.w_cursor.lnum);
                            do_comments_list = true;
                        }
                    }
                }

                /*
                 * When the comment leader changes, it's the end of the paragraph.
                 */
                if (curbuf.b_ml.ml_line_count <= curwin.w_cursor.lnum
                        || !same_leader(curwin.w_cursor.lnum, leader_len[0], leader_flags[0],
                                                        next_leader_len[0], next_leader_flags[0]))
                    is_end_par = true;

                /*
                 * If we have got to the end of a paragraph, or the line is getting long, format it.
                 */
                if (is_end_par || force_format)
                {
                    if (need_set_indent)
                        /* Replace indent in first line with minimal number of tabs and spaces,
                         * according to current options. */
                        set_indent(get_indent(), SIN_CHANGED);

                    /* put cursor on last non-space */
                    State = NORMAL; /* don't go past end-of-line */
                    coladvance(MAXCOL);
                    while (curwin.w_cursor.col != 0 && vim_isspace(gchar_cursor()))
                        dec_cursor();

                    /* do the formatting, without 'showmode' */
                    State = INSERT; /* for open_line() */
                    boolean smd_save = p_smd[0];
                    p_smd[0] = false;
                    insertchar(NUL, INSCHAR_FORMAT
                            + (do_comments ? INSCHAR_DO_COM : 0)
                            + (do_comments && do_comments_list ? INSCHAR_COM_LIST : 0)
                            + (avoid_fex ? INSCHAR_NO_FEX : 0), second_indent);
                    State = old_State;
                    p_smd[0] = smd_save;
                    second_indent = -1;
                    /* at end of par.: need to set indent of next par. */
                    need_set_indent = is_end_par;
                    if (is_end_par)
                    {
                        /* When called with a negative line count,
                         * break at the end of the paragraph. */
                        if (line_count < 0)
                            break;
                        first_par_line = true;
                    }
                    force_format = false;
                }

                /*
                 * When still in same paragraph, join the lines together.
                 * But first delete the leader from the second line.
                 */
                if (!is_end_par)
                {
                    advance = false;
                    curwin.w_cursor.lnum++;
                    curwin.w_cursor.col = 0;
                    if (line_count < 0 && !u_save_cursor())
                        break;
                    if (0 < next_leader_len[0])
                    {
                        del_bytes(next_leader_len[0], false, false);
                        mark_col_adjust(curwin.w_cursor.lnum, 0, 0L, (long)-next_leader_len[0]);
                    }
                    else if (0 < second_indent)     /* the "leader" for FO_Q_SECOND */
                    {
                        Bytes p = ml_get_curline();
                        int indent = BDIFF(skipwhite(p), p);

                        if (0 < indent)
                        {
                            del_bytes(indent, false, false);
                            mark_col_adjust(curwin.w_cursor.lnum, 0, 0L, (long)-indent);
                        }
                    }
                    --curwin.w_cursor.lnum;
                    if (do_join(2, true, false, false, false) == false)
                    {
                        beep_flush();
                        break;
                    }
                    first_par_line = false;
                    /* If the line is getting long, format it next time. */
                    force_format = (max_len < strlen(ml_get_curline()));
                }
            }
            line_breakcheck();
        }
    }

    /*
     * Return true if line "lnum" ends in a white character.
     */
    /*private*/ static boolean ends_in_white(long lnum)
    {
        Bytes s = ml_get(lnum);
        if (s.at(0) == NUL)
            return false;

        return vim_iswhite(s.at(strlen(s) - 1));
    }

    /*
     * Blank lines, and lines containing only the comment leader, are left untouched
     * by the formatting.  The function returns true in this case.
     * It also returns true when a line starts with the end of a comment ('e' in comment flags),
     * so that this line is skipped, and not joined to the previous line.
     * A new paragraph starts after a blank line, or when the comment leader changes.
     */
    /*private*/ static boolean fmt_check_par(long lnum, int[] leader_len, Bytes[] leader_flags, boolean do_comments)
    {
        Bytes flags = null;
        Bytes ptr = ml_get(lnum);
        if (do_comments)
            leader_len[0] = get_leader_len(ptr, leader_flags, false, true);
        else
            leader_len[0] = 0;

        if (0 < leader_len[0])
        {
            /*
             * Search for 'e' flag in comment leader flags.
             */
            flags = leader_flags[0];
            while (flags.at(0) != NUL && flags.at(0) != (byte)':' && flags.at(0) != COM_END)
                flags = flags.plus(1);
        }

        return (skipwhite(ptr.plus(leader_len[0])).at(0) == NUL
                    || (0 < leader_len[0] && flags.at(0) == COM_END)
                    || startPS(lnum, NUL, false));
    }

    /*
     * Return true when a paragraph starts in line "lnum".
     * Return false when the previous line is in the same paragraph.
     * Used for auto-formatting.
     */
    /*private*/ static boolean paragraph_start(long lnum)
    {
        if (lnum <= 1)
            return true;                /* start of the file */

        Bytes p = ml_get(lnum - 1);
        if (p.at(0) == NUL)
            return true;                /* after empty line */

        boolean do_comments = has_format_option(FO_Q_COMS);     /* format comments */

        int[] leader_len = { 0 };             /* leader len of current line */
        Bytes[] leader_flags = { null };     /* flags for leader of current line */

        if (fmt_check_par(lnum - 1, leader_len, leader_flags, do_comments))
            return true;                /* after non-paragraph line */

        int[] next_leader_len = new int[1];            /* leader len of next line */
        Bytes[] next_leader_flags = new Bytes[1];       /* flags for leader of next line */

        if (fmt_check_par(lnum, next_leader_len, next_leader_flags, do_comments))
            return true;                /* "lnum" is not a paragraph line */

        if (has_format_option(FO_WHITE_PAR) && !ends_in_white(lnum - 1))
            return true;                /* missing trailing space in previous line. */

        if (has_format_option(FO_Q_NUMBER) && (0 < get_number_indent(lnum)))
            return true;                /* numbered item starts in "lnum". */

        if (!same_leader(lnum - 1, leader_len[0], leader_flags[0], next_leader_len[0], next_leader_flags[0]))
            return true;                /* change of comment leader. */

        return false;
    }

    /*
     * prepare a few things for block mode yank/delete/tilde
     *
     * for delete:
     * - textlen includes the first/last char to be (partly) deleted
     * - start/endspaces is the number of columns that are taken by the
     *   first/last deleted char minus the number of columns that have to be deleted.
     * for yank and tilde:
     * - textlen includes the first/last char to be wholly yanked
     * - start/endspaces is the number of columns of the first/last yanked char
     *   that are to be yanked.
     */
    /*private*/ static void block_prep(oparg_C oap, block_def_C bdp, long lnum, boolean is_del)
    {
        int incr = 0;

        bdp.startspaces = 0;
        bdp.endspaces = 0;
        bdp.textlen = 0;
        bdp.start_vcol = 0;
        bdp.end_vcol = 0;
        bdp.is_short = false;
        bdp.is_oneChar = false;
        bdp.pre_whitesp = 0;
        bdp.pre_whitesp_c = 0;
        bdp.end_char_vcols = 0;
        bdp.start_char_vcols = 0;

        Bytes line = ml_get(lnum);

        Bytes pstart = line;
        Bytes prev_pstart = line;

        while (bdp.start_vcol < oap.start_vcol && pstart.at(0) != NUL)
        {
            /* Count a tab for what it's worth (if list mode not on). */
            incr = lbr_chartabsize(line, pstart, bdp.start_vcol);
            bdp.start_vcol += incr;
            if (vim_iswhite(pstart.at(0)))
            {
                bdp.pre_whitesp += incr;
                bdp.pre_whitesp_c++;
            }
            else
            {
                bdp.pre_whitesp = 0;
                bdp.pre_whitesp_c = 0;
            }
            prev_pstart = pstart;
            pstart = pstart.plus(us_ptr2len_cc(pstart));
        }

        bdp.start_char_vcols = incr;

        if (bdp.start_vcol < oap.start_vcol)        /* line too short */
        {
            bdp.end_vcol = bdp.start_vcol;
            bdp.is_short = true;
            if (!is_del || oap.op_type == OP_APPEND)
                bdp.endspaces = oap.end_vcol - oap.start_vcol + 1;
        }
        else
        {
            /* notice: this converts partly selected Multibyte characters to spaces, too. */
            bdp.startspaces = bdp.start_vcol - oap.start_vcol;
            if (is_del && bdp.startspaces != 0)
                bdp.startspaces = bdp.start_char_vcols - bdp.startspaces;
            Bytes[] pend = { pstart };
            bdp.end_vcol = bdp.start_vcol;
            if (oap.end_vcol < bdp.end_vcol)        /* it's all in one character */
            {
                bdp.is_oneChar = true;
                if (oap.op_type == OP_INSERT)
                    bdp.endspaces = bdp.start_char_vcols - bdp.startspaces;
                else if (oap.op_type == OP_APPEND)
                {
                    bdp.startspaces += oap.end_vcol - oap.start_vcol + 1;
                    bdp.endspaces = bdp.start_char_vcols - bdp.startspaces;
                }
                else
                {
                    bdp.startspaces = oap.end_vcol - oap.start_vcol + 1;
                    if (is_del && oap.op_type != OP_LSHIFT)
                    {
                        /* just putting the sum of those two into
                         * bdp.startspaces doesn't work for Visual replace,
                         * so we have to split the tab in two */
                        bdp.startspaces = bdp.start_char_vcols - (bdp.start_vcol - oap.start_vcol);
                        bdp.endspaces = bdp.end_vcol - oap.end_vcol - 1;
                    }
                }
            }
            else
            {
                Bytes prev_pend = pend[0];
                while (bdp.end_vcol <= oap.end_vcol && pend[0].at(0) != NUL)
                {
                    /* Count a tab for what it's worth (if list mode not on). */
                    prev_pend = pend[0];
                    incr = lbr_chartabsize_adv(line, pend, bdp.end_vcol);
                    bdp.end_vcol += incr;
                }
                if (bdp.end_vcol <= oap.end_vcol
                        && (!is_del
                            || oap.op_type == OP_APPEND
                            || oap.op_type == OP_REPLACE)) /* line too short */
                {
                    bdp.is_short = true;
                    /* Alternative: include spaces to fill up the block.
                     * Disadvantage: can lead to trailing spaces when
                     * the line is short where the text is put. */
                    /* if (!is_del || oap.op_type == OP_APPEND) */
                    if (oap.op_type == OP_APPEND || virtual_op != FALSE)
                        bdp.endspaces = oap.end_vcol - bdp.end_vcol + (oap.inclusive ? 1 : 0);
                    else
                        bdp.endspaces = 0; /* replace doesn't add characters */
                }
                else if (oap.end_vcol < bdp.end_vcol)
                {
                    bdp.endspaces = bdp.end_vcol - oap.end_vcol - 1;
                    if (!is_del && bdp.endspaces != 0)
                    {
                        bdp.endspaces = incr - bdp.endspaces;
                        if (BNE(pend[0], pstart))
                            pend[0] = prev_pend;
                    }
                }
            }
            bdp.end_char_vcols = incr;
            if (is_del && bdp.startspaces != 0)
                pstart = prev_pstart;
            bdp.textlen = BDIFF(pend[0], pstart);
        }

        bdp.textcol = BDIFF(pstart, line);
        bdp.textstart = pstart;
    }

    /*private*/ static void reverse_line(Bytes s)
    {
        int i = strlen(s) - 1;
        if (i <= 0)
            return;

        curwin.w_cursor.col = i - curwin.w_cursor.col;
        for (int j = 0; j < i; j++, i--)
        {
            byte c = s.at(i);
            s.be(i, s.at(j));
            s.be(j, c);
        }
    }

    /*private*/ static boolean hexupper;                                /* 0xABC */

    /*
     * add or subtract 'Prenum1' from a number in a line
     * 'command' is CTRL-A for add, CTRL-X for subtract
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean do_addsub(int command, long Prenum1)
    {
        boolean dohex = (vim_strchr(curbuf.b_p_nf[0], 'x') != null);   /* "heX" */
        boolean dooct = (vim_strchr(curbuf.b_p_nf[0], 'o') != null);   /* "Octal" */
        boolean doalp = (vim_strchr(curbuf.b_p_nf[0], 'p') != null);   /* "alPha" */

        Bytes ptr = ml_get_curline();
        if (curwin.w_onebuf_opt.wo_rl[0])
            reverse_line(ptr);

        /*
         * First check if we are on a hexadecimal number, after the "0x".
         */
        int col = curwin.w_cursor.col;
        if (dohex)
            while (0 < col && asc_isxdigit(ptr.at(col)))
                --col;
        if (dohex
                && 0 < col
                && (ptr.at(col) == (byte)'X' || ptr.at(col) == (byte)'x')
                && ptr.at(col - 1) == (byte)'0'
                && asc_isxdigit(ptr.at(col + 1)))
        {
            /*
             * Found hexadecimal number, move to its start.
             */
            --col;
        }
        else
        {
            /*
             * Search forward and then backward to find the start of number.
             */
            col = curwin.w_cursor.col;

            while (ptr.at(col) != NUL
                    && !asc_isdigit(ptr.at(col))
                    && !(doalp && asc_isalpha(ptr.at(col))))
                col++;

            while (0 < col
                    && asc_isdigit(ptr.at(col - 1))
                    && !(doalp && asc_isalpha(ptr.at(col))))
                --col;
        }

        /*
         * If a number was found, and saving for undo works, replace the number.
         */
        int firstdigit = ptr.at(col);
        if (curwin.w_onebuf_opt.wo_rl[0])
            reverse_line(ptr);
        if ((!asc_isdigit(firstdigit) && !(doalp && asc_isalpha(firstdigit))) || !u_save_cursor())
        {
            beep_flush();
            return false;
        }

        /* get 'ptr' again, because u_save() may have changed it */
        ptr = ml_get_curline();
        if (curwin.w_onebuf_opt.wo_rl[0])
            reverse_line(ptr);

        if (doalp && asc_isalpha(firstdigit))
        {
            /* decrement or increment alphabetic character */
            if (command == Ctrl_X)
            {
                if (charOrd(firstdigit) < Prenum1)
                {
                    if (asc_isupper(firstdigit))
                        firstdigit = 'A';
                    else
                        firstdigit = 'a';
                }
                else
                    firstdigit -= Prenum1;
            }
            else
            {
                if (26 - charOrd(firstdigit) - 1 < Prenum1)
                {
                    if (asc_isupper(firstdigit))
                        firstdigit = 'Z';
                    else
                        firstdigit = 'z';
                }
                else
                    firstdigit += Prenum1;
            }
            curwin.w_cursor.col = col;
            del_char(false);
            ins_char(firstdigit);
        }
        else
        {
            boolean negative = false;
            if (0 < col && ptr.at(col - 1) == (byte)'-')         /* negative number */
            {
                --col;
                negative = true;
            }

            int[] hex = new int[1];                                    /* 'X' or 'x': hex; '0': octal */
            int[] length = { 0 };                             /* character length of the number */
            long[] n = new long[1];
            /* get the number value (unsigned) */
            vim_str2nr(ptr.plus(col), hex, length, dooct ? TRUE : FALSE, dohex ? TRUE : FALSE, n);
            if (n[0] < 0)
                n[0] = -n[0];

            /* ignore leading '-' for hex and octal numbers */
            if (hex[0] != 0 && negative)
            {
                col++;
                --length[0];
                negative = false;
            }

            /* add or subtract */
            boolean subtract = false;
            if (command == Ctrl_X)
                subtract ^= true;
            if (negative)
                subtract ^= true;

            long oldn = n[0];
            if (subtract)
                n[0] -= Prenum1;
            else
                n[0] += Prenum1;

            final long roof = 0x7fffffffffffffffL;
            n[0] &= roof;

            /* handle wraparound for decimal numbers */
            if (hex[0] == 0)
            {
                if (subtract)
                {
                    if (oldn < n[0])
                    {
                        n[0] = 1 + (n[0] ^ roof);
                        negative ^= true;
                    }
                }
                else /* add */
                {
                    if (n[0] < oldn)
                    {
                        n[0] = (n[0] ^ roof);
                        negative ^= true;
                    }
                }
                if (n[0] == 0)
                    negative = false;
            }

            /*
             * Delete the old number.
             */
            curwin.w_cursor.col = col;
            int todel = length[0];
            int c = gchar_cursor();
            /*
             * Don't include the '-' in the length, only the length of the part
             * after it is kept the same.
             */
            if (c == '-')
                --length[0];
            while (0 < todel--)
            {
                if (c < 0x100 && asc_isalpha(c))
                {
                    if (asc_isupper(c))
                        hexupper = true;
                    else
                        hexupper = false;
                }
                /* del_char() will mark line needing displaying */
                del_char(false);
                c = gchar_cursor();
            }

            /*
             * Prepare the leading characters in buf1[].
             * When there are many leading zeros it could be very long.
             * Allocate a bit too much.
             */
            Bytes buf1 = new Bytes(length[0] + NUMBUFLEN);

            ptr = buf1;
            if (negative)
            {
                (ptr = ptr.plus(1)).be(-1, (byte)'-');
            }
            if (hex[0] != 0)
            {
                (ptr = ptr.plus(1)).be(-1, (byte)'0');
                --length[0];
            }
            if (hex[0] == 'x' || hex[0] == 'X')
            {
                (ptr = ptr.plus(1)).be(-1, hex[0]);
                --length[0];
            }

            /*
             * Put the number characters in buf2[].
             */
            Bytes buf2 = new Bytes(NUMBUFLEN);
            if (hex[0] == 0)
                libC.sprintf(buf2, u8("%ld"), n[0]);
            else if (hex[0] == '0')
                libC.sprintf(buf2, u8("%lo"), n[0]);
            else if (hex[0] != 0 && hexupper)
                libC.sprintf(buf2, u8("%lX"), n[0]);
            else
                libC.sprintf(buf2, u8("%lx"), n[0]);
            length[0] -= strlen(buf2);

            /*
             * Adjust number of zeros to the new number of digits,
             * so the total length of the number remains the same.
             * Don't do this when the result may look like an octal number.
             */
            if (firstdigit == '0' && !(dooct && hex[0] == 0))
                while (0 < length[0]--)
                    (ptr = ptr.plus(1)).be(-1, (byte)'0');
            ptr.be(0, NUL);
            STRCAT(buf1, buf2);
            ins_str(buf1);          /* insert the new number */
        }

        --curwin.w_cursor.col;
        curwin.w_set_curswant = true;
        ptr = ml_get_buf(curbuf, curwin.w_cursor.lnum, true);
        if (curwin.w_onebuf_opt.wo_rl[0])
            reverse_line(ptr);
        return true;
    }

    /*
     * SELECTION / PRIMARY ('*')
     *
     * Text selection stuff that uses the GUI selection register '*'.  When using a
     * GUI this may be text from another window, otherwise it is the last text we
     * had highlighted with VIsual mode.  With mouse support, clicking the middle
     * button performs the paste, otherwise you will need to do <"*p>. "
     * If not under X, it is synonymous with the clipboard register '+'.
     *
     * X CLIPBOARD ('+')
     *
     * Text selection stuff that uses the GUI clipboard register '+'.
     * Under X, this matches the standard cut/paste buffer CLIPBOARD selection.
     * It will be used for unnamed cut/pasting is 'clipboard' contains "unnamed",
     * otherwise you will need to do <"+p>. "
     * If not under X, it is synonymous with the selection register '*'.
     */

    /*
     * Routine to export any final X selection we had to the environment
     * so that the text is still available after vim has exited.  X selections
     * only exist while the owning application exists, so we write to the
     * permanent (while X runs) store CUT_BUFFER0.
     * Dump the CLIPBOARD selection if we own it (it's logically the more
     * 'permanent' of the two), otherwise the PRIMARY one.
     * For now, use a hard-coded sanity limit of 1Mb of data.
     */

    /*private*/ static void clip_free_selection(clipboard_C cbd)
    {
        yankreg_C y_ptr = y_current;

        if (cbd == clip_plus)
            y_current = y_regs[PLUS_REGISTER];
        else
            y_current = y_regs[STAR_REGISTER];

        y_current.y_array = null;
        y_current.y_size = 0;

        y_current = y_ptr;
    }

    /*
     * Get the selected text and put it in the gui selection register '*' or '+'.
     */
    /*private*/ static void clip_get_selection(clipboard_C cbd)
    {
        if (cbd.owned)
        {
            if ((cbd == clip_plus && y_regs[PLUS_REGISTER].y_array != null)
             || (cbd == clip_star && y_regs[STAR_REGISTER].y_array != null))
                return;

            /* Get the text between clip_star.cbd_start & clip_star.cbd_end. */
            yankreg_C old_y_previous = y_previous;
            yankreg_C old_y_current = y_current;
            pos_C old_cursor = new pos_C();
            COPY_pos(old_cursor, curwin.w_cursor);

            int old_curswant = curwin.w_curswant;
            boolean old_set_curswant = curwin.w_set_curswant;
            pos_C old_op_start = new pos_C();
            COPY_pos(old_op_start, curbuf.b_op_start);
            pos_C old_op_end = new pos_C();
            COPY_pos(old_op_end, curbuf.b_op_end);
            pos_C old_visual = new pos_C();
            COPY_pos(old_visual, VIsual);
            int old_visual_mode = VIsual_mode;

            oparg_C oa = new oparg_C();
            oa.regname = (cbd == clip_plus) ? '+' : '*';
            oa.op_type = OP_YANK;

            cmdarg_C ca = new cmdarg_C();
            ca.oap = oa;
            ca.cmdchar = 'y';
            ca.count1 = 1;
            ca.retval = CA_NO_ADJ_OP_END;

            do_pending_operator(ca, 0, true);

            y_previous = old_y_previous;
            y_current = old_y_current;
            COPY_pos(curwin.w_cursor, old_cursor);

            changed_cline_bef_curs();               /* need to update w_virtcol et al */

            curwin.w_curswant = old_curswant;
            curwin.w_set_curswant = old_set_curswant;
            COPY_pos(curbuf.b_op_start, old_op_start);
            COPY_pos(curbuf.b_op_end, old_op_end);
            COPY_pos(VIsual, old_visual);
            VIsual_mode = old_visual_mode;
        }
        else
        {
            clip_free_selection(cbd);

            /* Try to get selected text from another window. */
            clip_gen_request_selection(cbd);
        }
    }

    /*
     * Convert from the GUI selection string into the '*'/'+' register.
     */
    /*private*/ static void clip_yank_selection(byte type, Bytes str, int len, clipboard_C cbd)
    {
        yankreg_C y_ptr;

        if (cbd == clip_plus)
            y_ptr = y_regs[PLUS_REGISTER];
        else
            y_ptr = y_regs[STAR_REGISTER];

        clip_free_selection(cbd);

        str_to_reg(y_ptr, type, str, len, 0, false);
    }

    /*
     * If we have written to a clipboard register, send the text to the clipboard.
     */
    /*private*/ static void may_set_selection()
    {
        if (y_current == y_regs[STAR_REGISTER] && clip_star.available)
        {
            clip_own_selection(clip_star);
            clip_gen_set_selection(clip_star);
        }
        else if (y_current == y_regs[PLUS_REGISTER] && clip_plus.available)
        {
            clip_own_selection(clip_plus);
            clip_gen_set_selection(clip_plus);
        }
    }

    /*
     * Replace the contents of the '~' register with str.
     */
    /*private*/ static void dnd_yank_drag_data(Bytes str, int len)
    {
        yankreg_C curr = y_current;
        y_current = y_regs[TILDE_REGISTER];
        y_current.y_array = null;
        str_to_reg(y_current, MCHAR, str, len, 0, false);
        y_current = curr;
    }

    /*
     * Return the type of a register.
     * Used for getregtype()
     * Returns MAUTO for error.
     */
    /*private*/ static byte get_reg_type(int regname, long[] reglen)
    {
        switch (regname)
        {
            case '%':               /* file name */
            case '#':               /* alternate file name */
            case '=':               /* expression */
            case ':':               /* last command line */
            case '/':               /* last search-pattern */
            case '.':               /* last inserted text */
            case Ctrl_W:            /* word under cursor */
            case Ctrl_A:            /* WORD (mnemonic All) under cursor */
            case '_':               /* black hole: always empty */
                return MCHAR;
        }

        regname = may_get_selection(regname);

        if (regname != NUL && !valid_yank_reg(regname, false))
            return MAUTO;

        get_yank_register(regname, false);

        if (y_current.y_array != null)
        {
            if (reglen != null && y_current.y_type == MBLOCK)
                reglen[0] = y_current.y_width;
            return y_current.y_type;
        }

        return MAUTO;
    }

    /*
     * When "flags" has GREG_LIST return a list with text "s".
     * Otherwise just return "s".
     */
    /*private*/ static /*Bytes|list_C*/Object getreg_wrap_one_line(Bytes s, int flags)
    {
        if ((flags & GREG_LIST) != 0)
        {
            list_C list = new list_C();

            if (list_append_string(list, null, -1) == false)
            {
                list_free(list, true);
                return null;
            }
            list.lv_first.li_tv.tv_string = s;

            return list;
        }

        return s;
    }

    /*
     * Return the contents of a register as a single allocated string.
     * Used for "@r" in expressions and for getreg().
     * Returns null for error.
     * Flags:
     *      GREG_NO_EXPR    Do not allow expression register
     *      GREG_EXPR_SRC   For the expression register: return expression itself,
     *                      not the result of its evaluation.
     *      GREG_LIST       Return a list of lines in place of a single string.
     */
    /*private*/ static /*Bytes|list_C*/Object get_reg_contents(int regname, int flags)
    {
        /* Don't allow using an expression register inside an expression. */
        if (regname == '=')
        {
            if ((flags & GREG_NO_EXPR) != 0)
                return null;
            if ((flags & GREG_EXPR_SRC) != 0)
                return getreg_wrap_one_line(get_expr_line_src(), flags);

            return getreg_wrap_one_line(get_expr_line(), flags);
        }

        if (regname == '@')     /* "@@" is used for unnamed register */
            regname = '"';

        /* check for valid regname */
        if (regname != NUL && !valid_yank_reg(regname, false))
            return null;

        regname = may_get_selection(regname);

        Bytes[] ret = new Bytes[1];
        boolean[] allocated = new boolean[1];
        if (get_spec_reg(regname, ret, allocated, false))
        {
            if (ret[0] == null)
                return null;

            return getreg_wrap_one_line(allocated[0] ? ret[0] : STRDUP(ret[0]), flags);
        }

        get_yank_register(regname, false);
        if (y_current.y_array == null)
            return null;

        if ((flags & GREG_LIST) != 0)
        {
            list_C list = new list_C();

            boolean error = false;
            for (int i = 0; i < y_current.y_size; i++)
                if (list_append_string(list, y_current.y_array[i], -1) == false)
                    error = true;
            if (error)
            {
                list_free(list, true);
                return null;
            }

            return list;
        }

        /*
         * Compute length of resulting string.
         */
        int len = 0;
        for (int i = 0; i < y_current.y_size; i++)
        {
            len += strlen(y_current.y_array[i]);
            /*
             * Insert a newline between lines and after last line if y_type is MLINE.
             */
            if (y_current.y_type == MLINE || i < y_current.y_size - 1)
                len++;
        }

        Bytes retval = new Bytes(len + 1);

        /*
         * Copy the lines of the yank register into the string.
         */
        len = 0;
        for (int i = 0; i < y_current.y_size; i++)
        {
            STRCPY(retval.plus(len), y_current.y_array[i]);
            len += strlen(retval, len);

            /*
                * Insert a NL between lines and after the last line if y_type is MLINE.
                */
            if (y_current.y_type == MLINE || i < y_current.y_size - 1)
                retval.be(len++, (byte)'\n');
        }
        retval.be(len, NUL);

        return retval;
    }

    /*private*/ static boolean init_write_reg(int name, yankreg_C[] old_y_previous, yankreg_C[] old_y_current, boolean must_append, byte[] _yank_type)
    {
        if (!valid_yank_reg(name, true))        /* check for valid reg name */
        {
            emsg_invreg(name);
            return false;
        }

        /* Don't want to change the current (unnamed) register. */
        old_y_previous[0] = y_previous;
        old_y_current[0] = y_current;

        get_yank_register(name, true);
        if (!y_append && !must_append)
            y_current.y_array = null;

        return true;
    }

    /*private*/ static void finish_write_reg(int name, yankreg_C old_y_previous, yankreg_C old_y_current)
    {
        /* Send text of clipboard register to the clipboard. */
        may_set_selection();

        /* ':let @" = "val"' should change the meaning of the "" register */
        if (name != '"')
            y_previous = old_y_previous;
        y_current = old_y_current;
    }

    /*
     * Store string "str" in register "name".
     * "maxlen" is the maximum number of bytes to use, -1 for all bytes.
     * If "must_append" is true, always append to the register.
     * Otherwise append if "name" is an uppercase letter.
     * Note: "maxlen" and "must_append" don't work for the "/" register.
     * Careful: 'str' is modified, you may have to use a copy!
     * If "str" ends in '\n' or '\r', use linewise, otherwise use characterwise.
     */
    /*private*/ static void write_reg_contents(int name, Bytes str, int maxlen, boolean must_append)
    {
        write_reg_contents_ex(name, str, maxlen, must_append, MAUTO, 0);
    }

    /*private*/ static void write_reg_contents_lst(int name, Bytes[] strings, int _maxlen, boolean must_append, byte _yank_type, int block_len)
    {
        byte[] yank_type = { _yank_type };

        if (name == '/' || name == '=')
        {
            Bytes s;

            if (strings[0] == null)
                s = u8("");
            else if (strings[1] != null)
            {
                emsg(u8("E883: search pattern and expression register may not contain two or more lines"));
                return;
            }
            else
                s = strings[0];
            write_reg_contents_ex(name, s, -1, must_append, yank_type[0], block_len);
            return;
        }

        if (name == '_')        /* black hole: nothing to do */
            return;

        yankreg_C[] old_y_previous = new yankreg_C[1];
        yankreg_C[] old_y_current = new yankreg_C[1];

        if (init_write_reg(name, old_y_previous, old_y_current, must_append, yank_type) == false)
            return;

        str_to_reg(y_current, yank_type[0], strings, -1, block_len, true);

        finish_write_reg(name, old_y_previous[0], old_y_current[0]);
    }

    /*private*/ static void write_reg_contents_ex(int name, Bytes str, int maxlen, boolean must_append, byte _yank_type, int block_len)
    {
        byte[] yank_type = { _yank_type };

        int len = (maxlen < 0) ? strlen(str) : maxlen;

        /* Special case: '/' search pattern. */
        if (name == '/')
        {
            set_last_search_pat(str, RE_SEARCH, true, true);
            return;
        }

        if (name == '#')
        {
            buffer_C buf;

            if (asc_isdigit(str.at(0)))
            {
                int num = libC.atoi(str);

                buf = buflist_findnr(num);
                if (buf == null)
                    emsgn(e_nobufnr, (long)num);
            }
            else
                buf = buflist_findnr(buflist_findpat(str, str.plus(strlen(str)), true, false));
            if (buf == null)
                return;

            curwin.w_alt_fnum = buf.b_fnum;
            return;
        }

        if (name == '=')
        {
            Bytes p = STRNDUP(str, len);
            if (must_append)
                p = concat_str(get_expr_line_src(), p);
            set_expr_line(p);
            return;
        }

        if (name == '_')        /* black hole: nothing to do */
            return;

        yankreg_C[] old_y_previous = new yankreg_C[1];
        yankreg_C[] old_y_current = new yankreg_C[1];

        if (init_write_reg(name, old_y_previous, old_y_current, must_append, yank_type) == false)
            return;

        str_to_reg(y_current, yank_type[0], str, len, block_len, false);

        finish_write_reg(name, old_y_previous[0], old_y_current[0]);
    }

    /*
     * Put a string into a register.  When the register is not empty, the string is appended.
     */
    /*private*/ static void str_to_reg(yankreg_C y_ptr, byte yank_type, Object str, int len, int blocklen, boolean str_list)
        /* y_ptr: pointer to yank register */
        /* yank_type: MCHAR, MLINE, MBLOCK, MAUTO */
        /* str: string to put in register */
        /* len: length of string */
        /* blocklen: width of Visual block */
        /* str_list: true if str is Bytes[] */
    {
        int extraline = 0;                      /* extra line at the end */
        boolean append = false;                 /* append to last line in register */

        if (y_ptr.y_array == null)              /* null means empty register */
            y_ptr.y_size = 0;

        byte type;                               /* MCHAR, MLINE or MBLOCK */
        if (yank_type == MAUTO)
            type = (str_list || (0 < len && (((Bytes)str).at(len - 1) == NL || ((Bytes)str).at(len - 1) == CAR))) ? MLINE : MCHAR;
        else
            type = yank_type;

        /*
         * Count the number of lines within the string.
         */
        int newlines = 0;                       /* number of lines added */
        if (str_list)
        {
            Bytes[] pp = (Bytes[])str;

            for (int i = 0; pp[i] != null; i++)
                newlines++;
        }
        else
        {
            Bytes p = (Bytes)str;

            for (int i = 0; i < len; i++)
                if (p.at(i) == (byte)'\n')
                    newlines++;
            if (type == MCHAR || len == 0 || p.at(len - 1) != (byte)'\n')
            {
                extraline = 1;
                newlines++;                     /* count extra newline at the end */
            }
            if (0 < y_ptr.y_size && y_ptr.y_type == MCHAR)
            {
                append = true;
                --newlines;                     /* uncount newline when appending first line */
            }
        }

        /*
         * Allocate an array to hold the pointers to the new register lines.
         * If the register was not empty, move the existing lines to the new array.
         */
        Bytes[] pp = new Bytes[y_ptr.y_size + newlines];

        int lnum;
        for (lnum = 0; lnum < y_ptr.y_size; lnum++)
            pp[lnum] = y_ptr.y_array[lnum];
        y_ptr.y_array = pp;
        int maxlen = 0;

        /*
         * Find the end of each line and save it into the array.
         */
        if (str_list)
        {
            Bytes[] qq = (Bytes[])str;

            for (int i = 0; qq[i] != null; i++, lnum++)
            {
                int n = strlen(qq[i]);
                pp[lnum] = STRNDUP(qq[i], n);
                if (maxlen < n)
                    maxlen = n;
            }
        }
        else
        {
            Bytes p = (Bytes)str;

            for (int start = 0, i; start < len + extraline; start += i + 1)
            {
                for (i = start; i < len; i++)   /* find the end of the line */
                    if (p.at(i) == (byte)'\n')
                        break;
                i -= start;                     /* i is now length of line */
                if (maxlen < i)
                    maxlen = i;
                int extra = 0;
                if (append)
                {
                    --lnum;
                    extra = strlen(y_ptr.y_array[lnum]);
                }

                Bytes s = new Bytes(i + extra + 1);

                if (extra != 0)
                    BCOPY(s, y_ptr.y_array[lnum], extra);
                if (append)
                    y_ptr.y_array[lnum] = null;
                if (i != 0)
                    BCOPY(s, extra, p, start, i);
                extra += i;
                s.be(extra, NUL);
                y_ptr.y_array[lnum++] = s;
                while (0 <= --extra)
                {
                    if (s.at(0) == NUL)
                        s.be(0, (byte)'\n');              /* replace NUL with newline */
                    s = s.plus(1);
                }
                append = false;                 /* only first line is appended */
            }
        }
        y_ptr.y_type = type;
        y_ptr.y_size = lnum;
        if (type == MBLOCK)
            y_ptr.y_width = (blocklen < 0) ? maxlen - 1 : blocklen;
        else
            y_ptr.y_width = 0;
    }

    /*
     *  Count the number of bytes, characters and "words" in a line.
     *
     *  "Words" are counted by looking for boundaries between non-space and
     *  space characters.  (it seems to produce results that match 'wc'.)
     *
     *  Return value is byte count; word count for the line is added to "*wc".
     *  Char count is added to "*cc".
     *
     *  The function will only examine the first "limit" characters in the
     *  line, stopping if it encounters an end-of-line (NUL byte).  In that
     *  case, eol_size will be added to the character count to account for
     *  the size of the EOL character.
     */
    /*private*/ static int line_count_info(Bytes line, int[] wc, int[] cc, int limit, int eol_size)
    {
        int i;

        int words = 0;
        int chars = 0;
        boolean is_word = false;

        for (i = 0; i < limit && line.at(i) != NUL; )
        {
            if (is_word)
            {
                if (vim_isspace(line.at(i)))
                {
                    words++;
                    is_word = false;
                }
            }
            else if (!vim_isspace(line.at(i)))
                is_word = true;
            chars++;
            i += us_ptr2len_cc(line.plus(i));
        }

        if (is_word)
            words++;
        wc[0] += words;

        /* Add eol_size if the end of line was reached before hitting limit. */
        if (i < limit && line.at(i) == NUL)
        {
            i += eol_size;
            chars += eol_size;
        }
        cc[0] += chars;

        return i;
    }

    /*
     * Give some info about the position of the cursor (for "g CTRL-G").
     * In Visual mode, give some info about the selected region.  (In this case,
     * the *_count_cursor variables store running totals for the selection.)
     */
    /*private*/ static void cursor_pos_info()
    {
        /*
         * Compute the length of the file in characters.
         */
        if ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0)
        {
            msg(no_lines_msg);
            return;
        }

        Bytes buf1 = new Bytes(50);
        Bytes buf2 = new Bytes(40);
        int byte_count = 0;
        int byte_count_cursor = 0;
        int[] char_count = { 0 };
        int[] char_count_cursor = { 0 };
        int[] word_count = { 0 };
        int[] word_count_cursor = { 0 };
        int last_check = 100000;
        long line_count_selected = 0;

        pos_C min_pos = new pos_C();
        pos_C max_pos = new pos_C();
        oparg_C oparg = new oparg_C();

        int eol_size = (get_fileformat(curbuf) == EOL_DOS) ? 2 : 1;

        if (VIsual_active)
        {
            if (ltpos(VIsual, curwin.w_cursor))
            {
                COPY_pos(min_pos, VIsual);
                COPY_pos(max_pos, curwin.w_cursor);
            }
            else
            {
                COPY_pos(min_pos, curwin.w_cursor);
                COPY_pos(max_pos, VIsual);
            }
            if (p_sel[0].at(0) == (byte)'e' && 0 < max_pos.col)
                --max_pos.col;

            if (VIsual_mode == Ctrl_V)
            {
                Bytes saved_sbr = p_sbr[0];

                /* Make 'sbr' empty for a moment to get the correct size. */
                p_sbr[0] = EMPTY_OPTION;
                oparg.is_VIsual = true;
                oparg.block_mode = true;
                oparg.op_type = OP_NOP;
                {
                    int[] scol = { oparg.start_vcol };
                    int[] ecol = { oparg.end_vcol };
                    getvcols(curwin, min_pos, max_pos, scol, ecol);
                    oparg.start_vcol = scol[0];
                    oparg.end_vcol = ecol[0];
                }
                p_sbr[0] = saved_sbr;
                if (curwin.w_curswant == MAXCOL)
                    oparg.end_vcol = MAXCOL;
                /* Swap the start, end vcol if needed. */
                if (oparg.end_vcol < oparg.start_vcol)
                {
                    oparg.end_vcol += oparg.start_vcol;
                    oparg.start_vcol = oparg.end_vcol - oparg.start_vcol;
                    oparg.end_vcol -= oparg.start_vcol;
                }
            }

            line_count_selected = max_pos.lnum - min_pos.lnum + 1;
        }

        for (long lnum = 1; lnum <= curbuf.b_ml.ml_line_count; lnum++)
        {
            /* Check for a CTRL-C every 100000 characters. */
            if (last_check < byte_count)
            {
                ui_breakcheck();
                if (got_int)
                    return;
                last_check = byte_count + 100000;
            }

            /* Do extra processing for VIsual mode. */
            if (VIsual_active && min_pos.lnum <= lnum && lnum <= max_pos.lnum)
            {
                Bytes s = null;
                int len = 0;

                switch (VIsual_mode)
                {
                    case Ctrl_V:
                    {
                        virtual_op = virtual_active() ? TRUE : FALSE;
                        block_def_C bd = new block_def_C();
                        block_prep(oparg, bd, lnum, false);
                        virtual_op = MAYBE;
                        s = bd.textstart;
                        len = bd.textlen;
                        break;
                    }

                    case 'V':
                    {
                        s = ml_get(lnum);
                        len = MAXCOL;
                        break;
                    }

                    case 'v':
                    {
                        int start_col = (lnum == min_pos.lnum) ? min_pos.col : 0;
                        int end_col = (lnum == max_pos.lnum) ? max_pos.col - start_col + 1 : MAXCOL;

                        s = ml_get(lnum).plus(start_col);
                        len = end_col;
                        break;
                    }
                }

                if (s != null)
                {
                    byte_count_cursor += line_count_info(s, word_count_cursor, char_count_cursor, len, eol_size);
                    if (lnum == curbuf.b_ml.ml_line_count
                            && !curbuf.b_p_eol[0]
                            && curbuf.b_p_bin[0]
                            && strlen(s) < len)
                        byte_count_cursor -= eol_size;
                }
            }
            else
            {
                /* In non-visual mode, check for the line the cursor is on. */
                if (lnum == curwin.w_cursor.lnum)
                {
                    word_count_cursor[0] += word_count[0];
                    char_count_cursor[0] += char_count[0];
                    byte_count_cursor = byte_count +
                        line_count_info(ml_get(lnum), word_count_cursor, char_count_cursor,
                                    curwin.w_cursor.col + 1, eol_size);
                }
            }

            /* Add to the running totals. */
            byte_count += line_count_info(ml_get(lnum), word_count, char_count, MAXCOL, eol_size);
        }

        /* Correction for when last line doesn't have an EOL. */
        if (!curbuf.b_p_eol[0] && curbuf.b_p_bin[0])
            byte_count -= eol_size;

        if (VIsual_active)
        {
            if (VIsual_mode == Ctrl_V && curwin.w_curswant < MAXCOL)
            {
                {
                    int[] _1 = { min_pos.col };
                    int[] _2 = { max_pos.col };
                    getvcols(curwin, min_pos, max_pos, _1, _2);
                    min_pos.col = _1[0];
                    max_pos.col = _2[0];
                }
                vim_snprintf(buf1, buf1.size(), u8("%ld Cols; "), oparg.end_vcol - oparg.start_vcol + 1);
            }
            else
                buf1.be(0, NUL);

            if (char_count_cursor[0] == byte_count_cursor && char_count[0] == byte_count)
                vim_snprintf(ioBuff, IOSIZE,
                        u8("Selected %s%ld of %ld Lines; %ld of %ld Words; %ld of %ld Bytes"),
                        buf1, line_count_selected, curbuf.b_ml.ml_line_count,
                        word_count_cursor[0], word_count[0],
                        byte_count_cursor, byte_count);
            else
                vim_snprintf(ioBuff, IOSIZE,
                        u8("Selected %s%ld of %ld Lines; %ld of %ld Words; %ld of %ld Chars; %ld of %ld Bytes"),
                        buf1, line_count_selected, curbuf.b_ml.ml_line_count,
                        word_count_cursor[0], word_count[0],
                        char_count_cursor[0], char_count[0],
                        byte_count_cursor, byte_count);
        }
        else
        {
            Bytes p = ml_get_curline();
            validate_virtcol();
            col_print(buf1, buf1.size(), curwin.w_cursor.col + 1, curwin.w_virtcol + 1);
            col_print(buf2, buf2.size(), strlen(p), linetabsize(p));

            if (char_count_cursor[0] == byte_count_cursor && char_count[0] == byte_count)
                vim_snprintf(ioBuff, IOSIZE,
                    u8("Col %s of %s; Line %ld of %ld; Word %ld of %ld; Byte %ld of %ld"),
                    buf1, buf2,
                    curwin.w_cursor.lnum, curbuf.b_ml.ml_line_count,
                    word_count_cursor[0], word_count[0],
                    byte_count_cursor, byte_count);
            else
                vim_snprintf(ioBuff, IOSIZE,
                    u8("Col %s of %s; Line %ld of %ld; Word %ld of %ld; Char %ld of %ld; Byte %ld of %ld"),
                    buf1, buf2,
                    curwin.w_cursor.lnum, curbuf.b_ml.ml_line_count,
                    word_count_cursor[0], word_count[0],
                    char_count_cursor[0], char_count[0],
                    byte_count_cursor, byte_count);
        }

        long n = bomb_size();
        if (0 < n)
            libC.sprintf(ioBuff.plus(strlen(ioBuff)), u8("(+%ld for BOM)"), n);

        /* Don't shorten this message, the user asked for it. */
        Bytes p = p_shm[0];
        p_shm[0] = u8("");
        msg(ioBuff);
        p_shm[0] = p;
    }

    /*
     * mark.c: functions for setting marks and jumping to them ----------------------------------------
     */

    /*
     * If a named file mark's lnum is non-zero, it is valid.
     * If a named file mark's fnum is non-zero, it is for an existing buffer,
     * otherwise it is from .viminfo and namedfm[n].fname is the file name.
     * There are marks 'A - 'Z (set by user) and '0 to '9 (set when writing viminfo).
     */
    /*private*/ static final int EXTRA_MARKS = 10;                                                  /* marks 0-9 */
    /*private*/ static xfmark_C[] namedfm = ARRAY_xfmark(NMARKS + EXTRA_MARKS); /* marks with file nr */

    /*
     * Set named mark "c" at current cursor position.
     * Returns true on success, false if bad name given.
     */
    /*private*/ static boolean setmark(int c)
    {
        return setmark_pos(c, curwin.w_cursor, curbuf.b_fnum);
    }

    /*
     * Set named mark "c" to position "pos".
     * When "c" is upper case use file "fnum".
     * Returns true on success, false if bad name given.
     */
    /*private*/ static boolean setmark_pos(int c, pos_C pos, int fnum)
    {
        /* Check for a special key (may cause islower() to crash). */
        if (c < 0)
            return false;

        if (c == '\'' || c == '`')
        {
            if (pos == curwin.w_cursor)
            {
                setpcmark();
                /* keep it even when the cursor doesn't move */
                COPY_pos(curwin.w_prev_pcmark, curwin.w_pcmark);
            }
            else
                COPY_pos(curwin.w_pcmark, pos);
            return true;
        }

        if (c == '"')
        {
            COPY_pos(curbuf.b_last_cursor, pos);
            return true;
        }

        /* Allow setting '[ and '] for an autocommand that simulates reading a file. */
        if (c == '[')
        {
            COPY_pos(curbuf.b_op_start, pos);
            return true;
        }
        if (c == ']')
        {
            COPY_pos(curbuf.b_op_end, pos);
            return true;
        }

        if (c == '<' || c == '>')
        {
            if (c == '<')
                COPY_pos(curbuf.b_visual.vi_start, pos);
            else
                COPY_pos(curbuf.b_visual.vi_end, pos);
            if (curbuf.b_visual.vi_mode == NUL)
                /* Visual_mode has not yet been set, use a sane default. */
                curbuf.b_visual.vi_mode = 'v';
            return true;
        }

        if ('z' < c)        /* some islower() and isupper() cannot handle characters above 127 */
            return false;
        if (asc_islower(c))
        {
            int i = c - 'a';
            COPY_pos(curbuf.b_namedm[i], pos);
            return true;
        }
        if (asc_isupper(c))
        {
            int i = c - 'A';
            COPY_pos(namedfm[i].fmark.mark, pos);
            namedfm[i].fmark.fnum = fnum;
            namedfm[i].fname = null;
            return true;
        }

        return false;
    }

    /*
     * Set the previous context mark to the current position and add it to the jump list.
     */
    /*private*/ static void setpcmark()
    {
        /* for :global the mark is set only once */
        if (global_busy != 0 || listcmd_busy || cmdmod.keepjumps)
            return;

        COPY_pos(curwin.w_prev_pcmark, curwin.w_pcmark);
        COPY_pos(curwin.w_pcmark, curwin.w_cursor);

        /* If jumplist is full: remove oldest entry. */
        if (++curwin.w_jumplistlen > JUMPLISTSIZE)
        {
            curwin.w_jumplistlen = JUMPLISTSIZE;
            curwin.w_jumplist[0].fname = null;
            for (int i = 1; i < JUMPLISTSIZE; i++)
                COPY_xfmark(curwin.w_jumplist[i - 1], curwin.w_jumplist[i]);
        }
        curwin.w_jumplistidx = curwin.w_jumplistlen;
        xfmark_C fm = curwin.w_jumplist[curwin.w_jumplistlen - 1];

        COPY_pos(fm.fmark.mark, curwin.w_pcmark);
        fm.fmark.fnum = curbuf.b_fnum;
        fm.fname = null;
    }

    /*
     * To change context, call setpcmark(), then move the current position to
     * where ever, then call checkpcmark().  This ensures that the previous
     * context will only be changed if the cursor moved to a different line.
     * If pcmark was deleted (with "dG") the previous mark is restored.
     */
    /*private*/ static void checkpcmark()
    {
        if (curwin.w_prev_pcmark.lnum != 0
                && (eqpos(curwin.w_pcmark, curwin.w_cursor) || curwin.w_pcmark.lnum == 0))
        {
            COPY_pos(curwin.w_pcmark, curwin.w_prev_pcmark);
            curwin.w_prev_pcmark.lnum = 0;      /* show it has been checked */
        }
    }

    /*
     * move "count" positions in the jump list (count may be negative)
     */
    /*private*/ static pos_C movemark(int count)
    {
        cleanup_jumplist();

        if (curwin.w_jumplistlen == 0)          /* nothing to jump to */
            return null;

        for ( ; ; )
        {
            if (curwin.w_jumplistidx + count < 0 || curwin.w_jumplistlen <= curwin.w_jumplistidx + count)
                return null;

            /*
             * If first CTRL-O or CTRL-I command after a jump, add cursor position
             * to list.  Careful: If there are duplicates (CTRL-O immediately after
             * starting Vim on a file), another entry may have been removed.
             */
            if (curwin.w_jumplistidx == curwin.w_jumplistlen)
            {
                setpcmark();
                --curwin.w_jumplistidx;         /* skip the new entry */
                if (curwin.w_jumplistidx + count < 0)
                    return null;
            }

            curwin.w_jumplistidx += count;

            pos_C pos;

            xfmark_C jmp = curwin.w_jumplist[curwin.w_jumplistidx];
            if (jmp.fmark.fnum == 0)
                fname2fnum(jmp);
            if (jmp.fmark.fnum != curbuf.b_fnum)
            {
                /* jump to other file */
                if (buflist_findnr(jmp.fmark.fnum) == null)
                {                                               /* skip this one .. */
                    count += count < 0 ? -1 : 1;
                    continue;
                }
                if (buflist_getfile(jmp.fmark.fnum, jmp.fmark.mark.lnum, 0, false) == false)
                    return null;
                /* set lnum again, autocommands my have changed it */
                COPY_pos(curwin.w_cursor, jmp.fmark.mark);
                pos = NOPOS;
            }
            else
                pos = jmp.fmark.mark;

            return pos;
        }
    }

    /*
     * Move "count" positions in the changelist (count may be negative).
     */
    /*private*/ static pos_C movechangelist(int count)
    {
        if (curbuf.b_changelistlen == 0)    /* nothing to jump to */
            return null;

        int n = curwin.w_changelistidx;
        if (n + count < 0)
        {
            if (n == 0)
                return null;
            n = 0;
        }
        else if (curbuf.b_changelistlen <= n + count)
        {
            if (n == curbuf.b_changelistlen - 1)
                return null;
            n = curbuf.b_changelistlen - 1;
        }
        else
            n += count;
        curwin.w_changelistidx = n;
        return curbuf.b_changelist[n];
    }

    /*
     * Find mark "c" in buffer pointed to by "buf".
     * If "changefile" is true it's allowed to edit another file for '0, 'A, etc.
     * If "fnum" is not null store the fnum there for '0, 'A etc., don't edit another file.
     * Returns:
     * - pointer to pos_C if found.  lnum is 0 when mark not set, -1 when mark is
     *   in another file which can't be gotten. (caller needs to check lnum!)
     * - null if there is no mark called 'c'.
     * - -1 if mark is in other file and jumped there (only if changefile is true)
     */
    /*private*/ static pos_C getmark_buf(buffer_C buf, int c, boolean changefile)
    {
        return getmark_buf_fnum(buf, c, changefile, null);
    }

    /*private*/ static pos_C getmark(int c, boolean changefile)
    {
        return getmark_buf_fnum(curbuf, c, changefile, null);
    }

    /*private*/ static pos_C _1_pos_copy = new pos_C();

    /*private*/ static pos_C getmark_buf_fnum(buffer_C buf, int c, boolean changefile, int[] fnum)
    {
        pos_C posp = null;

        /* Check for special key, can't be a mark name and might cause islower() to crash. */
        if (c < 0)
            return posp;
        if ('~' < c)                                /* check for islower()/isupper() */
            ;
        else if (c == '\'' || c == '`')             /* previous context mark */
        {
            COPY_pos(_1_pos_copy, curwin.w_pcmark); /* need to make a copy because */
            posp = _1_pos_copy;                     /* w_pcmark may be changed soon */
        }
        else if (c == '"')                          /* to pos when leaving buffer */
            posp = buf.b_last_cursor;
        else if (c == '^')                          /* to where Insert mode stopped */
            posp = buf.b_last_insert;
        else if (c == '.')                          /* to where last change was made */
            posp = buf.b_last_change;
        else if (c == '[')                          /* to start of previous operator */
            posp = buf.b_op_start;
        else if (c == ']')                          /* to end of previous operator */
            posp = buf.b_op_end;
        else if (c == '{' || c == '}')              /* to previous/next paragraph */
        {
            boolean slcb = listcmd_busy;

            pos_C pos = new pos_C();
            COPY_pos(pos, curwin.w_cursor);
            listcmd_busy = true;                    /* avoid that '' is changed */
            oparg_C oa = new oparg_C();
            boolean b;
            { boolean[] __ = { oa.inclusive }; b = findpar(__, (c == '}') ? FORWARD : BACKWARD, 1L, NUL, false); oa.inclusive = __[0]; }
            if (b)
            {
                COPY_pos(_1_pos_copy, curwin.w_cursor);
                posp = _1_pos_copy;
            }
            COPY_pos(curwin.w_cursor, pos);
            listcmd_busy = slcb;
        }
        else if (c == '(' || c == ')')              /* to previous/next sentence */
        {
            boolean slcb = listcmd_busy;

            pos_C pos = new pos_C();
            COPY_pos(pos, curwin.w_cursor);
            listcmd_busy = true;                    /* avoid that '' is changed */
            if (findsent(c == ')' ? FORWARD : BACKWARD, 1L))
            {
                COPY_pos(_1_pos_copy, curwin.w_cursor);
                posp = _1_pos_copy;
            }
            COPY_pos(curwin.w_cursor, pos);
            listcmd_busy = slcb;
        }
        else if (c == '<' || c == '>')              /* start/end of visual area */
        {
            pos_C startp = buf.b_visual.vi_start;
            pos_C endp = buf.b_visual.vi_end;
            if ((c == '<') == ltpos(startp, endp))
                posp = startp;
            else
                posp = endp;
            /*
             * For Visual line mode, set mark at begin or end of line
             */
            if (buf.b_visual.vi_mode == 'V')
            {
                COPY_pos(_1_pos_copy, posp);
                posp = _1_pos_copy;
                if (c == '<')
                    _1_pos_copy.col = 0;
                else
                    _1_pos_copy.col = MAXCOL;
                _1_pos_copy.coladd = 0;
            }
        }
        else if (asc_islower(c))                    /* normal named mark */
        {
            posp = buf.b_namedm[c - 'a'];
        }
        else if (asc_isupper(c) || asc_isdigit(c))  /* named file mark */
        {
            if (asc_isdigit(c))
                c = c - '0' + NMARKS;
            else
                c -= 'A';
            posp = namedfm[c].fmark.mark;

            if (namedfm[c].fmark.fnum == 0)
                fname2fnum(namedfm[c]);

            if (fnum != null)
                fnum[0] = namedfm[c].fmark.fnum;
            else if (namedfm[c].fmark.fnum != buf.b_fnum)
            {
                /* mark is in another file */
                posp = _1_pos_copy;

                if (namedfm[c].fmark.mark.lnum != 0 && changefile && namedfm[c].fmark.fnum != 0)
                {
                    if (buflist_getfile(namedfm[c].fmark.fnum, 1, GETF_SETMARK, false) == true)
                    {
                        /* Set the lnum now, autocommands could have changed it. */
                        COPY_pos(curwin.w_cursor, namedfm[c].fmark.mark);
                        return NOPOS;
                    }
                    _1_pos_copy.lnum = -1; /* can't get file */
                }
                else
                    _1_pos_copy.lnum = 0;  /* mark exists, but is not valid in current buffer */
            }
        }

        return posp;
    }

    /*
     * Search for the next named mark in the current file.
     *
     * Returns pointer to pos_C of the next mark or null if no mark is found.
     */
    /*private*/ static pos_C getnextmark(pos_C startpos, int dir, boolean begin_line)
        /* startpos: where to start */
        /* dir: direction for search */
    {
        pos_C result = null;
        pos_C pos = new pos_C();
        COPY_pos(pos, startpos);

        /* When searching backward and leaving the cursor on the first non-blank,
         * position must be in a previous line.
         * When searching forward and leaving the cursor on the first non-blank,
         * position must be in a next line. */
        if (dir == BACKWARD && begin_line)
            pos.col = 0;
        else if (dir == FORWARD && begin_line)
            pos.col = MAXCOL;

        for (int i = 0; i < NMARKS; i++)
        {
            if (0 < curbuf.b_namedm[i].lnum)
            {
                if (dir == FORWARD)
                {
                    if ((result == null || ltpos(curbuf.b_namedm[i], result)) && ltpos(pos, curbuf.b_namedm[i]))
                        result = curbuf.b_namedm[i];
                }
                else
                {
                    if ((result == null || ltpos(result, curbuf.b_namedm[i])) && ltpos(curbuf.b_namedm[i], pos))
                        result = curbuf.b_namedm[i];
                }
            }
        }

        return result;
    }

    /*
     * For an xtended filemark: set the fnum from the fname.
     * This is used for marks obtained from the .viminfo file.
     * It's postponed until the mark is used to avoid a long startup delay.
     */
    /*private*/ static void fname2fnum(xfmark_C fm)
    {
        if (fm.fname != null)
        {
            vim_strncpy(nameBuff, fm.fname, MAXPATHL - 1);

            /* Try to shorten the file name. */
            mch_dirname(ioBuff, IOSIZE);
            Bytes p = shorten_fname(nameBuff, ioBuff);

            /* buflist_new() will call fmarks_check_names() */
            buflist_new(nameBuff, p, 1, 0);
        }
    }

    /*
     * Check all file marks for a name that matches the file name in buf.
     * May replace the name with an fnum.
     * Used for marks that come from the .viminfo file.
     */
    /*private*/ static void fmarks_check_names(buffer_C buf)
    {
        Bytes name = buf.b_ffname;
        if (name != null)
        {
            for (int i = 0; i < NMARKS + EXTRA_MARKS; i++)
                fmarks_check_one(namedfm[i], name, buf);

            for (window_C wp = firstwin; wp != null; wp = wp.w_next)
                for (int i = 0; i < wp.w_jumplistlen; i++)
                    fmarks_check_one(wp.w_jumplist[i], name, buf);
        }
    }

    /*private*/ static void fmarks_check_one(xfmark_C fm, Bytes name, buffer_C buf)
    {
        if (fm.fmark.fnum == 0 && fm.fname != null && STRCMP(name, fm.fname) == 0)
        {
            fm.fmark.fnum = buf.b_fnum;
            fm.fname = null;
        }
    }

    /*
     * Check a if a position from a mark is valid.
     * Give and error message and return false if not.
     */
    /*private*/ static boolean check_mark(pos_C pos)
    {
        if (pos == null)
        {
            emsg(e_umark);
            return false;
        }
        if (pos.lnum <= 0)
        {
            /* 'lnum' is negative if mark is in another file and can't get that file,
             * error message already give then. */
            if (pos.lnum == 0)
                emsg(e_marknotset);
            return false;
        }
        if (curbuf.b_ml.ml_line_count < pos.lnum)
        {
            emsg(e_markinval);
            return false;
        }
        return true;
    }

    /*private*/ static int mark_i = -1;

    /*
     * clrallmarks() - clear all marks in the buffer 'buf'
     *
     * Used mainly when trashing the entire buffer during ":e" type commands
     */
    /*private*/ static void clrallmarks(buffer_C buf)
    {
        if (mark_i == -1)                   /* first call ever: initialize */
            for (mark_i = 0; mark_i < NMARKS + 1; mark_i++)
            {
                namedfm[mark_i].fmark.mark.lnum = 0;
                namedfm[mark_i].fname = null;
            }

        for (mark_i = 0; mark_i < NMARKS; mark_i++)
            buf.b_namedm[mark_i].lnum = 0;
        buf.b_op_start.lnum = 0;            /* start/end op mark cleared */
        buf.b_op_end.lnum = 0;
        buf.b_last_cursor.lnum = 1;         /* '" mark cleared */
        buf.b_last_cursor.col = 0;
        buf.b_last_cursor.coladd = 0;
        buf.b_last_insert.lnum = 0;         /* '^ mark cleared */
        buf.b_last_change.lnum = 0;         /* '. mark cleared */
        buf.b_changelistlen = 0;
    }

    /*
     * Get name of file from a filemark.
     * When it's in the current buffer, return the text at the mark.
     * Returns an allocated string.
     */
    /*private*/ static Bytes fm_getname(fmark_C fmark, int lead_len)
    {
        if (fmark.fnum == curbuf.b_fnum)    /* current buffer */
            return mark_line(fmark.mark, lead_len);

        return buflist_nr2name(fmark.fnum, false);
    }

    /*
     * Return the line at mark "mp".  Truncate to fit in window.
     * The returned string has been allocated.
     */
    /*private*/ static Bytes mark_line(pos_C mp, int lead_len)
    {
        if (mp.lnum == 0 || curbuf.b_ml.ml_line_count < mp.lnum)
            return STRDUP(u8("-invalid-"));

        Bytes s = STRNDUP(skipwhite(ml_get(mp.lnum)), (int)Columns[0]);

        /* Truncate the line to fit it in the window. */
        int len = 0;
        Bytes p;
        for (p = s; p.at(0) != NUL; p = p.plus(us_ptr2len_cc(p)))
        {
            len += mb_ptr2cells(p);
            if ((int)Columns[0] - lead_len <= len)
                break;
        }
        p.be(0, NUL);

        return s;
    }

    /*
     * print the marks
     */
    /*private*/ static final ex_func_C ex_marks = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes arg = eap.arg;

            if (arg != null && arg.at(0) == NUL)
                arg = null;

            show_one_mark('\'', arg, curwin.w_pcmark, null, true);
            for (int i = 0; i < NMARKS; i++)
                show_one_mark(i + 'a', arg, curbuf.b_namedm[i], null, true);
            for (int i = 0; i < NMARKS + EXTRA_MARKS; i++)
            {
                Bytes name;
                if (namedfm[i].fmark.fnum != 0)
                    name = fm_getname(namedfm[i].fmark, 15);
                else
                    name = namedfm[i].fname;
                if (name != null)
                {
                    show_one_mark(NMARKS <= i ? i - NMARKS + '0' : i + 'A',
                            arg, namedfm[i].fmark.mark, name, namedfm[i].fmark.fnum == curbuf.b_fnum);
                }
            }
            show_one_mark('"', arg, curbuf.b_last_cursor, null, true);
            show_one_mark('[', arg, curbuf.b_op_start, null, true);
            show_one_mark(']', arg, curbuf.b_op_end, null, true);
            show_one_mark('^', arg, curbuf.b_last_insert, null, true);
            show_one_mark('.', arg, curbuf.b_last_change, null, true);
            show_one_mark('<', arg, curbuf.b_visual.vi_start, null, true);
            show_one_mark('>', arg, curbuf.b_visual.vi_end, null, true);
            show_one_mark(-1, arg, null, null, false);
        }
    };

    /*private*/ static boolean did_title;

    /*private*/ static void show_one_mark(int c, Bytes arg, pos_C p, Bytes name, boolean current)
        /* current: in current file */
    {
        if (c == -1)                            /* finish up */
        {
            if (did_title)
                did_title = false;
            else
            {
                if (arg == null)
                    msg(u8("No marks set"));
                else
                    emsg2(u8("E283: No marks matching \"%s\""), arg);
            }
        }
        /* don't output anything if 'q' typed at --more-- prompt */
        else if (!got_int && (arg == null || vim_strchr(arg, c) != null) && p.lnum != 0)
        {
            if (!did_title)
            {
                /* Highlight title. */
                msg_puts_title(u8("\nmark line  col file/text"));
                did_title = true;
            }
            msg_putchar('\n');
            if (!got_int)
            {
                libC.sprintf(ioBuff, u8(" %c %6ld %4d "), c, p.lnum, p.col);
                msg_outtrans(ioBuff);
                if (name == null && current)
                    name = mark_line(p, 15);
                if (name != null)
                    msg_outtrans_attr(name, current ? hl_attr(HLF_D) : 0);
            }
            out_flush();                /* show one line at a time */
        }
    }

    /*
     * ":delmarks[!] [marks]"
     */
    /*private*/ static final ex_func_C ex_delmarks = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.arg.at(0) == NUL && eap.forceit)
                /* clear all marks */
                clrallmarks(curbuf);
            else if (eap.forceit)
                emsg(e_invarg);
            else if (eap.arg.at(0) == NUL)
                emsg(e_argreq);
            else
            {
                /* clear specified marks only */
                for (Bytes p = eap.arg; p.at(0) != NUL; p = p.plus(1))
                {
                    boolean lower = asc_islower(p.at(0));
                    boolean digit = asc_isdigit(p.at(0));
                    if (lower || digit || asc_isupper(p.at(0)))
                    {
                        int from, to;
                        if (p.at(1) == (byte)'-')
                        {
                            /* clear range of marks */
                            from = p.at(0);
                            to = p.at(2);
                            if (!(lower ? asc_islower(p.at(2)) : (digit ? asc_isdigit(p.at(2)) : asc_isupper(p.at(2)))) || to < from)
                            {
                                emsg2(e_invarg2, p);
                                return;
                            }
                            p = p.plus(2);
                        }
                        else
                            /* clear one lower case mark */
                            from = to = p.at(0);

                        for (int i = from; i <= to; i++)
                        {
                            if (lower)
                                curbuf.b_namedm[i - 'a'].lnum = 0;
                            else
                            {
                                int n;
                                if (digit)
                                    n = i - '0' + NMARKS;
                                else
                                    n = i - 'A';

                                namedfm[n].fmark.mark.lnum = 0;
                                namedfm[n].fname = null;
                            }
                        }
                    }
                    else
                        switch (p.at(0))
                        {
                            case '"': curbuf.b_last_cursor.lnum = 0; break;
                            case '^': curbuf.b_last_insert.lnum = 0; break;
                            case '.': curbuf.b_last_change.lnum = 0; break;
                            case '[': curbuf.b_op_start.lnum    = 0; break;
                            case ']': curbuf.b_op_end.lnum      = 0; break;
                            case '<': curbuf.b_visual.vi_start.lnum = 0; break;
                            case '>': curbuf.b_visual.vi_end.lnum   = 0; break;
                            case ' ': break;
                            default:  emsg2(e_invarg2, p);
                                    return;
                        }
                }
            }
        }
    };

    /*
     * print the jumplist
     */
    /*private*/ static final ex_func_C ex_jumps = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            cleanup_jumplist();

            /* Highlight title. */
            msg_puts_title(u8("\n jump line  col file/text"));

            for (int i = 0; i < curwin.w_jumplistlen && !got_int; i++)
            {
                if (curwin.w_jumplist[i].fmark.mark.lnum != 0)
                {
                    if (curwin.w_jumplist[i].fmark.fnum == 0)
                        fname2fnum(curwin.w_jumplist[i]);

                    Bytes name = fm_getname(curwin.w_jumplist[i].fmark, 16);
                    if (name == null)       /* file name not available */
                        continue;

                    msg_putchar('\n');
                    if (got_int)
                        break;

                    int x = curwin.w_jumplistidx;
                    libC.sprintf(ioBuff, u8("%c %2d %5ld %4d "),
                        (i == x) ? (byte)'>' : (byte)' ',
                        (x < i) ? i - x : x - i,
                        curwin.w_jumplist[i].fmark.mark.lnum,
                        curwin.w_jumplist[i].fmark.mark.col);
                    msg_outtrans(ioBuff);
                    msg_outtrans_attr(name, (curwin.w_jumplist[i].fmark.fnum == curbuf.b_fnum) ? hl_attr(HLF_D) : 0);
                    ui_breakcheck();
                }
                out_flush();
            }

            if (curwin.w_jumplistidx == curwin.w_jumplistlen)
                msg_puts(u8("\n>"));
        }
    };

    /*
     * print the changelist
     */
    /*private*/ static final ex_func_C ex_changes = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            /* Highlight title. */
            msg_puts_title(u8("\nchange line  col text"));

            for (int i = 0; i < curbuf.b_changelistlen && !got_int; i++)
            {
                if (curbuf.b_changelist[i].lnum != 0)
                {
                    msg_putchar('\n');
                    if (got_int)
                        break;
                    int x = curwin.w_changelistidx;
                    libC.sprintf(ioBuff, u8("%c %3d %5ld %4d "),
                            (i == x) ? (byte)'>' : (byte)' ',
                            (x < i) ? i - x : x - i,
                            curbuf.b_changelist[i].lnum,
                            curbuf.b_changelist[i].col);
                    msg_outtrans(ioBuff);
                    Bytes name = mark_line(curbuf.b_changelist[i], 17);
                    if (name == null)
                        break;
                    msg_outtrans_attr(name, hl_attr(HLF_D));
                    ui_breakcheck();
                }
                out_flush();
            }

            if (curwin.w_changelistidx == curbuf.b_changelistlen)
                msg_puts(u8("\n>"));
        }
    };

    /*private*/ static long one_adjust(long add, long line1, long line2, long amount, long amount_after)
    {
        if (line1 <= add && add <= line2)
        {
            if (amount == MAXLNUM)
                add = 0;
            else
                add += amount;
        }
        else if (amount_after != 0 && line2 < add)
            add += amount_after;

        return add;
    }

    /* don't delete the line, just put at first deleted line */
    /*private*/ static long one_adjust_nodel(long add, long line1, long line2, long amount, long amount_after)
    {
        if (line1 <= add && add <= line2)
        {
            if (amount == MAXLNUM)
                add = line1;
            else
                add += amount;
        }
        else if (amount_after != 0 && line2 < add)
            add += amount_after;

        return add;
    }

    /*private*/ static pos_C mark_initpos = new_pos(1, 0, 0);

    /*
     * Adjust marks between line1 and line2 (inclusive) to move 'amount' lines.
     * Must be called before changed_*(), appended_lines() or deleted_lines().
     * May be called before or after changing the text.
     * When deleting lines line1 to line2, use an 'amount' of MAXLNUM:
     * The marks within this range are made invalid.
     * If 'amount_after' is non-zero adjust marks after line2.
     * Example: Delete lines 34 and 35: mark_adjust(34, 35, MAXLNUM, -2);
     * Example: Insert two lines below 55: mark_adjust(56, MAXLNUM, 2, 0);
     *                                 or: mark_adjust(56, 55, MAXLNUM, 2);
     */
    /*private*/ static void mark_adjust(long line1, long line2, long amount, long amount_after)
    {
        int fnum = curbuf.b_fnum;

        if (line2 < line1 && amount_after == 0L)                /* nothing to do */
            return;

        if (!cmdmod.lockmarks)
        {
            /* named marks, lower case and upper case */
            for (int i = 0; i < NMARKS; i++)
            {
                curbuf.b_namedm[i].lnum = one_adjust(curbuf.b_namedm[i].lnum, line1, line2, amount, amount_after);
                if (namedfm[i].fmark.fnum == fnum)
                    namedfm[i].fmark.mark.lnum = one_adjust_nodel(namedfm[i].fmark.mark.lnum, line1, line2, amount, amount_after);
            }
            for (int i = NMARKS; i < NMARKS + EXTRA_MARKS; i++)
            {
                if (namedfm[i].fmark.fnum == fnum)
                    namedfm[i].fmark.mark.lnum = one_adjust_nodel(namedfm[i].fmark.mark.lnum, line1, line2, amount, amount_after);
            }

            /* last Insert position */
            curbuf.b_last_insert.lnum = one_adjust(curbuf.b_last_insert.lnum, line1, line2, amount, amount_after);

            /* last change position */
            curbuf.b_last_change.lnum = one_adjust(curbuf.b_last_change.lnum, line1, line2, amount, amount_after);

            /* last cursor position, if it was set */
            if (!eqpos(curbuf.b_last_cursor, mark_initpos))
                curbuf.b_last_cursor.lnum = one_adjust(curbuf.b_last_cursor.lnum, line1, line2, amount, amount_after);

            /* list of change positions */
            for (int i = 0; i < curbuf.b_changelistlen; i++)
                curbuf.b_changelist[i].lnum = one_adjust_nodel(curbuf.b_changelist[i].lnum, line1, line2, amount, amount_after);

            /* Visual area. */
            curbuf.b_visual.vi_start.lnum = one_adjust_nodel(curbuf.b_visual.vi_start.lnum, line1, line2, amount, amount_after);
            curbuf.b_visual.vi_end.lnum = one_adjust_nodel(curbuf.b_visual.vi_end.lnum, line1, line2, amount, amount_after);
        }

        /* previous context mark */
        curwin.w_pcmark.lnum = one_adjust(curwin.w_pcmark.lnum, line1, line2, amount, amount_after);

        /* previous pcmark */
        curwin.w_prev_pcmark.lnum = one_adjust(curwin.w_prev_pcmark.lnum, line1, line2, amount, amount_after);

        /* saved cursor for formatting */
        if (saved_cursor.lnum != 0)
            saved_cursor.lnum = one_adjust_nodel(saved_cursor.lnum, line1, line2, amount, amount_after);

        /*
         * Adjust items in all windows related to the current buffer.
         */
        for (tabpage_C tab = first_tabpage; tab != null; tab = tab.tp_next)
            for (window_C win = (tab == curtab) ? firstwin : tab.tp_firstwin; win != null; win = win.w_next)
            {
                if (!cmdmod.lockmarks)
                    /* Marks in the jumplist.  When deleting lines, this may create
                     * duplicate marks in the jumplist, they will be removed later. */
                    for (int i = 0; i < win.w_jumplistlen; i++)
                        if (win.w_jumplist[i].fmark.fnum == fnum)
                            win.w_jumplist[i].fmark.mark.lnum = one_adjust_nodel(win.w_jumplist[i].fmark.mark.lnum, line1, line2, amount, amount_after);

                if (win.w_buffer == curbuf)
                {
                    /* the displayed Visual area */
                    if (win.w_old_cursor_lnum != 0)
                    {
                        win.w_old_cursor_lnum = one_adjust_nodel(win.w_old_cursor_lnum, line1, line2, amount, amount_after);
                        win.w_old_visual_lnum = one_adjust_nodel(win.w_old_visual_lnum, line1, line2, amount, amount_after);
                    }

                    /* topline and cursor position for windows with the same buffer
                     * other than the current window */
                    if (win != curwin)
                    {
                        if (line1 <= win.w_topline && win.w_topline <= line2)
                        {
                            if (amount == MAXLNUM)              /* topline is deleted */
                            {
                                if (line1 <= 1)
                                    win.w_topline = 1;
                                else
                                    win.w_topline = line1 - 1;
                            }
                            else                                /* keep topline on the same line */
                                win.w_topline += amount;
                        }
                        else if (amount_after != 0 && line2 < win.w_topline)
                        {
                            win.w_topline += amount_after;
                        }
                        if (line1 <= win.w_cursor.lnum && win.w_cursor.lnum <= line2)
                        {
                            if (amount == MAXLNUM)              /* line with cursor is deleted */
                            {
                                if (line1 <= 1)
                                    win.w_cursor.lnum = 1;
                                else
                                    win.w_cursor.lnum = line1 - 1;
                                win.w_cursor.col = 0;
                            }
                            else                                /* keep cursor on the same line */
                                win.w_cursor.lnum += amount;
                        }
                        else if (amount_after != 0 && line2 < win.w_cursor.lnum)
                            win.w_cursor.lnum += amount_after;
                    }
                }
            }
    }

    /* This code is used often, needs to be fast. */
    /*private*/ static void col_adjust(pos_C posp, long lnum, int mincol, long lnum_amount, long col_amount)
    {
        if (posp.lnum == lnum && mincol <= posp.col)
        {
            posp.lnum += lnum_amount;
            if (col_amount < 0 && posp.col <= (int)-col_amount)
                posp.col = 0;
            else
                posp.col += col_amount;
        }
    }

    /*
     * Adjust marks in line "lnum" at column "mincol" and further: add
     * "lnum_amount" to the line number and add "col_amount" to the column position.
     */
    /*private*/ static void mark_col_adjust(long lnum, int mincol, long lnum_amount, long col_amount)
    {
        int fnum = curbuf.b_fnum;

        if ((col_amount == 0L && lnum_amount == 0L) || cmdmod.lockmarks)
            return; /* nothing to do */

        /* named marks, lower case and upper case */
        for (int i = 0; i < NMARKS; i++)
        {
            col_adjust(curbuf.b_namedm[i], lnum, mincol, lnum_amount, col_amount);
            if (namedfm[i].fmark.fnum == fnum)
                col_adjust(namedfm[i].fmark.mark, lnum, mincol, lnum_amount, col_amount);
        }
        for (int i = NMARKS; i < NMARKS + EXTRA_MARKS; i++)
        {
            if (namedfm[i].fmark.fnum == fnum)
                col_adjust(namedfm[i].fmark.mark, lnum, mincol, lnum_amount, col_amount);
        }

        /* last Insert position */
        col_adjust(curbuf.b_last_insert, lnum, mincol, lnum_amount, col_amount);

        /* last change position */
        col_adjust(curbuf.b_last_change, lnum, mincol, lnum_amount, col_amount);

        /* list of change positions */
        for (int i = 0; i < curbuf.b_changelistlen; i++)
            col_adjust(curbuf.b_changelist[i], lnum, mincol, lnum_amount, col_amount);

        /* Visual area. */
        col_adjust(curbuf.b_visual.vi_start, lnum, mincol, lnum_amount, col_amount);
        col_adjust(curbuf.b_visual.vi_end, lnum, mincol, lnum_amount, col_amount);

        /* previous context mark */
        col_adjust(curwin.w_pcmark, lnum, mincol, lnum_amount, col_amount);

        /* previous pcmark */
        col_adjust(curwin.w_prev_pcmark, lnum, mincol, lnum_amount, col_amount);

        /* saved cursor for formatting */
        col_adjust(saved_cursor, lnum, mincol, lnum_amount, col_amount);

        /*
         * Adjust items in all windows related to the current buffer.
         */
        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
        {
            /* marks in the jumplist */
            for (int i = 0; i < wp.w_jumplistlen; i++)
                if (wp.w_jumplist[i].fmark.fnum == fnum)
                    col_adjust(wp.w_jumplist[i].fmark.mark, lnum, mincol, lnum_amount, col_amount);

            if (wp.w_buffer == curbuf)
            {
                /* cursor position for other windows with the same buffer */
                if (wp != curwin)
                    col_adjust(wp.w_cursor, lnum, mincol, lnum_amount, col_amount);
            }
        }
    }

    /*
     * When deleting lines, this may create duplicate marks in the jumplist.
     * They will be removed here for the current window.
     */
    /*private*/ static void cleanup_jumplist()
    {
        int to = 0;

        for (int from = 0; from < curwin.w_jumplistlen; from++)
        {
            if (curwin.w_jumplistidx == from)
                curwin.w_jumplistidx = to;
            int i;
            for (i = from + 1; i < curwin.w_jumplistlen; i++)
                if (curwin.w_jumplist[i].fmark.fnum == curwin.w_jumplist[from].fmark.fnum
                        && curwin.w_jumplist[from].fmark.fnum != 0
                        && curwin.w_jumplist[i].fmark.mark.lnum == curwin.w_jumplist[from].fmark.mark.lnum)
                    break;
            if (curwin.w_jumplistlen <= i)  /* no duplicate */
                COPY_xfmark(curwin.w_jumplist[to++], curwin.w_jumplist[from]);
            else
                curwin.w_jumplist[from].fname = null;
        }

        if (curwin.w_jumplistidx == curwin.w_jumplistlen)
            curwin.w_jumplistidx = to;
        curwin.w_jumplistlen = to;
    }

    /*
     * Copy the jumplist from window "from" to window "to".
     */
    /*private*/ static void copy_jumplist(window_C from, window_C to)
    {
        for (int i = 0; i < from.w_jumplistlen; i++)
        {
            COPY_xfmark(to.w_jumplist[i], from.w_jumplist[i]);
            if (from.w_jumplist[i].fname != null)
                to.w_jumplist[i].fname = STRDUP(from.w_jumplist[i].fname);
        }
        to.w_jumplistlen = from.w_jumplistlen;
        to.w_jumplistidx = from.w_jumplistidx;
    }

    /*
     * Free items in the jumplist of window "wp".
     */
    /*private*/ static void free_jumplist(window_C wp)
    {
        for (int i = 0; i < wp.w_jumplistlen; i++)
            wp.w_jumplist[i].fname = null;
    }

    /*private*/ static void set_last_cursor(window_C win)
    {
        if (win.w_buffer != null)
            COPY_pos(win.w_buffer.b_last_cursor, win.w_cursor);
    }
}
