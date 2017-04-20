PUBLIC int MEMCMP(byte *p1, byte *p2, size_t n)
{
    for ( ; 0 < n; --n)
    {
        int cmp = *p1++ - *p2++;
        if (cmp != 0)
            return cmp;
    }

    return 0;
}
