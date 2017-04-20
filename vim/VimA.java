package vim;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;

import jnr.ffi.LibraryLoader;
import jnr.ffi.mapper.DataConverter;
import jnr.ffi.mapper.FromNativeContext;
import jnr.ffi.mapper.ToNativeContext;
import jnr.ffi.Memory;
import jnr.ffi.NativeType;
import jnr.ffi.Pointer;
import jnr.ffi.StructLayout;
import jnr.ffi.Variable;
import jnr.ffi.types.*;

import static vim.VimA.*;
import static vim.VimB.*;
import static vim.VimC.*;
import static vim.VimD.*;
import static vim.VimE.*;
import static vim.VimF.*;
import static vim.VimG.*;
import static vim.VimH.*;
import static vim.VimI.*;
import static vim.VimJ.*;
import static vim.VimK.*;
import static vim.VimL.*;
import static vim.VimM.*;
import static vim.VimN.*;
import static vim.VimO.*;
import static vim.VimP.*;
import static vim.VimQ.*;
import static vim.VimR.*;
import static vim.VimS.*;
import static vim.VimT.*;
import static vim.VimU.*;
import static vim.VimV.*;
import static vim.VimW.*;
import static vim.VimX.*;
import static vim.VimY.*;

/*
 * Supported Types
 * ------
 *
 * All java primitives are mapped simply to the equivalent C types.
 *
 * - byte - 8 bit signed integer
 * - short - 16 bit signed integer
 * - int - 32 bit signed integer
 * - long - natural long (i.e. 32 bits wide on 32 bit systems, 64 bit wide on 64 bit systems)
 * - float - 32 bit float
 * - double - 64 bit float
 *
 * The width and/or signed-ness of these basic types can be specified using one of the type alias annotations.
 *  e.g.
 *
 *     // Use the correct width for the result from getpid(3)
 *     @pid_t long getpid();
 *
 *     // read(2) returns a signed long result, and its length parameter is an unsigned long
 *     @ssize_t long read(int fd, Pointer data, @size_t long len);
 *
 *
 * In addition, the following java types are mapped to a C pointer
 *
 * - String - equivalent to "const char *"
 * - Pointer - equivalent to "void *"
 * - Buffer - equivalent to "void *"
 */
public class VimA
{
    /*private*/ static final /*MAYBEAN*/int FALSE = 0, TRUE = 1, MAYBE = 2;

    /* ----------------------------------------------------------------------- */

    /*private*/ static final int char_u(byte b)
    {
        return (b < 0) ? b + 0x100 : b;
    }

    /*private*/ static final long int_u(int i)
    {
        return (i < 0) ? i + 0x100000000L : i;
    }

    /* ----------------------------------------------------------------------- */

    /*private*/ static final byte NUL = '\000';

    /*private*/ static final Charset UTF8 = Charset.forName("UTF-8");

    /*private*/ static Bytes u8(String s)
    {
        ByteBuffer bb = UTF8.encode(s);
        int n = bb.remaining();
        byte[] ba = new byte[n + 1];
        bb.get(ba, 0, n);
        return new Bytes(ba);
    }

    /*private*/ static final class Bytes
    {
        byte[]      array;
        int         index;

        /*private*/ Bytes(int length)
        {
            array = new byte[length];
        }

        /*private*/ Bytes(byte[] array)
        {
            this.array = array;
        }

        /*private*/ Bytes(byte[] array, int index)
        {
            this.array = array;
            this.index = index;
        }

        /*private*/ int size()
        {
            return array.length - index;
        }

        /*private*/ byte at(int i)
        {
            return array[index + i];
        }

        /*private*/ Bytes be(int i, byte b)
        {
            array[index + i] = b;

            return this;
        }

        /*private*/ final Bytes be(int i, int c)
        {
            be(i, (byte)c);	// %%

            return this;
        }

        /*private*/ ByteBuffer buf()
        {
            return ByteBuffer.wrap(array, index, array.length - index);
        }

        /*private*/ Bytes plus(int i)
        {
            return new Bytes(array, index + i);
        }

        /*private*/ Bytes minus(int i)
        {
            return new Bytes(array, index - i);
        }
    }

    /*private*/ static boolean BEQ(Bytes s1, Bytes s0)
    {
        return (s1 == s0 || (s1 != null && s0 != null && s1.array == s0.array && s1.index == s0.index));
    }

    /*private*/ static final boolean BNE(Bytes s1, Bytes s0)
    {
        return !BEQ(s1, s0);
    }

    /*private*/ static int BDIFF(Bytes s1, Bytes s0)
    {
        if (s1.array != s0.array)
            throw new IllegalArgumentException("BDIFF array mismatch");

        return s1.index - s0.index;
    }

    /*private*/ static boolean BLT(Bytes s1, Bytes s0)
    {
        if (s1.array != s0.array)
            throw new IllegalArgumentException("BLT array mismatch");

        return (s1.index < s0.index);
    }

    /*private*/ static boolean BLE(Bytes s1, Bytes s0)
    {
        if (s1.array != s0.array)
            throw new IllegalArgumentException("BLE array mismatch");

        return (s1.index <= s0.index);
    }

    /*private*/ static int asc_toupper(int c)
    {
        return (c < 'a' || 'z' < c) ? c : c - ('a' - 'A');
    }

    /*private*/ static int asc_tolower(int c)
    {
        return (c < 'A' || 'Z' < c) ? c : c + ('a' - 'A');
    }

    /*private*/ static Bytes MEMCHR(Bytes p, byte b, int n)
    {
        for (int i = 0; i < n; i++)
            if (p.at(i) == b)
                return p.plus(i);

        return null;
    }

    /*private*/ static int MEMCMP(Bytes p1, Bytes p2, int n)
    {
        for (int i = 0; i < n; i++)
        {
            int cmp = p1.at(i) - p2.at(i);
            if (cmp != 0)
                return cmp;
        }

        return 0;
    }

    /*private*/ static void ACOPY(byte[] d, int di, byte[] s, int si, int n)
    {
        System.arraycopy(s, si, d, di, n);
    }

    /*private*/ static void ACOPY(short[] d, int di, short[] s, int si, int n)
    {
        System.arraycopy(s, si, d, di, n);
    }

    /*private*/ static void ACOPY(int[] d, int di, int[] s, int si, int n)
    {
        System.arraycopy(s, si, d, di, n);
    }

    /*private*/ static void ACOPY(Object[] d, int di, Object[] s, int si, int n)
    {
        System.arraycopy(s, si, d, di, n);
    }

    /*private*/ static void BCOPY(Bytes d, int di, Bytes s, int si, int n)
    {
        System.arraycopy(s.array, s.index + si, d.array, d.index + di, n);
    }

    /*private*/ static void BCOPY(Bytes d, Bytes s, int n)
    {
        BCOPY(d, 0, s, 0, n);
    }

    /*private*/ static void AFILL(boolean[] a, boolean b)
    {
        Arrays.fill(a, b);
    }

    /*private*/ static void AFILL(int[] a, int i)
    {
        Arrays.fill(a, i);
    }

    /*private*/ static void AFILL(int[] a, int ai, int i, int n)
    {
        Arrays.fill(a, ai, ai + n, i);
    }

    /*private*/ static void BFILL(Bytes p, int pi, byte b, int n)
    {
        Arrays.fill(p.array, p.index + pi, p.index + pi + n, b);
    }

    /*private*/ static int STRCASECMP(Bytes s1, Bytes s2)
    {
        if (BNE(s1, s2))
        {
            byte b1;
            do
            {
                int cmp = asc_tolower(b1 = (s1 = s1.plus(1)).at(-1)) - asc_tolower((s2 = s2.plus(1)).at(-1));
                if (cmp != 0)
                    return cmp;
            }
            while (b1 != NUL);
        }

        return 0;
    }

    /*private*/ static Bytes STRCAT(Bytes d, Bytes s)
    {
        Bytes p = d;
        byte b;

        do
        {
            b = (p = p.plus(1)).at(-1);
        }
        while (b != NUL);

        p = p.minus(2);

        do
        {
            b = (s = s.plus(1)).at(-1);
            (p = p.plus(1)).be(0, b);
        }
        while (b != NUL);

        return d;
    }

    /*private*/ static Bytes STRCHR(Bytes s, byte b)
    {
        for ( ; ; s = s.plus(1))
        {
            if (s.at(0) == b)
                return s;
            if (s.at(0) == NUL)
                return null;
        }
    }

    /*private*/ static int STRCMP(Bytes s1, Bytes s2)
    {
        byte b1, b2;

        do
        {
            b1 = (s1 = s1.plus(1)).at(-1);
            b2 = (s2 = s2.plus(1)).at(-1);
        }
        while (b1 != NUL && b1 == b2);

        return b1 - b2;
    }

    /*private*/ static Bytes STRCPY(Bytes d, Bytes s)
    {
        int i = BDIFF(d, s) - 1;

        byte b;
        do
        {
            b = (s = s.plus(1)).at(-1);
            s.be(i, b);
        }
        while (b != NUL);

        return d;
    }

    /*private*/ static final int strlen(Bytes s, int i)
    {
        return strlen(s.plus(i));
    }

    /*private*/ static int strlen(Bytes s)
    {
        for (Bytes p = s; ; p = p.plus(1))
            if (p.at(0) == NUL)
                return BDIFF(p, s);
    }

    /*private*/ static int STRNCASECMP(Bytes s1, Bytes s2, int n)
    {
        if (BNE(s1, s2) && 0 < n)
        {
            byte b1;
            do
            {
                int cmp = asc_tolower(b1 = (s1 = s1.plus(1)).at(-1)) - asc_tolower((s2 = s2.plus(1)).at(-1));
                if (cmp != 0)
                    return cmp;
            }
            while (b1 != NUL && 0 < --n);
        }

        return 0;
    }

    /*private*/ static Bytes STRNCAT(Bytes d, Bytes s, int n)
    {
        Bytes p = d;
        byte b;

        do
        {
            b = (p = p.plus(1)).at(-1);
        }
        while (b != NUL);

        p = p.minus(2);

        for ( ; 0 < n; --n)
        {
            b = (s = s.plus(1)).at(-1);
            (p = p.plus(1)).be(0, b);
            if (b == NUL)
                return d;
        }

        if (b != NUL)
            (p = p.plus(1)).be(0, NUL);

        return d;
    }

    /*private*/ static int STRNCMP(Bytes s1, Bytes s2, int n)
    {
        for ( ; 0 < n; --n)
        {
            byte b1 = (s1 = s1.plus(1)).at(-1), b2 = (s2 = s2.plus(1)).at(-1);

            if (b1 == NUL || b1 != b2)
                return b1 - b2;
        }

        return 0;
    }

    /*private*/ static Bytes STRNCPY(Bytes d, Bytes s, int n)
    {
        if (0 < n)
        {
            Bytes p = d;
            byte b;

            p = p.minus(1);

            do
            {
                b = (s = s.plus(1)).at(-1);
                (p = p.plus(1)).be(0, b);
                if (--n == 0)
                    return d;
            }
            while (b != NUL);

            do
            {
                (p = p.plus(1)).be(0, NUL);
            }
            while (0 < --n);
        }

        return d;
    }

    /*private*/ static Bytes STRPBRK(Bytes s, Bytes accept)
    {
        for ( ; s.at(0) != NUL; s = s.plus(1))
            for (Bytes a = accept; a.at(0) != NUL; a = a.plus(1))
                if (a.at(0) == s.at(0))
                    return s;

        return null;
    }

    /*private*/ static final int SIZE_MAX = 0xffffffff;

    /*
     * We use the Two-Way string matching algorithm, which guarantees linear
     * complexity with constant space.  Additionally, for long needles,
     * we also use a bad character shift table similar to the Boyer-Moore
     * algorithm to achieve improved (potentially sub-linear) performance.
     *
     * See http://www-igm.univ-mlv.fr/~lecroq/string/node26.html#SECTION00260
     * and http://en.wikipedia.org/wiki/Boyer-Moore_string_search_algorithm
     */

    /*private*/ static int critical_factorization(Bytes needle, int needle_len, int[] period)
    {
        int max_suffix = SIZE_MAX;
        int j = 0;
        int p = 1;
        int k = p;
        while (j + k < needle_len)
        {
            byte a = needle.at(j + k);
            byte b = needle.at(max_suffix + k);
            if (a < b)
            {
                j += k;
                k = 1;
                p = j - max_suffix;
            }
            else if (a == b)
            {
                if (k != p)
                    k++;
                else
                {
                    j += p;
                    k = 1;
                }
            }
            else
            {
                max_suffix = j++;
                k = p = 1;
            }
        }
        period[0] = p;

        int max_suffix_rev = SIZE_MAX;
        j = 0;
        k = p = 1;
        while (j + k < needle_len)
        {
            byte a = needle.at(j + k);
            byte b = needle.at(max_suffix_rev + k);
            if (b < a)
            {
                j += k;
                k = 1;
                p = j - max_suffix_rev;
            }
            else if (a == b)
            {
                if (k != p)
                    k++;
                else
                {
                    j += p;
                    k = 1;
                }
            }
            else
            {
                max_suffix_rev = j++;
                k = p = 1;
            }
        }

        if (max_suffix_rev + 1 < max_suffix + 1)
            return max_suffix + 1;

        period[0] = p;
        return max_suffix_rev + 1;
    }

    /*private*/ static Bytes two_way_short_needle(Bytes haystack, int haystack_len, Bytes needle, int needle_len)
    {
        int[] period = new int[1];
        int suffix = critical_factorization(needle, needle_len, period);

        if (MEMCMP(needle, needle.plus(period[0]), suffix) == 0)
        {
            int memory = 0;
            int j = 0;
            while (MEMCHR(haystack.plus(haystack_len), NUL, j + needle_len - haystack_len) == null && (haystack_len = j + needle_len) != 0)
            {
                int i = (suffix < memory) ? memory : suffix;
                while (i < needle_len && needle.at(i) == haystack.at(i + j))
                    i++;
                if (needle_len <= i)
                {
                    i = suffix - 1;
                    while (memory < i + 1 && needle.at(i) == haystack.at(i + j))
                        --i;
                    if (i + 1 < memory + 1)
                        return haystack.plus(j);

                    j += period[0];
                    memory = needle_len - period[0];
                }
                else
                {
                    j += i - suffix + 1;
                    memory = 0;
                }
            }
        }
        else
        {
            period[0] = ((suffix < needle_len - suffix) ? needle_len - suffix : suffix) + 1;
            int j = 0;
            while (MEMCHR(haystack.plus(haystack_len), NUL, j + needle_len - haystack_len) == null && (haystack_len = j + needle_len) != 0)
            {
                int i = suffix;
                while (i < needle_len && needle.at(i) == haystack.at(i + j))
                    i++;
                if (needle_len <= i)
                {
                    i = suffix - 1;
                    while (i != SIZE_MAX && needle.at(i) == haystack.at(i + j))
                        --i;
                    if (i == SIZE_MAX)
                        return haystack.plus(j);
                    j += period[0];
                }
                else
                    j += i - suffix + 1;
            }
        }

        return null;
    }

    /*private*/ static Bytes two_way_long_needle(Bytes haystack, int haystack_len, Bytes needle, int needle_len)
    {
        int[] shift_table = new int[1 << 8];

        int[] period = new int[1];
        int suffix = critical_factorization(needle, needle_len, period);

        for (int i = 0; i < (1 << 8); i++)
            shift_table[i] = needle_len;
        for (int i = 0; i < needle_len; i++)
            shift_table[char_u(needle.at(i))] = needle_len - i - 1;

        if (MEMCMP(needle, needle.plus(period[0]), suffix) == 0)
        {
            int memory = 0;
            int j = 0;
            while (MEMCHR(haystack.plus(haystack_len), NUL, j + needle_len - haystack_len) == null && (haystack_len = j + needle_len) != 0)
            {
                int shift = shift_table[char_u(haystack.at(j + needle_len - 1))];
                if (0 < shift)
                {
                    if (memory != 0 && shift < period[0])
                    {
                        shift = needle_len - period[0];
                        memory = 0;
                    }
                    j += shift;
                    continue;
                }
                int i = (suffix < memory) ? memory : suffix;
                while (i < needle_len - 1 && needle.at(i) == haystack.at(i + j))
                    i++;
                if (needle_len - 1 <= i)
                {
                    i = suffix - 1;
                    while (memory < i + 1 && needle.at(i) == haystack.at(i + j))
                        --i;
                    if (i + 1 < memory + 1)
                        return haystack.plus(j);

                    j += period[0];
                    memory = needle_len - period[0];
                }
                else
                {
                    j += i - suffix + 1;
                    memory = 0;
                }
            }
        }
        else
        {
            period[0] = ((suffix < needle_len - suffix) ? needle_len - suffix : suffix) + 1;
            int j = 0;
            while (MEMCHR(haystack.plus(haystack_len), NUL, j + needle_len - haystack_len) == null && (haystack_len = j + needle_len) != 0)
            {
                int shift = shift_table[char_u(haystack.at(j + needle_len - 1))];
                if (0 < shift)
                {
                    j += shift;
                    continue;
                }
                int i = suffix;
                while (i < needle_len - 1 && needle.at(i) == haystack.at(i + j))
                    i++;
                if (needle_len - 1 <= i)
                {
                    i = suffix - 1;
                    while (i != SIZE_MAX && needle.at(i) == haystack.at(i + j))
                        --i;
                    if (i == SIZE_MAX)
                        return haystack.plus(j);
                    j += period[0];
                }
                else
                    j += i - suffix + 1;
            }
        }

        return null;
    }

    /*private*/ static Bytes STRSTR(Bytes haystack_start, Bytes needle_start)
    {
        Bytes haystack = haystack_start;
        Bytes needle = needle_start;

        boolean ok = true;
        while (haystack.at(0) != NUL && needle.at(0) != NUL)
            ok &= ((haystack = haystack.plus(1)).at(-1) == (needle = needle.plus(1)).at(-1));
        if (needle.at(0) != NUL)
            return null;
        if (ok)
            return haystack_start;

        int needle_len = BDIFF(needle, needle_start);
        haystack = STRCHR(haystack_start.plus(1), needle_start.at(0));
        if (haystack == null || needle_len == 1)
            return haystack;

        needle = needle.minus(needle_len);
        int haystack_len = (BLT(haystack_start.plus(needle_len), haystack)) ? 1 : BDIFF(haystack_start.plus(needle_len), haystack);

        if (needle_len < 32)
            return two_way_short_needle(haystack, haystack_len, needle, needle_len);

        return two_way_long_needle(haystack, haystack_len, needle, needle_len);
    }

    /* ----------------------------------------------------------------------- */

    /*private*/ static final Bytes VIM_VERSION_LONG = u8("VIM - Vi IMproved 7.4.692");

    /*private*/ static Bytes longVersion = VIM_VERSION_LONG;

    /*private*/ static final Bytes VIMRUNTIME = u8("./runtime");
}
