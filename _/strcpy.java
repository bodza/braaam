PUBLIC byte *STRCPY(byte *d, byte *s)
{
    size_t i = (size_t)(d - s - 1);

    byte b;
    do
    {
        b = *s++;
        s[i] = b;
    }
    while (b != NUL);

    return d;
}
