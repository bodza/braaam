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
public class VimT
{
    /*
     * fileio.c: read from and write to a file --------------------------------------------------------
     */

    /*private*/ static final int BUFSIZE         = 8192;    /* size of normal write buffer */

    /*private*/ static final int AUGROUP_DEFAULT    = -1;   /* default autocmd group */
    /*private*/ static final int AUGROUP_ERROR      = -2;   /* erroneous autocmd group */
    /*private*/ static final int AUGROUP_ALL        = -3;   /* all autocmd groups */

    /*private*/ static final int FIO_LATIN1     = 0x01;     /* convert Latin1 */
    /*private*/ static final int FIO_UTF8       = 0x02;     /* convert UTF-8 */
    /*private*/ static final int FIO_UCS2       = 0x04;     /* convert UCS-2 */
    /*private*/ static final int FIO_UCS4       = 0x08;     /* convert UCS-4 */
    /*private*/ static final int FIO_UTF16      = 0x10;     /* convert UTF-16 */
    /*private*/ static final int FIO_ENDIAN_L   = 0x80;     /* little endian */
    /*private*/ static final int FIO_NOCONVERT  = 0x2000;   /* skip encoding conversion */
    /*private*/ static final int FIO_UCSBOM     = 0x4000;   /* check for BOM at start of file */
    /*private*/ static final int FIO_ALL        = -1;       /* allow all formats */

    /* When converting, a read() or write() may leave some bytes to be converted
     * for the next call.  The value is guessed... */
    /*private*/ static final int CONV_RESTLEN = 30;

    /* We have to guess how much a sequence of bytes may expand when converting
     * with iconv() to be able to allocate a buffer. */
    /*private*/ static final int ICONV_MULT = 8;

    /*
     * Structure to pass arguments from buf_write() to buf_write_bytes().
     */
    /*private*/ static final class bw_info_C
    {
        int         bw_fd;                  /* file descriptor */
        Bytes       bw_buf;                 /* buffer with data to be written */
        int         bw_len;                 /* length of data */
        int         bw_flags;               /* FIO_ flags */
        Bytes       bw_rest;                /* not converted bytes */
        int         bw_restlen;             /* nr of bytes in bw_rest[] */
        int         bw_first;               /* first write call */
        Bytes       bw_conv_buf;            /* buffer for writing converted chars */
        int         bw_conv_buflen;         /* size of "bw_conv_buf" */
        boolean     bw_conv_error;          /* set for conversion error */
        long        bw_conv_error_lnum;     /* first line with error or zero */
        long        bw_start_lnum;          /* line number at start of buffer */

        /*private*/ bw_info_C()
        {
            bw_rest = new Bytes(CONV_RESTLEN);
        }
    }

    /*private*/ static Bytes e_auchangedbuf = u8("E812: Autocommands changed buffer or buffer name");

    /*private*/ static void filemess(Bytes name, Bytes s, int attr)
    {
        if (msg_silent != 0)
            return;

        msg_add_fname(name);                        /* put file name in ioBuff with quotes */
        /* If it's extremely long, truncate it. */
        if (IOSIZE - 80 < strlen(ioBuff))
            ioBuff.be(IOSIZE - 80, NUL);
        STRCAT(ioBuff, s);
        /*
         * For the first message may have to start a new line.
         * For further ones overwrite the previous one, reset msg_scroll before calling filemess().
         */
        boolean msg_scroll_save = msg_scroll;
        if (shortmess(SHM_OVERALL) && !exiting && p_verbose[0] == 0)
            msg_scroll = false;
        if (!msg_scroll)                            /* wait a bit when overwriting an error msg */
            check_for_delay(false);
        msg_start();
        msg_scroll = msg_scroll_save;

        msg_scrolled_ign = true;
        /* may truncate the message to avoid a hit-return prompt */
        msg_outtrans_attr(msg_may_trunc(false, ioBuff), attr);
        msg_clr_eos();
        out_flush();
        msg_scrolled_ign = false;
    }

    /*
     * Read lines from file "fname" into the buffer after line "from".
     *
     * 1. We allocate blocks with lalloc, as big as possible.
     * 2. Each block is filled with characters from the file with a single read().
     * 3. The lines are inserted in the buffer with ml_append().
     *
     * (caller must check that fname != null, unless READ_STDIN is used)
     *
     * "lines_to_skip" is the number of lines that must be skipped
     * "lines_to_read" is the number of lines that are appended
     * When not recovering lines_to_skip is 0 and lines_to_read MAXLNUM.
     *
     * flags:
     * READ_NEW     starting to edit a new buffer
     * READ_FILTER  reading filter output
     * READ_STDIN   read from stdin instead of a file
     * READ_BUFFER  read from curbuf instead of a file (converting after reading stdin)
     * READ_DUMMY   read into a dummy buffer (to check if file contents changed)
     * READ_KEEP_UNDO  don't clear undo info or read it from a file
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean readfile(Bytes fname, Bytes sfname, long from, long lines_to_skip, long lines_to_read, exarg_C eap, int flags)
        /* eap: can be null! */
    {
        boolean newfile = ((flags & READ_NEW) != 0);
        boolean filtering = ((flags & READ_FILTER) != 0);
        boolean read_stdin = ((flags & READ_STDIN) != 0);
        boolean read_buffer = ((flags & READ_BUFFER) != 0);

        boolean set_options = newfile || read_buffer || (eap != null && eap.read_edit);

        curbuf.b_no_eol_lnum = 0;   /* in case it was set by the previous read */

        /*
         * If there is no file name yet, use the one for the read file.
         * BF_NOTEDITED is set to reflect this.
         * Don't do this for a read from a filter.
         * Only do this when 'cpoptions' contains the 'f' flag.
         */
        if (curbuf.b_ffname == null
                && !filtering
                && fname != null
                && vim_strbyte(p_cpo[0], CPO_FNAMER) != null
                && (flags & READ_DUMMY) == 0)
        {
            if (set_rw_fname(fname, sfname) == false)
                return false;
        }

        /* Remember the initial values of curbuf, curbuf.b_ffname and curbuf.b_fname
         * to detect whether they are altered as a result of executing nasty autocommands.
         * Also check if "fname" and "sfname" point to one of these values. */
        buffer_C old_curbuf = curbuf;
        Bytes old_b_ffname = curbuf.b_ffname;
        Bytes old_b_fname = curbuf.b_fname;
        boolean using_b_ffname = (BEQ(fname, curbuf.b_ffname) || BEQ(sfname, curbuf.b_ffname));
        boolean using_b_fname = (BEQ(fname, curbuf.b_fname) || BEQ(sfname, curbuf.b_fname));

        /* After reading a file the cursor line changes but we don't want to display the line. */
        ex_no_reprint = true;

        /* don't display the file info for another buffer now */
        need_fileinfo = false;

        /*
         * For Unix: Use the short file name whenever possible.
         * Avoids problems with networks and when directory names are changed.
         */
        if (sfname == null)
            sfname = fname;
        fname = sfname;

        /*
         * The BufReadCmd and FileReadCmd events intercept the reading process
         * by executing the associated commands instead.
         */
        if (!filtering && !read_stdin && !read_buffer)
        {
            pos_C pos = new pos_C();
            COPY_pos(pos, curbuf.b_op_start);

            /* Set '[ mark to the line above where the lines go (line 1 if zero). */
            curbuf.b_op_start.lnum = (from == 0) ? 1 : from;
            curbuf.b_op_start.col = 0;

            if (newfile)
            {
                if (apply_autocmds_exarg(EVENT_BUFREADCMD, null, sfname, false, curbuf, eap))
                    return aborting() ? false : true;
            }
            else
            {
                if (apply_autocmds_exarg(EVENT_FILEREADCMD, sfname, sfname, false, null, eap))
                    return aborting() ? false : true;
            }

            COPY_pos(curbuf.b_op_start, pos);
        }

        boolean msg_save = msg_scroll;
        if (shortmess(SHM_OVER) && p_verbose[0] == 0)
            msg_scroll = false;                     /* overwrite previous file message */
        else
            msg_scroll = true;                      /* don't overwrite previous file message */

        /*
         * If the name ends in a path separator, we can't open it.  Check here,
         * because reading the file may actually work, but then creating the swap
         * file may destroy it!  Reported on MS-DOS and Win 95.
         * If the name is too long we might crash further on, quit here.
         */
        if (fname != null && fname.at(0) != NUL)
        {
            Bytes p = fname.plus(strlen(fname));
            if (after_pathsep(fname, p) || MAXPATHL <= strlen(fname))
            {
                filemess(fname, u8("Illegal file name"), 0);
                msg_end();
                msg_scroll = msg_save;
                return false;
            }
        }

        int perm = 0;
        if (!read_stdin && !read_buffer)
        {
            /*
             * On Unix it is possible to read a directory,
             * so we have to check for it before the open().
             */
            perm = mch_getperm(fname);
            if (0 <= perm && !S_ISREG(perm)         /* not a regular file ... */
                          && !S_ISFIFO(perm)        /* ... nor fifo */
                          && !S_ISSOCK(perm))       /* ... nor socket */
            {
                if (S_ISDIR(perm))
                    filemess(fname, u8("is a directory"), 0);
                else
                    filemess(fname, u8("is not a file"), 0);
                msg_end();
                msg_scroll = msg_save;
                return false;
            }
        }

        /* Set default or forced 'fileformat' and 'binary'. */
        set_file_options(set_options, eap);

        /*
         * When opening a new file we take the readonly flag from the file.
         * Default is r/w, can be set to r/o below.
         * Don't reset it when in readonly mode
         * Only set/reset "b_p_ro" when BF_CHECK_RO is set.
         */
        boolean check_readonly = (newfile && (curbuf.b_flags & BF_CHECK_RO) != 0);
        if (check_readonly && !readonlymode)
            curbuf.b_p_ro[0] = false;

        stat_C st = new stat_C();
        if (newfile && !read_stdin && !read_buffer)
        {
            /* Remember time of file. */
            if (0 <= libC.stat(fname, st))
            {
                buf_store_time(curbuf, st);
                curbuf.b_mtime_read = curbuf.b_mtime;
            }
            else
            {
                curbuf.b_mtime = 0;
                curbuf.b_mtime_read = 0;
                curbuf.b_orig_size = 0;
                curbuf.b_orig_mode = 0;
            }

            /* Reset the "new file" flag.  It will be set again below when the file doesn't exist. */
            curbuf.b_flags &= ~(BF_NEW | BF_NEW_W);
        }

        int fd = 0;

        boolean file_readonly = false;
        if (!read_stdin && !read_buffer)
        {
            if ((perm & 0222) == 0 || libC.access(fname, W_OK) != 0)
                file_readonly = true;
            fd = libC.open(fname, O_RDONLY, 0);
        }

        if (fd < 0)                                 /* cannot open at all */
        {
            msg_scroll = msg_save;

            if (newfile)
            {
                if (perm < 0 && libC.errno() == ENOENT)
                {
                    /*
                     * Set the 'new-file' flag, so that when the file has
                     * been created by someone else, a ":w" will complain.
                     */
                    curbuf.b_flags |= BF_NEW;

                    /* SwapExists autocommand may mess things up. */
                    if (curbuf != old_curbuf
                            || (using_b_ffname && BNE(old_b_ffname, curbuf.b_ffname))
                            || (using_b_fname && BNE(old_b_fname, curbuf.b_fname)))
                    {
                        emsg(e_auchangedbuf);
                        return false;
                    }

                    if (dir_of_file_exists(fname))
                        filemess(sfname, u8("[New File]"), 0);
                    else
                        filemess(sfname, u8("[New DIRECTORY]"), 0);
                    apply_autocmds_exarg(EVENT_BUFNEWFILE, sfname, sfname, false, curbuf, eap);
                    /* remember the current fileformat */
                    save_file_ff(curbuf);

                    if (aborting())             /* autocmds may abort script processing */
                        return false;

                    return true;                /* a new file is not an error */
                }
                else
                {
                    filemess(sfname, (libC.errno() == EFBIG) ? u8("[File too big]")
                                            : (libC.errno() == EOVERFLOW) ? u8("[File too big]")
                                                        : u8("[Permission Denied]"), 0);
                    curbuf.b_p_ro[0] = true;       /* must use "w!" now */
                }
            }

            return false;
        }

        /*
         * Only set the 'ro' flag for readonly files the first time they are loaded.
         */
        if (check_readonly && file_readonly)
            curbuf.b_p_ro[0] = true;

        if (set_options)
        {
            /* Don't change 'eol' if reading from buffer as it will already be
             * correctly set when reading stdin. */
            if (!read_buffer)
            {
                curbuf.b_p_eol[0] = true;
                curbuf.b_start_eol = true;
            }
            curbuf.b_p_bomb[0] = false;
            curbuf.b_start_bomb = false;
        }

        if (!read_stdin && (curbuf != old_curbuf
                || (using_b_ffname && BNE(old_b_ffname, curbuf.b_ffname))
                || (using_b_fname && BNE(old_b_fname, curbuf.b_fname))))
        {
            emsg(e_auchangedbuf);
            if (!read_buffer)
                libc.close(fd);
            return false;
        }

        /* If "Quit" selected at ATTENTION dialog, don't load the file. */
        if (swap_exists_action == SEA_QUIT)
        {
            if (!read_buffer && !read_stdin)
                libc.close(fd);
            return false;
        }

        no_wait_return++;                           /* don't wait for return yet */

        /*
         * Set '[ mark to the line above where the lines go (line 1 if zero).
         */
        curbuf.b_op_start.lnum = (from == 0) ? 1 : from;
        curbuf.b_op_start.col = 0;

        if (!read_buffer)
        {
            boolean m = msg_scroll;
            int n = msg_scrolled;

            /*
             * The file must be closed again,
             * the autocommands may want to change the file before reading it.
             */
            if (!read_stdin)
                libc.close(fd);                          /* ignore errors */

            /*
             * The output from the autocommands should not overwrite anything and
             * should not be overwritten: Set msg_scroll, restore its value if no
             * output was done.
             */
            msg_scroll = true;
            if (filtering)
                apply_autocmds_exarg(EVENT_FILTERREADPRE, null, sfname, false, curbuf, eap);
            else if (read_stdin)
                apply_autocmds_exarg(EVENT_STDINREADPRE, null, sfname, false, curbuf, eap);
            else if (newfile)
                apply_autocmds_exarg(EVENT_BUFREADPRE, null, sfname, false, curbuf, eap);
            else
                apply_autocmds_exarg(EVENT_FILEREADPRE, sfname, sfname, false, null, eap);
            if (msg_scrolled == n)
                msg_scroll = m;

            if (aborting())                         /* autocmds may abort script processing */
            {
                --no_wait_return;
                msg_scroll = msg_save;
                curbuf.b_p_ro[0] = true;               /* must use "w!" now */
                return false;
            }
            /*
             * Don't allow the autocommands to change the current buffer.
             * Try to re-open the file.
             *
             * Don't allow the autocommands to change the buffer name either
             * (cd for example) if it invalidates "fname" or "sfname".
             */
            if (!read_stdin && (curbuf != old_curbuf
                    || (using_b_ffname && BNE(old_b_ffname, curbuf.b_ffname))
                    || (using_b_fname && BNE(old_b_fname, curbuf.b_fname))
                    || (fd = libC.open(fname, O_RDONLY, 0)) < 0))
            {
                --no_wait_return;
                msg_scroll = msg_save;
                if (fd < 0)
                    emsg(u8("E200: *ReadPre autocommands made the file unreadable"));
                else
                    emsg(u8("E201: *ReadPre autocommands must not change current buffer"));
                curbuf.b_p_ro[0] = true;               /* must use "w!" now */
                return false;
            }
        }

        /* Autocommands may add lines to the file, need to check if it is empty. */
        boolean wasempty = ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0);

        if (!filtering && (flags & READ_DUMMY) == 0)
        {
            /*
             * Show the user that we are busy reading the input.  Sometimes this
             * may take a while.  When reading from stdin another program may
             * still be running, don't move the cursor to the last line, unless
             * always using the GUI.
             */
            if (read_stdin)
                libC.fprintf(stdout, u8("Vim: Reading from stdin...\n"));
            else if (!read_buffer)
                filemess(sfname, u8(""), 0);
        }

        msg_scroll = false;                         /* overwrite the file message */

        /*
         * Set linecnt now, before the "retry" caused by a wrong guess for
         * fileformat, and after the autocommands, which may change them.
         */
        long linecnt = curbuf.b_ml.ml_line_count;

        /* "++bad=" argument. */
        int bad_char_behavior = BAD_REPLACE;    /* BAD_KEEP, BAD_DROP or character to replace with */
        if (eap != null && eap.bad_char != 0)
        {
            bad_char_behavior = eap.bad_char;
            if (set_options)
                curbuf.b_bad_char = eap.bad_char;
        }
        else
            curbuf.b_bad_char = 0;

        /*
         * Decide which 'encoding' to use or use first.
         */
        Bytes[] fenc_next = { null };                    /* next item in 'fencs' or null */
        Bytes fenc;                                /* fileencoding to use */
        if (curbuf.b_p_bin[0])
        {
            fenc = u8("");                              /* binary: don't convert */
        }
        else if (p_fencs[0].at(0) == NUL)
        {
            fenc = curbuf.b_p_fenc[0];                 /* use format from buffer */
        }
        else
        {
            fenc_next[0] = p_fencs[0];                    /* try items in 'fileencodings' */
            fenc = next_fenc(fenc_next);
        }

        int try_mac = (vim_strchr(p_ffs[0], 'm') != null) ? 1 : 0;
        boolean try_dos = (vim_strchr(p_ffs[0], 'd') != null);
        int try_unix = (vim_strchr(p_ffs[0], 'x') != null) ? 1 : 0;

        /*
         * Jump back here to retry reading the file in different ways.
         * Reasons to retry:
         * - encoding conversion failed: try another one from "fenc_next"
         * - BOM detected and "fenc" was set, need to setup conversion
         * - "fileformat" check failed: try another
         *
         * Variables set for special retry actions:
         * "file_rewind"    Rewind the file to start reading it again.
         * "advance_fenc"   Advance "fenc" using "fenc_next".
         * "skip_read"      Re-use already read bytes (BOM detected).
         * "keep_fileformat" Don't reset "fileformat".
         */
        int fileformat = 0;                     /* end-of-line format */
        int ff_error = EOL_UNKNOWN;             /* file format with errors */

        boolean file_rewind = false;
        boolean advance_fenc = false;
        boolean skip_read = false;
        boolean keep_fileformat = false;

        boolean read_undo_file = false;

        boolean converted = false;              /* true if conversion done */
        boolean notconverted = false;           /* true if conversion wanted but it wasn't possible */

        long read_buf_lnum = 1;                 /* next line to read from curbuf */
        int read_buf_col = 0;                   /* next char to read from this line */

        Bytes conv_rest = new Bytes(CONV_RESTLEN);
        int conv_restlen = 0;                   /* nr of bytes in conv_rest[] */

        long conv_error = 0;                    /* line nr with conversion error */
        long illegal_byte = 0;                  /* line nr with illegal byte */
        long read_no_eol_lnum = 0;              /* non-zero lnum when last line of
                                                 * last read was missing the eol */
        int size = 0;
        long filesize = 0;
        int real_size = 0;
        long skip_count = 0;
        long read_count = 0;
        int linerest = 0;                      /* remaining chars in line */

        boolean error = false;                  /* errors encountered */

        Bytes ptr = null;                      /* pointer into read buffer */
        Bytes buffer = null;                   /* read buffer */
        Bytes new_buffer = null;
        Bytes line_start = null;

        long lnum = from;

        context_sha256_C sha_ctx = new context_sha256_C();

        retry:
        for ( ; ; )
        {
            if (file_rewind)
            {
                if (read_buffer)
                {
                    read_buf_lnum = 1;
                    read_buf_col = 0;
                }
                else if (read_stdin || libc.lseek(fd, 0L, SEEK_SET) != 0)
                {
                    /* Can't rewind the file, give up. */
                    error = true;
                    break retry;
                }
                /* Delete the previously read lines. */
                while (from < lnum)
                    ml_delete(lnum--, false);
                file_rewind = false;
                if (set_options)
                {
                    curbuf.b_p_bomb[0] = false;
                    curbuf.b_start_bomb = false;
                }
                conv_error = 0;
            }

            /*
             * When retrying with another "fenc" and the first time "fileformat" will be reset.
             */
            if (keep_fileformat)
                keep_fileformat = false;
            else
            {
                if (curbuf.b_p_bin[0])
                    fileformat = EOL_UNIX;                  /* binary: use Unix format */
                else if (p_ffs[0].at(0) == NUL)
                    fileformat = get_fileformat(curbuf);    /* use format from buffer */
                else
                    fileformat = EOL_UNKNOWN;               /* detect from file */
            }

            if (advance_fenc)
            {
                /*
                 * Try the next entry in 'fileencodings'.
                 */
                advance_fenc = false;

                if (fenc_next[0] != null)
                    fenc = next_fenc(fenc_next);
                else
                    fenc = u8("");
            }

            /*
             * Conversion may be required when the encoding of the file is different
             * from 'encoding' or 'encoding' is UTF-16, UCS-2 or UCS-4.
             */
            int fio_flags = 0;
            converted = need_conversion(fenc);
            if (converted)
            {
                /* "ucs-bom" means we need to check the first bytes of the file for a BOM. */
                if (STRCMP(fenc, u8("ucs-bom")) == 0)
                    fio_flags = FIO_UCSBOM;

                /*
                 * Check if UCS-2/4 or Latin1 to UTF-8 conversion needs to be done.
                 * This is handled below after read().  Prepare the fio_flags to avoid
                 * having to parse the string each time.  Also check for Unicode to Latin1
                 * conversion, because iconv() appears not to handle this correctly.
                 * This works just like conversion to UTF-8 except how the resulting
                 * character is put in the buffer.
                 */
                else
                    fio_flags = get_fio_flags(fenc);

                if (fio_flags == 0)
                {
                    /* Conversion wanted but we can't.
                     * Try the next conversion in 'fileencodings'. */
                    advance_fenc = true;
                    continue retry;
                }
            }

            /* Set 'can_retry' when it's possible to rewind the file and try with another 'fenc' value.
             * It's false when no other 'fenc' to try, reading stdin or fixed at a specific encoding. */
            boolean can_retry = (fenc.at(0) != NUL && !read_stdin);

            if (!skip_read)
            {
                linerest = 0;
                filesize = 0;
                skip_count = lines_to_skip;
                read_count = lines_to_read;
                conv_restlen = 0;
                read_undo_file = (newfile && (flags & READ_KEEP_UNDO) == 0
                                        && curbuf.b_ffname != null
                                        && curbuf.b_p_udf[0]
                                        && !filtering
                                        && !read_stdin
                                        && !read_buffer);
                if (read_undo_file)
                    sha256_start(sha_ctx);
            }

            while (!error && !got_int)
            {
                /*
                 * We allocate as much space for the file as we can get, plus
                 * space for the old line plus room for one terminating NUL.
                 * The amount is limited by the fact that read() only can read
                 * upto max_unsigned characters (and other things).
                 */
                if (!skip_read)
                {
                    size = 0x10000;                /* use buffer >= 64K */

                    new_buffer = new Bytes(size + linerest + 1);
                    if (linerest != 0)                   /* copy characters from the previous buffer */
                        BCOPY(new_buffer, 0, ptr, -linerest, linerest);
                    buffer = new_buffer;
                    ptr = buffer.plus(linerest);
                    line_start = buffer;

                    /* May need room to translate into.
                     * For iconv() we don't really know the required space, use a factor ICONV_MULT.
                     * latin1 to utf-8: 1 byte becomes up to 2 bytes
                     * utf-16 to utf-8: 2 bytes become up to 3 bytes, 4 bytes
                     * become up to 4 bytes, size must be multiple of 2
                     * ucs-2 to utf-8: 2 bytes become up to 3 bytes, size must be multiple of 2
                     * ucs-4 to utf-8: 4 bytes become up to 6 bytes, size must be multiple of 4
                     */
                    real_size = size;
                    if ((fio_flags & FIO_LATIN1) != 0)
                        size = size / 2;
                    else if ((fio_flags & (FIO_UCS2 | FIO_UTF16)) != 0)
                        size = (size * 2 / 3) & ~1;
                    else if ((fio_flags & FIO_UCS4) != 0)
                        size = (size * 2 / 3) & ~3;
                    else if (fio_flags == FIO_UCSBOM)
                        size = size / ICONV_MULT;   /* worst case */

                    if (0 < conv_restlen)
                    {
                        /* Insert unconverted bytes from previous line. */
                        BCOPY(ptr, conv_rest, conv_restlen);
                        ptr = ptr.plus(conv_restlen);
                        size -= conv_restlen;
                    }

                    if (read_buffer)
                    {
                        /* Read bytes from curbuf.
                         * Used for converting text read from stdin. */
                        if (from < read_buf_lnum)
                            size = 0;
                        else
                        {
                            int tlen = 0;

                            for ( ; ; )
                            {
                                Bytes p = ml_get(read_buf_lnum).plus(read_buf_col);
                                int n = strlen(p);
                                if (size < tlen + n + 1)
                                {
                                    /* Filled up to "size", append partial line.
                                     * Change NL to NUL to reverse the effect done below. */
                                    n = size - tlen;
                                    for (int ni = 0; ni < n; ni++)
                                    {
                                        if (p.at(ni) == NL)
                                            ptr.be(tlen++, NUL);
                                        else
                                            ptr.be(tlen++, p.at(ni));
                                    }
                                    read_buf_col += n;
                                    break;
                                }
                                else
                                {
                                    /* Append whole line and new-line.
                                     * Change NL to NUL to reverse the effect done below. */
                                    for (int ni = 0; ni < n; ni++)
                                    {
                                        if (p.at(ni) == NL)
                                            ptr.be(tlen++, NUL);
                                        else
                                            ptr.be(tlen++, p.at(ni));
                                    }
                                    ptr.be(tlen++, NL);
                                    read_buf_col = 0;
                                    if (from < ++read_buf_lnum)
                                    {
                                        /* When the last line didn't have an end-of-line,
                                         * don't add it now either. */
                                        if (!curbuf.b_p_eol[0])
                                            --tlen;
                                        size = tlen;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        /* Read bytes from the file. */
                        size = read_eintr(fd, ptr, size);
                    }

                    if (size <= 0)
                    {
                        if (size < 0)               /* read error */
                            error = true;
                        else if (0 < conv_restlen)
                        {
                            /*
                             * Reached end-of-file but some trailing bytes could not be converted.
                             * Truncated file?
                             */

                            /* When we did a conversion report an error. */
                            if (fio_flags != 0)
                            {
                                if (can_retry)
                                {
                                    /* Retry reading with another conversion.
                                    * Use next item from 'fileencodings'. */
                                    advance_fenc = true;
                                    file_rewind = true;
                                    continue retry;
                                }
                                if (conv_error == 0)
                                    conv_error = curbuf.b_ml.ml_line_count - linecnt + 1;
                            }
                            /* Remember the first linenr with an illegal byte. */
                            else if (illegal_byte == 0)
                                illegal_byte = curbuf.b_ml.ml_line_count - linecnt + 1;
                            if (bad_char_behavior == BAD_DROP)
                            {
                                ptr.be(-conv_restlen, NUL);
                                conv_restlen = 0;
                            }
                            else
                            {
                                /* Replace the trailing bytes with the replacement character
                                 * if we were converting; if we weren't, leave the UTF8
                                 * checking code to do it, as it works slightly differently.
                                 */
                                if (bad_char_behavior != BAD_KEEP && fio_flags != 0)
                                {
                                    while (0 < conv_restlen)
                                    {
                                        (ptr = ptr.minus(1)).be(0, bad_char_behavior);
                                        --conv_restlen;
                                    }
                                }
                                fio_flags = 0;      /* don't convert this */
                            }
                        }
                    }
                }
                skip_read = false;

                /*
                 * At start of file (or after crypt magic number): Check for BOM.
                 * Also check for a BOM for other Unicode encodings, but not when
                 * a BOM has already been found.
                 */
                if (filesize == 0 && (fio_flags == FIO_UCSBOM || (!curbuf.b_p_bomb[0] && (fenc.at(0) == (byte)'u' || fenc.at(0) == NUL))))
                {
                    Bytes ccname;
                    int[] blen = new int[1];

                    /* no BOM detection in a short file or in binary mode */
                    if (size < 2 || curbuf.b_p_bin[0])
                        ccname = null;
                    else
                        ccname = check_for_bom(ptr, size, blen,
                            (fio_flags == FIO_UCSBOM) ? FIO_ALL : get_fio_flags(fenc));
                    if (ccname != null)
                    {
                        /* Remove BOM from the text. */
                        filesize += blen[0];
                        size -= blen[0];
                        BCOPY(ptr, 0, ptr, blen[0], size);
                        if (set_options)
                        {
                            curbuf.b_p_bomb[0] = true;
                            curbuf.b_start_bomb = true;
                        }
                    }

                    if (fio_flags == FIO_UCSBOM)
                    {
                        if (ccname == null)
                        {
                            /* No BOM detected: retry with next encoding. */
                            advance_fenc = true;
                        }
                        else
                        {
                            /* BOM detected: set "fenc" and jump back. */
                            fenc = ccname;
                        }
                        /* retry reading without getting new bytes or rewinding */
                        skip_read = true;
                        continue retry;
                    }
                }

                /* Include not converted bytes. */
                ptr = ptr.minus(conv_restlen);
                size += conv_restlen;
                conv_restlen = 0;
                /*
                 * Break here for a read error or end-of-file.
                 */
                if (size <= 0)
                    break;

                if (fio_flags != 0)
                {
                    Bytes tail = null;

                    /*
                     * Convert Unicode or Latin1 to UTF-8.
                     * Go from end to start through the buffer, because the number of bytes may increase.
                     * "dest" points to after where the UTF-8 bytes go,
                     * "p" points to after the next character to convert.
                     */
                    Bytes dest = ptr.plus(real_size);
                    Bytes p;
                    if (fio_flags == FIO_LATIN1 || fio_flags == FIO_UTF8)
                    {
                        p = ptr.plus(size);
                        if (fio_flags == FIO_UTF8)
                        {
                            /* Check for a trailing incomplete UTF-8 sequence. */
                            tail = ptr.plus(size - 1);
                            while (BLT(ptr, tail) && (char_u(tail.at(0)) & 0xc0) == 0x80)
                                tail = tail.minus(1);
                            if (BLE(tail.plus(us_byte2len(tail.at(0), false)), ptr.plus(size)))
                                tail = null;
                            else
                                p = tail;
                        }
                    }
                    else if ((fio_flags & (FIO_UCS2 | FIO_UTF16)) != 0)
                    {
                        /* Check for a trailing byte. */
                        p = ptr.plus((size & ~1));
                        if ((size & 1) != 0)
                            tail = p;
                        if ((fio_flags & FIO_UTF16) != 0 && BLT(ptr, p))
                        {
                            int u8c;
                            /* Check for a trailing leading word. */
                            if ((fio_flags & FIO_ENDIAN_L) != 0)
                            {
                                u8c = (char_u((p = p.minus(1)).at(0)) << 8);
                                u8c += char_u((p = p.minus(1)).at(0));
                            }
                            else
                            {
                                u8c = char_u((p = p.minus(1)).at(0));
                                u8c += (char_u((p = p.minus(1)).at(0)) << 8);
                            }
                            if (0xd800 <= u8c && u8c <= 0xdbff)
                                tail = p;
                            else
                                p = p.plus(2);
                        }
                    }
                    else /* FIO_UCS4 */
                    {
                        /* Check for trailing 1, 2 or 3 bytes. */
                        p = ptr.plus((size & ~3));
                        if ((size & 3) != 0)
                            tail = p;
                    }

                    /* If there is a trailing incomplete sequence move it to conv_rest[]. */
                    if (tail != null)
                    {
                        conv_restlen = BDIFF(ptr.plus(size), tail);
                        BCOPY(conv_rest, tail, conv_restlen);
                        size -= conv_restlen;
                    }

                    while (BLT(ptr, p))
                    {
                        int u8c;
                        if ((fio_flags & FIO_LATIN1) != 0)
                            u8c = char_u((p = p.minus(1)).at(0));
                        else if ((fio_flags & (FIO_UCS2 | FIO_UTF16)) != 0)
                        {
                            if ((fio_flags & FIO_ENDIAN_L) != 0)
                            {
                                u8c = (char_u((p = p.minus(1)).at(0)) << 8);
                                u8c += char_u((p = p.minus(1)).at(0));
                            }
                            else
                            {
                                u8c = char_u((p = p.minus(1)).at(0));
                                u8c += (char_u((p = p.minus(1)).at(0)) << 8);
                            }
                            if ((fio_flags & FIO_UTF16) != 0 && 0xdc00 <= u8c && u8c <= 0xdfff)
                            {
                                if (BEQ(p, ptr))
                                {
                                    /* Missing leading word. */
                                    if (can_retry)
                                    {
                                        /* Retry reading with another conversion.
                                        * Use next item from 'fileencodings'. */
                                        advance_fenc = true;
                                        file_rewind = true;
                                        continue retry;
                                    }
                                    if (conv_error == 0)
                                        conv_error = readfile_linenr(linecnt, ptr, p);
                                    if (bad_char_behavior == BAD_DROP)
                                        continue;
                                    if (bad_char_behavior != BAD_KEEP)
                                        u8c = bad_char_behavior;
                                }

                                /* Found second word of double-word,
                                 * get the first word and compute the resulting character. */
                                int u16c;
                                if ((fio_flags & FIO_ENDIAN_L) != 0)
                                {
                                    u16c = (char_u((p = p.minus(1)).at(0)) << 8);
                                    u16c += char_u((p = p.minus(1)).at(0));
                                }
                                else
                                {
                                    u16c = char_u((p = p.minus(1)).at(0));
                                    u16c += (char_u((p = p.minus(1)).at(0)) << 8);
                                }
                                u8c = 0x10000 + ((u16c & 0x3ff) << 10) + (u8c & 0x3ff);

                                /* Check if the word is indeed a leading word. */
                                if (u16c < 0xd800 || 0xdbff < u16c)
                                {
                                    if (can_retry)
                                    {
                                        /* Retry reading with another conversion.
                                        * Use next item from 'fileencodings'. */
                                        advance_fenc = true;
                                        file_rewind = true;
                                        continue retry;
                                    }
                                    if (conv_error == 0)
                                        conv_error = readfile_linenr(linecnt, ptr, p);
                                    if (bad_char_behavior == BAD_DROP)
                                        continue;
                                    if (bad_char_behavior != BAD_KEEP)
                                        u8c = bad_char_behavior;
                                }
                            }
                        }
                        else if ((fio_flags & FIO_UCS4) != 0)
                        {
                            if ((fio_flags & FIO_ENDIAN_L) != 0)
                            {
                                u8c = (char_u((p = p.minus(1)).at(0)) << 24);
                                u8c += (char_u((p = p.minus(1)).at(0)) << 16);
                                u8c += (char_u((p = p.minus(1)).at(0)) << 8);
                                u8c += char_u((p = p.minus(1)).at(0));
                            }
                            else /* big endian */
                            {
                                u8c = char_u((p = p.minus(1)).at(0));
                                u8c += (char_u((p = p.minus(1)).at(0)) << 8);
                                u8c += (char_u((p = p.minus(1)).at(0)) << 16);
                                u8c += (char_u((p = p.minus(1)).at(0)) << 24);
                            }
                        }
                        else /* UTF-8 */
                        {
                            if (char_u((p = p.minus(1)).at(0)) < 0x80)
                                u8c = char_u(p.at(0));
                            else
                            {
                                int len = us_head_off(ptr, p);
                                p = p.minus(len);
                                u8c = us_ptr2char(p);
                                if (len == 0)
                                {
                                    /* Not a valid UTF-8 character, retry with another 'fenc'
                                     * when possible, otherwise just report the error. */
                                    if (can_retry)
                                    {
                                        /* Retry reading with another conversion.
                                        * Use next item from 'fileencodings'. */
                                        advance_fenc = true;
                                        file_rewind = true;
                                        continue retry;
                                    }
                                    if (conv_error == 0)
                                        conv_error = readfile_linenr(linecnt, ptr, p);
                                    if (bad_char_behavior == BAD_DROP)
                                        continue;
                                    if (bad_char_behavior != BAD_KEEP)
                                        u8c = bad_char_behavior;
                                }
                            }
                        }

                        dest = dest.minus(utf_char2len(u8c));
                        utf_char2bytes(u8c, dest);
                    }

                    /* move the linerest to before the converted characters */
                    line_start = dest.minus(linerest);
                    BCOPY(line_start, buffer, linerest);
                    size = BDIFF(ptr.plus(real_size), dest);
                    ptr = dest;
                }
                else if (!curbuf.b_p_bin[0])
                {
                    boolean incomplete_tail = false;

                    /* Reading UTF-8: Check if the bytes are valid UTF-8. */
                    Bytes p;
                    for (p = ptr; ; p = p.plus(1))
                    {
                        int todo = BDIFF(ptr.plus(size), p);
                        if (todo <= 0)
                            break;

                        if (0x80 <= char_u(p.at(0)))
                        {
                            /* A length of 1 means it's an illegal byte.  Accept
                             * an incomplete character at the end though, the next
                             * read() will get the next bytes, we'll check it then. */
                            int l = us_ptr2len_len(p, todo);
                            if (todo < l && !incomplete_tail)
                            {
                                /* Avoid retrying with a different encoding when
                                 * a truncated file is more likely, or attempting
                                 * to read the rest of an incomplete sequence when
                                 * we have already done so. */
                                if (BLT(ptr, p) || 0 < filesize)
                                    incomplete_tail = true;
                                /* Incomplete byte sequence, move it to conv_rest[]
                                 * and try to read the rest of it, unless we've
                                 * already done so. */
                                if (BLT(ptr, p))
                                {
                                    conv_restlen = todo;
                                    BCOPY(conv_rest, p, conv_restlen);
                                    size -= conv_restlen;
                                    break;
                                }
                            }
                            if (l == 1 || todo < l)
                            {
                                /* Illegal byte.  If we can try another encoding
                                 * do that, unless at EOF where a truncated
                                 * file is more likely than a conversion error. */
                                if (can_retry && !incomplete_tail)
                                    break;

                                /* Remember the first linenr with an illegal byte. */
                                if (conv_error == 0 && illegal_byte == 0)
                                    illegal_byte = readfile_linenr(linecnt, ptr, p);

                                /* Drop, keep or replace the bad byte. */
                                if (bad_char_behavior == BAD_DROP)
                                {
                                    BCOPY(p, 0, p, 1, todo - 1);
                                    p = p.minus(1);
                                    --size;
                                }
                                else if (bad_char_behavior != BAD_KEEP)
                                    p.be(0, bad_char_behavior);
                            }
                            else
                                p = p.plus(l - 1);
                        }
                    }
                    if (BLT(p, ptr.plus(size)) && !incomplete_tail)
                    {
                        /* Detected a UTF-8 error.
                         * Retry reading with another conversion.
                         * Use next item from 'fileencodings'. */
                        advance_fenc = true;
                        file_rewind = true;
                        continue retry;
                    }
                }

                /* count the number of characters (after conversion!) */
                filesize += size;

                /*
                 * when reading the first part of a file: guess EOL type
                 */
                if (fileformat == EOL_UNKNOWN)
                {
                    /* First try finding a NL, for Dos and Unix. */
                    if (try_dos || try_unix != 0)
                    {
                        /* Reset the carriage return counter. */
                        if (try_mac != 0)
                            try_mac = 1;

                        Bytes p;
                        for (p = ptr; BLT(p, ptr.plus(size)); p = p.plus(1))
                        {
                            if (p.at(0) == NL)
                            {
                                if (try_unix == 0 || (try_dos && BLT(ptr, p) && p.at(-1) == CAR))
                                    fileformat = EOL_DOS;
                                else
                                    fileformat = EOL_UNIX;
                                break;
                            }
                            else if (p.at(0) == CAR && try_mac != 0)
                                try_mac++;
                        }

                        /* Don't give in to EOL_UNIX if EOL_MAC is more likely. */
                        if (fileformat == EOL_UNIX && try_mac != 0)
                        {
                            /* Need to reset the counters when retrying 'fenc'. */
                            try_mac = 1;
                            try_unix = 1;
                            for ( ; BLE(ptr, p) && p.at(0) != CAR; p = p.minus(1))
                                ;
                            if (BLE(ptr, p))
                            {
                                for (p = ptr; BLT(p, ptr.plus(size)); p = p.plus(1))
                                {
                                    if (p.at(0) == NL)
                                        try_unix++;
                                    else if (p.at(0) == CAR)
                                        try_mac++;
                                }
                                if (try_unix < try_mac)
                                    fileformat = EOL_MAC;
                            }
                        }
                        else if (fileformat == EOL_UNKNOWN && try_mac == 1)
                            /* Looking for CR but found no end-of-line markers at all:
                             * use the default format. */
                            fileformat = default_fileformat();
                    }

                    /* No NL found: may use Mac format. */
                    if (fileformat == EOL_UNKNOWN && try_mac != 0)
                        fileformat = EOL_MAC;

                    /* Still nothing found?  Use first format in 'ffs'. */
                    if (fileformat == EOL_UNKNOWN)
                        fileformat = default_fileformat();

                    /* if editing a new file: may set "p_tx" and "p_ff" */
                    if (set_options)
                        set_fileformat(fileformat, OPT_LOCAL);
                }

                /*
                 * This loop is executed once for every character read.
                 * Keep it fast!
                 */
                if (fileformat == EOL_MAC)
                {
                    for ( ; 0 < size--; ptr = ptr.plus(1))
                    {
                        /* catch most common case first */
                        byte c = ptr.at(0);
                        if (c != NUL && c != CAR && c != NL)
                            continue;
                        if (c == NUL)
                            ptr.be(0, NL);                  /* NULs are replaced by newlines! */
                        else if (c == NL)
                            ptr.be(0, CAR);                 /* NLs are replaced by CRs! */
                        else
                        {
                            if (skip_count == 0)
                            {
                                ptr.be(0, NUL);             /* end of line */
                                int len = BDIFF(ptr, line_start) + 1;
                                if (!ml_append(lnum, line_start, len, newfile))
                                {
                                    error = true;
                                    break;
                                }
                                if (read_undo_file)
                                    sha256_update(sha_ctx, line_start, len);
                                lnum++;
                                if (--read_count == 0)
                                {
                                    error = true;       /* break loop */
                                    line_start = ptr;   /* nothing left to write */
                                    break;
                                }
                            }
                            else
                                --skip_count;
                            line_start = ptr.plus(1);
                        }
                    }
                }
                else
                {
                    for ( ; 0 < size--; ptr = ptr.plus(1))
                    {
                        byte c = ptr.at(0);
                        if (c != NUL && c != NL)            /* catch most common case */
                            continue;
                        if (c == NUL)
                            ptr.be(0, NL);                      /* NULs are replaced by newlines! */
                        else
                        {
                            if (skip_count == 0)
                            {
                                ptr.be(0, NUL);                 /* end of line */
                                int len = BDIFF(ptr, line_start) + 1;
                                if (fileformat == EOL_DOS)
                                {
                                    if (ptr.at(-1) == CAR)     /* remove CR */
                                    {
                                        ptr.be(-1, NUL);
                                        --len;
                                    }
                                    /*
                                     * Reading in Dos format, but no CR-LF found!
                                     * When 'fileformats' includes "unix", delete all
                                     * the lines read so far and start all over again.
                                     * Otherwise give an error message later.
                                     */
                                    else if (ff_error != EOL_DOS)
                                    {
                                        if (try_unix != 0
                                            && !read_stdin
                                            && (read_buffer || libc.lseek(fd, 0L, SEEK_SET) == 0))
                                        {
                                            fileformat = EOL_UNIX;
                                            if (set_options)
                                                set_fileformat(EOL_UNIX, OPT_LOCAL);
                                            file_rewind = true;
                                            keep_fileformat = true;
                                            continue retry;
                                        }
                                        ff_error = EOL_DOS;
                                    }
                                }
                                if (!ml_append(lnum, line_start, len, newfile))
                                {
                                    error = true;
                                    break;
                                }
                                if (read_undo_file)
                                    sha256_update(sha_ctx, line_start, len);
                                lnum++;
                                if (--read_count == 0)
                                {
                                    error = true;       /* break loop */
                                    line_start = ptr;   /* nothing left to write */
                                    break;
                                }
                            }
                            else
                                --skip_count;
                            line_start = ptr.plus(1);
                        }
                    }
                }
                linerest = BDIFF(ptr, line_start);
                ui_breakcheck();
            }

            break;
        }

        /* not an error, max. number of lines reached */
        if (error && read_count == 0)
            error = false;

        /*
         * If we get EOF in the middle of a line, note the fact and
         * complete the line ourselves.
         * In Dos format ignore a trailing CTRL-Z, unless 'binary' set.
         */
        if (!error
                && !got_int
                && linerest != 0
                && !(!curbuf.b_p_bin[0]
                    && fileformat == EOL_DOS
                    && line_start.at(0) == Ctrl_Z
                    && BEQ(ptr, line_start.plus(1))))
        {
            /* remember for when writing */
            if (set_options)
                curbuf.b_p_eol[0] = false;
            ptr.be(0, NUL);
            int len = BDIFF(ptr, line_start) + 1;
            if (!ml_append(lnum, line_start, len, newfile))
                error = true;
            else
            {
                if (read_undo_file)
                    sha256_update(sha_ctx, line_start, len);
                read_no_eol_lnum = ++lnum;
            }
        }

        if (set_options)
            save_file_ff(curbuf);                   /* remember the current file format */

        /* If editing a new file: set 'fenc' for the current buffer.
         * Also for ":read ++edit file". */
        if (set_options)
            set_string_option_direct(u8("fenc"), -1, fenc, OPT_FREE|OPT_LOCAL, 0);

        if (!read_buffer && !read_stdin)
            libc.close(fd);                              /* errors are ignored */
        else
        {
            int fdflags = libc.fcntl(fd, F_GETFD);
            if (0 <= fdflags && (fdflags & FD_CLOEXEC) == 0)
                libc.fcntl(fd, F_SETFD, fdflags | FD_CLOEXEC);
        }

        if (read_stdin)
        {
            /* Use stderr for stdin, makes shell commands work. */
            libc.close(0);
            libc.dup(2);
        }

        --no_wait_return;                           /* may wait for return now */

        /* need to delete the last line, which comes from the empty buffer */
        if (newfile && wasempty && (curbuf.b_ml.ml_flags & ML_EMPTY) == 0)
        {
            ml_delete(curbuf.b_ml.ml_line_count, false);
            --linecnt;
        }
        linecnt = curbuf.b_ml.ml_line_count - linecnt;
        if (filesize == 0)
            linecnt = 0;
        if (newfile || read_buffer)
            redraw_curbuf_later(NOT_VALID);
        else if (linecnt != 0)                           /* appended at least one line */
            appended_lines_mark(from, linecnt);

        /*
         * If we were reading from the same terminal as where messages go,
         * the screen will have been messed up.
         * Switch on raw mode now and clear the screen.
         */
        if (read_stdin)
        {
            settmode(TMODE_RAW);                    /* set to raw mode */
            starttermcap();
            screenclear();
        }

        if (got_int)
        {
            if ((flags & READ_DUMMY) == 0)
            {
                filemess(sfname, e_interr, 0);
                if (newfile)
                    curbuf.b_p_ro[0] = true;           /* must use "w!" now */
            }
            msg_scroll = msg_save;
            return true;                            /* an interrupt isn't really an error */
        }

        if (!filtering && (flags & READ_DUMMY) == 0)
        {
            msg_add_fname(sfname);                  /* "fname" in ioBuff with quotes */
            boolean c = false;

            if (S_ISFIFO(perm))                     /* fifo or socket */
            {
                STRCAT(ioBuff, u8("[fifo/socket]"));
                c = true;
            }
            if (curbuf.b_p_ro[0])
            {
                STRCAT(ioBuff, shortmess(SHM_RO) ? u8("[RO]") : u8("[readonly]"));
                c = true;
            }
            if (read_no_eol_lnum != 0)
            {
                msg_add_eol();
                c = true;
            }
            if (ff_error == EOL_DOS)
            {
                STRCAT(ioBuff, u8("[CR missing]"));
                c = true;
            }
            if (notconverted)
            {
                STRCAT(ioBuff, u8("[NOT converted]"));
                c = true;
            }
            else if (converted)
            {
                STRCAT(ioBuff, u8("[converted]"));
                c = true;
            }
            if (conv_error != 0)
            {
                libC.sprintf(ioBuff.plus(strlen(ioBuff)), u8("[CONVERSION ERROR in line %ld]"), conv_error);
                c = true;
            }
            else if (0 < illegal_byte)
            {
                libC.sprintf(ioBuff.plus(strlen(ioBuff)), u8("[ILLEGAL BYTE in line %ld]"), illegal_byte);
                c = true;
            }
            else if (error)
            {
                STRCAT(ioBuff, u8("[READ ERRORS]"));
                c = true;
            }
            if (msg_add_fileformat(fileformat))
                c = true;
            msg_add_lines(c, linecnt, filesize);

            keep_msg = null;
            msg_scrolled_ign = true;
            Bytes p = msg_trunc_attr(ioBuff, false, 0);
            if (read_stdin || read_buffer || restart_edit != 0
                    || (msg_scrolled != 0 && !need_wait_return))
                /* Need to repeat the message after redrawing when:
                 * - When reading from stdin (the screen will be cleared next).
                 * - When restart_edit is set (otherwise there will be a delay before redrawing).
                 * - When the screen was scrolled but there is no wait-return prompt. */
                set_keep_msg(p, 0);
            msg_scrolled_ign = false;
        }

        /* with errors writing the file requires ":w!" */
        if (newfile && (error
                    || conv_error != 0
                    || (0 < illegal_byte && bad_char_behavior != BAD_KEEP)))
            curbuf.b_p_ro[0] = true;

        u_clearline();                  /* cannot use "U" command after adding lines */

        /*
         * In Ex mode: cursor at last new line.
         * Otherwise: cursor at first new line.
         */
        if (exmode_active != 0)
            curwin.w_cursor.lnum = from + linecnt;
        else
            curwin.w_cursor.lnum = from + 1;
        check_cursor_lnum();
        beginline(BL_WHITE | BL_FIX);               /* on first non-blank */

        /*
         * Set '[ and '] marks to the newly read lines.
         */
        curbuf.b_op_start.lnum = from + 1;
        curbuf.b_op_start.col = 0;
        curbuf.b_op_end.lnum = from + linecnt;
        curbuf.b_op_end.col = 0;

        msg_scroll = msg_save;

        /*
         * Trick: We remember if the last line of the read didn't have an eol even
         * when 'binary' is off, for when writing it again with 'binary' on.  This is
         * required for * ":autocmd FileReadPost *.gz set bin|'[,']!gunzip" to work.
         */
        curbuf.b_no_eol_lnum = read_no_eol_lnum;

        /* When reloading a buffer put the cursor at the first line that is different. */
        if ((flags & READ_KEEP_UNDO) != 0)
            u_find_first_changed();

        /*
         * When opening a new file locate undo info and read it.
         */
        if (read_undo_file)
        {
            Bytes hash = new Bytes(UNDO_HASH_SIZE);

            sha256_finish(sha_ctx, hash);
            u_read_undo(null, hash, fname);
        }

        if (!read_stdin && !read_buffer)
        {
            boolean m = msg_scroll;
            int n = msg_scrolled;

            /* Save the fileformat now, otherwise the buffer will be considered
             * modified if the format/encoding was automatically detected. */
            if (set_options)
                save_file_ff(curbuf);

            /*
             * The output from the autocommands should not overwrite anything and
             * should not be overwritten: set msg_scroll, restore its value if no
             * output was done.
             */
            msg_scroll = true;
            if (filtering)
                apply_autocmds_exarg(EVENT_FILTERREADPOST, null, sfname, false, curbuf, eap);
            else if (newfile)
                apply_autocmds_exarg(EVENT_BUFREADPOST, null, sfname, false, curbuf, eap);
            else
                apply_autocmds_exarg(EVENT_FILEREADPOST, sfname, sfname, false, null, eap);
            if (msg_scrolled == n)
                msg_scroll = m;
            if (aborting())             /* autocmds may abort script processing */
                return false;
        }

        return true;
    }

    /*
     * From the current line count and characters read after that, estimate the line number where we are now.
     * Used for error messages that include a line number.
     */
    /*private*/ static long readfile_linenr(long linecnt, Bytes p, Bytes endp)
        /* linecnt: line count before reading more bytes */
        /* p: start of more bytes read */
        /* endp: end of more bytes read */
    {
        long lnum = curbuf.b_ml.ml_line_count - linecnt + 1;
        for (Bytes s = p; BLT(s, endp); s = s.plus(1))
            if (s.at(0) == (byte)'\n')
                lnum++;
        return lnum;
    }

    /*
     * ... used for calling readfile()
     */
    /*private*/ static void prep_exarg(exarg_C eap, buffer_C buf)
    {
        eap.cmd = STRDUP(u8("e"));

        eap.bad_char = buf.b_bad_char;
        eap.force_bin = buf.b_p_bin[0] ? FORCE_BIN : FORCE_NOBIN;
        eap.read_edit = false;
        eap.forceit = false;
    }

    /*
     * Set default or forced 'fileformat' and 'binary'.
     */
    /*private*/ static void set_file_options(boolean set_options, exarg_C eap)
    {
        /* set default 'fileformat' */
        if (set_options && p_ffs[0].at(0) != NUL)
            set_fileformat(default_fileformat(), OPT_LOCAL);

        /* set or reset 'binary' */
        if (eap != null && eap.force_bin != 0)
        {
            boolean oldval = curbuf.b_p_bin[0];

            curbuf.b_p_bin[0] = (eap.force_bin == FORCE_BIN);
            set_options_bin(oldval, curbuf.b_p_bin[0], OPT_LOCAL);
        }
    }

    /*
     * Find next fileencoding to use from 'fileencodings'.
     * "pp" points to fenc_next.  It's advanced to the next item.
     * When there are no more items, an empty string is returned and *pp is set to null.
     * When *pp is not set to null, the result is in allocated memory.
     */
    /*private*/ static Bytes next_fenc(Bytes[] pp)
    {
        if (pp[0].at(0) == NUL)
        {
            pp[0] = null;
            return u8("");
        }

        Bytes r;

        Bytes p = vim_strchr(pp[0], ',');
        if (p == null)
        {
            r = enc_canonize(pp[0]);
            pp[0] = pp[0].plus(strlen(pp[0]));
        }
        else
        {
            r = STRNDUP(pp[0], BDIFF(p, pp[0]));
            pp[0] = p.plus(1);
            if (r != null)
                r = enc_canonize(r);
        }

        return r;
    }

    /*
     * Return true if a file appears to be read-only from the file permissions.
     */
    /*private*/ static boolean check_file_readonly(Bytes fname, int perm)
        /* fname: full path to file */
        /* perm: known permissions on file */
    {
        return ((perm & 0222) == 0 || libC.access(fname, W_OK) != 0);
    }

    /*private*/ static Bytes err_readonly = u8("is read-only (cannot override: \"W\" in 'cpoptions')");

    /*
     * buf_write() - write to file "fname" lines "start" through "end"
     *
     * We do our own buffering here because fwrite() is so slow.
     *
     * If "forceit" is true, we don't care for errors when attempting backups.
     * In case of an error everything possible is done to restore the original
     * file.  But when "forceit" is true, we risk losing it.
     *
     * When "reset_changed" is true and "append" == false and "start" == 1 and
     * "end" == curbuf.b_ml.ml_line_count, reset "curbuf.b_changed".
     *
     * This function must NOT use nameBuff (because it's called by autowrite()).
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean buf_write(buffer_C buf, Bytes fname, Bytes sfname, long start, long end, exarg_C eap, boolean append, boolean forceit, boolean reset_changed, boolean filtering)
        /* eap: for forced 'ff' and 'fenc', can be null! */
        /* append: append to the file */
    {
        boolean retval = true;

        boolean msg_save = msg_scroll;
        boolean prev_got_int = got_int;

        long old_line_count = buf.b_ml.ml_line_count;
        boolean whole = (start == 1 && end == old_line_count);  /* writing everything */

        if (fname == null || fname.at(0) == NUL)                     /* safety check */
            return false;

        if (buf.b_ml.ml_mfp == null)
        {
            /* This can happen during startup when there is a stray "w" in the vimrc file. */
            emsg(e_emptybuf);
            return false;
        }

        /*
         * Disallow writing from .exrc and .vimrc in current directory for security reasons.
         */
        if (check_secure())
            return false;

        /* Avoid a crash for a long name. */
        if (MAXPATHL <= strlen(fname))
        {
            emsg(e_longname);
            return false;
        }

        /* After writing a file changedtick changes but we don't want to display the line. */
        ex_no_reprint = true;

        /*
         * If there is no file name yet, use the one for the written file.
         * BF_NOTEDITED is set to reflect this (in case the write fails).
         * Don't do this when the write is for a filter command.
         * Don't do this when appending.
         * Only do this when 'cpoptions' contains the 'F' flag.
         */
        if (buf.b_ffname == null
                && reset_changed
                && whole
                && buf == curbuf
                && !filtering
                && (!append || vim_strbyte(p_cpo[0], CPO_FNAMEAPP) != null)
                && vim_strbyte(p_cpo[0], CPO_FNAMEW) != null)
        {
            if (set_rw_fname(fname, sfname) == false)
                return false;
            buf = curbuf;               /* just in case autocmds made "buf" invalid */
        }

        if (sfname == null)
            sfname = fname;
        /*
         * For Unix: Use the short file name whenever possible.
         * Avoids problems with networks and when directory names are changed.
         */
        Bytes ffname = fname;          /* remember full "fname" */
        fname = sfname;

        boolean overwriting;            /* true if writing over original */
        if (buf.b_ffname != null && STRCMP(ffname, buf.b_ffname) == 0)
            overwriting = true;
        else
            overwriting = false;

        if (exiting)
            settmode(TMODE_COOK);       /* when exiting allow typeahead now */

        no_wait_return++;               /* don't wait for return yet */

        /*
         * Set '[ and '] marks to the lines to be written.
         */
        buf.b_op_start.lnum = start;
        buf.b_op_start.col = 0;
        buf.b_op_end.lnum = end;
        buf.b_op_end.col = 0;

        {
            boolean did_cmd = false;
            boolean nofile_err = false;
            boolean empty_memline = (buf.b_ml.ml_mfp == null);

            /*
             * Apply PRE autocommands.
             * Set curbuf to the buffer to be written.
             * Careful: the autocommands may call buf_write() recursively!
             */
            boolean buf_ffname = BEQ(ffname, buf.b_ffname);
            boolean buf_sfname = BEQ(sfname, buf.b_sfname);
            boolean buf_fname_f = BEQ(fname, buf.b_ffname);
            boolean buf_fname_s = BEQ(fname, buf.b_sfname);

            /* set curwin/curbuf to buf and save a few things */
            aco_save_C aco = new aco_save_C();
            aucmd_prepbuf(aco, buf);

            if (append)
            {
                did_cmd = apply_autocmds_exarg(EVENT_FILEAPPENDCMD, sfname, sfname, false, curbuf, eap);
                if (!did_cmd)
                    apply_autocmds_exarg(EVENT_FILEAPPENDPRE, sfname, sfname, false, curbuf, eap);
            }
            else if (filtering)
            {
                apply_autocmds_exarg(EVENT_FILTERWRITEPRE, null, sfname, false, curbuf, eap);
            }
            else if (reset_changed && whole)
            {
                boolean was_changed = curbufIsChanged();

                did_cmd = apply_autocmds_exarg(EVENT_BUFWRITECMD, sfname, sfname, false, curbuf, eap);
                if (did_cmd)
                {
                    if (was_changed && !curbufIsChanged())
                    {
                        /* Written everything correctly and BufWriteCmd has reset 'modified':
                         * correct the undo information so that an undo now sets 'modified'. */
                        u_unchanged(curbuf);
                        u_update_save_nr(curbuf);
                    }
                }
                else
                {
                    apply_autocmds_exarg(EVENT_BUFWRITEPRE, sfname, sfname, false, curbuf, eap);
                }
            }
            else
            {
                did_cmd = apply_autocmds_exarg(EVENT_FILEWRITECMD, sfname, sfname, false, curbuf, eap);
                if (!did_cmd)
                    apply_autocmds_exarg(EVENT_FILEWRITEPRE, sfname, sfname, false, curbuf, eap);
            }

            /* restore curwin/curbuf and a few other things */
            aucmd_restbuf(aco);

            /*
             * In three situations we return here and don't write the file:
             * 1. the autocommands deleted or unloaded the buffer.
             * 2. The autocommands abort script processing.
             * 3. If one of the "Cmd" autocommands was executed.
             */
            if (!buf_valid(buf))
                buf = null;
            if (buf == null || (buf.b_ml.ml_mfp == null && !empty_memline) || did_cmd || nofile_err || aborting())
            {
                --no_wait_return;
                msg_scroll = msg_save;
                if (nofile_err)
                    emsg(u8("E676: No matching autocommands for acwrite buffer"));

                if (nofile_err || aborting())
                    /* An aborting error, interrupt or exception in the autocommands. */
                    return false;
                if (did_cmd)
                {
                    if (buf == null)
                        /* The buffer was deleted.  We assume it was written (can't retry anyway). */
                        return true;
                    if (overwriting)
                    {
                        /* Assume the buffer was written, update the timestamp. */
                        ml_timestamp(buf);
                        if (append)
                            buf.b_flags &= ~BF_NEW;
                        else
                            buf.b_flags &= ~BF_WRITE_MASK;
                    }
                    if (reset_changed && buf.b_changed[0] && !append
                            && (overwriting || vim_strbyte(p_cpo[0], CPO_PLUS) != null))
                        /* Buffer still changed, the autocommands didn't work properly. */
                        return false;

                    return true;
                }
                if (!aborting())
                    emsg(u8("E203: Autocommands deleted or unloaded buffer to be written"));
                return false;
            }

            /*
             * The autocommands may have changed the number of lines in the file.
             * When writing the whole file, adjust the end.
             * When writing part of the file, assume that the autocommands only
             * changed the number of lines that are to be written (tricky!).
             */
            if (buf.b_ml.ml_line_count != old_line_count)
            {
                if (whole)                                              /* write all */
                    end = buf.b_ml.ml_line_count;
                else if (buf.b_ml.ml_line_count > old_line_count)       /* more lines */
                    end += buf.b_ml.ml_line_count - old_line_count;
                else                                                    /* less lines */
                {
                    end -= old_line_count - buf.b_ml.ml_line_count;
                    if (end < start)
                    {
                        --no_wait_return;
                        msg_scroll = msg_save;
                        emsg(u8("E204: Autocommand changed number of lines in unexpected way"));
                        return false;
                    }
                }
            }

            /*
             * The autocommands may have changed the name of the buffer,
             * which may be kept in "fname", "ffname" and "sfname".
             */
            if (buf_ffname)
                ffname = buf.b_ffname;
            if (buf_sfname)
                sfname = buf.b_sfname;
            if (buf_fname_f)
                fname = buf.b_ffname;
            if (buf_fname_s)
                fname = buf.b_sfname;
        }

        if (shortmess(SHM_OVER) && !exiting)
            msg_scroll = false;                 /* overwrite previous file message */
        else
            msg_scroll = true;                  /* don't overwrite previous file message */
        if (!filtering)
            filemess(fname, u8(""), 0);             /* show that we are busy */
        msg_scroll = false;                     /* always overwrite the file message now */

        Bytes buffer = new Bytes(BUFSIZE);
        int bufsize = BUFSIZE;

        Bytes wfname = null;                   /* name of file to write to */
        boolean newfile = false;                /* true if file doesn't exist yet */
        boolean file_readonly = false;          /* overwritten file is read-only */
        boolean made_writable = false;          /* 'w' bit has been set */
        boolean no_eol = false;                 /* no end-of-line written */
        boolean device = false;                 /* writing to a device */

        boolean notconverted = false;
        int wb_flags = 0;

        Bytes errmsg = null;
        Bytes errnum = null;

        bw_info_C write_info = new bw_info_C(); /* info for buf_write_bytes() */
        write_info.bw_conv_buf = null;          /* must init bw_conv_buf before jumping to "fail" */
        write_info.bw_conv_error = false;
        write_info.bw_conv_error_lnum = 0;
        write_info.bw_restlen = 0;

        context_sha256_C sha_ctx = new context_sha256_C();
        boolean write_undo_file = false;

        nofail:
        {
            fail:
            {
                /*
                 * Get information about original file (if there is one).
                 */
                stat_C st_old = new stat_C();
                st_old.st_dev(0);
                st_old.st_ino(0);
                int perm = -1;                         /* file permissions */
                if (libC.stat(fname, st_old) < 0)
                    newfile = true;
                else
                {
                    perm = st_old.st_mode();
                    if (!S_ISREG(st_old.st_mode()))     /* not a file */
                    {
                        if (S_ISDIR(st_old.st_mode()))
                        {
                            errnum = u8("E502: ");
                            errmsg = u8("is a directory");
                            break fail;
                        }
                        if (mch_nodetype(fname) != NODE_WRITABLE)
                        {
                            errnum = u8("E503: ");
                            errmsg = u8("is not a file or writable device");
                            break fail;
                        }
                        /* It's a device of some kind (or a fifo) which we can write to
                         * but for which we can't make a backup. */
                        device = true;
                        newfile = true;
                        perm = -1;
                    }
                }

                if (!device && !newfile)
                {
                    /*
                     * Check if the file is really writable
                     * (when renaming the file to make a backup we won't discover it later).
                     */
                    file_readonly = check_file_readonly(fname, perm);

                    if (!forceit && file_readonly)
                    {
                        if (vim_strbyte(p_cpo[0], CPO_FWRITE) != null)
                        {
                            errnum = u8("E504: ");
                            errmsg = err_readonly;
                        }
                        else
                        {
                            errnum = u8("E505: ");
                            errmsg = u8("is read-only (add ! to override)");
                        }
                        break fail;
                    }

                    /*
                     * Check if the timestamp hasn't changed since reading the file.
                     */
                    if (overwriting)
                    {
                        retval = check_mtime(buf, st_old);
                        if (retval == false)
                            break fail;
                    }
                }

                /*
                 * Save the value of got_int and reset it.  We don't want a previous interruption
                 * cancel writing, only hitting CTRL-C while writing should abort it.
                 */
                prev_got_int = got_int;
                got_int = false;

                /* Mark the buffer as 'being saved' to prevent changed buffer warnings. */
                buf.b_saving = true;

                /* When using ":w!" and the file was read-only: make it writable. */
                if (forceit && 0 <= perm && (perm & 0200) == 0 && st_old.st_uid() == libc.getuid()
                                                                        && vim_strbyte(p_cpo[0], CPO_FWRITE) == null)
                {
                    perm |= 0200;
                    mch_setperm(fname, perm);
                    made_writable = true;
                }

                /* When using ":w!" and writing to the current file, 'readonly' makes no sense;
                 * reset it, unless 'Z' appears in 'cpoptions'. */
                if (forceit && overwriting && vim_strbyte(p_cpo[0], CPO_KEEPRO) == null)
                {
                    buf.b_p_ro[0] = false;
                    status_redraw_all();            /* redraw status lines later */
                }

                if (end > buf.b_ml.ml_line_count)
                    end = buf.b_ml.ml_line_count;
                if ((buf.b_ml.ml_flags & ML_EMPTY) != 0)
                    start = end + 1;

                /* Default: write the file directly. */
                wfname = fname;

                Bytes fenc = buf.b_p_fenc[0];         /* effective 'fileencoding' */

                /*
                 * Check if the file needs to be converted.
                 */
                boolean converted = need_conversion(fenc);

                /*
                 * Check if UTF-8 to UCS-2/4 or Latin1 conversion needs to be done.
                 * Or Latin1 to Unicode conversion.  This is handled in buf_write_bytes().
                 * Prepare the flags for it and allocate bw_conv_buf when needed.
                 */
                if (converted)
                {
                    wb_flags = get_fio_flags(fenc);
                    if ((wb_flags & (FIO_UCS2 | FIO_UCS4 | FIO_UTF16 | FIO_UTF8)) != 0)
                    {
                        /* Need to allocate a buffer to translate into. */
                        if ((wb_flags & (FIO_UCS2 | FIO_UTF16 | FIO_UTF8)) != 0)
                            write_info.bw_conv_buflen = bufsize * 2;
                        else /* FIO_UCS4 */
                            write_info.bw_conv_buflen = bufsize * 4;
                        write_info.bw_conv_buf = new Bytes(write_info.bw_conv_buflen);
                    }
                }

                if (converted && wb_flags == 0 && BEQ(wfname, fname))
                {
                    if (!forceit)
                    {
                        errmsg = u8("E213: Cannot convert (add ! to write without conversion)");

                        stat_C st = new stat_C();

                        /* if original file no longer exists give an extra warning */
                        if (!newfile && libC.stat(fname, st) < 0)
                            end = 0;

                        break fail;
                    }
                    notconverted = true;
                }

                /*
                 * Open the file "wfname" for writing.
                 * We may try to open the file twice: If we can't write to the
                 * file and forceit is true we delete the existing file and try to create
                 * a new one.  If this still fails we may have lost the original file!
                 * (this may happen when the user reached his quotum for number of files).
                 * Appending will fail if the file does not exist and forceit is false.
                 */
                int fd;
                while ((fd = libC.open(wfname,
                    O_WRONLY | (append ? (forceit ? (O_APPEND | O_CREAT) : O_APPEND) : (O_CREAT | O_TRUNC)),
                    (perm < 0) ? 0666 : (perm & 0777))) < 0)
                {
                    stat_C st = new stat_C();

                    /*
                     * A forced write will try to create a new file if the old one is still readonly.
                     * This may also happen when the directory is read-only.
                     * In that case the unlink() will fail.
                     */
                    if (errmsg == null)
                    {
                        /* Don't delete the file when it's a hard or symbolic link. */
                        if ((!newfile && 1 < st_old.st_nlink())
                                || (libC.lstat(fname, st) == 0 && (st.st_dev() != st_old.st_dev() || st.st_ino() != st_old.st_ino())))
                            errmsg = u8("E166: Can't open linked file for writing");
                        else
                        {
                            errmsg = u8("E212: Can't open file for writing");
                            if (forceit && vim_strbyte(p_cpo[0], CPO_FWRITE) == null && 0 <= perm)
                            {
                                /* we write to the file, thus it should be marked writable after all */
                                if ((perm & 0200) == 0)
                                    made_writable = true;
                                perm |= 0200;
                                if (st_old.st_uid() != libc.getuid() || st_old.st_gid() != libc.getgid())
                                    perm &= 0777;
                                if (!append)            /* don't remove when appending */
                                    libC.unlink(wfname);
                                continue;
                            }
                        }
                    }

                    /* if original file no longer exists give an extra warning */
                    if (!newfile && libC.stat(fname, st) < 0)
                        end = 0;

                    break fail;
                }
                errmsg = null;

                write_info.bw_fd = fd;

                write_info.bw_buf = buffer;
                long nchars = 0;

                boolean write_bin;
                /* use "++bin", "++nobin" or 'binary' */
                if (eap != null && eap.force_bin != 0)
                    write_bin = (eap.force_bin == FORCE_BIN);
                else
                    write_bin = buf.b_p_bin[0];

                /*
                 * The BOM is written just after the encryption magic number.
                 * Skip it when appending and the file already existed, the BOM only makes
                 * sense at the start of the file.
                 */
                if (buf.b_p_bomb[0] && !write_bin && (!append || perm < 0))
                {
                    write_info.bw_len = make_bom(buffer, fenc);
                    if (0 < write_info.bw_len)
                    {
                        /* don't convert, do encryption */
                        write_info.bw_flags = FIO_NOCONVERT | wb_flags;
                        if (buf_write_bytes(write_info) == false)
                            end = 0;
                        else
                            nchars += write_info.bw_len;
                    }
                }
                write_info.bw_start_lnum = start;

                write_undo_file = (buf.b_p_udf[0] && overwriting && !append && !filtering && reset_changed);
                if (write_undo_file)
                    /* Prepare for computing the hash value of the text. */
                    sha256_start(sha_ctx);

                write_info.bw_len = bufsize;
                write_info.bw_flags = wb_flags;
                int fileformat = get_fileformat_force(buf, eap);
                Bytes s = buffer;
                int len = 0;
                long lnum;
                for (lnum = start; lnum <= end; lnum++)
                {
                    /*
                     * The next while loop is done once for each character written.
                     * Keep it fast!
                     */
                    Bytes ptr = ml_get_buf(buf, lnum, false).minus(1);
                    if (write_undo_file)
                        sha256_update(sha_ctx, ptr.plus(1), strlen(ptr, 1) + 1);
                    byte c;
                    while ((c = (ptr = ptr.plus(1)).at(0)) != NUL)
                    {
                        if (c == NL)
                            s.be(0, NUL);               /* replace newlines with NULs */
                        else if (c == CAR && fileformat == EOL_MAC)
                            s.be(0, NL);                /* Mac: replace CRs with NLs */
                        else
                            s.be(0, c);
                        s = s.plus(1);
                        if (++len != bufsize)
                            continue;
                        if (buf_write_bytes(write_info) == false)
                        {
                            end = 0;                /* write error: break loop */
                            break;
                        }
                        nchars += bufsize;
                        s = buffer;
                        len = 0;
                        write_info.bw_start_lnum = lnum;
                    }
                    /* write failed or last line has no EOL: stop here */
                    if (end == 0
                            || (lnum == end
                                && write_bin
                                && (lnum == buf.b_no_eol_lnum
                                        || (lnum == buf.b_ml.ml_line_count && !buf.b_p_eol[0]))))
                    {
                        lnum++;                     /* written the line, count it */
                        no_eol = true;
                        break;
                    }
                    if (fileformat == EOL_UNIX)
                        (s = s.plus(1)).be(-1, NL);
                    else
                    {
                        (s = s.plus(1)).be(-1, CAR);                 /* EOL_MAC or EOL_DOS: write CR */
                        if (fileformat == EOL_DOS)  /* write CR-NL */
                        {
                            if (++len == bufsize)
                            {
                                if (buf_write_bytes(write_info) == false)
                                {
                                    end = 0;        /* write error: break loop */
                                    break;
                                }
                                nchars += bufsize;
                                s = buffer;
                                len = 0;
                            }
                            (s = s.plus(1)).be(-1, NL);
                        }
                    }
                    if (++len == bufsize && end != 0)
                    {
                        if (buf_write_bytes(write_info) == false)
                        {
                            end = 0;                /* write error: break loop */
                            break;
                        }
                        nchars += bufsize;
                        s = buffer;
                        len = 0;

                        ui_breakcheck();
                        if (got_int)
                        {
                            end = 0;                /* Interrupted, break loop */
                            break;
                        }
                    }
                }
                if (0 < len && 0 < end)
                {
                    write_info.bw_len = len;
                    if (buf_write_bytes(write_info) == false)
                        end = 0;                /* write error */
                    nchars += len;
                }

                /* On many journalling file systems there is a bug that causes both the
                 * original and the backup file to be lost when halting the system right
                 * after writing the file.  That's because only the meta-data is
                 * journalled.  Syncing the file slows down the system, but assures it has
                 * been written to disk and we don't lose it.
                 * For a device do try the fsync() but don't complain if it does not work (could be a pipe).
                 * If the 'fsync' option is false, don't fsync().  Useful for laptops. */
                if (p_fs[0] && libc.fsync(fd) != 0 && !device)
                {
                    errmsg = u8("E667: Fsync failed");
                    end = 0;
                }

                /* When creating a new file, set its owner/group to that of the original file.
                 * Get the new device and inode number. */
                if (!buf.b_dev_valid)
                    /* Set the inode when creating a new file. */
                    buf_setino(buf);

                if (libc.close(fd) != 0)
                {
                    errmsg = u8("E512: Close failed");
                    end = 0;
                }

                if (made_writable)
                    perm &= ~0200;          /* reset 'w' bit for security reasons */
                if (0 <= perm)              /* set perm of new file same as old file */
                    mch_setperm(wfname, perm);

                if (BNE(wfname, fname))
                    libC.unlink(wfname);

                if (end == 0)
                {
                    if (errmsg == null)
                    {
                        if (write_info.bw_conv_error)
                        {
                            if (write_info.bw_conv_error_lnum == 0)
                                errmsg = u8("E513: write error, conversion failed (make 'fenc' empty to override)");
                            else
                            {
                                errmsg = new Bytes(300);
                                vim_snprintf(errmsg, errmsg.size(), u8("E513: write error, conversion failed in line %ld (make 'fenc' empty to override)"), write_info.bw_conv_error_lnum);
                            }
                        }
                        else if (got_int)
                            errmsg = e_interr;
                        else
                            errmsg = u8("E514: write error (file system full?)");
                    }

                    break fail;
                }

                lnum -= start;                  /* compute number of written lines */
                --no_wait_return;               /* may wait for return now */

                if (!filtering)
                {
                    msg_add_fname(fname);       /* put "fname" in ioBuff with quotes */
                    boolean c = false;
                    if (write_info.bw_conv_error)
                    {
                        STRCAT(ioBuff, u8(" CONVERSION ERROR"));
                        c = true;
                        if (write_info.bw_conv_error_lnum != 0)
                            vim_snprintf_add(ioBuff, IOSIZE, u8(" in line %ld;"), write_info.bw_conv_error_lnum);
                    }
                    else if (notconverted)
                    {
                        STRCAT(ioBuff, u8("[NOT converted]"));
                        c = true;
                    }
                    else if (converted)
                    {
                        STRCAT(ioBuff, u8("[converted]"));
                        c = true;
                    }
                    if (device)
                    {
                        STRCAT(ioBuff, u8("[Device]"));
                        c = true;
                    }
                    else if (newfile)
                    {
                        STRCAT(ioBuff, shortmess(SHM_NEW) ? u8("[New]") : u8("[New File]"));
                        c = true;
                    }
                    if (no_eol)
                    {
                        msg_add_eol();
                        c = true;
                    }
                    /* may add [unix/dos/mac] */
                    if (msg_add_fileformat(fileformat))
                        c = true;
                    msg_add_lines(c, lnum, nchars);     /* add line/char count */
                    if (!shortmess(SHM_WRITE))
                    {
                        if (append)
                            STRCAT(ioBuff, shortmess(SHM_WRI) ? u8(" [a]") : u8(" appended"));
                        else
                            STRCAT(ioBuff, shortmess(SHM_WRI) ? u8(" [w]") : u8(" written"));
                    }

                    set_keep_msg(msg_trunc_attr(ioBuff, false, 0), 0);
                }

                /* When written everything correctly: reset 'modified'.
                 * Unless not writing to the original file and '+' is not in 'cpoptions'. */
                if (reset_changed && whole && !append
                        && !write_info.bw_conv_error
                        && (overwriting || vim_strbyte(p_cpo[0], CPO_PLUS) != null))
                {
                    unchanged(buf, true);
                    /* buf.b_changedtick is always incremented in unchanged(),
                     * but that should not trigger a TextChanged event. */
                    if (last_changedtick + 1 == buf.b_changedtick && last_changedtick_buf == buf)
                        last_changedtick = buf.b_changedtick;
                    u_unchanged(buf);
                    u_update_save_nr(buf);
                }

                /*
                 * If written to the current file, update the timestamp of the swap file
                 * and reset the BF_WRITE_MASK flags.  Also sets buf.b_mtime.
                 */
                if (overwriting)
                {
                    ml_timestamp(buf);
                    if (append)
                        buf.b_flags &= ~BF_NEW;
                    else
                        buf.b_flags &= ~BF_WRITE_MASK;
                }

                break nofail;
            }

            /*
             * Finish up.  We get here either after failure or success.
             */
            --no_wait_return;           /* may wait for return now */
        }

        /* Done saving, we accept changed buffer warnings again. */
        buf.b_saving = false;

        if (errmsg != null)
        {
            int numlen = (errnum != null) ? strlen(errnum) : 0;

            int attr = hl_attr(HLF_E);          /* set highlight for error messages */
            msg_add_fname(fname);               /* put file name in ioBuff with quotes */
            if (IOSIZE <= strlen(ioBuff) + strlen(errmsg) + numlen)
                ioBuff.be(IOSIZE - strlen(errmsg) - numlen - 1, NUL);
            /* If the error message has the form "is ...", put the error number in front of the file name. */
            if (errnum != null)
            {
                BCOPY(ioBuff, numlen, ioBuff, 0, strlen(ioBuff) + 1);
                BCOPY(ioBuff, errnum, numlen);
            }
            STRCAT(ioBuff, errmsg);
            emsg(ioBuff);

            retval = false;
            if (end == 0)
            {
                msg_puts_attr(u8("\nWARNING: Original file may be lost or damaged\n"), attr | MSG_HIST);
                msg_puts_attr(u8("don't quit the editor until the file is successfully written!"), attr | MSG_HIST);

                stat_C st_old = new stat_C();

                /* Update the timestamp to avoid an "overwrite changed file" prompt when writing again. */
                if (0 <= libC.stat(fname, st_old))
                {
                    buf_store_time(buf, st_old);
                    buf.b_mtime_read = buf.b_mtime;
                }
            }
        }
        msg_scroll = msg_save;

        /*
         * When writing the whole file and 'undofile' is set, also write the undo file.
         */
        if (retval == true && write_undo_file)
        {
            Bytes hash = new Bytes(UNDO_HASH_SIZE);

            sha256_finish(sha_ctx, hash);
            u_write_undo(null, false, buf, hash);
        }

        if (!should_abort(retval))
        {
            curbuf.b_no_eol_lnum = 0;   /* in case it was set by the previous read */

            /*
             * Apply POST autocommands.
             * Careful: The autocommands may call buf_write() recursively!
             */
            aco_save_C aco = new aco_save_C();
            aucmd_prepbuf(aco, buf);

            if (append)
                apply_autocmds_exarg(EVENT_FILEAPPENDPOST, fname, fname, false, curbuf, eap);
            else if (filtering)
                apply_autocmds_exarg(EVENT_FILTERWRITEPOST, null, fname, false, curbuf, eap);
            else if (reset_changed && whole)
                apply_autocmds_exarg(EVENT_BUFWRITEPOST, fname, fname, false, curbuf, eap);
            else
                apply_autocmds_exarg(EVENT_FILEWRITEPOST, fname, fname, false, curbuf, eap);

            /* restore curwin/curbuf and a few other things */
            aucmd_restbuf(aco);

            if (aborting())     /* autocmds may abort script processing */
                retval = false;
        }

        got_int |= prev_got_int;

        return retval;
    }

    /*
     * Set the name of the current buffer.
     * Use when the buffer doesn't have a name and a ":r" or ":w" command with a file name is used.
     */
    /*private*/ static boolean set_rw_fname(Bytes fname, Bytes sfname)
    {
        buffer_C buf = curbuf;

        /* It's like the unnamed buffer is deleted.... */
        if (curbuf.b_p_bl[0])
            apply_autocmds(EVENT_BUFDELETE, null, null, false, curbuf);
        apply_autocmds(EVENT_BUFWIPEOUT, null, null, false, curbuf);
        if (aborting())         /* autocmds may abort script processing */
            return false;
        if (curbuf != buf)
        {
            /* We are in another buffer now, don't do the renaming. */
            emsg(e_auchangedbuf);
            return false;
        }

        if (setfname(curbuf, fname, sfname, false) == true)
            curbuf.b_flags |= BF_NOTEDITED;

        /* ....and a new named one is created */
        apply_autocmds(EVENT_BUFNEW, null, null, false, curbuf);
        if (curbuf.b_p_bl[0])
            apply_autocmds(EVENT_BUFADD, null, null, false, curbuf);
        if (aborting())         /* autocmds may abort script processing */
            return false;

        /* Do filetype detection now if 'filetype' is empty. */
        if (curbuf.b_p_ft[0].at(0) == NUL)
        {
            if (au_has_group(u8("filetypedetect")))
                do_doautocmd(u8("filetypedetect BufRead"), false);
        }

        return true;
    }

    /*
     * Put file name into ioBuff with quotes.
     */
    /*private*/ static void msg_add_fname(Bytes fname)
    {
        if (fname == null)
            fname = u8("-stdin-");

        vim_strncpy(ioBuff.plus(1), fname, IOSIZE - 4 - 1);
        ioBuff.be(0, (byte)'"');
        STRCAT(ioBuff, u8("\" "));
    }

    /*
     * Append message for text mode to ioBuff.
     * Return true if something appended.
     */
    /*private*/ static boolean msg_add_fileformat(int eol_type)
    {
        if (eol_type == EOL_DOS)
        {
            STRCAT(ioBuff, shortmess(SHM_TEXT) ? u8("[dos]") : u8("[dos format]"));
            return true;
        }
        if (eol_type == EOL_MAC)
        {
            STRCAT(ioBuff, shortmess(SHM_TEXT) ? u8("[mac]") : u8("[mac format]"));
            return true;
        }
        return false;
    }

    /*
     * Append line and character count to ioBuff.
     */
    /*private*/ static void msg_add_lines(boolean insert_space, long lnum, long nchars)
    {
        Bytes p = ioBuff.plus(strlen(ioBuff));

        if (insert_space)
            (p = p.plus(1)).be(-1, (byte)' ');
        if (shortmess(SHM_LINES))
            libC.sprintf(p, u8("%ldL, %ldC"), lnum, nchars);
        else
        {
            if (lnum == 1)
                STRCPY(p, u8("1 line, "));
            else
                libC.sprintf(p, u8("%ld lines, "), lnum);
            p = p.plus(strlen(p));
            if (nchars == 1)
                STRCPY(p, u8("1 character"));
            else
                libC.sprintf(p, u8("%ld characters"), nchars);
        }
    }

    /*
     * Append message for missing line separator to ioBuff.
     */
    /*private*/ static void msg_add_eol()
    {
        STRCAT(ioBuff, shortmess(SHM_LAST) ? u8("[noeol]") : u8("[Incomplete last line]"));
    }

    /*
     * Check modification time of file, before writing to it.
     * The size isn't checked, because using a tool like "gzip" takes care of
     * using the same timestamp but can't set the size.
     */
    /*private*/ static boolean check_mtime(buffer_C buf, stat_C st)
    {
        if (buf.b_mtime_read != 0 && time_differs(st.st_mtime(), buf.b_mtime_read))
        {
            msg_scroll = true;          /* don't overwrite messages here */
            msg_silent = 0;             /* must give this prompt */
            /* don't use emsg() here, don't want to flush the buffers */
            msg_attr(u8("WARNING: The file has been changed since reading it!!!"), hl_attr(HLF_E));
            if (ask_yesno(u8("Do you really want to write to it"), true) == 'n')
                return false;
            msg_scroll = false;         /* always overwrite the file message now */
        }
        return true;
    }

    /*private*/ static boolean time_differs(long t1, long t2)
    {
        /* On a FAT filesystem, esp. under Linux, there are only 5 bits to store
         * the seconds.  Since the roundoff is done when flushing the inode, the
         * time may change unexpectedly by one second!!! */
        return (1 < t1 - t2 || 1 < t2 - t1);
    }

    /*
     * Call write() to write a number of bytes to the file.
     * Handles encryption and 'encoding' conversion.
     *
     * Return false for failure, true otherwise.
     */
    /*private*/ static boolean buf_write_bytes(bw_info_C ip)
    {
        Bytes buf = ip.bw_buf;         /* data to write */
        int len = ip.bw_len;            /* length of data */
        int flags = ip.bw_flags;        /* extra flags */

        /*
         * Skip conversion when writing the crypt magic number or the BOM.
         */
        if ((flags & FIO_NOCONVERT) == 0)
        {
            if ((flags & FIO_UTF8) != 0)
            {
                /*
                 * Convert latin1 in the buffer to UTF-8 in the file.
                 */
                Bytes p = ip.bw_conv_buf;      /* translate to buffer */
                for (int wlen = 0; wlen < len; wlen++)
                    p = p.plus(utf_char2bytes(buf.at(wlen), p));
                buf = ip.bw_conv_buf;
                len = BDIFF(p, ip.bw_conv_buf);
            }
            else if ((flags & (FIO_UCS4 | FIO_UTF16 | FIO_UCS2 | FIO_LATIN1)) != 0)
            {
                /*
                 * Convert UTF-8 bytes in the buffer to UCS-2, UCS-4, UTF-16 or
                 * Latin1 chars in the file.
                 */
                Bytes p;
                if ((flags & FIO_LATIN1) != 0)
                    p = buf;        /* translate in-place (can only get shorter) */
                else
                    p = ip.bw_conv_buf;     /* translate to buffer */
                int n;
                for (int wlen = 0; wlen < len; wlen += n)
                {
                    int c;

                    if (wlen == 0 && ip.bw_restlen != 0)
                    {
                        /* Use remainder of previous call.
                         * Append the start of buf[] to get a full sequence.
                         * Might still be too short!
                         */
                        int l = CONV_RESTLEN - ip.bw_restlen;
                        if (len < l)
                            l = len;
                        BCOPY(ip.bw_rest, ip.bw_restlen, buf, 0, l);
                        n = us_ptr2len_len(ip.bw_rest, ip.bw_restlen + l);
                        if (ip.bw_restlen + len < n)
                        {
                            /* We have an incomplete byte sequence at the end to be written.
                             * We can't convert it without the remaining bytes.
                             * Keep them for the next call.
                             */
                            if (CONV_RESTLEN < ip.bw_restlen + len)
                                return false;
                            ip.bw_restlen += len;
                            break;
                        }
                        if (1 < n)
                            c = us_ptr2char(ip.bw_rest);
                        else
                            c = ip.bw_rest.at(0);
                        if (ip.bw_restlen <= n)
                        {
                            n -= ip.bw_restlen;
                            ip.bw_restlen = 0;
                        }
                        else
                        {
                            ip.bw_restlen -= n;
                            BCOPY(ip.bw_rest, 0, ip.bw_rest, n, ip.bw_restlen);
                            n = 0;
                        }
                    }
                    else
                    {
                        n = us_ptr2len_len(buf.plus(wlen), len - wlen);
                        if (len - wlen < n)
                        {
                            /* We have an incomplete byte sequence at the end to
                             * be written.  We can't convert it without the
                             * remaining bytes.  Keep them for the next call. */
                            if (CONV_RESTLEN < len - wlen)
                                return false;
                            ip.bw_restlen = len - wlen;
                            BCOPY(ip.bw_rest, 0, buf, wlen, ip.bw_restlen);
                            break;
                        }
                        if (1 < n)
                            c = us_ptr2char(buf.plus(wlen));
                        else
                            c = buf.at(wlen);
                    }

                    boolean b;
                    { Bytes[] __ = { p }; b = ucs2bytes(c, __, flags); p = __[0]; }
                    if (b && !ip.bw_conv_error)
                    {
                        ip.bw_conv_error = true;
                        ip.bw_conv_error_lnum = ip.bw_start_lnum;
                    }
                    if (c == NL)
                        ip.bw_start_lnum++;
                }
                if ((flags & FIO_LATIN1) != 0)
                    len = BDIFF(p, buf);
                else
                {
                    buf = ip.bw_conv_buf;
                    len = BDIFF(p, ip.bw_conv_buf);
                }
            }
        }

        int wlen = write_eintr(ip.bw_fd, buf, len);
        return (wlen < len) ? false : true;
    }

    /*
     * Convert a Unicode character to bytes.
     * Return true for an error, false when it's OK.
     */
    /*private*/ static boolean ucs2bytes(int c, Bytes[] pp, int flags)
        /* c: in: character */
        /* pp: in/out: pointer to result */
        /* flags: FIO_ flags */
    {
        boolean error = false;
        Bytes p = pp[0];

        if ((flags & FIO_UCS4) != 0)
        {
            if ((flags & FIO_ENDIAN_L) != 0)
            {
                (p = p.plus(1)).be(-1, c);
                (p = p.plus(1)).be(-1, (c >>> 8));
                (p = p.plus(1)).be(-1, (c >>> 16));
                (p = p.plus(1)).be(-1, (c >>> 24));
            }
            else
            {
                (p = p.plus(1)).be(-1, (c >>> 24));
                (p = p.plus(1)).be(-1, (c >>> 16));
                (p = p.plus(1)).be(-1, (c >>> 8));
                (p = p.plus(1)).be(-1, c);
            }
        }
        else if ((flags & (FIO_UCS2 | FIO_UTF16)) != 0)
        {
            if (0x10000 <= c)
            {
                if ((flags & FIO_UTF16) != 0)
                {
                    /* Make two words, ten bits of the character in each.
                     * First word is 0xd800 - 0xdbff, second one 0xdc00 - 0xdfff.
                     */
                    c -= 0x10000;
                    if (0x100000 <= c)
                        error = true;
                    int cc = ((c >>> 10) & 0x3ff) + 0xd800;
                    if ((flags & FIO_ENDIAN_L) != 0)
                    {
                        (p = p.plus(1)).be(-1, cc);
                        (p = p.plus(1)).be(-1, (cc >>> 8));
                    }
                    else
                    {
                        (p = p.plus(1)).be(-1, (cc >>> 8));
                        (p = p.plus(1)).be(-1, cc);
                    }
                    c = (c & 0x3ff) + 0xdc00;
                }
                else
                    error = true;
            }
            if ((flags & FIO_ENDIAN_L) != 0)
            {
                (p = p.plus(1)).be(-1, c);
                (p = p.plus(1)).be(-1, (c >>> 8));
            }
            else
            {
                (p = p.plus(1)).be(-1, (c >>> 8));
                (p = p.plus(1)).be(-1, c);
            }
        }
        else    /* Latin1 */
        {
            if (0x100 <= c)
            {
                error = true;
                (p = p.plus(1)).be(-1, 0xbf);
            }
            else
                (p = p.plus(1)).be(-1, c);
        }

        pp[0] = p;
        return error;
    }

    /*
     * Return true if file encoding "fenc" requires conversion from or to 'encoding'.
     */
    /*private*/ static boolean need_conversion(Bytes fenc)
    {
        boolean same_encoding;
        int enc_flags;
        int fenc_flags;

        if (fenc.at(0) == NUL || STRCMP(u8("utf-8"), fenc) == 0)
        {
            same_encoding = true;
            fenc_flags = 0;
        }
        else
        {
            /* Ignore difference between "ansi" and "latin1", "ucs-4" and "ucs-4be", etc. */
            enc_flags = get_fio_flags(u8("utf-8"));
            fenc_flags = get_fio_flags(fenc);
            same_encoding = (enc_flags != 0 && fenc_flags == enc_flags);
        }
        if (same_encoding)
        {
            /* Specified encoding matches with 'encoding'.  This requires
             * conversion when 'encoding' is Unicode but not UTF-8. */
            return false;
        }

        /* Encodings differ.  However, conversion is not needed when 'enc'
         * is any Unicode encoding and the file is UTF-8. */
        return (fenc_flags != FIO_UTF8);
    }

    /*
     * Check "name" for a unicode encoding and return the FIO_ flags needed for the internal conversion.
     * If "name" is an empty string, use 'encoding'.
     */
    /*private*/ static int get_fio_flags(Bytes name)
    {
        if (name.at(0) == NUL)
            name = u8("utf-8");

        if (STRCMP(name, u8("utf-8")) == 0)
            return FIO_UTF8;

        return 0;
    }

    /*
     * Check for a Unicode BOM (Byte Order Mark) at the start of p[size].
     * "size" must be at least 2.
     * Return the name of the encoding and set "*lenp" to the length.
     * Returns null when no BOM found.
     */
    /*private*/ static Bytes check_for_bom(Bytes p, long size, int[] lenp, int flags)
    {
        Bytes name = null;
        int len = 2;

        if (char_u(p.at(0)) == 0xef && char_u(p.at(1)) == 0xbb && 3 <= size && char_u(p.at(2)) == 0xbf
                && (flags == FIO_ALL || flags == FIO_UTF8 || flags == 0))
        {
            name = u8("utf-8");         /* EF BB BF */
            len = 3;
        }
        else if (char_u(p.at(0)) == 0xff && char_u(p.at(1)) == 0xfe)
        {
            if (4 <= size && p.at(2) == 0 && p.at(3) == 0
                && (flags == FIO_ALL || flags == (FIO_UCS4 | FIO_ENDIAN_L)))
            {
                name = u8("ucs-4le");   /* FF FE 00 00 */
                len = 4;
            }
            else if (flags == (FIO_UCS2 | FIO_ENDIAN_L))
                name = u8("ucs-2le");   /* FF FE */
            else if (flags == FIO_ALL || flags == (FIO_UTF16 | FIO_ENDIAN_L))
                /* utf-16le is preferred, it also works for ucs-2le text */
                name = u8("utf-16le");  /* FF FE */
        }
        else if (char_u(p.at(0)) == 0xfe && char_u(p.at(1)) == 0xff
                && (flags == FIO_ALL || flags == FIO_UCS2 || flags == FIO_UTF16))
        {
            /* Default to utf-16, it works also for ucs-2 text. */
            if (flags == FIO_UCS2)
                name = u8("ucs-2");     /* FE FF */
            else
                name = u8("utf-16");    /* FE FF */
        }
        else if (4 <= size && p.at(0) == 0 && p.at(1) == 0 && char_u(p.at(2)) == 0xfe && char_u(p.at(3)) == 0xff
                && (flags == FIO_ALL || flags == FIO_UCS4))
        {
            name = u8("ucs-4");         /* 00 00 FE FF */
            len = 4;
        }

        lenp[0] = len;
        return name;
    }

    /*
     * Generate a BOM in "buf[4]" for encoding "name".
     * Return the length of the BOM (zero when no BOM).
     */
    /*private*/ static int make_bom(Bytes buf, Bytes name)
    {
        int flags = get_fio_flags(name);

        /* Can't put a BOM in a non-Unicode file. */
        if (flags == FIO_LATIN1 || flags == 0)
            return 0;

        if (flags == FIO_UTF8)      /* UTF-8 */
        {
            buf.be(0, 0xef);
            buf.be(1, 0xbb);
            buf.be(2, 0xbf);
            return 3;
        }

        Bytes[] p = { buf };
        ucs2bytes(0xfeff, p, flags);
        return BDIFF(p[0], buf);
    }

    /*
     * Try to find a shortname by comparing the fullname with the current directory.
     * Returns "full_path" or pointer into "full_path" if shortened.
     */
    /*private*/ static Bytes shorten_fname1(Bytes full_path)
    {
        Bytes p = full_path;

        Bytes dirname = new Bytes(MAXPATHL);
        if (mch_dirname(dirname, MAXPATHL) == true)
        {
            p = shorten_fname(full_path, dirname);
            if (p == null || p.at(0) == NUL)
                p = full_path;
        }

        return p;
    }

    /*
     * Try to find a shortname by comparing the fullname with the current directory.
     * Returns null if not shorter name possible, pointer into "full_path" otherwise.
     */
    /*private*/ static Bytes shorten_fname(Bytes full_path, Bytes dir_name)
    {
        if (full_path == null)
            return null;

        int len = strlen(dir_name);
        if (STRNCMP(dir_name, full_path, len) != 0)
            return null;

        Bytes p = full_path.plus(len);
        if (!vim_ispathsep(p.at(0)))
            return null;

        return p.plus(1);
    }

    /*
     * Shorten filenames for all buffers.
     * When "force" is true: use full path from now on for files currently being edited,
     * both for file name and swap file name.  Try to shorten the file names a bit,
     * if safe to do so.
     * When "force" is false: only try to shorten absolute file names.
     * For buffers that have buftype "nofile" or "scratch": never change the file name.
     */
    /*private*/ static void shorten_fnames(boolean force)
    {
        Bytes dirname = new Bytes(MAXPATHL);
        mch_dirname(dirname, MAXPATHL);

        for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
        {
            if (buf.b_fname != null
                    && path_with_url(buf.b_fname) == 0
                    && (force
                        || buf.b_sfname == null
                        || mch_isFullName(buf.b_sfname)))
            {
                buf.b_sfname = null;
                Bytes p = shorten_fname(buf.b_ffname, dirname);
                if (p != null)
                {
                    buf.b_sfname = STRDUP(p);
                    buf.b_fname = buf.b_sfname;
                }
                if (p == null || buf.b_fname == null)
                    buf.b_fname = buf.b_ffname;
            }
        }
        status_redraw_all();
        redraw_tabline = true;
    }

    /*private*/ static boolean already_warned;

    /*
     * Check if any not hidden buffer has been changed.
     * Postpone the check if there are characters in the stuff buffer,
     * a global command is being executed, a mapping is being executed or an autocommand is busy.
     * Returns true if some message was written (screen should be redrawn and cursor positioned).
     */
    /*private*/ static int check_timestamps(boolean focus)
        /* focus: called for GUI focus event */
    {
        int didit = 0;

        /* Don't check timestamps while system() or another low-level function
         * may cause us to lose and gain focus. */
        if (0 < no_check_timestamps)
            return FALSE;

        /* Avoid doing a check twice.  The OK/Reload dialog can cause a focus
         * event and we would keep on checking if the file is steadily growing.
         * Do check again after typing something. */
        if (focus && did_check_timestamps)
        {
            need_check_timestamps = true;
            return FALSE;
        }

        if (!stuff_empty() || global_busy != 0 || !typebuf_typed() || autocmd_busy || 0 < curbuf_lock || 0 < allbuf_lock)
            need_check_timestamps = true;           /* check later */
        else
        {
            no_wait_return++;
            did_check_timestamps = true;
            already_warned = false;
            for (buffer_C buf = firstbuf; buf != null; )
            {
                /* Only check buffers in a window. */
                if (0 < buf.b_nwindows)
                {
                    int n = buf_check_timestamp(buf);
                    if (didit < n)
                        didit = n;
                    if (0 < n && !buf_valid(buf))
                    {
                        /* Autocommands have removed the buffer, start at the first one again. */
                        buf = firstbuf;
                        continue;
                    }
                }
                buf = buf.b_next;
            }
            --no_wait_return;
            need_check_timestamps = false;
            if (need_wait_return && didit == 2)
            {
                /* make sure msg isn't overwritten */
                msg_puts(u8("\n"));
                out_flush();
            }
        }

        return didit;
    }

    /*
     * Move all the lines from buffer "frombuf" to buffer "tobuf".
     * Return true or false.  When false "tobuf" is incomplete and/or "frombuf" is not empty.
     */
    /*private*/ static boolean move_lines(buffer_C frombuf, buffer_C tobuf)
    {
        boolean retval = true;
        buffer_C tbuf = curbuf;

        /* Copy the lines in "frombuf" to "tobuf". */
        curbuf = tobuf;
        for (long lnum = 1; lnum <= frombuf.b_ml.ml_line_count; lnum++)
        {
            Bytes p = STRDUP(ml_get_buf(frombuf, lnum, false));
            if (!ml_append(lnum - 1, p, 0, false))
            {
                retval = false;
                break;
            }
        }

        /* Delete all the lines in "frombuf". */
        if (retval != false)
        {
            curbuf = frombuf;
            for (long lnum = curbuf.b_ml.ml_line_count; 0 < lnum; --lnum)
                if (ml_delete(lnum, false) == false)
                {
                    /* Oops!  We could try putting back the saved lines,
                     * but that might fail again... */
                    retval = false;
                    break;
                }
        }

        curbuf = tbuf;
        return retval;
    }

    /*private*/ static boolean _1_busy;

    /*
     * Check if buffer "buf" has been changed.
     * Also check if the file for a new buffer unexpectedly appeared.
     * return 1 if a changed buffer was found.
     * return 2 if a message has been displayed.
     * return 0 otherwise.
     */
    /*private*/ static int buf_check_timestamp(buffer_C buf)
    {
        Bytes mesg = null;
        Bytes mesg2 = u8("");
        boolean helpmesg = false;
        boolean reload = false;
        boolean can_reload = false;
        long orig_size = buf.b_orig_size;
        int orig_mode = buf.b_orig_mode;

        /* If there is no file name, the buffer is not loaded, 'buftype' is set,
         * we are in the middle of a save or being called recursively: ignore this buffer. */
        if (buf.b_ffname == null || buf.b_ml.ml_mfp == null || buf.b_saving || _1_busy)
            return 0;

        int retval = 0;

        int stat_res;
        stat_C st = new stat_C();
        if ((buf.b_flags & BF_NOTEDITED) == 0
                && buf.b_mtime != 0
                && ((stat_res = libC.stat(buf.b_ffname, st)) < 0
                    || time_differs(st.st_mtime(), buf.b_mtime)
                    || st.st_size() != buf.b_orig_size
                    || st.st_mode() != buf.b_orig_mode))
        {
            retval = 1;

            /* set b_mtime to stop further warnings (e.g., when executing FileChangedShell autocmd) */
            if (stat_res < 0)
            {
                buf.b_mtime = 0;
                buf.b_orig_size = 0;
                buf.b_orig_mode = 0;
            }
            else
                buf_store_time(buf, st);

            /* Don't do anything for a directory.  Might contain the file explorer. */
            if (mch_isdir(buf.b_fname))
                ;

            /*
             * If 'autoread' is set, the buffer has no changes and the file still exists, reload the buffer.
             * Use the buffer-local option value if it was set, the global option value otherwise.
             */
            else if (((-1 < buf.b_p_ar[0]) ? (buf.b_p_ar[0] != FALSE) : p_ar[0]) && !bufIsChanged(buf) && 0 <= stat_res)
                reload = true;
            else
            {
                Bytes reason;
                if (stat_res < 0)
                    reason = u8("deleted");
                else if (bufIsChanged(buf))
                    reason = u8("conflict");
                else if (orig_size != buf.b_orig_size || buf_contents_changed(buf))
                    reason = u8("changed");
                else if (orig_mode != buf.b_orig_mode)
                    reason = u8("mode");
                else
                    reason = u8("time");

                /*
                 * Only give the warning if there are no FileChangedShell autocommands.
                 * Avoid being called recursively by setting "_1_busy".
                 */
                _1_busy = true;
                set_vim_var_string(VV_FCS_REASON, reason, -1);
                set_vim_var_string(VV_FCS_CHOICE, u8(""), -1);
                allbuf_lock++;
                boolean b = apply_autocmds(EVENT_FILECHANGEDSHELL, buf.b_fname, buf.b_fname, false, buf);
                --allbuf_lock;
                _1_busy = false;
                if (b)
                {
                    if (!buf_valid(buf))
                        emsg(u8("E246: FileChangedShell autocommand deleted buffer"));
                    Bytes s = get_vim_var_str(VV_FCS_CHOICE);
                    if (STRCMP(s, u8("reload")) == 0 && reason.at(0) != (byte)'d')
                        reload = true;
                    else if (STRCMP(s, u8("ask")) == 0)
                        b = false;
                    else
                        return 2;
                }
                if (!b)
                {
                    if (reason.at(0) == (byte)'d')
                        mesg = u8("E211: File \"%s\" no longer available");
                    else
                    {
                        helpmesg = true;
                        can_reload = true;
                        /*
                         * Check if the file contents really changed to avoid giving a warning
                         * when only the timestamp was set (e.g. checked out of CVS).
                         * Always warn when the buffer was changed.
                         */
                        if (reason.at(2) == (byte)'n')
                        {
                            mesg = u8("W12: Warning: File \"%s\" has changed and the buffer was changed in Vim as well");
                            mesg2 = u8("See \":help W12\" for more info.");
                        }
                        else if (reason.at(1) == (byte)'h')
                        {
                            mesg = u8("W11: Warning: File \"%s\" has changed since editing started");
                            mesg2 = u8("See \":help W11\" for more info.");
                        }
                        else if (reason.at(0) == (byte)'m')
                        {
                            mesg = u8("W16: Warning: Mode of file \"%s\" has changed since editing started");
                            mesg2 = u8("See \":help W16\" for more info.");
                        }
                        else
                            /* Only timestamp changed.
                             * Store it to avoid a warning in check_mtime() later. */
                            buf.b_mtime_read = buf.b_mtime;
                    }
                }
            }
        }
        else if ((buf.b_flags & BF_NEW) != 0 && (buf.b_flags & BF_NEW_W) == 0 && vim_fexists(buf.b_ffname))
        {
            retval = 1;
            mesg = u8("W13: Warning: File \"%s\" has been created after editing started");
            buf.b_flags |= BF_NEW_W;
            can_reload = true;
        }

        if (mesg != null)
        {
            Bytes path = buf.b_fname;
            if (path != null)
            {
                if (!helpmesg)
                    mesg2 = u8("");
                Bytes tbuf = new Bytes(strlen(path) + strlen(mesg) + strlen(mesg2) + 2);
                libC.sprintf(tbuf, mesg, path);
                /* Set "warningmsg" before the unimportant and output-specific "mesg2" has been appended. */
                set_vim_var_string(VV_WARNINGMSG, tbuf, -1);
                if (can_reload)
                {
                    if (mesg2.at(0) != NUL)
                    {
                        STRCAT(tbuf, u8("\n"));
                        STRCAT(tbuf, mesg2);
                    }
                    if (do_dialog(tbuf, u8("&OK\n&Load File"), 1, true) == 2)
                        reload = true;
                }
                else if (NORMAL_BUSY < State || (State & CMDLINE) != 0 || already_warned)
                {
                    if (mesg2.at(0) != NUL)
                    {
                        STRCAT(tbuf, u8("; "));
                        STRCAT(tbuf, mesg2);
                    }
                    emsg(tbuf);
                    retval = 2;
                }
                else
                {
                    if (!autocmd_busy)
                    {
                        msg_start();
                        msg_puts_attr(tbuf, hl_attr(HLF_E) + MSG_HIST);
                        if (mesg2.at(0) != NUL)
                            msg_puts_attr(mesg2, hl_attr(HLF_W) + MSG_HIST);
                        msg_clr_eos();
                        msg_end();
                        if (emsg_silent == 0)
                        {
                            out_flush();
                            /* give the user some time to think about it */
                            ui_delay(1000L, true);

                            /* don't redraw and erase the message */
                            redraw_cmdline = false;
                        }
                    }
                    already_warned = true;
                }
            }
        }

        if (reload)
        {
            /* Reload the buffer. */
            buf_reload(buf, orig_mode);
            if (buf.b_p_udf[0] && buf.b_ffname != null)
            {
                Bytes hash = new Bytes(UNDO_HASH_SIZE);
                buffer_C save_curbuf = curbuf;

                /* Any existing undo file is unusable, write it now. */
                curbuf = buf;
                u_compute_hash(hash);
                u_write_undo(null, false, buf, hash);
                curbuf = save_curbuf;
            }
        }

        /* Trigger FileChangedShell when the file was changed in any way. */
        if (buf_valid(buf) && retval != 0)
            apply_autocmds(EVENT_FILECHANGEDSHELLPOST, buf.b_fname, buf.b_fname, false, buf);

        return retval;
    }

    /*
     * Reload a buffer that is already loaded.
     * Used when the file was changed outside of Vim.
     * "orig_mode" is buf.b_orig_mode before the need for reloading was detected.
     * buf.b_orig_mode may have been reset already.
     */
    /*private*/ static void buf_reload(buffer_C buf, int orig_mode)
    {
        boolean old_ro = buf.b_p_ro[0];
        boolean saved = true;
        int flags = READ_NEW;

        /* set curwin/curbuf for "buf" and save some things */
        aco_save_C aco = new aco_save_C();
        aucmd_prepbuf(aco, buf);

        /* We only want to read the text from the file,
         * not reset the syntax highlighting, clear marks, diff status, etc.
         */
        exarg_C ea = new exarg_C();
        prep_exarg(ea, buf);

        pos_C old_cursor = new pos_C();
        COPY_pos(old_cursor, curwin.w_cursor);
        long old_topline = curwin.w_topline;

        if (p_ur[0] < 0 || curbuf.b_ml.ml_line_count <= p_ur[0])
        {
            /* Save all the text, so that the reload can be undone.
             * Sync first so that this is a separate undo-able action. */
            u_sync(false);
            saved = u_savecommon(0, curbuf.b_ml.ml_line_count + 1, 0, true);
            flags |= READ_KEEP_UNDO;
        }

        /*
         * To behave like when a new file is edited (matters for BufReadPost
         * autocommands) we first need to delete the current buffer contents.
         * But if reading the file fails we should keep the old contents.
         * Can't use memory only, the file might be too big.
         * Use a hidden buffer to move the buffer contents to.
         */
        buffer_C savebuf;
        if (bufempty() || saved == false)
            savebuf = null;
        else
        {
            /* Allocate a buffer without putting it in the buffer list. */
            savebuf = buflist_new(null, null, 1, BLN_DUMMY);
            if (savebuf != null && buf == curbuf)
            {
                /* Open the memline. */
                curbuf = savebuf;
                curwin.w_buffer = savebuf;
                saved = ml_open(curbuf);
                curbuf = buf;
                curwin.w_buffer = buf;
            }
            if (savebuf == null || saved == false || buf != curbuf || move_lines(buf, savebuf) == false)
            {
                emsg2(u8("E462: Could not prepare for reloading \"%s\""), buf.b_fname);
                saved = false;
            }
        }

        if (saved == true)
        {
            curbuf.b_flags |= BF_CHECK_RO;              /* check for RO again */
            keep_filetype = true;                       /* don't detect 'filetype' */
            if (readfile(buf.b_ffname, buf.b_fname, 0, 0, MAXLNUM, ea, flags) == false)
            {
                if (!aborting())
                    emsg2(u8("E321: Could not reload \"%s\""), buf.b_fname);
                if (savebuf != null && buf_valid(savebuf) && buf == curbuf)
                {
                    /* Put the text back from the save buffer.
                     * First delete any lines that readfile() added. */
                    while (!bufempty())
                        if (ml_delete(buf.b_ml.ml_line_count, false) == false)
                            break;
                    move_lines(savebuf, buf);
                }
            }
            else if (buf == curbuf)                     /* "buf" still valid */
            {
                /* Mark the buffer as unmodified and free undo info. */
                unchanged(buf, true);
                if ((flags & READ_KEEP_UNDO) == 0)
                {
                    u_blockfree(buf);
                    u_clearall(buf);
                }
                else
                {
                    /* Mark all undo states as changed. */
                    u_unchanged(curbuf);
                }
            }
        }

        if (savebuf != null && buf_valid(savebuf))
            wipe_buffer(savebuf, false);

        /* Restore the topline and cursor position and check it (lines may have been removed). */
        if (curbuf.b_ml.ml_line_count < old_topline)
            curwin.w_topline = curbuf.b_ml.ml_line_count;
        else
            curwin.w_topline = old_topline;
        COPY_pos(curwin.w_cursor, old_cursor);
        check_cursor();
        update_topline();
        keep_filetype = false;
        /* If the mode didn't change and 'readonly' was set, keep the old value;
         * the user probably used the ":view" command.  But don't reset it,
         * might have had a read error. */
        if (orig_mode == curbuf.b_orig_mode)
            curbuf.b_p_ro[0] |= old_ro;

        /* restore curwin/curbuf and a few other things */
        aucmd_restbuf(aco);

        /* Careful: autocommands may have made "buf" invalid! */
    }

    /*private*/ static void buf_store_time(buffer_C buf, stat_C st)
    {
        buf.b_mtime = st.st_mtime();
        buf.b_orig_size = st.st_size();
        buf.b_orig_mode = st.st_mode();
    }

    /*
     * Adjust the line with missing eol, used for the next write.
     * Used for do_filter(), when the input lines for the filter are deleted.
     */
    /*private*/ static void write_lnum_adjust(long offset)
    {
        if (curbuf.b_no_eol_lnum != 0)      /* only if there is a missing eol */
            curbuf.b_no_eol_lnum += offset;
    }

    /*
     * Code for automatic commands.
     */

    /*
     * The autocommands are stored in a list for each event.
     * Autocommands for the same pattern, that are consecutive, are joined
     * together, to avoid having to match the pattern too often.
     * The result is an array of Autopat lists, which point to AutoCmd lists:
     *
     * first_autopat[0] --> Autopat.next  -->  Autopat.next -->  null
     *                      Autopat.cmds       Autopat.cmds
     *                          |                    |
     *                          V                    V
     *                      AutoCmd.next       AutoCmd.next
     *                          |                    |
     *                          V                    V
     *                      AutoCmd.next            null
     *                          |
     *                          V
     *                         null
     *
     * first_autopat[1] --> Autopat.next  -->  null
     *                      Autopat.cmds
     *                          |
     *                          V
     *                      AutoCmd.next
     *                          |
     *                          V
     *                         null
     *   etc.
     *
     *   The order of AutoCmds is important, this is the order in which they were
     *   defined and will have to be executed.
     */
    /*private*/ static final class AutoCmd_C
    {
        Bytes           cmd;                /* the command to be executed (null,
                                             * when command has been removed) */
        boolean         nested;             /* if autocommands nest here */
        boolean         last;               /* last command in list */
        int             scriptID;           /* script ID where defined */
        AutoCmd_C       next;               /* next AutoCmd in list */

        /*private*/ AutoCmd_C()
        {
        }
    }

    /*private*/ static final class AutoPat_C
    {
        Bytes           pat;                /* pattern as typed (null when pattern has been removed) */
        regprog_C       reg_prog;           /* compiled regprog for pattern */
        AutoCmd_C       cmds;               /* list of commands to do */
        AutoPat_C       next;               /* next AutoPat in AutoPat list */
        int             group;              /* group ID */
        int             patlen;             /* strlen() of "pat" */
        int             buflocal_nr;        /* !=0 for buffer-local AutoPat */
        boolean         allow_dirs;         /* Pattern may match whole path */
        boolean         last;               /* last pattern for apply_autocmds() */

        /*private*/ AutoPat_C()
        {
        }
    }

    /*private*/ static final class event_name_C
    {
        Bytes       name;       /* event name */
        int         event;      /* event number */

        /*private*/ event_name_C(Bytes name, int event)
        {
            this.name = name;
            this.event = event;
        }
    }

    /*private*/ static event_name_C[] event_names = new event_name_C[]
    {
        new event_name_C(u8("BufAdd"),               EVENT_BUFADD              ),
        new event_name_C(u8("BufCreate"),            EVENT_BUFADD              ),
        new event_name_C(u8("BufDelete"),            EVENT_BUFDELETE           ),
        new event_name_C(u8("BufEnter"),             EVENT_BUFENTER            ),
        new event_name_C(u8("BufFilePost"),          EVENT_BUFFILEPOST         ),
        new event_name_C(u8("BufFilePre"),           EVENT_BUFFILEPRE          ),
        new event_name_C(u8("BufHidden"),            EVENT_BUFHIDDEN           ),
        new event_name_C(u8("BufLeave"),             EVENT_BUFLEAVE            ),
        new event_name_C(u8("BufNew"),               EVENT_BUFNEW              ),
        new event_name_C(u8("BufNewFile"),           EVENT_BUFNEWFILE          ),
        new event_name_C(u8("BufRead"),              EVENT_BUFREADPOST         ),
        new event_name_C(u8("BufReadCmd"),           EVENT_BUFREADCMD          ),
        new event_name_C(u8("BufReadPost"),          EVENT_BUFREADPOST         ),
        new event_name_C(u8("BufReadPre"),           EVENT_BUFREADPRE          ),
        new event_name_C(u8("BufUnload"),            EVENT_BUFUNLOAD           ),
        new event_name_C(u8("BufWinEnter"),          EVENT_BUFWINENTER         ),
        new event_name_C(u8("BufWinLeave"),          EVENT_BUFWINLEAVE         ),
        new event_name_C(u8("BufWipeout"),           EVENT_BUFWIPEOUT          ),
        new event_name_C(u8("BufWrite"),             EVENT_BUFWRITEPRE         ),
        new event_name_C(u8("BufWritePost"),         EVENT_BUFWRITEPOST        ),
        new event_name_C(u8("BufWritePre"),          EVENT_BUFWRITEPRE         ),
        new event_name_C(u8("BufWriteCmd"),          EVENT_BUFWRITECMD         ),
        new event_name_C(u8("CmdwinEnter"),          EVENT_CMDWINENTER         ),
        new event_name_C(u8("CmdwinLeave"),          EVENT_CMDWINLEAVE         ),
        new event_name_C(u8("CmdUndefined"),         EVENT_CMDUNDEFINED        ),
        new event_name_C(u8("ColorScheme"),          EVENT_COLORSCHEME         ),
        new event_name_C(u8("CursorHold"),           EVENT_CURSORHOLD          ),
        new event_name_C(u8("CursorHoldI"),          EVENT_CURSORHOLDI         ),
        new event_name_C(u8("CursorMoved"),          EVENT_CURSORMOVED         ),
        new event_name_C(u8("CursorMovedI"),         EVENT_CURSORMOVEDI        ),
        new event_name_C(u8("FileAppendPost"),       EVENT_FILEAPPENDPOST      ),
        new event_name_C(u8("FileAppendPre"),        EVENT_FILEAPPENDPRE       ),
        new event_name_C(u8("FileAppendCmd"),        EVENT_FILEAPPENDCMD       ),
        new event_name_C(u8("FileChangedShell"),     EVENT_FILECHANGEDSHELL    ),
        new event_name_C(u8("FileChangedShellPost"), EVENT_FILECHANGEDSHELLPOST),
        new event_name_C(u8("FileChangedRO"),        EVENT_FILECHANGEDRO       ),
        new event_name_C(u8("FileReadPost"),         EVENT_FILEREADPOST        ),
        new event_name_C(u8("FileReadPre"),          EVENT_FILEREADPRE         ),
        new event_name_C(u8("FileReadCmd"),          EVENT_FILEREADCMD         ),
        new event_name_C(u8("FileType"),             EVENT_FILETYPE            ),
        new event_name_C(u8("FileWritePost"),        EVENT_FILEWRITEPOST       ),
        new event_name_C(u8("FileWritePre"),         EVENT_FILEWRITEPRE        ),
        new event_name_C(u8("FileWriteCmd"),         EVENT_FILEWRITECMD        ),
        new event_name_C(u8("FilterReadPost"),       EVENT_FILTERREADPOST      ),
        new event_name_C(u8("FilterReadPre"),        EVENT_FILTERREADPRE       ),
        new event_name_C(u8("FilterWritePost"),      EVENT_FILTERWRITEPOST     ),
        new event_name_C(u8("FilterWritePre"),       EVENT_FILTERWRITEPRE      ),
        new event_name_C(u8("FocusGained"),          EVENT_FOCUSGAINED         ),
        new event_name_C(u8("FocusLost"),            EVENT_FOCUSLOST           ),
        new event_name_C(u8("FuncUndefined"),        EVENT_FUNCUNDEFINED       ),
        new event_name_C(u8("InsertChange"),         EVENT_INSERTCHANGE        ),
        new event_name_C(u8("InsertEnter"),          EVENT_INSERTENTER         ),
        new event_name_C(u8("InsertLeave"),          EVENT_INSERTLEAVE         ),
        new event_name_C(u8("InsertCharPre"),        EVENT_INSERTCHARPRE       ),
        new event_name_C(u8("QuitPre"),              EVENT_QUITPRE             ),
        new event_name_C(u8("RemoteReply"),          EVENT_REMOTEREPLY         ),
        new event_name_C(u8("ShellCmdPost"),         EVENT_SHELLCMDPOST        ),
        new event_name_C(u8("ShellFilterPost"),      EVENT_SHELLFILTERPOST     ),
        new event_name_C(u8("SourcePre"),            EVENT_SOURCEPRE           ),
        new event_name_C(u8("SourceCmd"),            EVENT_SOURCECMD           ),
        new event_name_C(u8("StdinReadPost"),        EVENT_STDINREADPOST       ),
        new event_name_C(u8("StdinReadPre"),         EVENT_STDINREADPRE        ),
        new event_name_C(u8("SwapExists"),           EVENT_SWAPEXISTS          ),
        new event_name_C(u8("Syntax"),               EVENT_SYNTAX              ),
        new event_name_C(u8("TabEnter"),             EVENT_TABENTER            ),
        new event_name_C(u8("TabLeave"),             EVENT_TABLEAVE            ),
        new event_name_C(u8("TermChanged"),          EVENT_TERMCHANGED         ),
        new event_name_C(u8("TermResponse"),         EVENT_TERMRESPONSE        ),
        new event_name_C(u8("TextChanged"),          EVENT_TEXTCHANGED         ),
        new event_name_C(u8("TextChangedI"),         EVENT_TEXTCHANGEDI        ),
        new event_name_C(u8("User"),                 EVENT_USER                ),
        new event_name_C(u8("VimEnter"),             EVENT_VIMENTER            ),
        new event_name_C(u8("VimLeave"),             EVENT_VIMLEAVE            ),
        new event_name_C(u8("VimLeavePre"),          EVENT_VIMLEAVEPRE         ),
        new event_name_C(u8("WinEnter"),             EVENT_WINENTER            ),
        new event_name_C(u8("WinLeave"),             EVENT_WINLEAVE            ),
        new event_name_C(u8("VimResized"),           EVENT_VIMRESIZED          ),
    };

    /*private*/ static AutoPat_C[] first_autopat = new AutoPat_C[NUM_EVENTS];

    /*
     * Struct used to keep status while executing autocommands for an event.
     */
    /*private*/ static final class AutoPatCmd_C
    {
        AutoPat_C   curpat;         /* next AutoPat to examine */
        AutoCmd_C   nextcmd;        /* next AutoCmd to execute */
        int         group;          /* group being used */
        Bytes       fname;          /* fname to match with */
        Bytes       sfname;         /* sfname to match with */
        Bytes       tail;           /* tail of "fname" */
        int         event;          /* current event */
        int         arg_bufnr;      /* initially equal to <abuf>, set to zero when buf is deleted */
        AutoPatCmd_C next;          /* chain of active apc-s for auto-invalidation */

        /*private*/ AutoPatCmd_C()
        {
        }
    }

    /*private*/ static AutoPatCmd_C active_apc_list;            /* stack of active autocommands */

    /*
     * augroups stores a list of autocmd group names.
     */
    /*private*/ static Growing<Bytes> augroups = new Growing<Bytes>(Bytes.class, 10);

    /*
     * The ID of the current group.  Group 0 is the default one.
     */
    /*private*/ static int current_augroup = AUGROUP_DEFAULT;

    /*private*/ static boolean au_need_clean;                   /* need to delete marked patterns */

    /*private*/ static int last_event;
    /*private*/ static int last_group;
    /*private*/ static int autocmd_blocked;                /* block all autocmds */

    /*
     * Show the autocommands for one AutoPat.
     */
    /*private*/ static void show_autocmd(AutoPat_C ap, int event)
    {
        /* Check for "got_int" (here and at various places below),
         * which is set when "q" has been hit for the "--more--" prompt.
         */
        if (got_int)
            return;

        if (ap.pat == null)     /* pattern has been removed */
            return;

        msg_putchar('\n');
        if (got_int)
            return;

        if (event != last_event || ap.group != last_group)
        {
            if (ap.group != AUGROUP_DEFAULT)
            {
                if (augroups.ga_data[ap.group] == null)
                    msg_puts_attr(u8("--Deleted--"), hl_attr(HLF_E));
                else
                    msg_puts_attr(augroups.ga_data[ap.group], hl_attr(HLF_T));
                msg_puts(u8("  "));
            }
            msg_puts_attr(event_nr2name(event), hl_attr(HLF_T));

            last_event = event;
            last_group = ap.group;

            msg_putchar('\n');
            if (got_int)
                return;
        }

        msg_col = 4;
        msg_outtrans(ap.pat);

        for (AutoCmd_C ac = ap.cmds; ac != null; ac = ac.next)
        {
            if (ac.cmd != null) /* skip removed commands */
            {
                if (14 <= msg_col)
                    msg_putchar('\n');
                msg_col = 14;
                if (got_int)
                    return;

                msg_outtrans(ac.cmd);
                if (0 < p_verbose[0])
                    last_set_msg(ac.scriptID);
                if (got_int)
                    return;

                if (ac.next != null)
                {
                    msg_putchar('\n');
                    if (got_int)
                        return;
                }
            }
        }
    }

    /*
     * Mark an autocommand pattern for deletion.
     */
    /*private*/ static void au_remove_pat(AutoPat_C ap)
    {
        ap.pat = null;
        ap.buflocal_nr = -1;

        au_need_clean = true;
    }

    /*
     * Mark all commands for a pattern for deletion.
     */
    /*private*/ static void au_remove_cmds(AutoPat_C ap)
    {
        for (AutoCmd_C ac = ap.cmds; ac != null; ac = ac.next)
            ac.cmd = null;

        au_need_clean = true;
    }

    /*
     * Cleanup autocommands and patterns that have been deleted.
     * This is only done when not executing autocommands.
     */
    /*private*/ static void au_cleanup()
    {
        if (autocmd_busy || !au_need_clean)
            return;

        /* loop over all events */
        for (int event = 0; event < NUM_EVENTS; event++)
        {
            /* loop over all autocommand patterns */
            for (AutoPat_C prev_ap = null, ap = first_autopat[event]; ap != null; ap = ap.next)
            {
                /* loop over all commands for this pattern */
                for (AutoCmd_C prev_ac = null, ac = ap.cmds; ac != null; ac = ac.next)
                {
                    /* remove the command if the pattern is to be deleted
                     * or when the command has been marked for deletion */
                    if (ap.pat != null && ac.cmd != null)
                        prev_ac = ac;
                    else if (prev_ac != null)
                        prev_ac.next = ac.next;
                    else
                        ap.cmds = ac.next;
                }

                /* remove the pattern if it has been marked for deletion */
                if (ap.pat != null)
                    prev_ap = ap;
                else if (prev_ap != null)
                    prev_ap.next = ap.next;
                else
                    first_autopat[event] = ap.next;
            }
        }

        au_need_clean = false;
    }

    /*
     * Called when buffer is freed, to remove/invalidate related buffer-local autocmds.
     */
    /*private*/ static void aubuflocal_remove(buffer_C buf)
    {
        /* invalidate currently executing autocommands */
        for (AutoPatCmd_C apc = active_apc_list; apc != null; apc = apc.next)
            if (buf.b_fnum == apc.arg_bufnr)
                apc.arg_bufnr = 0;

        /* invalidate buflocals looping through events */
        for (int event = 0; event < NUM_EVENTS; event++)
            /* loop over all autocommand patterns */
            for (AutoPat_C ap = first_autopat[event]; ap != null; ap = ap.next)
                if (ap.buflocal_nr == buf.b_fnum)
                {
                    au_remove_pat(ap);
                    if (6 <= p_verbose[0])
                    {
                        verbose_enter();
                        smsg(u8("auto-removing autocommand: %s <buffer=%d>"), event_nr2name(event), buf.b_fnum);
                        verbose_leave();
                    }
                }

        au_cleanup();
    }

    /*
     * Add an autocmd group name.
     * Return it's ID.  Returns AUGROUP_ERROR (< 0) for error.
     */
    /*private*/ static int au_new_group(Bytes name)
    {
        int i = au_find_group(name);

        if (i == AUGROUP_ERROR)         /* the group doesn't exist yet, add it */
        {
            /* First try using a free entry. */
            for (i = 0; i < augroups.ga_len; i++)
                if (augroups.ga_data[i] == null)
                    break;

            if (i == augroups.ga_len)
                augroups.ga_grow(1);
            augroups.ga_data[i] = STRDUP(name);
            if (i == augroups.ga_len)
                augroups.ga_len++;
        }

        return i;
    }

    /*private*/ static void au_del_group(Bytes name)
    {
        int i = au_find_group(name);
        if (i == AUGROUP_ERROR)         /* the group doesn't exist */
            emsg2(u8("E367: No such group: \"%s\""), name);
        else
            augroups.ga_data[i] = null;
    }

    /*
     * Find the ID of an autocmd group name.
     * Return it's ID.  Returns AUGROUP_ERROR (< 0) for error.
     */
    /*private*/ static int au_find_group(Bytes name)
    {
        for (int i = 0; i < augroups.ga_len; i++)
            if (augroups.ga_data[i] != null && STRCMP(augroups.ga_data[i], name) == 0)
                return i;

        return AUGROUP_ERROR;
    }

    /*
     * Return true if augroup "name" exists.
     */
    /*private*/ static boolean au_has_group(Bytes name)
    {
        return (au_find_group(name) != AUGROUP_ERROR);
    }

    /*
     * ":augroup {name}".
     */
    /*private*/ static void do_augroup(Bytes arg, boolean del_group)
    {
        if (del_group)
        {
            if (arg.at(0) == NUL)
                emsg(e_argreq);
            else
                au_del_group(arg);
        }
        else if (STRCASECMP(arg, u8("end")) == 0)           /* ":aug end": back to group 0 */
            current_augroup = AUGROUP_DEFAULT;
        else if (arg.at(0) != NUL)                                  /* ":aug xxx": switch to group xxx */
        {
            int i = au_new_group(arg);
            if (i != AUGROUP_ERROR)
                current_augroup = i;
        }
        else                                            /* ":aug": list the group names */
        {
            msg_start();
            for (int i = 0; i < augroups.ga_len; i++)
            {
                if (augroups.ga_data[i] != null)
                {
                    msg_puts(augroups.ga_data[i]);
                    msg_puts(u8("  "));
                }
            }
            msg_clr_eos();
            msg_end();
        }
    }

    /*
     * Return the event number for event name "start".
     * Return NUM_EVENTS if the event name was not found.
     * Return a pointer to the next event name in "end".
     */
    /*private*/ static int event_name2nr(Bytes start, Bytes[] end)
    {
        /* the event name ends with end of line, a blank or a comma */
        Bytes p;
        for (p = start; p.at(0) != NUL && !vim_iswhite(p.at(0)) && p.at(0) != (byte)','; p = p.plus(1))
            ;
        int i;
        for (i = 0; i < event_names.length; i++)
        {
            int len = strlen(event_names[i].name);
            if (len == BDIFF(p, start) && STRNCASECMP(event_names[i].name, start, len) == 0)
                break;
        }
        if (p.at(0) == (byte)',')
            p = p.plus(1);
        end[0] = p;
        if (i < event_names.length)
            return event_names[i].event;

        return NUM_EVENTS;
    }

    /*
     * Return the name for event "event".
     */
    /*private*/ static Bytes event_nr2name(int event)
    {
        for (int i = 0; i < event_names.length; i++)
            if (event_names[i].event == event)
                return event_names[i].name;

        return u8("Unknown");
    }

    /*
     * Scan over the events.  "*" stands for all events.
     */
    /*private*/ static Bytes find_end_event(Bytes arg, boolean have_group)
        /* have_group: true when group name was found */
    {
        if (arg.at(0) == (byte)'*')
        {
            if (arg.at(1) != NUL && !vim_iswhite(arg.at(1)))
            {
                emsg2(u8("E215: Illegal character after *: %s"), arg);
                return null;
            }
            return arg.plus(1);
        }

        Bytes pat = arg;

        while (pat.at(0) != NUL && !vim_iswhite(pat.at(0)))
        {
            Bytes[] p = new Bytes[1];
            if (NUM_EVENTS <= event_name2nr(pat, p))
            {
                if (have_group)
                    emsg2(u8("E216: No such event: %s"), pat);
                else
                    emsg2(u8("E216: No such group or event: %s"), pat);
                return null;
            }
            pat = p[0];
        }

        return pat;
    }

    /*
     * Return true if "event" is included in 'eventignore'.
     */
    /*private*/ static boolean event_ignored(int event)
    {
        Bytes[] p = { p_ei[0] };

        while (p[0].at(0) != NUL)
        {
            if (STRNCASECMP(p[0], u8("all"), 3) == 0 && (p[0].at(3) == NUL || p[0].at(3) == (byte)','))
                return true;
            if (event_name2nr(p[0], p) == event)
                return true;
        }

        return false;
    }

    /*
     * Return true when the contents of "p_ei" is valid, false otherwise.
     */
    /*private*/ static boolean check_ei()
    {
        Bytes[] p = { p_ei[0] };

        while (p[0].at(0) != NUL)
        {
            if (STRNCASECMP(p[0], u8("all"), 3) == 0 && (p[0].at(3) == NUL || p[0].at(3) == (byte)','))
            {
                p[0] = p[0].plus(3);
                if (p[0].at(0) == (byte)',')
                    p[0] = p[0].plus(1);
            }
            else if (event_name2nr(p[0], p) == NUM_EVENTS)
                return false;
        }

        return true;
    }

    /*
     * Add "what" to 'eventignore' to skip loading syntax highlighting for
     * every buffer loaded into the window.  "what" must start with a comma.
     * Returns the old value of 'eventignore' in allocated memory.
     */
    /*private*/ static Bytes au_event_disable(Bytes what)
    {
        Bytes save_ei = STRDUP(p_ei[0]);

        Bytes new_ei = STRNDUP(p_ei[0], strlen(p_ei[0]) + strlen(what));
        if (what.at(0) == (byte)',' && p_ei[0].at(0) == NUL)
            STRCPY(new_ei, what.plus(1));
        else
            STRCAT(new_ei, what);
        set_string_option_direct(u8("ei"), -1, new_ei, OPT_FREE, SID_NONE);

        return save_ei;
    }

    /*private*/ static void au_event_restore(Bytes old_ei)
    {
        if (old_ei != null)
            set_string_option_direct(u8("ei"), -1, old_ei, OPT_FREE, SID_NONE);
    }

    /*
     * do_autocmd() -- implements the :autocmd command.  Can be used in the
     *  following ways:
     *
     * :autocmd <event> <pat> <cmd>     Add <cmd> to the list of commands that
     *                                  will be automatically executed for <event>
     *                                  when editing a file matching <pat>, in
     *                                  the current group.
     * :autocmd <event> <pat>           Show the auto-commands associated with
     *                                  <event> and <pat>.
     * :autocmd <event>                 Show the auto-commands associated with
     *                                  <event>.
     * :autocmd                         Show all auto-commands.
     * :autocmd! <event> <pat> <cmd>    Remove all auto-commands associated with
     *                                  <event> and <pat>, and add the command
     *                                  <cmd>, for the current group.
     * :autocmd! <event> <pat>          Remove all auto-commands associated with
     *                                  <event> and <pat> for the current group.
     * :autocmd! <event>                Remove all auto-commands associated with
     *                                  <event> for the current group.
     * :autocmd!                        Remove ALL auto-commands for the current
     *                                  group.
     *
     *  Multiple events and patterns may be given separated by commas.  Here are
     *  some examples:
     * :autocmd bufread,bufenter *.c,*.h    set tw=0 smartindent noic
     * :autocmd bufleave         *          set tw=79 nosmartindent ic infercase
     *
     * :autocmd * *.c               show all autocommands for *.c files.
     *
     * Mostly a {group} argument can optionally appear before <event>.
     */
    /*private*/ static void do_autocmd(Bytes _arg, boolean forceit)
    {
        Bytes[] arg = { _arg };
        boolean nested = false;

        /*
         * Check for a legal group name.  If not, use AUGROUP_ALL.
         */
        int group = au_get_grouparg(arg);
        if (arg[0] == null)        /* out of memory */
            return;

        /*
         * Scan over the events.
         * If we find an illegal name, return here, don't do anything.
         */
        Bytes pat = find_end_event(arg[0], group != AUGROUP_ALL);
        if (pat == null)
            return;

        /*
         * Scan over the pattern.  Put a NUL at the end.
         */
        pat = skipwhite(pat);
        Bytes cmd = pat;
        while (cmd.at(0) != NUL && (!vim_iswhite(cmd.at(0)) || cmd.at(-1) == (byte)'\\'))
            cmd = cmd.plus(1);
        if (cmd.at(0) != NUL)
            (cmd = cmd.plus(1)).be(-1, NUL);

        /*
         * Check for "nested" flag.
         */
        cmd = skipwhite(cmd);
        if (cmd.at(0) != NUL && STRNCMP(cmd, u8("nested"), 6) == 0 && vim_iswhite(cmd.at(6)))
        {
            nested = true;
            cmd = skipwhite(cmd.plus(6));
        }

        /*
         * Find the start of the commands.
         * Expand <sfile> in it.
         */
        if (cmd.at(0) != NUL)
        {
            cmd = expand_sfile(cmd);
            if (cmd == null)            /* some error */
                return;
        }

        /*
         * Print header when showing autocommands.
         */
        if (!forceit && cmd.at(0) == NUL)
        {
            /* Highlight title. */
            msg_puts_title(u8("\n--- Auto-Commands ---"));
        }

        /*
         * Loop over the events.
         */
        last_event = -1;           /* for listing the event name */
        last_group = AUGROUP_ERROR;         /* for listing the group name */
        if (arg[0].at(0) == (byte)'*' || arg[0].at(0) == NUL)
        {
            for (int event = 0; event < NUM_EVENTS; event++)
                if (do_autocmd_event(event, pat, nested, cmd, forceit, group) == false)
                    break;
        }
        else
        {
            while (arg[0].at(0) != NUL && !vim_iswhite(arg[0].at(0)))
                if (do_autocmd_event(event_name2nr(arg[0], arg), pat, nested, cmd, forceit, group) == false)
                    break;
        }
    }

    /*
     * Find the group ID in a ":autocmd" or ":doautocmd" argument.
     * The "argp" argument is advanced to the following argument.
     *
     * Returns the group ID, AUGROUP_ERROR for error (out of memory).
     */
    /*private*/ static int au_get_grouparg(Bytes[] argp)
    {
        int group = AUGROUP_ALL;

        Bytes arg = argp[0];

        Bytes p = skiptowhite(arg);
        if (BLT(arg, p))
        {
            Bytes group_name = STRNDUP(arg, BDIFF(p, arg));

            group = au_find_group(group_name);
            if (group == AUGROUP_ERROR)
                group = AUGROUP_ALL;                    /* no match, use all groups */
            else
                argp[0] = skipwhite(p);                   /* match, skip over group name */
        }

        return group;
    }

    /*
     * do_autocmd() for one event.
     * If pat[0] == NUL do for all patterns.
     * If cmd[0] == NUL show entries.
     * If forceit == true delete entries.
     * If group is not AUGROUP_ALL, only use this group.
     */
    /*private*/ static boolean do_autocmd_event(int event, Bytes pat, boolean nested, Bytes cmd, boolean forceit, int group)
    {
        Bytes buflocal_pat = new Bytes(25);     /* for "<buffer=X>" */

        int findgroup;
        if (group == AUGROUP_ALL)
            findgroup = current_augroup;
        else
            findgroup = group;
        boolean allgroups = (group == AUGROUP_ALL && !forceit && cmd.at(0) == NUL);

        /*
         * Show or delete all patterns for an event.
         */
        if (pat.at(0) == NUL)
        {
            for (AutoPat_C ap = first_autopat[event]; ap != null; ap = ap.next)
            {
                if (forceit)            /* delete the AutoPat, if it's in the current group */
                {
                    if (ap.group == findgroup)
                        au_remove_pat(ap);
                }
                else if (group == AUGROUP_ALL || ap.group == group)
                    show_autocmd(ap, event);
            }
        }

        /*
         * Loop through all the specified patterns.
         */
        Bytes endpat;
        for ( ; pat.at(0) != NUL; pat = (endpat.at(0) == (byte)',') ? endpat.plus(1) : endpat)
        {
            /*
             * Find end of the pattern.
             * Watch out for a comma in braces, like "*.\{obj,o\}".
             */
            int brace_level = 0;
            for (endpat = pat; endpat.at(0) != NUL && (endpat.at(0) != (byte)',' || brace_level != 0 || endpat.at(-1) == (byte)'\\'); endpat = endpat.plus(1))
            {
                if (endpat.at(0) == (byte)'{')
                    brace_level++;
                else if (endpat.at(0) == (byte)'}')
                    brace_level--;
            }
            if (BEQ(pat, endpat))              /* ignore single comma */
                continue;
            int patlen = BDIFF(endpat, pat);

            /*
             * detect special <buflocal[=X]> buffer-local patterns
             */
            boolean is_buflocal = false;
            int buflocal_nr = 0;

            if (8 <= patlen && STRNCMP(pat, u8("<buffer"), 7) == 0 && pat.at(patlen - 1) == (byte)'>')
            {
                /* "<buffer...>": error will be printed only for addition.
                 * Printing and removing will proceed silently. */
                is_buflocal = true;
                if (patlen == 8)
                    /* "<buffer>" */
                    buflocal_nr = curbuf.b_fnum;
                else if (9 < patlen && pat.at(7) == (byte)'=')
                {
                    if (patlen == 13 && STRNCASECMP(pat, u8("<buffer=abuf>"), 13) == 0)
                        /* "<buffer=abuf>" */
                        buflocal_nr = autocmd_bufnr;
                    else if (BEQ(skipdigits(pat.plus(8)), pat.plus(patlen - 1)))
                        /* "<buffer=123>" */
                        buflocal_nr = libC.atoi(pat.plus(8));
                }
            }

            if (is_buflocal)
            {
                /* normalize "pat" into standard "<buffer>#N" form */
                libC.sprintf(buflocal_pat, u8("<buffer=%d>"), buflocal_nr);
                pat = buflocal_pat;                             /* can modify "pat" and "patlen", but not "endpat" */
                patlen = strlen(buflocal_pat);
            }

            /*
             * Find AutoPat entries with this pattern.
             */
            AutoPat_C prev_ap = null, ap = first_autopat[event];
            for ( ; ap != null; ap = ap.next)
            {
                if (ap.pat != null)
                {
                    /* Accept a pattern when:
                     * - a group was specified and it's that group, or a group was
                     *   not specified and it's the current group, or a group was
                     *   not specified and we are listing
                     * - the length of the pattern matches
                     * - the pattern matches.
                     * For <buffer[=X]>, this condition works because we normalize
                     * all buffer-local patterns.
                     */
                    if ((allgroups || ap.group == findgroup)
                            && ap.patlen == patlen
                            && STRNCMP(pat, ap.pat, patlen) == 0)
                    {
                        /*
                         * Remove existing autocommands.
                         * If adding any new autocmd's for this AutoPat, don't delete
                         * the pattern from the autopat list, append to this list.
                         */
                        if (forceit)
                        {
                            if (cmd.at(0) != NUL && ap.next == null)
                            {
                                au_remove_cmds(ap);
                                break;
                            }
                            au_remove_pat(ap);
                        }
                        /*
                         * Show autocmd's for this autopat, or buflocals <buffer=X>
                         */
                        else if (cmd.at(0) == NUL)
                            show_autocmd(ap, event);
                        /*
                         * Add autocmd to this autopat, if it's the last one.
                         */
                        else if (ap.next == null)
                            break;
                    }
                }
                prev_ap = ap;
            }

            /*
             * Add a new command.
             */
            if (cmd.at(0) != NUL)
            {
                /*
                 * If the pattern we want to add a command to does appear at the
                 * end of the list (or not is not in the list at all), add the
                 * pattern at the end of the list.
                 */
                if (ap == null)
                {
                    /* refuse to add buffer-local ap if buffer number is invalid */
                    if (is_buflocal && (buflocal_nr == 0 || buflist_findnr(buflocal_nr) == null))
                    {
                        emsgn(u8("E680: <buffer=%d>: invalid buffer number "), (long)buflocal_nr);
                        return false;
                    }

                    ap = new AutoPat_C();

                    ap.pat = STRNDUP(pat, patlen);
                    ap.patlen = patlen;

                    if (is_buflocal)
                    {
                        ap.buflocal_nr = buflocal_nr;
                        ap.reg_prog = null;
                    }
                    else
                    {
                        ap.buflocal_nr = 0;
                        Bytes reg_pat;
                        { boolean[] __ = { ap.allow_dirs }; reg_pat = file_pat_to_reg_pat(pat, endpat, __); ap.allow_dirs = __[0]; }
                        if (reg_pat != null)
                            ap.reg_prog = vim_regcomp(reg_pat, RE_MAGIC);
                        if (reg_pat == null || ap.reg_prog == null)
                            return false;
                    }
                    ap.cmds = null;
                    if (prev_ap != null)
                        prev_ap.next = ap;
                    else
                        first_autopat[event] = ap;
                    ap.next = null;
                    if (group == AUGROUP_ALL)
                        ap.group = current_augroup;
                    else
                        ap.group = group;
                }

                /*
                 * Add the autocmd at the end of the AutoCmd list.
                 */
                AutoCmd_C prev_ac = null;
                for (AutoCmd_C ac = ap.cmds; ac != null; ac = ac.next)
                    prev_ac = ac;

                AutoCmd_C ac = new AutoCmd_C();

                ac.cmd = STRDUP(cmd);
                ac.nested = nested;
                ac.scriptID = current_SID;

                if (prev_ac != null)
                    prev_ac.next = ac;
                else
                    ap.cmds = ac;
            }
        }

        au_cleanup();       /* may really delete removed patterns/commands now */
        return true;
    }

    /*
     * Implementation of ":doautocmd [group] event [fname]".
     * Return true for success, false for failure;
     */
    /*private*/ static boolean do_doautocmd(Bytes _arg, boolean do_msg)
        /* do_msg: give message for no matching autocmds? */
    {
        Bytes[] arg = { _arg };
        /*
         * Check for a legal group name.  If not, use AUGROUP_ALL.
         */
        int group = au_get_grouparg(arg);
        if (arg[0] == null)        /* out of memory */
            return false;

        if (arg[0].at(0) == (byte)'*')
        {
            emsg(u8("E217: Can't execute autocommands for ALL events"));
            return false;
        }

        /*
         * Scan over the events.
         * If we find an illegal name, return here, don't do anything.
         */
        Bytes fname = find_end_event(arg[0], group != AUGROUP_ALL);
        if (fname == null)
            return false;
        fname = skipwhite(fname);

        boolean nothing_done = true;

        /*
         * Loop over the events.
         */
        while (arg[0].at(0) != NUL && !vim_iswhite(arg[0].at(0)))
            if (apply_autocmds_group(event_name2nr(arg[0], arg), fname, null, true, group, curbuf, null))
                nothing_done = false;

        if (nothing_done && do_msg)
            msg(u8("No matching autocommands"));

        return aborting() ? false : true;
    }

    /*
     * ":doautoall": execute autocommands for each loaded buffer.
     */
    /*private*/ static final ex_func_C ex_doautoall = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes arg = eap.arg;

            /*
             * This is a bit tricky: For some commands curwin.w_buffer needs to be equal to curbuf,
             * but for some buffers there may not be a window.
             * So we change the buffer for the current window for a moment.
             * This gives problems when the autocommands make changes to the list of buffers or windows...
             */
            for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
                if (buf.b_ml.ml_mfp != null)
                {
                    /* find a window for this buffer and save some values */
                    aco_save_C aco = new aco_save_C();
                    aucmd_prepbuf(aco, buf);

                    /* execute the autocommands for this buffer */
                    boolean retval = do_doautocmd(arg, false);

                    /* restore the current window */
                    aucmd_restbuf(aco);

                    /* stop if there is some error or buffer was deleted */
                    if (retval == false || !buf_valid(buf))
                        break;
                }

            check_cursor();         /* just in case lines got deleted */
        }
    };

    /*
     * Prepare for executing autocommands for (hidden) buffer "buf".
     * Search for a visible window containing the current buffer.  If there isn't
     * one then use "aucmd_win".
     * Set "curbuf" and "curwin" to match "buf".
     * When FEAT_AUTOCMD is not defined another version is used, see below.
     */
    /*private*/ static void aucmd_prepbuf(aco_save_C aco, buffer_C buf)
        /* aco: structure to save values in */
        /* buf: new curbuf */
    {
        /* Find a window that is for the new buffer. */
        window_C win;
        if (buf == curbuf)          /* be quick when buf is curbuf */
            win = curwin;
        else
        {
            for (win = firstwin; win != null; win = win.w_next)
                if (win.w_buffer == buf)
                    break;
        }

        /* Allocate "aucmd_win" when needed.
         * If this fails (out of memory) fall back to using the current window. */
        if (win == null && aucmd_win == null)
        {
            win_alloc_aucmd_win();
            if (aucmd_win == null)
                win = curwin;
        }
        if (win == null && aucmd_win_used)
            /* Strange recursive autocommand, fall back to using the current window.
             * Expect a few side effects... */
            win = curwin;

        aco.save_curwin = curwin;
        aco.save_curbuf = curbuf;
        if (win != null)
        {
            /* There is a window for "buf" in the current tab page, make it the curwin.
             * This is preferred, it has the least side effects (esp. if "buf" is curbuf). */
            aco.use_aucmd_win = false;
            curwin = win;
        }
        else
        {
            /* There is no window for "buf", use "aucmd_win".
             * To minimize the side effects, insert it in the current tab page.
             * Anything related to a window (e.g., setting folds) may have unexpected results. */
            aco.use_aucmd_win = true;
            aucmd_win_used = true;
            aucmd_win.w_buffer = buf;
            aucmd_win.w_s = buf.b_s;
            buf.b_nwindows++;
            win_init_empty(aucmd_win); /* set cursor and topline to safe values */

            /* Split the current window, put the aucmd_win in the upper half.
             * We don't want the BufEnter or WinEnter autocommands. */
            block_autocmds();
            make_snapshot(SNAP_AUCMD_IDX);
            boolean save_ea = p_ea[0];
            p_ea[0] = false;

            win_split_ins(0, WSP_TOP, aucmd_win, 0);
            win_comp_pos();                 /* recompute window positions */
            p_ea[0] = save_ea;
            unblock_autocmds();
            curwin = aucmd_win;
        }
        curbuf = buf;
        aco.new_curwin = curwin;
        aco.new_curbuf = curbuf;
    }

    /*
     * Cleanup after executing autocommands for a (hidden) buffer.
     * Restore the window as it was (if possible).
     * When FEAT_AUTOCMD is not defined another version is used, see below.
     */
    /*private*/ static void aucmd_restbuf(aco_save_C aco)
        /* aco: structure holding saved values */
    {
        if (aco.use_aucmd_win)
        {
            --curbuf.b_nwindows;
            /* Find "aucmd_win", it can't be closed, but it may be in another tab page.
             * Do not trigger autocommands here. */
            block_autocmds();
            if (curwin != aucmd_win)
            {
                win_found:
                for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
                    for (window_C wp = (tp == curtab) ? firstwin : tp.tp_firstwin; wp != null; wp = wp.w_next)
                        if (wp == aucmd_win)
                        {
                            if (tp != curtab)
                                goto_tabpage_tp(tp, true, true);
                            win_goto(aucmd_win);
                            break win_found;
                        }
            }

            int[] dummy = new int[1];
            /* Remove the window and frame from the tree of frames. */
            winframe_remove(curwin, dummy, null);
            win_remove(curwin, null);
            aucmd_win_used = false;
            last_status(false);                             /* may need to remove last status line */
            restore_snapshot(SNAP_AUCMD_IDX, false);
            win_comp_pos();                                 /* recompute window positions */
            unblock_autocmds();

            if (win_valid(aco.save_curwin))
                curwin = aco.save_curwin;
            else
                /* Hmm, original window disappeared.  Just use the first one. */
                curwin = firstwin;
            vars_clear(aucmd_win.w_vars.dv_hashtab);        /* free all w: variables */
            hash_init(aucmd_win.w_vars.dv_hashtab);         /* re-use the hashtab */
            curbuf = curwin.w_buffer;

            /* the buffer contents may have changed */
            check_cursor();
            if (curwin.w_topline > curbuf.b_ml.ml_line_count)
                curwin.w_topline = curbuf.b_ml.ml_line_count;
        }
        else
        {
            /* restore curwin */
            if (win_valid(aco.save_curwin))
            {
                /* Restore the buffer which was previously edited by curwin, if it was changed,
                 * we are still the same window and the buffer is valid. */
                if (curwin == aco.new_curwin
                        && curbuf != aco.new_curbuf
                        && buf_valid(aco.new_curbuf)
                        && aco.new_curbuf.b_ml.ml_mfp != null)
                {
                    if (curwin.w_s == curbuf.b_s)
                        curwin.w_s = aco.new_curbuf.b_s;
                    --curbuf.b_nwindows;
                    curbuf = aco.new_curbuf;
                    curwin.w_buffer = curbuf;
                    curbuf.b_nwindows++;
                }

                curwin = aco.save_curwin;
                curbuf = curwin.w_buffer;
                /* In case the autocommand move the cursor to a position that that not exist in curbuf. */
                check_cursor();
            }
        }
    }

    /*private*/ static boolean  autocmd_nested;

    /*
     * Execute autocommands for "event" and file name "fname".
     * Return true if some commands were executed.
     */
    /*private*/ static boolean apply_autocmds(int event, Bytes fname, Bytes fname_io, boolean force, buffer_C buf)
        /* fname: null or empty means use actual file name */
        /* fname_io: fname to use for <afile> on cmdline */
        /* force: when true, ignore autocmd_busy */
        /* buf: buffer for <abuf> */
    {
        return apply_autocmds_group(event, fname, fname_io, force, AUGROUP_ALL, buf, null);
    }

    /*
     * Like apply_autocmds(), but with extra "eap" argument.  This takes care of setting v:filearg.
     */
    /*private*/ static boolean apply_autocmds_exarg(int event, Bytes fname, Bytes fname_io, boolean force, buffer_C buf, exarg_C eap)
    {
        return apply_autocmds_group(event, fname, fname_io, force, AUGROUP_ALL, buf, eap);
    }

    /*
     * Like apply_autocmds(), but handles the caller's retval.  If the script
     * processing is being aborted or if retval is false when inside a try
     * conditional, no autocommands are executed.  If otherwise the autocommands
     * cause the script to be aborted, retval is set to false.
     */
    /*private*/ static boolean apply_autocmds_retval(int event, Bytes fname, Bytes fname_io, boolean force, buffer_C buf, boolean[] retval)
        /* fname: null or empty means use actual file name */
        /* fname_io: fname to use for <afile> on cmdline */
        /* force: when true, ignore autocmd_busy */
        /* buf: buffer for <abuf> */
        /* retval: pointer to caller's retval */
    {
        if (should_abort(retval[0]))
            return false;

        boolean did_cmd = apply_autocmds_group(event, fname, fname_io, force, AUGROUP_ALL, buf, null);
        if (did_cmd && aborting())
            retval[0] = false;
        return did_cmd;
    }

    /*
     * Return true when there is a CursorHold autocommand defined.
     */
    /*private*/ static boolean has_cursorhold()
    {
        int state = (get_real_state() == NORMAL_BUSY) ? EVENT_CURSORHOLD : EVENT_CURSORHOLDI;
        return (first_autopat[state] != null);
    }

    /*
     * Return true if the CursorHold event can be triggered.
     */
    /*private*/ static boolean trigger_cursorhold()
    {
        if (!did_cursorhold && has_cursorhold() && !Recording && typebuf.tb_len == 0)
        {
            int state = get_real_state();
            if (state == NORMAL_BUSY || (state & INSERT) != 0)
                return true;
        }
        return false;
    }

    /*
     * Return true when there is a CursorMoved autocommand defined.
     */
    /*private*/ static boolean has_cursormoved()
    {
        return (first_autopat[EVENT_CURSORMOVED] != null);
    }

    /*
     * Return true when there is a CursorMovedI autocommand defined.
     */
    /*private*/ static boolean has_cursormovedI()
    {
        return (first_autopat[EVENT_CURSORMOVEDI] != null);
    }

    /*
     * Return true when there is a TextChanged autocommand defined.
     */
    /*private*/ static boolean has_textchanged()
    {
        return (first_autopat[EVENT_TEXTCHANGED] != null);
    }

    /*
     * Return true when there is a TextChangedI autocommand defined.
     */
    /*private*/ static boolean has_textchangedI()
    {
        return (first_autopat[EVENT_TEXTCHANGEDI] != null);
    }

    /*
     * Return true when there is an InsertCharPre autocommand defined.
     */
    /*private*/ static boolean has_insertcharpre()
    {
        return (first_autopat[EVENT_INSERTCHARPRE] != null);
    }

    /*
     * Return true when there is an CmdUndefined autocommand defined.
     */
    /*private*/ static boolean has_cmdundefined()
    {
        return (first_autopat[EVENT_CMDUNDEFINED] != null);
    }

    /*
     * Return true when there is an FuncUndefined autocommand defined.
     */
    /*private*/ static boolean has_funcundefined()
    {
        return (first_autopat[EVENT_FUNCUNDEFINED] != null);
    }

    /*private*/ static boolean filechangeshell_busy;

    /*private*/ static int __nesting;

    /*private*/ static boolean apply_autocmds_group(int event, Bytes fname, Bytes fname_io, boolean force, int group, buffer_C buf, exarg_C eap)
        /* fname: null or empty means use actual file name */
        /* fname_io: fname to use for <afile> on cmdline, null means use fname */
        /* force: when true, ignore autocmd_busy */
        /* group: group ID, or AUGROUP_ALL */
        /* buf: buffer for <abuf> */
        /* eap: command arguments */
    {
        boolean retval = false;

        AutoPatCmd_C patcmd = new AutoPatCmd_C();

        theend:
        {
            /*
             * Quickly return if there are no autocommands for this event or autocommands are blocked.
             */
            if (first_autopat[event] == null || 0 < autocmd_blocked)
                break theend;

            /*
             * When autocommands are busy, new autocommands are only executed when
             * explicitly enabled with the "nested" flag.
             */
            if (autocmd_busy && !(force || autocmd_nested))
                break theend;

            /*
             * Quickly return when immediately aborting on error, or when an interrupt
             * occurred or an exception was thrown but not caught.
             */
            if (aborting())
                break theend;

            /*
             * FileChangedShell never nests, because it can create an endless loop.
             */
            if (filechangeshell_busy && (event == EVENT_FILECHANGEDSHELL || event == EVENT_FILECHANGEDSHELLPOST))
                break theend;

            /*
             * Ignore events in 'eventignore'.
             */
            if (event_ignored(event))
                break theend;

            /*
             * Allow nesting of autocommands, but restrict the depth, because it's
             * possible to create an endless loop.
             */
            if (__nesting == 10)
            {
                emsg(u8("E218: autocommand nesting too deep"));
                break theend;
            }

            /*
             * Check if these autocommands are disabled.  Used when doing ":all" or ":ball".
             */
            if ((autocmd_no_enter != 0 && (event == EVENT_WINENTER || event == EVENT_BUFENTER))
             || (autocmd_no_leave != 0 && (event == EVENT_WINLEAVE || event == EVENT_BUFLEAVE)))
                break theend;

            /*
             * Save the autocmd_* variables and info about the current buffer.
             */
            Bytes save_autocmd_fname = autocmd_fname;
            boolean save_autocmd_fname_full = autocmd_fname_full;
            int save_autocmd_bufnr = autocmd_bufnr;
            Bytes save_autocmd_match = autocmd_match;
            boolean save_autocmd_busy = autocmd_busy;
            boolean save_autocmd_nested = autocmd_nested;
            boolean save_changed = curbuf.b_changed[0];

            buffer_C old_curbuf = curbuf;

            /*
             * Set the file name to be used for <afile>.
             * Make a copy to avoid that changing a buffer name or directory makes it invalid.
             */
            if (fname_io == null)
            {
                if (event == EVENT_COLORSCHEME)
                    autocmd_fname = null;
                else if (fname != null && fname.at(0) != NUL)
                    autocmd_fname = fname;
                else if (buf != null)
                    autocmd_fname = buf.b_ffname;
                else
                    autocmd_fname = null;
            }
            else
                autocmd_fname = fname_io;
            if (autocmd_fname != null)
                autocmd_fname = STRDUP(autocmd_fname);
            autocmd_fname_full = false;                         /* call fullName_save() later */

            /*
             * Set the buffer number to be used for <abuf>.
             */
            autocmd_bufnr = (buf != null) ? buf.b_fnum : 0;

            Bytes sfname = null;           /* short file name */

            /*
             * When the file name is null or empty, use the file name of buffer "buf".
             * Always use the full path of the file name to match with, in case "allow_dirs" is set.
             */
            if (fname == null || fname.at(0) == NUL)
            {
                if (buf == null)
                    fname = null;
                else
                {
                    if (event == EVENT_SYNTAX)
                        fname = buf.b_p_syn[0];
                    else if (event == EVENT_FILETYPE)
                        fname = buf.b_p_ft[0];
                    else
                    {
                        if (buf.b_sfname != null)
                            sfname = STRDUP(buf.b_sfname);
                        fname = buf.b_ffname;
                    }
                }
                if (fname == null)
                    fname = u8("");
                fname = STRDUP(fname);      /* make a copy, so we can change it */
            }
            else
            {
                sfname = STRDUP(fname);
                /* Don't try expanding FileType, Syntax, FuncUndefined, WindowID or ColorScheme. */
                if (event == EVENT_FILETYPE
                        || event == EVENT_SYNTAX
                        || event == EVENT_FUNCUNDEFINED
                        || event == EVENT_REMOTEREPLY
                        || event == EVENT_COLORSCHEME)
                    fname = STRDUP(fname);
                else
                    fname = fullName_save(fname, false);
            }

            /*
             * Set the name to be used for <amatch>.
             */
            autocmd_match = fname;

            /* Don't redraw while doing auto commands. */
            redrawingDisabled++;
            Bytes save_sourcing_name = sourcing_name;
            sourcing_name = null;                   /* don't free this one */
            long save_sourcing_lnum = sourcing_lnum;
            sourcing_lnum = 0;                      /* no line number here */

            int save_current_SID = current_SID;

            /* Don't use local function variables, if called from a function. */
            funccall_C save_funccalp = save_funccal();

            boolean did_save_redobuff = false;

            /*
             * When starting to execute autocommands, save the search patterns.
             */
            if (!autocmd_busy)
            {
                save_search_patterns();
                saveRedobuff();
                did_save_redobuff = true;

                did_filetype = keep_filetype;
            }

            /*
             * Note that we are applying autocmds.  Some commands need to know.
             */
            autocmd_busy = true;
            filechangeshell_busy = (event == EVENT_FILECHANGEDSHELL);
            __nesting++;    /* see matching decrement below */

            /* Remember that FileType was triggered.  Used for did_filetype(). */
            if (event == EVENT_FILETYPE)
                did_filetype = true;

            Bytes tail = gettail(fname);

            /* Find first autocommand that matches. */
            patcmd.curpat = first_autopat[event];
            patcmd.nextcmd = null;
            patcmd.group = group;
            patcmd.fname = fname;
            patcmd.sfname = sfname;
            patcmd.tail = tail;
            patcmd.event = event;
            patcmd.arg_bufnr = autocmd_bufnr;
            patcmd.next = null;
            auto_next_pat(patcmd, false);

            /* found one, start executing the autocommands */
            if (patcmd.curpat != null)
            {
                /* add to active_apc_list */
                patcmd.next = active_apc_list;
                active_apc_list = patcmd;

                /* set v:cmdarg (only when there is a matching pattern) */
                long save_cmdbang = get_vim_var_nr(VV_CMDBANG);
                Bytes save_cmdarg = null;
                if (eap != null)
                {
                    save_cmdarg = set_cmdarg(eap, null);
                    set_vim_var_nr(VV_CMDBANG, eap.forceit ? 1 : 0);
                }
                retval = true;
                /* mark the last pattern, to avoid an endless loop
                 * when more patterns are added when executing autocommands */
                AutoPat_C ap;
                for (ap = patcmd.curpat; ap.next != null; ap = ap.next)
                    ap.last = false;
                ap.last = true;
                check_lnums(true);      /* make sure cursor and topline are valid */
                do_cmdline(null, getnextac, patcmd, DOCMD_NOWAIT|DOCMD_VERBOSE|DOCMD_REPEAT);
                if (eap != null)
                {
                    set_cmdarg(null, save_cmdarg);
                    set_vim_var_nr(VV_CMDBANG, save_cmdbang);
                }
                /* delete from active_apc_list */
                if (active_apc_list == patcmd)      /* just in case */
                    active_apc_list = patcmd.next;
            }

            --redrawingDisabled;
            autocmd_busy = save_autocmd_busy;
            filechangeshell_busy = false;
            autocmd_nested = save_autocmd_nested;
            sourcing_name = save_sourcing_name;
            sourcing_lnum = save_sourcing_lnum;
            autocmd_fname = save_autocmd_fname;
            autocmd_fname_full = save_autocmd_fname_full;
            autocmd_bufnr = save_autocmd_bufnr;
            autocmd_match = save_autocmd_match;
            current_SID = save_current_SID;
            restore_funccal(save_funccalp);
            --__nesting;    /* see matching increment above */

            /*
             * When stopping to execute autocommands, restore the search patterns and
             * the redo buffer.  Free any buffers in the au_pending_free_buf list and
             * free any windows in the au_pending_free_win list.
             */
            if (!autocmd_busy)
            {
                restore_search_patterns();
                if (did_save_redobuff)
                    restoreRedobuff();
                did_filetype = false;
                while (au_pending_free_buf != null)
                    au_pending_free_buf = au_pending_free_buf.b_next;
                while (au_pending_free_win != null)
                    au_pending_free_win = au_pending_free_win.w_next;
            }

            /*
             * Some events don't set or reset the Changed flag.
             * Check if still in the same buffer!
             */
            if (curbuf == old_curbuf
                    && (event == EVENT_BUFREADPOST
                        || event == EVENT_BUFWRITEPOST
                        || event == EVENT_FILEAPPENDPOST
                        || event == EVENT_VIMLEAVE
                        || event == EVENT_VIMLEAVEPRE))
            {
                curbuf.b_changed[0] = save_changed;
            }

            au_cleanup();       /* may really delete removed patterns/commands now */
        }

        /* When wiping out a buffer make sure all its buffer-local autocommands are deleted. */
        if (event == EVENT_BUFWIPEOUT && buf != null)
            aubuflocal_remove(buf);

        return retval;
    }

    /*private*/ static Bytes old_termresponse;

    /*
     * Block triggering autocommands until unblock_autocmd() is called.
     * Can be used recursively, so long as it's symmetric.
     */
    /*private*/ static void block_autocmds()
    {
        /* Remember the value of v:termresponse. */
        if (autocmd_blocked == 0)
            old_termresponse = get_vim_var_str(VV_TERMRESPONSE);
        autocmd_blocked++;
    }

    /*private*/ static void unblock_autocmds()
    {
        --autocmd_blocked;

        /* When v:termresponse was set while autocommands were blocked, trigger the autocommands now.
         * Esp. useful when executing a shell command during startup (vimdiff). */
        if (autocmd_blocked == 0 && BNE(get_vim_var_str(VV_TERMRESPONSE), old_termresponse))
            apply_autocmds(EVENT_TERMRESPONSE, null, null, false, curbuf);
    }

    /*private*/ static boolean is_autocmd_blocked()
    {
        return (autocmd_blocked != 0);
    }

    /*
     * Find next autocommand pattern that matches.
     */
    /*private*/ static void auto_next_pat(AutoPatCmd_C apc, boolean stop_at_last)
        /* stop_at_last: stop when 'last' flag is set */
    {
        sourcing_name = null;

        for (AutoPat_C ap = apc.curpat; ap != null && !got_int; ap = ap.next)
        {
            apc.curpat = null;

            /* Only use a pattern when it has not been removed, has commands and the group matches.
             * For buffer-local autocommands only check the buffer number. */
            if (ap.pat != null && ap.cmds != null && (apc.group == AUGROUP_ALL || apc.group == ap.group))
            {
                /* execution-condition */
                boolean b;
                { regprog_C[] __ = { ap.reg_prog }; b = (ap.buflocal_nr == 0 ? match_file_pat(null, __, apc.fname, apc.sfname, apc.tail, ap.allow_dirs) : ap.buflocal_nr == apc.arg_bufnr); ap.reg_prog = __[0]; }
                if (b)
                {
                    Bytes name = event_nr2name(apc.event);
                    Bytes s = u8("%s Auto commands for \"%s\"");
                    sourcing_name = new Bytes(strlen(s) + strlen(name) + ap.patlen + 1);

                    libC.sprintf(sourcing_name, s, name, ap.pat);
                    if (8 <= p_verbose[0])
                    {
                        verbose_enter();
                        smsg(u8("Executing %s"), sourcing_name);
                        verbose_leave();
                    }

                    apc.curpat = ap;
                    apc.nextcmd = ap.cmds;

                    /* mark last command */
                    AutoCmd_C cp;
                    for (cp = ap.cmds; cp.next != null; cp = cp.next)
                        cp.last = false;
                    cp.last = true;
                }
                line_breakcheck();
                if (apc.curpat != null) /* found a match */
                    break;
            }
            if (stop_at_last && ap.last)
                break;
        }
    }

    /*
     * Get next autocommand command.
     * Called by do_cmdline() to get the next line for ":if".
     * Returns allocated string, or null for end of autocommands.
     */
    /*private*/ static final getline_C getnextac = new getline_C()
    {
        public Bytes getline(int _c, Object cookie, int _indent)
        {
            AutoPatCmd_C acp = (AutoPatCmd_C)cookie;

            /* Can be called again after returning the last line. */
            if (acp.curpat == null)
                return null;

            /* repeat until we find an autocommand to execute */
            for ( ; ; )
            {
                /* skip removed commands */
                while (acp.nextcmd != null && acp.nextcmd.cmd == null)
                    if (acp.nextcmd.last)
                        acp.nextcmd = null;
                    else
                        acp.nextcmd = acp.nextcmd.next;

                if (acp.nextcmd != null)
                    break;

                /* at end of commands, find next pattern that matches */
                if (acp.curpat.last)
                    acp.curpat = null;
                else
                    acp.curpat = acp.curpat.next;
                if (acp.curpat != null)
                    auto_next_pat(acp, true);
                if (acp.curpat == null)
                    return null;
            }

            AutoCmd_C ac = acp.nextcmd;

            if (9 <= p_verbose[0])
            {
                verbose_enter_scroll();
                smsg(u8("autocommand %s"), ac.cmd);
                msg_puts(u8("\n"));                     /* don't overwrite this either */
                verbose_leave_scroll();
            }

            Bytes retval = STRDUP(ac.cmd);

            autocmd_nested = ac.nested;
            current_SID = ac.scriptID;
            if (ac.last)
                acp.nextcmd = null;
            else
                acp.nextcmd = ac.next;

            return retval;
        }
    };

    /*
     * Return true if there is a matching autocommand for "fname".
     * To account for buffer-local autocommands, function needs to know
     * in which buffer the file will be opened.
     */
    /*private*/ static boolean has_autocmd(int event, Bytes sfname, buffer_C buf)
    {
        Bytes tail = gettail(sfname);

        Bytes fname = fullName_save(sfname, false);
        if (fname == null)
            return false;

        for (AutoPat_C ap = first_autopat[event]; ap != null; ap = ap.next)
            if (ap.pat != null && ap.cmds != null)
            {
                boolean b;
                { regprog_C[] __ = { ap.reg_prog }; b = (ap.buflocal_nr == 0 ? match_file_pat(null, __, fname, sfname, tail, ap.allow_dirs) : buf != null && ap.buflocal_nr == buf.b_fnum); ap.reg_prog = __[0]; }
                if (b)
                    return true;
            }

        return false;
    }

    /*
     * Function given to expandGeneric() to obtain the list of autocommand group names.
     */
    /*private*/ static final expfun_C get_augroup_name = new expfun_C()
    {
        public Bytes expand(expand_C _xp, int idx)
        {
            if (idx == augroups.ga_len)                     /* add "END" add the end */
                return u8("END");
            if (augroups.ga_len <= idx)                     /* end of list */
                return null;
            if (augroups.ga_data[idx] == null)  /* skip deleted entries */
                return u8("");

            return augroups.ga_data[idx];       /* return a name */
        }
    };

    /*private*/ static boolean include_groups;

    /*private*/ static Bytes set_context_in_autocmd(expand_C xp, Bytes _arg)
    {
        Bytes[] arg = { _arg };
        /* check for a group name, skip it if present */
        include_groups = false;

        Bytes p = arg[0];

        int group = au_get_grouparg(arg);
        if (group == AUGROUP_ERROR)
            return null;

        /* If there only is a group name that's what we expand. */
        if (arg[0].at(0) == NUL && group != AUGROUP_ALL && !vim_iswhite(arg[0].at(-1)))
        {
            arg[0] = p;
            group = AUGROUP_ALL;
        }

        /* skip over event name */
        for (p = arg[0]; p.at(0) != NUL && !vim_iswhite(p.at(0)); p = p.plus(1))
            if (p.at(0) == (byte)',')
                arg[0] = p.plus(1);
        if (p.at(0) == NUL)
        {
            if (group == AUGROUP_ALL)
                include_groups = true;
            xp.xp_context = EXPAND_EVENTS;      /* expand event name */
            xp.xp_pattern = arg[0];
            return null;
        }

        /* skip over pattern */
        p = skipwhite(p);
        while (p.at(0) != NUL && (!vim_iswhite(p.at(0)) || p.at(-1) == (byte)'\\'))
            p = p.plus(1);
        if (p.at(0) != NUL)
            return p;                         /* expand (next) command */

        xp.xp_context = EXPAND_NOTHING;         /* pattern is not expanded */

        return null;
    }

    /*
     * Function given to expandGeneric() to obtain the list of event names.
     */
    /*private*/ static final expfun_C get_event_name = new expfun_C()
    {
        public Bytes expand(expand_C _xp, int idx)
        {
            if (idx < augroups.ga_len)                      /* first list group names, if wanted */
            {
                if (!include_groups || augroups.ga_data[idx] == null)
                    return u8("");                              /* skip deleted entries */

                return augroups.ga_data[idx];   /* return a name */
            }
            return event_names[idx - augroups.ga_len].name;
        }
    };

    /*
     * Return true if autocmd is supported.
     */
    /*private*/ static boolean autocmd_supported(Bytes name)
    {
        Bytes[] p = new Bytes[1];

        return (event_name2nr(name, p) != NUM_EVENTS);
    }

    /*
     * Return true if an autocommand is defined for a group, event and
     * pattern:  The group can be omitted to accept any group. "event" and "pattern"
     * can be null to accept any event and pattern. "pattern" can be null to accept
     * any pattern.  Buffer-local patterns <buffer> or <buffer=N> are accepted.
     * Used for:
     *      exists("#Group") or
     *      exists("#Group#Event") or
     *      exists("#Group#Event#pat") or
     *      exists("#Event") or
     *      exists("#Event#pat")
     */
    /*private*/ static boolean au_exists(Bytes arg)
    {
        boolean retval = false;

        /* Make a copy so that we can change the '#' chars to a NUL. */
        Bytes arg_save = STRDUP(arg);

        Bytes p = vim_strchr(arg_save, '#');
        if (p != null)
            (p = p.plus(1)).be(-1, NUL);

        Bytes event_name;

        /* First, look for an autocmd group name. */
        int group = au_find_group(arg_save);
        if (group == AUGROUP_ERROR)
        {
            /* Didn't match a group name, assume the first argument is an event. */
            group = AUGROUP_ALL;
            event_name = arg_save;
        }
        else
        {
            if (p == null)
            {
                /* "Group": group name is present and it's recognized. */
                return true;
            }

            /* Must be "Group#Event" or "Group#Event#pat". */
            event_name = p;
            p = vim_strchr(event_name, '#');
            if (p != null)
                (p = p.plus(1)).be(-1, NUL);     /* "Group#Event#pat" */
        }

        Bytes pattern = p;     /* "pattern" is null when there is no pattern */

        /* find the index (enum) for the event name */
        int event;
        { Bytes[] __ = { p }; event = event_name2nr(event_name, __); p = __[0]; }

        /* return false if the event name is not recognized */
        if (event == NUM_EVENTS)
            return retval;

        /* Find the first autocommand for this event.
         * If there isn't any, return false;
         * If there is one and no pattern given, return true; */
        AutoPat_C ap = first_autopat[event];
        if (ap == null)
            return retval;

        buffer_C buflocal_buf = null;

        /* if pattern is "<buffer>", special handling is needed which uses curbuf */
        /* for pattern "<buffer=N>, fnamecmp() will work fine */
        if (pattern != null && STRCASECMP(pattern, u8("<buffer>")) == 0)
            buflocal_buf = curbuf;

        /* Check if there is an autocommand with the given pattern. */
        for ( ; ap != null; ap = ap.next)
            /* only use a pattern when it has not been removed and has commands. */
            /* For buffer-local autocommands, fnamecmp() works fine. */
            if (ap.pat != null && ap.cmds != null
                && (group == AUGROUP_ALL || ap.group == group)
                && (pattern == null
                    || (buflocal_buf == null ? STRCMP(ap.pat, pattern) == 0 : ap.buflocal_nr == buflocal_buf.b_fnum)))
            {
                retval = true;
                break;
            }

        return retval;
    }

    /*
     * Try matching a filename with a "pattern" ("prog" is null),
     * or use the precompiled regprog "prog" ("pattern" is null).
     * That avoids calling vim_regcomp() often.
     * Used for autocommands and 'wildignore'.
     * Returns true if there is a match, false otherwise.
     */
    /*private*/ static boolean match_file_pat(Bytes pattern, regprog_C[] prog, Bytes fname, Bytes sfname, Bytes tail, boolean allow_dirs)
        /* pattern: pattern to match with */
        /* prog: pre-compiled regprog or null */
        /* fname: full path of file name */
        /* sfname: short file name or null */
        /* tail: tail of path */
        /* allow_dirs: allow matching with dir */
    {
        boolean result = false;

        regmatch_C regmatch = new regmatch_C();
        regmatch.rm_ic = false;
        if (prog != null)
            regmatch.regprog = prog[0];
        else
            regmatch.regprog = vim_regcomp(pattern, RE_MAGIC);

        /*
         * Try for a match with the pattern with:
         * 1. the full file name, when the pattern has a '/'.
         * 2. the short file name, when the pattern has a '/'.
         * 3. the tail of the file name, when the pattern has no '/'.
         */
        if (regmatch.regprog != null
                 && ((allow_dirs
                         && (vim_regexec(regmatch, fname, 0)
                             || (sfname != null && vim_regexec(regmatch, sfname, 0))))
                     || (!allow_dirs && vim_regexec(regmatch, tail, 0))))
            result = true;

        if (prog != null)
            prog[0] = regmatch.regprog;

        return result;
    }

    /*
     * Convert the given pattern "pat" which has shell style wildcards in it,
     * into a regular expression, and return the result in allocated memory.
     * If there is a directory path separator to be matched, then true is put
     * in "allow_dirs", otherwise false is put there.
     * Handle backslashes before special characters, like "\*" and "\ ".
     *
     * Returns null when out of memory.
     */
    /*private*/ static Bytes file_pat_to_reg_pat(Bytes pat, Bytes pat_end, boolean[] allow_dirs)
        /* pat_end: first char after pattern or null */
        /* allow_dirs: result passed back out in here */
    {
        if (allow_dirs != null)
            allow_dirs[0] = false;
        if (pat_end == null)
            pat_end = pat.plus(strlen(pat));

        int size = 2;               /* '^' at start, '$' at end */

        for (Bytes p = pat; BLT(p, pat_end); p = p.plus(1))
        {
            switch (p.at(0))
            {
                case '*':
                case '.':
                case ',':
                case '{':
                case '}':
                case '~':
                    size += 2;      /* extra backslash */
                    break;

                default:
                    size++;
                    break;
            }
        }

        Bytes reg_pat = new Bytes(size + 1);

        int i = 0;

        if (pat.at(0) == (byte)'*')
        {
            while (pat.at(0) == (byte)'*' && BLT(pat, pat_end.minus(1)))
                pat = pat.plus(1);
        }
        else
            reg_pat.be(i++, (byte)'^');

        boolean add_dollar = true;

        Bytes endp = pat_end.minus(1);
        if (endp.at(0) == (byte)'*')
        {
            while (0 < BDIFF(endp, pat) && endp.at(0) == (byte)'*')
                endp = endp.minus(1);
            add_dollar = false;
        }

        int nested = 0;
        for (Bytes p = pat; p.at(0) != NUL && 0 <= nested && BLE(p, endp); p = p.plus(1))
        {
            switch (p.at(0))
            {
                case '*':
                    reg_pat.be(i++, (byte)'.');
                    reg_pat.be(i++, (byte)'*');
                    while (p.at(1) == (byte)'*')     /* "**" matches like "*" */
                        p = p.plus(1);
                    break;

                case '.':
                case '~':
                    reg_pat.be(i++, (byte)'\\');
                    reg_pat.be(i++, p.at(0));
                    break;

                case '?':
                    reg_pat.be(i++, (byte)'.');
                    break;

                case '\\':
                    if (p.at(1) == NUL)
                        break;
                    /* Undo escaping from expandEscape():
                     * foo\?bar -> foo?bar
                     * foo\%bar -> foo%bar
                     * foo\,bar -> foo,bar
                     * foo\ bar -> foo bar
                     * Don't unescape \, * and others that are also special in a regexp.
                     * An escaped { must be unescaped since we use magic not verymagic.
                     * Use "\\\{n,m\}"" to get "\{n,m}".
                     */
                    if ((p = p.plus(1)).at(0) == (byte)'?')
                        reg_pat.be(i++, (byte)'?');
                    else if (p.at(0) == (byte)',' || p.at(0) == (byte)'%' || p.at(0) == (byte)'#' || p.at(0) == (byte)' ' || p.at(0) == (byte)'{' || p.at(0) == (byte)'}')
                        reg_pat.be(i++, p.at(0));
                    else if (p.at(0) == (byte)'\\' && p.at(1) == (byte)'\\' && p.at(2) == (byte)'{')
                    {
                        reg_pat.be(i++, (byte)'\\');
                        reg_pat.be(i++, (byte)'{');
                        p = p.plus(2);
                    }
                    else
                    {
                        if (allow_dirs != null && vim_ispathsep(p.at(0)))
                            allow_dirs[0] = true;
                        reg_pat.be(i++, (byte)'\\');
                        reg_pat.be(i++, p.at(0));
                    }
                    break;

                case '{':
                    reg_pat.be(i++, (byte)'\\');
                    reg_pat.be(i++, (byte)'(');
                    nested++;
                    break;

                case '}':
                    reg_pat.be(i++, (byte)'\\');
                    reg_pat.be(i++, (byte)')');
                    --nested;
                    break;

                case ',':
                    if (nested != 0)
                    {
                        reg_pat.be(i++, (byte)'\\');
                        reg_pat.be(i++, (byte)'|');
                    }
                    else
                        reg_pat.be(i++, (byte)',');
                    break;

                default:
                    if (allow_dirs != null && vim_ispathsep(p.at(0)))
                        allow_dirs[0] = true;
                    reg_pat.be(i++, p.at(0));
                    break;
            }
        }
        if (add_dollar)
            reg_pat.be(i++, (byte)'$');
        reg_pat.be(i, NUL);
        if (nested != 0)
        {
            if (nested < 0)
                emsg(u8("E219: Missing {."));
            else
                emsg(u8("E220: Missing }."));
            reg_pat = null;
        }
        return reg_pat;
    }

    /*
     * Version of read() that retries when interrupted by EINTR (possibly by a SIGWINCH).
     */
    /*private*/ static int read_eintr(int fd, Bytes buf, int bufsize)
    {
        int ret;

        for ( ; ; )
        {
            ret = (int)libC.read(fd, buf, bufsize);
            if (0 <= ret || libC.errno() != EINTR)
                break;
        }

        return ret;
    }

    /*
     * Version of write() that retries when interrupted by EINTR (possibly by a SIGWINCH).
     */
    /*private*/ static int write_eintr(int fd, Bytes buf, int bufsize)
    {
        int ret = 0;

        /* Repeat the write() so long it didn't fail, other than being interrupted by a signal. */
        while (ret < bufsize)
        {
            int wlen = (int)libC.write(fd, buf.plus(ret), bufsize - ret);
            if (wlen < 0)
            {
                if (libC.errno() != EINTR)
                    break;
            }
            else
                ret += wlen;
        }

        return ret;
    }
}
