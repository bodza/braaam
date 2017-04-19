/*
 * misc.c: functions that didn't seem to fit elsewhere
 */

#include "vim.h"

static char_u *vim_runtime_dir(char_u *vimdir);
static void init_users(void);
static int copy_indent(int size, char_u *src);

/* All user names (for ~user completion as done by shell). */
static garray_T ga_users;

/*
 * Count the size (in window cells) of the indent in the current line.
 */
    int
get_indent()
{
    return get_indent_str(ml_get_curline(), (int)curbuf->b_p_ts, FALSE);
}

/*
 * Count the size (in window cells) of the indent in line "lnum".
 */
    int
get_indent_lnum(lnum)
    linenr_T    lnum;
{
    return get_indent_str(ml_get(lnum), (int)curbuf->b_p_ts, FALSE);
}

/*
 * count the size (in window cells) of the indent in line "ptr", with 'tabstop' at "ts"
 */
    int
get_indent_str(ptr, ts, list)
    char_u      *ptr;
    int         ts;
    int         list; /* if TRUE, count only screen size for tabs */
{
    int         count = 0;

    for ( ; *ptr; ++ptr)
    {
        if (*ptr == TAB)
        {
            if (!list || lcs_tab1)    /* count a tab for what it is worth */
                count += ts - (count % ts);
            else
                /* In list mode, when tab is not set, count screen char width
                 * for Tab, displays: ^I */
                count += ptr2cells(ptr);
        }
        else if (*ptr == ' ')
            ++count;            /* count a space for one */
        else
            break;
    }
    return count;
}

/*
 * Set the indent of the current line.
 * Leaves the cursor on the first non-blank in the line.
 * Caller must take care of undo.
 * "flags":
 *      SIN_CHANGED:    call changed_bytes() if the line was changed.
 *      SIN_INSERT:     insert the indent in front of the line.
 *      SIN_UNDO:       save line for undo before changing it.
 * Returns TRUE if the line was changed.
 */
    int
set_indent(size, flags)
    int         size;               /* measured in spaces */
    int         flags;
{
    char_u      *p;
    char_u      *newline;
    char_u      *oldline;
    char_u      *s;
    int         todo;
    int         ind_len;            /* measured in characters */
    int         line_len;
    int         doit = FALSE;
    int         ind_done = 0;       /* measured in spaces */
    int         tab_pad;
    int         retval = FALSE;
    int         orig_char_len = -1; /* number of initial whitespace chars when
                                       'et' and 'pi' are both set */

    /*
     * First check if there is anything to do and compute the number of
     * characters needed for the indent.
     */
    todo = size;
    ind_len = 0;
    p = oldline = ml_get_curline();

    /* Calculate the buffer size for the new indent, and check to see if it
     * isn't already set */

    /* if 'expandtab' isn't set: use TABs; if both 'expandtab' and
     * 'preserveindent' are set count the number of characters at the
     * beginning of the line to be copied */
    if (!curbuf->b_p_et || (!(flags & SIN_INSERT) && curbuf->b_p_pi))
    {
        /* If 'preserveindent' is set then reuse as much as possible of
         * the existing indent structure for the new indent */
        if (!(flags & SIN_INSERT) && curbuf->b_p_pi)
        {
            ind_done = 0;

            /* count as many characters as we can use */
            while (todo > 0 && vim_iswhite(*p))
            {
                if (*p == TAB)
                {
                    tab_pad = (int)curbuf->b_p_ts - (ind_done % (int)curbuf->b_p_ts);
                    /* stop if this tab will overshoot the target */
                    if (todo < tab_pad)
                        break;
                    todo -= tab_pad;
                    ++ind_len;
                    ind_done += tab_pad;
                }
                else
                {
                    --todo;
                    ++ind_len;
                    ++ind_done;
                }
                ++p;
            }

            /* Set initial number of whitespace chars to copy if we are
             * preserving indent but expandtab is set */
            if (curbuf->b_p_et)
                orig_char_len = ind_len;

            /* Fill to next tabstop with a tab, if possible */
            tab_pad = (int)curbuf->b_p_ts - (ind_done % (int)curbuf->b_p_ts);
            if (todo >= tab_pad && orig_char_len == -1)
            {
                doit = TRUE;
                todo -= tab_pad;
                ++ind_len;
                /* ind_done += tab_pad; */
            }
        }

        /* count tabs required for indent */
        while (todo >= (int)curbuf->b_p_ts)
        {
            if (*p != TAB)
                doit = TRUE;
            else
                ++p;
            todo -= (int)curbuf->b_p_ts;
            ++ind_len;
            /* ind_done += (int)curbuf->b_p_ts; */
        }
    }
    /* count spaces required for indent */
    while (todo > 0)
    {
        if (*p != ' ')
            doit = TRUE;
        else
            ++p;
        --todo;
        ++ind_len;
        /* ++ind_done; */
    }

    /* Return if the indent is OK already. */
    if (!doit && !vim_iswhite(*p) && !(flags & SIN_INSERT))
        return FALSE;

    /* Allocate memory for the new line. */
    if (flags & SIN_INSERT)
        p = oldline;
    else
        p = skipwhite(p);
    line_len = (int)STRLEN(p) + 1;

    /* If 'preserveindent' and 'expandtab' are both set keep the original
     * characters and allocate accordingly.  We will fill the rest with spaces
     * after the if (!curbuf->b_p_et) below. */
    if (orig_char_len != -1)
    {
        newline = alloc(orig_char_len + size - ind_done + line_len);
        if (newline == NULL)
            return FALSE;
        todo = size - ind_done;
        ind_len = orig_char_len + todo;    /* Set total length of indent in
                                            * characters, which may have been
                                            * undercounted until now */
        p = oldline;
        s = newline;
        while (orig_char_len > 0)
        {
            *s++ = *p++;
            orig_char_len--;
        }

        /* Skip over any additional white space (useful when newindent is less than old) */
        while (vim_iswhite(*p))
            ++p;
    }
    else
    {
        todo = size;
        newline = alloc(ind_len + line_len);
        if (newline == NULL)
            return FALSE;
        s = newline;
    }

    /* Put the characters in the new line. */
    /* if 'expandtab' isn't set: use TABs */
    if (!curbuf->b_p_et)
    {
        /* If 'preserveindent' is set then reuse as much as possible of
         * the existing indent structure for the new indent */
        if (!(flags & SIN_INSERT) && curbuf->b_p_pi)
        {
            p = oldline;
            ind_done = 0;

            while (todo > 0 && vim_iswhite(*p))
            {
                if (*p == TAB)
                {
                    tab_pad = (int)curbuf->b_p_ts - (ind_done % (int)curbuf->b_p_ts);
                    /* stop if this tab will overshoot the target */
                    if (todo < tab_pad)
                        break;
                    todo -= tab_pad;
                    ind_done += tab_pad;
                }
                else
                {
                    --todo;
                    ++ind_done;
                }
                *s++ = *p++;
            }

            /* Fill to next tabstop with a tab, if possible */
            tab_pad = (int)curbuf->b_p_ts - (ind_done % (int)curbuf->b_p_ts);
            if (todo >= tab_pad)
            {
                *s++ = TAB;
                todo -= tab_pad;
            }

            p = skipwhite(p);
        }

        while (todo >= (int)curbuf->b_p_ts)
        {
            *s++ = TAB;
            todo -= (int)curbuf->b_p_ts;
        }
    }
    while (todo > 0)
    {
        *s++ = ' ';
        --todo;
    }
    mch_memmove(s, p, (size_t)line_len);

    /* Replace the line (unless undo fails). */
    if (!(flags & SIN_UNDO) || u_savesub(curwin->w_cursor.lnum) == OK)
    {
        ml_replace(curwin->w_cursor.lnum, newline, FALSE);
        if (flags & SIN_CHANGED)
            changed_bytes(curwin->w_cursor.lnum, 0);
        /* Correct saved cursor position if it is in this line. */
        if (saved_cursor.lnum == curwin->w_cursor.lnum)
        {
            if (saved_cursor.col >= (colnr_T)(p - oldline))
                /* cursor was after the indent, adjust for the number of
                 * bytes added/removed */
                saved_cursor.col += ind_len - (colnr_T)(p - oldline);
            else if (saved_cursor.col >= (colnr_T)(s - newline))
                /* cursor was in the indent, and is now after it, put it back
                 * at the start of the indent (replacing spaces with TAB) */
                saved_cursor.col = (colnr_T)(s - newline);
        }
        retval = TRUE;
    }
    else
        vim_free(newline);

    curwin->w_cursor.col = ind_len;
    return retval;
}

/*
 * Copy the indent from ptr to the current line (and fill to size)
 * Leaves the cursor on the first non-blank in the line.
 * Returns TRUE if the line was changed.
 */
    static int
copy_indent(size, src)
    int         size;
    char_u      *src;
{
    char_u      *p = NULL;
    char_u      *line = NULL;
    char_u      *s;
    int         todo;
    int         ind_len;
    int         line_len = 0;
    int         tab_pad;
    int         ind_done;
    int         round;

    /* Round 1: compute the number of characters needed for the indent
     * Round 2: copy the characters. */
    for (round = 1; round <= 2; ++round)
    {
        todo = size;
        ind_len = 0;
        ind_done = 0;
        s = src;

        /* Count/copy the usable portion of the source line */
        while (todo > 0 && vim_iswhite(*s))
        {
            if (*s == TAB)
            {
                tab_pad = (int)curbuf->b_p_ts - (ind_done % (int)curbuf->b_p_ts);
                /* Stop if this tab will overshoot the target */
                if (todo < tab_pad)
                    break;
                todo -= tab_pad;
                ind_done += tab_pad;
            }
            else
            {
                --todo;
                ++ind_done;
            }
            ++ind_len;
            if (p != NULL)
                *p++ = *s;
            ++s;
        }

        /* Fill to next tabstop with a tab, if possible */
        tab_pad = (int)curbuf->b_p_ts - (ind_done % (int)curbuf->b_p_ts);
        if (todo >= tab_pad && !curbuf->b_p_et)
        {
            todo -= tab_pad;
            ++ind_len;
            if (p != NULL)
                *p++ = TAB;
        }

        /* Add tabs required for indent */
        while (todo >= (int)curbuf->b_p_ts && !curbuf->b_p_et)
        {
            todo -= (int)curbuf->b_p_ts;
            ++ind_len;
            if (p != NULL)
                *p++ = TAB;
        }

        /* Count/add spaces required for indent */
        while (todo > 0)
        {
            --todo;
            ++ind_len;
            if (p != NULL)
                *p++ = ' ';
        }

        if (p == NULL)
        {
            /* Allocate memory for the result: the copied indent, new indent
             * and the rest of the line. */
            line_len = (int)STRLEN(ml_get_curline()) + 1;
            line = alloc(ind_len + line_len);
            if (line == NULL)
                return FALSE;
            p = line;
        }
    }

    /* Append the original line */
    mch_memmove(p, ml_get_curline(), (size_t)line_len);

    /* Replace the line */
    ml_replace(curwin->w_cursor.lnum, line, FALSE);

    /* Put the cursor after the indent. */
    curwin->w_cursor.col = ind_len;
    return TRUE;
}

/*
 * Return the indent of the current line after a number.  Return -1 if no
 * number was found.  Used for 'n' in 'formatoptions': numbered list.
 * Since a pattern is used it can actually handle more than numbers.
 */
    int
get_number_indent(lnum)
    linenr_T    lnum;
{
    colnr_T     col;
    pos_T       pos;

    regmatch_T  regmatch;
    int         lead_len = 0;   /* length of comment leader */

    if (lnum > curbuf->b_ml.ml_line_count)
        return -1;
    pos.lnum = 0;

    /* In format_lines() (i.e. not insert mode), fo+=q is needed too... */
    if ((State & INSERT) || has_format_option(FO_Q_COMS))
        lead_len = get_leader_len(ml_get(lnum), NULL, FALSE, TRUE);
    regmatch.regprog = vim_regcomp(curbuf->b_p_flp, RE_MAGIC);
    if (regmatch.regprog != NULL)
    {
        regmatch.rm_ic = FALSE;

        /* vim_regexec() expects a pointer to a line.  This lets us
         * start matching for the flp beyond any comment leader... */
        if (vim_regexec(&regmatch, ml_get(lnum) + lead_len, (colnr_T)0))
        {
            pos.lnum = lnum;
            pos.col = (colnr_T)(*regmatch.endp - ml_get(lnum));
            pos.coladd = 0;
        }
        vim_regfree(regmatch.regprog);
    }

    if (pos.lnum == 0 || *ml_get_pos(&pos) == NUL)
        return -1;
    getvcol(curwin, &pos, &col, NULL, NULL);
    return (int)col;
}

/*
 * Return appropriate space number for breakindent, taking influencing
 * parameters into account. Window must be specified, since it is not
 * necessarily always the current one.
 */
    int
get_breakindent_win(wp, line)
    win_T       *wp;
    char_u      *line; /* start of the line */
{
    static int      prev_indent = 0;  /* cached indent value */
    static long     prev_ts     = 0L; /* cached tabstop value */
    static char_u   *prev_line = NULL; /* cached pointer to line */
    static int      prev_tick = 0;   /* changedtick of cached value */
    int             bri = 0;
    /* window width minus window margin space, i.e. what rests for text */
    const int       eff_wwidth = W_WIDTH(wp)
                            - ((wp->w_p_nu || wp->w_p_rnu)
                                && (vim_strchr(p_cpo, CPO_NUMCOL) == NULL)
                                                ? number_width(wp) + 1 : 0);

    /* used cached indent, unless pointer or 'tabstop' changed */
    if (prev_line != line || prev_ts != wp->w_buffer->b_p_ts
                                  || prev_tick != wp->w_buffer->b_changedtick)
    {
        prev_line = line;
        prev_ts = wp->w_buffer->b_p_ts;
        prev_tick = wp->w_buffer->b_changedtick;
        prev_indent = get_indent_str(line, (int)wp->w_buffer->b_p_ts, wp->w_p_list);
    }
    bri = prev_indent + wp->w_p_brishift;

    /* indent minus the length of the showbreak string */
    if (wp->w_p_brisbr)
        bri -= vim_strsize(p_sbr);

    /* Add offset for number column, if 'n' is in 'cpoptions' */
    bri += win_col_off2(wp);

    /* never indent past left window margin */
    if (bri < 0)
        bri = 0;
    /* always leave at least bri_min characters on the left,
     * if text width is sufficient */
    else if (bri > eff_wwidth - wp->w_p_brimin)
        bri = (eff_wwidth - wp->w_p_brimin < 0) ? 0 : eff_wwidth - wp->w_p_brimin;

    return bri;
}

static int cin_is_cinword(char_u *line);

/*
 * Return TRUE if the string "line" starts with a word from 'cinwords'.
 */
    static int
cin_is_cinword(line)
    char_u      *line;
{
    char_u      *cinw;
    char_u      *cinw_buf;
    int         cinw_len;
    int         retval = FALSE;
    int         len;

    cinw_len = (int)STRLEN(curbuf->b_p_cinw) + 1;
    cinw_buf = alloc((unsigned)cinw_len);
    if (cinw_buf != NULL)
    {
        line = skipwhite(line);
        for (cinw = curbuf->b_p_cinw; *cinw; )
        {
            len = copy_option_part(&cinw, cinw_buf, cinw_len, ",");
            if (STRNCMP(line, cinw_buf, len) == 0
                    && (!vim_iswordc(line[len]) || !vim_iswordc(line[len - 1])))
            {
                retval = TRUE;
                break;
            }
        }
        vim_free(cinw_buf);
    }
    return retval;
}

/*
 * open_line: Add a new line below or above the current line.
 *
 * For VREPLACE mode, we only add a new line when we get to the end of the
 * file, otherwise we just start replacing the next line.
 *
 * Caller must take care of undo.  Since VREPLACE may affect any number of
 * lines however, it may call u_save_cursor() again when starting to change a new line.
 * "flags": OPENLINE_DELSPACES  delete spaces after cursor
 *          OPENLINE_DO_COM     format comments
 *          OPENLINE_KEEPTRAIL  keep trailing spaces
 *          OPENLINE_MARKFIX    adjust mark positions after the line break
 *          OPENLINE_COM_LIST   format comments with list or 2nd line indent
 *
 * "second_line_indent": indent for after ^^D in Insert mode or if flag
 *                        OPENLINE_COM_LIST
 *
 * Return TRUE for success, FALSE for failure
 */
    int
open_line(dir, flags, second_line_indent)
    int         dir;            /* FORWARD or BACKWARD */
    int         flags;
    int         second_line_indent;
{
    char_u      *saved_line;            /* copy of the original line */
    char_u      *next_line = NULL;      /* copy of the next line */
    char_u      *p_extra = NULL;        /* what goes to next line */
    int         less_cols = 0;          /* less columns for mark in new line */
    int         less_cols_off = 0;      /* columns to skip for mark adjust */
    pos_T       old_cursor;             /* old cursor position */
    int         newcol = 0;             /* new cursor column */
    int         newindent = 0;          /* auto-indent of the new line */
    int         n;
    int         trunc_line = FALSE;     /* truncate current line afterwards */
    int         retval = FALSE;         /* return value, default is FAIL */
    int         extra_len = 0;          /* length of p_extra string */
    int         lead_len;               /* length of comment leader */
    char_u      *lead_flags;    /* position in 'comments' for comment leader */
    char_u      *leader = NULL;         /* copy of comment leader */
    char_u      *allocated = NULL;      /* allocated memory */
    char_u      *p;
    int         saved_char = NUL;       /* init for GCC */
    pos_T       *pos;
    int         do_si = (!p_paste && curbuf->b_p_si && !curbuf->b_p_cin);
    int         no_si = FALSE;          /* reset did_si afterwards */
    int         first_char = NUL;       /* init for GCC */
    int         vreplace_mode;
    int         did_append;             /* appended a new line */
    int         saved_pi = curbuf->b_p_pi; /* copy of preserveindent setting */

    /*
     * make a copy of the current line so we can mess with it
     */
    saved_line = vim_strsave(ml_get_curline());
    if (saved_line == NULL)         /* out of memory! */
        return FALSE;

    if (State & VREPLACE_FLAG)
    {
        /*
         * With VREPLACE we make a copy of the next line, which we will be
         * starting to replace.  First make the new line empty and let vim play
         * with the indenting and comment leader to its heart's content.  Then
         * we grab what it ended up putting on the new line, put back the
         * original line, and call ins_char() to put each new character onto
         * the line, replacing what was there before and pushing the right
         * stuff onto the replace stack.  -- webb.
         */
        if (curwin->w_cursor.lnum < orig_line_count)
            next_line = vim_strsave(ml_get(curwin->w_cursor.lnum + 1));
        else
            next_line = vim_strsave((char_u *)"");
        if (next_line == NULL)      /* out of memory! */
            goto theend;

        /*
         * In VREPLACE mode, a NL replaces the rest of the line, and starts
         * replacing the next line, so push all of the characters left on the
         * line onto the replace stack.  We'll push any other characters that
         * might be replaced at the start of the next line (due to autoindent
         * etc) a bit later.
         */
        replace_push(NUL);  /* Call twice because BS over NL expects it */
        replace_push(NUL);
        p = saved_line + curwin->w_cursor.col;
        while (*p != NUL)
        {
            p += replace_push_mb(p);
        }
        saved_line[curwin->w_cursor.col] = NUL;
    }

    if ((State & INSERT) && !(State & VREPLACE_FLAG))
    {
        p_extra = saved_line + curwin->w_cursor.col;
        if (do_si)              /* need first char after new line break */
        {
            p = skipwhite(p_extra);
            first_char = *p;
        }
        extra_len = (int)STRLEN(p_extra);
        saved_char = *p_extra;
        *p_extra = NUL;
    }

    u_clearline();              /* cannot do "U" command when adding lines */
    did_si = FALSE;
    ai_col = 0;

    /*
     * If we just did an auto-indent, then we didn't type anything on
     * the prior line, and it should be truncated.  Do this even if 'ai' is not
     * set because automatically inserting a comment leader also sets did_ai.
     */
    if (dir == FORWARD && did_ai)
        trunc_line = TRUE;

    /*
     * If 'autoindent' and/or 'smartindent' is set, try to figure out what
     * indent to use for the new line.
     */
    if (curbuf->b_p_ai || do_si)
    {
        /*
         * count white space on current line
         */
        newindent = get_indent_str(saved_line, (int)curbuf->b_p_ts, FALSE);
        if (newindent == 0 && !(flags & OPENLINE_COM_LIST))
            newindent = second_line_indent; /* for ^^D command in insert mode */

        /*
         * Do smart indenting.
         * In insert/replace mode (only when dir == FORWARD)
         * we may move some text to the next line. If it starts with '{'
         * don't add an indent. Fixes inserting a NL before '{' in line
         *      "if (condition) {"
         */
        if (!trunc_line && do_si && *saved_line != NUL && (p_extra == NULL || first_char != '{'))
        {
            char_u  *ptr;
            char_u  last_char;

            old_cursor = curwin->w_cursor;
            ptr = saved_line;
            if (flags & OPENLINE_DO_COM)
                lead_len = get_leader_len(ptr, NULL, FALSE, TRUE);
            else
                lead_len = 0;
            if (dir == FORWARD)
            {
                /*
                 * Skip preprocessor directives, unless they are
                 * recognised as comments.
                 */
                if (lead_len == 0 && ptr[0] == '#')
                {
                    while (ptr[0] == '#' && curwin->w_cursor.lnum > 1)
                        ptr = ml_get(--curwin->w_cursor.lnum);
                    newindent = get_indent();
                }
                if (flags & OPENLINE_DO_COM)
                    lead_len = get_leader_len(ptr, NULL, FALSE, TRUE);
                else
                    lead_len = 0;
                if (lead_len > 0)
                {
                    /*
                     * This case gets the following right:
                     *      \*
                     *       * A comment (read '\' as '/').
                     *       *\
                     * #define IN_THE_WAY
                     *      This should line up here;
                     */
                    p = skipwhite(ptr);
                    if (p[0] == '/' && p[1] == '*')
                        p++;
                    if (p[0] == '*')
                    {
                        for (p++; *p; p++)
                        {
                            if (p[0] == '/' && p[-1] == '*')
                            {
                                /*
                                 * End of C comment, indent should line up
                                 * with the line containing the start of
                                 * the comment
                                 */
                                curwin->w_cursor.col = (colnr_T)(p - ptr);
                                if ((pos = findmatch(NULL, NUL)) != NULL)
                                {
                                    curwin->w_cursor.lnum = pos->lnum;
                                    newindent = get_indent();
                                }
                            }
                        }
                    }
                }
                else    /* Not a comment line */
                {
                    /* Find last non-blank in line */
                    p = ptr + STRLEN(ptr) - 1;
                    while (p > ptr && vim_iswhite(*p))
                        --p;
                    last_char = *p;

                    /*
                     * find the character just before the '{' or ';'
                     */
                    if (last_char == '{' || last_char == ';')
                    {
                        if (p > ptr)
                            --p;
                        while (p > ptr && vim_iswhite(*p))
                            --p;
                    }
                    /*
                     * Try to catch lines that are split over multiple
                     * lines.  eg:
                     *      if (condition &&
                     *                  condition) {
                     *          Should line up here!
                     *      }
                     */
                    if (*p == ')')
                    {
                        curwin->w_cursor.col = (colnr_T)(p - ptr);
                        if ((pos = findmatch(NULL, '(')) != NULL)
                        {
                            curwin->w_cursor.lnum = pos->lnum;
                            newindent = get_indent();
                            ptr = ml_get_curline();
                        }
                    }
                    /*
                     * If last character is '{' do indent, without
                     * checking for "if" and the like.
                     */
                    if (last_char == '{')
                    {
                        did_si = TRUE;  /* do indent */
                        no_si = TRUE;   /* don't delete it when '{' typed */
                    }
                    /*
                     * Look for "if" and the like, use 'cinwords'.
                     * Don't do this if the previous line ended in ';' or '}'.
                     */
                    else if (last_char != ';' && last_char != '}' && cin_is_cinword(ptr))
                        did_si = TRUE;
                }
            }
            else /* dir == BACKWARD */
            {
                /*
                 * Skip preprocessor directives, unless they are
                 * recognised as comments.
                 */
                if (lead_len == 0 && ptr[0] == '#')
                {
                    int was_backslashed = FALSE;

                    while ((ptr[0] == '#' || was_backslashed) &&
                         curwin->w_cursor.lnum < curbuf->b_ml.ml_line_count)
                    {
                        if (*ptr && ptr[STRLEN(ptr) - 1] == '\\')
                            was_backslashed = TRUE;
                        else
                            was_backslashed = FALSE;
                        ptr = ml_get(++curwin->w_cursor.lnum);
                    }
                    if (was_backslashed)
                        newindent = 0;      /* Got to end of file */
                    else
                        newindent = get_indent();
                }
                p = skipwhite(ptr);
                if (*p == '}')      /* if line starts with '}': do indent */
                    did_si = TRUE;
                else                /* can delete indent when '{' typed */
                    can_si_back = TRUE;
            }
            curwin->w_cursor = old_cursor;
        }
        if (do_si)
            can_si = TRUE;

        did_ai = TRUE;
    }

    /*
     * Find out if the current line starts with a comment leader.
     * This may then be inserted in front of the new line.
     */
    end_comment_pending = NUL;
    if (flags & OPENLINE_DO_COM)
        lead_len = get_leader_len(saved_line, &lead_flags, dir == BACKWARD, TRUE);
    else
        lead_len = 0;
    if (lead_len > 0)
    {
        char_u  *lead_repl = NULL;          /* replaces comment leader */
        int     lead_repl_len = 0;          /* length of *lead_repl */
        char_u  lead_middle[COM_MAX_LEN];   /* middle-comment string */
        char_u  lead_end[COM_MAX_LEN];      /* end-comment string */
        char_u  *comment_end = NULL;        /* where lead_end has been found */
        int     extra_space = FALSE;        /* append extra space */
        int     current_flag;
        int     require_blank = FALSE;      /* requires blank after middle */
        char_u  *p2;

        /*
         * If the comment leader has the start, middle or end flag, it may not
         * be used or may be replaced with the middle leader.
         */
        for (p = lead_flags; *p && *p != ':'; ++p)
        {
            if (*p == COM_BLANK)
            {
                require_blank = TRUE;
                continue;
            }
            if (*p == COM_START || *p == COM_MIDDLE)
            {
                current_flag = *p;
                if (*p == COM_START)
                {
                    /*
                     * Doing "O" on a start of comment does not insert leader.
                     */
                    if (dir == BACKWARD)
                    {
                        lead_len = 0;
                        break;
                    }

                    /* find start of middle part */
                    (void)copy_option_part(&p, lead_middle, COM_MAX_LEN, ",");
                    require_blank = FALSE;
                }

                /*
                 * Isolate the strings of the middle and end leader.
                 */
                while (*p && p[-1] != ':')      /* find end of middle flags */
                {
                    if (*p == COM_BLANK)
                        require_blank = TRUE;
                    ++p;
                }
                (void)copy_option_part(&p, lead_middle, COM_MAX_LEN, ",");

                while (*p && p[-1] != ':')      /* find end of end flags */
                {
                    /* Check whether we allow automatic ending of comments */
                    if (*p == COM_AUTO_END)
                        end_comment_pending = -1; /* means we want to set it */
                    ++p;
                }
                n = copy_option_part(&p, lead_end, COM_MAX_LEN, ",");

                if (end_comment_pending == -1)  /* we can set it now */
                    end_comment_pending = lead_end[n - 1];

                /*
                 * If the end of the comment is in the same line, don't use
                 * the comment leader.
                 */
                if (dir == FORWARD)
                {
                    for (p = saved_line + lead_len; *p; ++p)
                        if (STRNCMP(p, lead_end, n) == 0)
                        {
                            comment_end = p;
                            lead_len = 0;
                            break;
                        }
                }

                /*
                 * Doing "o" on a start of comment inserts the middle leader.
                 */
                if (lead_len > 0)
                {
                    if (current_flag == COM_START)
                    {
                        lead_repl = lead_middle;
                        lead_repl_len = (int)STRLEN(lead_middle);
                    }

                    /*
                     * If we have hit RETURN immediately after the start
                     * comment leader, then put a space after the middle
                     * comment leader on the next line.
                     */
                    if (!vim_iswhite(saved_line[lead_len - 1])
                            && ((p_extra != NULL
                                    && (int)curwin->w_cursor.col == lead_len)
                                || (p_extra == NULL
                                    && saved_line[lead_len] == NUL)
                                || require_blank))
                        extra_space = TRUE;
                }
                break;
            }
            if (*p == COM_END)
            {
                /*
                 * Doing "o" on the end of a comment does not insert leader.
                 * Remember where the end is, might want to use it to find the
                 * start (for C-comments).
                 */
                if (dir == FORWARD)
                {
                    comment_end = skipwhite(saved_line);
                    lead_len = 0;
                    break;
                }

                /*
                 * Doing "O" on the end of a comment inserts the middle leader.
                 * Find the string for the middle leader, searching backwards.
                 */
                while (p > curbuf->b_p_com && *p != ',')
                    --p;
                for (lead_repl = p; lead_repl > curbuf->b_p_com && lead_repl[-1] != ':'; --lead_repl)
                    ;
                lead_repl_len = (int)(p - lead_repl);

                /* We can probably always add an extra space when doing "O" on the comment-end */
                extra_space = TRUE;

                /* Check whether we allow automatic ending of comments */
                for (p2 = p; *p2 && *p2 != ':'; p2++)
                {
                    if (*p2 == COM_AUTO_END)
                        end_comment_pending = -1; /* means we want to set it */
                }
                if (end_comment_pending == -1)
                {
                    /* Find last character in end-comment string */
                    while (*p2 && *p2 != ',')
                        p2++;
                    end_comment_pending = p2[-1];
                }
                break;
            }
            if (*p == COM_FIRST)
            {
                /*
                 * Comment leader for first line only:  Don't repeat leader
                 * when using "O", blank out leader when using "o".
                 */
                if (dir == BACKWARD)
                    lead_len = 0;
                else
                {
                    lead_repl = (char_u *)"";
                    lead_repl_len = 0;
                }
                break;
            }
        }
        if (lead_len)
        {
            /* allocate buffer (may concatenate p_extra later) */
            leader = alloc(lead_len + lead_repl_len + extra_space + extra_len
                     + (second_line_indent > 0 ? second_line_indent : 0) + 1);
            allocated = leader;             /* remember to free it later */

            if (leader == NULL)
                lead_len = 0;
            else
            {
                vim_strncpy(leader, saved_line, lead_len);

                /*
                 * Replace leader with lead_repl, right or left adjusted
                 */
                if (lead_repl != NULL)
                {
                    int         c = 0;
                    int         off = 0;

                    for (p = lead_flags; *p != NUL && *p != ':'; )
                    {
                        if (*p == COM_RIGHT || *p == COM_LEFT)
                            c = *p++;
                        else if (VIM_ISDIGIT(*p) || *p == '-')
                            off = getdigits(&p);
                        else
                            ++p;
                    }
                    if (c == COM_RIGHT)    /* right adjusted leader */
                    {
                        /* find last non-white in the leader to line up with */
                        for (p = leader + lead_len - 1; p > leader && vim_iswhite(*p); --p)
                            ;
                        ++p;

                        /* Compute the length of the replaced characters in
                         * screen characters, not bytes. */
                        {
                            int     repl_size = vim_strnsize(lead_repl, lead_repl_len);
                            int     old_size = 0;
                            char_u  *endp = p;
                            int     l;

                            while (old_size < repl_size && p > leader)
                            {
                                mb_ptr_back(leader, p);
                                old_size += ptr2cells(p);
                            }
                            l = lead_repl_len - (int)(endp - p);
                            if (l != 0)
                                mch_memmove(endp + l, endp, (size_t)((leader + lead_len) - endp));
                            lead_len += l;
                        }
                        mch_memmove(p, lead_repl, (size_t)lead_repl_len);
                        if (p + lead_repl_len > leader + lead_len)
                            p[lead_repl_len] = NUL;

                        /* blank-out any other chars from the old leader. */
                        while (--p >= leader)
                        {
                            int l = utf_head_off(leader, p);

                            if (l > 1)
                            {
                                p -= l;
                                if (ptr2cells(p) > 1)
                                {
                                    p[1] = ' ';
                                    --l;
                                }
                                mch_memmove(p + 1, p + l + 1,
                                   (size_t)((leader + lead_len) - (p + l + 1)));
                                lead_len -= l;
                                *p = ' ';
                            }
                            else if (!vim_iswhite(*p))
                                *p = ' ';
                        }
                    }
                    else                    /* left adjusted leader */
                    {
                        p = skipwhite(leader);
                        /* Compute the length of the replaced characters in
                         * screen characters, not bytes. Move the part that is
                         * not to be overwritten. */
                        {
                            int     repl_size = vim_strnsize(lead_repl, lead_repl_len);
                            int     i;
                            int     l;

                            for (i = 0; p[i] != NUL && i < lead_len; i += l)
                            {
                                l = utfc_ptr2len(p + i);
                                if (vim_strnsize(p, i + l) > repl_size)
                                    break;
                            }
                            if (i != lead_repl_len)
                            {
                                mch_memmove(p + lead_repl_len, p + i,
                                       (size_t)(lead_len - i - (p - leader)));
                                lead_len += lead_repl_len - i;
                            }
                        }
                        mch_memmove(p, lead_repl, (size_t)lead_repl_len);

                        /* Replace any remaining non-white chars in the old
                         * leader by spaces.  Keep Tabs, the indent must
                         * remain the same. */
                        for (p += lead_repl_len; p < leader + lead_len; ++p)
                            if (!vim_iswhite(*p))
                            {
                                /* Don't put a space before a TAB. */
                                if (p + 1 < leader + lead_len && p[1] == TAB)
                                {
                                    --lead_len;
                                    mch_memmove(p, p + 1, (leader + lead_len) - p);
                                }
                                else
                                {
                                    int     l = utfc_ptr2len(p);

                                    if (l > 1)
                                    {
                                        if (ptr2cells(p) > 1)
                                        {
                                            /* Replace a double-wide char with two spaces */
                                            --l;
                                            *p++ = ' ';
                                        }
                                        mch_memmove(p + 1, p + l, (leader + lead_len) - p);
                                        lead_len -= l - 1;
                                    }
                                    *p = ' ';
                                }
                            }
                        *p = NUL;
                    }

                    /* Recompute the indent, it may have changed. */
                    if (curbuf->b_p_ai || do_si)
                        newindent = get_indent_str(leader, (int)curbuf->b_p_ts, FALSE);

                    /* Add the indent offset */
                    if (newindent + off < 0)
                    {
                        off = -newindent;
                        newindent = 0;
                    }
                    else
                        newindent += off;

                    /* Correct trailing spaces for the shift, so that
                     * alignment remains equal. */
                    while (off > 0 && lead_len > 0 && leader[lead_len - 1] == ' ')
                    {
                        /* Don't do it when there is a tab before the space */
                        if (vim_strchr(skipwhite(leader), '\t') != NULL)
                            break;
                        --lead_len;
                        --off;
                    }

                    /* If the leader ends in white space, don't add an extra space */
                    if (lead_len > 0 && vim_iswhite(leader[lead_len - 1]))
                        extra_space = FALSE;
                    leader[lead_len] = NUL;
                }

                if (extra_space)
                {
                    leader[lead_len++] = ' ';
                    leader[lead_len] = NUL;
                }

                newcol = lead_len;

                /*
                 * if a new indent will be set below, remove the indent that
                 * is in the comment leader
                 */
                if (newindent || did_si)
                {
                    while (lead_len && vim_iswhite(*leader))
                    {
                        --lead_len;
                        --newcol;
                        ++leader;
                    }
                }
            }
            did_si = can_si = FALSE;
        }
        else if (comment_end != NULL)
        {
            /*
             * We have finished a comment, so we don't use the leader.
             * If this was a C-comment and 'ai' or 'si' is set do a normal
             * indent to align with the line containing the start of the comment.
             */
            if (comment_end[0] == '*' && comment_end[1] == '/' && (curbuf->b_p_ai || do_si))
            {
                old_cursor = curwin->w_cursor;
                curwin->w_cursor.col = (colnr_T)(comment_end - saved_line);
                if ((pos = findmatch(NULL, NUL)) != NULL)
                {
                    curwin->w_cursor.lnum = pos->lnum;
                    newindent = get_indent();
                }
                curwin->w_cursor = old_cursor;
            }
        }
    }

    /* (State == INSERT || State == REPLACE), only when dir == FORWARD */
    if (p_extra != NULL)
    {
        *p_extra = saved_char;          /* restore char that NUL replaced */

        /*
         * When 'ai' set or "flags" has OPENLINE_DELSPACES, skip to the first non-blank.
         *
         * When in REPLACE mode, put the deleted blanks on the replace stack,
         * preceded by a NUL, so they can be put back when a BS is entered.
         */
        if (REPLACE_NORMAL(State))
            replace_push(NUL);      /* end of extra blanks */
        if (curbuf->b_p_ai || (flags & OPENLINE_DELSPACES))
        {
            while ((*p_extra == ' ' || *p_extra == '\t') && !utf_iscomposing(utf_ptr2char(p_extra + 1)))
            {
                if (REPLACE_NORMAL(State))
                    replace_push(*p_extra);
                ++p_extra;
                ++less_cols_off;
            }
        }
        if (*p_extra != NUL)
            did_ai = FALSE;         /* append some text, don't truncate now */

        /* columns for marks adjusted for removed columns */
        less_cols = (int)(p_extra - saved_line);
    }

    if (p_extra == NULL)
        p_extra = (char_u *)"";             /* append empty line */

    /* concatenate leader and p_extra, if there is a leader */
    if (lead_len)
    {
        if (flags & OPENLINE_COM_LIST && second_line_indent > 0)
        {
            int i;
            int padding = second_line_indent - (newindent + (int)STRLEN(leader));

            /* Here whitespace is inserted after the comment char.
             * Below, set_indent(newindent, SIN_INSERT) will insert the
             * whitespace needed before the comment char. */
            for (i = 0; i < padding; i++)
            {
                STRCAT(leader, " ");
                less_cols--;
                newcol++;
            }
        }
        STRCAT(leader, p_extra);
        p_extra = leader;
        did_ai = TRUE;      /* So truncating blanks works with comments */
        less_cols -= lead_len;
    }
    else
        end_comment_pending = NUL;  /* turns out there was no leader */

    old_cursor = curwin->w_cursor;
    if (dir == BACKWARD)
        --curwin->w_cursor.lnum;
    if (!(State & VREPLACE_FLAG) || old_cursor.lnum >= orig_line_count)
    {
        if (ml_append(curwin->w_cursor.lnum, p_extra, (colnr_T)0, FALSE) == FAIL)
            goto theend;
        /* Postpone calling changed_lines(), because it would mess up folding with markers. */
        mark_adjust(curwin->w_cursor.lnum + 1, (linenr_T)MAXLNUM, 1L, 0L);
        did_append = TRUE;
    }
    else
    {
        /*
         * In VREPLACE mode we are starting to replace the next line.
         */
        curwin->w_cursor.lnum++;
        if (curwin->w_cursor.lnum >= Insstart.lnum + vr_lines_changed)
        {
            /* In case we NL to a new line, BS to the previous one, and NL
             * again, we don't want to save the new line for undo twice.
             */
            (void)u_save_cursor();                  /* errors are ignored! */
            vr_lines_changed++;
        }
        ml_replace(curwin->w_cursor.lnum, p_extra, TRUE);
        changed_bytes(curwin->w_cursor.lnum, 0);
        curwin->w_cursor.lnum--;
        did_append = FALSE;
    }

    if (newindent || did_si)
    {
        ++curwin->w_cursor.lnum;
        if (did_si)
        {
            int sw = (int)get_sw_value(curbuf);

            if (p_sr)
                newindent -= newindent % sw;
            newindent += sw;
        }
        /* Copy the indent */
        if (curbuf->b_p_ci)
        {
            (void)copy_indent(newindent, saved_line);

            /*
             * Set the 'preserveindent' option so that any further screwing
             * with the line doesn't entirely destroy our efforts to preserve
             * it.  It gets restored at the function end.
             */
            curbuf->b_p_pi = TRUE;
        }
        else
            (void)set_indent(newindent, SIN_INSERT);
        less_cols -= curwin->w_cursor.col;

        ai_col = curwin->w_cursor.col;

        /*
         * In REPLACE mode, for each character in the new indent, there must
         * be a NUL on the replace stack, for when it is deleted with BS
         */
        if (REPLACE_NORMAL(State))
            for (n = 0; n < (int)curwin->w_cursor.col; ++n)
                replace_push(NUL);
        newcol += curwin->w_cursor.col;
        if (no_si)
            did_si = FALSE;
    }

    /*
     * In REPLACE mode, for each character in the extra leader, there must be
     * a NUL on the replace stack, for when it is deleted with BS.
     */
    if (REPLACE_NORMAL(State))
        while (lead_len-- > 0)
            replace_push(NUL);

    curwin->w_cursor = old_cursor;

    if (dir == FORWARD)
    {
        if (trunc_line || (State & INSERT))
        {
            /* truncate current line at cursor */
            saved_line[curwin->w_cursor.col] = NUL;
            /* Remove trailing white space, unless OPENLINE_KEEPTRAIL used. */
            if (trunc_line && !(flags & OPENLINE_KEEPTRAIL))
                truncate_spaces(saved_line);
            ml_replace(curwin->w_cursor.lnum, saved_line, FALSE);
            saved_line = NULL;
            if (did_append)
            {
                changed_lines(curwin->w_cursor.lnum, curwin->w_cursor.col,
                                               curwin->w_cursor.lnum + 1, 1L);
                did_append = FALSE;

                /* Move marks after the line break to the new line. */
                if (flags & OPENLINE_MARKFIX)
                    mark_col_adjust(curwin->w_cursor.lnum,
                                         curwin->w_cursor.col + less_cols_off,
                                                        1L, (long)-less_cols);
            }
            else
                changed_bytes(curwin->w_cursor.lnum, curwin->w_cursor.col);
        }

        /*
         * Put the cursor on the new line.  Careful: the scrollup() above may
         * have moved w_cursor, we must use old_cursor.
         */
        curwin->w_cursor.lnum = old_cursor.lnum + 1;
    }
    if (did_append)
        changed_lines(curwin->w_cursor.lnum, 0, curwin->w_cursor.lnum, 1L);

    curwin->w_cursor.col = newcol;
    curwin->w_cursor.coladd = 0;

    /*
     * In VREPLACE mode, we are handling the replace stack ourselves, so stop
     * fixthisline() from doing it (via change_indent()) by telling it we're in
     * normal INSERT mode.
     */
    if (State & VREPLACE_FLAG)
    {
        vreplace_mode = State;  /* So we know to put things right later */
        State = INSERT;
    }
    else
        vreplace_mode = 0;
    /*
     * May do lisp indenting.
     */
    if (!p_paste
            && leader == NULL
            && curbuf->b_p_lisp
            && curbuf->b_p_ai)
    {
        fixthisline(get_lisp_indent);
        p = ml_get_curline();
        ai_col = (colnr_T)(skipwhite(p) - p);
    }
    /*
     * May do indenting after opening a new line.
     */
    if (!p_paste
            && (curbuf->b_p_cin || *curbuf->b_p_inde != NUL)
            && in_cinkeys(dir == FORWARD
                ? KEY_OPEN_FORW
                : KEY_OPEN_BACK, ' ', linewhite(curwin->w_cursor.lnum)))
    {
        do_c_expr_indent();
        p = ml_get_curline();
        ai_col = (colnr_T)(skipwhite(p) - p);
    }
    if (vreplace_mode != 0)
        State = vreplace_mode;

    /*
     * Finally, VREPLACE gets the stuff on the new line, then puts back the
     * original line, and inserts the new stuff char by char, pushing old stuff
     * onto the replace stack (via ins_char()).
     */
    if (State & VREPLACE_FLAG)
    {
        /* Put new line in p_extra */
        p_extra = vim_strsave(ml_get_curline());
        if (p_extra == NULL)
            goto theend;

        /* Put back original line */
        ml_replace(curwin->w_cursor.lnum, next_line, FALSE);

        /* Insert new stuff into line again */
        curwin->w_cursor.col = 0;
        curwin->w_cursor.coladd = 0;
        ins_bytes(p_extra);     /* will call changed_bytes() */
        vim_free(p_extra);
        next_line = NULL;
    }

    retval = TRUE;              /* success! */
theend:
    curbuf->b_p_pi = saved_pi;
    vim_free(saved_line);
    vim_free(next_line);
    vim_free(allocated);
    return retval;
}

/*
 * get_leader_len() returns the length in bytes of the prefix of the given
 * string which introduces a comment.  If this string is not a comment then 0 is returned.
 * When "flags" is not NULL, it is set to point to the flags of the recognized comment leader.
 * "backward" must be true for the "O" command.
 * If "include_space" is set, include trailing whitespace while calculating the length.
 */
    int
get_leader_len(line, flags, backward, include_space)
    char_u      *line;
    char_u      **flags;
    int         backward;
    int         include_space;
{
    int         i, j;
    int         result;
    int         got_com = FALSE;
    int         found_one;
    char_u      part_buf[COM_MAX_LEN];  /* buffer for one option part */
    char_u      *string;                /* pointer to comment string */
    char_u      *list;
    int         middle_match_len = 0;
    char_u      *prev_list;
    char_u      *saved_flags = NULL;

    result = i = 0;
    while (vim_iswhite(line[i]))    /* leading white space is ignored */
        ++i;

    /*
     * Repeat to match several nested comment strings.
     */
    while (line[i] != NUL)
    {
        /*
         * scan through the 'comments' option for a match
         */
        found_one = FALSE;
        for (list = curbuf->b_p_com; *list; )
        {
            /* Get one option part into part_buf[].  Advance "list" to next
             * one.  Put "string" at start of string. */
            if (!got_com && flags != NULL)
                *flags = list;      /* remember where flags started */
            prev_list = list;
            (void)copy_option_part(&list, part_buf, COM_MAX_LEN, ",");
            string = vim_strchr(part_buf, ':');
            if (string == NULL)     /* missing ':', ignore this part */
                continue;
            *string++ = NUL;        /* isolate flags from string */

            /* If we found a middle match previously, use that match when this
             * is not a middle or end. */
            if (middle_match_len != 0
                    && vim_strchr(part_buf, COM_MIDDLE) == NULL
                    && vim_strchr(part_buf, COM_END) == NULL)
                break;

            /* When we already found a nested comment, only accept further
             * nested comments. */
            if (got_com && vim_strchr(part_buf, COM_NEST) == NULL)
                continue;

            /* When 'O' flag present and using "O" command skip this one. */
            if (backward && vim_strchr(part_buf, COM_NOBACK) != NULL)
                continue;

            /* Line contents and string must match.
             * When string starts with white space, must have some white space
             * (but the amount does not need to match, there might be a mix of
             * TABs and spaces). */
            if (vim_iswhite(string[0]))
            {
                if (i == 0 || !vim_iswhite(line[i - 1]))
                    continue;  /* missing white space */
                while (vim_iswhite(string[0]))
                    ++string;
            }
            for (j = 0; string[j] != NUL && string[j] == line[i + j]; ++j)
                ;
            if (string[j] != NUL)
                continue;  /* string doesn't match */

            /* When 'b' flag used, there must be white space or an
             * end-of-line after the string in the line. */
            if (vim_strchr(part_buf, COM_BLANK) != NULL
                           && !vim_iswhite(line[i + j]) && line[i + j] != NUL)
                continue;

            /* We have found a match, stop searching unless this is a middle
             * comment. The middle comment can be a substring of the end
             * comment in which case it's better to return the length of the
             * end comment and its flags.  Thus we keep searching with middle
             * and end matches and use an end match if it matches better. */
            if (vim_strchr(part_buf, COM_MIDDLE) != NULL)
            {
                if (middle_match_len == 0)
                {
                    middle_match_len = j;
                    saved_flags = prev_list;
                }
                continue;
            }
            if (middle_match_len != 0 && j > middle_match_len)
                /* Use this match instead of the middle match, since it's a
                 * longer thus better match. */
                middle_match_len = 0;

            if (middle_match_len == 0)
                i += j;
            found_one = TRUE;
            break;
        }

        if (middle_match_len != 0)
        {
            /* Use the previously found middle match after failing to find a
             * match with an end. */
            if (!got_com && flags != NULL)
                *flags = saved_flags;
            i += middle_match_len;
            found_one = TRUE;
        }

        /* No match found, stop scanning. */
        if (!found_one)
            break;

        result = i;

        /* Include any trailing white space. */
        while (vim_iswhite(line[i]))
            ++i;

        if (include_space)
            result = i;

        /* If this comment doesn't nest, stop here. */
        got_com = TRUE;
        if (vim_strchr(part_buf, COM_NEST) == NULL)
            break;
    }
    return result;
}

/*
 * Return the offset at which the last comment in line starts. If there is no
 * comment in the whole line, -1 is returned.
 *
 * When "flags" is not null, it is set to point to the flags describing the
 * recognized comment leader.
 */
    int
get_last_leader_offset(line, flags)
    char_u      *line;
    char_u      **flags;
{
    int         result = -1;
    int         i, j;
    int         lower_check_bound = 0;
    char_u      *string;
    char_u      *com_leader;
    char_u      *com_flags;
    char_u      *list;
    int         found_one;
    char_u      part_buf[COM_MAX_LEN];  /* buffer for one option part */

    /*
     * Repeat to match several nested comment strings.
     */
    i = (int)STRLEN(line);
    while (--i >= lower_check_bound)
    {
        /*
         * scan through the 'comments' option for a match
         */
        found_one = FALSE;
        for (list = curbuf->b_p_com; *list; )
        {
            char_u *flags_save = list;

            /*
             * Get one option part into part_buf[].  Advance list to next one.
             * put string at start of string.
             */
            (void)copy_option_part(&list, part_buf, COM_MAX_LEN, ",");
            string = vim_strchr(part_buf, ':');
            if (string == NULL) /* If everything is fine, this cannot actually happen. */
            {
                continue;
            }
            *string++ = NUL;    /* Isolate flags from string. */
            com_leader = string;

            /*
             * Line contents and string must match.
             * When string starts with white space, must have some white space
             * (but the amount does not need to match, there might be a mix of TABs and spaces).
             */
            if (vim_iswhite(string[0]))
            {
                if (i == 0 || !vim_iswhite(line[i - 1]))
                    continue;
                while (vim_iswhite(string[0]))
                    ++string;
            }
            for (j = 0; string[j] != NUL && string[j] == line[i + j]; ++j)
                /* do nothing */;
            if (string[j] != NUL)
                continue;

            /*
             * When 'b' flag used, there must be white space or an
             * end-of-line after the string in the line.
             */
            if (vim_strchr(part_buf, COM_BLANK) != NULL
                    && !vim_iswhite(line[i + j]) && line[i + j] != NUL)
            {
                continue;
            }

            /*
             * We have found a match, stop searching.
             */
            found_one = TRUE;

            if (flags)
                *flags = flags_save;
            com_flags = flags_save;

            break;
        }

        if (found_one)
        {
            char_u  part_buf2[COM_MAX_LEN];     /* buffer for one option part */
            int     len1, len2, off;

            result = i;
            /*
             * If this comment nests, continue searching.
             */
            if (vim_strchr(part_buf, COM_NEST) != NULL)
                continue;

            lower_check_bound = i;

            /* Let's verify whether the comment leader found is a substring
             * of other comment leaders. If it is, let's adjust the
             * lower_check_bound so that we make sure that we have determined
             * the comment leader correctly.
             */

            while (vim_iswhite(*com_leader))
                ++com_leader;
            len1 = (int)STRLEN(com_leader);

            for (list = curbuf->b_p_com; *list; )
            {
                char_u *flags_save = list;

                (void)copy_option_part(&list, part_buf2, COM_MAX_LEN, ",");
                if (flags_save == com_flags)
                    continue;
                string = vim_strchr(part_buf2, ':');
                ++string;
                while (vim_iswhite(*string))
                    ++string;
                len2 = (int)STRLEN(string);
                if (len2 == 0)
                    continue;

                /* Now we have to verify whether string ends with a substring
                 * beginning the com_leader. */
                for (off = (len2 > i ? i : len2); off > 0 && off + len1 > len2;)
                {
                    --off;
                    if (!STRNCMP(string + off, com_leader, len2 - off))
                    {
                        if (i - off < lower_check_bound)
                            lower_check_bound = i - off;
                    }
                }
            }
        }
    }
    return result;
}

/*
 * Return the number of window lines occupied by buffer line "lnum".
 */
    int
plines(lnum)
    linenr_T    lnum;
{
    return plines_win(curwin, lnum, TRUE);
}

    int
plines_win(wp, lnum, winheight)
    win_T       *wp;
    linenr_T    lnum;
    int         winheight;      /* when TRUE limit to window height */
{
    int         lines;

    if (!wp->w_p_wrap)
        return 1;

    if (wp->w_width == 0)
        return 1;

    lines = plines_win_nofold(wp, lnum);
    if (winheight > 0 && lines > wp->w_height)
        return (int)wp->w_height;

    return lines;
}

/*
 * Return number of window lines physical line "lnum" will occupy in window
 * "wp".  Does not care about folding, 'wrap' or 'diff'.
 */
    int
plines_win_nofold(wp, lnum)
    win_T       *wp;
    linenr_T    lnum;
{
    char_u      *s;
    long        col;
    int         width;

    s = ml_get_buf(wp->w_buffer, lnum, FALSE);
    if (*s == NUL)              /* empty line */
        return 1;
    col = win_linetabsize(wp, s, (colnr_T)MAXCOL);

    /*
     * If list mode is on, then the '$' at the end of the line may take up one extra column.
     */
    if (wp->w_p_list && lcs_eol != NUL)
        col += 1;

    /*
     * Add column offset for 'number', 'relativenumber' and 'foldcolumn'.
     */
    width = W_WIDTH(wp) - win_col_off(wp);
    if (width <= 0)
        return 32000;
    if (col <= width)
        return 1;
    col -= width;
    width += win_col_off2(wp);
    return (col + (width - 1)) / width + 1;
}

/*
 * Like plines_win(), but only reports the number of physical screen lines
 * used from the start of the line to the given column number.
 */
    int
plines_win_col(wp, lnum, column)
    win_T       *wp;
    linenr_T    lnum;
    long        column;
{
    long        col;
    char_u      *s;
    int         lines = 0;
    int         width;
    char_u      *line;

    if (!wp->w_p_wrap)
        return lines + 1;

    if (wp->w_width == 0)
        return lines + 1;

    line = s = ml_get_buf(wp->w_buffer, lnum, FALSE);

    col = 0;
    while (*s != NUL && --column >= 0)
    {
        col += win_lbr_chartabsize(wp, line, s, (colnr_T)col, NULL);
        s += utfc_ptr2len(s);
    }

    /*
     * If *s is a TAB, and the TAB is not displayed as ^I, and we're not in
     * INSERT mode, then col must be adjusted so that it represents the last
     * screen position of the TAB.  This only fixes an error when the TAB wraps
     * from one screen line to the next (when 'columns' is not a multiple of
     * 'ts') -- webb.
     */
    if (*s == TAB && (State & NORMAL) && (!wp->w_p_list || lcs_tab1))
        col += win_lbr_chartabsize(wp, line, s, (colnr_T)col, NULL) - 1;

    /*
     * Add column offset for 'number', 'relativenumber', 'foldcolumn', etc.
     */
    width = W_WIDTH(wp) - win_col_off(wp);
    if (width <= 0)
        return 9999;

    lines += 1;
    if (col > width)
        lines += (col - width) / (width + win_col_off2(wp)) + 1;
    return lines;
}

    int
plines_m_win(wp, first, last)
    win_T       *wp;
    linenr_T    first, last;
{
    int         count = 0;

    while (first <= last)
    {
        count += plines_win(wp, first, TRUE);
        ++first;
    }
    return (count);
}

/*
 * Insert string "p" at the cursor position.  Stops at a NUL byte.
 * Handles Replace mode and multi-byte characters.
 */
    void
ins_bytes(p)
    char_u      *p;
{
    ins_bytes_len(p, (int)STRLEN(p));
}

/*
 * Insert string "p" with length "len" at the cursor position.
 * Handles Replace mode and multi-byte characters.
 */
    void
ins_bytes_len(p, len)
    char_u      *p;
    int         len;
{
    int         i;
    int         n;

    for (i = 0; i < len; i += n)
    {
        /* avoid reading past p[len] */
        n = utfc_ptr2len_len(p + i, len - i);
        ins_char_bytes(p + i, n);
    }
}

/*
 * Insert or replace a single character at the cursor position.
 * When in REPLACE or VREPLACE mode, replace any existing character.
 * Caller must have prepared for undo.
 * For multi-byte characters we get the whole character, the caller must
 * convert bytes to a character.
 */
    void
ins_char(c)
    int         c;
{
    char_u      buf[MB_MAXBYTES + 1];
    int         n;

    n = utf_char2bytes(c, buf);

    /* When "c" is 0x100, 0x200, etc. we don't want to insert a NUL byte.
     * Happens for CTRL-Vu9900. */
    if (buf[0] == 0)
        buf[0] = '\n';

    ins_char_bytes(buf, n);
}

    void
ins_char_bytes(buf, charlen)
    char_u      *buf;
    int         charlen;
{
    int         newlen;         /* nr of bytes inserted */
    int         oldlen;         /* nr of bytes deleted (0 when not replacing) */
    char_u      *p;
    char_u      *newp;
    char_u      *oldp;
    int         linelen;        /* length of old line including NUL */
    colnr_T     col;
    linenr_T    lnum = curwin->w_cursor.lnum;
    int         i;

    /* Break tabs if needed. */
    if (virtual_active() && curwin->w_cursor.coladd > 0)
        coladvance_force(getviscol());

    col = curwin->w_cursor.col;
    oldp = ml_get(lnum);
    linelen = (int)STRLEN(oldp) + 1;

    /* The lengths default to the values for when not replacing. */
    oldlen = 0;
    newlen = charlen;

    if (State & REPLACE_FLAG)
    {
        if (State & VREPLACE_FLAG)
        {
            colnr_T     new_vcol = 0;   /* init for GCC */
            colnr_T     vcol;
            int         old_list;

            /*
             * Disable 'list' temporarily, unless 'cpo' contains the 'L' flag.
             * Returns the old value of list, so when finished,
             * curwin->w_p_list should be set back to this.
             */
            old_list = curwin->w_p_list;
            if (old_list && vim_strchr(p_cpo, CPO_LISTWM) == NULL)
                curwin->w_p_list = FALSE;

            /*
             * In virtual replace mode each character may replace one or more
             * characters (zero if it's a TAB).  Count the number of bytes to
             * be deleted to make room for the new character, counting screen
             * cells.  May result in adding spaces to fill a gap.
             */
            getvcol(curwin, &curwin->w_cursor, NULL, &vcol, NULL);
            new_vcol = vcol + chartabsize(buf, vcol);
            while (oldp[col + oldlen] != NUL && vcol < new_vcol)
            {
                vcol += chartabsize(oldp + col + oldlen, vcol);
                /* Don't need to remove a TAB that takes us to the right position. */
                if (vcol > new_vcol && oldp[col + oldlen] == TAB)
                    break;
                oldlen += utfc_ptr2len(oldp + col + oldlen);
                /* Deleted a bit too much, insert spaces. */
                if (vcol > new_vcol)
                    newlen += vcol - new_vcol;
            }
            curwin->w_p_list = old_list;
        }
        else if (oldp[col] != NUL)
        {
            /* normal replace */
            oldlen = utfc_ptr2len(oldp + col);
        }

        /* Push the replaced bytes onto the replace stack, so that they can be
         * put back when BS is used.  The bytes of a multi-byte character are
         * done the other way around, so that the first byte is popped off
         * first (it tells the byte length of the character). */
        replace_push(NUL);
        for (i = 0; i < oldlen; ++i)
        {
            i += replace_push_mb(oldp + col + i) - 1;
        }
    }

    newp = alloc_check((unsigned)(linelen + newlen - oldlen));
    if (newp == NULL)
        return;

    /* Copy bytes before the cursor. */
    if (col > 0)
        mch_memmove(newp, oldp, (size_t)col);

    /* Copy bytes after the changed character(s). */
    p = newp + col;
    mch_memmove(p + newlen, oldp + col + oldlen, (size_t)(linelen - col - oldlen));

    /* Insert or overwrite the new character. */
    mch_memmove(p, buf, charlen);
    i = charlen;

    /* Fill with spaces when necessary. */
    while (i < newlen)
        p[i++] = ' ';

    /* Replace the line in the buffer. */
    ml_replace(lnum, newp, FALSE);

    /* mark the buffer as changed and prepare for displaying */
    changed_bytes(lnum, col);

    /*
     * If we're in Insert or Replace mode and 'showmatch' is set, then briefly
     * show the match for right parens and braces.
     */
    if (p_sm && (State & INSERT) && msg_silent == 0)
    {
        showmatch(utf_ptr2char(buf));
    }

    if (!p_ri || (State & REPLACE_FLAG))
    {
        /* Normal insert: move cursor right */
        curwin->w_cursor.col += charlen;
    }
    /*
     * TODO: should try to update w_row here, to avoid recomputing it later.
     */
}

/*
 * Insert a string at the cursor position.
 * Note: Does NOT handle Replace mode.
 * Caller must have prepared for undo.
 */
    void
ins_str(s)
    char_u      *s;
{
    char_u      *oldp, *newp;
    int         newlen = (int)STRLEN(s);
    int         oldlen;
    colnr_T     col;
    linenr_T    lnum = curwin->w_cursor.lnum;

    if (virtual_active() && curwin->w_cursor.coladd > 0)
        coladvance_force(getviscol());

    col = curwin->w_cursor.col;
    oldp = ml_get(lnum);
    oldlen = (int)STRLEN(oldp);

    newp = alloc_check((unsigned)(oldlen + newlen + 1));
    if (newp == NULL)
        return;
    if (col > 0)
        mch_memmove(newp, oldp, (size_t)col);
    mch_memmove(newp + col, s, (size_t)newlen);
    mch_memmove(newp + col + newlen, oldp + col, (size_t)(oldlen - col + 1));
    ml_replace(lnum, newp, FALSE);
    changed_bytes(lnum, col);
    curwin->w_cursor.col += newlen;
}

/*
 * Delete one character under the cursor.
 * If "fixpos" is TRUE, don't leave the cursor on the NUL after the line.
 * Caller must have prepared for undo.
 *
 * return FAIL for failure, OK otherwise
 */
    int
del_char(fixpos)
    int         fixpos;
{
    /* Make sure the cursor is at the start of a character. */
    mb_adjust_cursor();
    if (*ml_get_cursor() == NUL)
        return FAIL;

    return del_chars(1L, fixpos);
}

/*
 * Like del_bytes(), but delete characters instead of bytes.
 */
    int
del_chars(count, fixpos)
    long        count;
    int         fixpos;
{
    long        bytes = 0;
    long        i;
    char_u      *p;
    int         l;

    p = ml_get_cursor();
    for (i = 0; i < count && *p != NUL; ++i)
    {
        l = utfc_ptr2len(p);
        bytes += l;
        p += l;
    }
    return del_bytes(bytes, fixpos, TRUE);
}

/*
 * Delete "count" bytes under the cursor.
 * If "fixpos" is TRUE, don't leave the cursor on the NUL after the line.
 * Caller must have prepared for undo.
 *
 * return FAIL for failure, OK otherwise
 */
    int
del_bytes(count, fixpos_arg, use_delcombine)
    long        count;
    int         fixpos_arg;
    int         use_delcombine UNUSED;      /* 'delcombine' option applies */
{
    char_u      *oldp, *newp;
    colnr_T     oldlen;
    linenr_T    lnum = curwin->w_cursor.lnum;
    colnr_T     col = curwin->w_cursor.col;
    int         was_alloced;
    long        movelen;
    int         fixpos = fixpos_arg;

    oldp = ml_get(lnum);
    oldlen = (int)STRLEN(oldp);

    /*
     * Can't do anything when the cursor is on the NUL after the line.
     */
    if (col >= oldlen)
        return FAIL;

    /* If 'delcombine' is set and deleting (less than) one character, only
     * delete the last combining character. */
    if (p_deco && use_delcombine && utfc_ptr2len(oldp + col) >= count)
    {
        int     cc[MAX_MCO];
        int     n;

        (void)utfc_ptr2char(oldp + col, cc);
        if (cc[0] != NUL)
        {
            /* Find the last composing char, there can be several. */
            n = col;
            do
            {
                col = n;
                count = utf_ptr2len(oldp + n);
                n += count;
            } while (UTF_COMPOSINGLIKE(oldp + col, oldp + n));
            fixpos = 0;
        }
    }

    /*
     * When count is too big, reduce it.
     */
    movelen = (long)oldlen - (long)col - count + 1; /* includes trailing NUL */
    if (movelen <= 1)
    {
        /*
         * If we just took off the last character of a non-blank line, and
         * fixpos is TRUE, we don't want to end up positioned at the NUL,
         * unless "restart_edit" is set or 'virtualedit' contains "onemore".
         */
        if (col > 0 && fixpos && restart_edit == 0 && (ve_flags & VE_ONEMORE) == 0)
        {
            --curwin->w_cursor.col;
            curwin->w_cursor.coladd = 0;
            curwin->w_cursor.col -= utf_head_off(oldp, oldp + curwin->w_cursor.col);
        }
        count = oldlen - col;
        movelen = 1;
    }

    /*
     * If the old line has been allocated the deletion can be done in the
     * existing line. Otherwise a new line has to be allocated
     * Can't do this when using Netbeans, because we would need to invoke
     * netbeans_removed(), which deallocates the line.  Let ml_replace() take
     * care of notifying Netbeans.
     */
        was_alloced = ml_line_alloced();    /* check if oldp was allocated */
    if (was_alloced)
        newp = oldp;                        /* use same allocated memory */
    else
    {                                       /* need to allocate a new line */
        newp = alloc((unsigned)(oldlen + 1 - count));
        if (newp == NULL)
            return FAIL;
        mch_memmove(newp, oldp, (size_t)col);
    }
    mch_memmove(newp + col, oldp + col + count, (size_t)movelen);
    if (!was_alloced)
        ml_replace(lnum, newp, FALSE);

    /* mark the buffer as changed and prepare for displaying */
    changed_bytes(lnum, curwin->w_cursor.col);

    return OK;
}

/*
 * Delete from cursor to end of line.
 * Caller must have prepared for undo.
 *
 * return FAIL for failure, OK otherwise
 */
    int
truncate_line(fixpos)
    int         fixpos;     /* if TRUE fix the cursor position when done */
{
    char_u      *newp;
    linenr_T    lnum = curwin->w_cursor.lnum;
    colnr_T     col = curwin->w_cursor.col;

    if (col == 0)
        newp = vim_strsave((char_u *)"");
    else
        newp = vim_strnsave(ml_get(lnum), col);

    if (newp == NULL)
        return FAIL;

    ml_replace(lnum, newp, FALSE);

    /* mark the buffer as changed and prepare for displaying */
    changed_bytes(lnum, curwin->w_cursor.col);

    /*
     * If "fixpos" is TRUE we don't want to end up positioned at the NUL.
     */
    if (fixpos && curwin->w_cursor.col > 0)
        --curwin->w_cursor.col;

    return OK;
}

/*
 * Delete "nlines" lines at the cursor.
 * Saves the lines for undo first if "undo" is TRUE.
 */
    void
del_lines(nlines, undo)
    long        nlines;         /* number of lines to delete */
    int         undo;           /* if TRUE, prepare for undo */
{
    long        n;
    linenr_T    first = curwin->w_cursor.lnum;

    if (nlines <= 0)
        return;

    /* save the deleted lines for undo */
    if (undo && u_savedel(first, nlines) == FAIL)
        return;

    for (n = 0; n < nlines; )
    {
        if (curbuf->b_ml.ml_flags & ML_EMPTY)       /* nothing to delete */
            break;

        ml_delete(first, TRUE);
        ++n;

        /* If we delete the last line in the file, stop */
        if (first > curbuf->b_ml.ml_line_count)
            break;
    }

    /* Correct the cursor position before calling deleted_lines_mark(), it may
     * trigger a callback to display the cursor. */
    curwin->w_cursor.col = 0;
    check_cursor_lnum();

    /* adjust marks, mark the buffer as changed and prepare for displaying */
    deleted_lines_mark(first, n);
}

    int
gchar_pos(pos)
    pos_T *pos;
{
    char_u      *ptr = ml_get_pos(pos);

    return utf_ptr2char(ptr);
}

    int
gchar_cursor()
{
    return utf_ptr2char(ml_get_cursor());
}

/*
 * Write a character at the current cursor position.
 * It is directly written into the block.
 */
    void
pchar_cursor(c)
    int c;
{
    *(ml_get_buf(curbuf, curwin->w_cursor.lnum, TRUE) + curwin->w_cursor.col) = c;
}

/*
 * When extra == 0: Return TRUE if the cursor is before or on the first
 *                  non-blank in the line.
 * When extra == 1: Return TRUE if the cursor is before the first non-blank in
 *                  the line.
 */
    int
inindent(extra)
    int     extra;
{
    char_u      *ptr;
    colnr_T     col;

    for (col = 0, ptr = ml_get_curline(); vim_iswhite(*ptr); ++col)
        ++ptr;
    if (col >= curwin->w_cursor.col + extra)
        return TRUE;
    else
        return FALSE;
}

/*
 * Skip to next part of an option argument: Skip space and comma.
 */
    char_u *
skip_to_option_part(p)
    char_u  *p;
{
    if (*p == ',')
        ++p;
    while (*p == ' ')
        ++p;
    return p;
}

/*
 * Call this function when something in the current buffer is changed.
 *
 * Most often called through changed_bytes() and changed_lines(), which also
 * mark the area of the display to be redrawn.
 *
 * Careful: may trigger autocommands that reload the buffer.
 */
    void
changed()
{
    if (!curbuf->b_changed)
    {
        int     save_msg_scroll = msg_scroll;

        /* Give a warning about changing a read-only file.  This may also
         * check-out the file, thus change "curbuf"! */
        change_warning(0);

        /* Create a swap file if that is wanted.
         * Don't do this for "nofile" and "nowrite" buffer types. */
        if (curbuf->b_may_swap)
        {
            ml_open_file(curbuf);

            /* The ml_open_file() can cause an ATTENTION message.
             * Wait two seconds, to make sure the user reads this unexpected
             * message.  Since we could be anywhere, call wait_return() now,
             * and don't let the emsg() set msg_scroll. */
            if (need_wait_return && emsg_silent == 0)
            {
                out_flush();
                ui_delay(2000L, TRUE);
                wait_return(TRUE);
                msg_scroll = save_msg_scroll;
            }
        }
        changed_int();
    }
    ++curbuf->b_changedtick;
}

/*
 * Internal part of changed(), no user interaction.
 */
    void
changed_int()
{
    curbuf->b_changed = TRUE;
    ml_setflags(curbuf);
    check_status(curbuf);
    redraw_tabline = TRUE;
    need_maketitle = TRUE;          /* set window title later */
}

static void changedOneline(buf_T *buf, linenr_T lnum);
static void changed_lines_buf(buf_T *buf, linenr_T lnum, linenr_T lnume, long xtra);
static void changed_common(linenr_T lnum, colnr_T col, linenr_T lnume, long xtra);

/*
 * Changed bytes within a single line for the current buffer.
 * - marks the windows on this buffer to be redisplayed
 * - marks the buffer changed by calling changed()
 * - invalidates cached values
 * Careful: may trigger autocommands that reload the buffer.
 */
    void
changed_bytes(lnum, col)
    linenr_T    lnum;
    colnr_T     col;
{
    changedOneline(curbuf, lnum);
    changed_common(lnum, col, lnum + 1, 0L);
}

    static void
changedOneline(buf, lnum)
    buf_T       *buf;
    linenr_T    lnum;
{
    if (buf->b_mod_set)
    {
        /* find the maximum area that must be redisplayed */
        if (lnum < buf->b_mod_top)
            buf->b_mod_top = lnum;
        else if (lnum >= buf->b_mod_bot)
            buf->b_mod_bot = lnum + 1;
    }
    else
    {
        /* set the area that must be redisplayed to one line */
        buf->b_mod_set = TRUE;
        buf->b_mod_top = lnum;
        buf->b_mod_bot = lnum + 1;
        buf->b_mod_xlines = 0;
    }
}

/*
 * Appended "count" lines below line "lnum" in the current buffer.
 * Must be called AFTER the change and after mark_adjust().
 * Takes care of marking the buffer to be redrawn and sets the changed flag.
 */
    void
appended_lines(lnum, count)
    linenr_T    lnum;
    long        count;
{
    changed_lines(lnum + 1, 0, lnum + 1, count);
}

/*
 * Like appended_lines(), but adjust marks first.
 */
    void
appended_lines_mark(lnum, count)
    linenr_T    lnum;
    long        count;
{
    mark_adjust(lnum + 1, (linenr_T)MAXLNUM, count, 0L);
    changed_lines(lnum + 1, 0, lnum + 1, count);
}

/*
 * Deleted "count" lines at line "lnum" in the current buffer.
 * Must be called AFTER the change and after mark_adjust().
 * Takes care of marking the buffer to be redrawn and sets the changed flag.
 */
    void
deleted_lines(lnum, count)
    linenr_T    lnum;
    long        count;
{
    changed_lines(lnum, 0, lnum + count, -count);
}

/*
 * Like deleted_lines(), but adjust marks first.
 * Make sure the cursor is on a valid line before calling, a GUI callback may
 * be triggered to display the cursor.
 */
    void
deleted_lines_mark(lnum, count)
    linenr_T    lnum;
    long        count;
{
    mark_adjust(lnum, (linenr_T)(lnum + count - 1), (long)MAXLNUM, -count);
    changed_lines(lnum, 0, lnum + count, -count);
}

/*
 * Changed lines for the current buffer.
 * Must be called AFTER the change and after mark_adjust().
 * - mark the buffer changed by calling changed()
 * - mark the windows on this buffer to be redisplayed
 * - invalidate cached values
 * "lnum" is the first line that needs displaying, "lnume" the first line
 * below the changed lines (BEFORE the change).
 * When only inserting lines, "lnum" and "lnume" are equal.
 * Takes care of calling changed() and updating b_mod_*.
 * Careful: may trigger autocommands that reload the buffer.
 */
    void
changed_lines(lnum, col, lnume, xtra)
    linenr_T    lnum;       /* first line with change */
    colnr_T     col;        /* column in first line with change */
    linenr_T    lnume;      /* line below last changed line */
    long        xtra;       /* number of extra lines (negative when deleting) */
{
    changed_lines_buf(curbuf, lnum, lnume, xtra);

    changed_common(lnum, col, lnume, xtra);
}

    static void
changed_lines_buf(buf, lnum, lnume, xtra)
    buf_T       *buf;
    linenr_T    lnum;       /* first line with change */
    linenr_T    lnume;      /* line below last changed line */
    long        xtra;       /* number of extra lines (negative when deleting) */
{
    if (buf->b_mod_set)
    {
        /* find the maximum area that must be redisplayed */
        if (lnum < buf->b_mod_top)
            buf->b_mod_top = lnum;
        if (lnum < buf->b_mod_bot)
        {
            /* adjust old bot position for xtra lines */
            buf->b_mod_bot += xtra;
            if (buf->b_mod_bot < lnum)
                buf->b_mod_bot = lnum;
        }
        if (lnume + xtra > buf->b_mod_bot)
            buf->b_mod_bot = lnume + xtra;
        buf->b_mod_xlines += xtra;
    }
    else
    {
        /* set the area that must be redisplayed */
        buf->b_mod_set = TRUE;
        buf->b_mod_top = lnum;
        buf->b_mod_bot = lnume + xtra;
        buf->b_mod_xlines = xtra;
    }
}

/*
 * Common code for when a change is was made.
 * See changed_lines() for the arguments.
 * Careful: may trigger autocommands that reload the buffer.
 */
    static void
changed_common(lnum, col, lnume, xtra)
    linenr_T    lnum;
    colnr_T     col;
    linenr_T    lnume;
    long        xtra;
{
    win_T       *wp;
    tabpage_T   *tp;
    int         i;
    int         cols;
    pos_T       *p;
    int         add;

    /* mark the buffer as modified */
    changed();

    /* set the '. mark */
    if (!cmdmod.keepjumps)
    {
        curbuf->b_last_change.lnum = lnum;
        curbuf->b_last_change.col = col;

        /* Create a new entry if a new undo-able change was started or we
         * don't have an entry yet. */
        if (curbuf->b_new_change || curbuf->b_changelistlen == 0)
        {
            if (curbuf->b_changelistlen == 0)
                add = TRUE;
            else
            {
                /* Don't create a new entry when the line number is the same
                 * as the last one and the column is not too far away.  Avoids
                 * creating many entries for typing "xxxxx". */
                p = &curbuf->b_changelist[curbuf->b_changelistlen - 1];
                if (p->lnum != lnum)
                    add = TRUE;
                else
                {
                    cols = comp_textwidth(FALSE);
                    if (cols == 0)
                        cols = 79;
                    add = (p->col + cols < col || col + cols < p->col);
                }
            }
            if (add)
            {
                /* This is the first of a new sequence of undo-able changes
                 * and it's at some distance of the last change.  Use a new
                 * position in the changelist. */
                curbuf->b_new_change = FALSE;

                if (curbuf->b_changelistlen == JUMPLISTSIZE)
                {
                    /* changelist is full: remove oldest entry */
                    curbuf->b_changelistlen = JUMPLISTSIZE - 1;
                    mch_memmove(curbuf->b_changelist, curbuf->b_changelist + 1,
                                          sizeof(pos_T) * (JUMPLISTSIZE - 1));
                    FOR_ALL_TAB_WINDOWS(tp, wp)
                    {
                        /* Correct position in changelist for other windows on this buffer. */
                        if (wp->w_buffer == curbuf && wp->w_changelistidx > 0)
                            --wp->w_changelistidx;
                    }
                }
                FOR_ALL_TAB_WINDOWS(tp, wp)
                {
                    /* For other windows, if the position in the changelist is
                     * at the end it stays at the end. */
                    if (wp->w_buffer == curbuf && wp->w_changelistidx == curbuf->b_changelistlen)
                        ++wp->w_changelistidx;
                }
                ++curbuf->b_changelistlen;
            }
        }
        curbuf->b_changelist[curbuf->b_changelistlen - 1] = curbuf->b_last_change;
        /* The current window is always after the last change, so that "g,"
         * takes you back to it. */
        curwin->w_changelistidx = curbuf->b_changelistlen;
    }

    FOR_ALL_TAB_WINDOWS(tp, wp)
    {
        if (wp->w_buffer == curbuf)
        {
            /* Mark this window to be redrawn later. */
            if (wp->w_redr_type < VALID)
                wp->w_redr_type = VALID;

            /* Check if a change in the buffer has invalidated the cached
             * values for the cursor. */

            if (wp->w_cursor.lnum > lnum)
                changed_line_abv_curs_win(wp);
            else if (wp->w_cursor.lnum == lnum && wp->w_cursor.col >= col)
                changed_cline_bef_curs_win(wp);
            if (wp->w_botline >= lnum)
            {
                /* Assume that botline doesn't change (inserted lines make
                 * other lines scroll down below botline). */
                approximate_botline_win(wp);
            }

            /* Check if any w_lines[] entries have become invalid.
             * For entries below the change: Correct the lnums for
             * inserted/deleted lines.  Makes it possible to stop displaying
             * after the change. */
            for (i = 0; i < wp->w_lines_valid; ++i)
                if (wp->w_lines[i].wl_valid)
                {
                    if (wp->w_lines[i].wl_lnum >= lnum)
                    {
                        if (wp->w_lines[i].wl_lnum < lnume)
                        {
                            /* line included in change */
                            wp->w_lines[i].wl_valid = FALSE;
                        }
                        else if (xtra != 0)
                        {
                            /* line below change */
                            wp->w_lines[i].wl_lnum += xtra;
                        }
                    }
                }

            /* relative numbering may require updating more */
            if (wp->w_p_rnu)
                redraw_win_later(wp, SOME_VALID);
        }
    }

    /* Call update_screen() later, which checks out what needs to be redrawn,
     * since it notices b_mod_set and then uses b_mod_*. */
    if (must_redraw < VALID)
        must_redraw = VALID;

    /* when the cursor line is changed always trigger CursorMoved */
    if (lnum <= curwin->w_cursor.lnum && lnume + (xtra < 0 ? -xtra : xtra) > curwin->w_cursor.lnum)
        last_cursormoved.lnum = 0;
}

/*
 * unchanged() is called when the changed flag must be reset for buffer 'buf'
 */
    void
unchanged(buf, ff)
    buf_T       *buf;
    int         ff;     /* also reset 'fileformat' */
{
    if (buf->b_changed || (ff && file_ff_differs(buf, FALSE)))
    {
        buf->b_changed = 0;
        ml_setflags(buf);
        if (ff)
            save_file_ff(buf);
        check_status(buf);
        redraw_tabline = TRUE;
        need_maketitle = TRUE;      /* set window title later */
    }
    ++buf->b_changedtick;
}

/*
 * check_status: called when the status bars for the buffer 'buf'
 *               need to be updated
 */
    void
check_status(buf)
    buf_T       *buf;
{
    win_T       *wp;

    for (wp = firstwin; wp != NULL; wp = wp->w_next)
        if (wp->w_buffer == buf && wp->w_status_height)
        {
            wp->w_redr_status = TRUE;
            if (must_redraw < VALID)
                must_redraw = VALID;
        }
}

/*
 * If the file is readonly, give a warning message with the first change.
 * Don't do this for autocommands.
 * Don't use emsg(), because it flushes the macro buffer.
 * If we have undone all changes b_changed will be FALSE, but "b_did_warn"
 * will be TRUE.
 * Careful: may trigger autocommands that reload the buffer.
 */
    void
change_warning(col)
    int     col;                /* column for message; non-zero when in insert
                                   mode and 'showmode' is on */
{
    static char *w_readonly = "W10: Warning: Changing a readonly file";

    if (curbuf->b_did_warn == FALSE
            && curbufIsChanged() == 0
            && !autocmd_busy
            && curbuf->b_p_ro)
    {
        ++curbuf_lock;
        apply_autocmds(EVENT_FILECHANGEDRO, NULL, NULL, FALSE, curbuf);
        --curbuf_lock;
        if (!curbuf->b_p_ro)
            return;
        /*
         * Do what msg() does, but with a column offset if the warning should
         * be after the mode message.
         */
        msg_start();
        if (msg_row == Rows - 1)
            msg_col = col;
        msg_source(hl_attr(HLF_W));
        MSG_PUTS_ATTR((char *)w_readonly, hl_attr(HLF_W) | MSG_HIST);
        set_vim_var_string(VV_WARNINGMSG, (char_u *)w_readonly, -1);
        msg_clr_eos();
        (void)msg_end();
        if (msg_silent == 0 && !silent_mode)
        {
            out_flush();
            ui_delay(1000L, TRUE); /* give the user time to think about it */
        }
        curbuf->b_did_warn = TRUE;
        redraw_cmdline = FALSE; /* don't redraw and erase the message */
        if (msg_row < Rows - 1)
            showmode();
    }
}

/*
 * Ask for a reply from the user, a 'y' or a 'n'.
 * No other characters are accepted, the message is repeated until a valid
 * reply is entered or CTRL-C is hit.
 * If direct is TRUE, don't use vgetc() but ui_inchar(), don't get characters
 * from any buffers but directly from the user.
 *
 * return the 'y' or 'n'
 */
    int
ask_yesno(str, direct)
    char_u  *str;
    int     direct;
{
    int     r = ' ';
    int     save_State = State;

    if (exiting)                /* put terminal in raw mode for this question */
        settmode(TMODE_RAW);
    ++no_wait_return;
    State = CONFIRM;            /* mouse behaves like with :confirm */
    setmouse();                 /* disables mouse for xterm */
    ++no_mapping;
    ++allow_keys;               /* no mapping here, but recognize keys */

    while (r != 'y' && r != 'n')
    {
        /* same highlighting as for wait_return */
        smsg_attr(hl_attr(HLF_R), (char_u *)"%s (y/n)?", str);
        if (direct)
            r = get_keystroke();
        else
            r = plain_vgetc();
        if (r == Ctrl_C || r == ESC)
            r = 'n';
        msg_putchar(r);     /* show what you typed */
        out_flush();
    }
    --no_wait_return;
    State = save_State;
    setmouse();
    --no_mapping;
    --allow_keys;

    return r;
}

/*
 * Return TRUE if "c" is a mouse key.
 */
    int
is_mouse_key(c)
    int c;
{
    return c == K_LEFTMOUSE
        || c == K_LEFTMOUSE_NM
        || c == K_LEFTDRAG
        || c == K_LEFTRELEASE
        || c == K_LEFTRELEASE_NM
        || c == K_MIDDLEMOUSE
        || c == K_MIDDLEDRAG
        || c == K_MIDDLERELEASE
        || c == K_RIGHTMOUSE
        || c == K_RIGHTDRAG
        || c == K_RIGHTRELEASE
        || c == K_MOUSEDOWN
        || c == K_MOUSEUP
        || c == K_MOUSELEFT
        || c == K_MOUSERIGHT
        || c == K_X1MOUSE
        || c == K_X1DRAG
        || c == K_X1RELEASE
        || c == K_X2MOUSE
        || c == K_X2DRAG
        || c == K_X2RELEASE;
}

/*
 * Get a key stroke directly from the user.
 * Ignores mouse clicks and scrollbar events, except a click for the left
 * button (used at the more prompt).
 * Doesn't use vgetc(), because it syncs undo and eats mapped characters.
 * Disadvantage: typeahead is ignored.
 * Translates the interrupt character for unix to ESC.
 */
    int
get_keystroke()
{
    char_u      *buf = NULL;
    int         buflen = 150;
    int         maxlen;
    int         len = 0;
    int         n;
    int         save_mapped_ctrl_c = mapped_ctrl_c;
    int         waited = 0;

    mapped_ctrl_c = FALSE;      /* mappings are not used here */
    for (;;)
    {
        cursor_on();
        out_flush();

        /* Leave some room for check_termcode() to insert a key code into (max
         * 5 chars plus NUL).  And fix_input_buffer() can triple the number of bytes. */
        maxlen = (buflen - 6 - len) / 3;
        if (buf == NULL)
            buf = alloc(buflen);
        else if (maxlen < 10)
        {
            char_u  *t_buf = buf;

            /* Need some more space. This might happen when receiving a long
             * escape sequence. */
            buflen += 100;
            buf = realloc(buf, buflen);
            if (buf == NULL)
                vim_free(t_buf);
            maxlen = (buflen - 6 - len) / 3;
        }
        if (buf == NULL)
        {
            do_outofmem_msg((long_u)buflen);
            return ESC;  /* panic! */
        }

        /* First time: blocking wait.  Second time: wait up to 100ms for a
         * terminal code to complete. */
        n = ui_inchar(buf + len, maxlen, len == 0 ? -1L : 100L, 0);
        if (n > 0)
        {
            /* Replace zero and CSI by a special key code. */
            n = fix_input_buffer(buf + len, n, FALSE);
            len += n;
            waited = 0;
        }
        else if (len > 0)
            ++waited;       /* keep track of the waiting time */

        /* Incomplete termcode and not timed out yet: get more characters */
        if ((n = check_termcode(1, buf, buflen, &len)) < 0
               && (!p_ttimeout || waited * 100L < (p_ttm < 0 ? p_tm : p_ttm)))
            continue;

        if (n == KEYLEN_REMOVED)  /* key code removed */
        {
            if (must_redraw != 0 && !need_wait_return && (State & CMDLINE) == 0)
            {
                /* Redrawing was postponed, do it now. */
                update_screen(0);
                setcursor(); /* put cursor back where it belongs */
            }
            continue;
        }
        if (n > 0)              /* found a termcode: adjust length */
            len = n;
        if (len == 0)           /* nothing typed yet */
            continue;

        /* Handle modifier and/or special key code. */
        n = buf[0];
        if (n == K_SPECIAL)
        {
            n = TO_SPECIAL(buf[1], buf[2]);
            if (buf[1] == KS_MODIFIER
                    || n == K_IGNORE
                    || (is_mouse_key(n) && n != K_LEFTMOUSE)
               )
            {
                if (buf[1] == KS_MODIFIER)
                    mod_mask = buf[2];
                len -= 3;
                if (len > 0)
                    mch_memmove(buf, buf + 3, (size_t)len);
                continue;
            }
            break;
        }

        if (MB_BYTE2LEN(n) > len)
            continue;       /* more bytes to get */
        buf[len >= buflen ? buflen - 1 : len] = NUL;
        n = utf_ptr2char(buf);

        if (n == intr_char)
            n = ESC;
        break;
    }
    vim_free(buf);

    mapped_ctrl_c = save_mapped_ctrl_c;
    return n;
}

/*
 * Get a number from the user.
 * When "mouse_used" is not NULL allow using the mouse.
 */
    int
get_number(colon, mouse_used)
    int     colon;                      /* allow colon to abort */
    int     *mouse_used;
{
    int n = 0;
    int c;
    int typed = 0;

    if (mouse_used != NULL)
        *mouse_used = FALSE;

    /* When not printing messages, the user won't know what to type, return a
     * zero (as if CR was hit). */
    if (msg_silent != 0)
        return 0;

    ++no_mapping;
    ++allow_keys;               /* no mapping here, but recognize keys */
    for (;;)
    {
        windgoto(msg_row, msg_col);
        c = safe_vgetc();
        if (VIM_ISDIGIT(c))
        {
            n = n * 10 + c - '0';
            msg_putchar(c);
            ++typed;
        }
        else if (c == K_DEL || c == K_KDEL || c == K_BS || c == Ctrl_H)
        {
            if (typed > 0)
            {
                MSG_PUTS("\b \b");
                --typed;
            }
            n /= 10;
        }
        else if (mouse_used != NULL && c == K_LEFTMOUSE)
        {
            *mouse_used = TRUE;
            n = mouse_row + 1;
            break;
        }
        else if (n == 0 && c == ':' && colon)
        {
            stuffcharReadbuff(':');
            if (!exmode_active)
                cmdline_row = msg_row;
            skip_redraw = TRUE;     /* skip redraw once */
            do_redraw = FALSE;
            break;
        }
        else if (c == CAR || c == NL || c == Ctrl_C || c == ESC)
            break;
    }
    --no_mapping;
    --allow_keys;
    return n;
}

/*
 * Ask the user to enter a number.
 * When "mouse_used" is not NULL allow using the mouse and in that case return the line number.
 */
    int
prompt_for_number(mouse_used)
    int         *mouse_used;
{
    int         i;
    int         save_cmdline_row;
    int         save_State;

    /* When using ":silent" assume that <CR> was entered. */
    if (mouse_used != NULL)
        MSG_PUTS("Type number and <Enter> or click with mouse (empty cancels): ");
    else
        MSG_PUTS("Type number and <Enter> (empty cancels): ");

    /* Set the state such that text can be selected/copied/pasted and we still
     * get mouse events. */
    save_cmdline_row = cmdline_row;
    cmdline_row = 0;
    save_State = State;
    State = CMDLINE;

    i = get_number(TRUE, mouse_used);
    if (KeyTyped)
    {
        /* don't call wait_return() now */
        /* msg_putchar('\n'); */
        cmdline_row = msg_row - 1;
        need_wait_return = FALSE;
        msg_didany = FALSE;
        msg_didout = FALSE;
    }
    else
        cmdline_row = save_cmdline_row;
    State = save_State;

    return i;
}

    void
msgmore(n)
    long n;
{
    long pn;

    if (global_busy         /* no messages now, wait until global is finished */
            || !messaging())  /* 'lazyredraw' set, don't do messages now */
        return;

    /* We don't want to overwrite another important message, but do overwrite
     * a previous "more lines" or "fewer lines" message, so that "5dd" and
     * then "put" reports the last action. */
    if (keep_msg != NULL && !keep_msg_more)
        return;

    if (n > 0)
        pn = n;
    else
        pn = -n;

    if (pn > p_report)
    {
        if (pn == 1)
        {
            if (n > 0)
                vim_strncpy(msg_buf, (char_u *)"1 more line", MSG_BUF_LEN - 1);
            else
                vim_strncpy(msg_buf, (char_u *)"1 line less", MSG_BUF_LEN - 1);
        }
        else
        {
            if (n > 0)
                vim_snprintf((char *)msg_buf, MSG_BUF_LEN, "%ld more lines", pn);
            else
                vim_snprintf((char *)msg_buf, MSG_BUF_LEN, "%ld fewer lines", pn);
        }
        if (got_int)
            vim_strcat(msg_buf, (char_u *)" (Interrupted)", MSG_BUF_LEN);
        if (msg(msg_buf))
        {
            set_keep_msg(msg_buf, 0);
            keep_msg_more = TRUE;
        }
    }
}

/*
 * flush map and typeahead buffers and give a warning for an error
 */
    void
beep_flush()
{
    if (emsg_silent == 0)
    {
        flush_buffers(FALSE);
        vim_beep();
    }
}

/*
 * give a warning for an error
 */
    void
vim_beep()
{
    if (emsg_silent == 0)
    {
        if (p_vb)
        {
            out_str(T_VB);
        }
        else
        {
            out_char(BELL);
        }

        /* When 'verbose' is set and we are sourcing a script or executing a
         * function give the user a hint where the beep comes from. */
        if (vim_strchr(p_debug, 'e') != NULL)
        {
            msg_source(hl_attr(HLF_W));
            msg_attr((char_u *)"Beep!", hl_attr(HLF_W));
        }
    }
}

/*
 * To get the "real" home directory:
 * - get value of $HOME
 * For Unix:
 *  - go to that directory
 *  - do mch_dirname() to get the real name of that directory.
 *  This also works with mounts and links.
 *  Don't do this for MS-DOS, it will change the "current dir" for a drive.
 */
static char_u   *homedir = NULL;

    void
init_homedir()
{
    char_u  *var;

    /* In case we are called a second time (when 'encoding' changes). */
    vim_free(homedir);
    homedir = NULL;

    var = mch_getenv((char_u *)"HOME");

    if (var != NULL && *var == NUL)     /* empty is same as not set */
        var = NULL;

    if (var != NULL)
    {
        /*
         * Change to the directory and get the actual path.  This resolves
         * links.  Don't do it when we can't return.
         */
        if (mch_dirname(NameBuff, MAXPATHL) == OK && mch_chdir((char *)NameBuff) == 0)
        {
            if (!mch_chdir((char *)var) && mch_dirname(IObuff, IOSIZE) == OK)
                var = IObuff;
            if (mch_chdir((char *)NameBuff) != 0)
                EMSG((char *)e_prev_dir);
        }
        homedir = vim_strsave(var);
    }
}

/*
 * Call expand_env() and store the result in an allocated string.
 * This is not very memory efficient, this expects the result to be freed again soon.
 */
    char_u *
expand_env_save(src)
    char_u      *src;
{
    return expand_env_save_opt(src, FALSE);
}

/*
 * Idem, but when "one" is TRUE handle the string as one file name, only
 * expand "~" at the start.
 */
    char_u *
expand_env_save_opt(src, one)
    char_u      *src;
    int         one;
{
    char_u      *p;

    p = alloc(MAXPATHL);
    if (p != NULL)
        expand_env_esc(src, p, MAXPATHL, FALSE, one, NULL);
    return p;
}

/*
 * Expand environment variable with path name.
 * "~/" is also expanded, using $HOME.  For Unix "~user/" is expanded.
 * Skips over "\ ", "\~" and "\$" (not for Win32 though).
 * If anything fails no expansion is done and dst equals src.
 */
    void
expand_env(src, dst, dstlen)
    char_u      *src;           /* input string e.g. "$HOME/vim.hlp" */
    char_u      *dst;           /* where to put the result */
    int         dstlen;         /* maximum length of the result */
{
    expand_env_esc(src, dst, dstlen, FALSE, FALSE, NULL);
}

    void
expand_env_esc(srcp, dst, dstlen, esc, one, startstr)
    char_u      *srcp;          /* input string e.g. "$HOME/vim.hlp" */
    char_u      *dst;           /* where to put the result */
    int         dstlen;         /* maximum length of the result */
    int         esc;            /* escape spaces in expanded variables */
    int         one;            /* "srcp" is one file name */
    char_u      *startstr;      /* start again after this (can be NULL) */
{
    char_u      *src;
    char_u      *tail;
    int         c;
    char_u      *var;
    int         copy_char;
    int         mustfree;       /* var was allocated, need to free it later */
    int         at_start = TRUE; /* at start of a name */
    int         startstr_len = 0;

    if (startstr != NULL)
        startstr_len = (int)STRLEN(startstr);

    src = skipwhite(srcp);
    --dstlen;               /* leave one char space for "\," */
    while (*src && dstlen > 0)
    {
        copy_char = TRUE;
        if ((*src == '$') || (*src == '~' && at_start))
        {
            mustfree = FALSE;

            /*
             * The variable name is copied into dst temporarily, because it may
             * be a string in read-only memory and a NUL needs to be appended.
             */
            if (*src != '~')                            /* environment var */
            {
                tail = src + 1;
                var = dst;
                c = dstlen - 1;

                /* Unix has ${var-name} type environment vars */
                if (*tail == '{' && !vim_isIDc('{'))
                {
                    tail++;     /* ignore '{' */
                    while (c-- > 0 && *tail && *tail != '}')
                        *var++ = *tail++;
                }
                else
                {
                    while (c-- > 0 && *tail != NUL && (vim_isIDc(*tail)))
                        *var++ = *tail++;
                }

                if (src[1] == '{' && *tail != '}')
                    var = NULL;
                else
                {
                    if (src[1] == '{')
                        ++tail;
                    *var = NUL;
                    var = vim_getenv(dst, &mustfree);
                }
            }
                                                        /* home directory */
            else if (  src[1] == NUL
                    || vim_ispathsep(src[1])
                    || vim_strchr((char_u *)" ,\t\n", src[1]) != NULL)
            {
                var = homedir;
                tail = src + 1;
            }
            else                                        /* user directory */
            {
                /*
                 * Copy ~user to dst[], so we can put a NUL after it.
                 */
                tail = src;
                var = dst;
                c = dstlen - 1;
                while (    c-- > 0
                        && *tail
                        && vim_isfilec(*tail)
                        && !vim_ispathsep(*tail))
                    *var++ = *tail++;
                *var = NUL;
                /*
                 * If the system supports getpwnam(), use it.
                 * Otherwise, or if getpwnam() fails, the shell is used to
                 * expand ~user.  This is slower and may fail if the shell
                 * does not support ~user (old versions of /bin/sh).
                 */
                {
                    struct passwd *pw;

                    /* Note: memory allocated by getpwnam() is never freed.
                     * Calling endpwent() apparently doesn't help. */
                    pw = getpwnam((char *)dst + 1);
                    if (pw != NULL)
                        var = (char_u *)pw->pw_dir;
                    else
                        var = NULL;
                }
                if (var == NULL)
                {
                    expand_T    xpc;

                    ExpandInit(&xpc);
                    xpc.xp_context = EXPAND_FILES;
                    var = ExpandOne(&xpc, dst, NULL,
                                WILD_ADD_SLASH|WILD_SILENT, WILD_EXPAND_FREE);
                    mustfree = TRUE;
                }
            }

            /* If "var" contains white space, escape it with a backslash.
             * Required for ":e ~/tt" when $HOME includes a space. */
            if (esc && var != NULL && vim_strpbrk(var, (char_u *)" \t") != NULL)
            {
                char_u  *p = vim_strsave_escaped(var, (char_u *)" \t");

                if (p != NULL)
                {
                    if (mustfree)
                        vim_free(var);
                    var = p;
                    mustfree = TRUE;
                }
            }

            if (var != NULL && *var != NUL && (STRLEN(var) + STRLEN(tail) + 1 < (unsigned)dstlen))
            {
                STRCPY(dst, var);
                dstlen -= (int)STRLEN(var);
                c = (int)STRLEN(var);
                /* if var[] ends in a path separator and tail[] starts
                 * with it, skip a character */
                if (*var != NUL && after_pathsep(dst, dst + c) && vim_ispathsep(*tail))
                    ++tail;
                dst += c;
                src = tail;
                copy_char = FALSE;
            }
            if (mustfree)
                vim_free(var);
        }

        if (copy_char)      /* copy at least one char */
        {
            /*
             * Recognize the start of a new name, for '~'.
             * Don't do this when "one" is TRUE, to avoid expanding "~" in ":edit foo ~ foo".
             */
            at_start = FALSE;
            if (src[0] == '\\' && src[1] != NUL)
            {
                *dst++ = *src++;
                --dstlen;
            }
            else if ((src[0] == ' ' || src[0] == ',') && !one)
                at_start = TRUE;
            *dst++ = *src++;
            --dstlen;

            if (startstr != NULL && src - startstr_len >= srcp
                    && STRNCMP(src - startstr_len, startstr, startstr_len) == 0)
                at_start = TRUE;
        }
    }
    *dst = NUL;
}

/*
 * Vim's version of getenv().
 * Special handling of $HOME, $VIM and $VIMRUNTIME.
 * "mustfree" is set to TRUE when returned is allocated, it must be
 * initialized to FALSE by the caller.
 */
    char_u *
vim_getenv(name, mustfree)
    char_u      *name;
    int         *mustfree;
{
    char_u      *p;
    int         vimruntime;

    p = mch_getenv(name);
    if (p != NULL && *p == NUL)     /* empty is the same as not set */
        p = NULL;

    if (p != NULL)
        return p;

    vimruntime = (STRCMP(name, "VIMRUNTIME") == 0);
    if (!vimruntime && STRCMP(name, "VIM") != 0)
        return NULL;

    /*
     * When expanding $VIMRUNTIME fails, try using $VIM.
     * Don't do this when default_vimruntime_dir is non-empty.
     */
    if (vimruntime)
    {
        p = mch_getenv((char_u *)"VIM");
        if (p != NULL && *p == NUL)         /* empty is the same as not set */
            p = NULL;
        if (p != NULL)
        {
            p = vim_runtime_dir(p);
            if (p != NULL)
                *mustfree = TRUE;
            else
                p = mch_getenv((char_u *)"VIM");
        }
    }

    /*
     * Set the environment variable, so that the new value can be found fast
     * next time, and others can also use it (e.g. Perl).
     */
    if (p != NULL)
    {
        if (vimruntime)
        {
            vim_setenv((char_u *)"VIMRUNTIME", p);
            didset_vimruntime = TRUE;
        }
        else
        {
            vim_setenv((char_u *)"VIM", p);
            didset_vim = TRUE;
        }
    }
    return p;
}

/*
 * Check if the directory "vimdir/runtime" exists.
 * Return NULL if not, return its name in allocated memory otherwise.
 */
    static char_u *
vim_runtime_dir(vimdir)
    char_u      *vimdir;
{
    char_u      *p;

    if (vimdir == NULL || *vimdir == NUL)
        return NULL;

    p = concat_fnames(vimdir, (char_u *)RUNTIME_DIRNAME, TRUE);
    if (p != NULL && mch_isdir(p))
        return p;
    vim_free(p);

    return NULL;
}

/*
 * Our portable version of setenv.
 */
    void
vim_setenv(name, val)
    char_u      *name;
    char_u      *val;
{
    mch_setenv((char *)name, (char *)val, 1);
}

/*
 * Function given to ExpandGeneric() to obtain an environment variable name.
 */
    char_u *
get_env_name(xp, idx)
    expand_T    *xp UNUSED;
    int         idx;
{
    extern char         **environ;
#define ENVNAMELEN 100
    static char_u       name[ENVNAMELEN];
    char_u              *str;
    int                 n;

    str = (char_u *)environ[idx];
    if (str == NULL)
        return NULL;

    for (n = 0; n < ENVNAMELEN - 1; ++n)
    {
        if (str[n] == '=' || str[n] == NUL)
            break;
        name[n] = str[n];
    }
    name[n] = NUL;
    return name;
}

/*
 * Find all user names for user completion.
 * Done only once and then cached.
 */
    static void
init_users()
{
    static int  lazy_init_done = FALSE;

    if (lazy_init_done)
        return;

    lazy_init_done = TRUE;
    ga_init2(&ga_users, sizeof(char_u *), 20);

    {
        char_u*         user;
        struct passwd*  pw;

        setpwent();
        while ((pw = getpwent()) != NULL)
            /* pw->pw_name shouldn't be NULL but just in case... */
            if (pw->pw_name != NULL)
            {
                if (ga_grow(&ga_users, 1) == FAIL)
                    break;
                user = vim_strsave((char_u*)pw->pw_name);
                if (user == NULL)
                    break;
                ((char_u **)(ga_users.ga_data))[ga_users.ga_len++] = user;
            }
        endpwent();
    }
}

/*
 * Function given to ExpandGeneric() to obtain an user names.
 */
    char_u*
get_users(xp, idx)
    expand_T    *xp UNUSED;
    int         idx;
{
    init_users();
    if (idx < ga_users.ga_len)
        return ((char_u **)ga_users.ga_data)[idx];

    return NULL;
}

/*
 * Check whether name matches a user name. Return:
 * 0 if name does not match any user name.
 * 1 if name partially matches the beginning of a user name.
 * 2 is name fully matches a user name.
 */
int match_user(name)
    char_u* name;
{
    int i;
    int n = (int)STRLEN(name);
    int result = 0;

    init_users();
    for (i = 0; i < ga_users.ga_len; i++)
    {
        if (STRCMP(((char_u **)ga_users.ga_data)[i], name) == 0)
            return 2; /* full match */
        if (STRNCMP(((char_u **)ga_users.ga_data)[i], name, n) == 0)
            result = 1; /* partial match */
    }
    return result;
}

/*
 * Replace home directory by "~" in each space or comma separated file name in 'src'.
 * If anything fails (except when out of space) dst equals src.
 */
    void
home_replace(src, dst, dstlen, one)
    char_u      *src;   /* input file name */
    char_u      *dst;   /* where to put the result */
    int         dstlen; /* maximum length of the result */
    int         one;    /* if TRUE, only replace one file name, include
                           spaces and commas in the file name. */
{
    size_t      dirlen = 0, envlen = 0;
    size_t      len;
    char_u      *homedir_env, *homedir_env_orig;
    char_u      *p;

    if (src == NULL)
    {
        *dst = NUL;
        return;
    }

    /*
     * We check both the value of the $HOME environment variable and the "real" home directory.
     */
    if (homedir != NULL)
        dirlen = STRLEN(homedir);

    homedir_env_orig = homedir_env = mch_getenv((char_u *)"HOME");
    /* Empty is the same as not set. */
    if (homedir_env != NULL && *homedir_env == NUL)
        homedir_env = NULL;

    if (homedir_env != NULL && vim_strchr(homedir_env, '~') != NULL)
    {
        int     usedlen = 0;
        int     flen;
        char_u  *fbuf = NULL;

        flen = (int)STRLEN(homedir_env);
        (void)modify_fname((char_u *)":p", &usedlen, &homedir_env, &fbuf, &flen);
        flen = (int)STRLEN(homedir_env);
        if (flen > 0 && vim_ispathsep(homedir_env[flen - 1]))
            /* Remove the trailing / that is added to a directory. */
            homedir_env[flen - 1] = NUL;
    }

    if (homedir_env != NULL)
        envlen = STRLEN(homedir_env);

    if (!one)
        src = skipwhite(src);
    while (*src && dstlen > 0)
    {
        /*
         * Here we are at the beginning of a file name.
         * First, check to see if the beginning of the file name matches
         * $HOME or the "real" home directory. Check that there is a '/'
         * after the match (so that if e.g. the file is "/home/pieter/bla",
         * and the home directory is "/home/piet", the file does not end up
         * as "~er/bla" (which would seem to indicate the file "bla" in user
         * er's home directory)).
         */
        p = homedir;
        len = dirlen;
        for (;;)
        {
            if (   len
                && fnamencmp(src, p, len) == 0
                && (vim_ispathsep(src[len])
                    || (!one && (src[len] == ',' || src[len] == ' '))
                    || src[len] == NUL))
            {
                src += len;
                if (--dstlen > 0)
                    *dst++ = '~';

                /*
                 * If it's just the home directory, add  "/".
                 */
                if (!vim_ispathsep(src[0]) && --dstlen > 0)
                    *dst++ = '/';
                break;
            }
            if (p == homedir_env)
                break;
            p = homedir_env;
            len = envlen;
        }

        /* if (!one) skip to separator: space or comma */
        while (*src && (one || (*src != ',' && *src != ' ')) && --dstlen > 0)
            *dst++ = *src++;
        /* skip separator */
        while ((*src == ' ' || *src == ',') && --dstlen > 0)
            *dst++ = *src++;
    }
    /* if (dstlen == 0) out of space, what to do??? */

    *dst = NUL;

    if (homedir_env != homedir_env_orig)
        vim_free(homedir_env);
}

/*
 * Like home_replace, store the replaced string in allocated memory.
 * When something fails, NULL is returned.
 */
    char_u  *
home_replace_save(src)
    char_u      *src;   /* input file name */
{
    char_u      *dst;
    unsigned    len;

    len = 3;                    /* space for "~/" and trailing NUL */
    if (src != NULL)            /* just in case */
        len += (unsigned)STRLEN(src);
    dst = alloc(len);
    if (dst != NULL)
        home_replace(src, dst, len, TRUE);
    return dst;
}

/*
 * Compare two file names and return:
 * FPC_SAME   if they both exist and are the same file.
 * FPC_SAMEX  if they both don't exist and have the same file name.
 * FPC_DIFF   if they both exist and are different files.
 * FPC_NOTX   if they both don't exist.
 * FPC_DIFFX  if one of them doesn't exist.
 * For the first name environment variables are expanded
 */
    int
fullpathcmp(s1, s2, checkname)
    char_u *s1, *s2;
    int     checkname;          /* when both don't exist, check file names */
{
    char_u          exp1[MAXPATHL];
    char_u          full1[MAXPATHL];
    char_u          full2[MAXPATHL];
    struct stat     st1, st2;
    int             r1, r2;

    expand_env(s1, exp1, MAXPATHL);
    r1 = mch_stat((char *)exp1, &st1);
    r2 = mch_stat((char *)s2, &st2);
    if (r1 != 0 && r2 != 0)
    {
        /* if mch_stat() doesn't work, may compare the names */
        if (checkname)
        {
            if (fnamecmp(exp1, s2) == 0)
                return FPC_SAMEX;
            r1 = vim_FullName(exp1, full1, MAXPATHL, FALSE);
            r2 = vim_FullName(s2, full2, MAXPATHL, FALSE);
            if (r1 == OK && r2 == OK && fnamecmp(full1, full2) == 0)
                return FPC_SAMEX;
        }
        return FPC_NOTX;
    }
    if (r1 != 0 || r2 != 0)
        return FPC_DIFFX;
    if (st1.st_dev == st2.st_dev && st1.st_ino == st2.st_ino)
        return FPC_SAME;

    return FPC_DIFF;
}

/*
 * Get the tail of a path: the file name.
 * When the path ends in a path separator the tail is the NUL after it.
 * Fail safe: never returns NULL.
 */
    char_u *
gettail(fname)
    char_u *fname;
{
    char_u  *p1, *p2;

    if (fname == NULL)
        return (char_u *)"";
    for (p1 = p2 = get_past_head(fname); *p2; ) /* find last part of path */
    {
        if (vim_ispathsep_nocolon(*p2))
            p1 = p2 + 1;
        p2 += utfc_ptr2len(p2);
    }
    return p1;
}

/*
 * Get pointer to tail of "fname", including path separators.  Putting a NUL
 * here leaves the directory name.  Takes care of "c:/" and "//".
 * Always returns a valid pointer.
 */
    char_u *
gettail_sep(fname)
    char_u      *fname;
{
    char_u      *p;
    char_u      *t;

    p = get_past_head(fname);   /* don't remove the '/' from "c:/file" */
    t = gettail(fname);
    while (t > p && after_pathsep(fname, t))
        --t;
    return t;
}

/*
 * get the next path component (just after the next path separator).
 */
    char_u *
getnextcomp(fname)
    char_u *fname;
{
    while (*fname && !vim_ispathsep(*fname))
        fname += utfc_ptr2len(fname);
    if (*fname)
        ++fname;
    return fname;
}

/*
 * Get a pointer to one character past the head of a path name.
 * Unix: after "/"; DOS: after "c:\"; Amiga: after "disk:/"; Mac: no head.
 * If there is no head, path is returned.
 */
    char_u *
get_past_head(path)
    char_u  *path;
{
    char_u  *retval;

    retval = path;

    while (vim_ispathsep(*retval))
        ++retval;

    return retval;
}

/*
 * Return TRUE if 'c' is a path separator.
 * Note that for MS-Windows this includes the colon.
 */
    int
vim_ispathsep(c)
    int c;
{
    return (c == '/');      /* UNIX has ':' inside file names */
}

/*
 * Like vim_ispathsep(c), but exclude the colon for MS-Windows.
 */
    int
vim_ispathsep_nocolon(c)
    int c;
{
    return vim_ispathsep(c)
        ;
}

/*
 * Shorten the path of a file from "~/foo/../.bar/fname" to "~/f/../.b/fname"
 * It's done in-place.
 */
    void
shorten_dir(str)
    char_u *str;
{
    char_u      *tail, *s, *d;
    int         skip = FALSE;

    tail = gettail(str);
    d = str;
    for (s = str; ; ++s)
    {
        if (s >= tail)              /* copy the whole tail */
        {
            *d++ = *s;
            if (*s == NUL)
                break;
        }
        else if (vim_ispathsep(*s))         /* copy '/' and next char */
        {
            *d++ = *s;
            skip = FALSE;
        }
        else if (!skip)
        {
            *d++ = *s;              /* copy next char */
            if (*s != '~' && *s != '.') /* and leading "~" and "." */
                skip = TRUE;

            {
                int l = utfc_ptr2len(s);

                while (--l > 0)
                    *d++ = *++s;
            }
        }
    }
}

/*
 * Return TRUE if the directory of "fname" exists, FALSE otherwise.
 * Also returns TRUE if there is no directory name.
 * "fname" must be writable!.
 */
    int
dir_of_file_exists(fname)
    char_u      *fname;
{
    char_u      *p;
    int         c;
    int         retval;

    p = gettail_sep(fname);
    if (p == fname)
        return TRUE;
    c = *p;
    *p = NUL;
    retval = mch_isdir(fname);
    *p = c;
    return retval;
}

/*
 * Versions of fnamecmp() and fnamencmp() that handle '/' and '\' equally
 * and deal with 'fileignorecase'.
 */
    int
vim_fnamecmp(x, y)
    char_u      *x, *y;
{
    if (p_fic)
        return MB_STRICMP(x, y);

    return STRCMP(x, y);
}

    int
vim_fnamencmp(x, y, len)
    char_u      *x, *y;
    size_t      len;
{
    if (p_fic)
        return MB_STRNICMP(x, y, len);

    return STRNCMP(x, y, len);
}

/*
 * Concatenate file names fname1 and fname2 into allocated memory.
 * Only add a '/' or '\\' when 'sep' is TRUE and it is necessary.
 */
    char_u  *
concat_fnames(fname1, fname2, sep)
    char_u  *fname1;
    char_u  *fname2;
    int     sep;
{
    char_u  *dest;

    dest = alloc((unsigned)(STRLEN(fname1) + STRLEN(fname2) + 3));
    if (dest != NULL)
    {
        STRCPY(dest, fname1);
        if (sep)
            add_pathsep(dest);
        STRCAT(dest, fname2);
    }
    return dest;
}

/*
 * Concatenate two strings and return the result in allocated memory.
 * Returns NULL when out of memory.
 */
    char_u  *
concat_str(str1, str2)
    char_u  *str1;
    char_u  *str2;
{
    char_u  *dest;
    size_t  l = STRLEN(str1);

    dest = alloc((unsigned)(l + STRLEN(str2) + 1L));
    if (dest != NULL)
    {
        STRCPY(dest, str1);
        STRCPY(dest + l, str2);
    }
    return dest;
}

/*
 * Add a path separator to a file name, unless it already ends in a path separator.
 */
    void
add_pathsep(p)
    char_u      *p;
{
    if (*p != NUL && !after_pathsep(p, p + STRLEN(p)))
        STRCAT(p, PATHSEPSTR);
}

/*
 * FullName_save - Make an allocated copy of a full file name.
 * Returns NULL when out of memory.
 */
    char_u  *
FullName_save(fname, force)
    char_u      *fname;
    int         force;          /* force expansion, even when it already looks
                                 * like a full path name */
{
    char_u      *buf;
    char_u      *new_fname = NULL;

    if (fname == NULL)
        return NULL;

    buf = alloc((unsigned)MAXPATHL);
    if (buf != NULL)
    {
        if (vim_FullName(fname, buf, MAXPATHL, force) != FAIL)
            new_fname = vim_strsave(buf);
        else
            new_fname = vim_strsave(fname);
        vim_free(buf);
    }
    return new_fname;
}

static char_u   *skip_string(char_u *p);
static pos_T *ind_find_start_comment(void);

/*
 * Find the start of a comment, not knowing if we are in a comment right now.
 * Search starts at w_cursor.lnum and goes backwards.
 */
    static pos_T *
ind_find_start_comment()            /* XXX */
{
    return find_start_comment(curbuf->b_ind_maxcomment);
}

    pos_T *
find_start_comment(ind_maxcomment)          /* XXX */
    int         ind_maxcomment;
{
    pos_T       *pos;
    char_u      *line;
    char_u      *p;
    int         cur_maxcomment = ind_maxcomment;

    for (;;)
    {
        pos = findmatchlimit(NULL, '*', FM_BACKWARD, cur_maxcomment);
        if (pos == NULL)
            break;

        /*
         * Check if the comment start we found is inside a string.
         * If it is then restrict the search to below this line and try again.
         */
        line = ml_get(pos->lnum);
        for (p = line; *p && (colnr_T)(p - line) < pos->col; ++p)
            p = skip_string(p);
        if ((colnr_T)(p - line) <= pos->col)
            break;
        cur_maxcomment = curwin->w_cursor.lnum - pos->lnum - 1;
        if (cur_maxcomment <= 0)
        {
            pos = NULL;
            break;
        }
    }
    return pos;
}

/*
 * Skip to the end of a "string" and a 'c' character.
 * If there is no string or character, return argument unmodified.
 */
    static char_u *
skip_string(p)
    char_u  *p;
{
    int     i;

    /*
     * We loop, because strings may be concatenated: "date""time".
     */
    for ( ; ; ++p)
    {
        if (p[0] == '\'')                   /* 'c' or '\n' or '\000' */
        {
            if (!p[1])                      /* ' at end of line */
                break;
            i = 2;
            if (p[1] == '\\')               /* '\n' or '\000' */
            {
                ++i;
                while (vim_isdigit(p[i - 1]))   /* '\000' */
                    ++i;
            }
            if (p[i] == '\'')               /* check for trailing ' */
            {
                p += i;
                continue;
            }
        }
        else if (p[0] == '"')               /* start of string */
        {
            for (++p; p[0]; ++p)
            {
                if (p[0] == '\\' && p[1] != NUL)
                    ++p;
                else if (p[0] == '"')       /* end of string */
                    break;
            }
            if (p[0] == '"')
                continue;
        }
        break;                              /* no string found */
    }
    if (!*p)
        --p;                                /* backup from NUL */
    return p;
}

/*
 * Do C or expression indenting on the current line.
 */
    void
do_c_expr_indent()
{
    if (*curbuf->b_p_inde != NUL)
        fixthisline(get_expr_indent);
    else
        fixthisline(get_c_indent);
}

/*
 * Functions for C-indenting.
 * Most of this originally comes from Eric Fischer.
 */
/*
 * Below "XXX" means that this function may unlock the current line.
 */

static char_u   *cin_skipcomment(char_u *);
static int      cin_nocode(char_u *);
static pos_T    *find_line_comment(void);
static int      cin_has_js_key(char_u *text);
static int      cin_islabel_skip(char_u **);
static int      cin_isdefault(char_u *);
static char_u   *after_label(char_u *l);
static int      get_indent_nolabel(linenr_T lnum);
static int      skip_label(linenr_T, char_u **pp);
static int      cin_first_id_amount(void);
static int      cin_get_equal_amount(linenr_T lnum);
static int      cin_ispreproc(char_u *);
static int      cin_ispreproc_cont(char_u **pp, linenr_T *lnump);
static int      cin_iscomment(char_u *);
static int      cin_islinecomment(char_u *);
static int      cin_isterminated(char_u *, int, int);
static int      cin_isinit(void);
static int      cin_isfuncdecl(char_u **, linenr_T, linenr_T);
static int      cin_isif(char_u *);
static int      cin_iselse(char_u *);
static int      cin_isdo(char_u *);
static int      cin_iswhileofdo(char_u *, linenr_T);
static int      cin_is_if_for_while_before_offset(char_u *line, int *poffset);
static int      cin_iswhileofdo_end(int terminated);
static int      cin_isbreak(char_u *);
static int      cin_is_cpp_baseclass(colnr_T *col);
static int      get_baseclass_amount(int col);
static int      cin_ends_in(char_u *, char_u *, char_u *);
static int      cin_starts_with(char_u *s, char *word);
static int      cin_skip2pos(pos_T *trypos);
static pos_T    *find_start_brace(void);
static pos_T    *find_match_paren(int);
static pos_T    *find_match_char(int c, int ind_maxparen);
static int      corr_ind_maxparen(pos_T *startpos);
static int      find_last_paren(char_u *l, int start, int end);
static int      find_match(int lookfor, linenr_T ourscope);
static int      cin_is_cpp_namespace(char_u *);

/*
 * Skip over white space and C comments within the line.
 * Also skip over Perl/shell comments if desired.
 */
    static char_u *
cin_skipcomment(s)
    char_u      *s;
{
    while (*s)
    {
        char_u *prev_s = s;

        s = skipwhite(s);

        /* Perl/shell # comment comment continues until eol.  Require a space
         * before # to avoid recognizing $#array. */
        if (curbuf->b_ind_hash_comment != 0 && s != prev_s && *s == '#')
        {
            s += STRLEN(s);
            break;
        }
        if (*s != '/')
            break;
        ++s;
        if (*s == '/')          /* slash-slash comment continues till eol */
        {
            s += STRLEN(s);
            break;
        }
        if (*s != '*')
            break;
        for (++s; *s; ++s)      /* skip slash-star comment */
            if (s[0] == '*' && s[1] == '/')
            {
                s += 2;
                break;
            }
    }
    return s;
}

/*
 * Return TRUE if there is no code at *s.  White space and comments are
 * not considered code.
 */
    static int
cin_nocode(s)
    char_u      *s;
{
    return *cin_skipcomment(s) == NUL;
}

/*
 * Check previous lines for a "//" line comment, skipping over blank lines.
 */
    static pos_T *
find_line_comment() /* XXX */
{
    static pos_T pos;
    char_u       *line;
    char_u       *p;

    pos = curwin->w_cursor;
    while (--pos.lnum > 0)
    {
        line = ml_get(pos.lnum);
        p = skipwhite(line);
        if (cin_islinecomment(p))
        {
            pos.col = (int)(p - line);
            return &pos;
        }
        if (*p != NUL)
            break;
    }
    return NULL;
}

/*
 * Return TRUE if "text" starts with "key:".
 */
    static int
cin_has_js_key(text)
    char_u *text;
{
    char_u *s = skipwhite(text);
    int     quote = -1;

    if (*s == '\'' || *s == '"')
    {
        /* can be 'key': or "key": */
        quote = *s;
        ++s;
    }
    if (!vim_isIDc(*s))     /* need at least one ID character */
        return FALSE;

    while (vim_isIDc(*s))
        ++s;
    if (*s == quote)
        ++s;

    s = cin_skipcomment(s);

    /* "::" is not a label, it's C++ */
    return (*s == ':' && s[1] != ':');
}

/*
 * Check if string matches "label:"; move to character after ':' if true.
 * "*s" must point to the start of the label, if there is one.
 */
    static int
cin_islabel_skip(s)
    char_u      **s;
{
    if (!vim_isIDc(**s))            /* need at least one ID character */
        return FALSE;

    while (vim_isIDc(**s))
        (*s)++;

    *s = cin_skipcomment(*s);

    /* "::" is not a label, it's C++ */
    return (**s == ':' && *++*s != ':');
}

/*
 * Recognize a label: "label:".
 * Note: curwin->w_cursor must be where we are looking for the label.
 */
    int
cin_islabel()           /* XXX */
{
    char_u      *s;

    s = cin_skipcomment(ml_get_curline());

    /*
     * Exclude "default" from labels, since it should be indented
     * like a switch label.  Same for C++ scope declarations.
     */
    if (cin_isdefault(s))
        return FALSE;
    if (cin_isscopedecl(s))
        return FALSE;

    if (cin_islabel_skip(&s))
    {
        /*
         * Only accept a label if the previous line is terminated or is a case label.
         */
        pos_T   cursor_save;
        pos_T   *trypos;
        char_u  *line;

        cursor_save = curwin->w_cursor;
        while (curwin->w_cursor.lnum > 1)
        {
            --curwin->w_cursor.lnum;

            /*
             * If we're in a comment now, skip to the start of the comment.
             */
            curwin->w_cursor.col = 0;
            if ((trypos = ind_find_start_comment()) != NULL) /* XXX */
                curwin->w_cursor = *trypos;

            line = ml_get_curline();
            if (cin_ispreproc(line))    /* ignore #defines, #if, etc. */
                continue;
            if (*(line = cin_skipcomment(line)) == NUL)
                continue;

            curwin->w_cursor = cursor_save;
            if (cin_isterminated(line, TRUE, FALSE)
                    || cin_isscopedecl(line)
                    || cin_iscase(line, TRUE)
                    || (cin_islabel_skip(&line) && cin_nocode(line)))
                return TRUE;

            return FALSE;
        }
        curwin->w_cursor = cursor_save;
        return TRUE;            /* label at start of file??? */
    }
    return FALSE;
}

/*
 * Recognize structure initialization and enumerations:
 * "[typedef] [static|public|protected|private] enum"
 * "[typedef] [static|public|protected|private] = {"
 */
    static int
cin_isinit(void)
{
    char_u      *s;
    static char *skip[] = {"static", "public", "protected", "private"};

    s = cin_skipcomment(ml_get_curline());

    if (cin_starts_with(s, "typedef"))
        s = cin_skipcomment(s + 7);

    for (;;)
    {
        int i, l;

        for (i = 0; i < (int)(sizeof(skip) / sizeof(char *)); ++i)
        {
            l = (int)strlen(skip[i]);
            if (cin_starts_with(s, skip[i]))
            {
                s = cin_skipcomment(s + l);
                l = 0;
                break;
            }
        }
        if (l != 0)
            break;
    }

    if (cin_starts_with(s, "enum"))
        return TRUE;

    if (cin_ends_in(s, (char_u *)"=", (char_u *)"{"))
        return TRUE;

    return FALSE;
}

/*
 * Recognize a switch label: "case .*:" or "default:".
 */
     int
cin_iscase(s, strict)
    char_u *s;
    int strict; /* Allow relaxed check of case statement for JS */
{
    s = cin_skipcomment(s);
    if (cin_starts_with(s, "case"))
    {
        for (s += 4; *s; ++s)
        {
            s = cin_skipcomment(s);
            if (*s == ':')
            {
                if (s[1] == ':')        /* skip over "::" for C++ */
                    ++s;
                else
                    return TRUE;
            }
            if (*s == '\'' && s[1] && s[2] == '\'')
                s += 2;                 /* skip over ':' */
            else if (*s == '/' && (s[1] == '*' || s[1] == '/'))
                return FALSE;           /* stop at comment */
            else if (*s == '"')
            {
                /* JS etc. */
                if (strict)
                    return FALSE;               /* stop at string */
                else
                    return TRUE;
            }
        }
        return FALSE;
    }

    if (cin_isdefault(s))
        return TRUE;

    return FALSE;
}

/*
 * Recognize a "default" switch label.
 */
    static int
cin_isdefault(s)
    char_u  *s;
{
    return (STRNCMP(s, "default", 7) == 0
            && *(s = cin_skipcomment(s + 7)) == ':'
            && s[1] != ':');
}

/*
 * Recognize a "public/private/protected" scope declaration label.
 */
    int
cin_isscopedecl(s)
    char_u      *s;
{
    int         i;

    s = cin_skipcomment(s);
    if (STRNCMP(s, "public", 6) == 0)
        i = 6;
    else if (STRNCMP(s, "protected", 9) == 0)
        i = 9;
    else if (STRNCMP(s, "private", 7) == 0)
        i = 7;
    else
        return FALSE;

    return (*(s = cin_skipcomment(s + i)) == ':' && s[1] != ':');
}

/* Maximum number of lines to search back for a "namespace" line. */
#define FIND_NAMESPACE_LIM 20

/*
 * Recognize a "namespace" scope declaration.
 */
    static int
cin_is_cpp_namespace(s)
    char_u      *s;
{
    char_u      *p;
    int         has_name = FALSE;

    s = cin_skipcomment(s);
    if (STRNCMP(s, "namespace", 9) == 0 && (s[9] == NUL || !vim_iswordc(s[9])))
    {
        p = cin_skipcomment(skipwhite(s + 9));
        while (*p != NUL)
        {
            if (vim_iswhite(*p))
            {
                has_name = TRUE; /* found end of a name */
                p = cin_skipcomment(skipwhite(p));
            }
            else if (*p == '{')
            {
                break;
            }
            else if (vim_iswordc(*p))
            {
                if (has_name)
                    return FALSE; /* word character after skipping past name */
                ++p;
            }
            else
            {
                return FALSE;
            }
        }
        return TRUE;
    }
    return FALSE;
}

/*
 * Return a pointer to the first non-empty non-comment character after a ':'.
 * Return NULL if not found.
 *        case 234:    a = b;
 *                     ^
 */
    static char_u *
after_label(l)
    char_u  *l;
{
    for ( ; *l; ++l)
    {
        if (*l == ':')
        {
            if (l[1] == ':')        /* skip over "::" for C++ */
                ++l;
            else if (!cin_iscase(l + 1, FALSE))
                break;
        }
        else if (*l == '\'' && l[1] && l[2] == '\'')
            l += 2;                 /* skip over 'x' */
    }
    if (*l == NUL)
        return NULL;
    l = cin_skipcomment(l + 1);
    if (*l == NUL)
        return NULL;

    return l;
}

/*
 * Get indent of line "lnum", skipping a label.
 * Return 0 if there is nothing after the label.
 */
    static int
get_indent_nolabel(lnum)                /* XXX */
    linenr_T    lnum;
{
    char_u      *l;
    pos_T       fp;
    colnr_T     col;
    char_u      *p;

    l = ml_get(lnum);
    p = after_label(l);
    if (p == NULL)
        return 0;

    fp.col = (colnr_T)(p - l);
    fp.lnum = lnum;
    getvcol(curwin, &fp, &col, NULL, NULL);
    return (int)col;
}

/*
 * Find indent for line "lnum", ignoring any case or jump label.
 * Also return a pointer to the text (after the label) in "pp".
 *   label:     if (asdf && asdfasdf)
 *              ^
 */
    static int
skip_label(lnum, pp)
    linenr_T    lnum;
    char_u      **pp;
{
    char_u      *l;
    int         amount;
    pos_T       cursor_save;

    cursor_save = curwin->w_cursor;
    curwin->w_cursor.lnum = lnum;
    l = ml_get_curline();
                                    /* XXX */
    if (cin_iscase(l, FALSE) || cin_isscopedecl(l) || cin_islabel())
    {
        amount = get_indent_nolabel(lnum);
        l = after_label(ml_get_curline());
        if (l == NULL)          /* just in case */
            l = ml_get_curline();
    }
    else
    {
        amount = get_indent();
        l = ml_get_curline();
    }
    *pp = l;

    curwin->w_cursor = cursor_save;
    return amount;
}

/*
 * Return the indent of the first variable name after a type in a declaration.
 *  int     a,                  indent of "a"
 *  static struct foo    b,     indent of "b"
 *  enum bla    c,              indent of "c"
 * Returns zero when it doesn't look like a declaration.
 */
    static int
cin_first_id_amount()
{
    char_u      *line, *p, *s;
    int         len;
    pos_T       fp;
    colnr_T     col;

    line = ml_get_curline();
    p = skipwhite(line);
    len = (int)(skiptowhite(p) - p);
    if (len == 6 && STRNCMP(p, "static", 6) == 0)
    {
        p = skipwhite(p + 6);
        len = (int)(skiptowhite(p) - p);
    }
    if (len == 6 && STRNCMP(p, "struct", 6) == 0)
        p = skipwhite(p + 6);
    else if (len == 4 && STRNCMP(p, "enum", 4) == 0)
        p = skipwhite(p + 4);
    else if ((len == 8 && STRNCMP(p, "unsigned", 8) == 0)
          || (len == 6 && STRNCMP(p, "signed", 6) == 0))
    {
        s = skipwhite(p + len);
        if ((STRNCMP(s, "int", 3) == 0 && vim_iswhite(s[3]))
         || (STRNCMP(s, "long", 4) == 0 && vim_iswhite(s[4]))
         || (STRNCMP(s, "short", 5) == 0 && vim_iswhite(s[5]))
         || (STRNCMP(s, "char", 4) == 0 && vim_iswhite(s[4])))
            p = s;
    }
    for (len = 0; vim_isIDc(p[len]); ++len)
        ;
    if (len == 0 || !vim_iswhite(p[len]) || cin_nocode(p))
        return 0;

    p = skipwhite(p + len);
    fp.lnum = curwin->w_cursor.lnum;
    fp.col = (colnr_T)(p - line);
    getvcol(curwin, &fp, &col, NULL, NULL);
    return (int)col;
}

/*
 * Return the indent of the first non-blank after an equal sign.
 *       char *foo = "here";
 * Return zero if no (useful) equal sign found.
 * Return -1 if the line above "lnum" ends in a backslash.
 *      foo = "asdf\
 *             asdf\
 *             here";
 */
    static int
cin_get_equal_amount(lnum)
    linenr_T    lnum;
{
    char_u      *line;
    char_u      *s;
    colnr_T     col;
    pos_T       fp;

    if (lnum > 1)
    {
        line = ml_get(lnum - 1);
        if (*line != NUL && line[STRLEN(line) - 1] == '\\')
            return -1;
    }

    line = s = ml_get(lnum);
    while (*s != NUL && vim_strchr((char_u *)"=;{}\"'", *s) == NULL)
    {
        if (cin_iscomment(s))   /* ignore comments */
            s = cin_skipcomment(s);
        else
            ++s;
    }
    if (*s != '=')
        return 0;

    s = skipwhite(s + 1);
    if (cin_nocode(s))
        return 0;

    if (*s == '"')      /* nice alignment for continued strings */
        ++s;

    fp.lnum = lnum;
    fp.col = (colnr_T)(s - line);
    getvcol(curwin, &fp, &col, NULL, NULL);
    return (int)col;
}

/*
 * Recognize a preprocessor statement: Any line that starts with '#'.
 */
    static int
cin_ispreproc(s)
    char_u *s;
{
    if (*skipwhite(s) == '#')
        return TRUE;

    return FALSE;
}

/*
 * Return TRUE if line "*pp" at "*lnump" is a preprocessor statement or a
 * continuation line of a preprocessor statement.  Decrease "*lnump" to the
 * start and return the line in "*pp".
 */
    static int
cin_ispreproc_cont(pp, lnump)
    char_u      **pp;
    linenr_T    *lnump;
{
    char_u      *line = *pp;
    linenr_T    lnum = *lnump;
    int         retval = FALSE;

    for (;;)
    {
        if (cin_ispreproc(line))
        {
            retval = TRUE;
            *lnump = lnum;
            break;
        }
        if (lnum == 1)
            break;
        line = ml_get(--lnum);
        if (*line == NUL || line[STRLEN(line) - 1] != '\\')
            break;
    }

    if (lnum != *lnump)
        *pp = ml_get(*lnump);
    return retval;
}

/*
 * Recognize the start of a C or C++ comment.
 */
    static int
cin_iscomment(p)
    char_u  *p;
{
    return (p[0] == '/' && (p[1] == '*' || p[1] == '/'));
}

/*
 * Recognize the start of a "//" comment.
 */
    static int
cin_islinecomment(p)
    char_u *p;
{
    return (p[0] == '/' && p[1] == '/');
}

/*
 * Recognize a line that starts with '{' or '}', or ends with ';', ',', '{' or '}'.
 * Don't consider "} else" a terminated line.
 * If a line begins with an "else", only consider it terminated if no unmatched
 * opening braces follow (handle "else { foo();" correctly).
 * Return the character terminating the line (ending char's have precedence if
 * both apply in order to determine initializations).
 */
    static int
cin_isterminated(s, incl_open, incl_comma)
    char_u      *s;
    int         incl_open;      /* include '{' at the end as terminator */
    int         incl_comma;     /* recognize a trailing comma */
{
    char_u      found_start = 0;
    unsigned    n_open = 0;
    int         is_else = FALSE;

    s = cin_skipcomment(s);

    if (*s == '{' || (*s == '}' && !cin_iselse(s)))
        found_start = *s;

    if (!found_start)
        is_else = cin_iselse(s);

    while (*s)
    {
        /* skip over comments, "" strings and 'c'haracters */
        s = skip_string(cin_skipcomment(s));
        if (*s == '}' && n_open > 0)
            --n_open;
        if ((!is_else || n_open == 0)
                && (*s == ';' || *s == '}' || (incl_comma && *s == ','))
                && cin_nocode(s + 1))
            return *s;
        else if (*s == '{')
        {
            if (incl_open && cin_nocode(s + 1))
                return *s;
            else
                ++n_open;
        }

        if (*s)
            s++;
    }
    return found_start;
}

/*
 * Recognize the basic picture of a function declaration -- it needs to
 * have an open paren somewhere and a close paren at the end of the line and
 * no semicolons anywhere.
 * When a line ends in a comma we continue looking in the next line.
 * "sp" points to a string with the line.  When looking at other lines it must
 * be restored to the line.  When it's NULL fetch lines here.
 * "lnum" is where we start looking.
 * "min_lnum" is the line before which we will not be looking.
 */
    static int
cin_isfuncdecl(sp, first_lnum, min_lnum)
    char_u      **sp;
    linenr_T    first_lnum;
    linenr_T    min_lnum;
{
    char_u      *s;
    linenr_T    lnum = first_lnum;
    int         retval = FALSE;
    pos_T       *trypos;
    int         just_started = TRUE;

    if (sp == NULL)
        s = ml_get(lnum);
    else
        s = *sp;

    if (find_last_paren(s, '(', ')') && (trypos = find_match_paren(curbuf->b_ind_maxparen)) != NULL)
    {
        lnum = trypos->lnum;
        if (lnum < min_lnum)
            return FALSE;

        s = ml_get(lnum);
    }

    /* Ignore line starting with #. */
    if (cin_ispreproc(s))
        return FALSE;

    while (*s && *s != '(' && *s != ';' && *s != '\'' && *s != '"')
    {
        if (cin_iscomment(s))   /* ignore comments */
            s = cin_skipcomment(s);
        else
            ++s;
    }
    if (*s != '(')
        return FALSE;           /* ';', ' or "  before any () or no '(' */

    while (*s && *s != ';' && *s != '\'' && *s != '"')
    {
        if (*s == ')' && cin_nocode(s + 1))
        {
            /* ')' at the end: may have found a match
             * Check for he previous line not to end in a backslash:
             *       #if defined(x) && \
             *           defined(y)
             */
            lnum = first_lnum - 1;
            s = ml_get(lnum);
            if (*s == NUL || s[STRLEN(s) - 1] != '\\')
                retval = TRUE;
            goto done;
        }
        if ((*s == ',' && cin_nocode(s + 1)) || s[1] == NUL || cin_nocode(s))
        {
            int comma = (*s == ',');

            /* ',' at the end: continue looking in the next line.
             * At the end: check for ',' in the next line, for this style:
             * func(arg1
             *       , arg2) */
            for (;;)
            {
                if (lnum >= curbuf->b_ml.ml_line_count)
                    break;
                s = ml_get(++lnum);
                if (!cin_ispreproc(s))
                    break;
            }
            if (lnum >= curbuf->b_ml.ml_line_count)
                break;
            /* Require a comma at end of the line or a comma or ')' at the
             * start of next line. */
            s = skipwhite(s);
            if (!just_started && (!comma && *s != ',' && *s != ')'))
                break;
            just_started = FALSE;
        }
        else if (cin_iscomment(s))      /* ignore comments */
            s = cin_skipcomment(s);
        else
        {
            ++s;
            just_started = FALSE;
        }
    }

done:
    if (lnum != first_lnum && sp != NULL)
        *sp = ml_get(first_lnum);

    return retval;
}

    static int
cin_isif(p)
    char_u  *p;
{
    return (STRNCMP(p, "if", 2) == 0 && !vim_isIDc(p[2]));
}

    static int
cin_iselse(p)
    char_u  *p;
{
    if (*p == '}')          /* accept "} else" */
        p = cin_skipcomment(p + 1);
    return (STRNCMP(p, "else", 4) == 0 && !vim_isIDc(p[4]));
}

    static int
cin_isdo(p)
    char_u  *p;
{
    return (STRNCMP(p, "do", 2) == 0 && !vim_isIDc(p[2]));
}

/*
 * Check if this is a "while" that should have a matching "do".
 * We only accept a "while (condition) ;", with only white space between the
 * ')' and ';'. The condition may be spread over several lines.
 */
    static int
cin_iswhileofdo(p, lnum)            /* XXX */
    char_u      *p;
    linenr_T    lnum;
{
    pos_T       cursor_save;
    pos_T       *trypos;
    int         retval = FALSE;

    p = cin_skipcomment(p);
    if (*p == '}')              /* accept "} while (cond);" */
        p = cin_skipcomment(p + 1);
    if (cin_starts_with(p, "while"))
    {
        cursor_save = curwin->w_cursor;
        curwin->w_cursor.lnum = lnum;
        curwin->w_cursor.col = 0;
        p = ml_get_curline();
        while (*p && *p != 'w') /* skip any '}', until the 'w' of the "while" */
        {
            ++p;
            ++curwin->w_cursor.col;
        }
        if ((trypos = findmatchlimit(NULL, 0, 0, curbuf->b_ind_maxparen)) != NULL
                && *cin_skipcomment(ml_get_pos(trypos) + 1) == ';')
            retval = TRUE;
        curwin->w_cursor = cursor_save;
    }
    return retval;
}

/*
 * Check whether in "p" there is an "if", "for" or "while" before "*poffset".
 * Return 0 if there is none.
 * Otherwise return !0 and update "*poffset" to point to the place where the string was found.
 */
    static int
cin_is_if_for_while_before_offset(line, poffset)
    char_u *line;
    int    *poffset;
{
    int offset = *poffset;

    if (offset-- < 2)
        return 0;
    while (offset > 2 && vim_iswhite(line[offset]))
        --offset;

    offset -= 1;
    if (!STRNCMP(line + offset, "if", 2))
        goto probablyFound;

    if (offset >= 1)
    {
        offset -= 1;
        if (!STRNCMP(line + offset, "for", 3))
            goto probablyFound;

        if (offset >= 2)
        {
            offset -= 2;
            if (!STRNCMP(line + offset, "while", 5))
                goto probablyFound;
        }
    }
    return 0;

probablyFound:
    if (!offset || !vim_isIDc(line[offset - 1]))
    {
        *poffset = offset;
        return 1;
    }
    return 0;
}

/*
 * Return TRUE if we are at the end of a do-while.
 *    do
 *       nothing;
 *    while (foo
 *             && bar);  <-- here
 * Adjust the cursor to the line with "while".
 */
    static int
cin_iswhileofdo_end(terminated)
    int     terminated;
{
    char_u      *line;
    char_u      *p;
    char_u      *s;
    pos_T       *trypos;
    int         i;

    if (terminated != ';')      /* there must be a ';' at the end */
        return FALSE;

    p = line = ml_get_curline();
    while (*p != NUL)
    {
        p = cin_skipcomment(p);
        if (*p == ')')
        {
            s = skipwhite(p + 1);
            if (*s == ';' && cin_nocode(s + 1))
            {
                /* Found ");" at end of the line, now check there is "while"
                 * before the matching '('.  XXX */
                i = (int)(p - line);
                curwin->w_cursor.col = i;
                trypos = find_match_paren(curbuf->b_ind_maxparen);
                if (trypos != NULL)
                {
                    s = cin_skipcomment(ml_get(trypos->lnum));
                    if (*s == '}')              /* accept "} while (cond);" */
                        s = cin_skipcomment(s + 1);
                    if (cin_starts_with(s, "while"))
                    {
                        curwin->w_cursor.lnum = trypos->lnum;
                        return TRUE;
                    }
                }

                /* Searching may have made "line" invalid, get it again. */
                line = ml_get_curline();
                p = line + i;
            }
        }
        if (*p != NUL)
            ++p;
    }
    return FALSE;
}

    static int
cin_isbreak(p)
    char_u  *p;
{
    return (STRNCMP(p, "break", 5) == 0 && !vim_isIDc(p[5]));
}

/*
 * Find the position of a C++ base-class declaration or
 * constructor-initialization. eg:
 *
 * class MyClass :
 *      baseClass               <-- here
 * class MyClass : public baseClass,
 *      anotherBaseClass        <-- here (should probably lineup ??)
 * MyClass::MyClass(...) :
 *      baseClass(...)          <-- here (constructor-initialization)
 *
 * This is a lot of guessing.  Watch out for "cond ? func() : foo".
 */
    static int
cin_is_cpp_baseclass(col)
    colnr_T     *col;       /* return: column to align with */
{
    char_u      *s;
    int         class_or_struct, lookfor_ctor_init, cpp_base_class;
    linenr_T    lnum = curwin->w_cursor.lnum;
    char_u      *line = ml_get_curline();

    *col = 0;

    s = skipwhite(line);
    if (*s == '#')              /* skip #define FOO x ? (x) : x */
        return FALSE;
    s = cin_skipcomment(s);
    if (*s == NUL)
        return FALSE;

    cpp_base_class = lookfor_ctor_init = class_or_struct = FALSE;

    /* Search for a line starting with '#', empty, ending in ';' or containing
     * '{' or '}' and start below it.  This handles the following situations:
     *  a = cond ?
     *        func() :
     *             asdf;
     *  func::foo()
     *        : something
     *  {}
     *  Foo::Foo (int one, int two)
     *          : something(4),
     *          somethingelse(3)
     *  {}
     */
    while (lnum > 1)
    {
        line = ml_get(lnum - 1);
        s = skipwhite(line);
        if (*s == '#' || *s == NUL)
            break;
        while (*s != NUL)
        {
            s = cin_skipcomment(s);
            if (*s == '{' || *s == '}' || (*s == ';' && cin_nocode(s + 1)))
                break;
            if (*s != NUL)
                ++s;
        }
        if (*s != NUL)
            break;
        --lnum;
    }

    line = ml_get(lnum);
    s = cin_skipcomment(line);
    for (;;)
    {
        if (*s == NUL)
        {
            if (lnum == curwin->w_cursor.lnum)
                break;
            /* Continue in the cursor line. */
            line = ml_get(++lnum);
            s = cin_skipcomment(line);
            if (*s == NUL)
                continue;
        }

        if (s[0] == '"')
            s = skip_string(s) + 1;
        else if (s[0] == ':')
        {
            if (s[1] == ':')
            {
                /* skip double colon. It can't be a constructor
                 * initialization any more */
                lookfor_ctor_init = FALSE;
                s = cin_skipcomment(s + 2);
            }
            else if (lookfor_ctor_init || class_or_struct)
            {
                /* we have something found, that looks like the start of
                 * cpp-base-class-declaration or constructor-initialization */
                cpp_base_class = TRUE;
                lookfor_ctor_init = class_or_struct = FALSE;
                *col = 0;
                s = cin_skipcomment(s + 1);
            }
            else
                s = cin_skipcomment(s + 1);
        }
        else if ((STRNCMP(s, "class", 5) == 0 && !vim_isIDc(s[5]))
                || (STRNCMP(s, "struct", 6) == 0 && !vim_isIDc(s[6])))
        {
            class_or_struct = TRUE;
            lookfor_ctor_init = FALSE;

            if (*s == 'c')
                s = cin_skipcomment(s + 5);
            else
                s = cin_skipcomment(s + 6);
        }
        else
        {
            if (s[0] == '{' || s[0] == '}' || s[0] == ';')
            {
                cpp_base_class = lookfor_ctor_init = class_or_struct = FALSE;
            }
            else if (s[0] == ')')
            {
                /* Constructor-initialization is assumed if we come across
                 * something like "):" */
                class_or_struct = FALSE;
                lookfor_ctor_init = TRUE;
            }
            else if (s[0] == '?')
            {
                /* Avoid seeing '() :' after '?' as constructor init. */
                return FALSE;
            }
            else if (!vim_isIDc(s[0]))
            {
                /* if it is not an identifier, we are wrong */
                class_or_struct = FALSE;
                lookfor_ctor_init = FALSE;
            }
            else if (*col == 0)
            {
                /* it can't be a constructor-initialization any more */
                lookfor_ctor_init = FALSE;

                /* the first statement starts here: lineup with this one... */
                if (cpp_base_class)
                    *col = (colnr_T)(s - line);
            }

            /* When the line ends in a comma don't align with it. */
            if (lnum == curwin->w_cursor.lnum && *s == ',' && cin_nocode(s + 1))
                *col = 0;

            s = cin_skipcomment(s + 1);
        }
    }

    return cpp_base_class;
}

    static int
get_baseclass_amount(col)
    int         col;
{
    int         amount;
    colnr_T     vcol;
    pos_T       *trypos;

    if (col == 0)
    {
        amount = get_indent();
        if (find_last_paren(ml_get_curline(), '(', ')')
                && (trypos = find_match_paren(curbuf->b_ind_maxparen)) != NULL)
            amount = get_indent_lnum(trypos->lnum); /* XXX */
        if (!cin_ends_in(ml_get_curline(), (char_u *)",", NULL))
            amount += curbuf->b_ind_cpp_baseclass;
    }
    else
    {
        curwin->w_cursor.col = col;
        getvcol(curwin, &curwin->w_cursor, &vcol, NULL, NULL);
        amount = (int)vcol;
    }
    if (amount < curbuf->b_ind_cpp_baseclass)
        amount = curbuf->b_ind_cpp_baseclass;
    return amount;
}

/*
 * Return TRUE if string "s" ends with the string "find", possibly followed by
 * white space and comments.  Skip strings and comments.
 * Ignore "ignore" after "find" if it's not NULL.
 */
    static int
cin_ends_in(s, find, ignore)
    char_u      *s;
    char_u      *find;
    char_u      *ignore;
{
    char_u      *p = s;
    char_u      *r;
    int         len = (int)STRLEN(find);

    while (*p != NUL)
    {
        p = cin_skipcomment(p);
        if (STRNCMP(p, find, len) == 0)
        {
            r = skipwhite(p + len);
            if (ignore != NULL && STRNCMP(r, ignore, STRLEN(ignore)) == 0)
                r = skipwhite(r + STRLEN(ignore));
            if (cin_nocode(r))
                return TRUE;
        }
        if (*p != NUL)
            ++p;
    }
    return FALSE;
}

/*
 * Return TRUE when "s" starts with "word" and then a non-ID character.
 */
    static int
cin_starts_with(s, word)
    char_u *s;
    char *word;
{
    int l = (int)STRLEN(word);

    return (STRNCMP(s, word, l) == 0 && !vim_isIDc(s[l]));
}

/*
 * Skip strings, chars and comments until at or past "trypos".
 * Return the column found.
 */
    static int
cin_skip2pos(trypos)
    pos_T       *trypos;
{
    char_u      *line;
    char_u      *p;

    p = line = ml_get(trypos->lnum);
    while (*p && (colnr_T)(p - line) < trypos->col)
    {
        if (cin_iscomment(p))
            p = cin_skipcomment(p);
        else
        {
            p = skip_string(p);
            ++p;
        }
    }
    return (int)(p - line);
}

/*
 * Find the '{' at the start of the block we are in.
 * Return NULL if no match found.
 * Ignore a '{' that is in a comment, makes indenting the next three lines work. */
/* foo()    */
/* {        */
/* }        */

    static pos_T *
find_start_brace()          /* XXX */
{
    pos_T       cursor_save;
    pos_T       *trypos;
    pos_T       *pos;
    static pos_T        pos_copy;

    cursor_save = curwin->w_cursor;
    while ((trypos = findmatchlimit(NULL, '{', FM_BLOCKSTOP, 0)) != NULL)
    {
        pos_copy = *trypos;     /* copy pos_T, next findmatch will change it */
        trypos = &pos_copy;
        curwin->w_cursor = *trypos;
        pos = NULL;
        /* ignore the { if it's in a // or / *  * / comment */
        if ((colnr_T)cin_skip2pos(trypos) == trypos->col
                       && (pos = ind_find_start_comment()) == NULL) /* XXX */
            break;
        if (pos != NULL)
            curwin->w_cursor.lnum = pos->lnum;
    }
    curwin->w_cursor = cursor_save;
    return trypos;
}

/*
 * Find the matching '(', ignoring it if it is in a comment.
 * Return NULL if no match found.
 */
    static pos_T *
find_match_paren(ind_maxparen)      /* XXX */
    int         ind_maxparen;
{
    return find_match_char('(', ind_maxparen);
}

    static pos_T *
find_match_char(c, ind_maxparen)            /* XXX */
    int         c;
    int         ind_maxparen;
{
    pos_T       cursor_save;
    pos_T       *trypos;
    static pos_T pos_copy;
    int         ind_maxp_wk;

    cursor_save = curwin->w_cursor;
    ind_maxp_wk = ind_maxparen;
retry:
    if ((trypos = findmatchlimit(NULL, c, 0, ind_maxp_wk)) != NULL)
    {
        /* check if the ( is in a // comment */
        if ((colnr_T)cin_skip2pos(trypos) > trypos->col)
        {
            ind_maxp_wk = ind_maxparen - (int)(cursor_save.lnum - trypos->lnum);
            if (ind_maxp_wk > 0)
            {
                curwin->w_cursor = *trypos;
                curwin->w_cursor.col = 0;       /* XXX */
                goto retry;
            }
            trypos = NULL;
        }
        else
        {
            pos_T       *trypos_wk;

            pos_copy = *trypos;     /* copy trypos, findmatch will change it */
            trypos = &pos_copy;
            curwin->w_cursor = *trypos;
            if ((trypos_wk = ind_find_start_comment()) != NULL) /* XXX */
            {
                ind_maxp_wk = ind_maxparen - (int)(cursor_save.lnum - trypos_wk->lnum);
                if (ind_maxp_wk > 0)
                {
                    curwin->w_cursor = *trypos_wk;
                    goto retry;
                }
                trypos = NULL;
            }
        }
    }
    curwin->w_cursor = cursor_save;
    return trypos;
}

/*
 * Find the matching '(', ignoring it if it is in a comment or before an unmatched {.
 * Return NULL if no match found.
 */
    static pos_T *
find_match_paren_after_brace(ind_maxparen)          /* XXX */
    int         ind_maxparen;
{
    pos_T       *trypos = find_match_paren(ind_maxparen);

    if (trypos != NULL)
    {
        pos_T   *tryposBrace = find_start_brace();

        /* If both an unmatched '(' and '{' is found.  Ignore the '('
         * position if the '{' is further down. */
        if (tryposBrace != NULL
                && (trypos->lnum != tryposBrace->lnum
                    ? trypos->lnum < tryposBrace->lnum
                    : trypos->col < tryposBrace->col))
            trypos = NULL;
    }
    return trypos;
}

/*
 * Return ind_maxparen corrected for the difference in line number between the
 * cursor position and "startpos".  This makes sure that searching for a
 * matching paren above the cursor line doesn't find a match because of
 * looking a few lines further.
 */
    static int
corr_ind_maxparen(startpos)
    pos_T       *startpos;
{
    long        n = (long)startpos->lnum - (long)curwin->w_cursor.lnum;

    if (n > 0 && n < curbuf->b_ind_maxparen / 2)
        return curbuf->b_ind_maxparen - (int)n;

    return curbuf->b_ind_maxparen;
}

/*
 * Set w_cursor.col to the column number of the last unmatched ')' or '{' in
 * line "l".  "l" must point to the start of the line.
 */
    static int
find_last_paren(l, start, end)
    char_u      *l;
    int         start, end;
{
    int         i;
    int         retval = FALSE;
    int         open_count = 0;

    curwin->w_cursor.col = 0;               /* default is start of line */

    for (i = 0; l[i] != NUL; i++)
    {
        i = (int)(cin_skipcomment(l + i) - l); /* ignore parens in comments */
        i = (int)(skip_string(l + i) - l);    /* ignore parens in quotes */
        if (l[i] == start)
            ++open_count;
        else if (l[i] == end)
        {
            if (open_count > 0)
                --open_count;
            else
            {
                curwin->w_cursor.col = i;
                retval = TRUE;
            }
        }
    }
    return retval;
}

/*
 * Parse 'cinoptions' and set the values in "curbuf".
 * Must be called when 'cinoptions', 'shiftwidth' and/or 'tabstop' changes.
 */
    void
parse_cino(buf)
    buf_T       *buf;
{
    char_u      *p;
    char_u      *l;
    char_u      *digits;
    int         n;
    int         divider;
    int         fraction = 0;
    int         sw = (int)get_sw_value(buf);

    /*
     * Set the default values.
     */
    /* Spaces from a block's opening brace the prevailing indent for that
     * block should be. */
    buf->b_ind_level = sw;

    /* Spaces from the edge of the line an open brace that's at the end of a
     * line is imagined to be. */
    buf->b_ind_open_imag = 0;

    /* Spaces from the prevailing indent for a line that is not preceded by
     * an opening brace. */
    buf->b_ind_no_brace = 0;

    /* Column where the first { of a function should be located }. */
    buf->b_ind_first_open = 0;

    /* Spaces from the prevailing indent a leftmost open brace should be located. */
    buf->b_ind_open_extra = 0;

    /* Spaces from the matching open brace (real location for one at the left
     * edge; imaginary location from one that ends a line) the matching close
     * brace should be located. */
    buf->b_ind_close_extra = 0;

    /* Spaces from the edge of the line an open brace sitting in the leftmost
     * column is imagined to be. */
    buf->b_ind_open_left_imag = 0;

    /* Spaces jump labels should be shifted to the left if N is non-negative,
     * otherwise the jump label will be put to column 1. */
    buf->b_ind_jump_label = -1;

    /* Spaces from the switch() indent a "case xx" label should be located. */
    buf->b_ind_case = sw;

    /* Spaces from the "case xx:" code after a switch() should be located. */
    buf->b_ind_case_code = sw;

    /* Lineup break at end of case in switch() with case label. */
    buf->b_ind_case_break = 0;

    /* Spaces from the class declaration indent a scope declaration label
     * should be located. */
    buf->b_ind_scopedecl = sw;

    /* Spaces from the scope declaration label code should be located. */
    buf->b_ind_scopedecl_code = sw;

    /* Amount K&R-style parameters should be indented. */
    buf->b_ind_param = sw;

    /* Amount a function type spec should be indented. */
    buf->b_ind_func_type = sw;

    /* Amount a cpp base class declaration or constructor initialization
     * should be indented. */
    buf->b_ind_cpp_baseclass = sw;

    /* additional spaces beyond the prevailing indent a continuation line
     * should be located. */
    buf->b_ind_continuation = sw;

    /* Spaces from the indent of the line with an unclosed parentheses. */
    buf->b_ind_unclosed = sw * 2;

    /* Spaces from the indent of the line with an unclosed parentheses, which
     * itself is also unclosed. */
    buf->b_ind_unclosed2 = sw;

    /* Suppress ignoring spaces from the indent of a line starting with an
     * unclosed parentheses. */
    buf->b_ind_unclosed_noignore = 0;

    /* If the opening paren is the last nonwhite character on the line, and
     * b_ind_unclosed_wrapped is nonzero, use this indent relative to the outer
     * context (for very long lines). */
    buf->b_ind_unclosed_wrapped = 0;

    /* Suppress ignoring white space when lining up with the character after
     * an unclosed parentheses. */
    buf->b_ind_unclosed_whiteok = 0;

    /* Indent a closing parentheses under the line start of the matching
     * opening parentheses. */
    buf->b_ind_matching_paren = 0;

    /* Indent a closing parentheses under the previous line. */
    buf->b_ind_paren_prev = 0;

    /* Extra indent for comments. */
    buf->b_ind_comment = 0;

    /* Spaces from the comment opener when there is nothing after it. */
    buf->b_ind_in_comment = 3;

    /* Boolean: if non-zero, use b_ind_in_comment even if there is something
     * after the comment opener. */
    buf->b_ind_in_comment2 = 0;

    /* Max lines to search for an open paren. */
    buf->b_ind_maxparen = 20;

    /* Max lines to search for an open comment. */
    buf->b_ind_maxcomment = 70;

    /* Handle braces for java code. */
    buf->b_ind_java = 0;

    /* Not to confuse JS object properties with labels. */
    buf->b_ind_js = 0;

    /* Handle blocked cases correctly. */
    buf->b_ind_keep_case_label = 0;

    /* Handle C++ namespace. */
    buf->b_ind_cpp_namespace = 0;

    /* Handle continuation lines containing conditions of if(), for() and while(). */
    buf->b_ind_if_for_while = 0;

    for (p = buf->b_p_cino; *p; )
    {
        l = p++;
        if (*p == '-')
            ++p;
        digits = p;         /* remember where the digits start */
        n = getdigits(&p);
        divider = 0;
        if (*p == '.')      /* ".5s" means a fraction */
        {
            fraction = atol((char *)++p);
            while (VIM_ISDIGIT(*p))
            {
                ++p;
                if (divider)
                    divider *= 10;
                else
                    divider = 10;
            }
        }
        if (*p == 's')      /* "2s" means two times 'shiftwidth' */
        {
            if (p == digits)
                n = sw; /* just "s" is one 'shiftwidth' */
            else
            {
                n *= sw;
                if (divider)
                    n += (sw * fraction + divider / 2) / divider;
            }
            ++p;
        }
        if (l[1] == '-')
            n = -n;

        /* When adding an entry here, also update the default 'cinoptions' in
         * doc/indent.txt, and add explanation for it! */
        switch (*l)
        {
            case '>': buf->b_ind_level = n; break;
            case 'e': buf->b_ind_open_imag = n; break;
            case 'n': buf->b_ind_no_brace = n; break;
            case 'f': buf->b_ind_first_open = n; break;
            case '{': buf->b_ind_open_extra = n; break;
            case '}': buf->b_ind_close_extra = n; break;
            case '^': buf->b_ind_open_left_imag = n; break;
            case 'L': buf->b_ind_jump_label = n; break;
            case ':': buf->b_ind_case = n; break;
            case '=': buf->b_ind_case_code = n; break;
            case 'b': buf->b_ind_case_break = n; break;
            case 'p': buf->b_ind_param = n; break;
            case 't': buf->b_ind_func_type = n; break;
            case '/': buf->b_ind_comment = n; break;
            case 'c': buf->b_ind_in_comment = n; break;
            case 'C': buf->b_ind_in_comment2 = n; break;
            case 'i': buf->b_ind_cpp_baseclass = n; break;
            case '+': buf->b_ind_continuation = n; break;
            case '(': buf->b_ind_unclosed = n; break;
            case 'u': buf->b_ind_unclosed2 = n; break;
            case 'U': buf->b_ind_unclosed_noignore = n; break;
            case 'W': buf->b_ind_unclosed_wrapped = n; break;
            case 'w': buf->b_ind_unclosed_whiteok = n; break;
            case 'm': buf->b_ind_matching_paren = n; break;
            case 'M': buf->b_ind_paren_prev = n; break;
            case ')': buf->b_ind_maxparen = n; break;
            case '*': buf->b_ind_maxcomment = n; break;
            case 'g': buf->b_ind_scopedecl = n; break;
            case 'h': buf->b_ind_scopedecl_code = n; break;
            case 'j': buf->b_ind_java = n; break;
            case 'J': buf->b_ind_js = n; break;
            case 'l': buf->b_ind_keep_case_label = n; break;
            case '#': buf->b_ind_hash_comment = n; break;
            case 'N': buf->b_ind_cpp_namespace = n; break;
            case 'k': buf->b_ind_if_for_while = n; break;
        }
        if (*p == ',')
            ++p;
    }
}

    int
get_c_indent()
{
    pos_T       cur_curpos;
    int         amount;
    int         scope_amount;
    int         cur_amount = MAXCOL;
    colnr_T     col;
    char_u      *theline;
    char_u      *linecopy;
    pos_T       *trypos;
    pos_T       *tryposBrace = NULL;
    pos_T       tryposBraceCopy;
    pos_T       our_paren_pos;
    char_u      *start;
    int         start_brace;
#define BRACE_IN_COL0           1           /* '{' is in column 0 */
#define BRACE_AT_START          2           /* '{' is at start of line */
#define BRACE_AT_END            3           /* '{' is at end of line */
    linenr_T    ourscope;
    char_u      *l;
    char_u      *look;
    char_u      terminated;
    int         lookfor;
#define LOOKFOR_INITIAL         0
#define LOOKFOR_IF              1
#define LOOKFOR_DO              2
#define LOOKFOR_CASE            3
#define LOOKFOR_ANY             4
#define LOOKFOR_TERM            5
#define LOOKFOR_UNTERM          6
#define LOOKFOR_SCOPEDECL       7
#define LOOKFOR_NOBREAK         8
#define LOOKFOR_CPP_BASECLASS   9
#define LOOKFOR_ENUM_OR_INIT    10
#define LOOKFOR_JS_KEY          11
#define LOOKFOR_COMMA   12

    int         whilelevel;
    linenr_T    lnum;
    int         n;
    int         iscase;
    int         lookfor_break;
    int         lookfor_cpp_namespace = FALSE;
    int         cont_amount = 0;    /* amount for continuation line */
    int         original_line_islabel;
    int         added_to_amount = 0;
    int         js_cur_has_key = 0;

    /* make a copy, value is changed below */
    int         ind_continuation = curbuf->b_ind_continuation;

    /* remember where the cursor was when we started */
    cur_curpos = curwin->w_cursor;

    /* if we are at line 1 0 is fine, right? */
    if (cur_curpos.lnum == 1)
        return 0;

    /* Get a copy of the current contents of the line.
     * This is required, because only the most recent line obtained with
     * ml_get is valid! */
    linecopy = vim_strsave(ml_get(cur_curpos.lnum));
    if (linecopy == NULL)
        return 0;

    /*
     * In insert mode and the cursor is on a ')' truncate the line at the
     * cursor position.  We don't want to line up with the matching '(' when
     * inserting new stuff.
     * For unknown reasons the cursor might be past the end of the line, thus
     * check for that.
     */
    if ((State & INSERT)
            && curwin->w_cursor.col < (colnr_T)STRLEN(linecopy)
            && linecopy[curwin->w_cursor.col] == ')')
        linecopy[curwin->w_cursor.col] = NUL;

    theline = skipwhite(linecopy);

    /* move the cursor to the start of the line */

    curwin->w_cursor.col = 0;

    original_line_islabel = cin_islabel();  /* XXX */

    /*
     * #defines and so on always go at the left when included in 'cinkeys'.
     */
    if (*theline == '#' && (*linecopy == '#' || in_cinkeys('#', ' ', TRUE)))
        amount = curbuf->b_ind_hash_comment;

    /*
     * Is it a non-case label?  Then that goes at the left margin too unless:
     *  - JS flag is set.
     *  - 'L' item has a positive value.
     */
    else if (original_line_islabel && !curbuf->b_ind_js && curbuf->b_ind_jump_label < 0)
    {
        amount = 0;
    }

    /*
     * If we're inside a "//" comment and there is a "//" comment in a
     * previous line, lineup with that one.
     */
    else if (cin_islinecomment(theline) && (trypos = find_line_comment()) != NULL) /* XXX */
    {
        /* find how indented the line beginning the comment is */
        getvcol(curwin, trypos, &col, NULL, NULL);
        amount = col;
    }

    /*
     * If we're inside a comment and not looking at the start of the
     * comment, try using the 'comments' option.
     */
    else if (!cin_iscomment(theline) && (trypos = ind_find_start_comment()) != NULL)
        /* XXX */
    {
        int     lead_start_len = 2;
        int     lead_middle_len = 1;
        char_u  lead_start[COM_MAX_LEN];        /* start-comment string */
        char_u  lead_middle[COM_MAX_LEN];       /* middle-comment string */
        char_u  lead_end[COM_MAX_LEN];          /* end-comment string */
        char_u  *p;
        int     start_align = 0;
        int     start_off = 0;
        int     done = FALSE;

        /* find how indented the line beginning the comment is */
        getvcol(curwin, trypos, &col, NULL, NULL);
        amount = col;
        *lead_start = NUL;
        *lead_middle = NUL;

        p = curbuf->b_p_com;
        while (*p != NUL)
        {
            int align = 0;
            int off = 0;
            int what = 0;

            while (*p != NUL && *p != ':')
            {
                if (*p == COM_START || *p == COM_END || *p == COM_MIDDLE)
                    what = *p++;
                else if (*p == COM_LEFT || *p == COM_RIGHT)
                    align = *p++;
                else if (VIM_ISDIGIT(*p) || *p == '-')
                    off = getdigits(&p);
                else
                    ++p;
            }

            if (*p == ':')
                ++p;
            (void)copy_option_part(&p, lead_end, COM_MAX_LEN, ",");
            if (what == COM_START)
            {
                STRCPY(lead_start, lead_end);
                lead_start_len = (int)STRLEN(lead_start);
                start_off = off;
                start_align = align;
            }
            else if (what == COM_MIDDLE)
            {
                STRCPY(lead_middle, lead_end);
                lead_middle_len = (int)STRLEN(lead_middle);
            }
            else if (what == COM_END)
            {
                /* If our line starts with the middle comment string, line it
                 * up with the comment opener per the 'comments' option. */
                if (STRNCMP(theline, lead_middle, lead_middle_len) == 0
                        && STRNCMP(theline, lead_end, STRLEN(lead_end)) != 0)
                {
                    done = TRUE;
                    if (curwin->w_cursor.lnum > 1)
                    {
                        /* If the start comment string matches in the previous
                         * line, use the indent of that line plus offset.  If
                         * the middle comment string matches in the previous
                         * line, use the indent of that line.  XXX */
                        look = skipwhite(ml_get(curwin->w_cursor.lnum - 1));
                        if (STRNCMP(look, lead_start, lead_start_len) == 0)
                            amount = get_indent_lnum(curwin->w_cursor.lnum - 1);
                        else if (STRNCMP(look, lead_middle, lead_middle_len) == 0)
                        {
                            amount = get_indent_lnum(curwin->w_cursor.lnum - 1);
                            break;
                        }
                        /* If the start comment string doesn't match with the
                         * start of the comment, skip this entry. XXX */
                        else if (STRNCMP(ml_get(trypos->lnum) + trypos->col,
                                             lead_start, lead_start_len) != 0)
                            continue;
                    }
                    if (start_off != 0)
                        amount += start_off;
                    else if (start_align == COM_RIGHT)
                        amount += vim_strsize(lead_start) - vim_strsize(lead_middle);
                    break;
                }

                /* If our line starts with the end comment string, line it up
                 * with the middle comment */
                if (STRNCMP(theline, lead_middle, lead_middle_len) != 0
                        && STRNCMP(theline, lead_end, STRLEN(lead_end)) == 0)
                {
                    amount = get_indent_lnum(curwin->w_cursor.lnum - 1);
                                                                     /* XXX */
                    if (off != 0)
                        amount += off;
                    else if (align == COM_RIGHT)
                        amount += vim_strsize(lead_start) - vim_strsize(lead_middle);
                    done = TRUE;
                    break;
                }
            }
        }

        /* If our line starts with an asterisk, line up with the
         * asterisk in the comment opener; otherwise, line up
         * with the first character of the comment text.
         */
        if (done)
            ;
        else if (theline[0] == '*')
            amount += 1;
        else
        {
            /*
             * If we are more than one line away from the comment opener, take
             * the indent of the previous non-empty line.  If 'cino' has "CO"
             * and we are just below the comment opener and there are any
             * white characters after it line up with the text after it;
             * otherwise, add the amount specified by "c" in 'cino'
             */
            amount = -1;
            for (lnum = cur_curpos.lnum - 1; lnum > trypos->lnum; --lnum)
            {
                if (linewhite(lnum))                /* skip blank lines */
                    continue;
                amount = get_indent_lnum(lnum);     /* XXX */
                break;
            }
            if (amount == -1)                       /* use the comment opener */
            {
                if (!curbuf->b_ind_in_comment2)
                {
                    start = ml_get(trypos->lnum);
                    look = start + trypos->col + 2; /* skip / and * */
                    if (*look != NUL)               /* if something after it */
                        trypos->col = (colnr_T)(skipwhite(look) - start);
                }
                getvcol(curwin, trypos, &col, NULL, NULL);
                amount = col;
                if (curbuf->b_ind_in_comment2 || *look == NUL)
                    amount += curbuf->b_ind_in_comment;
            }
        }
    }

    /*
     * Are we looking at a ']' that has a match?
     */
    else if (*skipwhite(theline) == ']'
            && (trypos = find_match_char('[', curbuf->b_ind_maxparen)) != NULL)
    {
        /* align with the line containing the '['. */
        amount = get_indent_lnum(trypos->lnum);
    }

    /*
     * Are we inside parentheses or braces?
     */                                             /* XXX */
    else if (((trypos = find_match_paren(curbuf->b_ind_maxparen)) != NULL
                && curbuf->b_ind_java == 0)
            || (tryposBrace = find_start_brace()) != NULL
            || trypos != NULL)
    {
      if (trypos != NULL && tryposBrace != NULL)
      {
          /* Both an unmatched '(' and '{' is found.  Use the one which is
           * closer to the current cursor position, set the other to NULL. */
          if (trypos->lnum != tryposBrace->lnum
                  ? trypos->lnum < tryposBrace->lnum
                  : trypos->col < tryposBrace->col)
              trypos = NULL;
          else
              tryposBrace = NULL;
      }

      if (trypos != NULL)
      {
        /*
         * If the matching paren is more than one line away, use the indent of
         * a previous non-empty line that matches the same paren.
         */
        if (theline[0] == ')' && curbuf->b_ind_paren_prev)
        {
            /* Line up with the start of the matching paren line. */
            amount = get_indent_lnum(curwin->w_cursor.lnum - 1);  /* XXX */
        }
        else
        {
            amount = -1;
            our_paren_pos = *trypos;
            for (lnum = cur_curpos.lnum - 1; lnum > our_paren_pos.lnum; --lnum)
            {
                l = skipwhite(ml_get(lnum));
                if (cin_nocode(l))              /* skip comment lines */
                    continue;
                if (cin_ispreproc_cont(&l, &lnum))
                    continue;                   /* ignore #define, #if, etc. */
                curwin->w_cursor.lnum = lnum;

                /* Skip a comment. XXX */
                if ((trypos = ind_find_start_comment()) != NULL)
                {
                    lnum = trypos->lnum + 1;
                    continue;
                }

                /* XXX */
                if ((trypos = find_match_paren(corr_ind_maxparen(&cur_curpos))) != NULL
                        && trypos->lnum == our_paren_pos.lnum
                        && trypos->col == our_paren_pos.col)
                {
                    amount = get_indent_lnum(lnum); /* XXX */

                    if (theline[0] == ')')
                    {
                        if (our_paren_pos.lnum != lnum && cur_amount > amount)
                            cur_amount = amount;
                        amount = -1;
                    }
                    break;
                }
            }
        }

        /*
         * Line up with line where the matching paren is. XXX
         * If the line starts with a '(' or the indent for unclosed
         * parentheses is zero, line up with the unclosed parentheses.
         */
        if (amount == -1)
        {
            int     ignore_paren_col = 0;
            int     is_if_for_while = 0;

            if (curbuf->b_ind_if_for_while)
            {
                /* Look for the outermost opening parenthesis on this line
                 * and check whether it belongs to an "if", "for" or "while". */

                pos_T       cursor_save = curwin->w_cursor;
                pos_T       outermost;
                char_u      *line;

                trypos = &our_paren_pos;
                do
                {
                    outermost = *trypos;
                    curwin->w_cursor.lnum = outermost.lnum;
                    curwin->w_cursor.col = outermost.col;

                    trypos = find_match_paren(curbuf->b_ind_maxparen);
                } while (trypos && trypos->lnum == outermost.lnum);

                curwin->w_cursor = cursor_save;

                line = ml_get(outermost.lnum);

                is_if_for_while = cin_is_if_for_while_before_offset(line, &outermost.col);
            }

            amount = skip_label(our_paren_pos.lnum, &look);
            look = skipwhite(look);
            if (*look == '(')
            {
                linenr_T    save_lnum = curwin->w_cursor.lnum;
                char_u      *line;
                int         look_col;

                /* Ignore a '(' in front of the line that has a match before
                 * our matching '('. */
                curwin->w_cursor.lnum = our_paren_pos.lnum;
                line = ml_get_curline();
                look_col = (int)(look - line);
                curwin->w_cursor.col = look_col + 1;
                if ((trypos = findmatchlimit(NULL, ')', 0, curbuf->b_ind_maxparen)) != NULL
                          && trypos->lnum == our_paren_pos.lnum
                          && trypos->col < our_paren_pos.col)
                    ignore_paren_col = trypos->col + 1;

                curwin->w_cursor.lnum = save_lnum;
                look = ml_get(our_paren_pos.lnum) + look_col;
            }
            if (theline[0] == ')' || (curbuf->b_ind_unclosed == 0 && is_if_for_while == 0)
                    || (!curbuf->b_ind_unclosed_noignore && *look == '(' && ignore_paren_col == 0))
            {
                /*
                 * If we're looking at a close paren, line up right there;
                 * otherwise, line up with the next (non-white) character.
                 * When b_ind_unclosed_wrapped is set and the matching paren is
                 * the last nonwhite character of the line, use either the
                 * indent of the current line or the indentation of the next
                 * outer paren and add b_ind_unclosed_wrapped (for very long lines).
                 */
                if (theline[0] != ')')
                {
                    cur_amount = MAXCOL;
                    l = ml_get(our_paren_pos.lnum);
                    if (curbuf->b_ind_unclosed_wrapped && cin_ends_in(l, (char_u *)"(", NULL))
                    {
                        /* look for opening unmatched paren, indent one level
                         * for each additional level */
                        n = 1;
                        for (col = 0; col < our_paren_pos.col; ++col)
                        {
                            switch (l[col])
                            {
                                case '(':
                                case '{': ++n;
                                          break;

                                case ')':
                                case '}': if (n > 1)
                                              --n;
                                          break;
                            }
                        }

                        our_paren_pos.col = 0;
                        amount += n * curbuf->b_ind_unclosed_wrapped;
                    }
                    else if (curbuf->b_ind_unclosed_whiteok)
                        our_paren_pos.col++;
                    else
                    {
                        col = our_paren_pos.col + 1;
                        while (vim_iswhite(l[col]))
                            col++;
                        if (l[col] != NUL)      /* In case of trailing space */
                            our_paren_pos.col = col;
                        else
                            our_paren_pos.col++;
                    }
                }

                /*
                 * Find how indented the paren is, or the character after it
                 * if we did the above "if".
                 */
                if (our_paren_pos.col > 0)
                {
                    getvcol(curwin, &our_paren_pos, &col, NULL, NULL);
                    if (cur_amount > (int)col)
                        cur_amount = col;
                }
            }

            if (theline[0] == ')' && curbuf->b_ind_matching_paren)
            {
                /* Line up with the start of the matching paren line. */
            }
            else if ((curbuf->b_ind_unclosed == 0 && is_if_for_while == 0)
                     || (!curbuf->b_ind_unclosed_noignore && *look == '(' && ignore_paren_col == 0))
            {
                if (cur_amount != MAXCOL)
                    amount = cur_amount;
            }
            else
            {
                /* Add b_ind_unclosed2 for each '(' before our matching one,
                 * but ignore (void) before the line (ignore_paren_col). */
                col = our_paren_pos.col;
                while ((int)our_paren_pos.col > ignore_paren_col)
                {
                    --our_paren_pos.col;
                    switch (*ml_get_pos(&our_paren_pos))
                    {
                        case '(': amount += curbuf->b_ind_unclosed2;
                                  col = our_paren_pos.col;
                                  break;
                        case ')': amount -= curbuf->b_ind_unclosed2;
                                  col = MAXCOL;
                                  break;
                    }
                }

                /* Use b_ind_unclosed once, when the first '(' is not inside braces */
                if (col == MAXCOL)
                    amount += curbuf->b_ind_unclosed;
                else
                {
                    curwin->w_cursor.lnum = our_paren_pos.lnum;
                    curwin->w_cursor.col = col;
                    if (find_match_paren_after_brace(curbuf->b_ind_maxparen) != NULL)
                        amount += curbuf->b_ind_unclosed2;
                    else
                    {
                        if (is_if_for_while)
                            amount += curbuf->b_ind_if_for_while;
                        else
                            amount += curbuf->b_ind_unclosed;
                    }
                }
                /*
                 * For a line starting with ')' use the minimum of the two
                 * positions, to avoid giving it more indent than the previous
                 * lines:
                 *  func_long_name(                 if (x
                 *      arg                                 && yy
                 *      )         ^ not here           )    ^ not here
                 */
                if (cur_amount < amount)
                    amount = cur_amount;
            }
        }

        /* add extra indent for a comment */
        if (cin_iscomment(theline))
            amount += curbuf->b_ind_comment;
      }
      else
      {
        /*
         * We are inside braces, there is a { before this line at the position
         * stored in tryposBrace.
         * Make a copy of tryposBrace, it may point to pos_copy inside
         * find_start_brace(), which may be changed somewhere.
         */
        tryposBraceCopy = *tryposBrace;
        tryposBrace = &tryposBraceCopy;
        trypos = tryposBrace;
        ourscope = trypos->lnum;
        start = ml_get(ourscope);

        /*
         * Now figure out how indented the line is in general.
         * If the brace was at the start of the line, we use that;
         * otherwise, check out the indentation of the line as
         * a whole and then add the "imaginary indent" to that.
         */
        look = skipwhite(start);
        if (*look == '{')
        {
            getvcol(curwin, trypos, &col, NULL, NULL);
            amount = col;
            if (*start == '{')
                start_brace = BRACE_IN_COL0;
            else
                start_brace = BRACE_AT_START;
        }
        else
        {
            /* That opening brace might have been on a continuation
             * line.  if so, find the start of the line. */
            curwin->w_cursor.lnum = ourscope;

            /* Position the cursor over the rightmost paren, so that
             * matching it will take us back to the start of the line. */
            lnum = ourscope;
            if (find_last_paren(start, '(', ')')
                        && (trypos = find_match_paren(curbuf->b_ind_maxparen)) != NULL)
                lnum = trypos->lnum;

            /* It could have been something like
             *     case 1: if (asdf &&
             *                  ldfd) {
             *              }
             */
            if ((curbuf->b_ind_js || curbuf->b_ind_keep_case_label)
                           && cin_iscase(skipwhite(ml_get_curline()), FALSE))
                amount = get_indent();
            else if (curbuf->b_ind_js)
                amount = get_indent_lnum(lnum);
            else
                amount = skip_label(lnum, &l);

            start_brace = BRACE_AT_END;
        }

        /* For Javascript check if the line starts with "key:". */
        if (curbuf->b_ind_js)
            js_cur_has_key = cin_has_js_key(theline);

        /*
         * If we're looking at a closing brace, that's where
         * we want to be.  otherwise, add the amount of room
         * that an indent is supposed to be.
         */
        if (theline[0] == '}')
        {
            /*
             * they may want closing braces to line up with something
             * other than the open brace.  indulge them, if so.
             */
            amount += curbuf->b_ind_close_extra;
        }
        else
        {
            /*
             * If we're looking at an "else", try to find an "if" to match it with.
             * If we're looking at a "while", try to find a "do" to match it with.
             */
            lookfor = LOOKFOR_INITIAL;
            if (cin_iselse(theline))
                lookfor = LOOKFOR_IF;
            else if (cin_iswhileofdo(theline, cur_curpos.lnum)) /* XXX */
                lookfor = LOOKFOR_DO;
            if (lookfor != LOOKFOR_INITIAL)
            {
                curwin->w_cursor.lnum = cur_curpos.lnum;
                if (find_match(lookfor, ourscope) == OK)
                {
                    amount = get_indent();      /* XXX */
                    goto theend;
                }
            }

            /*
             * We get here if we are not on an "while-of-do" or "else" (or
             * failed to find a matching "if").
             * Search backwards for something to line up with.
             * First set amount for when we don't find anything.
             */

            /*
             * if the '{' is  _really_ at the left margin, use the imaginary
             * location of a left-margin brace.  Otherwise, correct the
             * location for b_ind_open_extra.
             */

            if (start_brace == BRACE_IN_COL0)       /* '{' is in column 0 */
            {
                amount = curbuf->b_ind_open_left_imag;
                lookfor_cpp_namespace = TRUE;
            }
            else if (start_brace == BRACE_AT_START && lookfor_cpp_namespace)  /* '{' is at start */
            {
                lookfor_cpp_namespace = TRUE;
            }
            else
            {
                if (start_brace == BRACE_AT_END)    /* '{' is at end of line */
                {
                    amount += curbuf->b_ind_open_imag;

                    l = skipwhite(ml_get_curline());
                    if (cin_is_cpp_namespace(l))
                        amount += curbuf->b_ind_cpp_namespace;
                }
                else
                {
                    /* Compensate for adding b_ind_open_extra later. */
                    amount -= curbuf->b_ind_open_extra;
                    if (amount < 0)
                        amount = 0;
                }
            }

            lookfor_break = FALSE;

            if (cin_iscase(theline, FALSE))     /* it's a switch() label */
            {
                lookfor = LOOKFOR_CASE; /* find a previous switch() label */
                amount += curbuf->b_ind_case;
            }
            else if (cin_isscopedecl(theline))  /* private:, ... */
            {
                lookfor = LOOKFOR_SCOPEDECL;    /* class decl is this block */
                amount += curbuf->b_ind_scopedecl;
            }
            else
            {
                if (curbuf->b_ind_case_break && cin_isbreak(theline))
                    /* break; ... */
                    lookfor_break = TRUE;

                lookfor = LOOKFOR_INITIAL;
                /* b_ind_level from start of block */
                amount += curbuf->b_ind_level;
            }
            scope_amount = amount;
            whilelevel = 0;

            /*
             * Search backwards.  If we find something we recognize, line up with that.
             *
             * If we're looking at an open brace, indent
             * the usual amount relative to the conditional
             * that opens the block.
             */
            curwin->w_cursor = cur_curpos;
            for (;;)
            {
                curwin->w_cursor.lnum--;
                curwin->w_cursor.col = 0;

                /*
                 * If we went all the way back to the start of our scope, line up with it.
                 */
                if (curwin->w_cursor.lnum <= ourscope)
                {
                    /* we reached end of scope:
                     * if looking for a enum or structure initialization
                     * go further back:
                     * if it is an initializer (enum xxx or xxx =), then
                     * don't add ind_continuation, otherwise it is a variable
                     * declaration:
                     * int x,
                     *     here; <-- add ind_continuation
                     */
                    if (lookfor == LOOKFOR_ENUM_OR_INIT)
                    {
                        if (curwin->w_cursor.lnum == 0
                                || curwin->w_cursor.lnum < ourscope - curbuf->b_ind_maxparen)
                        {
                            /* nothing found (abuse curbuf->b_ind_maxparen as
                             * limit) assume terminated line (i.e. a variable initialization) */
                            if (cont_amount > 0)
                                amount = cont_amount;
                            else if (!curbuf->b_ind_js)
                                amount += ind_continuation;
                            break;
                        }

                        l = ml_get_curline();

                        /*
                         * If we're in a comment now, skip to the start of the comment.
                         */
                        trypos = ind_find_start_comment();
                        if (trypos != NULL)
                        {
                            curwin->w_cursor.lnum = trypos->lnum + 1;
                            curwin->w_cursor.col = 0;
                            continue;
                        }

                        /*
                         * Skip preprocessor directives and blank lines.
                         */
                        if (cin_ispreproc_cont(&l, &curwin->w_cursor.lnum))
                            continue;

                        if (cin_nocode(l))
                            continue;

                        terminated = cin_isterminated(l, FALSE, TRUE);

                        /*
                         * If we are at top level and the line looks like a
                         * function declaration, we are done
                         * (it's a variable declaration).
                         */
                        if (start_brace != BRACE_IN_COL0
                             || !cin_isfuncdecl(&l, curwin->w_cursor.lnum, 0))
                        {
                            /* if the line is terminated with another ','
                             * it is a continued variable initialization.
                             * don't add extra indent.
                             * TODO: does not work, if  a function
                             * declaration is split over multiple lines:
                             * cin_isfuncdecl returns FALSE then.
                             */
                            if (terminated == ',')
                                break;

                            /* if it es a enum declaration or an assignment, we are done.
                             */
                            if (terminated != ';' && cin_isinit())
                                break;

                            /* nothing useful found */
                            if (terminated == 0 || terminated == '{')
                                continue;
                        }

                        if (terminated != ';')
                        {
                            /* Skip parens and braces. Position the cursor
                             * over the rightmost paren, so that matching it
                             * will take us back to the start of the line.
                             */                                 /* XXX */
                            trypos = NULL;
                            if (find_last_paren(l, '(', ')'))
                                trypos = find_match_paren(curbuf->b_ind_maxparen);

                            if (trypos == NULL && find_last_paren(l, '{', '}'))
                                trypos = find_start_brace();

                            if (trypos != NULL)
                            {
                                curwin->w_cursor.lnum = trypos->lnum + 1;
                                curwin->w_cursor.col = 0;
                                continue;
                            }
                        }

                        /* it's a variable declaration, add indentation
                         * like in
                         * int a,
                         *    b;
                         */
                        if (cont_amount > 0)
                            amount = cont_amount;
                        else
                            amount += ind_continuation;
                    }
                    else if (lookfor == LOOKFOR_UNTERM)
                    {
                        if (cont_amount > 0)
                            amount = cont_amount;
                        else
                            amount += ind_continuation;
                    }
                    else
                    {
                        if (lookfor != LOOKFOR_TERM
                                        && lookfor != LOOKFOR_CPP_BASECLASS
                                        && lookfor != LOOKFOR_COMMA)
                        {
                            amount = scope_amount;
                            if (theline[0] == '{')
                            {
                                amount += curbuf->b_ind_open_extra;
                                added_to_amount = curbuf->b_ind_open_extra;
                            }
                        }

                        if (lookfor_cpp_namespace)
                        {
                            /*
                             * Looking for C++ namespace, need to look further back.
                             */
                            if (curwin->w_cursor.lnum == ourscope)
                                continue;

                            if (curwin->w_cursor.lnum == 0
                                    || curwin->w_cursor.lnum < ourscope - FIND_NAMESPACE_LIM)
                                break;

                            l = ml_get_curline();

                            /* If we're in a comment now, skip to the start of the comment. */
                            trypos = ind_find_start_comment();
                            if (trypos != NULL)
                            {
                                curwin->w_cursor.lnum = trypos->lnum + 1;
                                curwin->w_cursor.col = 0;
                                continue;
                            }

                            /* Skip preprocessor directives and blank lines. */
                            if (cin_ispreproc_cont(&l, &curwin->w_cursor.lnum))
                                continue;

                            /* Finally the actual check for "namespace". */
                            if (cin_is_cpp_namespace(l))
                            {
                                amount += curbuf->b_ind_cpp_namespace - added_to_amount;
                                break;
                            }

                            if (cin_nocode(l))
                                continue;
                        }
                    }
                    break;
                }

                /*
                 * If we're in a comment now, skip to the start of the comment.
                 */                                         /* XXX */
                if ((trypos = ind_find_start_comment()) != NULL)
                {
                    curwin->w_cursor.lnum = trypos->lnum + 1;
                    curwin->w_cursor.col = 0;
                    continue;
                }

                l = ml_get_curline();

                /*
                 * If this is a switch() label, may line up relative to that.
                 * If this is a C++ scope declaration, do the same.
                 */
                iscase = cin_iscase(l, FALSE);
                if (iscase || cin_isscopedecl(l))
                {
                    /* we are only looking for cpp base class
                     * declaration/initialization any longer */
                    if (lookfor == LOOKFOR_CPP_BASECLASS)
                        break;

                    /* When looking for a "do" we are not interested in labels. */
                    if (whilelevel > 0)
                        continue;

                    /*
                     *  case xx:
                     *      c = 99 +        <- this indent plus continuation
                     *->           here;
                     */
                    if (lookfor == LOOKFOR_UNTERM || lookfor == LOOKFOR_ENUM_OR_INIT)
                    {
                        if (cont_amount > 0)
                            amount = cont_amount;
                        else
                            amount += ind_continuation;
                        break;
                    }

                    /*
                     *  case xx:        <- line up with this case
                     *      x = 333;
                     *  case yy:
                     */
                    if (       (iscase && lookfor == LOOKFOR_CASE)
                            || (iscase && lookfor_break)
                            || (!iscase && lookfor == LOOKFOR_SCOPEDECL))
                    {
                        /*
                         * Check that this case label is not for another switch()
                         */                                 /* XXX */
                        if ((trypos = find_start_brace()) == NULL || trypos->lnum == ourscope)
                        {
                            amount = get_indent();      /* XXX */
                            break;
                        }
                        continue;
                    }

                    n = get_indent_nolabel(curwin->w_cursor.lnum);  /* XXX */

                    /*
                     *   case xx: if (cond)         <- line up with this if
                     *                y = y + 1;
                     * ->         s = 99;
                     *
                     *   case xx:
                     *       if (cond)          <- line up with this line
                     *           y = y + 1;
                     * ->    s = 99;
                     */
                    if (lookfor == LOOKFOR_TERM)
                    {
                        if (n)
                            amount = n;

                        if (!lookfor_break)
                            break;
                    }

                    /*
                     *   case xx: x = x + 1;        <- line up with this x
                     * ->         y = y + 1;
                     *
                     *   case xx: if (cond)         <- line up with this if
                     * ->              y = y + 1;
                     */
                    if (n)
                    {
                        amount = n;
                        l = after_label(ml_get_curline());
                        if (l != NULL && cin_is_cinword(l))
                        {
                            if (theline[0] == '{')
                                amount += curbuf->b_ind_open_extra;
                            else
                                amount += curbuf->b_ind_level + curbuf->b_ind_no_brace;
                        }
                        break;
                    }

                    /*
                     * Try to get the indent of a statement before the switch
                     * label.  If nothing is found, line up relative to the
                     * switch label.
                     *      break;              <- may line up with this line
                     *   case xx:
                     * ->   y = 1;
                     */
                    scope_amount = get_indent() + (iscase    /* XXX */
                                        ? curbuf->b_ind_case_code
                                        : curbuf->b_ind_scopedecl_code);
                    lookfor = curbuf->b_ind_case_break
                                              ? LOOKFOR_NOBREAK : LOOKFOR_ANY;
                    continue;
                }

                /*
                 * Looking for a switch() label or C++ scope declaration,
                 * ignore other lines, skip {}-blocks.
                 */
                if (lookfor == LOOKFOR_CASE || lookfor == LOOKFOR_SCOPEDECL)
                {
                    if (find_last_paren(l, '{', '}') && (trypos = find_start_brace()) != NULL)
                    {
                        curwin->w_cursor.lnum = trypos->lnum + 1;
                        curwin->w_cursor.col = 0;
                    }
                    continue;
                }

                /*
                 * Ignore jump labels with nothing after them.
                 */
                if (!curbuf->b_ind_js && cin_islabel())
                {
                    l = after_label(ml_get_curline());
                    if (l == NULL || cin_nocode(l))
                        continue;
                }

                /*
                 * Ignore #defines, #if, etc.
                 * Ignore comment and empty lines.
                 * (need to get the line again, cin_islabel() may have unlocked it)
                 */
                l = ml_get_curline();
                if (cin_ispreproc_cont(&l, &curwin->w_cursor.lnum) || cin_nocode(l))
                    continue;

                /*
                 * Are we at the start of a cpp base class declaration or
                 * constructor initialization?
                 */                                                 /* XXX */
                n = FALSE;
                if (lookfor != LOOKFOR_TERM && curbuf->b_ind_cpp_baseclass > 0)
                {
                    n = cin_is_cpp_baseclass(&col);
                    l = ml_get_curline();
                }
                if (n)
                {
                    if (lookfor == LOOKFOR_UNTERM)
                    {
                        if (cont_amount > 0)
                            amount = cont_amount;
                        else
                            amount += ind_continuation;
                    }
                    else if (theline[0] == '{')
                    {
                        /* Need to find start of the declaration. */
                        lookfor = LOOKFOR_UNTERM;
                        ind_continuation = 0;
                        continue;
                    }
                    else
                                                                     /* XXX */
                        amount = get_baseclass_amount(col);
                    break;
                }
                else if (lookfor == LOOKFOR_CPP_BASECLASS)
                {
                    /* only look, whether there is a cpp base class
                     * declaration or initialization before the opening brace.
                     */
                    if (cin_isterminated(l, TRUE, FALSE))
                        break;
                    else
                        continue;
                }

                /*
                 * What happens next depends on the line being terminated.
                 * If terminated with a ',' only consider it terminating if
                 * there is another unterminated statement behind, eg:
                 *   123,
                 *   sizeof
                 *        here
                 * Otherwise check whether it is a enumeration or structure
                 * initialisation (not indented) or a variable declaration
                 * (indented).
                 */
                terminated = cin_isterminated(l, FALSE, TRUE);

                if (js_cur_has_key)
                {
                    js_cur_has_key = 0; /* only check the first line */
                    if (curbuf->b_ind_js && terminated == ',')
                    {
                        /* For Javascript we might be inside an object:
                         *   key: something,  <- align with this
                         *   key: something
                         * or:
                         *   key: something +  <- align with this
                         *       something,
                         *   key: something
                         */
                        lookfor = LOOKFOR_JS_KEY;
                    }
                }
                if (lookfor == LOOKFOR_JS_KEY && cin_has_js_key(l))
                {
                    amount = get_indent();
                    break;
                }
                if (lookfor == LOOKFOR_COMMA)
                {
                    if (tryposBrace != NULL && tryposBrace->lnum >= curwin->w_cursor.lnum)
                        break;
                    if (terminated == ',')
                        /* line below current line is the one that starts a
                         * (possibly broken) line ending in a comma. */
                        break;
                    else
                    {
                        amount = get_indent();
                        if (curwin->w_cursor.lnum - 1 == ourscope)
                            /* line above is start of the scope, thus current
                             * line is the one that stars a (possibly broken)
                             * line ending in a comma. */
                            break;
                    }
                }

                if (terminated == 0 || (lookfor != LOOKFOR_UNTERM && terminated == ','))
                {
                    if (*skipwhite(l) == '[' || l[STRLEN(l) - 1] == '[')
                        amount += ind_continuation;
                    /*
                     * if we're in the middle of a paren thing,
                     * go back to the line that starts it so
                     * we can get the right prevailing indent
                     *     if ( foo &&
                     *              bar )
                     */
                    /*
                     * Position the cursor over the rightmost paren, so that
                     * matching it will take us back to the start of the line.
                     * Ignore a match before the start of the block.
                     */
                    (void)find_last_paren(l, '(', ')');
                    trypos = find_match_paren(corr_ind_maxparen(&cur_curpos));
                    if (trypos != NULL && (trypos->lnum < tryposBrace->lnum
                                || (trypos->lnum == tryposBrace->lnum
                                    && trypos->col < tryposBrace->col)))
                        trypos = NULL;

                    /*
                     * If we are looking for ',', we also look for matching braces.
                     */
                    if (trypos == NULL && terminated == ',' && find_last_paren(l, '{', '}'))
                        trypos = find_start_brace();

                    if (trypos != NULL)
                    {
                        /*
                         * Check if we are on a case label now.  This is
                         * handled above.
                         *     case xx:  if ( asdf &&
                         *                      asdf)
                         */
                        curwin->w_cursor = *trypos;
                        l = ml_get_curline();
                        if (cin_iscase(l, FALSE) || cin_isscopedecl(l))
                        {
                            ++curwin->w_cursor.lnum;
                            curwin->w_cursor.col = 0;
                            continue;
                        }
                    }

                    /*
                     * Skip over continuation lines to find the one to get the
                     * indent from
                     * char *usethis = "bla\
                     *           bla",
                     *      here;
                     */
                    if (terminated == ',')
                    {
                        while (curwin->w_cursor.lnum > 1)
                        {
                            l = ml_get(curwin->w_cursor.lnum - 1);
                            if (*l == NUL || l[STRLEN(l) - 1] != '\\')
                                break;
                            --curwin->w_cursor.lnum;
                            curwin->w_cursor.col = 0;
                        }
                    }

                    /*
                     * Get indent and pointer to text for current line,
                     * ignoring any jump label.     XXX
                     */
                    if (curbuf->b_ind_js)
                        cur_amount = get_indent();
                    else
                        cur_amount = skip_label(curwin->w_cursor.lnum, &l);
                    /*
                     * If this is just above the line we are indenting, and it
                     * starts with a '{', line it up with this line.
                     *          while (not)
                     * ->       {
                     *          }
                     */
                    if (terminated != ',' && lookfor != LOOKFOR_TERM && theline[0] == '{')
                    {
                        amount = cur_amount;
                        /*
                         * Only add b_ind_open_extra when the current line
                         * doesn't start with a '{', which must have a match
                         * in the same line (scope is the same).  Probably:
                         *      { 1, 2 },
                         * ->   { 3, 4 }
                         */
                        if (*skipwhite(l) != '{')
                            amount += curbuf->b_ind_open_extra;

                        if (curbuf->b_ind_cpp_baseclass && !curbuf->b_ind_js)
                        {
                            /* have to look back, whether it is a cpp base
                             * class declaration or initialization */
                            lookfor = LOOKFOR_CPP_BASECLASS;
                            continue;
                        }
                        break;
                    }

                    /*
                     * Check if we are after an "if", "while", etc.
                     * Also allow "   } else".
                     */
                    if (cin_is_cinword(l) || cin_iselse(skipwhite(l)))
                    {
                        /*
                         * Found an unterminated line after an if (), line up
                         * with the last one.
                         *   if (cond)
                         *          100 +
                         * ->           here;
                         */
                        if (lookfor == LOOKFOR_UNTERM || lookfor == LOOKFOR_ENUM_OR_INIT)
                        {
                            if (cont_amount > 0)
                                amount = cont_amount;
                            else
                                amount += ind_continuation;
                            break;
                        }

                        /*
                         * If this is just above the line we are indenting, we
                         * are finished.
                         *          while (not)
                         * ->           here;
                         * Otherwise this indent can be used when the line
                         * before this is terminated.
                         *      yyy;
                         *      if (stat)
                         *          while (not)
                         *              xxx;
                         * ->   here;
                         */
                        amount = cur_amount;
                        if (theline[0] == '{')
                            amount += curbuf->b_ind_open_extra;
                        if (lookfor != LOOKFOR_TERM)
                        {
                            amount += curbuf->b_ind_level + curbuf->b_ind_no_brace;
                            break;
                        }

                        /*
                         * Special trick: when expecting the while () after a
                         * do, line up with the while()
                         *     do
                         *          x = 1;
                         * ->  here
                         */
                        l = skipwhite(ml_get_curline());
                        if (cin_isdo(l))
                        {
                            if (whilelevel == 0)
                                break;
                            --whilelevel;
                        }

                        /*
                         * When searching for a terminated line, don't use the
                         * one between the "if" and the matching "else".
                         * Need to use the scope of this "else".  XXX
                         * If whilelevel != 0 continue looking for a "do {".
                         */
                        if (cin_iselse(l) && whilelevel == 0)
                        {
                            /* If we're looking at "} else", let's make sure we
                             * find the opening brace of the enclosing scope,
                             * not the one from "if () {". */
                            if (*l == '}')
                                curwin->w_cursor.col = (colnr_T)(l - ml_get_curline()) + 1;

                            if ((trypos = find_start_brace()) == NULL
                                       || find_match(LOOKFOR_IF, trypos->lnum) == FAIL)
                                break;
                        }
                    }

                    /*
                     * If we're below an unterminated line that is not an
                     * "if" or something, we may line up with this line or
                     * add something for a continuation line, depending on
                     * the line before this one.
                     */
                    else
                    {
                        /*
                         * Found two unterminated lines on a row, line up with
                         * the last one.
                         *   c = 99 +
                         *          100 +
                         * ->       here;
                         */
                        if (lookfor == LOOKFOR_UNTERM)
                        {
                            /* When line ends in a comma add extra indent */
                            if (terminated == ',')
                                amount += ind_continuation;
                            break;
                        }

                        if (lookfor == LOOKFOR_ENUM_OR_INIT)
                        {
                            /* Found two lines ending in ',', lineup with the
                             * lowest one, but check for cpp base class
                             * declaration/initialization, if it is an
                             * opening brace or we are looking just for
                             * enumerations/initializations. */
                            if (terminated == ',')
                            {
                                if (curbuf->b_ind_cpp_baseclass == 0)
                                    break;

                                lookfor = LOOKFOR_CPP_BASECLASS;
                                continue;
                            }

                            /* Ignore unterminated lines in between, but reduce indent. */
                            if (amount > cur_amount)
                                amount = cur_amount;
                        }
                        else
                        {
                            /*
                             * Found first unterminated line on a row, may
                             * line up with this line, remember its indent
                             *      100 +
                             * ->           here;
                             */
                            l = ml_get_curline();
                            amount = cur_amount;
                            if (*skipwhite(l) == ']' || l[STRLEN(l) - 1] == ']')
                                break;

                            /*
                             * If previous line ends in ',', check whether we
                             * are in an initialization or enum
                             * struct xxx =
                             * {
                             *      sizeof a,
                             *      124 };
                             * or a normal possible continuation line.
                             * but only, of no other statement has been found yet.
                             */
                            if (lookfor == LOOKFOR_INITIAL && terminated == ',')
                            {
                                if (curbuf->b_ind_js)
                                {
                                    /* Search for a line ending in a comma
                                     * and line up with the line below it
                                     * (could be the current line).
                                     * some = [
                                     *     1,     <- line up here
                                     *     2,
                                     * some = [
                                     *     3 +    <- line up here
                                     *       4 *
                                     *        5,
                                     *     6,
                                     */
                                    if (cin_iscomment(skipwhite(l)))
                                        break;
                                    lookfor = LOOKFOR_COMMA;
                                    trypos = find_match_char('[', curbuf->b_ind_maxparen);
                                    if (trypos != NULL)
                                    {
                                        if (trypos->lnum == curwin->w_cursor.lnum - 1)
                                        {
                                            /* Current line is first inside
                                             * [], line up with it. */
                                            break;
                                        }
                                        ourscope = trypos->lnum;
                                    }
                                }
                                else
                                {
                                    lookfor = LOOKFOR_ENUM_OR_INIT;
                                    cont_amount = cin_first_id_amount();
                                }
                            }
                            else
                            {
                                if (lookfor == LOOKFOR_INITIAL
                                        && *l != NUL
                                        && l[STRLEN(l) - 1] == '\\')
                                                                /* XXX */
                                    cont_amount = cin_get_equal_amount(curwin->w_cursor.lnum);
                                if (lookfor != LOOKFOR_TERM
                                                && lookfor != LOOKFOR_JS_KEY
                                                && lookfor != LOOKFOR_COMMA)
                                    lookfor = LOOKFOR_UNTERM;
                            }
                        }
                    }
                }

                /*
                 * Check if we are after a while (cond);
                 * If so: Ignore until the matching "do".
                 */
                else if (cin_iswhileofdo_end(terminated)) /* XXX */
                {
                    /*
                     * Found an unterminated line after a while ();, line up
                     * with the last one.
                     *      while (cond);
                     *      100 +               <- line up with this one
                     * ->           here;
                     */
                    if (lookfor == LOOKFOR_UNTERM || lookfor == LOOKFOR_ENUM_OR_INIT)
                    {
                        if (cont_amount > 0)
                            amount = cont_amount;
                        else
                            amount += ind_continuation;
                        break;
                    }

                    if (whilelevel == 0)
                    {
                        lookfor = LOOKFOR_TERM;
                        amount = get_indent();      /* XXX */
                        if (theline[0] == '{')
                            amount += curbuf->b_ind_open_extra;
                    }
                    ++whilelevel;
                }

                /*
                 * We are after a "normal" statement.
                 * If we had another statement we can stop now and use the
                 * indent of that other statement.
                 * Otherwise the indent of the current statement may be used,
                 * search backwards for the next "normal" statement.
                 */
                else
                {
                    /*
                     * Skip single break line, if before a switch label. It
                     * may be lined up with the case label.
                     */
                    if (lookfor == LOOKFOR_NOBREAK && cin_isbreak(skipwhite(ml_get_curline())))
                    {
                        lookfor = LOOKFOR_ANY;
                        continue;
                    }

                    /*
                     * Handle "do {" line.
                     */
                    if (whilelevel > 0)
                    {
                        l = cin_skipcomment(ml_get_curline());
                        if (cin_isdo(l))
                        {
                            amount = get_indent();      /* XXX */
                            --whilelevel;
                            continue;
                        }
                    }

                    /*
                     * Found a terminated line above an unterminated line. Add
                     * the amount for a continuation line.
                     *   x = 1;
                     *   y = foo +
                     * ->       here;
                     * or
                     *   int x = 1;
                     *   int foo,
                     * ->       here;
                     */
                    if (lookfor == LOOKFOR_UNTERM || lookfor == LOOKFOR_ENUM_OR_INIT)
                    {
                        if (cont_amount > 0)
                            amount = cont_amount;
                        else
                            amount += ind_continuation;
                        break;
                    }

                    /*
                     * Found a terminated line above a terminated line or "if"
                     * etc. line. Use the amount of the line below us.
                     *   x = 1;                         x = 1;
                     *   if (asdf)                  y = 2;
                     *       while (asdf)         ->here;
                     *          here;
                     * ->foo;
                     */
                    if (lookfor == LOOKFOR_TERM)
                    {
                        if (!lookfor_break && whilelevel == 0)
                            break;
                    }

                    /*
                     * First line above the one we're indenting is terminated.
                     * To know what needs to be done look further backward for
                     * a terminated line.
                     */
                    else
                    {
                        /*
                         * position the cursor over the rightmost paren, so
                         * that matching it will take us back to the start of
                         * the line.  Helps for:
                         *     func(asdr,
                         *            asdfasdf);
                         *     here;
                         */
term_again:
                        l = ml_get_curline();
                        if (find_last_paren(l, '(', ')')
                                && (trypos = find_match_paren(curbuf->b_ind_maxparen)) != NULL)
                        {
                            /*
                             * Check if we are on a case label now.  This is
                             * handled above.
                             *     case xx:  if ( asdf &&
                             *                      asdf)
                             */
                            curwin->w_cursor = *trypos;
                            l = ml_get_curline();
                            if (cin_iscase(l, FALSE) || cin_isscopedecl(l))
                            {
                                ++curwin->w_cursor.lnum;
                                curwin->w_cursor.col = 0;
                                continue;
                            }
                        }

                        /* When aligning with the case statement, don't align
                         * with a statement after it.
                         *  case 1: {   <-- don't use this { position
                         *      stat;
                         *  }
                         *  case 2:
                         *      stat;
                         * }
                         */
                        iscase = (curbuf->b_ind_keep_case_label && cin_iscase(l, FALSE));

                        /*
                         * Get indent and pointer to text for current line,
                         * ignoring any jump label.
                         */
                        amount = skip_label(curwin->w_cursor.lnum, &l);

                        if (theline[0] == '{')
                            amount += curbuf->b_ind_open_extra;
                        /* See remark above: "Only add b_ind_open_extra.." */
                        l = skipwhite(l);
                        if (*l == '{')
                            amount -= curbuf->b_ind_open_extra;
                        lookfor = iscase ? LOOKFOR_ANY : LOOKFOR_TERM;

                        /*
                         * When a terminated line starts with "else" skip to
                         * the matching "if":
                         *       else 3;
                         *           indent this;
                         * Need to use the scope of this "else".  XXX
                         * If whilelevel != 0 continue looking for a "do {".
                         */
                        if (lookfor == LOOKFOR_TERM
                                && *l != '}'
                                && cin_iselse(l)
                                && whilelevel == 0)
                        {
                            if ((trypos = find_start_brace()) == NULL
                                       || find_match(LOOKFOR_IF, trypos->lnum) == FAIL)
                                break;
                            continue;
                        }

                        /*
                         * If we're at the end of a block, skip to the start of that block.
                         */
                        l = ml_get_curline();
                        if (find_last_paren(l, '{', '}') /* XXX */
                                     && (trypos = find_start_brace()) != NULL)
                        {
                            curwin->w_cursor = *trypos;
                            /* if not "else {" check for terminated again */
                            /* but skip block for "} else {" */
                            l = cin_skipcomment(ml_get_curline());
                            if (*l == '}' || !cin_iselse(l))
                                goto term_again;
                            ++curwin->w_cursor.lnum;
                            curwin->w_cursor.col = 0;
                        }
                    }
                }
            }
        }
      }

      /* add extra indent for a comment */
      if (cin_iscomment(theline))
          amount += curbuf->b_ind_comment;

      /* subtract extra left-shift for jump labels */
      if (curbuf->b_ind_jump_label > 0 && original_line_islabel)
          amount -= curbuf->b_ind_jump_label;
    }
    else
    {
        /*
         * ok -- we're not inside any sort of structure at all!
         *
         * This means we're at the top level, and everything should
         * basically just match where the previous line is, except
         * for the lines immediately following a function declaration,
         * which are K&R-style parameters and need to be indented.
         *
         * if our line starts with an open brace, forget about any
         * prevailing indent and make sure it looks like the start
         * of a function
         */

        if (theline[0] == '{')
        {
            amount = curbuf->b_ind_first_open;
        }

        /*
         * If the NEXT line is a function declaration, the current
         * line needs to be indented as a function type spec.
         * Don't do this if the current line looks like a comment or if the
         * current line is terminated, ie. ends in ';', or if the current line
         * contains { or }: "void f() {\n if (1)"
         */
        else if (cur_curpos.lnum < curbuf->b_ml.ml_line_count
                && !cin_nocode(theline)
                && vim_strchr(theline, '{') == NULL
                && vim_strchr(theline, '}') == NULL
                && !cin_ends_in(theline, (char_u *)":", NULL)
                && !cin_ends_in(theline, (char_u *)",", NULL)
                && cin_isfuncdecl(NULL, cur_curpos.lnum + 1, cur_curpos.lnum + 1)
                && !cin_isterminated(theline, FALSE, TRUE))
        {
            amount = curbuf->b_ind_func_type;
        }
        else
        {
            amount = 0;
            curwin->w_cursor = cur_curpos;

            /* search backwards until we find something we recognize */

            while (curwin->w_cursor.lnum > 1)
            {
                curwin->w_cursor.lnum--;
                curwin->w_cursor.col = 0;

                l = ml_get_curline();

                /*
                 * If we're in a comment now, skip to the start of the comment.
                 */                                             /* XXX */
                if ((trypos = ind_find_start_comment()) != NULL)
                {
                    curwin->w_cursor.lnum = trypos->lnum + 1;
                    curwin->w_cursor.col = 0;
                    continue;
                }

                /*
                 * Are we at the start of a cpp base class declaration or
                 * constructor initialization?
                 */                                                 /* XXX */
                n = FALSE;
                if (curbuf->b_ind_cpp_baseclass != 0 && theline[0] != '{')
                {
                    n = cin_is_cpp_baseclass(&col);
                    l = ml_get_curline();
                }
                if (n)
                {
                                                                     /* XXX */
                    amount = get_baseclass_amount(col);
                    break;
                }

                /*
                 * Skip preprocessor directives and blank lines.
                 */
                if (cin_ispreproc_cont(&l, &curwin->w_cursor.lnum))
                    continue;

                if (cin_nocode(l))
                    continue;

                /*
                 * If the previous line ends in ',', use one level of
                 * indentation:
                 * int foo,
                 *     bar;
                 * do this before checking for '}' in case of eg.
                 * enum foobar
                 * {
                 *   ...
                 * } foo,
                 *   bar;
                 */
                n = 0;
                if (cin_ends_in(l, (char_u *)",", NULL) || (*l != NUL && (n = l[STRLEN(l) - 1]) == '\\'))
                {
                    /* take us back to opening paren */
                    if (find_last_paren(l, '(', ')')
                            && (trypos = find_match_paren(curbuf->b_ind_maxparen)) != NULL)
                        curwin->w_cursor = *trypos;

                    /* For a line ending in ',' that is a continuation line go
                     * back to the first line with a backslash:
                     * char *foo = "bla\
                     *           bla",
                     *      here;
                     */
                    while (n == 0 && curwin->w_cursor.lnum > 1)
                    {
                        l = ml_get(curwin->w_cursor.lnum - 1);
                        if (*l == NUL || l[STRLEN(l) - 1] != '\\')
                            break;
                        --curwin->w_cursor.lnum;
                        curwin->w_cursor.col = 0;
                    }

                    amount = get_indent();          /* XXX */

                    if (amount == 0)
                        amount = cin_first_id_amount();
                    if (amount == 0)
                        amount = ind_continuation;
                    break;
                }

                /*
                 * If the line looks like a function declaration, and we're
                 * not in a comment, put it the left margin.
                 */
                if (cin_isfuncdecl(NULL, cur_curpos.lnum, 0))  /* XXX */
                    break;
                l = ml_get_curline();

                /*
                 * Finding the closing '}' of a previous function.  Put
                 * current line at the left margin.  For when 'cino' has "fs".
                 */
                if (*skipwhite(l) == '}')
                    break;

                /*                          (matching {)
                 * If the previous line ends on '};' (maybe followed by
                 * comments) align at column 0.  For example:
                 * char *string_array[] = { "foo",
                 *     / * x * / "b};ar" }; / * foobar * /
                 */
                if (cin_ends_in(l, (char_u *)"};", NULL))
                    break;

                /*
                 * If the previous line ends on '[' we are probably in an
                 * array constant:
                 * something = [
                 *     234,  <- extra indent
                 */
                if (cin_ends_in(l, (char_u *)"[", NULL))
                {
                    amount = get_indent() + ind_continuation;
                    break;
                }

                /*
                 * Find a line only has a semicolon that belongs to a previous
                 * line ending in '}', e.g. before an #endif.  Don't increase
                 * indent then.
                 */
                if (*(look = skipwhite(l)) == ';' && cin_nocode(look + 1))
                {
                    pos_T curpos_save = curwin->w_cursor;

                    while (curwin->w_cursor.lnum > 1)
                    {
                        look = ml_get(--curwin->w_cursor.lnum);
                        if (!(cin_nocode(look) || cin_ispreproc_cont(&look, &curwin->w_cursor.lnum)))
                            break;
                    }
                    if (curwin->w_cursor.lnum > 0 && cin_ends_in(look, (char_u *)"}", NULL))
                        break;

                    curwin->w_cursor = curpos_save;
                }

                /*
                 * If the PREVIOUS line is a function declaration, the current
                 * line (and the ones that follow) needs to be indented as parameters.
                 */
                if (cin_isfuncdecl(&l, curwin->w_cursor.lnum, 0))
                {
                    amount = curbuf->b_ind_param;
                    break;
                }

                /*
                 * If the previous line ends in ';' and the line before the
                 * previous line ends in ',' or '\', ident to column zero:
                 * int foo,
                 *     bar;
                 * indent_to_0 here;
                 */
                if (cin_ends_in(l, (char_u *)";", NULL))
                {
                    l = ml_get(curwin->w_cursor.lnum - 1);
                    if (cin_ends_in(l, (char_u *)",", NULL) || (*l != NUL && l[STRLEN(l) - 1] == '\\'))
                        break;
                    l = ml_get_curline();
                }

                /*
                 * Doesn't look like anything interesting -- so just
                 * use the indent of this line.
                 *
                 * Position the cursor over the rightmost paren, so that
                 * matching it will take us back to the start of the line.
                 */
                find_last_paren(l, '(', ')');

                if ((trypos = find_match_paren(curbuf->b_ind_maxparen)) != NULL)
                    curwin->w_cursor = *trypos;
                amount = get_indent();      /* XXX */
                break;
            }

            /* add extra indent for a comment */
            if (cin_iscomment(theline))
                amount += curbuf->b_ind_comment;

            /* add extra indent if the previous line ended in a backslash:
             *        "asdfasdf\
             *            here";
             *      char *foo = "asdf\
             *                   here";
             */
            if (cur_curpos.lnum > 1)
            {
                l = ml_get(cur_curpos.lnum - 1);
                if (*l != NUL && l[STRLEN(l) - 1] == '\\')
                {
                    cur_amount = cin_get_equal_amount(cur_curpos.lnum - 1);
                    if (cur_amount > 0)
                        amount = cur_amount;
                    else if (cur_amount == 0)
                        amount += ind_continuation;
                }
            }
        }
    }

theend:
    /* put the cursor back where it belongs */
    curwin->w_cursor = cur_curpos;

    vim_free(linecopy);

    if (amount < 0)
        return 0;

    return amount;
}

    static int
find_match(lookfor, ourscope)
    int         lookfor;
    linenr_T    ourscope;
{
    char_u      *look;
    pos_T       *theirscope;
    char_u      *mightbeif;
    int         elselevel;
    int         whilelevel;

    if (lookfor == LOOKFOR_IF)
    {
        elselevel = 1;
        whilelevel = 0;
    }
    else
    {
        elselevel = 0;
        whilelevel = 1;
    }

    curwin->w_cursor.col = 0;

    while (curwin->w_cursor.lnum > ourscope + 1)
    {
        curwin->w_cursor.lnum--;
        curwin->w_cursor.col = 0;

        look = cin_skipcomment(ml_get_curline());
        if (cin_iselse(look)
                || cin_isif(look)
                || cin_isdo(look)                           /* XXX */
                || cin_iswhileofdo(look, curwin->w_cursor.lnum))
        {
            /*
             * if we've gone outside the braces entirely,
             * we must be out of scope...
             */
            theirscope = find_start_brace();  /* XXX */
            if (theirscope == NULL)
                break;

            /*
             * and if the brace enclosing this is further
             * back than the one enclosing the else, we're
             * out of luck too.
             */
            if (theirscope->lnum < ourscope)
                break;

            /*
             * and if they're enclosed in a *deeper* brace,
             * then we can ignore it because it's in a
             * different scope...
             */
            if (theirscope->lnum > ourscope)
                continue;

            /*
             * if it was an "else" (that's not an "else if")
             * then we need to go back to another if, so
             * increment elselevel
             */
            look = cin_skipcomment(ml_get_curline());
            if (cin_iselse(look))
            {
                mightbeif = cin_skipcomment(look + 4);
                if (!cin_isif(mightbeif))
                    ++elselevel;
                continue;
            }

            /*
             * if it was a "while" then we need to go back to
             * another "do", so increment whilelevel.  XXX
             */
            if (cin_iswhileofdo(look, curwin->w_cursor.lnum))
            {
                ++whilelevel;
                continue;
            }

            /* If it's an "if" decrement elselevel */
            look = cin_skipcomment(ml_get_curline());
            if (cin_isif(look))
            {
                elselevel--;
                /*
                 * When looking for an "if" ignore "while"s that get in the way.
                 */
                if (elselevel == 0 && lookfor == LOOKFOR_IF)
                    whilelevel = 0;
            }

            /* If it's a "do" decrement whilelevel */
            if (cin_isdo(look))
                whilelevel--;

            /*
             * if we've used up all the elses, then
             * this must be the if that we want!
             * match the indent level of that if.
             */
            if (elselevel <= 0 && whilelevel <= 0)
            {
                return OK;
            }
        }
    }
    return FAIL;
}

/*
 * Get indent level from 'indentexpr'.
 */
    int
get_expr_indent()
{
    int         indent;
    pos_T       save_pos;
    colnr_T     save_curswant;
    int         save_set_curswant;
    int         save_State;
    int         use_sandbox = was_set_insecurely((char_u *)"indentexpr", OPT_LOCAL);

    /* Save and restore cursor position and curswant, in case it was changed
     * via :normal commands */
    save_pos = curwin->w_cursor;
    save_curswant = curwin->w_curswant;
    save_set_curswant = curwin->w_set_curswant;
    set_vim_var_nr(VV_LNUM, curwin->w_cursor.lnum);
    if (use_sandbox)
        ++sandbox;
    ++textlock;
    indent = eval_to_number(curbuf->b_p_inde);
    if (use_sandbox)
        --sandbox;
    --textlock;

    /* Restore the cursor position so that 'indentexpr' doesn't need to.
     * Pretend to be in Insert mode, allow cursor past end of line for "o" command. */
    save_State = State;
    State = INSERT;
    curwin->w_cursor = save_pos;
    curwin->w_curswant = save_curswant;
    curwin->w_set_curswant = save_set_curswant;
    check_cursor();
    State = save_State;

    /* If there is an error, just keep the current indent. */
    if (indent < 0)
        indent = get_indent();

    return indent;
}

static int lisp_match(char_u *p);

    static int
lisp_match(p)
    char_u      *p;
{
    char_u      buf[LSIZE];
    int         len;
    char_u      *word = *curbuf->b_p_lw != NUL ? curbuf->b_p_lw : p_lispwords;

    while (*word != NUL)
    {
        (void)copy_option_part(&word, buf, LSIZE, ",");
        len = (int)STRLEN(buf);
        if (STRNCMP(buf, p, len) == 0 && p[len] == ' ')
            return TRUE;
    }
    return FALSE;
}

/*
 * When 'p' is present in 'cpoptions, a Vi compatible method is used.
 * The incompatible newer method is quite a bit better at indenting
 * code in lisp-like languages than the traditional one; it's still
 * mostly heuristics however -- Dirk van Deun, dirk@rave.org
 *
 * TODO:
 * Findmatch() should be adapted for lisp, also to make showmatch
 * work correctly: now (v5.3) it seems all C/C++ oriented:
 * - it does not recognize the #\( and #\) notations as character literals
 * - it doesn't know about comments starting with a semicolon
 * - it incorrectly interprets '(' as a character literal
 * All this messes up get_lisp_indent in some rare cases.
 * Update from Sergey Khorev:
 * I tried to fix the first two issues.
 */
    int
get_lisp_indent()
{
    pos_T       *pos, realpos, paren;
    int         amount;
    char_u      *that;
    colnr_T     col;
    colnr_T     firsttry;
    int         parencount, quotecount;
    int         vi_lisp;

    /* Set vi_lisp to use the vi-compatible method */
    vi_lisp = (vim_strchr(p_cpo, CPO_LISP) != NULL);

    realpos = curwin->w_cursor;
    curwin->w_cursor.col = 0;

    if ((pos = findmatch(NULL, '(')) == NULL)
        pos = findmatch(NULL, '[');
    else
    {
        paren = *pos;
        pos = findmatch(NULL, '[');
        if (pos == NULL || ltp(pos, &paren))
            pos = &paren;
    }
    if (pos != NULL)
    {
        /* Extra trick: Take the indent of the first previous non-white
         * line that is at the same () level. */
        amount = -1;
        parencount = 0;

        while (--curwin->w_cursor.lnum >= pos->lnum)
        {
            if (linewhite(curwin->w_cursor.lnum))
                continue;
            for (that = ml_get_curline(); *that != NUL; ++that)
            {
                if (*that == ';')
                {
                    while (*(that + 1) != NUL)
                        ++that;
                    continue;
                }
                if (*that == '\\')
                {
                    if (*(that + 1) != NUL)
                        ++that;
                    continue;
                }
                if (*that == '"' && *(that + 1) != NUL)
                {
                    while (*++that && *that != '"')
                    {
                        /* skipping escaped characters in the string */
                        if (*that == '\\')
                        {
                            if (*++that == NUL)
                                break;
                            if (that[1] == NUL)
                            {
                                ++that;
                                break;
                            }
                        }
                    }
                }
                if (*that == '(' || *that == '[')
                    ++parencount;
                else if (*that == ')' || *that == ']')
                    --parencount;
            }
            if (parencount == 0)
            {
                amount = get_indent();
                break;
            }
        }

        if (amount == -1)
        {
            curwin->w_cursor.lnum = pos->lnum;
            curwin->w_cursor.col = pos->col;
            col = pos->col;

            that = ml_get_curline();

            if (vi_lisp && get_indent() == 0)
                amount = 2;
            else
            {
                char_u *line = that;

                amount = 0;
                while (*that && col)
                {
                    amount += lbr_chartabsize_adv(line, &that, (colnr_T)amount);
                    col--;
                }

                /*
                 * Some keywords require "body" indenting rules (the
                 * non-standard-lisp ones are Scheme special forms):
                 *
                 * (let ((a 1))    instead    (let ((a 1))
                 *   (...))           of           (...))
                 */

                if (!vi_lisp && (*that == '(' || *that == '[') && lisp_match(that + 1))
                    amount += 2;
                else
                {
                    that++;
                    amount++;
                    firsttry = amount;

                    while (vim_iswhite(*that))
                    {
                        amount += lbr_chartabsize(line, that, (colnr_T)amount);
                        ++that;
                    }

                    if (*that && *that != ';') /* not a comment line */
                    {
                        /* test *that != '(' to accommodate first let/do
                         * argument if it is more than one line */
                        if (!vi_lisp && *that != '(' && *that != '[')
                            firsttry++;

                        parencount = 0;
                        quotecount = 0;

                        if (vi_lisp
                                || (*that != '"'
                                    && *that != '\''
                                    && *that != '#'
                                    && (*that < '0' || *that > '9')))
                        {
                            while (*that
                                    && (!vim_iswhite(*that)
                                        || quotecount
                                        || parencount)
                                    && (!((*that == '(' || *that == '[')
                                            && !quotecount
                                            && !parencount
                                            && vi_lisp)))
                            {
                                if (*that == '"')
                                    quotecount = !quotecount;
                                if ((*that == '(' || *that == '[') && !quotecount)
                                    ++parencount;
                                if ((*that == ')' || *that == ']') && !quotecount)
                                    --parencount;
                                if (*that == '\\' && *(that+1) != NUL)
                                    amount += lbr_chartabsize_adv(line, &that, (colnr_T)amount);
                                amount += lbr_chartabsize_adv(line, &that, (colnr_T)amount);
                            }
                        }
                        while (vim_iswhite(*that))
                        {
                            amount += lbr_chartabsize(line, that, (colnr_T)amount);
                            that++;
                        }
                        if (!*that || *that == ';')
                            amount = firsttry;
                    }
                }
            }
        }
    }
    else
        amount = 0;     /* no matching '(' or '[' found, use zero indent */

    curwin->w_cursor = realpos;

    return amount;
}

    void
prepare_to_exit()
{
#if defined(SIGHUP) && defined(SIG_IGN)
    /* Ignore SIGHUP, because a dropped connection causes a read error, which
     * makes Vim exit and then handling SIGHUP causes various reentrance problems.
     */
    signal(SIGHUP, SIG_IGN);
#endif

    {
        windgoto((int)Rows - 1, 0);

        /*
         * Switch terminal mode back now, so messages end up on the "normal"
         * screen (if there are two screens).
         */
        settmode(TMODE_COOK);
        stoptermcap();
        out_flush();
    }
}

/*
 * Preserve files and exit.
 * When called IObuff must contain a message.
 * NOTE: This may be called from deathtrap() in a signal handler, avoid unsafe
 * functions, such as allocating memory.
 */
    void
preserve_exit()
{
    buf_T       *buf;

    prepare_to_exit();

    /* Setting this will prevent free() calls.  That avoids calling free()
     * recursively when free() was invoked with a bad pointer. */
    really_exiting = TRUE;

    out_str(IObuff);
    screen_start();                 /* don't know where cursor is now */
    out_flush();

    ml_close_notmod();              /* close all not-modified buffers */

    for (buf = firstbuf; buf != NULL; buf = buf->b_next)
    {
        if (buf->b_ml.ml_mfp != NULL && buf->b_ml.ml_mfp->mf_fname != NULL)
        {
            OUT_STR("Vim: preserving files...\n");
            screen_start();         /* don't know where cursor is now */
            out_flush();
            ml_sync_all(FALSE, FALSE);  /* preserve all swap files */
            break;
        }
    }

    ml_close_all(FALSE);            /* close all memfiles, without deleting */

    OUT_STR("Vim: Finished.\n");

    getout(1);
}

/*
 * return TRUE if "fname" exists.
 */
    int
vim_fexists(fname)
    char_u  *fname;
{
    struct stat st;

    if (mch_stat((char *)fname, &st))
        return FALSE;

    return TRUE;
}

/*
 * Check for CTRL-C pressed, but only once in a while.
 * Should be used instead of ui_breakcheck() for functions that check for
 * each line in the file.  Calling ui_breakcheck() each time takes too much
 * time, because it can be a system call.
 */

#if !defined(BREAKCHECK_SKIP)
#define BREAKCHECK_SKIP 32
#endif

static int      breakcheck_count = 0;

    void
line_breakcheck()
{
    if (++breakcheck_count >= BREAKCHECK_SKIP)
    {
        breakcheck_count = 0;
        ui_breakcheck();
    }
}

/*
 * Like line_breakcheck() but check 10 times less often.
 */
    void
fast_breakcheck()
{
    if (++breakcheck_count >= BREAKCHECK_SKIP * 10)
    {
        breakcheck_count = 0;
        ui_breakcheck();
    }
}

/*
 * Invoke expand_wildcards() for one pattern.
 * Expand items like "%:h" before the expansion.
 * Returns OK or FAIL.
 */
    int
expand_wildcards_eval(pat, num_file, file, flags)
    char_u       **pat;         /* pointer to input pattern */
    int           *num_file;    /* resulting number of files */
    char_u      ***file;        /* array of resulting files */
    int            flags;       /* EW_DIR, etc. */
{
    int         ret = FAIL;
    char_u      *eval_pat = NULL;
    char_u      *exp_pat = *pat;
    char_u      *ignored_msg;
    int         usedlen;

    if (*exp_pat == '%' || *exp_pat == '#' || *exp_pat == '<')
    {
        ++emsg_off;
        eval_pat = eval_vars(exp_pat, exp_pat, &usedlen, NULL, &ignored_msg, NULL);
        --emsg_off;
        if (eval_pat != NULL)
            exp_pat = concat_str(eval_pat, exp_pat + usedlen);
    }

    if (exp_pat != NULL)
        ret = expand_wildcards(1, &exp_pat, num_file, file, flags);

    if (eval_pat != NULL)
    {
        vim_free(exp_pat);
        vim_free(eval_pat);
    }

    return ret;
}

/*
 * Expand wildcards.  Calls gen_expand_wildcards() and removes files matching 'wildignore'.
 * Returns OK or FAIL.  When FAIL then "num_file" won't be set.
 */
    int
expand_wildcards(num_pat, pat, num_file, file, flags)
    int            num_pat;     /* number of input patterns */
    char_u       **pat;         /* array of input patterns */
    int           *num_file;    /* resulting number of files */
    char_u      ***file;        /* array of resulting files */
    int            flags;       /* EW_DIR, etc. */
{
    int         retval;
    int         i, j;
    char_u      *p;
    int         non_suf_match;  /* number without matching suffix */

    retval = gen_expand_wildcards(num_pat, pat, num_file, file, flags);

    /* When keeping all matches, return here */
    if ((flags & EW_KEEPALL) || retval == FAIL)
        return retval;

    /*
     * Move the names where 'suffixes' match to the end.
     */
    if (*num_file > 1)
    {
        non_suf_match = 0;
        for (i = 0; i < *num_file; ++i)
        {
            if (!match_suffix((*file)[i]))
            {
                /*
                 * Move the name without matching suffix to the front of the list.
                 */
                p = (*file)[i];
                for (j = i; j > non_suf_match; --j)
                    (*file)[j] = (*file)[j - 1];
                (*file)[non_suf_match++] = p;
            }
        }
    }

    return retval;
}

/*
 * Return TRUE if "fname" matches with an entry in 'suffixes'.
 */
    int
match_suffix(fname)
    char_u      *fname;
{
    int         fnamelen, setsuflen;
    char_u      *setsuf;
#define MAXSUFLEN 30        /* maximum length of a file suffix */
    char_u      suf_buf[MAXSUFLEN];

    fnamelen = (int)STRLEN(fname);
    setsuflen = 0;
    for (setsuf = p_su; *setsuf; )
    {
        setsuflen = copy_option_part(&setsuf, suf_buf, MAXSUFLEN, ".,");
        if (setsuflen == 0)
        {
            char_u *tail = gettail(fname);

            /* empty entry: match name without a '.' */
            if (vim_strchr(tail, '.') == NULL)
            {
                setsuflen = 1;
                break;
            }
        }
        else
        {
            if (fnamelen >= setsuflen
                    && fnamencmp(suf_buf, fname + fnamelen - setsuflen, (size_t)setsuflen) == 0)
                break;
            setsuflen = 0;
        }
    }
    return (setsuflen != 0);
}

static int vim_backtick(char_u *p);
static int expand_backtick(garray_T *gap, char_u *pat, int flags);

/*
 * Unix style wildcard expansion code.
 * It's here because it's used both for Unix and Mac.
 */
static int      pstrcmp(const void *, const void *);

    static int
pstrcmp(a, b)
    const void *a, *b;
{
    return (pathcmp(*(char **)a, *(char **)b, -1));
}

/*
 * Recursively expand one path component into all matching files and/or
 * directories.  Adds matches to "gap".  Handles "*", "?", "[a-z]", "**", etc.
 * "path" has backslashes before chars that are not to be expanded, starting
 * at "path + wildoff".
 * Return the number of matches found.
 * NOTE: much of this is identical to dos_expandpath(), keep in sync!
 */
    int
unix_expandpath(gap, path, wildoff, flags, didstar)
    garray_T    *gap;
    char_u      *path;
    int         wildoff;
    int         flags;          /* EW_* flags */
    int         didstar;        /* expanded "**" once already */
{
    char_u      *buf;
    char_u      *path_end;
    char_u      *p, *s, *e;
    int         start_len = gap->ga_len;
    char_u      *pat;
    regmatch_T  regmatch;
    int         starts_with_dot;
    int         matches;
    int         len;
    int         starstar = FALSE;
    static int  stardepth = 0;      /* depth for "**" expansion */

    DIR         *dirp;
    struct dirent *dp;

    /* Expanding "**" may take a long time, check for CTRL-C. */
    if (stardepth > 0)
    {
        ui_breakcheck();
        if (got_int)
            return 0;
    }

    /* make room for file name */
    buf = alloc((int)STRLEN(path) + BASENAMELEN + 5);
    if (buf == NULL)
        return 0;

    /*
     * Find the first part in the path name that contains a wildcard.
     * When EW_ICASE is set every letter is considered to be a wildcard.
     * Copy it into "buf", including the preceding characters.
     */
    p = buf;
    s = buf;
    e = NULL;
    path_end = path;
    while (*path_end != NUL)
    {
        /* May ignore a wildcard that has a backslash before it; it will
         * be removed by rem_backslash() or file_pat_to_reg_pat() below. */
        if (path_end >= path + wildoff && rem_backslash(path_end))
            *p++ = *path_end++;
        else if (*path_end == '/')
        {
            if (e != NULL)
                break;
            s = p + 1;
        }
        else if (path_end >= path + wildoff
                         && (vim_strchr((char_u *)"*?[{~$", *path_end) != NULL
                             || (!p_fic && (flags & EW_ICASE)
                                             && isalpha(utf_ptr2char(path_end)))))
            e = p;

        len = utfc_ptr2len(path_end);
        STRNCPY(p, path_end, len);
        p += len;
        path_end += len;
    }
    e = p;
    *e = NUL;

    /* Now we have one wildcard component between "s" and "e". */
    /* Remove backslashes between "wildoff" and the start of the wildcard component. */
    for (p = buf + wildoff; p < s; ++p)
        if (rem_backslash(p))
        {
            STRMOVE(p, p + 1);
            --e;
            --s;
        }

    /* Check for "**" between "s" and "e". */
    for (p = s; p < e; ++p)
        if (p[0] == '*' && p[1] == '*')
            starstar = TRUE;

    /* convert the file pattern to a regexp pattern */
    starts_with_dot = (*s == '.');
    pat = file_pat_to_reg_pat(s, e, NULL, FALSE);
    if (pat == NULL)
    {
        vim_free(buf);
        return 0;
    }

    /* compile the regexp into a program */
    if (flags & EW_ICASE)
        regmatch.rm_ic = TRUE;          /* 'wildignorecase' set */
    else
        regmatch.rm_ic = p_fic; /* ignore case when 'fileignorecase' is set */
    if (flags & (EW_NOERROR | EW_NOTWILD))
        ++emsg_silent;
    regmatch.regprog = vim_regcomp(pat, RE_MAGIC);
    if (flags & (EW_NOERROR | EW_NOTWILD))
        --emsg_silent;
    vim_free(pat);

    if (regmatch.regprog == NULL && (flags & EW_NOTWILD) == 0)
    {
        vim_free(buf);
        return 0;
    }

    /* If "**" is by itself, this is the first time we encounter it and more
     * is following then find matches without any directory. */
    if (!didstar && stardepth < 100 && starstar && e - s == 2 && *path_end == '/')
    {
        STRCPY(s, path_end + 1);
        ++stardepth;
        (void)unix_expandpath(gap, buf, (int)(s - buf), flags, TRUE);
        --stardepth;
    }

    /* open the directory for scanning */
    *s = NUL;
    dirp = opendir(*buf == NUL ? "." : (char *)buf);

    /* Find all matching entries */
    if (dirp != NULL)
    {
        for (;;)
        {
            dp = readdir(dirp);
            if (dp == NULL)
                break;
            if ((dp->d_name[0] != '.' || starts_with_dot)
                && ((regmatch.regprog != NULL && vim_regexec(&regmatch, (char_u *)dp->d_name, (colnr_T)0))
                   || ((flags & EW_NOTWILD) && fnamencmp(path + (s - buf), dp->d_name, e - s) == 0)))
            {
                STRCPY(s, dp->d_name);
                len = STRLEN(buf);

                if (starstar && stardepth < 100)
                {
                    /* For "**" in the pattern first go deeper in the tree to find matches. */
                    STRCPY(buf + len, "/**");
                    STRCPY(buf + len + 3, path_end);
                    ++stardepth;
                    (void)unix_expandpath(gap, buf, len + 1, flags, TRUE);
                    --stardepth;
                }

                STRCPY(buf + len, path_end);
                if (mch_has_exp_wildcard(path_end)) /* handle more wildcards */
                {
                    /* need to expand another component of the path */
                    /* remove backslashes for the remaining components only */
                    (void)unix_expandpath(gap, buf, len + 1, flags, FALSE);
                }
                else
                {
                    struct stat sb;

                    /* no more wildcards, check if there is a match */
                    /* remove backslashes for the remaining components only */
                    if (*path_end != NUL)
                        backslash_halve(buf + len + 1);
                    /* add existing file or symbolic link */
                    if ((flags & EW_ALLLINKS) ? mch_lstat((char *)buf, &sb) >= 0 : mch_getperm(buf) >= 0)
                    {
                        addfile(gap, buf, flags);
                    }
                }
            }
        }

        closedir(dirp);
    }

    vim_free(buf);
    vim_regfree(regmatch.regprog);

    matches = gap->ga_len - start_len;
    if (matches > 0)
        qsort(((char_u **)gap->ga_data) + start_len, matches, sizeof(char_u *), pstrcmp);
    return matches;
}

/*
 * Sort "gap" and remove duplicate entries.  "gap" is expected to contain a
 * list of file names in allocated memory.
 */
    void
remove_duplicates(gap)
    garray_T    *gap;
{
    int     i;
    int     j;
    char_u  **fnames = (char_u **)gap->ga_data;

    sort_strings(fnames, gap->ga_len);
    for (i = gap->ga_len - 1; i > 0; --i)
        if (fnamecmp(fnames[i - 1], fnames[i]) == 0)
        {
            vim_free(fnames[i]);
            for (j = i + 1; j < gap->ga_len; ++j)
                fnames[j - 1] = fnames[j];
            --gap->ga_len;
        }
}

static int has_env_var(char_u *p);

/*
 * Return TRUE if "p" contains what looks like an environment variable.
 * Allowing for escaping.
 */
    static int
has_env_var(p)
    char_u *p;
{
    for ( ; *p; p += utfc_ptr2len(p))
    {
        if (*p == '\\' && p[1] != NUL)
            ++p;
        else if (vim_strchr((char_u *)"$", *p) != NULL)
            return TRUE;
    }
    return FALSE;
}

/* Special wildcards that need to be handled by the shell */
#define SPECIAL_WILDCHAR    "`'{"

static int has_special_wildchar(char_u *p);

/*
 * Return TRUE if "p" contains a special wildcard character.
 * Allowing for escaping.
 */
    static int
has_special_wildchar(p)
    char_u  *p;
{
    for ( ; *p; p += utfc_ptr2len(p))
    {
        if (*p == '\\' && p[1] != NUL)
            ++p;
        else if (vim_strchr((char_u *)SPECIAL_WILDCHAR, *p) != NULL)
            return TRUE;
    }
    return FALSE;
}

/*
 * Generic wildcard expansion code.
 *
 * Characters in "pat" that should not be expanded must be preceded with a
 * backslash. E.g., "/path\ with\ spaces/my\*star*"
 *
 * Return FAIL when no single file was found.  In this case "num_file" is not
 * set, and "file" may contain an error message.
 * Return OK when some files found.  "num_file" is set to the number of
 * matches, "file" to the array of matches.  Call FreeWild() later.
 */
    int
gen_expand_wildcards(num_pat, pat, num_file, file, flags)
    int         num_pat;        /* number of input patterns */
    char_u      **pat;          /* array of input patterns */
    int         *num_file;      /* resulting number of files */
    char_u      ***file;        /* array of resulting files */
    int         flags;          /* EW_* flags */
{
    int                 i;
    garray_T            ga;
    char_u              *p;
    static int          recursive = FALSE;
    int                 add_pat;

    /*
     * expand_env() is called to expand things like "~user".  If this fails,
     * it calls ExpandOne(), which brings us back here.  In this case, always
     * call the machine specific expansion function, if possible.  Otherwise,
     * return FAIL.
     */
    if (recursive)
        return mch_expand_wildcards(num_pat, pat, num_file, file, flags);

    /*
     * If there are any special wildcard characters which we cannot handle
     * here, call machine specific function for all the expansion.  This
     * avoids starting the shell for each argument separately.
     * For `=expr` do use the internal function.
     */
    for (i = 0; i < num_pat; i++)
    {
        if (has_special_wildchar(pat[i]) && !(vim_backtick(pat[i]) && pat[i][1] == '='))
            return mch_expand_wildcards(num_pat, pat, num_file, file, flags);
    }

    recursive = TRUE;

    /*
     * The matching file names are stored in a growarray.  Init it empty.
     */
    ga_init2(&ga, (int)sizeof(char_u *), 30);

    for (i = 0; i < num_pat; ++i)
    {
        add_pat = -1;
        p = pat[i];

        if (vim_backtick(p))
            add_pat = expand_backtick(&ga, p, flags);
        else
        {
            /*
             * First expand environment variables, "~/" and "~user/".
             */
            if (has_env_var(p) || *p == '~')
            {
                p = expand_env_save_opt(p, TRUE);
                if (p == NULL)
                    p = pat[i];
                /*
                 * On Unix, if expand_env() can't expand an environment
                 * variable, use the shell to do that.  Discard previously
                 * found file names and start all over again.
                 */
                else if (has_env_var(p) || *p == '~')
                {
                    vim_free(p);
                    ga_clear_strings(&ga);
                    i = mch_expand_wildcards(num_pat, pat, num_file, file, flags|EW_KEEPDOLLAR);
                    recursive = FALSE;
                    return i;
                }
            }

            /*
             * If there are wildcards: Expand file names and add each match to
             * the list.  If there is no match, and EW_NOTFOUND is given, add the pattern.
             * If there are no wildcards: Add the file name if it exists or
             * when EW_NOTFOUND is given.
             */
            if (mch_has_exp_wildcard(p))
            {
                add_pat = mch_expandpath(&ga, p, flags);
            }
        }

        if (add_pat == -1 || (add_pat == 0 && (flags & EW_NOTFOUND)))
        {
            char_u      *t = backslash_halve_save(p);

            /* When EW_NOTFOUND is used, always add files and dirs.  Makes "vim c:/" work. */
            if (flags & EW_NOTFOUND)
                addfile(&ga, t, flags | EW_DIR | EW_FILE);
            else if (mch_getperm(t) >= 0)
                addfile(&ga, t, flags);
            vim_free(t);
        }

        if (p != pat[i])
            vim_free(p);
    }

    *num_file = ga.ga_len;
    *file = (ga.ga_data != NULL) ? (char_u **)ga.ga_data : (char_u **)"";

    recursive = FALSE;

    return (ga.ga_data != NULL) ? OK : FAIL;
}

/*
 * Return TRUE if we can expand this backtick thing here.
 */
    static int
vim_backtick(p)
    char_u      *p;
{
    return (*p == '`' && *(p + 1) != NUL && *(p + STRLEN(p) - 1) == '`');
}

/*
 * Expand an item in `backticks` by executing it as a command.
 * Currently only works when pat[] starts and ends with a `.
 * Returns number of file names found.
 */
    static int
expand_backtick(gap, pat, flags)
    garray_T    *gap;
    char_u      *pat;
    int         flags;  /* EW_* flags */
{
    char_u      *p;
    char_u      *cmd;
    char_u      *buffer;
    int         cnt = 0;
    int         i;

    /* Create the command: lop off the backticks. */
    cmd = vim_strnsave(pat + 1, (int)STRLEN(pat) - 2);
    if (cmd == NULL)
        return 0;

    if (*cmd == '=')        /* `={expr}`: Expand expression */
        buffer = eval_to_string(cmd + 1, &p, TRUE);
    else
        buffer = get_cmd_output(cmd, NULL, (flags & EW_SILENT) ? SHELL_SILENT : 0, NULL);
    vim_free(cmd);
    if (buffer == NULL)
        return 0;

    cmd = buffer;
    while (*cmd != NUL)
    {
        cmd = skipwhite(cmd);           /* skip over white space */
        p = cmd;
        while (*p != NUL && *p != '\r' && *p != '\n') /* skip over entry */
            ++p;
        /* add an entry if it is not empty */
        if (p > cmd)
        {
            i = *p;
            *p = NUL;
            addfile(gap, cmd, flags);
            *p = i;
            ++cnt;
        }
        cmd = p;
        while (*cmd != NUL && (*cmd == '\r' || *cmd == '\n'))
            ++cmd;
    }

    vim_free(buffer);
    return cnt;
}

/*
 * Add a file to a file list.  Accepted flags:
 * EW_DIR       add directories
 * EW_FILE      add files
 * EW_EXEC      add executable files
 * EW_NOTFOUND  add even when it doesn't exist
 * EW_ADDSLASH  add slash after directory name
 * EW_ALLLINKS  add symlink also when the referred file does not exist
 */
    void
addfile(gap, f, flags)
    garray_T    *gap;
    char_u      *f;     /* filename */
    int         flags;
{
    char_u      *p;
    int         isdir;
    struct stat sb;

    /* if the file/dir/link doesn't exist, may not add it */
    if (!(flags & EW_NOTFOUND) && ((flags & EW_ALLLINKS) ? mch_lstat((char *)f, &sb) < 0 : mch_getperm(f) < 0))
        return;

#if defined(FNAME_ILLEGAL)
    /* if the file/dir contains illegal characters, don't add it */
    if (vim_strpbrk(f, (char_u *)FNAME_ILLEGAL) != NULL)
        return;
#endif

    isdir = mch_isdir(f);
    if ((isdir && !(flags & EW_DIR)) || (!isdir && !(flags & EW_FILE)))
        return;

    /* If the file isn't executable, may not add it.  Do accept directories.
     * When invoked from expand_shellcmd() do not use $PATH. */
    if (!isdir && (flags & EW_EXEC) && !mch_can_exe(f, NULL, !(flags & EW_SHELLCMD)))
        return;

    /* Make room for another item in the file list. */
    if (ga_grow(gap, 1) == FAIL)
        return;

    p = alloc((unsigned)(STRLEN(f) + 1 + isdir));
    if (p == NULL)
        return;

    STRCPY(p, f);
    /*
     * Append a slash or backslash after directory names if none is present.
     */
    if (isdir && (flags & EW_ADDSLASH))
        add_pathsep(p);
    ((char_u **)gap->ga_data)[gap->ga_len++] = p;
}

/*
 * Get the stdout of an external command.
 * If "ret_len" is NULL replace NUL characters with NL.  When "ret_len" is not
 * NULL store the length there.
 * Returns an allocated string, or NULL for error.
 */
    char_u *
get_cmd_output(cmd, infile, flags, ret_len)
    char_u      *cmd;
    char_u      *infile;        /* optional input file name */
    int         flags;          /* can be SHELL_SILENT */
    int         *ret_len;
{
    char_u      *tempname;
    char_u      *command;
    char_u      *buffer = NULL;
    int         len;
    int         i = 0;
    FILE        *fd;

    if (check_restricted() || check_secure())
        return NULL;

    /* get a name for the temp file */
    if ((tempname = vim_tempname('o', FALSE)) == NULL)
    {
        EMSG((char *)e_notmp);
        return NULL;
    }

    /* Add the redirection stuff */
    command = make_filter_cmd(cmd, infile, tempname);
    if (command == NULL)
        goto done;

    /*
     * Call the shell to execute the command (errors are ignored).
     * Don't check timestamps here.
     */
    ++no_check_timestamps;
    call_shell(command, SHELL_DOOUT | SHELL_EXPAND | flags);
    --no_check_timestamps;

    vim_free(command);

    /*
     * read the names from the file into memory
     */
    fd = mch_fopen((char *)tempname, READBIN);

    if (fd == NULL)
    {
        EMSG2((char *)e_notopen, tempname);
        goto done;
    }

    fseek(fd, 0L, SEEK_END);
    len = ftell(fd);                /* get size of temp file */
    fseek(fd, 0L, SEEK_SET);

    buffer = alloc(len + 1);
    if (buffer != NULL)
        i = (int)fread((char *)buffer, (size_t)1, (size_t)len, fd);
    fclose(fd);
    unlink((char *)tempname);
    if (buffer == NULL)
        goto done;
    if (i != len)
    {
        EMSG2((char *)e_notread, tempname);
        vim_free(buffer);
        buffer = NULL;
    }
    else if (ret_len == NULL)
    {
        /* Change NUL into SOH, otherwise the string is truncated. */
        for (i = 0; i < len; ++i)
            if (buffer[i] == NUL)
                buffer[i] = 1;

        buffer[len] = NUL;      /* make sure the buffer is terminated */
    }
    else
        *ret_len = len;

done:
    vim_free(tempname);
    return buffer;
}

/*
 * Free the list of files returned by expand_wildcards() or other expansion functions.
 */
    void
FreeWild(count, files)
    int     count;
    char_u  **files;
{
    if (count <= 0 || files == NULL)
        return;
    while (count--)
        vim_free(files[count]);
    vim_free(files);
}

/*
 * Return TRUE when need to go to Insert mode because of 'insertmode'.
 * Don't do this when still processing a command or a mapping.
 * Don't do this when inside a ":normal" command.
 */
    int
goto_im()
{
    return (p_im && stuff_empty() && typebuf_typed());
}

/*
 * Returns the isolated name of the shell in allocated memory:
 * - Skip beyond any path.  E.g., "/usr/bin/csh -f" -> "csh -f".
 * - Remove any argument.  E.g., "csh -f" -> "csh".
 * But don't allow a space in the path, so that this works:
 *   "/usr/bin/csh --rcfile ~/.cshrc"
 * But don't do that for Windows, it's common to have a space in the path.
 */
    char_u *
get_isolated_shell_name()
{
    char_u *p;

    p = skiptowhite(p_sh);
    if (*p == NUL)
    {
        /* No white space, use the tail. */
        p = vim_strsave(gettail(p_sh));
    }
    else
    {
        char_u  *p1, *p2;

        /* Find the last path separator before the space. */
        p1 = p_sh;
        for (p2 = p_sh; p2 < p; p2 += utfc_ptr2len(p2))
            if (vim_ispathsep(*p2))
                p1 = p2 + 1;
        p = vim_strnsave(p1, (int)(p - p1));
    }
    return p;
}

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
    for (int i = 0; i < gap->ga_len; ++i)
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
    c = TOUPPER_ASC(c);
    for (int i = 0; mod_mask_table[i].mod_mask != 0; i++)
        if (c == mod_mask_table[i].name)
            return mod_mask_table[i].mod_flag;

    return 0;
}

/*
 * Check if if there is a special key code for "key" that includes the modifiers specified.
 */
    int
simplify_key(key, modifiers)
    int     key;
    int     *modifiers;
{
    if (*modifiers & (MOD_MASK_SHIFT | MOD_MASK_CTRL | MOD_MASK_ALT))
    {
        int     key0;
        int     key1;

        /* TAB is a special case */
        if (key == TAB && (*modifiers & MOD_MASK_SHIFT))
        {
            *modifiers &= ~MOD_MASK_SHIFT;
            return K_S_TAB;
        }

        key0 = KEY2TERMCAP0(key);
        key1 = KEY2TERMCAP1(key);
        for (int i = 0; modifier_keys_table[i] != NUL; i += MOD_KEYS_ENTRY_SIZE)
            if (key0 == modifier_keys_table[i + 3] && key1 == modifier_keys_table[i + 4] && (*modifiers & modifier_keys_table[i]))
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
        if (table_idx < 0 && (!vim_isprintc(c) || (c & 0x7f) == ' ') && (c & 0x80))
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
    long_u      n;
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
    for (int i = 0; mouse_table[i].pseudo_code; i++)
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
    for (int i = 0; mouse_table[i].pseudo_code; i++)
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

            return (p_fic) ? vim_toupper(c1) - vim_toupper(c2) : c1 - c2;  /* no match */
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
    for (int i = 0; i < 8; ++i)
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
