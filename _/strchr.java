PUBLIC byte *STRCHR(byte *s, byte b)
{
    for ( ; ; s++)
    {
        if (*s == b)
            return s;
        if (*s == NUL)
            return null;
    }
}
