/*
 * misc2.c: Various functions.
 */
#include "vim.h"

static char_u   *username = NULL; /* cached result of mch_get_user_name() */

static int coladvance2(pos_T *pos, int addspaces, int finetune, colnr_T wcol);

/*
 * Return TRUE if in the current mode we need to use virtual.
 */
    int
virtual_active()
{
    /* While an operator is being executed we return "virtual_op", because
     * VIsual_active has already been reset, thus we can't check for "block" being used. */
    if (virtual_op != MAYBE)
        return virtual_op;
    return (ve_flags == VE_ALL
            || ((ve_flags & VE_BLOCK) && VIsual_active && VIsual_mode == Ctrl_V)
            || ((ve_flags & VE_INSERT) && (State & INSERT)));
}

/*
 * Get the screen position of the cursor.
 */
    int
getviscol()
{
    colnr_T     x;

    getvvcol(curwin, &curwin->w_cursor, &x, NULL, NULL);
    return (int)x;
}

/*
 * Get the screen position of character col with a coladd in the cursor line.
 */
    int
getviscol2(col, coladd)
    colnr_T     col;
    colnr_T     coladd;
{
    colnr_T     x;
    pos_T       pos;

    pos.lnum = curwin->w_cursor.lnum;
    pos.col = col;
    pos.coladd = coladd;
    getvvcol(curwin, &pos, &x, NULL, NULL);
    return (int)x;
}

/*
 * Go to column "wcol", and add/insert white space as necessary to get the
 * cursor in that column.
 * The caller must have saved the cursor line for undo!
 */
    int
coladvance_force(wcol)
    colnr_T wcol;
{
    int rc = coladvance2(&curwin->w_cursor, TRUE, FALSE, wcol);

    if (wcol == MAXCOL)
        curwin->w_valid &= ~VALID_VIRTCOL;
    else
    {
        /* Virtcol is valid */
        curwin->w_valid |= VALID_VIRTCOL;
        curwin->w_virtcol = wcol;
    }
    return rc;
}

/*
 * Try to advance the Cursor to the specified screen column.
 * If virtual editing: fine tune the cursor position.
 * Note that all virtual positions off the end of a line should share
 * a curwin->w_cursor.col value (n.b. this is equal to STRLEN(line)),
 * beginning at coladd 0.
 *
 * return OK if desired column is reached, FAIL if not
 */
    int
coladvance(wcol)
    colnr_T     wcol;
{
    int rc = getvpos(&curwin->w_cursor, wcol);

    if (wcol == MAXCOL || rc == FAIL)
        curwin->w_valid &= ~VALID_VIRTCOL;
    else if (*ml_get_cursor() != TAB)
    {
        /* Virtcol is valid when not on a TAB */
        curwin->w_valid |= VALID_VIRTCOL;
        curwin->w_virtcol = wcol;
    }
    return rc;
}

/*
 * Return in "pos" the position of the cursor advanced to screen column "wcol".
 * return OK if desired column is reached, FAIL if not
 */
    int
getvpos(pos, wcol)
    pos_T   *pos;
    colnr_T wcol;
{
    return coladvance2(pos, FALSE, virtual_active(), wcol);
}

    static int
coladvance2(pos, addspaces, finetune, wcol)
    pos_T       *pos;
    int         addspaces;      /* change the text to achieve our goal? */
    int         finetune;       /* change char offset for the exact column */
    colnr_T     wcol;           /* column to move to */
{
    int         idx;
    char_u      *ptr;
    char_u      *line;
    colnr_T     col = 0;
    int         csize = 0;
    int         one_more;
    int         head = 0;

    one_more = (State & INSERT)
                    || restart_edit != NUL
                    || (VIsual_active && *p_sel != 'o')
                    || ((ve_flags & VE_ONEMORE) && wcol < MAXCOL)
                    ;
    line = ml_get_buf(curbuf, pos->lnum, FALSE);

    if (wcol >= MAXCOL)
    {
        idx = (int)STRLEN(line) - 1 + one_more;
        col = wcol;

        if ((addspaces || finetune) && !VIsual_active)
        {
            curwin->w_curswant = linetabsize(line) + one_more;
            if (curwin->w_curswant > 0)
                --curwin->w_curswant;
        }
    }
    else
    {
        int width = W_WIDTH(curwin) - win_col_off(curwin);

        if (finetune
                && curwin->w_p_wrap
                && curwin->w_width != 0
                && wcol >= (colnr_T)width)
        {
            csize = linetabsize(line);
            if (csize > 0)
                csize--;

            if (wcol / width > (colnr_T)csize / width
                    && ((State & INSERT) == 0 || (int)wcol > csize + 1))
            {
                /* In case of line wrapping don't move the cursor beyond the
                 * right screen edge.  In Insert mode allow going just beyond
                 * the last character (like what happens when typing and
                 * reaching the right window edge). */
                wcol = (csize / width + 1) * width - 1;
            }
        }

        ptr = line;
        while (col <= wcol && *ptr != NUL)
        {
            /* Count a tab for what it's worth (if list mode not on) */
            csize = win_lbr_chartabsize(curwin, line, ptr, col, &head);
            ptr += utfc_ptr2len(ptr);
            col += csize;
        }
        idx = (int)(ptr - line);
        /*
         * Handle all the special cases.  The virtual_active() check
         * is needed to ensure that a virtual position off the end of
         * a line has the correct indexing.  The one_more comparison
         * replaces an explicit add of one_more later on.
         */
        if (col > wcol || (!virtual_active() && one_more == 0))
        {
            idx -= 1;
            /* Don't count the chars from 'showbreak'. */
            csize -= head;
            col -= csize;
        }

        if (virtual_active() && addspaces && ((col != wcol && col != wcol + 1) || csize > 1))
        {
            /* 'virtualedit' is set: The difference between wcol and col is filled with spaces. */

            if (line[idx] == NUL)
            {
                /* Append spaces */
                int     correct = wcol - col;
                char_u  *newline = alloc(idx + correct + 1);
                int     t;

                if (newline == NULL)
                    return FAIL;

                for (t = 0; t < idx; ++t)
                    newline[t] = line[t];

                for (t = 0; t < correct; ++t)
                    newline[t + idx] = ' ';

                newline[idx + correct] = NUL;

                ml_replace(pos->lnum, newline, FALSE);
                changed_bytes(pos->lnum, (colnr_T)idx);
                idx += correct;
                col = wcol;
            }
            else
            {
                /* Break a tab */
                int     linelen = (int)STRLEN(line);
                int     correct = wcol - col - csize + 1; /* negative!! */
                char_u  *newline;
                int     t, s = 0;
                int     v;

                if (-correct > csize)
                    return FAIL;

                newline = alloc(linelen + csize);
                if (newline == NULL)
                    return FAIL;

                for (t = 0; t < linelen; t++)
                {
                    if (t != idx)
                        newline[s++] = line[t];
                    else
                        for (v = 0; v < csize; v++)
                            newline[s++] = ' ';
                }

                newline[linelen + csize - 1] = NUL;

                ml_replace(pos->lnum, newline, FALSE);
                changed_bytes(pos->lnum, idx);
                idx += (csize - 1 + correct);
                col += correct;
            }
        }
    }

    if (idx < 0)
        pos->col = 0;
    else
        pos->col = idx;

    pos->coladd = 0;

    if (finetune)
    {
        if (wcol == MAXCOL)
        {
            /* The width of the last character is used to set coladd. */
            if (!one_more)
            {
                colnr_T     scol, ecol;

                getvcol(curwin, pos, &scol, NULL, &ecol);
                pos->coladd = ecol - scol;
            }
        }
        else
        {
            int b = (int)wcol - (int)col;

            /* The difference between wcol and col is used to set coladd. */
            if (b > 0 && b < (MAXCOL - 2 * W_WIDTH(curwin)))
                pos->coladd = b;

            col += b;
        }
    }

    /* prevent from moving onto a trail byte */
    mb_adjustpos(curbuf, pos);

    if (col < wcol)
        return FAIL;
    return OK;
}

/*
 * Increment the cursor position.  See inc() for return values.
 */
    int
inc_cursor()
{
    return inc(&curwin->w_cursor);
}

/*
 * Increment the line pointer "lp" crossing line boundaries as necessary.
 * Return 1 when going to the next line.
 * Return 2 when moving forward onto a NUL at the end of the line).
 * Return -1 when at the end of file.
 * Return 0 otherwise.
 */
    int
inc(lp)
    pos_T  *lp;
{
    char_u  *p = ml_get_pos(lp);

    if (*p != NUL)      /* still within line, move to next char (may be NUL) */
    {
        int l = utfc_ptr2len(p);

        lp->col += l;
        return ((p[l] != NUL) ? 0 : 2);
    }
    if (lp->lnum != curbuf->b_ml.ml_line_count)     /* there is a next line */
    {
        lp->col = 0;
        lp->lnum++;
        lp->coladd = 0;
        return 1;
    }
    return -1;
}

/*
 * incl(lp): same as inc(), but skip the NUL at the end of non-empty lines
 */
    int
incl(lp)
    pos_T    *lp;
{
    int     r;

    if ((r = inc(lp)) >= 1 && lp->col)
        r = inc(lp);
    return r;
}

/*
 * dec(p)
 *
 * Decrement the line pointer 'p' crossing line boundaries as necessary.
 * Return 1 when crossing a line, -1 when at start of file, 0 otherwise.
 */
    int
dec_cursor()
{
    return dec(&curwin->w_cursor);
}

    int
dec(lp)
    pos_T  *lp;
{
    char_u      *p;

    lp->coladd = 0;
    if (lp->col > 0)            /* still within line */
    {
        lp->col--;
        p = ml_get(lp->lnum);
        lp->col -= utf_head_off(p, p + lp->col);
        return 0;
    }
    if (lp->lnum > 1)           /* there is a prior line */
    {
        lp->lnum--;
        p = ml_get(lp->lnum);
        lp->col = (colnr_T)STRLEN(p);
        lp->col -= utf_head_off(p, p + lp->col);
        return 1;
    }
    return -1;                  /* at start of file */
}

/*
 * decl(lp): same as dec(), but skip the NUL at the end of non-empty lines
 */
    int
decl(lp)
    pos_T    *lp;
{
    int     r;

    if ((r = dec(lp)) == 1 && lp->col)
        r = dec(lp);
    return r;
}

/*
 * Get the line number relative to the current cursor position, i.e. the
 * difference between line number and cursor position. Only look for lines that
 * can be visible, folded lines don't count.
 */
    linenr_T
get_cursor_rel_lnum(wp, lnum)
    win_T       *wp;
    linenr_T    lnum;               /* line number to get the result for */
{
    linenr_T    cursor = wp->w_cursor.lnum;
    linenr_T    retval = 0;

        retval = lnum - cursor;

    return retval;
}

/*
 * Make sure curwin->w_cursor.lnum is valid.
 */
    void
check_cursor_lnum()
{
    if (curwin->w_cursor.lnum > curbuf->b_ml.ml_line_count)
    {
        curwin->w_cursor.lnum = curbuf->b_ml.ml_line_count;
    }
    if (curwin->w_cursor.lnum <= 0)
        curwin->w_cursor.lnum = 1;
}

/*
 * Make sure curwin->w_cursor.col is valid.
 */
    void
check_cursor_col()
{
    check_cursor_col_win(curwin);
}

/*
 * Make sure win->w_cursor.col is valid.
 */
    void
check_cursor_col_win(win)
    win_T *win;
{
    colnr_T len;
    colnr_T oldcol = win->w_cursor.col;
    colnr_T oldcoladd = win->w_cursor.col + win->w_cursor.coladd;

    len = (colnr_T)STRLEN(ml_get_buf(win->w_buffer, win->w_cursor.lnum, FALSE));
    if (len == 0)
        win->w_cursor.col = 0;
    else if (win->w_cursor.col >= len)
    {
        /* Allow cursor past end-of-line when:
         * - in Insert mode or restarting Insert mode
         * - in Visual mode and 'selection' isn't "old"
         * - 'virtualedit' is set */
        if ((State & INSERT) || restart_edit
                || (VIsual_active && *p_sel != 'o')
                || (ve_flags & VE_ONEMORE)
                || virtual_active())
            win->w_cursor.col = len;
        else
        {
            win->w_cursor.col = len - 1;
            /* Move the cursor to the head byte. */
            mb_adjustpos(win->w_buffer, &win->w_cursor);
        }
    }
    else if (win->w_cursor.col < 0)
        win->w_cursor.col = 0;

    /* If virtual editing is on, we can leave the cursor on the old position,
     * only we must set it to virtual.  But don't do it when at the end of the line. */
    if (oldcol == MAXCOL)
        win->w_cursor.coladd = 0;
    else if (ve_flags == VE_ALL)
    {
        if (oldcoladd > win->w_cursor.col)
            win->w_cursor.coladd = oldcoladd - win->w_cursor.col;
        else
            /* avoid weird number when there is a miscalculation or overflow */
            win->w_cursor.coladd = 0;
    }
}

/*
 * make sure curwin->w_cursor in on a valid character
 */
    void
check_cursor()
{
    check_cursor_lnum();
    check_cursor_col();
}

/*
 * Make sure curwin->w_cursor is not on the NUL at the end of the line.
 * Allow it when in Visual mode and 'selection' is not "old".
 */
    void
adjust_cursor_col()
{
    if (curwin->w_cursor.col > 0
            && (!VIsual_active || *p_sel == 'o')
            && gchar_cursor() == NUL)
        --curwin->w_cursor.col;
}

/*
 * When curwin->w_leftcol has changed, adjust the cursor position.
 * Return TRUE if the cursor was moved.
 */
    int
leftcol_changed()
{
    long        lastcol;
    colnr_T     s, e;
    int         retval = FALSE;

    changed_cline_bef_curs();
    lastcol = curwin->w_leftcol + W_WIDTH(curwin) - curwin_col_off() - 1;
    validate_virtcol();

    /*
     * If the cursor is right or left of the screen, move it to last or first character.
     */
    if (curwin->w_virtcol > (colnr_T)(lastcol - p_siso))
    {
        retval = TRUE;
        coladvance((colnr_T)(lastcol - p_siso));
    }
    else if (curwin->w_virtcol < curwin->w_leftcol + p_siso)
    {
        retval = TRUE;
        (void)coladvance((colnr_T)(curwin->w_leftcol + p_siso));
    }

    /*
     * If the start of the character under the cursor is not on the screen,
     * advance the cursor one more char.  If this fails (last char of the
     * line) adjust the scrolling.
     */
    getvvcol(curwin, &curwin->w_cursor, &s, NULL, &e);
    if (e > (colnr_T)lastcol)
    {
        retval = TRUE;
        coladvance(s - 1);
    }
    else if (s < curwin->w_leftcol)
    {
        retval = TRUE;
        if (coladvance(e + 1) == FAIL)  /* there isn't another character */
        {
            curwin->w_leftcol = s;      /* adjust w_leftcol instead */
            changed_cline_bef_curs();
        }
    }

    if (retval)
        curwin->w_set_curswant = TRUE;
    redraw_later(NOT_VALID);
    return retval;
}

/**********************************************************************
 * Various routines dealing with allocation and deallocation of memory.
 */

/*
 * Some memory is reserved for error messages and for being able to
 * call mf_release_all(), which needs some memory for mf_trans_add().
 */
#define KEEP_ROOM (2 * 8192L)
#define KEEP_ROOM_KB (KEEP_ROOM / 1024L)

/*
 * Note: if unsigned is 16 bits we can only allocate up to 64K with alloc().
 * Use lalloc for larger blocks.
 */
    char_u *
alloc(size)
    unsigned        size;
{
    return (lalloc((long_u)size, TRUE));
}

/*
 * Allocate memory and set all bytes to zero.
 */
    char_u *
alloc_clear(size)
    unsigned        size;
{
    char_u *p;

    p = lalloc((long_u)size, TRUE);
    if (p != NULL)
        (void)vim_memset(p, 0, (size_t)size);
    return p;
}

/*
 * alloc() with check for maximum line length
 */
    char_u *
alloc_check(size)
    unsigned        size;
{
    return (lalloc((long_u)size, TRUE));
}

/*
 * Allocate memory like lalloc() and set all bytes to zero.
 */
    char_u *
lalloc_clear(size, message)
    long_u      size;
    int         message;
{
    char_u *p;

    p = (lalloc(size, message));
    if (p != NULL)
        (void)vim_memset(p, 0, (size_t)size);
    return p;
}

/*
 * Low level memory allocation function.
 * This is used often, KEEP IT FAST!
 */
    char_u *
lalloc(size, message)
    long_u      size;
    int         message;
{
    char_u      *p;                 /* pointer to new storage space */
    static int  releasing = FALSE;  /* don't do mf_release_all() recursive */
    int         try_again;

    /* Safety check for allocating zero bytes */
    if (size == 0)
    {
        /* Don't hide this message */
        emsg_silent = 0;
        EMSGN("E341: Internal error: lalloc(%ld, )", size);
        return NULL;
    }

    /*
     * Loop when out of memory: Try to release some memfile blocks and
     * if some blocks are released call malloc again.
     */
    for (;;)
    {
        /*
         * Handle three kind of systems:
         * 1. No check for available memory: Just return.
         * 2. Slow check for available memory: call mch_avail_mem() after
         *    allocating KEEP_ROOM amount of memory.
         * 3. Strict check for available memory: call mch_avail_mem()
         */
        if ((p = (char_u *)malloc((size_t)size)) != NULL)
        {
            /* 1. No check for available memory: Just return. */
            goto theend;
        }
        /*
         * Remember that mf_release_all() is being called to avoid an endless
         * loop, because mf_release_all() may call alloc() recursively.
         */
        if (releasing)
            break;
        releasing = TRUE;

        clear_sb_text();              /* free any scrollback text */
        try_again = mf_release_all(); /* release as many blocks as possible */
        try_again |= garbage_collect(); /* cleanup recursive lists/dicts */

        releasing = FALSE;
        if (!try_again)
            break;
    }

    if (message && p == NULL)
        do_outofmem_msg(size);

theend:
    return p;
}

/*
 * Avoid repeating the error message many times (they take 1 second each).
 * Did_outofmem_msg is reset when a character is read.
 */
    void
do_outofmem_msg(size)
    long_u      size;
{
    if (!did_outofmem_msg)
    {
        /* Don't hide this message */
        emsg_silent = 0;

        /* Must come first to avoid coming back here when printing the error
         * message fails, e.g. when setting v:errmsg. */
        did_outofmem_msg = TRUE;

        EMSGN("E342: Out of memory!  (allocating %lu bytes)", size);
    }
}

#if defined(EXITFREE)

/*
 * Free everything that we allocated.
 * Can be used to detect memory leaks, e.g., with ccmalloc.
 * NOTE: This is tricky!  Things are freed that functions depend on.  Don't be
 * surprised if Vim crashes...
 * Some things can't be freed, esp. things local to a library function.
 */
    void
free_all_mem()
{
    buf_T       *buf, *nextbuf;
    static int  entered = FALSE;

    /* When we cause a crash here it is caught and Vim tries to exit cleanly.
     * Don't try freeing everything again. */
    if (entered)
        return;
    entered = TRUE;

    /* Don't want to trigger autocommands from here on. */
    block_autocmds();

    /* Close all tabs and windows.  Reset 'equalalways' to avoid redraws. */
    p_ea = FALSE;
    if (first_tabpage->tp_next != NULL)
        do_cmdline_cmd((char_u *)"tabonly!");
    if (firstwin != lastwin)
        do_cmdline_cmd((char_u *)"only!");

    /* Clear user commands (before deleting buffers). */
    ex_comclear(NULL);

    /* Clear mappings, abbreviations, breakpoints. */
    do_cmdline_cmd((char_u *)"lmapclear");
    do_cmdline_cmd((char_u *)"xmapclear");
    do_cmdline_cmd((char_u *)"mapclear");
    do_cmdline_cmd((char_u *)"mapclear!");
    do_cmdline_cmd((char_u *)"abclear");
    do_cmdline_cmd((char_u *)"breakdel *");

    free_titles();

    /* Obviously named calls. */
    free_all_autocmds();
    clear_termcodes();
    free_all_options();
    free_all_marks();
    alist_clear(&global_alist);
    free_homedir();
    free_users();
    free_search_patterns();
    free_old_sub();
    free_last_insert();
    free_prev_shellcmd();
    free_regexp_stuff();
    free_cd_dir();
    set_expr_line(NULL);
    clear_sb_text();          /* free any scrollback text */

    /* Free some global vars. */
    vim_free(username);
    vim_regfree(clip_exclude_prog);
    vim_free(last_cmdline);
    vim_free(new_last_cmdline);
    set_keep_msg(NULL, 0);

    /* Clear cmdline history. */
    p_hi = 0;
    init_history();

    /* Close all script inputs. */
    close_all_scripts();

    /* Destroy all windows.  Must come before freeing buffers. */
    win_free_all();

    /* Free all buffers.  Reset 'autochdir' to avoid accessing things that
     * were freed already. */
    for (buf = firstbuf; buf != NULL; )
    {
        nextbuf = buf->b_next;
        close_buffer(NULL, buf, DOBUF_WIPE, FALSE);
        if (buf_valid(buf))
            buf = nextbuf;      /* didn't work, try next one */
        else
            buf = firstbuf;
    }

    /* Clear registers. */
    clear_registers();
    ResetRedobuff();
    ResetRedobuff();

    /* highlight info */
    free_highlight();

    reset_last_sourcing();

    free_tabpage(first_tabpage);
    first_tabpage = NULL;

    /* Machine-specific free. */
    mch_free_mem();

    /* message history */
    for (;;)
        if (delete_first_msg() == FAIL)
            break;

    eval_clear();

    free_termoptions();

    /* screenlines (can't display anything now!) */
    free_screenlines();

    clear_hl_tables();

    vim_free(IObuff);
    vim_free(NameBuff);
}
#endif

/*
 * Copy "string" into newly allocated memory.
 */
    char_u *
vim_strsave(string)
    char_u      *string;
{
    char_u      *p;
    unsigned    len;

    len = (unsigned)STRLEN(string) + 1;
    p = alloc(len);
    if (p != NULL)
        mch_memmove(p, string, (size_t)len);
    return p;
}

/*
 * Copy up to "len" bytes of "string" into newly allocated memory and terminate with a NUL.
 * The allocated memory always has size "len + 1", also when "string" is shorter.
 */
    char_u *
vim_strnsave(string, len)
    char_u      *string;
    int         len;
{
    char_u      *p;

    p = alloc((unsigned)(len + 1));
    if (p != NULL)
    {
        STRNCPY(p, string, len);
        p[len] = NUL;
    }
    return p;
}

/*
 * Same as vim_strsave(), but any characters found in esc_chars are preceded by a backslash.
 */
    char_u *
vim_strsave_escaped(string, esc_chars)
    char_u      *string;
    char_u      *esc_chars;
{
    return vim_strsave_escaped_ext(string, esc_chars, '\\', FALSE);
}

/*
 * Same as vim_strsave_escaped(), but when "bsl" is TRUE also escape
 * characters where rem_backslash() would remove the backslash.
 * Escape the characters with "cc".
 */
    char_u *
vim_strsave_escaped_ext(string, esc_chars, cc, bsl)
    char_u      *string;
    char_u      *esc_chars;
    int         cc;
    int         bsl;
{
    char_u      *p;
    char_u      *p2;
    char_u      *escaped_string;
    unsigned    length;
    int         l;

    /*
     * First count the number of backslashes required.
     * Then allocate the memory and insert them.
     */
    length = 1;                         /* count the trailing NUL */
    for (p = string; *p; p++)
    {
        if ((l = utfc_ptr2len(p)) > 1)
        {
            length += l;                /* count a multibyte char */
            p += l - 1;
            continue;
        }
        if (vim_strchr(esc_chars, *p) != NULL || (bsl && rem_backslash(p)))
            ++length;                   /* count a backslash */
        ++length;                       /* count an ordinary char */
    }
    escaped_string = alloc(length);
    if (escaped_string != NULL)
    {
        p2 = escaped_string;
        for (p = string; *p; p++)
        {
            if ((l = utfc_ptr2len(p)) > 1)
            {
                mch_memmove(p2, p, (size_t)l);
                p2 += l;
                p += l - 1;             /* skip multibyte char */
                continue;
            }
            if (vim_strchr(esc_chars, *p) != NULL || (bsl && rem_backslash(p)))
                *p2++ = cc;
            *p2++ = *p;
        }
        *p2 = NUL;
    }
    return escaped_string;
}

/*
 * Return TRUE when 'shell' has "csh" in the tail.
 */
    int
csh_like_shell()
{
    return (strstr((char *)gettail(p_sh), "csh") != NULL);
}

/*
 * Escape "string" for use as a shell argument with system().
 * This uses single quotes, except when we know we need to use double quotes
 * (MS-DOS and MS-Windows without 'shellslash' set).
 * Escape a newline, depending on the 'shell' option.
 * When "do_special" is TRUE also replace "!", "%", "#" and things starting
 * with "<" like "<cfile>".
 * When "do_newline" is FALSE do not escape newline unless it is csh shell.
 * Returns the result in allocated memory, NULL if we have run out.
 */
    char_u *
vim_strsave_shellescape(string, do_special, do_newline)
    char_u      *string;
    int         do_special;
    int         do_newline;
{
    unsigned    length;
    char_u      *p;
    char_u      *d;
    char_u      *escaped_string;
    int         l;
    int         csh_like;

    /* Only csh and similar shells expand '!' within single quotes.  For sh and
     * the like we must not put a backslash before it, it will be taken
     * literally.  If do_special is set the '!' will be escaped twice.
     * Csh also needs to have "\n" escaped twice when do_special is set. */
    csh_like = csh_like_shell();

    /* First count the number of extra bytes required. */
    length = (unsigned)STRLEN(string) + 3;  /* two quotes and a trailing NUL */
    for (p = string; *p != NUL; p += utfc_ptr2len(p))
    {
        if (*p == '\'')
            length += 3;                /* ' => '\'' */
        if ((*p == '\n' && (csh_like || do_newline)) || (*p == '!' && (csh_like || do_special)))
        {
            ++length;                   /* insert backslash */
            if (csh_like && do_special)
                ++length;               /* insert backslash */
        }
        if (do_special && find_cmdline_var(p, &l) >= 0)
        {
            ++length;                   /* insert backslash */
            p += l - 1;
        }
    }

    /* Allocate memory for the result and fill it. */
    escaped_string = alloc(length);
    if (escaped_string != NULL)
    {
        d = escaped_string;

        /* add opening quote */
            *d++ = '\'';

        for (p = string; *p != NUL; )
        {
            if (*p == '\'')
            {
                *d++ = '\'';
                *d++ = '\\';
                *d++ = '\'';
                *d++ = '\'';
                ++p;
                continue;
            }
            if ((*p == '\n' && (csh_like || do_newline)) || (*p == '!' && (csh_like || do_special)))
            {
                *d++ = '\\';
                if (csh_like && do_special)
                    *d++ = '\\';
                *d++ = *p++;
                continue;
            }
            if (do_special && find_cmdline_var(p, &l) >= 0)
            {
                *d++ = '\\';            /* insert backslash */
                while (--l >= 0)        /* copy the var */
                    *d++ = *p++;
                continue;
            }

            mb_copy_char(&p, &d);
        }

        /* add terminating quote and finish with a NUL */
            *d++ = '\'';
        *d = NUL;
    }

    return escaped_string;
}

/*
 * Like vim_strsave(), but make all characters uppercase.
 * This uses ASCII lower-to-upper case translation, language independent.
 */
    char_u *
vim_strsave_up(string)
    char_u      *string;
{
    char_u *p1;

    p1 = vim_strsave(string);
    vim_strup(p1);
    return p1;
}

/*
 * Like vim_strnsave(), but make all characters uppercase.
 * This uses ASCII lower-to-upper case translation, language independent.
 */
    char_u *
vim_strnsave_up(string, len)
    char_u      *string;
    int         len;
{
    char_u *p1;

    p1 = vim_strnsave(string, len);
    vim_strup(p1);
    return p1;
}

/*
 * ASCII lower-to-upper case translation, language independent.
 */
    void
vim_strup(p)
    char_u      *p;
{
    char_u  *p2;
    int     c;

    if (p != NULL)
    {
        p2 = p;
        while ((c = *p2) != NUL)
            *p2++ = (c < 'a' || c > 'z') ? c : (c - 0x20);
    }
}

/*
 * Make string "s" all upper-case and return it in allocated memory.
 * Handles multi-byte characters as well as possible.
 * Returns NULL when out of memory.
 */
    char_u *
strup_save(orig)
    char_u      *orig;
{
    char_u      *p;
    char_u      *res;

    res = p = vim_strsave(orig);

    if (res != NULL)
        while (*p != NUL)
        {
            int     c, uc;
            int     l, newl;

            c = utf_ptr2char(p);
            uc = utf_toupper(c);

            /* Reallocate string when byte count changes.  This is rare,
             * thus it's OK to do another malloc()/free(). */
            l = utf_ptr2len(p);
            newl = utf_char2len(uc);
            if (newl != l)
            {
                char_u  *s;

                s = alloc((unsigned)STRLEN(res) + 1 + newl - l);
                if (s == NULL)
                    break;

                mch_memmove(s, res, p - res);
                STRCPY(s + (p - res) + newl, p + l);
                p = s + (p - res);
                vim_free(res);
                res = s;
            }

            utf_char2bytes(uc, p);
            p += newl;
        }

    return res;
}

/*
 * copy a space a number of times
 */
    void
copy_spaces(ptr, count)
    char_u      *ptr;
    size_t      count;
{
    size_t      i = count;
    char_u      *p = ptr;

    while (i--)
        *p++ = ' ';
}

/*
 * Copy a character a number of times.
 * Does not work for multi-byte characters!
 */
    void
copy_chars(ptr, count, c)
    char_u      *ptr;
    size_t      count;
    int         c;
{
    size_t      i = count;
    char_u      *p = ptr;

    while (i--)
        *p++ = c;
}

/*
 * delete spaces at the end of a string
 */
    void
del_trailing_spaces(ptr)
    char_u      *ptr;
{
    char_u      *q;

    q = ptr + STRLEN(ptr);
    while (--q > ptr && vim_iswhite(q[0]) && q[-1] != '\\' && q[-1] != Ctrl_V)
        *q = NUL;
}

/*
 * Like strncpy(), but always terminate the result with one NUL.
 * "to" must be "len + 1" long!
 */
    void
vim_strncpy(to, from, len)
    char_u      *to;
    char_u      *from;
    size_t      len;
{
    STRNCPY(to, from, len);
    to[len] = NUL;
}

/*
 * Like strcat(), but make sure the result fits in "tosize" bytes and is
 * always NUL terminated.
 */
    void
vim_strcat(to, from, tosize)
    char_u      *to;
    char_u      *from;
    size_t      tosize;
{
    size_t tolen = STRLEN(to);
    size_t fromlen = STRLEN(from);

    if (tolen + fromlen + 1 > tosize)
    {
        mch_memmove(to + tolen, from, tosize - tolen - 1);
        to[tosize - 1] = NUL;
    }
    else
        STRCPY(to + tolen, from);
}

/*
 * Isolate one part of a string option where parts are separated with "sep_chars".
 * The part is copied into "buf[maxlen]".
 * "*option" is advanced to the next part.
 * The length is returned.
 */
    int
copy_option_part(option, buf, maxlen, sep_chars)
    char_u      **option;
    char_u      *buf;
    int         maxlen;
    char        *sep_chars;
{
    int     len = 0;
    char_u  *p = *option;

    /* skip '.' at start of option part, for 'suffixes' */
    if (*p == '.')
        buf[len++] = *p++;
    while (*p != NUL && vim_strchr((char_u *)sep_chars, *p) == NULL)
    {
        /*
         * Skip backslash before a separator character and space.
         */
        if (p[0] == '\\' && vim_strchr((char_u *)sep_chars, p[1]) != NULL)
            ++p;
        if (len < maxlen - 1)
            buf[len++] = *p;
        ++p;
    }
    buf[len] = NUL;

    if (*p != NUL && *p != ',') /* skip non-standard separator */
        ++p;
    p = skip_to_option_part(p); /* p points to next file name */

    *option = p;
    return len;
}

/*
 * Replacement for free() that ignores NULL pointers.
 * Also skip free() when exiting for sure, this helps when we caught a deadly
 * signal that was caused by a crash in free().
 */
    void
vim_free(x)
    void *x;
{
    if (x != NULL && !really_exiting)
    {
        free(x);
    }
}

#if defined(VIM_MEMCMP)
/*
 * Return zero when "b1" and "b2" are the same for "len" bytes.
 * Return non-zero otherwise.
 */
    int
vim_memcmp(b1, b2, len)
    void    *b1;
    void    *b2;
    size_t  len;
{
    char_u  *p1 = (char_u *)b1, *p2 = (char_u *)b2;

    for ( ; len > 0; --len)
    {
        if (*p1 != *p2)
            return 1;
        ++p1;
        ++p2;
    }
    return 0;
}
#endif

/*
 * Version of strchr() and strrchr() that handle unsigned char strings
 * with characters from 128 to 255 correctly.  It also doesn't return a
 * pointer to the NUL at the end of the string.
 */
    char_u  *
vim_strchr(string, c)
    char_u      *string;
    int         c;
{
    char_u      *p;
    int         b;

    p = string;
    if (c >= 0x80)
    {
        while (*p != NUL)
        {
            if (utf_ptr2char(p) == c)
                return p;
            p += utfc_ptr2len(p);
        }
        return NULL;
    }

    while ((b = *p) != NUL)
    {
        if (b == c)
            return p;
        p += utfc_ptr2len(p);
    }
    return NULL;
}

/*
 * Version of strchr() that only works for bytes and handles unsigned char
 * strings with characters above 128 correctly. It also doesn't return a
 * pointer to the NUL at the end of the string.
 */
    char_u  *
vim_strbyte(string, c)
    char_u      *string;
    int         c;
{
    char_u      *p = string;

    while (*p != NUL)
    {
        if (*p == c)
            return p;
        ++p;
    }
    return NULL;
}

/*
 * Search for last occurrence of "c" in "string".
 * Return NULL if not found.
 * Does not handle multi-byte char for "c"!
 */
    char_u  *
vim_strrchr(string, c)
    char_u      *string;
    int         c;
{
    char_u      *retval = NULL;
    char_u      *p = string;

    while (*p)
    {
        if (*p == c)
            retval = p;
        p += utfc_ptr2len(p);
    }
    return retval;
}

/*
 * Vim has its own isspace() function, because on some machines isspace()
 * can't handle characters above 128.
 */
    int
vim_isspace(x)
    int     x;
{
    return ((x >= 9 && x <= 13) || x == ' ');
}

/************************************************************************
 * Functions for handling growing arrays.
 */

/*
 * Clear an allocated growing array.
 */
    void
ga_clear(gap)
    garray_T *gap;
{
    vim_free(gap->ga_data);
    ga_init(gap);
}

/*
 * Clear a growing array that contains a list of strings.
 */
    void
ga_clear_strings(gap)
    garray_T *gap;
{
    int         i;

    for (i = 0; i < gap->ga_len; ++i)
        vim_free(((char_u **)(gap->ga_data))[i]);
    ga_clear(gap);
}

/*
 * Initialize a growing array.  Don't forget to set ga_itemsize and
 * ga_growsize!  Or use ga_init2().
 */
    void
ga_init(gap)
    garray_T *gap;
{
    gap->ga_data = NULL;
    gap->ga_maxlen = 0;
    gap->ga_len = 0;
}

    void
ga_init2(gap, itemsize, growsize)
    garray_T    *gap;
    int         itemsize;
    int         growsize;
{
    ga_init(gap);
    gap->ga_itemsize = itemsize;
    gap->ga_growsize = growsize;
}

/*
 * Make room in growing array "gap" for at least "n" items.
 * Return FAIL for failure, OK otherwise.
 */
    int
ga_grow(gap, n)
    garray_T    *gap;
    int         n;
{
    size_t      old_len;
    size_t      new_len;
    char_u      *pp;

    if (gap->ga_maxlen - gap->ga_len < n)
    {
        if (n < gap->ga_growsize)
            n = gap->ga_growsize;
        new_len = gap->ga_itemsize * (gap->ga_len + n);
        pp = (gap->ga_data == NULL) ? alloc((unsigned)new_len) : realloc(gap->ga_data, new_len);
        if (pp == NULL)
            return FAIL;
        old_len = gap->ga_itemsize * gap->ga_maxlen;
        vim_memset(pp + old_len, 0, new_len - old_len);
        gap->ga_maxlen = gap->ga_len + n;
        gap->ga_data = pp;
    }
    return OK;
}

/*
 * For a growing array that contains a list of strings: concatenate all the
 * strings with a separating "sep".
 * Returns NULL when out of memory.
 */
    char_u *
ga_concat_strings(gap, sep)
    garray_T *gap;
    char     *sep;
{
    int         i;
    int         len = 0;
    int         sep_len = (int)STRLEN(sep);
    char_u      *s;
    char_u      *p;

    for (i = 0; i < gap->ga_len; ++i)
        len += (int)STRLEN(((char_u **)(gap->ga_data))[i]) + sep_len;

    s = alloc(len + 1);
    if (s != NULL)
    {
        *s = NUL;
        p = s;
        for (i = 0; i < gap->ga_len; ++i)
        {
            if (p != s)
            {
                STRCPY(p, sep);
                p += sep_len;
            }
            STRCPY(p, ((char_u **)(gap->ga_data))[i]);
            p += STRLEN(p);
        }
    }
    return s;
}

/*
 * Concatenate a string to a growarray which contains characters.
 * Note: Does NOT copy the NUL at the end!
 */
    void
ga_concat(gap, s)
    garray_T    *gap;
    char_u      *s;
{
    int    len = (int)STRLEN(s);

    if (ga_grow(gap, len) == OK)
    {
        mch_memmove((char *)gap->ga_data + gap->ga_len, s, (size_t)len);
        gap->ga_len += len;
    }
}

/*
 * Append one byte to a growarray which contains bytes.
 */
    void
ga_append(gap, c)
    garray_T    *gap;
    int         c;
{
    if (ga_grow(gap, 1) == OK)
    {
        *((char *)gap->ga_data + gap->ga_len) = c;
        ++gap->ga_len;
    }
}

/*
 * Append the text in "gap" below the cursor line and clear "gap".
 */
    void
append_ga_line(gap)
    garray_T    *gap;
{
    /* Remove trailing CR. */
    if (gap->ga_len > 0
            && !curbuf->b_p_bin
            && ((char_u *)gap->ga_data)[gap->ga_len - 1] == CAR)
        --gap->ga_len;
    ga_append(gap, NUL);
    ml_append(curwin->w_cursor.lnum++, gap->ga_data, 0, FALSE);
    gap->ga_len = 0;
}

/************************************************************************
 * functions that use lookup tables for various things, generally to do with
 * special key codes.
 */

/*
 * Some useful tables.
 */

static struct modmasktable
{
    short       mod_mask;       /* Bit-mask for particular key modifier */
    short       mod_flag;       /* Bit(s) for particular key modifier */
    char_u      name;           /* Single letter name of modifier */
} mod_mask_table[] =
{
    {MOD_MASK_ALT,              MOD_MASK_ALT,           (char_u)'M'},
    {MOD_MASK_META,             MOD_MASK_META,          (char_u)'T'},
    {MOD_MASK_CTRL,             MOD_MASK_CTRL,          (char_u)'C'},
    {MOD_MASK_SHIFT,            MOD_MASK_SHIFT,         (char_u)'S'},
    {MOD_MASK_MULTI_CLICK,      MOD_MASK_2CLICK,        (char_u)'2'},
    {MOD_MASK_MULTI_CLICK,      MOD_MASK_3CLICK,        (char_u)'3'},
    {MOD_MASK_MULTI_CLICK,      MOD_MASK_4CLICK,        (char_u)'4'},
    /* 'A' must be the last one */
    {MOD_MASK_ALT,              MOD_MASK_ALT,           (char_u)'A'},
    {0, 0, NUL}
};

/*
 * Shifted key terminal codes and their unshifted equivalent.
 * Don't add mouse codes here, they are handled separately!
 */
#define MOD_KEYS_ENTRY_SIZE 5

static char_u modifier_keys_table[] =
{
/*  mod mask        with modifier               without modifier */
    MOD_MASK_SHIFT, '&', '9',                   '@', '1',       /* begin */
    MOD_MASK_SHIFT, '&', '0',                   '@', '2',       /* cancel */
    MOD_MASK_SHIFT, '*', '1',                   '@', '4',       /* command */
    MOD_MASK_SHIFT, '*', '2',                   '@', '5',       /* copy */
    MOD_MASK_SHIFT, '*', '3',                   '@', '6',       /* create */
    MOD_MASK_SHIFT, '*', '4',                   'k', 'D',       /* delete char */
    MOD_MASK_SHIFT, '*', '5',                   'k', 'L',       /* delete line */
    MOD_MASK_SHIFT, '*', '7',                   '@', '7',       /* end */
    MOD_MASK_CTRL,  KS_EXTRA, (int)KE_C_END,    '@', '7',       /* end */
    MOD_MASK_SHIFT, '*', '9',                   '@', '9',       /* exit */
    MOD_MASK_SHIFT, '*', '0',                   '@', '0',       /* find */
    MOD_MASK_SHIFT, '#', '1',                   '%', '1',       /* help */
    MOD_MASK_SHIFT, '#', '2',                   'k', 'h',       /* home */
    MOD_MASK_CTRL,  KS_EXTRA, (int)KE_C_HOME,   'k', 'h',       /* home */
    MOD_MASK_SHIFT, '#', '3',                   'k', 'I',       /* insert */
    MOD_MASK_SHIFT, '#', '4',                   'k', 'l',       /* left arrow */
    MOD_MASK_CTRL,  KS_EXTRA, (int)KE_C_LEFT,   'k', 'l',       /* left arrow */
    MOD_MASK_SHIFT, '%', 'a',                   '%', '3',       /* message */
    MOD_MASK_SHIFT, '%', 'b',                   '%', '4',       /* move */
    MOD_MASK_SHIFT, '%', 'c',                   '%', '5',       /* next */
    MOD_MASK_SHIFT, '%', 'd',                   '%', '7',       /* options */
    MOD_MASK_SHIFT, '%', 'e',                   '%', '8',       /* previous */
    MOD_MASK_SHIFT, '%', 'f',                   '%', '9',       /* print */
    MOD_MASK_SHIFT, '%', 'g',                   '%', '0',       /* redo */
    MOD_MASK_SHIFT, '%', 'h',                   '&', '3',       /* replace */
    MOD_MASK_SHIFT, '%', 'i',                   'k', 'r',       /* right arr. */
    MOD_MASK_CTRL,  KS_EXTRA, (int)KE_C_RIGHT,  'k', 'r',       /* right arr. */
    MOD_MASK_SHIFT, '%', 'j',                   '&', '5',       /* resume */
    MOD_MASK_SHIFT, '!', '1',                   '&', '6',       /* save */
    MOD_MASK_SHIFT, '!', '2',                   '&', '7',       /* suspend */
    MOD_MASK_SHIFT, '!', '3',                   '&', '8',       /* undo */
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_UP,     'k', 'u',       /* up arrow */
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_DOWN,   'k', 'd',       /* down arrow */

                                                                /* vt100 F1 */
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_XF1,    KS_EXTRA, (int)KE_XF1,
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_XF2,    KS_EXTRA, (int)KE_XF2,
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_XF3,    KS_EXTRA, (int)KE_XF3,
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_XF4,    KS_EXTRA, (int)KE_XF4,

    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F1,     'k', '1',       /* F1 */
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F2,     'k', '2',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F3,     'k', '3',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F4,     'k', '4',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F5,     'k', '5',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F6,     'k', '6',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F7,     'k', '7',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F8,     'k', '8',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F9,     'k', '9',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F10,    'k', ';',       /* F10 */

    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F11,    'F', '1',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F12,    'F', '2',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F13,    'F', '3',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F14,    'F', '4',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F15,    'F', '5',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F16,    'F', '6',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F17,    'F', '7',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F18,    'F', '8',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F19,    'F', '9',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F20,    'F', 'A',

    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F21,    'F', 'B',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F22,    'F', 'C',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F23,    'F', 'D',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F24,    'F', 'E',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F25,    'F', 'F',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F26,    'F', 'G',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F27,    'F', 'H',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F28,    'F', 'I',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F29,    'F', 'J',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F30,    'F', 'K',

    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F31,    'F', 'L',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F32,    'F', 'M',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F33,    'F', 'N',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F34,    'F', 'O',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F35,    'F', 'P',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F36,    'F', 'Q',
    MOD_MASK_SHIFT, KS_EXTRA, (int)KE_S_F37,    'F', 'R',

                                                            /* TAB pseudo code */
    MOD_MASK_SHIFT, 'k', 'B',                   KS_EXTRA, (int)KE_TAB,

    NUL
};

static struct key_name_entry
{
    int     key;        /* Special key code or ascii value */
    char_u  *name;      /* Name of key */
} key_names_table[] =
{
    {' ',               (char_u *)"Space"},
    {TAB,               (char_u *)"Tab"},
    {K_TAB,             (char_u *)"Tab"},
    {NL,                (char_u *)"NL"},
    {NL,                (char_u *)"NewLine"},   /* Alternative name */
    {NL,                (char_u *)"LineFeed"},  /* Alternative name */
    {NL,                (char_u *)"LF"},        /* Alternative name */
    {CAR,               (char_u *)"CR"},
    {CAR,               (char_u *)"Return"},    /* Alternative name */
    {CAR,               (char_u *)"Enter"},     /* Alternative name */
    {K_BS,              (char_u *)"BS"},
    {K_BS,              (char_u *)"BackSpace"}, /* Alternative name */
    {ESC,               (char_u *)"Esc"},
    {CSI,               (char_u *)"CSI"},
    {K_CSI,             (char_u *)"xCSI"},
    {'|',               (char_u *)"Bar"},
    {'\\',              (char_u *)"Bslash"},
    {K_DEL,             (char_u *)"Del"},
    {K_DEL,             (char_u *)"Delete"},    /* Alternative name */
    {K_KDEL,            (char_u *)"kDel"},
    {K_UP,              (char_u *)"Up"},
    {K_DOWN,            (char_u *)"Down"},
    {K_LEFT,            (char_u *)"Left"},
    {K_RIGHT,           (char_u *)"Right"},
    {K_XUP,             (char_u *)"xUp"},
    {K_XDOWN,           (char_u *)"xDown"},
    {K_XLEFT,           (char_u *)"xLeft"},
    {K_XRIGHT,          (char_u *)"xRight"},

    {K_F1,              (char_u *)"F1"},
    {K_F2,              (char_u *)"F2"},
    {K_F3,              (char_u *)"F3"},
    {K_F4,              (char_u *)"F4"},
    {K_F5,              (char_u *)"F5"},
    {K_F6,              (char_u *)"F6"},
    {K_F7,              (char_u *)"F7"},
    {K_F8,              (char_u *)"F8"},
    {K_F9,              (char_u *)"F9"},
    {K_F10,             (char_u *)"F10"},

    {K_F11,             (char_u *)"F11"},
    {K_F12,             (char_u *)"F12"},
    {K_F13,             (char_u *)"F13"},
    {K_F14,             (char_u *)"F14"},
    {K_F15,             (char_u *)"F15"},
    {K_F16,             (char_u *)"F16"},
    {K_F17,             (char_u *)"F17"},
    {K_F18,             (char_u *)"F18"},
    {K_F19,             (char_u *)"F19"},
    {K_F20,             (char_u *)"F20"},

    {K_F21,             (char_u *)"F21"},
    {K_F22,             (char_u *)"F22"},
    {K_F23,             (char_u *)"F23"},
    {K_F24,             (char_u *)"F24"},
    {K_F25,             (char_u *)"F25"},
    {K_F26,             (char_u *)"F26"},
    {K_F27,             (char_u *)"F27"},
    {K_F28,             (char_u *)"F28"},
    {K_F29,             (char_u *)"F29"},
    {K_F30,             (char_u *)"F30"},

    {K_F31,             (char_u *)"F31"},
    {K_F32,             (char_u *)"F32"},
    {K_F33,             (char_u *)"F33"},
    {K_F34,             (char_u *)"F34"},
    {K_F35,             (char_u *)"F35"},
    {K_F36,             (char_u *)"F36"},
    {K_F37,             (char_u *)"F37"},

    {K_XF1,             (char_u *)"xF1"},
    {K_XF2,             (char_u *)"xF2"},
    {K_XF3,             (char_u *)"xF3"},
    {K_XF4,             (char_u *)"xF4"},

    {K_HELP,            (char_u *)"Help"},
    {K_UNDO,            (char_u *)"Undo"},
    {K_INS,             (char_u *)"Insert"},
    {K_INS,             (char_u *)"Ins"},       /* Alternative name */
    {K_KINS,            (char_u *)"kInsert"},
    {K_HOME,            (char_u *)"Home"},
    {K_KHOME,           (char_u *)"kHome"},
    {K_XHOME,           (char_u *)"xHome"},
    {K_ZHOME,           (char_u *)"zHome"},
    {K_END,             (char_u *)"End"},
    {K_KEND,            (char_u *)"kEnd"},
    {K_XEND,            (char_u *)"xEnd"},
    {K_ZEND,            (char_u *)"zEnd"},
    {K_PAGEUP,          (char_u *)"PageUp"},
    {K_PAGEDOWN,        (char_u *)"PageDown"},
    {K_KPAGEUP,         (char_u *)"kPageUp"},
    {K_KPAGEDOWN,       (char_u *)"kPageDown"},

    {K_KPLUS,           (char_u *)"kPlus"},
    {K_KMINUS,          (char_u *)"kMinus"},
    {K_KDIVIDE,         (char_u *)"kDivide"},
    {K_KMULTIPLY,       (char_u *)"kMultiply"},
    {K_KENTER,          (char_u *)"kEnter"},
    {K_KPOINT,          (char_u *)"kPoint"},

    {K_K0,              (char_u *)"k0"},
    {K_K1,              (char_u *)"k1"},
    {K_K2,              (char_u *)"k2"},
    {K_K3,              (char_u *)"k3"},
    {K_K4,              (char_u *)"k4"},
    {K_K5,              (char_u *)"k5"},
    {K_K6,              (char_u *)"k6"},
    {K_K7,              (char_u *)"k7"},
    {K_K8,              (char_u *)"k8"},
    {K_K9,              (char_u *)"k9"},

    {'<',               (char_u *)"lt"},

    {K_MOUSE,           (char_u *)"Mouse"},
    {K_LEFTMOUSE,       (char_u *)"LeftMouse"},
    {K_LEFTMOUSE_NM,    (char_u *)"LeftMouseNM"},
    {K_LEFTDRAG,        (char_u *)"LeftDrag"},
    {K_LEFTRELEASE,     (char_u *)"LeftRelease"},
    {K_LEFTRELEASE_NM,  (char_u *)"LeftReleaseNM"},
    {K_MIDDLEMOUSE,     (char_u *)"MiddleMouse"},
    {K_MIDDLEDRAG,      (char_u *)"MiddleDrag"},
    {K_MIDDLERELEASE,   (char_u *)"MiddleRelease"},
    {K_RIGHTMOUSE,      (char_u *)"RightMouse"},
    {K_RIGHTDRAG,       (char_u *)"RightDrag"},
    {K_RIGHTRELEASE,    (char_u *)"RightRelease"},
    {K_MOUSEDOWN,       (char_u *)"ScrollWheelUp"},
    {K_MOUSEUP,         (char_u *)"ScrollWheelDown"},
    {K_MOUSELEFT,       (char_u *)"ScrollWheelRight"},
    {K_MOUSERIGHT,      (char_u *)"ScrollWheelLeft"},
    {K_MOUSEDOWN,       (char_u *)"MouseDown"}, /* OBSOLETE: Use          */
    {K_MOUSEUP,         (char_u *)"MouseUp"},   /* ScrollWheelXXX instead */
    {K_X1MOUSE,         (char_u *)"X1Mouse"},
    {K_X1DRAG,          (char_u *)"X1Drag"},
    {K_X1RELEASE,               (char_u *)"X1Release"},
    {K_X2MOUSE,         (char_u *)"X2Mouse"},
    {K_X2DRAG,          (char_u *)"X2Drag"},
    {K_X2RELEASE,               (char_u *)"X2Release"},
    {K_DROP,            (char_u *)"Drop"},
    {K_ZERO,            (char_u *)"Nul"},
    {K_SNR,             (char_u *)"SNR"},
    {K_PLUG,            (char_u *)"Plug"},
    {K_CURSORHOLD,      (char_u *)"CursorHold"},
    {0,                 NULL}
};

#define KEY_NAMES_TABLE_LEN (sizeof(key_names_table) / sizeof(struct key_name_entry))

static struct mousetable
{
    int     pseudo_code;        /* Code for pseudo mouse event */
    int     button;             /* Which mouse button is it? */
    int     is_click;           /* Is it a mouse button click event? */
    int     is_drag;            /* Is it a mouse drag event? */
} mouse_table[] =
{
    {(int)KE_LEFTMOUSE,         MOUSE_LEFT,     TRUE,   FALSE},
    {(int)KE_LEFTDRAG,          MOUSE_LEFT,     FALSE,  TRUE},
    {(int)KE_LEFTRELEASE,       MOUSE_LEFT,     FALSE,  FALSE},
    {(int)KE_MIDDLEMOUSE,       MOUSE_MIDDLE,   TRUE,   FALSE},
    {(int)KE_MIDDLEDRAG,        MOUSE_MIDDLE,   FALSE,  TRUE},
    {(int)KE_MIDDLERELEASE,     MOUSE_MIDDLE,   FALSE,  FALSE},
    {(int)KE_RIGHTMOUSE,        MOUSE_RIGHT,    TRUE,   FALSE},
    {(int)KE_RIGHTDRAG,         MOUSE_RIGHT,    FALSE,  TRUE},
    {(int)KE_RIGHTRELEASE,      MOUSE_RIGHT,    FALSE,  FALSE},
    {(int)KE_X1MOUSE,           MOUSE_X1,       TRUE,   FALSE},
    {(int)KE_X1DRAG,            MOUSE_X1,       FALSE,  TRUE},
    {(int)KE_X1RELEASE,         MOUSE_X1,       FALSE,  FALSE},
    {(int)KE_X2MOUSE,           MOUSE_X2,       TRUE,   FALSE},
    {(int)KE_X2DRAG,            MOUSE_X2,       FALSE,  TRUE},
    {(int)KE_X2RELEASE,         MOUSE_X2,       FALSE,  FALSE},
    /* DRAG without CLICK */
    {(int)KE_IGNORE,            MOUSE_RELEASE,  FALSE,  TRUE},
    /* RELEASE without CLICK */
    {(int)KE_IGNORE,            MOUSE_RELEASE,  FALSE,  FALSE},
    {0,                         0,              0,      0},
};

/*
 * Return the modifier mask bit (MOD_MASK_*) which corresponds to the given
 * modifier name ('S' for Shift, 'C' for Ctrl etc).
 */
    int
name_to_mod_mask(c)
    int     c;
{
    int     i;

    c = TOUPPER_ASC(c);
    for (i = 0; mod_mask_table[i].mod_mask != 0; i++)
        if (c == mod_mask_table[i].name)
            return mod_mask_table[i].mod_flag;
    return 0;
}

/*
 * Check if if there is a special key code for "key" that includes the
 * modifiers specified.
 */
    int
simplify_key(key, modifiers)
    int     key;
    int     *modifiers;
{
    int     i;
    int     key0;
    int     key1;

    if (*modifiers & (MOD_MASK_SHIFT | MOD_MASK_CTRL | MOD_MASK_ALT))
    {
        /* TAB is a special case */
        if (key == TAB && (*modifiers & MOD_MASK_SHIFT))
        {
            *modifiers &= ~MOD_MASK_SHIFT;
            return K_S_TAB;
        }
        key0 = KEY2TERMCAP0(key);
        key1 = KEY2TERMCAP1(key);
        for (i = 0; modifier_keys_table[i] != NUL; i += MOD_KEYS_ENTRY_SIZE)
            if (key0 == modifier_keys_table[i + 3]
                    && key1 == modifier_keys_table[i + 4]
                    && (*modifiers & modifier_keys_table[i]))
            {
                *modifiers &= ~modifier_keys_table[i];
                return TERMCAP2KEY(modifier_keys_table[i + 1], modifier_keys_table[i + 2]);
            }
    }
    return key;
}

/*
 * Change <xHome> to <Home>, <xUp> to <Up>, etc.
 */
    int
handle_x_keys(key)
    int     key;
{
    switch (key)
    {
        case K_XUP:     return K_UP;
        case K_XDOWN:   return K_DOWN;
        case K_XLEFT:   return K_LEFT;
        case K_XRIGHT:  return K_RIGHT;
        case K_XHOME:   return K_HOME;
        case K_ZHOME:   return K_HOME;
        case K_XEND:    return K_END;
        case K_ZEND:    return K_END;
        case K_XF1:     return K_F1;
        case K_XF2:     return K_F2;
        case K_XF3:     return K_F3;
        case K_XF4:     return K_F4;
        case K_S_XF1:   return K_S_F1;
        case K_S_XF2:   return K_S_F2;
        case K_S_XF3:   return K_S_F3;
        case K_S_XF4:   return K_S_F4;
    }
    return key;
}

/*
 * Return a string which contains the name of the given key when the given
 * modifiers are down.
 */
    char_u *
get_special_key_name(c, modifiers)
    int     c;
    int     modifiers;
{
    static char_u string[MAX_KEY_NAME_LEN + 1];

    int     i, idx;
    int     table_idx;
    char_u  *s;

    string[0] = '<';
    idx = 1;

    /* Key that stands for a normal character. */
    if (IS_SPECIAL(c) && KEY2TERMCAP0(c) == KS_KEY)
        c = KEY2TERMCAP1(c);

    /*
     * Translate shifted special keys into unshifted keys and set modifier.
     * Same for CTRL and ALT modifiers.
     */
    if (IS_SPECIAL(c))
    {
        for (i = 0; modifier_keys_table[i] != 0; i += MOD_KEYS_ENTRY_SIZE)
            if (       KEY2TERMCAP0(c) == (int)modifier_keys_table[i + 1]
                    && (int)KEY2TERMCAP1(c) == (int)modifier_keys_table[i + 2])
            {
                modifiers |= modifier_keys_table[i];
                c = TERMCAP2KEY(modifier_keys_table[i + 3], modifier_keys_table[i + 4]);
                break;
            }
    }

    /* try to find the key in the special key table */
    table_idx = find_special_key_in_table(c);

    /*
     * When not a known special key, and not a printable character, try to extract modifiers.
     */
    if (c > 0 && utf_char2len(c) == 1)
    {
        if (table_idx < 0
                && (!vim_isprintc(c) || (c & 0x7f) == ' ')
                && (c & 0x80))
        {
            c &= 0x7f;
            modifiers |= MOD_MASK_ALT;
            /* try again, to find the un-alted key in the special key table */
            table_idx = find_special_key_in_table(c);
        }
        if (table_idx < 0 && !vim_isprintc(c) && c < ' ')
        {
            c += '@';
            modifiers |= MOD_MASK_CTRL;
        }
    }

    /* translate the modifier into a string */
    for (i = 0; mod_mask_table[i].name != 'A'; i++)
        if ((modifiers & mod_mask_table[i].mod_mask) == mod_mask_table[i].mod_flag)
        {
            string[idx++] = mod_mask_table[i].name;
            string[idx++] = (char_u)'-';
        }

    if (table_idx < 0)          /* unknown special key, may output t_xx */
    {
        if (IS_SPECIAL(c))
        {
            string[idx++] = 't';
            string[idx++] = '_';
            string[idx++] = KEY2TERMCAP0(c);
            string[idx++] = KEY2TERMCAP1(c);
        }
        /* Not a special key, only modifiers, output directly */
        else
        {
            if (utf_char2len(c) > 1)
                idx += utf_char2bytes(c, string + idx);
            else if (vim_isprintc(c))
                string[idx++] = c;
            else
            {
                s = transchar(c);
                while (*s)
                    string[idx++] = *s++;
            }
        }
    }
    else                /* use name of special key */
    {
        STRCPY(string + idx, key_names_table[table_idx].name);
        idx = (int)STRLEN(string);
    }
    string[idx++] = '>';
    string[idx] = NUL;
    return string;
}

/*
 * Try translating a <> name at (*srcp)[] to dst[].
 * Return the number of characters added to dst[], zero for no match.
 * If there is a match, srcp is advanced to after the <> name.
 * dst[] must be big enough to hold the result (up to six characters)!
 */
    int
trans_special(srcp, dst, keycode)
    char_u      **srcp;
    char_u      *dst;
    int         keycode; /* prefer key code, e.g. K_DEL instead of DEL */
{
    int         modifiers = 0;
    int         key;
    int         dlen = 0;

    key = find_special_key(srcp, &modifiers, keycode, FALSE);
    if (key == 0)
        return 0;

    /* Put the appropriate modifier in a string */
    if (modifiers != 0)
    {
        dst[dlen++] = K_SPECIAL;
        dst[dlen++] = KS_MODIFIER;
        dst[dlen++] = modifiers;
    }

    if (IS_SPECIAL(key))
    {
        dst[dlen++] = K_SPECIAL;
        dst[dlen++] = KEY2TERMCAP0(key);
        dst[dlen++] = KEY2TERMCAP1(key);
    }
    else if (!keycode)
        dlen += utf_char2bytes(key, dst + dlen);
    else if (keycode)
        dlen = (int)(add_char2buf(key, dst + dlen) - dst);
    else
        dst[dlen++] = key;

    return dlen;
}

/*
 * Try translating a <> name at (*srcp)[], return the key and modifiers.
 * srcp is advanced to after the <> name.
 * returns 0 if there is no match.
 */
    int
find_special_key(srcp, modp, keycode, keep_x_key)
    char_u      **srcp;
    int         *modp;
    int         keycode;     /* prefer key code, e.g. K_DEL instead of DEL */
    int         keep_x_key;  /* don't translate xHome to Home key */
{
    char_u      *last_dash;
    char_u      *end_of_name;
    char_u      *src;
    char_u      *bp;
    int         modifiers;
    int         bit;
    int         key;
    unsigned long n;
    int         l;

    src = *srcp;
    if (src[0] != '<')
        return 0;

    /* Find end of modifier list */
    last_dash = src;
    for (bp = src + 1; *bp == '-' || vim_isIDc(*bp); bp++)
    {
        if (*bp == '-')
        {
            last_dash = bp;
            if (bp[1] != NUL)
            {
                l = utfc_ptr2len(bp + 1);
                if (bp[l + 1] == '>')
                    bp += l;    /* anything accepted, like <C-?> */
            }
        }
        if (bp[0] == 't' && bp[1] == '_' && bp[2] && bp[3])
            bp += 3;    /* skip t_xx, xx may be '-' or '>' */
        else if (STRNICMP(bp, "char-", 5) == 0)
        {
            vim_str2nr(bp + 5, NULL, &l, TRUE, TRUE, NULL, NULL);
            bp += l + 5;
            break;
        }
    }

    if (*bp == '>')     /* found matching '>' */
    {
        end_of_name = bp + 1;

        /* Which modifiers are given? */
        modifiers = 0x0;
        for (bp = src + 1; bp < last_dash; bp++)
        {
            if (*bp != '-')
            {
                bit = name_to_mod_mask(*bp);
                if (bit == 0x0)
                    break;      /* Illegal modifier name */
                modifiers |= bit;
            }
        }

        /*
         * Legal modifier name.
         */
        if (bp >= last_dash)
        {
            if (STRNICMP(last_dash + 1, "char-", 5) == 0 && VIM_ISDIGIT(last_dash[6]))
            {
                /* <Char-123> or <Char-033> or <Char-0x33> */
                vim_str2nr(last_dash + 6, NULL, NULL, TRUE, TRUE, NULL, &n);
                key = (int)n;
            }
            else
            {
                /*
                 * Modifier with single letter, or special key name.
                 */
                l = utfc_ptr2len(last_dash + 1);
                if (modifiers != 0 && last_dash[l + 1] == '>')
                    key = utf_ptr2char(last_dash + 1);
                else
                {
                    key = get_special_key_code(last_dash + 1);
                    if (!keep_x_key)
                        key = handle_x_keys(key);
                }
            }

            /*
             * get_special_key_code() may return NUL for invalid special key name.
             */
            if (key != NUL)
            {
                /*
                 * Only use a modifier when there is no special key code that
                 * includes the modifier.
                 */
                key = simplify_key(key, &modifiers);

                if (!keycode)
                {
                    /* don't want keycode, use single byte code */
                    if (key == K_BS)
                        key = BS;
                    else if (key == K_DEL || key == K_KDEL)
                        key = DEL;
                }

                /*
                 * Normal Key with modifier: Try to make a single byte code.
                 */
                if (!IS_SPECIAL(key))
                    key = extract_modifiers(key, &modifiers);

                *modp = modifiers;
                *srcp = end_of_name;
                return key;
            }
        }
    }
    return 0;
}

/*
 * Try to include modifiers in the key.
 * Changes "Shift-a" to 'A', "Alt-A" to 0xc0, etc.
 */
    int
extract_modifiers(key, modp)
    int     key;
    int     *modp;
{
    int modifiers = *modp;

    if ((modifiers & MOD_MASK_SHIFT) && ASCII_ISALPHA(key))
    {
        key = TOUPPER_ASC(key);
        modifiers &= ~MOD_MASK_SHIFT;
    }
    if ((modifiers & MOD_MASK_CTRL) && ((key >= '?' && key <= '_') || ASCII_ISALPHA(key)))
    {
        key = Ctrl_chr(key);
        modifiers &= ~MOD_MASK_CTRL;
        /* <C-@> is <Nul> */
        if (key == 0)
            key = K_ZERO;
    }
    if ((modifiers & MOD_MASK_ALT) && key < 0x80)    /* avoid creating a lead byte */
    {
        key |= 0x80;
        modifiers &= ~MOD_MASK_ALT;     /* remove the META modifier */
    }

    *modp = modifiers;
    return key;
}

/*
 * Try to find key "c" in the special key table.
 * Return the index when found, -1 when not found.
 */
    int
find_special_key_in_table(c)
    int     c;
{
    int     i;

    for (i = 0; key_names_table[i].name != NULL; i++)
        if (c == key_names_table[i].key)
            break;
    if (key_names_table[i].name == NULL)
        i = -1;
    return i;
}

/*
 * Find the special key with the given name (the given string does not have to
 * end with NUL, the name is assumed to end before the first non-idchar).
 * If the name starts with "t_" the next two characters are interpreted as a termcap name.
 * Return the key code, or 0 if not found.
 */
    int
get_special_key_code(name)
    char_u  *name;
{
    char_u  *table_name;
    char_u  string[3];
    int     i, j;

    /*
     * If it's <t_xx> we get the code for xx from the termcap
     */
    if (name[0] == 't' && name[1] == '_' && name[2] != NUL && name[3] != NUL)
    {
        string[0] = name[2];
        string[1] = name[3];
        string[2] = NUL;
        if (add_termcap_entry(string, FALSE) == OK)
            return TERMCAP2KEY(name[2], name[3]);
    }
    else
        for (i = 0; key_names_table[i].name != NULL; i++)
        {
            table_name = key_names_table[i].name;
            for (j = 0; vim_isIDc(name[j]) && table_name[j] != NUL; j++)
                if (TOLOWER_ASC(table_name[j]) != TOLOWER_ASC(name[j]))
                    break;
            if (!vim_isIDc(name[j]) && table_name[j] == NUL)
                return key_names_table[i].key;
        }
    return 0;
}

    char_u *
get_key_name(i)
    int     i;
{
    if (i >= (int)KEY_NAMES_TABLE_LEN)
        return NULL;
    return  key_names_table[i].name;
}

/*
 * Look up the given mouse code to return the relevant information in the other
 * arguments.  Return which button is down or was released.
 */
    int
get_mouse_button(code, is_click, is_drag)
    int     code;
    int     *is_click;
    int     *is_drag;
{
    int     i;

    for (i = 0; mouse_table[i].pseudo_code; i++)
        if (code == mouse_table[i].pseudo_code)
        {
            *is_click = mouse_table[i].is_click;
            *is_drag = mouse_table[i].is_drag;
            return mouse_table[i].button;
        }
    return 0;       /* Shouldn't get here */
}

/*
 * Return the appropriate pseudo mouse event token (KE_LEFTMOUSE etc) based on
 * the given information about which mouse button is down, and whether the
 * mouse was clicked, dragged or released.
 */
    int
get_pseudo_mouse_code(button, is_click, is_drag)
    int     button;     /* eg MOUSE_LEFT */
    int     is_click;
    int     is_drag;
{
    int     i;

    for (i = 0; mouse_table[i].pseudo_code; i++)
        if (button == mouse_table[i].button
            && is_click == mouse_table[i].is_click
            && is_drag == mouse_table[i].is_drag)
        {
            return mouse_table[i].pseudo_code;
        }
    return (int)KE_IGNORE;          /* not recognized, ignore it */
}

/*
 * Return the current end-of-line type: EOL_DOS, EOL_UNIX or EOL_MAC.
 */
    int
get_fileformat(buf)
    buf_T       *buf;
{
    int         c = *buf->b_p_ff;

    if (buf->b_p_bin || c == 'u')
        return EOL_UNIX;
    if (c == 'm')
        return EOL_MAC;
    return EOL_DOS;
}

/*
 * Like get_fileformat(), but override 'fileformat' with "p" for "++opt=val" argument.
 */
    int
get_fileformat_force(buf, eap)
    buf_T       *buf;
    exarg_T     *eap;       /* can be NULL! */
{
    int         c;

    if (eap != NULL && eap->force_ff != 0)
        c = eap->cmd[eap->force_ff];
    else
    {
        if ((eap != NULL && eap->force_bin != 0) ? (eap->force_bin == FORCE_BIN) : buf->b_p_bin)
            return EOL_UNIX;
        c = *buf->b_p_ff;
    }
    if (c == 'u')
        return EOL_UNIX;
    if (c == 'm')
        return EOL_MAC;
    return EOL_DOS;
}

/*
 * Set the current end-of-line type to EOL_DOS, EOL_UNIX or EOL_MAC.
 * Sets both 'textmode' and 'fileformat'.
 * Note: Does _not_ set global value of 'textmode'!
 */
    void
set_fileformat(t, opt_flags)
    int         t;
    int         opt_flags;      /* OPT_LOCAL and/or OPT_GLOBAL */
{
    char        *p = NULL;

    switch (t)
    {
    case EOL_DOS:
        p = FF_DOS;
        curbuf->b_p_tx = TRUE;
        break;
    case EOL_UNIX:
        p = FF_UNIX;
        curbuf->b_p_tx = FALSE;
        break;
    case EOL_MAC:
        p = FF_MAC;
        curbuf->b_p_tx = FALSE;
        break;
    }
    if (p != NULL)
        set_string_option_direct((char_u *)"ff", -1, (char_u *)p, OPT_FREE | opt_flags, 0);

    /* This may cause the buffer to become (un)modified. */
    check_status(curbuf);
    redraw_tabline = TRUE;
    need_maketitle = TRUE;          /* set window title later */
}

/*
 * Return the default fileformat from 'fileformats'.
 */
    int
default_fileformat()
{
    switch (*p_ffs)
    {
        case 'm':   return EOL_MAC;
        case 'd':   return EOL_DOS;
    }
    return EOL_UNIX;
}

/*
 * Call shell.  Calls mch_call_shell, with 'shellxquote' added.
 */
    int
call_shell(cmd, opt)
    char_u      *cmd;
    int         opt;
{
    char_u      *ncmd;
    int         retval;

    if (p_verbose > 3)
    {
        verbose_enter();
        smsg((char_u *)"Calling shell to execute: \"%s\"", cmd == NULL ? p_sh : cmd);
        out_char('\n');
        cursor_on();
        verbose_leave();
    }

    if (*p_sh == NUL)
    {
        EMSG((char *)e_shellempty);
        retval = -1;
    }
    else
    {
        if (cmd == NULL || *p_sxq == NUL)
            retval = mch_call_shell(cmd, opt);
        else
        {
            char_u *ecmd = cmd;

            if (*p_sxe != NUL && STRCMP(p_sxq, "(") == 0)
            {
                ecmd = vim_strsave_escaped_ext(cmd, p_sxe, '^', FALSE);
                if (ecmd == NULL)
                    ecmd = cmd;
            }
            ncmd = alloc((unsigned)(STRLEN(ecmd) + STRLEN(p_sxq) * 2 + 1));
            if (ncmd != NULL)
            {
                STRCPY(ncmd, p_sxq);
                STRCAT(ncmd, ecmd);
                /* When 'shellxquote' is ( append ).
                 * When 'shellxquote' is "( append )". */
                STRCAT(ncmd, STRCMP(p_sxq, "(") == 0 ? (char_u *)")"
                           : STRCMP(p_sxq, "\"(") == 0 ? (char_u *)")\""
                           : p_sxq);
                retval = mch_call_shell(ncmd, opt);
                vim_free(ncmd);
            }
            else
                retval = -1;
            if (ecmd != cmd)
                vim_free(ecmd);
        }
        /*
         * Check the window size, in case it changed while executing the external command.
         */
        shell_resized_check();
    }

    set_vim_var_nr(VV_SHELL_ERROR, (long)retval);

    return retval;
}

/*
 * VISUAL, SELECTMODE and OP_PENDING State are never set, they are equal to
 * NORMAL State with a condition.  This function returns the real State.
 */
    int
get_real_state()
{
    if (State & NORMAL)
    {
        if (VIsual_active)
        {
            if (VIsual_select)
                return SELECTMODE;
            return VISUAL;
        }
        else if (finish_op)
            return OP_PENDING;
    }
    return State;
}

/*
 * Return TRUE if "p" points to just after a path separator.
 * Takes care of multi-byte characters.
 * "b" must point to the start of the file name
 */
    int
after_pathsep(b, p)
    char_u      *b;
    char_u      *p;
{
    return p > b && vim_ispathsep(p[-1]) && utf_head_off(b, p - 1) == 0;
}

/*
 * Return TRUE if file names "f1" and "f2" are in the same directory.
 * "f1" may be a short name, "f2" must be a full path.
 */
    int
same_directory(f1, f2)
    char_u      *f1;
    char_u      *f2;
{
    char_u      ffname[MAXPATHL];
    char_u      *t1;
    char_u      *t2;

    /* safety check */
    if (f1 == NULL || f2 == NULL)
        return FALSE;

    (void)vim_FullName(f1, ffname, MAXPATHL, FALSE);
    t1 = gettail_sep(ffname);
    t2 = gettail_sep(f2);
    return (t1 - ffname == t2 - f2 && pathcmp((char *)ffname, (char *)f2, (int)(t1 - ffname)) == 0);
}

/*
 * Handling of cursor and mouse pointer shapes in various modes.
 */

cursorentry_T shape_table[SHAPE_IDX_COUNT] =
{
    /* The values will be filled in from the 'guicursor' and 'mouseshape'
     * defaults when Vim starts.
     * Adjust the SHAPE_IDX_ defines when making changes! */
    {0, 0, 0, 700L, 400L, 250L, 0, 0, "n", SHAPE_CURSOR+SHAPE_MOUSE},
    {0, 0, 0, 700L, 400L, 250L, 0, 0, "v", SHAPE_CURSOR+SHAPE_MOUSE},
    {0, 0, 0, 700L, 400L, 250L, 0, 0, "i", SHAPE_CURSOR+SHAPE_MOUSE},
    {0, 0, 0, 700L, 400L, 250L, 0, 0, "r", SHAPE_CURSOR+SHAPE_MOUSE},
    {0, 0, 0, 700L, 400L, 250L, 0, 0, "c", SHAPE_CURSOR+SHAPE_MOUSE},
    {0, 0, 0, 700L, 400L, 250L, 0, 0, "ci", SHAPE_CURSOR+SHAPE_MOUSE},
    {0, 0, 0, 700L, 400L, 250L, 0, 0, "cr", SHAPE_CURSOR+SHAPE_MOUSE},
    {0, 0, 0, 700L, 400L, 250L, 0, 0, "o", SHAPE_CURSOR+SHAPE_MOUSE},
    {0, 0, 0, 700L, 400L, 250L, 0, 0, "ve", SHAPE_CURSOR+SHAPE_MOUSE},
    {0, 0, 0,   0L,   0L,   0L, 0, 0, "e", SHAPE_MOUSE},
    {0, 0, 0,   0L,   0L,   0L, 0, 0, "s", SHAPE_MOUSE},
    {0, 0, 0,   0L,   0L,   0L, 0, 0, "sd", SHAPE_MOUSE},
    {0, 0, 0,   0L,   0L,   0L, 0, 0, "vs", SHAPE_MOUSE},
    {0, 0, 0,   0L,   0L,   0L, 0, 0, "vd", SHAPE_MOUSE},
    {0, 0, 0,   0L,   0L,   0L, 0, 0, "m", SHAPE_MOUSE},
    {0, 0, 0,   0L,   0L,   0L, 0, 0, "ml", SHAPE_MOUSE},
    {0, 0, 0, 100L, 100L, 100L, 0, 0, "sm", SHAPE_CURSOR},
};

/*
 * Parse the 'guicursor' option ("what" is SHAPE_CURSOR) or 'mouseshape'
 * ("what" is SHAPE_MOUSE).
 * Returns error message for an illegal option, NULL otherwise.
 */
    char_u *
parse_shape_opt(what)
    int         what;
{
    char_u      *modep;
    char_u      *colonp;
    char_u      *commap;
    char_u      *slashp;
    char_u      *p, *endp;
    int         idx = 0;                /* init for GCC */
    int         all_idx;
    int         len;
    int         i;
    long        n;
    int         found_ve = FALSE;       /* found "ve" flag */
    int         round;

    /*
     * First round: check for errors; second round: do it for real.
     */
    for (round = 1; round <= 2; ++round)
    {
        /*
         * Repeat for all comma separated parts.
         */
            modep = p_guicursor;
        while (*modep != NUL)
        {
            colonp = vim_strchr(modep, ':');
            if (colonp == NULL)
                return (char_u *)"E545: Missing colon";
            if (colonp == modep)
                return (char_u *)"E546: Illegal mode";
            commap = vim_strchr(modep, ',');

            /*
             * Repeat for all mode's before the colon.
             * For the 'a' mode, we loop to handle all the modes.
             */
            all_idx = -1;
            while (modep < colonp || all_idx >= 0)
            {
                if (all_idx < 0)
                {
                    /* Find the mode. */
                    if (modep[1] == '-' || modep[1] == ':')
                        len = 1;
                    else
                        len = 2;
                    if (len == 1 && TOLOWER_ASC(modep[0]) == 'a')
                        all_idx = SHAPE_IDX_COUNT - 1;
                    else
                    {
                        for (idx = 0; idx < SHAPE_IDX_COUNT; ++idx)
                            if (STRNICMP(modep, shape_table[idx].name, len) == 0)
                                break;
                        if (idx == SHAPE_IDX_COUNT || (shape_table[idx].used_for & what) == 0)
                            return (char_u *)"E546: Illegal mode";
                        if (len == 2 && modep[0] == 'v' && modep[1] == 'e')
                            found_ve = TRUE;
                    }
                    modep += len + 1;
                }

                if (all_idx >= 0)
                    idx = all_idx--;
                else if (round == 2)
                {
                    {
                        /* Set the defaults, for the missing parts */
                        shape_table[idx].shape = SHAPE_BLOCK;
                        shape_table[idx].blinkwait = 700L;
                        shape_table[idx].blinkon = 400L;
                        shape_table[idx].blinkoff = 250L;
                    }
                }

                /* Parse the part after the colon */
                for (p = colonp + 1; *p && *p != ','; )
                {
                    {
                        /*
                         * First handle the ones with a number argument.
                         */
                        i = *p;
                        len = 0;
                        if (STRNICMP(p, "ver", 3) == 0)
                            len = 3;
                        else if (STRNICMP(p, "hor", 3) == 0)
                            len = 3;
                        else if (STRNICMP(p, "blinkwait", 9) == 0)
                            len = 9;
                        else if (STRNICMP(p, "blinkon", 7) == 0)
                            len = 7;
                        else if (STRNICMP(p, "blinkoff", 8) == 0)
                            len = 8;
                        if (len != 0)
                        {
                            p += len;
                            if (!VIM_ISDIGIT(*p))
                                return (char_u *)"E548: digit expected";
                            n = getdigits(&p);
                            if (len == 3)   /* "ver" or "hor" */
                            {
                                if (n == 0)
                                    return (char_u *)"E549: Illegal percentage";
                                if (round == 2)
                                {
                                    if (TOLOWER_ASC(i) == 'v')
                                        shape_table[idx].shape = SHAPE_VER;
                                    else
                                        shape_table[idx].shape = SHAPE_HOR;
                                    shape_table[idx].percentage = n;
                                }
                            }
                            else if (round == 2)
                            {
                                if (len == 9)
                                    shape_table[idx].blinkwait = n;
                                else if (len == 7)
                                    shape_table[idx].blinkon = n;
                                else
                                    shape_table[idx].blinkoff = n;
                            }
                        }
                        else if (STRNICMP(p, "block", 5) == 0)
                        {
                            if (round == 2)
                                shape_table[idx].shape = SHAPE_BLOCK;
                            p += 5;
                        }
                        else    /* must be a highlight group name then */
                        {
                            endp = vim_strchr(p, '-');
                            if (commap == NULL)             /* last part */
                            {
                                if (endp == NULL)
                                    endp = p + STRLEN(p);   /* find end of part */
                            }
                            else if (endp > commap || endp == NULL)
                                endp = commap;
                            slashp = vim_strchr(p, '/');
                            if (slashp != NULL && slashp < endp)
                            {
                                /* "group/langmap_group" */
                                i = syn_check_group(p, (int)(slashp - p));
                                p = slashp + 1;
                            }
                            if (round == 2)
                            {
                                shape_table[idx].id = syn_check_group(p, (int)(endp - p));
                                shape_table[idx].id_lm = shape_table[idx].id;
                                if (slashp != NULL && slashp < endp)
                                    shape_table[idx].id = i;
                            }
                            p = endp;
                        }
                    }

                    if (*p == '-')
                        ++p;
                }
            }
            modep = p;
            if (*modep == ',')
                ++modep;
        }
    }

    /* If the 's' flag is not given, use the 'v' cursor for 's' */
    if (!found_ve)
    {
        {
            shape_table[SHAPE_IDX_VE].shape = shape_table[SHAPE_IDX_V].shape;
            shape_table[SHAPE_IDX_VE].percentage = shape_table[SHAPE_IDX_V].percentage;
            shape_table[SHAPE_IDX_VE].blinkwait = shape_table[SHAPE_IDX_V].blinkwait;
            shape_table[SHAPE_IDX_VE].blinkon = shape_table[SHAPE_IDX_V].blinkon;
            shape_table[SHAPE_IDX_VE].blinkoff = shape_table[SHAPE_IDX_V].blinkoff;
            shape_table[SHAPE_IDX_VE].id = shape_table[SHAPE_IDX_V].id;
            shape_table[SHAPE_IDX_VE].id_lm = shape_table[SHAPE_IDX_V].id_lm;
        }
    }

    return NULL;
}

/*
 * Change directory to "new_dir".  If FEAT_SEARCHPATH is defined, search
 * 'cdpath' for relative directory names, otherwise just mch_chdir().
 */
    int
vim_chdir(new_dir)
    char_u      *new_dir;
{
    return mch_chdir((char *)new_dir);
}

/*
 * Get user name from machine-specific function.
 * Returns the user name in "buf[len]".
 * Some systems are quite slow in obtaining the user name (Windows NT), thus cache the result.
 * Returns OK or FAIL.
 */
    int
get_user_name(buf, len)
    char_u      *buf;
    int         len;
{
    if (username == NULL)
    {
        if (mch_get_user_name(buf, len) == FAIL)
            return FAIL;
        username = vim_strsave(buf);
    }
    else
        vim_strncpy(buf, username, len - 1);
    return OK;
}

/*
 * Sort an array of strings.
 */
static int
sort_compare(const void *s1, const void *s2);

    static int
sort_compare(s1, s2)
    const void  *s1;
    const void  *s2;
{
    return STRCMP(*(char **)s1, *(char **)s2);
}

    void
sort_strings(files, count)
    char_u      **files;
    int         count;
{
    qsort((void *)files, (size_t)count, sizeof(char_u *), sort_compare);
}

/*
 * Compare path "p[]" to "q[]".
 * If "maxlen" >= 0 compare "p[maxlen]" to "q[maxlen]"
 * Return value like strcmp(p, q), but consider path separators.
 */
    int
pathcmp(p, q, maxlen)
    const char *p, *q;
    int maxlen;
{
    int         i;
    int         c1, c2;
    const char  *s = NULL;

    for (i = 0; maxlen < 0 || i < maxlen; i += utfc_ptr2len((char_u *)p + i))
    {
        c1 = utf_ptr2char((char_u *)p + i);
        c2 = utf_ptr2char((char_u *)q + i);

        /* End of "p": check if "q" also ends or just has a slash. */
        if (c1 == NUL)
        {
            if (c2 == NUL)  /* full match */
                return 0;
            s = q;
            break;
        }

        /* End of "q": check if "p" just has a slash. */
        if (c2 == NUL)
        {
            s = p;
            break;
        }

        if ((p_fic ? vim_toupper(c1) != vim_toupper(c2) : c1 != c2))
        {
            if (vim_ispathsep(c1))
                return -1;
            if (vim_ispathsep(c2))
                return 1;
            return p_fic ? vim_toupper(c1) - vim_toupper(c2)
                    : c1 - c2;  /* no match */
        }
    }
    if (s == NULL)      /* "i" ran into "maxlen" */
        return 0;

    c1 = utf_ptr2char((char_u *)s + i);
    c2 = utf_ptr2char((char_u *)s + i + utfc_ptr2len((char_u *)s + i));
    /* ignore a trailing slash, but not "//" or ":/" */
    if (c2 == NUL && i > 0 && !after_pathsep((char_u *)s, (char_u *)s + i) && c1 == '/')
        return 0;   /* match with trailing slash */
    if (s == q)
        return -1;          /* no match */
    return 1;
}

/*
 * The putenv() implementation below comes from the "screen" program.
 * Included with permission from Juergen Weigert.
 * See pty.c for the copyright notice.
 */

/*
 *  putenv  --  put value into environment
 *
 *  Usage:  i = putenv (string)
 *    int i;
 *    char  *string;
 *
 *  where string is of the form <name>=<value>.
 *  Putenv returns 0 normally, -1 on error (not enough core for malloc).
 *
 *  Putenv may need to add a new name into the environment, or to
 *  associate a value longer than the current value with a particular
 *  name.  So, to make life simpler, putenv() copies your entire
 *  environment into the heap (i.e. malloc()) from the stack
 *  (i.e. where it resides when your process is initiated) the first
 *  time you call it.
 *
 *  (history removed, not very interesting.  See the "screen" sources.)
 */

/*
 * Return 0 for not writable, 1 for writable file, 2 for a dir which we have
 * rights to write into.
 */
    int
filewritable(fname)
    char_u      *fname;
{
    int         retval = 0;
    int         perm = 0;

    perm = mch_getperm(fname);
    if ((perm & 0222) && mch_access((char *)fname, W_OK) == 0)
    {
        ++retval;
        if (mch_isdir(fname))
            ++retval;
    }
    return retval;
}

/*
 * Print an error message with one or two "%s" and one or two string arguments.
 * This is not in message.c to avoid a warning for prototypes.
 */
    int
emsg3(s, a1, a2)
    char_u *s, *a1, *a2;
{
    if (emsg_not_now())
        return TRUE;            /* no error messages at the moment */
    vim_snprintf((char *)IObuff, IOSIZE, (char *)s, a1, a2);
    return emsg(IObuff);
}

/*
 * Print an error message with one "%ld" and one long int argument.
 * This is not in message.c to avoid a warning for prototypes.
 */
    int
emsgn(s, n)
    char_u      *s;
    long        n;
{
    if (emsg_not_now())
        return TRUE;            /* no error messages at the moment */
    vim_snprintf((char *)IObuff, IOSIZE, (char *)s, n);
    return emsg(IObuff);
}

/*
 * Read 2 bytes from "fd" and turn them into an int, MSB first.
 */
    int
get2c(fd)
    FILE        *fd;
{
    int         n;

    n = getc(fd);
    n = (n << 8) + getc(fd);
    return n;
}

/*
 * Read 3 bytes from "fd" and turn them into an int, MSB first.
 */
    int
get3c(fd)
    FILE        *fd;
{
    int         n;

    n = getc(fd);
    n = (n << 8) + getc(fd);
    n = (n << 8) + getc(fd);
    return n;
}

/*
 * Read 4 bytes from "fd" and turn them into an int, MSB first.
 */
    int
get4c(fd)
    FILE        *fd;
{
    /* Use unsigned rather than int otherwise result is undefined
     * when left-shift sets the MSB. */
    unsigned    n;

    n = (unsigned)getc(fd);
    n = (n << 8) + (unsigned)getc(fd);
    n = (n << 8) + (unsigned)getc(fd);
    n = (n << 8) + (unsigned)getc(fd);
    return (int)n;
}

/*
 * Read 8 bytes from "fd" and turn them into a time_t, MSB first.
 */
    time_t
get8ctime(fd)
    FILE        *fd;
{
    time_t      n = 0;
    int         i;

    for (i = 0; i < 8; ++i)
        n = (n << 8) + getc(fd);
    return n;
}

/*
 * Read a string of length "cnt" from "fd" into allocated memory.
 * Returns NULL when out of memory or unable to read that many bytes.
 */
    char_u *
read_string(fd, cnt)
    FILE        *fd;
    int         cnt;
{
    char_u      *str;
    int         i;
    int         c;

    /* allocate memory */
    str = alloc((unsigned)cnt + 1);
    if (str != NULL)
    {
        /* Read the string.  Quit when running into the EOF. */
        for (i = 0; i < cnt; ++i)
        {
            c = getc(fd);
            if (c == EOF)
            {
                vim_free(str);
                return NULL;
            }
            str[i] = c;
        }
        str[i] = NUL;
    }
    return str;
}

/*
 * Write time_t to "buf[8]".
 */
    void
time_to_bytes(the_time, buf)
    time_t      the_time;
    char_u      *buf;
{
    int         c;
    int         i;
    int         bi = 0;
    time_t      wtime = the_time;

    /* time_t can be up to 8 bytes in size, more than long_u, thus we
     * can't use put_bytes() here.
     * Another problem is that ">>" may do an arithmetic shift that keeps the
     * sign.  This happens for large values of wtime.  A cast to long_u may
     * truncate if time_t is 8 bytes.  So only use a cast when it is 4 bytes,
     * it's safe to assume that long_u is 4 bytes or more and when using 8
     * bytes the top bit won't be set. */
    for (i = 7; i >= 0; --i)
    {
        if (i + 1 > (int)sizeof(time_t))
            /* ">>" doesn't work well when shifting more bits than avail */
            buf[bi++] = 0;
        else
        {
#if defined(SIZEOF_TIME_T) && SIZEOF_TIME_T > 4
            c = (int)(wtime >> (i * 8));
#else
            c = (int)((long_u)wtime >> (i * 8));
#endif
            buf[bi++] = c;
        }
    }
}
