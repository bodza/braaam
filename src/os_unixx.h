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

#if defined(HAVE_SYS_WAIT_H)
#include <sys/wait.h>
#endif

#if !defined(WEXITSTATUS)
#define WEXITSTATUS(stat_val) (((stat_val) >> 8) & 0377)
#endif

#if !defined(WIFEXITED)
#define WIFEXITED(stat_val) (((stat_val) & 255) == 0)
#endif

#if defined(HAVE_STRING_H)
#include <string.h>
#endif

#if defined(HAVE_SYS_UTSNAME_H)
#include <sys/utsname.h>
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
