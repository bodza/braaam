/*
 * mbyte.c: Code specifically for handling multi-byte characters.
 *
 * "enc_utf8"       Use Unicode characters in UTF-8 encoding.
 *                  The cell width on the display needs to be determined from
 *                  the character value.
 *                  Recognizing bytes is easy: 0xxx.xxxx is a single-byte
 *                  char, 10xx.xxxx is a trailing byte, 11xx.xxxx is a leading
 *                  byte of a multi-byte character.
 *                  To make things complicated, up to six composing characters
 *                  are allowed.  These are drawn on top of the first char.
 *                  For most editing the sequence of bytes with composing
 *                  characters included is considered to be one character.
 *
 * 'encoding' specifies the encoding used in the core.  This is in registers,
 * text manipulation, buffers, etc.  Conversion has to be done when characters
 * in another encoding are received or send:
 *
 *                     clipboard
 *                         ^
 *                         | (2)
 *                         V
 *                 +---------------+
 *            (1)  |               | (3)
 *  keyboard ----->|     core      |-----> display
 *                 |               |
 *                 +---------------+
 *                         ^
 *                         | (4)
 *                         V
 *                       file
 *
 * (1) Typed characters arrive in the current locale.  Conversion is to be
 *     done when 'encoding' is different from 'termencoding'.
 * (2) Text will be made available with the encoding specified with
 *     'encoding'.  If this is not sufficient, system-specific conversion
 *     might be required.
 * (3) For the GUI the correct font must be selected, no conversion done.
 *     Otherwise, conversion is to be done when 'encoding' differs from
 *     'termencoding'.
 * (4) The encoding of the file is specified with 'fileencoding'.  Conversion
 *     is to be done when it's different from 'encoding'.
 */

#include "vim.h"

#define WINBYTE BYTE

#include <wchar.h>

static int enc_canon_search(char_u *name);
static int utf_ptr2cells_len(char_u *p, int size);
static int utf_safe_read_char_adv(char_u **s, size_t *n);

/*
 * Lookup table to quickly get the length in bytes of a UTF-8 character from
 * the first byte of a UTF-8 string.
 * Bytes which are illegal when used as the first byte have a 1.
 * The NUL byte has length 1.
 */
static char utf8len_tab[256] =
{
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
    2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
    3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,4,4,4,4,4,4,4,4,5,5,5,5,6,6,1,1,
};

/*
 * Like utf8len_tab above, but using a zero for illegal lead bytes.
 */
static char utf8len_tab_zero[256] =
{
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
    1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
    2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
    3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,4,4,4,4,4,4,4,4,5,5,5,5,6,6,0,0,
};

/*
 * Canonical encoding names and their properties.
 */
static struct
{   char *name;         int prop;               int ___codepage;}
enc_canon_table[] =
{
    {"utf-8",           ENC_UNICODE,            0},
#define IDX_COUNT       1
};

/*
 * Find encoding "name" in the list of canonical encoding names.
 * Returns -1 if not found.
 */
    static int
enc_canon_search(name)
    char_u      *name;
{
    int         i;

    for (i = 0; i < IDX_COUNT; ++i)
        if (STRCMP(name, enc_canon_table[i].name) == 0)
            return i;
    return -1;
}

/*
 * Find canonical encoding "name" in the list and return its properties.
 * Returns 0 if not found.
 */
    int
enc_canon_props(name)
    char_u      *name;
{
    int         i;

    i = enc_canon_search(name);
    if (i >= 0)
        return enc_canon_table[i].prop;
    return 0;
}

/*
 * Set up for using multi-byte characters.
 * Called in two cases:
 * - by main() to initialize "early"
 * - by set_init_1() after 'encoding' was set to its default.
 * Fills mb_bytelen_tab[].
 */
    void
mb_init(early)
    int         early;
{
    int         i;
    int         n;

    if (early)
    {
        /* Just starting up: set the whole table to one's. */
        for (i = 0; i < 256; ++i)
            mb_bytelen_tab[i] = 1;

        return;
    }

    /*
     * Fill the mb_bytelen_tab[] for MB_BYTE2LEN().
     */
    for (i = 0; i < 256; ++i)
    {
        /* Our own to reliably check the length of UTF-8 characters, independent of mblen(). */
        n = utf8len_tab[i];

        mb_bytelen_tab[i] = n;
    }

    /* The cell width depends on the type of multi-byte characters. */
    (void)init_chartab();

    /* When enc_utf8 is set or reset, (de)allocate ScreenLinesUC[] */
    screenalloc(FALSE);

    /* When using Unicode, set default for 'fileencodings'. */
    if (!option_was_set((char_u *)"fencs"))
        set_string_option_direct((char_u *)"fencs", -1, (char_u *)"ucs-bom,utf-8", OPT_FREE, 0);
}

/*
 * Return the size of the BOM for the current buffer:
 * 0 - no BOM
 * 2 - UCS-2 or UTF-16 BOM
 * 4 - UCS-4 BOM
 * 3 - UTF-8 BOM
 */
    int
bomb_size()
{
    int n = 0;

    if (curbuf->b_p_bomb && !curbuf->b_p_bin)
    {
        if (*curbuf->b_p_fenc == NUL)
            n = 3;
        else if (STRCMP(curbuf->b_p_fenc, "utf-8") == 0)
            n = 3;
        else if (STRNCMP(curbuf->b_p_fenc, "ucs-2", 5) == 0
              || STRNCMP(curbuf->b_p_fenc, "utf-16", 6) == 0)
            n = 2;
        else if (STRNCMP(curbuf->b_p_fenc, "ucs-4", 5) == 0)
            n = 4;
    }
    return n;
}

/*
 * Remove all BOM from "s" by moving remaining text.
 */
    void
remove_bom(s)
    char_u *s;
{
    char_u *p = s;

    while ((p = vim_strbyte(p, 0xef)) != NULL)
    {
        if (p[1] == 0xbb && p[2] == 0xbf)
            STRMOVE(p, p + 3);
        else
            ++p;
    }
}

/*
 * Get class of pointer:
 * 0 for blank or NUL
 * 1 for punctuation
 * 2 for an (ASCII) word character
 * >2 for other word characters
 */
    int
mb_get_class(p)
    char_u      *p;
{
    return mb_get_class_buf(p, curbuf);
}

    int
mb_get_class_buf(p, buf)
    char_u      *p;
    buf_T       *buf;
{
    if (MB_BYTE2LEN(p[0]) == 1)
    {
        if (p[0] == NUL || vim_iswhite(p[0]))
            return 0;
        if (vim_iswordc_buf(p[0], buf))
            return 2;
        return 1;
    }
    return utf_class(utf_ptr2char(p));
}

struct interval
{
    long first;
    long last;
};
static int intable(struct interval *table, size_t size, int c);

/*
 * Return TRUE if "c" is in "table[size / sizeof(struct interval)]".
 */
    static int
intable(table, size, c)
    struct interval     *table;
    size_t              size;
    int                 c;
{
    int mid, bot, top;

    /* first quick check for Latin1 etc. characters */
    if (c < table[0].first)
        return FALSE;

    /* binary search in table */
    bot = 0;
    top = (int)(size / sizeof(struct interval) - 1);
    while (top >= bot)
    {
        mid = (bot + top) / 2;
        if (table[mid].last < c)
            bot = mid + 1;
        else if (table[mid].first > c)
            top = mid - 1;
        else
            return TRUE;
    }
    return FALSE;
}

/*
 * For UTF-8 character "c" return 2 for a double-width character, 1 for others.
 * Returns 4 or 6 for an unprintable character.
 * Is only correct for characters >= 0x80.
 * When p_ambw is "double", return 2 for a character with East Asian Width
 * class 'A'(mbiguous).
 */
    int
utf_char2cells(c)
    int         c;
{
    /* Sorted list of non-overlapping intervals of East Asian double width
     * characters, generated with ../runtime/tools/unicode.vim. */
    static struct interval doublewidth[] =
    {
        {0x1100, 0x115f},
        {0x2329, 0x232a},
        {0x2e80, 0x2e99},
        {0x2e9b, 0x2ef3},
        {0x2f00, 0x2fd5},
        {0x2ff0, 0x2ffb},
        {0x3000, 0x303e},
        {0x3041, 0x3096},
        {0x3099, 0x30ff},
        {0x3105, 0x312d},
        {0x3131, 0x318e},
        {0x3190, 0x31ba},
        {0x31c0, 0x31e3},
        {0x31f0, 0x321e},
        {0x3220, 0x3247},
        {0x3250, 0x32fe},
        {0x3300, 0x4dbf},
        {0x4e00, 0xa48c},
        {0xa490, 0xa4c6},
        {0xa960, 0xa97c},
        {0xac00, 0xd7a3},
        {0xf900, 0xfaff},
        {0xfe10, 0xfe19},
        {0xfe30, 0xfe52},
        {0xfe54, 0xfe66},
        {0xfe68, 0xfe6b},
        {0xff01, 0xff60},
        {0xffe0, 0xffe6},
        {0x1b000, 0x1b001},
        {0x1f200, 0x1f202},
        {0x1f210, 0x1f23a},
        {0x1f240, 0x1f248},
        {0x1f250, 0x1f251},
        {0x20000, 0x2fffd},
        {0x30000, 0x3fffd}
    };

    /* Sorted list of non-overlapping intervals of East Asian Ambiguous
     * characters, generated with ../runtime/tools/unicode.vim. */
    static struct interval ambiguous[] =
    {
        {0x00a1, 0x00a1},
        {0x00a4, 0x00a4},
        {0x00a7, 0x00a8},
        {0x00aa, 0x00aa},
        {0x00ad, 0x00ae},
        {0x00b0, 0x00b4},
        {0x00b6, 0x00ba},
        {0x00bc, 0x00bf},
        {0x00c6, 0x00c6},
        {0x00d0, 0x00d0},
        {0x00d7, 0x00d8},
        {0x00de, 0x00e1},
        {0x00e6, 0x00e6},
        {0x00e8, 0x00ea},
        {0x00ec, 0x00ed},
        {0x00f0, 0x00f0},
        {0x00f2, 0x00f3},
        {0x00f7, 0x00fa},
        {0x00fc, 0x00fc},
        {0x00fe, 0x00fe},
        {0x0101, 0x0101},
        {0x0111, 0x0111},
        {0x0113, 0x0113},
        {0x011b, 0x011b},
        {0x0126, 0x0127},
        {0x012b, 0x012b},
        {0x0131, 0x0133},
        {0x0138, 0x0138},
        {0x013f, 0x0142},
        {0x0144, 0x0144},
        {0x0148, 0x014b},
        {0x014d, 0x014d},
        {0x0152, 0x0153},
        {0x0166, 0x0167},
        {0x016b, 0x016b},
        {0x01ce, 0x01ce},
        {0x01d0, 0x01d0},
        {0x01d2, 0x01d2},
        {0x01d4, 0x01d4},
        {0x01d6, 0x01d6},
        {0x01d8, 0x01d8},
        {0x01da, 0x01da},
        {0x01dc, 0x01dc},
        {0x0251, 0x0251},
        {0x0261, 0x0261},
        {0x02c4, 0x02c4},
        {0x02c7, 0x02c7},
        {0x02c9, 0x02cb},
        {0x02cd, 0x02cd},
        {0x02d0, 0x02d0},
        {0x02d8, 0x02db},
        {0x02dd, 0x02dd},
        {0x02df, 0x02df},
        {0x0300, 0x036f},
        {0x0391, 0x03a1},
        {0x03a3, 0x03a9},
        {0x03b1, 0x03c1},
        {0x03c3, 0x03c9},
        {0x0401, 0x0401},
        {0x0410, 0x044f},
        {0x0451, 0x0451},
        {0x2010, 0x2010},
        {0x2013, 0x2016},
        {0x2018, 0x2019},
        {0x201c, 0x201d},
        {0x2020, 0x2022},
        {0x2024, 0x2027},
        {0x2030, 0x2030},
        {0x2032, 0x2033},
        {0x2035, 0x2035},
        {0x203b, 0x203b},
        {0x203e, 0x203e},
        {0x2074, 0x2074},
        {0x207f, 0x207f},
        {0x2081, 0x2084},
        {0x20ac, 0x20ac},
        {0x2103, 0x2103},
        {0x2105, 0x2105},
        {0x2109, 0x2109},
        {0x2113, 0x2113},
        {0x2116, 0x2116},
        {0x2121, 0x2122},
        {0x2126, 0x2126},
        {0x212b, 0x212b},
        {0x2153, 0x2154},
        {0x215b, 0x215e},
        {0x2160, 0x216b},
        {0x2170, 0x2179},
        {0x2189, 0x2189},
        {0x2190, 0x2199},
        {0x21b8, 0x21b9},
        {0x21d2, 0x21d2},
        {0x21d4, 0x21d4},
        {0x21e7, 0x21e7},
        {0x2200, 0x2200},
        {0x2202, 0x2203},
        {0x2207, 0x2208},
        {0x220b, 0x220b},
        {0x220f, 0x220f},
        {0x2211, 0x2211},
        {0x2215, 0x2215},
        {0x221a, 0x221a},
        {0x221d, 0x2220},
        {0x2223, 0x2223},
        {0x2225, 0x2225},
        {0x2227, 0x222c},
        {0x222e, 0x222e},
        {0x2234, 0x2237},
        {0x223c, 0x223d},
        {0x2248, 0x2248},
        {0x224c, 0x224c},
        {0x2252, 0x2252},
        {0x2260, 0x2261},
        {0x2264, 0x2267},
        {0x226a, 0x226b},
        {0x226e, 0x226f},
        {0x2282, 0x2283},
        {0x2286, 0x2287},
        {0x2295, 0x2295},
        {0x2299, 0x2299},
        {0x22a5, 0x22a5},
        {0x22bf, 0x22bf},
        {0x2312, 0x2312},
        {0x2460, 0x24e9},
        {0x24eb, 0x254b},
        {0x2550, 0x2573},
        {0x2580, 0x258f},
        {0x2592, 0x2595},
        {0x25a0, 0x25a1},
        {0x25a3, 0x25a9},
        {0x25b2, 0x25b3},
        {0x25b6, 0x25b7},
        {0x25bc, 0x25bd},
        {0x25c0, 0x25c1},
        {0x25c6, 0x25c8},
        {0x25cb, 0x25cb},
        {0x25ce, 0x25d1},
        {0x25e2, 0x25e5},
        {0x25ef, 0x25ef},
        {0x2605, 0x2606},
        {0x2609, 0x2609},
        {0x260e, 0x260f},
        {0x2614, 0x2615},
        {0x261c, 0x261c},
        {0x261e, 0x261e},
        {0x2640, 0x2640},
        {0x2642, 0x2642},
        {0x2660, 0x2661},
        {0x2663, 0x2665},
        {0x2667, 0x266a},
        {0x266c, 0x266d},
        {0x266f, 0x266f},
        {0x269e, 0x269f},
        {0x26be, 0x26bf},
        {0x26c4, 0x26cd},
        {0x26cf, 0x26e1},
        {0x26e3, 0x26e3},
        {0x26e8, 0x26ff},
        {0x273d, 0x273d},
        {0x2757, 0x2757},
        {0x2776, 0x277f},
        {0x2b55, 0x2b59},
        {0x3248, 0x324f},
        {0xe000, 0xf8ff},
        {0xfe00, 0xfe0f},
        {0xfffd, 0xfffd},
        {0x1f100, 0x1f10a},
        {0x1f110, 0x1f12d},
        {0x1f130, 0x1f169},
        {0x1f170, 0x1f19a},
        {0xe0100, 0xe01ef},
        {0xf0000, 0xffffd},
        {0x100000, 0x10fffd}
    };

    if (c >= 0x100)
    {
        if (!utf_printable(c))
            return 6;           /* unprintable, displays <xxxx> */
        if (intable(doublewidth, sizeof(doublewidth), c))
            return 2;
    }

    /* Characters below 0x100 are influenced by 'isprint' option */
    else if (c >= 0x80 && !vim_isprintc(c))
        return 4;               /* unprintable, displays <xx> */

    if (c >= 0x80 && *p_ambw == 'd' && intable(ambiguous, sizeof(ambiguous), c))
        return 2;

    return 1;
}

    int
utf_ptr2cells(p)
    char_u      *p;
{
    int         c;

    /* Need to convert to a wide character. */
    if (*p >= 0x80)
    {
        c = utf_ptr2char(p);
        /* An illegal byte is displayed as <xx>. */
        if (utf_ptr2len(p) == 1 || c == NUL)
            return 4;
        /* If the char is ASCII it must be an overlong sequence. */
        if (c < 0x80)
            return char2cells(c);
        return utf_char2cells(c);
    }
    return 1;
}

    static int
utf_ptr2cells_len(p, size)
    char_u      *p;
    int         size;
{
    int         c;

    /* Need to convert to a wide character. */
    if (size > 0 && *p >= 0x80)
    {
        if (utf_ptr2len_len(p, size) < utf8len_tab[*p])
            return 1;  /* truncated */
        c = utf_ptr2char(p);
        /* An illegal byte is displayed as <xx>. */
        if (utf_ptr2len(p) == 1 || c == NUL)
            return 4;
        /* If the char is ASCII it must be an overlong sequence. */
        if (c < 0x80)
            return char2cells(c);
        return utf_char2cells(c);
    }
    return 1;
}

/*
 * Return the number of cells occupied by string "p".
 * Stop at a NUL character.  When "len" >= 0 stop at character "p[len]".
 */
    int
mb_string2cells(p, len)
    char_u  *p;
    int     len;
{
    int i;
    int clen = 0;

    for (i = 0; (len < 0 || i < len) && p[i] != NUL; i += utfc_ptr2len(p + i))
        clen += utf_ptr2cells(p + i);
    return clen;
}

    int
utf_off2cells(off, max_off)
    unsigned    off;
    unsigned    max_off;
{
    return (off + 1 < max_off && ScreenLines[off + 1] == 0) ? 2 : 1;
}

/*
 * Convert a UTF-8 byte sequence to a wide character.
 * If the sequence is illegal or truncated by a NUL the first byte is returned.
 * Does not include composing characters, of course.
 */
    int
utf_ptr2char(p)
    char_u      *p;
{
    int         len;

    if (p[0] < 0x80)    /* be quick for ASCII */
        return p[0];

    len = utf8len_tab_zero[p[0]];
    if (len > 1 && (p[1] & 0xc0) == 0x80)
    {
        if (len == 2)
            return ((p[0] & 0x1f) << 6) + (p[1] & 0x3f);
        if ((p[2] & 0xc0) == 0x80)
        {
            if (len == 3)
                return ((p[0] & 0x0f) << 12) + ((p[1] & 0x3f) << 6) + (p[2] & 0x3f);
            if ((p[3] & 0xc0) == 0x80)
            {
                if (len == 4)
                    return ((p[0] & 0x07) << 18) + ((p[1] & 0x3f) << 12)
                        + ((p[2] & 0x3f) << 6) + (p[3] & 0x3f);
                if ((p[4] & 0xc0) == 0x80)
                {
                    if (len == 5)
                        return ((p[0] & 0x03) << 24) + ((p[1] & 0x3f) << 18)
                            + ((p[2] & 0x3f) << 12) + ((p[3] & 0x3f) << 6)
                            + (p[4] & 0x3f);
                    if ((p[5] & 0xc0) == 0x80 && len == 6)
                        return ((p[0] & 0x01) << 30) + ((p[1] & 0x3f) << 24)
                            + ((p[2] & 0x3f) << 18) + ((p[3] & 0x3f) << 12)
                            + ((p[4] & 0x3f) << 6) + (p[5] & 0x3f);
                }
            }
        }
    }
    /* Illegal value, just return the first byte */
    return p[0];
}

/*
 * Convert a UTF-8 byte sequence to a wide character.
 * String is assumed to be terminated by NUL or after "n" bytes, whichever comes first.
 * The function is safe in the sense that it never accesses memory beyond the
 * first "n" bytes of "s".
 *
 * On success, returns decoded codepoint, advances "s" to the beginning of
 * next character and decreases "n" accordingly.
 *
 * If end of string was reached, returns 0 and, if "n" > 0, advances "s" past NUL byte.
 *
 * If byte sequence is illegal or incomplete, returns -1 and does not advance "s".
 */
    static int
utf_safe_read_char_adv(s, n)
    char_u      **s;
    size_t      *n;
{
    int         c, k;

    if (*n == 0) /* end of buffer */
        return 0;

    k = utf8len_tab_zero[**s];

    if (k == 1)
    {
        /* ASCII character or NUL */
        (*n)--;
        return *(*s)++;
    }

    if ((size_t)k <= *n)
    {
        /* We have a multibyte sequence and it isn't truncated by buffer
         * limits so utf_ptr2char() is safe to use. Or the first byte is
         * illegal (k=0), and it's also safe to use utf_ptr2char(). */
        c = utf_ptr2char(*s);

        /* On failure, utf_ptr2char() returns the first byte, so here we
         * check equality with the first byte. The only non-ASCII character
         * which equals the first byte of its own UTF-8 representation is
         * U+00C3 (UTF-8: 0xC3 0x83), so need to check that special case too.
         * It's safe even if n=1, else we would have k=2 > n. */
        if (c != (int)(**s) || (c == 0xC3 && (*s)[1] == 0x83))
        {
            /* byte sequence was successfully decoded */
            *s += k;
            *n -= k;
            return c;
        }
    }

    /* byte sequence is incomplete or illegal */
    return -1;
}

/*
 * Get character at **pp and advance *pp to the next character.
 * Note: composing characters are skipped!
 */
    int
mb_ptr2char_adv(pp)
    char_u      **pp;
{
    int         c;

    c = utf_ptr2char(*pp);
    *pp += utfc_ptr2len(*pp);
    return c;
}

/*
 * Get character at **pp and advance *pp to the next character.
 * Note: composing characters are returned as separate characters.
 */
    int
mb_cptr2char_adv(pp)
    char_u      **pp;
{
    int         c;

    c = utf_ptr2char(*pp);
    *pp += utf_ptr2len(*pp);

    return c;
}

/*
 * Convert a UTF-8 byte string to a wide character.  Also get up to MAX_MCO
 * composing characters.
 */
    int
utfc_ptr2char(p, pcc)
    char_u      *p;
    int         *pcc;   /* return: composing chars, last one is 0 */
{
    int         len;
    int         c;
    int         cc;
    int         i = 0;

    c = utf_ptr2char(p);
    len = utf_ptr2len(p);

    /* Only accept a composing char when the first char isn't illegal. */
    if ((len > 1 || *p < 0x80) && p[len] >= 0x80 && UTF_COMPOSINGLIKE(p, p + len))
    {
        cc = utf_ptr2char(p + len);
        for (;;)
        {
            pcc[i++] = cc;
            if (i == MAX_MCO)
                break;
            len += utf_ptr2len(p + len);
            if (p[len] < 0x80 || !utf_iscomposing(cc = utf_ptr2char(p + len)))
                break;
        }
    }

    if (i < MAX_MCO)    /* last composing char must be 0 */
        pcc[i] = 0;

    return c;
}

/*
 * Convert a UTF-8 byte string to a wide character.  Also get up to MAX_MCO
 * composing characters.  Use no more than p[maxlen].
 */
    int
utfc_ptr2char_len(p, pcc, maxlen)
    char_u      *p;
    int         *pcc;   /* return: composing chars, last one is 0 */
    int         maxlen;
{
    int         len;
    int         c;
    int         cc;
    int         i = 0;

    c = utf_ptr2char(p);
    len = utf_ptr2len_len(p, maxlen);
    /* Only accept a composing char when the first char isn't illegal. */
    if ((len > 1 || *p < 0x80)
            && len < maxlen
            && p[len] >= 0x80
            && UTF_COMPOSINGLIKE(p, p + len))
    {
        cc = utf_ptr2char(p + len);
        for (;;)
        {
            pcc[i++] = cc;
            if (i == MAX_MCO)
                break;
            len += utf_ptr2len_len(p + len, maxlen - len);
            if (len >= maxlen
                    || p[len] < 0x80
                    || !utf_iscomposing(cc = utf_ptr2char(p + len)))
                break;
        }
    }

    if (i < MAX_MCO)    /* last composing char must be 0 */
        pcc[i] = 0;

    return c;
}

/*
 * Convert the character at screen position "off" to a sequence of bytes.
 * Includes the composing characters.
 * "buf" must at least have the length MB_MAXBYTES + 1.
 * Only to be used when ScreenLinesUC[off] != 0.
 * Returns the produced number of bytes.
 */
    int
utfc_char2bytes(off, buf)
    int         off;
    char_u      *buf;
{
    int         len;
    int         i;

    len = utf_char2bytes(ScreenLinesUC[off], buf);
    for (i = 0; i < Screen_mco; ++i)
    {
        if (ScreenLinesC[i][off] == 0)
            break;
        len += utf_char2bytes(ScreenLinesC[i][off], buf + len);
    }
    return len;
}

/*
 * Get the length of a UTF-8 byte sequence, not including any following
 * composing characters.
 * Returns 0 for "".
 * Returns 1 for an illegal byte sequence.
 */
    int
utf_ptr2len(p)
    char_u      *p;
{
    int         len;
    int         i;

    if (*p == NUL)
        return 0;
    len = utf8len_tab[*p];
    for (i = 1; i < len; ++i)
        if ((p[i] & 0xc0) != 0x80)
            return 1;
    return len;
}

/*
 * Return length of UTF-8 character, obtained from the first byte.
 * "b" must be between 0 and 255!
 * Returns 1 for an invalid first byte value.
 */
    int
utf_byte2len(b)
    int         b;
{
    return utf8len_tab[b];
}

/*
 * Get the length of UTF-8 byte sequence "p[size]".  Does not include any
 * following composing characters.
 * Returns 1 for "".
 * Returns 1 for an illegal byte sequence (also in incomplete byte seq.).
 * Returns number > "size" for an incomplete byte sequence.
 * Never returns zero.
 */
    int
utf_ptr2len_len(p, size)
    char_u      *p;
    int         size;
{
    int         len;
    int         i;
    int         m;

    len = utf8len_tab[*p];
    if (len == 1)
        return 1;       /* NUL, ascii or illegal lead byte */
    if (len > size)
        m = size;       /* incomplete byte sequence. */
    else
        m = len;
    for (i = 1; i < m; ++i)
        if ((p[i] & 0xc0) != 0x80)
            return 1;
    return len;
}

/*
 * Return the number of bytes the UTF-8 encoding of the character at "p" takes.
 * This includes following composing characters.
 */
    int
utfc_ptr2len(p)
    char_u      *p;
{
    int         len;
    int         b0 = *p;

    if (b0 == NUL)
        return 0;
    if (b0 < 0x80 && p[1] < 0x80)       /* be quick for ASCII */
        return 1;

    /* Skip over first UTF-8 char, stopping at a NUL byte. */
    len = utf_ptr2len(p);

    /* Check for illegal byte. */
    if (len == 1 && b0 >= 0x80)
        return 1;

    /*
     * Check for composing characters.  We can handle only the first six, but
     * skip all of them (otherwise the cursor would get stuck).
     */
    for (;;)
    {
        if (p[len] < 0x80 || !UTF_COMPOSINGLIKE(p + prevlen, p + len))
            return len;

        /* Skip over composing char */
        len += utf_ptr2len(p + len);
    }
}

/*
 * Return the number of bytes the UTF-8 encoding of the character at "p[size]"
 * takes.  This includes following composing characters.
 * Returns 0 for an empty string.
 * Returns 1 for an illegal char or an incomplete byte sequence.
 */
    int
utfc_ptr2len_len(p, size)
    char_u      *p;
    int         size;
{
    int         len;

    if (size < 1 || *p == NUL)
        return 0;
    if (p[0] < 0x80 && (size == 1 || p[1] < 0x80)) /* be quick for ASCII */
        return 1;

    /* Skip over first UTF-8 char, stopping at a NUL byte. */
    len = utf_ptr2len_len(p, size);

    /* Check for illegal byte and incomplete byte sequence. */
    if ((len == 1 && p[0] >= 0x80) || len > size)
        return 1;

    /*
     * Check for composing characters.  We can handle only the first six, but
     * skip all of them (otherwise the cursor would get stuck).
     */
    while (len < size)
    {
        int     len_next_char;

        if (p[len] < 0x80)
            break;

        /*
         * Next character length should not go beyond size to ensure that
         * UTF_COMPOSINGLIKE(...) does not read beyond size.
         */
        len_next_char = utf_ptr2len_len(p + len, size - len);
        if (len_next_char > size - len)
            break;

        if (!UTF_COMPOSINGLIKE(p + prevlen, p + len))
            break;

        /* Skip over composing char */
        len += len_next_char;
    }
    return len;
}

/*
 * Return the number of bytes the UTF-8 encoding of character "c" takes.
 * This does not include composing characters.
 */
    int
utf_char2len(c)
    int         c;
{
    if (c < 0x80)
        return 1;
    if (c < 0x800)
        return 2;
    if (c < 0x10000)
        return 3;
    if (c < 0x200000)
        return 4;
    if (c < 0x4000000)
        return 5;
    return 6;
}

/*
 * Convert Unicode character "c" to UTF-8 string in "buf[]".
 * Returns the number of bytes.
 * This does not include composing characters.
 */
    int
utf_char2bytes(c, buf)
    int         c;
    char_u      *buf;
{
    if (c < 0x80)               /* 7 bits */
    {
        buf[0] = c;
        return 1;
    }
    if (c < 0x800)              /* 11 bits */
    {
        buf[0] = 0xc0 + ((unsigned)c >> 6);
        buf[1] = 0x80 + (c & 0x3f);
        return 2;
    }
    if (c < 0x10000)            /* 16 bits */
    {
        buf[0] = 0xe0 + ((unsigned)c >> 12);
        buf[1] = 0x80 + (((unsigned)c >> 6) & 0x3f);
        buf[2] = 0x80 + (c & 0x3f);
        return 3;
    }
    if (c < 0x200000)           /* 21 bits */
    {
        buf[0] = 0xf0 + ((unsigned)c >> 18);
        buf[1] = 0x80 + (((unsigned)c >> 12) & 0x3f);
        buf[2] = 0x80 + (((unsigned)c >> 6) & 0x3f);
        buf[3] = 0x80 + (c & 0x3f);
        return 4;
    }
    if (c < 0x4000000)          /* 26 bits */
    {
        buf[0] = 0xf8 + ((unsigned)c >> 24);
        buf[1] = 0x80 + (((unsigned)c >> 18) & 0x3f);
        buf[2] = 0x80 + (((unsigned)c >> 12) & 0x3f);
        buf[3] = 0x80 + (((unsigned)c >> 6) & 0x3f);
        buf[4] = 0x80 + (c & 0x3f);
        return 5;
    }
                                /* 31 bits */
    buf[0] = 0xfc + ((unsigned)c >> 30);
    buf[1] = 0x80 + (((unsigned)c >> 24) & 0x3f);
    buf[2] = 0x80 + (((unsigned)c >> 18) & 0x3f);
    buf[3] = 0x80 + (((unsigned)c >> 12) & 0x3f);
    buf[4] = 0x80 + (((unsigned)c >> 6) & 0x3f);
    buf[5] = 0x80 + (c & 0x3f);
    return 6;
}

/*
 * Return TRUE if "c" is a composing UTF-8 character.  This means it will be
 * drawn on top of the preceding character.
 * Based on code from Markus Kuhn.
 */
    int
utf_iscomposing(c)
    int         c;
{
    /* Sorted list of non-overlapping intervals.
     * Generated by ../runtime/tools/unicode.vim. */
    static struct interval combining[] =
    {
        {0x0300, 0x036f},
        {0x0483, 0x0489},
        {0x0591, 0x05bd},
        {0x05bf, 0x05bf},
        {0x05c1, 0x05c2},
        {0x05c4, 0x05c5},
        {0x05c7, 0x05c7},
        {0x0610, 0x061a},
        {0x064b, 0x065f},
        {0x0670, 0x0670},
        {0x06d6, 0x06dc},
        {0x06df, 0x06e4},
        {0x06e7, 0x06e8},
        {0x06ea, 0x06ed},
        {0x0711, 0x0711},
        {0x0730, 0x074a},
        {0x07a6, 0x07b0},
        {0x07eb, 0x07f3},
        {0x0816, 0x0819},
        {0x081b, 0x0823},
        {0x0825, 0x0827},
        {0x0829, 0x082d},
        {0x0859, 0x085b},
        {0x08e4, 0x0903},
        {0x093a, 0x093c},
        {0x093e, 0x094f},
        {0x0951, 0x0957},
        {0x0962, 0x0963},
        {0x0981, 0x0983},
        {0x09bc, 0x09bc},
        {0x09be, 0x09c4},
        {0x09c7, 0x09c8},
        {0x09cb, 0x09cd},
        {0x09d7, 0x09d7},
        {0x09e2, 0x09e3},
        {0x0a01, 0x0a03},
        {0x0a3c, 0x0a3c},
        {0x0a3e, 0x0a42},
        {0x0a47, 0x0a48},
        {0x0a4b, 0x0a4d},
        {0x0a51, 0x0a51},
        {0x0a70, 0x0a71},
        {0x0a75, 0x0a75},
        {0x0a81, 0x0a83},
        {0x0abc, 0x0abc},
        {0x0abe, 0x0ac5},
        {0x0ac7, 0x0ac9},
        {0x0acb, 0x0acd},
        {0x0ae2, 0x0ae3},
        {0x0b01, 0x0b03},
        {0x0b3c, 0x0b3c},
        {0x0b3e, 0x0b44},
        {0x0b47, 0x0b48},
        {0x0b4b, 0x0b4d},
        {0x0b56, 0x0b57},
        {0x0b62, 0x0b63},
        {0x0b82, 0x0b82},
        {0x0bbe, 0x0bc2},
        {0x0bc6, 0x0bc8},
        {0x0bca, 0x0bcd},
        {0x0bd7, 0x0bd7},
        {0x0c00, 0x0c03},
        {0x0c3e, 0x0c44},
        {0x0c46, 0x0c48},
        {0x0c4a, 0x0c4d},
        {0x0c55, 0x0c56},
        {0x0c62, 0x0c63},
        {0x0c81, 0x0c83},
        {0x0cbc, 0x0cbc},
        {0x0cbe, 0x0cc4},
        {0x0cc6, 0x0cc8},
        {0x0cca, 0x0ccd},
        {0x0cd5, 0x0cd6},
        {0x0ce2, 0x0ce3},
        {0x0d01, 0x0d03},
        {0x0d3e, 0x0d44},
        {0x0d46, 0x0d48},
        {0x0d4a, 0x0d4d},
        {0x0d57, 0x0d57},
        {0x0d62, 0x0d63},
        {0x0d82, 0x0d83},
        {0x0dca, 0x0dca},
        {0x0dcf, 0x0dd4},
        {0x0dd6, 0x0dd6},
        {0x0dd8, 0x0ddf},
        {0x0df2, 0x0df3},
        {0x0e31, 0x0e31},
        {0x0e34, 0x0e3a},
        {0x0e47, 0x0e4e},
        {0x0eb1, 0x0eb1},
        {0x0eb4, 0x0eb9},
        {0x0ebb, 0x0ebc},
        {0x0ec8, 0x0ecd},
        {0x0f18, 0x0f19},
        {0x0f35, 0x0f35},
        {0x0f37, 0x0f37},
        {0x0f39, 0x0f39},
        {0x0f3e, 0x0f3f},
        {0x0f71, 0x0f84},
        {0x0f86, 0x0f87},
        {0x0f8d, 0x0f97},
        {0x0f99, 0x0fbc},
        {0x0fc6, 0x0fc6},
        {0x102b, 0x103e},
        {0x1056, 0x1059},
        {0x105e, 0x1060},
        {0x1062, 0x1064},
        {0x1067, 0x106d},
        {0x1071, 0x1074},
        {0x1082, 0x108d},
        {0x108f, 0x108f},
        {0x109a, 0x109d},
        {0x135d, 0x135f},
        {0x1712, 0x1714},
        {0x1732, 0x1734},
        {0x1752, 0x1753},
        {0x1772, 0x1773},
        {0x17b4, 0x17d3},
        {0x17dd, 0x17dd},
        {0x180b, 0x180d},
        {0x18a9, 0x18a9},
        {0x1920, 0x192b},
        {0x1930, 0x193b},
        {0x19b0, 0x19c0},
        {0x19c8, 0x19c9},
        {0x1a17, 0x1a1b},
        {0x1a55, 0x1a5e},
        {0x1a60, 0x1a7c},
        {0x1a7f, 0x1a7f},
        {0x1ab0, 0x1abe},
        {0x1b00, 0x1b04},
        {0x1b34, 0x1b44},
        {0x1b6b, 0x1b73},
        {0x1b80, 0x1b82},
        {0x1ba1, 0x1bad},
        {0x1be6, 0x1bf3},
        {0x1c24, 0x1c37},
        {0x1cd0, 0x1cd2},
        {0x1cd4, 0x1ce8},
        {0x1ced, 0x1ced},
        {0x1cf2, 0x1cf4},
        {0x1cf8, 0x1cf9},
        {0x1dc0, 0x1df5},
        {0x1dfc, 0x1dff},
        {0x20d0, 0x20f0},
        {0x2cef, 0x2cf1},
        {0x2d7f, 0x2d7f},
        {0x2de0, 0x2dff},
        {0x302a, 0x302f},
        {0x3099, 0x309a},
        {0xa66f, 0xa672},
        {0xa674, 0xa67d},
        {0xa69f, 0xa69f},
        {0xa6f0, 0xa6f1},
        {0xa802, 0xa802},
        {0xa806, 0xa806},
        {0xa80b, 0xa80b},
        {0xa823, 0xa827},
        {0xa880, 0xa881},
        {0xa8b4, 0xa8c4},
        {0xa8e0, 0xa8f1},
        {0xa926, 0xa92d},
        {0xa947, 0xa953},
        {0xa980, 0xa983},
        {0xa9b3, 0xa9c0},
        {0xa9e5, 0xa9e5},
        {0xaa29, 0xaa36},
        {0xaa43, 0xaa43},
        {0xaa4c, 0xaa4d},
        {0xaa7b, 0xaa7d},
        {0xaab0, 0xaab0},
        {0xaab2, 0xaab4},
        {0xaab7, 0xaab8},
        {0xaabe, 0xaabf},
        {0xaac1, 0xaac1},
        {0xaaeb, 0xaaef},
        {0xaaf5, 0xaaf6},
        {0xabe3, 0xabea},
        {0xabec, 0xabed},
        {0xfb1e, 0xfb1e},
        {0xfe00, 0xfe0f},
        {0xfe20, 0xfe2d},
        {0x101fd, 0x101fd},
        {0x102e0, 0x102e0},
        {0x10376, 0x1037a},
        {0x10a01, 0x10a03},
        {0x10a05, 0x10a06},
        {0x10a0c, 0x10a0f},
        {0x10a38, 0x10a3a},
        {0x10a3f, 0x10a3f},
        {0x10ae5, 0x10ae6},
        {0x11000, 0x11002},
        {0x11038, 0x11046},
        {0x1107f, 0x11082},
        {0x110b0, 0x110ba},
        {0x11100, 0x11102},
        {0x11127, 0x11134},
        {0x11173, 0x11173},
        {0x11180, 0x11182},
        {0x111b3, 0x111c0},
        {0x1122c, 0x11237},
        {0x112df, 0x112ea},
        {0x11301, 0x11303},
        {0x1133c, 0x1133c},
        {0x1133e, 0x11344},
        {0x11347, 0x11348},
        {0x1134b, 0x1134d},
        {0x11357, 0x11357},
        {0x11362, 0x11363},
        {0x11366, 0x1136c},
        {0x11370, 0x11374},
        {0x114b0, 0x114c3},
        {0x115af, 0x115b5},
        {0x115b8, 0x115c0},
        {0x11630, 0x11640},
        {0x116ab, 0x116b7},
        {0x16af0, 0x16af4},
        {0x16b30, 0x16b36},
        {0x16f51, 0x16f7e},
        {0x16f8f, 0x16f92},
        {0x1bc9d, 0x1bc9e},
        {0x1d165, 0x1d169},
        {0x1d16d, 0x1d172},
        {0x1d17b, 0x1d182},
        {0x1d185, 0x1d18b},
        {0x1d1aa, 0x1d1ad},
        {0x1d242, 0x1d244},
        {0x1e8d0, 0x1e8d6},
        {0xe0100, 0xe01ef}
    };

    return intable(combining, sizeof(combining), c);
}

/*
 * Return TRUE for characters that can be displayed in a normal way.
 * Only for characters of 0x100 and above!
 */
    int
utf_printable(c)
    int         c;
{
    /* Sorted list of non-overlapping intervals.
     * 0xd800-0xdfff is reserved for UTF-16, actually illegal. */
    static struct interval nonprint[] =
    {
        {0x070f, 0x070f}, {0x180b, 0x180e}, {0x200b, 0x200f}, {0x202a, 0x202e},
        {0x206a, 0x206f}, {0xd800, 0xdfff}, {0xfeff, 0xfeff}, {0xfff9, 0xfffb},
        {0xfffe, 0xffff}
    };

    return !intable(nonprint, sizeof(nonprint), c);
}

/*
 * Get class of a Unicode character.
 * 0: white space
 * 1: punctuation
 * 2 or bigger: some class of word character.
 */
    int
utf_class(c)
    int         c;
{
    /* sorted list of non-overlapping intervals */
    static struct clinterval
    {
        unsigned int first;
        unsigned int last;
        unsigned int class;
    } classes[] =
    {
        {0x037e, 0x037e, 1},            /* Greek question mark */
        {0x0387, 0x0387, 1},            /* Greek ano teleia */
        {0x055a, 0x055f, 1},            /* Armenian punctuation */
        {0x0589, 0x0589, 1},            /* Armenian full stop */
        {0x05be, 0x05be, 1},
        {0x05c0, 0x05c0, 1},
        {0x05c3, 0x05c3, 1},
        {0x05f3, 0x05f4, 1},
        {0x060c, 0x060c, 1},
        {0x061b, 0x061b, 1},
        {0x061f, 0x061f, 1},
        {0x066a, 0x066d, 1},
        {0x06d4, 0x06d4, 1},
        {0x0700, 0x070d, 1},            /* Syriac punctuation */
        {0x0964, 0x0965, 1},
        {0x0970, 0x0970, 1},
        {0x0df4, 0x0df4, 1},
        {0x0e4f, 0x0e4f, 1},
        {0x0e5a, 0x0e5b, 1},
        {0x0f04, 0x0f12, 1},
        {0x0f3a, 0x0f3d, 1},
        {0x0f85, 0x0f85, 1},
        {0x104a, 0x104f, 1},            /* Myanmar punctuation */
        {0x10fb, 0x10fb, 1},            /* Georgian punctuation */
        {0x1361, 0x1368, 1},            /* Ethiopic punctuation */
        {0x166d, 0x166e, 1},            /* Canadian Syl. punctuation */
        {0x1680, 0x1680, 0},
        {0x169b, 0x169c, 1},
        {0x16eb, 0x16ed, 1},
        {0x1735, 0x1736, 1},
        {0x17d4, 0x17dc, 1},            /* Khmer punctuation */
        {0x1800, 0x180a, 1},            /* Mongolian punctuation */
        {0x2000, 0x200b, 0},            /* spaces */
        {0x200c, 0x2027, 1},            /* punctuation and symbols */
        {0x2028, 0x2029, 0},
        {0x202a, 0x202e, 1},            /* punctuation and symbols */
        {0x202f, 0x202f, 0},
        {0x2030, 0x205e, 1},            /* punctuation and symbols */
        {0x205f, 0x205f, 0},
        {0x2060, 0x27ff, 1},            /* punctuation and symbols */
        {0x2070, 0x207f, 0x2070},       /* superscript */
        {0x2080, 0x2094, 0x2080},       /* subscript */
        {0x20a0, 0x27ff, 1},            /* all kinds of symbols */
        {0x2800, 0x28ff, 0x2800},       /* braille */
        {0x2900, 0x2998, 1},            /* arrows, brackets, etc. */
        {0x29d8, 0x29db, 1},
        {0x29fc, 0x29fd, 1},
        {0x2e00, 0x2e7f, 1},            /* supplemental punctuation */
        {0x3000, 0x3000, 0},            /* ideographic space */
        {0x3001, 0x3020, 1},            /* ideographic punctuation */
        {0x3030, 0x3030, 1},
        {0x303d, 0x303d, 1},
        {0x3040, 0x309f, 0x3040},       /* Hiragana */
        {0x30a0, 0x30ff, 0x30a0},       /* Katakana */
        {0x3300, 0x9fff, 0x4e00},       /* CJK Ideographs */
        {0xac00, 0xd7a3, 0xac00},       /* Hangul Syllables */
        {0xf900, 0xfaff, 0x4e00},       /* CJK Ideographs */
        {0xfd3e, 0xfd3f, 1},
        {0xfe30, 0xfe6b, 1},            /* punctuation forms */
        {0xff00, 0xff0f, 1},            /* half/fullwidth ASCII */
        {0xff1a, 0xff20, 1},            /* half/fullwidth ASCII */
        {0xff3b, 0xff40, 1},            /* half/fullwidth ASCII */
        {0xff5b, 0xff65, 1},            /* half/fullwidth ASCII */
        {0x20000, 0x2a6df, 0x4e00},     /* CJK Ideographs */
        {0x2a700, 0x2b73f, 0x4e00},     /* CJK Ideographs */
        {0x2b740, 0x2b81f, 0x4e00},     /* CJK Ideographs */
        {0x2f800, 0x2fa1f, 0x4e00},     /* CJK Ideographs */
    };
    int bot = 0;
    int top = sizeof(classes) / sizeof(struct clinterval) - 1;
    int mid;

    /* First quick check for Latin1 characters, use 'iskeyword'. */
    if (c < 0x100)
    {
        if (c == ' ' || c == '\t' || c == NUL || c == 0xa0)
            return 0;       /* blank */
        if (vim_iswordc(c))
            return 2;       /* word character */
        return 1;           /* punctuation */
    }

    /* binary search in table */
    while (top >= bot)
    {
        mid = (bot + top) / 2;
        if (classes[mid].last < (unsigned int)c)
            bot = mid + 1;
        else if (classes[mid].first > (unsigned int)c)
            top = mid - 1;
        else
            return (int)classes[mid].class;
    }

    /* most other characters are "word" characters */
    return 2;
}

/*
 * Code for Unicode case-dependent operations.  Based on notes in
 * http://www.unicode.org/Public/UNIDATA/CaseFolding.txt
 * This code uses simple case folding, not full case folding.
 * Last updated for Unicode 5.2.
 */

/*
 * The following tables are built by ../runtime/tools/unicode.vim.
 * They must be in numeric order, because we use binary search.
 * An entry such as {0x41,0x5a,1,32} means that Unicode characters in the
 * range from 0x41 to 0x5a inclusive, stepping by 1, are changed to
 * folded/upper/lower by adding 32.
 */
typedef struct
{
    int rangeStart;
    int rangeEnd;
    int step;
    int offset;
} convertStruct;

static convertStruct foldCase[] =
{
        {0x41,0x5a,1,32},
        {0xb5,0xb5,-1,775},
        {0xc0,0xd6,1,32},
        {0xd8,0xde,1,32},
        {0x100,0x12e,2,1},
        {0x132,0x136,2,1},
        {0x139,0x147,2,1},
        {0x14a,0x176,2,1},
        {0x178,0x178,-1,-121},
        {0x179,0x17d,2,1},
        {0x17f,0x17f,-1,-268},
        {0x181,0x181,-1,210},
        {0x182,0x184,2,1},
        {0x186,0x186,-1,206},
        {0x187,0x187,-1,1},
        {0x189,0x18a,1,205},
        {0x18b,0x18b,-1,1},
        {0x18e,0x18e,-1,79},
        {0x18f,0x18f,-1,202},
        {0x190,0x190,-1,203},
        {0x191,0x191,-1,1},
        {0x193,0x193,-1,205},
        {0x194,0x194,-1,207},
        {0x196,0x196,-1,211},
        {0x197,0x197,-1,209},
        {0x198,0x198,-1,1},
        {0x19c,0x19c,-1,211},
        {0x19d,0x19d,-1,213},
        {0x19f,0x19f,-1,214},
        {0x1a0,0x1a4,2,1},
        {0x1a6,0x1a6,-1,218},
        {0x1a7,0x1a7,-1,1},
        {0x1a9,0x1a9,-1,218},
        {0x1ac,0x1ac,-1,1},
        {0x1ae,0x1ae,-1,218},
        {0x1af,0x1af,-1,1},
        {0x1b1,0x1b2,1,217},
        {0x1b3,0x1b5,2,1},
        {0x1b7,0x1b7,-1,219},
        {0x1b8,0x1bc,4,1},
        {0x1c4,0x1c4,-1,2},
        {0x1c5,0x1c5,-1,1},
        {0x1c7,0x1c7,-1,2},
        {0x1c8,0x1c8,-1,1},
        {0x1ca,0x1ca,-1,2},
        {0x1cb,0x1db,2,1},
        {0x1de,0x1ee,2,1},
        {0x1f1,0x1f1,-1,2},
        {0x1f2,0x1f4,2,1},
        {0x1f6,0x1f6,-1,-97},
        {0x1f7,0x1f7,-1,-56},
        {0x1f8,0x21e,2,1},
        {0x220,0x220,-1,-130},
        {0x222,0x232,2,1},
        {0x23a,0x23a,-1,10795},
        {0x23b,0x23b,-1,1},
        {0x23d,0x23d,-1,-163},
        {0x23e,0x23e,-1,10792},
        {0x241,0x241,-1,1},
        {0x243,0x243,-1,-195},
        {0x244,0x244,-1,69},
        {0x245,0x245,-1,71},
        {0x246,0x24e,2,1},
        {0x345,0x345,-1,116},
        {0x370,0x372,2,1},
        {0x376,0x376,-1,1},
        {0x37f,0x37f,-1,116},
        {0x386,0x386,-1,38},
        {0x388,0x38a,1,37},
        {0x38c,0x38c,-1,64},
        {0x38e,0x38f,1,63},
        {0x391,0x3a1,1,32},
        {0x3a3,0x3ab,1,32},
        {0x3c2,0x3c2,-1,1},
        {0x3cf,0x3cf,-1,8},
        {0x3d0,0x3d0,-1,-30},
        {0x3d1,0x3d1,-1,-25},
        {0x3d5,0x3d5,-1,-15},
        {0x3d6,0x3d6,-1,-22},
        {0x3d8,0x3ee,2,1},
        {0x3f0,0x3f0,-1,-54},
        {0x3f1,0x3f1,-1,-48},
        {0x3f4,0x3f4,-1,-60},
        {0x3f5,0x3f5,-1,-64},
        {0x3f7,0x3f7,-1,1},
        {0x3f9,0x3f9,-1,-7},
        {0x3fa,0x3fa,-1,1},
        {0x3fd,0x3ff,1,-130},
        {0x400,0x40f,1,80},
        {0x410,0x42f,1,32},
        {0x460,0x480,2,1},
        {0x48a,0x4be,2,1},
        {0x4c0,0x4c0,-1,15},
        {0x4c1,0x4cd,2,1},
        {0x4d0,0x52e,2,1},
        {0x531,0x556,1,48},
        {0x10a0,0x10c5,1,7264},
        {0x10c7,0x10cd,6,7264},
        {0x1e00,0x1e94,2,1},
        {0x1e9b,0x1e9b,-1,-58},
        {0x1e9e,0x1e9e,-1,-7615},
        {0x1ea0,0x1efe,2,1},
        {0x1f08,0x1f0f,1,-8},
        {0x1f18,0x1f1d,1,-8},
        {0x1f28,0x1f2f,1,-8},
        {0x1f38,0x1f3f,1,-8},
        {0x1f48,0x1f4d,1,-8},
        {0x1f59,0x1f5f,2,-8},
        {0x1f68,0x1f6f,1,-8},
        {0x1f88,0x1f8f,1,-8},
        {0x1f98,0x1f9f,1,-8},
        {0x1fa8,0x1faf,1,-8},
        {0x1fb8,0x1fb9,1,-8},
        {0x1fba,0x1fbb,1,-74},
        {0x1fbc,0x1fbc,-1,-9},
        {0x1fbe,0x1fbe,-1,-7173},
        {0x1fc8,0x1fcb,1,-86},
        {0x1fcc,0x1fcc,-1,-9},
        {0x1fd8,0x1fd9,1,-8},
        {0x1fda,0x1fdb,1,-100},
        {0x1fe8,0x1fe9,1,-8},
        {0x1fea,0x1feb,1,-112},
        {0x1fec,0x1fec,-1,-7},
        {0x1ff8,0x1ff9,1,-128},
        {0x1ffa,0x1ffb,1,-126},
        {0x1ffc,0x1ffc,-1,-9},
        {0x2126,0x2126,-1,-7517},
        {0x212a,0x212a,-1,-8383},
        {0x212b,0x212b,-1,-8262},
        {0x2132,0x2132,-1,28},
        {0x2160,0x216f,1,16},
        {0x2183,0x2183,-1,1},
        {0x24b6,0x24cf,1,26},
        {0x2c00,0x2c2e,1,48},
        {0x2c60,0x2c60,-1,1},
        {0x2c62,0x2c62,-1,-10743},
        {0x2c63,0x2c63,-1,-3814},
        {0x2c64,0x2c64,-1,-10727},
        {0x2c67,0x2c6b,2,1},
        {0x2c6d,0x2c6d,-1,-10780},
        {0x2c6e,0x2c6e,-1,-10749},
        {0x2c6f,0x2c6f,-1,-10783},
        {0x2c70,0x2c70,-1,-10782},
        {0x2c72,0x2c75,3,1},
        {0x2c7e,0x2c7f,1,-10815},
        {0x2c80,0x2ce2,2,1},
        {0x2ceb,0x2ced,2,1},
        {0x2cf2,0xa640,31054,1},
        {0xa642,0xa66c,2,1},
        {0xa680,0xa69a,2,1},
        {0xa722,0xa72e,2,1},
        {0xa732,0xa76e,2,1},
        {0xa779,0xa77b,2,1},
        {0xa77d,0xa77d,-1,-35332},
        {0xa77e,0xa786,2,1},
        {0xa78b,0xa78b,-1,1},
        {0xa78d,0xa78d,-1,-42280},
        {0xa790,0xa792,2,1},
        {0xa796,0xa7a8,2,1},
        {0xa7aa,0xa7aa,-1,-42308},
        {0xa7ab,0xa7ab,-1,-42319},
        {0xa7ac,0xa7ac,-1,-42315},
        {0xa7ad,0xa7ad,-1,-42305},
        {0xa7b0,0xa7b0,-1,-42258},
        {0xa7b1,0xa7b1,-1,-42282},
        {0xff21,0xff3a,1,32},
        {0x10400,0x10427,1,40},
        {0x118a0,0x118bf,1,32}
};

static int utf_convert(int a, convertStruct table[], int tableSize);
static int utf_strnicmp(char_u *s1, char_u *s2, size_t n1, size_t n2);

/*
 * Generic conversion function for case operations.
 * Return the converted equivalent of "a", which is a UCS-4 character.  Use
 * the given conversion "table".  Uses binary search on "table".
 */
    static int
utf_convert(a, table, tableSize)
    int                 a;
    convertStruct       table[];
    int                 tableSize;
{
    int start, mid, end; /* indices into table */
    int entries = tableSize / sizeof(convertStruct);

    start = 0;
    end = entries;
    while (start < end)
    {
        /* need to search further */
        mid = (end + start) / 2;
        if (table[mid].rangeEnd < a)
            start = mid + 1;
        else
            end = mid;
    }
    if (start < entries
            && table[start].rangeStart <= a
            && a <= table[start].rangeEnd
            && (a - table[start].rangeStart) % table[start].step == 0)
        return (a + table[start].offset);
    else
        return a;
}

/*
 * Return the folded-case equivalent of "a", which is a UCS-4 character.  Uses
 * simple case folding.
 */
    int
utf_fold(a)
    int         a;
{
    return utf_convert(a, foldCase, (int)sizeof(foldCase));
}

static convertStruct toLower[] =
{
        {0x41,0x5a,1,32},
        {0xc0,0xd6,1,32},
        {0xd8,0xde,1,32},
        {0x100,0x12e,2,1},
        {0x130,0x130,-1,-199},
        {0x132,0x136,2,1},
        {0x139,0x147,2,1},
        {0x14a,0x176,2,1},
        {0x178,0x178,-1,-121},
        {0x179,0x17d,2,1},
        {0x181,0x181,-1,210},
        {0x182,0x184,2,1},
        {0x186,0x186,-1,206},
        {0x187,0x187,-1,1},
        {0x189,0x18a,1,205},
        {0x18b,0x18b,-1,1},
        {0x18e,0x18e,-1,79},
        {0x18f,0x18f,-1,202},
        {0x190,0x190,-1,203},
        {0x191,0x191,-1,1},
        {0x193,0x193,-1,205},
        {0x194,0x194,-1,207},
        {0x196,0x196,-1,211},
        {0x197,0x197,-1,209},
        {0x198,0x198,-1,1},
        {0x19c,0x19c,-1,211},
        {0x19d,0x19d,-1,213},
        {0x19f,0x19f,-1,214},
        {0x1a0,0x1a4,2,1},
        {0x1a6,0x1a6,-1,218},
        {0x1a7,0x1a7,-1,1},
        {0x1a9,0x1a9,-1,218},
        {0x1ac,0x1ac,-1,1},
        {0x1ae,0x1ae,-1,218},
        {0x1af,0x1af,-1,1},
        {0x1b1,0x1b2,1,217},
        {0x1b3,0x1b5,2,1},
        {0x1b7,0x1b7,-1,219},
        {0x1b8,0x1bc,4,1},
        {0x1c4,0x1c4,-1,2},
        {0x1c5,0x1c5,-1,1},
        {0x1c7,0x1c7,-1,2},
        {0x1c8,0x1c8,-1,1},
        {0x1ca,0x1ca,-1,2},
        {0x1cb,0x1db,2,1},
        {0x1de,0x1ee,2,1},
        {0x1f1,0x1f1,-1,2},
        {0x1f2,0x1f4,2,1},
        {0x1f6,0x1f6,-1,-97},
        {0x1f7,0x1f7,-1,-56},
        {0x1f8,0x21e,2,1},
        {0x220,0x220,-1,-130},
        {0x222,0x232,2,1},
        {0x23a,0x23a,-1,10795},
        {0x23b,0x23b,-1,1},
        {0x23d,0x23d,-1,-163},
        {0x23e,0x23e,-1,10792},
        {0x241,0x241,-1,1},
        {0x243,0x243,-1,-195},
        {0x244,0x244,-1,69},
        {0x245,0x245,-1,71},
        {0x246,0x24e,2,1},
        {0x370,0x372,2,1},
        {0x376,0x376,-1,1},
        {0x37f,0x37f,-1,116},
        {0x386,0x386,-1,38},
        {0x388,0x38a,1,37},
        {0x38c,0x38c,-1,64},
        {0x38e,0x38f,1,63},
        {0x391,0x3a1,1,32},
        {0x3a3,0x3ab,1,32},
        {0x3cf,0x3cf,-1,8},
        {0x3d8,0x3ee,2,1},
        {0x3f4,0x3f4,-1,-60},
        {0x3f7,0x3f7,-1,1},
        {0x3f9,0x3f9,-1,-7},
        {0x3fa,0x3fa,-1,1},
        {0x3fd,0x3ff,1,-130},
        {0x400,0x40f,1,80},
        {0x410,0x42f,1,32},
        {0x460,0x480,2,1},
        {0x48a,0x4be,2,1},
        {0x4c0,0x4c0,-1,15},
        {0x4c1,0x4cd,2,1},
        {0x4d0,0x52e,2,1},
        {0x531,0x556,1,48},
        {0x10a0,0x10c5,1,7264},
        {0x10c7,0x10cd,6,7264},
        {0x1e00,0x1e94,2,1},
        {0x1e9e,0x1e9e,-1,-7615},
        {0x1ea0,0x1efe,2,1},
        {0x1f08,0x1f0f,1,-8},
        {0x1f18,0x1f1d,1,-8},
        {0x1f28,0x1f2f,1,-8},
        {0x1f38,0x1f3f,1,-8},
        {0x1f48,0x1f4d,1,-8},
        {0x1f59,0x1f5f,2,-8},
        {0x1f68,0x1f6f,1,-8},
        {0x1f88,0x1f8f,1,-8},
        {0x1f98,0x1f9f,1,-8},
        {0x1fa8,0x1faf,1,-8},
        {0x1fb8,0x1fb9,1,-8},
        {0x1fba,0x1fbb,1,-74},
        {0x1fbc,0x1fbc,-1,-9},
        {0x1fc8,0x1fcb,1,-86},
        {0x1fcc,0x1fcc,-1,-9},
        {0x1fd8,0x1fd9,1,-8},
        {0x1fda,0x1fdb,1,-100},
        {0x1fe8,0x1fe9,1,-8},
        {0x1fea,0x1feb,1,-112},
        {0x1fec,0x1fec,-1,-7},
        {0x1ff8,0x1ff9,1,-128},
        {0x1ffa,0x1ffb,1,-126},
        {0x1ffc,0x1ffc,-1,-9},
        {0x2126,0x2126,-1,-7517},
        {0x212a,0x212a,-1,-8383},
        {0x212b,0x212b,-1,-8262},
        {0x2132,0x2132,-1,28},
        {0x2160,0x216f,1,16},
        {0x2183,0x2183,-1,1},
        {0x24b6,0x24cf,1,26},
        {0x2c00,0x2c2e,1,48},
        {0x2c60,0x2c60,-1,1},
        {0x2c62,0x2c62,-1,-10743},
        {0x2c63,0x2c63,-1,-3814},
        {0x2c64,0x2c64,-1,-10727},
        {0x2c67,0x2c6b,2,1},
        {0x2c6d,0x2c6d,-1,-10780},
        {0x2c6e,0x2c6e,-1,-10749},
        {0x2c6f,0x2c6f,-1,-10783},
        {0x2c70,0x2c70,-1,-10782},
        {0x2c72,0x2c75,3,1},
        {0x2c7e,0x2c7f,1,-10815},
        {0x2c80,0x2ce2,2,1},
        {0x2ceb,0x2ced,2,1},
        {0x2cf2,0xa640,31054,1},
        {0xa642,0xa66c,2,1},
        {0xa680,0xa69a,2,1},
        {0xa722,0xa72e,2,1},
        {0xa732,0xa76e,2,1},
        {0xa779,0xa77b,2,1},
        {0xa77d,0xa77d,-1,-35332},
        {0xa77e,0xa786,2,1},
        {0xa78b,0xa78b,-1,1},
        {0xa78d,0xa78d,-1,-42280},
        {0xa790,0xa792,2,1},
        {0xa796,0xa7a8,2,1},
        {0xa7aa,0xa7aa,-1,-42308},
        {0xa7ab,0xa7ab,-1,-42319},
        {0xa7ac,0xa7ac,-1,-42315},
        {0xa7ad,0xa7ad,-1,-42305},
        {0xa7b0,0xa7b0,-1,-42258},
        {0xa7b1,0xa7b1,-1,-42282},
        {0xff21,0xff3a,1,32},
        {0x10400,0x10427,1,40},
        {0x118a0,0x118bf,1,32}
};

static convertStruct toUpper[] =
{
        {0x61,0x7a,1,-32},
        {0xb5,0xb5,-1,743},
        {0xe0,0xf6,1,-32},
        {0xf8,0xfe,1,-32},
        {0xff,0xff,-1,121},
        {0x101,0x12f,2,-1},
        {0x131,0x131,-1,-232},
        {0x133,0x137,2,-1},
        {0x13a,0x148,2,-1},
        {0x14b,0x177,2,-1},
        {0x17a,0x17e,2,-1},
        {0x17f,0x17f,-1,-300},
        {0x180,0x180,-1,195},
        {0x183,0x185,2,-1},
        {0x188,0x18c,4,-1},
        {0x192,0x192,-1,-1},
        {0x195,0x195,-1,97},
        {0x199,0x199,-1,-1},
        {0x19a,0x19a,-1,163},
        {0x19e,0x19e,-1,130},
        {0x1a1,0x1a5,2,-1},
        {0x1a8,0x1ad,5,-1},
        {0x1b0,0x1b4,4,-1},
        {0x1b6,0x1b9,3,-1},
        {0x1bd,0x1bd,-1,-1},
        {0x1bf,0x1bf,-1,56},
        {0x1c5,0x1c5,-1,-1},
        {0x1c6,0x1c6,-1,-2},
        {0x1c8,0x1c8,-1,-1},
        {0x1c9,0x1c9,-1,-2},
        {0x1cb,0x1cb,-1,-1},
        {0x1cc,0x1cc,-1,-2},
        {0x1ce,0x1dc,2,-1},
        {0x1dd,0x1dd,-1,-79},
        {0x1df,0x1ef,2,-1},
        {0x1f2,0x1f2,-1,-1},
        {0x1f3,0x1f3,-1,-2},
        {0x1f5,0x1f9,4,-1},
        {0x1fb,0x21f,2,-1},
        {0x223,0x233,2,-1},
        {0x23c,0x23c,-1,-1},
        {0x23f,0x240,1,10815},
        {0x242,0x247,5,-1},
        {0x249,0x24f,2,-1},
        {0x250,0x250,-1,10783},
        {0x251,0x251,-1,10780},
        {0x252,0x252,-1,10782},
        {0x253,0x253,-1,-210},
        {0x254,0x254,-1,-206},
        {0x256,0x257,1,-205},
        {0x259,0x259,-1,-202},
        {0x25b,0x25b,-1,-203},
        {0x25c,0x25c,-1,42319},
        {0x260,0x260,-1,-205},
        {0x261,0x261,-1,42315},
        {0x263,0x263,-1,-207},
        {0x265,0x265,-1,42280},
        {0x266,0x266,-1,42308},
        {0x268,0x268,-1,-209},
        {0x269,0x269,-1,-211},
        {0x26b,0x26b,-1,10743},
        {0x26c,0x26c,-1,42305},
        {0x26f,0x26f,-1,-211},
        {0x271,0x271,-1,10749},
        {0x272,0x272,-1,-213},
        {0x275,0x275,-1,-214},
        {0x27d,0x27d,-1,10727},
        {0x280,0x283,3,-218},
        {0x287,0x287,-1,42282},
        {0x288,0x288,-1,-218},
        {0x289,0x289,-1,-69},
        {0x28a,0x28b,1,-217},
        {0x28c,0x28c,-1,-71},
        {0x292,0x292,-1,-219},
        {0x29e,0x29e,-1,42258},
        {0x345,0x345,-1,84},
        {0x371,0x373,2,-1},
        {0x377,0x377,-1,-1},
        {0x37b,0x37d,1,130},
        {0x3ac,0x3ac,-1,-38},
        {0x3ad,0x3af,1,-37},
        {0x3b1,0x3c1,1,-32},
        {0x3c2,0x3c2,-1,-31},
        {0x3c3,0x3cb,1,-32},
        {0x3cc,0x3cc,-1,-64},
        {0x3cd,0x3ce,1,-63},
        {0x3d0,0x3d0,-1,-62},
        {0x3d1,0x3d1,-1,-57},
        {0x3d5,0x3d5,-1,-47},
        {0x3d6,0x3d6,-1,-54},
        {0x3d7,0x3d7,-1,-8},
        {0x3d9,0x3ef,2,-1},
        {0x3f0,0x3f0,-1,-86},
        {0x3f1,0x3f1,-1,-80},
        {0x3f2,0x3f2,-1,7},
        {0x3f3,0x3f3,-1,-116},
        {0x3f5,0x3f5,-1,-96},
        {0x3f8,0x3fb,3,-1},
        {0x430,0x44f,1,-32},
        {0x450,0x45f,1,-80},
        {0x461,0x481,2,-1},
        {0x48b,0x4bf,2,-1},
        {0x4c2,0x4ce,2,-1},
        {0x4cf,0x4cf,-1,-15},
        {0x4d1,0x52f,2,-1},
        {0x561,0x586,1,-48},
        {0x1d79,0x1d79,-1,35332},
        {0x1d7d,0x1d7d,-1,3814},
        {0x1e01,0x1e95,2,-1},
        {0x1e9b,0x1e9b,-1,-59},
        {0x1ea1,0x1eff,2,-1},
        {0x1f00,0x1f07,1,8},
        {0x1f10,0x1f15,1,8},
        {0x1f20,0x1f27,1,8},
        {0x1f30,0x1f37,1,8},
        {0x1f40,0x1f45,1,8},
        {0x1f51,0x1f57,2,8},
        {0x1f60,0x1f67,1,8},
        {0x1f70,0x1f71,1,74},
        {0x1f72,0x1f75,1,86},
        {0x1f76,0x1f77,1,100},
        {0x1f78,0x1f79,1,128},
        {0x1f7a,0x1f7b,1,112},
        {0x1f7c,0x1f7d,1,126},
        {0x1f80,0x1f87,1,8},
        {0x1f90,0x1f97,1,8},
        {0x1fa0,0x1fa7,1,8},
        {0x1fb0,0x1fb1,1,8},
        {0x1fb3,0x1fb3,-1,9},
        {0x1fbe,0x1fbe,-1,-7205},
        {0x1fc3,0x1fc3,-1,9},
        {0x1fd0,0x1fd1,1,8},
        {0x1fe0,0x1fe1,1,8},
        {0x1fe5,0x1fe5,-1,7},
        {0x1ff3,0x1ff3,-1,9},
        {0x214e,0x214e,-1,-28},
        {0x2170,0x217f,1,-16},
        {0x2184,0x2184,-1,-1},
        {0x24d0,0x24e9,1,-26},
        {0x2c30,0x2c5e,1,-48},
        {0x2c61,0x2c61,-1,-1},
        {0x2c65,0x2c65,-1,-10795},
        {0x2c66,0x2c66,-1,-10792},
        {0x2c68,0x2c6c,2,-1},
        {0x2c73,0x2c76,3,-1},
        {0x2c81,0x2ce3,2,-1},
        {0x2cec,0x2cee,2,-1},
        {0x2cf3,0x2cf3,-1,-1},
        {0x2d00,0x2d25,1,-7264},
        {0x2d27,0x2d2d,6,-7264},
        {0xa641,0xa66d,2,-1},
        {0xa681,0xa69b,2,-1},
        {0xa723,0xa72f,2,-1},
        {0xa733,0xa76f,2,-1},
        {0xa77a,0xa77c,2,-1},
        {0xa77f,0xa787,2,-1},
        {0xa78c,0xa791,5,-1},
        {0xa793,0xa797,4,-1},
        {0xa799,0xa7a9,2,-1},
        {0xff41,0xff5a,1,-32},
        {0x10428,0x1044f,1,-40},
        {0x118c0,0x118df,1,-32}
};
/*
 * Return the upper-case equivalent of "a", which is a UCS-4 character.  Use
 * simple case folding.
 */
    int
utf_toupper(a)
    int         a;
{
    /* If 'casemap' contains "keepascii" use ASCII style toupper(). */
    if (a < 128 && (cmp_flags & CMP_KEEPASCII))
        return TOUPPER_ASC(a);

#if defined(__STDC_ISO_10646__)
    /* If towupper() is available and handles Unicode, use it. */
    if (!(cmp_flags & CMP_INTERNAL))
        return towupper(a);
#endif

    /* For characters below 128 use locale sensitive toupper(). */
    if (a < 128)
        return toupper(a);

    /* For any other characters use the above mapping table. */
    return utf_convert(a, toUpper, (int)sizeof(toUpper));
}

    int
utf_islower(a)
    int         a;
{
    /* German sharp s is lower case but has no upper case equivalent. */
    return (utf_toupper(a) != a) || a == 0xdf;
}

/*
 * Return the lower-case equivalent of "a", which is a UCS-4 character.  Use
 * simple case folding.
 */
    int
utf_tolower(a)
    int         a;
{
    /* If 'casemap' contains "keepascii" use ASCII style tolower(). */
    if (a < 128 && (cmp_flags & CMP_KEEPASCII))
        return TOLOWER_ASC(a);

#if defined(__STDC_ISO_10646__)
    /* If towlower() is available and handles Unicode, use it. */
    if (!(cmp_flags & CMP_INTERNAL))
        return towlower(a);
#endif

    /* For characters below 128 use locale sensitive tolower(). */
    if (a < 128)
        return tolower(a);

    /* For any other characters use the above mapping table. */
    return utf_convert(a, toLower, (int)sizeof(toLower));
}

    int
utf_isupper(a)
    int         a;
{
    return (utf_tolower(a) != a);
}

    static int
utf_strnicmp(s1, s2, n1, n2)
    char_u      *s1, *s2;
    size_t      n1, n2;
{
    int         c1, c2, cdiff;
    char_u      buffer[6];

    for (;;)
    {
        c1 = utf_safe_read_char_adv(&s1, &n1);
        c2 = utf_safe_read_char_adv(&s2, &n2);

        if (c1 <= 0 || c2 <= 0)
            break;

        if (c1 == c2)
            continue;

        cdiff = utf_fold(c1) - utf_fold(c2);
        if (cdiff != 0)
            return cdiff;
    }

    /* some string ended or has an incomplete/illegal character sequence */

    if (c1 == 0 || c2 == 0)
    {
        /* some string ended. shorter string is smaller */
        if (c1 == 0 && c2 == 0)
            return 0;
        return c1 == 0 ? -1 : 1;
    }

    /* Continue with bytewise comparison to produce some result that
     * would make comparison operations involving this function transitive.
     *
     * If only one string had an error, comparison should be made with
     * folded version of the other string. In this case it is enough
     * to fold just one character to determine the result of comparison. */

    if (c1 != -1 && c2 == -1)
    {
        n1 = utf_char2bytes(utf_fold(c1), buffer);
        s1 = buffer;
    }
    else if (c2 != -1 && c1 == -1)
    {
        n2 = utf_char2bytes(utf_fold(c2), buffer);
        s2 = buffer;
    }

    while (n1 > 0 && n2 > 0 && *s1 != NUL && *s2 != NUL)
    {
        cdiff = (int)(*s1) - (int)(*s2);
        if (cdiff != 0)
            return cdiff;

        s1++;
        s2++;
        n1--;
        n2--;
    }

    if (n1 > 0 && *s1 == NUL)
        n1 = 0;
    if (n2 > 0 && *s2 == NUL)
        n2 = 0;

    if (n1 == 0 && n2 == 0)
        return 0;
    return n1 == 0 ? -1 : 1;
}

/*
 * Version of strnicmp() that handles multi-byte characters.
 * Needed for Big5, Shift-JIS and UTF-8 encoding.
 * Returns zero if s1 and s2 are equal (ignoring case), the difference between
 * two characters otherwise.
 */
    int
mb_strnicmp(s1, s2, nn)
    char_u      *s1, *s2;
    size_t      nn;
{
    return utf_strnicmp(s1, s2, nn, nn);
}

/*
 * "g8": show bytes of the UTF-8 char under the cursor.  Doesn't matter what
 * 'encoding' has been set to.
 */
    void
show_utf8()
{
    int         len;
    int         rlen = 0;
    char_u      *line;
    int         clen;
    int         i;

    /* Get the byte length of the char under the cursor, including composing characters. */
    line = ml_get_cursor();
    len = utfc_ptr2len(line);
    if (len == 0)
    {
        MSG("NUL");
        return;
    }

    clen = 0;
    for (i = 0; i < len; ++i)
    {
        if (clen == 0)
        {
            /* start of (composing) character, get its length */
            if (i > 0)
            {
                STRCPY(IObuff + rlen, "+ ");
                rlen += 2;
            }
            clen = utf_ptr2len(line + i);
        }
        sprintf((char *)IObuff + rlen, "%02x ",
                (line[i] == NL) ? NUL : line[i]);  /* NUL is stored as NL */
        --clen;
        rlen += (int)STRLEN(IObuff + rlen);
        if (rlen > IOSIZE - 20)
            break;
    }

    msg(IObuff);
}

    int
utf_head_off(base, p)
    char_u      *base;
    char_u      *p;
{
    char_u      *q;
    char_u      *s;
    int         c;
    int         len;

    if (*p < 0x80)              /* be quick for ASCII */
        return 0;

    /* Skip backwards over trailing bytes: 10xx.xxxx
     * Skip backwards again if on a composing char. */
    for (q = p; ; --q)
    {
        /* Move s to the last byte of this char. */
        for (s = q; (s[1] & 0xc0) == 0x80; ++s)
            ;
        /* Move q to the first byte of this char. */
        while (q > base && (*q & 0xc0) == 0x80)
            --q;
        /* Check for illegal sequence. Do allow an illegal byte after where we started. */
        len = utf8len_tab[*q];
        if (len != (int)(s - q + 1) && len != (int)(p - q + 1))
            return 0;

        if (q <= base)
            break;

        c = utf_ptr2char(q);
        if (utf_iscomposing(c))
            continue;

        break;
    }

    return (int)(p - q);
}

/*
 * Copy a character from "*fp" to "*tp" and advance the pointers.
 */
    void
mb_copy_char(fp, tp)
    char_u      **fp;
    char_u      **tp;
{
    int     l = utfc_ptr2len(*fp);

    mch_memmove(*tp, *fp, (size_t)l);
    *tp += l;
    *fp += l;
}

/*
 * Return the offset from "p" to the first byte of a character.  When "p" is
 * at the start of a character 0 is returned, otherwise the offset to the next
 * character.  Can start anywhere in a stream of bytes.
 */
    int
mb_off_next(base, p)
    char_u      *base;
    char_u      *p;
{
    int         i;
    int         j;

    if (*p < 0x80)          /* be quick for ASCII */
        return 0;

    /* Find the next character that isn't 10xx.xxxx */
    for (i = 0; (p[i] & 0xc0) == 0x80; ++i)
        ;
    if (i > 0)
    {
        /* Check for illegal sequence. */
        for (j = 0; p - j > base; ++j)
            if ((p[-j] & 0xc0) != 0x80)
                break;
        if (utf8len_tab[p[-j]] != i + j)
            return 0;
    }
    return i;
}

/*
 * Return the offset from "p" to the last byte of the character it points
 * into.  Can start anywhere in a stream of bytes.
 */
    int
mb_tail_off(base, p)
    char_u      *base;
    char_u      *p;
{
    int         i, j;

    if (*p == NUL)
        return 0;

    /* Find the last character that is 10xx.xxxx */
    for (i = 0; (p[i + 1] & 0xc0) == 0x80; ++i)
        ;

    /* Check for illegal sequence. */
    for (j = 0; p - j > base; ++j)
        if ((p[-j] & 0xc0) != 0x80)
            break;

    if (utf8len_tab[p[-j]] != i + j + 1)
        return 0;

    return i;
}

/*
 * Find the next illegal byte sequence.
 */
    void
utf_find_illegal()
{
    pos_T       pos = curwin->w_cursor;
    char_u      *p;
    int         len;
    char_u      *tofree = NULL;

    curwin->w_cursor.coladd = 0;
    for (;;)
    {
        p = ml_get_cursor();

        while (*p != NUL)
        {
            /* Illegal means that there are not enough trail bytes (checked by
             * utf_ptr2len()) or too many of them (overlong sequence). */
            len = utf_ptr2len(p);
            if (*p >= 0x80 && (len == 1 || utf_char2len(utf_ptr2char(p)) != len))
            {
                curwin->w_cursor.col += (colnr_T)(p - ml_get_cursor());
                goto theend;
            }
            p += len;
        }
        if (curwin->w_cursor.lnum == curbuf->b_ml.ml_line_count)
            break;
        ++curwin->w_cursor.lnum;
        curwin->w_cursor.col = 0;
    }

    /* didn't find it: don't move and beep */
    curwin->w_cursor = pos;
    beep_flush();

theend:
    vim_free(tofree);
}

/*
 * If the cursor moves on an trail byte, set the cursor on the lead byte.
 * Thus it moves left if necessary.
 * Return TRUE when the cursor was adjusted.
 */
    void
mb_adjust_cursor()
{
    mb_adjustpos(curbuf, &curwin->w_cursor);
}

/*
 * Adjust position "*lp" to point to the first byte of a multi-byte character.
 * If it points to a tail byte it's moved backwards to the head byte.
 */
    void
mb_adjustpos(buf, lp)
    buf_T       *buf;
    pos_T       *lp;
{
    char_u      *p;

    if (lp->col > 0 || lp->coladd > 1)
    {
        p = ml_get_buf(buf, lp->lnum, FALSE);
        lp->col -= utf_head_off(p, p + lp->col);
        /* Reset "coladd" when the cursor would be on the right half of a
         * double-wide character. */
        if (lp->coladd == 1
                && p[lp->col] != TAB
                && vim_isprintc(utf_ptr2char(p + lp->col))
                && ptr2cells(p + lp->col) > 1)
            lp->coladd = 0;
    }
}

/*
 * Return a pointer to the character before "*p", if there is one.
 */
    char_u *
mb_prevptr(line, p)
    char_u *line;       /* start of the string */
    char_u *p;
{
    if (p > line)
        mb_ptr_back(line, p);
    return p;
}

/*
 * Return the character length of "str".  Each multi-byte character (with
 * following composing characters) counts as one.
 */
    int
mb_charlen(str)
    char_u      *str;
{
    char_u      *p = str;
    int         count;

    if (p == NULL)
        return 0;

    for (count = 0; *p != NUL; count++)
        p += utfc_ptr2len(p);

    return count;
}

/*
 * Try to un-escape a multi-byte character.
 * Used for the "to" and "from" part of a mapping.
 * Return the un-escaped string if it is a multi-byte character, and advance
 * "pp" to just after the bytes that formed it.
 * Return NULL if no multi-byte char was found.
 */
    char_u *
mb_unescape(pp)
    char_u **pp;
{
    static char_u       buf[6];
    int                 n;
    int                 m = 0;
    char_u              *str = *pp;

    /* Must translate K_SPECIAL KS_SPECIAL KE_FILLER to K_SPECIAL and CSI
     * KS_EXTRA KE_CSI to CSI.
     * Maximum length of a utf-8 character is 4 bytes. */
    for (n = 0; str[n] != NUL && m < 4; ++n)
    {
        if (str[n] == K_SPECIAL
                && str[n + 1] == KS_SPECIAL
                && str[n + 2] == KE_FILLER)
        {
            buf[m++] = K_SPECIAL;
            n += 2;
        }
        else if ((str[n] == K_SPECIAL)
                && str[n + 1] == KS_EXTRA
                && str[n + 2] == (int)KE_CSI)
        {
            buf[m++] = CSI;
            n += 2;
        }
        else if (str[n] == K_SPECIAL)
            break;              /* a special key can't be a multibyte char */
        else
            buf[m++] = str[n];
        buf[m] = NUL;

        /* Return a multi-byte character if it's found.  An illegal sequence
         * will result in a 1 here. */
        if (utfc_ptr2len(buf) > 1)
        {
            *pp = str + n + 1;
            return buf;
        }

        /* Bail out quickly for ASCII. */
        if (buf[0] < 128)
            break;
    }
    return NULL;
}

/*
 * Return TRUE if the character at "row"/"col" on the screen is the left side
 * of a double-width character.
 * Caller must make sure "row" and "col" are not invalid!
 */
    int
mb_lefthalve(row, col)
    int     row;
    int     col;
{
    return utf_off2cells(LineOffset[row] + col, LineOffset[row] + screen_Columns) > 1;
}

/*
 * Correct a position on the screen, if it's the right half of a double-wide
 * char move it to the left half.  Returns the corrected column.
 */
    int
mb_fix_col(col, row)
    int         col;
    int         row;
{
    col = check_col(col);
    row = check_row(row);
    if (ScreenLines != NULL && col > 0 && ScreenLines[LineOffset[row] + col] == 0)
        return col - 1;
    return col;
}

/*
 * Find the canonical name for encoding "enc".
 * When the name isn't recognized, returns "enc" itself, but with all lower
 * case characters and '_' replaced with '-'.
 * Returns an allocated string.  NULL for out-of-memory.
 */
    char_u *
enc_canonize(enc)
    char_u      *enc;
{
    char_u      *r;
    char_u      *p, *s;

    if (STRCMP(enc, "default") == 0)
    {
        r = (char_u *)ENC_DFLT;
        return vim_strsave(r);
    }

    /* copy "enc" to allocated memory, with room for two '-' */
    r = alloc((unsigned)(STRLEN(enc) + 3));
    if (r != NULL)
    {
        /* Make it all lower case and replace '_' with '-'. */
        p = r;
        for (s = enc; *s != NUL; ++s)
        {
            if (*s == '_')
                *p++ = '-';
            else
                *p++ = TOLOWER_ASC(*s);
        }
        *p = NUL;

        p = r;

        if (enc_canon_search(p) >= 0)
        {
            /* canonical name can be used unmodified */
            if (p != r)
                STRMOVE(r, p);
        }
    }
    return r;
}

/*
 * Convert text "ptr[*lenp]" according to "vcp".
 * Returns the result in allocated memory and sets "*lenp".
 * When "lenp" is NULL, use NUL terminated strings.
 * When something goes wrong, NULL is returned and "*lenp" is unchanged.
 */
    char_u *
string_convert(ptr, lenp)
    char_u      *ptr;
    int         *lenp;
{
    int         len;

    if (lenp == NULL)
        len = (int)STRLEN(ptr);
    else
        len = *lenp;
    if (len == 0)
        return vim_strsave((char_u *)"");

    return NULL;
}
