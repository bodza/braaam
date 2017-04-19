/*
 * proto.h: include the (automatically generated) function prototypes
 */

/*
 * Don't include these while generating prototypes.  Prevents problems when
 * files are missing.
 */
#if (1)

/*
 * Machine-dependent routines.
 */
/* avoid errors in function prototypes */
#if !defined(FEAT_X11) && !defined(FEAT_GUI_GTK)
#define Display int
#define Widget int
#endif
#if !defined(FEAT_GUI_GTK)
#define GdkEvent int
#define GdkEventKey int
#endif

#if defined(UNIX)
#include "os_unix.pro"
#endif

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
#if defined(FEAT_HANGULIN)
#include "hangulin.pro"
#endif
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
#if defined(FEAT_MBYTE) || defined(FEAT_XIM) || defined(FEAT_KEYMAP) || defined(FEAT_POSTSCRIPT)
#include "mbyte.pro"
#endif
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
#if defined(HAVE_TGETENT) && (0)
#include "termlib.pro"
#endif
#include "ui.pro"
#include "undo.pro"
#include "version.pro"
#include "window.pro"

#if defined(FEAT_LUA)
#include "if_lua.pro"
#endif

#if defined(FEAT_MZSCHEME)
#include "if_mzsch.pro"
#endif

#if defined(FEAT_PYTHON)
#include "if_python.pro"
#endif

#if defined(FEAT_PYTHON3)
#include "if_python3.pro"
#endif

#if defined(FEAT_TCL)
#include "if_tcl.pro"
#endif

#if defined(FEAT_RUBY)
#include "if_ruby.pro"
#endif

/* Ugly solution for "BalloonEval" not being defined while it's used in some
 * .pro files. */
#if !defined(FEAT_BEVAL)
#define BalloonEval int
#endif

#if defined(FEAT_NETBEANS_INTG)
#include "netbeans.pro"
#endif

#if defined(FEAT_GUI)
#include "gui.pro"
#if defined(UNIX) || defined(MACOS)
#include "pty.pro"
#endif
#if !defined(HAVE_SETENV) && !defined(HAVE_PUTENV)
extern int putenv __ARGS((const char *string));         /* from pty.c */
#if defined(USE_VIMPTY_GETENV)
extern char_u *vimpty_getenv __ARGS((const char_u *string));    /* from pty.c */
#endif
#endif
#if defined(FEAT_GUI_W16)
#include "gui_w16.pro"
#endif
#if defined(FEAT_GUI_W32)
#include "gui_w32.pro"
#endif
#if defined(FEAT_GUI_GTK)
#include "gui_gtk.pro"
#include "gui_gtk_x11.pro"
#endif
#if defined(FEAT_GUI_MOTIF)
#include "gui_motif.pro"
#include "gui_xmdlg.pro"
#endif
#if defined(FEAT_GUI_ATHENA)
#include "gui_athena.pro"
#if defined(FEAT_BROWSE)
extern char *vim_SelFile __ARGS((Widget toplevel, char *prompt, char *init_path, int (*show_entry)(), int x, int y, guicolor_T fg, guicolor_T bg, guicolor_T scroll_fg, guicolor_T scroll_bg));
#endif
#endif
#if defined(FEAT_GUI_MAC)
#include "gui_mac.pro"
#endif
#if defined(FEAT_GUI_X11)
#include "gui_x11.pro"
#endif
#if defined(FEAT_GUI_PHOTON)
#include "gui_photon.pro"
#endif
#if defined(FEAT_SUN_WORKSHOP)
#include "workshop.pro"
#endif
#endif

#if defined(FEAT_OLE)
#include "if_ole.pro"
#endif
#if defined(FEAT_CLIENTSERVER) && defined(FEAT_X11)
#include "if_xcmdsrv.pro"
#endif

/*
 * The perl include files pollute the namespace, therefore proto.h must be
 * included before the perl include files.  But then CV is not defined, which
 * is used in if_perl.pro.  To get around this, the perl prototype files are
 * not included here for the perl files.  Use a dummy define for CV for the
 * other files.
 */
#if defined(FEAT_PERL) && !defined(IN_PERL_FILE)
#define CV void
#include "if_perl.pro"
#include "if_perlsfio.pro"
#endif

#if defined(MACOS_CONVERT)
#include "os_mac_conv.pro"
#endif
#if defined(MACOS_X_UNIX) && defined(FEAT_CLIPBOARD) && !defined(FEAT_GUI)
/* functions in os_macosx.m */
void clip_mch_lose_selection(VimClipboard *cbd);
int clip_mch_own_selection(VimClipboard *cbd);
void clip_mch_request_selection(VimClipboard *cbd);
void clip_mch_set_selection(VimClipboard *cbd);
#endif

#endif
