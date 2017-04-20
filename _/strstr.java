#define SIZE_MAX (0xffffffffffffffffUL)

/*
 * We use the Two-Way string matching algorithm, which guarantees linear
 * complexity with constant space.  Additionally, for long needles,
 * we also use a bad character shift table similar to the Boyer-Moore
 * algorithm to achieve improved (potentially sub-linear) performance.
 *
 * See http://www-igm.univ-mlv.fr/~lecroq/string/node26.html#SECTION00260
 * and http://en.wikipedia.org/wiki/Boyer-Moore_string_search_algorithm
 */

PRIVATE size_t critical_factorization(unsigned byte *needle, size_t needle_len, size_t *period)
{
    size_t max_suffix = SIZE_MAX;
    size_t j = 0;
    size_t p = 1;
    size_t k = p;
    while (j + k < needle_len)
    {
        unsigned byte a = needle[j + k];
        unsigned byte b = needle[max_suffix + k];
        if (a < b)
        {
            j += k;
            k = 1;
            p = j - max_suffix;
        }
        else if (a == b)
        {
            if (k != p)
                ++k;
            else
            {
                j += p;
                k = 1;
            }
        }
        else
        {
            max_suffix = j++;
            k = p = 1;
        }
    }
    *period = p;

    size_t max_suffix_rev = SIZE_MAX;
    j = 0;
    k = p = 1;
    while (j + k < needle_len)
    {
        unsigned byte a = needle[j + k];
        unsigned byte b = needle[max_suffix_rev + k];
        if (b < a)
        {
            j += k;
            k = 1;
            p = j - max_suffix_rev;
        }
        else if (a == b)
        {
            if (k != p)
                ++k;
            else
            {
                j += p;
                k = 1;
            }
        }
        else
        {
            max_suffix_rev = j++;
            k = p = 1;
        }
    }

    if (max_suffix_rev + 1 < max_suffix + 1)
        return max_suffix + 1;

    *period = p;
    return max_suffix_rev + 1;
}

PRIVATE byte *two_way_short_needle(unsigned byte *haystack, size_t haystack_len, unsigned byte *needle, size_t needle_len)
{
    size_t period;
    size_t suffix = critical_factorization(needle, needle_len, &period);

    if (MEMCMP(needle, needle + period, suffix) == 0)
    {
        size_t memory = 0;
        size_t j = 0;
        while (MEMCHR(haystack + haystack_len, NUL, j + needle_len - haystack_len) == null && (haystack_len = j + needle_len) != 0)
        {
            size_t i = (suffix < memory) ? memory : suffix;
            while (i < needle_len && needle[i] == haystack[i + j])
                ++i;
            if (needle_len <= i)
            {
                i = suffix - 1;
                while (memory < i + 1 && needle[i] == haystack[i + j])
                    --i;
                if (i + 1 < memory + 1)
                    return (byte *)(haystack + j);

                j += period;
                memory = needle_len - period;
            }
            else
            {
                j += i - suffix + 1;
                memory = 0;
            }
        }
    }
    else
    {
        period = ((suffix < needle_len - suffix) ? needle_len - suffix : suffix) + 1;
        size_t j = 0;
        while (MEMCHR(haystack + haystack_len, NUL, j + needle_len - haystack_len) == null && (haystack_len = j + needle_len) != 0)
        {
            size_t i = suffix;
            while (i < needle_len && needle[i] == haystack[i + j])
                ++i;
            if (needle_len <= i)
            {
                i = suffix - 1;
                while (i != SIZE_MAX && needle[i] == haystack[i + j])
                    --i;
                if (i == SIZE_MAX)
                    return (byte *)(haystack + j);
                j += period;
            }
            else
                j += i - suffix + 1;
        }
    }

    return null;
}

PRIVATE byte *two_way_long_needle(unsigned byte *haystack, size_t haystack_len, unsigned byte *needle, size_t needle_len)
{
    size_t shift_table[1 << 8];

    size_t period;
    size_t suffix = critical_factorization(needle, needle_len, &period);

    for (int i = 0; i < (1 << 8); i++)
        shift_table[i] = needle_len;
    for (int i = 0; i < needle_len; i++)
        shift_table[needle[i]] = needle_len - i - 1;

    if (MEMCMP(needle, needle + period, suffix) == 0)
    {
        size_t memory = 0;
        size_t j = 0;
        while (MEMCHR(haystack + haystack_len, NUL, j + needle_len - haystack_len) == null && (haystack_len = j + needle_len) != 0)
        {
            size_t shift = shift_table[haystack[j + needle_len - 1]];
            if (0 < shift)
            {
                if (memory != 0 && shift < period)
                {
                    shift = needle_len - period;
                    memory = 0;
                }
                j += shift;
                continue;
            }
            size_t i = (suffix < memory) ? memory : suffix;
            while (i < needle_len - 1 && needle[i] == haystack[i + j])
                ++i;
            if (needle_len - 1 <= i)
            {
                i = suffix - 1;
                while (memory < i + 1 && needle[i] == haystack[i + j])
                    --i;
                if (i + 1 < memory + 1)
                    return (byte *)(haystack + j);

                j += period;
                memory = needle_len - period;
            }
            else
            {
                j += i - suffix + 1;
                memory = 0;
            }
        }
    }
    else
    {
        period = ((suffix < needle_len - suffix) ? needle_len - suffix : suffix) + 1;
        size_t j = 0;
        while (MEMCHR(haystack + haystack_len, NUL, j + needle_len - haystack_len) == null && (haystack_len = j + needle_len) != 0)
        {
            size_t shift = shift_table[haystack[j + needle_len - 1]];
            if (0 < shift)
            {
                j += shift;
                continue;
            }
            size_t i = suffix;
            while (i < needle_len - 1 && needle[i] == haystack[i + j])
                ++i;
            if (needle_len - 1 <= i)
            {
                i = suffix - 1;
                while (i != SIZE_MAX && needle[i] == haystack[i + j])
                    --i;
                if (i == SIZE_MAX)
                    return (byte *)(haystack + j);
                j += period;
            }
            else
                j += i - suffix + 1;
        }
    }

    return null;
}

PUBLIC byte *STRSTR(byte *haystack_start, byte *needle_start)
{
    byte *haystack = haystack_start;
    byte *needle = needle_start;

    boolean ok = true;
    while (*haystack && *needle)
        ok &= (*haystack++ == *needle++);
    if (*needle != NUL)
        return null;
    if (ok)
        return haystack_start;

    size_t needle_len = needle - needle_start;
    haystack = STRCHR(haystack_start + 1, *needle_start);
    if (haystack == null || needle_len == 1)
        return haystack;

    needle -= needle_len;
    size_t haystack_len = (haystack_start + needle_len < haystack ? 1 : haystack_start + needle_len - haystack);

    if (needle_len < 32)
        return two_way_short_needle((unsigned byte *)haystack, haystack_len, (unsigned byte *)needle, needle_len);

    return two_way_long_needle((unsigned byte *)haystack, haystack_len, (unsigned byte *)needle, needle_len);
}
