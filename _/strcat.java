PUBLIC byte *STRCAT(byte *d, byte *s)
{
    byte *p = d, b;

    do
    {
        b = *p++;
    }
    while (b != NUL);

    p -= 2;

    do
    {
        b = *s++;
        *++p = b;
    }
    while (b != NUL);

    return d;
}
