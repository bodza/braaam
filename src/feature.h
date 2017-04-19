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
 * +folding             Fold lines.
 */
#define FEAT_FOLDING

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
 * +emacs_tags          When FEAT_EMACS_TAGS defined: Include support for
 *                      emacs style TAGS file.
 */
#undef FEAT_EMACS_TAGS

/*
 * +tag_binary          Can use a binary search for the tags file.
 */
#define FEAT_TAG_BINS

/*
 * +tag_old_static      Old style static tags: "file:tag  file  ..".  Slows
 *                      down tag searching a bit.
 */
#define FEAT_TAG_OLDSTATIC

/*
 * +tag_any_white       Allow any white space to separate the fields in a tags
 *                      file.  When not defined, only a TAB is allowed.
 */
/* #define FEAT_TAG_ANYWHITE */

/*
 * +cscope              Unix only: Cscope support.
 */
#if defined(UNIX) && defined(FEAT_UNSURE) && !defined(FEAT_CSCOPE) && !defined(MACOS_X)
#define FEAT_CSCOPE
#endif

/*
 * +eval                Built-in script language and expression evaluation,
 *                      ":let", ":if", etc.
 * +float               Floating point variables.
 */
#define FEAT_EVAL
#if defined(HAVE_FLOAT_FUNCS) || defined(MACOS)
#define FEAT_FLOAT
#endif

/*
 * +reltime             reltime() function
 */
#if defined(FEAT_EVAL) && ((defined(HAVE_GETTIMEOFDAY) && defined(HAVE_SYS_TIME_H)))
#define FEAT_RELTIME
#endif

/*
 * +textobjects         Text objects: "vaw", "das", etc.
 */
#if defined(FEAT_EVAL)
#define FEAT_TEXTOBJ
#endif

/*
 *                      Insert mode completion with 'completefunc'.
 */
#if defined(FEAT_INS_EXPAND) && defined(FEAT_EVAL)
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
#if defined(FEAT_EVAL)
#define FEAT_PRINTER
#endif
#if defined(FEAT_PRINTER) && defined(FEAT_EVAL)
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
 * +diff                Displaying diffs in a nice way.
 *                      Requires +windows and +autocmd.
 */
#if defined(FEAT_WINDOWS) && defined(FEAT_AUTOCMD)
#define FEAT_DIFF
#endif

/*
 * +title               'title' and 'icon' options
 * +statusline          'statusline', 'rulerformat' and special format of
 *                      'titlestring' and 'iconstring' options.
 * +byte_offset         '%o' in 'statusline' and builtin functions line2byte()
 *                      and byte2line().
 *                      Note: Required for Macintosh.
 */
#if (1)
#define FEAT_TITLE
#endif

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
#define FEAT_VIMINFO
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
 * +spell               spell checking
 */
#define FEAT_SPELL

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

#if !defined(NO_BUILTIN_TCAPS) && !defined(FEAT_GUI_W16)
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

/*
 * +cryptv              Encryption (by Mohsin Ahmed <mosh@sasi.com>).
 */
#if !defined(FEAT_CRYPT)
#define FEAT_CRYPT
#endif

/*
 * +mksession           ":mksession" command.
 *                      Requires +windows and +vertsplit.
 */
#if defined(FEAT_WINDOWS) && defined(FEAT_VERTSPLIT)
#define FEAT_SESSION
#endif

/*
 * +multi_lang          Multi language support. ":menutrans", ":language", etc.
 * +gettext             Message translations (requires +multi_lang)
 *                      (only when "lang" archive unpacked)
 */
#define FEAT_MULTI_LANG
#if defined(HAVE_GETTEXT) && defined(FEAT_MULTI_LANG) && (defined(HAVE_LOCALE_H) || defined(X_LOCALE))
#define FEAT_GETTEXT
#endif

/*
 * +multi_byte          Generic multi-byte character handling.  Doesn't work
 *                      with 16 bit ints.  Required for GTK+ 2.
 */
#if !defined(FEAT_MBYTE) && VIM_SIZEOF_INT >= 4
#define FEAT_MBYTE
#endif

/* Define this if you want to use 16 bit Unicode only, reduces memory used for
 * the screen structures. */
/* #define UNICODE16 */

#if defined(FEAT_MBYTE) && VIM_SIZEOF_INT < 4
        Error: Can only handle multi-byte feature with 32 bit int or larger
#endif

/* Use iconv() when it's available. */
#if defined(FEAT_MBYTE) && ((defined(HAVE_ICONV_H) && defined(HAVE_ICONV)) || defined(DYNAMIC_ICONV))
#define USE_ICONV
#endif

/*
 * +xim                 X Input Method.  For entering special languages like
 *                      chinese and Japanese.
 * +hangul_input        Internal Hangul input method.  Must be included
 *                      through configure: "--enable-hangulin"
 * Both are for Unix and VMS only.
 */
#if !defined(FEAT_XIM)
/* #define FEAT_XIM */
#endif

#if defined(FEAT_XIM) && defined(FEAT_GUI_GTK)
#define USE_XIM 1              /* needed for GTK include files */
#endif

#if defined(FEAT_HANGULIN)
#define HANGUL_DEFAULT_KEYBOARD 2      /* 2 or 3 bulsik keyboard */
#define ESC_CHG_TO_ENG_MODE            /* if defined, when ESC pressed,
                                         * turn to english mode
                                         */
#if !defined(FEAT_XFONTSET) && defined(HAVE_X11) && !defined(FEAT_GUI_GTK)
#define FEAT_XFONTSET                 /* Hangul input requires xfontset */
#endif
#if defined(FEAT_XIM) && !defined(LINT)
        Error: You should select only ONE of XIM and HANGUL INPUT
#endif
#endif
#if defined(FEAT_HANGULIN) || defined(FEAT_XIM)
/* # define X_LOCALE */                 /* for OS with incomplete locale
                                           support, like old linux versions. */
/* # define SLOW_XSERVER */             /* for extremely slow X server */
#endif

/*
 * +xfontset            X fontset support.  For outputting wide characters.
 */
#if !defined(FEAT_XFONTSET)
#if defined(FEAT_MBYTE) && defined(HAVE_X11) && !defined(FEAT_GUI_GTK)
#define FEAT_XFONTSET
#else
/* #  define FEAT_XFONTSET */
#endif
#endif

/*
 * +libcall             libcall() function
 */
/* Using dlopen() also requires dlsym() to be available. */
#if defined(HAVE_DLOPEN) && defined(HAVE_DLSYM)
#define USE_DLOPEN
#endif
#if defined(FEAT_EVAL) && (defined(UNIX) && (defined(USE_DLOPEN) || defined(HAVE_SHL_LOAD)))
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
#if defined(FEAT_GUI_W32)
#define FEAT_TEAROFF
#endif

/* There are two ways to use XPM. */
#if (defined(HAVE_XM_XPMP_H) && defined(FEAT_GUI_MOTIF)) || defined(HAVE_X11_XPM_H)
#define HAVE_XPM 1
#endif

/*
 * +toolbar             Include code for a toolbar (for the Win32 GUI, GTK
 *                      always has it).  But only if menus are enabled.
 */
#if defined(FEAT_MENU) && (defined(FEAT_GUI_GTK) || defined(FEAT_GUI_MSWIN) || ((defined(FEAT_GUI_MOTIF) || defined(FEAT_GUI_ATHENA)) && defined(HAVE_XPM)) || defined(FEAT_GUI_PHOTON))
#define FEAT_TOOLBAR
#endif

#if defined(FEAT_TOOLBAR) && !defined(FEAT_MENU)
#define FEAT_MENU
#endif

/*
 * GUI tabline
 */
#if defined(FEAT_WINDOWS) && (defined(FEAT_GUI_GTK) || (defined(FEAT_GUI_MOTIF) && defined(HAVE_XM_NOTEBOOK_H)) || defined(FEAT_GUI_MAC) || (defined(FEAT_GUI_MSWIN) && (!defined(_MSC_VER) || _MSC_VER > 1020)))
#define FEAT_GUI_TABLINE
#endif

/*
 * +browse              ":browse" command.
 *                      or just the ":browse" command modifier
 */
#define FEAT_BROWSE_CMD
#if defined(FEAT_GUI_MSWIN) || defined(FEAT_GUI_MOTIF) || defined(FEAT_GUI_ATHENA) || defined(FEAT_GUI_GTK) || defined(FEAT_GUI_PHOTON) || defined(FEAT_GUI_MAC)
#define FEAT_BROWSE
#endif

/*
 * On some systems, when we compile with the GUI, we always use it.  On Mac
 * there is no terminal version, and on Windows we can't figure out how to
 * fork one off with :gui.
 */
#if defined(FEAT_GUI_MSWIN) || (defined(FEAT_GUI_MAC) && !defined(MACOS_X_UNIX))
#define ALWAYS_USE_GUI
#endif

/*
 * +dialog_gui          Use GUI dialog.
 * +dialog_con          May use Console dialog.
 *                      When none of these defined there is no dialog support.
 */
#if ((defined(FEAT_GUI_ATHENA) || defined(FEAT_GUI_MOTIF)) && defined(HAVE_X11_XPM_H)) || defined(FEAT_GUI_GTK) || defined(FEAT_GUI_PHOTON) || defined(FEAT_GUI_MSWIN) || defined(FEAT_GUI_MAC)
#define FEAT_CON_DIALOG
#define FEAT_GUI_DIALOG
#else
#define FEAT_CON_DIALOG
#endif
#if !defined(FEAT_GUI_DIALOG) && (defined(FEAT_GUI_MOTIF) || defined(FEAT_GUI_ATHENA) || defined(FEAT_GUI_GTK) || defined(FEAT_GUI_W32))
/* need a dialog to show error messages when starting from the desktop */
#define FEAT_GUI_DIALOG
#endif
#if defined(FEAT_GUI_DIALOG) && (defined(FEAT_GUI_MOTIF) || defined(FEAT_GUI_ATHENA) || defined(FEAT_GUI_GTK) || defined(FEAT_GUI_MSWIN) || defined(FEAT_GUI_PHOTON) || defined(FEAT_GUI_MAC))
#define FEAT_GUI_TEXTDIALOG
#if !defined(ALWAYS_USE_GUI)
#define FEAT_CON_DIALOG
#endif
#endif

/* Mac specific thing: Codewarrior interface. */
#if defined(FEAT_GUI_MAC)
#define FEAT_CW_EDITOR
#endif

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
 *                      VMS note: It does work on VMS as well, but because of
 *                      version handling it does not have any purpose.
 *                      Overwrite will write to the new version.
 */
#if (1)
#define FEAT_WRITEBACKUP
#endif

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
 * STARTUPTIME          Time the startup process.  Writes a file with
 *                      timestamps.
 */
#if (defined(HAVE_GETTIMEOFDAY) && defined(HAVE_SYS_TIME_H))
#define STARTUPTIME 1
#endif

/*
 * MEM_PROFILE          Debugging of memory allocation and freeing.
 */
/* #define MEM_PROFILE */

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
 * USR_GVIMRC_FILE      Name of the user .gvimrc file.
 * USR_GVIMRC_FILE2     Name of the alternate user .gvimrc file.
 */
/* #define USR_GVIMRC_FILE      "~/foo/.gvimrc" */
/* #define USR_GVIMRC_FILE2     "~/bar/.gvimrc" */
/* #define USR_GVIMRC_FILE3     "$VIM/.gvimrc" */

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
 * MODIFIED_BY          Name of who modified Vim.  Required when distributing
 *                      a modified version of Vim.
 *                      Also from the "--with-modified-by" configure argument.
 */
/* #define MODIFIED_BY "John Doe" */

/*
 * Machine dependent:
 * ==================
 */

/*
 * +fork                Unix only: fork() support (detected by configure)
 * +system              Use system() instead of fork/exec for starting a
 *                      shell.  Doesn't work for the GUI!
 */
/* #define USE_SYSTEM */

/*
 * +X11                 Unix only.  Include code for xterm title saving and X
 *                      clipboard.  Only works if HAVE_X11 is also defined.
 */
#define WANT_X11

/*
 * XSMP - X11 Session Management Protocol
 * It may be preferred to disable this if the GUI supports it (e.g.,
 * GNOME/KDE) and implement save-yourself etc. through that, but it may also
 * be cleaner to have all SM-aware vims do the same thing (libSM does not
 * depend upon X11).
 * If your GUI wants to support SM itself, change this ifdef.
 * I'm assuming that any X11 implementation will cope with this for now.
 */
#if defined(HAVE_X11) && defined(WANT_X11) && defined(HAVE_X11_SM_SMLIB_H)
#define USE_XSMP
#endif
#if defined(USE_XSMP_INTERACT) && !defined(USE_XSMP)
#undef USE_XSMP_INTERACT
#endif

/*
 * +mouse_xterm         Unix only: Include code for xterm mouse handling.
 * +mouse_dec           idem, for Dec mouse handling.
 * +mouse_jsbterm       idem, for Jsbterm mouse handling.
 * +mouse_netterm       idem, for Netterm mouse handling.
 * (none)               MS-DOS mouse support.
 * +mouse_gpm           Unix only: Include code for Linux console mouse
 *                      handling.
 * +mouse_pterm         PTerm mouse support for QNX
 * +mouse_sgr           Unix only: Include code for for SGR-styled mouse.
 * +mouse_sysmouse      Unix only: Include code for FreeBSD and DragonFly
 *                      console mouse handling.
 * +mouse_urxvt         Unix only: Include code for for urxvt mosue handling.
 * +mouse               Any mouse support (any of the above enabled).
 */
/* OS/2 and Amiga console have no mouse support */
#if (1)
#define FEAT_MOUSE_XTERM
#undef FEAT_MOUSE_NET
#undef FEAT_MOUSE_DEC
#undef FEAT_MOUSE_URXVT
#undef FEAT_MOUSE_SGR
#endif

/*
 * Note: Only one of the following may be defined:
 * FEAT_MOUSE_GPM
 * FEAT_SYSMOUSE
 * FEAT_MOUSE_JSB
 * FEAT_MOUSE_PTERM
 */
#if defined(HAVE_GPM)
#define FEAT_MOUSE_GPM
#endif

#if defined(HAVE_SYSMOUSE)
#define FEAT_SYSMOUSE
#endif

/* urxvt is a small variation of mouse_xterm, and shares its code */
#if defined(FEAT_MOUSE_URXVT) && !defined(FEAT_MOUSE_XTERM)
#define FEAT_MOUSE_XTERM
#endif

/* sgr is a small variation of mouse_xterm, and shares its code */
#if defined(FEAT_MOUSE_SGR) && !defined(FEAT_MOUSE_XTERM)
#define FEAT_MOUSE_XTERM
#endif

/* Define FEAT_MOUSE when any of the above is defined or FEAT_GUI. */
#if !defined(FEAT_MOUSE_TTY) && (defined(FEAT_MOUSE_XTERM) || defined(FEAT_MOUSE_NET) || defined(FEAT_MOUSE_DEC) || defined(DOS_MOUSE) || defined(FEAT_MOUSE_GPM) || defined(FEAT_MOUSE_JSB) || defined(FEAT_MOUSE_PTERM) || defined(FEAT_SYSMOUSE) || defined(FEAT_MOUSE_URXVT) || defined(FEAT_MOUSE_SGR))
#define FEAT_MOUSE_TTY         /* include non-GUI mouse support */
#endif
#if !defined(FEAT_MOUSE) && (defined(FEAT_MOUSE_TTY) || defined(FEAT_GUI))
#define FEAT_MOUSE             /* include generic mouse support */
#endif

/*
 * +clipboard           Clipboard support.  Always used for the GUI.
 * +xterm_clipboard     Unix only: Include code for handling the clipboard
 *                      in an xterm like in the GUI.
 */

#if defined(FEAT_CYGWIN_WIN32_CLIPBOARD)
#define FEAT_CLIPBOARD
#endif

#if defined(FEAT_GUI)
#if !defined(FEAT_CLIPBOARD)
#define FEAT_CLIPBOARD
#endif
#endif

#if defined(UNIX) && defined(WANT_X11) && defined(HAVE_X11)
#define FEAT_XCLIPBOARD
#if !defined(FEAT_CLIPBOARD)
#define FEAT_CLIPBOARD
#endif
#endif

/*
 * +dnd         Drag'n'drop support.  Always used for the GTK+ GUI.
 */
#if defined(FEAT_CLIPBOARD) && defined(FEAT_GUI_GTK)
#define FEAT_DND
#endif

#if defined(FEAT_GUI_MSWIN)
#define MSWIN_FIND_REPLACE     /* include code for find/replace dialog */
#define MSWIN_FR_BUFSIZE 256
#endif

#if defined(FEAT_GUI_GTK) || defined(FEAT_GUI_MOTIF) || defined(MSWIN_FIND_REPLACE)
#define FIND_REPLACE_DIALOG 1
#endif

/*
 * +clientserver        Remote control via the remote_send() function
 *                      and the --remote argument
 */
#if defined(FEAT_XCLIPBOARD) && defined(FEAT_EVAL)
#define FEAT_CLIENTSERVER
#endif

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
/* MS-DOS console and Win32 console can change cursor shape */
#if defined(FEAT_GUI_W32) || defined(FEAT_GUI_W16) || defined(FEAT_GUI_MOTIF) || defined(FEAT_GUI_ATHENA) || defined(FEAT_GUI_GTK) || defined(FEAT_GUI_PHOTON)
#define FEAT_MOUSESHAPE
#endif

/* GUI and some consoles can change the shape of the cursor.  The code is also
 * needed for the 'mouseshape' and 'concealcursor' options. */
#if defined(FEAT_GUI) || defined(MCH_CURSOR_SHAPE) || defined(FEAT_MOUSESHAPE) || defined(FEAT_CONCEAL) || defined(UNIX)
#define CURSOR_SHAPE
#endif

#if defined(FEAT_MZSCHEME) && (defined(FEAT_GUI_W32) || defined(FEAT_GUI_GTK) || defined(FEAT_GUI_MOTIF) || defined(FEAT_GUI_ATHENA) || defined(FEAT_GUI_MAC))
#define MZSCHEME_GUI_THREADS
#endif

/*
 * +ARP                 Amiga only. Use arp.library, DOS 2.0 is not required.
 */
#if !defined(NO_ARP) && !defined(__amigaos4__)
#define FEAT_ARP
#endif

/*
 * +GUI_Athena          To compile Vim with or without the GUI (gvim) you have
 * +GUI_Motif           to edit the Makefile.
 */

/*
 * +ole                 Win32 OLE automation: Use Makefile.ovc.
 */

/*
 * These features can only be included by using a configure argument.  See the
 * Makefile for a line to uncomment.
 * +lua                 Lua interface: "--enable-luainterp"
 * +mzscheme            MzScheme interface: "--enable-mzscheme"
 * +perl                Perl interface: "--enable-perlinterp"
 * +python              Python interface: "--enable-pythoninterp"
 * +tcl                 TCL interface: "--enable-tclinterp"
 * +sniff               Sniff interface: "--enable-sniff"
 * +sun_workshop        Sun Workshop integration
 * +netbeans_intg       Netbeans integration
 */

/*
 * These features are automatically detected:
 * +terminfo
 * +tgetent
 */

/*
 * The Sun Workshop features currently only work with Motif.
 */
#if !defined(FEAT_GUI_MOTIF) && defined(FEAT_SUN_WORKSHOP)
#undef FEAT_SUN_WORKSHOP
#endif

/*
 * The Netbeans feature requires +listcmds and +eval.
 */
#if (!defined(FEAT_LISTCMDS) || !defined(FEAT_EVAL)) && defined(FEAT_NETBEANS_INTG)
#undef FEAT_NETBEANS_INTG
#endif

/*
 * +signs               Allow signs to be displayed to the left of text lines.
 *                      Adds the ":sign" command.
 */
#if defined(FEAT_SUN_WORKSHOP) || defined(FEAT_NETBEANS_INTG)
#define FEAT_SIGNS
#if ((defined(FEAT_GUI_MOTIF) || defined(FEAT_GUI_ATHENA)) && defined(HAVE_X11_XPM_H)) || defined(FEAT_GUI_GTK) || (0)
#define FEAT_SIGN_ICONS
#endif
#endif

/*
 * +balloon_eval        Allow balloon expression evaluation. Used with a
 *                      debugger and for tooltips.
 *                      Only for GUIs where it was implemented.
 */
#if (defined(FEAT_GUI_MOTIF) || defined(FEAT_GUI_ATHENA) || defined(FEAT_GUI_GTK) || defined(FEAT_GUI_W32)) && ( ((defined(FEAT_TOOLBAR) || defined(FEAT_GUI_TABLINE)) && !defined(FEAT_GUI_GTK) && !defined(FEAT_GUI_W32)) || defined(FEAT_SUN_WORKSHOP) || defined(FEAT_NETBEANS_INTG) || defined(FEAT_EVAL))
#define FEAT_BEVAL
#if !defined(FEAT_XFONTSET) && !defined(FEAT_GUI_GTK) && !defined(FEAT_GUI_W32)
#define FEAT_XFONTSET
#endif
#endif

#if defined(FEAT_BEVAL) && (defined(FEAT_GUI_MOTIF) || defined(FEAT_GUI_ATHENA))
#define FEAT_BEVAL_TIP         /* balloon eval used for toolbar tooltip */
#endif

/* both Motif and Athena are X11 and share some code */
#if defined(FEAT_GUI_MOTIF) || defined(FEAT_GUI_ATHENA)
#define FEAT_GUI_X11
#endif

#if defined(FEAT_SUN_WORKSHOP) || defined(FEAT_NETBEANS_INTG)
/*
 * The following features are (currently) only used by Sun Visual WorkShop 6
 * and NetBeans. These features could be used with other integrations with
 * debuggers so I've used separate feature defines.
 */
#if !defined(FEAT_MENU)
#define FEAT_MENU
#endif
#endif

#if defined(FEAT_SUN_WORKSHOP)
/*
 *                      Use an alternative method of X input for a secondary
 *                      command input.
 */
#define ALT_X_INPUT

/*
 * +footer              Motif only: Add a message area at the bottom of the
 *                      main window area.
 */
#define FEAT_FOOTER

#endif

/*
 * +autochdir           'autochdir' option.
 */
#if defined(FEAT_SUN_WORKSHOP) || defined(FEAT_NETBEANS_INTG)
#define FEAT_AUTOCHDIR
#endif

/*
 * +persistent_undo     'undofile', 'undodir' options, :wundo and :rundo, and
 * implementation.
 */
#define FEAT_PERSISTENT_UNDO

/*
 * +filterpipe
 */
#if (defined(UNIX) && !defined(USE_SYSTEM)) || (0)
#define FEAT_FILTERPIPE
#endif
