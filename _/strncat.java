PUBLIC byte *STRNCAT(byte *d, byte *s, size_t n)
{
    byte *p = d, b;

    do
    {
        b = *p++;
    }
    while (b != NUL);

    p -= 2;

    for ( ; 0 < n; --n)
    {
        b = *s++;
        *++p = b;
        if (b == NUL)
            return d;
    }

    if (b != NUL)
        *++p = NUL;

    return d;
}
