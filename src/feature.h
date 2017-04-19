/*
 * feature.h: Defines for optional code and preferences
 *
 * Edit this file to include/exclude parts of Vim, before compiling.
 * The only other file that may be edited is Makefile, it contains machine
 * specific options.
 *
 * To include specific options, change the "#if*" and "#endif" into comments,
 * or uncomment the "#define".
 * To exclude specific options, change the "#define" into a comment.
 */

/*
 * When adding a new feature:
 * - Add a #define below.
 * - Add a message in the table above ex_version().
 * - Add a string to f_has().
 * - Add a feature to ":help feature-list" in doc/eval.txt.
 * - Add feature to ":help +feature-list" in doc/various.txt.
 * - Add comment for the documentation of commands that use the feature.
 */

/*
 * Optional code (see ":help +feature-list")
 * =============
 */

/*
 * +windows             Multiple windows.  Without this there is no help
 *                      window and no status lines.
 */
#define FEAT_WINDOWS

/*
 * +listcmds            Vim commands for the buffer list and the argument
 *                      list.  Without this there is no ":buffer" ":bnext",
 *                      ":bdel", ":argdelete", etc.
 */
#define FEAT_LISTCMDS

/*
 * +vertsplit           Vertically split windows.
 */
#define FEAT_VERTSPLIT
#if defined(FEAT_VERTSPLIT) && !defined(FEAT_WINDOWS)
#define FEAT_WINDOWS
#endif

/*
 * +cmdhist             Command line history.
 */
#define FEAT_CMDHIST

/*
 * Message history is fixed at 200 message, 20 for the tiny version.
 */
#define MAX_MSG_HIST_LEN 200

/*
 * +jumplist            Jumplist, CTRL-O and CTRL-I commands.
 */
#define FEAT_JUMPLIST

/* the cmdline-window requires FEAT_VERTSPLIT and FEAT_CMDHIST */
#if defined(FEAT_VERTSPLIT) && defined(FEAT_CMDHIST)
#define FEAT_CMDWIN
#endif

/*
 * +digraphs            Digraphs.
 *                      In insert mode and on the command line you will be
 *                      able to use digraphs. The CTRL-K command will work.
 *                      RFC 1345.
 */
#define FEAT_DIGRAPHS

/*
 * +langmap             'langmap' option.  Only useful when you put your
 *                      keyboard in a special language mode, e.g. for typing
 *                      greek.
 */
#undef FEAT_LANGMAP

/*
 * +keymap              'keymap' option.  Allows you to map typed keys in
 *                      Insert mode for a special language.
 */
#undef FEAT_KEYMAP

/*
 * +localmap            Mappings and abbreviations local to a buffer.
 */
#define FEAT_LOCALMAP

/*
 * +insert_expand       CTRL-N/CTRL-P/CTRL-X in insert mode. Takes about
 *                      4Kbyte of code.
 */
#define FEAT_INS_EXPAND

/*
 * +cmdline_compl       completion of mappings/abbreviations in cmdline mode.
 *                      Takes a few Kbyte of code.
 */
#define FEAT_CMDL_COMPL

#define VIM_BACKTICK           /* internal backtick expansion */

/*
 * +visual              Visual mode - now always included.
 * +visualextra         Extra features for Visual mode (mostly block operators).
 */
#define FEAT_VISUALEXTRA

/*
 * +virtualedit         'virtualedit' option and its implementation
 */
#define FEAT_VIRTUALEDIT

/*
 * +vreplace            "gR" and "gr" commands.
 */
#define FEAT_VREPLACE

/*
 * +cmdline_info        'showcmd' and 'ruler' options.
 */
#define FEAT_CMDL_INFO

/*
 * +linebreak           'showbreak', 'breakat'  and 'linebreak' options.
 *                      Also 'numberwidth'.
 */
#define FEAT_LINEBREAK

/*
 * +ex_extra            ":retab", ":right", ":left", ":center", ":normal".
 */
#define FEAT_EX_EXTRA

/*
 * +extra_search        'hlsearch' and 'incsearch' options.
 */
#define FEAT_SEARCH_EXTRA

/*
 * +quickfix            Quickfix commands.
 */
#define FEAT_QUICKFIX

/*
 * +file_in_path        "gf" and "<cfile>" commands.
 */
#define FEAT_SEARCHPATH

/*
 * +find_in_path        "[I" ":isearch" "^W^I", ":checkpath", etc.
 */
#if defined(FEAT_SEARCHPATH) /* FEAT_SEARCHPATH is required */
#define FEAT_FIND_ID
#endif

/*
 * +path_extra          up/downwards searching in 'path' and 'tags'.
 */
#define FEAT_PATH_EXTRA

/*
 * +rightleft           Right-to-left editing/typing support.
 */
#undef FEAT_RIGHTLEFT

/*
 * +tag_binary          Can use a binary search for the tags file.
 */
#define FEAT_TAG_BINS

/*
 * +eval                Built-in script language and expression evaluation,
 *                      ":let", ":if", etc.
 * +float               Floating point variables.
 */
#if defined(HAVE_FLOAT_FUNCS)
#define FEAT_FLOAT
#endif

/*
 * +reltime             reltime() function
 */
#if ((defined(HAVE_GETTIMEOFDAY) && defined(HAVE_SYS_TIME_H)))
#define FEAT_RELTIME
#endif

/*
 * +textobjects         Text objects: "vaw", "das", etc.
 */
#define FEAT_TEXTOBJ

/*
 *                      Insert mode completion with 'completefunc'.
 */
#if defined(FEAT_INS_EXPAND)
#define FEAT_COMPL_FUNC
#endif

/*
 * +user_commands       Allow the user to define his own commands.
 */
#define FEAT_USR_CMDS

/*
 * +printer             ":hardcopy" command
 * +postscript          Printing uses PostScript file output.
 */
#define FEAT_PRINTER
#if defined(FEAT_PRINTER)
#define FEAT_POSTSCRIPT
#endif

/*
 * +modify_fname        modifiers for file name.  E.g., "%:p:h".
 */
#define FEAT_MODIFY_FNAME

/*
 * +autocmd             ":autocmd" command
 */
#define FEAT_AUTOCMD

/*
 * +title               'title' and 'icon' options
 * +statusline          'statusline', 'rulerformat' and special format of
 *                      'titlestring' and 'iconstring' options.
 * +byte_offset         '%o' in 'statusline' and builtin functions line2byte()
 *                      and byte2line().
 *                      Note: Required for Macintosh.
 */
#define FEAT_TITLE

#define FEAT_STL_OPT
#if !defined(FEAT_CMDL_INFO)
#define FEAT_CMDL_INFO        /* 'ruler' is required for 'statusline' */
#endif

#define FEAT_BYTEOFF

/*
 * +wildignore          'wildignore' and 'backupskip' options
 *                      Needed for Unix to make "crontab -e" work.
 */
#define FEAT_WILDIGN

/*
 * +wildmenu            'wildmenu' option
 */
#if defined(FEAT_WINDOWS)
#define FEAT_WILDMENU
#endif

/*
 * +viminfo             reading/writing the viminfo file. Takes about 8Kbyte
 *                      of code.
 * VIMINFO_FILE         Location of user .viminfo file (should start with $).
 * VIMINFO_FILE2        Location of alternate user .viminfo file.
 */
/* #define VIMINFO_FILE "$HOME/foo/.viminfo" */
/* #define VIMINFO_FILE2 "~/bar/.viminfo" */

/*
 * +syntax              syntax highlighting.  When using this, it's a good
 *                      idea to have +autocmd and +eval too.
 */
#define FEAT_SYN_HL

/*
 * +conceal             'conceal' option.  Needs syntax highlighting
 *                      as this is how the concealed text is defined.
 */
#if defined(FEAT_UNSURE) && defined(FEAT_SYN_HL)
#define FEAT_CONCEAL
#endif

/*
 * +builtin_terms       Choose one out of the following four:
 *
 * NO_BUILTIN_TCAPS     Do not include any builtin termcap entries (used only
 *                      with HAVE_TGETENT defined).
 *
 * (nothing)            Machine specific termcap entries will be included.
 *                      This is default for win16 to save static data.
 *
 * SOME_BUILTIN_TCAPS   Include most useful builtin termcap entries (used only
 *                      with NO_BUILTIN_TCAPS not defined).
 *                      This is the default.
 *
 * ALL_BUILTIN_TCAPS    Include all builtin termcap entries
 *                      (used only with NO_BUILTIN_TCAPS not defined).
 */
#if defined(HAVE_TGETENT)
/* #define NO_BUILTIN_TCAPS */
#endif

#if !defined(NO_BUILTIN_TCAPS)
#if defined(FEAT_UNSURE)
#define ALL_BUILTIN_TCAPS
#else
#define SOME_BUILTIN_TCAPS            /* default */
#endif
#endif

/*
 * +lispindent          lisp indenting (From Eric Fischer).
 * +cindent             C code indenting (From Eric Fischer).
 * +smartindent         smart C code indenting when the 'si' option is set.
 *
 * These two need to be defined when making prototypes.
 */
#define FEAT_LISP

#define FEAT_CINDENT

#define FEAT_SMARTINDENT

/*
 * +comments            'comments' option.
 */
#define FEAT_COMMENTS

/* Define this if you want to use 16 bit Unicode only, reduces memory used for
 * the screen structures. */
/* #define UNICODE16 */

/* Use iconv() when it's available. */
#if ((defined(HAVE_ICONV_H) && defined(HAVE_ICONV)) || defined(DYNAMIC_ICONV))
#define USE_ICONV
#endif

/*
 * +libcall             libcall() function
 */
/* Using dlopen() also requires dlsym() to be available. */
#if defined(HAVE_DLOPEN) && defined(HAVE_DLSYM)
#define USE_DLOPEN
#endif
#if defined(USE_DLOPEN)
#define FEAT_LIBCALL
#endif

/*
 * +scrollbind          synchronization of split windows
 */
#if defined(FEAT_WINDOWS)
#define FEAT_SCROLLBIND
#endif

/*
 * +cursorbind          synchronization of split windows
 */
#if defined(FEAT_WINDOWS)
#define FEAT_CURSORBIND
#endif

/*
 * +menu                ":menu" command
 */
#define FEAT_MENU

/*
 * +toolbar             Include code for a toolbar (for the Win32 GUI, GTK
 *                      always has it).  But only if menus are enabled.
 */
#if defined(FEAT_TOOLBAR) && !defined(FEAT_MENU)
#define FEAT_MENU
#endif

/*
 * On some systems, when we compile with the GUI, we always use it.  On Mac
 * there is no terminal version, and on Windows we can't figure out how to
 * fork one off with :gui.
 */

/*
 * +dialog_gui          Use GUI dialog.
 * +dialog_con          May use Console dialog.
 *                      When none of these defined there is no dialog support.
 */
#define FEAT_CON_DIALOG

/* Mac specific thing: Codewarrior interface. */

/*
 * Preferences:
 * ============
 */

/*
 * +writebackup         'writebackup' is default on:
 *                      Use a backup file while overwriting a file.  But it's
 *                      deleted again when 'backup' is not set.  Changing this
 *                      is strongly discouraged: You can lose all your
 *                      changes when the computer crashes while writing the
 *                      file.
 */
#define FEAT_WRITEBACKUP

/*
 * +xterm_save          The t_ti and t_te entries for the builtin xterm will
 *                      be set to save the screen when starting Vim and
 *                      restoring it when exiting.
 */
/* #define FEAT_XTERM_SAVE */

/*
 * DEBUG                Output a lot of debugging garbage.
 */
/* #define DEBUG */

/*
 * VIMRC_FILE           Name of the .vimrc file in current dir.
 */
/* #define VIMRC_FILE   ".vimrc" */

/*
 * EXRC_FILE            Name of the .exrc file in current dir.
 */
/* #define EXRC_FILE    ".exrc" */

/*
 * GVIMRC_FILE          Name of the .gvimrc file in current dir.
 */
/* #define GVIMRC_FILE  ".gvimrc" */

/*
 * SESSION_FILE         Name of the default ":mksession" file.
 */
#define SESSION_FILE    "Session.vim"

/*
 * USR_VIMRC_FILE       Name of the user .vimrc file.
 * USR_VIMRC_FILE2      Name of alternate user .vimrc file.
 * USR_VIMRC_FILE3      Name of alternate user .vimrc file.
 */
/* #define USR_VIMRC_FILE       "~/foo/.vimrc" */
/* #define USR_VIMRC_FILE2      "~/bar/.vimrc" */
/* #define USR_VIMRC_FILE3      "$VIM/.vimrc" */

/*
 * EVIM_FILE            Name of the evim.vim script file
 */
/* #define EVIM_FILE            "$VIMRUNTIME/evim.vim" */

/*
 * USR_EXRC_FILE        Name of the user .exrc file.
 * USR_EXRC_FILE2       Name of the alternate user .exrc file.
 */
/* #define USR_EXRC_FILE        "~/foo/.exrc" */
/* #define USR_EXRC_FILE2       "~/bar/.exrc" */

/*
 * SYS_VIMRC_FILE       Name of the system-wide .vimrc file.
 */
/* #define SYS_VIMRC_FILE       "/etc/vimrc" */

/*
 * SYS_GVIMRC_FILE      Name of the system-wide .gvimrc file.
 */
/* #define SYS_GVIMRC_FILE      "/etc/gvimrc" */

/*
 * DFLT_HELPFILE        Name of the help file.
 */
/* # define DFLT_HELPFILE       "$VIMRUNTIME/doc/help.txt.gz" */

/*
 * File names for:
 * FILETYPE_FILE        switch on file type detection
 * FTPLUGIN_FILE        switch on loading filetype plugin files
 * INDENT_FILE          switch on loading indent files
 * FTOFF_FILE           switch off file type detection
 * FTPLUGOF_FILE        switch off loading settings files
 * INDOFF_FILE          switch off loading indent files
 */
/* # define FILETYPE_FILE       "filetype.vim" */
/* # define FTPLUGIN_FILE       "ftplugin.vim" */
/* # define INDENT_FILE         "indent.vim" */
/* # define FTOFF_FILE          "ftoff.vim" */
/* # define FTPLUGOF_FILE       "ftplugof.vim" */
/* # define INDOFF_FILE         "indoff.vim" */

/*
 * SYS_MENU_FILE        Name of the default menu.vim file.
 */
/* # define SYS_MENU_FILE       "$VIMRUNTIME/menu.vim" */

/*
 * SYS_OPTWIN_FILE      Name of the default optwin.vim file.
 */
#if !defined(SYS_OPTWIN_FILE)
#define SYS_OPTWIN_FILE        "$VIMRUNTIME/optwin.vim"
#endif

/*
 * SYNTAX_FNAME         Name of a syntax file, where %s is the syntax name.
 */
/* #define SYNTAX_FNAME "/foo/%s.vim" */

/*
 * RUNTIME_DIRNAME      Generic name for the directory of the runtime files.
 */
#if !defined(RUNTIME_DIRNAME)
#define RUNTIME_DIRNAME "runtime"
#endif

/*
 * RUNTIME_GLOBAL       Directory name for global Vim runtime directory.
 *                      Don't define this if the preprocessor can't handle
 *                      string concatenation.
 *                      Also set by "--with-global-runtime" configure argument.
 */
/* #define RUNTIME_GLOBAL "/etc/vim" */

/*
 * Machine dependent:
 * ==================
 */

/*
 * +mouse_xterm         Unix only: Include code for xterm mouse handling.
 * +mouse_gpm           Unix only: Include code for Linux console mouse handling.
 * +mouse_sysmouse      Unix only: Include code for FreeBSD and DragonFly console mouse handling.
 * +mouse               Any mouse support (any of the above enabled).
 */
#define FEAT_MOUSE_XTERM

/*
 * Note: Only one of the following may be defined:
 * FEAT_MOUSE_GPM
 * FEAT_SYSMOUSE
 */
#if defined(HAVE_GPM)
#define FEAT_MOUSE_GPM
#endif

#if defined(HAVE_SYSMOUSE)
#define FEAT_SYSMOUSE
#endif

/* urxvt is a small variation of mouse_xterm, and shares its code */

/* sgr is a small variation of mouse_xterm, and shares its code */

/* Define FEAT_MOUSE when any of the above is defined or FEAT_GUI. */
#if !defined(FEAT_MOUSE_TTY) && (defined(FEAT_MOUSE_XTERM) || defined(FEAT_MOUSE_GPM) || defined(FEAT_SYSMOUSE))
#define FEAT_MOUSE_TTY         /* include non-GUI mouse support */
#endif
#if !defined(FEAT_MOUSE) && defined(FEAT_MOUSE_TTY)
#define FEAT_MOUSE             /* include generic mouse support */
#endif

/*
 * +clipboard           Clipboard support.  Always used for the GUI.
 * +xterm_clipboard     Unix only: Include code for handling the clipboard
 *                      in an xterm like in the GUI.
 */

/*
 * +clientserver        Remote control via the remote_send() function
 *                      and the --remote argument
 */

/*
 * +termresponse        send t_RV to obtain terminal response.  Used for xterm
 *                      to check if mouse dragging can be used and if term
 *                      codes can be obtained.
 */
#if defined(HAVE_TGETENT)
#define FEAT_TERMRESPONSE
#endif

/*
 * cursor shape         Adjust the shape of the cursor to the mode.
 * mouse shape          Adjust the shape of the mouse pointer to the mode.
 */

/* GUI and some consoles can change the shape of the cursor.  The code is also
 * needed for the 'mouseshape' and 'concealcursor' options. */
#define CURSOR_SHAPE

/*
 * These features are automatically detected:
 * +terminfo
 * +tgetent
 */

/*
 * +signs               Allow signs to be displayed to the left of text lines.
 *                      Adds the ":sign" command.
 */

/*
 * +autochdir           'autochdir' option.
 */

/*
 * +persistent_undo     'undofile', 'undodir' options, :wundo and :rundo, and
 * implementation.
 */
#define FEAT_PERSISTENT_UNDO

/*
 * +filterpipe
 */
#define FEAT_FILTERPIPE
