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
public class VimQ
{
    /*
     * charset.c --------------------------------------------------------------------------------------
     */

    /*private*/ static boolean chartab_initialized;

    /* b_chartab[] is an array of 8 ints, each bit representing one of the characters 0-255. */
    /*private*/ static void set_chartab(buffer_C buf, int c)
    {
        buf.b_chartab[c >>> 5] |= (1 << (c & 0x1f));
    }

    /*private*/ static void reset_chartab(buffer_C buf, int c)
    {
        buf.b_chartab[c >>> 5] &= ~(1 << (c & 0x1f));
    }

    /*private*/ static int get_chartab(buffer_C buf, int c)
    {
        return (buf.b_chartab[c >>> 5] & (1 << (c & 0x1f)));
    }

    /*
     * Fill chartab[].  Also fills curbuf.b_chartab[] with flags for keyword
     * characters for current buffer.
     *
     * Depends on the option settings 'iskeyword', 'isident', 'isfname',
     * 'isprint' and 'encoding'.
     *
     * The index in chartab[] depends on 'encoding':
     * - For non-multi-byte index with the byte (same as the character).
     * - For UTF-8 index with the character (when first byte is up to 0x80 it is
     *   the same as the character, if the first byte is 0x80 and above it depends
     *   on further bytes).
     *
     * The contents of chartab[]:
     * - The lower two bits, masked by CT_CELL_MASK, give the number of display
     *   cells the character occupies (1 or 2).  Not valid for UTF-8 above 0x80.
     * - CT_PRINT_CHAR bit is set when the character is printable (no need to
     *   translate the character before displaying it).
     * - CT_FNAME_CHAR bit is set when the character can be in a file name.
     * - CT_ID_CHAR bit is set when the character can be in an identifier.
     *
     * Return false if 'iskeyword', 'isident', 'isfname' or 'isprint' option has
     * an error, true otherwise.
     */
    /*private*/ static boolean init_chartab()
    {
        return buf_init_chartab(curbuf, true);
    }

    /*private*/ static boolean buf_init_chartab(buffer_C buf, boolean global)
        /* global: false: only set buf.b_chartab[] */
    {
        if (global)
        {
            /*
             * Set the default size for printable characters:
             * From <Space> to '~' is 1 (printable), others are 2 (not printable).
             * This also inits all 'isident' and 'isfname' flags to false.
             */
            int c = 0;
            while (c < ' ')
                chartab[c++] = (dy_flags[0] & DY_UHEX) != 0 ? (byte)4 : (byte)2;
            while (c <= '~')
                chartab[c++] = 1 + CT_PRINT_CHAR;
            while (c < 256)
            {
                /* UTF-8: bytes 0xa0 - 0xff are printable (latin1). */
                if (0xa0 <= c)
                    chartab[c++] = CT_PRINT_CHAR + 1;
                else
                    /* the rest is unprintable by default */
                    chartab[c++] = (dy_flags[0] & DY_UHEX) != 0 ? (byte)4 : (byte)2;
            }

            /* Assume that every multi-byte char is a filename character. */
            for (c = 1; c < 256; c++)
                if (0xa0 <= c)
                    chartab[c] |= CT_FNAME_CHAR;
        }

        /*
         * Init word char flags all to false.
         */
        AFILL(buf.b_chartab, 0);

        /*
         * In lisp mode the '-' character is included in keywords.
         */
        if (buf.b_p_lisp[0])
            set_chartab(buf, '-');

        /* Walk through the 'isident', 'iskeyword', 'isfname' and 'isprint' options.
         * Each option is a list of characters, character numbers or ranges,
         * separated by commas, e.g.: "200-210,x,#-178,-"
         */
        for (int i = global ? 0 : 3; i <= 3; i++)
        {
            Bytes p;
            if (i == 0)
                p = p_isi[0];          /* first round: 'isident' */
            else if (i == 1)
                p = p_isp[0];          /* second round: 'isprint' */
            else if (i == 2)
                p = p_isf[0];          /* third round: 'isfname' */
            else    /* i == 3 */
                p = buf.b_p_isk[0];    /* fourth round: 'iskeyword' */

            while (p.at(0) != NUL)
            {
                boolean tilde = false;
                boolean do_isalpha = false;
                if (p.at(0) == (byte)'^' && p.at(1) != NUL)
                {
                    tilde = true;
                    p = p.plus(1);
                }
                int c;
                if (asc_isdigit(p.at(0)))
                {
                    Bytes[] __ = { p }; c = (int)getdigits(__); p = __[0];
                }
                else
                {
                    Bytes[] __ = { p }; c = us_ptr2char_adv(__, true); p = __[0];
                }
                int c2 = -1;
                if (p.at(0) == (byte)'-' && p.at(1) != NUL)
                {
                    p = p.plus(1);
                    if (asc_isdigit(p.at(0)))
                    {
                        Bytes[] __ = { p }; c2 = (int)getdigits(__); p = __[0];
                    }
                    else
                    {
                        Bytes[] __ = { p }; c2 = us_ptr2char_adv(__, true); p = __[0];
                    }
                }
                if (c <= 0 || 256 <= c || (c2 < c && c2 != -1) || 256 <= c2 || !(p.at(0) == NUL || p.at(0) == (byte)','))
                    return false;

                if (c2 == -1)       /* not a range */
                {
                    /* A single '@' (not "@-@"):
                     * Decide on letters being ID/printable/keyword chars with
                     * standard function isalpha().  This takes care of locale
                     * for single-byte characters).
                     */
                    if (c == '@')
                    {
                        do_isalpha = true;
                        c = 1;
                        c2 = 255;
                    }
                    else
                        c2 = c;
                }
                while (c <= c2)
                {
                    /* Use the MB_ functions here, because isalpha() doesn't work properly
                     * when 'encoding' is "latin1" and the locale is "C". */
                    if (!do_isalpha || utf_islower(c) || utf_isupper(c))
                    {
                        if (i == 0)                 /* (re)set ID flag */
                        {
                            if (tilde)
                                chartab[c] &= ~CT_ID_CHAR;
                            else
                                chartab[c] |= CT_ID_CHAR;
                        }
                        else if (i == 1)            /* (re)set printable */
                        {
                            if (c < ' ' || '~' < c)
                            {
                                if (tilde)
                                {
                                    chartab[c] = (byte)((chartab[c] & ~CT_CELL_MASK) + ((dy_flags[0] & DY_UHEX) != 0 ? 4 : 2));
                                    chartab[c] &= ~CT_PRINT_CHAR;
                                }
                                else
                                {
                                    chartab[c] = (byte)((chartab[c] & ~CT_CELL_MASK) + 1);
                                    chartab[c] |= CT_PRINT_CHAR;
                                }
                            }
                        }
                        else if (i == 2)            /* (re)set fname flag */
                        {
                            if (tilde)
                                chartab[c] &= ~CT_FNAME_CHAR;
                            else
                                chartab[c] |= CT_FNAME_CHAR;
                        }
                        else /* i == 3 */           /* (re)set keyword flag */
                        {
                            if (tilde)
                                reset_chartab(buf, c);
                            else
                                set_chartab(buf, c);
                        }
                    }
                    c++;
                }

                c = p.at(0);
                p = skip_to_option_part(p);
                if (c == ',' && p.at(0) == NUL)
                    /* Trailing comma is not allowed. */
                    return false;
            }
        }

        chartab_initialized = true;
        return true;
    }

    /*
     * Translate any special characters in buf[bufsize] in-place.
     * The result is a string with only printable characters, but if there is not
     * enough room, not all characters will be translated.
     */
    /*private*/ static void trans_characters(Bytes buf, int bufsize)
    {
        int len = strlen(buf);
        int room = bufsize - len;                           /* room in buffer after string */

        while (buf.at(0) != 0)
        {
            int trs_len = us_ptr2len_cc(buf);

            /* Assume a multi-byte character doesn't need translation. */
            if (1 < trs_len)
                len -= trs_len;
            else
            {
                Bytes trs = transchar_byte(buf.at(0));          /* translated character */
                trs_len = strlen(trs);
                if (1 < trs_len)
                {
                    room -= trs_len - 1;
                    if (room <= 0)
                        return;
                    BCOPY(buf, trs_len, buf, 1, len);
                }
                BCOPY(buf, trs, trs_len);
                --len;
            }

            buf = buf.plus(trs_len);
        }
    }

    /*
     * Translate a string into allocated memory, replacing special chars with printable chars.
     */
    /*private*/ static Bytes transstr(Bytes s)
    {
        Bytes hexbuf = new Bytes(11);

        /* Compute the length of the result, taking account of unprintable multi-byte characters. */
        int len = 0;

        for (Bytes p = s; p.at(0) != NUL; )
        {
            int l = us_ptr2len_cc(p);
            if (1 < l)
            {
                int c = us_ptr2char(p);
                p = p.plus(l);
                if (vim_isprintc(c))
                    len += l;
                else
                {
                    transchar_hex(hexbuf, c);
                    len += strlen(hexbuf);
                }
            }
            else
            {
                l = mb_byte2cells((p = p.plus(1)).at(-1));
                if (0 < l)
                    len += l;
                else
                    len += 4;   /* illegal byte sequence */
            }
        }

        Bytes res = new Bytes(len + 1);
        res.be(0, NUL);

        for (Bytes p = s; p.at(0) != NUL; )
        {
            int l = us_ptr2len_cc(p);
            if (1 < l)
            {
                int c = us_ptr2char(p);
                if (vim_isprintc(c))
                    STRNCAT(res, p, l);     /* append printable multi-byte char */
                else
                    transchar_hex(res.plus(strlen(res)), c);
                p = p.plus(l);
            }
            else
                STRCAT(res, transchar_byte((p = p.plus(1)).at(-1)));
        }

        return res;
    }

    /*
     * Convert the string "str[orglen]" to do ignore-case comparing.
     * Puts the result in "buf[buflen]".
     */
    /*private*/ static Bytes str_foldcase(Bytes str, int orglen, Bytes buf, int buflen)
    {
        int len = orglen;

        if (buflen <= len)          /* Ugly! */
            len = buflen - 1;
        BCOPY(buf, str, len);
        buf.be(len, NUL);

        /* Make each character lower case. */
        int i = 0;
        while (buf.at(i) != NUL)
        {
            int c = us_ptr2char(buf.plus(i));
            int olen = us_ptr2len(buf.plus(i));
            int lc = utf_tolower(c);

            /* Only replace the character when it is not an invalid
             * sequence (ASCII character or more than one byte) and
             * utf_tolower() doesn't return the original character. */
            if ((c < 0x80 || 1 < olen) && c != lc)
            {
                int nlen = utf_char2len(lc);

                /* If the byte length changes, need to shift
                 * the following characters forward or backward. */
                if (olen != nlen)
                {
                    if (olen < nlen && buflen <= len + nlen - olen)
                    {
                        /* out of memory, keep old char */
                        lc = c;
                        nlen = olen;
                    }
                    if (olen != nlen)
                    {
                        BCOPY(buf, i + nlen, buf, i + olen, strlen(buf, i + olen) + 1);
                        len += nlen - olen;
                    }
                }
                utf_char2bytes(lc, buf.plus(i));
            }

            /* skip to next multi-byte char */
            i += us_ptr2len_cc(buf.plus(i));
        }

        return buf;
    }

    /*
     * Catch 22: chartab[] can't be initialized before the options are initialized,
     * and initializing options may cause transchar() to be called!
     * When !chartab_initialized, don't use chartab[].
     * Does NOT work for multi-byte characters, c must be <= 255.
     * Also doesn't work for the first byte of a multi-byte, "c" must be a character!
     */
    /*private*/ static Bytes transchar_buf = new Bytes(7);

    /*private*/ static Bytes transchar(int c)
    {
        int i = 0;

        if (is_special(c))      /* special key code, display as ~@ char */
        {
            transchar_buf.be(0, (byte)'~');
            transchar_buf.be(1, (byte)'@');
            i = 2;
            c = char_u(KB_SECOND(c));
        }

        if ((!chartab_initialized && (' ' <= c && c <= '~')) || (c < 256 && vim_isprintc(c)))
        {
            /* printable character */
            transchar_buf.be(i, c);
            transchar_buf.be(i + 1, NUL);
        }
        else
            transchar_nonprint(transchar_buf.plus(i), c);

        return transchar_buf;
    }

    /*
     * Like transchar(), but called with a byte instead of a character.
     * Checks for an illegal UTF-8 byte.
     */
    /*private*/ static Bytes transchar_byte(byte b)
    {
        if (0x80 <= char_u(b))
        {
            transchar_nonprint(transchar_buf, char_u(b));
            return transchar_buf;
        }
        return transchar(char_u(b));
    }

    /*
     * Convert non-printable character to two or more printable characters in "buf[]".
     * "buf" needs to be able to hold five bytes.
     * Does NOT work for multi-byte characters, c must be <= 255.
     */
    /*private*/ static void transchar_nonprint(Bytes buf, int c)
    {
        if (c == NL)
            c = NUL;                                    /* we use newline in place of a NUL */
        else if (c == CAR && get_fileformat(curbuf) == EOL_MAC)
            c = NL;                                     /* we use CR in place of  NL in this case */

        if ((dy_flags[0] & DY_UHEX) != 0)                  /* 'display' has "uhex" */
            transchar_hex(buf, c);

        else if (c <= 0x7f)                             /* 0x00 - 0x1f and 0x7f */
        {
            buf.be(0, (byte)'^');
            buf.be(1, (byte)(c ^ 0x40));                          /* DEL displayed as ^? */

            buf.be(2, NUL);
        }
        else if (0x80 <= c)
        {
            transchar_hex(buf, c);
        }
        else if (' ' + 0x80 <= c && c <= '~' + 0x80)    /* 0xa0 - 0xfe */
        {
            buf.be(0, (byte)'|');
            buf.be(1, (byte)(c - 0x80));
            buf.be(2, NUL);
        }
        else                                            /* 0x80 - 0x9f and 0xff */
        {
            buf.be(0, (byte)'~');
            buf.be(1, (byte)((c - 0x80) ^ 0x40));                 /* 0xff displayed as ~? */
            buf.be(2, NUL);
        }
    }

    /*private*/ static void transchar_hex(Bytes buf, int c)
    {
        int i = 0;

        buf.be(i, (byte)'<');
        if (0xff < c)
        {
            buf.be(++i, nr2hex(c >>> 12));
            buf.be(++i, nr2hex(c >>> 8));
        }
        buf.be(++i, nr2hex(c >>> 4));
        buf.be(++i, nr2hex(c));
        buf.be(++i, (byte)'>');
        buf.be(++i, NUL);
    }

    /*
     * Convert the lower 4 bits of byte "c" to its hex character.
     * Lower case letters are used to avoid the confusion of <F1> being 0xf1 or function key 1.
     */
    /*private*/ static int nr2hex(int c)
    {
        if ((c & 0xf) <= 9)
            return (c & 0xf) + '0';

        return (c & 0xf) - 10 + 'a';
    }

    /*
     * Return number of display cells occupied by byte "b".
     * For multi-byte mode "b" must be the first byte of a character.
     * A TAB is counted as two cells: "^I".
     * For UTF-8 mode this will return 0 for bytes >= 0x80, because the number of cells depends on further bytes.
     */
    /*private*/ static int mb_byte2cells(byte b)
    {
        if (0x80 <= char_u(b))
            return 0;

        return (chartab[char_u(b)] & CT_CELL_MASK);
    }

    /*
     * Return number of display cells occupied by character "c".
     * "c" can be a special key (negative number) in which case 3 or 4 is returned.
     * A TAB is counted as two cells: "^I" or four: "<09>".
     */
    /*private*/ static int mb_char2cells(int c)
    {
        if (is_special(c))
            return mb_char2cells(char_u(KB_SECOND(c))) + 2;

        /* UTF-8: above 0x80 need to check the value. */
        if (0x80 <= c)
            return utf_char2cells(c);

        return (chartab[c & 0xff] & CT_CELL_MASK);
    }

    /*
     * Return number of display cells occupied by character at "*p".
     * A TAB is counted as two cells: "^I" or four: "<09>".
     */
    /*private*/ static int mb_ptr2cells(Bytes p)
    {
        /* For UTF-8 we need to look at more bytes if the first byte is >= 0x80. */
        if (0x80 <= char_u(p.at(0)))
            return us_ptr2cells(p);

        return (chartab[char_u(p.at(0))] & CT_CELL_MASK);
    }

    /*
     * Return the number of character cells string "s[len]" will take on the screen,
     * counting TABs as two characters: "^I".
     */
    /*private*/ static int mb_string2cells(Bytes p, int len)
    {
        int cells = 0;

        for (int i = 0; (len < 0 || i < len) && p.at(i) != NUL; i += us_ptr2len_cc(p.plus(i)))
            cells += mb_ptr2cells(p.plus(i));

        return cells;
    }

    /*
     * Return the number of characters 'c' will take on the screen,
     * taking into account the size of a tab.
     * Use a define to make it fast, this is used very often!!!
     * Also see getvcol() below.
     */
    /*private*/ static int win_buf_chartabsize(window_C wp, buffer_C buf, Bytes p, int col)
    {
        if (p.at(0) == TAB && (!wp.w_onebuf_opt.wo_list[0] || lcs_tab1[0] != NUL))
        {
            int ts = (int)buf.b_p_ts[0];
            return ts - (col % ts);
        }

        return mb_ptr2cells(p);
    }

    /*private*/ static int chartabsize(Bytes p, int col)
    {
        return win_buf_chartabsize(curwin, curbuf, p, col);
    }

    /*
     * Return the number of characters the string 's' will take on the screen,
     * taking into account the size of a tab.
     */
    /*private*/ static int linetabsize(Bytes s)
    {
        return linetabsize_col(s, 0);
    }

    /*
     * Like linetabsize(), but starting at column "startcol".
     */
    /*private*/ static int linetabsize_col(Bytes _s, int startcol)
    {
        Bytes[] s = { _s };

        int col = startcol;

        Bytes line = s[0];        /* pointer to start of line, for breakindent */
        while (s[0].at(0) != NUL)
            col += lbr_chartabsize_adv(line, s, col);

        return col;
    }

    /*
     * Like linetabsize(), but for a given window instead of the current one.
     */
    /*private*/ static int win_linetabsize(window_C wp, Bytes line, int len)
    {
        int col = 0;

        for (Bytes s = line; s.at(0) != NUL && (len == MAXCOL || BLT(s, line.plus(len))); s = s.plus(us_ptr2len_cc(s)))
            col += win_lbr_chartabsize(wp, line, s, col, null);

        return col;
    }

    /*
     * Return true if 'c' is a normal identifier character:
     * Letters and characters from the 'isident' option.
     */
    /*private*/ static boolean vim_isIDc(int c)
    {
        return (0 < c && c < 0x100 && (chartab[c] & CT_ID_CHAR) != 0);
    }

    /*
     * Return true if 'c' is a keyword character:
     * Letters and characters from 'iskeyword' option for current buffer.
     * For multi-byte characters us_get_class() is used (builtin rules).
     */
    /*private*/ static boolean vim_iswordc(int c, buffer_C buf)
    {
        if (0x100 <= c)
            return (2 <= utf_class(c));

        return (0 < c && c < 0x100 && get_chartab(buf, c) != 0);
    }

    /*private*/ static boolean us_iswordb(byte b, buffer_C buf)
    {
        return (b != 0 && get_chartab(buf, char_u(b)) != 0);
    }

    /*private*/ static boolean us_iswordp(Bytes p, buffer_C buf)
    {
        if (1 < us_byte2len(p.at(0), false))
            return (2 <= us_get_class(p, buf));

        return us_iswordb(p.at(0), buf);
    }

    /*
     * Return true if 'c' is a valid file-name character.
     * Assume characters above 0x100 are valid (multi-byte).
     */
    /*private*/ static boolean vim_isfilec(int c)
    {
        return (0x100 <= c || (0 < c && (chartab[c] & CT_FNAME_CHAR) != 0));
    }

    /*
     * return true if 'c' is a printable character
     * Assume characters above 0x100 are printable (multi-byte), except for Unicode.
     */
    /*private*/ static boolean vim_isprintc(int c)
    {
        if (0x100 <= c)
            return utf_printable(c);

        return (0x100 <= c || (0 < c && (chartab[c] & CT_PRINT_CHAR) != 0));
    }

    /*
     * like chartabsize(), but also check for line breaks on the screen
     */
    /*private*/ static int lbr_chartabsize(Bytes line, Bytes s, int col)
        /* line: start of the line */
    {
        if (!curwin.w_onebuf_opt.wo_lbr[0] && p_sbr[0].at(0) == NUL && !curwin.w_onebuf_opt.wo_bri[0])
        {
            if (curwin.w_onebuf_opt.wo_wrap[0])
                return win_nolbr_chartabsize(curwin, s, col, null);

            return win_buf_chartabsize(curwin, curbuf, s, col);
        }
        return win_lbr_chartabsize(curwin, (line == null) ? s : line, s, col, null);
    }

    /*
     * Call lbr_chartabsize() and advance the pointer.
     */
    /*private*/ static int lbr_chartabsize_adv(Bytes line, Bytes[] s, int col)
        /* line: start of the line */
    {
        int retval = lbr_chartabsize(line, s[0], col);
        s[0] = s[0].plus(us_ptr2len_cc(s[0]));
        return retval;
    }

    /*
     * This function is used very often, keep it fast!!!!
     *
     * If "headp" not null, set "*headp" to the size of what we for 'showbreak' string at start of line.
     * Warning: "*headp" is only set if it's a non-zero value, init to 0 before calling.
     */
    /*private*/ static int win_lbr_chartabsize(window_C wp, Bytes line, Bytes s, int col, int[] headp)
        /* line: start of the line */
    {
        int col_adj = 0;                /* col + screen size of tab */
        int mb_added = 0;
        boolean tab_corr = (s.at(0) == TAB);

        /*
         * No 'linebreak', 'showbreak' and 'breakindent': return quickly.
         */
        if (!wp.w_onebuf_opt.wo_lbr[0] && !wp.w_onebuf_opt.wo_bri[0] && p_sbr[0].at(0) == NUL)
        {
            if (wp.w_onebuf_opt.wo_wrap[0])
                return win_nolbr_chartabsize(wp, s, col, headp);

            return win_buf_chartabsize(wp, wp.w_buffer, s, col);
        }

        /*
         * First get normal size, without 'linebreak'.
         */
        int size = win_buf_chartabsize(wp, wp.w_buffer, s, col);
        byte c = s.at(0);
        if (tab_corr)
            col_adj = size - 1;

        /*
         * If 'linebreak' set check at a blank before a non-blank if the line needs a break here.
         */
        if (wp.w_onebuf_opt.wo_lbr[0] && breakat_flags[char_u(c)] && !breakat_flags[char_u(s.at(1))]
                        && wp.w_onebuf_opt.wo_wrap[0] && wp.w_width != 0)
        {
            /*
             * Count all characters from first non-blank after a blank up to next non-blank after a blank.
             */
            int numberextra = win_col_off(wp);
            int col2 = col;
            int colmax = wp.w_width - numberextra - col_adj;
            if (colmax <= col)
            {
                colmax += col_adj;
                int n = colmax +  win_col_off2(wp);
                if (0 < n)
                    colmax += (((col - colmax) / n) + 1) * n - col_adj;
            }

            for ( ; ; )
            {
                Bytes ps = s;
                s = s.plus(us_ptr2len_cc(s));
                c = s.at(0);
                if (!(c != NUL
                        && (breakat_flags[char_u(c)]
                            || (!breakat_flags[char_u(c)]
                                && (col2 == col || !breakat_flags[char_u(ps.at(0))])))))
                    break;

                col2 += win_buf_chartabsize(wp, wp.w_buffer, s, col2);
                if (colmax <= col2)         /* doesn't fit */
                {
                    size = colmax - col + col_adj;
                    tab_corr = false;
                    break;
                }
            }
        }
        else if (size == 2 && 1 < us_byte2len(s.at(0), false) && wp.w_onebuf_opt.wo_wrap[0] && in_win_border(wp, col))
        {
            size++;         /* Count the ">" in the last column. */
            mb_added = 1;
        }

        /*
         * May have to add something for 'breakindent' and/or 'showbreak' string at start of line.
         * Set "*headp" to the size of what we add.
         */
        int added = 0;
        if ((p_sbr[0].at(0) != NUL || wp.w_onebuf_opt.wo_bri[0]) && wp.w_onebuf_opt.wo_wrap[0] && col != 0)
        {
            int sbrlen = 0;
            int numberwidth = win_col_off(wp);

            int numberextra = numberwidth;
            col += numberextra + mb_added;
            if (wp.w_width <= col)
            {
                col -= wp.w_width;
                numberextra = wp.w_width - (numberextra - win_col_off2(wp));
                if (col >= numberextra && 0 < numberextra)
                    col %= numberextra;
                if (p_sbr[0].at(0) != NUL)
                {
                    sbrlen = us_charlen(p_sbr[0]);
                    if (sbrlen <= col)
                        col -= sbrlen;
                }
                if (col >= numberextra && 0 < numberextra)
                    col %= numberextra;
                else if (0 < col && 0 < numberextra)
                    col += numberwidth - win_col_off2(wp);

                numberwidth -= win_col_off2(wp);
            }
            if (col == 0 || wp.w_width < col + size + sbrlen)
            {
                added = 0;
                if (p_sbr[0].at(0) != NUL)
                {
                    if (wp.w_width < size + sbrlen + numberwidth)
                    {
                        /* calculate effective window width */
                        int width = wp.w_width - sbrlen - numberwidth;
                        int prev_width = (col != 0) ? wp.w_width - (sbrlen + col) : 0;
                        if (width == 0)
                            width = wp.w_width;
                        added += ((size - prev_width) / width) * mb_string2cells(p_sbr[0], -1);
                        if ((size - prev_width) % width != 0)
                            /* wrapped, add another length of 'sbr' */
                            added += mb_string2cells(p_sbr[0], -1);
                    }
                    else
                        added += mb_string2cells(p_sbr[0], -1);
                }
                if (wp.w_onebuf_opt.wo_bri[0])
                    added += get_breakindent_win(wp, line);

                size += added;
                if (col != 0)
                    added = 0;
            }
        }
        if (headp != null)
            headp[0] = added + mb_added;

        return size;
    }

    /*
     * Like win_lbr_chartabsize(), except that we know 'linebreak' is off and 'wrap' is on.
     * This means we need to check for a double-byte character that doesn't fit
     * at the end of the screen line.
     */
    /*private*/ static int win_nolbr_chartabsize(window_C wp, Bytes p, int col, int[] headp)
    {
        if (p.at(0) == TAB && (!wp.w_onebuf_opt.wo_list[0] || lcs_tab1[0] != NUL))
        {
            int ts = (int)wp.w_buffer.b_p_ts[0];
            return ts - (col % ts);
        }

        int n = mb_ptr2cells(p);
        /* Add one cell for a double-width character in the last column of the window,
         * displayed with a ">". */
        if (n == 2 && 1 < us_byte2len(p.at(0), false) && in_win_border(wp, col))
        {
            if (headp != null)
                headp[0] = 1;
            return 3;
        }
        return n;
    }

    /*
     * Return true if virtual column "vcol" is in the rightmost column of window "wp".
     */
    /*private*/ static boolean in_win_border(window_C wp, int vcol)
    {
        if (wp.w_width == 0)                            /* there is no border */
            return false;
        int width1 = wp.w_width - win_col_off(wp);      /* width of first line (after line number) */
        if (vcol < width1 - 1)
            return false;
        if (vcol == width1 - 1)
            return true;
        int width2 = width1 + win_col_off2(wp);         /* width of further lines */
        if (width2 <= 0)
            return false;

        return ((vcol - width1) % width2 == width2 - 1);
    }

    /*
     * Get virtual column number of pos.
     *  start: on the first position of this character (TAB, ctrl)
     * cursor: where the cursor is on this character (first char, except for TAB)
     *    end: on the last position of this character (TAB, ctrl)
     *
     * This is used very often, keep it fast!
     */
    /*private*/ static void getvcol(window_C wp, pos_C pos, int[] start, int[] cursor, int[] end)
    {
        Bytes p = ml_get_buf(wp.w_buffer, pos.lnum, false);    /* points to current char */
        Bytes line = p;                                        /* start of the line */
        Bytes posptr;                                          /* points to char at pos.col */
        if (pos.col == MAXCOL)
            posptr = null;                                      /* continue until the NUL */
        else
            posptr = p.plus(pos.col);

        int vcol = 0;
        int ts = (int)wp.w_buffer.b_p_ts[0];

        int[] head = new int[1];
        int incr;
        /*
         * This function is used very often, do some speed optimizations.
         * When 'list', 'linebreak', 'showbreak' and 'breakindent' are not set use a simple loop.
         * Also use this when 'list' is set but tabs take their normal size.
         */
        if ((!wp.w_onebuf_opt.wo_list[0] || lcs_tab1[0] != NUL) && !wp.w_onebuf_opt.wo_lbr[0] && p_sbr[0].at(0) == NUL && !wp.w_onebuf_opt.wo_bri[0])
        {
            for ( ; ; p = p.plus(us_ptr2len_cc(p)))
            {
                head[0] = 0;
                /* make sure we don't go past the end of the line */
                if (p.at(0) == NUL)
                {
                    incr = 1;       /* NUL at end of line only takes one column */
                    break;
                }
                /* A tab gets expanded, depending on the current column. */
                if (p.at(0) == TAB)
                    incr = ts - (vcol % ts);
                else
                {
                    incr = mb_ptr2cells(p);

                    /* If a double-cell char doesn't fit at the end of a line,
                     * it wraps to the next line, it's like this char is three cells wide. */
                    if (incr == 2 && wp.w_onebuf_opt.wo_wrap[0] && 1 < us_byte2len(p.at(0), false) && in_win_border(wp, vcol))
                    {
                        incr++;
                        head[0] = 1;
                    }
                }

                if (posptr != null && BLE(posptr, p))  /* character at pos.col */
                    break;

                vcol += incr;
            }
        }
        else
        {
            for ( ; ; p = p.plus(us_ptr2len_cc(p)))
            {
                /* A tab gets expanded, depending on the current column. */
                head[0] = 0;
                incr = win_lbr_chartabsize(wp, line, p, vcol, head);
                /* make sure we don't go past the end of the line */
                if (p.at(0) == NUL)
                {
                    incr = 1;       /* NUL at end of line only takes one column */
                    break;
                }

                if (posptr != null && BLE(posptr, p))  /* character at pos.col */
                    break;

                vcol += incr;
            }
        }
        if (start != null)
            start[0] = vcol + head[0];
        if (end != null)
            end[0] = vcol + incr - 1;
        if (cursor != null)
        {
            if (p.at(0) == TAB
                    && (State & NORMAL) != 0
                    && !wp.w_onebuf_opt.wo_list[0]
                    && !virtual_active()
                    && !(VIsual_active && (p_sel[0].at(0) == (byte)'e' || ltoreq(pos, VIsual))))
                cursor[0] = vcol + incr - 1;      /* cursor at end */
            else
                cursor[0] = vcol + head[0];          /* cursor at start */
        }
    }

    /*
     * Get virtual cursor column in the current window, pretending 'list' is off.
     */
    /*private*/ static int getvcol_nolist(pos_C posp)
    {
        boolean list_save = curwin.w_onebuf_opt.wo_list[0];
        curwin.w_onebuf_opt.wo_list[0] = false;

        int[] vcol = new int[1];
        getvcol(curwin, posp, null, vcol, null);

        curwin.w_onebuf_opt.wo_list[0] = list_save;

        return vcol[0];
    }

    /*
     * Get virtual column in virtual mode.
     */
    /*private*/ static void getvvcol(window_C wp, pos_C pos, int[] start, int[] cursor, int[] end)
    {
        if (virtual_active())
        {
            /* For virtual mode, only want one value. */
            int[] col = new int[1];
            getvcol(wp, pos, col, null, null);
            int coladd = pos.coladd;
            int endadd = 0;

            /* Cannot put the cursor on part of a wide character. */
            Bytes ptr = ml_get_buf(wp.w_buffer, pos.lnum, false);
            if (pos.col < strlen(ptr))
            {
                int c = us_ptr2char(ptr.plus(pos.col));

                if (c != TAB && vim_isprintc(c))
                {
                    endadd = mb_char2cells(c) - 1;
                    if (endadd < coladd)    /* past end of line */
                        endadd = 0;
                    else
                        coladd = 0;
                }
            }
            col[0] += coladd;
            if (start != null)
                start[0] = col[0];
            if (cursor != null)
                cursor[0] = col[0];
            if (end != null)
                end[0] = col[0] + endadd;
        }
        else
            getvcol(wp, pos, start, cursor, end);
    }

    /*
     * Get the leftmost and rightmost virtual column of pos1 and pos2.
     * Used for Visual block mode.
     */
    /*private*/ static void getvcols(window_C wp, pos_C pos1, pos_C pos2, int[] left, int[] right)
    {
        int[] from1 = new int[1];
        int[] from2 = new int[1];
        int[] to1 = new int[1];
        int[] to2 = new int[1];

        if (ltpos(pos1, pos2))
        {
            getvvcol(wp, pos1, from1, null, to1);
            getvvcol(wp, pos2, from2, null, to2);
        }
        else
        {
            getvvcol(wp, pos2, from1, null, to1);
            getvvcol(wp, pos1, from2, null, to2);
        }
        if (from2[0] < from1[0])
            left[0] = from2[0];
        else
            left[0] = from1[0];
        if (to1[0] < to2[0])
        {
            if (p_sel[0].at(0) == (byte)'e' && to1[0] <= from2[0] - 1)
                right[0] = from2[0] - 1;
            else
                right[0] = to2[0];
        }
        else
            right[0] = to1[0];
    }

    /*
     * Skip over ' ' and '\t'.
     */
    /*private*/ static Bytes skipwhite(Bytes q)
    {
        Bytes p = q;

        while (vim_iswhite(p.at(0))) /* skip to next non-white */
            p = p.plus(1);
        return p;
    }

    /*
     * skip over digits
     */
    /*private*/ static Bytes skipdigits(Bytes q)
    {
        Bytes p = q;

        while (asc_isdigit(p.at(0)))     /* skip to next non-digit */
            p = p.plus(1);
        return p;
    }

    /*
     * skip over digits and hex characters
     */
    /*private*/ static Bytes skiphex(Bytes q)
    {
        Bytes p = q;

        while (asc_isxdigit(p.at(0)))    /* skip to next non-digit */
            p = p.plus(1);
        return p;
    }

    /*
     * skip to digit (or NUL after the string)
     */
    /*private*/ static Bytes skiptodigit(Bytes q)
    {
        Bytes p = q;

        while (p.at(0) != NUL && !asc_isdigit(p.at(0)))       /* skip to next digit */
            p = p.plus(1);
        return p;
    }

    /*
     * skip to hex character (or NUL after the string)
     */
    /*private*/ static Bytes skiptohex(Bytes q)
    {
        Bytes p = q;

        while (p.at(0) != NUL && !asc_isxdigit(p.at(0)))      /* skip to next digit */
            p = p.plus(1);
        return p;
    }

    /*
     * Variant of isdigit() that can handle characters > 0x100.
     * We don't use isdigit() here, because on some systems it also considers
     * superscript 1 to be a digit.
     */
    /*private*/ static boolean asc_isdigit(int c)
    {
        return ('0' <= c && c <= '9');
    }

    /*private*/ static boolean asc_isodigit(int c)
    {
        return ('0' <= c && c <= '7');
    }

    /*
     * Variant of isxdigit() that can handle characters > 0x100.
     * We don't use isxdigit() here, because on some systems it also considers
     * superscript 1 to be a digit.
     */
    /*private*/ static boolean asc_isxdigit(int c)
    {
        return ('0' <= c && c <= '9')
            || ('a' <= c && c <= 'f')
            || ('A' <= c && c <= 'F');
    }

    /*
     * Skip over text until ' ' or '\t' or NUL.
     */
    /*private*/ static Bytes skiptowhite(Bytes p)
    {
        while (p.at(0) != (byte)' ' && p.at(0) != (byte)'\t' && p.at(0) != NUL)
            p = p.plus(1);
        return p;
    }

    /*
     * Like skiptowhite(), but also skip escaped chars.
     */
    /*private*/ static Bytes skiptowhite_esc(Bytes p)
    {
        while (p.at(0) != (byte)' ' && p.at(0) != (byte)'\t' && p.at(0) != NUL)
        {
            if ((p.at(0) == (byte)'\\' || p.at(0) == Ctrl_V) && p.at(1) != NUL)
                p = p.plus(1);
            p = p.plus(1);
        }
        return p;
    }

    /*
     * Getdigits: Get a number from a string and skip over it.
     * Note: the argument is a pointer to a byte pointer!
     */
    /*private*/ static long getdigits(Bytes[] pp)
    {
        Bytes p = pp[0];
        long retval = libC.atol(p);
        if (p.at(0) == (byte)'-')                  /* skip negative sign */
            p = p.plus(1);
        p = skipdigits(p);              /* skip to next non-digit */
        pp[0] = p;

        return retval;
    }

    /*
     * Return true if "lbuf" is empty or only contains blanks.
     */
    /*private*/ static boolean vim_isblankline(Bytes lbuf)
    {
        Bytes p = skipwhite(lbuf);
        return (p.at(0) == NUL || p.at(0) == (byte)'\r' || p.at(0) == (byte)'\n');
    }

    /*
     * Convert a string into a long and/or unsigned long, taking care of
     * hexadecimal and octal numbers.  Accepts a '-' sign.
     * If "hexp" is not null, returns a flag to indicate the type of the number:
     *  0       decimal
     *  '0'     octal
     *  'X'     hex
     *  'x'     hex
     * If "len" is not null, the length of the number in characters is returned.
     * If "nptr" is not null, the signed result is returned in it.
     * If "unptr" is not null, the unsigned result is returned in it.
     * If "dooct" is non-zero recognize octal numbers, when > 1 always assume octal number.
     * If "dohex" is non-zero recognize hex numbers, when > 1 always assume hex number.
     */
    /*private*/ static void vim_str2nr(Bytes start, int[] hexp, int[] len, int dooct, int dohex, long[] nptr)
        /* hexp: return: type of number 0 = decimal, 'x' or 'X' is hex, '0' = octal */
        /* len: return: detected length of number */
        /* dooct: recognize octal number */
        /* dohex: recognize hex number */
        /* nptr: return: signed result */
    {
        Bytes ptr = start;

        boolean negative = false;
        if (ptr.at(0) == (byte)'-')
        {
            negative = true;
            ptr = ptr.plus(1);
        }

        int hex = 0;                        /* default is decimal */

        /* Recognize hex and octal. */
        if (ptr.at(0) == (byte)'0' && ptr.at(1) != (byte)'8' && ptr.at(1) != (byte)'9')
        {
            hex = ptr.at(1);
            if (dohex != 0 && (hex == 'X' || hex == 'x') && asc_isxdigit(ptr.at(2)))
                ptr = ptr.plus(2);                   /* hexadecimal */
            else
            {
                hex = 0;                    /* default is decimal */
                if (dooct != 0)
                {
                    /* Don't interpret "0", "08" or "0129" as octal. */
                    for (int n = 1; asc_isdigit(ptr.at(n)); n++)
                    {
                        if ('7' < ptr.at(n))
                        {
                            hex = 0;        /* can't be octal */
                            break;
                        }
                        if ('0' <= ptr.at(n))
                            hex = '0';      /* assume octal */
                    }
                }
            }
        }

        long nr = 0;

        /*
         * Do the string-to-numeric conversion "manually" to avoid sscanf quirks.
         */
        if (hex == '0' || 1 < dooct)
        {
            for ( ; asc_isodigit(ptr.at(0)); ptr = ptr.plus(1))          /* octal */
            {
                long l = 8 * nr + (long)(ptr.at(0) - '0');
                if (l < nr)
                    break;
                nr = l;
            }
        }
        else if (hex != 0 || 1 < dohex)
        {
            for ( ; asc_isxdigit(ptr.at(0)); ptr = ptr.plus(1))          /* hex */
            {
                long l = 16 * nr + (long)hex2nr(ptr.at(0));
                if (l < nr)
                    break;
                nr = l;
            }
        }
        else
        {
            for ( ; asc_isdigit(ptr.at(0)); ptr = ptr.plus(1))           /* decimal */
            {
                long l = 10 * nr + (long)(ptr.at(0) - '0');
                if (l < nr)
                    break;
                nr = l;
            }
        }

        if (hexp != null)
            hexp[0] = hex;
        if (len != null)
            len[0] = BDIFF(ptr, start);
        if (nptr != null)
        {
            if (negative)               /* account for leading '-' for decimal numbers */
                nptr[0] = -nr;
            else
                nptr[0] = nr;
        }
    }

    /*
     * Return the value of a single hex character.
     * Only valid when the argument is '0' - '9', 'A' - 'F' or 'a' - 'f'.
     */
    /*private*/ static int hex2nr(int c)
    {
        if ('a' <= c && c <= 'f')
            return c - 'a' + 10;
        if ('A' <= c && c <= 'F')
            return c - 'A' + 10;

        return c - '0';
    }

    /*
     * Convert two hex characters to a byte.
     * Return -1 if one of the characters is not hex.
     */
    /*private*/ static int hexhex2nr(Bytes p)
    {
        if (!asc_isxdigit(p.at(0)) || !asc_isxdigit(p.at(1)))
            return -1;

        return (hex2nr(p.at(0)) << 4) + hex2nr(p.at(1));
    }

    /*
     * Return true if "str" starts with a backslash that should be removed.
     */
    /*private*/ static boolean rem_backslash(Bytes str)
    {
        return (str.at(0) == (byte)'\\' && str.at(1) != NUL);
    }

    /*
     * Halve the number of backslashes in a file name argument.
     * For MS-DOS we only do this if the character after the backslash
     * is not a normal file character.
     */
    /*private*/ static void backslash_halve(Bytes p)
    {
        for ( ; p.at(0) != NUL; p = p.plus(1))
            if (rem_backslash(p))
                BCOPY(p, 0, p, 1, strlen(p, 1) + 1);
    }

    /*
     * backslash_halve() plus save the result in allocated memory.
     */
    /*private*/ static Bytes backslash_halve_save(Bytes p)
    {
        Bytes res = STRDUP(p);
        backslash_halve(res);
        return res;
    }
}
