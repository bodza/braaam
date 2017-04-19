/*
 * mark.c: functions for setting marks and jumping to them
 */

#include "vim.h"

/*
 * This file contains routines to maintain and manipulate marks.
 */

/*
 * If a named file mark's lnum is non-zero, it is valid.
 * If a named file mark's fnum is non-zero, it is for an existing buffer,
 * otherwise it is from .viminfo and namedfm[n].fname is the file name.
 * There are marks 'A - 'Z (set by user) and '0 to '9 (set when writing
 * viminfo).
 */
#define EXTRA_MARKS 10                                  /* marks 0-9 */
static xfmark_T namedfm[NMARKS + EXTRA_MARKS];          /* marks with file nr */

static void fname2fnum(xfmark_T *fm);
static void fmarks_check_one(xfmark_T *fm, char_u *name, buf_T *buf);
static char_u *mark_line(pos_T *mp, int lead_len);
static void show_one_mark(int, char_u *, pos_T *, char_u *, int current);
#if defined(FEAT_JUMPLIST)
static void cleanup_jumplist(void);
#endif

/*
 * Set named mark "c" at current cursor position.
 * Returns OK on success, FAIL if bad name given.
 */
    int
setmark(c)
    int         c;
{
    return setmark_pos(c, &curwin->w_cursor, curbuf->b_fnum);
}

/*
 * Set named mark "c" to position "pos".
 * When "c" is upper case use file "fnum".
 * Returns OK on success, FAIL if bad name given.
 */
    int
setmark_pos(c, pos, fnum)
    int         c;
    pos_T       *pos;
    int         fnum;
{
    int         i;

    /* Check for a special key (may cause islower() to crash). */
    if (c < 0)
        return FAIL;

    if (c == '\'' || c == '`')
    {
        if (pos == &curwin->w_cursor)
        {
            setpcmark();
            /* keep it even when the cursor doesn't move */
            curwin->w_prev_pcmark = curwin->w_pcmark;
        }
        else
            curwin->w_pcmark = *pos;
        return OK;
    }

    if (c == '"')
    {
        curbuf->b_last_cursor = *pos;
        return OK;
    }

    /* Allow setting '[ and '] for an autocommand that simulates reading a
     * file. */
    if (c == '[')
    {
        curbuf->b_op_start = *pos;
        return OK;
    }
    if (c == ']')
    {
        curbuf->b_op_end = *pos;
        return OK;
    }

    if (c == '<' || c == '>')
    {
        if (c == '<')
            curbuf->b_visual.vi_start = *pos;
        else
            curbuf->b_visual.vi_end = *pos;
        if (curbuf->b_visual.vi_mode == NUL)
            /* Visual_mode has not yet been set, use a sane default. */
            curbuf->b_visual.vi_mode = 'v';
        return OK;
    }

    if (c > 'z')            /* some islower() and isupper() cannot handle characters above 127 */
        return FAIL;
    if (islower(c))
    {
        i = c - 'a';
        curbuf->b_namedm[i] = *pos;
        return OK;
    }
    if (isupper(c))
    {
        i = c - 'A';
        namedfm[i].fmark.mark = *pos;
        namedfm[i].fmark.fnum = fnum;
        vim_free(namedfm[i].fname);
        namedfm[i].fname = NULL;
        return OK;
    }
    return FAIL;
}

/*
 * Set the previous context mark to the current position and add it to the
 * jump list.
 */
    void
setpcmark()
{
#if defined(FEAT_JUMPLIST)
    int         i;
    xfmark_T    *fm;
#endif
#if defined(JUMPLIST_ROTATE)
    xfmark_T    tempmark;
#endif

    /* for :global the mark is set only once */
    if (global_busy || listcmd_busy || cmdmod.keepjumps)
        return;

    curwin->w_prev_pcmark = curwin->w_pcmark;
    curwin->w_pcmark = curwin->w_cursor;

#if defined(FEAT_JUMPLIST)
#if defined(JUMPLIST_ROTATE)
    /*
     * If last used entry is not at the top, put it at the top by rotating
     * the stack until it is (the newer entries will be at the bottom).
     * Keep one entry (the last used one) at the top.
     */
    if (curwin->w_jumplistidx < curwin->w_jumplistlen)
        ++curwin->w_jumplistidx;
    while (curwin->w_jumplistidx < curwin->w_jumplistlen)
    {
        tempmark = curwin->w_jumplist[curwin->w_jumplistlen - 1];
        for (i = curwin->w_jumplistlen - 1; i > 0; --i)
            curwin->w_jumplist[i] = curwin->w_jumplist[i - 1];
        curwin->w_jumplist[0] = tempmark;
        ++curwin->w_jumplistidx;
    }
#endif

    /* If jumplist is full: remove oldest entry */
    if (++curwin->w_jumplistlen > JUMPLISTSIZE)
    {
        curwin->w_jumplistlen = JUMPLISTSIZE;
        vim_free(curwin->w_jumplist[0].fname);
        for (i = 1; i < JUMPLISTSIZE; ++i)
            curwin->w_jumplist[i - 1] = curwin->w_jumplist[i];
    }
    curwin->w_jumplistidx = curwin->w_jumplistlen;
    fm = &curwin->w_jumplist[curwin->w_jumplistlen - 1];

    fm->fmark.mark = curwin->w_pcmark;
    fm->fmark.fnum = curbuf->b_fnum;
    fm->fname = NULL;
#endif
}

/*
 * To change context, call setpcmark(), then move the current position to
 * where ever, then call checkpcmark().  This ensures that the previous
 * context will only be changed if the cursor moved to a different line.
 * If pcmark was deleted (with "dG") the previous mark is restored.
 */
    void
checkpcmark()
{
    if (curwin->w_prev_pcmark.lnum != 0
            && (equalpos(curwin->w_pcmark, curwin->w_cursor)
                || curwin->w_pcmark.lnum == 0))
    {
        curwin->w_pcmark = curwin->w_prev_pcmark;
        curwin->w_prev_pcmark.lnum = 0;         /* Show it has been checked */
    }
}

#if defined(FEAT_JUMPLIST)
/*
 * move "count" positions in the jump list (count may be negative)
 */
    pos_T *
movemark(count)
    int count;
{
    pos_T       *pos;
    xfmark_T    *jmp;

    cleanup_jumplist();

    if (curwin->w_jumplistlen == 0)         /* nothing to jump to */
        return (pos_T *)NULL;

    for (;;)
    {
        if (curwin->w_jumplistidx + count < 0
                || curwin->w_jumplistidx + count >= curwin->w_jumplistlen)
            return (pos_T *)NULL;

        /*
         * if first CTRL-O or CTRL-I command after a jump, add cursor position
         * to list.  Careful: If there are duplicates (CTRL-O immediately after
         * starting Vim on a file), another entry may have been removed.
         */
        if (curwin->w_jumplistidx == curwin->w_jumplistlen)
        {
            setpcmark();
            --curwin->w_jumplistidx;    /* skip the new entry */
            if (curwin->w_jumplistidx + count < 0)
                return (pos_T *)NULL;
        }

        curwin->w_jumplistidx += count;

        jmp = curwin->w_jumplist + curwin->w_jumplistidx;
        if (jmp->fmark.fnum == 0)
            fname2fnum(jmp);
        if (jmp->fmark.fnum != curbuf->b_fnum)
        {
            /* jump to other file */
            if (buflist_findnr(jmp->fmark.fnum) == NULL)
            {                                        /* Skip this one .. */
                count += count < 0 ? -1 : 1;
                continue;
            }
            if (buflist_getfile(jmp->fmark.fnum, jmp->fmark.mark.lnum,
                                                            0, FALSE) == FAIL)
                return (pos_T *)NULL;
            /* Set lnum again, autocommands my have changed it */
            curwin->w_cursor = jmp->fmark.mark;
            pos = (pos_T *)-1;
        }
        else
            pos = &(jmp->fmark.mark);
        return pos;
    }
}

/*
 * Move "count" positions in the changelist (count may be negative).
 */
    pos_T *
movechangelist(count)
    int         count;
{
    int         n;

    if (curbuf->b_changelistlen == 0)       /* nothing to jump to */
        return (pos_T *)NULL;

    n = curwin->w_changelistidx;
    if (n + count < 0)
    {
        if (n == 0)
            return (pos_T *)NULL;
        n = 0;
    }
    else if (n + count >= curbuf->b_changelistlen)
    {
        if (n == curbuf->b_changelistlen - 1)
            return (pos_T *)NULL;
        n = curbuf->b_changelistlen - 1;
    }
    else
        n += count;
    curwin->w_changelistidx = n;
    return curbuf->b_changelist + n;
}
#endif

/*
 * Find mark "c" in buffer pointed to by "buf".
 * If "changefile" is TRUE it's allowed to edit another file for '0, 'A, etc.
 * If "fnum" is not NULL store the fnum there for '0, 'A etc., don't edit
 * another file.
 * Returns:
 * - pointer to pos_T if found.  lnum is 0 when mark not set, -1 when mark is
 *   in another file which can't be gotten. (caller needs to check lnum!)
 * - NULL if there is no mark called 'c'.
 * - -1 if mark is in other file and jumped there (only if changefile is TRUE)
 */
    pos_T *
getmark_buf(buf, c, changefile)
    buf_T       *buf;
    int         c;
    int         changefile;
{
    return getmark_buf_fnum(buf, c, changefile, NULL);
}

    pos_T *
getmark(c, changefile)
    int         c;
    int         changefile;
{
    return getmark_buf_fnum(curbuf, c, changefile, NULL);
}

    pos_T *
getmark_buf_fnum(buf, c, changefile, fnum)
    buf_T       *buf;
    int         c;
    int         changefile;
    int         *fnum;
{
    pos_T               *posp;
    pos_T               *startp, *endp;
    static pos_T        pos_copy;

    posp = NULL;

    /* Check for special key, can't be a mark name and might cause islower()
     * to crash. */
    if (c < 0)
        return posp;
    if (c > '~')                        /* check for islower()/isupper() */
        ;
    else if (c == '\'' || c == '`')      /* previous context mark */
    {
        pos_copy = curwin->w_pcmark;    /* need to make a copy because */
        posp = &pos_copy;               /*   w_pcmark may be changed soon */
    }
    else if (c == '"')                  /* to pos when leaving buffer */
        posp = &(buf->b_last_cursor);
    else if (c == '^')                  /* to where Insert mode stopped */
        posp = &(buf->b_last_insert);
    else if (c == '.')                  /* to where last change was made */
        posp = &(buf->b_last_change);
    else if (c == '[')                  /* to start of previous operator */
        posp = &(buf->b_op_start);
    else if (c == ']')                  /* to end of previous operator */
        posp = &(buf->b_op_end);
    else if (c == '{' || c == '}')      /* to previous/next paragraph */
    {
        pos_T   pos;
        oparg_T oa;
        int     slcb = listcmd_busy;

        pos = curwin->w_cursor;
        listcmd_busy = TRUE;        /* avoid that '' is changed */
        if (findpar(&oa.inclusive,
                               c == '}' ? FORWARD : BACKWARD, 1L, NUL, FALSE))
        {
            pos_copy = curwin->w_cursor;
            posp = &pos_copy;
        }
        curwin->w_cursor = pos;
        listcmd_busy = slcb;
    }
    else if (c == '(' || c == ')')      /* to previous/next sentence */
    {
        pos_T   pos;
        int     slcb = listcmd_busy;

        pos = curwin->w_cursor;
        listcmd_busy = TRUE;        /* avoid that '' is changed */
        if (findsent(c == ')' ? FORWARD : BACKWARD, 1L))
        {
            pos_copy = curwin->w_cursor;
            posp = &pos_copy;
        }
        curwin->w_cursor = pos;
        listcmd_busy = slcb;
    }
    else if (c == '<' || c == '>')      /* start/end of visual area */
    {
        startp = &buf->b_visual.vi_start;
        endp = &buf->b_visual.vi_end;
        if ((c == '<') == lt(*startp, *endp))
            posp = startp;
        else
            posp = endp;
        /*
         * For Visual line mode, set mark at begin or end of line
         */
        if (buf->b_visual.vi_mode == 'V')
        {
            pos_copy = *posp;
            posp = &pos_copy;
            if (c == '<')
                pos_copy.col = 0;
            else
                pos_copy.col = MAXCOL;
#if defined(FEAT_VIRTUALEDIT)
            pos_copy.coladd = 0;
#endif
        }
    }
    else if (ASCII_ISLOWER(c))          /* normal named mark */
    {
        posp = &(buf->b_namedm[c - 'a']);
    }
    else if (ASCII_ISUPPER(c) || VIM_ISDIGIT(c))        /* named file mark */
    {
        if (VIM_ISDIGIT(c))
            c = c - '0' + NMARKS;
        else
            c -= 'A';
        posp = &(namedfm[c].fmark.mark);

        if (namedfm[c].fmark.fnum == 0)
            fname2fnum(&namedfm[c]);

        if (fnum != NULL)
            *fnum = namedfm[c].fmark.fnum;
        else if (namedfm[c].fmark.fnum != buf->b_fnum)
        {
            /* mark is in another file */
            posp = &pos_copy;

            if (namedfm[c].fmark.mark.lnum != 0
                                       && changefile && namedfm[c].fmark.fnum)
            {
                if (buflist_getfile(namedfm[c].fmark.fnum,
                                      (linenr_T)1, GETF_SETMARK, FALSE) == OK)
                {
                    /* Set the lnum now, autocommands could have changed it */
                    curwin->w_cursor = namedfm[c].fmark.mark;
                    return (pos_T *)-1;
                }
                pos_copy.lnum = -1;     /* can't get file */
            }
            else
                pos_copy.lnum = 0;      /* mark exists, but is not valid in
                                           current buffer */
        }
    }

    return posp;
}

/*
 * Search for the next named mark in the current file.
 *
 * Returns pointer to pos_T of the next mark or NULL if no mark is found.
 */
    pos_T *
getnextmark(startpos, dir, begin_line)
    pos_T       *startpos;      /* where to start */
    int         dir;    /* direction for search */
    int         begin_line;
{
    int         i;
    pos_T       *result = NULL;
    pos_T       pos;

    pos = *startpos;

    /* When searching backward and leaving the cursor on the first non-blank,
     * position must be in a previous line.
     * When searching forward and leaving the cursor on the first non-blank,
     * position must be in a next line. */
    if (dir == BACKWARD && begin_line)
        pos.col = 0;
    else if (dir == FORWARD && begin_line)
        pos.col = MAXCOL;

    for (i = 0; i < NMARKS; i++)
    {
        if (curbuf->b_namedm[i].lnum > 0)
        {
            if (dir == FORWARD)
            {
                if ((result == NULL || lt(curbuf->b_namedm[i], *result))
                        && lt(pos, curbuf->b_namedm[i]))
                    result = &curbuf->b_namedm[i];
            }
            else
            {
                if ((result == NULL || lt(*result, curbuf->b_namedm[i]))
                        && lt(curbuf->b_namedm[i], pos))
                    result = &curbuf->b_namedm[i];
            }
        }
    }

    return result;
}

/*
 * For an xtended filemark: set the fnum from the fname.
 * This is used for marks obtained from the .viminfo file.  It's postponed
 * until the mark is used to avoid a long startup delay.
 */
    static void
fname2fnum(fm)
    xfmark_T    *fm;
{
    char_u      *p;

    if (fm->fname != NULL)
    {
        /*
         * First expand "~/" in the file name to the home directory.
         * Don't expand the whole name, it may contain other '~' chars.
         */
        if (fm->fname[0] == '~' && (fm->fname[1] == '/'))
        {
            int len;

            expand_env((char_u *)"~/", NameBuff, MAXPATHL);
            len = (int)STRLEN(NameBuff);
            vim_strncpy(NameBuff + len, fm->fname + 2, MAXPATHL - len - 1);
        }
        else
            vim_strncpy(NameBuff, fm->fname, MAXPATHL - 1);

        /* Try to shorten the file name. */
        mch_dirname(IObuff, IOSIZE);
        p = shorten_fname(NameBuff, IObuff);

        /* buflist_new() will call fmarks_check_names() */
        (void)buflist_new(NameBuff, p, (linenr_T)1, 0);
    }
}

/*
 * Check all file marks for a name that matches the file name in buf.
 * May replace the name with an fnum.
 * Used for marks that come from the .viminfo file.
 */
    void
fmarks_check_names(buf)
    buf_T       *buf;
{
    char_u      *name;
    int         i;
#if defined(FEAT_JUMPLIST)
    win_T       *wp;
#endif

    if (buf->b_ffname == NULL)
        return;

    name = home_replace_save(buf, buf->b_ffname);
    if (name == NULL)
        return;

    for (i = 0; i < NMARKS + EXTRA_MARKS; ++i)
        fmarks_check_one(&namedfm[i], name, buf);

#if defined(FEAT_JUMPLIST)
    FOR_ALL_WINDOWS(wp)
    {
        for (i = 0; i < wp->w_jumplistlen; ++i)
            fmarks_check_one(&wp->w_jumplist[i], name, buf);
    }
#endif

    vim_free(name);
}

    static void
fmarks_check_one(fm, name, buf)
    xfmark_T    *fm;
    char_u      *name;
    buf_T       *buf;
{
    if (fm->fmark.fnum == 0
            && fm->fname != NULL
            && fnamecmp(name, fm->fname) == 0)
    {
        fm->fmark.fnum = buf->b_fnum;
        vim_free(fm->fname);
        fm->fname = NULL;
    }
}

/*
 * Check a if a position from a mark is valid.
 * Give and error message and return FAIL if not.
 */
    int
check_mark(pos)
    pos_T    *pos;
{
    if (pos == NULL)
    {
        EMSG(_(e_umark));
        return FAIL;
    }
    if (pos->lnum <= 0)
    {
        /* lnum is negative if mark is in another file can can't get that
         * file, error message already give then. */
        if (pos->lnum == 0)
            EMSG(_(e_marknotset));
        return FAIL;
    }
    if (pos->lnum > curbuf->b_ml.ml_line_count)
    {
        EMSG(_(e_markinval));
        return FAIL;
    }
    return OK;
}

/*
 * clrallmarks() - clear all marks in the buffer 'buf'
 *
 * Used mainly when trashing the entire buffer during ":e" type commands
 */
    void
clrallmarks(buf)
    buf_T       *buf;
{
    static int          i = -1;

    if (i == -1)        /* first call ever: initialize */
        for (i = 0; i < NMARKS + 1; i++)
        {
            namedfm[i].fmark.mark.lnum = 0;
            namedfm[i].fname = NULL;
        }

    for (i = 0; i < NMARKS; i++)
        buf->b_namedm[i].lnum = 0;
    buf->b_op_start.lnum = 0;           /* start/end op mark cleared */
    buf->b_op_end.lnum = 0;
    buf->b_last_cursor.lnum = 1;        /* '" mark cleared */
    buf->b_last_cursor.col = 0;
#if defined(FEAT_VIRTUALEDIT)
    buf->b_last_cursor.coladd = 0;
#endif
    buf->b_last_insert.lnum = 0;        /* '^ mark cleared */
    buf->b_last_change.lnum = 0;        /* '. mark cleared */
#if defined(FEAT_JUMPLIST)
    buf->b_changelistlen = 0;
#endif
}

/*
 * Get name of file from a filemark.
 * When it's in the current buffer, return the text at the mark.
 * Returns an allocated string.
 */
    char_u *
fm_getname(fmark, lead_len)
    fmark_T     *fmark;
    int         lead_len;
{
    if (fmark->fnum == curbuf->b_fnum)              /* current buffer */
        return mark_line(&(fmark->mark), lead_len);
    return buflist_nr2name(fmark->fnum, FALSE, TRUE);
}

/*
 * Return the line at mark "mp".  Truncate to fit in window.
 * The returned string has been allocated.
 */
    static char_u *
mark_line(mp, lead_len)
    pos_T       *mp;
    int         lead_len;
{
    char_u      *s, *p;
    int         len;

    if (mp->lnum == 0 || mp->lnum > curbuf->b_ml.ml_line_count)
        return vim_strsave((char_u *)"-invalid-");
    s = vim_strnsave(skipwhite(ml_get(mp->lnum)), (int)Columns);
    if (s == NULL)
        return NULL;
    /* Truncate the line to fit it in the window */
    len = 0;
    for (p = s; *p != NUL; mb_ptr_adv(p))
    {
        len += ptr2cells(p);
        if (len >= Columns - lead_len)
            break;
    }
    *p = NUL;
    return s;
}

/*
 * print the marks
 */
    void
do_marks(eap)
    exarg_T     *eap;
{
    char_u      *arg = eap->arg;
    int         i;
    char_u      *name;

    if (arg != NULL && *arg == NUL)
        arg = NULL;

    show_one_mark('\'', arg, &curwin->w_pcmark, NULL, TRUE);
    for (i = 0; i < NMARKS; ++i)
        show_one_mark(i + 'a', arg, &curbuf->b_namedm[i], NULL, TRUE);
    for (i = 0; i < NMARKS + EXTRA_MARKS; ++i)
    {
        if (namedfm[i].fmark.fnum != 0)
            name = fm_getname(&namedfm[i].fmark, 15);
        else
            name = namedfm[i].fname;
        if (name != NULL)
        {
            show_one_mark(i >= NMARKS ? i - NMARKS + '0' : i + 'A',
                    arg, &namedfm[i].fmark.mark, name,
                    namedfm[i].fmark.fnum == curbuf->b_fnum);
            if (namedfm[i].fmark.fnum != 0)
                vim_free(name);
        }
    }
    show_one_mark('"', arg, &curbuf->b_last_cursor, NULL, TRUE);
    show_one_mark('[', arg, &curbuf->b_op_start, NULL, TRUE);
    show_one_mark(']', arg, &curbuf->b_op_end, NULL, TRUE);
    show_one_mark('^', arg, &curbuf->b_last_insert, NULL, TRUE);
    show_one_mark('.', arg, &curbuf->b_last_change, NULL, TRUE);
    show_one_mark('<', arg, &curbuf->b_visual.vi_start, NULL, TRUE);
    show_one_mark('>', arg, &curbuf->b_visual.vi_end, NULL, TRUE);
    show_one_mark(-1, arg, NULL, NULL, FALSE);
}

    static void
show_one_mark(c, arg, p, name, current)
    int         c;
    char_u      *arg;
    pos_T       *p;
    char_u      *name;
    int         current;        /* in current file */
{
    static int  did_title = FALSE;
    int         mustfree = FALSE;

    if (c == -1)                            /* finish up */
    {
        if (did_title)
            did_title = FALSE;
        else
        {
            if (arg == NULL)
                MSG(_("No marks set"));
            else
                EMSG2(_("E283: No marks matching \"%s\""), arg);
        }
    }
    /* don't output anything if 'q' typed at --more-- prompt */
    else if (!got_int
            && (arg == NULL || vim_strchr(arg, c) != NULL)
            && p->lnum != 0)
    {
        if (!did_title)
        {
            /* Highlight title */
            MSG_PUTS_TITLE(_("\nmark line  col file/text"));
            did_title = TRUE;
        }
        msg_putchar('\n');
        if (!got_int)
        {
            sprintf((char *)IObuff, " %c %6ld %4d ", c, p->lnum, p->col);
            msg_outtrans(IObuff);
            if (name == NULL && current)
            {
                name = mark_line(p, 15);
                mustfree = TRUE;
            }
            if (name != NULL)
            {
                msg_outtrans_attr(name, current ? hl_attr(HLF_D) : 0);
                if (mustfree)
                    vim_free(name);
            }
        }
        out_flush();                /* show one line at a time */
    }
}

/*
 * ":delmarks[!] [marks]"
 */
    void
ex_delmarks(eap)
    exarg_T *eap;
{
    char_u      *p;
    int         from, to;
    int         i;
    int         lower;
    int         digit;
    int         n;

    if (*eap->arg == NUL && eap->forceit)
        /* clear all marks */
        clrallmarks(curbuf);
    else if (eap->forceit)
        EMSG(_(e_invarg));
    else if (*eap->arg == NUL)
        EMSG(_(e_argreq));
    else
    {
        /* clear specified marks only */
        for (p = eap->arg; *p != NUL; ++p)
        {
            lower = ASCII_ISLOWER(*p);
            digit = VIM_ISDIGIT(*p);
            if (lower || digit || ASCII_ISUPPER(*p))
            {
                if (p[1] == '-')
                {
                    /* clear range of marks */
                    from = *p;
                    to = p[2];
                    if (!(lower ? ASCII_ISLOWER(p[2])
                                : (digit ? VIM_ISDIGIT(p[2])
                                    : ASCII_ISUPPER(p[2])))
                            || to < from)
                    {
                        EMSG2(_(e_invarg2), p);
                        return;
                    }
                    p += 2;
                }
                else
                    /* clear one lower case mark */
                    from = to = *p;

                for (i = from; i <= to; ++i)
                {
                    if (lower)
                        curbuf->b_namedm[i - 'a'].lnum = 0;
                    else
                    {
                        if (digit)
                            n = i - '0' + NMARKS;
                        else
                            n = i - 'A';
                        namedfm[n].fmark.mark.lnum = 0;
                        vim_free(namedfm[n].fname);
                        namedfm[n].fname = NULL;
                    }
                }
            }
            else
                switch (*p)
                {
                    case '"': curbuf->b_last_cursor.lnum = 0; break;
                    case '^': curbuf->b_last_insert.lnum = 0; break;
                    case '.': curbuf->b_last_change.lnum = 0; break;
                    case '[': curbuf->b_op_start.lnum    = 0; break;
                    case ']': curbuf->b_op_end.lnum      = 0; break;
                    case '<': curbuf->b_visual.vi_start.lnum = 0; break;
                    case '>': curbuf->b_visual.vi_end.lnum   = 0; break;
                    case ' ': break;
                    default:  EMSG2(_(e_invarg2), p);
                              return;
                }
        }
    }
}

#if defined(FEAT_JUMPLIST)
/*
 * print the jumplist
 */
    void
ex_jumps(eap)
    exarg_T     *eap UNUSED;
{
    int         i;
    char_u      *name;

    cleanup_jumplist();
    /* Highlight title */
    MSG_PUTS_TITLE(_("\n jump line  col file/text"));
    for (i = 0; i < curwin->w_jumplistlen && !got_int; ++i)
    {
        if (curwin->w_jumplist[i].fmark.mark.lnum != 0)
        {
            if (curwin->w_jumplist[i].fmark.fnum == 0)
                fname2fnum(&curwin->w_jumplist[i]);
            name = fm_getname(&curwin->w_jumplist[i].fmark, 16);
            if (name == NULL)       /* file name not available */
                continue;

            msg_putchar('\n');
            if (got_int)
            {
                vim_free(name);
                break;
            }
            sprintf((char *)IObuff, "%c %2d %5ld %4d ",
                i == curwin->w_jumplistidx ? '>' : ' ',
                i > curwin->w_jumplistidx ? i - curwin->w_jumplistidx
                                          : curwin->w_jumplistidx - i,
                curwin->w_jumplist[i].fmark.mark.lnum,
                curwin->w_jumplist[i].fmark.mark.col);
            msg_outtrans(IObuff);
            msg_outtrans_attr(name,
                            curwin->w_jumplist[i].fmark.fnum == curbuf->b_fnum ? hl_attr(HLF_D) : 0);
            vim_free(name);
            ui_breakcheck();
        }
        out_flush();
    }
    if (curwin->w_jumplistidx == curwin->w_jumplistlen)
        MSG_PUTS("\n>");
}

/*
 * print the changelist
 */
    void
ex_changes(eap)
    exarg_T     *eap UNUSED;
{
    int         i;
    char_u      *name;

    /* Highlight title */
    MSG_PUTS_TITLE(_("\nchange line  col text"));

    for (i = 0; i < curbuf->b_changelistlen && !got_int; ++i)
    {
        if (curbuf->b_changelist[i].lnum != 0)
        {
            msg_putchar('\n');
            if (got_int)
                break;
            sprintf((char *)IObuff, "%c %3d %5ld %4d ",
                    i == curwin->w_changelistidx ? '>' : ' ',
                    i > curwin->w_changelistidx ? i - curwin->w_changelistidx
                                                : curwin->w_changelistidx - i,
                    (long)curbuf->b_changelist[i].lnum,
                    curbuf->b_changelist[i].col);
            msg_outtrans(IObuff);
            name = mark_line(&curbuf->b_changelist[i], 17);
            if (name == NULL)
                break;
            msg_outtrans_attr(name, hl_attr(HLF_D));
            vim_free(name);
            ui_breakcheck();
        }
        out_flush();
    }
    if (curwin->w_changelistidx == curbuf->b_changelistlen)
        MSG_PUTS("\n>");
}
#endif

#define one_adjust(add) \
    { \
        lp = add; \
        if (*lp >= line1 && *lp <= line2) \
        { \
            if (amount == MAXLNUM) \
                *lp = 0; \
            else \
                *lp += amount; \
        } \
        else if (amount_after && *lp > line2) \
            *lp += amount_after; \
    }

/* don't delete the line, just put at first deleted line */
#define one_adjust_nodel(add) \
    { \
        lp = add; \
        if (*lp >= line1 && *lp <= line2) \
        { \
            if (amount == MAXLNUM) \
                *lp = line1; \
            else \
                *lp += amount; \
        } \
        else if (amount_after && *lp > line2) \
            *lp += amount_after; \
    }

/*
 * Adjust marks between line1 and line2 (inclusive) to move 'amount' lines.
 * Must be called before changed_*(), appended_lines() or deleted_lines().
 * May be called before or after changing the text.
 * When deleting lines line1 to line2, use an 'amount' of MAXLNUM: The marks
 * within this range are made invalid.
 * If 'amount_after' is non-zero adjust marks after line2.
 * Example: Delete lines 34 and 35: mark_adjust(34, 35, MAXLNUM, -2);
 * Example: Insert two lines below 55: mark_adjust(56, MAXLNUM, 2, 0);
 *                                 or: mark_adjust(56, 55, MAXLNUM, 2);
 */
    void
mark_adjust(line1, line2, amount, amount_after)
    linenr_T    line1;
    linenr_T    line2;
    long        amount;
    long        amount_after;
{
    int         i;
    int         fnum = curbuf->b_fnum;
    linenr_T    *lp;
    win_T       *win;
    tabpage_T   *tab;
    static pos_T initpos = INIT_POS_T(1, 0, 0);

    if (line2 < line1 && amount_after == 0L)        /* nothing to do */
        return;

    if (!cmdmod.lockmarks)
    {
        /* named marks, lower case and upper case */
        for (i = 0; i < NMARKS; i++)
        {
            one_adjust(&(curbuf->b_namedm[i].lnum));
            if (namedfm[i].fmark.fnum == fnum)
                one_adjust_nodel(&(namedfm[i].fmark.mark.lnum));
        }
        for (i = NMARKS; i < NMARKS + EXTRA_MARKS; i++)
        {
            if (namedfm[i].fmark.fnum == fnum)
                one_adjust_nodel(&(namedfm[i].fmark.mark.lnum));
        }

        /* last Insert position */
        one_adjust(&(curbuf->b_last_insert.lnum));

        /* last change position */
        one_adjust(&(curbuf->b_last_change.lnum));

        /* last cursor position, if it was set */
        if (!equalpos(curbuf->b_last_cursor, initpos))
            one_adjust(&(curbuf->b_last_cursor.lnum));

#if defined(FEAT_JUMPLIST)
        /* list of change positions */
        for (i = 0; i < curbuf->b_changelistlen; ++i)
            one_adjust_nodel(&(curbuf->b_changelist[i].lnum));
#endif

        /* Visual area */
        one_adjust_nodel(&(curbuf->b_visual.vi_start.lnum));
        one_adjust_nodel(&(curbuf->b_visual.vi_end.lnum));

#if defined(FEAT_QUICKFIX)
        /* quickfix marks */
        qf_mark_adjust(NULL, line1, line2, amount, amount_after);
        /* location lists */
        FOR_ALL_TAB_WINDOWS(tab, win)
            qf_mark_adjust(win, line1, line2, amount, amount_after);
#endif
    }

    /* previous context mark */
    one_adjust(&(curwin->w_pcmark.lnum));

    /* previous pcmark */
    one_adjust(&(curwin->w_prev_pcmark.lnum));

    /* saved cursor for formatting */
    if (saved_cursor.lnum != 0)
        one_adjust_nodel(&(saved_cursor.lnum));

    /*
     * Adjust items in all windows related to the current buffer.
     */
    FOR_ALL_TAB_WINDOWS(tab, win)
    {
#if defined(FEAT_JUMPLIST)
        if (!cmdmod.lockmarks)
            /* Marks in the jumplist.  When deleting lines, this may create
             * duplicate marks in the jumplist, they will be removed later. */
            for (i = 0; i < win->w_jumplistlen; ++i)
                if (win->w_jumplist[i].fmark.fnum == fnum)
                    one_adjust_nodel(&(win->w_jumplist[i].fmark.mark.lnum));
#endif

        if (win->w_buffer == curbuf)
        {
            if (!cmdmod.lockmarks)
                /* marks in the tag stack */
                for (i = 0; i < win->w_tagstacklen; i++)
                    if (win->w_tagstack[i].fmark.fnum == fnum)
                        one_adjust_nodel(&(win->w_tagstack[i].fmark.mark.lnum));

            /* the displayed Visual area */
            if (win->w_old_cursor_lnum != 0)
            {
                one_adjust_nodel(&(win->w_old_cursor_lnum));
                one_adjust_nodel(&(win->w_old_visual_lnum));
            }

            /* topline and cursor position for windows with the same buffer
             * other than the current window */
            if (win != curwin)
            {
                if (win->w_topline >= line1 && win->w_topline <= line2)
                {
                    if (amount == MAXLNUM)          /* topline is deleted */
                    {
                        if (line1 <= 1)
                            win->w_topline = 1;
                        else
                            win->w_topline = line1 - 1;
                    }
                    else                /* keep topline on the same line */
                        win->w_topline += amount;
                }
                else if (amount_after && win->w_topline > line2)
                {
                    win->w_topline += amount_after;
                }
                if (win->w_cursor.lnum >= line1 && win->w_cursor.lnum <= line2)
                {
                    if (amount == MAXLNUM) /* line with cursor is deleted */
                    {
                        if (line1 <= 1)
                            win->w_cursor.lnum = 1;
                        else
                            win->w_cursor.lnum = line1 - 1;
                        win->w_cursor.col = 0;
                    }
                    else                /* keep cursor on the same line */
                        win->w_cursor.lnum += amount;
                }
                else if (amount_after && win->w_cursor.lnum > line2)
                    win->w_cursor.lnum += amount_after;
            }
        }
    }

}

/* This code is used often, needs to be fast. */
#define col_adjust(pp) \
    { \
        posp = pp; \
        if (posp->lnum == lnum && posp->col >= mincol) \
        { \
            posp->lnum += lnum_amount; \
            if (col_amount < 0 && posp->col <= (colnr_T)-col_amount) \
                posp->col = 0; \
            else \
                posp->col += col_amount; \
        } \
    }

/*
 * Adjust marks in line "lnum" at column "mincol" and further: add
 * "lnum_amount" to the line number and add "col_amount" to the column
 * position.
 */
    void
mark_col_adjust(lnum, mincol, lnum_amount, col_amount)
    linenr_T    lnum;
    colnr_T     mincol;
    long        lnum_amount;
    long        col_amount;
{
    int         i;
    int         fnum = curbuf->b_fnum;
    win_T       *win;
    pos_T       *posp;

    if ((col_amount == 0L && lnum_amount == 0L) || cmdmod.lockmarks)
        return; /* nothing to do */

    /* named marks, lower case and upper case */
    for (i = 0; i < NMARKS; i++)
    {
        col_adjust(&(curbuf->b_namedm[i]));
        if (namedfm[i].fmark.fnum == fnum)
            col_adjust(&(namedfm[i].fmark.mark));
    }
    for (i = NMARKS; i < NMARKS + EXTRA_MARKS; i++)
    {
        if (namedfm[i].fmark.fnum == fnum)
            col_adjust(&(namedfm[i].fmark.mark));
    }

    /* last Insert position */
    col_adjust(&(curbuf->b_last_insert));

    /* last change position */
    col_adjust(&(curbuf->b_last_change));

#if defined(FEAT_JUMPLIST)
    /* list of change positions */
    for (i = 0; i < curbuf->b_changelistlen; ++i)
        col_adjust(&(curbuf->b_changelist[i]));
#endif

    /* Visual area */
    col_adjust(&(curbuf->b_visual.vi_start));
    col_adjust(&(curbuf->b_visual.vi_end));

    /* previous context mark */
    col_adjust(&(curwin->w_pcmark));

    /* previous pcmark */
    col_adjust(&(curwin->w_prev_pcmark));

    /* saved cursor for formatting */
    col_adjust(&saved_cursor);

    /*
     * Adjust items in all windows related to the current buffer.
     */
    FOR_ALL_WINDOWS(win)
    {
#if defined(FEAT_JUMPLIST)
        /* marks in the jumplist */
        for (i = 0; i < win->w_jumplistlen; ++i)
            if (win->w_jumplist[i].fmark.fnum == fnum)
                col_adjust(&(win->w_jumplist[i].fmark.mark));
#endif

        if (win->w_buffer == curbuf)
        {
            /* marks in the tag stack */
            for (i = 0; i < win->w_tagstacklen; i++)
                if (win->w_tagstack[i].fmark.fnum == fnum)
                    col_adjust(&(win->w_tagstack[i].fmark.mark));

            /* cursor position for other windows with the same buffer */
            if (win != curwin)
                col_adjust(&win->w_cursor);
        }
    }
}

#if defined(FEAT_JUMPLIST)
/*
 * When deleting lines, this may create duplicate marks in the
 * jumplist. They will be removed here for the current window.
 */
    static void
cleanup_jumplist()
{
    int     i;
    int     from, to;

    to = 0;
    for (from = 0; from < curwin->w_jumplistlen; ++from)
    {
        if (curwin->w_jumplistidx == from)
            curwin->w_jumplistidx = to;
        for (i = from + 1; i < curwin->w_jumplistlen; ++i)
            if (curwin->w_jumplist[i].fmark.fnum
                                        == curwin->w_jumplist[from].fmark.fnum
                    && curwin->w_jumplist[from].fmark.fnum != 0
                    && curwin->w_jumplist[i].fmark.mark.lnum
                                  == curwin->w_jumplist[from].fmark.mark.lnum)
                break;
        if (i >= curwin->w_jumplistlen)     /* no duplicate */
            curwin->w_jumplist[to++] = curwin->w_jumplist[from];
        else
            vim_free(curwin->w_jumplist[from].fname);
    }
    if (curwin->w_jumplistidx == curwin->w_jumplistlen)
        curwin->w_jumplistidx = to;
    curwin->w_jumplistlen = to;
}

/*
 * Copy the jumplist from window "from" to window "to".
 */
    void
copy_jumplist(from, to)
    win_T       *from;
    win_T       *to;
{
    int         i;

    for (i = 0; i < from->w_jumplistlen; ++i)
    {
        to->w_jumplist[i] = from->w_jumplist[i];
        if (from->w_jumplist[i].fname != NULL)
            to->w_jumplist[i].fname = vim_strsave(from->w_jumplist[i].fname);
    }
    to->w_jumplistlen = from->w_jumplistlen;
    to->w_jumplistidx = from->w_jumplistidx;
}

/*
 * Free items in the jumplist of window "wp".
 */
    void
free_jumplist(wp)
    win_T       *wp;
{
    int         i;

    for (i = 0; i < wp->w_jumplistlen; ++i)
        vim_free(wp->w_jumplist[i].fname);
}
#endif

    void
set_last_cursor(win)
    win_T       *win;
{
    if (win->w_buffer != NULL)
        win->w_buffer->b_last_cursor = win->w_cursor;
}

#if defined(EXITFREE)
    void
free_all_marks()
{
    int         i;

    for (i = 0; i < NMARKS + EXTRA_MARKS; i++)
        if (namedfm[i].fmark.mark.lnum != 0)
            vim_free(namedfm[i].fname);
}
#endif
