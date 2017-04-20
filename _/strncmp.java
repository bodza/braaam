PUBLIC int STRNCMP(byte *s1, byte *s2, size_t n)
{
    for ( ; 0 < n; --n)
    {
        byte b1 = *s1++, b2 = *s2++;

        if (b1 == NUL || b1 != b2)
            return b1 - b2;
    }

    return 0;
}
