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
public class VimM
{
    /*
     * edit.c: functions for Insert mode --------------------------------------------------------------
     */

    /*private*/ static final int BACKSPACE_CHAR              = 1;
    /*private*/ static final int BACKSPACE_WORD              = 2;
    /*private*/ static final int BACKSPACE_WORD_NOT_SPACE    = 3;
    /*private*/ static final int BACKSPACE_LINE              = 4;

    /*private*/ static int      insStart_textlen;               /* length of line when insert started */
    /*private*/ static int      insStart_blank_vcol;            /* vcol for first inserted blank */
    /*private*/ static boolean  update_insStart_orig = true;    /* set insStart_orig to insStart */

    /*private*/ static Bytes    last_insert;                    /* the text of the previous insert,
                                                             * KB_SPECIAL and CSI are escaped */
    /*private*/ static int      last_insert_skip;               /* nr of chars in front of previous insert */
    /*private*/ static int      new_insert_skip;                /* nr of chars in front of current insert */
    /*private*/ static int      did_restart_edit;               /* "restart_edit" when calling edit() */

    /*private*/ static boolean  can_cindent;                    /* may do cindenting on this line */

    /*private*/ static int      old_indent;                     /* for ^^D command in insert mode */

    /*private*/ static boolean  revins_on;                      /* reverse insert mode on */
    /*private*/ static int      revins_chars;                   /* how much to skip after edit */
    /*private*/ static int      revins_legal;                   /* was the last char 'legal' */
    /*private*/ static int      revins_scol;                    /* start column of revins session */

    /*private*/ static boolean  ins_need_undo;                  /* call u_save() before inserting a char;
                                                             * set when edit() is called;
                                                             * after that arrow_used is used */

    /*private*/ static boolean  did_add_space;          /* auto_format() added an extra space under the cursor */

    /*private*/ static long     o_lnum;

    /*
     * edit(): Start inserting text.
     *
     * "cmdchar" can be:
     * 'i'  normal insert command
     * 'a'  normal append command
     * 'R'  replace command
     * 'r'  "r<CR>" command: insert one <CR>.  Note: count can be > 1, for redo,
     *      but still only one <CR> is inserted.  The <Esc> is not used for redo.
     * 'g'  "gI" command.
     * 'V'  "gR" command for Virtual Replace mode.
     * 'v'  "gr" command for single character Virtual Replace mode.
     *
     * This function is not called recursively.  For CTRL-O commands, it returns
     * and lets the caller handle the Normal-mode command.
     *
     * Return true if a CTRL-O command caused the return (insert mode pending).
     */
    /*private*/ static boolean edit(int cmdchar, boolean startln, long _count)
        /* startln: if set, insert at start of line */
    {
        long[] count = { _count };

        boolean did_backspace = true;           /* previous char was backspace */
        boolean line_is_white = false;          /* line is empty before insert */
        long old_topline = 0;                   /* topline before insertion */
        boolean[] inserted_space = { false };         /* just inserted a space */
        boolean nomove = false;                 /* don't move cursor on return */

        /* Remember whether editing was restarted after CTRL-O. */
        did_restart_edit = restart_edit;

        /* sleep before redrawing, needed for "CTRL-O :" that results in an error message */
        check_for_delay(true);

        /* set insStart_orig to insStart */
        update_insStart_orig = true;

        /* Don't allow inserting in the sandbox. */
        if (sandbox != 0)
        {
            emsg(e_sandbox);
            return false;
        }
        /* Don't allow changes in the buffer while editing the cmdline.
         * The caller of getcmdline() may get confused. */
        if (textlock != 0)
        {
            emsg(e_secure);
            return false;
        }

        /*
         * Trigger InsertEnter autocommands.  Do not do this for "r<CR>" or "grx".
         */
        if (cmdchar != 'r' && cmdchar != 'v')
        {
            pos_C save_cursor = new pos_C();
            COPY_pos(save_cursor, curwin.w_cursor);

            Bytes p;
            if (cmdchar == 'R')
                p = u8("r");
            else if (cmdchar == 'V')
                p = u8("v");
            else
                p = u8("i");
            set_vim_var_string(VV_INSERTMODE, p, 1);
            set_vim_var_string(VV_CHAR, null, -1);  /* clear v:char */
            apply_autocmds(EVENT_INSERTENTER, null, null, false, curbuf);

            /* Make sure the cursor didn't move.  Do call check_cursor_col() in
             * case the text was modified.  Since Insert mode was not started yet
             * a call to check_cursor_col() may move the cursor, especially with
             * the "A" command, thus set State to avoid that.  Also check that the
             * line number is still valid (lines may have been deleted).
             * Do not restore if v:char was set to a non-empty string. */
            if (!eqpos(curwin.w_cursor, save_cursor)
                    && get_vim_var_str(VV_CHAR).at(0) == NUL
                    && save_cursor.lnum <= curbuf.b_ml.ml_line_count)
            {
                int save_state = State;

                COPY_pos(curwin.w_cursor, save_cursor);
                State = INSERT;
                check_cursor_col();
                State = save_state;
            }
        }

        /* Check if the cursor line needs redrawing before changing State.
         * If 'concealcursor' is "n", it needs to be redrawn without concealing. */
        conceal_check_cursor_line();

        /*
         * When doing a paste with the middle mouse button,
         * insStart is set to where the paste started.
         */
        if (where_paste_started.lnum != 0)
            COPY_pos(insStart, where_paste_started);
        else
        {
            COPY_pos(insStart, curwin.w_cursor);
            if (startln)
                insStart.col = 0;
        }
        insStart_textlen = linetabsize(ml_get_curline());
        insStart_blank_vcol = MAXCOL;
        if (!did_ai)
            ai_col = 0;

        if (cmdchar != NUL && restart_edit == 0)
        {
            resetRedobuff();
            appendNumberToRedobuff(count[0]);
            if (cmdchar == 'V' || cmdchar == 'v')
            {
                /* "gR" or "gr" command */
                appendCharToRedobuff('g');
                appendCharToRedobuff((cmdchar == 'v') ? 'r' : 'R');
            }
            else
            {
                appendCharToRedobuff(cmdchar);
                if (cmdchar == 'g')             /* "gI" command */
                    appendCharToRedobuff('I');
                else if (cmdchar == 'r')        /* "r<CR>" command */
                    count[0] = 1;                  /* insert only one <CR> */
            }
        }

        int replaceState = REPLACE;
        if (cmdchar == 'R')
        {
            State = REPLACE;
        }
        else if (cmdchar == 'V' || cmdchar == 'v')
        {
            State = VREPLACE;
            replaceState = VREPLACE;
            orig_line_count = curbuf.b_ml.ml_line_count;
            vr_lines_changed = 1;
        }
        else
            State = INSERT;

        stop_insert_mode = false;

        /*
         * Need to recompute the cursor position,
         * it might move when the cursor is on a TAB or special character.
         */
        curs_columns(true);

        /*
         * Enable langmap or IME, indicated by 'iminsert'.
         * Note that IME may enabled/disabled without us noticing here,
         * thus the 'iminsert' value may not reflect what is actually used.
         * It is updated when hitting <Esc>.
         */
        if (curbuf.b_p_iminsert[0] == B_IMODE_LMAP)
            State |= LANGMAP;

        setmouse();
        clear_showcmd();
        /* there is no reverse replace mode */
        revins_on = (State == INSERT && p_ri[0]);
        if (revins_on)
            undisplay_dollar();
        revins_chars = 0;
        revins_legal = 0;
        revins_scol = -1;

        /*
         * Handle restarting Insert mode.
         * Don't do this for "CTRL-O ." (repeat an insert): we get here with
         * restart_edit non-zero, and something in the stuff buffer.
         */
        if (restart_edit != 0 && stuff_empty())
        {
            /*
             * After a paste we consider text typed to be part of the insert for
             * the pasted text.  You can backspace over the pasted text too.
             */
            arrow_used = (where_paste_started.lnum == 0);
            restart_edit = 0;

            /*
             * If the cursor was after the end-of-line before the CTRL-O and it is
             * now at the end-of-line, put it after the end-of-line (this is not
             * correct in very rare cases).
             * Also do this if curswant is greater than the current virtual column.
             * Eg after "^O$" or "^O80|".
             */
            validate_virtcol();
            update_curswant();
            if ((ins_at_eol && curwin.w_cursor.lnum == o_lnum) || curwin.w_virtcol < curwin.w_curswant)
            {
                Bytes p = ml_get_curline().plus(curwin.w_cursor.col);
                if (p.at(0) != NUL)
                {
                    if (p.at(1) == NUL)
                        curwin.w_cursor.col++;
                    else
                    {
                        int n = us_ptr2len_cc(p);
                        if (p.at(n) == NUL)
                            curwin.w_cursor.col += n;
                    }
                }
            }
            ins_at_eol = false;
        }
        else
            arrow_used = false;

        /* We are in insert mode now, don't need to start it anymore. */
        need_start_insertmode = false;

        /* Need to save the line for undo before inserting the first char. */
        ins_need_undo = true;

        where_paste_started.lnum = 0;
        can_cindent = true;

        /*
         * If 'showmode' is set, show the current (insert/replace/..) mode.
         * A warning message for changing a readonly file is given here, before
         * actually changing anything.  It's put after the mode, if any.
         */
        int i = 0;
        if (p_smd[0] && msg_silent == 0)
            i = showmode();

        if (!p_im[0] && did_restart_edit == 0)
            change_warning(i == 0 ? 0 : i + 1);

        ui_cursor_shape();          /* may show different cursor shape */
        do_digraph(-1);             /* clear digraphs */

        /*
         * Get the current length of the redo buffer,
         * those characters have to be skipped if we want to get to the inserted characters.
         */
        Bytes ptr = get_inserted();
        if (ptr == null)
            new_insert_skip = 0;
        else
            new_insert_skip = strlen(ptr);

        old_indent = 0;

        /*
         * Main loop in Insert mode: repeat until Insert mode is left.
         */
        for (int lastc = 0, c = 0; ; )
        {
            if (revins_legal == 0)
                revins_scol = -1;       /* reset on illegal motions */
            else
                revins_legal = 0;
            if (arrow_used)     /* don't repeat insert when arrow key used */
                count[0] = 0;

            if (update_insStart_orig)
                COPY_pos(insStart_orig, insStart);

            doESCkey:
            {
                if (stop_insert_mode)
                {
                    /* ":stopinsert" used or 'insertmode' reset */
                    count[0] = 0;
                    break doESCkey;
                }

                /* set curwin.w_curswant for next K_DOWN or K_UP */
                if (!arrow_used)
                    curwin.w_set_curswant = true;

                /* If there is no typeahead may check for timestamps
                 * (e.g., for when a menu invoked a shell command). */
                if (stuff_empty())
                {
                    did_check_timestamps = false;
                    if (need_check_timestamps)
                        check_timestamps(false);
                }

                /*
                 * When emsg() was called msg_scroll will have been set.
                 */
                msg_scroll = false;

                /*
                 * If we inserted a character at the last position of the last line in the window,
                 * scroll the window one line up.  This avoids an extra redraw.
                 * This is detected when the cursor column is smaller after inserting something.
                 * Don't do this when the topline changed already,
                 * it has already been adjusted (by insertchar() calling open_line())).
                 */
                if (curbuf.b_mod_set && curwin.w_onebuf_opt.wo_wrap[0] && !did_backspace && curwin.w_topline == old_topline)
                {
                    int mincol = curwin.w_wcol;
                    validate_cursor_col();

                    if (curwin.w_wcol < mincol - curbuf.b_p_ts[0]
                        && curwin.w_wrow == curwin.w_winrow + curwin.w_height - 1 - p_so[0]
                        && curwin.w_cursor.lnum != curwin.w_topline)
                    {
                        set_topline(curwin, curwin.w_topline + 1);
                    }
                }

                /* May need to adjust w_topline to show the cursor. */
                update_topline();

                did_backspace = false;

                validate_cursor();              /* may set must_redraw */

                /*
                 * Redraw the display when no characters are waiting.
                 * Also shows mode, ruler and positions cursor.
                 */
                ins_redraw(true);

                if (curwin.w_onebuf_opt.wo_scb[0])
                    do_check_scrollbind(true);

                if (curwin.w_onebuf_opt.wo_crb[0])
                    do_check_cursorbind();
                update_curswant();
                old_topline = curwin.w_topline;

                /*
                 * Get a character for Insert mode.  Ignore K_IGNORE.
                 */
                if (c != K_CURSORHOLD)
                    lastc = c;          /* remember the previous char for CTRL-D */
                do
                {
                    c = safe_vgetc();
                } while (c == K_IGNORE);

                /* Don't want K_CURSORHOLD for the second key, e.g., after CTRL-V. */
                did_cursorhold = true;

                /* CTRL-\ CTRL-N goes to Normal mode,
                 * CTRL-\ CTRL-G goes to mode selected with 'insertmode',
                 * CTRL-\ CTRL-O is like CTRL-O but without moving the cursor. */
                if (c == Ctrl_BSL)
                {
                    /* may need to redraw when no more chars available now */
                    ins_redraw(false);

                    no_mapping++;
                    allow_keys++;
                    c = plain_vgetc();
                    --no_mapping;
                    --allow_keys;

                    if (c != Ctrl_N && c != Ctrl_G && c != Ctrl_O)
                    {
                        /* it's something else */
                        vungetc(c);
                        c = Ctrl_BSL;
                    }
                    else if (c == Ctrl_G && p_im[0])
                        continue;
                    else
                    {
                        if (c == Ctrl_O)
                        {
                            ins_ctrl_o();
                            ins_at_eol = false; /* cursor keeps its column */
                            nomove = true;
                        }
                        count[0] = 0;
                        break doESCkey;
                    }
                }

                c = do_digraph(c);

                if (c == Ctrl_V || c == Ctrl_Q)
                {
                    ins_ctrl_v();
                    c = Ctrl_V;         /* pretend CTRL-V is last typed character */
                    continue;
                }

                if (cindent_on())
                {
                    /* A key name preceded by a bang means this key is not to be inserted.
                     * Skip ahead to the re-indenting below.
                     * A key name preceded by a star means that indenting has to be done
                     * before inserting the key. */
                    line_is_white = inindent(0);
                    if (in_cinkeys(c, '!', line_is_white))
                    {
                        /* Indent now if a key was typed that is in 'cinkeys'. */
                        if (in_cinkeys(c, ' ', line_is_white) && stop_arrow())
                            do_c_expr_indent();
                        continue;
                    }
                    if (can_cindent && in_cinkeys(c, '*', line_is_white) && stop_arrow())
                        do_c_expr_indent();
                }

                if (curwin.w_onebuf_opt.wo_rl[0])
                    switch (c)
                    {
                        case K_LEFT:    c = K_RIGHT;   break;
                        case K_S_LEFT:  c = K_S_RIGHT; break;
                        case K_C_LEFT:  c = K_C_RIGHT; break;
                        case K_RIGHT:   c = K_LEFT;    break;
                        case K_S_RIGHT: c = K_S_LEFT;  break;
                        case K_C_RIGHT: c = K_C_LEFT;  break;
                    }

                /*
                 * If 'keymodel' contains "startsel", may start selection.
                 * If it does, a CTRL-O and c will be stuffed, we need to get these characters.
                 */
                if (ins_start_select(c))
                    continue;

                normalchar:
                {
                    /*
                     * The big switch to handle a character in insert mode.
                     */
                    switch (c)
                    {
                        case ESC:                           /* end input mode */
                            if (echeck_abbr(ESC + ABBR_OFF))
                                break normalchar;
                            /* FALLTHROUGH */

                        case Ctrl_C:                        /* end input mode */
                            if (c == Ctrl_C && cmdwin_type != 0)
                            {
                                /* Close the cmdline window. */
                                cmdwin_result = K_IGNORE;
                                got_int = false;            /* don't stop executing autocommands et al. */
                                nomove = true;
                                break doESCkey;
                            }

                            /* When 'insertmode' set, and not halfway a mapping, don't leave Insert mode. */
                            if (goto_im())
                            {
                                if (got_int)
                                {
                                    vgetc();                /* flush all buffers */
                                    got_int = false;
                                }
                                else
                                    vim_beep();
                                break normalchar;
                            }
                            break doESCkey;

                        case Ctrl_Z:                        /* suspend when 'insertmode' set */
                            if (!p_im[0])
                                break;            /* insert CTRL-Z as normal char */
                            stuffReadbuff(u8(":st\r"));
                            c = Ctrl_O;
                            /* FALLTHROUGH */

                        case Ctrl_O:                        /* execute one command */
                            if (echeck_abbr(Ctrl_O + ABBR_OFF))
                                break normalchar;
                            ins_ctrl_o();

                            /* Don't move the cursor left when 'virtualedit' has "onemore". */
                            if ((ve_flags[0] & VE_ONEMORE) != 0)
                            {
                                ins_at_eol = false;
                                nomove = true;
                            }
                            count[0] = 0;
                            break doESCkey;

                        case K_INS:                         /* toggle insert/replace mode */
                        case K_KINS:
                            ins_insert(replaceState);
                            break normalchar;

                        case K_SELECT:                      /* end of Select mode mapping - ignore */
                            break normalchar;

                        case K_HELP:                        /* Help key works like <ESC> <Help> */
                        case K_F1:
                        case K_XF1:
                            stuffcharReadbuff(K_HELP);
                            if (p_im[0])
                                need_start_insertmode = true;
                            break doESCkey;

                        case K_ZERO:                        /* insert the previously inserted text */
                        case NUL:
                        case Ctrl_A:
                            /* For ^@ the trailing ESC will end the insert, unless there is an error. */
                            if (!stuff_inserted(NUL, 1L, (c == Ctrl_A)) && c != Ctrl_A && !p_im[0])
                                break doESCkey;              /* quit insert mode */
                            inserted_space[0] = false;
                            break normalchar;

                        case Ctrl_R:                        /* insert the contents of a register */
                            ins_reg();
                            auto_format(false, true);
                            inserted_space[0] = false;
                            break normalchar;

                        case Ctrl_G:                        /* commands starting with CTRL-G */
                            ins_ctrl_g();
                            break normalchar;

                        case Ctrl_HAT:                      /* switch input mode and/or langmap */
                            ins_ctrl_hat();
                            break normalchar;

                        case Ctrl__:                        /* switch between languages */
                            if (!p_ari[0])
                                break;
                            ins_ctrl_();
                            break normalchar;

                        case Ctrl_D:                        /* make indent one shiftwidth smaller */
                            /* FALLTHROUGH */
                        case Ctrl_T:                        /* make indent one shiftwidth greater */
                            ins_shift(c, lastc);
                            auto_format(false, true);
                            inserted_space[0] = false;
                            break normalchar;

                        case K_DEL:                         /* delete character under the cursor */
                        case K_KDEL:
                            ins_del();
                            auto_format(false, true);
                            break normalchar;

                        case K_BS:                          /* delete character before the cursor */
                        case Ctrl_H:
                            did_backspace = ins_bs(c, BACKSPACE_CHAR, inserted_space);
                            auto_format(false, true);
                            break normalchar;

                        case Ctrl_W:                        /* delete word before the cursor */
                            did_backspace = ins_bs(c, BACKSPACE_WORD, inserted_space);
                            auto_format(false, true);
                            break normalchar;

                        case Ctrl_U:                        /* delete all inserted text in current line */
                            did_backspace = ins_bs(c, BACKSPACE_LINE, inserted_space);
                            auto_format(false, true);
                            inserted_space[0] = false;
                            break normalchar;

                        case K_LEFTMOUSE:                   /* mouse keys */
                        case K_LEFTMOUSE_NM:
                        case K_LEFTDRAG:
                        case K_LEFTRELEASE:
                        case K_LEFTRELEASE_NM:
                        case K_MIDDLEMOUSE:
                        case K_MIDDLEDRAG:
                        case K_MIDDLERELEASE:
                        case K_RIGHTMOUSE:
                        case K_RIGHTDRAG:
                        case K_RIGHTRELEASE:
                        case K_X1MOUSE:
                        case K_X1DRAG:
                        case K_X1RELEASE:
                        case K_X2MOUSE:
                        case K_X2DRAG:
                        case K_X2RELEASE:
                            ins_mouse(c);
                            break normalchar;

                        case K_MOUSEDOWN:                   /* default action for scroll wheel up: scroll up */
                            ins_mousescroll(MSCR_DOWN);
                            break normalchar;

                        case K_MOUSEUP:                     /* default action for scroll wheel down: scroll down */
                            ins_mousescroll(MSCR_UP);
                            break normalchar;

                        case K_MOUSELEFT:                   /* scroll wheel left */
                            ins_mousescroll(MSCR_LEFT);
                            break normalchar;

                        case K_MOUSERIGHT:                  /* scroll wheel right */
                            ins_mousescroll(MSCR_RIGHT);
                            break normalchar;

                        case K_IGNORE:                      /* something mapped to nothing */
                            break normalchar;

                        case K_CURSORHOLD:                  /* didn't type something for a while */
                            apply_autocmds(EVENT_CURSORHOLDI, null, null, false, curbuf);
                            did_cursorhold = true;
                            break normalchar;

                        case K_HOME:                        /* <Home> */
                        case K_KHOME:
                        case K_S_HOME:
                        case K_C_HOME:
                            ins_home(c);
                            break normalchar;

                        case K_END:                         /* <End> */
                        case K_KEND:
                        case K_S_END:
                        case K_C_END:
                            ins_end(c);
                            break normalchar;

                        case K_LEFT:                        /* <Left> */
                            if ((mod_mask & (MOD_MASK_SHIFT|MOD_MASK_CTRL)) != 0)
                                ins_s_left();
                            else
                                ins_left();
                            break normalchar;

                        case K_S_LEFT:                      /* <S-Left> */
                        case K_C_LEFT:
                            ins_s_left();
                            break normalchar;

                        case K_RIGHT:                       /* <Right> */
                            if ((mod_mask & (MOD_MASK_SHIFT|MOD_MASK_CTRL)) != 0)
                                ins_s_right();
                            else
                                ins_right();
                            break normalchar;

                        case K_S_RIGHT:                     /* <S-Right> */
                        case K_C_RIGHT:
                            ins_s_right();
                            break normalchar;

                        case K_UP:                          /* <Up> */
                            if ((mod_mask & MOD_MASK_SHIFT) != 0)
                                ins_pageup();
                            else
                                ins_up(false);
                            break normalchar;

                        case K_S_UP:                        /* <S-Up> */
                        case K_PAGEUP:
                        case K_KPAGEUP:
                            ins_pageup();
                            break normalchar;

                        case K_DOWN:                        /* <Down> */
                            if ((mod_mask & MOD_MASK_SHIFT) != 0)
                                ins_pagedown();
                            else
                                ins_down(false);
                            break normalchar;

                        case K_S_DOWN:                      /* <S-Down> */
                        case K_PAGEDOWN:
                        case K_KPAGEDOWN:
                            ins_pagedown();
                            break normalchar;

                        case K_DROP:                        /* drag-n-drop event */
                            ins_drop();
                            break normalchar;

                        case K_S_TAB:                       /* when not mapped, use like a normal TAB */
                            c = TAB;
                            /* FALLTHROUGH */
                        case TAB:                           /* TAB or Complete patterns along path */
                            inserted_space[0] = false;
                            if (ins_tab())
                                break;            /* insert TAB as a normal char */
                            auto_format(false, true);
                            break normalchar;

                        case K_KENTER:                      /* <Enter> */
                            c = CAR;
                            /* FALLTHROUGH */
                        case CAR:
                        case NL:
                            if (cmdwin_type != 0)
                            {
                                /* Execute the command in the cmdline window. */
                                cmdwin_result = CAR;
                                break doESCkey;
                            }
                            if (ins_eol(c) && !p_im[0])
                                break doESCkey;              /* out of memory */
                            auto_format(false, false);
                            inserted_space[0] = false;
                            break normalchar;

                        case Ctrl_K:                        /* digraph or keyword completion */
                            c = ins_digraph();
                            if (c == NUL)
                                break normalchar;
                            break;

                        case Ctrl_L:                        /* whole line completion after ^X */
                        {
                            /* CTRL-L with 'insertmode' set: Leave Insert mode. */
                            if (p_im[0])
                            {
                                if (echeck_abbr(Ctrl_L + ABBR_OFF))
                                    break normalchar;
                                break doESCkey;
                            }
                            break;
                        }

                        case Ctrl_Y:                        /* copy from previous line or scroll down */
                        case Ctrl_E:                        /* copy from next     line or scroll up */
                            c = ins_ctrl_ey(c);
                            break normalchar;

                        default:
                            if (c == intr_char)             /* special interrupt char */
                            {
                                /* When 'insertmode' set, and not halfway a mapping, don't leave Insert mode. */
                                if (goto_im())
                                {
                                    if (got_int)
                                    {
                                        vgetc();                /* flush all buffers */
                                        got_int = false;
                                    }
                                    else
                                        vim_beep();
                                    break normalchar;
                                }
                                break doESCkey;
                            }
                            break;
                    }

                    /*
                     * Insert a normal character.
                     */
                    if (!p_paste[0])
                    {
                        /* Trigger InsertCharPre. */
                        Bytes s = do_insert_char_pre(c);

                        if (s != null)
                        {
                            if (s.at(0) != NUL && stop_arrow())
                            {
                                /* Insert the new value of v:char literally. */
                                for (Bytes p = s; p.at(0) != NUL; p = p.plus(us_ptr2len_cc(p)))
                                {
                                    c = us_ptr2char(p);
                                    if (c == CAR || c == K_KENTER || c == NL)
                                        ins_eol(c);
                                    else
                                        ins_char(c);
                                }
                                appendToRedobuffLit(s, -1);
                            }
                            c = NUL;
                        }

                        /* If the new value is already inserted or an empty string,
                         * then don't insert any character. */
                        if (c == NUL)
                            break normalchar;
                    }
                    /* Try to perform smart-indenting. */
                    ins_try_si(c);

                    if (c == ' ')
                    {
                        inserted_space[0] = true;
                        if (inindent(0))
                            can_cindent = false;
                        if (insStart_blank_vcol == MAXCOL && curwin.w_cursor.lnum == insStart.lnum)
                            insStart_blank_vcol = get_nolist_virtcol();
                    }

                    /* Insert a normal character and check for abbreviations on a special character.
                     * Let CTRL-] expand abbreviations without inserting it.
                     * Add ABBR_OFF for characters above 0x100, this is what check_abbr() expects. */
                    if (vim_iswordc(c, curbuf) || (!echeck_abbr((0x100 <= c) ? (c + ABBR_OFF) : c) && c != Ctrl_RSB))
                    {
                        insert_special(c, false, false);
                        revins_legal++;
                        revins_chars++;
                    }

                    auto_format(false, true);
                }

                /* If typed something may trigger CursorHoldI again. */
                if (c != K_CURSORHOLD)
                    did_cursorhold = false;

                /* If the cursor was moved we didn't just insert a space. */
                if (arrow_used)
                    inserted_space[0] = false;

                if (can_cindent && cindent_on())
                {
                    /* Indent now if a key was typed that is in 'cinkeys'. */
                    if (in_cinkeys(c, ' ', line_is_white) && stop_arrow())
                        do_c_expr_indent();
                }

                continue;
            }

            /*
             * This is the ONLY return from edit()!
             */

            /* Always update o_lnum, so that a "CTRL-O ." that adds a line
             * still puts the cursor back after the inserted text. */
            if (ins_at_eol && gchar_cursor() == NUL)
                o_lnum = curwin.w_cursor.lnum;

            if (ins_esc(count, cmdchar, nomove))
            {
                if (cmdchar != 'r' && cmdchar != 'v')
                    apply_autocmds(EVENT_INSERTLEAVE, null, null, false, curbuf);
                did_cursorhold = false;
                return (c == Ctrl_O);
            }
        }

        /* NOTREACHED */
    }

    /*
     * Redraw for Insert mode.
     * This is postponed until getting the next character to make '$' in the 'cpo' option work correctly.
     * Only redraw when there are no characters available.
     * This speeds up inserting sequences of characters (e.g., for CTRL-R).
     */
    /*private*/ static void ins_redraw(boolean ready)
        /* ready: not busy with something */
    {
        if (char_avail())
            return;

        long conceal_old_cursor_line = 0;
        long conceal_new_cursor_line = 0;
        boolean conceal_update_lines = false;

        /* Trigger CursorMoved if the cursor moved.
         * Not when the popup menu is visible, the command might delete it. */
        if (ready && (has_cursormovedI() || 0 < curwin.w_onebuf_opt.wo_cole[0])
                  && !eqpos(last_cursormoved, curwin.w_cursor))
        {
            /* Need to update the screen first, to make sure syntax highlighting is correct
             * after making a change (e.g., inserting a "(".  The autocommand may also require
             * a redraw, so it's done again below, unfortunately. */
            if (syntax_present(curwin) && must_redraw != 0)
                update_screen(0);
            if (has_cursormovedI())
                apply_autocmds(EVENT_CURSORMOVEDI, null, null, false, curbuf);
            if (0 < curwin.w_onebuf_opt.wo_cole[0])
            {
                conceal_old_cursor_line = last_cursormoved.lnum;
                conceal_new_cursor_line = curwin.w_cursor.lnum;
                conceal_update_lines = true;
            }
            COPY_pos(last_cursormoved, curwin.w_cursor);
        }

        /* Trigger TextChangedI if b_changedtick differs. */
        if (ready && has_textchangedI() && last_changedtick != curbuf.b_changedtick)
        {
            if (last_changedtick_buf == curbuf)
                apply_autocmds(EVENT_TEXTCHANGEDI, null, null, false, curbuf);
            last_changedtick_buf = curbuf;
            last_changedtick = curbuf.b_changedtick;
        }

        if (must_redraw != 0)
            update_screen(0);
        else if (clear_cmdline || redraw_cmdline)
            showmode();             /* clear cmdline and show mode */
        if ((conceal_update_lines
                && (conceal_old_cursor_line != conceal_new_cursor_line || conceal_cursor_line(curwin)))
                || need_cursor_line_redraw)
        {
            if (conceal_old_cursor_line != conceal_new_cursor_line)
                update_single_line(curwin, conceal_old_cursor_line);
            update_single_line(curwin, conceal_new_cursor_line == 0 ? curwin.w_cursor.lnum : conceal_new_cursor_line);
            curwin.w_valid &= ~VALID_CROW;
        }
        showruler(false);
        setcursor();
        emsg_on_display = false;    /* may remove error message now */
    }

    /*
     * Handle a CTRL-V or CTRL-Q typed in Insert mode.
     */
    /*private*/ static void ins_ctrl_v()
    {
        boolean did_putchar = false;

        /* may need to redraw when no more chars available now */
        ins_redraw(false);

        if (redrawing() && !char_avail())
        {
            edit_putchar('^', true);
            did_putchar = true;
        }
        appendToRedobuff(CTRL_V_STR);   /* CTRL-V */

        add_to_showcmd_c(Ctrl_V);

        int c = get_literal();
        if (did_putchar)
            /* When the line fits in 'columns' the '^' is at the start
             * of the next line and will not removed by the redraw. */
            edit_unputchar();
        clear_showcmd();
        insert_special(c, false, true);
        revins_chars++;
        revins_legal++;
    }

    /*
     * Put a character directly onto the screen.  It's not stored in a buffer.
     * Used while handling CTRL-K, CTRL-V, etc. in Insert mode.
     */
    /*private*/ static int  pc_status;
    /*private*/ static final int PC_STATUS_UNSET = 0;                   /* "pc_bytes" was not set */
    /*private*/ static final int PC_STATUS_RIGHT = 1;                   /* right halve of double-wide char */
    /*private*/ static final int PC_STATUS_LEFT  = 2;                   /* left halve of double-wide char */
    /*private*/ static final int PC_STATUS_SET   = 3;                   /* "pc_bytes" was filled */

    /*private*/ static Bytes pc_bytes = new Bytes(MB_MAXBYTES + 1); /* saved bytes */
    /*private*/ static int[]  pc_attr = new int[1];
    /*private*/ static int  pc_row;
    /*private*/ static int  pc_col;

    /*private*/ static void edit_putchar(int c, boolean highlight)
    {
        if (screenLines != null)
        {
            update_topline();       /* just in case w_topline isn't valid */
            validate_cursor();

            int attr = highlight ? hl_attr(HLF_8) : 0;

            pc_row = curwin.w_winrow + curwin.w_wrow;
            pc_col = curwin.w_wincol;
            pc_status = PC_STATUS_UNSET;
            if (curwin.w_onebuf_opt.wo_rl[0])
            {
                pc_col += curwin.w_width - 1 - curwin.w_wcol;

                int fix_col = mb_fix_col(pc_col, pc_row);
                if (fix_col != pc_col)
                {
                    screen_putchar(' ', pc_row, fix_col, attr);
                    --curwin.w_wcol;
                    pc_status = PC_STATUS_RIGHT;
                }
            }
            else
            {
                pc_col += curwin.w_wcol;
                if (mb_lefthalve(pc_row, pc_col))
                    pc_status = PC_STATUS_LEFT;
            }

            /* save the character to be able to put it back */
            if (pc_status == PC_STATUS_UNSET)
            {
                screen_getbytes(pc_row, pc_col, pc_bytes, pc_attr);
                pc_status = PC_STATUS_SET;
            }
            screen_putchar(c, pc_row, pc_col, attr);
        }
    }

    /*
     * Undo the previous edit_putchar().
     */
    /*private*/ static void edit_unputchar()
    {
        if (pc_status != PC_STATUS_UNSET && msg_scrolled <= pc_row)
        {
            if (pc_status == PC_STATUS_RIGHT)
                curwin.w_wcol++;
            if (pc_status == PC_STATUS_RIGHT || pc_status == PC_STATUS_LEFT)
                redrawWinline(curwin.w_cursor.lnum);
            else
                screen_puts(pc_bytes, pc_row - msg_scrolled, pc_col, pc_attr[0]);
        }
    }

    /*
     * Called when p_dollar is set: display a '$' at the end of the changed text
     * Only works when cursor is in the line that changes.
     */
    /*private*/ static void display_dollar(int col)
    {
        if (!redrawing())
            return;

        cursor_off();
        int save_col = curwin.w_cursor.col;
        curwin.w_cursor.col = col;

        /* If on the last byte of a multi-byte move to the first byte. */
        Bytes p = ml_get_curline();
        curwin.w_cursor.col -= us_head_off(p, p.plus(col));

        curs_columns(false);                    /* recompute w_wrow and w_wcol */
        if (curwin.w_wcol < curwin.w_width)
        {
            edit_putchar('$', false);
            dollar_vcol = curwin.w_virtcol;
        }
        curwin.w_cursor.col = save_col;
    }

    /*
     * Call this function before moving the cursor from the normal insert position in insert mode.
     */
    /*private*/ static void undisplay_dollar()
    {
        if (0 <= dollar_vcol)
        {
            dollar_vcol = -1;
            redrawWinline(curwin.w_cursor.lnum);
        }
    }

    /*
     * Insert an indent (for <Tab> or CTRL-T) or delete an indent (for CTRL-D).
     * Keep the cursor on the same character.
     * type == INDENT_INC   increase indent (for CTRL-T or <Tab>)
     * type == INDENT_DEC   decrease indent (for CTRL-D)
     * type == INDENT_SET   set indent to "amount"
     * If round is true, round the indent to 'shiftwidth' (only with _INC and _DEC).
     */
    /*private*/ static void change_indent(int type, int amount, boolean round, int replaced, boolean call_changed_bytes)
        /* replaced: replaced character, put on replace stack */
        /* call_changed_bytes: call changed_bytes() */
    {
        Bytes orig_line = null;
        int orig_col = 0;

        /* VREPLACE mode needs to know what the line was like before changing. */
        if ((State & VREPLACE_FLAG) != 0)
        {
            orig_line = STRDUP(ml_get_curline());   /* Deal with null below */
            orig_col = curwin.w_cursor.col;
        }

        /* for the following tricks we don't want list mode */
        boolean save_p_list = curwin.w_onebuf_opt.wo_list[0];
        curwin.w_onebuf_opt.wo_list[0] = false;
        int vc = getvcol_nolist(curwin.w_cursor);
        int vcol = vc;

        /*
         * For Replace mode we need to fix the replace stack later, which is only
         * possible when the cursor is in the indent.  Remember the number of
         * characters before the cursor if it's possible.
         */
        int start_col = curwin.w_cursor.col;

        /* determine offset from first non-blank */
        int new_cursor_col = curwin.w_cursor.col;
        beginline(BL_WHITE);
        new_cursor_col -= curwin.w_cursor.col;

        int insstart_less = curwin.w_cursor.col;    /* reduction for insStart.col */

        /*
         * If the cursor is in the indent, compute how many screen columns the
         * cursor is to the left of the first non-blank.
         */
        if (new_cursor_col < 0)
            vcol = get_indent() - vcol;

        if (0 < new_cursor_col)         /* can't fix replace stack */
            start_col = -1;

        /*
         * Set the new indent.  The cursor will be put on the first non-blank.
         */
        if (type == INDENT_SET)
            set_indent(amount, call_changed_bytes ? SIN_CHANGED : 0);
        else
        {
            int save_State = State;

            /* Avoid being called recursively. */
            if ((State & VREPLACE_FLAG) != 0)
                State = INSERT;
            shift_line(type == INDENT_DEC, round, 1, call_changed_bytes);

            State = save_State;
        }
        insstart_less -= curwin.w_cursor.col;

        /*
         * Try to put cursor on same character.
         * If the cursor is at or after the first non-blank in the line,
         * compute the cursor column relative to the column of the first non-blank character.
         * If we are not in insert mode, leave the cursor on the first non-blank.
         * If the cursor is before the first non-blank, position it relative
         * to the first non-blank, counted in screen columns.
         */
        if (0 <= new_cursor_col)
        {
            /*
             * When changing the indent while the cursor is touching it, reset insStart_col to 0.
             */
            if (new_cursor_col == 0)
                insstart_less = MAXCOL;
            new_cursor_col += curwin.w_cursor.col;
        }
        else if ((State & INSERT) == 0)
            new_cursor_col = curwin.w_cursor.col;
        else
        {
            /*
             * Compute the screen column where the cursor should be.
             */
            vcol = get_indent() - vcol;
            curwin.w_virtcol = (vcol < 0) ? 0 : vcol;

            /*
             * Advance the cursor until we reach the right screen column.
             */
            int last_vcol = vcol = 0;
            new_cursor_col = -1;
            Bytes ptr = ml_get_curline();
            while (vcol <= curwin.w_virtcol)
            {
                last_vcol = vcol;
                if (0 <= new_cursor_col)
                    new_cursor_col += us_ptr2len_cc(ptr.plus(new_cursor_col));
                else
                    new_cursor_col++;
                vcol += lbr_chartabsize(ptr, ptr.plus(new_cursor_col), vcol);
            }
            vcol = last_vcol;

            /*
             * May need to insert spaces to be able to position the cursor on
             * the right screen column.
             */
            if (vcol != curwin.w_virtcol)
            {
                curwin.w_cursor.col = new_cursor_col;
                int i = curwin.w_virtcol - vcol;
                ptr = new Bytes(i + 1);

                new_cursor_col += i;
                ptr.be(i, NUL);
                while (0 <= --i)
                    ptr.be(i, (byte)' ');
                ins_str(ptr);
            }

            /*
             * When changing the indent while the cursor is in it, reset insStart_col to 0.
             */
            insstart_less = MAXCOL;
        }

        curwin.w_onebuf_opt.wo_list[0] = save_p_list;

        if (new_cursor_col <= 0)
            curwin.w_cursor.col = 0;
        else
            curwin.w_cursor.col = new_cursor_col;
        curwin.w_set_curswant = true;
        changed_cline_bef_curs();

        /*
         * May have to adjust the start of the insert.
         */
        if ((State & INSERT) != 0)
        {
            if (curwin.w_cursor.lnum == insStart.lnum && insStart.col != 0)
            {
                if (insStart.col <= insstart_less)
                    insStart.col = 0;
                else
                    insStart.col -= insstart_less;
            }
            if (ai_col <= insstart_less)
                ai_col = 0;
            else
                ai_col -= insstart_less;
        }

        /*
         * For REPLACE mode, may have to fix the replace stack, if it's possible.
         * If the number of characters before the cursor decreased, need to pop a
         * few characters from the replace stack.
         * If the number of characters before the cursor increased, need to push a
         * few NULs onto the replace stack.
         */
        if ((State & REPLACE_FLAG) != 0 && (State & VREPLACE_FLAG) == 0 && 0 <= start_col)
        {
            while (curwin.w_cursor.col < start_col)
            {
                replace_join(0);        /* remove a NUL from the replace stack */
                --start_col;
            }
            while (start_col < curwin.w_cursor.col || replaced != NUL)
            {
                replace_push(NUL);
                if (replaced != NUL)
                {
                    replace_push(replaced);
                    replaced = NUL;
                }
                start_col++;
            }
        }

        /*
         * For VREPLACE mode, we also have to fix the replace stack.  In this case
         * it is always possible because we backspace over the whole line and then
         * put it back again the way we wanted it.
         */
        if ((State & VREPLACE_FLAG) != 0)
        {
            /* If 'orig_line' didn't allocate, just return.
             * At least we did the job, even if you can't backspace. */
            if (orig_line == null)
                return;

            /* Save new line. */
            Bytes new_line = STRDUP(ml_get_curline());

            /* We only put back the new line up to the cursor. */
            new_line.be(curwin.w_cursor.col, NUL);

            /* Put back original line. */
            ml_replace(curwin.w_cursor.lnum, orig_line, false);
            curwin.w_cursor.col = orig_col;

            /* Backspace from cursor to start of line. */
            backspace_until_column(0);

            /* Insert new stuff into line again. */
            ins_bytes(new_line);
        }
    }

    /*
     * Truncate the space at the end of a line.  This is to be used only in an
     * insert mode.  It handles fixing the replace stack for REPLACE and VREPLACE modes.
     */
    /*private*/ static void truncate_spaces(Bytes line)
    {
        /* find start of trailing white space */
        int i;
        for (i = strlen(line) - 1; 0 <= i && vim_iswhite(line.at(i)); i--)
        {
            if ((State & REPLACE_FLAG) != 0)
                replace_join(0);        /* remove a NUL from the replace stack */
        }
        line.be(i + 1, NUL);
    }

    /*
     * Backspace the cursor until the given column.  Handles REPLACE and VREPLACE
     * modes correctly.  May also be used when not in insert mode at all.
     * Will attempt not to go before "col" even when there is a composing character.
     */
    /*private*/ static void backspace_until_column(int col)
    {
        while (col < curwin.w_cursor.col)
        {
            --curwin.w_cursor.col;
            if ((State & REPLACE_FLAG) != 0)
                replace_do_bs(col);
            else if (!del_char_after_col(col))
                break;
        }
    }

    /*
     * Like del_char(), but make sure not to go before column "limit_col".
     * Only matters when there are composing characters.
     * Return true when something was deleted.
     */
    /*private*/ static boolean del_char_after_col(int limit_col)
    {
        if (0 <= limit_col)
        {
            int ecol = curwin.w_cursor.col + 1;

            /* Make sure the cursor is at the start of a character, but
             * skip forward again when going too far back because of a
             * composing character. */
            mb_adjust_pos(curbuf, curwin.w_cursor);
            while (curwin.w_cursor.col < limit_col)
            {
                int l = us_ptr2len(ml_get_cursor());

                if (l == 0)     /* end of line */
                    break;
                curwin.w_cursor.col += l;
            }
            if (ml_get_cursor().at(0) == NUL || curwin.w_cursor.col == ecol)
                return false;
            del_bytes(ecol - curwin.w_cursor.col, false, true);
        }
        else
            del_char(false);
        return true;
    }

    /*
     * Next character is interpreted literally.
     * A one, two or three digit decimal number is interpreted as its byte value.
     * If one or two digits are entered, the next character is given to vungetc().
     * For Unicode a character > 255 may be returned.
     */
    /*private*/ static int get_literal()
    {
        if (got_int)
            return Ctrl_C;

        boolean hex = false;
        boolean octal = false;
        int unicode = 0;

        int nc;
        no_mapping++;               /* don't map the next key hits */

        int cc = 0;
        int i = 0;
        for ( ; ; )
        {
            nc = plain_vgetc();

            if ((State & CMDLINE) == 0 && mb_byte2len(nc) == 1)
                add_to_showcmd(nc);

            if (nc == 'x' || nc == 'X')
                hex = true;
            else if (nc == 'o' || nc == 'O')
                octal = true;
            else if (nc == 'u' || nc == 'U')
                unicode = nc;
            else
            {
                if (hex || unicode != 0)
                {
                    if (!asc_isxdigit(nc))
                        break;
                    cc = cc * 16 + hex2nr(nc);
                }
                else if (octal)
                {
                    if (nc < '0' || '7' < nc)
                        break;
                    cc = cc * 8 + nc - '0';
                }
                else
                {
                    if (!asc_isdigit(nc))
                        break;
                    cc = cc * 10 + nc - '0';
                }

                i++;
            }

            if (cc > 255 && unicode == 0)
                cc = 255;           /* limit range to 0-255 */
            nc = 0;

            if (hex)                /* hex: up to two chars */
            {
                if (2 <= i)
                    break;
            }
            else if (unicode != 0)       /* Unicode: up to four or eight chars */
            {
                if ((unicode == 'u' && 4 <= i) || (unicode == 'U' && 8 <= i))
                    break;
            }
            else if (3 <= i)        /* decimal or octal: up to three chars */
                break;
        }
        if (i == 0)                 /* no number entered */
        {
            if (nc == K_ZERO)       /* NUL is stored as NL */
            {
                cc = '\n';
                nc = 0;
            }
            else
            {
                cc = nc;
                nc = 0;
            }
        }

        if (cc == 0)                /* NUL is stored as NL */
            cc = '\n';

        --no_mapping;
        if (nc != 0)
            vungetc(nc);

        got_int = false;            /* CTRL-C typed after CTRL-V is not an interrupt */
        return cc;
    }

    /*
     * Insert character, taking care of special keys and mod_mask
     */
    /*private*/ static void insert_special(int c, boolean allow_modmask, boolean ctrlv)
        /* ctrlv: c was typed after CTRL-V */
    {
        /*
         * Special function key, translate into "<Key>".  Up to the last '>' is
         * inserted with ins_str(), so as not to replace characters in replace mode.
         * Only use mod_mask for special keys, to avoid things like <S-Space>,
         * unless 'allow_modmask' is true.
         */
        if (is_special(c) || (mod_mask != 0 && allow_modmask))
        {
            Bytes p = get_special_key_name(c, mod_mask);
            int len = strlen(p);
            c = p.at(len - 1);
            if (2 < len)
            {
                if (!stop_arrow())
                    return;
                p.be(len - 1, NUL);
                ins_str(p);
                appendToRedobuffLit(p, -1);
                ctrlv = false;
            }
        }
        if (stop_arrow())
            insertchar(c, ctrlv ? INSCHAR_CTRLV : 0, -1);
    }

    /*
     * Special characters in this context are those that need processing other
     * than the simple insertion that can be performed here.  This includes ESC
     * which terminates the insert, and CR/NL which need special processing to
     * open up a new line.  This routine tries to optimize insertions performed by
     * the "redo", "undo" or "put" commands, so it needs to know when it should
     * stop and defer processing to the "normal" mechanism.
     * '0' and '^' are special, because they can be followed by CTRL-D.
     */
    /*private*/ static boolean isspecial(int c)
    {
        return (c < ' ' || DEL <= c || c == '0' || c == '^');
    }

    /*private*/ static boolean whitechar(int cc)
    {
        return (vim_iswhite(cc) && !utf_iscomposing(us_ptr2char(ml_get_cursor().plus(1))));
    }

    /*
     * "flags": INSCHAR_FORMAT - force formatting
     *          INSCHAR_CTRLV  - char typed just after CTRL-V
     *          INSCHAR_NO_FEX - don't use 'formatexpr'
     *
     *   NOTE: passes the flags value straight through to internal_format() which,
     *         beside INSCHAR_FORMAT (above), is also looking for these:
     *          INSCHAR_DO_COM   - format comments
     *          INSCHAR_COM_LIST - format comments with num list or 2nd line indent
     */
    /*private*/ static void insertchar(int c, int flags, int second_indent)
        /* c: character to insert or NUL */
        /* flags: INSCHAR_FORMAT, etc. */
        /* second_indent: indent for second line if >= 0 */
    {
        boolean force_format = ((flags & INSCHAR_FORMAT) != 0);

        int textwidth = comp_textwidth(force_format);
        boolean fo_ins_blank = has_format_option(FO_INS_BLANK);

        /*
         * Try to break the line in two or more pieces when:
         * - Always do this if we have been called to do formatting only.
         * - Always do this when 'formatoptions' has the 'a' flag and the line ends in white space.
         * - Otherwise:
         *   - Don't do this if inserting a blank
         *   - Don't do this if an existing character is being replaced, unless we're in VREPLACE mode.
         *   - Do this if the cursor is not on the line where insert started
         *   or - 'formatoptions' doesn't have 'l' or the line was not too long before the insert.
         *      - 'formatoptions' doesn't have 'b' or a blank was inserted at or before 'textwidth'
         */
        if (0 < textwidth
                && (force_format
                    || (!vim_iswhite(c)
                        && !((State & REPLACE_FLAG) != 0
                            && (State & VREPLACE_FLAG) == 0
                            && ml_get_cursor().at(0) != NUL)
                        && (curwin.w_cursor.lnum != insStart.lnum
                            || ((!has_format_option(FO_INS_LONG) || insStart_textlen <= textwidth)
                                && (!fo_ins_blank || insStart_blank_vcol <= textwidth))))))
        {
            /* Format with 'formatexpr' when it's set.
             * Use internal formatting when 'formatexpr' isn't set or it returns non-zero. */
            boolean do_internal = true;
            int virtcol = get_nolist_virtcol() + mb_char2cells(c != NUL ? c : gchar_cursor());

            if (curbuf.b_p_fex[0].at(0) != NUL && (flags & INSCHAR_NO_FEX) == 0 && (force_format || textwidth < virtcol))
            {
                do_internal = (fex_format(curwin.w_cursor.lnum, 1L, c) != 0);
                /* It may be required to save for undo again, e.g. when setline() was called. */
                ins_need_undo = true;
            }
            if (do_internal)
                internal_format(textwidth, second_indent, flags, c == NUL, c);
        }

        if (c == NUL)           /* only formatting was wanted */
            return;

        /* Check whether this character should end a comment. */
        if (did_ai && c == end_comment_pending)
        {
            Bytes line;
            Bytes[] p = new Bytes[1];

            /*
             * Need to remove existing (middle) comment leader and insert end
             * comment leader.  First, check what comment leader we can find.
             */
            int i = get_leader_len(line = ml_get_curline(), p, false, true);
            if (0 < i && vim_strchr(p[0], COM_MIDDLE) != null)
            {
                Bytes lead_end = new Bytes(COM_MAX_LEN);    /* end-comment string */

                /* Skip middle-comment string. */
                while (p[0].at(0) != NUL && p[0].at(-1) != (byte)':')           /* find end of middle flags */
                    p[0] = p[0].plus(1);
                int middle_len = copy_option_part(p, lead_end, COM_MAX_LEN, u8(","));
                /* Don't count trailing white space for middle_len. */
                while (0 < middle_len && vim_iswhite(lead_end.at(middle_len - 1)))
                    --middle_len;

                /* Find the end-comment string. */
                while (p[0].at(0) != NUL && p[0].at(-1) != (byte)':')           /* find end of end flags */
                    p[0] = p[0].plus(1);
                int end_len = copy_option_part(p, lead_end, COM_MAX_LEN, u8(","));

                /* Skip white space before the cursor. */
                i = curwin.w_cursor.col;
                while (0 <= --i && vim_iswhite(line.at(i)))
                    ;
                i++;

                /* Skip to before the middle leader. */
                i -= middle_len;

                /* Check some expected things before we go on. */
                if (0 <= i && lead_end.at(end_len - 1) == end_comment_pending)
                {
                    /* Backspace over all the stuff we want to replace. */
                    backspace_until_column(i);

                    /*
                     * Insert the end-comment string, except for the last
                     * character, which will get inserted as normal later.
                     */
                    ins_bytes_len(lead_end, end_len - 1);
                }
            }
        }
        end_comment_pending = NUL;

        did_ai = false;
        did_si = false;
        can_si = false;
        can_si_back = false;

        /*
         * If there's any pending input, grab up to INPUT_BUFLEN at once.
         * This speeds up normal text input considerably.
         * Don't do this when 'cindent' or 'indentexpr' is set, because we might
         * need to re-indent at a ':', or any other character (but not what 'paste' is set)..
         * Don't do this when there an InsertCharPre autocommand is defined,
         * because we need to fire the event for every character.
         */
        if (!isspecial(c)
                && utf_char2len(c) == 1
                && vpeekc() != NUL
                && (State & REPLACE_FLAG) == 0
                && !cindent_on()
                && !p_ri[0]
                && !has_insertcharpre())
        {
            final int INPUT_BUFLEN = 100;
            Bytes buf = new Bytes(INPUT_BUFLEN + 1);
            int virtcol = 0;

            buf.be(0, c);
            int i = 1;
            if (0 < textwidth)
                virtcol = get_nolist_virtcol();
            /*
             * Stop the string when:
             * - no more chars available
             * - finding a special character (command key)
             * - buffer is full
             * - running into the 'textwidth' boundary
             * - need to check for abbreviation: A non-word char after a word-char
             */
            while ((c = vpeekc()) != NUL
                    && !isspecial(c)
                    && mb_byte2len(c) == 1
                    && i < INPUT_BUFLEN
                    && (textwidth == 0 || (virtcol += mb_byte2cells(buf.at(i - 1))) < textwidth)
                    && !(!no_abbr && !vim_iswordc(c, curbuf) && vim_iswordc(buf.at(i - 1), curbuf)))
            {
                c = vgetc();
                buf.be(i++, c);
            }

            do_digraph(-1);                 /* clear digraphs */
            do_digraph(buf.at(i - 1));           /* may be the start of a digraph */
            buf.be(i, NUL);
            ins_str(buf);
            if ((flags & INSCHAR_CTRLV) != 0)
            {
                redo_literal(buf.at(0));
                i = 1;
            }
            else
                i = 0;
            if (buf.at(i) != NUL)
                appendToRedobuffLit(buf.plus(i), -1);
        }
        else
        {
            int cc = utf_char2len(c);
            if (1 < cc)
            {
                Bytes buf = new Bytes(MB_MAXBYTES + 1);

                utf_char2bytes(c, buf);
                buf.be(cc, NUL);
                ins_char_bytes(buf, cc);
                appendCharToRedobuff(c);
            }
            else
            {
                ins_char(c);
                if ((flags & INSCHAR_CTRLV) != 0)
                    redo_literal(c);
                else
                    appendCharToRedobuff(c);
            }
        }
    }

    /*
     * Format text at the current insert position.
     *
     * If the INSCHAR_COM_LIST flag is present, then the value of second_indent
     * will be the comment leader length sent to open_line().
     */
    /*private*/ static void internal_format(int textwidth, int second_indent, int flags, boolean format_only, int c)
        /* c: character to be inserted (can be NUL) */
    {
        boolean haveto_redraw = false;
        boolean fo_ins_blank = has_format_option(FO_INS_BLANK);
        boolean fo_multibyte = has_format_option(FO_MBYTE_BREAK);
        boolean fo_white_par = has_format_option(FO_WHITE_PAR);
        boolean first_line = true;
        boolean no_leader = false;
        boolean do_comments = ((flags & INSCHAR_DO_COM) != 0);
        boolean has_lbr = curwin.w_onebuf_opt.wo_lbr[0];

        /* make sure win_lbr_chartabsize() counts correctly */
        curwin.w_onebuf_opt.wo_lbr[0] = false;

        /*
         * When 'ai' is off we don't want a space under the cursor to be deleted.
         * Replace it with an 'x' temporarily.
         */
        int save_char = NUL;
        if (!curbuf.b_p_ai[0] && (State & VREPLACE_FLAG) == 0)
        {
            int cc = gchar_cursor();
            if (vim_iswhite(cc))
            {
                save_char = cc;
                pchar_cursor('x');
            }
        }

        /*
         * Repeat breaking lines, until the current line is not too long.
         */
        while (!got_int)
        {
            int end_foundcol = 0;       /* column for start of word */
            int orig_col = 0;
            Bytes saved_text = null;

            int virtcol = get_nolist_virtcol() + mb_char2cells(c != NUL ? c : gchar_cursor());
            if (virtcol <= textwidth)
                break;

            if (no_leader)
                do_comments = false;
            else if ((flags & INSCHAR_FORMAT) == 0 && has_format_option(FO_WRAP_COMS))
                do_comments = true;

            /* Don't break until after the comment leader. */
            int leader_len = 0;
            if (do_comments)
                leader_len = get_leader_len(ml_get_curline(), null, false, true);

            /* If the line doesn't start with a comment leader, then don't start one
             * in a following broken line.  Avoids that a %word moved to the start
             * of the next line causes all following lines to start with %. */
            if (leader_len == 0)
                no_leader = true;
            if ((flags & INSCHAR_FORMAT) == 0 && leader_len == 0 && !has_format_option(FO_WRAP))
                break;

            int startcol = curwin.w_cursor.col;     /* cursor column at entry */
            if (startcol == 0)
                break;

            /* find column of textwidth border */
            coladvance(textwidth);
            int wantcol = curwin.w_cursor.col;      /* column at textwidth border */

            curwin.w_cursor.col = startcol;
            int foundcol = 0;                       /* column for start of spaces */

            /*
             * Find position to break at.
             * Stop at first entered white when 'formatoptions' has 'v'.
             */
            while ((!fo_ins_blank && !has_format_option(FO_INS_VI))
                        || (flags & INSCHAR_FORMAT) != 0
                        || curwin.w_cursor.lnum != insStart.lnum
                        || insStart.col <= curwin.w_cursor.col)
            {
                int cc;
                if (curwin.w_cursor.col == startcol && c != NUL)
                    cc = c;
                else
                    cc = gchar_cursor();

                if (whitechar(cc))
                {
                    /* remember position of blank just before text */
                    int end_col = curwin.w_cursor.col;

                    /* find start of sequence of blanks */
                    while (0 < curwin.w_cursor.col && whitechar(cc))
                    {
                        dec_cursor();
                        cc = gchar_cursor();
                    }
                    if (curwin.w_cursor.col == 0 && whitechar(cc))
                        break;                      /* only spaces in front of text */

                    /* Don't break until after the comment leader. */
                    if (curwin.w_cursor.col < leader_len)
                        break;

                    if (has_format_option(FO_ONE_LETTER))
                    {
                        /* do not break after one-letter words */
                        if (curwin.w_cursor.col == 0)
                            break;                  /* one-letter word at begin */

                        /* do not break "#a b" when 'tw' is 2 */
                        if (curwin.w_cursor.col <= leader_len)
                            break;

                        int col = curwin.w_cursor.col;
                        dec_cursor();
                        if (whitechar(gchar_cursor()))
                            continue;               /* one-letter, continue */

                        curwin.w_cursor.col = col;
                    }

                    inc_cursor();

                    end_foundcol = end_col + 1;
                    foundcol = curwin.w_cursor.col;

                    if (curwin.w_cursor.col <= wantcol)
                        break;
                }
                else if (0x100 <= cc && fo_multibyte)
                {
                    /* Break after or before a multi-byte character. */
                    if (curwin.w_cursor.col != startcol)
                    {
                        /* Don't break until after the comment leader. */
                        if (curwin.w_cursor.col < leader_len)
                            break;

                        int col = curwin.w_cursor.col;
                        inc_cursor();
                        /* Don't change end_foundcol if already set. */
                        if (foundcol != curwin.w_cursor.col)
                        {
                            foundcol = curwin.w_cursor.col;
                            end_foundcol = foundcol;
                            if (curwin.w_cursor.col <= wantcol)
                                break;
                        }
                        curwin.w_cursor.col = col;
                    }

                    if (curwin.w_cursor.col == 0)
                        break;

                    int col = curwin.w_cursor.col;

                    dec_cursor();
                    if (whitechar(gchar_cursor()))
                        continue;           /* break with space */

                    /* Don't break until after the comment leader. */
                    if (curwin.w_cursor.col < leader_len)
                        break;

                    curwin.w_cursor.col = col;

                    foundcol = curwin.w_cursor.col;
                    end_foundcol = foundcol;
                    if (curwin.w_cursor.col <= wantcol)
                        break;
                }

                if (curwin.w_cursor.col == 0)
                    break;

                dec_cursor();
            }

            if (foundcol == 0)              /* no spaces, cannot break line */
            {
                curwin.w_cursor.col = startcol;
                break;
            }

            /* Going to break the line, remove any "$" now. */
            undisplay_dollar();

            /*
             * Offset between cursor position and line break is used by replace stack functions.
             * VREPLACE does not use this, and backspaces over the text instead.
             */
            if ((State & VREPLACE_FLAG) != 0)
                orig_col = startcol;        /* will start backspacing from here */
            else
                replace_offset = startcol - end_foundcol;

            /*
             * Adjust startcol for spaces that will be deleted and
             * characters that will remain on top line.
             */
            curwin.w_cursor.col = foundcol;
            while (whitechar(gchar_cursor()) && (!fo_white_par || curwin.w_cursor.col < startcol))
                inc_cursor();
            startcol -= curwin.w_cursor.col;
            if (startcol < 0)
                startcol = 0;

            if ((State & VREPLACE_FLAG) != 0)
            {
                /*
                 * In VREPLACE mode, we will backspace over the text to be
                 * wrapped, so save a copy now to put on the next line.
                 */
                saved_text = STRDUP(ml_get_cursor());
                curwin.w_cursor.col = orig_col;
                saved_text.be(startcol, NUL);

                /* Backspace over characters that will move to the next line. */
                if (!fo_white_par)
                    backspace_until_column(foundcol);
            }
            else
            {
                /* Put cursor after pos to break line. */
                if (!fo_white_par)
                    curwin.w_cursor.col = foundcol;
            }

            /*
             * Split the line just before the margin.
             * Only insert/delete lines, but don't really redraw the window.
             */
            open_line(FORWARD, OPENLINE_DELSPACES + OPENLINE_MARKFIX
                    + (fo_white_par ? OPENLINE_KEEPTRAIL : 0)
                    + (do_comments ? OPENLINE_DO_COM : 0)
                    + ((flags & INSCHAR_COM_LIST) != 0 ? OPENLINE_COM_LIST : 0)
                    , ((flags & INSCHAR_COM_LIST) != 0 ? second_indent : old_indent));
            if ((flags & INSCHAR_COM_LIST) == 0)
                old_indent = 0;

            replace_offset = 0;
            if (first_line)
            {
                if ((flags & INSCHAR_COM_LIST) == 0)
                {
                    /*
                     * This section is for auto-wrap of numeric lists.  When not
                     * in insert mode (i.e. format_lines()), the INSCHAR_COM_LIST
                     * flag will be set and open_line() will handle it (as seen
                     * above).  The code here (and in get_number_indent()) will
                     * recognize comments if needed...
                     */
                    if (second_indent < 0 && has_format_option(FO_Q_NUMBER))
                        second_indent = get_number_indent(curwin.w_cursor.lnum - 1);
                    if (0 <= second_indent)
                    {
                        if ((State & VREPLACE_FLAG) != 0)
                            change_indent(INDENT_SET, second_indent, false, NUL, true);
                        else if (0 < leader_len && 0 < second_indent - leader_len)
                        {
                            int padding = second_indent - leader_len;

                            /* We started at the first_line of a numbered list
                             * that has a comment.  the open_line() function has
                             * inserted the proper comment leader and positioned
                             * the cursor at the end of the split line.  Now we
                             * add the additional whitespace needed after the
                             * comment leader for the numbered list. */
                            for (int i = 0; i < padding; i++)
                                ins_str(u8(" "));
                            changed_bytes(curwin.w_cursor.lnum, leader_len);
                        }
                        else
                            set_indent(second_indent, SIN_CHANGED);
                    }
                }
                first_line = false;
            }

            if ((State & VREPLACE_FLAG) != 0)
            {
                /*
                 * In VREPLACE mode we have backspaced over the text to be
                 * moved, now we re-insert it into the new line.
                 */
                ins_bytes(saved_text);
            }
            else
            {
                /*
                 * Check if cursor is not past the NUL off the line, cindent
                 * may have added or removed indent.
                 */
                curwin.w_cursor.col += startcol;
                int len = strlen(ml_get_curline());
                if (curwin.w_cursor.col > len)
                    curwin.w_cursor.col = len;
            }

            haveto_redraw = true;
            can_cindent = true;
            /* moved the cursor, don't autoindent or cindent now */
            did_ai = false;
            did_si = false;
            can_si = false;
            can_si_back = false;
            line_breakcheck();
        }

        if (save_char != NUL)               /* put back space after cursor */
            pchar_cursor(save_char);

        curwin.w_onebuf_opt.wo_lbr[0] = has_lbr;
        if (!format_only && haveto_redraw)
        {
            update_topline();
            redraw_curbuf_later(VALID);
        }
    }

    /*
     * Called after inserting or deleting text: When 'formatoptions' includes
     * the 'a' flag format from the current line until the end of the paragraph.
     * Keep the cursor at the same position relative to the text.
     * The caller must have saved the cursor line for undo, following ones will be saved here.
     */
    /*private*/ static void auto_format(boolean trailblank, boolean prev_line)
        /* trailblank: when true also format with trailing blank */
        /* prev_line: may start in previous line */
    {
        if (!has_format_option(FO_AUTO))
            return;

        pos_C pos = new pos_C();
        COPY_pos(pos, curwin.w_cursor);
        Bytes old = ml_get_curline();

        /* may remove added space */
        check_auto_format(false);

        /* Don't format in Insert mode when the cursor is on a trailing blank, the
         * user might insert normal text next.  Also skip formatting when "1" is
         * in 'formatoptions' and there is a single character before the cursor.
         * Otherwise the line would be broken and when typing another non-white
         * next they are not joined back together. */
        boolean wasatend = (pos.col == strlen(old));
        if (old.at(0) != NUL && !trailblank && wasatend)
        {
            dec_cursor();
            int cc = gchar_cursor();
            if (!whitechar(cc) && 0 < curwin.w_cursor.col && has_format_option(FO_ONE_LETTER))
                dec_cursor();
            cc = gchar_cursor();
            if (whitechar(cc))
            {
                COPY_pos(curwin.w_cursor, pos);
                return;
            }
            COPY_pos(curwin.w_cursor, pos);
        }

        /* With the 'c' flag in 'formatoptions' and 't' missing: only format comments. */
        if (has_format_option(FO_WRAP_COMS) && !has_format_option(FO_WRAP)
                                         && get_leader_len(old, null, false, true) == 0)
        {
            return;
        }

        /*
         * May start formatting in a previous line, so that after "x" a word is
         * moved to the previous line if it fits there now.  Only when this is not
         * the start of a paragraph.
         */
        if (prev_line && !paragraph_start(curwin.w_cursor.lnum))
        {
            --curwin.w_cursor.lnum;
            if (!u_save_cursor())
                return;
        }

        /*
         * Do the formatting and restore the cursor position.
         * "saved_cursor" will be adjusted for the text formatting.
         */
        COPY_pos(saved_cursor, pos);
        format_lines(-1, false);
        COPY_pos(curwin.w_cursor, saved_cursor);
        saved_cursor.lnum = 0;

        if (curwin.w_cursor.lnum > curbuf.b_ml.ml_line_count)
        {
            /* "cannot happen" */
            curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
            coladvance(MAXCOL);
        }
        else
            check_cursor_col();

        /* Insert mode: If the cursor is now after the end of the line while it previously
         * wasn't, the line was broken.  Because of the rule above we need to add a space
         * when 'w' is in 'formatoptions' to keep a paragraph formatted. */
        if (!wasatend && has_format_option(FO_WHITE_PAR))
        {
            Bytes p = ml_get_curline();
            int len = strlen(p);
            if (curwin.w_cursor.col == len)
            {
                p = STRNDUP(p, len + 2);
                p.be(len, (byte)' ');
                p.be(len + 1, NUL);
                ml_replace(curwin.w_cursor.lnum, p, false);
                /* remove the space later */
                did_add_space = true;
            }
            else
                /* may remove added space */
                check_auto_format(false);
        }

        check_cursor();
    }

    /*
     * When an extra space was added to continue a paragraph for auto-formatting,
     * delete it now.  The space must be under the cursor, just after the insert position.
     */
    /*private*/ static void check_auto_format(boolean end_insert)
        /* end_insert: true when ending Insert mode */
    {
        if (did_add_space)
        {
            int cc = gchar_cursor();
            if (!whitechar(cc))
                /* Somehow the space was removed already. */
                did_add_space = false;
            else
            {
                int c = ' ';

                if (!end_insert)
                {
                    inc_cursor();
                    c = gchar_cursor();
                    dec_cursor();
                }
                if (c != NUL)
                {
                    /* The space is no longer at the end of the line, delete it. */
                    del_char(false);
                    did_add_space = false;
                }
            }
        }
    }

    /*
     * Find out textwidth to be used for formatting:
     *      if 'textwidth' option is set, use it
     *      else if 'wrapmargin' option is set, use curwin.w_width - 'wrapmargin'
     *      if invalid value, use 0.
     *      Set default to window width (maximum 79) for "gq" operator.
     */
    /*private*/ static int comp_textwidth(boolean ff)
        /* ff: force formatting (for "gq" command) */
    {
        int textwidth = (int)curbuf.b_p_tw[0];
        if (textwidth == 0 && curbuf.b_p_wm[0] != 0)
        {
            /* The width is the window width minus 'wrapmargin' minus
             * all the things that add to the margin. */
            textwidth = curwin.w_width - (int)curbuf.b_p_wm[0];
            if (cmdwin_type != 0)
                textwidth -= 1;
            if (curwin.w_onebuf_opt.wo_nu[0] || curwin.w_onebuf_opt.wo_rnu[0])
                textwidth -= 8;
        }
        if (textwidth < 0)
            textwidth = 0;
        if (ff && textwidth == 0)
        {
            textwidth = curwin.w_width - 1;
            if (79 < textwidth)
                textwidth = 79;
        }
        return textwidth;
    }

    /*
     * Put a character in the redo buffer, for when just after a CTRL-V.
     */
    /*private*/ static void redo_literal(int c)
    {
        Bytes buf = new Bytes(10);

        /* Only digits need special treatment.  Translate them into a string of three digits. */
        if (asc_isdigit(c))
        {
            vim_snprintf(buf, buf.size(), u8("%03d"), c);
            appendToRedobuff(buf);
        }
        else
            appendCharToRedobuff(c);
    }

    /*
     * start_arrow() is called when an arrow key is used in insert mode.
     * For undo/redo it resembles hitting the <ESC> key.
     */
    /*private*/ static void start_arrow(pos_C end_insert_pos)
        /* end_insert_pos: can be null */
    {
        if (!arrow_used)                /* something has been inserted */
        {
            appendToRedobuff(ESC_STR);
            stop_insert(end_insert_pos, false, false);
            arrow_used = true;          /* this means we stopped the current insert */
        }
    }

    /*
     * stop_arrow() is called before a change is made in insert mode.
     * If an arrow key has been used, start a new insertion.
     * Returns false if undo is impossible, shouldn't insert then.
     */
    /*private*/ static boolean stop_arrow()
    {
        if (arrow_used)
        {
            COPY_pos(insStart, curwin.w_cursor);    /* new insertion starts here */
            if (insStart_orig.col < insStart.col && !ins_need_undo)
                /* Don't update the original insert position when moved to the right,
                 * except when nothing was inserted yet. */
                update_insStart_orig = false;
            insStart_textlen = linetabsize(ml_get_curline());

            if (u_save_cursor())
            {
                arrow_used = false;
                ins_need_undo = false;
            }

            ai_col = 0;
            if ((State & VREPLACE_FLAG) != 0)
            {
                orig_line_count = curbuf.b_ml.ml_line_count;
                vr_lines_changed = 1;
            }
            resetRedobuff();
            appendToRedobuff(u8("1i"));         /* pretend we start an insertion */
            new_insert_skip = 2;
        }
        else if (ins_need_undo)
        {
            if (u_save_cursor())
                ins_need_undo = false;
        }

        return (arrow_used || !ins_need_undo);
    }

    /*
     * Do a few things to stop inserting.
     * "end_insert_pos" is where insert ended.
     * It is null when we already jumped to another window/buffer.
     */
    /*private*/ static void stop_insert(pos_C end_insert_pos, boolean esc, boolean nomove)
        /* esc: called by ins_esc() */
        /* nomove: <c-\><c-o>, don't move cursor */
    {
        stop_redo_ins();
        replace_flush();            /* abandon replace stack */

        /*
         * Save the inserted text for later redo with ^@ and CTRL-A.
         * Don't do it when "restart_edit" was set and nothing was inserted,
         * otherwise CTRL-O w and then <Left> will clear "last_insert".
         */
        Bytes ptr = get_inserted();
        if (did_restart_edit == 0 || (ptr != null && new_insert_skip < strlen(ptr)))
        {
            last_insert = ptr;
            last_insert_skip = new_insert_skip;
        }

        if (!arrow_used && end_insert_pos != null)
        {
            int cc;

            /* Auto-format now.  It may seem strange to do this when stopping an
             * insertion (or moving the cursor), but it's required when appending
             * a line and having it end in a space.  But only do it when something
             * was actually inserted, otherwise undo won't work. */
            if (!ins_need_undo && has_format_option(FO_AUTO))
            {
                pos_C tpos = new pos_C();
                COPY_pos(tpos, curwin.w_cursor);

                /* When the cursor is at the end of the line after a space
                 * the formatting will move it to the following word.
                 * Avoid that by moving the cursor onto the space. */
                cc = 'x';
                if (0 < curwin.w_cursor.col && gchar_cursor() == NUL)
                {
                    dec_cursor();
                    cc = gchar_cursor();
                    if (!vim_iswhite(cc))
                        COPY_pos(curwin.w_cursor, tpos);
                }

                auto_format(true, false);

                if (vim_iswhite(cc))
                {
                    if (gchar_cursor() != NUL)
                        inc_cursor();
                    /* If the cursor is still at the same character, also keep the "coladd". */
                    if (gchar_cursor() == NUL
                            && curwin.w_cursor.lnum == tpos.lnum
                            && curwin.w_cursor.col == tpos.col)
                        curwin.w_cursor.coladd = tpos.coladd;
                }
            }

            /* If a space was inserted for auto-formatting, remove it now. */
            check_auto_format(true);

            /* If we just did an auto-indent, remove the white space from the end of the line,
             * and put the cursor back.  Do this when ESC was used or moving the cursor up/down.
             * Check for the old position still being valid, just in case the text got changed
             * unexpectedly. */
            if (!nomove && did_ai
                    && (esc || (vim_strbyte(p_cpo[0], CPO_INDENT) == null
                                    && curwin.w_cursor.lnum != end_insert_pos.lnum))
                    && end_insert_pos.lnum <= curbuf.b_ml.ml_line_count)
            {
                pos_C tpos = new pos_C();
                COPY_pos(tpos, curwin.w_cursor);

                COPY_pos(curwin.w_cursor, end_insert_pos);
                check_cursor_col();                     /* make sure it is not past the line */
                for ( ; ; )
                {
                    if (gchar_cursor() == NUL && 0 < curwin.w_cursor.col)
                        --curwin.w_cursor.col;
                    cc = gchar_cursor();
                    if (!vim_iswhite(cc))
                        break;
                    if (del_char(true) == false)
                        break;  /* should not happen */
                }
                if (curwin.w_cursor.lnum != tpos.lnum)
                    COPY_pos(curwin.w_cursor, tpos);
                else
                {
                    /* reset tpos, could have been invalidated in the loop above */
                    COPY_pos(tpos, curwin.w_cursor);
                    tpos.col++;
                    if (cc != NUL && gchar_pos(tpos) == NUL)
                        curwin.w_cursor.col++;      /* put cursor back on the NUL */
                }

                /* <C-S-Right> may have started Visual mode, adjust the position for deleted characters. */
                if (VIsual_active && VIsual.lnum == curwin.w_cursor.lnum)
                {
                    int len = strlen(ml_get_curline());

                    if (len < VIsual.col)
                    {
                        VIsual.col = len;
                        VIsual.coladd = 0;
                    }
                }
            }
        }

        did_ai = false;
        did_si = false;
        can_si = false;
        can_si_back = false;

        /* Set '[ and '] to the inserted text.
         * When end_insert_pos is null we are now in a different buffer. */
        if (end_insert_pos != null)
        {
            COPY_pos(curbuf.b_op_start, insStart);
            COPY_pos(curbuf.b_op_start_orig, insStart_orig);
            COPY_pos(curbuf.b_op_end, end_insert_pos);
        }
    }

    /*
     * Set the last inserted text to a single character.
     * Used for the replace command.
     */
    /*private*/ static void set_last_insert(int c)
    {
        last_insert = new Bytes(MB_MAXBYTES * 3 + 5);

        Bytes s = last_insert;
        /* Use the CTRL-V only when entering a special char. */
        if (c < ' ' || c == DEL)
            (s = s.plus(1)).be(-1, Ctrl_V);
        s = add_char2buf(c, s);
        (s = s.plus(1)).be(-1, ESC);
        (s = s.plus(1)).be(-1, NUL);
        last_insert_skip = 0;
    }

    /*
     * Add character "c" to buffer "s".
     * Escape the special meaning of KB_SPECIAL and CSI.
     * Handle multi-byte characters.
     * Returns a pointer to after the added bytes.
     */
    /*private*/ static Bytes add_char2buf(int c, Bytes s)
    {
        Bytes temp = new Bytes(MB_MAXBYTES + 1);
        int len = utf_char2bytes(c, temp);
        for (int i = 0; i < len; i++)
        {
            byte b = temp.at(i);
            /* Need to escape KB_SPECIAL and CSI like in the typeahead buffer. */
            if (b == KB_SPECIAL)
            {
                (s = s.plus(1)).be(-1, KB_SPECIAL);
                (s = s.plus(1)).be(-1, KS_SPECIAL);
                (s = s.plus(1)).be(-1, KE_FILLER);
            }
            else
                (s = s.plus(1)).be(-1, b);
        }
        return s;
    }

    /*
     * Move cursor to start of line:
     *  if (flags & BL_WHITE) move to first non-white;
     *  if (flags & BL_SOL)   move to first non-white if startofline is set, otherwise keep "curswant" column;
     *  if (flags & BL_FIX)   don't leave the cursor on a NUL.
     */
    /*private*/ static void beginline(int flags)
    {
        if ((flags & BL_SOL) != 0 && !p_sol[0])
            coladvance(curwin.w_curswant);
        else
        {
            curwin.w_cursor.col = 0;
            curwin.w_cursor.coladd = 0;

            if ((flags & (BL_WHITE | BL_SOL)) != 0)
            {
                for (Bytes ptr = ml_get_curline(); vim_iswhite(ptr.at(0))
                                   && !((flags & BL_FIX) != 0 && ptr.at(1) == NUL); ptr = ptr.plus(1))
                    curwin.w_cursor.col++;
            }
            curwin.w_set_curswant = true;
        }
    }

    /*
     * oneright oneleft cursor_down cursor_up
     *
     * Move one char {right,left,down,up}.
     * Doesn't move onto the NUL past the end of the line, unless it is allowed.
     * Return true when successful, false when we hit a line of file boundary.
     */

    /*private*/ static boolean oneright()
    {
        if (virtual_active())
        {
            pos_C prevpos = new pos_C();
            COPY_pos(prevpos, curwin.w_cursor);

            /* Adjust for multi-wide char (excluding TAB). */
            Bytes ptr = ml_get_cursor();
            coladvance(getviscol() + ((ptr.at(0) != TAB && vim_isprintc(us_ptr2char(ptr))) ? mb_ptr2cells(ptr) : 1));
            curwin.w_set_curswant = true;
            /* Return true if the cursor moved, false otherwise (at window edge). */
            return (prevpos.col != curwin.w_cursor.col || prevpos.coladd != curwin.w_cursor.coladd);
        }

        Bytes ptr = ml_get_cursor();
        if (ptr.at(0) == NUL)
            return false;           /* already at the very end */

        int l = us_ptr2len_cc(ptr);

        /* Move "l" bytes right, but don't end up on the NUL, unless 'virtualedit' contains "onemore". */
        if (ptr.at(l) == NUL && (ve_flags[0] & VE_ONEMORE) == 0)
            return false;
        curwin.w_cursor.col += l;

        curwin.w_set_curswant = true;
        return true;
    }

    /*private*/ static boolean oneleft()
    {
        if (virtual_active())
        {
            int v = getviscol();
            if (v == 0)
                return false;

            /* We might get stuck on 'showbreak', skip over it. */
            for (int width = 1; ; )
            {
                coladvance(v - width);
                /* getviscol() is slow, skip it when 'showbreak' is empty,
                 * 'breakindent' is not set and there are no multi-byte characters */
                if (getviscol() < v)
                    break;
                width++;
            }

            if (curwin.w_cursor.coladd == 1)
            {
                /* Adjust for multi-wide char (not a TAB). */
                Bytes ptr = ml_get_cursor();
                if (ptr.at(0) != TAB && vim_isprintc(us_ptr2char(ptr)) && 1 < mb_ptr2cells(ptr))
                    curwin.w_cursor.coladd = 0;
            }

            curwin.w_set_curswant = true;
            return true;
        }

        if (curwin.w_cursor.col == 0)
            return false;

        curwin.w_set_curswant = true;
        --curwin.w_cursor.col;

        /* If the character on the left of the current cursor is a multi-byte character,
         * move to its first byte. */
        mb_adjust_pos(curbuf, curwin.w_cursor);
        return true;
    }

    /*private*/ static boolean cursor_up(long n, boolean upd_topline)
        /* upd_topline: When true: update topline */
    {
        if (0 < n)
        {
            long lnum = curwin.w_cursor.lnum;
            /* This fails if the cursor is already in the first line or the count
             * is larger than the line number and '-' is in 'cpoptions'. */
            if (lnum <= 1 || (lnum <= n && vim_strbyte(p_cpo[0], CPO_MINUS) != null))
                return false;
            if (lnum <= n)
                lnum = 1;
            else
                lnum -= n;
            curwin.w_cursor.lnum = lnum;
        }

        /* try to advance to the column we want to be at */
        coladvance(curwin.w_curswant);

        if (upd_topline)
            update_topline();       /* make sure curwin.w_topline is valid */

        return true;
    }

    /*
     * Cursor down a number of logical lines.
     */
    /*private*/ static boolean cursor_down(long n, boolean upd_topline)
        /* upd_topline: When true: update topline */
    {
        if (0 < n)
        {
            long lnum = curwin.w_cursor.lnum;
            /* This fails if the cursor is already in the last line
             * or would move beyond the last line and '-' is in 'cpoptions'. */
            if (curbuf.b_ml.ml_line_count <= lnum
                    || (curbuf.b_ml.ml_line_count < lnum + n && vim_strbyte(p_cpo[0], CPO_MINUS) != null))
                return false;
            if (curbuf.b_ml.ml_line_count <= lnum + n)
                lnum = curbuf.b_ml.ml_line_count;
            else
                lnum += n;
            curwin.w_cursor.lnum = lnum;
        }

        /* try to advance to the column we want to be at */
        coladvance(curwin.w_curswant);

        if (upd_topline)
            update_topline();       /* make sure curwin.w_topline is valid */

        return true;
    }

    /*
     * Stuff the last inserted text in the read buffer.
     * Last_insert actually is a copy of the redo buffer, so we
     * first have to remove the command.
     */
    /*private*/ static boolean stuff_inserted(int c, long count, boolean no_esc)
        /* c: Command character to be inserted */
        /* count: Repeat this many times */
        /* no_esc: Don't add an ESC at the end */
    {
        Bytes ptr = get_last_insert();
        if (ptr == null)
        {
            emsg(e_noinstext);
            return false;
        }

        /* may want to stuff the command character, to start Insert mode */
        if (c != NUL)
            stuffcharReadbuff(c);
        Bytes esc_ptr = vim_strrchr(ptr, ESC);
        if (esc_ptr != null)
            esc_ptr.be(0, NUL);     /* remove the ESC */

        byte last = NUL;

        /* when the last char is either "0" or "^" it will be quoted if no ESC comes
         * after it OR if it will inserted more than once and "ptr" starts with ^D.
         */
        Bytes last_ptr = (esc_ptr != null) ? esc_ptr.minus(1) : ptr.plus(strlen(ptr) - 1);
        if (BLE(ptr, last_ptr) && (last_ptr.at(0) == (byte)'0' || last_ptr.at(0) == (byte)'^')
                && (no_esc || (ptr.at(0) == Ctrl_D && 1 < count)))
        {
            last = last_ptr.at(0);
            last_ptr.be(0, NUL);
        }

        do
        {
            stuffReadbuff(ptr);
            /* a trailing "0" is inserted as "<C-V>048", "^" as "<C-V>^" */
            if (last != NUL)
                stuffReadbuff((last == '0') ? u8("\026\060\064\070") : u8("\026^"));
        } while (0 < --count);

        if (last != NUL)
            last_ptr.be(0, last);

        if (esc_ptr != null)
            esc_ptr.be(0, ESC);     /* put the ESC back */

        /* may want to stuff a trailing ESC, to get out of Insert mode */
        if (!no_esc)
            stuffcharReadbuff(ESC);

        return true;
    }

    /*private*/ static Bytes get_last_insert()
    {
        if (last_insert == null)
            return null;

        return last_insert.plus(last_insert_skip);
    }

    /*
     * Get last inserted string, and remove trailing <Esc>.
     * Returns pointer to allocated memory (must be freed) or null.
     */
    /*private*/ static Bytes get_last_insert_save()
    {
        if (last_insert == null)
            return null;

        Bytes s = STRDUP(last_insert.plus(last_insert_skip));
        int len = strlen(s);
        if (0 < len && s.at(len - 1) == ESC)       /* remove trailing ESC */
            s.be(len - 1, NUL);
        return s;
    }

    /*
     * Check the word in front of the cursor for an abbreviation.
     * Called when the non-id character "c" has been entered.
     * When an abbreviation is recognized it is removed from the text and
     * the replacement string is inserted in typebuf.tb_buf[], followed by "c".
     */
    /*private*/ static boolean echeck_abbr(int c)
    {
        /* Don't check for abbreviation in paste mode, when disabled,
         * or just after moving around with cursor keys. */
        if (p_paste[0] || no_abbr || arrow_used)
            return false;

        return check_abbr(c, ml_get_curline(), curwin.w_cursor.col,
                    (curwin.w_cursor.lnum == insStart.lnum) ? insStart.col : 0);
    }

    /*
     * replace-stack functions
     *
     * When replacing characters, the replaced characters are remembered for each
     * new character.  This is used to re-insert the old text when backspacing.
     *
     * There is a NUL headed list of characters for each character that is
     * currently in the file after the insertion point.  When BS is used, one NUL
     * headed list is put back for the deleted character.
     *
     * For a newline, there are two NUL headed lists.  One contains the characters
     * that the NL replaced.  The extra one stores the characters after the cursor
     * that were deleted (always white space).
     *
     * Replace_offset is normally 0, in which case replace_push will add a new
     * character at the end of the stack.  If replace_offset is not 0, that many
     * characters will be left on the stack above the newly inserted character.
     */

    /*private*/ static Bytes    replace_stack;
    /*private*/ static int      replace_stack_nr;       /* next entry in replace stack */
    /*private*/ static int      replace_stack_len;      /* max. number of entries */

    /*private*/ static void replace_push(int c)
        /* c: character that is replaced (NUL is none) */
    {
        if (replace_stack_nr < replace_offset)      /* nothing to do */
            return;

        if (replace_stack_len <= replace_stack_nr)
        {
            replace_stack_len += 50;
            Bytes p = new Bytes(replace_stack_len);
            if (replace_stack != null)
                BCOPY(p, replace_stack, replace_stack_nr);
            replace_stack = p;
        }

        Bytes p = replace_stack.plus(replace_stack_nr - replace_offset);
        if (replace_offset != 0)
            BCOPY(p, 1, p, 0, replace_offset);
        p.be(0, c);
        replace_stack_nr++;
    }

    /*
     * Push a character onto the replace stack.
     * Handles a multi-byte character in reverse byte order, so that the first byte is popped off first.
     * Return the number of bytes done (includes composing characters).
     */
    /*private*/ static int replace_push_mb(Bytes p)
    {
        int l = us_ptr2len_cc(p);

        for (int j = l - 1; 0 <= j; --j)
            replace_push(p.at(j));

        return l;
    }

    /*
     * Pop one item from the replace stack.
     * return -1 if stack empty
     * return replaced character or NUL otherwise
     */
    /*private*/ static int replace_pop()
    {
        if (replace_stack_nr == 0)
            return -1;

        return replace_stack.at(--replace_stack_nr);
    }

    /*
     * Join the top two items on the replace stack.  This removes to "off"'th NUL encountered.
     */
    /*private*/ static void replace_join(int off)
        /* off: offset for which NUL to remove */
    {
        for (int i = replace_stack_nr; 0 <= --i; )
            if (replace_stack.at(i) == NUL && off-- <= 0)
            {
                --replace_stack_nr;
                BCOPY(replace_stack, i, replace_stack, i + 1, replace_stack_nr - i);
                return;
            }
    }

    /*
     * Pop bytes from the replace stack until a NUL is found, and insert them
     * before the cursor.  Can only be used in REPLACE or VREPLACE mode.
     */
    /*private*/ static void replace_pop_ins()
    {
        int oldState = State;
        State = NORMAL;                     /* don't want REPLACE here */

        for (int cc; 0 < (cc = replace_pop()); )
        {
            mb_replace_pop_ins(cc);
            dec_cursor();
        }

        State = oldState;
    }

    /*
     * Insert bytes popped from the replace stack. "cc" is the first byte.
     * If it indicates a multi-byte char, pop the other bytes too.
     */
    /*private*/ static void mb_replace_pop_ins(int cc)
    {
        Bytes buf = new Bytes(MB_MAXBYTES + 1);

        int n = mb_byte2len(cc);
        if (1 < n)
        {
            buf.be(0, cc);
            for (int i = 1; i < n; i++)
                buf.be(i, replace_pop());
            ins_bytes_len(buf, n);
        }
        else
            ins_char(cc);

        /* Handle composing chars. */
        for ( ; ; )
        {
            int c = replace_pop();
            if (c == -1)            /* stack empty */
                break;
            if ((n = mb_byte2len(c)) == 1)
            {
                /* Not a multi-byte char, put it back. */
                replace_push(c);
                break;
            }
            else
            {
                buf.be(0, c);
                for (int i = 1; i < n; i++)
                    buf.be(i, replace_pop());
                if (utf_iscomposing(us_ptr2char(buf)))
                    ins_bytes_len(buf, n);
                else
                {
                    /* Not a composing char, put it back. */
                    for (int i = n - 1; 0 <= i; --i)
                        replace_push(buf.at(i));
                    break;
                }
            }
        }
    }

    /*
     * make the replace stack empty
     * (called when exiting replace mode)
     */
    /*private*/ static void replace_flush()
    {
        replace_stack = null;
        replace_stack_len = 0;
        replace_stack_nr = 0;
    }

    /*
     * Handle doing a BS for one character.
     * cc < 0: replace stack empty, just move cursor
     * cc == 0: character was inserted, delete it
     * cc > 0: character was replaced, put cc (first byte of original char) back
     * and check for more characters to be put back
     * When "limit_col" is >= 0, don't delete before this column.  Matters when
     * using composing characters, use del_char_after_col() instead of del_char().
     */
    /*private*/ static void replace_do_bs(int limit_col)
    {
        int orig_len = 0;
        int orig_vcols = 0;

        int cc = replace_pop();
        if (0 < cc)
        {
            int[] start_vcol = new int[1];
            if ((State & VREPLACE_FLAG) != 0)
            {
                /* Get the number of screen cells used by the character we are going to delete. */
                getvcol(curwin, curwin.w_cursor, null, start_vcol, null);
                orig_vcols = chartabsize(ml_get_cursor(), start_vcol[0]);
            }

            del_char_after_col(limit_col);
            if ((State & VREPLACE_FLAG) != 0)
                orig_len = strlen(ml_get_cursor());
            replace_push(cc);

            replace_pop_ins();

            if ((State & VREPLACE_FLAG) != 0)
            {
                /* Get the number of screen cells used by the inserted characters. */
                Bytes p = ml_get_cursor();
                int ins_len = strlen(p) - orig_len;
                int vcol = start_vcol[0];
                for (int i = 0; i < ins_len; i++)
                {
                    vcol += chartabsize(p.plus(i), vcol);
                    i += us_ptr2len_cc(p) - 1;
                }
                vcol -= start_vcol[0];

                /* Delete spaces that were inserted after the cursor to keep the text aligned. */
                curwin.w_cursor.col += ins_len;
                while (orig_vcols < vcol && gchar_cursor() == ' ')
                {
                    del_char(false);
                    orig_vcols++;
                }
                curwin.w_cursor.col -= ins_len;
            }

            /* mark the buffer as changed and prepare for displaying */
            changed_bytes(curwin.w_cursor.lnum, curwin.w_cursor.col);
        }
        else if (cc == 0)
            del_char_after_col(limit_col);
    }

    /*
     * Return true if C-indenting is on.
     */
    /*private*/ static boolean cindent_on()
    {
        return (!p_paste[0] && (curbuf.b_p_cin[0] || curbuf.b_p_inde[0].at(0) != NUL));
    }

    /*
     * Re-indent the current line, based on the current contents of it and the
     * surrounding lines.  Fixing the cursor position seems really easy -- I'm very
     * confused what all the part that handles Control-T is doing that I'm not.
     * "getindent" should be "get_c_indent", "get_expr_indent" or "get_lisp_indent".
     */
    /*private*/ static abstract class getindent_C
    {
        public abstract int getindent();
    }

    /*private*/ static void fixthisline(getindent_C getindent)
    {
        change_indent(INDENT_SET, getindent.getindent(), false, NUL, true);
        if (linewhite(curwin.w_cursor.lnum))
            did_ai = true;      /* delete the indent if the line stays empty */
    }

    /*private*/ static void fix_indent()
    {
        if (p_paste[0])
            return;

        if (curbuf.b_p_lisp[0] && curbuf.b_p_ai[0])
            fixthisline(get_lisp_indent);
        else if (cindent_on())
            do_c_expr_indent();
    }

    /*
     * return true if 'cinkeys' contains the key "keytyped",
     * when == '*':     Only if key is preceded with '*'    (indent before insert)
     * when == '!':     Only if key is preceded with '!'    (don't insert)
     * when == ' ':     Only if key is not preceded with '*'(indent afterwards)
     *
     * "keytyped" can have a few special values:
     * KEY_OPEN_FORW
     * KEY_OPEN_BACK
     * KEY_COMPLETE     just finished completion.
     *
     * If line_is_empty is true accept keys with '0' before them.
     */
    /*private*/ static boolean in_cinkeys(int keytyped, int when, boolean line_is_empty)
    {
        if (keytyped == NUL)
            /* Can happen with CTRL-Y and CTRL-E on a short line. */
            return false;

        Bytes look;
        if (curbuf.b_p_inde[0].at(0) != NUL)
            look = curbuf.b_p_indk[0];     /* 'indentexpr' set: use 'indentkeys' */
        else
            look = curbuf.b_p_cink[0];     /* 'indentexpr' empty: use 'cinkeys' */
        while (look.at(0) != NUL)
        {
            boolean try_match;
            /*
             * Find out if we want to try a match with this key,
             * depending on 'when' and a '*' or '!' before the key.
             */
            switch (when)
            {
                case '*': try_match = (look.at(0) == (byte)'*'); break;
                case '!': try_match = (look.at(0) == (byte)'!'); break;
                 default: try_match = (look.at(0) != (byte)'*'); break;
            }
            if (look.at(0) == (byte)'*' || look.at(0) == (byte)'!')
                look = look.plus(1);

            /*
             * If there is a '0', only accept a match if the line is empty.
             * But may still match when typing last char of a word.
             */
            boolean try_match_word;
            if (look.at(0) == (byte)'0')
            {
                try_match_word = try_match;
                if (!line_is_empty)
                    try_match = false;
                look = look.plus(1);
            }
            else
                try_match_word = false;

            /*
             * does it look like a control character?
             */
            if (look.at(0) == (byte)'^' && '?' <= look.at(1) && look.at(1) <= '_')
            {
                if (try_match && keytyped == ctrl_key(look.at(1)))
                    return true;
                look = look.plus(2);
            }
            /*
             * 'o' means "o" command, open forward.
             * 'O' means "O" command, open backward.
             */
            else if (look.at(0) == (byte)'o')
            {
                if (try_match && keytyped == KEY_OPEN_FORW)
                    return true;
                look = look.plus(1);
            }
            else if (look.at(0) == (byte)'O')
            {
                if (try_match && keytyped == KEY_OPEN_BACK)
                    return true;
                look = look.plus(1);
            }

            /*
             * 'e' means to check for "else" at start of line and just before the cursor.
             */
            else if (look.at(0) == (byte)'e')
            {
                if (try_match && keytyped == 'e' && 4 <= curwin.w_cursor.col)
                {
                    Bytes p = ml_get_curline();
                    if (BEQ(skipwhite(p), p.plus(curwin.w_cursor.col - 4))
                             && STRNCMP(p.plus(curwin.w_cursor.col - 4), u8("else"), 4) == 0)
                        return true;
                }
                look = look.plus(1);
            }

            /*
             * ':' only causes an indent if it is at the end of a label or case statement,
             * or when it was before typing the ':' (to fix class::method for C++).
             */
            else if (look.at(0) == (byte)':')
            {
                if (try_match && keytyped == ':')
                {
                    Bytes p = ml_get_curline();
                    if (cin_iscase(p, false) || cin_isscopedecl(p) || cin_islabel())
                        return true;
                    /* Need to get the line again after cin_islabel(). */
                    p = ml_get_curline();
                    if (2 < curwin.w_cursor.col
                            && p.at(curwin.w_cursor.col - 1) == (byte)':'
                            && p.at(curwin.w_cursor.col - 2) == (byte)':')
                    {
                        p.be(curwin.w_cursor.col - 1, (byte)' ');
                        boolean i = (cin_iscase(p, false) || cin_isscopedecl(p) || cin_islabel());
                        p = ml_get_curline();
                        p.be(curwin.w_cursor.col - 1, (byte)':');
                        if (i)
                            return true;
                    }
                }
                look = look.plus(1);
            }

            /*
             * Is it a key in <>, maybe?
             */
            else if (look.at(0) == (byte)'<')
            {
                if (try_match)
                {
                    /*
                     * Make up some named keys <o>, <O>, <e>, <0>, <>>, <<>, <*>, <:> and <!>
                     * so that people can re-indent on o, O, e, 0, <, >, *, : and ! keys
                     * if they really really want to.
                     */
                    if (vim_strbyte(u8("<>!*oOe0:"), look.at(1)) != null && keytyped == look.at(1))
                        return true;

                    if (keytyped == get_special_key_code(look.plus(1)))
                        return true;
                }
                while (look.at(0) != NUL && look.at(0) != (byte)'>')
                    look = look.plus(1);
                while (look.at(0) == (byte)'>')
                    look = look.plus(1);
            }

            /*
             * Is it a word: "=word"?
             */
            else if (look.at(0) == (byte)'=' && look.at(1) != (byte)',' && look.at(1) != NUL)
            {
                boolean icase = false;
                look = look.plus(1);
                if (look.at(0) == (byte)'~')
                {
                    icase = true;
                    look = look.plus(1);
                }
                Bytes p = vim_strchr(look, ',');
                if (p == null)
                    p = look.plus(strlen(look));
                int diff = BDIFF(p, look);
                if ((try_match || try_match_word) && diff <= curwin.w_cursor.col)
                {
                    boolean match = false;

                    /* TODO: multi-byte */
                    if (keytyped == (int)p.at(-1)
                        || (icase && keytyped < 256 && asc_tolower(keytyped) == asc_tolower((int)p.at(-1))))
                    {
                        Bytes line = ml_get_cursor();
                        if ((curwin.w_cursor.col == diff
                                    || !us_iswordb(line.at(-diff - 1), curbuf))
                                && (icase
                                    ? us_strnicmp(line.minus(diff), look, diff)
                                    : STRNCMP(line.minus(diff), look, diff)) == 0)
                            match = true;
                    }
                    if (match && try_match_word && !try_match)
                    {
                        /* "0=word": Check if there are only blanks before the word. */
                        Bytes line = ml_get_curline();
                        if (BDIFF(skipwhite(line), line) != curwin.w_cursor.col - diff)
                            match = false;
                    }
                    if (match)
                        return true;
                }
                look = p;
            }

            /*
             * ok, it's a boring generic character.
             */
            else
            {
                if (try_match && look.at(0) == keytyped)
                    return true;
                look = look.plus(1);
            }

            /*
             * Skip over ", ".
             */
            look = skip_to_option_part(look);
        }

        return false;
    }

    /*private*/ static void ins_reg()
    {
        boolean need_redraw = false;
        int literally = 0;
        boolean vis_active = VIsual_active;

        /*
         * If we are going to wait for a character, show a '"'.
         */
        pc_status = PC_STATUS_UNSET;
        if (redrawing() && !char_avail())
        {
            /* may need to redraw when no more chars available now */
            ins_redraw(false);

            edit_putchar('"', true);
            add_to_showcmd_c(Ctrl_R);
        }

        /*
         * Don't map the register name.
         * This also prevents the mode message to be deleted when ESC is hit.
         */
        no_mapping++;
        int regname = plain_vgetc();
        if (regname == Ctrl_R || regname == Ctrl_O || regname == Ctrl_P)
        {
            /* Get a third key for literal register insertion. */
            literally = regname;
            add_to_showcmd_c(literally);
            regname = plain_vgetc();
        }
        --no_mapping;

        /* Don't call u_sync() while typing the expression or giving an error message for it.
         * Only call it explicitly. */
        no_u_sync++;
        if (regname == '=')
        {
            /* Sync undo when evaluating the expression calls setline() or append(),
             * so that it can be undone separately. */
            u_sync_once = 2;

            regname = get_expr_register();
        }
        if (regname == NUL || !valid_yank_reg(regname, false))
        {
            vim_beep();
            need_redraw = true;     /* remove the '"' */
        }
        else
        {
            if (literally == Ctrl_O || literally == Ctrl_P)
            {
                /* Append the command to the redo buffer. */
                appendCharToRedobuff(Ctrl_R);
                appendCharToRedobuff(literally);
                appendCharToRedobuff(regname);

                do_put(regname, BACKWARD, 1, (literally == Ctrl_P ? PUT_FIXINDENT : 0) | PUT_CURSEND);
            }
            else if (insert_reg(regname, literally != 0) == false)
            {
                vim_beep();
                need_redraw = true; /* remove the '"' */
            }
            else if (stop_insert_mode)
                /* When the '=' register was used and a function was invoked that
                 * did ":stopinsert" then stuff_empty() returns false but we won't
                 * insert anything, need to remove the '"' */
                need_redraw = true;
        }
        --no_u_sync;
        if (u_sync_once == 1)
            ins_need_undo = true;
        u_sync_once = 0;
        clear_showcmd();

        /* If the inserted register is empty, we need to remove the '"'. */
        if (need_redraw || stuff_empty())
            edit_unputchar();

        /* Disallow starting Visual mode here, would get a weird mode. */
        if (!vis_active && VIsual_active)
            end_visual_mode();
    }

    /*
     * CTRL-G commands in Insert mode.
     */
    /*private*/ static void ins_ctrl_g()
    {
        /*
         * Don't map the second key.  This also prevents the mode message to be deleted when ESC is hit.
         */
        no_mapping++;
        int c = plain_vgetc();
        --no_mapping;
        switch (c)
        {
            /* CTRL-G k and CTRL-G <Up>: cursor up to insStart.col. */
            case K_UP:
            case Ctrl_K:
            case 'k': ins_up(true);
                      break;

            /* CTRL-G j and CTRL-G <Down>: cursor down to insStart.col. */
            case K_DOWN:
            case Ctrl_J:
            case 'j': ins_down(true);
                      break;

            /* CTRL-G u: start new undoable edit. */
            case 'u': u_sync(true);
                      ins_need_undo = true;

                      /* Need to reset insStart, esp. because a BS that joins
                       * a line to the previous one must save for undo. */
                      update_insStart_orig = false;
                      COPY_pos(insStart, curwin.w_cursor);
                      break;

            /* Unknown CTRL-G command, reserved for future expansion. */
            default:  vim_beep();
        }
    }

    /*
     * CTRL-^ in Insert mode.
     */
    /*private*/ static void ins_ctrl_hat()
    {
        if (map_to_exists_mode(u8(""), LANGMAP, false))
        {
            /* ":lmap" mappings exists, Toggle use of ":lmap" mappings. */
            if ((State & LANGMAP) != 0)
            {
                curbuf.b_p_iminsert[0] = B_IMODE_NONE;
                State &= ~LANGMAP;
            }
            else
            {
                curbuf.b_p_iminsert[0] = B_IMODE_LMAP;
                State |= LANGMAP;
            }
        }
        set_iminsert_global();
        showmode();
    }

    /*private*/ static boolean disabled_redraw;

    /*
     * Handle ESC in insert mode.
     * Returns true when leaving insert mode, false when going to repeat the insert.
     */
    /*private*/ static boolean ins_esc(long[] count, int cmdchar, boolean nomove)
        /* nomove: don't move cursor */
    {
        int temp = curwin.w_cursor.col;

        if (disabled_redraw)
        {
            --redrawingDisabled;
            disabled_redraw = false;
        }
        if (!arrow_used)
        {
            /*
             * Don't append the ESC for "r<CR>" and "grx".
             * When 'insertmode' is set only CTRL-L stops Insert mode.
             * Needed for when "count" is non-zero.
             */
            if (cmdchar != 'r' && cmdchar != 'v')
                appendToRedobuff(p_im[0] ? u8("\014") : ESC_STR);

            /*
             * Repeating insert may take a long time.  Check for interrupt now and then.
             */
            if (0 < count[0])
            {
                line_breakcheck();
                if (got_int)
                    count[0] = 0;
            }

            if (0 < --count[0])       /* repeat what was typed */
            {
                /* Vi repeats the insert without replacing characters. */
                if (vim_strbyte(p_cpo[0], CPO_REPLCNT) != null)
                    State &= ~REPLACE_FLAG;

                start_redo_ins();
                if (cmdchar == 'r' || cmdchar == 'v')
                    stuffRedoReadbuff(ESC_STR);     /* no ESC in redo buffer */
                redrawingDisabled++;
                disabled_redraw = true;
                return false;       /* repeat the insert */
            }
            stop_insert(curwin.w_cursor, true, nomove);
            undisplay_dollar();
        }

        /* When an autoindent was removed, curswant stays after the indent. */
        if (restart_edit == NUL && temp == curwin.w_cursor.col)
            curwin.w_set_curswant = true;

        /* Remember the last Insert position in the '^ mark. */
        if (!cmdmod.keepjumps)
            COPY_pos(curbuf.b_last_insert, curwin.w_cursor);

        /*
         * The cursor should end up on the last inserted character.
         * Don't do it for CTRL-O, unless past the end of the line.
         */
        if (!nomove
                && (curwin.w_cursor.col != 0 || 0 < curwin.w_cursor.coladd)
                && (restart_edit == NUL || (gchar_cursor() == NUL && !VIsual_active))
                && !revins_on)
        {
            if (0 < curwin.w_cursor.coladd || ve_flags[0] == VE_ALL)
            {
                oneleft();
                if (restart_edit != NUL)
                    curwin.w_cursor.coladd++;
            }
            else
            {
                --curwin.w_cursor.col;
                /* Correct cursor for multi-byte character. */
                mb_adjust_pos(curbuf, curwin.w_cursor);
            }
        }

        State = NORMAL;
        /* need to position cursor again (e.g. when on a TAB ) */
        changed_cline_bef_curs();

        setmouse();
        ui_cursor_shape();          /* may show different cursor shape */

        /*
         * When recording or for CTRL-O, need to display the new mode.
         * Otherwise remove the mode message.
         */
        if (Recording || restart_edit != NUL)
            showmode();
        else if (p_smd[0])
            msg(u8(""));

        return true;            /* exit Insert mode */
    }

    /*
     * Toggle language: revins_on.
     * Move to end of reverse inserted text.
     */
    /*private*/ static void ins_ctrl_()
    {
        if (revins_on && revins_chars != 0 && 0 <= revins_scol)
        {
            while (gchar_cursor() != NUL && 0 < revins_chars--)
                curwin.w_cursor.col++;
        }
        p_ri[0] = !p_ri[0];
        revins_on = (State == INSERT && p_ri[0]);
        if (revins_on)
        {
            revins_scol = curwin.w_cursor.col;
            revins_legal++;
            revins_chars = 0;
            undisplay_dollar();
        }
        else
            revins_scol = -1;
        showmode();
    }

    /*
     * If 'keymodel' contains "startsel", may start selection.
     * Returns true when a CTRL-O and other keys stuffed.
     */
    /*private*/ static boolean ins_start_select(int c)
    {
        if (km_startsel)
            switch (c)
            {
                case K_KHOME:
                case K_KEND:
                case K_PAGEUP:
                case K_KPAGEUP:
                case K_PAGEDOWN:
                case K_KPAGEDOWN:
                    if ((mod_mask & MOD_MASK_SHIFT) == 0)
                        break;
                    /* FALLTHROUGH */
                case K_S_LEFT:
                case K_S_RIGHT:
                case K_S_UP:
                case K_S_DOWN:
                case K_S_END:
                case K_S_HOME:
                    /* Start selection right away, the cursor can move with
                     * CTRL-O when beyond the end of the line. */
                    start_selection();

                    /* Execute the key in (insert) Select mode. */
                    stuffcharReadbuff(Ctrl_O);
                    if (mod_mask != 0)
                    {
                        Bytes buf = new Bytes(4);

                        buf.be(0, KB_SPECIAL);
                        buf.be(1, KS_MODIFIER);
                        buf.be(2, mod_mask);
                        buf.be(3, NUL);
                        stuffReadbuff(buf);
                    }
                    stuffcharReadbuff(c);
                    return true;
            }

        return false;
    }

    /*
     * <Insert> key in Insert mode: toggle insert/replace mode.
     */
    /*private*/ static void ins_insert(int replaceState)
    {
        set_vim_var_string(VV_INSERTMODE,
                    ((State & REPLACE_FLAG) != 0) ? u8("i") : (replaceState == VREPLACE) ? u8("v") : u8("r"), 1);
        apply_autocmds(EVENT_INSERTCHANGE, null, null, false, curbuf);
        if ((State & REPLACE_FLAG) != 0)
            State = INSERT | (State & LANGMAP);
        else
            State = replaceState | (State & LANGMAP);
        appendCharToRedobuff(K_INS);
        showmode();
        ui_cursor_shape();          /* may show different cursor shape */
    }

    /*
     * Pressed CTRL-O in Insert mode.
     */
    /*private*/ static void ins_ctrl_o()
    {
        if ((State & VREPLACE_FLAG) != 0)
            restart_edit = 'V';
        else if ((State & REPLACE_FLAG) != 0)
            restart_edit = 'R';
        else
            restart_edit = 'I';

        if (virtual_active())
            ins_at_eol = false;     /* cursor always keeps its column */
        else
            ins_at_eol = (gchar_cursor() == NUL);
    }

    /*
     * If the cursor is on an indent, ^T/^D insert/delete one shiftwidth.
     * Otherwise ^T/^D behave like a "<<" or ">>".
     * Always round the indent to 'shiftwidth', this is compatible with vi.
     * But vi only supports ^T and ^D after an autoindent, we support it everywhere.
     */
    /*private*/ static void ins_shift(int c, int lastc)
    {
        if (!stop_arrow())
            return;

        appendCharToRedobuff(c);

        /*
         * 0^D and ^^D: remove all indent.
         */
        if (c == Ctrl_D && (lastc == '0' || lastc == '^') && 0 < curwin.w_cursor.col)
        {
            --curwin.w_cursor.col;
            del_char(false);                /* delete the '^' or '0' */
            /* In Replace mode, restore the characters that '^' or '0' replaced. */
            if ((State & REPLACE_FLAG) != 0)
                replace_pop_ins();
            if (lastc == '^')
                old_indent = get_indent();  /* remember curr. indent */
            change_indent(INDENT_SET, 0, true, NUL, true);
        }
        else
            change_indent((c == Ctrl_D) ? INDENT_DEC : INDENT_INC, 0, true, NUL, true);

        if (did_ai && skipwhite(ml_get_curline()).at(0) != NUL)
            did_ai = false;
        did_si = false;
        can_si = false;
        can_si_back = false;
        can_cindent = false;        /* no cindenting after ^D or ^T */
    }

    /*private*/ static void ins_del()
    {
        if (!stop_arrow())
            return;

        if (gchar_cursor() == NUL)              /* delete newline */
        {
            int temp = curwin.w_cursor.col;
            if (!can_bs(BS_EOL)                 /* only if "eol" included */
                    || do_join(2, false, true, false, false) == false)
                vim_beep();
            else
                curwin.w_cursor.col = temp;
        }
        else if (del_char(false) == false)      /* delete char under cursor */
            vim_beep();

        did_ai = false;
        did_si = false;
        can_si = false;
        can_si_back = false;

        appendCharToRedobuff(K_DEL);
    }

    /*
     * Delete one character for ins_bs().
     */
    /*private*/ static void ins_bs_one(int[] vcolp)
    {
        dec_cursor();
        getvcol(curwin, curwin.w_cursor, vcolp, null, null);
        if ((State & REPLACE_FLAG) != 0)
        {
            /* Don't delete characters before the insert point when in Replace mode. */
            if (curwin.w_cursor.lnum != insStart.lnum || insStart.col <= curwin.w_cursor.col)
                replace_do_bs(-1);
        }
        else
            del_char(false);
    }

    /*
     * Handle Backspace, delete-word and delete-line in Insert mode.
     * Return true when backspace was actually used.
     */
    /*private*/ static boolean ins_bs(int c, int mode, boolean[] inserted_space_p)
    {
        boolean did_backspace = false;

        /*
         * can't delete anything in an empty file
         * can't backup past first character in buffer
         * can't backup past starting point unless 'backspace' > 1
         * can backup to a previous line if 'backspace' == 0
         */
        if (bufempty()
            || (!revins_on
                && ((curwin.w_cursor.lnum == 1 && curwin.w_cursor.col == 0)
                    || (!can_bs(BS_START)
                        && (arrow_used
                            || (curwin.w_cursor.lnum == insStart_orig.lnum
                                && curwin.w_cursor.col <= insStart_orig.col)))
                    || (!can_bs(BS_INDENT) && !arrow_used && 0 < ai_col && curwin.w_cursor.col <= ai_col)
                    || (!can_bs(BS_EOL) && curwin.w_cursor.col == 0))))
        {
            vim_beep();
            return false;
        }

        if (!stop_arrow())
            return false;

        boolean in_indent = inindent(0);
        if (in_indent)
            can_cindent = false;
        end_comment_pending = NUL;  /* After BS, don't auto-end comment */
        if (revins_on)              /* put cursor after last inserted char */
            inc_cursor();

        /* Virtualedit:
         *  BACKSPACE_CHAR eats a virtual space
         *  BACKSPACE_WORD eats all coladd
         *  BACKSPACE_LINE eats all coladd and keeps going
         */
        if (0 < curwin.w_cursor.coladd)
        {
            if (mode == BACKSPACE_CHAR)
            {
                --curwin.w_cursor.coladd;
                return true;
            }
            if (mode == BACKSPACE_WORD)
            {
                curwin.w_cursor.coladd = 0;
                return true;
            }
            curwin.w_cursor.coladd = 0;
        }

        /*
         * delete newline!
         */
        if (curwin.w_cursor.col == 0)
        {
            long lnum = insStart.lnum;
            if (curwin.w_cursor.lnum == lnum || revins_on)
            {
                if (!u_save(curwin.w_cursor.lnum - 2, curwin.w_cursor.lnum + 1))
                    return false;
                --insStart.lnum;
                insStart.col = MAXCOL;
            }
            /*
             * In replace mode:
             * cc < 0: NL was inserted, delete it
             * cc >= 0: NL was replaced, put original characters back
             */
            int cc = -1;
            if ((State & REPLACE_FLAG) != 0)
                cc = replace_pop();     /* returns -1 if NL was inserted */
            /*
             * In replace mode, in the line we started replacing, we only move the cursor.
             */
            if ((State & REPLACE_FLAG) != 0 && curwin.w_cursor.lnum <= lnum)
            {
                dec_cursor();
            }
            else
            {
                if ((State & VREPLACE_FLAG) == 0 || orig_line_count < curwin.w_cursor.lnum)
                {
                    int temp = gchar_cursor();      /* remember current char */
                    --curwin.w_cursor.lnum;

                    /*
                     * When "aw" is in 'formatoptions' we must delete the space at the end of
                     * the line, otherwise the line will be broken again when auto-formatting.
                     */
                    if (has_format_option(FO_AUTO) && has_format_option(FO_WHITE_PAR))
                    {
                        Bytes ptr = ml_get_buf(curbuf, curwin.w_cursor.lnum, true);

                        int len = strlen(ptr);
                        if (0 < len && ptr.at(len - 1) == (byte)' ')
                            ptr.be(len - 1, NUL);
                    }

                    do_join(2, false, false, false, false);
                    if (temp == NUL && gchar_cursor() != NUL)
                        inc_cursor();
                }
                else
                    dec_cursor();

                /*
                 * In REPLACE mode we have to put back the text that was replaced by the NL.
                 * On the replace stack is first a NUL-terminated sequence of characters
                 * that were deleted and then the characters that NL replaced.
                 */
                if ((State & REPLACE_FLAG) != 0)
                {
                    /*
                     * Do the next ins_char() in NORMAL state, to prevent ins_char()
                     * from replacing characters and avoiding showmatch().
                     */
                    int oldState = State;
                    State = NORMAL;
                    /*
                     * restore characters (blanks) deleted after cursor
                     */
                    while (0 < cc)
                    {
                        int save_col = curwin.w_cursor.col;
                        mb_replace_pop_ins(cc);
                        curwin.w_cursor.col = save_col;
                        cc = replace_pop();
                    }
                    /* restore the characters that NL replaced */
                    replace_pop_ins();
                    State = oldState;
                }
            }
            did_ai = false;
        }
        else
        {
            /*
             * Delete character(s) before the cursor.
             */
            if (revins_on)          /* put cursor on last inserted char */
                dec_cursor();
            int mincol = 0;
                                                    /* keep indent */
            if (mode == BACKSPACE_LINE && (curbuf.b_p_ai[0] || cindent_on()) && !revins_on)
            {
                int save_col = curwin.w_cursor.col;
                beginline(BL_WHITE);
                if (curwin.w_cursor.col < save_col)
                    mincol = curwin.w_cursor.col;
                curwin.w_cursor.col = save_col;
            }

            /*
             * Handle deleting one 'shiftwidth' or 'softtabstop'.
             */
            if (mode == BACKSPACE_CHAR
                    && ((p_sta[0] && in_indent)
                        || (get_sts_value() != 0
                            && 0 < curwin.w_cursor.col
                            && (ml_get_cursor().at(-1) == TAB
                                || (ml_get_cursor().at(-1) == (byte)' '
                                    && (!inserted_space_p[0]
                                        || arrow_used))))))
            {
                inserted_space_p[0] = false;

                int ts;
                if (p_sta[0] && in_indent)
                    ts = (int)get_sw_value(curbuf);
                else
                    ts = (int)get_sts_value();

                /* Compute the virtual column where we want to be.  Since 'showbreak' may
                 * get in the way, need to get the last column of the previous character. */
                int[] vcol = new int[1];
                getvcol(curwin, curwin.w_cursor, vcol, null, null);
                int start_vcol = vcol[0];
                dec_cursor();
                int[] want_vcol = new int[1];
                getvcol(curwin, curwin.w_cursor, null, null, want_vcol);
                inc_cursor();
                want_vcol[0] = (want_vcol[0] / ts) * ts;

                /* delete characters until we are at or before "want_vcol" */
                while (want_vcol[0] < vcol[0] && vim_iswhite(ml_get_cursor().at(-1)))
                    ins_bs_one(vcol);

                /* insert extra spaces until we are at "want_vcol" */
                while (vcol[0] < want_vcol[0])
                {
                    /* Remember the first char we inserted. */
                    if (curwin.w_cursor.lnum == insStart_orig.lnum && curwin.w_cursor.col < insStart_orig.col)
                        insStart_orig.col = curwin.w_cursor.col;

                    if ((State & VREPLACE_FLAG) != 0)
                        ins_char(' ');
                    else
                    {
                        ins_str(u8(" "));
                        if ((State & REPLACE_FLAG) != 0)
                            replace_push(NUL);
                    }
                    getvcol(curwin, curwin.w_cursor, vcol, null, null);
                }

                /* If we are now back where we started delete one character.
                 * Can happen when using 'sts' and 'linebreak'. */
                if (start_vcol <= vcol[0])
                    ins_bs_one(vcol);
            }
            /*
             * Delete upto starting point, start of line or previous word.
             */
            else
            {
                int prev_cclass = 0;
                int cclass = us_get_class(ml_get_cursor(), curbuf);

                boolean temp = false;
                do
                {
                    if (!revins_on) /* put cursor on char to be deleted */
                        dec_cursor();

                    int cc = gchar_cursor();
                    /* look multi-byte character class */
                    prev_cclass = cclass;
                    cclass = us_get_class(ml_get_cursor(), curbuf);

                    /* start of word? */
                    if (mode == BACKSPACE_WORD && !vim_isspace(cc))
                    {
                        mode = BACKSPACE_WORD_NOT_SPACE;
                        temp = vim_iswordc(cc, curbuf);
                    }
                    /* end of word? */
                    else if (mode == BACKSPACE_WORD_NOT_SPACE
                            && ((vim_isspace(cc) || vim_iswordc(cc, curbuf) != temp) || prev_cclass != cclass))
                    {
                        if (!revins_on)
                            inc_cursor();
                        else if ((State & REPLACE_FLAG) != 0)
                            dec_cursor();
                        break;
                    }
                    if ((State & REPLACE_FLAG) != 0)
                        replace_do_bs(-1);
                    else
                    {
                        int[] cpc = new int[MAX_MCO];   /* composing characters */
                        if (p_deco[0])
                            us_ptr2char_cc(ml_get_cursor(), cpc);
                        del_char(false);
                        /*
                         * If there are combining characters and 'delcombine' is set
                         * move the cursor back.  Don't back up before the base character.
                         */
                        if (p_deco[0] && cpc[0] != NUL)
                            inc_cursor();
                        if (0 < revins_chars)
                        {
                            revins_chars--;
                            revins_legal++;
                        }
                        if (revins_on && gchar_cursor() == NUL)
                            break;
                    }
                    /* Just a single backspace?: */
                    if (mode == BACKSPACE_CHAR)
                        break;
                } while (revins_on ||
                        (mincol < curwin.w_cursor.col
                        && (curwin.w_cursor.lnum != insStart_orig.lnum
                            || curwin.w_cursor.col != insStart_orig.col)));
            }
            did_backspace = true;
        }

        did_si = false;
        can_si = false;
        can_si_back = false;
        if (curwin.w_cursor.col <= 1)
            did_ai = false;
        /*
         * It's a little strange to put backspaces into the redo buffer,
         * but it makes auto-indent a lot easier to deal with.
         */
        appendCharToRedobuff(c);

        /* If deleted before the insertion point, adjust it. */
        if (curwin.w_cursor.lnum == insStart_orig.lnum && curwin.w_cursor.col < insStart_orig.col)
            insStart_orig.col = curwin.w_cursor.col;

        /* vi: the cursor moves backward but the character that was there remains visible
         * vim: the cursor moves backward and the character that was there is erased from the screen
         * We can emulate vi by pretending there is a dollar displayed even when there isn't.
         */
        if (vim_strbyte(p_cpo[0], CPO_BACKSPACE) != null && dollar_vcol == -1)
            dollar_vcol = curwin.w_virtcol;

        return did_backspace;
    }

    /*private*/ static void ins_mouse(int c)
    {
        window_C old_curwin = curwin;

        if (!mouse_has(MOUSE_INSERT))
            return;

        undisplay_dollar();

        pos_C tpos = new pos_C();
        COPY_pos(tpos, curwin.w_cursor);

        if (do_mouse(null, c, BACKWARD, 1, 0))
        {
            window_C new_curwin = curwin;

            if (curwin != old_curwin && win_valid(old_curwin))
            {
                /* Mouse took us to another window.
                 * We need to go back to the previous one to stop insert there properly. */
                curwin = old_curwin;
                curbuf = curwin.w_buffer;
            }
            start_arrow(curwin == old_curwin ? tpos : null);
            if (curwin != new_curwin && win_valid(new_curwin))
            {
                curwin = new_curwin;
                curbuf = curwin.w_buffer;
            }
            can_cindent = true;
        }

        /* redraw status lines (in case another window became active) */
        redraw_statuslines();
    }

    /*private*/ static void ins_mousescroll(int dir)
    {
        window_C old_curwin = curwin;

        pos_C tpos = new pos_C();
        COPY_pos(tpos, curwin.w_cursor);

        if (0 <= mouse_row && 0 <= mouse_col)
        {
            int[] row = { mouse_row };
            int[] col = { mouse_col };

            /* find the window at the pointer coordinates */
            curwin = mouse_find_win(row, col);
            curbuf = curwin.w_buffer;
        }

        if (curwin == old_curwin)
            undisplay_dollar();

        if (dir == MSCR_DOWN || dir == MSCR_UP)
        {
            if ((mod_mask & (MOD_MASK_SHIFT | MOD_MASK_CTRL)) != 0)
                scroll_redraw(dir != MSCR_DOWN, curwin.w_botline - curwin.w_topline);
            else
                scroll_redraw(dir != MSCR_DOWN, 3L);
        }

        curwin.w_redr_status = true;

        curwin = old_curwin;
        curbuf = curwin.w_buffer;

        if (!eqpos(curwin.w_cursor, tpos))
        {
            start_arrow(tpos);
            can_cindent = true;
        }
    }

    /*private*/ static void ins_left()
    {
        undisplay_dollar();

        pos_C tpos = new pos_C();
        COPY_pos(tpos, curwin.w_cursor);

        if (oneleft() == true)
        {
            start_arrow(tpos);
            /* If exit reversed string, position is fixed. */
            if (revins_scol != -1 && revins_scol <= curwin.w_cursor.col)
                revins_legal++;
            revins_chars++;
        }
        /*
         * if 'whichwrap' set for cursor in insert mode may go to previous line
         */
        else if (vim_strchr(p_ww[0], '[') != null && 1 < curwin.w_cursor.lnum)
        {
            start_arrow(tpos);
            --curwin.w_cursor.lnum;
            coladvance(MAXCOL);
            curwin.w_set_curswant = true;   /* so we stay at the end */
        }
        else
            vim_beep();
    }

    /*private*/ static void ins_home(int c)
    {
        undisplay_dollar();

        pos_C tpos = new pos_C();
        COPY_pos(tpos, curwin.w_cursor);

        if (c == K_C_HOME)
            curwin.w_cursor.lnum = 1;
        curwin.w_cursor.col = 0;
        curwin.w_cursor.coladd = 0;
        curwin.w_curswant = 0;

        start_arrow(tpos);
    }

    /*private*/ static void ins_end(int c)
    {
        undisplay_dollar();

        pos_C tpos = new pos_C();
        COPY_pos(tpos, curwin.w_cursor);

        if (c == K_C_END)
            curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
        coladvance(MAXCOL);
        curwin.w_curswant = MAXCOL;

        start_arrow(tpos);
    }

    /*private*/ static void ins_s_left()
    {
        undisplay_dollar();

        if (1 < curwin.w_cursor.lnum || 0 < curwin.w_cursor.col)
        {
            start_arrow(curwin.w_cursor);
            bck_word(1L, false, false);
            curwin.w_set_curswant = true;
        }
        else
            vim_beep();
    }

    /*private*/ static void ins_right()
    {
        undisplay_dollar();

        if (gchar_cursor() != NUL || virtual_active())
        {
            start_arrow(curwin.w_cursor);
            curwin.w_set_curswant = true;
            if (virtual_active())
                oneright();
            else
                curwin.w_cursor.col += us_ptr2len_cc(ml_get_cursor());

            revins_legal++;
            if (0 < revins_chars)
                revins_chars--;
        }
        /* if 'whichwrap' set for cursor in insert mode, may move the cursor to the next line */
        else if (vim_strchr(p_ww[0], ']') != null && curwin.w_cursor.lnum < curbuf.b_ml.ml_line_count)
        {
            start_arrow(curwin.w_cursor);
            curwin.w_set_curswant = true;
            curwin.w_cursor.lnum++;
            curwin.w_cursor.col = 0;
        }
        else
            vim_beep();
    }

    /*private*/ static void ins_s_right()
    {
        undisplay_dollar();

        if (curwin.w_cursor.lnum < curbuf.b_ml.ml_line_count || gchar_cursor() != NUL)
        {
            start_arrow(curwin.w_cursor);
            fwd_word(1L, false, false);
            curwin.w_set_curswant = true;
        }
        else
            vim_beep();
    }

    /*private*/ static void ins_up(boolean startcol)
        /* startcol: when true move to insStart.col */
    {
        long old_topline = curwin.w_topline;

        undisplay_dollar();

        pos_C tpos = new pos_C();
        COPY_pos(tpos, curwin.w_cursor);

        if (cursor_up(1L, true) == true)
        {
            if (startcol)
                coladvance(getvcol_nolist(insStart));
            if (old_topline != curwin.w_topline)
                redraw_later(VALID);
            start_arrow(tpos);
            can_cindent = true;
        }
        else
            vim_beep();
    }

    /*private*/ static void ins_pageup()
    {
        undisplay_dollar();

        if ((mod_mask & MOD_MASK_CTRL) != 0)
        {
            /* <C-PageUp>: tab page back */
            if (first_tabpage.tp_next != null)
            {
                start_arrow(curwin.w_cursor);
                goto_tabpage(-1);
            }
            return;
        }

        pos_C tpos = new pos_C();
        COPY_pos(tpos, curwin.w_cursor);

        if (onepage(BACKWARD, 1L) == true)
        {
            start_arrow(tpos);
            can_cindent = true;
        }
        else
            vim_beep();
    }

    /*private*/ static void ins_down(boolean startcol)
        /* startcol: when true move to insStart.col */
    {
        long old_topline = curwin.w_topline;

        undisplay_dollar();

        pos_C tpos = new pos_C();
        COPY_pos(tpos, curwin.w_cursor);

        if (cursor_down(1L, true) == true)
        {
            if (startcol)
                coladvance(getvcol_nolist(insStart));
            if (old_topline != curwin.w_topline)
                redraw_later(VALID);
            start_arrow(tpos);
            can_cindent = true;
        }
        else
            vim_beep();
    }

    /*private*/ static void ins_pagedown()
    {
        undisplay_dollar();

        if ((mod_mask & MOD_MASK_CTRL) != 0)
        {
            /* <C-PageDown>: tab page forward */
            if (first_tabpage.tp_next != null)
            {
                start_arrow(curwin.w_cursor);
                goto_tabpage(0);
            }
            return;
        }

        pos_C tpos = new pos_C();
        COPY_pos(tpos, curwin.w_cursor);

        if (onepage(FORWARD, 1L) == true)
        {
            start_arrow(tpos);
            can_cindent = true;
        }
        else
            vim_beep();
    }

    /*private*/ static void ins_drop()
    {
        do_put('~', BACKWARD, 1, PUT_CURSEND);
    }

    /*
     * Handle TAB in Insert or Replace mode.
     * Return true when the TAB needs to be inserted like a normal character.
     */
    /*private*/ static boolean ins_tab()
    {
        if (insStart_blank_vcol == MAXCOL && curwin.w_cursor.lnum == insStart.lnum)
            insStart_blank_vcol = get_nolist_virtcol();
        if (echeck_abbr(TAB + ABBR_OFF))
            return false;

        boolean ind = inindent(0);
        if (ind)
            can_cindent = false;

        /*
         * When nothing special, insert TAB like a normal character
         */
        if (!curbuf.b_p_et[0]
                && !(p_sta[0] && ind && curbuf.b_p_ts[0] != get_sw_value(curbuf))
                && get_sts_value() == 0)
            return true;

        if (!stop_arrow())
            return true;

        did_ai = false;
        did_si = false;
        can_si = false;
        can_si_back = false;

        appendToRedobuff(u8("\t"));

        int temp;
        if (p_sta[0] && ind)                       /* insert tab in indent, use 'shiftwidth' */
            temp = (int)get_sw_value(curbuf);
        else if (curbuf.b_p_sts[0] != 0)           /* use 'softtabstop' when set */
            temp = (int)get_sts_value();
        else                                    /* otherwise use 'tabstop' */
            temp = (int)curbuf.b_p_ts[0];
        temp -= get_nolist_virtcol() % temp;

        /*
         * Insert the first space with ins_char().  It will delete one char in
         * replace mode.  Insert the rest with ins_str(); it will not delete any
         * chars.  For VREPLACE mode, we use ins_char() for all characters.
         */
        ins_char(' ');
        while (0 < --temp)
        {
            if ((State & VREPLACE_FLAG) != 0)
                ins_char(' ');
            else
            {
                ins_str(u8(" "));
                if ((State & REPLACE_FLAG) != 0)    /* no char replaced */
                    replace_push(NUL);
            }
        }

        /*
         * When 'expandtab' not set: Replace spaces by TABs where possible.
         */
        if (!curbuf.b_p_et[0] && (get_sts_value() != 0 || (p_sta[0] && ind)))
        {
            boolean save_list = curwin.w_onebuf_opt.wo_list[0];

            /*
             * Get the current line.
             * For VREPLACE mode, don't make real changes yet, just work on a copy of the line.
             */
            pos_C pos = new pos_C();
            pos_C cursor;
            Bytes saved_line = null;
            Bytes ptr;
            if ((State & VREPLACE_FLAG) != 0)
            {
                COPY_pos(pos, curwin.w_cursor);
                cursor = pos;
                saved_line = STRDUP(ml_get_curline());
                ptr = saved_line.plus(pos.col);
            }
            else
            {
                ptr = ml_get_cursor();
                cursor = curwin.w_cursor;
            }

            /* When 'L' is not in 'cpoptions' a tab always takes up 'ts' spaces. */
            if (vim_strbyte(p_cpo[0], CPO_LISTWM) == null)
                curwin.w_onebuf_opt.wo_list[0] = false;

            /* Find first white before the cursor. */
            pos_C fpos = new pos_C();
            COPY_pos(fpos, curwin.w_cursor);
            while (0 < fpos.col && vim_iswhite(ptr.at(-1)))
            {
                --fpos.col;
                ptr = ptr.minus(1);
            }

            /* In Replace mode, don't change characters before the insert point. */
            if ((State & REPLACE_FLAG) != 0 && fpos.lnum == insStart.lnum && fpos.col < insStart.col)
            {
                ptr = ptr.plus(insStart.col - fpos.col);
                fpos.col = insStart.col;
            }

            /* compute virtual column numbers of first white and cursor */
            int[] vcol = new int[1];
            getvcol(curwin, fpos, vcol, null, null);
            int[] want_vcol = new int[1];
            getvcol(curwin, cursor, want_vcol, null, null);

            int change_col = -1;
            /* Use as many TABs as possible.
             * Beware of 'breakindent', 'showbreak' and 'linebreak' adding extra virtual columns. */
            while (vim_iswhite(ptr.at(0)))
            {
                int i = lbr_chartabsize(null, u8("\t"), vcol[0]);
                if (want_vcol[0] < vcol[0] + i)
                    break;
                if (ptr.at(0) != TAB)
                {
                    ptr.be(0, TAB);
                    if (change_col < 0)
                    {
                        change_col = fpos.col; /* column of first change */
                        /* May have to adjust insStart. */
                        if (fpos.lnum == insStart.lnum && fpos.col < insStart.col)
                            insStart.col = fpos.col;
                    }
                }
                fpos.col++;
                ptr = ptr.plus(1);
                vcol[0] += i;
            }

            if (0 <= change_col)
            {
                int repl_off = 0;
                Bytes line = ptr;

                /* Skip over the spaces we need. */
                while (vcol[0] < want_vcol[0] && ptr.at(0) == (byte)' ')
                {
                    vcol[0] += lbr_chartabsize(line, ptr, vcol[0]);
                    ptr = ptr.plus(1);
                    repl_off++;
                }
                if (want_vcol[0] < vcol[0])
                {
                    /* Must have a char with 'showbreak' just before it. */
                    ptr = ptr.minus(1);
                    --repl_off;
                }
                fpos.col += repl_off;

                /* Delete following spaces. */
                int i = cursor.col - fpos.col;
                if (0 < i)
                {
                    BCOPY(ptr, 0, ptr, i, strlen(ptr, i) + 1);
                    /* correct replace stack. */
                    if ((State & REPLACE_FLAG) != 0 && (State & VREPLACE_FLAG) == 0)
                        for (temp = i; 0 <= --temp; )
                            replace_join(repl_off);
                }
                cursor.col -= i;

                /*
                 * In VREPLACE mode, we haven't changed anything yet.  Do it now by
                 * backspacing over the changed spacing and then inserting the new spacing.
                 */
                if ((State & VREPLACE_FLAG) != 0)
                {
                    /* Backspace from real cursor to change_col. */
                    backspace_until_column(change_col);

                    /* Insert each char in saved_line from changed_col to ptr-cursor. */
                    ins_bytes_len(saved_line.plus(change_col), cursor.col - change_col);
                }
            }

            curwin.w_onebuf_opt.wo_list[0] = save_list;
        }

        return false;
    }

    /*
     * Handle CR or NL in insert mode.
     * Return true when out of memory or can't undo.
     */
    /*private*/ static boolean ins_eol(int c)
    {
        if (echeck_abbr(c + ABBR_OFF))
            return false;
        if (!stop_arrow())
            return true;

        undisplay_dollar();

        /*
         * Strange Vi behaviour:
         * In Replace mode, typing a NL will not delete the character under the cursor.
         * Only push a NUL on the replace stack, nothing to put back when the NL is deleted.
         */
        if ((State & REPLACE_FLAG) != 0 && (State & VREPLACE_FLAG) == 0)
            replace_push(NUL);

        /*
         * In VREPLACE mode, a NL replaces the rest of the line, and starts replacing the next line,
         * so we push all of the characters left on the line onto the replace stack.
         * This is not done here though, it is done in open_line().
         */

        /* Put cursor on NUL if on the last char and coladd is 1 (happens after CTRL-O). */
        if (virtual_active() && 0 < curwin.w_cursor.coladd)
            coladvance(getviscol());

        /* NL in reverse insert will always start in the end of current line. */
        if (revins_on)
            curwin.w_cursor.col += strlen(ml_get_cursor());

        appendToRedobuff(NL_STR);
        boolean b = open_line(FORWARD, has_format_option(FO_RET_COMS) ? OPENLINE_DO_COM : 0, old_indent);
        old_indent = 0;
        can_cindent = true;

        return (!b);
    }

    /*
     * Handle digraph in insert mode.
     * Returns character still to be inserted, or NUL when nothing remaining to be done.
     */
    /*private*/ static int ins_digraph()
    {
        boolean did_putchar = false;

        pc_status = PC_STATUS_UNSET;
        if (redrawing() && !char_avail())
        {
            /* May need to redraw when no more chars available now. */
            ins_redraw(false);

            edit_putchar('?', true);
            did_putchar = true;
            add_to_showcmd_c(Ctrl_K);
        }

        /* Don't map the digraph chars.
         * This also prevents the mode message to be deleted when ESC is hit. */
        no_mapping++;
        allow_keys++;
        int c = plain_vgetc();
        --no_mapping;
        --allow_keys;
        if (did_putchar)
            /* When the line fits in 'columns',
             * the '?' is at the start of the next line and will not be removed by the redraw. */
            edit_unputchar();

        if (is_special(c) || mod_mask != 0)         /* special key */
        {
            clear_showcmd();
            insert_special(c, true, false);
            return NUL;
        }

        if (c != ESC)
        {
            did_putchar = false;
            if (redrawing() && !char_avail())
            {
                /* May need to redraw when no more chars available now. */
                ins_redraw(false);

                if (mb_char2cells(c) == 1)
                {
                    ins_redraw(false);
                    edit_putchar(c, true);
                    did_putchar = true;
                }
                add_to_showcmd_c(c);
            }
            no_mapping++;
            allow_keys++;
            int cc = plain_vgetc();
            --no_mapping;
            --allow_keys;
            if (did_putchar)
                /* When the line fits in 'columns',
                 * the '?' is at the start of the next line and will not be removed by a redraw. */
                edit_unputchar();
            if (cc != ESC)
            {
                appendToRedobuff(CTRL_V_STR);
                c = getdigraph(c, cc, true);
                clear_showcmd();
                return c;
            }
        }
        clear_showcmd();
        return NUL;
    }

    /*
     * Handle CTRL-E and CTRL-Y in Insert mode: copy char from other line.
     * Returns the char to be inserted, or NUL if none found.
     */
    /*private*/ static int ins_copychar(long lnum)
    {
        if (lnum < 1 || curbuf.b_ml.ml_line_count < lnum)
        {
            vim_beep();
            return NUL;
        }

        /* try to advance to the cursor column */
        Bytes line = ml_get(lnum);
        Bytes[] ptr = { line };
        Bytes prev_ptr = ptr[0];
        validate_virtcol();

        int temp = 0;
        while (temp < curwin.w_virtcol && ptr[0].at(0) != NUL)
        {
            prev_ptr = ptr[0];
            temp += lbr_chartabsize_adv(line, ptr, temp);
        }
        if (curwin.w_virtcol < temp)
            ptr[0] = prev_ptr;

        int c = us_ptr2char(ptr[0]);
        if (c == NUL)
            vim_beep();
        return c;
    }

    /*
     * CTRL-Y or CTRL-E typed in Insert mode.
     */
    /*private*/ static int ins_ctrl_ey(int tc)
    {
        int c = ins_copychar(curwin.w_cursor.lnum + (tc == Ctrl_Y ? -1 : 1));
        if (c != NUL)
        {
            /* The character must be taken literally, insert like it was typed after a CTRL-V,
             * and pretend 'textwidth' wasn't set.  Digits, 'o' and 'x' are special after a
             * CTRL-V, don't use it for these. */
            if (c < 256 && !asc_isalnum(c))
                appendToRedobuff(CTRL_V_STR); /* CTRL-V */

            long tw_save = curbuf.b_p_tw[0];
            curbuf.b_p_tw[0] = -1;
            insert_special(c, true, false);
            curbuf.b_p_tw[0] = tw_save;

            revins_chars++;
            revins_legal++;
            c = Ctrl_V;                     /* pretend CTRL-V is last character */
            auto_format(false, true);
        }

        return c;
    }

    /*
     * Try to do some very smart auto-indenting.
     * Used when inserting a "normal" character.
     */
    /*private*/ static void ins_try_si(int c)
    {
        /*
         * do some very smart indenting when entering '{' or '}'
         */
        if (((did_si || can_si_back) && c == '{') || (can_si && c == '}'))
        {
            pos_C pos;

            /*
             * for '}' set indent equal to indent of line containing matching '{'
             */
            if (c == '}' && (pos = findmatch(null, '{')) != null)
            {
                pos_C old_pos = new pos_C();
                COPY_pos(old_pos, curwin.w_cursor);
                /*
                 * If the matching '{' has a ')' immediately before it (ignoring
                 * white-space), then line up with the start of the line containing
                 * the matching '(' if there is one.  This handles the case where
                 * an "if (..\n..) {" statement continues over multiple lines.
                 */
                Bytes ptr = ml_get(pos.lnum);
                int i = pos.col;
                if (0 < i)          /* skip blanks before '{' */
                    while (0 < --i && vim_iswhite(ptr.at(i)))
                        ;
                curwin.w_cursor.lnum = pos.lnum;
                curwin.w_cursor.col = i;
                if (ptr.at(i) == (byte)')' && (pos = findmatch(null, '(')) != null)
                    COPY_pos(curwin.w_cursor, pos);
                i = get_indent();
                COPY_pos(curwin.w_cursor, old_pos);
                if ((State & VREPLACE_FLAG) != 0)
                    change_indent(INDENT_SET, i, false, NUL, true);
                else
                    set_indent(i, SIN_CHANGED);
            }
            else if (0 < curwin.w_cursor.col)
            {
                /*
                 * when inserting '{' after "O" reduce indent,
                 * but not more than indent of previous line
                 */
                boolean temp = true;
                if (c == '{' && can_si_back && 1 < curwin.w_cursor.lnum)
                {
                    pos_C old_pos = new pos_C();
                    COPY_pos(old_pos, curwin.w_cursor);
                    int i = get_indent();
                    while (1 < curwin.w_cursor.lnum)
                    {
                        Bytes ptr = skipwhite(ml_get(--curwin.w_cursor.lnum));

                        /* ignore empty lines and lines starting with '#'. */
                        if (ptr.at(0) != (byte)'#' && ptr.at(0) != NUL)
                            break;
                    }
                    if (i <= get_indent())
                        temp = false;
                    COPY_pos(curwin.w_cursor, old_pos);
                }
                if (temp)
                    shift_line(true, false, 1, true);
            }
        }

        /*
         * set indent of '#' always to 0
         */
        if (0 < curwin.w_cursor.col && can_si && c == '#')
        {
            /* remember current indent for next line */
            old_indent = get_indent();
            set_indent(0, SIN_CHANGED);
        }

        /* Adjust ai_col, the char at this position can be deleted. */
        if (ai_col > curwin.w_cursor.col)
            ai_col = curwin.w_cursor.col;
    }

    /*
     * Get the value that w_virtcol would have when 'list' is off.
     * Unless 'cpo' contains the 'L' flag.
     */
    /*private*/ static int get_nolist_virtcol()
    {
        if (curwin.w_onebuf_opt.wo_list[0] && vim_strbyte(p_cpo[0], CPO_LISTWM) == null)
            return getvcol_nolist(curwin.w_cursor);
        validate_virtcol();
        return curwin.w_virtcol;
    }

    /*
     * Handle the InsertCharPre autocommand.
     * "c" is the character that was typed.
     * Return a pointer to allocated memory with the replacement string.
     * Return null to continue inserting "c".
     */
    /*private*/ static Bytes do_insert_char_pre(int c)
    {
        /* Return quickly when there is nothing to do. */
        if (!has_insertcharpre())
            return null;

        Bytes buf = new Bytes(MB_MAXBYTES + 1);

        buf.be(utf_char2bytes(c, buf), NUL);

        /* Lock the text to avoid weird things from happening. */
        textlock++;
        set_vim_var_string(VV_CHAR, buf, -1);   /* set v:char */

        Bytes res = null;

        if (apply_autocmds(EVENT_INSERTCHARPRE, null, null, false, curbuf))
        {
            /* Get the value of v:char.  It may be empty or more than one
             * character.  Only use it when changed, otherwise continue with
             * the original character to avoid breaking autoindent. */
            Bytes s = get_vim_var_str(VV_CHAR);
            if (STRCMP(buf, s) != 0)
                res = STRDUP(s);
        }

        set_vim_var_string(VV_CHAR, null, -1);  /* clear v:char */
        --textlock;

        return res;
    }
}
