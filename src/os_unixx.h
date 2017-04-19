/*
 * os_unixx.h -- include files that are only used in os_unix.c
 */

/*
 * Stuff for signals
 */
#if defined(HAVE_SIGSET) && !defined(signal)
#define signal sigset
#endif

   /* sun's sys/ioctl.h redefines symbols from termio world */
#if defined(HAVE_SYS_IOCTL_H) && !defined(sun)
#include <sys/ioctl.h>
#endif

#if defined(HAVE_SYS_WAIT_H) || defined(HAVE_UNION_WAIT)
#include <sys/wait.h>
#endif

#if !defined(WEXITSTATUS)
#if defined(HAVE_UNION_WAIT)
#define WEXITSTATUS(stat_val) ((stat_val).w_T.w_Retcode)
#else
#define WEXITSTATUS(stat_val) (((stat_val) >> 8) & 0377)
#endif
#endif

#if !defined(WIFEXITED)
#if defined(HAVE_UNION_WAIT)
#define WIFEXITED(stat_val) ((stat_val).w_T.w_Termsig == 0)
#else
#define WIFEXITED(stat_val) (((stat_val) & 255) == 0)
#endif
#endif

#if defined(HAVE_STROPTS_H)
#if defined(sinix)
#define buf_T __system_buf_t__
#endif
#include <stropts.h>
#if defined(sinix)
#undef buf_T
#endif
#endif

#if defined(HAVE_STRING_H)
#include <string.h>
#endif

#if defined(HAVE_SYS_STREAM_H)
#include <sys/stream.h>
#endif

#if defined(HAVE_SYS_UTSNAME_H)
#include <sys/utsname.h>
#endif

#if defined(HAVE_SYS_SYSTEMINFO_H)
/*
 * foolish Sinix <sys/systeminfo.h> uses SYS_NMLN but doesn't include
 * <limits.h>, where it is defined. Perhaps other systems have the same
 * problem? Include it here. -- Slootman
 */
#if defined(HAVE_LIMITS_H) && !defined(_LIMITS_H)
#include <limits.h>           /* for SYS_NMLN (Sinix 5.41 / Unix SysV.4) */
#endif

/* Define SYS_NMLN ourselves if it still isn't defined (for CrayT3E). */
#if !defined(SYS_NMLN)
#define SYS_NMLN 32
#endif

#include <sys/systeminfo.h>    /* for sysinfo */
#endif

/*
 * We use termios.h if both termios.h and termio.h are available.
 * Termios is supposed to be a superset of termio.h.  Don't include them both,
 * it may give problems on some systems (e.g. hpux).
 * I don't understand why we don't want termios.h for apollo.
 */
#if defined(HAVE_TERMIOS_H) && !defined(apollo)
#include <termios.h>
#else
#if defined(HAVE_TERMIO_H)
#include <termio.h>
#else
#if defined(HAVE_SGTTY_H)
#include <sgtty.h>
#endif
#endif
#endif

#if defined(HAVE_SYS_PTEM_H)
#include <sys/ptem.h>  /* must be after termios.h for Sinix */
#if !defined(_IO_PTEM_H) /* For UnixWare that should check for _IO_PT_PTEM_H */
#define _IO_PTEM_H
#endif
#endif

/* shared library access */
#if defined(HAVE_DLFCN_H) && defined(USE_DLOPEN)
#include <dlfcn.h>
#endif
