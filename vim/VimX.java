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
public class VimX
{
    /*
     * syntax.c: code for syntax highlighting ---------------------------------------------------------
     */

    /*
     * Structure that stores information about a highlight group.
     * The ID of a highlight group is also called group ID.
     * It is the index in the highlight_ga array PLUS ONE.
     */
    /*private*/ static final class hl_group_C
    {
        Bytes       sg_name;            /* highlight group name */
        Bytes       sg_name_u;          /* uppercase of "sg_name" */
    /* for normal terminals */
        int         sg_term;            /* "term=" highlighting attributes */
        Bytes       sg_start;           /* terminal string for start highl */
        Bytes       sg_stop;            /* terminal string for stop highl */
        int         sg_term_attr;       /* Screen attr for term mode */
    /* for color terminals */
        int         sg_cterm;           /* "cterm=" highlighting attr */
        boolean     sg_cterm_bold;      /* bold attr was set for light color */
        int         sg_cterm_fg;        /* terminal fg color number + 1 */
        int         sg_cterm_bg;        /* terminal bg color number + 1 */
        int         sg_cterm_attr;      /* Screen attr for color term mode */
    /* Store the sp color name for the GUI or synIDattr(). */
        int         sg_gui;             /* "gui=" highlighting attributes */
        Bytes       sg_gui_fg_name;     /* GUI foreground color name */
        Bytes       sg_gui_bg_name;     /* GUI background color name */
        Bytes       sg_gui_sp_name;     /* GUI special color name */
        int         sg_link;            /* link to this highlight group ID */
        int         sg_set;             /* combination of SG_* flags */
        int         sg_scriptID;        /* script in which the group was last set */

        /*private*/ hl_group_C()
        {
        }
    }

    /*private*/ static void COPY_hl_group(hl_group_C sg1, hl_group_C sg0)
    {
        sg1.sg_name = sg0.sg_name;
        sg1.sg_name_u = sg0.sg_name_u;
        sg1.sg_term = sg0.sg_term;
        sg1.sg_start = sg0.sg_start;
        sg1.sg_stop = sg0.sg_stop;
        sg1.sg_term_attr = sg0.sg_term_attr;
        sg1.sg_cterm = sg0.sg_cterm;
        sg1.sg_cterm_bold = sg0.sg_cterm_bold;
        sg1.sg_cterm_fg = sg0.sg_cterm_fg;
        sg1.sg_cterm_bg = sg0.sg_cterm_bg;
        sg1.sg_cterm_attr = sg0.sg_cterm_attr;
        sg1.sg_gui = sg0.sg_gui;
        sg1.sg_gui_fg_name = sg0.sg_gui_fg_name;
        sg1.sg_gui_bg_name = sg0.sg_gui_bg_name;
        sg1.sg_gui_sp_name = sg0.sg_gui_sp_name;
        sg1.sg_link = sg0.sg_link;
        sg1.sg_set = sg0.sg_set;
        sg1.sg_scriptID = sg0.sg_scriptID;
    }

    /*private*/ static final int SG_TERM         = 1;           /* term has been set */
    /*private*/ static final int SG_CTERM        = 2;           /* cterm has been set */
    /*private*/ static final int SG_GUI          = 4;           /* gui has been set */
    /*private*/ static final int SG_LINK         = 8;           /* link has been set */

    /* highlight groups for 'highlight' option */
    /*private*/ static Growing<hl_group_C> highlight_ga = new Growing<hl_group_C>(hl_group_C.class, 10);

    /*private*/ static final int MAX_HL_ID       = 20000;           /* maximum value for a highlight ID. */

    /* Flags to indicate an additional string for highlight name completion. */
    /*private*/ static int include_none;        /* when 1 include "None" */
    /*private*/ static int include_default;     /* when 1 include "default" */
    /*private*/ static int include_link;        /* when 2 include "link" and "clear" */

    /*
     * The "term", "cterm" and "gui" arguments can be any combination of
     * the following names, separated by commas (but no spaces!).
     */
    /*private*/ static Bytes[] hl_name_table =
    {
        u8("bold"), u8("standout"), u8("underline"), u8("undercurl"), u8("italic"), u8("reverse"), u8("inverse"), u8("NONE")
    };
    /*private*/ static int[] hl_attr_table =
    {
        HL_BOLD, HL_STANDOUT, HL_UNDERLINE, HL_UNDERCURL, HL_ITALIC, HL_INVERSE, HL_INVERSE, 0
    };

    /*
     * An attribute number is the index in attr_table plus ATTR_OFF.
     */
    /*private*/ static final int ATTR_OFF = HL_ALL + 1;

    /*private*/ static final int SYN_NAMELEN     = 50;              /* maximum length of a syntax name */

    /* different types of offsets that are possible */
    /*private*/ static final int SPO_MS_OFF      = 0;       /* match  start offset */
    /*private*/ static final int SPO_ME_OFF      = 1;       /* match  end   offset */
    /*private*/ static final int SPO_HS_OFF      = 2;       /* highl. start offset */
    /*private*/ static final int SPO_HE_OFF      = 3;       /* highl. end   offset */
    /*private*/ static final int SPO_RS_OFF      = 4;       /* region start offset */
    /*private*/ static final int SPO_RE_OFF      = 5;       /* region end   offset */
    /*private*/ static final int SPO_LC_OFF      = 6;       /* leading context offset */
    /*private*/ static final int SPO_COUNT       = 7;

    /*private*/ static Bytes[/*SPO_COUNT*/] spo_name_tab =
    {
        u8("ms="), u8("me="), u8("hs="), u8("he="), u8("rs="), u8("re="), u8("lc=")
    };

    /*
     * The patterns that are being searched for are stored in a syn_pattern.
     * A match item consists of one pattern.
     * A start/end item consists of n start patterns and m end patterns.
     * A start/skip/end item consists of n start patterns, one skip pattern and m end patterns.
     * For the latter two, the patterns are always consecutive: start-skip-end.
     *
     * A character offset can be given for the matched text (_m_start and _m_end)
     * and for the actually highlighted text (_h_start and _h_end).
     */
    /*private*/ static final class synpat_C
    {
        byte        sp_type;            /* see SPTYPE_ defines below */
        boolean     sp_syncing;         /* this item used for syncing */
        int         sp_flags;           /* see HL_ defines below */
        int         sp_cchar;           /* conceal substitute character */
        sp_syn_C    sp_syn;             /* struct passed to in_id_list() */
        short       sp_syn_match_id;    /* highlight group ID of pattern */
        Bytes       sp_pattern;         /* regexp to match, pattern */
        regprog_C   sp_prog;            /* regexp to match, program */
        boolean     sp_ic;              /* ignore-case flag for sp_prog */
        short       sp_off_flags;       /* see below */
        int[]       sp_offsets;         /* offsets */
        short[]     sp_cont_list;       /* cont. group IDs, if non-zero */
        short[]     sp_next_list;       /* next group IDs, if non-zero */
        int         sp_sync_idx;        /* sync item index (syncing only) */
        int         sp_line_id;         /* ID of last line where tried */
        int         sp_startcol;        /* next match in sp_line_id line */

        /*private*/ synpat_C()
        {
            sp_syn = new sp_syn_C();
            sp_offsets = new int[SPO_COUNT];
        }
    }

    /*private*/ static void COPY_synpat(synpat_C sp1, synpat_C sp0)
    {
        sp1.sp_type = sp0.sp_type;
        sp1.sp_syncing = sp0.sp_syncing;
        sp1.sp_flags = sp0.sp_flags;
        sp1.sp_cchar = sp0.sp_cchar;
        COPY_sp_syn(sp1.sp_syn, sp0.sp_syn);
        sp1.sp_syn_match_id = sp0.sp_syn_match_id;
        sp1.sp_pattern = sp0.sp_pattern;
        sp1.sp_prog = sp0.sp_prog;
        sp1.sp_ic = sp0.sp_ic;
        sp1.sp_off_flags = sp0.sp_off_flags;
        sp1.sp_offsets = sp0.sp_offsets;
        sp1.sp_cont_list = sp0.sp_cont_list;
        sp1.sp_next_list = sp0.sp_next_list;
        sp1.sp_sync_idx = sp0.sp_sync_idx;
        sp1.sp_line_id = sp0.sp_line_id;
        sp1.sp_startcol = sp0.sp_startcol;
    }

    /* The sp_off_flags are computed like this:
     * offset from the start of the matched text: (1 << SPO_XX_OFF)
     * offset from the end   of the matched text: (1 << (SPO_XX_OFF + SPO_COUNT))
     * When both are present, only one is used.
     */

    /*private*/ static final byte SPTYPE_MATCH    = 1;           /* match keyword with this group ID */
    /*private*/ static final byte SPTYPE_START    = 2;           /* match a regexp, start of item */
    /*private*/ static final byte SPTYPE_END      = 3;           /* match a regexp, end of item */
    /*private*/ static final byte SPTYPE_SKIP     = 4;           /* match a regexp, skip within item */

    /*private*/ static final int NONE_IDX        = -2;          /* value of sp_sync_idx for "NONE" */

    /*
     * Flags for b_syn_sync_flags:
     */
    /*private*/ static final int SF_CCOMMENT     = 0x01;        /* sync on a C-style comment */
    /*private*/ static final int SF_MATCH        = 0x02;        /* sync by matching a pattern */

    /*private*/ static final int MAXKEYWLEN      = 80;          /* maximum length of a keyword */

    /*
     * The attributes of the syntax item that has been recognized.
     */
    /*private*/ static int current_attr;        /* attr of current syntax word */
    /*private*/ static int current_id;          /* ID of current char for syn_get_id() */
    /*private*/ static int current_trans_id;    /* idem, transparency removed */
    /*private*/ static int current_flags;
    /*private*/ static int current_seqnr;
    /*private*/ static int current_sub_char;

    /*private*/ static final class syn_cluster_C
    {
        Bytes           scl_name;       /* syntax cluster name */
        Bytes           scl_name_u;     /* uppercase of "scl_name" */
        short[]         scl_list;       /* IDs in this syntax cluster */

        /*private*/ syn_cluster_C()
        {
        }
    }

    /*
     * Methods of combining two clusters
     */
    /*private*/ static final int CLUSTER_REPLACE     = 1;   /* replace first list with second */
    /*private*/ static final int CLUSTER_ADD         = 2;   /* add second list to first */
    /*private*/ static final int CLUSTER_SUBTRACT    = 3;   /* subtract second list from first */

    /*
     * Syntax group IDs have different types:
     *     0 - 19999  normal syntax groups
     * 20000 - 20999  ALLBUT indicator (current_syn_inc_tag added)
     * 21000 - 21999  TOP indicator (current_syn_inc_tag added)
     * 22000 - 22999  CONTAINED indicator (current_syn_inc_tag added)
     * 23000 - 32767  cluster IDs (subtract SYNID_CLUSTER for the cluster ID)
     */
    /*private*/ static final short SYNID_ALLBUT    = MAX_HL_ID;   /* syntax group ID for contains=ALLBUT */
    /*private*/ static final short SYNID_TOP       = 21000;       /* syntax group ID for contains=TOP */
    /*private*/ static final short SYNID_CONTAINED = 22000;       /* syntax group ID for contains=CONTAINED */
    /*private*/ static final short SYNID_CLUSTER   = 23000;       /* first syntax group ID for clusters */

    /*private*/ static final short MAX_SYN_INC_TAG = 999;         /* maximum before the above overflow */
    /*private*/ static final short MAX_CLUSTER_ID  = 32767 - SYNID_CLUSTER;

    /*
     * Another Annoying Hack(TM):  To prevent rules from other ":syn include"'d
     * files from leaking into ALLBUT lists, we assign a unique ID to the
     * rules in each ":syn include"'d file.
     */
    /*private*/ static int current_syn_inc_tag;
    /*private*/ static int running_syn_inc_tag;

    /*
     * To reduce the time spent in keepend(), remember at which level in the state
     * stack the first item with "keepend" is present.  When "-1", there is no
     * "keepend" on the stack.
     */
    /*private*/ static int keepend_level = -1;

    /*
     * For the current state we need to remember more than just the idx.
     * When si_m_endpos.lnum is 0, the items other than si_idx are unknown.
     * (The end positions have the column number of the next char)
     */
    /*private*/ static final class stateitem_C
    {
        int         si_idx;                 /* index of syntax pattern or KEYWORD_IDX */
        int         si_id;                  /* highlight group ID for keywords */
        int         si_trans_id;            /* idem, transparency removed */
        long        si_m_lnum;              /* lnum of the match */
        int         si_m_startcol;          /* starting column of the match */
        lpos_C      si_m_endpos;            /* just after end posn of the match */
        lpos_C      si_h_startpos;          /* start position of the highlighting */
        lpos_C      si_h_endpos;            /* end position of the highlighting */
        lpos_C      si_eoe_pos;             /* end position of end pattern */
        int         si_end_idx;             /* group ID for end pattern or zero */
        boolean     si_ends;                /* if match ends before si_m_endpos */
        int         si_attr;                /* attributes in this state */
        int         si_flags;               /* HL_HAS_EOL flag in this state, and
                                             * HL_SKIP* for si_next_list */
        int         si_seqnr;               /* sequence number */
        int         si_cchar;               /* substitution character for conceal */
        short[]     si_cont_list;           /* list of contained groups */
        short[]     si_next_list;           /* nextgroup IDs after this item ends */
        reg_extmatch_C si_extmatch;         /* \z(...\) matches from start pattern */

        /*private*/ stateitem_C()
        {
            si_m_endpos = new lpos_C();
            si_h_startpos = new lpos_C();
            si_h_endpos = new lpos_C();
            si_eoe_pos = new lpos_C();
        }
    }

    /*private*/ static final int KEYWORD_IDX     = -1;              /* value of si_idx for keywords */
    /*private*/ static final short[] ID_LIST_ALL = new short[0];    /* valid of si_cont_list for containing all
                                                                 * but contained groups */

    /*private*/ static int next_seqnr = 1;      /* value to use for si_seqnr */

    /*
     * Struct to reduce the number of arguments to get_syn_options(), it's used very often.
     */
    /*private*/ static final class syn_opt_arg_C
    {
        int         flags;          /* flags for contained and transparent */
        boolean     keyword;        /* true for ":syn keyword" */
        int[]       sync_idx;       /* syntax item for "grouphere" argument, null if not allowed */
        boolean     has_cont_list;  /* true if "cont_list" can be used */
        short[]     cont_list;      /* group IDs for "contains" argument */
        short[]     cont_in_list;   /* group IDs for "containedin" argument */
        short[]     next_list;      /* group IDs for "nextgroup" argument */

        /*private*/ syn_opt_arg_C()
        {
        }
    }

    /*
     * The next possible match in the current line for any pattern is remembered,
     * to avoid having to try for a match in each column.
     * If next_match_idx == -1, not tried (in this line) yet.
     * If next_match_col == MAXCOL, no match found in this line.
     * (All end positions have the column of the char after the end)
     */
    /*private*/ static int         next_match_col;                      /* column for start of next match */
    /*private*/ static lpos_C      next_match_m_endpos = new lpos_C();  /* position for end of next match */
    /*private*/ static lpos_C      next_match_h_startpos = new lpos_C(); /* pos. for highl. start of next match */
    /*private*/ static lpos_C      next_match_h_endpos = new lpos_C();  /* pos. for highl. end of next match */
    /*private*/ static int         next_match_idx;                      /* index of matched item */
    /*private*/ static int         next_match_flags;                    /* flags for next match */
    /*private*/ static lpos_C      next_match_eos_pos = new lpos_C();   /* end of start pattn (start region) */
    /*private*/ static lpos_C      next_match_eoe_pos = new lpos_C();   /* pos. for end of end pattern */
    /*private*/ static int         next_match_end_idx;                  /* ID of group for end pattn or zero */
    /*private*/ static reg_extmatch_C next_match_extmatch;

    /*
     * The current state (within the line) of the recognition engine.
     */
    /*private*/ static window_C    syn_win;                     /* current window for highlighting */
    /*private*/ static buffer_C    syn_buf;                     /* current buffer for highlighting */
    /*private*/ static synblock_C  syn_block;                   /* current buffer for highlighting */
    /*private*/ static long        current_lnum;                /* lnum of current state */
    /*private*/ static int         current_col;                 /* column of current state */
    /*private*/ static boolean     current_state_stored;        /* true if stored current state
                                                             * after setting current_finished */
    /*private*/ static boolean     current_finished;            /* current line has been finished */
    /* current stack of state_items */
    /*private*/ static Growing<stateitem_C>    current_state = new Growing<stateitem_C>(stateitem_C.class, 3);
    /*private*/ static boolean     current_state_is_valid;
    /*private*/ static short[]     current_next_list;           /* when non-zero, nextgroup list */
    /*private*/ static int         current_next_flags;          /* flags for current_next_list */
    /*private*/ static int         current_line_id;             /* unique number for current line */

    /*private*/ static int __changedtick;       /* remember the last change ID */

    /*
     * Start the syntax recognition for a line.  This function is normally called
     * from the screen updating, once for each displayed line.
     * The buffer is remembered in syn_buf, because get_syntax_attr() doesn't get it.
     * Careful: curbuf and curwin are likely to point to another buffer and window.
     */
    /*private*/ static void syntax_start(window_C wp, long lnum)
    {
        current_sub_char = NUL;

        /*
         * After switching buffers, invalidate current_state.
         * Also do this when a change was made, the current state may be invalid then.
         */
        if (syn_block != wp.w_s || __changedtick != syn_buf.b_changedtick)
        {
            invalidate_current_state();
            syn_buf = wp.w_buffer;
            syn_block = wp.w_s;
        }
        __changedtick = syn_buf.b_changedtick;
        syn_win = wp;

        /*
         * Allocate syntax stack when needed.
         */
        syn_stack_alloc();
        if (syn_block.b_sst_array == null)
            return;         /* out of memory */
        syn_block.b_sst_lasttick = display_tick;

        /*
         * If the state of the end of the previous line is useful, store it.
         */
        if (current_state_is_valid
                && current_lnum < lnum
                && current_lnum < syn_buf.b_ml.ml_line_count)
        {
            syn_finish_line(false);
            if (!current_state_stored)
            {
                current_lnum++;
                store_current_state();
            }

            /*
             * If the current_lnum is now the same as "lnum", keep the current state (this happens very often!).
             * Otherwise invalidate current_state and figure it out below.
             */
            if (current_lnum != lnum)
                invalidate_current_state();
        }
        else
            invalidate_current_state();

        synstate_C last_valid = null;
        synstate_C last_min_valid = null;
        /*
         * Try to synchronize from a saved state in b_sst_array[].
         * Only do this if lnum is not before and not to far beyond a saved state.
         */
        if (!current_state_is_valid && syn_block.b_sst_array != null)
        {
            /* Find last valid saved state before start_lnum. */
            for (synstate_C p = syn_block.b_sst_first; p != null; p = p.sst_next)
            {
                if (lnum < p.sst_lnum)
                    break;
                if (p.sst_lnum <= lnum && p.sst_change_lnum == 0)
                {
                    last_valid = p;
                    if (lnum - syn_block.b_syn_sync_minlines <= p.sst_lnum)
                        last_min_valid = p;
                }
            }
            if (last_min_valid != null)
                load_current_state(last_min_valid);
        }

        long first_stored;
        /*
         * If "lnum" is before or far beyond a line with a saved state, need to re-synchronize.
         */
        if (!current_state_is_valid)
        {
            syn_sync(wp, lnum, last_valid);
            if (current_lnum == 1)
                /* First line is always valid, no matter "minlines". */
                first_stored = 1;
            else
                /* Need to parse "minlines" lines before state can be considered valid to store. */
                first_stored = current_lnum + syn_block.b_syn_sync_minlines;
        }
        else
            first_stored = current_lnum;

        /*
         * Advance from the sync point or saved state until the current line.
         * Save some entries for syncing with later on.
         */
        int dist;
        if (syn_block.b_sst_len <= Rows[0])
            dist = 999999;
        else
            dist = (int)syn_buf.b_ml.ml_line_count / (syn_block.b_sst_len - (int)Rows[0]) + 1;

        synstate_C prev = null;

        while (current_lnum < lnum)
        {
            syn_start_line();
            syn_finish_line(false);
            current_lnum++;

            /* If we parsed at least "minlines" lines or started at a valid state,
             * the current state is considered valid. */
            if (first_stored <= current_lnum)
            {
                /* Check if the saved state entry is for the current line and is
                 * equal to the current state.  If so, then validate all saved
                 * states that depended on a change before the parsed line. */
                if (prev == null)
                    prev = syn_stack_find_entry(current_lnum - 1);
                synstate_C sp = (prev != null) ? prev : syn_block.b_sst_first;
                while (sp != null && sp.sst_lnum < current_lnum)
                    sp = sp.sst_next;
                if (sp != null && sp.sst_lnum == current_lnum && syn_stack_equal(sp))
                {
                    prev = sp;
                    for (long parsed_lnum = current_lnum; sp != null && sp.sst_change_lnum <= parsed_lnum; )
                    {
                        if (sp.sst_lnum <= lnum)
                            prev = sp;              /* valid state before desired line, use this one */
                        else if (sp.sst_change_lnum == 0)
                            break;                  /* past saved states depending on change, break here */
                        sp.sst_change_lnum = 0;
                        sp = sp.sst_next;
                    }
                    load_current_state(prev);
                }
                /* Store the state at this line when it's the first one, the line
                 * where we start parsing, or some distance from the previously
                 * saved state.  But only when parsed at least 'minlines'. */
                else if (prev == null || current_lnum == lnum || prev.sst_lnum + dist <= current_lnum)
                    prev = store_current_state();
            }

            /* This can take a long time: break when CTRL-C pressed.
             * The current state will be wrong then. */
            line_breakcheck();
            if (got_int)
            {
                current_lnum = lnum;
                break;
            }
        }

        syn_start_line();
    }

    /*
     * We cannot simply discard growarrays full of state_items or buf_states;
     * we have to manually release their extmatch pointers first.
     */
    /*private*/ static void clear_syn_state(synstate_C sst)
    {
        Growing<bufstate_C> gap = sst.sst_ga;
        for (int i = 0; i < gap.ga_len; i++)
            gap.ga_data[i].bs_extmatch = null;
        gap.ga_clear();
    }

    /*
     * Cleanup the current_state stack.
     */
    /*private*/ static void clear_current_state()
    {
        stateitem_C[] sip = current_state.ga_data;
        for (int i = 0; i < current_state.ga_len; i++)
            sip[i].si_extmatch = null;
        current_state.ga_clear();
    }

    /*
     * Try to find a synchronisation point for line "lnum".
     *
     * This sets current_lnum and the current state.  One of three methods is used:
     * 1. Search backwards for the end of a C-comment.
     * 2. Search backwards for given sync patterns.
     * 3. Simply start on a given number of lines above "lnum".
     */
    /*private*/ static void syn_sync(window_C wp, long start_lnum, synstate_C last_valid)
    {
        pos_C cursor_save = new pos_C();
        int found_flags = 0;
        int found_match_idx = 0;
        long found_current_lnum = 0;
        int found_current_col= 0;
        lpos_C found_m_endpos = new lpos_C();

        /*
         * Clear any current state that might be hanging around.
         */
        invalidate_current_state();

        /*
         * Start at least "minlines" back.  Default starting point for parsing is there.
         * Start further back, to avoid that scrolling backwards will result in
         * resyncing for every line.  Now it resyncs only one out of N lines,
         * where N is minlines * 1.5, or minlines * 2 if minlines is small.
         * Watch out for overflow when minlines is MAXLNUM.
         */
        if (start_lnum < syn_block.b_syn_sync_minlines)
            start_lnum = 1;
        else
        {
            long lnum;
            if (syn_block.b_syn_sync_minlines == 1)
                lnum = 1;
            else if (syn_block.b_syn_sync_minlines < 10)
                lnum = syn_block.b_syn_sync_minlines * 2;
            else
                lnum = syn_block.b_syn_sync_minlines * 3 / 2;
            if (syn_block.b_syn_sync_maxlines != 0 && syn_block.b_syn_sync_maxlines < lnum)
                lnum = syn_block.b_syn_sync_maxlines;
            if (start_lnum <= lnum)
                start_lnum = 1;
            else
                start_lnum -= lnum;
        }
        current_lnum = start_lnum;

        /*
         * 1. Search backwards for the end of a C-style comment.
         */
        if ((syn_block.b_syn_sync_flags & SF_CCOMMENT) != 0)
        {
            /* Need to make syn_buf the current buffer for a moment, to be able to use find_start_comment(). */
            window_C curwin_save = curwin;
            curwin = wp;
            buffer_C curbuf_save = curbuf;
            curbuf = syn_buf;

            /*
             * Skip lines that end in a backslash.
             */
            for ( ; 1 < start_lnum; --start_lnum)
            {
                Bytes line = ml_get(start_lnum - 1);
                if (line.at(0) == NUL || line.at(strlen(line) - 1) != '\\')
                    break;
            }
            current_lnum = start_lnum;

            /* set cursor to start of search */
            COPY_pos(cursor_save, wp.w_cursor);
            wp.w_cursor.lnum = start_lnum;
            wp.w_cursor.col = 0;

            /*
             * If the line is inside a comment, need to find the syntax item that defines the comment.
             * Restrict the search for the end of a comment to b_syn_sync_maxlines.
             */
            if (find_start_comment((int)syn_block.b_syn_sync_maxlines) != null)
            {
                for (int i = syn_block.b_syn_patterns.ga_len; 0 <= --i; )
                {
                    synpat_C sp = syn_block.b_syn_patterns.ga_data[i];

                    if (sp.sp_syn.id == syn_block.b_syn_sync_id && sp.sp_type == SPTYPE_START)
                    {
                        validate_current_state();
                        push_current_state(i);
                        update_si_attr(current_state.ga_len - 1);
                        break;
                    }
                }
            }

            /* restore cursor and buffer */
            COPY_pos(wp.w_cursor, cursor_save);
            curwin = curwin_save;
            curbuf = curbuf_save;
        }

        /*
         * 2. Search backwards for given sync patterns.
         */
        else if ((syn_block.b_syn_sync_flags & SF_MATCH) != 0)
        {
            long break_lnum;
            if (syn_block.b_syn_sync_maxlines != 0 && syn_block.b_syn_sync_maxlines < start_lnum)
                break_lnum = start_lnum - syn_block.b_syn_sync_maxlines;
            else
                break_lnum = 0;

            found_m_endpos.lnum = 0;
            found_m_endpos.col = 0;
            long end_lnum = start_lnum;
            long lnum = start_lnum;
            while (break_lnum < --lnum)
            {
                /* This can take a long time: break when CTRL-C pressed. */
                line_breakcheck();
                if (got_int)
                {
                    invalidate_current_state();
                    current_lnum = start_lnum;
                    break;
                }

                /* Check if we have run into a valid saved state stack now. */
                if (last_valid != null && lnum == last_valid.sst_lnum)
                {
                    load_current_state(last_valid);
                    break;
                }

                /*
                 * Check if the previous line has the line-continuation pattern.
                 */
                if (1 < lnum && syn_match_linecont(lnum - 1))
                    continue;

                /*
                 * Start with nothing on the state stack
                 */
                validate_current_state();

                for (current_lnum = lnum; current_lnum < end_lnum; current_lnum++)
                {
                    syn_start_line();
                    for ( ; ; )
                    {
                        boolean had_sync_point = syn_finish_line(true);
                        /*
                         * When a sync point has been found, remember where, and
                         * continue to look for another one, further on in the line.
                         */
                        if (had_sync_point && current_state.ga_len != 0)
                        {
                            stateitem_C cur_si = current_state.ga_data[current_state.ga_len - 1];

                            if (start_lnum < cur_si.si_m_endpos.lnum)
                            {
                                /* ignore match that goes to after where started */
                                current_lnum = end_lnum;
                                break;
                            }
                            if (cur_si.si_idx < 0)
                            {
                                /* Cannot happen? */
                                found_flags = 0;
                                found_match_idx = KEYWORD_IDX;
                            }
                            else
                            {
                                synpat_C[] syn_items = syn_block.b_syn_patterns.ga_data;
                                synpat_C spp = syn_items[cur_si.si_idx];

                                found_flags = spp.sp_flags;
                                found_match_idx = spp.sp_sync_idx;
                            }
                            found_current_lnum = current_lnum;
                            found_current_col = current_col;
                            COPY_lpos(found_m_endpos, cur_si.si_m_endpos);
                            /*
                             * Continue after the match (be aware of a zero-length match).
                             */
                            if (current_lnum < found_m_endpos.lnum)
                            {
                                current_lnum = found_m_endpos.lnum;
                                current_col = found_m_endpos.col;
                                if (end_lnum <= current_lnum)
                                    break;
                            }
                            else if (current_col < found_m_endpos.col)
                                current_col = found_m_endpos.col;
                            else
                                current_col++;

                            /* syn_current_attr() will have skipped the check for
                             * an item that ends here, need to do that now.
                             * Be careful not to go past the NUL. */
                            int prev_current_col = current_col;
                            if (syn_getcurline().at(current_col) != NUL)
                                current_col++;
                            check_state_ends();
                            current_col = prev_current_col;
                        }
                        else
                            break;
                    }
                }

                /*
                 * If a sync point was encountered, break here.
                 */
                if (found_flags != 0)
                {
                    /*
                     * Put the item that was specified by the sync point on the state stack.
                     * If there was no item specified, make the state stack empty.
                     */
                    clear_current_state();
                    if (0 <= found_match_idx)
                    {
                        push_current_state(found_match_idx);
                        update_si_attr(current_state.ga_len - 1);
                    }

                    /*
                     * When using "grouphere", continue from the sync point match,
                     * until the end of the line.  Parsing starts at the next line.
                     * For "groupthere" the parsing starts at start_lnum.
                     */
                    if ((found_flags & HL_SYNC_HERE) != 0)
                    {
                        if (current_state.ga_len != 0)
                        {
                            stateitem_C cur_si = current_state.ga_data[current_state.ga_len - 1];
                            cur_si.si_h_startpos.lnum = found_current_lnum;
                            cur_si.si_h_startpos.col = found_current_col;
                            update_si_end(cur_si, current_col, true);
                            check_keepend();
                        }
                        current_col = found_m_endpos.col;
                        current_lnum = found_m_endpos.lnum;
                        syn_finish_line(false);
                        current_lnum++;
                    }
                    else
                        current_lnum = start_lnum;

                    break;
                }

                end_lnum = lnum;
                invalidate_current_state();
            }

            /* Ran into start of the file or exceeded maximum number of lines. */
            if (lnum <= break_lnum)
            {
                invalidate_current_state();
                current_lnum = break_lnum + 1;
            }
        }

        validate_current_state();
    }

    /*
     * Return true if the line-continuation pattern matches in line "lnum".
     */
    /*private*/ static boolean syn_match_linecont(long lnum)
    {
        if (syn_block.b_syn_linecont_prog != null)
        {
            regmmatch_C regmatch = new regmmatch_C();

            regmatch.rmm_ic = syn_block.b_syn_linecont_ic;
            regmatch.regprog = syn_block.b_syn_linecont_prog;
            boolean r = syn_regexec(regmatch, lnum, 0);
            syn_block.b_syn_linecont_prog = regmatch.regprog;

            return r;
        }

        return false;
    }

    /*
     * Prepare the current state for the start of a line.
     */
    /*private*/ static void syn_start_line()
    {
        current_finished = false;
        current_col = 0;

        /*
         * Need to update the end of a start/skip/end that continues from the
         * previous line and regions that have "keepend".
         */
        if (0 < current_state.ga_len)
        {
            syn_update_ends(true);
            check_state_ends();
        }

        next_match_idx = -1;
        current_line_id++;
    }

    /*
     * Check for items in the stack that need their end updated.
     * When "startofline" is true the last item is always updated.
     * When "startofline" is false the item with "keepend" is forcefully updated.
     */
    /*private*/ static void syn_update_ends(boolean startofline)
    {
        if (startofline)
        {
            /* Check for a match carried over from a previous line with a
             * contained region.  The match ends as soon as the region ends. */
            for (int i = 0; i < current_state.ga_len; i++)
            {
                stateitem_C cur_si = current_state.ga_data[i];

                synpat_C[] syn_items = syn_block.b_syn_patterns.ga_data;

                if (0 <= cur_si.si_idx
                        && syn_items[cur_si.si_idx].sp_type == SPTYPE_MATCH
                        && cur_si.si_m_endpos.lnum < current_lnum)
                {
                    cur_si.si_flags |= HL_MATCHCONT;
                    cur_si.si_m_endpos.lnum = 0;
                    cur_si.si_m_endpos.col = 0;
                    COPY_lpos(cur_si.si_h_endpos, cur_si.si_m_endpos);
                    cur_si.si_ends = true;
                }
            }
        }

        /*
         * Need to update the end of a start/skip/end that continues from the previous line.
         * And regions that have "keepend", because they may influence contained items.
         * If we've just removed "extend" (startofline == 0) then we should update ends of
         * normal regions contained inside "keepend" because "extend" could have extended
         * these "keepend" regions as well as contained normal regions.
         * Then check for items ending in column 0.
         */
        int i = current_state.ga_len - 1;
        if (0 <= keepend_level)
            for ( ; keepend_level < i; --i)
                if ((current_state.ga_data[i].si_flags & HL_EXTEND) != 0)
                    break;

        boolean seen_keepend = false;
        for ( ; i < current_state.ga_len; i++)
        {
            stateitem_C cur_si = current_state.ga_data[i];

            if ((cur_si.si_flags & HL_KEEPEND) != 0
                                || (seen_keepend && !startofline)
                                || (i == current_state.ga_len - 1 && startofline))
            {
                cur_si.si_h_startpos.col = 0;   /* start highl. in col 0 */
                cur_si.si_h_startpos.lnum = current_lnum;

                if ((cur_si.si_flags & HL_MATCHCONT) == 0)
                    update_si_end(cur_si, current_col, !startofline);

                if (!startofline && (cur_si.si_flags & HL_KEEPEND) != 0)
                    seen_keepend = true;
            }
        }
        check_keepend();
    }

    /****************************************
     * Handling of the state stack cache.
     */

    /*
     * EXPLANATION OF THE SYNTAX STATE STACK CACHE
     *
     * To speed up syntax highlighting, the state stack for the start of some
     * lines is cached.  These entries can be used to start parsing at that point.
     *
     * The stack is kept in b_sst_array[] for each buffer.  There is a list of
     * valid entries.  b_sst_first points to the first one, then follow sst_next.
     * The entries are sorted on line number.  The first entry is often for line 2
     * (line 1 always starts with an empty stack).
     * There is also a list for free entries.  This construction is used to avoid
     * having to allocate and free memory blocks too often.
     *
     * When making changes to the buffer, this is logged in b_mod_*.  When calling
     * update_screen() to update the display, it will call
     * syn_stack_apply_changes() for each displayed buffer to adjust the cached
     * entries.  The entries which are inside the changed area are removed,
     * because they must be recomputed.  Entries below the changed have their line
     * number adjusted for deleted/inserted lines, and have their sst_change_lnum
     * set to indicate that a check must be made if the changed lines would change
     * the cached entry.
     *
     * When later displaying lines, an entry is stored for each line.  Displayed
     * lines are likely to be displayed again, in which case the state at the
     * start of the line is needed.
     * For not displayed lines, an entry is stored for every so many lines.  These
     * entries will be used e.g., when scrolling backwards.  The distance between
     * entries depends on the number of lines in the buffer.  For small buffers
     * the distance is fixed at SST_DIST, for large buffers there is a fixed
     * number of entries SST_MAX_ENTRIES, and the distance is computed.
     */

    /*private*/ static void syn_stack_free_block(synblock_C block)
    {
        if (block.b_sst_array != null)
        {
            for (synstate_C p = block.b_sst_first; p != null; p = p.sst_next)
                clear_syn_state(p);
            block.b_sst_array = null;
            block.b_sst_len = 0;
        }
    }
    /*
     * Free b_sst_array[] for buffer "buf".
     * Used when syntax items changed to force resyncing everywhere.
     */
    /*private*/ static void syn_stack_free_all(synblock_C block)
    {
        syn_stack_free_block(block);
    }

    /*
     * Allocate the syntax state stack for syn_buf when needed.
     * If the number of entries in b_sst_array[] is much too big or a bit too small, reallocate it.
     * Also used to allocate b_sst_array[] for the first time.
     */
    /*private*/ static void syn_stack_alloc()
    {
        int len = (int)syn_buf.b_ml.ml_line_count / SST_DIST + (int)Rows[0] * 2;
        if (len < SST_MIN_ENTRIES)
            len = SST_MIN_ENTRIES;
        else if (SST_MAX_ENTRIES < len)
            len = SST_MAX_ENTRIES;

        if (len * 2 < syn_block.b_sst_len || syn_block.b_sst_len < len)
        {
            /* Allocate 50% too much, to avoid reallocating too often. */
            len = (int)syn_buf.b_ml.ml_line_count;
            len = (len + len / 2) / SST_DIST + (int)Rows[0] * 2;
            if (len < SST_MIN_ENTRIES)
                len = SST_MIN_ENTRIES;
            else if (SST_MAX_ENTRIES < len)
                len = SST_MAX_ENTRIES;

            if (syn_block.b_sst_array != null)
            {
                /* When shrinking the array, cleanup the existing stack.
                 * Make sure that all valid entries fit in the new array. */
                while (len < syn_block.b_sst_len - syn_block.b_sst_freecount + 2 && syn_stack_cleanup())
                    ;
                if (len < syn_block.b_sst_len - syn_block.b_sst_freecount + 2)
                    len = syn_block.b_sst_len - syn_block.b_sst_freecount + 2;
            }

            synstate_C[] sstp = ARRAY_synstate(len);

            int to = -1;
            if (syn_block.b_sst_array != null)
            {
                /* Move the states from the old array to the new one. */
                for (synstate_C from = syn_block.b_sst_first; from != null; from = from.sst_next)
                {
                    COPY_synstate(sstp[++to], from);
                    sstp[to].sst_next = sstp[to + 1];
                }
            }
            if (to != -1)
            {
                sstp[to].sst_next = null;
                syn_block.b_sst_first = sstp[0];
                syn_block.b_sst_freecount = len - to - 1;
            }
            else
            {
                syn_block.b_sst_first = null;
                syn_block.b_sst_freecount = len;
            }

            /* Create the list of free entries. */
            syn_block.b_sst_firstfree = sstp[to + 1];
            while (++to < len)
                sstp[to].sst_next = sstp[to + 1];
            sstp[len - 1].sst_next = null;

            syn_block.b_sst_array = sstp;
            syn_block.b_sst_len = len;
        }
    }

    /*
     * Check for changes in a buffer to affect stored syntax states.  Uses the b_mod_* fields.
     * Called from update_screen(), before screen is being updated, once for each displayed buffer.
     */
    /*private*/ static void syn_stack_apply_changes(buffer_C buf)
    {
        syn_stack_apply_changes_block(buf.b_s, buf);

        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            if ((wp.w_buffer == buf) && (wp.w_s != buf.b_s))
                syn_stack_apply_changes_block(wp.w_s, buf);
    }

    /*private*/ static void syn_stack_apply_changes_block(synblock_C block, buffer_C buf)
    {
        if (block.b_sst_array == null)      /* nothing to do */
            return;

        synstate_C prev = null;
        for (synstate_C p = block.b_sst_first; p != null; )
        {
            if (buf.b_mod_top < p.sst_lnum + block.b_syn_sync_linebreaks)
            {
                long n = p.sst_lnum + buf.b_mod_xlines;
                if (n <= buf.b_mod_bot)
                {
                    /* this state is inside the changed area, remove it */
                    synstate_C np = p.sst_next;
                    if (prev == null)
                        block.b_sst_first = np;
                    else
                        prev.sst_next = np;
                    syn_stack_free_entry(block, p);
                    p = np;
                    continue;
                }
                /* This state is below the changed area.  Remember the line
                 * that needs to be parsed before this entry can be made valid again. */
                if (p.sst_change_lnum != 0 && buf.b_mod_top < p.sst_change_lnum)
                {
                    if (buf.b_mod_top < p.sst_change_lnum + buf.b_mod_xlines)
                        p.sst_change_lnum += buf.b_mod_xlines;
                    else
                        p.sst_change_lnum = buf.b_mod_top;
                }
                if (p.sst_change_lnum == 0 || p.sst_change_lnum < buf.b_mod_bot)
                    p.sst_change_lnum = buf.b_mod_bot;

                p.sst_lnum = n;
            }
            prev = p;
            p = p.sst_next;
        }
    }

    /*
     * Reduce the number of entries in the state stack for syn_buf.
     * Returns true if at least one entry was freed.
     */
    /*private*/ static boolean syn_stack_cleanup()
    {
        boolean retval = false;

        if (syn_block.b_sst_array == null || syn_block.b_sst_first == null)
            return retval;

        /* Compute normal distance between non-displayed entries. */
        int dist;
        if (syn_block.b_sst_len <= Rows[0])
            dist = 999999;
        else
            dist = (int)syn_buf.b_ml.ml_line_count / (syn_block.b_sst_len - (int)Rows[0]) + 1;

        /*
         * Go through the list to find the "tick" for the oldest entry that can
         * be removed.  Set "above" when the "tick" for the oldest entry is above
         * "b_sst_lasttick" (the display tick wraps around).
         */
        short tick = syn_block.b_sst_lasttick;
        boolean above = false;
        synstate_C prev = syn_block.b_sst_first;
        for (synstate_C p = prev.sst_next; p != null; prev = p, p = p.sst_next)
        {
            if (p.sst_lnum < prev.sst_lnum + dist)
            {
                if (syn_block.b_sst_lasttick < p.sst_tick)
                {
                    if (!above || p.sst_tick < tick)
                        tick = p.sst_tick;
                    above = true;
                }
                else if (!above && p.sst_tick < tick)
                    tick = p.sst_tick;
            }
        }

        /*
         * Go through the list to make the entries for the oldest tick at an
         * interval of several lines.
         */
        prev = syn_block.b_sst_first;
        for (synstate_C p = prev.sst_next; p != null; prev = p, p = p.sst_next)
        {
            if (p.sst_tick == tick && p.sst_lnum < prev.sst_lnum + dist)
            {
                /* Move this entry from used list to free list. */
                prev.sst_next = p.sst_next;
                syn_stack_free_entry(syn_block, p);
                p = prev;
                retval = true;
            }
        }

        return retval;
    }

    /*
     * Free the allocated memory for a syn_state item.
     * Move the entry into the free list.
     */
    /*private*/ static void syn_stack_free_entry(synblock_C block, synstate_C p)
    {
        clear_syn_state(p);
        p.sst_next = block.b_sst_firstfree;
        block.b_sst_firstfree = p;
        block.b_sst_freecount++;
    }

    /*
     * Find an entry in the list of state stacks at or before "lnum".
     * Returns null when there is no entry or the first entry is after "lnum".
     */
    /*private*/ static synstate_C syn_stack_find_entry(long lnum)
    {
        synstate_C prev = null;
        for (synstate_C p = syn_block.b_sst_first; p != null; prev = p, p = p.sst_next)
        {
            if (p.sst_lnum == lnum)
                return p;
            if (lnum < p.sst_lnum)
                break;
        }
        return prev;
    }

    /*
     * Try saving the current state in b_sst_array[].
     * The current state must be valid for the start of the current_lnum line!
     */
    /*private*/ static synstate_C store_current_state()
    {
        synstate_C sp = syn_stack_find_entry(current_lnum);

        /*
         * If the current state contains a start or end pattern that continues
         * from the previous line, we can't use it.  Don't store it then.
         */
        int ii;
        for (ii = current_state.ga_len - 1; 0 <= ii; --ii)
        {
            stateitem_C si = current_state.ga_data[ii];

            if (current_lnum <= si.si_h_startpos.lnum
                    || current_lnum <= si.si_m_endpos.lnum
                    || current_lnum <= si.si_h_endpos.lnum
                    || (si.si_end_idx != 0 && current_lnum <= si.si_eoe_pos.lnum))
                break;
        }
        if (0 <= ii)
        {
            if (sp != null)
            {
                /* find "sp" in the list and remove it */
                if (syn_block.b_sst_first == sp)
                    /* it's the first entry */
                    syn_block.b_sst_first = sp.sst_next;
                else
                {
                    /* find the entry just before this one to adjust sst_next */
                    synstate_C p;
                    for (p = syn_block.b_sst_first; p != null; p = p.sst_next)
                        if (p.sst_next == sp)
                            break;
                    if (p != null)  /* just in case */
                        p.sst_next = sp.sst_next;
                }
                syn_stack_free_entry(syn_block, sp);
                sp = null;
            }
        }
        else if (sp == null || sp.sst_lnum != current_lnum)
        {
            /*
             * Add a new entry
             */
            /* If no free items, cleanup the array first. */
            if (syn_block.b_sst_freecount == 0)
            {
                syn_stack_cleanup();
                /* "sp" may have been moved to the freelist now */
                sp = syn_stack_find_entry(current_lnum);
            }
            /* Still no free items?  Must be a strange problem... */
            if (syn_block.b_sst_freecount == 0)
                sp = null;
            else
            {
                /* Take the first item from the free list and put it in the used list, after "*sp". */
                synstate_C p = syn_block.b_sst_firstfree;
                syn_block.b_sst_firstfree = p.sst_next;
                --syn_block.b_sst_freecount;
                if (sp == null)
                {
                    /* Insert in front of the list. */
                    p.sst_next = syn_block.b_sst_first;
                    syn_block.b_sst_first = p;
                }
                else
                {
                    /* Insert in list after "*sp". */
                    p.sst_next = sp.sst_next;
                    sp.sst_next = p;
                }
                sp = p;
                sp.sst_stacksize = 0;
                sp.sst_lnum = current_lnum;
            }
        }
        if (sp != null)
        {
            /* When overwriting an existing state stack, clear it first. */
            clear_syn_state(sp);
            sp.sst_stacksize = current_state.ga_len;
            /* Need to clear it, might be something remaining
             * from when the length was less than SST_FIX_STATES. */
            sp.sst_ga.ga_clear();
            sp.sst_ga.ga_grow(sp.sst_stacksize);
            sp.sst_ga.ga_len = sp.sst_stacksize;

            bufstate_C[] bp = sp.sst_ga.ga_data;

            for (int i = 0; i < sp.sst_stacksize; i++)
            {
                stateitem_C si = current_state.ga_data[i];

                bp[i] = new bufstate_C();
                bp[i].bs_idx = si.si_idx;
                bp[i].bs_flags = si.si_flags;
                bp[i].bs_seqnr = si.si_seqnr;
                bp[i].bs_cchar = si.si_cchar;
                bp[i].bs_extmatch = ref_extmatch(si.si_extmatch);
            }
            sp.sst_next_flags = current_next_flags;
            sp.sst_next_list = current_next_list;
            sp.sst_tick = display_tick;
            sp.sst_change_lnum = 0;
        }
        current_state_stored = true;
        return sp;
    }

    /*
     * Copy a state stack from "from" in b_sst_array[] to current_state.
     */
    /*private*/ static void load_current_state(synstate_C from)
    {
        clear_current_state();
        validate_current_state();
        keepend_level = -1;

        if (0 < from.sst_stacksize)
        {
            current_state.ga_grow(from.sst_stacksize);

            bufstate_C[] bp = from.sst_ga.ga_data;

            for (int i = 0; i < from.sst_stacksize; i++)
            {
                stateitem_C si = current_state.ga_data[i] = new stateitem_C();

                si.si_idx = bp[i].bs_idx;
                si.si_flags = bp[i].bs_flags;
                si.si_seqnr = bp[i].bs_seqnr;
                si.si_cchar = bp[i].bs_cchar;
                si.si_extmatch = ref_extmatch(bp[i].bs_extmatch);
                if (keepend_level < 0 && (si.si_flags & HL_KEEPEND) != 0)
                    keepend_level = i;
                si.si_ends = false;
                si.si_m_lnum = 0;
                if (0 <= si.si_idx)
                {
                    synpat_C[] syn_items = syn_block.b_syn_patterns.ga_data;
                    si.si_next_list = syn_items[si.si_idx].sp_next_list;
                }
                else
                    si.si_next_list = null;
                update_si_attr(i);
            }

            current_state.ga_len = from.sst_stacksize;
        }

        current_next_list = from.sst_next_list;
        current_next_flags = from.sst_next_flags;
        current_lnum = from.sst_lnum;
    }

    /*
     * Compare saved state stack "*sp" with the current state.
     * Return true when they are equal.
     */
    /*private*/ static boolean syn_stack_equal(synstate_C sp)
    {
        /* First a quick check if the stacks have the same size end nextlist. */
        if (sp.sst_stacksize == current_state.ga_len && sp.sst_next_list == current_next_list)
        {
            /* Need to compare all states on both stacks. */
            bufstate_C[] bp = sp.sst_ga.ga_data;

            int i;
            for (i = current_state.ga_len; 0 <= --i; )
            {
                stateitem_C cur_si = current_state.ga_data[i];

                /* If the item has another index the state is different. */
                if (bp[i].bs_idx != cur_si.si_idx)
                    break;
                if (bp[i].bs_extmatch != cur_si.si_extmatch)
                {
                    /* When the extmatch pointers are different,
                     * the strings in them can still be the same.
                     * Check if the extmatch references are equal. */
                    reg_extmatch_C bsx = bp[i].bs_extmatch;
                    reg_extmatch_C six = cur_si.si_extmatch;
                    /* If one of the extmatch pointers is null the states are different. */
                    if (bsx == null || six == null)
                        break;
                    int j;
                    for (j = 0; j < NSUBEXP; j++)
                    {
                        /* Check each referenced match string.  They must all be equal. */
                        if (BNE(bsx.matches[j], six.matches[j]))
                        {
                            /* If the pointer is different it can still be the same text.
                             * Compare the strings, ignore case when the start item has
                             * the sp_ic flag set. */
                            if (bsx.matches[j] == null || six.matches[j] == null)
                                break;

                            synpat_C[] syn_items = syn_block.b_syn_patterns.ga_data;
                            if (syn_items[cur_si.si_idx].sp_ic
                                    ? us_strnicmp(bsx.matches[j], six.matches[j], MAXCOL) != 0
                                    : STRCMP(bsx.matches[j], six.matches[j]) != 0)
                                break;
                        }
                    }
                    if (j != NSUBEXP)
                        break;
                }
            }
            if (i < 0)
                return true;
        }
        return false;
    }

    /*
     * We stop parsing syntax above line "lnum".  If the stored state at or below
     * this line depended on a change before it, it now depends on the line below
     * the last parsed line.
     * The window looks like this:
     *          line which changed
     *          displayed line
     *          displayed line
     * lnum ->  line below window
     */
    /*private*/ static void syntax_end_parsing(long lnum)
    {
        synstate_C sp = syn_stack_find_entry(lnum);
        if (sp != null && sp.sst_lnum < lnum)
            sp = sp.sst_next;

        if (sp != null && sp.sst_change_lnum != 0)
            sp.sst_change_lnum = lnum;
    }

    /*
     * End of handling of the state stack.
     ****************************************/

    /*private*/ static void invalidate_current_state()
    {
        clear_current_state();
        current_state_is_valid = false;
        current_next_list = null;
        keepend_level = -1;
    }

    /*private*/ static void validate_current_state()
    {
        current_state_is_valid = true;
    }

    /*
     * Return true if the syntax at start of lnum changed since last time.
     * This will only be called just after get_syntax_attr() for the previous line,
     * to check if the next line needs to be redrawn too.
     */
    /*private*/ static boolean syntax_check_changed(long lnum)
    {
        boolean retval = true;

        /*
         * Check the state stack when:
         * - lnum is just below the previously syntaxed line.
         * - lnum is not before the lines with saved states.
         * - lnum is not past the lines with saved states.
         * - lnum is at or before the last changed line.
         */
        if (current_state_is_valid && lnum == current_lnum + 1)
        {
            synstate_C sp = syn_stack_find_entry(lnum);
            if (sp != null && sp.sst_lnum == lnum)
            {
                /*
                 * finish the previous line (needed when not all of the line was drawn)
                 */
                syn_finish_line(false);

                /*
                 * Compare the current state with the previously saved state of the line.
                 */
                if (syn_stack_equal(sp))
                    retval = false;

                /*
                 * Store the current state in b_sst_array[] for later use.
                 */
                current_lnum++;
                store_current_state();
            }
        }

        return retval;
    }

    /*
     * Finish the current line.
     * This doesn't return any attributes, it only gets the state at the end of the line.
     * It can start anywhere in the line, as long as the current state is valid.
     */
    /*private*/ static boolean syn_finish_line(boolean syncing)
        /* syncing: called for syncing */
    {
        if (!current_finished)
        {
            while (!current_finished)
            {
                syn_current_attr(syncing, false, false);
                /*
                 * When syncing, and found some item, need to check the item.
                 */
                if (syncing && current_state.ga_len != 0)
                {
                    /*
                     * Check for match with sync item.
                     */
                    stateitem_C cur_si = current_state.ga_data[current_state.ga_len - 1];
                    synpat_C[] syn_items = syn_block.b_syn_patterns.ga_data;
                    if (0 <= cur_si.si_idx
                            && (syn_items[cur_si.si_idx].sp_flags & (HL_SYNC_HERE|HL_SYNC_THERE)) != 0)
                        return true;

                    /* syn_current_attr() will have skipped the check for an item that ends here,
                     * need to do that now.  Be careful not to go past the NUL. */
                    int prev_current_col = current_col;
                    if (syn_getcurline().at(current_col) != NUL)
                        current_col++;
                    check_state_ends();
                    current_col = prev_current_col;
                }
                current_col++;
            }
        }

        return false;
    }

    /*
     * Return highlight attributes for next character.
     * Must first call syntax_start() once for the line.
     * "col" is normally 0 for the first use in a line, and increments by one each
     * time.  It's allowed to skip characters and to stop before the end of the
     * line.  But only a "col" after a previously used column is allowed.
     */
    /*private*/ static int get_syntax_attr(int col, boolean keep_state)
        /* keep_state: keep state of char at "col" */
    {
        int attr = 0;

        /* check for out of memory situation */
        if (syn_block.b_sst_array == null)
            return 0;

        /* After 'synmaxcol' the attribute is always zero. */
        if (0 < syn_buf.b_p_smc[0] && (int)syn_buf.b_p_smc[0] <= col)
        {
            clear_current_state();
            current_id = 0;
            current_trans_id = 0;
            current_flags = 0;
            return 0;
        }

        /* Make sure current_state is valid. */
        if (!current_state_is_valid)
            validate_current_state();

        /*
         * Skip from the current column to "col", get the attributes for "col".
         */
        while (current_col <= col)
        {
            attr = syn_current_attr(false, true, (current_col == col) ? keep_state : false);
            current_col++;
        }

        return attr;
    }

    /*private*/ static boolean try_next_column;     /* must try in next col */

    /*
     * Get syntax attributes for current_lnum, current_col.
     */
    /*private*/ static int syn_current_attr(boolean syncing, boolean displaying, boolean keep_state)
        /* syncing: When 1: called for syncing */
        /* displaying: result will be displayed */
        /* keep_state: keep syntax stack afterwards */
    {
        reg_extmatch_C cur_extmatch = null;

        /* variables for zero-width matches that have a "nextgroup" argument */
        boolean zero_width_next_list = false;

        /*
         * No character, no attributes!  Past end of line?
         * Do try matching with an empty line (could be the start of a region).
         */
        Bytes line = syn_getcurline(); /* Current line.
                                        * NOTE: becomes invalid after looking for a pattern match! */
        if (line.at(current_col) == NUL && current_col != 0)
        {
            /*
             * If we found a match after the last column, use it.
             */
            if (0 <= next_match_idx && current_col <= next_match_col && next_match_col != MAXCOL)
                push_next_match();

            current_finished = true;
            current_state_stored = false;
            return 0;
        }

        /* if the current or next character is NUL, we will finish the line now */
        if (line.at(current_col) == NUL || line.at(current_col + 1) == NUL)
        {
            current_finished = true;
            current_state_stored = false;
        }

        /*
         * When in the previous column there was a match but it could not be used
         * (empty match or already matched in this column) need to try again in the next column.
         */
        if (try_next_column)
        {
            next_match_idx = -1;
            try_next_column = false;
        }

        /* Only check for keywords when not syncing and there are some. */
        boolean do_keywords = !syncing
                        && (0 < syn_block.b_keywtab.ht_used || 0 < syn_block.b_keywtab_ic.ht_used);

        /* Init the list of zero-width matches with a nextlist.
         * This is used to avoid matching the same item in the same position twice. */
        iarray_C zero_width_next = new iarray_C(10);

        stateitem_C cur_si;
        /*
         * Repeat matching keywords and patterns, to find contained items at the same column.
         * This stops when there are no extra matches at the current column.
         */
        boolean found_match;                /* found usable match */
        do
        {
            found_match = false;
            boolean keep_next_list = false;
            int syn_id = 0;

            /*
             * 1. Check for a current state.
             *    Only when there is no current state, or if the current state may
             *    contain other things, we need to check for keywords and patterns.
             *    Always need to check for contained items if some item has the
             *    "containedin" argument (takes extra time!).
             */
            int cur_i = current_state.ga_len - 1;
            cur_si = (cur_i < 0) ? null : current_state.ga_data[cur_i];

            if (syn_block.b_syn_containedin || cur_si == null || cur_si.si_cont_list != null)
            {
                /*
                 * 2. Check for keywords, if on a keyword char after a non-keyword char.
                 *    Don't do this when syncing.
                 */
                if (do_keywords)
                {
                    line = syn_getcurline();
                    if (us_iswordp(line.plus(current_col), syn_buf)
                            && (current_col == 0
                                || !us_iswordp(line.plus(current_col - 1 - us_head_off(line, line.plus(current_col - 1))), syn_buf)))
                    {
                        int[] endcol = new int[1];
                        int[] flags = new int[1];
                        short[][] next_list = new short[1][];
                        int[] cchar = new int[1];
                        syn_id = check_keyword_id(line, current_col,
                                                endcol, flags, next_list, cur_i, cur_si, cchar);
                        if (syn_id != 0)
                        {
                            push_current_state(KEYWORD_IDX);

                            cur_i = current_state.ga_len - 1;
                            cur_si = current_state.ga_data[cur_i];

                            cur_si.si_m_startcol = current_col;
                            cur_si.si_h_startpos.lnum = current_lnum;
                            cur_si.si_h_startpos.col = 0; /* starts right away */
                            cur_si.si_m_endpos.lnum = current_lnum;
                            cur_si.si_m_endpos.col = endcol[0];
                            cur_si.si_h_endpos.lnum = current_lnum;
                            cur_si.si_h_endpos.col = endcol[0];
                            cur_si.si_ends = true;
                            cur_si.si_end_idx = 0;
                            cur_si.si_flags = flags[0];
                            cur_si.si_seqnr = next_seqnr++;
                            cur_si.si_cchar = cchar[0];
                            if (1 < current_state.ga_len)
                            {
                                stateitem_C siip = current_state.ga_data[current_state.ga_len - 2];
                                cur_si.si_flags |= siip.si_flags & HL_CONCEAL;
                            }
                            cur_si.si_id = syn_id;
                            cur_si.si_trans_id = syn_id;
                            if ((flags[0] & HL_TRANSP) != 0)
                            {
                                if (current_state.ga_len < 2)
                                {
                                    cur_si.si_attr = 0;
                                    cur_si.si_trans_id = 0;
                                }
                                else
                                {
                                    stateitem_C siip = current_state.ga_data[current_state.ga_len - 2];
                                    cur_si.si_attr = siip.si_attr;
                                    cur_si.si_trans_id = siip.si_trans_id;
                                }
                            }
                            else
                                cur_si.si_attr = syn_id2attr(syn_id);
                            cur_si.si_cont_list = null;
                            cur_si.si_next_list = next_list[0];
                            check_keepend();
                        }
                    }
                }

                /*
                 * 3. Check for patterns (only if no keyword found).
                 */
                if (syn_id == 0 && syn_block.b_syn_patterns.ga_len != 0)
                {
                    /*
                     * If we didn't check for a match yet, or we are past it,
                     * check for any match with a pattern.
                     */
                    if (next_match_idx < 0 || next_match_col < current_col)
                    {
                        regmmatch_C regmatch = new regmmatch_C();
                        lpos_C pos = new lpos_C();
                        lpos_C endpos = new lpos_C();
                        lpos_C hl_startpos = new lpos_C();
                        lpos_C eos_pos = new lpos_C(); /* end-of-start match (start region) */
                        lpos_C eoe_pos = new lpos_C(); /* end-of-end pattern */
                        lpos_C hl_endpos = new lpos_C();

                        /*
                         * Check all relevant patterns for a match at this position.
                         * This is complicated, because matching with a pattern takes quite
                         * a bit of time, thus we want to avoid doing it when it's not needed.
                         */
                        next_match_idx = 0;         /* no match in this line yet */
                        next_match_col = MAXCOL;
                        for (int idx = syn_block.b_syn_patterns.ga_len; 0 <= --idx; )
                        {
                            synpat_C[] syn_items = syn_block.b_syn_patterns.ga_data;
                            synpat_C spp = syn_items[idx];

                            if (spp.sp_syncing == syncing
                                    && (displaying || (spp.sp_flags & HL_DISPLAY) == 0)
                                    && (spp.sp_type == SPTYPE_MATCH || spp.sp_type == SPTYPE_START)
                                    && (current_next_list != null
                                        ? in_id_list(-1, null, current_next_list, spp.sp_syn, false)
                                        : (cur_si == null
                                            ? (spp.sp_flags & HL_CONTAINED) == 0
                                            : in_id_list(cur_i, cur_si, cur_si.si_cont_list, spp.sp_syn,
                                                                        (spp.sp_flags & HL_CONTAINED) != 0))))
                            {
                                /* If we already tried matching in this line and there isn't
                                 * a match before next_match_col, skip this item. */
                                if (spp.sp_line_id == current_line_id && next_match_col <= spp.sp_startcol)
                                    continue;
                                spp.sp_line_id = current_line_id;

                                int lc_col = current_col - spp.sp_offsets[SPO_LC_OFF];
                                if (lc_col < 0)
                                    lc_col = 0;

                                regmatch.rmm_ic = spp.sp_ic;
                                regmatch.regprog = spp.sp_prog;
                                boolean r = syn_regexec(regmatch, current_lnum, lc_col);
                                spp.sp_prog = regmatch.regprog;
                                if (!r)
                                {
                                    /* no match in this line, try another one */
                                    spp.sp_startcol = MAXCOL;
                                    continue;
                                }

                                /*
                                 * Compute the first column of the match.
                                 */
                                syn_add_start_off(pos, regmatch, spp, SPO_MS_OFF, -1);
                                if (current_lnum < pos.lnum)
                                {
                                    /* must have used end of match in a next line, we can't handle that */
                                    spp.sp_startcol = MAXCOL;
                                    continue;
                                }
                                int startcol = pos.col;

                                /* remember the next column where this pattern matches in the current line */
                                spp.sp_startcol = startcol;

                                /*
                                 * If a previously found match starts at a lower column number,
                                 * don't use this one.
                                 */
                                if (next_match_col <= startcol)
                                    continue;

                                /*
                                 * If we matched this pattern at this position before, skip it.
                                 * Must retry in the next column, because it may match from there.
                                 */
                                if (did_match_already(idx, zero_width_next))
                                {
                                    try_next_column = true;
                                    continue;
                                }

                                endpos.lnum = regmatch.endpos[0].lnum;
                                endpos.col = regmatch.endpos[0].col;

                                /* Compute the highlight start. */
                                syn_add_start_off(hl_startpos, regmatch, spp, SPO_HS_OFF, -1);

                                /*
                                 * Compute the region start.
                                 * Default is to use the end of the match.
                                 */
                                syn_add_end_off(eos_pos, regmatch, spp, SPO_RS_OFF, 0);

                                /*
                                 * Grab the external submatches before they get overwritten.
                                 * Reference count doesn't change.
                                 */
                                cur_extmatch = re_extmatch_out;
                                re_extmatch_out = null;

                                int[] flags = { 0 };

                                eoe_pos.lnum = 0;           /* avoid warning */
                                eoe_pos.col = 0;

                                int[] end_idx = { 0 };            /* group ID for end pattern */

                                hl_endpos.lnum = 0;

                                /*
                                 * For a "oneline" the end must be found in the same line too.
                                 * Search for it after the end of the match with the start pattern.
                                 * Set the resulting end positions at the same time.
                                 */
                                if (spp.sp_type == SPTYPE_START && (spp.sp_flags & HL_ONELINE) != 0)
                                {
                                    lpos_C startpos = new lpos_C();
                                    COPY_lpos(startpos, endpos);
                                    find_endpos(idx, startpos, endpos, hl_endpos,
                                                        flags, eoe_pos, end_idx, cur_extmatch);
                                    if (endpos.lnum == 0)
                                        continue;               /* not found */
                                }

                                /*
                                 * For a "match" the size must be > 0 after the end offset needs has been added.
                                 * Except when syncing.
                                 */
                                else if (spp.sp_type == SPTYPE_MATCH)
                                {
                                    syn_add_end_off(hl_endpos, regmatch, spp, SPO_HE_OFF, 0);
                                    syn_add_end_off(endpos, regmatch, spp, SPO_ME_OFF, 0);
                                    if (endpos.lnum == current_lnum && endpos.col + (syncing ? 1 : 0) < startcol)
                                    {
                                        /*
                                         * If an empty string is matched,
                                         * may need to try matching again at next column.
                                         */
                                        if (regmatch.startpos[0].col == regmatch.endpos[0].col)
                                            try_next_column = true;
                                        continue;
                                    }
                                }

                                /*
                                 * keep the best match so far in next_match_*
                                 */
                                /* Highlighting must start after startpos and end before endpos. */
                                if (hl_startpos.lnum == current_lnum && hl_startpos.col < startcol)
                                    hl_startpos.col = startcol;
                                limit_pos_zero(hl_endpos, endpos);

                                next_match_idx = idx;
                                next_match_col = startcol;
                                COPY_lpos(next_match_m_endpos, endpos);
                                COPY_lpos(next_match_h_endpos, hl_endpos);
                                COPY_lpos(next_match_h_startpos, hl_startpos);
                                next_match_flags = flags[0];
                                COPY_lpos(next_match_eos_pos, eos_pos);
                                COPY_lpos(next_match_eoe_pos, eoe_pos);
                                next_match_end_idx = end_idx[0];
                                next_match_extmatch = cur_extmatch;
                                cur_extmatch = null;
                            }
                        }
                    }

                    /*
                     * If we found a match at the current column, use it.
                     */
                    if (0 <= next_match_idx && next_match_col == current_col)
                    {
                        synpat_C[] syn_items = syn_block.b_syn_patterns.ga_data;
                        synpat_C lspp = syn_items[next_match_idx];

                        /* When a zero-width item matched which has a nextgroup,
                         * don't push the item but set nextgroup. */
                        if (next_match_m_endpos.lnum == current_lnum
                                && next_match_m_endpos.col == current_col
                                && lspp.sp_next_list != null)
                        {
                            current_next_list = lspp.sp_next_list;
                            current_next_flags = lspp.sp_flags;
                            keep_next_list = true;
                            zero_width_next_list = true;

                            /* Add the index to a list, so that we can check later
                             * that we don't match it again (and cause an endless loop). */
                            ia_grow(zero_width_next, 1);
                            zero_width_next.ia_data[zero_width_next.ia_len++] = next_match_idx;
                            next_match_idx = -1;
                        }
                        else
                        {
                            cur_i = push_next_match();
                            cur_si = current_state.ga_data[cur_i];
                        }
                        found_match = true;
                    }
                }
            }

            /*
             * Handle searching for nextgroup match.
             */
            if (current_next_list != null && !keep_next_list)
            {
                /*
                 * If a nextgroup was not found, continue looking for one if:
                 * - this is an empty line and the "skipempty" option was given
                 * - we are on white space and the "skipwhite" option was given
                 */
                if (!found_match)
                {
                    line = syn_getcurline();
                    if (((current_next_flags & HL_SKIPWHITE) != 0 && vim_iswhite(line.at(current_col)))
                            || ((current_next_flags & HL_SKIPEMPTY) != 0 && line.at(0) == NUL))
                        break;
                }

                /*
                 * If a nextgroup was found: Use it, and continue looking for contained matches.
                 * If a nextgroup was not found: Continue looking for a normal match.
                 * When did set current_next_list for a zero-width item and no
                 * match was found don't loop (would get stuck).
                 */
                current_next_list = null;
                next_match_idx = -1;
                if (!zero_width_next_list)
                    found_match = true;
            }
        } while (found_match);

        /*
         * Use attributes from the current state, if within its highlighting.
         * If not, use attributes from the current-but-one state, etc.
         */
        current_attr = 0;
        current_id = 0;
        current_trans_id = 0;
        current_flags = 0;
        if (cur_si != null)
        {
            for (int idx = current_state.ga_len - 1; 0 <= idx; --idx)
            {
                stateitem_C sip = current_state.ga_data[idx];

                if ((sip.si_h_startpos.lnum < current_lnum
                        || (current_lnum == sip.si_h_startpos.lnum && sip.si_h_startpos.col <= current_col))
                    && (sip.si_h_endpos.lnum == 0
                        || current_lnum < sip.si_h_endpos.lnum
                        || (current_lnum == sip.si_h_endpos.lnum && current_col < sip.si_h_endpos.col)))
                {
                    current_attr = sip.si_attr;
                    current_id = sip.si_id;
                    current_trans_id = sip.si_trans_id;
                    current_flags = sip.si_flags;
                    current_seqnr = sip.si_seqnr;
                    current_sub_char = sip.si_cchar;
                    break;
                }
            }

            /*
             * Check for end of current state (and the states before it) at the
             * next column.  Don't do this for syncing, because we would miss a
             * single character match.
             * First check if the current state ends at the current column.  It
             * may be for an empty match and a containing item might end in the
             * current column.
             */
            if (!syncing && !keep_state)
            {
                check_state_ends();
                if (0 < current_state.ga_len && syn_getcurline().at(current_col) != NUL)
                {
                    current_col++;
                    check_state_ends();
                    --current_col;
                }
            }
        }

        /* nextgroup ends at end of line, unless "skipnl" or "skipempty" present */
        if (current_next_list != null
                && syn_getcurline().at(current_col + 1) == NUL
                && (current_next_flags & (HL_SKIPNL | HL_SKIPEMPTY)) == 0)
            current_next_list = null;

        /* No longer need external matches.  But keep next_match_extmatch. */
        re_extmatch_out = null;

        return current_attr;
    }

    /*
     * Check if we already matched pattern "idx" at the current column.
     */
    /*private*/ static boolean did_match_already(int idx, iarray_C iap)
    {
        for (int i = current_state.ga_len; 0 <= --i; )
        {
            stateitem_C si = current_state.ga_data[i];

            if (si.si_m_startcol == current_col && si.si_m_lnum == (int)current_lnum && si.si_idx == idx)
                return true;
        }

        /* Zero-width matches with a nextgroup argument are not put on the syntax stack,
         * and can only be matched once anyway. */
        for (int i = iap.ia_len; 0 <= --i; )
            if (iap.ia_data[i] == idx)
                return true;

        return false;
    }

    /*
     * Push the next match onto the stack.
     */
    /*private*/ static int push_next_match()
    {
        synpat_C[] syn_items = syn_block.b_syn_patterns.ga_data;
        synpat_C spp = syn_items[next_match_idx];

        /*
         * Push the item in current_state stack;
         */
        push_current_state(next_match_idx);

        /*
         * If it's a start-skip-end type that crosses lines, figure out how
         * much it continues in this line.  Otherwise just fill in the length.
         */
        int cur_i = current_state.ga_len - 1;
        stateitem_C cur_si = current_state.ga_data[cur_i];

        COPY_lpos(cur_si.si_h_startpos, next_match_h_startpos);
        cur_si.si_m_startcol = current_col;
        cur_si.si_m_lnum = current_lnum;
        cur_si.si_flags = spp.sp_flags;
        cur_si.si_seqnr = next_seqnr++;
        cur_si.si_cchar = spp.sp_cchar;
        if (1 < current_state.ga_len)
        {
            stateitem_C siip = current_state.ga_data[current_state.ga_len - 2];
            cur_si.si_flags |= siip.si_flags & HL_CONCEAL;
        }
        cur_si.si_next_list = spp.sp_next_list;
        cur_si.si_extmatch = ref_extmatch(next_match_extmatch);
        if (spp.sp_type == SPTYPE_START && (spp.sp_flags & HL_ONELINE) == 0)
        {
            /* Try to find the end pattern in the current line. */
            update_si_end(cur_si, next_match_m_endpos.col, true);
            check_keepend();
        }
        else
        {
            COPY_lpos(cur_si.si_m_endpos, next_match_m_endpos);
            COPY_lpos(cur_si.si_h_endpos, next_match_h_endpos);
            cur_si.si_ends = true;
            cur_si.si_flags |= next_match_flags;
            COPY_lpos(cur_si.si_eoe_pos, next_match_eoe_pos);
            cur_si.si_end_idx = next_match_end_idx;
        }
        if (keepend_level < 0 && (cur_si.si_flags & HL_KEEPEND) != 0)
            keepend_level = current_state.ga_len - 1;
        check_keepend();
        update_si_attr(current_state.ga_len - 1);

        long save_flags = cur_si.si_flags & (HL_CONCEAL | HL_CONCEALENDS);
        /*
         * If the start pattern has another highlight group,
         * push another item on the stack for the start pattern.
         */
        if (spp.sp_type == SPTYPE_START && spp.sp_syn_match_id != 0)
        {
            push_current_state(next_match_idx);

            cur_i = current_state.ga_len - 1;
            cur_si = current_state.ga_data[cur_i];

            COPY_lpos(cur_si.si_h_startpos, next_match_h_startpos);
            cur_si.si_m_startcol = current_col;
            cur_si.si_m_lnum = current_lnum;
            COPY_lpos(cur_si.si_m_endpos, next_match_eos_pos);
            COPY_lpos(cur_si.si_h_endpos, next_match_eos_pos);
            cur_si.si_ends = true;
            cur_si.si_end_idx = 0;
            cur_si.si_flags = HL_MATCH;
            cur_si.si_seqnr = next_seqnr++;
            cur_si.si_flags |= save_flags;
            if ((cur_si.si_flags & HL_CONCEALENDS) != 0)
                cur_si.si_flags |= HL_CONCEAL;
            cur_si.si_next_list = null;
            check_keepend();
            update_si_attr(current_state.ga_len - 1);
        }

        next_match_idx = -1;        /* try other match next time */

        return cur_i;
    }

    /*
     * Check for end of current state (and the states before it).
     */
    /*private*/ static void check_state_ends()
    {
        stateitem_C cur_si = current_state.ga_data[current_state.ga_len - 1];

        for ( ; ; )
        {
            if (cur_si.si_ends
                    && (cur_si.si_m_endpos.lnum < current_lnum
                        || (cur_si.si_m_endpos.lnum == current_lnum
                            && cur_si.si_m_endpos.col <= current_col)))
            {
                /*
                 * If there is an end pattern group ID, highlight the end pattern now.
                 * No need to pop the current item from the stack.
                 * Only do this if the end pattern continues beyond the current position.
                 */
                if (cur_si.si_end_idx != 0
                        && (current_lnum < cur_si.si_eoe_pos.lnum
                            || (cur_si.si_eoe_pos.lnum == current_lnum
                                && current_col < cur_si.si_eoe_pos.col)))
                {
                    cur_si.si_idx = cur_si.si_end_idx;
                    cur_si.si_end_idx = 0;
                    COPY_lpos(cur_si.si_m_endpos, cur_si.si_eoe_pos);
                    COPY_lpos(cur_si.si_h_endpos, cur_si.si_eoe_pos);
                    cur_si.si_flags |= HL_MATCH;
                    cur_si.si_seqnr = next_seqnr++;
                    if ((cur_si.si_flags & HL_CONCEALENDS) != 0)
                        cur_si.si_flags |= HL_CONCEAL;
                    update_si_attr(current_state.ga_len - 1);

                    /* nextgroup= should not match in the end pattern */
                    current_next_list = null;

                    /* what matches next may be different now, clear it */
                    next_match_idx = 0;
                    next_match_col = MAXCOL;
                    break;
                }
                else
                {
                    /* handle next_list, unless at end of line and no "skipnl" or "skipempty" */
                    current_next_list = cur_si.si_next_list;
                    current_next_flags = cur_si.si_flags;
                    if ((current_next_flags & (HL_SKIPNL | HL_SKIPEMPTY)) == 0
                            && syn_getcurline().at(current_col) == NUL)
                        current_next_list = null;

                    /* When the ended item has "extend", another item with
                     * "keepend" now needs to check for its end. */
                    boolean had_extend = ((cur_si.si_flags & HL_EXTEND) != 0);

                    pop_current_state();

                    if (current_state.ga_len == 0)
                        break;

                    if (had_extend && 0 <= keepend_level)
                    {
                        syn_update_ends(false);
                        if (current_state.ga_len == 0)
                            break;
                    }

                    cur_si = current_state.ga_data[current_state.ga_len - 1];

                    /*
                     * Only for a region the search for the end continues after
                     * the end of the contained item.  If the contained match
                     * included the end-of-line, break here, the region continues.
                     * Don't do this when:
                     * - "keepend" is used for the contained item
                     * - not at the end of the line (could be end="x$"me=e-1).
                     * - "excludenl" is used (HL_HAS_EOL won't be set)
                     */
                    synpat_C[] syn_items = syn_block.b_syn_patterns.ga_data;
                    if (0 <= cur_si.si_idx
                            && syn_items[cur_si.si_idx].sp_type == SPTYPE_START
                            && (cur_si.si_flags & (HL_MATCH | HL_KEEPEND)) == 0)
                    {
                        update_si_end(cur_si, current_col, true);
                        check_keepend();
                        if ((current_next_flags & HL_HAS_EOL) != 0
                                && keepend_level < 0
                                && syn_getcurline().at(current_col) == NUL)
                            break;
                    }
                }
            }
            else
                break;
        }
    }

    /*
     * Update an entry in the current_state stack for a match or region.
     * This fills in si_attr, si_next_list and si_cont_list.
     */
    /*private*/ static void update_si_attr(int idx)
    {
        stateitem_C sip = current_state.ga_data[idx];

        /* This should not happen... */
        if (sip.si_idx < 0)
            return;

        synpat_C[] syn_items = syn_block.b_syn_patterns.ga_data;
        synpat_C spp = syn_items[sip.si_idx];

        if ((sip.si_flags & HL_MATCH) != 0)
            sip.si_id = spp.sp_syn_match_id;
        else
            sip.si_id = spp.sp_syn.id;
        sip.si_attr = syn_id2attr(sip.si_id);
        sip.si_trans_id = sip.si_id;
        if ((sip.si_flags & HL_MATCH) != 0)
            sip.si_cont_list = null;
        else
            sip.si_cont_list = spp.sp_cont_list;

        /*
         * For transparent items, take attr from outer item.
         * Also take cont_list, if there is none.
         * Don't do this for the matchgroup of a start or end pattern.
         */
        if ((spp.sp_flags & HL_TRANSP) != 0 && (sip.si_flags & HL_MATCH) == 0)
        {
            if (idx == 0)
            {
                sip.si_attr = 0;
                sip.si_trans_id = 0;
                if (sip.si_cont_list == null)
                    sip.si_cont_list = ID_LIST_ALL;
            }
            else
            {
                stateitem_C siip = current_state.ga_data[idx - 1];

                sip.si_attr = siip.si_attr;
                sip.si_trans_id = siip.si_trans_id;
                COPY_lpos(sip.si_h_startpos, siip.si_h_startpos);
                COPY_lpos(sip.si_h_endpos, siip.si_h_endpos);
                if (sip.si_cont_list == null)
                {
                    sip.si_flags |= HL_TRANS_CONT;
                    sip.si_cont_list = siip.si_cont_list;
                }
            }
        }
    }

    /*
     * Check the current stack for patterns with "keepend" flag.
     * Propagate the match-end to contained items, until a "skipend" item is found.
     */
    /*private*/ static void check_keepend()
    {
        /*
         * This check can consume a lot of time;
         * only do it from the level where there really is a keepend.
         */
        if (keepend_level < 0)
            return;

        /*
         * Find the last index of an "extend" item.  "keepend" items before that
         * won't do anything.  If there is no "extend" item "i" will be
         * "keepend_level" and all "keepend" items will work normally.
         */
        int i;
        for (i = current_state.ga_len - 1; keepend_level < i; --i)
        {
            stateitem_C sip = current_state.ga_data[i];
            if ((sip.si_flags & HL_EXTEND) != 0)
                break;
        }

        lpos_C maxpos = new lpos_C();
        maxpos.lnum = 0;
        maxpos.col = 0;

        lpos_C maxpos_h = new lpos_C();
        maxpos_h.lnum = 0;
        maxpos_h.col = 0;

        for ( ; i < current_state.ga_len; i++)
        {
            stateitem_C sip = current_state.ga_data[i];
            if (maxpos.lnum != 0)
            {
                limit_pos_zero(sip.si_m_endpos, maxpos);
                limit_pos_zero(sip.si_h_endpos, maxpos_h);
                limit_pos_zero(sip.si_eoe_pos, maxpos);
                sip.si_ends = true;
            }
            if (sip.si_ends && (sip.si_flags & HL_KEEPEND) != 0)
            {
                if (maxpos.lnum == 0
                        || sip.si_m_endpos.lnum < maxpos.lnum
                        || (maxpos.lnum == sip.si_m_endpos.lnum && sip.si_m_endpos.col < maxpos.col))
                    COPY_lpos(maxpos, sip.si_m_endpos);
                if (maxpos_h.lnum == 0
                        || sip.si_h_endpos.lnum < maxpos_h.lnum
                        || (maxpos_h.lnum == sip.si_h_endpos.lnum && sip.si_h_endpos.col < maxpos_h.col))
                    COPY_lpos(maxpos_h, sip.si_h_endpos);
            }
        }
    }

    /*
     * Update an entry in the current_state stack for a start-skip-end pattern.
     * This finds the end of the current item, if it's in the current line.
     *
     * Return the flags for the matched END.
     */
    /*private*/ static void update_si_end(stateitem_C sip, int startcol, boolean force)
        /* startcol: where to start searching for the end */
        /* force: when true overrule a previous end */
    {
        /* return quickly for a keyword */
        if (sip.si_idx < 0)
            return;

        /* Don't update when it's already done.  Can be a match of an end pattern
         * that started in a previous line.  Watch out: can also be a "keepend"
         * from a containing item. */
        if (!force && current_lnum <= sip.si_m_endpos.lnum)
            return;

        /*
         * We need to find the end of the region.  It may continue in the next line.
         */
        int[] end_idx = { 0 };

        lpos_C startpos = new lpos_C();
        startpos.lnum = current_lnum;
        startpos.col = startcol;

        lpos_C endpos = new lpos_C();
        lpos_C hl_endpos = new lpos_C();
        lpos_C end_endpos = new lpos_C();

        { int[] __ = { sip.si_flags }; find_endpos(sip.si_idx, startpos, endpos, hl_endpos, __, end_endpos, end_idx, sip.si_extmatch); sip.si_flags = __[0]; }

        if (endpos.lnum == 0)
        {
            synpat_C[] syn_items = syn_block.b_syn_patterns.ga_data;

            /* No end pattern matched. */
            if ((syn_items[sip.si_idx].sp_flags & HL_ONELINE) != 0)
            {
                /* a "oneline" never continues in the next line */
                sip.si_ends = true;
                sip.si_m_endpos.lnum = current_lnum;
                sip.si_m_endpos.col = strlen(syn_getcurline());
            }
            else
            {
                /* continues in the next line */
                sip.si_ends = false;
                sip.si_m_endpos.lnum = 0;
            }
            COPY_lpos(sip.si_h_endpos, sip.si_m_endpos);
        }
        else
        {
            /* match within this line */
            COPY_lpos(sip.si_m_endpos, endpos);
            COPY_lpos(sip.si_h_endpos, hl_endpos);
            COPY_lpos(sip.si_eoe_pos, end_endpos);
            sip.si_ends = true;
            sip.si_end_idx = end_idx[0];
        }
    }

    /*
     * Add a new state to the current state stack.
     * It is cleared and the index set to "idx".
     */
    /*private*/ static void push_current_state(int idx)
    {
        current_state.ga_grow(1);

        stateitem_C sip = current_state.ga_data[current_state.ga_len++] = new stateitem_C();
        sip.si_idx = idx;
    }

    /*
     * Remove a state from the current_state stack.
     */
    /*private*/ static void pop_current_state()
    {
        if (0 < current_state.ga_len)
            current_state.ga_data[--current_state.ga_len] = null;

        /* after the end of a pattern, try matching a keyword or pattern */
        next_match_idx = -1;

        /* if first state with "keepend" is popped, reset keepend_level */
        if (current_state.ga_len <= keepend_level)
            keepend_level = -1;
    }

    /*
     * Find the end of a start/skip/end syntax region after "startpos".
     * Only checks one line.
     * Also handles a match item that continued from a previous line.
     * If not found, the syntax item continues in the next line.  m_endpos.lnum will be 0.
     * If found, the end of the region and the end of the highlighting is computed.
     */
    /*private*/ static void find_endpos(int idx, lpos_C startpos, lpos_C m_endpos, lpos_C hl_endpos, int[] flagsp, lpos_C end_endpos, int[] end_idx, reg_extmatch_C start_ext)
        /* idx: index of the pattern */
        /* startpos: where to start looking for an END match */
        /* m_endpos: return: end of match */
        /* hl_endpos: return: end of highlighting */
        /* flagsp: return: flags of matching END */
        /* end_endpos: return: end of end pattern match */
        /* end_idx: return: group ID for end pat. match, or 0 */
        /* start_ext: submatches from the start pattern */
    {
        if (idx < 0)    /* just in case we are invoked for a keyword */
            return;

        /*
         * Check for being called with a START pattern.
         * Can happen with a match that continues to the next line,
         * because it contained a region.
         */
        synpat_C[] syn_items = syn_block.b_syn_patterns.ga_data;
        synpat_C spp = syn_items[idx];
        if (spp.sp_type != SPTYPE_START)
        {
            COPY_lpos(hl_endpos, startpos);
            return;
        }

        /*
         * Find the SKIP or first END pattern after the last START pattern.
         */
        for ( ; ; )
        {
            spp = syn_items[idx];
            if (spp.sp_type != SPTYPE_START)
                break;
            idx++;
        }

        /*
         *  Lookup the SKIP pattern (if present)
         */
        synpat_C spp_skip;
        if (spp.sp_type == SPTYPE_SKIP)
        {
            spp_skip = spp;
            idx++;
        }
        else
            spp_skip = null;

        /* Setup external matches for syn_regexec(). */
        re_extmatch_in = ref_extmatch(start_ext);

        int matchcol = startpos.col;                    /* start looking for a match at sstart */
        int start_idx = idx;                            /* remember the first END pattern. */

        regmmatch_C best_regmatch = new regmmatch_C();  /* startpos/endpos of best match */
        best_regmatch.startpos[0].col = 0;

        regmmatch_C regmatch = new regmmatch_C();

        boolean had_match = false;

        for ( ; ; )
        {
            /*
             * Find end pattern that matches first after "matchcol".
             */
            int best_idx = -1;
            for (idx = start_idx; idx < syn_block.b_syn_patterns.ga_len; idx++)
            {
                int lc_col = matchcol;

                spp = syn_items[idx];
                if (spp.sp_type != SPTYPE_END)  /* past last END pattern */
                    break;
                lc_col -= spp.sp_offsets[SPO_LC_OFF];
                if (lc_col < 0)
                    lc_col = 0;

                regmatch.rmm_ic = spp.sp_ic;
                regmatch.regprog = spp.sp_prog;
                boolean r = syn_regexec(regmatch, startpos.lnum, lc_col);
                spp.sp_prog = regmatch.regprog;
                if (r)
                {
                    if (best_idx == -1 || regmatch.startpos[0].col < best_regmatch.startpos[0].col)
                    {
                        best_idx = idx;
                        COPY_lpos(best_regmatch.startpos[0], regmatch.startpos[0]);
                        COPY_lpos(best_regmatch.endpos[0], regmatch.endpos[0]);
                    }
                }
            }

            /*
             * If all end patterns have been tried, and there is no match,
             * the item continues until end-of-line.
             */
            if (best_idx == -1)
                break;

            /*
             * If the skip pattern matches before the end pattern,
             * continue searching after the skip pattern.
             */
            if (spp_skip != null)
            {
                int lc_col = matchcol - spp_skip.sp_offsets[SPO_LC_OFF];
                if (lc_col < 0)
                    lc_col = 0;

                regmatch.rmm_ic = spp_skip.sp_ic;
                regmatch.regprog = spp_skip.sp_prog;
                boolean r = syn_regexec(regmatch, startpos.lnum, lc_col);
                spp_skip.sp_prog = regmatch.regprog;
                if (r && regmatch.startpos[0].col <= best_regmatch.startpos[0].col)
                {
                    /* Add offset to skip pattern match. */
                    lpos_C pos = new lpos_C();
                    syn_add_end_off(pos, regmatch, spp_skip, SPO_ME_OFF, 1);

                    /* If the skip pattern goes on to the next line,
                     * there is no match with an end pattern in this line. */
                    if (startpos.lnum < pos.lnum)
                        break;

                    Bytes line = ml_get_buf(syn_buf, startpos.lnum, false);

                    /* take care of an empty match or negative offset */
                    if (pos.col <= matchcol)
                        matchcol++;
                    else if (pos.col <= regmatch.endpos[0].col)
                        matchcol = pos.col;
                    else
                        /* Be careful not to jump over the NUL at the end-of-line. */
                        for (matchcol = regmatch.endpos[0].col; line.at(matchcol) != NUL && matchcol < pos.col; )
                            matchcol++;

                    /* if the skip pattern includes end-of-line, break here */
                    if (line.at(matchcol) == NUL)
                        break;

                    continue;           /* start with first end pattern again */
                }
            }

            /*
             * Match from start pattern to end pattern.
             * Correct for match and highlight offset of end pattern.
             */
            spp = syn_items[best_idx];

            syn_add_end_off(m_endpos, best_regmatch, spp, SPO_ME_OFF, 1);
            /* can't end before the start */
            if (m_endpos.lnum == startpos.lnum && m_endpos.col < startpos.col)
                m_endpos.col = startpos.col;

            syn_add_end_off(end_endpos, best_regmatch, spp, SPO_HE_OFF, 1);
            /* can't end before the start */
            if (end_endpos.lnum == startpos.lnum && end_endpos.col < startpos.col)
                end_endpos.col = startpos.col;
            /* can't end after the match */
            limit_pos(end_endpos, m_endpos);

            /*
             * If the end group is highlighted differently, adjust the pointers.
             */
            if (spp.sp_syn_match_id != spp.sp_syn.id && spp.sp_syn_match_id != 0)
            {
                end_idx[0] = best_idx;
                if ((spp.sp_off_flags & (1 << (SPO_RE_OFF + SPO_COUNT))) != 0)
                {
                    hl_endpos.lnum = best_regmatch.endpos[0].lnum;
                    hl_endpos.col = best_regmatch.endpos[0].col;
                }
                else
                {
                    hl_endpos.lnum = best_regmatch.startpos[0].lnum;
                    hl_endpos.col = best_regmatch.startpos[0].col;
                }
                hl_endpos.col += spp.sp_offsets[SPO_RE_OFF];

                /* can't end before the start */
                if (hl_endpos.lnum == startpos.lnum && hl_endpos.col < startpos.col)
                    hl_endpos.col = startpos.col;
                limit_pos(hl_endpos, m_endpos);

                /* now the match ends where the highlighting ends,
                 * it is turned into the matchgroup for the end */
                COPY_lpos(m_endpos, hl_endpos);
            }
            else
            {
                end_idx[0] = 0;
                COPY_lpos(hl_endpos, end_endpos);
            }

            flagsp[0] = spp.sp_flags;

            had_match = true;
            break;
        }

        /* no match for an END pattern in this line */
        if (!had_match)
            m_endpos.lnum = 0;

        /* Remove external matches. */
        re_extmatch_in = null;
    }

    /*
     * Limit "pos" not to be after "limit".
     */
    /*private*/ static void limit_pos(lpos_C pos, lpos_C limit)
    {
        if (limit.lnum < pos.lnum)
            COPY_lpos(pos, limit);
        else if (pos.lnum == limit.lnum && limit.col < pos.col)
            pos.col = limit.col;
    }

    /*
     * Limit "pos" not to be after "limit", unless pos.lnum is zero.
     */
    /*private*/ static void limit_pos_zero(lpos_C pos, lpos_C limit)
    {
        if (pos.lnum == 0)
            COPY_lpos(pos, limit);
        else
            limit_pos(pos, limit);
    }

    /*
     * Add offset to matched text for end of match or highlight.
     */
    /*private*/ static void syn_add_end_off(lpos_C result, regmmatch_C regmatch, synpat_C spp, int idx, int extra)
        /* result: returned position */
        /* regmatch: start/end of match */
        /* spp: matched pattern */
        /* idx: index of offset */
        /* extra: extra chars for offset to start */
    {
        int col;
        int off;

        if ((spp.sp_off_flags & (1 << idx)) != 0)
        {
            result.lnum = regmatch.startpos[0].lnum;
            col = regmatch.startpos[0].col;
            off = spp.sp_offsets[idx] + extra;
        }
        else
        {
            result.lnum = regmatch.endpos[0].lnum;
            col = regmatch.endpos[0].col;
            off = spp.sp_offsets[idx];
        }

        /* Don't go past the end of the line.  Matters for "rs=e+2" when there
         * is a matchgroup.  Watch out for match with last NL in the buffer. */
        if (syn_buf.b_ml.ml_line_count < result.lnum)
            col = 0;
        else if (off != 0)
        {
            Bytes base = ml_get_buf(syn_buf, result.lnum, false);
            Bytes p = base.plus(col);
            if (0 < off)
            {
                while (0 < off-- && p.at(0) != NUL)
                    p = p.plus(us_ptr2len_cc(p));
            }
            else if (off < 0)
            {
                while (off++ < 0 && BLT(base, p))
                    p = p.minus(us_ptr_back(base, p));
            }
            col = BDIFF(p, base);
        }

        result.col = col;
    }

    /*
     * Add offset to matched text for start of match or highlight.
     * Avoid resulting column to become negative.
     */
    /*private*/ static void syn_add_start_off(lpos_C result, regmmatch_C regmatch, synpat_C spp, int idx, int extra)
        /* result: returned position */
        /* regmatch: start/end of match */
        /* extra: extra chars for offset to end */
    {
        int col;
        int off;

        if ((spp.sp_off_flags & (1 << (idx + SPO_COUNT))) != 0)
        {
            result.lnum = regmatch.endpos[0].lnum;
            col = regmatch.endpos[0].col;
            off = spp.sp_offsets[idx] + extra;
        }
        else
        {
            result.lnum = regmatch.startpos[0].lnum;
            col = regmatch.startpos[0].col;
            off = spp.sp_offsets[idx];
        }

        if (syn_buf.b_ml.ml_line_count < result.lnum)
        {
            /* a "\n" at the end of the pattern may take us below the last line */
            result.lnum = syn_buf.b_ml.ml_line_count;
            col = strlen(ml_get_buf(syn_buf, result.lnum, false));
        }

        if (off != 0)
        {
            Bytes base = ml_get_buf(syn_buf, result.lnum, false);
            Bytes p = base.plus(col);
            if (0 < off)
            {
                while (0 < off-- && p.at(0) != NUL)
                    p = p.plus(us_ptr2len_cc(p));
            }
            else if (off < 0)
            {
                while (off++ < 0 && BLT(base, p))
                    p = p.minus(us_ptr_back(base, p));
            }
            col = BDIFF(p, base);
        }

        result.col = col;
    }

    /*
     * Get current line in syntax buffer.
     */
    /*private*/ static Bytes syn_getcurline()
    {
        return ml_get_buf(syn_buf, current_lnum, false);
    }

    /*
     * Call vim_regexec() to find a match with "rmp" in "syn_buf".
     * Returns true when there is a match.
     */
    /*private*/ static boolean syn_regexec(regmmatch_C rmp, long lnum, int col)
    {
        rmp.rmm_maxcol = (int)syn_buf.b_p_smc[0];

        if (0 < vim_regexec_multi(rmp, syn_win, syn_buf, lnum, col, null))
        {
            rmp.startpos[0].lnum += lnum;
            rmp.endpos[0].lnum += lnum;
            return true;
        }

        return false;
    }

    /*
     * Check one position in a line for a matching keyword.
     * The caller must check if a keyword can start at startcol.
     * Return it's ID if found, 0 otherwise.
     */
    /*private*/ static int check_keyword_id(Bytes line, int startcol, int[] endcolp, int[] flagsp, short[][] next_listp, int cur_i, stateitem_C cur_si, int[] ccharp)
        /* startcol: position in line to check for keyword */
        /* endcolp: return: character after found keyword */
        /* flagsp: return: flags of matching keyword */
        /* next_listp: return: next_list of matching keyword */
        /* cur_si: item at the top of the stack */
        /* ccharp: conceal substitution char */
    {
        /* Find first character after the keyword.  First character was already checked. */
        Bytes kwp = line.plus(startcol);
        int kwlen = 0;
        do
        {
            kwlen += us_ptr2len_cc(kwp.plus(kwlen));
        } while (us_iswordp(kwp.plus(kwlen), syn_buf));

        if (MAXKEYWLEN < kwlen)
            return 0;

        Bytes keyword = new Bytes(MAXKEYWLEN + 1);  /* assume max. keyword len is 80 */
        /*
         * Must make a copy of the keyword, so we can add a NUL and make it lowercase.
         */
        vim_strncpy(keyword, kwp, kwlen);

        /*
         * Try twice:
         * 1. matching case
         * 2. ignoring case
         */
        for (int round = 1; round <= 2; round++)
        {
            hashtab_C ht = (round == 1) ? syn_block.b_keywtab : syn_block.b_keywtab_ic;
            if (ht.ht_used == 0)
                continue;
            if (round == 2) /* ignore case */
                str_foldcase(kwp, kwlen, keyword, MAXKEYWLEN + 1);

            /*
             * Find keywords that match.  There can be several with different attributes.
             * When current_next_list is non-zero accept only that group, otherwise:
             *  - Accept a not-contained keyword at toplevel.
             *  - Accept a keyword at other levels only if it is in the contains list.
             */
            hashitem_C hi = hash_find(ht, keyword);
            if (!hashitem_empty(hi))
                for (keyentry_C kp = (keyentry_C)hi.hi_data; kp != null; kp = kp.ke_next)
                {
                    if (current_next_list != null
                        ? in_id_list(-1, null, current_next_list, kp.ke_syn, false)
                        : (cur_si == null
                            ? (kp.ke_flags & HL_CONTAINED) == 0
                            : in_id_list(cur_i, cur_si, cur_si.si_cont_list, kp.ke_syn, (kp.ke_flags & HL_CONTAINED) != 0)))
                    {
                        endcolp[0] = startcol + kwlen;
                        flagsp[0] = kp.ke_flags;
                        next_listp[0] = kp.ke_next_list;
                        ccharp[0] = kp.ke_char;
                        return kp.ke_syn.id;
                    }
                }
        }

        return 0;
    }

    /*
     * Handle ":syntax conceal" command.
     */
    /*private*/ static void syn_cmd_conceal(exarg_C eap, boolean _syncing)
    {
        Bytes arg = eap.arg;

        eap.nextcmd = find_nextcmd(arg);
        if (eap.skip)
            return;

        Bytes next = skiptowhite(arg);
        if (STRNCASECMP(arg, u8("on"), 2) == 0 && BDIFF(next, arg) == 2)
            curwin.w_s.b_syn_conceal = true;
        else if (STRNCASECMP(arg, u8("off"), 3) == 0 && BDIFF(next, arg) == 3)
            curwin.w_s.b_syn_conceal = false;
        else
            emsg2(u8("E390: Illegal argument: %s"), arg);
    }

    /*
     * Handle ":syntax case" command.
     */
    /*private*/ static void syn_cmd_case(exarg_C eap, boolean _syncing)
    {
        Bytes arg = eap.arg;

        eap.nextcmd = find_nextcmd(arg);
        if (eap.skip)
            return;

        Bytes next = skiptowhite(arg);
        if (STRNCASECMP(arg, u8("match"), 5) == 0 && BDIFF(next, arg) == 5)
            curwin.w_s.b_syn_ic = false;
        else if (STRNCASECMP(arg, u8("ignore"), 6) == 0 && BDIFF(next, arg) == 6)
            curwin.w_s.b_syn_ic = true;
        else
            emsg2(u8("E390: Illegal argument: %s"), arg);
    }

    /*
     * Clear all syntax info for one buffer.
     */
    /*private*/ static void syntax_clear(synblock_C block)
    {
        block.b_syn_error = false;      /* clear previous error */
        block.b_syn_ic = false;         /* use case, by default */
        block.b_syn_containedin = false;

        /* free the keywords */
        clear_keywtab(block.b_keywtab);
        clear_keywtab(block.b_keywtab_ic);

        /* free the syntax patterns */
        for (int i = block.b_syn_patterns.ga_len; 0 <= --i; )
            syn_clear_pattern(block, i);
        block.b_syn_patterns.ga_clear();

        /* free the syntax clusters */
        for (int i = block.b_syn_clusters.ga_len; 0 <= --i; )
            syn_clear_cluster(block, i);
        block.b_syn_clusters.ga_clear();
        block.b_spell_cluster_id = 0;
        block.b_nospell_cluster_id = 0;

        block.b_syn_sync_flags = 0;
        block.b_syn_sync_minlines = 0;
        block.b_syn_sync_maxlines = 0;
        block.b_syn_sync_linebreaks = 0;

        block.b_syn_linecont_prog = null;
        block.b_syn_linecont_pat = null;

        /* free the stored states */
        syn_stack_free_all(block);
        invalidate_current_state();

        /* Reset the counter for ":syn include". */
        running_syn_inc_tag = 0;
    }

    /*
     * Get rid of ownsyntax for window "wp".
     */
    /*private*/ static void reset_synblock(window_C wp)
    {
        if (wp.w_s != wp.w_buffer.b_s)
        {
            syntax_clear(wp.w_s);
            wp.w_s = wp.w_buffer.b_s;
        }
    }

    /*
     * Clear syncing info for one buffer.
     */
    /*private*/ static void syntax_sync_clear()
    {
        /* free the syntax patterns */
        for (int i = curwin.w_s.b_syn_patterns.ga_len; 0 <= --i; )
        {
            synpat_C sp = curwin.w_s.b_syn_patterns.ga_data[i];
            if (sp.sp_syncing)
                syn_remove_pattern(curwin.w_s, i);
        }

        curwin.w_s.b_syn_sync_flags = 0;
        curwin.w_s.b_syn_sync_minlines = 0;
        curwin.w_s.b_syn_sync_maxlines = 0;
        curwin.w_s.b_syn_sync_linebreaks = 0;

        curwin.w_s.b_syn_linecont_prog = null;
        curwin.w_s.b_syn_linecont_pat = null;

        syn_stack_free_all(curwin.w_s);     /* Need to recompute all syntax. */
    }

    /*
     * Remove one pattern from the buffer's pattern list.
     */
    /*private*/ static void syn_remove_pattern(synblock_C block, int i)
    {
        syn_clear_pattern(block, i);

        synpat_C[] spp = block.b_syn_patterns.ga_data;

        for (--block.b_syn_patterns.ga_len; i < block.b_syn_patterns.ga_len; i++)
            spp[i] = spp[i + 1];

        spp[block.b_syn_patterns.ga_len] = null;
    }

    /*
     * Clear and free one syntax pattern.  When clearing all, must be called from last to first!
     */
    /*private*/ static void syn_clear_pattern(synblock_C block, int i)
    {
        synpat_C[] spp = block.b_syn_patterns.ga_data;

        spp[i].sp_pattern = null;
        spp[i].sp_prog = null;
        /* Only free sp_cont_list and sp_next_list of first start pattern. */
        if (i == 0 || spp[i - 1].sp_type != SPTYPE_START)
        {
            spp[i].sp_cont_list = null;
            spp[i].sp_next_list = null;
            spp[i].sp_syn.cont_in_list = null;
        }
    }

    /*
     * Clear and free one syntax cluster.
     */
    /*private*/ static void syn_clear_cluster(synblock_C block, int i)
    {
        syn_cluster_C[] syn_clstr = block.b_syn_clusters.ga_data;

        syn_clstr[i].scl_name = null;
        syn_clstr[i].scl_name_u = null;
        syn_clstr[i].scl_list = null;
    }

    /*
     * Handle ":syntax clear" command.
     */
    /*private*/ static void syn_cmd_clear(exarg_C eap, boolean syncing)
    {
        Bytes arg = eap.arg;

        eap.nextcmd = find_nextcmd(arg);
        if (eap.skip)
            return;

        /*
         * We have to disable this within ":syn include @group filename",
         * because otherwise @group would get deleted.
         * Only required for Vim 5.x syntax files, 6.0 ones don't contain ":syn clear".
         */
        if (curwin.w_s.b_syn_topgrp != 0)
            return;

        if (ends_excmd(arg.at(0)))
        {
            /*
             * No argument: Clear all syntax items.
             */
            if (syncing)
                syntax_sync_clear();
            else
            {
                syntax_clear(curwin.w_s);
                if (curwin.w_s == curwin.w_buffer.b_s)
                    do_unlet(u8("b:current_syntax"), true);
                do_unlet(u8("w:current_syntax"), true);
            }
        }
        else
        {
            /*
             * Clear the group IDs that are in the argument.
             */
            while (!ends_excmd(arg.at(0)))
            {
                Bytes arg_end = skiptowhite(arg);
                if (arg.at(0) == (byte)'@')
                {
                    int id = syn_scl_namen2id(arg.plus(1), BDIFF(arg_end, arg.plus(1)));
                    if (id == 0)
                    {
                        emsg2(u8("E391: No such syntax cluster: %s"), arg);
                        break;
                    }
                    else
                    {
                        /*
                         * We can't physically delete a cluster without changing the IDs of
                         * other clusters, so we do the next best thing and make it empty.
                         */
                        int scl_id = id - SYNID_CLUSTER;

                        syn_cluster_C[] syn_clstr = curwin.w_s.b_syn_clusters.ga_data;
                        syn_clstr[scl_id].scl_list = null;
                    }
                }
                else
                {
                    int id = syn_namen2id(arg, BDIFF(arg_end, arg));
                    if (id == 0)
                    {
                        emsg2(e_nogroup, arg);
                        break;
                    }
                    else
                        syn_clear_one(id, syncing);
                }
                arg = skipwhite(arg_end);
            }
        }
        redraw_curbuf_later(SOME_VALID);
        syn_stack_free_all(curwin.w_s);     /* Need to recompute all syntax. */
    }

    /*
     * Clear one syntax group for the current buffer.
     */
    /*private*/ static void syn_clear_one(int id, boolean syncing)
    {
        /* Clear keywords only when not ":syn sync clear group-name". */
        if (!syncing)
        {
            syn_clear_keyword(id, curwin.w_s.b_keywtab);
            syn_clear_keyword(id, curwin.w_s.b_keywtab_ic);
        }

        /* clear the patterns for "id" */
        for (int i = curwin.w_s.b_syn_patterns.ga_len; 0 <= --i; )
        {
            synpat_C sp = curwin.w_s.b_syn_patterns.ga_data[i];

            if (sp.sp_syn.id == id && sp.sp_syncing == syncing)
                syn_remove_pattern(curwin.w_s, i);
        }
    }

    /*
     * Handle ":syntax on" command.
     */
    /*private*/ static void syn_cmd_on(exarg_C eap, boolean _syncing)
    {
        syn_cmd_onoff(eap, u8("syntax"));
    }

    /*
     * Handle ":syntax enable" command.
     */
    /*private*/ static void syn_cmd_enable(exarg_C eap, boolean _syncing)
    {
        set_internal_string_var(u8("syntax_cmd"), u8("enable"));
        syn_cmd_onoff(eap, u8("syntax"));
        do_unlet(u8("g:syntax_cmd"), true);
    }

    /*
     * Handle ":syntax reset" command.
     */
    /*private*/ static void syn_cmd_reset(exarg_C eap, boolean _syncing)
    {
        eap.nextcmd = check_nextcmd(eap.arg);
        if (!eap.skip)
        {
            set_internal_string_var(u8("syntax_cmd"), u8("reset"));
            do_cmdline_cmd(u8("runtime! syntax/syncolor.vim"));
            do_unlet(u8("g:syntax_cmd"), true);
        }
    }

    /*
     * Handle ":syntax manual" command.
     */
    /*private*/ static void syn_cmd_manual(exarg_C eap, boolean _syncing)
    {
        syn_cmd_onoff(eap, u8("manual"));
    }

    /*
     * Handle ":syntax off" command.
     */
    /*private*/ static void syn_cmd_off(exarg_C eap, boolean _syncing)
    {
        syn_cmd_onoff(eap, u8("nosyntax"));
    }

    /*private*/ static void syn_cmd_onoff(exarg_C eap, Bytes name)
    {
        Bytes buf = new Bytes(100);

        eap.nextcmd = check_nextcmd(eap.arg);
        if (!eap.skip)
        {
            STRCPY(buf, u8("so "));
            vim_snprintf(buf.plus(3), buf.size() - 3, u8("%s/syntax/%s.vim"), VIMRUNTIME, name);
            do_cmdline_cmd(buf);
        }
    }

    /*
     * Handle ":syntax [list]" command: list current syntax words.
     */
    /*private*/ static void syn_cmd_list(exarg_C eap, boolean syncing)
        /* syncing: when true: list syncing items */
    {
        Bytes arg = eap.arg;

        eap.nextcmd = find_nextcmd(arg);
        if (eap.skip)
            return;

        if (!syntax_present(curwin))
        {
            msg(u8("No Syntax items defined for this buffer"));
            return;
        }

        if (syncing)
        {
            if ((curwin.w_s.b_syn_sync_flags & SF_CCOMMENT) != 0)
            {
                msg_puts(u8("syncing on C-style comments"));
                syn_lines_msg();
                syn_match_msg();
                return;
            }
            else if ((curwin.w_s.b_syn_sync_flags & SF_MATCH) == 0)
            {
                if (curwin.w_s.b_syn_sync_minlines == 0)
                    msg_puts(u8("no syncing"));
                else
                {
                    msg_puts(u8("syncing starts "));
                    msg_outnum(curwin.w_s.b_syn_sync_minlines);
                    msg_puts(u8(" lines before top line"));
                    syn_match_msg();
                }
                return;
            }
            msg_puts_title(u8("\n--- Syntax sync items ---"));
            if (0 < curwin.w_s.b_syn_sync_minlines
                    || 0 < curwin.w_s.b_syn_sync_maxlines
                    || 0 < curwin.w_s.b_syn_sync_linebreaks)
            {
                msg_puts(u8("\nsyncing on items"));
                syn_lines_msg();
                syn_match_msg();
            }
        }
        else
            msg_puts_title(u8("\n--- Syntax items ---"));
        if (ends_excmd(arg.at(0)))
        {
            /*
             * No argument: List all group IDs and all syntax clusters.
             */
            for (int id = 1; id <= highlight_ga.ga_len && !got_int; id++)
                syn_list_one(id, syncing, false);
            for (int id = 0; id < curwin.w_s.b_syn_clusters.ga_len && !got_int; id++)
                syn_list_cluster(id);
        }
        else
        {
            /*
             * List the group IDs and syntax clusters that are in the argument.
             */
            while (!ends_excmd(arg.at(0)) && !got_int)
            {
                Bytes arg_end = skiptowhite(arg);
                if (arg.at(0) == (byte)'@')
                {
                    int id = syn_scl_namen2id(arg.plus(1), BDIFF(arg_end, arg.plus(1)));
                    if (id == 0)
                        emsg2(u8("E392: No such syntax cluster: %s"), arg);
                    else
                        syn_list_cluster(id - SYNID_CLUSTER);
                }
                else
                {
                    int id = syn_namen2id(arg, BDIFF(arg_end, arg));
                    if (id == 0)
                        emsg2(e_nogroup, arg);
                    else
                        syn_list_one(id, syncing, true);
                }
                arg = skipwhite(arg_end);
            }
        }
        eap.nextcmd = check_nextcmd(arg);
    }

    /*private*/ static void syn_lines_msg()
    {
        if (0 < curwin.w_s.b_syn_sync_maxlines || 0 < curwin.w_s.b_syn_sync_minlines)
        {
            msg_puts(u8("; "));
            if (0 < curwin.w_s.b_syn_sync_minlines)
            {
                msg_puts(u8("minimal "));
                msg_outnum(curwin.w_s.b_syn_sync_minlines);
                if (0 < curwin.w_s.b_syn_sync_maxlines)
                    msg_puts(u8(", "));
            }
            if (0 < curwin.w_s.b_syn_sync_maxlines)
            {
                msg_puts(u8("maximal "));
                msg_outnum(curwin.w_s.b_syn_sync_maxlines);
            }
            msg_puts(u8(" lines before top line"));
        }
    }

    /*private*/ static void syn_match_msg()
    {
        if (0 < curwin.w_s.b_syn_sync_linebreaks)
        {
            msg_puts(u8("; match "));
            msg_outnum(curwin.w_s.b_syn_sync_linebreaks);
            msg_puts(u8(" line breaks"));
        }
    }

    /*private*/ static int last_matchgroup;

    /*private*/ static final class name_list_C
    {
        int     flag;
        Bytes name;

        /*private*/ name_list_C(int flag, Bytes name)
        {
            this.flag = flag;
            this.name = name;
        }
    }

    /*private*/ static name_list_C[] namelist1 = new name_list_C[]
    {
        new name_list_C(HL_DISPLAY,     u8("display")    ),
        new name_list_C(HL_CONTAINED,   u8("contained")  ),
        new name_list_C(HL_ONELINE,     u8("oneline")    ),
        new name_list_C(HL_KEEPEND,     u8("keepend")    ),
        new name_list_C(HL_EXTEND,      u8("extend")     ),
        new name_list_C(HL_EXCLUDENL,   u8("excludenl")  ),
        new name_list_C(HL_TRANSP,      u8("transparent")),
        new name_list_C(HL_FOLD,        u8("fold")       ),
        new name_list_C(HL_CONCEAL,     u8("conceal")    ),
        new name_list_C(HL_CONCEALENDS, u8("concealends")),
    };

    /*private*/ static name_list_C[] namelist2 = new name_list_C[]
    {
        new name_list_C(HL_SKIPWHITE,   u8("skipwhite")  ),
        new name_list_C(HL_SKIPNL,      u8("skipnl")     ),
        new name_list_C(HL_SKIPEMPTY,   u8("skipempty")  ),
    };

    /*private*/ static void syn_list_flags(name_list_C[] nlist, int flags, int attr)
    {
        for (int i = 0; i < nlist.length; i++)
            if ((flags & nlist[i].flag) != 0)
            {
                msg_puts_attr(nlist[i].name, attr);
                msg_putchar(' ');
            }
    }

    /*
     * List one syntax item, for ":syntax" or "syntax list syntax_name".
     */
    /*private*/ static void syn_list_one(int id, boolean syncing, boolean link_only)
        /* syncing: when true: list syncing items */
        /* link_only: when true; list link-only too */
    {
        boolean did_header = false;

        int attr = hl_attr(HLF_D);                  /* highlight like directories */

        /* list the keywords for "id" */
        if (!syncing)
        {
            did_header = syn_list_keywords(id, curwin.w_s.b_keywtab, false, attr);
            did_header = syn_list_keywords(id, curwin.w_s.b_keywtab_ic, did_header, attr);
        }

        hl_group_C[] hlt = highlight_ga.ga_data;

        /* list the patterns for "id" */
        for (int idx = 0; idx < curwin.w_s.b_syn_patterns.ga_len && !got_int; idx++)
        {
            synpat_C[] syn_items = curwin.w_s.b_syn_patterns.ga_data;
            synpat_C spp = syn_items[idx];

            if (spp.sp_syn.id != id || spp.sp_syncing != syncing)
                continue;

            syn_list_header(did_header, 999, id);
            did_header = true;
            last_matchgroup = 0;
            if (spp.sp_type == SPTYPE_MATCH)
            {
                put_pattern(u8("match"), ' ', spp, attr);
                msg_putchar(' ');
            }
            else if (spp.sp_type == SPTYPE_START)
            {
                while (syn_items[idx].sp_type == SPTYPE_START)
                    put_pattern(u8("start"), '=', syn_items[idx++], attr);
                if (syn_items[idx].sp_type == SPTYPE_SKIP)
                    put_pattern(u8("skip"), '=', syn_items[idx++], attr);
                while (idx < curwin.w_s.b_syn_patterns.ga_len && syn_items[idx].sp_type == SPTYPE_END)
                    put_pattern(u8("end"), '=', syn_items[idx++], attr);
                --idx;
                msg_putchar(' ');
            }
            syn_list_flags(namelist1, spp.sp_flags, attr);

            if (spp.sp_cont_list != null)
                put_id_list(u8("contains"), spp.sp_cont_list, attr);

            if (spp.sp_syn.cont_in_list != null)
                put_id_list(u8("containedin"), spp.sp_syn.cont_in_list, attr);

            if (spp.sp_next_list != null)
            {
                put_id_list(u8("nextgroup"), spp.sp_next_list, attr);
                syn_list_flags(namelist2, spp.sp_flags, attr);
            }
            if ((spp.sp_flags & (HL_SYNC_HERE|HL_SYNC_THERE)) != 0)
            {
                if ((spp.sp_flags & HL_SYNC_HERE) != 0)
                    msg_puts_attr(u8("grouphere"), attr);
                else
                    msg_puts_attr(u8("groupthere"), attr);
                msg_putchar(' ');
                if (0 <= spp.sp_sync_idx)
                    msg_outtrans(hlt[syn_items[spp.sp_sync_idx].sp_syn.id - 1].sg_name);
                else
                    msg_puts(u8("NONE"));
                msg_putchar(' ');
            }
        }

        /* list the link, if there is one */
        if (hlt[id - 1].sg_link != 0 && (did_header || link_only) && !got_int)
        {
            syn_list_header(did_header, 999, id);
            msg_puts_attr(u8("links to"), attr);
            msg_putchar(' ');
            msg_outtrans(hlt[hlt[id - 1].sg_link - 1].sg_name);
        }
    }

    /*
     * List one syntax cluster, for ":syntax" or "syntax list syntax_name".
     */
    /*private*/ static void syn_list_cluster(int id)
    {
        syn_cluster_C[] syn_clstr = curwin.w_s.b_syn_clusters.ga_data;

        int endcol = 15;

        /* slight hack:  roughly duplicate the guts of syn_list_header() */
        msg_putchar('\n');
        msg_outtrans(syn_clstr[id].scl_name);

        if (endcol <= msg_col)      /* output at least one space */
            endcol = msg_col + 1;
        if ((int)Columns[0] <= endcol)      /* avoid hang for tiny window */
            endcol = (int)Columns[0] - 1;

        msg_advance(endcol);
        if (syn_clstr[id].scl_list != null)
        {
            put_id_list(u8("cluster"), syn_clstr[id].scl_list, hl_attr(HLF_D));
        }
        else
        {
            msg_puts_attr(u8("cluster"), hl_attr(HLF_D));
            msg_puts(u8("=NONE"));
        }
    }

    /*private*/ static void put_id_list(Bytes name, short[] list, int attr)
    {
        hl_group_C[] hlt = highlight_ga.ga_data;

        msg_puts_attr(name, attr);
        msg_putchar('=');
        for (int i = 0; list[i] != 0; i++)
        {
            if (SYNID_ALLBUT <= list[i] && list[i] < SYNID_TOP)
            {
                if (list[i + 1] != 0)
                    msg_puts(u8("ALLBUT"));
                else
                    msg_puts(u8("ALL"));
            }
            else if (SYNID_TOP <= list[i] && list[i] < SYNID_CONTAINED)
            {
                msg_puts(u8("TOP"));
            }
            else if (SYNID_CONTAINED <= list[i] && list[i] < SYNID_CLUSTER)
            {
                msg_puts(u8("CONTAINED"));
            }
            else if (SYNID_CLUSTER <= list[i])
            {
                syn_cluster_C[] syn_clstr = curwin.w_s.b_syn_clusters.ga_data;
                int scl_id = list[i] - SYNID_CLUSTER;

                msg_putchar('@');
                msg_outtrans(syn_clstr[scl_id].scl_name);
            }
            else
                msg_outtrans(hlt[list[i] - 1].sg_name);
            if (list[i + 1] != 0)
                msg_putchar(',');
        }
        msg_putchar(' ');
    }

    /*private*/ static Bytes sepchars = u8("/+=-#@\"|'^&");

    /*private*/ static void put_pattern(Bytes s, int c, synpat_C spp, int attr)
    {
        hl_group_C[] hlt = highlight_ga.ga_data;

        /* May have to write "matchgroup=group". */
        if (last_matchgroup != spp.sp_syn_match_id)
        {
            last_matchgroup = spp.sp_syn_match_id;
            msg_puts_attr(u8("matchgroup"), attr);
            msg_putchar('=');
            if (last_matchgroup == 0)
                msg_outtrans(u8("NONE"));
            else
                msg_outtrans(hlt[last_matchgroup - 1].sg_name);
            msg_putchar(' ');
        }

        /* output the name of the pattern and an '=' or ' ' */
        msg_puts_attr(s, attr);
        msg_putchar(c);

        int i;

        /* output the pattern, in between a char that is not in the pattern */
        for (i = 0; vim_strchr(spp.sp_pattern, sepchars.at(i)) != null; )
            if (sepchars.at(++i) == NUL)
            {
                i = 0;      /* no good char found, just use the first one */
                break;
            }
        msg_putchar(sepchars.at(i));
        msg_outtrans(spp.sp_pattern);
        msg_putchar(sepchars.at(i));

        /* output any pattern options */
        boolean first = true;
        for (i = 0; i < SPO_COUNT; i++)
        {
            int mask = (1 << i);
            if ((spp.sp_off_flags & (mask + (mask << SPO_COUNT))) != 0)
            {
                if (!first)
                    msg_putchar(',');       /* separate with commas */
                msg_puts(spo_name_tab[i]);
                long n = spp.sp_offsets[i];
                if (i != SPO_LC_OFF)
                {
                    if ((spp.sp_off_flags & mask) != 0)
                        msg_putchar('s');
                    else
                        msg_putchar('e');
                    if (0 < n)
                        msg_putchar('+');
                }
                if (n != 0 || i == SPO_LC_OFF)
                    msg_outnum(n);
                first = false;
            }
        }
        msg_putchar(' ');
    }

    /*
     * List or clear the keywords for one syntax group.
     * Return true if the header has been printed.
     */
    /*private*/ static boolean syn_list_keywords(int id, hashtab_C ht, boolean did_header, int attr)
        /* did_header: header has already been printed */
    {
        int prev_contained = 0;
        short[] prev_next_list = null;
        short[] prev_cont_in_list = null;
        int prev_skipnl = 0;
        int prev_skipwhite = 0;
        int prev_skipempty = 0;

        /*
         * Unfortunately, this list of keywords is not sorted on alphabet but on hash value...
         */
        for (int i = 0, todo = (int)ht.ht_used; 0 < todo && !got_int; i++)
        {
            hashitem_C hi = ht.ht_buckets[i];
            if (!hashitem_empty(hi))
            {
                for (keyentry_C kp = (keyentry_C)hi.hi_data; kp != null && !got_int; kp = kp.ke_next)
                {
                    if (kp.ke_syn.id == id)
                    {
                        int outlen;
                        if (prev_contained != (kp.ke_flags & HL_CONTAINED)
                                || prev_skipnl != (kp.ke_flags & HL_SKIPNL)
                                || prev_skipwhite != (kp.ke_flags & HL_SKIPWHITE)
                                || prev_skipempty != (kp.ke_flags & HL_SKIPEMPTY)
                                || prev_cont_in_list != kp.ke_syn.cont_in_list
                                || prev_next_list != kp.ke_next_list)
                            outlen = 9999;
                        else
                            outlen = strlen(kp.ke_keyword);
                        /* output "contained" and "nextgroup" on each line */
                        if (syn_list_header(did_header, outlen, id))
                        {
                            prev_contained = 0;
                            prev_next_list = null;
                            prev_cont_in_list = null;
                            prev_skipnl = 0;
                            prev_skipwhite = 0;
                            prev_skipempty = 0;
                        }
                        did_header = true;
                        if (prev_contained != (kp.ke_flags & HL_CONTAINED))
                        {
                            msg_puts_attr(u8("contained"), attr);
                            msg_putchar(' ');
                            prev_contained = (kp.ke_flags & HL_CONTAINED);
                        }
                        if (kp.ke_syn.cont_in_list != prev_cont_in_list)
                        {
                            put_id_list(u8("containedin"), kp.ke_syn.cont_in_list, attr);
                            msg_putchar(' ');
                            prev_cont_in_list = kp.ke_syn.cont_in_list;
                        }
                        if (kp.ke_next_list != prev_next_list)
                        {
                            put_id_list(u8("nextgroup"), kp.ke_next_list, attr);
                            msg_putchar(' ');
                            prev_next_list = kp.ke_next_list;
                            if ((kp.ke_flags & HL_SKIPNL) != 0)
                            {
                                msg_puts_attr(u8("skipnl"), attr);
                                msg_putchar(' ');
                                prev_skipnl = (kp.ke_flags & HL_SKIPNL);
                            }
                            if ((kp.ke_flags & HL_SKIPWHITE) != 0)
                            {
                                msg_puts_attr(u8("skipwhite"), attr);
                                msg_putchar(' ');
                                prev_skipwhite = (kp.ke_flags & HL_SKIPWHITE);
                            }
                            if ((kp.ke_flags & HL_SKIPEMPTY) != 0)
                            {
                                msg_puts_attr(u8("skipempty"), attr);
                                msg_putchar(' ');
                                prev_skipempty = (kp.ke_flags & HL_SKIPEMPTY);
                            }
                        }
                        msg_outtrans(kp.ke_keyword);
                    }
                }
                --todo;
            }
        }

        return did_header;
    }

    /*private*/ static void syn_clear_keyword(int id, hashtab_C ht)
    {
        hash_lock(ht);

        for (int i = 0, todo = (int)ht.ht_used; 0 < todo; i++)
        {
            hashitem_C hi = ht.ht_buckets[i];
            if (!hashitem_empty(hi))
            {
                for (keyentry_C kp = (keyentry_C)hi.hi_data, kp_prev = null; kp != null; )
                {
                    if (kp.ke_syn.id == id)
                    {
                        keyentry_C kp_next = kp.ke_next;
                        if (kp_prev == null)
                        {
                            if (kp_next == null)
                                hash_remove(ht, hi);
                            else
                            {
                                hi.hi_data = kp_next;
                                hi.hi_key = kp_next.ke_keyword;
                            }
                        }
                        else
                            kp_prev.ke_next = kp_next;
                        kp.ke_keyword = null;
                        kp.ke_next_list = null;
                        kp.ke_syn.cont_in_list = null;
                        kp = kp_next;
                    }
                    else
                    {
                        kp_prev = kp;
                        kp = kp.ke_next;
                    }
                }
                --todo;
            }
        }

        hash_unlock(ht);
    }

    /*
     * Clear a whole keyword table.
     */
    /*private*/ static void clear_keywtab(hashtab_C ht)
    {
        for (int i = 0, todo = (int)ht.ht_used; 0 < todo; i++)
        {
            hashitem_C hi = ht.ht_buckets[i];
            if (!hashitem_empty(hi))
            {
                for (keyentry_C kp = (keyentry_C)hi.hi_data, kp_next; kp != null; kp = kp_next)
                {
                    kp_next = kp.ke_next;
                    kp.ke_keyword = null;
                    kp.ke_next_list = null;
                    kp.ke_syn.cont_in_list = null;
                }
                --todo;
            }
        }
        hash_clear(ht);
        hash_init(ht);
    }

    /*
     * Add a keyword to the list of keywords.
     */
    /*private*/ static void add_keyword(Bytes name, int id, int flags, short[] cont_in_list, short[] next_list, int conceal_char)
        /* name: name of keyword */
        /* id: group ID for this keyword */
        /* flags: flags for this keyword */
        /* cont_in_list: containedin for this keyword */
        /* next_list: nextgroup for this keyword */
    {
        Bytes name_folded = new Bytes(MAXKEYWLEN + 1);

        Bytes name_ic;
        if (curwin.w_s.b_syn_ic)
            name_ic = str_foldcase(name, strlen(name), name_folded, MAXKEYWLEN + 1);
        else
            name_ic = name;

        keyentry_C kp = new keyentry_C();

        kp.ke_keyword = STRDUP(name_ic);
        kp.ke_syn.id = (short)id;
        kp.ke_syn.inc_tag = current_syn_inc_tag;
        kp.ke_flags = flags;
        kp.ke_char = conceal_char;
        kp.ke_syn.cont_in_list = copy_id_list(cont_in_list);
        if (cont_in_list != null)
            curwin.w_s.b_syn_containedin = true;
        kp.ke_next_list = copy_id_list(next_list);

        hashtab_C ht;
        if (curwin.w_s.b_syn_ic)
            ht = curwin.w_s.b_keywtab_ic;
        else
            ht = curwin.w_s.b_keywtab;

        long hash = hash_hash(kp.ke_keyword);
        hashitem_C hi = hash_lookup(ht, kp.ke_keyword, hash);
        if (hashitem_empty(hi))
        {
            /* new keyword, add to hashtable */
            kp.ke_next = null;
            hash_add_item(ht, hi, kp, kp.ke_keyword, hash);
        }
        else
        {
            /* keyword already exists, prepend to list */
            kp.ke_next = (keyentry_C)hi.hi_data;
            hi.hi_data = kp;
            hi.hi_key = kp.ke_keyword;
        }
    }

    /*
     * Get the start and end of the group name argument.
     * Return a pointer to the first argument.
     * Return null if the end of the command was found instead of further args.
     */
    /*private*/ static Bytes get_group_name(Bytes arg, Bytes[] name_end)
        /* arg: start of the argument */
        /* name_end: pointer to end of the name */
    {
        name_end[0] = skiptowhite(arg);
        Bytes rest = skipwhite(name_end[0]);

        /*
         * Check if there are enough arguments.  The first argument may be a
         * pattern, where '|' is allowed, so only check for NUL.
         */
        if (ends_excmd(arg.at(0)) || rest.at(0) == NUL)
            return null;

        return rest;
    }

    /*private*/ static final class flag_C
    {
        Bytes name;
        int     argtype;
        int     flags;

        /*private*/ flag_C(Bytes name, int argtype, int flags)
        {
            this.name = name;
            this.argtype = argtype;
            this.flags = flags;
        }
    }

    /*private*/ static flag_C[] flagtab = new flag_C[]
    {
        new flag_C(u8("cCoOnNtTaAiInNeEdD"),      0, HL_CONTAINED  ),
        new flag_C(u8("oOnNeElLiInNeE"),          0, HL_ONELINE    ),
        new flag_C(u8("kKeEeEpPeEnNdD"),          0, HL_KEEPEND    ),
        new flag_C(u8("eExXtTeEnNdD"),            0, HL_EXTEND     ),
        new flag_C(u8("eExXcClLuUdDeEnNlL"),      0, HL_EXCLUDENL  ),
        new flag_C(u8("tTrRaAnNsSpPaArReEnNtT"),  0, HL_TRANSP     ),
        new flag_C(u8("sSkKiIpPnNlL"),            0, HL_SKIPNL     ),
        new flag_C(u8("sSkKiIpPwWhHiItTeE"),      0, HL_SKIPWHITE  ),
        new flag_C(u8("sSkKiIpPeEmMpPtTyY"),      0, HL_SKIPEMPTY  ),
        new flag_C(u8("gGrRoOuUpPhHeErReE"),      0, HL_SYNC_HERE  ),
        new flag_C(u8("gGrRoOuUpPtThHeErReE"),    0, HL_SYNC_THERE ),
        new flag_C(u8("dDiIsSpPlLaAyY"),          0, HL_DISPLAY    ),
        new flag_C(u8("fFoOlLdD"),                0, HL_FOLD       ),
        new flag_C(u8("cCoOnNcCeEaAlL"),          0, HL_CONCEAL    ),
        new flag_C(u8("cCoOnNcCeEaAlLeEnNdDsS"),  0, HL_CONCEALENDS),
        new flag_C(u8("cCcChHaArR"),             11, 0             ),
        new flag_C(u8("cCoOnNtTaAiInNsS"),        1, 0             ),
        new flag_C(u8("cCoOnNtTaAiInNeEdDiInN"),  2, 0             ),
        new flag_C(u8("nNeExXtTgGrRoOuUpP"),      3, 0             ),
    };

    /*private*/ static Bytes first_letters = u8("cCoOkKeEtTsSgGdDfFnN");

    /*
     * Check for syntax command option arguments.
     * This can be called at any place in the list of arguments, and just picks
     * out the arguments that are known.  Can be called several times in a row
     * to collect all options in between other arguments.
     * Return a pointer to the next argument (which isn't an option).
     * Return null for any error.
     */
    /*private*/ static Bytes get_syn_options(Bytes _arg, syn_opt_arg_C opt, int[] conceal_char)
        /* arg: next argument to be checked */
        /* opt: various things */
    {
        Bytes[] arg = { _arg };
        if (arg[0] == null)            /* already detected error */
            return null;

        if (curwin.w_s.b_syn_conceal)
            opt.flags |= HL_CONCEAL;

        for ( ; ; )
        {
            int len = 0;	// %% anno dunno
            /*
             * This is used very often when a large number of keywords is defined.
             * Need to skip quickly when no option name is found.
             */
            if (STRCHR(first_letters, arg[0].at(0)) == null)
                break;

            int fidx;
            for (fidx = flagtab.length; 0 <= --fidx; )
            {
                Bytes p = flagtab[fidx].name;
                int i;
                for (i = 0, len = 0; p.at(i) != NUL; i += 2, ++len)
                    if (arg[0].at(len) != p.at(i) && arg[0].at(len) != p.at(i + 1))
                        break;
                if (p.at(i) == NUL && (vim_iswhite(arg[0].at(len))
                            || (0 < flagtab[fidx].argtype ? (arg[0].at(len) == (byte)'=') : ends_excmd(arg[0].at(len)))))
                {
                    if (opt.keyword
                            && (flagtab[fidx].flags == HL_DISPLAY
                                || flagtab[fidx].flags == HL_FOLD
                                || flagtab[fidx].flags == HL_EXTEND))
                        /* treat "display", "fold" and "extend" as a keyword */
                        fidx = -1;
                    break;
                }
            }
            if (fidx < 0)       /* no match found */
                break;

            if (flagtab[fidx].argtype == 1)
            {
                if (!opt.has_cont_list)
                {
                    emsg(u8("E395: contains argument not accepted here"));
                    return null;
                }
                boolean b;
                { short[][] __ = { opt.cont_list }; b = get_id_list(arg, 8, __); opt.cont_list = __[0]; }
                if (b == false)
                    return null;
            }
            else if (flagtab[fidx].argtype == 2)
            {
                boolean b;
                { short[][] __ = { opt.cont_in_list }; b = get_id_list(arg, 11, __); opt.cont_in_list = __[0]; }
                if (b == false)
                    return null;
            }
            else if (flagtab[fidx].argtype == 3)
            {
                boolean b;
                { short[][] __ = { opt.next_list }; b = get_id_list(arg, 9, __); opt.next_list = __[0]; }
                if (b == false)
                    return null;
            }
            else if (flagtab[fidx].argtype == 11 && arg[0].at(5) == (byte)'=')
            {
                /* cchar=? */
                conceal_char[0] = us_ptr2char(arg[0].plus(6));
                arg[0] = arg[0].plus(us_ptr2len_cc(arg[0].plus(6)) - 1);
                if (!vim_isprintc(conceal_char[0]))
                {
                    emsg(u8("E844: invalid cchar value"));
                    return null;
                }
                arg[0] = skipwhite(arg[0].plus(7));
            }
            else
            {
                opt.flags |= flagtab[fidx].flags;
                arg[0] = skipwhite(arg[0].plus(len));

                if (flagtab[fidx].flags == HL_SYNC_HERE || flagtab[fidx].flags == HL_SYNC_THERE)
                {
                    if (opt.sync_idx == null)
                    {
                        emsg(u8("E393: group[t]here not accepted here"));
                        return null;
                    }
                    Bytes gname_start = arg[0];
                    arg[0] = skiptowhite(arg[0]);
                    if (BEQ(gname_start, arg[0]))
                        return null;

                    Bytes gname = STRNDUP(gname_start, BDIFF(arg[0], gname_start));

                    if (STRCMP(gname, u8("NONE")) == 0)
                        opt.sync_idx[0] = NONE_IDX;
                    else
                    {
                        int syn_id = syn_name2id(gname);
                        int i;
                        for (i = curwin.w_s.b_syn_patterns.ga_len; 0 <= --i; )
                        {
                            synpat_C sp = curwin.w_s.b_syn_patterns.ga_data[i];

                            if (sp.sp_syn.id == syn_id && sp.sp_type == SPTYPE_START)
                            {
                                opt.sync_idx[0] = i;
                                break;
                            }
                        }
                        if (i < 0)
                        {
                            emsg2(u8("E394: Didn't find region item for %s"), gname);
                            return null;
                        }
                    }

                    arg[0] = skipwhite(arg[0]);
                }
            }
        }

        return arg[0];
    }

    /*
     * Adjustments to syntax item when declared in a ":syn include"'d file.
     * Set the contained flag, and if the item is not already contained, add it
     * to the specified top-level group, if any.
     */
    /*private*/ static void syn_incl_toplevel(int id, int[] flagsp)
    {
        if ((flagsp[0] & HL_CONTAINED) != 0 || curwin.w_s.b_syn_topgrp == 0)
            return;

        flagsp[0] |= HL_CONTAINED;

        if (SYNID_CLUSTER <= curwin.w_s.b_syn_topgrp)
        {
            /* We have to alloc this, because syn_combine_list() will free it. */
            short[][] grp_list = new short[1][2];
            int tlg_id = curwin.w_s.b_syn_topgrp - SYNID_CLUSTER;

            if (grp_list[0] != null)
            {
                grp_list[0][0] = (short)id;
                grp_list[0][1] = 0;
                syn_cluster_C[] syn_clstr = curwin.w_s.b_syn_clusters.ga_data;
                { short[][] __ = { syn_clstr[tlg_id].scl_list }; syn_combine_list(__, grp_list, CLUSTER_ADD); syn_clstr[tlg_id].scl_list = __[0]; }
            }
        }
    }

    /*
     * Handle ":syntax include [@{group-name}] filename" command.
     */
    /*private*/ static void syn_cmd_include(exarg_C eap, boolean _syncing)
    {
        Bytes arg = eap.arg;

        eap.nextcmd = find_nextcmd(arg);
        if (eap.skip)
            return;

        int sgl_id = 1;
        if (arg.at(0) == (byte)'@')
        {
            arg = arg.plus(1);
            Bytes[] group_name_end = new Bytes[1];
            Bytes rest = get_group_name(arg, group_name_end);
            if (rest == null)
            {
                emsg(u8("E397: Filename required"));
                return;
            }
            sgl_id = syn_check_cluster(arg, BDIFF(group_name_end[0], arg));
            if (sgl_id == 0)
                return;
            /* separate_nextcmd() depend on this */
            eap.arg = rest;
        }

        boolean source = false;
        /*
         * Everything that's left, up to the next command, should be the filename to include.
         */
        eap.argt |= (XFILE | NOSPC);
        separate_nextcmd(eap);
        if (eap.arg.at(0) == (byte)'<' || eap.arg.at(0) == (byte)'$' || mch_isFullName(eap.arg))
        {
            /* For an absolute path or "<sfile>.." we ":source" the file.
             * In other cases ":runtime!" is used.
             */
            source = true;
        }

        /*
         * Save and restore the existing top-level grouplist id and
         * ":syn include" tag around the actual inclusion.
         */
        if (MAX_SYN_INC_TAG <= running_syn_inc_tag)
        {
            emsg(u8("E847: Too many syntax includes"));
            return;
        }

        int prev_syn_inc_tag = current_syn_inc_tag;
        current_syn_inc_tag = ++running_syn_inc_tag;
        int prev_toplvl_grp = curwin.w_s.b_syn_topgrp;
        curwin.w_s.b_syn_topgrp = sgl_id;
        if (source ? do_source(eap.arg, false) == false : source_runtime(eap.arg, true) == false)
            emsg2(e_notopen, eap.arg);
        curwin.w_s.b_syn_topgrp = prev_toplvl_grp;
        current_syn_inc_tag = prev_syn_inc_tag;
    }

    /*
     * Handle ":syntax keyword {group-name} [{option}] keyword .." command.
     */
    /*private*/ static void syn_cmd_keyword(exarg_C eap, boolean _syncing)
    {
        Bytes arg = eap.arg;
        int[] conceal_char = { NUL };

        Bytes[] group_name_end = new Bytes[1];
        Bytes rest = get_group_name(arg, group_name_end);

        if (rest != null)
        {
            int syn_id = syn_check_group(arg, BDIFF(group_name_end[0], arg));
            if (syn_id != 0)
            {
                /* allocate a buffer, for removing backslashes in the keyword */
                Bytes keyword_copy = new Bytes(strlen(rest) + 1);

                syn_opt_arg_C syn_opt_arg = new syn_opt_arg_C();
                syn_opt_arg.flags = 0;
                syn_opt_arg.keyword = true;
                syn_opt_arg.sync_idx = null;
                syn_opt_arg.has_cont_list = false;
                syn_opt_arg.cont_in_list = null;
                syn_opt_arg.next_list = null;

                /*
                 * The options given apply to ALL keywords,
                 * so all options must be found before keywords can be created.
                 *
                 * 1: collect the options and copy the keywords to "keyword_copy".
                 */
                int cnt = 0;
                Bytes p = keyword_copy;
                for ( ; rest != null && !ends_excmd(rest.at(0)); rest = skipwhite(rest))
                {
                    rest = get_syn_options(rest, syn_opt_arg, conceal_char);
                    if (rest == null || ends_excmd(rest.at(0)))
                        break;
                    /* Copy the keyword, removing backslashes, and add a NUL. */
                    while (rest.at(0) != NUL && !vim_iswhite(rest.at(0)))
                    {
                        if (rest.at(0) == (byte)'\\' && rest.at(1) != NUL)
                            rest = rest.plus(1);
                        (p = p.plus(1)).be(-1, (rest = rest.plus(1)).at(-1));
                    }
                    (p = p.plus(1)).be(-1, NUL);
                    cnt++;
                }

                if (!eap.skip)
                {
                    /* Adjust flags for use of ":syn include". */
                    { int[] __ = { syn_opt_arg.flags }; syn_incl_toplevel(syn_id, __); syn_opt_arg.flags = __[0]; }

                    /*
                     * 2: Add an entry for each keyword.
                     */
                    for (Bytes kw = keyword_copy; 0 <= --cnt; kw = kw.plus(strlen(kw) + 1))
                    {
                        for (p = vim_strchr(kw, '['); ; )
                        {
                            if (p != null)
                                p.be(0, NUL);
                            add_keyword(kw, syn_id,
                                syn_opt_arg.flags, syn_opt_arg.cont_in_list, syn_opt_arg.next_list, conceal_char[0]);
                            if (p == null)
                                break;
                            if (p.at(1) == NUL)
                            {
                                emsg2(u8("E789: Missing ']': %s"), kw);
                                kw = p.plus(2);         /* skip over the NUL */
                                break;
                            }
                            if (p.at(1) == (byte)']')
                            {
                                kw = p.plus(1);         /* skip over the "]" */
                                break;
                            }

                            int l = us_ptr2len_cc(p.plus(1));
                            BCOPY(p, 0, p, 1, l);
                            p = p.plus(l);
                        }
                    }
                }
            }
        }

        if (rest != null)
            eap.nextcmd = check_nextcmd(rest);
        else
            emsg2(e_invarg2, arg);

        redraw_curbuf_later(SOME_VALID);
        syn_stack_free_all(curwin.w_s);     /* Need to recompute all syntax. */
    }

    /*
     * Handle ":syntax match {name} [{options}] {pattern} [{options}]".
     *
     * Also ":syntax sync match {name} [[grouphere | groupthere] {group-name}] .."
     */
    /*private*/ static void syn_cmd_match(exarg_C eap, boolean syncing)
        /* syncing: true for ":syntax sync match .. " */
    {
        Bytes arg = eap.arg;
        int[] sync_idx = { 0 };
        int[] conceal_char = { NUL };

        /* Isolate the group name, check for validity. */
        Bytes[] group_name_end = new Bytes[1];
        Bytes rest = get_group_name(arg, group_name_end);

        /* Get options before the pattern. */
        syn_opt_arg_C syn_opt_arg = new syn_opt_arg_C();
        syn_opt_arg.flags = 0;
        syn_opt_arg.keyword = false;
        syn_opt_arg.sync_idx = syncing ? sync_idx : null;
        syn_opt_arg.has_cont_list = true;
        syn_opt_arg.cont_list = null;
        syn_opt_arg.cont_in_list = null;
        syn_opt_arg.next_list = null;
        rest = get_syn_options(rest, syn_opt_arg, conceal_char);

        synpat_C item = new synpat_C();     /* the item found in the line */
        rest = get_syn_pattern(rest, item);

        if (vim_regcomp_had_eol() && (syn_opt_arg.flags & HL_EXCLUDENL) == 0)
            syn_opt_arg.flags |= HL_HAS_EOL;

        /* Get options after the pattern. */
        rest = get_syn_options(rest, syn_opt_arg, conceal_char);

        if (rest != null)                   /* all arguments are valid */
        {
            /*
             * Check for trailing command and illegal trailing arguments.
             */
            eap.nextcmd = check_nextcmd(rest);
            if (!ends_excmd(rest.at(0)) || eap.skip)
                rest = null;
            else
            {
                int syn_id = syn_check_group(arg, BDIFF(group_name_end[0], arg));
                if (syn_id != 0)
                {
                    { int[] __ = { syn_opt_arg.flags }; syn_incl_toplevel(syn_id, __); syn_opt_arg.flags = __[0]; }

                    /*
                     * Store the pattern in the syn_items list.
                     */
                    synpat_C[] spp = curwin.w_s.b_syn_patterns.ga_grow(1);
                    synpat_C sp = spp[curwin.w_s.b_syn_patterns.ga_len++] = new synpat_C();

                    COPY_synpat(sp, item);
                    sp.sp_syncing = syncing;
                    sp.sp_type = SPTYPE_MATCH;
                    sp.sp_syn.id = (short)syn_id;
                    sp.sp_syn.inc_tag = current_syn_inc_tag;
                    sp.sp_flags = syn_opt_arg.flags;
                    sp.sp_sync_idx = sync_idx[0];
                    sp.sp_cont_list = syn_opt_arg.cont_list;
                    sp.sp_syn.cont_in_list = syn_opt_arg.cont_in_list;
                    sp.sp_cchar = conceal_char[0];
                    if (syn_opt_arg.cont_in_list != null)
                        curwin.w_s.b_syn_containedin = true;
                    sp.sp_next_list = syn_opt_arg.next_list;

                    /* remember that we found a match for syncing on */
                    if ((syn_opt_arg.flags & (HL_SYNC_HERE|HL_SYNC_THERE)) != 0)
                        curwin.w_s.b_syn_sync_flags |= SF_MATCH;

                    redraw_curbuf_later(SOME_VALID);
                    syn_stack_free_all(curwin.w_s);     /* Need to recompute all syntax. */

                    return;     /* don't free the progs and patterns now */
                }
            }
        }

        if (rest == null)
            emsg2(e_invarg2, arg);
    }

    /*private*/ static final class pat_ptr_C
    {
        synpat_C        pp_synp;                /* pointer to syn_pattern */
        int             pp_matchgroup_id;       /* matchgroup ID */
        pat_ptr_C       pp_next;                /* pointer to next pat_ptr */

        /*private*/ pat_ptr_C()
        {
        }
    }

    /*
     * Handle ":syntax region {group-name} [matchgroup={group-name}]
     *              start {start} .. [skip {skip}] end {end} .. [{options}]".
     */
    /*private*/ static void syn_cmd_region(exarg_C eap, boolean syncing)
        /* syncing: true for ":syntax sync region .." */
    {
        Bytes arg = eap.arg;
        Bytes key = null;
        int pat_count = 0;                  /* nr of syn_patterns found */
        int matchgroup_id = 0;
        boolean not_enough = false;         /* not enough arguments */
        boolean illegal = false;            /* illegal arguments */
        boolean success = false;
        int[] conceal_char = { NUL };

        /* Isolate the group name, check for validity. */
        Bytes[] group_name_end = new Bytes[1];
        Bytes rest = get_group_name(arg, group_name_end);

        pat_ptr_C[] pat_ptrs = new pat_ptr_C[3];    /* patterns found in the line */

        syn_opt_arg_C syn_opt_arg = new syn_opt_arg_C();
        syn_opt_arg.flags = 0;
        syn_opt_arg.keyword = false;
        syn_opt_arg.sync_idx = null;
        syn_opt_arg.has_cont_list = true;
        syn_opt_arg.cont_list = null;
        syn_opt_arg.cont_in_list = null;
        syn_opt_arg.next_list = null;

        final int
            ITEM_START = 0,
            ITEM_SKIP = 1,
            ITEM_END = 2,
            ITEM_MATCHGROUP = 3;

        /*
         * Get the options, patterns and matchgroup.
         */
        while (rest != null && !ends_excmd(rest.at(0)))
        {
            /* Check for option arguments. */
            rest = get_syn_options(rest, syn_opt_arg, conceal_char);
            if (rest == null || ends_excmd(rest.at(0)))
                break;

            /* must be a pattern or matchgroup then */
            Bytes key_end = rest;
            while (key_end.at(0) != NUL && !vim_iswhite(key_end.at(0)) && key_end.at(0) != (byte)'=')
                key_end = key_end.plus(1);
            key = vim_strnsave_up(rest, BDIFF(key_end, rest));

            int item;
            if (STRCMP(key, u8("MATCHGROUP")) == 0)
                item = ITEM_MATCHGROUP;
            else if (STRCMP(key, u8("START")) == 0)
                item = ITEM_START;
            else if (STRCMP(key, u8("END")) == 0)
                item = ITEM_END;
            else if (STRCMP(key, u8("SKIP")) == 0)
            {
                if (pat_ptrs[ITEM_SKIP] != null)    /* one skip pattern allowed */
                {
                    illegal = true;
                    break;
                }
                item = ITEM_SKIP;
            }
            else
                break;

            rest = skipwhite(key_end);
            if (rest.at(0) != (byte)'=')
            {
                rest = null;
                emsg2(u8("E398: Missing '=': %s"), arg);
                break;
            }
            rest = skipwhite(rest.plus(1));
            if (rest.at(0) == NUL)
            {
                not_enough = true;
                break;
            }

            if (item == ITEM_MATCHGROUP)
            {
                Bytes p = skiptowhite(rest);
                if ((BDIFF(p, rest) == 4 && STRNCMP(rest, u8("NONE"), 4) == 0) || eap.skip)
                    matchgroup_id = 0;
                else
                {
                    matchgroup_id = syn_check_group(rest, BDIFF(p, rest));
                    if (matchgroup_id == 0)
                    {
                        illegal = true;
                        break;
                    }
                }
                rest = skipwhite(p);
            }
            else
            {
                /*
                 * Allocate room for a syn_pattern,
                 * and link it in the list of syn_patterns for this item,
                 * at the start (because the list is used from end to start).
                 */
                pat_ptr_C ppp = new pat_ptr_C();
                ppp.pp_next = pat_ptrs[item];
                pat_ptrs[item] = ppp;
                ppp.pp_synp = new synpat_C();

                /*
                 * Get the syntax pattern and the following offset(s).
                 */
                /* Enable the appropriate \z specials. */
                if (item == ITEM_START)
                    reg_do_extmatch = REX_SET;
                else if (item == ITEM_SKIP || item == ITEM_END)
                    reg_do_extmatch = REX_USE;
                rest = get_syn_pattern(rest, ppp.pp_synp);
                reg_do_extmatch = 0;
                if (item == ITEM_END && vim_regcomp_had_eol() && (syn_opt_arg.flags & HL_EXCLUDENL) == 0)
                    ppp.pp_synp.sp_flags |= HL_HAS_EOL;
                ppp.pp_matchgroup_id = matchgroup_id;
                pat_count++;
            }
        }

        if (illegal || not_enough)
            rest = null;

        /*
         * Must have a "start" and "end" pattern.
         */
        if (rest != null && (pat_ptrs[ITEM_START] == null || pat_ptrs[ITEM_END] == null))
        {
            not_enough = true;
            rest = null;
        }

        if (rest != null)
        {
            /*
             * Check for trailing garbage or command.
             * If OK, add the item.
             */
            eap.nextcmd = check_nextcmd(rest);
            if (!ends_excmd(rest.at(0)) || eap.skip)
                rest = null;
            else
            {
                int syn_id = syn_check_group(arg, BDIFF(group_name_end[0], arg));
                if (syn_id != 0)
                {
                    { int[] __ = { syn_opt_arg.flags }; syn_incl_toplevel(syn_id, __); syn_opt_arg.flags = __[0]; }

                    /*
                     * Store the start/skip/end in the syn_items list.
                     */
                    curwin.w_s.b_syn_patterns.ga_grow(pat_count);

                    for (int item = ITEM_START; item <= ITEM_END; item++)
                    {
                        for (pat_ptr_C ppp = pat_ptrs[item]; ppp != null; ppp = ppp.pp_next)
                        {
                            synpat_C[] spp = curwin.w_s.b_syn_patterns.ga_data;
                            synpat_C sp = spp[curwin.w_s.b_syn_patterns.ga_len++] = new synpat_C();

                            COPY_synpat(sp, ppp.pp_synp);
                            sp.sp_syncing = syncing;
                            sp.sp_type = (item == ITEM_START) ? SPTYPE_START :
                                          (item == ITEM_SKIP) ? SPTYPE_SKIP : SPTYPE_END;
                            sp.sp_flags |= syn_opt_arg.flags;
                            sp.sp_syn.id = (short)syn_id;
                            sp.sp_syn.inc_tag = current_syn_inc_tag;
                            sp.sp_syn_match_id = (short)ppp.pp_matchgroup_id;
                            sp.sp_cchar = conceal_char[0];
                            if (item == ITEM_START)
                            {
                                sp.sp_cont_list = syn_opt_arg.cont_list;
                                sp.sp_syn.cont_in_list = syn_opt_arg.cont_in_list;
                                if (syn_opt_arg.cont_in_list != null)
                                    curwin.w_s.b_syn_containedin = true;
                                sp.sp_next_list = syn_opt_arg.next_list;
                            }
                        }
                    }

                    redraw_curbuf_later(SOME_VALID);
                    syn_stack_free_all(curwin.w_s);     /* need to recompute all syntax */
                    success = true;                     /* don't free the progs and patterns now */
                }
            }
        }

        if (!success)
        {
            if (not_enough)
                emsg2(u8("E399: Not enough arguments: syntax region %s"), arg);
            else if (illegal || rest == null)
                emsg2(e_invarg2, arg);
        }
    }

    /*
     * Combines lists of syntax clusters.
     * clstr1[0] and clstr2[0] must both be allocated memory; they will be consumed.
     */
    /*private*/ static void syn_combine_list(short[][] clstr1, short[][] clstr2, int list_op)
    {
        short[] clstr = null;

        /*
         * Handle degenerate cases.
         */
        if (clstr2[0] == null)
            return;
        if (clstr1[0] == null || list_op == CLUSTER_REPLACE)
        {
            if (list_op == CLUSTER_REPLACE)
                clstr1[0] = null;
            if (list_op == CLUSTER_REPLACE || list_op == CLUSTER_ADD)
                clstr1[0] = clstr2[0];
            else
                clstr2[0] = null;
            return;
        }

        int count1 = 0;
        while (clstr1[0][count1] != 0)
            count1++;
        int count2 = 0;
        while (clstr2[0][count2] != 0)
            count2++;

        /*
         * For speed purposes, sort both lists.
         */
        Arrays.sort(clstr1[0], 0, count1);
        Arrays.sort(clstr2[0], 0, count2);

        /*
         * We proceed in two passes; in round 1, we count the elements to place
         * in the new list, and in round 2, we allocate and populate the new
         * list.  For speed, we use a mergesort-like method, adding the smaller
         * of the current elements in each list to the new list.
         */
        for (int round = 1; round <= 2; round++)
        {
            short[] g1 = clstr1[0], g2 = clstr2[0];
            int i1 = 0, i2 = 0, count = 0;

            /*
             * First, loop through the lists until one of them is empty.
             */
            while (g1[i1] != 0 && g2[i2] != 0)
            {
                /*
                 * We always want to add from the first list.
                 */
                if (g1[i1] < g2[i2])
                {
                    if (round == 2)
                        clstr[count] = g1[i1];
                    count++;
                    i1++;
                    continue;
                }
                /*
                 * We only want to add from the second list if we're adding the lists.
                 */
                if (list_op == CLUSTER_ADD)
                {
                    if (round == 2)
                        clstr[count] = g2[i2];
                    count++;
                }
                if (g1[i1] == g2[i2])
                    i1++;
                i2++;
            }

            /*
             * Now add the leftovers from whichever list didn't get finished first.
             * As before, we only want to add from the second list if we're adding the lists.
             */
            for ( ; g1[i1] != 0; i1++, count++)
                if (round == 2)
                    clstr[count] = g1[i1];
            if (list_op == CLUSTER_ADD)
                for ( ; g2[i2] != 0; i2++, count++)
                    if (round == 2)
                        clstr[count] = g2[i2];

            if (round == 1)
            {
                /*
                 * If the group ended up empty, we don't need to allocate any space for it.
                 */
                if (count == 0)
                {
                    clstr = null;
                    break;
                }
                clstr = new short[count + 1];
                clstr[count] = 0;
            }
        }

        /*
         * Finally, put the new list in place.
         */
        clstr1[0] = null;
        clstr2[0] = null;
        clstr1[0] = clstr;
    }

    /*
     * Lookup a syntax cluster name and return it's ID.
     * If it is not found, 0 is returned.
     */
    /*private*/ static int syn_scl_name2id(Bytes name)
    {
        /* Avoid using stricmp() too much, it's slow on some systems. */
        Bytes name_u = vim_strsave_up(name);

        int i;
        for (i = curwin.w_s.b_syn_clusters.ga_len; 0 <= --i; )
        {
            syn_cluster_C scl = curwin.w_s.b_syn_clusters.ga_data[i];

            if (scl.scl_name_u != null && STRCMP(name_u, scl.scl_name_u) == 0)
                break;
        }
        return (i < 0) ? 0 : i + SYNID_CLUSTER;
    }

    /*
     * Like syn_scl_name2id(), but take a pointer + length argument.
     */
    /*private*/ static int syn_scl_namen2id(Bytes linep, int len)
    {
        Bytes name = STRNDUP(linep, len);
        return syn_scl_name2id(name);
    }

    /*
     * Find syntax cluster name in the table and return it's ID.
     * The argument is a pointer to the name and the length of the name.
     * If it doesn't exist yet, a new entry is created.
     * Return 0 for failure.
     */
    /*private*/ static int syn_check_cluster(Bytes pp, int len)
    {
        Bytes name = STRNDUP(pp, len);

        int id = syn_scl_name2id(name);
        if (id == 0)                        /* doesn't exist yet */
            id = syn_add_cluster(name);

        return id;
    }

    /*
     * Add new syntax cluster and return it's ID.
     * "name" must be an allocated string, it will be consumed.
     * Return 0 for failure.
     */
    /*private*/ static int syn_add_cluster(Bytes name)
    {
        int n = curwin.w_s.b_syn_clusters.ga_len;
        if (MAX_CLUSTER_ID <= n)
        {
            emsg(u8("E848: Too many syntax clusters"));
            return 0;
        }

        /*
         * Make room for at least one other cluster entry.
         */
        syn_cluster_C[] syn_clstr = curwin.w_s.b_syn_clusters.ga_grow(1);

        syn_clstr[n] = new syn_cluster_C();
        syn_clstr[n].scl_name = name;
        syn_clstr[n].scl_name_u = vim_strsave_up(name);
        syn_clstr[n].scl_list = null;

        curwin.w_s.b_syn_clusters.ga_len++;

        if (STRCASECMP(name, u8("Spell")) == 0)
            curwin.w_s.b_spell_cluster_id = n + SYNID_CLUSTER;
        if (STRCASECMP(name, u8("NoSpell")) == 0)
            curwin.w_s.b_nospell_cluster_id = n + SYNID_CLUSTER;

        return n + SYNID_CLUSTER;
    }

    /*
     * Handle ":syntax cluster {cluster-name} [contains={groupname},..]
     *              [add={groupname},..] [remove={groupname},..]".
     */
    /*private*/ static void syn_cmd_cluster(exarg_C eap, boolean _syncing)
    {
        Bytes arg = eap.arg;
        boolean got_clstr = false;

        eap.nextcmd = find_nextcmd(arg);
        if (eap.skip)
            return;

        Bytes[] group_name_end = new Bytes[1];
        Bytes[] rest = { get_group_name(arg, group_name_end) };

        if (rest[0] != null)
        {
            int scl_id = syn_check_cluster(arg, BDIFF(group_name_end[0], arg));
            if (scl_id == 0)
                return;
            scl_id -= SYNID_CLUSTER;

            for ( ; ; )
            {
                int opt_len;
                int list_op;

                if (STRNCASECMP(rest[0], u8("add"), 3) == 0 && (vim_iswhite(rest[0].at(3)) || rest[0].at(3) == (byte)'='))
                {
                    opt_len = 3;
                    list_op = CLUSTER_ADD;
                }
                else if (STRNCASECMP(rest[0], u8("remove"), 6) == 0 && (vim_iswhite(rest[0].at(6)) || rest[0].at(6) == (byte)'='))
                {
                    opt_len = 6;
                    list_op = CLUSTER_SUBTRACT;
                }
                else if (STRNCASECMP(rest[0], u8("contains"), 8) == 0 && (vim_iswhite(rest[0].at(8)) || rest[0].at(8) == (byte)'='))
                {
                    opt_len = 8;
                    list_op = CLUSTER_REPLACE;
                }
                else
                    break;

                short[][] clstr_list = { null };
                if (get_id_list(rest, opt_len, clstr_list) == false)
                {
                    emsg2(e_invarg2, rest[0]);
                    break;
                }
                syn_cluster_C[] syn_clstr = curwin.w_s.b_syn_clusters.ga_data;
                { short[][] __ = { syn_clstr[scl_id].scl_list }; syn_combine_list(__, clstr_list, list_op); syn_clstr[scl_id].scl_list = __[0]; }
                got_clstr = true;
            }

            if (got_clstr)
            {
                redraw_curbuf_later(SOME_VALID);
                syn_stack_free_all(curwin.w_s);     /* Need to recompute all. */
            }
        }

        if (!got_clstr)
            emsg(u8("E400: No cluster specified"));
        if (rest[0] == null || !ends_excmd(rest[0].at(0)))
            emsg2(e_invarg2, arg);
    }

    /*
     * Get one pattern for a ":syntax match" or ":syntax region" command.
     * Stores the pattern and program in a synpat_C.
     * Returns a pointer to the next argument, or null in case of an error.
     */
    /*private*/ static Bytes get_syn_pattern(Bytes arg, synpat_C ci)
    {
        /* need at least three chars */
        if (arg == null || arg.at(1) == NUL || arg.at(2) == NUL)
            return null;

        Bytes end = skip_regexp(arg.plus(1), arg.at(0), true, null);
        if (end.at(0) != arg.at(0))                       /* end delimiter not found */
        {
            emsg2(u8("E401: Pattern delimiter not found: %s"), arg);
            return null;
        }
        /* store the pattern and compiled regexp program */
        if ((ci.sp_pattern = STRNDUP(arg.plus(1), BDIFF(end, arg.plus(1)))) == null)
            return null;

        /* make 'cpoptions' empty, to avoid the 'l' flag */
        Bytes cpo_save = p_cpo[0];
        p_cpo[0] = u8("");
        ci.sp_prog = vim_regcomp(ci.sp_pattern, RE_MAGIC);
        p_cpo[0] = cpo_save;

        if (ci.sp_prog == null)
            return null;
        ci.sp_ic = curwin.w_s.b_syn_ic;

        /*
         * Check for a match, highlight or region offset.
         */
        end = end.plus(1);
        int idx;
        do
        {
            for (idx = SPO_COUNT; 0 <= --idx; )
                if (STRNCMP(end, spo_name_tab[idx], 3) == 0)
                    break;
            if (0 <= idx)
            {
                int pi = idx;
                if (idx != SPO_LC_OFF)
                    switch (end.at(3))
                    {
                        case 's':   break;
                        case 'b':   break;
                        case 'e':   idx += SPO_COUNT; break;
                        default:    idx = -1; break;
                    }
                if (0 <= idx)
                {
                    ci.sp_off_flags |= (1 << idx);
                    if (idx == SPO_LC_OFF)      /* lc=99 */
                    {
                        end = end.plus(3);
                        { Bytes[] __ = { end }; ci.sp_offsets[pi] = (int)getdigits(__); end = __[0]; }

                        /* "lc=" offset automatically sets "ms=" offset */
                        if ((ci.sp_off_flags & (1 << SPO_MS_OFF)) == 0)
                        {
                            ci.sp_off_flags |= (1 << SPO_MS_OFF);
                            ci.sp_offsets[SPO_MS_OFF] = ci.sp_offsets[pi];
                        }
                    }
                    else                        /* yy=x+99 */
                    {
                        end = end.plus(4);
                        if (end.at(0) == (byte)'+')
                        {
                            end = end.plus(1);
                            { Bytes[] __ = { end }; ci.sp_offsets[pi] = (int)getdigits(__); end = __[0]; } /* positive offset */
                        }
                        else if (end.at(0) == (byte)'-')
                        {
                            end = end.plus(1);
                            { Bytes[] __ = { end }; ci.sp_offsets[pi] = (int)-getdigits(__); end = __[0]; } /* negative offset */
                        }
                    }
                    if (end.at(0) != (byte)',')
                        break;
                    end = end.plus(1);
                }
            }
        } while (0 <= idx);

        if (!ends_excmd(end.at(0)) && !vim_iswhite(end.at(0)))
        {
            emsg2(u8("E402: Garbage after pattern: %s"), arg);
            return null;
        }
        return skipwhite(end);
    }

    /*
     * Handle ":syntax sync .." command.
     */
    /*private*/ static void syn_cmd_sync(exarg_C eap, boolean _syncing)
    {
        Bytes arg_start = eap.arg;
        Bytes key = null;
        boolean illegal = false;
        boolean finished = false;

        if (ends_excmd(arg_start.at(0)))
        {
            syn_cmd_list(eap, true);
            return;
        }

        while (!ends_excmd(arg_start.at(0)))
        {
            Bytes arg_end = skiptowhite(arg_start);
            Bytes next_arg = skipwhite(arg_end);
            key = vim_strnsave_up(arg_start, BDIFF(arg_end, arg_start));
            if (STRCMP(key, u8("CCOMMENT")) == 0)
            {
                if (!eap.skip)
                    curwin.w_s.b_syn_sync_flags |= SF_CCOMMENT;
                if (!ends_excmd(next_arg.at(0)))
                {
                    arg_end = skiptowhite(next_arg);
                    if (!eap.skip)
                        curwin.w_s.b_syn_sync_id = (short)syn_check_group(next_arg, BDIFF(arg_end, next_arg));
                    next_arg = skipwhite(arg_end);
                }
                else if (!eap.skip)
                    curwin.w_s.b_syn_sync_id = (short)syn_name2id(u8("Comment"));
            }
            else if (STRNCMP(key, u8("LINES"), 5) == 0
                  || STRNCMP(key, u8("MINLINES"), 8) == 0
                  || STRNCMP(key, u8("MAXLINES"), 8) == 0
                  || STRNCMP(key, u8("LINEBREAKS"), 10) == 0)
            {
                if (key.at(4) == (byte)'S')
                    arg_end = key.plus(6);
                else if (key.at(0) == (byte)'L')
                    arg_end = key.plus(11);
                else
                    arg_end = key.plus(9);
                if (arg_end.at(-1) != (byte)'=' || !asc_isdigit(arg_end.at(0)))
                {
                    illegal = true;
                    break;
                }
                long n;
                { Bytes[] __ = { arg_end }; n = getdigits(__); arg_end = __[0]; }
                if (!eap.skip)
                {
                    if (key.at(4) == (byte)'B')
                        curwin.w_s.b_syn_sync_linebreaks = n;
                    else if (key.at(1) == (byte)'A')
                        curwin.w_s.b_syn_sync_maxlines = n;
                    else
                        curwin.w_s.b_syn_sync_minlines = n;
                }
            }
            else if (STRCMP(key, u8("FROMSTART")) == 0)
            {
                if (!eap.skip)
                {
                    curwin.w_s.b_syn_sync_minlines = MAXLNUM;
                    curwin.w_s.b_syn_sync_maxlines = 0;
                }
            }
            else if (STRCMP(key, u8("LINECONT")) == 0)
            {
                if (curwin.w_s.b_syn_linecont_pat != null)
                {
                    emsg(u8("E403: syntax sync: line continuations pattern specified twice"));
                    finished = true;
                    break;
                }
                arg_end = skip_regexp(next_arg.plus(1), next_arg.at(0), true, null);
                if (arg_end.at(0) != next_arg.at(0))      /* end delimiter not found */
                {
                    illegal = true;
                    break;
                }

                if (!eap.skip)
                {
                    /* store the pattern and compiled regexp program */
                    if ((curwin.w_s.b_syn_linecont_pat = STRNDUP(next_arg.plus(1),
                                                                    BDIFF(arg_end, next_arg.plus(1)))) == null)
                    {
                        finished = true;
                        break;
                    }
                    curwin.w_s.b_syn_linecont_ic = curwin.w_s.b_syn_ic;

                    /* Make 'cpoptions' empty, to avoid the 'l' flag. */
                    Bytes cpo_save = p_cpo[0];
                    p_cpo[0] = u8("");
                    curwin.w_s.b_syn_linecont_prog = vim_regcomp(curwin.w_s.b_syn_linecont_pat, RE_MAGIC);
                    p_cpo[0] = cpo_save;

                    if (curwin.w_s.b_syn_linecont_prog == null)
                    {
                        curwin.w_s.b_syn_linecont_pat = null;
                        finished = true;
                        break;
                    }
                }
                next_arg = skipwhite(arg_end.plus(1));
            }
            else
            {
                eap.arg = next_arg;
                if (STRCMP(key, u8("MATCH")) == 0)
                    syn_cmd_match(eap, true);
                else if (STRCMP(key, u8("REGION")) == 0)
                    syn_cmd_region(eap, true);
                else if (STRCMP(key, u8("CLEAR")) == 0)
                    syn_cmd_clear(eap, true);
                else
                    illegal = true;
                finished = true;
                break;
            }
            arg_start = next_arg;
        }

        if (illegal)
            emsg2(u8("E404: Illegal arguments: %s"), arg_start);
        else if (!finished)
        {
            eap.nextcmd = check_nextcmd(arg_start);
            redraw_curbuf_later(SOME_VALID);
            syn_stack_free_all(curwin.w_s);     /* Need to recompute all syntax. */
        }
    }

    /*
     * Convert a line of highlight group names into a list of group ID numbers.
     * "arg" should point to the "contains" or "nextgroup" keyword.
     * "arg" is advanced to after the last group name.
     * Careful: the argument is modified (NULs added).
     * returns false for some error, true for success.
     */
    /*private*/ static boolean get_id_list(Bytes[] arg, int keylen, short[][] list)
        /* keylen: length of keyword */
        /* list: where to store the resulting list, if not null, the list is silently skipped! */
    {
        Bytes p = null;
        int total_count = 0;
        short[] retval = null;
        boolean failed = false;

        /*
         * We parse the list twice:
         * round == 1: count the number of items, allocate the array.
         * round == 2: fill the array with the items.
         * In round 1 new groups may be added, causing the number of items to
         * grow when a regexp is used.  In that case round 1 is done once again.
         */
        for (int round = 1; round <= 2; round++)
        {
            /*
             * skip "contains"
             */
            p = skipwhite(arg[0].plus(keylen));
            if (p.at(0) != (byte)'=')
            {
                emsg2(u8("E405: Missing equal sign: %s"), arg[0]);
                break;
            }
            p = skipwhite(p.plus(1));
            if (ends_excmd(p.at(0)))
            {
                emsg2(u8("E406: Empty argument: %s"), arg[0]);
                break;
            }

            /*
             * parse the arguments after "contains"
             */
            int count = 0;
            while (!ends_excmd(p.at(0)))
            {
                Bytes end;
                for (end = p; end.at(0) != NUL && !vim_iswhite(end.at(0)) && end.at(0) != (byte)','; end = end.plus(1))
                    ;
                Bytes name = new Bytes(BDIFF(end, p) + 3);    /* leave room for "^$" */
                vim_strncpy(name.plus(1), p, BDIFF(end, p));
                int id;
                if (   STRCMP(name.plus(1), u8("ALLBUT")) == 0
                    || STRCMP(name.plus(1), u8("ALL")) == 0
                    || STRCMP(name.plus(1), u8("TOP")) == 0
                    || STRCMP(name.plus(1), u8("CONTAINED")) == 0)
                {
                    if (asc_toupper(arg[0].at(0)) != 'C')
                    {
                        emsg2(u8("E407: %s not allowed here"), name.plus(1));
                        failed = true;
                        break;
                    }
                    if (count != 0)
                    {
                        emsg2(u8("E408: %s must be first in contains list"), name.plus(1));
                        failed = true;
                        break;
                    }
                    if (name.at(1) == (byte)'A')
                        id = SYNID_ALLBUT;
                    else if (name.at(1) == (byte)'T')
                        id = SYNID_TOP;
                    else
                        id = SYNID_CONTAINED;
                    id += current_syn_inc_tag;
                }
                else if (name.at(1) == (byte)'@')
                {
                    id = syn_check_cluster(name.plus(2), BDIFF(end, p.plus(1)));
                }
                else
                {
                    /*
                     * Handle full group name.
                     */
                    if (STRPBRK(name.plus(1), u8("\\.*^$~[")) == null)
                        id = syn_check_group(name.plus(1), BDIFF(end, p));
                    else
                    {
                        regmatch_C regmatch = new regmatch_C();
                        /*
                         * Handle match of regexp with group names.
                         */
                        name.be(0, (byte)'^');
                        STRCAT(name, u8("$"));
                        regmatch.regprog = vim_regcomp(name, RE_MAGIC);
                        if (regmatch.regprog == null)
                        {
                            failed = true;
                            break;
                        }

                        hl_group_C[] hlt = highlight_ga.ga_data;

                        regmatch.rm_ic = true;
                        id = 0;
                        for (int i = highlight_ga.ga_len; 0 <= --i; )
                        {
                            if (vim_regexec(regmatch, hlt[i].sg_name, 0))
                            {
                                if (round == 2)
                                {
                                    /* Got more items than expected;
                                     * can happen when adding items that match:
                                     * "contains=a.*b,axb".
                                     * Go back to first round */
                                    if (total_count <= count)
                                    {
                                        retval = null;
                                        round = 1;
                                    }
                                    else
                                        retval[count] = (short)(i + 1);
                                }
                                count++;
                                id = -1;        /* remember that we found one */
                            }
                        }
                    }
                }
                if (id == 0)
                {
                    emsg2(u8("E409: Unknown group name: %s"), p);
                    failed = true;
                    break;
                }
                if (0 < id)
                {
                    if (round == 2)
                    {
                        /* Got more items than expected, go back to first round. */
                        if (total_count <= count)
                        {
                            retval = null;
                            round = 1;
                        }
                        else
                            retval[count] = (short)id;
                    }
                    count++;
                }
                p = skipwhite(end);
                if (p.at(0) != (byte)',')
                    break;
                p = skipwhite(p.plus(1));       /* skip comma in between arguments */
            }
            if (failed)
                break;
            if (round == 1)
            {
                retval = new short[count + 1];
                retval[count] = 0;          /* zero means end of the list */
                total_count = count;
            }
        }

        arg[0] = p;
        if (failed || retval == null)
            return false;

        if (list[0] == null)
            list[0] = retval;

        return true;
    }

    /*
     * Make a copy of an ID list.
     */
    /*private*/ static short[] copy_id_list(short[] list)
    {
        if (list == null)
            return null;

        int n;
        for (n = 0; list[n] != 0; n++)
            ;

        short[] retval = new short[n + 1];
        ACOPY(retval, 0, list, 0, n + 1);

        return retval;
    }

    /*private*/ static int _2_depth;

    /*
     * Check if syntax group "ssp" is in the ID list "list" of "cur_si".
     * "cur_si" can be null if not checking the "containedin" list.
     * Used to check if a syntax item is in the "contains" or "nextgroup" list of the current item.
     * This function is called very often, keep it fast!!
     */
    /*private*/ static boolean in_id_list(int cur_i, stateitem_C cur_si, short[] list, sp_syn_C ssp, boolean contained)
        /* cur_si: current item or null */
        /* list: id list */
        /* ssp: group id and ":syn include" tag of group */
        /* contained: group id is contained */
    {
        short id = ssp.id;

        /* If ssp has a "containedin" list and "cur_si" is in it, return true. */
        if (cur_si != null && ssp.cont_in_list != null && (cur_si.si_flags & HL_MATCH) == 0)
        {
            /* Ignore transparent items without a contains argument.
             * Double check that we don't go back past the first one.
             */
            while ((cur_si.si_flags & HL_TRANS_CONT) != 0 && 0 < cur_i)
                cur_si = current_state.ga_data[--cur_i];

            /* cur_si.si_idx is -1 for keywords, these never contain anything. */
            synpat_C[] syn_items = syn_block.b_syn_patterns.ga_data;
            if (0 <= cur_si.si_idx && in_id_list(-1, null, ssp.cont_in_list,
                    syn_items[cur_si.si_idx].sp_syn,
                    (syn_items[cur_si.si_idx].sp_flags & HL_CONTAINED) != 0))
                return true;
        }

        if (list == null)
            return false;

        /*
         * If list is ID_LIST_ALL, we are in a transparent item that isn't inside anything.
         * Only allow not-contained groups.
         */
        if (list == ID_LIST_ALL)
            return !contained;

        boolean retval;
        /*
         * If the first item is "ALLBUT", return true if "id" is NOT in the contains list.
         * We also require that "id" is at the same ":syn include" level as the list.
         */
        int i = 0;
        short item = list[i];
        if (SYNID_ALLBUT <= item && item < SYNID_CLUSTER)
        {
            if (item < SYNID_TOP)
            {
                /* ALL or ALLBUT: accept all groups in the same file. */
                if (item - SYNID_ALLBUT != ssp.inc_tag)
                    return false;
            }
            else if (item < SYNID_CONTAINED)
            {
                /* TOP: accept all not-contained groups in the same file. */
                if (item - SYNID_TOP != ssp.inc_tag || contained)
                    return false;
            }
            else
            {
                /* CONTAINED: accept all contained groups in the same file. */
                if (item - SYNID_CONTAINED != ssp.inc_tag || !contained)
                    return false;
            }
            item = list[++i];
            retval = false;
        }
        else
            retval = true;

        /*
         * Return "retval" if id is in the contains list.
         */
        while (item != 0)
        {
            if (item == id)
                return retval;
            if (SYNID_CLUSTER <= item)
            {
                syn_cluster_C[] syn_clstr = syn_block.b_syn_clusters.ga_data;
                short[] scl_list = syn_clstr[item - SYNID_CLUSTER].scl_list;
                /* Restrict recursiveness to 30 to avoid an endless loop
                 * for a cluster that includes itself (indirectly). */
                if (scl_list != null && _2_depth < 30)
                {
                    _2_depth++;
                    boolean r = in_id_list(-1, null, scl_list, ssp, contained);
                    --_2_depth;
                    if (r)
                        return retval;
                }
            }
            item = list[++i];
        }

        return !retval;
    }

    /*private*/ static abstract class subcommand_C
    {
        Bytes name;

        protected subcommand_C(Bytes name)
        {
            this.name = name;
        }

        public abstract void func(exarg_C eap, boolean syncing);
    }

    /*private*/ static subcommand_C[] subcommands = new subcommand_C[]
    {
        new subcommand_C(u8("case"))    { public void func(exarg_C eap, boolean syncing) { syn_cmd_case(eap, syncing); }    },
        new subcommand_C(u8("clear"))   { public void func(exarg_C eap, boolean syncing) { syn_cmd_clear(eap, syncing); }   },
        new subcommand_C(u8("cluster")) { public void func(exarg_C eap, boolean syncing) { syn_cmd_cluster(eap, syncing); } },
        new subcommand_C(u8("conceal")) { public void func(exarg_C eap, boolean syncing) { syn_cmd_conceal(eap, syncing); } },
        new subcommand_C(u8("enable"))  { public void func(exarg_C eap, boolean syncing) { syn_cmd_enable(eap, syncing); }  },
        new subcommand_C(u8("include")) { public void func(exarg_C eap, boolean syncing) { syn_cmd_include(eap, syncing); } },
        new subcommand_C(u8("keyword")) { public void func(exarg_C eap, boolean syncing) { syn_cmd_keyword(eap, syncing); } },
        new subcommand_C(u8("list"))    { public void func(exarg_C eap, boolean syncing) { syn_cmd_list(eap, syncing); }    },
        new subcommand_C(u8("manual"))  { public void func(exarg_C eap, boolean syncing) { syn_cmd_manual(eap, syncing); }  },
        new subcommand_C(u8("match"))   { public void func(exarg_C eap, boolean syncing) { syn_cmd_match(eap, syncing); }   },
        new subcommand_C(u8("on"))      { public void func(exarg_C eap, boolean syncing) { syn_cmd_on(eap, syncing); }      },
        new subcommand_C(u8("off"))     { public void func(exarg_C eap, boolean syncing) { syn_cmd_off(eap, syncing); }     },
        new subcommand_C(u8("region"))  { public void func(exarg_C eap, boolean syncing) { syn_cmd_region(eap, syncing); }  },
        new subcommand_C(u8("reset"))   { public void func(exarg_C eap, boolean syncing) { syn_cmd_reset(eap, syncing); }   },
        new subcommand_C(u8("sync"))    { public void func(exarg_C eap, boolean syncing) { syn_cmd_sync(eap, syncing); }    },

        new subcommand_C(u8(""))        { public void func(exarg_C eap, boolean syncing) { syn_cmd_list(eap, syncing); }    },
    };

    /*
     * ":syntax".
     * This searches the subcommands[] table for the subcommand name,
     * and calls a syntax_subcommand() function to do the rest.
     */
    /*private*/ static final ex_func_C ex_syntax = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes arg = eap.arg;

            /* isolate subcommand name */
            Bytes subcmd_end;
            for (subcmd_end = arg; asc_isalpha(subcmd_end.at(0)); subcmd_end = subcmd_end.plus(1))
                ;
            Bytes subcmd_name = STRNDUP(arg, BDIFF(subcmd_end, arg));

            if (eap.skip)       /* skip error messages for all subcommands */
                emsg_skip++;

            for (int i = 0; ; i++)
            {
                if (subcommands.length <= i)
                {
                    emsg2(u8("E410: Invalid :syntax subcommand: %s"), subcmd_name);
                    break;
                }
                if (STRCMP(subcmd_name, subcommands[i].name) == 0)
                {
                    eap.arg = skipwhite(subcmd_end);
                    subcommands[i].func(eap, false);
                    break;
                }
            }

            if (eap.skip)
                --emsg_skip;
        }
    };

    /*private*/ static final ex_func_C ex_ownsyntax = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (curwin.w_s == curwin.w_buffer.b_s)
                curwin.w_s = new synblock_C();

            /* save value of b:current_syntax */
            Bytes old_value = get_var_value(u8("b:current_syntax"));
            if (old_value != null)
                old_value = STRDUP(old_value);

            /* Apply the "syntax" autocommand event, this finds and loads the syntax file. */
            apply_autocmds(EVENT_SYNTAX, eap.arg, curbuf.b_fname, true, curbuf);

            /* move value of b:current_syntax to w:current_syntax */
            Bytes new_value = get_var_value(u8("b:current_syntax"));
            if (new_value != null)
                set_internal_string_var(u8("w:current_syntax"), new_value);

            /* restore value of b:current_syntax */
            if (old_value == null)
                do_unlet(u8("b:current_syntax"), true);
            else
                set_internal_string_var(u8("b:current_syntax"), old_value);
        }
    };

    /*private*/ static boolean syntax_present(window_C win)
    {
        return (win.w_s.b_syn_patterns.ga_len != 0
             || win.w_s.b_syn_clusters.ga_len != 0
             || 0 < win.w_s.b_keywtab.ht_used
             || 0 < win.w_s.b_keywtab_ic.ht_used);
    }

    /*private*/ static final int
        EXP_SUBCMD = 0,     /* expand ":syn" sub-commands */
        EXP_CASE   = 1;     /* expand ":syn case" arguments */
    /*private*/ static int expand_what;

    /*
     * Reset include_link, include_default, include_none to 0.
     * Called when we are done expanding.
     */
    /*private*/ static void reset_expand_highlight()
    {
        include_link = include_default = include_none = 0;
    }

    /*
     * Handle command line completion for :match and :echohl command:
     * Add "None" as highlight group.
     */
    /*private*/ static void set_context_in_echohl_cmd(expand_C xp, Bytes arg)
    {
        xp.xp_context = EXPAND_HIGHLIGHT;
        xp.xp_pattern = arg;
        include_none = 1;
    }

    /*
     * Handle command line completion for :syntax command.
     */
    /*private*/ static void set_context_in_syntax_cmd(expand_C xp, Bytes arg)
    {
        /* Default: expand subcommands. */
        xp.xp_context = EXPAND_SYNTAX;
        expand_what = EXP_SUBCMD;
        xp.xp_pattern = arg;
        include_link = 0;
        include_default = 0;

        /* (part of) subcommand already typed */
        if (arg.at(0) != NUL)
        {
            Bytes p = skiptowhite(arg);
            if (p.at(0) != NUL)                                  /* past first word */
            {
                xp.xp_pattern = skipwhite(p);
                if (skiptowhite(xp.xp_pattern).at(0) != NUL)
                    xp.xp_context = EXPAND_NOTHING;
                else if (STRNCASECMP(arg, u8("case"), BDIFF(p, arg)) == 0)
                    expand_what = EXP_CASE;
                else if (  STRNCASECMP(arg, u8("keyword"), BDIFF(p, arg)) == 0
                        || STRNCASECMP(arg, u8("region"), BDIFF(p, arg)) == 0
                        || STRNCASECMP(arg, u8("match"), BDIFF(p, arg)) == 0
                        || STRNCASECMP(arg, u8("list"), BDIFF(p, arg)) == 0)
                    xp.xp_context = EXPAND_HIGHLIGHT;
                else
                    xp.xp_context = EXPAND_NOTHING;
            }
        }
    }

    /*private*/ static Bytes[] case_args = { u8("match"), u8("ignore"), null };

    /*
     * Function given to expandGeneric() to obtain the list syntax names for expansion.
     */
    /*private*/ static final expfun_C get_syntax_name = new expfun_C()
    {
        public Bytes expand(expand_C _xp, int idx)
        {
            if (expand_what == EXP_SUBCMD)
                return subcommands[idx].name;

            return case_args[idx];
        }
    };

    /*
     * Function called for expression evaluation: get syntax ID at file position.
     */
    /*private*/ static int syn_get_id(window_C wp, long lnum, int col, boolean trans, boolean keep_state)
        /* trans: remove transparency */
        /* keep_state: keep state of char at "col" */
    {
        /* When the position is not after the current position and in
         * the same line of the same buffer, need to restart parsing. */
        if (wp.w_buffer != syn_buf || lnum != current_lnum || col < current_col)
            syntax_start(wp, lnum);

        get_syntax_attr(col, keep_state);

        return (trans ? current_trans_id : current_id);
    }

    /*
     * Get extra information about the syntax item.  Must be called right after get_syntax_attr().
     * Stores the current item sequence nr in "*seqnrp".
     * Returns the current flags.
     */
    /*private*/ static int get_syntax_info(int[] seqnrp)
    {
        seqnrp[0] = current_seqnr;
        return current_flags;
    }

    /*
     * Return conceal substitution character
     */
    /*private*/ static int syn_get_sub_char()
    {
        return current_sub_char;
    }

    /*
     * Return the syntax ID at position "i" in the current stack.
     * The caller must have called syn_get_id() before to fill the stack.
     * Returns -1 when "i" is out of range.
     */
    /*private*/ static int syn_get_stack_item(int i)
    {
        if (current_state.ga_len <= i)
        {
            /* Need to invalidate the state, because we didn't properly
             * finish it for the last character, "keep_state" was true. */
            invalidate_current_state();
            current_col = MAXCOL;
            return -1;
        }

        return current_state.ga_data[i].si_id;
    }

    /**************************************
     *  Highlighting stuff                *
     **************************************/

    /*
     * The default highlight groups.  These are compiled-in for fast startup
     * and they still work when the runtime files can't be found.
     * When making changes here, also change runtime/colors/default.vim!
     */
    /*private*/ static Bytes[] highlight_init_both =
    {
        u8("ErrorMsg term=standout ctermbg=DarkRed ctermfg=White guibg=Red guifg=White"),
        u8("IncSearch term=reverse cterm=reverse gui=reverse"),
        u8("ModeMsg term=bold cterm=bold gui=bold"),
        u8("NonText term=bold ctermfg=Blue gui=bold guifg=Blue"),
        u8("StatusLine term=reverse,bold cterm=reverse,bold gui=reverse,bold"),
        u8("StatusLineNC term=reverse cterm=reverse gui=reverse"),
        u8("VertSplit term=reverse cterm=reverse gui=reverse"),
        u8("VisualNOS term=underline,bold cterm=underline,bold gui=underline,bold"),
        u8("TabLineSel term=bold cterm=bold gui=bold"),
        u8("TabLineFill term=reverse cterm=reverse gui=reverse"),
        null
    };

    /*private*/ static Bytes[] highlight_init_light =
    {
        u8("Directory term=bold ctermfg=DarkBlue guifg=Blue"),
        u8("LineNr term=underline ctermfg=Brown guifg=Brown"),
        u8("CursorLineNr term=bold ctermfg=Brown gui=bold guifg=Brown"),
        u8("MoreMsg term=bold ctermfg=DarkGreen gui=bold guifg=SeaGreen"),
        u8("Question term=standout ctermfg=DarkGreen gui=bold guifg=SeaGreen"),
        u8("Search term=reverse ctermbg=Yellow ctermfg=NONE guibg=Yellow guifg=NONE"),
        u8("SpecialKey term=bold ctermfg=DarkBlue guifg=Blue"),
        u8("Title term=bold ctermfg=DarkMagenta gui=bold guifg=Magenta"),
        u8("WarningMsg term=standout ctermfg=DarkRed guifg=Red"),
        u8("Visual term=reverse guibg=LightGrey"),
        u8("TabLine term=underline cterm=underline ctermfg=black ctermbg=LightGrey gui=underline guibg=LightGrey"),
        u8("CursorColumn term=reverse ctermbg=LightGrey guibg=Grey90"),
        u8("CursorLine term=underline cterm=underline guibg=Grey90"),
        u8("ColorColumn term=reverse ctermbg=LightRed guibg=LightRed"),
        u8("Conceal ctermbg=DarkGrey ctermfg=LightGrey guibg=DarkGrey guifg=LightGrey"),
        u8("MatchParen term=reverse ctermbg=Cyan guibg=Cyan"),
        null
    };

    /*private*/ static Bytes[] highlight_init_dark =
    {
        u8("Directory term=bold ctermfg=LightCyan guifg=Cyan"),
        u8("LineNr term=underline ctermfg=Yellow guifg=Yellow"),
        u8("CursorLineNr term=bold ctermfg=Yellow gui=bold guifg=Yellow"),
        u8("MoreMsg term=bold ctermfg=LightGreen gui=bold guifg=SeaGreen"),
        u8("Question term=standout ctermfg=LightGreen gui=bold guifg=Green"),
        u8("Search term=reverse ctermbg=Yellow ctermfg=Black guibg=Yellow guifg=Black"),
        u8("SpecialKey term=bold ctermfg=LightBlue guifg=Cyan"),
        u8("Title term=bold ctermfg=LightMagenta gui=bold guifg=Magenta"),
        u8("WarningMsg term=standout ctermfg=LightRed guifg=Red"),
        u8("Visual term=reverse guibg=DarkGrey"),
        u8("TabLine term=underline cterm=underline ctermfg=white ctermbg=DarkGrey gui=underline guibg=DarkGrey"),
        u8("CursorColumn term=reverse ctermbg=DarkGrey guibg=Grey40"),
        u8("CursorLine term=underline cterm=underline guibg=Grey40"),
        u8("ColorColumn term=reverse ctermbg=DarkRed guibg=DarkRed"),
        u8("MatchParen term=reverse ctermbg=DarkCyan guibg=DarkCyan"),
        u8("Conceal ctermbg=DarkGrey ctermfg=LightGrey guibg=DarkGrey guifg=LightGrey"),
        null
    };

    /*private*/ static boolean __had_both;
    /*private*/ static int __recursive;

    /*private*/ static void init_highlight(boolean both, boolean reset)
        /* both: include groups where 'bg' doesn't matter */
        /* reset: clear group first */
    {
        /*
         * Try finding the color scheme file.
         * Used when a color file was loaded and 'background' or 't_Co' is changed.
         */
        Bytes p = get_var_value(u8("g:colors_name"));
        if (p != null)
        {
            /* The value of g:colors_name could be freed when sourcing the script,
             * making "p" invalid, so copy it. */
            Bytes copy_p = STRDUP(p);

            boolean r = load_colors(copy_p);
            if (r == true)
                return;
        }

        /*
         * Didn't use a color file, use the compiled-in colors.
         */
        if (both)
        {
            __had_both = true;
            Bytes[] pp = highlight_init_both;
            for (int i = 0; pp[i] != null; i++)
                do_highlight(pp[i], reset, true);
        }
        else if (!__had_both)
            /* Don't do anything before the call with both == true from main().
             * Not everything has been setup then, and that call will overrule everything anyway. */
            return;

        Bytes[] pp;
        if (p_bg[0].at(0) == (byte)'l')
            pp = highlight_init_light;
        else
            pp = highlight_init_dark;
        for (int i = 0; pp[i] != null; i++)
            do_highlight(pp[i], reset, true);

        /* Reverse looks ugly, but grey may not work for 8 colors.
         * Thus let it depend on the number of colors available.
         * With 8 colors brown is equal to yellow, need to use black
         * for Search fg to avoid Statement highlighted text disappears.
         * Clear the attributes, needed when changing the t_Co value. */
        if (8 < t_colors)
            do_highlight((p_bg[0].at(0) == (byte)'l') ? u8("Visual cterm=NONE ctermbg=LightGrey")
                                          : u8("Visual cterm=NONE ctermbg=DarkGrey"), false, true);
        else
        {
            do_highlight(u8("Visual cterm=reverse ctermbg=NONE"), false, true);
            if (p_bg[0].at(0) == (byte)'l')
                do_highlight(u8("Search ctermfg=black"), false, true);
        }

        /*
         * If syntax highlighting is enabled load the highlighting for it.
         */
        if (get_var_value(u8("g:syntax_on")) != null)
        {
            if (5 <= __recursive)
                emsg(u8("E679: recursive loop loading syncolor.vim"));
            else
            {
                __recursive++;
                source_runtime(u8("syntax/syncolor.vim"), true);
                --__recursive;
            }
        }
    }

    /*private*/ static boolean _3_recursive;

    /*
     * Load color file "name".
     * Return true for success, false for failure.
     */
    /*private*/ static boolean load_colors(Bytes name)
    {
        /* When being called recursively,
         * this is probably because setting 'background' caused the highlighting to be reloaded.
         * This means it is working, thus we should return true. */
        if (_3_recursive)
            return true;

        _3_recursive = true;

        Bytes buf = new Bytes(strlen(name) + 12);
        libC.sprintf(buf, u8("colors/%s.vim"), name);

        boolean retval = source_runtime(buf, false);

        apply_autocmds(EVENT_COLORSCHEME, name, curbuf.b_fname, false, curbuf);

        _3_recursive = false;

        return retval;
    }

    /*private*/ static Bytes[/*28*/] color_names =
    {
        u8("Black"), u8("DarkBlue"), u8("DarkGreen"), u8("DarkCyan"),
        u8("DarkRed"), u8("DarkMagenta"), u8("Brown"), u8("DarkYellow"),
        u8("Gray"), u8("Grey"),
        u8("LightGray"), u8("LightGrey"), u8("DarkGray"), u8("DarkGrey"),
        u8("Blue"), u8("LightBlue"), u8("Green"), u8("LightGreen"),
        u8("Cyan"), u8("LightCyan"), u8("Red"), u8("LightRed"), u8("Magenta"),
        u8("LightMagenta"), u8("Yellow"), u8("LightYellow"), u8("White"), u8("NONE")
    };
    /*private*/ static int[/*28*/] color_numbers_16 =
    {
        0, 1, 2, 3,
        4, 5, 6, 6,
        7, 7,
        7, 7, 8, 8,
        9, 9, 10, 10,
        11, 11, 12, 12, 13,
        13, 14, 14, 15, -1
    };
    /*private*/ static int[/*28*/] color_numbers_88 =       /* for xterm with 88 colors... */
    {
        0, 4, 2, 6,
        1, 5, 32, 72,
        84, 84,
        7, 7, 82, 82,
        12, 43, 10, 61,
        14, 63, 9, 74, 13,
        75, 11, 78, 15, -1
    };
    /*private*/ static int[/*28*/] color_numbers_256 =      /* for xterm with 256 colors... */
    {
        0, 4, 2, 6,
        1, 5, 130, 130,
        248, 248,
        7, 7, 242, 242,
        12, 81, 10, 121,
        14, 159, 9, 224, 13,
        225, 11, 229, 15, -1
    };
    /*private*/ static int[/*28*/] color_numbers_8 =        /* for terminals with less than 16 colors... */
    {
        0, 4, 2, 6,
        1, 5, 3, 3,
        7, 7,
        7, 7, 0+8, 0+8,
        4+8, 4+8, 2+8, 2+8,
        6+8, 6+8, 1+8, 1+8, 5+8,
        5+8, 3+8, 3+8, 7+8, -1
    };

    /*
     * Handle the ":highlight .." command.
     * When using ":hi clear" this is called recursively for each group with
     * "forceit" and "init" both true.
     */
    /*private*/ static void do_highlight(Bytes line, boolean forceit, boolean init)
        /* init: true when called for initializing */
    {
        /*
         * If no argument, list current highlighting.
         */
        if (ends_excmd(line.at(0)))
        {
            for (int i = 1; i <= highlight_ga.ga_len && !got_int; i++)
                /* TODO: only call when the group has attributes set. */
                highlight_list_one(i);
            return;
        }

        /*
         * Isolate the name.
         */
        Bytes name_end = skiptowhite(line);
        Bytes linep = skipwhite(name_end);

        /*
         * Check for "default" argument.
         */
        boolean dodefault = false;
        if (STRNCMP(line, u8("default"), BDIFF(name_end, line)) == 0)
        {
            dodefault = true;
            line = linep;
            name_end = skiptowhite(line);
            linep = skipwhite(name_end);
        }

        /*
         * Check for "clear" or "link" argument.
         */
        boolean doclear = (STRNCMP(line, u8("clear"), BDIFF(name_end, line)) == 0);
        boolean dolink = (STRNCMP(line, u8("link"), BDIFF(name_end, line)) == 0);

        /*
         * ":highlight {group-name}": list highlighting for one group.
         */
        if (!doclear && !dolink && ends_excmd(linep.at(0)))
        {
            int id = syn_namen2id(line, BDIFF(name_end, line));
            if (id == 0)
                emsg2(u8("E411: highlight group not found: %s"), line);
            else
                highlight_list_one(id);
            return;
        }

        hl_group_C[] hlt = highlight_ga.ga_data;

        /*
         * Handle ":highlight link {from} {to}" command.
         */
        if (dolink)
        {
            Bytes from_start = linep;
            Bytes from_end = skiptowhite(from_start);
            Bytes to_start = skipwhite(from_end);
            Bytes to_end = skiptowhite(to_start);

            if (ends_excmd(from_start.at(0)) || ends_excmd(to_start.at(0)))
            {
                emsg2(u8("E412: Not enough arguments: \":highlight link %s\""), from_start);
                return;
            }

            if (!ends_excmd(skipwhite(to_end).at(0)))
            {
                emsg2(u8("E413: Too many arguments: \":highlight link %s\""), from_start);
                return;
            }

            int from_id = syn_check_group(from_start, BDIFF(from_end, from_start));
            int to_id;
            if (STRNCMP(to_start, u8("NONE"), 4) == 0)
                to_id = 0;
            else
                to_id = syn_check_group(to_start, BDIFF(to_end, to_start));

            if (0 < from_id && (!init || hlt[from_id - 1].sg_set == 0))
            {
                /*
                 * Don't allow a link when there already is some highlighting for the group,
                 * unless '!' is used.
                 */
                if (0 < to_id && !forceit && !init && hl_has_settings(from_id - 1, dodefault))
                {
                    if (sourcing_name == null && !dodefault)
                        emsg(u8("E414: group has settings, highlight link ignored"));
                }
                else
                {
                    if (!init)
                        hlt[from_id - 1].sg_set |= SG_LINK;
                    hlt[from_id - 1].sg_link = to_id;
                    hlt[from_id - 1].sg_scriptID = current_SID;
                    redraw_all_later(SOME_VALID);
                }
            }

            /* Only call highlight_changed() once, after sourcing a syntax file. */
            need_highlight_changed = true;
            return;
        }

        if (doclear)
        {
            /*
             * ":highlight clear [group]" command.
             */
            line = linep;
            if (ends_excmd(line.at(0)))
            {
                do_unlet(u8("colors_name"), true);
                restore_cterm_colors();

                /*
                 * Clear all default highlight groups and load the defaults.
                 */
                for (int i = 0; i < highlight_ga.ga_len; i++)
                    highlight_clear(i);
                init_highlight(true, true);
                highlight_changed();
                redraw_later_clear();
                return;
            }
            name_end = skiptowhite(line);
            linep = skipwhite(name_end);
        }

        /*
         * Find the group name in the table.  If it does not exist yet, add it.
         */
        int id = syn_check_group(line, BDIFF(name_end, line));
        if (id == 0)                            /* failed (out of memory) */
            return;
        int idx = id - 1;                       /* index is ID minus one */

        /* Return if "default" was used and the group already has settings. */
        if (dodefault && hl_has_settings(idx, true))
            return;

        /* "Normal" group. */
        boolean is_normal_group = (STRCMP(hlt[idx].sg_name_u, u8("NORMAL")) == 0);

        /* Clear the highlighting for ":hi clear {group}" and ":hi clear". */
        if (doclear || (forceit && init))
        {
            highlight_clear(idx);
            if (!doclear)
                hlt[idx].sg_set = 0;
        }

        Bytes key = null, arg = null;
        boolean error = false;

        if (!doclear)
            while (!ends_excmd(linep.at(0)))
            {
                Bytes key_start = linep;
                if (linep.at(0) == (byte)'=')
                {
                    emsg2(u8("E415: unexpected equal sign: %s"), key_start);
                    error = true;
                    break;
                }

                /*
                 * Isolate the key ("term", "ctermfg", "ctermbg", "font", "guifg" or "guibg").
                 */
                while (linep.at(0) != NUL && !vim_iswhite(linep.at(0)) && linep.at(0) != (byte)'=')
                    linep = linep.plus(1);
                key = vim_strnsave_up(key_start, BDIFF(linep, key_start));
                linep = skipwhite(linep);

                if (STRCMP(key, u8("NONE")) == 0)
                {
                    if (!init || hlt[idx].sg_set == 0)
                    {
                        if (!init)
                            hlt[idx].sg_set |= SG_TERM+SG_CTERM+SG_GUI;
                        highlight_clear(idx);
                    }
                    continue;
                }

                /*
                 * Check for the equal sign.
                 */
                if (linep.at(0) != (byte)'=')
                {
                    emsg2(u8("E416: missing equal sign: %s"), key_start);
                    error = true;
                    break;
                }
                linep = linep.plus(1);

                Bytes arg_start;
                /*
                 * Isolate the argument.
                 */
                linep = skipwhite(linep);
                if (linep.at(0) == (byte)'\'')             /* guifg='color name' */
                {
                    arg_start = (linep = linep.plus(1));
                    linep = vim_strchr(linep, '\'');
                    if (linep == null)
                    {
                        emsg2(e_invarg2, key_start);
                        error = true;
                        break;
                    }
                }
                else
                {
                    arg_start = linep;
                    linep = skiptowhite(linep);
                }
                if (BEQ(linep, arg_start))
                {
                    emsg2(u8("E417: missing argument: %s"), key_start);
                    error = true;
                    break;
                }
                arg = STRNDUP(arg_start, BDIFF(linep, arg_start));
                if (linep.at(0) == (byte)'\'')
                    linep = linep.plus(1);

                /*
                 * Store the argument.
                 */
                if (STRCMP(key, u8("TERM")) == 0 || STRCMP(key, u8("CTERM")) == 0 || STRCMP(key, u8("GUI")) == 0)
                {
                    int attr = 0;

                    for (int off = 0; arg.at(off) != NUL; )
                    {
                        int i;
                        for (i = hl_attr_table.length; 0 <= --i; )
                        {
                            int len = strlen(hl_name_table[i]);
                            if (STRNCASECMP(arg.plus(off), hl_name_table[i], len) == 0)
                            {
                                attr |= hl_attr_table[i];
                                off += len;
                                break;
                            }
                        }
                        if (i < 0)
                        {
                            emsg2(u8("E418: Illegal value: %s"), arg);
                            error = true;
                            break;
                        }
                        if (arg.at(off) == (byte)',')            /* another one follows */
                            off++;
                    }
                    if (error)
                        break;

                    if (key.at(0) == (byte)'T')
                    {
                        if (!init || (hlt[idx].sg_set & SG_TERM) == 0)
                        {
                            if (!init)
                                hlt[idx].sg_set |= SG_TERM;
                            hlt[idx].sg_term = attr;
                        }
                    }
                    else if (key.at(0) == (byte)'C')
                    {
                        if (!init || (hlt[idx].sg_set & SG_CTERM) == 0)
                        {
                            if (!init)
                                hlt[idx].sg_set |= SG_CTERM;
                            hlt[idx].sg_cterm = attr;
                            hlt[idx].sg_cterm_bold = false;
                        }
                    }
                    else
                    {
                        if (!init || (hlt[idx].sg_set & SG_GUI) == 0)
                        {
                            if (!init)
                                hlt[idx].sg_set |= SG_GUI;
                            hlt[idx].sg_gui = attr;
                        }
                    }
                }
                else if (STRCMP(key, u8("FONT")) == 0)
                {
                    /* in non-GUI fonts are simply ignored */
                }
                else if (STRCMP(key, u8("CTERMFG")) == 0 || STRCMP(key, u8("CTERMBG")) == 0)
                {
                    if (!init || (hlt[idx].sg_set & SG_CTERM) == 0)
                    {
                        if (!init)
                            hlt[idx].sg_set |= SG_CTERM;

                        /* When setting the foreground color, and previously
                         * the "bold" flag was set for a light color, reset it now. */
                        if (key.at(5) == (byte)'F' && hlt[idx].sg_cterm_bold)
                        {
                            hlt[idx].sg_cterm &= ~HL_BOLD;
                            hlt[idx].sg_cterm_bold = false;
                        }

                        int color;
                        if (asc_isdigit(arg.at(0)))
                            color = libC.atoi(arg);
                        else if (STRCASECMP(arg, u8("fg")) == 0)
                        {
                            if (cterm_normal_fg_color != 0)
                                color = cterm_normal_fg_color - 1;
                            else
                            {
                                emsg(u8("E419: FG color unknown"));
                                error = true;
                                break;
                            }
                        }
                        else if (STRCASECMP(arg, u8("bg")) == 0)
                        {
                            if (0 < cterm_normal_bg_color)
                                color = cterm_normal_bg_color - 1;
                            else
                            {
                                emsg(u8("E420: BG color unknown"));
                                error = true;
                                break;
                            }
                        }
                        else
                        {
                            int i = color_names.length;
                            /* reduce calls to STRCASECMP a bit, it can be slow */
                            for (int c = asc_toupper(arg.at(0)); 0 < i--; )
                                if (color_names[i].at(0) == c && STRCASECMP(arg.plus(1), color_names[i].plus(1)) == 0)
                                    break;
                            if (i < 0)
                            {
                                emsg2(u8("E421: Color name or number not recognized: %s"), key_start);
                                error = true;
                                break;
                            }

                            /* Use the _16 table to check if its a valid color name. */
                            color = color_numbers_16[i];
                            if (0 <= color)
                            {
                                if (t_colors == 8)
                                {
                                    /* t_Co is 8: use the 8 colors table */
                                    color = color_numbers_8[i];
                                    if (key.at(5) == (byte)'F')
                                    {
                                        /* set/reset bold attribute to get light foreground
                                         * colors (on some terminals, e.g. "linux") */
                                        if ((color & 8) != 0)
                                        {
                                            hlt[idx].sg_cterm |= HL_BOLD;
                                            hlt[idx].sg_cterm_bold = true;
                                        }
                                        else
                                            hlt[idx].sg_cterm &= ~HL_BOLD;
                                    }
                                    color &= 7;     /* truncate to 8 colors */
                                }
                                else if (t_colors == 16 || t_colors == 88 || t_colors == 256)
                                {
                                    /*
                                     * Guess: if the termcap entry ends in 'm', it is probably
                                     * an xterm-like terminal.  Use the changed order for colors.
                                     */
                                    Bytes p;
                                    if (T_CAF[0].at(0) != NUL)
                                        p = T_CAF[0];
                                    else
                                        p = T_CSF[0];
                                    if (p.at(0) != NUL && p.at(strlen(p) - 1) == 'm')
                                        switch (t_colors)
                                        {
                                            case 16:
                                                color = color_numbers_8[i];
                                                break;
                                            case 88:
                                                color = color_numbers_88[i];
                                                break;
                                            case 256:
                                                color = color_numbers_256[i];
                                                break;
                                        }
                                }
                            }
                        }

                        /* Add one to the argument, to avoid zero.
                         * Zero is used for "NONE", then "color" is -1. */
                        if (key.at(5) == (byte)'F')
                        {
                            hlt[idx].sg_cterm_fg = color + 1;
                            if (is_normal_group)
                            {
                                cterm_normal_fg_color = color + 1;
                                cterm_normal_fg_bold = (hlt[idx].sg_cterm & HL_BOLD);

                                must_redraw = CLEAR;
                                if (termcap_active && 0 <= color)
                                    term_fg_color(color);
                            }
                        }
                        else
                        {
                            hlt[idx].sg_cterm_bg = color + 1;
                            if (is_normal_group)
                            {
                                cterm_normal_bg_color = color + 1;

                                must_redraw = CLEAR;
                                if (0 <= color)
                                {
                                    if (termcap_active)
                                        term_bg_color(color);
                                    boolean b;
                                    if (t_colors < 16)
                                        b = (color == 0 || color == 4);
                                    else
                                        b = (color < 7 || color == 8);
                                    /* Set the 'background' option if the value is wrong. */
                                    if (b != (p_bg[0].at(0) == (byte)'d'))
                                        set_option_value(u8("bg"), 0L, b ? u8("dark") : u8("light"), 0);
                                }
                            }
                        }
                    }
                }
                else if (STRCMP(key, u8("GUIFG")) == 0)
                {
                    if (!init || (hlt[idx].sg_set & SG_GUI) == 0)
                    {
                        if (!init)
                            hlt[idx].sg_set |= SG_GUI;

                        if (STRCMP(arg, u8("NONE")) != 0)
                            hlt[idx].sg_gui_fg_name = STRDUP(arg);
                        else
                            hlt[idx].sg_gui_fg_name = null;
                    }
                }
                else if (STRCMP(key, u8("GUIBG")) == 0)
                {
                    if (!init || (hlt[idx].sg_set & SG_GUI) == 0)
                    {
                        if (!init)
                            hlt[idx].sg_set |= SG_GUI;

                        if (STRCMP(arg, u8("NONE")) != 0)
                            hlt[idx].sg_gui_bg_name = STRDUP(arg);
                        else
                            hlt[idx].sg_gui_bg_name = null;
                    }
                }
                else if (STRCMP(key, u8("GUISP")) == 0)
                {
                    if (!init || (hlt[idx].sg_set & SG_GUI) == 0)
                    {
                        if (!init)
                            hlt[idx].sg_set |= SG_GUI;

                        if (STRCMP(arg, u8("NONE")) != 0)
                            hlt[idx].sg_gui_sp_name = STRDUP(arg);
                        else
                            hlt[idx].sg_gui_sp_name = null;
                    }
                }
                else if (STRCMP(key, u8("START")) == 0 || STRCMP(key, u8("STOP")) == 0)
                {
                    Bytes buf = new Bytes(100);
                    Bytes tname;

                    if (!init)
                        hlt[idx].sg_set |= SG_TERM;

                    /*
                     * The "start" and "stop"  arguments can be a literal escape
                     * sequence, or a comma separated list of terminal codes.
                     */
                    if (STRNCMP(arg, u8("t_"), 2) == 0)
                    {
                        buf.be(0, NUL);
                        for (int off = 0; arg.at(off) != NUL; )
                        {
                            int len;
                            /* Isolate one termcap name. */
                            for (len = 0; arg.at(off + len) != NUL && arg.at(off + len) != (byte)','; len++)
                                ;
                            tname = STRNDUP(arg.plus(off), len);
                            /* lookup the escape sequence for the item */
                            Bytes p = get_term_code(tname);
                            if (p == null)          /* ignore non-existing things */
                                p = u8("");

                            /* Append it to the already found stuff. */
                            if (99 <= strlen(buf) + strlen(p))
                            {
                                emsg2(u8("E422: terminal code too long: %s"), arg);
                                error = true;
                                break;
                            }
                            STRCAT(buf, p);

                            /* Advance to the next item. */
                            off += len;
                            if (arg.at(off) == (byte)',')            /* another one follows */
                                off++;
                        }
                    }
                    else
                    {
                        /*
                         * Copy characters from arg[] to buf[], translating <> codes.
                         */
                        int off = 0;
                        for (Bytes[] p = { arg }; off < 100 - 6 && p[0].at(0) != NUL; )
                        {
                            int len = trans_special(p, buf.plus(off), false);
                            if (0 < len)                /* recognized special char */
                                off += len;
                            else                        /* copy as normal char */
                                buf.be(off++, (p[0] = p[0].plus(1)).at(-1));
                        }
                        buf.be(off, NUL);
                    }
                    if (error)
                        break;

                    Bytes p = null;
                    if (STRCMP(buf, u8("NONE")) != 0)
                        p = STRDUP(buf);

                    if (key.at(2) == (byte)'A')
                        hlt[idx].sg_start = p;
                    else
                        hlt[idx].sg_stop = p;
                }
                else
                {
                    emsg2(u8("E423: Illegal argument: %s"), key_start);
                    error = true;
                    break;
                }

                /*
                 * When highlighting has been given for a group, don't link it.
                 */
                if (!init || (hlt[idx].sg_set & SG_LINK) == 0)
                    hlt[idx].sg_link = 0;

                /*
                 * Continue with next argument.
                 */
                linep = skipwhite(linep);
            }

        /*
         * If there is an error, and it's a new entry, remove it from the table.
         */
        if (error && idx == highlight_ga.ga_len)
            syn_unadd_group();
        else
        {
            if (is_normal_group)
            {
                hlt[idx].sg_term_attr = 0;
                hlt[idx].sg_cterm_attr = 0;
            }
            else
                set_hl_attr(idx);
            hlt[idx].sg_scriptID = current_SID;
            redraw_all_later(NOT_VALID);
        }

        /* Only call highlight_changed() once, after sourcing a syntax file. */
        need_highlight_changed = true;
    }

    /*
     * Reset the cterm colors to what they were before Vim was started,
     * if possible.  Otherwise reset them to zero.
     */
    /*private*/ static void restore_cterm_colors()
    {
        cterm_normal_fg_color = 0;
        cterm_normal_fg_bold = 0;
        cterm_normal_bg_color = 0;
    }

    /*
     * Return true if highlight group "idx" has any settings.
     * When "check_link" is true also check for an existing link.
     */
    /*private*/ static boolean hl_has_settings(int idx, boolean check_link)
    {
        hl_group_C[] hlt = highlight_ga.ga_data;

        return (hlt[idx].sg_term_attr != 0
             || hlt[idx].sg_cterm_attr != 0
             || hlt[idx].sg_cterm_fg != 0
             || hlt[idx].sg_cterm_bg != 0
             || (check_link && (hlt[idx].sg_set & SG_LINK) != 0));
    }

    /*
     * Clear highlighting for one group.
     */
    /*private*/ static void highlight_clear(int idx)
    {
        hl_group_C[] hlt = highlight_ga.ga_data;

        hlt[idx].sg_term = 0;
        hlt[idx].sg_start = null;
        hlt[idx].sg_stop = null;
        hlt[idx].sg_term_attr = 0;
        hlt[idx].sg_cterm = 0;
        hlt[idx].sg_cterm_bold = false;
        hlt[idx].sg_cterm_fg = 0;
        hlt[idx].sg_cterm_bg = 0;
        hlt[idx].sg_cterm_attr = 0;
        hlt[idx].sg_gui = 0;
        hlt[idx].sg_gui_fg_name = null;
        hlt[idx].sg_gui_bg_name = null;
        hlt[idx].sg_gui_sp_name = null;
        /* Clear the script ID only when there is no link, since that is not cleared. */
        if (hlt[idx].sg_link == 0)
            hlt[idx].sg_scriptID = 0;
    }

    /*
     * Table with the specifications for an attribute number.
     * Note that this table is used by ALL buffers.
     * This is required because the GUI can redraw at any time for any buffer.
     */
    /*private*/ static Growing<attrentry_C> term_attr_table = new Growing<attrentry_C>(attrentry_C.class, 7);
    /*private*/ static Growing<attrentry_C> cterm_attr_table = new Growing<attrentry_C>(attrentry_C.class, 7);

    /*private*/ static boolean _4_recursive;

    /*
     * Return the attr number for a set of colors and font.
     * Add a new entry to the term_attr_table, cterm_attr_table or gui_attr_table, if the combination is new.
     * Return 0 for error (no more room).
     */
    /*private*/ static int get_attr_entry(Growing<attrentry_C> table, attrentry_C aep)
    {
        /*
         * Try to find an entry with the same specifications.
         */
        for (int i = 0; i < table.ga_len; i++)
        {
            attrentry_C taep = table.ga_data[i];
            if (aep.ae_attr == taep.ae_attr
                    && ((table == term_attr_table
                            && (aep.ae_esc_start == null) == (taep.ae_esc_start == null)
                            && (aep.ae_esc_start == null
                                || STRCMP(aep.ae_esc_start, taep.ae_esc_start) == 0)
                            && (aep.ae_esc_stop == null) == (taep.ae_esc_stop == null)
                            && (aep.ae_esc_stop == null
                                || STRCMP(aep.ae_esc_stop, taep.ae_esc_stop) == 0))
                        || (table == cterm_attr_table
                                && aep.ae_fg_color == taep.ae_fg_color
                                && aep.ae_bg_color == taep.ae_bg_color)
                         ))

            return i + ATTR_OFF;
        }

        final int MAX_TYPENR = 65535;

        if (MAX_TYPENR < table.ga_len + ATTR_OFF)
        {
            /*
             * Running out of attribute entries!
             * Remove all attributes, and compute new ones for all groups.
             * When called recursively, we are really out of numbers.
             */
            if (_4_recursive)
            {
                emsg(u8("E424: Too many different highlighting attributes in use"));
                return 0;
            }
            _4_recursive = true;

            clear_hl_tables();

            must_redraw = CLEAR;

            for (int i = 0; i < highlight_ga.ga_len; i++)
                set_hl_attr(i);

            _4_recursive = false;
        }

        /*
         * This is a new combination of colors and font, add an entry.
         */
        table.ga_grow(1);

        attrentry_C taep = table.ga_data[table.ga_len++] = new attrentry_C();

        taep.ae_attr = aep.ae_attr;
        if (table == term_attr_table)
        {
            if (aep.ae_esc_start == null)
                taep.ae_esc_start = null;
            else
                taep.ae_esc_start = STRDUP(aep.ae_esc_start);
            if (aep.ae_esc_stop == null)
                taep.ae_esc_stop = null;
            else
                taep.ae_esc_stop = STRDUP(aep.ae_esc_stop);
        }
        else if (table == cterm_attr_table)
        {
            taep.ae_fg_color = aep.ae_fg_color;
            taep.ae_bg_color = aep.ae_bg_color;
        }

        return table.ga_len - 1 + ATTR_OFF;
    }

    /*
     * Clear all highlight tables.
     */
    /*private*/ static void clear_hl_tables()
    {
        for (int i = 0; i < term_attr_table.ga_len; i++)
        {
            attrentry_C taep = term_attr_table.ga_data[i];
            taep.ae_esc_start = null;
            taep.ae_esc_stop = null;
        }
        term_attr_table.ga_clear();
        cterm_attr_table.ga_clear();
    }

    /*
     * Combine special attributes (e.g., for spelling)
     * with other attributes (e.g., for syntax highlighting).
     * "prim_attr" overrules "char_attr".
     * This creates a new group when required.
     * Since we expect there to be few spelling mistakes we don't cache the result.
     * Return the resulting attributes.
     */
    /*private*/ static int hl_combine_attr(int char_attr, int prim_attr)
    {
        attrentry_C char_aep = null;
        attrentry_C spell_aep;

        if (char_attr == 0)
            return prim_attr;
        if (char_attr <= HL_ALL && prim_attr <= HL_ALL)
            return char_attr | prim_attr;

        attrentry_C new_en = new attrentry_C();

        if (1 < t_colors)
        {
            if (HL_ALL < char_attr)
                char_aep = syn_cterm_attr2entry(char_attr);
            if (char_aep != null)
                COPY_attrentry(new_en, char_aep);
            else
            {
                ZER0_attrentry(new_en);
                if (char_attr <= HL_ALL)
                    new_en.ae_attr = char_attr;
            }

            if (prim_attr <= HL_ALL)
                new_en.ae_attr |= prim_attr;
            else
            {
                spell_aep = syn_cterm_attr2entry(prim_attr);
                if (spell_aep != null)
                {
                    new_en.ae_attr |= spell_aep.ae_attr;
                    if (0 < spell_aep.ae_fg_color)
                        new_en.ae_fg_color = spell_aep.ae_fg_color;
                    if (0 < spell_aep.ae_bg_color)
                        new_en.ae_bg_color = spell_aep.ae_bg_color;
                }
            }

            return get_attr_entry(cterm_attr_table, new_en);
        }

        if (HL_ALL < char_attr)
            char_aep = syn_term_attr2entry(char_attr);
        if (char_aep != null)
            COPY_attrentry(new_en, char_aep);
        else
        {
            ZER0_attrentry(new_en);
            if (char_attr <= HL_ALL)
                new_en.ae_attr = char_attr;
        }

        if (prim_attr <= HL_ALL)
            new_en.ae_attr |= prim_attr;
        else
        {
            spell_aep = syn_term_attr2entry(prim_attr);
            if (spell_aep != null)
            {
                new_en.ae_attr |= spell_aep.ae_attr;
                if (spell_aep.ae_esc_start != null)
                {
                    new_en.ae_esc_start = spell_aep.ae_esc_start;
                    new_en.ae_esc_stop = spell_aep.ae_esc_stop;
                }
            }
        }

        return get_attr_entry(term_attr_table, new_en);
    }

    /*
     * Get the highlight attributes (HL_BOLD etc.) from an attribute nr.
     * Only to be used when "attr" > HL_ALL.
     */
    /*private*/ static int syn_attr2attr(int attr)
    {
        attrentry_C aep;

        if (1 < t_colors)
            aep = syn_cterm_attr2entry(attr);
        else
            aep = syn_term_attr2entry(attr);

        if (aep == null)        /* highlighting not set */
            return 0;

        return aep.ae_attr;
    }

    /*private*/ static attrentry_C syn_term_attr2entry(int attr)
    {
        attr -= ATTR_OFF;
        if (term_attr_table.ga_len <= attr)     /* did ":syntax clear" */
            return null;

        return term_attr_table.ga_data[attr];
    }

    /*private*/ static attrentry_C syn_cterm_attr2entry(int attr)
    {
        attr -= ATTR_OFF;
        if (cterm_attr_table.ga_len <= attr)    /* did ":syntax clear" */
            return null;

        return cterm_attr_table.ga_data[attr];
    }

    /*private*/ static final int LIST_ATTR   = 1;
    /*private*/ static final int LIST_STRING = 2;
    /*private*/ static final int LIST_INT    = 3;

    /*private*/ static void highlight_list_one(int id)
    {
        hl_group_C[] hlt = highlight_ga.ga_data;
        hl_group_C sgp = hlt[id - 1];           /* index is ID minus one */

        boolean didh = false;

        didh = highlight_list_arg(id, didh, LIST_ATTR, sgp.sg_term, null, u8("term"));
        didh = highlight_list_arg(id, didh, LIST_STRING, 0, sgp.sg_start, u8("start"));
        didh = highlight_list_arg(id, didh, LIST_STRING, 0, sgp.sg_stop, u8("stop"));

        didh = highlight_list_arg(id, didh, LIST_ATTR, sgp.sg_cterm, null, u8("cterm"));
        didh = highlight_list_arg(id, didh, LIST_INT, sgp.sg_cterm_fg, null, u8("ctermfg"));
        didh = highlight_list_arg(id, didh, LIST_INT, sgp.sg_cterm_bg, null, u8("ctermbg"));

        didh = highlight_list_arg(id, didh, LIST_ATTR, sgp.sg_gui, null, u8("gui"));
        didh = highlight_list_arg(id, didh, LIST_STRING, 0, sgp.sg_gui_fg_name, u8("guifg"));
        didh = highlight_list_arg(id, didh, LIST_STRING, 0, sgp.sg_gui_bg_name, u8("guibg"));
        didh = highlight_list_arg(id, didh, LIST_STRING, 0, sgp.sg_gui_sp_name, u8("guisp"));

        if (sgp.sg_link != 0 && !got_int)
        {
            syn_list_header(didh, 9999, id);
            didh = true;
            msg_puts_attr(u8("links to"), hl_attr(HLF_D));
            msg_putchar(' ');
            msg_outtrans(hlt[hlt[id - 1].sg_link - 1].sg_name);
        }

        if (!didh)
            highlight_list_arg(id, didh, LIST_STRING, 0, u8("cleared"), u8(""));
        if (0 < p_verbose[0])
            last_set_msg(sgp.sg_scriptID);
    }

    /*private*/ static boolean highlight_list_arg(int id, boolean didh, int type, int iarg, Bytes sarg, Bytes name)
    {
        if (got_int)
            return false;

        if ((type == LIST_STRING) ? (sarg != null) : (iarg != 0))
        {
            Bytes buf = new Bytes(100);
            Bytes ts = buf;

            if (type == LIST_INT)
                libC.sprintf(buf, u8("%d"), iarg - 1);
            else if (type == LIST_STRING)
                ts = sarg;
            else /* type == LIST_ATTR */
            {
                buf.be(0, NUL);
                for (int i = 0; hl_attr_table[i] != 0; i++)
                    if ((iarg & hl_attr_table[i]) != 0)
                    {
                        if (buf.at(0) != NUL)
                            vim_strcat(buf, u8(","), buf.size());
                        vim_strcat(buf, hl_name_table[i], buf.size());
                        iarg &= ~hl_attr_table[i];      /* don't want "inverse" */
                    }
            }

            syn_list_header(didh, mb_string2cells(ts, -1) + strlen(name) + 1, id);
            didh = true;
            if (!got_int)
            {
                if (name.at(0) != NUL)
                {
                    msg_puts_attr(name, hl_attr(HLF_D));
                    msg_puts_attr(u8("="), hl_attr(HLF_D));
                }
                msg_outtrans(ts);
            }
        }

        return didh;
    }

    /*
     * Return "1" if highlight group "id" has attribute "flag".
     * Return null otherwise.
     */
    /*private*/ static Bytes highlight_has_attr(int id, int flag, int modec)
        /* modec: 'g' for GUI, 'c' for cterm, 't' for term */
    {
        hl_group_C[] hlt = highlight_ga.ga_data;

        if (id <= 0 || highlight_ga.ga_len < id)
            return null;

        int attr;

        if (modec == 'g')
            attr = hlt[id - 1].sg_gui;
        else if (modec == 'c')
            attr = hlt[id - 1].sg_cterm;
        else
            attr = hlt[id - 1].sg_term;

        if ((attr & flag) != 0)
            return u8("1");

        return null;
    }

    /*private*/ static Bytes hc_name = new Bytes(20);

    /*
     * Return color name of highlight group "id".
     */
    /*private*/ static Bytes highlight_color(int id, Bytes what, int modec)
        /* what: "font", "fg", "bg", "sp", "fg#", "bg#" or "sp#" */
        /* modec: 'g' for GUI, 'c' for cterm, 't' for term */
    {
        boolean fg = false;
        boolean font = false;
        boolean sp = false;

        if (id <= 0 || highlight_ga.ga_len < id)
            return null;

        if (asc_tolower(what.at(0)) == 'f' && asc_tolower(what.at(1)) == 'g')
            fg = true;
        else if (asc_tolower(what.at(0)) == 'f' && asc_tolower(what.at(1)) == 'o'
              && asc_tolower(what.at(2)) == 'n' && asc_tolower(what.at(3)) == 't')
            font = true;
        else if (asc_tolower(what.at(0)) == 's' && asc_tolower(what.at(1)) == 'p')
            sp = true;
        else if (!(asc_tolower(what.at(0)) == 'b' && asc_tolower(what.at(1)) == 'g'))
            return null;

        hl_group_C[] hlt = highlight_ga.ga_data;

        if (modec == 'g')
        {
            if (fg)
                return hlt[id - 1].sg_gui_fg_name;
            if (sp)
                return hlt[id - 1].sg_gui_sp_name;

            return hlt[id - 1].sg_gui_bg_name;
        }

        if (font || sp)
            return null;

        if (modec == 'c')
        {
            int n;
            if (fg)
                n = hlt[id - 1].sg_cterm_fg - 1;
            else
                n = hlt[id - 1].sg_cterm_bg - 1;
            libC.sprintf(hc_name, u8("%d"), n);
            return hc_name;
        }

        /* term doesn't have color */
        return null;
    }

    /*
     * Output the syntax list header.
     * Return true when started a new line.
     */
    /*private*/ static boolean syn_list_header(boolean did_header, int outlen, int id)
        /* did_header: did header already */
        /* outlen: length of string that comes */
        /* id: highlight group id */
    {
        int endcol = 19;
        boolean newline = true;

        if (!did_header)
        {
            msg_putchar('\n');
            if (got_int)
                return true;

            hl_group_C[] hlt = highlight_ga.ga_data;

            msg_outtrans(hlt[id - 1].sg_name);
            endcol = 15;
        }
        else if ((int)Columns[0] <= msg_col + outlen + 1)
        {
            msg_putchar('\n');
            if (got_int)
                return true;
        }
        else
        {
            if (endcol <= msg_col)  /* wrap around is like starting a new line */
                newline = false;
        }

        if (endcol <= msg_col)      /* output at least one space */
            endcol = msg_col + 1;
        if ((int)Columns[0] <= endcol)      /* avoid hang for tiny window */
            endcol = (int)Columns[0] - 1;

        msg_advance(endcol);

        /* Show "xxx" with the attributes. */
        if (!did_header)
        {
            msg_puts_attr(u8("xxx"), syn_id2attr(id));
            msg_putchar(' ');
        }

        return newline;
    }

    /*
     * Set the attribute numbers for a highlight group.
     * Called after one of the attributes has changed.
     */
    /*private*/ static void set_hl_attr(int idx)
        /* idx: index in array */
    {
        hl_group_C[] hlt = highlight_ga.ga_data;
        hl_group_C sgp = hlt[idx];

        /* The "Normal" group doesn't need an attribute number. */
        if (sgp.sg_name_u != null && STRCMP(sgp.sg_name_u, u8("NORMAL")) == 0)
            return;

        /*
         * For the term mode: If there are other than "normal" highlighting
         * attributes, need to allocate an attr number.
         */
        if (sgp.sg_start == null && sgp.sg_stop == null)
            sgp.sg_term_attr = sgp.sg_term;
        else
        {
            attrentry_C at_en = new attrentry_C();
            at_en.ae_attr = sgp.sg_term;
            at_en.ae_esc_start = sgp.sg_start;
            at_en.ae_esc_stop = sgp.sg_stop;
            sgp.sg_term_attr = get_attr_entry(term_attr_table, at_en);
        }

        /*
         * For the color term mode: If there are other than "normal"
         * highlighting attributes, need to allocate an attr number.
         */
        if (sgp.sg_cterm_fg == 0 && sgp.sg_cterm_bg == 0)
            sgp.sg_cterm_attr = sgp.sg_cterm;
        else
        {
            attrentry_C at_en = new attrentry_C();
            at_en.ae_attr = sgp.sg_cterm;
            at_en.ae_fg_color = sgp.sg_cterm_fg;
            at_en.ae_bg_color = sgp.sg_cterm_bg;
            sgp.sg_cterm_attr = get_attr_entry(cterm_attr_table, at_en);
        }
    }

    /*
     * Like syn_name2id(), but take a pointer + length argument.
     */
    /*private*/ static int syn_namen2id(Bytes linep, int len)
    {
        Bytes name = STRNDUP(linep, len);
        return syn_name2id(name);
    }

    /*
     * Lookup a highlight group name and return it's ID.
     * If it is not found, 0 is returned.
     */
    /*private*/ static int syn_name2id(Bytes name)
    {
        Bytes name_u = vim_strsave_up(name);

        hl_group_C[] hlt = highlight_ga.ga_data;

        int i;
        for (i = highlight_ga.ga_len; 0 <= --i; )
            if (hlt[i].sg_name_u != null && STRCMP(name_u, hlt[i].sg_name_u) == 0)
                break;
        return i + 1;
    }

    /*
     * Return true if highlight group "name" exists.
     */
    /*private*/ static boolean highlight_exists(Bytes name)
    {
        return (0 < syn_name2id(name));
    }

    /*
     * Return the name of highlight group "id".
     * When not a valid ID return an empty string.
     */
    /*private*/ static Bytes syn_id2name(int id)
    {
        hl_group_C[] hlt = highlight_ga.ga_data;

        if (id <= 0 || highlight_ga.ga_len < id)
            return u8("");

        return hlt[id - 1].sg_name;
    }

    /*
     * Find highlight group name in the table and return it's ID.
     * The argument is a pointer to the name and the length of the name.
     * If it doesn't exist yet, a new entry is created.
     * Return 0 for failure.
     */
    /*private*/ static int syn_check_group(Bytes pp, int len)
    {
        Bytes name = STRNDUP(pp, len);

        int id = syn_name2id(name);
        if (id == 0)                        /* doesn't exist yet */
            id = syn_add_group(name);

        return id;
    }

    /*
     * Add new highlight group and return it's ID.
     * "name" must be an allocated string, it will be consumed.
     * Return 0 for failure.
     */
    /*private*/ static int syn_add_group(Bytes name)
    {
        /* Check that the name is ASCII letters, digits and underscore. */
        for (Bytes p = name; p.at(0) != NUL; p = p.plus(1))
        {
            if (!vim_isprintc(p.at(0)))
            {
                emsg(u8("E669: Unprintable character in group name"));
                return 0;
            }
            else if (!asc_isalnum(p.at(0)) && p.at(0) != (byte)'_')
            {
                /* This is an error, but since there previously was no check only give a warning. */
                msg_source(hl_attr(HLF_W));
                msg(u8("W18: Invalid character in group name"));
                break;
            }
        }

        if (MAX_HL_ID <= highlight_ga.ga_len)
        {
            emsg(u8("E849: Too many highlight and syntax groups"));
            return 0;
        }

        /*
         * Make room for at least one other syntax_highlight entry.
         */
        hl_group_C[] hlt = highlight_ga.ga_grow(1);

        hlt[highlight_ga.ga_len] = new hl_group_C();
        hlt[highlight_ga.ga_len].sg_name = name;
        hlt[highlight_ga.ga_len].sg_name_u = vim_strsave_up(name);

        highlight_ga.ga_len++;

        return highlight_ga.ga_len; /* ID is index plus one */
    }

    /*
     * When, just after calling syn_add_group(), an error is discovered, this function deletes the new name.
     */
    /*private*/ static void syn_unadd_group()
    {
        hl_group_C[] hlt = highlight_ga.ga_data;

        hlt[--highlight_ga.ga_len] = null;
    }

    /*
     * Translate a group ID to highlight attributes.
     */
    /*private*/ static int syn_id2attr(int hl_id)
    {
        hl_id = syn_get_final_id(hl_id);

        hl_group_C[] hlt = highlight_ga.ga_data;
        hl_group_C sgp = hlt[hl_id - 1];                        /* index is ID minus one */

        return (1 < t_colors) ? sgp.sg_cterm_attr : sgp.sg_term_attr;
    }

    /*
     * Translate a group ID to the final group ID (following links).
     */
    /*private*/ static int syn_get_final_id(int hl_id)
    {
        if (highlight_ga.ga_len < hl_id || hl_id < 1)
            return 0;                                           /* Can be called from eval!! */

        hl_group_C[] hlt = highlight_ga.ga_data;

        /*
         * Follow links until there is no more.
         * Look out for loops!  Break after 100 links.
         */
        for (int count = 100; 0 <= --count; )
        {
            hl_group_C sgp = hlt[hl_id - 1];                    /* index is ID minus one */
            if (sgp.sg_link == 0 || highlight_ga.ga_len < sgp.sg_link)
                break;
            hl_id = sgp.sg_link;
        }

        return hl_id;
    }

    /* The HL_FLAGS must be in the same order as the HLF_ enums!
     * When changing this also adjust the default for 'highlight'.
     */
    /*private*/ static int[/*HLF_COUNT*/] hl_flags =
    {
        '8', '@', 'd', 'e', 'h', 'i', 'l', 'm',
        'M', 'n', 'N', 'r', 's', 'S', 'c', 't',
        'v', 'V', 'w', 'W', 'f', 'F', 'A', 'C',
        'D', 'T', '-', '>', 'B', 'P', 'R', 'L',
        '+', '=', 'x', 'X', '*', '#', '_', '!',
        '.', 'o'
    };

    /*
     * Translate the 'highlight' option into attributes in highlight_attr[] and
     * set up the user highlights User1..9.  If FEAT_STL_OPT is in use, a set of
     * corresponding highlights to use on top of HLF_SNC is computed.
     * Called only when the 'highlight' option has been changed and upon first
     * screen redraw after any :highlight command.
     * Return false when an invalid flag is found in 'highlight'; true otherwise.
     */
    /*private*/ static boolean highlight_changed()
    {
        int id_SNC = -1;
        int id_S = -1;

        need_highlight_changed = false;

        /*
         * Clear all attributes.
         */
        for (int hlf = 0; hlf < HLF_COUNT; hlf++)
            highlight_attr[hlf] = 0;

        /*
         * First set all attributes to their default value.
         * Then use the attributes from the 'highlight' option.
         */
        for (int i = 0; i < 2; i++)
        {
            Bytes p;
            if (i != 0)
                p = p_hl[0];
            else
                p = get_highlight_default();
            if (p == null)      /* just in case */
                continue;

            while (p.at(0) != NUL)
            {
                int hlf;
                for (hlf = 0; hlf < HLF_COUNT; hlf++)
                    if (hl_flags[hlf] == p.at(0))
                        break;
                p = p.plus(1);
                if (hlf == HLF_COUNT || p.at(0) == NUL)
                    return false;

                /*
                 * Allow several hl_flags to be combined, like "bu" for bold-underlined.
                 */
                int attr = 0;
                for ( ; p.at(0) != NUL && p.at(0) != (byte)','; p = p.plus(1))            /* parse upto comma */
                {
                    if (vim_iswhite(p.at(0)))                        /* ignore white space */
                        continue;

                    if (HL_ALL < attr)  /* Combination with ':' is not allowed. */
                        return false;

                    switch (p.at(0))
                    {
                        case 'b':
                            attr |= HL_BOLD;
                            break;
                        case 'i':
                            attr |= HL_ITALIC;
                            break;
                        case '-':
                        case 'n':                       /* no highlighting */
                            break;
                        case 'r':
                            attr |= HL_INVERSE;
                            break;
                        case 's':
                            attr |= HL_STANDOUT;
                            break;
                        case 'u':
                            attr |= HL_UNDERLINE;
                            break;
                        case 'c':
                            attr |= HL_UNDERCURL;
                            break;
                        case ':':
                        {
                            p = p.plus(1);                        /* highlight group name */
                            if (attr != 0 || p.at(0) == NUL)      /* no combinations */
                                return false;
                            Bytes end = vim_strchr(p, ',');
                            if (end == null)
                                end = p.plus(strlen(p));
                            int id = syn_check_group(p, BDIFF(end, p));
                            if (id == 0)
                                return false;
                            attr = syn_id2attr(id);
                            p = end.minus(1);
                            if (hlf == HLF_SNC)
                                id_SNC = syn_get_final_id(id);
                            else if (hlf == HLF_S)
                                id_S = syn_get_final_id(id);
                            break;
                        }
                        default:
                            return false;
                    }
                }
                highlight_attr[hlf] = attr;

                p = skip_to_option_part(p);             /* skip comma and spaces */
            }
        }

        /* Setup the user highlights
         *
         * Temporarily utilize 10 more hl entries.  Have to be in there
         * simultaneously in case of table overflows in get_attr_entry()
         */
        hl_group_C[] hlt = highlight_ga.ga_grow(10);

        int n = highlight_ga.ga_len;
        for (int i = 0; i < 10; i++)
            hlt[n + i] = new hl_group_C();

        /* Make sure id_S is always valid to simplify code below. */
        if (id_S == 0)
        {
            hlt[n + 9].sg_term = highlight_attr[HLF_S];
            id_S = n + 10;
        }

        for (int i = 0; i < 9; i++)
        {
            Bytes userhl = new Bytes(10);
            libC.sprintf(userhl, u8("User%d"), i + 1);

            int id = syn_name2id(userhl);
            if (id == 0)
            {
                highlight_user[i] = 0;
                highlight_stlnc[i] = 0;
            }
            else
            {
                highlight_user[i] = syn_id2attr(id);
                if (id_SNC == 0)
                {
                    hlt[n + i].sg_term = highlight_attr[HLF_SNC];
                    hlt[n + i].sg_cterm = highlight_attr[HLF_SNC];
                    hlt[n + i].sg_gui = highlight_attr[HLF_SNC];
                }
                else
                    COPY_hl_group(hlt[n + i], hlt[id_SNC - 1]);
                hlt[n + i].sg_link = 0;

                /* Apply difference between UserX and HLF_S to HLF_SNC. */
                hlt[n + i].sg_term ^= (hlt[id - 1].sg_term ^ hlt[id_S - 1].sg_term);
                if (BNE(hlt[id - 1].sg_start, hlt[id_S - 1].sg_start))
                    hlt[n + i].sg_start = hlt[id - 1].sg_start;
                if (BNE(hlt[id - 1].sg_stop, hlt[id_S - 1].sg_stop))
                    hlt[n + i].sg_stop = hlt[id - 1].sg_stop;
                hlt[n + i].sg_cterm ^= (hlt[id - 1].sg_cterm ^ hlt[id_S - 1].sg_cterm);
                if (hlt[id - 1].sg_cterm_fg != hlt[id_S - 1].sg_cterm_fg)
                    hlt[n + i].sg_cterm_fg = hlt[id - 1].sg_cterm_fg;
                if (hlt[id - 1].sg_cterm_bg != hlt[id_S - 1].sg_cterm_bg)
                    hlt[n + i].sg_cterm_bg = hlt[id - 1].sg_cterm_bg;
                hlt[n + i].sg_gui ^= (hlt[id - 1].sg_gui ^ hlt[id_S - 1].sg_gui);
                highlight_ga.ga_len = n + i + 1;
                set_hl_attr(n + i);         /* at long last we can apply */
                highlight_stlnc[i] = syn_id2attr(n + i + 1);
            }
        }

        highlight_ga.ga_len = n;
        for (int i = 0; i < 10; i++)
            hlt[n + i] = null;

        return true;
    }

    /*
     * Handle command line completion for :highlight command.
     */
    /*private*/ static void set_context_in_highlight_cmd(expand_C xp, Bytes arg)
    {
        /* Default: expand group names. */
        xp.xp_context = EXPAND_HIGHLIGHT;
        xp.xp_pattern = arg;
        include_link = 2;
        include_default = 1;

        /* (part of) subcommand already typed */
        if (arg.at(0) != NUL)
        {
            Bytes p = skiptowhite(arg);
            if (p.at(0) != NUL)                                  /* past "default" or group name */
            {
                include_default = 0;
                if (STRNCMP(u8("default"), arg, BDIFF(p, arg)) == 0)
                {
                    arg = skipwhite(p);
                    xp.xp_pattern = arg;
                    p = skiptowhite(arg);
                }
                if (p.at(0) != NUL)                              /* past group name */
                {
                    include_link = 0;
                    if (arg.at(1) == (byte)'i' && arg.at(0) == (byte)'N')
                        highlight_list();
                    if (STRNCMP(u8("link"), arg, BDIFF(p, arg)) == 0
                     || STRNCMP(u8("clear"), arg, BDIFF(p, arg)) == 0)
                    {
                        xp.xp_pattern = skipwhite(p);
                        p = skiptowhite(xp.xp_pattern);
                        if (p.at(0) != NUL)                      /* past first group name */
                        {
                            xp.xp_pattern = skipwhite(p);
                            p = skiptowhite(xp.xp_pattern);
                        }
                    }
                    if (p.at(0) != NUL)                          /* past group name(s) */
                        xp.xp_context = EXPAND_NOTHING;
                }
            }
        }
    }

    /*
     * List highlighting matches in a nice way.
     */
    /*private*/ static void highlight_list()
    {
        for (int i = 10; 0 <= --i; )
            highlight_list_two(i, hl_attr(HLF_D));
        for (int i = 40; 0 <= --i; )
            highlight_list_two(99, 0);
    }

    /*private*/ static void highlight_list_two(int cnt, int attr)
    {
        msg_puts_attr(u8("N \bI \b!  \b").plus(cnt / 11), attr);
        msg_clr_eos();
        out_flush();
        ui_delay(cnt == 99 ? 40L : (long)cnt * 50L, false);
    }

    /*
     * Function given to expandGeneric() to obtain the list of group names.
     * Also used for synIDattr() function.
     */
    /*private*/ static final expfun_C get_highlight_name = new expfun_C()
    {
        public Bytes expand(expand_C _xp, int idx)
        {
            hl_group_C[] hlt = highlight_ga.ga_data;

            if (idx == highlight_ga.ga_len && include_none != 0)
                return u8("none");
            if (idx == highlight_ga.ga_len + include_none && include_default != 0)
                return u8("default");
            if (idx == highlight_ga.ga_len + include_none + include_default && include_link != 0)
                return u8("link");
            if (idx == highlight_ga.ga_len + include_none + include_default + 1 && include_link != 0)
                return u8("clear");
            if (idx < 0 || highlight_ga.ga_len <= idx)
                return null;

            return hlt[idx].sg_name;
        }
    };
}
