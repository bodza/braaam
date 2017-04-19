#if !defined(VIM__H)
#define VIM__H

/* ============ the header file puzzle (ca. 50-100 pieces) ========= */

#if defined(HAVE_CONFIG_H) /* GNU autoconf (or something else) was here */

/* Define when terminfo support found */
#define TERMINFO 1

/* Define when termcap.h contains ospeed */
#define HAVE_OSPEED 1

/* Define when termcap.h contains UP, BC and PC */
#define HAVE_UP_BC_PC 1

/* Define when __DATE__ " " __TIME__ can be used */
#define HAVE_DATE_TIME 1

/* Define when __attribute__((unused)) can be used */
#define HAVE_ATTRIBUTE_UNUSED 1

/* Defined to the size of an int */
#define VIM_SIZEOF_INT 4

/* Defined to the size of a long */
#define VIM_SIZEOF_LONG 8

/* Defined to the size of off_t */
#define SIZEOF_OFF_T 8

/* Defined to the size of time_t */
#define SIZEOF_TIME_T 8

/*
 * If we cannot trust one of the following from the libraries, we use our
 * own safe but probably slower vim_memmove().
 */
#define USEMEMMOVE 1

/* Define to empty if the keyword does not work.  */
/* #undef const */

/* Define to empty if the keyword does not work.  */
/* #undef volatile */

/* Define to `int' if <sys/types.h> doesn't define.  */
/* #undef mode_t */

/* Define to `long' if <sys/types.h> doesn't define.  */
/* #undef off_t */

/* Define to `long' if <sys/types.h> doesn't define.  */
/* #undef pid_t */

/* Define to `unsigned' if <sys/types.h> doesn't define.  */
/* #undef size_t */

/* Define to `int' if <sys/types.h> doesn't define.  */
/* #undef uid_t */

/* Define to `unsigned int' or other type that is 32 bit.  */
/* #undef uint32_t */

/* Define to `int' if <sys/types.h> doesn't define.  */
/* #undef gid_t */

/* Define to `long' if <sys/types.h> doesn't define.  */
/* #undef ino_t */

/* Define to `unsigned' if <sys/types.h> doesn't define.  */
/* #undef dev_t */

/* Define to `unsigned long' if <sys/types.h> doesn't define.  */
/* #undef rlim_t */

/* Define to `struct sigaltstack' if <signal.h> doesn't define.  */
/* #undef stack_t */

/* Define if you can safely include both <sys/time.h> and <time.h>.  */
#define TIME_WITH_SYS_TIME 1

/* Define if you can safely include both <sys/time.h> and <sys/select.h>.  */
#define SYS_SELECT_WITH_SYS_TIME 1

/* Define as the return type of signal handlers (int or void).  */
#define RETSIGTYPE void

/* Define as the command at the end of signal handlers ("" or "return 0;").  */
#define SIGRETURN return

/* Define if struct sigcontext is present */
#define HAVE_SIGCONTEXT 1

/* Define if tgetent() returns zero for an error */
#define TGETENT_ZERO_ERR 0

/* Define if you the function: */
#define HAVE_BCMP 1
#define HAVE_FCHDIR 1
#define HAVE_FCHOWN 1
#define HAVE_FSEEKO 1
#define HAVE_FSYNC 1
#define HAVE_GETCWD 1
#define HAVE_GETPWENT 1
#define HAVE_GETPWNAM 1
#define HAVE_GETPWUID 1
#define HAVE_GETRLIMIT 1
#define HAVE_GETTIMEOFDAY 1
#define HAVE_GETWD 1
#define HAVE_ICONV 1
#define HAVE_NL_LANGINFO_CODESET 1
#define HAVE_LSTAT 1
#define HAVE_MEMCMP 1
#define HAVE_MEMSET 1
#define HAVE_MKDTEMP 1
#define HAVE_NANOSLEEP 1
#define HAVE_OPENDIR 1
#define HAVE_PUTENV 1
#define HAVE_QSORT 1
#define HAVE_READLINK 1
#define HAVE_RENAME 1
#define HAVE_SELECT 1
#define HAVE_SETENV 1
#define HAVE_SETPGID 1
#define HAVE_SETSID 1
#define HAVE_SIGACTION 1
#define HAVE_SIGALTSTACK 1
#define HAVE_SIGSET 1
#define HAVE_SIGSTACK 1
#define HAVE_SIGVEC 1
#define HAVE_STRCASECMP 1
#define HAVE_STRERROR 1
#define HAVE_STRFTIME 1
#define HAVE_STRNCASECMP 1
#define HAVE_STRPBRK 1
#define HAVE_STRTOL 1
#define HAVE_ST_BLKSIZE 1
#define HAVE_SYSCONF 1
#define HAVE_SYSINFO 1
#define HAVE_SYSINFO_MEM_UNIT 1
#define HAVE_TGETENT 1
#define HAVE_TOWLOWER 1
#define HAVE_TOWUPPER 1
#define HAVE_ISWUPPER 1
#define HAVE_USLEEP 1
#define HAVE_UTIME 1

/* Define if you do not have utime(), but do have the utimes() function. */
#define HAVE_UTIMES 1

/* Define if you have the header file: */
#define HAVE_DIRENT_H 1
#define HAVE_ERRNO_H 1
#define HAVE_FCNTL_H 1
#define HAVE_ICONV_H 1
#define HAVE_INTTYPES_H 1
#define HAVE_LANGINFO_H 1
#define HAVE_LIBGEN_H 1
#define HAVE_LIBINTL_H 1
#define HAVE_LOCALE_H 1
#define HAVE_MATH_H 1
#define HAVE_POLL_H 1
#define HAVE_PWD_H 1
#define HAVE_SETJMP_H 1
#define HAVE_SGTTY_H 1
#define HAVE_STDINT_H 1
#define HAVE_STRINGS_H 1
#define HAVE_SYS_IOCTL_H 1
#define HAVE_SYS_PARAM_H 1
#define HAVE_SYS_POLL_H 1
#define HAVE_SYS_RESOURCE_H 1
#define HAVE_SYS_SELECT_H 1
#define HAVE_SYS_STATFS_H 1
#define HAVE_SYS_SYSCTL_H 1
#define HAVE_SYS_SYSINFO_H 1
#define HAVE_SYS_TIME_H 1
#define HAVE_SYS_TYPES_H 1
#define HAVE_SYS_UTSNAME_H 1
#define HAVE_TERMCAP_H 1
#define HAVE_TERMIOS_H 1
#define HAVE_TERMIO_H 1
#define HAVE_WCHAR_H 1
#define HAVE_WCTYPE_H 1
#define HAVE_UNISTD_H 1
#define HAVE_UTIME_H 1

/* Define if you have <sys/wait.h> that is POSIX.1 compatible.  */
#define HAVE_SYS_WAIT_H 1

/* instead, we check a few STDC things ourselves */
#define HAVE_STDARG_H 1
#define HAVE_STDLIB_H 1
#define HAVE_STRING_H 1

/* Define if you want to add support of GPM (Linux console mouse daemon) */
/* #undef HAVE_GPM */

/* Define if you want to add support of sysmouse (*BSD console mouse) */
/* #undef HAVE_SYSMOUSE */

/* Define if fcntl()'s F_SETFD command knows about FD_CLOEXEC */
#define HAVE_FD_CLOEXEC 1

/* We may need to define the uint32_t on non-Unix system, but using the same
 * identifier causes conflicts.  Therefore use UINT32_T. */
#define UINT32_TYPEDEF uint32_t
#endif

#if !defined(UINT32_TYPEDEF)
#if defined(uint32_t) /* this doesn't catch typedefs, unfortunately */
#define UINT32_TYPEDEF uint32_t
#else
  /* Fall back to assuming unsigned int is 32 bit.  If this is wrong then the
   * test in blowfish.c will fail. */
#define UINT32_TYPEDEF unsigned int
#endif
#endif

/* user ID of root is usually zero, but not for everybody */
#define ROOT_UID 0

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
 */

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
#if ((defined(HAVE_ICONV_H) && defined(HAVE_ICONV)))
#define USE_ICONV
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
 * +dialog_con          May use Console dialog.
 */
#define FEAT_CON_DIALOG

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

/* Can't use "PACKAGE" here, conflicts with a Perl include file. */
#if !defined(VIMPACKAGE)
#define VIMPACKAGE     "vim"
#endif

#include "os_unix.h"       /* bring lots of system header files */

#if !defined(__ARGS)
#define __ARGS(x) x
#endif

/* Mark unused function arguments with UNUSED, so that gcc -Wunused-parameter
 * can be used to check for mistakes. */
#if defined(HAVE_ATTRIBUTE_UNUSED)
#define UNUSED __attribute__((unused))
#else
#define UNUSED
#endif

/* if we're compiling in C++ (currently only KVim), the system
 * headers must have the correct prototypes or nothing will build.
 * conversely, our prototypes might clash due to throw() specifiers and
 * cause compilation failures even though the headers are correct.  For
 * a concrete example, gcc-3.2 enforces exception specifications, and
 * glibc-2.2.5 has them in their system headers.
 */
#if !defined(__cplusplus) /* MACOS_X doesn't yet support osdef.h */
#include "osdef.h"     /* bring missing declarations in */
#endif

#if defined(HAVE_LOCALE_H)
#include <locale.h>
#endif

/*
 * Maximum length of a path (for non-unix systems) Make it a bit long, to stay
 * on the safe side.  But not too long to put on the stack.
 */
#if !defined(MAXPATHL)
#if defined(MAXPATHLEN)
#define MAXPATHL  MAXPATHLEN
#else
#define MAXPATHL  256
#endif
#endif
#define PATH_ESC_CHARS ((char_u *)" \t\n*?[{`$\\%#'\"|!<")
#define SHELL_ESC_CHARS ((char_u *)" \t\n*?[{`$\\%#'\"|!<>();&")

#define NUMBUFLEN 30        /* length of a buffer to store a number in ASCII */

/*
 * Shorthand for unsigned variables. Many systems, but not all, have u_char
 * already defined, so we use char_u to avoid trouble.
 */
typedef unsigned char   char_u;
typedef unsigned short  short_u;
typedef unsigned int    int_u;
/* Make sure long_u is big enough to hold a pointer.
 * On Win64, longs are 32 bits and pointers are 64 bits.
 * For printf() and scanf(), we need to take care of long_u specifically. */

  /* Microsoft-specific. The __w64 keyword should be specified on any typedefs
   * that change size between 32-bit and 64-bit platforms.  For any such type,
   * __w64 should appear only on the 32-bit definition of the typedef.
   * Define __w64 as an empty token for everything but MSVC 7.x or later.
   */
#define __w64
typedef unsigned long __w64     long_u;
typedef          long __w64     long_i;
#define SCANF_HEX_LONG_U       "%lx"
#define SCANF_DECIMAL_LONG_U   "%lu"
#define PRINTF_HEX_LONG_U      "0x%lx"

#define PRINTF_DECIMAL_LONG_U SCANF_DECIMAL_LONG_U

/*
 * Only systems which use configure will have SIZEOF_OFF_T and VIM_SIZEOF_LONG
 * defined, which is ok since those are the same systems which can have
 * varying sizes for off_t.  The other systems will continue to use "%ld" to
 * print off_t since off_t is simply a typedef to long for them.
 */
#if defined(SIZEOF_OFF_T) && (SIZEOF_OFF_T > VIM_SIZEOF_LONG)
#define LONG_LONG_OFF_T
#endif

/*
 * The characters and attributes cached for the screen.
 */
typedef char_u schar_T;
#if defined(FEAT_SYN_HL)
typedef unsigned short sattr_T;
#define MAX_TYPENR 65535
#else
typedef unsigned char sattr_T;
#define MAX_TYPENR 255
#endif

/*
 * The u8char_T can hold one decoded UTF-8 character.
 * We normally use 32 bits now, since some Asian characters don't fit in 16
 * bits.  u8char_T is only used for displaying, it could be 16 bits to save
 * memory.
 */
#if defined(UNICODE16)
typedef unsigned short u8char_T;    /* short should be 16 bits */
#else
typedef unsigned int u8char_T;      /* int is 32 bits */
#endif

#include "ascii.h"
#include "keymap.h"
#include "term.h"
#include "macros.h"

#if defined(HAVE_ERRNO_H)
#include <errno.h>
#endif

#include <assert.h>

#if defined(HAVE_STDINT_H)
#include <stdint.h>
#endif
#if defined(HAVE_INTTYPES_H)
#include <inttypes.h>
#endif
#if defined(HAVE_WCTYPE_H)
#include <wctype.h>
#endif
#if defined(HAVE_STDARG_H)
#include <stdarg.h>
#endif

#if defined(HAVE_SYS_SELECT_H) && (!defined(HAVE_SYS_TIME_H) || defined(SYS_SELECT_WITH_SYS_TIME))
#include <sys/select.h>
#endif

#if !defined(HAVE_SELECT)
#if defined(HAVE_SYS_POLL_H)
#include <sys/poll.h>
#define HAVE_POLL
#else
#if defined(HAVE_POLL_H)
#include <poll.h>
#define HAVE_POLL
#endif
#endif
#endif

/* ================ end of the header file puzzle =============== */

/*
 * Check input method control.
 */

/*
 * The _() stuff is for using gettext().  It is a no-op when libintl.h is not
 * found or the +multilang feature is disabled.
 */
#define _(x) ((char *)(x))
#define N_(x) x
#if defined(bindtextdomain)
#undef bindtextdomain
#endif
#define bindtextdomain(x, y) /* empty */
#if defined(bind_textdomain_codeset)
#undef bind_textdomain_codeset
#endif
#define bind_textdomain_codeset(x, y) /* empty */
#if defined(textdomain)
#undef textdomain
#endif
#define textdomain(x) /* empty */

/*
 * flags for update_screen()
 * The higher the value, the higher the priority
 */
#define VALID                   10  /* buffer not changed, or changes marked
                                       with b_mod_* */
#define INVERTED                20  /* redisplay inverted part that changed */
#define INVERTED_ALL            25  /* redisplay whole inverted part */
#define REDRAW_TOP              30  /* display first w_upd_rows screen lines */
#define SOME_VALID              35  /* like NOT_VALID but may scroll */
#define NOT_VALID               40  /* buffer needs complete redraw */
#define CLEAR                   50  /* screen messed up, clear it */

/*
 * Flags for w_valid.
 * These are set when something in a window structure becomes invalid, except
 * when the cursor is moved.  Call check_cursor_moved() before testing one of
 * the flags.
 * These are reset when that thing has been updated and is valid again.
 *
 * Every function that invalidates one of these must call one of the
 * invalidate_* functions.
 *
 * w_valid is supposed to be used only in screen.c.  From other files, use the
 * functions that set or reset the flags.
 *
 * VALID_BOTLINE    VALID_BOTLINE_AP
 *     on               on              w_botline valid
 *     off              on              w_botline approximated
 *     off              off             w_botline not valid
 *     on               off             not possible
 */
#define VALID_WROW      0x01    /* w_wrow (window row) is valid */
#define VALID_WCOL      0x02    /* w_wcol (window col) is valid */
#define VALID_VIRTCOL   0x04    /* w_virtcol (file col) is valid */
#define VALID_CHEIGHT   0x08    /* w_cline_height and w_cline_folded valid */
#define VALID_CROW      0x10    /* w_cline_row is valid */
#define VALID_BOTLINE   0x20    /* w_botine and w_empty_rows are valid */
#define VALID_BOTLINE_AP 0x40   /* w_botine is approximated */
#define VALID_TOPLINE   0x80    /* w_topline is valid (for cursor position) */

/*
 * Terminal highlighting attribute bits.
 * Attributes above HL_ALL are used for syntax highlighting.
 */
#define HL_NORMAL               0x00
#define HL_INVERSE              0x01
#define HL_BOLD                 0x02
#define HL_ITALIC               0x04
#define HL_UNDERLINE            0x08
#define HL_UNDERCURL            0x10
#define HL_STANDOUT             0x20
#define HL_ALL                  0x3f

/* special attribute addition: Put message in history */
#define MSG_HIST                0x1000

/*
 * values for State
 *
 * The lower bits up to 0x20 are used to distinguish normal/visual/op_pending
 * and cmdline/insert+replace mode.  This is used for mapping.  If none of
 * these bits are set, no mapping is done.
 * The upper bits are used to distinguish between other states.
 */
#define NORMAL          0x01    /* Normal mode, command expected */
#define VISUAL          0x02    /* Visual mode - use get_real_state() */
#define OP_PENDING      0x04    /* Normal mode, operator is pending - use
                                   get_real_state() */
#define CMDLINE         0x08    /* Editing command line */
#define INSERT          0x10    /* Insert mode */
#define LANGMAP         0x20    /* Language mapping, can be combined with
                                   INSERT and CMDLINE */

#define REPLACE_FLAG    0x40    /* Replace mode flag */
#define REPLACE         (REPLACE_FLAG + INSERT)
#if defined(FEAT_VREPLACE)
#define VREPLACE_FLAG  0x80    /* Virtual-replace mode flag */
#define VREPLACE       (REPLACE_FLAG + VREPLACE_FLAG + INSERT)
#endif
#define LREPLACE        (REPLACE_FLAG + LANGMAP)

#define NORMAL_BUSY     (0x100 + NORMAL) /* Normal mode, busy with a command */
#define HITRETURN       (0x200 + NORMAL) /* waiting for return or command */
#define ASKMORE         0x300   /* Asking if you want --more-- */
#define SETWSIZE        0x400   /* window size has changed */
#define ABBREV          0x500   /* abbreviation instead of mapping */
#define EXTERNCMD       0x600   /* executing an external command */
#define SHOWMATCH       (0x700 + INSERT) /* show matching paren */
#define CONFIRM         0x800   /* ":confirm" prompt */
#define SELECTMODE      0x1000  /* Select mode, only for mappings */

#define MAP_ALL_MODES   (0x3f | SELECTMODE)     /* all mode bits used for
                                                 * mapping */

/* directions */
#define FORWARD                 1
#define BACKWARD                (-1)
#define FORWARD_FILE            3
#define BACKWARD_FILE           (-3)

/* return values for functions */
#if !(defined(OK) && (OK == 1))
/* OK already defined to 1 in MacOS X curses, skip this */
#define OK                     1
#endif
#define FAIL                    0
#define NOTDONE                 2   /* not OK or FAIL but skipped */

/* flags for b_flags */
#define BF_RECOVERED    0x01    /* buffer has been recovered */
#define BF_CHECK_RO     0x02    /* need to check readonly when loading file
                                   into buffer (set by ":e", may be reset by
                                   ":buf" */
#define BF_NEVERLOADED  0x04    /* file has never been loaded into buffer,
                                   many variables still need to be set */
#define BF_NOTEDITED    0x08    /* Set when file name is changed after
                                   starting to edit, reset when file is
                                   written out. */
#define BF_NEW          0x10    /* file didn't exist when editing started */
#define BF_NEW_W        0x20    /* Warned for BF_NEW and file created */
#define BF_READERR      0x40    /* got errors while reading the file */
#define BF_DUMMY        0x80    /* dummy buffer, only used internally */
#define BF_PRESERVED    0x100   /* ":preserve" was used */

/* Mask to check for flags that prevent normal writing */
#define BF_WRITE_MASK   (BF_NOTEDITED + BF_NEW + BF_READERR)

/*
 * values for xp_context when doing command line completion
 */
#define EXPAND_UNSUCCESSFUL     (-2)
#define EXPAND_OK               (-1)
#define EXPAND_NOTHING          0
#define EXPAND_COMMANDS         1
#define EXPAND_FILES            2
#define EXPAND_DIRECTORIES      3
#define EXPAND_SETTINGS         4
#define EXPAND_BOOL_SETTINGS    5
#define EXPAND_TAGS             6
#define EXPAND_OLD_SETTING      7
#define EXPAND_HELP             8
#define EXPAND_BUFFERS          9
#define EXPAND_EVENTS           10
#define EXPAND_MENUS            11
#define EXPAND_SYNTAX           12
#define EXPAND_HIGHLIGHT        13
#define EXPAND_AUGROUP          14
#define EXPAND_USER_VARS        15
#define EXPAND_MAPPINGS         16
#define EXPAND_TAGS_LISTFILES   17
#define EXPAND_FUNCTIONS        18
#define EXPAND_USER_FUNC        19
#define EXPAND_EXPRESSION       20
#define EXPAND_MENUNAMES        21
#define EXPAND_USER_COMMANDS    22
#define EXPAND_USER_CMD_FLAGS   23
#define EXPAND_USER_NARGS       24
#define EXPAND_USER_COMPLETE    25
#define EXPAND_ENV_VARS         26
#define EXPAND_LANGUAGE         27
#define EXPAND_COLORS           28
#define EXPAND_COMPILER         29
#define EXPAND_USER_DEFINED     30
#define EXPAND_USER_LIST        31
#define EXPAND_SHELLCMD         32
#define EXPAND_CSCOPE           33
#define EXPAND_SIGN             34
#define EXPAND_PROFILE          35
#define EXPAND_BEHAVE           36
#define EXPAND_FILETYPE         37
#define EXPAND_FILES_IN_PATH    38
#define EXPAND_OWNSYNTAX        39
#define EXPAND_LOCALES          40
#define EXPAND_HISTORY          41
#define EXPAND_USER             42
#define EXPAND_SYNTIME          43
#define EXPAND_USER_ADDR_TYPE   44

/* Values for exmode_active (0 is no exmode) */
#define EXMODE_NORMAL           1
#define EXMODE_VIM              2

/* Values for nextwild() and ExpandOne().  See ExpandOne() for meaning. */
#define WILD_FREE               1
#define WILD_EXPAND_FREE        2
#define WILD_EXPAND_KEEP        3
#define WILD_NEXT               4
#define WILD_PREV               5
#define WILD_ALL                6
#define WILD_LONGEST            7
#define WILD_ALL_KEEP           8

#define WILD_LIST_NOTFOUND      0x01
#define WILD_HOME_REPLACE       0x02
#define WILD_USE_NL             0x04
#define WILD_NO_BEEP            0x08
#define WILD_ADD_SLASH          0x10
#define WILD_KEEP_ALL           0x20
#define WILD_SILENT             0x40
#define WILD_ESCAPE             0x80
#define WILD_ICASE              0x100
#define WILD_ALLLINKS           0x200

/* Flags for expand_wildcards() */
#define EW_DIR          0x01    /* include directory names */
#define EW_FILE         0x02    /* include file names */
#define EW_NOTFOUND     0x04    /* include not found names */
#define EW_ADDSLASH     0x08    /* append slash to directory name */
#define EW_KEEPALL      0x10    /* keep all matches */
#define EW_SILENT       0x20    /* don't print "1 returned" from shell */
#define EW_EXEC         0x40    /* executable files */
#define EW_PATH         0x80    /* search in 'path' too */
#define EW_ICASE        0x100   /* ignore case */
#define EW_NOERROR      0x200   /* no error for bad regexp */
#define EW_NOTWILD      0x400   /* add match with literal name if exists */
#define EW_KEEPDOLLAR   0x800   /* do not escape $, $var is expanded */
/* Note: mostly EW_NOTFOUND and EW_SILENT are mutually exclusive: EW_NOTFOUND
 * is used when executing commands and EW_SILENT for interactive expanding. */
#define EW_ALLLINKS     0x1000  /* also links not pointing to existing file */
#define EW_SHELLCMD     0x2000  /* called from expand_shellcmd(), don't check
                                 * if executable is in $PATH */

/* Flags for find_file_*() functions. */
#define FINDFILE_FILE   0       /* only files */
#define FINDFILE_DIR    1       /* only directories */
#define FINDFILE_BOTH   2       /* files and directories */

#if defined(FEAT_VERTSPLIT)
#define W_WINCOL(wp)   (wp->w_wincol)
#define W_WIDTH(wp)    (wp->w_width)
#define W_ENDCOL(wp)   (wp->w_wincol + wp->w_width)
#define W_VSEP_WIDTH(wp) (wp->w_vsep_width)
#else
#define W_WINCOL(wp)   0
#define W_WIDTH(wp)    Columns
#define W_ENDCOL(wp)   Columns
#define W_VSEP_WIDTH(wp) 0
#endif
#if defined(FEAT_WINDOWS)
#define W_STATUS_HEIGHT(wp) (wp->w_status_height)
#define W_WINROW(wp)   (wp->w_winrow)
#else
#define W_STATUS_HEIGHT(wp) 0
#define W_WINROW(wp)   0
#endif

#if defined(NO_EXPANDPATH)
#define gen_expand_wildcards mch_expand_wildcards
#endif

/* Values for the find_pattern_in_path() function args 'type' and 'action': */
#define FIND_ANY        1
#define FIND_DEFINE     2
#define CHECK_PATH      3

#define ACTION_SHOW     1
#define ACTION_GOTO     2
#define ACTION_SPLIT    3
#define ACTION_SHOW_ALL 4
#if defined(FEAT_INS_EXPAND)
#define ACTION_EXPAND  5
#endif

#if defined(FEAT_SYN_HL)
#define SST_MIN_ENTRIES 150    /* minimal size for state stack array */
#define SST_MAX_ENTRIES 1000  /* maximal size for state stack array */
#define SST_FIX_STATES  7      /* size of sst_stack[]. */
#define SST_DIST        16     /* normal distance between entries */
#define SST_INVALID    (synstate_T *)-1        /* invalid syn_state pointer */

#define HL_CONTAINED   0x01    /* not used on toplevel */
#define HL_TRANSP      0x02    /* has no highlighting  */
#define HL_ONELINE     0x04    /* match within one line only */
#define HL_HAS_EOL     0x08    /* end pattern that matches with $ */
#define HL_SYNC_HERE   0x10    /* sync point after this item (syncing only) */
#define HL_SYNC_THERE  0x20    /* sync point at current line (syncing only) */
#define HL_MATCH       0x40    /* use match ID instead of item ID */
#define HL_SKIPNL      0x80    /* nextgroup can skip newlines */
#define HL_SKIPWHITE   0x100   /* nextgroup can skip white space */
#define HL_SKIPEMPTY   0x200   /* nextgroup can skip empty lines */
#define HL_KEEPEND     0x400   /* end match always kept */
#define HL_EXCLUDENL   0x800   /* exclude NL from match */
#define HL_DISPLAY     0x1000  /* only used for displaying, not syncing */
#define HL_FOLD        0x2000  /* define fold */
#define HL_EXTEND      0x4000  /* ignore a keepend */
#define HL_MATCHCONT   0x8000  /* match continued from previous line */
#define HL_TRANS_CONT  0x10000 /* transparent item without contains arg */
#define HL_CONCEAL     0x20000 /* can be concealed */
#define HL_CONCEALENDS 0x40000 /* can be concealed */
#endif

/* Values for 'options' argument in do_search() and searchit() */
#define SEARCH_REV    0x01  /* go in reverse of previous dir. */
#define SEARCH_ECHO   0x02  /* echo the search command and handle options */
#define SEARCH_MSG    0x0c  /* give messages (yes, it's not 0x04) */
#define SEARCH_NFMSG  0x08  /* give all messages except not found */
#define SEARCH_OPT    0x10  /* interpret optional flags */
#define SEARCH_HIS    0x20  /* put search pattern in history */
#define SEARCH_END    0x40  /* put cursor at end of match */
#define SEARCH_NOOF   0x80  /* don't add offset to position */
#define SEARCH_START 0x100  /* start search without col offset */
#define SEARCH_MARK  0x200  /* set previous context mark */
#define SEARCH_KEEP  0x400  /* keep previous search pattern */
#define SEARCH_PEEK  0x800  /* peek for typed char, cancel search */

/* Values for find_ident_under_cursor() */
#define FIND_IDENT      1       /* find identifier (word) */
#define FIND_STRING     2       /* find any string (WORD) */
#define FIND_EVAL       4       /* include "->", "[]" and "." */

/* Values for file_name_in_line() */
#define FNAME_MESS      1       /* give error message */
#define FNAME_EXP       2       /* expand to path */
#define FNAME_HYP       4       /* check for hypertext link */
#define FNAME_INCL      8       /* apply 'includeexpr' */
#define FNAME_REL       16      /* ".." and "./" are relative to the (current)
                                   file instead of the current directory */
#define FNAME_UNESC     32      /* remove backslashes used for escaping */

/* Values for buflist_getfile() */
#define GETF_SETMARK    0x01    /* set pcmark before jumping */
#define GETF_ALT        0x02    /* jumping to alternate file (not buf num) */
#define GETF_SWITCH     0x04    /* respect 'switchbuf' settings when jumping */

/* Values for buflist_new() flags */
#define BLN_CURBUF      1       /* May re-use curbuf for new buffer */
#define BLN_LISTED      2       /* Put new buffer in buffer list */
#define BLN_DUMMY       4       /* Allocating dummy buffer */

/* Values for in_cinkeys() */
#define KEY_OPEN_FORW   0x101
#define KEY_OPEN_BACK   0x102
#define KEY_COMPLETE    0x103   /* end of completion */

/* Values for "noremap" argument of ins_typebuf().  Also used for
 * map->m_noremap and menu->noremap[]. */
#define REMAP_YES       0       /* allow remapping */
#define REMAP_NONE      -1      /* no remapping */
#define REMAP_SCRIPT    -2      /* remap script-local mappings only */
#define REMAP_SKIP      -3      /* no remapping for first char */

/* Values for mch_call_shell() second argument */
#define SHELL_FILTER    1       /* filtering text */
#define SHELL_EXPAND    2       /* expanding wildcards */
#define SHELL_COOKED    4       /* set term to cooked mode */
#define SHELL_DOOUT     8       /* redirecting output */
#define SHELL_SILENT    16      /* don't print error returned by command */
#define SHELL_READ      32      /* read lines and insert into buffer */
#define SHELL_WRITE     64      /* write lines from buffer */

/* Values returned by mch_nodetype() */
#define NODE_NORMAL     0       /* file or directory, check with mch_isdir()*/
#define NODE_WRITABLE   1       /* something we can write to (character
                                   device, fifo, socket, ..) */
#define NODE_OTHER      2       /* non-writable thing (e.g., block device) */

/* Values for readfile() flags */
#define READ_NEW        0x01    /* read a file into a new buffer */
#define READ_FILTER     0x02    /* read filter output */
#define READ_STDIN      0x04    /* read from stdin */
#define READ_BUFFER     0x08    /* read from curbuf (converting stdin) */
#define READ_DUMMY      0x10    /* reading into a dummy buffer */
#define READ_KEEP_UNDO  0x20    /* keep undo info*/

/* Values for change_indent() */
#define INDENT_SET      1       /* set indent */
#define INDENT_INC      2       /* increase indent */
#define INDENT_DEC      3       /* decrease indent */

/* Values for flags argument for findmatchlimit() */
#define FM_BACKWARD     0x01    /* search backwards */
#define FM_FORWARD      0x02    /* search forwards */
#define FM_BLOCKSTOP    0x04    /* stop at start/end of block */
#define FM_SKIPCOMM     0x08    /* skip comments */

/* Values for action argument for do_buffer() */
#define DOBUF_GOTO      0       /* go to specified buffer */
#define DOBUF_SPLIT     1       /* split window and go to specified buffer */
#define DOBUF_UNLOAD    2       /* unload specified buffer(s) */
#define DOBUF_DEL       3       /* delete specified buffer(s) from buflist */
#define DOBUF_WIPE      4       /* delete specified buffer(s) really */

/* Values for start argument for do_buffer() */
#define DOBUF_CURRENT   0       /* "count" buffer from current buffer */
#define DOBUF_FIRST     1       /* "count" buffer from first buffer */
#define DOBUF_LAST      2       /* "count" buffer from last buffer */
#define DOBUF_MOD       3       /* "count" mod. buffer from current buffer */

/* Values for sub_cmd and which_pat argument for search_regcomp() */
/* Also used for which_pat argument for searchit() */
#define RE_SEARCH       0       /* save/use pat in/from search_pattern */
#define RE_SUBST        1       /* save/use pat in/from subst_pattern */
#define RE_BOTH         2       /* save pat in both patterns */
#define RE_LAST         2       /* use last used pattern if "pat" is NULL */

/* Second argument for vim_regcomp(). */
#define RE_MAGIC        1       /* 'magic' option */
#define RE_STRING       2       /* match in string instead of buffer text */
#define RE_STRICT       4       /* don't allow [abc] without ] */
#define RE_AUTO         8       /* automatic engine selection */

#if defined(FEAT_SYN_HL)
/* values for reg_do_extmatch */
#define REX_SET        1       /* to allow \z\(...\), */
#define REX_USE        2       /* to allow \z\1 et al. */
#endif

/* Return values for fullpathcmp() */
/* Note: can use (fullpathcmp() & FPC_SAME) to check for equal files */
#define FPC_SAME        1       /* both exist and are the same file. */
#define FPC_DIFF        2       /* both exist and are different files. */
#define FPC_NOTX        4       /* both don't exist. */
#define FPC_DIFFX       6       /* one of them doesn't exist. */
#define FPC_SAMEX       7       /* both don't exist and file names are same. */

/* flags for do_ecmd() */
#define ECMD_HIDE       0x01    /* don't free the current buffer */
#define ECMD_SET_HELP   0x02    /* set b_help flag of (new) buffer before
                                   opening file */
#define ECMD_OLDBUF     0x04    /* use existing buffer if it exists */
#define ECMD_FORCEIT    0x08    /* ! used in Ex command */
#define ECMD_ADDBUF     0x10    /* don't edit, just add to buffer list */

/* for lnum argument in do_ecmd() */
#define ECMD_LASTL      (linenr_T)0     /* use last position in loaded file */
#define ECMD_LAST       (linenr_T)-1    /* use last position in all files */
#define ECMD_ONE        (linenr_T)1     /* use first line */

/* flags for do_cmdline() */
#define DOCMD_VERBOSE   0x01    /* included command in error message */
#define DOCMD_NOWAIT    0x02    /* don't call wait_return() and friends */
#define DOCMD_REPEAT    0x04    /* repeat exec. until getline() returns NULL */
#define DOCMD_KEYTYPED  0x08    /* don't reset KeyTyped */
#define DOCMD_EXCRESET  0x10    /* reset exception environment (for debugging)*/
#define DOCMD_KEEPLINE  0x20    /* keep typed line for repeating with "." */

/* flags for beginline() */
#define BL_WHITE        1       /* cursor on first non-white in the line */
#define BL_SOL          2       /* use 'sol' option */
#define BL_FIX          4       /* don't leave cursor on a NUL */

/* flags for mf_sync() */
#define MFS_ALL         1       /* also sync blocks with negative numbers */
#define MFS_STOP        2       /* stop syncing when a character is available */
#define MFS_FLUSH       4       /* flushed file to disk */
#define MFS_ZERO        8       /* only write block 0 */

/* flags for buf_copy_options() */
#define BCO_ENTER       1       /* going to enter the buffer */
#define BCO_ALWAYS      2       /* always copy the options */
#define BCO_NOHELP      4       /* don't touch the help related options */

/* flags for do_put() */
#define PUT_FIXINDENT   1       /* make indent look nice */
#define PUT_CURSEND     2       /* leave cursor after end of new text */
#define PUT_CURSLINE    4       /* leave cursor on last line of new text */
#define PUT_LINE        8       /* put register as lines */
#define PUT_LINE_SPLIT  16      /* split line for linewise register */
#define PUT_LINE_FORWARD 32     /* put linewise register below Visual sel. */

/* flags for set_indent() */
#define SIN_CHANGED     1       /* call changed_bytes() when line changed */
#define SIN_INSERT      2       /* insert indent before existing text */
#define SIN_UNDO        4       /* save line for undo before changing it */

/* flags for insertchar() */
#define INSCHAR_FORMAT  1       /* force formatting */
#define INSCHAR_DO_COM  2       /* format comments */
#define INSCHAR_CTRLV   4       /* char typed just after CTRL-V */
#define INSCHAR_NO_FEX  8       /* don't use 'formatexpr' */
#define INSCHAR_COM_LIST 16     /* format comments with list/2nd line indent */

/* flags for open_line() */
#define OPENLINE_DELSPACES  1   /* delete spaces after cursor */
#define OPENLINE_DO_COM     2   /* format comments */
#define OPENLINE_KEEPTRAIL  4   /* keep trailing spaces */
#define OPENLINE_MARKFIX    8   /* fix mark positions */
#define OPENLINE_COM_LIST  16   /* format comments with list/2nd line indent */

/*
 * There are four history tables:
 */
#define HIST_CMD        0       /* colon commands */
#define HIST_SEARCH     1       /* search commands */
#define HIST_EXPR       2       /* expressions (from entering = register) */
#define HIST_INPUT      3       /* input() lines */
#define HIST_DEBUG      4       /* debug commands */
#define HIST_COUNT      5       /* number of history tables */

/*
 * Flags for chartab[].
 */
#define CT_CELL_MASK    0x07    /* mask: nr of display cells (1, 2 or 4) */
#define CT_PRINT_CHAR   0x10    /* flag: set for printable chars */
#define CT_ID_CHAR      0x20    /* flag: set for ID chars */
#define CT_FNAME_CHAR   0x40    /* flag: set for file name chars */

/*
 * Values for do_tag().
 */
#define DT_TAG          1       /* jump to newer position or same tag again */
#define DT_POP          2       /* jump to older position */
#define DT_NEXT         3       /* jump to next match of same tag */
#define DT_PREV         4       /* jump to previous match of same tag */
#define DT_FIRST        5       /* jump to first match of same tag */
#define DT_LAST         6       /* jump to first match of same tag */
#define DT_SELECT       7       /* jump to selection from list */
#define DT_HELP         8       /* like DT_TAG, but no wildcards */
#define DT_JUMP         9       /* jump to new tag or selection from list */
#define DT_CSCOPE       10      /* cscope find command (like tjump) */
#define DT_LTAG         11      /* tag using location list */
#define DT_FREE         99      /* free cached matches */

/*
 * flags for find_tags().
 */
#define TAG_HELP        1       /* only search for help tags */
#define TAG_NAMES       2       /* only return name of tag */
#define TAG_REGEXP      4       /* use tag pattern as regexp */
#define TAG_NOIC        8       /* don't always ignore case */
#define TAG_VERBOSE     32      /* message verbosity */
#define TAG_INS_COMP    64      /* Currently doing insert completion */
#define TAG_KEEP_LANG   128     /* keep current language */

#define TAG_MANY        300     /* When finding many tags (for completion),
                                   find up to this many tags */

/*
 * Types of dialogs passed to do_vim_dialog().
 */
#define VIM_GENERIC     0
#define VIM_ERROR       1
#define VIM_WARNING     2
#define VIM_INFO        3
#define VIM_QUESTION    4
#define VIM_LAST_TYPE   4       /* sentinel value */

/*
 * Return values for functions like gui_yesnocancel()
 */
#define VIM_YES         2
#define VIM_NO          3
#define VIM_CANCEL      4
#define VIM_ALL         5
#define VIM_DISCARDALL  6

/*
 * arguments for win_split()
 */
#define WSP_ROOM        1       /* require enough room */
#define WSP_VERT        2       /* split vertically */
#define WSP_TOP         4       /* window at top-left of shell */
#define WSP_BOT         8       /* window at bottom-right of shell */
#define WSP_HELP        16      /* creating the help window */
#define WSP_BELOW       32      /* put new window below/right */
#define WSP_ABOVE       64      /* put new window above/left */
#define WSP_NEWLOC      128     /* don't copy location list */

/*
 * arguments for gui_set_shellsize()
 */
#define RESIZE_VERT     1       /* resize vertically */
#define RESIZE_HOR      2       /* resize horizontally */
#define RESIZE_BOTH     15      /* resize in both directions */

/*
 * flags for check_changed()
 */
#define CCGD_AW         1       /* do autowrite if buffer was changed */
#define CCGD_MULTWIN    2       /* check also when several wins for the buf */
#define CCGD_FORCEIT    4       /* ! used */
#define CCGD_ALLBUF     8       /* may write all buffers */
#define CCGD_EXCMD      16      /* may suggest using ! */

/*
 * "flags" values for option-setting functions.
 * When OPT_GLOBAL and OPT_LOCAL are both missing, set both local and global
 * values, get local value.
 */
#define OPT_FREE        1       /* free old value if it was allocated */
#define OPT_GLOBAL      2       /* use global value */
#define OPT_LOCAL       4       /* use local value */
#define OPT_MODELINE    8       /* option in modeline */
#define OPT_WINONLY     16      /* only set window-local options */
#define OPT_NOWIN       32      /* don't set window-local options */

/* Magic chars used in confirm dialog strings */
#define DLG_BUTTON_SEP  '\n'
#define DLG_HOTKEY_CHAR '&'

/* Values for "starting" */
#define NO_SCREEN       2       /* no screen updating yet */
#define NO_BUFFERS      1       /* not all buffers loaded yet */
/*                      0          not starting anymore */

/* Values for swap_exists_action: what to do when swap file already exists */
#define SEA_NONE        0       /* don't use dialog */
#define SEA_DIALOG      1       /* use dialog when possible */
#define SEA_QUIT        2       /* quit editing the file */
#define SEA_RECOVER     3       /* recover the file */

/*
 * Minimal size for block 0 of a swap file.
 * NOTE: This depends on size of struct block0! It's not done with a sizeof(),
 * because struct block0 is defined in memline.c (Sorry).
 * The maximal block size is arbitrary.
 */
#define MIN_SWAP_PAGE_SIZE 1048
#define MAX_SWAP_PAGE_SIZE 50000

/* Special values for current_SID. */
#define SID_MODELINE    -1      /* when using a modeline */
#define SID_CMDARG      -2      /* for "--cmd" argument */
#define SID_CARG        -3      /* for "-c" argument */
#define SID_ENV         -4      /* for sourcing environment variable */
#define SID_ERROR       -5      /* option was reset because of an error */
#define SID_NONE        -6      /* don't set scriptID */

/*
 * Events for autocommands.
 */
enum auto_event
{
    EVENT_BUFADD = 0,           /* after adding a buffer to the buffer list */
    EVENT_BUFNEW,               /* after creating any buffer */
    EVENT_BUFDELETE,            /* deleting a buffer from the buffer list */
    EVENT_BUFWIPEOUT,           /* just before really deleting a buffer */
    EVENT_BUFENTER,             /* after entering a buffer */
    EVENT_BUFFILEPOST,          /* after renaming a buffer */
    EVENT_BUFFILEPRE,           /* before renaming a buffer */
    EVENT_BUFLEAVE,             /* before leaving a buffer */
    EVENT_BUFNEWFILE,           /* when creating a buffer for a new file */
    EVENT_BUFREADPOST,          /* after reading a buffer */
    EVENT_BUFREADPRE,           /* before reading a buffer */
    EVENT_BUFREADCMD,           /* read buffer using command */
    EVENT_BUFUNLOAD,            /* just before unloading a buffer */
    EVENT_BUFHIDDEN,            /* just after buffer becomes hidden */
    EVENT_BUFWINENTER,          /* after showing a buffer in a window */
    EVENT_BUFWINLEAVE,          /* just after buffer removed from window */
    EVENT_BUFWRITEPOST,         /* after writing a buffer */
    EVENT_BUFWRITEPRE,          /* before writing a buffer */
    EVENT_BUFWRITECMD,          /* write buffer using command */
    EVENT_CMDWINENTER,          /* after entering the cmdline window */
    EVENT_CMDWINLEAVE,          /* before leaving the cmdline window */
    EVENT_COLORSCHEME,          /* after loading a colorscheme */
    EVENT_COMPLETEDONE,         /* after finishing insert complete */
    EVENT_FILEAPPENDPOST,       /* after appending to a file */
    EVENT_FILEAPPENDPRE,        /* before appending to a file */
    EVENT_FILEAPPENDCMD,        /* append to a file using command */
    EVENT_FILECHANGEDSHELL,     /* after shell command that changed file */
    EVENT_FILECHANGEDSHELLPOST, /* after (not) reloading changed file */
    EVENT_FILECHANGEDRO,        /* before first change to read-only file */
    EVENT_FILEREADPOST,         /* after reading a file */
    EVENT_FILEREADPRE,          /* before reading a file */
    EVENT_FILEREADCMD,          /* read from a file using command */
    EVENT_FILETYPE,             /* new file type detected (user defined) */
    EVENT_FILEWRITEPOST,        /* after writing a file */
    EVENT_FILEWRITEPRE,         /* before writing a file */
    EVENT_FILEWRITECMD,         /* write to a file using command */
    EVENT_FILTERREADPOST,       /* after reading from a filter */
    EVENT_FILTERREADPRE,        /* before reading from a filter */
    EVENT_FILTERWRITEPOST,      /* after writing to a filter */
    EVENT_FILTERWRITEPRE,       /* before writing to a filter */
    EVENT_FOCUSGAINED,          /* got the focus */
    EVENT_FOCUSLOST,            /* lost the focus to another app */
    EVENT_GUIENTER,             /* after starting the GUI */
    EVENT_GUIFAILED,            /* after starting the GUI failed */
    EVENT_INSERTCHANGE,         /* when changing Insert/Replace mode */
    EVENT_INSERTENTER,          /* when entering Insert mode */
    EVENT_INSERTLEAVE,          /* when leaving Insert mode */
    EVENT_MENUPOPUP,            /* just before popup menu is displayed */
    EVENT_QUICKFIXCMDPOST,      /* after :make, :grep etc. */
    EVENT_QUICKFIXCMDPRE,       /* before :make, :grep etc. */
    EVENT_QUITPRE,              /* before :quit */
    EVENT_SESSIONLOADPOST,      /* after loading a session file */
    EVENT_STDINREADPOST,        /* after reading from stdin */
    EVENT_STDINREADPRE,         /* before reading from stdin */
    EVENT_SYNTAX,               /* syntax selected */
    EVENT_TERMCHANGED,          /* after changing 'term' */
    EVENT_TERMRESPONSE,         /* after setting "v:termresponse" */
    EVENT_USER,                 /* user defined autocommand */
    EVENT_VIMENTER,             /* after starting Vim */
    EVENT_VIMLEAVE,             /* before exiting Vim */
    EVENT_VIMLEAVEPRE,          /* before exiting Vim and writing .viminfo */
    EVENT_VIMRESIZED,           /* after Vim window was resized */
    EVENT_WINENTER,             /* after entering a window */
    EVENT_WINLEAVE,             /* before leaving a window */
    EVENT_ENCODINGCHANGED,      /* after changing the 'encoding' option */
    EVENT_INSERTCHARPRE,        /* before inserting a char */
    EVENT_CURSORHOLD,           /* cursor in same position for a while */
    EVENT_CURSORHOLDI,          /* idem, in Insert mode */
    EVENT_FUNCUNDEFINED,        /* if calling a function which doesn't exist */
    EVENT_REMOTEREPLY,          /* upon string reception from a remote vim */
    EVENT_SWAPEXISTS,           /* found existing swap file */
    EVENT_SOURCEPRE,            /* before sourcing a Vim script */
    EVENT_SOURCECMD,            /* sourcing a Vim script using command */
    EVENT_SPELLFILEMISSING,     /* spell file missing */
    EVENT_CURSORMOVED,          /* cursor was moved */
    EVENT_CURSORMOVEDI,         /* cursor was moved in Insert mode */
    EVENT_TABLEAVE,             /* before leaving a tab page */
    EVENT_TABENTER,             /* after entering a tab page */
    EVENT_SHELLCMDPOST,         /* after ":!cmd" */
    EVENT_SHELLFILTERPOST,      /* after ":1,2!cmd", ":w !cmd", ":r !cmd". */
    EVENT_TEXTCHANGED,          /* text was modified */
    EVENT_TEXTCHANGEDI,         /* text was modified in Insert mode*/
    EVENT_CMDUNDEFINED,         /* command undefined */
    NUM_EVENTS                  /* MUST be the last one */
};

typedef enum auto_event event_T;

/*
 * Values for index in highlight_attr[].
 * When making changes, also update HL_FLAGS below!  And update the default
 * value of 'highlight' in option.c.
 */
typedef enum
{
    HLF_8 = 0       /* Meta & special keys listed with ":map", text that is
                       displayed different from what it is */
    , HLF_AT        /* @ and ~ characters at end of screen, characters that
                       don't really exist in the text */
    , HLF_D         /* directories in CTRL-D listing */
    , HLF_E         /* error messages */
    , HLF_H         /* obsolete, ignored */
    , HLF_I         /* incremental search */
    , HLF_L         /* last search string */
    , HLF_M         /* "--More--" message */
    , HLF_CM        /* Mode (e.g., "-- INSERT --") */
    , HLF_N         /* line number for ":number" and ":#" commands */
    , HLF_CLN       /* current line number */
    , HLF_R         /* return to continue message and yes/no questions */
    , HLF_S         /* status lines */
    , HLF_SNC       /* status lines of not-current windows */
    , HLF_C         /* column to separate vertically split windows */
    , HLF_T         /* Titles for output from ":set all", ":autocmd" etc. */
    , HLF_V         /* Visual mode */
    , HLF_VNC       /* Visual mode, autoselecting and not clipboard owner */
    , HLF_W         /* warning messages */
    , HLF_WM        /* Wildmenu highlight */
    , HLF_FL        /* Folded line */
    , HLF_FC        /* Fold column */
    , HLF_ADD       /* Added diff line */
    , HLF_CHD       /* Changed diff line */
    , HLF_DED       /* Deleted diff line */
    , HLF_TXD       /* Text Changed in diff line */
    , HLF_CONCEAL   /* Concealed text */
    , HLF_SC        /* Sign column */
    , HLF_SPB       /* SpellBad */
    , HLF_SPC       /* SpellCap */
    , HLF_SPR       /* SpellRare */
    , HLF_SPL       /* SpellLocal */
    , HLF_PNI       /* popup menu normal item */
    , HLF_PSI       /* popup menu selected item */
    , HLF_PSB       /* popup menu scrollbar */
    , HLF_PST       /* popup menu scrollbar thumb */
    , HLF_TP        /* tabpage line */
    , HLF_TPS       /* tabpage line selected */
    , HLF_TPF       /* tabpage line filler */
    , HLF_CUC       /* 'cursurcolumn' */
    , HLF_CUL       /* 'cursurline' */
    , HLF_MC        /* 'colorcolumn' */
    , HLF_COUNT     /* MUST be the last one */
} hlf_T;

/* The HL_FLAGS must be in the same order as the HLF_ enums!
 * When changing this also adjust the default for 'highlight'. */
#define HL_FLAGS {'8', '@', 'd', 'e', 'h', 'i', 'l', 'm', 'M', \
                  'n', 'N', 'r', 's', 'S', 'c', 't', 'v', 'V', 'w', 'W', \
                  'f', 'F', 'A', 'C', 'D', 'T', '-', '>', \
                  'B', 'P', 'R', 'L', \
                  '+', '=', 'x', 'X', '*', '#', '_', '!', '.', 'o'}

/*
 * Boolean constants
 */
#if !defined(TRUE)
#define FALSE  0           /* note: this is an int, not a long! */
#define TRUE   1
#endif

#define MAYBE   2           /* sometimes used for a variant on TRUE */

#if !defined(UINT32_T)
typedef UINT32_TYPEDEF UINT32_T;
#endif

/*
 * Operator IDs; The order must correspond to opchars[] in ops.c!
 */
#define OP_NOP          0       /* no pending operation */
#define OP_DELETE       1       /* "d"  delete operator */
#define OP_YANK         2       /* "y"  yank operator */
#define OP_CHANGE       3       /* "c"  change operator */
#define OP_LSHIFT       4       /* "<"  left shift operator */
#define OP_RSHIFT       5       /* ">"  right shift operator */
#define OP_FILTER       6       /* "!"  filter operator */
#define OP_TILDE        7       /* "g~" switch case operator */
#define OP_INDENT       8       /* "="  indent operator */
#define OP_FORMAT       9       /* "gq" format operator */
#define OP_COLON        10      /* ":"  colon operator */
#define OP_UPPER        11      /* "gU" make upper case operator */
#define OP_LOWER        12      /* "gu" make lower case operator */
#define OP_JOIN         13      /* "J"  join operator, only for Visual mode */
#define OP_JOIN_NS      14      /* "gJ"  join operator, only for Visual mode */
#define OP_ROT13        15      /* "g?" rot-13 encoding */
#define OP_REPLACE      16      /* "r"  replace chars, only for Visual mode */
#define OP_INSERT       17      /* "I"  Insert column, only for Visual mode */
#define OP_APPEND       18      /* "A"  Append column, only for Visual mode */
#define OP_FOLD         19      /* "zf" define a fold */
#define OP_FOLDOPEN     20      /* "zo" open folds */
#define OP_FOLDOPENREC  21      /* "zO" open folds recursively */
#define OP_FOLDCLOSE    22      /* "zc" close folds */
#define OP_FOLDCLOSEREC 23      /* "zC" close folds recursively */
#define OP_FOLDDEL      24      /* "zd" delete folds */
#define OP_FOLDDELREC   25      /* "zD" delete folds recursively */
#define OP_FORMAT2      26      /* "gw" format operator, keeps cursor pos */
#define OP_FUNCTION     27      /* "g@" call 'operatorfunc' */

/*
 * Motion types, used for operators and for yank/delete registers.
 */
#define MCHAR   0               /* character-wise movement/register */
#define MLINE   1               /* line-wise movement/register */
#define MBLOCK  2               /* block-wise register */

#define MAUTO   0xff            /* Decide between MLINE/MCHAR */

/*
 * Minimum screen size
 */
#define MIN_COLUMNS     12      /* minimal columns for screen */
#define MIN_LINES       2       /* minimal lines for screen */
#define STATUS_HEIGHT   1       /* height of a status line under a window */
#define QF_WINHEIGHT    10      /* default height for quickfix window */

/*
 * Buffer sizes
 */
#if !defined(CMDBUFFSIZE)
#define CMDBUFFSIZE    256     /* size of the command processing buffer */
#endif

#define LSIZE       512         /* max. size of a line in the tags file */

#define IOSIZE     (1024+1)     /* file i/o and sprintf buffer size */

#define DIALOG_MSG_SIZE 1000    /* buffer size for dialog_msg() */

#define MSG_BUF_LEN 480        /* length of buffer for small messages */
#define MSG_BUF_CLEN  (MSG_BUF_LEN / 6)    /* cell length (worst case: utf-8
                                               takes 6 bytes for one cell) */

/* Size of the buffer used for tgetent().  Unfortunately this is largely
 * undocumented, some systems use 1024.  Using a buffer that is too small
 * causes a buffer overrun and a crash.  Use the maximum known value to stay
 * on the safe side. */
#define TBUFSZ 2048             /* buffer size for termcap entry */

/*
 * Maximum length of key sequence to be mapped.
 * Must be able to hold an Amiga resize report.
 */
#define MAXMAPLEN   50

/* Size in bytes of the hash used in the undo file. */
#define UNDO_HASH_SIZE 32

#if defined(HAVE_FCNTL_H)
#include <fcntl.h>
#endif

#if defined(BINARY_FILE_IO)
#define WRITEBIN   "wb"        /* no CR-LF translation */
#define READBIN    "rb"
#define APPENDBIN  "ab"
#else
#define WRITEBIN   "w"
#define READBIN    "r"
#define APPENDBIN  "a"
#endif

/*
 * EMX doesn't have a global way of making open() use binary I/O.
 * Use O_BINARY for all open() calls.
 */
#define O_EXTRA    0

#if !defined(O_NOFOLLOW)
#define O_NOFOLLOW 0
#endif

#if !defined(W_OK)
#define W_OK 2         /* for systems that don't have W_OK in unistd.h */
#endif
#if !defined(R_OK)
#define R_OK 4         /* for systems that don't have R_OK in unistd.h */
#endif

/*
 * defines to avoid typecasts from (char_u *) to (char *) and back
 * (vim_strchr() and vim_strrchr() are now in alloc.c)
 */
#define STRLEN(s)           strlen((char *)(s))
#define STRCPY(d, s)        strcpy((char *)(d), (char *)(s))
#define STRNCPY(d, s, n)    strncpy((char *)(d), (char *)(s), (size_t)(n))
#define STRCMP(d, s)        strcmp((char *)(d), (char *)(s))
#define STRNCMP(d, s, n)    strncmp((char *)(d), (char *)(s), (size_t)(n))
#if defined(HAVE_STRCASECMP)
#define STRICMP(d, s)      strcasecmp((char *)(d), (char *)(s))
#else
#define STRICMP(d, s)     vim_stricmp((char *)(d), (char *)(s))
#endif

/* Like strcpy() but allows overlapped source and destination. */
#define STRMOVE(d, s)       mch_memmove((d), (s), STRLEN(s) + 1)

#if defined(HAVE_STRNCASECMP)
#define STRNICMP(d, s, n)  strncasecmp((char *)(d), (char *)(s), (size_t)(n))
#else
#define STRNICMP(d, s, n) vim_strnicmp((char *)(d), (char *)(s), (size_t)(n))
#endif

/* We need to call mb_stricmp() even when we aren't dealing with a multi-byte
 * encoding because mb_stricmp() takes care of all ascii and non-ascii
 * encodings, including characters with umlauts in latin1, etc., while
 * STRICMP() only handles the system locale version, which often does not
 * handle non-ascii properly. */

#define MB_STRICMP(d, s)       mb_strnicmp((char_u *)(d), (char_u *)(s), (int)MAXCOL)
#define MB_STRNICMP(d, s, n)   mb_strnicmp((char_u *)(d), (char_u *)(s), (int)(n))

#define STRCAT(d, s)        strcat((char *)(d), (char *)(s))
#define STRNCAT(d, s, n)    strncat((char *)(d), (char *)(s), (size_t)(n))

#if defined(HAVE_STRPBRK)
#define vim_strpbrk(s, cs) (char_u *)strpbrk((char *)(s), (char *)(cs))
#endif

#define MSG(s)                      msg((char_u *)(s))
#define MSG_ATTR(s, attr)           msg_attr((char_u *)(s), (attr))
#define EMSG(s)                     emsg((char_u *)(s))
#define EMSG2(s, p)                 emsg2((char_u *)(s), (char_u *)(p))
#define EMSG3(s, p, q)              emsg3((char_u *)(s), (char_u *)(p), (char_u *)(q))
#define EMSGN(s, n)                 emsgn((char_u *)(s), (long)(n))
#define EMSGU(s, n)                 emsgu((char_u *)(s), (long_u)(n))
#define OUT_STR(s)                  out_str((char_u *)(s))
#define OUT_STR_NF(s)               out_str_nf((char_u *)(s))
#define MSG_PUTS(s)                 msg_puts((char_u *)(s))
#define MSG_PUTS_ATTR(s, a)         msg_puts_attr((char_u *)(s), (a))
#define MSG_PUTS_TITLE(s)           msg_puts_title((char_u *)(s))
#define MSG_PUTS_LONG(s)            msg_puts_long_attr((char_u *)(s), 0)
#define MSG_PUTS_LONG_ATTR(s, a)    msg_puts_long_attr((char_u *)(s), (a))

/* Prefer using emsg3(), because perror() may send the output to the wrong
 * destination and mess up the screen. */
#if defined(HAVE_STRERROR)
#define PERROR(msg)                (void)emsg3((char_u *)"%s: %s", (char_u *)msg, (char_u *)strerror(errno))
#else
#define PERROR(msg)                perror(msg)
#endif

typedef long    linenr_T;               /* line number type */
typedef int     colnr_T;                /* column number type */
typedef unsigned short disptick_T;      /* display tick type */

#define MAXLNUM (0x7fffffffL)           /* maximum (invalid) line number */

/*
 * Well, you won't believe it, but some S/390 machines ("host", now also known
 * as zServer) use 31 bit pointers. There are also some newer machines, that
 * use 64 bit pointers. I don't know how to distinguish between 31 and 64 bit
 * machines, so the best way is to assume 31 bits whenever we detect OS/390
 * Unix.
 * With this we restrict the maximum line length to 1073741823. I guess this is
 * not a real problem. BTW:  Longer lines are split.
 */
#define MAXCOL (0x7fffffffL)          /* maximum column number, 31 bits */

#define SHOWCMD_COLS 10                 /* columns needed by shown command */
#define STL_MAX_ITEM 80                 /* max nr of %<flag> in statusline */

typedef void        *vim_acl_T;         /* dummy to pass an ACL to a function */

/*
 * Include a prototype for mch_memmove(), it may not be in alloc.pro.
 */
#if defined(VIM_MEMMOVE)
void mch_memmove __ARGS((void *, void *, size_t));
#else
#if !defined(mch_memmove)
#define mch_memmove(to, from, len) memmove(to, from, len)
#endif
#endif

/*
 * fnamecmp() is used to compare file names.
 * On some systems case in a file name does not matter, on others it does.
 * (this does not account for maximum name lengths and things like "../dir",
 * thus it is not 100% accurate!)
 */
#define fnamecmp(x, y) vim_fnamecmp((char_u *)(x), (char_u *)(y))
#define fnamencmp(x, y, n) vim_fnamencmp((char_u *)(x), (char_u *)(y), (size_t)(n))

#if defined(HAVE_MEMSET)
#define vim_memset(ptr, c, size)   memset((ptr), (c), (size))
#else
void *vim_memset __ARGS((void *, int, size_t));
#endif

#if defined(HAVE_MEMCMP)
#define vim_memcmp(p1, p2, len)   memcmp((p1), (p2), (len))
#else
#if defined(HAVE_BCMP)
#define vim_memcmp(p1, p2, len)   bcmp((p1), (p2), (len))
#else
int vim_memcmp __ARGS((void *, void *, size_t));
#define VIM_MEMCMP
#endif
#endif

#define USE_INPUT_BUF

#if !defined(EINTR)
#define read_eintr(fd, buf, count) vim_read((fd), (buf), (count))
#define write_eintr(fd, buf, count) vim_write((fd), (buf), (count))
#endif

#define vim_read(fd, buf, count)   read((fd), (char *)(buf), (size_t) (count))
#define vim_write(fd, buf, count)  write((fd), (char *)(buf), (size_t) (count))

/*
 * Enums need a typecast to be used as array index (for Ultrix).
 */
#define hl_attr(n)      highlight_attr[(int)(n)]
#define term_str(n)     term_strings[(int)(n)]

/*
 * vim_iswhite() is used for "^" and the like. It differs from isspace()
 * because it doesn't include <CR> and <LF> and the like.
 */
#define vim_iswhite(x)  ((x) == ' ' || (x) == '\t')

/*
 * EXTERN is only defined in main.c.  That's where global variables are
 * actually defined and initialized.
 */
#if !defined(EXTERN)
#define EXTERN extern
#define INIT(x)
#else
#if !defined(INIT)
#define INIT(x) x
#define DO_INIT
#endif
#endif

#define MAX_MCO        6       /* maximum value for 'maxcombine' */

/* Maximum number of bytes in a multi-byte character.  It can be one 32-bit
 * character of up to 6 bytes, or one 16-bit character of up to three bytes
 * plus six following composing characters of three bytes each. */
#define MB_MAXBYTES    21

#if defined(FEAT_RELTIME)
typedef struct timeval proftime_T;
#else
typedef int proftime_T;     /* dummy for function prototypes */
#endif

/* Include option.h before structs.h, because the number of window-local and
 * buffer-local options is used there. */
#include "option.h"         /* options and default values */

#include "structs.h"        /* file that defines many structures */

/* Values for "do_profiling". */
#define PROF_NONE       0       /* profiling not started */
#define PROF_YES        1       /* profiling busy */
#define PROF_PAUSED     2       /* profiling paused */

#if defined(FEAT_MOUSE)

/* Codes for mouse button events in lower three bits: */
#define MOUSE_LEFT     0x00
#define MOUSE_MIDDLE   0x01
#define MOUSE_RIGHT    0x02
#define MOUSE_RELEASE  0x03

/* bit masks for modifiers: */
#define MOUSE_SHIFT    0x04
#define MOUSE_ALT      0x08
#define MOUSE_CTRL     0x10

/* mouse buttons that are handled like a key press (GUI only) */
/* Note that the scroll wheel keys are inverted: MOUSE_5 scrolls lines up but
 * the result of this is that the window moves down, similarly MOUSE_6 scrolls
 * columns left but the window moves right. */
#define MOUSE_4        0x100   /* scroll wheel down */
#define MOUSE_5        0x200   /* scroll wheel up */

#define MOUSE_X1       0x300 /* Mouse-button X1 (6th) */
#define MOUSE_X2       0x400 /* Mouse-button X2 */

#define MOUSE_6        0x500   /* scroll wheel left */
#define MOUSE_7        0x600   /* scroll wheel right */

/* 0x20 is reserved by xterm */
#define MOUSE_DRAG_XTERM   0x40

#define MOUSE_DRAG     (0x40 | MOUSE_RELEASE)

/* Lowest button code for using the mouse wheel (xterm only) */
#define MOUSEWHEEL_LOW         0x60

#define MOUSE_CLICK_MASK       0x03

#define NUM_MOUSE_CLICKS(code) \
    (((unsigned)((code) & 0xC0) >> 6) + 1)

#define SET_NUM_MOUSE_CLICKS(code, num) \
    (code) = ((code) & 0x3f) | ((((num) - 1) & 3) << 6)

/* Added to mouse column for GUI when 'mousefocus' wants to give focus to a
 * window by simulating a click on its status line.  We could use up to 128 *
 * 128 = 16384 columns, now it's reduced to 10000. */
#define MOUSE_COLOFF 10000

/*
 * jump_to_mouse() returns one of first four these values, possibly with
 * some of the other three added.
 */
#define IN_UNKNOWN             0
#define IN_BUFFER              1
#define IN_STATUS_LINE         2       /* on status or command line */
#define IN_SEP_LINE            4       /* on vertical separator line */
#define IN_OTHER_WIN           8       /* in other window but can't go there */
#define CURSOR_MOVED           0x100
#define MOUSE_FOLD_CLOSE       0x200   /* clicked on '-' in fold column */
#define MOUSE_FOLD_OPEN        0x400   /* clicked on '+' in fold column */

/* flags for jump_to_mouse() */
#define MOUSE_FOCUS            0x01    /* need to stay in this window */
#define MOUSE_MAY_VIS          0x02    /* may start Visual mode */
#define MOUSE_DID_MOVE         0x04    /* only act when mouse has moved */
#define MOUSE_SETPOS           0x08    /* only set current mouse position */
#define MOUSE_MAY_STOP_VIS     0x10    /* may stop Visual mode */
#define MOUSE_RELEASED         0x20    /* button was released */

#if defined(HAVE_GETTIMEOFDAY) && defined(HAVE_SYS_TIME_H)
#define CHECK_DOUBLE_CLICK 1  /* Checking for double clicks ourselves. */
#endif

#endif

/* defines for eval_vars() */
#define VALID_PATH              1
#define VALID_HEAD              2

/* Defines for Vim variables.  These must match vimvars[] in eval.c! */
#define VV_COUNT        0
#define VV_COUNT1       1
#define VV_PREVCOUNT    2
#define VV_ERRMSG       3
#define VV_WARNINGMSG   4
#define VV_STATUSMSG    5
#define VV_SHELL_ERROR  6
#define VV_THIS_SESSION 7
#define VV_VERSION      8
#define VV_LNUM         9
#define VV_TERMRESPONSE 10
#define VV_FNAME        11
#define VV_LANG         12
#define VV_LC_TIME      13
#define VV_CTYPE        14
#define VV_CC_FROM      15
#define VV_CC_TO        16
#define VV_FNAME_IN     17
#define VV_FNAME_OUT    18
#define VV_FNAME_NEW    19
#define VV_FNAME_DIFF   20
#define VV_CMDARG       21
#define VV_FOLDSTART    22
#define VV_FOLDEND      23
#define VV_FOLDDASHES   24
#define VV_FOLDLEVEL    25
#define VV_PROGNAME     26
#define VV_SEND_SERVER  27
#define VV_DYING        28
#define VV_EXCEPTION    29
#define VV_THROWPOINT   30
#define VV_REG          31
#define VV_CMDBANG      32
#define VV_INSERTMODE   33
#define VV_VAL          34
#define VV_KEY          35
#define VV_PROFILING    36
#define VV_FCS_REASON   37
#define VV_FCS_CHOICE   38
#define VV_BEVAL_BUFNR  39
#define VV_BEVAL_WINNR  40
#define VV_BEVAL_LNUM   41
#define VV_BEVAL_COL    42
#define VV_BEVAL_TEXT   43
#define VV_SCROLLSTART  44
#define VV_SWAPNAME     45
#define VV_SWAPCHOICE   46
#define VV_SWAPCOMMAND  47
#define VV_CHAR         48
#define VV_MOUSE_WIN    49
#define VV_MOUSE_LNUM   50
#define VV_MOUSE_COL    51
#define VV_OP           52
#define VV_SEARCHFORWARD 53
#define VV_HLSEARCH     54
#define VV_OLDFILES     55
#define VV_WINDOWID     56
#define VV_PROGPATH     57
#define VV_LEN          58      /* number of v: vars */

#if defined(FEAT_CLIPBOARD)

/* VIM_ATOM_NAME is the older Vim-specific selection type for X11.  Still
 * supported for when a mix of Vim versions is used. VIMENC_ATOM_NAME includes
 * the encoding to support Vims using different 'encoding' values. */
#define VIM_ATOM_NAME "_VIM_TEXT"
#define VIMENC_ATOM_NAME "_VIMENC_TEXT"

/* Selection states for modeless selection */
#define SELECT_CLEARED         0
#define SELECT_IN_PROGRESS     1
#define SELECT_DONE            2

#define SELECT_MODE_CHAR       0
#define SELECT_MODE_WORD       1
#define SELECT_MODE_LINE       2

/* Info about selected text */
typedef struct VimClipboard
{
    int         available;      /* Is clipboard available? */
    int         owned;          /* Flag: do we own the selection? */
    pos_T       start;          /* Start of selected area */
    pos_T       end;            /* End of selected area */
    int         vmode;          /* Visual mode character */

    /* Fields for selection that doesn't use Visual mode */
    short_u     origin_row;
    short_u     origin_start_col;
    short_u     origin_end_col;
    short_u     word_start_col;
    short_u     word_end_col;

    pos_T       prev;           /* Previous position */
    short_u     state;          /* Current selection state */
    short_u     mode;           /* Select by char, word, or line. */

} VimClipboard;
#else
typedef int VimClipboard;       /* This is required for the prototypes. */
#endif

#include "ex_cmds.h"        /* Ex command defines */
#include "proto.h"          /* function prototypes */

/* This has to go after the include of proto.h, as proto/gui.pro declares
 * functions of these names. The declarations would break if the defines had
 * been seen at that stage.  But it must be before globals.h, where error_ga
 * is declared. */
#define mch_errmsg(str)        fprintf(stderr, "%s", (str))
#define display_errors()       fflush(stderr)
#define mch_msg(str)           printf("%s", (str))

#if !defined(FEAT_LINEBREAK)
/* Without the 'numberwidth' option line numbers are always 7 chars. */
#define number_width(x) 7
#endif

#include "globals.h"        /* global variables and messages */

#if !defined(FEAT_VIRTUALEDIT)
#define getvvcol(w, p, s, c, e) getvcol(w, p, s, c, e)
#define virtual_active() FALSE
#define virtual_op FALSE
#endif

/* Note: a NULL argument for vim_realloc() is not portable, don't use it. */
#define vim_realloc(ptr, size)  realloc((ptr), (size))

/*
 * The following macros stop display/event loop nesting at the wrong time.
 */
#if defined(ALT_X_INPUT)
#define ALT_INPUT_LOCK_OFF     suppress_alternate_input = FALSE
#define ALT_INPUT_LOCK_ON      suppress_alternate_input = TRUE
#endif

/*
 * Return byte length of character that starts with byte "b".
 * Returns 1 for a single-byte character.
 * MB_BYTE2LEN_CHECK() can be used to count a special key as one byte.
 * Don't call MB_BYTE2LEN(b) with b < 0 or b > 255!
 */
#define MB_BYTE2LEN(b)         mb_bytelen_tab[b]
#define MB_BYTE2LEN_CHECK(b)   (((b) < 0 || (b) > 255) ? 1 : mb_bytelen_tab[b])

/* properties used in enc_canon_table[] (first three mutually exclusive) */
#define ENC_8BIT       0x01
#define ENC_DBCS       0x02
#define ENC_UNICODE    0x04

#define ENC_ENDIAN_B   0x10        /* Unicode: Big endian */
#define ENC_ENDIAN_L   0x20        /* Unicode: Little endian */

#define ENC_2BYTE      0x40        /* Unicode: UCS-2 */
#define ENC_4BYTE      0x80        /* Unicode: UCS-4 */
#define ENC_2WORD      0x100       /* Unicode: UTF-16 */

#define ENC_LATIN1     0x200       /* Latin1 */
#define ENC_LATIN9     0x400       /* Latin9 */
#define ENC_MACROMAN   0x800       /* Mac Roman (not Macro Man! :-) */

#if defined(USE_ICONV)
#if !defined(EILSEQ)
#define EILSEQ 123
#endif
#define ICONV_ERRNO errno
#define ICONV_E2BIG  E2BIG
#define ICONV_EINVAL EINVAL
#define ICONV_EILSEQ EILSEQ
#endif

/* ISSYMLINK(mode) tests if a file is a symbolic link. */
#if (defined(S_IFMT) && defined(S_IFLNK)) || defined(S_ISLNK)
#define HAVE_ISSYMLINK
#if defined(S_IFMT) && defined(S_IFLNK)
#define ISSYMLINK(mode) (((mode) & S_IFMT) == S_IFLNK)
#else
#define ISSYMLINK(mode) S_ISLNK(mode)
#endif
#endif

#define SIGN_BYTE 1         /* byte value used where sign is displayed;
                               attribute value is sign type */

/* values for vim_handle_signal() that are not a signal */
#define SIGNAL_BLOCK    -1
#define SIGNAL_UNBLOCK  -2

/* flags for skip_vimgrep_pat() */
#define VGR_GLOBAL      1
#define VGR_NOJUMP      2

/* behavior for bad character, "++bad=" argument */
#define BAD_REPLACE     '?'     /* replace it with '?' (default) */
#define BAD_KEEP        -1      /* leave it */
#define BAD_DROP        -2      /* erase it */

/* last argument for do_source() */
#define DOSO_NONE       0
#define DOSO_VIMRC      1       /* loading vimrc file */
#define DOSO_GVIMRC     2       /* loading gvimrc file */

/* flags for buf_freeall() */
#define BFA_DEL         1       /* buffer is going to be deleted */
#define BFA_WIPE        2       /* buffer is going to be wiped out */
#define BFA_KEEP_UNDO   4       /* do not free undo information */

/* direction for nv_mousescroll() and ins_mousescroll() */
#define MSCR_DOWN       0       /* DOWN must be FALSE */
#define MSCR_UP         1
#define MSCR_LEFT       -1
#define MSCR_RIGHT      -2

#define KEYLEN_PART_KEY -1      /* keylen value for incomplete key-code */
#define KEYLEN_PART_MAP -2      /* keylen value for incomplete mapping */
#define KEYLEN_REMOVED  9999    /* keylen value for removed sequence */

/* Return values from win32_fileinfo(). */
#define FILEINFO_OK          0
#define FILEINFO_ENC_FAIL    1  /* enc_to_utf16() failed */
#define FILEINFO_READ_FAIL   2  /* CreateFile() failed */
#define FILEINFO_INFO_FAIL   3  /* GetFileInformationByHandle() failed */

/* Return value from get_option_value_strict */
#define SOPT_BOOL       0x01    /* Boolean option */
#define SOPT_NUM        0x02    /* Number option */
#define SOPT_STRING     0x04    /* String option */
#define SOPT_GLOBAL     0x08    /* Option has global value */
#define SOPT_WIN        0x10    /* Option has window-local value */
#define SOPT_BUF        0x20    /* Option has buffer-local value */
#define SOPT_UNSET      0x40    /* Option does not have local value set */

/* Option types for various functions in option.c */
#define SREQ_GLOBAL     0       /* Request global option */
#define SREQ_WIN        1       /* Request window-local option */
#define SREQ_BUF        2       /* Request buffer-local option */

/* Flags for get_reg_contents */
#define GREG_NO_EXPR    1       /* Do not allow expression register */
#define GREG_EXPR_SRC   2       /* Return expression itself for "=" register */
#define GREG_LIST       4       /* Return list */

/* Character used as separated in autoload function/variable names. */
#define AUTOLOAD_CHAR '#'

#define SET_NO_HLSEARCH(flag) no_hlsearch = (flag); set_vim_var_nr(VV_HLSEARCH, !no_hlsearch && p_hls)

#endif
