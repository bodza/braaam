PUBLIC byte *STRPBRK(byte *s, byte *accept)
{
    for ( ; *s != NUL; s++)
        for (byte *a = accept; *a != NUL; a++)
            if (*a == *s)
                return s;

    return null;
}
