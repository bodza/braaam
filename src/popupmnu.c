/*
 * popupmnu.c: Popup menu (PUM)
 */
#include "vim.h"

#if defined(FEAT_INS_EXPAND)

static pumitem_T *pum_array = NULL;     /* items of displayed pum */
static int pum_size;                    /* nr of items in "pum_array" */
static int pum_selected;                /* index of selected item or -1 */
static int pum_first = 0;               /* index of top item */

static int pum_height;                  /* nr of displayed pum items */
static int pum_width;                   /* width of displayed pum items */
static int pum_base_width;              /* width of pum items base */
static int pum_kind_width;              /* width of pum items kind column */
static int pum_scrollbar;               /* TRUE when scrollbar present */

static int pum_row;                     /* top row of pum */
static int pum_col;                     /* left column of pum */

static int pum_do_redraw = FALSE;       /* do redraw anyway */

static int pum_set_selected(int n, int repeat);

#define PUM_DEF_HEIGHT 10
#define PUM_DEF_WIDTH  15

/*
 * Show the popup menu with items "array[size]".
 * "array" must remain valid until pum_undisplay() is called!
 * When possible the leftmost character is aligned with screen column "col".
 * The menu appears above the screen line "row" or at "row" + "height" - 1.
 */
    void
pum_display(array, size, selected)
    pumitem_T   *array;
    int         size;
    int         selected;       /* index of initially selected item, none if out of range */
{
    int         w;
    int         def_width;
    int         max_width;
    int         kind_width;
    int         extra_width;
    int         i;
    int         top_clear;
    int         row;
    int         context_lines;
    int         col;
    int         above_row = cmdline_row;
    int         redo_count = 0;

redo:
    def_width = PUM_DEF_WIDTH;
    max_width = 0;
    kind_width = 0;
    extra_width = 0;

    /* Pretend the pum is already there to avoid that must_redraw is set when
     * 'cuc' is on. */
    pum_array = (pumitem_T *)1;
    validate_cursor_col();
    pum_array = NULL;

    row = curwin->w_wrow + W_WINROW(curwin);

        top_clear = 0;

    /*
     * Figure out the size and position of the pum.
     */
    if (size < PUM_DEF_HEIGHT)
        pum_height = size;
    else
        pum_height = PUM_DEF_HEIGHT;
    if (p_ph > 0 && pum_height > p_ph)
        pum_height = p_ph;

    /* Put the pum below "row" if possible.  If there are few lines decide on
     * where there is more room. */
    if (row  + 2 >= above_row - pum_height && row > (above_row - top_clear) / 2)
    {
        /* pum above "row" */

        /* Leave two lines of context if possible */
        if (curwin->w_wrow - curwin->w_cline_row >= 2)
            context_lines = 2;
        else
            context_lines = curwin->w_wrow - curwin->w_cline_row;

        if (row >= size + context_lines)
        {
            pum_row = row - size - context_lines;
            pum_height = size;
        }
        else
        {
            pum_row = 0;
            pum_height = row - context_lines;
        }
        if (p_ph > 0 && pum_height > p_ph)
        {
            pum_row += pum_height - p_ph;
            pum_height = p_ph;
        }
    }
    else
    {
        /* pum below "row" */

        /* Leave two lines of context if possible */
        if (curwin->w_cline_row + curwin->w_cline_height - curwin->w_wrow >= 3)
            context_lines = 3;
        else
            context_lines = curwin->w_cline_row + curwin->w_cline_height - curwin->w_wrow;

        pum_row = row + context_lines;
        if (size > above_row - pum_row)
            pum_height = above_row - pum_row;
        else
            pum_height = size;
        if (p_ph > 0 && pum_height > p_ph)
            pum_height = p_ph;
    }

    /* don't display when we only have room for one line */
    if (pum_height < 1 || (pum_height == 1 && size > 1))
        return;

    /* Compute the width of the widest match and the widest extra. */
    for (i = 0; i < size; ++i)
    {
        w = vim_strsize(array[i].pum_text);
        if (max_width < w)
            max_width = w;
        if (array[i].pum_kind != NULL)
        {
            w = vim_strsize(array[i].pum_kind) + 1;
            if (kind_width < w)
                kind_width = w;
        }
        if (array[i].pum_extra != NULL)
        {
            w = vim_strsize(array[i].pum_extra) + 1;
            if (extra_width < w)
                extra_width = w;
        }
    }
    pum_base_width = max_width;
    pum_kind_width = kind_width;

    /* Calculate column */
    if (curwin->w_p_rl)
        col = W_WINCOL(curwin) + W_WIDTH(curwin) - curwin->w_wcol - 1;
    else
        col = W_WINCOL(curwin) + curwin->w_wcol;

    /* if there are more items than room we need a scrollbar */
    if (pum_height < size)
    {
        pum_scrollbar = 1;
        ++max_width;
    }
    else
        pum_scrollbar = 0;

    if (def_width < max_width)
        def_width = max_width;

    if (((col < Columns - PUM_DEF_WIDTH || col < Columns - max_width)
                && !curwin->w_p_rl)
            || (curwin->w_p_rl && (col > PUM_DEF_WIDTH || col > max_width)
       ))
    {
        /* align pum column with "col" */
        pum_col = col;

        if (curwin->w_p_rl)
            pum_width = pum_col - pum_scrollbar + 1;
        else
            pum_width = Columns - pum_col - pum_scrollbar;

        if (pum_width > max_width + kind_width + extra_width + 1
                                                 && pum_width > PUM_DEF_WIDTH)
        {
            pum_width = max_width + kind_width + extra_width + 1;
            if (pum_width < PUM_DEF_WIDTH)
                pum_width = PUM_DEF_WIDTH;
        }
    }
    else if (Columns < def_width)
    {
        /* not enough room, will use what we have */
        if (curwin->w_p_rl)
            pum_col = Columns - 1;
        else
            pum_col = 0;
        pum_width = Columns - 1;
    }
    else
    {
        if (max_width > PUM_DEF_WIDTH)
            max_width = PUM_DEF_WIDTH;  /* truncate */
        if (curwin->w_p_rl)
            pum_col = max_width - 1;
        else
            pum_col = Columns - max_width;
        pum_width = max_width - pum_scrollbar;
    }

    pum_array = array;
    pum_size = size;

    /* Set selected item and redraw.  If the window size changed need to redo
     * the positioning.  Limit this to two times, when there is not much
     * room the window size will keep changing. */
    if (pum_set_selected(selected, redo_count) && ++redo_count <= 2)
        goto redo;
}

/*
 * Redraw the popup menu, using "pum_first" and "pum_selected".
 */
    void
pum_redraw()
{
    int         row = pum_row;
    int         col;
    int         attr_norm = highlight_attr[HLF_PNI];
    int         attr_select = highlight_attr[HLF_PSI];
    int         attr_scroll = highlight_attr[HLF_PSB];
    int         attr_thumb = highlight_attr[HLF_PST];
    int         attr;
    int         i;
    int         idx;
    char_u      *s;
    char_u      *p = NULL;
    int         totwidth, width, w;
    int         thumb_pos = 0;
    int         thumb_heigth = 1;
    int         round;
    int         n;

    /* Never display more than we have */
    if (pum_first > pum_size - pum_height)
        pum_first = pum_size - pum_height;

    if (pum_scrollbar)
    {
        thumb_heigth = pum_height * pum_height / pum_size;
        if (thumb_heigth == 0)
            thumb_heigth = 1;
        thumb_pos = (pum_first * (pum_height - thumb_heigth)
                            + (pum_size - pum_height) / 2)
                                                    / (pum_size - pum_height);
    }

    for (i = 0; i < pum_height; ++i)
    {
        idx = i + pum_first;
        attr = (idx == pum_selected) ? attr_select : attr_norm;

        /* prepend a space if there is room */
        if (curwin->w_p_rl)
        {
            if (pum_col < W_WINCOL(curwin) + W_WIDTH(curwin) - 1)
                screen_putchar(' ', row, pum_col + 1, attr);
        }
        else
            if (pum_col > 0)
                screen_putchar(' ', row, pum_col - 1, attr);

        /* Display each entry, use two spaces for a Tab.
         * Do this 3 times: For the main text, kind and extra info */
        col = pum_col;
        totwidth = 0;
        for (round = 1; round <= 3; ++round)
        {
            width = 0;
            s = NULL;
            switch (round)
            {
                case 1: p = pum_array[idx].pum_text; break;
                case 2: p = pum_array[idx].pum_kind; break;
                case 3: p = pum_array[idx].pum_extra; break;
            }
            if (p != NULL)
                for ( ; ; mb_ptr_adv(p))
                {
                    if (s == NULL)
                        s = p;
                    w = ptr2cells(p);
                    if (*p == NUL || *p == TAB || totwidth + w > pum_width)
                    {
                        /* Display the text that fits or comes before a Tab.
                         * First convert it to printable characters. */
                        char_u  *st;
                        int     saved = *p;

                        *p = NUL;
                        st = transstr(s);
                        *p = saved;
                        if (curwin->w_p_rl)
                        {
                            if (st != NULL)
                            {
                                char_u  *rt = reverse_text(st);

                                if (rt != NULL)
                                {
                                    char_u      *rt_start = rt;
                                    int         size;

                                    size = vim_strsize(rt);
                                    if (size > pum_width)
                                    {
                                        do
                                        {
                                            size -= has_mbyte ? (*mb_ptr2cells)(rt) : 1;
                                            mb_ptr_adv(rt);
                                        } while (size > pum_width);

                                        if (size < pum_width)
                                        {
                                            /* Most left character requires
                                             * 2-cells but only 1 cell is
                                             * available on screen.  Put a
                                             * '<' on the left of the pum
                                             * item */
                                            *(--rt) = '<';
                                            size++;
                                        }
                                    }
                                    screen_puts_len(rt, (int)STRLEN(rt), row, col - size + 1, attr);
                                    vim_free(rt_start);
                                }
                                vim_free(st);
                            }
                            col -= width;
                        }
                        else
                        {
                            if (st != NULL)
                            {
                                screen_puts_len(st, (int)STRLEN(st), row, col, attr);
                                vim_free(st);
                            }
                            col += width;
                        }

                        if (*p != TAB)
                            break;

                        /* Display two spaces for a Tab. */
                        if (curwin->w_p_rl)
                        {
                            screen_puts_len((char_u *)"  ", 2, row, col - 1, attr);
                            col -= 2;
                        }
                        else
                        {
                            screen_puts_len((char_u *)"  ", 2, row, col, attr);
                            col += 2;
                        }
                        totwidth += 2;
                        s = NULL;           /* start text at next char */
                        width = 0;
                    }
                    else
                        width += w;
                }

            if (round > 1)
                n = pum_kind_width + 1;
            else
                n = 1;

            /* Stop when there is nothing more to display. */
            if (round == 3
                    || (round == 2 && pum_array[idx].pum_extra == NULL)
                    || (round == 1 && pum_array[idx].pum_kind == NULL
                                          && pum_array[idx].pum_extra == NULL)
                    || pum_base_width + n >= pum_width)
                break;
            if (curwin->w_p_rl)
            {
                screen_fill(row, row + 1, pum_col - pum_base_width - n + 1, col + 1, ' ', ' ', attr);
                col = pum_col - pum_base_width - n + 1;
            }
            else
            {
                screen_fill(row, row + 1, col, pum_col + pum_base_width + n, ' ', ' ', attr);
                col = pum_col + pum_base_width + n;
            }
            totwidth = pum_base_width + n;
        }

        if (curwin->w_p_rl)
            screen_fill(row, row + 1, pum_col - pum_width + 1, col + 1, ' ', ' ', attr);
        else
            screen_fill(row, row + 1, col, pum_col + pum_width, ' ', ' ', attr);
        if (pum_scrollbar > 0)
        {
            if (curwin->w_p_rl)
                screen_putchar(' ', row, pum_col - pum_width,
                        i >= thumb_pos && i < thumb_pos + thumb_heigth ? attr_thumb : attr_scroll);
            else
                screen_putchar(' ', row, pum_col + pum_width,
                        i >= thumb_pos && i < thumb_pos + thumb_heigth ? attr_thumb : attr_scroll);
        }

        ++row;
    }
}

/*
 * Set the index of the currently selected item.  The menu will scroll when
 * necessary.  When "n" is out of range don't scroll.
 * This may be repeated when the preview window is used:
 * "repeat" == 0: open preview window normally
 * "repeat" == 1: open preview window but don't set the size
 * "repeat" == 2: don't open preview window
 * Returns TRUE when the window was resized and the location of the popup menu
 * must be recomputed.
 */
    static int
pum_set_selected(n, repeat)
    int     n;
    int     repeat;
{
    int     resized = FALSE;
    int     context = pum_height / 2;

    pum_selected = n;

    if (pum_selected >= 0 && pum_selected < pum_size)
    {
        if (pum_first > pum_selected - 4)
        {
            /* scroll down; when we did a jump it's probably a PageUp then
             * scroll a whole page */
            if (pum_first > pum_selected - 2)
            {
                pum_first -= pum_height - 2;
                if (pum_first < 0)
                    pum_first = 0;
                else if (pum_first > pum_selected)
                    pum_first = pum_selected;
            }
            else
                pum_first = pum_selected;
        }
        else if (pum_first < pum_selected - pum_height + 5)
        {
            /* scroll up; when we did a jump it's probably a PageDown then
             * scroll a whole page */
            if (pum_first < pum_selected - pum_height + 1 + 2)
            {
                pum_first += pum_height - 2;
                if (pum_first < pum_selected - pum_height + 1)
                    pum_first = pum_selected - pum_height + 1;
            }
            else
                pum_first = pum_selected - pum_height + 1;
        }

        /* Give a few lines of context when possible. */
        if (context > 3)
            context = 3;
        if (pum_height > 2)
        {
            if (pum_first > pum_selected - context)
            {
                /* scroll down */
                pum_first = pum_selected - context;
                if (pum_first < 0)
                    pum_first = 0;
            }
            else if (pum_first < pum_selected + context - pum_height + 1)
            {
                /* scroll up */
                pum_first = pum_selected + context - pum_height + 1;
            }
        }
    }

    if (!resized)
        pum_redraw();

    return resized;
}

/*
 * Undisplay the popup menu (later).
 */
    void
pum_undisplay()
{
    pum_array = NULL;
    redraw_all_later(SOME_VALID);
    redraw_tabline = TRUE;
    status_redraw_all();
}

/*
 * Clear the popup menu.  Currently only resets the offset to the first
 * displayed item.
 */
    void
pum_clear()
{
    pum_first = 0;
}

/*
 * Return TRUE if the popup menu is displayed.
 * Overruled when "pum_do_redraw" is set, used to redraw the status lines.
 */
    int
pum_visible()
{
    return !pum_do_redraw && pum_array != NULL;
}

/*
 * Return the height of the popup menu, the number of entries visible.
 * Only valid when pum_visible() returns TRUE!
 */
    int
pum_get_height()
{
    return pum_height;
}

#endif
