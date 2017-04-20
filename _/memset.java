PUBLIC void MEMSET(byte *p, byte b, size_t n)
{
    for ( ; 0 < n; --n)
        *p++ = b;
}
