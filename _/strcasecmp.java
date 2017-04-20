PUBLIC int STRCASECMP(byte *s1, byte *s2)
{
    if (s1 != s2)
    {
        byte b1;
        do
        {
            int cmp = asc_tolower(b1 = *s1++) - asc_tolower(*s2++);
            if (cmp != 0)
                return cmp;
        }
        while (b1 != NUL);
    }

    return 0;
}
