PUBLIC byte *STRNCPY(byte *d, byte *s, size_t n)
{
    if (0 < n)
    {
        byte *p = d, b;

        --p;

        do
        {
            b = *s++;
            *++p = b;
            if (--n == 0)
                return d;
        }
        while (b != NUL);

        do
        {
            *++p = NUL;
        }
        while (0 < --n);
    }

    return d;
}
