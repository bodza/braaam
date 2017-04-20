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
public class VimL
{
    /*
     * getchar.c --------------------------------------------------------------------------------------
     *
     * functions related with getting a character from the user/mapping/redo/...
     *
     * manipulations with redo buffer and stuff buffer
     * mappings and abbreviations
     */

    /*
     * These buffers are used for storing:
     * - stuffed characters: A command that is translated into another command.
     * - redo characters: will redo the last change.
     * - recorded characters: for the "q" command.
     *
     * The bytes are stored like in the typeahead buffer:
     * - KB_SPECIAL introduces a special key (two more bytes follow).
     *   A literal KB_SPECIAL is stored as KB_SPECIAL KS_SPECIAL KE_FILLER.
     * - CSI introduces a GUI termcap code (also when gui.in_use is false,
     *   otherwise switching the GUI on would make mappings invalid).
     *   A literal CSI is stored as CSI KS_EXTRA KE_CSI.
     * These translations are also done on multi-byte characters!
     *
     * Escaping CSI bytes is done by the system-specific input functions, called by ui_inchar().
     * Escaping KB_SPECIAL is done by inchar().
     * Un-escaping is done by vgetc().
     */

    /*private*/ static buffheader_C redobuff = new buffheader_C();
    /*private*/ static buffheader_C old_redobuff = new buffheader_C();
    /*private*/ static buffheader_C save_redobuff = new buffheader_C();
    /*private*/ static buffheader_C save_old_redobuff = new buffheader_C();
    /*private*/ static buffheader_C recordbuff = new buffheader_C();

    /*private*/ static int typeahead_char;      /* typeahead char that's not flushed */

    /*
     * When block_redo is true redo buffer will not be changed;
     * used by edit() to repeat insertions and 'V' command for redoing.
     */
    /*private*/ static boolean block_redo;

    /*
     * Make a hash value for a mapping.
     * "mode" is the lower 4 bits of the State for the mapping.
     * "c1" is the first character of the "lhs".
     * Returns a value between 0 and 255, index in maphash.
     * Put Normal/Visual mode mappings mostly separately from Insert/Cmdline mode.
     */
    /*private*/ static int map_hash(int mode, int c1)
    {
        return ((mode & (NORMAL + VISUAL + SELECTMODE + OP_PENDING)) != 0) ? c1 : (c1 ^ 0x80);
    }

    /*
     * Each mapping is put in one of the 256 hash lists, to speed up finding it.
     */
    /*private*/ static mapblock_C[][] maphash = new mapblock_C[256][1];

    /*
     * List used for abbreviations.
     */
    /*private*/ static mapblock_C[] first_abbr = new mapblock_C[1];   /* first entry in abbrlist */

    /*private*/ static int keyNoremap;          /* remapping flags */

    /*
     * Variables used by vgetorpeek() and flush_buffers().
     *
     * typebuf.tb_buf[] contains all characters that are not consumed yet.
     * typebuf.tb_buf[typebuf.tb_off] is the first valid character.
     * typebuf.tb_buf[typebuf.tb_off + typebuf.tb_len - 1] is the last valid char.
     * typebuf.tb_buf[typebuf.tb_off + typebuf.tb_len] must be NUL.
     *
     * The head of the buffer may contain the result of mappings, abbreviations
     * and @a commands.  The length of this part is typebuf.tb_maplen.
     * typebuf.tb_silent is the part where <silent> applies.
     * After the head are characters that come from the terminal.
     * typebuf.tb_no_abbr_cnt is the number of characters in typebuf.tb_buf that
     * should not be considered for abbreviations.
     * Some parts of typebuf.tb_buf may not be mapped.  These parts are remembered
     * in typebuf.tb_noremap[], which is the same length as typebuf.tb_buf and
     * contains RM_NONE for the characters that are not to be remapped.
     * typebuf.tb_noremap[typebuf.tb_off] is the first valid flag.
     */
    /*private*/ static final int RM_YES          = 0;       /* tb_noremap: remap */
    /*private*/ static final int RM_NONE         = 1;       /* tb_noremap: don't remap */
    /*private*/ static final int RM_SCRIPT       = 2;       /* tb_noremap: remap local script mappings */
    /*private*/ static final int RM_ABBR         = 4;       /* tb_noremap: don't remap, do abbrev. */

    /* typebuf.tb_buf has three parts:
     *  room in front (for result of mappings),
     *  the middle for typeahead and
     *  room for new characters (which needs to be 3 * MAXMAPLEN).
     */
    /*private*/ static final int TYPELEN_INIT    = 5 * (MAXMAPLEN + 3);
    /*private*/ static Bytes    typebuf_init = new Bytes(TYPELEN_INIT);      /* initial typebuf.tb_buf */
    /*private*/ static Bytes    noremapbuf_init = new Bytes(TYPELEN_INIT);   /* initial typebuf.tb_noremap */

    /*private*/ static int      last_recorded_len;          /* number of last recorded chars */

    /*
     * Free and clear a buffer.
     */
    /*private*/ static void free_buff(buffheader_C buf)
    {
        buf.bh_first.bb_next = null;
    }

    /*
     * Return the contents of a buffer as a single string.
     * KB_SPECIAL and CSI in the returned string are escaped.
     */
    /*private*/ static Bytes get_buffcont(buffheader_C buffer, boolean dozero)
        /* dozero: count == zero is not an error */
    {
        int count = 0;

        /* compute the total length of the string */
        for (buffblock_C bp = buffer.bh_first.bb_next; bp != null; bp = bp.bb_next)
            count += strlen(bp.bb_str);

        Bytes p = null;

        if (0 < count || dozero)
        {
            p = new Bytes(count + 1);

            Bytes q = p;
            for (buffblock_C bp = buffer.bh_first.bb_next; bp != null; bp = bp.bb_next)
                for (Bytes s = bp.bb_str; s.at(0) != NUL; )
                    (q = q.plus(1)).be(-1, (s = s.plus(1)).at(-1));
            q.be(0, NUL);
        }

        return p;
    }

    /*
     * Return the contents of the record buffer as a single string and clear the record buffer.
     * KB_SPECIAL and CSI in the returned string are escaped.
     */
    /*private*/ static Bytes get_recorded()
    {
        Bytes p = get_buffcont(recordbuff, true);
        free_buff(recordbuff);

        /*
         * Remove the characters that were added the last time, these must be the
         * (possibly mapped) characters that stopped the recording.
         */
        int len = strlen(p);
        if (last_recorded_len <= len)
        {
            len -= last_recorded_len;
            p.be(len, NUL);
        }

        /*
         * When stopping recording from Insert mode with CTRL-O q, also remove the CTRL-O.
         */
        if (0 < len && restart_edit != 0 && p.at(len - 1) == Ctrl_O)
            p.be(len - 1, NUL);

        return p;
    }

    /*
     * Return the contents of the redo buffer as a single string.
     * KB_SPECIAL and CSI in the returned string are escaped.
     */
    /*private*/ static Bytes get_inserted()
    {
        return get_buffcont(redobuff, false);
    }

    /*
     * Add string "s" after the current block of buffer "buf".
     * KB_SPECIAL and CSI should have been escaped already.
     */
    /*private*/ static void add_buff(buffheader_C buf, Bytes s, long slen)
        /* slen: length of "s" or -1 */
    {
        if (slen < 0)
            slen = strlen(s);
        if (slen == 0)                          /* don't add empty strings */
            return;

        if (buf.bh_first.bb_next == null)       /* first add to list */
        {
            buf.bh_space = 0;
            buf.bh_curr = buf.bh_first;
        }
        else if (buf.bh_curr == null)           /* buffer has already been read */
        {
            emsg(u8("E222: Add to read buffer"));
            return;
        }
        else if (buf.bh_index != 0)
        {
            buffblock_C bp = buf.bh_first.bb_next;
            int len = strlen(bp.bb_str.plus(buf.bh_index)) + 1;
            BCOPY(bp.bb_str, 0, bp.bb_str, buf.bh_index, len);
        }
        buf.bh_index = 0;

        if ((int)slen <= buf.bh_space)
        {
            int len = strlen(buf.bh_curr.bb_str);
            vim_strncpy(buf.bh_curr.bb_str.plus(len), s, (int)slen);
            buf.bh_space -= slen;
        }
        else
        {
            final int MINIMAL_SIZE = 20;            /* minimal size for bb_str */

            int len = ((int)slen < MINIMAL_SIZE) ? MINIMAL_SIZE : (int)slen;

            buffblock_C bp = new buffblock_C();

            bp.bb_str = new Bytes(len + 1);
            vim_strncpy(bp.bb_str, s, (int)slen);
            buf.bh_space = len - (int)slen;

            bp.bb_next = buf.bh_curr.bb_next;
            buf.bh_curr.bb_next = bp;
            buf.bh_curr = bp;
        }
    }

    /*
     * Add number "n" to buffer "buf".
     */
    /*private*/ static void add_num_buff(buffheader_C buf, long n)
    {
        Bytes number = new Bytes(32);

        libC.sprintf(number, u8("%ld"), n);
        add_buff(buf, number, -1L);
    }

    /*
     * Add character 'c' to buffer "buf".
     * Translates special keys, NUL, CSI, KB_SPECIAL and multibyte characters.
     */
    /*private*/ static void add_char_buff(buffheader_C buf, int c)
    {
        Bytes bytes = new Bytes(MB_MAXBYTES + 1);

        int len = is_special(c) ? 1 : utf_char2bytes(c, bytes);

        Bytes temp = new Bytes(4);

        for (int i = 0; i < len; i++)
        {
            if (!is_special(c))
                c = char_u(bytes.at(i));

            if (is_special(c) || c == char_u(KB_SPECIAL) || c == NUL)
            {
                /* translate special key code into three byte sequence */
                temp.be(0, KB_SPECIAL);
                temp.be(1, KB_SECOND(c));
                temp.be(2, KB_THIRD(c));
                temp.be(3, NUL);
            }
            else
            {
                temp.be(0, c);
                temp.be(1, NUL);
            }

            add_buff(buf, temp, -1L);
        }
    }

    /* First read ahead buffer.  Used for translated commands. */
    /*private*/ static buffheader_C readbuf1 = new buffheader_C();

    /* Second read ahead buffer.  Used for redo. */
    /*private*/ static buffheader_C readbuf2 = new buffheader_C();

    /*
     * Get one byte from a read buffer.
     * If advance == true go to the next char.
     * No translation is done KB_SPECIAL and CSI are escaped.
     */
    /*private*/ static byte read_readbuf(buffheader_C buf, boolean advance)
    {
        buffblock_C bp = buf.bh_first.bb_next;
        if (bp == null) /* buffer is empty */
            return NUL;

        byte b = bp.bb_str.at(buf.bh_index);

        if (advance && bp.bb_str.at(++buf.bh_index) == NUL)
        {
            buf.bh_first.bb_next = bp.bb_next;
            buf.bh_index = 0;
        }

        return b;
    }

    /*
     * Get one byte from the read buffers.
     * Use readbuf1 one first, use readbuf2 if that one is empty.
     * If advance == true go to the next char.
     * No translation is done KB_SPECIAL and CSI are escaped.
     */
    /*private*/ static byte read_readbuffers(boolean advance)
    {
        byte b = read_readbuf(readbuf1, advance);
        if (b == NUL)
            b = read_readbuf(readbuf2, advance);
        return b;
    }

    /*
     * Prepare the read buffers for reading (if they contain something).
     */
    /*private*/ static void start_stuff()
    {
        if (readbuf1.bh_first.bb_next != null)
        {
            readbuf1.bh_curr = readbuf1.bh_first;
            readbuf1.bh_space = 0;
        }
        if (readbuf2.bh_first.bb_next != null)
        {
            readbuf2.bh_curr = readbuf2.bh_first;
            readbuf2.bh_space = 0;
        }
    }

    /*
     * Return true if the stuff buffer is empty.
     */
    /*private*/ static boolean stuff_empty()
    {
        return (readbuf1.bh_first.bb_next == null && readbuf2.bh_first.bb_next == null);
    }

    /*
     * Return true if readbuf1 is empty.  There may still be redo characters in readbuf2.
     */
    /*private*/ static boolean readbuf1_empty()
    {
        return (readbuf1.bh_first.bb_next == null);
    }

    /*
     * Set a typeahead character that won't be flushed.
     */
    /*private*/ static void typeahead_noflush(int c)
    {
        typeahead_char = c;
    }

    /*
     * Remove the contents of the stuff buffer and the mapped characters
     * in the typeahead buffer (used in case of an error).
     * If "flush_typeahead" is true, flush all typeahead characters
     * (used when interrupted by a CTRL-C).
     */
    /*private*/ static void flush_buffers(boolean flush_typeahead)
    {
        init_typebuf();

        start_stuff();
        while (read_readbuffers(true) != NUL)
            ;

        if (flush_typeahead)            /* remove all typeahead */
        {
            /*
             * We have to get all characters, because we may delete the first part of an escape sequence.
             * In an xterm we get one char at a time and we have to get them all.
             */
            while (inchar(typebuf.tb_buf, typebuf.tb_buflen - 1, 10L, typebuf.tb_change_cnt) != 0)
                ;
            typebuf.tb_off = MAXMAPLEN;
            typebuf.tb_len = 0;
        }
        else                    /* remove mapped characters at the start only */
        {
            typebuf.tb_off += typebuf.tb_maplen;
            typebuf.tb_len -= typebuf.tb_maplen;
        }
        typebuf.tb_maplen = 0;
        typebuf.tb_silent = 0;
        cmd_silent = false;
        typebuf.tb_no_abbr_cnt = 0;
    }

    /*
     * The previous contents of the redo buffer is kept in old_redobuffer.
     * This is used for the CTRL-O <.> command in insert mode.
     */
    /*private*/ static void resetRedobuff()
    {
        if (!block_redo)
        {
            free_buff(old_redobuff);
            COPY_buffheader(old_redobuff, redobuff);
            redobuff.bh_first.bb_next = null;
        }
    }

    /*
     * Discard the contents of the redo buffer and restore the previous redo buffer.
     */
    /*private*/ static void cancelRedo()
    {
        if (!block_redo)
        {
            free_buff(redobuff);
            COPY_buffheader(redobuff, old_redobuff);
            old_redobuff.bh_first.bb_next = null;
            start_stuff();
            while (read_readbuffers(true) != NUL)
                ;
        }
    }

    /*
     * Save redobuff and old_redobuff to save_redobuff and save_old_redobuff.
     * Used before executing autocommands and user functions.
     */
    /*private*/ static int save__level;

    /*private*/ static void saveRedobuff()
    {
        if (save__level++ == 0)
        {
            COPY_buffheader(save_redobuff, redobuff);
            redobuff.bh_first.bb_next = null;
            COPY_buffheader(save_old_redobuff, old_redobuff);
            old_redobuff.bh_first.bb_next = null;

            /* Make a copy, so that ":normal ." in a function works. */
            Bytes s = get_buffcont(save_redobuff, false);
            if (s != null)
                add_buff(redobuff, s, -1L);
        }
    }

    /*
     * Restore redobuff and old_redobuff from save_redobuff and save_old_redobuff.
     * Used after executing autocommands and user functions.
     */
    /*private*/ static void restoreRedobuff()
    {
        if (--save__level == 0)
        {
            free_buff(redobuff);
            COPY_buffheader(redobuff, save_redobuff);
            free_buff(old_redobuff);
            COPY_buffheader(old_redobuff, save_old_redobuff);
        }
    }

    /*
     * Append "s" to the redo buffer.
     * KB_SPECIAL and CSI should already have been escaped.
     */
    /*private*/ static void appendToRedobuff(Bytes s)
    {
        if (!block_redo)
            add_buff(redobuff, s, -1L);
    }

    /*
     * Append to Redo buffer literally, escaping special characters with CTRL-V.
     * KB_SPECIAL and CSI are escaped as well.
     */
    /*private*/ static void appendToRedobuffLit(Bytes str, int len)
        /* len: length of "str" or -1 for up to the NUL */
    {
        if (block_redo)
            return;

        for (Bytes[] s = { str }; (len < 0) ? (s[0].at(0) != NUL) : (BDIFF(s[0], str) < len); )
        {
            /* Put a string of normal characters in the redo buffer (that's faster). */
            Bytes start = s[0];
            while (' ' <= s[0].at(0) && s[0].at(0) < DEL && (len < 0 || BDIFF(s[0], str) < len))
                s[0] = s[0].plus(1);

            /* Don't put '0' or '^' as last character, just in case a CTRL-D is typed next. */
            if (s[0].at(0) == NUL && (s[0].at(-1) == (byte)'0' || s[0].at(-1) == (byte)'^'))
                s[0] = s[0].minus(1);
            if (BLT(start, s[0]))
                add_buff(redobuff, start, BDIFF(s[0], start));

            if (s[0].at(0) == NUL || (0 <= len && len <= BDIFF(s[0], str)))
                break;

            /* Handle a special or multibyte character.
             * Handle composing chars separately. */
            int c = us_ptr2char_adv(s, false);
            if (c < ' ' || c == DEL || (s[0].at(0) == NUL && (c == '0' || c == '^')))
                add_char_buff(redobuff, Ctrl_V);

            /* CTRL-V '0' must be inserted as CTRL-V 048 */
            if (s[0].at(0) == NUL && c == '0')
                add_buff(redobuff, u8("048"), 3L);
            else
                add_char_buff(redobuff, c);
        }
    }

    /*
     * Append a character to the redo buffer.
     * Translates special keys, NUL, CSI, KB_SPECIAL and multibyte characters.
     */
    /*private*/ static void appendCharToRedobuff(int c)
    {
        if (!block_redo)
            add_char_buff(redobuff, c);
    }

    /*
     * Append a number to the redo buffer.
     */
    /*private*/ static void appendNumberToRedobuff(long n)
    {
        if (!block_redo)
            add_num_buff(redobuff, n);
    }

    /*
     * Append string "s" to the stuff buffer.
     * CSI and KB_SPECIAL must already have been escaped.
     */
    /*private*/ static void stuffReadbuff(Bytes s)
    {
        add_buff(readbuf1, s, -1L);
    }

    /*
     * Append string "s" to the redo stuff buffer.
     * CSI and KB_SPECIAL must already have been escaped.
     */
    /*private*/ static void stuffRedoReadbuff(Bytes s)
    {
        add_buff(readbuf2, s, -1L);
    }

    /*private*/ static void stuffReadbuffLen(Bytes s, long len)
    {
        add_buff(readbuf1, s, len);
    }

    /*
     * Stuff "s" into the stuff buffer,
     * leaving special key codes unmodified and escaping other KB_SPECIAL and CSI bytes.
     * Change CR, LF and ESC into a space.
     */
    /*private*/ static void stuffReadbuffSpec(Bytes _s)
    {
        for (Bytes[] s = { _s }; s[0].at(0) != NUL; )
        {
            if (s[0].at(0) == KB_SPECIAL && s[0].at(1) != NUL && s[0].at(2) != NUL)
            {
                /* Insert special key literally. */
                stuffReadbuffLen(s[0], 3L);
                s[0] = s[0].plus(3);
            }
            else
            {
                int c = us_ptr2char_adv(s, true);
                if (c == CAR || c == NL || c == ESC)
                    c = ' ';
                stuffcharReadbuff(c);
            }
        }
    }

    /*
     * Append a character to the stuff buffer.
     * Translates special keys, NUL, CSI, KB_SPECIAL and multibyte characters.
     */
    /*private*/ static void stuffcharReadbuff(int c)
    {
        add_char_buff(readbuf1, c);
    }

    /*
     * Append a number to the stuff buffer.
     */
    /*private*/ static void stuffnumReadbuff(long n)
    {
        add_num_buff(readbuf1, n);
    }

    /*private*/ static buffblock_C redo_bp;
    /*private*/ static Bytes redo_sp;

    /*
     * Prepare for redo; return false if nothing to redo, true otherwise.
     * If old_redo is true, use old_redobuff instead of redobuff.
     */
    /*private*/ static boolean init_redo(boolean old_redo)
    {
        if (old_redo)
            redo_bp = old_redobuff.bh_first.bb_next;
        else
            redo_bp = redobuff.bh_first.bb_next;
        if (redo_bp == null)
            return false;

        redo_sp = redo_bp.bb_str;
        return true;
    }

    /*
     * Read a character from the redo buffer.
     * Translates KB_SPECIAL, CSI and multibyte characters.
     * The redo buffer is left as it is.
     */
    /*private*/ static int read_redo()
    {
        if (redo_sp.at(0) != NUL)
        {
            /* For a multi-byte character get all the bytes and return the converted character. */
            int n;
            if (redo_sp.at(0) != KB_SPECIAL || redo_sp.at(1) == KS_SPECIAL)
                n = mb_byte2len(char_u(redo_sp.at(0)));
            else
                n = 1;

            Bytes buf = new Bytes(MB_MAXBYTES + 1);

            for (int i = 0; ; i++)
            {
                int c;
                if (redo_sp.at(0) == KB_SPECIAL)   /* special key or escaped KB_SPECIAL */
                {
                    c = toSpecial(redo_sp.at(1), redo_sp.at(2));
                    redo_sp = redo_sp.plus(3);
                }
                else
                {
                    c = char_u(redo_sp.at(0));
                    redo_sp = redo_sp.plus(1);
                }

                if (redo_sp.at(0) == NUL && redo_bp.bb_next != null)
                {
                    redo_bp = redo_bp.bb_next;
                    redo_sp = redo_bp.bb_str;
                }

                buf.be(i, c);
                if (i == n - 1)                 /* last byte of a character */
                {
                    if (n != 1)
                        c = us_ptr2char(buf);
                    return c;
                }

                if (redo_sp.at(0) == NUL)          /* cannot happen? */
                    break;
            }
        }

        return NUL;
    }

    /*
     * Stuff the redo buffer into readbuf2.
     * Insert the redo count into the command.
     * If "old_redo" is true, the last but one command is repeated
     * instead of the last command (inserting text).
     * This is used for CTRL-O <.> in insert mode.
     *
     * Return false for failure, true otherwise.
     */
    /*private*/ static boolean start_redo(long count, boolean old_redo)
    {
        /* init the pointers; return if nothing to redo */
        if (!init_redo(old_redo))
            return false;

        int c = read_redo();

        /* copy the buffer name, if present */
        if (c == '"')
        {
            add_buff(readbuf2, u8("\""), 1L);
            c = read_redo();

            /* if a numbered buffer is used, increment the number */
            if ('1' <= c && c < '9')
                c++;
            add_char_buff(readbuf2, c);
            c = read_redo();
        }

        if (c == 'v')   /* redo Visual */
        {
            COPY_pos(VIsual, curwin.w_cursor);
            VIsual_active = true;
            VIsual_select = false;
            VIsual_reselect = true;
            redo_VIsual_busy = true;
            c = read_redo();
        }

        /* try to enter the count (in place of a previous count) */
        if (count != 0)
        {
            while (asc_isdigit(c))  /* skip "old" count */
                c = read_redo();
            add_num_buff(readbuf2, count);
        }

        /* copy from the redo buffer into the stuff buffer */
        add_char_buff(readbuf2, c);
        while ((c = read_redo()) != NUL)
            add_char_buff(readbuf2, c);

        return true;
    }

    /*
     * Repeat the last insert (R, o, O, a, A, i or I command) by stuffing the redo buffer into readbuf2.
     * Return false for failure, true otherwise.
     */
    /*private*/ static boolean start_redo_ins()
    {
        if (!init_redo(false))
            return false;

        start_stuff();

        /* skip the count and the command character */
        for (int c; (c = read_redo()) != NUL; )
        {
            if (vim_strchr(u8("AaIiRrOo"), c) != null)
            {
                if (c == 'O' || c == 'o')
                    add_buff(readbuf2, NL_STR, -1L);
                break;
            }
        }

        /* copy the typed text from the redo buffer into the stuff buffer */
        for (int c; (c = read_redo()) != NUL; )
            add_char_buff(readbuf2, c);

        block_redo = true;
        return true;
    }

    /*private*/ static void stop_redo_ins()
    {
        block_redo = false;
    }

    /*
     * Initialize typebuf.tb_buf to point to typebuf_init.
     * calloc() cannot be used here: In out-of-memory situations it would
     * be impossible to type anything.
     */
    /*private*/ static void init_typebuf()
    {
        if (typebuf.tb_buf == null)
        {
            typebuf.tb_buf = typebuf_init;
            typebuf.tb_noremap = noremapbuf_init;
            typebuf.tb_buflen = TYPELEN_INIT;
            typebuf.tb_len = 0;
            typebuf.tb_off = 0;
            typebuf.tb_change_cnt = 1;
        }
    }

    /*
     * Insert a string in position 'offset' in the typeahead buffer
     * (for "@r" and ":normal" command, vgetorpeek() and check_termcode()).
     *
     * If noremap is REMAP_YES, new string can be mapped again.
     * If noremap is REMAP_NONE, new string cannot be mapped again.
     * If noremap is REMAP_SKIP, fist char of new string cannot be mapped again,
     * but abbreviations are allowed.
     * If noremap is REMAP_SCRIPT, new string cannot be mapped again,
     * except for script-local mappings.
     * If noremap is > 0, that many characters of the new string cannot be mapped.
     *
     * If nottyped is true, the string does not return keyTyped
     * (don't use when offset is non-zero!).
     *
     * If silent is true, cmd_silent is set when the characters are obtained.
     *
     * Return false for failure, true otherwise.
     */
    /*private*/ static boolean ins_typebuf(Bytes str, int noremap, int offset, boolean nottyped, boolean silent)
    {
        init_typebuf();
        if (++typebuf.tb_change_cnt == 0)
            typebuf.tb_change_cnt = 1;

        int addlen = strlen(str);

        /*
         * Easy case: there is room in front of typebuf.tb_buf[typebuf.tb_off]
         */
        if (offset == 0 && addlen <= typebuf.tb_off)
        {
            typebuf.tb_off -= addlen;
            BCOPY(typebuf.tb_buf, typebuf.tb_off, str, 0, addlen);
        }
        /*
         * Need to allocate a new buffer.
         * In typebuf.tb_buf there must always be room for 3 * MAXMAPLEN + 4 characters.
         * We add some extra room to avoid having to allocate too often.
         */
        else
        {
            int newoff = MAXMAPLEN + 4;
            int newlen = typebuf.tb_len + addlen + newoff + 4 * (MAXMAPLEN + 4);
            if (newlen < 0)                 /* string is getting too long */
            {
                emsg(e_toocompl);           /* also calls flush_buffers */
                setcursor();
                return false;
            }
            Bytes s1 = new Bytes(newlen);
            Bytes s2 = new Bytes(newlen);
            typebuf.tb_buflen = newlen;

            /* copy the old chars, before the insertion point */
            BCOPY(s1, newoff, typebuf.tb_buf, typebuf.tb_off, offset);
            /* copy the new chars */
            BCOPY(s1, newoff + offset, str, 0, addlen);
            /* copy the old chars, after the insertion point, including the NUL at the end */
            BCOPY(s1, newoff + offset + addlen, typebuf.tb_buf, typebuf.tb_off + offset, typebuf.tb_len - offset + 1);
            typebuf.tb_buf = s1;

            BCOPY(s2, newoff, typebuf.tb_noremap, typebuf.tb_off, offset);
            BCOPY(s2, newoff + offset + addlen, typebuf.tb_noremap, typebuf.tb_off + offset, typebuf.tb_len - offset);
            typebuf.tb_noremap = s2;

            typebuf.tb_off = newoff;
        }
        typebuf.tb_len += addlen;

        /* If noremap == REMAP_SCRIPT: do remap script-local mappings. */
        int val;
        if (noremap == REMAP_SCRIPT)
            val = RM_SCRIPT;
        else if (noremap == REMAP_SKIP)
            val = RM_ABBR;
        else
            val = RM_NONE;

        /*
         * Adjust typebuf.tb_noremap[] for the new characters:
         * If noremap == REMAP_NONE or REMAP_SCRIPT: new characters are (sometimes) not remappable.
         * If noremap == REMAP_YES: all the new characters are mappable.
         * If noremap  > 0: "noremap" characters are not remappable, the rest mappable.
         */
        int nrm;
        if (noremap == REMAP_SKIP)
            nrm = 1;
        else if (noremap < 0)
            nrm = addlen;
        else
            nrm = noremap;
        for (int i = 0; i < addlen; i++)
            typebuf.tb_noremap.be(typebuf.tb_off + i + offset, (0 <= --nrm) ? val : RM_YES);

        /* 'tb_maplen' and 'tb_silent' only remember the length of mapped and/or silent mappings at the
         * start of the buffer, assuming that a mapped sequence doesn't result in typed characters. */
        if (nottyped || offset < typebuf.tb_maplen)
            typebuf.tb_maplen += addlen;
        if (silent || offset < typebuf.tb_silent)
        {
            typebuf.tb_silent += addlen;
            cmd_silent = true;
        }
        if (typebuf.tb_no_abbr_cnt != 0 && offset == 0) /* and not used for abbrev.s */
            typebuf.tb_no_abbr_cnt += addlen;

        return true;
    }

    /*
     * Put character "c" back into the typeahead buffer.
     * Can be used for a character obtained by vgetc() that needs to be put back.
     * Uses cmd_silent, keyTyped and keyNoremap to restore the flags belonging to the char.
     */
    /*private*/ static void ins_char_typebuf(int c)
    {
        Bytes buf = new Bytes(MB_MAXBYTES + 1);
        if (is_special(c))
        {
            buf.be(0, KB_SPECIAL);
            buf.be(1, KB_SECOND(c));
            buf.be(2, KB_THIRD(c));
            buf.be(3, NUL);
        }
        else
        {
            buf.be(utf_char2bytes(c, buf), NUL);
        }
        ins_typebuf(buf, keyNoremap, 0, !keyTyped, cmd_silent);
    }

    /*
     * Return true if the typeahead buffer was changed (while waiting for a character to arrive).
     * Happens when a message was received from a client or from feedkeys().
     * But check in a more generic way to avoid trouble: When "typebuf.tb_buf"
     * changed it was reallocated and the old pointer can no longer be used.
     * Or "typebuf.tb_off" may have been changed and we would overwrite characters that was just added.
     */
    /*private*/ static boolean typebuf_changed(int tb_change_cnt)
        /* tb_change_cnt: old value of typebuf.tb_change_cnt */
    {
        return (tb_change_cnt != 0 && (typebuf.tb_change_cnt != tb_change_cnt || typebuf_was_filled));
    }

    /*
     * Return true if there are no characters in the typeahead buffer that have
     * not been typed (result from a mapping or come from ":normal").
     */
    /*private*/ static boolean typebuf_typed()
    {
        return (typebuf.tb_maplen == 0);
    }

    /*
     * Return the number of characters that are mapped (or not typed).
     */
    /*private*/ static int typebuf_maplen()
    {
        return typebuf.tb_maplen;
    }

    /*
     * remove "len" characters from typebuf.tb_buf[typebuf.tb_off + offset]
     */
    /*private*/ static void del_typebuf(int len, int offset)
    {
        if (len == 0)
            return;         /* nothing to do */

        typebuf.tb_len -= len;

        /*
         * Easy case: Just increase typebuf.tb_off.
         */
        if (offset == 0 && 3 * MAXMAPLEN + 3 <= typebuf.tb_buflen - (typebuf.tb_off + len))
            typebuf.tb_off += len;
        /*
         * Have to move the characters in typebuf.tb_buf[] and typebuf.tb_noremap[]
         */
        else
        {
            int i = typebuf.tb_off + offset;
            /*
             * Leave some extra room at the end to avoid reallocation.
             */
            if (MAXMAPLEN < typebuf.tb_off)
            {
                BCOPY(typebuf.tb_buf, MAXMAPLEN, typebuf.tb_buf, typebuf.tb_off, offset);
                BCOPY(typebuf.tb_noremap, MAXMAPLEN, typebuf.tb_noremap, typebuf.tb_off, offset);
                typebuf.tb_off = MAXMAPLEN;
            }
            /* adjust typebuf.tb_buf (include the NUL at the end) */
            BCOPY(typebuf.tb_buf, typebuf.tb_off + offset, typebuf.tb_buf, i + len, typebuf.tb_len - offset + 1);
            /* adjust typebuf.tb_noremap[] */
            BCOPY(typebuf.tb_noremap, typebuf.tb_off + offset, typebuf.tb_noremap, i + len, typebuf.tb_len - offset);
        }

        if (offset < typebuf.tb_maplen)             /* adjust tb_maplen */
        {
            if (typebuf.tb_maplen < offset + len)
                typebuf.tb_maplen = offset;
            else
                typebuf.tb_maplen -= len;
        }
        if (offset < typebuf.tb_silent)             /* adjust tb_silent */
        {
            if (typebuf.tb_silent < offset + len)
                typebuf.tb_silent = offset;
            else
                typebuf.tb_silent -= len;
        }
        if (offset < typebuf.tb_no_abbr_cnt)        /* adjust tb_no_abbr_cnt */
        {
            if (typebuf.tb_no_abbr_cnt < offset + len)
                typebuf.tb_no_abbr_cnt = offset;
            else
                typebuf.tb_no_abbr_cnt -= len;
        }

        /* Reset the flag that text received from a client or from feedkeys()
         * was inserted in the typeahead buffer. */
        typebuf_was_filled = false;
        if (++typebuf.tb_change_cnt == 0)
            typebuf.tb_change_cnt = 1;
    }

    /*
     * Write typed characters to script file.
     * If recording is on put the character in the recordbuffer.
     */
    /*private*/ static void gotchars(Bytes chars, int len)
    {
        Bytes buf = new Bytes(2);

        /* Remember how many chars were last recorded. */
        if (Recording)
            last_recorded_len += len;

        for (int todo = len; 0 < todo--; )
        {
            /* Handle one byte at a time; no translation to be done. */
            byte c = (chars = chars.plus(1)).at(-1);
            updatescript(c);

            if (Recording)
            {
                buf.be(0, c);
                add_buff(recordbuff, buf, 1L);
            }
        }
        may_sync_undo();

        /* Output "debug mode" message next time in debug mode. */
        debug_did_msg = false;

        /* Since characters have been typed, consider the following to be in another mapping.
         * Search string will be kept in history. */
        maptick++;
    }

    /*
     * Sync undo.  Called when typed characters are obtained from the typeahead
     * buffer, or when a menu is used.
     * Do not sync:
     * - In Insert mode, unless cursor key has been used.
     * - While reading a script file.
     * - When no_u_sync is non-zero.
     */
    /*private*/ static void may_sync_undo()
    {
        if (((State & (INSERT + CMDLINE)) == 0 || arrow_used) && scriptin[curscript] == null)
            u_sync(false);
    }

    /*
     * Make "typebuf" empty and allocate new buffers.
     */
    /*private*/ static void alloc_typebuf()
    {
        typebuf.tb_buf = new Bytes(TYPELEN_INIT);
        typebuf.tb_noremap = new Bytes(TYPELEN_INIT);
        typebuf.tb_buflen = TYPELEN_INIT;
        typebuf.tb_off = 0;
        typebuf.tb_len = 0;
        typebuf.tb_maplen = 0;
        typebuf.tb_silent = 0;
        typebuf.tb_no_abbr_cnt = 0;
        if (++typebuf.tb_change_cnt == 0)
            typebuf.tb_change_cnt = 1;
    }

    /*
     * Free the buffers of "typebuf".
     */
    /*private*/ static void free_typebuf()
    {
        if (BNE(typebuf.tb_buf, typebuf_init))
            typebuf.tb_buf = null;
        if (BNE(typebuf.tb_noremap, noremapbuf_init))
            typebuf.tb_noremap = null;
    }

    /*
     * When doing ":so! file", the current typeahead needs to be saved,
     * and restored when "file" has been read completely.
     */
    /*private*/ static typebuf_C[] saved_typebuf = ARRAY_typebuf(NSCRIPT);

    /*private*/ static void save_typebuf()
    {
        init_typebuf();
        COPY_typebuf(saved_typebuf[curscript], typebuf);
        alloc_typebuf();
    }

    /*private*/ static int old_char = -1;       /* character put back by vungetc() */
    /*private*/ static int old_mod_mask;        /* mod_mask for ungotten character */
    /*private*/ static int old_mouse_row;       /* mouse_row related to old_char */
    /*private*/ static int old_mouse_col;       /* mouse_col related to old_char */

    /*
     * Save all three kinds of typeahead, so that the user must type at a prompt.
     */
    /*private*/ static void save_typeahead(tasave_C tp)
    {
        alloc_typebuf();

        tp.old_char = old_char;
        tp.old_mod_mask = old_mod_mask;
        old_char = -1;

        COPY_buffheader(tp.save_readbuf1, readbuf1);
        readbuf1.bh_first.bb_next = null;
        COPY_buffheader(tp.save_readbuf2, readbuf2);
        readbuf2.bh_first.bb_next = null;

        save_input_buf(tp);
    }

    /*
     * Restore the typeahead to what it was before calling save_typeahead().
     * The allocated memory is freed, can only be called once!
     */
    /*private*/ static void restore_typeahead(tasave_C tp)
    {
        free_typebuf();

        old_char = tp.old_char;
        old_mod_mask = tp.old_mod_mask;

        free_buff(readbuf1);
        COPY_buffheader(readbuf1, tp.save_readbuf1);
        free_buff(readbuf2);
        COPY_buffheader(readbuf2, tp.save_readbuf2);

        restore_input_buf(tp);
    }

    /*
     * Open a new script file for the ":source!" command.
     */
    /*private*/ static void openscript(Bytes name, boolean directly)
        /* directly: when true execute directly */
    {
        if (curscript + 1 == NSCRIPT)
        {
            emsg(e_nesting);
            return;
        }

        if (ignore_script)
            /* Not reading from script, also don't open one.  Warning message? */
            return;

        if (scriptin[curscript] != null)        /* already reading script */
            curscript++;

        if ((scriptin[curscript] = libC.fopen(name, u8("r"))) == null)
        {
            emsg2(e_notopen, name);
            if (0 < curscript)
                --curscript;
            return;
        }

        save_typebuf();

        /*
         * Execute the commands from the file right now when using ":source!"
         * after ":global" or ":argdo" or in a loop.  Also when another command
         * follows.  This means the display won't be updated.  Don't do this
         * always, "make test" would fail.
         */
        if (directly)
        {
            int save_State = State;
            int save_restart_edit = restart_edit;
            boolean save_insertmode = p_im[0];
            boolean save_finish_op = finish_op;
            boolean save_msg_scroll = msg_scroll;

            State = NORMAL;
            msg_scroll = false;                         /* no msg scrolling in Normal mode */
            restart_edit = 0;                           /* don't go to Insert mode */
            p_im[0] = false;                               /* don't use 'insertmode' */
            oparg_C oa = new oparg_C();
            finish_op = false;

            int oldcurscript = curscript;
            do
            {
                update_topline_cursor();                /* update cursor position and topline */
                normal_cmd(oa, false);                  /* execute one command */
                vpeekc();                               /* check for end of file */
            } while (scriptin[oldcurscript] != null);

            State = save_State;
            msg_scroll = save_msg_scroll;
            restart_edit = save_restart_edit;
            p_im[0] = save_insertmode;
            finish_op = save_finish_op;
        }
    }

    /*
     * Close the currently active input script.
     */
    /*private*/ static void closescript()
    {
        free_typebuf();
        COPY_typebuf(typebuf, saved_typebuf[curscript]);

        libc.fclose(scriptin[curscript]);
        scriptin[curscript] = null;
        if (0 < curscript)
            --curscript;
    }

    /*
     * This function is called just before doing a blocking wait.
     * Thus after waiting 'updatetime' for a character to arrive.
     */
    /*private*/ static void before_blocking()
    {
        updatescript(NUL);
    }

    /*
     * updatescipt() is called when a character can be written into the script file
     * or when we have waited some time for a character (c == 0)
     */
    /*private*/ static void updatescript(byte c)
    {
        if (c != NUL && scriptout != null)
            libc.putc(c, scriptout);
    }

    /*
     * Get the next input character.
     * Can return a special key or a multi-byte character.
     * Can return NUL when called recursively, use safe_vgetc() if that's not wanted.
     * This translates escaped KB_SPECIAL and CSI bytes to a KB_SPECIAL or CSI byte.
     * Collects the bytes of a multibyte character into the whole character.
     * Returns the modifiers in the global "mod_mask".
     */
    /*private*/ static int vgetc()
    {
        int c;

        /*
         * If a character was put back with vungetc, it was already processed.
         * Return it directly.
         */
        if (old_char != -1)
        {
            c = old_char;
            old_char = -1;
            mod_mask = old_mod_mask;
            mouse_row = old_mouse_row;
            mouse_col = old_mouse_col;
        }
        else
        {
            Bytes buf = new Bytes(MB_MAXBYTES + 1);

            mod_mask = 0;
            last_recorded_len = 0;

            for ( ; ; )                     /* this is done twice if there are modifiers */
            {
                if (mod_mask != 0)          /* no mapping after modifier has been read */
                {
                    no_mapping++;
                    allow_keys++;
                }
                c = vgetorpeek(true);
                if (mod_mask != 0)
                {
                    --no_mapping;
                    --allow_keys;
                }

                /* Get two extra bytes for special keys. */
                if (c == char_u(KB_SPECIAL))
                {
                    int save_allow_keys = allow_keys;

                    no_mapping++;
                    allow_keys = 0;                 /* make sure BS is not found */
                    int c2 = vgetorpeek(true);      /* no mapping for these chars */
                    c = vgetorpeek(true);
                    --no_mapping;
                    allow_keys = save_allow_keys;

                    if (c2 == char_u(KS_MODIFIER))
                    {
                        mod_mask = c;
                        continue;
                    }
                    c = toSpecial((byte)c2, (byte)c);
                }

                /* a keypad or special function key was not mapped, use it like its ASCII equivalent */
                switch (c)
                {
                    case K_KPLUS:       c = '+'; break;
                    case K_KMINUS:      c = '-'; break;
                    case K_KDIVIDE:     c = '/'; break;
                    case K_KMULTIPLY:   c = '*'; break;
                    case K_KENTER:      c = CAR; break;
                    case K_KPOINT:      c = '.'; break;
                    case K_K0:          c = '0'; break;
                    case K_K1:          c = '1'; break;
                    case K_K2:          c = '2'; break;
                    case K_K3:          c = '3'; break;
                    case K_K4:          c = '4'; break;
                    case K_K5:          c = '5'; break;
                    case K_K6:          c = '6'; break;
                    case K_K7:          c = '7'; break;
                    case K_K8:          c = '8'; break;
                    case K_K9:          c = '9'; break;

                    case K_XHOME:
                    case K_ZHOME:       if (mod_mask == MOD_MASK_SHIFT)
                                        {
                                            c = K_S_HOME;
                                            mod_mask = 0;
                                        }
                                        else if (mod_mask == MOD_MASK_CTRL)
                                        {
                                            c = K_C_HOME;
                                            mod_mask = 0;
                                        }
                                        else
                                            c = K_HOME;
                                        break;
                    case K_XEND:
                    case K_ZEND:        if (mod_mask == MOD_MASK_SHIFT)
                                        {
                                            c = K_S_END;
                                            mod_mask = 0;
                                        }
                                        else if (mod_mask == MOD_MASK_CTRL)
                                        {
                                            c = K_C_END;
                                            mod_mask = 0;
                                        }
                                        else
                                            c = K_END;
                                        break;

                    case K_XUP:         c = K_UP; break;
                    case K_XDOWN:       c = K_DOWN; break;
                    case K_XLEFT:       c = K_LEFT; break;
                    case K_XRIGHT:      c = K_RIGHT; break;
                }

                /* For a multi-byte character get all the bytes and return the converted character.
                 * Note: This will loop until enough bytes are received!
                 */
                int n = mb_byte2len(c);
                if (1 < n)
                {
                    no_mapping++;
                    buf.be(0, c);
                    for (int i = 1; i < n; i++)
                    {
                        buf.be(i, vgetorpeek(true));
                        if (buf.at(i) == KB_SPECIAL)
                        {
                            /* Must be a KB_SPECIAL - KS_SPECIAL - KE_FILLER sequence,
                             * which represents a KB_SPECIAL (0x80),
                             * or a CSI - KS_EXTRA - KE_CSI sequence, which represents a CSI (0x9B),
                             * of a KB_SPECIAL - KS_EXTRA - KE_CSI, which is CSI too. */
                            c = vgetorpeek(true);
                            if (vgetorpeek(true) == KE_CSI && c == char_u(KS_EXTRA))
                                buf.be(i, CSI);
                        }
                    }
                    --no_mapping;
                    c = us_ptr2char(buf);
                }

                break;
            }
        }

        return c;
    }

    /*
     * Like vgetc(), but never return a NUL when called recursively,
     * get a key directly from the user (ignoring typeahead).
     */
    /*private*/ static int safe_vgetc()
    {
        int c = vgetc();
        if (c == NUL)
            c = get_keystroke();
        return c;
    }

    /*
     * Like safe_vgetc(), but loop to handle K_IGNORE.
     * Also ignore scrollbar events.
     */
    /*private*/ static int plain_vgetc()
    {
        int c;

        do
        {
            c = safe_vgetc();
        } while (c == K_IGNORE || c == K_VER_SCROLLBAR || c == K_HOR_SCROLLBAR);

        return c;
    }

    /*
     * Check if a character is available, such that vgetc() will not block.
     * If the next character is a special character or multi-byte, the returned character is not valid!.
     */
    /*private*/ static int vpeekc()
    {
        if (old_char != -1)
            return old_char;

        return vgetorpeek(false);
    }

    /*
     * Like vpeekc(), but don't allow mapping.  Do allow checking for terminal codes.
     */
    /*private*/ static int vpeekc_nomap()
    {
        int c;

        no_mapping++;
        allow_keys++;
        c = vpeekc();
        --no_mapping;
        --allow_keys;

        return c;
    }

    /*
     * Check if any character is available, also half an escape sequence.
     * Trick: when no typeahead found, but there is something in the typeahead
     * buffer, it must be an ESC that is recognized as the start of a key code.
     */
    /*private*/ static int vpeekc_any()
    {
        int c = vpeekc();
        if (c == NUL && 0 < typebuf.tb_len)
            c = ESC;
        return c;
    }

    /*
     * Call vpeekc() without causing anything to be mapped.
     * Return true if a character is available, false otherwise.
     */
    /*private*/ static boolean char_avail()
    {
        int retval;

        no_mapping++;
        retval = vpeekc();
        --no_mapping;

        return (retval != NUL);
    }

    /* unget one character (can only be done once!) */
    /*private*/ static void vungetc(int c)
    {
        old_char = c;
        old_mod_mask = mod_mask;
        old_mouse_row = mouse_row;
        old_mouse_col = mouse_col;
    }

    /*private*/ static int __tc;

    /*
     * get a character:
     * 1. from the stuffbuffer
     *      This is used for abbreviated commands like "D" -> "d$".
     *      Also used to redo a command for ".".
     * 2. from the typeahead buffer
     *      Stores text obtained previously but not used yet.
     *      Also stores the result of mappings.
     *      Also used for the ":normal" command.
     * 3. from the user
     *      This may do a blocking wait if "advance" is true.
     *
     * if "advance" is true (vgetc()):
     *      really get the character.
     *      keyTyped is set to true in the case the user typed the key.
     *      keyStuffed is true if the character comes from the stuff buffer.
     * if "advance" is false (vpeekc()):
     *      just look whether there is a character available.
     *
     * When "no_mapping" is zero, checks for mappings in the current mode.
     * Only returns one byte (of a multi-byte character).
     * KB_SPECIAL and CSI may be escaped, need to get two more bytes then.
     */
    /*private*/ static int vgetorpeek(boolean advance)
    {
        /*
         * This function doesn't work very well when called recursively.
         * It may happen though, because of:
         *
         * 1. The call to add_to_showcmd(). char_avail() is then used to check
         * if there is a character available, which calls this function.
         * In that case we must return NUL, to indicate no character is available.
         *
         * 2. A GUI callback function writes to the screen, causing a wait_return().
         * Using ":normal" can also do this, but it saves the typeahead buffer,
         * thus it should be OK.  But don't get a key from the user then.
         */
        if (0 < vgetc_busy && ex_normal_busy == 0)
            return NUL;

        int local_State = get_real_state();

        vgetc_busy++;

        if (advance)
            keyStuffed = false;

        init_typebuf();
        start_stuff();
        if (advance && typebuf.tb_maplen == 0)
            execReg = false;

        boolean timedout = false;       /* waited for more than 1 second for mapping to complete */
        int mapdepth = 0;               /* check for recursive mapping */
        boolean mode_deleted = false;   /* set when mode has been deleted */

        int c;

        do
        {
    /*
     * get a character: 1. from the stuffbuffer
     */
            if (typeahead_char != 0)
            {
                c = typeahead_char;
                if (advance)
                    typeahead_char = 0;
            }
            else
                c = char_u(read_readbuffers(advance));

            if (c != NUL && !got_int)
            {
                if (advance)
                {
                    /* keyTyped = false;
                     * When the command that stuffed something was typed,
                     * behave like the stuffed command was typed;
                     * needed e.g. for CTRL-W CTRl-] to open a fold. */
                    keyStuffed = true;
                }
                if (typebuf.tb_no_abbr_cnt == 0)
                    typebuf.tb_no_abbr_cnt = 1; /* no abbreviations now */
            }
            else
            {
                /*
                 * Loop until we either find a matching mapped key,
                 * or we are sure that it is not a mapped key.
                 * If a mapped key sequence is found, we go back to the start to try re-mapping.
                 */
                for ( ; ; )
                {
                    /*
                     * ui_breakcheck() is slow, don't use it too often when inside a mapping.
                     * But call it each time for typed characters.
                     */
                    if (typebuf.tb_maplen != 0)
                        line_breakcheck();
                    else
                        ui_breakcheck();            /* check for CTRL-C */

                    if (got_int)
                    {
                        /* flush all input */
                        int len = inchar(typebuf.tb_buf, typebuf.tb_buflen - 1, 0L, typebuf.tb_change_cnt);
                        /*
                         * If inchar() returns true (script file was active)
                         * or we are inside a mapping, get out of insert mode.
                         * Otherwise we behave like having gotten a CTRL-C.
                         * As a result typing CTRL-C in insert mode will really insert a CTRL-C.
                         */
                        if ((len != 0 || typebuf.tb_maplen != 0) && (State & (INSERT + CMDLINE)) != 0)
                            c = ESC;
                        else
                            c = Ctrl_C;

                        flush_buffers(true);        /* flush all typeahead */

                        if (advance)
                        {
                            /* Also record this character, it might be needed to get out of Insert mode. */
                            typebuf.tb_buf.be(0, c);
                            gotchars(typebuf.tb_buf, 1);
                        }
                        cmd_silent = false;

                        break;
                    }

                    int keylen = 0;

                    if (0 < typebuf.tb_len)
                    {
                        int mp_match_len = 0;

                        /*
                         * Check for a mappable key sequence.
                         * Walk through one maphash[] list until we find an entry that matches.
                         *
                         * Don't look for mappings if:
                         * - no_mapping set: mapping disabled (e.g. for CTRL-V)
                         * - typebuf.tb_buf[typebuf.tb_off] should not be remapped
                         * - in insert or cmdline mode and 'paste' option set
                         * - waiting for "hit return to continue" and CR or SPACE typed
                         * - waiting for a char with --more--
                         * - in Ctrl-X mode, and we get a valid char for that mode
                         */
                        mapblock_C mp = null;
                        int max_mlen = 0;
                        int c1 = char_u(typebuf.tb_buf.at(typebuf.tb_off));
                        if (no_mapping == 0
                                && (no_zero_mapping == 0 || c1 != '0')
                                && (typebuf.tb_maplen == 0
                                    || (p_remap[0]
                                        && (typebuf.tb_noremap.at(typebuf.tb_off) & (RM_NONE|RM_ABBR)) == 0))
                                && !(p_paste[0] && (State & (INSERT + CMDLINE)) != 0)
                                && !(State == HITRETURN && (c1 == CAR || c1 == ' '))
                                && State != ASKMORE
                                && State != CONFIRM)
                        {
                            /* First try buffer-local mappings. */
                            int h1 = map_hash(local_State, c1);
                            mp = curbuf.b_maphash[h1][0];
                            mapblock_C mp2 = maphash[h1][0];
                            if (mp == null)
                            {
                                /* There are no buffer-local mappings. */
                                mp = mp2;
                                mp2 = null;
                            }
                            /*
                             * Loop until a partly matching mapping is found or
                             * all (local) mappings have been checked.
                             * The longest full match is remembered in "mp_match".
                             * A full match is only accepted if there is no partly
                             * match, so "aa" and "aaa" can both be mapped.
                             */
                            mapblock_C mp_match = null;
                            boolean __;
                            for ( ; mp != null; mp = mp.m_next, __ = (mp == null && (mp = mp2) == mp2 && (mp2 = null) == null))
                            {
                                /*
                                 * Only consider an entry if the first character
                                 * matches and it is for the current state.
                                 * Skip ":lmap" mappings if keys were mapped.
                                 */
                                if (mp.m_keys.at(0) == c1
                                        && (mp.m_mode & local_State) != 0
                                        && ((mp.m_mode & LANGMAP) == 0 || typebuf.tb_maplen == 0))
                                {
                                    /* find the match length of this mapping */
                                    int mlen;
                                    for (mlen = 1; mlen < typebuf.tb_len; mlen++)
                                    {
                                        if (mp.m_keys.at(mlen) != typebuf.tb_buf.at(typebuf.tb_off + mlen))
                                            break;
                                    }

                                    /* Don't allow mapping the first byte(s) of a multi-byte char.
                                     * Happens when mapping <M-a> and then changing 'encoding'.
                                     * Beware that 0x80 is escaped. */
                                    {
                                        Bytes[] p1 = { mp.m_keys };
                                        Bytes p2 = mb_unescape(p1);

                                        if (p2 != null && us_ptr2len_cc(p2) < mb_byte2len(c1))
                                            mlen = 0;
                                    }
                                    /*
                                     * Check an entry whether it matches.
                                     * - full match: mlen == keylen
                                     * - partly match: mlen == typebuf.tb_len
                                     */
                                    keylen = mp.m_keylen;
                                    if (mlen == keylen || (mlen == typebuf.tb_len && typebuf.tb_len < keylen))
                                    {
                                        /*
                                         * If only script-local mappings are allowed,
                                         * check if the mapping starts with K_SNR.
                                         */
                                        Bytes s = typebuf.tb_noremap.plus(typebuf.tb_off);
                                        if (s.at(0) == RM_SCRIPT
                                                && (mp.m_keys.at(0) != KB_SPECIAL
                                                 || mp.m_keys.at(1) != KS_EXTRA
                                                 || mp.m_keys.at(2) != KE_SNR))
                                            continue;
                                        /*
                                         * If one of the typed keys cannot be remapped, skip the entry.
                                         */
                                        int n;
                                        for (n = mlen; 0 <= --n; )
                                            if (((s = s.plus(1)).at(-1) & (RM_NONE|RM_ABBR)) != 0)
                                                break;
                                        if (0 <= n)
                                            continue;

                                        if (typebuf.tb_len < keylen)
                                        {
                                            if (!timedout && !(mp_match != null && mp_match.m_nowait))
                                            {
                                                /* break at a partly match */
                                                keylen = KEYLEN_PART_MAP;
                                                break;
                                            }
                                        }
                                        else if (mp_match_len < keylen)
                                        {
                                            /* found a longer match */
                                            mp_match = mp;
                                            mp_match_len = keylen;
                                        }
                                    }
                                    else
                                        /* No match; may have to check for termcode at next character. */
                                        if (max_mlen < mlen)
                                            max_mlen = mlen;
                                }
                            }

                            /* If no partly match found, use the longest full match. */
                            if (keylen != KEYLEN_PART_MAP)
                            {
                                mp = mp_match;
                                keylen = mp_match_len;
                            }
                        }

                        /* Check for match with 'pastetoggle'. */
                        if (p_pt[0].at(0) != NUL && mp == null && (State & (INSERT|NORMAL)) != 0)
                        {
                            int mlen;
                            for (mlen = 0; mlen < typebuf.tb_len && p_pt[0].at(mlen) != NUL; mlen++)
                                if (p_pt[0].at(mlen) != typebuf.tb_buf.at(typebuf.tb_off + mlen))
                                    break;
                            if (p_pt[0].at(mlen) == NUL)          /* match */
                            {
                                /* write chars to script file(s) */
                                if (typebuf.tb_maplen < mlen)
                                    gotchars(typebuf.tb_buf.plus(typebuf.tb_off + typebuf.tb_maplen),
                                                                        mlen - typebuf.tb_maplen);

                                del_typebuf(mlen, 0);       /* remove the chars */
                                set_option_value(u8("paste"), !p_paste[0] ? TRUE : FALSE, null, 0);
                                if ((State & INSERT) == 0)
                                {
                                    msg_col = 0;
                                    msg_row = (int)Rows[0] - 1;
                                    msg_clr_eos();          /* clear ruler */
                                }
                                status_redraw_all();
                                redraw_statuslines();
                                showmode();
                                setcursor();
                                continue;
                            }
                            /* Need more chars for partly match. */
                            if (mlen == typebuf.tb_len)
                                keylen = KEYLEN_PART_KEY;
                            else if (max_mlen < mlen)
                                /* No match; may have to check for termcode at next character. */
                                max_mlen = mlen + 1;
                        }

                        if ((mp == null || mp_match_len <= max_mlen) && keylen != KEYLEN_PART_MAP)
                        {
                            int save_keylen = keylen;

                            /*
                             * When no matching mapping found or found a non-matching mapping
                             * that matches at least what the matching mapping matched:
                             * Check if we have a terminal code, when:
                             * - mapping is allowed,
                             * - keys have not been mapped,
                             * - and not an ESC sequence, not in insert mode or "p_ek" is on,
                             * - and when not timed out.
                             */
                            if ((no_mapping == 0 || allow_keys != 0)
                                    && (typebuf.tb_maplen == 0
                                        || (p_remap[0] && typebuf.tb_noremap.at(typebuf.tb_off) == RM_YES))
                                    && !timedout)
                            {
                                keylen = check_termcode(max_mlen + 1, null, 0, null);

                                /* If no termcode matched but 'pastetoggle' matched partially,
                                 * it's like an incomplete key sequence. */
                                if (keylen == 0 && save_keylen == KEYLEN_PART_KEY)
                                    keylen = KEYLEN_PART_KEY;

                                /*
                                 * When getting a partial match, but the last characters were not typed,
                                 * don't wait for a typed character to complete the termcode.
                                 * This helps a lot when a ":normal" command ends in an ESC.
                                 */
                                if (keylen < 0 && typebuf.tb_len == typebuf.tb_maplen)
                                    keylen = 0;
                            }
                            else
                                keylen = 0;

                            if (keylen == 0)        /* no matching terminal code */
                            {
                                /* When there was a matching mapping and no termcode could be
                                 * replaced after another one, use that mapping (loop around).
                                 * If there was no mapping use the character from the
                                 * typeahead buffer right here. */
                                if (mp == null)
                                {
    /*
     * get a character: 2. from the typeahead buffer
     */
                                    c = typebuf.tb_buf.at(typebuf.tb_off) & 0xff;
                                    if (advance)    /* remove chars from tb_buf */
                                    {
                                        cmd_silent = (0 < typebuf.tb_silent);
                                        if (0 < typebuf.tb_maplen)
                                            keyTyped = false;
                                        else
                                        {
                                            keyTyped = true;
                                            /* write char to script file(s) */
                                            gotchars(typebuf.tb_buf.plus(typebuf.tb_off), 1);
                                        }
                                        keyNoremap = typebuf.tb_noremap.at(typebuf.tb_off);
                                        del_typebuf(1, 0);
                                    }
                                    break;          /* got character, break for loop */
                                }
                            }
                            if (0 < keylen)         /* full matching terminal code */
                            {
                                continue;           /* try mapping again */
                            }

                            /* Partial match: get some more characters.
                             * When a matching mapping was found use that one. */
                            if (mp == null || keylen < 0)
                                keylen = KEYLEN_PART_KEY;
                            else
                                keylen = mp_match_len;
                        }

                        /* complete match */
                        if (0 <= keylen && keylen <= typebuf.tb_len)
                        {
                            /* write chars to script file(s) */
                            if (typebuf.tb_maplen < keylen)
                                gotchars(typebuf.tb_buf.plus(typebuf.tb_off + typebuf.tb_maplen),
                                                                    keylen - typebuf.tb_maplen);

                            cmd_silent = (0 < typebuf.tb_silent);
                            del_typebuf(keylen, 0);     /* remove the mapped keys */

                            /*
                             * Put the replacement string in front of mapstr.
                             * The depth check catches ":map x y" and ":map y x".
                             */
                            if (p_mmd[0] <= ++mapdepth)
                            {
                                emsg(u8("E223: recursive mapping"));
                                if ((State & CMDLINE) != 0)
                                    redrawcmdline();
                                else
                                    setcursor();
                                flush_buffers(false);
                                mapdepth = 0;           /* for next one */
                                c = -1;
                                break;
                            }

                            /*
                             * In Select mode and a Visual mode mapping is used:
                             * switch to Visual mode temporarily.
                             * Append K_SELECT to switch back to Select mode.
                             */
                            if (VIsual_active && VIsual_select && (mp.m_mode & VISUAL) != 0)
                            {
                                VIsual_select = false;
                                ins_typebuf(K_SELECT_STRING, REMAP_NONE, 0, true, false);
                            }

                            /* Copy the values from *mp that are used, because
                             * evaluating the expression may invoke a function that
                             * redefines the mapping, thereby making *mp invalid. */
                            int save_m_noremap = mp.m_noremap;
                            boolean save_m_silent = mp.m_silent;
                            Bytes save_m_keys = null;      /* only saved when needed */
                            Bytes save_m_str = null;       /* only saved when needed */

                            /*
                             * Handle ":map <expr>": evaluate the {rhs} as an expression.
                             * Also save and restore the command line for "normal :".
                             */
                            Bytes s;
                            if (mp.m_expr)
                            {
                                int save_vgetc_busy = vgetc_busy;

                                vgetc_busy = 0;
                                save_m_keys = STRDUP(mp.m_keys);
                                save_m_str = STRDUP(mp.m_str);
                                s = eval_map_expr(save_m_str, NUL);
                                vgetc_busy = save_vgetc_busy;
                            }
                            else
                                s = mp.m_str;

                            /*
                             * Insert the 'to' part in the typebuf.tb_buf.
                             * If 'from' field is the same as the start of the 'to' field,
                             * don't remap the first character (but do allow abbreviations).
                             * If m_noremap is set, don't remap the whole 'to' part.
                             */
                            boolean b = false;
                            if (s != null)
                            {
                                int noremap;

                                if (save_m_noremap != REMAP_YES)
                                    noremap = save_m_noremap;
                                else if (STRNCMP(s, (save_m_keys != null) ? save_m_keys : mp.m_keys, keylen) != 0)
                                    noremap = REMAP_YES;
                                else
                                    noremap = REMAP_SKIP;
                                b = ins_typebuf(s, noremap, 0, true, cmd_silent || save_m_silent);
                            }
                            if (!b)
                            {
                                c = -1;
                                break;
                            }
                            continue;
                        }
                    }

    /*
     * get a character: 3. from the user - handle <Esc> in Insert mode
     */
                    /*
                     * Special case: if we get an <ESC> in insert mode and there are no more
                     * characters at once, we pretend to go out of insert mode.  This prevents
                     * the one second delay after typing an <ESC>.  If we get something after
                     * all, we may have to redisplay the mode.  That the cursor is in the wrong
                     * place does not matter.
                     */
                    int len = 0;
                    int new_wcol = curwin.w_wcol;
                    int new_wrow = curwin.w_wrow;
                    if (advance
                            && typebuf.tb_len == 1
                            && typebuf.tb_buf.at(typebuf.tb_off) == ESC
                            && no_mapping == 0
                            && ex_normal_busy == 0
                            && typebuf.tb_maplen == 0
                            && (State & INSERT) != 0
                            && (p_timeout[0] || (keylen == KEYLEN_PART_KEY && p_ttimeout[0]))
                            && (len = inchar(typebuf.tb_buf.plus(typebuf.tb_off + typebuf.tb_len), 3, 25L,
                                                                                    typebuf.tb_change_cnt)) == 0)
                    {
                        if (mode_displayed)
                        {
                            unshowmode(true);
                            mode_deleted = true;
                        }
                        validate_cursor();
                        int old_wcol = curwin.w_wcol;
                        int old_wrow = curwin.w_wrow;

                        /* move cursor left, if possible */
                        if (curwin.w_cursor.col != 0)
                        {
                            int col = 0;
                            if (0 < curwin.w_wcol)
                            {
                                if (did_ai)
                                {
                                    /*
                                     * We are expecting to truncate the trailing white-space,
                                     * so find the last non-white character.
                                     */
                                    col = curwin.w_wcol = 0;
                                    Bytes ptr = ml_get_curline();
                                    for (int vcol = col; col < curwin.w_cursor.col; )
                                    {
                                        if (!vim_iswhite(ptr.at(col)))
                                            curwin.w_wcol = vcol;
                                        vcol += lbr_chartabsize(ptr, ptr.plus(col), vcol);
                                        col += us_ptr2len_cc(ptr.plus(col));
                                    }
                                    curwin.w_wrow = curwin.w_cline_row + curwin.w_wcol / curwin.w_width;
                                    curwin.w_wcol %= curwin.w_width;
                                    curwin.w_wcol += curwin_col_off();
                                    col = 0;        /* no correction needed */
                                }
                                else
                                {
                                    --curwin.w_wcol;
                                    col = curwin.w_cursor.col - 1;
                                }
                            }
                            else if (curwin.w_onebuf_opt.wo_wrap[0] && 0 < curwin.w_wrow)
                            {
                                --curwin.w_wrow;
                                curwin.w_wcol = curwin.w_width - 1;
                                col = curwin.w_cursor.col - 1;
                            }

                            if (0 < col && 0 < curwin.w_wcol)
                            {
                                /* Correct when the cursor is on the right halve of a double-wide character. */
                                Bytes p = ml_get_curline();
                                col -= us_head_off(p, p.plus(col));
                                if (1 < us_ptr2cells(p.plus(col)))
                                    --curwin.w_wcol;
                            }
                        }
                        setcursor();
                        out_flush();
                        new_wcol = curwin.w_wcol;
                        new_wrow = curwin.w_wrow;
                        curwin.w_wcol = old_wcol;
                        curwin.w_wrow = old_wrow;
                    }
                    if (len < 0)
                        continue;   /* end of input script reached */

                    /* Allow mapping for just typed characters.
                     * When we get here, len is the number of extra bytes and typebuf.tb_len is 1. */
                    for (int n = 1; n <= len; n++)
                        typebuf.tb_noremap.be(typebuf.tb_off + n, RM_YES);
                    typebuf.tb_len += len;

                    /* buffer full, don't map */
                    if (typebuf.tb_maplen + MAXMAPLEN <= typebuf.tb_len)
                    {
                        timedout = true;
                        continue;
                    }

                    if (0 < ex_normal_busy)
                    {
                        /* No typeahead left and inside ":normal".
                         * Must return something to avoid getting stuck.
                         * When an incomplete mapping is present, behave like it timed out. */
                        if (0 < typebuf.tb_len)
                        {
                            timedout = true;
                            continue;
                        }
                        /* When 'insertmode' is set, ESC just beeps in Insert mode.
                         * Use CTRL-L to make edit() return.
                         * For the command line only CTRL-C always breaks it.
                         * For the cmdline window: Alternate between ESC and CTRL-C:
                         * ESC for most situations and CTRL-C to close the cmdline window. */
                        if (p_im[0] && (State & INSERT) != 0)
                            c = Ctrl_L;
                        else if ((State & CMDLINE) != 0 || (0 < cmdwin_type && __tc == ESC))
                            c = Ctrl_C;
                        else
                            c = ESC;
                        __tc = c;
                        break;
                    }

    /*
     * get a character: 3. from the user - update display
     */
                    /* In insert mode a screen update is skipped when characters are still available.
                     * But when those available characters are part of a mapping, and we are going
                     * to do a blocking wait here.  Need to update the screen to display the changed
                     * text so far.  Also for when 'lazyredraw' is set and redrawing was postponed
                     * because there was something in the input buffer (e.g., termresponse). */
                    if (((State & INSERT) != 0 || p_lz[0]) && (State & CMDLINE) == 0
                              && advance && must_redraw != 0 && !need_wait_return)
                    {
                        update_screen(0);
                        setcursor();            /* put cursor back where it belongs */
                    }

                    /*
                     * If we have a partial match (and are going to wait for more input from the user),
                     * show the partially matched characters to the user with showcmd.
                     */
                    int i = 0;
                    int c1 = 0;
                    if (0 < typebuf.tb_len && advance && exmode_active == 0)
                    {
                        if (((State & (NORMAL | INSERT)) != 0 || State == LANGMAP) && State != HITRETURN)
                        {
                            /* this looks nice when typing a dead character map */
                            if ((State & INSERT) != 0
                                && mb_ptr2cells(typebuf.tb_buf.plus(typebuf.tb_off + typebuf.tb_len - 1)) == 1)
                            {
                                edit_putchar(typebuf.tb_buf.at(typebuf.tb_off + typebuf.tb_len - 1), false);
                                setcursor();    /* put cursor back where it belongs */
                                c1 = 1;
                            }
                            /* need to use the col and row from above here */
                            int old_wcol = curwin.w_wcol;
                            int old_wrow = curwin.w_wrow;
                            curwin.w_wcol = new_wcol;
                            curwin.w_wrow = new_wrow;
                            push_showcmd();
                            if (SHOWCMD_COLS < typebuf.tb_len)
                                i = typebuf.tb_len - SHOWCMD_COLS;
                            for ( ; i < typebuf.tb_len; i++)
                                add_to_showcmd(typebuf.tb_buf.at(typebuf.tb_off + i));
                            curwin.w_wcol = old_wcol;
                            curwin.w_wrow = old_wrow;
                        }

                        /* this looks nice when typing a dead character map */
                        if ((State & CMDLINE) != 0
                                && cmdline_star == 0
                                && mb_ptr2cells(typebuf.tb_buf.plus(typebuf.tb_off + typebuf.tb_len - 1)) == 1)
                        {
                            putcmdline(typebuf.tb_buf.at(typebuf.tb_off + typebuf.tb_len - 1), false);
                            c1 = 1;
                        }
                    }

    /*
     * get a character: 3. from the user - get it
     */
                    int wait_tb_len = typebuf.tb_len;
                    len = inchar(typebuf.tb_buf.plus(typebuf.tb_off + typebuf.tb_len),
                            typebuf.tb_buflen - typebuf.tb_off - typebuf.tb_len - 1,
                            !advance
                                ? 0
                                : ((typebuf.tb_len == 0
                                        || !(p_timeout[0] || (p_ttimeout[0] && keylen == KEYLEN_PART_KEY)))
                                        ? -1L
                                        : ((keylen == KEYLEN_PART_KEY && 0 <= p_ttm[0])
                                                ? p_ttm[0]
                                                : p_tm[0])), typebuf.tb_change_cnt);

                    if (i != 0)
                        pop_showcmd();
                    if (c1 == 1)
                    {
                        if ((State & INSERT) != 0)
                            edit_unputchar();
                        if ((State & CMDLINE) != 0)
                            unputcmdline();
                        else
                            setcursor();            /* put cursor back where it belongs */
                    }

                    if (len < 0)
                        continue;                   /* end of input script reached */
                    if (len == 0)                   /* no character available */
                    {
                        if (!advance)
                        {
                            c = NUL;
                            break;
                        }
                        if (0 < wait_tb_len)        /* timed out */
                        {
                            timedout = true;
                            continue;
                        }
                    }
                    else
                    {   /* allow mapping for just typed characters */
                        while (typebuf.tb_buf.at(typebuf.tb_off + typebuf.tb_len) != NUL)
                            typebuf.tb_noremap.be(typebuf.tb_off + typebuf.tb_len++, RM_YES);
                    }
                }
            }
        } while (c < 0 || (advance && c == NUL));   /* if advance is false don't loop on NULs */

        /*
         * The "INSERT" message is taken care of here:
         *   if we return an ESC to exit insert mode, the message is deleted;
         *   if we don't return an ESC, but deleted the message before, redisplay it.
         */
        if (advance && p_smd[0] && msg_silent == 0 && (State & INSERT) != 0)
        {
            if (c == ESC && !mode_deleted && no_mapping == 0 && mode_displayed)
            {
                if (typebuf.tb_len != 0 && !keyTyped)
                    redraw_cmdline = true;          /* delete mode later */
                else
                    unshowmode(false);
            }
            else if (c != ESC && mode_deleted)
            {
                if (typebuf.tb_len != 0 && !keyTyped)
                    redraw_cmdline = true;          /* show mode later */
                else
                    showmode();
            }
        }

        --vgetc_busy;

        return c;
    }

    /*
     * inchar() - get one character from
     *      1. a scriptfile
     *      2. the keyboard
     *
     *  As much characters as we can get (upto 'maxlen') are put in "buf" and
     *  NUL terminated (buffer length must be 'maxlen' + 1).
     *  Minimum for "maxlen" is 3!!!!
     *
     *  "tb_change_cnt" is the value of typebuf.tb_change_cnt if "buf" points into it.
     *  When typebuf.tb_change_cnt changes (e.g., when a message is received from
     *  a remote client) "buf" can no longer be used.  "tb_change_cnt" is 0 otherwise.
     *
     *  If we got an interrupt all input is read until none is available.
     *
     *  If wait_time == 0  there is no waiting for the char.
     *  If wait_time == n  we wait for n msec for a character to arrive.
     *  If wait_time == -1 we wait forever for a character to arrive.
     *
     *  Return the number of obtained characters.
     *  Return -1 when end of input script reached.
     */
    /*private*/ static int inchar(Bytes buf, int maxlen, long wait_time, int tb_change_cnt)
        /* wait_time: milli seconds */
    {
        if (wait_time == -1L || 100L < wait_time)   /* flush output before waiting */
        {
            cursor_on();
            out_flush();
        }

        undo_off = false;                           /* restart undo now */

        int len = 0;
        boolean retesc = false;                     /* return ESC with gotint */

        /*
         * Get a character from a script file if there is one.
         * If interrupted: Stop reading script files, close them all.
         */
        int script_char = -1;
        while (scriptin[curscript] != null && script_char < 0 && !ignore_script)
        {
            if (got_int || (script_char = libc.getc(scriptin[curscript])) < 0)
            {
                /* Reached EOF.
                 * Careful: closescript() frees typebuf.tb_buf[] and *buf may
                 * point inside typebuf.tb_buf[].  Don't use *buf after this! */
                closescript();
                /*
                 * When reading script file is interrupted, return an ESC to get back to normal mode.
                 * Otherwise return -1, because typebuf.tb_buf[] has changed.
                 */
                if (got_int)
                    retesc = true;
                else
                    return -1;
            }
            else
            {
                buf.be(0, script_char);
                len = 1;
            }
        }

        if (script_char < 0)        /* did not get a character from script */
        {
            /*
             * If we got an interrupt, skip all previously typed characters
             * and return true if quit reading script file.
             * Stop reading typeahead when a single CTRL-C was read,
             * fill_input_buf() returns this when not able to read from stdin.
             * Don't use *buf here, closescript() may have freed typebuf.tb_buf[]
             * and "buf" may be pointing inside typebuf.tb_buf[].
             */
            if (got_int)
            {
                final int DUM_LEN = MAXMAPLEN * 3 + 3;
                Bytes dum = new Bytes(DUM_LEN + 1);

                for ( ; ; )
                {
                    len = ui_inchar(dum, DUM_LEN, 0L, 0);
                    if (len == 0 || (len == 1 && dum.at(0) == 3))
                        break;
                }
                return retesc ? 1 : 0;
            }

            /*
             * Always flush the output characters when getting input characters from the user.
             */
            out_flush();

            /*
             * Fill up to a third of the buffer, because each character may be tripled below.
             */
            len = ui_inchar(buf, maxlen / 3, wait_time, tb_change_cnt);
        }

        if (typebuf_changed(tb_change_cnt))
            return 0;

        return fix_input_buffer(buf, len, 0 <= script_char);
    }

    /*
     * Fix typed characters for use by vgetc() and check_termcode().
     * buf[] must have room to triple the number of bytes!
     * Returns the new length.
     */
    /*private*/ static int fix_input_buffer(Bytes buf, int len, boolean script)
        /* script: true when reading from a script */
    {
        /*
         * Two characters are special: NUL and KB_SPECIAL.
         * When compiled With the GUI CSI is also special.
         * Replace        NUL by KB_SPECIAL KS_ZERO    KE_FILLER
         * Replace KB_SPECIAL by KB_SPECIAL KS_SPECIAL KE_FILLER
         * Replace        CSI by KB_SPECIAL KS_EXTRA   KE_CSI
         * Don't replace KB_SPECIAL when reading a script file.
         */
        Bytes p = buf;
        for (int i = len; 0 <= --i; p = p.plus(1))
        {
            if (p.at(0) == NUL || (p.at(0) == KB_SPECIAL && !script
                    /* timeout may generate K_CURSORHOLD */
                    && (i < 2 || p.at(1) != KS_EXTRA || p.at(2) != KE_CURSORHOLD)))
            {
                BCOPY(p, 3, p, 1, i);
                p.be(2, KB_THIRD(char_u(p.at(0))));
                p.be(1, KB_SECOND(char_u(p.at(0))));
                p.be(0, KB_SPECIAL);
                p = p.plus(2);
                len += 2;
            }
        }
        p.be(0, NUL);           /* add trailing NUL */

        return len;
    }

    /*
     * Return true when bytes are in the input buffer or in the typeahead buffer.
     * Normally the input buffer would be sufficient, but the server_to_input_buf()
     * or feedkeys() may insert characters in the typeahead buffer while we are
     * waiting for input to arrive.
     */
    /*private*/ static boolean input_available()
    {
        return (!is_input_buf_empty() || typebuf_was_filled);
    }

    /*
     * map[!]                   : show all key mappings
     * map[!] {lhs}             : show key mapping for {lhs}
     * map[!] {lhs} {rhs}       : set key mapping for {lhs} to {rhs}
     * noremap[!] {lhs} {rhs}   : same, but no remapping for {rhs}
     * unmap[!] {lhs}           : remove key mapping for {lhs}
     * abbr                     : show all abbreviations
     * abbr {lhs}               : show abbreviations for {lhs}
     * abbr {lhs} {rhs}         : set abbreviation for {lhs} to {rhs}
     * noreabbr {lhs} {rhs}     : same, but no remapping for {rhs}
     * unabbr {lhs}             : remove abbreviation for {lhs}
     *
     * maptype: 0 for :map, 1 for :unmap, 2 for noremap.
     *
     * arg is pointer to any arguments.  Note: arg cannot be a read-only string,
     * it will be modified.
     *
     * for :map   mode is NORMAL + VISUAL + SELECTMODE + OP_PENDING
     * for :map!  mode is INSERT + CMDLINE
     * for :cmap  mode is CMDLINE
     * for :imap  mode is INSERT
     * for :lmap  mode is LANGMAP
     * for :nmap  mode is NORMAL
     * for :vmap  mode is VISUAL + SELECTMODE
     * for :xmap  mode is VISUAL
     * for :smap  mode is SELECTMODE
     * for :omap  mode is OP_PENDING
     *
     * for :abbr  mode is INSERT + CMDLINE
     * for :iabbr mode is INSERT
     * for :cabbr mode is CMDLINE
     *
     * Return 0 for success
     *        1 for invalid arguments
     *        2 for no match
     *        4 for out of mem
     *        5 for entry not unique
     */
    /*private*/ static int do_map(int maptype, Bytes arg, int mode, boolean abbrev)
        /* abbrev: not a mapping but an abbreviation */
    {
        int retval = 0;

        int len = 0;
        boolean did_it = false;
        boolean did_local = false;
        boolean unique = false;
        boolean nowait = false;
        boolean silent = false;
        boolean special = false;
        boolean expr = false;

        Bytes keys = arg;
        mapblock_C[][] map_table = maphash;
        mapblock_C[] abbr_table = first_abbr;

        /* For ":noremap" don't remap, otherwise do remap. */
        int noremap;
        if (maptype == 2)
            noremap = REMAP_NONE;
        else
            noremap = REMAP_YES;

        /* Accept <buffer>, <nowait>, <silent>, <expr> <script> and <unique> in any order. */
        for ( ; ; )
        {
            /*
             * Check for "<buffer>": mapping local to buffer.
             */
            if (STRNCMP(keys, u8("<buffer>"), 8) == 0)
            {
                keys = skipwhite(keys.plus(8));
                map_table = curbuf.b_maphash;
                abbr_table = curbuf.b_first_abbr;
                continue;
            }

            /*
             * Check for "<nowait>": don't wait for more characters.
             */
            if (STRNCMP(keys, u8("<nowait>"), 8) == 0)
            {
                keys = skipwhite(keys.plus(8));
                nowait = true;
                continue;
            }

            /*
             * Check for "<silent>": don't echo commands.
             */
            if (STRNCMP(keys, u8("<silent>"), 8) == 0)
            {
                keys = skipwhite(keys.plus(8));
                silent = true;
                continue;
            }

            /*
             * Check for "<special>": accept special keys in <>
             */
            if (STRNCMP(keys, u8("<special>"), 9) == 0)
            {
                keys = skipwhite(keys.plus(9));
                special = true;
                continue;
            }

            /*
             * Check for "<script>": remap script-local mappings only
             */
            if (STRNCMP(keys, u8("<script>"), 8) == 0)
            {
                keys = skipwhite(keys.plus(8));
                noremap = REMAP_SCRIPT;
                continue;
            }

            /*
             * Check for "<expr>": {rhs} is an expression.
             */
            if (STRNCMP(keys, u8("<expr>"), 6) == 0)
            {
                keys = skipwhite(keys.plus(6));
                expr = true;
                continue;
            }
            /*
             * Check for "<unique>": don't overwrite an existing mapping.
             */
            if (STRNCMP(keys, u8("<unique>"), 8) == 0)
            {
                keys = skipwhite(keys.plus(8));
                unique = true;
                continue;
            }
            break;
        }

        /*
         * Find end of keys and skip CTRL-Vs (and backslashes) in it.
         * Accept backslash like CTRL-V when 'cpoptions' does not contain 'B'.
         * with :unmap white space is included in the keys, no argument possible.
         */
        Bytes p = keys;
        boolean do_backslash = (vim_strbyte(p_cpo[0], CPO_BSLASH) == null);
        while (p.at(0) != NUL && (maptype == 1 || !vim_iswhite(p.at(0))))
        {
            if ((p.at(0) == Ctrl_V || (do_backslash && p.at(0) == (byte)'\\')) && p.at(1) != NUL)
                p = p.plus(1);                /* skip CTRL-V or backslash */
            p = p.plus(1);
        }
        if (p.at(0) != NUL)
            (p = p.plus(1)).be(-1, NUL);

        p = skipwhite(p);
        Bytes rhs = p;
        boolean hasarg = (rhs.at(0) != NUL);
        boolean haskey = (keys.at(0) != NUL);

        /* check for :unmap without argument */
        if (maptype == 1 && !haskey)
            return 1;

        /*
         * If mapping has been given as ^V<C_UP> say, then replace the term codes
         * with the appropriate two bytes.  If it is a shifted special key,
         * unshift it too, giving another two bytes.
         * replace_termcodes() may move the result to allocated memory,
         * which needs to be freed later (keys_buf[0] and arg_buf[0]).
         * replace_termcodes() also removes CTRL-Vs and sometimes backslashes.
         */
        if (haskey)
            keys = replace_termcodes(keys, true, true, special);
        Bytes orig_rhs = rhs;
        if (hasarg)
        {
            if (STRCASECMP(rhs, u8("<nop>")) == 0)      /* "<Nop>" means nothing */
                rhs = u8("");
            else
                rhs = replace_termcodes(rhs, false, true, special);
        }

        /*
         * check arguments and translate function keys
         */
        if (haskey)
        {
            len = strlen(keys);
            if (MAXMAPLEN < len)            /* maximum length of MAXMAPLEN chars */
                return 1;

            if (abbrev && maptype != 1)
            {
                /*
                 * If an abbreviation ends in a keyword character,
                 * the rest must be all keyword-char or all non-keyword-char.
                 * Otherwise we won't be able to find the start of it
                 * in a vi-compatible way.
                 */
                int same = -1;

                boolean first = us_iswordp(keys, curbuf);
                boolean last = first;
                p = keys.plus(us_ptr2len_cc(keys));
                int n = 1;
                while (BLT(p, keys.plus(len)))
                {
                    n++;                                /* nr of (multi-byte) chars */
                    last = us_iswordp(p, curbuf);       /* type of last char */
                    if (same == -1 && last != first)
                        same = n - 1;                   /* count of same char type */
                    p = p.plus(us_ptr2len_cc(p));
                }
                if (last && 2 < n && 0 <= same && same < n - 1)
                    return 1;

                /* An abbreviation cannot contain white space. */
                for (/*int */n = 0; n < len; n++)
                    if (vim_iswhite(keys.at(n)))
                        return 1;
            }
        }

        if (haskey && hasarg && abbrev)     /* if we will add an abbreviation */
            no_abbr = false;                /* reset flag that indicates there are no abbreviations */

        if (!haskey || (maptype != 1 && !hasarg))
            msg_start();

        /*
         * Check if a new local mapping wasn't already defined globally.
         */
        if (map_table == curbuf.b_maphash && haskey && hasarg && maptype != 1)
        {
            /* need to loop over all global hash lists */
            for (int hash = 0; hash < 256 && !got_int; hash++)
            {
                mapblock_C mp;
                if (abbrev)
                {
                    if (hash != 0)  /* there is only one abbreviation list */
                        break;
                    mp = first_abbr[0];
                }
                else
                    mp = maphash[hash][0];

                for ( ; mp != null && !got_int; mp = mp.m_next)
                {
                    /* check entries with the same mode */
                    if ((mp.m_mode & mode) != 0
                            && mp.m_keylen == len
                            && unique
                            && STRNCMP(mp.m_keys, keys, len) == 0)
                    {
                        if (abbrev)
                            emsg2(u8("E224: global abbreviation already exists for %s"), mp.m_keys);
                        else
                            emsg2(u8("E225: global mapping already exists for %s"), mp.m_keys);
                        return 5;
                    }
                }
            }
        }

        /*
         * When listing global mappings, also list buffer-local ones here.
         */
        if (map_table != curbuf.b_maphash && !hasarg && maptype != 1)
        {
            /* need to loop over all global hash lists */
            for (int hash = 0; hash < 256 && !got_int; hash++)
            {
                mapblock_C mp;
                if (abbrev)
                {
                    if (hash != 0)  /* there is only one abbreviation list */
                        break;
                    mp = curbuf.b_first_abbr[0];
                }
                else
                    mp = curbuf.b_maphash[hash][0];
                for ( ; mp != null && !got_int; mp = mp.m_next)
                {
                    /* check entries with the same mode */
                    if ((mp.m_mode & mode) != 0)
                    {
                        if (!haskey)                    /* show all entries */
                        {
                            showmap(mp, true);
                            did_local = true;
                        }
                        else
                        {
                            int n = mp.m_keylen;
                            if (STRNCMP(mp.m_keys, keys, (n < len) ? n : len) == 0)
                            {
                                showmap(mp, true);
                                did_local = true;
                            }
                        }
                    }
                }
            }
        }

        /*
         * Find an entry in the maphash[] list that matches.
         * For :unmap we may loop two times: once to try to unmap an entry with a
         * matching 'from' part, a second time, if the first fails, to unmap an
         * entry with a matching 'to' part.  This was done to allow ":ab foo bar"
         * to be unmapped by typing ":unab foo", where "foo" will be replaced by
         * "bar" because of the abbreviation.
         */
        for (int round = 0; (round == 0 || maptype == 1) && round <= 1 && !did_it && !got_int; round++)
        {
            /* need to loop over all hash lists */
            for (int hash = 0; hash < 256 && !got_int; hash++)
            {
                mapblock_C[] mpp0;
                if (abbrev)
                {
                    if (0 < hash)   /* there is only one abbreviation list */
                        break;
                    mpp0 = abbr_table;
                }
                else
                    mpp0 = map_table[hash];
                for (mapblock_C mpp = null, mp = mpp0[0]; mp != null && !got_int; mp = mpp.m_next)
                {
                    if ((mp.m_mode & mode) == 0)    /* skip entries with wrong mode */
                    {
                        mpp = mp;
                        continue;
                    }
                    if (!haskey)                /* show all entries */
                    {
                        showmap(mp, map_table != maphash);
                        did_it = true;
                    }
                    else                        /* do we have a match? */
                    {
                        int n;
                        if (round != 0)      /* second round: Try unmap "rhs" string */
                        {
                            n = strlen(mp.m_str);
                            p = mp.m_str;
                        }
                        else
                        {
                            n = mp.m_keylen;
                            p = mp.m_keys;
                        }
                        if (STRNCMP(p, keys, (n < len) ? n : len) == 0)
                        {
                            if (maptype == 1)       /* delete entry */
                            {
                                /* Only accept a full match.  For abbreviations we
                                 * ignore trailing space when matching with the
                                 * "lhs", since an abbreviation can't have trailing space. */
                                if (n != len && (!abbrev || round != 0 || len < n || skipwhite(keys.plus(n)).at(0) != NUL))
                                {
                                    mpp = mp;
                                    continue;
                                }
                                /*
                                 * We reset the indicated mode bits.  If nothing is
                                 * left the entry is deleted below.
                                 */
                                mp.m_mode &= ~mode;
                                did_it = true;      /* remember we did something */
                            }
                            else if (!hasarg)       /* show matching entry */
                            {
                                showmap(mp, map_table != maphash);
                                did_it = true;
                            }
                            else if (n != len)      /* new entry is ambiguous */
                            {
                                mpp = mp;
                                continue;
                            }
                            else if (unique)
                            {
                                if (abbrev)
                                    emsg2(u8("E226: abbreviation already exists for %s"), p);
                                else
                                    emsg2(u8("E227: mapping already exists for %s"), p);
                                return 5;
                            }
                            else                    /* new "rhs" for existing entry */
                            {
                                mp.m_mode &= ~mode;     /* remove mode bits */
                                if (mp.m_mode == 0 && !did_it) /* reuse entry */
                                {
                                    Bytes newstr = STRDUP(rhs);
                                    mp.m_str = newstr;
                                    mp.m_orig_str = STRDUP(orig_rhs);
                                    mp.m_noremap = noremap;
                                    mp.m_nowait = nowait;
                                    mp.m_silent = silent;
                                    mp.m_mode = mode;
                                    mp.m_expr = expr;
                                    mp.m_script_ID = current_SID;
                                    did_it = true;
                                }
                            }
                            if (mp.m_mode == 0)     /* entry can be deleted */
                            {
                                if (mpp == null)
                                    mpp0[0] = map_free(mpp0[0]);
                                else
                                    mpp.m_next = map_free(mpp.m_next);
                                continue;           /* continue with *mpp */
                            }

                            /*
                             * May need to put this entry into another hash list.
                             */
                            int new_hash = map_hash(mp.m_mode, mp.m_keys.at(0));
                            if (!abbrev && new_hash != hash)
                            {
                                if (mpp == null)
                                    mpp0[0] = mp.m_next;
                                else
                                    mpp.m_next = mp.m_next;
                                mp.m_next = map_table[new_hash][0];
                                map_table[new_hash][0] = mp;

                                continue;           /* continue with *mpp */
                            }
                        }
                    }
                    mpp = mp;
                }
            }
        }

        if (maptype == 1)                       /* delete entry */
        {
            if (!did_it)
                retval = 2;                     /* no match */
            else if (keys.at(0) == Ctrl_C)
            {
                /* If CTRL-C has been unmapped, reuse it for Interrupting. */
                if (map_table == curbuf.b_maphash)
                    curbuf.b_mapped_ctrl_c &= ~mode;
                else
                    mapped_ctrl_c &= ~mode;
            }
            return retval;
        }

        if (!haskey || !hasarg)                 /* print entries */
        {
            if (!did_it && !did_local)
            {
                if (abbrev)
                    msg(u8("No abbreviation found"));
                else
                    msg(u8("No mapping found"));
            }
            return retval;                      /* listing finished */
        }

        if (did_it)                     /* have added the new entry already */
            return retval;

        /*
         * Get here when adding a new entry to the maphash[] list or abbrlist.
         */
        mapblock_C mp = new mapblock_C();

        /* If CTRL-C has been mapped, don't always use it for Interrupting. */
        if (keys.at(0) == Ctrl_C)
        {
            if (map_table == curbuf.b_maphash)
                curbuf.b_mapped_ctrl_c |= mode;
            else
                mapped_ctrl_c |= mode;
        }

        mp.m_keys = STRDUP(keys);
        mp.m_str = STRDUP(rhs);
        mp.m_orig_str = STRDUP(orig_rhs);
        mp.m_keylen = strlen(mp.m_keys);
        mp.m_noremap = noremap;
        mp.m_nowait = nowait;
        mp.m_silent = silent;
        mp.m_mode = mode;
        mp.m_expr = expr;
        mp.m_script_ID = current_SID;

        /* add the new entry in front of the abbrlist or maphash[] list */
        if (abbrev)
        {
            mp.m_next = abbr_table[0];
            abbr_table[0] = mp;
        }
        else
        {
            int n = map_hash(mp.m_mode, mp.m_keys.at(0));
            mp.m_next = map_table[n][0];
            map_table[n][0] = mp;
        }

        return retval;
    }

    /*
     * Delete one entry from the abbrlist or maphash[].
     * "mpp" is a pointer to the m_next field of the PREVIOUS entry!
     */
    /*private*/ static mapblock_C map_free(mapblock_C mp)
    {
        mp.m_keys = null;
        mp.m_str = null;
        mp.m_orig_str = null;

        return mp.m_next;
    }

    /*
     * Get the mapping mode from the command name.
     */
    /*private*/ static int get_map_mode(Bytes[] cmdp, boolean forceit)
    {
        int mode;

        Bytes p = cmdp[0];
        byte modec = (p = p.plus(1)).at(-1);

        if (modec == 'i')
            mode = INSERT;                                          /* :imap */
        else if (modec == 'l')
            mode = LANGMAP;                                         /* :lmap */
        else if (modec == 'c')
            mode = CMDLINE;                                         /* :cmap */
        else if (modec == 'n' && p.at(0) != (byte)'o')                         /* avoid :noremap */
            mode = NORMAL;                                          /* :nmap */
        else if (modec == 'v')
            mode = VISUAL + SELECTMODE;                             /* :vmap */
        else if (modec == 'x')
            mode = VISUAL;                                          /* :xmap */
        else if (modec == 's')
            mode = SELECTMODE;                                      /* :smap */
        else if (modec == 'o')
            mode = OP_PENDING;                                      /* :omap */
        else
        {
            p = p.minus(1);
            if (forceit)
                mode = INSERT + CMDLINE;                            /* :map ! */
            else
                mode = VISUAL + SELECTMODE + NORMAL + OP_PENDING;   /* :map */
        }

        cmdp[0] = p;
        return mode;
    }

    /*
     * Clear all mappings or abbreviations.
     * 'abbr' should be false for mappings, true for abbreviations.
     */
    /*private*/ static void map_clear(Bytes _cmdp, Bytes arg, boolean forceit, boolean abbr)
    {
        Bytes[] cmdp = { _cmdp };

        boolean local = (STRCMP(arg, u8("<buffer>")) == 0);
        if (!local && arg.at(0) != NUL)
        {
            emsg(e_invarg);
            return;
        }

        int mode = get_map_mode(cmdp, forceit);
        map_clear_int(curbuf, mode, local, abbr);
    }

    /*
     * Clear all mappings in "mode".
     */
    /*private*/ static void map_clear_int(buffer_C buf, int mode, boolean local, boolean abbr)
        /* buf: buffer for local mappings */
        /* mode: mode in which to delete */
        /* local: true for buffer-local mappings */
        /* abbr: true for abbreviations */
    {
        for (int hash = 0; hash < 256; hash++)
        {
            mapblock_C[] mpp0;

            if (abbr)
            {
                if (0 < hash)       /* there is only one abbrlist */
                    break;
                if (local)
                    mpp0 = buf.b_first_abbr;
                else
                    mpp0 = first_abbr;
            }
            else
            {
                if (local)
                    mpp0 = buf.b_maphash[hash];
                else
                    mpp0 = maphash[hash];
            }

            for (mapblock_C mpp = null, mp = mpp0[0]; mp != null; mp = mpp.m_next)
            {
                if ((mp.m_mode & mode) != 0)
                {
                    mp.m_mode &= ~mode;
                    if (mp.m_mode == 0) /* entry can be deleted */
                    {
                        if (mpp == null)
                            mpp0[0] = map_free(mpp0[0]);
                        else
                            mpp.m_next = map_free(mpp.m_next);
                        continue;
                    }
                    /*
                     * May need to put this entry into another hash list.
                     */
                    int new_hash = map_hash(mp.m_mode, mp.m_keys.at(0));
                    if (!abbr && new_hash != hash)
                    {
                        if (mpp == null)
                            mpp0[0] = mp.m_next;
                        else
                            mpp.m_next = mp.m_next;
                        if (local)
                        {
                            mp.m_next = buf.b_maphash[new_hash][0];
                            buf.b_maphash[new_hash][0] = mp;
                        }
                        else
                        {
                            mp.m_next = maphash[new_hash][0];
                            maphash[new_hash][0] = mp;
                        }
                        continue;           /* continue with *mpp */
                    }
                }
                mpp = mp;
            }
        }
    }

    /*
     * Return characters to represent the map mode in an allocated string.
     * Returns null when out of memory.
     */
    /*private*/ static Bytes map_mode_to_chars(int mode)
    {
        barray_C mapmode = new barray_C(7);

        if ((mode & (INSERT + CMDLINE)) == INSERT + CMDLINE)
            ba_append(mapmode, (byte)'!');                        /* :map! */
        else if ((mode & INSERT) != 0)
            ba_append(mapmode, (byte)'i');                        /* :imap */
        else if ((mode & LANGMAP) != 0)
            ba_append(mapmode, (byte)'l');                        /* :lmap */
        else if ((mode & CMDLINE) != 0)
            ba_append(mapmode, (byte)'c');                        /* :cmap */
        else if ((mode & (NORMAL + VISUAL + SELECTMODE + OP_PENDING)) == NORMAL + VISUAL + SELECTMODE + OP_PENDING)
            ba_append(mapmode, (byte)' ');                        /* :map */
        else
        {
            if ((mode & NORMAL) != 0)
                ba_append(mapmode, (byte)'n');                    /* :nmap */
            if ((mode & OP_PENDING) != 0)
                ba_append(mapmode, (byte)'o');                    /* :omap */
            if ((mode & (VISUAL + SELECTMODE)) == VISUAL + SELECTMODE)
                ba_append(mapmode, (byte)'v');                    /* :vmap */
            else
            {
                if ((mode & VISUAL) != 0)
                    ba_append(mapmode, (byte)'x');                /* :xmap */
                if ((mode & SELECTMODE) != 0)
                    ba_append(mapmode, (byte)'s');                /* :smap */
            }
        }

        ba_append(mapmode, NUL);
        return new Bytes(mapmode.ba_data);
    }

    /*private*/ static void showmap(mapblock_C mp, boolean local)
        /* local: true for buffer-local map */
    {
        if (msg_didout || msg_silent != 0)
        {
            msg_putchar('\n');
            if (got_int)        /* 'q' typed at MORE prompt */
                return;
        }

        int len = 1;

        Bytes mapchars = map_mode_to_chars(mp.m_mode);
        if (mapchars != null)
        {
            msg_puts(mapchars);
            len = strlen(mapchars);
        }

        while (++len <= 3)
            msg_putchar(' ');

        /* Display the LHS.  Get length of what we write. */
        len = msg_outtrans_special(mp.m_keys, true);
        do
        {
            msg_putchar(' ');               /* padd with blanks */
            len++;
        } while (len < 12);

        if (mp.m_noremap == REMAP_NONE)
            msg_puts_attr(u8("*"), hl_attr(HLF_8));
        else if (mp.m_noremap == REMAP_SCRIPT)
            msg_puts_attr(u8("&"), hl_attr(HLF_8));
        else
            msg_putchar(' ');

        if (local)
            msg_putchar('@');
        else
            msg_putchar(' ');

        /* Use false below if we only want things like <Up> to show up
         * as such on the rhs, and not M-x etc, true gets both. */
        if (mp.m_str.at(0) == NUL)
            msg_puts_attr(u8("<Nop>"), hl_attr(HLF_8));
        else
        {
            /* Remove escaping of CSI, because "m_str" is in a format to be used as typeahead. */
            Bytes s = STRDUP(mp.m_str);
            vim_unescape_csi(s);
            msg_outtrans_special(s, false);
        }
        if (0 < p_verbose[0])
            last_set_msg(mp.m_script_ID);
        out_flush();                        /* show one line at a time */
    }

    /*
     * Return true if a map exists that has "str" in the rhs for mode "modechars".
     * Recognize termcap codes in "str".
     * Also checks mappings local to the current buffer.
     */
    /*private*/ static boolean map_to_exists(Bytes str, Bytes modechars, boolean abbr)
    {
        Bytes rhs = replace_termcodes(str, false, true, false);

        int mode = 0;
        if (vim_strchr(modechars, 'n') != null)
            mode |= NORMAL;
        if (vim_strchr(modechars, 'v') != null)
            mode |= VISUAL + SELECTMODE;
        if (vim_strchr(modechars, 'x') != null)
            mode |= VISUAL;
        if (vim_strchr(modechars, 's') != null)
            mode |= SELECTMODE;
        if (vim_strchr(modechars, 'o') != null)
            mode |= OP_PENDING;
        if (vim_strchr(modechars, 'i') != null)
            mode |= INSERT;
        if (vim_strchr(modechars, 'l') != null)
            mode |= LANGMAP;
        if (vim_strchr(modechars, 'c') != null)
            mode |= CMDLINE;

        return map_to_exists_mode(rhs, mode, abbr);
    }

    /*
     * Return true if a map exists that has "str" in the rhs for mode "mode".
     * Also checks mappings local to the current buffer.
     */
    /*private*/ static boolean map_to_exists_mode(Bytes rhs, int mode, boolean abbr)
    {
        /* Do it twice: once for global maps and once for local maps. */
        for (boolean expand_buffer = false; !expand_buffer; expand_buffer = true)
        {
            for (int hash = 0; hash < 256; hash++)
            {
                mapblock_C mp;

                if (abbr)
                {
                    if (0 < hash)           /* there is only one abbr list */
                        break;
                    if (expand_buffer)
                        mp = curbuf.b_first_abbr[0];
                    else
                        mp = first_abbr[0];
                }
                else if (expand_buffer)
                    mp = curbuf.b_maphash[hash][0];
                else
                    mp = maphash[hash][0];

                for ( ; mp != null; mp = mp.m_next)
                {
                    if ((mp.m_mode & mode) != 0 && STRSTR(mp.m_str, rhs) != null)
                        return true;
                }
            }
        }

        return false;
    }

    /*
     * Used below when expanding mapping/abbreviation names.
     */
    /*private*/ static int      expand_mapmodes;
    /*private*/ static boolean  expand_isabbrev;
    /*private*/ static boolean  expand_buffer;

    /*
     * Work out what to complete when doing command line completion of mapping
     * or abbreviation names.
     */
    /*private*/ static Bytes set_context_in_map_cmd(expand_C xp, Bytes _cmd, Bytes arg, boolean forceit, boolean isabbrev, boolean isunmap, int cmdidx)
        /* forceit: true if '!' given */
        /* isabbrev: true if abbreviation */
        /* isunmap: true if unmap/unabbrev command */
    {
        Bytes[] cmd = { _cmd };

        if (forceit && cmdidx != CMD_map && cmdidx != CMD_unmap)
            xp.xp_context = EXPAND_NOTHING;
        else
        {
            if (isunmap)
                expand_mapmodes = get_map_mode(cmd, forceit || isabbrev);
            else
            {
                expand_mapmodes = INSERT + CMDLINE;
                if (!isabbrev)
                    expand_mapmodes += VISUAL + SELECTMODE + NORMAL + OP_PENDING;
            }
            expand_isabbrev = isabbrev;
            xp.xp_context = EXPAND_MAPPINGS;
            expand_buffer = false;
            for ( ; ; )
            {
                if (STRNCMP(arg, u8("<buffer>"), 8) == 0)
                {
                    expand_buffer = true;
                    arg = skipwhite(arg.plus(8));
                    continue;
                }
                if (STRNCMP(arg, u8("<unique>"), 8) == 0)
                {
                    arg = skipwhite(arg.plus(8));
                    continue;
                }
                if (STRNCMP(arg, u8("<nowait>"), 8) == 0)
                {
                    arg = skipwhite(arg.plus(8));
                    continue;
                }
                if (STRNCMP(arg, u8("<silent>"), 8) == 0)
                {
                    arg = skipwhite(arg.plus(8));
                    continue;
                }
                if (STRNCMP(arg, u8("<script>"), 8) == 0)
                {
                    arg = skipwhite(arg.plus(8));
                    continue;
                }
                if (STRNCMP(arg, u8("<expr>"), 6) == 0)
                {
                    arg = skipwhite(arg.plus(6));
                    continue;
                }
                break;
            }
            xp.xp_pattern = arg;
        }

        return null;
    }

    /*
     * Find all mapping/abbreviation names that match regexp 'prog'.
     * For command line expansion of ":[un]map" and ":[un]abbrev" in all modes.
     * Return true if matches found, false otherwise.
     */
    /*private*/ static boolean expandMappings(regmatch_C regmatch, int[] num_file, Bytes[][] file)
    {
        num_file[0] = 0;                  /* return values in case of FAIL */
        file[0] = null;

        int count = 0;	// %% red.

        /*
         * round == 1: Count the matches.
         * round == 2: Build the array to keep the matches.
         */
        for (int round = 1; round <= 2; round++)
        {
            count = 0;

            for (int i = 0; i < 6; i++)
            {
                Bytes p;

                if (i == 0)
                    p = u8("<silent>");
                else if (i == 1)
                    p = u8("<unique>");
                else if (i == 2)
                    p = u8("<script>");
                else if (i == 3)
                    p = u8("<expr>");
                else if (i == 4 && !expand_buffer)
                    p = u8("<buffer>");
                else if (i == 5)
                    p = u8("<nowait>");
                else
                    continue;

                if (vim_regexec(regmatch, p, 0))
                {
                    if (round == 1)
                        count++;
                    else
                        file[0][count++] = STRDUP(p);
                }
            }

            for (int hash = 0; hash < 256; hash++)
            {
                mapblock_C mp;

                if (expand_isabbrev)
                {
                    if (0 < hash)   /* only one abbrev list */
                        break;
                    mp = first_abbr[0];
                }
                else if (expand_buffer)
                    mp = curbuf.b_maphash[hash][0];
                else
                    mp = maphash[hash][0];

                for ( ; mp != null; mp = mp.m_next)
                {
                    if ((mp.m_mode & expand_mapmodes) != 0)
                    {
                        Bytes p = translate_mapping(mp.m_keys);
                        if (p != null && vim_regexec(regmatch, p, 0))
                        {
                            if (round == 1)
                                count++;
                            else
                            {
                                file[0][count++] = p;
                                p = null;
                            }
                        }
                    }
                }
            }

            if (count == 0)                 /* no match found */
                break;

            if (round == 1)
                file[0] = new Bytes[count];
        }

        if (1 < count)
        {
            /* Sort the matches. */
            sort_strings(file[0], count);

            /* Remove multiple entries. */
            Bytes[] pp = file[0];

            for (int i1 = 0, i2 = 1; i2 < count; )
            {
                if (STRCMP(pp[i1], pp[i2]) != 0)
                    pp[++i1] = pp[i2++];
                else
                {
                    pp[i2++] = null;
                    count--;
                }
            }
        }

        num_file[0] = count;
        return (count != 0);
    }

    /*
     * Check for an abbreviation.
     * Cursor is at ptr[col].  When inserting, mincol is where insert started.
     * "c" is the character typed before check_abbr was called.
     * It may have ABBR_OFF added to avoid prepending a CTRL-V to it.
     *
     * Historic vi practice:
     * The last character of an abbreviation must be an id character ([a-zA-Z0-9_]).
     * The characters in front of it must be all id characters or all non-id characters.
     * This allows for abbr. "#i" to "#include".
     *
     * Vim addition:
     * Allow for abbreviations that end in a non-keyword character.
     * Then there must be white space before the abbr.
     *
     * Return true if there is an abbreviation, false if not.
     */
    /*private*/ static boolean check_abbr(int c, Bytes ptr, int col, int mincol)
    {
        if (typebuf.tb_no_abbr_cnt != 0)            /* abbrev. are not recursive */
            return false;

        /* no remapping implies no abbreviation, except for CTRL-] */
        if ((keyNoremap & (RM_NONE|RM_SCRIPT)) != 0 && c != Ctrl_RSB)
            return false;

        /*
         * Check for word before the cursor: if it ends in a keyword char, all chars
         * before it must be keyword chars or non-keyword chars, but not white space.
         * If it ends in a non-keyword char, we accept any characters before it except white space.
         */
        if (col == 0)                               /* cannot be an abbr. */
            return false;

        Bytes p = us_prevptr(ptr, ptr.plus(col));

        boolean vim_abbr = true;                    /* Vim added abbr. */
        boolean is_id = true;
        if (us_iswordp(p, curbuf))
        {
            vim_abbr = false;                       /* vi compatible abbr. */
            if (BLT(ptr, p))
                is_id = us_iswordp(us_prevptr(ptr, p), curbuf);
        }

        int clen = 1;                               /* length in characters */
        while (BLT(ptr.plus(mincol), p))
        {
            p = us_prevptr(ptr, p);
            if (vim_isspace(p.at(0)) || (!vim_abbr && is_id != us_iswordp(p, curbuf)))
            {
                p = p.plus(us_ptr2len_cc(p));
                break;
            }
            clen++;
        }

        int scol = BDIFF(p, ptr);              /* starting column of the abbr. */
        if (scol < mincol)
            scol = mincol;

        if (scol < col)                     /* there is a word in front of the cursor */
        {
            ptr = ptr.plus(scol);
            int len = col - scol;
            mapblock_C mp = curbuf.b_first_abbr[0];
            mapblock_C mp2 = first_abbr[0];
            if (mp == null)
            {
                mp = mp2;
                mp2 = null;
            }
            boolean __;
            for ( ; mp != null; mp = mp.m_next, __ = (mp == null && (mp = mp2) == mp2 && (mp2 = null) == null))
            {
                int qlen = mp.m_keylen;
                Bytes q = mp.m_keys;

                if (vim_strbyte(mp.m_keys, KB_SPECIAL) != null)
                {
                    /* might have CSI escaped mp.m_keys */
                    q = STRDUP(mp.m_keys);
                    vim_unescape_csi(q);
                    qlen = strlen(q);
                }

                /* find entries with right mode and keys */
                boolean match = ((mp.m_mode & State) != 0 && qlen == len && STRNCMP(q, ptr, len) == 0);
                if (match)
                    break;
            }
            if (mp != null)
            {
                Bytes tb = new Bytes(MB_MAXBYTES + 4);
                /*
                 * Found a match:
                 * insert the rest of the abbreviation in typebuf.tb_buf[].
                 * This goes from end to start.
                 *
                 * Characters 0x000 - 0x100: normal chars, may need CTRL-V,
                 * except KB_SPECIAL: becomes KB_SPECIAL KS_SPECIAL KE_FILLER.
                 * Characters where is_special() == true: key codes, need KB_SPECIAL.
                 * Other characters (with ABBR_OFF): don't use CTRL-V.
                 *
                 * Character CTRL-] is treated specially - it completes the
                 * abbreviation, but is not inserted into the input stream.
                 */
                int i = 0;
                if (c != Ctrl_RSB)
                {
                    if (is_special(c) || c == char_u(KB_SPECIAL))   /* special key code, split up */
                    {
                        tb.be(i++, KB_SPECIAL);
                        tb.be(i++, KB_SECOND(c));
                        tb.be(i++, KB_THIRD(c));
                    }
                    else
                    {
                        if (c < ABBR_OFF && (c < ' ' || '~' < c))
                            tb.be(i++, Ctrl_V);                       /* special char needs CTRL-V */

                        /* if ABBR_OFF has been added, remove it here */
                        if (ABBR_OFF <= c)
                            c -= ABBR_OFF;
                        i += utf_char2bytes(c, tb.plus(i));
                    }
                    tb.be(i, NUL);
                    ins_typebuf(tb, 1, 0, true, mp.m_silent);       /* insert the last typed char */
                }

                Bytes s = mp.m_str;
                if (mp.m_expr)
                    s = eval_map_expr(s, c);
                if (s != null)
                {
                    ins_typebuf(s, mp.m_noremap, 0, true, mp.m_silent); /* insert the to string */
                    typebuf.tb_no_abbr_cnt += strlen(s) + i + 1;   /* no abbrev. for these chars */
                }

                tb.be(0, Ctrl_H);
                tb.be(1, NUL);
                len = clen;                                         /* delete characters instead of bytes */
                while (0 < len--)                                   /* delete the from string */
                    ins_typebuf(tb, 1, 0, true, mp.m_silent);

                return true;
            }
        }

        return false;
    }

    /*
     * Evaluate the RHS of a mapping or abbreviations and take care of escaping special characters.
     */
    /*private*/ static Bytes eval_map_expr(Bytes str, int c)
        /* c: NUL or typed character for abbreviation */
    {
        /* Remove escaping of CSI, because "str" is in a format to be used as typeahead. */
        Bytes expr = STRDUP(str);

        vim_unescape_csi(expr);

        cmdline_info_C save_cli = save_cmdline_alloc();

        /* Forbid changing text or using ":normal" to avoid most of the bad side effects.
         * Also restore the cursor position. */
        textlock++;
        ex_normal_lock++;
        set_vim_var_char(c);                /* set v:char to the typed character */

        pos_C save_cursor = new pos_C();
        COPY_pos(save_cursor, curwin.w_cursor);
        int save_msg_col = msg_col;
        int save_msg_row = msg_row;

        Bytes p = eval_to_string(expr, null, false);

        --textlock;
        --ex_normal_lock;

        COPY_pos(curwin.w_cursor, save_cursor);
        msg_col = save_msg_col;
        msg_row = save_msg_row;

        restore_cmdline_alloc(save_cli);

        if (p == null)
            return null;

        /* Escape CSI in the result to be able to use the string as typeahead. */
        return vim_strsave_escape_csi(p);
    }

    /*
     * Copy "p" to allocated memory, escaping KB_SPECIAL and CSI
     * so that the result can be put in the typeahead buffer.
     */
    /*private*/ static Bytes vim_strsave_escape_csi(Bytes p)
    {
        /* Need a buffer to hold up to three times as much. */
        Bytes res = new Bytes(strlen(p) * 3 + 1);

        Bytes d = res;
        for (Bytes s = p; s.at(0) != NUL; )
        {
            if (s.at(0) == KB_SPECIAL && s.at(1) != NUL && s.at(2) != NUL)
            {
                /* Copy special key unmodified. */
                (d = d.plus(1)).be(-1, (s = s.plus(1)).at(-1));
                (d = d.plus(1)).be(-1, (s = s.plus(1)).at(-1));
                (d = d.plus(1)).be(-1, (s = s.plus(1)).at(-1));
            }
            else
            {
                /* Add character, possibly multi-byte to destination, escaping CSI and KB_SPECIAL. */
                int c = us_ptr2char(s);
                d = add_char2buf(c, d);
                for (int len = utf_char2len(c), end = us_ptr2len_cc(s); len < end; len += utf_char2len(c))
                {
                    /* Add following combining char. */
                    c = us_ptr2char(s.plus(len));
                    d = add_char2buf(c, d);
                }
                s = s.plus(us_ptr2len_cc(s));
            }
        }
        d.be(0, NUL);

        return res;
    }

    /*
     * Remove escaping from CSI and KB_SPECIAL characters.
     * Reverse of vim_strsave_escape_csi().
     * Works in-place.
     */
    /*private*/ static void vim_unescape_csi(Bytes p)
    {
        Bytes d = p;
        for (Bytes s = p; s.at(0) != NUL; )
        {
            if (s.at(0) == KB_SPECIAL && s.at(1) == KS_SPECIAL && s.at(2) == KE_FILLER)
            {
                (d = d.plus(1)).be(-1, KB_SPECIAL);
                s = s.plus(3);
            }
            else if ((s.at(0) == KB_SPECIAL || s.at(0) == CSI) && s.at(1) == KS_EXTRA && s.at(2) == KE_CSI)
            {
                (d = d.plus(1)).be(-1, CSI);
                s = s.plus(3);
            }
            else
                (d = d.plus(1)).be(-1, (s = s.plus(1)).at(-1));
        }
        d.be(0, NUL);
    }

    /*
     * Check all mappings for the presence of special key codes.
     * Used after ":set term=xxx".
     */
    /*private*/ static void check_map_keycodes()
    {
        Bytes save_name = sourcing_name;
        sourcing_name = u8("mappings");                 /* avoids giving error messages */

        /* This this once for each buffer,
         * and then once for global mappings/abbreviations with bp == null. */
        for (buffer_C bp = firstbuf; ; bp = bp.b_next)
        {
            /*
             * Do the loop twice: Once for mappings, once for abbreviations.
             * Then loop over all map hash lists.
             */
            for (int abbr = 0; abbr <= 1; abbr++)
                for (int hash = 0; hash < 256; hash++)
                {
                    mapblock_C mp;
                    if (abbr != 0)
                    {
                        if (hash != 0)                   /* there is only one abbr list */
                            break;
                        if (bp != null)
                            mp = bp.b_first_abbr[0];
                        else
                            mp = first_abbr[0];
                    }
                    else
                    {
                        if (bp != null)
                            mp = bp.b_maphash[hash][0];
                        else
                            mp = maphash[hash][0];
                    }
                    for ( ; mp != null; mp = mp.m_next)
                    {
                        for (int i = 0; i <= 1; i++)        /* do this twice */
                        {
                            Bytes p;
                            if (i == 0)
                                p = mp.m_keys;              /* once for the "from" part */
                            else
                                p = mp.m_str;               /* and once for the "to" part */
                            while (p.at(0) != NUL)
                            {
                                if (p.at(0) == KB_SPECIAL)
                                {
                                    p = p.plus(1);
                                    if (char_u(p.at(0)) < 0x80)  /* for "normal" tcap entries */
                                    {
                                        Bytes buf = new Bytes(3);

                                        buf.be(0, p.at(0));
                                        buf.be(1, p.at(1));
                                        buf.be(2, NUL);
                                        add_termcap_entry(buf, false);
                                    }
                                    p = p.plus(1);
                                }
                                p = p.plus(1);
                            }
                        }
                    }
                }

            if (bp == null)
                break;
        }

        sourcing_name = save_name;
    }

    /*
     * Check the string "keys" against the lhs of all mappings.
     * Return pointer to rhs of mapping (mapblock.m_str); null when no mapping found.
     */
    /*private*/ static Bytes check_map(Bytes keys, int mode, boolean exact, boolean ign_mod, boolean abbr, mapblock_C[] mp_ptr, int[] local_ptr)
        /* exact: require exact match */
        /* ign_mod: ignore preceding modifier */
        /* abbr: do abbreviations */
        /* mp_ptr: return: pointer to mapblock or null */
        /* local_ptr: return: buffer-local mapping or null */
    {
        int len = strlen(keys);
        for (int local = 1; 0 <= local; --local)
            /* loop over all hash lists */
            for (int hash = 0; hash < 256; hash++)
            {
                mapblock_C mp;
                if (abbr)
                {
                    if (0 < hash)           /* there is only one list. */
                        break;
                    if (local != 0)
                        mp = curbuf.b_first_abbr[0];
                    else
                        mp = first_abbr[0];
                }
                else if (local != 0)
                    mp = curbuf.b_maphash[hash][0];
                else
                    mp = maphash[hash][0];

                for ( ; mp != null; mp = mp.m_next)
                {
                    /* skip entries with wrong mode, wrong length and not matching ones */
                    if ((mp.m_mode & mode) != 0 && (!exact || mp.m_keylen == len))
                    {
                        int minlen;
                        if (mp.m_keylen < len)
                            minlen = mp.m_keylen;
                        else
                            minlen = len;
                        Bytes s = mp.m_keys;
                        if (ign_mod && s.at(0) == KB_SPECIAL && s.at(1) == KS_MODIFIER && s.at(2) != NUL)
                        {
                            s = s.plus(3);
                            if (mp.m_keylen - 3 < len)
                                minlen = mp.m_keylen - 3;
                        }
                        if (STRNCMP(s, keys, minlen) == 0)
                        {
                            if (mp_ptr != null)
                                mp_ptr[0] = mp;
                            if (local_ptr != null)
                                local_ptr[0] = local;
                            return mp.m_str;
                        }
                    }
                }
            }

        return null;
    }

    /*
     * Set up default mappings.
     */
    /*private*/ static void init_mappings()
    {
    }

    /*
     * Add a mapping "map" for mode "mode".
     * Need to put string in allocated memory, because do_map() will modify it.
     */
    /*private*/ static void add_map(Bytes map, int mode)
    {
        Bytes cpo_save = p_cpo[0];

        p_cpo[0] = u8("");                     /* allow <> notation */
        Bytes s = STRDUP(map);
        do_map(0, s, mode, false);

        p_cpo[0] = cpo_save;
    }
}
