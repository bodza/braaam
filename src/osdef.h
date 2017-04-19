/*
 * osdef.h is automagically created from osdef?.h.in by osdef.sh -- DO NOT EDIT
 */
/* autoconf cannot fiddle out declarations. Use our homebrewn tools. (jw) */
/*
 * Declarations that may cause conflicts belong here so that osdef.sh
 * can clean out the forest. Everything else belongs in os_unix.h
 *
 * How this works:
 * - This file contains all unix prototypes that Vim might need.
 * - The shell script osdef.sh is executed at compile time to remove all the
 *   prototypes that are in an include file. This results in osdef.h.
 * - osdef.h is included in vim.h.
 *
 * sed cannot always handle so many commands, this is file 1 of 2
 */

#if defined(sun) || defined(_SEQUENT_)
/* used inside of stdio macros getc(), puts(), putchar()... */
extern int      _flsbuf __ARGS((int, FILE *));
extern int      _filbuf __ARGS((FILE *));
#endif

#if !defined(HAVE_SELECT)
struct pollfd;                  /* for poll __ARGS */
extern int      poll __ARGS((struct pollfd *, long, int));
#endif

#if defined(HAVE_SIGSET)
extern RETSIGTYPE (*sigset __ARGS((int, RETSIGTYPE (*func) SIGPROTOARG))) __ARGS(SIGPROTOARG);
#endif

/*
 * osdef2.h.in - See osdef1.h.in for a description.
 */

#if defined(ISC)
extern int      _Xmblen __ARGS((char const *, size_t));
#else
                /* This is different from the header but matches mblen() */
extern int      _Xmblen __ARGS((char *, size_t));
#endif
