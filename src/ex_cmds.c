/*
 * ex_cmds.c: some functions for command line commands
 */

#include "vim.h"

static int linelen(int *has_tab);
static void do_filter(linenr_T line1, linenr_T line2, exarg_T *eap, char_u *cmd, int do_in, int do_out);

static int check_readonly(int *forceit, buf_T *buf);
static void delbuf_msg(char_u *name);

/*
 * ":ascii" and "ga".
 */
    void
do_ascii(eap)
    exarg_T     *eap UNUSED;
{
    int         c;
    int         cval;
    char        buf1[20];
    char        buf2[20];
    char_u      buf3[7];
    int         cc[MAX_MCO];
    int         ci = 0;
    int         len;

    c = utfc_ptr2char(ml_get_cursor(), cc);
    if (c == NUL)
    {
        MSG("NUL");
        return;
    }

    IObuff[0] = NUL;
    if (c < 0x80)
    {
        if (c == NL)        /* NUL is stored as NL */
            c = NUL;
        if (c == CAR && get_fileformat(curbuf) == EOL_MAC)
            cval = NL;      /* NL is stored as CR */
        else
            cval = c;
        if (vim_isprintc_strict(c) && (c < ' ' || c > '~'))
        {
            transchar_nonprint(buf3, c);
            vim_snprintf(buf1, sizeof(buf1), "  <%s>", (char *)buf3);
        }
        else
            buf1[0] = NUL;
        if (c >= 0x80)
            vim_snprintf(buf2, sizeof(buf2), "  <M-%s>", (char *)transchar(c & 0x7f));
        else
            buf2[0] = NUL;
        vim_snprintf((char *)IObuff, IOSIZE, "<%s>%s%s  %d,  Hex %02x,  Octal %03o",
                                  transchar(c), buf1, buf2, cval, cval, cval);
        c = cc[ci++];
    }

    /* Repeat for combining characters. */
    while (c >= 0x100 || c >= 0x80)
    {
        len = (int)STRLEN(IObuff);
        /* This assumes every multi-byte char is printable... */
        if (len > 0)
            IObuff[len++] = ' ';
        IObuff[len++] = '<';
        if (utf_iscomposing(c))
            IObuff[len++] = ' '; /* draw composing char on top of a space */
        len += utf_char2bytes(c, IObuff + len);
        vim_snprintf((char *)IObuff + len, IOSIZE - len,
                        c < 0x10000 ? "> %d, Hex %04x, Octal %o"
                                    : "> %d, Hex %08x, Octal %o", c, c, c);
        if (ci == MAX_MCO)
            break;
        c = cc[ci++];
    }

    msg(IObuff);
}

/*
 * ":left", ":center" and ":right": align text.
 */
    void
ex_align(eap)
    exarg_T     *eap;
{
    pos_T       save_curpos;
    int         len;
    int         indent = 0;
    int         new_indent;
    int         has_tab;
    int         width;

    if (curwin->w_p_rl)
    {
        /* switch left and right aligning */
        if (eap->cmdidx == CMD_right)
            eap->cmdidx = CMD_left;
        else if (eap->cmdidx == CMD_left)
            eap->cmdidx = CMD_right;
    }

    width = atoi((char *)eap->arg);
    save_curpos = curwin->w_cursor;
    if (eap->cmdidx == CMD_left)    /* width is used for new indent */
    {
        if (width >= 0)
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
            width = curbuf->b_p_tw;
        if (width == 0 && curbuf->b_p_wm > 0)
            width = W_WIDTH(curwin) - curbuf->b_p_wm;
        if (width <= 0)
            width = 80;
    }

    if (u_save((linenr_T)(eap->line1 - 1), (linenr_T)(eap->line2 + 1)) == FAIL)
        return;

    for (curwin->w_cursor.lnum = eap->line1; curwin->w_cursor.lnum <= eap->line2; ++curwin->w_cursor.lnum)
    {
        if (eap->cmdidx == CMD_left)            /* left align */
            new_indent = indent;
        else
        {
            has_tab = FALSE;    /* avoid uninit warnings */
            len = linelen(eap->cmdidx == CMD_right ? &has_tab
                                                   : NULL) - get_indent();

            if (len <= 0)                       /* skip blank lines */
                continue;

            if (eap->cmdidx == CMD_center)
                new_indent = (width - len) / 2;
            else
            {
                new_indent = width - len;       /* right align */

                /*
                 * Make sure that embedded TABs don't make the text go too far to the right.
                 */
                if (has_tab)
                    while (new_indent > 0)
                    {
                        (void)set_indent(new_indent, 0);
                        if (linelen(NULL) <= width)
                        {
                            /*
                             * Now try to move the line as much as possible to
                             * the right.  Stop when it moves too far.
                             */
                            do
                                (void)set_indent(++new_indent, 0);
                            while (linelen(NULL) <= width);
                            --new_indent;
                            break;
                        }
                        --new_indent;
                    }
            }
        }
        if (new_indent < 0)
            new_indent = 0;
        (void)set_indent(new_indent, 0);                /* set indent */
    }
    changed_lines(eap->line1, 0, eap->line2 + 1, 0L);
    curwin->w_cursor = save_curpos;
    beginline(BL_WHITE | BL_FIX);
}

/*
 * Get the length of the current line, excluding trailing white space.
 */
    static int
linelen(has_tab)
    int     *has_tab;
{
    char_u  *line;
    char_u  *first;
    char_u  *last;
    int     save;
    int     len;

    /* find the first non-blank character */
    line = ml_get_curline();
    first = skipwhite(line);

    /* find the character after the last non-blank character */
    for (last = first + STRLEN(first); last > first && vim_iswhite(last[-1]); --last)
        ;
    save = *last;
    *last = NUL;
    len = linetabsize(line);            /* get line length */
    if (has_tab != NULL)                /* check for embedded TAB */
        *has_tab = (vim_strrchr(first, TAB) != NULL);
    *last = save;

    return len;
}

/* Buffer for two lines used during sorting.  They are allocated to
 * contain the longest line being sorted. */
static char_u   *sortbuf1;
static char_u   *sortbuf2;

static int      sort_ic;                /* ignore case */
static int      sort_nr;                /* sort on number */
static int      sort_rx;                /* sort on regex instead of skipping it */

static int      sort_abort;             /* flag to indicate if sorting has been interrupted */

/* Struct to store info to be sorted. */
typedef struct
{
    linenr_T    lnum;                   /* line number */
    long        start_col_nr;           /* starting column number or number */
    long        end_col_nr;             /* ending column number */
} sorti_T;

static int
sort_compare(const void *s1, const void *s2);

    static int
sort_compare(s1, s2)
    const void  *s1;
    const void  *s2;
{
    sorti_T     l1 = *(sorti_T *)s1;
    sorti_T     l2 = *(sorti_T *)s2;
    int         result = 0;

    /* If the user interrupts, there's no way to stop qsort() immediately, but
     * if we return 0 every time, qsort will assume it's done sorting and exit. */
    if (sort_abort)
        return 0;
    fast_breakcheck();
    if (got_int)
        sort_abort = TRUE;

    /* When sorting numbers "start_col_nr" is the number, not the column number. */
    if (sort_nr)
        result = l1.start_col_nr == l2.start_col_nr ? 0
                                 : l1.start_col_nr > l2.start_col_nr ? 1 : -1;
    else
    {
        /* We need to copy one line into "sortbuf1", because there is no
         * guarantee that the first pointer becomes invalid when obtaining the second one. */
        STRNCPY(sortbuf1, ml_get(l1.lnum) + l1.start_col_nr,
                                         l1.end_col_nr - l1.start_col_nr + 1);
        sortbuf1[l1.end_col_nr - l1.start_col_nr] = 0;
        STRNCPY(sortbuf2, ml_get(l2.lnum) + l2.start_col_nr,
                                         l2.end_col_nr - l2.start_col_nr + 1);
        sortbuf2[l2.end_col_nr - l2.start_col_nr] = 0;

        result = sort_ic ? STRICMP(sortbuf1, sortbuf2) : STRCMP(sortbuf1, sortbuf2);
    }

    /* If two lines have the same value, preserve the original line order. */
    if (result == 0)
        return (int)(l1.lnum - l2.lnum);

    return result;
}

/*
 * ":sort".
 */
    void
ex_sort(eap)
    exarg_T     *eap;
{
    regmatch_T  regmatch;
    int         len;
    linenr_T    lnum;
    long        maxlen = 0;
    sorti_T     *nrs;
    size_t      count = (size_t)(eap->line2 - eap->line1 + 1);
    size_t      i;
    char_u      *p;
    char_u      *s;
    char_u      *s2;
    char_u      c;                      /* temporary character storage */
    int         unique = FALSE;
    long        deleted;
    colnr_T     start_col;
    colnr_T     end_col;
    int         sort_oct;               /* sort on octal number */
    int         sort_hex;               /* sort on hex number */

    /* Sorting one line is really quick! */
    if (count <= 1)
        return;

    if (u_save((linenr_T)(eap->line1 - 1), (linenr_T)(eap->line2 + 1)) == FAIL)
        return;
    sortbuf1 = NULL;
    sortbuf2 = NULL;
    regmatch.regprog = NULL;
    nrs = (sorti_T *)lalloc((long_u)(count * sizeof(sorti_T)), TRUE);
    if (nrs == NULL)
        goto sortend;

    sort_abort = sort_ic = sort_rx = sort_nr = sort_oct = sort_hex = 0;

    for (p = eap->arg; *p != NUL; ++p)
    {
        if (vim_iswhite(*p))
            ;
        else if (*p == 'i')
            sort_ic = TRUE;
        else if (*p == 'r')
            sort_rx = TRUE;
        else if (*p == 'n')
            sort_nr = 2;
        else if (*p == 'o')
            sort_oct = 2;
        else if (*p == 'x')
            sort_hex = 2;
        else if (*p == 'u')
            unique = TRUE;
        else if (*p == '"')     /* comment start */
            break;
        else if (check_nextcmd(p) != NULL)
        {
            eap->nextcmd = check_nextcmd(p);
            break;
        }
        else if (!ASCII_ISALPHA(*p) && regmatch.regprog == NULL)
        {
            s = skip_regexp(p + 1, *p, TRUE, NULL);
            if (*s != *p)
            {
                EMSG((char *)e_invalpat);
                goto sortend;
            }
            *s = NUL;
            /* Use last search pattern if sort pattern is empty. */
            if (s == p + 1)
            {
                if (last_search_pat() == NULL)
                {
                    EMSG((char *)e_noprevre);
                    goto sortend;
                }
                regmatch.regprog = vim_regcomp(last_search_pat(), RE_MAGIC);
            }
            else
                regmatch.regprog = vim_regcomp(p + 1, RE_MAGIC);
            if (regmatch.regprog == NULL)
                goto sortend;
            p = s;              /* continue after the regexp */
            regmatch.rm_ic = p_ic;
        }
        else
        {
            EMSG2((char *)e_invarg2, p);
            goto sortend;
        }
    }

    /* Can only have one of 'n', 'o' and 'x'. */
    if (sort_nr + sort_oct + sort_hex > 2)
    {
        EMSG((char *)e_invarg);
        goto sortend;
    }

    /* From here on "sort_nr" is used as a flag for any number sorting. */
    sort_nr += sort_oct + sort_hex;

    /*
     * Make an array with all line numbers.  This avoids having to copy all
     * the lines into allocated memory.
     * When sorting on strings "start_col_nr" is the offset in the line, for
     * numbers sorting it's the number to sort on.  This means the pattern
     * matching and number conversion only has to be done once per line.
     * Also get the longest line length for allocating "sortbuf".
     */
    for (lnum = eap->line1; lnum <= eap->line2; ++lnum)
    {
        s = ml_get(lnum);
        len = (int)STRLEN(s);
        if (maxlen < len)
            maxlen = len;

        start_col = 0;
        end_col = len;
        if (regmatch.regprog != NULL && vim_regexec(&regmatch, s, 0))
        {
            if (sort_rx)
            {
                start_col = (colnr_T)(regmatch.startp[0] - s);
                end_col = (colnr_T)(regmatch.endp[0] - s);
            }
            else
                start_col = (colnr_T)(regmatch.endp[0] - s);
        }
        else if (regmatch.regprog != NULL)
                end_col = 0;

        if (sort_nr)
        {
            /* Make sure vim_str2nr doesn't read any digits past the end
             * of the match, by temporarily terminating the string there */
            s2 = s + end_col;
            c = *s2;
            *s2 = NUL;
            /* Sorting on number: Store the number itself. */
            p = s + start_col;
            if (sort_hex)
                s = skiptohex(p);
            else
                s = skiptodigit(p);
            if (s > p && s[-1] == '-')
                --s;  /* include preceding negative sign */
            if (*s == NUL)
                /* empty line should sort before any number */
                nrs[lnum - eap->line1].start_col_nr = -MAXLNUM;
            else
                vim_str2nr(s, NULL, NULL, sort_oct, sort_hex,
                                  &nrs[lnum - eap->line1].start_col_nr, NULL);
            *s2 = c;
        }
        else
        {
            /* Store the column to sort at. */
            nrs[lnum - eap->line1].start_col_nr = start_col;
            nrs[lnum - eap->line1].end_col_nr = end_col;
        }

        nrs[lnum - eap->line1].lnum = lnum;

        if (regmatch.regprog != NULL)
            fast_breakcheck();
        if (got_int)
            goto sortend;
    }

    /* Allocate a buffer that can hold the longest line. */
    sortbuf1 = alloc((unsigned)maxlen + 1);
    if (sortbuf1 == NULL)
        goto sortend;
    sortbuf2 = alloc((unsigned)maxlen + 1);
    if (sortbuf2 == NULL)
        goto sortend;

    /* Sort the array of line numbers.  Note: can't be interrupted! */
    qsort((void *)nrs, count, sizeof(sorti_T), sort_compare);

    if (sort_abort)
        goto sortend;

    /* Insert the lines in the sorted order below the last one. */
    lnum = eap->line2;
    for (i = 0; i < count; ++i)
    {
        s = ml_get(nrs[eap->forceit ? count - i - 1 : i].lnum);
        if (!unique || i == 0 || (sort_ic ? STRICMP(s, sortbuf1) : STRCMP(s, sortbuf1)) != 0)
        {
            if (ml_append(lnum++, s, (colnr_T)0, FALSE) == FAIL)
                break;
            if (unique)
                STRCPY(sortbuf1, s);
        }
        fast_breakcheck();
        if (got_int)
            goto sortend;
    }

    /* delete the original lines if appending worked */
    if (i == count)
        for (i = 0; i < count; ++i)
            ml_delete(eap->line1, FALSE);
    else
        count = 0;

    /* Adjust marks for deleted (or added) lines and prepare for displaying. */
    deleted = (long)(count - (lnum - eap->line2));
    if (deleted > 0)
        mark_adjust(eap->line2 - deleted, eap->line2, (long)MAXLNUM, -deleted);
    else if (deleted < 0)
        mark_adjust(eap->line2, MAXLNUM, -deleted, 0L);
    changed_lines(eap->line1, 0, eap->line2 + 1, -deleted);

    curwin->w_cursor.lnum = eap->line1;
    beginline(BL_WHITE | BL_FIX);

sortend:
    vim_free(nrs);
    vim_free(sortbuf1);
    vim_free(sortbuf2);
    vim_regfree(regmatch.regprog);
    if (got_int)
        EMSG((char *)e_interr);
}

/*
 * ":retab".
 */
    void
ex_retab(eap)
    exarg_T     *eap;
{
    linenr_T    lnum;
    int         got_tab = FALSE;
    long        num_spaces = 0;
    long        num_tabs;
    long        len;
    long        col;
    long        vcol;
    long        start_col = 0;          /* For start of white-space string */
    long        start_vcol = 0;         /* For start of white-space string */
    int         temp;
    long        old_len;
    char_u      *ptr;
    char_u      *new_line = (char_u *)1;    /* init to non-NULL */
    int         did_undo;               /* called u_save for current line */
    int         new_ts;
    int         save_list;
    linenr_T    first_line = 0;         /* first changed line */
    linenr_T    last_line = 0;          /* last changed line */

    save_list = curwin->w_p_list;
    curwin->w_p_list = 0;           /* don't want list mode here */

    new_ts = getdigits(&(eap->arg));
    if (new_ts < 0)
    {
        EMSG((char *)e_positive);
        return;
    }
    if (new_ts == 0)
        new_ts = curbuf->b_p_ts;
    for (lnum = eap->line1; !got_int && lnum <= eap->line2; ++lnum)
    {
        ptr = ml_get(lnum);
        col = 0;
        vcol = 0;
        did_undo = FALSE;
        for (;;)
        {
            if (vim_iswhite(ptr[col]))
            {
                if (!got_tab && num_spaces == 0)
                {
                    /* First consecutive white-space */
                    start_vcol = vcol;
                    start_col = col;
                }
                if (ptr[col] == ' ')
                    num_spaces++;
                else
                    got_tab = TRUE;
            }
            else
            {
                if (got_tab || (eap->forceit && num_spaces > 1))
                {
                    /* Retabulate this string of white-space */

                    /* len is virtual length of white string */
                    len = num_spaces = vcol - start_vcol;
                    num_tabs = 0;
                    if (!curbuf->b_p_et)
                    {
                        temp = new_ts - (start_vcol % new_ts);
                        if (num_spaces >= temp)
                        {
                            num_spaces -= temp;
                            num_tabs++;
                        }
                        num_tabs += num_spaces / new_ts;
                        num_spaces -= (num_spaces / new_ts) * new_ts;
                    }
                    if (curbuf->b_p_et || got_tab || (num_spaces + num_tabs < len))
                    {
                        if (did_undo == FALSE)
                        {
                            did_undo = TRUE;
                            if (u_save((linenr_T)(lnum - 1), (linenr_T)(lnum + 1)) == FAIL)
                            {
                                new_line = NULL;        /* flag out-of-memory */
                                break;
                            }
                        }

                        /* len is actual number of white characters used */
                        len = num_spaces + num_tabs;
                        old_len = (long)STRLEN(ptr);
                        new_line = lalloc(old_len - col + start_col + len + 1, TRUE);
                        if (new_line == NULL)
                            break;
                        if (start_col > 0)
                            mch_memmove(new_line, ptr, (size_t)start_col);
                        mch_memmove(new_line + start_col + len, ptr + col, (size_t)(old_len - col + 1));
                        ptr = new_line + start_col;
                        for (col = 0; col < len; col++)
                            ptr[col] = (col < num_tabs) ? '\t' : ' ';
                        ml_replace(lnum, new_line, FALSE);
                        if (first_line == 0)
                            first_line = lnum;
                        last_line = lnum;
                        ptr = new_line;
                        col = start_col + len;
                    }
                }
                got_tab = FALSE;
                num_spaces = 0;
            }
            if (ptr[col] == NUL)
                break;
            vcol += chartabsize(ptr + col, (colnr_T)vcol);
            col += utfc_ptr2len(ptr + col);
        }
        if (new_line == NULL)               /* out of memory */
            break;
        line_breakcheck();
    }
    if (got_int)
        EMSG((char *)e_interr);

    if (curbuf->b_p_ts != new_ts)
        redraw_curbuf_later(NOT_VALID);
    if (first_line != 0)
        changed_lines(first_line, 0, last_line + 1, 0L);

    curwin->w_p_list = save_list;       /* restore 'list' */

    curbuf->b_p_ts = new_ts;
    coladvance(curwin->w_curswant);

    u_clearline();
}

/*
 * :move command - move lines line1-line2 to line dest
 *
 * return FAIL for failure, OK otherwise
 */
    int
do_move(line1, line2, dest)
    linenr_T    line1;
    linenr_T    line2;
    linenr_T    dest;
{
    char_u      *str;
    linenr_T    l;
    linenr_T    extra;      /* Num lines added before line1 */
    linenr_T    num_lines;  /* Num lines moved */
    linenr_T    last_line;  /* Last line in file after adding new text */

    if (dest >= line1 && dest < line2)
    {
        EMSG("E134: Move lines into themselves");
        return FAIL;
    }

    num_lines = line2 - line1 + 1;

    /*
     * First we copy the old text to its new location -- webb
     * Also copy the flag that ":global" command uses.
     */
    if (u_save(dest, dest + 1) == FAIL)
        return FAIL;
    for (extra = 0, l = line1; l <= line2; l++)
    {
        str = vim_strsave(ml_get(l + extra));
        if (str != NULL)
        {
            ml_append(dest + l - line1, str, (colnr_T)0, FALSE);
            vim_free(str);
            if (dest < line1)
                extra++;
        }
    }

    /*
     * Now we must be careful adjusting our marks so that we don't overlap our
     * mark_adjust() calls.
     *
     * We adjust the marks within the old text so that they refer to the
     * last lines of the file (temporarily), because we know no other marks
     * will be set there since these line numbers did not exist until we added our new lines.
     *
     * Then we adjust the marks on lines between the old and new text positions
     * (either forwards or backwards).
     *
     * And Finally we adjust the marks we put at the end of the file back to
     * their final destination at the new text position -- webb
     */
    last_line = curbuf->b_ml.ml_line_count;
    mark_adjust(line1, line2, last_line - line2, 0L);
    changed_lines(last_line - num_lines + 1, 0, last_line + 1, num_lines);
    if (dest >= line2)
    {
        mark_adjust(line2 + 1, dest, -num_lines, 0L);
        curbuf->b_op_start.lnum = dest - num_lines + 1;
        curbuf->b_op_end.lnum = dest;
    }
    else
    {
        mark_adjust(dest + 1, line1 - 1, num_lines, 0L);
        curbuf->b_op_start.lnum = dest + 1;
        curbuf->b_op_end.lnum = dest + num_lines;
    }
    curbuf->b_op_start.col = curbuf->b_op_end.col = 0;
    mark_adjust(last_line - num_lines + 1, last_line, -(last_line - dest - extra), 0L);
    changed_lines(last_line - num_lines + 1, 0, last_line + 1, -extra);

    /*
     * Now we delete the original text -- webb
     */
    if (u_save(line1 + extra - 1, line2 + extra + 1) == FAIL)
        return FAIL;

    for (l = line1; l <= line2; l++)
        ml_delete(line1 + extra, TRUE);

    if (!global_busy && num_lines > p_report)
    {
        if (num_lines == 1)
            MSG("1 line moved");
        else
            smsg((char_u *)"%ld lines moved", num_lines);
    }

    /*
     * Leave the cursor on the last of the moved lines.
     */
    if (dest >= line1)
        curwin->w_cursor.lnum = dest;
    else
        curwin->w_cursor.lnum = dest + (line2 - line1) + 1;

    if (line1 < dest)
    {
        dest += num_lines + 1;
        last_line = curbuf->b_ml.ml_line_count;
        if (dest > last_line + 1)
            dest = last_line + 1;
        changed_lines(line1, 0, dest, 0L);
    }
    else
        changed_lines(dest + 1, 0, line1 + num_lines, 0L);

    return OK;
}

/*
 * ":copy"
 */
    void
ex_copy(line1, line2, n)
    linenr_T    line1;
    linenr_T    line2;
    linenr_T    n;
{
    linenr_T    count;
    char_u      *p;

    count = line2 - line1 + 1;
    curbuf->b_op_start.lnum = n + 1;
    curbuf->b_op_end.lnum = n + count;
    curbuf->b_op_start.col = curbuf->b_op_end.col = 0;

    /*
     * there are three situations:
     * 1. destination is above line1
     * 2. destination is between line1 and line2
     * 3. destination is below line2
     *
     * n = destination (when starting)
     * curwin->w_cursor.lnum = destination (while copying)
     * line1 = start of source (while copying)
     * line2 = end of source (while copying)
     */
    if (u_save(n, n + 1) == FAIL)
        return;

    curwin->w_cursor.lnum = n;
    while (line1 <= line2)
    {
        /* need to use vim_strsave() because the line will be unlocked within ml_append() */
        p = vim_strsave(ml_get(line1));
        if (p != NULL)
        {
            ml_append(curwin->w_cursor.lnum, p, (colnr_T)0, FALSE);
            vim_free(p);
        }
        /* situation 2: skip already copied lines */
        if (line1 == n)
            line1 = curwin->w_cursor.lnum;
        ++line1;
        if (curwin->w_cursor.lnum < line1)
            ++line1;
        if (curwin->w_cursor.lnum < line2)
            ++line2;
        ++curwin->w_cursor.lnum;
    }

    appended_lines_mark(n, count);

    msgmore((long)count);
}

static char_u   *prevcmd = NULL;        /* the previous command */

/*
 * Handle the ":!cmd" command.  Also for ":r !cmd" and ":w !cmd"
 * Bangs in the argument are replaced with the previously entered command.
 * Remember the argument.
 */
    void
do_bang(addr_count, eap, forceit, do_in, do_out)
    int         addr_count;
    exarg_T     *eap;
    int         forceit;
    int         do_in, do_out;
{
    char_u              *arg = eap->arg;        /* command */
    linenr_T            line1 = eap->line1;     /* start of range */
    linenr_T            line2 = eap->line2;     /* end of range */
    char_u              *newcmd = NULL;         /* the new command */
    int                 free_newcmd = FALSE;    /* need to free() newcmd */
    int                 ins_prevcmd;
    char_u              *t;
    char_u              *p;
    char_u              *trailarg;
    int                 len;
    int                 scroll_save = msg_scroll;

    /*
     * Disallow shell commands for "rvim".
     * Disallow shell commands from .exrc and .vimrc in current directory for security reasons.
     */
    if (check_restricted() || check_secure())
        return;

    if (addr_count == 0)                /* :! */
    {
        msg_scroll = FALSE;         /* don't scroll here */
        autowrite_all();
        msg_scroll = scroll_save;
    }

    /*
     * Try to find an embedded bang, like in :!<cmd> ! [args]
     * (:!! is indicated by the 'forceit' variable)
     */
    ins_prevcmd = forceit;
    trailarg = arg;
    do
    {
        len = (int)STRLEN(trailarg) + 1;
        if (newcmd != NULL)
            len += (int)STRLEN(newcmd);
        if (ins_prevcmd)
        {
            if (prevcmd == NULL)
            {
                EMSG((char *)e_noprev);
                vim_free(newcmd);
                return;
            }
            len += (int)STRLEN(prevcmd);
        }
        if ((t = alloc((unsigned)len)) == NULL)
        {
            vim_free(newcmd);
            return;
        }
        *t = NUL;
        if (newcmd != NULL)
            STRCAT(t, newcmd);
        if (ins_prevcmd)
            STRCAT(t, prevcmd);
        p = t + STRLEN(t);
        STRCAT(t, trailarg);
        vim_free(newcmd);
        newcmd = t;

        /*
         * Scan the rest of the argument for '!', which is replaced by the
         * previous command.  "\!" is replaced by "!" (this is vi compatible).
         */
        trailarg = NULL;
        while (*p)
        {
            if (*p == '!')
            {
                if (p > newcmd && p[-1] == '\\')
                    STRMOVE(p - 1, p);
                else
                {
                    trailarg = p;
                    *trailarg++ = NUL;
                    ins_prevcmd = TRUE;
                    break;
                }
            }
            ++p;
        }
    } while (trailarg != NULL);

    vim_free(prevcmd);
    prevcmd = newcmd;

    if (bangredo)           /* put cmd in redo buffer for ! command */
    {
        /* If % or # appears in the command, it must have been escaped.
         * Reescape them, so that redoing them does not substitute them by the buffername. */
        char_u *cmd = vim_strsave_escaped(prevcmd, (char_u *)"%#");

        if (cmd != NULL)
        {
            AppendToRedobuffLit(cmd, -1);
            vim_free(cmd);
        }
        else
            AppendToRedobuffLit(prevcmd, -1);
        AppendToRedobuff((char_u *)"\n");
        bangredo = FALSE;
    }
    /*
     * Add quotes around the command, for shells that need them.
     */
    if (*p_shq != NUL)
    {
        newcmd = alloc((unsigned)(STRLEN(prevcmd) + 2 * STRLEN(p_shq) + 1));
        if (newcmd == NULL)
            return;
        STRCPY(newcmd, p_shq);
        STRCAT(newcmd, prevcmd);
        STRCAT(newcmd, p_shq);
        free_newcmd = TRUE;
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
        /* Careful: This may recursively call do_bang() again! (because of autocommands) */
        do_filter(line1, line2, eap, newcmd, do_in, do_out);
        apply_autocmds(EVENT_SHELLFILTERPOST, NULL, NULL, FALSE, curbuf);
    }
    if (free_newcmd)
        vim_free(newcmd);
}

/*
 * do_filter: filter lines through a command given by the user
 *
 * We mostly use temp files and the call_shell() routine here. This would
 * normally be done using pipes on a UNIX machine, but this is more portable
 * to non-unix machines. The call_shell() routine needs to be able
 * to deal with redirection somehow, and should handle things like looking
 * at the PATH env. variable, and adding reasonable extensions to the
 * command name given by the user. All reasonable versions of call_shell() do this.
 * Alternatively, if on Unix and redirecting input or output, but not both,
 * and the 'shelltemp' option isn't set, use pipes.
 * We use input redirection if do_in is TRUE.
 * We use output redirection if do_out is TRUE.
 */
    static void
do_filter(line1, line2, eap, cmd, do_in, do_out)
    linenr_T    line1, line2;
    exarg_T     *eap;           /* for forced 'ff' and 'fenc' */
    char_u      *cmd;
    int         do_in, do_out;
{
    char_u      *itmp = NULL;
    char_u      *otmp = NULL;
    linenr_T    linecount;
    linenr_T    read_linecount;
    pos_T       cursor_save;
    char_u      *cmd_buf;
    buf_T       *old_curbuf = curbuf;
    int         shell_flags = 0;

    if (*cmd == NUL)        /* no filter command */
        return;

    cursor_save = curwin->w_cursor;
    linecount = line2 - line1 + 1;
    curwin->w_cursor.lnum = line1;
    curwin->w_cursor.col = 0;
    changed_line_abv_curs();
    invalidate_botline();

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

    if (do_out)
        shell_flags |= SHELL_DOOUT;

    if (!do_in && do_out && !p_stmp)
    {
        /* Use a pipe to fetch stdout of the command, do not use a temp file. */
        shell_flags |= SHELL_READ;
        curwin->w_cursor.lnum = line2;
    }
    else if (do_in && !do_out && !p_stmp)
    {
        /* Use a pipe to write stdin of the command, do not use a temp file. */
        shell_flags |= SHELL_WRITE;
        curbuf->b_op_start.lnum = line1;
        curbuf->b_op_end.lnum = line2;
    }
    else if (do_in && do_out && !p_stmp)
    {
        /* Use a pipe to write stdin and fetch stdout of the command, do not
         * use a temp file. */
        shell_flags |= SHELL_READ|SHELL_WRITE;
        curbuf->b_op_start.lnum = line1;
        curbuf->b_op_end.lnum = line2;
        curwin->w_cursor.lnum = line2;
    }
    else if ((do_in && (itmp = vim_tempname('i', FALSE)) == NULL)
          || (do_out && (otmp = vim_tempname('o', FALSE)) == NULL))
    {
        EMSG((char *)e_notmp);
        goto filterend;
    }

/*
 * The writing and reading of temp files will not be shown.
 * Vi also doesn't do this and the messages are not very informative.
 */
    ++no_wait_return;           /* don't call wait_return() while busy */
    if (itmp != NULL && buf_write(curbuf, itmp, NULL, line1, line2, eap,
                                           FALSE, FALSE, FALSE, TRUE) == FAIL)
    {
        msg_putchar('\n');              /* keep message from buf_write() */
        --no_wait_return;
        if (!aborting())
            (void)EMSG2((char *)e_notcreate, itmp);  /* will call wait_return */
        goto filterend;
    }
    if (curbuf != old_curbuf)
        goto filterend;

    if (!do_out)
        msg_putchar('\n');

    /* Create the shell command in allocated memory. */
    cmd_buf = make_filter_cmd(cmd, itmp, otmp);
    if (cmd_buf == NULL)
        goto filterend;

    windgoto((int)Rows - 1, 0);
    cursor_on();

    /*
     * When not redirecting the output the command can write anything to the
     * screen. If 'shellredir' is equal to ">", screen may be messed up by
     * stderr output of external command. Clear the screen later.
     * If do_in is FALSE, this could be something like ":r !cat", which may
     * also mess up the screen, clear it later.
     */
    if (!do_out || STRCMP(p_srr, ">") == 0 || !do_in)
        redraw_later_clear();

    if (do_out)
    {
        if (u_save((linenr_T)(line2), (linenr_T)(line2 + 1)) == FAIL)
        {
            vim_free(cmd_buf);
            goto error;
        }
        redraw_curbuf_later(VALID);
    }
    read_linecount = curbuf->b_ml.ml_line_count;

    /*
     * When call_shell() fails wait_return() is called to give the user a
     * chance to read the error messages. Otherwise errors are ignored, so you
     * can see the error messages from the command that appear on stdout; use
     * 'u' to fix the text
     * Switch to cooked mode when not redirecting stdin, avoids that something
     * like ":r !cat" hangs.
     * Pass on the SHELL_DOOUT flag when the output is being redirected.
     */
    if (call_shell(cmd_buf, SHELL_FILTER | SHELL_COOKED | shell_flags))
    {
        redraw_later_clear();
        wait_return(FALSE);
    }
    vim_free(cmd_buf);

    did_check_timestamps = FALSE;
    need_check_timestamps = TRUE;

    /* When interrupting the shell command, it may still have produced some
     * useful output.  Reset got_int here, so that readfile() won't cancel reading. */
    ui_breakcheck();
    got_int = FALSE;

    if (do_out)
    {
        if (otmp != NULL)
        {
            if (readfile(otmp, NULL, line2, (linenr_T)0, (linenr_T)MAXLNUM, eap, READ_FILTER) == FAIL)
            {
                if (!aborting())
                {
                    msg_putchar('\n');
                    EMSG2((char *)e_notread, otmp);
                }
                goto error;
            }
            if (curbuf != old_curbuf)
                goto filterend;
        }

        read_linecount = curbuf->b_ml.ml_line_count - read_linecount;

        if (shell_flags & SHELL_READ)
        {
            curbuf->b_op_start.lnum = line2 + 1;
            curbuf->b_op_end.lnum = curwin->w_cursor.lnum;
            appended_lines_mark(line2, read_linecount);
        }

        if (do_in)
        {
            if (cmdmod.keepmarks || vim_strchr(p_cpo, CPO_REMMARK) == NULL)
            {
                if (read_linecount >= linecount)
                    /* move all marks from old lines to new lines */
                    mark_adjust(line1, line2, linecount, 0L);
                else
                {
                    /* move marks from old lines to new lines, delete marks
                     * that are in deleted lines */
                    mark_adjust(line1, line1 + read_linecount - 1, linecount, 0L);
                    mark_adjust(line1 + read_linecount, line2, MAXLNUM, 0L);
                }
            }

            /*
             * Put cursor on first filtered line for ":range!cmd".
             * Adjust '[ and '] (set by buf_write()).
             */
            curwin->w_cursor.lnum = line1;
            del_lines(linecount, TRUE);
            curbuf->b_op_start.lnum -= linecount;       /* adjust '[ */
            curbuf->b_op_end.lnum -= linecount;         /* adjust '] */
            write_lnum_adjust(-linecount);              /* adjust last line for next write */
        }
        else
        {
            /*
             * Put cursor on last new line for ":r !cmd".
             */
            linecount = curbuf->b_op_end.lnum - curbuf->b_op_start.lnum + 1;
            curwin->w_cursor.lnum = curbuf->b_op_end.lnum;
        }

        beginline(BL_WHITE | BL_FIX);       /* cursor on first non-blank */
        --no_wait_return;

        if (linecount > p_report)
        {
            if (do_in)
            {
                vim_snprintf((char *)msg_buf, sizeof(msg_buf),
                                    "%ld lines filtered", (long)linecount);
                if (msg(msg_buf) && !msg_scroll)
                    /* save message to display it after redraw */
                    set_keep_msg(msg_buf, 0);
            }
            else
                msgmore((long)linecount);
        }
    }
    else
    {
error:
        /* put cursor back in same position for ":w !cmd" */
        curwin->w_cursor = cursor_save;
        --no_wait_return;
        wait_return(FALSE);
    }

filterend:

    if (curbuf != old_curbuf)
    {
        --no_wait_return;
        EMSG("E135: *Filter* Autocommands must not change current buffer");
    }
    if (itmp != NULL)
        unlink((char *)itmp);
    if (otmp != NULL)
        unlink((char *)otmp);
    vim_free(itmp);
    vim_free(otmp);
}

/*
 * Call a shell to execute a command.
 * When "cmd" is NULL start an interactive shell.
 */
    void
do_shell(cmd, flags)
    char_u      *cmd;
    int         flags;  /* may be SHELL_DOOUT when output is redirected */
{
    buf_T       *buf;
    int         save_nwr;

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
     * For autocommands we want to get the output on the current screen, to
     * avoid having to type return below.
     */
    msg_putchar('\r');                  /* put cursor at start of line */
    if (!autocmd_busy)
    {
        stoptermcap();
    }
    msg_putchar('\n');              /* may shift screen one line up */

    /* warning message before calling the shell */
    if (p_warn && !autocmd_busy && msg_silent == 0)
        for (buf = firstbuf; buf; buf = buf->b_next)
            if (bufIsChanged(buf))
            {
                MSG_PUTS("[No write since last change]\n");
                break;
            }

    /* This windgoto is required for when the '\n' resulted in a "delete line
     * 1" command to the terminal. */
    if (!swapping_screen())
        windgoto(msg_row, msg_col);
    cursor_on();
    (void)call_shell(cmd, SHELL_COOKED | flags);
    did_check_timestamps = FALSE;
    need_check_timestamps = TRUE;

    /*
     * put the message cursor at the end of the screen, avoids wait_return()
     * to overwrite the text that the external command showed
     */
    if (!swapping_screen())
    {
        msg_row = Rows - 1;
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
        if (cmd == NULL)
        {
            if (msg_silent == 0)
                redraw_later_clear();
            need_wait_return = FALSE;
        }
        else
        {
            /*
             * If we switch screens when starttermcap() is called, we really
             * want to wait for "hit return to continue".
             */
            save_nwr = no_wait_return;
            if (swapping_screen())
                no_wait_return = FALSE;
            wait_return(msg_silent == 0);
            no_wait_return = save_nwr;
        }

        starttermcap();     /* start termcap if not done by wait_return() */
    }

    /* display any error messages now */
    display_errors();

    apply_autocmds(EVENT_SHELLCMDPOST, NULL, NULL, FALSE, curbuf);
}

/*
 * Create a shell command from a command string, input redirection file and
 * output redirection file.
 * Returns an allocated string with the shell command, or NULL for failure.
 */
    char_u *
make_filter_cmd(cmd, itmp, otmp)
    char_u      *cmd;           /* command */
    char_u      *itmp;          /* NULL or name of input file */
    char_u      *otmp;          /* NULL or name of output file */
{
    char_u      *buf;
    long_u      len;

    int         is_fish_shell;
    char_u      *shell_name = get_isolated_shell_name();

    /* Account for fish's different syntax for subshells */
    is_fish_shell = (fnamecmp(shell_name, "fish") == 0);
    vim_free(shell_name);
    if (is_fish_shell)
        len = (long_u)STRLEN(cmd) + 13;         /* "begin; " + "; end" + NUL */
    else
        len = (long_u)STRLEN(cmd) + 3;                  /* "()" + NUL */
    if (itmp != NULL)
        len += (long_u)STRLEN(itmp) + 9;                /* " { < " + " } " */
    if (otmp != NULL)
        len += (long_u)STRLEN(otmp) + (long_u)STRLEN(p_srr) + 2; /* "  " */
    buf = lalloc(len, TRUE);
    if (buf == NULL)
        return NULL;

    /*
     * Put braces around the command (for concatenated commands) when
     * redirecting input and/or output.
     */
    if (itmp != NULL || otmp != NULL)
    {
        if (is_fish_shell)
            vim_snprintf((char *)buf, len, "begin; %s; end", (char *)cmd);
        else
            vim_snprintf((char *)buf, len, "(%s)", (char *)cmd);
    }
    else
        STRCPY(buf, cmd);
    if (itmp != NULL)
    {
        STRCAT(buf, " < ");
        STRCAT(buf, itmp);
    }
    if (otmp != NULL)
        append_redir(buf, (int)len, p_srr, otmp);

    return buf;
}

/*
 * Append output redirection for file "fname" to the end of string buffer
 * "buf[buflen]"
 * Works with the 'shellredir' and 'shellpipe' options.
 * The caller should make sure that there is enough room:
 *      STRLEN(opt) + STRLEN(fname) + 3
 */
    void
append_redir(buf, buflen, opt, fname)
    char_u      *buf;
    int         buflen;
    char_u      *opt;
    char_u      *fname;
{
    char_u      *p;
    char_u      *end;

    end = buf + STRLEN(buf);
    /* find "%s" */
    for (p = opt; (p = vim_strchr(p, '%')) != NULL; ++p)
    {
        if (p[1] == 's') /* found %s */
            break;
        if (p[1] == '%') /* skip %% */
            ++p;
    }
    if (p != NULL)
    {
        *end = ' '; /* not really needed? Not with sh, ksh or bash */
        vim_snprintf((char *)end + 1, (size_t)(buflen - (end + 1 - buf)), (char *)opt, (char *)fname);
    }
    else
        vim_snprintf((char *)end, (size_t)(buflen - (end - buf)), " %s%s", (char *)opt, (char *)fname);
}

/*
 * Implementation of ":fixdel", also used by get_stty().
 *  <BS>    resulting <Del>
 *   ^?         ^H
 * not ^?       ^?
 */
    void
do_fixdel(eap)
    exarg_T     *eap UNUSED;
{
    char_u  *p;

    p = find_termcode((char_u *)"kb");
    add_termcode((char_u *)"kD", p != NULL && *p == DEL ? (char_u *)CTRL_H_STR : DEL_STR, FALSE);
}

    void
print_line_no_prefix(lnum, use_number, list)
    linenr_T    lnum;
    int         use_number;
    int         list;
{
    char_u      numbuf[30];

    if (curwin->w_p_nu || use_number)
    {
        vim_snprintf((char *)numbuf, sizeof(numbuf), "%*ld ", number_width(curwin), (long)lnum);
        msg_puts_attr(numbuf, hl_attr(HLF_N));  /* Highlight line nrs */
    }
    msg_prt_line(ml_get(lnum), list);
}

/*
 * Print a text line.  Also in silent mode ("ex -s").
 */
    void
print_line(lnum, use_number, list)
    linenr_T    lnum;
    int         use_number;
    int         list;
{
    int         save_silent = silent_mode;

    msg_start();
    silent_mode = FALSE;
    info_message = TRUE;        /* use mch_msg(), not mch_errmsg() */
    print_line_no_prefix(lnum, use_number, list);
    if (save_silent)
    {
        msg_putchar('\n');
        cursor_on();            /* msg_start() switches it off */
        out_flush();
        silent_mode = save_silent;
    }
    info_message = FALSE;
}

    int
rename_buffer(new_fname)
    char_u      *new_fname;
{
    char_u      *fname, *sfname, *xfname;
    buf_T       *buf;

    buf = curbuf;
    apply_autocmds(EVENT_BUFFILEPRE, NULL, NULL, FALSE, curbuf);
    /* buffer changed, don't change name now */
    if (buf != curbuf)
        return FAIL;
    if (aborting())         /* autocmds may abort script processing */
        return FAIL;
    /*
     * The name of the current buffer will be changed.
     * A new (unlisted) buffer entry needs to be made to hold the old file
     * name, which will become the alternate file name.
     * But don't set the alternate file name if the buffer didn't have a name.
     */
    fname = curbuf->b_ffname;
    sfname = curbuf->b_sfname;
    xfname = curbuf->b_fname;
    curbuf->b_ffname = NULL;
    curbuf->b_sfname = NULL;
    if (setfname(curbuf, new_fname, NULL, TRUE) == FAIL)
    {
        curbuf->b_ffname = fname;
        curbuf->b_sfname = sfname;
        return FAIL;
    }
    curbuf->b_flags |= BF_NOTEDITED;
    if (xfname != NULL && *xfname != NUL)
    {
        buf = buflist_new(fname, xfname, curwin->w_cursor.lnum, 0);
        if (buf != NULL && !cmdmod.keepalt)
            curwin->w_alt_fnum = buf->b_fnum;
    }
    vim_free(fname);
    vim_free(sfname);
    apply_autocmds(EVENT_BUFFILEPOST, NULL, NULL, FALSE, curbuf);

    return OK;
}

/*
 * ":file[!] [fname]".
 */
    void
ex_file(eap)
    exarg_T     *eap;
{
    /* ":0file" removes the file name.  Check for illegal uses ":3file",
     * "0file name", etc. */
    if (eap->addr_count > 0 && (*eap->arg != NUL || eap->line2 > 0 || eap->addr_count > 1))
    {
        EMSG((char *)e_invarg);
        return;
    }

    if (*eap->arg != NUL || eap->addr_count == 1)
    {
        if (rename_buffer(eap->arg) == FAIL)
            return;
    }
    /* print full file name if :cd used */
    fileinfo(FALSE, eap->forceit);
}

/*
 * ":update".
 */
    void
ex_update(eap)
    exarg_T     *eap;
{
    if (curbufIsChanged())
        (void)do_write(eap);
}

/*
 * ":write" and ":saveas".
 */
    void
ex_write(eap)
    exarg_T     *eap;
{
    if (eap->usefilter)         /* input lines to shell command */
        do_bang(1, eap, FALSE, TRUE, FALSE);
    else
        (void)do_write(eap);
}

/*
 * write current buffer to file 'eap->arg'
 * if 'eap->append' is TRUE, append to the file
 *
 * if *eap->arg == NUL write to current file
 *
 * return FAIL for failure, OK otherwise
 */
    int
do_write(eap)
    exarg_T     *eap;
{
    int         other;
    char_u      *fname = NULL;          /* init to shut up gcc */
    char_u      *ffname;
    int         retval = FAIL;
    char_u      *free_fname = NULL;
    buf_T       *alt_buf = NULL;

    if (not_writing())          /* check 'write' option */
        return FAIL;

    ffname = eap->arg;
    if (*ffname == NUL)
    {
        if (eap->cmdidx == CMD_saveas)
        {
            EMSG((char *)e_argreq);
            goto theend;
        }
        other = FALSE;
    }
    else
    {
        fname = ffname;
        free_fname = fix_fname(ffname);
        /*
         * When out-of-memory, keep unexpanded file name, because we MUST be
         * able to write the file in this situation.
         */
        if (free_fname != NULL)
            ffname = free_fname;
        other = otherfile(ffname);
    }

    /*
     * If we have a new file, put its name in the list of alternate file names.
     */
    if (other)
    {
        if (vim_strchr(p_cpo, CPO_ALTWRITE) != NULL || eap->cmdidx == CMD_saveas)
            alt_buf = setaltfname(ffname, fname, (linenr_T)1);
        else
            alt_buf = buflist_findname(ffname);
        if (alt_buf != NULL && alt_buf->b_ml.ml_mfp != NULL)
        {
            /* Overwriting a file that is loaded in another buffer is not a good idea. */
            EMSG((char *)e_bufloaded);
            goto theend;
        }
    }

    /*
     * Writing to the current file is not allowed in readonly mode
     * and a file name is required.
     * "nofile" and "nowrite" buffers cannot be written implicitly either.
     */
    if (!other && (check_fname() == FAIL || check_readonly(&eap->forceit, curbuf)))
        goto theend;

    if (!other)
    {
        ffname = curbuf->b_ffname;
        fname = curbuf->b_fname;
        /*
         * Not writing the whole file is only allowed with '!'.
         */
        if (       (eap->line1 != 1 || eap->line2 != curbuf->b_ml.ml_line_count)
                && !eap->forceit
                && !eap->append
                && !p_wa)
        {
            if (p_confirm || cmdmod.confirm)
            {
                if (vim_dialog_yesno(VIM_QUESTION, NULL,
                               (char_u *)"Write partial file?", 2) != VIM_YES)
                    goto theend;
                eap->forceit = TRUE;
            }
            else
            {
                EMSG("E140: Use ! to write partial buffer");
                goto theend;
            }
        }
    }

    if (check_overwrite(eap, curbuf, fname, ffname, other) == OK)
    {
        if (eap->cmdidx == CMD_saveas && alt_buf != NULL)
        {
            buf_T       *was_curbuf = curbuf;

            apply_autocmds(EVENT_BUFFILEPRE, NULL, NULL, FALSE, curbuf);
            apply_autocmds(EVENT_BUFFILEPRE, NULL, NULL, FALSE, alt_buf);
            if (curbuf != was_curbuf || aborting())
            {
                /* buffer changed, don't change name now */
                retval = FAIL;
                goto theend;
            }
            /* Exchange the file names for the current and the alternate
             * buffer.  This makes it look like we are now editing the buffer
             * under the new name.  Must be done before buf_write(), because
             * if there is no file name and 'cpo' contains 'F', it will set
             * the file name. */
            fname = alt_buf->b_fname;
            alt_buf->b_fname = curbuf->b_fname;
            curbuf->b_fname = fname;
            fname = alt_buf->b_ffname;
            alt_buf->b_ffname = curbuf->b_ffname;
            curbuf->b_ffname = fname;
            fname = alt_buf->b_sfname;
            alt_buf->b_sfname = curbuf->b_sfname;
            curbuf->b_sfname = fname;
            buf_name_changed(curbuf);
            apply_autocmds(EVENT_BUFFILEPOST, NULL, NULL, FALSE, curbuf);
            apply_autocmds(EVENT_BUFFILEPOST, NULL, NULL, FALSE, alt_buf);
            if (!alt_buf->b_p_bl)
            {
                alt_buf->b_p_bl = TRUE;
                apply_autocmds(EVENT_BUFADD, NULL, NULL, FALSE, alt_buf);
            }
            if (curbuf != was_curbuf || aborting())
            {
                /* buffer changed, don't write the file */
                retval = FAIL;
                goto theend;
            }

            /* If 'filetype' was empty try detecting it now. */
            if (*curbuf->b_p_ft == NUL)
            {
                if (au_has_group((char_u *)"filetypedetect"))
                    (void)do_doautocmd((char_u *)"filetypedetect BufRead", TRUE);
                do_modelines(0);
            }

            /* Autocommands may have changed buffer names, esp. when 'autochdir' is set. */
            fname = curbuf->b_sfname;
        }

        retval = buf_write(curbuf, ffname, fname, eap->line1, eap->line2,
                                 eap, eap->append, eap->forceit, TRUE, FALSE);

        /* After ":saveas fname" reset 'readonly'. */
        if (eap->cmdidx == CMD_saveas)
        {
            if (retval == OK)
            {
                curbuf->b_p_ro = FALSE;
                redraw_tabline = TRUE;
            }
        }
    }

theend:
    vim_free(free_fname);
    return retval;
}

/*
 * Check if it is allowed to overwrite a file.  If b_flags has BF_NOTEDITED,
 * BF_NEW or BF_READERR, check for overwriting current file.
 * May set eap->forceit if a dialog says it's OK to overwrite.
 * Return OK if it's OK, FAIL if it is not.
 */
    int
check_overwrite(eap, buf, fname, ffname, other)
    exarg_T     *eap;
    buf_T       *buf;
    char_u      *fname;     /* file name to be used (can differ from
                               buf->ffname) */
    char_u      *ffname;    /* full path version of fname */
    int         other;      /* writing under other name */
{
    /*
     * write to other file or b_flags set or not writing the whole file:
     * overwriting only allowed with '!'
     */
    if ((other
               || (buf->b_flags & BF_NOTEDITED)
               || ((buf->b_flags & BF_NEW) && vim_strchr(p_cpo, CPO_OVERNEW) == NULL)
               || (buf->b_flags & BF_READERR))
            && !p_wa
            && vim_fexists(ffname))
    {
        if (!eap->forceit && !eap->append)
        {
            /* with UNIX it is possible to open a directory */
            if (mch_isdir(ffname))
            {
                EMSG2((char *)e_isadir2, ffname);
                return FAIL;
            }
            if (p_confirm || cmdmod.confirm)
            {
                char_u  buff[DIALOG_MSG_SIZE];

                dialog_msg(buff, "Overwrite existing file \"%s\"?", fname);
                if (vim_dialog_yesno(VIM_QUESTION, NULL, buff, 2) != VIM_YES)
                    return FAIL;
                eap->forceit = TRUE;
            }
            else
            {
                EMSG((char *)e_exists);
                return FAIL;
            }
        }

        /* For ":w! filename" check that no swap file exists for "filename". */
        if (other && !emsg_silent)
        {
            char_u      *dir;
            char_u      *p;
            int         r;
            char_u      *swapname;

            /* We only try the first entry in 'directory', without checking if
             * it's writable.  If the "." directory is not writable the write
             * will probably fail anyway.
             * Use 'shortname' of the current buffer, since there is no buffer
             * for the written file. */
            if (*p_dir == NUL)
            {
                dir = alloc(5);
                if (dir == NULL)
                    return FAIL;
                STRCPY(dir, ".");
            }
            else
            {
                dir = alloc(MAXPATHL);
                if (dir == NULL)
                    return FAIL;
                p = p_dir;
                copy_option_part(&p, dir, MAXPATHL, ",");
            }
            swapname = makeswapname(fname, ffname, curbuf, dir);
            vim_free(dir);
            r = vim_fexists(swapname);
            if (r)
            {
                if (p_confirm || cmdmod.confirm)
                {
                    char_u      buff[DIALOG_MSG_SIZE];

                    dialog_msg(buff, "Swap file \"%s\" exists, overwrite anyway?", swapname);
                    if (vim_dialog_yesno(VIM_QUESTION, NULL, buff, 2) != VIM_YES)
                    {
                        vim_free(swapname);
                        return FAIL;
                    }
                    eap->forceit = TRUE;
                }
                else
                {
                    EMSG2("E768: Swap file exists: %s (:silent! overrides)", swapname);
                    vim_free(swapname);
                    return FAIL;
                }
            }
            vim_free(swapname);
        }
    }
    return OK;
}

/*
 * Handle ":wnext", ":wNext" and ":wprevious" commands.
 */
    void
ex_wnext(eap)
    exarg_T     *eap;
{
    int         i;

    if (eap->cmd[1] == 'n')
        i = curwin->w_arg_idx + (int)eap->line2;
    else
        i = curwin->w_arg_idx - (int)eap->line2;
    eap->line1 = 1;
    eap->line2 = curbuf->b_ml.ml_line_count;
    if (do_write(eap) != FAIL)
        do_argfile(eap, i);
}

/*
 * ":wall", ":wqall" and ":xall": Write all changed files (and exit).
 */
    void
do_wqall(eap)
    exarg_T     *eap;
{
    buf_T       *buf;
    int         error = 0;
    int         save_forceit = eap->forceit;

    if (eap->cmdidx == CMD_xall || eap->cmdidx == CMD_wqall)
        exiting = TRUE;

    for (buf = firstbuf; buf != NULL; buf = buf->b_next)
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
                ++error;
                break;
            }
            if (buf->b_ffname == NULL)
            {
                EMSGN("E141: No file name for buffer %ld", (long)buf->b_fnum);
                ++error;
            }
            else if (check_readonly(&eap->forceit, buf)
                    || check_overwrite(eap, buf, buf->b_fname, buf->b_ffname, FALSE) == FAIL)
            {
                ++error;
            }
            else
            {
                if (buf_write_all(buf, eap->forceit) == FAIL)
                    ++error;
                /* an autocommand may have deleted the buffer */
                if (!buf_valid(buf))
                    buf = firstbuf;
            }
            eap->forceit = save_forceit;    /* check_overwrite() may set it */
        }
    }
    if (exiting)
    {
        if (!error)
            getout(0);          /* exit Vim */
        not_exiting();
    }
}

/*
 * Check the 'write' option.
 * Return TRUE and give a message when it's not st.
 */
    int
not_writing()
{
    if (p_write)
        return FALSE;
    EMSG("E142: File not written: Writing is disabled by 'write' option");
    return TRUE;
}

/*
 * Check if a buffer is read-only (either 'readonly' option is set or file is
 * read-only). Ask for overruling in a dialog. Return TRUE and give an error
 * message when the buffer is readonly.
 */
    static int
check_readonly(forceit, buf)
    int         *forceit;
    buf_T       *buf;
{
    struct stat st;

    /* Handle a file being readonly when the 'readonly' option is set or when
     * the file exists and permissions are read-only.
     * We will send 0777 to check_file_readonly(), as the "perm" variable is
     * important for device checks but not here. */
    if (!*forceit && (buf->b_p_ro
                || (mch_stat((char *)buf->b_ffname, &st) >= 0
                    && check_file_readonly(buf->b_ffname, 0777))))
    {
        if ((p_confirm || cmdmod.confirm) && buf->b_fname != NULL)
        {
            char_u      buff[DIALOG_MSG_SIZE];

            if (buf->b_p_ro)
                dialog_msg(buff, "'readonly' option is set for \"%s\".\nDo you wish to write anyway?", buf->b_fname);
            else
                dialog_msg(buff, "File permissions of \"%s\" are read-only.\nIt may still be possible to write it.\nDo you wish to try?", buf->b_fname);

            if (vim_dialog_yesno(VIM_QUESTION, NULL, buff, 2) == VIM_YES)
            {
                /* Set forceit, to force the writing of a readonly file */
                *forceit = TRUE;
                return FALSE;
            }
            else
                return TRUE;
        }
        else if (buf->b_p_ro)
            EMSG((char *)e_readonly);
        else
            EMSG2("E505: \"%s\" is read-only (add ! to override)", buf->b_fname);
        return TRUE;
    }

    return FALSE;
}

/*
 * Try to abandon current file and edit a new or existing file.
 * 'fnum' is the number of the file, if zero use ffname/sfname.
 *
 * Return 1 for "normal" error, 2 for "not written" error, 0 for success
 * -1 for successfully opening another file.
 * 'lnum' is the line number for the cursor in the new file (if non-zero).
 */
    int
getfile(fnum, ffname, sfname, setpm, lnum, forceit)
    int         fnum;
    char_u      *ffname;
    char_u      *sfname;
    int         setpm;
    linenr_T    lnum;
    int         forceit;
{
    int         other;
    int         retval;
    char_u      *free_me = NULL;

    if (text_locked())
        return 1;
    if (curbuf_locked())
        return 1;

    if (fnum == 0)
    {
                                        /* make ffname full path, set sfname */
        fname_expand(curbuf, &ffname, &sfname);
        other = otherfile(ffname);
        free_me = ffname;               /* has been allocated, free() later */
    }
    else
        other = (fnum != curbuf->b_fnum);

    if (other)
        ++no_wait_return;           /* don't wait for autowrite message */
    if (other && !forceit && curbuf->b_nwindows == 1 && !P_HID(curbuf)
                   && curbufIsChanged() && autowrite(curbuf, forceit) == FAIL)
    {
        if (p_confirm && p_write)
            dialog_changed(curbuf, FALSE);
        if (curbufIsChanged())
        {
            if (other)
                --no_wait_return;
            EMSG((char *)e_nowrtmsg);
            retval = 2; /* file has been changed */
            goto theend;
        }
    }
    if (other)
        --no_wait_return;
    if (setpm)
        setpcmark();
    if (!other)
    {
        if (lnum != 0)
            curwin->w_cursor.lnum = lnum;
        check_cursor_lnum();
        beginline(BL_SOL | BL_FIX);
        retval = 0;     /* it's in the same file */
    }
    else if (do_ecmd(fnum, ffname, sfname, NULL, lnum,
                (P_HID(curbuf) ? ECMD_HIDE : 0) + (forceit ? ECMD_FORCEIT : 0), curwin) == OK)
        retval = -1;    /* opened another file */
    else
        retval = 1;     /* error encountered */

theend:
    vim_free(free_me);
    return retval;
}

/*
 * start editing a new file
 *
 *     fnum: file number; if zero use ffname/sfname
 *   ffname: the file name
 *              - full path if sfname used,
 *              - any file name if sfname is NULL
 *              - empty string to re-edit with the same file name (but may be
 *                  in a different directory)
 *              - NULL to start an empty buffer
 *   sfname: the short file name (or NULL)
 *      eap: contains the command to be executed after loading the file and
 *           forced 'ff' and 'fenc'
 *  newlnum: if > 0: put cursor on this line number (if possible)
 *           if ECMD_LASTL: use last position in loaded file
 *           if ECMD_LAST: use last position in all files
 *           if ECMD_ONE: use first line
 *    flags:
 *         ECMD_HIDE: if TRUE don't free the current buffer
 *       ECMD_OLDBUF: use existing buffer if it exists
 *      ECMD_FORCEIT: ! used for Ex command
 *       ECMD_ADDBUF: don't edit, just add to buffer list
 *   oldwin: Should be "curwin" when editing a new buffer in the current
 *           window, NULL when splitting the window first.  When not NULL info
 *           of the previous buffer for "oldwin" is stored.
 *
 * return FAIL for failure, OK otherwise
 */
    int
do_ecmd(fnum, ffname, sfname, eap, newlnum, flags, oldwin)
    int         fnum;
    char_u      *ffname;
    char_u      *sfname;
    exarg_T     *eap;                   /* can be NULL! */
    linenr_T    newlnum;
    int         flags;
    win_T       *oldwin;
{
    int         other_file;             /* TRUE if editing another file */
    int         oldbuf;                 /* TRUE if using existing buffer */
    int         auto_buf = FALSE;       /* TRUE if autocommands brought us
                                           into the buffer unexpectedly */
    char_u      *new_name = NULL;
    int         did_set_swapcommand = FALSE;
    buf_T       *buf;
    buf_T       *old_curbuf = curbuf;
    char_u      *free_fname = NULL;
    int         retval = FAIL;
    long        n;
    pos_T       orig_pos;
    linenr_T    topline = 0;
    int         newcol = -1;
    int         solcol = -1;
    pos_T       *pos;
    char_u      *command = NULL;
    int         readfile_flags = 0;

    if (eap != NULL)
        command = eap->do_ecmd_cmd;

    if (fnum != 0)
    {
        if (fnum == curbuf->b_fnum)     /* file is already being edited */
            return OK;                  /* nothing to do */
        other_file = TRUE;
    }
    else
    {
        /* if no short name given, use ffname for short name */
        if (sfname == NULL)
            sfname = ffname;

        if ((flags & ECMD_ADDBUF) && (ffname == NULL || *ffname == NUL))
            goto theend;

        if (ffname == NULL)
            other_file = TRUE;
                                            /* there is no file name */
        else if (*ffname == NUL && curbuf->b_ffname == NULL)
            other_file = FALSE;
        else
        {
            if (*ffname == NUL)             /* re-edit with same file name */
            {
                ffname = curbuf->b_ffname;
                sfname = curbuf->b_fname;
            }
            free_fname = fix_fname(ffname); /* may expand to full path name */
            if (free_fname != NULL)
                ffname = free_fname;
            other_file = otherfile(ffname);
        }
    }

    /*
     * if the file was changed we may not be allowed to abandon it
     * - if we are going to re-edit the same file
     * - or if we are the only window on this file and if ECMD_HIDE is FALSE
     */
    if (  ((!other_file && !(flags & ECMD_OLDBUF))
            || (curbuf->b_nwindows == 1
                && !(flags & (ECMD_HIDE | ECMD_ADDBUF))))
        && check_changed(curbuf, (p_awa ? CCGD_AW : 0)
                               | (other_file ? 0 : CCGD_MULTWIN)
                               | ((flags & ECMD_FORCEIT) ? CCGD_FORCEIT : 0)
                               | (eap == NULL ? 0 : CCGD_EXCMD)))
    {
        if (fnum == 0 && other_file && ffname != NULL)
            (void)setaltfname(ffname, sfname, newlnum < 0 ? 0 : newlnum);
        goto theend;
    }

    /*
     * End Visual mode before switching to another buffer, so the text can be
     * copied into the GUI selection buffer.
     */
    reset_VIsual();

    if ((command != NULL || newlnum > (linenr_T)0) && *get_vim_var_str(VV_SWAPCOMMAND) == NUL)
    {
        int     len;
        char_u  *p;

        /* Set v:swapcommand for the SwapExists autocommands. */
        if (command != NULL)
            len = (int)STRLEN(command) + 3;
        else
            len = 30;
        p = alloc((unsigned)len);
        if (p != NULL)
        {
            if (command != NULL)
                vim_snprintf((char *)p, len, ":%s\r", command);
            else
                vim_snprintf((char *)p, len, "%ldG", (long)newlnum);
            set_vim_var_string(VV_SWAPCOMMAND, p, -1);
            did_set_swapcommand = TRUE;
            vim_free(p);
        }
    }

    /*
     * If we are starting to edit another file, open a (new) buffer.
     * Otherwise we re-use the current buffer.
     */
    if (other_file)
    {
        if (!(flags & ECMD_ADDBUF))
        {
            if (!cmdmod.keepalt)
                curwin->w_alt_fnum = curbuf->b_fnum;
            if (oldwin != NULL)
                buflist_altfpos(oldwin);
        }

        if (fnum)
            buf = buflist_findnr(fnum);
        else
        {
            if (flags & ECMD_ADDBUF)
            {
                linenr_T        tlnum = 1L;

                if (command != NULL)
                {
                    tlnum = atol((char *)command);
                    if (tlnum <= 0)
                        tlnum = 1L;
                }
                (void)buflist_new(ffname, sfname, tlnum, BLN_LISTED);
                goto theend;
            }
            buf = buflist_new(ffname, sfname, 0L, BLN_CURBUF | BLN_LISTED);
            /* autocommands may change curwin and curbuf */
            if (oldwin != NULL)
                oldwin = curwin;
            old_curbuf = curbuf;
        }
        if (buf == NULL)
            goto theend;
        if (buf->b_ml.ml_mfp == NULL)           /* no memfile yet */
        {
            oldbuf = FALSE;
        }
        else                                    /* existing memfile */
        {
            oldbuf = TRUE;
            (void)buf_check_timestamp(buf, FALSE);
            /* Check if autocommands made buffer invalid or changed the current buffer. */
            if (!buf_valid(buf) || curbuf != old_curbuf)
                goto theend;
            if (aborting())         /* autocmds may abort script processing */
                goto theend;
        }

        /* May jump to last used line number for a loaded buffer or when asked for explicitly */
        if ((oldbuf && newlnum == ECMD_LASTL) || newlnum == ECMD_LAST)
        {
            pos = buflist_findfpos(buf);
            newlnum = pos->lnum;
            solcol = pos->col;
        }

        /*
         * Make the (new) buffer the one used by the current window.
         * If the old buffer becomes unused, free it if ECMD_HIDE is FALSE.
         * If the current buffer was empty and has no file name, curbuf
         * is returned by buflist_new(), nothing to do here.
         */
        if (buf != curbuf)
        {
            /*
             * Be careful: The autocommands may delete any buffer and change
             * the current buffer.
             * - If the buffer we are going to edit is deleted, give up.
             * - If the current buffer is deleted, prefer to load the new
             *   buffer when loading a buffer is required.  This avoids
             *   loading another buffer which then must be closed again.
             * - If we ended up in the new buffer already, need to skip a few
             *   things, set auto_buf.
             */
            if (buf->b_fname != NULL)
                new_name = vim_strsave(buf->b_fname);
            au_new_curbuf = buf;
            apply_autocmds(EVENT_BUFLEAVE, NULL, NULL, FALSE, curbuf);
            if (!buf_valid(buf))        /* new buffer has been deleted */
            {
                delbuf_msg(new_name);   /* frees new_name */
                goto theend;
            }
            if (aborting())         /* autocmds may abort script processing */
            {
                vim_free(new_name);
                goto theend;
            }
            if (buf == curbuf)          /* already in new buffer */
                auto_buf = TRUE;
            else
            {
                if (curbuf == old_curbuf)
                    buf_copy_options(buf, BCO_ENTER);

                /* close the link to the current buffer */
                u_sync(FALSE);
                close_buffer(oldwin, curbuf,
                               (flags & ECMD_HIDE) ? 0 : DOBUF_UNLOAD, FALSE);

                /* Autocommands may open a new window and leave oldwin open
                 * which leads to crashes since the above call sets
                 * oldwin->w_buffer to NULL. */
                if (curwin != oldwin && oldwin != aucmd_win
                             && win_valid(oldwin) && oldwin->w_buffer == NULL)
                    win_close(oldwin, FALSE);

                if (aborting())     /* autocmds may abort script processing */
                {
                    vim_free(new_name);
                    goto theend;
                }
                /* Be careful again, like above. */
                if (!buf_valid(buf))    /* new buffer has been deleted */
                {
                    delbuf_msg(new_name);       /* frees new_name */
                    goto theend;
                }
                if (buf == curbuf)              /* already in new buffer */
                    auto_buf = TRUE;
                else
                {
                    /*
                     * <VN> We could instead free the synblock
                     * and re-attach to buffer, perhaps.
                     */
                    if (curwin->w_s == &(curwin->w_buffer->b_s))
                        curwin->w_s = &(buf->b_s);
                    curwin->w_buffer = buf;
                    curbuf = buf;
                    ++curbuf->b_nwindows;

                    /* Set 'fileformat', 'binary' and 'fenc' when forced. */
                    if (!oldbuf && eap != NULL)
                    {
                        set_file_options(TRUE, eap);
                        set_forced_fenc(eap);
                    }
                }

                /* May get the window options from the last time this buffer
                 * was in this window (or another window).  If not used
                 * before, reset the local window options to the global
                 * values.  Also restores old folding stuff. */
                get_winopts(curbuf);
            }
            vim_free(new_name);
            au_new_curbuf = NULL;
        }

        curwin->w_pcmark.lnum = 1;
        curwin->w_pcmark.col = 0;
    }
    else /* !other_file */
    {
        if ((flags & ECMD_ADDBUF) || check_fname() == FAIL)
            goto theend;

        oldbuf = (flags & ECMD_OLDBUF);
    }

    buf = curbuf;
    set_buflisted(TRUE);

    /* If autocommands change buffers under our fingers, forget about editing the file. */
    if (buf != curbuf)
        goto theend;
    if (aborting())         /* autocmds may abort script processing */
        goto theend;

    /* Since we are starting to edit a file, consider the filetype to be
     * unset.  Helps for when an autocommand changes files and expects syntax
     * highlighting to work in the other file. */
    did_filetype = FALSE;

/*
 * other_file   oldbuf
 *  FALSE       FALSE       re-edit same file, buffer is re-used
 *  FALSE       TRUE        re-edit same file, nothing changes
 *  TRUE        FALSE       start editing new file, new buffer
 *  TRUE        TRUE        start editing in existing buffer (nothing to do)
 */
    if (!other_file && !oldbuf)         /* re-use the buffer */
    {
        set_last_cursor(curwin);        /* may set b_last_cursor */
        if (newlnum == ECMD_LAST || newlnum == ECMD_LASTL)
        {
            newlnum = curwin->w_cursor.lnum;
            solcol = curwin->w_cursor.col;
        }
        buf = curbuf;
        if (buf->b_fname != NULL)
            new_name = vim_strsave(buf->b_fname);
        else
            new_name = NULL;
        if (p_ur < 0 || curbuf->b_ml.ml_line_count <= p_ur)
        {
            /* Save all the text, so that the reload can be undone.
             * Sync first so that this is a separate undo-able action. */
            u_sync(FALSE);
            if (u_savecommon(0, curbuf->b_ml.ml_line_count + 1, 0, TRUE) == FAIL)
                goto theend;
            u_unchanged(curbuf);
            buf_freeall(curbuf, BFA_KEEP_UNDO);

            /* tell readfile() not to clear or reload undo info */
            readfile_flags = READ_KEEP_UNDO;
        }
        else
            buf_freeall(curbuf, 0);   /* free all things for buffer */
        /* If autocommands deleted the buffer we were going to re-edit, give
         * up and jump to the end. */
        if (!buf_valid(buf))
        {
            delbuf_msg(new_name);       /* frees new_name */
            goto theend;
        }
        vim_free(new_name);

        /* If autocommands change buffers under our fingers, forget about
         * re-editing the file.  Should do the buf_clear_file(), but perhaps
         * the autocommands changed the buffer... */
        if (buf != curbuf)
            goto theend;
        if (aborting())     /* autocmds may abort script processing */
            goto theend;
        buf_clear_file(curbuf);
        curbuf->b_op_start.lnum = 0;    /* clear '[ and '] marks */
        curbuf->b_op_end.lnum = 0;
    }

/*
 * If we get here we are sure to start editing
 */
    /* don't redraw until the cursor is in the right line */
    ++RedrawingDisabled;

    /* Assume success now */
    retval = OK;

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
         * Set cursor and init window before reading the file and executing
         * autocommands.  This allows for the autocommands to position the cursor.
         */
        curwin_init();

        /*
         * Careful: open_buffer() and apply_autocmds() may change the current buffer and window.
         */
        orig_pos = curwin->w_cursor;
        topline = curwin->w_topline;
        if (!oldbuf)                        /* need to read the file */
        {
            swap_exists_action = SEA_DIALOG;
            curbuf->b_flags |= BF_CHECK_RO; /* set/reset 'ro' flag */

            /*
             * Open the buffer and read the file.
             */
            if (should_abort(open_buffer(FALSE, eap, readfile_flags)))
                retval = FAIL;

            if (swap_exists_action == SEA_QUIT)
                retval = FAIL;
            handle_swap_exists(old_curbuf);
        }
        else
        {
            /* Read the modelines, but only to set window-local options.  Any
             * buffer-local options have already been set and may have been
             * changed by the user. */
            do_modelines(OPT_WINONLY);

            apply_autocmds_retval(EVENT_BUFENTER, NULL, NULL, FALSE, curbuf, &retval);
            apply_autocmds_retval(EVENT_BUFWINENTER, NULL, NULL, FALSE, curbuf, &retval);
        }
        check_arg_idx(curwin);

        /* If autocommands change the cursor position or topline, we should
         * keep it.  Also when it moves within a line. */
        if (!equalpos(curwin->w_cursor, orig_pos))
        {
            newlnum = curwin->w_cursor.lnum;
            newcol = curwin->w_cursor.col;
        }
        if (curwin->w_topline == topline)
            topline = 0;

        /* Even when cursor didn't move we need to recompute topline. */
        changed_line_abv_curs();

        maketitle();
    }

    if (command == NULL)
    {
        if (newcol >= 0)        /* position set by autocommands */
        {
            curwin->w_cursor.lnum = newlnum;
            curwin->w_cursor.col = newcol;
            check_cursor();
        }
        else if (newlnum > 0)   /* line number from caller or old position */
        {
            curwin->w_cursor.lnum = newlnum;
            check_cursor_lnum();
            if (solcol >= 0 && !p_sol)
            {
                /* 'sol' is off: Use last known column. */
                curwin->w_cursor.col = solcol;
                check_cursor_col();
                curwin->w_cursor.coladd = 0;
                curwin->w_set_curswant = TRUE;
            }
            else
                beginline(BL_SOL | BL_FIX);
        }
        else                    /* no line number, go to last line in Ex mode */
        {
            if (exmode_active)
                curwin->w_cursor.lnum = curbuf->b_ml.ml_line_count;
            beginline(BL_WHITE | BL_FIX);
        }
    }

    /* Check if cursors in other windows on the same buffer are still valid */
    check_lnums(FALSE);

    /*
     * Did not read the file, need to show some info about the file.
     * Do this after setting the cursor.
     */
    if (oldbuf && !auto_buf)
    {
        int     msg_scroll_save = msg_scroll;

        /* Obey the 'O' flag in 'cpoptions': overwrite any previous file message. */
        if (shortmess(SHM_OVERALL) && !exiting && p_verbose == 0)
            msg_scroll = FALSE;
        if (!msg_scroll)        /* wait a bit when overwriting an error msg */
            check_for_delay(FALSE);
        msg_start();
        msg_scroll = msg_scroll_save;
        msg_scrolled_ign = TRUE;

        fileinfo(FALSE, FALSE);

        msg_scrolled_ign = FALSE;
    }

    if (command != NULL)
        do_cmdline(command, NULL, NULL, DOCMD_VERBOSE);

    --RedrawingDisabled;
    if (!skip_redraw)
    {
        n = p_so;
        if (topline == 0 && command == NULL)
            p_so = 999;                 /* force cursor halfway the window */
        update_topline();
        curwin->w_scbind_pos = curwin->w_topline;
        p_so = n;
        redraw_curbuf_later(NOT_VALID); /* redraw this buffer later */
    }

    if (p_im)
        need_start_insertmode = TRUE;

theend:
    if (did_set_swapcommand)
        set_vim_var_string(VV_SWAPCOMMAND, NULL, -1);
    vim_free(free_fname);
    return retval;
}

    static void
delbuf_msg(name)
    char_u      *name;
{
    EMSG2("E143: Autocommands unexpectedly deleted new buffer %s", name == NULL ? (char_u *)"" : name);
    vim_free(name);
    au_new_curbuf = NULL;
}

static int append_indent = 0;       /* autoindent for first line */

/*
 * ":insert" and ":append", also used by ":change"
 */
    void
ex_append(eap)
    exarg_T     *eap;
{
    char_u      *theline;
    int         did_undo = FALSE;
    linenr_T    lnum = eap->line2;
    int         indent = 0;
    char_u      *p;
    int         vcol;
    int         empty = (curbuf->b_ml.ml_flags & ML_EMPTY);

    /* the ! flag toggles autoindent */
    if (eap->forceit)
        curbuf->b_p_ai = !curbuf->b_p_ai;

    /* First autoindent comes from the line we start on */
    if (eap->cmdidx != CMD_change && curbuf->b_p_ai && lnum > 0)
        append_indent = get_indent_lnum(lnum);

    if (eap->cmdidx != CMD_append)
        --lnum;

    /* when the buffer is empty append to line 0 and delete the dummy line */
    if (empty && lnum == 1)
        lnum = 0;

    State = INSERT;                 /* behave like in Insert mode */
    if (curbuf->b_p_iminsert == B_IMODE_LMAP)
        State |= LANGMAP;

    for (;;)
    {
        msg_scroll = TRUE;
        need_wait_return = FALSE;
        if (curbuf->b_p_ai)
        {
            if (append_indent >= 0)
            {
                indent = append_indent;
                append_indent = -1;
            }
            else if (lnum > 0)
                indent = get_indent_lnum(lnum);
        }
        ex_keep_indent = FALSE;
        if (eap->getline == NULL)
        {
            /* No getline() function, use the lines that follow. This ends
             * when there is no more. */
            if (eap->nextcmd == NULL || *eap->nextcmd == NUL)
                break;
            p = vim_strchr(eap->nextcmd, NL);
            if (p == NULL)
                p = eap->nextcmd + STRLEN(eap->nextcmd);
            theline = vim_strnsave(eap->nextcmd, (int)(p - eap->nextcmd));
            if (*p != NUL)
                ++p;
            eap->nextcmd = p;
        }
        else
        {
            int save_State = State;

            /* Set State to avoid the cursor shape to be set to INSERT mode
             * when getline() returns. */
            State = CMDLINE;
            theline = eap->getline(
                    (eap->cstack->cs_looplevel > 0) ? -1 : NUL, eap->cookie, indent);
            State = save_State;
        }
        lines_left = Rows - 1;
        if (theline == NULL)
            break;

        /* Using ^ CTRL-D in getexmodeline() makes us repeat the indent. */
        if (ex_keep_indent)
            append_indent = indent;

        /* Look for the "." after automatic indent. */
        vcol = 0;
        for (p = theline; indent > vcol; ++p)
        {
            if (*p == ' ')
                ++vcol;
            else if (*p == TAB)
                vcol += 8 - vcol % 8;
            else
                break;
        }
        if ((p[0] == '.' && p[1] == NUL) || (!did_undo && u_save(lnum, lnum + 1 + (empty ? 1 : 0)) == FAIL))
        {
            vim_free(theline);
            break;
        }

        /* don't use autoindent if nothing was typed. */
        if (p[0] == NUL)
            theline[0] = NUL;

        did_undo = TRUE;
        ml_append(lnum, theline, (colnr_T)0, FALSE);
        appended_lines_mark(lnum, 1L);

        vim_free(theline);
        ++lnum;

        if (empty)
        {
            ml_delete(2L, FALSE);
            empty = FALSE;
        }
    }
    State = NORMAL;

    if (eap->forceit)
        curbuf->b_p_ai = !curbuf->b_p_ai;

    /* "start" is set to eap->line2+1 unless that position is invalid (when
     * eap->line2 pointed to the end of the buffer and nothing was appended)
     * "end" is set to lnum when something has been appended, otherwise
     * it is the same than "start"  -- Acevedo */
    curbuf->b_op_start.lnum = (eap->line2 < curbuf->b_ml.ml_line_count) ?
        eap->line2 + 1 : curbuf->b_ml.ml_line_count;
    if (eap->cmdidx != CMD_append)
        --curbuf->b_op_start.lnum;
    curbuf->b_op_end.lnum = (eap->line2 < lnum) ? lnum : curbuf->b_op_start.lnum;
    curbuf->b_op_start.col = curbuf->b_op_end.col = 0;
    curwin->w_cursor.lnum = lnum;
    check_cursor_lnum();
    beginline(BL_SOL | BL_FIX);

    need_wait_return = FALSE;   /* don't use wait_return() now */
    ex_no_reprint = TRUE;
}

/*
 * ":change"
 */
    void
ex_change(eap)
    exarg_T     *eap;
{
    linenr_T    lnum;

    if (eap->line2 >= eap->line1 && u_save(eap->line1 - 1, eap->line2 + 1) == FAIL)
        return;

    /* the ! flag toggles autoindent */
    if (eap->forceit ? !curbuf->b_p_ai : curbuf->b_p_ai)
        append_indent = get_indent_lnum(eap->line1);

    for (lnum = eap->line2; lnum >= eap->line1; --lnum)
    {
        if (curbuf->b_ml.ml_flags & ML_EMPTY)       /* nothing to delete */
            break;
        ml_delete(eap->line1, FALSE);
    }

    /* make sure the cursor is not beyond the end of the file now */
    check_cursor_lnum();
    deleted_lines_mark(eap->line1, (long)(eap->line2 - lnum));

    /* ":append" on the line above the deleted lines. */
    eap->line2 = eap->line1;
    ex_append(eap);
}

    void
ex_z(eap)
    exarg_T     *eap;
{
    char_u      *x;
    int         bigness;
    char_u      *kind;
    int         minus = 0;
    linenr_T    start, end, curs, i;
    int         j;
    linenr_T    lnum = eap->line2;

    /* Vi compatible: ":z!" uses display height, without a count uses 'scroll' */
    if (eap->forceit)
        bigness = curwin->w_height;
    else if (firstwin != lastwin)
        bigness = curwin->w_height - 3;
    else
        bigness = curwin->w_p_scr * 2;
    if (bigness < 1)
        bigness = 1;

    x = eap->arg;
    kind = x;
    if (*kind == '-' || *kind == '+' || *kind == '=' || *kind == '^' || *kind == '.')
        ++x;
    while (*x == '-' || *x == '+')
        ++x;

    if (*x != 0)
    {
        if (!VIM_ISDIGIT(*x))
        {
            EMSG("E144: non-numeric argument to :z");
            return;
        }
        else
        {
            bigness = atoi((char *)x);
            p_window = bigness;
            if (*kind == '=')
                bigness += 2;
        }
    }

    /* the number of '-' and '+' multiplies the distance */
    if (*kind == '-' || *kind == '+')
        for (x = kind + 1; *x == *kind; ++x)
            ;

    switch (*kind)
    {
        case '-':
            start = lnum - bigness * (linenr_T)(x - kind) + 1;
            end = start + bigness - 1;
            curs = end;
            break;

        case '=':
            start = lnum - (bigness + 1) / 2 + 1;
            end = lnum + (bigness + 1) / 2 - 1;
            curs = lnum;
            minus = 1;
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

        default:  /* '+' */
            start = lnum;
            if (*kind == '+')
                start += bigness * (linenr_T)(x - kind - 1) + 1;
            else if (eap->addr_count == 0)
                ++start;
            end = start + bigness - 1;
            curs = end;
            break;
    }

    if (start < 1)
        start = 1;

    if (end > curbuf->b_ml.ml_line_count)
        end = curbuf->b_ml.ml_line_count;

    if (curs > curbuf->b_ml.ml_line_count)
        curs = curbuf->b_ml.ml_line_count;

    for (i = start; i <= end; i++)
    {
        if (minus && i == lnum)
        {
            msg_putchar('\n');

            for (j = 1; j < Columns; j++)
                msg_putchar('-');
        }

        print_line(i, eap->flags & EXFLAG_NR, eap->flags & EXFLAG_LIST);

        if (minus && i == lnum)
        {
            msg_putchar('\n');

            for (j = 1; j < Columns; j++)
                msg_putchar('-');
        }
    }

    curwin->w_cursor.lnum = curs;
    ex_no_reprint = TRUE;
}

/*
 * Check if the restricted flag is set.
 * If so, give an error message and return TRUE.
 * Otherwise, return FALSE.
 */
    int
check_restricted()
{
    if (restricted)
    {
        EMSG("E145: Shell commands not allowed in rvim");
        return TRUE;
    }
    return FALSE;
}

/*
 * Check if the secure flag is set (.exrc or .vimrc in current directory).
 * If so, give an error message and return TRUE.
 * Otherwise, return FALSE.
 */
    int
check_secure()
{
    if (secure)
    {
        secure = 2;
        EMSG((char *)e_curdir);
        return TRUE;
    }
    /*
     * In the sandbox more things are not allowed, including the things
     * disallowed in secure mode.
     */
    if (sandbox != 0)
    {
        EMSG((char *)e_sandbox);
        return TRUE;
    }
    return FALSE;
}

static char_u   *old_sub = NULL;        /* previous substitute pattern */
static int      global_need_beginline;  /* call beginline() after ":g" */

/* do_sub()
 *
 * Perform a substitution from line eap->line1 to line eap->line2 using the
 * command pointed to by eap->arg which should be of the form:
 *
 * /pattern/substitution/{flags}
 *
 * The usual escapes are supported as described in the regexp docs.
 */
    void
do_sub(eap)
    exarg_T     *eap;
{
    linenr_T    lnum;
    long        i = 0;
    regmmatch_T regmatch;
    static int  do_all = FALSE;         /* do multiple substitutions per line */
    static int  do_ask = FALSE;         /* ask for confirmation */
    static int  do_count = FALSE;       /* count only */
    static int  do_error = TRUE;        /* if false, ignore errors */
    static int  do_print = FALSE;       /* print last line with subs. */
    static int  do_list = FALSE;        /* list last line with subs. */
    static int  do_number = FALSE;      /* list last line with line nr */
    static int  do_ic = 0;              /* ignore case flag */
    char_u      *pat = NULL, *sub = NULL;       /* init for GCC */
    int         delimiter;
    int         sublen;
    int         got_quit = FALSE;
    int         got_match = FALSE;
    int         temp;
    int         which_pat;
    char_u      *cmd;
    int         save_State;
    linenr_T    first_line = 0;         /* first changed line */
    linenr_T    last_line= 0;           /* below last changed line AFTER the
                                         * change */
    linenr_T    old_line_count = curbuf->b_ml.ml_line_count;
    linenr_T    line2;
    long        nmatch;                 /* number of lines in match */
    char_u      *sub_firstline;         /* allocated copy of first sub line */
    int         endcolumn = FALSE;      /* cursor in last column when done */
    pos_T       old_cursor = curwin->w_cursor;
    int         start_nsubs;
    int         save_ma = 0;

    cmd = eap->arg;
    if (!global_busy)
    {
        sub_nsubs = 0;
        sub_nlines = 0;
    }
    start_nsubs = sub_nsubs;

    if (eap->cmdidx == CMD_tilde)
        which_pat = RE_LAST;    /* use last used regexp */
    else
        which_pat = RE_SUBST;   /* use last substitute regexp */

                                /* new pattern and substitution */
    if (eap->cmd[0] == 's' && *cmd != NUL && !vim_iswhite(*cmd)
                && vim_strchr((char_u *)"0123456789cegriIp|\"", *cmd) == NULL)
    {
                                /* don't accept alphanumeric for separator */
        if (isalpha(*cmd))
        {
            EMSG("E146: Regular expressions can't be delimited by letters");
            return;
        }
        /*
         * undocumented vi feature:
         *  "\/sub/" and "\?sub?" use last used search pattern (almost like
         *  //sub/r).  "\&sub&" use last substitute pattern (like //sub/).
         */
        if (*cmd == '\\')
        {
            ++cmd;
            if (vim_strchr((char_u *)"/?&", *cmd) == NULL)
            {
                EMSG((char *)e_backslash);
                return;
            }
            if (*cmd != '&')
                which_pat = RE_SEARCH;      /* use last '/' pattern */
            pat = (char_u *)"";             /* empty search pattern */
            delimiter = *cmd++;             /* remember delimiter character */
        }
        else            /* find the end of the regexp */
        {
            which_pat = RE_LAST;            /* use last used regexp */
            delimiter = *cmd++;             /* remember delimiter character */
            pat = cmd;                      /* remember start of search pat */
            cmd = skip_regexp(cmd, delimiter, p_magic, &eap->arg);
            if (cmd[0] == delimiter)        /* end delimiter found */
                *cmd++ = NUL;               /* replace it with a NUL */
        }

        /*
         * Small incompatibility: vi sees '\n' as end of the command, but in
         * Vim we want to use '\n' to find/substitute a NUL.
         */
        sub = cmd;          /* remember the start of the substitution */

        while (cmd[0])
        {
            if (cmd[0] == delimiter)            /* end delimiter found */
            {
                *cmd++ = NUL;                   /* replace it with a NUL */
                break;
            }
            if (cmd[0] == '\\' && cmd[1] != 0)  /* skip escaped characters */
                ++cmd;
            cmd += utfc_ptr2len(cmd);
        }

        if (!eap->skip)
        {
            /* In POSIX vi ":s/pat/%/" uses the previous subst. string. */
            if (STRCMP(sub, "%") == 0 && vim_strchr(p_cpo, CPO_SUBPERCENT) != NULL)
            {
                if (old_sub == NULL)    /* there is no previous command */
                {
                    EMSG((char *)e_nopresub);
                    return;
                }
                sub = old_sub;
            }
            else
            {
                vim_free(old_sub);
                old_sub = vim_strsave(sub);
            }
        }
    }
    else if (!eap->skip)        /* use previous pattern and substitution */
    {
        if (old_sub == NULL)    /* there is no previous command */
        {
            EMSG((char *)e_nopresub);
            return;
        }
        pat = NULL;             /* search_regcomp() will use previous pattern */
        sub = old_sub;

        /* Vi compatibility quirk: repeating with ":s" keeps the cursor in the
         * last column after using "$". */
        endcolumn = (curwin->w_curswant == MAXCOL);
    }

    /* Recognize ":%s/\n//" and turn it into a join command, which is much more efficient.
     * TODO: find a generic solution to make line-joining operations more
     * efficient, avoid allocating a string that grows in size.
     */
    if (pat != NULL && STRCMP(pat, "\\n") == 0
            && *sub == NUL
            && (*cmd == NUL || (cmd[1] == NUL && (*cmd == 'g' || *cmd == 'l' || *cmd == 'p' || *cmd == '#'))))
    {
        linenr_T    joined_lines_count;

        curwin->w_cursor.lnum = eap->line1;
        if (*cmd == 'l')
            eap->flags = EXFLAG_LIST;
        else if (*cmd == '#')
            eap->flags = EXFLAG_NR;
        else if (*cmd == 'p')
            eap->flags = EXFLAG_PRINT;

        /* The number of lines joined is the number of lines in the range plus
         * one.  One less when the last line is included. */
        joined_lines_count = eap->line2 - eap->line1 + 1;
        if (eap->line2 < curbuf->b_ml.ml_line_count)
            ++joined_lines_count;
        if (joined_lines_count > 1)
        {
            (void)do_join(joined_lines_count, FALSE, TRUE, FALSE, TRUE);
            sub_nsubs = joined_lines_count - 1;
            sub_nlines = 1;
            (void)do_sub_msg(FALSE);
            ex_may_print(eap);
        }

        if (!cmdmod.keeppatterns)
            save_re_pat(RE_SUBST, pat, p_magic);
        /* put pattern in history */
        add_to_history(HIST_SEARCH, pat, TRUE, NUL);

        return;
    }

    /*
     * Find trailing options.  When '&' is used, keep old options.
     */
    if (*cmd == '&')
        ++cmd;
    else
    {
        if (!p_ed)
        {
            if (p_gd)           /* default is global on */
                do_all = TRUE;
            else
                do_all = FALSE;
            do_ask = FALSE;
        }
        do_error = TRUE;
        do_print = FALSE;
        do_count = FALSE;
        do_number = FALSE;
        do_ic = 0;
    }
    while (*cmd)
    {
        /*
         * Note that 'g' and 'c' are always inverted, also when p_ed is off.
         * 'r' is never inverted.
         */
        if (*cmd == 'g')
            do_all = !do_all;
        else if (*cmd == 'c')
            do_ask = !do_ask;
        else if (*cmd == 'n')
            do_count = TRUE;
        else if (*cmd == 'e')
            do_error = !do_error;
        else if (*cmd == 'r')       /* use last used regexp */
            which_pat = RE_LAST;
        else if (*cmd == 'p')
            do_print = TRUE;
        else if (*cmd == '#')
        {
            do_print = TRUE;
            do_number = TRUE;
        }
        else if (*cmd == 'l')
        {
            do_print = TRUE;
            do_list = TRUE;
        }
        else if (*cmd == 'i')       /* ignore case */
            do_ic = 'i';
        else if (*cmd == 'I')       /* don't ignore case */
            do_ic = 'I';
        else
            break;
        ++cmd;
    }
    if (do_count)
        do_ask = FALSE;

    /*
     * check for a trailing count
     */
    cmd = skipwhite(cmd);
    if (VIM_ISDIGIT(*cmd))
    {
        i = getdigits(&cmd);
        if (i <= 0 && !eap->skip && do_error)
        {
            EMSG((char *)e_zerocount);
            return;
        }
        eap->line1 = eap->line2;
        eap->line2 += i - 1;
        if (eap->line2 > curbuf->b_ml.ml_line_count)
            eap->line2 = curbuf->b_ml.ml_line_count;
    }

    /*
     * check for trailing command or garbage
     */
    cmd = skipwhite(cmd);
    if (*cmd && *cmd != '"')        /* if not end-of-line or comment */
    {
        eap->nextcmd = check_nextcmd(cmd);
        if (eap->nextcmd == NULL)
        {
            EMSG((char *)e_trailing);
            return;
        }
    }

    if (eap->skip)          /* not executing commands, only parsing */
        return;

    if (!do_count && !curbuf->b_p_ma)
    {
        /* Substitution is not allowed in non-'modifiable' buffer */
        EMSG((char *)e_modifiable);
        return;
    }

    if (search_regcomp(pat, RE_SUBST, which_pat, SEARCH_HIS, &regmatch) == FAIL)
    {
        if (do_error)
            EMSG((char *)e_invcmd);
        return;
    }

    /* the 'i' or 'I' flag overrules 'ignorecase' and 'smartcase' */
    if (do_ic == 'i')
        regmatch.rmm_ic = TRUE;
    else if (do_ic == 'I')
        regmatch.rmm_ic = FALSE;

    sub_firstline = NULL;

    /*
     * ~ in the substitute pattern is replaced with the old pattern.
     * We do it here once to avoid it to be replaced over and over again.
     * But don't do it when it starts with "\=", then it's an expression.
     */
    if (!(sub[0] == '\\' && sub[1] == '='))
        sub = regtilde(sub, p_magic);

    /*
     * Check for a match on each line.
     */
    line2 = eap->line2;
    for (lnum = eap->line1; lnum <= line2 && !(got_quit || aborting()); ++lnum)
    {
        nmatch = vim_regexec_multi(&regmatch, curwin, curbuf, lnum, (colnr_T)0, NULL);
        if (nmatch)
        {
            colnr_T     copycol;
            colnr_T     matchcol;
            colnr_T     prev_matchcol = MAXCOL;
            char_u      *new_end, *new_start = NULL;
            unsigned    new_start_len = 0;
            char_u      *p1;
            int         did_sub = FALSE;
            int         lastone;
            int         len, copy_len, needed_len;
            long        nmatch_tl = 0;  /* nr of lines matched below lnum */
            int         do_again;       /* do it again after joining lines */
            int         skip_match = FALSE;
            linenr_T    sub_firstlnum;  /* nr of first sub line */

            /*
             * The new text is build up step by step, to avoid too much
             * copying.  There are these pieces:
             * sub_firstline    The old text, unmodified.
             * copycol          Column in the old text where we started
             *                  looking for a match; from here old text still
             *                  needs to be copied to the new text.
             * matchcol         Column number of the old text where to look
             *                  for the next match.  It's just after the
             *                  previous match or one further.
             * prev_matchcol    Column just after the previous match (if any).
             *                  Mostly equal to matchcol, except for the first
             *                  match and after skipping an empty match.
             * regmatch.*pos    Where the pattern matched in the old text.
             * new_start        The new text, all that has been produced so
             *                  far.
             * new_end          The new text, where to append new text.
             *
             * lnum             The line number where we found the start of
             *                  the match.  Can be below the line we searched
             *                  when there is a \n before a \zs in the
             *                  pattern.
             * sub_firstlnum    The line number in the buffer where to look
             *                  for a match.  Can be different from "lnum"
             *                  when the pattern or substitute string contains
             *                  line breaks.
             *
             * Special situations:
             * - When the substitute string contains a line break, the part up
             *   to the line break is inserted in the text, but the copy of
             *   the original line is kept.  "sub_firstlnum" is adjusted for
             *   the inserted lines.
             * - When the matched pattern contains a line break, the old line
             *   is taken from the line at the end of the pattern.  The lines
             *   in the match are deleted later, "sub_firstlnum" is adjusted
             *   accordingly.
             *
             * The new text is built up in new_start[].  It has some extra
             * room to avoid using alloc()/free() too often.  new_start_len is
             * the length of the allocated memory at new_start.
             *
             * Make a copy of the old line, so it won't be taken away when
             * updating the screen or handling a multi-line match.  The "old_"
             * pointers point into this copy.
             */
            sub_firstlnum = lnum;
            copycol = 0;
            matchcol = 0;

            /* At first match, remember current cursor position. */
            if (!got_match)
            {
                setpcmark();
                got_match = TRUE;
            }

            /*
             * Loop until nothing more to replace in this line.
             * 1. Handle match with empty string.
             * 2. If do_ask is set, ask for confirmation.
             * 3. substitute the string.
             * 4. if do_all is set, find next match
             * 5. break if there isn't another match in this line
             */
            for (;;)
            {
                /* Advance "lnum" to the line where the match starts.  The
                 * match does not start in the first line when there is a line
                 * break before \zs. */
                if (regmatch.startpos[0].lnum > 0)
                {
                    lnum += regmatch.startpos[0].lnum;
                    sub_firstlnum += regmatch.startpos[0].lnum;
                    nmatch -= regmatch.startpos[0].lnum;
                    vim_free(sub_firstline);
                    sub_firstline = NULL;
                }

                if (sub_firstline == NULL)
                {
                    sub_firstline = vim_strsave(ml_get(sub_firstlnum));
                    if (sub_firstline == NULL)
                    {
                        vim_free(new_start);
                        goto outofmem;
                    }
                }

                /* Save the line number of the last change for the final
                 * cursor position (just like Vi). */
                curwin->w_cursor.lnum = lnum;
                do_again = FALSE;

                /*
                 * 1. Match empty string does not count, except for first
                 * match.  This reproduces the strange vi behaviour.
                 * This also catches endless loops.
                 */
                if (matchcol == prev_matchcol
                        && regmatch.endpos[0].lnum == 0
                        && matchcol == regmatch.endpos[0].col)
                {
                    if (sub_firstline[matchcol] == NUL)
                        /* We already were at the end of the line.  Don't look
                         * for a match in this line again. */
                        skip_match = TRUE;
                    else
                    {
                        /* search for a match at next column */
                        matchcol += utfc_ptr2len(sub_firstline + matchcol);
                    }
                    goto skip;
                }

                /* Normally we continue searching for a match just after the previous match. */
                matchcol = regmatch.endpos[0].col;
                prev_matchcol = matchcol;

                /*
                 * 2. If do_count is set only increase the counter.
                 *    If do_ask is set, ask for confirmation.
                 */
                if (do_count)
                {
                    /* For a multi-line match, put matchcol at the NUL at
                     * the end of the line and set nmatch to one, so that
                     * we continue looking for a match on the next line.
                     * Avoids that ":s/\nB\@=//gc" get stuck. */
                    if (nmatch > 1)
                    {
                        matchcol = (colnr_T)STRLEN(sub_firstline);
                        nmatch = 1;
                        skip_match = TRUE;
                    }
                    sub_nsubs++;
                    did_sub = TRUE;
                    /* Skip the substitution, unless an expression is used,
                     * then it is evaluated in the sandbox. */
                    if (!(sub[0] == '\\' && sub[1] == '='))
                        goto skip;
                }

                if (do_ask)
                {
                    int typed = 0;

                    /* change State to CONFIRM, so that the mouse works properly */
                    save_State = State;
                    State = CONFIRM;
                    setmouse();         /* disable mouse in xterm */
                    curwin->w_cursor.col = regmatch.startpos[0].col;

                    /* When 'cpoptions' contains "u" don't sync undo when
                     * asking for confirmation. */
                    if (vim_strchr(p_cpo, CPO_UNDO) != NULL)
                        ++no_u_sync;

                    /*
                     * Loop until 'y', 'n', 'q', CTRL-E or CTRL-Y typed.
                     */
                    while (do_ask)
                    {
                        if (exmode_active)
                        {
                            char_u      *resp;
                            colnr_T     sc, ec;

                            print_line_no_prefix(lnum, do_number, do_list);

                            getvcol(curwin, &curwin->w_cursor, &sc, NULL, NULL);
                            curwin->w_cursor.col = regmatch.endpos[0].col - 1;
                            getvcol(curwin, &curwin->w_cursor, NULL, NULL, &ec);
                            if (do_number || curwin->w_p_nu)
                            {
                                int numw = number_width(curwin) + 1;
                                sc += numw;
                                ec += numw;
                            }
                            msg_start();
                            for (i = 0; i < (long)sc; ++i)
                                msg_putchar(' ');
                            for ( ; i <= (long)ec; ++i)
                                msg_putchar('^');

                            resp = getexmodeline('?', NULL, 0);
                            if (resp != NULL)
                            {
                                typed = *resp;
                                vim_free(resp);
                            }
                        }
                        else
                        {
                            char_u *orig_line = NULL;
                            int    len_change = 0;
                            /* Invert the matched string.
                             * Remove the inversion afterwards. */
                            temp = RedrawingDisabled;
                            RedrawingDisabled = 0;

                            if (new_start != NULL)
                            {
                                /* There already was a substitution, we would
                                 * like to show this to the user.  We cannot
                                 * really update the line, it would change
                                 * what matches.  Temporarily replace the line
                                 * and change it back afterwards. */
                                orig_line = vim_strsave(ml_get(lnum));
                                if (orig_line != NULL)
                                {
                                    char_u *new_line = concat_str(new_start, sub_firstline + copycol);

                                    if (new_line == NULL)
                                    {
                                        vim_free(orig_line);
                                        orig_line = NULL;
                                    }
                                    else
                                    {
                                        /* Position the cursor relative to the
                                         * end of the line, the previous
                                         * substitute may have inserted or
                                         * deleted characters before the cursor. */
                                        len_change = (int)STRLEN(new_line) - (int)STRLEN(orig_line);
                                        curwin->w_cursor.col += len_change;
                                        ml_replace(lnum, new_line, FALSE);
                                    }
                                }
                            }

                            search_match_lines = regmatch.endpos[0].lnum - regmatch.startpos[0].lnum;
                            search_match_endcol = regmatch.endpos[0].col + len_change;
                            highlight_match = TRUE;

                            update_topline();
                            validate_cursor();
                            update_screen(SOME_VALID);
                            highlight_match = FALSE;
                            redraw_later(SOME_VALID);

                            if (msg_row == Rows - 1)
                                msg_didout = FALSE;     /* avoid a scroll-up */
                            msg_starthere();
                            i = msg_scroll;
                            msg_scroll = 0;             /* truncate msg when
                                                           needed */
                            msg_no_more = TRUE;
                            /* write message same highlighting as for wait_return */
                            smsg_attr(hl_attr(HLF_R),
                                    (char_u *)"replace with %s (y/n/a/q/l/^E/^Y)?", sub);
                            msg_no_more = FALSE;
                            msg_scroll = i;
                            showruler(TRUE);
                            windgoto(msg_row, msg_col);
                            RedrawingDisabled = temp;

#if defined(USE_ON_FLY_SCROLL)
                            dont_scroll = FALSE; /* allow scrolling here */
#endif
                            ++no_mapping;       /* don't map this key */
                            ++allow_keys;       /* allow special keys */
                            typed = plain_vgetc();
                            --allow_keys;
                            --no_mapping;

                            /* clear the question */
                            msg_didout = FALSE; /* don't scroll up */
                            msg_col = 0;
                            gotocmdline(TRUE);

                            /* restore the line */
                            if (orig_line != NULL)
                                ml_replace(lnum, orig_line, FALSE);
                        }

                        need_wait_return = FALSE; /* no hit-return prompt */
                        if (typed == 'q' || typed == ESC || typed == Ctrl_C || typed == intr_char)
                        {
                            got_quit = TRUE;
                            break;
                        }
                        if (typed == 'n')
                            break;
                        if (typed == 'y')
                            break;
                        if (typed == 'l')
                        {
                            /* last: replace and then stop */
                            do_all = FALSE;
                            line2 = lnum;
                            break;
                        }
                        if (typed == 'a')
                        {
                            do_ask = FALSE;
                            break;
                        }
                    }
                    State = save_State;
                    setmouse();
                    if (vim_strchr(p_cpo, CPO_UNDO) != NULL)
                        --no_u_sync;

                    if (typed == 'n')
                    {
                        /* For a multi-line match, put matchcol at the NUL at
                         * the end of the line and set nmatch to one, so that
                         * we continue looking for a match on the next line.
                         * Avoids that ":%s/\nB\@=//gc" and ":%s/\n/,\r/gc"
                         * get stuck when pressing 'n'. */
                        if (nmatch > 1)
                        {
                            matchcol = (colnr_T)STRLEN(sub_firstline);
                            skip_match = TRUE;
                        }
                        goto skip;
                    }
                    if (got_quit)
                        goto skip;
                }

                /* Move the cursor to the start of the match, so that we can
                 * use "\=col("."). */
                curwin->w_cursor.col = regmatch.startpos[0].col;

                /*
                 * 3. substitute the string.
                 */
                if (do_count)
                {
                    /* prevent accidentally changing the buffer by a function */
                    save_ma = curbuf->b_p_ma;
                    curbuf->b_p_ma = FALSE;
                    sandbox++;
                }
                /* get length of substitution part */
                sublen = vim_regsub_multi(&regmatch,
                                    sub_firstlnum - regmatch.startpos[0].lnum,
                                    sub, sub_firstline, FALSE, p_magic, TRUE);
                if (do_count)
                {
                    curbuf->b_p_ma = save_ma;
                    sandbox--;
                    goto skip;
                }

                /* When the match included the "$" of the last line it may
                 * go beyond the last line of the buffer. */
                if (nmatch > curbuf->b_ml.ml_line_count - sub_firstlnum + 1)
                {
                    nmatch = curbuf->b_ml.ml_line_count - sub_firstlnum + 1;
                    skip_match = TRUE;
                }

                /* Need room for:
                 * - result so far in new_start (not for first sub in line)
                 * - original text up to match
                 * - length of substituted part
                 * - original text after match
                 */
                if (nmatch == 1)
                    p1 = sub_firstline;
                else
                {
                    p1 = ml_get(sub_firstlnum + nmatch - 1);
                    nmatch_tl += nmatch - 1;
                }
                copy_len = regmatch.startpos[0].col - copycol;
                needed_len = copy_len + ((unsigned)STRLEN(p1) - regmatch.endpos[0].col) + sublen + 1;
                if (new_start == NULL)
                {
                    /*
                     * Get some space for a temporary buffer to do the
                     * substitution into (and some extra space to avoid
                     * too many calls to alloc()/free()).
                     */
                    new_start_len = needed_len + 50;
                    if ((new_start = alloc_check(new_start_len)) == NULL)
                        goto outofmem;
                    *new_start = NUL;
                    new_end = new_start;
                }
                else
                {
                    /*
                     * Check if the temporary buffer is long enough to do the
                     * substitution into.  If not, make it larger (with a bit
                     * extra to avoid too many calls to alloc()/free()).
                     */
                    len = (unsigned)STRLEN(new_start);
                    needed_len += len;
                    if (needed_len > (int)new_start_len)
                    {
                        new_start_len = needed_len + 50;
                        if ((p1 = alloc_check(new_start_len)) == NULL)
                        {
                            vim_free(new_start);
                            goto outofmem;
                        }
                        mch_memmove(p1, new_start, (size_t)(len + 1));
                        vim_free(new_start);
                        new_start = p1;
                    }
                    new_end = new_start + len;
                }

                /*
                 * copy the text up to the part that matched
                 */
                mch_memmove(new_end, sub_firstline + copycol, (size_t)copy_len);
                new_end += copy_len;

                (void)vim_regsub_multi(&regmatch,
                                    sub_firstlnum - regmatch.startpos[0].lnum,
                                           sub, new_end, TRUE, p_magic, TRUE);
                sub_nsubs++;
                did_sub = TRUE;

                /* Move the cursor to the start of the line, to avoid that it
                 * is beyond the end of the line after the substitution. */
                curwin->w_cursor.col = 0;

                /* For a multi-line match, make a copy of the last matched
                 * line and continue in that one. */
                if (nmatch > 1)
                {
                    sub_firstlnum += nmatch - 1;
                    vim_free(sub_firstline);
                    sub_firstline = vim_strsave(ml_get(sub_firstlnum));
                    /* When going beyond the last line, stop substituting. */
                    if (sub_firstlnum <= line2)
                        do_again = TRUE;
                    else
                        do_all = FALSE;
                }

                /* Remember next character to be copied. */
                copycol = regmatch.endpos[0].col;

                if (skip_match)
                {
                    /* Already hit end of the buffer, sub_firstlnum is one
                     * less than what it ought to be. */
                    vim_free(sub_firstline);
                    sub_firstline = vim_strsave((char_u *)"");
                    copycol = 0;
                }

                /*
                 * Now the trick is to replace CTRL-M chars with a real line
                 * break.  This would make it impossible to insert a CTRL-M in
                 * the text.  The line break can be avoided by preceding the
                 * CTRL-M with a backslash.  To be able to insert a backslash,
                 * they must be doubled in the string and are halved here.
                 * That is Vi compatible.
                 */
                for (p1 = new_end; *p1; ++p1)
                {
                    if (p1[0] == '\\' && p1[1] != NUL)  /* remove backslash */
                        STRMOVE(p1, p1 + 1);
                    else if (*p1 == CAR)
                    {
                        if (u_inssub(lnum) == OK)   /* prepare for undo */
                        {
                            *p1 = NUL;              /* truncate up to the CR */
                            ml_append(lnum - 1, new_start, (colnr_T)(p1 - new_start + 1), FALSE);
                            mark_adjust(lnum + 1, (linenr_T)MAXLNUM, 1L, 0L);
                            if (do_ask)
                                appended_lines(lnum - 1, 1L);
                            else
                            {
                                if (first_line == 0)
                                    first_line = lnum;
                                last_line = lnum + 1;
                            }
                            /* All line numbers increase. */
                            ++sub_firstlnum;
                            ++lnum;
                            ++line2;
                            /* move the cursor to the new line, like Vi */
                            ++curwin->w_cursor.lnum;
                            /* copy the rest */
                            STRMOVE(new_start, p1 + 1);
                            p1 = new_start - 1;
                        }
                    }
                    else
                        p1 += utfc_ptr2len(p1) - 1;
                }

                /*
                 * 4. If do_all is set, find next match.
                 * Prevent endless loop with patterns that match empty
                 * strings, e.g. :s/$/pat/g or :s/[a-z]* /(&)/g.
                 * But ":s/\n/#/" is OK.
                 */
skip:
                /* We already know that we did the last subst when we are at
                 * the end of the line, except that a pattern like
                 * "bar\|\nfoo" may match at the NUL.  "lnum" can be below
                 * "line2" when there is a \zs in the pattern after a line break. */
                lastone = (skip_match
                        || got_int
                        || got_quit
                        || lnum > line2
                        || !(do_all || do_again)
                        || (sub_firstline[matchcol] == NUL && nmatch <= 1
                                         && !re_multiline(regmatch.regprog)));
                nmatch = -1;

                /*
                 * Replace the line in the buffer when needed.  This is
                 * skipped when there are more matches.
                 * The check for nmatch_tl is needed for when multi-line
                 * matching must replace the lines before trying to do another
                 * match, otherwise "\@<=" won't work.
                 * When the match starts below where we start searching also
                 * need to replace the line first (using \zs after \n).
                 */
                if (lastone
                        || nmatch_tl > 0
                        || (nmatch = vim_regexec_multi(&regmatch, curwin,
                                                        curbuf, sub_firstlnum,
                                                         matchcol, NULL)) == 0
                        || regmatch.startpos[0].lnum > 0)
                {
                    if (new_start != NULL)
                    {
                        /*
                         * Copy the rest of the line, that didn't match.
                         * "matchcol" has to be adjusted, we use the end of
                         * the line as reference, because the substitute may
                         * have changed the number of characters.  Same for
                         * "prev_matchcol".
                         */
                        STRCAT(new_start, sub_firstline + copycol);
                        matchcol = (colnr_T)STRLEN(sub_firstline) - matchcol;
                        prev_matchcol = (colnr_T)STRLEN(sub_firstline) - prev_matchcol;

                        if (u_savesub(lnum) != OK)
                            break;
                        ml_replace(lnum, new_start, TRUE);

                        if (nmatch_tl > 0)
                        {
                            /*
                             * Matched lines have now been substituted and are
                             * useless, delete them.  The part after the match
                             * has been appended to new_start, we don't need
                             * it in the buffer.
                             */
                            ++lnum;
                            if (u_savedel(lnum, nmatch_tl) != OK)
                                break;
                            for (i = 0; i < nmatch_tl; ++i)
                                ml_delete(lnum, (int)FALSE);
                            mark_adjust(lnum, lnum + nmatch_tl - 1, (long)MAXLNUM, -nmatch_tl);
                            if (do_ask)
                                deleted_lines(lnum, nmatch_tl);
                            --lnum;
                            line2 -= nmatch_tl; /* nr of lines decreases */
                            nmatch_tl = 0;
                        }

                        /* When asking, undo is saved each time, must also set
                         * changed flag each time. */
                        if (do_ask)
                            changed_bytes(lnum, 0);
                        else
                        {
                            if (first_line == 0)
                                first_line = lnum;
                            last_line = lnum + 1;
                        }

                        sub_firstlnum = lnum;
                        vim_free(sub_firstline);    /* free the temp buffer */
                        sub_firstline = new_start;
                        new_start = NULL;
                        matchcol = (colnr_T)STRLEN(sub_firstline) - matchcol;
                        prev_matchcol = (colnr_T)STRLEN(sub_firstline) - prev_matchcol;
                        copycol = 0;
                    }
                    if (nmatch == -1 && !lastone)
                        nmatch = vim_regexec_multi(&regmatch, curwin, curbuf,
                                               sub_firstlnum, matchcol, NULL);

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
                ++sub_nlines;
            vim_free(new_start);        /* for when substitute was cancelled */
            vim_free(sub_firstline);    /* free the copy of the original line */
            sub_firstline = NULL;
        }

        line_breakcheck();
    }

    if (first_line != 0)
    {
        /* Need to subtract the number of added lines from "last_line" to get
         * the line number before the change (same as adding the number of deleted lines). */
        i = curbuf->b_ml.ml_line_count - old_line_count;
        changed_lines(first_line, 0, last_line - i, i);
    }

outofmem:
    vim_free(sub_firstline); /* may have to free allocated copy of the line */

    /* ":s/pat//n" doesn't move the cursor */
    if (do_count)
        curwin->w_cursor = old_cursor;

    if (sub_nsubs > start_nsubs)
    {
        /* Set the '[ and '] marks. */
        curbuf->b_op_start.lnum = eap->line1;
        curbuf->b_op_end.lnum = line2;
        curbuf->b_op_start.col = curbuf->b_op_end.col = 0;

        if (!global_busy)
        {
            if (!do_ask)  /* when interactive leave cursor on the match */
            {
                if (endcolumn)
                    coladvance((colnr_T)MAXCOL);
                else
                    beginline(BL_WHITE | BL_FIX);
            }
            if (!do_sub_msg(do_count) && do_ask)
                MSG("");
        }
        else
            global_need_beginline = TRUE;
        if (do_print)
            print_line(curwin->w_cursor.lnum, do_number, do_list);
    }
    else if (!global_busy)
    {
        if (got_int)            /* interrupted */
            EMSG((char *)e_interr);
        else if (got_match)     /* did find something but nothing substituted */
            MSG("");
        else if (do_error)      /* nothing found */
            EMSG2((char *)e_patnotf2, get_search_pat());
    }

    vim_regfree(regmatch.regprog);
}

/*
 * Give message for number of substitutions.
 * Can also be used after a ":global" command.
 * Return TRUE if a message was given.
 */
    int
do_sub_msg(count_only)
    int     count_only;         /* used 'n' flag for ":s" */
{
    /*
     * Only report substitutions when:
     * - more than 'report' substitutions
     * - command was typed by user, or number of changed lines > 'report'
     * - giving messages is not disabled by 'lazyredraw'
     */
    if (((sub_nsubs > p_report && (KeyTyped || sub_nlines > 1 || p_report < 1)) || count_only)
            && messaging())
    {
        if (got_int)
            STRCPY(msg_buf, "(Interrupted) ");
        else
            *msg_buf = NUL;
        if (sub_nsubs == 1)
            vim_snprintf_add((char *)msg_buf, sizeof(msg_buf),
                    "%s", count_only ? "1 match" : "1 substitution");
        else
            vim_snprintf_add((char *)msg_buf, sizeof(msg_buf),
                    count_only ? "%ld matches" : "%ld substitutions", sub_nsubs);
        if (sub_nlines == 1)
            vim_snprintf_add((char *)msg_buf, sizeof(msg_buf), "%s", " on 1 line");
        else
            vim_snprintf_add((char *)msg_buf, sizeof(msg_buf), " on %ld lines", (long)sub_nlines);
        if (msg(msg_buf))
            /* save message to display it after redraw */
            set_keep_msg(msg_buf, 0);
        return TRUE;
    }
    if (got_int)
    {
        EMSG((char *)e_interr);
        return TRUE;
    }
    return FALSE;
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
 * set a mark for each line that (not) matches. Secondly we execute the command
 * for each line that has a mark. This is required because after deleting
 * lines we do not know where to search for the next match.
 */
    void
ex_global(eap)
    exarg_T     *eap;
{
    linenr_T    lnum;           /* line number according to old situation */
    int         ndone = 0;
    int         type;           /* first char of cmd: 'v' or 'g' */
    char_u      *cmd;           /* command argument */

    char_u      delim;          /* delimiter, normally '/' */
    char_u      *pat;
    regmmatch_T regmatch;
    int         match;
    int         which_pat;

    if (global_busy)
    {
        EMSG("E147: Cannot do :global recursive");   /* will increment global_busy */
        return;
    }

    if (eap->forceit)               /* ":global!" is like ":vglobal" */
        type = 'v';
    else
        type = *eap->cmd;
    cmd = eap->arg;
    which_pat = RE_LAST;            /* default: use last used regexp */

    /*
     * undocumented vi feature:
     *  "\/" and "\?": use previous search pattern.
     *           "\&": use previous substitute pattern.
     */
    if (*cmd == '\\')
    {
        ++cmd;
        if (vim_strchr((char_u *)"/?&", *cmd) == NULL)
        {
            EMSG((char *)e_backslash);
            return;
        }
        if (*cmd == '&')
            which_pat = RE_SUBST;       /* use previous substitute pattern */
        else
            which_pat = RE_SEARCH;      /* use previous search pattern */
        ++cmd;
        pat = (char_u *)"";
    }
    else if (*cmd == NUL)
    {
        EMSG("E148: Regular expression missing from global");
        return;
    }
    else
    {
        delim = *cmd;           /* get the delimiter */
        if (delim)
            ++cmd;              /* skip delimiter if there is one */
        pat = cmd;              /* remember start of pattern */
        cmd = skip_regexp(cmd, delim, p_magic, &eap->arg);
        if (cmd[0] == delim)                /* end delimiter found */
            *cmd++ = NUL;                   /* replace it with a NUL */
    }

    if (search_regcomp(pat, RE_BOTH, which_pat, SEARCH_HIS, &regmatch) == FAIL)
    {
        EMSG((char *)e_invcmd);
        return;
    }

    /*
     * pass 1: set marks for each (not) matching line
     */
    for (lnum = eap->line1; lnum <= eap->line2 && !got_int; ++lnum)
    {
        /* a match on this line? */
        match = vim_regexec_multi(&regmatch, curwin, curbuf, lnum, (colnr_T)0, NULL);
        if ((type == 'g' && match) || (type == 'v' && !match))
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
        MSG((char *)e_interr);
    else if (ndone == 0)
    {
        if (type == 'v')
            smsg((char_u *)"Pattern found in every line: %s", pat);
        else
            smsg((char_u *)"Pattern not found: %s", pat);
    }
    else
    {
        start_global_changes();
        global_exe(cmd);
        end_global_changes();
    }

    ml_clearmarked();      /* clear rest of the marks */
    vim_regfree(regmatch.regprog);
}

/*
 * Execute "cmd" on lines marked with ml_setmarked().
 */
    void
global_exe(cmd)
    char_u      *cmd;
{
    linenr_T old_lcount;        /* b_ml.ml_line_count before the command */
    buf_T    *old_buf = curbuf; /* remember what buffer we started in */
    linenr_T lnum;              /* line number according to old situation */

    /*
     * Set current position only once for a global command.
     * If global_busy is set, setpcmark() will not do anything.
     * If there is an error, global_busy will be incremented.
     */
    setpcmark();

    /* When the command writes a message, don't overwrite the command. */
    msg_didout = TRUE;

    sub_nsubs = 0;
    sub_nlines = 0;
    global_need_beginline = FALSE;
    global_busy = 1;
    old_lcount = curbuf->b_ml.ml_line_count;
    while (!got_int && (lnum = ml_firstmarked()) != 0 && global_busy == 1)
    {
        curwin->w_cursor.lnum = lnum;
        curwin->w_cursor.col = 0;
        if (*cmd == NUL || *cmd == '\n')
            do_cmdline((char_u *)"p", NULL, NULL, DOCMD_NOWAIT);
        else
            do_cmdline(cmd, NULL, NULL, DOCMD_NOWAIT);
        ui_breakcheck();
    }

    global_busy = 0;
    if (global_need_beginline)
        beginline(BL_WHITE | BL_FIX);
    else
        check_cursor(); /* cursor may be beyond the end of the line */

    /* the cursor may not have moved in the text but a change in a previous
     * line may move it on the screen */
    changed_line_abv_curs();

    /* If it looks like no message was written, allow overwriting the
     * command with the report for number of changes. */
    if (msg_col == 0 && msg_scrolled == 0)
        msg_didout = FALSE;

    /* If substitutes done, report number of substitutes, otherwise report
     * number of extra or deleted lines.
     * Don't report extra or deleted lines in the edge case where the buffer
     * we are in after execution is different from the buffer we started in. */
    if (!do_sub_msg(FALSE) && curbuf == old_buf)
        msgmore(curbuf->b_ml.ml_line_count - old_lcount);
}
