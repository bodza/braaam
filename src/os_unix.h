#include <stdio.h>
#include <ctype.h>

#include <sys/types.h>
#include <sys/stat.h>

#include <stdlib.h>
#include <unistd.h>

#include <sys/param.h>

#define vim_mkdir(x, y) mkdir((char *)(x), y)
#define mch_rmdir(x) rmdir((char *)(x))
#define mch_remove(x) unlink((char *)(x))

#define SIGDEFARG(s)  (s) int s UNUSED;

#include <dirent.h>
#if !defined(NAMLEN)
#define NAMLEN(dirent) strlen((dirent)->d_name)
#endif

#include <time.h>          /* on some systems time.h should not be
                               included together with sys/time.h */
#include <sys/time.h>

#include <signal.h>

#if defined(DIRSIZ) && !defined(MAXNAMLEN)
#define MAXNAMLEN DIRSIZ
#endif

#if defined(UFS_MAXNAMLEN) && !defined(MAXNAMLEN)
#define MAXNAMLEN UFS_MAXNAMLEN    /* for dynix/ptx */
#endif

#if defined(NAME_MAX) && !defined(MAXNAMLEN)
#define MAXNAMLEN NAME_MAX         /* for Linux before .99p3 */
#endif

/*
 * Note: if MAXNAMLEN has the wrong value, you will get error messages
 *       for not being able to open the swap file.
 */
#if !defined(MAXNAMLEN)
#define MAXNAMLEN 512              /* for all other Unix */
#endif

#define BASENAMELEN     (MAXNAMLEN - 5)

#include <pwd.h>

/*
 * Unix system-dependent file names
 */
#define SYS_VIMRC_FILE "$VIM/vimrc"

#define DFLT_HELPFILE  "$VIMRUNTIME/doc/help.txt"
#define FILETYPE_FILE  "filetype.vim"
#define FTPLUGIN_FILE  "ftplugin.vim"
#define INDENT_FILE    "indent.vim"
#define FTOFF_FILE     "ftoff.vim"
#define FTPLUGOF_FILE  "ftplugof.vim"
#define INDOFF_FILE    "indoff.vim"

#define USR_EXRC_FILE "$HOME/.exrc"
#define USR_VIMRC_FILE "$HOME/.vimrc"

#define EVIM_FILE      "$VIMRUNTIME/evim.vim"

#define EXRC_FILE      ".exrc"
#define VIMRC_FILE     ".vimrc"

#define SYNTAX_FNAME   "$VIMRUNTIME/syntax/%s.vim"

#define DFLT_BDIR    ".,~/tmp,~/"    /* default for 'backupdir' */
#define DFLT_DIR     ".,~/tmp,/var/tmp,/tmp" /* default for 'directory' */

#define DFLT_ERRORFILE          "errors.err"

#define DFLT_RUNTIMEPATH     "~/.vim,$VIM/vimfiles,$VIMRUNTIME,$VIM/vimfiles/after,~/.vim/after"

#define TEMPDIRNAMES  "$TMPDIR", "/tmp", ".", "$HOME"
#define TEMPNAMELEN    256

/* Special wildcards that need to be handled by the shell */
#define SPECIAL_WILDCHAR    "`'{"

/*
 * Unix has plenty of memory, use large buffers
 */
#define CMDBUFFSIZE 1024        /* size of the command processing buffer */

/* Use the system path length if it makes sense. */
#if defined(PATH_MAX) && (PATH_MAX > 1000)
#define MAXPATHL       PATH_MAX
#else
#define MAXPATHL       1024
#endif

#define CHECK_INODE             /* used when checking if a swap file already exists for a file */
#if !defined(DFLT_MAXMEM)
#define DFLT_MAXMEM   (5*1024)         /* use up to 5 Mbyte for a buffer */
#endif
#if !defined(DFLT_MAXMEMTOT)
#define DFLT_MAXMEMTOT        (10*1024)    /* use up to 10 Mbyte for Vim */
#endif

/* memmove is not present on all systems, use memmove, bcopy, memcpy or our
 * own version */
/* Some systems have (void *) arguments, some (char *). If we use (char *) it
 * works for all */
#define mch_memmove(to, from, len) memmove((char *)(to), (char *)(from), len)

#define mch_rename(src, dst) rename(src, dst)
#define mch_getenv(x) (char_u *)getenv((char *)(x))
#define mch_setenv(name, val, x) setenv(name, val, x)

#if !defined(S_ISDIR) && defined(S_IFDIR)
#define S_ISDIR(m) (((m) & S_IFMT) == S_IFDIR)
#endif
#if !defined(S_ISREG) && defined(S_IFREG)
#define S_ISREG(m) (((m) & S_IFMT) == S_IFREG)
#endif
#if !defined(S_ISBLK) && defined(S_IFBLK)
#define S_ISBLK(m) (((m) & S_IFMT) == S_IFBLK)
#endif
#if !defined(S_ISSOCK) && defined(S_IFSOCK)
#define S_ISSOCK(m) (((m) & S_IFMT) == S_IFSOCK)
#endif
#if !defined(S_ISFIFO) && defined(S_IFIFO)
#define S_ISFIFO(m) (((m) & S_IFMT) == S_IFIFO)
#endif
#if !defined(S_ISCHR) && defined(S_IFCHR)
#define S_ISCHR(m) (((m) & S_IFMT) == S_IFCHR)
#endif

/* Note: Some systems need both string.h and strings.h (Savage).  However,
 * some systems can't handle both, only use string.h in that case. */
#include <string.h>
#include <strings.h>

#include <setjmp.h>
#define JMP_BUF jmp_buf
#define SETJMP(x) setjmp(x)
#define LONGJMP longjmp
