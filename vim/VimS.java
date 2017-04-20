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
public class VimS
{
    /*
     * sha256.c ---------------------------------------------------------------------------------------
     */

    /*
     * FIPS-180-2 compliant SHA-256 implementation
     */

    /*private*/ static int get_uint32(Bytes b, int i)
    {
        return ((int)b.at(i    ) << 24)
             | ((int)b.at(i + 1) << 16)
             | ((int)b.at(i + 2) <<  8)
             | ((int)b.at(i + 3)      );
    }

    /*private*/ static void put_uint32(int n, Bytes b, int i)
    {
        b.be(i    , (byte)((n >>> 24)       )); // & 0xff
        b.be(i + 1, (byte)((n >>> 16) & 0xff));
        b.be(i + 2, (byte)((n >>>  8) & 0xff));
        b.be(i + 3, (byte)((n       ) & 0xff));
    }

    /*private*/ static void sha256_start(context_sha256_C ctx)
    {
        ctx.total = 0;

        ctx.state[0] = 0x6A09E667;
        ctx.state[1] = 0xBB67AE85;
        ctx.state[2] = 0x3C6EF372;
        ctx.state[3] = 0xA54FF53A;
        ctx.state[4] = 0x510E527F;
        ctx.state[5] = 0x9B05688C;
        ctx.state[6] = 0x1F83D9AB;
        ctx.state[7] = 0x5BE0CD19;
    }

    /*private*/ static int SHR(int x, int n) { return (x >>> n); }
    /*private*/ static int ROTR(int x, int n) { return (SHR(x, n) | (x << (32 - n))); }

    /*private*/ static int S0(int x) { return (ROTR(x, 7) ^ ROTR(x, 18) ^ SHR(x, 3)); }
    /*private*/ static int S1(int x) { return (ROTR(x, 17) ^ ROTR(x, 19) ^ SHR(x, 10)); }

    /*private*/ static int S2(int x) { return (ROTR(x, 2) ^ ROTR(x, 13) ^ ROTR(x, 22)); }
    /*private*/ static int S3(int x) { return (ROTR(x, 6) ^ ROTR(x, 11) ^ ROTR(x, 25)); }

    /*private*/ static int F0(int x, int y, int z) { return ((x & y) | (z & (x | y))); }
    /*private*/ static int F1(int x, int y, int z) { return (z ^ (x & (y ^ z))); }

    /*private*/ static int R(int[] W, int t)
    {
        if (16 <= t)
            W[t] = S1(W[t - 2]) + W[t - 7] + S0(W[t - 15]) + W[t - 16];

        return W[t];
    }

    /*private*/ static void P(int[] Z, int a, int b, int c, int d, int e, int f, int g, int h, int[] W, int t, int K)
    {
        int temp1 = Z[h] + S3(Z[e]) + F1(Z[e], Z[f], Z[g]) + K + R(W, t);
        int temp2 = S2(Z[a]) + F0(Z[a], Z[b], Z[c]);

        Z[d] += temp1;
        Z[h] = temp1 + temp2;
    }

    /*private*/ static void sha256_process(context_sha256_C ctx, Bytes/*(64)*/ data)
    {
        int[] W = new int[64];
        int[] Z = new int[8];

        for (int i = 0; i < 16; i++)
            W[i] = get_uint32(data, i << 2);

        for (int i = 0; i < 8; i++)
            Z[i] = ctx.state[i];

        P(Z, 0, 1, 2, 3, 4, 5, 6, 7, W,  0, 0x428A2F98);
        P(Z, 7, 0, 1, 2, 3, 4, 5, 6, W,  1, 0x71374491);
        P(Z, 6, 7, 0, 1, 2, 3, 4, 5, W,  2, 0xB5C0FBCF);
        P(Z, 5, 6, 7, 0, 1, 2, 3, 4, W,  3, 0xE9B5DBA5);
        P(Z, 4, 5, 6, 7, 0, 1, 2, 3, W,  4, 0x3956C25B);
        P(Z, 3, 4, 5, 6, 7, 0, 1, 2, W,  5, 0x59F111F1);
        P(Z, 2, 3, 4, 5, 6, 7, 0, 1, W,  6, 0x923F82A4);
        P(Z, 1, 2, 3, 4, 5, 6, 7, 0, W,  7, 0xAB1C5ED5);
        P(Z, 0, 1, 2, 3, 4, 5, 6, 7, W,  8, 0xD807AA98);
        P(Z, 7, 0, 1, 2, 3, 4, 5, 6, W,  9, 0x12835B01);
        P(Z, 6, 7, 0, 1, 2, 3, 4, 5, W, 10, 0x243185BE);
        P(Z, 5, 6, 7, 0, 1, 2, 3, 4, W, 11, 0x550C7DC3);
        P(Z, 4, 5, 6, 7, 0, 1, 2, 3, W, 12, 0x72BE5D74);
        P(Z, 3, 4, 5, 6, 7, 0, 1, 2, W, 13, 0x80DEB1FE);
        P(Z, 2, 3, 4, 5, 6, 7, 0, 1, W, 14, 0x9BDC06A7);
        P(Z, 1, 2, 3, 4, 5, 6, 7, 0, W, 15, 0xC19BF174);
        P(Z, 0, 1, 2, 3, 4, 5, 6, 7, W, 16, 0xE49B69C1);
        P(Z, 7, 0, 1, 2, 3, 4, 5, 6, W, 17, 0xEFBE4786);
        P(Z, 6, 7, 0, 1, 2, 3, 4, 5, W, 18, 0x0FC19DC6);
        P(Z, 5, 6, 7, 0, 1, 2, 3, 4, W, 19, 0x240CA1CC);
        P(Z, 4, 5, 6, 7, 0, 1, 2, 3, W, 20, 0x2DE92C6F);
        P(Z, 3, 4, 5, 6, 7, 0, 1, 2, W, 21, 0x4A7484AA);
        P(Z, 2, 3, 4, 5, 6, 7, 0, 1, W, 22, 0x5CB0A9DC);
        P(Z, 1, 2, 3, 4, 5, 6, 7, 0, W, 23, 0x76F988DA);
        P(Z, 0, 1, 2, 3, 4, 5, 6, 7, W, 24, 0x983E5152);
        P(Z, 7, 0, 1, 2, 3, 4, 5, 6, W, 25, 0xA831C66D);
        P(Z, 6, 7, 0, 1, 2, 3, 4, 5, W, 26, 0xB00327C8);
        P(Z, 5, 6, 7, 0, 1, 2, 3, 4, W, 27, 0xBF597FC7);
        P(Z, 4, 5, 6, 7, 0, 1, 2, 3, W, 28, 0xC6E00BF3);
        P(Z, 3, 4, 5, 6, 7, 0, 1, 2, W, 29, 0xD5A79147);
        P(Z, 2, 3, 4, 5, 6, 7, 0, 1, W, 30, 0x06CA6351);
        P(Z, 1, 2, 3, 4, 5, 6, 7, 0, W, 31, 0x14292967);
        P(Z, 0, 1, 2, 3, 4, 5, 6, 7, W, 32, 0x27B70A85);
        P(Z, 7, 0, 1, 2, 3, 4, 5, 6, W, 33, 0x2E1B2138);
        P(Z, 6, 7, 0, 1, 2, 3, 4, 5, W, 34, 0x4D2C6DFC);
        P(Z, 5, 6, 7, 0, 1, 2, 3, 4, W, 35, 0x53380D13);
        P(Z, 4, 5, 6, 7, 0, 1, 2, 3, W, 36, 0x650A7354);
        P(Z, 3, 4, 5, 6, 7, 0, 1, 2, W, 37, 0x766A0ABB);
        P(Z, 2, 3, 4, 5, 6, 7, 0, 1, W, 38, 0x81C2C92E);
        P(Z, 1, 2, 3, 4, 5, 6, 7, 0, W, 39, 0x92722C85);
        P(Z, 0, 1, 2, 3, 4, 5, 6, 7, W, 40, 0xA2BFE8A1);
        P(Z, 7, 0, 1, 2, 3, 4, 5, 6, W, 41, 0xA81A664B);
        P(Z, 6, 7, 0, 1, 2, 3, 4, 5, W, 42, 0xC24B8B70);
        P(Z, 5, 6, 7, 0, 1, 2, 3, 4, W, 43, 0xC76C51A3);
        P(Z, 4, 5, 6, 7, 0, 1, 2, 3, W, 44, 0xD192E819);
        P(Z, 3, 4, 5, 6, 7, 0, 1, 2, W, 45, 0xD6990624);
        P(Z, 2, 3, 4, 5, 6, 7, 0, 1, W, 46, 0xF40E3585);
        P(Z, 1, 2, 3, 4, 5, 6, 7, 0, W, 47, 0x106AA070);
        P(Z, 0, 1, 2, 3, 4, 5, 6, 7, W, 48, 0x19A4C116);
        P(Z, 7, 0, 1, 2, 3, 4, 5, 6, W, 49, 0x1E376C08);
        P(Z, 6, 7, 0, 1, 2, 3, 4, 5, W, 50, 0x2748774C);
        P(Z, 5, 6, 7, 0, 1, 2, 3, 4, W, 51, 0x34B0BCB5);
        P(Z, 4, 5, 6, 7, 0, 1, 2, 3, W, 52, 0x391C0CB3);
        P(Z, 3, 4, 5, 6, 7, 0, 1, 2, W, 53, 0x4ED8AA4A);
        P(Z, 2, 3, 4, 5, 6, 7, 0, 1, W, 54, 0x5B9CCA4F);
        P(Z, 1, 2, 3, 4, 5, 6, 7, 0, W, 55, 0x682E6FF3);
        P(Z, 0, 1, 2, 3, 4, 5, 6, 7, W, 56, 0x748F82EE);
        P(Z, 7, 0, 1, 2, 3, 4, 5, 6, W, 57, 0x78A5636F);
        P(Z, 6, 7, 0, 1, 2, 3, 4, 5, W, 58, 0x84C87814);
        P(Z, 5, 6, 7, 0, 1, 2, 3, 4, W, 59, 0x8CC70208);
        P(Z, 4, 5, 6, 7, 0, 1, 2, 3, W, 60, 0x90BEFFFA);
        P(Z, 3, 4, 5, 6, 7, 0, 1, 2, W, 61, 0xA4506CEB);
        P(Z, 2, 3, 4, 5, 6, 7, 0, 1, W, 62, 0xBEF9A3F7);
        P(Z, 1, 2, 3, 4, 5, 6, 7, 0, W, 63, 0xC67178F2);

        for (int i = 0; i < 8; i++)
            ctx.state[i] += Z[i];
    }

    /*private*/ static void sha256_update(context_sha256_C ctx, Bytes input, int length)
    {
        if (length == 0)
            return;

        int left = (int)(ctx.total & 0x3f);
        int fill = 64 - left;

        ctx.total += length;

        if (0 < left && fill <= length)
        {
            BCOPY(ctx.buffer, left, input, 0, fill);
            sha256_process(ctx, ctx.buffer);
            length -= fill;
            input = input.plus(fill);
            left = 0;
        }

        while (64 <= length)
        {
            sha256_process(ctx, input);
            length -= 64;
            input = input.plus(64);
        }

        if (0 < length)
            BCOPY(ctx.buffer, left, input, 0, length);
    }

    /*private*/ static Bytes sha256_padding = new Bytes(new byte[/*64*/]
    {
        (byte)0x80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    });

    /*private*/ static void sha256_finish(context_sha256_C ctx, Bytes/*(32)*/ digest)
    {
        Bytes msglen = new Bytes(8);

        final long roof = 0xffffffffL;
        int high = (int)((ctx.total >>> 29) & roof);
        int low  = (int)((ctx.total << 3) & roof);

        put_uint32(high, msglen, 0);
        put_uint32(low,  msglen, 4);

        int last = (int)(ctx.total & 0x3f);
        int padn = (last < 56) ? (56 - last) : (120 - last);

        sha256_update(ctx, sha256_padding, padn);
        sha256_update(ctx, msglen, 8);

        put_uint32(ctx.state[0], digest,  0);
        put_uint32(ctx.state[1], digest,  4);
        put_uint32(ctx.state[2], digest,  8);
        put_uint32(ctx.state[3], digest, 12);
        put_uint32(ctx.state[4], digest, 16);
        put_uint32(ctx.state[5], digest, 20);
        put_uint32(ctx.state[6], digest, 24);
        put_uint32(ctx.state[7], digest, 28);
    }
}
