PUBLIC void MEMMOVE(byte *d, byte *s, size_t n)
{
    if (n <= (size_t)(d - s)) /* *Unsigned* compare! */
    {
        for ( ; 0 < n; --n)
            *d++ = *s++;
    }
    else
    {
        for (d += n, s += n; 0 < n; --n)
            *--d = *--s;
    }
}
