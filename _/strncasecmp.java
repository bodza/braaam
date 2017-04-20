PUBLIC int STRNCASECMP(byte *s1, byte *s2, size_t n)
{
    if (s1 != s2 && 0 < n)
    {
        byte b1;
        do
        {
            int cmp = asc_tolower(b1 = *s1++) - asc_tolower(*s2++);
            if (cmp != 0)
                return cmp;
        }
        while (b1 != NUL && 0 < --n);
    }

    return 0;
}
