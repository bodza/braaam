PUBLIC int STRCMP(byte *s1, byte *s2)
{
    byte b1, b2;

    do
    {
        b1 = *s1++;
        b2 = *s2++;
    }
    while (b1 != NUL && b1 == b2);

    return b1 - b2;
}
