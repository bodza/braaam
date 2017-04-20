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
public class VimP
{
    /*
     * search.c: code for normal mode searching commands ----------------------------------------------
     */

    /*
     * This file contains various searching-related routines.  These fall into
     * three groups:
     * 1. string searches (for /, ?, n, and N)
     * 2. character searches within a single line (for f, F, t, T, etc)
     * 3. "other" kinds of searches like the '%' command, and 'word' searches.
     */

    /*
     * String searches
     *
     * The string search functions are divided into two levels:
     * lowest:  searchit(); uses an pos_C for starting position and found match.
     * Highest: do_search(); uses curwin.w_cursor; calls searchit().
     *
     * The last search pattern is remembered for repeating the same search.
     * This pattern is shared between the :g, :s, ? and / commands.
     * This is in search_regcomp().
     *
     * The actual string matching is done using a heavily modified version of
     * Henry Spencer's regular expression library.  See regexp.c.
     */

    /* The offset for a search command is store in a soff struct. */
    /* Note: only spats[0].sp_off is really used. */
    /*private*/ static final class soffset_C
    {
        byte        dir;            /* search direction, '/' or '?' */
        boolean     line;           /* search has line offset */
        boolean     end;            /* search set cursor at end */
        long        off;            /* line or char offset */

        /*private*/ soffset_C()
        {
        }
    }

    /*private*/ static void COPY_soffset(soffset_C so1, soffset_C so0)
    {
        so1.dir = so0.dir;
        so1.line = so0.line;
        so1.end = so0.end;
        so1.off = so0.off;
    }

    /* A search pattern and its attributes are stored in a spat struct. */
    /*private*/ static final class spat_C
    {
        Bytes       pat;        /* the pattern (in allocated memory) or null */
        boolean     magic;      /* magicness of the pattern */
        boolean     no_scs;     /* no smartcase for this pattern */
        soffset_C   sp_off;

        /*private*/ spat_C()
        {
            sp_off = new soffset_C();
        }
    }

    /*private*/ static void COPY_spat(spat_C sp1, spat_C sp0)
    {
        sp1.pat = sp0.pat;
        sp1.magic = sp0.magic;
        sp1.no_scs = sp0.no_scs;
        COPY_soffset(sp1.sp_off, sp0.sp_off);
    }

    /*private*/ static spat_C new_spat()
    {
        spat_C sp = new spat_C();

        sp.magic = true;
        sp.sp_off.dir = '/';

        return sp;
    }

    /*
     * Two search patterns are remembered: one for the :substitute command and one for other searches.
     * last_idx points to the one that was used the last time.
     */
    /*private*/ static spat_C[] spats =/* new spat_C[2]*/
    {
        new_spat(),                     /* last used search pat */
        new_spat()                      /* last used substitute pat */
    };
    /*private*/ static int last_idx;        /* index in spats[] for RE_LAST */

    /*
     * Copy of spats[], for keeping the search patterns while executing autocmds.
     */
    /*private*/ static spat_C[] saved_spats =/* new spat_C[2]*/
    {
        new_spat(),
        new_spat()
    };
    /*private*/ static int saved_last_idx;
    /*private*/ static boolean saved_no_hlsearch;

    /*private*/ static Bytes mr_pattern;   /* pattern used by search_regcomp() */

    /*
     * translate search pattern for vim_regcomp()
     *
     * pat_save == RE_SEARCH: save pat in spats[RE_SEARCH].pat (normal search cmd)
     * pat_save == RE_SUBST: save pat in spats[RE_SUBST].pat (:substitute command)
     * pat_save == RE_BOTH: save pat in both patterns (:global command)
     * pat_use  == RE_SEARCH: use previous search pattern if "pat" is null
     * pat_use  == RE_SUBST: use previous substitute pattern if "pat" is null
     * pat_use  == RE_LAST: use last used pattern if "pat" is null
     * options & SEARCH_HIS: put search string in history
     * options & SEARCH_KEEP: keep previous search pattern
     *
     * returns false if failed, true otherwise.
     */
    /*private*/ static boolean search_regcomp(Bytes pat, int pat_save, int pat_use, int options, regmmatch_C regmatch)
        /* regmatch: return: pattern and ignore-case flag */
    {
        rc_did_emsg = false;
        boolean magic = p_magic[0];

        /*
         * If no pattern given, use a previously defined pattern.
         */
        if (pat == null || pat.at(0) == NUL)
        {
            int i;
            if (pat_use == RE_LAST)
                i = last_idx;
            else
                i = pat_use;
            if (spats[i].pat == null)           /* pattern was never defined */
            {
                if (pat_use == RE_SUBST)
                    emsg(e_nopresub);
                else
                    emsg(e_noprevre);
                rc_did_emsg = true;
                return false;
            }
            pat = spats[i].pat;
            magic = spats[i].magic;
            no_smartcase = spats[i].no_scs;
        }
        else if ((options & SEARCH_HIS) != 0)   /* put new pattern in history */
            add_to_history(HIST_SEARCH, pat, true, NUL);

        if (curwin.w_onebuf_opt.wo_rl[0] && curwin.w_onebuf_opt.wo_rlc[0].at(0) == (byte)'s')
            mr_pattern = reverse_text(pat);
        else
            mr_pattern = pat;

        /*
         * Save the currently used pattern in the appropriate place,
         * unless the pattern should not be remembered.
         */
        if ((options & SEARCH_KEEP) == 0 && !cmdmod.keeppatterns)
        {
            /* search or global command */
            if (pat_save == RE_SEARCH || pat_save == RE_BOTH)
                save_re_pat(RE_SEARCH, pat, magic);
            /* substitute or global command */
            if (pat_save == RE_SUBST || pat_save == RE_BOTH)
                save_re_pat(RE_SUBST, pat, magic);
        }

        regmatch.rmm_ic = ignorecase(pat);
        regmatch.rmm_maxcol = 0;
        regmatch.regprog = vim_regcomp(pat, magic ? RE_MAGIC : 0);

        return (regmatch.regprog != null);
    }

    /*
     * Get search pattern used by search_regcomp().
     */
    /*private*/ static Bytes get_search_pat()
    {
        return mr_pattern;
    }

    /*
     * Reverse text into allocated memory.
     */
    /*private*/ static Bytes reverse_text(Bytes s)
    {
        /*
         * Reverse the pattern.
         */
        int len = strlen(s);
        Bytes rev = new Bytes(len + 1);

        int j = len;
        for (int i = 0; i < len; i++)
        {
            int mb_len = us_ptr2len_cc(s.plus(i));
            j -= mb_len;
            BCOPY(rev, j, s, i, mb_len);
            i += mb_len - 1;
        }
        rev.be(len, NUL);

        return rev;
    }

    /*private*/ static void save_re_pat(int idx, Bytes pat, boolean magic)
    {
        if (BNE(spats[idx].pat, pat))
        {
            spats[idx].pat = STRDUP(pat);
            spats[idx].magic = magic;
            spats[idx].no_scs = no_smartcase;
            last_idx = idx;
            /* If 'hlsearch' set and search 'pat' changed: need redraw. */
            if (p_hls[0])
                redraw_all_later(SOME_VALID);
            no_hlsearch = false;
            set_vim_var_nr(VV_HLSEARCH, (!no_hlsearch && p_hls[0]) ? 1 : 0);
        }
    }

    /*
     * Save the search patterns, so they can be restored later.
     * Used before/after executing autocommands and user functions.
     */
    /*private*/ static int save_level;

    /*private*/ static void save_search_patterns()
    {
        if (save_level++ == 0)
        {
            COPY_spat(saved_spats[0], spats[0]);
            if (spats[0].pat != null)
                saved_spats[0].pat = STRDUP(spats[0].pat);
            COPY_spat(saved_spats[1], spats[1]);
            if (spats[1].pat != null)
                saved_spats[1].pat = STRDUP(spats[1].pat);
            saved_last_idx = last_idx;
            saved_no_hlsearch = no_hlsearch;
        }
    }

    /*private*/ static void restore_search_patterns()
    {
        if (--save_level == 0)
        {
            COPY_spat(spats[0], saved_spats[0]);
            set_vv_searchforward();
            COPY_spat(spats[1], saved_spats[1]);
            last_idx = saved_last_idx;
            no_hlsearch = saved_no_hlsearch;
            set_vim_var_nr(VV_HLSEARCH, (!no_hlsearch && p_hls[0]) ? 1 : 0);
        }
    }

    /*
     * Return true when case should be ignored for search pattern "pat".
     * Uses the 'ignorecase' and 'smartcase' options.
     */
    /*private*/ static boolean ignorecase(Bytes pat)
    {
        boolean ic = p_ic[0];

        if (ic && !no_smartcase && p_scs[0])
            ic = !pat_has_uppercase(pat);
        no_smartcase = false;

        return ic;
    }

    /*
     * Return true if patter "pat" has an uppercase character.
     */
    /*private*/ static boolean pat_has_uppercase(Bytes pat)
    {
        Bytes p = pat;

        while (p.at(0) != NUL)
        {
            int len = us_ptr2len_cc(p);
            if (1 < len)
            {
                if (utf_isupper(us_ptr2char(p)))
                    return true;
                p = p.plus(len);
            }
            else if (p.at(0) == (byte)'\\')
            {
                if (p.at(1) == (byte)'_' && p.at(2) != NUL)         /* skip "\_X" */
                    p = p.plus(3);
                else if (p.at(1) == (byte)'%' && p.at(2) != NUL)    /* skip "\%X" */
                    p = p.plus(3);
                else if (p.at(1) != NUL)                   /* skip "\X" */
                    p = p.plus(2);
                else
                    p = p.plus(1);
            }
            else if (utf_isupper(p.at(0)))
                return true;
            else
                p = p.plus(1);
        }
        return false;
    }

    /*private*/ static Bytes last_search_pat()
    {
        return spats[last_idx].pat;
    }

    /*
     * Reset search direction to forward.  For "gd" and "gD" commands.
     */
    /*private*/ static void reset_search_dir()
    {
        spats[0].sp_off.dir = '/';
        set_vv_searchforward();
    }

    /*
     * Set the last search pattern.  For ":let @/ =" and viminfo.
     * Also set the saved search pattern, so that this works in an autocommand.
     */
    /*private*/ static void set_last_search_pat(Bytes s, int idx, boolean magic, boolean setlast)
    {
        /* An empty string means that nothing should be matched. */
        spats[idx].pat = (s.at(0) == NUL) ? null : STRDUP(s);
        spats[idx].magic = magic;
        spats[idx].no_scs = false;
        spats[idx].sp_off.dir = '/';
        set_vv_searchforward();
        spats[idx].sp_off.line = false;
        spats[idx].sp_off.end = false;
        spats[idx].sp_off.off = 0;

        if (setlast)
            last_idx = idx;

        if (save_level != 0)
        {
            COPY_spat(saved_spats[idx], spats[0]);
            if (spats[idx].pat == null)
                saved_spats[idx].pat = null;
            else
                saved_spats[idx].pat = STRDUP(spats[idx].pat);
            saved_last_idx = last_idx;
        }

        /* If 'hlsearch' set and search pat changed: need redraw. */
        if (p_hls[0] && idx == last_idx && !no_hlsearch)
            redraw_all_later(SOME_VALID);
    }

    /*
     * Get a regexp program for the last used search pattern.
     * This is used for highlighting all matches in a window.
     * Values returned in regmatch.regprog and regmatch.rmm_ic.
     */
    /*private*/ static void last_pat_prog(regmmatch_C regmatch)
    {
        if (spats[last_idx].pat == null)
        {
            regmatch.regprog = null;
            return;
        }
        emsg_off++;         /* So it doesn't beep if bad expr */
        search_regcomp(u8(""), 0, last_idx, SEARCH_KEEP, regmatch);
        --emsg_off;
    }

    /*private*/ static Bytes top_bot_msg = u8("search hit TOP, continuing at BOTTOM");
    /*private*/ static Bytes bot_top_msg = u8("search hit BOTTOM, continuing at TOP");

    /*
     * Lowest level search function.
     * Search for 'count'th occurrence of pattern 'pat' in direction 'dir'.
     * Start at position 'pos' and return the found position in 'pos'.
     *
     * if (options & SEARCH_MSG) == 0 don't give any messages
     * if (options & SEARCH_MSG) == SEARCH_NFMSG don't give 'notfound' messages
     * if (options & SEARCH_MSG) == SEARCH_MSG give all messages
     * if (options & SEARCH_HIS) put search pattern in history
     * if (options & SEARCH_END) return position at end of match
     * if (options & SEARCH_START) accept match at pos itself
     * if (options & SEARCH_KEEP) keep previous search pattern
     * if (options & SEARCH_FOLD) match only once in a closed fold
     * if (options & SEARCH_PEEK) check for typed char, cancel search
     *
     * Return false (zero) for failure, non-zero for success.
     * When FEAT_EVAL is defined, returns the index of the first matching
     * subpattern plus one; one if there was none.
     */
    /*private*/ static int searchit(window_C win, buffer_C buf, pos_C pos, int dir, Bytes pat, long count, int options, int pat_use, long stop_lnum, timeval_C tm)
        /* win: window to search in; can be null for a buffer without a window! */
        /* pat_use: which pattern to use when "pat" is empty */
        /* stop_lnum: stop after this line number when != 0 */
        /* tm: timeout limit or null */
    {
        int submatch = 0;

        boolean first_match = true;
        boolean save_called_emsg = called_emsg;
        boolean break_loop = false;

        regmmatch_C regmatch = new regmmatch_C();
        if (search_regcomp(pat, RE_SEARCH, pat_use, (options & (SEARCH_HIS + SEARCH_KEEP)), regmatch) == false)
        {
            if ((options & SEARCH_MSG) != 0 && !rc_did_emsg)
                emsg2(u8("E383: Invalid search string: %s"), mr_pattern);
            return 0;
        }

        boolean found;
        long lnum;

        pos_C start_pos = new pos_C();
        lpos_C matchpos = new lpos_C();
        lpos_C endpos = new lpos_C();

        /*
         * find the string
         */
        called_emsg = false;
        do  /* loop for count */
        {
            /* When not accepting a match at the start position, set "extra_col" to a non-zero value.
             * Don't do that when starting at MAXCOL, since MAXCOL + 1 is zero. */
            int extra_col;
            if ((options & SEARCH_START) != 0 || pos.col == MAXCOL)
                extra_col = 0;
            /* Watch out for the "col" being MAXCOL - 2, used in a closed fold. */
            else if (dir != BACKWARD
                && 1 <= pos.lnum && pos.lnum <= buf.b_ml.ml_line_count && pos.col < MAXCOL - 2)
            {
                Bytes ptr = ml_get_buf(buf, pos.lnum, false).plus(pos.col);
                if (ptr.at(0) == NUL)
                    extra_col = 1;
                else
                    extra_col = us_ptr2len_cc(ptr);
            }
            else
                extra_col = 1;

            COPY_pos(start_pos, pos);       /* remember start pos for detecting no match */
            found = false;                  /* default: not found */
            boolean at_first_line = true;   /* default: start in first line */
            if (pos.lnum == 0)              /* correct lnum for when starting in line 0 */
            {
                pos.lnum = 1;
                pos.col = 0;
                at_first_line = false;      /* not in first line now */
            }

            /*
             * Start searching in current line, unless searching backwards and we're in column 0.
             * If we are searching backwards, in column 0, and not including the
             * current position, gain some efficiency by skipping back a line.
             * Otherwise begin the search in the current line.
             */
            if (dir == BACKWARD && start_pos.col == 0 && (options & SEARCH_START) == 0)
            {
                lnum = pos.lnum - 1;
                at_first_line = false;
            }
            else
                lnum = pos.lnum;

            for (int loop = 0; loop <= 1; loop++)   /* loop twice if 'wrapscan' set */
            {
                for ( ; 0 < lnum && lnum <= buf.b_ml.ml_line_count; lnum += dir, at_first_line = false)
                {
                    /* Stop after checking "stop_lnum", if it's set. */
                    if (stop_lnum != 0 && (dir == FORWARD ? stop_lnum < lnum : lnum < stop_lnum))
                        break;
                    /* Stop after passing the "tm" time limit. */
                    if (tm != null && profile_passed_limit(tm))
                        break;

                    /*
                     * Look for a match somewhere in line "lnum".
                     */
                    long nmatched = vim_regexec_multi(regmatch, win, buf, lnum, 0, tm);
                    /* Abort searching on an error (e.g., out of stack). */
                    if (called_emsg)
                        break;
                    if (0 < nmatched)
                    {
                        /* match may actually be in another line when using \zs */
                        COPY_lpos(matchpos, regmatch.startpos[0]);
                        COPY_lpos(endpos, regmatch.endpos[0]);
                        submatch = first_submatch(regmatch);
                        /* "lnum" may be past end of buffer for "\n\zs". */
                        Bytes ptr;
                        if (buf.b_ml.ml_line_count < lnum + matchpos.lnum)
                            ptr = u8("");
                        else
                            ptr = ml_get_buf(buf, lnum + matchpos.lnum, false);

                        /*
                         * Forward search in the first line: match should be after
                         * the start position.  If not, continue at the end of the
                         * match (this is vi compatible) or on the next char.
                         */
                        if (dir == FORWARD && at_first_line)
                        {
                            boolean match_ok = true;
                            /*
                             * When the match starts in a next line it's certainly
                             * past the start position.
                             * When match lands on a NUL the cursor will be put
                             * one back afterwards, compare with that position,
                             * otherwise "/$" will get stuck on end of line.
                             */
                            while (matchpos.lnum == 0
                                    && ((options & SEARCH_END) != 0 && first_match
                                        ? (nmatched == 1 && endpos.col - 1 < start_pos.col + extra_col)
                                        : (matchpos.col - (ptr.at(matchpos.col) == NUL ? 1 : 0) < start_pos.col + extra_col)))
                            {
                                /*
                                 * If vi-compatible searching, continue at the end
                                 * of the match, otherwise continue one position forward.
                                 */
                                int matchcol;
                                if (vim_strbyte(p_cpo[0], CPO_SEARCH) != null)
                                {
                                    if (1 < nmatched)
                                    {
                                        /* end is in next line, thus no match in this line */
                                        match_ok = false;
                                        break;
                                    }
                                    matchcol = endpos.col;
                                    /* for empty match: advance one char */
                                    if (matchcol == matchpos.col && ptr.at(matchcol) != NUL)
                                        matchcol += us_ptr2len_cc(ptr.plus(matchcol));
                                }
                                else
                                {
                                    matchcol = matchpos.col;
                                    if (ptr.at(matchcol) != NUL)
                                        matchcol += us_ptr2len_cc(ptr.plus(matchcol));
                                }
                                if (matchcol == 0 && (options & SEARCH_START) != 0)
                                    break;
                                if (ptr.at(matchcol) == NUL
                                        || (nmatched = vim_regexec_multi(regmatch,
                                                  win, buf, lnum + matchpos.lnum, matchcol, tm)) == 0)
                                {
                                    match_ok = false;
                                    break;
                                }
                                COPY_lpos(matchpos, regmatch.startpos[0]);
                                COPY_lpos(endpos, regmatch.endpos[0]);
                                submatch = first_submatch(regmatch);

                                /* Need to get the line pointer again,
                                 * a multi-line search may have made it invalid. */
                                ptr = ml_get_buf(buf, lnum + matchpos.lnum, false);
                            }
                            if (!match_ok)
                                continue;
                        }
                        if (dir == BACKWARD)
                        {
                            /*
                             * Now, if there are multiple matches on this line,
                             * we have to get the last one.  Or the last one before
                             * the cursor, if we're on that line.
                             * When putting the new cursor at the end, compare
                             * relative to the end of the match.
                             */
                            boolean match_ok = false;
                            for ( ; ; )
                            {
                                /* Remember a position that is before the start
                                 * position, we use it if it's the last match in
                                 * the line.  Always accept a position after
                                 * wrapping around. */
                                if (loop != 0
                                    || ((options & SEARCH_END) != 0
                                        ? (lnum + regmatch.endpos[0].lnum < start_pos.lnum
                                            || (lnum + regmatch.endpos[0].lnum == start_pos.lnum
                                                 && regmatch.endpos[0].col - 1 + extra_col <= start_pos.col))
                                        : (lnum + regmatch.startpos[0].lnum < start_pos.lnum
                                            || (lnum + regmatch.startpos[0].lnum == start_pos.lnum
                                                 && regmatch.startpos[0].col + extra_col <= start_pos.col))))
                                {
                                    match_ok = true;
                                    COPY_lpos(matchpos, regmatch.startpos[0]);
                                    COPY_lpos(endpos, regmatch.endpos[0]);
                                    submatch = first_submatch(regmatch);
                                }
                                else
                                    break;

                                /*
                                 * We found a valid match, now check if there is another one after it.
                                 * If vi-compatible searching, continue at the end of the match,
                                 * otherwise continue one position forward.
                                 */
                                int matchcol;
                                if (vim_strbyte(p_cpo[0], CPO_SEARCH) != null)
                                {
                                    if (1 < nmatched)
                                        break;
                                    matchcol = endpos.col;
                                    /* for empty match: advance one char */
                                    if (matchcol == matchpos.col && ptr.at(matchcol) != NUL)
                                        matchcol += us_ptr2len_cc(ptr.plus(matchcol));
                                }
                                else
                                {
                                    /* Stop when the match is in a next line. */
                                    if (0 < matchpos.lnum)
                                        break;
                                    matchcol = matchpos.col;
                                    if (ptr.at(matchcol) != NUL)
                                        matchcol += us_ptr2len_cc(ptr.plus(matchcol));
                                }
                                if (ptr.at(matchcol) == NUL
                                        || (nmatched = vim_regexec_multi(regmatch,
                                                  win, buf, lnum + matchpos.lnum, matchcol, tm)) == 0)
                                    break;

                                /* Need to get the line pointer again,
                                 * a multi-line search may have made it invalid. */
                                ptr = ml_get_buf(buf, lnum + matchpos.lnum, false);
                            }

                            /*
                             * If there is only a match after the cursor, skip this match.
                             */
                            if (!match_ok)
                                continue;
                        }

                        /* With the SEARCH_END option move to the last character of the match.
                         * Don't do it for an empty match, end should be same as start then. */
                        if ((options & SEARCH_END) != 0 && (options & SEARCH_NOOF) == 0
                                && (matchpos.lnum != endpos.lnum || matchpos.col != endpos.col))
                        {
                            /* For a match in the first column,
                             * set the position on the NUL in the previous line. */
                            pos.lnum = lnum + endpos.lnum;
                            pos.col = endpos.col;
                            if (endpos.col == 0)
                            {
                                if (1 < pos.lnum)   /* just in case */
                                {
                                    --pos.lnum;
                                    pos.col = strlen(ml_get_buf(buf, pos.lnum, false));
                                }
                            }
                            else
                            {
                                --pos.col;
                                if (pos.lnum <= buf.b_ml.ml_line_count)
                                {
                                    ptr = ml_get_buf(buf, pos.lnum, false);
                                    pos.col -= us_head_off(ptr, ptr.plus(pos.col));
                                }
                            }
                        }
                        else
                        {
                            pos.lnum = lnum + matchpos.lnum;
                            pos.col = matchpos.col;
                        }
                        pos.coladd = 0;
                        found = true;
                        first_match = false;

                        /* Set variables used for 'incsearch' highlighting. */
                        search_match_lines = endpos.lnum - matchpos.lnum;
                        search_match_endcol = endpos.col;
                        break;
                    }
                    line_breakcheck();      /* stop if ctrl-C typed */
                    if (got_int)
                        break;

                    /* Cancel searching if a character was typed.  Used for 'incsearch'.
                     * Don't check too often, that would slowdown searching too much. */
                    if ((options & SEARCH_PEEK) != 0 && ((lnum - pos.lnum) & 0x3f) == 0 && char_avail())
                    {
                        break_loop = true;
                        break;
                    }

                    if (loop != 0 && lnum == start_pos.lnum)
                        break;              /* if second loop, stop where started */
                }
                at_first_line = false;

                /*
                 * Stop the search if wrapscan isn't set, "stop_lnum" is specified,
                 * after an interrupt, after a match and after looping twice.
                 */
                if (!p_ws[0] || stop_lnum != 0 || got_int || called_emsg || break_loop || found || loop != 0)
                    break;

                /*
                 * If 'wrapscan' is set we continue at the other end of the file.
                 * If 'shortmess' does not contain 's', we give a message.
                 * This message is also remembered in "keep_msg" for when the screen is redrawn.
                 * The "keep_msg" is cleared whenever another message is written.
                 */
                if (dir == BACKWARD)    /* start second loop at the other end */
                    lnum = buf.b_ml.ml_line_count;
                else
                    lnum = 1;
                if (!shortmess(SHM_SEARCH) && (options & SEARCH_MSG) != 0)
                    give_warning((dir == BACKWARD) ? top_bot_msg : bot_top_msg, true);
            }
            if (got_int || called_emsg || break_loop)
                break;
        } while (0 < --count && found);     /* stop after count matches or no match */

        called_emsg |= save_called_emsg;

        if (!found)             /* did not find it */
        {
            if (got_int)
                emsg(e_interr);
            else if ((options & SEARCH_MSG) == SEARCH_MSG)
            {
                if (p_ws[0])
                    emsg2(e_patnotf2, mr_pattern);
                else if (lnum == 0)
                    emsg2(u8("E384: search hit TOP without match for: %s"), mr_pattern);
                else
                    emsg2(u8("E385: search hit BOTTOM without match for: %s"), mr_pattern);
            }
            return 0;
        }

        /* A pattern like "\n\zs" may go past the last line. */
        if (pos.lnum > buf.b_ml.ml_line_count)
        {
            pos.lnum = buf.b_ml.ml_line_count;
            pos.col = strlen(ml_get_buf(buf, pos.lnum, false));
            if (0 < pos.col)
                --pos.col;
        }

        return 1 + submatch;
    }

    /*private*/ static void set_search_direction(byte dirc)
    {
        spats[0].sp_off.dir = dirc;
    }

    /*private*/ static void set_vv_searchforward()
    {
        set_vim_var_nr(VV_SEARCHFORWARD, (spats[0].sp_off.dir == '/') ? 1 : 0);
    }

    /*
     * Return the number of the first subpat that matched.
     */
    /*private*/ static int first_submatch(regmmatch_C rp)
    {
        int submatch;

        for (submatch = 1; ; submatch++)
        {
            if (0 <= rp.startpos[submatch].lnum)
                break;
            if (submatch == 9)
            {
                submatch = 0;
                break;
            }
        }

        return submatch;
    }

    /*
     * Highest level string search function.
     * Search for the 'count'th occurrence of pattern 'pat' in direction 'dirc'
     *                If 'dirc' is 0: use previous dir.
     *    If 'pat' is null or empty : use previous string.
     *    If 'options & SEARCH_REV' : go in reverse of previous dir.
     *    If 'options & SEARCH_ECHO': echo the search command and handle options
     *    If 'options & SEARCH_MSG' : may give error message
     *    If 'options & SEARCH_OPT' : interpret optional flags
     *    If 'options & SEARCH_HIS' : put search pattern in history
     *    If 'options & SEARCH_NOOF': don't add offset to position
     *    If 'options & SEARCH_MARK': set previous context mark
     *    If 'options & SEARCH_KEEP': keep previous search pattern
     *    If 'options & SEARCH_START': accept match at curpos itself
     *    If 'options & SEARCH_PEEK': check for typed char, cancel search
     *
     * Careful: If spats[0].sp_off.line == true and spats[0].sp_off.off == 0,
     * this makes the movement linewise without moving the match position.
     *
     * Return 0 for failure, 1 for found, 2 for found and line offset added.
     */
    /*private*/ static int do_search(oparg_C oap, byte dirc, Bytes pat, long count, int options, timeval_C tm)
        /* oap: can be null */
        /* dirc: '/' or '?' */
        /* tm: timeout limit or null */
    {
        int retval;

        /*
         * A line offset is not remembered, this is vi compatible.
         */
        if (spats[0].sp_off.line && vim_strbyte(p_cpo[0], CPO_LINEOFF) != null)
        {
            spats[0].sp_off.line = false;
            spats[0].sp_off.off = 0;
        }

        /*
         * Save the values for when (options & SEARCH_KEEP) is used.
         * (there is no "if ()" around this because gcc wants them initialized)
         */
        soffset_C old_off = new soffset_C();
        COPY_soffset(old_off, spats[0].sp_off);

        /* position of the last match */
        pos_C pos = new pos_C();
        COPY_pos(pos, curwin.w_cursor); /* start searching at the cursor position */

        /*
         * Find out the direction of the search.
         */
        if (dirc == 0)
            dirc = spats[0].sp_off.dir;
        else
        {
            spats[0].sp_off.dir = dirc;
            set_vv_searchforward();
        }
        if ((options & SEARCH_REV) != 0)
        {
            if (dirc == '/')
                dirc = '?';
            else
                dirc = '/';
        }

        /*
         * Turn 'hlsearch' highlighting back on.
         */
        if (no_hlsearch && (options & SEARCH_KEEP) == 0)
        {
            redraw_all_later(SOME_VALID);
            no_hlsearch = false;
            set_vim_var_nr(VV_HLSEARCH, (!no_hlsearch && p_hls[0]) ? 1 : 0);
        }

        end_do_search:
        {
            Bytes[] strcopy = { null };

            /*
            * Repeat the search when pattern followed by ';', e.g. "/foo/;?bar".
            */
            for ( ; ; )
            {
                Bytes searchstr = pat;                     /* use previous pattern */
                Bytes dircp = null;

                if (pat == null || pat.at(0) == NUL || pat.at(0) == dirc)
                {
                    if (spats[RE_SEARCH].pat == null)       /* no previous pattern */
                    {
                        pat = spats[RE_SUBST].pat;
                        if (pat == null)
                        {
                            emsg(e_noprevre);
                            retval = 0;
                            break end_do_search;
                        }
                        searchstr = pat;
                    }
                    else
                    {
                        /* make search_regcomp() use spats[RE_SEARCH].pat */
                        searchstr = u8("");
                    }
                }

                if (pat != null && pat.at(0) != NUL)             /* look for (new) offset */
                {
                    /*
                     * Find end of regular expression.
                     * If there is a matching '/' or '?', toss it.
                     */
                    Bytes ps = strcopy[0];
                    Bytes p = skip_regexp(pat, dirc, p_magic[0], strcopy);
                    if (BNE(strcopy[0], ps))
                    {
                        /* made a copy of "pat" to change "\?" to "?" */
                        searchcmdlen += strlen(pat) - strlen(strcopy[0]);
                        pat = strcopy[0];
                        searchstr = strcopy[0];
                    }
                    if (p.at(0) == dirc)
                    {
                        dircp = p;                  /* remember where we put the NUL */
                        (p = p.plus(1)).be(-1, NUL);
                    }
                    spats[0].sp_off.line = false;
                    spats[0].sp_off.end = false;
                    spats[0].sp_off.off = 0;
                    /*
                     * Check for a line offset or a character offset.
                     * For get_address (echo off) we don't check for a character offset,
                     * because it is meaningless and the 's' could be a substitute command.
                     */
                    if (p.at(0) == (byte)'+' || p.at(0) == (byte)'-' || asc_isdigit(p.at(0)))
                        spats[0].sp_off.line = true;
                    else if ((options & SEARCH_OPT) != 0 && (p.at(0) == (byte)'e' || p.at(0) == (byte)'s' || p.at(0) == (byte)'b'))
                    {
                        if (p.at(0) == (byte)'e')                                  /* end */
                            spats[0].sp_off.end = (SEARCH_END != 0);
                        p = p.plus(1);
                    }
                    if (asc_isdigit(p.at(0)) || p.at(0) == (byte)'+' || p.at(0) == (byte)'-')      /* got an offset */
                    {
                        if (asc_isdigit(p.at(0)) || asc_isdigit(p.at(1)))   /* 'nr' or '+nr' or '-nr' */
                            spats[0].sp_off.off = libC.atol(p);
                        else if (p.at(0) == (byte)'-')                             /* single '-' */
                            spats[0].sp_off.off = -1;
                        else                                            /* single '+' */
                            spats[0].sp_off.off = 1;
                        p = p.plus(1);
                        while (asc_isdigit(p.at(0)))                         /* skip number */
                            p = p.plus(1);
                    }

                    /* compute length of search command for get_address() */
                    searchcmdlen += BDIFF(p, pat);

                    pat = p;                        /* put "pat" after search command */
                }

                if ((options & SEARCH_ECHO) != 0 && messaging() && !cmd_silent && msg_silent == 0)
                {
                    Bytes p;
                    if (searchstr.at(0) == NUL)
                        p = spats[last_idx].pat;
                    else
                        p = searchstr;

                    Bytes msgbuf = new Bytes(strlen(p) + 40);

                    msgbuf.be(0, dirc);
                    if (utf_iscomposing(us_ptr2char(p)))
                    {
                        /* Use a space to draw the composing char on. */
                        msgbuf.be(1, (byte)' ');
                        STRCPY(msgbuf.plus(2), p);
                    }
                    else
                        STRCPY(msgbuf.plus(1), p);

                    if (spats[0].sp_off.line || spats[0].sp_off.end || spats[0].sp_off.off != 0)
                    {
                        p = msgbuf.plus(strlen(msgbuf));
                        (p = p.plus(1)).be(-1, dirc);
                        if (spats[0].sp_off.end)
                            (p = p.plus(1)).be(-1, (byte)'e');
                        else if (!spats[0].sp_off.line)
                            (p = p.plus(1)).be(-1, (byte)'s');
                        if (0 < spats[0].sp_off.off || spats[0].sp_off.line)
                            (p = p.plus(1)).be(-1, (byte)'+');
                        if (spats[0].sp_off.off != 0 || spats[0].sp_off.line)
                            libC.sprintf(p, u8("%ld"), spats[0].sp_off.off);
                        else
                            p.be(0, NUL);
                    }

                    msg_start();
                    Bytes trunc = msg_strtrunc(msgbuf, false);

                    /* The search pattern could be shown on the right in rightleft mode, but
                     * the 'ruler' and 'showcmd' area use it too, thus it would be blanked out
                     * again very soon.  Show it on the left, but do reverse the text. */
                    if (curwin.w_onebuf_opt.wo_rl[0] && curwin.w_onebuf_opt.wo_rlc[0].at(0) == (byte)'s')
                        trunc = reverse_text(trunc != null ? trunc : msgbuf);
                    if (trunc != null)
                        msg_outtrans(trunc);
                    else
                        msg_outtrans(msgbuf);
                    msg_clr_eos();
                    msg_check();

                    gotocmdline(false);
                    out_flush();
                    msg_nowait = true;              /* don't wait for this message */
                }

                /*
                 * If there is a character offset, subtract it from the current
                 * position, so we don't get stuck at "?pat?e+2" or "/pat/s-2".
                 * Skip this if pos.col is near MAXCOL (closed fold).
                 * This is not done for a line offset, because then we would not be vi compatible.
                 */
                if (!spats[0].sp_off.line && spats[0].sp_off.off != 0 && pos.col < MAXCOL - 2)
                {
                    if (0 < spats[0].sp_off.off)
                    {
                        long c;
                        for (c = spats[0].sp_off.off; c != 0; --c)
                            if (decl(pos) == -1)
                                break;
                        if (c != 0)                 /* at start of buffer */
                        {
                            pos.lnum = 0;           /* allow lnum == 0 here */
                            pos.col = MAXCOL;
                        }
                    }
                    else
                    {
                        long c;
                        for (c = spats[0].sp_off.off; c != 0; c++)
                            if (incl(pos) == -1)
                                break;
                        if (c != 0)                 /* at end of buffer */
                        {
                            pos.lnum = curbuf.b_ml.ml_line_count + 1;
                            pos.col = 0;
                        }
                    }
                }

                int i = searchit(curwin, curbuf, pos, (dirc == '/') ? FORWARD : BACKWARD, searchstr, count,
                            (spats[0].sp_off.end ? SEARCH_REV : 0)
                                + (options & (SEARCH_KEEP + SEARCH_PEEK + SEARCH_HIS + SEARCH_MSG + SEARCH_START
                                    + ((pat != null && pat.at(0) == (byte)';') ? 0 : SEARCH_NOOF))),
                                        RE_LAST, 0, tm);

                if (dircp != null)
                    dircp.be(0, dirc);          /* restore second '/' or '?' for normal_cmd() */
                if (i == 0)
                {
                    retval = 0;
                    break end_do_search;
                }
                if (spats[0].sp_off.end && oap != null)
                    oap.inclusive = true;           /* 'e' includes last character */

                retval = 1;                         /* pattern found */

                /*
                 * Add character and/or line offset
                 */
                if ((options & SEARCH_NOOF) == 0 || (pat != null && pat.at(0) == (byte)';'))
                {
                    if (spats[0].sp_off.line)       /* add the offset to the line number */
                    {
                        long c = pos.lnum + spats[0].sp_off.off;
                        if (c < 1)
                            pos.lnum = 1;
                        else if (curbuf.b_ml.ml_line_count < c)
                            pos.lnum = curbuf.b_ml.ml_line_count;
                        else
                            pos.lnum = c;
                        pos.col = 0;

                        retval = 2;                 /* pattern found, line offset added */
                    }
                    else if (pos.col < MAXCOL - 2) /* just in case */
                    {
                        long c = spats[0].sp_off.off;
                        if (0 < c)                  /* to the right, check for end of file */
                        {
                            while (0 < c--)
                                if (incl(pos) == -1)
                                    break;
                        }
                        else                        /* to the left, check for start of file */
                        {
                            while (c++ < 0)
                                if (decl(pos) == -1)
                                    break;
                        }
                    }
                }

                /*
                 * The search command can be followed by a ';' to do another search.
                 * For example: "/pat/;/foo/+3;?bar"
                 * This is like doing another search command, except:
                 * - The remembered direction '/' or '?' is from the first search.
                 * - When an error happens the cursor isn't moved at all.
                 * Don't do this when called by get_address() (it handles ';' itself).
                 */
                if ((options & SEARCH_OPT) == 0 || pat == null || pat.at(0) != (byte)';')
                    break;

                dirc = (pat = pat.plus(1)).at(0);
                if (dirc != '?' && dirc != '/')
                {
                    retval = 0;
                    emsg(u8("E386: Expected '?' or '/'  after ';'"));
                    break end_do_search;
                }
                pat = pat.plus(1);
            }

            if ((options & SEARCH_MARK) != 0)
                setpcmark();
            COPY_pos(curwin.w_cursor, pos);
            curwin.w_set_curswant = true;
        }

        if ((options & SEARCH_KEEP) != 0 || cmdmod.keeppatterns)
            COPY_soffset(spats[0].sp_off, old_off);

        return retval;
    }

    /*
     * Character Searches
     */

    /*private*/ static int sc__lastc = NUL;         /* last character searched for */
    /*private*/ static int sc__lastcdir;            /* last direction of character search */
    /*private*/ static boolean sc__last_t_cmd;      /* last search t_cmd */

    /*private*/ static Bytes sc__bytes = new Bytes(MB_MAXBYTES + 1);
    /*private*/ static int sc__bytelen = 1;         /* >1 for multi-byte char */

    /*
     * Search for a character in a line.  If "t_cmd" is false, move to the
     * position of the character, otherwise move to just before the char.
     * Do this "cap.count1" times.
     * Return false or true.
     */
    /*private*/ static boolean searchc(cmdarg_C cap, boolean t_cmd)
    {
        int c = cap.nchar[0];                      /* char to search for */
        int dir = cap.arg;                      /* true for searching forward */
        long count = cap.count1;                /* repeat count */

        boolean stop = true;

        if (c != NUL)                           /* normal search: remember args for repeat */
        {
            if (!keyStuffed)                    /* don't remember when redoing */
            {
                sc__lastc = c;
                sc__lastcdir = dir;
                sc__last_t_cmd = t_cmd;
                sc__bytelen = utf_char2bytes(c, sc__bytes);
                if (cap.ncharC1 != 0)
                {
                    sc__bytelen += utf_char2bytes(cap.ncharC1, sc__bytes.plus(sc__bytelen));
                    if (cap.ncharC2 != 0)
                        sc__bytelen += utf_char2bytes(cap.ncharC2, sc__bytes.plus(sc__bytelen));
                }
            }
        }
        else                                    /* repeat previous search */
        {
            if (sc__lastc == NUL)
                return false;
            if (dir != 0)                            /* repeat in opposite direction */
                dir = -sc__lastcdir;
            else
                dir = sc__lastcdir;
            t_cmd = sc__last_t_cmd;
            c = sc__lastc;

            /* For multi-byte re-use last sc__bytes[] and sc__bytelen. */

            /* Force a move of at least one char, so ";" and "," will move the cursor,
             * even if the cursor is right in front of char we are looking at. */
            if (vim_strbyte(p_cpo[0], CPO_SCOLON) == null && count == 1 && t_cmd)
                stop = false;
        }

        if (dir == BACKWARD)
            cap.oap.inclusive = false;
        else
            cap.oap.inclusive = true;

        Bytes p = ml_get_curline();
        int col = curwin.w_cursor.col;
        int len = strlen(p);

        while (0 < count--)
        {
            for ( ; ; )
            {
                if (0 < dir)
                {
                    col += us_ptr2len_cc(p.plus(col));
                    if (len <= col)
                        return false;
                }
                else
                {
                    if (col == 0)
                        return false;
                    col -= us_head_off(p, p.plus(col - 1)) + 1;
                }
                if (sc__bytelen == 1)
                {
                    if (p.at(col) == c && stop)
                        break;
                }
                else
                {
                    if (MEMCMP(p.plus(col), sc__bytes, sc__bytelen) == 0 && stop)
                        break;
                }
                stop = true;
            }
        }

        if (t_cmd)
        {
            /* backup to before the character (possibly double-byte) */
            col -= dir;

            if (dir < 0)
                /* Landed on the search char which is sc__bytelen long. */
                col += sc__bytelen - 1;
            else
                /* To previous char, which may be multi-byte. */
                col -= us_head_off(p, p.plus(col));
        }
        curwin.w_cursor.col = col;

        return true;
    }

    /*
     * "Other" Searches
     */

    /*
     * findmatch - find the matching paren or brace
     *
     * Improvement over vi: Braces inside quotes are ignored.
     */
    /*private*/ static pos_C findmatch(oparg_C oap, int initc)
    {
        return findmatchlimit(oap, initc, 0, 0);
    }

    /*
     * Return true if the character before "linep[col]" equals "ch".
     * Return false if "col" is zero.
     * Update "*prevcol" to the column of the previous character, unless "prevcol" is null.
     * Handles multibyte string correctly.
     */
    /*private*/ static boolean check_prevcol(Bytes linep, int col, int ch, int[] prevcol)
    {
        --col;
        if (0 < col)
            col -= us_head_off(linep, linep.plus(col));
        if (prevcol != null)
            prevcol[0] = col;
        return (0 <= col && linep.at(col) == ch);
    }

    /*private*/ static pos_C _2_pos = new pos_C(); /* current search position */

    /*
     * findmatchlimit -- find the matching paren or brace, if it exists within
     * maxtravel lines of here.  A maxtravel of 0 means search until falling off
     * the edge of the file.
     *
     * "initc" is the character to find a match for.  NUL means to find the
     * character at or after the cursor.
     *
     * flags: FM_BACKWARD   search backwards (when initc is '/', '*' or '#')
     *        FM_FORWARD    search forwards (when initc is '/', '*' or '#')
     *        FM_BLOCKSTOP  stop at start/end of block ({ or } in column 0)
     *        FM_SKIPCOMM   skip comments (not implemented yet!)
     *
     * "oap" is only used to set oap.motion_type for a linewise motion, it be null
     */
    /*private*/ static pos_C findmatchlimit(oparg_C oap, int _initc, int flags, int maxtravel)
    {
        int[] initc = { _initc };
        int[] findc = { 0 };                      /* matching brace */
        int count = 0;                      /* cumulative number of braces */
        boolean[] backwards = { false };
        boolean inquote = false;            /* true when inside quotes */
        int hash_dir = 0;                   /* Direction searched for # things */
        int comment_dir = 0;                /* Direction searched for comments */
        int traveled = 0;                   /* how far we've searched so far */
        boolean ignore_cend = false;        /* ignore comment end */
        int match_escaped = 0;              /* search for escaped match */
        int comment_col = MAXCOL;           /* start of / / comment */
        boolean lispcomm = false;           /* inside of Lisp-style comment */
        boolean lisp = curbuf.b_p_lisp[0];         /* engage Lisp-specific hacks ;) */

        COPY_pos(_2_pos, curwin.w_cursor);
        _2_pos.coladd = 0;
        Bytes linep = ml_get(_2_pos.lnum); /* pointer to current line */

        boolean cpo_match = (vim_strbyte(p_cpo[0], CPO_MATCH) != null);    /* vi compatible matching */
        boolean cpo_bsl = (vim_strbyte(p_cpo[0], CPO_MATCHBSL) != null);   /* don't recognize backslashes */

        /* Direction to search when initc is '/', '*' or '#'. */
        int dir;
        if ((flags & FM_BACKWARD) != 0)
            dir = BACKWARD;
        else if ((flags & FM_FORWARD) != 0)
            dir = FORWARD;
        else
            dir = 0;

        /*
         * if initc given, look in the table for the matching character
         * '/' and '*' are special cases: look for start or end of comment.
         * When '/' is used, we ignore running backwards into an star-slash,
         * for "[*" command, we just want to find any comment.
         */
        if (initc[0] == '/' || initc[0] == '*')
        {
            comment_dir = dir;
            if (initc[0] == '/')
                ignore_cend = true;
            backwards[0] = (dir != FORWARD);
            initc[0] = NUL;
        }
        else if (initc[0] != '#' && initc[0] != NUL)
        {
            find_mps_values(initc, findc, backwards, true);
            if (findc[0] == NUL)
                return null;
        }
        /*
         * Either initc is '#', or no initc was given and we need to look under the cursor.
         */
        else
        {
            if (initc[0] == '#')
            {
                hash_dir = dir;
            }
            else
            {
                /*
                 * initc was not given, must look for something to match under or near the cursor.
                 * Only check for special things when 'cpo' doesn't have '%'.
                 */
                if (!cpo_match)
                {
                    /* Are we before or at #if, #else etc.? */
                    Bytes p = skipwhite(linep);
                    if (p.at(0) == (byte)'#' && _2_pos.col <= BDIFF(p, linep))
                    {
                        p = skipwhite(p.plus(1));
                        if (STRNCMP(p, u8("if"), 2) == 0 || STRNCMP(p, u8("endif"), 5) == 0 || STRNCMP(p, u8("el"), 2) == 0)
                            hash_dir = 1;
                    }

                    /* Are we on a comment? */
                    else if (linep.at(_2_pos.col) == (byte)'/')
                    {
                        if (linep.at(_2_pos.col + 1) == (byte)'*')
                        {
                            comment_dir = FORWARD;
                            backwards[0] = false;
                            _2_pos.col++;
                        }
                        else if (0 < _2_pos.col && linep.at(_2_pos.col - 1) == (byte)'*')
                        {
                            comment_dir = BACKWARD;
                            backwards[0] = true;
                            _2_pos.col--;
                        }
                    }
                    else if (linep.at(_2_pos.col) == (byte)'*')
                    {
                        if (linep.at(_2_pos.col + 1) == (byte)'/')
                        {
                            comment_dir = BACKWARD;
                            backwards[0] = true;
                        }
                        else if (0 < _2_pos.col && linep.at(_2_pos.col - 1) == (byte)'/')
                        {
                            comment_dir = FORWARD;
                            backwards[0] = false;
                        }
                    }
                }

                /*
                 * If we are not on a comment or the # at the start of a line,
                 * then look for brace anywhere on this line after the cursor.
                 */
                if (hash_dir == 0 && comment_dir == 0)
                {
                    /*
                     * Find the brace under or after the cursor.
                     * If beyond the end of the line, use the last character in the line.
                     */
                    if (linep.at(_2_pos.col) == NUL && _2_pos.col != 0)
                        --_2_pos.col;
                    for ( ; ; )
                    {
                        initc[0] = us_ptr2char(linep.plus(_2_pos.col));
                        if (initc[0] == NUL)
                            break;

                        find_mps_values(initc, findc, backwards, false);
                        if (findc[0] != NUL)
                            break;
                        _2_pos.col += us_ptr2len_cc(linep.plus(_2_pos.col));
                    }
                    if (findc[0] == NUL)
                    {
                        /* no brace in the line, maybe use "  #if" then */
                        if (!cpo_match && skipwhite(linep).at(0) == (byte)'#')
                            hash_dir = 1;
                        else
                            return null;
                    }
                    else if (!cpo_bsl)
                    {
                        int bslcnt = 0;

                        /* Set "match_escaped" if there are an odd number of backslashes. */
                        for (int[] col = { _2_pos.col }; check_prevcol(linep, col[0], '\\', col); )
                            bslcnt++;
                        match_escaped = (bslcnt & 1);
                    }
                }
            }

            if (hash_dir != 0)
            {
                /*
                 * Look for matching #if, #else, #elif, or #endif
                 */
                if (oap != null)
                    oap.motion_type = MLINE;    /* linewise for this case only */
                if (initc[0] != '#')
                {
                    Bytes p = skipwhite(skipwhite(linep).plus(1));
                    if (STRNCMP(p, u8("if"), 2) == 0 || STRNCMP(p, u8("el"), 2) == 0)
                        hash_dir = 1;
                    else if (STRNCMP(p, u8("endif"), 5) == 0)
                        hash_dir = -1;
                    else
                        return null;
                }
                _2_pos.col = 0;
                while (!got_int)
                {
                    if (0 < hash_dir)
                    {
                        if (_2_pos.lnum == curbuf.b_ml.ml_line_count)
                            break;
                    }
                    else if (_2_pos.lnum == 1)
                        break;
                    _2_pos.lnum += hash_dir;
                    linep = ml_get(_2_pos.lnum);
                    line_breakcheck();          /* check for CTRL-C typed */
                    Bytes p = skipwhite(linep);
                    if (p.at(0) != (byte)'#')
                        continue;
                    _2_pos.col = BDIFF(p, linep);
                    p = skipwhite(p.plus(1));
                    if (0 < hash_dir)
                    {
                        if (STRNCMP(p, u8("if"), 2) == 0)
                            count++;
                        else if (STRNCMP(p, u8("el"), 2) == 0)
                        {
                            if (count == 0)
                                return _2_pos;
                        }
                        else if (STRNCMP(p, u8("endif"), 5) == 0)
                        {
                            if (count == 0)
                                return _2_pos;
                            count--;
                        }
                    }
                    else
                    {
                        if (STRNCMP(p, u8("if"), 2) == 0)
                        {
                            if (count == 0)
                                return _2_pos;
                            count--;
                        }
                        else if (initc[0] == '#' && STRNCMP(p, u8("el"), 2) == 0)
                        {
                            if (count == 0)
                                return _2_pos;
                        }
                        else if (STRNCMP(p, u8("endif"), 5) == 0)
                            count++;
                    }
                }
                return null;
            }
        }

        /* This is just guessing: when 'rightleft' is set,
         * search for a matching paren/brace in the other direction. */
        if (curwin.w_onebuf_opt.wo_rl[0] && vim_strchr(u8("()[]{}<>"), initc[0]) != null)
            backwards[0] = !backwards[0];

        int do_quotes = -1;                 /* check for quotes in current line */
        /*MAYBEAN*/int start_in_quotes = MAYBE;    /* start position is in quotes */
        pos_C match_pos = new pos_C();      /* where last slash-star was found */

        /* backward search: Check if this line contains a single-line comment */
        if ((backwards[0] && comment_dir != 0) || lisp)
            comment_col = check_linecomment(linep);
        if (lisp && comment_col != MAXCOL && comment_col < _2_pos.col)
            lispcomm = true;                /* find match inside this comment */
        while (!got_int)
        {
            /*
             * Go to the next position, forward or backward.
             * We could use inc() and dec() here, but that is much slower.
             */
            if (backwards[0])
            {
                /* char to match is inside of comment, don't search outside */
                if (lispcomm && _2_pos.col < comment_col)
                    break;
                if (_2_pos.col == 0)                    /* at start of line, go to prev. one */
                {
                    if (_2_pos.lnum == 1)               /* start of file */
                        break;
                    --_2_pos.lnum;

                    if (0 < maxtravel && maxtravel < ++traveled)
                        break;

                    linep = ml_get(_2_pos.lnum);
                    _2_pos.col = strlen(linep);    /* _2_pos.col on trailing NUL */
                    do_quotes = -1;
                    line_breakcheck();

                    /* Check if this line contains a single-line comment. */
                    if (comment_dir != 0 || lisp)
                        comment_col = check_linecomment(linep);
                    /* skip comment */
                    if (lisp && comment_col != MAXCOL)
                        _2_pos.col = comment_col;
                }
                else
                {
                    --_2_pos.col;
                    _2_pos.col -= us_head_off(linep, linep.plus(_2_pos.col));
                }
            }
            else                            /* forward search */
            {
                if (linep.at(_2_pos.col) == NUL
                        /* at end of line, go to next one, don't search for match in comment */
                        || (lisp && comment_col != MAXCOL && _2_pos.col == comment_col))
                {
                    if (_2_pos.lnum == curbuf.b_ml.ml_line_count    /* end of file */
                            /* line is exhausted and comment with it, don't search for match in code */
                            || lispcomm)
                        break;
                    _2_pos.lnum++;

                    if (maxtravel != 0 && maxtravel < traveled++)
                        break;

                    linep = ml_get(_2_pos.lnum);
                    _2_pos.col = 0;
                    do_quotes = -1;
                    line_breakcheck();
                    if (lisp)   /* find comment pos in new line */
                        comment_col = check_linecomment(linep);
                }
                else
                    _2_pos.col += us_ptr2len_cc(linep.plus(_2_pos.col));
            }

            /*
             * If FM_BLOCKSTOP given, stop at a '{' or '}' in column 0.
             */
            if (_2_pos.col == 0 && (flags & FM_BLOCKSTOP) != 0 && (linep.at(0) == (byte)'{' || linep.at(0) == (byte)'}'))
            {
                if (linep.at(0) == findc[0] && count == 0)        /* match! */
                    return _2_pos;
                break;                                      /* out of scope */
            }

            if (comment_dir != 0)
            {
                /* Note: comments do not nest, and we ignore quotes in them. */
                /* TODO: ignore comment brackets inside strings. */
                if (comment_dir == FORWARD)
                {
                    if (linep.at(_2_pos.col) == (byte)'*' && linep.at(_2_pos.col + 1) == (byte)'/')
                    {
                        _2_pos.col++;
                        return _2_pos;
                    }
                }
                else    /* searching backwards */
                {
                    /*
                     * A comment may contain / * or / /, it may also start or end
                     * with / * /.  Ignore a / * after / /.
                     */
                    if (_2_pos.col == 0)
                        continue;
                    else if (linep.at(_2_pos.col - 1) == (byte)'/' && linep.at(_2_pos.col) == (byte)'*' && _2_pos.col < comment_col)
                    {
                        count++;
                        COPY_pos(match_pos, _2_pos);
                        match_pos.col--;
                    }
                    else if (linep.at(_2_pos.col - 1) == (byte)'*' && linep.at(_2_pos.col) == (byte)'/')
                    {
                        if (0 < count)
                            COPY_pos(_2_pos, match_pos);
                        else if (1 < _2_pos.col && linep.at(_2_pos.col - 2) == (byte)'/' && _2_pos.col <= comment_col)
                            _2_pos.col -= 2;
                        else if (ignore_cend)
                            continue;
                        else
                            return null;

                        return _2_pos;
                    }
                }
                continue;
            }

            /*
             * If smart matching ('cpoptions' does not contain '%'), braces inside
             * of quotes are ignored, but only if there is an even number of
             * quotes in the line.
             */
            if (cpo_match)
                do_quotes = 0;
            else if (do_quotes == -1)
            {
                /*
                 * Count the number of quotes in the line, skipping \" and '"'.
                 * Watch out for "\\".
                 */
                int at_start = do_quotes;       /* do_quotes value at start position */
                Bytes p;
                for (p = linep; p.at(0) != NUL; p = p.plus(1))
                {
                    if (BEQ(p, linep.plus(_2_pos.col + (backwards[0] ? 1 : 0))))
                        at_start = (do_quotes & 1);
                    if (p.at(0) == (byte)'"' && (BEQ(p, linep) || p.at(-1) != (byte)'\'' || p.at(1) != (byte)'\''))
                        do_quotes++;
                    if (p.at(0) == (byte)'\\' && p.at(1) != NUL)
                        p = p.plus(1);
                }
                do_quotes &= 1;                 /* result is 1 with even number of quotes */

                /*
                 * If we find an uneven count, check current line and previous one for a '\' at the end.
                 */
                if (do_quotes == 0)
                {
                    inquote = false;
                    if (p.at(-1) == (byte)'\\')
                    {
                        do_quotes = 1;
                        if (start_in_quotes == MAYBE)
                        {
                            /* Do we need to use at_start here? */
                            inquote = true;
                            start_in_quotes = TRUE;
                        }
                        else if (backwards[0])
                            inquote = true;
                    }
                    if (1 < _2_pos.lnum)
                    {
                        p = ml_get(_2_pos.lnum - 1);
                        if (p.at(0) != NUL && p.at(strlen(p) - 1) == '\\')
                        {
                            do_quotes = 1;
                            if (start_in_quotes == MAYBE)
                            {
                                inquote = (at_start != 0);
                                if (inquote)
                                    start_in_quotes = TRUE;
                            }
                            else if (!backwards[0])
                                inquote = true;
                        }

                        /* ml_get() only keeps one line, need to get "linep" again */
                        linep = ml_get(_2_pos.lnum);
                    }
                }
            }
            if (start_in_quotes == MAYBE)
                start_in_quotes = FALSE;

            /*
             * If 'smartmatch' is set:
             *   Things inside quotes are ignored by setting 'inquote'.
             *   If we find a quote without a preceding '\' invert 'inquote'.
             *   At the end of a line not ending in '\' we reset 'inquote'.
             *
             *   In lines with an uneven number of quotes (without preceding '\')
             *   we do not know which part to ignore.  Therefore we only set
             *   inquote if the number of quotes in a line is even, unless this
             *   line or the previous one ends in a '\'.  Complicated, isn't it?
             */
            int c = us_ptr2char(linep.plus(_2_pos.col));
            switch (c)
            {
                case NUL:
                    /* at end of line without trailing backslash, reset inquote */
                    if (_2_pos.col == 0 || linep.at(_2_pos.col - 1) != (byte)'\\')
                    {
                        inquote = false;
                        start_in_quotes = FALSE;
                    }
                    break;

                case '"':
                    /* a quote that is preceded with an odd number of backslashes is ignored */
                    if (do_quotes != 0)
                    {
                        int col;

                        for (col = _2_pos.col - 1; 0 <= col; --col)
                            if (linep.at(col) != (byte)'\\')
                                break;
                        if (((_2_pos.col - 1 - col) & 1) == 0)
                        {
                            inquote = !inquote;
                            start_in_quotes = FALSE;
                        }
                    }
                    break;

                /*
                 * If smart matching ('cpoptions' does not contain '%'):
                 *   Skip things in single quotes: 'x' or '\x'.  Be careful for single
                 *   single quotes, e.g. jon's.  Things like '\233' or '\x3f' are not
                 *   skipped, there is never a brace in them.
                 *   Ignore this when finding matches for `'.
                 */
                case '\'':
                    if (!cpo_match && initc[0] != '\'' && findc[0] != '\'')
                    {
                        if (backwards[0])
                        {
                            if (1 < _2_pos.col)
                            {
                                if (linep.at(_2_pos.col - 2) == (byte)'\'')
                                {
                                    _2_pos.col -= 2;
                                    break;
                                }
                                else if (linep.at(_2_pos.col - 2) == (byte)'\\' && 2 < _2_pos.col && linep.at(_2_pos.col - 3) == (byte)'\'')
                                {
                                    _2_pos.col -= 3;
                                    break;
                                }
                            }
                        }
                        else if (linep.at(_2_pos.col + 1) != NUL) /* forward search */
                        {
                            if (linep.at(_2_pos.col + 1) == (byte)'\\' && linep.at(_2_pos.col + 2) != NUL && linep.at(_2_pos.col + 3) == (byte)'\'')
                            {
                                _2_pos.col += 3;
                                break;
                            }
                            else if (linep.at(_2_pos.col + 2) == (byte)'\'')
                            {
                                _2_pos.col += 2;
                                break;
                            }
                        }
                    }
                    /* FALLTHROUGH */

                default:
                    /*
                     * For Lisp skip over backslashed (), {} and [].
                     * (actually, we skip #\( et al)
                     */
                    if (curbuf.b_p_lisp[0]
                            && vim_strchr(u8("(){}[]"), c) != null
                            && 1 < _2_pos.col
                            && check_prevcol(linep, _2_pos.col, '\\', null)
                            && check_prevcol(linep, _2_pos.col - 1, '#', null))
                        break;

                    /* Check for match outside of quotes, and inside of
                     * quotes when the start is also inside of quotes. */
                    if ((!inquote || start_in_quotes == TRUE) && (c == initc[0] || c == findc[0]))
                    {
                        int bslcnt = 0;

                        if (!cpo_bsl)
                        {
                            for (int[] col = { _2_pos.col }; check_prevcol(linep, col[0], '\\', col); )
                                bslcnt++;
                        }
                        /* Only accept a match when 'M' is in 'cpo'
                         * or when escaping is what we expect. */
                        if (cpo_bsl || (bslcnt & 1) == match_escaped)
                        {
                            if (c == initc[0])
                                count++;
                            else
                            {
                                if (count == 0)
                                    return _2_pos;
                                count--;
                            }
                        }
                    }
                    break;
            }
        }

        if (comment_dir == BACKWARD && 0 < count)
        {
            COPY_pos(_2_pos, match_pos);
            return _2_pos;
        }

        return null; /* never found it */
    }

    /*
     * Check if line[] contains a / / comment.
     * Return MAXCOL if not, otherwise return the column.
     * TODO: skip strings.
     */
    /*private*/ static int check_linecomment(Bytes line)
    {
        Bytes p = line;

        /* skip Lispish one-line comments */
        if (curbuf.b_p_lisp[0])
        {
            if (vim_strchr(p, ';') != null)                     /* there may be comments */
            {
                boolean in_str = false;                         /* inside of string */

                p = line;                                       /* scan from start */
                while ((p = STRPBRK(p, u8("\";"))) != null)
                {
                    if (p.at(0) == (byte)'"')
                    {
                        if (in_str)
                        {
                            if (p.at(-1) != (byte)'\\')               /* skip escaped quote */
                                in_str = false;
                        }
                        else if (BEQ(p, line) || (2 <= BDIFF(p, line)
                                                                /* skip #\" form */
                                          && p.at(-1) != (byte)'\\' && p.at(-2) != (byte)'#'))
                            in_str = true;
                    }
                    else if (!in_str && (BDIFF(p, line) < 2 || (p.at(-1) != (byte)'\\' && p.at(-2) != (byte)'#')))
                        break;                                  /* found! */
                    p = p.plus(1);
                }
            }
            else
                p = null;
        }
        else
            while ((p = vim_strchr(p, '/')) != null)
            {
                /* Accept a double /, unless it's preceded with * and followed by *,
                 * because * / / * is an end and start of a C comment. */
                if (p.at(1) == (byte)'/' && (BEQ(p, line) || p.at(-1) != (byte)'*' || p.at(2) != (byte)'*'))
                    break;
                p = p.plus(1);
            }

        if (p == null)
            return MAXCOL;

        return BDIFF(p, line);
    }

    /*
     * Move cursor briefly to character matching the one under the cursor.
     * Used for Insert mode and "r" command.
     * Show the match only if it is visible on the screen.
     * If there isn't a match, then beep.
     */
    /*private*/ static void showmatch(int c)
        /* c: char to show match for */
    {
        /*
         * Only show match for chars in the 'matchpairs' option.
         */
        /* 'matchpairs' is "x:y,x:y" */
        for (Bytes p = curbuf.b_p_mps[0]; p.at(0) != NUL; p = p.plus(1))
        {
            if (us_ptr2char(p) == c && (curwin.w_onebuf_opt.wo_rl[0] ^ p_ri[0]))
                break;
            p = p.plus(us_ptr2len_cc(p) + 1);
            if (us_ptr2char(p) == c && !(curwin.w_onebuf_opt.wo_rl[0] ^ p_ri[0]))
                break;
            p = p.plus(us_ptr2len_cc(p));
            if (p.at(0) == NUL)
                return;
        }

        pos_C lpos = findmatch(null, NUL);
        if (lpos == null)                   /* no match, so beep */
            vim_beep();
        else if (curwin.w_topline <= lpos.lnum && lpos.lnum < curwin.w_botline)
        {
            int[] vcol = new int[1];
            if (!curwin.w_onebuf_opt.wo_wrap[0])
                getvcol(curwin, lpos, null, vcol, null);
            if (curwin.w_onebuf_opt.wo_wrap[0]
                || (curwin.w_leftcol <= vcol[0] && vcol[0] < curwin.w_leftcol + curwin.w_width))
            {
                pos_C save_cursor = new pos_C();
                pos_C mpos = new pos_C();

                COPY_pos(mpos, lpos);       /* save the pos, update_screen() may change it */
                COPY_pos(save_cursor, curwin.w_cursor);
                long save_so = p_so[0];
                long save_siso = p_siso[0];
                /* Handle "$" in 'cpo': If the ')' is typed on top of the "$", stop displaying the "$". */
                if (0 <= dollar_vcol && dollar_vcol == curwin.w_virtcol)
                    dollar_vcol = -1;
                curwin.w_virtcol++;         /* do display ')' just before "$" */
                update_screen(VALID);       /* show the new char first */

                int save_dollar_vcol = dollar_vcol;
                int save_state = State;
                State = SHOWMATCH;
                ui_cursor_shape();          /* may show different cursor shape */
                COPY_pos(curwin.w_cursor, mpos); /* move to matching char */
                p_so[0] = 0;                   /* don't use 'scrolloff' here */
                p_siso[0] = 0;                 /* don't use 'sidescrolloff' here */
                showruler(false);
                setcursor();
                cursor_on();                /* make sure that the cursor is shown */
                out_flush();
                /* Restore dollar_vcol(), because setcursor() may call curs_rows() which resets it
                 * if the matching position is in a previous line and has a higher column number. */
                dollar_vcol = save_dollar_vcol;

                /*
                 * brief pause, unless 'm' is present in 'cpo' and a character is available.
                 */
                if (vim_strbyte(p_cpo[0], CPO_SHOWMATCH) != null)
                    ui_delay(p_mat[0] * 100L, true);
                else if (!char_avail())
                    ui_delay(p_mat[0] * 100L, false);
                COPY_pos(curwin.w_cursor, save_cursor); /* restore cursor position */
                p_so[0] = save_so;
                p_siso[0] = save_siso;
                State = save_state;
                ui_cursor_shape();          /* may show different cursor shape */
            }
        }
    }

    /*
     * findsent(dir, count) - Find the start of the next sentence in direction "dir".
     * Sentences are supposed to end in ".", "!" or "?" followed by white space or a line break.
     * Also stop at an empty line.
     * Return true if the next sentence was found.
     */
    /*private*/ static boolean findsent(int dir, long count)
    {
        boolean noskip = false;     /* do not skip blanks */

        pos_C pos = new pos_C();
        COPY_pos(pos, curwin.w_cursor);

        while (0 < count--)
        {
            found:
            {
                /*
                 * If on an empty line, skip upto a non-empty line.
                 */
                if (gchar_pos(pos) == NUL)
                {
                    do
                    {
                        if (((dir == FORWARD) ? incl(pos) : decl(pos)) == -1)
                            break;
                    } while (gchar_pos(pos) == NUL);
                    if (dir == FORWARD)
                        break found;
                }
                /*
                 * If on the start of a paragraph or a section and searching forward, go to the next line.
                 */
                else if (dir == FORWARD && pos.col == 0 && startPS(pos.lnum, NUL, false))
                {
                    if (pos.lnum == curbuf.b_ml.ml_line_count)
                        return false;
                    pos.lnum++;
                    break found;
                }
                else if (dir == BACKWARD)
                    decl(pos);

                /* go back to the previous non-blank char */
                boolean found_dot = false;
                for (int c; (c = gchar_pos(pos)) == ' ' || c == '\t' ||
                    (dir == BACKWARD && vim_strchr(u8(".!?)]\"'"), c) != null); )
                {
                    if (vim_strchr(u8(".!?"), c) != null)
                    {
                        /* only skip over a '.', '!' and '?' once */
                        if (found_dot)
                            break;
                        found_dot = true;
                    }
                    if (decl(pos) == -1)
                        break;
                    /* when going forward: stop in front of empty line */
                    if (lineempty(pos.lnum) && dir == FORWARD)
                    {
                        incl(pos);
                        break found;
                    }
                }

                /* remember the line where the search started */
                long startlnum = pos.lnum;
                boolean cpo_J = (vim_strbyte(p_cpo[0], CPO_ENDOFSENT) != null);

                pos_C tpos = new pos_C();

                for ( ; ; )                 /* find end of sentence */
                {
                    int c = gchar_pos(pos);
                    if (c == NUL || (pos.col == 0 && startPS(pos.lnum, NUL, false)))
                    {
                        if (dir == BACKWARD && pos.lnum != startlnum)
                            pos.lnum++;
                        break;
                    }
                    if (c == '.' || c == '!' || c == '?')
                    {
                        COPY_pos(tpos, pos);
                        do
                        {
                            if ((c = inc(tpos)) == -1)
                                break;
                        } while (vim_strchr(u8(")]\"'"), c = gchar_pos(tpos)) != null);
                        if (c == -1 || (!cpo_J && (c == ' ' || c == '\t')) || c == NUL
                            || (cpo_J && (c == ' ' && 0 <= inc(tpos) && gchar_pos(tpos) == ' ')))
                        {
                            COPY_pos(pos, tpos);
                            if (gchar_pos(pos) == NUL)
                                inc(pos);               /* skip NUL at EOL */
                            break;
                        }
                    }
                    if (((dir == FORWARD) ? incl(pos) : decl(pos)) == -1)
                    {
                        if (count != 0)
                            return false;
                        noskip = true;
                        break;
                    }
                }
            }

            for (int c; !noskip && ((c = gchar_pos(pos)) == ' ' || c == '\t'); )
                if (incl(pos) == -1)                /* skip white space */
                    break;
        }

        setpcmark();
        COPY_pos(curwin.w_cursor, pos);
        return true;
    }

    /*
     * Find the next paragraph or section in direction 'dir'.
     * Paragraphs are currently supposed to be separated by empty lines.
     * If 'what' is NUL we go to the next paragraph.
     * If 'what' is '{' or '}' we go to the next section.
     * If 'both' is true also stop at '}'.
     * Return true if the next paragraph or section was found.
     */
    /*private*/ static boolean findpar(boolean[] pincl, int dir, long count, int what, boolean both)
        /* pincl: Return: true if last char is to be included */
    {
        boolean posix = (vim_strbyte(p_cpo[0], CPO_PARA) != null);

        long curr = curwin.w_cursor.lnum;

        while (0 < count--)
        {
            boolean did_skip = false;       /* true after separating lines have been skipped */

            for (boolean first = true; ; first = false)     /* true on first line */
            {
                if (ml_get(curr).at(0) != NUL)
                    did_skip = true;

                /* POSIX has it's own ideas of what a paragraph boundary is and
                 * it doesn't match historical Vi: It also stops at a "{" in the
                 * first column and at an empty line. */
                if (!first && did_skip && (startPS(curr, what, both)
                               || (posix && what == NUL && ml_get(curr).at(0) == (byte)'{')))
                    break;

                if ((curr += dir) < 1 || curbuf.b_ml.ml_line_count < curr)
                {
                    if (count != 0)
                        return false;
                    curr -= dir;
                    break;
                }
            }
        }
        setpcmark();
        if (both && ml_get(curr).at(0) == (byte)'}')   /* include line with '}' */
            curr++;
        curwin.w_cursor.lnum = curr;
        if (curr == curbuf.b_ml.ml_line_count && what != '}')
        {
            if ((curwin.w_cursor.col = strlen(ml_get(curr))) != 0)
            {
                --curwin.w_cursor.col;
                pincl[0] = true;
            }
        }
        else
            curwin.w_cursor.col = 0;
        return true;
    }

    /*
     * check if the string 's' is a nroff macro that is in option 'opt'
     */
    /*private*/ static boolean inmacro(Bytes opt, Bytes s)
    {
        Bytes macro;

        for (macro = opt; macro.at(0) != NUL; macro = macro.plus(1))
        {
            /* Accept two characters in the option being equal to two characters in the line.
             * A space in the option matches with a space in the line or the line having ended.
             */
            if ((macro.at(0) == s.at(0)
                        || (macro.at(0) == (byte)' '
                            && (s.at(0) == NUL || s.at(0) == (byte)' ')))
                    && (macro.at(1) == s.at(1)
                        || ((macro.at(1) == NUL || macro.at(1) == (byte)' ')
                            && (s.at(0) == NUL || s.at(1) == NUL || s.at(1) == (byte)' '))))
                break;
            macro = macro.plus(1);
            if (macro.at(0) == NUL)
                break;
        }

        return (macro.at(0) != NUL);
    }

    /*
     * startPS: return true if line 'lnum' is the start of a section or paragraph.
     * If 'para' is '{' or '}' only check for sections.
     * If 'both' is true also stop at '}'.
     */
    /*private*/ static boolean startPS(long lnum, int para, boolean both)
    {
        Bytes s = ml_get(lnum);
        if (s.at(0) == para || s.at(0) == (byte)'\f' || (both && s.at(0) == (byte)'}'))
            return true;
        if (s.at(0) == (byte)'.' && (inmacro(p_sections[0], s.plus(1)) || (para == NUL && inmacro(p_para[0], s.plus(1)))))
            return true;

        return false;
    }

    /*
     * The following routines do the word searches performed
     * by the 'w', 'W', 'b', 'B', 'e', and 'E' commands.
     */

    /*
     * To perform these searches, characters are placed into one of three
     * classes, and transitions between classes determine word boundaries.
     *
     * The classes are:
     *
     * 0 - white space
     * 1 - punctuation
     * 2 or higher - keyword characters (letters, digits and underscore)
     */

    /*private*/ static boolean cls_bigword;     /* true for "W", "B" or "E" */

    /*
     * cls() - returns the class of character at curwin.w_cursor
     *
     * If a 'W', 'B', or 'E' motion is being done (cls_bigword == true),
     * chars from class 2 and higher are reported as class 1 since only
     * white space boundaries are of interest.
     */
    /*private*/ static int cls()
    {
        int c = gchar_cursor();
        if (c == ' ' || c == '\t' || c == NUL)
            return 0;

        c = utf_class(c);
        if (c != 0 && cls_bigword)
            return 1;

        return c;
    }

    /*
     * fwd_word(count, type, eol) - move forward one word
     *
     * Returns false if the cursor was already at the end of the file.
     * If eol is true, last word stops at end of line (for operators).
     */
    /*private*/ static boolean fwd_word(long count, boolean bigword, boolean eol)
        /* bigword: "W", "E" or "B" */
    {
        curwin.w_cursor.coladd = 0;
        cls_bigword = bigword;

        while (0 < count--)
        {
            int sclass = cls();         /* starting class */

            /*
             * We always move at least one character,
             * unless on the last character in the buffer.
             */
            boolean last_line = (curwin.w_cursor.lnum == curbuf.b_ml.ml_line_count);
            int i = inc_cursor();
            if (i == -1 || (1 <= i && last_line))   /* started at last char in file */
                return false;
            if (1 <= i && eol && count == 0)        /* started at last char in line */
                return true;

            /*
             * Go one char past end of current word (if any).
             */
            if (sclass != 0)
                while (cls() == sclass)
                {
                    i = inc_cursor();
                    if (i == -1 || (1 <= i && eol && count == 0))
                        return true;
                }

            /*
             * go to next non-white
             */
            while (cls() == 0)
            {
                /*
                 * We'll stop if we land on a blank line
                 */
                if (curwin.w_cursor.col == 0 && ml_get_curline().at(0) == NUL)
                    break;

                i = inc_cursor();
                if (i == -1 || (1 <= i && eol && count == 0))
                    return true;
            }
        }

        return true;
    }

    /*
     * bck_word() - move backward 'count' words
     *
     * If stop is true and we are already on the start of a word, move one less.
     *
     * Returns false if top of the file was reached.
     */
    /*private*/ static boolean bck_word(long count, boolean bigword, boolean stop)
    {
        curwin.w_cursor.coladd = 0;
        cls_bigword = bigword;

        while (0 < count--)
        {
            int sclass = cls();             /* starting class */

            if (dec_cursor() == -1)         /* started at start of file */
                return false;

            finished:
            {
                if (!stop || sclass == cls() || sclass == 0)
                {
                    /*
                     * Skip white space before the word.
                     * Stop on an empty line.
                     */
                    while (cls() == 0)
                    {
                        if (curwin.w_cursor.col == 0 && lineempty(curwin.w_cursor.lnum))
                            break finished;
                        if (dec_cursor() == -1) /* hit start of file, stop here */
                            return true;
                    }

                    /*
                     * Move backward to start of this word.
                     */
                    if (skip_chars(cls(), BACKWARD))
                        return true;
                }

                inc_cursor();                   /* overshot - forward one */
            }

            stop = false;
        }

        return true;
    }

    /*
     * end_word() - move to the end of the word
     *
     * There is an apparent bug in the 'e' motion of the real vi.  At least on the
     * System V Release 3 version for the 80386.  Unlike 'b' and 'w', the 'e'
     * motion crosses blank lines.  When the real vi crosses a blank line in an
     * 'e' motion, the cursor is placed on the FIRST character of the next
     * non-blank line. The 'E' command, however, works correctly.  Since this
     * appears to be a bug, I have not duplicated it here.
     *
     * Returns false if end of the file was reached.
     *
     * If stop is true and we are already on the end of a word, move one less.
     * If empty is true stop on an empty line.
     */
    /*private*/ static boolean end_word(long count, boolean bigword, boolean stop, boolean empty)
    {
        curwin.w_cursor.coladd = 0;
        cls_bigword = bigword;

        while (0 < count--)
        {
            int sclass = cls();         /* starting class */

            if (inc_cursor() == -1)
                return false;

            finished:
            {
                /*
                 * If we're in the middle of a word, we just have to move to the end of it.
                 */
                if (cls() == sclass && sclass != 0)
                {
                    /*
                     * Move forward to end of the current word
                     */
                    if (skip_chars(sclass, FORWARD))
                        return false;
                }
                else if (!stop || sclass == 0)
                {
                    /*
                     * We were at the end of a word.  Go to the end of the next word.
                     * First skip white space, if 'empty' is true, stop at empty line.
                     */
                    while (cls() == 0)
                    {
                        if (empty && curwin.w_cursor.col == 0 && lineempty(curwin.w_cursor.lnum))
                            break finished;
                        if (inc_cursor() == -1)     /* hit end of file, stop here */
                            return false;
                    }

                    /*
                     * Move forward to the end of this word.
                     */
                    if (skip_chars(cls(), FORWARD))
                        return false;
                }
                dec_cursor();                   /* overshot - one char backward */
            }

            stop = false;                   /* we move only one word less */
        }

        return true;
    }

    /*
     * Move back to the end of the word.
     *
     * Returns false if start of the file was reached.
     */
    /*private*/ static boolean bckend_word(long count, boolean bigword, boolean eol)
        /* bigword: true for "B" */
        /* eol: true: stop at end of line. */
    {
        curwin.w_cursor.coladd = 0;
        cls_bigword = bigword;

        while (0 < count--)
        {
            int sclass = cls();         /* starting class */

            int i = dec_cursor();
            if (i == -1)
                return false;
            if (eol && i == 1)
                return true;

            /*
             * Move backward to before the start of this word.
             */
            if (sclass != 0)
            {
                while (cls() == sclass)
                    if ((i = dec_cursor()) == -1 || (eol && i == 1))
                        return true;
            }

            /*
             * Move backward to end of the previous word
             */
            while (cls() == 0)
            {
                if (curwin.w_cursor.col == 0 && lineempty(curwin.w_cursor.lnum))
                    break;
                if ((i = dec_cursor()) == -1 || (eol && i == 1))
                    return true;
            }
        }

        return true;
    }

    /*
     * Skip a row of characters of the same class.
     * Return true when end-of-file reached, false otherwise.
     */
    /*private*/ static boolean skip_chars(int cclass, int dir)
    {
        while (cls() == cclass)
            if ((dir == FORWARD ? inc_cursor() : dec_cursor()) == -1)
                return true;

        return false;
    }

    /*
     * Go back to the start of the word or the start of white space
     */
    /*private*/ static void back_in_line()
    {
        int sclass = cls();                     /* starting class */

        for ( ; ; )
        {
            if (curwin.w_cursor.col == 0)       /* stop at start of line */
                break;
            dec_cursor();
            if (cls() != sclass)                /* stop at start of word */
            {
                inc_cursor();
                break;
            }
        }
    }

    /*private*/ static void find_first_blank(pos_C posp)
    {
        while (decl(posp) != -1)
        {
            int c = gchar_pos(posp);
            if (!vim_iswhite(c))
            {
                incl(posp);
                break;
            }
        }
    }

    /*
     * Skip count/2 sentences and count/2 separating white spaces.
     */
    /*private*/ static void findsent_forward(long count, boolean at_start_sent)
        /* at_start_sent: cursor is at start of sentence */
    {
        while (0 < count--)
        {
            findsent(FORWARD, 1L);
            if (at_start_sent)
                find_first_blank(curwin.w_cursor);
            if (count == 0 || at_start_sent)
                decl(curwin.w_cursor);
            at_start_sent = !at_start_sent;
        }
    }

    /*
     * Find word under cursor, cursor at end.
     * Used while an operator is pending, and in Visual mode.
     */
    /*private*/ static boolean current_word(oparg_C oap, long count, boolean include, boolean bigword)
        /* include: true: include word and white space */
        /* bigword: false == word, true == WORD */
    {
        boolean inclusive = true;
        boolean include_white = false;

        cls_bigword = bigword;

        pos_C start_pos = new pos_C();

        /* Correct cursor when 'selection' is exclusive. */
        if (VIsual_active && p_sel[0].at(0) == (byte)'e' && ltpos(VIsual, curwin.w_cursor))
            dec_cursor();

        /*
         * When Visual mode is not active, or when the VIsual area is only one
         * character, select the word and/or white space under the cursor.
         */
        if (!VIsual_active || eqpos(curwin.w_cursor, VIsual))
        {
            /*
             * Go to start of current word or white space.
             */
            back_in_line();
            COPY_pos(start_pos, curwin.w_cursor);

            /*
             * If the start is on white space, and white space should be included
             * ("   word"), or start is not on white space, and white space should
             * not be included ("word"), find end of word.
             */
            if ((cls() == 0) == include)
            {
                if (end_word(1L, bigword, true, true) == false)
                    return false;
            }
            else
            {
                /*
                 * If the start is not on white space, and white space should be included ("word   "),
                 * or start is on white space and white space should not be included ("   "),
                 * find start of word.
                 * If we end up in the first column of the next line (single char word)
                 * back up to end of the line.
                 */
                fwd_word(1L, bigword, true);
                if (curwin.w_cursor.col == 0)
                    decl(curwin.w_cursor);
                else
                    oneleft();

                if (include)
                    include_white = true;
            }

            if (VIsual_active)
            {
                /* should do something when inclusive == false ! */
                COPY_pos(VIsual, start_pos);
                redraw_curbuf_later(INVERTED);      /* update the inversion */
            }
            else
            {
                COPY_pos(oap.op_start, start_pos);
                oap.motion_type = MCHAR;
            }
            --count;
        }

        /*
         * When count is still > 0, extend with more objects.
         */
        while (0 < count)
        {
            inclusive = true;
            if (VIsual_active && ltpos(curwin.w_cursor, VIsual))
            {
                /*
                 * In Visual mode, with cursor at start: move cursor back.
                 */
                if (decl(curwin.w_cursor) == -1)
                    return false;

                if (include != (cls() != 0))
                {
                    if (bck_word(1L, bigword, true) == false)
                        return false;
                }
                else
                {
                    if (bckend_word(1L, bigword, true) == false)
                        return false;
                    incl(curwin.w_cursor);
                }
            }
            else
            {
                /*
                 * Move cursor forward one word and/or white area.
                 */
                if (incl(curwin.w_cursor) == -1)
                    return false;

                if (include != (cls() == 0))
                {
                    if (fwd_word(1L, bigword, true) == false && 1 < count)
                        return false;
                    /*
                     * If end is just past a new-line,
                     * we don't want to include the first character on the line.
                     * Put cursor on last char of white.
                     */
                    if (oneleft() == false)
                        inclusive = false;
                }
                else
                {
                    if (end_word(1L, bigword, true, true) == false)
                        return false;
                }
            }
            --count;
        }

        if (include_white && (cls() != 0 || (curwin.w_cursor.col == 0 && !inclusive)))
        {
            /*
             * If we don't include white space at the end, move the start to include
             * some white space there.  This makes "daw" work better on the last word in
             * a sentence (and "2daw" on last-but-one word).  Also when "2daw" deletes
             * "word." at the end of the line (cursor is at start of next line).
             * But don't delete white space at start of line (indent).
             */
            pos_C pos = new pos_C();
            COPY_pos(pos, curwin.w_cursor); /* save cursor position */
            COPY_pos(curwin.w_cursor, start_pos);
            if (oneleft() == true)
            {
                back_in_line();
                if (cls() == 0 && 0 < curwin.w_cursor.col)
                {
                    if (VIsual_active)
                        COPY_pos(VIsual, curwin.w_cursor);
                    else
                        COPY_pos(oap.op_start, curwin.w_cursor);
                }
            }
            COPY_pos(curwin.w_cursor, pos); /* put cursor back at end */
        }

        if (VIsual_active)
        {
            if (p_sel[0].at(0) == (byte)'e' && inclusive && ltoreq(VIsual, curwin.w_cursor))
                inc_cursor();
            if (VIsual_mode == 'V')
            {
                VIsual_mode = 'v';
                redraw_cmdline = true;              /* show mode later */
            }
        }
        else
            oap.inclusive = inclusive;

        return true;
    }

    /*
     * Find sentence(s) under the cursor, cursor at end.
     * When Visual active, extend it by one or more sentences.
     */
    /*private*/ static boolean current_sent(oparg_C oap, long count, boolean include)
    {
        pos_C start_pos = new pos_C();
        COPY_pos(start_pos, curwin.w_cursor);
        pos_C pos = new pos_C();
        COPY_pos(pos, start_pos);
        findsent(FORWARD, 1L);      /* Find start of next sentence. */

        extend:
        {
            /*
             * When the Visual area is bigger than one character: Extend it.
             */
            if (VIsual_active && !eqpos(start_pos, VIsual))
                break extend;

            /*
             * If the cursor started on a blank, check if it is just before
             * the start of the next sentence.
             */
            while (vim_iswhite(gchar_pos(pos)))
                incl(pos);

            boolean start_blank;
            if (eqpos(pos, curwin.w_cursor))
            {
                start_blank = true;
                find_first_blank(start_pos);    /* go back to first blank */
            }
            else
            {
                start_blank = false;
                findsent(BACKWARD, 1L);
                COPY_pos(start_pos, curwin.w_cursor);
            }

            long ncount;
            if (include)
                ncount = count * 2;
            else
            {
                ncount = count;
                if (start_blank)
                    --ncount;
            }
            if (0 < ncount)
                findsent_forward(ncount, true);
            else
                decl(curwin.w_cursor);

            if (include)
            {
                /*
                 * If the blank in front of the sentence is included, exclude the
                 * blanks at the end of the sentence, go back to the first blank.
                 * If there are no trailing blanks, try to include leading blanks.
                 */
                if (start_blank)
                {
                    find_first_blank(curwin.w_cursor);
                    if (vim_iswhite(gchar_pos(curwin.w_cursor)))
                        decl(curwin.w_cursor);
                }
                else
                {
                    if (!vim_iswhite(gchar_cursor()))
                        find_first_blank(start_pos);
                }
            }

            if (VIsual_active)
            {
                /* Avoid getting stuck with "is" on a single space before a sentence. */
                if (eqpos(start_pos, curwin.w_cursor))
                    break extend;
                if (p_sel[0].at(0) == (byte)'e')
                    curwin.w_cursor.col++;
                COPY_pos(VIsual, start_pos);
                VIsual_mode = 'v';
                redraw_curbuf_later(INVERTED);  /* update the inversion */
            }
            else
            {
                /* include a newline after the sentence, if there is one */
                if (incl(curwin.w_cursor) == -1)
                    oap.inclusive = true;
                else
                    oap.inclusive = false;
                COPY_pos(oap.op_start, start_pos);
                oap.motion_type = MCHAR;
            }

            return true;
        }

        if (ltpos(start_pos, VIsual))
        {
            /*
             * Cursor at start of Visual area.
             * Find out where we are:
             * - in the white space before a sentence
             * - in a sentence or just after it
             * - at the start of a sentence
             */
            boolean at_start_sent = true;
            decl(pos);
            while (ltpos(pos, curwin.w_cursor))
            {
                if (!vim_iswhite(gchar_pos(pos)))
                {
                    at_start_sent = false;
                    break;
                }
                incl(pos);
            }
            if (!at_start_sent)
            {
                findsent(BACKWARD, 1L);
                if (eqpos(curwin.w_cursor, start_pos))
                    at_start_sent = true;   /* exactly at start of sentence */
                else
                    /* inside a sentence, go to its end (start of next) */
                    findsent(FORWARD, 1L);
            }
            if (include)                    /* "as" gets twice as much as "is" */
                count *= 2;
            while (0 < count--)
            {
                if (at_start_sent)
                    find_first_blank(curwin.w_cursor);
                if (!at_start_sent || (!include && !vim_iswhite(gchar_cursor())))
                    findsent(BACKWARD, 1L);
                at_start_sent = !at_start_sent;
            }
        }
        else
        {
            /*
             * Cursor at end of Visual area.
             * Find out where we are:
             * - just before a sentence
             * - just before or in the white space before a sentence
             * - in a sentence
             */
            incl(pos);
            boolean at_start_sent = true;
            if (!eqpos(pos, curwin.w_cursor))   /* not just before a sentence */
            {
                at_start_sent = false;
                while (ltpos(pos, curwin.w_cursor))
                {
                    if (!vim_iswhite(gchar_pos(pos)))
                    {
                        at_start_sent = true;
                        break;
                    }
                    incl(pos);
                }
                if (at_start_sent)      /* in the sentence */
                    findsent(BACKWARD, 1L);
                else            /* in/before white before a sentence */
                    COPY_pos(curwin.w_cursor, start_pos);
            }

            if (include)        /* "as" gets twice as much as "is" */
                count *= 2;
            findsent_forward(count, at_start_sent);
            if (p_sel[0].at(0) == (byte)'e')
                curwin.w_cursor.col++;
        }

        return true;
    }

    /*
     * Find block under the cursor, cursor at end.
     * "what" and "other" are two matching parenthesis/brace/etc.
     */
    /*private*/ static boolean current_block(oparg_C oap, long count, boolean include, int what, int other)
        /* include: true == include white space */
        /* what: '(', '{', etc. */
        /* other: ')', '}', etc. */
    {
        pos_C pos = null;
        pos_C start_pos = new pos_C();
        pos_C end_pos;
        boolean sol = false;                    /* '{' at start of line */

        pos_C old_pos = new pos_C();
        COPY_pos(old_pos, curwin.w_cursor);
        pos_C old_end = new pos_C();
        COPY_pos(old_end, curwin.w_cursor);     /* remember where we started */
        pos_C old_start = new pos_C();
        COPY_pos(old_start, old_end);

        /*
         * If we start on '(', '{', ')', '}', etc., use the whole block inclusive.
         */
        if (!VIsual_active || eqpos(VIsual, curwin.w_cursor))
        {
            setpcmark();
            if (what == '{')                    /* ignore indent */
                while (inindent(1))
                    if (inc_cursor() != 0)
                        break;
            if (gchar_cursor() == what)
                /* cursor on '(' or '{', move cursor just after it */
                curwin.w_cursor.col++;
        }
        else if (ltpos(VIsual, curwin.w_cursor))
        {
            COPY_pos(old_start, VIsual);
            COPY_pos(curwin.w_cursor, VIsual); /* cursor at low end of Visual */
        }
        else
            COPY_pos(old_end, VIsual);

        /*
         * Search backwards for unclosed '(', '{', etc..
         * Put this position in start_pos.
         * Ignore quotes here.  Keep the "M" flag in 'cpo', as that is what the user wants.
         */
        Bytes save_cpo = p_cpo[0];
        p_cpo[0] = (vim_strbyte(p_cpo[0], CPO_MATCHBSL) != null) ? u8("%M") : u8("%");
        while (0 < count--)
        {
            if ((pos = findmatch(null, what)) == null)
                break;
            COPY_pos(curwin.w_cursor, pos);
            COPY_pos(start_pos, pos); /* the findmatch for end_pos will overwrite *pos */
        }
        p_cpo[0] = save_cpo;

        /*
         * Search for matching ')', '}', etc.
         * Put this position in curwin.w_cursor.
         */
        if (pos == null || (end_pos = findmatch(null, other)) == null)
        {
            COPY_pos(curwin.w_cursor, old_pos);
            return false;
        }
        COPY_pos(curwin.w_cursor, end_pos);

        /*
         * Try to exclude the '(', '{', ')', '}', etc. when "include" is false.
         * If the ending '}', ')' or ']' is only preceded by indent, skip that indent.
         * But only if the resulting area is not smaller than what we started with.
         */
        while (!include)
        {
            incl(start_pos);
            sol = (curwin.w_cursor.col == 0);
            decl(curwin.w_cursor);
            while (inindent(1))
            {
                sol = true;
                if (decl(curwin.w_cursor) != 0)
                    break;
            }

            /*
             * In Visual mode, when the resulting area is not bigger than what we
             * started with, extend it to the next block, and then exclude again.
             */
            if (!ltpos(start_pos, old_start) && !ltpos(old_end, curwin.w_cursor) && VIsual_active)
            {
                COPY_pos(curwin.w_cursor, old_start);
                decl(curwin.w_cursor);
                if ((pos = findmatch(null, what)) == null)
                {
                    COPY_pos(curwin.w_cursor, old_pos);
                    return false;
                }
                COPY_pos(start_pos, pos);
                COPY_pos(curwin.w_cursor, pos);
                if ((end_pos = findmatch(null, other)) == null)
                {
                    COPY_pos(curwin.w_cursor, old_pos);
                    return false;
                }
                COPY_pos(curwin.w_cursor, end_pos);
            }
            else
                break;
        }

        if (VIsual_active)
        {
            if (p_sel[0].at(0) == (byte)'e')
                curwin.w_cursor.col++;
            if (sol && gchar_cursor() != NUL)
                inc(curwin.w_cursor);               /* include the line break */
            COPY_pos(VIsual, start_pos);
            VIsual_mode = 'v';
            redraw_curbuf_later(INVERTED);          /* update the inversion */
            showmode();
        }
        else
        {
            COPY_pos(oap.op_start, start_pos);
            oap.motion_type = MCHAR;
            oap.inclusive = false;
            if (sol)
                incl(curwin.w_cursor);
            else if (ltoreq(start_pos, curwin.w_cursor))
                /* Include the character under the cursor. */
                oap.inclusive = true;
            else
                /* End is before the start (no text in between <>, [], etc.): don't operate on any text. */
                COPY_pos(curwin.w_cursor, start_pos);
        }

        return true;
    }

    /*
     * Return true if the cursor is on a "<aaa>" tag.  Ignore "<aaa/>".
     * When "end_tag" is true return true if the cursor is on "</aaa>".
     */
    /*private*/ static boolean in_html_tag(boolean end_tag)
    {
        Bytes line = ml_get_curline();

        Bytes p = line.plus(curwin.w_cursor.col);
        while (BLT(line, p))
        {
            if (p.at(0) == (byte)'<')      /* find '<' under/before cursor */
                break;
            p = p.minus(us_ptr_back(line, p));
            if (p.at(0) == (byte)'>')      /* find '>' before cursor */
                break;
        }
        if (p.at(0) != (byte)'<')
            return false;

        pos_C pos = new pos_C();
        pos.lnum = curwin.w_cursor.lnum;
        pos.col = BDIFF(p, line);

        p = p.plus(us_ptr2len_cc(p));
        if (end_tag)
        {
            /* check that there is a '/' after the '<' */
            return (p.at(0) == (byte)'/');
        }

        /* check that there is no '/' after the '<' */
        if (p.at(0) == (byte)'/')
            return false;

        byte lc = NUL;

        /* check that the matching '>' is not preceded by '/' */
        for ( ; ; )
        {
            if (inc(pos) < 0)
                return false;
            byte c = ml_get_pos(pos).at(0);
            if (c == '>')
                break;
            lc = c;
        }

        return (lc != '/');
    }

    /*
     * Find tag block under the cursor, cursor at end.
     */
    /*private*/ static boolean current_tagblock(oparg_C oap, long count_arg, boolean include)
        /* include: true == include white space */
    {
        boolean retval = false;

        long count = count_arg;
        boolean do_include = include;
        boolean save_p_ws = p_ws[0];
        boolean is_inclusive = true;

        p_ws[0] = false;

        pos_C old_pos = new pos_C();
        COPY_pos(old_pos, curwin.w_cursor);
        pos_C old_end = new pos_C();
        COPY_pos(old_end, curwin.w_cursor);         /* remember where we started */
        pos_C old_start = new pos_C();
        COPY_pos(old_start, old_end);
        if (!VIsual_active || p_sel[0].at(0) == (byte)'e')
            decl(old_end);                          /* old_end is inclusive */

        /*
         * If we start on "<aaa>" select that block.
         */
        if (!VIsual_active || eqpos(VIsual, curwin.w_cursor))
        {
            setpcmark();

            /* ignore indent */
            while (inindent(1))
                if (inc_cursor() != 0)
                    break;

            if (in_html_tag(false))
            {
                /* cursor on start tag, move to its '>' */
                while (ml_get_cursor().at(0) != (byte)'>')
                    if (inc_cursor() < 0)
                        break;
            }
            else if (in_html_tag(true))
            {
                /* cursor on end tag, move to just before it */
                while (ml_get_cursor().at(0) != (byte)'<')
                    if (dec_cursor() < 0)
                        break;
                dec_cursor();
                COPY_pos(old_end, curwin.w_cursor);
            }
        }
        else if (ltpos(VIsual, curwin.w_cursor))
        {
            COPY_pos(old_start, VIsual);
            COPY_pos(curwin.w_cursor, VIsual); /* cursor at low end of Visual */
        }
        else
            COPY_pos(old_end, VIsual);

        pos_C start_pos = new pos_C();
        pos_C end_pos = new pos_C();

        theend:
        {
            again:
            for ( ; ; )
            {
                /*
                 * Search backwards for unclosed "<aaa>".
                 * Put this position in start_pos.
                 */
                for (long n = 0; n < count; n++)
                {
                    if (do_searchpair(u8("<[^ \t>/!]\\+\\%(\\_s\\_[^>]\\{-}[^/]>\\|$\\|\\_s\\=>\\)"), u8(""), u8("</[^>]*>"), BACKWARD, u8(""), 0, null, 0, 0L) <= 0)
                    {
                        COPY_pos(curwin.w_cursor, old_pos);
                        break theend;
                    }
                }

                COPY_pos(start_pos, curwin.w_cursor);

                /*
                 * Search for matching "</aaa>".  First isolate the "aaa".
                 */
                inc_cursor();
                Bytes p = ml_get_cursor();
                Bytes cp;
                for (cp = p; cp.at(0) != NUL && cp.at(0) != (byte)'>' && !vim_iswhite(cp.at(0)); cp = cp.plus(us_ptr2len_cc(cp)))
                    ;
                int len = BDIFF(cp, p);
                if (len == 0)
                {
                    COPY_pos(curwin.w_cursor, old_pos);
                    break theend;
                }

                Bytes spat = new Bytes(len + 31);
                Bytes epat = new Bytes(len + 9);
                libC.sprintf(spat, u8("<%.*s\\>\\%%(\\s\\_[^>]\\{-}[^/]>\\|>\\)\\c"), len, p);
                libC.sprintf(epat, u8("</%.*s>\\c"), len, p);

                long r = do_searchpair(spat, u8(""), epat, FORWARD, u8(""), 0, null, 0, 0L);

                if (r < 1 || ltpos(curwin.w_cursor, old_end))
                {
                    /* Can't find other end or it's before the previous end.
                     * Could be a HTML tag that doesn't have a matching end.
                     * Search backwards for another starting tag. */
                    count = 1;
                    COPY_pos(curwin.w_cursor, start_pos);
                    continue again;
                }

                if (do_include || r < 1)
                {
                    /* Include up to the '>'. */
                    while (ml_get_cursor().at(0) != (byte)'>')
                        if (inc_cursor() < 0)
                            break;
                }
                else
                {
                    Bytes c = ml_get_cursor();

                    /* Exclude the '<' of the end tag.
                     * If the closing tag is on new line, do not decrement cursor, but
                     * make operation exclusive, so that the linefeed will be selected */
                    if (c.at(0) == (byte)'<' && !VIsual_active && curwin.w_cursor.col == 0)
                        /* do not decrement cursor */
                        is_inclusive = false;
                    else if (c.at(0) == (byte)'<')
                        dec_cursor();
                }

                COPY_pos(end_pos, curwin.w_cursor);

                if (!do_include)
                {
                    /* Exclude the start tag. */
                    COPY_pos(curwin.w_cursor, start_pos);
                    while (0 <= inc_cursor())
                        if (ml_get_cursor().at(0) == (byte)'>')
                        {
                            inc_cursor();
                            COPY_pos(start_pos, curwin.w_cursor);
                            break;
                        }
                    COPY_pos(curwin.w_cursor, end_pos);

                    /* If we now have the same text as before reset "do_include" and try again. */
                    if (eqpos(start_pos, old_start) && eqpos(end_pos, old_end))
                    {
                        do_include = true;
                        COPY_pos(curwin.w_cursor, old_start);
                        count = count_arg;
                        continue again;
                    }
                }

                break;
            }

            if (VIsual_active)
            {
                /* If the end is before the start there is no text between tags,
                 * select the char under the cursor. */
                if (ltpos(end_pos, start_pos))
                    COPY_pos(curwin.w_cursor, start_pos);
                else if (p_sel[0].at(0) == (byte)'e')
                    inc_cursor();
                COPY_pos(VIsual, start_pos);
                VIsual_mode = 'v';
                redraw_curbuf_later(INVERTED);  /* update the inversion */
                showmode();
            }
            else
            {
                COPY_pos(oap.op_start, start_pos);
                oap.motion_type = MCHAR;
                if (ltpos(end_pos, start_pos))
                {
                    /* End is before the start: there is no text between tags;
                     * operate on an empty area. */
                    COPY_pos(curwin.w_cursor, start_pos);
                    oap.inclusive = false;
                }
                else
                    oap.inclusive = is_inclusive;
            }
            retval = true;
        }

        p_ws[0] = save_p_ws;
        return retval;
    }

    /*private*/ static boolean current_par(oparg_C oap, long count, boolean include, int type)
        /* include: true == include white space */
        /* type: 'p' for paragraph, 'S' for section */
    {
        if (type == 'S')        /* not implemented yet */
            return false;

        long start_lnum = curwin.w_cursor.lnum;

        extend:
        {
            /*
             * When visual area is more than one line: extend it.
             */
            if (VIsual_active && start_lnum != VIsual.lnum)
                break extend;

            /*
             * First move back to the start_lnum of the paragraph or white lines
             */
            boolean white_in_front = linewhite(start_lnum);
            while (1 < start_lnum)
            {
                if (white_in_front)         /* stop at first white line */
                {
                    if (!linewhite(start_lnum - 1))
                        break;
                }
                else            /* stop at first non-white line of start of paragraph */
                {
                    if (linewhite(start_lnum - 1) || startPS(start_lnum, NUL, false))
                        break;
                }
                --start_lnum;
            }

            /*
             * Move past the end of any white lines.
             */
            long end_lnum = start_lnum;
            while (end_lnum <= curbuf.b_ml.ml_line_count && linewhite(end_lnum))
                end_lnum++;

            boolean do_white = false;

            --end_lnum;
            int i = (int)count;
            if (!include && white_in_front)
                --i;
            while (0 < i--)
            {
                if (end_lnum == curbuf.b_ml.ml_line_count)
                    return false;

                if (!include)
                    do_white = linewhite(end_lnum + 1);

                if (include || !do_white)
                {
                    end_lnum++;
                    /*
                     * skip to end of paragraph
                     */
                    while (end_lnum < curbuf.b_ml.ml_line_count
                            && !linewhite(end_lnum + 1)
                            && !startPS(end_lnum + 1, NUL, false))
                        end_lnum++;
                }

                if (i == 0 && white_in_front && include)
                    break;

                /*
                 * skip to end of white lines after paragraph
                 */
                if (include || do_white)
                    while (end_lnum < curbuf.b_ml.ml_line_count && linewhite(end_lnum + 1))
                        end_lnum++;
            }

            /*
             * If there are no empty lines at the end, try to find some empty lines
             * at the start (unless that has been already done).
             */
            if (!white_in_front && !linewhite(end_lnum) && include)
                while (1 < start_lnum && linewhite(start_lnum - 1))
                    --start_lnum;

            if (VIsual_active)
            {
                /* Problem: when doing "Vipipip" nothing happens in a single white line,
                 * we get stuck there.  Trap this here. */
                if (VIsual_mode == 'V' && start_lnum == curwin.w_cursor.lnum)
                    break extend;
                VIsual.lnum = start_lnum;
                VIsual_mode = 'V';
                redraw_curbuf_later(INVERTED);  /* update the inversion */
                showmode();
            }
            else
            {
                oap.op_start.lnum = start_lnum;
                oap.op_start.col = 0;
                oap.motion_type = MLINE;
            }

            curwin.w_cursor.lnum = end_lnum;
            curwin.w_cursor.col = 0;

            return true;
        }

        boolean retval = true;
        int dir = (start_lnum < VIsual.lnum) ? BACKWARD : FORWARD;

        for (int i = (int)count; 0 <= --i; )
        {
            if (start_lnum == (dir == BACKWARD ? 1 : curbuf.b_ml.ml_line_count))
            {
                retval = false;
                break;
            }

            int prev_start_is_white = -1;
            for (int t = 0; t < 2; t++)
            {
                start_lnum += dir;
                boolean start_is_white = linewhite(start_lnum);
                if (prev_start_is_white == (start_is_white ? 1 : 0))
                {
                    start_lnum -= dir;
                    break;
                }
                for ( ; ; )
                {
                    if (start_lnum == (dir == BACKWARD ? 1 : curbuf.b_ml.ml_line_count))
                        break;
                    if (start_is_white != linewhite(start_lnum + dir)
                            || (!start_is_white && startPS(start_lnum + (0 < dir ? 1 : 0), NUL, false)))
                        break;
                    start_lnum += dir;
                }
                if (!include)
                    break;
                if (start_lnum == (dir == BACKWARD ? 1 : curbuf.b_ml.ml_line_count))
                    break;
                prev_start_is_white = start_is_white ? 1 : 0;
            }
        }

        curwin.w_cursor.lnum = start_lnum;
        curwin.w_cursor.col = 0;

        return retval;
    }

    /*
     * Search quote char from string line[col].
     * Quote character escaped by one of the characters in "escape" is not counted as a quote.
     * Returns column number of "quotechar" or -1 when not found.
     */
    /*private*/ static int find_next_quote(Bytes line, int col, int quotechar, Bytes escape)
        /* escape: escape characters, can be null */
    {
        for ( ; ; )
        {
            int c = line.at(col);
            if (c == NUL)
                return -1;
            else if (escape != null && vim_strchr(escape, c) != null)
                col++;
            else if (c == quotechar)
                break;
            col += us_ptr2len_cc(line.plus(col));
        }
        return col;
    }

    /*
     * Search backwards in "line" from column "col_start" to find "quotechar".
     * Quote character escaped by one of the characters in "escape" is not counted as a quote.
     * Return the found column or zero.
     */
    /*private*/ static int find_prev_quote(Bytes line, int col_start, int quotechar, Bytes escape)
        /* escape: escape characters, can be null */
    {
        int n;

        while (0 < col_start)
        {
            --col_start;
            col_start -= us_head_off(line, line.plus(col_start));
            n = 0;
            if (escape != null)
                while (0 < col_start - n && vim_strchr(escape, line.at(col_start - n - 1)) != null)
                    n++;
            if ((n & 1) != 0)
                col_start -= n;     /* uneven number of escape chars, skip it */
            else if (line.at(col_start) == quotechar)
                break;
        }
        return col_start;
    }

    /*
     * Find quote under the cursor, cursor at end.
     * Returns true if found, else false.
     */
    /*private*/ static boolean current_quote(oparg_C oap, long count, boolean include, int quotechar)
        /* include: true == include quote char */
        /* quotechar: Quote character */
    {
        Bytes line = ml_get_curline();
        int col_end;
        int col_start = curwin.w_cursor.col;

        boolean inclusive = false;
        boolean vis_empty = true;           /* Visual selection <= 1 char */
        boolean vis_bef_curs = false;       /* Visual starts before cursor */
        boolean inside_quotes = false;      /* Looks like "i'" done before */
        boolean selected_quote = false;     /* Has quote inside selection */

        /* Correct cursor when 'selection' is exclusive. */
        if (VIsual_active)
        {
            vis_bef_curs = ltpos(VIsual, curwin.w_cursor);
            if (p_sel[0].at(0) == (byte)'e' && vis_bef_curs)
                dec_cursor();
            vis_empty = eqpos(VIsual, curwin.w_cursor);
        }

        if (!vis_empty)
        {
            int i;
            /* Check if the existing selection exactly spans the text inside quotes. */
            if (vis_bef_curs)
            {
                inside_quotes = 0 < VIsual.col
                            && line.at(VIsual.col - 1) == quotechar
                            && line.at(curwin.w_cursor.col) != NUL
                            && line.at(curwin.w_cursor.col + 1) == quotechar;
                i = VIsual.col;
                col_end = curwin.w_cursor.col;
            }
            else
            {
                inside_quotes = 0 < curwin.w_cursor.col
                            && line.at(curwin.w_cursor.col - 1) == quotechar
                            && line.at(VIsual.col) != NUL
                            && line.at(VIsual.col + 1) == quotechar;
                i = curwin.w_cursor.col;
                col_end = VIsual.col;
            }

            /* Find out if we have a quote in the selection. */
            while (i <= col_end)
                if (line.at(i++) == quotechar)
                {
                    selected_quote = true;
                    break;
                }
        }

        if (!vis_empty && line.at(col_start) == quotechar)
        {
            /* Already selecting something and on a quote character.
             * Find the next quoted string. */
            if (vis_bef_curs)
            {
                /* Assume we are on a closing quote: move to after the next opening quote. */
                col_start = find_next_quote(line, col_start + 1, quotechar, null);
                if (col_start < 0)
                    return false;
                col_end = find_next_quote(line, col_start + 1, quotechar, curbuf.b_p_qe[0]);
                if (col_end < 0)
                {
                    /* We were on a starting quote perhaps? */
                    col_end = col_start;
                    col_start = curwin.w_cursor.col;
                }
            }
            else
            {
                col_end = find_prev_quote(line, col_start, quotechar, null);
                if (line.at(col_end) != quotechar)
                    return false;
                col_start = find_prev_quote(line, col_end, quotechar, curbuf.b_p_qe[0]);
                if (line.at(col_start) != quotechar)
                {
                    /* We were on an ending quote perhaps? */
                    col_start = col_end;
                    col_end = curwin.w_cursor.col;
                }
            }
        }
        else

        if (line.at(col_start) == quotechar || !vis_empty)
        {
            int first_col = col_start;

            if (!vis_empty)
            {
                if (vis_bef_curs)
                    first_col = find_next_quote(line, col_start, quotechar, null);
                else
                    first_col = find_prev_quote(line, col_start, quotechar, null);
            }

            /* The cursor is on a quote, we don't know if it's the opening or
             * closing quote.  Search from the start of the line to find out.
             * Also do this when there is a Visual area, a' may leave the cursor
             * in between two strings. */
            col_start = 0;
            for ( ; ; )
            {
                /* Find open quote character. */
                col_start = find_next_quote(line, col_start, quotechar, null);
                if (col_start < 0 || first_col < col_start)
                    return false;
                /* Find close quote character. */
                col_end = find_next_quote(line, col_start + 1, quotechar, curbuf.b_p_qe[0]);
                if (col_end < 0)
                    return false;
                /* If is cursor between start and end quote character,
                 * it is target text object. */
                if (col_start <= first_col && first_col <= col_end)
                    break;
                col_start = col_end + 1;
            }
        }
        else
        {
            /* Search backward for a starting quote. */
            col_start = find_prev_quote(line, col_start, quotechar, curbuf.b_p_qe[0]);
            if (line.at(col_start) != quotechar)
            {
                /* No quote before the cursor, look after the cursor. */
                col_start = find_next_quote(line, col_start, quotechar, null);
                if (col_start < 0)
                    return false;
            }

            /* Find close quote character. */
            col_end = find_next_quote(line, col_start + 1, quotechar, curbuf.b_p_qe[0]);
            if (col_end < 0)
                return false;
        }

        /* When "include" is true,
         * include spaces after closing quote or before the starting quote. */
        if (include)
        {
            if (vim_iswhite(line.at(col_end + 1)))
                while (vim_iswhite(line.at(col_end + 1)))
                    col_end++;
            else
                while (0 < col_start && vim_iswhite(line.at(col_start - 1)))
                    --col_start;
        }

        /* Set start position.  After vi" another i" must include the ".
         * For v2i" include the quotes. */
        if (!include && count < 2 && (vis_empty || !inside_quotes))
            col_start++;
        curwin.w_cursor.col = col_start;
        if (VIsual_active)
        {
            /* Set the start of the Visual area when the Visual area was empty, we
             * were just inside quotes or the Visual area didn't start at a quote
             * and didn't include a quote.
             */
            if (vis_empty
                    || (vis_bef_curs
                        && !selected_quote
                        && (inside_quotes
                            || (line.at(VIsual.col) != quotechar
                                && (VIsual.col == 0
                                    || line.at(VIsual.col - 1) != quotechar)))))
            {
                COPY_pos(VIsual, curwin.w_cursor);
                redraw_curbuf_later(INVERTED);
            }
        }
        else
        {
            COPY_pos(oap.op_start, curwin.w_cursor);
            oap.motion_type = MCHAR;
        }

        /* Set end position. */
        curwin.w_cursor.col = col_end;
        /* After vi" another i" must include the ". */
        if ((include || 1 < count || (!vis_empty && inside_quotes)) && inc_cursor() == 2)
            inclusive = true;
        if (VIsual_active)
        {
            if (vis_empty || vis_bef_curs)
            {
                /* decrement cursor when 'selection' is not exclusive */
                if (p_sel[0].at(0) != (byte)'e')
                    dec_cursor();
            }
            else
            {
                /* Cursor is at start of Visual area.  Set the end of the Visual area
                 * when it was just inside quotes or it didn't end at a quote. */
                if (inside_quotes
                        || (!selected_quote
                            && line.at(VIsual.col) != quotechar
                            && (line.at(VIsual.col) == NUL
                                || line.at(VIsual.col + 1) != quotechar)))
                {
                    dec_cursor();
                    COPY_pos(VIsual, curwin.w_cursor);
                }
                curwin.w_cursor.col = col_start;
            }
            if (VIsual_mode == 'V')
            {
                VIsual_mode = 'v';
                redraw_cmdline = true;              /* show mode later */
            }
        }
        else
        {
            /* Set inclusive and other oap's flags. */
            oap.inclusive = inclusive;
        }

        return true;
    }

    /*
     * Find next search match under cursor, cursor at end.
     * Used while an operator is pending, and in Visual mode.
     */
    /*private*/ static boolean current_search(long count, boolean forward)
        /* forward: move forward or backwards */
    {
        pos_C save_VIsual = new pos_C();
        COPY_pos(save_VIsual, VIsual);

        /* wrapping should not occur */
        boolean old_p_ws = p_ws[0];
        p_ws[0] = false;

        /* Correct cursor when 'selection' is exclusive. */
        if (VIsual_active && p_sel[0].at(0) == (byte)'e' && ltpos(VIsual, curwin.w_cursor))
            dec_cursor();

        pos_C orig_pos = new pos_C();       /* position of the cursor at beginning */
        COPY_pos(orig_pos, curwin.w_cursor);
        pos_C pos = new pos_C();            /* position after the pattern */
        COPY_pos(pos, curwin.w_cursor);
        pos_C start_pos = new pos_C();      /* position before the pattern */
        COPY_pos(start_pos, curwin.w_cursor);

        if (VIsual_active)
        {
            COPY_pos(start_pos, VIsual);

            /* make sure, searching further will extend the match */
            if (VIsual_active)
            {
                if (forward)
                    incl(pos);
                else
                    decl(pos);
            }
        }

        /* Is the pattern is zero-width? */
        int one_char = is_one_char(spats[last_idx].pat, true);
        if (one_char == -1)
        {
            p_ws[0] = old_p_ws;
            return false;                   /* pattern not found */
        }

        /*
         * The trick is to first search backwards and then search forward again,
         * so that a match at the current cursor position will be correctly captured.
         */
        for (int round = 0; round < 2; round++)
        {
            boolean dir = forward ? (round != 0) : (round == 0);

            int flags = 0;
            if (!dir && one_char == 0)
                flags = SEARCH_END;

            int result = searchit(curwin, curbuf, pos, (dir ? FORWARD : BACKWARD),
                            spats[last_idx].pat, (round != 0) ? count : 1,
                                SEARCH_KEEP | flags, RE_SEARCH, 0, null);

            /* First search may fail, but then start searching from the beginning of
             * the file (cursor might be on the search match) except when Visual mode
             * is active, so that extending the visual selection works. */
            if (result == 0)
            {
                if (round != 0)     /* not found, abort */
                {
                    COPY_pos(curwin.w_cursor, orig_pos);
                    if (VIsual_active)
                        COPY_pos(VIsual, save_VIsual);
                    p_ws[0] = old_p_ws;
                    return false;
                }

                if (forward)        /* try again from start of buffer */
                {
                    clearpos(pos);
                }
                else                /* try again from end of buffer */
                {
                    /* searching backwards, so set pos to last line and col */
                    pos.lnum = curwin.w_buffer.b_ml.ml_line_count;
                    pos.col  = strlen(ml_get(curwin.w_buffer.b_ml.ml_line_count));
                }
            }
            p_ws[0] = old_p_ws;
        }

        COPY_pos(start_pos, pos);
        int flags = forward ? SEARCH_END : 0;

        /* Check again from the current cursor position,
         * since the next match might actually be only one char wide. */
        one_char = is_one_char(spats[last_idx].pat, false);

        /* Move to match, except for zero-width matches,
         * in which case, we are already on the next match. */
        if (one_char == 0)
            searchit(curwin, curbuf, pos, (forward ? FORWARD : BACKWARD),
                spats[last_idx].pat, 0L, flags | SEARCH_KEEP, RE_SEARCH, 0, null);

        if (!VIsual_active)
            COPY_pos(VIsual, start_pos);

        COPY_pos(curwin.w_cursor, pos);
        VIsual_active = true;
        VIsual_mode = 'v';

        if (VIsual_active)
        {
            redraw_curbuf_later(INVERTED);  /* update the inversion */
            if (p_sel[0].at(0) == (byte)'e')
            {
                /* Correction for exclusive selection depends on the direction. */
                if (forward && ltoreq(VIsual, curwin.w_cursor))
                    inc_cursor();
                else if (!forward && ltoreq(curwin.w_cursor, VIsual))
                    inc(VIsual);
            }
        }

        may_start_select('c');
        setmouse();
        /* Make sure the clipboard gets updated.  Needed because start and
         * end are still the same, and the selection needs to be owned. */
        clip_star.vmode = NUL;
        redraw_curbuf_later(INVERTED);
        showmode();

        return true;
    }

    /*
     * Check if the pattern is one character or zero-width.
     * If move is true, check from the beginning of the buffer, else from the current cursor position.
     * Returns true, false or -1 for failure.
     */
    /*private*/ static int is_one_char(Bytes pattern, boolean move)
    {
        int result = -1;

        boolean save_called_emsg = called_emsg;

        regmmatch_C regmatch = new regmmatch_C();
        if (search_regcomp(pattern, RE_SEARCH, RE_SEARCH, SEARCH_KEEP, regmatch) == false)
            return -1;

        pos_C pos = new pos_C();
        int flag = 0;
        /* move to match */
        if (!move)
        {
            COPY_pos(pos, curwin.w_cursor);
            /* accept a match at the cursor position */
            flag = SEARCH_START;
        }

        if (searchit(curwin, curbuf, pos, FORWARD, spats[last_idx].pat, 1, SEARCH_KEEP + flag, RE_SEARCH, 0, null) != 0)
        {
            /* Zero-width pattern should match somewhere,
             * then we can check if start and end are in the same position. */
            called_emsg = false;
            long nmatched = vim_regexec_multi(regmatch, curwin, curbuf, pos.lnum, 0, null);

            if (!called_emsg)
                result = (nmatched != 0
                    && regmatch.startpos[0].lnum == regmatch.endpos[0].lnum
                     && regmatch.startpos[0].col == regmatch.endpos[0].col) ? TRUE : FALSE;

            if (result == FALSE && 0 <= inc(pos) && pos.col == regmatch.endpos[0].col)
                result = TRUE;
        }

        called_emsg |= save_called_emsg;

        return result;
    }

    /*
     * return true if line 'lnum' is empty or has white chars only.
     */
    /*private*/ static boolean linewhite(long lnum)
    {
        Bytes p = skipwhite(ml_get(lnum));
        return (p.at(0) == NUL);
    }

    /*
     * memfile.c: Contains the functions for handling blocks of memory which can
     * be stored in a file.  This is the implementation of a sort of virtual memory.
     *
     * A memfile consists of a sequence of blocks.  The blocks numbered from 0
     * upwards have been assigned a place in the actual file.  The block number
     * is equal to the page number in the file.
     *
     * The blocks with negative numbers are currently in memory only.  They can be
     * assigned a place in the file when too much memory is being used.  At that
     * moment they get a new, positive, number.  A list is used for translation of
     * negative to positive numbers.
     *
     * The size of a block is a multiple of a page size, normally the page size of
     * the device the file is on. Most blocks are 1 page long.  A block of multiple
     * pages is used for a line that does not fit in a single page.
     *
     * Each block can be in memory and/or in a file.  The block stays in memory
     * as long as it is locked.  If it is no longer locked it can be swapped out to
     * the file.  It is only written to the file if it has been changed.
     *
     * Under normal operation the file is created when opening the memory file and
     * deleted when closing the memory file.  Only with recovery an existing memory
     * file is opened.
     */

    /*private*/ static final int MEMFILE_PAGE_SIZE = 4096;  /* default page size */

    /*
     * The functions for using a memfile:
     *
     * mf_open()        open a new or existing memfile
     * mf_close()       close a memfile
     * mf_new()         create a new block in a memfile and lock it
     * mf_get()         get an existing block and lock it
     * mf_put()         unlock a block, may be marked for writing
     * mf_free()        remove a block
     * mf_trans_del()   may translate negative to positive block number
     */

    /*private*/ static memfile_C mf_open()
    {
        memfile_C mfp = new memfile_C();

        mfp.mf_used_first = null;       /* used list is empty */
        mfp.mf_used_last = null;
        mf_hash_init(mfp.mf_hash);
        mf_hash_init(mfp.mf_trans);

        mfp.mf_blocknr_max = 0;         /* no file or empty file */
        mfp.mf_blocknr_min = -1;
        mfp.mf_neg_count = 0;

        return mfp;
    }

    /*private*/ static void mf_close(memfile_C mfp)
    {
        mf_hash_free(mfp.mf_hash);
        mf_hash_free(mfp.mf_trans);     /* free hashtable and its items */
    }

    /*
     * get a new block
     *
     *   negative: true if negative block number desired (data block)
     */
    /*private*/ static block_hdr_C mf_new(memfile_C mfp, boolean negative, Object data, int page_count)
    {
        block_hdr_C hp = mf_alloc_bhdr(mfp, data, page_count);

        /* Use mf_block_min for a negative number, mf_block_max for a positive number. */
        if (negative)
        {
            hp.bh_bnum(mfp.mf_blocknr_min--);
            mfp.mf_neg_count++;
        }
        else
        {
            hp.bh_bnum(mfp.mf_blocknr_max);
            mfp.mf_blocknr_max += page_count;
        }

        hp.bh_flags = BH_LOCKED | BH_DIRTY;     /* new block is always dirty */
        hp.bh_page_count = page_count;
        mf_ins_used(mfp, hp);
        mf_ins_hash(mfp, hp);

        return hp;
    }

    /*
     * Get existing block "nr" with "page_count" pages.
     *
     * Note: The caller should first check a negative nr with mf_trans_del().
     */
    /*private*/ static block_hdr_C mf_get(memfile_C mfp, long nr, int _page_count)
    {
        if (mfp.mf_blocknr_max <= nr || nr <= mfp.mf_blocknr_min)   /* doesn't exist */
            return null;

        block_hdr_C hp = mf_find_hash(mfp, nr);
        if (hp == null)                                 /* not in the hash list */
            return null;

        mf_rem_used(mfp, hp);           /* remove from list, insert in front below */
        mf_rem_hash(mfp, hp);

        hp.bh_flags |= BH_LOCKED;
        mf_ins_used(mfp, hp);           /* put in front of used list */
        mf_ins_hash(mfp, hp);           /* put in front of hash list */

        return hp;
    }

    /*
     * release the block *hp
     *
     *   dirty: Block must be written to file later
     *   infile: Block should be in file (needed for recovery)
     */
    /*private*/ static void mf_put(memfile_C mfp, block_hdr_C hp, boolean dirty, boolean infile)
    {
        byte flags = hp.bh_flags;

        if ((flags & BH_LOCKED) == 0)
            emsg(u8("E293: block was not locked"));
        flags &= ~BH_LOCKED;
        if (dirty)
            flags |= BH_DIRTY;
        hp.bh_flags = flags;
        if (infile)
            mf_trans_add(mfp, hp);      /* may translate negative in positive nr */
    }

    /*
     * block *hp is no longer in used, may put it in the free list of memfile *mfp
     */
    /*private*/ static void mf_free(memfile_C mfp, block_hdr_C hp)
    {
        mf_rem_hash(mfp, hp);       /* get *hp out of the hash list */
        mf_rem_used(mfp, hp);       /* get *hp out of the used list */
        if (hp.bh_bnum() < 0)
            mfp.mf_neg_count--;
    }

    /*
     * insert block *hp in front of hashlist of memfile *mfp
     */
    /*private*/ static void mf_ins_hash(memfile_C mfp, block_hdr_C hp)
    {
        mf_hash_add_item(mfp.mf_hash, hp.bh_hashitem);
    }

    /*
     * remove block *hp from hashlist of memfile list *mfp
     */
    /*private*/ static void mf_rem_hash(memfile_C mfp, block_hdr_C hp)
    {
        mf_hash_rem_item(mfp.mf_hash, hp.bh_hashitem);
    }

    /*
     * look in hash lists of memfile *mfp for block header with number 'nr'
     */
    /*private*/ static block_hdr_C mf_find_hash(memfile_C mfp, long nr)
    {
        mf_hashitem_C mhi = mf_hash_find(mfp.mf_hash, nr);

        return (mhi != null) ? (block_hdr_C)mhi.mhi_data : null;
    }

    /*
     * insert block *hp in front of used list of memfile *mfp
     */
    /*private*/ static void mf_ins_used(memfile_C mfp, block_hdr_C hp)
    {
        hp.bh_next = mfp.mf_used_first;
        mfp.mf_used_first = hp;
        hp.bh_prev = null;
        if (hp.bh_next == null)     /* list was empty, adjust last pointer */
            mfp.mf_used_last = hp;
        else
            hp.bh_next.bh_prev = hp;
    }

    /*
     * remove block *hp from used list of memfile *mfp
     */
    /*private*/ static void mf_rem_used(memfile_C mfp, block_hdr_C hp)
    {
        if (hp.bh_next == null)     /* last block in used list */
            mfp.mf_used_last = hp.bh_prev;
        else
            hp.bh_next.bh_prev = hp.bh_prev;
        if (hp.bh_prev == null)     /* first block in used list */
            mfp.mf_used_first = hp.bh_next;
        else
            hp.bh_prev.bh_next = hp.bh_next;
    }

    /*
     * Allocate a block header and a block of memory for it.
     */
    /*private*/ static block_hdr_C mf_alloc_bhdr(memfile_C _mfp, Object data, int page_count)
    {
        block_hdr_C hp = new block_hdr_C();

        hp.bh_data = data;
        hp.bh_page_count = page_count;

        return hp;
    }

    /*
     * Make block number for *hp positive and add it to the translation list.
     */
    /*private*/ static void mf_trans_add(memfile_C mfp, block_hdr_C hp)
    {
        if (hp.bh_bnum() < 0)
        {
            /* Get a new number for the block. */
            long new_bnum = mfp.mf_blocknr_max;
            mfp.mf_blocknr_max += hp.bh_page_count;

            nr_trans_C np = new nr_trans_C();

            np.nt_old_bnum(hp.bh_bnum());        /* adjust number */
            np.nt_new_bnum = new_bnum;

            mf_rem_hash(mfp, hp);               /* remove from old hash list */
            hp.bh_bnum(new_bnum);
            mf_ins_hash(mfp, hp);               /* insert in new hash list */

            /* Insert "np" into "mf_trans" hashtable with key "np.nt_old_bnum". */
            mf_hash_add_item(mfp.mf_trans, np.nt_hashitem);
        }
    }

    /*
     * Lookup a translation from the trans lists and delete the entry.
     *
     * Return the positive new number when found, the old number when not found.
     */
    /*private*/ static long mf_trans_del(memfile_C mfp, long old_nr)
    {
        mf_hashitem_C mhi = mf_hash_find(mfp.mf_trans, old_nr);
        nr_trans_C np = (mhi != null) ? (nr_trans_C)mhi.mhi_data : null;

        if (np == null)             /* not found */
            return old_nr;

        mfp.mf_neg_count--;
        long new_bnum = np.nt_new_bnum;

        /* remove entry from the trans list */
        mf_hash_rem_item(mfp.mf_trans, np.nt_hashitem);

        return new_bnum;
    }

    /*
     * Implementation of mf_hashtab_C follows.
     */

    /*
     * The number of buckets in the hashtable is increased by a factor of MHT_GROWTH_FACTOR
     * when the average number of items per bucket exceeds (2 ^ MHT_LOG_LOAD_FACTOR).
     */
    /*private*/ static final int MHT_LOG_LOAD_FACTOR = 6;
    /*private*/ static final int MHT_GROWTH_FACTOR   = 2;   /* must be a power of two */

    /*
     * Initialize an empty hash table.
     */
    /*private*/ static void mf_hash_init(mf_hashtab_C mht)
    {
        ZER0_mf_hashtab(mht);
        mht.mht_buckets = ARRAY_mf_hashitem(MHT_INIT_SIZE);
        mht.mht_mask = MHT_INIT_SIZE - 1;
    }

    /*
     * Free the array of a hash table.  Does not free the items it contains!
     * The hash table must not be used again without another mf_hash_init() call.
     */
    /*private*/ static void mf_hash_free(mf_hashtab_C mht)
    {
        mht.mht_buckets = null;
    }

    /*
     * Find "key" in hashtable "mht".
     * Returns a pointer to a mf_hashitem_C or null if the item was not found.
     */
    /*private*/ static mf_hashitem_C mf_hash_find(mf_hashtab_C mht, long key)
    {
        mf_hashitem_C mhi = mht.mht_buckets[(int)(key & mht.mht_mask)];

        while (mhi != null && mhi.mhi_key != key)
            mhi = mhi.mhi_next;

        return mhi;
    }

    /*
     * Add item "mhi" to hashtable "mht".
     * "mhi" must not be null.
     */
    /*private*/ static void mf_hash_add_item(mf_hashtab_C mht, mf_hashitem_C mhi)
    {
        int i = (int)(mhi.mhi_key & mht.mht_mask);

        mhi.mhi_next = mht.mht_buckets[i];
        mhi.mhi_prev = null;
        if (mhi.mhi_next != null)
            mhi.mhi_next.mhi_prev = mhi;
        mht.mht_buckets[i] = mhi;

        mht.mht_count++;

        /*
         * Grow hashtable when we have more than (2 ^ MHT_LOG_LOAD_FACTOR) items per bucket on average.
         */
        if (mht.mht_mask < (mht.mht_count >>> MHT_LOG_LOAD_FACTOR))
            mf_hash_grow(mht);
    }

    /*
     * Remove item "mhi" from hashtable "mht".
     * "mhi" must not be null and must have been inserted into "mht".
     */
    /*private*/ static void mf_hash_rem_item(mf_hashtab_C mht, mf_hashitem_C mhi)
    {
        if (mhi.mhi_prev == null)
            mht.mht_buckets[(int)(mhi.mhi_key & mht.mht_mask)] = mhi.mhi_next;
        else
            mhi.mhi_prev.mhi_next = mhi.mhi_next;

        if (mhi.mhi_next != null)
            mhi.mhi_next.mhi_prev = mhi.mhi_prev;

        mht.mht_count--;

        /* We could shrink the table here, but it typically takes little memory, so why bother? */
    }

    /*
     * Increase number of buckets in the hashtable by MHT_GROWTH_FACTOR and rehash items.
     */
    /*private*/ static void mf_hash_grow(mf_hashtab_C mht)
    {
        mf_hashitem_C[] buckets = new mf_hashitem_C[(mht.mht_mask + 1) * MHT_GROWTH_FACTOR];

        int shift = 0;
        while ((mht.mht_mask >>> shift) != 0)
            shift++;

        for (int i = 0; i <= mht.mht_mask; i++)
        {
            /*
             * Traverse the items in the i-th original bucket and move them into MHT_GROWTH_FACTOR new buckets,
             * preserving their relative order within each new bucket.  Preserving the order is important
             * because mf_get() tries to keep most recently used items at the front of each bucket.
             *
             * Here we strongly rely on the fact the hashes are computed modulo a power of two.
             */
            mf_hashitem_C[] tails = new mf_hashitem_C[MHT_GROWTH_FACTOR];

            for (mf_hashitem_C mhi = mht.mht_buckets[i]; mhi != null; mhi = mhi.mhi_next)
            {
                int j = (int)((mhi.mhi_key >>> shift) & (MHT_GROWTH_FACTOR - 1));
                if (tails[j] == null)
                {
                    buckets[i + (j << shift)] = mhi;
                    tails[j] = mhi;
                    mhi.mhi_prev = null;
                }
                else
                {
                    tails[j].mhi_next = mhi;
                    mhi.mhi_prev = tails[j];
                    tails[j] = mhi;
                }
            }

            for (int j = 0; j < MHT_GROWTH_FACTOR; j++)
                if (tails[j] != null)
                    tails[j].mhi_next = null;
        }

        mht.mht_buckets = buckets;
        mht.mht_mask = (mht.mht_mask + 1) * MHT_GROWTH_FACTOR - 1;
    }

    /*
     * memline.c: Contains the functions for appending, deleting and changing the
     * text lines.  The memfile functions are used to store the information in
     * blocks of memory, backed up by a file.  The structure of the information is
     * a tree.  The root of the tree is a pointer block.  The leaves of the tree
     * are data blocks.  In between may be several layers of pointer blocks,
     * forming branches.
     *
     * Three types of blocks are used:
     * - Block nr 0 contains information for recovery.
     * - Pointer blocks contain list of pointers to other blocks.
     * - Data blocks contain the actual text.
     *
     * Block nr 0 contains the block0 structure (see below).
     *
     * Block nr 1 is the first pointer block.  It is the root of the tree.
     * Other pointer blocks are branches.
     *
     *  If a line is too big to fit in a single page, the block containing that
     *  line is made big enough to hold the line.  It may span several pages.
     *  Otherwise all blocks are one page.
     *
     *  A data block that was filled when starting to edit a file and was not
     *  changed since then, can have a negative block number.  This means that it
     *  has not yet been assigned a place in the file.  When recovering, the lines
     *  in this data block can be read from the original file.  When the block is
     *  changed (lines appended/deleted/changed) or when it is flushed it gets a
     *  positive number.  Use mf_trans_del() to get the new number, before calling
     *  mf_get().
     */

    /*private*/ static final short
        B0_ID   = ('b' << 8) + '0',             /* block 0 id */
        DATA_ID = ('d' << 8) + 'a',             /* data block id */
        PTR_ID  = ('p' << 8) + 't';             /* pointer block id */

    /*
     * Block zero holds all info about the swap file.
     */
    /*private*/ static final class zero_block_C
    {
        short       b0_id;              /* ID for block 0: B0_ID */

        /*private*/ zero_block_C()
        {
            b0_id = B0_ID;
        }
    }

    /*
     * Pointer to a block, used in a pointer block.
     */
    /*private*/ static final class ptr_entry_C
    {
        long        pe_bnum;            /* block number */
        long        pe_line_count;      /* number of lines in this branch */
        int         pe_page_count;      /* number of pages in block pe_bnum */

        /*private*/ ptr_entry_C()
        {
        }
    }

    /*private*/ static void COPY_ptr_entry(ptr_entry_C pe1, ptr_entry_C pe0)
    {
        pe1.pe_bnum = pe0.pe_bnum;
        pe1.pe_line_count = pe0.pe_line_count;
        pe1.pe_page_count = pe0.pe_page_count;
    }

    /*private*/ static ptr_entry_C[] ARRAY_ptr_entry(int n)
    {
        ptr_entry_C[] a = new ptr_entry_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new ptr_entry_C();
        return a;
    }

    /*private*/ static void COPY__ptr_entry(ptr_entry_C[] a1, ptr_entry_C[] a0, int n)
    {
        for (int i = 0; i < n; i++)
            COPY_ptr_entry(a1[i], a0[i]);
    }

    /*
     * A pointer block contains a list of branches in the tree.
     */
    /*private*/ static final class ptr_block_C
    {
        short       pb_id;              /* ID for pointer block: PTR_ID */
        int         pb_count;           /* number of pointers in this block */
        int         pb_count_max;       /* maximum value for pb_count */
        ptr_entry_C[] pb_pointer;       /* array of pointers to blocks */

        /*private*/ ptr_block_C()
        {
            pb_id = PTR_ID;
            pb_count = 0;
            pb_count_max = MEMFILE_PAGE_SIZE / 8;
            pb_pointer = ARRAY_ptr_entry(pb_count_max);
        }
    }

    /*private*/ static void COPY_ptr_block(ptr_block_C pb1, ptr_block_C pb0)
    {
        pb1.pb_id = pb0.pb_id;
        pb1.pb_count = pb0.pb_count;
        pb1.pb_count_max = pb0.pb_count_max;
        COPY__ptr_entry(pb1.pb_pointer, pb0.pb_pointer, pb0.pb_count_max);
    }

    /*
     * A data block is a leaf in the tree.
     *
     * The text of the lines is at the end of the block.  The text of the first line
     * in the block is put at the end, the text of the second line in front of it,
     * etc.  Thus the order of the lines is the opposite of the line number.
     */
    /*private*/ static final class data_block_C
    {
        short       db_id;              /* ID for data block: DATA_ID */
        int         db_free;            /* free space available */
        int         db_txt_start;       /* byte where text starts */
        int         db_txt_end;         /* byte just after data block */
        int         db_line_count;      /* number of lines in this block */
        int[]       db_index;           /* index for start of line (*union* db_text)
                                         * followed by empty space upto db_txt_start */
        Bytes       db_text;            /* followed by the text in the lines until end of page */

        /*private*/ data_block_C(int page_count)
        {
            db_id = DATA_ID;
            db_free = db_txt_start = db_txt_end = page_count * MEMFILE_PAGE_SIZE;
            db_line_count = 0;
            db_index = new int[page_count * MEMFILE_PAGE_SIZE / INDEX_SIZE];   /* sic! */
            db_text = new Bytes(page_count * MEMFILE_PAGE_SIZE);   /* sic! */
        }
    }

    /*
     * The low bits of db_index hold the actual index.
     * The topmost bit is used for the global command to be able to mark a line.
     * This method is not clean, but otherwise there would be at least one extra byte used for each line.
     * The mark has to be in this place to keep it with the correct line
     * when other lines are inserted or deleted.
     */
    /*private*/ static final int INDEX_SIZE      = 4;     /* size of one db_index entry */

    /*private*/ static final int DB_MARKED       = (1 << ((INDEX_SIZE * 8) - 1));
    /*private*/ static final int DB_INDEX_MASK   = ~DB_MARKED;

    /*private*/ static final int STACK_INCR      = 5;       /* nr of entries added to ml_stack at a time */

    /*
     * The line number where the first mark may be is remembered.
     * If it is 0 there are no marks at all.
     * (always used for the current buffer only, no buffer change possible while executing a global command).
     */
    /*private*/ static long lowest_marked;

    /*
     * arguments for ml_find_line()
     */
    /*private*/ static final int ML_DELETE       = 0x11;        /* delete line */
    /*private*/ static final int ML_INSERT       = 0x12;        /* insert line */
    /*private*/ static final int ML_FIND         = 0x13;        /* just find the line */
    /*private*/ static final int ML_FLUSH        = 0x02;        /* flush locked block */

    /*private*/ static boolean ml_simple(int action)
    {
        return ((action & 0x10) != 0);         /* DEL, INS or FIND */
    }

    /*
     * Open a new memline for "buf".
     *
     * Return false for failure, true otherwise.
     */
    /*private*/ static boolean ml_open(buffer_C buf)
    {
        /*
         * init fields in memline struct
         */
        buf.b_ml.ml_stack_size = 0;     /* no stack yet */
        buf.b_ml.ml_stack = null;       /* no stack yet */
        buf.b_ml.ml_stack_top = 0;      /* nothing in the stack */
        buf.b_ml.ml_locked = null;      /* no cached block */
        buf.b_ml.ml_line_lnum = 0;      /* no cached line */
        buf.b_ml.ml_chunksize = null;

        /*
         * Open the memfile.  No swap file is created yet.
         */
        memfile_C mfp = mf_open();

        buf.b_ml.ml_mfp = mfp;
        buf.b_ml.ml_flags = ML_EMPTY;
        buf.b_ml.ml_line_count = 1;
        curwin.w_nrwidth_line_count = 0;

        block_hdr_C hp;

        error:
        {
            /*
             * fill block0 struct and write page 0
             */
            hp = ml_new_zero(mfp);
            if (hp.bh_bnum() != 0)
            {
                emsg(u8("E298: Didn't get block nr 0?"));
                break error;
            }

            set_mtime(buf);

            /*
             * Always sync block number 0 to disk.
             * Only works when there's a swapfile, otherwise it's done when the file is created.
             */
            mf_put(mfp, hp, true, false);

            /*
             * Fill in root pointer block and write page 1.
             */
            hp = ml_new_ptr(mfp);
            if (hp.bh_bnum() != 1)
            {
                emsg(u8("E298: Didn't get block nr 1?"));
                break error;
            }

            ptr_block_C pp = (ptr_block_C)hp.bh_data;
            pp.pb_count = 1;
            pp.pb_pointer[0].pe_bnum = 2;
            pp.pb_pointer[0].pe_page_count = 1;
            pp.pb_pointer[0].pe_line_count = 1; /* line count after insertion */
            mf_put(mfp, hp, true, false);

            /*
             * Allocate first data block and create an empty line 1.
             */
            hp = ml_new_data(mfp, false, 1);
            if (hp.bh_bnum() != 2)
            {
                emsg(u8("E298: Didn't get block nr 2?"));
                break error;
            }

            data_block_C dp = (data_block_C)hp.bh_data;

            dp.db_index[0] = --dp.db_txt_start;     /* at end of block */
            dp.db_free -= 1 + INDEX_SIZE;
            dp.db_line_count = 1;
            dp.db_text.be(dp.db_txt_start, NUL);      /* empty line */

            return true;
        }

        if (hp != null)
            mf_put(mfp, hp, false, false);
        mf_close(mfp);

        buf.b_ml.ml_mfp = null;

        return false;
    }

    /*
     * Close memline for buffer 'buf'.
     */
    /*private*/ static void ml_close(buffer_C buf)
    {
        if (buf.b_ml.ml_mfp != null)
        {
            mf_close(buf.b_ml.ml_mfp);
            if (buf.b_ml.ml_line_lnum != 0 && (buf.b_ml.ml_flags & ML_LINE_DIRTY) != 0)
                buf.b_ml.ml_line_ptr = null;
            buf.b_ml.ml_stack = null;
            buf.b_ml.ml_chunksize = null;
            buf.b_ml.ml_mfp = null;

            /* Reset the "recovered" flag,
             * give the ATTENTION prompt the next time this buffer is loaded. */
            buf.b_flags &= ~BF_RECOVERED;
        }
    }

    /*
     * Close all existing memlines and memfiles.
     * Only used when exiting.
     * But don't delete files that were ":preserve"d when we are POSIX compatible.
     */
    /*private*/ static void ml_close_all()
    {
        for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
            ml_close(buf);
    }

    /*
     * Update the timestamp in the .swp file.
     * Used when the file has been written.
     */
    /*private*/ static void ml_timestamp(buffer_C buf)
    {
        memfile_C mfp = buf.b_ml.ml_mfp;
        if (mfp == null)
            return;

        block_hdr_C hp = mf_get(mfp, 0, 1);
        if (hp == null)
            return;

        zero_block_C b0p = (zero_block_C)hp.bh_data;
        if (b0p.b0_id != B0_ID)
        {
            emsg(u8("E304: Didn't get block 0??"));
            return;
        }

        set_mtime(buf);

        mf_put(mfp, hp, true, false);
    }

    /*
     * Write timestamp into block 0.
     * Also set buf.b_mtime.
     */
    /*private*/ static void set_mtime(buffer_C buf)
    {
        if (buf.b_ffname != null)
        {
            stat_C st = new stat_C();
            if (0 <= libC.stat(buf.b_ffname, st))
            {
                buf_store_time(buf, st);
                buf.b_mtime_read = buf.b_mtime;
            }
            else
            {
                buf.b_mtime = 0;
                buf.b_mtime_read = 0;
                buf.b_orig_size = 0;
                buf.b_orig_mode = 0;
            }
        }
    }

    /*
     * NOTE: The pointer returned by the ml_get_*() functions only remains valid until the next call!
     *  line1 = ml_get(1);
     *  line2 = ml_get(2);  // line1 is now invalid!
     * Make a copy of the line if necessary.
     */

    /*
     * Return a pointer to a (read-only copy of a) line.
     *
     * On failure an error message is given and ioBuff is returned
     * (to avoid having to check for error everywhere).
     */
    /*private*/ static Bytes ml_get(long lnum)
    {
        return ml_get_buf(curbuf, lnum, false);
    }

    /*
     * Return pointer to position "pos".
     */
    /*private*/ static Bytes ml_get_pos(pos_C pos)
    {
        return ml_get_buf(curbuf, pos.lnum, false).plus(pos.col);
    }

    /*
     * Return pointer to cursor line.
     */
    /*private*/ static Bytes ml_get_curline()
    {
        return ml_get_buf(curbuf, curwin.w_cursor.lnum, false);
    }

    /*
     * Return pointer to cursor position.
     */
    /*private*/ static Bytes ml_get_cursor()
    {
        return ml_get_buf(curbuf, curwin.w_cursor.lnum, false).plus(curwin.w_cursor.col);
    }

    /*private*/ static int _4_recurse;

    /*
     * Return a pointer to a line in a specific buffer
     *
     * "will_change": if true mark the buffer dirty (chars in the line will be changed)
     */
    /*private*/ static Bytes ml_get_buf(buffer_C buf, long lnum, boolean will_change)
        /* will_change: line will be changed */
    {
        if (buf.b_ml.ml_line_count < lnum)  /* invalid line number */
        {
            if (_4_recurse == 0)
            {
                /* Avoid giving this message for a recursive call,
                 * may happen when the GUI redraws part of the text. */
                _4_recurse++;
                emsgn(u8("E315: ml_get: invalid lnum: %ld"), lnum);
                --_4_recurse;
            }

            STRCPY(ioBuff, u8("???"));
            return ioBuff;
        }
        if (lnum <= 0)                      /* pretend line 0 is line 1 */
            lnum = 1;

        if (buf.b_ml.ml_mfp == null)        /* there are no lines */
            return u8("");

        /*
         * See if it is the same line as requested last time.
         * Otherwise may need to flush last used line.
         * Don't use the last used line when 'swapfile' is reset, need to load all blocks.
         */
        if (buf.b_ml.ml_line_lnum != lnum)
        {
            ml_flush_line(buf);

            /*
             * Find the data block containing the line.
             * This also fills the stack with the blocks from the root to the data
             * block and releases any locked block.
             */
            block_hdr_C hp = ml_find_line(buf, lnum, ML_FIND);
            if (hp == null)
            {
                if (_4_recurse == 0)
                {
                    /* Avoid giving this message for a recursive call,
                     * may happen when the GUI redraws part of the text. */
                    _4_recurse++;
                    emsgn(u8("E316: ml_get: cannot find line %ld"), lnum);
                    --_4_recurse;
                }

                STRCPY(ioBuff, u8("???"));
                return ioBuff;
            }

            data_block_C dp = (data_block_C)hp.bh_data;

            buf.b_ml.ml_line_ptr = dp.db_text.plus(dp.db_index[(int)(lnum - buf.b_ml.ml_locked_low)] & DB_INDEX_MASK);
            buf.b_ml.ml_line_lnum = lnum;
            buf.b_ml.ml_flags &= ~ML_LINE_DIRTY;
        }
        if (will_change)
            buf.b_ml.ml_flags |= (ML_LOCKED_DIRTY | ML_LOCKED_POS);

        return buf.b_ml.ml_line_ptr;
    }

    /*
     * Check if a line that was just obtained by a call to ml_get() is in allocated memory.
     */
    /*private*/ static boolean ml_line_alloced()
    {
        return ((curbuf.b_ml.ml_flags & ML_LINE_DIRTY) != 0);
    }

    /*
     * Append a line after lnum (may be 0 to insert a line in front of the file).
     * "line" does not need to be allocated, but can't be another line in a buffer,
     * unlocking may make it invalid.
     *
     *   newfile: true when starting to edit a new file
     *
     * Check: The caller of this function should probably also call appended_lines().
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean ml_append(long lnum, Bytes line, int len, boolean newfile)
        /* lnum: append after this line (can be 0) */
        /* line: text of the new line */
        /* len: length of new line, including NUL, or 0 */
        /* newfile: flag, see above */
    {
        /* When starting up, we might still need to create the memfile. */
        if (curbuf.b_ml.ml_mfp == null && open_buffer(false, null, 0) == false)
            return false;

        if (curbuf.b_ml.ml_line_lnum != 0)
            ml_flush_line(curbuf);

        return ml_append_int(curbuf, lnum, line, len, newfile, false);
    }

    /*private*/ static boolean ml_append_int(buffer_C buf, long lnum, Bytes line, int len, boolean newfile, boolean mark)
        /* lnum: append after this line (can be 0) */
        /* line: text of the new line */
        /* len: length of line, including NUL, or 0 */
        /* newfile: flag, see above */
        /* mark: mark the new line */
    {
        if (buf.b_ml.ml_line_count < lnum || buf.b_ml.ml_mfp == null) /* lnum out of range */
            return false;

        if (lowest_marked != 0 && lnum < lowest_marked)
            lowest_marked = lnum + 1;

        if (len == 0)
            len = strlen(line) + 1;                /* space needed for the text */
        int space_needed = len + INDEX_SIZE;            /* space needed for new line (text + index) */

        memfile_C mfp = buf.b_ml.ml_mfp;

        /*
         * Find the data block containing the previous line.
         * This also fills the stack with the blocks from the root to the data block.
         * This also releases any locked block.
         */
        block_hdr_C hp = ml_find_line(buf, (lnum == 0) ? 1 : lnum, ML_INSERT);
        if (hp == null)
            return false;

        buf.b_ml.ml_flags &= ~ML_EMPTY;

        int db_idx;                                         /* index for lnum in data block */
        if (lnum == 0)                                      /* got line one instead, correct db_idx */
            db_idx = -1;                                    /* careful, it is negative! */
        else
            db_idx = (int)(lnum - buf.b_ml.ml_locked_low);

        int line_count;                                     /* number of indexes in current block */
        /* get line count before the insertion */
        line_count = (int)(buf.b_ml.ml_locked_high - buf.b_ml.ml_locked_low);

        data_block_C dp = (data_block_C)hp.bh_data;

        /*
         * If
         * - there is not enough room in the current block
         * - appending to the last line in the block
         * - not appending to the last line in the file
         * insert in front of the next block.
         */
        if (dp.db_free < space_needed && db_idx == line_count - 1 && lnum < buf.b_ml.ml_line_count)
        {
            /*
             * Now that the line is not going to be inserted in the block that we expected,
             * the line count has to be adjusted in the pointer blocks by using ml_locked_lineadd.
             */
            --buf.b_ml.ml_locked_lineadd;
            --buf.b_ml.ml_locked_high;

            hp = ml_find_line(buf, lnum + 1, ML_INSERT);
            if (hp == null)
                return false;

            db_idx = -1;                                    /* careful, it is negative! */
            /* get line count before the insertion */
            line_count = (int)(buf.b_ml.ml_locked_high - buf.b_ml.ml_locked_low);

            dp = (data_block_C)hp.bh_data;
        }

        buf.b_ml.ml_line_count++;

        if (space_needed <= dp.db_free)                     /* enough room in data block */
        {
            /*
             * Insert new line in existing data block, or in data block allocated above.
             */
            dp.db_txt_start -= len;
            dp.db_free -= space_needed;
            dp.db_line_count++;

            /*
             * move the text of the lines that follow to the front
             * adjust the indexes of the lines that follow
             */
            if (db_idx + 1 < line_count)                    /* if there are following lines */
            {
                /*
                 * 'over' is the start of the previous line.
                 * This will become the character just after the new line.
                 */
                int from = dp.db_txt_start + len;
                int over = (db_idx < 0) ? dp.db_txt_end : (dp.db_index[db_idx] & DB_INDEX_MASK);
                BCOPY(dp.db_text, dp.db_txt_start, dp.db_text, from, over - from);
                for (int i = line_count; db_idx < --i; )
                    dp.db_index[i + 1] = dp.db_index[i] - len;
                dp.db_index[db_idx + 1] = over - len;
            }
            else                                            /* add line at the end */
                dp.db_index[db_idx + 1] = dp.db_txt_start;

            /*
             * copy the text into the block
             */
            BCOPY(dp.db_text, dp.db_index[db_idx + 1], line, 0, len);
            if (mark)
                dp.db_index[db_idx + 1] |= DB_MARKED;

            /*
             * Mark the block dirty.
             */
            buf.b_ml.ml_flags |= ML_LOCKED_DIRTY;
            if (!newfile)
                buf.b_ml.ml_flags |= ML_LOCKED_POS;
        }
        else                                                /* not enough space in data block */
        {
            /*
             * If there is not enough room we have to create a new data block
             * and copy some lines into it.  Then we have to insert an entry
             * in the pointer block.  If this pointer block also is full,
             * we go up another block, and so on, up to the root if necessary.
             * The line counts in the pointer blocks have already been adjusted
             * by ml_find_line().
             */
            int lines_moved;
            int data_moved = 0;
            int total_moved = 0;
            boolean in_left;

            /*
             * We are going to allocate a new data block.  Depending on the
             * situation it will be put to the left or right of the existing
             * block.  If possible we put the new line in the left block and move
             * the lines after it to the right block.  Otherwise the new line is
             * also put in the right block.  This method is more efficient when
             * inserting a lot of lines at one place.
             */
            if (db_idx < 0)                         /* left block is new, right block is existing */
            {
                lines_moved = 0;
                in_left = true;                     /* space_needed does not change */
            }
            else                                    /* left block is existing, right block is new */
            {
                lines_moved = line_count - db_idx - 1;
                if (lines_moved == 0)               /* put new line in right block */
                    in_left = false;                /* space_needed does not change */
                else
                {
                    data_moved = (dp.db_index[db_idx] & DB_INDEX_MASK) - dp.db_txt_start;
                    total_moved = data_moved + lines_moved * INDEX_SIZE;
                    if (space_needed <= dp.db_free + total_moved)
                    {
                        in_left = true;             /* put new line in left block */
                        space_needed = total_moved;
                    }
                    else
                    {
                        in_left = false;            /* put new line in right block */
                        space_needed += total_moved;
                    }
                }
            }

            int page_count = (space_needed + MEMFILE_PAGE_SIZE - 1) / MEMFILE_PAGE_SIZE;
            block_hdr_C hp_new = ml_new_data(mfp, newfile, page_count);

            block_hdr_C hp_left, hp_right;
            int line_count_left, line_count_right;
            if (db_idx < 0)         /* left block is new */
            {
                hp_left = hp_new;
                hp_right = hp;
                line_count_left = 0;
                line_count_right = line_count;
            }
            else                    /* right block is new */
            {
                hp_left = hp;
                hp_right = hp_new;
                line_count_left = line_count;
                line_count_right = 0;
            }
            data_block_C dp_left = (data_block_C)hp_left.bh_data;
            data_block_C dp_right = (data_block_C)hp_right.bh_data;
            long bnum_left = hp_left.bh_bnum();
            long bnum_right = hp_right.bh_bnum();
            int page_count_left = hp_left.bh_page_count;
            int page_count_right = hp_right.bh_page_count;

            /*
             * May move the new line into the right/new block.
             */
            if (!in_left)
            {
                dp_right.db_txt_start -= len;
                dp_right.db_free -= len + INDEX_SIZE;
                dp_right.db_index[0] = dp_right.db_txt_start;
                if (mark)
                    dp_right.db_index[0] |= DB_MARKED;

                BCOPY(dp_right.db_text, dp_right.db_txt_start, line, 0, len);
                line_count_right++;
            }
            /*
             * may move lines from the left/old block to the right/new one.
             */
            if (lines_moved != 0)
            {
                dp_right.db_txt_start -= data_moved;
                dp_right.db_free -= total_moved;
                BCOPY(dp_right.db_text, dp_right.db_txt_start, dp_left.db_text, dp_left.db_txt_start, data_moved);
                int offset = dp_right.db_txt_start - dp_left.db_txt_start;
                dp_left.db_txt_start += data_moved;
                dp_left.db_free += total_moved;

                /*
                 * update indexes in the new block
                 */
                for (int from = db_idx + 1, to = line_count_right; from < line_count_left; from++, to++)
                    dp_right.db_index[to] = dp.db_index[from] + offset;
                line_count_right += lines_moved;
                line_count_left -= lines_moved;
            }

            /*
             * May move the new line into the left (old or new) block.
             */
            if (in_left)
            {
                dp_left.db_txt_start -= len;
                dp_left.db_free -= len + INDEX_SIZE;
                dp_left.db_index[line_count_left] = dp_left.db_txt_start;
                if (mark)
                    dp_left.db_index[line_count_left] |= DB_MARKED;
                BCOPY(dp_left.db_text, dp_left.db_txt_start, line, 0, len);
                line_count_left++;
            }

            long lnum_left, lnum_right;
            if (db_idx < 0)         /* left block is new */
            {
                lnum_left = lnum + 1;
                lnum_right = 0;
            }
            else                    /* right block is new */
            {
                lnum_left = 0;
                if (in_left)
                    lnum_right = lnum + 2;
                else
                    lnum_right = lnum + 1;
            }
            dp_left.db_line_count = line_count_left;
            dp_right.db_line_count = line_count_right;

            /*
             * release the two data blocks
             * The new one (hp_new) already has a correct blocknumber.
             * The old one (hp, in ml_locked) gets a positive blocknumber if
             * we changed it and we are not editing a new file.
             */
            if (lines_moved != 0 || in_left)
                buf.b_ml.ml_flags |= ML_LOCKED_DIRTY;
            if (!newfile && 0 <= db_idx && in_left)
                buf.b_ml.ml_flags |= ML_LOCKED_POS;
            mf_put(mfp, hp_new, true, false);

            /*
             * flush the old data block
             * set ml_locked_lineadd to 0, because the updating of the
             * pointer blocks is done below
             */
            int lineadd = buf.b_ml.ml_locked_lineadd;
            buf.b_ml.ml_locked_lineadd = 0;
            ml_find_line(buf, 0, ML_FLUSH);             /* flush data block */

            /*
             * update pointer blocks for the new data block
             */
            int stack_idx;
            for (stack_idx = buf.b_ml.ml_stack_top - 1; 0 <= stack_idx; --stack_idx)
            {
                infoptr_C ip = buf.b_ml.ml_stack[stack_idx];
                int pb_idx = ip.ip_index;
                if ((hp = mf_get(mfp, ip.ip_bnum, 1)) == null)
                    return false;

                ptr_block_C pp = (ptr_block_C)hp.bh_data; /* must be pointer block */
                if (pp.pb_id != PTR_ID)
                {
                    emsg(u8("E317: pointer block id wrong 3"));
                    mf_put(mfp, hp, false, false);
                    return false;
                }

                /*
                 * TODO: If the pointer block is full and we are adding at the end,
                 * try to insert in front of the next block.
                 */
                /* block not full, add one entry */
                if (pp.pb_count < pp.pb_count_max)
                {
                    for (int i = pp.pb_count; pb_idx + 1 <= --i; )
                        COPY_ptr_entry(pp.pb_pointer[i + 1], pp.pb_pointer[i]);
                    pp.pb_count++;
                    pp.pb_pointer[pb_idx].pe_line_count = line_count_left;
                    pp.pb_pointer[pb_idx].pe_bnum = bnum_left;
                    pp.pb_pointer[pb_idx].pe_page_count = page_count_left;
                    pp.pb_pointer[pb_idx + 1].pe_line_count = line_count_right;
                    pp.pb_pointer[pb_idx + 1].pe_bnum = bnum_right;
                    pp.pb_pointer[pb_idx + 1].pe_page_count = page_count_right;

                    mf_put(mfp, hp, true, false);
                    buf.b_ml.ml_stack_top = stack_idx + 1;  /* truncate stack */

                    if (lineadd != 0)
                    {
                        --buf.b_ml.ml_stack_top;
                        /* fix line count for rest of blocks in the stack */
                        ml_lineadd(buf, lineadd);
                                                            /* fix stack itself */
                        buf.b_ml.ml_stack[buf.b_ml.ml_stack_top].ip_high += lineadd;
                        buf.b_ml.ml_stack_top++;
                    }

                    /*
                     * We are finished, break the loop here.
                     */
                    break;
                }
                else                        /* pointer block full */
                {
                    ptr_block_C pp_new;
                    /*
                     * split the pointer block
                     * allocate a new pointer block
                     * move some of the pointer into the new block
                     * prepare for updating the parent block
                     */
                    for ( ; ; )             /* do this twice when splitting block 1 */
                    {
                        hp_new = ml_new_ptr(mfp);
                        pp_new = (ptr_block_C)hp_new.bh_data;

                        if (hp.bh_bnum() != 1)
                            break;

                        /*
                         * if block 1 becomes full the tree is given an extra level
                         * The pointers from block 1 are moved into the new block.
                         * block 1 is updated to point to the new block
                         * then continue to split the new block
                         */
                        COPY_ptr_block(pp_new, pp);
                        pp.pb_count = 1;
                        pp.pb_pointer[0].pe_bnum = hp_new.bh_bnum();
                        pp.pb_pointer[0].pe_line_count = buf.b_ml.ml_line_count;
                        pp.pb_pointer[0].pe_page_count = 1;
                        mf_put(mfp, hp, true, false);           /* release block 1 */
                        hp = hp_new;                            /* new block is to be split */
                        pp = pp_new;
                        ip.ip_index = 0;
                        stack_idx++;                            /* do block 1 again later */
                    }

                    /*
                     * Move the pointers after the current one to the new block.
                     * If there are none, the new entry will be in the new block.
                     */
                    total_moved = pp.pb_count - pb_idx - 1;
                    if (total_moved != 0)
                    {
                        for (int i = 0; i < total_moved; i++)
                            COPY_ptr_entry(pp_new.pb_pointer[i], pp.pb_pointer[pb_idx + 1 + i]);
                        pp_new.pb_count = total_moved;
                        pp.pb_count -= total_moved - 1;
                        pp.pb_pointer[pb_idx + 1].pe_bnum = bnum_right;
                        pp.pb_pointer[pb_idx + 1].pe_line_count = line_count_right;
                        pp.pb_pointer[pb_idx + 1].pe_page_count = page_count_right;
                    }
                    else
                    {
                        pp_new.pb_count = 1;
                        pp_new.pb_pointer[0].pe_bnum = bnum_right;
                        pp_new.pb_pointer[0].pe_line_count = line_count_right;
                        pp_new.pb_pointer[0].pe_page_count = page_count_right;
                    }
                    pp.pb_pointer[pb_idx].pe_bnum = bnum_left;
                    pp.pb_pointer[pb_idx].pe_line_count = line_count_left;
                    pp.pb_pointer[pb_idx].pe_page_count = page_count_left;
                    lnum_left = 0;
                    lnum_right = 0;

                    /*
                     * recompute line counts
                     */
                    line_count_right = 0;
                    for (int i = 0; i < pp_new.pb_count; i++)
                        line_count_right += pp_new.pb_pointer[i].pe_line_count;
                    line_count_left = 0;
                    for (int i = 0; i < pp.pb_count; i++)
                        line_count_left += pp.pb_pointer[i].pe_line_count;

                    bnum_left = hp.bh_bnum();
                    bnum_right = hp_new.bh_bnum();
                    page_count_left = 1;
                    page_count_right = 1;
                    mf_put(mfp, hp, true, false);
                    mf_put(mfp, hp_new, true, false);
                }
            }

            /*
             * Safety check: fallen out of for loop?
             */
            if (stack_idx < 0)
            {
                emsg(u8("E318: Updated too many blocks?"));
                buf.b_ml.ml_stack_top = 0;      /* invalidate stack */
            }
        }

        /* The line was inserted below 'lnum'. */
        ml_updatechunk(buf, lnum + 1, (long)len, ML_CHNK_ADDLINE);
        return true;
    }

    /*
     * Replace line lnum, with buffering, in current buffer.
     *
     * If "copy" is true, make a copy of the line,
     * otherwise the line has been copied to allocated memory already.
     *
     * Check: The caller of this function should probably also call
     * changed_lines(), unless update_screen(NOT_VALID) is used.
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean ml_replace(long lnum, Bytes line, boolean copy)
    {
        if (line == null)           /* just checking... */
            return false;

        /* When starting up, we might still need to create the memfile. */
        if (curbuf.b_ml.ml_mfp == null && open_buffer(false, null, 0) == false)
            return false;

        if (copy)
            line = STRDUP(line);

        if (curbuf.b_ml.ml_line_lnum != lnum)       /* other line buffered */
            ml_flush_line(curbuf);                  /* flush it */

        curbuf.b_ml.ml_line_ptr = line;
        curbuf.b_ml.ml_line_lnum = lnum;
        curbuf.b_ml.ml_flags = (curbuf.b_ml.ml_flags | ML_LINE_DIRTY) & ~ML_EMPTY;

        return true;
    }

    /*
     * Delete line 'lnum' in the current buffer.
     *
     * Check: The caller of this function should probably also call
     * deleted_lines() after this.
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean ml_delete(long lnum, boolean message)
    {
        ml_flush_line(curbuf);
        return ml_delete_int(curbuf, lnum, message);
    }

    /*private*/ static boolean ml_delete_int(buffer_C buf, long lnum, boolean message)
    {
        if (lnum < 1 || buf.b_ml.ml_line_count < lnum)
            return false;

        if (lowest_marked != 0 && lnum < lowest_marked)
            lowest_marked--;

        /*
         * If the file becomes empty the last line is replaced by an empty line.
         */
        if (buf.b_ml.ml_line_count == 1)    /* file becomes empty */
        {
            if (message)
                set_keep_msg(no_lines_msg, 0);

            /* FEAT_BYTEOFF already handled in there, don't worry 'bout it below. */
            boolean b = ml_replace(1, u8(""), true);
            buf.b_ml.ml_flags |= ML_EMPTY;

            return b;
        }

        /*
         * Find the data block containing the line.
         * This also fills the stack with the blocks from the root to the data block.
         * This also releases any locked block.
         */
        memfile_C mfp = buf.b_ml.ml_mfp;
        if (mfp == null)
            return false;

        block_hdr_C hp = ml_find_line(buf, lnum, ML_DELETE);
        if (hp == null)
            return false;

        data_block_C dp = (data_block_C)hp.bh_data;

        /* compute line count before the delete; number of entries in block */
        int count = (int)(buf.b_ml.ml_locked_high - buf.b_ml.ml_locked_low + 2);
        int idx = (int)(lnum - buf.b_ml.ml_locked_low);

        --buf.b_ml.ml_line_count;

        int line_start = (dp.db_index[idx] & DB_INDEX_MASK);
        int line_size;
        if (idx == 0)                           /* first line in block, text at the end */
            line_size = dp.db_txt_end - line_start;
        else
            line_size = (dp.db_index[idx - 1] & DB_INDEX_MASK) - line_start;

        /*
         * special case: If there is only one line in the data block it becomes empty.
         * Then we have to remove the entry, pointing to this data block, from the pointer block.
         * If this pointer block also becomes empty, we go up another block, and so on,
         * up to the root if necessary.
         * The line counts in the pointer blocks have already been adjusted by ml_find_line().
         */
        if (count == 1)
        {
            mf_free(mfp, hp);                           /* free the data block */
            buf.b_ml.ml_locked = null;

            for (int stack_idx = buf.b_ml.ml_stack_top - 1; 0 <= stack_idx; --stack_idx)
            {
                buf.b_ml.ml_stack_top = 0;              /* stack is invalid when failing */
                infoptr_C ip = buf.b_ml.ml_stack[stack_idx];
                idx = ip.ip_index;
                if ((hp = mf_get(mfp, ip.ip_bnum, 1)) == null)
                    return false;

                ptr_block_C pp = (ptr_block_C)hp.bh_data; /* must be pointer block */
                if (pp.pb_id != PTR_ID)
                {
                    emsg(u8("E317: pointer block id wrong 4"));
                    mf_put(mfp, hp, false, false);
                    return false;
                }
                count = --pp.pb_count;
                if (count == 0)                         /* the pointer block becomes empty! */
                    mf_free(mfp, hp);
                else
                {
                    if (count != idx)                   /* move entries after the deleted one */
                        for (int i = idx; i < count; i++)
                            COPY_ptr_entry(pp.pb_pointer[i], pp.pb_pointer[i + 1]);
                    mf_put(mfp, hp, true, false);

                    buf.b_ml.ml_stack_top = stack_idx; /* truncate stack */
                    /* fix line count for rest of blocks in the stack */
                    if (buf.b_ml.ml_locked_lineadd != 0)
                    {
                        ml_lineadd(buf, buf.b_ml.ml_locked_lineadd);
                        buf.b_ml.ml_stack[buf.b_ml.ml_stack_top].ip_high += buf.b_ml.ml_locked_lineadd;
                    }
                    buf.b_ml.ml_stack_top++;

                    break;
                }
            }
        }
        else
        {
            /*
             * delete the text by moving the next lines forwards
             */
            int text_start = dp.db_txt_start;
            BCOPY(dp.db_text, text_start + line_size, dp.db_text, text_start, line_start - text_start);

            /*
             * delete the index by moving the next indexes backwards
             * Adjust the indexes for the text movement.
             */
            for (int i = idx; i < count - 1; i++)
                dp.db_index[i] = dp.db_index[i + 1] + line_size;

            dp.db_free += line_size + INDEX_SIZE;
            dp.db_txt_start += line_size;
            --dp.db_line_count;

            /*
             * mark the block dirty and make sure it is in the file (for recovery)
             */
            buf.b_ml.ml_flags |= (ML_LOCKED_DIRTY | ML_LOCKED_POS);
        }

        ml_updatechunk(buf, lnum, line_size, ML_CHNK_DELLINE);
        return true;
    }

    /*
     * set the B_MARKED flag for line 'lnum'
     */
    /*private*/ static void ml_setmarked(long lnum)
    {
        /* invalid line number */
        if (lnum < 1 || curbuf.b_ml.ml_line_count < lnum || curbuf.b_ml.ml_mfp == null)
            return;                     /* give error message? */

        if (lowest_marked == 0 || lnum < lowest_marked)
            lowest_marked = lnum;

        /*
         * find the data block containing the line
         * This also fills the stack with the blocks from the root to the data block.
         * This also releases any locked block.
         */
        block_hdr_C hp = ml_find_line(curbuf, lnum, ML_FIND);
        if (hp == null)
            return;                     /* give error message? */

        data_block_C dp = (data_block_C)hp.bh_data;
        dp.db_index[(int)(lnum - curbuf.b_ml.ml_locked_low)] |= DB_MARKED;
        curbuf.b_ml.ml_flags |= ML_LOCKED_DIRTY;
    }

    /*
     * find the first line with its B_MARKED flag set
     */
    /*private*/ static long ml_firstmarked()
    {
        if (curbuf.b_ml.ml_mfp == null)
            return 0;

        /*
         * The search starts with lowest_marked line.
         * This is the last line where a mark was found, adjusted by inserting/deleting lines.
         */
        for (long lnum = lowest_marked; lnum <= curbuf.b_ml.ml_line_count; )
        {
            /*
             * Find the data block containing the line.
             * This also fills the stack with the blocks from the root
             * to the data block and releases any locked block.
             */
            block_hdr_C hp = ml_find_line(curbuf, lnum, ML_FIND);
            if (hp == null)
                return 0;               /* give error message? */

            data_block_C dp = (data_block_C)hp.bh_data;

            for (int i = (int)(lnum - curbuf.b_ml.ml_locked_low); lnum <= curbuf.b_ml.ml_locked_high; i++, lnum++)
                if ((dp.db_index[i] & DB_MARKED) != 0)
                {
                    dp.db_index[i] &= DB_INDEX_MASK;
                    curbuf.b_ml.ml_flags |= ML_LOCKED_DIRTY;
                    lowest_marked = lnum + 1;
                    return lnum;
                }
        }

        return 0;
    }

    /*
     * clear all DB_MARKED flags
     */
    /*private*/ static void ml_clearmarked()
    {
        if (curbuf.b_ml.ml_mfp == null)     /* nothing to do */
            return;

        /*
         * The search starts with line lowest_marked.
         */
        for (long lnum = lowest_marked; lnum <= curbuf.b_ml.ml_line_count; )
        {
            /*
             * Find the data block containing the line.
             * This also fills the stack with the blocks from the root
             * to the data block and releases any locked block.
             */
            block_hdr_C hp = ml_find_line(curbuf, lnum, ML_FIND);
            if (hp == null)
                return;                         /* give error message? */

            data_block_C dp = (data_block_C)hp.bh_data;

            for (int i = (int)(lnum - curbuf.b_ml.ml_locked_low); lnum <= curbuf.b_ml.ml_locked_high; i++, lnum++)
                if ((dp.db_index[i] & DB_MARKED) != 0)
                {
                    dp.db_index[i] &= DB_INDEX_MASK;
                    curbuf.b_ml.ml_flags |= ML_LOCKED_DIRTY;
                }
        }

        lowest_marked = 0;
    }

    /*private*/ static boolean _1_entered;

    /*
     * flush ml_line if necessary
     */
    /*private*/ static void ml_flush_line(buffer_C buf)
    {
        if (buf.b_ml.ml_line_lnum == 0 || buf.b_ml.ml_mfp == null)
            return;                                         /* nothing to do */

        if ((buf.b_ml.ml_flags & ML_LINE_DIRTY) != 0)
        {
            /* This code doesn't work recursively,
             * but Netbeans may call back here when obtaining the cursor position. */
            if (_1_entered)
                return;
            _1_entered = true;

            long lnum = buf.b_ml.ml_line_lnum;
            Bytes new_line = buf.b_ml.ml_line_ptr;

            block_hdr_C hp = ml_find_line(buf, lnum, ML_FIND);
            if (hp == null)
                emsgn(u8("E320: Cannot find line %ld"), lnum);
            else
            {
                data_block_C dp = (data_block_C)hp.bh_data;

                int idx = (int)(lnum - buf.b_ml.ml_locked_low);
                int start = (dp.db_index[idx] & DB_INDEX_MASK);
                int old_len;
                if (idx == 0)                               /* line is last in block */
                    old_len = dp.db_txt_end - start;
                else                                        /* text of previous line follows */
                    old_len = (dp.db_index[idx - 1] & DB_INDEX_MASK) - start;
                int new_len = strlen(new_line) + 1;
                int extra = new_len - old_len;              /* negative if lines gets smaller */

                /*
                 * if new line fits in data block, replace directly
                 */
                if (extra <= dp.db_free)
                {
                    /* if the length changes and there are following lines */
                    int count = (int)(buf.b_ml.ml_locked_high - buf.b_ml.ml_locked_low + 1);
                    if (extra != 0 && idx < count - 1)
                    {
                        /* move text of following lines */
                        BCOPY(dp.db_text, dp.db_txt_start - extra, dp.db_text, dp.db_txt_start, start - dp.db_txt_start);

                        /* adjust pointers of this and following lines */
                        for (int i = idx + 1; i < count; i++)
                            dp.db_index[i] -= extra;
                    }
                    dp.db_index[idx] -= extra;

                    /* adjust free space */
                    dp.db_free -= extra;
                    dp.db_txt_start -= extra;

                    /* copy new line into the data block */
                    BCOPY(dp.db_text, start - extra, new_line, 0, new_len);
                    buf.b_ml.ml_flags |= (ML_LOCKED_DIRTY | ML_LOCKED_POS);
                    /* The else case is already covered by the insert and delete. */
                    ml_updatechunk(buf, lnum, (long)extra, ML_CHNK_UPDLINE);
                }
                else
                {
                    /*
                     * Cannot do it in one data block: Delete and append.
                     * Append first, because ml_delete_int() cannot delete the last line
                     * in a buffer, which causes trouble for a buffer that has only one line.
                     * Don't forget to copy the mark!
                     */
                    /* How about handling errors??? */
                    ml_append_int(buf, lnum, new_line, new_len, false, (dp.db_index[idx] & DB_MARKED) != 0);
                    ml_delete_int(buf, lnum, false);
                }
            }

            _1_entered = false;
        }

        buf.b_ml.ml_line_lnum = 0;
    }

    /*
     * create a new, empty, zero block
     */
    /*private*/ static block_hdr_C ml_new_zero(memfile_C mfp)
    {
        return mf_new(mfp, false, new zero_block_C(), 1);
    }

    /*
     * create a new, empty, pointer block
     */
    /*private*/ static block_hdr_C ml_new_ptr(memfile_C mfp)
    {
        return mf_new(mfp, false, new ptr_block_C(), 1);
    }

    /*
     * create a new, empty, data block
     */
    /*private*/ static block_hdr_C ml_new_data(memfile_C mfp, boolean negative, int page_count)
    {
        return mf_new(mfp, negative, new data_block_C(page_count), page_count);
    }

    /*
     * lookup line 'lnum' in a memline
     *
     *   action: if ML_DELETE or ML_INSERT the line count is updated while searching
     *           if ML_FLUSH only flush a locked block
     *           if ML_FIND just find the line
     *
     * If the block was found it is locked and put in ml_locked.
     * The stack is updated to lead to the locked block.  The ip_high field in
     * the stack is updated to reflect the last line in the block AFTER the
     * insert or delete, also if the pointer block has not been updated yet.
     * But if ml_locked != null ml_locked_lineadd must be added to ip_high.
     *
     * return: null for failure, pointer to block header otherwise
     */
    /*private*/ static block_hdr_C ml_find_line(buffer_C buf, long lnum, int action)
    {
        memfile_C mfp = buf.b_ml.ml_mfp;

        /*
         * If there is a locked block check if the wanted line is in it.
         * If not, flush and release the locked block.
         * Don't do this for ML_INSERT_SAME, because the stack need to be updated.
         * Don't do this for ML_FLUSH, because we want to flush the locked block.
         * Don't do this when 'swapfile' is reset, we want to load all the blocks.
         */
        if (buf.b_ml.ml_locked != null)
        {
            if (ml_simple(action) && buf.b_ml.ml_locked_low <= lnum && lnum <= buf.b_ml.ml_locked_high)
            {
                /* remember to update pointer blocks and stack later */
                if (action == ML_INSERT)
                {
                    buf.b_ml.ml_locked_lineadd++;
                    buf.b_ml.ml_locked_high++;
                }
                else if (action == ML_DELETE)
                {
                    --buf.b_ml.ml_locked_lineadd;
                    --buf.b_ml.ml_locked_high;
                }
                return buf.b_ml.ml_locked;
            }

            mf_put(mfp, buf.b_ml.ml_locked, (buf.b_ml.ml_flags & ML_LOCKED_DIRTY) != 0,
                                              (buf.b_ml.ml_flags & ML_LOCKED_POS) != 0);
            buf.b_ml.ml_locked = null;

            /*
             * If lines have been added or deleted in the locked block,
             * need to update the line count in pointer blocks.
             */
            if (buf.b_ml.ml_locked_lineadd != 0)
                ml_lineadd(buf, buf.b_ml.ml_locked_lineadd);
        }

        if (action == ML_FLUSH)                         /* nothing else to do */
            return null;

        long bnum = 1;                                  /* start at the root of the tree */
        int page_count = 1;
        long low = 1;
        long high = buf.b_ml.ml_line_count;

        if (action == ML_FIND)                          /* first try stack entries */
        {
            int top;
            for (top = buf.b_ml.ml_stack_top - 1; 0 <= top; --top)
            {
                infoptr_C ip = buf.b_ml.ml_stack[top];
                if (ip.ip_low <= lnum && lnum <= ip.ip_high)
                {
                    bnum = ip.ip_bnum;
                    low = ip.ip_low;
                    high = ip.ip_high;
                    buf.b_ml.ml_stack_top = top;        /* truncate stack at prev entry */
                    break;
                }
            }
            if (top < 0)
                buf.b_ml.ml_stack_top = 0;              /* not found, start at the root */
        }
        else        /* ML_DELETE or ML_INSERT */
            buf.b_ml.ml_stack_top = 0;                  /* start at the root */

        error_noblock:
        {
            block_hdr_C hp;

            /*
            * search downwards in the tree until a data block is found
            */
            error_block:
            for ( ; ; )
            {
                hp = mf_get(mfp, bnum, page_count);
                if (hp == null)
                    break error_noblock;

                /*
                * update high for insert/delete
                */
                if (action == ML_INSERT)
                    high++;
                else if (action == ML_DELETE)
                    --high;

                if (hp.bh_data instanceof data_block_C)
                {
                    data_block_C dp = (data_block_C)hp.bh_data;
                    if (dp.db_id == DATA_ID)                    /* data block */
                    {
                        buf.b_ml.ml_locked = hp;
                        buf.b_ml.ml_locked_low = low;
                        buf.b_ml.ml_locked_high = high;
                        buf.b_ml.ml_locked_lineadd = 0;
                        buf.b_ml.ml_flags &= ~(ML_LOCKED_DIRTY | ML_LOCKED_POS);
                        return hp;
                    }
                }

                ptr_block_C pp = (ptr_block_C)hp.bh_data;           /* must be pointer block */
                if (pp.pb_id != PTR_ID)
                {
                    emsg(u8("E317: pointer block id wrong"));
                    break error_block;
                }

                int top = ml_add_stack(buf);
                if (top < 0)                                /* add new entry to stack */
                    break error_block;

                infoptr_C ip = buf.b_ml.ml_stack[top];
                ip.ip_bnum = bnum;
                ip.ip_low = low;
                ip.ip_high = high;
                ip.ip_index = -1;                           /* index not known yet */

                boolean dirty = false;
                int idx;
                for (idx = 0; idx < pp.pb_count; idx++)
                {
                    long t = pp.pb_pointer[idx].pe_line_count;

                    if (lnum < (low += t))
                    {
                        ip.ip_index = idx;
                        bnum = pp.pb_pointer[idx].pe_bnum;
                        page_count = pp.pb_pointer[idx].pe_page_count;
                        high = low - 1;
                        low -= t;

                        /*
                        * a negative block number may have been changed
                        */
                        if (bnum < 0)
                        {
                            long bnum2 = mf_trans_del(mfp, bnum);
                            if (bnum != bnum2)
                            {
                                bnum = bnum2;
                                pp.pb_pointer[idx].pe_bnum = bnum;
                                dirty = true;
                            }
                        }

                        break;
                    }
                }
                if (pp.pb_count <= idx)         /* past the end: something wrong! */
                {
                    if (buf.b_ml.ml_line_count < lnum)
                        emsgn(u8("E322: line number out of range: %ld past the end"), lnum - buf.b_ml.ml_line_count);

                    else
                        emsgn(u8("E323: line count wrong in block %ld"), bnum);
                    break error_block;
                }
                if (action == ML_DELETE)
                {
                    pp.pb_pointer[idx].pe_line_count--;
                    dirty = true;
                }
                else if (action == ML_INSERT)
                {
                    pp.pb_pointer[idx].pe_line_count++;
                    dirty = true;
                }
                mf_put(mfp, hp, dirty, false);
            }

            mf_put(mfp, hp, false, false);
        }

        /*
         * If action is ML_DELETE or ML_INSERT we have to correct the tree for
         * the incremented/decremented line counts, because there won't be a line
         * inserted/deleted after all.
         */
        if (action == ML_DELETE)
            ml_lineadd(buf, 1);
        else if (action == ML_INSERT)
            ml_lineadd(buf, -1);
        buf.b_ml.ml_stack_top = 0;
        return null;
    }

    /*
     * add an entry to the info pointer stack
     *
     * return -1 for failure, number of the new entry otherwise
     */
    /*private*/ static int ml_add_stack(buffer_C buf)
    {
        int top = buf.b_ml.ml_stack_top;

        /* may have to increase the stack size */
        if (top == buf.b_ml.ml_stack_size)
        {
            infoptr_C[] newstack = ARRAY_infoptr(buf.b_ml.ml_stack_size + STACK_INCR);

            for (int i = 0; i < top; i++)
                COPY_infoptr(newstack[i], buf.b_ml.ml_stack[i]);
            buf.b_ml.ml_stack = newstack;
            buf.b_ml.ml_stack_size += STACK_INCR;
        }

        buf.b_ml.ml_stack_top++;
        return top;
    }

    /*
     * Update the pointer blocks on the stack for inserted/deleted lines.
     * The stack itself is also updated.
     *
     * When a insert/delete line action fails, the line is not inserted/deleted,
     * but the pointer blocks have already been updated.  That is fixed here by
     * walking through the stack.
     *
     * Count is the number of lines added, negative if lines have been deleted.
     */
    /*private*/ static void ml_lineadd(buffer_C buf, int count)
    {
        memfile_C mfp = buf.b_ml.ml_mfp;

        for (int idx = buf.b_ml.ml_stack_top - 1; 0 <= idx; --idx)
        {
            infoptr_C ip = buf.b_ml.ml_stack[idx];
            block_hdr_C hp = mf_get(mfp, ip.ip_bnum, 1);
            if (hp == null)
                break;

            ptr_block_C pp = (ptr_block_C)hp.bh_data; /* must be pointer block */
            if (pp.pb_id != PTR_ID)
            {
                mf_put(mfp, hp, false, false);
                emsg(u8("E317: pointer block id wrong 2"));
                break;
            }

            pp.pb_pointer[ip.ip_index].pe_line_count += count;
            ip.ip_high += count;
            mf_put(mfp, hp, true, false);
        }
    }

    /*
     * Resolve a symlink in the last component of a file name.
     * If it worked returns true and the resolved link in "buf[MAXPATHL]".
     * Otherwise returns false.
     */
    /*private*/ static boolean resolve_symlink(Bytes fname, Bytes buf)
    {
        Bytes tmp = new Bytes(MAXPATHL);

        if (fname == null)
            return false;

        /* Put the result so far in tmp[], starting with the original name. */
        vim_strncpy(tmp, fname, MAXPATHL - 1);

        for (int depth = 0; ; )
        {
            /* Limit symlink depth to 100, catch recursive loops. */
            if (++depth == 100)
            {
                emsg2(u8("E773: Symlink loop for \"%s\""), fname);
                return false;
            }

            int ret = (int)libC.readlink(tmp, buf, MAXPATHL - 1);
            if (ret <= 0)
            {
                if (libC.errno() == EINVAL || libC.errno() == ENOENT)
                {
                    /* Found non-symlink or not existing file, stop here.
                     * When at the first level use the unmodified name,
                     * skip the call to vim_fullName(). */
                    if (depth == 1)
                        return false;

                    /* Use the resolved name in tmp[]. */
                    break;
                }

                /* There must be some error reading links, use original name. */
                return false;
            }
            buf.be(ret, NUL);

            /*
             * Check whether the symlink is relative or absolute.
             * If it's relative, build a new path based on the directory
             * portion of the filename (if any) and the path the symlink points to.
             */
            if (mch_isFullName(buf))
                STRCPY(tmp, buf);
            else
            {
                Bytes tail = gettail(tmp);
                if (MAXPATHL <= strlen(tail) + strlen(buf))
                    return false;
                STRCPY(tail, buf);
            }
        }

        /*
         * Try to resolve the full name of the file so that the swapfile name will
         * be consistent even when opening a relative symlink from different
         * working directories.
         */
        return vim_fullName(tmp, buf, MAXPATHL, true);
    }

    /*
     * Set the flags in the first block of the swap file:
     * - file is modified or not: buf.b_changed
     */
    /*private*/ static void ml_setflags(buffer_C buf)
    {
        if (buf.b_ml.ml_mfp == null)
            return;

        for (block_hdr_C hp = buf.b_ml.ml_mfp.mf_used_last; hp != null; hp = hp.bh_prev)
        {
            if (hp.bh_bnum() == 0)
            {
                hp.bh_flags |= BH_DIRTY;
                break;
            }
        }
    }

    /*private*/ static final int MLCS_MAXL = 800;   /* max no of lines in chunk */
    /*private*/ static final int MLCS_MINL = 400;   /* should be half of MLCS_MAXL */

    /*private*/ static buffer_C     ml_upd_lastbuf;
    /*private*/ static long         ml_upd_lastline;
    /*private*/ static long         ml_upd_lastcurline;
    /*private*/ static int          ml_upd_lastcurix;

    /*
     * Keep information for finding byte offset of a line, updtype may be one of:
     * ML_CHNK_ADDLINE: Add len to parent chunk, possibly splitting it.
     *         Careful: ML_CHNK_ADDLINE may cause ml_find_line() to be called.
     * ML_CHNK_DELLINE: Subtract len from parent chunk, possibly deleting it.
     * ML_CHNK_UPDLINE: Add len to parent chunk, as a signed entity.
     */
    /*private*/ static void ml_updatechunk(buffer_C buf, long line, long len, int updtype)
    {
        long curline = ml_upd_lastcurline;
        int curix = ml_upd_lastcurix;

        if (buf.b_ml.ml_usedchunks == -1 || len == 0)
            return;

        if (buf.b_ml.ml_chunksize == null)
        {
            buf.b_ml.ml_chunksize = ARRAY_chunksize(100);
            buf.b_ml.ml_numchunks = 100;
            buf.b_ml.ml_usedchunks = 1;
            buf.b_ml.ml_chunksize[0].mlcs_numlines = 1;
            buf.b_ml.ml_chunksize[0].mlcs_totalsize = 1;
        }

        if (updtype == ML_CHNK_UPDLINE && buf.b_ml.ml_line_count == 1)
        {
            /*
             * First line in empty buffer from ml_flush_line() -- reset.
             */
            buf.b_ml.ml_usedchunks = 1;
            buf.b_ml.ml_chunksize[0].mlcs_numlines = 1;
            buf.b_ml.ml_chunksize[0].mlcs_totalsize = strlen(buf.b_ml.ml_line_ptr) + 1;
            return;
        }

        /*
         * Find chunk that our line belongs to, curline will be at start of the chunk.
         */
        if (buf != ml_upd_lastbuf || line != ml_upd_lastline + 1 || updtype != ML_CHNK_ADDLINE)
        {
            for (curline = 1, curix = 0;
                 curix < buf.b_ml.ml_usedchunks - 1
                    && curline + buf.b_ml.ml_chunksize[curix].mlcs_numlines <= line;
                 curix++)
            {
                curline += buf.b_ml.ml_chunksize[curix].mlcs_numlines;
            }
        }
        else if (curline + buf.b_ml.ml_chunksize[curix].mlcs_numlines <= line
                    && curix < buf.b_ml.ml_usedchunks - 1)
        {
            /* Adjust cached curix & curline. */
            curline += buf.b_ml.ml_chunksize[curix].mlcs_numlines;
            curix++;
        }

        if (updtype == ML_CHNK_DELLINE)
            len = -len;
        buf.b_ml.ml_chunksize[curix].mlcs_totalsize += len;

        if (updtype == ML_CHNK_ADDLINE)
        {
            chunksize_C[] chunks = buf.b_ml.ml_chunksize;

            chunks[curix].mlcs_numlines++;

            /* May resize here so we don't have to do it in both cases below. */
            if (buf.b_ml.ml_numchunks <= buf.b_ml.ml_usedchunks + 1)
            {
                int n = buf.b_ml.ml_numchunks;
                buf.b_ml.ml_numchunks = n * 3 / 2;
                chunks = ARRAY_chunksize(buf.b_ml.ml_numchunks);
                for (int i = 0; i < buf.b_ml.ml_usedchunks; i++)
                    COPY_chunksize(chunks[i], buf.b_ml.ml_chunksize[i]);
                buf.b_ml.ml_chunksize = chunks;
            }

            if (MLCS_MAXL <= chunks[curix].mlcs_numlines)
            {
                for (int i = buf.b_ml.ml_usedchunks; curix <= --i; )
                    COPY_chunksize(chunks[i + 1], chunks[i]);

                /* Compute length of first half of lines in the split chunk. */
                long size = 0;
                int linecnt = 0;
                while (curline < buf.b_ml.ml_line_count && linecnt < MLCS_MINL)
                {
                    block_hdr_C hp = ml_find_line(buf, curline, ML_FIND);
                    if (hp == null)
                    {
                        buf.b_ml.ml_usedchunks = -1;
                        return;
                    }

                    data_block_C dp = (data_block_C)hp.bh_data;

                    /* number of entries in block */
                    int count = (int)(buf.b_ml.ml_locked_high - buf.b_ml.ml_locked_low + 1);
                    int idx = (int)(curline - buf.b_ml.ml_locked_low);
                    curline = buf.b_ml.ml_locked_high + 1;
                    int text_end;
                    if (idx == 0)       /* first line in block, text at the end */
                        text_end = dp.db_txt_end;
                    else
                        text_end = (dp.db_index[idx - 1] & DB_INDEX_MASK);
                    /* Compute index of last line to use in this MEMLINE. */
                    int rest = count - idx;
                    if (MLCS_MINL < linecnt + rest)
                    {
                        idx += MLCS_MINL - linecnt - 1;
                        linecnt = MLCS_MINL;
                    }
                    else
                    {
                        idx = count - 1;
                        linecnt += rest;
                    }
                    size += text_end - (dp.db_index[idx] & DB_INDEX_MASK);
                }

                chunks[curix].mlcs_numlines = linecnt;
                chunks[curix + 1].mlcs_numlines -= linecnt;
                chunks[curix].mlcs_totalsize = size;
                chunks[curix + 1].mlcs_totalsize -= size;
                buf.b_ml.ml_usedchunks++;

                ml_upd_lastbuf = null;      /* Force recalc of curix & curline. */
                return;
            }

            if (MLCS_MINL <= chunks[curix].mlcs_numlines
                    && curix == buf.b_ml.ml_usedchunks - 1 && buf.b_ml.ml_line_count - line <= 1)
            {
                /*
                 * We are in the last chunk and it is cheap to crate a new one after this.
                 * Do it now to avoid the loop above later on.
                 */
                buf.b_ml.ml_usedchunks++;

                if (line == buf.b_ml.ml_line_count)
                {
                    chunks[curix + 1].mlcs_numlines = 0;
                    chunks[curix + 1].mlcs_totalsize = 0;
                }
                else
                {
                    /*
                     * Line is just prior to last, move count for last.
                     * This is the common case when loading a new file.
                     */
                    block_hdr_C hp = ml_find_line(buf, buf.b_ml.ml_line_count, ML_FIND);
                    if (hp == null)
                    {
                        buf.b_ml.ml_usedchunks = -1;
                        return;
                    }

                    data_block_C dp = (data_block_C)hp.bh_data;

                    int rest;
                    if (dp.db_line_count == 1)
                        rest = dp.db_txt_end - dp.db_txt_start;
                    else
                        rest = (dp.db_index[dp.db_line_count - 2] & DB_INDEX_MASK) - dp.db_txt_start;

                    chunks[curix].mlcs_numlines -= 1;
                    chunks[curix + 1].mlcs_numlines = 1;
                    chunks[curix].mlcs_totalsize -= rest;
                    chunks[curix + 1].mlcs_totalsize = rest;
                }
            }
        }
        else if (updtype == ML_CHNK_DELLINE)
        {
            chunksize_C[] chunks = buf.b_ml.ml_chunksize;

            chunks[curix].mlcs_numlines--;
            ml_upd_lastbuf = null;          /* Force recalc of curix & curline. */

            if (curix < buf.b_ml.ml_usedchunks - 1
                    && chunks[curix].mlcs_numlines + chunks[curix + 1].mlcs_numlines <= MLCS_MINL)
            {
                curix++;
            }
            else if (curix == 0 && chunks[curix].mlcs_numlines <= 0)
            {
                buf.b_ml.ml_usedchunks--;
                for (int i = 0; i < buf.b_ml.ml_usedchunks; i++)
                    COPY_chunksize(chunks[i], chunks[i + 1]);
                return;
            }
            else if (curix == 0 || (10 < chunks[curix].mlcs_numlines
                    && MLCS_MINL < chunks[curix].mlcs_numlines + chunks[curix - 1].mlcs_numlines))
            {
                return;
            }

            /* Collapse chunks. */
            chunks[curix - 1].mlcs_numlines += chunks[curix].mlcs_numlines;
            chunks[curix - 1].mlcs_totalsize += chunks[curix].mlcs_totalsize;
            buf.b_ml.ml_usedchunks--;
            for (int i = curix; i < buf.b_ml.ml_usedchunks; i++)
                COPY_chunksize(chunks[i], chunks[i + 1]);
            return;
        }

        ml_upd_lastbuf = buf;
        ml_upd_lastline = line;
        ml_upd_lastcurline = curline;
        ml_upd_lastcurix = curix;
    }

    /*
     * Find offset for line or line with offset.
     * Find line with offset if "lnum" is 0; return remaining offset in offp.
     * Find offset of line if "lnum" > 0.
     * return -1 if information is not available
     */
    /*private*/ static long ml_find_line_or_offset(buffer_C buf, long lnum, long[] offp)
    {
        boolean ffdos = (get_fileformat(buf) == EOL_DOS);
        int extra = 0;

        /* take care of cached line first */
        ml_flush_line(curbuf);

        if (buf.b_ml.ml_usedchunks == -1 || buf.b_ml.ml_chunksize == null || lnum < 0)
            return -1;

        long offset = (offp != null) ? offp[0] : 0;
        if (lnum == 0 && offset <= 0)
            return 1;   /* Not a "find offset" and offset 0 _must_ be in line 1. */

        /*
         * Find the last chunk before the one containing our line.
         * Last chunk is special because it will never qualify.
         */
        long curline = 1;
        int curix = 0;
        long size = curix;
        while (curix < buf.b_ml.ml_usedchunks - 1
            && ((lnum != 0 && curline + buf.b_ml.ml_chunksize[curix].mlcs_numlines <= lnum)
                || (offset != 0
                    && size
                        + buf.b_ml.ml_chunksize[curix].mlcs_totalsize
                        + (ffdos ? 1 : 0) * buf.b_ml.ml_chunksize[curix].mlcs_numlines < offset)))
        {
            curline += buf.b_ml.ml_chunksize[curix].mlcs_numlines;
            size += buf.b_ml.ml_chunksize[curix].mlcs_totalsize;
            if (offset != 0 && ffdos)
                size += buf.b_ml.ml_chunksize[curix].mlcs_numlines;
            curix++;
        }

        while ((lnum != 0 && curline < lnum) || (offset != 0 && size < offset))
        {
            if (buf.b_ml.ml_line_count < curline)
                return -1;

            block_hdr_C hp = ml_find_line(buf, curline, ML_FIND);
            if (hp == null)
                return -1;

            data_block_C dp = (data_block_C)hp.bh_data;

            /* number of entries in block */
            int count = (int)(buf.b_ml.ml_locked_high - buf.b_ml.ml_locked_low + 1);
            int idx = (int)(curline - buf.b_ml.ml_locked_low);
            int start_idx = idx;
            int text_end;
            if (idx == 0)                           /* first line in block, text at the end */
                text_end = dp.db_txt_end;
            else
                text_end = (dp.db_index[idx - 1] & DB_INDEX_MASK);
            /* Compute index of last line to use in this MEMLINE. */
            if (lnum != 0)
            {
                if (lnum <= curline + (count - idx))
                    idx += lnum - curline - 1;
                else
                    idx = count - 1;
            }
            else
            {
                extra = 0;
                while (size + text_end - (dp.db_index[idx] & DB_INDEX_MASK) + (ffdos ? 1 : 0) <= offset)
                {
                    if (ffdos)
                        size++;
                    if (idx == count - 1)
                    {
                        extra = 1;
                        break;
                    }
                    idx++;
                }
            }
            int len = text_end - (dp.db_index[idx] & DB_INDEX_MASK);
            size += len;
            if (offset != 0 && offset <= size)
            {
                if (size + (ffdos ? 1 : 0) == offset)
                    offp[0] = 0;
                else if (idx == start_idx)
                    offp[0] = offset - size + len;
                else
                    offp[0] = offset - size + len - (text_end - (dp.db_index[idx - 1] & DB_INDEX_MASK));
                curline += idx - start_idx + extra;
                if (buf.b_ml.ml_line_count < curline)
                    return -1;      /* exactly one byte beyond the end */

                return curline;
            }
            curline = buf.b_ml.ml_locked_high + 1;
        }

        if (lnum != 0)
        {
            /* Count extra CR characters. */
            if (ffdos)
                size += lnum - 1;

            /* Don't count the last line break if 'bin' and 'noeol'. */
            if (buf.b_p_bin[0] && !buf.b_p_eol[0])
                size -= (ffdos ? 1 : 0) + 1;
        }

        return size;
    }

    /*
     * Goto byte in buffer with offset 'cnt'.
     */
    /*private*/ static void goto_byte(long cnt)
    {
        long[] boff = { cnt };

        ml_flush_line(curbuf);      /* cached line may be dirty */
        setpcmark();
        if (boff[0] != 0)
            --boff[0];

        long lnum = ml_find_line_or_offset(curbuf, 0, boff);
        if (lnum < 1)       /* past the end */
        {
            curwin.w_cursor.lnum = curbuf.b_ml.ml_line_count;
            curwin.w_curswant = MAXCOL;
            coladvance(MAXCOL);
        }
        else
        {
            curwin.w_cursor.lnum = lnum;
            curwin.w_cursor.col = (int)boff[0];
            curwin.w_cursor.coladd = 0;
            curwin.w_set_curswant = true;
        }
        check_cursor();

        /* Make sure the cursor is on the first byte of a multi-byte char. */
        mb_adjust_pos(curbuf, curwin.w_cursor);
    }

    /*
     * buffer.c: functions for dealing with the buffer structure --------------------------------------
     */

    /*
     * The buffer list is a double linked list of all buffers.
     * Each buffer can be in one of these states:
     * never loaded: BF_NEVERLOADED is set, only the file name is valid
     *   not loaded: b_ml.ml_mfp == null, no memfile allocated
     *       hidden: b_nwindows == 0, loaded but not displayed in a window
     *       normal: loaded and displayed in a window
     *
     * Instead of storing file names all over the place, each file name is
     * stored in the buffer list.  It can be referenced by a number.
     *
     * The current implementation remembers all file names ever used.
     */

    /*private*/ static Bytes e_auabort = u8("E855: Autocommands caused command to abort");

    /*
     * Open current buffer, that is: open the memfile and read the file into memory.
     * Return false for failure, true otherwise.
     */
    /*private*/ static boolean open_buffer(boolean read_stdin, exarg_C eap, int flags)
        /* read_stdin: read file from stdin */
        /* eap: for forced 'ff' and 'fenc' or null */
        /* flags: extra flags for readfile() */
    {
        boolean[] retval = { true };
        long old_tw = curbuf.b_p_tw[0];

        /*
         * The 'readonly' flag is only set when BF_NEVERLOADED is being reset.
         * When re-entering the same buffer, it should not change, because the
         * user may have reset the flag by hand.
         */
        if (readonlymode && curbuf.b_ffname != null && (curbuf.b_flags & BF_NEVERLOADED) != 0)
            curbuf.b_p_ro[0] = true;

        if (ml_open(curbuf) == false)
        {
            /*
             * There MUST be a memfile, otherwise we can't do anything.
             * If we can't create one for the current buffer, take another buffer.
             */
            close_buffer(null, curbuf, 0, false);
            for (curbuf = firstbuf; curbuf != null; curbuf = curbuf.b_next)
                if (curbuf.b_ml.ml_mfp != null)
                    break;
            /*
             * If there is no memfile at all, exit.
             * This is OK, since there are no changes to lose.
             */
            if (curbuf == null)
            {
                emsg(u8("E82: Cannot allocate any buffer, exiting..."));
                getout(2);
            }
            emsg(u8("E83: Cannot allocate buffer, using other one..."));
            enter_buffer(curbuf);
            if (old_tw != curbuf.b_p_tw[0])
                check_colorcolumn(curwin);
            return false;
        }

        /* The autocommands in readfile() may change the buffer, but only AFTER reading the file. */
        buffer_C old_curbuf = curbuf;
        modified_was_set = false;

        /* mark cursor position as being invalid */
        curwin.w_valid = 0;

        if (curbuf.b_ffname != null)
        {
            retval[0] = readfile(curbuf.b_ffname, curbuf.b_fname, 0, 0, MAXLNUM, eap, flags | READ_NEW);
        }
        else if (read_stdin)
        {
            boolean save_bin = curbuf.b_p_bin[0];
            long line_count;

            /*
             * First read the text in binary mode into the buffer.
             * Then read from that same buffer and append at the end.
             * This makes it possible to retry when 'fileformat' or 'fileencoding' was guessed wrong.
             */
            curbuf.b_p_bin[0] = true;
            retval[0] = readfile(null, null, 0, 0, MAXLNUM, null, flags | (READ_NEW + READ_STDIN));
            curbuf.b_p_bin[0] = save_bin;

            if (retval[0] == true)
            {
                line_count = curbuf.b_ml.ml_line_count;
                retval[0] = readfile(null, null, line_count, 0, MAXLNUM, eap, flags | READ_BUFFER);
                if (retval[0] == true)
                {
                    /* Delete the binary lines. */
                    while (0 <= --line_count)
                        ml_delete(1, false);
                }
                else
                {
                    /* Delete the converted lines. */
                    while (line_count < curbuf.b_ml.ml_line_count)
                        ml_delete(line_count, false);
                }
                /* Put the cursor on the first line. */
                curwin.w_cursor.lnum = 1;
                curwin.w_cursor.col = 0;

                /* Set or reset 'modified' before executing autocommands,
                 * so that it can be changed there. */
                if (!readonlymode && !bufempty())
                    changed();
                else if (retval[0] != false)
                    unchanged(curbuf, false);
                apply_autocmds_retval(EVENT_STDINREADPOST, null, null, false, curbuf, retval);
            }
        }

        /* if first time loading this buffer, init b_chartab[] */
        if ((curbuf.b_flags & BF_NEVERLOADED) != 0)
        {
            buf_init_chartab(curbuf, false);
            parse_cino(curbuf);
        }

        /*
         * Set/reset the Changed flag first, autocmds may change the buffer.
         * Apply the automatic commands, before processing the modelines.
         * So the modelines have priority over auto commands.
         */
        /* When reading stdin, the buffer contents always needs writing, so set
         * the changed flag.  Unless in readonly mode: "ls | gview -".
         * When interrupted and 'cpoptions' contains 'i' set changed flag. */
        if ((got_int && vim_strbyte(p_cpo[0], CPO_INTMOD) != null)
                    || modified_was_set     /* ":set modified" used in autocmd */
                    || (aborting() && vim_strbyte(p_cpo[0], CPO_INTMOD) != null))
            changed();
        else if (retval[0] != false && !read_stdin)
            unchanged(curbuf, false);
        save_file_ff(curbuf);               /* keep this fileformat */

        /* require "!" to overwrite the file, because it wasn't read completely */
        if (aborting())
            curbuf.b_flags |= BF_READERR;

        /* need to set w_topline, unless some autocommand already did that. */
        if ((curwin.w_valid & VALID_TOPLINE) == 0)
        {
            curwin.w_topline = 1;
        }
        apply_autocmds_retval(EVENT_BUFENTER, null, null, false, curbuf, retval);

        if (retval[0] != false)
        {
            /*
             * The autocommands may have changed the current buffer.  Apply the
             * modelines to the correct buffer, if it still exists and is loaded.
             */
            if (buf_valid(old_curbuf) && old_curbuf.b_ml.ml_mfp != null)
            {
                /* go to the buffer that was opened */
                aco_save_C aco = new aco_save_C();
                aucmd_prepbuf(aco, old_curbuf);

                curbuf.b_flags &= ~(BF_CHECK_RO | BF_NEVERLOADED);

                apply_autocmds_retval(EVENT_BUFWINENTER, null, null, false, curbuf, retval);

                /* restore curwin/curbuf and a few other things */
                aucmd_restbuf(aco);
            }
        }

        return retval[0];
    }

    /*
     * Return true if "buf" points to a valid buffer (in the buffer list).
     */
    /*private*/ static boolean buf_valid(buffer_C buf)
    {
        for (buffer_C bp = firstbuf; bp != null; bp = bp.b_next)
            if (bp == buf)
                return true;

        return false;
    }

    /*
     * Close the link to a buffer.
     * "action" is used when there is no longer a window for the buffer.
     * It can be:
     * 0                    buffer becomes hidden
     * DOBUF_UNLOAD         buffer is unloaded
     * DOBUF_DELETE         buffer is unloaded and removed from buffer list
     * DOBUF_WIPE           buffer is unloaded and really deleted
     * When doing all but the first one on the current buffer, the caller should
     * get a new buffer very soon!
     *
     * The 'bufhidden' option can force freeing and deleting.
     *
     * When "abort_if_last" is true then do not close the buffer if autocommands
     * cause there to be only one window with this buffer.  e.g. when ":quit" is
     * supposed to close the window but autocommands close all other windows.
     */
    /*private*/ static void close_buffer(window_C win, buffer_C buf, int action, boolean abort_if_last)
        /* win: if not null, set b_last_cursor */
    {
        boolean unload_buf = (action != 0);
        boolean del_buf = (action == DOBUF_DEL || action == DOBUF_WIPE);
        boolean wipe_buf = (action == DOBUF_WIPE);

        if (win != null && win_valid(win))      /* in case autocommands closed the window */
        {
            /* Set b_last_cursor when closing the last window for the buffer.
             * Remember the last cursor position and window options of the buffer.
             * This used to be only for the current window, but then options like
             * 'foldmethod' may be lost with a ":only" command. */
            if (buf.b_nwindows == 1)
                set_last_cursor(win);
            buflist_setfpos(buf, win, (win.w_cursor.lnum == 1) ? 0 : win.w_cursor.lnum, win.w_cursor.col, true);
        }

        /* When the buffer is no longer in a window, trigger BufWinLeave. */
        if (buf.b_nwindows == 1)
        {
            buf.b_closing = true;
            apply_autocmds(EVENT_BUFWINLEAVE, buf.b_fname, buf.b_fname, false, buf);
            if (!buf_valid(buf))
            {
                /* Autocommands deleted the buffer. */
                emsg(e_auabort);
                return;
            }
            buf.b_closing = false;
            if (abort_if_last && one_window())
            {
                /* Autocommands made this the only window. */
                emsg(e_auabort);
                return;
            }

            /* When the buffer becomes hidden, but is not unloaded, trigger BufHidden. */
            if (!unload_buf)
            {
                buf.b_closing = true;
                apply_autocmds(EVENT_BUFHIDDEN, buf.b_fname, buf.b_fname, false, buf);
                if (!buf_valid(buf))
                {
                    /* Autocommands deleted the buffer. */
                    emsg(e_auabort);
                    return;
                }
                buf.b_closing = false;
                if (abort_if_last && one_window())
                {
                    /* Autocommands made this the only window. */
                    emsg(e_auabort);
                    return;
                }
            }
            if (aborting())     /* autocmds may abort script processing */
                return;
        }

        int nwindows = buf.b_nwindows;

        /* decrease the link count from windows (unless not in any window) */
        if (0 < buf.b_nwindows)
            --buf.b_nwindows;

        /* Return when a window is displaying the buffer or when it's not unloaded. */
        if (0 < buf.b_nwindows || !unload_buf)
            return;

        /* Always remove the buffer when there is no file name. */
        if (buf.b_ffname == null)
            del_buf = true;

        /*
         * Free all things allocated for this buffer.
         * Also calls the "BufDelete" autocommands when del_buf is true.
         */
        /* Remember if we are closing the current buffer.  Restore the number of
         * windows, so that autocommands in buf_freeall() don't get confused. */
        boolean is_curbuf = (buf == curbuf);
        buf.b_nwindows = nwindows;

        buf_freeall(buf, (del_buf ? BFA_DEL : 0) + (wipe_buf ? BFA_WIPE : 0));
        if (win_valid(win) && win.w_buffer == buf)
            win.w_buffer = null;    /* make sure we don't use the buffer now */

        /* Autocommands may have deleted the buffer. */
        if (!buf_valid(buf))
            return;
        if (aborting())             /* autocmds may abort script processing */
            return;

        /* Autocommands may have opened or closed windows for this buffer.
         * Decrement the count for the close we do here. */
        if (0 < buf.b_nwindows)
            --buf.b_nwindows;

        /*
         * It's possible that autocommands change curbuf to the one being deleted.
         * This might cause the previous curbuf to be deleted unexpectedly.
         * But in some cases it's OK to delete the curbuf, because a new one is
         * obtained anyway.  Therefore only return if curbuf changed to the deleted buffer.
         */
        if (buf == curbuf && !is_curbuf)
            return;

        /*
         * Remove the buffer from the list.
         */
        if (wipe_buf)
        {
            buf.b_ffname = null;
            buf.b_sfname = null;
            if (buf.b_prev == null)
                firstbuf = buf.b_next;
            else
                buf.b_prev.b_next = buf.b_next;
            if (buf.b_next == null)
                lastbuf = buf.b_prev;
            else
                buf.b_next.b_prev = buf.b_prev;
            free_buffer(buf);
        }
        else
        {
            if (del_buf)
            {
                /* Free all internal variables and reset option values
                 * to make ":bdel" compatible with Vim 5.7. */
                free_buffer_stuff(buf, true);

                /* Make it look like a new buffer. */
                buf.b_flags = BF_CHECK_RO | BF_NEVERLOADED;

                /* Init the options when loaded again. */
                buf.b_p_initialized = false;
            }
            buf_clear_file(buf);
            if (del_buf)
                buf.b_p_bl[0] = false;
        }
    }

    /*
     * Make buffer not contain a file.
     */
    /*private*/ static void buf_clear_file(buffer_C buf)
    {
        buf.b_ml.ml_line_count = 1;
        unchanged(buf, true);
        buf.b_shortname = false;
        buf.b_p_eol[0] = true;
        buf.b_start_eol = true;
        buf.b_p_bomb[0] = false;
        buf.b_start_bomb = false;
        buf.b_ml.ml_mfp = null;
        buf.b_ml.ml_flags = ML_EMPTY;   /* empty buffer */
    }

    /*
     * buf_freeall() - free all things allocated for a buffer that are related to the file.
     * flags:
     *  BFA_DEL        buffer is going to be deleted
     *  BFA_WIPE       buffer is going to be wiped out
     *  BFA_KEEP_UNDO  do not free undo information
     */
    /*private*/ static void buf_freeall(buffer_C buf, int flags)
    {
        boolean is_curbuf = (buf == curbuf);

        buf.b_closing = true;
        apply_autocmds(EVENT_BUFUNLOAD, buf.b_fname, buf.b_fname, false, buf);
        if (!buf_valid(buf))            /* autocommands may delete the buffer */
            return;
        if ((flags & BFA_DEL) != 0 && buf.b_p_bl[0])
        {
            apply_autocmds(EVENT_BUFDELETE, buf.b_fname, buf.b_fname, false, buf);
            if (!buf_valid(buf))        /* autocommands may delete the buffer */
                return;
        }
        if ((flags & BFA_WIPE) != 0)
        {
            apply_autocmds(EVENT_BUFWIPEOUT, buf.b_fname, buf.b_fname, false, buf);
            if (!buf_valid(buf))        /* autocommands may delete the buffer */
                return;
        }
        buf.b_closing = false;
        if (aborting())                 /* autocommands may abort script processing */
            return;

        /*
         * It's possible that autocommands change curbuf to the one being deleted.
         * This might cause curbuf to be deleted unexpectedly.  But in some cases
         * it's OK to delete the curbuf, because a new one is obtained anyway.
         * Therefore only return if curbuf changed to the deleted buffer.
         */
        if (buf == curbuf && !is_curbuf)
            return;
        /* Remove any ownsyntax, unless exiting. */
        if (firstwin != null && curwin.w_buffer == buf)
            reset_synblock(curwin);

        ml_close(buf);                  /* close the memline/memfile */
        buf.b_ml.ml_line_count = 0;     /* no lines in buffer */
        if ((flags & BFA_KEEP_UNDO) == 0)
        {
            u_blockfree(buf);           /* free the memory allocated for undo */
            u_clearall(buf);            /* reset all undo information */
        }
        syntax_clear(buf.b_s);          /* reset syntax info */
        buf.b_flags &= ~BF_READERR;     /* a read error is no longer relevant */
    }

    /*
     * Free a buffer structure and the things it contains related to the buffer
     * itself (not the file, that must have been done already).
     */
    /*private*/ static void free_buffer(buffer_C buf)
    {
        free_buffer_stuff(buf, true);
        unref_var_dict(buf.b_vars);
        aubuflocal_remove(buf);
        if (autocmd_busy)
        {
            /* Do not free the buffer structure while autocommands are executing,
             * it's still needed.  Free it when autocmd_busy is reset. */
            buf.b_next = au_pending_free_buf;
            au_pending_free_buf = buf;
        }
    }

    /*
     * Free stuff in the buffer for ":bdel" and when wiping out the buffer.
     */
    /*private*/ static void free_buffer_stuff(buffer_C buf, boolean free_options)
        /* free_options: free options as well */
    {
        if (free_options)
        {
            clear_wininfo(buf);                             /* including window-local options */
            free_buf_options(buf, true);
        }
        vars_clear(buf.b_vars.dv_hashtab);                  /* free all internal variables */
        hash_init(buf.b_vars.dv_hashtab);
        uc_clear(buf.b_ucmds);                              /* clear local user commands */
        map_clear_int(buf, MAP_ALL_MODES, true, false);     /* clear local mappings */
        map_clear_int(buf, MAP_ALL_MODES, true, true);      /* clear local abbrevs */
        buf.b_start_fenc = null;
    }

    /*
     * Free the b_wininfo list for buffer "buf".
     */
    /*private*/ static void clear_wininfo(buffer_C buf)
    {
        while (buf.b_wininfo != null)
        {
            wininfo_C wip = buf.b_wininfo;
            buf.b_wininfo = wip.wi_next;
            if (wip.wi_optset)
                clear_winopt(wip.wi_opt);
        }
    }

    /*
     * Go to another buffer.  Handles the result of the ATTENTION dialog.
     */
    /*private*/ static void goto_buffer(exarg_C eap, int start, int dir, int count)
    {
        buffer_C old_curbuf = curbuf;

        swap_exists_action = SEA_DIALOG;
        do_buffer(eap.cmd.at(0) == (byte)'s' ? DOBUF_SPLIT : DOBUF_GOTO, start, dir, count, eap.forceit);
        if (swap_exists_action == SEA_QUIT && eap.cmd.at(0) == (byte)'s')
        {
            cleanup_C cs = new cleanup_C();

            /* Reset the error/interrupt/exception state here so that
             * aborting() returns false when closing a window. */
            enter_cleanup(cs);

            /* Quitting means closing the split window, nothing else. */
            win_close(curwin, true);
            swap_exists_action = SEA_NONE;
            swap_exists_did_quit = true;

            /* Restore the error/interrupt/exception state if not discarded
             * by a new aborting error, interrupt, or uncaught exception. */
            leave_cleanup(cs);
        }
        else
            handle_swap_exists(old_curbuf);
    }

    /*
     * Handle the situation of swap_exists_action being set.
     * It is allowed for "old_curbuf" to be null or invalid.
     */
    /*private*/ static void handle_swap_exists(buffer_C old_curbuf)
    {
        long old_tw = curbuf.b_p_tw[0];

        if (swap_exists_action == SEA_QUIT)
        {
            cleanup_C cs = new cleanup_C();
            /* Reset the error/interrupt/exception state here so that
             * aborting() returns false when closing a buffer. */
            enter_cleanup(cs);

            /* User selected Quit at ATTENTION prompt.  Go back to previous buffer.
             * If that buffer is gone or the same as the current one,
             * open a new, empty buffer. */
            swap_exists_action = SEA_NONE;  /* don't want it again */
            swap_exists_did_quit = true;
            close_buffer(curwin, curbuf, DOBUF_UNLOAD, false);
            if (!buf_valid(old_curbuf) || old_curbuf == curbuf)
                old_curbuf = buflist_new(null, null, 1L, BLN_CURBUF | BLN_LISTED);
            if (old_curbuf != null)
            {
                enter_buffer(old_curbuf);
                if (old_tw != curbuf.b_p_tw[0])
                    check_colorcolumn(curwin);
            }
            /* If "old_curbuf" is null we are in big trouble here... */

            /* Restore the error/interrupt/exception state if not discarded
             * by a new aborting error, interrupt, or uncaught exception. */
            leave_cleanup(cs);
        }

        swap_exists_action = SEA_NONE;
    }

    /*
     * do_bufdel() - delete or unload buffer(s)
     *
     * addr_count == 0: ":bdel" - delete current buffer
     * addr_count == 1: ":N bdel" or ":bdel N [N ..]" - first delete
     *                  buffer "end_bnr", then any other arguments.
     * addr_count == 2: ":N,N bdel" - delete buffers in range
     *
     * command can be DOBUF_UNLOAD (":bunload"), DOBUF_WIPE (":bwipeout") or
     * DOBUF_DEL (":bdel")
     *
     * Returns error message or null
     */
    /*private*/ static Bytes do_bufdel(int command, Bytes arg, int addr_count, int start_bnr, int end_bnr, boolean forceit)
        /* arg: pointer to extra arguments */
        /* start_bnr: first buffer number in a range */
        /* end_bnr: buffer nr or last buffer nr in a range */
    {
        int do_current = 0;         /* delete current buffer? */
        int deleted = 0;            /* number of buffers deleted */
        Bytes errormsg = null;     /* return value */

        if (addr_count == 0)
        {
            do_buffer(command, DOBUF_CURRENT, FORWARD, 0, forceit);
        }
        else
        {
            int bnr;                    /* buffer number */
            if (addr_count == 2)
            {
                if (arg.at(0) != NUL)           /* both range and argument is not allowed */
                    return e_trailing;
                bnr = start_bnr;
            }
            else    /* addr_count == 1 */
                bnr = end_bnr;

            for ( ; !got_int; ui_breakcheck())
            {
                /*
                 * delete the current buffer last, otherwise when the
                 * current buffer is deleted, the next buffer becomes
                 * the current one and will be loaded, which may then
                 * also be deleted, etc.
                 */
                if (bnr == curbuf.b_fnum)
                    do_current = bnr;
                else if (do_buffer(command, DOBUF_FIRST, FORWARD, bnr, forceit) == true)
                    deleted++;

                /*
                 * find next buffer number to delete/unload
                 */
                if (addr_count == 2)
                {
                    if (end_bnr < ++bnr)
                        break;
                }
                else    /* addr_count == 1 */
                {
                    arg = skipwhite(arg);
                    if (arg.at(0) == NUL)
                        break;
                    if (!asc_isdigit(arg.at(0)))
                    {
                        Bytes p = skiptowhite_esc(arg);
                        bnr = buflist_findpat(arg, p, command == DOBUF_WIPE, false);
                        if (bnr < 0)            /* failed */
                            break;
                        arg = p;
                    }
                    else
                    {
                        Bytes[] __ = { arg }; bnr = (int)getdigits(__); arg = __[0];
                    }
                }
            }
            if (!got_int && do_current != 0 && do_buffer(command, DOBUF_FIRST, FORWARD, do_current, forceit) == true)
                deleted++;

            if (deleted == 0)
            {
                if (command == DOBUF_UNLOAD)
                    STRCPY(ioBuff, u8("E515: No buffers were unloaded"));
                else if (command == DOBUF_DEL)
                    STRCPY(ioBuff, u8("E516: No buffers were deleted"));
                else
                    STRCPY(ioBuff, u8("E517: No buffers were wiped out"));
                errormsg = ioBuff;
            }
            else if (p_report[0] <= deleted)
            {
                if (command == DOBUF_UNLOAD)
                {
                    if (deleted == 1)
                        msg(u8("1 buffer unloaded"));
                    else
                        smsg(u8("%d buffers unloaded"), deleted);
                }
                else if (command == DOBUF_DEL)
                {
                    if (deleted == 1)
                        msg(u8("1 buffer deleted"));
                    else
                        smsg(u8("%d buffers deleted"), deleted);
                }
                else
                {
                    if (deleted == 1)
                        msg(u8("1 buffer wiped out"));
                    else
                        smsg(u8("%d buffers wiped out"), deleted);
                }
            }
        }

        return errormsg;
    }

    /*
     * Make the current buffer empty.
     * Used when it is wiped out and it's the last buffer.
     */
    /*private*/ static boolean empty_curbuf(boolean close_others, boolean forceit, int action)
    {
        if (action == DOBUF_UNLOAD)
        {
            emsg(u8("E90: Cannot unload last buffer"));
            return false;
        }

        buffer_C buf = curbuf;

        if (close_others)
        {
            /* Close any other windows on this buffer, then make it empty. */
            close_windows(buf, true);
        }

        setpcmark();
        boolean retval = do_ecmd(0, null, null, null, ECMD_ONE, forceit ? ECMD_FORCEIT : 0, curwin);

        /*
         * do_ecmd() may create a new buffer, then we have to delete the old one.
         * But do_ecmd() may have done that already, check if the buffer still exists.
         */
        if (buf != curbuf && buf_valid(buf) && buf.b_nwindows == 0)
            close_buffer(null, buf, action, false);
        if (!close_others)
            need_fileinfo = false;

        return retval;
    }
    /*
     * Implementation of the commands for the buffer list.
     *
     * action == DOBUF_GOTO     go to specified buffer
     * action == DOBUF_SPLIT    split window and go to specified buffer
     * action == DOBUF_UNLOAD   unload specified buffer(s)
     * action == DOBUF_DEL      delete specified buffer(s) from buffer list
     * action == DOBUF_WIPE     delete specified buffer(s) really
     *
     * start == DOBUF_CURRENT   go to "count" buffer from current buffer
     * start == DOBUF_FIRST     go to "count" buffer from first buffer
     * start == DOBUF_LAST      go to "count" buffer from last buffer
     * start == DOBUF_MOD       go to "count" modified buffer from current buffer
     *
     * Return false or true.
     */
    /*private*/ static boolean do_buffer(int action, int start, int dir, int count, boolean forceit)
        /* dir: FORWARD or BACKWARD */
        /* count: buffer number or number of buffers */
        /* forceit: true for :...! */
    {
        boolean unload = (action == DOBUF_UNLOAD || action == DOBUF_DEL || action == DOBUF_WIPE);

        buffer_C buf;
        switch (start)
        {
            case DOBUF_FIRST:   buf = firstbuf; break;
            case DOBUF_LAST:    buf = lastbuf;  break;
            default:            buf = curbuf;   break;
        }
        if (start == DOBUF_MOD)         /* find next modified buffer */
        {
            while (0 < count--)
            {
                do
                {
                    buf = buf.b_next;
                    if (buf == null)
                        buf = firstbuf;
                } while (buf != curbuf && !bufIsChanged(buf));
            }
            if (!bufIsChanged(buf))
            {
                emsg(u8("E84: No modified buffer found"));
                return false;
            }
        }
        else if (start == DOBUF_FIRST && count != 0) /* find specified buffer number */
        {
            while (buf != null && buf.b_fnum != count)
                buf = buf.b_next;
        }
        else
        {
            for (buffer_C bp = null; 0 < count || (!unload && !buf.b_p_bl[0] && bp != buf); )
            {
                /* Remember the buffer where we start,
                 * we come back there when all buffers are unlisted. */
                if (bp == null)
                    bp = buf;
                if (dir == FORWARD)
                {
                    buf = buf.b_next;
                    if (buf == null)
                        buf = firstbuf;
                }
                else
                {
                    buf = buf.b_prev;
                    if (buf == null)
                        buf = lastbuf;
                }
                /* don't count unlisted buffers */
                if (unload || buf.b_p_bl[0])
                {
                    --count;
                    bp = null;      /* use this buffer as new starting point */
                }
                if (bp == buf)
                {
                    /* back where we started, didn't find anything. */
                    emsg(u8("E85: There is no listed buffer"));
                    return false;
                }
            }
        }

        if (buf == null)        /* could not find it */
        {
            if (start == DOBUF_FIRST)
            {
                /* don't warn when deleting */
                if (!unload)
                    emsgn(e_nobufnr, (long)count);
            }
            else if (dir == FORWARD)
                emsg(u8("E87: Cannot go beyond last buffer"));
            else
                emsg(u8("E88: Cannot go before first buffer"));
            return false;
        }

        /*
         * delete buffer buf from memory and/or the list
         */
        if (unload)
        {
            /* When unloading or deleting a buffer that's already unloaded and unlisted: fail silently. */
            if (action != DOBUF_WIPE && buf.b_ml.ml_mfp == null && !buf.b_p_bl[0])
                return false;

            if (!forceit && bufIsChanged(buf))
            {
                if ((p_confirm[0] || cmdmod.confirm) && p_write[0])
                {
                    dialog_changed(buf, false);
                    if (!buf_valid(buf))
                        /* Autocommand deleted buffer, oops!  It's not changed now. */
                        return false;
                    /* If it's still changed, fail silently.  The dialog already mentioned why it fails. */
                    if (bufIsChanged(buf))
                        return false;
                }
                else
                {
                    emsgn(u8("E89: No write since last change for buffer %ld (add ! to override)"), (long)buf.b_fnum);
                    return false;
                }
            }

            /*
             * If deleting the last (listed) buffer, make it empty.
             * The last (listed) buffer cannot be unloaded.
             */
            buffer_C bp;
            for (bp = firstbuf; bp != null; bp = bp.b_next)
                if (bp.b_p_bl[0] && bp != buf)
                    break;
            if (bp == null && buf == curbuf)
                return empty_curbuf(true, forceit, action);

            /*
             * If the deleted buffer is the current one, close the current window
             * (unless it's the only window).  Repeat this so long as we end up in
             * a window with this buffer.
             */
            while (buf == curbuf
                       && !(curwin.w_closing || curwin.w_buffer.b_closing)
                       && (firstwin != lastwin || first_tabpage.tp_next != null))
            {
                if (win_close(curwin, false) == false)
                    break;
            }

            /*
             * If the buffer to be deleted is not the current one, delete it here.
             */
            if (buf != curbuf)
            {
                close_windows(buf, false);
                if (buf != curbuf && buf_valid(buf) && buf.b_nwindows <= 0)
                    close_buffer(null, buf, action, false);
                return true;
            }

            /*
             * Deleting the current buffer: Need to find another buffer to go to.
             * There should be another, otherwise it would have been handled
             * above.  However, autocommands may have deleted all buffers.
             * First use au_new_curbuf, if it is valid.
             * Then prefer the buffer we most recently visited.
             * Else try to find one that is loaded, after the current buffer,
             * then before the current buffer.
             * Finally use any buffer.
             */
            buf = null;     /* selected buffer */
            bp = null;      /* used when no loaded buffer found */
            if (au_new_curbuf != null && buf_valid(au_new_curbuf))
                buf = au_new_curbuf;
            else if (0 < curwin.w_jumplistlen)
            {
                int jumpidx = curwin.w_jumplistidx - 1;
                if (jumpidx < 0)
                    jumpidx = curwin.w_jumplistlen - 1;

                for (int stop = jumpidx; jumpidx != curwin.w_jumplistidx; )
                {
                    buf = buflist_findnr(curwin.w_jumplist[jumpidx].fmark.fnum);
                    if (buf != null)
                    {
                        if (buf == curbuf || !buf.b_p_bl[0])
                            buf = null;     /* skip current and unlisted bufs */
                        else if (buf.b_ml.ml_mfp == null)
                        {
                            /* skip unloaded buf, but may keep it for later */
                            if (bp == null)
                                bp = buf;
                            buf = null;
                        }
                    }
                    if (buf != null)    /* found a valid buffer: stop searching */
                        break;
                    /* advance to older entry in jump list */
                    if (jumpidx == 0 && curwin.w_jumplistidx == curwin.w_jumplistlen)
                        break;
                    if (--jumpidx < 0)
                        jumpidx = curwin.w_jumplistlen - 1;
                    if (jumpidx == stop)         /* list exhausted for sure */
                        break;
                }
            }

            if (buf == null)            /* No previous buffer, Try 2'nd approach */
            {
                boolean forward = true;
                buf = curbuf.b_next;
                for ( ; ; )
                {
                    if (buf == null)
                    {
                        if (!forward)       /* tried both directions */
                            break;
                        buf = curbuf.b_prev;
                        forward = false;
                        continue;
                    }
                    /* in non-help buffer, try to skip help buffers, and vv */
                    if (buf.b_p_bl[0])
                    {
                        if (buf.b_ml.ml_mfp != null)    /* found loaded buffer */
                            break;
                        if (bp == null)     /* remember unloaded buf for later */
                            bp = buf;
                    }
                    if (forward)
                        buf = buf.b_next;
                    else
                        buf = buf.b_prev;
                }
            }
            if (buf == null)        /* no loaded buffer, use unloaded one */
                buf = bp;
            if (buf == null)        /* no loaded buffer, find listed one */
            {
                for (buf = firstbuf; buf != null; buf = buf.b_next)
                    if (buf.b_p_bl[0] && buf != curbuf)
                        break;
            }
            if (buf == null)        /* Still no buffer, just take one */
            {
                if (curbuf.b_next != null)
                    buf = curbuf.b_next;
                else
                    buf = curbuf.b_prev;
            }
        }

        if (buf == null)
        {
            /* Autocommands must have wiped out all other buffers.
             * Only option now is to make the current buffer empty. */
            return empty_curbuf(false, forceit, action);
        }

        /*
         * make buf current buffer
         */
        if (action == DOBUF_SPLIT)      /* split window first */
        {
            /* If 'switchbuf' contains "useopen": jump to first window containing
             * "buf" if one exists. */
            if ((swb_flags[0] & SWB_USEOPEN) != 0 && buf_jump_open_win(buf) != null)
                return true;
            /* If 'switchbuf' contains "usetab": jump to first window in any tab
             * page containing "buf" if one exists. */
            if ((swb_flags[0] & SWB_USETAB) != 0 && buf_jump_open_tab(buf) != null)
                return true;
            if (win_split(0, 0) == false)
                return false;
        }

        /* go to current buffer - nothing to do */
        if (buf == curbuf)
            return true;

        /*
         * Check if the current buffer may be abandoned.
         */
        if (action == DOBUF_GOTO && !can_abandon(curbuf, forceit))
        {
            if ((p_confirm[0] || cmdmod.confirm) && p_write[0])
            {
                dialog_changed(curbuf, false);
                if (!buf_valid(buf))
                    /* Autocommand deleted buffer, oops! */
                    return false;
            }
            if (bufIsChanged(curbuf))
            {
                emsg(e_nowrtmsg);
                return false;
            }
        }

        /* Go to the other buffer. */
        set_curbuf(buf, action);

        if (action == DOBUF_SPLIT)
        {
            curwin.w_onebuf_opt.wo_scb[0] = false;    /* reset 'scrollbind' and 'cursorbind' */
            curwin.w_onebuf_opt.wo_crb[0] = false;
        }

        if (aborting())         /* autocmds may abort script processing */
            return false;

        return true;
    }

    /*
     * Set current buffer to "buf".  Executes autocommands and closes current
     * buffer.  "action" tells how to close the current buffer:
     * DOBUF_GOTO       free or hide it
     * DOBUF_SPLIT      nothing
     * DOBUF_UNLOAD     unload it
     * DOBUF_DEL        delete it
     * DOBUF_WIPE       wipe it out
     */
    /*private*/ static void set_curbuf(buffer_C buf, int action)
    {
        buffer_C prevbuf;
        boolean unload = (action == DOBUF_UNLOAD || action == DOBUF_DEL || action == DOBUF_WIPE);
        long old_tw = curbuf.b_p_tw[0];

        setpcmark();
        if (!cmdmod.keepalt)
            curwin.w_alt_fnum = curbuf.b_fnum;      /* remember alternate file */
        buflist_altfpos(curwin);                    /* remember curpos */

        /* Don't restart Select mode after switching to another buffer. */
        VIsual_reselect = false;

        /* close_windows() or apply_autocmds() may change curbuf */
        prevbuf = curbuf;

        apply_autocmds(EVENT_BUFLEAVE, null, null, false, curbuf);
        if (buf_valid(prevbuf) && !aborting())
        {
            if (prevbuf == curwin.w_buffer)
                reset_synblock(curwin);
            if (unload)
                close_windows(prevbuf, false);
            if (buf_valid(prevbuf) && !aborting())
            {
                window_C previouswin = curwin;
                if (prevbuf == curbuf)
                    u_sync(false);
                close_buffer(prevbuf == curwin.w_buffer ? curwin : null, prevbuf,
                        unload ? action : (action == DOBUF_GOTO
                            && !P_HID(prevbuf)
                            && !bufIsChanged(prevbuf)) ? DOBUF_UNLOAD : 0, false);
                if (curwin != previouswin && win_valid(previouswin))
                    /* autocommands changed curwin, Grr! */
                    curwin = previouswin;
            }
        }
        /* An autocommand may have deleted "buf", already entered it
         * (e.g., when it did ":bunload") or aborted the script processing!
         * If curwin.w_buffer is null, enter_buffer() will make it valid again */
        if ((buf_valid(buf) && buf != curbuf && !aborting()) || curwin.w_buffer == null)
        {
            enter_buffer(buf);
            if (old_tw != curbuf.b_p_tw[0])
                check_colorcolumn(curwin);
        }
    }

    /*
     * Enter a new current buffer.
     * Old curbuf must have been abandoned already!  This also means "curbuf" may
     * be pointing to freed memory.
     */
    /*private*/ static void enter_buffer(buffer_C buf)
    {
        /* Copy buffer and window local option values. */
        buf_copy_options(buf, BCO_ENTER);
        get_winopts(buf);

        /* Get the buffer in the current window. */
        curwin.w_buffer = buf;
        curbuf = buf;
        curbuf.b_nwindows++;

        curwin.w_s = buf.b_s;

        /* Cursor on first line by default. */
        curwin.w_cursor.lnum = 1;
        curwin.w_cursor.col = 0;
        curwin.w_cursor.coladd = 0;
        curwin.w_set_curswant = true;
        curwin.w_topline_was_set = false;

        /* mark cursor position as being invalid */
        curwin.w_valid = 0;

        /* Make sure the buffer is loaded. */
        if (curbuf.b_ml.ml_mfp == null)     /* need to load the file */
        {
            /* If there is no filetype, allow for detecting one.  Esp. useful for
             * ":ball" used in a autocommand.  If there already is a filetype we
             * might prefer to keep it. */
            if (curbuf.b_p_ft[0].at(0) == NUL)
                did_filetype = false;

            open_buffer(false, null, 0);
        }
        else
        {
            if (msg_silent == 0)
                need_fileinfo = true;       /* display file info after redraw */
            buf_check_timestamp(curbuf);    /* check if file changed */
            curwin.w_topline = 1;
            apply_autocmds(EVENT_BUFENTER, null, null, false, curbuf);
            apply_autocmds(EVENT_BUFWINENTER, null, null, false, curbuf);
        }

        /* If autocommands did not change the cursor position,
         * restore cursor lnum and possibly cursor col. */
        if (curwin.w_cursor.lnum == 1 && inindent(0))
            buflist_getfpos();

        check_arg_idx(curwin);              /* check for valid arg_idx */

        /* when autocmds didn't change it */
        if (curwin.w_topline == 1 && !curwin.w_topline_was_set)
            scroll_cursor_halfway(false);   /* redisplay at correct position */

        redraw_later(NOT_VALID);
    }

    /*
     * functions for dealing with the buffer list
     */

    /*
     * Add a file name to the buffer list.  Return a pointer to the buffer.
     * If the same file name already exists return a pointer to that buffer.
     * If it does not exist, or if fname == null, a new entry is created.
     * If (flags & BLN_CURBUF) is true, may use current buffer.
     * If (flags & BLN_LISTED) is true, add new buffer to buffer list.
     * If (flags & BLN_DUMMY) is true, don't count it as a real buffer.
     * This is the ONLY way to create a new buffer.
     */
    /*private*/ static int  top_file_num = 1;       /* highest file number */

    /*private*/ static buffer_C buflist_new(Bytes _ffname, Bytes _sfname, long lnum, int flags)
        /* ffname: full path of fname or relative */
        /* sfname: short fname or null */
        /* lnum: preferred cursor line */
        /* flags: BLN_ defines */
    {
        Bytes[] ffname = { _ffname };
        Bytes[] sfname = { _sfname };
        fname_expand(ffname, sfname);         /* will allocate "ffname" */

        /*
         * If file name already exists in the list, update the entry.
         */
        /* On Unix we can use inode numbers when the file exists.  Works better for hard links. */
        stat_C st = new stat_C();
        if (sfname[0] == null || libC.stat(sfname[0], st) < 0)
            st.st_dev(-1);

        buffer_C buf;
        if (ffname[0] != null && (flags & BLN_DUMMY) == 0 && (buf = buflist_findname_stat(ffname[0], st)) != null)
        {
            if (lnum != 0)
                buflist_setfpos(buf, curwin, lnum, 0, false);
            /* copy the options now, if 'cpo' doesn't have 's' and not done already */
            buf_copy_options(buf, 0);
            if ((flags & BLN_LISTED) != 0 && !buf.b_p_bl[0])
            {
                buf.b_p_bl[0] = true;
                if ((flags & BLN_DUMMY) == 0)
                {
                    apply_autocmds(EVENT_BUFADD, null, null, false, buf);
                    if (!buf_valid(buf))
                        return null;
                }
            }
            return buf;
        }

        /*
         * If the current buffer has no name and no contents, use the current buffer.
         * Otherwise: Need to allocate a new buffer structure.
         *
         * This is the ONLY place where a new buffer structure is allocated!
         */
        buf = null;
        if ((flags & BLN_CURBUF) != 0
                && curbuf != null
                && curbuf.b_ffname == null
                && curbuf.b_nwindows <= 1
                && (curbuf.b_ml.ml_mfp == null || bufempty()))
        {
            buf = curbuf;
            /* It's like this buffer is deleted.  Watch out for autocommands that
             * change curbuf!  If that happens, allocate a new buffer anyway. */
            if (curbuf.b_p_bl[0])
                apply_autocmds(EVENT_BUFDELETE, null, null, false, curbuf);
            if (buf == curbuf)
                apply_autocmds(EVENT_BUFWIPEOUT, null, null, false, curbuf);
            if (aborting())         /* autocmds may abort script processing */
                return null;
        }
        if (buf != curbuf || curbuf == null)
        {
            buf = new buffer_C();

            buf.b_ml = new memline_C();
            buf.b_namedm = ARRAY_pos(NMARKS);
            buf.b_visual = new visualinfo_C();
            buf.b_last_cursor = new pos_C();
            buf.b_last_insert = new pos_C();
            buf.b_last_change = new pos_C();
            buf.b_changelist = ARRAY_pos(JUMPLISTSIZE);
            buf.b_chartab = new int[8];
            buf.b_maphash = new mapblock_C[256][1];
            buf.b_ucmds = new Growing<ucmd_C>(ucmd_C.class, 4);
            buf.b_op_start = new pos_C();
            buf.b_op_start_orig = new pos_C();
            buf.b_op_end = new pos_C();
            buf.b_p_scriptID = new int[BV_COUNT];
            buf.b_bufvar = new dictitem_C();
            buf.b_s = new synblock_C();

            /* init b: variables */
            buf.b_vars = newDict();
            init_var_dict(buf.b_vars, buf.b_bufvar, VAR_SCOPE);
        }

        if (ffname[0] != null)
        {
            buf.b_ffname = ffname[0];
            buf.b_sfname = STRDUP(sfname[0]);
        }

        clear_wininfo(buf);
        buf.b_wininfo = new wininfo_C();

        if (ffname[0] != null && (buf.b_ffname == null || buf.b_sfname == null))
        {
            buf.b_ffname = null;
            buf.b_sfname = null;
            if (buf != curbuf)
                free_buffer(buf);
            return null;
        }

        if (buf == curbuf)
        {
            /* free all things allocated for this buffer */
            buf_freeall(buf, 0);
            if (buf != curbuf)              /* autocommands deleted the buffer! */
                return null;
            if (aborting())                 /* autocmds may abort script processing */
                return null;
            free_buffer_stuff(buf, false);  /* delete local variables et al. */

            /* Init the options. */
            buf.b_p_initialized = false;
            buf_copy_options(buf, BCO_ENTER);
        }
        else
        {
            /*
             * put new buffer at the end of the buffer list
             */
            buf.b_next = null;
            if (firstbuf == null)           /* buffer list is empty */
            {
                buf.b_prev = null;
                firstbuf = buf;
            }
            else                            /* append new buffer at end of list */
            {
                lastbuf.b_next = buf;
                buf.b_prev = lastbuf;
            }
            lastbuf = buf;

            buf.b_fnum = top_file_num++;
            if (top_file_num < 0)           /* wrap around (may cause duplicates) */
            {
                emsg(u8("W14: Warning: List of file names overflow"));
                if (emsg_silent == 0)
                {
                    out_flush();
                    ui_delay(3000L, true);  /* make sure it is noticed */
                }
                top_file_num = 1;
            }

            /*
             * Always copy the options from the current buffer.
             */
            buf_copy_options(buf, BCO_ALWAYS);
        }

        buf.b_wininfo.wi_fpos.lnum = lnum;
        buf.b_wininfo.wi_win = curwin;

        hash_init(buf.b_s.b_keywtab);
        hash_init(buf.b_s.b_keywtab_ic);

        buf.b_fname = buf.b_sfname;
        if (st.st_dev() == -1)
            buf.b_dev_valid = false;
        else
        {
            buf.b_dev_valid = true;
            buf.b_dev = st.st_dev();
            buf.b_ino = st.st_ino();
        }
        buf.b_u_synced = true;
        buf.b_flags = BF_CHECK_RO | BF_NEVERLOADED;
        if ((flags & BLN_DUMMY) != 0)
            buf.b_flags |= BF_DUMMY;
        buf_clear_file(buf);
        clrallmarks(buf);                           /* clear marks */
        fmarks_check_names(buf);                    /* check file marks for this file */
        buf.b_p_bl[0] = ((flags & BLN_LISTED) != 0);   /* init 'buflisted' */
        if ((flags & BLN_DUMMY) == 0)
        {
            /* Tricky: these autocommands may change the buffer list.  They could
             * also split the window with re-using the one empty buffer.  This may
             * result in unexpectedly losing the empty buffer. */
            apply_autocmds(EVENT_BUFNEW, null, null, false, buf);
            if (!buf_valid(buf))
                return null;
            if ((flags & BLN_LISTED) != 0)
            {
                apply_autocmds(EVENT_BUFADD, null, null, false, buf);
                if (!buf_valid(buf))
                    return null;
            }
            if (aborting())         /* autocmds may abort script processing */
                return null;
        }

        return buf;
    }

    /*
     * Free the memory for the options of a buffer.
     * If "free_p_ff" is true also free 'fileformat', 'buftype' and 'fileencoding'.
     */
    /*private*/ static void free_buf_options(buffer_C buf, boolean free_p_ff)
    {
        if (free_p_ff)
        {
            clear_string_option(buf.b_p_fenc);
            clear_string_option(buf.b_p_ff);
        }
        clear_string_option(buf.b_p_inde);
        clear_string_option(buf.b_p_indk);
        clear_string_option(buf.b_p_fex);
        clear_string_option(buf.b_p_kp);
        clear_string_option(buf.b_p_mps);
        clear_string_option(buf.b_p_fo);
        clear_string_option(buf.b_p_flp);
        clear_string_option(buf.b_p_isk);
        clear_string_option(buf.b_p_com);
        clear_string_option(buf.b_p_nf);
        clear_string_option(buf.b_p_syn);
        clear_string_option(buf.b_p_ft);
        clear_string_option(buf.b_p_cink);
        clear_string_option(buf.b_p_cino);
        clear_string_option(buf.b_p_cinw);
        clear_string_option(buf.b_p_ep);
        clear_string_option(buf.b_p_qe);
        buf.b_p_ar[0] = -1;
        buf.b_p_ul[0] = NO_LOCAL_UNDOLEVEL;
        clear_string_option(buf.b_p_lw);
    }

    /*
     * get alternate file n
     * set linenr to lnum or altfpos.lnum if lnum == 0
     *      also set cursor column to altfpos.col if 'startofline' is not set.
     * if (options & GETF_SETMARK) call setpcmark()
     * if (options & GETF_ALT) we are jumping to an alternate file.
     * if (options & GETF_SWITCH) respect 'switchbuf' settings when jumping
     *
     * return false for failure, true for success
     */
    /*private*/ static boolean buflist_getfile(int n, long lnum, int options, boolean forceit)
    {
        buffer_C buf = buflist_findnr(n);
        if (buf == null)
        {
            if ((options & GETF_ALT) != 0 && n == 0)
                emsg(e_noalt);
            else
                emsgn(u8("E92: Buffer %ld not found"), (long)n);
            return false;
        }

        /* if alternate file is the current buffer, nothing to do */
        if (buf == curbuf)
            return true;

        if (text_locked())
        {
            text_locked_msg();
            return false;
        }
        if (curbuf_locked())
            return false;

        /* altfpos may be changed by getfile(), get it now */
        int col;
        if (lnum == 0)
        {
            pos_C fpos = buflist_findfpos(buf);
            lnum = fpos.lnum;
            col = fpos.col;
        }
        else
            col = 0;

        window_C wp = null;

        if ((options & GETF_SWITCH) != 0)
        {
            /* If 'switchbuf' contains "useopen": jump to first window containing
             * "buf" if one exists. */
            if ((swb_flags[0] & SWB_USEOPEN) != 0)
                wp = buf_jump_open_win(buf);
            /* If 'switchbuf' contains "usetab": jump to first window in any tab
             * page containing "buf" if one exists. */
            if (wp == null && (swb_flags[0] & SWB_USETAB) != 0)
                wp = buf_jump_open_tab(buf);
            /* If 'switchbuf' contains "split" or "newtab" and the current buffer
             * isn't empty: open new window. */
            if (wp == null && (swb_flags[0] & (SWB_SPLIT | SWB_NEWTAB)) != 0 && !bufempty())
            {
                if ((swb_flags[0] & SWB_NEWTAB) != 0)      /* open in a new tab */
                    tabpage_new();
                else if (win_split(0, 0) == false)      /* open in a new window */
                    return false;
                curwin.w_onebuf_opt.wo_scb[0] = false;
                curwin.w_onebuf_opt.wo_crb[0] = false;
            }
        }

        redrawingDisabled++;
        if (getfile(buf.b_fnum, null, null, (options & GETF_SETMARK) != 0, lnum, forceit) <= 0)
        {
            --redrawingDisabled;

            /* cursor is at to BOL and w_cursor.lnum is checked due to getfile() */
            if (!p_sol[0] && col != 0)
            {
                curwin.w_cursor.col = col;
                check_cursor_col();
                curwin.w_cursor.coladd = 0;
                curwin.w_set_curswant = true;
            }
            return true;
        }
        --redrawingDisabled;
        return false;
    }

    /*
     * go to the last know line number for the current buffer
     */
    /*private*/ static void buflist_getfpos()
    {
        pos_C fpos = buflist_findfpos(curbuf);

        curwin.w_cursor.lnum = fpos.lnum;
        check_cursor_lnum();

        if (p_sol[0])
            curwin.w_cursor.col = 0;
        else
        {
            curwin.w_cursor.col = fpos.col;
            check_cursor_col();
            curwin.w_cursor.coladd = 0;
            curwin.w_set_curswant = true;
        }
    }

    /*
     * Find file in buffer list by name (it has to be for the current window).
     * Returns null if not found.
     */
    /*private*/ static buffer_C buflist_findname_exp(Bytes fname)
    {
        buffer_C buf = null;

        /* First make the name into a full path name. */
        Bytes ffname = fullName_save(fname, true); /* force expansion, get rid of symbolic links */
        if (ffname != null)
            buf = buflist_findname(ffname);

        return buf;
    }

    /*
     * Find file in buffer list by name (it has to be for the current window).
     * "ffname" must have a full path.
     * Skips dummy buffers.
     * Returns null if not found.
     */
    /*private*/ static buffer_C buflist_findname(Bytes ffname)
    {
        stat_C st = new stat_C();

        if (libC.stat(ffname, st) < 0)
            st.st_dev(-1);

        return buflist_findname_stat(ffname, st);
    }

    /*
     * Same as buflist_findname(), but pass the stat structure to avoid getting it twice for the same file.
     * Returns null if not found.
     */
    /*private*/ static buffer_C buflist_findname_stat(Bytes ffname, stat_C st)
    {
        for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
            if ((buf.b_flags & BF_DUMMY) == 0 && !otherfile_buf(buf, ffname, st))
                return buf;

        return null;
    }

    /*
     * Find file in buffer list by a regexp pattern.
     * Return fnum of the found buffer.
     * Return < 0 for error.
     */
    /*private*/ static int buflist_findpat(Bytes pattern, Bytes pattern_end, boolean unlisted, boolean curtab_only)
        /* pattern_end: pointer to first char after pattern */
        /* unlisted: find unlisted buffers */
        /* curtab_only: find buffers in current tab only */
    {
        int match = -1;
        if (BEQ(pattern_end, pattern.plus(1)) && (pattern.at(0) == (byte)'%' || pattern.at(0) == (byte)'#'))
        {
            if (pattern.at(0) == (byte)'%')
                match = curbuf.b_fnum;
            else
                match = curwin.w_alt_fnum;
        }

        /*
         * Try four ways of matching a listed buffer:
         * attempt == 0: without '^' or '$' (at any position)
         * attempt == 1: with '^' at start (only at position 0)
         * attempt == 2: with '$' at end (only match at end)
         * attempt == 3: with '^' at start and '$' at end (only full match)
         * Repeat this for finding an unlisted buffer if there was no matching listed buffer.
         */
        else
        {
            Bytes pat = file_pat_to_reg_pat(pattern, pattern_end, null);
            if (pat == null)
                return -1;
            Bytes patend = pat.plus(strlen(pat) - 1);
            boolean toggledollar = (BLT(pat, patend) && patend.at(0) == (byte)'$');

            /* First try finding a listed buffer.
             * If not found and "unlisted" is true, try finding an unlisted buffer. */
            boolean find_listed = true;
            for ( ; ; )
            {
                for (int attempt = 0; attempt <= 3; attempt++)
                {
                    regmatch_C regmatch = new regmatch_C();

                    /* may add '^' and '$' */
                    if (toggledollar)
                        patend.be(0, (attempt < 2) ? NUL : (byte)'$');    /* add/remove '$' */
                    Bytes p = pat;
                    if (p.at(0) == (byte)'^' && (attempt & 1) == 0)        /* add/remove '^' */
                        p = p.plus(1);

                    regmatch.regprog = vim_regcomp(p, p_magic[0] ? RE_MAGIC : 0);
                    if (regmatch.regprog == null)
                        return -1;

                    for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
                        if (buf.b_p_bl[0] == find_listed && buflist_match(regmatch, buf, false) != null)
                        {
                            if (curtab_only)
                            {
                                /* Ignore the match if the buffer is not open in the current tab. */
                                window_C wp;
                                for (wp = firstwin; wp != null; wp = wp.w_next)
                                    if (wp.w_buffer == buf)
                                        break;
                                if (wp == null)
                                    continue;
                            }
                            if (0 <= match)         /* already found a match */
                            {
                                match = -2;
                                break;
                            }
                            match = buf.b_fnum;     /* remember first match */
                        }

                    if (0 <= match)                 /* found one match */
                        break;
                }

                /* Only search for unlisted buffers if there was no match with a listed buffer. */
                if (!unlisted || !find_listed || match != -1)
                    break;
                find_listed = false;
            }
        }

        if (match == -2)
            emsg2(u8("E93: More than one match for %s"), pattern);
        else if (match < 0)
            emsg2(u8("E94: No matching buffer for %s"), pattern);
        return match;
    }

    /*
     * Find all buffer names that match.
     * For command line expansion of ":buf" and ":sbuf".
     * Return true if matches found, false otherwise.
     */
    /*private*/ static boolean expandBufnames(Bytes pat, int[] num_file, Bytes[][] file, int _options)
    {
        int count = 0;

        num_file[0] = 0;                  /* return values in case of FAIL */
        file[0] = null;

        /* Make a copy of "pat" and change "^" to "\(^\|[\/]\)". */
        Bytes patc;
        if (pat.at(0) == (byte)'^')
        {
            patc = new Bytes(strlen(pat) + 11);

            STRCPY(patc, u8("\\(^\\|[\\/]\\)"));
            STRCPY(patc.plus(11), pat.plus(1));
        }
        else
            patc = pat;

        regmatch_C regmatch = new regmatch_C();

        /*
         * attempt == 0: try match with    '\<', match at start of word
         * attempt == 1: try match without '\<', match anywhere
         */
        for (int attempt = 0; attempt <= 1; attempt++)
        {
            if (0 < attempt && BEQ(patc, pat))
                break;      /* there was no anchor, no need to try again */

            regmatch.regprog = vim_regcomp(patc.plus(attempt * 11), RE_MAGIC);
            if (regmatch.regprog == null)
                return false;

            /*
             * round == 1: Count the matches.
             * round == 2: Build the array to keep the matches.
             */
            for (int round = 1; round <= 2; round++)
            {
                count = 0;
                for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
                {
                    if (!buf.b_p_bl[0])        /* skip unlisted buffers */
                        continue;
                    Bytes p = buflist_match(regmatch, buf, p_wic[0]);
                    if (p != null)
                    {
                        if (round == 1)
                            count++;
                        else
                            file[0][count++] = STRDUP(p);
                    }
                }
                if (count == 0)             /* no match found, break here */
                    break;
                if (round == 1)
                    file[0] = new Bytes[count];
            }

            if (count != 0)                 /* match(es) found, break here */
                break;
        }

        num_file[0] = count;
        return (count != 0);
    }

    /*
     * Check for a match on the file name for buffer "buf" with regprog "prog".
     */
    /*private*/ static Bytes buflist_match(regmatch_C rmp, buffer_C buf, boolean ignore_case)
    {
        Bytes match;

        /* First try the short file name, then the long file name. */
        match = fname_match(rmp, buf.b_sfname, ignore_case);
        if (match == null)
            match = fname_match(rmp, buf.b_ffname, ignore_case);

        return match;
    }

    /*
     * Try matching the regexp in "prog" with file name "name".
     * Return "name" when there is a match, null when not.
     */
    /*private*/ static Bytes fname_match(regmatch_C rmp, Bytes name, boolean ignore_case)
    {
        if (name != null)
        {
            rmp.rm_ic = ignore_case;
            if (vim_regexec(rmp, name, 0))
                return name;
        }

        return null;
    }

    /*
     * find file in buffer list by number
     */
    /*private*/ static buffer_C buflist_findnr(int nr)
    {
        if (nr == 0)
            nr = curwin.w_alt_fnum;

        for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
            if (buf.b_fnum == nr)
                return buf;

        return null;
    }

    /*
     * Get name of file 'n' in the buffer list.
     * When the file has no name an empty string is returned.
     * Returns a pointer to allocated memory, of null when failed.
     */
    /*private*/ static Bytes buflist_nr2name(int n, boolean fullname)
    {
        buffer_C buf = buflist_findnr(n);
        if (buf == null)
            return null;

        return STRDUP(fullname ? buf.b_ffname : buf.b_fname);
    }

    /*
     * Set the "lnum" and "col" for the buffer "buf" and the current window.
     * When "copy_options" is true save the local window option values.
     * When "lnum" is 0 only do the options.
     */
    /*private*/ static void buflist_setfpos(buffer_C buf, window_C win, long lnum, int col, boolean copy_options)
    {
        wininfo_C wip;

        for (wip = buf.b_wininfo; wip != null; wip = wip.wi_next)
            if (wip.wi_win == win)
                break;
        if (wip == null)
        {
            /* allocate a new entry */
            wip = new wininfo_C();
            wip.wi_win = win;
            if (lnum == 0)          /* set lnum even when it's 0 */
                lnum = 1;
        }
        else
        {
            /* remove the entry from the list */
            if (wip.wi_prev != null)
                wip.wi_prev.wi_next = wip.wi_next;
            else
                buf.b_wininfo = wip.wi_next;
            if (wip.wi_next != null)
                wip.wi_next.wi_prev = wip.wi_prev;
            if (copy_options && wip.wi_optset)
                clear_winopt(wip.wi_opt);
        }
        if (lnum != 0)
        {
            wip.wi_fpos.lnum = lnum;
            wip.wi_fpos.col = col;
        }
        if (copy_options)
        {
            /* Save the window-specific option values. */
            copy_winopt(win.w_onebuf_opt, wip.wi_opt);
            wip.wi_optset = true;
        }

        /* insert the entry in front of the list */
        wip.wi_next = buf.b_wininfo;
        buf.b_wininfo = wip;
        wip.wi_prev = null;
        if (wip.wi_next != null)
            wip.wi_next.wi_prev = wip;
    }

    /*
     * Find info for the current window in buffer "buf".
     * If not found, return the info for the most recently used window.
     * Returns null when there isn't any info.
     */
    /*private*/ static wininfo_C find_wininfo(buffer_C buf)
    {
        wininfo_C wip;

        for (wip = buf.b_wininfo; wip != null; wip = wip.wi_next)
            if (wip.wi_win == curwin)
                break;

        /* If no wininfo for curwin, use the first in the list
         * (that doesn't have 'diff' set and is in another tab page). */
        if (wip == null)
            wip = buf.b_wininfo;

        return wip;
    }

    /*
     * Reset the local window options to the values last used in this window.
     * If the buffer wasn't used in this window before, use the values from
     * the most recently used window.  If the values were never set, use the
     * global values for the window.
     */
    /*private*/ static void get_winopts(buffer_C buf)
    {
        clear_winopt(curwin.w_onebuf_opt);

        wininfo_C wip = find_wininfo(buf);
        if (wip != null && wip.wi_optset)
            copy_winopt(wip.wi_opt, curwin.w_onebuf_opt);
        else
            copy_winopt(curwin.w_allbuf_opt, curwin.w_onebuf_opt);

        check_colorcolumn(curwin);
    }

    /*private*/ static pos_C no_position = new_pos(1, 0, 0);

    /*
     * Find the position (lnum and col) for the buffer 'buf' for the current window.
     * Returns a pointer to no_position if no position is found.
     */
    /*private*/ static pos_C buflist_findfpos(buffer_C buf)
    {
        wininfo_C wip = find_wininfo(buf);
        if (wip != null)
            return wip.wi_fpos;
        else
            return no_position;
    }

    /*
     * Find the lnum for the buffer 'buf' for the current window.
     */
    /*private*/ static long buflist_findlnum(buffer_C buf)
    {
        return buflist_findfpos(buf).lnum;
    }

    /*
     * List all know file names (for :files and :buffers command).
     */
    /*private*/ static final ex_func_C ex_buflist = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            for (buffer_C buf = firstbuf; buf != null && !got_int; buf = buf.b_next)
            {
                /* skip unlisted buffers, unless ! was used */
                if (!buf.b_p_bl[0] && !eap.forceit)
                    continue;
                msg_putchar('\n');

                int len = vim_snprintf(ioBuff, IOSIZE - 20, u8("%3d%c%c%c%c%c \"%s\""),
                        buf.b_fnum,
                        buf.b_p_bl[0] ? ' ' : 'u',
                        (buf == curbuf) ? '%' : (curwin.w_alt_fnum == buf.b_fnum ? '#' : ' '),
                        (buf.b_ml.ml_mfp == null) ? ' ' : (buf.b_nwindows == 0 ? 'h' : 'a'),
                        !buf.b_p_ma[0] ? '-' : (buf.b_p_ro[0] ? '=' : ' '),
                        (buf.b_flags & BF_READERR) != 0 ? 'x' : (bufIsChanged(buf) ? '+' : ' '),
                        buf_spname(buf, false));

                /* put "line 999" in column 40 or after the file name */
                int i = 40 - mb_string2cells(ioBuff, -1);
                do
                {
                    ioBuff.be(len++, (byte)' ');
                } while (0 < --i && len < IOSIZE - 18);
                vim_snprintf(ioBuff.plus(len), IOSIZE - len, u8("line %ld"),
                    (buf == curbuf) ? curwin.w_cursor.lnum : buflist_findlnum(buf));

                msg_outtrans(ioBuff);
                out_flush();        /* output one line at a time */
                ui_breakcheck();
            }
        }
    };

    /*
     * Get file name and line number for file 'fnum'.
     * Used by DoOneCmd() for translating '%' and '#'.
     * Used by insert_reg() and cmdline_paste() for '#' register.
     * Return false if not found, true for success.
     */
    /*private*/ static boolean buflist_name_nr(int fnum, Bytes[] fname, long[] lnum)
    {
        buffer_C buf = buflist_findnr(fnum);
        if (buf == null || buf.b_fname == null)
            return false;

        fname[0] = buf.b_fname;
        lnum[0] = buflist_findlnum(buf);

        return true;
    }

    /*
     * Set the file name for "buf"' to 'ffname', short file name to 'sfname'.
     * The file name with the full path is also remembered, for when :cd is used.
     * Returns false for failure (file name already in use by other buffer)
     *      true otherwise.
     */
    /*private*/ static boolean setfname(buffer_C buf, Bytes _ffname, Bytes _sfname, boolean message)
        /* message: give message when buffer already exists */
    {
        Bytes[] ffname = { _ffname };
        Bytes[] sfname = { _sfname };
        stat_C st = new stat_C();

        if (ffname[0] == null || ffname[0].at(0) == NUL)
        {
            /* Removing the name. */
            buf.b_ffname = null;
            buf.b_sfname = null;
            st.st_dev(-1);
        }
        else
        {
            buffer_C obuf = null;

            fname_expand(ffname, sfname); /* will allocate "ffname" */
            if (ffname[0] == null)                 /* out of memory */
                return false;

            /*
             * if the file name is already used in another buffer:
             * - if the buffer is loaded, fail
             * - if the buffer is not loaded, delete it from the list
             */
            if (libC.stat(ffname[0], st) < 0)
                st.st_dev(-1);
            if ((buf.b_flags & BF_DUMMY) == 0)
                obuf = buflist_findname_stat(ffname[0], st);
            if (obuf != null && obuf != buf)
            {
                if (obuf.b_ml.ml_mfp != null)   /* it's loaded, fail */
                {
                    if (message)
                        emsg(u8("E95: Buffer with this name already exists"));
                    return false;
                }
                /* delete from the list */
                close_buffer(null, obuf, DOBUF_WIPE, false);
            }
            sfname[0] = STRDUP(sfname[0]);
            if (ffname[0] == null || sfname[0] == null)
                return false;
            buf.b_ffname = ffname[0];
            buf.b_sfname = sfname[0];
        }
        buf.b_fname = buf.b_sfname;
        if (st.st_dev() == -1)
            buf.b_dev_valid = false;
        else
        {
            buf.b_dev_valid = true;
            buf.b_dev = st.st_dev();
            buf.b_ino = st.st_ino();
        }

        buf.b_shortname = false;

        buf_name_changed(buf);
        return true;
    }

    /*
     * Crude way of changing the name of a buffer.  Use with care!
     * The name should be relative to the current directory.
     */
    /*private*/ static void buf_set_name(int fnum, Bytes name)
    {
        buffer_C buf = buflist_findnr(fnum);
        if (buf != null)
        {
            buf.b_ffname = STRDUP(name);
            buf.b_sfname = null;
            /* Allocate ffname and expand into full path.  Also resolves .lnk files on Win32. */
            {
                Bytes[] _1 = { buf.b_ffname };
                Bytes[] _2 = { buf.b_sfname };
                fname_expand(_1, _2);
                buf.b_ffname = _1[0];
                buf.b_sfname = _2[0];
            }
            buf.b_fname = buf.b_sfname;
        }
    }

    /*
     * Take care of what needs to be done when the name of buffer "buf" has changed.
     */
    /*private*/ static void buf_name_changed(buffer_C buf)
    {
        if (curwin.w_buffer == buf)
            check_arg_idx(curwin);      /* check file name for arg list */

        status_redraw_all();            /* status lines need to be redrawn */
        fmarks_check_names(buf);        /* check named file marks */
        ml_timestamp(buf);              /* reset timestamp */
    }

    /*
     * set alternate file name for current window
     *
     * Used by do_one_cmd(), do_write() and do_ecmd().
     * Return the buffer.
     */
    /*private*/ static buffer_C setaltfname(Bytes ffname, Bytes sfname, long lnum)
    {
        /* Create a buffer.  'buflisted' is not set if it's a new buffer. */
        buffer_C buf = buflist_new(ffname, sfname, lnum, 0);
        if (buf != null && !cmdmod.keepalt)
            curwin.w_alt_fnum = buf.b_fnum;
        return buf;
    }

    /*
     * Get alternate file name for current window.
     * Return null if there isn't any, and give error message if requested.
     */
    /*private*/ static Bytes getaltfname(boolean errmsg)
        /* errmsg: give error message */
    {
        Bytes[] fname = new Bytes[1];
        long[] dummy = new long[1];

        if (buflist_name_nr(0, fname, dummy) == false)
        {
            if (errmsg)
                emsg(e_noalt);
            return null;
        }
        return fname[0];
    }

    /*
     * Add a file name to the buflist and return its number.
     * Uses same flags as buflist_new(), except BLN_DUMMY.
     *
     * used by qf_init(), main() and doarglist()
     */
    /*private*/ static int buflist_add(Bytes fname, int flags)
    {
        buffer_C buf = buflist_new(fname, null, 0, flags);
        if (buf != null)
            return buf.b_fnum;

        return 0;
    }

    /*
     * Set alternate cursor position for the current buffer and window "win".
     * Also save the local window option values.
     */
    /*private*/ static void buflist_altfpos(window_C win)
    {
        buflist_setfpos(curbuf, win, win.w_cursor.lnum, win.w_cursor.col, true);
    }

    /*
     * Return true if 'ffname' is not the same file as current file.
     * Fname must have a full path (expanded by mch_fullName()).
     */
    /*private*/ static boolean otherfile(Bytes ffname)
    {
        return otherfile_buf(curbuf, ffname, null);
    }

    /*private*/ static boolean otherfile_buf(buffer_C buf, Bytes ffname, stat_C st)
    {
        /* no name is different */
        if (ffname == null || ffname.at(0) == NUL || buf.b_ffname == null)
            return true;
        if (STRCMP(ffname, buf.b_ffname) == 0)
            return false;

        /* If no struct stat given, get it now. */
        if (st == null)
        {
            st = new stat_C();
            if (!buf.b_dev_valid || libC.stat(ffname, st) < 0)
                st.st_dev(-1);
        }

        /* Use dev/ino to check if the files are the same, even when the names are different
         * (possible with links).  Still need to compare the name above, for when the file
         * doesn't exist yet.
         * Problem: The dev/ino changes when a file is deleted (and created again) and remains
         * the same when renamed/moved.  We don't want to stat() each buffer each time, that
         * would be too slow.  Get the dev/ino again when they appear to match, but not when
         * they appear to be different: Could skip a buffer when it's actually the same file.
         */
        if (buf_same_ino(buf, st))
        {
            buf_setino(buf);
            if (buf_same_ino(buf, st))
                return false;
        }

        return true;
    }

    /*
     * Set inode and device number for a buffer.
     * Must always be called when b_fname is changed!.
     */
    /*private*/ static void buf_setino(buffer_C buf)
    {
        stat_C st = new stat_C();

        if (buf.b_fname != null && 0 <= libC.stat(buf.b_fname, st))
        {
            buf.b_dev_valid = true;
            buf.b_dev = st.st_dev();
            buf.b_ino = st.st_ino();
        }
        else
            buf.b_dev_valid = false;
    }

    /*
     * Return true if dev/ino in buffer "buf" matches with "stp".
     */
    /*private*/ static boolean buf_same_ino(buffer_C buf, stat_C st)
    {
        return (buf.b_dev_valid && st.st_dev() == buf.b_dev && st.st_ino() == buf.b_ino);
    }

    /*
     * Print info about the current buffer.
     */
    /*private*/ static void fileinfo(int fullname, boolean dont_truncate)
        /* fullname: when non-zero print full path */
    {
        Bytes buffer = new Bytes(IOSIZE);

        Bytes p = buffer;
        if (1 < fullname)       /* 2 CTRL-G: include buffer number */
        {
            vim_snprintf(buffer, IOSIZE, u8("buf %d: "), curbuf.b_fnum);
            p = buffer.plus(strlen(buffer));
        }

        (p = p.plus(1)).be(-1, (byte)'"');
        vim_strncpy(p, buf_spname(curbuf, fullname != 0), IOSIZE - BDIFF(p, buffer) - 1);

        vim_snprintf_add(buffer, IOSIZE, u8("\"%s%s%s%s%s%s"),
                curbufIsChanged() ? (shortmess(SHM_MOD) ? u8(" [+]") : u8(" [Modified]")) : u8(" "),
                (curbuf.b_flags & BF_NOTEDITED) != 0 ? u8("[Not edited]") : u8(""),
                (curbuf.b_flags & BF_NEW) != 0 ? u8("[New file]") : u8(""),
                (curbuf.b_flags & BF_READERR) != 0 ? u8("[Read errors]") : u8(""),
                curbuf.b_p_ro[0] ? (shortmess(SHM_RO) ? u8("[RO]") : u8("[readonly]")) : u8(""),
                (curbufIsChanged() || (curbuf.b_flags & BF_WRITE_MASK) != 0 || curbuf.b_p_ro[0]) ? u8(" ") : u8(""));
        /* With 32 bit longs and more than 21,474,836 lines multiplying by 100
         * causes an overflow, thus for large numbers divide instead. */
        int n;
        if (1000000L < curwin.w_cursor.lnum)
            n = (int)(curwin.w_cursor.lnum / (curbuf.b_ml.ml_line_count / 100L));
        else
            n = (int)((curwin.w_cursor.lnum * 100L) / curbuf.b_ml.ml_line_count);
        if ((curbuf.b_ml.ml_flags & ML_EMPTY) != 0)
        {
            vim_snprintf_add(buffer, IOSIZE, u8("%s"), no_lines_msg);
        }
        else if (p_ru[0])
        {
            /* Current line and column are already on the screen. */
            if (curbuf.b_ml.ml_line_count == 1)
                vim_snprintf_add(buffer, IOSIZE, u8("1 line --%d%%--"), n);
            else
                vim_snprintf_add(buffer, IOSIZE, u8("%ld lines --%d%%--"), curbuf.b_ml.ml_line_count, n);
        }
        else
        {
            vim_snprintf_add(buffer, IOSIZE, u8("line %ld of %ld --%d%%-- col "),
                                                curwin.w_cursor.lnum, curbuf.b_ml.ml_line_count, n);
            validate_virtcol();
            int len = strlen(buffer);
            col_print(buffer.plus(len), IOSIZE - len, curwin.w_cursor.col + 1, curwin.w_virtcol + 1);
        }

        append_arg_number(curwin, buffer, IOSIZE, !shortmess(SHM_FILE));

        if (dont_truncate)
        {
            /* Temporarily set msg_scroll to avoid the message being truncated.
             * First call msg_start() to get the message in the right place. */
            msg_start();
            boolean m = msg_scroll;
            msg_scroll = true;
            msg(buffer);
            msg_scroll = m;
        }
        else
        {
            p = msg_trunc_attr(buffer, false, 0);
            if (restart_edit != 0 || (msg_scrolled != 0 && !need_wait_return))
                /* Need to repeat the message after redrawing when:
                 * - When restart_edit is set (otherwise there will be a delay before redrawing).
                 * - When the screen was scrolled but there is no wait-return prompt. */
                set_keep_msg(p, 0);
        }
    }

    /*private*/ static void col_print(Bytes buf, int buflen, int col, int vcol)
    {
        if (col == vcol)
            vim_snprintf(buf, buflen, u8("%d"), col);
        else
            vim_snprintf(buf, buflen, u8("%d-%d"), col, vcol);
    }

    /* enum for "type" below */
    /*private*/ static final int
        Normal    = 0,
        Empty     = 1,
        Group     = 2,
        Middle    = 3,
        Highlight = 4,
        TabPage   = 5,
        Trunc     = 6;

    /*private*/ static final class stl_item_C
    {
        Bytes       start;
        int         minwid;
        int         maxwid;
        int         type;

        /*private*/ stl_item_C()
        {
        }
    }

    /*private*/ static stl_item_C[] ARRAY_stl_item(int n)
    {
        stl_item_C[] a = new stl_item_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new stl_item_C();
        return a;
    }

    /*
     * Build a string from the status line items in "fmt".
     * Return length of string in screen cells.
     *
     * Normally works for window "wp", except when working for 'tabline' then it is "curwin".
     *
     * Items are drawn interspersed with the text that surrounds it
     * Specials: %-<wid>(xxx%) => group, %= => middle marker, %< => truncation
     * Item: %-<minwid>.<maxwid><itemch> All but <itemch> are optional
     *
     * If maxwidth is not zero, the string will be filled at any middle marker
     * or truncated if too long, fillchar is used for all whitespace.
     */
    /*private*/ static int build_stl_str_hl(window_C wp, Bytes out, int outlen, Bytes fmt, boolean use_sandbox, int fillchar, int maxwidth, stl_hlrec_C[] hltab, stl_hlrec_C[] tabtab)
        /* out: buffer to write into != nameBuff */
        /* outlen: length of out[] */
        /* use_sandbox: "fmt" was set insecurely, use sandbox */
        /* hltab: return: HL attributes (can be null) */
        /* tabtab: return: tab page nrs (can be null) */
    {
        int[] groupitem = new int[STL_MAX_ITEM];
        stl_item_C[] item = ARRAY_stl_item(STL_MAX_ITEM);

        final int TMPLEN = 70;
        Bytes tmp = new Bytes(TMPLEN);

        /*
         * When the format starts with "%!" then evaluate it as an expression and
         * use the result as the actual format string.
         */
        Bytes usefmt = fmt;
        if (fmt.at(0) == (byte)'%' && fmt.at(1) == (byte)'!')
        {
            usefmt = eval_to_string_safe(fmt.plus(2), null, use_sandbox);
            if (usefmt == null)
                usefmt = fmt;
        }

        if (fillchar == 0)
            fillchar = ' ';
        /* Can't handle a multi-byte fill character yet. */
        else if (1 < utf_char2len(fillchar))
            fillchar = '-';

        /* Get line and check if empty (cursorpos will show "0-1").
         * Note that 'p' will become invalid when getting another buffer line. */
        Bytes p = ml_get_buf(wp.w_buffer, wp.w_cursor.lnum, false);
        boolean empty_line = (p.at(0) == NUL);

        /* Get the byte value now in case we need it below.
         * This is more efficient than making a copy of the line. */
        int byteval;
        if (strlen(p) < wp.w_cursor.col)
            byteval = 0;
        else
            byteval = us_ptr2char(p.plus(wp.w_cursor.col));

        int groupdepth = 0;
        p = out;
        int curitem = 0;
        boolean prevchar_isflag = true;
        boolean prevchar_isitem = false;

        Bytes s;
        for (s = usefmt; s.at(0) != NUL; )
        {
            if (curitem == STL_MAX_ITEM)
            {
                /* There are too many items.  Add the error code to the statusline
                 * to give the user a hint about what went wrong. */
                if (BLT(p.plus(6), out.plus(outlen)))
                {
                    BCOPY(p, u8(" E541"), 5);
                    p = p.plus(5);
                }
                break;
            }

            if (s.at(0) != NUL && s.at(0) != (byte)'%')
                prevchar_isflag = prevchar_isitem = false;

            /*
             * Handle up to the next '%' or the end.
             */
            while (s.at(0) != NUL && s.at(0) != (byte)'%' && BLT(p.plus(1), out.plus(outlen)))
                (p = p.plus(1)).be(-1, (s = s.plus(1)).at(-1));
            if (s.at(0) == NUL || BLE(out.plus(outlen), p.plus(1)))
                break;

            /*
             * Handle one '%' item.
             */
            s = s.plus(1);
            if (s.at(0) == NUL)  /* ignore trailing % */
                break;
            if (s.at(0) == (byte)'%')
            {
                if (BLE(out.plus(outlen), p.plus(1)))
                    break;
                (p = p.plus(1)).be(-1, (s = s.plus(1)).at(-1));
                prevchar_isflag = prevchar_isitem = false;
                continue;
            }
            if (s.at(0) == STL_MIDDLEMARK)
            {
                s = s.plus(1);
                if (0 < groupdepth)
                    continue;
                item[curitem].type = Middle;
                item[curitem++].start = p;
                continue;
            }
            if (s.at(0) == STL_TRUNCMARK)
            {
                s = s.plus(1);
                item[curitem].type = Trunc;
                item[curitem++].start = p;
                continue;
            }
            if (s.at(0) == (byte)')')
            {
                s = s.plus(1);
                if (groupdepth < 1)
                    continue;
                groupdepth--;

                Bytes t = item[groupitem[groupdepth]].start;
                p.be(0, NUL);
                int l = mb_string2cells(t, -1);
                if (groupitem[groupdepth] + 1 < curitem && item[groupitem[groupdepth]].minwid == 0)
                {
                    /* remove group if all items are empty */
                    int n;
                    for (n = groupitem[groupdepth] + 1; n < curitem; n++)
                        if (item[n].type == Normal)
                            break;
                    if (n == curitem)
                    {
                        p = t;
                        l = 0;
                    }
                }
                if (item[groupitem[groupdepth]].maxwid < l)
                {
                    /* truncate, remove n bytes of text at the start */

                    /* Find the first character that should be included. */
                    int n = 0;
                    while (item[groupitem[groupdepth]].maxwid <= l)
                    {
                        l -= mb_ptr2cells(t.plus(n));
                        n += us_ptr2len_cc(t.plus(n));
                    }

                    t.be(0, (byte)'<');
                    BCOPY(t, 1, t, n, BDIFF(p, t) - n);
                    p = p.minus(n - 1);
                    /* Fill up space left over by half a double-wide char. */
                    while (++l < item[groupitem[groupdepth]].minwid)
                        (p = p.plus(1)).be(-1, fillchar);

                    /* correct the start of the items for the truncation */
                    for (l = groupitem[groupdepth] + 1; l < curitem; l++)
                    {
                        item[l].start = item[l].start.minus(n);
                        if (BLT(item[l].start, t))
                            item[l].start = t;
                    }
                }
                else if (l < Math.abs(item[groupitem[groupdepth]].minwid))
                {
                    /* fill */
                    int n = item[groupitem[groupdepth]].minwid;
                    if (n < 0)
                    {
                        /* fill by appending characters */
                        n = 0 - n;
                        while (l++ < n && BLT(p.plus(1), out.plus(outlen)))
                            (p = p.plus(1)).be(-1, fillchar);
                    }
                    else
                    {
                        /* fill by inserting characters */
                        BCOPY(t, n - l, t, 0, BDIFF(p, t));
                        l = n - l;
                        if (BLE(out.plus(outlen), p.plus(l)))
                            l = BDIFF(out.plus(outlen), p.minus(1));
                        p = p.plus(l);
                        for (n = groupitem[groupdepth] + 1; n < curitem; n++)
                            item[n].start = item[n].start.plus(l);
                        for ( ; 0 < l; l--)
                            (t = t.plus(1)).be(-1, fillchar);
                    }
                }
                continue;
            }
            int minwid = 0, maxwid = 9999;
            boolean zeropad = false;
            int l = 1;
            if (s.at(0) == (byte)'0')
            {
                s = s.plus(1);
                zeropad = true;
            }
            if (s.at(0) == (byte)'-')
            {
                s = s.plus(1);
                l = -1;
            }
            if (asc_isdigit(s.at(0)))
            {
                { Bytes[] __ = { s }; minwid = (int)getdigits(__); s = __[0]; }
                if (minwid < 0)     /* overflow */
                    minwid = 0;
            }
            if (s.at(0) == STL_USER_HL)
            {
                item[curitem].type = Highlight;
                item[curitem].start = p;
                item[curitem].minwid = (9 < minwid) ? 1 : minwid;
                s = s.plus(1);
                curitem++;
                continue;
            }
            if (s.at(0) == STL_TABPAGENR || s.at(0) == STL_TABCLOSENR)
            {
                if (s.at(0) == STL_TABCLOSENR)
                {
                    if (minwid == 0)
                    {
                        /* %X ends the close label, go back to the previously define tab label nr. */
                        for (int n = curitem - 1; 0 <= n; --n)
                            if (item[n].type == TabPage && 0 <= item[n].minwid)
                            {
                                minwid = item[n].minwid;
                                break;
                            }
                    }
                    else
                        /* close nrs are stored as negative values */
                        minwid = - minwid;
                }
                item[curitem].type = TabPage;
                item[curitem].start = p;
                item[curitem].minwid = minwid;
                s = s.plus(1);
                curitem++;
                continue;
            }
            if (s.at(0) == (byte)'.')
            {
                s = s.plus(1);
                if (asc_isdigit(s.at(0)))
                {
                    { Bytes[] __ = { s }; maxwid = (int)getdigits(__); s = __[0]; }
                    if (maxwid <= 0)        /* overflow */
                        maxwid = 50;
                }
            }
            minwid = (50 < minwid ? 50 : minwid) * l;
            if (s.at(0) == (byte)'(')
            {
                groupitem[groupdepth++] = curitem;
                item[curitem].type = Group;
                item[curitem].start = p;
                item[curitem].minwid = minwid;
                item[curitem].maxwid = maxwid;
                s = s.plus(1);
                curitem++;
                continue;
            }
            if (vim_strchr(STL_ALL, s.at(0)) == null)
            {
                s = s.plus(1);
                continue;
            }
            byte opt = (s = s.plus(1)).at(-1);

            /* OK - now for the real work. */
            byte base = 'D';
            boolean itemisflag = false;
            boolean fillable = true;
            long num = -1;
            Bytes str = null;
            switch (opt)
            {
                case STL_FILEPATH:
                case STL_FULLPATH:
                case STL_FILENAME:
                {
                    fillable = false;   /* don't change ' ' to fillchar */
                    vim_strncpy(nameBuff, buf_spname(wp.w_buffer, opt == STL_FULLPATH), MAXPATHL - 1);
                    trans_characters(nameBuff, MAXPATHL);
                    if (opt != STL_FILENAME)
                        str = nameBuff;
                    else
                        str = gettail(nameBuff);
                    break;
                }

                case STL_VIM_EXPR: /* '{' */
                {
                    itemisflag = true;
                    Bytes[] t = { p };
                    while (s.at(0) != (byte)'}' && s.at(0) != NUL && BLT(p.plus(1), out.plus(outlen)))
                        (p = p.plus(1)).be(-1, (s = s.plus(1)).at(-1));
                    if (s.at(0) != (byte)'}')      /* missing '}' or out of space */
                        break;
                    s = s.plus(1);
                    p.be(0, NUL);
                    p = t[0];

                    vim_snprintf(tmp, tmp.size(), u8("%d"), curbuf.b_fnum);
                    set_internal_string_var(u8("actual_curbuf"), tmp);

                    buffer_C o_curbuf = curbuf;
                    window_C o_curwin = curwin;
                    curwin = wp;
                    curbuf = wp.w_buffer;

                    str = eval_to_string_safe(p, t, use_sandbox);

                    curwin = o_curwin;
                    curbuf = o_curbuf;
                    do_unlet(u8("g:actual_curbuf"), true);

                    if (str != null && str.at(0) != 0)
                    {
                        if (skipdigits(str).at(0) == NUL)
                        {
                            num = libC.atoi(str);
                            str = null;
                            itemisflag = false;
                        }
                    }
                    break;
                }

                case STL_LINE:
                {
                    num = ((wp.w_buffer.b_ml.ml_flags & ML_EMPTY) != 0) ? 0 : wp.w_cursor.lnum;
                    break;
                }

                case STL_NUMLINES:
                {
                    num = wp.w_buffer.b_ml.ml_line_count;
                    break;
                }

                case STL_COLUMN:
                {
                    num = ((State & INSERT) == 0 && empty_line) ? 0 : wp.w_cursor.col + 1;
                    break;
                }

                case STL_VIRTCOL:
                case STL_VIRTCOL_ALT:
                {
                    /* In list mode virtcol needs to be recomputed. */
                    int[] virtcol = { wp.w_virtcol };
                    if (wp.w_onebuf_opt.wo_list[0] && lcs_tab1[0] == NUL)
                    {
                        wp.w_onebuf_opt.wo_list[0] = false;
                        getvcol(wp, wp.w_cursor, null, virtcol, null);
                        wp.w_onebuf_opt.wo_list[0] = true;
                    }
                    virtcol[0]++;
                    /* Don't display %V if it's the same as %c. */
                    if (opt == STL_VIRTCOL_ALT
                            && (virtcol[0] == (((State & INSERT) == 0 && empty_line) ? 0 : wp.w_cursor.col + 1)))
                        break;
                    num = (long)virtcol[0];
                    break;
                }

                case STL_PERCENTAGE:
                {
                    num = (int)(wp.w_cursor.lnum * 100L / wp.w_buffer.b_ml.ml_line_count);
                    break;
                }

                case STL_ALTPERCENT:
                {
                    str = tmp;
                    get_rel_pos(wp, str, TMPLEN);
                    break;
                }

                case STL_ARGLISTSTAT:
                {
                    fillable = false;
                    tmp.be(0, NUL);
                    if (append_arg_number(wp, tmp, tmp.size(), false))
                        str = tmp;
                    break;
                }

                case STL_KEYMAP:
                {
                    fillable = false;
                    if (get_keymap_str(wp, tmp, TMPLEN))
                        str = tmp;
                    break;
                }

                case STL_PAGENUM:
                {
                    num = 0;
                    break;
                }

                case STL_BUFNO:
                {
                    num = wp.w_buffer.b_fnum;
                    break;
                }

                case STL_OFFSET_X:
                    base = 'X';
                case STL_OFFSET:
                {
                    long off = ml_find_line_or_offset(wp.w_buffer, wp.w_cursor.lnum, null);
                    num = ((wp.w_buffer.b_ml.ml_flags & ML_EMPTY) != 0 || off < 0)
                        ? 0
                        : off + 1 + (((State & INSERT) == 0 && empty_line) ? 0 : wp.w_cursor.col);
                    break;
                }

                case STL_BYTEVAL_X:
                    base = 'X';
                case STL_BYTEVAL:
                    num = byteval;
                    if (num == NL)
                        num = 0;
                    else if (num == CAR && get_fileformat(wp.w_buffer) == EOL_MAC)
                        num = NL;
                    break;

                case STL_ROFLAG:
                case STL_ROFLAG_ALT:
                {
                    itemisflag = true;
                    if (wp.w_buffer.b_p_ro[0])
                        str = (opt == STL_ROFLAG_ALT) ? u8(",RO") : u8("[RO]");
                    break;
                }

                case STL_HELPFLAG:
                case STL_HELPFLAG_ALT:
                {
                    itemisflag = true;
                    break;
                }

                case STL_FILETYPE:
                {
                    if (wp.w_buffer.b_p_ft[0].at(0) != NUL && strlen(wp.w_buffer.b_p_ft[0]) < TMPLEN - 3)
                    {
                        vim_snprintf(tmp, tmp.size(), u8("[%s]"), wp.w_buffer.b_p_ft[0]);
                        str = tmp;
                    }
                    break;
                }

                case STL_FILETYPE_ALT:
                {
                    itemisflag = true;
                    if (wp.w_buffer.b_p_ft[0].at(0) != NUL && strlen(wp.w_buffer.b_p_ft[0]) < TMPLEN - 2)
                    {
                        vim_snprintf(tmp, tmp.size(), u8(",%s"), wp.w_buffer.b_p_ft[0]);
                        for (Bytes t = tmp; t.at(0) != NUL; t = t.plus(1))
                            t.be(0, asc_toupper(t.at(0)));
                        str = tmp;
                    }
                    break;
                }

                case STL_MODIFIED:
                case STL_MODIFIED_ALT:
                {
                    itemisflag = true;
                    switch ((opt == STL_MODIFIED_ALT ? 1 : 0) + (bufIsChanged(wp.w_buffer) ? 2 : 0) + (!wp.w_buffer.b_p_ma[0] ? 4 : 0))
                    {
                        case 2: str = u8("[+]"); break;
                        case 3: str = u8(",+"); break;
                        case 4: str = u8("[-]"); break;
                        case 5: str = u8(",-"); break;
                        case 6: str = u8("[+-]"); break;
                        case 7: str = u8(",+-"); break;
                    }
                    break;
                }

                case STL_HIGHLIGHT:
                {
                    Bytes t = s;
                    while (s.at(0) != (byte)'#' && s.at(0) != NUL)
                        s = s.plus(1);
                    if (s.at(0) == (byte)'#')
                    {
                        item[curitem].type = Highlight;
                        item[curitem].start = p;
                        item[curitem].minwid = -syn_namen2id(t, BDIFF(s, t));
                        curitem++;
                    }
                    if (s.at(0) != NUL)
                        s = s.plus(1);
                    continue;
                }
            }

            item[curitem].start = p;
            item[curitem].type = Normal;
            if (str != null && str.at(0) != NUL)
            {
                Bytes t = str;
                if (itemisflag)
                {
                    if ((t.at(0) != NUL && t.at(1) != NUL)
                            && ((!prevchar_isitem && t.at(0) == (byte)',') || (prevchar_isflag && t.at(0) == (byte)' ')))
                        t = t.plus(1);
                    prevchar_isflag = true;
                }
                l = mb_string2cells(t, -1);
                if (0 < l)
                    prevchar_isitem = true;
                if (maxwid < l)
                {
                    while (maxwid <= l)
                    {
                        l -= mb_ptr2cells(t);
                        t = t.plus(us_ptr2len_cc(t));
                    }
                    if (BLE(out.plus(outlen), p.plus(1)))
                        break;
                    (p = p.plus(1)).be(-1, (byte)'<');
                }
                if (0 < minwid)
                {
                    for ( ; l < minwid && BLT(p.plus(1), out.plus(outlen)); l++)
                    {
                        /* Don't put a "-" in front of a digit. */
                        if (l + 1 == minwid && fillchar == '-' && asc_isdigit(t.at(0)))
                            (p = p.plus(1)).be(-1, (byte)' ');
                        else
                            (p = p.plus(1)).be(-1, fillchar);
                    }
                    minwid = 0;
                }
                else
                    minwid *= -1;
                while (t.at(0) != NUL && BLT(p.plus(1), out.plus(outlen)))
                {
                    (p = p.plus(1)).be(-1, (t = t.plus(1)).at(-1));
                    /* Change a space by fillchar, unless fillchar is '-' and a digit follows. */
                    if (fillable && p.at(-1) == (byte)' ' && (!asc_isdigit(t.at(0)) || fillchar != '-'))
                        p.be(-1, fillchar);
                }
                for ( ; l < minwid && BLT(p.plus(1), out.plus(outlen)); l++)
                    (p = p.plus(1)).be(-1, fillchar);
            }
            else if (0 <= num)
            {
                int nbase = (base == 'D' ? 10 : (base == 'O' ? 8 : 16));
                Bytes nstr = new Bytes(20);

                if (BLE(out.plus(outlen), p.plus(20)))
                    break;          /* not sufficient space */
                prevchar_isitem = true;
                Bytes t = nstr;
                if (opt == STL_VIRTCOL_ALT)
                {
                    (t = t.plus(1)).be(-1, (byte)'-');
                    minwid--;
                }
                (t = t.plus(1)).be(-1, (byte)'%');
                if (zeropad)
                    (t = t.plus(1)).be(-1, (byte)'0');
                (t = t.plus(1)).be(-1, (byte)'*');
                (t = t.plus(1)).be(-1, (nbase == 16) ? base : (nbase == 8) ? (byte)'o' : (byte)'d');
                t.be(0, NUL);

                l = 1;
                for (long n = num; nbase <= n; n /= nbase)
                    l++;
                if (opt == STL_VIRTCOL_ALT)
                    l++;
                if (maxwid < l)
                {
                    l += 2;
                    long n = l - maxwid;
                    while (maxwid < l--)
                        num /= nbase;
                    (t = t.plus(1)).be(-1, (byte)'>');
                    (t = t.plus(1)).be(-1, (byte)'%');
                    t.be(0, t.at(-3));
                    (t = t.plus(1)).be(0, NUL);
                    vim_snprintf(p, outlen - BDIFF(p, out), nstr, 0, num, n);
                }
                else
                    vim_snprintf(p, outlen - BDIFF(p, out), nstr, minwid, num);
                p = p.plus(strlen(p));
            }
            else
                item[curitem].type = Empty;

         /* if (opt == STL_VIM_EXPR)
                ; */ /* str FREAK'd anno */

            if (0 <= num || (!itemisflag && str != null && str.at(0) != NUL))
                prevchar_isflag = false;        /* Item not null, but not a flag. */
            curitem++;
        }
        p.be(0, NUL);
        int itemcnt = curitem;

        int width = mb_string2cells(out, -1);
        if (0 < maxwidth && maxwidth < width)
        {
            /* Result is too long, must truncate somewhere. */
            int l = 0;
            if (itemcnt == 0)
                s = out;
            else
            {
                for ( ; l < itemcnt; l++)
                    if (item[l].type == Trunc)
                    {
                        /* Truncate at %< item. */
                        s = item[l].start;
                        break;
                    }
                if (l == itemcnt)
                {
                    /* No %< item, truncate first item. */
                    l = 0;
                    s = item[l].start;
                }
            }

            if (maxwidth <= width - mb_string2cells(s, -1))
            {
                /* Truncation mark is beyond max length. */
                s = out;
                width = 0;
                for ( ; ; )
                {
                    width += mb_ptr2cells(s);
                    if (maxwidth <= width)
                        break;
                    s = s.plus(us_ptr2len_cc(s));
                }
                /* Fill up for half a double-wide character. */
                while (++width < maxwidth)
                    (s = s.plus(1)).be(-1, fillchar);
                for (l = 0; l < itemcnt; l++)
                    if (BLT(s, item[l].start))
                        break;
                itemcnt = l;
                (s = s.plus(1)).be(-1, (byte)'>');
                s.be(0, NUL);
            }
            else
            {
                int n = 0;
                while (maxwidth <= width)
                {
                    width -= mb_ptr2cells(s.plus(n));
                    n += us_ptr2len_cc(s.plus(n));
                }

                p = s.plus(n);
                BCOPY(s, 1, p, 0, strlen(p) + 1);
                s.be(0, (byte)'<');

                /* Fill up for half a double-wide character. */
                while (++width < maxwidth)
                {
                    s = s.plus(strlen(s));
                    (s = s.plus(1)).be(-1, fillchar);
                    s.be(0, NUL);
                }

                --n;        /* count the '<' */
                for ( ; l < itemcnt; l++)
                {
                    if (BLE(s, item[l].start.minus(n)))
                        item[l].start = item[l].start.minus(n);
                    else
                        item[l].start = s;
                }
            }
            width = maxwidth;
        }
        else if (width < maxwidth && strlen(out) + maxwidth - width + 1 < outlen)
        {
            /* Apply STL_MIDDLE if any. */
            int l;
            for (l = 0; l < itemcnt; l++)
                if (item[l].type == Middle)
                    break;
            if (l < itemcnt)
            {
                p = item[l].start.plus(maxwidth - width);
                BCOPY(p, item[l].start, strlen(item[l].start) + 1);
                for (s = item[l].start; BLT(s, p); s = s.plus(1))
                    s.be(0, fillchar);
                for (l++; l < itemcnt; l++)
                    item[l].start = item[l].start.plus(maxwidth - width);
                width = maxwidth;
            }
        }

        /* Store the info about highlighting. */
        if (hltab != null)
        {
            int i = 0;
            for (int j = 0; j < itemcnt; j++)
            {
                if (item[j].type == Highlight)
                {
                    hltab[i].start = item[j].start;
                    hltab[i].userhl = item[j].minwid;
                    i++;
                }
            }
            hltab[i].start = null;
            hltab[i].userhl = 0;
        }

        /* Store the info about tab pages labels. */
        if (tabtab != null)
        {
            int i = 0;
            for (int j = 0; j < itemcnt; j++)
            {
                if (item[j].type == TabPage)
                {
                    tabtab[i].start = item[j].start;
                    tabtab[i].userhl = item[j].minwid;
                    i++;
                }
            }
            tabtab[i].start = null;
            tabtab[i].userhl = 0;
        }

        return width;
    }

    /*
     * Get relative cursor position in window into "buf[buflen]", in the form 99%,
     * using "Top", "Bot" or "All" when appropriate.
     */
    /*private*/ static void get_rel_pos(window_C wp, Bytes buf, int buflen)
    {
        if (buflen < 3) /* need at least 3 chars for writing */
            return;

        /* number of lines above/below window */
        long above = wp.w_topline - 1;
        long below = wp.w_buffer.b_ml.ml_line_count - wp.w_botline + 1;

        if (below <= 0)
            vim_strncpy(buf, (above == 0) ? u8("All") : u8("Bot"), buflen - 1);
        else if (above <= 0)
            vim_strncpy(buf, u8("Top"), buflen - 1);
        else
        {
            int cent = (1000000L < above)
                ? (int)(above / ((above + below) / 100L))
                : (int)(above * 100L / (above + below));
            vim_snprintf(buf, buflen, u8("%2d%%"), cent);
        }
    }

    /*
     * Append (file 2 of 8) to "buf[buflen]", if editing more than one file.
     * Return true if it was appended.
     */
    /*private*/ static boolean append_arg_number(window_C wp, Bytes buf, int buflen, boolean add_file)
        /* add_file: Add "file" before the arg number */
    {
        if (curwin.w_alist.al_ga.ga_len <= 1) /* nothing to do */
            return false;

        Bytes p = buf.plus(strlen(buf));           /* go to the end of the buffer */
        if (buflen <= BDIFF(p, buf) + 35)             /* getting too long */
            return false;

        (p = p.plus(1)).be(-1, (byte)' ');
        (p = p.plus(1)).be(-1, (byte)'(');
        if (add_file)
        {
            STRCPY(p, u8("file "));
            p = p.plus(5);
        }
        vim_snprintf(p, buflen - BDIFF(p, buf),
                    wp.w_arg_idx_invalid ? u8("(%d) of %d)") : u8("%d of %d)"),
                    wp.w_arg_idx + 1, curwin.w_alist.al_ga.ga_len);

        return true;
    }

    /*
     * Make "ffname" a full file name, set "sfname" to "ffname" if not null.
     * "ffname" becomes a pointer to allocated memory (or null).
     */
    /*private*/ static void fname_expand(Bytes[] ffname, Bytes[] sfname)
    {
        if (ffname[0] == null)        /* if no file name given, nothing to do */
            return;
        if (sfname[0] == null)        /* if no short file name given, use "ffname" */
            sfname[0] = ffname[0];
        ffname[0] = fullName_save(ffname[0], true); /* expand to full path */
    }

    /*
     * Get the file name for an argument list entry.
     */
    /*private*/ static Bytes alist_name(aentry_C aep)
    {
        /* Use the name from the associated buffer if it exists. */
        buffer_C bp = buflist_findnr(aep.ae_fnum);
        if (bp == null || bp.b_fname == null)
            return aep.ae_fname;

        return bp.b_fname;
    }

    /*
     * do_arg_all(): Open up to 'count' windows, one for each argument.
     */
    /*private*/ static void do_arg_all(int count, boolean forceit, boolean keep_tabs)
        /* forceit: hide buffers in current windows */
        /* keep_tabs: keep current tabs, for ":tab drop file" */
    {
        boolean use_firstwin = false;   /* use first window for arglist */
        boolean split_ret = true;
        int had_tab = cmdmod.tab;
        window_C new_curwin = null;
        tabpage_C new_curtab = null;

        if (curwin.w_alist.al_ga.ga_len <= 0)
        {
            /* Don't give an error message.
             * We don't want it when the ":all" command is in the .vimrc. */
            return;
        }
        setpcmark();

        /* Array of weight for which args are open:
         *  0: not opened
         *  1: opened in other tab
         *  2: opened in curtab
         *  3: opened in curtab and curwin
         */
        int opened_len = curwin.w_alist.al_ga.ga_len;
        byte[] opened = new byte[opened_len];

        /* Autocommands may do anything to the argument list.
         * Make sure it's not freed while we are working here by "locking" it.
         * We still have to watch out for its size to be changed. */
        alist_C alist = curwin.w_alist;
        alist.al_refcount++;

        window_C old_curwin = curwin;
        tabpage_C old_curtab = curtab;

        /*
         * Try closing all windows that are not in the argument list.
         * Also close windows that are not full width;
         * When 'hidden' or "forceit" set the buffer becomes hidden.
         * Windows that have a changed buffer and can't be hidden won't be closed.
         * When the ":tab" modifier was used do this for all tab pages.
         */
        if (0 < had_tab)
            goto_tabpage_tp(first_tabpage, true, true);
        for ( ; ; )
        {
            tabpage_C tpnext = curtab.tp_next;

            for (window_C wp = firstwin, wpnext; wp != null; wp = wpnext)
            {
                wpnext = wp.w_next;
                buffer_C buf = wp.w_buffer;
                int i;
                if (buf.b_ffname == null || (!keep_tabs && 1 < buf.b_nwindows) || wp.w_width != (int)Columns[0])
                    i = opened_len;
                else
                {
                    /* check if the buffer in this window is in the arglist */
                    for (i = 0; i < opened_len; i++)
                    {
                        aentry_C[] aep = alist.al_ga.ga_data;
                        if (i < alist.al_ga.ga_len
                                && (aep[i].ae_fnum == buf.b_fnum
                                        || (fullpathcmp(alist_name(aep[i]), buf.b_ffname, true) & FPC_SAME) != 0))
                        {
                            byte weight = 1;

                            if (old_curtab == curtab)
                            {
                                weight++;
                                if (old_curwin == wp)
                                    weight++;
                            }

                            if (opened[i] < weight)
                            {
                                opened[i] = weight;
                                if (i == 0)
                                {
                                    if (new_curwin != null)
                                        new_curwin.w_arg_idx = opened_len;
                                    new_curwin = wp;
                                    new_curtab = curtab;
                                }
                            }
                            else if (keep_tabs)
                                i = opened_len;

                            if (wp.w_alist != alist)
                            {
                                /* Use the current argument list for all windows
                                 * containing a file from it. */
                                alist_unlink(wp.w_alist);
                                wp.w_alist = alist;
                                wp.w_alist.al_refcount++;
                            }
                            break;
                        }
                    }
                }
                wp.w_arg_idx = i;

                if (i == opened_len && !keep_tabs)  /* close this window */
                {
                    if (P_HID(buf) || forceit || 1 < buf.b_nwindows || !bufIsChanged(buf))
                    {
                        /* If the buffer was changed, and we would like to hide it,
                         * try autowriting. */
                        if (!P_HID(buf) && buf.b_nwindows <= 1 && bufIsChanged(buf))
                        {
                            autowrite(buf, false);
                            /* check if autocommands removed the window */
                            if (!win_valid(wp) || !buf_valid(buf))
                            {
                                wpnext = firstwin;  /* start all over... */
                                continue;
                            }
                        }
                        /* don't close last window */
                        if (firstwin == lastwin && (first_tabpage.tp_next == null || had_tab == 0))
                            use_firstwin = true;
                        else
                        {
                            win_close(wp, !P_HID(buf) && !bufIsChanged(buf));
                            /* check if autocommands removed the next window */
                            if (!win_valid(wpnext))
                                wpnext = firstwin;  /* start all over... */
                        }
                    }
                }
            }

            /* Without the ":tab" modifier only do the current tab page. */
            if (had_tab == 0 || tpnext == null)
                break;

            /* check if autocommands removed the next tab page */
            if (!valid_tabpage(tpnext))
                tpnext = first_tabpage;     /* start all over... */
            goto_tabpage_tp(tpnext, true, true);
        }

        /*
         * Open a window for files in the argument list that don't have one.
         * ARGCOUNT may change while doing this, because of autocommands.
         */
        if (count > opened_len || count <= 0)
            count = opened_len;

        /* Don't execute Win/Buf Enter/Leave autocommands here. */
        autocmd_no_enter++;
        autocmd_no_leave++;
        window_C last_curwin = curwin;
        tabpage_C last_curtab = curtab;
        win_enter(lastwin, false);
        /* ":drop all" should re-use an empty window to avoid "--remote-tab"
         * leaving an empty tab page when executed locally. */
        if (keep_tabs && bufempty() && curbuf.b_nwindows == 1
                                && curbuf.b_ffname == null && !curbuf.b_changed[0])
            use_firstwin = true;

        for (int i = 0; i < count && i < opened_len && !got_int; i++)
        {
            if (alist == global_alist && i == global_alist.al_ga.ga_len - 1)
                arg_had_last = true;
            if (0 < opened[i])
            {
                /* Move the already present window to below the current window. */
                if (curwin.w_arg_idx != i)
                    for (window_C wpnext = firstwin; wpnext != null; wpnext = wpnext.w_next)
                    {
                        if (wpnext.w_arg_idx == i)
                        {
                            if (keep_tabs)
                            {
                                new_curwin = wpnext;
                                new_curtab = curtab;
                            }
                            else
                                win_move_after(wpnext, curwin);
                            break;
                        }
                    }
            }
            else if (split_ret == true)
            {
                if (!use_firstwin)          /* split current window */
                {
                    boolean p_ea_save = p_ea[0];
                    p_ea[0] = true;            /* use space from all windows */
                    split_ret = win_split(0, WSP_ROOM | WSP_BELOW);
                    p_ea[0] = p_ea_save;
                    if (split_ret == false)
                        continue;
                }
                else    /* first window: do autocmd for leaving this buffer */
                    --autocmd_no_leave;

                /*
                 * edit file "i"
                 */
                curwin.w_arg_idx = i;
                if (i == 0)
                {
                    new_curwin = curwin;
                    new_curtab = curtab;
                }
                do_ecmd(0, alist_name(alist.al_ga.ga_data[i]), null, null,
                          ECMD_ONE,
                          ((P_HID(curwin.w_buffer)
                               || bufIsChanged(curwin.w_buffer)) ? ECMD_HIDE : 0) + ECMD_OLDBUF, curwin);
                if (use_firstwin)
                    autocmd_no_leave++;
                use_firstwin = false;
            }
            ui_breakcheck();

            /* When ":tab" was used open a new tab for a new window repeatedly. */
            if (0 < had_tab && tabpage_index(null) <= p_tpm[0])
                cmdmod.tab = 9999;
        }

        /* Remove the "lock" on the argument list. */
        alist_unlink(alist);

        --autocmd_no_enter;
        /* restore last referenced tabpage's curwin */
        if (last_curtab != new_curtab)
        {
            if (valid_tabpage(last_curtab))
                goto_tabpage_tp(last_curtab, true, true);
            if (win_valid(last_curwin))
                win_enter(last_curwin, false);
        }
        /* to window with first arg */
        if (valid_tabpage(new_curtab))
            goto_tabpage_tp(new_curtab, true, true);
        if (win_valid(new_curwin))
            win_enter(new_curwin, false);

        --autocmd_no_leave;
    }

    /*
     * Open a window for a number of buffers.
     */
    /*private*/ static final ex_func_C ex_buffer_all = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            boolean split_ret = true;
            int open_wins = 0;
            int had_tab = cmdmod.tab;

            /* Maximum number of windows to open. */
            int count;
            if (eap.addr_count == 0)    /* make as many windows as possible */
                count = 9999;
            else
                count = (int)eap.line2;      /* make as many windows as specified */

            /* When true also load inactive buffers. */
            boolean all;
            if (eap.cmdidx == CMD_unhide || eap.cmdidx == CMD_sunhide)
                all = false;
            else
                all = true;

            setpcmark();

            /*
             * Close superfluous windows (two windows for the same buffer).
             * Also close windows that are not full-width.
             */
            if (0 < had_tab)
                goto_tabpage_tp(first_tabpage, true, true);
            for ( ; ; )
            {
                tabpage_C tpnext = curtab.tp_next;

                for (window_C wp = firstwin, wpnext; wp != null; wp = wpnext)
                {
                    wpnext = wp.w_next;
                    if ((1 < wp.w_buffer.b_nwindows
                            || ((cmdmod.split & WSP_VERT) != 0
                                ? wp.w_height + wp.w_status_height < Rows[0] - p_ch[0] - tabline_height()
                                : wp.w_width != (int)Columns[0])
                            || (0 < had_tab && wp != firstwin)
                            ) && firstwin != lastwin
                            && !(wp.w_closing || wp.w_buffer.b_closing))
                    {
                        win_close(wp, false);
                        wpnext = firstwin;          /* just in case an autocommand does
                                                    * something strange with windows */
                        tpnext = first_tabpage;     /* start all over... */
                        open_wins = 0;
                    }
                    else
                        open_wins++;
                }

                /* Without the ":tab" modifier only do the current tab page. */
                if (had_tab == 0 || tpnext == null)
                    break;
                goto_tabpage_tp(tpnext, true, true);
            }

            /*
             * Go through the buffer list.  When a buffer doesn't have a window yet,
             * open one.  Otherwise move the window to the right position.
             * Watch out for autocommands that delete buffers or windows!
             */
            /* Don't execute Win/Buf Enter/Leave autocommands here. */
            autocmd_no_enter++;
            win_enter(lastwin, false);
            autocmd_no_leave++;
            for (buffer_C buf = firstbuf; buf != null && open_wins < count; buf = buf.b_next)
            {
                /* Check if this buffer needs a window. */
                if ((!all && buf.b_ml.ml_mfp == null) || !buf.b_p_bl[0])
                    continue;

                window_C wp;
                if (had_tab != 0)
                {
                    /* With the ":tab" modifier don't move the window. */
                    if (0 < buf.b_nwindows)
                        wp = lastwin;       /* buffer has a window, skip it */
                    else
                        wp = null;
                }
                else
                {
                    /* Check if this buffer already has a window. */
                    for (wp = firstwin; wp != null; wp = wp.w_next)
                        if (wp.w_buffer == buf)
                            break;
                    /* If the buffer already has a window, move it. */
                    if (wp != null)
                        win_move_after(wp, curwin);
                }

                if (wp == null && split_ret == true)
                {
                    /* Split the window and put the buffer in it. */
                    boolean p_ea_save = p_ea[0];
                    p_ea[0] = true;                        /* use space from all windows */
                    split_ret = win_split(0, WSP_ROOM | WSP_BELOW);
                    open_wins++;
                    p_ea[0] = p_ea_save;
                    if (split_ret == false)
                        continue;

                    /* Open the buffer in this window. */
                    swap_exists_action = SEA_DIALOG;
                    set_curbuf(buf, DOBUF_GOTO);
                    if (!buf_valid(buf))                /* autocommands deleted the buffer!!! */
                    {
                        swap_exists_action = SEA_NONE;
                        break;
                    }
                    if (swap_exists_action == SEA_QUIT)
                    {
                        cleanup_C cs = new cleanup_C();

                        /* Reset the error/interrupt/exception state here so that
                         * aborting() returns false when closing a window. */
                        enter_cleanup(cs);

                        /* User selected Quit at ATTENTION prompt; close this window. */
                        win_close(curwin, true);
                        --open_wins;
                        swap_exists_action = SEA_NONE;
                        swap_exists_did_quit = true;

                        /* Restore the error/interrupt/exception state if not discarded
                         * by a new aborting error, interrupt, or uncaught exception. */
                        leave_cleanup(cs);
                    }
                    else
                        handle_swap_exists(null);
                }

                ui_breakcheck();
                if (got_int)
                {
                    vgetc();        /* only break the file loading, not the rest */
                    break;
                }
                /* Autocommands deleted the buffer or aborted script processing!!! */
                if (aborting())
                    break;
                /* When ":tab" was used open a new tab for a new window repeatedly. */
                if (0 < had_tab && tabpage_index(null) <= p_tpm[0])
                    cmdmod.tab = 9999;
            }
            --autocmd_no_enter;
            win_enter(firstwin, false);         /* back to first window */
            --autocmd_no_leave;

            /*
             * Close superfluous windows.
             */
            for (window_C wp = lastwin; count < open_wins; )
            {
                boolean r = (P_HID(wp.w_buffer) || !bufIsChanged(wp.w_buffer) || autowrite(wp.w_buffer, false) == true);
                if (!win_valid(wp))
                {
                    /* BufWrite Autocommands made the window invalid, start over. */
                    wp = lastwin;
                }
                else if (r)
                {
                    win_close(wp, !P_HID(wp.w_buffer));
                    --open_wins;
                    wp = lastwin;
                }
                else
                {
                    wp = wp.w_prev;
                    if (wp == null)
                        break;
                }
            }
        }
    };

    /*
     * Return special buffer name.
     * Returns null when the buffer has a normal file name.
     */
    /*private*/ static Bytes buf_spname(buffer_C buf, boolean full)
    {
        if (buf.b_fname == null)
            return u8("[No Name]");

        return (full) ? buf.b_ffname : buf.b_fname;
    }

    /*
     * Set 'buflisted' for curbuf to "on" and trigger autocommands if it changed.
     */
    /*private*/ static void set_buflisted(boolean on)
    {
        if (on != curbuf.b_p_bl[0])
        {
            curbuf.b_p_bl[0] = on;
            if (on)
                apply_autocmds(EVENT_BUFADD, null, null, false, curbuf);
            else
                apply_autocmds(EVENT_BUFDELETE, null, null, false, curbuf);
        }
    }

    /*
     * Read the file for "buf" again and check if the contents changed.
     * Return true if it changed or this could not be checked.
     */
    /*private*/ static boolean buf_contents_changed(buffer_C buf)
    {
        boolean differ = true;

        /* Allocate a buffer without putting it in the buffer list. */
        buffer_C newbuf = buflist_new(null, null, 1, BLN_DUMMY);
        if (newbuf == null)
            return true;

        exarg_C ea = new exarg_C();
        prep_exarg(ea, buf);

        /* set curwin/curbuf to buf and save a few things */
        aco_save_C aco = new aco_save_C();
        aucmd_prepbuf(aco, newbuf);

        if (ml_open(curbuf) == true
                && readfile(buf.b_ffname, buf.b_fname,
                        0, 0, MAXLNUM, ea, READ_NEW | READ_DUMMY) == true)
        {
            /* compare the two files line by line */
            if (buf.b_ml.ml_line_count == curbuf.b_ml.ml_line_count)
            {
                differ = false;
                for (long lnum = 1; lnum <= curbuf.b_ml.ml_line_count; lnum++)
                    if (STRCMP(ml_get_buf(buf, lnum, false), ml_get(lnum)) != 0)
                    {
                        differ = true;
                        break;
                    }
            }
        }

        /* restore curwin/curbuf and a few other things */
        aucmd_restbuf(aco);

        if (curbuf != newbuf)       /* safety check */
            wipe_buffer(newbuf, false);

        return differ;
    }

    /*
     * Wipe out a buffer and decrement the last buffer number if it was used for
     * this buffer.  Call this to wipe out a temp buffer that does not contain any marks.
     */
    /*private*/ static void wipe_buffer(buffer_C buf, boolean aucmd)
        /* aucmd: When true trigger autocommands. */
    {
        if (buf.b_fnum == top_file_num - 1)
            --top_file_num;

        if (!aucmd)                 /* Don't trigger BufDelete autocommands here. */
            block_autocmds();
        close_buffer(null, buf, DOBUF_WIPE, false);
        if (!aucmd)
            unblock_autocmds();
    }
}
