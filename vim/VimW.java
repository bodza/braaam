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
public class VimW
{
    /*
     * window.c ---------------------------------------------------------------------------------------
     */

    /*private*/ static final int URL_SLASH       = 1;               /* path_is_url() has found "://" */
    /*private*/ static final int URL_BACKSLASH   = 2;               /* path_is_url() has found ":\\" */

    /*private*/ static final window_C NOWIN = new window_C();    /* non-existing window */

    /*private*/ static Bytes m_onlyone = u8("Already only one window");

    /*
     * all CTRL-W window commands are handled here, called from normal_cmd().
     */
    /*private*/ static void do_window(int nchar, long Prenum, int xchar)
        /* xchar: extra char from ":wincmd gx" or NUL */
    {
        Bytes cbuf = new Bytes(40);

        long Prenum1 = (Prenum == 0) ? 1 : Prenum;

        switch (nchar)
        {
            /* split current window in two parts, horizontally */
            case 'S':
            case Ctrl_S:
            case 's':
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                reset_VIsual_and_resel();
                win_split((int)Prenum, 0);
                break;

            /* split current window in two parts, vertically */
            case Ctrl_V:
            case 'v':
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                reset_VIsual_and_resel();
                win_split((int)Prenum, WSP_VERT);
                break;

            /* split current window and edit alternate file */
            case Ctrl_HAT:
            case '^':
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                reset_VIsual_and_resel();
                cmd_with_count(u8("split #"), cbuf, cbuf.size(), Prenum);
                do_cmdline_cmd(cbuf);
                break;

            /* open new window */
            case Ctrl_N:
            case 'n':
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                reset_VIsual_and_resel();
                if (Prenum != 0)
                    /* window height */
                    vim_snprintf(cbuf, cbuf.size() - 5, u8("%ld"), Prenum);
                else
                    cbuf.be(0, NUL);
                STRCAT(cbuf, u8("new"));
                do_cmdline_cmd(cbuf);
                break;

            /* quit current window */
            case Ctrl_Q:
            case 'q':
                reset_VIsual_and_resel();
                cmd_with_count(u8("quit"), cbuf, cbuf.size(), Prenum);
                do_cmdline_cmd(cbuf);
                break;

            /* close current window */
            case Ctrl_C:
            case 'c':
                reset_VIsual_and_resel();
                cmd_with_count(u8("close"), cbuf, cbuf.size(), Prenum);
                do_cmdline_cmd(cbuf);
                break;

            /* close all but current window */
            case Ctrl_O:
            case 'o':
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                reset_VIsual_and_resel();
                cmd_with_count(u8("only"), cbuf, cbuf.size(), Prenum);
                do_cmdline_cmd(cbuf);
                break;

            /* cursor to next window with wrap around */
            case Ctrl_W:
            case 'w':
            /* cursor to previous window with wrap around */
            case 'W':
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                if (firstwin == lastwin && Prenum != 1) /* just one window */
                    beep_flush();
                else
                {
                    window_C wp;
                    if (Prenum != 0)                     /* go to specified window */
                    {
                        for (wp = firstwin; 0 < --Prenum; )
                        {
                            if (wp.w_next == null)
                                break;
                            else
                                wp = wp.w_next;
                        }
                    }
                    else
                    {
                        if (nchar == 'W')           /* go to previous window */
                        {
                            wp = curwin.w_prev;
                            if (wp == null)
                                wp = lastwin;       /* wrap around */
                        }
                        else                        /* go to next window */
                        {
                            wp = curwin.w_next;
                            if (wp == null)
                                wp = firstwin;      /* wrap around */
                        }
                    }
                    win_goto(wp);
                }
                break;

            /* cursor to window below */
            case 'j':
            case K_DOWN:
            case Ctrl_J:
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                win_goto_ver(false, Prenum1);
                break;

            /* cursor to window above */
            case 'k':
            case K_UP:
            case Ctrl_K:
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                win_goto_ver(true, Prenum1);
                break;

            /* cursor to left window */
            case 'h':
            case K_LEFT:
            case Ctrl_H:
            case K_BS:
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                win_goto_hor(true, Prenum1);
                break;

            /* cursor to right window */
            case 'l':
            case K_RIGHT:
            case Ctrl_L:
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                win_goto_hor(false, Prenum1);
                break;

            /* move window to new tab page */
            case 'T':
                if (one_window())
                    msg(m_onlyone);
                else
                {
                    tabpage_C oldtab = curtab;

                    /* First create a new tab with the window,
                     * then go back to the old tab and close the window there. */
                    window_C wp = curwin;
                    if (win_new_tabpage((int)Prenum) == true && valid_tabpage(oldtab))
                    {
                        tabpage_C newtab = curtab;
                        goto_tabpage_tp(oldtab, true, true);
                        if (curwin == wp)
                            win_close(curwin, false);
                        if (valid_tabpage(newtab))
                            goto_tabpage_tp(newtab, true, true);
                    }
                }
                break;

            /* cursor to top-left window */
            case 't':
            case Ctrl_T:
                win_goto(firstwin);
                break;

            /* cursor to bottom-right window */
            case 'b':
            case Ctrl_B:
                win_goto(lastwin);
                break;

            /* cursor to last accessed (previous) window */
            case 'p':
            case Ctrl_P:
                if (prevwin == null)
                    beep_flush();
                else
                    win_goto(prevwin);
                break;

            /* exchange current and next window */
            case 'x':
            case Ctrl_X:
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                win_exchange(Prenum);
                break;

            /* rotate windows downwards */
            case Ctrl_R:
            case 'r':
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                reset_VIsual_and_resel();
                win_rotate(false, (int)Prenum1);
                break;

            /* rotate windows upwards */
            case 'R':
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                reset_VIsual_and_resel();
                win_rotate(true, (int)Prenum1);
                break;

            /* move window to the very top/bottom/left/right */
            case 'K':
            case 'J':
            case 'H':
            case 'L':
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                win_totop((int)Prenum,
                        ((nchar == 'H' || nchar == 'L') ? WSP_VERT : 0)
                      | ((nchar == 'H' || nchar == 'K') ? WSP_TOP : WSP_BOT));
                break;

            /* make all windows the same height */
            case '=':
                win_equal(null, false, 'b');
                break;

            /* increase current window height */
            case '+':
                win_setheight(curwin.w_height + (int)Prenum1);
                break;

            /* decrease current window height */
            case '-':
                win_setheight(curwin.w_height - (int)Prenum1);
                break;

            /* set current window height */
            case Ctrl__:
            case '_':
                win_setheight(Prenum != 0 ? (int)Prenum : 9999);
                break;

            /* increase current window width */
            case '>':
                win_setwidth(curwin.w_width + (int)Prenum1);
                break;

            /* decrease current window width */
            case '<':
                win_setwidth(curwin.w_width - (int)Prenum1);
                break;

            /* set current window width */
            case '|':
                win_setwidth(Prenum != 0 ? (int)Prenum : 9999);
                break;

            /* jump to tag and split window if tag exists (in preview window) */
            case ']':
            case Ctrl_RSB:
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                /* keep Visual mode, can select words to use as a tag */
                if (Prenum != 0)
                    postponed_split = (int)Prenum;
                else
                    postponed_split = -1;

                /* Execute the command right here,
                 * required when "wincmd ]" was used in a function. */
                do_nv_ident(Ctrl_RSB, NUL);
                break;

            case K_KENTER:
            case CAR:
                break;

            /* CTRL-W g extended commands. */
            case 'g':
            case Ctrl_G:
                if (cmdwin_type != 0)
                {
                    emsg(e_cmdwin);
                    break;
                }
                no_mapping++;
                allow_keys++;       /* no mapping for xchar, but allow key codes */
                if (xchar == NUL)
                    xchar = plain_vgetc();
                --no_mapping;
                --allow_keys;
                add_to_showcmd(xchar);
                switch (xchar)
                {
                    case ']':
                    case Ctrl_RSB:
                        /* keep Visual mode, can select words to use as a tag */
                        if (Prenum != 0)
                            postponed_split = (int)Prenum;
                        else
                            postponed_split = -1;

                        /* Execute the command right here,
                         * required when "wincmd g}" was used in a function. */
                        do_nv_ident('g', xchar);
                        break;

                    default:
                        beep_flush();
                        break;
                }
                break;

            default:
                beep_flush();
                break;
        }
    }

    /*
     * Figure out the address type for ":wnncmd".
     */
    /*private*/ static void get_wincmd_addr_type(Bytes arg, exarg_C eap)
    {
        switch (arg.at(0))
        {
            case 'S':
            case Ctrl_S:
            case 's':
            case Ctrl_N:
            case 'n':
            case 'j':
            case Ctrl_J:
            case 'k':
            case Ctrl_K:
            case 'T':
            case Ctrl_R:
            case 'r':
            case 'R':
            case 'K':
            case 'J':
            case '+':
            case '-':
            case Ctrl__:
            case '_':
            case '|':
            case ']':
            case Ctrl_RSB:
            case 'g':
            case Ctrl_G:
            case Ctrl_V:
            case 'v':
            case 'h':
            case Ctrl_H:
            case 'l':
            case Ctrl_L:
            case 'H':
            case 'L':
            case '>':
            case '<':
                /* window size or any count */
                eap.addr_type = ADDR_LINES;
                break;

            case Ctrl_HAT:
            case '^':
                /* buffer number */
                eap.addr_type = ADDR_BUFFERS;
                break;

            case Ctrl_Q:
            case 'q':
            case Ctrl_C:
            case 'c':
            case Ctrl_O:
            case 'o':
            case Ctrl_W:
            case 'w':
            case 'W':
            case 'x':
            case Ctrl_X:
                /* window number */
                eap.addr_type = ADDR_WINDOWS;
                break;

            case 't':
            case Ctrl_T:
            case 'b':
            case Ctrl_B:
            case 'p':
            case Ctrl_P:
            case '=':
            case CAR:
                /* no count */
                eap.addr_type = 0;
                break;
        }
    }

    /*private*/ static void cmd_with_count(Bytes cmd, Bytes bufp, int bufsize, long Prenum)
    {
        int len = strlen(cmd);

        STRCPY(bufp, cmd);
        if (0 < Prenum)
            vim_snprintf(bufp.plus(len), bufsize - len, u8("%ld"), Prenum);
    }

    /*
     * split the current window, implements CTRL-W s and :split
     *
     * "size" is the height or width for the new window, 0 to use half of current height or width.
     *
     * "flags":
     * WSP_ROOM: require enough room for new window
     * WSP_VERT: vertical split.
     * WSP_TOP:  open window at the top-left of the shell (help window).
     * WSP_BOT:  open window at the bottom-right of the shell (quickfix window).
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean win_split(int size, int flags)
    {
        /* When the ":tab" modifier was used open a new tab page instead. */
        if (may_open_tabpage() == true)
            return true;

        /* Add flags from ":vertical", ":topleft" and ":botright". */
        flags |= cmdmod.split;
        if ((flags & WSP_TOP) != 0 && (flags & WSP_BOT) != 0)
        {
            emsg(u8("E442: Can't split topleft and botright at the same time"));
            return false;
        }

        return win_split_ins(size, flags, null, 0);
    }

    /*
     * When "new_wp" is null: split the current window in two.
     * When "new_wp" is not null: insert this window at the far top/left/right/bottom.
     * return false for failure, true otherwise
     */
    /*private*/ static boolean win_split_ins(int size, int flags, window_C new_wp, int dir)
    {
        window_C wp = new_wp;
        int new_size = size;
        boolean do_equal = false;
        int oldwin_height = 0;

        window_C oldwin;
        if ((flags & WSP_TOP) != 0)
            oldwin = firstwin;
        else if ((flags & WSP_BOT) != 0)
            oldwin = lastwin;
        else
            oldwin = curwin;

        int need_status = 0;
        /* add a status line when p_ls == 1 and splitting the first window */
        if (lastwin == firstwin && p_ls[0] == 1 && oldwin.w_status_height == 0)
        {
            if (oldwin.w_height <= p_wmh[0] && new_wp == null)
            {
                emsg(e_noroom);
                return false;
            }
            need_status = STATUS_HEIGHT;
        }

        byte layout;
        if ((flags & WSP_VERT) != 0)
        {
            layout = FR_ROW;

            /*
             * Check if we are able to split the current window and compute its width.
             */
            /* Current window requires at least 1 space. */
            int wmw1 = (p_wmw[0] == 0) ? 1 : (int)p_wmw[0];
            int needed = wmw1 + 1;
            if ((flags & WSP_ROOM) != 0)
                needed += p_wiw[0] - wmw1;
            int minwidth;
            int available;
            if ((flags & (WSP_BOT | WSP_TOP)) != 0)
            {
                minwidth = frame_minwidth(topframe, NOWIN);
                available = topframe.fr_width;
                needed += minwidth;
            }
            else if (p_ea[0])
            {
                minwidth = frame_minwidth(oldwin.w_frame, NOWIN);
                frame_C prevfrp = oldwin.w_frame;
                for (frame_C frp = oldwin.w_frame.fr_parent; frp != null; frp = frp.fr_parent)
                {
                    if (frp.fr_layout == FR_ROW)
                        for (frame_C frp2 = frp.fr_child; frp2 != null; frp2 = frp2.fr_next)
                            if (frp2 != prevfrp)
                                minwidth += frame_minwidth(frp2, NOWIN);
                    prevfrp = frp;
                }
                available = topframe.fr_width;
                needed += minwidth;
            }
            else
            {
                minwidth = frame_minwidth(oldwin.w_frame, NOWIN);
                available = oldwin.w_frame.fr_width;
                needed += minwidth;
            }
            if (available < needed && new_wp == null)
            {
                emsg(e_noroom);
                return false;
            }
            if (new_size == 0)
                new_size = oldwin.w_width / 2;
            if (new_size > available - minwidth - 1)
                new_size = available - minwidth - 1;
            if (new_size < wmw1)
                new_size = wmw1;

            /* if it doesn't fit in the current window, need win_equal() */
            if (oldwin.w_width - new_size - 1 < p_wmw[0])
                do_equal = true;

            /* We don't like to take lines for the new window from a 'winfixwidth' window.
             * Take them from a window to the left or right instead, if possible. */
            if (oldwin.w_onebuf_opt.wo_wfw[0])
                win_setwidth_win(oldwin.w_width + new_size, oldwin);

            /* Only make all windows the same width if one of them (except oldwin)
             * is wider than one of the split windows. */
            if (!do_equal && p_ea[0] && size == 0 && p_ead[0].at(0) != (byte)'v' && oldwin.w_frame.fr_parent != null)
            {
                frame_C frp = oldwin.w_frame.fr_parent.fr_child;
                while (frp != null)
                {
                    if (frp.fr_win != oldwin && frp.fr_win != null
                            && (new_size < frp.fr_win.w_width
                                || oldwin.w_width - new_size - 1 < frp.fr_win.w_width))
                    {
                        do_equal = true;
                        break;
                    }
                    frp = frp.fr_next;
                }
            }
        }
        else
        {
            layout = FR_COL;

            /*
             * Check if we are able to split the current window and compute its height.
             */
            /* Current window requires at least 1 space. */
            int wmh1 = (p_wmh[0] == 0) ? 1 : (int)p_wmh[0];
            int needed = wmh1 + STATUS_HEIGHT;
            if ((flags & WSP_ROOM) != 0)
                needed += p_wh[0] - wmh1;
            int minheight;
            int available;
            if ((flags & (WSP_BOT | WSP_TOP)) != 0)
            {
                minheight = frame_minheight(topframe, NOWIN) + need_status;
                available = topframe.fr_height;
                needed += minheight;
            }
            else if (p_ea[0])
            {
                minheight = frame_minheight(oldwin.w_frame, NOWIN) + need_status;
                frame_C prevfrp = oldwin.w_frame;
                for (frame_C frp = oldwin.w_frame.fr_parent; frp != null; frp = frp.fr_parent)
                {
                    if (frp.fr_layout == FR_COL)
                        for (frame_C frp2 = frp.fr_child; frp2 != null; frp2 = frp2.fr_next)
                            if (frp2 != prevfrp)
                                minheight += frame_minheight(frp2, NOWIN);
                    prevfrp = frp;
                }
                available = topframe.fr_height;
                needed += minheight;
            }
            else
            {
                minheight = frame_minheight(oldwin.w_frame, NOWIN) + need_status;
                available = oldwin.w_frame.fr_height;
                needed += minheight;
            }
            if (available < needed && new_wp == null)
            {
                emsg(e_noroom);
                return false;
            }
            oldwin_height = oldwin.w_height;
            if (need_status != 0)
            {
                oldwin.w_status_height = STATUS_HEIGHT;
                oldwin_height -= STATUS_HEIGHT;
            }
            if (new_size == 0)
                new_size = oldwin_height / 2;
            if (new_size > available - minheight - STATUS_HEIGHT)
                new_size = available - minheight - STATUS_HEIGHT;
            if (new_size < wmh1)
                new_size = wmh1;

            /* if it doesn't fit in the current window, need win_equal() */
            if (oldwin_height - new_size - STATUS_HEIGHT < p_wmh[0])
                do_equal = true;

            /* We don't like to take lines for the new window from a 'winfixheight' window.
             * Take them from a window above or below instead, if possible. */
            if (oldwin.w_onebuf_opt.wo_wfh[0])
            {
                win_setheight_win(oldwin.w_height + new_size + STATUS_HEIGHT, oldwin);
                oldwin_height = oldwin.w_height;
                if (need_status != 0)
                    oldwin_height -= STATUS_HEIGHT;
            }

            /* Only make all windows the same height if one of them (except oldwin)
             * is higher than one of the split windows. */
            if (!do_equal && p_ea[0] && size == 0 && p_ead[0].at(0) != (byte)'h' && oldwin.w_frame.fr_parent != null)
            {
                frame_C frp = oldwin.w_frame.fr_parent.fr_child;
                while (frp != null)
                {
                    if (frp.fr_win != oldwin && frp.fr_win != null
                            && (new_size < frp.fr_win.w_height
                                || oldwin_height - new_size - STATUS_HEIGHT < frp.fr_win.w_height))
                    {
                        do_equal = true;
                        break;
                    }
                    frp = frp.fr_next;
                }
            }
        }

        /*
         * allocate new window structure and link it in the window list
         */
        if ((flags & WSP_TOP) == 0
                && ((flags & WSP_BOT) != 0
                    || (flags & WSP_BELOW) != 0
                    || ((flags & WSP_ABOVE) == 0
                        && ((flags & WSP_VERT) != 0 ? p_spr[0] : p_sb[0]))))
        {
            /* new window below/right of current one */
            if (new_wp == null)
                wp = newWindow(oldwin, false);
            else
                win_append(oldwin, wp);
        }
        else
        {
            if (new_wp == null)
                wp = newWindow(oldwin.w_prev, false);
            else
                win_append(oldwin.w_prev, wp);
        }

        if (new_wp == null)
        {
            if (wp == null)
                return false;

            wp.w_frame = newFrame(wp);

            /* make the contents of the new window the same as the current one */
            win_init(wp, curwin, flags);
        }

        /*
         * Reorganise the tree of frames to insert the new window.
         */
        frame_C curfrp;
        boolean before;
        if ((flags & (WSP_TOP | WSP_BOT)) != 0)
        {
            if ((topframe.fr_layout == FR_COL && (flags & WSP_VERT) == 0)
                || (topframe.fr_layout == FR_ROW && (flags & WSP_VERT) != 0))
            {
                curfrp = topframe.fr_child;
                if ((flags & WSP_BOT) != 0)
                    while (curfrp.fr_next != null)
                        curfrp = curfrp.fr_next;
            }
            else
                curfrp = topframe;
            before = ((flags & WSP_TOP) != 0);
        }
        else
        {
            curfrp = oldwin.w_frame;
            if ((flags & WSP_BELOW) != 0)
                before = false;
            else if ((flags & WSP_ABOVE) != 0)
                before = true;
            else if ((flags & WSP_VERT) != 0)
                before = !p_spr[0];
            else
                before = !p_sb[0];
        }
        if (curfrp.fr_parent == null || curfrp.fr_parent.fr_layout != layout)
        {
            /* Need to create a new frame in the tree to make a branch. */
            frame_C frp = new frame_C();
            COPY_frame(frp, curfrp);
            curfrp.fr_layout = layout;
            frp.fr_parent = curfrp;
            frp.fr_next = null;
            frp.fr_prev = null;
            curfrp.fr_child = frp;
            curfrp.fr_win = null;
            curfrp = frp;
            if (frp.fr_win != null)
                oldwin.w_frame = frp;
            else
                for (frp = frp.fr_child; frp != null; frp = frp.fr_next)
                    frp.fr_parent = curfrp;
        }

        frame_C frp;
        if (new_wp == null)
            frp = wp.w_frame;
        else
            frp = new_wp.w_frame;
        frp.fr_parent = curfrp.fr_parent;

        /* Insert the new frame at the right place in the frame list. */
        if (before)
            frame_insert(curfrp, frp);
        else
            frame_append(curfrp, frp);

        /* Set w_fraction now so that the cursor keeps the same relative vertical position. */
        if (0 < oldwin.w_height)
            set_fraction(oldwin);
        wp.w_fraction = oldwin.w_fraction;

        if ((flags & WSP_VERT) != 0)
        {
            wp.w_onebuf_opt.wo_scr[0] = curwin.w_onebuf_opt.wo_scr[0];

            if (need_status != 0)
            {
                win_new_height(oldwin, oldwin.w_height - 1);
                oldwin.w_status_height = need_status;
            }
            if ((flags & (WSP_TOP | WSP_BOT)) != 0)
            {
                /* set height and row of new window to full height */
                wp.w_winrow = tabline_height();
                win_new_height(wp, curfrp.fr_height - (0 < p_ls[0] ? 1 : 0));
                wp.w_status_height = (0 < p_ls[0]) ? 1 : 0;
            }
            else
            {
                /* height and row of new window is same as current window */
                wp.w_winrow = oldwin.w_winrow;
                win_new_height(wp, oldwin.w_height);
                wp.w_status_height = oldwin.w_status_height;
            }
            frp.fr_height = curfrp.fr_height;

            /* "new_size" of the current window goes to the new window,
             * use one column for the vertical separator */
            win_new_width(wp, new_size);
            if (before)
                wp.w_vsep_width = 1;
            else
            {
                wp.w_vsep_width = oldwin.w_vsep_width;
                oldwin.w_vsep_width = 1;
            }
            if ((flags & (WSP_TOP | WSP_BOT)) != 0)
            {
                if ((flags & WSP_BOT) != 0)
                    frame_add_vsep(curfrp);
                /* Set width of neighbor frame. */
                frame_new_width(curfrp, curfrp.fr_width - (new_size + ((flags & WSP_TOP) != 0 ? 1 : 0)), (flags & WSP_TOP) != 0, false);
            }
            else
                win_new_width(oldwin, oldwin.w_width - (new_size + 1));
            if (before)     /* new window left of current one */
            {
                wp.w_wincol = oldwin.w_wincol;
                oldwin.w_wincol += new_size + 1;
            }
            else            /* new window right of current one */
                wp.w_wincol = oldwin.w_wincol + oldwin.w_width + 1;
            frame_fix_width(oldwin);
            frame_fix_width(wp);
        }
        else
        {
            /* width and column of new window is same as current window */
            if ((flags & (WSP_TOP | WSP_BOT)) != 0)
            {
                wp.w_wincol = 0;
                win_new_width(wp, (int)Columns[0]);
                wp.w_vsep_width = 0;
            }
            else
            {
                wp.w_wincol = oldwin.w_wincol;
                win_new_width(wp, oldwin.w_width);
                wp.w_vsep_width = oldwin.w_vsep_width;
            }
            frp.fr_width = curfrp.fr_width;

            /* "new_size" of the current window goes to the new window,
             * use one row for the status line */
            win_new_height(wp, new_size);
            if ((flags & (WSP_TOP | WSP_BOT)) != 0)
                frame_new_height(curfrp, curfrp.fr_height - (new_size + STATUS_HEIGHT), (flags & WSP_TOP) != 0, false);
            else
                win_new_height(oldwin, oldwin_height - (new_size + STATUS_HEIGHT));
            if (before)     /* new window above current one */
            {
                wp.w_winrow = oldwin.w_winrow;
                wp.w_status_height = STATUS_HEIGHT;
                oldwin.w_winrow += wp.w_height + STATUS_HEIGHT;
            }
            else            /* new window below current one */
            {
                wp.w_winrow = oldwin.w_winrow + oldwin.w_height + STATUS_HEIGHT;
                wp.w_status_height = oldwin.w_status_height;
                oldwin.w_status_height = STATUS_HEIGHT;
            }
            if ((flags & WSP_BOT) != 0)
                frame_add_statusline(curfrp);
            frame_fix_height(wp);
            frame_fix_height(oldwin);
        }

        if ((flags & (WSP_TOP | WSP_BOT)) != 0)
            win_comp_pos();

        /*
         * Both windows need redrawing
         */
        redraw_win_later(wp, NOT_VALID);
        wp.w_redr_status = true;
        redraw_win_later(oldwin, NOT_VALID);
        oldwin.w_redr_status = true;

        if (need_status != 0)
        {
            msg_row = (int)Rows[0] - 1;
            msg_col = sc_col;
            msg_clr_eos_force();    /* old command/ruler may still be there */
            comp_col();
            msg_row = (int)Rows[0] - 1;
            msg_col = 0;            /* put position back at start of line */
        }

        /*
         * equalize the window sizes.
         */
        if (do_equal || dir != 0)
            win_equal(wp, true, (flags & WSP_VERT) != 0 ? (dir == 'v' ? 'b' : 'h') : (dir == 'h' ? 'b' : 'v'));

        /* Don't change the window height/width to 'winheight' / 'winwidth' if a size was given. */
        int i;
        if ((flags & WSP_VERT) != 0)
        {
            i = (int)p_wiw[0];
            if (size != 0)
                p_wiw[0] = size;
        }
        else
        {
            i = (int)p_wh[0];
            if (size != 0)
                p_wh[0] = size;
        }

        /* Keep same changelist position in new window. */
        wp.w_changelistidx = oldwin.w_changelistidx;

        /*
         * make the new window the current window
         */
        win_enter(wp, false);
        if ((flags & WSP_VERT) != 0)
            p_wiw[0] = i;
        else
            p_wh[0] = i;

        return true;
    }

    /*
     * Initialize window "newp" from window "oldp".
     * Used when splitting a window and when creating a new tab page.
     * The windows will both edit the same buffer.
     * WSP_NEWLOC may be specified in flags to prevent the location list from being copied.
     */
    /*private*/ static void win_init(window_C newp, window_C oldp, int _flags)
    {
        newp.w_buffer = oldp.w_buffer;
        newp.w_s = oldp.w_buffer.b_s;
        oldp.w_buffer.b_nwindows++;
        COPY_pos(newp.w_cursor, oldp.w_cursor);
        newp.w_valid = 0;
        newp.w_curswant = oldp.w_curswant;
        newp.w_set_curswant = oldp.w_set_curswant;
        newp.w_topline = oldp.w_topline;
        newp.w_leftcol = oldp.w_leftcol;
        COPY_pos(newp.w_pcmark, oldp.w_pcmark);
        COPY_pos(newp.w_prev_pcmark, oldp.w_prev_pcmark);
        newp.w_alt_fnum = oldp.w_alt_fnum;
        newp.w_wrow = oldp.w_wrow;
        newp.w_fraction = oldp.w_fraction;
        newp.w_prev_fraction_row = oldp.w_prev_fraction_row;
        copy_jumplist(oldp, newp);

        win_init_some(newp, oldp);

        check_colorcolumn(newp);
    }

    /*
     * Initialize window "newp" from window "old".
     * Only the essential things are copied.
     */
    /*private*/ static void win_init_some(window_C newp, window_C oldp)
    {
        /* Use the same argument list. */
        newp.w_alist = oldp.w_alist;
        newp.w_alist.al_refcount++;
        newp.w_arg_idx = oldp.w_arg_idx;

        /* copy options from existing window */
        win_copy_options(oldp, newp);
    }

    /*
     * Check if "win" is a pointer to an existing window.
     */
    /*private*/ static boolean win_valid(window_C win)
    {
        if (win == null)
            return false;

        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            if (wp == win)
                return true;

        return false;
    }

    /*
     * Return the number of windows.
     */
    /*private*/ static int win_count()
    {
        int count = 0;

        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            count++;

        return count;
    }

    /*
     * Make "count" windows on the screen.
     * Return actual number of windows on the screen.
     * Must be called when there is just one window, filling the whole screen
     * (excluding the command line).
     */
    /*private*/ static int make_windows(int count, boolean vertical)
        /* vertical: split windows vertically if true */
    {
        int maxcount;
        if (vertical)
        {
            /* Each windows needs at least 'winminwidth' lines and a separator column. */
            maxcount = (curwin.w_width + curwin.w_vsep_width - (int)(p_wiw[0] - p_wmw[0])) / ((int)p_wmw[0] + 1);
        }
        else
        {
            /* Each window needs at least 'winminheight' lines and a status line. */
            maxcount = (curwin.w_height + curwin.w_status_height - (int)(p_wh[0] - p_wmh[0])) / ((int)p_wmh[0] + STATUS_HEIGHT);
        }

        if (maxcount < 2)
            maxcount = 2;
        if (maxcount < count)
            count = maxcount;

        /*
         * add status line now, otherwise first window will be too big
         */
        if (1 < count)
            last_status(true);

        /*
         * Don't execute autocommands while creating the windows.  Must do that
         * when putting the buffers in the windows.
         */
        block_autocmds();

        int todo;
        /* todo is number of windows left to create */
        for (todo = count - 1; 0 < todo; --todo)
            if (vertical)
            {
                if (win_split(curwin.w_width - (curwin.w_width - todo) / (todo + 1) - 1, WSP_VERT | WSP_ABOVE) == false)
                    break;
            }
            else
            {
                if (win_split(curwin.w_height - (curwin.w_height - todo * STATUS_HEIGHT) / (todo + 1) - STATUS_HEIGHT, WSP_ABOVE) == false)
                    break;
            }

        unblock_autocmds();

        /* return actual number of windows */
        return (count - todo);
    }

    /*
     * Exchange current and next window
     */
    /*private*/ static void win_exchange(long Prenum)
    {
        if (lastwin == firstwin)        /* just one window */
        {
            beep_flush();
            return;
        }

        frame_C frp;
        /*
         * find window to exchange with
         */
        if (Prenum != 0)
        {
            frp = curwin.w_frame.fr_parent.fr_child;
            while (frp != null && 0 < --Prenum)
                frp = frp.fr_next;
        }
        else if (curwin.w_frame.fr_next != null)    /* Swap with next. */
            frp = curwin.w_frame.fr_next;
        else    /* Swap last window in row/col with previous. */
            frp = curwin.w_frame.fr_prev;

        /* We can only exchange a window with another window, not with a frame containing windows. */
        if (frp == null || frp.fr_win == null || frp.fr_win == curwin)
            return;

        window_C wp = frp.fr_win;

        /*
         * 1. remove curwin from the list.  Remember after which window it was in wp2
         * 2. insert curwin before wp in the list
         * if wp != wp2
         *    3. remove wp from the list
         *    4. insert wp after wp2
         * 5. exchange the status line height and vsep width.
         */
        window_C wp2 = curwin.w_prev;
        frame_C frp2 = curwin.w_frame.fr_prev;
        if (wp.w_prev != curwin)
        {
            win_remove(curwin, null);
            frame_remove(curwin.w_frame);
            win_append(wp.w_prev, curwin);
            frame_insert(frp, curwin.w_frame);
        }
        if (wp != wp2)
        {
            win_remove(wp, null);
            frame_remove(wp.w_frame);
            win_append(wp2, wp);
            if (frp2 == null)
                frame_insert(wp.w_frame.fr_parent.fr_child, wp.w_frame);
            else
                frame_append(frp2, wp.w_frame);
        }

        int temp = curwin.w_status_height;
        curwin.w_status_height = wp.w_status_height;
        wp.w_status_height = temp;
        temp = curwin.w_vsep_width;
        curwin.w_vsep_width = wp.w_vsep_width;
        wp.w_vsep_width = temp;

        /* If the windows are not in the same frame, exchange the sizes to avoid
         * messing up the window layout.  Otherwise fix the frame sizes. */
        if (curwin.w_frame.fr_parent != wp.w_frame.fr_parent)
        {
            temp = curwin.w_height;
            curwin.w_height = wp.w_height;
            wp.w_height = temp;
            temp = curwin.w_width;
            curwin.w_width = wp.w_width;
            wp.w_width = temp;
        }
        else
        {
            frame_fix_height(curwin);
            frame_fix_height(wp);
            frame_fix_width(curwin);
            frame_fix_width(wp);
        }

        win_comp_pos();                 /* recompute window positions */

        win_enter(wp, true);
        redraw_later(CLEAR);
    }

    /*
     * rotate windows: if upwards true the second window becomes the first one
     *                 if upwards false the first window becomes the second one
     */
    /*private*/ static void win_rotate(boolean upwards, int count)
    {
        if (firstwin == lastwin)            /* nothing to do */
        {
            beep_flush();
            return;
        }

        frame_C frp;
        /* Check if all frames in this row/col have one window. */
        for (frp = curwin.w_frame.fr_parent.fr_child; frp != null; frp = frp.fr_next)
            if (frp.fr_win == null)
            {
                emsg(u8("E443: Cannot rotate when another window is split"));
                return;
            }

        while (0 < count--)
        {
            window_C wp1, wp2;

            if (upwards)            /* first window becomes last window */
            {
                /* remove first window/frame from the list */
                frp = curwin.w_frame.fr_parent.fr_child;
                wp1 = frp.fr_win;
                win_remove(wp1, null);
                frame_remove(frp);

                /* find last frame and append removed window/frame after it */
                for ( ; frp.fr_next != null; frp = frp.fr_next)
                    ;
                win_append(frp.fr_win, wp1);
                frame_append(frp, wp1.w_frame);

                wp2 = frp.fr_win;   /* previously last window */
            }
            else                    /* last window becomes first window */
            {
                /* find last window/frame in the list and remove it */
                for (frp = curwin.w_frame; frp.fr_next != null; frp = frp.fr_next)
                    ;
                wp1 = frp.fr_win;
                wp2 = wp1.w_prev;   /* will become last window */
                win_remove(wp1, null);
                frame_remove(frp);

                /* append the removed window/frame before the first in the list */
                win_append(frp.fr_parent.fr_child.fr_win.w_prev, wp1);
                frame_insert(frp.fr_parent.fr_child, frp);
            }

            /* exchange status height and vsep width of old and new last window */
            int n = wp2.w_status_height;
            wp2.w_status_height = wp1.w_status_height;
            wp1.w_status_height = n;
            frame_fix_height(wp1);
            frame_fix_height(wp2);

            n = wp2.w_vsep_width;
            wp2.w_vsep_width = wp1.w_vsep_width;
            wp1.w_vsep_width = n;
            frame_fix_width(wp1);
            frame_fix_width(wp2);

            /* recompute w_winrow and w_wincol for all windows */
            win_comp_pos();
        }

        redraw_later(CLEAR);
    }

    /*
     * Move the current window to the very top/bottom/left/right of the screen.
     */
    /*private*/ static void win_totop(int size, int flags)
    {
        int height = curwin.w_height;

        if (lastwin == firstwin)
        {
            beep_flush();
            return;
        }

        /* Remove the window and frame from the tree of frames. */
        int[] dir = new int[1];
        winframe_remove(curwin, dir, null);
        win_remove(curwin, null);
        last_status(false);             /* may need to remove last status line */
        win_comp_pos();                 /* recompute window positions */

        /* Split a window on the desired side and put the window there. */
        win_split_ins(size, flags, curwin, dir[0]);
        if ((flags & WSP_VERT) == 0)
        {
            win_setheight(height);
            if (p_ea[0])
                win_equal(curwin, true, 'v');
        }
    }

    /*
     * Move window "win1" to below/right of "win2" and make "win1" the current
     * window.  Only works within the same frame!
     */
    /*private*/ static void win_move_after(window_C win1, window_C win2)
    {
        /* check if the arguments are reasonable */
        if (win1 == win2)
            return;

        /* check if there is something to do */
        if (win2.w_next != win1)
        {
            /* may need move the status line/vertical separator of the last window */
            if (win1 == lastwin)
            {
                int height = win1.w_prev.w_status_height;
                win1.w_prev.w_status_height = win1.w_status_height;
                win1.w_status_height = height;
                if (win1.w_prev.w_vsep_width == 1)
                {
                    /* Remove the vertical separator from the last-but-one window,
                     * add it to the last window.  Adjust the frame widths. */
                    win1.w_prev.w_vsep_width = 0;
                    win1.w_prev.w_frame.fr_width -= 1;
                    win1.w_vsep_width = 1;
                    win1.w_frame.fr_width += 1;
                }
            }
            else if (win2 == lastwin)
            {
                int height = win1.w_status_height;
                win1.w_status_height = win2.w_status_height;
                win2.w_status_height = height;
                if (win1.w_vsep_width == 1)
                {
                    /* Remove the vertical separator from win1, add it to the last
                     * window, win2.  Adjust the frame widths. */
                    win2.w_vsep_width = 1;
                    win2.w_frame.fr_width += 1;
                    win1.w_vsep_width = 0;
                    win1.w_frame.fr_width -= 1;
                }
            }
            win_remove(win1, null);
            frame_remove(win1.w_frame);
            win_append(win2, win1);
            frame_append(win2.w_frame, win1.w_frame);

            win_comp_pos();             /* recompute w_winrow for all windows */
            redraw_later(NOT_VALID);
        }
        win_enter(win1, false);
    }

    /*
     * Make all windows the same height.
     * 'next_curwin' will soon be the current window, make sure it has enough rows.
     */
    /*private*/ static void win_equal(window_C next_curwin, boolean current, int dir)
        /* next_curwin: pointer to current window to be or null */
        /* current: do only frame with current window */
        /* dir: 'v' for vertically, 'h' for horizontally, 'b' for both, 0 for using "p_ead" */
    {
        if (dir == 0)
            dir = p_ead[0].at(0);
        win_equal_rec((next_curwin == null) ? curwin : next_curwin, current,
                          topframe, dir, 0, tabline_height(), (int)Columns[0], topframe.fr_height);
    }

    /*
     * Set a frame to a new position and height, spreading the available room
     * equally over contained frames.
     * The window "next_curwin" (if not null) should at least get the size from
     * 'winheight' and 'winwidth' if possible.
     */
    /*private*/ static void win_equal_rec(window_C next_curwin, boolean current, frame_C topfr, int dir, int col, int row, int width, int height)
        /* next_curwin: pointer to current window to be or null */
        /* current: do only frame with current window */
        /* topfr: frame to set size off */
        /* dir: 'v', 'h' or 'b', see win_equal() */
        /* col: horizontal position for frame */
        /* row: vertical position for frame */
        /* width: new width of frame */
        /* height: new height of frame */
    {
        int extra_sep = 0;
        int wincount, totwincount = 0;
        int next_curwin_size = 0;
        int room = 0;
        boolean has_next_curwin = false;

        if (topfr.fr_layout == FR_LEAF)
        {
            /* Set the width/height of this frame.
             * Redraw when size or position changes */
            if (topfr.fr_height != height || topfr.fr_win.w_winrow != row
               || topfr.fr_width != width || topfr.fr_win.w_wincol != col)
            {
                topfr.fr_win.w_winrow = row;
                frame_new_height(topfr, height, false, false);
                topfr.fr_win.w_wincol = col;
                frame_new_width(topfr, width, false, false);
                redraw_all_later(CLEAR);
            }
        }
        else if (topfr.fr_layout == FR_ROW)
        {
            topfr.fr_width = width;
            topfr.fr_height = height;

            if (dir != 'v')                 /* equalize frame widths */
            {
                /* Compute the maximum number of windows horizontally in this frame. */
                int n = frame_minwidth(topfr, NOWIN);
                /* add one for the rightmost window, it doesn't have a separator */
                if (col + width == (int)Columns[0])
                    extra_sep = 1;
                else
                    extra_sep = 0;
                totwincount = (n + extra_sep) / ((int)p_wmw[0] + 1);
                has_next_curwin = frame_has_win(topfr, next_curwin);

                /*
                 * Compute width for "next_curwin" window and room available for other windows.
                 * "m" is the minimal width when counting "p_wiw" for "next_curwin".
                 */
                int m = frame_minwidth(topfr, next_curwin);
                room = width - m;
                if (room < 0)
                {
                    next_curwin_size = (int)p_wiw[0] + room;
                    room = 0;
                }
                else
                {
                    next_curwin_size = -1;
                    for (frame_C fr = topfr.fr_child; fr != null; fr = fr.fr_next)
                    {
                        /* If 'winfixwidth' set keep the window width if possible.
                         * Watch out for this window being the next_curwin. */
                        if (frame_fixed_width(fr))
                        {
                            n = frame_minwidth(fr, NOWIN);
                            int new_size = fr.fr_width;
                            if (frame_has_win(fr, next_curwin))
                            {
                                room += p_wiw[0] - p_wmw[0];
                                next_curwin_size = 0;
                                if (new_size < p_wiw[0])
                                    new_size = (int)p_wiw[0];
                            }
                            else
                                /* These windows don't use up room. */
                                totwincount -= (n + (fr.fr_next == null ? extra_sep : 0)) / (p_wmw[0] + 1);
                            room -= new_size - n;
                            if (room < 0)
                            {
                                new_size += room;
                                room = 0;
                            }
                            fr.fr_newwidth = new_size;
                        }
                    }
                    if (next_curwin_size == -1)
                    {
                        if (!has_next_curwin)
                            next_curwin_size = 0;
                        else if (1 < totwincount && p_wiw[0] < (room + (totwincount - 2)) / (totwincount - 1))
                        {
                            /* Can make all windows wider than 'winwidth', spread the room equally. */
                            next_curwin_size = (room + (int)p_wiw[0]
                                             + (totwincount - 1) * (int)p_wmw[0]
                                             + (totwincount - 1)) / totwincount;
                            room -= next_curwin_size - p_wiw[0];
                        }
                        else
                            next_curwin_size = (int)p_wiw[0];
                    }
                }

                if (has_next_curwin)
                    --totwincount;          /* don't count curwin */
            }

            for (frame_C fr = topfr.fr_child; fr != null; fr = fr.fr_next)
            {
                wincount = 1;
                int new_size;
                if (fr.fr_next == null)
                    /* last frame gets all that remains (avoid roundoff error) */
                    new_size = width;
                else if (dir == 'v')
                    new_size = fr.fr_width;
                else if (frame_fixed_width(fr))
                {
                    new_size = fr.fr_newwidth;
                    wincount = 0;       /* doesn't count as a sizeable window */
                }
                else
                {
                    /* Compute the maximum number of windows horiz. in "fr". */
                    int n = frame_minwidth(fr, NOWIN);
                    wincount = (n + (fr.fr_next == null ? extra_sep : 0)) / ((int)p_wmw[0] + 1);
                    int m = frame_minwidth(fr, next_curwin);
                    boolean hnc;
                    if (has_next_curwin)
                        hnc = frame_has_win(fr, next_curwin);
                    else
                        hnc = false;
                    if (hnc)            /* don't count next_curwin */
                        --wincount;
                    if (totwincount == 0)
                        new_size = room;
                    else
                        new_size = (wincount * room + (totwincount >>> 1)) / totwincount;
                    if (hnc)            /* add next_curwin size */
                    {
                        next_curwin_size -= p_wiw[0] - (m - n);
                        new_size += next_curwin_size;
                        room -= new_size - next_curwin_size;
                    }
                    else
                        room -= new_size;
                    new_size += n;
                }

                /* Skip frame that is full width when splitting or closing a window,
                 * unless equalizing all frames. */
                if (!current || dir != 'v'
                        || topfr.fr_parent != null || new_size != fr.fr_width || frame_has_win(fr, next_curwin))
                    win_equal_rec(next_curwin, current, fr, dir, col, row, new_size, height);
                col += new_size;
                width -= new_size;
                totwincount -= wincount;
            }
        }
        else /* topfr.fr_layout == FR_COL */
        {
            topfr.fr_width = width;
            topfr.fr_height = height;

            if (dir != 'h')                 /* equalize frame heights */
            {
                /* Compute maximum number of windows vertically in this frame. */
                int n = frame_minheight(topfr, NOWIN);
                /* add one for the bottom window if it doesn't have a statusline */
                if (row + height == cmdline_row && p_ls[0] == 0)
                    extra_sep = 1;
                else
                    extra_sep = 0;
                totwincount = (n + extra_sep) / ((int)p_wmh[0] + 1);
                has_next_curwin = frame_has_win(topfr, next_curwin);

                /*
                 * Compute height for "next_curwin" window and room available for other windows.
                 * "m" is the minimal height when counting "p_wh" for "next_curwin".
                 */
                int m = frame_minheight(topfr, next_curwin);
                room = height - m;
                if (room < 0)
                {
                    /* The room is less then 'winheight', use all space for the current window. */
                    next_curwin_size = (int)p_wh[0] + room;
                    room = 0;
                }
                else
                {
                    next_curwin_size = -1;
                    for (frame_C fr = topfr.fr_child; fr != null; fr = fr.fr_next)
                    {
                        /* If 'winfixheight' set keep the window height if possible.
                         * Watch out for this window being the next_curwin. */
                        if (frame_fixed_height(fr))
                        {
                            n = frame_minheight(fr, NOWIN);
                            int new_size = fr.fr_height;
                            if (frame_has_win(fr, next_curwin))
                            {
                                room += p_wh[0] - p_wmh[0];
                                next_curwin_size = 0;
                                if (new_size < p_wh[0])
                                    new_size = (int)p_wh[0];
                            }
                            else
                                /* These windows don't use up room. */
                                totwincount -= (n + (fr.fr_next == null ? extra_sep : 0)) / (p_wmh[0] + 1);
                            room -= new_size - n;
                            if (room < 0)
                            {
                                new_size += room;
                                room = 0;
                            }
                            fr.fr_newheight = new_size;
                        }
                    }
                    if (next_curwin_size == -1)
                    {
                        if (!has_next_curwin)
                            next_curwin_size = 0;
                        else if (1 < totwincount && p_wh[0] < (room + (totwincount - 2)) / (totwincount - 1))
                        {
                            /* Can make all windows higher than 'winheight', spread the room equally. */
                            next_curwin_size = (room + (int)p_wh[0]
                                             + (totwincount - 1) * (int)p_wmh[0]
                                             + (totwincount - 1)) / totwincount;
                            room -= next_curwin_size - p_wh[0];
                        }
                        else
                            next_curwin_size = (int)p_wh[0];
                    }
                }

                if (has_next_curwin)
                    --totwincount;          /* don't count curwin */
            }

            for (frame_C fr = topfr.fr_child; fr != null; fr = fr.fr_next)
            {
                wincount = 1;
                int new_size;
                if (fr.fr_next == null)
                    /* last frame gets all that remains (avoid roundoff error) */
                    new_size = height;
                else if (dir == 'h')
                    new_size = fr.fr_height;
                else if (frame_fixed_height(fr))
                {
                    new_size = fr.fr_newheight;
                    wincount = 0;       /* doesn't count as a sizeable window */
                }
                else
                {
                    /* Compute the maximum number of windows vert. in "fr". */
                    int n = frame_minheight(fr, NOWIN);
                    wincount = (n + (fr.fr_next == null ? extra_sep : 0)) / ((int)p_wmh[0] + 1);
                    int m = frame_minheight(fr, next_curwin);
                    boolean hnc;
                    if (has_next_curwin)
                        hnc = frame_has_win(fr, next_curwin);
                    else
                        hnc = false;
                    if (hnc)            /* don't count next_curwin */
                        --wincount;
                    if (totwincount == 0)
                        new_size = room;
                    else
                        new_size = (wincount * room + (totwincount >>> 1)) / totwincount;
                    if (hnc)            /* add next_curwin size */
                    {
                        next_curwin_size -= p_wh[0] - (m - n);
                        new_size += next_curwin_size;
                        room -= new_size - next_curwin_size;
                    }
                    else
                        room -= new_size;
                    new_size += n;
                }
                /* Skip frame that is full width when splitting or closing a window,
                 * unless equalizing all frames. */
                if (!current || dir != 'h' || topfr.fr_parent != null
                        || (new_size != fr.fr_height)
                        || frame_has_win(fr, next_curwin))
                    win_equal_rec(next_curwin, current, fr, dir, col, row, width, new_size);
                row += new_size;
                height -= new_size;
                totwincount -= wincount;
            }
        }
    }

    /*
     * close all windows for buffer 'buf'
     */
    /*private*/ static void close_windows(buffer_C buf, boolean keep_curwin)
        /* keep_curwin: don't close "curwin" */
    {
        int h = tabline_height();

        redrawingDisabled++;

        for (window_C wp = firstwin; wp != null && lastwin != firstwin; )
        {
            if (wp.w_buffer == buf && (!keep_curwin || wp != curwin) && !(wp.w_closing || wp.w_buffer.b_closing))
            {
                win_close(wp, false);

                /* Start all over, autocommands may change the window layout. */
                wp = firstwin;
            }
            else
                wp = wp.w_next;
        }

        /* Also check windows in other tab pages. */
        for (tabpage_C tp = first_tabpage, nexttp; tp != null; tp = nexttp)
        {
            nexttp = tp.tp_next;
            if (tp != curtab)
                for (window_C wp = tp.tp_firstwin; wp != null; wp = wp.w_next)
                    if (wp.w_buffer == buf && !(wp.w_closing || wp.w_buffer.b_closing))
                    {
                        win_close_othertab(wp, false, tp);

                        /* Start all over, the tab page may be closed and
                         * autocommands may change the window layout. */
                        nexttp = first_tabpage;
                        break;
                    }
        }

        --redrawingDisabled;

        redraw_tabline = true;
        if (h != tabline_height())
            shell_new_rows();
    }

    /*
     * Return true if the current window is the only window that exists (ignoring "aucmd_win").
     * Returns false if there is a window, possibly in another tab page.
     */
    /*private*/ static boolean last_window()
    {
        return (one_window() && first_tabpage.tp_next == null);
    }

    /*
     * Return true if there is only one window other than "aucmd_win" in the current tab page.
     */
    /*private*/ static boolean one_window()
    {
        boolean seen_one = false;

        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
        {
            if (wp != aucmd_win)
            {
                if (seen_one)
                    return false;
                seen_one = true;
            }
        }

        return true;
    }

    /*
     * Close the possibly last window in a tab page.
     * Returns true when the window was closed already.
     */
    /*private*/ static boolean close_last_window_tabpage(window_C win, boolean free_buf, tabpage_C prev_curtab)
    {
        if (firstwin == lastwin)
        {
            buffer_C old_curbuf = curbuf;

            /*
             * Closing the last window in a tab page.  First go to another tab page and then
             * close the window and the tab page.  This avoids that curwin and curtab are
             * invalid while we are freeing memory, they may be used in GUI events.
             * Don't trigger autocommands yet, they may use wrong values, so do that below.
             */
            goto_tabpage_tp(alt_tabpage(), false, true);
            redraw_tabline = true;

            /* Safety check:
             * autocommands may have closed the window when jumping to the other tab page. */
            if (valid_tabpage(prev_curtab) && prev_curtab.tp_firstwin == win)
            {
                int h = tabline_height();

                win_close_othertab(win, free_buf, prev_curtab);
                if (h != tabline_height())
                    shell_new_rows();
            }
            /* Since goto_tabpage_tp above did not trigger *Enter autocommands, do that now. */
            apply_autocmds(EVENT_WINENTER, null, null, false, curbuf);
            apply_autocmds(EVENT_TABENTER, null, null, false, curbuf);
            if (old_curbuf != curbuf)
                apply_autocmds(EVENT_BUFENTER, null, null, false, curbuf);
            return true;
        }
        return false;
    }

    /*
     * Close window "win".  Only works for the current tab page.
     * If "free_buf" is true related buffer may be unloaded.
     *
     * Called by :quit, :close, :xit, :wq and findtag().
     * Returns false when the window was not closed.
     */
    /*private*/ static boolean win_close(window_C win, boolean free_buf)
    {
        boolean other_buffer = false;
        boolean close_curwin = false;

        tabpage_C prev_curtab = curtab;

        if (last_window())
        {
            emsg(u8("E444: Cannot close last window"));
            return false;
        }

        if (win.w_closing || (win.w_buffer != null && win.w_buffer.b_closing))
            return false; /* window is already being closed */
        if (win == aucmd_win)
        {
            emsg(u8("E813: Cannot close autocmd window"));
            return false;
        }
        if ((firstwin == aucmd_win || lastwin == aucmd_win) && one_window())
        {
            emsg(u8("E814: Cannot close window, only autocmd window would remain"));
            return false;
        }

        /* When closing the last window in a tab page first go to another tab page
         * and then close the window and the tab page to avoid that curwin and
         * curtab are invalid while we are freeing memory. */
        if (close_last_window_tabpage(win, free_buf, prev_curtab))
            return false;

        if (win == curwin)
        {
            /*
             * Guess which window is going to be the new current window.
             * This may change because of the autocommands (sigh).
             */
            window_C wp = frame2win(win_altframe(win, null));

            /*
             * Be careful: If autocommands delete the window or cause this window
             * to be the last one left, return now.
             */
            if (wp.w_buffer != curbuf)
            {
                other_buffer = true;
                win.w_closing = true;
                apply_autocmds(EVENT_BUFLEAVE, null, null, false, curbuf);
                if (!win_valid(win))
                    return false;
                win.w_closing = false;
                if (last_window())
                    return false;
            }
            win.w_closing = true;
            apply_autocmds(EVENT_WINLEAVE, null, null, false, curbuf);
            if (!win_valid(win))
                return false;
            win.w_closing = false;
            if (last_window())
                return false;
            /* autocmds may abort script processing */
            if (aborting())
                return false;
        }

        /* Free independent synblock before the buffer is freed. */
        if (win.w_buffer != null)
            reset_synblock(win);

        /*
         * Close the link to the buffer.
         */
        if (win.w_buffer != null)
        {
            win.w_closing = true;
            close_buffer(win, win.w_buffer, free_buf ? DOBUF_UNLOAD : 0, true);
            if (win_valid(win))
                win.w_closing = false;
        }

        if (only_one_window() && win_valid(win) && win.w_buffer == null
                && (last_window() || curtab != prev_curtab
                    || close_last_window_tabpage(win, free_buf, prev_curtab)))
        {
            /* Autocommands have close all windows, quit now.
             * Restore curwin.w_buffer, otherwise writing viminfo may fail. */
            if (curwin.w_buffer == null)
                curwin.w_buffer = curbuf;
            getout(0);
        }

        /* Autocommands may have closed the window already,
         * or closed the only other window or moved to another tab page. */
        else if (!win_valid(win) || last_window() || curtab != prev_curtab
                || close_last_window_tabpage(win, free_buf, prev_curtab))
            return false;

        /* Free the memory used for the window and get the window that received the screen space. */
        int[] dir = new int[1];
        window_C wp = win_free_mem(win, dir, null);

        /* Make sure curwin isn't invalid.
         * It can cause severe trouble when printing an error message.
         * For win_equal() curbuf needs to be valid too. */
        if (win == curwin)
        {
            curwin = wp;
            curbuf = curwin.w_buffer;
            close_curwin = true;
        }
        if (p_ea[0] && (p_ead[0].at(0) == (byte)'b' || p_ead[0].at(0) == dir[0]))
            win_equal(curwin, true, dir[0]);
        else
            win_comp_pos();
        if (close_curwin)
        {
            win_enter_ext(wp, false, true, true, true);
            if (other_buffer)
                /* careful: after this wp and win may be invalid! */
                apply_autocmds(EVENT_BUFENTER, null, null, false, curbuf);
        }

        /*
         * If last window has a status line now and we don't want one,
         * remove the status line.
         */
        last_status(false);

        redraw_all_later(NOT_VALID);
        return true;
    }

    /*
     * Close window "win" in tab page "tp", which is not the current tab page.
     * This may be the last window in that tab page and result in closing the tab,
     * thus "tp" may become invalid!
     * Caller must check if buffer is hidden and whether the tabline needs to be updated.
     */
    /*private*/ static void win_close_othertab(window_C win, boolean free_buf, tabpage_C tp)
    {
        if (win.w_closing || win.w_buffer.b_closing)
            return; /* window is already being closed */

        /* Close the link to the buffer. */
        close_buffer(win, win.w_buffer, free_buf ? DOBUF_UNLOAD : 0, false);

        /* Careful: Autocommands may have closed the tab page or made it the current tab page. */
        tabpage_C ptp;
        for (ptp = first_tabpage; ptp != null && ptp != tp; ptp = ptp.tp_next)
            ;
        if (ptp == null || tp == curtab)
            return;

        /* Autocommands may have closed the window already. */
        window_C wp;
        for (wp = tp.tp_firstwin; wp != null && wp != win; wp = wp.w_next)
            ;
        if (wp == null)
            return;

        boolean free_tp = false;

        /* When closing the last window in a tab page remove the tab page. */
        if (tp == null ? firstwin == lastwin : tp.tp_firstwin == tp.tp_lastwin)
        {
            if (tp == first_tabpage)
                first_tabpage = tp.tp_next;
            else
            {
                for (ptp = first_tabpage; ptp != null && ptp.tp_next != tp; ptp = ptp.tp_next)
                    ;
                if (ptp == null)
                {
                    emsg2(e_intern2, u8("win_close_othertab()"));
                    return;
                }
                ptp.tp_next = tp.tp_next;
            }
            free_tp = true;
        }

        /* Free the memory used for the window. */
        int[] dir = new int[1];
        win_free_mem(win, dir, tp);

        if (free_tp)
            free_tabpage(tp);
    }

    /*
     * Free the memory used for a window.
     * Returns a pointer to the window that got the freed up space.
     */
    /*private*/ static window_C win_free_mem(window_C win, int[] dirp, tabpage_C tp)
        /* dirp: set to 'v' or 'h' for direction if 'ea' */
        /* tp: tab page "win" is in, null for current */
    {
        /* Remove the window and its frame from the tree of frames. */
        frame_C frp = win.w_frame;
        window_C wp = winframe_remove(win, dirp, tp);
        win_free(win, tp);

        /* When deleting the current window of another tab page select a new current window. */
        if (tp != null && win == tp.tp_curwin)
            tp.tp_curwin = wp;

        return wp;
    }

    /*
     * Remove a window and its frame from the tree of frames.
     * Returns a pointer to the window that got the freed up space.
     */
    /*private*/ static window_C winframe_remove(window_C win, int[] dirp, tabpage_C tp)
        /* dirp: set to 'v' or 'h' for direction if 'ea' */
        /* tp: tab page "win" is in, null for current */
    {
        frame_C frp_close = win.w_frame;

        /*
         * If there is only one window there is nothing to remove.
         */
        if (tp == null ? firstwin == lastwin : tp.tp_firstwin == tp.tp_lastwin)
            return null;

        /*
         * Remove the window from its frame.
         */
        frame_C frp2 = win_altframe(win, tp);
        window_C wp = frame2win(frp2);

        /* Remove this frame from the list of frames. */
        frame_remove(frp_close);

        if (frp_close.fr_parent.fr_layout == FR_COL)
        {
            /* When 'winfixheight' is set, try to find another frame in the column
             * (as close to the closed frame as possible) to distribute the height to. */
            if (frp2.fr_win != null && frp2.fr_win.w_onebuf_opt.wo_wfh[0])
            {
                frame_C frp = frp_close.fr_prev;
                frame_C frp3 = frp_close.fr_next;
                while (frp != null || frp3 != null)
                {
                    if (frp != null)
                    {
                        if (frp.fr_win != null && !frp.fr_win.w_onebuf_opt.wo_wfh[0])
                        {
                            frp2 = frp;
                            wp = frp.fr_win;
                            break;
                        }
                        frp = frp.fr_prev;
                    }
                    if (frp3 != null)
                    {
                        if (frp3.fr_win != null && !frp3.fr_win.w_onebuf_opt.wo_wfh[0])
                        {
                            frp2 = frp3;
                            wp = frp3.fr_win;
                            break;
                        }
                        frp3 = frp3.fr_next;
                    }
                }
            }
            frame_new_height(frp2, frp2.fr_height + frp_close.fr_height,
                                (frp2 == frp_close.fr_next) ? true : false, false);
            dirp[0] = 'v';
        }
        else
        {
            /* When 'winfixwidth' is set, try to find another frame in the column
             * (as close to the closed frame as possible) to distribute the width to. */
            if (frp2.fr_win != null && frp2.fr_win.w_onebuf_opt.wo_wfw[0])
            {
                frame_C frp = frp_close.fr_prev;
                frame_C frp3 = frp_close.fr_next;
                while (frp != null || frp3 != null)
                {
                    if (frp != null)
                    {
                        if (frp.fr_win != null && !frp.fr_win.w_onebuf_opt.wo_wfw[0])
                        {
                            frp2 = frp;
                            wp = frp.fr_win;
                            break;
                        }
                        frp = frp.fr_prev;
                    }
                    if (frp3 != null)
                    {
                        if (frp3.fr_win != null && !frp3.fr_win.w_onebuf_opt.wo_wfw[0])
                        {
                            frp2 = frp3;
                            wp = frp3.fr_win;
                            break;
                        }
                        frp3 = frp3.fr_next;
                    }
                }
            }
            frame_new_width(frp2, frp2.fr_width + frp_close.fr_width, (frp2 == frp_close.fr_next), false);
            dirp[0] = 'h';
        }

        /* If rows/columns go to a window below/right its positions need to be updated.
         * Can only be done after the sizes have been updated. */
        if (frp2 == frp_close.fr_next)
        {
            int[] row = { win.w_winrow };
            int[] col = { win.w_wincol };

            frame_comp_pos(frp2, row, col);
        }

        if (frp2.fr_next == null && frp2.fr_prev == null)
        {
            /* There is no other frame in this list, move its info to the parent and remove it. */
            frp2.fr_parent.fr_layout = frp2.fr_layout;
            frp2.fr_parent.fr_child = frp2.fr_child;
            for (frame_C frp = frp2.fr_child; frp != null; frp = frp.fr_next)
                frp.fr_parent = frp2.fr_parent;
            frp2.fr_parent.fr_win = frp2.fr_win;
            if (frp2.fr_win != null)
                frp2.fr_win.w_frame = frp2.fr_parent;
            frame_C frp = frp2.fr_parent;

            frp2 = frp.fr_parent;
            if (frp2 != null && frp2.fr_layout == frp.fr_layout)
            {
                /* The frame above the parent has the same layout,
                 * have to merge the frames into this list. */
                if (frp2.fr_child == frp)
                    frp2.fr_child = frp.fr_child;
                frp.fr_child.fr_prev = frp.fr_prev;
                if (frp.fr_prev != null)
                    frp.fr_prev.fr_next = frp.fr_child;
                for (frame_C frp3 = frp.fr_child; ; frp3 = frp3.fr_next)
                {
                    frp3.fr_parent = frp2;
                    if (frp3.fr_next == null)
                    {
                        frp3.fr_next = frp.fr_next;
                        if (frp.fr_next != null)
                            frp.fr_next.fr_prev = frp3;
                        break;
                    }
                }
            }
        }

        return wp;
    }

    /*
     * Find out which frame is going to get the freed up space when "win" is closed.
     * if 'splitbelow'/'splitleft' the space goes to the window above/left.
     * if 'nosplitbelow'/'nosplitleft' the space goes to the window below/right.
     * This makes opening a window and closing it immediately keep the same window layout.
     */
    /*private*/ static frame_C win_altframe(window_C win, tabpage_C tp)
        /* tp: tab page "win" is in, null for current */
    {
        if (tp == null ? firstwin == lastwin : tp.tp_firstwin == tp.tp_lastwin)
            /* Last window in this tab page, will go to next tab page. */
            return alt_tabpage().tp_curwin.w_frame;

        frame_C frp = win.w_frame;
        boolean b;
        if (frp.fr_parent != null && frp.fr_parent.fr_layout == FR_ROW)
            b = p_spr[0];
        else
            b = p_sb[0];
        if ((!b && frp.fr_next != null) || frp.fr_prev == null)
            return frp.fr_next;

        return frp.fr_prev;
    }

    /*
     * Return the tabpage that will be used if the current one is closed.
     */
    /*private*/ static tabpage_C alt_tabpage()
    {
        /* Use the next tab page if possible. */
        if (curtab.tp_next != null)
            return curtab.tp_next;

        tabpage_C tp;
        /* Find the last but one tab page. */
        for (tp = first_tabpage; tp.tp_next != curtab; tp = tp.tp_next)
            ;
        return tp;
    }

    /*
     * Find the left-upper window in frame "frp".
     */
    /*private*/ static window_C frame2win(frame_C frp)
    {
        while (frp.fr_win == null)
            frp = frp.fr_child;
        return frp.fr_win;
    }

    /*
     * Return true if frame "frp" contains window "wp".
     */
    /*private*/ static boolean frame_has_win(frame_C frp, window_C wp)
    {
        if (frp.fr_layout == FR_LEAF)
            return (frp.fr_win == wp);

        for (frame_C p = frp.fr_child; p != null; p = p.fr_next)
            if (frame_has_win(p, wp))
                return true;

        return false;
    }

    /*
     * Set a new height for a frame.  Recursively sets the height for contained
     * frames and windows.  Caller must take care of positions.
     */
    /*private*/ static void frame_new_height(frame_C topfrp, int height, boolean topfirst, boolean wfh)
        /* topfirst: resize topmost contained frame first */
        /* wfh: obey 'winfixheight' when there is a choice; may cause the height not to be set */
    {
        if (topfrp.fr_win != null)
        {
            /* Simple case: just one window. */
            win_new_height(topfrp.fr_win, height - topfrp.fr_win.w_status_height);
        }
        else if (topfrp.fr_layout == FR_ROW)
        {
            frame_C frp;
            do
            {
                /* All frames in this row get the same new height. */
                for (frp = topfrp.fr_child; frp != null; frp = frp.fr_next)
                {
                    frame_new_height(frp, height, topfirst, wfh);
                    if (height < frp.fr_height)
                    {
                        /* Could not fit the windows, make the whole row higher. */
                        height = frp.fr_height;
                        break;
                    }
                }
            } while (frp != null);
        }
        else    /* fr_layout == FR_COL */
        {
            /* Complicated case: resize a column of frames.
             * Resize the bottom frame first, frames above that when needed.
             */
            frame_C frp = topfrp.fr_child;
            if (wfh)
                /* Advance past frames with one window with 'wfh' set. */
                while (frame_fixed_height(frp))
                {
                    frp = frp.fr_next;
                    if (frp == null)
                        return;         /* no frame without 'wfh', give up */
                }
            if (!topfirst)
            {
                /* Find the bottom frame of this column. */
                while (frp.fr_next != null)
                    frp = frp.fr_next;
                if (wfh)
                    /* Advance back for frames with one window with 'wfh' set. */
                    while (frame_fixed_height(frp))
                        frp = frp.fr_prev;
            }

            int extra_lines = height - topfrp.fr_height;
            if (extra_lines < 0)
            {
                /* reduce height of contained frames, bottom or top frame first */
                while (frp != null)
                {
                    int h = frame_minheight(frp, null);
                    if (frp.fr_height + extra_lines < h)
                    {
                        extra_lines += frp.fr_height - h;
                        frame_new_height(frp, h, topfirst, wfh);
                    }
                    else
                    {
                        frame_new_height(frp, frp.fr_height + extra_lines, topfirst, wfh);
                        break;
                    }
                    if (topfirst)
                    {
                        do
                        {
                            frp = frp.fr_next;
                        } while (wfh && frp != null && frame_fixed_height(frp));
                    }
                    else
                    {
                        do
                        {
                            frp = frp.fr_prev;
                        } while (wfh && frp != null && frame_fixed_height(frp));
                    }
                    /* Increase "height" if we could not reduce enough frames. */
                    if (frp == null)
                        height -= extra_lines;
                }
            }
            else if (0 < extra_lines)
            {
                /* increase height of bottom or top frame */
                frame_new_height(frp, frp.fr_height + extra_lines, topfirst, wfh);
            }
        }
        topfrp.fr_height = height;
    }

    /*
     * Return true if height of frame "frp" should not be changed because of
     * the 'winfixheight' option.
     */
    /*private*/ static boolean frame_fixed_height(frame_C frp)
    {
        /* frame with one window: fixed height if 'winfixheight' set. */
        if (frp.fr_win != null)
            return frp.fr_win.w_onebuf_opt.wo_wfh[0];

        if (frp.fr_layout == FR_ROW)
        {
            /* The frame is fixed height if one of the frames in the row is fixed height. */
            for (frp = frp.fr_child; frp != null; frp = frp.fr_next)
                if (frame_fixed_height(frp))
                    return true;

            return false;
        }

        /* frp.fr_layout == FR_COL: the frame is fixed height
         * if all of the frames in the row are fixed height. */
        for (frp = frp.fr_child; frp != null; frp = frp.fr_next)
            if (!frame_fixed_height(frp))
                return false;

        return true;
    }

    /*
     * Return true if width of frame "frp" should not be changed
     * because of the 'winfixwidth' option.
     */
    /*private*/ static boolean frame_fixed_width(frame_C frp)
    {
        /* frame with one window: fixed width if 'winfixwidth' set. */
        if (frp.fr_win != null)
            return frp.fr_win.w_onebuf_opt.wo_wfw[0];

        if (frp.fr_layout == FR_COL)
        {
            /* The frame is fixed width if one of the frames in the row is fixed width. */
            for (frp = frp.fr_child; frp != null; frp = frp.fr_next)
                if (frame_fixed_width(frp))
                    return true;

            return false;
        }

        /* frp.fr_layout == FR_ROW: the frame is fixed width
         * if all of the frames in the row are fixed width. */
        for (frp = frp.fr_child; frp != null; frp = frp.fr_next)
            if (!frame_fixed_width(frp))
                return false;

        return true;
    }

    /*
     * Add a status line to windows at the bottom of "frp".
     * Note: Does not check if there is room!
     */
    /*private*/ static void frame_add_statusline(frame_C frp)
    {
        if (frp.fr_layout == FR_LEAF)
        {
            window_C wp = frp.fr_win;
            if (wp.w_status_height == 0)
            {
                if (0 < wp.w_height)    /* don't make it negative */
                    --wp.w_height;
                wp.w_status_height = STATUS_HEIGHT;
            }
        }
        else if (frp.fr_layout == FR_ROW)
        {
            /* Handle all the frames in the row. */
            for (frp = frp.fr_child; frp != null; frp = frp.fr_next)
                frame_add_statusline(frp);
        }
        else /* frp.fr_layout == FR_COL */
        {
            /* Only need to handle the last frame in the column. */
            for (frp = frp.fr_child; frp.fr_next != null; frp = frp.fr_next)
                ;
            frame_add_statusline(frp);
        }
    }

    /*
     * Set width of a frame.  Handles recursively going through contained frames.
     * May remove separator line for windows at the right side (for win_close()).
     */
    /*private*/ static void frame_new_width(frame_C topfrp, int width, boolean leftfirst, boolean wfw)
        /* leftfirst: resize leftmost contained frame first */
        /* wfw: obey 'winfixwidth' when there is a choice; may cause the width not to be set */
    {
        if (topfrp.fr_layout == FR_LEAF)
        {
            /* Simple case: just one window. */
            window_C wp = topfrp.fr_win;
            /* Find out if there are any windows right of this one. */
            frame_C frp;
            for (frp = topfrp; frp.fr_parent != null; frp = frp.fr_parent)
                if (frp.fr_parent.fr_layout == FR_ROW && frp.fr_next != null)
                    break;
            if (frp.fr_parent == null)
                wp.w_vsep_width = 0;
            win_new_width(wp, width - wp.w_vsep_width);
        }
        else if (topfrp.fr_layout == FR_COL)
        {
            frame_C frp;
            do
            {
                /* All frames in this column get the same new width. */
                for (frp = topfrp.fr_child; frp != null; frp = frp.fr_next)
                {
                    frame_new_width(frp, width, leftfirst, wfw);
                    if (width < frp.fr_width)
                    {
                        /* Could not fit the windows, make whole column wider. */
                        width = frp.fr_width;
                        break;
                    }
                }
            } while (frp != null);
        }
        else    /* fr_layout == FR_ROW */
        {
            /* Complicated case: resize a row of frames.
             * Resize the rightmost frame first, frames left of it when needed.
             */
            frame_C frp = topfrp.fr_child;
            if (wfw)
                /* Advance past frames with one window with 'wfw' set. */
                while (frame_fixed_width(frp))
                {
                    frp = frp.fr_next;
                    if (frp == null)
                        return;         /* no frame without 'wfw', give up */
                }
            if (!leftfirst)
            {
                /* Find the rightmost frame of this row. */
                while (frp.fr_next != null)
                    frp = frp.fr_next;
                if (wfw)
                    /* Advance back for frames with one window with 'wfw' set. */
                    while (frame_fixed_width(frp))
                        frp = frp.fr_prev;
            }

            int extra_cols = width - topfrp.fr_width;
            if (extra_cols < 0)
            {
                /* reduce frame width, rightmost frame first */
                while (frp != null)
                {
                    int w = frame_minwidth(frp, null);
                    if (frp.fr_width + extra_cols < w)
                    {
                        extra_cols += frp.fr_width - w;
                        frame_new_width(frp, w, leftfirst, wfw);
                    }
                    else
                    {
                        frame_new_width(frp, frp.fr_width + extra_cols, leftfirst, wfw);
                        break;
                    }
                    if (leftfirst)
                    {
                        do
                        {
                            frp = frp.fr_next;
                        } while (wfw && frp != null && frame_fixed_width(frp));
                    }
                    else
                    {
                        do
                        {
                            frp = frp.fr_prev;
                        } while (wfw && frp != null && frame_fixed_width(frp));
                    }
                    /* Increase "width" if we could not reduce enough frames. */
                    if (frp == null)
                        width -= extra_cols;
                }
            }
            else if (0 < extra_cols)
            {
                /* increase width of rightmost frame */
                frame_new_width(frp, frp.fr_width + extra_cols, leftfirst, wfw);
            }
        }
        topfrp.fr_width = width;
    }

    /*
     * Add the vertical separator to windows at the right side of "frp".
     * Note: Does not check if there is room!
     */
    /*private*/ static void frame_add_vsep(frame_C frp)
    {
        if (frp.fr_layout == FR_LEAF)
        {
            window_C wp = frp.fr_win;
            if (wp.w_vsep_width == 0)
            {
                if (0 < wp.w_width)     /* don't make it negative */
                    --wp.w_width;
                wp.w_vsep_width = 1;
            }
        }
        else if (frp.fr_layout == FR_COL)
        {
            /* Handle all the frames in the column. */
            for (frp = frp.fr_child; frp != null; frp = frp.fr_next)
                frame_add_vsep(frp);
        }
        else /* frp.fr_layout == FR_ROW */
        {
            /* Only need to handle the last frame in the row. */
            frp = frp.fr_child;
            while (frp.fr_next != null)
                frp = frp.fr_next;
            frame_add_vsep(frp);
        }
    }

    /*
     * Set frame width from the window it contains.
     */
    /*private*/ static void frame_fix_width(window_C wp)
    {
        wp.w_frame.fr_width = wp.w_width + wp.w_vsep_width;
    }

    /*
     * Set frame height from the window it contains.
     */
    /*private*/ static void frame_fix_height(window_C wp)
    {
        wp.w_frame.fr_height = wp.w_height + wp.w_status_height;
    }

    /*
     * Compute the minimal height for frame "topfrp".
     * Uses the 'winminheight' option.
     * When "next_curwin" isn't null, use "p_wh" for this window.
     * When "next_curwin" is NOWIN, don't use at least one line for the current window.
     */
    /*private*/ static int frame_minheight(frame_C topfrp, window_C next_curwin)
    {
        int m;

        if (topfrp.fr_win != null)
        {
            if (topfrp.fr_win == next_curwin)
                m = (int)p_wh[0] + topfrp.fr_win.w_status_height;
            else
            {
                /* window: minimal height of the window plus status line */
                m = (int)p_wmh[0] + topfrp.fr_win.w_status_height;
                /* Current window is minimal one line high. */
                if (p_wmh[0] == 0 && topfrp.fr_win == curwin && next_curwin == null)
                    m++;
            }
        }
        else if (topfrp.fr_layout == FR_ROW)
        {
            /* get the minimal height from each frame in this row */
            m = 0;
            for (frame_C frp = topfrp.fr_child; frp != null; frp = frp.fr_next)
            {
                int n = frame_minheight(frp, next_curwin);
                if (m < n)
                    m = n;
            }
        }
        else
        {
            /* Add up the minimal heights for all frames in this column. */
            m = 0;
            for (frame_C frp = topfrp.fr_child; frp != null; frp = frp.fr_next)
                m += frame_minheight(frp, next_curwin);
        }

        return m;
    }

    /*
     * Compute the minimal width for frame "topfrp".
     * When "next_curwin" isn't null, use "p_wiw" for this window.
     * When "next_curwin" is NOWIN, don't use at least one column for the current window.
     */
    /*private*/ static int frame_minwidth(frame_C topfrp, window_C next_curwin)
        /* next_curwin: use "p_wh" and "p_wiw" for next_curwin */
    {
        int m;

        if (topfrp.fr_win != null)
        {
            if (topfrp.fr_win == next_curwin)
                m = (int)p_wiw[0] + topfrp.fr_win.w_vsep_width;
            else
            {
                /* window: minimal width of the window plus separator column */
                m = (int)p_wmw[0] + topfrp.fr_win.w_vsep_width;
                /* Current window is minimal one column wide. */
                if (p_wmw[0] == 0 && topfrp.fr_win == curwin && next_curwin == null)
                    m++;
            }
        }
        else if (topfrp.fr_layout == FR_COL)
        {
            /* get the minimal width from each frame in this column */
            m = 0;
            for (frame_C frp = topfrp.fr_child; frp != null; frp = frp.fr_next)
            {
                int n = frame_minwidth(frp, next_curwin);
                if (m < n)
                    m = n;
            }
        }
        else
        {
            /* Add up the minimal widths for all frames in this row. */
            m = 0;
            for (frame_C frp = topfrp.fr_child; frp != null; frp = frp.fr_next)
                m += frame_minwidth(frp, next_curwin);
        }

        return m;
    }

    /*
     * Try to close all windows except current one.
     * Buffers in the other windows become hidden if 'hidden' is set, or '!' is
     * used and the buffer was modified.
     *
     * Used by ":bdel" and ":only".
     */
    /*private*/ static void close_others(boolean message, boolean forceit)
        /* forceit: always hide all other windows */
    {
        if (one_window())
        {
            if (message && !autocmd_busy)
                msg(m_onlyone);
            return;
        }

        /* Be very careful here: autocommands may change the window layout. */
        for (window_C wp = firstwin, nextwp; win_valid(wp); wp = nextwp)
        {
            nextwp = wp.w_next;
            if (wp != curwin)               /* don't close current window */
            {
                /* Check if it's allowed to abandon this window. */
                boolean r = can_abandon(wp.w_buffer, forceit);
                if (!win_valid(wp))         /* autocommands messed wp up */
                {
                    nextwp = firstwin;
                    continue;
                }
                if (!r)
                {
                    if (message && (p_confirm[0] || cmdmod.confirm) && p_write[0])
                    {
                        dialog_changed(wp.w_buffer, false);
                        if (!win_valid(wp))         /* autocommands messed wp up */
                        {
                            nextwp = firstwin;
                            continue;
                        }
                    }
                    if (bufIsChanged(wp.w_buffer))
                        continue;
                }
                win_close(wp, !P_HID(wp.w_buffer) && !bufIsChanged(wp.w_buffer));
            }
        }

        if (message && lastwin != firstwin)
            emsg(u8("E445: Other window contains changes"));
    }

    /*
     * Init the current window "curwin".
     * Called when a new file is being edited.
     */
    /*private*/ static void curwin_init()
    {
        win_init_empty(curwin);
    }

    /*private*/ static void win_init_empty(window_C wp)
    {
        redraw_win_later(wp, NOT_VALID);

        wp.w_lines_valid = 0;
        wp.w_cursor.lnum = 1;
        wp.w_curswant = wp.w_cursor.col = 0;
        wp.w_cursor.coladd = 0;
        wp.w_pcmark.lnum = 1;       /* pcmark not cleared but set to line 1 */
        wp.w_pcmark.col = 0;
        wp.w_prev_pcmark.lnum = 0;
        wp.w_prev_pcmark.col = 0;
        wp.w_topline = 1;
        wp.w_botline = 2;
        wp.w_s = wp.w_buffer.b_s;
    }

    /*
     * Allocate the first window and put an empty buffer in it.
     * Called from main().
     * Return false when something goes wrong (out of memory).
     */
    /*private*/ static boolean win_alloc_first()
    {
        if (win_alloc_firstwin(null) == false)
            return false;

        first_tabpage = newTabpage();
        first_tabpage.tp_topframe = topframe;
        curtab = first_tabpage;

        return true;
    }

    /*
     * Init "aucmd_win".  This can only be done after the first
     * window is fully initialized, thus it can't be in win_alloc_first().
     */
    /*private*/ static void win_alloc_aucmd_win()
    {
        aucmd_win = newWindow(null, true);

        win_init_some(aucmd_win, curwin);
        aucmd_win.w_onebuf_opt.wo_scb[0] = false;
        aucmd_win.w_onebuf_opt.wo_crb[0] = false;
        aucmd_win.w_frame = newFrame(aucmd_win);
    }

    /*
     * Allocate the first window or the first window in a new tab page.
     * When "oldwin" is null create an empty buffer for it.
     * When "oldwin" is not null copy info from it to the new window.
     * Return false when something goes wrong (out of memory).
     */
    /*private*/ static boolean win_alloc_firstwin(window_C oldwin)
    {
        curwin = newWindow(null, false);

        if (oldwin == null)
        {
            /* Very first window,
             * need to create an empty buffer for it and initialize from scratch. */
            curbuf = buflist_new(null, null, 1L, BLN_LISTED);
            if (curbuf == null)
                return false;

            curwin.w_buffer = curbuf;
            curwin.w_s = curbuf.b_s;
            curbuf.b_nwindows = 1;          /* there is one window */
            curwin.w_alist = global_alist;
            curwin_init();                  /* init current window */
        }
        else
        {
            /* First window in new tab page, initialize it from "oldwin". */
            win_init(curwin, oldwin, 0);

            /* We don't want cursor- and scroll-binding in the first window. */
            curwin.w_onebuf_opt.wo_scb[0] = false;
            curwin.w_onebuf_opt.wo_crb[0] = false;
        }

        curwin.w_frame = newFrame(curwin);

        topframe = curwin.w_frame;
        topframe.fr_width = (int)Columns[0];
        topframe.fr_height = (int)(Rows[0] - p_ch[0]);
        topframe.fr_win = curwin;

        return true;
    }

    /*
     * Create a frame for window "wp".
     */
    /*private*/ static frame_C newFrame(window_C wp)
    {
        frame_C frp = new frame_C();

        frp.fr_layout = FR_LEAF;
        frp.fr_win = wp;

        return frp;
    }

    /*
     * Initialize the window and frame size to the maximum.
     */
    /*private*/ static void win_init_size()
    {
        long rows_avail = Rows[0] - p_ch[0] - tabline_height();

        firstwin.w_height = (int)rows_avail;
        topframe.fr_height = (int)rows_avail;
        firstwin.w_width = (int)Columns[0];
        topframe.fr_width = (int)Columns[0];
    }

    /*
     * Allocate a new tabpage_C and init the values.
     */
    /*private*/ static tabpage_C newTabpage()
    {
        tabpage_C tp = new tabpage_C();

        /* init t: variables */
        tp.tp_vars = newDict();
        init_var_dict(tp.tp_vars, tp.tp_winvar, VAR_SCOPE);

        tp.tp_ch_used = p_ch[0];

        return tp;
    }

    /*private*/ static void free_tabpage(tabpage_C tp)
    {
        for (int i = 0; i < SNAP_COUNT; i++)
            clear_snapshot(tp, i);
        vars_clear(tp.tp_vars.dv_hashtab);      /* free all t: variables */
        hash_init(tp.tp_vars.dv_hashtab);
        unref_var_dict(tp.tp_vars);
    }

    /*
     * Create a new Tab page with one window.
     * It will edit the current buffer, like after ":split".
     * When "after" is 0 put it just after the current Tab page.
     * Otherwise put it just before tab page "after".
     * Return false or true.
     */
    /*private*/ static boolean win_new_tabpage(int after)
    {
        tabpage_C tp = curtab;

        tabpage_C newtp = newTabpage();

        /* Remember the current windows in this Tab page. */
        if (leave_tabpage(curbuf, true) == false)
            return false;

        curtab = newtp;

        /* Create a new empty window. */
        if (win_alloc_firstwin(tp.tp_curwin) == true)
        {
            /* Make the new Tab page the new topframe. */
            if (after == 1)
            {
                /* New tab page becomes the first one. */
                newtp.tp_next = first_tabpage;
                first_tabpage = newtp;
            }
            else
            {
                if (0 < after)
                {
                    /* Put new tab page before tab page "after". */
                    int n = 2;
                    for (tp = first_tabpage; tp.tp_next != null && n < after; tp = tp.tp_next)
                        n++;
                }
                newtp.tp_next = tp.tp_next;
                tp.tp_next = newtp;
            }
            win_init_size();
            firstwin.w_winrow = tabline_height();
            win_comp_scroll(curwin);

            newtp.tp_topframe = topframe;
            last_status(false);

            redraw_all_later(CLEAR);
            apply_autocmds(EVENT_WINENTER, null, null, false, curbuf);
            apply_autocmds(EVENT_TABENTER, null, null, false, curbuf);
            return true;
        }

        /* Failed, get back the previous Tab page. */
        enter_tabpage(curtab, curbuf, true, true);
        return false;
    }

    /*
     * Open a new tab page if ":tab cmd" was used.  It will edit the same buffer,
     * like with ":split".
     * Returns true if a new tab page was created, false otherwise.
     */
    /*private*/ static boolean may_open_tabpage()
    {
        int n = (cmdmod.tab == 0) ? postponed_split_tab : cmdmod.tab;
        if (n != 0)
        {
            cmdmod.tab = 0;     /* reset it to avoid doing it twice */
            postponed_split_tab = 0;
            return win_new_tabpage(n);
        }

        return false;
    }

    /*
     * Create up to "maxcount" tabpages with empty windows.
     * Returns the number of resulting tab pages.
     */
    /*private*/ static int make_tabpages(int maxcount)
    {
        int count = maxcount;

        /* Limit to 'tabpagemax' tabs. */
        if (p_tpm[0] < count)
            count = (int)p_tpm[0];

        /*
         * Don't execute autocommands while creating the tab pages.
         * Must do that when putting the buffers in the windows.
         */
        block_autocmds();

        int todo;

        for (todo = count - 1; 0 < todo; --todo)
            if (win_new_tabpage(0) == false)
                break;

        unblock_autocmds();

        /* return actual number of tab pages */
        return (count - todo);
    }

    /*
     * Return true when "tpc" points to a valid tab page.
     */
    /*private*/ static boolean valid_tabpage(tabpage_C tpc)
    {
        tabpage_C tp;

        for (tp = first_tabpage; tp != null; tp = tp.tp_next)
            if (tp == tpc)
                return true;

        return false;
    }

    /*
     * Find tab page "n" (first one is 1).  Returns null when not found.
     */
    /*private*/ static tabpage_C find_tabpage(int n)
    {
        tabpage_C tp;
        int i = 1;

        for (tp = first_tabpage; tp != null && i != n; tp = tp.tp_next)
            i++;

        return tp;
    }

    /*
     * Get index of tab page "tp".  First one has index 1.
     * When not found returns number of tab pages plus one.
     */
    /*private*/ static int tabpage_index(tabpage_C ftp)
    {
        tabpage_C tp;
        int i = 1;

        for (tp = first_tabpage; tp != null && tp != ftp; tp = tp.tp_next)
            i++;

        return i;
    }

    /*
     * Prepare for leaving the current tab page.
     * When autocommands change "curtab" we don't leave the tab page and return false.
     * Careful: When true is returned need to get a new tab page very very soon!
     */
    /*private*/ static boolean leave_tabpage(buffer_C new_curbuf, boolean trigger_leave_autocmds)
        /* new_curbuf: what is going to be the new curbuf, null if unknown */
    {
        tabpage_C tp = curtab;

        reset_VIsual_and_resel();   /* stop Visual mode */

        if (trigger_leave_autocmds)
        {
            if (new_curbuf != curbuf)
            {
                apply_autocmds(EVENT_BUFLEAVE, null, null, false, curbuf);
                if (curtab != tp)
                    return false;
            }
            apply_autocmds(EVENT_WINLEAVE, null, null, false, curbuf);
            if (curtab != tp)
                return false;
            apply_autocmds(EVENT_TABLEAVE, null, null, false, curbuf);
            if (curtab != tp)
                return false;
        }

        tp.tp_curwin = curwin;
        tp.tp_prevwin = prevwin;
        tp.tp_firstwin = firstwin;
        tp.tp_lastwin = lastwin;
        tp.tp_old_Rows = Rows[0];
        tp.tp_old_Columns = Columns[0];

        firstwin = null;
        lastwin = null;

        return true;
    }

    /*
     * Start using tab page "tp".
     * Only to be used after leave_tabpage() or freeing the current tab page.
     * Only trigger *Enter autocommands when trigger_enter_autocmds is true.
     * Only trigger *Leave autocommands when trigger_leave_autocmds is true.
     */
    /*private*/ static void enter_tabpage(tabpage_C tp, buffer_C old_curbuf, boolean trigger_enter_autocmds, boolean trigger_leave_autocmds)
    {
        int old_off = tp.tp_firstwin.w_winrow;
        window_C next_prevwin = tp.tp_prevwin;

        curtab = tp;
        firstwin = tp.tp_firstwin;
        lastwin = tp.tp_lastwin;
        topframe = tp.tp_topframe;

        /* We would like doing the TabEnter event first, but we don't have a
         * valid current window yet, which may break some commands.
         * This triggers autocommands, thus may make "tp" invalid. */
        win_enter_ext(tp.tp_curwin, false, true, trigger_enter_autocmds, trigger_leave_autocmds);
        prevwin = next_prevwin;

        last_status(false);         /* status line may appear or disappear */
        win_comp_pos();             /* recompute w_winrow for all windows */
        must_redraw = CLEAR;        /* need to redraw everything */

        /* The tabpage line may have appeared or disappeared, may need to resize
         * the frames for that.  When the Vim window was resized need to update
         * frame sizes too.  Use the stored value of "p_ch", so that it can be
         * different for each tab page. */
        p_ch[0] = curtab.tp_ch_used;
        if (curtab.tp_old_Rows != Rows[0] || (old_off != firstwin.w_winrow))
            shell_new_rows();
        if (curtab.tp_old_Columns != Columns[0] && starting == 0)
            shell_new_columns();    /* update window widths */

        /* Apply autocommands after updating the display,
         * when 'rows' and 'columns' have been set correctly. */
        if (trigger_enter_autocmds)
        {
            apply_autocmds(EVENT_TABENTER, null, null, false, curbuf);
            if (old_curbuf != curbuf)
                apply_autocmds(EVENT_BUFENTER, null, null, false, curbuf);
        }

        redraw_all_later(CLEAR);
    }

    /*
     * Go to tab page "n".  For ":tab N" and "Ngt".
     * When "n" is 9999 go to the last tab page.
     */
    /*private*/ static void goto_tabpage(int n)
    {
        if (text_locked())
        {
            /* Not allowed when editing the command line. */
            if (cmdwin_type != 0)
                emsg(e_cmdwin);
            else
                emsg(e_secure);
            return;
        }

        /* If there is only one it can't work. */
        if (first_tabpage.tp_next == null)
        {
            if (1 < n)
                beep_flush();
            return;
        }

        tabpage_C tp = null;	// %% anno dunno

        if (n == 0)
        {
            /* No count, go to next tab page, wrap around end. */
            if (curtab.tp_next == null)
                tp = first_tabpage;
            else
                tp = curtab.tp_next;
        }
        else if (n < 0)
        {
            /* "gT": go to previous tab page, wrap around end.  "N gT" repeats this N times. */
            tabpage_C ttp = curtab;
            for (int i = n; i < 0; i++)
            {
                for (tp = first_tabpage; tp.tp_next != ttp && tp.tp_next != null; tp = tp.tp_next)
                    ;
                ttp = tp;
            }
        }
        else if (n == 9999)
        {
            /* Go to last tab page. */
            for (tp = first_tabpage; tp.tp_next != null; tp = tp.tp_next)
                ;
        }
        else
        {
            /* Go to tab page "n". */
            tp = find_tabpage(n);
            if (tp == null)
            {
                beep_flush();
                return;
            }
        }

        goto_tabpage_tp(tp, true, true);
    }

    /*
     * Go to tabpage "tp".
     * Only trigger *Enter autocommands when trigger_enter_autocmds is true.
     * Only trigger *Leave autocommands when trigger_leave_autocmds is true.
     * Note: doesn't update the GUI tab.
     */
    /*private*/ static void goto_tabpage_tp(tabpage_C tp, boolean trigger_enter_autocmds, boolean trigger_leave_autocmds)
    {
        /* Don't repeat a message in another tab page. */
        set_keep_msg(null, 0);

        if (tp != curtab && leave_tabpage(tp.tp_curwin.w_buffer, trigger_leave_autocmds) == true)
        {
            if (valid_tabpage(tp))
                enter_tabpage(tp, curbuf, trigger_enter_autocmds, trigger_leave_autocmds);
            else
                enter_tabpage(curtab, curbuf, trigger_enter_autocmds, trigger_leave_autocmds);
        }
    }

    /*
     * Enter window "wp" in tab page "tp".
     * Also updates the GUI tab.
     */
    /*private*/ static void goto_tabpage_win(tabpage_C tp, window_C wp)
    {
        goto_tabpage_tp(tp, true, true);

        if (curtab == tp && win_valid(wp))
            win_enter(wp, true);
    }

    /*
     * Move the current tab page to before tab page "nr".
     */
    /*private*/ static void tabpage_move(int nr)
    {
        if (first_tabpage.tp_next == null)
            return;

        int n = nr;

        /* Remove the current tab page from the list of tab pages. */
        if (curtab == first_tabpage)
            first_tabpage = curtab.tp_next;
        else
        {
            tabpage_C tp;
            for (tp = first_tabpage; tp != null; tp = tp.tp_next)
                if (tp.tp_next == curtab)
                    break;
            if (tp == null) /* "cannot happen" */
                return;
            tp.tp_next = curtab.tp_next;
        }

        /* Re-insert it at the specified position. */
        if (n <= 0)
        {
            curtab.tp_next = first_tabpage;
            first_tabpage = curtab;
        }
        else
        {
            tabpage_C tp;
            for (tp = first_tabpage; tp.tp_next != null && 1 < n; tp = tp.tp_next)
                --n;
            curtab.tp_next = tp.tp_next;
            tp.tp_next = curtab;
        }

        /* Need to redraw the tabline.  Tab page contents doesn't change. */
        redraw_tabline = true;
    }

    /*
     * Go to another window.
     * When jumping to another buffer, stop Visual mode.  Do this before
     * changing windows so we can yank the selection into the '*' register.
     * When jumping to another window on the same buffer, adjust its cursor
     * position to keep the same Visual area.
     */
    /*private*/ static void win_goto(window_C wp)
    {
        window_C owp = curwin;

        if (text_locked())
        {
            beep_flush();
            text_locked_msg();
            return;
        }
        if (curbuf_locked())
            return;

        if (wp.w_buffer != curbuf)
            reset_VIsual_and_resel();
        else if (VIsual_active)
            COPY_pos(wp.w_cursor, curwin.w_cursor);

        win_enter(wp, true);

        /* Conceal cursor line in previous window, unconceal in current window. */
        if (win_valid(owp) && 0 < owp.w_onebuf_opt.wo_cole[0] && msg_scrolled == 0)
            update_single_line(owp, owp.w_cursor.lnum);
        if (0 < curwin.w_onebuf_opt.wo_cole[0] && msg_scrolled == 0)
            need_cursor_line_redraw = true;
    }

    /*
     * Move to window above or below "count" times.
     */
    /*private*/ static void win_goto_ver(boolean up, long count)
        /* up: true to go to win above */
    {
        frame_C foundfr = curwin.w_frame;

        end:
        while (0 < count--)
        {
            frame_C nfr;

            /*
             * First go upwards in the tree of frames until we find a upwards or downwards neighbor.
             */
            for (frame_C fr = foundfr; ; fr = fr.fr_parent)
            {
                if (fr == topframe)
                    break end;
                if (up)
                    nfr = fr.fr_prev;
                else
                    nfr = fr.fr_next;
                if (fr.fr_parent.fr_layout == FR_COL && nfr != null)
                    break;
            }

            /*
             * Now go downwards to find the bottom or top frame in it.
             */
            for ( ; ; )
            {
                if (nfr.fr_layout == FR_LEAF)
                {
                    foundfr = nfr;
                    break;
                }
                frame_C fr = nfr.fr_child;
                if (nfr.fr_layout == FR_ROW)
                {
                    /* Find the frame at the cursor row. */
                    while (fr.fr_next != null
                            && frame2win(fr).w_wincol + fr.fr_width <= curwin.w_wincol + curwin.w_wcol)
                        fr = fr.fr_next;
                }
                if (nfr.fr_layout == FR_COL && up)
                    while (fr.fr_next != null)
                        fr = fr.fr_next;
                nfr = fr;
            }
        }

        if (foundfr != null)
            win_goto(foundfr.fr_win);
    }

    /*
     * Move to left or right window.
     */
    /*private*/ static void win_goto_hor(boolean left, long count)
        /* left: true to go to left win */
    {
        frame_C foundfr = curwin.w_frame;

        end:
        while (0 < count--)
        {
            frame_C nfr;

            /*
             * First go upwards in the tree of frames until we find a left or right neighbor.
             */
            for (frame_C fr = foundfr; ; fr = fr.fr_parent)
            {
                if (fr == topframe)
                    break end;
                if (left)
                    nfr = fr.fr_prev;
                else
                    nfr = fr.fr_next;
                if (fr.fr_parent.fr_layout == FR_ROW && nfr != null)
                    break;
            }

            /*
             * Now go downwards to find the leftmost or rightmost frame in it.
             */
            for ( ; ; )
            {
                if (nfr.fr_layout == FR_LEAF)
                {
                    foundfr = nfr;
                    break;
                }
                frame_C fr = nfr.fr_child;
                if (nfr.fr_layout == FR_COL)
                {
                    /* Find the frame at the cursor row. */
                    while (fr.fr_next != null
                            && frame2win(fr).w_winrow + fr.fr_height <= curwin.w_winrow + curwin.w_wrow)
                        fr = fr.fr_next;
                }
                if (nfr.fr_layout == FR_ROW && left)
                    while (fr.fr_next != null)
                        fr = fr.fr_next;
                nfr = fr;
            }
        }

        if (foundfr != null)
            win_goto(foundfr.fr_win);
    }

    /*
     * Make window "wp" the current window.
     */
    /*private*/ static void win_enter(window_C wp, boolean undo_sync)
    {
        win_enter_ext(wp, undo_sync, false, true, true);
    }

    /*
     * Make window wp the current window.
     * Can be called with "curwin_invalid" true, which means that curwin has just
     * been closed and isn't valid.
     */
    /*private*/ static void win_enter_ext(window_C wp, boolean undo_sync, boolean curwin_invalid, boolean trigger_enter_autocmds, boolean trigger_leave_autocmds)
    {
        boolean other_buffer = false;

        if (wp == curwin && !curwin_invalid)        /* nothing to do */
            return;

        if (!curwin_invalid && trigger_leave_autocmds)
        {
            /*
             * Be careful: If autocommands delete the window, return now.
             */
            if (wp.w_buffer != curbuf)
            {
                apply_autocmds(EVENT_BUFLEAVE, null, null, false, curbuf);
                other_buffer = true;
                if (!win_valid(wp))
                    return;
            }
            apply_autocmds(EVENT_WINLEAVE, null, null, false, curbuf);
            if (!win_valid(wp))
                return;
            /* autocmds may abort script processing */
            if (aborting())
                return;
        }

        /* sync undo before leaving the current buffer */
        if (undo_sync && curbuf != wp.w_buffer)
            u_sync(false);

        /* Might need to scroll the old window before switching, e.g., when the cursor was moved. */
        update_topline();

        /* may have to copy the buffer options when 'cpo' contains 'S' */
        if (wp.w_buffer != curbuf)
            buf_copy_options(wp.w_buffer, BCO_ENTER);
        if (!curwin_invalid)
        {
            prevwin = curwin;       /* remember for CTRL-W p */
            curwin.w_redr_status = true;
        }
        curwin = wp;
        curbuf = wp.w_buffer;
        check_cursor();
        if (!virtual_active())
            curwin.w_cursor.coladd = 0;
        changed_line_abv_curs();    /* assume cursor position needs updating */

        if (trigger_enter_autocmds)
        {
            apply_autocmds(EVENT_WINENTER, null, null, false, curbuf);
            if (other_buffer)
                apply_autocmds(EVENT_BUFENTER, null, null, false, curbuf);
        }

        curwin.w_redr_status = true;
        redraw_tabline = true;
        if (restart_edit != 0)
            redraw_later(VALID);    /* causes status line redraw */

        /* set window height to desired minimal value */
        if (curwin.w_height < p_wh[0] && !curwin.w_onebuf_opt.wo_wfh[0])
            win_setheight((int)p_wh[0]);
        else if (curwin.w_height == 0)
            win_setheight(1);

        /* set window width to desired minimal value */
        if (curwin.w_width < p_wiw[0] && !curwin.w_onebuf_opt.wo_wfw[0])
            win_setwidth((int)p_wiw[0]);

        setmouse();                 /* in case jumped to/from help buffer */
    }

    /*
     * Jump to the first open window that contains buffer "buf", if one exists.
     * Returns a pointer to the window found, otherwise null.
     */
    /*private*/ static window_C buf_jump_open_win(buffer_C buf)
    {
        window_C wp = null;

        if (curwin.w_buffer == buf)
            wp = curwin;
        else
            for (wp = firstwin; wp != null; wp = wp.w_next)
                if (wp.w_buffer == buf)
                    break;
        if (wp != null)
            win_enter(wp, false);

        return wp;
    }

    /*
     * Jump to the first open window in any tab page that contains buffer "buf", if one exists.
     * Returns a pointer to the window found, otherwise null.
     */
    /*private*/ static window_C buf_jump_open_tab(buffer_C buf)
    {
        window_C wp = buf_jump_open_win(buf);

        if (wp != null)
            return wp;

        for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
            if (tp != curtab)
            {
                for (wp = tp.tp_firstwin; wp != null; wp = wp.w_next)
                    if (wp.w_buffer == buf)
                        break;
                if (wp != null)
                {
                    goto_tabpage_win(tp, wp);
                    if (curwin != wp)
                        wp = null;  /* something went wrong */
                    break;
                }
            }

        return wp;
    }

    /*
     * Allocate a window structure and link it in the window list when "hidden" is false.
     */
    /*private*/ static window_C newWindow(window_C after, boolean hidden)
    {
        /* allocate window structure and linesizes arrays */
        window_C new_wp = new window_C();

        win_alloc_lines(new_wp);

        /* init w: variables */
        new_wp.w_vars = newDict();
        init_var_dict(new_wp.w_vars, new_wp.w_winvar, VAR_SCOPE);

        /*
         * Don't execute autocommands while the window is not properly initialized yet.
         */
        block_autocmds();

        /* link the window in the window list */
        if (!hidden)
            win_append(after, new_wp);
        new_wp.w_wincol = 0;
        new_wp.w_width = (int)Columns[0];

        /* position the display and the cursor at the top of the file */
        new_wp.w_topline = 1;
        new_wp.w_botline = 2;
        new_wp.w_cursor.lnum = 1;
        new_wp.w_scbind_pos = 1;

        /* We won't calculate w_fraction until resizing the window. */
        new_wp.w_fraction = 0;
        new_wp.w_prev_fraction_row = -1;

        unblock_autocmds();
        new_wp.w_match_head = null;
        new_wp.w_next_match_id = 4;

        return new_wp;
    }

    /*
     * Remove window 'wp' from the window list and free the structure.
     */
    /*private*/ static void win_free(window_C wp, tabpage_C tp)
        /* tp: tab page "win" is in, null for current */
    {
        /* reduce the reference count to the argument list. */
        alist_unlink(wp.w_alist);

        /* Don't execute autocommands while the window is halfway being deleted.
         * gui_mch_destroy_scrollbar() may trigger a FocusGained event. */
        block_autocmds();

        clear_winopt(wp.w_onebuf_opt);
        clear_winopt(wp.w_allbuf_opt);

        vars_clear(wp.w_vars.dv_hashtab);       /* free all w: variables */
        hash_init(wp.w_vars.dv_hashtab);
        unref_var_dict(wp.w_vars);

        if (prevwin == wp)
            prevwin = null;
        win_free_lines(wp);

        /* Remove the window from the b_wininfo lists,
         * it may happen that the freed memory is re-used for another window. */
        for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
            for (wininfo_C wip = buf.b_wininfo; wip != null; wip = wip.wi_next)
                if (wip.wi_win == wp)
                    wip.wi_win = null;

        clear_matches(wp);

        free_jumplist(wp);

        wp.w_p_cc_cols = null;

        if (wp != aucmd_win)
            win_remove(wp, tp);
        if (autocmd_busy)
        {
            wp.w_next = au_pending_free_win;
            au_pending_free_win = wp;
        }

        unblock_autocmds();
    }

    /*
     * Append window "wp" in the window list after window "after".
     */
    /*private*/ static void win_append(window_C after, window_C wp)
    {
        window_C before;
        if (after == null)      /* after null is in front of the first */
            before = firstwin;
        else
            before = after.w_next;

        wp.w_next = before;
        wp.w_prev = after;
        if (after == null)
            firstwin = wp;
        else
            after.w_next = wp;
        if (before == null)
            lastwin = wp;
        else
            before.w_prev = wp;
    }

    /*
     * Remove a window from the window list.
     */
    /*private*/ static void win_remove(window_C wp, tabpage_C tp)
        /* tp: tab page "win" is in, null for current */
    {
        if (wp.w_prev != null)
            wp.w_prev.w_next = wp.w_next;
        else if (tp == null)
            firstwin = wp.w_next;
        else
            tp.tp_firstwin = wp.w_next;
        if (wp.w_next != null)
            wp.w_next.w_prev = wp.w_prev;
        else if (tp == null)
            lastwin = wp.w_prev;
        else
            tp.tp_lastwin = wp.w_prev;
    }

    /*
     * Append frame "frp" in a frame list after frame "after".
     */
    /*private*/ static void frame_append(frame_C after, frame_C frp)
    {
        frp.fr_next = after.fr_next;
        after.fr_next = frp;
        if (frp.fr_next != null)
            frp.fr_next.fr_prev = frp;
        frp.fr_prev = after;
    }

    /*
     * Insert frame "frp" in a frame list before frame "before".
     */
    /*private*/ static void frame_insert(frame_C before, frame_C frp)
    {
        frp.fr_next = before;
        frp.fr_prev = before.fr_prev;
        before.fr_prev = frp;
        if (frp.fr_prev != null)
            frp.fr_prev.fr_next = frp;
        else
            frp.fr_parent.fr_child = frp;
    }

    /*
     * Remove a frame from a frame list.
     */
    /*private*/ static void frame_remove(frame_C frp)
    {
        if (frp.fr_prev != null)
            frp.fr_prev.fr_next = frp.fr_next;
        else
            frp.fr_parent.fr_child = frp.fr_next;
        if (frp.fr_next != null)
            frp.fr_next.fr_prev = frp.fr_prev;
    }

    /*
     * Allocate w_lines[] for window "wp".
     */
    /*private*/ static void win_alloc_lines(window_C wp)
    {
        wp.w_lines_valid = 0;
        wp.w_lines_len = (int)Rows[0];
        wp.w_lines = ARRAY_wline(wp.w_lines_len);
    }

    /*
     * Free w_lines[] for window "wp".
     */
    /*private*/ static void win_free_lines(window_C wp)
    {
        wp.w_lines_len = 0;
        wp.w_lines = null;
    }

    /*
     * Called from win_new_shellsize() after Rows changed.
     * This only does the current tab page, others must be done when made active.
     */
    /*private*/ static void shell_new_rows()
    {
        long rows_avail = Rows[0] - p_ch[0] - tabline_height();

        int h = (int)rows_avail;

        if (firstwin == null)           /* not initialized yet */
            return;
        if (h < frame_minheight(topframe, null))
            h = frame_minheight(topframe, null);

        /* First try setting the heights of windows with 'winfixheight'.
         * If that doesn't result in the right height, forget about that option. */
        frame_new_height(topframe, h, false, true);
        if (!frame_check_height(topframe, h))
            frame_new_height(topframe, h, false, false);

        win_comp_pos();                 /* recompute w_winrow and w_wincol */
        compute_cmdrow();
        curtab.tp_ch_used = p_ch[0];
    }

    /*
     * Called from win_new_shellsize() after Columns changed.
     */
    /*private*/ static void shell_new_columns()
    {
        if (firstwin == null)           /* not initialized yet */
            return;

        /* First try setting the widths of windows with 'winfixwidth'.
         * If that doesn't result in the right width, forget about that option. */
        frame_new_width(topframe, (int)Columns[0], false, true);
        if (!frame_check_width(topframe, (int)Columns[0]))
            frame_new_width(topframe, (int)Columns[0], false, false);

        win_comp_pos();                 /* recompute w_winrow and w_wincol */
    }

    /*
     * Save the size of all windows in "iap".
     */
    /*private*/ static void win_size_save(iarray_C iap)
    {
        ia_grow(iap, win_count() * 2);

        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
        {
            iap.ia_data[iap.ia_len++] = wp.w_width + wp.w_vsep_width;
            iap.ia_data[iap.ia_len++] = wp.w_height;
        }
    }

    /*
     * Restore window sizes, but only if the number of windows is still the same.
     * Does not free the growarray.
     */
    /*private*/ static void win_size_restore(iarray_C iap)
    {
        if (win_count() * 2 == iap.ia_len)
        {
            /* The order matters, because frames contain other frames, but it's
             * difficult to get right.  The easy way out is to do it twice. */
            for (int round = 0; round < 2; round++)
            {
                int i = 0;
                for (window_C wp = firstwin; wp != null; wp = wp.w_next)
                {
                    frame_setwidth(wp.w_frame, iap.ia_data[i++]);
                    win_setheight_win(iap.ia_data[i++], wp);
                }
            }
            /* recompute the window positions */
            win_comp_pos();
        }
    }

    /*
     * Update the position for all windows, using the width and height of the frames.
     * Returns the row just after the last window.
     */
    /*private*/ static int win_comp_pos()
    {
        int[] row = { tabline_height() };
        int[] col = { 0 };

        frame_comp_pos(topframe, row, col);
        return row[0];
    }

    /*
     * Update the position of the windows in frame "topfrp",
     * using the width and height of the frames.
     * "*row" and "*col" are the top-left position of the frame.
     * They are updated to the bottom-right position plus one.
     */
    /*private*/ static void frame_comp_pos(frame_C topfrp, int[] row, int[] col)
    {
        window_C wp = topfrp.fr_win;
        if (wp != null)
        {
            if (wp.w_winrow != row[0] || wp.w_wincol != col[0])
            {
                /* position changed, redraw */
                wp.w_winrow = row[0];
                wp.w_wincol = col[0];
                redraw_win_later(wp, NOT_VALID);
                wp.w_redr_status = true;
            }
            row[0] += wp.w_height + wp.w_status_height;
            col[0] += wp.w_width + wp.w_vsep_width;
        }
        else
        {
            int startrow = row[0];
            int startcol = col[0];
            for (frame_C frp = topfrp.fr_child; frp != null; frp = frp.fr_next)
            {
                if (topfrp.fr_layout == FR_ROW)
                    row[0] = startrow;        /* all frames are at the same row */
                else
                    col[0] = startcol;        /* all frames are at the same col */
                frame_comp_pos(frp, row, col);
            }
        }
    }

    /*
     * Set current window height and take care of repositioning other windows to fit around it.
     */
    /*private*/ static void win_setheight(int height)
    {
        win_setheight_win(height, curwin);
    }

    /*
     * Set the window height of window "win" and take care of repositioning other windows to fit around it.
     */
    /*private*/ static void win_setheight_win(int height, window_C win)
    {
        if (win == curwin)
        {
            /* Always keep current window at least one line high, even when 'winminheight' is zero. */
            if (height < p_wmh[0])
                height = (int)p_wmh[0];
            if (height == 0)
                height = 1;
        }

        frame_setheight(win.w_frame, height + win.w_status_height);

        /* recompute the window positions */
        int row = win_comp_pos();

        /*
         * If there is extra space created between the last window and the command line, clear it.
         */
        if (full_screen && msg_scrolled == 0 && row < cmdline_row)
            screen_fill(row, cmdline_row, 0, (int)Columns[0], ' ', ' ', 0);
        cmdline_row = row;
        msg_row = row;
        msg_col = 0;

        redraw_all_later(NOT_VALID);
    }

    /*
     * Set the height of a frame to "height" and take care that all frames and
     * windows inside it are resized.  Also resize frames on the left and right
     * if the are in the same FR_ROW frame.
     *
     * Strategy:
     * If the frame is part of a FR_COL frame, try fitting the frame in that frame.
     * If that doesn't work (the FR_COL frame is too small), recursively go to
     * containing frames to resize them and make room.
     * If the frame is part of a FR_ROW frame, all frames must be resized as well.
     * Check for the minimal height of the FR_ROW frame.
     * At the top level we can also use change the command line height.
     */
    /*private*/ static void frame_setheight(frame_C curfrp, int height)
    {
        /* If the height already is the desired value, nothing to do. */
        if (curfrp.fr_height == height)
            return;

        if (curfrp.fr_parent == null)
        {
            long rows_avail = Rows[0] - p_ch[0] - tabline_height();

            /* topframe: can only change the command line */
            if (rows_avail < height)
                height = (int)rows_avail;
            if (0 < height)
                frame_new_height(curfrp, height, false, false);
        }
        else if (curfrp.fr_parent.fr_layout == FR_ROW)
        {
            /* Row of frames: also need to resize frames left and right of this one.
             * First check for the minimal height of these. */
            int h = frame_minheight(curfrp.fr_parent, null);
            if (height < h)
                height = h;
            frame_setheight(curfrp.fr_parent, height);
        }
        else
        {
            // %% red. 3x
            int room = 0;                       /* total number of lines available */
            int room_reserved = 0;
            int room_cmdline = 0;               /* lines available from cmdline */

            /*
             * Column of frames: try to change only frames in this column.
             * Do this twice:
             * 1: compute room available, if it's not enough try resizing the containing frame.
             * 2: compute the room available and adjust the height to it.
             * Try not to reduce the height of a window with 'winfixheight' set.
             */
            for (int run = 1; run <= 2; run++)
            {
                room = 0;                   /* total number of lines available */
                room_reserved = 0;

                for (frame_C frp = curfrp.fr_parent.fr_child; frp != null; frp = frp.fr_next)
                {
                    if (frp != curfrp && frp.fr_win != null && frp.fr_win.w_onebuf_opt.wo_wfh[0])
                        room_reserved += frp.fr_height;
                    room += frp.fr_height;
                    if (frp != curfrp)
                        room -= frame_minheight(frp, null);
                }
                if (curfrp.fr_width != (int)Columns[0])
                    room_cmdline = 0;
                else
                {
                    room_cmdline = (int)(Rows[0] - p_ch[0]) - (lastwin.w_winrow + lastwin.w_height + lastwin.w_status_height);
                    if (room_cmdline < 0)
                        room_cmdline = 0;
                }

                if (height <= room + room_cmdline)
                    break;
                if (run == 2 || curfrp.fr_width == (int)Columns[0])
                {
                    if (height > room + room_cmdline)
                        height = room + room_cmdline;
                    break;
                }

                frame_setheight(curfrp.fr_parent, height
                    + frame_minheight(curfrp.fr_parent, NOWIN) - (int)p_wmh[0] - 1);
            }

            /*
             * Compute the number of lines we will take from others frames (can be negative!).
             */
            int take = height - curfrp.fr_height;

            /* If there is not enough room,
             * also reduce the height of a window with 'winfixheight' set.
             */
            if (room + room_cmdline - room_reserved < height)
                room_reserved = room + room_cmdline - height;

            /* If there is only a 'winfixheight' window and making the window smaller,
             * need to make the other window taller.
             */
            if (take < 0 && room - curfrp.fr_height < room_reserved)
                room_reserved = 0;

            if (0 < take && 0 < room_cmdline)
            {
                /* use lines from cmdline first */
                if (take < room_cmdline)
                    room_cmdline = take;
                take -= room_cmdline;
                topframe.fr_height += room_cmdline;
            }

            /*
             * set the current frame to the new height
             */
            frame_new_height(curfrp, height, false, false);

            /*
             * First take lines from the frames after the current frame.
             * If that is not enough, takes lines from frames above the current frame.
             */
            for (int run = 0; run < 2; run++)
            {
                frame_C frp;
                if (run == 0)
                    frp = curfrp.fr_next;   /* 1st run: start with next window */
                else
                    frp = curfrp.fr_prev;   /* 2nd run: start with prev window */
                while (frp != null && take != 0)
                {
                    int h = frame_minheight(frp, null);
                    if (0 < room_reserved && frp.fr_win != null && frp.fr_win.w_onebuf_opt.wo_wfh[0])
                    {
                        if (room_reserved >= frp.fr_height)
                            room_reserved -= frp.fr_height;
                        else
                        {
                            if (take < frp.fr_height - room_reserved)
                                room_reserved = frp.fr_height - take;
                            take -= frp.fr_height - room_reserved;
                            frame_new_height(frp, room_reserved, false, false);
                            room_reserved = 0;
                        }
                    }
                    else
                    {
                        if (frp.fr_height - take < h)
                        {
                            take -= frp.fr_height - h;
                            frame_new_height(frp, h, false, false);
                        }
                        else
                        {
                            frame_new_height(frp, frp.fr_height - take, false, false);
                            take = 0;
                        }
                    }
                    if (run == 0)
                        frp = frp.fr_next;
                    else
                        frp = frp.fr_prev;
                }
            }
        }
    }

    /*
     * Set current window width and take care of repositioning other windows to fit around it.
     */
    /*private*/ static void win_setwidth(int width)
    {
        win_setwidth_win(width, curwin);
    }

    /*private*/ static void win_setwidth_win(int width, window_C win)
    {
        /* Always keep current window at least one column wide, even when 'winminwidth' is zero. */
        if (win == curwin)
        {
            if (width < p_wmw[0])
                width = (int)p_wmw[0];
            if (width == 0)
                width = 1;
        }

        frame_setwidth(win.w_frame, width + win.w_vsep_width);

        /* recompute the window positions */
        win_comp_pos();

        redraw_all_later(NOT_VALID);
    }

    /*
     * Set the width of a frame to "width"
     * and take care that all frames and windows inside it are resized.
     * Also resize frames above and below if the are in the same FR_ROW frame.
     *
     * Strategy is similar to frame_setheight().
     */
    /*private*/ static void frame_setwidth(frame_C curfrp, int width)
    {
        /* If the width already is the desired value, nothing to do. */
        if (curfrp.fr_width == width)
            return;

        if (curfrp.fr_parent == null)
            /* topframe: can't change width */
            return;

        if (curfrp.fr_parent.fr_layout == FR_COL)
        {
            /* Column of frames: also need to resize frames above and below of this one.
             * First check for the minimal width of these. */
            int w = frame_minwidth(curfrp.fr_parent, null);
            if (width < w)
                width = w;
            frame_setwidth(curfrp.fr_parent, width);
        }
        else
        {
            // %% red. 2x
            int room = 0;                       /* total number of lines available */
            int room_reserved = 0;

            /*
             * Row of frames: try to change only frames in this row.
             * Do this twice:
             * 1: compute room available, if it's not enough try resizing the containing frame.
             * 2: compute the room available and adjust the width to it.
             */
            for (int run = 1; run <= 2; run++)
            {
                room = 0;
                room_reserved = 0;

                for (frame_C frp = curfrp.fr_parent.fr_child; frp != null; frp = frp.fr_next)
                {
                    if (frp != curfrp && frp.fr_win != null && frp.fr_win.w_onebuf_opt.wo_wfw[0])
                        room_reserved += frp.fr_width;
                    room += frp.fr_width;
                    if (frp != curfrp)
                        room -= frame_minwidth(frp, null);
                }

                if (width <= room)
                    break;

                long rows_avail = Rows[0] - p_ch[0] - tabline_height();

                if (run == 2 || rows_avail <= curfrp.fr_height)
                {
                    if (room < width)
                        width = room;
                    break;
                }

                frame_setwidth(curfrp.fr_parent, width
                     + frame_minwidth(curfrp.fr_parent, NOWIN) - (int)p_wmw[0] - 1);
            }

            /*
             * Compute the number of lines we will take from others frames (can be negative!).
             */
            int take = width - curfrp.fr_width;

            /* If there is not enough room,
             * also reduce the width of a window with 'winfixwidth' set.
             */
            if (room - room_reserved < width)
                room_reserved = room - width;

            /* If there is only a 'winfixwidth' window and making the window smaller,
             * need to make the other window narrower.
             */
            if (take < 0 && room - curfrp.fr_width < room_reserved)
                room_reserved = 0;

            /*
             * set the current frame to the new width
             */
            frame_new_width(curfrp, width, false, false);

            /*
             * First take lines from the frames right of the current frame.
             * If that is not enough, takes lines from frames left of the current frame.
             */
            for (int run = 0; run < 2; run++)
            {
                frame_C frp;
                if (run == 0)
                    frp = curfrp.fr_next;   /* 1st run: start with next window */
                else
                    frp = curfrp.fr_prev;   /* 2nd run: start with prev window */
                while (frp != null && take != 0)
                {
                    int w = frame_minwidth(frp, null);
                    if (0 < room_reserved && frp.fr_win != null && frp.fr_win.w_onebuf_opt.wo_wfw[0])
                    {
                        if (room_reserved >= frp.fr_width)
                            room_reserved -= frp.fr_width;
                        else
                        {
                            if (take < frp.fr_width - room_reserved)
                                room_reserved = frp.fr_width - take;
                            take -= frp.fr_width - room_reserved;
                            frame_new_width(frp, room_reserved, false, false);
                            room_reserved = 0;
                        }
                    }
                    else
                    {
                        if (frp.fr_width - take < w)
                        {
                            take -= frp.fr_width - w;
                            frame_new_width(frp, w, false, false);
                        }
                        else
                        {
                            frame_new_width(frp, frp.fr_width - take, false, false);
                            take = 0;
                        }
                    }
                    if (run == 0)
                        frp = frp.fr_next;
                    else
                        frp = frp.fr_prev;
                }
            }
        }
    }

    /*
     * Check 'winminheight' for a valid value.
     */
    /*private*/ static void win_setminheight()
    {
        boolean first = true;

        /* loop until there is a 'winminheight' that is possible */
        while (0 < p_wmh[0])
        {
            /* TODO: handle vertical splits */
            int room = (int)-p_wh[0];
            for (window_C wp = firstwin; wp != null; wp = wp.w_next)
                room += wp.w_height - p_wmh[0];
            if (0 <= room)
                break;
            --p_wmh[0];
            if (first)
            {
                emsg(e_noroom);
                first = false;
            }
        }
    }

    /*
     * Status line of dragwin is dragged "offset" lines down (negative is up).
     */
    /*private*/ static void win_drag_status_line(window_C dragwin, int offset)
    {
        frame_C fr = dragwin.w_frame;
        frame_C curfr = fr;
        if (fr != topframe)         /* more than one window */
        {
            fr = fr.fr_parent;
            /* When the parent frame is not a column of frames, its parent should be. */
            if (fr.fr_layout != FR_COL)
            {
                curfr = fr;
                if (fr != topframe) /* only a row of windows, may drag statusline */
                    fr = fr.fr_parent;
            }
        }

        /* If this is the last frame in a column, may want to resize
         * the parent frame instead (go two up to skip a row of frames). */
        while (curfr != topframe && curfr.fr_next == null)
        {
            if (fr != topframe)
                fr = fr.fr_parent;
            curfr = fr;
            if (fr != topframe)
                fr = fr.fr_parent;
        }

        boolean up;     /* if true, drag status line up, otherwise down */
        int room;

        if (offset < 0) /* drag up */
        {
            up = true;
            offset = -offset;
            /* sum up the room of the current frame and above it */
            if (fr == curfr)
            {
                /* only one window */
                room = fr.fr_height - frame_minheight(fr, null);
            }
            else
            {
                room = 0;
                for (fr = fr.fr_child; ; fr = fr.fr_next)
                {
                    room += fr.fr_height - frame_minheight(fr, null);
                    if (fr == curfr)
                        break;
                }
            }
            fr = curfr.fr_next;     /* put fr at frame that grows */
        }
        else            /* drag down */
        {
            up = false;
            /*
             * Only dragging the last status line can reduce "p_ch".
             */
            room = (int)Rows[0] - cmdline_row;
            if (curfr.fr_next == null)
                room -= 1;
            else
                room -= p_ch[0];
            if (room < 0)
                room = 0;
            /* sum up the room of frames below of the current one */
            for (fr = curfr.fr_next; fr != null; fr = fr.fr_next)
                room += fr.fr_height - frame_minheight(fr, null);
            fr = curfr;                     /* put fr at window that grows */
        }

        if (room < offset)          /* not enough room */
            offset = room;          /* move as far as we can */
        if (offset <= 0)
            return;

        /*
         * Grow frame fr by "offset" lines.
         * Doesn't happen when dragging the last status line up.
         */
        if (fr != null)
            frame_new_height(fr, fr.fr_height + offset, up, false);

        if (up)
            fr = curfr;             /* current frame gets smaller */
        else
            fr = curfr.fr_next;     /* next frame gets smaller */

        /*
         * Now make the other frames smaller.
         */
        while (fr != null && 0 < offset)
        {
            int n = frame_minheight(fr, null);
            if (fr.fr_height - offset <= n)
            {
                offset -= fr.fr_height - n;
                frame_new_height(fr, n, !up, false);
            }
            else
            {
                frame_new_height(fr, fr.fr_height - offset, !up, false);
                break;
            }
            if (up)
                fr = fr.fr_prev;
            else
                fr = fr.fr_next;
        }

        int row = win_comp_pos();
        screen_fill(row, cmdline_row, 0, (int)Columns[0], ' ', ' ', 0);
        cmdline_row = row;

        p_ch[0] = Rows[0] - cmdline_row;
        if (p_ch[0] < 1)
            p_ch[0] = 1;
        curtab.tp_ch_used = p_ch[0];

        redraw_all_later(SOME_VALID);
        showmode();
    }

    /*
     * Separator line of dragwin is dragged "offset" lines right (negative is left).
     */
    /*private*/ static void win_drag_vsep_line(window_C dragwin, int offset)
    {
        frame_C fr = dragwin.w_frame;
        if (fr == topframe)         /* only one window (cannot happen?) */
            return;

        frame_C curfr = fr;
        fr = fr.fr_parent;
        /* When the parent frame is not a row of frames, its parent should be. */
        if (fr.fr_layout != FR_ROW)
        {
            if (fr == topframe)     /* only a column of windows (cannot happen?) */
                return;
            curfr = fr;
            fr = fr.fr_parent;
        }

        /* If this is the last frame in a row, may want to resize a parent frame instead. */
        while (curfr.fr_next == null)
        {
            if (fr == topframe)
                break;
            curfr = fr;
            fr = fr.fr_parent;
            if (fr != topframe)
            {
                curfr = fr;
                fr = fr.fr_parent;
            }
        }

        boolean left;   /* if true, drag separator line left, otherwise right */
        int room;

        if (offset < 0) /* drag left */
        {
            left = true;
            offset = -offset;
            /* sum up the room of the current frame and left of it */
            room = 0;
            for (fr = fr.fr_child; ; fr = fr.fr_next)
            {
                room += fr.fr_width - frame_minwidth(fr, null);
                if (fr == curfr)
                    break;
            }
            fr = curfr.fr_next;     /* put fr at frame that grows */
        }
        else            /* drag right */
        {
            left = false;
            /* sum up the room of frames right of the current one */
            room = 0;
            for (fr = curfr.fr_next; fr != null; fr = fr.fr_next)
                room += fr.fr_width - frame_minwidth(fr, null);
            fr = curfr;             /* put fr at window that grows */
        }

        if (room < offset)          /* not enough room */
            offset = room;          /* move as far as we can */
        if (offset <= 0)            /* No room at all, quit. */
            return;

        /* grow frame fr by offset lines */
        frame_new_width(fr, fr.fr_width + offset, left, false);

        /* shrink other frames: current and at the left or at the right */
        if (left)
            fr = curfr;             /* current frame gets smaller */
        else
            fr = curfr.fr_next;     /* next frame gets smaller */

        while (fr != null && 0 < offset)
        {
            int n = frame_minwidth(fr, null);
            if (fr.fr_width - offset <= n)
            {
                offset -= fr.fr_width - n;
                frame_new_width(fr, n, !left, false);
            }
            else
            {
                frame_new_width(fr, fr.fr_width - offset, !left, false);
                break;
            }
            if (left)
                fr = fr.fr_prev;
            else
                fr = fr.fr_next;
        }

        win_comp_pos();
        redraw_all_later(NOT_VALID);
    }

    /*private*/ static final long FRACTION_MULT = 16384L;

    /*
     * Set wp.w_fraction for the current w_wrow and w_height.
     */
    /*private*/ static void set_fraction(window_C wp)
    {
        wp.w_fraction = (int)(((long)wp.w_wrow * FRACTION_MULT + (long)wp.w_height / 2) / (long)wp.w_height);
    }

    /*
     * Set the height of a window.
     * This takes care of the things inside the window, not what happens to the
     * window position, the frame or to other windows.
     */
    /*private*/ static void win_new_height(window_C wp, int height)
    {
        int prev_height = wp.w_height;

        /* Don't want a negative height.  Happens when splitting a tiny window.
         * Will equalize heights soon to fix it. */
        if (height < 0)
            height = 0;
        if (wp.w_height == height)
            return;                             /* nothing to do */

        if (0 < wp.w_height)
        {
            if (wp == curwin)
                /* w_wrow needs to be valid.  When setting 'laststatus' this may
                 * call win_new_height() recursively. */
                validate_cursor();

            if (wp.w_height != prev_height)
                return;                         /* Recursive call already changed the size,
                                                 * bail out here to avoid the following
                                                 * to mess things up. */

            if (wp.w_wrow != wp.w_prev_fraction_row)
                set_fraction(wp);
        }

        wp.w_height = height;
        wp.w_skipcol = 0;

        /* Don't change w_topline when height is zero.  Don't set w_topline
         * when 'scrollbind' is set and this isn't the current window. */
        if (0 < height && (!wp.w_onebuf_opt.wo_scb[0] || wp == curwin))
        {
            /*
             * Find a value for w_topline that shows the cursor at the same
             * relative position in the window as before (more or less).
             */
            long lnum = wp.w_cursor.lnum;
            if (lnum < 1)           /* can happen when starting up */
                lnum = 1;
            wp.w_wrow = (int)(((long)wp.w_fraction * (long)height - 1L + FRACTION_MULT / 2) / FRACTION_MULT);
            int line_size = plines_win_col(wp, lnum, (long)wp.w_cursor.col) - 1;
            int sline = wp.w_wrow - line_size;

            if (0 <= sline)
            {
                /* Make sure the whole cursor line is visible, if possible. */
                int rows = plines_win(wp, lnum, false);

                if (sline > wp.w_height - rows)
                {
                    sline = wp.w_height - rows;
                    wp.w_wrow -= rows - line_size;
                }
            }

            if (sline < 0)
            {
                /*
                 * Cursor line would go off top of screen if w_wrow was this high.
                 * Make cursor line the first line in the window.  If not enough
                 * room use w_skipcol;
                 */
                wp.w_wrow = line_size;
                if (wp.w_height <= wp.w_wrow && 0 < wp.w_width - win_col_off(wp))
                {
                    wp.w_skipcol += wp.w_width - win_col_off(wp);
                    --wp.w_wrow;
                    while (wp.w_height <= wp.w_wrow)
                    {
                        wp.w_skipcol += wp.w_width - win_col_off(wp) + win_col_off2(wp);
                        --wp.w_wrow;
                    }
                }
                set_topline(wp, lnum);
            }
            else if (0 < sline)
            {
                while (0 < sline && 1 < lnum)
                {
                    --lnum;
                        line_size = plines_win(wp, lnum, true);
                    sline -= line_size;
                }

                if (sline < 0)
                {
                    /*
                     * Line we want at top would go off top of screen.  Use next line instead.
                     */
                    lnum++;
                    wp.w_wrow -= line_size + sline;
                }
                else if (0 < sline)
                {
                    /* First line of file reached, use that as topline. */
                    lnum = 1;
                    wp.w_wrow -= sline;
                }

                set_topline(wp, lnum);
            }
        }

        if (wp == curwin)
        {
            if (p_so[0] != 0)
                update_topline();
            curs_columns(false);    /* validate w_wrow */
        }
        if (0 < prev_height)
            wp.w_prev_fraction_row = wp.w_wrow;

        win_comp_scroll(wp);
        redraw_win_later(wp, SOME_VALID);
        wp.w_redr_status = true;
        invalidate_botline_win(wp);
    }

    /*
     * Set the width of a window.
     */
    /*private*/ static void win_new_width(window_C wp, int width)
    {
        wp.w_width = width;
        wp.w_lines_valid = 0;
        changed_line_abv_curs_win(wp);
        invalidate_botline_win(wp);
        if (wp == curwin)
        {
            update_topline();
            curs_columns(true);     /* validate w_wrow */
        }
        redraw_win_later(wp, NOT_VALID);
        wp.w_redr_status = true;
    }

    /*private*/ static void win_comp_scroll(window_C wp)
    {
        wp.w_onebuf_opt.wo_scr[0] = (wp.w_height >>> 1);
        if (wp.w_onebuf_opt.wo_scr[0] == 0)
            wp.w_onebuf_opt.wo_scr[0] = 1;
    }

    /*
     * command_height: called whenever "p_ch" has been changed
     */
    /*private*/ static void command_height()
    {
        long old_p_ch = curtab.tp_ch_used;

        /* Use the value of "p_ch" that we remembered.  This is needed for when the
         * GUI starts up, we can't be sure in what order things happen.  And when
         * "p_ch" was changed in another tab page. */
        curtab.tp_ch_used = p_ch[0];

        /* Find bottom frame with width of screen. */
        frame_C frp = lastwin.w_frame;
        while (frp.fr_width != (int)Columns[0] && frp.fr_parent != null)
            frp = frp.fr_parent;

        /* Avoid changing the height of a window with 'winfixheight' set. */
        while (frp.fr_prev != null && frp.fr_layout == FR_LEAF && frp.fr_win.w_onebuf_opt.wo_wfh[0])
            frp = frp.fr_prev;

        if (starting != NO_SCREEN)
        {
            cmdline_row = (int)(Rows[0] - p_ch[0]);

            if (old_p_ch < p_ch[0])                /* "p_ch" got bigger */
            {
                while (old_p_ch < p_ch[0])
                {
                    if (frp == null)
                    {
                        emsg(e_noroom);
                        p_ch[0] = old_p_ch;
                        curtab.tp_ch_used = p_ch[0];
                        cmdline_row = (int)(Rows[0] - p_ch[0]);
                        break;
                    }
                    int h = frp.fr_height - frame_minheight(frp, null);
                    if (h > p_ch[0] - old_p_ch)
                        h = (int)(p_ch[0] - old_p_ch);
                    old_p_ch += h;
                    frame_add_height(frp, -h);
                    frp = frp.fr_prev;
                }

                /* Recompute window positions. */
                win_comp_pos();

                /* clear the lines added to cmdline */
                if (full_screen)
                    screen_fill(cmdline_row, (int)Rows[0], 0, (int)Columns[0], ' ', ' ', 0);
                msg_row = cmdline_row;
                redraw_cmdline = true;
                return;
            }

            if (msg_row < cmdline_row)
                msg_row = cmdline_row;
            redraw_cmdline = true;
        }
        frame_add_height(frp, (int)(old_p_ch - p_ch[0]));

        /* Recompute window positions. */
        if (frp != lastwin.w_frame)
            win_comp_pos();
    }

    /*
     * Resize frame "frp" to be "n" lines higher (negative for less high).
     * Also resize the frames it is contained in.
     */
    /*private*/ static void frame_add_height(frame_C frp, int n)
    {
        frame_new_height(frp, frp.fr_height + n, false, false);
        for ( ; ; )
        {
            frp = frp.fr_parent;
            if (frp == null)
                break;
            frp.fr_height += n;
        }
    }

    /*
     * Add or remove a status line for the bottom window(s), according to the
     * value of 'laststatus'.
     */
    /*private*/ static void last_status(boolean morewin)
        /* morewin: pretend there are two or more windows */
    {
        /* Don't make a difference between horizontal or vertical split. */
        last_status_rec(topframe, (p_ls[0] == 2 || (p_ls[0] == 1 && (morewin || lastwin != firstwin))));
    }

    /*private*/ static void last_status_rec(frame_C fr, boolean statusline)
    {
        if (fr.fr_layout == FR_LEAF)
        {
            window_C wp = fr.fr_win;
            if (wp.w_status_height != 0 && !statusline)
            {
                /* remove status line */
                win_new_height(wp, wp.w_height + 1);
                wp.w_status_height = 0;
                comp_col();
            }
            else if (wp.w_status_height == 0 && statusline)
            {
                /* Find a frame to take a line from. */
                frame_C fp = fr;
                while (fp.fr_height <= frame_minheight(fp, null))
                {
                    if (fp == topframe)
                    {
                        emsg(e_noroom);
                        return;
                    }
                    /* In a column of frames: go to frame above.
                     * If already at the top or in a row of frames: go to parent. */
                    if (fp.fr_parent.fr_layout == FR_COL && fp.fr_prev != null)
                        fp = fp.fr_prev;
                    else
                        fp = fp.fr_parent;
                }
                wp.w_status_height = 1;
                if (fp != fr)
                {
                    frame_new_height(fp, fp.fr_height - 1, false, false);
                    frame_fix_height(wp);
                    win_comp_pos();
                }
                else
                    win_new_height(wp, wp.w_height - 1);
                comp_col();
                redraw_all_later(SOME_VALID);
            }
        }
        else if (fr.fr_layout == FR_ROW)
        {
            /* vertically split windows, set status line for each one */
            for (frame_C fp = fr.fr_child; fp != null; fp = fp.fr_next)
                last_status_rec(fp, statusline);
        }
        else
        {
            /* horizontally split window, set status line for last one */
            frame_C fp;
            for (fp = fr.fr_child; fp.fr_next != null; fp = fp.fr_next)
                ;
            last_status_rec(fp, statusline);
        }
    }

    /*
     * Return the number of lines used by the tab page line.
     */
    /*private*/ static int tabline_height()
    {
        switch ((int)p_stal[0])
        {
            case 0: return 0;
            case 1: return (first_tabpage.tp_next != null) ? 1 : 0;
        }
        return 1;
    }

    /*
     * Check if the "://" of a URL is at the pointer, return URL_SLASH.
     * Also check for ":\\", which MS Internet Explorer accepts, return URL_BACKSLASH.
     */
    /*private*/ static int path_is_url(Bytes p)
    {
        if (STRNCMP(p, u8("://"), 3) == 0)
            return URL_SLASH;
        else if (STRNCMP(p, u8(":\\\\"), 3) == 0)
            return URL_BACKSLASH;

        return 0;
    }

    /*
     * Check if "fname" starts with "name://".  Return URL_SLASH if it does.
     * Return URL_BACKSLASH for "name:\\".
     * Return zero otherwise.
     */
    /*private*/ static int path_with_url(Bytes fname)
    {
        Bytes p;

        for (p = fname; asc_isalpha(p.at(0)); p = p.plus(1))
            ;

        return path_is_url(p);
    }

    /*
     * Return true if "name" is a full (absolute) path name or URL.
     */
    /*private*/ static boolean vim_isAbsName(Bytes name)
    {
        return (path_with_url(name) != 0 || mch_isFullName(name));
    }

    /*
     * Get absolute file name into buffer "buf[len]".
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean vim_fullName(Bytes fname, Bytes buf, int len, boolean force)
        /* force: force expansion even when already absolute */
    {
        boolean retval = true;

        buf.be(0, NUL);
        if (fname == null)
            return false;

        int url = path_with_url(fname);
        if (url == 0)
            retval = mch_fullName(fname, buf, len, force);
        if (url != 0 || retval == false)
        {
            /* something failed; use the file name (truncate when too long) */
            vim_strncpy(buf, fname, len - 1);
        }

        return retval;
    }

    /*
     * Return the minimal number of rows that is needed on the screen to display
     * the current number of windows.
     */
    /*private*/ static int min_rows()
    {
        if (firstwin == null)       /* not initialized yet */
            return MIN_LINES;

        int total = 0;

        for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
        {
            int n = frame_minheight(tp.tp_topframe, null);
            if (total < n)
                total = n;
        }
        total += tabline_height();
        total += 1;         /* count the room for the command line */

        return total;
    }

    /*
     * Return true if there is only one window (in the current tab page),
     * not counting a help or preview window, unless it is the current window.
     * Does not count "aucmd_win".
     */
    /*private*/ static boolean only_one_window()
    {
        int count = 0;

        /* If there is another tab page there always is another window. */
        if (first_tabpage.tp_next != null)
            return false;

        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            if (wp.w_buffer != null && wp != aucmd_win)
                count++;

        return (count <= 1);
    }

    /*
     * Correct the cursor line number in other windows.  Used after changing the
     * current buffer, and before applying autocommands.
     * When "do_curwin" is true, also check current window.
     */
    /*private*/ static void check_lnums(boolean do_curwin)
    {
        for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
            for (window_C wp = (tp == curtab) ? firstwin : tp.tp_firstwin; wp != null; wp = wp.w_next)
                if ((do_curwin || wp != curwin) && wp.w_buffer == curbuf)
                {
                    if (wp.w_cursor.lnum > curbuf.b_ml.ml_line_count)
                        wp.w_cursor.lnum = curbuf.b_ml.ml_line_count;
                    if (wp.w_topline > curbuf.b_ml.ml_line_count)
                        wp.w_topline = curbuf.b_ml.ml_line_count;
                }
    }

    /*
     * A snapshot of the window sizes, to restore them after closing the help window.
     * Only these fields are used:
     * fr_layout
     * fr_width
     * fr_height
     * fr_next
     * fr_child
     * fr_win (only valid for the old curwin, null otherwise)
     */

    /*
     * Create a snapshot of the current frame sizes.
     */
    /*private*/ static void make_snapshot(int idx)
    {
        clear_snapshot(curtab, idx);
        curtab.tp_snapshot[idx] = make_snapshot_rec(topframe);
    }

    /*private*/ static frame_C make_snapshot_rec(frame_C fr)
    {
        frame_C snap = new frame_C();

        snap.fr_layout = fr.fr_layout;
        snap.fr_width = fr.fr_width;
        snap.fr_height = fr.fr_height;
        if (fr.fr_next != null)
            snap.fr_next = make_snapshot_rec(fr.fr_next);
        if (fr.fr_child != null)
            snap.fr_child = make_snapshot_rec(fr.fr_child);
        if (fr.fr_layout == FR_LEAF && fr.fr_win == curwin)
            snap.fr_win = curwin;

        return snap;
    }

    /*
     * Remove any existing snapshot.
     */
    /*private*/ static void clear_snapshot(tabpage_C tp, int idx)
    {
        clear_snapshot_rec(tp.tp_snapshot[idx]);
        tp.tp_snapshot[idx] = null;
    }

    /*private*/ static void clear_snapshot_rec(frame_C fr)
    {
        if (fr != null)
        {
            clear_snapshot_rec(fr.fr_next);
            clear_snapshot_rec(fr.fr_child);
        }
    }

    /*
     * Restore a previously created snapshot, if there is any.
     * This is only done if the screen size didn't change and the window layout is still the same.
     */
    /*private*/ static void restore_snapshot(int idx, boolean close_curwin)
        /* close_curwin: closing current window */
    {
        if (curtab.tp_snapshot[idx] != null
                && curtab.tp_snapshot[idx].fr_width == topframe.fr_width
                && curtab.tp_snapshot[idx].fr_height == topframe.fr_height
                && check_snapshot_rec(curtab.tp_snapshot[idx], topframe) == true)
        {
            window_C wp = restore_snapshot_rec(curtab.tp_snapshot[idx], topframe);
            win_comp_pos();
            if (wp != null && close_curwin)
                win_goto(wp);
            redraw_all_later(CLEAR);
        }
        clear_snapshot(curtab, idx);
    }

    /*
     * Check if frames "sn" and "fr" have the same layout, same following frames and same children.
     */
    /*private*/ static boolean check_snapshot_rec(frame_C sn, frame_C fr)
    {
        if (sn.fr_layout != fr.fr_layout
                || (sn.fr_next == null) != (fr.fr_next == null)
                || (sn.fr_child == null) != (fr.fr_child == null)
                || (sn.fr_next != null && check_snapshot_rec(sn.fr_next, fr.fr_next) == false)
                || (sn.fr_child != null && check_snapshot_rec(sn.fr_child, fr.fr_child) == false))
            return false;

        return true;
    }

    /*
     * Copy the size of snapshot frame "sn" to frame "fr".  Do the same for all
     * following frames and children.
     * Returns a pointer to the old current window, or null.
     */
    /*private*/ static window_C restore_snapshot_rec(frame_C sn, frame_C fr)
    {
        window_C wp = null;

        fr.fr_height = sn.fr_height;
        fr.fr_width = sn.fr_width;
        if (fr.fr_layout == FR_LEAF)
        {
            frame_new_height(fr, fr.fr_height, false, false);
            frame_new_width(fr, fr.fr_width, false, false);
            wp = sn.fr_win;
        }
        if (sn.fr_next != null)
        {
            window_C wp2 = restore_snapshot_rec(sn.fr_next, fr.fr_next);
            if (wp2 != null)
                wp = wp2;
        }
        if (sn.fr_child != null)
        {
            window_C wp2 = restore_snapshot_rec(sn.fr_child, fr.fr_child);
            if (wp2 != null)
                wp = wp2;
        }

        return wp;
    }

    /*
     * Set "win" to be the curwin and "tp" to be the current tab page.
     * restore_win() MUST be called to undo, also when false is returned.
     * No autocommands will be executed until restore_win() is called.
     * When "no_display" is true the display won't be affected,
     * no redraw is triggered, another tabpage access is limited.
     * Returns false if switching to "win" failed.
     */
    /*private*/ static boolean switch_win(window_C[] save_curwin, tabpage_C[] save_curtab, window_C win, tabpage_C tp, boolean no_display)
    {
        block_autocmds();
        save_curwin[0] = curwin;
        if (tp != null)
        {
            save_curtab[0] = curtab;
            if (no_display)
            {
                curtab.tp_firstwin = firstwin;
                curtab.tp_lastwin = lastwin;
                curtab = tp;
                firstwin = curtab.tp_firstwin;
                lastwin = curtab.tp_lastwin;
            }
            else
                goto_tabpage_tp(tp, false, false);
        }
        if (!win_valid(win))
            return false;
        curwin = win;
        curbuf = curwin.w_buffer;
        return true;
    }

    /*
     * Restore current tabpage and window saved by switch_win(), if still valid.
     * When "no_display" is true the display won't be affected, no redraw is triggered.
     */
    /*private*/ static void restore_win(window_C save_curwin, tabpage_C save_curtab, boolean no_display)
    {
        if (save_curtab != null && valid_tabpage(save_curtab))
        {
            if (no_display)
            {
                curtab.tp_firstwin = firstwin;
                curtab.tp_lastwin = lastwin;
                curtab = save_curtab;
                firstwin = curtab.tp_firstwin;
                lastwin = curtab.tp_lastwin;
            }
            else
                goto_tabpage_tp(save_curtab, false, false);
        }
        if (win_valid(save_curwin))
        {
            curwin = save_curwin;
            curbuf = curwin.w_buffer;
        }
        unblock_autocmds();
    }

    /*
     * Add match to the match list of window 'wp'.
     * The pattern 'pat' will be highlighted with the group 'grp' with priority 'prio'.
     * Optionally, a desired ID 'id' can be specified (greater than or equal to 1).
     * If no particular ID is desired, -1 must be specified for 'id'.
     * Return ID of added match, -1 on failure.
     */
    /*private*/ static int match_add(window_C wp, Bytes grp, Bytes pat, int prio, int id, list_C pos_list)
    {
        regprog_C regprog = null;
        int rtype = SOME_VALID;

        if (grp.at(0) == NUL || (pat != null && pat.at(0) == NUL))
            return -1;
        if (id < -1 || id == 0)
        {
            emsgn(u8("E799: Invalid ID: %ld (must be greater than or equal to 1)"), (long)id);
            return -1;
        }
        if (id != -1)
        {
            for (matchitem_C mi = wp.w_match_head; mi != null; mi = mi.next)
                if (mi.id == id)
                {
                    emsgn(u8("E801: ID already taken: %ld"), (long)id);
                    return -1;
                }
        }
        int hlg_id = syn_namen2id(grp, strlen(grp));
        if (hlg_id == 0)
        {
            emsg2(e_nogroup, grp);
            return -1;
        }
        if (pat != null && (regprog = vim_regcomp(pat, RE_MAGIC)) == null)
        {
            emsg2(e_invarg2, pat);
            return -1;
        }

        /* Find available match ID. */
        while (id == -1)
        {
            matchitem_C mi = wp.w_match_head;
            while (mi != null && mi.id != wp.w_next_match_id)
                mi = mi.next;
            if (mi == null)
                id = wp.w_next_match_id;
            wp.w_next_match_id++;
        }

        /* Build new match. */
        matchitem_C m = new matchitem_C();
        m.id = id;
        m.priority = prio;
        m.pattern = (pat == null) ? null : STRDUP(pat);
        m.hlg_id = hlg_id;
        m.mi_match.regprog = regprog;
        m.mi_match.rmm_ic = false;
        m.mi_match.rmm_maxcol = 0;

        /* Set up position matches. */
        if (pos_list != null)
        {
            long toplnum = 0;
            long botlnum = 0;

            int i;
            listitem_C li;
            for (i = 0, li = pos_list.lv_first; li != null && i < MAXPOSMATCH; i++, li = li.li_next)
            {
                long lnum = 0;
                int col = 0;
                int len = 1;

                if (li.li_tv.tv_type == VAR_LIST)
                {
                    boolean[] error = { false };

                    list_C subl = li.li_tv.tv_list;
                    if (subl == null)
                        return -1;
                    listitem_C subli = subl.lv_first;
                    if (subli == null)
                        return -1;
                    lnum = get_tv_number_chk(subli.li_tv, error);
                    if (error[0])
                        return -1;
                    if (lnum == 0)
                    {
                        --i;
                        continue;
                    }
                    m.mi_pos.pm_pos[i].lnum = lnum;
                    subli = subli.li_next;
                    if (subli != null)
                    {
                        col = (int)get_tv_number_chk(subli.li_tv, error);
                        if (error[0])
                            return -1;
                        subli = subli.li_next;
                        if (subli != null)
                        {
                            len = (int)get_tv_number_chk(subli.li_tv, error);
                            if (error[0])
                                return -1;
                        }
                    }
                    m.mi_pos.pm_pos[i].col = col;
                    m.mi_pos.pm_pos[i].len = len;
                }
                else if (li.li_tv.tv_type == VAR_NUMBER)
                {
                    if (li.li_tv.tv_number == 0)
                    {
                        --i;
                        continue;
                    }
                    m.mi_pos.pm_pos[i].lnum = li.li_tv.tv_number;
                    m.mi_pos.pm_pos[i].col = 0;
                    m.mi_pos.pm_pos[i].len = 0;
                }
                else
                {
                    emsg(u8("List or number required"));
                    return -1;
                }

                if (toplnum == 0 || lnum < toplnum)
                    toplnum = lnum;
                if (botlnum == 0 || lnum >= botlnum)
                    botlnum = lnum + 1;
            }

            /* Calculate top and bottom lines for redrawing area. */
            if (toplnum != 0)
            {
                if (wp.w_buffer.b_mod_set)
                {
                    if (wp.w_buffer.b_mod_top > toplnum)
                        wp.w_buffer.b_mod_top = toplnum;
                    if (wp.w_buffer.b_mod_bot < botlnum)
                        wp.w_buffer.b_mod_bot = botlnum;
                }
                else
                {
                    wp.w_buffer.b_mod_set = true;
                    wp.w_buffer.b_mod_top = toplnum;
                    wp.w_buffer.b_mod_bot = botlnum;
                    wp.w_buffer.b_mod_xlines = 0;
                }
                m.mi_pos.toplnum = toplnum;
                m.mi_pos.botlnum = botlnum;
                rtype = VALID;
            }
        }

        /* Insert new match.
         * The match list is in ascending order with regard to the match priorities. */
        matchitem_C mi = wp.w_match_head;
        matchitem_C prev = mi;
        while (mi != null && mi.priority <= prio)
        {
            prev = mi;
            mi = mi.next;
        }
        if (mi == prev)
            wp.w_match_head = m;
        else
            prev.next = m;
        m.next = mi;

        redraw_later(rtype);
        return id;
    }

    /*
     * Delete match with ID 'id' in the match list of window 'wp'.
     * Print error messages if 'perr' is true.
     */
    /*private*/ static int match_delete(window_C wp, int id, boolean perr)
    {
        if (id < 1)
        {
            if (perr == true)
                emsgn(u8("E802: Invalid ID: %ld (must be greater than or equal to 1)"), (long)id);
            return -1;
        }

        matchitem_C mi = wp.w_match_head;
        matchitem_C prev = mi;

        while (mi != null && mi.id != id)
        {
            prev = mi;
            mi = mi.next;
        }
        if (mi == null)
        {
            if (perr == true)
                emsgn(u8("E803: ID not found: %ld"), (long)id);
            return -1;
        }
        if (mi == prev)
            wp.w_match_head = mi.next;
        else
            prev.next = mi.next;

        mi.mi_match.regprog = null;
        mi.pattern = null;

        int rtype = SOME_VALID;
        if (mi.mi_pos.toplnum != 0)
        {
            buffer_C buf = wp.w_buffer;
            if (wp.w_buffer.b_mod_set)
            {
                if (buf.b_mod_top > mi.mi_pos.toplnum)
                    buf.b_mod_top = mi.mi_pos.toplnum;
                if (buf.b_mod_bot < mi.mi_pos.botlnum)
                    buf.b_mod_bot = mi.mi_pos.botlnum;
            }
            else
            {
                buf.b_mod_set = true;
                buf.b_mod_top = mi.mi_pos.toplnum;
                buf.b_mod_bot = mi.mi_pos.botlnum;
                buf.b_mod_xlines = 0;
            }
            rtype = VALID;
        }

        redraw_later(rtype);
        return 0;
    }

    /*
     * Delete all matches in the match list of window 'wp'.
     */
    /*private*/ static void clear_matches(window_C wp)
    {
        while (wp.w_match_head != null)
        {
            matchitem_C mi = wp.w_match_head.next;
            wp.w_match_head.mi_match.regprog = null;
            wp.w_match_head = mi;
        }

        redraw_later(SOME_VALID);
    }

    /*
     * Get match from ID 'id' in window 'wp'.
     * Return null if match not found.
     */
    /*private*/ static matchitem_C get_match(window_C wp, int id)
    {
        matchitem_C mi = wp.w_match_head;

        while (mi != null && mi.id != id)
            mi = mi.next;

        return mi;
    }

    /*
     * Return true if "topfrp" and its children are at the right height.
     */
    /*private*/ static boolean frame_check_height(frame_C topfrp, int height)
    {
        if (topfrp.fr_height != height)
            return false;

        if (topfrp.fr_layout == FR_ROW)
            for (frame_C frp = topfrp.fr_child; frp != null; frp = frp.fr_next)
                if (frp.fr_height != height)
                    return false;

        return true;
    }

    /*
     * Return true if "topfrp" and its children are at the right width.
     */
    /*private*/ static boolean frame_check_width(frame_C topfrp, int width)
    {
        if (topfrp.fr_width != width)
            return false;

        if (topfrp.fr_layout == FR_COL)
            for (frame_C frp = topfrp.fr_child; frp != null; frp = frp.fr_next)
                if (frp.fr_width != width)
                    return false;

        return true;
    }

    /*
     * move.c: Functions for moving the cursor and scrolling text.
     *
     * There are two ways to move the cursor:
     * 1. Move the cursor directly, the text is scrolled to keep the cursor in the window.
     * 2. Scroll the text, the cursor is moved into the text visible in the window.
     * The 'scrolloff' option makes this a bit complicated.
     */

    /*private*/ static final class lineoff_C
    {
        long            lnum;       /* line number */
        int             height;     /* height of added line */

        /*private*/ lineoff_C()
        {
        }
    }

    /*private*/ static void COPY_lineoff(lineoff_C lo1, lineoff_C lo0)
    {
        lo1.lnum = lo0.lnum;
        lo1.height = lo0.height;
    }

    /*
     * Compute wp.w_botline for the current wp.w_topline.
     * Can be called after wp.w_topline changed.
     */
    /*private*/ static void comp_botline(window_C wp)
    {
        long lnum;
        int done;

        /*
         * If w_cline_row is valid, start there.
         * Otherwise have to start at w_topline.
         */
        check_cursor_moved(wp);
        if ((wp.w_valid & VALID_CROW) != 0)
        {
            lnum = wp.w_cursor.lnum;
            done = wp.w_cline_row;
        }
        else
        {
            lnum = wp.w_topline;
            done = 0;
        }

        for ( ; lnum <= wp.w_buffer.b_ml.ml_line_count; lnum++)
        {
            int n = plines_win(wp, lnum, true);
            if (lnum == wp.w_cursor.lnum)
            {
                wp.w_cline_row = done;
                wp.w_cline_height = n;
                redraw_for_cursorline(wp);
                wp.w_valid |= (VALID_CROW|VALID_CHEIGHT);
            }
            if (wp.w_height < done + n)
                break;
            done += n;
        }

        /* wp.w_botline is the line that is just below the window */
        wp.w_botline = lnum;
        wp.w_valid |= VALID_BOTLINE|VALID_BOTLINE_AP;

        set_empty_rows(wp, done);
    }

    /*
     * Redraw when w_cline_row changes and 'relativenumber' or 'cursorline' is set.
     */
    /*private*/ static void redraw_for_cursorline(window_C wp)
    {
        if ((wp.w_onebuf_opt.wo_rnu[0] || wp.w_onebuf_opt.wo_cul[0]) && (wp.w_valid & VALID_CROW) == 0)
            redraw_win_later(wp, SOME_VALID);
    }

    /*
     * Update curwin.w_topline and redraw if necessary.
     * Used to update the screen before printing a message.
     */
    /*private*/ static void update_topline_redraw()
    {
        update_topline();
        if (must_redraw != 0)
            update_screen(0);
    }

    /*
     * Update curwin.w_topline to move the cursor onto the screen.
     */
    /*private*/ static void update_topline()
    {
        boolean check_topline = false;
        boolean check_botline = false;
        long save_so = p_so[0];

        if (!screen_valid(true))
            return;

        /* If the window height is zero just use the cursor line. */
        if (curwin.w_height == 0)
        {
            curwin.w_topline = curwin.w_cursor.lnum;
            curwin.w_botline = curwin.w_topline;
            curwin.w_valid |= VALID_BOTLINE|VALID_BOTLINE_AP;
            curwin.w_scbind_pos = 1;
            return;
        }

        check_cursor_moved(curwin);
        if ((curwin.w_valid & VALID_TOPLINE) != 0)
            return;

        /* When dragging with the mouse, don't scroll that quickly. */
        if (0 < mouse_dragging)
            p_so[0] = mouse_dragging - 1;

        long old_topline = curwin.w_topline;

        /*
         * If the buffer is empty, always set topline to 1.
         */
        if (bufempty())             /* special case - file is empty */
        {
            if (curwin.w_topline != 1)
                redraw_later(NOT_VALID);
            curwin.w_topline = 1;
            curwin.w_botline = 2;
            curwin.w_valid |= VALID_BOTLINE|VALID_BOTLINE_AP;
            curwin.w_scbind_pos = 1;
        }

        /*
         * If the cursor is above or near the top of the window, scroll the window
         * to show the line the cursor is in, with 'scrolloff' context.
         */
        else
        {
            if (1 < curwin.w_topline)
            {
                /* If the cursor is above topline, scrolling is always needed.
                 * If the cursor is far below topline and there is no folding,
                 * scrolling down is never needed. */
                if (curwin.w_cursor.lnum < curwin.w_topline)
                    check_topline = true;
                else if (check_top_offset())
                    check_topline = true;
            }

            if (check_topline)
            {
                int halfheight = curwin.w_height / 2 - 1;
                if (halfheight < 2)
                    halfheight = 2;

                int n = (int)(curwin.w_topline + p_so[0] - curwin.w_cursor.lnum);

                /* If we weren't very close to begin with, we scroll to put the
                 * cursor in the middle of the window.  Otherwise put the cursor
                 * near the top of the window. */
                if (halfheight <= n)
                    scroll_cursor_halfway(false);
                else
                {
                    scroll_cursor_top(scrolljump_value(), false);
                    check_botline = true;
                }
            }

            else
            {
                check_botline = true;
            }
        }

        /*
         * If the cursor is below the bottom of the window, scroll the window
         * to put the cursor on the window.
         * When w_botline is invalid, recompute it first, to avoid a redraw later.
         * If w_botline was approximated, we might need a redraw later in a few
         * cases, but we don't want to spend (a lot of) time recomputing w_botline
         * for every small change.
         */
        if (check_botline)
        {
            if ((curwin.w_valid & VALID_BOTLINE_AP) == 0)
                validate_botline();

            if (curwin.w_botline <= curbuf.b_ml.ml_line_count)
            {
                if (curwin.w_cursor.lnum < curwin.w_botline)
                {
                    if (curwin.w_botline - p_so[0] <= curwin.w_cursor.lnum)
                    {
                        /* Cursor is (a few lines) above botline,
                         * check if there are 'scrolloff' window lines below the cursor.
                         * If not, need to scroll. */
                        int n = curwin.w_empty_rows;
                        lineoff_C loff = new lineoff_C();
                        loff.lnum = curwin.w_cursor.lnum;
                        loff.height = 0;
                        while (loff.lnum < curwin.w_botline)
                        {
                            n += loff.height;
                            if (p_so[0] <= n)
                                break;
                            botline_forw(loff);
                        }
                        if (p_so[0] <= n)
                            /* sufficient context, no need to scroll */
                            check_botline = false;
                    }
                    else
                        /* sufficient context, no need to scroll */
                        check_botline = false;
                }
                if (check_botline)
                {
                    long line_count = curwin.w_cursor.lnum - curwin.w_botline + 1 + p_so[0];
                    if (line_count <= curwin.w_height + 1)
                        scroll_cursor_bot(scrolljump_value(), false);
                    else
                        scroll_cursor_halfway(false);
                }
            }
        }
        curwin.w_valid |= VALID_TOPLINE;

        /*
         * Need to redraw when topline changed.
         */
        if (curwin.w_topline != old_topline)
        {
            dollar_vcol = -1;
            if (curwin.w_skipcol != 0)
            {
                curwin.w_skipcol = 0;
                redraw_later(NOT_VALID);
            }
            else
                redraw_later(VALID);
            /* May need to set w_skipcol when cursor in w_topline. */
            if (curwin.w_cursor.lnum == curwin.w_topline)
                validate_cursor();
        }

        p_so[0] = save_so;
    }

    /*
     * Return the scrolljump value to use for the current window.
     * When 'scrolljump' is positive use it as-is.
     * When 'scrolljump' is negative use it as a percentage of the window height.
     */
    /*private*/ static int scrolljump_value()
    {
        if (0 <= p_sj[0])
            return (int)p_sj[0];

        return (curwin.w_height * (int)-p_sj[0]) / 100;
    }

    /*
     * Return true when there are not 'scrolloff' lines above the cursor for the current window.
     */
    /*private*/ static boolean check_top_offset()
    {
        if (curwin.w_cursor.lnum < curwin.w_topline + p_so[0])
        {
            lineoff_C loff = new lineoff_C();
            loff.lnum = curwin.w_cursor.lnum;

            int n = 0;
            /* Count the visible screen lines above the cursor line. */
            for ( ; n < p_so[0]; n += loff.height)
            {
                topline_back(loff);
                /* Stop when included a line above the window. */
                if (loff.lnum < curwin.w_topline)
                    break;
            }

            if (n < p_so[0])
                return true;
        }

        return false;
    }

    /*private*/ static void update_curswant()
    {
        if (curwin.w_set_curswant)
        {
            validate_virtcol();
            curwin.w_curswant = curwin.w_virtcol;
            curwin.w_set_curswant = false;
        }
    }

    /*
     * Check if the cursor has moved.  Set the w_valid flag accordingly.
     */
    /*private*/ static void check_cursor_moved(window_C wp)
    {
        if (wp.w_cursor.lnum != wp.w_valid_cursor.lnum)
        {
            wp.w_valid &= ~(VALID_WROW|VALID_WCOL|VALID_VIRTCOL|VALID_CHEIGHT|VALID_CROW|VALID_TOPLINE);
            COPY_pos(wp.w_valid_cursor, wp.w_cursor);
            wp.w_valid_leftcol = wp.w_leftcol;
        }
        else if (wp.w_cursor.col != wp.w_valid_cursor.col
                  || wp.w_leftcol != wp.w_valid_leftcol
           || wp.w_cursor.coladd != wp.w_valid_cursor.coladd)
        {
            wp.w_valid &= ~(VALID_WROW|VALID_WCOL|VALID_VIRTCOL);
            wp.w_valid_cursor.col = wp.w_cursor.col;
            wp.w_valid_leftcol = wp.w_leftcol;
            wp.w_valid_cursor.coladd = wp.w_cursor.coladd;
        }
    }

    /*
     * Call this function when some window settings have changed, which require
     * the cursor position, botline and topline to be recomputed and the window
     * to be redrawn.  E.g, when changing the 'wrap' option or folding.
     */
    /*private*/ static void changed_window_setting()
    {
        changed_window_setting_win(curwin);
    }

    /*private*/ static void changed_window_setting_win(window_C wp)
    {
        wp.w_lines_valid = 0;
        changed_line_abv_curs_win(wp);
        wp.w_valid &= ~(VALID_BOTLINE|VALID_BOTLINE_AP|VALID_TOPLINE);
        redraw_win_later(wp, NOT_VALID);
    }

    /*
     * Set wp.w_topline to a certain number.
     */
    /*private*/ static void set_topline(window_C wp, long lnum)
    {
        /* Approximate the value of w_botline. */
        wp.w_botline += lnum - wp.w_topline;
        wp.w_topline = lnum;
        wp.w_topline_was_set = true;
        wp.w_valid &= ~(VALID_WROW|VALID_CROW|VALID_BOTLINE|VALID_TOPLINE);
        /* Don't set VALID_TOPLINE here, 'scrolloff' needs to be checked. */
        redraw_later(VALID);
    }

    /*
     * Call this function when the length of the cursor line (in screen
     * characters) has changed, and the change is before the cursor.
     * Need to take care of w_botline separately!
     */
    /*private*/ static void changed_cline_bef_curs()
    {
        curwin.w_valid &= ~(VALID_WROW|VALID_WCOL|VALID_VIRTCOL|VALID_CHEIGHT|VALID_TOPLINE);
    }

    /*private*/ static void changed_cline_bef_curs_win(window_C wp)
    {
        wp.w_valid &= ~(VALID_WROW|VALID_WCOL|VALID_VIRTCOL|VALID_CHEIGHT|VALID_TOPLINE);
    }

    /*
     * Call this function when the length of a line (in screen characters) above
     * the cursor have changed.
     * Need to take care of w_botline separately!
     */
    /*private*/ static void changed_line_abv_curs()
    {
        curwin.w_valid &= ~(VALID_WROW|VALID_WCOL|VALID_VIRTCOL|VALID_CROW|VALID_CHEIGHT|VALID_TOPLINE);
    }

    /*private*/ static void changed_line_abv_curs_win(window_C wp)
    {
        wp.w_valid &= ~(VALID_WROW|VALID_WCOL|VALID_VIRTCOL|VALID_CROW|VALID_CHEIGHT|VALID_TOPLINE);
    }

    /*
     * Make sure the value of curwin.w_botline is valid.
     */
    /*private*/ static void validate_botline()
    {
        if ((curwin.w_valid & VALID_BOTLINE) == 0)
            comp_botline(curwin);
    }

    /*
     * Mark curwin.w_botline as invalid (because of some change in the buffer).
     */
    /*private*/ static void invalidate_botline()
    {
        curwin.w_valid &= ~(VALID_BOTLINE|VALID_BOTLINE_AP);
    }

    /*private*/ static void invalidate_botline_win(window_C wp)
    {
        wp.w_valid &= ~(VALID_BOTLINE|VALID_BOTLINE_AP);
    }

    /*private*/ static void approximate_botline_win(window_C wp)
    {
        wp.w_valid &= ~VALID_BOTLINE;
    }

    /*
     * Return true if curwin.w_wrow and curwin.w_wcol are valid.
     */
    /*private*/ static boolean cursor_valid()
    {
        check_cursor_moved(curwin);
        return ((curwin.w_valid & (VALID_WROW|VALID_WCOL)) == (VALID_WROW|VALID_WCOL));
    }

    /*
     * Validate cursor position.  Makes sure w_wrow and w_wcol are valid.
     * w_topline must be valid, you may need to call update_topline() first!
     */
    /*private*/ static void validate_cursor()
    {
        check_cursor_moved(curwin);
        if ((curwin.w_valid & (VALID_WCOL|VALID_WROW)) != (VALID_WCOL|VALID_WROW))
            curs_columns(true);
    }

    /*
     * Compute wp.w_cline_row and wp.w_cline_height, based on the current value of wp.w_topline.
     */
    /*private*/ static void curs_rows(window_C wp)
    {
        /* check if wp.w_lines[].wl_size is invalid */
        boolean all_invalid = (!redrawing()
                            || wp.w_lines_valid == 0
                            || wp.w_topline < wp.w_lines[0].wl_lnum);
        int i = 0;
        wp.w_cline_row = 0;
        for (long lnum = wp.w_topline; lnum < wp.w_cursor.lnum; i++)
        {
            boolean valid = false;
            if (!all_invalid && i < wp.w_lines_valid)
            {
                if (wp.w_lines[i].wl_lnum < lnum || !wp.w_lines[i].wl_valid)
                    continue;               /* skip changed or deleted lines */
                if (wp.w_lines[i].wl_lnum == lnum)
                {
                    valid = true;
                }
                else if (lnum < wp.w_lines[i].wl_lnum)
                    --i;                    /* hold at inserted lines */
            }
            if (valid)
            {
                lnum++;
                wp.w_cline_row += wp.w_lines[i].wl_size;
            }
            else
            {
                wp.w_cline_row += plines_win(wp, lnum++, true);
            }
        }

        check_cursor_moved(wp);
        if ((wp.w_valid & VALID_CHEIGHT) == 0)
        {
            if (all_invalid
                    || i == wp.w_lines_valid
                    || (i < wp.w_lines_valid
                            && (!wp.w_lines[i].wl_valid || wp.w_lines[i].wl_lnum != wp.w_cursor.lnum)))
            {
                wp.w_cline_height = plines_win(wp, wp.w_cursor.lnum, true);
            }
            else if (wp.w_lines_valid < i)
            {
                /* a line that is too long to fit on the last screen line */
                wp.w_cline_height = 0;
            }
            else
            {
                wp.w_cline_height = wp.w_lines[i].wl_size;
            }
        }

        redraw_for_cursorline(curwin);
        wp.w_valid |= VALID_CROW|VALID_CHEIGHT;
    }

    /*
     * Validate curwin.w_virtcol only.
     */
    /*private*/ static void validate_virtcol()
    {
        validate_virtcol_win(curwin);
    }

    /*
     * Validate wp.w_virtcol only.
     */
    /*private*/ static void validate_virtcol_win(window_C wp)
    {
        check_cursor_moved(wp);
        if ((wp.w_valid & VALID_VIRTCOL) == 0)
        {
            int[] vcol = { wp.w_virtcol };
            getvvcol(wp, wp.w_cursor, null, vcol, null);
            wp.w_virtcol = vcol[0];
            wp.w_valid |= VALID_VIRTCOL;
            if (wp.w_onebuf_opt.wo_cuc[0])
                redraw_win_later(wp, SOME_VALID);
        }
    }

    /*
     * Validate curwin.w_cline_height only.
     */
    /*private*/ static void validate_cheight()
    {
        check_cursor_moved(curwin);
        if ((curwin.w_valid & VALID_CHEIGHT) == 0)
        {
            curwin.w_cline_height = plines(curwin.w_cursor.lnum);
            curwin.w_valid |= VALID_CHEIGHT;
        }
    }

    /*
     * Validate w_wcol and w_virtcol only.
     */
    /*private*/ static void validate_cursor_col()
    {
        validate_virtcol();
        if ((curwin.w_valid & VALID_WCOL) == 0)
        {
            int col = curwin.w_virtcol;
            int off = curwin_col_off();
            col += off;
            int width = curwin.w_width - off + curwin_col_off2();

            /* long line wrapping, adjust curwin.w_wrow */
            if (curwin.w_onebuf_opt.wo_wrap[0] && curwin.w_width <= col && 0 < width)
                /* use same formula as what is used in curs_columns() */
                col -= ((col - curwin.w_width) / width + 1) * width;
            if (curwin.w_leftcol < col)
                col -= curwin.w_leftcol;
            else
                col = 0;
            curwin.w_wcol = col;

            curwin.w_valid |= VALID_WCOL;
        }
    }

    /*
     * Compute offset of a window, occupied by absolute or relative line number,
     * fold column and sign column (these don't move when scrolling horizontally).
     */
    /*private*/ static int win_col_off(window_C wp)
    {
        return (((wp.w_onebuf_opt.wo_nu[0] || wp.w_onebuf_opt.wo_rnu[0]) ? number_width(wp) + 1 : 0)
              + ((cmdwin_type == 0 || wp != curwin) ? 0 : 1));
    }

    /*private*/ static int curwin_col_off()
    {
        return win_col_off(curwin);
    }

    /*
     * Return the difference in column offset for the second screen line of a wrapped line.
     * It's 8 if 'number' or 'relativenumber' is on and 'n' is in 'cpoptions'.
     */
    /*private*/ static int win_col_off2(window_C wp)
    {
        if ((wp.w_onebuf_opt.wo_nu[0] || wp.w_onebuf_opt.wo_rnu[0]) && vim_strbyte(p_cpo[0], CPO_NUMCOL) != null)
            return number_width(wp) + 1;

        return 0;
    }

    /*private*/ static int curwin_col_off2()
    {
        return win_col_off2(curwin);
    }

    /*
     * compute curwin.w_wcol and curwin.w_virtcol.
     * Also updates curwin.w_wrow and curwin.w_cline_row.
     * Also updates curwin.w_leftcol.
     */
    /*private*/ static void curs_columns(boolean may_scroll)
        /* may_scroll: when true, may scroll horizontally */
    {
        /*
         * First make sure that w_topline is valid (after moving the cursor).
         */
        update_topline();

        /*
         * Next make sure that w_cline_row is valid.
         */
        if ((curwin.w_valid & VALID_CROW) == 0)
            curs_rows(curwin);

        /*
         * Compute the number of virtual columns.
         */
        int[] startcol = new int[1];
        int[] vcol = { curwin.w_virtcol };
        int[] endcol = new int[1];
        getvvcol(curwin, curwin.w_cursor, startcol, vcol, endcol);
        curwin.w_virtcol = vcol[0];

        /* remove '$' from change command when cursor moves onto it */
        if (dollar_vcol < startcol[0])
            dollar_vcol = -1;

        /* offset for first screen line */
        int extra = curwin_col_off();
        curwin.w_wcol = curwin.w_virtcol + extra;
        endcol[0] += extra;

        /*
         * Now compute w_wrow, counting screen lines from w_cline_row.
         */
        curwin.w_wrow = curwin.w_cline_row;

        int width = 0;

        int textwidth = curwin.w_width - extra;
        if (textwidth <= 0)
        {
            /* No room for text, put cursor in last char of window. */
            curwin.w_wcol = curwin.w_width - 1;
            curwin.w_wrow = curwin.w_height - 1;
        }
        else if (curwin.w_onebuf_opt.wo_wrap[0] && curwin.w_width != 0)
        {
            width = textwidth + curwin_col_off2();

            /* long line wrapping, adjust curwin.w_wrow */
            if (curwin.w_width <= curwin.w_wcol)
            {
                /* this same formula is used in validate_cursor_col() */
                int n = (curwin.w_wcol - curwin.w_width) / width + 1;
                curwin.w_wcol -= n * width;
                curwin.w_wrow += n;

                /* When cursor wraps to first char of next line in Insert mode,
                 * the 'showbreak' string isn't shown, backup to first column. */
                if (p_sbr[0].at(0) != NUL && ml_get_cursor().at(0) == NUL && curwin.w_wcol == mb_string2cells(p_sbr[0], -1))
                    curwin.w_wcol = 0;
            }
        }
        /* No line wrapping: compute curwin.w_leftcol if scrolling is on and line is not folded.
         * If scrolling is off, curwin.w_leftcol is assumed to be 0. */
        else if (may_scroll)
        {
            /*
             * If Cursor is left of the screen, scroll rightwards.
             * If Cursor is right of the screen, scroll leftwards.
             * If we get closer to the edge than 'sidescrolloff', scroll a little extra.
             */
            int off_left = startcol[0] - curwin.w_leftcol - (int)p_siso[0];
            int off_right = endcol[0] - (curwin.w_leftcol + curwin.w_width - (int)p_siso[0]) + 1;
            if (off_left < 0 || 0 < off_right)
            {
                int diff;
                if (off_left < 0)
                    diff = -off_left;
                else
                    diff = off_right;

                /* When far off or not enough room on either side, put cursor in middle of window. */
                int new_leftcol;
                if (p_ss[0] == 0 || textwidth / 2 <= diff || off_left <= off_right)
                    new_leftcol = curwin.w_wcol - extra - textwidth / 2;
                else
                {
                    if (diff < p_ss[0])
                        diff = (int)p_ss[0];
                    if (off_left < 0)
                        new_leftcol = curwin.w_leftcol - diff;
                    else
                        new_leftcol = curwin.w_leftcol + diff;
                }
                if (new_leftcol < 0)
                    new_leftcol = 0;
                if (new_leftcol != curwin.w_leftcol)
                {
                    curwin.w_leftcol = new_leftcol;
                    /* screen has to be redrawn with new curwin.w_leftcol */
                    redraw_later(NOT_VALID);
                }
            }
            curwin.w_wcol -= curwin.w_leftcol;
        }
        else if (curwin.w_leftcol < curwin.w_wcol)
            curwin.w_wcol -= curwin.w_leftcol;
        else
            curwin.w_wcol = 0;

        int prev_skipcol = curwin.w_skipcol;

        int p_lines = 0;
        if ((curwin.w_height <= curwin.w_wrow
                    || ((0 < prev_skipcol || curwin.w_height <= curwin.w_wrow + p_so[0])
                        && curwin.w_height <= (p_lines = plines_win(curwin, curwin.w_cursor.lnum, false)) - 1))
                && curwin.w_height != 0
                && curwin.w_cursor.lnum == curwin.w_topline
                && 0 < width
                && curwin.w_width != 0)
        {
            /* Cursor past end of screen.  Happens with a single line that does
             * not fit on screen.  Find a skipcol to show the text around the
             * cursor.  Avoid scrolling all the time. compute value of "extra":
             * 1: less than "p_so" lines above
             * 2: less than "p_so" lines below
             * 3: both of them */
            extra = 0;
            if (curwin.w_virtcol < curwin.w_skipcol + p_so[0] * width)
                extra = 1;
            /* Compute last display line of the buffer line that we want at the bottom of the window. */
            if (p_lines == 0)
                p_lines = plines_win(curwin, curwin.w_cursor.lnum, false);
            --p_lines;
            int n;
            if (curwin.w_wrow + p_so[0] < p_lines)
                n = curwin.w_wrow + (int)p_so[0];
            else
                n = p_lines;
            if (curwin.w_height + curwin.w_skipcol / width <= n)
                extra += 2;

            if (extra == 3 || p_lines < p_so[0] * 2)
            {
                /* not enough room for 'scrolloff', put cursor in the middle */
                n = curwin.w_virtcol / width;
                if (curwin.w_height / 2 < n)
                    n -= curwin.w_height / 2;
                else
                    n = 0;
                /* don't skip more than necessary */
                if (n > p_lines - curwin.w_height + 1)
                    n = p_lines - curwin.w_height + 1;
                curwin.w_skipcol = n * width;
            }
            else if (extra == 1)
            {
                /* less then 'scrolloff' lines above, decrease skipcol */
                extra = (curwin.w_skipcol + (int)p_so[0] * width - curwin.w_virtcol + width - 1) / width;
                if (0 < extra)
                {
                    if (curwin.w_skipcol < extra * width)
                        extra = curwin.w_skipcol / width;
                    curwin.w_skipcol -= extra * width;
                }
            }
            else if (extra == 2)
            {
                /* less then 'scrolloff' lines below, increase skipcol */
                endcol[0] = (n - curwin.w_height + 1) * width;
                while (curwin.w_virtcol < endcol[0])
                    endcol[0] -= width;
                if (curwin.w_skipcol < endcol[0])
                    curwin.w_skipcol = endcol[0];
            }

            curwin.w_wrow -= curwin.w_skipcol / width;
            if (curwin.w_height <= curwin.w_wrow)
            {
                /* small window, make sure cursor is in it */
                extra = curwin.w_wrow - curwin.w_height + 1;
                curwin.w_skipcol += extra * width;
                curwin.w_wrow -= extra;
            }

            extra = (prev_skipcol - curwin.w_skipcol) / width;
            if (0 < extra)
                win_ins_lines(curwin, 0, extra, false, false);
            else if (extra < 0)
                win_del_lines(curwin, 0, -extra, false, false);
        }
        else
            curwin.w_skipcol = 0;
        if (prev_skipcol != curwin.w_skipcol)
            redraw_later(NOT_VALID);

        /* Redraw when w_virtcol changes and 'cursorcolumn' is set. */
        if (curwin.w_onebuf_opt.wo_cuc[0] && (curwin.w_valid & VALID_VIRTCOL) == 0)
            redraw_later(SOME_VALID);

        curwin.w_valid |= VALID_WCOL|VALID_WROW|VALID_VIRTCOL;
    }

    /*
     * Scroll the current window down by "line_count" logical lines.  "CTRL-Y"
     */
    /*private*/ static void scrolldown(long line_count)
    {
        long done = 0;              /* total # of physical lines done */
        boolean moved = false;

        validate_cursor();          /* w_wrow needs to be valid */
        while (0 < line_count--)
        {
            if (curwin.w_topline == 1)
                break;
            --curwin.w_topline;
            done += plines(curwin.w_topline);

            --curwin.w_botline;         /* approximate w_botline */
            invalidate_botline();
        }
        curwin.w_wrow += done;          /* keep w_wrow updated */
        curwin.w_cline_row += done;     /* keep w_cline_row updated */

        /*
         * Compute the row number of the last row of the cursor line
         * and move the cursor onto the displayed part of the window.
         */
        int wrow = curwin.w_wrow;
        if (curwin.w_onebuf_opt.wo_wrap[0] && curwin.w_width != 0)
        {
            validate_virtcol();
            validate_cheight();
            wrow += curwin.w_cline_height - 1 - curwin.w_virtcol / curwin.w_width;
        }
        while (curwin.w_height <= wrow && 1 < curwin.w_cursor.lnum)
        {
            wrow -= plines(curwin.w_cursor.lnum--);
            curwin.w_valid &= ~(VALID_WROW|VALID_WCOL|VALID_CHEIGHT|VALID_CROW|VALID_VIRTCOL);
            moved = true;
        }
        if (moved)
            coladvance(curwin.w_curswant);
    }

    /*
     * Scroll the current window up by "line_count" logical lines.  "CTRL-E"
     */
    /*private*/ static void scrollup(long line_count)
    {
        curwin.w_topline += line_count;
        curwin.w_botline += line_count;     /* approximate w_botline */

        if (curwin.w_topline > curbuf.b_ml.ml_line_count)
            curwin.w_topline = curbuf.b_ml.ml_line_count;
        if (curwin.w_botline > curbuf.b_ml.ml_line_count + 1)
            curwin.w_botline = curbuf.b_ml.ml_line_count + 1;

        curwin.w_valid &= ~(VALID_WROW|VALID_CROW|VALID_BOTLINE);
        if (curwin.w_cursor.lnum < curwin.w_topline)
        {
            curwin.w_cursor.lnum = curwin.w_topline;
            curwin.w_valid &= ~(VALID_WROW|VALID_WCOL|VALID_CHEIGHT|VALID_CROW|VALID_VIRTCOL);
            coladvance(curwin.w_curswant);
        }
    }

    /*
     * Add one line above "lp.lnum".  This can be a filler line, a closed fold or
     * a (wrapped) text line.  Uses and sets "lp.fill".
     * Returns the height of the added line in "lp.height".
     * Lines above the first one are incredibly high: MAXCOL.
     */
    /*private*/ static void topline_back(lineoff_C lp)
    {
        --lp.lnum;
        if (lp.lnum < 1)
            lp.height = MAXCOL;
        else
        {
            lp.height = plines(lp.lnum);
        }
    }

    /*
     * Add one line below "lp.lnum".
     * This can be a filler line, a closed fold or a (wrapped) text line.
     * Uses and sets "lp.fill".
     * Returns the height of the added line in "lp.height".
     * Lines below the last one are incredibly high.
     */
    /*private*/ static void botline_forw(lineoff_C lp)
    {
        lp.lnum++;
        if (curbuf.b_ml.ml_line_count < lp.lnum)
            lp.height = MAXCOL;
        else
            lp.height = plines(lp.lnum);
    }

    /*
     * Recompute topline to put the cursor at the top of the window.
     * Scroll at least "min_scroll" lines.
     * If "always" is true, always set topline (for "zt").
     */
    /*private*/ static void scroll_cursor_top(int min_scroll, boolean always)
    {
        int scrolled = 0;
        int extra = 0;
        long old_topline = curwin.w_topline;

        int off = (int)p_so[0];
        if (0 < mouse_dragging)
            off = mouse_dragging - 1;

        /*
         * Decrease topline until:
         * - it has become 1
         * - (part of) the cursor line is moved off the screen or
         * - moved at least 'scrolljump' lines and
         * - at least 'scrolloff' lines above and below the cursor
         */
        validate_cheight();
        int used = curwin.w_cline_height;
        if (curwin.w_cursor.lnum < curwin.w_topline)
            scrolled = used;

        long top = curwin.w_cursor.lnum - 1;    /* just above displayed lines */
        long bot = curwin.w_cursor.lnum + 1;    /* just below displayed lines */
        long new_topline = top + 1;

        /*
         * Check if the lines from "top" to "bot" fit in the window.  If they do,
         * set new_topline and advance "top" and "bot" to include more lines.
         */
        while (0 < top)
        {
            int i = plines(top);
            used += i;
            if (extra + i <= off && bot < curbuf.b_ml.ml_line_count)
            {
                used += plines(bot);
            }
            if (curwin.w_height < used)
                break;
            if (top < curwin.w_topline)
                scrolled += i;

            /*
             * If scrolling is needed, scroll at least 'sj' lines.
             */
            if ((curwin.w_topline <= new_topline || min_scroll < scrolled) && off <= extra)
                break;

            extra += i;
            new_topline = top;
            --top;
            bot++;
        }

        /*
         * If we don't have enough space, put cursor in the middle.
         * This makes sure we get the same position when using "k" and "j" in a small window.
         */
        if (curwin.w_height < used)
            scroll_cursor_halfway(false);
        else
        {
            /*
             * If "always" is false, only adjust topline to a lower value, higher
             * value may happen with wrapping lines
             */
            if (new_topline < curwin.w_topline || always)
                curwin.w_topline = new_topline;
            if (curwin.w_topline > curwin.w_cursor.lnum)
                curwin.w_topline = curwin.w_cursor.lnum;
            if (curwin.w_topline != old_topline)
                curwin.w_valid &= ~(VALID_WROW|VALID_CROW|VALID_BOTLINE|VALID_BOTLINE_AP);
            curwin.w_valid |= VALID_TOPLINE;
        }
    }

    /*
     * Set w_empty_rows and w_filler_rows for window "wp", having used up "used"
     * screen lines for text lines.
     */
    /*private*/ static void set_empty_rows(window_C wp, int used)
    {
        if (used == 0)
            wp.w_empty_rows = 0;    /* single line that doesn't fit */
        else
            wp.w_empty_rows = wp.w_height - used;
    }

    /*
     * Recompute topline to put the cursor at the bottom of the window.
     * Scroll at least "min_scroll" lines.
     * If "set_topbot" is true, set topline and botline first (for "zb").
     * This is messy stuff!!!
     */
    /*private*/ static void scroll_cursor_bot(int min_scroll, boolean set_topbot)
    {
        int scrolled = 0;
        int extra = 0;
        long old_topline = curwin.w_topline;
        long old_botline = curwin.w_botline;
        int old_valid = curwin.w_valid;
        int old_empty_rows = curwin.w_empty_rows;

        lineoff_C loff = new lineoff_C();
        lineoff_C boff = new lineoff_C();

        long cln = curwin.w_cursor.lnum;            /* Cursor Line Number */
        if (set_topbot)
        {
            int used = 0;
            curwin.w_botline = cln + 1;
            for (curwin.w_topline = curwin.w_botline; 1 < curwin.w_topline; curwin.w_topline = loff.lnum)
            {
                loff.lnum = curwin.w_topline;
                topline_back(loff);
                if (loff.height == MAXCOL || curwin.w_height < used + loff.height)
                    break;
                used += loff.height;
            }
            set_empty_rows(curwin, used);
            curwin.w_valid |= VALID_BOTLINE|VALID_BOTLINE_AP;
            if (curwin.w_topline != old_topline)
                curwin.w_valid &= ~(VALID_WROW|VALID_CROW);
        }
        else
            validate_botline();

        /* The lines of the cursor line itself are always used. */
        validate_cheight();
        int used = curwin.w_cline_height;

        /* If the cursor is below botline, we will at least scroll by the height of the cursor line.
         * Correct for empty lines, which are really part of botline. */
        if (curwin.w_botline <= cln)
        {
            scrolled = used;
            if (cln == curwin.w_botline)
                scrolled -= curwin.w_empty_rows;
        }

        /*
         * Stop counting lines to scroll when
         * - hitting start of the file
         * - scrolled nothing or at least 'sj' lines
         * - at least 'so' lines below the cursor
         * - lines between botline and cursor have been counted
         */
        loff.lnum = cln;
        boff.lnum = cln;

        while (1 < loff.lnum)
        {
            /* Stop when scrolled nothing or at least "min_scroll", found "extra"
             * context for 'scrolloff' and counted all lines below the window. */
            if ((((scrolled <= 0 || min_scroll <= scrolled)
                            && (0 < mouse_dragging ? mouse_dragging - 1 : p_so[0]) <= extra)
                        || curbuf.b_ml.ml_line_count < boff.lnum + 1)
                    && loff.lnum <= curwin.w_botline)
                break;

            /* Add one line above. */
            topline_back(loff);
            if (loff.height == MAXCOL)
                used = MAXCOL;
            else
                used += loff.height;
            if (curwin.w_height < used)
                break;
            if (curwin.w_botline <= loff.lnum)
            {
                /* Count screen lines that are below the window. */
                scrolled += loff.height;
                if (loff.lnum == curwin.w_botline)
                    scrolled -= curwin.w_empty_rows;
            }

            if (boff.lnum < curbuf.b_ml.ml_line_count)
            {
                /* Add one line below. */
                botline_forw(boff);
                used += boff.height;
                if (curwin.w_height < used)
                    break;
                if (extra < (0 < mouse_dragging ? mouse_dragging - 1 : p_so[0]) || scrolled < min_scroll)
                {
                    extra += boff.height;
                    if (curwin.w_botline <= boff.lnum)
                    {
                        /* Count screen lines that are below the window. */
                        scrolled += boff.height;
                        if (boff.lnum == curwin.w_botline)
                            scrolled -= curwin.w_empty_rows;
                    }
                }
            }
        }

        long line_count;
        /* curwin.w_empty_rows is larger, no need to scroll */
        if (scrolled <= 0)
            line_count = 0;
        /* more than a screenfull, don't scroll but redraw */
        else if (curwin.w_height < used)
            line_count = used;
        /* scroll minimal number of lines */
        else
        {
            line_count = 0;
            boff.lnum = curwin.w_topline - 1;
            int i;
            for (i = 0; i < scrolled && boff.lnum < curwin.w_botline; )
            {
                botline_forw(boff);
                i += boff.height;
                line_count++;
            }
            if (i < scrolled)       /* below curwin.w_botline, don't scroll */
                line_count = 9999;
        }

        /*
         * Scroll up if the cursor is off the bottom of the screen a bit.
         * Otherwise put it at 1/2 of the screen.
         */
        if (curwin.w_height <= line_count && min_scroll < line_count)
            scroll_cursor_halfway(false);
        else
            scrollup(line_count);

        /*
         * If topline didn't change we need to restore w_botline and w_empty_rows (we changed them).
         * If topline did change, update_screen() will set botline.
         */
        if (curwin.w_topline == old_topline && set_topbot)
        {
            curwin.w_botline = old_botline;
            curwin.w_empty_rows = old_empty_rows;
            curwin.w_valid = old_valid;
        }
        curwin.w_valid |= VALID_TOPLINE;
    }

    /*
     * Recompute topline to put the cursor halfway the window
     * If "atend" is true, also put it halfway at the end of the file.
     */
    /*private*/ static void scroll_cursor_halfway(boolean atend)
    {
        lineoff_C loff = new lineoff_C();
        lineoff_C boff = new lineoff_C();

        loff.lnum = boff.lnum = curwin.w_cursor.lnum;
        int used = plines(loff.lnum);
        long topline = loff.lnum;

        for (int above = 0, below = 0; 1 < topline; )
        {
            if (below <= above)         /* add a line below the cursor first */
            {
                if (boff.lnum < curbuf.b_ml.ml_line_count)
                {
                    botline_forw(boff);
                    used += boff.height;
                    if (curwin.w_height < used)
                        break;
                    below += boff.height;
                }
                else
                {
                    below++;            /* count a "~" line */
                    if (atend)
                        used++;
                }
            }

            if (above < below)          /* add a line above the cursor */
            {
                topline_back(loff);
                if (loff.height == MAXCOL)
                    used = MAXCOL;
                else
                    used += loff.height;
                if (curwin.w_height < used)
                    break;
                above += loff.height;
                topline = loff.lnum;
            }
        }

        curwin.w_topline = topline;
        curwin.w_valid &= ~(VALID_WROW|VALID_CROW|VALID_BOTLINE|VALID_BOTLINE_AP);
        curwin.w_valid |= VALID_TOPLINE;
    }

    /*
     * Correct the cursor position so that it is in a part of the screen at least
     * 'so' lines from the top and bottom, if possible.
     * If not possible, put it at the same position as scroll_cursor_halfway().
     * When called topline must be valid!
     */
    /*private*/ static void cursor_correct()
    {
        /*
         * How many lines we would like to have above/below the cursor depends on
         * whether the first/last line of the file is on screen.
         */
        int above_wanted = (int)p_so[0];
        int below_wanted = (int)p_so[0];
        if (0 < mouse_dragging)
        {
            above_wanted = mouse_dragging - 1;
            below_wanted = mouse_dragging - 1;
        }
        if (curwin.w_topline == 1)
        {
            above_wanted = 0;
            int max_off = curwin.w_height / 2;
            if (max_off < below_wanted)
                below_wanted = max_off;
        }
        validate_botline();
        if (curwin.w_botline == curbuf.b_ml.ml_line_count + 1 && mouse_dragging == 0)
        {
            below_wanted = 0;
            int max_off = (curwin.w_height - 1) / 2;
            if (max_off < above_wanted)
                above_wanted = max_off;
        }

        /*
         * If there are sufficient file-lines above and below the cursor, we can return now.
         */
        long cln = curwin.w_cursor.lnum; /* Cursor Line Number */
        if (curwin.w_topline + above_wanted <= cln && cln < curwin.w_botline - below_wanted)
            return;

        int above = 0;          /* screen lines above topline */
        int below = 0;          /* screen lines below botline */
        /*
         * Narrow down the area where the cursor can be put by taking lines from
         * the top and the bottom until:
         * - the desired context lines are found
         * - the lines from the top is past the lines from the bottom
         */
        long topline = curwin.w_topline;
        long botline = curwin.w_botline - 1;
        while ((above < above_wanted || below < below_wanted) && topline < botline)
        {
            if (below < below_wanted && (below <= above || above_wanted <= above))
            {
                below += plines(botline);
                --botline;
            }
            if (above < above_wanted && (above < below || below_wanted <= below))
            {
                above += plines(topline);
                topline++;
            }
        }
        if (topline == botline || botline == 0)
            curwin.w_cursor.lnum = topline;
        else if (botline < topline)
            curwin.w_cursor.lnum = botline;
        else
        {
            if (cln < topline && 1 < curwin.w_topline)
            {
                curwin.w_cursor.lnum = topline;
                curwin.w_valid &= ~(VALID_WROW|VALID_WCOL|VALID_CHEIGHT|VALID_CROW);
            }
            if (botline < cln && curwin.w_botline <= curbuf.b_ml.ml_line_count)
            {
                curwin.w_cursor.lnum = botline;
                curwin.w_valid &= ~(VALID_WROW|VALID_WCOL|VALID_CHEIGHT|VALID_CROW);
            }
        }
        curwin.w_valid |= VALID_TOPLINE;
    }

    /*
     * move screen 'count' pages up or down and update screen
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean onepage(int dir, long count)
    {
        boolean retval = true;

        long old_topline = curwin.w_topline;

        if (curbuf.b_ml.ml_line_count == 1) /* nothing to do */
        {
            beep_flush();
            return false;
        }

        lineoff_C loff = new lineoff_C();

        for ( ; 0 < count; --count)
        {
            validate_botline();
            /*
             * It's an error to move a page up when the first line is already on
             * the screen.  It's an error to move a page down when the last line
             * is on the screen and the topline is 'scrolloff' lines from the last line.
             */
            if (dir == FORWARD
                    ? (curbuf.b_ml.ml_line_count - p_so[0] <= curwin.w_topline
                        && curbuf.b_ml.ml_line_count < curwin.w_botline)
                    : (curwin.w_topline == 1))
            {
                beep_flush();
                retval = false;
                break;
            }

            if (dir == FORWARD)
            {
                if (firstwin == lastwin && 0 < p_window[0] && p_window[0] < Rows[0] - 1)
                {
                    /* Vi compatible scrolling. */
                    if (p_window[0] <= 2)
                        curwin.w_topline++;
                    else
                        curwin.w_topline += p_window[0] - 2;
                    if (curwin.w_topline > curbuf.b_ml.ml_line_count)
                        curwin.w_topline = curbuf.b_ml.ml_line_count;
                    curwin.w_cursor.lnum = curwin.w_topline;
                }
                else if (curbuf.b_ml.ml_line_count < curwin.w_botline)
                {
                    /* at end of file */
                    curwin.w_topline = curbuf.b_ml.ml_line_count;
                    curwin.w_valid &= ~(VALID_WROW|VALID_CROW);
                }
                else
                {
                    /* For the overlap, start with the line just below the window and go upwards. */
                    loff.lnum = curwin.w_botline;
                    get_scroll_overlap(loff, -1);
                    curwin.w_topline = loff.lnum;
                    curwin.w_cursor.lnum = curwin.w_topline;
                    curwin.w_valid &= ~(VALID_WCOL|VALID_CHEIGHT|VALID_WROW|
                                       VALID_CROW|VALID_BOTLINE|VALID_BOTLINE_AP);
                }
            }
            else    /* dir == BACKWARDS */
            {
                if (firstwin == lastwin && 0 < p_window[0] && p_window[0] < Rows[0] - 1)
                {
                    /* Vi compatible scrolling (sort of). */
                    if (p_window[0] <= 2)
                        --curwin.w_topline;
                    else
                        curwin.w_topline -= p_window[0] - 2;
                    if (curwin.w_topline < 1)
                        curwin.w_topline = 1;
                    curwin.w_cursor.lnum = curwin.w_topline + p_window[0] - 1;
                    if (curwin.w_cursor.lnum > curbuf.b_ml.ml_line_count)
                        curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
                    continue;
                }

                /* Find the line at the top of the window that is going to be the
                 * line at the bottom of the window.  Make sure this results in
                 * the same line as before doing CTRL-F. */
                loff.lnum = curwin.w_topline - 1;
                get_scroll_overlap(loff, 1);

                if (loff.lnum > curbuf.b_ml.ml_line_count)
                    loff.lnum = curbuf.b_ml.ml_line_count;
                curwin.w_cursor.lnum = loff.lnum;

                /* Find the line just above the new topline to get the right line
                 * at the bottom of the window. */
                long n = 0;
                while (n <= curwin.w_height && 1 <= loff.lnum)
                {
                    topline_back(loff);
                    if (loff.height == MAXCOL)
                        n = MAXCOL;
                    else
                        n += loff.height;
                }
                if (loff.lnum < 1)      /* at begin of file */
                {
                    curwin.w_topline = 1;
                    curwin.w_valid &= ~(VALID_WROW|VALID_CROW|VALID_BOTLINE);
                }
                else
                {
                    /* Go two lines forward again. */
                    botline_forw(loff);
                    botline_forw(loff);

                    /* Always scroll at least one line.  Avoid getting stuck on very long lines. */
                    if (curwin.w_topline <= loff.lnum)
                    {
                        --curwin.w_topline;

                        comp_botline(curwin);
                        curwin.w_cursor.lnum = curwin.w_botline - 1;
                        curwin.w_valid &= ~(VALID_WCOL|VALID_CHEIGHT|VALID_WROW|VALID_CROW);
                    }
                    else
                    {
                        curwin.w_topline = loff.lnum;
                        curwin.w_valid &= ~(VALID_WROW|VALID_CROW|VALID_BOTLINE);
                    }
                }
            }
        }

        cursor_correct();
        if (retval == true)
            beginline(BL_SOL | BL_FIX);
        curwin.w_valid &= ~(VALID_WCOL|VALID_WROW|VALID_VIRTCOL);

        /*
         * Avoid the screen jumping up and down when 'scrolloff' is non-zero.
         * But make sure we scroll at least one line (happens with mix of long
         * wrapping lines and non-wrapping line).
         */
        if (retval == true && dir == FORWARD && check_top_offset())
        {
            scroll_cursor_top(1, false);
            if (curwin.w_topline <= old_topline && old_topline < curbuf.b_ml.ml_line_count)
            {
                curwin.w_topline = old_topline + 1;
            }
        }

        redraw_later(VALID);
        return retval;
    }

    /*
     * Decide how much overlap to use for page-up or page-down scrolling.
     * This is symmetric, so that doing both keeps the same lines displayed.
     * Three lines are examined:
     *
     *  before CTRL-F           after CTRL-F / before CTRL-B
     *     etc.                     l1
     *  l1 last but one line        ------------
     *  l2 last text line           l2 top text line
     *  -------------               l3 second text line
     *  l3                             etc.
     */
    /*private*/ static void get_scroll_overlap(lineoff_C lp, int dir)
    {
        int min_height = curwin.w_height - 2;

        lp.height = plines(lp.lnum);
        int h1 = lp.height;
        if (min_height < h1)
            return;         /* no overlap */

        lineoff_C loff0 = new lineoff_C();
        COPY_lineoff(loff0, lp);
        if (0 < dir)
            botline_forw(lp);
        else
            topline_back(lp);
        int h2 = lp.height;
        if (h2 == MAXCOL || min_height < h2 + h1)
        {
            COPY_lineoff(lp, loff0);    /* no overlap */
            return;
        }

        lineoff_C loff1 = new lineoff_C();
        COPY_lineoff(loff1, lp);
        if (0 < dir)
            botline_forw(lp);
        else
            topline_back(lp);
        int h3 = lp.height;
        if (h3 == MAXCOL || min_height < h3 + h2)
        {
            COPY_lineoff(lp, loff0);    /* no overlap */
            return;
        }

        lineoff_C loff2 = new lineoff_C();
        COPY_lineoff(loff2, lp);
        if (0 < dir)
            botline_forw(lp);
        else
            topline_back(lp);
        int h4 = lp.height;
        if (h4 == MAXCOL || min_height < h4 + h3 + h2 || min_height < h3 + h2 + h1)
            COPY_lineoff(lp, loff1);    /* 1 line overlap */
        else
            COPY_lineoff(lp, loff2);    /* 2 lines overlap */
    }

    /*
     * Scroll 'scroll' lines up or down.
     */
    /*private*/ static void halfpage(boolean flag, long Prenum)
    {
        long scrolled = 0;

        if (Prenum != 0)
            curwin.w_onebuf_opt.wo_scr[0] = (curwin.w_height < Prenum) ? curwin.w_height : Prenum;
        int n = ((int)curwin.w_onebuf_opt.wo_scr[0] <= curwin.w_height) ? (int)curwin.w_onebuf_opt.wo_scr[0] : curwin.w_height;

        validate_botline();
        int room = curwin.w_empty_rows;
        if (flag)
        {
            /*
             * scroll the text up
             */
            while (0 < n && curwin.w_botline <= curbuf.b_ml.ml_line_count)
            {
                int i = plines(curwin.w_topline);
                n -= i;
                if (n < 0 && 0 < scrolled)
                    break;
                curwin.w_topline++;

                if (curwin.w_cursor.lnum < curbuf.b_ml.ml_line_count)
                {
                    curwin.w_cursor.lnum++;
                    curwin.w_valid &= ~(VALID_VIRTCOL|VALID_CHEIGHT|VALID_WCOL);
                }

                curwin.w_valid &= ~(VALID_CROW|VALID_WROW);
                scrolled += i;

                /*
                 * Correct w_botline for changed w_topline.
                 * Won't work when there are filler lines.
                 */
                room += i;
                do
                {
                    i = plines(curwin.w_botline);
                    if (room < i)
                        break;
                    curwin.w_botline++;
                    room -= i;
                } while (curwin.w_botline <= curbuf.b_ml.ml_line_count);
            }

            /*
             * When hit bottom of the file: move cursor down.
             */
            if (0 < n)
            {
                curwin.w_cursor.lnum += n;
                check_cursor_lnum();
            }
        }
        else
        {
            /*
             * scroll the text down
             */
            while (0 < n && 1 < curwin.w_topline)
            {
                int i = plines(curwin.w_topline - 1);
                n -= i;
                if (n < 0 && 0 < scrolled)
                    break;
                --curwin.w_topline;

                curwin.w_valid &= ~(VALID_CROW|VALID_WROW|VALID_BOTLINE|VALID_BOTLINE_AP);
                scrolled += i;
                if (1 < curwin.w_cursor.lnum)
                {
                    --curwin.w_cursor.lnum;
                    curwin.w_valid &= ~(VALID_VIRTCOL|VALID_CHEIGHT|VALID_WCOL);
                }
            }
            /*
             * When hit top of the file: move cursor up.
             */
            if (0 < n)
            {
                if (curwin.w_cursor.lnum <= (long)n)
                    curwin.w_cursor.lnum = 1;
                else
                    curwin.w_cursor.lnum -= n;
            }
        }
        cursor_correct();
        beginline(BL_SOL | BL_FIX);
        redraw_later(VALID);
    }

    /*private*/ static void do_check_cursorbind()
    {
        long line = curwin.w_cursor.lnum;
        int col = curwin.w_cursor.col;
        int coladd = curwin.w_cursor.coladd;
        int curswant = curwin.w_curswant;
        boolean set_curswant = curwin.w_set_curswant;

        window_C old_curwin = curwin;
        buffer_C old_curbuf = curbuf;
        boolean old_VIsual_select = VIsual_select;
        boolean old_VIsual_active = VIsual_active;

        /*
         * loop through the cursorbound windows
         */
        VIsual_select = VIsual_active = false;
        for (curwin = firstwin; curwin != null; curwin = curwin.w_next)
        {
            curbuf = curwin.w_buffer;
            /* skip original window  and windows with 'noscrollbind' */
            if (curwin != old_curwin && curwin.w_onebuf_opt.wo_crb[0])
            {
                curwin.w_cursor.lnum = line;
                curwin.w_cursor.col = col;
                curwin.w_cursor.coladd = coladd;
                curwin.w_curswant = curswant;
                curwin.w_set_curswant = set_curswant;

                /* Make sure the cursor is in a valid position.  Temporarily set
                 * "restart_edit" to allow the cursor to be beyond the EOL. */
                int restart_edit_save = restart_edit;
                restart_edit = TRUE;
                check_cursor();
                restart_edit = restart_edit_save;
                /* Correct cursor for multi-byte character. */
                mb_adjust_pos(curbuf, curwin.w_cursor);
                redraw_later(VALID);

                /* Only scroll when 'scrollbind' hasn't done this. */
                if (!curwin.w_onebuf_opt.wo_scb[0])
                    update_topline();
                curwin.w_redr_status = true;
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
}
