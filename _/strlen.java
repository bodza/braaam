PUBLIC size_t STRLEN(byte *s)
{
    for (byte *p = s; ; p++)
        if (*p == NUL)
            return (size_t)(p - s);
}
