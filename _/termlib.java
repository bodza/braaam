PRIVATE byte *_addfmt(byte *buf, byte *fmt, int val)
{
    sprintf(buf, fmt, val);
    while (*buf != NUL)
        buf++;
    return buf;
}

PRIVATE byte *tgoto_UP = null, *tgoto_BC = null; /* pointers to UP and BC strings from database */
PRIVATE byte tgoto_buffer[32];

/*
 * Decode cm cursor motion string.
 * cm is cursor motion string, line and col are the desired destination.
 * Returns a pointer to the decoded string, or "OOPS" if it cannot be decoded.
 *
 * Accepted escapes are:
 *      %d       as in printf, 0 origin.
 *      %2, %3   like %02d, %03d in printf.
 *      %.       like %c
 *      %+x      adds <x> to value, then %.
 *      %>xy     if value > x, adds y. No output.
 *      %i       increments line & col. No output.
 *      %r       reverses order of line & col. No output.
 *      %%       prints as a single %.
 *      %n       exclusive or row & col with 0140.
 *      %B       BCD, no output.
 *      %D       reverse coding (x-2*(x%16)), no output.
 */
PRIVATE byte *tgoto(byte *cm, int col, int line)
    /* cm: string, from termcap */
    /* col: x position */
    /* line: y position */
{
    if (cm == null)
        return "OOPS";                          /* kludge, but standard */

    boolean reverse = false;                    /* reverse flag */
    boolean addup = false;                      /* add upline */
    boolean addbak = false;                     /* add backup */

    byte *p = tgoto_buffer;                     /* pointer in returned string */

    while (*cm != NUL)
    {
        byte b = *cm++;
        if (b != '%')                           /* normal char */
        {
            *p++ = b;
            continue;
        }

        b = *cm++;
        switch (b)                              /* % escape */
        {
            case 'd':                           /* decimal */
                p = _addfmt(p, "%d", line);
                line = col;
                break;

            case '2':                           /* 2 digit decimal */
                p = _addfmt(p, "%02d", line);
                line = col;
                break;

            case '3':                           /* 3 digit decimal */
                p = _addfmt(p, "%03d", line);
                line = col;
                break;

            case '>':                           /* %>xy: if >x, add y */
            {
                byte gx = *cm++, gy = *cm++;
                if (col > gx)
                    col += gy;
                if (line > gx)
                    line += gy;
                break;
            }

            case '+':                           /* %+c: add c */
                line += *cm++;

            case '.':                           /* print x/y */
                if (line == '\t'                /* these are */
                 || line == '\n'                /* chars that */
                 || line == '\004'              /* UNIX hates */
                 || line == '\000')
                {
                    line++;                     /* so go to next pos */
                    if (reverse == (line == col))
                        addup = true;           /* and mark UP */
                    else
                        addbak = true;          /* or BC */
                }
                *p++ = line;
                line = col;
                break;

            case 'r':                           /* r: reverse */
                gx = line;
                line = col;
                col = gx;
                reverse = true;
                break;

            case 'i':                           /* increment (1-origin screen) */
                col++;
                line++;
                break;

            case '%':                           /* %%=% literally */
                *p++ = '%';
                break;

            case 'n':                           /* magic DM2500 code */
                line ^= 0140;
                col ^= 0140;
                break;

            case 'B':                           /* bcd encoding */
                line = line / 10 << 4 + line % 10;
                col = col / 10 << 4 + col % 10;
                break;

            case 'D':                           /* magic Delta Data code */
                line = line - 2 * (line & 15);
                col = col - 2 * (col & 15);
                break;

            default:                            /* unknown escape */
                return "OOPS";
        }
    }

    if (addup)                                  /* add upline */
        if (tgoto_UP != null)
        {
            cm = tgoto_UP;
            while (asc_isdigit(*cm) || *cm == '.')
                cm++;
            if (*cm == '*')
                cm++;
            while (*cm != NUL)
                *p++ = *cm++;
        }

    if (addbak)                                 /* add backspace */
        if (tgoto_BC != null)
        {
            cm = tgoto_BC;
            while (asc_isdigit(*cm) || *cm == '.')
                cm++;
            if (*cm == '*')
                cm++;
            while (*cm != NUL)
                *p++ = *cm++;
        }
        else
            *p++ = '\b';

    *p = NUL;

    return tgoto_buffer;
}

/*
 * Note: "s" may have padding information ahead of it, in the form of nnnTEXT or nnn*TEXT.
 *  nnn is the number of milliseconds to delay, and may be a decimal fraction (nnn.mmm).
 *  In case an asterisk is given, the delay is to be multiplied by affcnt.
 *  The delay is produced by outputting a number of nulls (or other padding char) after printing the TEXT.
 */
PRIVATE int tputs(byte *s, int affcnt UNUSED, void (*outc)(int))
    /* s: string to print */
    /* affcnt: number of lines affected */
    /* outc: routine to output 1 character */
{
    if (asc_isdigit(*s))
    {
        for (++s; asc_isdigit(*s); )
            s++;
        if (*s == '.')
            for (++s; asc_isdigit(*s); )
                s++;
        if (*s == '*')
            s++;
    }

    while (*s != NUL)
        (*outc)(*s++);

    return 0;
}
