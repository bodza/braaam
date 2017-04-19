/*
 * proto.h: include the (automatically generated) function prototypes
 */

/*
 * Don't include these while generating prototypes.  Prevents problems when
 * files are missing.
 */

/*
 * Machine-dependent routines.
 */
/* avoid errors in function prototypes */
#if (1)
#define Display int
#define Widget int
#endif
#define GdkEvent int
#define GdkEventKey int

#include "os_unix.pro"

#if defined(FEAT_CRYPT)
#include "blowfish.pro"
#include "crypt.pro"
#include "crypt_zip.pro"
#endif
#include "buffer.pro"
#include "charset.pro"
#if defined(FEAT_CSCOPE)
#include "if_cscope.pro"
#endif
#include "diff.pro"
#include "digraph.pro"
#include "edit.pro"
#include "eval.pro"
#include "ex_cmds.pro"
#include "ex_cmds2.pro"
#include "ex_docmd.pro"
#include "ex_eval.pro"
#include "ex_getln.pro"
#include "fileio.pro"
#include "fold.pro"
#include "getchar.pro"
#include "hardcopy.pro"
#include "hashtab.pro"
#include "main.pro"
#include "mark.pro"
#include "memfile.pro"
#include "memline.pro"
#if defined(FEAT_MENU)
#include "menu.pro"
#endif

#if !defined(MESSAGE_FILE) || defined(HAVE_STDARG_H)
    /* These prototypes cannot be produced automatically and conflict with
     * the old-style prototypes in message.c. */
int
smsg __ARGS((char_u *, ...));

int
smsg_attr __ARGS((int, char_u *, ...));

int
vim_snprintf_add __ARGS((char *, size_t, char *, ...));

int
vim_snprintf __ARGS((char *, size_t, char *, ...));

#if defined(HAVE_STDARG_H)
int vim_vsnprintf(char *str, size_t str_m, char *fmt, va_list ap, typval_T *tvs);
#endif
#endif

#include "message.pro"
#include "misc1.pro"
#include "misc2.pro"
#if !defined(HAVE_STRPBRK) /* not generated automatically from misc2.c */
char_u *vim_strpbrk __ARGS((char_u *s, char_u *charset));
#endif
#if !defined(HAVE_QSORT)
/* Use our own qsort(), don't define the prototype when not used. */
void qsort __ARGS((void *base, size_t elm_count, size_t elm_size, int (*cmp)(const void *, const void *)));
#endif
#include "move.pro"
#include "mbyte.pro"
#include "normal.pro"
#include "ops.pro"
#include "option.pro"
#include "popupmnu.pro"
#if defined(FEAT_QUICKFIX)
#include "quickfix.pro"
#endif
#include "regexp.pro"
#include "screen.pro"
#if defined(FEAT_CRYPT) || defined(FEAT_PERSISTENT_UNDO)
#include "sha256.pro"
#endif
#include "search.pro"
#include "spell.pro"
#include "syntax.pro"
#include "tag.pro"
#include "term.pro"
#include "ui.pro"
#include "undo.pro"
#include "version.pro"
#include "window.pro"

/* Ugly solution for "BalloonEval" not being defined while it's used in some
 * .pro files. */
#if (1)
#define BalloonEval int
#endif

/*
 * The perl include files pollute the namespace, therefore proto.h must be
 * included before the perl include files.  But then CV is not defined, which
 * is used in if_perl.pro.  To get around this, the perl prototype files are
 * not included here for the perl files.  Use a dummy define for CV for the
 * other files.
 */

#if defined(MACOS_CONVERT)
#include "os_mac_conv.pro"
#endif
