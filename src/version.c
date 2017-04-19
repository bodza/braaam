#include "vim.h"

#include "version.h"

char            *Version = VIM_VERSION_SHORT;
static char     *mediumVersion = VIM_VERSION_MEDIUM;

#if defined(HAVE_DATE_TIME)
char    *longVersion = VIM_VERSION_LONG_DATE __DATE__ " " __TIME__ ")";
#else
char    *longVersion = VIM_VERSION_LONG;
#endif

static void list_features __ARGS((void));
static void version_msg __ARGS((char *s));

static char *(features[]) =
{
#if defined(FEAT_AUTOCMD)
        "+autocmd",
#else
        "-autocmd",
#endif
#if defined(NO_BUILTIN_TCAPS)
        "-builtin_terms",
#endif
#if defined(SOME_BUILTIN_TCAPS)
        "+builtin_terms",
#endif
#if defined(ALL_BUILTIN_TCAPS)
        "++builtin_terms",
#endif
#if defined(FEAT_BYTEOFF)
        "+byte_offset",
#else
        "-byte_offset",
#endif
#if defined(FEAT_CINDENT)
        "+cindent",
#else
        "-cindent",
#endif
#if defined(FEAT_CLIPBOARD)
        "+clipboard",
#else
        "-clipboard",
#endif
#if defined(FEAT_CMDL_COMPL)
        "+cmdline_compl",
#else
        "-cmdline_compl",
#endif
#if defined(FEAT_CMDHIST)
        "+cmdline_hist",
#else
        "-cmdline_hist",
#endif
#if defined(FEAT_CMDL_INFO)
        "+cmdline_info",
#else
        "-cmdline_info",
#endif
#if defined(FEAT_COMMENTS)
        "+comments",
#else
        "-comments",
#endif
#if defined(FEAT_CONCEAL)
        "+conceal",
#else
        "-conceal",
#endif
#if defined(FEAT_CURSORBIND)
        "+cursorbind",
#else
        "-cursorbind",
#endif
#if defined(CURSOR_SHAPE)
        "+cursorshape",
#else
        "-cursorshape",
#endif
#if defined(FEAT_CON_DIALOG)
        "+dialog_con",
#else
        "-dialog",
#endif
#if defined(FEAT_DIGRAPHS)
        "+digraphs",
#else
        "-digraphs",
#endif
#if defined(FEAT_DND)
        "+dnd",
#else
        "-dnd",
#endif
        "+eval",
#if defined(FEAT_EX_EXTRA)
        "+ex_extra",
#else
        "-ex_extra",
#endif
#if defined(FEAT_SEARCH_EXTRA)
        "+extra_search",
#else
        "-extra_search",
#endif
#if defined(FEAT_SEARCHPATH)
        "+file_in_path",
#else
        "-file_in_path",
#endif
#if defined(FEAT_FIND_ID)
        "+find_in_path",
#else
        "-find_in_path",
#endif
        "+float",
#if defined(FEAT_FOOTER)
        "+footer",
#else
        "-footer",
#endif
        "+fork()",
#if (defined(HAVE_ICONV_H) && defined(USE_ICONV))
        "+iconv",
#else
        "-iconv",
#endif
#if defined(FEAT_INS_EXPAND)
        "+insert_expand",
#else
        "-insert_expand",
#endif
#if defined(FEAT_JUMPLIST)
        "+jumplist",
#else
        "-jumplist",
#endif
#if defined(FEAT_KEYMAP)
        "+keymap",
#else
        "-keymap",
#endif
#if defined(FEAT_LANGMAP)
        "+langmap",
#else
        "-langmap",
#endif
#if defined(FEAT_LINEBREAK)
        "+linebreak",
#else
        "-linebreak",
#endif
#if defined(FEAT_LISP)
        "+lispindent",
#else
        "-lispindent",
#endif
#if defined(FEAT_LISTCMDS)
        "+listcmds",
#else
        "-listcmds",
#endif
#if defined(FEAT_LOCALMAP)
        "+localmap",
#else
        "-localmap",
#endif
#if defined(FEAT_MENU)
        "+menu",
#else
        "-menu",
#endif
#if defined(FEAT_MODIFY_FNAME)
        "+modify_fname",
#else
        "-modify_fname",
#endif
#if defined(FEAT_MOUSE)
        "+mouse",
#if defined(FEAT_MOUSESHAPE)
        "+mouseshape",
#else
        "-mouseshape",
#endif
#else
        "-mouse",
#endif
#if defined(FEAT_MOUSE_GPM)
        "+mouse_gpm",
#else
        "-mouse_gpm",
#endif
#if defined(FEAT_SYSMOUSE)
        "+mouse_sysmouse",
#else
        "-mouse_sysmouse",
#endif
#if defined(FEAT_MOUSE_XTERM)
        "+mouse_xterm",
#else
        "-mouse_xterm",
#endif
        "+multi_byte",
#if defined(FEAT_PATH_EXTRA)
        "+path_extra",
#else
        "-path_extra",
#endif
#if defined(FEAT_PERSISTENT_UNDO)
        "+persistent_undo",
#else
        "-persistent_undo",
#endif
#if defined(FEAT_PRINTER)
#if defined(FEAT_POSTSCRIPT)
        "+postscript",
#else
        "-postscript",
#endif
        "+printer",
#else
        "-printer",
#endif
#if defined(FEAT_QUICKFIX)
        "+quickfix",
#else
        "-quickfix",
#endif
#if defined(FEAT_RELTIME)
        "+reltime",
#else
        "-reltime",
#endif
#if defined(FEAT_RIGHTLEFT)
        "+rightleft",
#else
        "-rightleft",
#endif
#if defined(FEAT_SCROLLBIND)
        "+scrollbind",
#else
        "-scrollbind",
#endif
#if defined(FEAT_SMARTINDENT)
        "+smartindent",
#else
        "-smartindent",
#endif
#if defined(FEAT_STL_OPT)
        "+statusline",
#else
        "-statusline",
#endif
#if defined(FEAT_SYN_HL)
        "+syntax",
#else
        "-syntax",
#endif
#if defined(FEAT_TAG_BINS)
        "+tag_binary",
#else
        "-tag_binary",
#endif
#if defined(TERMINFO)
        "+terminfo",
#else
        "-terminfo",
#endif
#if defined(FEAT_TERMRESPONSE)
        "+termresponse",
#else
        "-termresponse",
#endif
#if defined(FEAT_TEXTOBJ)
        "+textobjects",
#else
        "-textobjects",
#endif
#if defined(FEAT_TITLE)
        "+title",
#else
        "-title",
#endif
#if defined(FEAT_USR_CMDS)
        "+user_commands",
#else
        "-user_commands",
#endif
#if defined(FEAT_VERTSPLIT)
        "+vertsplit",
#else
        "-vertsplit",
#endif
#if defined(FEAT_VIRTUALEDIT)
        "+virtualedit",
#else
        "-virtualedit",
#endif
        "+visual",
#if defined(FEAT_VISUALEXTRA)
        "+visualextra",
#else
        "-visualextra",
#endif
#if defined(FEAT_VREPLACE)
        "+vreplace",
#else
        "-vreplace",
#endif
#if defined(FEAT_WILDIGN)
        "+wildignore",
#else
        "-wildignore",
#endif
#if defined(FEAT_WILDMENU)
        "+wildmenu",
#else
        "-wildmenu",
#endif
#if defined(FEAT_WINDOWS)
        "+windows",
#else
        "-windows",
#endif
#if defined(FEAT_WRITEBACKUP)
        "+writebackup",
#else
        "-writebackup",
#endif
#if defined(FEAT_XTERM_SAVE)
        "+xterm_save",
#else
        "-xterm_save",
#endif
        NULL
};

static int included_patches[] =
{
    692, 691, 690,
    689, 688, 687, 686, 685, 684, 683, 682, 681, 680,
    679, 678, 677, 676, 675, 674, 673, 672, 671, 670,
    669, 668, 667, 666, 665, 664, 663, 662, 661, 660,
    659, 658, 657, 656, 655, 654, 653, 652, 651, 650,
    649, 648, 647, 646, 645, 644, 643, 642, 641, 640,
    639, 638, 637, 636, 635, 634, 633, 632, 631, 630,
    629, 628, 627, 626, 625, 624, 623, 622, 621, 620,
    619, 618, 617, 616, 615, 614, 613, 612, 611, 610,
    609, 608, 607, 606, 605, 604, 603, 602, 601, 600,
    599, 598, 597, 596, 595, 594, 593, 592, 591, 590,
    589, 588, 587, 586, 585, 584, 583, 582, 581, 580,
    579, 578, 577, 576, 575, 574, 573, 572, 571, 570,
    569, 568, 567, 566, 565, 564, 563, 562, 561, 560,
    559, 558, 557, 556, 555, 554, 553, 552, 551, 550,
    549, 548, 547, 546, 545, 544, 543, 542, 541, 540,
    539, 538, 537, 536, 535, 534, 533, 532, 531, 530,
    529, 528, 527, 526, 525, 524, 523, 522, 521, 520,
    519, 518, 517, 516, 515, 514, 513, 512, 511, 510,
    509, 508, 507, 506, 505, 504, 503, 502, 501, 500,
    499, 498, 497, 496, 495, 494, 493, 492, 491, 490,
    489, 488, 487, 486, 485, 484, 483, 482, 481, 480,
    479, 478, 477, 476, 475, 474, 473, 472, 471, 470,
    469, 468, 467, 466, 465, 464, 463, 462, 461, 460,
    459, 458, 457, 456, 455, 454, 453, 452, 451, 450,
    449, 448, 447, 446, 445, 444, 443, 442, 441, 440,
    439, 438, 437, 436, 435, 434, 433, 432, 431, 430,
    429, 428, 427, 426, 425, 424, 423, 422, 421, 420,
    419, 418, 417, 416, 415, 414, 413, 412, 411, 410,
    409, 408, 407, 406, 405, 404, 403, 402, 401, 400,
    399, 398, 397, 396, 395, 394, 393, 392, 391, 390,
    389, 388, 387, 386, 385, 384, 383, 382, 381, 380,
    379, 378, 377, 376, 375, 374, 373, 372, 371, 370,
    369, 368, 367, 366, 365, 364, 363, 362, 361, 360,
    359, 358, 357, 356, 355, 354, 353, 352, 351, 350,
    349, 348, 347, 346, 345, 344, 343, 342, 341, 340,
    339, 338, 337, 336, 335, 334, 333, 332, 331, 330,
    329, 328, 327, 326, 325, 324, 323, 322, 321, 320,
    319, 318, 317, 316, 315, 314, 313, 312, 311, 310,
    309, 308, 307, 306, 305, 304, 303, 302, 301, 300,
    299, 298, 297, 296, 295, 294, 293, 292, 291, 290,
    289, 288, 287, 286, 285, 284, 283, 282, 281, 280,
    279, 278, 277, 276, 275, 274, 273, 272, 271, 270,
    269, 268, 267, 266, 265, 264, 263, 262, 261, 260,
    259, 258, 257, 256, 255, 254, 253, 252, 251, 250,
    249, 248, 247, 246, 245, 244, 243, 242, 241, 240,
    239, 238, 237, 236, 235, 234, 233, 232, 231, 230,
    229, 228, 227, 226, 225, 224, 223, 222, 221, 220,
    219, 218, 217, 216, 215, 214, 213, 212, 211, 210,
    209, 208, 207, 206, 205, 204, 203, 202, 201, 200,
    199, 198, 197, 196, 195, 194, 193, 192, 191, 190,
    189, 188, 187, 186, 185, 184, 183, 182, 181, 180,
    179, 178, 177, 176, 175, 174, 173, 172, 171, 170,
    169, 168, 167, 166, 165, 164, 163, 162, 161, 160,
    159, 158, 157, 156, 155, 154, 153, 152, 151, 150,
    149, 148, 147, 146, 145, 144, 143, 142, 141, 140,
    139, 138, 137, 136, 135, 134, 133, 132, 131, 130,
    129, 128, 127, 126, 125, 124, 123, 122, 121, 120,
    119, 118, 117, 116, 115, 114, 113, 112, 111, 110,
    109, 108, 107, 106, 105, 104, 103, 102, 101, 100,
    99, 98, 97, 96, 95, 94, 93, 92, 91, 90,
    89, 88, 87, 86, 85, 84, 83, 82, 81, 80,
    79, 78, 77, 76, 75, 74, 73, 72, 71, 70,
    69, 68, 67, 66, 65, 64, 63, 62, 61, 60,
    59, 58, 57, 56, 55, 54, 53, 52, 51, 50,
    49, 48, 47, 46, 45, 44, 43, 42, 41, 40,
    39, 38, 37, 36, 35, 34, 33, 32, 31, 30,
    29, 28, 27, 26, 25, 24, 23, 22, 21, 20,
    19, 18, 17, 16, 15, 14, 13, 12, 11, 10,
    9, 8, 7, 6, 5, 4, 3, 2, 1, 0
};

/*
 * Place to put a short description when adding a feature with a patch.
 * Keep it short, e.g.,: "relative numbers", "persistent undo".
 * Also add a comment marker to separate the lines.
 * See the official Vim patches for the diff format: It must use a context of
 * one line only.  Create it by hand or use "diff -C2" and edit the patch.
 */
static char *(extra_patches[]) =
{   /* Add your patch description below this line */
/**/
    NULL
};

    int
highest_patch()
{
    int         i;
    int         h = 0;

    for (i = 0; included_patches[i] != 0; ++i)
        if (included_patches[i] > h)
            h = included_patches[i];
    return h;
}

/*
 * Return TRUE if patch "n" has been included.
 */
    int
has_patch(n)
    int         n;
{
    int         i;

    for (i = 0; included_patches[i] != 0; ++i)
        if (included_patches[i] == n)
            return TRUE;
    return FALSE;
}

    void
ex_version(eap)
    exarg_T     *eap;
{
    /*
     * Ignore a ":version 9.99" command.
     */
    if (*eap->arg == NUL)
    {
        msg_putchar('\n');
        list_version();
    }
}

/*
 * List all features aligned in columns, dictionary style.
 */
    static void
list_features()
{
    int         i;
    int         ncol;
    int         nrow;
    int         nfeat = 0;
    int         width = 0;

    /* Find the length of the longest feature name, use that + 1 as the column
     * width */
    for (i = 0; features[i] != NULL; ++i)
    {
        int l = (int)STRLEN(features[i]);

        if (l > width)
            width = l;
        ++nfeat;
    }
    width += 1;

    if (Columns < width)
    {
        /* Not enough screen columns - show one per line */
        for (i = 0; features[i] != NULL; ++i)
        {
            version_msg(features[i]);
            if (msg_col > 0)
                msg_putchar('\n');
        }
        return;
    }

    /* The rightmost column doesn't need a separator.
     * Sacrifice it to fit in one more column if possible. */
    ncol = (int) (Columns + 1) / width;
    nrow = nfeat / ncol + (nfeat % ncol ? 1 : 0);

    /* i counts columns then rows.  idx counts rows then columns. */
    for (i = 0; !got_int && i < nrow * ncol; ++i)
    {
        int idx = (i / ncol) + (i % ncol) * nrow;

        if (idx < nfeat)
        {
            int last_col = (i + 1) % ncol == 0;

            msg_puts((char_u *)features[idx]);
            if (last_col)
            {
                if (msg_col > 0)
                    msg_putchar('\n');
            }
            else
            {
                while (msg_col % width)
                    msg_putchar(' ');
            }
        }
        else
        {
            if (msg_col > 0)
                msg_putchar('\n');
        }
    }
}

    void
list_version()
{
    int         i;
    int         first;
    char        *s = "";

    /*
     * When adding features here, don't forget to update the list of
     * internal variables in eval.c!
     */
    MSG(longVersion);

    /* Print the list of patch numbers if there is at least one. */
    /* Print a range when patches are consecutive: "1-10, 12, 15-40, 42-45" */
    if (included_patches[0] != 0)
    {
        MSG_PUTS(_("\nIncluded patches: "));
        first = -1;
        /* find last one */
        for (i = 0; included_patches[i] != 0; ++i)
            ;
        while (--i >= 0)
        {
            if (first < 0)
                first = included_patches[i];
            if (i == 0 || included_patches[i - 1] != included_patches[i] + 1)
            {
                MSG_PUTS(s);
                s = ", ";
                msg_outnum((long)first);
                if (first != included_patches[i])
                {
                    MSG_PUTS("-");
                    msg_outnum((long)included_patches[i]);
                }
                first = -1;
            }
        }
    }

    /* Print the list of extra patch descriptions if there is at least one. */
    if (extra_patches[0] != NULL)
    {
        MSG_PUTS(_("\nExtra patches: "));
        s = "";
        for (i = 0; extra_patches[i] != NULL; ++i)
        {
            MSG_PUTS(s);
            s = ", ";
            MSG_PUTS(extra_patches[i]);
        }
    }

    MSG_PUTS(_("\nNormal version "));
    MSG_PUTS(_("without GUI."));
    version_msg(_("  Features included (+) or not (-):\n"));

    list_features();

#if defined(SYS_VIMRC_FILE)
    version_msg(_("   system vimrc file: \""));
    version_msg(SYS_VIMRC_FILE);
    version_msg("\"\n");
#endif
#if defined(USR_VIMRC_FILE)
    version_msg(_("     user vimrc file: \""));
    version_msg(USR_VIMRC_FILE);
    version_msg("\"\n");
#endif
#if defined(USR_VIMRC_FILE2)
    version_msg(_(" 2nd user vimrc file: \""));
    version_msg(USR_VIMRC_FILE2);
    version_msg("\"\n");
#endif
#if defined(USR_VIMRC_FILE3)
    version_msg(_(" 3rd user vimrc file: \""));
    version_msg(USR_VIMRC_FILE3);
    version_msg("\"\n");
#endif
#if defined(USR_EXRC_FILE)
    version_msg(_("      user exrc file: \""));
    version_msg(USR_EXRC_FILE);
    version_msg("\"\n");
#endif
#if defined(USR_EXRC_FILE2)
    version_msg(_("  2nd user exrc file: \""));
    version_msg(USR_EXRC_FILE2);
    version_msg("\"\n");
#endif
#if defined(DEBUG)
    version_msg("\n");
    version_msg(_("  DEBUG BUILD"));
#endif
}

/*
 * Output a string for the version message.  If it's going to wrap, output a
 * newline, unless the message is too long to fit on the screen anyway.
 */
    static void
version_msg(s)
    char        *s;
{
    int         len = (int)STRLEN(s);

    if (!got_int && len < (int)Columns && msg_col + len >= (int)Columns
                                                                && *s != '\n')
        msg_putchar('\n');
    if (!got_int)
        MSG_PUTS(s);
}

static void do_intro_line __ARGS((int row, char_u *mesg, int add_version, int attr));

/*
 * Show the intro message when not editing a file.
 */
    void
maybe_intro_message()
{
    if (bufempty()
            && curbuf->b_fname == NULL
#if defined(FEAT_WINDOWS)
            && firstwin->w_next == NULL
#endif
            && vim_strchr(p_shm, SHM_INTRO) == NULL)
        intro_message(FALSE);
}

/*
 * Give an introductory message about Vim.
 * Only used when starting Vim on an empty file, without a file name.
 * Or with the ":intro" command (for Sven :-).
 */
    void
intro_message(colon)
    int         colon;          /* TRUE for ":intro" */
{
    int         i;
    int         row;
    int         blanklines;
    int         sponsor;
    char        *p;
    static char *(lines[]) =
    {
        N_("VIM - Vi IMproved"),
        "",
        N_("version "),
        N_("by Bram Moolenaar et al."),
        N_("Vim is open source and freely distributable"),
        "",
        N_("Help poor children in Uganda!"),
        N_("type  :help iccf<Enter>       for information "),
        "",
        N_("type  :q<Enter>               to exit         "),
        N_("type  :help<Enter>  or  <F1>  for on-line help"),
        N_("type  :help version7<Enter>   for version info"),
        NULL,
        "",
        N_("Running in Vi compatible mode"),
        N_("type  :set nocp<Enter>        for Vim defaults"),
        N_("type  :help cp-default<Enter> for info on this"),
    };

    /* blanklines = screen height - # message lines */
    blanklines = (int)Rows - ((sizeof(lines) / sizeof(char *)) - 1);
    if (!p_cp)
        blanklines += 4;  /* add 4 for not showing "Vi compatible" message */

#if defined(FEAT_WINDOWS)
    /* Don't overwrite a statusline.  Depends on 'cmdheight'. */
    if (p_ls > 1)
        blanklines -= Rows - topframe->fr_height;
#endif
    if (blanklines < 0)
        blanklines = 0;

    /* Show the sponsor and register message one out of four times, the Uganda
     * message two out of four times. */
    sponsor = (int)time(NULL);
    sponsor = ((sponsor & 2) == 0) - ((sponsor & 4) == 0);

    /* start displaying the message lines after half of the blank lines */
    row = blanklines / 2;
    if ((row >= 2 && Columns >= 50) || colon)
    {
        for (i = 0; i < (int)(sizeof(lines) / sizeof(char *)); ++i)
        {
            p = lines[i];
            if (p == NULL)
            {
                if (!p_cp)
                    break;
                continue;
            }
            if (sponsor != 0)
            {
                if (strstr(p, "children") != NULL)
                    p = sponsor < 0
                        ? N_("Sponsor Vim development!")
                        : N_("Become a registered Vim user!");
                else if (strstr(p, "iccf") != NULL)
                    p = sponsor < 0
                        ? N_("type  :help sponsor<Enter>    for information ")
                        : N_("type  :help register<Enter>   for information ");
                else if (strstr(p, "Orphans") != NULL)
                    p = N_("menu  Help->Sponsor/Register  for information    ");
            }
            if (*p != NUL)
                do_intro_line(row, (char_u *)_(p), i == 2, 0);
            ++row;
        }
    }

    /* Make the wait-return message appear just below the text. */
    if (colon)
        msg_row = row;
}

    static void
do_intro_line(row, mesg, add_version, attr)
    int         row;
    char_u      *mesg;
    int         add_version;
    int         attr;
{
    char_u      vers[20];
    int         col;
    char_u      *p;
    int         l;
    int         clen;

    /* Center the message horizontally. */
    col = vim_strsize(mesg);
    if (add_version)
    {
        STRCPY(vers, mediumVersion);
        if (highest_patch())
        {
            /* Check for 9.9x or 9.9xx, alpha/beta version */
            if (isalpha((int)vers[3]))
            {
                int len = (isalpha((int)vers[4])) ? 5 : 4;
                sprintf((char *)vers + len, ".%d%s", highest_patch(),
                                                         mediumVersion + len);
            }
            else
                sprintf((char *)vers + 3, ".%d", highest_patch());
        }
        col += (int)STRLEN(vers);
    }
    col = (Columns - col) / 2;
    if (col < 0)
        col = 0;

    /* Split up in parts to highlight <> items differently. */
    for (p = mesg; *p != NUL; p += l)
    {
        clen = 0;
        for (l = 0; p[l] != NUL
                         && (l == 0 || (p[l] != '<' && p[l - 1] != '>')); ++l)
        {
            if (has_mbyte)
            {
                clen += ptr2cells(p + l);
                l += (*mb_ptr2len)(p + l) - 1;
            }
            else
                clen += byte2cells(p[l]);
        }
        screen_puts_len(p, l, row, col, *p == '<' ? hl_attr(HLF_8) : attr);
        col += clen;
    }

    /* Add the version number to the version line. */
    if (add_version)
        screen_puts(vers, row, col, 0);
}

/*
 * ":intro": clear screen, display intro screen and wait for return.
 */
    void
ex_intro(eap)
    exarg_T     *eap UNUSED;
{
    screenclear();
    intro_message(TRUE);
    wait_return(TRUE);
}
