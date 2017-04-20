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
public class VimU
{
    /*
     * undo.c: multi level undo facility
     *
     * The saved lines are stored in a list of lists (one for each buffer):
     *
     * b_u_oldhead------------------------------------------------+
     *                                                            |
     *                                                            V
     *                +--------------+    +--------------+    +--------------+
     * b_u_newhead--->| u_header     |    | u_header     |    | u_header     |
     *                |     uh_next------>|     uh_next------>|     uh_next---->null
     *         null<--------uh_prev  |<---------uh_prev  |<---------uh_prev  |
     *                |     uh_entry |    |     uh_entry |    |     uh_entry |
     *                +--------|-----+    +--------|-----+    +--------|-----+
     *                         |                   |                   |
     *                         V                   V                   V
     *                +--------------+    +--------------+    +--------------+
     *                | u_entry      |    | u_entry      |    | u_entry      |
     *                |     ue_next  |    |     ue_next  |    |     ue_next  |
     *                +--------|-----+    +--------|-----+    +--------|-----+
     *                         |                   |                   |
     *                         V                   V                   V
     *                +--------------+            null                null
     *                | u_entry      |
     *                |     ue_next  |
     *                +--------|-----+
     *                         |
     *                         V
     *                        etc.
     *
     * Each u_entry list contains the information for one undo or redo.
     * curbuf.b_u_curhead points to the header of the last undo (the next redo),
     * or is null if nothing has been undone (end of the branch).
     *
     * For keeping alternate undo/redo branches the uh_alt field is used.  Thus at
     * each point in the list a branch may appear for an alternate to redo.  The
     * uh_seq field is numbered sequentially to be able to find a newer or older
     * branch.
     *
     *                 +---------------+    +---------------+
     * b_u_oldhead --->| u_header      |    | u_header      |
     *                 |   uh_alt_next ---->|   uh_alt_next ----> null
     *         null <----- uh_alt_prev |<------ uh_alt_prev |
     *                 |   uh_prev     |    |   uh_prev     |
     *                 +-----|---------+    +-----|---------+
     *                       |                    |
     *                       V                    V
     *                 +---------------+    +---------------+
     *                 | u_header      |    | u_header      |
     *                 |   uh_alt_next |    |   uh_alt_next |
     * b_u_newhead --->|   uh_alt_prev |    |   uh_alt_prev |
     *                 |   uh_prev     |    |   uh_prev     |
     *                 +-----|---------+    +-----|---------+
     *                       |                    |
     *                       V                    V
     *                     null             +---------------+    +---------------+
     *                                      | u_header      |    | u_header      |
     *                                      |   uh_alt_next ---->|   uh_alt_next |
     *                                      |   uh_alt_prev |<------ uh_alt_prev |
     *                                      |   uh_prev     |    |   uh_prev     |
     *                                      +-----|---------+    +-----|---------+
     *                                            |                    |
     *                                           etc.                 etc.
     *
     * All data is allocated and will all be freed when the buffer is unloaded.
     */

    /* Structure passed around between functions. */
    /*private*/ static final class bufinfo_C
    {
        buffer_C    bi_buf;
        file_C      bi_fp;

        /*private*/ bufinfo_C()
        {
        }
    }

    /* used in undo_end() to report number of added and deleted lines */
    /*private*/ static long     u_newcount, u_oldcount;

    /*
     * When 'u' flag included in 'cpoptions', we behave like vi.
     * Need to remember the action that "u" should do.
     */
    /*private*/ static boolean  undo_undoes;

    /*private*/ static int      lastmark;

    /*
     * Save the current line for both the "u" and "U" command.
     * Careful: may trigger autocommands that reload the buffer.
     * Returns true or false.
     */
    /*private*/ static boolean u_save_cursor()
    {
        return u_save(curwin.w_cursor.lnum - 1, curwin.w_cursor.lnum + 1);
    }

    /*
     * Save the lines between "top" and "bot" for both the "u" and "U" command.
     * "top" may be 0 and bot may be curbuf.b_ml.ml_line_count + 1.
     * Careful: may trigger autocommands that reload the buffer.
     * Returns false when lines could not be saved, true otherwise.
     */
    /*private*/ static boolean u_save(long top, long bot)
    {
        if (undo_off)
            return true;

        if (curbuf.b_ml.ml_line_count < top || bot <= top || curbuf.b_ml.ml_line_count + 1 < bot)
            return false;   /* rely on caller to do error messages */

        if (top + 2 == bot)
            u_saveline(top + 1);

        return u_savecommon(top, bot, 0, false);
    }

    /*
     * Save the line "lnum" (used by ":s" and "~" command).
     * The line is replaced, so the new bottom line is lnum + 1.
     * Careful: may trigger autocommands that reload the buffer.
     * Returns false when lines could not be saved, true otherwise.
     */
    /*private*/ static boolean u_savesub(long lnum)
    {
        if (undo_off)
            return true;

        return u_savecommon(lnum - 1, lnum + 1, lnum + 1, false);
    }

    /*
     * A new line is inserted before line "lnum" (used by :s command).
     * The line is inserted, so the new bottom line is lnum + 1.
     * Careful: may trigger autocommands that reload the buffer.
     * Returns false when lines could not be saved, true otherwise.
     */
    /*private*/ static boolean u_inssub(long lnum)
    {
        if (undo_off)
            return true;

        return u_savecommon(lnum - 1, lnum, lnum + 1, false);
    }

    /*
     * Save the lines "lnum" - "lnum" + nlines (used by delete command).
     * The lines are deleted, so the new bottom line is lnum, unless the buffer becomes empty.
     * Careful: may trigger autocommands that reload the buffer.
     * Returns false when lines could not be saved, true otherwise.
     */
    /*private*/ static boolean u_savedel(long lnum, long nlines)
    {
        if (undo_off)
            return true;

        return u_savecommon(lnum - 1, lnum + nlines, (nlines == curbuf.b_ml.ml_line_count) ? 2 : lnum, false);
    }

    /*
     * Return true when undo is allowed.
     * Otherwise give an error message and return false.
     */
    /*private*/ static boolean undo_allowed()
    {
        /* Don't allow changes when 'modifiable' is off. */
        if (!curbuf.b_p_ma[0])
        {
            emsg(e_modifiable);
            return false;
        }

        /* In the sandbox it's not allowed to change the text. */
        if (sandbox != 0)
        {
            emsg(e_sandbox);
            return false;
        }

        /* Don't allow changes in the buffer while editing the cmdline.
         * The caller of getcmdline() may get confused. */
        if (textlock != 0)
        {
            emsg(e_secure);
            return false;
        }

        return true;
    }

    /*
     * Get the undolevle value for the current buffer.
     */
    /*private*/ static long get_undolevel()
    {
        if (curbuf.b_p_ul[0] == NO_LOCAL_UNDOLEVEL)
            return p_ul[0];

        return curbuf.b_p_ul[0];
    }

    /*
     * Common code for various ways to save text before a change.
     * "top" is the line above the first changed line.
     * "bot" is the line below the last changed line.
     * "newbot" is the new bottom line.  Use zero when not known.
     * "reload" is true when saving for a buffer reload.
     * Careful: may trigger autocommands that reload the buffer.
     * Returns false when lines could not be saved, true otherwise.
     */
    /*private*/ static boolean u_savecommon(long top, long bot, long newbot, boolean reload)
    {
        if (!reload)
        {
            /* When making changes is not allowed, return false.
             * It's a crude way to make all change commands fail. */
            if (!undo_allowed())
                return false;

            /*
             * Saving text for undo means we are going to make a change.
             * Give a warning for a read-only file before making the change,
             * so that the FileChangedRO event can replace the buffer with
             * a read-write version (e.g., obtained from a source control system).
             */
            change_warning(0);
            if (curbuf.b_ml.ml_line_count + 1 < bot)
            {
                /* This happens when the FileChangedRO autocommand changes
                 * the file in a way it becomes shorter. */
                emsg(u8("E881: Line count changed unexpectedly"));
                return false;
            }
        }

        long size = bot - top - 1;

        /*
         * If curbuf.b_u_synced == true make a new header.
         */
        if (curbuf.b_u_synced)
        {
            /* Need to create new entry in b_changelist. */
            curbuf.b_new_change = true;

            u_header_C uhp;
            if (0 <= get_undolevel())
                uhp = new u_header_C();
            else
                uhp = null;

            /*
             * If we undid more than we redid, move the entry lists before
             * and including curbuf.b_u_curhead to an alternate branch.
             */
            u_header_C[] old_curhead = { curbuf.b_u_curhead };
            if (old_curhead[0] != null)
            {
                curbuf.b_u_newhead = old_curhead[0].uh_next.ptr;
                curbuf.b_u_curhead = null;
            }

            /*
             * free headers to keep the size right
             */
            while (get_undolevel() < curbuf.b_u_numhead && curbuf.b_u_oldhead != null)
            {
                u_header_C uhfree = curbuf.b_u_oldhead;

                if (uhfree == old_curhead[0])
                    /* Can't reconnect the branch, delete all of it. */
                    u_freebranch(curbuf, uhfree, old_curhead);
                else if (uhfree.uh_alt_next.ptr == null)
                    /* There is no branch, only free one header. */
                    u_freeheader(curbuf, uhfree, old_curhead);
                else
                {
                    /* Free the oldest alternate branch as a whole. */
                    while (uhfree.uh_alt_next.ptr != null)
                        uhfree = uhfree.uh_alt_next.ptr;
                    u_freebranch(curbuf, uhfree, old_curhead);
                }
            }

            if (uhp == null)                /* no undo at all */
            {
                if (old_curhead[0] != null)
                    u_freebranch(curbuf, old_curhead[0], null);
                curbuf.b_u_synced = false;
                return true;
            }

            uhp.uh_prev.ptr = null;
            uhp.uh_next.ptr = curbuf.b_u_newhead;
            uhp.uh_alt_next.ptr = old_curhead[0];
            if (old_curhead[0] != null)
            {
                uhp.uh_alt_prev.ptr = old_curhead[0].uh_alt_prev.ptr;
                if (uhp.uh_alt_prev.ptr != null)
                    uhp.uh_alt_prev.ptr.uh_alt_next.ptr = uhp;
                old_curhead[0].uh_alt_prev.ptr = uhp;
                if (curbuf.b_u_oldhead == old_curhead[0])
                    curbuf.b_u_oldhead = uhp;
            }
            else
                uhp.uh_alt_prev.ptr = null;
            if (curbuf.b_u_newhead != null)
                curbuf.b_u_newhead.uh_prev.ptr = uhp;

            uhp.uh_seq = ++curbuf.b_u_seq_last;
            curbuf.b_u_seq_cur = uhp.uh_seq;
            uhp.uh_time = libC._time();
            uhp.uh_save_nr = 0;
            curbuf.b_u_time_cur = uhp.uh_time + 1;

            uhp.uh_walk = 0;
            uhp.uh_entry = null;
            uhp.uh_getbot_entry = null;
            COPY_pos(uhp.uh_cursor, curwin.w_cursor); /* save cursor pos. for undo */
            if (virtual_active() && 0 < curwin.w_cursor.coladd)
                uhp.uh_cursor_vcol = getviscol();
            else
                uhp.uh_cursor_vcol = -1;

            /* save changed and buffer empty flag for undo */
            uhp.uh_flags = (curbuf.b_changed[0] ? UH_CHANGED : 0) +
                           ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0 ? UH_EMPTYBUF : 0);

            /* save named marks and Visual marks for undo */
            for (int i = 0; i < NMARKS; i++)
                COPY_pos(uhp.uh_namedm[i], curbuf.b_namedm[i]);
            COPY_visualinfo(uhp.uh_visual, curbuf.b_visual);

            curbuf.b_u_newhead = uhp;
            if (curbuf.b_u_oldhead == null)
                curbuf.b_u_oldhead = uhp;
            curbuf.b_u_numhead++;
        }
        else
        {
            if (get_undolevel() < 0)        /* no undo at all */
                return true;

            /*
             * When saving a single line, and it has been saved just before, it
             * doesn't make sense saving it again.  Saves a lot of memory when
             * making lots of changes inside the same line.
             * This is only possible if the previous change didn't increase or
             * decrease the number of lines.
             * Check the ten last changes.  More doesn't make sense and takes too long.
             */
            if (size == 1)
            {
                u_entry_C uep = u_get_headentry();
                u_entry_C prev_uep = null;
                for (int i = 0; i < 10; i++)
                {
                    if (uep == null)
                        break;

                    /* If lines have been inserted/deleted we give up.
                     * Also when the line was included in a multi-line save. */
                    if ((curbuf.b_u_newhead.uh_getbot_entry != uep
                                ? (uep.ue_top + uep.ue_size + 1
                                    != (uep.ue_bot == 0
                                        ? curbuf.b_ml.ml_line_count + 1
                                        : uep.ue_bot))
                                : uep.ue_lcount != curbuf.b_ml.ml_line_count)
                            || (1 < uep.ue_size
                                && uep.ue_top <= top
                                && top + 2 <= uep.ue_top + uep.ue_size + 1))
                        break;

                    /* If it's the same line we can skip saving it again. */
                    if (uep.ue_size == 1 && uep.ue_top == top)
                    {
                        if (0 < i)
                        {
                            /* It's not the last entry: get ue_bot for the last entry now.
                             * Following deleted/inserted lines go to the re-used entry. */
                            u_getbot();
                            curbuf.b_u_synced = false;

                            /* Move the found entry to become the last entry.
                             * The order of undo/redo doesn't matter for the entries we move it over,
                             * since they don't change the line count and don't include this line.
                             * It does matter for the found entry if the line count is changed
                             * by the executed command. */
                            prev_uep.ue_next = uep.ue_next;
                            uep.ue_next = curbuf.b_u_newhead.uh_entry;
                            curbuf.b_u_newhead.uh_entry = uep;
                        }

                        /* The executed command may change the line count. */
                        if (newbot != 0)
                            uep.ue_bot = newbot;
                        else if (curbuf.b_ml.ml_line_count < bot)
                            uep.ue_bot = 0;
                        else
                        {
                            uep.ue_lcount = curbuf.b_ml.ml_line_count;
                            curbuf.b_u_newhead.uh_getbot_entry = uep;
                        }
                        return true;
                    }
                    prev_uep = uep;
                    uep = uep.ue_next;
                }
            }

            /* find line number for ue_bot for previous u_save() */
            u_getbot();
        }

        /*
         * add lines in front of entry list
         */
        u_entry_C uep = new u_entry_C();

        uep.ue_size = size;
        uep.ue_top = top;
        if (newbot != 0)
            uep.ue_bot = newbot;
        /*
         * Use 0 for ue_bot if bot is below last line.
         * Otherwise we have to compute ue_bot later.
         */
        else if (curbuf.b_ml.ml_line_count < bot)
            uep.ue_bot = 0;
        else
        {
            uep.ue_lcount = curbuf.b_ml.ml_line_count;
            curbuf.b_u_newhead.uh_getbot_entry = uep;
        }

        if (0 < size)
        {
            uep.ue_array = new Bytes[(int)size];
            long lnum = top + 1;
            for (int i = 0; i < size; i++)
            {
                fast_breakcheck();
                if (got_int)
                    return false;
                uep.ue_array[i] = STRDUP(ml_get(lnum++));
            }
        }
        else
            uep.ue_array = null;
        uep.ue_next = curbuf.b_u_newhead.uh_entry;
        curbuf.b_u_newhead.uh_entry = uep;
        curbuf.b_u_synced = false;
        undo_undoes = false;

        return true;
    }

    /*private*/ static final Bytes UF_START_MAGIC     = u8("Vim\237UnDo\345");    /* magic at start of undofile */
    /*private*/ static final int UF_START_MAGIC_LEN     = 9;
    /*private*/ static final int UF_HEADER_MAGIC        = 0x5fd0;           /* magic at start of header */
    /*private*/ static final int UF_HEADER_END_MAGIC    = 0xe7aa;           /* magic after last header */
    /*private*/ static final int UF_ENTRY_MAGIC         = 0xf518;           /* magic at start of entry */
    /*private*/ static final int UF_ENTRY_END_MAGIC     = 0x3581;           /* magic after last entry */
    /*private*/ static final int UF_VERSION             = 2;                /* 2-byte undofile version number */
    /*private*/ static final int UF_VERSION_CRYPT       = 0x8002;           /* idem, encrypted */

    /* extra fields for header */
    /*private*/ static final int UF_LAST_SAVE_NR        = 1;

    /* extra fields for uhp */
    /*private*/ static final int UHP_SAVE_NR            = 1;

    /*private*/ static Bytes e_not_open = u8("E828: Cannot open undo file for writing: %s");

    /*
     * Compute the hash for the current buffer text into hash[UNDO_HASH_SIZE].
     */
    /*private*/ static void u_compute_hash(Bytes hash)
    {
        context_sha256_C ctx = new context_sha256_C();

        sha256_start(ctx);
        for (long lnum = 1; lnum <= curbuf.b_ml.ml_line_count; lnum++)
        {
            Bytes p = ml_get(lnum);
            sha256_update(ctx, p, strlen(p) + 1);
        }
        sha256_finish(ctx, hash);
    }

    /*
     * Return an allocated string of the full path of the target undofile.
     * When "reading" is true find the file to read, go over all directories in 'undodir'.
     * When "reading" is false use the first name where the directory exists.
     * Returns null when there is no place to write or no file to read.
     */
    /*private*/ static Bytes u_get_undo_file_name(Bytes buf_ffname, boolean reading)
    {
        Bytes dir_name = new Bytes(IOSIZE + 1);
        Bytes munged_name = null;
        Bytes undo_file_name = null;
        Bytes ffname = buf_ffname;
        Bytes fname_buf = new Bytes(MAXPATHL);

        if (ffname == null)
            return null;

        /* Expand symlink in the file name, so that we put the undo file
         * with the actual file instead of with the symlink. */
        if (resolve_symlink(ffname, fname_buf) == true)
            ffname = fname_buf;

        /* Loop over 'undodir'.  When reading find the first file that exists.
         * When not reading use the first directory that exists or ".". */
        Bytes[] dirp = { p_udir[0] };
        while (dirp[0].at(0) != NUL)
        {
            int dir_len = copy_option_part(dirp, dir_name, IOSIZE, u8(","));
            if (dir_len == 1 && dir_name.at(0) == (byte)'.')
            {
                /* Use same directory as ffname: "dir/name" -> "dir/.name.un~". */
                undo_file_name = STRNDUP(ffname, strlen(ffname) + 5);

                Bytes p = gettail(undo_file_name);
                BCOPY(p, 1, p, 0, strlen(p) + 1);
                p.be(0, (byte)'.');
                STRCAT(p, u8(".un~"));
            }
            else
            {
                dir_name.be(dir_len, NUL);
                if (mch_isdir(dir_name))
                {
                    if (munged_name == null)
                    {
                        munged_name = STRDUP(ffname);

                        for (Bytes p = munged_name; p.at(0) != NUL; p = p.plus(us_ptr2len_cc(p)))
                            if (vim_ispathsep(p.at(0)))
                                p.be(0, (byte)'%');
                    }
                    undo_file_name = concat_fnames(dir_name, munged_name, true);
                }
            }

            /* When reading check if the file exists. */
            stat_C st = new stat_C();
            if (undo_file_name != null && (!reading || 0 <= libC.stat(undo_file_name, st)))
                break;
            undo_file_name = null;
        }

        return undo_file_name;
    }

    /*private*/ static void corruption_error(Bytes mesg, Bytes file_name)
    {
        emsg3(u8("E825: Corrupted undo file (%s): %s"), mesg, file_name);
    }

    /*
     * Write a sequence of bytes to the undo file.
     * Buffers and encrypts as needed.
     * Returns true or false.
     */
    /*private*/ static boolean undo_write(bufinfo_C bi, Bytes ptr, int len)
    {
        return (libC.fwrite(ptr, len, 1, bi.bi_fp) == 1);
    }

    /*
     * Write "ptr[len]" and crypt the bytes when needed.
     * Returns true or false.
     */
    /*private*/ static boolean fwrite_crypt(bufinfo_C bi, Bytes ptr, int len)
    {
        return undo_write(bi, ptr, len);
    }

    /*
     * Write a number, MSB first, in "len" bytes.
     * Must match with undo_read_?c() functions.
     * Returns true or false.
     */
    /*private*/ static boolean undo_write_bytes(bufinfo_C bi, long nr, int len)
    {
        Bytes buf = new Bytes(8);

        for (int i = len - 1, j = 0; 0 <= i; --i)
            buf.be(j++, (byte)((nr >>> (i << 3)) & 0xff));

        return undo_write(bi, buf, len);
    }

    /*
     * Write the pointer to an undo header.
     * Instead of writing the pointer itself we use the sequence number of the header.
     * This is converted back to pointers when reading.
     */
    /*private*/ static boolean put_header_ptr(bufinfo_C bi, u_header_C uhp)
    {
        return undo_write_bytes(bi, (uhp != null) ? uhp.uh_seq : 0, 4);
    }

    /*private*/ static int undo_read_4c(bufinfo_C bi)
    {
        return get4c(bi.bi_fp);
    }

    /*private*/ static int undo_read_2c(bufinfo_C bi)
    {
        return get2c(bi.bi_fp);
    }

    /*private*/ static int undo_read_byte(bufinfo_C bi)
    {
        return libc.getc(bi.bi_fp);
    }

    /*private*/ static long undo_read_time(bufinfo_C bi)
    {
        return get8c(bi.bi_fp);
    }

    /*
     * Read "buffer[size]" from the undo file.
     * Return true or false.
     */
    /*private*/ static boolean undo_read(bufinfo_C bi, Bytes buffer, int size)
    {
        if (libC.fread(buffer, size, 1, bi.bi_fp) != 1)
            return false;

        return true;
    }

    /*
     * Read a string of length "len" from "bi.bi_fd".
     * "len" can be zero to allocate an empty line.
     * Decrypt the bytes if needed.
     * Append a NUL.
     * Returns a pointer to allocated memory or null for failure.
     */
    /*private*/ static Bytes read_string_decrypt(bufinfo_C bi, int len)
    {
        Bytes ptr = new Bytes(len + 1);

        if (0 < len && undo_read(bi, ptr, len) == false)
            return null;

        ptr.be(len, NUL);

        return ptr;
    }

    /*
     * Writes the (not encrypted) header and initializes encryption if needed.
     */
    /*private*/ static boolean serialize_header(bufinfo_C bi, Bytes hash)
    {
        buffer_C buf = bi.bi_buf;
        file_C fp = bi.bi_fp;

        /* Start writing, first the magic marker and undo info version. */
        if (libC.fwrite(UF_START_MAGIC, UF_START_MAGIC_LEN, 1, fp) != 1)
            return false;

        /* If the buffer is encrypted then all text bytes following will be
         * encrypted.  Numbers and other info is not crypted. */
        undo_write_bytes(bi, UF_VERSION, 2);

        /* Write a hash of the buffer text, so that we can verify if it is
         * still the same when reading the buffer text. */
        if (undo_write(bi, hash, UNDO_HASH_SIZE) == false)
            return false;

        /* buffer-specific data */
        undo_write_bytes(bi, buf.b_ml.ml_line_count, 4);
        int len = (buf.b_u_line_ptr != null) ? strlen(buf.b_u_line_ptr) : 0;
        undo_write_bytes(bi, len, 4);
        if (0 < len && fwrite_crypt(bi, buf.b_u_line_ptr, len) == false)
            return false;
        undo_write_bytes(bi, buf.b_u_line_lnum, 4);
        undo_write_bytes(bi, buf.b_u_line_colnr, 4);

        /* Undo structures header data. */
        put_header_ptr(bi, buf.b_u_oldhead);
        put_header_ptr(bi, buf.b_u_newhead);
        put_header_ptr(bi, buf.b_u_curhead);

        undo_write_bytes(bi, buf.b_u_numhead, 4);
        undo_write_bytes(bi, buf.b_u_seq_last, 4);
        undo_write_bytes(bi, buf.b_u_seq_cur, 4);
        undo_write_bytes(bi, buf.b_u_time_cur, 8);

        /* Optional fields. */
        undo_write_bytes(bi, 4, 1);
        undo_write_bytes(bi, UF_LAST_SAVE_NR, 1);
        undo_write_bytes(bi, buf.b_u_save_nr_last, 4);

        undo_write_bytes(bi, 0, 1);     /* end marker */

        return true;
    }

    /*private*/ static boolean serialize_uhp(bufinfo_C bi, u_header_C uhp)
    {
        if (undo_write_bytes(bi, UF_HEADER_MAGIC, 2) == false)
            return false;

        put_header_ptr(bi, uhp.uh_next.ptr);
        put_header_ptr(bi, uhp.uh_prev.ptr);
        put_header_ptr(bi, uhp.uh_alt_next.ptr);
        put_header_ptr(bi, uhp.uh_alt_prev.ptr);
        undo_write_bytes(bi, uhp.uh_seq, 4);
        serialize_pos(bi, uhp.uh_cursor);
        undo_write_bytes(bi, uhp.uh_cursor_vcol, 4);
        undo_write_bytes(bi, uhp.uh_flags, 2);
        /* Assume NMARKS will stay the same. */
        for (int i = 0; i < NMARKS; i++)
            serialize_pos(bi, uhp.uh_namedm[i]);
        serialize_visualinfo(bi, uhp.uh_visual);
        undo_write_bytes(bi, uhp.uh_time, 8);

        /* Optional fields. */
        undo_write_bytes(bi, 4, 1);
        undo_write_bytes(bi, UHP_SAVE_NR, 1);
        undo_write_bytes(bi, uhp.uh_save_nr, 4);

        undo_write_bytes(bi, 0, 1);     /* end marker */

        /* Write all the entries. */
        for (u_entry_C uep = uhp.uh_entry; uep != null; uep = uep.ue_next)
        {
            undo_write_bytes(bi, UF_ENTRY_MAGIC, 2);
            if (serialize_uep(bi, uep) == false)
                return false;
        }
        undo_write_bytes(bi, UF_ENTRY_END_MAGIC, 2);
        return true;
    }

    /*private*/ static u_header_C unserialize_uhp(bufinfo_C bi, Bytes file_name)
    {
        u_header_C uhp = new u_header_C();

        uhp.uh_next.seq = undo_read_4c(bi);
        uhp.uh_prev.seq = undo_read_4c(bi);
        uhp.uh_alt_next.seq = undo_read_4c(bi);
        uhp.uh_alt_prev.seq = undo_read_4c(bi);
        uhp.uh_seq = undo_read_4c(bi);
        if (uhp.uh_seq <= 0)
        {
            corruption_error(u8("uh_seq"), file_name);
            return null;
        }

        unserialize_pos(bi, uhp.uh_cursor);
        uhp.uh_cursor_vcol = undo_read_4c(bi);
        uhp.uh_flags = undo_read_2c(bi);
        for (int i = 0; i < NMARKS; i++)
            unserialize_pos(bi, uhp.uh_namedm[i]);
        unserialize_visualinfo(bi, uhp.uh_visual);
        uhp.uh_time = undo_read_time(bi);

        /* Optional fields. */
        for ( ; ; )
        {
            int len = undo_read_byte(bi);
            if (len == 0)
                break;

            int what = undo_read_byte(bi);
            switch (what)
            {
                case UHP_SAVE_NR:
                    uhp.uh_save_nr = undo_read_4c(bi);
                    break;

                default:
                    /* field not supported, skip */
                    while (0 <= --len)
                        undo_read_byte(bi);
                    break;
            }
        }

        /* Unserialize the uep list. */
        int c;
        for (u_entry_C last_uep = null; (c = undo_read_2c(bi)) == UF_ENTRY_MAGIC; )
        {
            boolean[] error = { false };
            u_entry_C uep = unserialize_uep(bi, error, file_name);
            if (last_uep == null)
                uhp.uh_entry = uep;
            else
                last_uep.ue_next = uep;
            last_uep = uep;
            if (uep == null || error[0])
                return null;
        }
        if (c != UF_ENTRY_END_MAGIC)
        {
            corruption_error(u8("entry end"), file_name);
            return null;
        }

        return uhp;
    }

    /*
     * Serialize "uep".
     */
    /*private*/ static boolean serialize_uep(bufinfo_C bi, u_entry_C uep)
    {
        undo_write_bytes(bi, uep.ue_top, 4);
        undo_write_bytes(bi, uep.ue_bot, 4);
        undo_write_bytes(bi, uep.ue_lcount, 4);
        undo_write_bytes(bi, uep.ue_size, 4);
        for (int i = 0; i < uep.ue_size; i++)
        {
            int len = strlen(uep.ue_array[i]);

            if (undo_write_bytes(bi, len, 4) == false)
                return false;
            if (0 < len && fwrite_crypt(bi, uep.ue_array[i], len) == false)
                return false;
        }
        return true;
    }

    /*private*/ static u_entry_C unserialize_uep(bufinfo_C bi, boolean[] error, Bytes file_name)
    {
        u_entry_C uep = new u_entry_C();

        uep.ue_top = undo_read_4c(bi);
        uep.ue_bot = undo_read_4c(bi);
        uep.ue_lcount = undo_read_4c(bi);
        uep.ue_size = undo_read_4c(bi);
        Bytes[] array;
        if (0 < uep.ue_size)
            array = new Bytes[(int)uep.ue_size];
        else
            array = null;
        uep.ue_array = array;

        for (int i = 0; i < uep.ue_size; i++)
        {
            Bytes line;
            int line_len = undo_read_4c(bi);
            if (0 <= line_len)
                line = read_string_decrypt(bi, line_len);
            else
            {
                line = null;
                corruption_error(u8("line length"), file_name);
            }
            if (line == null)
            {
                error[0] = true;
                return uep;
            }
            array[i] = line;
        }

        return uep;
    }

    /*
     * Serialize "pos".
     */
    /*private*/ static void serialize_pos(bufinfo_C bi, pos_C pos)
    {
        undo_write_bytes(bi, pos.lnum, 4);
        undo_write_bytes(bi, pos.col, 4);
        undo_write_bytes(bi, pos.coladd, 4);
    }

    /*
     * Unserialize the pos_C at the current position.
     */
    /*private*/ static void unserialize_pos(bufinfo_C bi, pos_C pos)
    {
        pos.lnum = undo_read_4c(bi);
        if (pos.lnum < 0)
            pos.lnum = 0;
        pos.col = undo_read_4c(bi);
        if (pos.col < 0)
            pos.col = 0;
        pos.coladd = undo_read_4c(bi);
        if (pos.coladd < 0)
            pos.coladd = 0;
    }

    /*
     * Serialize "info".
     */
    /*private*/ static void serialize_visualinfo(bufinfo_C bi, visualinfo_C vi)
    {
        serialize_pos(bi, vi.vi_start);
        serialize_pos(bi, vi.vi_end);
        undo_write_bytes(bi, vi.vi_mode, 4);
        undo_write_bytes(bi, vi.vi_curswant, 4);
    }

    /*
     * Unserialize the visualinfo_C at the current position.
     */
    /*private*/ static void unserialize_visualinfo(bufinfo_C bi, visualinfo_C vi)
    {
        unserialize_pos(bi, vi.vi_start);
        unserialize_pos(bi, vi.vi_end);
        vi.vi_mode = undo_read_4c(bi);
        vi.vi_curswant = undo_read_4c(bi);
    }

    /*
     * Write the undo tree in an undo file.
     * When "name" is not null, use it as the name of the undo file.
     * Otherwise use buf.b_ffname to generate the undo file name.
     * "buf" must never be null, buf.b_ffname is used to obtain the original file permissions.
     * "forceit" is true for ":wundo!", false otherwise.
     * "hash[UNDO_HASH_SIZE]" must be the hash value of the buffer text.
     */
    /*private*/ static void u_write_undo(Bytes name, boolean forceit, buffer_C buf, Bytes hash)
    {
        boolean write_ok = false;

        Bytes file_name;
        if (name == null)
        {
            file_name = u_get_undo_file_name(buf.b_ffname, false);
            if (file_name == null)
            {
                if (0 < p_verbose[0])
                {
                    verbose_enter();
                    smsg(u8("Cannot write undo file in any directory in 'undodir'"));
                    verbose_leave();
                }
                return;
            }
        }
        else
            file_name = name;

        bufinfo_C bi = new bufinfo_C();

        boolean st_old_valid = false;
        stat_C st_old = new stat_C();
        /*
         * Decide about the permission to use for the undo file.
         * If the buffer has a name use the permission of the original file.
         * Otherwise only allow the user to access the undo file.
         */
        int perm = 0600;
        if (buf.b_ffname != null)
        {
            if (0 <= libC.stat(buf.b_ffname, st_old))
            {
                perm = st_old.st_mode();
                st_old_valid = true;
            }
        }

        /* strip any s-bit */
        perm = (perm & 0777);

        /* If the undo file already exists,
         * verify that it actually is an undo file,
         * and delete it. */
        if (0 <= mch_getperm(file_name))
        {
            if (name == null || !forceit)
            {
                /* Check we can read it and it's an undo file. */
                int fd = libC.open(file_name, O_RDONLY, 0);
                if (fd < 0)
                {
                    if (name != null || 0 < p_verbose[0])
                    {
                        if (name == null)
                            verbose_enter();
                        smsg(u8("Will not overwrite with undo file, cannot read: %s"), file_name);
                        if (name == null)
                            verbose_leave();
                    }
                    return;
                }
                else
                {
                    Bytes mbuf = new Bytes(UF_START_MAGIC_LEN);
                    int len = read_eintr(fd, mbuf, UF_START_MAGIC_LEN);
                    libc.close(fd);
                    if (len < UF_START_MAGIC_LEN || MEMCMP(mbuf, UF_START_MAGIC, UF_START_MAGIC_LEN) != 0)
                    {
                        if (name != null || 0 < p_verbose[0])
                        {
                            if (name == null)
                                verbose_enter();
                            smsg(u8("Will not overwrite, this is not an undo file: %s"), file_name);
                            if (name == null)
                                verbose_leave();
                        }
                        return;
                    }
                }
            }
            libC.unlink(file_name);
        }

        /* If there is no undo information at all, quit here after deleting any existing undo file. */
        if (buf.b_u_numhead == 0 && buf.b_u_line_ptr == null)
        {
            if (0 < p_verbose[0])
                verb_msg(u8("Skipping undo file write, nothing to undo"));
            return;
        }

        int fd = libC.open(file_name, O_CREAT|O_WRONLY|O_EXCL|O_NOFOLLOW, perm);
        if (fd < 0)
        {
            emsg2(e_not_open, file_name);
            return;
        }
        mch_setperm(file_name, perm);
        if (0 < p_verbose[0])
        {
            verbose_enter();
            smsg(u8("Writing undo file: %s"), file_name);
            verbose_leave();
        }

        /*
         * Try to set the group of the undo file same as the original file.
         * If this fails, set the protection bits for the group same as the
         * protection bits for others.
         */
        stat_C st_new = new stat_C();
        if (st_old_valid
                && 0 <= libC.stat(file_name, st_new)
                && st_new.st_gid() != st_old.st_gid()
                && libc.fchown(fd, -1, st_old.st_gid()) != 0)
            mch_setperm(file_name, (perm & 0707) | ((perm & 07) << 3));

        file_C fp = libC.fdopen(fd, u8("w"));
        if (fp == null)
        {
            emsg2(e_not_open, file_name);
            libc.close(fd);
            libC.unlink(file_name);
            return;
        }

        /* Undo must be synced. */
        u_sync(true);

        write_error:
        {
            /*
             * Write the header.  Initializes encryption, if enabled.
             */
            bi.bi_buf = buf;
            bi.bi_fp = fp;
            if (serialize_header(bi, hash) == false)
                break write_error;

            /*
             * Iteratively serialize UHPs and their UEPs from the top down.
             */
            int mark = ++lastmark;
            u_header_C uhp = buf.b_u_oldhead;
            while (uhp != null)
            {
                /* Serialize current UHP if we haven't seen it. */
                if (uhp.uh_walk != mark)
                {
                    uhp.uh_walk = mark;
                    if (serialize_uhp(bi, uhp) == false)
                        break write_error;
                }

                /* Now walk through the tree - algorithm from undo_time(). */
                if (uhp.uh_prev.ptr != null && uhp.uh_prev.ptr.uh_walk != mark)
                    uhp = uhp.uh_prev.ptr;
                else if (uhp.uh_alt_next.ptr != null && uhp.uh_alt_next.ptr.uh_walk != mark)
                    uhp = uhp.uh_alt_next.ptr;
                else if (uhp.uh_next.ptr != null && uhp.uh_alt_prev.ptr == null && uhp.uh_next.ptr.uh_walk != mark)
                    uhp = uhp.uh_next.ptr;
                else if (uhp.uh_alt_prev.ptr != null)
                    uhp = uhp.uh_alt_prev.ptr;
                else
                    uhp = uhp.uh_next.ptr;
            }

            if (undo_write_bytes(bi, UF_HEADER_END_MAGIC, 2) == true)
                write_ok = true;
        }

        libc.fclose(fp);
        if (!write_ok)
            emsg2(u8("E829: write error in undo file: %s"), file_name);
    }

    /*
     * Load the undo tree from an undo file.
     * If "name" is not null use it as the undo file name.
     * This also means being a bit more verbose.
     * Otherwise use curbuf.b_ffname to generate the undo file name.
     * "hash[UNDO_HASH_SIZE]" must be the hash value of the buffer text.
     */
    /*private*/ static void u_read_undo(Bytes name, Bytes hash, Bytes orig_name)
    {
        Bytes line_ptr = null;
        u_header_C[] uhp_table = null;
        int num_read_uhps = 0;

        Bytes file_name;
        if (name == null)
        {
            file_name = u_get_undo_file_name(curbuf.b_ffname, true);
            if (file_name == null)
                return;

            /* For safety we only read an undo file if the owner is equal
             * to the owner of the text file or equal to the current user. */
            stat_C st_orig = new stat_C(), st_undo = new stat_C();
            if (0 <= libC.stat(orig_name, st_orig) && 0 <= libC.stat(file_name, st_undo)
                   && st_orig.st_uid() != st_undo.st_uid() && st_undo.st_uid() != libc.getuid())
            {
                if (0 < p_verbose[0])
                {
                    verbose_enter();
                    smsg(u8("Not reading undo file, owner differs: %s"), file_name);
                    verbose_leave();
                }
                return;
            }
        }
        else
            file_name = name;

        if (0 < p_verbose[0])
        {
            verbose_enter();
            smsg(u8("Reading undo file: %s"), file_name);
            verbose_leave();
        }

        bufinfo_C bi = new bufinfo_C();

        file_C fp = libC.fopen(file_name, u8("r"));
        if (fp == null)
        {
            if (name != null || 0 < p_verbose[0])
                emsg2(u8("E822: Cannot open undo file for reading: %s"), file_name);
            return;
        }
        bi.bi_buf = curbuf;
        bi.bi_fp = fp;

        theend:
        {
            /*
             * Read the undo file header.
             */
            Bytes magic_buf = new Bytes(UF_START_MAGIC_LEN);
            if (libC.fread(magic_buf, UF_START_MAGIC_LEN, 1, fp) != 1
                        || MEMCMP(magic_buf, UF_START_MAGIC, UF_START_MAGIC_LEN) != 0)
            {
                emsg2(u8("E823: Not an undo file: %s"), file_name);
                break theend;
            }
            long version = get2c(fp);
            if (version == UF_VERSION_CRYPT)
            {
                emsg2(u8("E827: Undo file is encrypted: %s"), file_name);
                break theend;
            }
            else if (version != UF_VERSION)
            {
                emsg2(u8("E824: Incompatible undo file: %s"), file_name);
                break theend;
            }

            Bytes read_hash = new Bytes(UNDO_HASH_SIZE);
            if (undo_read(bi, read_hash, UNDO_HASH_SIZE) == false)
            {
                corruption_error(u8("hash"), file_name);
                break theend;
            }

            long line_count = undo_read_4c(bi);
            if (MEMCMP(hash, read_hash, UNDO_HASH_SIZE) != 0 || line_count != curbuf.b_ml.ml_line_count)
            {
                if (0 < p_verbose[0] || name != null)
                {
                    if (name == null)
                        verbose_enter();
                    give_warning(u8("File contents changed, cannot use undo info"), true);
                    if (name == null)
                        verbose_leave();
                }
                break theend;
            }

            /* Read undo data for "U" command. */
            int str_len = undo_read_4c(bi);
            if (str_len < 0)
                break theend;
            if (0 < str_len)
                line_ptr = read_string_decrypt(bi, str_len);

            long line_lnum = undo_read_4c(bi);
            int line_colnr = undo_read_4c(bi);
            if (line_lnum < 0 || line_colnr < 0)
            {
                corruption_error(u8("line lnum/col"), file_name);
                break theend;
            }

            /* Begin general undo data. */
            long old_header_seq = undo_read_4c(bi);
            long new_header_seq = undo_read_4c(bi);
            long cur_header_seq = undo_read_4c(bi);
            int num_head = undo_read_4c(bi);
            long seq_last = undo_read_4c(bi);
            long seq_cur = undo_read_4c(bi);
            long seq_time = undo_read_time(bi);

            long last_save_nr = 0;
            /* Optional header fields. */
            for ( ; ; )
            {
                int len = undo_read_byte(bi);
                if (len == 0 || len == EOF)
                    break;

                int what = undo_read_byte(bi);
                switch (what)
                {
                    case UF_LAST_SAVE_NR:
                        last_save_nr = undo_read_4c(bi);
                        break;

                    default:
                        /* field not supported, skip */
                        while (0 <= --len)
                            undo_read_byte(bi);
                        break;
                }
            }

            /* uhp_table will store the freshly created undo headers we allocate
             * until we insert them into curbuf.  The table remains sorted by the
             * sequence numbers of the headers.
             * When there are no headers uhp_table is null. */
            if (0 < num_head)
                uhp_table = new u_header_C[num_head];

            int c;
            while ((c = undo_read_2c(bi)) == UF_HEADER_MAGIC)
            {
                if (num_head <= num_read_uhps)
                {
                    corruption_error(u8("num_head too small"), file_name);
                    break theend;
                }

                u_header_C uhp = unserialize_uhp(bi, file_name);
                if (uhp == null)
                    break theend;
                uhp_table[num_read_uhps++] = uhp;
            }

            if (num_read_uhps != num_head)
            {
                corruption_error(u8("num_head"), file_name);
                break theend;
            }
            if (c != UF_HEADER_END_MAGIC)
            {
                corruption_error(u8("end marker"), file_name);
                break theend;
            }

            int old_idx = -1, new_idx = -1, cur_idx = -1;

            /* We have put all of the headers into a table.  Now we iterate through the
             * table and swizzle each sequence number we have stored in uh_*_seq into
             * a pointer corresponding to the header with that sequence number. */
            for (int i = 0; i < num_head; i++)
            {
                u_header_C uhp = uhp_table[i];
                if (uhp == null)
                    continue;
                for (int j = 0; j < num_head; j++)
                    if (uhp_table[j] != null && i != j && uhp_table[i].uh_seq == uhp_table[j].uh_seq)
                    {
                        corruption_error(u8("duplicate uh_seq"), file_name);
                        break theend;
                    }
                for (int j = 0; j < num_head; j++)
                    if (uhp_table[j] != null && uhp_table[j].uh_seq == uhp.uh_next.seq)
                    {
                        uhp.uh_next.ptr = uhp_table[j];
                        break;
                    }
                for (int j = 0; j < num_head; j++)
                    if (uhp_table[j] != null && uhp_table[j].uh_seq == uhp.uh_prev.seq)
                    {
                        uhp.uh_prev.ptr = uhp_table[j];
                        break;
                    }
                for (int j = 0; j < num_head; j++)
                    if (uhp_table[j] != null && uhp_table[j].uh_seq == uhp.uh_alt_next.seq)
                    {
                        uhp.uh_alt_next.ptr = uhp_table[j];
                        break;
                    }
                for (int j = 0; j < num_head; j++)
                    if (uhp_table[j] != null && uhp_table[j].uh_seq == uhp.uh_alt_prev.seq)
                    {
                        uhp.uh_alt_prev.ptr = uhp_table[j];
                        break;
                    }
                if (0 < old_header_seq && old_idx < 0 && uhp.uh_seq == old_header_seq)
                    old_idx = i;
                if (0 < new_header_seq && new_idx < 0 && uhp.uh_seq == new_header_seq)
                    new_idx = i;
                if (0 < cur_header_seq && cur_idx < 0 && uhp.uh_seq == cur_header_seq)
                    cur_idx = i;
            }

            /* Now that we have read the undo info successfully,
             * free the current undo info and use the info from the file. */
            u_blockfree(curbuf);
            curbuf.b_u_oldhead = (old_idx < 0) ? null : uhp_table[old_idx];
            curbuf.b_u_newhead = (new_idx < 0) ? null : uhp_table[new_idx];
            curbuf.b_u_curhead = (cur_idx < 0) ? null : uhp_table[cur_idx];
            curbuf.b_u_line_ptr = line_ptr;
            curbuf.b_u_line_lnum = line_lnum;
            curbuf.b_u_line_colnr = line_colnr;
            curbuf.b_u_numhead = num_head;
            curbuf.b_u_seq_last = seq_last;
            curbuf.b_u_seq_cur = seq_cur;
            curbuf.b_u_time_cur = seq_time;
            curbuf.b_u_save_nr_last = last_save_nr;
            curbuf.b_u_save_nr_cur = last_save_nr;

            curbuf.b_u_synced = true;

            if (name != null)
                smsg(u8("Finished reading undo file %s"), file_name);
        }

        if (fp != null)
            libc.fclose(fp);
    }

    /*
     * If 'cpoptions' contains 'u': Undo the previous undo or redo (vi compatible).
     * If 'cpoptions' does not contain 'u': Always undo.
     */
    /*private*/ static void u_undo(int count)
    {
        /*
         * If we get an undo command while executing a macro, we behave like the original vi.
         * If this happens twice in one macro the result will not be compatible.
         */
        if (curbuf.b_u_synced == false)
        {
            u_sync(true);
            count = 1;
        }

        if (vim_strbyte(p_cpo[0], CPO_UNDO) == null)
            undo_undoes = true;
        else
            undo_undoes = !undo_undoes;

        u_doit(count);
    }

    /*
     * If 'cpoptions' contains 'u': Repeat the previous undo or redo.
     * If 'cpoptions' does not contain 'u': Always redo.
     */
    /*private*/ static void u_redo(int count)
    {
        if (vim_strbyte(p_cpo[0], CPO_UNDO) == null)
            undo_undoes = false;

        u_doit(count);
    }

    /*
     * Undo or redo, depending on 'undo_undoes', 'count' times.
     */
    /*private*/ static void u_doit(int startcount)
    {
        if (!undo_allowed())
            return;

        u_newcount = 0;
        u_oldcount = 0;
        if ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0)
            u_oldcount = -1;

        for (int count = startcount; 0 < count--; )
        {
            /* Do the change warning now, so that it triggers FileChangedRO when
             * needed.  This may cause the file to be reloaded, that must happen
             * before we do anything, because it may change curbuf.b_u_curhead
             * and more. */
            change_warning(0);

            if (undo_undoes)
            {
                if (curbuf.b_u_curhead == null)             /* first undo */
                    curbuf.b_u_curhead = curbuf.b_u_newhead;
                else if (0 < get_undolevel())               /* multi level undo */
                    /* get next undo */
                    curbuf.b_u_curhead = curbuf.b_u_curhead.uh_next.ptr;
                /* nothing to undo */
                if (curbuf.b_u_numhead == 0 || curbuf.b_u_curhead == null)
                {
                    /* stick curbuf.b_u_curhead at end */
                    curbuf.b_u_curhead = curbuf.b_u_oldhead;
                    beep_flush();
                    if (count == startcount - 1)
                    {
                        msg(u8("Already at oldest change"));
                        return;
                    }
                    break;
                }

                u_undoredo(true);
            }
            else
            {
                if (curbuf.b_u_curhead == null || get_undolevel() <= 0)
                {
                    beep_flush();   /* nothing to redo */
                    if (count == startcount - 1)
                    {
                        msg(u8("Already at newest change"));
                        return;
                    }
                    break;
                }

                u_undoredo(false);

                /* Advance for next redo.
                 * Set "newhead" when at the end of the redoable changes. */
                if (curbuf.b_u_curhead.uh_prev.ptr == null)
                    curbuf.b_u_newhead = curbuf.b_u_curhead;
                curbuf.b_u_curhead = curbuf.b_u_curhead.uh_prev.ptr;
            }
        }

        u_undo_end(undo_undoes, false);
    }

    /*
     * Undo or redo over the timeline.
     * When "step" is negative go back in time, otherwise goes forward in time.
     * When "sec" is false make "step" steps, when "sec" is true use "step" as seconds.
     * When "file" is true use "step" as a number of file writes.
     * When "absolute" is true use "step" as the sequence number to jump to.
     * "sec" must be false then.
     */
    /*private*/ static void undo_time(long step, boolean sec, boolean file, boolean absolute)
    {
        boolean dosec = sec;
        boolean dofile = file;
        boolean above = false;
        boolean did_undo = true;

        /* First make sure the current undoable change is synced. */
        if (curbuf.b_u_synced == false)
            u_sync(true);

        u_newcount = 0;
        u_oldcount = 0;
        if ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0)
            u_oldcount = -1;

        u_header_C uhp = null;	// %% anno dunno

        long target;
        long closest;
        /* "target" is the node below which we want to be.
         * Init "closest" to a value we can't reach. */
        if (absolute)
        {
            target = step;
            closest = -1;
        }
        else
        {
            /* When doing computations with time_t subtract starttime, because
             * time_t converted to a long may result in a wrong number. */
            if (dosec)
                target = curbuf.b_u_time_cur - starttime + step;
            else if (dofile)
            {
                if (step < 0)
                {
                    /* Going back to a previous write.  If there were changes after
                     * the last write, count that as moving one file-write, so
                     * that ":earlier 1f" undoes all changes since the last save. */
                    uhp = curbuf.b_u_curhead;
                    if (uhp != null)
                        uhp = uhp.uh_next.ptr;
                    else
                        uhp = curbuf.b_u_newhead;
                    if (uhp != null && uhp.uh_save_nr != 0)
                        /* "uh_save_nr" was set in the last block, that means
                         * there were no changes since the last write */
                        target = curbuf.b_u_save_nr_cur + step;
                    else
                        /* count the changes since the last write as one step */
                        target = curbuf.b_u_save_nr_cur + step + 1;
                    if (target <= 0)
                        /* Go to before first write: before the oldest change.
                         * Use the sequence number for that. */
                        dofile = false;
                }
                else
                {
                    /* Moving forward to a newer write. */
                    target = curbuf.b_u_save_nr_cur + step;
                    if (curbuf.b_u_save_nr_last < target)
                    {
                        /* Go to after last write: after the latest change.
                         * Use the sequence number for that. */
                        target = curbuf.b_u_seq_last + 1;
                        dofile = false;
                    }
                }
            }
            else
                target = curbuf.b_u_seq_cur + step;
            if (step < 0)
            {
                if (target < 0)
                    target = 0;
                closest = -1;
            }
            else
            {
                if (dosec)
                    closest = libC._time() - starttime + 1;
                else if (dofile)
                    closest = curbuf.b_u_save_nr_last + 2;
                else
                    closest = curbuf.b_u_seq_last + 2;
                if (closest <= target)
                    target = closest - 1;
            }
        }
        long closest_start = closest;
        long closest_seq = curbuf.b_u_seq_cur;

        int mark = 0, nomark = 0;	// %% anno dunno
        /*
         * May do this twice:
         * 1. Search for "target", update "closest" to the best match found.
         * 2. If "target" not found search for "closest".
         *
         * When using the closest time we use the sequence number in the second
         * round, because there may be several entries with the same time.
         */
        for (int round = 1; round <= 2; round++)
        {
            /* Find the path from the current state to where we want to go.  The
             * desired state can be anywhere in the undo tree, need to go all over
             * it.  We put "nomark" in uh_walk where we have been without success,
             * "mark" where it could possibly be. */
            mark = ++lastmark;
            nomark = ++lastmark;

            if (curbuf.b_u_curhead == null)     /* at leaf of the tree */
                uhp = curbuf.b_u_newhead;
            else
                uhp = curbuf.b_u_curhead;

            while (uhp != null)
            {
                uhp.uh_walk = mark;
                long val;
                if (dosec)
                    val = uhp.uh_time - starttime;
                else if (dofile)
                    val = uhp.uh_save_nr;
                else
                    val = uhp.uh_seq;

                if (round == 1 && !(dofile && val == 0))
                {
                    /* Remember the header that is closest to the target.
                     * It must be at least in the right direction (checked with "b_u_seq_cur").
                     * When the timestamp is equal find the highest/lowest sequence number. */
                    if ((step < 0 ? uhp.uh_seq <= curbuf.b_u_seq_cur
                                  : uhp.uh_seq > curbuf.b_u_seq_cur)
                            && ((dosec && val == closest)
                                ? (step < 0
                                    ? uhp.uh_seq < closest_seq
                                    : uhp.uh_seq > closest_seq)
                                : closest == closest_start
                                    || (target < val
                                        ? (target < closest
                                            ? val - target <= closest - target
                                            : val - target <= target - closest)
                                        : (target < closest
                                            ? target - val <= closest - target
                                            : target - val <= target - closest))))
                    {
                        closest = val;
                        closest_seq = uhp.uh_seq;
                    }
                }

                /* Quit searching when we found a match.  But when searching for a time,
                 * we need to continue looking for the best uh_seq. */
                if (target == val && !dosec)
                {
                    target = uhp.uh_seq;
                    break;
                }

                /* go down in the tree if we haven't been there */
                if (uhp.uh_prev.ptr != null && uhp.uh_prev.ptr.uh_walk != nomark
                                              && uhp.uh_prev.ptr.uh_walk != mark)
                    uhp = uhp.uh_prev.ptr;

                /* go to alternate branch if we haven't been there */
                else if (uhp.uh_alt_next.ptr != null
                        && uhp.uh_alt_next.ptr.uh_walk != nomark
                        && uhp.uh_alt_next.ptr.uh_walk != mark)
                    uhp = uhp.uh_alt_next.ptr;

                /* Go up in the tree if we haven't been there and we are at
                 * the start of alternate branches. */
                else if (uhp.uh_next.ptr != null && uhp.uh_alt_prev.ptr == null
                        && uhp.uh_next.ptr.uh_walk != nomark
                        && uhp.uh_next.ptr.uh_walk != mark)
                {
                    /* If still at the start we don't go through this change. */
                    if (uhp == curbuf.b_u_curhead)
                        uhp.uh_walk = nomark;
                    uhp = uhp.uh_next.ptr;
                }

                else
                {
                    /* need to backtrack; mark this node as useless */
                    uhp.uh_walk = nomark;
                    if (uhp.uh_alt_prev.ptr != null)
                        uhp = uhp.uh_alt_prev.ptr;
                    else
                        uhp = uhp.uh_next.ptr;
                }
            }

            if (uhp != null)    /* found it */
                break;

            if (absolute)
            {
                emsgn(u8("E830: Undo number %ld not found"), step);
                return;
            }

            if (closest == closest_start)
            {
                if (step < 0)
                    msg(u8("Already at oldest change"));
                else
                    msg(u8("Already at newest change"));
                return;
            }

            target = closest_seq;
            dosec = false;
            dofile = false;
            if (step < 0)
                above = true;       /* stop above the header */
        }

        /* If we found it: Follow the path to go to where we want to be. */
        if (uhp != null)
        {
            /*
             * First go up the tree as much as needed.
             */
            while (!got_int)
            {
                /* Do the change warning now, for the same reason as above. */
                change_warning(0);

                uhp = curbuf.b_u_curhead;
                if (uhp == null)
                    uhp = curbuf.b_u_newhead;
                else
                    uhp = uhp.uh_next.ptr;
                if (uhp == null || uhp.uh_walk != mark || (uhp.uh_seq == target && !above))
                    break;
                curbuf.b_u_curhead = uhp;
                u_undoredo(true);
                uhp.uh_walk = nomark;   /* don't go back down here */
            }

            /*
             * And now go down the tree (redo), branching off where needed.
             */
            while (!got_int)
            {
                /* Do the change warning now, for the same reason as above. */
                change_warning(0);

                uhp = curbuf.b_u_curhead;
                if (uhp == null)
                    break;

                /* Go back to the first branch with a mark. */
                while (uhp.uh_alt_prev.ptr != null && uhp.uh_alt_prev.ptr.uh_walk == mark)
                    uhp = uhp.uh_alt_prev.ptr;

                /* Find the last branch with a mark, that's the one. */
                u_header_C last = uhp;
                while (last.uh_alt_next.ptr != null && last.uh_alt_next.ptr.uh_walk == mark)
                    last = last.uh_alt_next.ptr;
                if (last != uhp)
                {
                    /* Make the used branch the first entry in the list of
                     * alternatives to make "u" and CTRL-R take this branch. */
                    while (uhp.uh_alt_prev.ptr != null)
                        uhp = uhp.uh_alt_prev.ptr;
                    if (last.uh_alt_next.ptr != null)
                        last.uh_alt_next.ptr.uh_alt_prev.ptr = last.uh_alt_prev.ptr;
                    last.uh_alt_prev.ptr.uh_alt_next.ptr = last.uh_alt_next.ptr;
                    last.uh_alt_prev.ptr = null;
                    last.uh_alt_next.ptr = uhp;
                    uhp.uh_alt_prev.ptr = last;

                    if (curbuf.b_u_oldhead == uhp)
                        curbuf.b_u_oldhead = last;
                    uhp = last;
                    if (uhp.uh_next.ptr != null)
                        uhp.uh_next.ptr.uh_prev.ptr = uhp;
                }
                curbuf.b_u_curhead = uhp;

                if (uhp.uh_walk != mark)
                    break;      /* must have reached the target */

                /* Stop when going backwards in time and didn't find
                 * the exact header we were looking for. */
                if (uhp.uh_seq == target && above)
                {
                    curbuf.b_u_seq_cur = target - 1;
                    break;
                }

                u_undoredo(false);

                /* Advance "curhead" to below the header we last used.
                 * If it becomes null, then we need to set "newhead" to this leaf. */
                if (uhp.uh_prev.ptr == null)
                    curbuf.b_u_newhead = uhp;
                curbuf.b_u_curhead = uhp.uh_prev.ptr;
                did_undo = false;

                if (uhp.uh_seq == target)   /* found it! */
                    break;

                uhp = uhp.uh_prev.ptr;
                if (uhp == null || uhp.uh_walk != mark)
                {
                    /* Need to redo more but can't find it... */
                    emsg2(e_intern2, u8("undo_time()"));
                    break;
                }
            }
        }

        u_undo_end(did_undo, absolute);
    }

    /*
     * u_undoredo: common code for undo and redo
     *
     * The lines in the file are replaced by the lines in the entry list at
     * curbuf.b_u_curhead.  The replaced lines in the file are saved in the entry
     * list for the next undo/redo.
     *
     * When "undo" is true we go up in the tree, when false we go down.
     */
    /*private*/ static void u_undoredo(boolean undo)
    {
        Bytes[] newarray = null;
        long newlnum = MAXLNUM;
        u_entry_C newlist = null;
        u_header_C curhead = curbuf.b_u_curhead;

        /* Don't want autocommands using the undo structures here, they are invalid till the end. */
        block_autocmds();

        int old_flags = curhead.uh_flags;
        int new_flags = (curbuf.b_changed[0] ? UH_CHANGED : 0)
                      + ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0 ? UH_EMPTYBUF : 0);
        setpcmark();

        /*
         * save marks before undo/redo
         */
        pos_C[] namedm = ARRAY_pos(NMARKS);
        for (int i = 0; i < NMARKS; i++)
            COPY_pos(namedm[i], curbuf.b_namedm[i]);

        visualinfo_C visualinfo = new visualinfo_C();
        COPY_visualinfo(visualinfo, curbuf.b_visual);

        curbuf.b_op_start.lnum = curbuf.b_ml.ml_line_count;
        curbuf.b_op_start.col = 0;
        curbuf.b_op_end.lnum = 0;
        curbuf.b_op_end.col = 0;

        for (u_entry_C uep = curhead.uh_entry, nuep; uep != null; uep = nuep)
        {
            long top = uep.ue_top;
            long bot = uep.ue_bot;
            if (bot == 0)
                bot = curbuf.b_ml.ml_line_count + 1;
            if (top > curbuf.b_ml.ml_line_count || bot <= top || bot > curbuf.b_ml.ml_line_count + 1)
            {
                unblock_autocmds();
                emsg(u8("E438: u_undo: line numbers wrong"));
                changed();          /* don't want UNCHANGED now */
                return;
            }

            int oldsize = (int)(bot - top - 1);       /* number of lines before undo */
            int newsize = (int)uep.ue_size;         /* number of lines after undo */

            if (top < newlnum)
            {
                /* If the saved cursor is somewhere in this undo block,
                 * move it to the remembered position.
                 * Makes "gwap" put the cursor back where it was. */
                long lnum = curhead.uh_cursor.lnum;
                if (top <= lnum && lnum <= top + newsize + 1)
                {
                    COPY_pos(curwin.w_cursor, curhead.uh_cursor);
                    newlnum = curwin.w_cursor.lnum - 1;
                }
                else
                {
                    /* Use the first line that actually changed.
                     * Avoids that undoing auto-formatting puts the cursor in the previous line. */
                    int i;
                    for (i = 0; i < newsize && i < oldsize; i++)
                        if (STRCMP(uep.ue_array[i], ml_get(top + 1 + i)) != 0)
                            break;
                    if (i == newsize && newlnum == MAXLNUM && uep.ue_next == null)
                    {
                        newlnum = top;
                        curwin.w_cursor.lnum = newlnum + 1;
                    }
                    else if (i < newsize)
                    {
                        newlnum = top + i;
                        curwin.w_cursor.lnum = newlnum + 1;
                    }
                }
            }

            boolean empty_buffer = false;   /* buffer became empty */

            /* delete the lines between top and bot and save them in "newarray" */
            if (0 < oldsize)
            {
                newarray = new Bytes[oldsize];

                /* delete backwards, it goes faster in most cases */
                long lnum = bot - 1;
                for (int i = oldsize; 0 < i--; lnum--)
                {
                    newarray[i] = STRDUP(ml_get(lnum));
                    /* remember we deleted the last line in the buffer,
                     * and a dummy empty line will be inserted */
                    if (curbuf.b_ml.ml_line_count == 1)
                        empty_buffer = true;
                    ml_delete(lnum, false);
                }
            }
            else
                newarray = null;

            /* insert the lines in u_array between top and bot */
            long lnum = top;
            for (int i = 0; i < newsize; i++, lnum++)
            {
                /*
                 * If the file is empty, there is an empty line 1 that we
                 * should get rid of, by replacing it with the new line.
                 */
                if (empty_buffer && lnum == 0)
                    ml_replace(1, uep.ue_array[i], true);
                else
                    ml_append(lnum, uep.ue_array[i], 0, false);
            }

            /* adjust marks */
            if (oldsize != newsize)
            {
                mark_adjust(top + 1, top + oldsize, MAXLNUM, newsize - oldsize);
                if (curbuf.b_op_start.lnum > top + oldsize)
                    curbuf.b_op_start.lnum += newsize - oldsize;
                if (curbuf.b_op_end.lnum > top + oldsize)
                    curbuf.b_op_end.lnum += newsize - oldsize;
            }

            changed_lines(top + 1, 0, bot, newsize - oldsize);

            /* set '[ and '] mark */
            if (top + 1 < curbuf.b_op_start.lnum)
                curbuf.b_op_start.lnum = top + 1;
            if (newsize == 0 && curbuf.b_op_end.lnum < top + 1)
                curbuf.b_op_end.lnum = top + 1;
            else if (curbuf.b_op_end.lnum < top + newsize)
                curbuf.b_op_end.lnum = top + newsize;

            u_newcount += newsize;
            u_oldcount += oldsize;
            uep.ue_size = oldsize;
            uep.ue_array = newarray;
            uep.ue_bot = top + newsize + 1;

            /*
             * insert this entry in front of the new entry list
             */
            nuep = uep.ue_next;
            uep.ue_next = newlist;
            newlist = uep;
        }

        curhead.uh_entry = newlist;
        curhead.uh_flags = new_flags;
        if ((old_flags & UH_EMPTYBUF) != 0 && bufempty())
            curbuf.b_ml.ml_flags |= ML_EMPTY;
        if ((old_flags & UH_CHANGED) != 0)
            changed();
        else
            unchanged(curbuf, false);

        /*
         * restore marks from before undo/redo
         */
        for (int i = 0; i < NMARKS; i++)
        {
            if (curhead.uh_namedm[i].lnum != 0)
                COPY_pos(curbuf.b_namedm[i], curhead.uh_namedm[i]);
            if (namedm[i].lnum != 0)
                COPY_pos(curhead.uh_namedm[i], namedm[i]);
            else
                curhead.uh_namedm[i].lnum = 0;
        }
        if (curhead.uh_visual.vi_start.lnum != 0)
        {
            COPY_visualinfo(curbuf.b_visual, curhead.uh_visual);
            COPY_visualinfo(curhead.uh_visual, visualinfo);
        }

        /*
         * If the cursor is only off by one line,
         * put it at the same position as before starting the change (for the "o" command).
         * Otherwise the cursor should go to the first undone line.
         */
        if (curhead.uh_cursor.lnum + 1 == curwin.w_cursor.lnum && 1 < curwin.w_cursor.lnum)
            --curwin.w_cursor.lnum;
        if (curwin.w_cursor.lnum <= curbuf.b_ml.ml_line_count)
        {
            if (curhead.uh_cursor.lnum == curwin.w_cursor.lnum)
            {
                curwin.w_cursor.col = curhead.uh_cursor.col;
                if (virtual_active() && 0 <= curhead.uh_cursor_vcol)
                    coladvance((int)curhead.uh_cursor_vcol);
                else
                    curwin.w_cursor.coladd = 0;
            }
            else
                beginline(BL_SOL | BL_FIX);
        }
        else
        {
            /* We get here with the current cursor line being past the end (eg
             * after adding lines at the end of the file, and then undoing it).
             * check_cursor() will move the cursor to the last line.  Move it to
             * the first column here. */
            curwin.w_cursor.col = 0;
            curwin.w_cursor.coladd = 0;
        }

        /* Make sure the cursor is on an existing line and column. */
        check_cursor();

        /* Remember where we are for "g-" and ":earlier 10s". */
        curbuf.b_u_seq_cur = curhead.uh_seq;
        if (undo)
            /* We are below the previous undo.  However, to make ":earlier 1s"
             * work we compute this as being just above the just undone change. */
            --curbuf.b_u_seq_cur;

        /* Remember where we are for ":earlier 1f" and ":later 1f". */
        if (curhead.uh_save_nr != 0)
        {
            if (undo)
                curbuf.b_u_save_nr_cur = curhead.uh_save_nr - 1;
            else
                curbuf.b_u_save_nr_cur = curhead.uh_save_nr;
        }

        /* The timestamp can be the same for multiple changes,
         * just use the one of the undone/redone change. */
        curbuf.b_u_time_cur = curhead.uh_time;

        unblock_autocmds();
    }

    /*
     * If we deleted or added lines, report the number of less/more lines.
     * Otherwise, report the number of changes
     * (this may be incorrect in some cases, but it's better than nothing).
     */
    /*private*/ static void u_undo_end(boolean did_undo, boolean absolute)
        /* did_undo: just did an undo */
        /* absolute: used ":undo N" */
    {
        if (global_busy != 0             /* no messages now, wait until global is finished */
                || !messaging())    /* 'lazyredraw' set, don't do messages now */
            return;

        if ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0)
            --u_newcount;

        u_oldcount -= u_newcount;

        Bytes msgstr;
        if (u_oldcount == -1)
            msgstr = u8("more line");
        else if (u_oldcount < 0)
            msgstr = u8("more lines");
        else if (u_oldcount == 1)
            msgstr = u8("line less");
        else if (1 < u_oldcount)
            msgstr = u8("fewer lines");
        else
        {
            u_oldcount = u_newcount;

            if (u_newcount == 1)
                msgstr = u8("change");
            else
                msgstr = u8("changes");
        }

        u_header_C uhp;
        if (curbuf.b_u_curhead != null)
        {
            /* For ":undo N" we prefer a "after #N" message. */
            if (absolute && curbuf.b_u_curhead.uh_next.ptr != null)
            {
                uhp = curbuf.b_u_curhead.uh_next.ptr;
                did_undo = false;
            }
            else if (did_undo)
                uhp = curbuf.b_u_curhead;
            else
                uhp = curbuf.b_u_curhead.uh_next.ptr;
        }
        else
            uhp = curbuf.b_u_newhead;

        Bytes msgbuf = new Bytes(80);
        if (uhp == null)
            msgbuf.be(0, NUL);
        else
            u_add_time(msgbuf, msgbuf.size(), uhp.uh_time);

        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            if (wp.w_buffer == curbuf && 0 < wp.w_onebuf_opt.wo_cole[0])
                redraw_win_later(wp, NOT_VALID);

        smsg(u8("%ld %s; %s #%ld  %s"),
                (u_oldcount < 0) ? -u_oldcount : u_oldcount,
                msgstr,
                did_undo ? u8("before") : u8("after"),
                (uhp == null) ? 0L : uhp.uh_seq,
                msgbuf);
    }

    /*
     * u_sync: stop adding to the current entry list
     */
    /*private*/ static void u_sync(boolean force)
        /* force: Also sync when no_u_sync is set. */
    {
        /* Skip it when already synced or syncing is disabled. */
        if (curbuf.b_u_synced || (!force && 0 < no_u_sync))
            return;

        if (get_undolevel() < 0)
            curbuf.b_u_synced = true;   /* no entries, nothing to do */
        else
        {
            u_getbot();                 /* compute ue_bot of previous u_save() */
            curbuf.b_u_curhead = null;
        }
    }

    /*
     * ":undolist": List the leafs of the undo tree
     */
    /*private*/ static final ex_func_C ex_undolist = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            int changes = 1;

            /*
             * 1: walk the tree to find all leafs, put the info in "ga".
             * 2: sort the lines
             * 3: display the list
             */
            int mark = ++lastmark;
            int nomark = ++lastmark;

            Growing<Bytes> ga = new Growing<Bytes>(Bytes.class, 20);

            for (u_header_C uhp = curbuf.b_u_oldhead; uhp != null; )
            {
                if (uhp.uh_prev.ptr == null && uhp.uh_walk != nomark && uhp.uh_walk != mark)
                {
                    vim_snprintf(ioBuff, IOSIZE, u8("%6ld %7ld  "), uhp.uh_seq, changes);
                    u_add_time(ioBuff.plus(strlen(ioBuff)), IOSIZE - strlen(ioBuff), uhp.uh_time);
                    if (0 < uhp.uh_save_nr)
                    {
                        while (strlen(ioBuff) < 33)
                            STRCAT(ioBuff, u8(" "));
                        vim_snprintf_add(ioBuff, IOSIZE, u8("  %3ld"), uhp.uh_save_nr);
                    }

                    ga.ga_grow(1);
                    ga.ga_data[ga.ga_len++] = STRDUP(ioBuff);
                }

                uhp.uh_walk = mark;

                /* go down in the tree if we haven't been there */
                if (uhp.uh_prev.ptr != null
                        && uhp.uh_prev.ptr.uh_walk != nomark
                        && uhp.uh_prev.ptr.uh_walk != mark)
                {
                    uhp = uhp.uh_prev.ptr;
                    changes++;
                }
                /* go to alternate branch if we haven't been there */
                else if (uhp.uh_alt_next.ptr != null
                        && uhp.uh_alt_next.ptr.uh_walk != nomark
                        && uhp.uh_alt_next.ptr.uh_walk != mark)
                {
                    uhp = uhp.uh_alt_next.ptr;
                }
                /* go up in the tree if we haven't been there and we are at the start of alternate branches */
                else if (uhp.uh_next.ptr != null && uhp.uh_alt_prev.ptr == null
                        && uhp.uh_next.ptr.uh_walk != nomark
                        && uhp.uh_next.ptr.uh_walk != mark)
                {
                    uhp = uhp.uh_next.ptr;
                    --changes;
                }
                else
                {
                    /* need to backtrack; mark this node as done */
                    uhp.uh_walk = nomark;
                    if (uhp.uh_alt_prev.ptr != null)
                        uhp = uhp.uh_alt_prev.ptr;
                    else
                    {
                        uhp = uhp.uh_next.ptr;
                        --changes;
                    }
                }
            }

            if (ga.ga_len == 0)
                msg(u8("Nothing to undo"));
            else
            {
                sort_strings(ga.ga_data, ga.ga_len);

                msg_start();
                msg_puts_attr(u8("number changes  when               saved"), hl_attr(HLF_T));
                for (int i = 0; i < ga.ga_len && !got_int; i++)
                {
                    msg_putchar('\n');
                    if (got_int)
                        break;
                    msg_puts(ga.ga_data[i]);
                }
                msg_end();

                ga.ga_clear();
            }
        }
    };

    /*
     * Put the timestamp of an undo header in "buf[buflen]" in a nice format.
     */
    /*private*/ static void u_add_time(Bytes buf, int buflen, long seconds)
    {
        if (100 <= libC._time() - seconds)
        {
            tm_C curtime = libC._localtime(seconds);
            if (libC._time() - seconds < (60L * 60L * 12L))
                /* within 12 hours */
                libC.strftime(buf, buflen, u8("%H:%M:%S"), curtime);
            else
                /* longer ago */
                libC.strftime(buf, buflen, u8("%Y/%m/%d %H:%M:%S"), curtime);
        }
        else
            vim_snprintf(buf, buflen, u8("%ld seconds ago"), libC._time() - seconds);
    }

    /*
     * ":undojoin": continue adding to the last entry list
     */
    /*private*/ static final ex_func_C ex_undojoin = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            if (curbuf.b_u_newhead == null)
                return;                             /* nothing changed before */
            if (curbuf.b_u_curhead != null)
            {
                emsg(u8("E790: undojoin is not allowed after undo"));
                return;
            }
            if (!curbuf.b_u_synced)
                return;                             /* already unsynced */
            if (get_undolevel() < 0)
                return;                             /* no entries, nothing to do */
            else
            {
                /* Go back to the last entry. */
                curbuf.b_u_curhead = curbuf.b_u_newhead;
                curbuf.b_u_synced = false;          /* no entries, nothing to do */
            }
        }
    };

    /*
     * Called after writing or reloading the file and setting "b_changed" to false.
     * Now an undo means that the buffer is modified.
     */
    /*private*/ static void u_unchanged(buffer_C buf)
    {
        u_unch_branch(buf.b_u_oldhead);
        buf.b_did_warn = false;
    }

    /*
     * After reloading a buffer which was saved for 'undoreload':
     * find the first line that was changed and set the cursor there.
     */
    /*private*/ static void u_find_first_changed()
    {
        u_header_C uhp = curbuf.b_u_newhead;

        if (curbuf.b_u_curhead != null || uhp == null)
            return;                         /* undid something in an autocmd? */

        /* Check that the last undo block was for the whole file. */
        u_entry_C uep = uhp.uh_entry;
        if (uep.ue_top != 0 || uep.ue_bot != 0)
            return;

        long lnum;
        for (lnum = 1; lnum < curbuf.b_ml.ml_line_count && lnum <= uep.ue_size; lnum++)
            if (STRCMP(ml_get_buf(curbuf, lnum, false), uep.ue_array[(int)lnum - 1]) != 0)
            {
                clearpos(uhp.uh_cursor);
                uhp.uh_cursor.lnum = lnum;
                return;
            }
        if (curbuf.b_ml.ml_line_count != uep.ue_size)
        {
            /* lines added or deleted at the end, put the cursor there */
            clearpos(uhp.uh_cursor);
            uhp.uh_cursor.lnum = lnum;
        }
    }

    /*
     * Increase the write count, store it in the last undo header, what would be used for "u".
     */
    /*private*/ static void u_update_save_nr(buffer_C buf)
    {
        buf.b_u_save_nr_last++;
        buf.b_u_save_nr_cur = buf.b_u_save_nr_last;
        u_header_C uhp = buf.b_u_curhead;
        if (uhp != null)
            uhp = uhp.uh_next.ptr;
        else
            uhp = buf.b_u_newhead;
        if (uhp != null)
            uhp.uh_save_nr = buf.b_u_save_nr_last;
    }

    /*private*/ static void u_unch_branch(u_header_C uhp)
    {
        for (u_header_C uh = uhp; uh != null; uh = uh.uh_prev.ptr)
        {
            uh.uh_flags |= UH_CHANGED;
            if (uh.uh_alt_next.ptr != null)
                u_unch_branch(uh.uh_alt_next.ptr);  /* recursive */
        }
    }

    /*
     * Get pointer to last added entry.
     * If it's not valid, give an error message and return null.
     */
    /*private*/ static u_entry_C u_get_headentry()
    {
        if (curbuf.b_u_newhead == null || curbuf.b_u_newhead.uh_entry == null)
        {
            emsg(u8("E439: undo list corrupt"));
            return null;
        }
        return curbuf.b_u_newhead.uh_entry;
    }

    /*
     * u_getbot(): compute the line number of the previous u_save()
     *              It is called only when b_u_synced is false.
     */
    /*private*/ static void u_getbot()
    {
        u_entry_C uep = u_get_headentry(); /* check for corrupt undo list */
        if (uep == null)
            return;

        uep = curbuf.b_u_newhead.uh_getbot_entry;
        if (uep != null)
        {
            /*
             * the new ue_bot is computed from the number of lines that has been
             * inserted (0 - deleted) since calling u_save().  This is equal to the
             * old line count subtracted from the current line count.
             */
            long extra = curbuf.b_ml.ml_line_count - uep.ue_lcount;
            uep.ue_bot = uep.ue_top + uep.ue_size + 1 + extra;
            if (uep.ue_bot < 1 || curbuf.b_ml.ml_line_count < uep.ue_bot)
            {
                emsg(u8("E440: undo line missing"));
                uep.ue_bot = uep.ue_top + 1;    /* assume all lines deleted, will
                                                 * get all the old lines back
                                                 * without deleting the current ones */
            }

            curbuf.b_u_newhead.uh_getbot_entry = null;
        }

        curbuf.b_u_synced = true;
    }

    /*
     * Free one header "uhp" and its entry list and adjust the pointers.
     */
    /*private*/ static void u_freeheader(buffer_C buf, u_header_C uhp, u_header_C[] uhpp)
        /* uhpp: if not null reset when freeing this header */
    {
        /* When there is an alternate redo list free that branch completely,
         * because we can never go there. */
        if (uhp.uh_alt_next.ptr != null)
            u_freebranch(buf, uhp.uh_alt_next.ptr, uhpp);

        if (uhp.uh_alt_prev.ptr != null)
            uhp.uh_alt_prev.ptr.uh_alt_next.ptr = null;

        /* Update the links in the list to remove the header. */
        if (uhp.uh_next.ptr == null)
            buf.b_u_oldhead = uhp.uh_prev.ptr;
        else
            uhp.uh_next.ptr.uh_prev.ptr = uhp.uh_prev.ptr;

        if (uhp.uh_prev.ptr == null)
            buf.b_u_newhead = uhp.uh_next.ptr;
        else
            for (u_header_C uhap = uhp.uh_prev.ptr; uhap != null; uhap = uhap.uh_alt_next.ptr)
                uhap.uh_next.ptr = uhp.uh_next.ptr;

        u_freeentries(buf, uhp, uhpp);
    }

    /*
     * Free an alternate branch and any following alternate branches.
     */
    /*private*/ static void u_freebranch(buffer_C buf, u_header_C uhp, u_header_C[] uhpp)
        /* uhpp: if not null reset when freeing this header */
    {
        /* If this is the top branch we may need to use u_freeheader() to update all the pointers. */
        if (uhp == buf.b_u_oldhead)
        {
            while (buf.b_u_oldhead != null)
                u_freeheader(buf, buf.b_u_oldhead, uhpp);
            return;
        }

        if (uhp.uh_alt_prev.ptr != null)
            uhp.uh_alt_prev.ptr.uh_alt_next.ptr = null;

        for (u_header_C next = uhp; next != null; )
        {
            u_header_C tofree = next;
            if (tofree.uh_alt_next.ptr != null)
                u_freebranch(buf, tofree.uh_alt_next.ptr, uhpp);    /* recursive */
            next = tofree.uh_prev.ptr;
            u_freeentries(buf, tofree, uhpp);
        }
    }

    /*
     * Free all the undo entries for one header and the header itself.
     * This means that "uhp" is invalid when returning.
     */
    /*private*/ static void u_freeentries(buffer_C buf, u_header_C uhp, u_header_C[] uhpp)
        /* uhpp: if not null reset when freeing this header */
    {
        /* Check for pointers to the header that become invalid now. */
        if (buf.b_u_curhead == uhp)
            buf.b_u_curhead = null;
        if (buf.b_u_newhead == uhp)
            buf.b_u_newhead = null;         /* freeing the newest entry */
        if (uhpp != null && uhp == uhpp[0])
            uhpp[0] = null;

        --buf.b_u_numhead;
    }

    /*
     * invalidate the undo buffer; called when storage has already been released
     */
    /*private*/ static void u_clearall(buffer_C buf)
    {
        buf.b_u_newhead = buf.b_u_oldhead = buf.b_u_curhead = null;
        buf.b_u_synced = true;
        buf.b_u_numhead = 0;
        buf.b_u_line_ptr = null;
        buf.b_u_line_lnum = 0;
    }

    /*
     * save the line "lnum" for the "U" command
     */
    /*private*/ static void u_saveline(long lnum)
    {
        if (lnum == curbuf.b_u_line_lnum)                   /* line is already saved */
            return;
        if (lnum < 1 || curbuf.b_ml.ml_line_count < lnum)   /* should never happen */
            return;

        u_clearline();

        curbuf.b_u_line_lnum = lnum;
        if (curwin.w_cursor.lnum == lnum)
            curbuf.b_u_line_colnr = curwin.w_cursor.col;
        else
            curbuf.b_u_line_colnr = 0;
        curbuf.b_u_line_ptr = STRDUP(ml_get(lnum));
    }

    /*
     * clear the line saved for the "U" command
     * (this is used externally for crossing a line while in insert mode)
     */
    /*private*/ static void u_clearline()
    {
        if (curbuf.b_u_line_ptr != null)
        {
            curbuf.b_u_line_ptr = null;
            curbuf.b_u_line_lnum = 0;
        }
    }

    /*
     * Implementation of the "U" command.
     * Differentiation from vi: "U" can be undone with the next "U".
     * We also allow the cursor to be in another line.
     * Careful: may trigger autocommands that reload the buffer.
     */
    /*private*/ static void u_undoline()
    {
        if (undo_off)
            return;

        if (curbuf.b_u_line_ptr == null || curbuf.b_ml.ml_line_count < curbuf.b_u_line_lnum)
        {
            beep_flush();
            return;
        }

        /* first save the line for the 'u' command */
        if (u_savecommon(curbuf.b_u_line_lnum - 1, curbuf.b_u_line_lnum + 1, 0, false) == false)
            return;

        Bytes oldp = STRDUP(ml_get(curbuf.b_u_line_lnum));

        ml_replace(curbuf.b_u_line_lnum, curbuf.b_u_line_ptr, true);
        changed_bytes(curbuf.b_u_line_lnum, 0);
        curbuf.b_u_line_ptr = oldp;

        int t = curbuf.b_u_line_colnr;
        if (curwin.w_cursor.lnum == curbuf.b_u_line_lnum)
            curbuf.b_u_line_colnr = curwin.w_cursor.col;
        curwin.w_cursor.col = t;
        curwin.w_cursor.lnum = curbuf.b_u_line_lnum;
        check_cursor_col();
    }

    /*
     * Free all allocated memory blocks for the buffer 'buf'.
     */
    /*private*/ static void u_blockfree(buffer_C buf)
    {
        while (buf.b_u_oldhead != null)
            u_freeheader(buf, buf.b_u_oldhead, null);
        buf.b_u_line_ptr = null;
    }

    /*
     * Check if the 'modified' flag is set, or 'ff' has changed (only need to
     * check the first character, because it can only be "dos", "unix" or "mac").
     * "nofile" and "scratch" type buffers are considered to always be unchanged.
     */
    /*private*/ static boolean bufIsChanged(buffer_C buf)
    {
        return (buf.b_changed[0] || file_ff_differs(buf, true));
    }

    /*private*/ static boolean curbufIsChanged()
    {
        return (curbuf.b_changed[0] || file_ff_differs(curbuf, true));
    }

    /*
     * For undotree(): Append the list of undo blocks at "first_uhp" to "list".
     * Recursive.
     */
    /*private*/ static void u_eval_tree(u_header_C first_uhp, list_C list)
    {
        for (u_header_C uhp = first_uhp; uhp != null; uhp = uhp.uh_prev.ptr)
        {
            dict_C dict = newDict();

            dict_add_nr_str(dict, u8("seq"), uhp.uh_seq, null);
            dict_add_nr_str(dict, u8("time"), uhp.uh_time, null);
            if (uhp == curbuf.b_u_newhead)
                dict_add_nr_str(dict, u8("newhead"), 1, null);
            if (uhp == curbuf.b_u_curhead)
                dict_add_nr_str(dict, u8("curhead"), 1, null);
            if (0 < uhp.uh_save_nr)
                dict_add_nr_str(dict, u8("save"), uhp.uh_save_nr, null);

            if (uhp.uh_alt_next.ptr != null)
            {
                list_C alt_list = new list_C();

                /* Recursive call to add alternate undo tree. */
                u_eval_tree(uhp.uh_alt_next.ptr, alt_list);
                dict_add_list(dict, u8("alt"), alt_list);
            }

            list_append_dict(list, dict);
        }
    }
}
