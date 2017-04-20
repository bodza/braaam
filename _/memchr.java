PUBLIC byte *MEMCHR(byte *p, byte b, size_t n)
{
    for ( ; 0 < n; --n, p++)
        if (*p == b)
            return p;

    return null;
}
