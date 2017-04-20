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
public class VimR
{
    /*
     * digraph.c: code for digraphs -------------------------------------------------------------------
     */

    /*private*/ static final class digr_C
    {
        byte char1;
        byte char2;
        int result;

        /*private*/ digr_C()
        {
        }

        /*private*/ digr_C(byte char1, byte char2, int result)
        {
            this.char1 = char1;
            this.char2 = char2;
            this.result = result;
        }
    }

    /* digraphs added by the user */
    /*private*/ static Growing<digr_C> user_digraphs = new Growing<digr_C>(digr_C.class, 10);

    /*private*/ static final digr_C digr(/*byte*/char char1, /*byte*/char char2, int result)
    {
        return new digr_C((byte)char1, (byte)char2, result);
    }

    /* digraphs for Unicode from RFC1345 (also work for ISO-8859-1 aka latin1) */
    /*private*/ static digr_C[] digraphdefault =
    {
        digr('N', 'U', 0x0a),       /* LF for NUL */
        digr('S', 'H', 0x01),
        digr('S', 'X', 0x02),
        digr('E', 'X', 0x03),
        digr('E', 'T', 0x04),
        digr('E', 'Q', 0x05),
        digr('A', 'K', 0x06),
        digr('B', 'L', 0x07),
        digr('B', 'S', 0x08),
        digr('H', 'T', 0x09),
        digr('L', 'F', 0x0a),
        digr('V', 'T', 0x0b),
        digr('F', 'F', 0x0c),
        digr('C', 'R', 0x0d),
        digr('S', 'O', 0x0e),
        digr('S', 'I', 0x0f),
        digr('D', 'L', 0x10),
        digr('D', '1', 0x11),
        digr('D', '2', 0x12),
        digr('D', '3', 0x13),
        digr('D', '4', 0x14),
        digr('N', 'K', 0x15),
        digr('S', 'Y', 0x16),
        digr('E', 'B', 0x17),
        digr('C', 'N', 0x18),
        digr('E', 'M', 0x19),
        digr('S', 'B', 0x1a),
        digr('E', 'C', 0x1b),
        digr('F', 'S', 0x1c),
        digr('G', 'S', 0x1d),
        digr('R', 'S', 0x1e),
        digr('U', 'S', 0x1f),
        digr('S', 'P', 0x20),
        digr('N', 'b', 0x23),
        digr('D', 'O', 0x24),
        digr('A', 't', 0x40),
        digr('<', '(', 0x5b),
        digr('/', '/', 0x5c),
        digr(')', '>', 0x5d),
        digr('\'', '>', 0x5e),
        digr('\'', '!', 0x60),
        digr('(', '!', 0x7b),
        digr('!', '!', 0x7c),
        digr('!', ')', 0x7d),
        digr('\'', '?', 0x7e),
        digr('D', 'T', 0x7f),
        digr('P', 'A', 0x80),
        digr('H', 'O', 0x81),
        digr('B', 'H', 0x82),
        digr('N', 'H', 0x83),
        digr('I', 'N', 0x84),
        digr('N', 'L', 0x85),
        digr('S', 'A', 0x86),
        digr('E', 'S', 0x87),
        digr('H', 'S', 0x88),
        digr('H', 'J', 0x89),
        digr('V', 'S', 0x8a),
        digr('P', 'D', 0x8b),
        digr('P', 'U', 0x8c),
        digr('R', 'I', 0x8d),
        digr('S', '2', 0x8e),
        digr('S', '3', 0x8f),
        digr('D', 'C', 0x90),
        digr('P', '1', 0x91),
        digr('P', '2', 0x92),
        digr('T', 'S', 0x93),
        digr('C', 'C', 0x94),
        digr('M', 'W', 0x95),
        digr('S', 'G', 0x96),
        digr('E', 'G', 0x97),
        digr('S', 'S', 0x98),
        digr('G', 'C', 0x99),
        digr('S', 'C', 0x9a),
        digr('C', 'I', 0x9b),
        digr('S', 'T', 0x9c),
        digr('O', 'C', 0x9d),
        digr('P', 'M', 0x9e),
        digr('A', 'C', 0x9f),
        digr('N', 'S', 0xa0),
        digr('!', 'I', 0xa1),
        digr('C', 't', 0xa2),
        digr('P', 'd', 0xa3),
        digr('C', 'u', 0xa4),
        digr('Y', 'e', 0xa5),
        digr('B', 'B', 0xa6),
        digr('S', 'E', 0xa7),
        digr('\'', ':', 0xa8),
        digr('C', 'o', 0xa9),
        digr('-', 'a', 0xaa),
        digr('<', '<', 0xab),
        digr('N', 'O', 0xac),
        digr('-', '-', 0xad),
        digr('R', 'g', 0xae),
        digr('\'', 'm', 0xaf),
        digr('D', 'G', 0xb0),
        digr('+', '-', 0xb1),
        digr('2', 'S', 0xb2),
        digr('3', 'S', 0xb3),
        digr('\'', '\'', 0xb4),
        digr('M', 'y', 0xb5),
        digr('P', 'I', 0xb6),
        digr('.', 'M', 0xb7),
        digr('\'', ',', 0xb8),
        digr('1', 'S', 0xb9),
        digr('-', 'o', 0xba),
        digr('>', '>', 0xbb),
        digr('1', '4', 0xbc),
        digr('1', '2', 0xbd),
        digr('3', '4', 0xbe),
        digr('?', 'I', 0xbf),
        digr('A', '!', 0xc0),
        digr('A', '\'', 0xc1),
        digr('A', '>', 0xc2),
        digr('A', '?', 0xc3),
        digr('A', ':', 0xc4),
        digr('A', 'A', 0xc5),
        digr('A', 'E', 0xc6),
        digr('C', ',', 0xc7),
        digr('E', '!', 0xc8),
        digr('E', '\'', 0xc9),
        digr('E', '>', 0xca),
        digr('E', ':', 0xcb),
        digr('I', '!', 0xcc),
        digr('I', '\'', 0xcd),
        digr('I', '>', 0xce),
        digr('I', ':', 0xcf),
        digr('D', '-', 0xd0),
        digr('N', '?', 0xd1),
        digr('O', '!', 0xd2),
        digr('O', '\'', 0xd3),
        digr('O', '>', 0xd4),
        digr('O', '?', 0xd5),
        digr('O', ':', 0xd6),
        digr('*', 'X', 0xd7),
        digr('O', '/', 0xd8),
        digr('U', '!', 0xd9),
        digr('U', '\'', 0xda),
        digr('U', '>', 0xdb),
        digr('U', ':', 0xdc),
        digr('Y', '\'', 0xdd),
        digr('T', 'H', 0xde),
        digr('s', 's', 0xdf),
        digr('a', '!', 0xe0),
        digr('a', '\'', 0xe1),
        digr('a', '>', 0xe2),
        digr('a', '?', 0xe3),
        digr('a', ':', 0xe4),
        digr('a', 'a', 0xe5),
        digr('a', 'e', 0xe6),
        digr('c', ',', 0xe7),
        digr('e', '!', 0xe8),
        digr('e', '\'', 0xe9),
        digr('e', '>', 0xea),
        digr('e', ':', 0xeb),
        digr('i', '!', 0xec),
        digr('i', '\'', 0xed),
        digr('i', '>', 0xee),
        digr('i', ':', 0xef),
        digr('d', '-', 0xf0),
        digr('n', '?', 0xf1),
        digr('o', '!', 0xf2),
        digr('o', '\'', 0xf3),
        digr('o', '>', 0xf4),
        digr('o', '?', 0xf5),
        digr('o', ':', 0xf6),
        digr('-', ':', 0xf7),
        digr('o', '/', 0xf8),
        digr('u', '!', 0xf9),
        digr('u', '\'', 0xfa),
        digr('u', '>', 0xfb),
        digr('u', ':', 0xfc),
        digr('y', '\'', 0xfd),
        digr('t', 'h', 0xfe),
        digr('y', ':', 0xff),

        digr('A', '-', 0x0100),
        digr('a', '-', 0x0101),
        digr('A', '(', 0x0102),
        digr('a', '(', 0x0103),
        digr('A', ';', 0x0104),
        digr('a', ';', 0x0105),
        digr('C', '\'', 0x0106),
        digr('c', '\'', 0x0107),
        digr('C', '>', 0x0108),
        digr('c', '>', 0x0109),
        digr('C', '.', 0x010a),
        digr('c', '.', 0x010b),
        digr('C', '<', 0x010c),
        digr('c', '<', 0x010d),
        digr('D', '<', 0x010e),
        digr('d', '<', 0x010f),
        digr('D', '/', 0x0110),
        digr('d', '/', 0x0111),
        digr('E', '-', 0x0112),
        digr('e', '-', 0x0113),
        digr('E', '(', 0x0114),
        digr('e', '(', 0x0115),
        digr('E', '.', 0x0116),
        digr('e', '.', 0x0117),
        digr('E', ';', 0x0118),
        digr('e', ';', 0x0119),
        digr('E', '<', 0x011a),
        digr('e', '<', 0x011b),
        digr('G', '>', 0x011c),
        digr('g', '>', 0x011d),
        digr('G', '(', 0x011e),
        digr('g', '(', 0x011f),
        digr('G', '.', 0x0120),
        digr('g', '.', 0x0121),
        digr('G', ',', 0x0122),
        digr('g', ',', 0x0123),
        digr('H', '>', 0x0124),
        digr('h', '>', 0x0125),
        digr('H', '/', 0x0126),
        digr('h', '/', 0x0127),
        digr('I', '?', 0x0128),
        digr('i', '?', 0x0129),
        digr('I', '-', 0x012a),
        digr('i', '-', 0x012b),
        digr('I', '(', 0x012c),
        digr('i', '(', 0x012d),
        digr('I', ';', 0x012e),
        digr('i', ';', 0x012f),
        digr('I', '.', 0x0130),
        digr('i', '.', 0x0131),
        digr('I', 'J', 0x0132),
        digr('i', 'j', 0x0133),
        digr('J', '>', 0x0134),
        digr('j', '>', 0x0135),
        digr('K', ',', 0x0136),
        digr('k', ',', 0x0137),
        digr('k', 'k', 0x0138),
        digr('L', '\'', 0x0139),
        digr('l', '\'', 0x013a),
        digr('L', ',', 0x013b),
        digr('l', ',', 0x013c),
        digr('L', '<', 0x013d),
        digr('l', '<', 0x013e),
        digr('L', '.', 0x013f),
        digr('l', '.', 0x0140),
        digr('L', '/', 0x0141),
        digr('l', '/', 0x0142),
        digr('N', '\'', 0x0143),
        digr('n', '\'', 0x0144),
        digr('N', ',', 0x0145),
        digr('n', ',', 0x0146),
        digr('N', '<', 0x0147),
        digr('n', '<', 0x0148),
        digr('\'', 'n', 0x0149),
        digr('N', 'G', 0x014a),
        digr('n', 'g', 0x014b),
        digr('O', '-', 0x014c),
        digr('o', '-', 0x014d),
        digr('O', '(', 0x014e),
        digr('o', '(', 0x014f),
        digr('O', '"', 0x0150),
        digr('o', '"', 0x0151),
        digr('O', 'E', 0x0152),
        digr('o', 'e', 0x0153),
        digr('R', '\'', 0x0154),
        digr('r', '\'', 0x0155),
        digr('R', ',', 0x0156),
        digr('r', ',', 0x0157),
        digr('R', '<', 0x0158),
        digr('r', '<', 0x0159),
        digr('S', '\'', 0x015a),
        digr('s', '\'', 0x015b),
        digr('S', '>', 0x015c),
        digr('s', '>', 0x015d),
        digr('S', ',', 0x015e),
        digr('s', ',', 0x015f),
        digr('S', '<', 0x0160),
        digr('s', '<', 0x0161),
        digr('T', ',', 0x0162),
        digr('t', ',', 0x0163),
        digr('T', '<', 0x0164),
        digr('t', '<', 0x0165),
        digr('T', '/', 0x0166),
        digr('t', '/', 0x0167),
        digr('U', '?', 0x0168),
        digr('u', '?', 0x0169),
        digr('U', '-', 0x016a),
        digr('u', '-', 0x016b),
        digr('U', '(', 0x016c),
        digr('u', '(', 0x016d),
        digr('U', '0', 0x016e),
        digr('u', '0', 0x016f),
        digr('U', '"', 0x0170),
        digr('u', '"', 0x0171),
        digr('U', ';', 0x0172),
        digr('u', ';', 0x0173),
        digr('W', '>', 0x0174),
        digr('w', '>', 0x0175),
        digr('Y', '>', 0x0176),
        digr('y', '>', 0x0177),
        digr('Y', ':', 0x0178),
        digr('Z', '\'', 0x0179),
        digr('z', '\'', 0x017a),
        digr('Z', '.', 0x017b),
        digr('z', '.', 0x017c),
        digr('Z', '<', 0x017d),
        digr('z', '<', 0x017e),
        digr('O', '9', 0x01a0),
        digr('o', '9', 0x01a1),
        digr('O', 'I', 0x01a2),
        digr('o', 'i', 0x01a3),
        digr('y', 'r', 0x01a6),
        digr('U', '9', 0x01af),
        digr('u', '9', 0x01b0),
        digr('Z', '/', 0x01b5),
        digr('z', '/', 0x01b6),
        digr('E', 'D', 0x01b7),
        digr('A', '<', 0x01cd),
        digr('a', '<', 0x01ce),
        digr('I', '<', 0x01cf),
        digr('i', '<', 0x01d0),
        digr('O', '<', 0x01d1),
        digr('o', '<', 0x01d2),
        digr('U', '<', 0x01d3),
        digr('u', '<', 0x01d4),
        digr('A', '1', 0x01de),
        digr('a', '1', 0x01df),
        digr('A', '7', 0x01e0),
        digr('a', '7', 0x01e1),
        digr('A', '3', 0x01e2),
        digr('a', '3', 0x01e3),
        digr('G', '/', 0x01e4),
        digr('g', '/', 0x01e5),
        digr('G', '<', 0x01e6),
        digr('g', '<', 0x01e7),
        digr('K', '<', 0x01e8),
        digr('k', '<', 0x01e9),
        digr('O', ';', 0x01ea),
        digr('o', ';', 0x01eb),
        digr('O', '1', 0x01ec),
        digr('o', '1', 0x01ed),
        digr('E', 'Z', 0x01ee),
        digr('e', 'z', 0x01ef),
        digr('j', '<', 0x01f0),
        digr('G', '\'', 0x01f4),
        digr('g', '\'', 0x01f5),
        digr(';', 'S', 0x02bf),
        digr('\'', '<', 0x02c7),
        digr('\'', '(', 0x02d8),
        digr('\'', '.', 0x02d9),
        digr('\'', '0', 0x02da),
        digr('\'', ';', 0x02db),
        digr('\'', '"', 0x02dd),
        digr('A', '%', 0x0386),
        digr('E', '%', 0x0388),
        digr('Y', '%', 0x0389),
        digr('I', '%', 0x038a),
        digr('O', '%', 0x038c),
        digr('U', '%', 0x038e),
        digr('W', '%', 0x038f),
        digr('i', '3', 0x0390),
        digr('A', '*', 0x0391),
        digr('B', '*', 0x0392),
        digr('G', '*', 0x0393),
        digr('D', '*', 0x0394),
        digr('E', '*', 0x0395),
        digr('Z', '*', 0x0396),
        digr('Y', '*', 0x0397),
        digr('H', '*', 0x0398),
        digr('I', '*', 0x0399),
        digr('K', '*', 0x039a),
        digr('L', '*', 0x039b),
        digr('M', '*', 0x039c),
        digr('N', '*', 0x039d),
        digr('C', '*', 0x039e),
        digr('O', '*', 0x039f),
        digr('P', '*', 0x03a0),
        digr('R', '*', 0x03a1),
        digr('S', '*', 0x03a3),
        digr('T', '*', 0x03a4),
        digr('U', '*', 0x03a5),
        digr('F', '*', 0x03a6),
        digr('X', '*', 0x03a7),
        digr('Q', '*', 0x03a8),
        digr('W', '*', 0x03a9),
        digr('J', '*', 0x03aa),
        digr('V', '*', 0x03ab),
        digr('a', '%', 0x03ac),
        digr('e', '%', 0x03ad),
        digr('y', '%', 0x03ae),
        digr('i', '%', 0x03af),
        digr('u', '3', 0x03b0),
        digr('a', '*', 0x03b1),
        digr('b', '*', 0x03b2),
        digr('g', '*', 0x03b3),
        digr('d', '*', 0x03b4),
        digr('e', '*', 0x03b5),
        digr('z', '*', 0x03b6),
        digr('y', '*', 0x03b7),
        digr('h', '*', 0x03b8),
        digr('i', '*', 0x03b9),
        digr('k', '*', 0x03ba),
        digr('l', '*', 0x03bb),
        digr('m', '*', 0x03bc),
        digr('n', '*', 0x03bd),
        digr('c', '*', 0x03be),
        digr('o', '*', 0x03bf),
        digr('p', '*', 0x03c0),
        digr('r', '*', 0x03c1),
        digr('*', 's', 0x03c2),
        digr('s', '*', 0x03c3),
        digr('t', '*', 0x03c4),
        digr('u', '*', 0x03c5),
        digr('f', '*', 0x03c6),
        digr('x', '*', 0x03c7),
        digr('q', '*', 0x03c8),
        digr('w', '*', 0x03c9),
        digr('j', '*', 0x03ca),
        digr('v', '*', 0x03cb),
        digr('o', '%', 0x03cc),
        digr('u', '%', 0x03cd),
        digr('w', '%', 0x03ce),
        digr('\'', 'G', 0x03d8),
        digr(',', 'G', 0x03d9),
        digr('T', '3', 0x03da),
        digr('t', '3', 0x03db),
        digr('M', '3', 0x03dc),
        digr('m', '3', 0x03dd),
        digr('K', '3', 0x03de),
        digr('k', '3', 0x03df),
        digr('P', '3', 0x03e0),
        digr('p', '3', 0x03e1),
        digr('\'', '%', 0x03f4),
        digr('j', '3', 0x03f5),
        digr('I', 'O', 0x0401),
        digr('D', '%', 0x0402),
        digr('G', '%', 0x0403),
        digr('I', 'E', 0x0404),
        digr('D', 'S', 0x0405),
        digr('I', 'I', 0x0406),
        digr('Y', 'I', 0x0407),
        digr('J', '%', 0x0408),
        digr('L', 'J', 0x0409),
        digr('N', 'J', 0x040a),
        digr('T', 's', 0x040b),
        digr('K', 'J', 0x040c),
        digr('V', '%', 0x040e),
        digr('D', 'Z', 0x040f),
        digr('A', '=', 0x0410),
        digr('B', '=', 0x0411),
        digr('V', '=', 0x0412),
        digr('G', '=', 0x0413),
        digr('D', '=', 0x0414),
        digr('E', '=', 0x0415),
        digr('Z', '%', 0x0416),
        digr('Z', '=', 0x0417),
        digr('I', '=', 0x0418),
        digr('J', '=', 0x0419),
        digr('K', '=', 0x041a),
        digr('L', '=', 0x041b),
        digr('M', '=', 0x041c),
        digr('N', '=', 0x041d),
        digr('O', '=', 0x041e),
        digr('P', '=', 0x041f),
        digr('R', '=', 0x0420),
        digr('S', '=', 0x0421),
        digr('T', '=', 0x0422),
        digr('U', '=', 0x0423),
        digr('F', '=', 0x0424),
        digr('H', '=', 0x0425),
        digr('C', '=', 0x0426),
        digr('C', '%', 0x0427),
        digr('S', '%', 0x0428),
        digr('S', 'c', 0x0429),
        digr('=', '"', 0x042a),
        digr('Y', '=', 0x042b),
        digr('%', '"', 0x042c),
        digr('J', 'E', 0x042d),
        digr('J', 'U', 0x042e),
        digr('J', 'A', 0x042f),
        digr('a', '=', 0x0430),
        digr('b', '=', 0x0431),
        digr('v', '=', 0x0432),
        digr('g', '=', 0x0433),
        digr('d', '=', 0x0434),
        digr('e', '=', 0x0435),
        digr('z', '%', 0x0436),
        digr('z', '=', 0x0437),
        digr('i', '=', 0x0438),
        digr('j', '=', 0x0439),
        digr('k', '=', 0x043a),
        digr('l', '=', 0x043b),
        digr('m', '=', 0x043c),
        digr('n', '=', 0x043d),
        digr('o', '=', 0x043e),
        digr('p', '=', 0x043f),
        digr('r', '=', 0x0440),
        digr('s', '=', 0x0441),
        digr('t', '=', 0x0442),
        digr('u', '=', 0x0443),
        digr('f', '=', 0x0444),
        digr('h', '=', 0x0445),
        digr('c', '=', 0x0446),
        digr('c', '%', 0x0447),
        digr('s', '%', 0x0448),
        digr('s', 'c', 0x0449),
        digr('=', '\'', 0x044a),
        digr('y', '=', 0x044b),
        digr('%', '\'', 0x044c),
        digr('j', 'e', 0x044d),
        digr('j', 'u', 0x044e),
        digr('j', 'a', 0x044f),
        digr('i', 'o', 0x0451),
        digr('d', '%', 0x0452),
        digr('g', '%', 0x0453),
        digr('i', 'e', 0x0454),
        digr('d', 's', 0x0455),
        digr('i', 'i', 0x0456),
        digr('y', 'i', 0x0457),
        digr('j', '%', 0x0458),
        digr('l', 'j', 0x0459),
        digr('n', 'j', 0x045a),
        digr('t', 's', 0x045b),
        digr('k', 'j', 0x045c),
        digr('v', '%', 0x045e),
        digr('d', 'z', 0x045f),
        digr('Y', '3', 0x0462),
        digr('y', '3', 0x0463),
        digr('O', '3', 0x046a),
        digr('o', '3', 0x046b),
        digr('F', '3', 0x0472),
        digr('f', '3', 0x0473),
        digr('V', '3', 0x0474),
        digr('v', '3', 0x0475),
        digr('C', '3', 0x0480),
        digr('c', '3', 0x0481),
        digr('G', '3', 0x0490),
        digr('g', '3', 0x0491),
        digr('A', '+', 0x05d0),
        digr('B', '+', 0x05d1),
        digr('G', '+', 0x05d2),
        digr('D', '+', 0x05d3),
        digr('H', '+', 0x05d4),
        digr('W', '+', 0x05d5),
        digr('Z', '+', 0x05d6),
        digr('X', '+', 0x05d7),
        digr('T', 'j', 0x05d8),
        digr('J', '+', 0x05d9),
        digr('K', '%', 0x05da),
        digr('K', '+', 0x05db),
        digr('L', '+', 0x05dc),
        digr('M', '%', 0x05dd),
        digr('M', '+', 0x05de),
        digr('N', '%', 0x05df),
        digr('N', '+', 0x05e0),
        digr('S', '+', 0x05e1),
        digr('E', '+', 0x05e2),
        digr('P', '%', 0x05e3),
        digr('P', '+', 0x05e4),
        digr('Z', 'j', 0x05e5),
        digr('Z', 'J', 0x05e6),
        digr('Q', '+', 0x05e7),
        digr('R', '+', 0x05e8),
        digr('S', 'h', 0x05e9),
        digr('T', '+', 0x05ea),
        digr(',', '+', 0x060c),
        digr(';', '+', 0x061b),
        digr('?', '+', 0x061f),
        digr('H', '\'', 0x0621),
        digr('a', 'M', 0x0622),
        digr('a', 'H', 0x0623),
        digr('w', 'H', 0x0624),
        digr('a', 'h', 0x0625),
        digr('y', 'H', 0x0626),
        digr('a', '+', 0x0627),
        digr('b', '+', 0x0628),
        digr('t', 'm', 0x0629),
        digr('t', '+', 0x062a),
        digr('t', 'k', 0x062b),
        digr('g', '+', 0x062c),
        digr('h', 'k', 0x062d),
        digr('x', '+', 0x062e),
        digr('d', '+', 0x062f),
        digr('d', 'k', 0x0630),
        digr('r', '+', 0x0631),
        digr('z', '+', 0x0632),
        digr('s', '+', 0x0633),
        digr('s', 'n', 0x0634),
        digr('c', '+', 0x0635),
        digr('d', 'd', 0x0636),
        digr('t', 'j', 0x0637),
        digr('z', 'H', 0x0638),
        digr('e', '+', 0x0639),
        digr('i', '+', 0x063a),
        digr('+', '+', 0x0640),
        digr('f', '+', 0x0641),
        digr('q', '+', 0x0642),
        digr('k', '+', 0x0643),
        digr('l', '+', 0x0644),
        digr('m', '+', 0x0645),
        digr('n', '+', 0x0646),
        digr('h', '+', 0x0647),
        digr('w', '+', 0x0648),
        digr('j', '+', 0x0649),
        digr('y', '+', 0x064a),
        digr(':', '+', 0x064b),
        digr('"', '+', 0x064c),
        digr('=', '+', 0x064d),
        digr('/', '+', 0x064e),
        digr('\'', '+', 0x064f),
        digr('1', '+', 0x0650),
        digr('3', '+', 0x0651),
        digr('0', '+', 0x0652),
        digr('a', 'S', 0x0670),
        digr('p', '+', 0x067e),
        digr('v', '+', 0x06a4),
        digr('g', 'f', 0x06af),
        digr('0', 'a', 0x06f0),
        digr('1', 'a', 0x06f1),
        digr('2', 'a', 0x06f2),
        digr('3', 'a', 0x06f3),
        digr('4', 'a', 0x06f4),
        digr('5', 'a', 0x06f5),
        digr('6', 'a', 0x06f6),
        digr('7', 'a', 0x06f7),
        digr('8', 'a', 0x06f8),
        digr('9', 'a', 0x06f9),
        digr('B', '.', 0x1e02),
        digr('b', '.', 0x1e03),
        digr('B', '_', 0x1e06),
        digr('b', '_', 0x1e07),
        digr('D', '.', 0x1e0a),
        digr('d', '.', 0x1e0b),
        digr('D', '_', 0x1e0e),
        digr('d', '_', 0x1e0f),
        digr('D', ',', 0x1e10),
        digr('d', ',', 0x1e11),
        digr('F', '.', 0x1e1e),
        digr('f', '.', 0x1e1f),
        digr('G', '-', 0x1e20),
        digr('g', '-', 0x1e21),
        digr('H', '.', 0x1e22),
        digr('h', '.', 0x1e23),
        digr('H', ':', 0x1e26),
        digr('h', ':', 0x1e27),
        digr('H', ',', 0x1e28),
        digr('h', ',', 0x1e29),
        digr('K', '\'', 0x1e30),
        digr('k', '\'', 0x1e31),
        digr('K', '_', 0x1e34),
        digr('k', '_', 0x1e35),
        digr('L', '_', 0x1e3a),
        digr('l', '_', 0x1e3b),
        digr('M', '\'', 0x1e3e),
        digr('m', '\'', 0x1e3f),
        digr('M', '.', 0x1e40),
        digr('m', '.', 0x1e41),
        digr('N', '.', 0x1e44),
        digr('n', '.', 0x1e45),
        digr('N', '_', 0x1e48),
        digr('n', '_', 0x1e49),
        digr('P', '\'', 0x1e54),
        digr('p', '\'', 0x1e55),
        digr('P', '.', 0x1e56),
        digr('p', '.', 0x1e57),
        digr('R', '.', 0x1e58),
        digr('r', '.', 0x1e59),
        digr('R', '_', 0x1e5e),
        digr('r', '_', 0x1e5f),
        digr('S', '.', 0x1e60),
        digr('s', '.', 0x1e61),
        digr('T', '.', 0x1e6a),
        digr('t', '.', 0x1e6b),
        digr('T', '_', 0x1e6e),
        digr('t', '_', 0x1e6f),
        digr('V', '?', 0x1e7c),
        digr('v', '?', 0x1e7d),
        digr('W', '!', 0x1e80),
        digr('w', '!', 0x1e81),
        digr('W', '\'', 0x1e82),
        digr('w', '\'', 0x1e83),
        digr('W', ':', 0x1e84),
        digr('w', ':', 0x1e85),
        digr('W', '.', 0x1e86),
        digr('w', '.', 0x1e87),
        digr('X', '.', 0x1e8a),
        digr('x', '.', 0x1e8b),
        digr('X', ':', 0x1e8c),
        digr('x', ':', 0x1e8d),
        digr('Y', '.', 0x1e8e),
        digr('y', '.', 0x1e8f),
        digr('Z', '>', 0x1e90),
        digr('z', '>', 0x1e91),
        digr('Z', '_', 0x1e94),
        digr('z', '_', 0x1e95),
        digr('h', '_', 0x1e96),
        digr('t', ':', 0x1e97),
        digr('w', '0', 0x1e98),
        digr('y', '0', 0x1e99),
        digr('A', '2', 0x1ea2),
        digr('a', '2', 0x1ea3),
        digr('E', '2', 0x1eba),
        digr('e', '2', 0x1ebb),
        digr('E', '?', 0x1ebc),
        digr('e', '?', 0x1ebd),
        digr('I', '2', 0x1ec8),
        digr('i', '2', 0x1ec9),
        digr('O', '2', 0x1ece),
        digr('o', '2', 0x1ecf),
        digr('U', '2', 0x1ee6),
        digr('u', '2', 0x1ee7),
        digr('Y', '!', 0x1ef2),
        digr('y', '!', 0x1ef3),
        digr('Y', '2', 0x1ef6),
        digr('y', '2', 0x1ef7),
        digr('Y', '?', 0x1ef8),
        digr('y', '?', 0x1ef9),
        digr(';', '\'', 0x1f00),
        digr(',', '\'', 0x1f01),
        digr(';', '!', 0x1f02),
        digr(',', '!', 0x1f03),
        digr('?', ';', 0x1f04),
        digr('?', ',', 0x1f05),
        digr('!', ':', 0x1f06),
        digr('?', ':', 0x1f07),
        digr('1', 'N', 0x2002),
        digr('1', 'M', 0x2003),
        digr('3', 'M', 0x2004),
        digr('4', 'M', 0x2005),
        digr('6', 'M', 0x2006),
        digr('1', 'T', 0x2009),
        digr('1', 'H', 0x200a),
        digr('-', '1', 0x2010),
        digr('-', 'N', 0x2013),
        digr('-', 'M', 0x2014),
        digr('-', '3', 0x2015),
        digr('!', '2', 0x2016),
        digr('=', '2', 0x2017),
        digr('\'', '6', 0x2018),
        digr('\'', '9', 0x2019),
        digr('.', '9', 0x201a),
        digr('9', '\'', 0x201b),
        digr('"', '6', 0x201c),
        digr('"', '9', 0x201d),
        digr(':', '9', 0x201e),
        digr('9', '"', 0x201f),
        digr('/', '-', 0x2020),
        digr('/', '=', 0x2021),
        digr('.', '.', 0x2025),
        digr('%', '0', 0x2030),
        digr('1', '\'', 0x2032),
        digr('2', '\'', 0x2033),
        digr('3', '\'', 0x2034),
        digr('1', '"', 0x2035),
        digr('2', '"', 0x2036),
        digr('3', '"', 0x2037),
        digr('C', 'a', 0x2038),
        digr('<', '1', 0x2039),
        digr('>', '1', 0x203a),
        digr(':', 'X', 0x203b),
        digr('\'', '-', 0x203e),
        digr('/', 'f', 0x2044),
        digr('0', 'S', 0x2070),
        digr('4', 'S', 0x2074),
        digr('5', 'S', 0x2075),
        digr('6', 'S', 0x2076),
        digr('7', 'S', 0x2077),
        digr('8', 'S', 0x2078),
        digr('9', 'S', 0x2079),
        digr('+', 'S', 0x207a),
        digr('-', 'S', 0x207b),
        digr('=', 'S', 0x207c),
        digr('(', 'S', 0x207d),
        digr(')', 'S', 0x207e),
        digr('n', 'S', 0x207f),
        digr('0', 's', 0x2080),
        digr('1', 's', 0x2081),
        digr('2', 's', 0x2082),
        digr('3', 's', 0x2083),
        digr('4', 's', 0x2084),
        digr('5', 's', 0x2085),
        digr('6', 's', 0x2086),
        digr('7', 's', 0x2087),
        digr('8', 's', 0x2088),
        digr('9', 's', 0x2089),
        digr('+', 's', 0x208a),
        digr('-', 's', 0x208b),
        digr('=', 's', 0x208c),
        digr('(', 's', 0x208d),
        digr(')', 's', 0x208e),
        digr('L', 'i', 0x20a4),
        digr('P', 't', 0x20a7),
        digr('W', '=', 0x20a9),
        digr('=', 'e', 0x20ac), /* euro */
        digr('E', 'u', 0x20ac), /* euro */
        digr('=', 'R', 0x20bd), /* rouble */
        digr('=', 'P', 0x20bd), /* rouble */
        digr('o', 'C', 0x2103),
        digr('c', 'o', 0x2105),
        digr('o', 'F', 0x2109),
        digr('N', '0', 0x2116),
        digr('P', 'O', 0x2117),
        digr('R', 'x', 0x211e),
        digr('S', 'M', 0x2120),
        digr('T', 'M', 0x2122),
        digr('O', 'm', 0x2126),
        digr('A', 'O', 0x212b),
        digr('1', '3', 0x2153),
        digr('2', '3', 0x2154),
        digr('1', '5', 0x2155),
        digr('2', '5', 0x2156),
        digr('3', '5', 0x2157),
        digr('4', '5', 0x2158),
        digr('1', '6', 0x2159),
        digr('5', '6', 0x215a),
        digr('1', '8', 0x215b),
        digr('3', '8', 0x215c),
        digr('5', '8', 0x215d),
        digr('7', '8', 0x215e),
        digr('1', 'R', 0x2160),
        digr('2', 'R', 0x2161),
        digr('3', 'R', 0x2162),
        digr('4', 'R', 0x2163),
        digr('5', 'R', 0x2164),
        digr('6', 'R', 0x2165),
        digr('7', 'R', 0x2166),
        digr('8', 'R', 0x2167),
        digr('9', 'R', 0x2168),
        digr('a', 'R', 0x2169),
        digr('b', 'R', 0x216a),
        digr('c', 'R', 0x216b),
        digr('1', 'r', 0x2170),
        digr('2', 'r', 0x2171),
        digr('3', 'r', 0x2172),
        digr('4', 'r', 0x2173),
        digr('5', 'r', 0x2174),
        digr('6', 'r', 0x2175),
        digr('7', 'r', 0x2176),
        digr('8', 'r', 0x2177),
        digr('9', 'r', 0x2178),
        digr('a', 'r', 0x2179),
        digr('b', 'r', 0x217a),
        digr('c', 'r', 0x217b),
        digr('<', '-', 0x2190),
        digr('-', '!', 0x2191),
        digr('-', '>', 0x2192),
        digr('-', 'v', 0x2193),
        digr('<', '>', 0x2194),
        digr('U', 'D', 0x2195),
        digr('<', '=', 0x21d0),
        digr('=', '>', 0x21d2),
        digr('=', '=', 0x21d4),
        digr('F', 'A', 0x2200),
        digr('d', 'P', 0x2202),
        digr('T', 'E', 0x2203),
        digr('/', '0', 0x2205),
        digr('D', 'E', 0x2206),
        digr('N', 'B', 0x2207),
        digr('(', '-', 0x2208),
        digr('-', ')', 0x220b),
        digr('*', 'P', 0x220f),
        digr('+', 'Z', 0x2211),
        digr('-', '2', 0x2212),
        digr('-', '+', 0x2213),
        digr('*', '-', 0x2217),
        digr('O', 'b', 0x2218),
        digr('S', 'b', 0x2219),
        digr('R', 'T', 0x221a),
        digr('0', '(', 0x221d),
        digr('0', '0', 0x221e),
        digr('-', 'L', 0x221f),
        digr('-', 'V', 0x2220),
        digr('P', 'P', 0x2225),
        digr('A', 'N', 0x2227),
        digr('O', 'R', 0x2228),
        digr('(', 'U', 0x2229),
        digr(')', 'U', 0x222a),
        digr('I', 'n', 0x222b),
        digr('D', 'I', 0x222c),
        digr('I', 'o', 0x222e),
        digr('.', ':', 0x2234),
        digr(':', '.', 0x2235),
        digr(':', 'R', 0x2236),
        digr(':', ':', 0x2237),
        digr('?', '1', 0x223c),
        digr('C', 'G', 0x223e),
        digr('?', '-', 0x2243),
        digr('?', '=', 0x2245),
        digr('?', '2', 0x2248),
        digr('=', '?', 0x224c),
        digr('H', 'I', 0x2253),
        digr('!', '=', 0x2260),
        digr('=', '3', 0x2261),
        digr('=', '<', 0x2264),
        digr('>', '=', 0x2265),
        digr('<', '*', 0x226a),
        digr('*', '>', 0x226b),
        digr('!', '<', 0x226e),
        digr('!', '>', 0x226f),
        digr('(', 'C', 0x2282),
        digr(')', 'C', 0x2283),
        digr('(', '_', 0x2286),
        digr(')', '_', 0x2287),
        digr('0', '.', 0x2299),
        digr('0', '2', 0x229a),
        digr('-', 'T', 0x22a5),
        digr('.', 'P', 0x22c5),
        digr(':', '3', 0x22ee),
        digr('.', '3', 0x22ef),
        digr('E', 'h', 0x2302),
        digr('<', '7', 0x2308),
        digr('>', '7', 0x2309),
        digr('7', '<', 0x230a),
        digr('7', '>', 0x230b),
        digr('N', 'I', 0x2310),
        digr('(', 'A', 0x2312),
        digr('T', 'R', 0x2315),
        digr('I', 'u', 0x2320),
        digr('I', 'l', 0x2321),
        digr('<', '/', 0x2329),
        digr('/', '>', 0x232a),
        digr('V', 's', 0x2423),
        digr('1', 'h', 0x2440),
        digr('3', 'h', 0x2441),
        digr('2', 'h', 0x2442),
        digr('4', 'h', 0x2443),
        digr('1', 'j', 0x2446),
        digr('2', 'j', 0x2447),
        digr('3', 'j', 0x2448),
        digr('4', 'j', 0x2449),
        digr('1', '.', 0x2488),
        digr('2', '.', 0x2489),
        digr('3', '.', 0x248a),
        digr('4', '.', 0x248b),
        digr('5', '.', 0x248c),
        digr('6', '.', 0x248d),
        digr('7', '.', 0x248e),
        digr('8', '.', 0x248f),
        digr('9', '.', 0x2490),
        digr('h', 'h', 0x2500),
        digr('H', 'H', 0x2501),
        digr('v', 'v', 0x2502),
        digr('V', 'V', 0x2503),
        digr('3', '-', 0x2504),
        digr('3', '_', 0x2505),
        digr('3', '!', 0x2506),
        digr('3', '/', 0x2507),
        digr('4', '-', 0x2508),
        digr('4', '_', 0x2509),
        digr('4', '!', 0x250a),
        digr('4', '/', 0x250b),
        digr('d', 'r', 0x250c),
        digr('d', 'R', 0x250d),
        digr('D', 'r', 0x250e),
        digr('D', 'R', 0x250f),
        digr('d', 'l', 0x2510),
        digr('d', 'L', 0x2511),
        digr('D', 'l', 0x2512),
        digr('L', 'D', 0x2513),
        digr('u', 'r', 0x2514),
        digr('u', 'R', 0x2515),
        digr('U', 'r', 0x2516),
        digr('U', 'R', 0x2517),
        digr('u', 'l', 0x2518),
        digr('u', 'L', 0x2519),
        digr('U', 'l', 0x251a),
        digr('U', 'L', 0x251b),
        digr('v', 'r', 0x251c),
        digr('v', 'R', 0x251d),
        digr('V', 'r', 0x2520),
        digr('V', 'R', 0x2523),
        digr('v', 'l', 0x2524),
        digr('v', 'L', 0x2525),
        digr('V', 'l', 0x2528),
        digr('V', 'L', 0x252b),
        digr('d', 'h', 0x252c),
        digr('d', 'H', 0x252f),
        digr('D', 'h', 0x2530),
        digr('D', 'H', 0x2533),
        digr('u', 'h', 0x2534),
        digr('u', 'H', 0x2537),
        digr('U', 'h', 0x2538),
        digr('U', 'H', 0x253b),
        digr('v', 'h', 0x253c),
        digr('v', 'H', 0x253f),
        digr('V', 'h', 0x2542),
        digr('V', 'H', 0x254b),
        digr('F', 'D', 0x2571),
        digr('B', 'D', 0x2572),
        digr('T', 'B', 0x2580),
        digr('L', 'B', 0x2584),
        digr('F', 'B', 0x2588),
        digr('l', 'B', 0x258c),
        digr('R', 'B', 0x2590),
        digr('.', 'S', 0x2591),
        digr(':', 'S', 0x2592),
        digr('?', 'S', 0x2593),
        digr('f', 'S', 0x25a0),
        digr('O', 'S', 0x25a1),
        digr('R', 'O', 0x25a2),
        digr('R', 'r', 0x25a3),
        digr('R', 'F', 0x25a4),
        digr('R', 'Y', 0x25a5),
        digr('R', 'H', 0x25a6),
        digr('R', 'Z', 0x25a7),
        digr('R', 'K', 0x25a8),
        digr('R', 'X', 0x25a9),
        digr('s', 'B', 0x25aa),
        digr('S', 'R', 0x25ac),
        digr('O', 'r', 0x25ad),
        digr('U', 'T', 0x25b2),
        digr('u', 'T', 0x25b3),
        digr('P', 'R', 0x25b6),
        digr('T', 'r', 0x25b7),
        digr('D', 't', 0x25bc),
        digr('d', 'T', 0x25bd),
        digr('P', 'L', 0x25c0),
        digr('T', 'l', 0x25c1),
        digr('D', 'b', 0x25c6),
        digr('D', 'w', 0x25c7),
        digr('L', 'Z', 0x25ca),
        digr('0', 'm', 0x25cb),
        digr('0', 'o', 0x25ce),
        digr('0', 'M', 0x25cf),
        digr('0', 'L', 0x25d0),
        digr('0', 'R', 0x25d1),
        digr('S', 'n', 0x25d8),
        digr('I', 'c', 0x25d9),
        digr('F', 'd', 0x25e2),
        digr('B', 'd', 0x25e3),
        digr('*', '2', 0x2605),
        digr('*', '1', 0x2606),
        digr('<', 'H', 0x261c),
        digr('>', 'H', 0x261e),
        digr('0', 'u', 0x263a),
        digr('0', 'U', 0x263b),
        digr('S', 'U', 0x263c),
        digr('F', 'm', 0x2640),
        digr('M', 'l', 0x2642),
        digr('c', 'S', 0x2660),
        digr('c', 'H', 0x2661),
        digr('c', 'D', 0x2662),
        digr('c', 'C', 0x2663),
        digr('M', 'd', 0x2669),
        digr('M', '8', 0x266a),
        digr('M', '2', 0x266b),
        digr('M', 'b', 0x266d),
        digr('M', 'x', 0x266e),
        digr('M', 'X', 0x266f),
        digr('O', 'K', 0x2713),
        digr('X', 'X', 0x2717),
        digr('-', 'X', 0x2720),
        digr('I', 'S', 0x3000),
        digr(',', '_', 0x3001),
        digr('.', '_', 0x3002),
        digr('+', '"', 0x3003),
        digr('+', '_', 0x3004),
        digr('*', '_', 0x3005),
        digr(';', '_', 0x3006),
        digr('0', '_', 0x3007),
        digr('<', '+', 0x300a),
        digr('>', '+', 0x300b),
        digr('<', '\'', 0x300c),
        digr('>', '\'', 0x300d),
        digr('<', '"', 0x300e),
        digr('>', '"', 0x300f),
        digr('(', '"', 0x3010),
        digr(')', '"', 0x3011),
        digr('=', 'T', 0x3012),
        digr('=', '_', 0x3013),
        digr('(', '\'', 0x3014),
        digr(')', '\'', 0x3015),
        digr('(', 'I', 0x3016),
        digr(')', 'I', 0x3017),
        digr('-', '?', 0x301c),
        digr('A', '5', 0x3041),
        digr('a', '5', 0x3042),
        digr('I', '5', 0x3043),
        digr('i', '5', 0x3044),
        digr('U', '5', 0x3045),
        digr('u', '5', 0x3046),
        digr('E', '5', 0x3047),
        digr('e', '5', 0x3048),
        digr('O', '5', 0x3049),
        digr('o', '5', 0x304a),
        digr('k', 'a', 0x304b),
        digr('g', 'a', 0x304c),
        digr('k', 'i', 0x304d),
        digr('g', 'i', 0x304e),
        digr('k', 'u', 0x304f),
        digr('g', 'u', 0x3050),
        digr('k', 'e', 0x3051),
        digr('g', 'e', 0x3052),
        digr('k', 'o', 0x3053),
        digr('g', 'o', 0x3054),
        digr('s', 'a', 0x3055),
        digr('z', 'a', 0x3056),
        digr('s', 'i', 0x3057),
        digr('z', 'i', 0x3058),
        digr('s', 'u', 0x3059),
        digr('z', 'u', 0x305a),
        digr('s', 'e', 0x305b),
        digr('z', 'e', 0x305c),
        digr('s', 'o', 0x305d),
        digr('z', 'o', 0x305e),
        digr('t', 'a', 0x305f),
        digr('d', 'a', 0x3060),
        digr('t', 'i', 0x3061),
        digr('d', 'i', 0x3062),
        digr('t', 'U', 0x3063),
        digr('t', 'u', 0x3064),
        digr('d', 'u', 0x3065),
        digr('t', 'e', 0x3066),
        digr('d', 'e', 0x3067),
        digr('t', 'o', 0x3068),
        digr('d', 'o', 0x3069),
        digr('n', 'a', 0x306a),
        digr('n', 'i', 0x306b),
        digr('n', 'u', 0x306c),
        digr('n', 'e', 0x306d),
        digr('n', 'o', 0x306e),
        digr('h', 'a', 0x306f),
        digr('b', 'a', 0x3070),
        digr('p', 'a', 0x3071),
        digr('h', 'i', 0x3072),
        digr('b', 'i', 0x3073),
        digr('p', 'i', 0x3074),
        digr('h', 'u', 0x3075),
        digr('b', 'u', 0x3076),
        digr('p', 'u', 0x3077),
        digr('h', 'e', 0x3078),
        digr('b', 'e', 0x3079),
        digr('p', 'e', 0x307a),
        digr('h', 'o', 0x307b),
        digr('b', 'o', 0x307c),
        digr('p', 'o', 0x307d),
        digr('m', 'a', 0x307e),
        digr('m', 'i', 0x307f),
        digr('m', 'u', 0x3080),
        digr('m', 'e', 0x3081),
        digr('m', 'o', 0x3082),
        digr('y', 'A', 0x3083),
        digr('y', 'a', 0x3084),
        digr('y', 'U', 0x3085),
        digr('y', 'u', 0x3086),
        digr('y', 'O', 0x3087),
        digr('y', 'o', 0x3088),
        digr('r', 'a', 0x3089),
        digr('r', 'i', 0x308a),
        digr('r', 'u', 0x308b),
        digr('r', 'e', 0x308c),
        digr('r', 'o', 0x308d),
        digr('w', 'A', 0x308e),
        digr('w', 'a', 0x308f),
        digr('w', 'i', 0x3090),
        digr('w', 'e', 0x3091),
        digr('w', 'o', 0x3092),
        digr('n', '5', 0x3093),
        digr('v', 'u', 0x3094),
        digr('"', '5', 0x309b),
        digr('0', '5', 0x309c),
        digr('*', '5', 0x309d),
        digr('+', '5', 0x309e),
        digr('a', '6', 0x30a1),
        digr('A', '6', 0x30a2),
        digr('i', '6', 0x30a3),
        digr('I', '6', 0x30a4),
        digr('u', '6', 0x30a5),
        digr('U', '6', 0x30a6),
        digr('e', '6', 0x30a7),
        digr('E', '6', 0x30a8),
        digr('o', '6', 0x30a9),
        digr('O', '6', 0x30aa),
        digr('K', 'a', 0x30ab),
        digr('G', 'a', 0x30ac),
        digr('K', 'i', 0x30ad),
        digr('G', 'i', 0x30ae),
        digr('K', 'u', 0x30af),
        digr('G', 'u', 0x30b0),
        digr('K', 'e', 0x30b1),
        digr('G', 'e', 0x30b2),
        digr('K', 'o', 0x30b3),
        digr('G', 'o', 0x30b4),
        digr('S', 'a', 0x30b5),
        digr('Z', 'a', 0x30b6),
        digr('S', 'i', 0x30b7),
        digr('Z', 'i', 0x30b8),
        digr('S', 'u', 0x30b9),
        digr('Z', 'u', 0x30ba),
        digr('S', 'e', 0x30bb),
        digr('Z', 'e', 0x30bc),
        digr('S', 'o', 0x30bd),
        digr('Z', 'o', 0x30be),
        digr('T', 'a', 0x30bf),
        digr('D', 'a', 0x30c0),
        digr('T', 'i', 0x30c1),
        digr('D', 'i', 0x30c2),
        digr('T', 'U', 0x30c3),
        digr('T', 'u', 0x30c4),
        digr('D', 'u', 0x30c5),
        digr('T', 'e', 0x30c6),
        digr('D', 'e', 0x30c7),
        digr('T', 'o', 0x30c8),
        digr('D', 'o', 0x30c9),
        digr('N', 'a', 0x30ca),
        digr('N', 'i', 0x30cb),
        digr('N', 'u', 0x30cc),
        digr('N', 'e', 0x30cd),
        digr('N', 'o', 0x30ce),
        digr('H', 'a', 0x30cf),
        digr('B', 'a', 0x30d0),
        digr('P', 'a', 0x30d1),
        digr('H', 'i', 0x30d2),
        digr('B', 'i', 0x30d3),
        digr('P', 'i', 0x30d4),
        digr('H', 'u', 0x30d5),
        digr('B', 'u', 0x30d6),
        digr('P', 'u', 0x30d7),
        digr('H', 'e', 0x30d8),
        digr('B', 'e', 0x30d9),
        digr('P', 'e', 0x30da),
        digr('H', 'o', 0x30db),
        digr('B', 'o', 0x30dc),
        digr('P', 'o', 0x30dd),
        digr('M', 'a', 0x30de),
        digr('M', 'i', 0x30df),
        digr('M', 'u', 0x30e0),
        digr('M', 'e', 0x30e1),
        digr('M', 'o', 0x30e2),
        digr('Y', 'A', 0x30e3),
        digr('Y', 'a', 0x30e4),
        digr('Y', 'U', 0x30e5),
        digr('Y', 'u', 0x30e6),
        digr('Y', 'O', 0x30e7),
        digr('Y', 'o', 0x30e8),
        digr('R', 'a', 0x30e9),
        digr('R', 'i', 0x30ea),
        digr('R', 'u', 0x30eb),
        digr('R', 'e', 0x30ec),
        digr('R', 'o', 0x30ed),
        digr('W', 'A', 0x30ee),
        digr('W', 'a', 0x30ef),
        digr('W', 'i', 0x30f0),
        digr('W', 'e', 0x30f1),
        digr('W', 'o', 0x30f2),
        digr('N', '6', 0x30f3),
        digr('V', 'u', 0x30f4),
        digr('K', 'A', 0x30f5),
        digr('K', 'E', 0x30f6),
        digr('V', 'a', 0x30f7),
        digr('V', 'i', 0x30f8),
        digr('V', 'e', 0x30f9),
        digr('V', 'o', 0x30fa),
        digr('.', '6', 0x30fb),
        digr('-', '6', 0x30fc),
        digr('*', '6', 0x30fd),
        digr('+', '6', 0x30fe),
        digr('b', '4', 0x3105),
        digr('p', '4', 0x3106),
        digr('m', '4', 0x3107),
        digr('f', '4', 0x3108),
        digr('d', '4', 0x3109),
        digr('t', '4', 0x310a),
        digr('n', '4', 0x310b),
        digr('l', '4', 0x310c),
        digr('g', '4', 0x310d),
        digr('k', '4', 0x310e),
        digr('h', '4', 0x310f),
        digr('j', '4', 0x3110),
        digr('q', '4', 0x3111),
        digr('x', '4', 0x3112),
        digr('z', 'h', 0x3113),
        digr('c', 'h', 0x3114),
        digr('s', 'h', 0x3115),
        digr('r', '4', 0x3116),
        digr('z', '4', 0x3117),
        digr('c', '4', 0x3118),
        digr('s', '4', 0x3119),
        digr('a', '4', 0x311a),
        digr('o', '4', 0x311b),
        digr('e', '4', 0x311c),
        digr('a', 'i', 0x311e),
        digr('e', 'i', 0x311f),
        digr('a', 'u', 0x3120),
        digr('o', 'u', 0x3121),
        digr('a', 'n', 0x3122),
        digr('e', 'n', 0x3123),
        digr('a', 'N', 0x3124),
        digr('e', 'N', 0x3125),
        digr('e', 'r', 0x3126),
        digr('i', '4', 0x3127),
        digr('u', '4', 0x3128),
        digr('i', 'u', 0x3129),
        digr('v', '4', 0x312a),
        digr('n', 'G', 0x312b),
        digr('g', 'n', 0x312c),
        digr('1', 'c', 0x3220),
        digr('2', 'c', 0x3221),
        digr('3', 'c', 0x3222),
        digr('4', 'c', 0x3223),
        digr('5', 'c', 0x3224),
        digr('6', 'c', 0x3225),
        digr('7', 'c', 0x3226),
        digr('8', 'c', 0x3227),
        digr('9', 'c', 0x3228),
        /* Code points 0xe000 - 0xefff excluded;
         * they have no assigned characters, only used in proposals. */
        digr('f', 'f', 0xfb00),
        digr('f', 'i', 0xfb01),
        digr('f', 'l', 0xfb02),
        digr('f', 't', 0xfb05),
        digr('s', 't', 0xfb06),

        /* Vim 5.x compatible digraphs that don't conflict with the above. */
        digr('~', '!', 161),
        digr('c', '|', 162),
        digr('$', '$', 163),
        digr('o', 'x', 164),
        digr('Y', '-', 165),
        digr('|', '|', 166),
        digr('c', 'O', 169),
        digr('-', ',', 172),
        digr('-', '=', 175),
        digr('~', 'o', 176),
        digr('2', '2', 178),
        digr('3', '3', 179),
        digr('p', 'p', 182),
        digr('~', '.', 183),
        digr('1', '1', 185),
        digr('~', '?', 191),
        digr('A', '`', 192),
        digr('A', '^', 194),
        digr('A', '~', 195),
        digr('A', '"', 196),
        digr('A', '@', 197),
        digr('E', '`', 200),
        digr('E', '^', 202),
        digr('E', '"', 203),
        digr('I', '`', 204),
        digr('I', '^', 206),
        digr('I', '"', 207),
        digr('N', '~', 209),
        digr('O', '`', 210),
        digr('O', '^', 212),
        digr('O', '~', 213),
        digr('/', '\\', 215),
        digr('U', '`', 217),
        digr('U', '^', 219),
        digr('I', 'p', 222),
        digr('a', '`', 224),
        digr('a', '^', 226),
        digr('a', '~', 227),
        digr('a', '"', 228),
        digr('a', '@', 229),
        digr('e', '`', 232),
        digr('e', '^', 234),
        digr('e', '"', 235),
        digr('i', '`', 236),
        digr('i', '^', 238),
        digr('n', '~', 241),
        digr('o', '`', 242),
        digr('o', '^', 244),
        digr('o', '~', 245),
        digr('u', '`', 249),
        digr('u', '^', 251),
        digr('y', '"', 255)
    };

    /*private*/ static int backspaced;  /* character before K_BS */
    /*private*/ static int lastchar;    /* last typed character */

    /*
     * handle digraphs after typing a character
     */
    /*private*/ static int do_digraph(int c)
    {
        if (c == -1)                /* init values */
        {
            backspaced = -1;
        }
        else if (p_dg[0])
        {
            if (0 <= backspaced)
                c = getdigraph(backspaced, c, false);
            backspaced = -1;
            if ((c == K_BS || c == Ctrl_H) && 0 <= lastchar)
                backspaced = lastchar;
        }
        lastchar = c;
        return c;
    }

    /*
     * Get a digraph.  Used after typing CTRL-K on the command line or in normal mode.
     * Returns composed character, or NUL when ESC was used.
     */
    /*private*/ static int get_digraph(boolean cmdline)
        /* cmdline: true when called from the cmdline */
    {
        int c, cc;

        no_mapping++;
        allow_keys++;
        c = plain_vgetc();
        --no_mapping;
        --allow_keys;
        if (c != ESC)               /* ESC cancels CTRL-K */
        {
            if (is_special(c))      /* insert special key code */
                return c;
            if (cmdline)
            {
                if (mb_char2cells(c) == 1 && cmdline_star == 0)
                    putcmdline(c, true);
            }
            else
                add_to_showcmd(c);
            no_mapping++;
            allow_keys++;
            cc = plain_vgetc();
            --no_mapping;
            --allow_keys;
            if (cc != ESC)      /* ESC cancels CTRL-K */
                return getdigraph(c, cc, true);
        }
        return NUL;
    }

    /*
     * Lookup the pair "char1", "char2" in the digraph tables.
     * If no match, return "char2".
     * If "meta_char" is true and "char1" is a space, return "char2" | 0x80.
     */
    /*private*/ static int getexactdigraph(int char1, int char2, boolean meta_char)
    {
        int retval = 0;

        if (is_special(char1) || is_special(char2))
            return char2;

        /*
         * Search user digraphs first.
         */
        for (int i = 0; i < user_digraphs.ga_len; i++)
        {
            digr_C dp = user_digraphs.ga_data[i];
            if ((int)dp.char1 == char1 && (int)dp.char2 == char2)
            {
                retval = dp.result;
                break;
            }
        }

        /*
         * Search default digraphs.
         */
        if (retval == 0)
        {
            digr_C[] dgs = digraphdefault;
            for (int i = 0; i < dgs.length; i++)
                if ((int)dgs[i].char1 == char1 && (int)dgs[i].char2 == char2)
                {
                    retval = dgs[i].result;
                    break;
                }
        }

        if (retval == 0)            /* digraph deleted or not found */
        {
            if (char1 == ' ' && meta_char)  /* <space> <char> --> meta-char */
                return (char2 | 0x80);

            return char2;
        }

        return retval;
    }

    /*
     * Get digraph.
     * Allow for both char1-char2 and char2-char1
     */
    /*private*/ static int getdigraph(int char1, int char2, boolean meta_char)
    {
        int retval;

        if (((retval = getexactdigraph(char1, char2, meta_char)) == char2)
                && (char1 != char2)
                && ((retval = getexactdigraph(char2, char1, meta_char)) == char1))
            return char2;

        return retval;
    }

    /*
     * Add the digraphs in the argument to the digraph table.
     * Format: {c1}{c2} char {c1}{c2} char ...
     */
    /*private*/ static void putdigraph(Bytes s)
    {
        while (s.at(0) != NUL)
        {
            s = skipwhite(s);
            if (s.at(0) == NUL)
                return;
            byte char1 = (s = s.plus(1)).at(-1);
            byte char2 = (s = s.plus(1)).at(-1);
            if (char2 == NUL)
            {
                emsg(e_invarg);
                return;
            }
            if (char1 == ESC || char2 == ESC)
            {
                emsg(u8("E104: Escape not allowed in digraph"));
                return;
            }
            s = skipwhite(s);
            if (!asc_isdigit(s.at(0)))
            {
                emsg(e_number_exp);
                return;
            }

            int n;
            { Bytes[] __ = { s }; n = (int)getdigits(__); s = __[0]; }
            int i;

            /* If the digraph already exists, replace the result. */
            for (i = 0; i < user_digraphs.ga_len; i++)
            {
                digr_C dp = user_digraphs.ga_data[i];
                if (dp.char1 == char1 && dp.char2 == char2)
                {
                    dp.result = n;
                    break;
                }
            }

            /* Add a new digraph to the table. */
            if (i == user_digraphs.ga_len)
            {
                user_digraphs.ga_grow(1);

                digr_C dp = user_digraphs.ga_data[user_digraphs.ga_len++] = new digr_C();
                dp.char1 = char1;
                dp.char2 = char2;
                dp.result = n;
            }
        }
    }

    /*private*/ static void listdigraphs()
    {
        msg_putchar('\n');

        digr_C[] dgs = digraphdefault;
        for (int i = 0; i < dgs.length && !got_int; i++)
        {
            digr_C tmp = new digr_C();

            /* May need to convert the result to 'encoding'. */
            tmp.char1 = dgs[i].char1;
            tmp.char2 = dgs[i].char2;
            tmp.result = getexactdigraph(tmp.char1, tmp.char2, false);
            if (tmp.result != 0 && tmp.result != tmp.char2)
                printdigraph(tmp);
            ui_breakcheck();
        }

        for (int i = 0; i < user_digraphs.ga_len && !got_int; i++)
        {
            digr_C dp = user_digraphs.ga_data[i];
            printdigraph(dp);
            ui_breakcheck();
        }

        must_redraw = CLEAR;    /* clear screen, because some digraphs may be wrong,
                                 * in which case we messed up "screenLines" */
    }

    /*private*/ static void printdigraph(digr_C dp)
    {
        int list_width = 13;

        if (dp.result != 0)
        {
            if ((int)Columns[0] - list_width < msg_col)
                msg_putchar('\n');
            if (msg_col != 0)
                while (msg_col % list_width != 0)
                    msg_putchar(' ');

            Bytes buf = new Bytes(30);

            Bytes p = buf;
            (p = p.plus(1)).be(-1, dp.char1);
            (p = p.plus(1)).be(-1, dp.char2);
            (p = p.plus(1)).be(-1, (byte)' ');

            /* add a space to draw a composing char on */
            if (utf_iscomposing(dp.result))
                (p = p.plus(1)).be(-1, (byte)' ');
            p = p.plus(utf_char2bytes(dp.result, p));

            if (mb_char2cells(dp.result) == 1)
                (p = p.plus(1)).be(-1, (byte)' ');
            vim_snprintf(p, buf.size() - BDIFF(p, buf), u8(" %3d"), dp.result);
            msg_outtrans(buf);
        }
    }

    /*
     * mbyte.c: Code specifically for handling multi-byte characters.
     *
     * "enc_utf8"   Use Unicode characters in UTF-8 encoding.
     *              The cell width on the display needs to be determined from the character value.
     *              Recognizing bytes is easy: 0xxx.xxxx is a single-byte char, 10xx.xxxx is a
     *              trailing byte, 11xx.xxxx is a leading byte of a multi-byte character.
     *              To make things complicated, up to six composing characters are allowed.
     *              These are drawn on top of the first char.
     *              For most editing the sequence of bytes with composing
     *              characters included is considered to be one character.
     *
     * 'encoding' specifies the encoding used in the core.  This is in registers,
     * text manipulation, buffers, etc.  Conversion has to be done when characters
     * in another encoding are received or send:
     *
     *                     clipboard
     *                         ^
     *                         | (2)
     *                         V
     *                 +---------------+
     *            (1)  |               | (3)
     *  keyboard ----->|     core      |-----> display
     *                 |               |
     *                 +---------------+
     *                         ^
     *                         | (4)
     *                         V
     *                       file
     *
     * (1) Typed characters arrive in the current locale.  Conversion is to be
     *     done when 'encoding' is different from 'termencoding'.
     * (2) Text will be made available with the encoding specified with
     *     'encoding'.  If this is not sufficient, system-specific conversion
     *     might be required.
     * (3) For the GUI the correct font must be selected, no conversion done.
     *     Otherwise, conversion is to be done when 'encoding' differs from
     *     'termencoding'.
     * (4) The encoding of the file is specified with 'fileencoding'.  Conversion
     *     is to be done when it's different from 'encoding'.
     */

    /*
     * Lookup table to quickly get the length in bytes of a UTF-8 sequence from the first byte.
     * Bytes which are illegal when used as the first byte have a 0.
     * The NUL byte has length 1.
     */
    /*private*/ static final byte[/*256*/] utf8len_tab_zero =
    {
        1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
        1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
        1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
        1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
        2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,2,
        3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,3,4,4,4,4,4,4,4,4,5,5,5,5,6,6,0,0
    };

    /*
     * Returns the length of a UTF-8 sequence, obtained from the first byte.
     * For an invalid first byte returns zero ? 0 : 1.
     */
    /*private*/ static int us_byte2len(byte b, boolean zero)
    {
        int len = utf8len_tab_zero[char_u(b)];
        return (zero || 0 < len) ? len : 1;
    }

    /*
     * Return byte length of character that starts with byte "b".
     * Returns 1 for a single-byte character.
     * mb_byte2len() can be used to count a special key as one byte.
     */
    /*private*/ static int mb_byte2len(int b)
    {
        return (b < 0 || 0xff < b) ? 1 : us_byte2len((byte)b, false);
    }

    /*
     * Return the size of the BOM for the current buffer:
     * 0 - no BOM
     * 2 - UCS-2 or UTF-16 BOM
     * 4 - UCS-4 BOM
     * 3 - UTF-8 BOM
     */
    /*private*/ static int bomb_size()
    {
        int n = 0;

        if (curbuf.b_p_bomb[0] && !curbuf.b_p_bin[0])
        {
            if (curbuf.b_p_fenc[0].at(0) == NUL)
                n = 3;
            else if (STRCMP(curbuf.b_p_fenc[0], u8("utf-8")) == 0)
                n = 3;
            else if (STRNCMP(curbuf.b_p_fenc[0], u8("ucs-2"), 5) == 0
                  || STRNCMP(curbuf.b_p_fenc[0], u8("utf-16"), 6) == 0)
                n = 2;
            else if (STRNCMP(curbuf.b_p_fenc[0], u8("ucs-4"), 5) == 0)
                n = 4;
        }

        return n;
    }

    /*
     * Get class of pointer:
     *  0 for blank or NUL
     *  1 for punctuation
     *  2 for an (ASCII) word character
     * >2 for other word characters
     */
    /*private*/ static int us_get_class(Bytes p, buffer_C buf)
    {
        if (us_byte2len(p.at(0), false) == 1)
        {
            if (p.at(0) == NUL || vim_iswhite(p.at(0)))
                return 0;
            if (us_iswordb(p.at(0), buf))
                return 2;

            return 1;
        }
        return utf_class(us_ptr2char(p));
    }

    /*private*/ static boolean intable(int[] table, int c)
    {
        /* first quick check for Latin1 etc. characters */
        if (c < table[0])
            return false;

        /* binary search in table */
        for (int bot = 0, top = table.length / 2 - 1; bot <= top; )
        {
            int mid = (bot + top) / 2;
            if (table[2 * mid + 1] < c)
                bot = mid + 1;
            else if (c < table[2 * mid])
                top = mid - 1;
            else
                return true;
        }

        return false;
    }

    /* Sorted list of non-overlapping intervals of East Asian double width characters,
     * generated with tools/unicode.vim. */
    /*private*/ static int[] doublewidth =
    {
        0x1100, 0x115f,
        0x2329, 0x232a,
        0x2e80, 0x2e99,
        0x2e9b, 0x2ef3,
        0x2f00, 0x2fd5,
        0x2ff0, 0x2ffb,
        0x3000, 0x303e,
        0x3041, 0x3096,
        0x3099, 0x30ff,
        0x3105, 0x312d,
        0x3131, 0x318e,
        0x3190, 0x31ba,
        0x31c0, 0x31e3,
        0x31f0, 0x321e,
        0x3220, 0x3247,
        0x3250, 0x32fe,
        0x3300, 0x4dbf,
        0x4e00, 0xa48c,
        0xa490, 0xa4c6,
        0xa960, 0xa97c,
        0xac00, 0xd7a3,
        0xf900, 0xfaff,
        0xfe10, 0xfe19,
        0xfe30, 0xfe52,
        0xfe54, 0xfe66,
        0xfe68, 0xfe6b,
        0xff01, 0xff60,
        0xffe0, 0xffe6,
        0x1b000, 0x1b001,
        0x1f200, 0x1f202,
        0x1f210, 0x1f23a,
        0x1f240, 0x1f248,
        0x1f250, 0x1f251,
        0x20000, 0x2fffd,
        0x30000, 0x3fffd
    };

    /* Sorted list of non-overlapping intervals of East Asian Ambiguous characters,
     * generated with tools/unicode.vim. */
    /*private*/ static int[] ambiguous =
    {
        0x00a1, 0x00a1,
        0x00a4, 0x00a4,
        0x00a7, 0x00a8,
        0x00aa, 0x00aa,
        0x00ad, 0x00ae,
        0x00b0, 0x00b4,
        0x00b6, 0x00ba,
        0x00bc, 0x00bf,
        0x00c6, 0x00c6,
        0x00d0, 0x00d0,
        0x00d7, 0x00d8,
        0x00de, 0x00e1,
        0x00e6, 0x00e6,
        0x00e8, 0x00ea,
        0x00ec, 0x00ed,
        0x00f0, 0x00f0,
        0x00f2, 0x00f3,
        0x00f7, 0x00fa,
        0x00fc, 0x00fc,
        0x00fe, 0x00fe,
        0x0101, 0x0101,
        0x0111, 0x0111,
        0x0113, 0x0113,
        0x011b, 0x011b,
        0x0126, 0x0127,
        0x012b, 0x012b,
        0x0131, 0x0133,
        0x0138, 0x0138,
        0x013f, 0x0142,
        0x0144, 0x0144,
        0x0148, 0x014b,
        0x014d, 0x014d,
        0x0152, 0x0153,
        0x0166, 0x0167,
        0x016b, 0x016b,
        0x01ce, 0x01ce,
        0x01d0, 0x01d0,
        0x01d2, 0x01d2,
        0x01d4, 0x01d4,
        0x01d6, 0x01d6,
        0x01d8, 0x01d8,
        0x01da, 0x01da,
        0x01dc, 0x01dc,
        0x0251, 0x0251,
        0x0261, 0x0261,
        0x02c4, 0x02c4,
        0x02c7, 0x02c7,
        0x02c9, 0x02cb,
        0x02cd, 0x02cd,
        0x02d0, 0x02d0,
        0x02d8, 0x02db,
        0x02dd, 0x02dd,
        0x02df, 0x02df,
        0x0300, 0x036f,
        0x0391, 0x03a1,
        0x03a3, 0x03a9,
        0x03b1, 0x03c1,
        0x03c3, 0x03c9,
        0x0401, 0x0401,
        0x0410, 0x044f,
        0x0451, 0x0451,
        0x2010, 0x2010,
        0x2013, 0x2016,
        0x2018, 0x2019,
        0x201c, 0x201d,
        0x2020, 0x2022,
        0x2024, 0x2027,
        0x2030, 0x2030,
        0x2032, 0x2033,
        0x2035, 0x2035,
        0x203b, 0x203b,
        0x203e, 0x203e,
        0x2074, 0x2074,
        0x207f, 0x207f,
        0x2081, 0x2084,
        0x20ac, 0x20ac,
        0x2103, 0x2103,
        0x2105, 0x2105,
        0x2109, 0x2109,
        0x2113, 0x2113,
        0x2116, 0x2116,
        0x2121, 0x2122,
        0x2126, 0x2126,
        0x212b, 0x212b,
        0x2153, 0x2154,
        0x215b, 0x215e,
        0x2160, 0x216b,
        0x2170, 0x2179,
        0x2189, 0x2189,
        0x2190, 0x2199,
        0x21b8, 0x21b9,
        0x21d2, 0x21d2,
        0x21d4, 0x21d4,
        0x21e7, 0x21e7,
        0x2200, 0x2200,
        0x2202, 0x2203,
        0x2207, 0x2208,
        0x220b, 0x220b,
        0x220f, 0x220f,
        0x2211, 0x2211,
        0x2215, 0x2215,
        0x221a, 0x221a,
        0x221d, 0x2220,
        0x2223, 0x2223,
        0x2225, 0x2225,
        0x2227, 0x222c,
        0x222e, 0x222e,
        0x2234, 0x2237,
        0x223c, 0x223d,
        0x2248, 0x2248,
        0x224c, 0x224c,
        0x2252, 0x2252,
        0x2260, 0x2261,
        0x2264, 0x2267,
        0x226a, 0x226b,
        0x226e, 0x226f,
        0x2282, 0x2283,
        0x2286, 0x2287,
        0x2295, 0x2295,
        0x2299, 0x2299,
        0x22a5, 0x22a5,
        0x22bf, 0x22bf,
        0x2312, 0x2312,
        0x2460, 0x24e9,
        0x24eb, 0x254b,
        0x2550, 0x2573,
        0x2580, 0x258f,
        0x2592, 0x2595,
        0x25a0, 0x25a1,
        0x25a3, 0x25a9,
        0x25b2, 0x25b3,
        0x25b6, 0x25b7,
        0x25bc, 0x25bd,
        0x25c0, 0x25c1,
        0x25c6, 0x25c8,
        0x25cb, 0x25cb,
        0x25ce, 0x25d1,
        0x25e2, 0x25e5,
        0x25ef, 0x25ef,
        0x2605, 0x2606,
        0x2609, 0x2609,
        0x260e, 0x260f,
        0x2614, 0x2615,
        0x261c, 0x261c,
        0x261e, 0x261e,
        0x2640, 0x2640,
        0x2642, 0x2642,
        0x2660, 0x2661,
        0x2663, 0x2665,
        0x2667, 0x266a,
        0x266c, 0x266d,
        0x266f, 0x266f,
        0x269e, 0x269f,
        0x26be, 0x26bf,
        0x26c4, 0x26cd,
        0x26cf, 0x26e1,
        0x26e3, 0x26e3,
        0x26e8, 0x26ff,
        0x273d, 0x273d,
        0x2757, 0x2757,
        0x2776, 0x277f,
        0x2b55, 0x2b59,
        0x3248, 0x324f,
        0xe000, 0xf8ff,
        0xfe00, 0xfe0f,
        0xfffd, 0xfffd,
        0x1f100, 0x1f10a,
        0x1f110, 0x1f12d,
        0x1f130, 0x1f169,
        0x1f170, 0x1f19a,
        0xe0100, 0xe01ef,
        0xf0000, 0xffffd,
        0x100000, 0x10fffd
    };

    /*
     * For UTF-8 character "c" return 2 for a double-width character, 1 for others.
     * Returns 4 or 6 for an unprintable character.
     * Is only correct for characters >= 0x80.
     * When "p_ambw" is 'double', return 2 for a character with East Asian Width class 'A'(mbiguous).
     */
    /*private*/ static int utf_char2cells(int c)
    {
        if (0x80 <= c)
        {
            /* Characters below 0x100 are influenced by 'isprint' option. */
            if (c < 0x100)
            {
                if (!vim_isprintc(c))
                    return 4;                           /* unprintable, displays <xx> */
            }
            else
            {
                if (!utf_printable(c))
                    return 6;                           /* unprintable, displays <xxxx> */
                if (intable(doublewidth, c))
                    return 2;
            }

            if (p_ambw[0].at(0) == (byte)'d' && intable(ambiguous, c))
                return 2;
        }

        return 1;
    }

    /*private*/ static int us_ptr2cells(Bytes p)
    {
        /* Need to convert to a wide character. */
        if (0x80 <= char_u(p.at(0)))
        {
            int c = us_ptr2char(p);
            /* An illegal byte is displayed as <xx>. */
            if (us_ptr2len(p) == 1 || c == NUL)
                return 4;
            /* If the char is ASCII it must be an overlong sequence. */
            if (c < 0x80)
                return mb_char2cells(c);

            return utf_char2cells(c);
        }
        return 1;
    }

    /*
     * Return the number of cells occupied by string "p".
     * Stop at a NUL character.  When "len" >= 0 stop at character "p[len]".
     */
    /*private*/ static int us_string2cells(Bytes p, int len)
    {
        int cells = 0;

        for (int i = 0; (len < 0 || i < len) && p.at(i) != NUL; i += us_ptr2len_cc(p.plus(i)))
            cells += us_ptr2cells(p.plus(i));

        return cells;
    }

    /*private*/ static int utf_off2cells(int off, int max_off)
    {
        return (off + 1 < max_off && screenLines.at(off + 1) == 0) ? 2 : 1;
    }

    /*
     * Convert a UTF-8 byte sequence to a wide character.
     * If the sequence is illegal or truncated by a NUL the first byte is returned.
     * Does not include composing characters, of course.
     */
    /*private*/ static int us_ptr2char(Bytes p)
    {
        if (char_u(p.at(0)) < 0x80)    /* be quick for ASCII */
            return p.at(0);

        int len = us_byte2len(p.at(0), true);
        if (1 < len && (char_u(p.at(1)) & 0xc0) == 0x80)
        {
            if (len == 2)
                return ((p.at(0) & 0x1f) << 6) + (p.at(1) & 0x3f);

            if ((char_u(p.at(2)) & 0xc0) == 0x80)
            {
                if (len == 3)
                    return ((p.at(0) & 0x0f) << 12) + ((p.at(1) & 0x3f) << 6) + (p.at(2) & 0x3f);

                if ((char_u(p.at(3)) & 0xc0) == 0x80)
                {
                    if (len == 4)
                        return ((p.at(0) & 0x07) << 18) + ((p.at(1) & 0x3f) << 12)
                             + ((p.at(2) & 0x3f) << 6) + (p.at(3) & 0x3f);

                    if ((char_u(p.at(4)) & 0xc0) == 0x80)
                    {
                        if (len == 5)
                            return ((p.at(0) & 0x03) << 24) + ((p.at(1) & 0x3f) << 18)
                                 + ((p.at(2) & 0x3f) << 12) + ((p.at(3) & 0x3f) << 6) + (p.at(4) & 0x3f);

                        if ((char_u(p.at(5)) & 0xc0) == 0x80 && len == 6)
                            return ((p.at(0) & 0x01) << 30) + ((p.at(1) & 0x3f) << 24)
                                 + ((p.at(2) & 0x3f) << 18) + ((p.at(3) & 0x3f) << 12)
                                 + ((p.at(4) & 0x3f) << 6) + (p.at(5) & 0x3f);
                    }
                }
            }
        }

        /* Illegal value, just return the first byte. */
        return char_u(p.at(0));
    }

    /*
     * Convert a UTF-8 byte sequence to a wide character.
     * String is assumed to be terminated by NUL or after "n" bytes, whichever comes first.
     * The function is safe in the sense that it never accesses memory beyond the first "n" bytes of "s".
     *
     * On success, returns decoded codepoint,
     * advances "s" to the beginning of next character and decreases "n" accordingly.
     *
     * If end of string was reached, returns 0 and, if "n" > 0, advances "s" past NUL byte.
     *
     * If byte sequence is illegal or incomplete, returns -1 and does not advance "s".
     */
    /*private*/ static int us_safe_read_char_adv(Bytes[] s, int[] n)
    {
        if (n[0] == 0)    /* end of buffer */
            return 0;

        int k = us_byte2len(s[0].at(0), true);

        if (k == 1)     /* ASCII character or NUL */
        {
            n[0]--;
            return (s[0] = s[0].plus(1)).at(-1);
        }

        if (k <= n[0])
        {
            /* We have a multibyte sequence and it isn't truncated by buffer
             * limits so us_ptr2char() is safe to use.  Or the first byte is
             * illegal (k=0), and it's also safe to use us_ptr2char(). */
            int c = us_ptr2char(s[0]);

            /* On failure, us_ptr2char() returns the first byte, so here we
             * check equality with the first byte.  The only non-ASCII character
             * which equals the first byte of its own UTF-8 representation is
             * U+00C3 (UTF-8: 0xC3 0x83), so need to check that special case too.
             * It's safe even if n=1, else we would have k=2 > n. */
            if (c != char_u(s[0].at(0)) || (c == 0xc3 && char_u(s[0].at(1)) == 0x83))
            {
                /* byte sequence was successfully decoded */
                s[0] = s[0].plus(k);
                n[0] -= k;
                return c;
            }
        }

        /* byte sequence is incomplete or illegal */
        return -1;
    }

    /*
     * Get character at **pp and advance *pp to the next character.
     * skip: composing characters are skipped!
     * !skip: composing characters are returned as separate characters.
     */
    /*private*/ static int us_ptr2char_adv(Bytes[] pp, boolean skip)
    {
        int c = us_ptr2char(pp[0]);
        pp[0] = pp[0].plus((skip) ? us_ptr2len_cc(pp[0]) : us_ptr2len(pp[0]));
        return c;
    }

    /*
     * Convert a UTF-8 byte string to a wide character.
     * Also get up to MAX_MCO composing characters.
     */
    /*private*/ static int us_ptr2char_cc(Bytes p, int[] pcc)
        /* pcc: return: composing chars, last one is 0 */
    {
        int j = 0;

        /* Only accept a composing char when the first char isn't illegal. */
        int i = us_ptr2len(p);
        if (1 < i || char_u(p.at(0)) < 0x80)
            for (int cc; 0x80 <= char_u(p.at(i)) && utf_iscomposing(cc = us_ptr2char(p.plus(i))); i += us_ptr2len(p.plus(i)))
            {
                pcc[j++] = cc;
                if (j == MAX_MCO)
                    break;
            }

        if (j < MAX_MCO)    /* last composing char must be 0 */
            pcc[j] = 0;

        return us_ptr2char(p);
    }

    /*
     * Convert a UTF-8 byte string to a wide character.
     * Also get up to MAX_MCO composing characters.
     * Use no more than p[maxlen].
     */
    /*private*/ static int us_ptr2char_cc_len(Bytes p, int[] pcc, int maxlen)
        /* pcc: return: composing chars, last one is 0 */
    {
        int j = 0;

        /* Only accept a composing char when the first char isn't illegal. */
        int i = us_ptr2len_len(p, maxlen);
        if (1 < i || char_u(p.at(0)) < 0x80)
            for (int cc; i < maxlen && 0x80 <= char_u(p.at(i)) && utf_iscomposing(cc = us_ptr2char(p.plus(i))); i += us_ptr2len_len(p.plus(i), maxlen - i))
            {
                pcc[j++] = cc;
                if (j == MAX_MCO)
                    break;
            }

        if (j < MAX_MCO)    /* last composing char must be 0 */
            pcc[j] = 0;

        return us_ptr2char(p);
    }

    /*
     * Convert the character at screen position "off" to a sequence of bytes.
     * Includes the composing characters.
     * "buf" must at least have the length MB_MAXBYTES + 1.
     * Only to be used when screenLinesUC[off] != 0.
     * Returns the produced number of bytes.
     */
    /*private*/ static int utfc_char2bytes(int off, Bytes buf)
    {
        int len = utf_char2bytes(screenLinesUC[off], buf);
        for (int i = 0; i < screen_mco; i++)
        {
            if (screenLinesC[i][off] == 0)
                break;
            len += utf_char2bytes(screenLinesC[i][off], buf.plus(len));
        }
        return len;
    }

    /*
     * Get the length of a UTF-8 byte sequence, not including any following composing characters.
     * Returns 0 for "".
     * Returns 1 for an illegal byte sequence.
     */
    /*private*/ static int us_ptr2len(Bytes p)
    {
        if (p.at(0) == NUL)
            return 0;

        int len = us_byte2len(p.at(0), false);
        for (int i = 1; i < len; i++)
            if ((char_u(p.at(i)) & 0xc0) != 0x80)
                return 1;

        return len;
    }

    /*
     * Get the length of UTF-8 byte sequence "p[size]".
     * Does not include any following composing characters.
     * Returns 1 for "".
     * Returns 1 for an illegal byte sequence (also in incomplete byte seq.).
     * Returns number > "size" for an incomplete byte sequence.
     * Never returns zero.
     */
    /*private*/ static int us_ptr2len_len(Bytes p, int size)
    {
        int len = us_byte2len(p.at(0), false);
        if (len == 1)
            return 1;       /* NUL, ASCII or illegal lead byte */

        int m = (size < len) ? size : len;  /* incomplete byte sequence? */
        for (int i = 1; i < m; i++)
            if ((char_u(p.at(i)) & 0xc0) != 0x80)
                return 1;

        return len;
    }

    /*
     * Return the number of bytes the UTF-8 encoding of the character at "p" takes.
     * This includes following composing characters.
     */
    /*private*/ static int us_ptr2len_cc(Bytes p)
    {
        if (p.at(0) == NUL)
            return 0;
        if (char_u(p.at(0)) < 0x80 && char_u(p.at(1)) < 0x80)     /* be quick for ASCII */
            return 1;

        /* Skip over first UTF-8 char, stopping at a NUL byte. */
        int len = us_ptr2len(p);

        /* Check for illegal byte. */
        if (len == 1 && 0x80 <= char_u(p.at(0)))
            return 1;

        /*
         * Check for composing characters.  We can handle only the first six,
         * but skip all of them (otherwise the cursor would get stuck).
         */
        while (0x80 <= char_u(p.at(len)) && utf_iscomposing(us_ptr2char(p.plus(len))))
        {
            /* Skip over composing char. */
            len += us_ptr2len(p.plus(len));
        }

        return len;
    }

    /*
     * Return the number of bytes the UTF-8 encoding of the character at "p[size]" takes.
     * This includes following composing characters.
     * Returns 0 for an empty string.
     * Returns 1 for an illegal char or an incomplete byte sequence.
     */
    /*private*/ static int us_ptr2len_cc_len(Bytes p, int size)
    {
        if (size < 1 || p.at(0) == NUL)
            return 0;
        if (char_u(p.at(0)) < 0x80 && (size == 1 || char_u(p.at(1)) < 0x80)) /* be quick for ASCII */
            return 1;

        /* Skip over first UTF-8 char, stopping at a NUL byte. */
        int len = us_ptr2len_len(p, size);

        /* Check for illegal byte and incomplete byte sequence. */
        if ((len == 1 && 0x80 <= char_u(p.at(0))) || size < len)
            return 1;

        /*
         * Check for composing characters.  We can handle only the first six,
         * but skip all of them (otherwise the cursor would get stuck).
         */
        while (len < size && 0x80 <= char_u(p.at(len)))
        {
            /*
             * Next character length should not go beyond size to ensure
             * that UTF_COMPOSINGLIKE(...) does not read beyond size.
             */
            int len_next_char = us_ptr2len_len(p.plus(len), size - len);
            if (size - len < len_next_char)
                break;

            if (!utf_iscomposing(us_ptr2char(p.plus(len))))
                break;

            /* Skip over composing char. */
            len += len_next_char;
        }

        return len;
    }

    /*
     * Return the number of bytes the UTF-8 encoding of character "c" takes.
     * This does not include composing characters.
     */
    /*private*/ static int utf_char2len(int c)
    {
        if (c < 0x80)
            return 1;
        if (c < 0x800)
            return 2;
        if (c < 0x10000)
            return 3;
        if (c < 0x200000)
            return 4;
        if (c < 0x4000000)
            return 5;

        return 6;
    }

    /*
     * Convert Unicode character "c" to UTF-8 string in "buf[]".
     * Returns the number of bytes.
     * This does not include composing characters.
     */
    /*private*/ static int utf_char2bytes(int c, Bytes buf)
    {
        if (c < 0x80)               /* 7 bits */
        {
            buf.be(0, c);
            return 1;
        }

        if (c < 0x800)              /* 11 bits */
        {
            buf.be(0, 0xc0 + (c >>> 6));
            buf.be(1, 0x80 + (c & 0x3f));
            return 2;
        }

        if (c < 0x10000)            /* 16 bits */
        {
            buf.be(0, 0xe0 + (c >>> 12));
            buf.be(1, 0x80 + ((c >>> 6) & 0x3f));
            buf.be(2, 0x80 + (c & 0x3f));
            return 3;
        }

        if (c < 0x200000)           /* 21 bits */
        {
            buf.be(0, 0xf0 + (c >>> 18));
            buf.be(1, 0x80 + ((c >>> 12) & 0x3f));
            buf.be(2, 0x80 + ((c >>> 6) & 0x3f));
            buf.be(3, 0x80 + (c & 0x3f));
            return 4;
        }

        if (c < 0x4000000)          /* 26 bits */
        {
            buf.be(0, 0xf8 + (c >>> 24));
            buf.be(1, 0x80 + ((c >>> 18) & 0x3f));
            buf.be(2, 0x80 + ((c >>> 12) & 0x3f));
            buf.be(3, 0x80 + ((c >>> 6) & 0x3f));
            buf.be(4, 0x80 + (c & 0x3f));
            return 5;
        }

                                    /* 31 bits */
        {
            buf.be(0, 0xfc + (c >>> 30));
            buf.be(1, 0x80 + ((c >>> 24) & 0x3f));
            buf.be(2, 0x80 + ((c >>> 18) & 0x3f));
            buf.be(3, 0x80 + ((c >>> 12) & 0x3f));
            buf.be(4, 0x80 + ((c >>> 6) & 0x3f));
            buf.be(5, 0x80 + (c & 0x3f));
            return 6;
        }
    }

    /* Sorted list of non-overlapping intervals.
     * Generated by tools/unicode.vim. */
    /*private*/ static int[] combining =
    {
        0x0300, 0x036f,
        0x0483, 0x0489,
        0x0591, 0x05bd,
        0x05bf, 0x05bf,
        0x05c1, 0x05c2,
        0x05c4, 0x05c5,
        0x05c7, 0x05c7,
        0x0610, 0x061a,
        0x064b, 0x065f,
        0x0670, 0x0670,
        0x06d6, 0x06dc,
        0x06df, 0x06e4,
        0x06e7, 0x06e8,
        0x06ea, 0x06ed,
        0x0711, 0x0711,
        0x0730, 0x074a,
        0x07a6, 0x07b0,
        0x07eb, 0x07f3,
        0x0816, 0x0819,
        0x081b, 0x0823,
        0x0825, 0x0827,
        0x0829, 0x082d,
        0x0859, 0x085b,
        0x08e4, 0x0903,
        0x093a, 0x093c,
        0x093e, 0x094f,
        0x0951, 0x0957,
        0x0962, 0x0963,
        0x0981, 0x0983,
        0x09bc, 0x09bc,
        0x09be, 0x09c4,
        0x09c7, 0x09c8,
        0x09cb, 0x09cd,
        0x09d7, 0x09d7,
        0x09e2, 0x09e3,
        0x0a01, 0x0a03,
        0x0a3c, 0x0a3c,
        0x0a3e, 0x0a42,
        0x0a47, 0x0a48,
        0x0a4b, 0x0a4d,
        0x0a51, 0x0a51,
        0x0a70, 0x0a71,
        0x0a75, 0x0a75,
        0x0a81, 0x0a83,
        0x0abc, 0x0abc,
        0x0abe, 0x0ac5,
        0x0ac7, 0x0ac9,
        0x0acb, 0x0acd,
        0x0ae2, 0x0ae3,
        0x0b01, 0x0b03,
        0x0b3c, 0x0b3c,
        0x0b3e, 0x0b44,
        0x0b47, 0x0b48,
        0x0b4b, 0x0b4d,
        0x0b56, 0x0b57,
        0x0b62, 0x0b63,
        0x0b82, 0x0b82,
        0x0bbe, 0x0bc2,
        0x0bc6, 0x0bc8,
        0x0bca, 0x0bcd,
        0x0bd7, 0x0bd7,
        0x0c00, 0x0c03,
        0x0c3e, 0x0c44,
        0x0c46, 0x0c48,
        0x0c4a, 0x0c4d,
        0x0c55, 0x0c56,
        0x0c62, 0x0c63,
        0x0c81, 0x0c83,
        0x0cbc, 0x0cbc,
        0x0cbe, 0x0cc4,
        0x0cc6, 0x0cc8,
        0x0cca, 0x0ccd,
        0x0cd5, 0x0cd6,
        0x0ce2, 0x0ce3,
        0x0d01, 0x0d03,
        0x0d3e, 0x0d44,
        0x0d46, 0x0d48,
        0x0d4a, 0x0d4d,
        0x0d57, 0x0d57,
        0x0d62, 0x0d63,
        0x0d82, 0x0d83,
        0x0dca, 0x0dca,
        0x0dcf, 0x0dd4,
        0x0dd6, 0x0dd6,
        0x0dd8, 0x0ddf,
        0x0df2, 0x0df3,
        0x0e31, 0x0e31,
        0x0e34, 0x0e3a,
        0x0e47, 0x0e4e,
        0x0eb1, 0x0eb1,
        0x0eb4, 0x0eb9,
        0x0ebb, 0x0ebc,
        0x0ec8, 0x0ecd,
        0x0f18, 0x0f19,
        0x0f35, 0x0f35,
        0x0f37, 0x0f37,
        0x0f39, 0x0f39,
        0x0f3e, 0x0f3f,
        0x0f71, 0x0f84,
        0x0f86, 0x0f87,
        0x0f8d, 0x0f97,
        0x0f99, 0x0fbc,
        0x0fc6, 0x0fc6,
        0x102b, 0x103e,
        0x1056, 0x1059,
        0x105e, 0x1060,
        0x1062, 0x1064,
        0x1067, 0x106d,
        0x1071, 0x1074,
        0x1082, 0x108d,
        0x108f, 0x108f,
        0x109a, 0x109d,
        0x135d, 0x135f,
        0x1712, 0x1714,
        0x1732, 0x1734,
        0x1752, 0x1753,
        0x1772, 0x1773,
        0x17b4, 0x17d3,
        0x17dd, 0x17dd,
        0x180b, 0x180d,
        0x18a9, 0x18a9,
        0x1920, 0x192b,
        0x1930, 0x193b,
        0x19b0, 0x19c0,
        0x19c8, 0x19c9,
        0x1a17, 0x1a1b,
        0x1a55, 0x1a5e,
        0x1a60, 0x1a7c,
        0x1a7f, 0x1a7f,
        0x1ab0, 0x1abe,
        0x1b00, 0x1b04,
        0x1b34, 0x1b44,
        0x1b6b, 0x1b73,
        0x1b80, 0x1b82,
        0x1ba1, 0x1bad,
        0x1be6, 0x1bf3,
        0x1c24, 0x1c37,
        0x1cd0, 0x1cd2,
        0x1cd4, 0x1ce8,
        0x1ced, 0x1ced,
        0x1cf2, 0x1cf4,
        0x1cf8, 0x1cf9,
        0x1dc0, 0x1df5,
        0x1dfc, 0x1dff,
        0x20d0, 0x20f0,
        0x2cef, 0x2cf1,
        0x2d7f, 0x2d7f,
        0x2de0, 0x2dff,
        0x302a, 0x302f,
        0x3099, 0x309a,
        0xa66f, 0xa672,
        0xa674, 0xa67d,
        0xa69f, 0xa69f,
        0xa6f0, 0xa6f1,
        0xa802, 0xa802,
        0xa806, 0xa806,
        0xa80b, 0xa80b,
        0xa823, 0xa827,
        0xa880, 0xa881,
        0xa8b4, 0xa8c4,
        0xa8e0, 0xa8f1,
        0xa926, 0xa92d,
        0xa947, 0xa953,
        0xa980, 0xa983,
        0xa9b3, 0xa9c0,
        0xa9e5, 0xa9e5,
        0xaa29, 0xaa36,
        0xaa43, 0xaa43,
        0xaa4c, 0xaa4d,
        0xaa7b, 0xaa7d,
        0xaab0, 0xaab0,
        0xaab2, 0xaab4,
        0xaab7, 0xaab8,
        0xaabe, 0xaabf,
        0xaac1, 0xaac1,
        0xaaeb, 0xaaef,
        0xaaf5, 0xaaf6,
        0xabe3, 0xabea,
        0xabec, 0xabed,
        0xfb1e, 0xfb1e,
        0xfe00, 0xfe0f,
        0xfe20, 0xfe2d,
        0x101fd, 0x101fd,
        0x102e0, 0x102e0,
        0x10376, 0x1037a,
        0x10a01, 0x10a03,
        0x10a05, 0x10a06,
        0x10a0c, 0x10a0f,
        0x10a38, 0x10a3a,
        0x10a3f, 0x10a3f,
        0x10ae5, 0x10ae6,
        0x11000, 0x11002,
        0x11038, 0x11046,
        0x1107f, 0x11082,
        0x110b0, 0x110ba,
        0x11100, 0x11102,
        0x11127, 0x11134,
        0x11173, 0x11173,
        0x11180, 0x11182,
        0x111b3, 0x111c0,
        0x1122c, 0x11237,
        0x112df, 0x112ea,
        0x11301, 0x11303,
        0x1133c, 0x1133c,
        0x1133e, 0x11344,
        0x11347, 0x11348,
        0x1134b, 0x1134d,
        0x11357, 0x11357,
        0x11362, 0x11363,
        0x11366, 0x1136c,
        0x11370, 0x11374,
        0x114b0, 0x114c3,
        0x115af, 0x115b5,
        0x115b8, 0x115c0,
        0x11630, 0x11640,
        0x116ab, 0x116b7,
        0x16af0, 0x16af4,
        0x16b30, 0x16b36,
        0x16f51, 0x16f7e,
        0x16f8f, 0x16f92,
        0x1bc9d, 0x1bc9e,
        0x1d165, 0x1d169,
        0x1d16d, 0x1d172,
        0x1d17b, 0x1d182,
        0x1d185, 0x1d18b,
        0x1d1aa, 0x1d1ad,
        0x1d242, 0x1d244,
        0x1e8d0, 0x1e8d6,
        0xe0100, 0xe01ef
    };

    /*
     * Return true if "c" is a composing UTF-8 character.
     * This means it will be drawn on top of the preceding character.
     */
    /*private*/ static boolean utf_iscomposing(int c)
    {
        return intable(combining, c);
    }

    /* Sorted list of non-overlapping intervals.
     * 0xd800-0xdfff is reserved for UTF-16, actually illegal. */
    /*private*/ static int[] nonprint =
    {
        0x070f, 0x070f,
        0x180b, 0x180e,
        0x200b, 0x200f,
        0x202a, 0x202e,
        0x206a, 0x206f,
        0xd800, 0xdfff,
        0xfeff, 0xfeff,
        0xfff9, 0xfffb,
        0xfffe, 0xffff
    };

    /*
     * Return true for characters that can be displayed in a normal way.
     * Only for characters of 0x100 and above!
     */
    /*private*/ static boolean utf_printable(int c)
    {
        return !intable(nonprint, c);
    }

    /* sorted list of non-overlapping intervals */
    /*private*/ static int[] classes =
    {
        0x037e, 0x037e, 1,          /* Greek question mark */
        0x0387, 0x0387, 1,          /* Greek ano teleia */
        0x055a, 0x055f, 1,          /* Armenian punctuation */
        0x0589, 0x0589, 1,          /* Armenian full stop */
        0x05be, 0x05be, 1,
        0x05c0, 0x05c0, 1,
        0x05c3, 0x05c3, 1,
        0x05f3, 0x05f4, 1,
        0x060c, 0x060c, 1,
        0x061b, 0x061b, 1,
        0x061f, 0x061f, 1,
        0x066a, 0x066d, 1,
        0x06d4, 0x06d4, 1,
        0x0700, 0x070d, 1,          /* Syriac punctuation */
        0x0964, 0x0965, 1,
        0x0970, 0x0970, 1,
        0x0df4, 0x0df4, 1,
        0x0e4f, 0x0e4f, 1,
        0x0e5a, 0x0e5b, 1,
        0x0f04, 0x0f12, 1,
        0x0f3a, 0x0f3d, 1,
        0x0f85, 0x0f85, 1,
        0x104a, 0x104f, 1,          /* Myanmar punctuation */
        0x10fb, 0x10fb, 1,          /* Georgian punctuation */
        0x1361, 0x1368, 1,          /* Ethiopic punctuation */
        0x166d, 0x166e, 1,          /* Canadian Syl. punctuation */
        0x1680, 0x1680, 0,
        0x169b, 0x169c, 1,
        0x16eb, 0x16ed, 1,
        0x1735, 0x1736, 1,
        0x17d4, 0x17dc, 1,          /* Khmer punctuation */
        0x1800, 0x180a, 1,          /* Mongolian punctuation */
        0x2000, 0x200b, 0,          /* spaces */
        0x200c, 0x2027, 1,          /* punctuation and symbols */
        0x2028, 0x2029, 0,
        0x202a, 0x202e, 1,          /* punctuation and symbols */
        0x202f, 0x202f, 0,
        0x2030, 0x205e, 1,          /* punctuation and symbols */
        0x205f, 0x205f, 0,
        0x2060, 0x27ff, 1,          /* punctuation and symbols */
        0x2070, 0x207f, 0x2070,     /* superscript */
        0x2080, 0x2094, 0x2080,     /* subscript */
        0x20a0, 0x27ff, 1,          /* all kinds of symbols */
        0x2800, 0x28ff, 0x2800,     /* braille */
        0x2900, 0x2998, 1,          /* arrows, brackets, etc. */
        0x29d8, 0x29db, 1,
        0x29fc, 0x29fd, 1,
        0x2e00, 0x2e7f, 1,          /* supplemental punctuation */
        0x3000, 0x3000, 0,          /* ideographic space */
        0x3001, 0x3020, 1,          /* ideographic punctuation */
        0x3030, 0x3030, 1,
        0x303d, 0x303d, 1,
        0x3040, 0x309f, 0x3040,     /* Hiragana */
        0x30a0, 0x30ff, 0x30a0,     /* Katakana */
        0x3300, 0x9fff, 0x4e00,     /* CJK Ideographs */
        0xac00, 0xd7a3, 0xac00,     /* Hangul Syllables */
        0xf900, 0xfaff, 0x4e00,     /* CJK Ideographs */
        0xfd3e, 0xfd3f, 1,
        0xfe30, 0xfe6b, 1,          /* punctuation forms */
        0xff00, 0xff0f, 1,          /* half/fullwidth ASCII */
        0xff1a, 0xff20, 1,          /* half/fullwidth ASCII */
        0xff3b, 0xff40, 1,          /* half/fullwidth ASCII */
        0xff5b, 0xff65, 1,          /* half/fullwidth ASCII */
        0x20000, 0x2a6df, 0x4e00,   /* CJK Ideographs */
        0x2a700, 0x2b73f, 0x4e00,   /* CJK Ideographs */
        0x2b740, 0x2b81f, 0x4e00,   /* CJK Ideographs */
        0x2f800, 0x2fa1f, 0x4e00    /* CJK Ideographs */
    };

    /*
     * Get class of a Unicode character.
     *  0: white space
     *  1: punctuation
     *  2 or bigger: some class of word character.
     */
    /*private*/ static int utf_class(int c)
    {
        /* First quick check for Latin1 characters, use 'iskeyword'. */
        if (c < 0x100)
        {
            if (c == ' ' || c == '\t' || c == NUL || c == 0xa0)
                return 0;       /* blank */
            if (vim_iswordc(c, curbuf))
                return 2;       /* word character */

            return 1;           /* punctuation */
        }

        /* binary search in table */
        for (int bot = 0, top = classes.length / 3 - 1; bot <= top; )
        {
            int mid = (bot + top) / 2;
            if (classes[3 * mid + 1] < c)
                bot = mid + 1;
            else if (c < classes[3 * mid])
                top = mid - 1;
            else
                return classes[3 * mid + 2];
        }

        /* most other characters are "word" characters */
        return 2;
    }

    /*
     * Code for Unicode case-dependent operations.  Based on notes in
     * http://www.unicode.org/Public/UNIDATA/CaseFolding.txt
     * This code uses simple case folding, not full case folding.
     * Last updated for Unicode 5.2.
     */

    /*
     * Generic conversion function for case operations.
     * Return the converted equivalent of "c", which is a UCS-4 character.
     * Use the given conversion "table".
     * Uses binary search on "table".
     */
    /*private*/ static int utf_convert(int c, int[] table)
    {
        int start = 0, entries = table.length / 4;

        for (int end = entries; start < end; )
        {
            /* need to search further */
            int mid = (start + end) / 2;
            if (table[4 * mid + 1] < c)
                start = mid + 1;
            else
                end = mid;
        }

        if (start < entries)
        {
            int i = 4 * start;
            if (table[i] <= c && c <= table[i + 1] && (c - table[i]) % table[i + 2] == 0)
                return c + table[i + 3];
        }

        return c;
    }

    /*
     * The following tables are built by tools/unicode.vim.
     * They must be in numeric order, because we use binary search.
     * An entry such as {0x41,0x5a,1,32} means that Unicode characters in the
     * range from 0x41 to 0x5a inclusive, stepping by 1, are changed to
     * folded/upper/lower by adding 32.
     */

    /*private*/ static int[] foldCase =
    {
        0x41, 0x5a, 1, 32,
        0xb5, 0xb5,-1, 775,
        0xc0, 0xd6, 1, 32,
        0xd8, 0xde, 1, 32,
        0x100, 0x12e, 2, 1,
        0x132, 0x136, 2, 1,
        0x139, 0x147, 2, 1,
        0x14a, 0x176, 2, 1,
        0x178, 0x178,-1,-121,
        0x179, 0x17d, 2, 1,
        0x17f, 0x17f,-1,-268,
        0x181, 0x181,-1, 210,
        0x182, 0x184, 2, 1,
        0x186, 0x186,-1, 206,
        0x187, 0x187,-1, 1,
        0x189, 0x18a, 1, 205,
        0x18b, 0x18b,-1, 1,
        0x18e, 0x18e,-1, 79,
        0x18f, 0x18f,-1, 202,
        0x190, 0x190,-1, 203,
        0x191, 0x191,-1, 1,
        0x193, 0x193,-1, 205,
        0x194, 0x194,-1, 207,
        0x196, 0x196,-1, 211,
        0x197, 0x197,-1, 209,
        0x198, 0x198,-1, 1,
        0x19c, 0x19c,-1, 211,
        0x19d, 0x19d,-1, 213,
        0x19f, 0x19f,-1, 214,
        0x1a0, 0x1a4, 2, 1,
        0x1a6, 0x1a6,-1, 218,
        0x1a7, 0x1a7,-1, 1,
        0x1a9, 0x1a9,-1, 218,
        0x1ac, 0x1ac,-1, 1,
        0x1ae, 0x1ae,-1, 218,
        0x1af, 0x1af,-1, 1,
        0x1b1, 0x1b2, 1, 217,
        0x1b3, 0x1b5, 2, 1,
        0x1b7, 0x1b7,-1, 219,
        0x1b8, 0x1bc, 4, 1,
        0x1c4, 0x1c4,-1, 2,
        0x1c5, 0x1c5,-1, 1,
        0x1c7, 0x1c7,-1, 2,
        0x1c8, 0x1c8,-1, 1,
        0x1ca, 0x1ca,-1, 2,
        0x1cb, 0x1db, 2, 1,
        0x1de, 0x1ee, 2, 1,
        0x1f1, 0x1f1,-1, 2,
        0x1f2, 0x1f4, 2, 1,
        0x1f6, 0x1f6,-1,-97,
        0x1f7, 0x1f7,-1,-56,
        0x1f8, 0x21e, 2, 1,
        0x220, 0x220,-1,-130,
        0x222, 0x232, 2, 1,
        0x23a, 0x23a,-1, 10795,
        0x23b, 0x23b,-1, 1,
        0x23d, 0x23d,-1,-163,
        0x23e, 0x23e,-1, 10792,
        0x241, 0x241,-1, 1,
        0x243, 0x243,-1,-195,
        0x244, 0x244,-1, 69,
        0x245, 0x245,-1, 71,
        0x246, 0x24e, 2, 1,
        0x345, 0x345,-1, 116,
        0x370, 0x372, 2, 1,
        0x376, 0x376,-1, 1,
        0x37f, 0x37f,-1, 116,
        0x386, 0x386,-1, 38,
        0x388, 0x38a, 1, 37,
        0x38c, 0x38c,-1, 64,
        0x38e, 0x38f, 1, 63,
        0x391, 0x3a1, 1, 32,
        0x3a3, 0x3ab, 1, 32,
        0x3c2, 0x3c2,-1, 1,
        0x3cf, 0x3cf,-1, 8,
        0x3d0, 0x3d0,-1,-30,
        0x3d1, 0x3d1,-1,-25,
        0x3d5, 0x3d5,-1,-15,
        0x3d6, 0x3d6,-1,-22,
        0x3d8, 0x3ee, 2, 1,
        0x3f0, 0x3f0,-1,-54,
        0x3f1, 0x3f1,-1,-48,
        0x3f4, 0x3f4,-1,-60,
        0x3f5, 0x3f5,-1,-64,
        0x3f7, 0x3f7,-1, 1,
        0x3f9, 0x3f9,-1,-7,
        0x3fa, 0x3fa,-1, 1,
        0x3fd, 0x3ff, 1,-130,
        0x400, 0x40f, 1, 80,
        0x410, 0x42f, 1, 32,
        0x460, 0x480, 2, 1,
        0x48a, 0x4be, 2, 1,
        0x4c0, 0x4c0,-1, 15,
        0x4c1, 0x4cd, 2, 1,
        0x4d0, 0x52e, 2, 1,
        0x531, 0x556, 1, 48,
        0x10a0, 0x10c5, 1, 7264,
        0x10c7, 0x10cd, 6, 7264,
        0x1e00, 0x1e94, 2, 1,
        0x1e9b, 0x1e9b,-1,-58,
        0x1e9e, 0x1e9e,-1,-7615,
        0x1ea0, 0x1efe, 2, 1,
        0x1f08, 0x1f0f, 1,-8,
        0x1f18, 0x1f1d, 1,-8,
        0x1f28, 0x1f2f, 1,-8,
        0x1f38, 0x1f3f, 1,-8,
        0x1f48, 0x1f4d, 1,-8,
        0x1f59, 0x1f5f, 2,-8,
        0x1f68, 0x1f6f, 1,-8,
        0x1f88, 0x1f8f, 1,-8,
        0x1f98, 0x1f9f, 1,-8,
        0x1fa8, 0x1faf, 1,-8,
        0x1fb8, 0x1fb9, 1,-8,
        0x1fba, 0x1fbb, 1,-74,
        0x1fbc, 0x1fbc,-1,-9,
        0x1fbe, 0x1fbe,-1,-7173,
        0x1fc8, 0x1fcb, 1,-86,
        0x1fcc, 0x1fcc,-1,-9,
        0x1fd8, 0x1fd9, 1,-8,
        0x1fda, 0x1fdb, 1,-100,
        0x1fe8, 0x1fe9, 1,-8,
        0x1fea, 0x1feb, 1,-112,
        0x1fec, 0x1fec,-1,-7,
        0x1ff8, 0x1ff9, 1,-128,
        0x1ffa, 0x1ffb, 1,-126,
        0x1ffc, 0x1ffc,-1,-9,
        0x2126, 0x2126,-1,-7517,
        0x212a, 0x212a,-1,-8383,
        0x212b, 0x212b,-1,-8262,
        0x2132, 0x2132,-1, 28,
        0x2160, 0x216f, 1, 16,
        0x2183, 0x2183,-1, 1,
        0x24b6, 0x24cf, 1, 26,
        0x2c00, 0x2c2e, 1, 48,
        0x2c60, 0x2c60,-1, 1,
        0x2c62, 0x2c62,-1,-10743,
        0x2c63, 0x2c63,-1,-3814,
        0x2c64, 0x2c64,-1,-10727,
        0x2c67, 0x2c6b, 2, 1,
        0x2c6d, 0x2c6d,-1,-10780,
        0x2c6e, 0x2c6e,-1,-10749,
        0x2c6f, 0x2c6f,-1,-10783,
        0x2c70, 0x2c70,-1,-10782,
        0x2c72, 0x2c75, 3, 1,
        0x2c7e, 0x2c7f, 1,-10815,
        0x2c80, 0x2ce2, 2, 1,
        0x2ceb, 0x2ced, 2, 1,
        0x2cf2, 0xa640, 31054, 1,
        0xa642, 0xa66c, 2, 1,
        0xa680, 0xa69a, 2, 1,
        0xa722, 0xa72e, 2, 1,
        0xa732, 0xa76e, 2, 1,
        0xa779, 0xa77b, 2, 1,
        0xa77d, 0xa77d,-1,-35332,
        0xa77e, 0xa786, 2, 1,
        0xa78b, 0xa78b,-1, 1,
        0xa78d, 0xa78d,-1,-42280,
        0xa790, 0xa792, 2, 1,
        0xa796, 0xa7a8, 2, 1,
        0xa7aa, 0xa7aa,-1,-42308,
        0xa7ab, 0xa7ab,-1,-42319,
        0xa7ac, 0xa7ac,-1,-42315,
        0xa7ad, 0xa7ad,-1,-42305,
        0xa7b0, 0xa7b0,-1,-42258,
        0xa7b1, 0xa7b1,-1,-42282,
        0xff21, 0xff3a, 1, 32,
        0x10400, 0x10427, 1, 40,
        0x118a0, 0x118bf, 1, 32
    };

    /*
     * Return the folded-case equivalent of "c", which is a UCS-4 character.  Uses simple case folding.
     */
    /*private*/ static int utf_fold(int c)
    {
        return utf_convert(c, foldCase);
    }

    /*private*/ static int[] toLower =
    {
        0x41, 0x5a, 1, 32,
        0xc0, 0xd6, 1, 32,
        0xd8, 0xde, 1, 32,
        0x100, 0x12e, 2, 1,
        0x130, 0x130,-1,-199,
        0x132, 0x136, 2, 1,
        0x139, 0x147, 2, 1,
        0x14a, 0x176, 2, 1,
        0x178, 0x178,-1,-121,
        0x179, 0x17d, 2, 1,
        0x181, 0x181,-1, 210,
        0x182, 0x184, 2, 1,
        0x186, 0x186,-1, 206,
        0x187, 0x187,-1, 1,
        0x189, 0x18a, 1, 205,
        0x18b, 0x18b,-1, 1,
        0x18e, 0x18e,-1, 79,
        0x18f, 0x18f,-1, 202,
        0x190, 0x190,-1, 203,
        0x191, 0x191,-1, 1,
        0x193, 0x193,-1, 205,
        0x194, 0x194,-1, 207,
        0x196, 0x196,-1, 211,
        0x197, 0x197,-1, 209,
        0x198, 0x198,-1, 1,
        0x19c, 0x19c,-1, 211,
        0x19d, 0x19d,-1, 213,
        0x19f, 0x19f,-1, 214,
        0x1a0, 0x1a4, 2, 1,
        0x1a6, 0x1a6,-1, 218,
        0x1a7, 0x1a7,-1, 1,
        0x1a9, 0x1a9,-1, 218,
        0x1ac, 0x1ac,-1, 1,
        0x1ae, 0x1ae,-1, 218,
        0x1af, 0x1af,-1, 1,
        0x1b1, 0x1b2, 1, 217,
        0x1b3, 0x1b5, 2, 1,
        0x1b7, 0x1b7,-1, 219,
        0x1b8, 0x1bc, 4, 1,
        0x1c4, 0x1c4,-1, 2,
        0x1c5, 0x1c5,-1, 1,
        0x1c7, 0x1c7,-1, 2,
        0x1c8, 0x1c8,-1, 1,
        0x1ca, 0x1ca,-1, 2,
        0x1cb, 0x1db, 2, 1,
        0x1de, 0x1ee, 2, 1,
        0x1f1, 0x1f1,-1, 2,
        0x1f2, 0x1f4, 2, 1,
        0x1f6, 0x1f6,-1,-97,
        0x1f7, 0x1f7,-1,-56,
        0x1f8, 0x21e, 2, 1,
        0x220, 0x220,-1,-130,
        0x222, 0x232, 2, 1,
        0x23a, 0x23a,-1, 10795,
        0x23b, 0x23b,-1, 1,
        0x23d, 0x23d,-1,-163,
        0x23e, 0x23e,-1, 10792,
        0x241, 0x241,-1, 1,
        0x243, 0x243,-1,-195,
        0x244, 0x244,-1, 69,
        0x245, 0x245,-1, 71,
        0x246, 0x24e, 2, 1,
        0x370, 0x372, 2, 1,
        0x376, 0x376,-1, 1,
        0x37f, 0x37f,-1, 116,
        0x386, 0x386,-1, 38,
        0x388, 0x38a, 1, 37,
        0x38c, 0x38c,-1, 64,
        0x38e, 0x38f, 1, 63,
        0x391, 0x3a1, 1, 32,
        0x3a3, 0x3ab, 1, 32,
        0x3cf, 0x3cf,-1, 8,
        0x3d8, 0x3ee, 2, 1,
        0x3f4, 0x3f4,-1,-60,
        0x3f7, 0x3f7,-1, 1,
        0x3f9, 0x3f9,-1,-7,
        0x3fa, 0x3fa,-1, 1,
        0x3fd, 0x3ff, 1,-130,
        0x400, 0x40f, 1, 80,
        0x410, 0x42f, 1, 32,
        0x460, 0x480, 2, 1,
        0x48a, 0x4be, 2, 1,
        0x4c0, 0x4c0,-1, 15,
        0x4c1, 0x4cd, 2, 1,
        0x4d0, 0x52e, 2, 1,
        0x531, 0x556, 1, 48,
        0x10a0, 0x10c5, 1, 7264,
        0x10c7, 0x10cd, 6, 7264,
        0x1e00, 0x1e94, 2, 1,
        0x1e9e, 0x1e9e,-1,-7615,
        0x1ea0, 0x1efe, 2, 1,
        0x1f08, 0x1f0f, 1,-8,
        0x1f18, 0x1f1d, 1,-8,
        0x1f28, 0x1f2f, 1,-8,
        0x1f38, 0x1f3f, 1,-8,
        0x1f48, 0x1f4d, 1,-8,
        0x1f59, 0x1f5f, 2,-8,
        0x1f68, 0x1f6f, 1,-8,
        0x1f88, 0x1f8f, 1,-8,
        0x1f98, 0x1f9f, 1,-8,
        0x1fa8, 0x1faf, 1,-8,
        0x1fb8, 0x1fb9, 1,-8,
        0x1fba, 0x1fbb, 1,-74,
        0x1fbc, 0x1fbc,-1,-9,
        0x1fc8, 0x1fcb, 1,-86,
        0x1fcc, 0x1fcc,-1,-9,
        0x1fd8, 0x1fd9, 1,-8,
        0x1fda, 0x1fdb, 1,-100,
        0x1fe8, 0x1fe9, 1,-8,
        0x1fea, 0x1feb, 1,-112,
        0x1fec, 0x1fec,-1,-7,
        0x1ff8, 0x1ff9, 1,-128,
        0x1ffa, 0x1ffb, 1,-126,
        0x1ffc, 0x1ffc,-1,-9,
        0x2126, 0x2126,-1,-7517,
        0x212a, 0x212a,-1,-8383,
        0x212b, 0x212b,-1,-8262,
        0x2132, 0x2132,-1, 28,
        0x2160, 0x216f, 1, 16,
        0x2183, 0x2183,-1, 1,
        0x24b6, 0x24cf, 1, 26,
        0x2c00, 0x2c2e, 1, 48,
        0x2c60, 0x2c60,-1, 1,
        0x2c62, 0x2c62,-1,-10743,
        0x2c63, 0x2c63,-1,-3814,
        0x2c64, 0x2c64,-1,-10727,
        0x2c67, 0x2c6b, 2, 1,
        0x2c6d, 0x2c6d,-1,-10780,
        0x2c6e, 0x2c6e,-1,-10749,
        0x2c6f, 0x2c6f,-1,-10783,
        0x2c70, 0x2c70,-1,-10782,
        0x2c72, 0x2c75, 3, 1,
        0x2c7e, 0x2c7f, 1,-10815,
        0x2c80, 0x2ce2, 2, 1,
        0x2ceb, 0x2ced, 2, 1,
        0x2cf2, 0xa640, 31054, 1,
        0xa642, 0xa66c, 2, 1,
        0xa680, 0xa69a, 2, 1,
        0xa722, 0xa72e, 2, 1,
        0xa732, 0xa76e, 2, 1,
        0xa779, 0xa77b, 2, 1,
        0xa77d, 0xa77d,-1,-35332,
        0xa77e, 0xa786, 2, 1,
        0xa78b, 0xa78b,-1, 1,
        0xa78d, 0xa78d,-1,-42280,
        0xa790, 0xa792, 2, 1,
        0xa796, 0xa7a8, 2, 1,
        0xa7aa, 0xa7aa,-1,-42308,
        0xa7ab, 0xa7ab,-1,-42319,
        0xa7ac, 0xa7ac,-1,-42315,
        0xa7ad, 0xa7ad,-1,-42305,
        0xa7b0, 0xa7b0,-1,-42258,
        0xa7b1, 0xa7b1,-1,-42282,
        0xff21, 0xff3a, 1, 32,
        0x10400, 0x10427, 1, 40,
        0x118a0, 0x118bf, 1, 32
    };

    /*private*/ static int[] toUpper =
    {
        0x61, 0x7a, 1,-32,
        0xb5, 0xb5,-1, 743,
        0xe0, 0xf6, 1,-32,
        0xf8, 0xfe, 1,-32,
        0xff, 0xff,-1, 121,
        0x101, 0x12f, 2,-1,
        0x131, 0x131,-1,-232,
        0x133, 0x137, 2,-1,
        0x13a, 0x148, 2,-1,
        0x14b, 0x177, 2,-1,
        0x17a, 0x17e, 2,-1,
        0x17f, 0x17f,-1,-300,
        0x180, 0x180,-1, 195,
        0x183, 0x185, 2,-1,
        0x188, 0x18c, 4,-1,
        0x192, 0x192,-1,-1,
        0x195, 0x195,-1, 97,
        0x199, 0x199,-1,-1,
        0x19a, 0x19a,-1, 163,
        0x19e, 0x19e,-1, 130,
        0x1a1, 0x1a5, 2,-1,
        0x1a8, 0x1ad, 5,-1,
        0x1b0, 0x1b4, 4,-1,
        0x1b6, 0x1b9, 3,-1,
        0x1bd, 0x1bd,-1,-1,
        0x1bf, 0x1bf,-1, 56,
        0x1c5, 0x1c5,-1,-1,
        0x1c6, 0x1c6,-1,-2,
        0x1c8, 0x1c8,-1,-1,
        0x1c9, 0x1c9,-1,-2,
        0x1cb, 0x1cb,-1,-1,
        0x1cc, 0x1cc,-1,-2,
        0x1ce, 0x1dc, 2,-1,
        0x1dd, 0x1dd,-1,-79,
        0x1df, 0x1ef, 2,-1,
        0x1f2, 0x1f2,-1,-1,
        0x1f3, 0x1f3,-1,-2,
        0x1f5, 0x1f9, 4,-1,
        0x1fb, 0x21f, 2,-1,
        0x223, 0x233, 2,-1,
        0x23c, 0x23c,-1,-1,
        0x23f, 0x240, 1, 10815,
        0x242, 0x247, 5,-1,
        0x249, 0x24f, 2,-1,
        0x250, 0x250,-1, 10783,
        0x251, 0x251,-1, 10780,
        0x252, 0x252,-1, 10782,
        0x253, 0x253,-1,-210,
        0x254, 0x254,-1,-206,
        0x256, 0x257, 1,-205,
        0x259, 0x259,-1,-202,
        0x25b, 0x25b,-1,-203,
        0x25c, 0x25c,-1, 42319,
        0x260, 0x260,-1,-205,
        0x261, 0x261,-1, 42315,
        0x263, 0x263,-1,-207,
        0x265, 0x265,-1, 42280,
        0x266, 0x266,-1, 42308,
        0x268, 0x268,-1,-209,
        0x269, 0x269,-1,-211,
        0x26b, 0x26b,-1, 10743,
        0x26c, 0x26c,-1, 42305,
        0x26f, 0x26f,-1,-211,
        0x271, 0x271,-1, 10749,
        0x272, 0x272,-1,-213,
        0x275, 0x275,-1,-214,
        0x27d, 0x27d,-1, 10727,
        0x280, 0x283, 3,-218,
        0x287, 0x287,-1, 42282,
        0x288, 0x288,-1,-218,
        0x289, 0x289,-1,-69,
        0x28a, 0x28b, 1,-217,
        0x28c, 0x28c,-1,-71,
        0x292, 0x292,-1,-219,
        0x29e, 0x29e,-1, 42258,
        0x345, 0x345,-1, 84,
        0x371, 0x373, 2,-1,
        0x377, 0x377,-1,-1,
        0x37b, 0x37d, 1, 130,
        0x3ac, 0x3ac,-1,-38,
        0x3ad, 0x3af, 1,-37,
        0x3b1, 0x3c1, 1,-32,
        0x3c2, 0x3c2,-1,-31,
        0x3c3, 0x3cb, 1,-32,
        0x3cc, 0x3cc,-1,-64,
        0x3cd, 0x3ce, 1,-63,
        0x3d0, 0x3d0,-1,-62,
        0x3d1, 0x3d1,-1,-57,
        0x3d5, 0x3d5,-1,-47,
        0x3d6, 0x3d6,-1,-54,
        0x3d7, 0x3d7,-1,-8,
        0x3d9, 0x3ef, 2,-1,
        0x3f0, 0x3f0,-1,-86,
        0x3f1, 0x3f1,-1,-80,
        0x3f2, 0x3f2,-1, 7,
        0x3f3, 0x3f3,-1,-116,
        0x3f5, 0x3f5,-1,-96,
        0x3f8, 0x3fb, 3,-1,
        0x430, 0x44f, 1,-32,
        0x450, 0x45f, 1,-80,
        0x461, 0x481, 2,-1,
        0x48b, 0x4bf, 2,-1,
        0x4c2, 0x4ce, 2,-1,
        0x4cf, 0x4cf,-1,-15,
        0x4d1, 0x52f, 2,-1,
        0x561, 0x586, 1,-48,
        0x1d79, 0x1d79,-1, 35332,
        0x1d7d, 0x1d7d,-1, 3814,
        0x1e01, 0x1e95, 2,-1,
        0x1e9b, 0x1e9b,-1,-59,
        0x1ea1, 0x1eff, 2,-1,
        0x1f00, 0x1f07, 1, 8,
        0x1f10, 0x1f15, 1, 8,
        0x1f20, 0x1f27, 1, 8,
        0x1f30, 0x1f37, 1, 8,
        0x1f40, 0x1f45, 1, 8,
        0x1f51, 0x1f57, 2, 8,
        0x1f60, 0x1f67, 1, 8,
        0x1f70, 0x1f71, 1, 74,
        0x1f72, 0x1f75, 1, 86,
        0x1f76, 0x1f77, 1, 100,
        0x1f78, 0x1f79, 1, 128,
        0x1f7a, 0x1f7b, 1, 112,
        0x1f7c, 0x1f7d, 1, 126,
        0x1f80, 0x1f87, 1, 8,
        0x1f90, 0x1f97, 1, 8,
        0x1fa0, 0x1fa7, 1, 8,
        0x1fb0, 0x1fb1, 1, 8,
        0x1fb3, 0x1fb3,-1, 9,
        0x1fbe, 0x1fbe,-1,-7205,
        0x1fc3, 0x1fc3,-1, 9,
        0x1fd0, 0x1fd1, 1, 8,
        0x1fe0, 0x1fe1, 1, 8,
        0x1fe5, 0x1fe5,-1, 7,
        0x1ff3, 0x1ff3,-1, 9,
        0x214e, 0x214e,-1,-28,
        0x2170, 0x217f, 1,-16,
        0x2184, 0x2184,-1,-1,
        0x24d0, 0x24e9, 1,-26,
        0x2c30, 0x2c5e, 1,-48,
        0x2c61, 0x2c61,-1,-1,
        0x2c65, 0x2c65,-1,-10795,
        0x2c66, 0x2c66,-1,-10792,
        0x2c68, 0x2c6c, 2,-1,
        0x2c73, 0x2c76, 3,-1,
        0x2c81, 0x2ce3, 2,-1,
        0x2cec, 0x2cee, 2,-1,
        0x2cf3, 0x2cf3,-1,-1,
        0x2d00, 0x2d25, 1,-7264,
        0x2d27, 0x2d2d, 6,-7264,
        0xa641, 0xa66d, 2,-1,
        0xa681, 0xa69b, 2,-1,
        0xa723, 0xa72f, 2,-1,
        0xa733, 0xa76f, 2,-1,
        0xa77a, 0xa77c, 2,-1,
        0xa77f, 0xa787, 2,-1,
        0xa78c, 0xa791, 5,-1,
        0xa793, 0xa797, 4,-1,
        0xa799, 0xa7a9, 2,-1,
        0xff41, 0xff5a, 1,-32,
        0x10428, 0x1044f, 1,-40,
        0x118c0, 0x118df, 1,-32
    };

    /*
     * Return the lower-case equivalent of "c", which is a UCS-4 character.  Use simple case folding.
     */
    /*private*/ static int utf_tolower(int c)
    {
        if (c < 0x80)
            return asc_tolower(c);

        return utf_convert(c, toLower);
    }

    /*
     * Return the upper-case equivalent of "c", which is a UCS-4 character.  Use simple case folding.
     */
    /*private*/ static int utf_toupper(int c)
    {
        if (c < 0x80)
            return asc_toupper(c);

        return utf_convert(c, toUpper);
    }

    /*private*/ static boolean utf_islower(int c)
    {
        if (c < 0x80)
            return asc_islower(c);

        /* German sharp s is lower case but has no upper case equivalent. */
        return (utf_toupper(c) != c || c == 0xdf);
    }

    /*private*/ static boolean utf_isupper(int c)
    {
        if (c < 0x80)
            return asc_isupper(c);

        return (utf_tolower(c) != c);
    }

    /*private*/ static int us__strnicmp(Bytes _s1, Bytes _s2, int _n1, int _n2)
    {
        int c1, c2;
        Bytes[] s1 = { _s1 }, s2 = { _s2 };
        int[] n1 = { _n1 }, n2 = { _n2 };

        for ( ; ; )
        {
            c1 = us_safe_read_char_adv(s1, n1);
            c2 = us_safe_read_char_adv(s2, n2);

            if (c1 <= 0 || c2 <= 0)
                break;

            if (c1 == c2)
                continue;

            int cmp = utf_fold(c1) - utf_fold(c2);
            if (cmp != 0)
                return cmp;
        }

        /* some string ended or has an incomplete/illegal character sequence */

        if (c1 == 0 || c2 == 0)
        {
            /* some string ended. shorter string is smaller */
            if (c1 == 0 && c2 == 0)
                return 0;

            return (c1 == 0) ? -1 : 1;
        }

        /* Continue with bytewise comparison to produce some result that
         * would make comparison operations involving this function transitive.
         *
         * If only one string had an error, comparison should be made with
         * folded version of the other string.  In this case it is enough
         * to fold just one character to determine the result of comparison. */

        Bytes buffer = new Bytes(6);

        if (c1 != -1 && c2 == -1)
        {
            n1[0] = utf_char2bytes(utf_fold(c1), buffer);
            s1[0] = buffer;
        }
        else if (c2 != -1 && c1 == -1)
        {
            n2[0] = utf_char2bytes(utf_fold(c2), buffer);
            s2[0] = buffer;
        }

        while (0 < n1[0] && 0 < n2[0] && s1[0].at(0) != NUL && s2[0].at(0) != NUL)
        {
            int cmp = (int)s1[0].at(0) - (int)s2[0].at(0);
            if (cmp != 0)
                return cmp;

            s1[0] = s1[0].plus(1);
            s2[0] = s2[0].plus(1);
            n1[0]--;
            n2[0]--;
        }

        if (0 < n1[0] && s1[0].at(0) == NUL)
            n1[0] = 0;
        if (0 < n2[0] && s2[0].at(0) == NUL)
            n2[0] = 0;

        if (n1[0] == 0 && n2[0] == 0)
            return 0;

        return (n1[0] == 0) ? -1 : 1;
    }

    /*
     * Version of strnicmp() that handles multi-byte characters.
     * Needed for Big5, Shift-JIS and UTF-8 encoding.
     * Returns zero if s1 and s2 are equal (ignoring case),
     * the difference between two characters otherwise.
     */
    /*private*/ static int us_strnicmp(Bytes s1, Bytes s2, int nn)
    {
        return us__strnicmp(s1, s2, nn, nn);
    }

    /*
     * "g8": show bytes of the UTF-8 char under the cursor.
     * Doesn't matter what 'encoding' has been set to.
     */
    /*private*/ static void show_utf8()
    {
        Bytes p = ml_get_cursor();

        /* Get the byte length of the char under the cursor, including composing characters. */
        int len = us_ptr2len_cc(p);
        if (len == 0)
        {
            msg(u8("NUL"));
            return;
        }

        int clen = 0, rlen = 0;

        for (int i = 0; i < len; i++)
        {
            if (clen == 0)
            {
                /* start of (composing) character, get its length */
                if (0 < i)
                {
                    STRCPY(ioBuff.plus(rlen), u8("+ "));
                    rlen += 2;
                }
                clen = us_ptr2len(p.plus(i));
            }
            /* NUL is stored as NL */
            libC.sprintf(ioBuff.plus(rlen), u8("%02x "), (p.at(i) == NL) ? NUL : p.at(i));
            --clen;
            rlen += strlen(ioBuff, rlen);
            if (IOSIZE - 20 < rlen)
                break;
        }

        msg(ioBuff);
    }

    /*private*/ static int us_head_off(Bytes base, Bytes p)
    {
        if (char_u(p.at(0)) < 0x80)        /* be quick for ASCII */
            return 0;

        Bytes q;

        /* Skip backwards over trailing bytes: 10xx.xxxx
         * Skip backwards again if on a composing char. */
        for (q = p; ; q = q.minus(1))
        {
            Bytes s;
            /* Move 's' to the last byte of this char. */
            for (s = q; (char_u(s.at(1)) & 0xc0) == 0x80; s = s.plus(1))
                ;
            /* Move 'q' to the first byte of this char. */
            while (BLT(base, q) && (char_u(q.at(0)) & 0xc0) == 0x80)
                q = q.minus(1);
            /* Check for illegal sequence.  Do allow an illegal byte after where we started. */
            int len = us_byte2len(q.at(0), false);
            if (len != BDIFF(s, q) + 1 && len != BDIFF(p, q) + 1)
                return 0;

            if (BLE(q, base) || !utf_iscomposing(us_ptr2char(q)))
                break;
        }

        return BDIFF(p, q);
    }

    /*
     * Return the offset from "p" to the first byte of a character.  When "p" is
     * at the start of a character 0 is returned, otherwise the offset to the next
     * character.  Can start anywhere in a stream of bytes.
     */
    /*private*/ static int us_off_next(Bytes base, Bytes p)
    {
        if (char_u(p.at(0)) < 0x80)        /* be quick for ASCII */
            return 0;

        /* Find the next character that isn't 10xx.xxxx. */
        int i;
        for (i = 0; (char_u(p.at(i)) & 0xc0) == 0x80; i++)
            ;
        if (0 < i)
        {
            /* Check for illegal sequence. */
            int j;
            for (j = 0; BLT(base, p.minus(j)); j++)
                if ((char_u(p.at(-j)) & 0xc0) != 0x80)
                    break;
            if (us_byte2len(p.at(-j), false) != i + j)
                return 0;
        }
        return i;
    }

    /*
     * Return the offset from "p" to the last byte of the character it points into.
     * Can start anywhere in a stream of bytes.
     */
    /*private*/ static int us_tail_off(Bytes base, Bytes p)
    {
        if (p.at(0) == NUL)
            return 0;

        int i, j;

        /* Find the last character that is 10xx.xxxx. */
        for (i = 0; (char_u(p.at(i + 1)) & 0xc0) == 0x80; i++)
            ;

        /* Check for illegal sequence. */
        for (j = 0; BLT(base, p.minus(j)); j++)
            if ((char_u(p.at(-j)) & 0xc0) != 0x80)
                break;

        if (us_byte2len(p.at(-j), false) != i + j + 1)
            return 0;

        return i;
    }

    /*
     * Find the next illegal byte sequence.
     */
    /*private*/ static void utf_find_illegal()
    {
        pos_C cursor = curwin.w_cursor;
        pos_C save_pos = new pos_C();
        COPY_pos(save_pos, cursor);

        for (cursor.coladd = 0; ; cursor.lnum++, cursor.col = 0)
        {
            for (Bytes p = ml_get_cursor(); p.at(0) != NUL; )
            {
                /* Illegal means that there are not enough trail bytes (checked
                 * by us_ptr2len()) or too many of them (overlong sequence). */
                int len = us_ptr2len(p);
                if (0x80 <= char_u(p.at(0)) && (len == 1 || utf_char2len(us_ptr2char(p)) != len))
                {
                    cursor.col += BDIFF(p, ml_get_cursor());
                    return;
                }
                p = p.plus(len);
            }

            if (cursor.lnum == curbuf.b_ml.ml_line_count)
                break;
        }

        /* didn't find it: don't move and beep */
        COPY_pos(cursor, save_pos);
        beep_flush();
    }

    /*
     * Adjust position "*posp" to point to the first byte of a multi-byte character.
     * If it points to a tail byte it's moved backwards to the head byte.
     */
    /*private*/ static void mb_adjust_pos(buffer_C buf, pos_C posp)
    {
        if (0 < posp.col || 1 < posp.coladd)
        {
            Bytes p = ml_get_buf(buf, posp.lnum, false);
            posp.col -= us_head_off(p, p.plus(posp.col));
            /* Reset "coladd" when the cursor would be on the right half of a double-wide character. */
            if (posp.coladd == 1
                    && p.at(posp.col) != TAB
                    && vim_isprintc(us_ptr2char(p.plus(posp.col)))
                    && 1 < mb_ptr2cells(p.plus(posp.col)))
                posp.coladd = 0;
        }
    }

    /* Backup multi-byte pointer.  Only use with "base" < "p" ! */
    /*private*/ static int us_ptr_back(Bytes base, Bytes p)
    {
        return us_head_off(base, p.minus(1)) + 1;
    }

    /*
     * Return a pointer to the character before "*p", if there is one.
     */
    /*private*/ static Bytes us_prevptr(Bytes base, Bytes p)
        /* base: start of the string */
    {
        if (BLT(base, p))
            p = p.minus(us_ptr_back(base, p));
        return p;
    }

    /*
     * Return the character length of "p".
     * Each multi-byte character (with following composing characters) counts as one.
     */
    /*private*/ static int us_charlen(Bytes p)
    {
        int count = 0;

        if (p != null)
            for ( ; p.at(0) != NUL; p = p.plus(us_ptr2len_cc(p)))
                count++;

        return count;
    }

    /*private*/ static Bytes mb_unescape_buf = new Bytes(6);

    /*
     * Try to un-escape a multi-byte character.
     * Used for the "to" and "from" part of a mapping.
     * Return the un-escaped string if it is a multi-byte character,
     * and advance "pp" to just after the bytes that formed it.
     * Return null if no multi-byte char was found.
     */
    /*private*/ static Bytes mb_unescape(Bytes[] pp)
    {
        Bytes p = pp[0];

        /* Must translate KB_SPECIAL KS_SPECIAL KE_FILLER to KB_SPECIAL and CSI KS_EXTRA KE_CSI to CSI.
         * Maximum length of a utf-8 character is 4 bytes. */
        for (int n = 0, m = 0; p.at(n) != NUL && m < 4; n++)
        {
            if (p.at(n) == KB_SPECIAL && p.at(n + 1) == KS_SPECIAL && p.at(n + 2) == KE_FILLER)
            {
                mb_unescape_buf.be(m++, KB_SPECIAL);
                n += 2;
            }
            else if (p.at(n) == KB_SPECIAL && p.at(n + 1) == KS_EXTRA && p.at(n + 2) == KE_CSI)
            {
                mb_unescape_buf.be(m++, CSI);
                n += 2;
            }
            else if (p.at(n) == KB_SPECIAL)
                break;                          /* a special key can't be a multibyte char */
            else
                mb_unescape_buf.be(m++, p.at(n));
            mb_unescape_buf.be(m, NUL);

            /* Return a multi-byte character if it's found.
             * An illegal sequence will result in a 1 here. */
            if (1 < us_ptr2len_cc(mb_unescape_buf))
            {
                pp[0] = p.plus(n + 1);
                return mb_unescape_buf;
            }

            /* Bail out quickly for ASCII. */
            if (char_u(mb_unescape_buf.at(0)) < 0x80)
                break;
        }

        return null;
    }

    /*
     * Return true if the character at "row"/"col" on the screen
     * is the left side of a double-width character.
     * Caller must make sure "row" and "col" are not invalid!
     */
    /*private*/ static boolean mb_lefthalve(int row, int col)
    {
        return (1 < utf_off2cells(lineOffset[row] + col, lineOffset[row] + screenColumns));
    }

    /*
     * Correct a position on the screen,
     * if it's the right half of a double-wide char move it to the left half.
     * Returns the corrected column.
     */
    /*private*/ static int mb_fix_col(int col, int row)
    {
        col = check_col(col);
        row = check_row(row);

        if (screenLines != null && 0 < col && screenLines.at(lineOffset[row] + col) == 0)
            return col - 1;

        return col;
    }

    /*
     * Find the canonical name for encoding "enc".
     * When the name isn't recognized, returns "enc" itself,
     * but with all lower case characters and '_' replaced with '-'.
     * Returns an allocated string.
     */
    /*private*/ static Bytes enc_canonize(Bytes enc)
    {
        if (STRCMP(enc, u8("default")) == 0)
            return STRDUP(u8("utf-8"));

        /* copy "enc" to allocated memory, with room for two '-' */
        Bytes r = new Bytes(strlen(enc) + 3);

        /* Make it all lower case and replace '_' with '-'. */
        Bytes p = r;
        for (Bytes s = enc; s.at(0) != NUL; s = s.plus(1))
        {
            if (s.at(0) == (byte)'_')
                (p = p.plus(1)).be(-1, (byte)'-');
            else
                (p = p.plus(1)).be(-1, asc_tolower(s.at(0)));
        }
        p.be(0, NUL);

        return r;
    }

    /*
     * misc.c: functions that didn't seem to fit elsewhere --------------------------------------------
     */

    /*
     * Count the size (in window cells) of the indent in the current line.
     */
    /*private*/ static int get_indent()
    {
        return get_indent_str(ml_get_curline(), (int)curbuf.b_p_ts[0], false);
    }

    /*
     * Count the size (in window cells) of the indent in line "lnum".
     */
    /*private*/ static int get_indent_lnum(long lnum)
    {
        return get_indent_str(ml_get(lnum), (int)curbuf.b_p_ts[0], false);
    }

    /*
     * count the size (in window cells) of the indent in line "ptr", with 'tabstop' at "ts"
     */
    /*private*/ static int get_indent_str(Bytes ptr, int ts, boolean list)
        /* list: if true, count only screen size for tabs */
    {
        int count = 0;

        for ( ; ptr.at(0) != NUL; ptr = ptr.plus(1))
        {
            if (ptr.at(0) == TAB)
            {
                if (!list || lcs_tab1[0] != NUL)       /* count a tab for what it is worth */
                    count += ts - (count % ts);
                else
                    /* In list mode, when tab is not set,
                     * count screen char width for Tab, displays: ^I */
                    count += mb_ptr2cells(ptr);
            }
            else if (ptr.at(0) == (byte)' ')
                count++;                            /* count a space for one */
            else
                break;
        }

        return count;
    }

    /*
     * Set the indent of the current line.
     * Leaves the cursor on the first non-blank in the line.
     * Caller must take care of undo.
     * "flags":
     *      SIN_CHANGED:    call changed_bytes() if the line was changed.
     *      SIN_INSERT:     insert the indent in front of the line.
     *      SIN_UNDO:       save line for undo before changing it.
     * Returns true if the line was changed.
     */
    /*private*/ static boolean set_indent(int size, int flags)
        /* size: measured in spaces */
    {
        boolean doit = false;
        int ind_done = 0;                       /* measured in spaces */
        boolean retval = false;
        int orig_char_len = -1;                 /* number of initial whitespace chars
                                                 * when 'et' and 'pi' are both set */

        /*
         * First check if there is anything to do and compute
         * the number of characters needed for the indent.
         */
        int todo = size;
        int ind_len = 0;                        /* measured in characters */
        Bytes oldline = ml_get_curline();
        Bytes p = oldline;

        /* Calculate the buffer size for the new indent and check if it isn't already set. */

        /* If 'expandtab' isn't set: use TABs; if both 'expandtab' and
         * 'preserveindent' are set: count the number of characters at
         * the beginning of the line to be copied. */
        if (!curbuf.b_p_et[0] || ((flags & SIN_INSERT) == 0 && curbuf.b_p_pi[0]))
        {
            /* If 'preserveindent' is set, then reuse as much as possible
             * of the existing indent structure for the new indent. */
            if ((flags & SIN_INSERT) == 0 && curbuf.b_p_pi[0])
            {
                ind_done = 0;

                /* count as many characters as we can use */
                while (0 < todo && vim_iswhite(p.at(0)))
                {
                    if (p.at(0) == TAB)
                    {
                        int tab_pad = (int)curbuf.b_p_ts[0] - (ind_done % (int)curbuf.b_p_ts[0]);
                        /* stop if this tab will overshoot the target */
                        if (todo < tab_pad)
                            break;
                        todo -= tab_pad;
                        ind_len++;
                        ind_done += tab_pad;
                    }
                    else
                    {
                        --todo;
                        ind_len++;
                        ind_done++;
                    }
                    p = p.plus(1);
                }

                /* Set initial number of whitespace chars to copy
                 * if we are preserving indent but expandtab is set. */
                if (curbuf.b_p_et[0])
                    orig_char_len = ind_len;

                /* Fill to next tabstop with a tab, if possible. */
                int tab_pad = (int)curbuf.b_p_ts[0] - (ind_done % (int)curbuf.b_p_ts[0]);
                if (tab_pad <= todo && orig_char_len == -1)
                {
                    doit = true;
                    todo -= tab_pad;
                    ind_len++;
                    /* ind_done += tab_pad; */
                }
            }

            /* count tabs required for indent */
            while ((int)curbuf.b_p_ts[0] <= todo)
            {
                if (p.at(0) != TAB)
                    doit = true;
                else
                    p = p.plus(1);
                todo -= (int)curbuf.b_p_ts[0];
                ind_len++;
                /* ind_done += (int)curbuf.b_p_ts[0]; */
            }
        }
        /* count spaces required for indent */
        while (0 < todo)
        {
            if (p.at(0) != (byte)' ')
                doit = true;
            else
                p = p.plus(1);
            --todo;
            ind_len++;
            /* ++ind_done; */
        }

        /* Return if the indent is OK already. */
        if (!doit && !vim_iswhite(p.at(0)) && (flags & SIN_INSERT) == 0)
            return false;

        /* Allocate memory for the new line. */
        if ((flags & SIN_INSERT) != 0)
            p = oldline;
        else
            p = skipwhite(p);
        int line_len = strlen(p) + 1;

        /* If 'preserveindent' and 'expandtab' are both set keep the original
         * characters and allocate accordingly.  We will fill the rest with spaces
         * after the if (!curbuf.b_p_et) below. */
        Bytes newline;
        Bytes s;
        if (orig_char_len != -1)
        {
            newline = new Bytes(orig_char_len + size - ind_done + line_len);

            todo = size - ind_done;
            ind_len = orig_char_len + todo;     /* Set total length of indent in characters,
                                                 * which may have been undercounted until now */
            p = oldline;
            s = newline;
            while (0 < orig_char_len)
            {
                (s = s.plus(1)).be(-1, (p = p.plus(1)).at(-1));
                orig_char_len--;
            }

            /* Skip over any additional white space (useful when newindent is less than old). */
            while (vim_iswhite(p.at(0)))
                p = p.plus(1);
        }
        else
        {
            todo = size;
            newline = new Bytes(ind_len + line_len);
            s = newline;
        }

        /* Put the characters in the new line. */
        /* If 'expandtab' isn't set: use TABs. */
        if (!curbuf.b_p_et[0])
        {
            /* If 'preserveindent' is set, then reuse as much as possible
             * of the existing indent structure for the new indent. */
            if ((flags & SIN_INSERT) == 0 && curbuf.b_p_pi[0])
            {
                p = oldline;
                ind_done = 0;

                while (0 < todo && vim_iswhite(p.at(0)))
                {
                    if (p.at(0) == TAB)
                    {
                        int tab_pad = (int)curbuf.b_p_ts[0] - (ind_done % (int)curbuf.b_p_ts[0]);
                        /* stop if this tab will overshoot the target */
                        if (todo < tab_pad)
                            break;
                        todo -= tab_pad;
                        ind_done += tab_pad;
                    }
                    else
                    {
                        --todo;
                        ind_done++;
                    }
                    (s = s.plus(1)).be(-1, (p = p.plus(1)).at(-1));
                }

                /* Fill to next tabstop with a tab, if possible. */
                int tab_pad = (int)curbuf.b_p_ts[0] - (ind_done % (int)curbuf.b_p_ts[0]);
                if (tab_pad <= todo)
                {
                    (s = s.plus(1)).be(-1, TAB);
                    todo -= tab_pad;
                }

                p = skipwhite(p);
            }

            while ((int)curbuf.b_p_ts[0] <= todo)
            {
                (s = s.plus(1)).be(-1, TAB);
                todo -= (int)curbuf.b_p_ts[0];
            }
        }
        while (0 < todo)
        {
            (s = s.plus(1)).be(-1, (byte)' ');
            --todo;
        }
        BCOPY(s, p, line_len);

        /* Replace the line (unless undo fails). */
        if ((flags & SIN_UNDO) == 0 || u_savesub(curwin.w_cursor.lnum) == true)
        {
            ml_replace(curwin.w_cursor.lnum, newline, false);
            if ((flags & SIN_CHANGED) != 0)
                changed_bytes(curwin.w_cursor.lnum, 0);
            /* Correct saved cursor position if it is in this line. */
            if (saved_cursor.lnum == curwin.w_cursor.lnum)
            {
                if (BDIFF(p, oldline) <= saved_cursor.col)
                    /* cursor was after the indent, adjust for the number of bytes added/removed */
                    saved_cursor.col += ind_len - BDIFF(p, oldline);
                else if (BDIFF(s, newline) <= saved_cursor.col)
                    /* cursor was in the indent and is now after it; put it back
                     * at the start of the indent (replacing spaces with TAB) */
                    saved_cursor.col = BDIFF(s, newline);
            }
            retval = true;
        }

        curwin.w_cursor.col = ind_len;
        return retval;
    }

    /*
     * Copy the indent from ptr to the current line (and fill to size)
     * Leaves the cursor on the first non-blank in the line.
     * Returns true if the line was changed.
     */
    /*private*/ static boolean copy_indent(int size, Bytes src)
    {
        Bytes p = null;
        Bytes line = null;
        int ind_len = 0;	// %% red.
        int line_len = 0;

        /* Round 1: compute the number of characters needed for the indent.
         * Round 2: copy the characters. */
        for (int round = 1; round <= 2; round++)
        {
            int todo = size;
            ind_len = 0;
            int ind_done = 0;
            Bytes s = src;

            /* Count/copy the usable portion of the source line. */
            while (0 < todo && vim_iswhite(s.at(0)))
            {
                if (s.at(0) == TAB)
                {
                    int tab_pad = (int)curbuf.b_p_ts[0] - (ind_done % (int)curbuf.b_p_ts[0]);
                    /* Stop if this tab will overshoot the target. */
                    if (todo < tab_pad)
                        break;
                    todo -= tab_pad;
                    ind_done += tab_pad;
                }
                else
                {
                    --todo;
                    ind_done++;
                }
                ind_len++;
                if (p != null)
                    (p = p.plus(1)).be(-1, s.at(0));
                s = s.plus(1);
            }

            /* Fill to next tabstop with a tab, if possible. */
            int tab_pad = (int)curbuf.b_p_ts[0] - (ind_done % (int)curbuf.b_p_ts[0]);
            if (tab_pad <= todo && !curbuf.b_p_et[0])
            {
                todo -= tab_pad;
                ind_len++;
                if (p != null)
                    (p = p.plus(1)).be(-1, TAB);
            }

            /* Add tabs required for indent. */
            while ((int)curbuf.b_p_ts[0] <= todo && !curbuf.b_p_et[0])
            {
                todo -= (int)curbuf.b_p_ts[0];
                ind_len++;
                if (p != null)
                    (p = p.plus(1)).be(-1, TAB);
            }

            /* Count/add spaces required for indent. */
            while (0 < todo)
            {
                --todo;
                ind_len++;
                if (p != null)
                    (p = p.plus(1)).be(-1, (byte)' ');
            }

            if (p == null)
            {
                /* Allocate memory for the result:
                 * the copied indent, new indent and the rest of the line. */
                line_len = strlen(ml_get_curline()) + 1;
                line = new Bytes(ind_len + line_len);
                p = line;
            }
        }

        /* Append the original line. */
        BCOPY(p, ml_get_curline(), line_len);

        /* Replace the line. */
        ml_replace(curwin.w_cursor.lnum, line, false);

        /* Put the cursor after the indent. */
        curwin.w_cursor.col = ind_len;
        return true;
    }

    /*
     * Return the indent of the current line after a number.  Return -1 if no
     * number was found.  Used for 'n' in 'formatoptions': numbered list.
     * Since a pattern is used it can actually handle more than numbers.
     */
    /*private*/ static int get_number_indent(long lnum)
    {
        if (curbuf.b_ml.ml_line_count < lnum)
            return -1;

        pos_C pos = new pos_C();
        pos.lnum = 0;

        /* In format_lines() (i.e. not insert mode), fo+=q is needed too... */
        int lead_len = 0;       /* length of comment leader */
        if ((State & INSERT) != 0 || has_format_option(FO_Q_COMS))
            lead_len = get_leader_len(ml_get(lnum), null, false, true);

        regmatch_C regmatch = new regmatch_C();
        regmatch.regprog = vim_regcomp(curbuf.b_p_flp[0], RE_MAGIC);
        if (regmatch.regprog != null)
        {
            regmatch.rm_ic = false;

            /* vim_regexec() expects a pointer to a line.
             * This lets us start matching for the "flp" beyond any comment leader... */
            if (vim_regexec(regmatch, ml_get(lnum).plus(lead_len), 0))
            {
                pos.lnum = lnum;
                pos.col = BDIFF(regmatch.endp[0], ml_get(lnum));
                pos.coladd = 0;
            }
        }

        if (pos.lnum == 0 || ml_get_pos(pos).at(0) == NUL)
            return -1;

        int[] col = new int[1];
        getvcol(curwin, pos, col, null, null);
        return col[0];
    }

    /*private*/ static int      bri_prev_indent;    /* cached indent value */
    /*private*/ static long     bri_prev_ts;        /* cached tabstop value */
    /*private*/ static Bytes    bri_prev_line;      /* cached pointer to line */
    /*private*/ static int      bri_prev_tick;      /* changedtick of cached value */

    /*
     * Return appropriate space number for breakindent, taking influencing
     * parameters into account.  Window must be specified, since it is not
     * necessarily always the current one.
     */
    /*private*/ static int get_breakindent_win(window_C wp, Bytes line)
        /* line: start of the line */
    {
        int bri = 0;
        /* window width minus window margin space, i.e. what rests for text */
        int eff_wwidth = wp.w_width - (((wp.w_onebuf_opt.wo_nu[0] || wp.w_onebuf_opt.wo_rnu[0]) && vim_strbyte(p_cpo[0], CPO_NUMCOL) == null) ? number_width(wp) + 1 : 0);

        /* used cached indent, unless pointer or 'tabstop' changed */
        if (BNE(bri_prev_line, line) || bri_prev_ts != wp.w_buffer.b_p_ts[0] || bri_prev_tick != wp.w_buffer.b_changedtick)
        {
            bri_prev_line = line;
            bri_prev_ts = wp.w_buffer.b_p_ts[0];
            bri_prev_tick = wp.w_buffer.b_changedtick;
            bri_prev_indent = get_indent_str(line, (int)wp.w_buffer.b_p_ts[0], wp.w_onebuf_opt.wo_list[0]);
        }
        bri = bri_prev_indent + wp.w_p_brishift;

        /* indent minus the length of the showbreak string */
        if (wp.w_p_brisbr)
            bri -= mb_string2cells(p_sbr[0], -1);

        /* add offset for number column, if 'n' is in 'cpoptions' */
        bri += win_col_off2(wp);

        /* never indent past left window margin */
        if (bri < 0)
            bri = 0;
        /* always leave at least bri_min characters on the left, if text width is sufficient */
        else if (eff_wwidth - wp.w_p_brimin < bri)
            bri = (eff_wwidth - wp.w_p_brimin < 0) ? 0 : eff_wwidth - wp.w_p_brimin;

        return bri;
    }

    /*
     * Return true if the string "line" starts with a word from 'cinwords'.
     */
    /*private*/ static boolean cin_is_cinword(Bytes line)
    {
        boolean retval = false;

        int cinw_len = strlen(curbuf.b_p_cinw[0]) + 1;
        Bytes cinw_buf = new Bytes(cinw_len);

        line = skipwhite(line);

        for (Bytes[] cinw = { curbuf.b_p_cinw[0] }; cinw[0].at(0) != NUL; )
        {
            int len = copy_option_part(cinw, cinw_buf, cinw_len, u8(","));
            if (STRNCMP(line, cinw_buf, len) == 0
                    && (!us_iswordb(line.at(len), curbuf) || !us_iswordb(line.at(len - 1), curbuf)))
            {
                retval = true;
                break;
            }
        }

        return retval;
    }

    /*
     * open_line: Add a new line below or above the current line.
     *
     * For VREPLACE mode, we only add a new line when we get to the end of the
     * file, otherwise we just start replacing the next line.
     *
     * Caller must take care of undo.  Since VREPLACE may affect any number of
     * lines however, it may call u_save_cursor() again when starting to change a new line.
     * "flags": OPENLINE_DELSPACES  delete spaces after cursor
     *          OPENLINE_DO_COM     format comments
     *          OPENLINE_KEEPTRAIL  keep trailing spaces
     *          OPENLINE_MARKFIX    adjust mark positions after the line break
     *          OPENLINE_COM_LIST   format comments with list or 2nd line indent
     *
     * "second_line_indent": indent for after ^^D in Insert mode or if flag
     *                        OPENLINE_COM_LIST
     *
     * Return true for success, false for failure
     */
    /*private*/ static boolean open_line(int dir, int flags, int second_line_indent)
        /* dir: FORWARD or BACKWARD */
    {
        boolean retval = false;             /* return value, default is FAIL */

        boolean do_si = (!p_paste[0] && curbuf.b_p_si[0] && !curbuf.b_p_cin[0]);
        boolean no_si = false;              /* reset did_si afterwards */
        boolean saved_pi = curbuf.b_p_pi[0];       /* copy of preserveindent setting */

        /*
         * make a copy of the current line so we can mess with it
         */
        Bytes saved_line = STRDUP(ml_get_curline());
        Bytes next_line = null;            /* copy of the next line */

        if ((State & VREPLACE_FLAG) != 0)
        {
            /*
             * With VREPLACE we make a copy of the next line, which we will be starting to replace.
             * First make the new line empty and let vim play with the indenting and comment leader
             * to its heart's content.  Then we grab what it ended up putting on the new line, put
             * back the original line, and call ins_char() to put each new character onto the line,
             * replacing what was there before and pushing the right stuff onto the replace stack.
             */
            if (curwin.w_cursor.lnum < orig_line_count)
                next_line = STRDUP(ml_get(curwin.w_cursor.lnum + 1));
            else
                next_line = STRDUP(u8(""));

            /*
             * In VREPLACE mode, a NL replaces the rest of the line, and starts replacing the next line,
             * so push all of the characters left on the line onto the replace stack.  We'll push any other
             * characters that might be replaced at the start of the next line (due to autoindent, etc.)
             * a bit later.
             */
            replace_push(NUL);      /* call twice because BS over NL expects it */
            replace_push(NUL);
            for (Bytes s = saved_line.plus(curwin.w_cursor.col); s.at(0) != NUL; )
                s = s.plus(replace_push_mb(s));
            saved_line.be(curwin.w_cursor.col, NUL);
        }

        Bytes p_extra = null;              /* what goes to next line */
        int first_char = NUL;
        int extra_len = 0;                  /* length of "p_extra" string */
        int saved_char = NUL;

        if ((State & INSERT) != 0 && (State & VREPLACE_FLAG) == 0)
        {
            p_extra = saved_line.plus(curwin.w_cursor.col);
            if (do_si)              /* need first char after new line break */
            {
                Bytes p = skipwhite(p_extra);
                first_char = p.at(0);
            }
            extra_len = strlen(p_extra);
            saved_char = p_extra.at(0);
            p_extra.be(0, NUL);
        }

        u_clearline();              /* cannot do "U" command when adding lines */
        did_si = false;
        ai_col = 0;

        /*
         * If we just did an auto-indent, then we didn't type anything on the
         * prior line, and it should be truncated.  Do this even if 'ai' is not set
         * because automatically inserting a comment leader also sets did_ai.
         */
        boolean trunc_line = false;         /* truncate current line afterwards */
        if (dir == FORWARD && did_ai)
            trunc_line = true;

        pos_C old_cursor = new pos_C();     /* old cursor position */

        int newindent = 0;                  /* auto-indent of the new line */

        /*
         * If 'autoindent' and/or 'smartindent' is set, try to figure out what
         * indent to use for the new line.
         */
        if (curbuf.b_p_ai[0] || do_si)
        {
            /*
             * count white space on current line
             */
            newindent = get_indent_str(saved_line, (int)curbuf.b_p_ts[0], false);
            if (newindent == 0 && (flags & OPENLINE_COM_LIST) == 0)
                newindent = second_line_indent; /* for ^^D command in insert mode */

            /*
             * Do smart indenting.
             * In insert/replace mode (only when dir == FORWARD)
             * we may move some text to the next line.  If it starts with '{'
             * don't add an indent.  Fixes inserting a NL before '{' in line
             *      "if (condition) {"
             */
            if (!trunc_line && do_si && saved_line.at(0) != NUL && (p_extra == null || first_char != '{'))
            {
                COPY_pos(old_cursor, curwin.w_cursor);

                int lead_len;                   /* length of comment leader */
                Bytes s = saved_line;
                if ((flags & OPENLINE_DO_COM) != 0)
                    lead_len = get_leader_len(s, null, false, true);
                else
                    lead_len = 0;

                if (dir == FORWARD)
                {
                    /*
                     * Skip preprocessor directives, unless they are recognised as comments.
                     */
                    if (lead_len == 0 && s.at(0) == (byte)'#')
                    {
                        while (s.at(0) == (byte)'#' && 1 < curwin.w_cursor.lnum)
                            s = ml_get(--curwin.w_cursor.lnum);
                        newindent = get_indent();
                    }
                    if ((flags & OPENLINE_DO_COM) != 0)
                        lead_len = get_leader_len(s, null, false, true);
                    else
                        lead_len = 0;
                    if (0 < lead_len)
                    {
                        /*
                         * This case gets the following right:
                         *      \*
                         *       * A comment (read '\' as '/').
                         *       *\
                         * #define IN_THE_WAY
                         *      This should line up here;
                         */
                        Bytes p = skipwhite(s);
                        if (p.at(0) == (byte)'/' && p.at(1) == (byte)'*')
                            p = p.plus(1);
                        if (p.at(0) == (byte)'*')
                        {
                            for (p = p.plus(1); p.at(0) != NUL; p = p.plus(1))
                            {
                                if (p.at(0) == (byte)'/' && p.at(-1) == (byte)'*')
                                {
                                    /*
                                     * End of C comment, indent should line up with
                                     * the line containing the start of the comment.
                                     */
                                    curwin.w_cursor.col = BDIFF(p, s);
                                    pos_C pos = findmatch(null, NUL);
                                    if (pos != null)
                                    {
                                        curwin.w_cursor.lnum = pos.lnum;
                                        newindent = get_indent();
                                    }
                                }
                            }
                        }
                    }
                    else    /* Not a comment line. */
                    {
                        /* Find last non-blank in line. */
                        Bytes p = s.plus(strlen(s) - 1);
                        while (BLT(s, p) && vim_iswhite(p.at(0)))
                            p = p.minus(1);
                        byte last_char = p.at(0);

                        /*
                         * find the character just before the '{' or ';'
                         */
                        if (last_char == '{' || last_char == ';')
                        {
                            if (BLT(s, p))
                                p = p.minus(1);
                            while (BLT(s, p) && vim_iswhite(p.at(0)))
                                p = p.minus(1);
                        }
                        /*
                         * Try to catch lines that are split over multiple
                         * lines.  eg:
                         *      if (condition &&
                         *                  condition) {
                         *          Should line up here!
                         *      }
                         */
                        if (p.at(0) == (byte)')')
                        {
                            curwin.w_cursor.col = BDIFF(p, s);
                            pos_C pos = findmatch(null, '(');
                            if (pos != null)
                            {
                                curwin.w_cursor.lnum = pos.lnum;
                                newindent = get_indent();
                                s = ml_get_curline();
                            }
                        }
                        /*
                         * If last character is '{' do indent, without checking for "if" and the like.
                         */
                        if (last_char == '{')
                        {
                            did_si = true;  /* do indent */
                            no_si = true;   /* don't delete it when '{' typed */
                        }
                        /*
                         * Look for "if" and the like, use 'cinwords'.
                         * Don't do this if the previous line ended in ';' or '}'.
                         */
                        else if (last_char != ';' && last_char != '}' && cin_is_cinword(s))
                            did_si = true;
                    }
                }
                else /* dir == BACKWARD */
                {
                    /*
                     * Skip preprocessor directives, unless they are recognised as comments.
                     */
                    if (lead_len == 0 && s.at(0) == (byte)'#')
                    {
                        boolean was_backslashed = false;

                        while ((s.at(0) == (byte)'#' || was_backslashed)
                            && curwin.w_cursor.lnum < curbuf.b_ml.ml_line_count)
                        {
                            was_backslashed = (s.at(0) != NUL && s.at(strlen(s) - 1) == '\\');
                            s = ml_get(++curwin.w_cursor.lnum);
                        }
                        if (was_backslashed)
                            newindent = 0;              /* got to end of file */
                        else
                            newindent = get_indent();
                    }
                    Bytes p = skipwhite(s);
                    if (p.at(0) == (byte)'}')                      /* if line starts with '}': do indent */
                        did_si = true;
                    else                                /* can delete indent when '{' typed */
                        can_si_back = true;
                }

                COPY_pos(curwin.w_cursor, old_cursor);
            }
            if (do_si)
                can_si = true;

            did_ai = true;
        }

        Bytes allocated = null;            /* allocated memory */

        int lead_len;                       /* length of comment leader */
        Bytes[] lead_flags = new Bytes[1];                  /* position in 'comments' for comment leader */
        Bytes leader = null;               /* copy of comment leader */

        int newcol = 0;                     /* new cursor column */

        /*
         * Find out if the current line starts with a comment leader.
         * This may then be inserted in front of the new line.
         */
        end_comment_pending = NUL;
        if ((flags & OPENLINE_DO_COM) != 0)
            lead_len = get_leader_len(saved_line, lead_flags, dir == BACKWARD, true);
        else
            lead_len = 0;
        if (0 < lead_len)
        {
            Bytes lead_repl = null;                    /* replaces comment leader */
            int lead_repl_len = 0;                      /* length of *lead_repl */
            Bytes lead_middle = new Bytes(COM_MAX_LEN); /* middle-comment string */
            Bytes lead_end = new Bytes(COM_MAX_LEN);    /* end-comment string */
            Bytes comment_end = null;                  /* where lead_end has been found */
            boolean extra_space = false;                /* append extra space */
            boolean require_blank = false;              /* requires blank after middle */

            /*
             * If the comment leader has the start, middle or end flag,
             * it may not be used or may be replaced with the middle leader.
             */
            for (Bytes[] p = { lead_flags[0] }; p[0].at(0) != NUL && p[0].at(0) != (byte)':'; p[0] = p[0].plus(1))
            {
                if (p[0].at(0) == COM_BLANK)
                {
                    require_blank = true;
                    continue;
                }
                if (p[0].at(0) == COM_START || p[0].at(0) == COM_MIDDLE)
                {
                    byte current_flag = p[0].at(0);
                    if (p[0].at(0) == COM_START)
                    {
                        /*
                         * Doing "O" on a start of comment does not insert leader.
                         */
                        if (dir == BACKWARD)
                        {
                            lead_len = 0;
                            break;
                        }

                        /* find start of middle part */
                        copy_option_part(p, lead_middle, COM_MAX_LEN, u8(","));
                        require_blank = false;
                    }

                    /*
                     * Isolate the strings of the middle and end leader.
                     */
                    while (p[0].at(0) != NUL && p[0].at(-1) != (byte)':')          /* find end of middle flags */
                    {
                        if (p[0].at(0) == COM_BLANK)
                            require_blank = true;
                        p[0] = p[0].plus(1);
                    }
                    copy_option_part(p, lead_middle, COM_MAX_LEN, u8(","));

                    while (p[0].at(0) != NUL && p[0].at(-1) != (byte)':')          /* find end of end flags */
                    {
                        /* Check whether we allow automatic ending of comments. */
                        if (p[0].at(0) == COM_AUTO_END)
                            end_comment_pending = -1;   /* means we want to set it */
                        p[0] = p[0].plus(1);
                    }
                    int n = copy_option_part(p, lead_end, COM_MAX_LEN, u8(","));

                    if (end_comment_pending == -1)      /* we can set it now */
                        end_comment_pending = lead_end.at(n - 1);

                    /*
                     * If the end of the comment is in the same line, don't use the comment leader.
                     */
                    if (dir == FORWARD)
                    {
                        for (p[0] = saved_line.plus(lead_len); p[0].at(0) != NUL; p[0] = p[0].plus(1))
                            if (STRNCMP(p[0], lead_end, n) == 0)
                            {
                                comment_end = p[0];
                                lead_len = 0;
                                break;
                            }
                    }

                    /*
                     * Doing "o" on a start of comment inserts the middle leader.
                     */
                    if (0 < lead_len)
                    {
                        if (current_flag == COM_START)
                        {
                            lead_repl = lead_middle;
                            lead_repl_len = strlen(lead_middle);
                        }

                        /*
                         * If we have hit RETURN immediately after the start comment leader,
                         * then put a space after the middle comment leader on the next line.
                         */
                        if (!vim_iswhite(saved_line.at(lead_len - 1))
                                && ((p_extra != null && curwin.w_cursor.col == lead_len)
                                    || (p_extra == null && saved_line.at(lead_len) == NUL)
                                    || require_blank))
                            extra_space = true;
                    }
                    break;
                }
                if (p[0].at(0) == COM_END)
                {
                    /*
                     * Doing "o" on the end of a comment does not insert leader.
                     * Remember where the end is, might want to use it to find the
                     * start (for C-comments).
                     */
                    if (dir == FORWARD)
                    {
                        comment_end = skipwhite(saved_line);
                        lead_len = 0;
                        break;
                    }

                    /*
                     * Doing "O" on the end of a comment inserts the middle leader.
                     * Find the string for the middle leader, searching backwards.
                     */
                    while (BLT(curbuf.b_p_com[0], p[0]) && p[0].at(0) != (byte)',')
                        p[0] = p[0].minus(1);
                    for (lead_repl = p[0]; BLT(curbuf.b_p_com[0], lead_repl) && lead_repl.at(-1) != (byte)':'; lead_repl = lead_repl.minus(1))
                        ;
                    lead_repl_len = BDIFF(p[0], lead_repl);

                    /* We can probably always add an extra space when doing "O" on the comment-end. */
                    extra_space = true;

                    /* Check whether we allow automatic ending of comments. */
                    Bytes p2;
                    for (p2 = p[0]; p2.at(0) != NUL && p2.at(0) != (byte)':'; p2 = p2.plus(1))
                    {
                        if (p2.at(0) == COM_AUTO_END)
                            end_comment_pending = -1;   /* means we want to set it */
                    }
                    if (end_comment_pending == -1)
                    {
                        /* Find last character in end-comment string. */
                        while (p2.at(0) != NUL && p2.at(0) != (byte)',')
                            p2 = p2.plus(1);
                        end_comment_pending = p2.at(-1);
                    }
                    break;
                }
                if (p[0].at(0) == COM_FIRST)
                {
                    /*
                     * Comment leader for first line only:
                     * don't repeat leader when using "O",
                     * blank out leader when using "o".
                     */
                    if (dir == BACKWARD)
                        lead_len = 0;
                    else
                    {
                        lead_repl = u8("");
                        lead_repl_len = 0;
                    }
                    break;
                }
            }

            if (0 < lead_len)
            {
                /* allocate buffer (may concatenate "p_extra" later) */
                leader = new Bytes(lead_len + lead_repl_len + (extra_space ? 1 : 0) + extra_len + (0 < second_line_indent ? second_line_indent : 0) + 1);
                allocated = leader;             /* remember to free it later */

                if (leader == null)
                    lead_len = 0;
                else
                {
                    vim_strncpy(leader, saved_line, lead_len);

                    /*
                     * Replace leader with "lead_repl", right or left adjusted.
                     */
                    if (lead_repl != null)
                    {
                        int c = 0;
                        int off = 0;

                        for (Bytes p = lead_flags[0]; p.at(0) != NUL && p.at(0) != (byte)':'; )
                        {
                            if (p.at(0) == COM_RIGHT || p.at(0) == COM_LEFT)
                                c = (p = p.plus(1)).at(-1);
                            else if (asc_isdigit(p.at(0)) || p.at(0) == (byte)'-')
                            {
                                Bytes[] __ = { p }; off = (int)getdigits(__); p = __[0];
                            }
                            else
                                p = p.plus(1);
                        }
                        if (c == COM_RIGHT)     /* right adjusted leader */
                        {
                            /* find last non-white in the leader to line up with */
                            Bytes p;
                            for (p = leader.plus(lead_len - 1); BLT(leader, p) && vim_iswhite(p.at(0)); p = p.minus(1))
                                ;
                            p = p.plus(1);

                            /* Compute the length of the replaced characters in
                             * screen characters, not bytes. */
                            {
                                int repl_size = mb_string2cells(lead_repl, lead_repl_len);
                                int old_size = 0;
                                Bytes endp = p;

                                while (old_size < repl_size && BLT(leader, p))
                                {
                                    p = p.minus(us_ptr_back(leader, p));
                                    old_size += mb_ptr2cells(p);
                                }
                                int l = lead_repl_len - BDIFF(endp, p);
                                if (l != 0)
                                    BCOPY(endp, l, endp, 0, BDIFF(leader, endp) + lead_len);
                                lead_len += l;
                            }
                            BCOPY(p, lead_repl, lead_repl_len);
                            if (BLT(leader.plus(lead_len), p.plus(lead_repl_len)))
                                p.be(lead_repl_len, NUL);

                            /* Blank-out any other chars from the old leader. */
                            for (p = p.minus(1); BLE(leader, p); p = p.minus(1))
                            {
                                int l = us_head_off(leader, p);

                                if (1 < l)
                                {
                                    p = p.minus(l);
                                    if (1 < mb_ptr2cells(p))
                                    {
                                        p.be(1, (byte)' ');
                                        --l;
                                    }
                                    Bytes pl = p.plus(l);
                                    BCOPY(p, 1, pl, 1, BDIFF(leader.plus(lead_len), pl.plus(1)));
                                    lead_len -= l;
                                    p.be(0, (byte)' ');
                                }
                                else if (!vim_iswhite(p.at(0)))
                                    p.be(0, (byte)' ');
                            }
                        }
                        else                    /* left adjusted leader */
                        {
                            Bytes p = skipwhite(leader);
                            /* Compute the length of the replaced characters in screen characters,
                             * not bytes.  Move the part that is not to be overwritten. */
                            {
                                int repl_size = mb_string2cells(lead_repl, lead_repl_len);

                                int i, l;
                                for (i = 0; p.at(i) != NUL && i < lead_len; i += l)
                                {
                                    l = us_ptr2len_cc(p.plus(i));
                                    if (repl_size < mb_string2cells(p, i + l))
                                        break;
                                }
                                if (i != lead_repl_len)
                                {
                                    BCOPY(p, lead_repl_len, p, i, BDIFF(leader.plus(lead_len), p.plus(i)));
                                    lead_len += lead_repl_len - i;
                                }
                            }
                            BCOPY(p, lead_repl, lead_repl_len);

                            /* Replace any remaining non-white chars in the old leader by spaces.
                             * Keep Tabs, the indent must remain the same. */
                            for (p = p.plus(lead_repl_len); BLT(p, leader.plus(lead_len)); p = p.plus(1))
                                if (!vim_iswhite(p.at(0)))
                                {
                                    /* Don't put a space before a TAB. */
                                    if (BLT(p.plus(1), leader.plus(lead_len)) && p.at(1) == TAB)
                                    {
                                        --lead_len;
                                        BCOPY(p, 0, p, 1, BDIFF(leader.plus(lead_len), p));
                                    }
                                    else
                                    {
                                        int l = us_ptr2len_cc(p);

                                        if (1 < l)
                                        {
                                            if (1 < mb_ptr2cells(p))
                                            {
                                                /* Replace a double-wide char with two spaces. */
                                                --l;
                                                (p = p.plus(1)).be(-1, (byte)' ');
                                            }
                                            BCOPY(p, 1, p, l, BDIFF(leader.plus(lead_len), p));
                                            lead_len -= l - 1;
                                        }
                                        p.be(0, (byte)' ');
                                    }
                                }
                            p.be(0, NUL);
                        }

                        /* Recompute the indent, it may have changed. */
                        if (curbuf.b_p_ai[0] || do_si)
                            newindent = get_indent_str(leader, (int)curbuf.b_p_ts[0], false);

                        /* Add the indent offset. */
                        if (newindent + off < 0)
                        {
                            off = -newindent;
                            newindent = 0;
                        }
                        else
                            newindent += off;

                        /* Correct trailing spaces for the shift,
                         * so that alignment remains equal. */
                        while (0 < off && 0 < lead_len && leader.at(lead_len - 1) == (byte)' ')
                        {
                            /* Don't do it when there is a tab before the space. */
                            if (vim_strchr(skipwhite(leader), '\t') != null)
                                break;
                            --lead_len;
                            --off;
                        }

                        /* If the leader ends in white space, don't add an extra space. */
                        if (0 < lead_len && vim_iswhite(leader.at(lead_len - 1)))
                            extra_space = false;
                        leader.be(lead_len, NUL);
                    }

                    if (extra_space)
                    {
                        leader.be(lead_len++, (byte)' ');
                        leader.be(lead_len, NUL);
                    }

                    newcol = lead_len;

                    /* If a new indent will be set below,
                     * remove the indent that is in the comment leader.
                     */
                    if (newindent != 0 || did_si)
                    {
                        while (0 < lead_len && vim_iswhite(leader.at(0)))
                        {
                            --lead_len;
                            --newcol;
                            leader = leader.plus(1);
                        }
                    }
                }
                did_si = can_si = false;
            }
            else if (comment_end != null)
            {
                /* We have finished a comment, so we don't use the leader.
                 * If this was a C-comment and 'ai' or 'si' is set do a normal
                 * indent to align with the line containing the start of the comment.
                 */
                if (comment_end.at(0) == (byte)'*' && comment_end.at(1) == (byte)'/' && (curbuf.b_p_ai[0] || do_si))
                {
                    COPY_pos(old_cursor, curwin.w_cursor);
                    curwin.w_cursor.col = BDIFF(comment_end, saved_line);
                    pos_C pos = findmatch(null, NUL);
                    if (pos != null)
                    {
                        curwin.w_cursor.lnum = pos.lnum;
                        newindent = get_indent();
                    }
                    COPY_pos(curwin.w_cursor, old_cursor);
                }
            }
        }

        int less_cols_off = 0;              /* columns to skip for mark adjust */
        int less_cols = 0;                  /* less columns for mark in new line */

        /* (State == INSERT || State == REPLACE), only when dir == FORWARD */
        if (p_extra != null)
        {
            p_extra.be(0, saved_char);          /* restore char that NUL replaced */

            /*
             * When 'ai' set or "flags" has OPENLINE_DELSPACES, skip to the first non-blank.
             *
             * When in REPLACE mode, put the deleted blanks on the replace stack,
             * preceded by a NUL, so they can be put back when a BS is entered.
             */
            if ((State & REPLACE_FLAG) != 0 && (State & VREPLACE_FLAG) == 0)
                replace_push(NUL);      /* end of extra blanks */
            if (curbuf.b_p_ai[0] || (flags & OPENLINE_DELSPACES) != 0)
            {
                while ((p_extra.at(0) == (byte)' ' || p_extra.at(0) == (byte)'\t') && !utf_iscomposing(us_ptr2char(p_extra.plus(1))))
                {
                    if ((State & REPLACE_FLAG) != 0 && (State & VREPLACE_FLAG) == 0)
                        replace_push(p_extra.at(0));
                    p_extra = p_extra.plus(1);
                    less_cols_off++;
                }
            }
            if (p_extra.at(0) != NUL)
                did_ai = false;             /* append some text, don't truncate now */

            /* columns for marks adjusted for removed columns */
            less_cols = BDIFF(p_extra, saved_line);
        }

        if (p_extra == null)
            p_extra = u8("");                   /* append empty line */

        /* concatenate leader and "p_extra", if there is a leader */
        if (0 < lead_len)
        {
            if ((flags & OPENLINE_COM_LIST) != 0 && 0 < second_line_indent)
            {
                int i;
                int padding = second_line_indent - (newindent + strlen(leader));

                /* Here whitespace is inserted after the comment char.
                 * Below, set_indent(newindent, SIN_INSERT) will insert
                 * the whitespace needed before the comment char. */
                for (i = 0; i < padding; i++)
                {
                    STRCAT(leader, u8(" "));
                    less_cols--;
                    newcol++;
                }
            }
            STRCAT(leader, p_extra);
            p_extra = leader;
            did_ai = true;                  /* so truncating blanks works with comments */
            less_cols -= lead_len;
        }
        else
            end_comment_pending = NUL;      /* turns out there was no leader */

        COPY_pos(old_cursor, curwin.w_cursor);
        if (dir == BACKWARD)
            --curwin.w_cursor.lnum;

        theend:
        {
            boolean did_append;
            if ((State & VREPLACE_FLAG) == 0 || orig_line_count <= old_cursor.lnum)
            {
                if (!ml_append(curwin.w_cursor.lnum, p_extra, 0, false))
                    break theend;
                /* Postpone calling changed_lines(), because it would mess up folding with markers. */
                mark_adjust(curwin.w_cursor.lnum + 1, MAXLNUM, 1L, 0L);
                did_append = true;
            }
            else
            {
                /*
                 * In VREPLACE mode we are starting to replace the next line.
                 */
                curwin.w_cursor.lnum++;
                if (insStart.lnum + vr_lines_changed <= curwin.w_cursor.lnum)
                {
                    /* In case we NL to a new line, BS to the previous one, and NL
                     * again, we don't want to save the new line for undo twice.
                     */
                    u_save_cursor();            /* errors are ignored! */
                    vr_lines_changed++;
                }
                ml_replace(curwin.w_cursor.lnum, p_extra, true);
                changed_bytes(curwin.w_cursor.lnum, 0);
                --curwin.w_cursor.lnum;
                did_append = false;
            }

            if (newindent != 0 || did_si)
            {
                curwin.w_cursor.lnum++;
                if (did_si)
                {
                    int sw = (int)get_sw_value(curbuf);

                    if (p_sr[0])
                        newindent -= newindent % sw;
                    newindent += sw;
                }
                /* Copy the indent. */
                if (curbuf.b_p_ci[0])
                {
                    copy_indent(newindent, saved_line);

                    /*
                     * Set the 'preserveindent' option so that any further screwing
                     * with the line doesn't entirely destroy our efforts to preserve it.
                     * It gets restored at the function end.
                     */
                    curbuf.b_p_pi[0] = true;
                }
                else
                    set_indent(newindent, SIN_INSERT);
                less_cols -= curwin.w_cursor.col;

                ai_col = curwin.w_cursor.col;

                /*
                 * In REPLACE mode, for each character in the new indent, there must
                 * be a NUL on the replace stack, for when it is deleted with BS.
                 */
                if ((State & REPLACE_FLAG) != 0 && (State & VREPLACE_FLAG) == 0)
                    for (int n = 0; n < curwin.w_cursor.col; n++)
                        replace_push(NUL);
                newcol += curwin.w_cursor.col;
                if (no_si)
                    did_si = false;
            }

            /*
             * In REPLACE mode, for each character in the extra leader, there must
             * be a NUL on the replace stack, for when it is deleted with BS.
             */
            if ((State & REPLACE_FLAG) != 0 && (State & VREPLACE_FLAG) == 0)
                while (0 < lead_len--)
                    replace_push(NUL);

            COPY_pos(curwin.w_cursor, old_cursor);

            if (dir == FORWARD)
            {
                if (trunc_line || (State & INSERT) != 0)
                {
                    /* truncate current line at cursor */
                    saved_line.be(curwin.w_cursor.col, NUL);
                    /* Remove trailing white space, unless OPENLINE_KEEPTRAIL used. */
                    if (trunc_line && (flags & OPENLINE_KEEPTRAIL) == 0)
                        truncate_spaces(saved_line);
                    ml_replace(curwin.w_cursor.lnum, saved_line, false);
                    saved_line = null;
                    if (did_append)
                    {
                        changed_lines(curwin.w_cursor.lnum, curwin.w_cursor.col, curwin.w_cursor.lnum + 1, 1L);
                        did_append = false;

                        /* Move marks after the line break to the new line. */
                        if ((flags & OPENLINE_MARKFIX) != 0)
                            mark_col_adjust(curwin.w_cursor.lnum,
                                            curwin.w_cursor.col + less_cols_off, 1L, (long)-less_cols);
                    }
                    else
                        changed_bytes(curwin.w_cursor.lnum, curwin.w_cursor.col);
                }

                /*
                 * Put the cursor on the new line.
                 * Careful: the scrollup() above may have moved w_cursor, we must use old_cursor.
                 */
                curwin.w_cursor.lnum = old_cursor.lnum + 1;
            }
            if (did_append)
                changed_lines(curwin.w_cursor.lnum, 0, curwin.w_cursor.lnum, 1L);

            curwin.w_cursor.col = newcol;
            curwin.w_cursor.coladd = 0;

            /*
             * In VREPLACE mode, we are handling the replace stack ourselves,
             * so stop fixthisline() from doing it (via change_indent()) by
             * telling it we're in normal INSERT mode.
             */
            int vreplace_mode;
            if ((State & VREPLACE_FLAG) != 0)
            {
                vreplace_mode = State;          /* so we know to put things right later */
                State = INSERT;
            }
            else
                vreplace_mode = 0;
            /*
             * May do lisp indenting.
             */
            if (!p_paste[0] && leader == null && curbuf.b_p_lisp[0] && curbuf.b_p_ai[0])
            {
                fixthisline(get_lisp_indent);
                Bytes p = ml_get_curline();
                ai_col = BDIFF(skipwhite(p), p);
            }
            /*
             * May do indenting after opening a new line.
             */
            if (!p_paste[0]
                    && (curbuf.b_p_cin[0] || curbuf.b_p_inde[0].at(0) != NUL)
                    && in_cinkeys(dir == FORWARD
                        ? KEY_OPEN_FORW
                        : KEY_OPEN_BACK, ' ', linewhite(curwin.w_cursor.lnum)))
            {
                do_c_expr_indent();
                Bytes p = ml_get_curline();
                ai_col = BDIFF(skipwhite(p), p);
            }
            if (vreplace_mode != 0)
                State = vreplace_mode;

            /*
             * Finally, VREPLACE gets the stuff on the new line, then puts back the
             * original line, and inserts the new stuff char by char, pushing old stuff
             * onto the replace stack (via ins_char()).
             */
            if ((State & VREPLACE_FLAG) != 0)
            {
                /* Put new line in "p_extra". */
                p_extra = STRDUP(ml_get_curline());

                /* Put back original line. */
                ml_replace(curwin.w_cursor.lnum, next_line, false);

                /* Insert new stuff into line again. */
                curwin.w_cursor.col = 0;
                curwin.w_cursor.coladd = 0;
                ins_bytes(p_extra);             /* will call changed_bytes() */
                next_line = null;
            }

            retval = true;              /* success! */
        }

        curbuf.b_p_pi[0] = saved_pi;
        return retval;
    }

    /*
     * get_leader_len() returns the length in bytes of the prefix of the given string,
     * which introduces a comment.  If this string is not a comment then 0 is returned.
     * When "flags" is not null, it is set to point to the flags of the recognized comment leader.
     * "backward" must be true for the "O" command.
     * If "include_space" is set, include trailing whitespace while calculating the length.
     */
    /*private*/ static int get_leader_len(Bytes line, Bytes[] flags, boolean backward, boolean include_space)
    {
        int length = 0;

        int i = length;
        while (vim_iswhite(line.at(i)))                /* leading white space is ignored */
            i++;

        Bytes part_buf = new Bytes(COM_MAX_LEN);    /* buffer for one option part */

        boolean got_com = false;
        int middle_match_len = 0;
        Bytes saved_flags = null;

        /*
         * Repeat to match several nested comment strings.
         */
        while (line.at(i) != NUL)
        {
            /*
             * Scan through the 'comments' option for a match.
             */
            boolean found_one = false;
            for (Bytes[] p = { curbuf.b_p_com[0] }; p[0].at(0) != NUL; )
            {
                if (!got_com && flags != null)
                    flags[0] = p[0];             /* remember where flags started */
                Bytes q = p[0];
                copy_option_part(p, part_buf, COM_MAX_LEN, u8(","));

                Bytes s = vim_strchr(part_buf, ':');
                if (s == null)              /* missing ':', ignore this part */
                    continue;
                (s = s.plus(1)).be(-1, NUL);                 /* isolate flags from string */

                /* If we found a middle match previously, use that match when this is not a middle or end. */
                if (middle_match_len != 0
                        && vim_strchr(part_buf, COM_MIDDLE) == null
                        && vim_strchr(part_buf, COM_END) == null)
                    break;

                /* When we already found a nested comment, only accept further nested comments. */
                if (got_com && vim_strchr(part_buf, COM_NEST) == null)
                    continue;

                /* When 'O' flag present and using "O" command skip this one. */
                if (backward && vim_strchr(part_buf, COM_NOBACK) != null)
                    continue;

                /* Line contents and string must match.
                 * When "s" starts with white space, must have some white space
                 * (but the amount does not need to match, there might be a mix of TABs and spaces). */
                if (vim_iswhite(s.at(0)))
                {
                    if (i == 0 || !vim_iswhite(line.at(i - 1)))
                        continue;           /* missing white space */
                    while (vim_iswhite(s.at(0)))
                        s = s.plus(1);
                }
                int j;
                for (j = 0; s.at(j) != NUL && s.at(j) == line.at(i + j); j++)
                    ;
                if (s.at(j) != NUL)
                    continue;               /* string doesn't match */

                /* When 'b' flag used, there must be white space
                 * or an end-of-line after the string in the line. */
                if (vim_strchr(part_buf, COM_BLANK) != null && !vim_iswhite(line.at(i + j)) && line.at(i + j) != NUL)
                    continue;

                /* We have found a match, stop searching unless this is a middle
                 * comment.  The middle comment can be a substring of the end
                 * comment in which case it's better to return the length of the
                 * end comment and its flags.  Thus we keep searching with middle
                 * and end matches and use an end match if it matches better. */
                if (vim_strchr(part_buf, COM_MIDDLE) != null)
                {
                    if (middle_match_len == 0)
                    {
                        middle_match_len = j;
                        saved_flags = q;
                    }
                    continue;
                }
                if (middle_match_len != 0 && middle_match_len < j)
                    /* Use this match instead of the middle match, since it's a longer thus better match. */
                    middle_match_len = 0;

                if (middle_match_len == 0)
                    i += j;
                found_one = true;
                break;
            }

            if (middle_match_len != 0)
            {
                /* Use the previously found middle match after failing to find a match with an end. */
                if (!got_com && flags != null)
                    flags[0] = saved_flags;
                i += middle_match_len;
                found_one = true;
            }

            /* No match found, stop scanning. */
            if (!found_one)
                break;

            length = i;

            /* Include any trailing white space. */
            while (vim_iswhite(line.at(i)))
                i++;

            if (include_space)
                length = i;

            /* If this comment doesn't nest, stop here. */
            got_com = true;
            if (vim_strchr(part_buf, COM_NEST) == null)
                break;
        }

        return length;
    }

    /*
     * Return the offset at which the last comment in line starts.
     * If there is no comment in the whole line, -1 is returned.
     *
     * When "flags" is not null,
     * it is set to point to the flags describing the recognized comment leader.
     */
    /*private*/ static int get_last_leader_offset(Bytes line, Bytes[] flags)
    {
        int offset = -1;

        Bytes com_leader = null, com_flags = null;	// %% anno dunno

        /*
         * Repeat to match several nested comment strings.
         */
        for (int lower_check_bound = 0, n = strlen(line); lower_check_bound <= --n; )
        {
            boolean found_one = false;

            Bytes part_buf = new Bytes(COM_MAX_LEN);    /* buffer for one option part */

            /*
             * Scan through the 'comments' option for a match.
             */
            for (Bytes[] p = { curbuf.b_p_com[0] }; p[0].at(0) != NUL; )
            {
                Bytes flags_save = p[0];

                copy_option_part(p, part_buf, COM_MAX_LEN, u8(","));

                Bytes s = vim_strchr(part_buf, ':');
                if (s == null)      /* if everything is fine, this cannot actually happen */
                    continue;
                (s = s.plus(1)).be(-1, NUL);                 /* isolate flags from string */

                com_leader = s;

                /*
                 * Line contents and string must match.
                 * When "s" starts with white space, must have some white space
                 * (but the amount does not need to match, there might be a mix of TABs and spaces).
                 */
                if (vim_iswhite(s.at(0)))
                {
                    if (n == 0 || !vim_iswhite(line.at(n - 1)))
                        continue;
                    while (vim_iswhite(s.at(0)))
                        s = s.plus(1);
                }

                int i;
                for (i = 0; s.at(i) != NUL && s.at(i) == line.at(n + i); i++)
                    /* do nothing */;
                if (s.at(i) != NUL)
                    continue;

                /*
                 * When 'b' flag used, there must be white space
                 * or an end-of-line after the string in the line.
                 */
                if (vim_strchr(part_buf, COM_BLANK) != null && !vim_iswhite(line.at(n + i)) && line.at(n + i) != NUL)
                    continue;

                /*
                 * We have found a match, stop searching.
                 */
                found_one = true;

                if (flags != null)
                    flags[0] = flags_save;

                com_flags = flags_save;
                break;
            }

            if (found_one)
            {
                offset = n;

                /*
                 * If this comment nests, continue searching.
                 */
                if (vim_strchr(part_buf, COM_NEST) != null)
                    continue;

                lower_check_bound = n;

                /*
                 * Let's verify whether the comment leader found is a substring of other comment leaders.
                 * If it is, let's adjust the lower_check_bound so that we make sure that we have determined
                 * the comment leader correctly.
                 */
                while (vim_iswhite(com_leader.at(0)))
                    com_leader = com_leader.plus(1);
                int len1 = strlen(com_leader);

                Bytes part_buf2 = new Bytes(COM_MAX_LEN);   /* buffer for one option part */

                for (Bytes[] p = { curbuf.b_p_com[0] }; p[0].at(0) != NUL; )
                {
                    Bytes flags_save = p[0];

                    copy_option_part(p, part_buf2, COM_MAX_LEN, u8(","));

                    if (BEQ(flags_save, com_flags))
                        continue;

                    Bytes s = vim_strchr(part_buf2, ':');
                    s = s.plus(1);
                    while (vim_iswhite(s.at(0)))
                        s = s.plus(1);
                    int len2 = strlen(s);
                    if (len2 == 0)
                        continue;

                    /* Now we have to verify whether "s" ends with a substring beginning the "com_leader". */
                    for (int off = (n < len2) ? n : len2; 0 < off && len2 < off + len1; )
                    {
                        --off;
                        if (STRNCMP(s.plus(off), com_leader, len2 - off) == 0)
                        {
                            if (n - off < lower_check_bound)
                                lower_check_bound = n - off;
                        }
                    }
                }
            }
        }

        return offset;
    }

    /*
     * Return the number of window lines occupied by buffer line "lnum".
     */
    /*private*/ static int plines(long lnum)
    {
        return plines_win(curwin, lnum, true);
    }

    /*private*/ static int plines_win(window_C wp, long lnum, boolean winheight)
        /* winheight: when true limit to window height */
    {
        if (!wp.w_onebuf_opt.wo_wrap[0])
            return 1;

        if (wp.w_width == 0)
            return 1;

        int lines = plines_win_nofold(wp, lnum);
        if (winheight && wp.w_height < lines)
            return wp.w_height;

        return lines;
    }

    /*
     * Return number of window lines physical line "lnum" will occupy in window "wp".
     * Does not care about folding, 'wrap' or 'diff'.
     */
    /*private*/ static int plines_win_nofold(window_C wp, long lnum)
    {
        Bytes s = ml_get_buf(wp.w_buffer, lnum, false);
        if (s.at(0) == NUL)      /* empty line */
            return 1;

        int col = win_linetabsize(wp, s, MAXCOL);

        /*
         * If list mode is on, then the '$' at the end of the line may take up one extra column.
         */
        if (wp.w_onebuf_opt.wo_list[0] && lcs_eol[0] != NUL)
            col += 1;

        /*
         * Add column offset for 'number', 'relativenumber' and 'foldcolumn'.
         */
        int width = wp.w_width - win_col_off(wp);
        if (width <= 0)
            return 32000;

        if (col <= width)
            return 1;

        col -= width;
        width += win_col_off2(wp);
        return (col + (width - 1)) / width + 1;
    }

    /*
     * Like plines_win(), but only reports the number of physical screen lines
     * used from the start of the line to the given column number.
     */
    /*private*/ static int plines_win_col(window_C wp, long lnum, long column)
    {
        int lines = 0;

        if (!wp.w_onebuf_opt.wo_wrap[0])
            return lines + 1;

        if (wp.w_width == 0)
            return lines + 1;

        Bytes line = ml_get_buf(wp.w_buffer, lnum, false);
        Bytes s = line;

        long col = 0;
        while (s.at(0) != NUL && 0 <= --column)
        {
            col += win_lbr_chartabsize(wp, line, s, (int)col, null);
            s = s.plus(us_ptr2len_cc(s));
        }

        /*
         * If s[0] is a TAB, and the TAB is not displayed as ^I, and we're not in INSERT mode,
         * then col must be adjusted so that it represents the last screen position of the TAB.
         * This only fixes an error when the TAB wraps from one screen line to the next
         * (when 'columns' is not a multiple of 'ts').
         */
        if (s.at(0) == TAB && (State & NORMAL) != 0 && (!wp.w_onebuf_opt.wo_list[0] || lcs_tab1[0] != NUL))
            col += win_lbr_chartabsize(wp, line, s, (int)col, null) - 1;

        /*
         * Add column offset for 'number', 'relativenumber', 'foldcolumn', etc.
         */
        int width = wp.w_width - win_col_off(wp);
        if (width <= 0)
            return 9999;

        lines += 1;
        if (width < col)
            lines += (col - width) / (width + win_col_off2(wp)) + 1;
        return lines;
    }

    /*private*/ static int plines_m_win(window_C wp, long first, long last)
    {
        int count = 0;

        while (first <= last)
        {
            count += plines_win(wp, first, true);
            first++;
        }

        return count;
    }

    /*
     * Insert string "p" at the cursor position.  Stops at a NUL byte.
     * Handles Replace mode and multi-byte characters.
     */
    /*private*/ static void ins_bytes(Bytes p)
    {
        ins_bytes_len(p, strlen(p));
    }

    /*
     * Insert string "p" with length "len" at the cursor position.
     * Handles Replace mode and multi-byte characters.
     */
    /*private*/ static void ins_bytes_len(Bytes p, int len)
    {
        for (int i = 0, n; i < len; i += n)
        {
            /* avoid reading past p[len] */
            n = us_ptr2len_cc_len(p.plus(i), len - i);
            ins_char_bytes(p.plus(i), n);
        }
    }

    /*
     * Insert or replace a single character at the cursor position.
     * When in REPLACE or VREPLACE mode, replace any existing character.
     * Caller must have prepared for undo.
     * For multi-byte characters we get the whole character,
     * the caller must convert bytes to a character.
     */
    /*private*/ static void ins_char(int c)
    {
        Bytes buf = new Bytes(MB_MAXBYTES + 1);
        int n = utf_char2bytes(c, buf);

        /* When "c" is 0x100, 0x200, etc. we don't want to insert a NUL byte.
         * Happens for CTRL-Vu9900. */
        if (buf.at(0) == 0)
            buf.be(0, (byte)'\n');

        ins_char_bytes(buf, n);
    }

    /*private*/ static void ins_char_bytes(Bytes buf, int charlen)
    {
        long lnum = curwin.w_cursor.lnum;

        /* Break tabs if needed. */
        if (virtual_active() && 0 < curwin.w_cursor.coladd)
            coladvance_force(getviscol());

        int col = curwin.w_cursor.col;
        Bytes oldp = ml_get(lnum);
        int linelen = strlen(oldp) + 1;        /* length of old line including NUL */

        /* The lengths default to the values for when not replacing. */
        int oldlen = 0;                             /* nr of bytes deleted (0 when not replacing) */
        int newlen = charlen;                       /* nr of bytes inserted */

        if ((State & REPLACE_FLAG) != 0)
        {
            if ((State & VREPLACE_FLAG) != 0)
            {
                /*
                 * Disable 'list' temporarily, unless 'cpo' contains the 'L' flag.
                 * Returns the old value of list, so when finished,
                 * curwin.w_onebuf_opt.wo_list should be set back to this.
                 */
                boolean old_list = curwin.w_onebuf_opt.wo_list[0];
                if (old_list && vim_strbyte(p_cpo[0], CPO_LISTWM) == null)
                    curwin.w_onebuf_opt.wo_list[0] = false;

                /*
                 * In virtual replace mode each character may replace one or more
                 * characters (zero if it's a TAB).  Count the number of bytes to
                 * be deleted to make room for the new character, counting screen
                 * cells.  May result in adding spaces to fill a gap.
                 */
                int[] vcol = new int[1];
                getvcol(curwin, curwin.w_cursor, null, vcol, null);

                int new_vcol = vcol[0] + chartabsize(buf, vcol[0]);
                while (oldp.at(col + oldlen) != NUL && vcol[0] < new_vcol)
                {
                    vcol[0] += chartabsize(oldp.plus(col + oldlen), vcol[0]);
                    /* Don't need to remove a TAB that takes us to the right position. */
                    if (new_vcol < vcol[0] && oldp.at(col + oldlen) == TAB)
                        break;
                    oldlen += us_ptr2len_cc(oldp.plus(col + oldlen));
                    /* Deleted a bit too much, insert spaces. */
                    if (new_vcol < vcol[0])
                        newlen += vcol[0] - new_vcol;
                }

                curwin.w_onebuf_opt.wo_list[0] = old_list;
            }
            else if (oldp.at(col) != NUL)
            {
                /* normal replace */
                oldlen = us_ptr2len_cc(oldp.plus(col));
            }

            /* Push the replaced bytes onto the replace stack, so that they can be
             * put back when BS is used.  The bytes of a multi-byte character are
             * done the other way around, so that the first byte is popped off
             * first (it tells the byte length of the character). */
            replace_push(NUL);
            for (int i = 0; i < oldlen; i++)
                i += replace_push_mb(oldp.plus(col + i)) - 1;
        }

        Bytes newp = new Bytes(linelen + newlen - oldlen);

        /* Copy bytes before the cursor. */
        if (0 < col)
            BCOPY(newp, oldp, col);

        /* Copy bytes after the changed character(s). */
        Bytes p = newp.plus(col);
        BCOPY(p, newlen, oldp, col + oldlen, linelen - col - oldlen);

        /* Insert or overwrite the new character. */
        BCOPY(p, buf, charlen);
        int i = charlen;

        /* Fill with spaces when necessary. */
        while (i < newlen)
            p.be(i++, (byte)' ');

        /* Replace the line in the buffer. */
        ml_replace(lnum, newp, false);

        /* mark the buffer as changed and prepare for displaying */
        changed_bytes(lnum, col);

        /*
         * If we're in Insert or Replace mode and 'showmatch' is set,
         * then briefly show the match for right parens and braces.
         */
        if (p_sm[0] && (State & INSERT) != 0 && msg_silent == 0)
        {
            showmatch(us_ptr2char(buf));
        }

        if (!p_ri[0] || (State & REPLACE_FLAG) != 0)
        {
            /* Normal insert: move cursor right. */
            curwin.w_cursor.col += charlen;
        }
        /*
         * TODO: should try to update w_row here, to avoid recomputing it later.
         */
    }

    /*
     * Insert a string at the cursor position.
     * Note: Does NOT handle Replace mode.
     * Caller must have prepared for undo.
     */
    /*private*/ static void ins_str(Bytes s)
    {
        int newlen = strlen(s);
        long lnum = curwin.w_cursor.lnum;

        if (virtual_active() && 0 < curwin.w_cursor.coladd)
            coladvance_force(getviscol());

        int col = curwin.w_cursor.col;
        Bytes oldp = ml_get(lnum);
        int oldlen = strlen(oldp);

        Bytes newp = new Bytes(oldlen + newlen + 1);

        if (0 < col)
            BCOPY(newp, oldp, col);
        BCOPY(newp, col, s, 0, newlen);
        BCOPY(newp, col + newlen, oldp, col, oldlen - col + 1);
        ml_replace(lnum, newp, false);
        changed_bytes(lnum, col);
        curwin.w_cursor.col += newlen;
    }

    /*
     * Delete one character under the cursor.
     * If "fixpos" is true, don't leave the cursor on the NUL after the line.
     * Caller must have prepared for undo.
     *
     * Return false for failure, true otherwise.
     */
    /*private*/ static boolean del_char(boolean fixpos)
    {
        /* Make sure the cursor is at the start of a character. */
        mb_adjust_pos(curbuf, curwin.w_cursor);
        if (ml_get_cursor().at(0) == NUL)
            return false;

        return del_chars(1, fixpos);
    }

    /*
     * Like del_bytes(), but delete characters instead of bytes.
     */
    /*private*/ static boolean del_chars(int count, boolean fixpos)
    {
        int bytes = 0;

        Bytes p = ml_get_cursor();
        for (int i = 0; i < count && p.at(0) != NUL; i++)
        {
            int l = us_ptr2len_cc(p);
            bytes += l;
            p = p.plus(l);
        }

        return del_bytes(bytes, fixpos, true);
    }

    /*
     * Delete "count" bytes under the cursor.
     * If "fixpos" is true, don't leave the cursor on the NUL after the line.
     * Caller must have prepared for undo.
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean del_bytes(int count, boolean fixpos, boolean use_delcombine)
        /* use_delcombine: 'delcombine' option applies */
    {
        long lnum = curwin.w_cursor.lnum;
        int col = curwin.w_cursor.col;

        Bytes oldp = ml_get(lnum);
        int oldlen = strlen(oldp);

        /*
         * Can't do anything when the cursor is on the NUL after the line.
         */
        if (oldlen <= col)
            return false;

        /* If 'delcombine' is set and deleting (less than) one character,
         * only delete the last combining character. */
        if (p_deco[0] && use_delcombine && count <= us_ptr2len_cc(oldp.plus(col)))
        {
            int[] cc = new int[MAX_MCO];

            us_ptr2char_cc(oldp.plus(col), cc);
            if (cc[0] != NUL)
            {
                /* Find the last composing char, there can be several. */
                int n = col;
                do
                {
                    col = n;
                    count = us_ptr2len(oldp.plus(n));
                    n += count;
                } while (utf_iscomposing(us_ptr2char(oldp.plus(n))));
                fixpos = false;
            }
        }

        /*
         * When count is too big, reduce it.
         */
        int movelen = oldlen - col - count + 1; /* includes trailing NUL */
        if (movelen <= 1)
        {
            /*
             * If we just took off the last character of a non-blank line, and
             * fixpos is true, we don't want to end up positioned at the NUL,
             * unless "restart_edit" is set or 'virtualedit' contains "onemore".
             */
            if (0 < col && fixpos && restart_edit == 0 && (ve_flags[0] & VE_ONEMORE) == 0)
            {
                --curwin.w_cursor.col;
                curwin.w_cursor.coladd = 0;
                curwin.w_cursor.col -= us_head_off(oldp, oldp.plus(curwin.w_cursor.col));
            }
            count = oldlen - col;
            movelen = 1;
        }

        /*
         * If the old line has been allocated the deletion can be done in the existing line.
         * Otherwise a new line has to be allocated.
         */
        boolean was_alloced = ml_line_alloced();    /* check if "oldp" was allocated */
        Bytes newp;
        if (was_alloced)
            newp = oldp;                            /* use same allocated memory */
        else
        {                                           /* need to allocate a new line */
            newp = new Bytes(oldlen + 1 - count);
            BCOPY(newp, oldp, col);
        }
        BCOPY(newp, col, oldp, col + count, movelen);
        if (!was_alloced)
            ml_replace(lnum, newp, false);

        /* mark the buffer as changed and prepare for displaying */
        changed_bytes(lnum, curwin.w_cursor.col);

        return true;
    }

    /*
     * Delete from cursor to end of line.
     * Caller must have prepared for undo.
     */
    /*private*/ static void truncate_line(boolean fixpos)
        /* fixpos: if true fix the cursor position when done */
    {
        long lnum = curwin.w_cursor.lnum;
        int col = curwin.w_cursor.col;

        Bytes newp;
        if (col == 0)
            newp = STRDUP(u8(""));
        else
            newp = STRNDUP(ml_get(lnum), col);

        ml_replace(lnum, newp, false);

        /* mark the buffer as changed and prepare for displaying */
        changed_bytes(lnum, curwin.w_cursor.col);

        /*
         * If "fixpos" is true we don't want to end up positioned at the NUL.
         */
        if (fixpos && 0 < curwin.w_cursor.col)
            --curwin.w_cursor.col;
    }

    /*
     * Delete "nlines" lines at the cursor.
     * Saves the lines for undo first if "undo" is true.
     */
    /*private*/ static void del_lines(long nlines, boolean undo)
        /* nlines: number of lines to delete */
        /* undo: if true, prepare for undo */
    {
        long first = curwin.w_cursor.lnum;

        if (nlines <= 0)
            return;

        /* save the deleted lines for undo */
        if (undo && u_savedel(first, nlines) == false)
            return;

        long n;
        for (n = 0; n < nlines; )
        {
            if ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0) /* nothing to delete */
                break;

            ml_delete(first, true);
            n++;

            /* If we delete the last line in the file, stop. */
            if (curbuf.b_ml.ml_line_count < first)
                break;
        }

        /* Correct the cursor position before calling deleted_lines_mark(),
         * it may trigger a callback to display the cursor. */
        curwin.w_cursor.col = 0;
        check_cursor_lnum();

        /* Adjust marks, mark the buffer as changed and prepare for displaying. */
        deleted_lines_mark(first, n);
    }

    /*private*/ static int gchar_pos(pos_C pos)
    {
        Bytes ptr = ml_get_pos(pos);

        return us_ptr2char(ptr);
    }

    /*private*/ static int gchar_cursor()
    {
        return us_ptr2char(ml_get_cursor());
    }

    /*
     * Write a character at the current cursor position.
     * It is directly written into the block.
     */
    /*private*/ static void pchar_cursor(int c)
    {
        ml_get_buf(curbuf, curwin.w_cursor.lnum, true).be(curwin.w_cursor.col, c);
    }

    /*
     * When extra == 0: Return true if the cursor is before or on the first non-blank in the line.
     * When extra == 1: Return true if the cursor is before the first non-blank in the line.
     */
    /*private*/ static boolean inindent(int extra)
    {
        int col = 0;

        for (Bytes p = ml_get_curline(); vim_iswhite(p.at(0)); col++)
            p = p.plus(1);

        return (curwin.w_cursor.col + extra <= col);
    }

    /*
     * Skip to next part of an option argument: Skip space and comma.
     */
    /*private*/ static Bytes skip_to_option_part(Bytes p)
    {
        if (p.at(0) == (byte)',')
            p = p.plus(1);
        while (p.at(0) == (byte)' ')
            p = p.plus(1);
        return p;
    }

    /*
     * Call this function when something in the current buffer is changed.
     *
     * Most often called through changed_bytes() and changed_lines(),
     * which also mark the area of the display to be redrawn.
     *
     * Careful: may trigger autocommands that reload the buffer.
     */
    /*private*/ static void changed()
    {
        if (!curbuf.b_changed[0])
        {
            /* Give a warning about changing a read-only file.
             * This may also check-out the file, thus change "curbuf"! */
            change_warning(0);

            changed_int();
        }
        curbuf.b_changedtick++;
    }

    /*
     * Internal part of changed(), no user interaction.
     */
    /*private*/ static void changed_int()
    {
        curbuf.b_changed[0] = true;
        ml_setflags(curbuf);
        check_status(curbuf);
        redraw_tabline = true;
    }

    /*
     * Changed bytes within a single line for the current buffer.
     * - marks the windows on this buffer to be redisplayed
     * - marks the buffer changed by calling changed()
     * - invalidates cached values
     * Careful: may trigger autocommands that reload the buffer.
     */
    /*private*/ static void changed_bytes(long lnum, int col)
    {
        changedOneline(curbuf, lnum);
        changed_common(lnum, col, lnum + 1, 0L);
    }

    /*private*/ static void changedOneline(buffer_C buf, long lnum)
    {
        if (buf.b_mod_set)
        {
            /* find the maximum area that must be redisplayed */
            if (lnum < buf.b_mod_top)
                buf.b_mod_top = lnum;
            else if (buf.b_mod_bot <= lnum)
                buf.b_mod_bot = lnum + 1;
        }
        else
        {
            /* set the area that must be redisplayed to one line */
            buf.b_mod_set = true;
            buf.b_mod_top = lnum;
            buf.b_mod_bot = lnum + 1;
            buf.b_mod_xlines = 0;
        }
    }

    /*
     * Appended "count" lines below line "lnum" in the current buffer.
     * Must be called AFTER the change and after mark_adjust().
     * Takes care of marking the buffer to be redrawn and sets the changed flag.
     */
    /*private*/ static void appended_lines(long lnum, long count)
    {
        changed_lines(lnum + 1, 0, lnum + 1, count);
    }

    /*
     * Like appended_lines(), but adjust marks first.
     */
    /*private*/ static void appended_lines_mark(long lnum, long count)
    {
        mark_adjust(lnum + 1, MAXLNUM, count, 0L);
        changed_lines(lnum + 1, 0, lnum + 1, count);
    }

    /*
     * Deleted "count" lines at line "lnum" in the current buffer.
     * Must be called AFTER the change and after mark_adjust().
     * Takes care of marking the buffer to be redrawn and sets the changed flag.
     */
    /*private*/ static void deleted_lines(long lnum, long count)
    {
        changed_lines(lnum, 0, lnum + count, -count);
    }

    /*
     * Like deleted_lines(), but adjust marks first.
     * Make sure the cursor is on a valid line before calling,
     * a GUI callback may be triggered to display the cursor.
     */
    /*private*/ static void deleted_lines_mark(long lnum, long count)
    {
        mark_adjust(lnum, lnum + count - 1, MAXLNUM, -count);
        changed_lines(lnum, 0, lnum + count, -count);
    }

    /*
     * Changed lines for the current buffer.
     * Must be called AFTER the change and after mark_adjust().
     * - mark the buffer changed by calling changed()
     * - mark the windows on this buffer to be redisplayed
     * - invalidate cached values
     * "lnum" is the first line that needs displaying,
     * "lnume" the first line below the changed lines (BEFORE the change).
     * When only inserting lines, "lnum" and "lnume" are equal.
     * Takes care of calling changed() and updating b_mod_*.
     * Careful: may trigger autocommands that reload the buffer.
     */
    /*private*/ static void changed_lines(long lnum, int col, long lnume, long xtra)
        /* lnum: first line with change */
        /* col: column in first line with change */
        /* lnume: line below last changed line */
        /* xtra: number of extra lines (negative when deleting) */
    {
        changed_lines_buf(curbuf, lnum, lnume, xtra);

        changed_common(lnum, col, lnume, xtra);
    }

    /*private*/ static void changed_lines_buf(buffer_C buf, long lnum, long lnume, long xtra)
        /* lnum: first line with change */
        /* lnume: line below last changed line */
        /* xtra: number of extra lines (negative when deleting) */
    {
        if (buf.b_mod_set)
        {
            /* find the maximum area that must be redisplayed */
            if (lnum < buf.b_mod_top)
                buf.b_mod_top = lnum;
            if (lnum < buf.b_mod_bot)
            {
                /* adjust old bot position for xtra lines */
                buf.b_mod_bot += xtra;
                if (buf.b_mod_bot < lnum)
                    buf.b_mod_bot = lnum;
            }
            if (buf.b_mod_bot < lnume + xtra)
                buf.b_mod_bot = lnume + xtra;
            buf.b_mod_xlines += xtra;
        }
        else
        {
            /* set the area that must be redisplayed */
            buf.b_mod_set = true;
            buf.b_mod_top = lnum;
            buf.b_mod_bot = lnume + xtra;
            buf.b_mod_xlines = xtra;
        }
    }

    /*
     * Common code for when a change is was made.
     * See changed_lines() for the arguments.
     * Careful: may trigger autocommands that reload the buffer.
     */
    /*private*/ static void changed_common(long lnum, int col, long lnume, long xtra)
    {
        /* mark the buffer as modified */
        changed();

        /* set the '. mark */
        if (!cmdmod.keepjumps)
        {
            curbuf.b_last_change.lnum = lnum;
            curbuf.b_last_change.col = col;

            /* Create a new entry if a new undo-able change was started
             * or we don't have an entry yet. */
            if (curbuf.b_new_change || curbuf.b_changelistlen == 0)
            {
                boolean add;
                if (curbuf.b_changelistlen == 0)
                    add = true;
                else
                {
                    /* Don't create a new entry when the line number is the
                     * same as the last one and the column is not too far away.
                     * Avoids creating many entries for typing "xxxxx". */
                    pos_C p = curbuf.b_changelist[curbuf.b_changelistlen - 1];
                    if (p.lnum != lnum)
                        add = true;
                    else
                    {
                        int cols = comp_textwidth(false);
                        if (cols == 0)
                            cols = 79;
                        add = (p.col + cols < col || col + cols < p.col);
                    }
                }
                if (add)
                {
                    /* This is the first of a new sequence of undo-able changes
                     * and it's at some distance of the last change.  Use a new
                     * position in the changelist. */
                    curbuf.b_new_change = false;

                    if (curbuf.b_changelistlen == JUMPLISTSIZE)
                    {
                        /* changelist is full: remove oldest entry */
                        curbuf.b_changelistlen = JUMPLISTSIZE - 1;
                        for (int i = 0; i < JUMPLISTSIZE - 1; i++)
                            COPY_pos(curbuf.b_changelist[i], curbuf.b_changelist[i + 1]);
                        for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
                            for (window_C wp = (tp == curtab) ? firstwin : tp.tp_firstwin; wp != null; wp = wp.w_next)
                            {
                                /* Correct position in changelist for other windows on this buffer. */
                                if (wp.w_buffer == curbuf && 0 < wp.w_changelistidx)
                                    --wp.w_changelistidx;
                            }
                    }
                    for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
                        for (window_C wp = (tp == curtab) ? firstwin : tp.tp_firstwin; wp != null; wp = wp.w_next)
                        {
                            /* For other windows, if the position in the changelist is at the end,
                             * it stays at the end. */
                            if (wp.w_buffer == curbuf && wp.w_changelistidx == curbuf.b_changelistlen)
                                wp.w_changelistidx++;
                        }
                    curbuf.b_changelistlen++;
                }
            }
            COPY_pos(curbuf.b_changelist[curbuf.b_changelistlen - 1], curbuf.b_last_change);
            /* The current window is always after the last change, so that "g," takes you back to it. */
            curwin.w_changelistidx = curbuf.b_changelistlen;
        }

        for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
            for (window_C wp = (tp == curtab) ? firstwin : tp.tp_firstwin; wp != null; wp = wp.w_next)
            {
                if (wp.w_buffer == curbuf)
                {
                    /* Mark this window to be redrawn later. */
                    if (wp.w_redr_type < VALID)
                        wp.w_redr_type = VALID;

                    /* Check if a change in the buffer has invalidated
                     * the cached values for the cursor. */
                    if (lnum < wp.w_cursor.lnum)
                        changed_line_abv_curs_win(wp);
                    else if (wp.w_cursor.lnum == lnum && col <= wp.w_cursor.col)
                        changed_cline_bef_curs_win(wp);
                    if (lnum <= wp.w_botline)
                    {
                        /* Assume that botline doesn't change
                         * (inserted lines make other lines scroll down below botline). */
                        approximate_botline_win(wp);
                    }

                    /* Check if any w_lines[] entries have become invalid.
                     * For entries below the change: Correct the lnums for inserted/deleted lines.
                     * Makes it possible to stop displaying after the change. */
                    for (int i = 0; i < wp.w_lines_valid; i++)
                        if (wp.w_lines[i].wl_valid)
                        {
                            if (lnum <= wp.w_lines[i].wl_lnum)
                            {
                                if (wp.w_lines[i].wl_lnum < lnume)
                                {
                                    /* line included in change */
                                    wp.w_lines[i].wl_valid = false;
                                }
                                else if (xtra != 0)
                                {
                                    /* line below change */
                                    wp.w_lines[i].wl_lnum += xtra;
                                }
                            }
                        }

                    /* relative numbering may require updating more */
                    if (wp.w_onebuf_opt.wo_rnu[0])
                        redraw_win_later(wp, SOME_VALID);
                }
            }

        /* Call update_screen() later, which checks out what needs to be redrawn,
         * since it notices b_mod_set and then uses b_mod_*. */
        if (must_redraw < VALID)
            must_redraw = VALID;

        /* When the cursor line is changed, always trigger CursorMoved. */
        if (lnum <= curwin.w_cursor.lnum && curwin.w_cursor.lnum < lnume + (xtra < 0 ? -xtra : xtra))
            last_cursormoved.lnum = 0;
    }

    /*
     * unchanged() is called when the changed flag must be reset for buffer 'buf'
     */
    /*private*/ static void unchanged(buffer_C buf, boolean ff)
        /* ff: also reset 'fileformat' */
    {
        if (buf.b_changed[0] || (ff && file_ff_differs(buf, false)))
        {
            buf.b_changed[0] = false;
            ml_setflags(buf);
            if (ff)
                save_file_ff(buf);
            check_status(buf);
            redraw_tabline = true;
        }
        buf.b_changedtick++;
    }

    /*
     * check_status: called when the status bars for the buffer 'buf' need to be updated
     */
    /*private*/ static void check_status(buffer_C buf)
    {
        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            if (wp.w_buffer == buf && wp.w_status_height != 0)
            {
                wp.w_redr_status = true;
                if (must_redraw < VALID)
                    must_redraw = VALID;
            }
    }

    /*private*/ static Bytes w_readonly = u8("W10: Warning: Changing a readonly file");

    /*
     * If the file is readonly, give a warning message with the first change.
     * Don't do this for autocommands.
     * Don't use emsg(), because it flushes the macro buffer.
     * If we have undone all changes "b_changed" will be false, but "b_did_warn" will be true.
     * Careful: may trigger autocommands that reload the buffer.
     */
    /*private*/ static void change_warning(int col)
        /* col: column for message; non-zero when in insert mode and 'showmode' is on */
    {
        if (!curbuf.b_did_warn && !curbufIsChanged() && !autocmd_busy && curbuf.b_p_ro[0])
        {
            curbuf_lock++;
            apply_autocmds(EVENT_FILECHANGEDRO, null, null, false, curbuf);
            --curbuf_lock;
            if (!curbuf.b_p_ro[0])
                return;
            /*
             * Do what msg() does, but with a column offset
             * if the warning should be after the mode message.
             */
            msg_start();
            if (msg_row == (int)Rows[0] - 1)
                msg_col = col;
            msg_source(hl_attr(HLF_W));
            msg_puts_attr(w_readonly, hl_attr(HLF_W) | MSG_HIST);
            set_vim_var_string(VV_WARNINGMSG, w_readonly, -1);
            msg_clr_eos();
            msg_end();
            if (msg_silent == 0 && !silent_mode)
            {
                out_flush();
                ui_delay(1000L, true);  /* give the user time to think about it */
            }
            curbuf.b_did_warn = true;
            redraw_cmdline = false;     /* don't redraw and erase the message */
            if (msg_row < Rows[0] - 1)
                showmode();
        }
    }

    /*
     * Ask for a reply from the user, a 'y' or a 'n'.
     * No other characters are accepted, the message is repeated
     * until a valid reply is entered or CTRL-C is hit.
     * If direct is true, don't use vgetc() but ui_inchar(),
     * don't get characters from any buffers but directly from the user.
     *
     * Answer 'y' or 'n'.
     */
    /*private*/ static int ask_yesno(Bytes q, boolean direct)
    {
        int c = ' ';

        int save_State = State;

        if (exiting)                /* put terminal in raw mode for this question */
            settmode(TMODE_RAW);

        no_wait_return++;
        State = CONFIRM;            /* mouse behaves like with :confirm */
        setmouse();                 /* disables mouse for xterm */
        no_mapping++;
        allow_keys++;               /* no mapping here, but recognize keys */

        while (c != 'y' && c != 'n')
        {
            /* same highlighting as for wait_return */
            smsg_attr(hl_attr(HLF_R), u8("%s (y/n)?"), q);

            c = (direct) ? get_keystroke() : plain_vgetc();
            if (c == Ctrl_C || c == ESC)
                c = 'n';

            msg_putchar(c);         /* show what you typed */
            out_flush();
        }

        --no_wait_return;
        State = save_State;
        setmouse();
        --no_mapping;
        --allow_keys;

        return c;
    }

    /*
     * Return true if "c" is a mouse key.
     */
    /*private*/ static boolean is_mouse_key(int c)
    {
        return c == K_LEFTMOUSE
            || c == K_LEFTMOUSE_NM
            || c == K_LEFTDRAG
            || c == K_LEFTRELEASE
            || c == K_LEFTRELEASE_NM
            || c == K_MIDDLEMOUSE
            || c == K_MIDDLEDRAG
            || c == K_MIDDLERELEASE
            || c == K_RIGHTMOUSE
            || c == K_RIGHTDRAG
            || c == K_RIGHTRELEASE
            || c == K_MOUSEDOWN
            || c == K_MOUSEUP
            || c == K_MOUSELEFT
            || c == K_MOUSERIGHT
            || c == K_X1MOUSE
            || c == K_X1DRAG
            || c == K_X1RELEASE
            || c == K_X2MOUSE
            || c == K_X2DRAG
            || c == K_X2RELEASE;
    }

    /*
     * Get a key stroke directly from the user.
     * Ignores mouse clicks and scrollbar events, except a click for the left button (used at the more prompt).
     * Doesn't use vgetc(), because it syncs undo and eats mapped characters.
     * Disadvantage: typeahead is ignored.
     * Translates the interrupt character for unix to ESC.
     */
    /*private*/ static int get_keystroke()
    {
        int c;

        int save_mapped_ctrl_c = mapped_ctrl_c;

        mapped_ctrl_c = 0;      /* mappings are not used here */

        Bytes buf = null;

        int buflen = 150;
        int[] len = { 0 };
        int waited = 0;

        for ( ; ; )
        {
            cursor_on();
            out_flush();

            /* Leave some room for check_termcode() to insert a key code into (max 5 chars plus NUL).
             * And fix_input_buffer() can triple the number of bytes. */
            int maxlen = (buflen - 6 - len[0]) / 3;
            if (buf == null)
                buf = new Bytes(buflen);
            else if (maxlen < 10)
            {
                /* Need some more space.
                 * This might happen when receiving a long escape sequence. */
                buflen += 100;
                Bytes p = new Bytes(buflen);
                BCOPY(p, buf, len[0]);
                buf = p;
                maxlen = (buflen - 6 - len[0]) / 3;
            }

            /* First time: blocking wait.
             * Second time: wait up to 100ms for a terminal code to complete. */
            int n = ui_inchar(buf.plus(len[0]), maxlen, (len[0] == 0) ? -1L : 100L, 0);
            if (0 < n)
            {
                /* Replace zero and CSI by a special key code. */
                n = fix_input_buffer(buf.plus(len[0]), n, false);
                len[0] += n;
                waited = 0;
            }
            else if (0 < len[0])
                waited++;                   /* keep track of the waiting time */

            /* Incomplete termcode and not timed out yet: get more characters. */
            n = check_termcode(1, buf, buflen, len);
            if (n < 0 && (!p_ttimeout[0] || waited * 100L < (p_ttm[0] < 0 ? p_tm[0] : p_ttm[0])))
                continue;

            if (n == KEYLEN_REMOVED)        /* key code removed */
            {
                if (must_redraw != 0 && !need_wait_return && (State & CMDLINE) == 0)
                {
                    /* Redrawing was postponed, do it now. */
                    update_screen(0);
                    setcursor();            /* put cursor back where it belongs */
                }
                continue;
            }
            if (0 < n)                      /* found a termcode: adjust length */
                len[0] = n;
            if (len[0] == 0)                   /* nothing typed yet */
                continue;

            /* Handle modifier and/or special key code. */
            if (buf.at(0) == KB_SPECIAL)
            {
                c = toSpecial(buf.at(1), buf.at(2));
                if (buf.at(1) == KS_MODIFIER || c == K_IGNORE || (is_mouse_key(c) && c != K_LEFTMOUSE))
                {
                    if (buf.at(1) == KS_MODIFIER)
                        mod_mask = char_u(buf.at(2));
                    len[0] -= 3;
                    if (0 < len[0])
                        BCOPY(buf, 0, buf, 3, len[0]);
                    continue;
                }
                break;
            }

            if (len[0] < mb_byte2len(char_u(buf.at(0))))
                continue;                   /* more bytes to get */

            buf.be(buflen <= len[0] ? buflen - 1 : len[0], NUL);
            c = us_ptr2char(buf);

            if (c == intr_char)
                c = ESC;
            break;
        }

        mapped_ctrl_c = save_mapped_ctrl_c;

        return c;
    }

    /*
     * Get a number from the user.
     * When "mouse_used" is not null allow using the mouse.
     */
    /*private*/ static int get_number(boolean colon, boolean[] mouse_used)
        /* colon: allow colon to abort */
    {
        int n = 0;
        int typed = 0;

        if (mouse_used != null)
            mouse_used[0] = false;

        /* When not printing messages, the user won't know what to type,
         * return a zero (as if CR was hit). */
        if (msg_silent != 0)
            return 0;

        no_mapping++;
        allow_keys++;               /* no mapping here, but recognize keys */

        for ( ; ; )
        {
            windgoto(msg_row, msg_col);
            int c = safe_vgetc();
            if (asc_isdigit(c))
            {
                n = n * 10 + c - '0';
                msg_putchar(c);
                typed++;
            }
            else if (c == K_DEL || c == K_KDEL || c == K_BS || c == Ctrl_H)
            {
                if (0 < typed)
                {
                    msg_puts(u8("\b \b"));
                    --typed;
                }
                n /= 10;
            }
            else if (mouse_used != null && c == K_LEFTMOUSE)
            {
                mouse_used[0] = true;
                n = mouse_row + 1;
                break;
            }
            else if (n == 0 && c == ':' && colon)
            {
                stuffcharReadbuff(':');
                if (exmode_active == 0)
                    cmdline_row = msg_row;
                skip_redraw = true;     /* skip redraw once */
                do_redraw = false;
                break;
            }
            else if (c == CAR || c == NL || c == Ctrl_C || c == ESC)
                break;
        }

        --no_mapping;
        --allow_keys;
        return n;
    }

    /*
     * Ask the user to enter a number.
     * When "mouse_used" is not null allow using the mouse and in that case return the line number.
     */
    /*private*/ static int prompt_for_number(boolean[] mouse_used)
    {
        /* When using ":silent" assume that <CR> was entered. */
        if (mouse_used != null)
            msg_puts(u8("Type number and <Enter> or click with mouse (empty cancels): "));
        else
            msg_puts(u8("Type number and <Enter> (empty cancels): "));

        /* Set the state such that text can be selected/copied/pasted and we still get mouse events. */
        int save_cmdline_row = cmdline_row;
        cmdline_row = 0;
        int save_State = State;
        State = CMDLINE;

        int i = get_number(true, mouse_used);
        if (keyTyped)
        {
            /* don't call wait_return() now */
            /* msg_putchar('\n'); */
            cmdline_row = msg_row - 1;
            need_wait_return = false;
            msg_didany = false;
            msg_didout = false;
        }
        else
            cmdline_row = save_cmdline_row;
        State = save_State;

        return i;
    }

    /*private*/ static void msgmore(long n)
    {
        if (global_busy != 0             /* no messages now, wait until global is finished */
                || !messaging())    /* 'lazyredraw' set, don't do messages now */
            return;

        /* We don't want to overwrite another important message, but do overwrite
         * a previous "more lines" or "fewer lines" message, so that "5dd" and
         * then "put" reports the last action. */
        if (keep_msg != null && !keep_msg_more)
            return;

        long pn;
        if (0 < n)
            pn = n;
        else
            pn = -n;

        if (p_report[0] < pn)
        {
            if (pn == 1)
            {
                if (0 < n)
                    vim_strncpy(msg_buf, u8("1 more line"), MSG_BUF_LEN - 1);
                else
                    vim_strncpy(msg_buf, u8("1 line less"), MSG_BUF_LEN - 1);
            }
            else
            {
                if (0 < n)
                    vim_snprintf(msg_buf, MSG_BUF_LEN, u8("%ld more lines"), pn);
                else
                    vim_snprintf(msg_buf, MSG_BUF_LEN, u8("%ld fewer lines"), pn);
            }
            if (got_int)
                vim_strcat(msg_buf, u8(" (Interrupted)"), MSG_BUF_LEN);
            if (msg(msg_buf))
            {
                set_keep_msg(msg_buf, 0);
                keep_msg_more = true;
            }
        }
    }

    /*
     * flush map and typeahead buffers and give a warning for an error
     */
    /*private*/ static void beep_flush()
    {
        if (emsg_silent == 0)
        {
            flush_buffers(false);
            vim_beep();
        }
    }

    /*
     * give a warning for an error
     */
    /*private*/ static void vim_beep()
    {
        if (emsg_silent == 0)
        {
            if (p_vb[0])
                out_str(T_VB[0]);
            else
                out_char(BELL);

            /* When 'verbose' is set and we are sourcing a script or executing a function,
             * give the user a hint where the beep comes from. */
            if (vim_strchr(p_debug[0], 'e') != null)
            {
                msg_source(hl_attr(HLF_W));
                msg_attr(u8("Beep!"), hl_attr(HLF_W));
            }
        }
    }

    /*
     * Compare two file names and return:
     * FPC_SAME   if they both exist and are the same file.
     * FPC_SAMEX  if they both don't exist and have the same file name.
     * FPC_DIFF   if they both exist and are different files.
     * FPC_NOTX   if they both don't exist.
     * FPC_DIFFX  if one of them doesn't exist.
     * For the first name environment variables are expanded
     */
    /*private*/ static int fullpathcmp(Bytes s1, Bytes s2, boolean checkname)
        /* checkname: when both don't exist, check file names */
    {
        Bytes full1 = new Bytes(MAXPATHL), full2 = new Bytes(MAXPATHL);
        stat_C st1 = new stat_C(), st2 = new stat_C();

        int r1 = libC.stat(s1, st1);
        int r2 = libC.stat(s2, st2);
        if (r1 != 0 && r2 != 0)
        {
            /* if stat() doesn't work, may compare the names */
            if (checkname)
            {
                if (STRCMP(s1, s2) == 0)
                    return FPC_SAMEX;
                boolean b1 = vim_fullName(s1, full1, MAXPATHL, false);
                boolean b2 = vim_fullName(s2, full2, MAXPATHL, false);
                if (b1 && b2 && STRCMP(full1, full2) == 0)
                    return FPC_SAMEX;
            }
            return FPC_NOTX;
        }
        if (r1 != 0 || r2 != 0)
            return FPC_DIFFX;
        if (st1.st_dev() == st2.st_dev() && st1.st_ino() == st2.st_ino())
            return FPC_SAME;

        return FPC_DIFF;
    }

    /*
     * Get the tail of a path: the file name.
     * When the path ends in a path separator the tail is the NUL after it.
     * Fail safe: never returns null.
     */
    /*private*/ static Bytes gettail(Bytes fname)
    {
        if (fname == null)
            return u8("");

        Bytes p1, p2;
        for (p1 = p2 = get_past_head(fname); p2.at(0) != NUL; )     /* find last part of path */
        {
            if (vim_ispathsep_nocolon(p2.at(0)))
                p1 = p2.plus(1);
            p2 = p2.plus(us_ptr2len_cc(p2));
        }
        return p1;
    }

    /*
     * Get pointer to tail of "fname", including path separators.
     * Putting a NUL here leaves the directory name.
     * Takes care of "c:/" and "//".
     * Always returns a valid pointer.
     */
    /*private*/ static Bytes gettail_sep(Bytes fname)
    {
        Bytes p = get_past_head(fname); /* don't remove the '/' from "c:/file" */
        Bytes t = gettail(fname);
        while (BLT(p, t) && after_pathsep(fname, t))
            t = t.minus(1);
        return t;
    }

    /*
     * get the next path component (just after the next path separator).
     */
    /*private*/ static Bytes getnextcomp(Bytes fname)
    {
        while (fname.at(0) != NUL && !vim_ispathsep(fname.at(0)))
            fname = fname.plus(us_ptr2len_cc(fname));
        if (fname.at(0) != NUL)
            fname = fname.plus(1);
        return fname;
    }

    /*
     * Get a pointer to one character past the head of a path name.
     * Unix: after "/"; DOS: after "c:\"; Amiga: after "disk:/"; Mac: no head.
     * If there is no head, path is returned.
     */
    /*private*/ static Bytes get_past_head(Bytes path)
    {
        Bytes retval = path;

        while (vim_ispathsep(retval.at(0)))
            retval = retval.plus(1);

        return retval;
    }

    /*
     * Return true if 'c' is a path separator.
     * Note that for MS-Windows this includes the colon.
     */
    /*private*/ static boolean vim_ispathsep(int c)
    {
        return (c == '/');      /* UNIX has ':' inside file names */
    }

    /*
     * Like vim_ispathsep(c), but exclude the colon for MS-Windows.
     */
    /*private*/ static boolean vim_ispathsep_nocolon(int c)
    {
        return vim_ispathsep(c);
    }

    /*
     * Shorten the path of a file from "~/foo/../.bar/fname" to "~/f/../.b/fname"
     * It's done in-place.
     */
    /*private*/ static void shorten_dir(Bytes str)
    {
        boolean skip = false;

        Bytes tail = gettail(str);
        Bytes d = str;
        for (Bytes s = str; ; s = s.plus(1))
        {
            if (BLE(tail, s))                      /* copy the whole tail */
            {
                (d = d.plus(1)).be(-1, s.at(0));
                if (s.at(0) == NUL)
                    break;
            }
            else if (vim_ispathsep(s.at(0)))         /* copy '/' and next char */
            {
                (d = d.plus(1)).be(-1, s.at(0));
                skip = false;
            }
            else if (!skip)
            {
                (d = d.plus(1)).be(-1, s.at(0));                      /* copy next char */
                if (s.at(0) != (byte)'~' && s.at(0) != (byte)'.')     /* and leading "~" and "." */
                    skip = true;

                for (int l = us_ptr2len_cc(s); 0 < --l; )
                    (d = d.plus(1)).be(-1, (s = s.plus(1)).at(0));
            }
        }
    }

    /*
     * Return true if the directory of "fname" exists, false otherwise.
     * Also returns true if there is no directory name.
     * "fname" must be writable!.
     */
    /*private*/ static boolean dir_of_file_exists(Bytes fname)
    {
        Bytes p = gettail_sep(fname);
        if (BEQ(p, fname))
            return true;

        byte c = p.at(0);
        p.be(0, NUL);
        boolean retval = mch_isdir(fname);
        p.be(0, c);
        return retval;
    }

    /*
     * Concatenate file names fname1 and fname2 into allocated memory.
     * Only add a '/' or '\\' when 'sep' is true and it is necessary.
     */
    /*private*/ static Bytes concat_fnames(Bytes fname1, Bytes fname2, boolean sep)
    {
        Bytes dest = new Bytes(strlen(fname1) + strlen(fname2) + 3);

        STRCPY(dest, fname1);
        if (sep)
            add_pathsep(dest);
        STRCAT(dest, fname2);

        return dest;
    }

    /*
     * Concatenate two strings and return the result in allocated memory.
     */
    /*private*/ static Bytes concat_str(Bytes str1, Bytes str2)
    {
        int len = strlen(str1);

        Bytes dest = new Bytes(len + strlen(str2) + 1);

        STRCPY(dest, str1);
        STRCPY(dest.plus(len), str2);

        return dest;
    }

    /*
     * Add a path separator to a file name, unless it already ends in a path separator.
     */
    /*private*/ static void add_pathsep(Bytes p)
    {
        if (p.at(0) != NUL && !after_pathsep(p, p.plus(strlen(p))))
            STRCAT(p, u8("/"));
    }

    /*
     * Make an allocated copy of a full file name.
     * Returns null when out of memory.
     */
    /*private*/ static Bytes fullName_save(Bytes fname, boolean force)
        /* force: force expansion, even when it already looks like a full path name */
    {
        if (fname == null)
            return null;

        Bytes buf = new Bytes(MAXPATHL);

        Bytes new_fname;
        if (vim_fullName(fname, buf, MAXPATHL, force) != false)
            new_fname = STRDUP(buf);
        else
            new_fname = STRDUP(fname);

        return new_fname;
    }

    /*
     * Find the start of a comment, not knowing if we are in a comment right now.
     * Search starts at w_cursor.lnum and goes backwards.
     */
    /*private*/ static pos_C ind_find_start_comment()
    {
        return find_start_comment(curbuf.b_ind_maxcomment);
    }

    /*private*/ static pos_C find_start_comment(int ind_maxcomment)
    {
        pos_C pos;
        Bytes line;
        Bytes p;
        int cur_maxcomment = ind_maxcomment;

        for ( ; ; )
        {
            pos = findmatchlimit(null, '*', FM_BACKWARD, cur_maxcomment);
            if (pos == null)
                break;

            /*
             * Check if the comment start we found is inside a string.
             * If it is then restrict the search to below this line and try again.
             */
            line = ml_get(pos.lnum);
            for (p = line; p.at(0) != NUL && BDIFF(p, line) < pos.col; p = p.plus(1))
                p = skip_string(p);
            if (BDIFF(p, line) <= pos.col)
                break;
            cur_maxcomment = (int)(curwin.w_cursor.lnum - pos.lnum - 1);
            if (cur_maxcomment <= 0)
            {
                pos = null;
                break;
            }
        }
        return pos;
    }

    /*
     * Skip to the end of a "string" and a 'c' character.
     * If there is no string or character, return argument unmodified.
     */
    /*private*/ static Bytes skip_string(Bytes p)
    {
        /*
         * We loop, because strings may be concatenated: "date""time".
         */
        for ( ; ; p = p.plus(1))
        {
            if (p.at(0) == (byte)'\'')                       /* 'c' or '\n' or '\000' */
            {
                if (p.at(1) == NUL)                          /* ' at end of line */
                    break;
                int i = 2;
                if (p.at(1) == (byte)'\\')                   /* '\n' or '\000' */
                {
                    i++;
                    while (asc_isdigit(p.at(i - 1)))   /* '\000' */
                        i++;
                }
                if (p.at(i) == (byte)'\'')                   /* check for trailing ' */
                {
                    p = p.plus(i);
                    continue;
                }
            }
            else if (p.at(0) == (byte)'"')                   /* start of string */
            {
                for (p = p.plus(1); p.at(0) != NUL; p = p.plus(1))
                {
                    if (p.at(0) == (byte)'\\' && p.at(1) != NUL)
                        p = p.plus(1);
                    else if (p.at(0) == (byte)'"')           /* end of string */
                        break;
                }
                if (p.at(0) == (byte)'"')
                    continue;
            }
            break;                                  /* no string found */
        }
        if (p.at(0) == NUL)
            p = p.minus(1);                                    /* backup from NUL */
        return p;
    }

    /*
     * Do C or expression indenting on the current line.
     */
    /*private*/ static void do_c_expr_indent()
    {
        if (curbuf.b_p_inde[0].at(0) != NUL)
            fixthisline(get_expr_indent);
        else
            fixthisline(get_c_indent);
    }

    /*
     * Functions for C-indenting.
     * Most of this originally comes from Eric Fischer.
     */

    /*
     * Skip over white space and C comments within the line.
     * Also skip over Perl/shell comments if desired.
     */
    /*private*/ static Bytes cin_skipcomment(Bytes s)
    {
        while (s.at(0) != NUL)
        {
            Bytes prev_s = s;

            s = skipwhite(s);

            /* Perl/shell # comment continues until eol.
             * Require a space before # to avoid recognizing $#array. */
            if (curbuf.b_ind_hash_comment != 0 && BNE(s, prev_s) && s.at(0) == (byte)'#')
            {
                s = s.plus(strlen(s));
                break;
            }
            if (s.at(0) != (byte)'/')
                break;
            s = s.plus(1);
            if (s.at(0) == (byte)'/')          /* slash-slash comment continues till eol */
            {
                s = s.plus(strlen(s));
                break;
            }
            if (s.at(0) != (byte)'*')
                break;
            for (s = s.plus(1); s.at(0) != NUL; s = s.plus(1))      /* skip slash-star comment */
                if (s.at(0) == (byte)'*' && s.at(1) == (byte)'/')
                {
                    s = s.plus(2);
                    break;
                }
        }
        return s;
    }

    /*
     * Return true if there is no code at s[0].  White space and comments are not considered code.
     */
    /*private*/ static boolean cin_nocode(Bytes s)
    {
        return (cin_skipcomment(s).at(0) == NUL);
    }

    /*private*/ static pos_C _3_pos = new pos_C();

    /*
     * Check previous lines for a "//" line comment, skipping over blank lines.
     */
    /*private*/ static pos_C find_line_comment()
    {
        COPY_pos(_3_pos, curwin.w_cursor);

        while (0 < --_3_pos.lnum)
        {
            Bytes line = ml_get(_3_pos.lnum);
            Bytes p = skipwhite(line);
            if (cin_islinecomment(p))
            {
                _3_pos.col = BDIFF(p, line);
                return _3_pos;
            }
            if (p.at(0) != NUL)
                break;
        }

        return null;
    }

    /*
     * Return true if "text" starts with "key:".
     */
    /*private*/ static boolean cin_has_js_key(Bytes text)
    {
        Bytes s = skipwhite(text);
        int quote = -1;

        if (s.at(0) == (byte)'\'' || s.at(0) == (byte)'"')
        {
            /* can be 'key': or "key": */
            quote = s.at(0);
            s = s.plus(1);
        }
        if (!vim_isIDc(s.at(0)))     /* need at least one ID character */
            return false;

        while (vim_isIDc(s.at(0)))
            s = s.plus(1);
        if (s.at(0) == quote)
            s = s.plus(1);

        s = cin_skipcomment(s);

        /* "::" is not a label, it's C++ */
        return (s.at(0) == (byte)':' && s.at(1) != (byte)':');
    }

    /*
     * Check if string matches "label:"; move to character after ':' if true.
     * "*s" must point to the start of the label, if there is one.
     */
    /*private*/ static boolean cin_islabel_skip(Bytes[] s)
    {
        if (!vim_isIDc(s[0].at(0)))            /* need at least one ID character */
            return false;

        while (vim_isIDc(s[0].at(0)))
            s[0] = s[0].plus(1);

        s[0] = cin_skipcomment(s[0]);

        /* "::" is not a label, it's C++ */
        return (s[0].at(0) == (byte)':' && (s[0] = s[0].plus(1)).at(0) != (byte)':');
    }

    /*
     * Recognize a label: "label:".
     * Note: curwin.w_cursor must be where we are looking for the label.
     */
    /*private*/ static boolean cin_islabel()
    {
        Bytes[] s = { cin_skipcomment(ml_get_curline()) };

        /*
         * Exclude "default" from labels, since it should be indented
         * like a switch label.  Same for C++ scope declarations.
         */
        if (cin_isdefault(s[0]))
            return false;
        if (cin_isscopedecl(s[0]))
            return false;

        if (cin_islabel_skip(s))
        {
            pos_C cursor_save = new pos_C();
            COPY_pos(cursor_save, curwin.w_cursor);

            /*
             * Only accept a label if the previous line is terminated or is a case label.
             */
            while (1 < curwin.w_cursor.lnum)
            {
                --curwin.w_cursor.lnum;

                /*
                 * If we're in a comment now, skip to the start of the comment.
                 */
                curwin.w_cursor.col = 0;
                pos_C trypos = ind_find_start_comment();
                if (trypos != null)
                    COPY_pos(curwin.w_cursor, trypos);

                Bytes[] line = { ml_get_curline() };
                if (cin_ispreproc(line[0]))    /* ignore #defines, #if, etc. */
                    continue;
                line[0] = cin_skipcomment(line[0]);
                if (line[0].at(0) == NUL)
                    continue;

                COPY_pos(curwin.w_cursor, cursor_save);
                return (cin_isterminated(line[0], true, false) != NUL
                        || cin_isscopedecl(line[0])
                        || cin_iscase(line[0], true)
                        || (cin_islabel_skip(line) && cin_nocode(line[0])));
            }

            COPY_pos(curwin.w_cursor, cursor_save);
            return true;            /* label at start of file??? */
        }

        return false;
    }

    /*private*/ static final Bytes[] cin__skip = { u8("static"), u8("public"), u8("protected"), u8("private") };

    /*
     * Recognize structure initialization and enumerations:
     * "[typedef] [static|public|protected|private] enum"
     * "[typedef] [static|public|protected|private] = {"
     */
    /*private*/ static boolean cin_isinit()
    {
        Bytes s = cin_skipcomment(ml_get_curline());

        if (cin_starts_with(s, u8("typedef")))
            s = cin_skipcomment(s.plus(7));

        for ( ; ; )
        {
            int len = 0;	// %% anno dunno
            for (int i = 0; i < cin__skip.length; i++)
            {
                len = strlen(cin__skip[i]);
                if (cin_starts_with(s, cin__skip[i]))
                {
                    s = cin_skipcomment(s.plus(len));
                    len = 0;
                    break;
                }
            }
            if (len != 0)
                break;
        }

        if (cin_starts_with(s, u8("enum")))
            return true;

        if (cin_ends_in(s, u8("="), u8("{")))
            return true;

        return false;
    }

    /*
     * Recognize a switch label: "case .*:" or "default:".
     */
    /*private*/ static boolean cin_iscase(Bytes s, boolean strict)
        /* strict: Allow relaxed check of case statement for JS */
    {
        s = cin_skipcomment(s);

        if (cin_starts_with(s, u8("case")))
        {
            for (s = s.plus(4); s.at(0) != NUL; s = s.plus(1))
            {
                s = cin_skipcomment(s);
                if (s.at(0) == (byte)':')
                {
                    if (s.at(1) == (byte)':')        /* skip over "::" for C++ */
                        s = s.plus(1);
                    else
                        return true;
                }
                if (s.at(0) == (byte)'\'' && s.at(1) != NUL && s.at(2) == (byte)'\'')
                    s = s.plus(2);                 /* skip over ':' */
                else if (s.at(0) == (byte)'/' && (s.at(1) == (byte)'*' || s.at(1) == (byte)'/'))
                    return false;           /* stop at comment */
                else if (s.at(0) == (byte)'"')
                {
                    /* JS etc. */
                    if (strict)
                        return false;       /* stop at string */
                    else
                        return true;
                }
            }
            return false;
        }

        if (cin_isdefault(s))
            return true;

        return false;
    }

    /*
     * Recognize a "default" switch label.
     */
    /*private*/ static boolean cin_isdefault(Bytes s)
    {
        return (STRNCMP(s, u8("default"), 7) == 0 && (s = cin_skipcomment(s.plus(7))).at(0) == (byte)':' && s.at(1) != (byte)':');
    }

    /*
     * Recognize a "public/private/protected" scope declaration label.
     */
    /*private*/ static boolean cin_isscopedecl(Bytes s)
    {
        int i;

        s = cin_skipcomment(s);
        if (STRNCMP(s, u8("public"), 6) == 0)
            i = 6;
        else if (STRNCMP(s, u8("protected"), 9) == 0)
            i = 9;
        else if (STRNCMP(s, u8("private"), 7) == 0)
            i = 7;
        else
            return false;

        return ((s = cin_skipcomment(s.plus(i))).at(0) == (byte)':' && s.at(1) != (byte)':');
    }

    /* Maximum number of lines to search back for a "namespace" line. */
    /*private*/ static final int FIND_NAMESPACE_LIM = 20;

    /*
     * Recognize a "namespace" scope declaration.
     */
    /*private*/ static boolean cin_is_cpp_namespace(Bytes s)
    {
        s = cin_skipcomment(s);

        if (STRNCMP(s, u8("namespace"), 9) == 0 && (s.at(9) == NUL || !us_iswordb(s.at(9), curbuf)))
        {
            boolean has_name = false;

            for (Bytes p = cin_skipcomment(skipwhite(s.plus(9))); p.at(0) != NUL; )
            {
                if (vim_iswhite(p.at(0)))
                {
                    has_name = true;                        /* found end of a name */
                    p = cin_skipcomment(skipwhite(p));
                }
                else if (p.at(0) == (byte)'{')
                {
                    break;
                }
                else if (us_iswordb(p.at(0), curbuf))
                {
                    if (has_name)
                        return false;               /* word character after skipping past name */
                    p = p.plus(1);
                }
                else
                    return false;
            }

            return true;
        }

        return false;
    }

    /*
     * Return a pointer to the first non-empty non-comment character after a ':'.
     * Return null if not found.
     *        case 234:    a = b;
     *                     ^
     */
    /*private*/ static Bytes after_label(Bytes s)
    {
        for ( ; s.at(0) != NUL; s = s.plus(1))
        {
            if (s.at(0) == (byte)':')
            {
                if (s.at(1) == (byte)':')        /* skip over "::" for C++ */
                    s = s.plus(1);
                else if (!cin_iscase(s.plus(1), false))
                    break;
            }
            else if (s.at(0) == (byte)'\'' && s.at(1) != NUL && s.at(2) == (byte)'\'')
                s = s.plus(2);                 /* skip over 'x' */
        }
        if (s.at(0) == NUL)
            return null;
        s = cin_skipcomment(s.plus(1));
        if (s.at(0) == NUL)
            return null;

        return s;
    }

    /*
     * Get indent of line "lnum", skipping a label.
     * Return 0 if there is nothing after the label.
     */
    /*private*/ static int get_indent_nolabel(long lnum)
    {
        Bytes line = ml_get(lnum);
        Bytes p = after_label(line);
        if (p == null)
            return 0;

        pos_C fp = new pos_C();
        fp.col = BDIFF(p, line);
        fp.lnum = lnum;

        int[] col = new int[1];
        getvcol(curwin, fp, col, null, null);
        return col[0];
    }

    /*
     * Find indent for line "lnum", ignoring any case or jump label.
     * Also return a pointer to the text (after the label) in "pp".
     *   label:     if (asdf && asdfasdf)
     *              ^
     */
    /*private*/ static int skip_label(long lnum, Bytes[] pp)
    {
        int amount;
        pos_C cursor_save = new pos_C();
        COPY_pos(cursor_save, curwin.w_cursor);

        curwin.w_cursor.lnum = lnum;
        Bytes p = ml_get_curline();

        if (cin_iscase(p, false) || cin_isscopedecl(p) || cin_islabel())
        {
            amount = get_indent_nolabel(lnum);
            p = after_label(ml_get_curline());
            if (p == null)                      /* just in case */
                p = ml_get_curline();
        }
        else
        {
            amount = get_indent();
            p = ml_get_curline();
        }
        pp[0] = p;

        COPY_pos(curwin.w_cursor, cursor_save);
        return amount;
    }

    /*
     * Return the indent of the first variable name after a type in a declaration.
     *  int     a,                  indent of "a"
     *  static struct foo    b,     indent of "b"
     *  enum bla    c,              indent of "c"
     * Returns zero when it doesn't look like a declaration.
     */
    /*private*/ static int cin_first_id_amount()
    {
        Bytes line = ml_get_curline();

        Bytes p = skipwhite(line);
        int len = BDIFF(skiptowhite(p), p);
        if (len == 6 && STRNCMP(p, u8("static"), 6) == 0)
        {
            p = skipwhite(p.plus(6));
            len = BDIFF(skiptowhite(p), p);
        }

        if (len == 6 && STRNCMP(p, u8("struct"), 6) == 0)
            p = skipwhite(p.plus(6));
        else if (len == 4 && STRNCMP(p, u8("enum"), 4) == 0)
            p = skipwhite(p.plus(4));
        else if ((len == 8 && STRNCMP(p, u8("unsigned"), 8) == 0)
              || (len == 6 && STRNCMP(p, u8("signed"), 6) == 0))
        {
            Bytes s = skipwhite(p.plus(len));
            if ((STRNCMP(s, u8("int"), 3) == 0 && vim_iswhite(s.at(3)))
             || (STRNCMP(s, u8("long"), 4) == 0 && vim_iswhite(s.at(4)))
             || (STRNCMP(s, u8("short"), 5) == 0 && vim_iswhite(s.at(5)))
             || (STRNCMP(s, u8("char"), 4) == 0 && vim_iswhite(s.at(4))))
                p = s;
        }

        for (len = 0; vim_isIDc(p.at(len)); len++)
            ;
        if (len == 0 || !vim_iswhite(p.at(len)) || cin_nocode(p))
            return 0;

        p = skipwhite(p.plus(len));

        pos_C fp = new pos_C();
        fp.lnum = curwin.w_cursor.lnum;
        fp.col = BDIFF(p, line);

        int[] col = new int[1];
        getvcol(curwin, fp, col, null, null);
        return col[0];
    }

    /*
     * Return the indent of the first non-blank after an equal sign.
     *       char *foo = "here";
     * Return zero if no (useful) equal sign found.
     * Return -1 if the line above "lnum" ends in a backslash.
     *      foo = "asdf\
     *             asdf\
     *             here";
     */
    /*private*/ static int cin_get_equal_amount(long lnum)
    {
        if (1 < lnum)
        {
            Bytes line = ml_get(lnum - 1);
            if (line.at(0) != NUL && line.at(strlen(line) - 1) == '\\')
                return -1;
        }

        Bytes line = ml_get(lnum);

        Bytes s = line;
        while (s.at(0) != NUL && vim_strbyte(u8("=;{}\"'"), s.at(0)) == null)
        {
            if (cin_iscomment(s))   /* ignore comments */
                s = cin_skipcomment(s);
            else
                s = s.plus(1);
        }
        if (s.at(0) != (byte)'=')
            return 0;

        s = skipwhite(s.plus(1));
        if (cin_nocode(s))
            return 0;

        if (s.at(0) == (byte)'"')      /* nice alignment for continued strings */
            s = s.plus(1);

        pos_C fp = new pos_C();
        fp.lnum = lnum;
        fp.col = BDIFF(s, line);

        int[] col = new int[1];
        getvcol(curwin, fp, col, null, null);
        return col[0];
    }

    /*
     * Recognize a preprocessor statement: Any line that starts with '#'.
     */
    /*private*/ static boolean cin_ispreproc(Bytes s)
    {
        return (skipwhite(s).at(0) == (byte)'#');
    }

    /*
     * Return true if line "*pp" at "*lnump" is a preprocessor statement or a
     * continuation line of a preprocessor statement.  Decrease "*lnump" to the
     * start and return the line in "*pp".
     */
    /*private*/ static boolean cin_ispreproc_cont(Bytes[] pp, long[] lnump)
    {
        boolean retval = false;

        Bytes line = pp[0];
        long lnum = lnump[0];

        for ( ; ; )
        {
            if (cin_ispreproc(line))
            {
                retval = true;
                lnump[0] = lnum;
                break;
            }
            if (lnum == 1)
                break;
            line = ml_get(--lnum);
            if (line.at(0) == NUL || line.at(strlen(line) - 1) != '\\')
                break;
        }

        if (lnum != lnump[0])
            pp[0] = ml_get(lnump[0]);

        return retval;
    }

    /*
     * Recognize the start of a C or C++ comment.
     */
    /*private*/ static boolean cin_iscomment(Bytes p)
    {
        return (p.at(0) == (byte)'/' && (p.at(1) == (byte)'*' || p.at(1) == (byte)'/'));
    }

    /*
     * Recognize the start of a "//" comment.
     */
    /*private*/ static boolean cin_islinecomment(Bytes p)
    {
        return (p.at(0) == (byte)'/' && p.at(1) == (byte)'/');
    }

    /*
     * Recognize a line that starts with '{' or '}', or ends with ';', ',', '{' or '}'.
     * Don't consider "} else" a terminated line.
     * If a line begins with an "else", only consider it terminated if no unmatched
     * opening braces follow (handle "else { foo();" correctly).
     * Return the character terminating the line (ending char's have precedence if
     * both apply in order to determine initializations).
     */
    /*private*/ static byte cin_isterminated(Bytes s, boolean incl_open, boolean incl_comma)
        /* incl_open: include '{' at the end as terminator */
        /* incl_comma: recognize a trailing comma */
    {
        byte found_start = NUL;
        int n_open = 0;
        boolean is_else = false;

        s = cin_skipcomment(s);

        if (s.at(0) == (byte)'{' || (s.at(0) == (byte)'}' && !cin_iselse(s)))
            found_start = s.at(0);

        if (found_start == NUL)
            is_else = cin_iselse(s);

        while (s.at(0) != NUL)
        {
            /* skip over comments, "" strings and 'c'haracters */
            s = skip_string(cin_skipcomment(s));
            if (s.at(0) == (byte)'}' && 0 < n_open)
                --n_open;
            if ((!is_else || n_open == 0)
                    && (s.at(0) == (byte)';' || s.at(0) == (byte)'}' || (incl_comma && s.at(0) == (byte)','))
                    && cin_nocode(s.plus(1)))
                return s.at(0);
            else if (s.at(0) == (byte)'{')
            {
                if (incl_open && cin_nocode(s.plus(1)))
                    return s.at(0);
                else
                    n_open++;
            }

            if (s.at(0) != NUL)
                s = s.plus(1);
        }

        return found_start;
    }

    /*
     * Recognize the basic picture of a function declaration -- it needs to have
     * an open paren somewhere and a close paren at the end of the line and no semicolons anywhere.
     * When a line ends in a comma, we continue looking in the next line.
     * "sp" points to a string with the line.  When looking at other lines,
     * it must be restored to the line.  When it's null, fetch lines here.
     * "lnum" is where we start looking.
     * "min_lnum" is the line before which we will not be looking.
     */
    /*private*/ static boolean cin_isfuncdecl(Bytes[] sp, long first_lnum, long min_lnum)
    {
        long lnum = first_lnum;

        Bytes s;
        if (sp == null)
            s = ml_get(lnum);
        else
            s = sp[0];

        pos_C trypos;
        if (find_last_paren(s, '(', ')') && (trypos = find_match_paren(curbuf.b_ind_maxparen)) != null)
        {
            lnum = trypos.lnum;
            if (lnum < min_lnum)
                return false;

            s = ml_get(lnum);
        }

        /* Ignore line starting with #. */
        if (cin_ispreproc(s))
            return false;

        while (s.at(0) != NUL && s.at(0) != (byte)'(' && s.at(0) != (byte)';' && s.at(0) != (byte)'\'' && s.at(0) != (byte)'"')
        {
            if (cin_iscomment(s))           /* ignore comments */
                s = cin_skipcomment(s);
            else
                s = s.plus(1);
        }
        if (s.at(0) != (byte)'(')
            return false;           /* ';', ' or "  before any () or no '(' */

        boolean just_started = true;
        boolean retval = false;

        while (s.at(0) != NUL && s.at(0) != (byte)';' && s.at(0) != (byte)'\'' && s.at(0) != (byte)'"')
        {
            if (s.at(0) == (byte)')' && cin_nocode(s.plus(1)))
            {
                /* ')' at the end: may have found a match
                 * Check for he previous line not to end in a backslash:
                 *       #if defined(x) && \
                 *           defined(y)
                 */
                lnum = first_lnum - 1;
                s = ml_get(lnum);
                if (s.at(0) == NUL || s.at(strlen(s) - 1) != '\\')
                    retval = true;
                break;
            }

            if ((s.at(0) == (byte)',' && cin_nocode(s.plus(1))) || s.at(1) == NUL || cin_nocode(s))
            {
                boolean comma = (s.at(0) == (byte)',');

                /* ',' at the end: continue looking in the next line.
                 * At the end: check for ',' in the next line, for this style:
                 * func(arg1
                 *       , arg2) */
                for ( ; ; )
                {
                    if (curbuf.b_ml.ml_line_count <= lnum)
                        break;
                    s = ml_get(++lnum);
                    if (!cin_ispreproc(s))
                        break;
                }
                if (curbuf.b_ml.ml_line_count <= lnum)
                    break;
                /* Require a comma at end of the line or a comma or ')' at the start of next line. */
                s = skipwhite(s);
                if (!just_started && (!comma && s.at(0) != (byte)',' && s.at(0) != (byte)')'))
                    break;
                just_started = false;
            }
            else if (cin_iscomment(s))      /* ignore comments */
                s = cin_skipcomment(s);
            else
            {
                s = s.plus(1);
                just_started = false;
            }
        }

        if (lnum != first_lnum && sp != null)
            sp[0] = ml_get(first_lnum);

        return retval;
    }

    /*private*/ static boolean cin_isif(Bytes p)
    {
        return (STRNCMP(p, u8("if"), 2) == 0 && !vim_isIDc(p.at(2)));
    }

    /*private*/ static boolean cin_iselse(Bytes p)
    {
        if (p.at(0) == (byte)'}')          /* accept "} else" */
            p = cin_skipcomment(p.plus(1));

        return (STRNCMP(p, u8("else"), 4) == 0 && !vim_isIDc(p.at(4)));
    }

    /*private*/ static boolean cin_isdo(Bytes p)
    {
        return (STRNCMP(p, u8("do"), 2) == 0 && !vim_isIDc(p.at(2)));
    }

    /*
     * Check if this is a "while" that should have a matching "do".
     * We only accept a "while (condition) ;", with only white space between the ')' and ';'.
     * The condition may be spread over several lines.
     */
    /*private*/ static boolean cin_iswhileofdo(Bytes p, long lnum)
    {
        boolean retval = false;

        p = cin_skipcomment(p);
        if (p.at(0) == (byte)'}')                      /* accept "} while (cond);" */
            p = cin_skipcomment(p.plus(1));

        if (cin_starts_with(p, u8("while")))
        {
            pos_C cursor_save = new pos_C();
            COPY_pos(cursor_save, curwin.w_cursor);
            curwin.w_cursor.lnum = lnum;
            curwin.w_cursor.col = 0;

            /* skip any '}', until the 'w' of the "while" */
            for (p = ml_get_curline(); p.at(0) != NUL && p.at(0) != (byte)'w'; p = p.plus(1))
                curwin.w_cursor.col++;

            pos_C trypos = findmatchlimit(null, 0, 0, curbuf.b_ind_maxparen);
            if (trypos != null && cin_skipcomment(ml_get_pos(trypos).plus(1)).at(0) == (byte)';')
                retval = true;

            COPY_pos(curwin.w_cursor, cursor_save);
        }

        return retval;
    }

    /*
     * Check whether in "p" there is an "if", "for" or "while" before "*poffset".
     * Return false if there is none.
     * Otherwise return true and update "*poffset" to point to the place where the string was found.
     */
    /*private*/ static boolean cin_is_if_for_while_before_offset(Bytes line, int[] poffset)
    {
        int offset = poffset[0];

        if (offset-- < 2)
            return false;
        while (2 < offset && vim_iswhite(line.at(offset)))
            --offset;

        probablyFound:
        {
            offset -= 1;
            if (STRNCMP(line.plus(offset), u8("if"), 2) == 0)
                break probablyFound;

            if (1 <= offset)
            {
                offset -= 1;
                if (STRNCMP(line.plus(offset), u8("for"), 3) == 0)
                    break probablyFound;

                if (2 <= offset)
                {
                    offset -= 2;
                    if (STRNCMP(line.plus(offset), u8("while"), 5) == 0)
                        break probablyFound;
                }
            }
            return false;
        }

        if (offset == 0 || !vim_isIDc(line.at(offset - 1)))
        {
            poffset[0] = offset;
            return true;
        }
        return false;
    }

    /*
     * Return true if we are at the end of a do-while.
     *    do
     *       nothing;
     *    while (foo
     *             && bar);  <-- here
     * Adjust the cursor to the line with "while".
     */
    /*private*/ static boolean cin_iswhileofdo_end(int terminated)
    {
        if (terminated != ';')      /* there must be a ';' at the end */
            return false;

        Bytes line = ml_get_curline();
        for (Bytes p = line; p.at(0) != NUL; )
        {
            p = cin_skipcomment(p);
            if (p.at(0) == (byte)')')
            {
                Bytes s = skipwhite(p.plus(1));
                if (s.at(0) == (byte)';' && cin_nocode(s.plus(1)))
                {
                    /* Found ");" at end of the line,
                     * now check there is "while" before the matching '('. */
                    int i = BDIFF(p, line);
                    curwin.w_cursor.col = i;
                    pos_C trypos = find_match_paren(curbuf.b_ind_maxparen);
                    if (trypos != null)
                    {
                        s = cin_skipcomment(ml_get(trypos.lnum));
                        if (s.at(0) == (byte)'}')              /* accept "} while (cond);" */
                            s = cin_skipcomment(s.plus(1));
                        if (cin_starts_with(s, u8("while")))
                        {
                            curwin.w_cursor.lnum = trypos.lnum;
                            return true;
                        }
                    }

                    /* Searching may have made "line" invalid, get it again. */
                    line = ml_get_curline();
                    p = line.plus(i);
                }
            }
            if (p.at(0) != NUL)
                p = p.plus(1);
        }

        return false;
    }

    /*private*/ static boolean cin_isbreak(Bytes p)
    {
        return (STRNCMP(p, u8("break"), 5) == 0 && !vim_isIDc(p.at(5)));
    }

    /*
     * Find the position of a C++ base-class declaration or
     * constructor-initialization. eg:
     *
     * class MyClass :
     *      baseClass               <-- here
     * class MyClass : public baseClass,
     *      anotherBaseClass        <-- here (should probably lineup ??)
     * MyClass::MyClass(...) :
     *      baseClass(...)          <-- here (constructor-initialization)
     *
     * This is a lot of guessing.  Watch out for "cond ? func() : foo".
     */
    /*private*/ static boolean cin_is_cpp_baseclass(int[] col)
        /* col: return: column to align with */
    {
        long lnum = curwin.w_cursor.lnum;
        Bytes line = ml_get_curline();

        col[0] = 0;

        Bytes s = skipwhite(line);
        if (s.at(0) == (byte)'#')              /* skip #define FOO x ? (x) : x */
            return false;
        s = cin_skipcomment(s);
        if (s.at(0) == NUL)
            return false;

        boolean class_or_struct = false;
        boolean lookfor_ctor_init = false;
        boolean cpp_base_class = false;

        /* Search for a line starting with '#', empty, ending in ';' or containing
         * '{' or '}' and start below it.  This handles the following situations:
         *  a = cond ?
         *        func() :
         *             asdf;
         *  func::foo()
         *        : something
         *  {}
         *  Foo::Foo (int one, int two)
         *          : something(4),
         *          somethingelse(3)
         *  {}
         */
        while (1 < lnum)
        {
            line = ml_get(lnum - 1);
            s = skipwhite(line);
            if (s.at(0) == (byte)'#' || s.at(0) == NUL)
                break;
            while (s.at(0) != NUL)
            {
                s = cin_skipcomment(s);
                if (s.at(0) == (byte)'{' || s.at(0) == (byte)'}' || (s.at(0) == (byte)';' && cin_nocode(s.plus(1))))
                    break;
                if (s.at(0) != NUL)
                    s = s.plus(1);
            }
            if (s.at(0) != NUL)
                break;
            --lnum;
        }

        line = ml_get(lnum);
        s = cin_skipcomment(line);
        for ( ; ; )
        {
            if (s.at(0) == NUL)
            {
                if (lnum == curwin.w_cursor.lnum)
                    break;
                /* Continue in the cursor line. */
                line = ml_get(++lnum);
                s = cin_skipcomment(line);
                if (s.at(0) == NUL)
                    continue;
            }

            if (s.at(0) == (byte)'"')
                s = skip_string(s).plus(1);
            else if (s.at(0) == (byte)':')
            {
                if (s.at(1) == (byte)':')
                {
                    /* Skip double colon: it can't be a constructor initialization any more. */
                    lookfor_ctor_init = false;
                    s = cin_skipcomment(s.plus(2));
                }
                else if (lookfor_ctor_init || class_or_struct)
                {
                    /* We have found something, that looks like the start of
                     * cpp-base-class-declaration or constructor-initialization. */
                    cpp_base_class = true;
                    lookfor_ctor_init = class_or_struct = false;
                    col[0] = 0;
                    s = cin_skipcomment(s.plus(1));
                }
                else
                    s = cin_skipcomment(s.plus(1));
            }
            else if ((STRNCMP(s, u8("class"), 5) == 0 && !vim_isIDc(s.at(5)))
                  || (STRNCMP(s, u8("struct"), 6) == 0 && !vim_isIDc(s.at(6))))
            {
                class_or_struct = true;
                lookfor_ctor_init = false;

                if (s.at(0) == (byte)'c')
                    s = cin_skipcomment(s.plus(5));
                else
                    s = cin_skipcomment(s.plus(6));
            }
            else
            {
                if (s.at(0) == (byte)'{' || s.at(0) == (byte)'}' || s.at(0) == (byte)';')
                {
                    cpp_base_class = lookfor_ctor_init = class_or_struct = false;
                }
                else if (s.at(0) == (byte)')')
                {
                    /* Constructor-initialization is assumed if we come across something like "):". */
                    class_or_struct = false;
                    lookfor_ctor_init = true;
                }
                else if (s.at(0) == (byte)'?')
                {
                    /* Avoid seeing '() :' after '?' as constructor init. */
                    return false;
                }
                else if (!vim_isIDc(s.at(0)))
                {
                    /* if it is not an identifier, we are wrong */
                    class_or_struct = false;
                    lookfor_ctor_init = false;
                }
                else if (col[0] == 0)
                {
                    /* it can't be a constructor-initialization any more */
                    lookfor_ctor_init = false;

                    /* the first statement starts here: lineup with this one... */
                    if (cpp_base_class)
                        col[0] = BDIFF(s, line);
                }

                /* When the line ends in a comma don't align with it. */
                if (lnum == curwin.w_cursor.lnum && s.at(0) == (byte)',' && cin_nocode(s.plus(1)))
                    col[0] = 0;

                s = cin_skipcomment(s.plus(1));
            }
        }

        return cpp_base_class;
    }

    /*private*/ static int get_baseclass_amount(int col)
    {
        int amount;

        if (col == 0)
        {
            amount = get_indent();
            pos_C trypos;
            if (find_last_paren(ml_get_curline(), '(', ')')
                    && (trypos = find_match_paren(curbuf.b_ind_maxparen)) != null)
                amount = get_indent_lnum(trypos.lnum);
            if (!cin_ends_in(ml_get_curline(), u8(","), null))
                amount += curbuf.b_ind_cpp_baseclass;
        }
        else
        {
            curwin.w_cursor.col = col;
            int[] vcol = new int[1];
            getvcol(curwin, curwin.w_cursor, vcol, null, null);
            amount = vcol[0];
        }

        if (amount < curbuf.b_ind_cpp_baseclass)
            amount = curbuf.b_ind_cpp_baseclass;

        return amount;
    }

    /*
     * Return true if string "s" ends with the string "find",
     * possibly followed by white space and comments.  Skip strings and comments.
     * Ignore "ignore" after "find" if it's not null.
     */
    /*private*/ static boolean cin_ends_in(Bytes s, Bytes find, Bytes ignore)
    {
        int len = strlen(find);

        for (Bytes p = s; p.at(0) != NUL; )
        {
            p = cin_skipcomment(p);
            if (STRNCMP(p, find, len) == 0)
            {
                Bytes r = skipwhite(p.plus(len));
                if (ignore != null && STRNCMP(r, ignore, strlen(ignore)) == 0)
                    r = skipwhite(r.plus(strlen(ignore)));
                if (cin_nocode(r))
                    return true;
            }
            if (p.at(0) != NUL)
                p = p.plus(1);
        }

        return false;
    }

    /*
     * Return true when "s" starts with "word" and then a non-ID character.
     */
    /*private*/ static boolean cin_starts_with(Bytes s, Bytes word)
    {
        int len = strlen(word);

        return (STRNCMP(s, word, len) == 0 && !vim_isIDc(s.at(len)));
    }

    /*
     * Skip strings, chars and comments until at or past "trypos".
     * Return the column found.
     */
    /*private*/ static int cin_skip2pos(pos_C trypos)
    {
        Bytes line = ml_get(trypos.lnum);

        Bytes p = line;
        while (p.at(0) != NUL && BDIFF(p, line) < trypos.col)
        {
            if (cin_iscomment(p))
                p = cin_skipcomment(p);
            else
            {
                p = skip_string(p);
                p = p.plus(1);
            }
        }

        return BDIFF(p, line);
    }

    /*private*/ static pos_C _2_pos_copy = new pos_C();

    /*
     * Find the '{' at the start of the block we are in.
     * Return null if no match found.
     * Ignore a '{' that is in a comment, makes indenting the next three lines work. */
    /* foo()    */
    /* {        */
    /* }        */

    /*private*/ static pos_C find_start_brace()
    {
        pos_C trypos;
        pos_C cursor_save = new pos_C();
        COPY_pos(cursor_save, curwin.w_cursor);

        while ((trypos = findmatchlimit(null, '{', FM_BLOCKSTOP, 0)) != null)
        {
            COPY_pos(_2_pos_copy, trypos); /* copy pos_C, next findmatch will change it */
            trypos = _2_pos_copy;
            COPY_pos(curwin.w_cursor, trypos);
            pos_C pos = null;
            /* ignore the { if it's in a // or / *  * / comment */
            if (cin_skip2pos(trypos) == trypos.col && (pos = ind_find_start_comment()) == null)
                break;
            if (pos != null)
                curwin.w_cursor.lnum = pos.lnum;
        }

        COPY_pos(curwin.w_cursor, cursor_save);
        return trypos;
    }

    /*
     * Find the matching '(', ignoring it if it is in a comment.
     * Return null if no match found.
     */
    /*private*/ static pos_C find_match_paren(int ind_maxparen)
    {
        return find_match_char('(', ind_maxparen);
    }

    /*private*/ static pos_C _3_pos_copy = new pos_C();

    /*private*/ static pos_C find_match_char(int c, int ind_maxparen)
    {
        pos_C trypos;

        pos_C cursor_save = new pos_C();
        COPY_pos(cursor_save, curwin.w_cursor);
        int ind_maxp_wk = ind_maxparen;

        retry:
        for ( ; ; )
        {
            if ((trypos = findmatchlimit(null, c, 0, ind_maxp_wk)) != null)
            {
                /* check if the ( is in a // comment */
                if (trypos.col < cin_skip2pos(trypos))
                {
                    ind_maxp_wk = ind_maxparen - (int)(cursor_save.lnum - trypos.lnum);
                    if (0 < ind_maxp_wk)
                    {
                        COPY_pos(curwin.w_cursor, trypos);
                        curwin.w_cursor.col = 0;
                        continue retry;
                    }
                    trypos = null;
                }
                else
                {
                    COPY_pos(_3_pos_copy, trypos); /* copy trypos, findmatch will change it */
                    trypos = _3_pos_copy;
                    COPY_pos(curwin.w_cursor, trypos);

                    pos_C trypos_wk = ind_find_start_comment();
                    if (trypos_wk != null)
                    {
                        ind_maxp_wk = ind_maxparen - (int)(cursor_save.lnum - trypos_wk.lnum);
                        if (0 < ind_maxp_wk)
                        {
                            COPY_pos(curwin.w_cursor, trypos_wk);
                            continue retry;
                        }
                        trypos = null;
                    }
                }
            }

            break;
        }

        COPY_pos(curwin.w_cursor, cursor_save);
        return trypos;
    }

    /*
     * Find the matching '(', ignoring it if it is in a comment or before an unmatched {.
     * Return null if no match found.
     */
    /*private*/ static pos_C find_match_paren_after_brace(int ind_maxparen)
    {
        pos_C trypos = find_match_paren(ind_maxparen);

        if (trypos != null)
        {
            pos_C brace = find_start_brace();

            /* If both an unmatched '(' and '{' is found, ignore the '(' position if the '{' is further down. */
            if (brace != null
                    && (trypos.lnum != brace.lnum ? trypos.lnum < brace.lnum : trypos.col < brace.col))
                trypos = null;
        }

        return trypos;
    }

    /*
     * Return ind_maxparen corrected for the difference in line number between the
     * cursor position and "startpos".  This makes sure that searching for a
     * matching paren above the cursor line doesn't find a match because of
     * looking a few lines further.
     */
    /*private*/ static int corr_ind_maxparen(pos_C startpos)
    {
        long n = startpos.lnum - curwin.w_cursor.lnum;

        if (0 < n && n < curbuf.b_ind_maxparen / 2)
            return curbuf.b_ind_maxparen - (int)n;

        return curbuf.b_ind_maxparen;
    }

    /*
     * Set w_cursor.col to the column number of the last unmatched ')' or '{' in line "p".
     * "p" must point to the start of the line.
     */
    /*private*/ static boolean find_last_paren(Bytes p, int start, int end)
    {
        boolean retval = false;
        int open_count = 0;

        curwin.w_cursor.col = 0;                        /* default is start of line */

        for (int i = 0; p.at(i) != NUL; i++)
        {
            i = BDIFF(cin_skipcomment(p.plus(i)), p);      /* ignore parens in comments */
            i = BDIFF(skip_string(p.plus(i)), p);          /* ignore parens in quotes */
            if (p.at(i) == start)
                open_count++;
            else if (p.at(i) == end)
            {
                if (0 < open_count)
                    --open_count;
                else
                {
                    curwin.w_cursor.col = i;
                    retval = true;
                }
            }
        }

        return retval;
    }

    /*
     * Parse 'cinoptions' and set the values in "curbuf".
     * Must be called when 'cinoptions', 'shiftwidth' and/or 'tabstop' changes.
     */
    /*private*/ static void parse_cino(buffer_C buf)
    {
        int sw = (int)get_sw_value(buf);

        /*
         * Set the default values.
         */

        /* Spaces from a block's opening brace the prevailing indent for that block should be. */
        buf.b_ind_level = sw;

        /* Spaces from the edge of the line an open brace that's at the end of a line is imagined to be. */
        buf.b_ind_open_imag = 0;

        /* Spaces from the prevailing indent for a line that is not preceded by an opening brace. */
        buf.b_ind_no_brace = 0;

        /* Column where the first { of a function should be located }. */
        buf.b_ind_first_open = 0;

        /* Spaces from the prevailing indent a leftmost open brace should be located. */
        buf.b_ind_open_extra = 0;

        /* Spaces from the matching open brace (real location for one at the left edge;
         * imaginary location from one that ends a line) the matching close brace should be located. */
        buf.b_ind_close_extra = 0;

        /* Spaces from the edge of the line an open brace sitting in the leftmost column is imagined to be. */
        buf.b_ind_open_left_imag = 0;

        /* Spaces jump labels should be shifted to the left if N is non-negative,
         * otherwise the jump label will be put to column 1. */
        buf.b_ind_jump_label = -1;

        /* Spaces from the switch() indent a "case xx" label should be located. */
        buf.b_ind_case = sw;

        /* Spaces from the "case xx:" code after a switch() should be located. */
        buf.b_ind_case_code = sw;

        /* Lineup break at end of case in switch() with case label. */
        buf.b_ind_case_break = 0;

        /* Spaces from the class declaration indent a scope declaration label should be located. */
        buf.b_ind_scopedecl = sw;

        /* Spaces from the scope declaration label code should be located. */
        buf.b_ind_scopedecl_code = sw;

        /* Amount K&R-style parameters should be indented. */
        buf.b_ind_param = sw;

        /* Amount a function type spec should be indented. */
        buf.b_ind_func_type = sw;

        /* Amount a cpp base class declaration or constructor initialization should be indented. */
        buf.b_ind_cpp_baseclass = sw;

        /* additional spaces beyond the prevailing indent a continuation line should be located. */
        buf.b_ind_continuation = sw;

        /* Spaces from the indent of the line with an unclosed parentheses. */
        buf.b_ind_unclosed = sw * 2;

        /* Spaces from the indent of the line with an unclosed parentheses, which itself is also unclosed. */
        buf.b_ind_unclosed2 = sw;

        /* Suppress ignoring spaces from the indent of a line starting with an unclosed parentheses. */
        buf.b_ind_unclosed_noignore = 0;

        /* If the opening paren is the last nonwhite character on the line, and b_ind_unclosed_wrapped
         * is nonzero, use this indent relative to the outer context (for very long lines). */
        buf.b_ind_unclosed_wrapped = 0;

        /* Suppress ignoring white space when lining up with the character after an unclosed parentheses. */
        buf.b_ind_unclosed_whiteok = 0;

        /* Indent a closing parentheses under the line start of the matching opening parentheses. */
        buf.b_ind_matching_paren = 0;

        /* Indent a closing parentheses under the previous line. */
        buf.b_ind_paren_prev = 0;

        /* Extra indent for comments. */
        buf.b_ind_comment = 0;

        /* Spaces from the comment opener when there is nothing after it. */
        buf.b_ind_in_comment = 3;

        /* Boolean: if non-zero, use b_ind_in_comment even if there is something after the comment opener. */
        buf.b_ind_in_comment2 = 0;

        /* Max lines to search for an open paren. */
        buf.b_ind_maxparen = 20;

        /* Max lines to search for an open comment. */
        buf.b_ind_maxcomment = 70;

        /* Handle braces for java code. */
        buf.b_ind_java = 0;

        /* Not to confuse JS object properties with labels. */
        buf.b_ind_js = 0;

        /* Handle blocked cases correctly. */
        buf.b_ind_keep_case_label = 0;

        /* Handle C++ namespace. */
        buf.b_ind_cpp_namespace = 0;

        /* Handle continuation lines containing conditions of if(), for() and while(). */
        buf.b_ind_if_for_while = 0;

        int fraction = 0;

        for (Bytes p = buf.b_p_cino[0]; p.at(0) != NUL; )
        {
            Bytes l = p;
            p = p.plus(1);
            if (p.at(0) == (byte)'-')
                p = p.plus(1);
            Bytes digits = p;              /* remember where the digits start */
            int n;
            { Bytes[] __ = { p }; n = (int)getdigits(__); p = __[0]; }
            int divider = 0;
            if (p.at(0) == (byte)'.')                  /* ".5s" means a fraction */
            {
                fraction = libC.atoi(p = p.plus(1));
                while (asc_isdigit(p.at(0)))
                {
                    p = p.plus(1);
                    if (divider != 0)
                        divider *= 10;
                    else
                        divider = 10;
                }
            }
            if (p.at(0) == (byte)'s')                  /* "2s" means two times 'shiftwidth' */
            {
                if (BEQ(p, digits))
                    n = sw;                 /* just "s" is one 'shiftwidth' */
                else
                {
                    n *= sw;
                    if (divider != 0)
                        n += (sw * fraction + divider / 2) / divider;
                }
                p = p.plus(1);
            }
            if (l.at(1) == (byte)'-')
                n = -n;

            /* When adding an entry here, also update the default 'cinoptions'
             * in doc/indent.txt, and add explanation for it! */
            switch (l.at(0))
            {
                case '>': buf.b_ind_level = n; break;
                case 'e': buf.b_ind_open_imag = n; break;
                case 'n': buf.b_ind_no_brace = n; break;
                case 'f': buf.b_ind_first_open = n; break;
                case '{': buf.b_ind_open_extra = n; break;
                case '}': buf.b_ind_close_extra = n; break;
                case '^': buf.b_ind_open_left_imag = n; break;
                case 'L': buf.b_ind_jump_label = n; break;
                case ':': buf.b_ind_case = n; break;
                case '=': buf.b_ind_case_code = n; break;
                case 'b': buf.b_ind_case_break = n; break;
                case 'p': buf.b_ind_param = n; break;
                case 't': buf.b_ind_func_type = n; break;
                case '/': buf.b_ind_comment = n; break;
                case 'c': buf.b_ind_in_comment = n; break;
                case 'C': buf.b_ind_in_comment2 = n; break;
                case 'i': buf.b_ind_cpp_baseclass = n; break;
                case '+': buf.b_ind_continuation = n; break;
                case '(': buf.b_ind_unclosed = n; break;
                case 'u': buf.b_ind_unclosed2 = n; break;
                case 'U': buf.b_ind_unclosed_noignore = n; break;
                case 'W': buf.b_ind_unclosed_wrapped = n; break;
                case 'w': buf.b_ind_unclosed_whiteok = n; break;
                case 'm': buf.b_ind_matching_paren = n; break;
                case 'M': buf.b_ind_paren_prev = n; break;
                case ')': buf.b_ind_maxparen = n; break;
                case '*': buf.b_ind_maxcomment = n; break;
                case 'g': buf.b_ind_scopedecl = n; break;
                case 'h': buf.b_ind_scopedecl_code = n; break;
                case 'j': buf.b_ind_java = n; break;
                case 'J': buf.b_ind_js = n; break;
                case 'l': buf.b_ind_keep_case_label = n; break;
                case '#': buf.b_ind_hash_comment = n; break;
                case 'N': buf.b_ind_cpp_namespace = n; break;
                case 'k': buf.b_ind_if_for_while = n; break;
            }
            if (p.at(0) == (byte)',')
                p = p.plus(1);
        }
    }

    /*private*/ static final int
        LOOKFOR_INITIAL = 0,
        LOOKFOR_IF = 1,
        LOOKFOR_DO = 2,
        LOOKFOR_CASE = 3,
        LOOKFOR_ANY = 4,
        LOOKFOR_TERM = 5,
        LOOKFOR_UNTERM = 6,
        LOOKFOR_SCOPEDECL = 7,
        LOOKFOR_NOBREAK = 8,
        LOOKFOR_CPP_BASECLASS = 9,
        LOOKFOR_ENUM_OR_INIT = 10,
        LOOKFOR_JS_KEY = 11,
        LOOKFOR_COMMA = 12;

    /*private*/ static final getindent_C get_c_indent = new getindent_C()
    {
        public int getindent()
        {
            int amount;

            int cur_amount = MAXCOL;

            int cont_amount = 0;                /* amount for continuation line */
            int added_to_amount = 0;

            /* make a copy, value is changed below */
            int ind_continuation = curbuf.b_ind_continuation;

            /* remember where the cursor was when we started */
            pos_C cur_curpos = new pos_C();
            COPY_pos(cur_curpos, curwin.w_cursor);

            /* if we are at line 1 0 is fine, right? */
            if (cur_curpos.lnum == 1)
                return 0;

            /* Get a copy of the current contents of the line.
             * This is required, because only the most recent line obtained with ml_get() is valid! */
            Bytes linecopy = STRDUP(ml_get(cur_curpos.lnum));

            /*
             * In insert mode and the cursor is on a ')' truncate the line at the cursor position.
             * We don't want to line up with the matching '(' when inserting new stuff.
             * For unknown reasons the cursor might be past the end of the line, thus check for that.
             */
            if ((State & INSERT) != 0
                    && curwin.w_cursor.col < strlen(linecopy)
                    && linecopy.at(curwin.w_cursor.col) == (byte)')')
                linecopy.be(curwin.w_cursor.col, NUL);

            Bytes theline = skipwhite(linecopy);

            /* move the cursor to the start of the line */

            curwin.w_cursor.col = 0;

            boolean original_line_islabel = cin_islabel();

            pos_C trypos;
            pos_C tryposBrace = null;

            theend:
            {
                /*
                 * #defines and so on always go at the left when included in 'cinkeys'.
                 */
                if (theline.at(0) == (byte)'#' && (linecopy.at(0) == (byte)'#' || in_cinkeys('#', ' ', true)))
                {
                    amount = curbuf.b_ind_hash_comment;
                }
                /*
                 * Is it a non-case label?  Then that goes at the left margin too unless:
                 *  - JS flag is set.
                 *  - 'L' item has a positive value.
                 */
                else if (original_line_islabel && curbuf.b_ind_js == 0 && curbuf.b_ind_jump_label < 0)
                {
                    amount = 0;
                }
                /*
                 * If we're inside a "//" comment and there is a "//" comment in a previous line,
                 * lineup with that one.
                 */
                else if (cin_islinecomment(theline) && (trypos = find_line_comment()) != null)
                {
                    /* find how indented the line beginning the comment is */
                    int[] col = new int[1];
                    getvcol(curwin, trypos, col, null, null);
                    amount = col[0];
                }
                /*
                 * If we're inside a comment and not looking at the start of the comment,
                 * try using the 'comments' option.
                 */
                else if (!cin_iscomment(theline) && (trypos = ind_find_start_comment()) != null)
                {
                    int lead_start_len = 2;
                    int lead_middle_len = 1;
                    Bytes lead_start = new Bytes(COM_MAX_LEN);  /* start-comment string */
                    Bytes lead_middle = new Bytes(COM_MAX_LEN); /* middle-comment string */
                    Bytes lead_end = new Bytes(COM_MAX_LEN);    /* end-comment string */
                    int start_align = 0;
                    int start_off = 0;
                    boolean done = false;

                    /* find how indented the line beginning the comment is */
                    int[] col = new int[1];
                    getvcol(curwin, trypos, col, null, null);
                    amount = col[0];
                    lead_start.be(0, NUL);
                    lead_middle.be(0, NUL);

                    for (Bytes[] p = { curbuf.b_p_com[0] }; p[0].at(0) != NUL; )
                    {
                        int align = 0;
                        int off = 0;
                        int what = 0;

                        while (p[0].at(0) != NUL && p[0].at(0) != (byte)':')
                        {
                            if (p[0].at(0) == COM_START || p[0].at(0) == COM_END || p[0].at(0) == COM_MIDDLE)
                                what = (p[0] = p[0].plus(1)).at(-1);
                            else if (p[0].at(0) == COM_LEFT || p[0].at(0) == COM_RIGHT)
                                align = (p[0] = p[0].plus(1)).at(-1);
                            else if (asc_isdigit(p[0].at(0)) || p[0].at(0) == (byte)'-')
                                off = (int)getdigits(p);
                            else
                                p[0] = p[0].plus(1);
                        }

                        if (p[0].at(0) == (byte)':')
                            p[0] = p[0].plus(1);
                        copy_option_part(p, lead_end, COM_MAX_LEN, u8(","));
                        if (what == COM_START)
                        {
                            STRCPY(lead_start, lead_end);
                            lead_start_len = strlen(lead_start);
                            start_off = off;
                            start_align = align;
                        }
                        else if (what == COM_MIDDLE)
                        {
                            STRCPY(lead_middle, lead_end);
                            lead_middle_len = strlen(lead_middle);
                        }
                        else if (what == COM_END)
                        {
                            /* If our line starts with the middle comment string,
                             * line it up with the comment opener per the 'comments' option. */
                            if (STRNCMP(theline, lead_middle, lead_middle_len) == 0
                             && STRNCMP(theline, lead_end, strlen(lead_end)) != 0)
                            {
                                done = true;
                                if (1 < curwin.w_cursor.lnum)
                                {
                                    /* If the start comment string matches in the previous line,
                                     * use the indent of that line plus offset.
                                     * If the middle comment string matches in the previous line,
                                     * use the indent of that line. */
                                    Bytes look = skipwhite(ml_get(curwin.w_cursor.lnum - 1));
                                    if (STRNCMP(look, lead_start, lead_start_len) == 0)
                                        amount = get_indent_lnum(curwin.w_cursor.lnum - 1);
                                    else if (STRNCMP(look, lead_middle, lead_middle_len) == 0)
                                    {
                                        amount = get_indent_lnum(curwin.w_cursor.lnum - 1);
                                        break;
                                    }
                                    /* If the start comment string doesn't match with
                                     * the start of the comment, skip this entry. */
                                    else if (STRNCMP(ml_get(trypos.lnum).plus(trypos.col), lead_start, lead_start_len) != 0)
                                        continue;
                                }
                                if (start_off != 0)
                                    amount += start_off;
                                else if (start_align == COM_RIGHT)
                                    amount += mb_string2cells(lead_start, -1) - mb_string2cells(lead_middle, -1);
                                break;
                            }

                            /* If our line starts with the end comment string,
                             * line it up with the middle comment. */
                            if (STRNCMP(theline, lead_middle, lead_middle_len) != 0
                             && STRNCMP(theline, lead_end, strlen(lead_end)) == 0)
                            {
                                amount = get_indent_lnum(curwin.w_cursor.lnum - 1);

                                if (off != 0)
                                    amount += off;
                                else if (align == COM_RIGHT)
                                    amount += mb_string2cells(lead_start, -1) - mb_string2cells(lead_middle, -1);
                                done = true;
                                break;
                            }
                        }
                    }

                    /* If our line starts with an asterisk, line up with the asterisk in the comment opener;
                     * otherwise, line up with the first character of the comment text.
                     */
                    if (done)
                        ;
                    else if (theline.at(0) == (byte)'*')
                        amount += 1;
                    else
                    {
                        /*
                         * If we are more than one line away from the comment opener, take the indent
                         * of the previous non-empty line.  If 'cino' has "CO" and we are just below
                         * the comment opener and there are any white characters after it line up with
                         * the text after it; otherwise, add the amount specified by "c" in 'cino'.
                         */
                        amount = -1;
                        for (long lnum = cur_curpos.lnum - 1; trypos.lnum < lnum; --lnum)
                        {
                            if (linewhite(lnum))                        /* skip blank lines */
                                continue;
                            amount = get_indent_lnum(lnum);
                            break;
                        }
                        if (amount == -1)                               /* use the comment opener */
                        {
                            Bytes look = null;
                            if (curbuf.b_ind_in_comment2 == 0)
                            {
                                Bytes start = ml_get(trypos.lnum);
                                look = start.plus(trypos.col + 2);          /* skip / and * */
                                if (look.at(0) != NUL)                       /* if something after it */
                                    trypos.col = BDIFF(skipwhite(look), start);
                            }
                            getvcol(curwin, trypos, col, null, null);
                            amount = col[0];
                            if (curbuf.b_ind_in_comment2 != 0 || look.at(0) == NUL)
                                amount += curbuf.b_ind_in_comment;
                        }
                    }
                }
                /*
                 * Are we looking at a ']' that has a match?
                 */
                else if (skipwhite(theline).at(0) == (byte)']'
                        && (trypos = find_match_char('[', curbuf.b_ind_maxparen)) != null)
                {
                    /* Align with the line containing the '['. */
                    amount = get_indent_lnum(trypos.lnum);
                }
                /*
                 * Are we inside parentheses or braces?
                 */
                else if (((trypos = find_match_paren(curbuf.b_ind_maxparen)) != null && curbuf.b_ind_java == 0)
                        || (tryposBrace = find_start_brace()) != null
                        || trypos != null)
                {
                    if (trypos != null && tryposBrace != null)
                    {
                        /* Both an unmatched '(' and '{' is found.  Use the one which is
                         * closer to the current cursor position, set the other to null. */
                        if (trypos.lnum != tryposBrace.lnum
                                ? trypos.lnum < tryposBrace.lnum
                                : trypos.col < tryposBrace.col)
                            trypos = null;
                        else
                            tryposBrace = null;
                    }

                    if (trypos != null)
                    {
                        pos_C our_paren_pos = new pos_C();

                        /*
                         * If the matching paren is more than one line away,
                         * use the indent of a previous non-empty line that matches the same paren.
                         */
                        if (theline.at(0) == (byte)')' && curbuf.b_ind_paren_prev != 0)
                        {
                            /* Line up with the start of the matching paren line. */
                            amount = get_indent_lnum(curwin.w_cursor.lnum - 1);
                        }
                        else
                        {
                            amount = -1;
                            COPY_pos(our_paren_pos, trypos);
                            for (long[] lnum = { cur_curpos.lnum - 1 }; our_paren_pos.lnum < lnum[0]; --lnum[0])
                            {
                                Bytes[] lp = { skipwhite(ml_get(lnum[0])) };
                                if (cin_nocode(lp[0]))                  /* skip comment lines */
                                    continue;
                                if (cin_ispreproc_cont(lp, lnum))
                                    continue;                       /* ignore #define, #if, etc. */
                                curwin.w_cursor.lnum = lnum[0];

                                /* Skip a comment. */
                                if ((trypos = ind_find_start_comment()) != null)
                                {
                                    lnum[0] = trypos.lnum + 1;
                                    continue;
                                }

                                if ((trypos = find_match_paren(corr_ind_maxparen(cur_curpos))) != null
                                        && trypos.lnum == our_paren_pos.lnum
                                        && trypos.col == our_paren_pos.col)
                                {
                                    amount = get_indent_lnum(lnum[0]);

                                    if (theline.at(0) == (byte)')')
                                    {
                                        if (our_paren_pos.lnum != lnum[0] && amount < cur_amount)
                                            cur_amount = amount;
                                        amount = -1;
                                    }
                                    break;
                                }
                            }
                        }

                        /*
                         * Line up with line where the matching paren is.
                         * If the line starts with a '(' or the indent for unclosed parentheses is zero,
                         * line up with the unclosed parentheses.
                         */
                        if (amount == -1)
                        {
                            int ignore_paren_col = 0;
                            boolean is_if_for_while = false;

                            if (curbuf.b_ind_if_for_while != 0)
                            {
                                /* Look for the outermost opening parenthesis on this line
                                 * and check whether it belongs to an "if", "for" or "while". */

                                pos_C cursor_save = new pos_C();
                                COPY_pos(cursor_save, curwin.w_cursor);
                                pos_C outermost = new pos_C();

                                trypos = our_paren_pos;
                                do
                                {
                                    COPY_pos(outermost, trypos);
                                    curwin.w_cursor.lnum = outermost.lnum;
                                    curwin.w_cursor.col = outermost.col;

                                    trypos = find_match_paren(curbuf.b_ind_maxparen);
                                } while (trypos != null && trypos.lnum == outermost.lnum);

                                COPY_pos(curwin.w_cursor, cursor_save);

                                Bytes line = ml_get(outermost.lnum);

                                { int[] __ = { outermost.col }; is_if_for_while = cin_is_if_for_while_before_offset(line, __); outermost.col = __[0]; }
                            }

                            Bytes look;

                            { Bytes[] __ = new Bytes[1]; amount = skip_label(our_paren_pos.lnum, __); look = __[0]; }
                            look = skipwhite(look);
                            if (look.at(0) == (byte)'(')
                            {
                                long save_lnum = curwin.w_cursor.lnum;

                                /* Ignore a '(' in front of the line that has a match before our matching '('. */
                                curwin.w_cursor.lnum = our_paren_pos.lnum;
                                Bytes line = ml_get_curline();
                                int look_col = BDIFF(look, line);
                                curwin.w_cursor.col = look_col + 1;
                                trypos = findmatchlimit(null, ')', 0, curbuf.b_ind_maxparen);
                                if (trypos != null
                                        && trypos.lnum == our_paren_pos.lnum
                                        && trypos.col < our_paren_pos.col)
                                    ignore_paren_col = trypos.col + 1;

                                curwin.w_cursor.lnum = save_lnum;
                                look = ml_get(our_paren_pos.lnum).plus(look_col);
                            }

                            if (theline.at(0) == (byte)')'
                                || (curbuf.b_ind_unclosed == 0 && is_if_for_while == false)
                                || (curbuf.b_ind_unclosed_noignore == 0 && look.at(0) == (byte)'(' && ignore_paren_col == 0))
                            {
                                /*
                                 * If we're looking at a close paren, line up right there;
                                 * otherwise, line up with the next (non-white) character.
                                 * When b_ind_unclosed_wrapped is set and the matching paren is
                                 * the last nonwhite character of the line, use either the indent
                                 * of the current line or the indentation of the next outer paren
                                 * and add b_ind_unclosed_wrapped (for very long lines).
                                 */
                                if (theline.at(0) != (byte)')')
                                {
                                    cur_amount = MAXCOL;
                                    Bytes l = ml_get(our_paren_pos.lnum);
                                    if (curbuf.b_ind_unclosed_wrapped != 0 && cin_ends_in(l, u8("("), null))
                                    {
                                        /* look for opening unmatched paren,
                                         * indent one level for each additional level */
                                        int n = 1;
                                        for (int col = 0; col < our_paren_pos.col; col++)
                                        {
                                            switch (l.at(col))
                                            {
                                                case '(':
                                                case '{': ++n;
                                                        break;

                                                case ')':
                                                case '}': if (1 < n)
                                                            --n;
                                                        break;
                                            }
                                        }

                                        our_paren_pos.col = 0;
                                        amount += n * curbuf.b_ind_unclosed_wrapped;
                                    }
                                    else if (curbuf.b_ind_unclosed_whiteok != 0)
                                        our_paren_pos.col++;
                                    else
                                    {
                                        int col = our_paren_pos.col + 1;
                                        while (vim_iswhite(l.at(col)))
                                            col++;
                                        if (l.at(col) != NUL)              /* in case of trailing space */
                                            our_paren_pos.col = col;
                                        else
                                            our_paren_pos.col++;
                                    }
                                }

                                /*
                                 * Find how indented the paren is, or the character after it
                                 * if we did the above "if".
                                 */
                                if (0 < our_paren_pos.col)
                                {
                                    int[] col = new int[1];
                                    getvcol(curwin, our_paren_pos, col, null, null);
                                    if (cur_amount > col[0])
                                        cur_amount = col[0];
                                }
                            }

                            if (theline.at(0) == (byte)')' && curbuf.b_ind_matching_paren != 0)
                            {
                                /* Line up with the start of the matching paren line. */
                            }
                            else if ((curbuf.b_ind_unclosed == 0 && is_if_for_while == false)
                                || (curbuf.b_ind_unclosed_noignore == 0 && look.at(0) == (byte)'(' && ignore_paren_col == 0))
                            {
                                if (cur_amount != MAXCOL)
                                    amount = cur_amount;
                            }
                            else
                            {
                                /* Add b_ind_unclosed2 for each '(' before our matching one,
                                 * but ignore (void) before the line (ignore_paren_col). */
                                int col = our_paren_pos.col;
                                while (ignore_paren_col < our_paren_pos.col)
                                {
                                    --our_paren_pos.col;
                                    switch (ml_get_pos(our_paren_pos).at(0))
                                    {
                                        case '(': amount += curbuf.b_ind_unclosed2;
                                                col = our_paren_pos.col;
                                                break;
                                        case ')': amount -= curbuf.b_ind_unclosed2;
                                                col = MAXCOL;
                                                break;
                                    }
                                }

                                /* Use b_ind_unclosed once, when the first '(' is not inside braces. */
                                if (col == MAXCOL)
                                    amount += curbuf.b_ind_unclosed;
                                else
                                {
                                    curwin.w_cursor.lnum = our_paren_pos.lnum;
                                    curwin.w_cursor.col = col;
                                    if (find_match_paren_after_brace(curbuf.b_ind_maxparen) != null)
                                        amount += curbuf.b_ind_unclosed2;
                                    else if (is_if_for_while)
                                        amount += curbuf.b_ind_if_for_while;
                                    else
                                        amount += curbuf.b_ind_unclosed;
                                }
                                /*
                                 * For a line starting with ')' use the minimum of the two
                                 * positions, to avoid giving it more indent than the previous
                                 * lines:
                                 *  func_long_name(                 if (x
                                 *      arg                                 && yy
                                 *      )         ^ not here           )    ^ not here
                                 */
                                if (cur_amount < amount)
                                    amount = cur_amount;
                            }
                        }

                        /* add extra indent for a comment */
                        if (cin_iscomment(theline))
                            amount += curbuf.b_ind_comment;
                    }
                    else
                    {
                        /*
                         * We are inside braces,
                         * there is a { before this line at the position stored in tryposBrace.
                         * Make a copy of tryposBrace, it may point to pos_copy inside find_start_brace(),
                         * which may be changed somewhere.
                         */ /* } */
                        pos_C tryposBraceCopy = new pos_C();
                        COPY_pos(tryposBraceCopy, tryposBrace);
                        tryposBrace = tryposBraceCopy;
                        trypos = tryposBrace;
                        long ourscope = trypos.lnum;
                        Bytes start = ml_get(ourscope);

                        final int
                            BRACE_IN_COL0 = 1,          /* '{' is in column 0 */
                            BRACE_AT_START = 2,         /* '{' is at start of line */
                            BRACE_AT_END = 3;           /* '{' is at end of line */

                        int start_brace;

                        /*
                         * Now figure out how indented the line is in general.
                         * If the brace was at the start of the line, we use that;
                         * otherwise, check out the indentation of the line as
                         * a whole and then add the "imaginary indent" to that.
                         */
                        Bytes look = skipwhite(start);
                        if (look.at(0) == (byte)'{')
                        {
                            int[] col = new int[1];
                            getvcol(curwin, trypos, col, null, null);
                            amount = col[0];
                            if (start.at(0) == (byte)'{')
                                start_brace = BRACE_IN_COL0;
                            else
                                start_brace = BRACE_AT_START;
                        }
                        else
                        {
                            /* That opening brace might have been on a continuation line.
                             * If so, find the start of the line. */
                            curwin.w_cursor.lnum = ourscope;

                            /* Position the cursor over the rightmost paren, so that
                             * matching it will take us back to the start of the line. */
                            long lnum = ourscope;
                            if (find_last_paren(start, '(', ')')
                                        && (trypos = find_match_paren(curbuf.b_ind_maxparen)) != null)
                                lnum = trypos.lnum;

                            /* It could have been something like
                             *     case 1: if (asdf &&
                             *                  ldfd) {
                             *              }
                             */
                            if ((curbuf.b_ind_js != 0 || curbuf.b_ind_keep_case_label != 0)
                                        && cin_iscase(skipwhite(ml_get_curline()), false))
                                amount = get_indent();
                            else if (curbuf.b_ind_js != 0)
                                amount = get_indent_lnum(lnum);
                            else
                            {
                                Bytes[] lp = new Bytes[1];
                                amount = skip_label(lnum, lp);
                            }

                            start_brace = BRACE_AT_END;
                        }

                        /* For Javascript check if the line starts with "key:". */
                        boolean js_cur_has_key = false;
                        if (curbuf.b_ind_js != 0)
                            js_cur_has_key = cin_has_js_key(theline);

                        /*
                         * If we're looking at a closing brace, that's where we want to be.
                         * Otherwise, add the amount of room that an indent is supposed to be.
                         */
                        if (theline.at(0) == (byte)'}')
                        {
                            /*
                             * they may want closing braces to line up with something
                             * other than the open brace.  Indulge them, if so.
                             */
                            amount += curbuf.b_ind_close_extra;
                        }
                        else
                        {
                            /*
                             * If we're looking at an "else", try to find an "if" to match it with.
                             * If we're looking at a "while", try to find a "do" to match it with.
                             */
                            int lookfor = LOOKFOR_INITIAL;
                            if (cin_iselse(theline))
                                lookfor = LOOKFOR_IF;
                            else if (cin_iswhileofdo(theline, cur_curpos.lnum))
                                lookfor = LOOKFOR_DO;
                            if (lookfor != LOOKFOR_INITIAL)
                            {
                                curwin.w_cursor.lnum = cur_curpos.lnum;
                                if (find_match(lookfor, ourscope) == true)
                                {
                                    amount = get_indent();
                                    break theend;
                                }
                            }

                            /*
                             * We get here if we are not on an "while-of-do" or "else"
                             * (or failed to find a matching "if").
                             * Search backwards for something to line up with.
                             * First set amount for when we don't find anything.
                             */

                            /*
                             * If the '{' is  _really_ at the left margin, use the imaginary location of
                             * a left-margin brace.  Otherwise, correct the location for b_ind_open_extra.
                             */

                            boolean lookfor_cpp_namespace = false;

                            if (start_brace == BRACE_IN_COL0)       /* '{' is in column 0 */
                            {
                                amount = curbuf.b_ind_open_left_imag;
                                lookfor_cpp_namespace = true;
                            }
                            else if (start_brace == BRACE_AT_START && lookfor_cpp_namespace)
                            {                                       /* '{' is at start */
                                lookfor_cpp_namespace = true;
                            }
                            else
                            {
                                if (start_brace == BRACE_AT_END)    /* '{' is at end of line */
                                {
                                    amount += curbuf.b_ind_open_imag;

                                    Bytes l = skipwhite(ml_get_curline());
                                    if (cin_is_cpp_namespace(l))
                                        amount += curbuf.b_ind_cpp_namespace;
                                }
                                else
                                {
                                    /* Compensate for adding b_ind_open_extra later. */
                                    amount -= curbuf.b_ind_open_extra;
                                    if (amount < 0)
                                        amount = 0;
                                }
                            }

                            boolean lookfor_break = false;

                            if (cin_iscase(theline, false))         /* it's a switch() label */
                            {
                                lookfor = LOOKFOR_CASE;             /* find a previous switch() label */
                                amount += curbuf.b_ind_case;
                            }
                            else if (cin_isscopedecl(theline))      /* private:, ... */
                            {
                                lookfor = LOOKFOR_SCOPEDECL;        /* class decl is this block */
                                amount += curbuf.b_ind_scopedecl;
                            }
                            else
                            {
                                if (curbuf.b_ind_case_break != 0 && cin_isbreak(theline))
                                    /* break; ... */
                                    lookfor_break = true;

                                lookfor = LOOKFOR_INITIAL;
                                /* b_ind_level from start of block */
                                amount += curbuf.b_ind_level;
                            }

                            int scope_amount = amount;
                            int whilelevel = 0;

                            /*
                             * Search backwards.  If we find something we recognize, line up with that.
                             *
                             * If we're looking at an open brace,
                             * indent the usual amount relative to the conditional that opens the block.
                             */
                            COPY_pos(curwin.w_cursor, cur_curpos);
                            for ( ; ; )
                            {
                                curwin.w_cursor.lnum--;
                                curwin.w_cursor.col = 0;

                                /*
                                 * If we went all the way back to the start of our scope, line up with it.
                                 */
                                if (curwin.w_cursor.lnum <= ourscope)
                                {
                                    /* we reached end of scope:
                                     * if looking for a enum or structure initialization
                                     * go further back:
                                     * if it is an initializer (enum xxx or xxx =), then don't add
                                     * ind_continuation, otherwise it is a variable declaration:
                                     * int x,
                                     *     here; <-- add ind_continuation
                                     */
                                    if (lookfor == LOOKFOR_ENUM_OR_INIT)
                                    {
                                        if (curwin.w_cursor.lnum == 0
                                                || curwin.w_cursor.lnum < ourscope - curbuf.b_ind_maxparen)
                                        {
                                            /* nothing found (abuse curbuf.b_ind_maxparen as limit)
                                            * assume terminated line (i.e. a variable initialization) */
                                            if (0 < cont_amount)
                                                amount = cont_amount;
                                            else if (curbuf.b_ind_js == 0)
                                                amount += ind_continuation;
                                            break;
                                        }

                                        Bytes[] lp = { ml_get_curline() };

                                        /*
                                         * If we're in a comment now, skip to the start of the comment.
                                         */
                                        trypos = ind_find_start_comment();
                                        if (trypos != null)
                                        {
                                            curwin.w_cursor.lnum = trypos.lnum + 1;
                                            curwin.w_cursor.col = 0;
                                            continue;
                                        }

                                        /*
                                         * Skip preprocessor directives and blank lines.
                                         */
                                        boolean b;
                                        { long[] __ = { curwin.w_cursor.lnum }; b = cin_ispreproc_cont(lp, __); curwin.w_cursor.lnum = __[0]; }
                                        if (b)
                                            continue;

                                        if (cin_nocode(lp[0]))
                                            continue;

                                        byte terminated = cin_isterminated(lp[0], false, true);

                                        /*
                                         * If we are at top level and the line looks like a function
                                         * declaration, we are done (it's a variable declaration).
                                         */
                                        if (start_brace != BRACE_IN_COL0
                                            || !cin_isfuncdecl(lp, curwin.w_cursor.lnum, 0))
                                        {
                                            /* if the line is terminated with another ','
                                             * it is a continued variable initialization.
                                             * don't add extra indent.
                                             * TODO: does not work, if a function declaration is split
                                             * over multiple lines:
                                             * cin_isfuncdecl returns false then.
                                             */
                                            if (terminated == ',')
                                                break;

                                            /* if it es a enum declaration or an assignment, we are done */
                                            if (terminated != ';' && cin_isinit())
                                                break;

                                            /* nothing useful found */
                                            if (terminated == NUL || terminated == '{')
                                                continue;
                                        }

                                        if (terminated != ';')
                                        {
                                            /* Skip parens and braces.  Position the cursor
                                             * over the rightmost paren, so that matching it
                                             * will take us back to the start of the line.
                                             */
                                            trypos = null;
                                            if (find_last_paren(lp[0], '(', ')'))
                                                trypos = find_match_paren(curbuf.b_ind_maxparen);

                                            if (trypos == null && find_last_paren(lp[0], '{', '}'))
                                                trypos = find_start_brace();

                                            if (trypos != null)
                                            {
                                                curwin.w_cursor.lnum = trypos.lnum + 1;
                                                curwin.w_cursor.col = 0;
                                                continue;
                                            }
                                        }

                                        /* it's a variable declaration, add indentation
                                         * like in
                                         * int a,
                                         *    b;
                                         */
                                        if (0 < cont_amount)
                                            amount = cont_amount;
                                        else
                                            amount += ind_continuation;
                                    }
                                    else if (lookfor == LOOKFOR_UNTERM)
                                    {
                                        if (0 < cont_amount)
                                            amount = cont_amount;
                                        else
                                            amount += ind_continuation;
                                    }
                                    else
                                    {
                                        if (lookfor != LOOKFOR_TERM
                                            && lookfor != LOOKFOR_CPP_BASECLASS
                                            && lookfor != LOOKFOR_COMMA)
                                        {
                                            amount = scope_amount;
                                            if (theline.at(0) == (byte)'{')
                                            {
                                                amount += curbuf.b_ind_open_extra;
                                                added_to_amount = curbuf.b_ind_open_extra;
                                            }
                                        }

                                        if (lookfor_cpp_namespace)
                                        {
                                            /*
                                             * Looking for C++ namespace, need to look further back.
                                             */
                                            if (curwin.w_cursor.lnum == ourscope)
                                                continue;

                                            if (curwin.w_cursor.lnum == 0
                                                    || curwin.w_cursor.lnum < ourscope - FIND_NAMESPACE_LIM)
                                                break;

                                            Bytes[] lp = { ml_get_curline() };

                                            /* If we're in a comment now, skip to the start of the comment. */
                                            trypos = ind_find_start_comment();
                                            if (trypos != null)
                                            {
                                                curwin.w_cursor.lnum = trypos.lnum + 1;
                                                curwin.w_cursor.col = 0;
                                                continue;
                                            }

                                            /* Skip preprocessor directives and blank lines. */
                                            boolean b;
                                            { long[] __ = { curwin.w_cursor.lnum }; b = cin_ispreproc_cont(lp, __); curwin.w_cursor.lnum = __[0]; }
                                            if (b)
                                                continue;

                                            /* Finally the actual check for "namespace". */
                                            if (cin_is_cpp_namespace(lp[0]))
                                            {
                                                amount += curbuf.b_ind_cpp_namespace - added_to_amount;
                                                break;
                                            }

                                            if (cin_nocode(lp[0]))
                                                continue;
                                        }
                                    }
                                    break;
                                }

                                /*
                                 * If we're in a comment now, skip to the start of the comment.
                                 */
                                if ((trypos = ind_find_start_comment()) != null)
                                {
                                    curwin.w_cursor.lnum = trypos.lnum + 1;
                                    curwin.w_cursor.col = 0;
                                    continue;
                                }

                                Bytes[] lp = { ml_get_curline() };

                                /*
                                 * If this is a switch() label, may line up relative to that.
                                 * If this is a C++ scope declaration, do the same.
                                 */
                                boolean iscase = cin_iscase(lp[0], false);
                                if (iscase || cin_isscopedecl(lp[0]))
                                {
                                    /* we are only looking for cpp base class
                                     * declaration/initialization any longer */
                                    if (lookfor == LOOKFOR_CPP_BASECLASS)
                                        break;

                                    /* When looking for a "do" we are not interested in labels. */
                                    if (0 < whilelevel)
                                        continue;

                                    /*
                                     *  case xx:
                                     *      c = 99 +        <- this indent plus continuation
                                     *->           here;
                                     */
                                    if (lookfor == LOOKFOR_UNTERM || lookfor == LOOKFOR_ENUM_OR_INIT)
                                    {
                                        if (0 < cont_amount)
                                            amount = cont_amount;
                                        else
                                            amount += ind_continuation;
                                        break;
                                    }

                                    /*
                                     *  case xx:        <- line up with this case
                                     *      x = 333;
                                     *  case yy:
                                     */
                                    if ((iscase && lookfor == LOOKFOR_CASE)
                                        || (iscase && lookfor_break)
                                        || (!iscase && lookfor == LOOKFOR_SCOPEDECL))
                                    {
                                        /*
                                         * Check that this case label is not for another switch()
                                         */
                                        if ((trypos = find_start_brace()) == null || trypos.lnum == ourscope)
                                        {
                                            amount = get_indent();
                                            break;
                                        }
                                        continue;
                                    }

                                    int n = get_indent_nolabel(curwin.w_cursor.lnum);

                                    /*
                                     *   case xx: if (cond)         <- line up with this if
                                     *                y = y + 1;
                                     * ->         s = 99;
                                     *
                                     *   case xx:
                                     *       if (cond)          <- line up with this line
                                     *           y = y + 1;
                                     * ->    s = 99;
                                     */
                                    if (lookfor == LOOKFOR_TERM)
                                    {
                                        if (n != 0)
                                            amount = n;

                                        if (!lookfor_break)
                                            break;
                                    }

                                    /*
                                     *   case xx: x = x + 1;        <- line up with this x
                                     * ->         y = y + 1;
                                     *
                                     *   case xx: if (cond)         <- line up with this if
                                     * ->              y = y + 1;
                                     */
                                    if (n != 0)
                                    {
                                        amount = n;
                                        Bytes l = after_label(ml_get_curline());
                                        if (l != null && cin_is_cinword(l))
                                        {
                                            if (theline.at(0) == (byte)'{')
                                                amount += curbuf.b_ind_open_extra;
                                            else
                                                amount += curbuf.b_ind_level + curbuf.b_ind_no_brace;
                                        }
                                        break;
                                    }

                                    /*
                                     * Try to get the indent of a statement before the switch
                                     * label.  If nothing is found, line up relative to the
                                     * switch label.
                                     *      break;              <- may line up with this line
                                     *   case xx:
                                     * ->   y = 1;
                                     */
                                    scope_amount = get_indent() + (iscase
                                                        ? curbuf.b_ind_case_code
                                                        : curbuf.b_ind_scopedecl_code);
                                    lookfor = (curbuf.b_ind_case_break != 0) ? LOOKFOR_NOBREAK : LOOKFOR_ANY;
                                    continue;
                                }

                                /*
                                 * Looking for a switch() label or C++ scope declaration,
                                 * ignore other lines, skip {}-blocks.
                                 */
                                if (lookfor == LOOKFOR_CASE || lookfor == LOOKFOR_SCOPEDECL)
                                {
                                    if (find_last_paren(lp[0], '{', '}') && (trypos = find_start_brace()) != null)
                                    {
                                        curwin.w_cursor.lnum = trypos.lnum + 1;
                                        curwin.w_cursor.col = 0;
                                    }
                                    continue;
                                }

                                /*
                                 * Ignore jump labels with nothing after them.
                                 */
                                if (curbuf.b_ind_js == 0 && cin_islabel())
                                {
                                    Bytes l = after_label(ml_get_curline());
                                    if (l == null || cin_nocode(l))
                                        continue;
                                }

                                /*
                                 * Ignore #defines, #if, etc.
                                 * Ignore comment and empty lines.
                                 * (need to get the line again, cin_islabel() may have unlocked it)
                                 */
                                lp[0] = ml_get_curline();
                                boolean b;
                                { long[] __ = { curwin.w_cursor.lnum }; b = cin_ispreproc_cont(lp, __); curwin.w_cursor.lnum = __[0]; }
                                if (b || cin_nocode(lp[0]))
                                    continue;

                                int[] col = new int[1];

                                /*
                                 * Are we at the start of a cpp base class declaration
                                 * or constructor initialization?
                                 */
                                /*boolean */b = false;
                                if (lookfor != LOOKFOR_TERM && 0 < curbuf.b_ind_cpp_baseclass)
                                {
                                    b = cin_is_cpp_baseclass(col);
                                    lp[0] = ml_get_curline();
                                }
                                if (b)
                                {
                                    if (lookfor == LOOKFOR_UNTERM)
                                    {
                                        if (0 < cont_amount)
                                            amount = cont_amount;
                                        else
                                            amount += ind_continuation;
                                    }
                                    else if (theline.at(0) == (byte)'{')
                                    {
                                        /* Need to find start of the declaration. */
                                        lookfor = LOOKFOR_UNTERM;
                                        ind_continuation = 0;
                                        continue;
                                    }
                                    else
                                        amount = get_baseclass_amount(col[0]);
                                    break;
                                }
                                else if (lookfor == LOOKFOR_CPP_BASECLASS)
                                {
                                    /* Only look, whether there is a cpp base class declaration
                                     * or initialization before the opening brace.
                                     */
                                    if (cin_isterminated(lp[0], true, false) != NUL)
                                        break;
                                    else
                                        continue;
                                }

                                /*
                                 * What happens next depends on the line being terminated.
                                 * If terminated with a ',' only consider it terminating if
                                 * there is another unterminated statement behind, eg:
                                 *   123,
                                 *   sizeof
                                 *        here
                                 * Otherwise check whether it is a enumeration or structure
                                 * initialisation (not indented) or a variable declaration
                                 * (indented).
                                 */
                                byte terminated = cin_isterminated(lp[0], false, true);

                                if (js_cur_has_key)
                                {
                                    js_cur_has_key = false;     /* only check the first line */
                                    if (curbuf.b_ind_js != 0 && terminated == ',')
                                    {
                                        /* For Javascript we might be inside an object:
                                         *   key: something,  <- align with this
                                         *   key: something
                                         * or:
                                         *   key: something +  <- align with this
                                         *       something,
                                         *   key: something
                                         */
                                        lookfor = LOOKFOR_JS_KEY;
                                    }
                                }
                                if (lookfor == LOOKFOR_JS_KEY && cin_has_js_key(lp[0]))
                                {
                                    amount = get_indent();
                                    break;
                                }
                                if (lookfor == LOOKFOR_COMMA)
                                {
                                    if (tryposBrace != null && curwin.w_cursor.lnum <= tryposBrace.lnum)
                                        break;
                                    if (terminated == ',')
                                        /* line below current line is the one that starts
                                         * a (possibly broken) line ending in a comma */
                                        break;
                                    else
                                    {
                                        amount = get_indent();
                                        if (curwin.w_cursor.lnum - 1 == ourscope)
                                            /* line above is start of the scope, thus current
                                             * line is the one that stars a (possibly broken)
                                             * line ending in a comma */
                                            break;
                                    }
                                }

                                if (terminated == NUL || (lookfor != LOOKFOR_UNTERM && terminated == ','))
                                {
                                    if (skipwhite(lp[0]).at(0) == (byte)'[' || lp[0].at(strlen(lp[0]) - 1) == '[')
                                        amount += ind_continuation;
                                    /*
                                     * if we're in the middle of a paren thing,
                                     * go back to the line that starts it so
                                     * we can get the right prevailing indent
                                     *     if ( foo &&
                                     *              bar )
                                     */
                                    /*
                                     * Position the cursor over the rightmost paren, so that
                                     * matching it will take us back to the start of the line.
                                     * Ignore a match before the start of the block.
                                     */
                                    find_last_paren(lp[0], '(', ')');
                                    trypos = find_match_paren(corr_ind_maxparen(cur_curpos));
                                    if (trypos != null && (trypos.lnum < tryposBrace.lnum
                                                || (trypos.lnum == tryposBrace.lnum && trypos.col < tryposBrace.col)))
                                        trypos = null;

                                    /*
                                     * If we are looking for ',', we also look for matching braces.
                                     */
                                    if (trypos == null && terminated == ',' && find_last_paren(lp[0], '{', '}'))
                                        trypos = find_start_brace();

                                    if (trypos != null)
                                    {
                                        /*
                                         * Check if we are on a case label now.
                                         * This is handled above.
                                         *     case xx:  if ( asdf &&
                                         *                      asdf)
                                         */
                                        COPY_pos(curwin.w_cursor, trypos);
                                        lp[0] = ml_get_curline();
                                        if (cin_iscase(lp[0], false) || cin_isscopedecl(lp[0]))
                                        {
                                            curwin.w_cursor.lnum++;
                                            curwin.w_cursor.col = 0;
                                            continue;
                                        }
                                    }

                                    /*
                                     * Skip over continuation lines to find
                                     * the one to get the indent from
                                     * char *usethis = "bla\
                                     *           bla",
                                     *      here;
                                     */
                                    if (terminated == ',')
                                    {
                                        while (1 < curwin.w_cursor.lnum)
                                        {
                                            lp[0] = ml_get(curwin.w_cursor.lnum - 1);
                                            if (lp[0].at(0) == NUL || lp[0].at(strlen(lp[0]) - 1) != '\\')
                                                break;
                                            --curwin.w_cursor.lnum;
                                            curwin.w_cursor.col = 0;
                                        }
                                    }

                                    /*
                                     * Get indent and pointer to text for current line,
                                     * ignoring any jump label.
                                     */
                                    if (curbuf.b_ind_js != 0)
                                        cur_amount = get_indent();
                                    else
                                        cur_amount = skip_label(curwin.w_cursor.lnum, lp);

                                    /*
                                     * If this is just above the line we are indenting, and it
                                     * starts with a '{', line it up with this line.
                                     *          while (not)
                                     * ->       {
                                     *          }
                                     */
                                    if (terminated != ',' && lookfor != LOOKFOR_TERM && theline.at(0) == (byte)'{')
                                    {
                                        amount = cur_amount;
                                        /*
                                         * Only add b_ind_open_extra when the current line
                                         * doesn't start with a '{', which must have a match
                                         * in the same line (scope is the same).  Probably:
                                         *      { 1, 2 },
                                         * ->   { 3, 4 }
                                         */
                                        if (skipwhite(lp[0]).at(0) != (byte)'{')
                                            amount += curbuf.b_ind_open_extra;

                                        if (curbuf.b_ind_cpp_baseclass != 0 && curbuf.b_ind_js == 0)
                                        {
                                            /* have to look back, whether it is a cpp base
                                             * class declaration or initialization */
                                            lookfor = LOOKFOR_CPP_BASECLASS;
                                            continue;
                                        }
                                        break;
                                    }

                                    /*
                                     * Check if we are after an "if", "while", etc.
                                     * Also allow "   } else".
                                     */
                                    if (cin_is_cinword(lp[0]) || cin_iselse(skipwhite(lp[0])))
                                    {
                                        /*
                                         * Found an unterminated line after an if (),
                                         * line up with the last one.
                                         *   if (cond)
                                         *          100 +
                                         * ->           here;
                                         */
                                        if (lookfor == LOOKFOR_UNTERM || lookfor == LOOKFOR_ENUM_OR_INIT)
                                        {
                                            if (0 < cont_amount)
                                                amount = cont_amount;
                                            else
                                                amount += ind_continuation;
                                            break;
                                        }

                                        /*
                                         * If this is just above the line we are indenting,
                                         * we are finished.
                                         *          while (not)
                                         * ->           here;
                                         * Otherwise this indent can be used when the line
                                         * before this is terminated.
                                         *      yyy;
                                         *      if (stat)
                                         *          while (not)
                                         *              xxx;
                                         * ->   here;
                                         */
                                        amount = cur_amount;
                                        if (theline.at(0) == (byte)'{')
                                            amount += curbuf.b_ind_open_extra;
                                        if (lookfor != LOOKFOR_TERM)
                                        {
                                            amount += curbuf.b_ind_level + curbuf.b_ind_no_brace;
                                            break;
                                        }

                                        /*
                                         * Special trick: when expecting the while () after a do,
                                         * line up with the while()
                                         *     do
                                         *          x = 1;
                                         * ->  here
                                         */
                                        lp[0] = skipwhite(ml_get_curline());
                                        if (cin_isdo(lp[0]))
                                        {
                                            if (whilelevel == 0)
                                                break;
                                            --whilelevel;
                                        }

                                        /*
                                         * When searching for a terminated line, don't use
                                         * the one between the "if" and the matching "else".
                                         * Need to use the scope of this "else".
                                         * If whilelevel != 0 continue looking for a "do {".
                                         */
                                        if (cin_iselse(lp[0]) && whilelevel == 0)
                                        {
                                            /* If we're looking at "} else", let's make sure we
                                             * find the opening brace of the enclosing scope,
                                             * not the one from "if () {". */
                                            if (lp[0].at(0) == (byte)'}')
                                                curwin.w_cursor.col = BDIFF(lp[0], ml_get_curline()) + 1;

                                            if ((trypos = find_start_brace()) == null
                                                    || find_match(LOOKFOR_IF, trypos.lnum) == false)
                                                break;
                                        }
                                    }

                                    /*
                                     * If we're below an unterminated line that is not an
                                     * "if" or something, we may line up with this line or
                                     * add something for a continuation line, depending on
                                     * the line before this one.
                                     */
                                    else
                                    {
                                        /*
                                         * Found two unterminated lines on a row, line up with
                                         * the last one.
                                         *   c = 99 +
                                         *          100 +
                                         * ->       here;
                                         */
                                        if (lookfor == LOOKFOR_UNTERM)
                                        {
                                            /* When line ends in a comma add extra indent. */
                                            if (terminated == ',')
                                                amount += ind_continuation;
                                            break;
                                        }

                                        if (lookfor == LOOKFOR_ENUM_OR_INIT)
                                        {
                                            /* Found two lines ending in ',', lineup with the lowest one,
                                             * but check for cpp base class declaration/initialization,
                                             * if it is an opening brace or we are looking just for
                                             * enumerations/initializations. */
                                            if (terminated == ',')
                                            {
                                                if (curbuf.b_ind_cpp_baseclass == 0)
                                                    break;

                                                lookfor = LOOKFOR_CPP_BASECLASS;
                                                continue;
                                            }

                                            /* Ignore unterminated lines in between, but reduce indent. */
                                            if (cur_amount < amount)
                                                amount = cur_amount;
                                        }
                                        else
                                        {
                                            /*
                                             * Found first unterminated line on a row, may
                                             * line up with this line, remember its indent
                                             *      100 +
                                             * ->           here;
                                             */
                                            Bytes l = ml_get_curline();
                                            amount = cur_amount;
                                            if (skipwhite(l).at(0) == (byte)']' || l.at(strlen(l) - 1) == ']')
                                                break;

                                            /*
                                             * If previous line ends in ',', check whether we
                                             * are in an initialization or enum
                                             * struct xxx =
                                             * {
                                             *      sizeof a,
                                             *      124 };
                                             * or a normal possible continuation line.
                                             * but only, of no other statement has been found yet.
                                             */
                                            if (lookfor == LOOKFOR_INITIAL && terminated == ',')
                                            {
                                                if (curbuf.b_ind_js != 0)
                                                {
                                                    /* Search for a line ending in a comma
                                                     * and line up with the line below it
                                                     * (could be the current line).
                                                     * some = [
                                                     *     1,     <- line up here
                                                     *     2,
                                                     * some = [
                                                     *     3 +    <- line up here
                                                     *       4 *
                                                     *        5,
                                                     *     6,
                                                     */
                                                    if (cin_iscomment(skipwhite(l)))
                                                        break;
                                                    lookfor = LOOKFOR_COMMA;
                                                    trypos = find_match_char('[', curbuf.b_ind_maxparen);
                                                    if (trypos != null)
                                                    {
                                                        if (trypos.lnum == curwin.w_cursor.lnum - 1)
                                                        {
                                                            /* Current line is first inside [],
                                                             * line up with it. */
                                                            break;
                                                        }
                                                        ourscope = trypos.lnum;
                                                    }
                                                }
                                                else
                                                {
                                                    lookfor = LOOKFOR_ENUM_OR_INIT;
                                                    cont_amount = cin_first_id_amount();
                                                }
                                            }
                                            else
                                            {
                                                if (lookfor == LOOKFOR_INITIAL
                                                        && l.at(0) != NUL
                                                        && l.at(strlen(l) - 1) == '\\')
                                                    cont_amount = cin_get_equal_amount(curwin.w_cursor.lnum);
                                                if (lookfor != LOOKFOR_TERM
                                                    && lookfor != LOOKFOR_JS_KEY
                                                    && lookfor != LOOKFOR_COMMA)
                                                    lookfor = LOOKFOR_UNTERM;
                                            }
                                        }
                                    }
                                }

                                /*
                                 * Check if we are after a while (cond);
                                 * If so: Ignore until the matching "do".
                                 */
                                else if (cin_iswhileofdo_end(terminated))
                                {
                                    /*
                                     * Found an unterminated line after a while ();,
                                     * line up with the last one.
                                     *      while (cond);
                                     *      100 +               <- line up with this one
                                     * ->           here;
                                     */
                                    if (lookfor == LOOKFOR_UNTERM || lookfor == LOOKFOR_ENUM_OR_INIT)
                                    {
                                        if (0 < cont_amount)
                                            amount = cont_amount;
                                        else
                                            amount += ind_continuation;
                                        break;
                                    }

                                    if (whilelevel == 0)
                                    {
                                        lookfor = LOOKFOR_TERM;
                                        amount = get_indent();
                                        if (theline.at(0) == (byte)'{')
                                            amount += curbuf.b_ind_open_extra;
                                    }
                                    whilelevel++;
                                }

                                /*
                                 * We are after a "normal" statement.
                                 * If we had another statement we can stop now and use the
                                 * indent of that other statement.
                                 * Otherwise the indent of the current statement may be used,
                                 * search backwards for the next "normal" statement.
                                 */
                                else
                                {
                                    /*
                                     * Skip single break line, if before a switch label.
                                     * It may be lined up with the case label.
                                     */
                                    if (lookfor == LOOKFOR_NOBREAK && cin_isbreak(skipwhite(ml_get_curline())))
                                    {
                                        lookfor = LOOKFOR_ANY;
                                        continue;
                                    }

                                    /*
                                     * Handle "do {" line.
                                     */
                                    if (0 < whilelevel)
                                    {
                                        lp[0] = cin_skipcomment(ml_get_curline());
                                        if (cin_isdo(lp[0]))
                                        {
                                            amount = get_indent();
                                            --whilelevel;
                                            continue;
                                        }
                                    }

                                    /*
                                     * Found a terminated line above an unterminated line.
                                     * Add the amount for a continuation line.
                                     *   x = 1;
                                     *   y = foo +
                                     * ->       here;
                                     * or
                                     *   int x = 1;
                                     *   int foo,
                                     * ->       here;
                                     */
                                    if (lookfor == LOOKFOR_UNTERM || lookfor == LOOKFOR_ENUM_OR_INIT)
                                    {
                                        if (0 < cont_amount)
                                            amount = cont_amount;
                                        else
                                            amount += ind_continuation;
                                        break;
                                    }

                                    /*
                                     * Found a terminated line above a terminated line or "if" etc. line.
                                     * Use the amount of the line below us.
                                     *   x = 1;                         x = 1;
                                     *   if (asdf)                  y = 2;
                                     *       while (asdf)         ->here;
                                     *          here;
                                     * ->foo;
                                     */
                                    if (lookfor == LOOKFOR_TERM)
                                    {
                                        if (!lookfor_break && whilelevel == 0)
                                            break;
                                    }

                                    /*
                                     * First line above the one we're indenting is terminated.
                                     * To know what needs to be done look further backward
                                     * for a terminated line.
                                     */
                                    else
                                    {
                                        /*
                                         * position the cursor over the rightmost paren, so that
                                         * matching it will take us back to the start of the line.
                                         * Helps for:
                                         *     func(asdr,
                                         *            asdfasdf);
                                         *     here;
                                         */
                                        term_again:
                                        for ( ; ; )
                                        {
                                            lp[0] = ml_get_curline();
                                            if (find_last_paren(lp[0], '(', ')')
                                                    && (trypos = find_match_paren(curbuf.b_ind_maxparen)) != null)
                                            {
                                                /*
                                                 * Check if we are on a case label now.
                                                 * This is handled above.
                                                 *     case xx:  if ( asdf &&
                                                 *                      asdf)
                                                 */
                                                COPY_pos(curwin.w_cursor, trypos);
                                                lp[0] = ml_get_curline();
                                                if (cin_iscase(lp[0], false) || cin_isscopedecl(lp[0]))
                                                {
                                                    curwin.w_cursor.lnum++;
                                                    curwin.w_cursor.col = 0;
                                                    continue;
                                                }
                                            }

                                            /* When aligning with the case statement, don't align
                                             * with a statement after it.
                                             *  case 1: {   <-- don't use this { position
                                             *      stat;
                                             *  }
                                             *  case 2:
                                             *      stat;
                                             * }
                                             */
                                            /*boolean */iscase = (curbuf.b_ind_keep_case_label != 0 && cin_iscase(lp[0], false));

                                            /*
                                             * Get indent and pointer to text for current line,
                                             * ignoring any jump label.
                                             */
                                            amount = skip_label(curwin.w_cursor.lnum, lp);

                                            if (theline.at(0) == (byte)'{')
                                                amount += curbuf.b_ind_open_extra;
                                            /* See remark above: "Only add b_ind_open_extra.." */
                                            lp[0] = skipwhite(lp[0]);
                                            if (lp[0].at(0) == (byte)'{')
                                                amount -= curbuf.b_ind_open_extra;
                                            lookfor = iscase ? LOOKFOR_ANY : LOOKFOR_TERM;

                                            /*
                                             * When a terminated line starts with "else" skip to
                                             * the matching "if":
                                             *       else 3;
                                             *           indent this;
                                             * Need to use the scope of this "else".
                                             * If whilelevel != 0 continue looking for a "do {".
                                             */
                                            if (lookfor == LOOKFOR_TERM
                                                    && lp[0].at(0) != (byte)'}'
                                                    && cin_iselse(lp[0])
                                                    && whilelevel == 0)
                                            {
                                                if ((trypos = find_start_brace()) == null
                                                        || find_match(LOOKFOR_IF, trypos.lnum) == false)
                                                    break;
                                                continue;
                                            }

                                            /*
                                             * If we're at the end of a block, skip to the start of that block.
                                             */
                                            Bytes l = ml_get_curline();
                                            if (find_last_paren(l, '{', '}') && (trypos = find_start_brace()) != null)
                                            {
                                                COPY_pos(curwin.w_cursor, trypos);
                                                /* if not "else {" check for terminated again,
                                                 * but skip block for "} else {" */
                                                l = cin_skipcomment(ml_get_curline());
                                                if (l.at(0) == (byte)'}' || !cin_iselse(l))
                                                    continue term_again;
                                                curwin.w_cursor.lnum++;
                                                curwin.w_cursor.col = 0;
                                            }

                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    /* add extra indent for a comment */
                    if (cin_iscomment(theline))
                        amount += curbuf.b_ind_comment;

                    /* subtract extra left-shift for jump labels */
                    if (0 < curbuf.b_ind_jump_label && original_line_islabel)
                        amount -= curbuf.b_ind_jump_label;
                }
                else
                {
                    /*
                     * ok -- we're not inside any sort of structure at all!
                     *
                     * This means we're at the top level, and everything should
                     * basically just match where the previous line is, except
                     * for the lines immediately following a function declaration,
                     * which are K&R-style parameters and need to be indented.
                     *
                     * If our line starts with an open brace, forget about any
                     * prevailing indent and make sure it looks like the start
                     * of a function.
                     */

                    if (theline.at(0) == (byte)'{')
                    {
                        amount = curbuf.b_ind_first_open;
                    }

                    /*
                     * If the NEXT line is a function declaration, the current
                     * line needs to be indented as a function type spec.
                     * Don't do this if the current line looks like a comment
                     * or if the current line is terminated, i.e. ends in ';',
                     * or if the current line contains { or }: "void f() {\n if (1)".
                     */
                    else if (cur_curpos.lnum < curbuf.b_ml.ml_line_count
                            && !cin_nocode(theline)
                            && vim_strchr(theline, '{') == null
                            && vim_strchr(theline, '}') == null
                            && !cin_ends_in(theline, u8(":"), null)
                            && !cin_ends_in(theline, u8(","), null)
                            && cin_isfuncdecl(null, cur_curpos.lnum + 1, cur_curpos.lnum + 1)
                            && cin_isterminated(theline, false, true) == NUL)
                    {
                        amount = curbuf.b_ind_func_type;
                    }
                    else
                    {
                        amount = 0;
                        COPY_pos(curwin.w_cursor, cur_curpos);

                        /* search backwards until we find something we recognize */

                        while (1 < curwin.w_cursor.lnum)
                        {
                            curwin.w_cursor.lnum--;
                            curwin.w_cursor.col = 0;

                            Bytes[] lp = { ml_get_curline() };

                            /*
                             * If we're in a comment now, skip to the start of the comment.
                             */
                            if ((trypos = ind_find_start_comment()) != null)
                            {
                                curwin.w_cursor.lnum = trypos.lnum + 1;
                                curwin.w_cursor.col = 0;
                                continue;
                            }

                            int[] col = new int[1];

                            /*
                             * Are we at the start of a cpp base class declaration or
                             * constructor initialization?
                             */
                            boolean b = false;
                            if (curbuf.b_ind_cpp_baseclass != 0 && theline.at(0) != (byte)'{')
                            {
                                b = cin_is_cpp_baseclass(col);
                                lp[0] = ml_get_curline();
                            }
                            if (b)
                            {
                                amount = get_baseclass_amount(col[0]);
                                break;
                            }

                            /*
                             * Skip preprocessor directives and blank lines.
                             */
                         /* boolean b; */
                            { long[] __ = { curwin.w_cursor.lnum }; b = cin_ispreproc_cont(lp, __); curwin.w_cursor.lnum = __[0]; }
                            if (b)
                                continue;

                            if (cin_nocode(lp[0]))
                                continue;

                            /*
                             * If the previous line ends in ',', use one level of
                             * indentation:
                             * int foo,
                             *     bar;
                             * do this before checking for '}' in case of eg.
                             * enum foobar
                             * {
                             *   ...
                             * } foo,
                             *   bar;
                             */
                            int n = 0;
                            if (cin_ends_in(lp[0], u8(","), null) || (lp[0].at(0) != NUL && (n = lp[0].at(strlen(lp[0]) - 1)) == '\\'))
                            {
                                /* take us back to opening paren */
                                if (find_last_paren(lp[0], '(', ')')
                                        && (trypos = find_match_paren(curbuf.b_ind_maxparen)) != null)
                                    COPY_pos(curwin.w_cursor, trypos);

                                /* For a line ending in ',' that is a continuation line
                                 * go back to the first line with a backslash:
                                 * char *foo = "bla\
                                 *           bla",
                                 *      here;
                                 */
                                while (n == 0 && 1 < curwin.w_cursor.lnum)
                                {
                                    Bytes l = ml_get(curwin.w_cursor.lnum - 1);
                                    if (l.at(0) == NUL || l.at(strlen(l) - 1) != '\\')
                                        break;
                                    --curwin.w_cursor.lnum;
                                    curwin.w_cursor.col = 0;
                                }

                                amount = get_indent();

                                if (amount == 0)
                                    amount = cin_first_id_amount();
                                if (amount == 0)
                                    amount = ind_continuation;
                                break;
                            }

                            /*
                             * If the line looks like a function declaration,
                             * and we're not in a comment, put it the left margin.
                             */
                            if (cin_isfuncdecl(null, cur_curpos.lnum, 0))
                                break;
                            lp[0] = ml_get_curline();

                            /*
                             * Finding the closing '}' of a previous function.
                             * Put current line at the left margin.
                             * For when 'cino' has "fs".
                             */
                            if (skipwhite(lp[0]).at(0) == (byte)'}')
                                break;

                            /*                          (matching {)
                             * If the previous line ends on '};' (maybe followed
                             * by comments) align at column 0.  For example:
                             * char *string_array[] = { "foo",
                             *     / * x * / "b};ar" }; / * foobar * /
                             */
                            if (cin_ends_in(lp[0], u8("};"), null))
                                break;

                            /*
                             * If the previous line ends on '[' we are probably
                             * in an array constant:
                             * something = [
                             *     234,  <- extra indent
                             */
                            if (cin_ends_in(lp[0], u8("["), null))
                            {
                                amount = get_indent() + ind_continuation;
                                break;
                            }

                            /*
                             * Find a line only has a semicolon that belongs to
                             * a previous line ending in '}', e.g. before an #endif.
                             * Don't increase indent then.
                             */
                            Bytes look = skipwhite(lp[0]);
                            if (look.at(0) == (byte)';' && cin_nocode(look.plus(1)))
                            {
                                pos_C curpos_save = new pos_C();
                                COPY_pos(curpos_save, curwin.w_cursor);

                                while (1 < curwin.w_cursor.lnum)
                                {
                                    look = ml_get(--curwin.w_cursor.lnum);
                                    if (!cin_nocode(look))
                                    {
                                        boolean _0;
                                        {
                                            Bytes[] _1 = { look };
                                            long[] _2 = { curwin.w_cursor.lnum };
                                            _0 = cin_ispreproc_cont(_1, _2);
                                            look = _1[0];
                                            curwin.w_cursor.lnum = _2[0];
                                        }
                                        if (!_0)
                                            break;
                                    }
                                }
                                if (0 < curwin.w_cursor.lnum && cin_ends_in(look, u8("}"), null))
                                    break;

                                COPY_pos(curwin.w_cursor, curpos_save);
                            }

                            /*
                             * If the PREVIOUS line is a function declaration, the current line
                             * (and the ones that follow) needs to be indented as parameters.
                             */
                            if (cin_isfuncdecl(lp, curwin.w_cursor.lnum, 0))
                            {
                                amount = curbuf.b_ind_param;
                                break;
                            }

                            /*
                             * If the previous line ends in ';' and the line before the
                             * previous line ends in ',' or '\', ident to column zero:
                             * int foo,
                             *     bar;
                             * indent_to_0 here;
                             */
                            if (cin_ends_in(lp[0], u8(";"), null))
                            {
                                Bytes l = ml_get(curwin.w_cursor.lnum - 1);
                                if (cin_ends_in(l, u8(","), null) || (l.at(0) != NUL && l.at(strlen(l) - 1) == '\\'))
                                    break;
                                lp[0] = ml_get_curline();
                            }

                            /*
                             * Doesn't look like anything interesting -- so just use
                             * the indent of this line.
                             *
                             * Position the cursor over the rightmost paren, so that
                             * matching it will take us back to the start of the line.
                             */
                            find_last_paren(lp[0], '(', ')');

                            if ((trypos = find_match_paren(curbuf.b_ind_maxparen)) != null)
                                COPY_pos(curwin.w_cursor, trypos);
                            amount = get_indent();
                            break;
                        }

                        /* add extra indent for a comment */
                        if (cin_iscomment(theline))
                            amount += curbuf.b_ind_comment;

                        /* add extra indent if the previous line ended in a backslash:
                         *        "asdfasdf\
                         *            here";
                         *      char *foo = "asdf\
                         *                   here";
                         */
                        if (1 < cur_curpos.lnum)
                        {
                            Bytes l = ml_get(cur_curpos.lnum - 1);
                            if (l.at(0) != NUL && l.at(strlen(l) - 1) == '\\')
                            {
                                cur_amount = cin_get_equal_amount(cur_curpos.lnum - 1);
                                if (0 < cur_amount)
                                    amount = cur_amount;
                                else if (cur_amount == 0)
                                    amount += ind_continuation;
                            }
                        }
                    }
                }
            }

            /* put the cursor back where it belongs */
            COPY_pos(curwin.w_cursor, cur_curpos);

            if (amount < 0)
                return 0;

            return amount;
        }
    };

    /*private*/ static boolean find_match(int lookfor, long ourscope)
    {
        int elselevel, whilelevel;
        if (lookfor == LOOKFOR_IF)
        {
            elselevel = 1;
            whilelevel = 0;
        }
        else
        {
            elselevel = 0;
            whilelevel = 1;
        }

        curwin.w_cursor.col = 0;

        while (ourscope + 1 < curwin.w_cursor.lnum)
        {
            curwin.w_cursor.lnum--;
            curwin.w_cursor.col = 0;

            Bytes look = cin_skipcomment(ml_get_curline());
            if (cin_iselse(look)
                    || cin_isif(look)
                    || cin_isdo(look)
                    || cin_iswhileofdo(look, curwin.w_cursor.lnum))
            {
                /*
                 * if we've gone outside the braces entirely,
                 * we must be out of scope...
                 */
                pos_C theirscope = find_start_brace();
                if (theirscope == null)
                    break;

                /*
                 * and if the brace enclosing this is further
                 * back than the one enclosing the else, we're
                 * out of luck too.
                 */
                if (theirscope.lnum < ourscope)
                    break;

                /*
                 * and if they're enclosed in a *deeper* brace,
                 * then we can ignore it because it's in a
                 * different scope...
                 */
                if (ourscope < theirscope.lnum)
                    continue;

                /*
                 * if it was an "else" (that's not an "else if")
                 * then we need to go back to another if, so
                 * increment elselevel
                 */
                look = cin_skipcomment(ml_get_curline());
                if (cin_iselse(look))
                {
                    Bytes mightbeif = cin_skipcomment(look.plus(4));
                    if (!cin_isif(mightbeif))
                        elselevel++;
                    continue;
                }

                /*
                 * if it was a "while" then we need to go back to
                 * another "do", so increment whilelevel.
                 */
                if (cin_iswhileofdo(look, curwin.w_cursor.lnum))
                {
                    whilelevel++;
                    continue;
                }

                /* If it's an "if" decrement elselevel. */
                look = cin_skipcomment(ml_get_curline());
                if (cin_isif(look))
                {
                    elselevel--;
                    /*
                     * When looking for an "if" ignore "while"s that get in the way.
                     */
                    if (elselevel == 0 && lookfor == LOOKFOR_IF)
                        whilelevel = 0;
                }

                /* If it's a "do" decrement whilelevel. */
                if (cin_isdo(look))
                    whilelevel--;

                /*
                 * if we've used up all the elses, then
                 * this must be the if that we want!
                 * match the indent level of that if.
                 */
                if (elselevel <= 0 && whilelevel <= 0)
                    return true;
            }
        }
        return false;
    }

    /*
     * Get indent level from 'indentexpr'.
     */
    /*private*/ static final getindent_C get_expr_indent = new getindent_C()
    {
        public int getindent()
        {
            boolean use_sandbox = was_set_insecurely(u8("indentexpr"), OPT_LOCAL);

            /* Save and restore cursor position and curswant,
             * in case it was changed via :normal commands. */
            pos_C save_pos = new pos_C();
            COPY_pos(save_pos, curwin.w_cursor);
            int save_curswant = curwin.w_curswant;
            boolean save_set_curswant = curwin.w_set_curswant;

            set_vim_var_nr(VV_LNUM, curwin.w_cursor.lnum);

            if (use_sandbox)
                sandbox++;
            textlock++;

            int indent = eval_to_number(curbuf.b_p_inde[0]);

            if (use_sandbox)
                --sandbox;
            --textlock;

            /* Restore the cursor position so that 'indentexpr' doesn't need to.
             * Pretend to be in Insert mode, allow cursor past end of line for "o" command. */
            int save_State = State;
            State = INSERT;

            COPY_pos(curwin.w_cursor, save_pos);
            curwin.w_curswant = save_curswant;
            curwin.w_set_curswant = save_set_curswant;

            check_cursor();

            State = save_State;

            /* If there is an error, just keep the current indent. */
            if (indent < 0)
                indent = get_indent();

            return indent;
        }
    };

    /*private*/ static boolean lisp_match(Bytes p)
    {
        final int LSIZE = 512;
        Bytes buf = new Bytes(LSIZE);

        Bytes[] word = { (curbuf.b_p_lw[0].at(0) != NUL) ? curbuf.b_p_lw[0] : p_lispwords[0] };
        while (word[0].at(0) != NUL)
        {
            copy_option_part(word, buf, LSIZE, u8(","));
            int len = strlen(buf);
            if (STRNCMP(buf, p, len) == 0 && p.at(len) == (byte)' ')
                return true;
        }

        return false;
    }

    /*
     * When 'p' is present in 'cpoptions, a Vi compatible method is used.
     * The incompatible newer method is quite a bit better at indenting
     * code in lisp-like languages than the traditional one; it's still
     * mostly heuristics however -- Dirk van Deun, dirk@rave.org
     *
     * TODO:
     * Findmatch() should be adapted for lisp, also to make showmatch
     * work correctly: now (v5.3) it seems all C/C++ oriented:
     * - it does not recognize the #\( and #\) notations as character literals
     * - it doesn't know about comments starting with a semicolon
     * - it incorrectly interprets '(' as a character literal
     * All this messes up get_lisp_indent in some rare cases.
     * Update from Sergey Khorev:
     * I tried to fix the first two issues.
     */
    /*private*/ static final getindent_C get_lisp_indent = new getindent_C()
    {
        public int getindent()
        {
            int amount;

            /* Set vi_lisp to use the vi-compatible method. */
            boolean vi_lisp = (vim_strbyte(p_cpo[0], CPO_LISP) != null);

            pos_C realpos = new pos_C();
            COPY_pos(realpos, curwin.w_cursor);
            curwin.w_cursor.col = 0;

            pos_C pos = findmatch(null, '(');
            if (pos == null)
                pos = findmatch(null, '[');
            else
            {
                pos_C paren = new pos_C();
                COPY_pos(paren, pos);
                pos = findmatch(null, '[');
                if (pos == null || ltpos(pos, paren))
                    pos = paren;
            }
            if (pos != null)
            {
                /* Extra trick: take the indent of the first previous
                 * non-white line, that is at the same () level. */
                amount = -1;
                int parencount = 0;

                while (pos.lnum <= --curwin.w_cursor.lnum)
                {
                    if (linewhite(curwin.w_cursor.lnum))
                        continue;

                    for (Bytes that = ml_get_curline(); that.at(0) != NUL; that = that.plus(1))
                    {
                        if (that.at(0) == (byte)';')
                        {
                            while (that.at(1) != NUL)
                                that = that.plus(1);
                            continue;
                        }
                        if (that.at(0) == (byte)'\\')
                        {
                            if (that.at(1) != NUL)
                                that = that.plus(1);
                            continue;
                        }
                        if (that.at(0) == (byte)'"' && that.at(1) != NUL)
                        {
                            while ((that = that.plus(1)).at(0) != NUL && that.at(0) != (byte)'"')
                            {
                                /* skipping escaped characters in the string */
                                if (that.at(0) == (byte)'\\')
                                {
                                    if ((that = that.plus(1)).at(0) == NUL)
                                        break;
                                    if (that.at(1) == NUL)
                                    {
                                        that = that.plus(1);
                                        break;
                                    }
                                }
                            }
                        }
                        if (that.at(0) == (byte)'(' || that.at(0) == (byte)'[')
                            parencount++;
                        else if (that.at(0) == (byte)')' || that.at(0) == (byte)']')
                            --parencount;
                    }

                    if (parencount == 0)
                    {
                        amount = get_indent();
                        break;
                    }
                }

                if (amount == -1)
                {
                    curwin.w_cursor.lnum = pos.lnum;
                    curwin.w_cursor.col = pos.col;
                    int col = pos.col;

                    Bytes[] that = { ml_get_curline() };

                    if (vi_lisp && get_indent() == 0)
                        amount = 2;
                    else
                    {
                        Bytes line = that[0];

                        amount = 0;
                        while (that[0].at(0) != NUL && col != 0)
                        {
                            amount += lbr_chartabsize_adv(line, that, amount);
                            col--;
                        }

                        /*
                         * Some keywords require "body" indenting rules (the
                         * non-standard-lisp ones are Scheme special forms):
                         *
                         * (let ((a 1))    instead    (let ((a 1))
                         *   (...))           of           (...))
                         */

                        if (!vi_lisp && (that[0].at(0) == (byte)'(' || that[0].at(0) == (byte)'[') && lisp_match(that[0].plus(1)))
                            amount += 2;
                        else
                        {
                            that[0] = that[0].plus(1);
                            amount++;
                            int firsttry = amount;

                            while (vim_iswhite(that[0].at(0)))
                            {
                                amount += lbr_chartabsize(line, that[0], amount);
                                that[0] = that[0].plus(1);
                            }

                            if (that[0].at(0) != NUL && that[0].at(0) != (byte)';') /* not a comment line */
                            {
                                /* test that[0].at(0) != (byte)'(' to accommodate first let/do argument
                                 * if it is more than one line */
                                if (!vi_lisp && that[0].at(0) != (byte)'(' && that[0].at(0) != (byte)'[')
                                    firsttry++;

                                parencount = 0;
                                boolean quotecount = false;

                                if (vi_lisp
                                        || (that[0].at(0) != (byte)'"'
                                            && that[0].at(0) != (byte)'\''
                                            && that[0].at(0) != (byte)'#'
                                            && (that[0].at(0) < '0' || '9' < that[0].at(0))))
                                {
                                    while (that[0].at(0) != NUL
                                            && (!vim_iswhite(that[0].at(0))
                                                || quotecount
                                                || parencount != 0)
                                            && (!((that[0].at(0) == (byte)'(' || that[0].at(0) == (byte)'[')
                                                    && !quotecount
                                                    && parencount == 0
                                                    && vi_lisp)))
                                    {
                                        if (that[0].at(0) == (byte)'"')
                                            quotecount = !quotecount;
                                        if ((that[0].at(0) == (byte)'(' || that[0].at(0) == (byte)'[') && !quotecount)
                                            parencount++;
                                        if ((that[0].at(0) == (byte)')' || that[0].at(0) == (byte)']') && !quotecount)
                                            --parencount;
                                        if (that[0].at(0) == (byte)'\\' && that[0].at(1) != NUL)
                                            amount += lbr_chartabsize_adv(line, that, amount);
                                        amount += lbr_chartabsize_adv(line, that, amount);
                                    }
                                }
                                while (vim_iswhite(that[0].at(0)))
                                {
                                    amount += lbr_chartabsize(line, that[0], amount);
                                    that[0] = that[0].plus(1);
                                }
                                if (that[0].at(0) == NUL || that[0].at(0) == (byte)';')
                                    amount = firsttry;
                            }
                        }
                    }
                }
            }
            else
                amount = 0;     /* no matching '(' or '[' found, use zero indent */

            COPY_pos(curwin.w_cursor, realpos);

            return amount;
        }
    };

    /*private*/ static void prepare_to_exit()
    {
        /* Ignore SIGHUP, because a dropped connection causes a read error, which
         * makes Vim exit and then handling SIGHUP causes various reentrance problems.
         */
        libC.sigset(SIGHUP, /*SIG_IGN*/null);

        windgoto((int)Rows[0] - 1, 0);

        /*
         * Switch terminal mode back now, so messages end up on the "normal"
         * screen (if there are two screens).
         */
        settmode(TMODE_COOK);
        stoptermcap();
        out_flush();
    }

    /*
     * Preserve files and exit.
     * When called ioBuff must contain a message.
     * NOTE: This may be called from deathtrap() in a signal handler,
     * avoid unsafe functions, such as allocating memory.
     */
    /*private*/ static void preserve_exit()
    {
        prepare_to_exit();

        out_str(ioBuff);
        screen_start();                 /* don't know where cursor is now */
        out_flush();

        ml_close_all();

        out_str(u8("Vim: Finished.\n"));

        getout(1);
    }

    /*
     * Return true if "fname" exists.
     */
    /*private*/ static boolean vim_fexists(Bytes fname)
    {
        stat_C st = new stat_C();

        return (libC.stat(fname, st) == 0);
    }

    /*
     * Check for CTRL-C pressed, but only once in a while.
     * Should be used instead of ui_breakcheck() for functions that check for each line in the file.
     * Calling ui_breakcheck() each time takes too much time, because it can be a system call.
     */

    /*private*/ static final int BREAKCHECK_SKIP = 32;

    /*private*/ static int      breakcheck_count;

    /*private*/ static void line_breakcheck()
    {
        if (BREAKCHECK_SKIP <= ++breakcheck_count)
        {
            breakcheck_count = 0;
            ui_breakcheck();
        }
    }

    /*
     * Like line_breakcheck() but check 10 times less often.
     */
    /*private*/ static void fast_breakcheck()
    {
        if (BREAKCHECK_SKIP * 10 <= ++breakcheck_count)
        {
            breakcheck_count = 0;
            ui_breakcheck();
        }
    }

    /*
     * Generic wildcard expansion code.
     *
     * Characters in "pat" that should not be expanded must be preceded with a backslash:
     * e.g. "/path\ with\ spaces/my\*star*"
     *
     * Return false when no single file was found.
     * In this case "num_file" is not set, and "file" may contain an error message.
     * Return true when some files found.
     * "num_file" is set to the number of matches, "file" to the array of matches.
     */
    /*private*/ static boolean dummy_expand_wildcards(int num_pats, Bytes[] pats, int[] num_files, Bytes[][] files, int flags)
        /* num_pats: number of input patterns */
        /* pats: array of input patterns */
        /* num_files: resulting number of files */
        /* files: array of resulting files */
        /* flags: EW_* flags */
    {
        Growing<Bytes> ga = new Growing<Bytes>(Bytes.class, 30);

        for (int i = 0; i < num_pats; i++)
        {
            Bytes p = backslash_halve_save(pats[i]);

            /* When EW_NOTFOUND is used, always add files and dirs.  Makes "vim c:/" work. */
            if ((flags & EW_NOTFOUND) != 0)
                addfile(ga, p, flags | EW_DIR | EW_FILE);
        }

        num_files[0] = ga.ga_len;
        files[0] = ga.ga_data;

        return (files[0] != null);
    }

    /*
     * Add a file to a file list.  Accepted flags:
     * EW_DIR       add directories
     * EW_FILE      add files
     * EW_NOTFOUND  add even when it doesn't exist
     * EW_ADDSLASH  add slash after directory name
     * EW_ALLLINKS  add symlink also when the referred file does not exist
     */
    /*private*/ static void addfile(Growing<Bytes> gap, Bytes f, int flags)
        /* f: filename */
    {
        /* if the file/dir/link doesn't exist, may not add it */
        if ((flags & EW_NOTFOUND) == 0)
        {
            stat_C st = new stat_C();
            if ((flags & EW_ALLLINKS) != 0 ? libC.lstat(f, st) < 0 : mch_getperm(f) < 0)
                return;
        }

        boolean isdir = mch_isdir(f);
        if ((isdir && (flags & EW_DIR) == 0) || (!isdir && (flags & EW_FILE) == 0))
            return;

        Bytes p = new Bytes(strlen(f) + (isdir ? 1 : 0) + 1);
        STRCPY(p, f);
        /* Append a slash or backslash after directory names if none is present. */
        if (isdir && (flags & EW_ADDSLASH) != 0)
            add_pathsep(p);

        gap.ga_grow(1);
        gap.ga_data[gap.ga_len++] = p;
    }

    /*
     * Return true when need to go to Insert mode because of 'insertmode'.
     * Don't do this when still processing a command or a mapping.
     * Don't do this when inside a ":normal" command.
     */
    /*private*/ static boolean goto_im()
    {
        return (p_im[0] && stuff_empty() && typebuf_typed());
    }

    /*
     * Return true if in the current mode we need to use virtual.
     */
    /*private*/ static boolean virtual_active()
    {
        /* While an operator is being executed we return "virtual_op",
         * because VIsual_active has already been reset,
         * thus we can't check for "block" being used. */
        if (virtual_op != MAYBE)
            return (virtual_op != FALSE);

        return (ve_flags[0] == VE_ALL
                || ((ve_flags[0] & VE_BLOCK) != 0 && VIsual_active && VIsual_mode == Ctrl_V)
                || ((ve_flags[0] & VE_INSERT) != 0 && (State & INSERT) != 0));
    }

    /*
     * Get the screen position of the cursor.
     */
    /*private*/ static int getviscol()
    {
        int[] x = new int[1];
        getvvcol(curwin, curwin.w_cursor, x, null, null);
        return x[0];
    }

    /*
     * Get the screen position of character col with a coladd in the cursor line.
     */
    /*private*/ static int getviscol2(int col, int coladd)
    {
        pos_C pos = new pos_C();
        pos.lnum = curwin.w_cursor.lnum;
        pos.col = col;
        pos.coladd = coladd;

        int[] x = new int[1];
        getvvcol(curwin, pos, x, null, null);
        return x[0];
    }

    /*
     * Go to column "wcol", and add/insert white space as necessary to get the
     * cursor in that column.
     * The caller must have saved the cursor line for undo!
     */
    /*private*/ static boolean coladvance_force(int wcol)
    {
        boolean rc = coladvance2(curwin.w_cursor, true, false, wcol);

        if (wcol == MAXCOL)
            curwin.w_valid &= ~VALID_VIRTCOL;
        else
        {
            /* Virtcol is valid. */
            curwin.w_valid |= VALID_VIRTCOL;
            curwin.w_virtcol = wcol;
        }
        return rc;
    }

    /*
     * Try to advance the Cursor to the specified screen column.
     * If virtual editing: fine tune the cursor position.
     * Note that all virtual positions off the end of a line should share
     * a curwin.w_cursor.col value (n.b. this is equal to strlen(line)),
     * beginning at coladd 0.
     *
     * return true if desired column is reached, false if not
     */
    /*private*/ static boolean coladvance(int wcol)
    {
        boolean rc = getvpos(curwin.w_cursor, wcol);

        if (wcol == MAXCOL || rc == false)
            curwin.w_valid &= ~VALID_VIRTCOL;
        else if (ml_get_cursor().at(0) != TAB)
        {
            /* Virtcol is valid when not on a TAB. */
            curwin.w_valid |= VALID_VIRTCOL;
            curwin.w_virtcol = wcol;
        }
        return rc;
    }

    /*
     * Return in "pos" the position of the cursor advanced to screen column "wcol".
     * return true if desired column is reached, false if not
     */
    /*private*/ static boolean getvpos(pos_C pos, int wcol)
    {
        return coladvance2(pos, false, virtual_active(), wcol);
    }

    /*private*/ static boolean coladvance2(pos_C pos, boolean addspaces, boolean finetune, int wcol)
        /* addspaces: change the text to achieve our goal? */
        /* finetune: change char offset for the exact column */
        /* wcol: column to move to */
    {
        int col = 0;
        int csize = 0;
        int[] head = { 0 };

        boolean one_more = (State & INSERT) != 0
                        || restart_edit != NUL
                        || (VIsual_active && p_sel[0].at(0) != (byte)'o')
                        || ((ve_flags[0] & VE_ONEMORE) != 0 && wcol < MAXCOL);

        Bytes line = ml_get_buf(curbuf, pos.lnum, false);

        int idx;
        if (MAXCOL <= wcol)
        {
            idx = strlen(line) - 1 + (one_more ? 1 : 0);
            col = wcol;

            if ((addspaces || finetune) && !VIsual_active)
            {
                curwin.w_curswant = linetabsize(line) + (one_more ? 1 : 0);
                if (0 < curwin.w_curswant)
                    --curwin.w_curswant;
            }
        }
        else
        {
            int width = curwin.w_width - win_col_off(curwin);

            if (finetune
                    && curwin.w_onebuf_opt.wo_wrap[0]
                    && curwin.w_width != 0
                    && width <= wcol)
            {
                csize = linetabsize(line);
                if (0 < csize)
                    csize--;

                if (csize / width < wcol / width && ((State & INSERT) == 0 || csize + 1 < wcol))
                {
                    /* In case of line wrapping don't move the cursor beyond the
                     * right screen edge.  In Insert mode allow going just beyond
                     * the last character (like what happens when typing and
                     * reaching the right window edge). */
                    wcol = (csize / width + 1) * width - 1;
                }
            }

            Bytes ptr = line;
            while (col <= wcol && ptr.at(0) != NUL)
            {
                /* Count a tab for what it's worth (if list mode not on). */
                csize = win_lbr_chartabsize(curwin, line, ptr, col, head);
                ptr = ptr.plus(us_ptr2len_cc(ptr));
                col += csize;
            }
            idx = BDIFF(ptr, line);
            /*
             * Handle all the special cases.  The virtual_active() check
             * is needed to ensure that a virtual position off the end of
             * a line has the correct indexing.  The one_more comparison
             * replaces an explicit add of one_more later on.
             */
            if (wcol < col || (!virtual_active() && !one_more))
            {
                idx -= 1;
                /* Don't count the chars from 'showbreak'. */
                csize -= head[0];
                col -= csize;
            }

            if (virtual_active() && addspaces && ((col != wcol && col != wcol + 1) || 1 < csize))
            {
                /* 'virtualedit' is set: The difference between wcol and col is filled with spaces. */

                if (line.at(idx) == NUL)
                {
                    /* Append spaces. */
                    int correct = wcol - col;
                    Bytes newline = new Bytes(idx + correct + 1);

                    for (int t = 0; t < idx; t++)
                        newline.be(t, line.at(t));

                    for (int t = 0; t < correct; t++)
                        newline.be(t + idx, (byte)' ');

                    newline.be(idx + correct, NUL);

                    ml_replace(pos.lnum, newline, false);
                    changed_bytes(pos.lnum, idx);
                    idx += correct;
                    col = wcol;
                }
                else
                {
                    /* Break a tab. */
                    int linelen = strlen(line);
                    int correct = wcol - col - csize + 1; /* negative!! */

                    if (csize < -correct)
                        return false;

                    Bytes newline = new Bytes(linelen + csize);

                    int s = 0;
                    for (int t = 0; t < linelen; t++)
                    {
                        if (t != idx)
                            newline.be(s++, line.at(t));
                        else
                            for (int v = 0; v < csize; v++)
                                newline.be(s++, (byte)' ');
                    }

                    newline.be(linelen + csize - 1, NUL);

                    ml_replace(pos.lnum, newline, false);
                    changed_bytes(pos.lnum, idx);
                    idx += (csize - 1 + correct);
                    col += correct;
                }
            }
        }

        if (idx < 0)
            pos.col = 0;
        else
            pos.col = idx;

        pos.coladd = 0;

        if (finetune)
        {
            if (wcol == MAXCOL)
            {
                /* The width of the last character is used to set coladd. */
                if (!one_more)
                {
                    int[] scol = new int[1];
                    int[] ecol = new int[1];

                    getvcol(curwin, pos, scol, null, ecol);
                    pos.coladd = ecol[0] - scol[0];
                }
            }
            else
            {
                int b = wcol - col;

                /* The difference between wcol and col is used to set coladd. */
                if (0 < b && b < (MAXCOL - 2 * curwin.w_width))
                    pos.coladd = b;

                col += b;
            }
        }

        /* prevent from moving onto a trail byte */
        mb_adjust_pos(curbuf, pos);

        if (col < wcol)
            return false;

        return true;
    }

    /*
     * Increment the cursor position.  See inc() for return values.
     */
    /*private*/ static int inc_cursor()
    {
        return inc(curwin.w_cursor);
    }

    /*
     * Increment the line pointer "lp" crossing line boundaries as necessary.
     * Return 1 when going to the next line.
     * Return 2 when moving forward onto a NUL at the end of the line).
     * Return -1 when at the end of file.
     * Return 0 otherwise.
     */
    /*private*/ static int inc(pos_C lp)
    {
        Bytes p = ml_get_pos(lp);

        if (p.at(0) != NUL)      /* still within line, move to next char (may be NUL) */
        {
            int l = us_ptr2len_cc(p);

            lp.col += l;
            return ((p.at(l) != NUL) ? 0 : 2);
        }
        if (lp.lnum != curbuf.b_ml.ml_line_count)   /* there is a next line */
        {
            lp.col = 0;
            lp.lnum++;
            lp.coladd = 0;
            return 1;
        }
        return -1;
    }

    /*
     * incl(lp): same as inc(), but skip the NUL at the end of non-empty lines
     */
    /*private*/ static int incl(pos_C lp)
    {
        int r = inc(lp);

        if (1 <= r && lp.col != 0)
            r = inc(lp);

        return r;
    }

    /*
     * dec(p)
     *
     * Decrement the line pointer 'p' crossing line boundaries as necessary.
     * Return 1 when crossing a line, -1 when at start of file, 0 otherwise.
     */
    /*private*/ static int dec_cursor()
    {
        return dec(curwin.w_cursor);
    }

    /*private*/ static int dec(pos_C lp)
    {
        lp.coladd = 0;

        if (0 < lp.col)         /* still within line */
        {
            lp.col--;
            Bytes p = ml_get(lp.lnum);
            lp.col -= us_head_off(p, p.plus(lp.col));
            return 0;
        }

        if (1 < lp.lnum)        /* there is a prior line */
        {
            lp.lnum--;
            Bytes p = ml_get(lp.lnum);
            lp.col = strlen(p);
            lp.col -= us_head_off(p, p.plus(lp.col));
            return 1;
        }

        return -1;                  /* at start of file */
    }

    /*
     * decl(lp): same as dec(), but skip the NUL at the end of non-empty lines
     */
    /*private*/ static int decl(pos_C lp)
    {
        int r = dec(lp);

        if (r == 1 && lp.col != 0)
            r = dec(lp);

        return r;
    }

    /*
     * Get the line number relative to the current cursor position,
     * i.e. the difference between line number and cursor position.
     * Only look for lines that can be visible, folded lines don't count.
     */
    /*private*/ static long get_cursor_rel_lnum(window_C wp, long lnum)
        /* lnum: line number to get the result for */
    {
        return lnum - wp.w_cursor.lnum;
    }

    /*
     * Make sure curwin.w_cursor.lnum is valid.
     */
    /*private*/ static void check_cursor_lnum()
    {
        if (curwin.w_cursor.lnum > curbuf.b_ml.ml_line_count)
            curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;

        if (curwin.w_cursor.lnum <= 0)
            curwin.w_cursor.lnum = 1;
    }

    /*
     * Make sure curwin.w_cursor.col is valid.
     */
    /*private*/ static void check_cursor_col()
    {
        check_cursor_col_win(curwin);
    }

    /*
     * Make sure win.w_cursor.col is valid.
     */
    /*private*/ static void check_cursor_col_win(window_C win)
    {
        int oldcol = win.w_cursor.col;
        int oldcoladd = win.w_cursor.col + win.w_cursor.coladd;

        int len = strlen(ml_get_buf(win.w_buffer, win.w_cursor.lnum, false));
        if (len == 0)
            win.w_cursor.col = 0;
        else if (len <= win.w_cursor.col)
        {
            /* Allow cursor past end-of-line when:
             * - in Insert mode or restarting Insert mode
             * - in Visual mode and 'selection' isn't "old"
             * - 'virtualedit' is set */
            if ((State & INSERT) != 0 || restart_edit != 0
                    || (VIsual_active && p_sel[0].at(0) != (byte)'o')
                    || (ve_flags[0] & VE_ONEMORE) != 0
                    || virtual_active())
                win.w_cursor.col = len;
            else
            {
                win.w_cursor.col = len - 1;
                /* Move the cursor to the head byte. */
                mb_adjust_pos(win.w_buffer, win.w_cursor);
            }
        }
        else if (win.w_cursor.col < 0)
            win.w_cursor.col = 0;

        /* If virtual editing is on, we can leave the cursor on the old position,
         * only we must set it to virtual.  But don't do it when at the end of the line. */
        if (oldcol == MAXCOL)
            win.w_cursor.coladd = 0;
        else if (ve_flags[0] == VE_ALL)
        {
            if (win.w_cursor.col < oldcoladd)
                win.w_cursor.coladd = oldcoladd - win.w_cursor.col;
            else
                /* avoid weird number when there is a miscalculation or overflow */
                win.w_cursor.coladd = 0;
        }
    }

    /*
     * make sure curwin.w_cursor in on a valid character
     */
    /*private*/ static void check_cursor()
    {
        check_cursor_lnum();
        check_cursor_col();
    }

    /*
     * Make sure curwin.w_cursor is not on the NUL at the end of the line.
     * Allow it when in Visual mode and 'selection' is not "old".
     */
    /*private*/ static void adjust_cursor_col()
    {
        if (0 < curwin.w_cursor.col
                && (!VIsual_active || p_sel[0].at(0) == (byte)'o')
                && gchar_cursor() == NUL)
            --curwin.w_cursor.col;
    }

    /*
     * When curwin.w_leftcol has changed, adjust the cursor position.
     * Return true if the cursor was moved.
     */
    /*private*/ static boolean leftcol_changed()
    {
        boolean retval = false;

        changed_cline_bef_curs();
        long lastcol = curwin.w_leftcol + curwin.w_width - curwin_col_off() - 1;
        validate_virtcol();

        /*
         * If the cursor is right or left of the screen, move it to last or first character.
         */
        if ((int)(lastcol - p_siso[0]) < curwin.w_virtcol)
        {
            retval = true;
            coladvance((int)(lastcol - p_siso[0]));
        }
        else if (curwin.w_virtcol < curwin.w_leftcol + p_siso[0])
        {
            retval = true;
            coladvance((int)(curwin.w_leftcol + p_siso[0]));
        }

        /*
         * If the start of the character under the cursor is not on the screen,
         * advance the cursor one more char.  If this fails (last char of the line),
         * adjust the scrolling.
         */
        int[] s = new int[1];
        int[] e = new int[1];
        getvvcol(curwin, curwin.w_cursor, s, null, e);
        if ((int)lastcol < e[0])
        {
            retval = true;
            coladvance(s[0] - 1);
        }
        else if (s[0] < curwin.w_leftcol)
        {
            retval = true;
            if (coladvance(e[0] + 1) == false)     /* there isn't another character */
            {
                curwin.w_leftcol = s[0];           /* adjust w_leftcol instead */
                changed_cline_bef_curs();
            }
        }

        if (retval)
            curwin.w_set_curswant = true;
        redraw_later(NOT_VALID);
        return retval;
    }

    /**********************************************************************
     * Various routines dealing with allocation and deallocation of memory.
     */

    /*
     * Copy "string" into newly allocated memory.
     */
    /*private*/ static Bytes STRDUP(Bytes string)
    {
        int len = strlen(string) + 1;
        Bytes s = new Bytes(len);
        BCOPY(s, string, len);
        return s;
    }

    /*
     * Copy up to "len" bytes of "string" into newly allocated memory and terminate with a NUL.
     * The allocated memory always has size "len + 1", also when "string" is shorter.
     */
    /*private*/ static Bytes STRNDUP(Bytes string, int len)
    {
        Bytes s = new Bytes(len + 1);
        STRNCPY(s, string, len);
        s.be(len, NUL);
        return s;
    }

    /*
     * Same as STRDUP(), but any characters found in esc_chars are preceded by a backslash.
     */
    /*private*/ static Bytes vim_strsave_escaped(Bytes string, Bytes esc_chars)
    {
        return vim_strsave_escaped_ext(string, esc_chars, '\\');
    }

    /*
     * Same as vim_strsave_escaped().
     * Escape the characters with "cc".
     */
    /*private*/ static Bytes vim_strsave_escaped_ext(Bytes string, Bytes esc_chars, int cc)
    {
        /*
         * First count the number of backslashes required.
         * Then allocate the memory and insert them.
         */
        int length = 1;                         /* count the trailing NUL */
        for (Bytes p = string; p.at(0) != NUL; p = p.plus(1))
        {
            int l = us_ptr2len_cc(p);
            if (1 < l)
            {
                length += l;                    /* count a multibyte char */
                p = p.plus(l - 1);
                continue;
            }
            if (vim_strchr(esc_chars, p.at(0)) != null)
                length++;                       /* count a backslash */
            length++;                           /* count an ordinary char */
        }

        Bytes escaped_string = new Bytes(length);

        Bytes p2 = escaped_string;
        for (Bytes p = string; p.at(0) != NUL; p = p.plus(1))
        {
            int l = us_ptr2len_cc(p);
            if (1 < l)
            {
                BCOPY(p2, p, l);
                p2 = p2.plus(l);
                p = p.plus(l - 1);                     /* skip multibyte char */
                continue;
            }
            if (vim_strchr(esc_chars, p.at(0)) != null)
                (p2 = p2.plus(1)).be(-1, cc);
            (p2 = p2.plus(1)).be(-1, p.at(0));
        }
        p2.be(0, NUL);

        return escaped_string;
    }

    /*
     * ASCII lower-to-upper case translation, language independent.
     */
    /*private*/ static void vim_strup(Bytes p)
    {
        if (p != null)
            for (int c; (c = p.at(0)) != NUL; )
                (p = p.plus(1)).be(-1, (c < 'a' || 'z' < c) ? c : (c - 0x20));
    }

    /*
     * Like STRDUP(), but make all characters uppercase.
     * This uses ASCII lower-to-upper case translation, language independent.
     */
    /*private*/ static Bytes vim_strsave_up(Bytes string)
    {
        Bytes p = STRDUP(string);
        vim_strup(p);
        return p;
    }

    /*
     * Like STRNDUP(), but make all characters uppercase.
     * This uses ASCII lower-to-upper case translation, language independent.
     */
    /*private*/ static Bytes vim_strnsave_up(Bytes string, int len)
    {
        Bytes p = STRNDUP(string, len);
        vim_strup(p);
        return p;
    }

    /*
     * Make string "s" all upper-case and return it in allocated memory.
     * Handles multi-byte characters as well as possible.
     */
    /*private*/ static Bytes strup_save(Bytes orig)
    {
        Bytes res = STRDUP(orig);

        for (Bytes p = res; p.at(0) != NUL; )
        {
            int c = us_ptr2char(p);
            int uc = utf_toupper(c);

            /* Reallocate string when byte count changes.
             * This is rare, thus it's OK to do another calloc()/free(). */
            int l = us_ptr2len(p);
            int newl = utf_char2len(uc);
            if (newl != l)
            {
                Bytes s = new Bytes(strlen(res) + 1 + newl - l);

                BCOPY(s, res, BDIFF(p, res));
                STRCPY(s.plus(BDIFF(p, res) + newl), p.plus(l));
                p = s.plus(BDIFF(p, res));
                res = s;
            }

            utf_char2bytes(uc, p);
            p = p.plus(newl);
        }

        return res;
    }

    /*
     * copy a space a number of times
     */
    /*private*/ static void copy_spaces(Bytes s, int n)
    {
        for (int i = 0; i < n; i++)
            s.be(i, (byte)' ');
    }

    /*
     * Copy a character a number of times.
     * Does not work for multi-byte characters!
     */
    /*private*/ static void copy_chars(Bytes s, int n, int c)
    {
        for (int i = 0; i < n; i++)
            s.be(i, c);
    }

    /*
     * delete spaces at the end of a string
     */
    /*private*/ static void del_trailing_spaces(Bytes p)
    {
        for (int i = strlen(p); 0 < --i && vim_iswhite(p.at(i)) && p.at(i - 1) != (byte)'\\' && p.at(i - 1) != Ctrl_V; )
            p.be(i, NUL);
    }

    /*
     * Like strncpy(), but always terminate the result with one NUL.
     * "dst" must be "len + 1" long!
     */
    /*private*/ static void vim_strncpy(Bytes dst, Bytes src, int len)
    {
        STRNCPY(dst, src, len);
        dst.be(len, NUL);
    }

    /*
     * Like strcat(), but make sure the result fits in "size" bytes
     * and is always NUL terminated.
     */
    /*private*/ static void vim_strcat(Bytes dst, Bytes src, int size)
    {
        int dlen = strlen(dst), slen = strlen(src);

        if (size < dlen + slen + 1)
        {
            BCOPY(dst, dlen, src, 0, size - dlen - 1);
            dst.be(size - 1, NUL);
        }
        else
            STRCPY(dst.plus(dlen), src);
    }

    /*
     * Isolate one part of a string option where parts are separated with "sep_chars".
     * The part is copied into "buf[maxlen]".
     * "*option" is advanced to the next part.
     * The length is returned.
     */
    /*private*/ static int copy_option_part(Bytes[] option, Bytes buf, int maxlen, Bytes sep_chars)
    {
        int len = 0;
        Bytes p = option[0];

        /* skip '.' at start of option part, for 'suffixes' */
        if (p.at(0) == (byte)'.')
            buf.be(len++, (p = p.plus(1)).at(-1));
        while (p.at(0) != NUL && vim_strchr(sep_chars, p.at(0)) == null)
        {
            /*
             * Skip backslash before a separator character and space.
             */
            if (p.at(0) == (byte)'\\' && vim_strchr(sep_chars, p.at(1)) != null)
                p = p.plus(1);
            if (len < maxlen - 1)
                buf.be(len++, p.at(0));
            p = p.plus(1);
        }
        buf.be(len, NUL);

        if (p.at(0) != NUL && p.at(0) != (byte)',') /* skip non-standard separator */
            p = p.plus(1);
        p = skip_to_option_part(p); /* "p" points to next file name */

        option[0] = p;
        return len;
    }

    /*
     * Version of strchr() and strrchr() that handle unsigned char strings
     * with characters from 128 to 255 correctly.  It also doesn't return
     * a pointer to the NUL at the end of the string.
     */
    /*private*/ static Bytes vim_strchr(Bytes string, int c)
    {
        for (Bytes p = string; p.at(0) != NUL; p = p.plus(us_ptr2len_cc(p)))
            if (us_ptr2char(p) == c)
                return p;

        return null;
    }

    /*
     * Version of strchr() that only works for bytes and handles unsigned char
     * strings with characters above 128 correctly.  It also doesn't return a
     * pointer to the NUL at the end of the string.
     */
    /*private*/ static Bytes vim_strbyte(Bytes string, byte b)
    {
        for (Bytes p = string; p.at(0) != NUL; p = p.plus(1))
            if (p.at(0) == b)
                return p;

        return null;
    }

    /*
     * Search for last occurrence of "b" in "string".
     * Return null if not found.
     * Does not handle multi-byte char for "b"!
     */
    /*private*/ static Bytes vim_strrchr(Bytes string, byte b)
    {
        Bytes q = null;

        for (Bytes p = string; p.at(0) != NUL; p = p.plus(us_ptr2len_cc(p)))
            if (p.at(0) == b)
                q = p;

        return q;
    }

    /*
     * Vim has its own isspace() function, because on some machines isspace()
     * can't handle characters above 128.
     */
    /*private*/ static boolean vim_isspace(int x)
    {
        return ((9 <= x && x <= 13) || x == ' ');
    }

    /*
     * Some useful tables.
     */

    /*private*/ static final class modmasktable_C
    {
        int         mod_mask;       /* bit-mask for particular key modifier */
        int         mod_flag;       /* bit(s) for particular key modifier */
        byte        name;           /* single letter name of modifier */

        /*private*/ modmasktable_C(int mod_mask, int mod_flag, byte name)
        {
            this.mod_mask = mod_mask;
            this.mod_flag = mod_flag;
            this.name = name;
        }
    }

    /*private*/ static modmasktable_C[] mod_mask_table = new modmasktable_C[]
    {
        new modmasktable_C(MOD_MASK_ALT,         MOD_MASK_ALT,    (byte)'M'),
        new modmasktable_C(MOD_MASK_META,        MOD_MASK_META,   (byte)'T'),
        new modmasktable_C(MOD_MASK_CTRL,        MOD_MASK_CTRL,   (byte)'C'),
        new modmasktable_C(MOD_MASK_SHIFT,       MOD_MASK_SHIFT,  (byte)'S'),
        new modmasktable_C(MOD_MASK_MULTI_CLICK, MOD_MASK_2CLICK, (byte)'2'),
        new modmasktable_C(MOD_MASK_MULTI_CLICK, MOD_MASK_3CLICK, (byte)'3'),
        new modmasktable_C(MOD_MASK_MULTI_CLICK, MOD_MASK_4CLICK, (byte)'4'),
        /* 'A' must be the last one */
        new modmasktable_C(MOD_MASK_ALT,         MOD_MASK_ALT,    (byte)'A')
    };

    /*
     * Shifted key terminal codes and their unshifted equivalent.
     * Don't add mouse codes here, they are handled separately!
     */
    /*private*/ static final int MOD_KEYS_ENTRY_SIZE = 5;

    /*private*/ static byte[] modifier_keys_table =
    {
    /*  mod mask        with modifier               without modifier */
        MOD_MASK_SHIFT, (byte)'&', (byte)'9',                   (byte)'@', (byte)'1',       /* begin */
        MOD_MASK_SHIFT, (byte)'&', (byte)'0',                   (byte)'@', (byte)'2',       /* cancel */
        MOD_MASK_SHIFT, (byte)'*', (byte)'1',                   (byte)'@', (byte)'4',       /* command */
        MOD_MASK_SHIFT, (byte)'*', (byte)'2',                   (byte)'@', (byte)'5',       /* copy */
        MOD_MASK_SHIFT, (byte)'*', (byte)'3',                   (byte)'@', (byte)'6',       /* create */
        MOD_MASK_SHIFT, (byte)'*', (byte)'4',                   (byte)'k', (byte)'D',       /* delete char */
        MOD_MASK_SHIFT, (byte)'*', (byte)'5',                   (byte)'k', (byte)'L',       /* delete line */
        MOD_MASK_SHIFT, (byte)'*', (byte)'7',                   (byte)'@', (byte)'7',       /* end */
        MOD_MASK_CTRL,  KS_EXTRA, KE_C_END,   (byte)'@', (byte)'7',       /* end */
        MOD_MASK_SHIFT, (byte)'*', (byte)'9',                   (byte)'@', (byte)'9',       /* exit */
        MOD_MASK_SHIFT, (byte)'*', (byte)'0',                   (byte)'@', (byte)'0',       /* find */
        MOD_MASK_SHIFT, (byte)'#', (byte)'1',                   (byte)'%', (byte)'1',       /* help */
        MOD_MASK_SHIFT, (byte)'#', (byte)'2',                   (byte)'k', (byte)'h',       /* home */
        MOD_MASK_CTRL,  KS_EXTRA, KE_C_HOME,  (byte)'k', (byte)'h',       /* home */
        MOD_MASK_SHIFT, (byte)'#', (byte)'3',                   (byte)'k', (byte)'I',       /* insert */
        MOD_MASK_SHIFT, (byte)'#', (byte)'4',                   (byte)'k', (byte)'l',       /* left arrow */
        MOD_MASK_CTRL,  KS_EXTRA, KE_C_LEFT,  (byte)'k', (byte)'l',       /* left arrow */
        MOD_MASK_SHIFT, (byte)'%', (byte)'a',                   (byte)'%', (byte)'3',       /* message */
        MOD_MASK_SHIFT, (byte)'%', (byte)'b',                   (byte)'%', (byte)'4',       /* move */
        MOD_MASK_SHIFT, (byte)'%', (byte)'c',                   (byte)'%', (byte)'5',       /* next */
        MOD_MASK_SHIFT, (byte)'%', (byte)'d',                   (byte)'%', (byte)'7',       /* options */
        MOD_MASK_SHIFT, (byte)'%', (byte)'e',                   (byte)'%', (byte)'8',       /* previous */
        MOD_MASK_SHIFT, (byte)'%', (byte)'f',                   (byte)'%', (byte)'9',       /* print */
        MOD_MASK_SHIFT, (byte)'%', (byte)'g',                   (byte)'%', (byte)'0',       /* redo */
        MOD_MASK_SHIFT, (byte)'%', (byte)'h',                   (byte)'&', (byte)'3',       /* replace */
        MOD_MASK_SHIFT, (byte)'%', (byte)'i',                   (byte)'k', (byte)'r',       /* right arr. */
        MOD_MASK_CTRL,  KS_EXTRA, KE_C_RIGHT, (byte)'k', (byte)'r',       /* right arr. */
        MOD_MASK_SHIFT, (byte)'%', (byte)'j',                   (byte)'&', (byte)'5',       /* resume */
        MOD_MASK_SHIFT, (byte)'!', (byte)'1',                   (byte)'&', (byte)'6',       /* save */
        MOD_MASK_SHIFT, (byte)'!', (byte)'2',                   (byte)'&', (byte)'7',       /* suspend */
        MOD_MASK_SHIFT, (byte)'!', (byte)'3',                   (byte)'&', (byte)'8',       /* undo */
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_UP,    (byte)'k', (byte)'u',       /* up arrow */
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_DOWN,  (byte)'k', (byte)'d',       /* down arrow */

                                                                    /* vt100 F1 */
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_XF1,   KS_EXTRA, KE_XF1,
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_XF2,   KS_EXTRA, KE_XF2,
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_XF3,   KS_EXTRA, KE_XF3,
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_XF4,   KS_EXTRA, KE_XF4,

        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F1,    (byte)'k', (byte)'1',       /* F1 */
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F2,    (byte)'k', (byte)'2',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F3,    (byte)'k', (byte)'3',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F4,    (byte)'k', (byte)'4',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F5,    (byte)'k', (byte)'5',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F6,    (byte)'k', (byte)'6',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F7,    (byte)'k', (byte)'7',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F8,    (byte)'k', (byte)'8',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F9,    (byte)'k', (byte)'9',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F10,   (byte)'k', (byte)';',       /* F10 */

        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F11,   (byte)'F', (byte)'1',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F12,   (byte)'F', (byte)'2',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F13,   (byte)'F', (byte)'3',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F14,   (byte)'F', (byte)'4',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F15,   (byte)'F', (byte)'5',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F16,   (byte)'F', (byte)'6',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F17,   (byte)'F', (byte)'7',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F18,   (byte)'F', (byte)'8',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F19,   (byte)'F', (byte)'9',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F20,   (byte)'F', (byte)'A',

        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F21,   (byte)'F', (byte)'B',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F22,   (byte)'F', (byte)'C',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F23,   (byte)'F', (byte)'D',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F24,   (byte)'F', (byte)'E',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F25,   (byte)'F', (byte)'F',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F26,   (byte)'F', (byte)'G',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F27,   (byte)'F', (byte)'H',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F28,   (byte)'F', (byte)'I',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F29,   (byte)'F', (byte)'J',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F30,   (byte)'F', (byte)'K',

        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F31,   (byte)'F', (byte)'L',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F32,   (byte)'F', (byte)'M',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F33,   (byte)'F', (byte)'N',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F34,   (byte)'F', (byte)'O',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F35,   (byte)'F', (byte)'P',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F36,   (byte)'F', (byte)'Q',
        MOD_MASK_SHIFT, KS_EXTRA, KE_S_F37,   (byte)'F', (byte)'R',

                                                                /* TAB pseudo code */
        MOD_MASK_SHIFT, (byte)'k', (byte)'B',                   KS_EXTRA, KE_TAB,

        NUL
    };

    /*private*/ static final class key_name_C
    {
        int     key;        /* special key code or ascii value */
        Bytes name;       /* name of key */

        /*private*/ key_name_C(int key, Bytes name)
        {
            this.key = key;
            this.name = name;
        }
    }

    /*private*/ static key_name_C[] key_names_table = new key_name_C[]
    {
        new key_name_C(' ',              u8("Space")           ),
        new key_name_C(TAB,              u8("Tab")             ),
        new key_name_C(K_TAB,            u8("Tab")             ),
        new key_name_C(NL,               u8("NL")              ),
        new key_name_C(NL,               u8("NewLine")         ),  /* alternative name */
        new key_name_C(NL,               u8("LineFeed")        ),  /* alternative name */
        new key_name_C(NL,               u8("LF")              ),  /* alternative name */
        new key_name_C(CAR,              u8("CR")              ),
        new key_name_C(CAR,              u8("Return")          ),  /* alternative name */
        new key_name_C(CAR,              u8("Enter")           ),  /* alternative name */
        new key_name_C(K_BS,             u8("BS")              ),
        new key_name_C(K_BS,             u8("BackSpace")       ),  /* alternative name */
        new key_name_C(ESC,              u8("Esc")             ),
        new key_name_C(char_u(CSI),      u8("CSI")             ),
        new key_name_C(K_CSI,            u8("xCSI")            ),
        new key_name_C('|',              u8("Bar")             ),
        new key_name_C('\\',             u8("Bslash")          ),
        new key_name_C(K_DEL,            u8("Del")             ),
        new key_name_C(K_DEL,            u8("Delete")          ),  /* alternative name */
        new key_name_C(K_KDEL,           u8("kDel")            ),
        new key_name_C(K_UP,             u8("Up")              ),
        new key_name_C(K_DOWN,           u8("Down")            ),
        new key_name_C(K_LEFT,           u8("Left")            ),
        new key_name_C(K_RIGHT,          u8("Right")           ),
        new key_name_C(K_XUP,            u8("xUp")             ),
        new key_name_C(K_XDOWN,          u8("xDown")           ),
        new key_name_C(K_XLEFT,          u8("xLeft")           ),
        new key_name_C(K_XRIGHT,         u8("xRight")          ),

        new key_name_C(K_F1,             u8("F1")              ),
        new key_name_C(K_F2,             u8("F2")              ),
        new key_name_C(K_F3,             u8("F3")              ),
        new key_name_C(K_F4,             u8("F4")              ),
        new key_name_C(K_F5,             u8("F5")              ),
        new key_name_C(K_F6,             u8("F6")              ),
        new key_name_C(K_F7,             u8("F7")              ),
        new key_name_C(K_F8,             u8("F8")              ),
        new key_name_C(K_F9,             u8("F9")              ),
        new key_name_C(K_F10,            u8("F10")             ),

        new key_name_C(K_F11,            u8("F11")             ),
        new key_name_C(K_F12,            u8("F12")             ),
        new key_name_C(K_F13,            u8("F13")             ),
        new key_name_C(K_F14,            u8("F14")             ),
        new key_name_C(K_F15,            u8("F15")             ),
        new key_name_C(K_F16,            u8("F16")             ),
        new key_name_C(K_F17,            u8("F17")             ),
        new key_name_C(K_F18,            u8("F18")             ),
        new key_name_C(K_F19,            u8("F19")             ),
        new key_name_C(K_F20,            u8("F20")             ),

        new key_name_C(K_F21,            u8("F21")             ),
        new key_name_C(K_F22,            u8("F22")             ),
        new key_name_C(K_F23,            u8("F23")             ),
        new key_name_C(K_F24,            u8("F24")             ),
        new key_name_C(K_F25,            u8("F25")             ),
        new key_name_C(K_F26,            u8("F26")             ),
        new key_name_C(K_F27,            u8("F27")             ),
        new key_name_C(K_F28,            u8("F28")             ),
        new key_name_C(K_F29,            u8("F29")             ),
        new key_name_C(K_F30,            u8("F30")             ),

        new key_name_C(K_F31,            u8("F31")             ),
        new key_name_C(K_F32,            u8("F32")             ),
        new key_name_C(K_F33,            u8("F33")             ),
        new key_name_C(K_F34,            u8("F34")             ),
        new key_name_C(K_F35,            u8("F35")             ),
        new key_name_C(K_F36,            u8("F36")             ),
        new key_name_C(K_F37,            u8("F37")             ),

        new key_name_C(K_XF1,            u8("xF1")             ),
        new key_name_C(K_XF2,            u8("xF2")             ),
        new key_name_C(K_XF3,            u8("xF3")             ),
        new key_name_C(K_XF4,            u8("xF4")             ),

        new key_name_C(K_HELP,           u8("Help")            ),
        new key_name_C(K_UNDO,           u8("Undo")            ),
        new key_name_C(K_INS,            u8("Insert")          ),
        new key_name_C(K_INS,            u8("Ins")             ),  /* alternative name */
        new key_name_C(K_KINS,           u8("kInsert")         ),
        new key_name_C(K_HOME,           u8("Home")            ),
        new key_name_C(K_KHOME,          u8("kHome")           ),
        new key_name_C(K_XHOME,          u8("xHome")           ),
        new key_name_C(K_ZHOME,          u8("zHome")           ),
        new key_name_C(K_END,            u8("End")             ),
        new key_name_C(K_KEND,           u8("kEnd")            ),
        new key_name_C(K_XEND,           u8("xEnd")            ),
        new key_name_C(K_ZEND,           u8("zEnd")            ),
        new key_name_C(K_PAGEUP,         u8("PageUp")          ),
        new key_name_C(K_PAGEDOWN,       u8("PageDown")        ),
        new key_name_C(K_KPAGEUP,        u8("kPageUp")         ),
        new key_name_C(K_KPAGEDOWN,      u8("kPageDown")       ),

        new key_name_C(K_KPLUS,          u8("kPlus")           ),
        new key_name_C(K_KMINUS,         u8("kMinus")          ),
        new key_name_C(K_KDIVIDE,        u8("kDivide")         ),
        new key_name_C(K_KMULTIPLY,      u8("kMultiply")       ),
        new key_name_C(K_KENTER,         u8("kEnter")          ),
        new key_name_C(K_KPOINT,         u8("kPoint")          ),

        new key_name_C(K_K0,             u8("k0")              ),
        new key_name_C(K_K1,             u8("k1")              ),
        new key_name_C(K_K2,             u8("k2")              ),
        new key_name_C(K_K3,             u8("k3")              ),
        new key_name_C(K_K4,             u8("k4")              ),
        new key_name_C(K_K5,             u8("k5")              ),
        new key_name_C(K_K6,             u8("k6")              ),
        new key_name_C(K_K7,             u8("k7")              ),
        new key_name_C(K_K8,             u8("k8")              ),
        new key_name_C(K_K9,             u8("k9")              ),

        new key_name_C('<',              u8("lt")              ),

        new key_name_C(K_MOUSE,          u8("Mouse")           ),
        new key_name_C(K_LEFTMOUSE,      u8("LeftMouse")       ),
        new key_name_C(K_LEFTMOUSE_NM,   u8("LeftMouseNM")     ),
        new key_name_C(K_LEFTDRAG,       u8("LeftDrag")        ),
        new key_name_C(K_LEFTRELEASE,    u8("LeftRelease")     ),
        new key_name_C(K_LEFTRELEASE_NM, u8("LeftReleaseNM")   ),
        new key_name_C(K_MIDDLEMOUSE,    u8("MiddleMouse")     ),
        new key_name_C(K_MIDDLEDRAG,     u8("MiddleDrag")      ),
        new key_name_C(K_MIDDLERELEASE,  u8("MiddleRelease")   ),
        new key_name_C(K_RIGHTMOUSE,     u8("RightMouse")      ),
        new key_name_C(K_RIGHTDRAG,      u8("RightDrag")       ),
        new key_name_C(K_RIGHTRELEASE,   u8("RightRelease")    ),
        new key_name_C(K_MOUSEDOWN,      u8("ScrollWheelUp")   ),
        new key_name_C(K_MOUSEUP,        u8("ScrollWheelDown") ),
        new key_name_C(K_MOUSELEFT,      u8("ScrollWheelRight")),
        new key_name_C(K_MOUSERIGHT,     u8("ScrollWheelLeft") ),
        new key_name_C(K_MOUSEDOWN,      u8("MouseDown")       ),  /* OBSOLETE: use          */
        new key_name_C(K_MOUSEUP,        u8("MouseUp")         ),  /* ScrollWheelXXX instead */
        new key_name_C(K_X1MOUSE,        u8("X1Mouse")         ),
        new key_name_C(K_X1DRAG,         u8("X1Drag")          ),
        new key_name_C(K_X1RELEASE,      u8("X1Release")       ),
        new key_name_C(K_X2MOUSE,        u8("X2Mouse")         ),
        new key_name_C(K_X2DRAG,         u8("X2Drag")          ),
        new key_name_C(K_X2RELEASE,      u8("X2Release")       ),
        new key_name_C(K_DROP,           u8("Drop")            ),
        new key_name_C(K_ZERO,           u8("Nul")             ),
        new key_name_C(K_SNR,            u8("SNR")             ),
        new key_name_C(K_PLUG,           u8("Plug")            ),
        new key_name_C(K_CURSORHOLD,     u8("CursorHold")      ),
    };

    /*private*/ static final class mousetable_C
    {
        int     pseudo_code;        /* Code for pseudo mouse event. */
        int     button;             /* Which mouse button is it? */
        boolean is_click;           /* Is it a mouse button click event? */
        boolean is_drag;            /* Is it a mouse drag event? */

        /*private*/ mousetable_C(int pseudo_code, int button, boolean is_click, boolean is_drag)
        {
            this.pseudo_code = pseudo_code;
            this.button = button;
            this.is_click = is_click;
            this.is_drag = is_drag;
        }
    }

    /*private*/ static mousetable_C[] mouse_table = new mousetable_C[]
    {
        new mousetable_C(KE_LEFTMOUSE,     MOUSE_LEFT,    true,  false),
        new mousetable_C(KE_LEFTDRAG,      MOUSE_LEFT,    false, true ),
        new mousetable_C(KE_LEFTRELEASE,   MOUSE_LEFT,    false, false),
        new mousetable_C(KE_MIDDLEMOUSE,   MOUSE_MIDDLE,  true,  false),
        new mousetable_C(KE_MIDDLEDRAG,    MOUSE_MIDDLE,  false, true ),
        new mousetable_C(KE_MIDDLERELEASE, MOUSE_MIDDLE,  false, false),
        new mousetable_C(KE_RIGHTMOUSE,    MOUSE_RIGHT,   true,  false),
        new mousetable_C(KE_RIGHTDRAG,     MOUSE_RIGHT,   false, true ),
        new mousetable_C(KE_RIGHTRELEASE,  MOUSE_RIGHT,   false, false),
        new mousetable_C(KE_X1MOUSE,       MOUSE_X1,      true,  false),
        new mousetable_C(KE_X1DRAG,        MOUSE_X1,      false, true ),
        new mousetable_C(KE_X1RELEASE,     MOUSE_X1,      false, false),
        new mousetable_C(KE_X2MOUSE,       MOUSE_X2,      true,  false),
        new mousetable_C(KE_X2DRAG,        MOUSE_X2,      false, true ),
        new mousetable_C(KE_X2RELEASE,     MOUSE_X2,      false, false),
        /* DRAG without CLICK */
        new mousetable_C(KE_IGNORE,        MOUSE_RELEASE, false, true ),
        /* RELEASE without CLICK */
        new mousetable_C(KE_IGNORE,        MOUSE_RELEASE, false, false),
    };

    /*
     * Return the modifier mask bit (MOD_MASK_*) which corresponds to the given
     * modifier name ('S' for Shift, 'C' for Ctrl etc).
     */
    /*private*/ static int name_to_mod_mask(int c)
    {
        c = asc_toupper(c);

        for (int i = 0; i < mod_mask_table.length; i++)
            if (mod_mask_table[i].name == c)
                return mod_mask_table[i].mod_flag;

        return 0;
    }

    /*
     * Check if if there is a special key code for "key" that includes the modifiers specified.
     */
    /*private*/ static int simplify_key(int key, int[] modifiers)
    {
        if ((modifiers[0] & (MOD_MASK_SHIFT | MOD_MASK_CTRL | MOD_MASK_ALT)) != 0)
        {
            /* TAB is a special case. */
            if (key == TAB && (modifiers[0] & MOD_MASK_SHIFT) != 0)
            {
                modifiers[0] &= ~MOD_MASK_SHIFT;
                return K_S_TAB;
            }

            byte[] mkt = modifier_keys_table;
            byte key0 = KEY2TERMCAP0(key), key1 = KEY2TERMCAP1(key);

            for (int i = 0; mkt[i] != NUL; i += MOD_KEYS_ENTRY_SIZE)
                if (key0 == mkt[i + 3] && key1 == mkt[i + 4] && (modifiers[0] & char_u(mkt[i])) != 0)
                {
                    modifiers[0] &= ~char_u(mkt[i]);
                    return TERMCAP2KEY(mkt[i + 1], mkt[i + 2]);
                }
        }

        return key;
    }

    /*
     * Change <xHome> to <Home>, <xUp> to <Up>, etc.
     */
    /*private*/ static int handle_x_keys(int key)
    {
        switch (key)
        {
            case K_XUP:     return K_UP;
            case K_XDOWN:   return K_DOWN;
            case K_XLEFT:   return K_LEFT;
            case K_XRIGHT:  return K_RIGHT;
            case K_XHOME:   return K_HOME;
            case K_ZHOME:   return K_HOME;
            case K_XEND:    return K_END;
            case K_ZEND:    return K_END;
            case K_XF1:     return K_F1;
            case K_XF2:     return K_F2;
            case K_XF3:     return K_F3;
            case K_XF4:     return K_F4;
            case K_S_XF1:   return K_S_F1;
            case K_S_XF2:   return K_S_F2;
            case K_S_XF3:   return K_S_F3;
            case K_S_XF4:   return K_S_F4;
        }
        return key;
    }

    /*private*/ static Bytes key__name = new Bytes(MAX_KEY_NAME_LEN + 1);

    /*
     * Return a string which contains the name of the given key when the given modifiers are down.
     */
    /*private*/ static Bytes get_special_key_name(int c, int modifiers)
    {
        key__name.be(0, (byte)'<');
        int idx = 1;

        /* Key that stands for a normal character. */
        if (is_special(c) && KEY2TERMCAP0(c) == KS_KEY)
            c = char_u(KEY2TERMCAP1(c));

        /*
         * Translate shifted special keys into unshifted keys and set modifier.
         * Same for CTRL and ALT modifiers.
         */
        if (is_special(c))
        {
            byte[] mkt = modifier_keys_table;
            byte key0 = KEY2TERMCAP0(c), key1 = KEY2TERMCAP1(c);

            for (int i = 0; mkt[i] != NUL; i += MOD_KEYS_ENTRY_SIZE)
                if (key0 == mkt[i + 1] && key1 == mkt[i + 2])
                {
                    modifiers |= char_u(mkt[i]);
                    c = TERMCAP2KEY(mkt[i + 3], mkt[i + 4]);
                    break;
                }
        }

        /* try to find the key in the special key table */
        int table_idx = find_special_key_in_table(c);

        /*
         * When not a known special key, and not a printable character, try to extract modifiers.
         */
        if (0 < c && utf_char2len(c) == 1)
        {
            if (table_idx < 0 && (!vim_isprintc(c) || (c & 0x7f) == ' ') && (c & 0x80) != 0)
            {
                c &= 0x7f;
                modifiers |= MOD_MASK_ALT;
                /* try again, to find the un-alted key in the special key table */
                table_idx = find_special_key_in_table(c);
            }
            if (table_idx < 0 && !vim_isprintc(c) && c < ' ')
            {
                c += '@';
                modifiers |= MOD_MASK_CTRL;
            }
        }

        /* translate the modifier into a string */
        for (int i = 0; i < mod_mask_table.length && mod_mask_table[i].name != 'A'; i++)
            if ((modifiers & mod_mask_table[i].mod_mask) == mod_mask_table[i].mod_flag)
            {
                key__name.be(idx++, mod_mask_table[i].name);
                key__name.be(idx++, (byte)'-');
            }

        if (table_idx < 0)          /* unknown special key, may output t_xx */
        {
            if (is_special(c))
            {
                key__name.be(idx++, (byte)'t');
                key__name.be(idx++, (byte)'_');
                key__name.be(idx++, KEY2TERMCAP0(c));
                key__name.be(idx++, KEY2TERMCAP1(c));
            }
            /* Not a special key, only modifiers, output directly. */
            else
            {
                if (1 < utf_char2len(c))
                    idx += utf_char2bytes(c, key__name.plus(idx));
                else if (vim_isprintc(c))
                    key__name.be(idx++, c);
                else
                {
                    Bytes s = transchar(c);
                    while (s.at(0) != NUL)
                        key__name.be(idx++, (s = s.plus(1)).at(-1));
                }
            }
        }
        else                        /* use name of special key */
        {
            STRCPY(key__name.plus(idx), key_names_table[table_idx].name);
            idx = strlen(key__name);
        }
        key__name.be(idx++, (byte)'>');
        key__name.be(idx, NUL);
        return key__name;
    }

    /*
     * Try translating a <> name at (*srcp)[] to dst[].
     * Return the number of characters added to dst[], zero for no match.
     * If there is a match, srcp is advanced to after the <> name.
     * dst[] must be big enough to hold the result (up to six characters)!
     */
    /*private*/ static int trans_special(Bytes[] srcp, Bytes dst, boolean keycode)
        /* keycode: prefer key code, e.g. K_DEL instead of DEL */
    {
        int dlen = 0;

        int[] modifiers = { 0 };
        int key = find_special_key(srcp, modifiers, keycode, false);
        if (key == 0)
            return 0;

        /* Put the appropriate modifier in a string. */
        if (modifiers[0] != 0)
        {
            dst.be(dlen++, KB_SPECIAL);
            dst.be(dlen++, KS_MODIFIER);
            dst.be(dlen++, modifiers[0]);
        }

        if (is_special(key))
        {
            dst.be(dlen++, KB_SPECIAL);
            dst.be(dlen++, KEY2TERMCAP0(key));
            dst.be(dlen++, KEY2TERMCAP1(key));
        }
        else if (!keycode)
            dlen += utf_char2bytes(key, dst.plus(dlen));
        else if (keycode)
            dlen = BDIFF(add_char2buf(key, dst.plus(dlen)), dst);
        else
            dst.be(dlen++, key);

        return dlen;
    }

    /*
     * Try translating a <> name at (*srcp)[], return the key and modifiers.
     * srcp is advanced to after the <> name.
     * returns 0 if there is no match.
     */
    /*private*/ static int find_special_key(Bytes[] srcp, int[] modp, boolean keycode, boolean keep_x_key)
        /* keycode: prefer key code, e.g. K_DEL instead of DEL */
        /* keep_x_key: don't translate xHome to Home key */
    {
        Bytes src = srcp[0];
        if (src.at(0) != (byte)'<')
            return 0;

        /* Find end of modifier list. */
        Bytes last_dash = src;
        Bytes bp;
        for (bp = src.plus(1); bp.at(0) == (byte)'-' || vim_isIDc(bp.at(0)); bp = bp.plus(1))
        {
            if (bp.at(0) == (byte)'-')
            {
                last_dash = bp;
                if (bp.at(1) != NUL)
                {
                    int l = us_ptr2len_cc(bp.plus(1));
                    if (bp.at(l + 1) == (byte)'>')
                        bp = bp.plus(l);    /* anything accepted, like <C-?> */
                }
            }
            if (bp.at(0) == (byte)'t' && bp.at(1) == (byte)'_' && bp.at(2) != NUL && bp.at(3) != NUL)
                bp = bp.plus(3);    /* skip t_xx, xx may be '-' or '>' */
            else if (STRNCASECMP(bp, u8("char-"), 5) == 0)
            {
                int[] l = new int[1];
                vim_str2nr(bp.plus(5), null, l, TRUE, TRUE, null);
                bp = bp.plus(l[0] + 5);
                break;
            }
        }

        if (bp.at(0) == (byte)'>')     /* found matching '>' */
        {
            Bytes end_of_name = bp.plus(1);

            /* Which modifiers are given? */
            int[] modifiers = { 0 };
            for (bp = src.plus(1); BLT(bp, last_dash); bp = bp.plus(1))
            {
                if (bp.at(0) != (byte)'-')
                {
                    int bit = name_to_mod_mask(bp.at(0));
                    if (bit == 0x0)
                        break;      /* Illegal modifier name */
                    modifiers[0] |= bit;
                }
            }

            /*
             * Legal modifier name.
             */
            if (BLE(last_dash, bp))
            {
                int key;
                if (STRNCASECMP(last_dash.plus(1), u8("char-"), 5) == 0 && asc_isdigit(last_dash.at(6)))
                {
                    /* <Char-123> or <Char-033> or <Char-0x33> */
                    long[] n = new long[1];
                    vim_str2nr(last_dash.plus(6), null, null, TRUE, TRUE, n);
                    final long roof = 0x7fffffffL;
                    if (n[0] < 0 || roof < n[0])
                        n[0] = 0;
                    key = (int)n[0];
                }
                else
                {
                    /*
                     * Modifier with single letter, or special key name.
                     */
                    int l = us_ptr2len_cc(last_dash.plus(1));
                    if (modifiers[0] != 0 && last_dash.at(l + 1) == (byte)'>')
                        key = us_ptr2char(last_dash.plus(1));
                    else
                    {
                        key = get_special_key_code(last_dash.plus(1));
                        if (!keep_x_key)
                            key = handle_x_keys(key);
                    }
                }

                /*
                 * get_special_key_code() may return NUL for invalid special key name.
                 */
                if (key != NUL)
                {
                    /*
                     * Only use a modifier when there is no special key code that
                     * includes the modifier.
                     */
                    key = simplify_key(key, modifiers);

                    if (!keycode)
                    {
                        /* don't want keycode, use single byte code */
                        if (key == K_BS)
                            key = BS;
                        else if (key == K_DEL || key == K_KDEL)
                            key = DEL;
                    }

                    /*
                     * Normal Key with modifier: Try to make a single byte code.
                     */
                    if (!is_special(key))
                        key = extract_modifiers(key, modifiers);

                    modp[0] = modifiers[0];
                    srcp[0] = end_of_name;
                    return key;
                }
            }
        }

        return 0;
    }

    /*
     * Try to include modifiers in the key.
     * Changes "Shift-a" to 'A', "Alt-A" to 0xc0, etc.
     */
    /*private*/ static int extract_modifiers(int key, int[] modp)
    {
        int modifiers = modp[0];

        if ((modifiers & MOD_MASK_SHIFT) != 0 && asc_isalpha(key))
        {
            key = asc_toupper(key);
            modifiers &= ~MOD_MASK_SHIFT;
        }
        if ((modifiers & MOD_MASK_CTRL) != 0 && (('?' <= key && key <= '_') || asc_isalpha(key)))
        {
            key = ctrl_key((byte)key);
            modifiers &= ~MOD_MASK_CTRL;
            /* <C-@> is <Nul> */
            if (key == 0)
                key = K_ZERO;
        }
        if ((modifiers & MOD_MASK_ALT) != 0 && key < 0x80)  /* avoid creating a lead byte */
        {
            key |= 0x80;
            modifiers &= ~MOD_MASK_ALT;                     /* remove the META modifier */
        }

        modp[0] = modifiers;
        return key;
    }

    /*
     * Try to find key "c" in the special key table.
     * Return the index when found, -1 when not found.
     */
    /*private*/ static int find_special_key_in_table(int c)
    {
        for (int i = 0; i < key_names_table.length; i++)
            if (key_names_table[i].key == c)
                return i;

        return -1;
    }

    /*
     * Find the special key with the given name (the given string does not have to
     * end with NUL, the name is assumed to end before the first non-idchar).
     * If the name starts with "t_" the next two characters are interpreted as a termcap name.
     * Return the key code, or 0 if not found.
     */
    /*private*/ static int get_special_key_code(Bytes name)
    {
        /*
         * If it's <t_xx> we get the code for xx from the termcap
         */
        if (name.at(0) == (byte)'t' && name.at(1) == (byte)'_' && name.at(2) != NUL && name.at(3) != NUL)
        {
            Bytes string = new Bytes(3);

            string.be(0, name.at(2));
            string.be(1, name.at(3));
            string.be(2, NUL);

            if (add_termcap_entry(string, false) == true)
                return TERMCAP2KEY(name.at(2), name.at(3));
        }
        else
            for (int i = 0; i < key_names_table.length; i++)
            {
                Bytes key_name = key_names_table[i].name;
                int j;
                for (j = 0; vim_isIDc(name.at(j)) && key_name.at(j) != NUL; j++)
                    if (asc_tolower(key_name.at(j)) != asc_tolower(name.at(j)))
                        break;
                if (!vim_isIDc(name.at(j)) && key_name.at(j) == NUL)
                    return key_names_table[i].key;
            }

        return 0;
    }

    /*private*/ static Bytes get_key_name(int i)
    {
        if (i < key_names_table.length)
            return key_names_table[i].name;

        return null;
    }

    /*
     * Look up the given mouse code to return the relevant information in the other arguments.
     * Return which button is down or was released.
     */
    /*private*/ static int get_mouse_button(int code, boolean[] is_click, boolean[] is_drag)
    {
        is_click[0] = is_drag[0] = false;

        for (int i = 0; i < mouse_table.length; i++)
            if (code == mouse_table[i].pseudo_code)
            {
                is_click[0] = mouse_table[i].is_click;
                is_drag[0] = mouse_table[i].is_drag;

                return mouse_table[i].button;
            }

        return 0;       /* Shouldn't get here. */
    }

    /*
     * Return the appropriate pseudo mouse event token (KE_LEFTMOUSE, etc.)
     * based on which mouse button is down, and whether it was clicked, dragged or released.
     */
    /*private*/ static int get_pseudo_mouse_code(int button, boolean is_click, boolean is_drag)
        /* button: e.g. MOUSE_LEFT */
    {
        for (int i = 0; i < mouse_table.length; i++)
            if (mouse_table[i].button == button
                    && mouse_table[i].is_click == is_click && mouse_table[i].is_drag == is_drag)
                return mouse_table[i].pseudo_code;

        return KE_IGNORE;          /* not recognized, ignore it */
    }

    /*
     * Return the current end-of-line type: EOL_DOS, EOL_UNIX or EOL_MAC.
     */
    /*private*/ static int get_fileformat(buffer_C buf)
    {
        byte c = buf.b_p_ff[0].at(0);

        if (buf.b_p_bin[0] || c == 'u')
            return EOL_UNIX;
        if (c == 'm')
            return EOL_MAC;

        return EOL_DOS;
    }

    /*
     * Like get_fileformat(), but override 'fileformat' with "p" for "++opt=val" argument.
     */
    /*private*/ static int get_fileformat_force(buffer_C buf, exarg_C eap)
        /* eap: can be null! */
    {
        if ((eap != null && eap.force_bin != 0) ? (eap.force_bin == FORCE_BIN) : buf.b_p_bin[0])
            return EOL_UNIX;

        byte c = buf.b_p_ff[0].at(0);
        if (c == 'u')
            return EOL_UNIX;
        if (c == 'm')
            return EOL_MAC;

        return EOL_DOS;
    }

    /*
     * Set the current end-of-line type to EOL_DOS, EOL_UNIX or EOL_MAC.
     * Sets both 'textmode' and 'fileformat'.
     * Note: Does _not_ set global value of 'textmode'!
     */
    /*private*/ static void set_fileformat(int t, int opt_flags)
        /* opt_flags: OPT_LOCAL and/or OPT_GLOBAL */
    {
        Bytes p = null;

        switch (t)
        {
            case EOL_DOS:
                p = FF_DOS;
                curbuf.b_p_tx[0] = true;
                break;
            case EOL_UNIX:
                p = FF_UNIX;
                curbuf.b_p_tx[0] = false;
                break;
            case EOL_MAC:
                p = FF_MAC;
                curbuf.b_p_tx[0] = false;
                break;
        }
        if (p != null)
            set_string_option_direct(u8("ff"), -1, p, OPT_FREE | opt_flags, 0);

        /* This may cause the buffer to become (un)modified. */
        check_status(curbuf);
        redraw_tabline = true;
    }

    /*
     * Return the default fileformat from 'fileformats'.
     */
    /*private*/ static int default_fileformat()
    {
        switch (p_ffs[0].at(0))
        {
            case 'm':   return EOL_MAC;
            case 'd':   return EOL_DOS;
        }
        return EOL_UNIX;
    }

    /*
     * Well, no shell.
     */
    /*private*/ static int call_shell(Bytes _cmd, int _opt)
    {
        return 127;
    }

    /*
     * VISUAL, SELECTMODE and OP_PENDING State are never set, they are equal to
     * NORMAL State with a condition.  This function returns the real State.
     */
    /*private*/ static int get_real_state()
    {
        if ((State & NORMAL) != 0)
        {
            if (VIsual_active)
            {
                if (VIsual_select)
                    return SELECTMODE;

                return VISUAL;
            }
            else if (finish_op)
                return OP_PENDING;
        }
        return State;
    }

    /*
     * Return true if "p" points to just after a path separator.
     * Takes care of multi-byte characters.
     * "b" must point to the start of the file name
     */
    /*private*/ static boolean after_pathsep(Bytes b, Bytes p)
    {
        return (BLT(b, p) && vim_ispathsep(p.at(-1)) && us_head_off(b, p.minus(1)) == 0);
    }

    /*
     * Sort an array of strings.
     */
    /*private*/ static final Comparator<Bytes> sort__compare = new Comparator<Bytes>()
    {
        public int compare(Bytes s1, Bytes s2)
        {
            return STRCMP(s1, s2);
        }
    };

    /*private*/ static void sort_strings(Bytes[] files, int count)
    {
        Arrays.sort(files, 0, count, sort__compare);
    }

    /*
     * Print an error message with one or two "%s" and one or two string arguments.
     * This is not in message.c to avoid a warning for prototypes.
     */
    /*private*/ static boolean emsg3(Bytes s, Bytes a1, Bytes a2)
    {
        if (emsg_not_now())
            return true;            /* no error messages at the moment */

        vim_snprintf(ioBuff, IOSIZE, s, a1, a2);
        return emsg(ioBuff);
    }

    /*
     * Print an error message with one "%ld" and one long int argument.
     * This is not in message.c to avoid a warning for prototypes.
     */
    /*private*/ static boolean emsgn(Bytes s, long n)
    {
        if (emsg_not_now())
            return true;            /* no error messages at the moment */

        vim_snprintf(ioBuff, IOSIZE, s, n);
        return emsg(ioBuff);
    }

    /*
     * Read 2 bytes from "fd" and turn them into an int, MSB first.
     */
    /*private*/ static int get2c(file_C fd)
    {
        int        n = libc.getc(fd);
        n = (n << 8) + libc.getc(fd);
        return n;
    }

    /*
     * Read 4 bytes from "fd" and turn them into an int, MSB first.
     */
    /*private*/ static int get4c(file_C fd)
    {
        /* Use long rather than int otherwise result is undefined when left-shift sets the MSB. */
        long       n = libc.getc(fd);
        n = (n << 8) + libc.getc(fd);
        n = (n << 8) + libc.getc(fd);
        n = (n << 8) + libc.getc(fd);
        return (int)n;
    }

    /*
     * Read 8 bytes from "fd" and turn them into a time_t, MSB first.
     */
    /*private*/ static long get8c(file_C fd)
    {
        long n = 0;
        for (int i = 0; i < 8; i++)
            n = (n << 8) + libc.getc(fd);
        return n;
    }
}
