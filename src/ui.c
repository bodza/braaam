/*
 * ui.c: functions that handle the user interface.
 * 1. Keyboard input stuff, and a bit of windowing stuff.  These are called
 *    before the machine specific stuff (mch_*) so that we can call the GUI
 *    stuff instead if the GUI is running.
 * 2. Clipboard stuff.
 * 3. Input buffer stuff.
 */

#include "vim.h"

    void
ui_write(s, len)
    char_u  *s;
    int     len;
{
    /* Don't output anything in silent mode ("ex -s") unless 'verbose' set */
    if (!(silent_mode && p_verbose == 0))
    {
        mch_write(s, len);
    }
}

/*
 * When executing an external program, there may be some typed characters that
 * are not consumed by it.  Give them back to ui_inchar() and they are stored
 * here for the next call.
 */
static char_u *ta_str = NULL;
static int ta_off;      /* offset for next char to use when ta_str != NULL */
static int ta_len;      /* length of ta_str when it's not NULL */

    void
ui_inchar_undo(s, len)
    char_u      *s;
    int         len;
{
    char_u  *new;
    int     newlen;

    newlen = len;
    if (ta_str != NULL)
        newlen += ta_len - ta_off;
    new = alloc(newlen);
    if (new != NULL)
    {
        if (ta_str != NULL)
        {
            mch_memmove(new, ta_str + ta_off, (size_t)(ta_len - ta_off));
            mch_memmove(new + ta_len - ta_off, s, (size_t)len);
            vim_free(ta_str);
        }
        else
            mch_memmove(new, s, (size_t)len);
        ta_str = new;
        ta_len = newlen;
        ta_off = 0;
    }
}

/*
 * ui_inchar(): low level input function.
 * Get characters from the keyboard.
 * Return the number of characters that are available.
 * If "wtime" == 0 do not wait for characters.
 * If "wtime" == -1 wait forever for characters.
 * If "wtime" > 0 wait "wtime" milliseconds for a character.
 *
 * "tb_change_cnt" is the value of typebuf.tb_change_cnt if "buf" points into it.
 * When typebuf.tb_change_cnt changes (e.g., when a message is received from a remote client)
 * "buf" can no longer be used.  "tb_change_cnt" is NULL otherwise.
 */
    int
ui_inchar(buf, maxlen, wtime, tb_change_cnt)
    char_u      *buf;
    int         maxlen;
    long        wtime;      /* don't use "time", MIPS cannot handle it */
    int         tb_change_cnt;
{
    int         retval = 0;

    /* If we are going to wait for some time or block... */
    if (wtime == -1 || wtime > 100L)
    {
        /* ... allow signals to kill us. */
        (void)vim_handle_signal(SIGNAL_UNBLOCK);

        /* ... there is no need for CTRL-C to interrupt something, don't let
         * it set got_int when it was mapped. */
        if ((mapped_ctrl_c | curbuf->b_mapped_ctrl_c) & get_real_state())
            ctrl_c_interrupts = FALSE;
    }

    retval = mch_inchar(buf, maxlen, wtime, tb_change_cnt);

    if (wtime == -1 || wtime > 100L)
        /* block SIGHUP et al. */
        (void)vim_handle_signal(SIGNAL_BLOCK);

    ctrl_c_interrupts = TRUE;

    return retval;
}

/*
 * return non-zero if a character is available
 */
    int
ui_char_avail()
{
    return mch_char_avail();
}

/*
 * Delay for the given number of milliseconds.  If ignoreinput is FALSE then we
 * cancel the delay if a key is hit.
 */
    void
ui_delay(msec, ignoreinput)
    long        msec;
    int         ignoreinput;
{
    mch_delay(msec, ignoreinput);
}

/*
 * If the machine has job control, use it to suspend the program,
 * otherwise fake it by starting a new shell.
 * When running the GUI iconify the window.
 */
    void
ui_suspend()
{
    mch_suspend();
}

#if !defined(SIGTSTP)
/*
 * When the OS can't really suspend, call this function to start a shell.
 * This is never called in the GUI.
 */
    void
suspend_shell()
{
    if (*p_sh == NUL)
        EMSG((char *)e_shellempty);
    else
    {
        MSG_PUTS("new shell started\n");
        do_shell(NULL, 0);
    }
}
#endif

/*
 * Try to get the current Vim shell size.  Put the result in Rows and Columns.
 * Use the new sizes as defaults for 'columns' and 'lines'.
 * Return OK when size could be determined, FAIL otherwise.
 */
    int
ui_get_shellsize()
{
    int     retval = mch_get_shellsize();

    check_shellsize();

    /* adjust the default for 'lines' and 'columns' */
    if (retval == OK)
    {
        set_number_default("lines", Rows);
        set_number_default("columns", Columns);
    }
    return retval;
}

/*
 * Set the size of the Vim shell according to Rows and Columns, if possible.
 * The gui_set_shellsize() or mch_set_shellsize() function will try to set the
 * new size.  If this is not possible, it will adjust Rows and Columns.
 */
    void
ui_set_shellsize(mustset)
    int         mustset UNUSED; /* set by the user */
{
    mch_set_shellsize();
}

/*
 * Called when Rows and/or Columns changed.  Adjust scroll region and mouse region.
 */
    void
ui_new_shellsize()
{
    if (full_screen && !exiting)
    {
        mch_new_shellsize();
    }
}

    void
ui_breakcheck()
{
    mch_breakcheck();
}

/*****************************************************************************
 * Functions for copying and pasting text between applications.
 * This is always included in a GUI version, but may also be included when the
 * clipboard and mouse is available to a terminal version such as xterm.
 * Note: there are some more functions in ops.c that handle selection stuff.
 *
 * Also note that the majority of functions here deal with the X 'primary'
 * (visible - for Visual mode use) selection, and only that. There are no
 * versions of these for the 'clipboard' selection, as Visual mode has no use for them.
 */

static void clip_copy_selection(clipboard_T *clip);

/*
 * Selection stuff using Visual mode, for cutting and pasting text to other windows.
 */

/*
 * Call this to initialise the clipboard.  Pass it FALSE if the clipboard code
 * is included, but the clipboard can not be used, or TRUE if the clipboard can be used.
 * Eg unix may call this with FALSE, then call it again with TRUE if the GUI starts.
 */
    void
clip_init(can_use)
    int     can_use;
{
    clipboard_T *cb;

    cb = &clip_star;
    for (;;)
    {
        cb->available  = can_use;
        cb->owned      = FALSE;
        cb->start.lnum = 0;
        cb->start.col  = 0;
        cb->end.lnum   = 0;
        cb->end.col    = 0;
        cb->state      = SELECT_CLEARED;

        if (cb == &clip_plus)
            break;
        cb = &clip_plus;
    }
}

/*
 * Check whether the VIsual area has changed, and if so try to become the owner
 * of the selection, and free any old converted selection we may still have
 * lying around.  If the VIsual mode has ended, make a copy of what was
 * selected so we can still give it to others.  Will probably have to make sure
 * this is called whenever VIsual mode is ended.
 */
    void
clip_update_selection(clip)
    clipboard_T     *clip;
{
    pos_T           start, end;

    /* If visual mode is only due to a redo command ("."), then ignore it */
    if (!redo_VIsual_busy && VIsual_active && (State & NORMAL))
    {
        if (lt(VIsual, curwin->w_cursor))
        {
            start = VIsual;
            end = curwin->w_cursor;
            end.col += utfc_ptr2len(ml_get_cursor()) - 1;
        }
        else
        {
            start = curwin->w_cursor;
            end = VIsual;
        }
        if (!equalpos(clip->start, start) || !equalpos(clip->end, end) || clip->vmode != VIsual_mode)
        {
            clip_clear_selection(clip);
            clip->start = start;
            clip->end = end;
            clip->vmode = VIsual_mode;
            clip_free_selection(clip);
            clip_own_selection(clip);
            clip_gen_set_selection(clip);
        }
    }
}

    void
clip_own_selection(clipboard_T *cbd)
{
    /*
     * Also want to check somehow that we are reading from the keyboard rather than a mapping etc.
     */
    /* Only own the clipboard when we didn't own it yet. */
    if (!cbd->owned && cbd->available)
        cbd->owned = (clip_gen_own_selection(cbd) == OK);
}

    void
clip_lose_selection(clipboard_T *cbd)
{
    int     visual_selection = FALSE;

    if (cbd == &clip_star || cbd == &clip_plus)
        visual_selection = TRUE;

    clip_free_selection(cbd);
    cbd->owned = FALSE;
    if (visual_selection)
        clip_clear_selection(cbd);
    clip_gen_lose_selection(cbd);
}

    static void
clip_copy_selection(clip)
    clipboard_T         *clip;
{
    if (VIsual_active && (State & NORMAL) && clip->available)
    {
        clip_update_selection(clip);
        clip_free_selection(clip);
        clip_own_selection(clip);
        if (clip->owned)
            clip_get_selection(clip);
        clip_gen_set_selection(clip);
    }
}

/*
 * Save and restore clip_unnamed before doing possibly many changes. This
 * prevents accessing the clipboard very often which might slow down Vim considerably.
 */
static int global_change_count = 0; /* if set, inside a start_global_changes */
static int clipboard_needs_update; /* clipboard needs to be updated */

/*
 * Save clip_unnamed and reset it.
 */
    void
start_global_changes()
{
    if (++global_change_count > 1)
        return;
    clip_unnamed_saved = clip_unnamed;
    clipboard_needs_update = FALSE;

    if (clip_did_set_selection)
    {
        clip_unnamed = FALSE;
        clip_did_set_selection = FALSE;
    }
}

/*
 * Restore clip_unnamed and set the selection when needed.
 */
    void
end_global_changes()
{
    if (--global_change_count > 0)
        /* recursive */
        return;
    if (!clip_did_set_selection)
    {
        clip_did_set_selection = TRUE;
        clip_unnamed = clip_unnamed_saved;
        clip_unnamed_saved = FALSE;
        if (clipboard_needs_update)
        {
            /* only store something in the clipboard,
             * if we have yanked anything to it */
            if (clip_unnamed & CLIP_UNNAMED)
            {
                clip_own_selection(&clip_star);
                clip_gen_set_selection(&clip_star);
            }
            if (clip_unnamed & CLIP_UNNAMED_PLUS)
            {
                clip_own_selection(&clip_plus);
                clip_gen_set_selection(&clip_plus);
            }
        }
    }
}

/*
 * Called when Visual mode is ended: update the selection.
 */
    void
clip_auto_select()
{
    if (clip_isautosel_star())
        clip_copy_selection(&clip_star);
    if (clip_isautosel_plus())
        clip_copy_selection(&clip_plus);
}

/*
 * Return TRUE if automatic selection of Visual area is desired for the * register.
 */
    int
clip_isautosel_star()
{
    return (clip_autoselect_star);
}

/*
 * Return TRUE if automatic selection of Visual area is desired for the + register.
 */
    int
clip_isautosel_plus()
{
    return (clip_autoselect_plus);
}

/*
 * Stuff for general mouse selection, without using Visual mode.
 */

static int clip_compare_pos(int row1, int col1, int row2, int col2);
static void clip_invert_area(int, int, int, int, int how);
static void clip_invert_rectangle(int row, int col, int height, int width, int invert);
static void clip_get_word_boundaries(clipboard_T *, int, int);
static int  clip_get_line_end(int);
static void clip_update_modeless_selection(clipboard_T *, int, int, int, int);

/* flags for clip_invert_area() */
#define CLIP_CLEAR      1
#define CLIP_SET        2
#define CLIP_TOGGLE     3

/*
 * Start, continue or end a modeless selection.  Used when editing the
 * command-line and in the cmdline window.
 */
    void
clip_modeless(button, is_click, is_drag)
    int         button;
    int         is_click;
    int         is_drag;
{
    int         repeat;

    repeat = ((clip_star.mode == SELECT_MODE_CHAR || clip_star.mode == SELECT_MODE_LINE)
                                              && (mod_mask & MOD_MASK_2CLICK))
            || (clip_star.mode == SELECT_MODE_WORD
                                             && (mod_mask & MOD_MASK_3CLICK));
    if (is_click && button == MOUSE_RIGHT)
    {
        /* Right mouse button: If there was no selection, start one.
         * Otherwise extend the existing selection. */
        if (clip_star.state == SELECT_CLEARED)
            clip_start_selection(mouse_col, mouse_row, FALSE);
        clip_process_selection(button, mouse_col, mouse_row, repeat);
    }
    else if (is_click)
        clip_start_selection(mouse_col, mouse_row, repeat);
    else if (is_drag)
    {
        /* Don't try extending a selection if there isn't one.  Happens when
         * button-down is in the cmdline and them moving mouse upwards. */
        if (clip_star.state != SELECT_CLEARED)
            clip_process_selection(button, mouse_col, mouse_row, repeat);
    }
    else /* release */
        clip_process_selection(MOUSE_RELEASE, mouse_col, mouse_row, FALSE);
}

/*
 * Compare two screen positions ala strcmp()
 */
    static int
clip_compare_pos(row1, col1, row2, col2)
    int         row1;
    int         col1;
    int         row2;
    int         col2;
{
    if (row1 > row2) return(1);
    if (row1 < row2) return(-1);
    if (col1 > col2) return(1);
    if (col1 < col2) return(-1);
                     return(0);
}

/*
 * Start the selection
 */
    void
clip_start_selection(col, row, repeated_click)
    int         col;
    int         row;
    int         repeated_click;
{
    clipboard_T         *cb = &clip_star;

    if (cb->state == SELECT_DONE)
        clip_clear_selection(cb);

    row = check_row(row);
    col = check_col(col);
    col = mb_fix_col(col, row);

    cb->start.lnum  = row;
    cb->start.col   = col;
    cb->end         = cb->start;
    cb->origin_row  = (short_u)cb->start.lnum;
    cb->state       = SELECT_IN_PROGRESS;

    if (repeated_click)
    {
        if (++cb->mode > SELECT_MODE_LINE)
            cb->mode = SELECT_MODE_CHAR;
    }
    else
        cb->mode = SELECT_MODE_CHAR;

    switch (cb->mode)
    {
        case SELECT_MODE_CHAR:
            cb->origin_start_col = cb->start.col;
            cb->word_end_col = clip_get_line_end((int)cb->start.lnum);
            break;

        case SELECT_MODE_WORD:
            clip_get_word_boundaries(cb, (int)cb->start.lnum, cb->start.col);
            cb->origin_start_col = cb->word_start_col;
            cb->origin_end_col   = cb->word_end_col;

            clip_invert_area((int)cb->start.lnum, cb->word_start_col,
                             (int)cb->end.lnum, cb->word_end_col, CLIP_SET);
            cb->start.col = cb->word_start_col;
            cb->end.col   = cb->word_end_col;
            break;

        case SELECT_MODE_LINE:
            clip_invert_area((int)cb->start.lnum, 0,
                             (int)cb->start.lnum, (int)Columns, CLIP_SET);
            cb->start.col = 0;
            cb->end.col   = Columns;
            break;
    }

    cb->prev = cb->start;
}

/*
 * Continue processing the selection
 */
    void
clip_process_selection(button, col, row, repeated_click)
    int         button;
    int         col;
    int         row;
    int_u       repeated_click;
{
    clipboard_T         *cb = &clip_star;
    int                 diff;
    int                 slen = 1;       /* cursor shape width */

    if (button == MOUSE_RELEASE)
    {
        /* Check to make sure we have something selected */
        if (cb->start.lnum == cb->end.lnum && cb->start.col == cb->end.col)
        {
            cb->state = SELECT_CLEARED;
            return;
        }

        if (clip_isautosel_star() || (clip_autoselectml))
            clip_copy_modeless_selection(FALSE);

        cb->state = SELECT_DONE;
        return;
    }

    row = check_row(row);
    col = check_col(col);
    col = mb_fix_col(col, row);

    if (col == (int)cb->prev.col && row == cb->prev.lnum && !repeated_click)
        return;

    /*
     * When extending the selection with the right mouse button, swap the
     * start and end if the position is before half the selection
     */
    if (cb->state == SELECT_DONE && button == MOUSE_RIGHT)
    {
        /*
         * If the click is before the start, or the click is inside the
         * selection and the start is the closest side, set the origin to the
         * end of the selection.
         */
        if (clip_compare_pos(row, col, (int)cb->start.lnum, cb->start.col) < 0
                || (clip_compare_pos(row, col, (int)cb->end.lnum, cb->end.col) < 0
                    && (((cb->start.lnum == cb->end.lnum
                            && cb->end.col - col > col - cb->start.col))
                        || ((diff = (cb->end.lnum - row) - (row - cb->start.lnum)) > 0
                            || (diff == 0 && col < (int)(cb->start.col + cb->end.col) / 2)))))
        {
            cb->origin_row = (short_u)cb->end.lnum;
            cb->origin_start_col = cb->end.col - 1;
            cb->origin_end_col = cb->end.col;
        }
        else
        {
            cb->origin_row = (short_u)cb->start.lnum;
            cb->origin_start_col = cb->start.col;
            cb->origin_end_col = cb->start.col;
        }
        if (cb->mode == SELECT_MODE_WORD && !repeated_click)
            cb->mode = SELECT_MODE_CHAR;
    }

    /* set state, for when using the right mouse button */
    cb->state = SELECT_IN_PROGRESS;

    if (repeated_click && ++cb->mode > SELECT_MODE_LINE)
        cb->mode = SELECT_MODE_CHAR;

    switch (cb->mode)
    {
        case SELECT_MODE_CHAR:
            /* If we're on a different line, find where the line ends */
            if (row != cb->prev.lnum)
                cb->word_end_col = clip_get_line_end(row);

            /* See if we are before or after the origin of the selection */
            if (clip_compare_pos(row, col, cb->origin_row, cb->origin_start_col) >= 0)
            {
                if (col >= (int)cb->word_end_col)
                    clip_update_modeless_selection(cb, cb->origin_row,
                            cb->origin_start_col, row, (int)Columns);
                else
                {
                    if (mb_lefthalve(row, col))
                        slen = 2;
                    clip_update_modeless_selection(cb, cb->origin_row,
                            cb->origin_start_col, row, col + slen);
                }
            }
            else
            {
                if (mb_lefthalve(cb->origin_row, cb->origin_start_col))
                    slen = 2;
                if (col >= (int)cb->word_end_col)
                    clip_update_modeless_selection(cb, row, cb->word_end_col,
                            cb->origin_row, cb->origin_start_col + slen);
                else
                    clip_update_modeless_selection(cb, row, col,
                            cb->origin_row, cb->origin_start_col + slen);
            }
            break;

        case SELECT_MODE_WORD:
            /* If we are still within the same word, do nothing */
            if (row == cb->prev.lnum && col >= (int)cb->word_start_col
                    && col < (int)cb->word_end_col && !repeated_click)
                return;

            /* Get new word boundaries */
            clip_get_word_boundaries(cb, row, col);

            /* Handle being after the origin point of selection */
            if (clip_compare_pos(row, col, cb->origin_row, cb->origin_start_col) >= 0)
                clip_update_modeless_selection(cb, cb->origin_row,
                        cb->origin_start_col, row, cb->word_end_col);
            else
                clip_update_modeless_selection(cb, row, cb->word_start_col,
                        cb->origin_row, cb->origin_end_col);
            break;

        case SELECT_MODE_LINE:
            if (row == cb->prev.lnum && !repeated_click)
                return;

            if (clip_compare_pos(row, col, cb->origin_row, cb->origin_start_col) >= 0)
                clip_update_modeless_selection(cb, cb->origin_row, 0, row, (int)Columns);
            else
                clip_update_modeless_selection(cb, row, 0, cb->origin_row, (int)Columns);
            break;
    }

    cb->prev.lnum = row;
    cb->prev.col  = col;
}

/*
 * Called from outside to clear selected region from the display
 */
    void
clip_clear_selection(clipboard_T *cbd)
{
    if (cbd->state == SELECT_CLEARED)
        return;

    clip_invert_area((int)cbd->start.lnum, cbd->start.col,
                     (int)cbd->end.lnum, cbd->end.col, CLIP_CLEAR);
    cbd->state = SELECT_CLEARED;
}

/*
 * Clear the selection if any lines from "row1" to "row2" are inside of it.
 */
    void
clip_may_clear_selection(row1, row2)
    int row1, row2;
{
    if (clip_star.state == SELECT_DONE && row2 >= clip_star.start.lnum
                                       && row1 <= clip_star.end.lnum)
        clip_clear_selection(&clip_star);
}

/*
 * Called before the screen is scrolled up or down.  Adjusts the line numbers
 * of the selection.  Call with big number when clearing the screen.
 */
    void
clip_scroll_selection(rows)
    int     rows;               /* negative for scroll down */
{
    int     lnum;

    if (clip_star.state == SELECT_CLEARED)
        return;

    lnum = clip_star.start.lnum - rows;
    if (lnum <= 0)
        clip_star.start.lnum = 0;
    else if (lnum >= screen_Rows)       /* scrolled off of the screen */
        clip_star.state = SELECT_CLEARED;
    else
        clip_star.start.lnum = lnum;

    lnum = clip_star.end.lnum - rows;
    if (lnum < 0)                       /* scrolled off of the screen */
        clip_star.state = SELECT_CLEARED;
    else if (lnum >= screen_Rows)
        clip_star.end.lnum = screen_Rows - 1;
    else
        clip_star.end.lnum = lnum;
}

/*
 * Invert a region of the display between a starting and ending row and column
 * Values for "how":
 * CLIP_CLEAR:  undo inversion
 * CLIP_SET:    set inversion
 * CLIP_TOGGLE: set inversion if pos1 < pos2, undo inversion otherwise.
 * 0: invert (GUI only).
 */
    static void
clip_invert_area(row1, col1, row2, col2, how)
    int         row1;
    int         col1;
    int         row2;
    int         col2;
    int         how;
{
    int         invert = FALSE;

    if (how == CLIP_SET)
        invert = TRUE;

    /* Swap the from and to positions so the from is always before */
    if (clip_compare_pos(row1, col1, row2, col2) > 0)
    {
        int tmp_row, tmp_col;

        tmp_row = row1;
        tmp_col = col1;
        row1    = row2;
        col1    = col2;
        row2    = tmp_row;
        col2    = tmp_col;
    }
    else if (how == CLIP_TOGGLE)
        invert = TRUE;

    /* If all on the same line, do it the easy way */
    if (row1 == row2)
    {
        clip_invert_rectangle(row1, col1, 1, col2 - col1, invert);
    }
    else
    {
        /* Handle a piece of the first line */
        if (col1 > 0)
        {
            clip_invert_rectangle(row1, col1, 1, (int)Columns - col1, invert);
            row1++;
        }

        /* Handle a piece of the last line */
        if (col2 < Columns - 1)
        {
            clip_invert_rectangle(row2, 0, 1, col2, invert);
            row2--;
        }

        /* Handle the rectangle thats left */
        if (row2 >= row1)
            clip_invert_rectangle(row1, 0, row2 - row1 + 1, (int)Columns, invert);
    }
}

/*
 * Invert or un-invert a rectangle of the screen.
 * "invert" is true if the result is inverted.
 */
    static void
clip_invert_rectangle(row, col, height, width, invert)
    int         row;
    int         col;
    int         height;
    int         width;
    int         invert;
{
    screen_draw_rectangle(row, col, height, width, invert);
}

/*
 * Copy the currently selected area into the '*' register so it will be
 * available for pasting.
 * When "both" is TRUE also copy to the '+' register.
 */
    void
clip_copy_modeless_selection(both)
    int         both UNUSED;
{
    char_u      *buffer;
    char_u      *bufp;
    int         row;
    int         start_col;
    int         end_col;
    int         line_end_col;
    int         add_newline_flag = FALSE;
    int         len;
    char_u      *p;
    int         row1 = clip_star.start.lnum;
    int         col1 = clip_star.start.col;
    int         row2 = clip_star.end.lnum;
    int         col2 = clip_star.end.col;

    /* Can't use ScreenLines unless initialized */
    if (ScreenLines == NULL)
        return;

    /*
     * Make sure row1 <= row2, and if row1 == row2 that col1 <= col2.
     */
    if (row1 > row2)
    {
        row = row1; row1 = row2; row2 = row;
        row = col1; col1 = col2; col2 = row;
    }
    else if (row1 == row2 && col1 > col2)
    {
        row = col1; col1 = col2; col2 = row;
    }
    /* correct starting point for being on right halve of double-wide char */
    p = ScreenLines + LineOffset[row1];
    if (p[col1] == 0)
        --col1;

    /* Create a temporary buffer for storing the text */
    len = (row2 - row1 + 1) * Columns + 1;
    len *= MB_MAXBYTES;
    buffer = lalloc((long_u)len, TRUE);
    if (buffer == NULL)     /* out of memory */
        return;

    /* Process each row in the selection */
    for (bufp = buffer, row = row1; row <= row2; row++)
    {
        if (row == row1)
            start_col = col1;
        else
            start_col = 0;

        if (row == row2)
            end_col = col2;
        else
            end_col = Columns;

        line_end_col = clip_get_line_end(row);

        /* See if we need to nuke some trailing whitespace */
        if (end_col >= Columns && (row < row2 || end_col > line_end_col))
        {
            /* Get rid of trailing whitespace */
            end_col = line_end_col;
            if (end_col < start_col)
                end_col = start_col;

            /* If the last line extended to the end, add an extra newline */
            if (row == row2)
                add_newline_flag = TRUE;
        }

        /* If after the first row, we need to always add a newline */
        if (row > row1 && !LineWraps[row - 1])
            *bufp++ = NL;

        if (row < screen_Rows && end_col <= screen_Columns)
        {
            int     off;
            int     ci;

            off = LineOffset[row];
            for (int i = start_col; i < end_col; ++i)
            {
                /* The base character is either in ScreenLinesUC[] or ScreenLines[]. */
                if (ScreenLinesUC[off + i] == 0)
                    *bufp++ = ScreenLines[off + i];
                else
                {
                    bufp += utf_char2bytes(ScreenLinesUC[off + i], bufp);
                    for (ci = 0; ci < Screen_mco; ++ci)
                    {
                        /* Add a composing character. */
                        if (ScreenLinesC[ci][off + i] == 0)
                            break;
                        bufp += utf_char2bytes(ScreenLinesC[ci][off + i], bufp);
                    }
                }
                /* Skip right halve of double-wide character. */
                if (ScreenLines[off + i + 1] == 0)
                    ++i;
            }
        }
    }

    /* Add a newline at the end if the selection ended there */
    if (add_newline_flag)
        *bufp++ = NL;

    /* First cleanup any old selection and become the owner. */
    clip_free_selection(&clip_star);
    clip_own_selection(&clip_star);

    /* Yank the text into the '*' register. */
    clip_yank_selection(MCHAR, buffer, (long)(bufp - buffer), &clip_star);

    /* Make the register contents available to the outside world. */
    clip_gen_set_selection(&clip_star);

    vim_free(buffer);
}

/*
 * Find the starting and ending positions of the word at the given row and
 * column.  Only white-separated words are recognized here.
 */
#define CHAR_CLASS(c)   (c <= ' ' ? ' ' : vim_iswordc(c))

    static void
clip_get_word_boundaries(cb, row, col)
    clipboard_T *cb;
    int         row;
    int         col;
{
    int         start_class;
    int         temp_col;
    char_u      *p;

    if (row >= screen_Rows || col >= screen_Columns || ScreenLines == NULL)
        return;

    p = ScreenLines + LineOffset[row];
    /* Correct for starting in the right halve of a double-wide char */
    if (p[col] == 0)
        --col;
    start_class = CHAR_CLASS(p[col]);

    temp_col = col;
    for ( ; temp_col > 0; temp_col--)
        if (CHAR_CLASS(p[temp_col - 1]) != start_class && p[temp_col - 1] != 0)
            break;
    cb->word_start_col = temp_col;

    temp_col = col;
    for ( ; temp_col < screen_Columns; temp_col++)
        if (CHAR_CLASS(p[temp_col]) != start_class && p[temp_col] != 0)
            break;
    cb->word_end_col = temp_col;
}

/*
 * Find the column position for the last non-whitespace character on the given line.
 */
    static int
clip_get_line_end(row)
    int         row;
{
    int     i;

    if (row >= screen_Rows || ScreenLines == NULL)
        return 0;
    for (i = screen_Columns; i > 0; i--)
        if (ScreenLines[LineOffset[row] + i - 1] != ' ')
            break;
    return i;
}

/*
 * Update the currently selected region by adding and/or subtracting from the
 * beginning or end and inverting the changed area(s).
 */
    static void
clip_update_modeless_selection(cb, row1, col1, row2, col2)
    clipboard_T     *cb;
    int             row1;
    int             col1;
    int             row2;
    int             col2;
{
    /* See if we changed at the beginning of the selection */
    if (row1 != cb->start.lnum || col1 != (int)cb->start.col)
    {
        clip_invert_area(row1, col1, (int)cb->start.lnum, cb->start.col, CLIP_TOGGLE);
        cb->start.lnum = row1;
        cb->start.col  = col1;
    }

    /* See if we changed at the end of the selection */
    if (row2 != cb->end.lnum || col2 != (int)cb->end.col)
    {
        clip_invert_area((int)cb->end.lnum, cb->end.col, row2, col2, CLIP_TOGGLE);
        cb->end.lnum = row2;
        cb->end.col  = col2;
    }
}

    static int
clip_mch_own_selection(clipboard_T *cbd)
{
    return TRUE;
}

    int
clip_gen_own_selection(clipboard_T *cbd)
{
    return clip_mch_own_selection(cbd);
}

    static void
clip_mch_lose_selection(clipboard_T *cbd)
{
}

    void
clip_gen_lose_selection(clipboard_T *cbd)
{
    clip_mch_lose_selection(cbd);
}

    static void
clip_mch_set_selection(clipboard_T *cbd)
{
}

    void
clip_gen_set_selection(clipboard_T *cbd)
{
    if (!clip_did_set_selection)
    {
        /* Updating postponed, so that accessing the system clipboard won't
         * hang Vim when accessing it many times (e.g. on a :g comand). */
        if ((cbd == &clip_plus && (clip_unnamed_saved & CLIP_UNNAMED_PLUS))
         || (cbd == &clip_star && (clip_unnamed_saved & CLIP_UNNAMED)))
        {
            clipboard_needs_update = TRUE;
            return;
        }
    }
    clip_mch_set_selection(cbd);
}

    static void
clip_mch_request_selection(clipboard_T *cbd)
{
}

    void
clip_gen_request_selection(clipboard_T *cbd)
{
    clip_mch_request_selection(cbd);
}

    int
clip_gen_owner_exists(clipboard_T *cbd)
{
    return TRUE;
}

/*****************************************************************************
 * Functions that handle the input buffer.
 * This is used for any GUI version, and the unix terminal version.
 *
 * For Unix, the input characters are buffered to be able to check for a
 * CTRL-C.  This should be done with signals, but I don't know how to do that
 * in a portable way for a tty in RAW mode.
 *
 * For the client-server code in the console the received keys are put in the input buffer.
 */

/*
 * Internal typeahead buffer.  Includes extra space for long key code
 * descriptions which would otherwise overflow.  The buffer is considered full
 * when only this extra space (or part of it) remains.
 */
#define INBUFLEN 250

static char_u   inbuf[INBUFLEN + MAX_KEY_CODE_LEN];
static int      inbufcount = 0;     /* number of chars in inbuf[] */

/*
 * vim_is_input_buf_full(), vim_is_input_buf_empty(), add_to_input_buf(), and
 * trash_input_buf() are functions for manipulating the input buffer.  These
 * are used by the gui_* calls when a GUI is used to handle keyboard input.
 */

    int
vim_is_input_buf_full()
{
    return (inbufcount >= INBUFLEN);
}

    int
vim_is_input_buf_empty()
{
    return (inbufcount == 0);
}

/*
 * Return the current contents of the input buffer and make it empty.
 * The returned pointer must be passed to set_input_buf() later.
 */
    char_u *
get_input_buf()
{
    garray_T    *gap;

    /* We use a growarray to store the data pointer and the length. */
    gap = (garray_T *)alloc((unsigned)sizeof(garray_T));
    if (gap != NULL)
    {
        /* Add one to avoid a zero size. */
        gap->ga_data = alloc((unsigned)inbufcount + 1);
        if (gap->ga_data != NULL)
            mch_memmove(gap->ga_data, inbuf, (size_t)inbufcount);
        gap->ga_len = inbufcount;
    }
    trash_input_buf();
    return (char_u *)gap;
}

/*
 * Restore the input buffer with a pointer returned from get_input_buf().
 * The allocated memory is freed, this only works once!
 */
    void
set_input_buf(p)
    char_u      *p;
{
    garray_T    *gap = (garray_T *)p;

    if (gap != NULL)
    {
        if (gap->ga_data != NULL)
        {
            mch_memmove(inbuf, gap->ga_data, gap->ga_len);
            inbufcount = gap->ga_len;
            vim_free(gap->ga_data);
        }
        vim_free(gap);
    }
}

/* Remove everything from the input buffer.  Called when ^C is found */
    void
trash_input_buf()
{
    inbufcount = 0;
}

/*
 * Read as much data from the input buffer as possible up to maxlen, and store it in buf.
 * Note: this function used to be Read() in unix.c
 */
    int
read_from_input_buf(buf, maxlen)
    char_u  *buf;
    long    maxlen;
{
    if (inbufcount == 0)        /* if the buffer is empty, fill it */
        fill_input_buf(TRUE);
    if (maxlen > inbufcount)
        maxlen = inbufcount;
    mch_memmove(buf, inbuf, (size_t)maxlen);
    inbufcount -= maxlen;
    if (inbufcount)
        mch_memmove(inbuf, inbuf + maxlen, (size_t)inbufcount);
    return (int)maxlen;
}

    void
fill_input_buf(exit_on_error)
    int exit_on_error UNUSED;
{
    int         len;
    int         try;
    static int  did_read_something = FALSE;
    static char_u *rest = NULL;     /* unconverted rest of previous read */
    static int  restlen = 0;
    int         unconverted;

    if (vim_is_input_buf_full())
        return;
    /*
     * Fill_input_buf() is only called when we really need a character.
     * If we can't get any, but there is some in the buffer, just return.
     * If we can't get any, and there isn't any in the buffer, we give up and exit Vim.
     */

    if (rest != NULL)
    {
        /* Use remainder of previous call, starts with an invalid character
         * that may become valid when reading more. */
        if (restlen > INBUFLEN - inbufcount)
            unconverted = INBUFLEN - inbufcount;
        else
            unconverted = restlen;
        mch_memmove(inbuf + inbufcount, rest, unconverted);
        if (unconverted == restlen)
        {
            vim_free(rest);
            rest = NULL;
        }
        else
        {
            restlen -= unconverted;
            mch_memmove(rest, rest + unconverted, restlen);
        }
        inbufcount += unconverted;
    }
    else
        unconverted = 0;

    len = 0;    /* to avoid gcc warning */
    for (try = 0; try < 100; ++try)
    {
        len = read(read_cmd_fd, (char *)inbuf + inbufcount, (size_t)(INBUFLEN - inbufcount));

        if (len > 0 || got_int)
            break;
        /*
         * If reading stdin results in an error, continue reading stderr.
         * This helps when using "foo | xargs vim".
         */
        if (!did_read_something && !isatty(read_cmd_fd) && read_cmd_fd == 0)
        {
            int m = cur_tmode;

            /* We probably set the wrong file descriptor to raw mode.  Switch
             * back to cooked mode, use another descriptor and set the mode to what it was. */
            settmode(TMODE_COOK);
            /* Use stderr for stdin, also works for shell commands. */
            close(0);
            ignored = dup(2);
            settmode(m);
        }
        if (!exit_on_error)
            return;
    }
    if (len <= 0 && !got_int)
        read_error_exit();
    if (len > 0)
        did_read_something = TRUE;
    if (got_int)
    {
        /* Interrupted, pretend a CTRL-C was typed. */
        inbuf[0] = 3;
        inbufcount = 1;
    }
    else
    {
        /*
         * May perform conversion on the input characters.
         * Include the unconverted rest of the previous call.
         * If there is an incomplete char at the end it is kept for the next
         * time, reading more bytes should make conversion possible.
         * Don't do this in the unlikely event that the input buffer is too
         * small ("rest" still contains more bytes).
         */
        while (len-- > 0)
        {
            /*
             * if a CTRL-C was typed, remove it from the buffer and set got_int
             */
            if (inbuf[inbufcount] == 3 && ctrl_c_interrupts)
            {
                /* remove everything typed before the CTRL-C */
                mch_memmove(inbuf, inbuf + inbufcount, (size_t)(len + 1));
                inbufcount = 0;
                got_int = TRUE;
            }
            ++inbufcount;
        }
    }
}

/*
 * Exit because of an input read error.
 */
    void
read_error_exit()
{
    if (silent_mode)    /* Normal way to exit for "ex -s" */
        getout(0);
    STRCPY(IObuff, "Vim: Error reading input, exiting...\n");
    preserve_exit();
}

/*
 * May update the shape of the cursor.
 */
    void
ui_cursor_shape()
{
    term_cursor_shape();

    conceal_check_cursur_line();
}

/*
 * Check bounds for column number
 */
    int
check_col(col)
    int     col;
{
    if (col < 0)
        return 0;
    if (col >= (int)screen_Columns)
        return (int)screen_Columns - 1;

    return col;
}

/*
 * Check bounds for row number
 */
    int
check_row(row)
    int     row;
{
    if (row < 0)
        return 0;
    if (row >= (int)screen_Rows)
        return (int)screen_Rows - 1;

    return row;
}

/*
 * Move the cursor to the specified row and column on the screen.
 * Change current window if necessary.  Returns an integer with the
 * CURSOR_MOVED bit set if the cursor has moved or unset otherwise.
 *
 * If flags has MOUSE_FOCUS, then the current window will not be changed, and
 * if the mouse is outside the window then the text will scroll, or if the
 * mouse was previously on a status line, then the status line may be dragged.
 *
 * If flags has MOUSE_MAY_VIS, then VIsual mode will be started before the
 * cursor is moved unless the cursor was on a status line.
 * This function returns one of IN_UNKNOWN, IN_BUFFER, IN_STATUS_LINE or
 * IN_SEP_LINE depending on where the cursor was clicked.
 *
 * If flags has MOUSE_MAY_STOP_VIS, then Visual mode will be stopped, unless
 * the mouse is on the status line of the same window.
 *
 * If flags has MOUSE_DID_MOVE, nothing is done if the mouse didn't move since
 * the last call.
 *
 * If flags has MOUSE_SETPOS, nothing is done, only the current position is
 * remembered.
 */
    int
jump_to_mouse(flags, inclusive, which_button)
    int         flags;
    int         *inclusive;     /* used for inclusive operator, can be NULL */
    int         which_button;   /* MOUSE_LEFT, MOUSE_RIGHT, MOUSE_MIDDLE */
{
    static int  on_status_line = 0;     /* #lines below bottom of window */
    static int  on_sep_line = 0;        /* on separator right of window */
    static int  prev_row = -1;
    static int  prev_col = -1;
    static win_T *dragwin = NULL;       /* window being dragged */
    static int  did_drag = FALSE;       /* drag was noticed */

    win_T       *wp, *old_curwin;
    pos_T       old_cursor;
    int         count;
    int         first;
    int         row = mouse_row;
    int         col = mouse_col;

    mouse_past_bottom = FALSE;
    mouse_past_eol = FALSE;

    if (flags & MOUSE_RELEASED)
    {
        /* On button release we may change window focus if positioned on a
         * status line and no dragging happened. */
        if (dragwin != NULL && !did_drag)
            flags &= ~(MOUSE_FOCUS | MOUSE_DID_MOVE);
        dragwin = NULL;
        did_drag = FALSE;
    }

    if ((flags & MOUSE_DID_MOVE) && prev_row == mouse_row && prev_col == mouse_col)
    {
retnomove:
        /* before moving the cursor for a left click which is NOT in a status
         * line, stop Visual mode */
        if (on_status_line)
            return IN_STATUS_LINE;
        if (on_sep_line)
            return IN_SEP_LINE;
        if (flags & MOUSE_MAY_STOP_VIS)
        {
            end_visual_mode();
            redraw_curbuf_later(INVERTED);      /* delete the inversion */
        }
        /* Continue a modeless selection in another window. */
        if (cmdwin_type != 0 && row < W_WINROW(curwin))
            return IN_OTHER_WIN;

        return IN_BUFFER;
    }

    prev_row = mouse_row;
    prev_col = mouse_col;

    if (flags & MOUSE_SETPOS)
        goto retnomove;                         /* ugly goto... */

    old_curwin = curwin;
    old_cursor = curwin->w_cursor;

    if (!(flags & MOUSE_FOCUS))
    {
        if (row < 0 || col < 0)                 /* check if it makes sense */
            return IN_UNKNOWN;

        /* find the window where the row is in */
        wp = mouse_find_win(&row, &col);
        dragwin = NULL;
        /*
         * winpos and height may change in win_enter()!
         */
        if (row >= wp->w_height)                /* In (or below) status line */
        {
            on_status_line = row - wp->w_height + 1;
            dragwin = wp;
        }
        else
            on_status_line = 0;
        if (col >= wp->w_width)         /* In separator line */
        {
            on_sep_line = col - wp->w_width + 1;
            dragwin = wp;
        }
        else
            on_sep_line = 0;

        /* The rightmost character of the status line might be a vertical
         * separator character if there is no connecting window to the right. */
        if (on_status_line && on_sep_line)
        {
            if (stl_connected(wp))
                on_sep_line = 0;
            else
                on_status_line = 0;
        }

        /* Before jumping to another buffer, or moving the cursor for a left
         * click, stop Visual mode. */
        if (VIsual_active
                && (wp->w_buffer != curwin->w_buffer
                    || (!on_status_line
                        && !on_sep_line
                        && (flags & MOUSE_MAY_STOP_VIS))))
        {
            end_visual_mode();
            redraw_curbuf_later(INVERTED);      /* delete the inversion */
        }
        if (cmdwin_type != 0 && wp != curwin)
        {
            /* A click outside the command-line window: Use modeless
             * selection if possible.  Allow dragging the status lines. */
            on_sep_line = 0;
            if (on_status_line)
                return IN_STATUS_LINE;

            return IN_OTHER_WIN;
        }
        /* Only change window focus when not clicking on or dragging the
         * status line.  Do change focus when releasing the mouse button
         * (MOUSE_FOCUS was set above if we dragged first). */
        if (dragwin == NULL || (flags & MOUSE_RELEASED))
            win_enter(wp, TRUE);                /* can make wp invalid! */
        /* set topline, to be able to check for double click ourselves */
        if (curwin != old_curwin)
            set_mouse_topline(curwin);
        if (on_status_line)                     /* In (or below) status line */
        {
            /* Don't use start_arrow() if we're in the same window */
            if (curwin == old_curwin)
                return IN_STATUS_LINE;
            else
                return IN_STATUS_LINE | CURSOR_MOVED;
        }
        if (on_sep_line)                        /* In (or below) status line */
        {
            /* Don't use start_arrow() if we're in the same window */
            if (curwin == old_curwin)
                return IN_SEP_LINE;
            else
                return IN_SEP_LINE | CURSOR_MOVED;
        }

        curwin->w_cursor.lnum = curwin->w_topline;
    }
    else if (on_status_line && which_button == MOUSE_LEFT)
    {
        if (dragwin != NULL)
        {
            /* Drag the status line */
            count = row - dragwin->w_winrow - dragwin->w_height + 1 - on_status_line;
            win_drag_status_line(dragwin, count);
            did_drag |= count;
        }
        return IN_STATUS_LINE;                  /* Cursor didn't move */
    }
    else if (on_sep_line && which_button == MOUSE_LEFT)
    {
        if (dragwin != NULL)
        {
            /* Drag the separator column */
            count = col - dragwin->w_wincol - dragwin->w_width + 1 - on_sep_line;
            win_drag_vsep_line(dragwin, count);
            did_drag |= count;
        }
        return IN_SEP_LINE;                     /* Cursor didn't move */
    }
    else /* keep_window_focus must be TRUE */
    {
        /* before moving the cursor for a left click, stop Visual mode */
        if (flags & MOUSE_MAY_STOP_VIS)
        {
            end_visual_mode();
            redraw_curbuf_later(INVERTED);      /* delete the inversion */
        }

        /* Continue a modeless selection in another window. */
        if (cmdwin_type != 0 && row < W_WINROW(curwin))
            return IN_OTHER_WIN;

        row -= W_WINROW(curwin);
        col -= W_WINCOL(curwin);

        /*
         * When clicking beyond the end of the window, scroll the screen.
         * Scroll by however many rows outside the window we are.
         */
        if (row < 0)
        {
            count = 0;
            for (first = TRUE; curwin->w_topline > 1; )
            {
                count += plines(curwin->w_topline - 1);
                if (!first && count > -row)
                    break;
                first = FALSE;
                --curwin->w_topline;
            }
            curwin->w_valid &= ~(VALID_WROW|VALID_CROW|VALID_BOTLINE|VALID_BOTLINE_AP);
            redraw_later(VALID);
            row = 0;
        }
        else if (row >= curwin->w_height)
        {
            count = 0;
            for (first = TRUE; curwin->w_topline < curbuf->b_ml.ml_line_count; )
            {
                count += plines(curwin->w_topline);
                if (!first && count > row - curwin->w_height + 1)
                    break;
                first = FALSE;
                ++curwin->w_topline;
            }
            redraw_later(VALID);
            curwin->w_valid &= ~(VALID_WROW|VALID_CROW|VALID_BOTLINE|VALID_BOTLINE_AP);
            row = curwin->w_height - 1;
        }
        else if (row == 0)
        {
            /* When dragging the mouse, while the text has been scrolled up as
             * far as it goes, moving the mouse in the top line should scroll
             * the text down (done later when recomputing w_topline). */
            if (mouse_dragging > 0
                    && curwin->w_cursor.lnum == curwin->w_buffer->b_ml.ml_line_count
                    && curwin->w_cursor.lnum == curwin->w_topline)
                curwin->w_valid &= ~(VALID_TOPLINE);
        }
    }

    /* compute the position in the buffer line from the posn on the screen */
    if (mouse_comp_pos(curwin, &row, &col, &curwin->w_cursor.lnum))
        mouse_past_bottom = TRUE;

    /* Start Visual mode before coladvance(), for when 'sel' != "old" */
    if ((flags & MOUSE_MAY_VIS) && !VIsual_active)
    {
        check_visual_highlight();
        VIsual = old_cursor;
        VIsual_active = TRUE;
        VIsual_reselect = TRUE;
        /* if 'selectmode' contains "mouse", start Select mode */
        may_start_select('o');
        setmouse();
        if (p_smd && msg_silent == 0)
            redraw_cmdline = TRUE;      /* show visual mode later */
    }

    curwin->w_curswant = col;
    curwin->w_set_curswant = FALSE;     /* May still have been TRUE */
    if (coladvance(col) == FAIL)        /* Mouse click beyond end of line */
    {
        if (inclusive != NULL)
            *inclusive = TRUE;
        mouse_past_eol = TRUE;
    }
    else if (inclusive != NULL)
        *inclusive = FALSE;

    count = IN_BUFFER;
    if (curwin != old_curwin || curwin->w_cursor.lnum != old_cursor.lnum
            || curwin->w_cursor.col != old_cursor.col)
        count |= CURSOR_MOVED;          /* Cursor has moved */

    return count;
}

/*
 * Compute the position in the buffer line from the posn on the screen in window "win".
 * Returns TRUE if the position is below the last line.
 */
    int
mouse_comp_pos(win, rowp, colp, lnump)
    win_T       *win;
    int         *rowp;
    int         *colp;
    linenr_T    *lnump;
{
    int         col = *colp;
    int         row = *rowp;
    linenr_T    lnum;
    int         retval = FALSE;
    int         off;
    int         count;

    if (win->w_p_rl)
        col = W_WIDTH(win) - 1 - col;

    lnum = win->w_topline;

    while (row > 0)
    {
        count = plines_win(win, lnum, TRUE);
        if (count > row)
            break;      /* Position is in this buffer line. */
        if (lnum == win->w_buffer->b_ml.ml_line_count)
        {
            retval = TRUE;
            break;              /* past end of file */
        }
        row -= count;
        ++lnum;
    }

    if (!retval)
    {
        /* Compute the column without wrapping. */
        off = win_col_off(win) - win_col_off2(win);
        if (col < off)
            col = off;
        col += row * (W_WIDTH(win) - off);
        /* add skip column (for long wrapping line) */
        col += win->w_skipcol;
    }

    if (!win->w_p_wrap)
        col += win->w_leftcol;

    /* skip line number and fold column in front of the line */
    col -= win_col_off(win);
    if (col < 0)
    {
        col = 0;
    }

    *colp = col;
    *rowp = row;
    *lnump = lnum;
    return retval;
}

/*
 * Find the window at screen position "*rowp" and "*colp".  The positions are
 * updated to become relative to the top-left of the window.
 */
    win_T *
mouse_find_win(rowp, colp)
    int         *rowp;
    int         *colp UNUSED;
{
    frame_T     *fp;

    fp = topframe;
    *rowp -= firstwin->w_winrow;
    for (;;)
    {
        if (fp->fr_layout == FR_LEAF)
            break;
        if (fp->fr_layout == FR_ROW)
        {
            for (fp = fp->fr_child; fp->fr_next != NULL; fp = fp->fr_next)
            {
                if (*colp < fp->fr_width)
                    break;
                *colp -= fp->fr_width;
            }
        }
        else    /* fr_layout == FR_COL */
        {
            for (fp = fp->fr_child; fp->fr_next != NULL; fp = fp->fr_next)
            {
                if (*rowp < fp->fr_height)
                    break;
                *rowp -= fp->fr_height;
            }
        }
    }
    return fp->fr_win;
}
