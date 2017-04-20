package vim;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Comparator;

import jnr.ffi.LibraryLoader;
import jnr.ffi.mapper.DataConverter;
import jnr.ffi.mapper.FromNativeContext;
import jnr.ffi.mapper.ToNativeContext;
import jnr.ffi.Memory;
import jnr.ffi.NativeType;
import jnr.ffi.Pointer;
import jnr.ffi.StructLayout;
import jnr.ffi.Variable;
import jnr.ffi.types.*;

import static vim.VimA.*;
import static vim.VimB.*;
import static vim.VimC.*;
import static vim.VimD.*;
import static vim.VimE.*;
import static vim.VimF.*;
import static vim.VimG.*;
import static vim.VimH.*;
import static vim.VimI.*;
import static vim.VimJ.*;
import static vim.VimK.*;
import static vim.VimL.*;
import static vim.VimM.*;
import static vim.VimN.*;
import static vim.VimO.*;
import static vim.VimP.*;
import static vim.VimQ.*;
import static vim.VimR.*;
import static vim.VimS.*;
import static vim.VimT.*;
import static vim.VimU.*;
import static vim.VimV.*;
import static vim.VimW.*;
import static vim.VimX.*;
import static vim.VimY.*;

/*
 * Supported Types
 * ------
 *
 * All java primitives are mapped simply to the equivalent C types.
 *
 * - byte - 8 bit signed integer
 * - short - 16 bit signed integer
 * - int - 32 bit signed integer
 * - long - natural long (i.e. 32 bits wide on 32 bit systems, 64 bit wide on 64 bit systems)
 * - float - 32 bit float
 * - double - 64 bit float
 *
 * The width and/or signed-ness of these basic types can be specified using one of the type alias annotations.
 *  e.g.
 *
 *     // Use the correct width for the result from getpid(3)
 *     @pid_t long getpid();
 *
 *     // read(2) returns a signed long result, and its length parameter is an unsigned long
 *     @ssize_t long read(int fd, Pointer data, @size_t long len);
 *
 *
 * In addition, the following java types are mapped to a C pointer
 *
 * - String - equivalent to "const char *"
 * - Pointer - equivalent to "void *"
 * - Buffer - equivalent to "void *"
 */
public class VimJ
{
    /*
     * eval.c: Expression evaluation ------------------------------------------------------------------
     */

    /*private*/ static final int DICT_MAXNEST = 100;        /* maximum nesting of lists and dicts */

    /*private*/ static final int DO_NOT_FREE_CNT = 99999;   /* refcount for dict or list that should not be freed. */

    /*
     * Structure returned by get_lval() and used by set_var_lval().
     * For a plain name:
     *      "name"      points to the variable name.
     *      "exp_name"  is null.
     *      "tv"        is null
     * For a magic braces name:
     *      "name"      points to the expanded variable name.
     *      "exp_name"  is non-null, to be freed later.
     *      "tv"        is null
     * For an index in a list:
     *      "name"      points to the (expanded) variable name.
     *      "exp_name"  null or non-null, to be freed later.
     *      "tv"        points to the (first) list item value
     *      "li"        points to the (first) list item
     *      "range", "n1", "n2" and "empty2" indicate what items are used.
     * For an existing Dict item:
     *      "name"      points to the (expanded) variable name.
     *      "exp_name"  null or non-null, to be freed later.
     *      "tv"        points to the dict item value
     *      "newkey"    is null
     * For a non-existing Dict item:
     *      "name"      points to the (expanded) variable name.
     *      "exp_name"  null or non-null, to be freed later.
     *      "tv"        points to the Dictionary typval_C
     *      "newkey"    is the key for the new item.
     */
    /*private*/ static final class lval_C
    {
        Bytes       ll_name;            /* start of variable name (can be null) */
        Bytes       ll_exp_name;        /* null or expanded name in allocated memory. */
        typval_C    ll_tv;              /* Typeval of item being used.  If "newkey"
                                         * isn't null it's the Dict to which to add the item. */
        listitem_C  ll_li;              /* The list item or null. */
        list_C      ll_list;            /* The list or null. */
        boolean     ll_range;           /* true when a [i:j] range was used */
        long        ll_n1;              /* First index for list */
        long        ll_n2;              /* Second index for list range */
        boolean     ll_empty2;          /* Second index is empty: [i:] */
        dict_C      ll_dict;            /* The Dictionary or null */
        dictitem_C  ll_di;              /* The dictitem or null */
        Bytes       ll_newkey;          /* New key for Dict in alloc. mem or null. */

        /*private*/ lval_C()
        {
        }
    }

    /*private*/ static void ZER0_lval(lval_C ll)
    {
        ll.ll_name = null;
        ll.ll_exp_name = null;
        ll.ll_tv = null;
        ll.ll_li = null;
        ll.ll_list = null;
        ll.ll_range = false;
        ll.ll_n1 = 0;
        ll.ll_n2 = 0;
        ll.ll_empty2 = false;
        ll.ll_dict = null;
        ll.ll_di = null;
        ll.ll_newkey = null;
    }

    /*private*/ static Bytes e_letunexp =          u8("E18: Unexpected characters in :let");
    /*private*/ static Bytes e_listidx =           u8("E684: list index out of range: %ld");
    /*private*/ static Bytes e_undefvar =          u8("E121: Undefined variable: %s");
    /*private*/ static Bytes e_missbrac =          u8("E111: Missing ']'");
    /*private*/ static Bytes e_listarg =           u8("E686: Argument of %s must be a List");
    /*private*/ static Bytes e_listdictarg =       u8("E712: Argument of %s must be a List or Dictionary");
    /*private*/ static Bytes e_emptykey =          u8("E713: Cannot use empty key for Dictionary");
    /*private*/ static Bytes e_listreq =           u8("E714: List required");
    /*private*/ static Bytes e_dictreq =           u8("E715: Dictionary required");
    /*private*/ static Bytes e_toomanyarg =        u8("E118: Too many arguments for function: %s");
    /*private*/ static Bytes e_dictkey =           u8("E716: Key not present in Dictionary: %s");
    /*private*/ static Bytes e_funcexts =          u8("E122: Function %s already exists, add ! to replace it");
    /*private*/ static Bytes e_funcdict =          u8("E717: Dictionary entry already exists");
    /*private*/ static Bytes e_funcref =           u8("E718: Funcref required");
    /*private*/ static Bytes e_dictrange =         u8("E719: Cannot use [:] with a Dictionary");
    /*private*/ static Bytes e_letwrong =          u8("E734: Wrong variable type for %s=");
    /*private*/ static Bytes e_nofunc =            u8("E130: Unknown function: %s");
    /*private*/ static Bytes e_illvar =            u8("E461: Illegal variable name: %s");

    /*private*/ static dictitem_C globvars_var;     /* variable used for g: */

    /*
     * When recursively copying lists and dicts we need to remember which ones we
     * have done to avoid endless recursiveness.  This unique ID is used for that.
     * The last bit is used for previous_funccal, ignored when comparing.
     */
    /*private*/ static int current_copyID;
    /*private*/ static final int COPYID_INC = 2;
    /*private*/ static final int COPYID_MASK = ~0x1;

    /* Abort conversion to string after a recursion error. */
    /*private*/ static boolean did_echo_string_emsg;

    /*
     * Array to hold the hashtab with variables local to each sourced script.
     * Each item holds a variable (nameless) that points to the dict_C.
     */
    /*private*/ static final class scriptvar_C
    {
        dictitem_C  sv_var;
        dict_C      sv_dict;

        /*private*/ scriptvar_C()
        {
        }
    }

    /*private*/ static Growing<scriptvar_C> ga_scripts = new Growing<scriptvar_C>(scriptvar_C.class, 4);

    /*private*/ static int echo_attr;   /* attributes used for ":echo" */

    /* Values for trans_function_name() argument: */
    /*private*/ static final int TFN_INT         = 1;       /* internal function name OK */
    /*private*/ static final int TFN_QUIET       = 2;       /* no error messages */
    /*private*/ static final int TFN_NO_AUTOLOAD = 4;       /* do not use script autoloading */

    /* Values for get_lval() flags argument: */
    /*private*/ static final int GLV_QUIET       = TFN_QUIET;       /* no error messages */
    /*private*/ static final int GLV_NO_AUTOLOAD = TFN_NO_AUTOLOAD; /* do not use script autoloading */

    /*
     * Structure to hold info for a user function.
     */
    /*private*/ static final class ufunc_C
    {
        boolean     uf_varargs;     /* variable nr of arguments */
        int         uf_flags;
        int         uf_calls;       /* nr of active calls */
        Growing<Bytes>    uf_args;        /* arguments */
        Growing<Bytes>    uf_lines;       /* function lines */
        int         uf_script_ID;   /* ID of script where function was defined, used for s: variables */
        int         uf_refcount;    /* for numbered function: reference count */
        Bytes       uf_name;        /* name of function;
                                     * can start with <SNR>123_ (<SNR> is KB_SPECIAL KS_EXTRA KE_SNR) */

        /*private*/ ufunc_C()
        {
            uf_args = new Growing<Bytes>(Bytes.class, 3);
            uf_lines = new Growing<Bytes>(Bytes.class, 3);
        }
    }

    /* function flags */
    /*private*/ static final int FC_ABORT    = 1;           /* abort function on error */
    /*private*/ static final int FC_RANGE    = 2;           /* function accepts range */
    /*private*/ static final int FC_DICT     = 4;           /* Dict function, uses "self" */

    /*
     * All user-defined functions are found in this hashtable.
     */
    /*private*/ static hashtab_C func_hashtab = new hashtab_C();

    /* The names of packages that once were loaded are remembered. */
    /*private*/ static Growing<Bytes> ga_loaded = new Growing<Bytes>(Bytes.class, 4);

    /*private*/ static final int MAX_FUNC_ARGS   = 20;                  /* maximum number of function arguments */

    /* structure to hold info for a function that is currently being executed. */
    /*private*/ static final class funccall_C
    {
        ufunc_C     func;                       /* function being called */
        int         linenr;                     /* next line to be executed */
        boolean     returned;                   /* ":return" used */
        dict_C      l_vars;                     /* l: local function variables */
        dictitem_C  l_vars_var;                 /* variable for l: scope */
        dict_C      l_avars;                    /* a: argument variables */
        dictitem_C  l_avars_var;                /* variable for a: scope */
        list_C      l_varlist;                  /* list for a:000 */
        listitem_C[] l_listitems;               /* listitems for a:000 */
        typval_C    rtv;                        /* return value */
        long[]      breakpoint = new long[1];   /* next line with breakpoint or zero */
        int[]       dbg_tick = new int[1];      /* debug_tick when breakpoint was set */
        int         level;                      /* top nesting level of executed function */
        funccall_C  caller;                     /* calling function or null */

        /*private*/ funccall_C()
        {
            l_vars = new dict_C();
            l_vars_var = new dictitem_C();
            l_avars = new dict_C();
            l_avars_var = new dictitem_C();
            l_varlist = new list_C();
            l_listitems = ARRAY_listitem(MAX_FUNC_ARGS);
        }
    }

    /*
     * Info used by a ":for" loop.
     */
    /*private*/ static final class forinfo_C
    {
        int         fi_semicolon;   /* true if ending in '; var]' */
        int         fi_varcount;    /* nr of variables in the list */
        listwatch_C fi_lw;          /* keep an eye on the item used. */
        list_C      fi_list;        /* list being used */

        /*private*/ forinfo_C()
        {
            fi_lw = new listwatch_C();
        }
    }

    /*
     * Struct used by trans_function_name().
     */
    /*private*/ static final class funcdict_C
    {
        dict_C      fd_dict;        /* Dictionary used */
        Bytes       fd_newkey;      /* new key in "dict" in allocated memory */
        dictitem_C  fd_di;          /* Dictionary item used */

        /*private*/ funcdict_C()
        {
        }
    }

    /*private*/ static void ZER0_funcdict(funcdict_C fd)
    {
        fd.fd_dict = null;
        fd.fd_newkey = null;
        fd.fd_di = null;
    }

    /*
     * Array to hold the value of v: variables.
     * The value is in a dictitem, so that it can also be used in the v: scope.
     * The reason to use this table anyway is for very quick access to the
     * variables with the VV_ defines.
     */

    /* values for vv_flags: */
    /*private*/ static final byte VV_RO           = 2;       /* read-only */
    /*private*/ static final byte VV_RO_SBX       = 4;       /* read-only in the sandbox */

    /*private*/ static final class vimvar_C
    {
        Bytes       vv_name;        /* name of variable, without v: */
        dictitem_C  vv_di;          /* value and name for key */
        byte        vv_flags;       /* VV_RO | VV_RO_SBX */

        /*private*/ vimvar_C(Bytes name, dictitem_C di, byte flags)
        {
            vv_name = name;
            vv_di = di;
            vv_flags = flags;
        }
    }

    /*private*/ static vimvar_C new_vimvar(Bytes name, byte type, byte flags)
    {
        dictitem_C di = dictitem_alloc(name);

        if ((flags & VV_RO) != 0)
            di.di_flags = DI_FLAGS_RO | DI_FLAGS_FIX;
        else if ((flags & VV_RO_SBX) != 0)
            di.di_flags = DI_FLAGS_RO_SBX | DI_FLAGS_FIX;
        else
            di.di_flags = DI_FLAGS_FIX;
        di.di_tv.tv_type = type;

        return new vimvar_C(name, di, flags);
    }

    /*private*/ static vimvar_C[] vimvars = new vimvar_C[/*VV_LEN*/]
    {
        /*
         * The order here must match the VV_ defines in vim.h!
         */
        new_vimvar(u8("count"),         VAR_NUMBER,  VV_RO    ),
        new_vimvar(u8("count1"),        VAR_NUMBER,  VV_RO    ),
        new_vimvar(u8("prevcount"),     VAR_NUMBER,  VV_RO    ),
        new_vimvar(u8("errmsg"),        VAR_STRING,  (byte)0  ),
        new_vimvar(u8("warningmsg"),    VAR_STRING,  (byte)0  ),
        new_vimvar(u8("statusmsg"),     VAR_STRING,  (byte)0  ),
        new_vimvar(u8("lnum"),          VAR_NUMBER,  VV_RO_SBX),
        new_vimvar(u8("termresponse"),  VAR_STRING,  VV_RO    ),
        new_vimvar(u8("cmdarg"),        VAR_STRING,  VV_RO    ),
        new_vimvar(u8("dying"),         VAR_NUMBER,  VV_RO    ),
        new_vimvar(u8("exception"),     VAR_STRING,  VV_RO    ),
        new_vimvar(u8("throwpoint"),    VAR_STRING,  VV_RO    ),
        new_vimvar(u8("register"),      VAR_STRING,  VV_RO    ),
        new_vimvar(u8("cmdbang"),       VAR_NUMBER,  VV_RO    ),
        new_vimvar(u8("insertmode"),    VAR_STRING,  VV_RO    ),
        new_vimvar(u8("val"),           VAR_UNKNOWN, VV_RO    ),
        new_vimvar(u8("key"),           VAR_UNKNOWN, VV_RO    ),
        new_vimvar(u8("fcs_reason"),    VAR_STRING,  VV_RO    ),
        new_vimvar(u8("fcs_choice"),    VAR_STRING,  (byte)0  ),
        new_vimvar(u8("scrollstart"),   VAR_STRING,  (byte)0  ),
        new_vimvar(u8("swapcommand"),   VAR_STRING,  VV_RO    ),
        new_vimvar(u8("char"),          VAR_STRING,  (byte)0  ),
        new_vimvar(u8("mouse_win"),     VAR_NUMBER,  (byte)0  ),
        new_vimvar(u8("mouse_lnum"),    VAR_NUMBER,  (byte)0  ),
        new_vimvar(u8("mouse_col"),     VAR_NUMBER,  (byte)0  ),
        new_vimvar(u8("operator"),      VAR_STRING,  VV_RO    ),
        new_vimvar(u8("searchforward"), VAR_NUMBER,  (byte)0  ),
        new_vimvar(u8("hlsearch"),      VAR_NUMBER,  (byte)0  ),
        new_vimvar(u8("oldfiles"),      VAR_LIST,    (byte)0  ),
    };

    /*private*/ static dictitem_C vimvars_var;      /* variable used for v: */

    /*private*/ static final int FNE_INCL_BR     = 1;       /* find_name_end(): include [] in name */
    /*private*/ static final int FNE_CHECK_START = 2;       /* find_name_end(): check name starts with valid character */

    /*
     * Initialize the global and v: variables.
     */
    /*private*/ static void eval_init()
    {
        globvardict = new dict_C();
        globvars_var = new dictitem_C();
        init_var_dict(globvardict, globvars_var, VAR_DEF_SCOPE);

        vimvardict = new dict_C();
        vimvars_var = new dictitem_C();
        init_var_dict(vimvardict, vimvars_var, VAR_SCOPE);
        vimvardict.dv_lock = VAR_FIXED;

        hash_init(func_hashtab);

        for (int i = 0; i < VV_LEN; i++)
        {
            dictitem_C di = vimvars[i].vv_di;

            /* add to v: scope dict, unless the value is not always available */
            if (di.di_tv.tv_type != VAR_UNKNOWN)
                hash_add(vimvardict.dv_hashtab, di, di.di_key);
        }

        set_vim_var_nr(VV_SEARCHFORWARD, 1L);
        set_vim_var_nr(VV_HLSEARCH, 1L);

        set_reg_var(0);                     /* default for v:register is not 0 but '"' */
    }

    /*
     * Return the name of the executed function.
     */
    /*private*/ static Bytes func_name(funccall_C cookie)
    {
        return cookie.func.uf_name;
    }

    /*
     * Return the address holding the next breakpoint line for a funccall cookie.
     */
    /*private*/ static long[] func_breakpoint(funccall_C cookie)
    {
        return cookie.breakpoint;
    }

    /*
     * Return the address holding the debug tick for a funccall cookie.
     */
    /*private*/ static int[] func_dbg_tick(funccall_C cookie)
    {
        return cookie.dbg_tick;
    }

    /*
     * Return the nesting level for a funccall cookie.
     */
    /*private*/ static int func_level(funccall_C cookie)
    {
        return cookie.level;
    }

    /* pointer to funccal for currently active function */
    /*private*/ static funccall_C current_funccal;

    /* Pointer to list of previously used funccal,
     * still around because some item in it is still being used. */
    /*private*/ static funccall_C previous_funccal;

    /*
     * Return true when a function was ended by a ":return" command.
     */
    /*private*/ static boolean current_func_returned()
    {
        return current_funccal.returned;
    }

    /*
     * Set an internal variable to a string value.  Creates the variable if it does not already exist.
     */
    /*private*/ static void set_internal_string_var(Bytes name, Bytes value)
    {
        Bytes val = STRDUP(value);

        typval_C tvp = alloc_string_tv(val);
        if (tvp != null)
        {
            set_var(name, tvp, false);
            free_tv(tvp);
        }
    }

    /*private*/ static Bytes    redir_varname;
    /*private*/ static lval_C   redir_lval;
    /*private*/ static barray_C redir_ba;       /* only valid when redir_lval is not null */
    /*private*/ static Bytes    redir_endp;

    /*
     * Start recording command output to a variable
     * Returns true if successfully completed the setup, false otherwise.
     */
    /*private*/ static boolean var_redir_start(Bytes name, boolean append)
        /* append: append to an existing variable */
    {
        /* Catch a bad name early. */
        if (!eval_isnamec1(name.at(0)))
        {
            emsg(e_invarg);
            return false;
        }

        /* Make a copy of the name, it is used in redir_lval until redir ends. */
        redir_varname = STRDUP(name);

        redir_lval = new lval_C();

        /* The output is stored in growarray "redir_ba" until redirection ends. */
        redir_ba = new barray_C(500);

        /* Parse the variable name (can be a dict or list entry). */
        redir_endp = get_lval(redir_varname, null, redir_lval, false, false, 0, FNE_CHECK_START);
        if (redir_endp == null || redir_lval.ll_name == null || redir_endp.at(0) != NUL)
        {
            clear_lval(redir_lval);
            if (redir_endp != null && redir_endp.at(0) != NUL)
                /* Trailing characters are present after the variable name. */
                emsg(e_trailing);
            else
                emsg(e_invarg);
            redir_endp = null;  /* don't store a value, only cleanup */
            var_redir_stop();
            return false;
        }

        /* check if we can write to the variable: set it to or append an empty string */
        boolean save_emsg = did_emsg;
        did_emsg = false;
        typval_C tv = new typval_C();
        tv.tv_type = VAR_STRING;
        tv.tv_string = u8("");
        set_var_lval(redir_lval, redir_endp, tv, true, append ? u8(".") : u8("="));
        clear_lval(redir_lval);
        boolean err = did_emsg;
        did_emsg |= save_emsg;
        if (err)
        {
            redir_endp = null;  /* don't store a value, only cleanup */
            var_redir_stop();
            return false;
        }

        return true;
    }

    /*
     * Append "value[value_len]" to the variable set by var_redir_start().
     * The actual appending is postponed until redirection ends, because the value
     * appended may in fact be the string we write to, changing it may cause freed
     * memory to be used:
     *   :redir => foo
     *   :let foo
     *   :redir END
     */
    /*private*/ static void var_redir_str(Bytes value, int value_len)
    {
        if (redir_lval == null)
            return;

        int len;
        if (value_len == -1)
            len = strlen(value);       /* append the entire string */
        else
            len = value_len;                /* append only "value_len" characters */

        ba_grow(redir_ba, len);
        ACOPY(redir_ba.ba_data, redir_ba.ba_len, value.array, value.index, len);
        redir_ba.ba_len += len;
    }

    /*
     * Stop redirecting command output to a variable.
     * Frees the allocated memory.
     */
    /*private*/ static void var_redir_stop()
    {
        if (redir_lval != null)
        {
            /* If there was no error: assign the text to the variable. */
            if (redir_endp != null)
            {
                typval_C tv = new typval_C();
                ba_append(redir_ba, NUL);   /* Append the trailing NUL. */
                tv.tv_type = VAR_STRING;
                tv.tv_string = new Bytes(redir_ba.ba_data);
                /* Call get_lval() again, if it's inside a Dict or List it may have changed. */
                redir_endp = get_lval(redir_varname, null, redir_lval, false, false, 0, FNE_CHECK_START);
                if (redir_endp != null && redir_lval.ll_name != null)
                    set_var_lval(redir_lval, redir_endp, tv, false, u8("."));
                clear_lval(redir_lval);
            }

            /* free the collected output */
            redir_ba.ba_data = null;
            redir_ba = null;

            redir_lval = null;
        }
        redir_varname = null;
    }

    /*
     * Top level evaluation function, returning a boolean.
     * Sets "error" to true if there was an error.
     * Return true or false.
     */
    /*private*/ static boolean eval_to_bool(Bytes arg, boolean[] error, Bytes[] nextcmd, boolean skip)
        /* skip: only parse, don't execute */
    {
        boolean result = false;

        if (skip)
            emsg_skip++;

        typval_C tv = new typval_C();
        if (eval0(arg, tv, nextcmd, !skip) == false)
            error[0] = true;
        else
        {
            error[0] = false;
            if (!skip)
            {
                result = (get_tv_number_chk(tv, error) != 0);
                clear_tv(tv);
            }
        }

        if (skip)
            --emsg_skip;

        return result;
    }

    /*
     * Top level evaluation function, returning a string.  If "skip" is true,
     * only parsing to "nextcmd" is done, without reporting errors.  Return
     * pointer to allocated memory, or null for failure or when "skip" is true.
     */
    /*private*/ static Bytes eval_to_string_skip(Bytes arg, Bytes[] nextcmd, boolean skip)
        /* skip: only parse, don't execute */
    {
        Bytes retval;

        if (skip)
            emsg_skip++;

        typval_C tv = new typval_C();
        if (eval0(arg, tv, nextcmd, !skip) == false || skip)
            retval = null;
        else
        {
            retval = STRDUP(get_tv_string(tv));
            clear_tv(tv);
        }

        if (skip)
            --emsg_skip;

        return retval;
    }

    /*
     * Skip over an expression at "*pp".
     * Return false for an error, true otherwise.
     */
    /*private*/ static boolean skip_expr(Bytes[] pp)
    {
        pp[0] = skipwhite(pp[0]);

        typval_C rtv = new typval_C();
        return eval1(pp, rtv, false);
    }

    /*
     * Top level evaluation function, returning a string.
     * When "convert" is true convert a List into a sequence of lines.
     * Return pointer to allocated memory, or null for failure.
     */
    /*private*/ static Bytes eval_to_string(Bytes arg, Bytes[] nextcmd, boolean convert)
    {
        Bytes retval;

        typval_C tv = new typval_C();
        if (eval0(arg, tv, nextcmd, true) == false)
            retval = null;
        else
        {
            if (convert && tv.tv_type == VAR_LIST)
            {
                barray_C ba = new barray_C(80);
                if (tv.tv_list != null)
                {
                    list_join(ba, tv.tv_list, u8("\n"), true, 0);
                    if (0 < tv.tv_list.lv_len)
                        ba_append(ba, NL);
                }
                ba_append(ba, NUL);
                retval = new Bytes(ba.ba_data);
            }
            else
                retval = STRDUP(get_tv_string(tv));
            clear_tv(tv);
        }

        return retval;
    }

    /*
     * Call eval_to_string() without using current local variables and using textlock.
     * When "use_sandbox" is true use the sandbox.
     */
    /*private*/ static Bytes eval_to_string_safe(Bytes arg, Bytes[] nextcmd, boolean use_sandbox)
    {
        Bytes retval;
        funccall_C save_funccalp = save_funccal();
        if (use_sandbox)
            sandbox++;
        textlock++;
        retval = eval_to_string(arg, nextcmd, false);
        if (use_sandbox)
            --sandbox;
        --textlock;
        restore_funccal(save_funccalp);
        return retval;
    }

    /*
     * Top level evaluation function, returning a number.
     * Evaluates "expr" silently.
     * Returns -1 for an error.
     */
    /*private*/ static int eval_to_number(Bytes expr)
    {
        int retval;

        Bytes[] p = { skipwhite(expr) };

        emsg_off++;

        typval_C rtv = new typval_C();
        if (eval1(p, rtv, true) == false)
            retval = -1;
        else
        {
            retval = (int)get_tv_number_chk(rtv, null);
            clear_tv(rtv);
        }

        --emsg_off;

        return retval;
    }

    /*
     * Prepare v: variable "idx" to be used.
     * Save the current typeval in "save_tv".
     * When not used yet add the variable to the v: hashtable.
     */
    /*private*/ static void prepare_vimvar(int idx, typval_C save_tv)
    {
        dictitem_C di = vimvars[idx].vv_di;

        COPY_typval(save_tv, di.di_tv);
        if (di.di_tv.tv_type == VAR_UNKNOWN)
            hash_add(vimvardict.dv_hashtab, di, di.di_key);
    }

    /*
     * Restore v: variable "idx" to typeval "save_tv".
     * When no longer defined, remove the variable from the v: hashtable.
     */
    /*private*/ static void restore_vimvar(int idx, typval_C save_tv)
    {
        dictitem_C di = vimvars[idx].vv_di;

        COPY_typval(di.di_tv, save_tv);
        if (di.di_tv.tv_type == VAR_UNKNOWN)
        {
            hashitem_C hi = hash_find(vimvardict.dv_hashtab, di.di_key);
            if (hashitem_empty(hi))
                emsg2(e_intern2, u8("restore_vimvar()"));
            else
                hash_remove(vimvardict.dv_hashtab, hi);
        }
    }

    /*
     * Top level evaluation function.
     * Returns an allocated typval_C with the result.
     * Returns null when there is an error.
     */
    /*private*/ static typval_C eval_expr(Bytes arg, Bytes[] nextcmd)
    {
        typval_C tv = new typval_C();

        if (eval0(arg, tv, nextcmd, true) == false)
            tv = null;

        return tv;
    }

    /*
     * Call some vimL function and return the result in "*rtv".
     * Uses argv[argc] for the function arguments.
     * Only Number and String arguments are currently supported.
     * Returns true or false.
     */
    /*private*/ static boolean call_vim_function(Bytes func, int argc, Bytes[] argv, boolean safe, boolean str_arg_only, typval_C rtv)
        /* safe: use the sandbox */
        /* str_arg_only: all arguments are strings */
    {
        long[] n = new long[1];

        typval_C[] argvars = ARRAY_typval(argc + 1);

        for (int i = 0; i < argc; i++)
        {
            /* Pass a null or empty argument as an empty string. */
            if (argv[i] == null || argv[i].at(0) == NUL)
            {
                argvars[i].tv_type = VAR_STRING;
                argvars[i].tv_string = u8("");
                continue;
            }

            int[] len = new int[1];
            if (str_arg_only)
                len[0] = 0;
            else
                /* Recognize a number argument, the others must be strings. */
                vim_str2nr(argv[i], null, len, TRUE, TRUE, n);
            if (len[0] != 0 && len[0] == strlen(argv[i]))
            {
                argvars[i].tv_type = VAR_NUMBER;
                argvars[i].tv_number = n[0];
            }
            else
            {
                argvars[i].tv_type = VAR_STRING;
                argvars[i].tv_string = argv[i];
            }
        }

        funccall_C save_funccalp = null;
        if (safe)
        {
            save_funccalp = save_funccal();
            sandbox++;
        }

        rtv.tv_type = VAR_UNKNOWN;            /* clear_tv() uses this */

        boolean[] doesrange = new boolean[1];
        boolean ret = call_func(func, strlen(func), rtv, argc, argvars,
                                    curwin.w_cursor.lnum, curwin.w_cursor.lnum, doesrange, true, null);

        if (safe)
        {
            --sandbox;
            restore_funccal(save_funccalp);
        }

        if (ret == false)
            clear_tv(rtv);

        return ret;
    }

    /*
     * Call vimL function "func" and return the result as a number.
     * Returns -1 when calling the function fails.
     * Uses argv[argc] for the function arguments.
     */
    /*private*/ static long call_func_retnr(Bytes func, int argc, Bytes[] argv, boolean safe)
        /* safe: use the sandbox */
    {
        typval_C rtv = new typval_C();

        /* All arguments are passed as strings, no conversion to number. */
        if (call_vim_function(func, argc, argv, safe, true, rtv) == false)
            return -1;

        long retval = get_tv_number_chk(rtv, null);
        clear_tv(rtv);
        return retval;
    }

    /*
     * Call vimL function "func" and return the result as a string.
     * Returns null when calling the function fails.
     * Uses argv[argc] for the function arguments.
     */
    /*private*/ static Bytes call_func_retstr(Bytes func, int argc, Bytes[] argv, boolean safe)
        /* safe: use the sandbox */
    {
        typval_C rtv = new typval_C();

        /* All arguments are passed as strings, no conversion to number. */
        if (call_vim_function(func, argc, argv, safe, true, rtv) == false)
            return null;

        Bytes s = STRDUP(get_tv_string(rtv));
        clear_tv(rtv);
        return s;
    }

    /*
     * Call vimL function "func" and return the result as a List.
     * Uses argv[argc] for the function arguments.
     * Returns null when there is something wrong.
     */
    /*private*/ static list_C call_func_retlist(Bytes func, int argc, Bytes[] argv, boolean safe)
        /* safe: use the sandbox */
    {
        typval_C rtv = new typval_C();

        /* All arguments are passed as strings, no conversion to number. */
        if (call_vim_function(func, argc, argv, safe, true, rtv) == false)
            return null;

        if (rtv.tv_type != VAR_LIST)
        {
            clear_tv(rtv);
            return null;
        }

        return rtv.tv_list;
    }

    /*
     * Save the current function call pointer, and set it to null.
     * Used when executing autocommands and for ":source".
     */
    /*private*/ static funccall_C save_funccal()
    {
        funccall_C fc = current_funccal;
        current_funccal = null;
        return fc;
    }

    /*private*/ static void restore_funccal(funccall_C fc)
    {
        current_funccal = fc;
    }

    /*
     * ":let"                       list all variable values
     * ":let var1 var2"             list variable values
     * ":let var = expr"            assignment command.
     * ":let var += expr"           assignment command.
     * ":let var -= expr"           assignment command.
     * ":let var .= expr"           assignment command.
     * ":let [var1, var2] = expr"   unpack list.
     */
    /*private*/ static final ex_func_C ex_let = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes arg = eap.arg;
            Bytes expr = null;
            int[] var_count = { 0 };
            int[] semicolon = { 0 };

            Bytes argend = skip_var_list(arg, var_count, semicolon);
            if (argend == null)
                return;
            if (BLT(arg, argend) && argend.at(-1) == (byte)'.')  /* for var.='str' */
                argend = argend.minus(1);
            expr = skipwhite(argend);
            if (expr.at(0) != (byte)'=' && !(vim_strbyte(u8("+-."), expr.at(0)) != null && expr.at(1) == (byte)'='))
            {
                boolean[] first = { true };

                /* ":let" without "=": list variables */
                if (arg.at(0) == (byte)'[')
                    emsg(e_invarg);
                else if (!ends_excmd(arg.at(0)))
                    /* ":let var1 var2" */
                    arg = list_arg_vars(eap, arg, first);
                else if (!eap.skip)
                {
                    /* ":let" */
                    list_glob_vars(first);
                    list_buf_vars(first);
                    list_win_vars(first);
                    list_tab_vars(first);
                    list_script_vars(first);
                    list_func_vars(first);
                    list_vim_vars(first);
                }
                eap.nextcmd = check_nextcmd(arg);
            }
            else
            {
                Bytes op = new Bytes(2);
                op.be(0, (byte)'=');
                op.be(1, NUL);
                if (expr.at(0) != (byte)'=')
                {
                    if (vim_strbyte(u8("+-."), expr.at(0)) != null)
                        op.be(0, expr.at(0)); /* +=, -= or .= */
                    expr = skipwhite(expr.plus(2));
                }
                else
                    expr = skipwhite(expr.plus(1));

                if (eap.skip)
                    emsg_skip++;

                typval_C rtv = new typval_C();
                boolean b;
                { Bytes[] __ = { eap.nextcmd }; b = eval0(expr, rtv, __, !eap.skip); eap.nextcmd = __[0]; }
                if (eap.skip)
                {
                    if (b != false)
                        clear_tv(rtv);
                    --emsg_skip;
                }
                else if (b != false)
                {
                    ex_let_vars(eap.arg, rtv, false, semicolon[0], var_count[0], op);
                    clear_tv(rtv);
                }
            }
        }
    };

    /*
     * Assign the typevalue "tv" to the variable or variables at "arg_start".
     * Handles both "var" with any type and "[var, var; var]" with a list type.
     * When "nextchars" is not null, it points to a string with characters that
     * must appear after the variable(s).
     * Use "+", "-" or "." for add, subtract or concatenate.
     * Returns true or false;
     */
    /*private*/ static boolean ex_let_vars(Bytes arg_start, typval_C tv, boolean copy, int semicolon, int var_count, Bytes nextchars)
        /* copy: copy values from "tv", don't move */
        /* semicolon: from skip_var_list() */
        /* var_count: from skip_var_list() */
    {
        Bytes arg = arg_start;
        if (arg.at(0) != (byte)'[')
        {
            /*
             * ":let var = expr" or ":for var in list"
             */
            if (ex_let_one(arg, tv, copy, nextchars, nextchars) == null)
                return false;

            return true;
        }

        list_C list;
        /*
         * ":let [v1, v2] = list" or ":for [v1, v2] in listlist"
         */
        if (tv.tv_type != VAR_LIST || (list = tv.tv_list) == null)
        {
            emsg(e_listreq);
            return false;
        }

        int len = list_len(list);
        if (semicolon == 0 && var_count < len)
        {
            emsg(u8("E687: Less targets than List items"));
            return false;
        }
        if (len < var_count - semicolon)
        {
            emsg(u8("E688: More targets than List items"));
            return false;
        }

        for (listitem_C item = list.lv_first; arg.at(0) != (byte)']'; )
        {
            arg = skipwhite(arg.plus(1));
            arg = ex_let_one(arg, item.li_tv, true, u8(",;]"), nextchars);
            item = item.li_next;
            if (arg == null)
                return false;

            arg = skipwhite(arg);
            if (arg.at(0) == (byte)';')
            {
                /* Put the rest of the list (may be empty) in the var after ';'.
                 * Create a new list for this. */
                list = new list_C();

                while (item != null)
                {
                    list_append_tv(list, item.li_tv);
                    item = item.li_next;
                }

                typval_C ltv = new typval_C();
                ltv.tv_type = VAR_LIST;
                ltv.tv_lock = 0;
                ltv.tv_list = list;
                list.lv_refcount = 1;

                arg = ex_let_one(skipwhite(arg.plus(1)), ltv, false, u8("]"), nextchars);

                clear_tv(ltv);

                if (arg == null)
                    return false;
                break;
            }
            else if (arg.at(0) != (byte)',' && arg.at(0) != (byte)']')
            {
                emsg2(e_intern2, u8("ex_let_vars()"));
                return false;
            }
        }

        return true;
    }

    /*
     * Skip over assignable variable "var" or list of variables "[var, var]".
     * Used for ":let varvar = expr" and ":for varvar in expr".
     * For "[var, var]" increment "*var_count" for each variable.
     * for "[var, var; var]" set "semicolon".
     * Return null for an error.
     */
    /*private*/ static Bytes skip_var_list(Bytes arg, int[] var_count, int[] semicolon)
    {
        if (arg.at(0) == (byte)'[')
        {
            /* "[var, var]": find the matching ']'. */
            Bytes p = arg;
            for ( ; ; )
            {
                p = skipwhite(p.plus(1));       /* skip whites after '[', ';' or ',' */
                Bytes s = skip_var_one(p);
                if (BEQ(s, p))
                {
                    emsg2(e_invarg2, p);
                    return null;
                }
                ++var_count[0];

                p = skipwhite(s);
                if (p.at(0) == (byte)']')
                    break;
                else if (p.at(0) == (byte)';')
                {
                    if (semicolon[0] == 1)
                    {
                        emsg(u8("Double ; in list of variables"));
                        return null;
                    }
                    semicolon[0] = 1;
                }
                else if (p.at(0) != (byte)',')
                {
                    emsg2(e_invarg2, p);
                    return null;
                }
            }
            return p.plus(1);
        }

        return skip_var_one(arg);
    }

    /*
     * Skip one (assignable) variable name, including @r, $VAR, &option, d.key, l[idx].
     */
    /*private*/ static Bytes skip_var_one(Bytes arg)
    {
        if (arg.at(0) == (byte)'@' && arg.at(1) != NUL)
            return arg.plus(2);

        return find_name_end(arg.at(0) == (byte)'$' || arg.at(0) == (byte)'&' ? arg.plus(1) : arg, null, null, FNE_INCL_BR | FNE_CHECK_START);
    }

    /*
     * List variables for hashtab "ht" with prefix "prefix".
     * If "empty" is true also list null strings as empty strings.
     */
    /*private*/ static void list_hashtable_vars(hashtab_C ht, Bytes prefix, boolean empty, boolean[] first)
    {
        for (int i = 0, todo = (int)ht.ht_used; 0 < todo && !got_int; i++)
        {
            hashitem_C hi = ht.ht_buckets[i];
            if (!hashitem_empty(hi))
            {
                dictitem_C di = (dictitem_C)hi.hi_data;
                if (empty || di.di_tv.tv_type != VAR_STRING || di.di_tv.tv_string != null)
                    list_one_var(di, prefix, first);
                --todo;
            }
        }
    }

    /*
     * List global variables.
     */
    /*private*/ static void list_glob_vars(boolean[] first)
    {
        list_hashtable_vars(globvardict.dv_hashtab, u8(""), true, first);
    }

    /*
     * List buffer variables.
     */
    /*private*/ static void list_buf_vars(boolean[] first)
    {
        Bytes numbuf = new Bytes(NUMBUFLEN);

        list_hashtable_vars(curbuf.b_vars.dv_hashtab, u8("b:"), true, first);

        libC.sprintf(numbuf, u8("%ld"), (long)curbuf.b_changedtick);
        list_one_var_a(u8("b:"), u8("changedtick"), VAR_NUMBER, numbuf, first);
    }

    /*
     * List window variables.
     */
    /*private*/ static void list_win_vars(boolean[] first)
    {
        list_hashtable_vars(curwin.w_vars.dv_hashtab, u8("w:"), true, first);
    }

    /*
     * List tab page variables.
     */
    /*private*/ static void list_tab_vars(boolean[] first)
    {
        list_hashtable_vars(curtab.tp_vars.dv_hashtab, u8("t:"), true, first);
    }

    /*
     * List Vim variables.
     */
    /*private*/ static void list_vim_vars(boolean[] first)
    {
        list_hashtable_vars(vimvardict.dv_hashtab, u8("v:"), false, first);
    }

    /*
     * List script-local variables, if there is a script.
     */
    /*private*/ static void list_script_vars(boolean[] first)
    {
        if (0 < current_SID && current_SID <= ga_scripts.ga_len)
        {
            scriptvar_C sv = ga_scripts.ga_data[current_SID - 1];
            list_hashtable_vars(sv.sv_dict.dv_hashtab, u8("s:"), false, first);
        }
    }

    /*
     * List function variables, if there is a function.
     */
    /*private*/ static void list_func_vars(boolean[] first)
    {
        if (current_funccal != null)
            list_hashtable_vars(current_funccal.l_vars.dv_hashtab, u8("l:"), false, first);
    }

    /*
     * List variables in "arg".
     */
    /*private*/ static Bytes list_arg_vars(exarg_C eap, Bytes _arg, boolean[] first)
    {
        Bytes[] arg = { _arg };
        boolean error = false;

        while (!ends_excmd(arg[0].at(0)) && !got_int)
        {
            if (error || eap.skip)
            {
                arg[0] = find_name_end(arg[0], null, null, FNE_INCL_BR | FNE_CHECK_START);
                if (!vim_iswhite(arg[0].at(0)) && !ends_excmd(arg[0].at(0)))
                {
                    emsg_severe = true;
                    emsg(e_trailing);
                    break;
                }
            }
            else
            {
                /* get_name_len() takes care of expanding curly braces */
                Bytes name = arg[0];
                Bytes name_start = name;
                Bytes[] tofree = new Bytes[1];
                int len = get_name_len(arg, tofree, true, true);
                if (len <= 0)
                {
                    /* This is mainly to keep test 49 working:
                     * when expanding curly braces fails, overrule the exception error message. */
                    if (len < 0 && !aborting())
                    {
                        emsg_severe = true;
                        emsg2(e_invarg2, arg[0]);
                        break;
                    }
                    error = true;
                }
                else
                {
                    if (tofree[0] != null)
                        name = tofree[0];

                    typval_C tv = new typval_C();
                    if (!get_var_tv(name, len, tv, true, false))
                        error = true;
                    else
                    {
                        /* handle d.key, l[idx], f(expr) */
                        Bytes arg_subsc = arg[0];
                        if (!handle_subscript(arg, tv, true, true))
                            error = true;
                        else
                        {
                            if (BEQ(arg[0], arg_subsc) && len == 2 && name.at(1) == (byte)':')
                            {
                                switch (name.at(0))
                                {
                                    case 'g': list_glob_vars(first); break;
                                    case 'b': list_buf_vars(first); break;
                                    case 'w': list_win_vars(first); break;
                                    case 't': list_tab_vars(first); break;
                                    case 'v': list_vim_vars(first); break;
                                    case 's': list_script_vars(first); break;
                                    case 'l': list_func_vars(first); break;
                                    default:
                                        emsg2(u8("E738: Can't list variables for %s"), name);
                                        break;
                                }
                            }
                            else
                            {
                                Bytes s = echo_string(tv, 0);
                                byte c = arg[0].at(0);

                                arg[0].be(0, NUL);
                                list_one_var_a(u8(""),
                                        BEQ(arg[0], arg_subsc) ? name : name_start,
                                        tv.tv_type,
                                        (s == null) ? u8("") : s,
                                        first);
                                arg[0].be(0, c);
                            }
                            clear_tv(tv);
                        }
                    }
                }
            }

            arg[0] = skipwhite(arg[0]);
        }

        return arg[0];
    }

    /*
     * Set one item of ":let var = expr" or ":let [v1, v2] = list" to its value.
     * Returns a pointer to the char just after the var name.
     * Returns null if there is an error.
     */
    /*private*/ static Bytes ex_let_one(Bytes _arg, typval_C tv, boolean copy, Bytes endchars, Bytes op)
        /* arg: points to variable name */
        /* tv: value to assign to variable */
        /* copy: copy value from "tv" */
        /* endchars: valid chars after variable name  or null */
        /* op: "+", "-", "."  or null */
    {
        Bytes[] arg = { _arg };
        Bytes arg_end = null;

        /*
         * ":let &option = expr": Set option value.
         * ":let &l:option = expr": Set local option value.
         * ":let &g:option = expr": Set global option value.
         */
        if (arg[0].at(0) == (byte)'&')
        {
            int[] opt_flags = new int[1];

            /* Find the end of the name. */
            Bytes p = find_option_end(arg, opt_flags);
            if (p == null || (endchars != null && vim_strchr(endchars, skipwhite(p).at(0)) == null))
                emsg(e_letunexp);
            else
            {
                Bytes[] stringval = { null };
                byte c1 = p.at(0);
                p.be(0, NUL);

                long n = get_tv_number(tv);
                Bytes s = get_tv_string_chk(tv);       /* != null if number or string */
                if (s != null && op != null && op.at(0) != (byte)'=')
                {
                    long[] numval = new long[1];
                    int opt_type = get_option_value(arg[0], numval, stringval, opt_flags[0]);
                    if ((opt_type == 1 && op.at(0) == (byte)'.') || (opt_type == 0 && op.at(0) != (byte)'.'))
                        emsg2(e_letwrong, op);
                    else
                    {
                        if (opt_type == 1)                              /* number */
                        {
                            if (op.at(0) == (byte)'+')
                                n = numval[0] + n;
                            else
                                n = numval[0] - n;
                        }
                        else if (opt_type == 0 && stringval[0] != null)    /* string */
                        {
                            s = concat_str(stringval[0], s);
                            stringval[0] = s;
                        }
                    }
                }
                if (s != null)
                {
                    set_option_value(arg[0], n, s, opt_flags[0]);
                    arg_end = p;
                }

                p.be(0, c1);
            }
        }

        /*
         * ":let @r = expr": Set register contents.
         */
        else if (arg[0].at(0) == (byte)'@')
        {
            arg[0] = arg[0].plus(1);
            if (op != null && (op.at(0) == (byte)'+' || op.at(0) == (byte)'-'))
                emsg2(e_letwrong, op);
            else if (endchars != null && vim_strchr(endchars, skipwhite(arg[0].plus(1)).at(0)) == null)
                emsg(e_letunexp);
            else
            {
                Bytes tofree = null;

                Bytes p = get_tv_string_chk(tv);
                if (p != null && op != null && op.at(0) == (byte)'.')
                {
                    Bytes s = (Bytes)get_reg_contents(arg[0].at(0) == (byte)'@' ? '"' : arg[0].at(0), GREG_EXPR_SRC);
                    if (s != null)
                        p = tofree = concat_str(s, p);
                }
                if (p != null)
                {
                    write_reg_contents(arg[0].at(0) == (byte)'@' ? '"' : arg[0].at(0), p, -1, false);
                    arg_end = arg[0].plus(1);
                }
            }
        }

        /*
         * ":let var = expr": Set internal variable.
         * ":let {expr} = expr": Idem, name made with curly braces
         */
        else if (eval_isnamec1(arg[0].at(0)) || arg[0].at(0) == (byte)'{')
        {
            lval_C lv = new lval_C();

            Bytes p = get_lval(arg[0], tv, lv, false, false, 0, FNE_CHECK_START);
            if (p != null && lv.ll_name != null)
            {
                if (endchars != null && vim_strchr(endchars, skipwhite(p).at(0)) == null)
                    emsg(e_letunexp);
                else
                {
                    set_var_lval(lv, p, tv, copy, op);
                    arg_end = p;
                }
            }

            clear_lval(lv);
        }

        else
            emsg2(e_invarg2, arg[0]);

        return arg_end;
    }

    /*
     * If "arg" is equal to "b:changedtick" give an error and return true.
     */
    /*private*/ static boolean check_changedtick(Bytes arg)
    {
        if (STRNCMP(arg, u8("b:changedtick"), 13) == 0 && !eval_isnamec(arg.at(13)))
        {
            emsg2(e_readonlyvar, arg);
            return true;
        }
        return false;
    }

    /*
     * Get an lval: variable, Dict item or List item that can be assigned a value
     * to: "name", "na{me}", "name[expr]", "name[expr:expr]", "name[expr][expr]",
     * "name.key", "name.key[expr]" etc.
     * Indexing only works if "name" is an existing List or Dictionary.
     * "name" points to the start of the name.
     * If "rtv" is not null it points to the value to be assigned.
     * "unlet" is true for ":unlet": slightly different behavior when something is
     * wrong; must end in space or cmd separator.
     *
     * flags:
     *  GLV_QUIET:       do not give error messages
     *  GLV_NO_AUTOLOAD: do not use script autoloading
     *
     * Returns a pointer to just after the name, including indexes.
     * When an evaluation error occurs "lp.ll_name" is null;
     * Returns null for a parsing error.  Still need to free items in "lp"!
     */
    /*private*/ static Bytes get_lval(Bytes name, typval_C rtv, lval_C lp, boolean unlet, boolean skip, int flags, int fne_flags)
        /* flags: GLV_ values */
        /* fne_flags: flags for find_name_end() */
    {
        /* Clear everything in "lp". */
        ZER0_lval(lp);

        if (skip)
        {
            /* When skipping just find the end of the name. */
            lp.ll_name = name;
            return find_name_end(name, null, null, FNE_INCL_BR | fne_flags);
        }

        boolean quiet = ((flags & GLV_QUIET) != 0);

        /* Find the end of the name. */
        Bytes[] expr_start = new Bytes[1];
        Bytes[] expr_end = new Bytes[1];
        Bytes[] p = { find_name_end(name, expr_start, expr_end, fne_flags) };
        if (expr_start[0] != null)
        {
            /* Don't expand the name when we already know there is an error. */
            if (unlet && !vim_iswhite(p[0].at(0)) && !ends_excmd(p[0].at(0)) && p[0].at(0) != (byte)'[' && p[0].at(0) != (byte)'.')
            {
                emsg(e_trailing);
                return null;
            }

            lp.ll_exp_name = make_expanded_name(name, expr_start[0], expr_end[0], p[0]);
            if (lp.ll_exp_name == null)
            {
                /* Report an invalid expression in braces, unless the
                 * expression evaluation has been cancelled due to an
                 * aborting error, an interrupt, or an exception. */
                if (!aborting() && !quiet)
                {
                    emsg_severe = true;
                    emsg2(e_invarg2, name);
                    return null;
                }
            }
            lp.ll_name = lp.ll_exp_name;
        }
        else
            lp.ll_name = name;

        /* Without [idx] or .key we are done. */
        if ((p[0].at(0) != (byte)'[' && p[0].at(0) != (byte)'.') || lp.ll_name == null)
            return p[0];

        byte cc = p[0].at(0);
        p[0].be(0, NUL);
        hashtab_C[] ht = new hashtab_C[1];
        dictitem_C v = find_var(lp.ll_name, ht, (flags & GLV_NO_AUTOLOAD) != 0);
        if (v == null && !quiet)
            emsg2(e_undefvar, lp.ll_name);
        p[0].be(0, cc);
        if (v == null)
            return null;

        typval_C var1 = new typval_C();
        typval_C var2 = new typval_C();

        boolean empty1 = false;
        Bytes key = null;

        /*
         * Loop until no more [idx] or .key is following.
         */
        lp.ll_tv = v.di_tv;
        while (p[0].at(0) == (byte)'[' || (p[0].at(0) == (byte)'.' && lp.ll_tv.tv_type == VAR_DICT))
        {
            if (!(lp.ll_tv.tv_type == VAR_LIST && lp.ll_tv.tv_list != null)
                    && !(lp.ll_tv.tv_type == VAR_DICT && lp.ll_tv.tv_dict != null))
            {
                if (!quiet)
                    emsg(u8("E689: Can only index a List or Dictionary"));
                return null;
            }
            if (lp.ll_range)
            {
                if (!quiet)
                    emsg(u8("E708: [:] must come last"));
                return null;
            }

            int len = -1;
            if (p[0].at(0) == (byte)'.')
            {
                key = p[0].plus(1);
                for (len = 0; asc_isalnum(key.at(len)) || key.at(len) == (byte)'_'; len++)
                    ;
                if (len == 0)
                {
                    if (!quiet)
                        emsg(e_emptykey);
                    return null;
                }
                p[0] = key.plus(len);
            }
            else
            {
                /* Get the index [expr] or the first index [expr: ]. */
                p[0] = skipwhite(p[0].plus(1));
                if (p[0].at(0) == (byte)':')
                    empty1 = true;
                else
                {
                    empty1 = false;
                    if (eval1(p, var1, true) == false)         /* recursive! */
                        return null;
                    if (get_tv_string_chk(var1) == null)
                    {
                        /* not a number or string */
                        clear_tv(var1);
                        return null;
                    }
                }

                /* Optionally get the second index [ :expr]. */
                if (p[0].at(0) == (byte)':')
                {
                    if (lp.ll_tv.tv_type == VAR_DICT)
                    {
                        if (!quiet)
                            emsg(e_dictrange);
                        if (!empty1)
                            clear_tv(var1);
                        return null;
                    }
                    if (rtv != null && (rtv.tv_type != VAR_LIST || rtv.tv_list == null))
                    {
                        if (!quiet)
                            emsg(u8("E709: [:] requires a List value"));
                        if (!empty1)
                            clear_tv(var1);
                        return null;
                    }
                    p[0] = skipwhite(p[0].plus(1));
                    if (p[0].at(0) == (byte)']')
                        lp.ll_empty2 = true;
                    else
                    {
                        lp.ll_empty2 = false;
                        if (eval1(p, var2, true) == false)     /* recursive! */
                        {
                            if (!empty1)
                                clear_tv(var1);
                            return null;
                        }
                        if (get_tv_string_chk(var2) == null)
                        {
                            /* not a number or string */
                            if (!empty1)
                                clear_tv(var1);
                            clear_tv(var2);
                            return null;
                        }
                    }
                    lp.ll_range = true;
                }
                else
                    lp.ll_range = false;

                if (p[0].at(0) != (byte)']')
                {
                    if (!quiet)
                        emsg(e_missbrac);
                    if (!empty1)
                        clear_tv(var1);
                    if (lp.ll_range && !lp.ll_empty2)
                        clear_tv(var2);
                    return null;
                }

                /* Skip to past ']'. */
                p[0] = p[0].plus(1);
            }

            if (lp.ll_tv.tv_type == VAR_DICT)
            {
                if (len == -1)
                {
                    /* "[key]": get key from "var1" */
                    key = get_tv_string(var1);      /* is number or string */
                    if (key.at(0) == NUL)
                    {
                        if (!quiet)
                            emsg(e_emptykey);
                        clear_tv(var1);
                        return null;
                    }
                }
                lp.ll_list = null;
                lp.ll_dict = lp.ll_tv.tv_dict;
                lp.ll_di = dict_find(lp.ll_dict, key, len);

                /* When assigning to a scope dictionary check that a function and
                 * variable name is valid (only variable name unless it is l: or
                 * g: dictionary).  Disallow overwriting a builtin function. */
                if (rtv != null && lp.ll_dict.dv_scope != 0)
                {
                    int prevval;
                    if (len != -1)
                    {
                        prevval = key.at(len);
                        key.be(len, NUL);
                    }
                    else
                        prevval = 0;
                    boolean wrong = (lp.ll_dict.dv_scope == VAR_DEF_SCOPE
                                   && rtv.tv_type == VAR_FUNC
                                   && var_check_func_name(key, lp.ll_di == null))
                            || !valid_varname(key);
                    if (len != -1)
                        key.be(len, prevval);
                    if (wrong)
                        return null;
                }

                if (lp.ll_di == null)
                {
                    /* Can't add "v:" variable. */
                    if (lp.ll_dict == vimvardict)
                    {
                        emsg2(e_illvar, name);
                        return null;
                    }

                    /* Key does not exist in dict: may need to add it. */
                    if (p[0].at(0) == (byte)'[' || p[0].at(0) == (byte)'.' || unlet)
                    {
                        if (!quiet)
                            emsg2(e_dictkey, key);
                        if (len == -1)
                            clear_tv(var1);
                        return null;
                    }
                    if (len == -1)
                        lp.ll_newkey = STRDUP(key);
                    else
                        lp.ll_newkey = STRNDUP(key, len);
                    if (len == -1)
                        clear_tv(var1);
                    if (lp.ll_newkey == null)
                        p[0] = null;
                    break;
                }
                /* existing variable, need to check if it can be changed */
                else if (var_check_ro(lp.ll_di.di_flags, name))
                    return null;

                if (len == -1)
                    clear_tv(var1);
                lp.ll_tv = lp.ll_di.di_tv;
            }
            else
            {
                /*
                 * Get the number and item for the only or first index of the List.
                 */
                if (empty1)
                    lp.ll_n1 = 0;
                else
                {
                    lp.ll_n1 = get_tv_number(var1); /* is number or string */
                    clear_tv(var1);
                }
                lp.ll_dict = null;
                lp.ll_list = lp.ll_tv.tv_list;
                lp.ll_li = list_find(lp.ll_list, lp.ll_n1);
                if (lp.ll_li == null)
                {
                    if (lp.ll_n1 < 0)
                    {
                        lp.ll_n1 = 0;
                        lp.ll_li = list_find(lp.ll_list, lp.ll_n1);
                    }
                }
                if (lp.ll_li == null)
                {
                    if (lp.ll_range && !lp.ll_empty2)
                        clear_tv(var2);
                    if (!quiet)
                        emsgn(e_listidx, lp.ll_n1);
                    return null;
                }

                /*
                 * May need to find the item or absolute index for the second index of a range.
                 * When no index given: "lp.ll_empty2" is true.
                 * Otherwise "lp.ll_n2" is set to the second index.
                 */
                if (lp.ll_range && !lp.ll_empty2)
                {
                    lp.ll_n2 = get_tv_number(var2); /* is number or string */
                    clear_tv(var2);
                    if (lp.ll_n2 < 0)
                    {
                        listitem_C ni = list_find(lp.ll_list, lp.ll_n2);
                        if (ni == null)
                        {
                            if (!quiet)
                                emsgn(e_listidx, lp.ll_n2);
                            return null;
                        }
                        lp.ll_n2 = list_idx_of_item(lp.ll_list, ni);
                    }

                    /* Check that lp.ll_n2 isn't before lp.ll_n1. */
                    if (lp.ll_n1 < 0)
                        lp.ll_n1 = list_idx_of_item(lp.ll_list, lp.ll_li);
                    if (lp.ll_n2 < lp.ll_n1)
                    {
                        if (!quiet)
                            emsgn(e_listidx, lp.ll_n2);
                        return null;
                    }
                }

                lp.ll_tv = lp.ll_li.li_tv;
            }
        }

        return p[0];
    }

    /*
     * Clear lval "lp" that was filled by get_lval().
     */
    /*private*/ static void clear_lval(lval_C lp)
    {
        lp.ll_exp_name = null;
        lp.ll_newkey = null;
    }

    /*
     * Set a variable that was parsed by get_lval() to "rtv".
     * "endp" points to just after the parsed name.
     * "op" is null, "+" for "+=", "-" for "-=", "." for ".=" or "=" for "=".
     */
    /*private*/ static void set_var_lval(lval_C lp, Bytes endp, typval_C rtv, boolean copy, Bytes op)
    {
        if (lp.ll_tv == null)
        {
            if (!check_changedtick(lp.ll_name))
            {
                byte cc = endp.at(0);
                endp.be(0, NUL);
                if (op != null && op.at(0) != (byte)'=')
                {
                    typval_C tv = new typval_C();

                    /* handle +=, -= and .= */
                    if (get_var_tv(lp.ll_name, strlen(lp.ll_name), tv, true, false))
                    {
                        if (tv_op(tv, rtv, op) == true)
                            set_var(lp.ll_name, tv, false);
                        clear_tv(tv);
                    }
                }
                else
                    set_var(lp.ll_name, rtv, copy);
                endp.be(0, cc);
            }
        }
        else if (tv_check_lock(lp.ll_newkey == null ? lp.ll_tv.tv_lock : lp.ll_tv.tv_dict.dv_lock, lp.ll_name))
            ;
        else if (lp.ll_range)
        {
            listitem_C ll_li = lp.ll_li;
            long ll_n1 = lp.ll_n1;

            listitem_C ri;

            /*
             * Check whether any of the list items is locked
             */
            for (ri = rtv.tv_list.lv_first; ri != null && ll_li != null; )
            {
                if (tv_check_lock(ll_li.li_tv.tv_lock, lp.ll_name))
                    return;
                ri = ri.li_next;
                if (ri == null || (!lp.ll_empty2 && lp.ll_n2 == ll_n1))
                    break;
                ll_li = ll_li.li_next;
                ll_n1++;
            }

            /*
             * Assign the List values to the list items.
             */
            for (ri = rtv.tv_list.lv_first; ri != null; )
            {
                if (op != null && op.at(0) != (byte)'=')
                    tv_op(lp.ll_li.li_tv, ri.li_tv, op);
                else
                {
                    clear_tv(lp.ll_li.li_tv);
                    copy_tv(ri.li_tv, lp.ll_li.li_tv);
                }
                ri = ri.li_next;
                if (ri == null || (!lp.ll_empty2 && lp.ll_n2 == lp.ll_n1))
                    break;
                if (lp.ll_li.li_next == null)
                {
                    /* Need to add an empty item. */
                    list_append_number(lp.ll_list, 0);
                }
                lp.ll_li = lp.ll_li.li_next;
                lp.ll_n1++;
            }
            if (ri != null)
                emsg(u8("E710: List value has more items than target"));
            else if (lp.ll_empty2
                    ? (lp.ll_li != null && lp.ll_li.li_next != null)
                    : lp.ll_n1 != lp.ll_n2)
                emsg(u8("E711: List value has not enough items"));
        }
        else
        {
            /*
             * Assign to a List or Dictionary item.
             */
            if (lp.ll_newkey != null)
            {
                if (op != null && op.at(0) != (byte)'=')
                {
                    emsg2(e_letwrong, op);
                    return;
                }

                /* Need to add an item to the Dictionary. */
                dictitem_C di = dictitem_alloc(lp.ll_newkey);
                if (!dict_add(lp.ll_tv.tv_dict, di))
                    return;

                lp.ll_tv = di.di_tv;
            }
            else if (op != null && op.at(0) != (byte)'=')
            {
                tv_op(lp.ll_tv, rtv, op);
                return;
            }
            else
                clear_tv(lp.ll_tv);

            /*
             * Assign the value to the variable or list item.
             */
            if (copy)
                copy_tv(rtv, lp.ll_tv);
            else
            {
                COPY_typval(lp.ll_tv, rtv);
                lp.ll_tv.tv_lock = 0;
                ZER0_typval(rtv);
            }
        }
    }

    /*
     * Handle "tv1 += tv2", "tv1 -= tv2" and "tv1 .= tv2"
     * Returns true or false.
     */
    /*private*/ static boolean tv_op(typval_C tv1, typval_C tv2, Bytes op)
    {
        /* Can't do anything with a Funcref or a Dict on the right. */
        if (tv2.tv_type != VAR_FUNC && tv2.tv_type != VAR_DICT)
        {
            switch (tv1.tv_type)
            {
                case VAR_DICT:
                case VAR_FUNC:
                    break;

                case VAR_LIST:
                {
                    if (op.at(0) != (byte)'+' || tv2.tv_type != VAR_LIST)
                        break;
                    /* List += List */
                    if (tv1.tv_list != null && tv2.tv_list != null)
                        list_extend(tv1.tv_list, tv2.tv_list, null);
                    return true;
                }
                case VAR_NUMBER:
                case VAR_STRING:
                {
                    if (tv2.tv_type == VAR_LIST)
                        break;
                    if (op.at(0) == (byte)'+' || op.at(0) == (byte)'-')
                    {
                        /* nr += nr  or  nr -= nr */
                        long n = get_tv_number(tv1);
                        if (op.at(0) == (byte)'+')
                            n += get_tv_number(tv2);
                        else
                            n -= get_tv_number(tv2);
                        clear_tv(tv1);
                        tv1.tv_type = VAR_NUMBER;
                        tv1.tv_number = n;
                    }
                    else
                    {
                        /* str .= str */
                        Bytes s = get_tv_string(tv1);
                        s = concat_str(s, get_tv_string(tv2));
                        clear_tv(tv1);
                        tv1.tv_type = VAR_STRING;
                        tv1.tv_string = s;
                    }
                    return true;
                }
            }
        }

        emsg2(e_letwrong, op);
        return false;
    }

    /*
     * Add a watcher to a list.
     */
    /*private*/ static void list_add_watch(list_C l, listwatch_C lw)
    {
        lw.lw_next = l.lv_watch;
        l.lv_watch = lw;
    }

    /*
     * Remove a watcher from a list.
     * No warning when it isn't found...
     */
    /*private*/ static void list_rem_watch(list_C l, listwatch_C lwrem)
    {
        listwatch_C lwp = null;

        for (listwatch_C lw = l.lv_watch; lw != null; lw = lw.lw_next)
        {
            if (lw == lwrem)
            {
                if (lwp == null)
                    l.lv_watch = lw.lw_next;
                else
                    lwp.lw_next = lw.lw_next;
                break;
            }
            lwp = lw;
        }
    }

    /*
     * Just before removing an item from a list: advance watchers to the next item.
     */
    /*private*/ static void list_fix_watch(list_C l, listitem_C item)
    {
        for (listwatch_C lw = l.lv_watch; lw != null; lw = lw.lw_next)
            if (lw.lw_item == item)
                lw.lw_item = item.li_next;
    }

    /*
     * Evaluate the expression used in a ":for var in expr" command.
     * "arg" points to "var".
     * Set "*errp" to true for an error, false otherwise.
     * Return a pointer that holds the info.
     */
    /*private*/ static forinfo_C eval_for_line(Bytes arg, boolean[] errp, Bytes[] nextcmdp, boolean skip)
    {
        errp[0] = true;       /* default: there is an error */

        forinfo_C fi = new forinfo_C();

        Bytes expr;
        {
            int[] _1 = { fi.fi_varcount };
            int[] _2 = { fi.fi_semicolon };
            expr = skip_var_list(arg, _1, _2);
            fi.fi_varcount = _1[0];
            fi.fi_semicolon = _2[0];
        }
        if (expr == null)
            return fi;

        expr = skipwhite(expr);
        if (expr.at(0) != (byte)'i' || expr.at(1) != (byte)'n' || !vim_iswhite(expr.at(2)))
        {
            emsg(u8("E690: Missing \"in\" after :for"));
            return fi;
        }

        if (skip)
            emsg_skip++;

        typval_C tv = new typval_C();
        if (eval0(skipwhite(expr.plus(2)), tv, nextcmdp, !skip) == true)
        {
            errp[0] = false;
            if (!skip)
            {
                list_C l = tv.tv_list;
                if (tv.tv_type != VAR_LIST || l == null)
                {
                    emsg(e_listreq);
                    clear_tv(tv);
                }
                else
                {
                    /* No need to increment the refcount,
                     * it's already set for the list being used in "tv". */
                    fi.fi_list = l;
                    list_add_watch(l, fi.fi_lw);
                    fi.fi_lw.lw_item = l.lv_first;
                }
            }
        }

        if (skip)
            --emsg_skip;

        return fi;
    }

    /*
     * Use the first item in a ":for" list.  Advance to the next.
     * Assign the values to the variable (list).  "arg" points to the first one.
     * Return true when a valid item was found, false when at end of list or something wrong.
     */
    /*private*/ static boolean next_for_item(forinfo_C fi, Bytes arg)
    {
        listitem_C item = fi.fi_lw.lw_item;
        if (item == null)
            return false;

        fi.fi_lw.lw_item = item.li_next;
        return ex_let_vars(arg, item.li_tv, true, fi.fi_semicolon, fi.fi_varcount, null);
    }

    /*
     * Free the structure used to store info used by ":for".
     */
    /*private*/ static void free_for_info(forinfo_C fi)
    {
        if (fi != null && fi.fi_list != null)
        {
            list_rem_watch(fi.fi_list, fi.fi_lw);
            list_unref(fi.fi_list);
        }
    }

    /*private*/ static void set_context_for_expression(expand_C xp, Bytes arg, int cmdidx)
    {
        boolean got_eq = false;

        if (cmdidx == CMD_let)
        {
            xp.xp_context = EXPAND_USER_VARS;
            if (STRPBRK(arg, u8("\"'+-*/%.=!?~|&$([<>,#")) == null)
            {
                /* ":let var1 var2 ...": find last space. */
                for (Bytes p = arg.plus(strlen(arg)); BLE(arg, p); )
                {
                    xp.xp_pattern = p;
                    p = p.minus(us_ptr_back(arg, p));
                    if (vim_iswhite(p.at(0)))
                        break;
                }
                return;
            }
        }
        else
            xp.xp_context = (cmdidx == CMD_call) ? EXPAND_FUNCTIONS : EXPAND_EXPRESSION;

        while ((xp.xp_pattern = STRPBRK(arg, u8("\"'+-*/%.=!?~|&$([<>,#"))) != null)
        {
            byte c = xp.xp_pattern.at(0);
            if (c == '&')
            {
                c = xp.xp_pattern.at(1);
                if (c == '&')
                {
                    xp.xp_pattern = xp.xp_pattern.plus(1);
                    xp.xp_context = (cmdidx != CMD_let || got_eq) ? EXPAND_EXPRESSION : EXPAND_NOTHING;
                }
                else if (c != ' ')
                {
                    xp.xp_context = EXPAND_SETTINGS;
                    if ((c == 'l' || c == 'g') && xp.xp_pattern.at(2) == (byte)':')
                        xp.xp_pattern = xp.xp_pattern.plus(2);
                }
            }
            else if (c == '=')
            {
                got_eq = true;
                xp.xp_context = EXPAND_EXPRESSION;
            }
            else if ((c == '<' || c == '#')
                    && xp.xp_context == EXPAND_FUNCTIONS
                    && vim_strchr(xp.xp_pattern, '(') == null)
            {
                /* Function name can start with "<SNR>" and contain '#'. */
                break;
            }
            else if (cmdidx != CMD_let || got_eq)
            {
                if (c == '"')           /* string */
                {
                    while ((c = (xp.xp_pattern = xp.xp_pattern.plus(1)).at(0)) != NUL && c != '"')
                        if (c == '\\' && xp.xp_pattern.at(1) != NUL)
                            xp.xp_pattern = xp.xp_pattern.plus(1);
                    xp.xp_context = EXPAND_NOTHING;
                }
                else if (c == '\'')     /* literal string */
                {
                    /* Trick: '' is like stopping and starting a literal string. */
                    while ((c = (xp.xp_pattern = xp.xp_pattern.plus(1)).at(0)) != NUL && c != '\'')
                        /* skip */;
                    xp.xp_context = EXPAND_NOTHING;
                }
                else if (c == '|')
                {
                    if (xp.xp_pattern.at(1) == (byte)'|')
                    {
                        xp.xp_pattern = xp.xp_pattern.plus(1);
                        xp.xp_context = EXPAND_EXPRESSION;
                    }
                    else
                        xp.xp_context = EXPAND_COMMANDS;
                }
                else
                    xp.xp_context = EXPAND_EXPRESSION;
            }
            else
                /* Doesn't look like something valid, expand as an expression anyway. */
                xp.xp_context = EXPAND_EXPRESSION;
            arg = xp.xp_pattern;
            if (arg.at(0) != NUL)
                while ((c = (arg = arg.plus(1)).at(0)) != NUL && (c == ' ' || c == '\t'))
                    /* skip */;
        }

        xp.xp_pattern = arg;
    }

    /*
     * ":1,25call func(arg1, arg2)" function call.
     */
    /*private*/ static final ex_func_C ex_call = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes[] arg = { eap.arg };
            boolean failed = false;

            if (eap.skip)
            {
                /* trans_function_name() doesn't work well when skipping, use eval0()
                 * instead to skip to any following command, e.g. for:
                 *   :if 0 | call dict.foo().bar() | endif */
                emsg_skip++;
                typval_C rtv = new typval_C();
                boolean b;
                { Bytes[] __ = { eap.nextcmd }; b = eval0(eap.arg, rtv, __, false); eap.nextcmd = __[0]; }
                if (b != false)
                    clear_tv(rtv);
                --emsg_skip;
                return;
            }

            funcdict_C fudi = new funcdict_C();
            Bytes tofree = trans_function_name(arg, eap.skip, TFN_INT, fudi);
            if (fudi.fd_newkey != null)
            {
                /* Still need to give an error message for missing key. */
                emsg2(e_dictkey, fudi.fd_newkey);
                fudi.fd_newkey = null;
            }
            if (tofree == null)
                return;

            /* Increase refcount on dictionary, it could get deleted when evaluating the arguments. */
            if (fudi.fd_dict != null)
                fudi.fd_dict.dv_refcount++;

            /* If it is the name of a variable of type VAR_FUNC use its contents. */
            int[] len = { strlen(tofree) };
            Bytes name = deref_func_name(tofree, len, false);

            /* Skip white space to allow ":call func ()".  Not good, but required for backward compatibility. */
            Bytes startarg = skipwhite(arg[0]);
            typval_C rtv = new typval_C();
            rtv.tv_type = VAR_UNKNOWN;        /* clear_tv() uses this */

            if (startarg.at(0) != (byte)'(')
            {
                emsg2(u8("E107: Missing parentheses: %s"), eap.arg);
                dict_unref(fudi.fd_dict);
                return;
            }

            /*
             * When skipping, evaluate the function once, to find the end of the arguments.
             * When the function takes a range, this is discovered after the first call, and the loop is broken.
             */
            long lnum;
            if (eap.skip)
            {
                emsg_skip++;
                lnum = eap.line2;       /* do it once, also with an invalid range */
            }
            else
                lnum = eap.line1;
            for ( ; lnum <= eap.line2; lnum++)
            {
                if (!eap.skip && 0 < eap.addr_count)
                {
                    curwin.w_cursor.lnum = lnum;
                    curwin.w_cursor.col = 0;
                    curwin.w_cursor.coladd = 0;
                }
                arg[0] = startarg;
                boolean[] doesrange = new boolean[1];
                if (get_func_tv(name, strlen(name), rtv, arg,
                            eap.line1, eap.line2, doesrange, !eap.skip, fudi.fd_dict) == false)
                {
                    failed = true;
                    break;
                }

                /* Handle a function returning a Funcref, Dictionary or List. */
                if (!handle_subscript(arg, rtv, !eap.skip, true))
                {
                    failed = true;
                    break;
                }

                clear_tv(rtv);
                if (doesrange[0] || eap.skip)
                    break;

                /* Stop when immediately aborting on error,
                 * or when an interrupt occurred, or an exception was thrown, but not caught.
                 * get_func_tv() returned OK, so that the check for trailing characters below is executed. */
                if (aborting())
                    break;
            }
            if (eap.skip)
                --emsg_skip;

            if (!failed)
            {
                /* Check for trailing illegal characters and a following command. */
                if (!ends_excmd(arg[0].at(0)))
                {
                    emsg_severe = true;
                    emsg(e_trailing);
                }
                else
                    eap.nextcmd = check_nextcmd(arg[0]);
            }

            dict_unref(fudi.fd_dict);
        }
    };

    /*
     * ":unlet[!] var1 ... " command.
     */
    /*private*/ static final ex_func_C ex_unlet = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            ex_unletlock(eap, eap.arg, 0);
        }
    };

    /*
     * ":lockvar" and ":unlockvar" commands
     */
    /*private*/ static final ex_func_C ex_lockvar = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes arg = eap.arg;
            int deep = 2;

            if (eap.forceit)
                deep = -1;
            else if (asc_isdigit(arg.at(0)))
            {
                { Bytes[] __ = { arg }; deep = (int)getdigits(__); arg = __[0]; }
                arg = skipwhite(arg);
            }

            ex_unletlock(eap, arg, deep);
        }
    };

    /*
     * ":unlet", ":lockvar" and ":unlockvar" are quite similar.
     */
    /*private*/ static void ex_unletlock(exarg_C eap, Bytes argstart, int deep)
    {
        Bytes arg = argstart;
        boolean error = false;

        lval_C lv = new lval_C();

        do
        {
            /* Parse the name and find the end. */
            Bytes name_end = get_lval(arg, null, lv, true, eap.skip || error, 0, FNE_CHECK_START);
            if (lv.ll_name == null)
                error = true;           /* error but continue parsing */
            if (name_end == null || (!vim_iswhite(name_end.at(0)) && !ends_excmd(name_end.at(0))))
            {
                if (name_end != null)
                {
                    emsg_severe = true;
                    emsg(e_trailing);
                }
                if (!(eap.skip || error))
                    clear_lval(lv);
                break;
            }

            if (!error && !eap.skip)
            {
                if (eap.cmdidx == CMD_unlet)
                {
                    if (do_unlet_var(lv, name_end, eap.forceit) == false)
                        error = true;
                }
                else
                {
                    if (do_lock_var(lv, name_end, deep, eap.cmdidx == CMD_lockvar) == false)
                        error = true;
                }
            }

            if (!eap.skip)
                clear_lval(lv);

            arg = skipwhite(name_end);
        } while (!ends_excmd(arg.at(0)));

        eap.nextcmd = check_nextcmd(arg);
    }

    /*private*/ static boolean do_unlet_var(lval_C lp, Bytes name_end, boolean forceit)
    {
        boolean ret = true;

        if (lp.ll_tv == null)
        {
            byte cc = name_end.at(0);
            name_end.be(0, NUL);

            /* Normal name or expanded name. */
            if (check_changedtick(lp.ll_name))
                ret = false;
            else if (do_unlet(lp.ll_name, forceit) == false)
                ret = false;
            name_end.be(0, cc);
        }
        else if (tv_check_lock(lp.ll_tv.tv_lock, lp.ll_name))
            return false;
        else if (lp.ll_range)
        {
            listitem_C ll_li = lp.ll_li;
            long ll_n1 = lp.ll_n1;

            while (ll_li != null && (lp.ll_empty2 || ll_n1 <= lp.ll_n2))
            {
                listitem_C li = ll_li.li_next;
                if (tv_check_lock(ll_li.li_tv.tv_lock, lp.ll_name))
                    return false;
                ll_li = li;
                ll_n1++;
            }

            /* Delete a range of List items. */
            while (lp.ll_li != null && (lp.ll_empty2 || lp.ll_n1 <= lp.ll_n2))
            {
                listitem_C li = lp.ll_li.li_next;
                listitem_remove(lp.ll_list, lp.ll_li);
                lp.ll_li = li;
                lp.ll_n1++;
            }
        }
        else
        {
            if (lp.ll_list != null)
                /* unlet a List item. */
                listitem_remove(lp.ll_list, lp.ll_li);
            else
                /* unlet a Dictionary item. */
                dictitem_remove(lp.ll_dict, lp.ll_di);
        }

        return ret;
    }

    /*
     * "unlet" a variable.  Return true if it existed, false if not.
     * When "forceit" is true don't complain if the variable doesn't exist.
     */
    /*private*/ static boolean do_unlet(Bytes name, boolean forceit)
    {
        Bytes[] varname = new Bytes[1];

        hashtab_C ht = find_var_ht(name, varname);
        if (ht != null && varname[0].at(0) != NUL)
        {
            hashitem_C hi = hash_find(ht, varname[0]);
            if (!hashitem_empty(hi))
            {
                dictitem_C di = (dictitem_C)hi.hi_data;
                if (var_check_fixed(di.di_flags, name) || var_check_ro(di.di_flags, name))
                    return false;
                delete_var(ht, hi);
                return true;
            }
        }
        if (forceit)
            return true;

        emsg2(u8("E108: No such variable: \"%s\""), name);
        return false;
    }

    /*
     * Lock or unlock variable indicated by "lp".
     * "deep" is the levels to go (-1 for unlimited);
     * "lock" is true for ":lockvar", false for ":unlockvar".
     */
    /*private*/ static boolean do_lock_var(lval_C lp, Bytes name_end, int deep, boolean lock)
    {
        boolean ret = true;

        if (deep == 0)      /* nothing to do */
            return true;

        if (lp.ll_tv == null)
        {
            byte cc = name_end.at(0);
            name_end.be(0, NUL);

            /* Normal name or expanded name. */
            if (check_changedtick(lp.ll_name))
                ret = false;
            else
            {
                dictitem_C di = find_var(lp.ll_name, null, true);
                if (di == null)
                    ret = false;
                else
                {
                    if (lock)
                        di.di_flags |= DI_FLAGS_LOCK;
                    else
                        di.di_flags &= ~DI_FLAGS_LOCK;
                    item_lock(di.di_tv, deep, lock);
                }
            }
            name_end.be(0, cc);
        }
        else if (lp.ll_range)
        {
            listitem_C li = lp.ll_li;

            /* (un)lock a range of List items. */
            while (li != null && (lp.ll_empty2 || lp.ll_n1 <= lp.ll_n2))
            {
                item_lock(li.li_tv, deep, lock);
                li = li.li_next;
                lp.ll_n1++;
            }
        }
        else if (lp.ll_list != null)
            /* (un)lock a List item. */
            item_lock(lp.ll_li.li_tv, deep, lock);
        else
            /* un(lock) a Dictionary item. */
            item_lock(lp.ll_di.di_tv, deep, lock);

        return ret;
    }

    /*private*/ static int _1_recurse;

    /*
     * Lock or unlock an item.  "deep" is nr of levels to go.
     */
    /*private*/ static void item_lock(typval_C tv, int deep, boolean lock)
    {
        if (DICT_MAXNEST <= _1_recurse)
        {
            emsg(u8("E743: variable nested too deep for (un)lock"));
            return;
        }
        if (deep == 0)
            return;

        _1_recurse++;

        /* lock/unlock the item itself */
        if (lock)
            tv.tv_lock |= VAR_LOCKED;
        else
            tv.tv_lock &= ~VAR_LOCKED;

        switch (tv.tv_type)
        {
            case VAR_LIST:
            {
                list_C l = tv.tv_list;
                if (l != null)
                {
                    if (lock)
                        l.lv_lock |= VAR_LOCKED;
                    else
                        l.lv_lock &= ~VAR_LOCKED;
                    if (deep < 0 || 1 < deep)
                        /* recursive: lock/unlock the items the List contains */
                        for (listitem_C li = l.lv_first; li != null; li = li.li_next)
                            item_lock(li.li_tv, deep - 1, lock);
                }
                break;
            }
            case VAR_DICT:
            {
                dict_C d = tv.tv_dict;
                if (d != null)
                {
                    if (lock)
                        d.dv_lock |= VAR_LOCKED;
                    else
                        d.dv_lock &= ~VAR_LOCKED;
                    if (deep < 0 || 1 < deep)
                    {
                        /* recursive: lock/unlock the items the List contains */
                        for (int i = 0, todo = (int)d.dv_hashtab.ht_used; 0 < todo; i++)
                        {
                            hashitem_C hi = d.dv_hashtab.ht_buckets[i];
                            if (!hashitem_empty(hi))
                            {
                                item_lock(((dictitem_C)hi.hi_data).di_tv, deep - 1, lock);
                                --todo;
                            }
                        }
                    }
                }
                break;
            }
        }

        --_1_recurse;
    }

    /*
     * Return true if typeval "tv" is locked: Either that value is locked itself
     * or it refers to a List or Dictionary that is locked.
     */
    /*private*/ static boolean tv_islocked(typval_C tv)
    {
        return (tv.tv_lock & VAR_LOCKED) != 0
            || (tv.tv_type == VAR_LIST
                    && tv.tv_list != null
                    && (tv.tv_list.lv_lock & VAR_LOCKED) != 0)
            || (tv.tv_type == VAR_DICT
                    && tv.tv_dict != null
                    && (tv.tv_dict.dv_lock & VAR_LOCKED) != 0);
    }

    /*
     * Local string buffer for the next two functions to store a variable name with its prefix.
     * Allocated in cat_prefix_varname(), freed later in get_user_var_name().
     */

    /*private*/ static Bytes    varnamebuf;
    /*private*/ static int      varnamebuflen;

    /*
     * Function to concatenate a prefix and a variable name.
     */
    /*private*/ static Bytes cat_prefix_varname(int prefix, Bytes name)
    {
        int len = strlen(name) + 3;
        if (varnamebuflen < len)
        {
            len += 10;                      /* some additional space */
            varnamebuf = new Bytes(len);
            varnamebuflen = len;
        }
        varnamebuf.be(0, prefix);
        varnamebuf.be(1, (byte)':');
        STRCPY(varnamebuf.plus(2), name);
        return varnamebuf;
    }

    /*private*/ static long uv__gdone, uv__bdone, uv__wdone, uv__tdone;
    /*private*/ static int uv__vidx;
    /*private*/ static hashitem_C[] uv__hi;
    /*private*/ static int uv__i;

    /*
     * Function given to expandGeneric() to obtain the list of user defined
     * (global/buffer/window/built-in) variable names.
     */
    /*private*/ static final expfun_C get_user_var_name = new expfun_C()
    {
        public Bytes expand(expand_C xp, int idx)
        {
            if (idx == 0)
            {
                uv__gdone = uv__bdone = uv__wdone = uv__tdone = 0;
                uv__vidx = 0;
            }

            /* Global variables. */
            hashtab_C ht = globvardict.dv_hashtab;
            if (uv__gdone < ht.ht_used)
            {
                if (uv__gdone++ == 0)
                {
                    uv__hi = ht.ht_buckets;
                    uv__i = 0;
                }
                else
                    uv__i++;
                while (hashitem_empty(uv__hi[uv__i]))
                    uv__i++;
                if (STRNCMP(u8("g:"), xp.xp_pattern, 2) == 0)
                    return cat_prefix_varname('g', uv__hi[uv__i].hi_key);

                return uv__hi[uv__i].hi_key;
            }

            /* b: variables */
            ht = curbuf.b_vars.dv_hashtab;
            if (uv__bdone < ht.ht_used)
            {
                if (uv__bdone++ == 0)
                {
                    uv__hi = ht.ht_buckets;
                    uv__i = 0;
                }
                else
                    uv__i++;
                while (hashitem_empty(uv__hi[uv__i]))
                    uv__i++;
                return cat_prefix_varname('b', uv__hi[uv__i].hi_key);
            }
            if (uv__bdone == ht.ht_used)
            {
                uv__bdone++;
                return u8("b:changedtick");
            }

            /* w: variables */
            ht = curwin.w_vars.dv_hashtab;
            if (uv__wdone < ht.ht_used)
            {
                if (uv__wdone++ == 0)
                {
                    uv__hi = ht.ht_buckets;
                    uv__i = 0;
                }
                else
                    uv__i++;
                while (hashitem_empty(uv__hi[uv__i]))
                    uv__i++;
                return cat_prefix_varname('w', uv__hi[uv__i].hi_key);
            }

            /* t: variables */
            ht = curtab.tp_vars.dv_hashtab;
            if (uv__tdone < ht.ht_used)
            {
                if (uv__tdone++ == 0)
                {
                    uv__hi = ht.ht_buckets;
                    uv__i = 0;
                }
                else
                    uv__i++;
                while (hashitem_empty(uv__hi[uv__i]))
                    uv__i++;
                return cat_prefix_varname('t', uv__hi[uv__i].hi_key);
            }

            /* v: variables */
            if (uv__vidx < VV_LEN)
                return cat_prefix_varname('v', vimvars[uv__vidx++].vv_name);

            varnamebuf = null;
            varnamebuflen = 0;
            return null;
        }
    };

    /*
     * types for expressions
     */
    /*private*/ static final int
        TYPE_UNKNOWN = 0,
        TYPE_EQUAL   = 1,   /* == */
        TYPE_NEQUAL  = 2,   /* != */
        TYPE_GREATER = 3,   /* >  */
        TYPE_GEQUAL  = 4,   /* >= */
        TYPE_SMALLER = 5,   /* <  */
        TYPE_SEQUAL  = 6,   /* <= */
        TYPE_MATCH   = 7,   /* =~ */
        TYPE_NOMATCH = 8;   /* !~ */

    /*
     * The "evaluate" argument: when false, the argument is only parsed but not executed.
     * The function may return true, but the "rtv" will be of type VAR_UNKNOWN.
     * The function still returns false for a syntax error.
     */

    /*
     * Handle zero level expression.
     * This calls eval1() and handles error message and nextcmd.
     * Put the result in "rtv" when returning true and "evaluate" is true.
     * Note: "rtv.tv_lock" is not set.
     * Return true or false.
     */
    /*private*/ static boolean eval0(Bytes arg, typval_C rtv, Bytes[] nextcmd, boolean evaluate)
    {
        Bytes[] p = { skipwhite(arg) };
        boolean ret = eval1(p, rtv, evaluate);

        if (ret == false || !ends_excmd(p[0].at(0)))
        {
            if (ret != false)
                clear_tv(rtv);
            /*
             * Report the invalid expression unless the expression evaluation has
             * been cancelled due to an aborting error, an interrupt, or an exception.
             */
            if (!aborting())
                emsg2(e_invexpr2, arg);
            ret = false;
        }
        if (nextcmd != null)
            nextcmd[0] = check_nextcmd(p[0]);

        return ret;
    }

    /*
     * Handle top level expression:
     *      expr2 ? expr1 : expr1
     *
     * "arg" must point to the first non-white of the expression.
     * "arg" is advanced to the next non-white after the recognized expression.
     *
     * Note: "rtv.tv_lock" is not set.
     *
     * Return true or false.
     */
    /*private*/ static boolean eval1(Bytes[] arg, typval_C rtv, boolean evaluate)
    {
        /*
         * Get the first variable.
         */
        if (eval2(arg, rtv, evaluate) == false)
            return false;

        if (arg[0].at(0) == (byte)'?')
        {
            boolean result = false;
            if (evaluate)
            {
                boolean[] error = { false };

                if (get_tv_number_chk(rtv, error) != 0)
                    result = true;
                clear_tv(rtv);
                if (error[0])
                    return false;
            }

            /*
             * Get the second variable.
             */
            arg[0] = skipwhite(arg[0].plus(1));
            if (eval1(arg, rtv, evaluate && result) == false) /* recursive! */
                return false;

            /*
             * Check for the ":".
             */
            if (arg[0].at(0) != (byte)':')
            {
                emsg(u8("E109: Missing ':' after '?'"));
                if (evaluate && result)
                    clear_tv(rtv);
                return false;
            }

            /*
             * Get the third variable.
             */
            arg[0] = skipwhite(arg[0].plus(1));
            typval_C var2 = new typval_C();
            if (eval1(arg, var2, evaluate && !result) == false)     /* recursive! */
            {
                if (evaluate && result)
                    clear_tv(rtv);
                return false;
            }
            if (evaluate && !result)
                COPY_typval(rtv, var2);
        }

        return true;
    }

    /*
     * Handle first level expression:
     *      expr2 || expr2 || expr2     logical OR
     *
     * "arg" must point to the first non-white of the expression.
     * "arg" is advanced to the next non-white after the recognized expression.
     *
     * Return true or false.
     */
    /*private*/ static boolean eval2(Bytes[] arg, typval_C rtv, boolean evaluate)
    {
        boolean[] error = { false };

        /*
         * Get the first variable.
         */
        if (eval3(arg, rtv, evaluate) == false)
            return false;

        /*
         * Repeat until there is no following "||".
         */
        boolean first = true;
        boolean result = false;
        while (arg[0].at(0) == (byte)'|' && arg[0].at(1) == (byte)'|')
        {
            if (evaluate && first)
            {
                if (get_tv_number_chk(rtv, error) != 0)
                    result = true;
                clear_tv(rtv);
                if (error[0])
                    return false;
                first = false;
            }

            /*
             * Get the second variable.
             */
            arg[0] = skipwhite(arg[0].plus(2));
            typval_C var2 = new typval_C();
            if (eval3(arg, var2, evaluate && !result) == false)
                return false;

            /*
             * Compute the result.
             */
            if (evaluate && !result)
            {
                if (get_tv_number_chk(var2, error) != 0)
                    result = true;
                clear_tv(var2);
                if (error[0])
                    return false;
            }
            if (evaluate)
            {
                rtv.tv_type = VAR_NUMBER;
                rtv.tv_number = result ? 1 : 0;
            }
        }

        return true;
    }

    /*
     * Handle second level expression:
     *      expr3 && expr3 && expr3     logical AND
     *
     * "arg" must point to the first non-white of the expression.
     * "arg" is advanced to the next non-white after the recognized expression.
     *
     * Return true or false.
     */
    /*private*/ static boolean eval3(Bytes[] arg, typval_C rtv, boolean evaluate)
    {
        boolean[] error = { false };

        /*
         * Get the first variable.
         */
        if (eval4(arg, rtv, evaluate) == false)
            return false;

        /*
         * Repeat until there is no following "&&".
         */
        boolean first = true;
        boolean result = true;
        while (arg[0].at(0) == (byte)'&' && arg[0].at(1) == (byte)'&')
        {
            if (evaluate && first)
            {
                if (get_tv_number_chk(rtv, error) == 0)
                    result = false;
                clear_tv(rtv);
                if (error[0])
                    return false;
                first = false;
            }

            /*
             * Get the second variable.
             */
            arg[0] = skipwhite(arg[0].plus(2));
            typval_C var2 = new typval_C();
            if (eval4(arg, var2, evaluate && result) == false)
                return false;

            /*
             * Compute the result.
             */
            if (evaluate && result)
            {
                if (get_tv_number_chk(var2, error) == 0)
                    result = false;
                clear_tv(var2);
                if (error[0])
                    return false;
            }
            if (evaluate)
            {
                rtv.tv_type = VAR_NUMBER;
                rtv.tv_number = result ? 1 : 0;
            }
        }

        return true;
    }

    /*
     * Handle third level expression:
     *      var1 == var2
     *      var1 =~ var2
     *      var1 != var2
     *      var1 !~ var2
     *      var1 > var2
     *      var1 >= var2
     *      var1 < var2
     *      var1 <= var2
     *      var1 is var2
     *      var1 isnot var2
     *
     * "arg" must point to the first non-white of the expression.
     * "arg" is advanced to the next non-white after the recognized expression.
     *
     * Return true or false.
     */
    /*private*/ static boolean eval4(Bytes[] arg, typval_C rtv, boolean evaluate)
    {
        int type = TYPE_UNKNOWN;
        boolean type_is = false;    /* true for "is" and "isnot" */
        int len = 2;

        /*
         * Get the first variable.
         */
        if (eval5(arg, rtv, evaluate) == false)
            return false;

        Bytes p = arg[0];
        switch (p.at(0))
        {
            case '=':
            {
                if (p.at(1) == (byte)'=')
                    type = TYPE_EQUAL;
                else if (p.at(1) == (byte)'~')
                    type = TYPE_MATCH;
                break;
            }
            case '!':
            {
                if (p.at(1) == (byte)'=')
                    type = TYPE_NEQUAL;
                else if (p.at(1) == (byte)'~')
                    type = TYPE_NOMATCH;
                break;
            }
            case '>':
            {
                if (p.at(1) != (byte)'=')
                {
                    type = TYPE_GREATER;
                    len = 1;
                }
                else
                    type = TYPE_GEQUAL;
                break;
            }
            case '<':
            {
                if (p.at(1) != (byte)'=')
                {
                    type = TYPE_SMALLER;
                    len = 1;
                }
                else
                    type = TYPE_SEQUAL;
                break;
            }
            case 'i':
            {
                if (p.at(1) == (byte)'s')
                {
                    if (p.at(2) == (byte)'n' && p.at(3) == (byte)'o' && p.at(4) == (byte)'t')
                        len = 5;
                    if (!vim_isIDc(p.at(len)))
                    {
                        type = (len == 2) ? TYPE_EQUAL : TYPE_NEQUAL;
                        type_is = true;
                    }
                }
                break;
            }
        }

        /*
         * If there is a comparative operator, use it.
         */
        if (type != TYPE_UNKNOWN)
        {
            boolean ic;

            /* extra question mark appended: ignore case */
            if (p.at(len) == (byte)'?')
            {
                ic = true;
                len++;
            }
            /* extra '#' appended: match case */
            else if (p.at(len) == (byte)'#')
            {
                ic = false;
                len++;
            }
            /* nothing appended: use 'ignorecase' */
            else
                ic = p_ic[0];

            /*
             * Get the second variable.
             */
            arg[0] = skipwhite(p.plus(len));
            typval_C var2 = new typval_C();
            if (eval5(arg, var2, evaluate) == false)
            {
                clear_tv(rtv);
                return false;
            }

            if (evaluate)
            {
                boolean result = false;

                if (type_is && rtv.tv_type != var2.tv_type)
                {
                    /* For "is" a different type always means false, for "notis" it means true. */
                    result = (type == TYPE_NEQUAL);
                }

                else if (rtv.tv_type == VAR_LIST || var2.tv_type == VAR_LIST)
                {
                    if (type_is)
                    {
                        result = (rtv.tv_type == var2.tv_type && rtv.tv_list == var2.tv_list);
                        if (type == TYPE_NEQUAL)
                            result = !result;
                    }
                    else if (rtv.tv_type != var2.tv_type || (type != TYPE_EQUAL && type != TYPE_NEQUAL))
                    {
                        if (rtv.tv_type != var2.tv_type)
                            emsg(u8("E691: Can only compare List with List"));
                        else
                            emsg(u8("E692: Invalid operation for List"));
                        clear_tv(rtv);
                        clear_tv(var2);
                        return false;
                    }
                    else
                    {
                        /* Compare two Lists for being equal or unequal. */
                        result = list_equal(rtv.tv_list, var2.tv_list, ic, false);
                        if (type == TYPE_NEQUAL)
                            result = !result;
                    }
                }

                else if (rtv.tv_type == VAR_DICT || var2.tv_type == VAR_DICT)
                {
                    if (type_is)
                    {
                        result = (rtv.tv_type == var2.tv_type && rtv.tv_dict == var2.tv_dict);
                        if (type == TYPE_NEQUAL)
                            result = !result;
                    }
                    else if (rtv.tv_type != var2.tv_type || (type != TYPE_EQUAL && type != TYPE_NEQUAL))
                    {
                        if (rtv.tv_type != var2.tv_type)
                            emsg(u8("E735: Can only compare Dictionary with Dictionary"));
                        else
                            emsg(u8("E736: Invalid operation for Dictionary"));
                        clear_tv(rtv);
                        clear_tv(var2);
                        return false;
                    }
                    else
                    {
                        /* Compare two Dictionaries for being equal or unequal. */
                        result = dict_equal(rtv.tv_dict, var2.tv_dict, ic, false);
                        if (type == TYPE_NEQUAL)
                            result = !result;
                    }
                }

                else if (rtv.tv_type == VAR_FUNC || var2.tv_type == VAR_FUNC)
                {
                    if (rtv.tv_type != var2.tv_type || (type != TYPE_EQUAL && type != TYPE_NEQUAL))
                    {
                        if (rtv.tv_type != var2.tv_type)
                            emsg(u8("E693: Can only compare Funcref with Funcref"));
                        else
                            emsg(u8("E694: Invalid operation for Funcrefs"));
                        clear_tv(rtv);
                        clear_tv(var2);
                        return false;
                    }
                    else
                    {
                        /* Compare two Funcrefs for being equal or unequal. */
                        if (rtv.tv_string != null && var2.tv_string != null)
                            result = (STRCMP(rtv.tv_string, var2.tv_string) == 0);
                        if (type == TYPE_NEQUAL)
                            result = !result;
                    }
                }

                /*
                 * If one of the two variables is a number, compare as a number.
                 * When using "=~" or "!~", always compare as string.
                 */
                else if ((rtv.tv_type == VAR_NUMBER || var2.tv_type == VAR_NUMBER)
                        && type != TYPE_MATCH && type != TYPE_NOMATCH)
                {
                    long n1 = get_tv_number(rtv);
                    long n2 = get_tv_number(var2);

                    switch (type)
                    {
                        case TYPE_EQUAL:    result = (n1 == n2); break;
                        case TYPE_NEQUAL:   result = (n1 != n2); break;
                        case TYPE_GREATER:  result = (n1 > n2); break;
                        case TYPE_GEQUAL:   result = (n1 >= n2); break;
                        case TYPE_SMALLER:  result = (n1 < n2); break;
                        case TYPE_SEQUAL:   result = (n1 <= n2); break;
                        case TYPE_UNKNOWN:
                        case TYPE_MATCH:
                        case TYPE_NOMATCH:  break;
                    }
                }

                else
                {
                    Bytes s1 = get_tv_string(rtv);
                    Bytes s2 = get_tv_string(var2);
                    int cmp = 0;
                    if (type != TYPE_MATCH && type != TYPE_NOMATCH)
                        cmp = ic ? us_strnicmp(s1, s2, MAXCOL) : STRCMP(s1, s2);

                    switch (type)
                    {
                        case TYPE_EQUAL:    result = (cmp == 0); break;
                        case TYPE_NEQUAL:   result = (cmp != 0); break;
                        case TYPE_GREATER:  result = (cmp > 0); break;
                        case TYPE_GEQUAL:   result = (cmp >= 0); break;
                        case TYPE_SMALLER:  result = (cmp < 0); break;
                        case TYPE_SEQUAL:   result = (cmp <= 0); break;

                        case TYPE_MATCH:
                        case TYPE_NOMATCH:
                        {
                            /* avoid 'l' flag in 'cpoptions' */
                            Bytes save_cpo = p_cpo[0];
                            p_cpo[0] = u8("");
                            regmatch_C regmatch = new regmatch_C();
                            regmatch.regprog = vim_regcomp(s2, RE_MAGIC + RE_STRING);
                            regmatch.rm_ic = ic;
                            if (regmatch.regprog != null)
                            {
                                result = vim_regexec_nl(regmatch, s1, 0);
                                if (type == TYPE_NOMATCH)
                                    result = !result;
                            }
                            p_cpo[0] = save_cpo;
                            break;
                        }

                        case TYPE_UNKNOWN:  break;
                    }
                }

                clear_tv(rtv);
                clear_tv(var2);

                rtv.tv_type = VAR_NUMBER;
                rtv.tv_number = result ? 1 : 0;
            }
        }

        return true;
    }

    /*
     * Handle fourth level expression:
     *      +       number addition
     *      -       number subtraction
     *      .       string concatenation
     *
     * "arg" must point to the first non-white of the expression.
     * "arg" is advanced to the next non-white after the recognized expression.
     *
     * Return true or false.
     */
    /*private*/ static boolean eval5(Bytes[] arg, typval_C rtv, boolean evaluate)
    {
        /*
         * Get the first variable.
         */
        if (eval6(arg, rtv, evaluate, false) == false)
            return false;

        /*
         * Repeat computing, until no '+', '-' or '.' is following.
         */
        for ( ; ; )
        {
            byte op = arg[0].at(0);
            if (op != '+' && op != '-' && op != '.')
                break;

            if (op != '+' || rtv.tv_type != VAR_LIST)
            {
                /* For "list + ...", an illegal use of the first operand as
                 * a number cannot be determined before evaluating the 2nd
                 * operand: if this is also a list, all is ok.
                 * For "something . ...", "something - ..." or "non-list + ...",
                 * we know that the first operand needs to be a string or number
                 * without evaluating the 2nd operand.  So check before to avoid
                 * side effects after an error. */
                if (evaluate && get_tv_string_chk(rtv) == null)
                {
                    clear_tv(rtv);
                    return false;
                }
            }

            /*
             * Get the second variable.
             */
            arg[0] = skipwhite(arg[0].plus(1));
            typval_C var2 = new typval_C();
            if (eval6(arg, var2, evaluate, op == '.') == false)
            {
                clear_tv(rtv);
                return false;
            }

            if (evaluate)
            {
                /*
                 * Compute the result.
                 */
                if (op == '.')
                {
                    Bytes s1 = get_tv_string(rtv);         /* already checked */
                    Bytes s2 = get_tv_string_chk(var2);
                    if (s2 == null)                                     /* type error ? */
                    {
                        clear_tv(rtv);
                        clear_tv(var2);
                        return false;
                    }
                    Bytes s3 = concat_str(s1, s2);
                    clear_tv(rtv);
                    rtv.tv_type = VAR_STRING;
                    rtv.tv_string = s3;
                }
                else if (op == '+' && rtv.tv_type == VAR_LIST && var2.tv_type == VAR_LIST)
                {
                    /* concatenate Lists */
                    typval_C var3 = new typval_C();
                    if (list_concat(rtv.tv_list, var2.tv_list, var3) == false)
                    {
                        clear_tv(rtv);
                        clear_tv(var2);
                        return false;
                    }
                    clear_tv(rtv);
                    COPY_typval(rtv, var3);
                }
                else
                {
                    boolean[] error = { false };

                    long n1 = get_tv_number_chk(rtv, error);
                    if (error[0])
                    {
                        /* This can only happen for "list + non-list".
                         * For "non-list + ..." or "something - ...",
                         * we returned before evaluating the 2nd operand. */
                        clear_tv(rtv);
                        return false;
                    }

                    long n2 = get_tv_number_chk(var2, error);
                    if (error[0])
                    {
                        clear_tv(rtv);
                        clear_tv(var2);
                        return false;
                    }
                    clear_tv(rtv);

                    if (op == '+')
                        n1 = n1 + n2;
                    else
                        n1 = n1 - n2;

                    rtv.tv_type = VAR_NUMBER;
                    rtv.tv_number = n1;
                }
                clear_tv(var2);
            }
        }

        return true;
    }

    /*
     * Handle fifth level expression:
     *      *       number multiplication
     *      /       number division
     *      %       number modulo
     *
     * "arg" must point to the first non-white of the expression.
     * "arg" is advanced to the next non-white after the recognized expression.
     *
     * Return true or false.
     */
    /*private*/ static boolean eval6(Bytes[] arg, typval_C rtv, boolean evaluate, boolean want_string)
        /* want_string: after "." operator */
    {
        /*
         * Get the first variable.
         */
        if (eval7(arg, rtv, evaluate, want_string) == false)
            return false;

        /*
         * Repeat computing, until no '*', '/' or '%' is following.
         */
        for (boolean[] error = { false }; ; )
        {
            byte op = arg[0].at(0);
            if (op != '*' && op != '/' && op != '%')
                break;

            long n1;

            if (evaluate)
            {
                n1 = get_tv_number_chk(rtv, error);
                clear_tv(rtv);
                if (error[0])
                    return false;
            }
            else
                n1 = 0;

            /*
             * Get the second variable.
             */
            arg[0] = skipwhite(arg[0].plus(1));
            typval_C var2 = new typval_C();
            if (eval7(arg, var2, evaluate, false) == false)
                return false;

            if (evaluate)
            {
                long n2 = get_tv_number_chk(var2, error);
                clear_tv(var2);
                if (error[0])
                    return false;

                /*
                 * Compute the result.
                 */
                if (op == '*')
                    n1 = n1 * n2;
                else if (op == '/')
                {
                    if (n2 == 0)        /* give an error message? */
                    {
                        if (n1 == 0)
                            n1 = -0x7fffffffL - 1L;     /* similar to NaN */
                        else if (n1 < 0)
                            n1 = -0x7fffffffL;
                        else
                            n1 = 0x7fffffffL;
                    }
                    else
                        n1 = n1 / n2;
                }
                else
                {
                    if (n2 == 0)        /* give an error message? */
                        n1 = 0;
                    else
                        n1 = n1 % n2;
                }

                rtv.tv_type = VAR_NUMBER;
                rtv.tv_number = n1;
            }
        }

        return true;
    }

    /*
     * Handle sixth level expression:
     *  number              number constant
     *  "string"            string constant
     *  'string'            literal string constant
     *  &option-name        option value
     *  @r                  register contents
     *  identifier          variable value
     *  function()          function call
     *  $VAR                environment variable
     *  (expression)        nested expression
     *  [expr, expr]        List
     *  {key: val, key: val}  Dictionary
     *
     *  Also handle:
     *  ! in front          logical NOT
     *  - in front          unary minus
     *  + in front          unary plus (ignored)
     *  trailing []         subscript in String or List
     *  trailing .name      entry in Dictionary
     *
     * "arg" must point to the first non-white of the expression.
     * "arg" is advanced to the next non-white after the recognized expression.
     *
     * Return true or false.
     */
    /*private*/ static boolean eval7(Bytes[] arg, typval_C rtv, boolean evaluate, boolean _want_string)
        /* want_string: after "." operator */
    {
        /*
         * Initialise variable so that clear_tv() can't mistake this
         * for a string and free a string that isn't there.
         */
        rtv.tv_type = VAR_UNKNOWN;

        /*
         * Skip '!' and '-' characters.  They are handled later.
         */
        Bytes start_leader = arg[0];
        while (arg[0].at(0) == (byte)'!' || arg[0].at(0) == (byte)'-' || arg[0].at(0) == (byte)'+')
            arg[0] = skipwhite(arg[0].plus(1));
        Bytes end_leader = arg[0];

        /*MAYBEAN*/int maybe = TRUE;

        switch (arg[0].at(0))
        {
            /*
             * Number constant.
             */
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
            {
                int[] len = new int[1];
                long[] n = new long[1];
                vim_str2nr(arg[0], null, len, TRUE, TRUE, n);
                arg[0] = arg[0].plus(len[0]);
                if (evaluate)
                {
                    rtv.tv_type = VAR_NUMBER;
                    rtv.tv_number = n[0];
                }
                break;
            }

            /*
             * String constant: "string".
             */
            case '"':   maybe = get_string_tv(arg, rtv, evaluate) ? TRUE : FALSE;
                        break;

            /*
             * Literal string constant: 'str''ing'.
             */
            case '\'':  maybe = get_lit_string_tv(arg, rtv, evaluate) ? TRUE : FALSE;
                        break;

            /*
             * List: [expr, expr]
             */
            case '[':   maybe = get_list_tv(arg, rtv, evaluate) ? TRUE : FALSE;
                        break;

            /*
             * Dictionary: {key: val, key: val}
             */
            case '{':   maybe = get_dict_tv(arg, rtv, evaluate);
                        break;

            /*
             * Option value: &name
             */
            case '&':   maybe = get_option_tv(arg, rtv, evaluate) ? TRUE : FALSE;
                        break;

            /*
             * Register contents: @r.
             */
            case '@':   arg[0] = arg[0].plus(1);
                        if (evaluate)
                        {
                            rtv.tv_type = VAR_STRING;
                            rtv.tv_string = (Bytes)get_reg_contents(arg[0].at(0), GREG_EXPR_SRC);
                        }
                        if (arg[0].at(0) != NUL)
                            arg[0] = arg[0].plus(1);
                        break;

            /*
             * nested expression: (expression).
             */
            case '(':   arg[0] = skipwhite(arg[0].plus(1));
                        maybe = eval1(arg, rtv, evaluate) ? TRUE : FALSE;     /* recursive! */
                        if (arg[0].at(0) == (byte)')')
                            arg[0] = arg[0].plus(1);
                        else if (maybe == TRUE)
                        {
                            emsg(u8("E110: Missing ')'"));
                            clear_tv(rtv);
                            maybe = FALSE;
                        }
                        break;

            default:    maybe = MAYBE;
                        break;
        }

        boolean ret;

        if (maybe != MAYBE)
            ret = (maybe != FALSE);
        else
        {
            /*
             * Must be a variable or function name.
             * Can also be a curly-braces kind of name: {expr}.
             */
            Bytes s = arg[0];
            Bytes[] alias = new Bytes[1];
            int[] len = { get_name_len(arg, alias, evaluate, true) };
            if (alias[0] != null)
                s = alias[0];

            if (len[0] <= 0)
                ret = false;
            else
            {
                if (arg[0].at(0) == (byte)'(')           /* recursive! */
                {
                    /* If "s" is the name of a variable of type VAR_FUNC use its contents. */
                    s = deref_func_name(s, len, !evaluate);

                    /* Invoke the function. */
                    boolean[] doesrange = new boolean[1];
                    ret = get_func_tv(s, len[0], rtv, arg,
                              curwin.w_cursor.lnum, curwin.w_cursor.lnum,
                              doesrange, evaluate, null);

                    /* If evaluate is false, rtv.tv_type was not set in get_func_tv,
                     * but it's needed in handle_subscript() to parse what follows.  So set it here. */
                    if (rtv.tv_type == VAR_UNKNOWN && !evaluate && arg[0].at(0) == (byte)'(')
                    {
                        rtv.tv_string = STRDUP(u8(""));
                        rtv.tv_type = VAR_FUNC;
                    }

                    /* Stop the expression evaluation when immediately aborting on error,
                     * or when an interrupt occurred or an exception was thrown but not caught. */
                    if (aborting())
                    {
                        if (ret)
                            clear_tv(rtv);
                        ret = false;
                    }
                }
                else if (evaluate)
                    ret = get_var_tv(s, len[0], rtv, true, false);
                else
                    ret = true;
            }
        }

        arg[0] = skipwhite(arg[0]);

        /* Handle following '[', '(' and '.' for expr[expr], expr.name, expr(expr). */
        if (ret)
            ret = handle_subscript(arg, rtv, evaluate, true);

        /*
         * Apply logical NOT and unary '-', from right to left, ignore '+'.
         */
        if (ret && evaluate && BLT(start_leader, end_leader))
        {
            boolean[] error = { false };

            long val = get_tv_number_chk(rtv, error);
            if (error[0])
            {
                clear_tv(rtv);
                ret = false;
            }
            else
            {
                while (BLT(start_leader, end_leader))
                {
                    end_leader = end_leader.minus(1);
                    if (end_leader.at(0) == (byte)'!')
                        val = (val == 0) ? TRUE : FALSE;
                    else if (end_leader.at(0) == (byte)'-')
                        val = -val;
                }
                clear_tv(rtv);
                rtv.tv_type = VAR_NUMBER;
                rtv.tv_number = val;
            }
        }

        return ret;
    }

    /*
     * Evaluate an "[expr]" or "[expr:expr]" index.  Also "dict.key".
     * "*arg" points to the '[' or '.'.
     * Returns false or true.
     * "*arg" is advanced to after the ']'.
     */
    /*private*/ static boolean eval_index(Bytes[] arg, typval_C rtv, boolean evaluate, boolean verbose)
        /* verbose: give error messages */
    {
        if (rtv.tv_type == VAR_FUNC)
        {
            if (verbose)
                emsg(u8("E695: Cannot index a Funcref"));
            return false;
        }

        boolean empty1 = false, empty2 = false;
        typval_C var1 = new typval_C();
        typval_C var2 = new typval_C();
        int n1, n2 = 0;
        int len = -1;
        boolean range = false;
        Bytes key = null;

        if (arg[0].at(0) == (byte)'.')
        {
            /*
             * dict.name
             */
            key = arg[0].plus(1);
            for (len = 0; asc_isalnum(key.at(len)) || key.at(len) == (byte)'_'; len++)
                ;
            if (len == 0)
                return false;
            arg[0] = skipwhite(key.plus(len));
        }
        else
        {
            /*
             * something[idx]
             *
             * Get the (first) variable from inside the [].
             */
            arg[0] = skipwhite(arg[0].plus(1));
            if (arg[0].at(0) == (byte)':')
                empty1 = true;
            else if (eval1(arg, var1, evaluate) == false)       /* recursive! */
                return false;
            else if (evaluate && get_tv_string_chk(var1) == null)
            {
                /* not a number or string */
                clear_tv(var1);
                return false;
            }

            /*
             * Get the second variable from inside the [:].
             */
            if (arg[0].at(0) == (byte)':')
            {
                range = true;
                arg[0] = skipwhite(arg[0].plus(1));
                if (arg[0].at(0) == (byte)']')
                    empty2 = true;
                else if (eval1(arg, var2, evaluate) == false)   /* recursive! */
                {
                    if (!empty1)
                        clear_tv(var1);
                    return false;
                }
                else if (evaluate && get_tv_string_chk(var2) == null)
                {
                    /* not a number or string */
                    if (!empty1)
                        clear_tv(var1);
                    clear_tv(var2);
                    return false;
                }
            }

            /* Check for the ']'. */
            if (arg[0].at(0) != (byte)']')
            {
                if (verbose)
                    emsg(e_missbrac);
                clear_tv(var1);
                if (range)
                    clear_tv(var2);
                return false;
            }
            arg[0] = skipwhite(arg[0].plus(1));     /* skip the ']' */
        }

        if (evaluate)
        {
            n1 = 0;
            if (!empty1 && rtv.tv_type != VAR_DICT)
            {
                n1 = (int)get_tv_number(var1);
                clear_tv(var1);
            }
            if (range)
            {
                if (empty2)
                    n2 = -1;
                else
                {
                    n2 = (int)get_tv_number(var2);
                    clear_tv(var2);
                }
            }

            switch (rtv.tv_type)
            {
                case VAR_NUMBER:
                case VAR_STRING:
                {
                    Bytes s = get_tv_string(rtv);
                    len = strlen(s);
                    if (range)
                    {
                        /* The resulting variable is a substring.
                         * If the indexes are out of range, the result is empty. */
                        if (n1 < 0)
                        {
                            n1 = len + n1;
                            if (n1 < 0)
                                n1 = 0;
                        }
                        if (n2 < 0)
                            n2 = len + n2;
                        else if (len <= n2)
                            n2 = len;
                        if (len <= n1 || n2 < 0 || n2 < n1)
                            s = null;
                        else
                            s = STRNDUP(s.plus(n1), n2 - n1 + 1);
                    }
                    else
                    {
                        /* The resulting variable is a string of a single character.
                         * If the index is too big or negative, the result is empty. */
                        if (len <= n1 || n1 < 0)
                            s = null;
                        else
                            s = STRNDUP(s.plus(n1), 1);
                    }
                    clear_tv(rtv);
                    rtv.tv_type = VAR_STRING;
                    rtv.tv_string = s;
                    break;
                }

                case VAR_LIST:
                {
                    len = list_len(rtv.tv_list);
                    if (n1 < 0)
                        n1 = len + n1;
                    if (!empty1 && (n1 < 0 || len <= n1))
                    {
                        /* For a range we allow invalid values and return an empty list.
                         * A list index out of range is an error. */
                        if (!range)
                        {
                            if (verbose)
                                emsgn(e_listidx, n1);
                            return false;
                        }
                        n1 = len;
                    }
                    if (range)
                    {
                        if (n2 < 0)
                            n2 = len + n2;
                        else if (len <= n2)
                            n2 = len - 1;
                        if (!empty2 && (n2 < 0 || n2 + 1 < n1))
                            n2 = -1;

                        list_C list = new list_C();

                        for (listitem_C item = list_find(rtv.tv_list, n1); n1 <= n2; n1++)
                        {
                            if (list_append_tv(list, item.li_tv) == false)
                            {
                                list_free(list, true);
                                return false;
                            }
                            item = item.li_next;
                        }

                        clear_tv(rtv);
                        rtv.tv_type = VAR_LIST;
                        rtv.tv_list = list;
                        list.lv_refcount++;
                    }
                    else
                    {
                        copy_tv(list_find(rtv.tv_list, n1).li_tv, var1);
                        clear_tv(rtv);
                        COPY_typval(rtv, var1);
                    }
                    break;
                }

                case VAR_DICT:
                {
                    if (range)
                    {
                        if (verbose)
                            emsg(e_dictrange);
                        if (len == -1)
                            clear_tv(var1);
                        return false;
                    }

                    if (len == -1)
                    {
                        key = get_tv_string(var1);
                        if (key.at(0) == NUL)
                        {
                            if (verbose)
                                emsg(e_emptykey);
                            clear_tv(var1);
                            return false;
                        }
                    }

                    dictitem_C item = dict_find(rtv.tv_dict, key, len);

                    if (item == null && verbose)
                        emsg2(e_dictkey, key);
                    if (len == -1)
                        clear_tv(var1);
                    if (item == null)
                        return false;

                    copy_tv(item.di_tv, var1);
                    clear_tv(rtv);
                    COPY_typval(rtv, var1);
                    break;
                }
            }
        }

        return true;
    }

    /*
     * Get an option value.
     * "arg" points to the '&' or '+' before the option name.
     * "arg" is advanced to character after the option name.
     * Return true or false.
     */
    /*private*/ static boolean get_option_tv(Bytes[] arg, typval_C rtv, boolean evaluate)
        /* rtv: when null, only check if option exists */
    {
        boolean working = (arg[0].at(0) == (byte)'+');       /* has("+option") */

        /*
         * Isolate the option name and find its value.
         */
        int[] opt_flags = new int[1];
        Bytes option_end = find_option_end(arg, opt_flags);
        if (option_end == null)
        {
            if (rtv != null)
                emsg2(u8("E112: Option name missing: %s"), arg[0]);
            return false;
        }

        if (!evaluate)
        {
            arg[0] = option_end;
            return true;
        }

        byte c = option_end.at(0);
        option_end.be(0, NUL);

        long[] numval = new long[1];
        Bytes[] stringval = new Bytes[1];
        int opt_type = get_option_value(arg[0], numval, (rtv == null) ? null : stringval, opt_flags[0]);

        boolean ret = true;

        if (opt_type == -3)                 /* invalid name */
        {
            if (rtv != null)
                emsg2(u8("E113: Unknown option: %s"), arg[0]);
            ret = false;
        }
        else if (rtv != null)
        {
            if (opt_type == -2)             /* hidden string option */
            {
                rtv.tv_type = VAR_STRING;
                rtv.tv_string = null;
            }
            else if (opt_type == -1)        /* hidden number option */
            {
                rtv.tv_type = VAR_NUMBER;
                rtv.tv_number = 0;
            }
            else if (opt_type == 1)         /* number option */
            {
                rtv.tv_type = VAR_NUMBER;
                rtv.tv_number = numval[0];
            }
            else                            /* string option */
            {
                rtv.tv_type = VAR_STRING;
                rtv.tv_string = stringval[0];
            }
        }
        else if (working && (opt_type == -2 || opt_type == -1))
            ret = false;

        option_end.be(0, c);                    /* put back for error messages */
        arg[0] = option_end;

        return ret;
    }

    /*
     * Allocate a variable for a string constant.
     * Return true or false.
     */
    /*private*/ static boolean get_string_tv(Bytes[] arg, typval_C rtv, boolean evaluate)
    {
        Bytes p;
        int extra = 0;

        /*
         * Find the end of the string, skipping backslashed characters.
         */
        for (p = arg[0].plus(1); p.at(0) != NUL && p.at(0) != (byte)'"'; p = p.plus(us_ptr2len_cc(p)))
        {
            if (p.at(0) == (byte)'\\' && p.at(1) != NUL)
            {
                p = p.plus(1);
                /* A "\<x>" form occupies at least 4 characters,
                 * and produces up to 6 characters: reserve space for 2 extra. */
                if (p.at(0) == (byte)'<')
                    extra += 2;
            }
        }

        if (p.at(0) != (byte)'"')
        {
            emsg2(u8("E114: Missing quote: %s"), arg[0]);
            return false;
        }

        /* If only parsing, set arg[0] and return here. */
        if (!evaluate)
        {
            arg[0] = p.plus(1);
            return true;
        }

        /*
         * Copy the string into allocated memory, handling backslashed characters.
         */
        Bytes name = new Bytes(BDIFF(p, arg[0]) + extra);

        rtv.tv_type = VAR_STRING;
        rtv.tv_string = name;

        for (p = arg[0].plus(1); p.at(0) != NUL && p.at(0) != (byte)'"'; )
        {
            if (p.at(0) == (byte)'\\')
            {
                switch ((p = p.plus(1)).at(0))
                {
                    case 'b': (name = name.plus(1)).be(-1, BS);  p = p.plus(1); break;
                    case 'e': (name = name.plus(1)).be(-1, ESC); p = p.plus(1); break;
                    case 'f': (name = name.plus(1)).be(-1, FF);  p = p.plus(1); break;
                    case 'n': (name = name.plus(1)).be(-1, NL);  p = p.plus(1); break;
                    case 'r': (name = name.plus(1)).be(-1, CAR); p = p.plus(1); break;
                    case 't': (name = name.plus(1)).be(-1, TAB); p = p.plus(1); break;

                    case 'X': /* hex: "\x1", "\x12" */
                    case 'x':
                    case 'u': /* Unicode: "\u0023" */
                    case 'U':
                    {
                        if (asc_isxdigit(p.at(1)))
                        {
                            int c = asc_toupper(p.at(0));

                            int n;
                            if (c == 'X')
                                n = 2;
                            else
                                n = 4;
                            int nr = 0;
                            while (0 <= --n && asc_isxdigit(p.at(1)))
                            {
                                p = p.plus(1);
                                nr = (nr << 4) + hex2nr(p.at(0));
                            }
                            p = p.plus(1);
                            /* For "\\u" (sic!) store the number according to 'encoding'. */
                            if (c != 'X')
                                name = name.plus(utf_char2bytes(nr, name));
                            else
                                (name = name.plus(1)).be(-1, nr);
                        }
                        break;
                    }

                    case '0': /* octal: "\1", "\12", "\123" */
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    {
                        name.be(0, (p = p.plus(1)).at(-1) - '0');
                        if ('0' <= p.at(0) && p.at(0) <= '7')
                        {
                            name.be(0, (name.at(0) << 3) + (p = p.plus(1)).at(-1) - '0');
                            if ('0' <= p.at(0) && p.at(0) <= '7')
                                name.be(0, (name.at(0) << 3) + (p = p.plus(1)).at(-1) - '0');
                        }
                        name = name.plus(1);
                        break;
                    }

                    case '<': /* Special key, e.g.: "\<C-W>" */
                    {
                        { Bytes[] __ = { p }; extra = trans_special(__, name, true); p = __[0]; }
                        if (extra != 0)
                        {
                            name = name.plus(extra);
                            break;
                        }
                        /* FALLTHROUGH */
                    }

                    default:
                    {
                        int l = us_ptr2len_cc(p);
                        BCOPY(name, p, l);
                        name = name.plus(l);
                        p = p.plus(l);
                        break;
                    }
                }
            }
            else
            {
                int l = us_ptr2len_cc(p);
                BCOPY(name, p, l);
                name = name.plus(l);
                p = p.plus(l);
            }
        }
        name.be(0, NUL);
        arg[0] = p.plus(1);

        return true;
    }

    /*
     * Allocate a variable for a 'str''ing' constant.
     * Return true or false.
     */
    /*private*/ static boolean get_lit_string_tv(Bytes[] arg, typval_C rtv, boolean evaluate)
    {
        Bytes p;
        int reduce = 0;

        /*
         * Find the end of the string, skipping ''.
         */
        for (p = arg[0].plus(1); p.at(0) != NUL; p = p.plus(us_ptr2len_cc(p)))
        {
            if (p.at(0) == (byte)'\'')
            {
                if (p.at(1) != (byte)'\'')
                    break;
                reduce++;
                p = p.plus(1);
            }
        }

        if (p.at(0) != (byte)'\'')
        {
            emsg2(u8("E115: Missing quote: %s"), arg[0]);
            return false;
        }

        /* If only parsing, return after setting arg[0]. */
        if (!evaluate)
        {
            arg[0] = p.plus(1);
            return true;
        }

        /*
         * Copy the string into allocated memory, handling '' to ' reduction.
         */
        Bytes str = new Bytes(BDIFF(p, arg[0]) - reduce);

        rtv.tv_type = VAR_STRING;
        rtv.tv_string = str;

        for (p = arg[0].plus(1); p.at(0) != NUL; )
        {
            if (p.at(0) == (byte)'\'')
            {
                if (p.at(1) != (byte)'\'')
                    break;
                p = p.plus(1);
            }
            int l = us_ptr2len_cc(p);
            BCOPY(str, p, l);
            str = str.plus(l);
            p = p.plus(l);
        }
        str.be(0, NUL);
        arg[0] = p.plus(1);

        return true;
    }

    /*
     * Allocate a variable for a List and fill it from "*arg".
     * Return true or false.
     */
    /*private*/ static boolean get_list_tv(Bytes[] arg, typval_C rtv, boolean evaluate)
    {
        list_C list = null;
        if (evaluate)
            list = new list_C();

        failret:
        {
            arg[0] = skipwhite(arg[0].plus(1));
            while (arg[0].at(0) != (byte)']' && arg[0].at(0) != NUL)
            {
                typval_C tv = new typval_C();

                if (eval1(arg, tv, evaluate) == false)      /* recursive! */
                    break failret;

                if (evaluate)
                {
                    listitem_C item = new listitem_C();

                    COPY_typval(item.li_tv, tv);
                    item.li_tv.tv_lock = 0;
                    list_append(list, item);
                }

                if (arg[0].at(0) == (byte)']')
                    break;
                if (arg[0].at(0) != (byte)',')
                {
                    emsg2(u8("E696: Missing comma in List: %s"), arg[0]);
                    break failret;
                }
                arg[0] = skipwhite(arg[0].plus(1));
            }

            if (arg[0].at(0) != (byte)']')
            {
                emsg2(u8("E697: Missing end of List ']': %s"), arg[0]);
                break failret;
            }

            arg[0] = skipwhite(arg[0].plus(1));
            if (evaluate)
            {
                rtv.tv_type = VAR_LIST;
                rtv.tv_list = list;
                list.lv_refcount++;
            }

            return true;
        }

        if (evaluate)
            list_free(list, true);

        return false;
    }

    /*
     * Allocate an empty list for a return value.
     */
    /*private*/ static void rettv_list_alloc(typval_C rtv)
    {
        list_C list = new list_C();

        rtv.tv_list = list;
        rtv.tv_type = VAR_LIST;
        list.lv_refcount++;
    }

    /*
     * Unreference a list: decrement the reference count and free it when it becomes zero.
     */
    /*private*/ static void list_unref(list_C l)
    {
        if (l != null && --l.lv_refcount <= 0)
            list_free(l, true);
    }

    /*
     * Free a list, including all non-container items it points to.
     * Ignores the reference count.
     */
    /*private*/ static void list_free(list_C l, boolean recurse)
        /* recurse: Free Lists and Dictionaries recursively. */
    {
        for (listitem_C item = l.lv_first; item != null; item = l.lv_first)
        {
            /* Remove the item before deleting it. */
            l.lv_first = item.li_next;
            if (recurse || (item.li_tv.tv_type != VAR_LIST && item.li_tv.tv_type != VAR_DICT))
                clear_tv(item.li_tv);
        }
    }

    /*
     * Free a list item.  Also clears the value.  Does not notify watchers.
     */
    /*private*/ static void listitem_free(listitem_C item)
    {
        clear_tv(item.li_tv);
    }

    /*
     * Remove a list item from a List and free it.  Also clears the value.
     */
    /*private*/ static void listitem_remove(list_C l, listitem_C item)
    {
        vimlist_remove(l, item, item);
        listitem_free(item);
    }

    /*
     * Get the number of items in a list.
     */
    /*private*/ static int list_len(list_C l)
    {
        if (l == null)
            return 0;

        return l.lv_len;
    }

    /*
     * Return true when two lists have exactly the same values.
     */
    /*private*/ static boolean list_equal(list_C l1, list_C l2, boolean ic, boolean recursive)
        /* ic: ignore case for strings */
        /* recursive: true when used recursively */
    {
        if (l1 == null || l2 == null)
            return false;
        if (l1 == l2)
            return true;
        if (list_len(l1) != list_len(l2))
            return false;

        listitem_C item1, item2;

        for (item1 = l1.lv_first, item2 = l2.lv_first;
                    item1 != null && item2 != null;
                            item1 = item1.li_next, item2 = item2.li_next)
            if (!tv_equal(item1.li_tv, item2.li_tv, ic, recursive))
                return false;

        return (item1 == null && item2 == null);
    }

    /*
     * Return true when two dictionaries have exactly the same key/values.
     */
    /*private*/ static boolean dict_equal(dict_C d1, dict_C d2, boolean ic, boolean recursive)
        /* ic: ignore case for strings */
        /* recursive: true when used recursively */
    {
        if (d1 == null || d2 == null)
            return false;
        if (d1 == d2)
            return true;
        if (dict_len(d1) != dict_len(d2))
            return false;

        for (int i = 0, todo = (int)d1.dv_hashtab.ht_used; 0 < todo; i++)
        {
            hashitem_C hi = d1.dv_hashtab.ht_buckets[i];
            if (!hashitem_empty(hi))
            {
                dictitem_C item2 = dict_find(d2, hi.hi_key, -1);
                if (item2 == null)
                    return false;
                if (!tv_equal(((dictitem_C)hi.hi_data).di_tv, item2.di_tv, ic, recursive))
                    return false;
                --todo;
            }
        }
        return true;
    }

    /*private*/ static int tv_equal_recurse_limit;

    /*private*/ static int recursive_cnt;           /* catch recursive loops */

    /*
     * Return true if "tv1" and "tv2" have the same value.
     * Compares the items just like "==" would compare them, but strings and
     * numbers are different.  Floats and numbers are also different.
     */
    /*private*/ static boolean tv_equal(typval_C tv1, typval_C tv2, boolean ic, boolean recursive)
        /* ic: ignore case */
        /* recursive: true when used recursively */
    {
        if (tv1.tv_type != tv2.tv_type)
            return false;

        /* Catch lists and dicts that have an endless loop by limiting
         * recursiveness to a limit.  We guess they are equal then.
         * A fixed limit has the problem of still taking an awful long time.
         * Reduce the limit every time running into it.  That should work fine for
         * deeply linked structures that are not recursively linked and catch
         * recursiveness quickly. */
        if (!recursive)
            tv_equal_recurse_limit = 1000;
        if (tv_equal_recurse_limit <= recursive_cnt)
        {
            --tv_equal_recurse_limit;
            return true;
        }

        switch (tv1.tv_type)
        {
            case VAR_LIST:
            {
                recursive_cnt++;
                boolean r = list_equal(tv1.tv_list, tv2.tv_list, ic, true);
                --recursive_cnt;
                return r;
            }

            case VAR_DICT:
            {
                recursive_cnt++;
                boolean r = dict_equal(tv1.tv_dict, tv2.tv_dict, ic, true);
                --recursive_cnt;
                return r;
            }

            case VAR_FUNC:
                return (tv1.tv_string != null && tv2.tv_string != null
                        && STRCMP(tv1.tv_string, tv2.tv_string) == 0);

            case VAR_NUMBER:
                return (tv1.tv_number == tv2.tv_number);

            case VAR_STRING:
            {
                Bytes s1 = get_tv_string(tv1);
                Bytes s2 = get_tv_string(tv2);
                return ((ic ? us_strnicmp(s1, s2, MAXCOL) : STRCMP(s1, s2)) == 0);
            }
        }

        emsg2(e_intern2, u8("tv_equal()"));
        return true;
    }

    /*
     * Locate item with index "n" in list "l" and return it.
     * A negative index is counted from the end; -1 is the last item.
     * Returns null when "n" is out of range.
     */
    /*private*/ static listitem_C list_find(list_C l, long n)
    {
        if (l == null)
            return null;

        /* Negative index is relative to the end. */
        if (n < 0)
            n = l.lv_len + n;

        /* Check for index out of range. */
        if (n < 0 || l.lv_len <= n)
            return null;

        listitem_C item;
        int idx;

        /* When there is a cached index may start search from there. */
        if (l.lv_idx_item != null)
        {
            if (n < l.lv_idx / 2)
            {
                /* closest to the start of the list */
                item = l.lv_first;
                idx = 0;
            }
            else if ((l.lv_idx + l.lv_len) / 2 < n)
            {
                /* closest to the end of the list */
                item = l.lv_last;
                idx = l.lv_len - 1;
            }
            else
            {
                /* closest to the cached index */
                item = l.lv_idx_item;
                idx = l.lv_idx;
            }
        }
        else
        {
            if (n < l.lv_len / 2)
            {
                /* closest to the start of the list */
                item = l.lv_first;
                idx = 0;
            }
            else
            {
                /* closest to the end of the list */
                item = l.lv_last;
                idx = l.lv_len - 1;
            }
        }

        while (idx < n)
        {
            /* search forward */
            item = item.li_next;
            idx++;
        }
        while (n < idx)
        {
            /* search backward */
            item = item.li_prev;
            --idx;
        }

        /* cache the used index */
        l.lv_idx = idx;
        l.lv_idx_item = item;

        return item;
    }

    /*
     * Get list item "l[idx]" as a number.
     */
    /*private*/ static long list_find_nr(list_C l, long idx, boolean[] errorp)
        /* errorp: set to true when something wrong */
    {
        listitem_C li = list_find(l, idx);
        if (li == null)
        {
            if (errorp != null)
                errorp[0] = true;
            return -1L;
        }
        return get_tv_number_chk(li.li_tv, errorp);
    }

    /*
     * Get list item "l[idx - 1]" as a string.  Returns null for failure.
     */
    /*private*/ static Bytes list_find_str(list_C l, long idx)
    {
        listitem_C li = list_find(l, idx - 1);
        if (li == null)
        {
            emsgn(e_listidx, idx);
            return null;
        }
        return get_tv_string(li.li_tv);
    }

    /*
     * Locate "item" list "l" and return its index.
     * Returns -1 when "item" is not in the list.
     */
    /*private*/ static long list_idx_of_item(list_C l, listitem_C item)
    {
        if (l == null)
            return -1;

        long idx = 0;

        listitem_C li;
        for (li = l.lv_first; li != null && li != item; li = li.li_next)
            idx++;
        if (li == null)
            return -1;

        return idx;
    }

    /*
     * Append item "item" to the end of list "l".
     */
    /*private*/ static void list_append(list_C l, listitem_C item)
    {
        if (l.lv_last == null)
        {
            /* empty list */
            l.lv_first = item;
            l.lv_last = item;
            item.li_prev = null;
        }
        else
        {
            l.lv_last.li_next = item;
            item.li_prev = l.lv_last;
            l.lv_last = item;
        }
        l.lv_len++;
        item.li_next = null;
    }

    /*
     * Append typval_C "tv" to the end of list "l".
     * Return false when out of memory.
     */
    /*private*/ static boolean list_append_tv(list_C l, typval_C tv)
    {
        listitem_C li = new listitem_C();

        copy_tv(tv, li.li_tv);
        list_append(l, li);
        return true;
    }

    /*
     * Add a dictionary to a list.
     * Return false when out of memory.
     */
    /*private*/ static boolean list_append_dict(list_C list, dict_C dict)
    {
        listitem_C li = new listitem_C();

        li.li_tv.tv_type = VAR_DICT;
        li.li_tv.tv_lock = 0;
        li.li_tv.tv_dict = dict;
        list_append(list, li);
        dict.dv_refcount++;
        return true;
    }

    /*
     * Make a copy of "str" and append it as an item to list "l".
     * When "len" >= 0 use "str[len]".
     * Returns false when out of memory.
     */
    /*private*/ static boolean list_append_string(list_C l, Bytes str, int len)
    {
        listitem_C li = new listitem_C();

        list_append(l, li);
        li.li_tv.tv_type = VAR_STRING;
        li.li_tv.tv_lock = 0;
        if (str == null)
            li.li_tv.tv_string = null;
        else if ((li.li_tv.tv_string = (0 <= len) ? STRNDUP(str, len) : STRDUP(str)) == null)
            return false;

        return true;
    }

    /*
     * Append "n" to list "l".
     */
    /*private*/ static void list_append_number(list_C l, long n)
    {
        listitem_C li = new listitem_C();

        li.li_tv.tv_type = VAR_NUMBER;
        li.li_tv.tv_lock = 0;
        li.li_tv.tv_number = n;

        list_append(l, li);
    }

    /*
     * Insert typval_C "tv" in list "l" before "item".
     * If "item" is null append at the end.
     * Return false when out of memory.
     */
    /*private*/ static boolean list_insert_tv(list_C l, typval_C tv, listitem_C item)
    {
        listitem_C ni = new listitem_C();

        copy_tv(tv, ni.li_tv);
        list_insert(l, ni, item);
        return true;
    }

    /*private*/ static void list_insert(list_C l, listitem_C ni, listitem_C item)
    {
        if (item == null)
            /* Append new item at end of list. */
            list_append(l, ni);
        else
        {
            /* Insert new item before existing item. */
            ni.li_prev = item.li_prev;
            ni.li_next = item;
            if (item.li_prev == null)
            {
                l.lv_first = ni;
                l.lv_idx++;
            }
            else
            {
                item.li_prev.li_next = ni;
                l.lv_idx_item = null;
            }
            item.li_prev = ni;
            l.lv_len++;
        }
    }

    /*
     * Extend "l1" with "l2".
     * If "bef" is null append at the end, otherwise insert before this item.
     * Returns false when out of memory.
     */
    /*private*/ static boolean list_extend(list_C l1, list_C l2, listitem_C bef)
    {
        int todo = l2.lv_len;

        /* We also quit the loop when we have inserted the original item count of the list,
         * avoid a hang when we extend a list with itself. */
        for (listitem_C item = l2.lv_first; item != null && 0 <= --todo; item = item.li_next)
            if (list_insert_tv(l1, item.li_tv, bef) == false)
                return false;

        return true;
    }

    /*
     * Concatenate lists "l1" and "l2" into a new list, stored in "tv".
     * Return false when out of memory.
     */
    /*private*/ static boolean list_concat(list_C l1, list_C l2, typval_C tv)
    {
        if (l1 == null || l2 == null)
            return false;

        /* make a copy of the first list. */
        list_C l = list_copy(l1, false, 0);
        if (l == null)
            return false;

        tv.tv_type = VAR_LIST;
        tv.tv_list = l;

        /* append all items from the second list */
        return list_extend(l, l2, null);
    }

    /*
     * Make a copy of list "orig".  Shallow if "deep" is false.
     * The refcount of the new list is set to 1.
     * See item_copy() for "copyID".
     * Returns null when out of memory.
     */
    /*private*/ static list_C list_copy(list_C orig, boolean deep, int copyID)
    {
        if (orig == null)
            return null;

        list_C copy = new list_C();

        if (copyID != 0)
        {
            /* Do this before adding the items,
             * because one of the items may refer back to this list. */
            orig.lv_copyID = copyID;
            orig.lv_copylist = copy;
        }

        listitem_C item;
        for (item = orig.lv_first; item != null && !got_int; item = item.li_next)
        {
            listitem_C ni = new listitem_C();

            if (deep)
            {
                if (item_copy(item.li_tv, ni.li_tv, deep, copyID) == false)
                    break;
            }
            else
                copy_tv(item.li_tv, ni.li_tv);
            list_append(copy, ni);
        }
        copy.lv_refcount++;
        if (item != null)
        {
            list_unref(copy);
            copy = null;
        }

        return copy;
    }

    /*
     * Remove items "item" to "item2" from list "l".
     * Does not free the listitem or the value!
     * This used to be called list_remove, but that conflicts with a Sun header file.
     */
    /*private*/ static void vimlist_remove(list_C l, listitem_C item, listitem_C item2)
    {
        /* notify watchers */
        for (listitem_C ip = item; ip != null; ip = ip.li_next)
        {
            --l.lv_len;
            list_fix_watch(l, ip);
            if (ip == item2)
                break;
        }

        if (item2.li_next == null)
            l.lv_last = item.li_prev;
        else
            item2.li_next.li_prev = item.li_prev;
        if (item.li_prev == null)
            l.lv_first = item2.li_next;
        else
            item.li_prev.li_next = item2.li_next;
        l.lv_idx_item = null;
    }

    /*
     * Return an allocated string with the string representation of a list.
     * May return null.
     */
    /*private*/ static Bytes list2string(typval_C tv, int copyID)
    {
        if (tv.tv_list == null)
            return null;

        barray_C ba = new barray_C(80);

        ba_append(ba, (byte)'[');
        if (list_join(ba, tv.tv_list, u8(", "), false, copyID) == false)
            return null;

        ba_append(ba, (byte)']');
        ba_append(ba, NUL);

        return new Bytes(ba.ba_data);
    }

    /*private*/ static boolean list_join_inner(barray_C bap, list_C l, Bytes sep, boolean echo_style, int copyID, Growing<Bytes> join_gap)
        /* bap: to store the result in */
        /* join_gap: to keep each list item string */
    {
        int sumlen = 0;

        /* Stringify each item in the list. */
        for (listitem_C item = l.lv_first; item != null && !got_int; item = item.li_next)
        {
            Bytes s;
            if (echo_style)
                s = echo_string(item.li_tv, copyID);
            else
                s = tv2string(item.li_tv, copyID);
            if (s == null)
                return false;

            sumlen += strlen(s);

            join_gap.ga_grow(1);
            join_gap.ga_data[join_gap.ga_len++] = s;

            line_breakcheck();
            if (did_echo_string_emsg)   /* recursion error, bail out */
                break;
        }

        /* Allocate result buffer with its total size, avoid re-allocation and multiple copy operations.
         * Add 2 for a tailing ']' and NUL. */
        if (2 <= join_gap.ga_len)
            sumlen += strlen(sep) * (join_gap.ga_len - 1);
        ba_grow(bap, sumlen + 2);

        for (int i = 0; i < join_gap.ga_len && !got_int; i++)
        {
            if (0 < i)
                ba_concat(bap, sep);
            ba_concat(bap, join_gap.ga_data[i]);

            line_breakcheck();
        }

        return true;
    }

    /*
     * Join list "l" into a string in "bap", using separator "sep".
     * When "echo_style" is true use String as echoed, otherwise as inside a List.
     * Return false or true.
     */
    /*private*/ static boolean list_join(barray_C bap, list_C l, Bytes sep, boolean echo_style, int copyID)
    {
        Growing<Bytes> join_ga = new Growing<Bytes>(Bytes.class, l.lv_len);

        boolean retval = list_join_inner(bap, l, sep, echo_style, copyID, join_ga);

        /* Dispose each item in join_ga. */
        if (join_ga.ga_data != null)
        {
            for (int i = 0; i < join_ga.ga_len; i++)
                join_ga.ga_data[i] = null;
            join_ga.ga_clear();
        }

        return retval;
    }

    /*
     * Mark all lists and dicts referenced through hashtab "ht" with "copyID".
     * "list_stack" is used to add lists to be marked.  Can be null.
     *
     * Returns true if setting references failed somehow.
     */
    /*private*/ static boolean set_ref_in_ht(hashtab_C ht, int copyID, list_stack_C[] list_stack)
    {
        boolean abort = false;

        ht_stack_C[] ht_stack = { null };

        for (hashtab_C cur_ht = ht; ; )
        {
            if (!abort)
            {
                /* Mark each item in the hashtab.
                 * If the item contains a hashtab, it is added to ht_stack;
                 * if it contains a list, it is added to list_stack. */
                for (int i = 0, todo = (int)cur_ht.ht_used; 0 < todo; i++)
                {
                    hashitem_C hi = cur_ht.ht_buckets[i];
                    if (!hashitem_empty(hi))
                    {
                        abort |= set_ref_in_item(((dictitem_C)hi.hi_data).di_tv, copyID, ht_stack, list_stack);
                        --todo;
                    }
                }
            }

            if (ht_stack[0] == null)
                break;

            /* take an item from the stack */
            cur_ht = ht_stack[0].ht;
            ht_stack[0] = ht_stack[0].prev;
        }

        return abort;
    }

    /*
     * Mark all lists and dicts referenced through list "l" with "copyID".
     * "ht_stack" is used to add hashtabs to be marked.  Can be null.
     *
     * Returns true if setting references failed somehow.
     */
    /*private*/ static boolean set_ref_in_list(list_C l, int copyID, ht_stack_C[] ht_stack)
    {
        boolean abort = false;

        list_stack_C[] list_stack = { null };

        for (list_C cur_l = l; ; list_stack[0] = list_stack[0].prev)
        {
            if (!abort)
                /* Mark each item in the list.
                 * If the item contains a hashtab, it is added to ht_stack,
                 * if it contains a list, it is added to list_stack. */
                for (listitem_C li = cur_l.lv_first; !abort && li != null; li = li.li_next)
                    abort |= set_ref_in_item(li.li_tv, copyID, ht_stack, list_stack);
            if (list_stack[0] == null)
                break;

            /* take an item from the stack */
            cur_l = list_stack[0].list;
        }

        return abort;
    }

    /*
     * Mark all lists and dicts referenced through typval "tv" with "copyID".
     * "list_stack" is used to add lists to be marked.  Can be null.
     * "ht_stack" is used to add hashtabs to be marked.  Can be null.
     *
     * Returns true if setting references failed somehow.
     */
    /*private*/ static boolean set_ref_in_item(typval_C tv, int copyID, ht_stack_C[] ht_stack, list_stack_C[] list_stack)
    {
        boolean abort = false;

        switch (tv.tv_type)
        {
            case VAR_DICT:
            {
                dict_C dd = tv.tv_dict;
                if (dd != null && dd.dv_copyID != copyID)
                {
                    /* Didn't see this dict yet. */
                    dd.dv_copyID = copyID;
                    if (ht_stack == null)
                    {
                        abort = set_ref_in_ht(dd.dv_hashtab, copyID, list_stack);
                    }
                    else
                    {
                        ht_stack_C newitem = new ht_stack_C();

                        newitem.ht = dd.dv_hashtab;
                        newitem.prev = ht_stack[0];
                        ht_stack[0] = newitem;
                    }
                }
                break;
            }

            case VAR_LIST:
            {
                list_C ll = tv.tv_list;
                if (ll != null && ll.lv_copyID != copyID)
                {
                    /* Didn't see this list yet. */
                    ll.lv_copyID = copyID;
                    if (list_stack == null)
                    {
                        abort = set_ref_in_list(ll, copyID, ht_stack);
                    }
                    else
                    {
                        list_stack_C newitem = new list_stack_C();

                        newitem.list = ll;
                        newitem.prev = list_stack[0];
                        list_stack[0] = newitem;
                    }
                }
                break;
            }
        }

        return abort;
    }

    /*
     * Allocate an empty header for a dictionary.
     */
    /*private*/ static dict_C newDict()
    {
        dict_C dict = new dict_C();

        hash_init(dict.dv_hashtab);
        dict.dv_lock = 0;
        dict.dv_scope = 0;
        dict.dv_refcount = 0;
        dict.dv_copyID = 0;

        return dict;
    }

    /*
     * Allocate an empty dict for a return value.
     */
    /*private*/ static void rettv_dict_alloc(typval_C rtv)
    {
        dict_C dict = newDict();

        rtv.tv_dict = dict;
        rtv.tv_type = VAR_DICT;
        dict.dv_refcount++;
    }

    /*
     * Unreference a Dictionary: decrement the reference count and free it when it becomes zero.
     */
    /*private*/ static void dict_unref(dict_C dict)
    {
        if (dict != null && --dict.dv_refcount <= 0)
            dict_free(dict, true);
    }

    /*
     * Free a Dictionary, including all non-container items it contains.
     * Ignores the reference count.
     */
    /*private*/ static void dict_free(dict_C dict, boolean recurse)
        /* recurse: Free Lists and Dictionaries recursively. */
    {
        /* Lock the hashtab, we don't want it to resize while freeing items. */
        hash_lock(dict.dv_hashtab);
        for (int i = 0, todo = (int)dict.dv_hashtab.ht_used; 0 < todo; i++)
        {
            hashitem_C hi = dict.dv_hashtab.ht_buckets[i];
            if (!hashitem_empty(hi))
            {
                --todo;
                /* Remove the item before deleting it,
                 * just in case there is something recursive causing trouble. */
                dictitem_C di = (dictitem_C)hi.hi_data;
                hash_remove(dict.dv_hashtab, hi);
                if (recurse || (di.di_tv.tv_type != VAR_LIST && di.di_tv.tv_type != VAR_DICT))
                    clear_tv(di.di_tv);
            }
        }
        hash_clear(dict.dv_hashtab);
    }

    /*
     * Allocate a Dictionary item.
     * The "key" is copied to the new item.
     * Note that the value of the item "di_tv" still needs to be initialized!
     */
    /*private*/ static dictitem_C dictitem_alloc(Bytes key)
    {
        dictitem_C di = new dictitem_C();

        di.di_key = STRDUP(key);

        return di;
    }

    /*
     * Make a copy of a Dictionary item.
     */
    /*private*/ static dictitem_C dictitem_copy(dictitem_C org)
    {
        dictitem_C di = dictitem_alloc(org.di_key);

        copy_tv(org.di_tv, di.di_tv);

        return di;
    }

    /*
     * Remove item "item" from Dictionary "dict" and free it.
     */
    /*private*/ static void dictitem_remove(dict_C dict, dictitem_C item)
    {
        hashitem_C hi = hash_find(dict.dv_hashtab, item.di_key);
        if (hashitem_empty(hi))
            emsg2(e_intern2, u8("dictitem_remove()"));
        else
            hash_remove(dict.dv_hashtab, hi);
        dictitem_free(item);
    }

    /*
     * Free a dict item.  Also clears the value.
     */
    /*private*/ static void dictitem_free(dictitem_C item)
    {
        clear_tv(item.di_tv);
    }

    /*
     * Make a copy of dict "d".  Shallow if "deep" is false.
     * The refcount of the new dict is set to 1.
     * See item_copy() for "copyID".
     * Returns null when out of memory.
     */
    /*private*/ static dict_C dict_copy(dict_C orig, boolean deep, int copyID)
    {
        if (orig == null)
            return null;

        dict_C copy = newDict();

        if (copyID != 0)
        {
            orig.dv_copyID = copyID;
            orig.dv_copydict = copy;
        }

        int todo = (int)orig.dv_hashtab.ht_used;
        for (int i = 0; 0 < todo && !got_int; i++)
        {
            hashitem_C hi = orig.dv_hashtab.ht_buckets[i];
            if (!hashitem_empty(hi))
            {
                --todo;

                dictitem_C di = dictitem_alloc(hi.hi_key);
                if (deep)
                {
                    if (item_copy(((dictitem_C)hi.hi_data).di_tv, di.di_tv, deep, copyID) == false)
                        break;
                }
                else
                    copy_tv(((dictitem_C)hi.hi_data).di_tv, di.di_tv);
                if (!dict_add(copy, di))
                {
                    dictitem_free(di);
                    break;
                }
            }
        }

        copy.dv_refcount++;
        if (0 < todo)
        {
            dict_unref(copy);
            copy = null;
        }

        return copy;
    }

    /*
     * Add item "item" to Dictionary "d".
     * Returns false when out of memory and when key already exists.
     */
    /*private*/ static boolean dict_add(dict_C d, dictitem_C item)
    {
        return hash_add(d.dv_hashtab, item, item.di_key);
    }

    /*
     * Add a number or string entry to dictionary "d".
     * When "str" is null use number "nr", otherwise use "str".
     * Returns false when out of memory and when key already exists.
     */
    /*private*/ static boolean dict_add_nr_str(dict_C d, Bytes key, long nr, Bytes str)
    {
        dictitem_C item = dictitem_alloc(key);

        item.di_tv.tv_lock = 0;

        if (str == null)
        {
            item.di_tv.tv_type = VAR_NUMBER;
            item.di_tv.tv_number = nr;
        }
        else
        {
            item.di_tv.tv_type = VAR_STRING;
            item.di_tv.tv_string = STRDUP(str);
        }

        if (!dict_add(d, item))
        {
            dictitem_free(item);
            return false;
        }

        return true;
    }

    /*
     * Add a list entry to dictionary "d".
     * Returns false when out of memory and when key already exists.
     */
    /*private*/ static boolean dict_add_list(dict_C d, Bytes key, list_C list)
    {
        dictitem_C item = dictitem_alloc(key);

        item.di_tv.tv_lock = 0;
        item.di_tv.tv_type = VAR_LIST;
        item.di_tv.tv_list = list;

        if (!dict_add(d, item))
        {
            dictitem_free(item);
            return false;
        }

        list.lv_refcount++;
        return true;
    }

    /*
     * Get the number of items in a Dictionary.
     */
    /*private*/ static long dict_len(dict_C dict)
    {
        if (dict == null)
            return 0L;

        return dict.dv_hashtab.ht_used;
    }

    /*
     * Find item "key[len]" in Dictionary "d".
     * If "len" is negative use strlen(key).
     * Returns null when not found.
     */
    /*private*/ static dictitem_C dict_find(dict_C d, Bytes key, int len)
    {
        final int AKEYLEN = 200;
        Bytes buf = new Bytes(AKEYLEN);

        Bytes akey;
        Bytes tofree = null;

        if (len < 0)
            akey = key;
        else if (AKEYLEN <= len)
            tofree = akey = STRNDUP(key, len);
        else
        {
            /* Avoid a calloc/free by using buf[]. */
            vim_strncpy(buf, key, len);
            akey = buf;
        }

        hashitem_C hi = hash_find(d.dv_hashtab, akey);
        if (hashitem_empty(hi))
            return null;

        return (dictitem_C)hi.hi_data;
    }

    /*
     * Get a string item from a dictionary.
     * When "save" is true allocate memory for it.
     * Returns null if the entry doesn't exist or out of memory.
     */
    /*private*/ static Bytes get_dict_string(dict_C d, Bytes key, boolean save)
    {
        dictitem_C di = dict_find(d, key, -1);
        if (di == null)
            return null;

        Bytes s = get_tv_string(di.di_tv);
        if (save && s != null)
            s = STRDUP(s);
        return s;
    }

    /*
     * Get a number item from a dictionary.
     * Returns 0 if the entry doesn't exist or out of memory.
     */
    /*private*/ static long get_dict_number(dict_C d, Bytes key)
    {
        dictitem_C di = dict_find(d, key, -1);
        if (di == null)
            return 0;

        return get_tv_number(di.di_tv);
    }

    /*
     * Return an allocated string with the string representation of a Dictionary.
     * May return null.
     */
    /*private*/ static Bytes dict2string(typval_C tv, int copyID)
    {
        dict_C d = tv.tv_dict;
        if (d == null)
            return null;

        barray_C ba = new barray_C(80);
        ba_append(ba, (byte)'{');

        boolean first = true;

        int todo = (int)d.dv_hashtab.ht_used;
        for (int i = 0; 0 < todo && !got_int; i++)
        {
            hashitem_C hi = d.dv_hashtab.ht_buckets[i];
            if (!hashitem_empty(hi))
            {
                --todo;

                if (first)
                    first = false;
                else
                    ba_concat(ba, u8(", "));

                Bytes s = string_quote(hi.hi_key, false);
                if (s != null)
                    ba_concat(ba, s);
                ba_concat(ba, u8(": "));

                s = tv2string(((dictitem_C)hi.hi_data).di_tv, copyID);
                if (s != null)
                    ba_concat(ba, s);
                if (s == null || did_echo_string_emsg)
                    break;

                line_breakcheck();
            }
        }

        if (0 < todo)
            return null;

        ba_append(ba, (byte)'}');
        ba_append(ba, NUL);

        return new Bytes(ba.ba_data);
    }

    /*
     * Allocate a variable for a Dictionary and fill it from "*arg".
     * Return TRUE or FALSE.  Returns MAYBE for {expr}.
     */
    /*private*/ static /*MAYBEAN*/int get_dict_tv(Bytes[] arg, typval_C rtv, boolean evaluate)
    {
        typval_C tvkey = new typval_C();
        typval_C tv = new typval_C();
        /*
         * First check if it's not a curly-braces thing: {expr}.
         * Must do this without evaluating, otherwise a function may be called twice.
         * Unfortunately this means we need to call eval1() twice for the first item.
         * But {} is an empty Dictionary.
         */
        Bytes[] start = { skipwhite(arg[0].plus(1)) };
        if (start[0].at(0) != (byte)'}')
        {
            if (eval1(start, tv, false) == false)          /* recursive! */
                return FALSE;
            if (start[0].at(0) == (byte)'}')
                return MAYBE;
        }

        dict_C dict = null;
        if (evaluate)
            dict = newDict();

        tvkey.tv_type = VAR_UNKNOWN;
        tv.tv_type = VAR_UNKNOWN;

        failret:
        {
            arg[0] = skipwhite(arg[0].plus(1));
            while (arg[0].at(0) != (byte)'}' && arg[0].at(0) != NUL)
            {
                Bytes key = null;

                if (eval1(arg, tvkey, evaluate) == false)       /* recursive! */
                    break failret;
                if (arg[0].at(0) != (byte)':')
                {
                    emsg2(u8("E720: Missing colon in Dictionary: %s"), arg[0]);
                    clear_tv(tvkey);
                    break failret;
                }
                if (evaluate)
                {
                    key = get_tv_string_chk(tvkey);
                    if (key == null || key.at(0) == NUL)
                    {
                        /* "key" is null when get_tv_string_chk() gave an errmsg */
                        if (key != null)
                            emsg(e_emptykey);
                        clear_tv(tvkey);
                        break failret;
                    }
                }

                arg[0] = skipwhite(arg[0].plus(1));
                if (eval1(arg, tv, evaluate) == false)      /* recursive! */
                {
                    if (evaluate)
                        clear_tv(tvkey);
                    break failret;
                }
                if (evaluate)
                {
                    dictitem_C di = dict_find(dict, key, -1);
                    if (di != null)
                    {
                        emsg2(u8("E721: Duplicate key in Dictionary: \"%s\""), key);
                        clear_tv(tvkey);
                        clear_tv(tv);
                        break failret;
                    }
                    di = dictitem_alloc(key);
                    clear_tv(tvkey);
                    COPY_typval(di.di_tv, tv);
                    di.di_tv.tv_lock = 0;
                    if (!dict_add(dict, di))
                        dictitem_free(di);
                }

                if (arg[0].at(0) == (byte)'}')
                    break;
                if (arg[0].at(0) != (byte)',')
                {
                    emsg2(u8("E722: Missing comma in Dictionary: %s"), arg[0]);
                    break failret;
                }
                arg[0] = skipwhite(arg[0].plus(1));
            }

            if (arg[0].at(0) != (byte)'}')
            {
                emsg2(u8("E723: Missing end of Dictionary '}': %s"), arg[0]);
                break failret;
            }

            arg[0] = skipwhite(arg[0].plus(1));
            if (evaluate)
            {
                rtv.tv_type = VAR_DICT;
                rtv.tv_dict = dict;
                dict.dv_refcount++;
            }

            return TRUE;
        }

        if (evaluate)
            dict_free(dict, true);
        return FALSE;
    }

    /*private*/ static int _2_recurse;

    /*
     * Return a string with the string representation of a variable.
     * Does not put quotes around strings, as ":echo" displays values.
     * When "copyID" is not null replace recursive lists and dicts with "...".
     * May return null.
     */
    /*private*/ static Bytes echo_string(typval_C tv, int copyID)
    {
        if (DICT_MAXNEST <= _2_recurse)
        {
            if (!did_echo_string_emsg)
            {
                /* Only give this message once for a recursive call to avoid flooding
                 * the user with errors.  And stop iterating over lists and dicts. */
                did_echo_string_emsg = true;
                emsg(u8("E724: variable nested too deep for displaying"));
            }
            return u8("{E724}");
        }
        _2_recurse++;

        Bytes r = null;

        switch (tv.tv_type)
        {
            case VAR_FUNC:
                r = tv.tv_string;
                break;

            case VAR_LIST:
                if (tv.tv_list == null)
                    r = null;
                else if (copyID != 0 && tv.tv_list.lv_copyID == copyID)
                    r = u8("[...]");
                else
                {
                    tv.tv_list.lv_copyID = copyID;
                    r = list2string(tv, copyID);
                }
                break;

            case VAR_DICT:
                if (tv.tv_dict == null)
                    r = null;
                else if (copyID != 0 && tv.tv_dict.dv_copyID == copyID)
                    r = u8("{...}");
                else
                {
                    tv.tv_dict.dv_copyID = copyID;
                    r = dict2string(tv, copyID);
                }
                break;

            case VAR_STRING:
            case VAR_NUMBER:
                r = get_tv_string(tv);
                break;

            default:
                emsg2(e_intern2, u8("echo_string()"));
                break;
        }

        if (--_2_recurse == 0)
            did_echo_string_emsg = false;
        return r;
    }

    /*
     * Return a string with the string representation of a variable.
     * Puts quotes around strings, so that they can be parsed back by eval().
     * May return null.
     */
    /*private*/ static Bytes tv2string(typval_C tv, int copyID)
    {
        switch (tv.tv_type)
        {
            case VAR_FUNC:
                return string_quote(tv.tv_string, true);

            case VAR_STRING:
                return string_quote(tv.tv_string, false);

            case VAR_NUMBER:
            case VAR_LIST:
            case VAR_DICT:
                break;

            default:
                emsg2(e_intern2, u8("tv2string()"));
        }

        return echo_string(tv, copyID);
    }

    /*
     * Return string "str" in ' quotes, doubling ' characters.
     * If "str" is null an empty string is assumed.
     * If "function" is true make it function('string').
     */
    /*private*/ static Bytes string_quote(Bytes str, boolean function)
    {
        int len = function ? 13 : 3;
        if (str != null)
        {
            len += strlen(str);
            for (Bytes p = str; p.at(0) != NUL; p = p.plus(us_ptr2len_cc(p)))
                if (p.at(0) == (byte)'\'')
                    len++;
        }

        Bytes s = new Bytes(len);
        Bytes r = s;
        if (r != null)
        {
            if (function)
            {
                STRCPY(r, u8("function('"));
                r = r.plus(10);
            }
            else
                (r = r.plus(1)).be(-1, (byte)'\'');
            if (str != null)
                for (Bytes p = str; p.at(0) != NUL; )
                {
                    if (p.at(0) == (byte)'\'')
                        (r = r.plus(1)).be(-1, (byte)'\'');
                    int l = us_ptr2len_cc(p);
                    BCOPY(r, p, l);
                    r = r.plus(l);
                    p = p.plus(l);
                }
            (r = r.plus(1)).be(-1, (byte)'\'');
            if (function)
                (r = r.plus(1)).be(-1, (byte)')');
            (r = r.plus(1)).be(-1, NUL);
        }
        return s;
    }

    /*private*/ static abstract class f_func_C
    {
        public abstract void fun(typval_C[] args, typval_C rvar);
    }

    /*private*/ static final class fst_C
    {
        Bytes       f_name;        /* function name */
        int         f_min_argc;    /* minimal number of arguments */
        int         f_max_argc;    /* maximal number of arguments */
        f_func_C    f_func;        /* implementation of function */

        /*private*/ fst_C(Bytes f_name, int f_min_argc, int f_max_argc, f_func_C f_func)
        {
            this.f_name = f_name;
            this.f_min_argc = f_min_argc;
            this.f_max_argc = f_max_argc;
            this.f_func = f_func;
        }
    }

    /*private*/ static int _1_intidx = -1;

    /*
     * Function given to expandGeneric() to obtain the list of internal
     * or user defined function names.
     */
    /*private*/ static final expfun_C get_function_name = new expfun_C()
    {
        public Bytes expand(expand_C xp, int idx)
        {
            if (idx == 0)
                _1_intidx = -1;
            if (_1_intidx < 0)
            {
                Bytes name = get_user_func_name.expand(xp, idx);
                if (name != null)
                    return name;
            }
            if (++_1_intidx < functions.length)
            {
                STRCPY(ioBuff, functions[_1_intidx].f_name);
                STRCAT(ioBuff, u8("("));
                if (functions[_1_intidx].f_max_argc == 0)
                    STRCAT(ioBuff, u8(")"));
                return ioBuff;
            }
            return null;
        }
    };

    /*private*/ static int _2_intidx = -1;

    /*
     * Function given to expandGeneric() to obtain the list of internal
     * or user defined variable or function names.
     */
    /*private*/ static final expfun_C get_expr_name = new expfun_C()
    {
        public Bytes expand(expand_C xp, int idx)
        {
            if (idx == 0)
                _2_intidx = -1;
            if (_2_intidx < 0)
            {
                Bytes name = get_function_name.expand(xp, idx);
                if (name != null)
                    return name;
            }
            return get_user_var_name.expand(xp, ++_2_intidx);
        }
    };

    /*
     * Find internal function in table above.
     * Return index, or -1 if not found
     */
    /*private*/ static int find_internal_func(Bytes name)
        /* name: name of the function */
    {
        int last = functions.length - 1;

        /*
         * Find the function name in the table.  Binary search.
         */
        for (int first = 0; first <= last; )
        {
            int x = first + ((last - first) >>> 1);
            int cmp = STRCMP(name, functions[x].f_name);
            if (cmp < 0)
                last = x - 1;
            else if (0 < cmp)
                first = x + 1;
            else
                return x;
        }

        return -1;
    }

    /*
     * Check if "name" is a variable of type VAR_FUNC.
     * If so, return the function name it contains, otherwise return "name".
     */
    /*private*/ static Bytes deref_func_name(Bytes name, int[] lenp, boolean no_autoload)
    {
        int cc = name.at(lenp[0]);
        name.be(lenp[0], NUL);
        dictitem_C v = find_var(name, null, no_autoload);
        name.be(lenp[0], cc);

        if (v != null && v.di_tv.tv_type == VAR_FUNC)
        {
            if (v.di_tv.tv_string == null)
            {
                lenp[0] = 0;
                return u8("");      /* just in case */
            }
            lenp[0] = strlen(v.di_tv.tv_string);
            return v.di_tv.tv_string;
        }

        return name;
    }

    /*
     * Allocate a variable for the result of a function.
     * Return true or false.
     */
    /*private*/ static boolean get_func_tv(Bytes name, int len, typval_C rtv, Bytes[] arg, long firstline, long lastline, boolean[] doesrange, boolean evaluate, dict_C selfdict)
        /* name: name of the function */
        /* len: length of "name" */
        /* arg: argument, pointing to the '(' */
        /* firstline: first line of range */
        /* lastline: last line of range */
        /* doesrange: return: function handled range */
        /* selfdict: Dictionary for "self" */
    {
        boolean ret = true;

        typval_C[] argvars = ARRAY_typval(MAX_FUNC_ARGS + 1);   /* vars for arguments */
        int argcount = 0;                                       /* number of arguments found */

        /*
         * Get the arguments.
         */
        Bytes[] argp = { arg[0] };
        while (argcount < MAX_FUNC_ARGS)
        {
            argp[0] = skipwhite(argp[0].plus(1));         /* skip the '(' or ',' */
            if (argp[0].at(0) == (byte)')' || argp[0].at(0) == (byte)',' || argp[0].at(0) == NUL)
                break;
            if (eval1(argp, argvars[argcount], evaluate) == false)
            {
                ret = false;
                break;
            }
            argcount++;
            if (argp[0].at(0) != (byte)',')
                break;
        }
        if (argp[0].at(0) == (byte)')')
            argp[0] = argp[0].plus(1);
        else
            ret = false;

        if (ret == true)
            ret = call_func(name, len, rtv, argcount, argvars,
                                        firstline, lastline, doesrange, evaluate, selfdict);
        else if (!aborting())
        {
            if (argcount == MAX_FUNC_ARGS)
                emsg_funcname(u8("E740: Too many arguments for function %s"), name);
            else
                emsg_funcname(u8("E116: Invalid arguments for function %s"), name);
        }

        while (0 <= --argcount)
            clear_tv(argvars[argcount]);

        arg[0] = skipwhite(argp[0]);

        return ret;
    }

    /*
     * Call a function with its resolved parameters.
     * Return false when the function can't be called, true otherwise.
     * Also returns true when an error was encountered while executing the function.
     */
    /*private*/ static boolean call_func(Bytes funcname, int len, typval_C rtv, int argcount, typval_C[] argvars, long firstline, long lastline, boolean[] doesrange, boolean evaluate, dict_C selfdict)
        /* funcname: name of the function */
        /* len: length of "name" */
        /* rtv: return value goes here */
        /* argcount: number of "argvars" */
        /* argvars: vars for arguments, must have "argcount" PLUS ONE elements! */
        /* firstline: first line of range */
        /* lastline: last line of range */
        /* doesrange: return: function handled range */
        /* selfdict: Dictionary for "self" */
    {
        boolean ret = false;

        final int
            ERROR_UNKNOWN = 0,
            ERROR_TOOMANY = 1,
            ERROR_TOOFEW  = 2,
            ERROR_SCRIPT  = 3,
            ERROR_DICT    = 4,
            ERROR_NONE    = 5,
            ERROR_OTHER   = 6;
        int error = ERROR_NONE;

        final int FLEN_FIXED = 40;
        Bytes fname_buf = new Bytes(FLEN_FIXED + 1);

        /* Make a copy of the name, if it comes from a funcref variable
         * it could be changed or deleted in the called function. */
        Bytes name = STRNDUP(funcname, len);

        Bytes fname;
        /*
         * In a script change <SID>name() and s:name() to K_SNR 123_name().
         * Change <SNR>123_name() to K_SNR 123_name().
         * Use fname_buf[] when it fits, otherwise allocate memory (slow).
         */
        int llen = eval_fname_script(name);
        if (0 < llen)
        {
            fname_buf.be(0, KB_SPECIAL);
            fname_buf.be(1, KS_EXTRA);
            fname_buf.be(2, KE_SNR);
            int i = 3;
            if (eval_fname_sid(name))       /* "<SID>" or "s:" */
            {
                if (current_SID <= 0)
                    error = ERROR_SCRIPT;
                else
                {
                    libC.sprintf(fname_buf.plus(3), u8("%ld_"), (long)current_SID);
                    i = strlen(fname_buf);
                }
            }
            if (i + strlen(name, llen) < FLEN_FIXED)
            {
                STRCPY(fname_buf.plus(i), name.plus(llen));
                fname = fname_buf;
            }
            else
            {
                fname = new Bytes(i + strlen(name, llen) + 1);

                BCOPY(fname, fname_buf, i);
                STRCPY(fname.plus(i), name.plus(llen));
            }
        }
        else
            fname = name;

        doesrange[0] = false;

        /* execute the function if no errors detected and executing */
        if (evaluate && error == ERROR_NONE)
        {
            Bytes rfname = fname;

            /* Ignore "g:" before a function name. */
            if (fname.at(0) == (byte)'g' && fname.at(1) == (byte)':')
                rfname = fname.plus(2);

            rtv.tv_type = VAR_NUMBER; /* default rtv is number zero */
            rtv.tv_number = 0;
            error = ERROR_UNKNOWN;

            if (!builtin_function(rfname, -1))
            {
                /*
                 * User defined function.
                 */
                ufunc_C fp = find_func(rfname);

                /* Trigger FuncUndefined event, may load the function. */
                if (fp == null
                        && apply_autocmds(EVENT_FUNCUNDEFINED, rfname, rfname, true, null)
                        && !aborting())
                {
                    /* executed an autocommand, search for the function again */
                    fp = find_func(rfname);
                }
                /* Try loading a package. */
                if (fp == null && script_autoload(rfname, true) && !aborting())
                {
                    /* loaded a package, search for the function again */
                    fp = find_func(rfname);
                }

                if (fp != null)
                {
                    if ((fp.uf_flags & FC_RANGE) != 0)
                        doesrange[0] = true;
                    if (argcount < fp.uf_args.ga_len)
                        error = ERROR_TOOFEW;
                    else if (!fp.uf_varargs && fp.uf_args.ga_len < argcount)
                        error = ERROR_TOOMANY;
                    else if ((fp.uf_flags & FC_DICT) != 0 && selfdict == null)
                        error = ERROR_DICT;
                    else
                    {
                        boolean did_save_redo = false;

                        /*
                         * Call the user function.
                         * Save and restore search patterns, script variables and redo buffer.
                         */
                        save_search_patterns();
                        saveRedobuff();
                        did_save_redo = true;

                        fp.uf_calls++;
                        call_user_func(fp, argcount, argvars, rtv, firstline, lastline,
                                                        (fp.uf_flags & FC_DICT) != 0 ? selfdict : null);
                        if (--fp.uf_calls <= 0 && asc_isdigit(fp.uf_name.at(0)) && fp.uf_refcount <= 0)
                            /* Function was unreferenced while being used, free it now. */
                            func_free(fp);

                        if (did_save_redo)
                            restoreRedobuff();
                        restore_search_patterns();

                        error = ERROR_NONE;
                    }
                }
            }
            else
            {
                /*
                 * Find the function name in the table, call its implementation.
                 */
                int i = find_internal_func(fname);
                if (0 <= i)
                {
                    if (argcount < functions[i].f_min_argc)
                        error = ERROR_TOOFEW;
                    else if (functions[i].f_max_argc < argcount)
                        error = ERROR_TOOMANY;
                    else
                    {
                        argvars[argcount].tv_type = VAR_UNKNOWN;
                        functions[i].f_func.fun(argvars, rtv);
                        error = ERROR_NONE;
                    }
                }
            }
            /*
             * The function call (or "FuncUndefined" autocommand sequence) might have been aborted
             * by an error, an interrupt, or an explicitly thrown exception that has not been caught
             * so far.  This situation can be tested for by calling aborting().  For an error in an
             * internal function or for the "E132" error in call_user_func(), however, the throw point
             * at which the "force_abort" flag (temporarily reset by emsg()) is normally updated has
             * not been reached yet.  We need to update that flag first to make aborting() reliable.
             */
            update_force_abort();
        }
        if (error == ERROR_NONE)
            ret = true;

        /*
         * Report an error unless the argument evaluation or function call has been
         * cancelled due to an aborting error, an interrupt, or an exception.
         */
        if (!aborting())
        {
            switch (error)
            {
                case ERROR_UNKNOWN:
                    emsg_funcname(u8("E117: Unknown function: %s"), name);
                    break;
                case ERROR_TOOMANY:
                    emsg_funcname(e_toomanyarg, name);
                    break;
                case ERROR_TOOFEW:
                    emsg_funcname(u8("E119: Not enough arguments for function: %s"), name);
                    break;
                case ERROR_SCRIPT:
                    emsg_funcname(u8("E120: Using <SID> not in a script context: %s"), name);
                    break;
                case ERROR_DICT:
                    emsg_funcname(u8("E725: Calling dict function without Dictionary: %s"), name);
                    break;
            }
        }

        return ret;
    }

    /*
     * Give an error message with a function name.  Handle <SNR> things.
     */
    /*private*/ static void emsg_funcname(Bytes ermsg, Bytes name)
    {
        Bytes p = name;
        if (p.at(0) == KB_SPECIAL)
            p = concat_str(u8("<SNR>"), p.plus(3));
        emsg2(ermsg, p);
    }

    /*
     * Return true for a non-zero Number and a non-empty String.
     */
    /*private*/ static boolean non_zero_arg(typval_C argvar)
    {
        return ((argvar.tv_type == VAR_NUMBER && argvar.tv_number != 0)
             || (argvar.tv_type == VAR_STRING && argvar.tv_string != null && argvar.tv_string.at(0) != NUL));
    }

    /********************************************
     * Implementation of the built-in functions *
     ********************************************/

    /*
     * "abs(expr)" function
     */
    /*private*/ static final f_func_C f_abs = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            boolean[] error = { false };

            long n = get_tv_number_chk(argvars[0], error);
            if (error[0])
                rtv.tv_number = -1;
            else if (0 < n)
                rtv.tv_number = n;
            else
                rtv.tv_number = -n;
        }
    };

    /*
     * "add(list, item)" function
     */
    /*private*/ static final f_func_C f_add = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = 1;                /* Default: Failed */
            if (argvars[0].tv_type == VAR_LIST)
            {
                list_C l = argvars[0].tv_list;
                if (l != null
                        && !tv_check_lock(l.lv_lock, u8("add() argument"))
                        && list_append_tv(l, argvars[1]) == true)
                    copy_tv(argvars[0], rtv);
            }
            else
                emsg(e_listreq);
        }
    };

    /*
     * "and(expr, expr)" function
     */
    /*private*/ static final f_func_C f_and = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = get_tv_number_chk(argvars[0], null) & get_tv_number_chk(argvars[1], null);
        }
    };

    /*
     * "append(lnum, string/list)" function
     */
    /*private*/ static final f_func_C f_append = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            list_C l = null;
            listitem_C li = null;
            long added = 0;

            /* When coming here from Insert mode, sync undo, so that this
             * can be undone separately from what was previously inserted. */
            if (u_sync_once == 2)
            {
                u_sync_once = 1;                            /* notify that u_sync() was called */
                u_sync(true);
            }

            long lnum = get_tv_lnum(argvars[0]);
            if (0 <= lnum && lnum <= curbuf.b_ml.ml_line_count && u_save(lnum, lnum + 1))
            {
                if (argvars[1].tv_type == VAR_LIST)
                {
                    l = argvars[1].tv_list;
                    if (l == null)
                        return;
                    li = l.lv_first;
                }
                for ( ; ; )
                {
                    typval_C tv;
                    if (l == null)
                        tv = argvars[1];                    /* append a string */
                    else if (li == null)
                        break;                              /* end of list */
                    else
                        tv = li.li_tv;                      /* append item from list */
                    Bytes line = get_tv_string_chk(tv);
                    if (line == null)                       /* type error */
                    {
                        rtv.tv_number = 1;                /* Failed */
                        break;
                    }
                    ml_append(lnum + added, line, 0, false);
                    added++;
                    if (l == null)
                        break;
                    li = li.li_next;
                }

                appended_lines_mark(lnum, added);
                if (curwin.w_cursor.lnum > lnum)
                    curwin.w_cursor.lnum += added;
            }
            else
                rtv.tv_number = 1;                        /* Failed */
        }
    };

    /*
     * "argc()" function
     */
    /*private*/ static final f_func_C f_argc = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rtv.tv_number = curwin.w_alist.al_ga.ga_len;
        }
    };

    /*
     * "argidx()" function
     */
    /*private*/ static final f_func_C f_argidx = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rtv.tv_number = curwin.w_arg_idx;
        }
    };

    /*
     * "arglistid()" function
     */
    /*private*/ static final f_func_C f_arglistid = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = -1;
            if (argvars[0].tv_type != VAR_UNKNOWN)
            {
                tabpage_C tp = null;
                if (argvars[1].tv_type != VAR_UNKNOWN)
                {
                    int n = (int)get_tv_number(argvars[1]);
                    if (0 <= n)
                        tp = find_tabpage(n);
                }
                else
                    tp = curtab;

                if (tp != null)
                {
                    window_C wp = find_win_by_nr(argvars[0], tp);
                    if (wp != null)
                        rtv.tv_number = wp.w_alist.id;
                }
            }
            else
                rtv.tv_number = curwin.w_alist.id;
        }
    };

    /*
     * "argv(nr)" function
     */
    /*private*/ static final f_func_C f_argv = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            aentry_C[] waep = curwin.w_alist.al_ga.ga_data;

            if (argvars[0].tv_type != VAR_UNKNOWN)
            {
                int i = (int)get_tv_number_chk(argvars[0], null);
                if (0 <= i && i < curwin.w_alist.al_ga.ga_len)
                    rtv.tv_string = STRDUP(alist_name(waep[i]));
                else
                    rtv.tv_string = null;
                rtv.tv_type = VAR_STRING;
            }
            else
            {
                rettv_list_alloc(rtv);
                for (int i = 0; i < curwin.w_alist.al_ga.ga_len; i++)
                    list_append_string(rtv.tv_list, alist_name(waep[i]), -1);
            }
        }
    };

    /*
     * Find a buffer by number or exact name.
     */
    /*private*/ static buffer_C find_buffer(typval_C avar)
    {
        buffer_C buf = null;

        if (avar.tv_type == VAR_NUMBER)
            buf = buflist_findnr((int)avar.tv_number);
        else if (avar.tv_type == VAR_STRING && avar.tv_string != null)
        {
            buf = buflist_findname_exp(avar.tv_string);
            if (buf == null)
            {
                /* No full path name match.  Try a match with a URL or a "nofile" buffer,
                 * these don't use the full path. */
                for (buf = firstbuf; buf != null; buf = buf.b_next)
                    if (buf.b_fname != null && path_with_url(buf.b_fname) != 0 && STRCMP(buf.b_fname, avar.tv_string) == 0)
                        break;
            }
        }

        return buf;
    }

    /*
     * "bufexists(expr)" function
     */
    /*private*/ static final f_func_C f_bufexists = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = (find_buffer(argvars[0]) != null) ? 1 : 0;
        }
    };

    /*
     * "buflisted(expr)" function
     */
    /*private*/ static final f_func_C f_buflisted = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            buffer_C buf = find_buffer(argvars[0]);
            rtv.tv_number = (buf != null && buf.b_p_bl[0]) ? 1 : 0;
        }
    };

    /*
     * "bufloaded(expr)" function
     */
    /*private*/ static final f_func_C f_bufloaded = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            buffer_C buf = find_buffer(argvars[0]);
            rtv.tv_number = (buf != null && buf.b_ml.ml_mfp != null) ? 1 : 0;
        }
    };

    /*
     * Get buffer by number or pattern.
     */
    /*private*/ static buffer_C get_buf_tv(typval_C tv, boolean curtab_only)
    {
        if (tv.tv_type == VAR_NUMBER)
            return buflist_findnr((int)tv.tv_number);
        if (tv.tv_type != VAR_STRING)
            return null;

        Bytes name = tv.tv_string;
        if (name == null || name.at(0) == NUL)
            return curbuf;
        if (name.at(0) == (byte)'$' && name.at(1) == NUL)
            return lastbuf;

        /* Ignore 'magic' and 'cpoptions' here to make scripts portable. */
        boolean save_magic = p_magic[0];
        p_magic[0] = true;
        Bytes save_cpo = p_cpo[0];
        p_cpo[0] = u8("");

        buffer_C buf = buflist_findnr(buflist_findpat(name, name.plus(strlen(name)), true, curtab_only));

        p_magic[0] = save_magic;
        p_cpo[0] = save_cpo;

        /* If not found, try expanding the name, like done for bufexists(). */
        if (buf == null)
            buf = find_buffer(tv);

        return buf;
    }

    /*
     * "bufname(expr)" function
     */
    /*private*/ static final f_func_C f_bufname = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            get_tv_number(argvars[0]);      /* issue errmsg if type error */
            emsg_off++;
            buffer_C buf = get_buf_tv(argvars[0], false);
            rtv.tv_type = VAR_STRING;
            if (buf != null && buf.b_fname != null)
                rtv.tv_string = STRDUP(buf.b_fname);
            else
                rtv.tv_string = null;
            --emsg_off;
        }
    };

    /*
     * "bufnr(expr)" function
     */
    /*private*/ static final f_func_C f_bufnr = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            get_tv_number(argvars[0]);      /* issue errmsg if type error */
            emsg_off++;
            buffer_C buf = get_buf_tv(argvars[0], false);
            --emsg_off;

            boolean[] error = { false };

            Bytes name;
            /* If the buffer isn't found and the second argument is not zero create a new buffer. */
            if (buf == null
                    && argvars[1].tv_type != VAR_UNKNOWN
                    && get_tv_number_chk(argvars[1], error) != 0
                    && !error[0]
                    && (name = get_tv_string_chk(argvars[0])) != null)
                buf = buflist_new(name, null, 1, 0);

            if (buf != null)
                rtv.tv_number = buf.b_fnum;
            else
                rtv.tv_number = -1;
        }
    };

    /*
     * "bufwinnr(nr)" function
     */
    /*private*/ static final f_func_C f_bufwinnr = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            get_tv_number(argvars[0]);      /* issue errmsg if type error */
            emsg_off++;
            buffer_C buf = get_buf_tv(argvars[0], true);
            int winnr = 0;
            window_C wp;
            for (wp = firstwin; wp != null; wp = wp.w_next)
            {
                winnr++;
                if (wp.w_buffer == buf)
                    break;
            }
            rtv.tv_number = (wp != null) ? winnr : -1;
            --emsg_off;
        }
    };

    /*
     * "byte2line(byte)" function
     */
    /*private*/ static final f_func_C f_byte2line = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            long[] boff = { get_tv_number(argvars[0]) - 1 };  /* boff gets -1 on type error */
            if (boff[0] < 0)
                rtv.tv_number = -1;
            else
                rtv.tv_number = ml_find_line_or_offset(curbuf, 0, boff);
        }
    };

    /*private*/ static void byteidx(typval_C[] argvars, typval_C rtv, boolean comp)
    {
        rtv.tv_number = -1;

        Bytes str = get_tv_string_chk(argvars[0]);
        long idx = get_tv_number_chk(argvars[1], null);
        if (str == null || idx < 0)
            return;

        Bytes t = str;
        for ( ; 0 < idx; idx--)
        {
            if (t.at(0) == NUL)          /* EOL reached */
                return;
            if (comp)
                t = t.plus(us_ptr2len(t));
            else
                t = t.plus(us_ptr2len_cc(t));
        }

        rtv.tv_number = BDIFF(t, str);
    }

    /*
     * "byteidx()" function
     */
    /*private*/ static final f_func_C f_byteidx = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            byteidx(argvars, rtv, false);
        }
    };

    /*
     * "byteidxcomp()" function
     */
    /*private*/ static final f_func_C f_byteidxcomp = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            byteidx(argvars, rtv, true);
        }
    };

    /*private*/ static boolean func_call(Bytes name, typval_C args, dict_C selfdict, typval_C rtv)
    {
        typval_C[] argv = ARRAY_typval(MAX_FUNC_ARGS + 1);
        int argc = 0;

        listitem_C item;
        for (item = args.tv_list.lv_first; item != null; item = item.li_next)
        {
            if (argc == MAX_FUNC_ARGS)
            {
                emsg(u8("E699: Too many arguments"));
                break;
            }
            /* Make a copy of each argument.  This is needed to be able to set
             * tv_lock to VAR_FIXED in the copy without changing the original list.
             */
            copy_tv(item.li_tv, argv[argc++]);
        }

        boolean r = false;
        boolean[] doesrange = new boolean[1];
        if (item == null)
            r = call_func(name, strlen(name), rtv, argc, argv,
                            curwin.w_cursor.lnum, curwin.w_cursor.lnum, doesrange, true, selfdict);

        /* Free the arguments. */
        while (0 < argc)
            clear_tv(argv[--argc]);

        return r;
    }

    /*
     * "call(func, arglist)" function
     */
    /*private*/ static final f_func_C f_call = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            if (argvars[1].tv_type != VAR_LIST)
            {
                emsg(e_listreq);
                return;
            }
            if (argvars[1].tv_list == null)
                return;

            Bytes func;
            if (argvars[0].tv_type == VAR_FUNC)
                func = argvars[0].tv_string;
            else
                func = get_tv_string(argvars[0]);
            if (func.at(0) == NUL)
                return;         /* type error or empty name */

            dict_C selfdict = null;

            if (argvars[2].tv_type != VAR_UNKNOWN)
            {
                if (argvars[2].tv_type != VAR_DICT)
                {
                    emsg(e_dictreq);
                    return;
                }
                selfdict = argvars[2].tv_dict;
            }

            func_call(func, argvars[1], selfdict, rtv);
        }
    };

    /*
     * "changenr()" function
     */
    /*private*/ static final f_func_C f_changenr = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rtv.tv_number = curbuf.b_u_seq_cur;
        }
    };

    /*
     * "char2nr(string)" function
     */
    /*private*/ static final f_func_C f_char2nr = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = us_ptr2char(get_tv_string(argvars[0]));
        }
    };

    /*
     * "cindent(lnum)" function
     */
    /*private*/ static final f_func_C f_cindent = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            pos_C pos = new pos_C();
            COPY_pos(pos, curwin.w_cursor);
            long lnum = get_tv_lnum(argvars[0]);

            if (1 <= lnum && lnum <= curbuf.b_ml.ml_line_count)
            {
                curwin.w_cursor.lnum = lnum;
                rtv.tv_number = get_c_indent.getindent();
                COPY_pos(curwin.w_cursor, pos);
            }
            else
                rtv.tv_number = -1;
        }
    };

    /*
     * "clearmatches()" function
     */
    /*private*/ static final f_func_C f_clearmatches = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C _rettv)
        {
            clear_matches(curwin);
        }
    };

    /*
     * "col(string)" function
     */
    /*private*/ static final f_func_C f_col = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int col = 0;

            int[] fnum = { curbuf.b_fnum };
            pos_C fp = var2fpos(argvars[0], false, fnum);
            if (fp != null && fnum[0] == curbuf.b_fnum)
            {
                if (fp.col == MAXCOL)
                {
                    /* '> can be MAXCOL, get the length of the line then */
                    if (fp.lnum <= curbuf.b_ml.ml_line_count)
                        col = strlen(ml_get(fp.lnum)) + 1;
                    else
                        col = MAXCOL;
                }
                else
                {
                    col = fp.col + 1;
                    /* col(".") when the cursor is on the NUL at the end of the line,
                     * because of "coladd" can be seen as an extra column. */
                    if (virtual_active() && fp == curwin.w_cursor)
                    {
                        Bytes p = ml_get_cursor();

                        if (chartabsize(p, curwin.w_virtcol - curwin.w_cursor.coladd) <= curwin.w_cursor.coladd)
                        {
                            int l;

                            if (p.at(0) != NUL && p.at((l = us_ptr2len_cc(p))) == NUL)
                                col += l;
                        }
                    }
                }
            }

            rtv.tv_number = col;
        }
    };

    /*
     * "confirm(message, buttons[, default [, type]])" function
     */
    /*private*/ static final f_func_C f_confirm = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            boolean[] error = { false };

            Bytes message = get_tv_string_chk(argvars[0]);
            if (message == null)
                error[0] = true;

            int def = 1;

            Bytes buttons = null;
            if (argvars[1].tv_type != VAR_UNKNOWN)
            {
                buttons = get_tv_string_chk(argvars[1]);
                if (buttons == null)
                    error[0] = true;

                if (argvars[2].tv_type != VAR_UNKNOWN)
                {
                    def = (int)get_tv_number_chk(argvars[2], error);
                    if (argvars[3].tv_type != VAR_UNKNOWN)
                    {
                        Bytes typestr = get_tv_string_chk(argvars[3]);
                        if (typestr == null)
                            error[0] = true;
                    }
                }
            }

            if (buttons == null || buttons.at(0) == NUL)
                buttons = u8("&Ok");

            if (!error[0])
                rtv.tv_number = do_dialog(message, buttons, def, false);
        }
    };

    /*
     * "copy()" function
     */
    /*private*/ static final f_func_C f_copy = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            item_copy(argvars[0], rtv, false, 0);
        }
    };

    /*
     * "count()" function
     */
    /*private*/ static final f_func_C f_count = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            long n = 0;
            boolean ic = false;

            if (argvars[0].tv_type == VAR_LIST)
            {
                list_C l = argvars[0].tv_list;
                if (l != null)
                {
                    listitem_C li = l.lv_first;
                    if (argvars[2].tv_type != VAR_UNKNOWN)
                    {
                        boolean[] error = { false };

                        ic = (get_tv_number_chk(argvars[2], error) != 0);
                        if (argvars[3].tv_type != VAR_UNKNOWN)
                        {
                            long idx = get_tv_number_chk(argvars[3], error);
                            if (!error[0])
                            {
                                li = list_find(l, idx);
                                if (li == null)
                                    emsgn(e_listidx, idx);
                            }
                        }
                        if (error[0])
                            li = null;
                    }

                    for ( ; li != null; li = li.li_next)
                        if (tv_equal(li.li_tv, argvars[1], ic, false))
                            n++;
                }
            }
            else if (argvars[0].tv_type == VAR_DICT)
            {
                dict_C d = argvars[0].tv_dict;
                if (d != null)
                {
                    boolean[] error = { false };

                    if (argvars[2].tv_type != VAR_UNKNOWN)
                    {
                        ic = (get_tv_number_chk(argvars[2], error) != 0);
                        if (argvars[3].tv_type != VAR_UNKNOWN)
                            emsg(e_invarg);
                    }

                    if (!error[0])
                        for (int i = 0, todo = (int)d.dv_hashtab.ht_used; 0 < todo; i++)
                        {
                            hashitem_C hi = d.dv_hashtab.ht_buckets[i];
                            if (!hashitem_empty(hi))
                            {
                                if (tv_equal(((dictitem_C)hi.hi_data).di_tv, argvars[1], ic, false))
                                    n++;
                                --todo;
                            }
                        }
                }
            }
            else
                emsg2(e_listdictarg, u8("count()"));

            rtv.tv_number = n;
        }
    };

    /*
     * "cursor(lnum, col)" function
     *
     * Moves the cursor to the specified line and column.
     * Returns 0 when the position could be set, -1 otherwise.
     */
    /*private*/ static final f_func_C f_cursor = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = -1;

            long line;
            int col, coladd = 0;
            if (argvars[1].tv_type == VAR_UNKNOWN)
            {
                pos_C pos = new pos_C();
                int[] curswant = { -1 };
                if (list2fpos(argvars[0], pos, null, curswant) == false)
                    return;

                line = pos.lnum;
                col = pos.col;
                coladd = pos.coladd;
                if (0 <= curswant[0])
                    curwin.w_curswant = curswant[0] - 1;
            }
            else
            {
                line = get_tv_lnum(argvars[0]);
                col = (int)get_tv_number_chk(argvars[1], null);
                if (argvars[2].tv_type != VAR_UNKNOWN)
                    coladd = (int)get_tv_number_chk(argvars[2], null);
            }
            if (line < 0 || col < 0 || coladd < 0)
                return;         /* type error; errmsg already given */

            if (0 < line)
                curwin.w_cursor.lnum = line;
            if (0 < col)
                curwin.w_cursor.col = col - 1;
            curwin.w_cursor.coladd = coladd;

            /* Make sure the cursor is in a valid position. */
            check_cursor();
            /* Correct cursor for multi-byte character. */
            mb_adjust_pos(curbuf, curwin.w_cursor);

            curwin.w_set_curswant = true;
            rtv.tv_number = 0;
        }
    };

    /*
     * "deepcopy()" function
     */
    /*private*/ static final f_func_C f_deepcopy = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int noref = 0;
            if (argvars[1].tv_type != VAR_UNKNOWN)
                noref = (int)get_tv_number_chk(argvars[1], null);

            if (noref < 0 || 1 < noref)
                emsg(e_invarg);
            else
            {
                current_copyID += COPYID_INC;
                item_copy(argvars[0], rtv, true, (noref == 0) ? current_copyID : 0);
            }
        }
    };

    /*
     * "did_filetype()" function
     */
    /*private*/ static final f_func_C f_did_filetype = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rtv.tv_number = did_filetype ? 1 : 0;
        }
    };

    /*
     * "empty({expr})" function
     */
    /*private*/ static final f_func_C f_empty = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            boolean b;

            switch (argvars[0].tv_type)
            {
                case VAR_STRING:
                case VAR_FUNC:
                    b = (argvars[0].tv_string == null || argvars[0].tv_string.at(0) == NUL);
                    break;

                case VAR_NUMBER:
                    b = (argvars[0].tv_number == 0);
                    break;

                case VAR_LIST:
                    b = (argvars[0].tv_list == null || argvars[0].tv_list.lv_first == null);
                    break;

                case VAR_DICT:
                    b = (argvars[0].tv_dict == null || argvars[0].tv_dict.dv_hashtab.ht_used == 0);
                    break;

                default:
                    emsg2(e_intern2, u8("f_empty()"));
                    b = false;
            }

            rtv.tv_number = (b) ? 1 : 0;
        }
    };

    /*
     * "escape({string}, {chars})" function
     */
    /*private*/ static final f_func_C f_escape = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_string = vim_strsave_escaped(get_tv_string(argvars[0]), get_tv_string(argvars[1]));
            rtv.tv_type = VAR_STRING;
        }
    };

    /*
     * "eval()" function
     */
    /*private*/ static final f_func_C f_eval = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes[] s = { get_tv_string_chk(argvars[0]) };
            if (s[0] != null)
                s[0] = skipwhite(s[0]);

            Bytes p = s[0];
            if (s[0] == null || eval1(s, rtv, true) == false)
            {
                if (p != null && !aborting())
                    emsg2(e_invexpr2, p);
                need_clr_eos = false;
                rtv.tv_type = VAR_NUMBER;
                rtv.tv_number = 0;
            }
            else if (s[0].at(0) != NUL)
                emsg(e_trailing);
        }
    };

    /*
     * "eventhandler()" function
     */
    /*private*/ static final f_func_C f_eventhandler = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rtv.tv_number = vgetc_busy;
        }
    };

    /*
     * "exists()" function
     */
    /*private*/ static final f_func_C f_exists = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            boolean exists = false;

            Bytes[] p = { get_tv_string(argvars[0]) };
            if (p[0].at(0) == (byte)'&' || p[0].at(0) == (byte)'+')                 /* option */
            {
                exists = get_option_tv(p, null, true);
                if (skipwhite(p[0]).at(0) != NUL)
                    exists = false;                     /* trailing garbage */
            }
            else if (p[0].at(0) == (byte)'*')                         /* internal or user defined function */
            {
                exists = function_exists(p[0].plus(1));
            }
            else if (p[0].at(0) == (byte)':')
            {
                exists = (cmd_exists(p[0].plus(1)) != 0);
            }
            else if (p[0].at(0) == (byte)'#')
            {
                if (p[0].at(1) == (byte)'#')
                    exists = autocmd_supported(p[0].plus(2));
                else
                    exists = au_exists(p[0].plus(1));
            }
            else                                        /* internal variable */
            {
                Bytes[] tofree = new Bytes[1];

                /* get_name_len() takes care of expanding curly braces */
                Bytes name = p[0];
                int len = get_name_len(p, tofree, true, false);
                if (0 < len)
                {
                    if (tofree[0] != null)
                        name = tofree[0];
                    typval_C tv = new typval_C();
                    exists = get_var_tv(name, len, tv, false, true);
                    if (exists)
                    {
                        /* handle d.key, l[idx], f(expr) */
                        exists = handle_subscript(p, tv, true, false);
                        if (exists)
                            clear_tv(tv);
                    }
                }
                if (p[0].at(0) != NUL)
                    exists = false;
            }

            rtv.tv_number = (exists) ? 1 : 0;
        }
    };

    /*
     * Go over all entries in "d2" and add them to "d1".
     * When "action" is "error" then a duplicate key is an error.
     * When "action" is "force" then a duplicate key is overwritten.
     * Otherwise duplicate keys are ignored ("action" is "keep").
     */
    /*private*/ static void dict_extend(dict_C d1, dict_C d2, Bytes action)
    {
        for (int i = 0, todo = (int)d2.dv_hashtab.ht_used; 0 < todo; i++)
        {
            hashitem_C hi2 = d2.dv_hashtab.ht_buckets[i];
            if (!hashitem_empty(hi2))
            {
                --todo;
                dictitem_C di1 = dict_find(d1, hi2.hi_key, -1);
                if (d1.dv_scope != 0)
                {
                    /* Disallow replacing a builtin function in l: and g:.
                     * Check the key to be valid when adding to any scope. */
                    if (d1.dv_scope == VAR_DEF_SCOPE
                            && ((dictitem_C)hi2.hi_data).di_tv.tv_type == VAR_FUNC
                            && var_check_func_name(hi2.hi_key, di1 == null))
                        break;
                    if (!valid_varname(hi2.hi_key))
                        break;
                }
                if (di1 == null)
                {
                    di1 = dictitem_copy((dictitem_C)hi2.hi_data);
                    if (!dict_add(d1, di1))
                        dictitem_free(di1);
                }
                else if (action.at(0) == (byte)'e')
                {
                    emsg2(u8("E737: Key already exists: %s"), hi2.hi_key);
                    break;
                }
                else if (action.at(0) == (byte)'f' && (dictitem_C)hi2.hi_data != di1)
                {
                    clear_tv(di1.di_tv);
                    copy_tv(((dictitem_C)hi2.hi_data).di_tv, di1.di_tv);
                }
            }
        }
    }

    /*private*/ static final Bytes[] __action = { u8("keep"), u8("force"), u8("error") };

    /*
     * "extend(list, list [, idx])" function
     * "extend(dict, dict [, action])" function
     */
    /*private*/ static final f_func_C f_extend = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes arg_errmsg = u8("extend() argument");

            if (argvars[0].tv_type == VAR_LIST && argvars[1].tv_type == VAR_LIST)
            {
                list_C l1 = argvars[0].tv_list;
                list_C l2 = argvars[1].tv_list;

                if (l1 != null && !tv_check_lock(l1.lv_lock, arg_errmsg) && l2 != null)
                {
                    listitem_C item = null;

                    if (argvars[2].tv_type != VAR_UNKNOWN)
                    {
                        boolean[] error = { false };
                        long before = get_tv_number_chk(argvars[2], error);
                        if (error[0])
                            return;             /* type error; errmsg already given */

                        if (before != l1.lv_len)
                        {
                            item = list_find(l1, before);
                            if (item == null)
                            {
                                emsgn(e_listidx, before);
                                return;
                            }
                        }
                    }

                    list_extend(l1, l2, item);

                    copy_tv(argvars[0], rtv);
                }
            }
            else if (argvars[0].tv_type == VAR_DICT && argvars[1].tv_type == VAR_DICT)
            {
                dict_C d1 = argvars[0].tv_dict;
                dict_C d2 = argvars[1].tv_dict;

                if (d1 != null && !tv_check_lock(d1.dv_lock, arg_errmsg) && d2 != null)
                {
                    Bytes action;

                    /* Check the third argument. */
                    if (argvars[2].tv_type != VAR_UNKNOWN)
                    {
                        action = get_tv_string_chk(argvars[2]);
                        if (action == null)
                            return;             /* type error; errmsg already given */

                        int i;
                        for (i = 0; i < 3; i++)
                            if (STRCMP(action, __action[i]) == 0)
                                break;
                        if (i == 3)
                        {
                            emsg2(e_invarg2, action);
                            return;
                        }
                    }
                    else
                        action = u8("force");

                    dict_extend(d1, d2, action);

                    copy_tv(argvars[0], rtv);
                }
            }
            else
                emsg2(e_listdictarg, u8("extend()"));
        }
    };

    /*
     * "feedkeys()" function
     */
    /*private*/ static final f_func_C f_feedkeys = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C _rettv)
        {
            /* This is not allowed in the sandbox.
             * If the commands would still be executed in the sandbox it would be OK,
             * but it probably happens later, when "sandbox" is no longer set. */
            if (check_secure())
                return;

            Bytes keys = get_tv_string(argvars[0]);
            if (keys.at(0) != NUL)
            {
                boolean remap = true;
                boolean insert = false;
                boolean typed = false;

                if (argvars[1].tv_type != VAR_UNKNOWN)
                {
                    for (Bytes s = get_tv_string(argvars[1]); s.at(0) != NUL; s = s.plus(1))
                    {
                        switch (s.at(0))
                        {
                            case 'n': remap = false; break;
                            case 'm': remap = true; break;
                            case 't': typed = true; break;
                            case 'i': insert = true; break;
                        }
                    }
                }

                /* Need to escape KB_SPECIAL and CSI before putting the string in the typeahead buffer. */
                Bytes keys_esc = vim_strsave_escape_csi(keys);

                ins_typebuf(keys_esc, remap ? REMAP_YES : REMAP_NONE, insert ? 0 : typebuf.tb_len, !typed, false);

                if (vgetc_busy != 0)
                    typebuf_was_filled = true;
            }
        }
    };

    /*
     * Implementation of map() and filter().
     */
    /*private*/ static void filter_map(typval_C[] argvars, typval_C rtv, boolean map)
    {
        Bytes errmsg = map ? u8("map()") : u8("filter()");
        Bytes arg_errmsg = map ? u8("map() argument") : u8("filter() argument");

        list_C l = null;
        dict_C d = null;
        if (argvars[0].tv_type == VAR_LIST)
        {
            if ((l = argvars[0].tv_list) == null || tv_check_lock(l.lv_lock, arg_errmsg))
                return;
        }
        else if (argvars[0].tv_type == VAR_DICT)
        {
            if ((d = argvars[0].tv_dict) == null || tv_check_lock(d.dv_lock, arg_errmsg))
                return;
        }
        else
        {
            emsg2(e_listdictarg, errmsg);
            return;
        }

        Bytes expr = get_tv_string_chk(argvars[1]);

        /* On type errors, the preceding call has already displayed an error message.
         * Avoid a misleading error message for an empty string that was not passed as argument.
         */
        if (expr != null)
        {
            typval_C save_val = new typval_C();
            prepare_vimvar(VV_VAL, save_val);
            expr = skipwhite(expr);

            /* We reset "did_emsg" to be able to detect whether
             * an error occurred during evaluation of the expression. */
            boolean save_did_emsg = did_emsg;
            did_emsg = false;

            typval_C save_key = new typval_C();
            prepare_vimvar(VV_KEY, save_key);
            if (argvars[0].tv_type == VAR_DICT)
            {
                vimvars[VV_KEY].vv_di.di_tv.tv_type = VAR_STRING;

                hashtab_C ht = d.dv_hashtab;
                hash_lock(ht);
                for (int i = 0, todo = (int)ht.ht_used; 0 < todo; i++)
                {
                    hashitem_C hi = ht.ht_buckets[i];
                    if (!hashitem_empty(hi))
                    {
                        --todo;
                        dictitem_C di = (dictitem_C)hi.hi_data;
                        if (tv_check_lock(di.di_tv.tv_lock, arg_errmsg))
                            break;
                        vimvars[VV_KEY].vv_di.di_tv.tv_string = STRDUP(di.di_key);
                        boolean[] rem = new boolean[1];
                        boolean r = filter_map_one(di.di_tv, expr, map, rem);
                        clear_tv(vimvars[VV_KEY].vv_di.di_tv);
                        if (r == false || did_emsg)
                            break;
                        if (!map && rem[0])
                            dictitem_remove(d, di);
                    }
                }
                hash_unlock(ht);
            }
            else
            {
                vimvars[VV_KEY].vv_di.di_tv.tv_type = VAR_NUMBER;

                int idx = 0;
                listitem_C nli;
                for (listitem_C li = l.lv_first; li != null; li = nli)
                {
                    if (tv_check_lock(li.li_tv.tv_lock, arg_errmsg))
                        break;
                    nli = li.li_next;
                    set_vim_var_nr(VV_KEY, idx);
                    boolean[] rem = new boolean[1];
                    if (filter_map_one(li.li_tv, expr, map, rem) == false || did_emsg)
                        break;
                    if (!map && rem[0])
                        listitem_remove(l, li);
                    idx++;
                }
            }

            restore_vimvar(VV_KEY, save_key);
            restore_vimvar(VV_VAL, save_val);

            did_emsg |= save_did_emsg;
        }

        copy_tv(argvars[0], rtv);
    }

    /*private*/ static boolean filter_map_one(typval_C tv, Bytes expr, boolean map, boolean[] remp)
    {
        boolean retval = false;

        copy_tv(tv, vimvars[VV_VAL].vv_di.di_tv);

        theend:
        {
            Bytes[] s = { expr };
            typval_C rtv = new typval_C();
            if (eval1(s, rtv, true) == false)
                break theend;

            if (s[0].at(0) != NUL)  /* check for trailing chars after "expr" */
            {
                emsg2(e_invexpr2, s[0]);
                clear_tv(rtv);
                break theend;
            }
            if (map)
            {
                /* map(): replace the list item value */
                clear_tv(tv);
                rtv.tv_lock = 0;
                COPY_typval(tv, rtv);
            }
            else
            {
                boolean[] error = { false };

                /* filter(): when "expr" is zero remove the item */
                remp[0] = (get_tv_number_chk(rtv, error) == 0);
                clear_tv(rtv);
                /* On type error, nothing has been removed; return false to stop the loop.
                 * The error message was given by get_tv_number_chk(). */
                if (error[0])
                    break theend;
            }
            retval = true;
        }

        clear_tv(vimvars[VV_VAL].vv_di.di_tv);
        return retval;
    }

    /*
     * "filter()" function
     */
    /*private*/ static final f_func_C f_filter = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            filter_map(argvars, rtv, false);
        }
    };

    /*
     * "function()" function
     */
    /*private*/ static final f_func_C f_function = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes s = get_tv_string(argvars[0]);

            if (s == null || s.at(0) == NUL || asc_isdigit(s.at(0)))
                emsg2(e_invarg2, s);
            /* Don't check an autoload name for existence here. */
            else if (vim_strchr(s, AUTOLOAD_CHAR) == null && !function_exists(s))
                emsg2(u8("E700: Unknown function: %s"), s);
            else
            {
                if (STRNCMP(s, u8("s:"), 2) == 0 || STRNCMP(s, u8("<SID>"), 5) == 0)
                {
                    Bytes sid_buf = new Bytes(25);
                    int off = (s.at(0) == (byte)'s') ? 2 : 5;

                    /* Expand "s:" and <SID> into <SNR>nr_, so that the function can also be
                     * called from another script.  Using trans_function_name() would also work,
                     * but some plugins depend on the name being printable text. */
                    libC.sprintf(sid_buf, u8("<SNR>%ld_"), (long)current_SID);
                    rtv.tv_string = new Bytes(strlen(sid_buf) + strlen(s, off) + 1);

                    STRCPY(rtv.tv_string, sid_buf);
                    STRCAT(rtv.tv_string, s.plus(off));
                }
                else
                    rtv.tv_string = STRDUP(s);
                rtv.tv_type = VAR_FUNC;
            }
        }
    };

    /*
     * "get()" function
     */
    /*private*/ static final f_func_C f_get = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            typval_C tv = null;

            if (argvars[0].tv_type == VAR_LIST)
            {
                list_C l = argvars[0].tv_list;
                if (l != null)
                {
                    boolean[] error = { false };

                    listitem_C li = list_find(l, get_tv_number_chk(argvars[1], error));
                    if (!error[0] && li != null)
                        tv = li.li_tv;
                }
            }
            else if (argvars[0].tv_type == VAR_DICT)
            {
                dict_C d = argvars[0].tv_dict;
                if (d != null)
                {
                    dictitem_C di = dict_find(d, get_tv_string(argvars[1]), -1);
                    if (di != null)
                        tv = di.di_tv;
                }
            }
            else
                emsg2(e_listdictarg, u8("get()"));

            if (tv == null)
            {
                if (argvars[2].tv_type != VAR_UNKNOWN)
                    copy_tv(argvars[2], rtv);
            }
            else
                copy_tv(tv, rtv);
        }
    };

    /*
     * Get line or list of lines from buffer "buf" into "rtv".
     * Return a range (from start to end) of lines in rtv from the specified buffer.
     * If 'retlist' is true, then the lines are returned as a Vim List.
     */
    /*private*/ static void get_buffer_lines(buffer_C buf, long start, long end, boolean retlist, typval_C rtv)
    {
        rtv.tv_type = VAR_STRING;
        rtv.tv_string = null;
        if (retlist)
            rettv_list_alloc(rtv);

        if (buf == null || buf.b_ml.ml_mfp == null || start < 0)
            return;

        if (!retlist)
        {
            Bytes p;
            if (1 <= start && start <= buf.b_ml.ml_line_count)
                p = ml_get_buf(buf, start, false);
            else
                p = u8("");
            rtv.tv_string = STRDUP(p);
        }
        else
        {
            if (end < start)
                return;

            if (start < 1)
                start = 1;
            if (end > buf.b_ml.ml_line_count)
                end = buf.b_ml.ml_line_count;
            while (start <= end)
                if (list_append_string(rtv.tv_list, ml_get_buf(buf, start++, false), -1) == false)
                    break;
        }
    }

    /*
     * "getbufline()" function
     */
    /*private*/ static final f_func_C f_getbufline = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            get_tv_number(argvars[0]);      /* issue errmsg if type error */
            emsg_off++;
            buffer_C buf = get_buf_tv(argvars[0], false);
            --emsg_off;

            long lnum = get_tv_lnum_buf(argvars[1], buf);
            long end;
            if (argvars[2].tv_type == VAR_UNKNOWN)
                end = lnum;
            else
                end = get_tv_lnum_buf(argvars[2], buf);

            get_buffer_lines(buf, lnum, end, true, rtv);
        }
    };

    /*
     * "getbufvar()" function
     */
    /*private*/ static final f_func_C f_getbufvar = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            boolean done = false;

            get_tv_number(argvars[0]);      /* issue errmsg if type error */
            Bytes[] varname = { get_tv_string_chk(argvars[1]) };

            emsg_off++;

            buffer_C buf = get_buf_tv(argvars[0], false);

            rtv.tv_type = VAR_STRING;
            rtv.tv_string = null;

            if (buf != null && varname[0] != null)
            {
                /* set curbuf to be our buf, temporarily */
                buffer_C save_curbuf = curbuf;
                curbuf = buf;

                if (varname[0].at(0) == (byte)'&')    /* buffer-local-option */
                {
                    if (get_option_tv(varname, rtv, true))
                        done = true;
                }
                else if (STRCMP(varname[0], u8("changedtick")) == 0)
                {
                    rtv.tv_type = VAR_NUMBER;
                    rtv.tv_number = curbuf.b_changedtick;
                    done = true;
                }
                else
                {
                    /* Look up the variable.
                     * Let getbufvar({nr}, "") return the "b:" dictionary. */
                    dictitem_C v = find_var_in_ht(curbuf.b_vars.dv_hashtab, 'b', varname[0], false);
                    if (v != null)
                    {
                        copy_tv(v.di_tv, rtv);
                        done = true;
                    }
                }

                /* restore previous notion of curbuf */
                curbuf = save_curbuf;
            }

            if (!done && argvars[2].tv_type != VAR_UNKNOWN)
                /* use the default value */
                copy_tv(argvars[2], rtv);

            --emsg_off;
        }
    };

    /*
     * "getchar()" function
     */
    /*private*/ static final f_func_C f_getchar = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            /* Position the cursor.  Needed after a message that ends in a space. */
            windgoto(msg_row, msg_col);

            int n;

            no_mapping++;
            allow_keys++;
            for ( ; ; )
            {
                boolean[] error = { false };

                if (argvars[0].tv_type == VAR_UNKNOWN)
                    /* getchar(): blocking wait. */
                    n = safe_vgetc();
                else if (get_tv_number_chk(argvars[0], error) == 1)
                    /* getchar(1): only check if char avail */
                    n = vpeekc_any();
                else if (error[0] || vpeekc_any() == NUL)
                    /* illegal argument or getchar(0) and no char avail: return zero */
                    n = 0;
                else
                    /* getchar(0) and char avail: return char */
                    n = safe_vgetc();

                if (n == K_IGNORE)
                    continue;
                break;
            }
            --no_mapping;
            --allow_keys;

            set_vim_var_nr(VV_MOUSE_WIN, 0);
            set_vim_var_nr(VV_MOUSE_LNUM, 0);
            set_vim_var_nr(VV_MOUSE_COL, 0);

            rtv.tv_number = n;
            if (is_special(n) || mod_mask != 0)
            {
                Bytes temp = new Bytes(10); /* modifier: 3, mbyte-char: 6, NUL: 1 */
                int i = 0;

                /* Turn a special key into three bytes, plus modifier. */
                if (mod_mask != 0)
                {
                    temp.be(i++, KB_SPECIAL);
                    temp.be(i++, KS_MODIFIER);
                    temp.be(i++, mod_mask);
                }
                if (is_special(n))
                {
                    temp.be(i++, KB_SPECIAL);
                    temp.be(i++, KB_SECOND(n));
                    temp.be(i++, KB_THIRD(n));
                }
                else
                    i += utf_char2bytes(n, temp.plus(i));
                temp.be(i++, NUL);
                rtv.tv_type = VAR_STRING;
                rtv.tv_string = STRDUP(temp);

                if (is_mouse_key(n))
                {
                    int[] row = { mouse_row };
                    int[] col = { mouse_col };

                    if (0 <= row[0] && 0 <= col[0])
                    {
                        long[] lnum = new long[1];

                        /* Find the window at the mouse coordinates and compute the text position. */
                        window_C win = mouse_find_win(row, col);
                        mouse_comp_pos(win, row, col, lnum);
                        int winnr = 1;
                        for (window_C wp = firstwin; wp != win; wp = wp.w_next)
                            winnr++;
                        set_vim_var_nr(VV_MOUSE_WIN, winnr);
                        set_vim_var_nr(VV_MOUSE_LNUM, lnum[0]);
                        set_vim_var_nr(VV_MOUSE_COL, col[0] + 1);
                    }
                }
            }
        }
    };

    /*
     * "getcharmod()" function
     */
    /*private*/ static final f_func_C f_getcharmod = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rtv.tv_number = mod_mask;
        }
    };

    /*
     * "getcmdline()" function
     */
    /*private*/ static final f_func_C f_getcmdline = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rtv.tv_type = VAR_STRING;
            rtv.tv_string = get_cmdline_str();
        }
    };

    /*
     * "getcmdpos()" function
     */
    /*private*/ static final f_func_C f_getcmdpos = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rtv.tv_number = get_cmdline_pos() + 1;
        }
    };

    /*
     * "getcmdtype()" function
     */
    /*private*/ static final f_func_C f_getcmdtype = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rtv.tv_type = VAR_STRING;
            rtv.tv_string = new Bytes(2);

            rtv.tv_string.be(0, get_cmdline_type());
            rtv.tv_string.be(1, NUL);
        }
    };

    /*
     * "getcmdwintype()" function
     */
    /*private*/ static final f_func_C f_getcmdwintype = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rtv.tv_type = VAR_STRING;
            rtv.tv_string = new Bytes(2);

            rtv.tv_string.be(0, cmdwin_type);
            rtv.tv_string.be(1, NUL);
        }
    };

    /*
     * "getline(lnum, [end])" function
     */
    /*private*/ static final f_func_C f_getline = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            long lnum = get_tv_lnum(argvars[0]);

            long end;
            boolean retlist;
            if (argvars[1].tv_type == VAR_UNKNOWN)
            {
                end = 0;
                retlist = false;
            }
            else
            {
                end = get_tv_lnum(argvars[1]);
                retlist = true;
            }

            get_buffer_lines(curbuf, lnum, end, retlist, rtv);
        }
    };

    /*
     * "getmatches()" function
     */
    /*private*/ static final f_func_C f_getmatches = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rettv_list_alloc(rtv);

            for (matchitem_C mi = curwin.w_match_head; mi != null; mi = mi.next)
            {
                dict_C dict = newDict();

                if (mi.mi_match.regprog == null)
                {
                    /* match added with matchaddpos() */
                    for (int i = 0; i < MAXPOSMATCH; i++)
                    {
                        Bytes buf = new Bytes(6);

                        llpos_C llpos = mi.mi_pos.pm_pos[i];
                        if (llpos.lnum == 0)
                            break;

                        list_C list = new list_C();

                        list_append_number(list, llpos.lnum);
                        if (0 < llpos.col)
                        {
                            list_append_number(list, llpos.col);
                            list_append_number(list, llpos.len);
                        }
                        libC.sprintf(buf, u8("pos%d"), i + 1);
                        dict_add_list(dict, buf, list);
                    }
                }
                else
                {
                    dict_add_nr_str(dict, u8("pattern"), 0L, mi.pattern);
                }
                dict_add_nr_str(dict, u8("group"), 0L, syn_id2name(mi.hlg_id));
                dict_add_nr_str(dict, u8("priority"), (long)mi.priority, null);
                dict_add_nr_str(dict, u8("id"), (long)mi.id, null);
                list_append_dict(rtv.tv_list, dict);
            }
        }
    };

    /*private*/ static void getpos_both(typval_C[] argvars, typval_C rtv, boolean getcurpos)
    {
        rettv_list_alloc(rtv);

        list_C l = rtv.tv_list;

        pos_C fp;
        int[] fnum = { -1 };
        if (getcurpos)
            fp = curwin.w_cursor;
        else
            fp = var2fpos(argvars[0], true, fnum);

        if (fnum[0] != -1)
            list_append_number(l, fnum[0]);
        else
            list_append_number(l, 0);

        if (fp != null)
        {
            list_append_number(l, fp.lnum);
            list_append_number(l, (fp.col == MAXCOL) ? MAXCOL : fp.col + 1);
            list_append_number(l, fp.coladd);
        }
        else
        {
            list_append_number(l, 0);
            list_append_number(l, 0);
            list_append_number(l, 0);
        }

        if (getcurpos)
            list_append_number(l, (curwin.w_curswant == MAXCOL) ? MAXCOL : curwin.w_curswant + 1);
    }

    /*
     * "getcurpos()" function
     */
    /*private*/ static final f_func_C f_getcurpos = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            getpos_both(argvars, rtv, true);
        }
    };

    /*
     * "getpos(string)" function
     */
    /*private*/ static final f_func_C f_getpos = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            getpos_both(argvars, rtv, false);
        }
    };

    /*
     * "getreg()" function
     */
    /*private*/ static final f_func_C f_getreg = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            boolean arg2 = false;
            boolean return_list = false;
            boolean[] error = { false };

            Bytes strregname;
            if (argvars[0].tv_type != VAR_UNKNOWN)
            {
                strregname = get_tv_string_chk(argvars[0]);
                error[0] = (strregname == null);
                if (argvars[1].tv_type != VAR_UNKNOWN)
                {
                    arg2 = (get_tv_number_chk(argvars[1], error) != 0);
                    if (!error[0] && argvars[2].tv_type != VAR_UNKNOWN)
                        return_list = (get_tv_number_chk(argvars[2], error) != 0);
                }
            }
            else
                strregname = vimvars[VV_REG].vv_di.di_tv.tv_string;

            if (error[0])
                return;

            int regname = (strregname == null) ? '"' : strregname.at(0);
            if (regname == 0)
                regname = '"';

            if (return_list)
            {
                rtv.tv_type = VAR_LIST;
                rtv.tv_list = (list_C)get_reg_contents(regname, (arg2 ? GREG_EXPR_SRC : 0) | GREG_LIST);
                if (rtv.tv_list != null)
                    rtv.tv_list.lv_refcount++;
            }
            else
            {
                rtv.tv_type = VAR_STRING;
                rtv.tv_string = (Bytes)get_reg_contents(regname, arg2 ? GREG_EXPR_SRC : 0);
            }
        }
    };

    /*
     * "getregtype()" function
     */
    /*private*/ static final f_func_C f_getregtype = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes strregname;
            if (argvars[0].tv_type != VAR_UNKNOWN)
            {
                strregname = get_tv_string_chk(argvars[0]);
                if (strregname == null)     /* type error; errmsg already given */
                {
                    rtv.tv_type = VAR_STRING;
                    rtv.tv_string = null;
                    return;
                }
            }
            else
                /* Default to v:register. */
                strregname = vimvars[VV_REG].vv_di.di_tv.tv_string;

            int regname = (strregname == null) ? '"' : strregname.at(0);
            if (regname == 0)
                regname = '"';

            Bytes buf = new Bytes(NUMBUFLEN + 2);

            long[] reglen = { 0 };
            switch (get_reg_type(regname, reglen))
            {
                case MLINE: buf.be(0, (byte)'V'); break;
                case MCHAR: buf.be(0, (byte)'v'); break;
                case MBLOCK:
                {
                    buf.be(0, Ctrl_V);
                    libC.sprintf(buf.plus(1), u8("%ld"), reglen[0] + 1);
                    break;
                }
            }
            rtv.tv_type = VAR_STRING;
            rtv.tv_string = STRDUP(buf);
        }
    };

    /*
     * "gettabvar()" function
     */
    /*private*/ static final f_func_C f_gettabvar = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_type = VAR_STRING;
            rtv.tv_string = null;

            boolean done = false;

            Bytes varname = get_tv_string_chk(argvars[1]);
            tabpage_C tp = find_tabpage((int)get_tv_number_chk(argvars[0], null));
            if (tp != null && varname != null)
            {
                /* Set tp to be our tabpage, temporarily.  Also set the window to the
                 * first window in the tabpage, otherwise the window is not valid. */
                window_C[] oldcurwin = new window_C[1];
                tabpage_C[] oldtabpage = new tabpage_C[1];
                if (switch_win(oldcurwin, oldtabpage, tp.tp_firstwin, tp, true))
                {
                    /* Look up the variable.
                     * Let gettabvar({nr}, "") return the "t:" dictionary. */
                    dictitem_C v = find_var_in_ht(tp.tp_vars.dv_hashtab, 't', varname, false);
                    if (v != null)
                    {
                        copy_tv(v.di_tv, rtv);
                        done = true;
                    }
                }

                /* restore previous notion of curwin */
                restore_win(oldcurwin[0], oldtabpage[0], true);
            }

            if (!done && argvars[2].tv_type != VAR_UNKNOWN)
                copy_tv(argvars[2], rtv);
        }
    };

    /*
     * Find window specified by "vp" in tabpage "tp".
     */
    /*private*/ static window_C find_win_by_nr(typval_C vp, tabpage_C tp)
        /* tp: null for current tab page */
    {
        int nr = (int)get_tv_number_chk(vp, null);
        if (nr < 0)
            return null;
        if (nr == 0)
            return curwin;

        window_C wp = (tp == null || tp == curtab) ? firstwin : tp.tp_firstwin;
        for ( ; wp != null; wp = wp.w_next)
            if (--nr <= 0)
                break;
        return wp;
    }

    /*
     * getwinvar() and gettabwinvar()
     */
    /*private*/ static void getwinvar(typval_C[] argvars, typval_C rtv, int off)
        /* off: 1 for gettabwinvar() */
    {
        rtv.tv_type = VAR_STRING;
        rtv.tv_string = null;

        tabpage_C tp;
        if (off == 1)
            tp = find_tabpage((int)get_tv_number_chk(argvars[0], null));
        else
            tp = curtab;
        window_C win = find_win_by_nr(argvars[off], tp);
        Bytes[] varname = { get_tv_string_chk(argvars[off + 1]) };

        emsg_off++;

        boolean done = false;

        if (win != null && varname[0] != null)
        {
            /* Set curwin to be our win, temporarily.
             * Also set the tabpage, otherwise the window is not valid. */
            window_C[] oldcurwin = new window_C[1];
            tabpage_C[] oldtabpage = new tabpage_C[1];
            if (switch_win(oldcurwin, oldtabpage, win, tp, true))
            {
                if (varname[0].at(0) == (byte)'&')        /* window-local-option */
                {
                    if (get_option_tv(varname, rtv, true))
                        done = true;
                }
                else
                {
                    /* Look up the variable. */
                    /* Let getwinvar({nr}, "") return the "w:" dictionary. */
                    dictitem_C v = find_var_in_ht(win.w_vars.dv_hashtab, 'w', varname[0], false);
                    if (v != null)
                    {
                        copy_tv(v.di_tv, rtv);
                        done = true;
                    }
                }
            }

            /* restore previous notion of curwin */
            restore_win(oldcurwin[0], oldtabpage[0], true);
        }

        if (!done && argvars[off + 2].tv_type != VAR_UNKNOWN)
            /* use the default return value */
            copy_tv(argvars[off + 2], rtv);

        --emsg_off;
    }

    /*
     * "gettabwinvar()" function
     */
    /*private*/ static final f_func_C f_gettabwinvar = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            getwinvar(argvars, rtv, 1);
        }
    };

    /*
     * "getwinvar()" function
     */
    /*private*/ static final f_func_C f_getwinvar = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            getwinvar(argvars, rtv, 0);
        }
    };

    /*
     * "glob2regpat()" function
     */
    /*private*/ static final f_func_C f_glob2regpat = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes pat = get_tv_string_chk(argvars[0]);

            rtv.tv_type = VAR_STRING;
            rtv.tv_string = file_pat_to_reg_pat(pat, null, null);
        }
    };

    /*
     * "has_key()" function
     */
    /*private*/ static final f_func_C f_has_key = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            if (argvars[0].tv_type != VAR_DICT)
            {
                emsg(e_dictreq);
                return;
            }
            if (argvars[0].tv_dict == null)
                return;

            rtv.tv_number = (dict_find(argvars[0].tv_dict, get_tv_string(argvars[1]), -1) != null) ? 1 : 0;
        }
    };

    /*
     * "hasmapto()" function
     */
    /*private*/ static final f_func_C f_hasmapto = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes name = get_tv_string(argvars[0]);
            Bytes mode;
            boolean abbr = false;

            if (argvars[1].tv_type == VAR_UNKNOWN)
                mode = u8("nvo");
            else
            {
                mode = get_tv_string(argvars[1]);
                if (argvars[2].tv_type != VAR_UNKNOWN)
                    abbr = (get_tv_number(argvars[2]) != 0);
            }

            rtv.tv_number = map_to_exists(name, mode, abbr) ? TRUE : FALSE;
        }
    };

    /*
     * "histadd()" function
     */
    /*private*/ static final f_func_C f_histadd = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = /*false*/0;
            if (check_restricted() || check_secure())
                return;

            Bytes str = get_tv_string_chk(argvars[0]);     /* null on type error */
            int histype = (str != null) ? get_histtype(str) : -1;
            if (0 <= histype)
            {
                str = get_tv_string(argvars[1]);
                if (str.at(0) != NUL)
                {
                    init_history();
                    add_to_history(histype, str, false, NUL);
                    rtv.tv_number = /*true*/1;
                }
            }
        }
    };

    /*
     * "histdel()" function
     */
    /*private*/ static final f_func_C f_histdel = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int n;

            Bytes str = get_tv_string_chk(argvars[0]);     /* null on type error */
            if (str == null)
                n = 0;
            else if (argvars[1].tv_type == VAR_UNKNOWN)
                /* only one argument: clear entire history */
                n = clr_history(get_histtype(str)) ? TRUE : FALSE;
            else if (argvars[1].tv_type == VAR_NUMBER)
                /* index given: remove that entry */
                n = del_history_idx(get_histtype(str), (int)get_tv_number(argvars[1])) ? TRUE : FALSE;
            else
                /* string given: remove all matching entries */
                n = del_history_entry(get_histtype(str), get_tv_string(argvars[1])) ? TRUE : FALSE;

            rtv.tv_number = n;
        }
    };

    /*
     * "histget()" function
     */
    /*private*/ static final f_func_C f_histget = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes str = get_tv_string_chk(argvars[0]);     /* null on type error */
            if (str == null)
                rtv.tv_string = null;
            else
            {
                int idx;

                int type = get_histtype(str);
                if (argvars[1].tv_type == VAR_UNKNOWN)
                    idx = get_history_idx(type);
                else
                    idx = (int)get_tv_number_chk(argvars[1], null);
                                                            /* -1 on type error */
                rtv.tv_string = STRDUP(get_history_entry(type, idx));
            }
            rtv.tv_type = VAR_STRING;
        }
    };

    /*
     * "histnr()" function
     */
    /*private*/ static final f_func_C f_histnr = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes history = get_tv_string_chk(argvars[0]);

            int i = (history == null) ? HIST_CMD - 1 : get_histtype(history);
            if (HIST_CMD <= i && i < HIST_COUNT)
                i = get_history_idx(i);
            else
                i = -1;

            rtv.tv_number = i;
        }
    };

    /*
     * "hlID(name)" function
     */
    /*private*/ static final f_func_C f_hlID = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = syn_name2id(get_tv_string(argvars[0]));
        }
    };

    /*
     * "hlexists()" function
     */
    /*private*/ static final f_func_C f_hlexists = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = highlight_exists(get_tv_string(argvars[0])) ? 1 : 0;
        }
    };

    /*
     * "indent()" function
     */
    /*private*/ static final f_func_C f_indent = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            long lnum = get_tv_lnum(argvars[0]);
            if (1 <= lnum && lnum <= curbuf.b_ml.ml_line_count)
                rtv.tv_number = get_indent_lnum(lnum);
            else
                rtv.tv_number = -1;
        }
    };

    /*
     * "index()" function
     */
    /*private*/ static final f_func_C f_index = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = -1;
            if (argvars[0].tv_type != VAR_LIST)
            {
                emsg(e_listreq);
                return;
            }
            list_C l = argvars[0].tv_list;
            if (l != null)
            {
                long idx = 0;
                boolean ic = false;

                listitem_C item = l.lv_first;
                if (argvars[2].tv_type != VAR_UNKNOWN)
                {
                    boolean[] error = { false };

                    /* Start at specified item.  Use the cached index that list_find() sets,
                     * so that a negative number also works. */
                    item = list_find(l, get_tv_number_chk(argvars[2], error));
                    idx = l.lv_idx;
                    if (argvars[3].tv_type != VAR_UNKNOWN)
                        ic = (get_tv_number_chk(argvars[3], error) != 0);
                    if (error[0])
                        item = null;
                }

                for ( ; item != null; item = item.li_next, ++idx)
                    if (tv_equal(item.li_tv, argvars[1], ic, false))
                    {
                        rtv.tv_number = idx;
                        break;
                    }
            }
        }
    };

    /*private*/ static int inputsecret_flag;

    /*
     * This function is used by f_input() and f_inputdialog() functions.
     * The third argument to f_input() specifies the type of completion to use at the prompt.
     * The third argument to f_inputdialog() specifies the value to return when the user cancels the prompt.
     */
    /*private*/ static void get_user_input(typval_C[] argvars, typval_C rtv, boolean inputdialog)
    {
        rtv.tv_type = VAR_STRING;
        rtv.tv_string = null;

        boolean cmd_silent_save = cmd_silent;
        cmd_silent = false;         /* Want to see the prompt. */

        Bytes prompt = get_tv_string_chk(argvars[0]);
        if (prompt != null)
        {
            /* Only the part of the message after the last NL
             * is considered as prompt for the command line. */
            Bytes p = vim_strrchr(prompt, (byte)'\n');
            if (p == null)
                p = prompt;
            else
            {
                p = p.plus(1);
                byte b = p.at(0);
                p.be(0, NUL);
                msg_start();
                msg_clr_eos();
                msg_puts_attr(prompt, echo_attr);
                msg_didout = false;
                msg_starthere();
                p.be(0, b);
            }
            cmdline_row = msg_row;

            int[] xp_type = { EXPAND_NOTHING };
            Bytes[] xp_arg = { null };

            Bytes defstr = u8("");
            if (argvars[1].tv_type != VAR_UNKNOWN)
            {
                defstr = get_tv_string_chk(argvars[1]);
                if (defstr != null)
                    stuffReadbuffSpec(defstr);

                if (!inputdialog && argvars[2].tv_type != VAR_UNKNOWN)
                {
                    /* input() with a third argument: completion */
                    rtv.tv_string = null;

                    Bytes xp_name = get_tv_string_chk(argvars[2]);
                    if (xp_name == null)
                        return;

                    int xp_namelen = strlen(xp_name);

                    long[] argt = new long[1];
                    if (parse_compl_arg(xp_name, xp_namelen, xp_type, argt, xp_arg) == false)
                        return;
                }
            }

            if (defstr != null)
            {
                int save_ex_normal_busy = ex_normal_busy;
                ex_normal_busy = 0;
                rtv.tv_string = getcmdline_prompt((inputsecret_flag != 0) ? NUL : '@', p, echo_attr, xp_type[0], xp_arg[0]);
                ex_normal_busy = save_ex_normal_busy;
            }
            if (inputdialog && rtv.tv_string == null
                    && argvars[1].tv_type != VAR_UNKNOWN
                    && argvars[2].tv_type != VAR_UNKNOWN)
                rtv.tv_string = STRDUP(get_tv_string(argvars[2]));

            /* since the user typed this, no need to wait for return */
            need_wait_return = false;
            msg_didout = false;
        }

        cmd_silent = cmd_silent_save;
    }

    /*
     * "input()" function
     *     Also handles inputsecret() when inputsecret is set.
     */
    /*private*/ static final f_func_C f_input = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            get_user_input(argvars, rtv, false);
        }
    };

    /*
     * "inputdialog()" function
     */
    /*private*/ static final f_func_C f_inputdialog = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            get_user_input(argvars, rtv, true);
        }
    };

    /*
     * "inputlist()" function
     */
    /*private*/ static final f_func_C f_inputlist = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            if (argvars[0].tv_type != VAR_LIST || argvars[0].tv_list == null)
            {
                emsg2(e_listarg, u8("inputlist()"));
                return;
            }

            msg_start();
            msg_row = (int)Rows[0] - 1;         /* for when 'cmdheight' > 1 */
            lines_left = (int)Rows[0];          /* avoid more prompt */
            msg_scroll = true;
            msg_clr_eos();

            for (listitem_C li = argvars[0].tv_list.lv_first; li != null; li = li.li_next)
            {
                msg_puts(get_tv_string(li.li_tv));
                msg_putchar('\n');
            }

            /* Ask for choice. */
            boolean[] mouse_used = new boolean[1];
            int selected = prompt_for_number(mouse_used);
            if (mouse_used[0])
                selected -= lines_left;

            rtv.tv_number = selected;
        }
    };

    /*private*/ static Growing<tasave_C> ga_userinput = new Growing<tasave_C>(tasave_C.class, 4);

    /*
     * "inputrestore()" function
     */
    /*private*/ static final f_func_C f_inputrestore = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            if (0 < ga_userinput.ga_len)
            {
                tasave_C[] taspp = ga_userinput.ga_data;
                int i = --ga_userinput.ga_len;
                restore_typeahead(taspp[i]);
                taspp[i] = null;
                /* default return is zero == OK */
            }
            else if (1 < p_verbose[0])
            {
                verb_msg(u8("called inputrestore() more often than inputsave()"));
                rtv.tv_number = 1; /* Failed */
            }
        }
    };

    /*
     * "inputsave()" function
     */
    /*private*/ static final f_func_C f_inputsave = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C _rettv)
        {
            /* Add an entry to the stack of typeahead storage. */
            tasave_C[] taspp = ga_userinput.ga_grow(1);
            int i = ga_userinput.ga_len++;
            taspp[i] = new_tasave();
            save_typeahead(taspp[i]);
            /* default return is zero == OK */
        }
    };

    /*
     * "inputsecret()" function
     */
    /*private*/ static final f_func_C f_inputsecret = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            cmdline_star++;
            inputsecret_flag++;

            f_input.fun(argvars, rtv);

            --inputsecret_flag;
            --cmdline_star;
        }
    };

    /*
     * "insert()" function
     */
    /*private*/ static final f_func_C f_insert = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            list_C l;

            if (argvars[0].tv_type != VAR_LIST)
                emsg2(e_listarg, u8("insert()"));
            else if ((l = argvars[0].tv_list) != null && !tv_check_lock(l.lv_lock, u8("insert() argument")))
            {
                boolean[] error = { false };

                long before = 0;
                if (argvars[2].tv_type != VAR_UNKNOWN)
                    before = get_tv_number_chk(argvars[2], error);
                if (error[0])
                    return;             /* type error; errmsg already given */

                listitem_C item;
                if (before == l.lv_len)
                    item = null;
                else
                {
                    item = list_find(l, before);
                    if (item == null)
                    {
                        emsgn(e_listidx, before);
                        l = null;
                    }
                }
                if (l != null)
                {
                    list_insert_tv(l, argvars[1], item);
                    copy_tv(argvars[0], rtv);
                }
            }
        }
    };

    /*
     * "invert(expr)" function
     */
    /*private*/ static final f_func_C f_invert = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = ~get_tv_number_chk(argvars[0], null);
        }
    };

    /*
     * "islocked()" function
     */
    /*private*/ static final f_func_C f_islocked = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = -1;

            lval_C lv = new lval_C();
            Bytes end = get_lval(get_tv_string(argvars[0]), null, lv, false, false, GLV_NO_AUTOLOAD, FNE_CHECK_START);
            if (end != null && lv.ll_name != null)
            {
                if (end.at(0) != NUL)
                    emsg(e_trailing);
                else
                {
                    if (lv.ll_tv == null)
                    {
                        if (check_changedtick(lv.ll_name))
                            rtv.tv_number = 1;        /* always locked */
                        else
                        {
                            dictitem_C di = find_var(lv.ll_name, null, true);
                            if (di != null)
                            {
                                /* Consider a variable locked when:
                                 * 1. the variable itself is locked
                                 * 2. the value of the variable is locked.
                                 * 3. the List or Dict value is locked.
                                 */
                                rtv.tv_number = ((di.di_flags & DI_FLAGS_LOCK) != 0 || tv_islocked(di.di_tv)) ? 1 : 0;
                            }
                        }
                    }
                    else if (lv.ll_range)
                        emsg(u8("E786: Range not allowed"));
                    else if (lv.ll_newkey != null)
                        emsg2(e_dictkey, lv.ll_newkey);
                    else if (lv.ll_list != null)
                        /* List item. */
                        rtv.tv_number = tv_islocked(lv.ll_li.li_tv) ? 1 : 0;
                    else
                        /* Dictionary item. */
                        rtv.tv_number = tv_islocked(lv.ll_di.di_tv) ? 1 : 0;
                }
            }

            clear_lval(lv);
        }
    };

    /*
     * Turn a dict into a list:
     * "what" == 0: list of keys
     * "what" == 1: list of values
     * "what" == 2: list of items
     */
    /*private*/ static void dict_list(typval_C varp, typval_C rtv, int what)
    {
        if (varp.tv_type != VAR_DICT)
        {
            emsg(e_dictreq);
            return;
        }

        dict_C dict = varp.tv_dict;
        if (dict == null)
            return;

        rettv_list_alloc(rtv);

        for (int i = 0, todo = (int)dict.dv_hashtab.ht_used; 0 < todo; i++)
        {
            hashitem_C hi = dict.dv_hashtab.ht_buckets[i];
            if (!hashitem_empty(hi))
            {
                dictitem_C di = (dictitem_C)hi.hi_data;

                listitem_C li = new listitem_C();

                list_append(rtv.tv_list, li);

                if (what == 0)
                {
                    /* keys() */
                    li.li_tv.tv_type = VAR_STRING;
                    li.li_tv.tv_lock = 0;
                    li.li_tv.tv_string = STRDUP(di.di_key);
                }
                else if (what == 1)
                {
                    /* values() */
                    copy_tv(di.di_tv, li.li_tv);
                }
                else
                {
                    /* items() */
                    list_C l2 = new list_C();

                    li.li_tv.tv_type = VAR_LIST;
                    li.li_tv.tv_lock = 0;
                    li.li_tv.tv_list = l2;

                    l2.lv_refcount++;

                    listitem_C li2 = new listitem_C();

                    list_append(l2, li2);
                    li2.li_tv.tv_type = VAR_STRING;
                    li2.li_tv.tv_lock = 0;
                    li2.li_tv.tv_string = STRDUP(di.di_key);

                    li2 = new listitem_C();

                    list_append(l2, li2);
                    copy_tv(di.di_tv, li2.li_tv);
                }

                --todo;
            }
        }
    }

    /*
     * "items(dict)" function
     */
    /*private*/ static final f_func_C f_items = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            dict_list(argvars[0], rtv, 2);
        }
    };

    /*
     * "join()" function
     */
    /*private*/ static final f_func_C f_join = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            if (argvars[0].tv_type != VAR_LIST)
            {
                emsg(e_listreq);
                return;
            }
            if (argvars[0].tv_list == null)
                return;

            Bytes sep;
            if (argvars[1].tv_type == VAR_UNKNOWN)
                sep = u8(" ");
            else
                sep = get_tv_string_chk(argvars[1]);

            rtv.tv_type = VAR_STRING;

            if (sep != null)
            {
                barray_C ba = new barray_C(80);

                list_join(ba, argvars[0].tv_list, sep, true, 0);
                ba_append(ba, NUL);
                rtv.tv_string = new Bytes(ba.ba_data);
            }
            else
                rtv.tv_string = null;
        }
    };

    /*
     * "keys(dict)" function
     */
    /*private*/ static final f_func_C f_keys = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            dict_list(argvars[0], rtv, 0);
        }
    };

    /*
     * "len()" function
     */
    /*private*/ static final f_func_C f_len = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            switch (argvars[0].tv_type)
            {
                case VAR_STRING:
                case VAR_NUMBER:
                    rtv.tv_number = strlen(get_tv_string(argvars[0]));
                    break;
                case VAR_LIST:
                    rtv.tv_number = list_len(argvars[0].tv_list);
                    break;
                case VAR_DICT:
                    rtv.tv_number = dict_len(argvars[0].tv_dict);
                    break;
                default:
                    emsg(u8("E701: Invalid type for len()"));
                    break;
            }
        }
    };

    /*
     * "line(string)" function
     */
    /*private*/ static final f_func_C f_line = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            long lnum = 0;

            int[] fnum = new int[1];
            pos_C fp = var2fpos(argvars[0], true, fnum);
            if (fp != null)
                lnum = fp.lnum;

            rtv.tv_number = lnum;
        }
    };

    /*
     * "line2byte(lnum)" function
     */
    /*private*/ static final f_func_C f_line2byte = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            long lnum = get_tv_lnum(argvars[0]);

            if (lnum < 1 || curbuf.b_ml.ml_line_count + 1 < lnum)
                rtv.tv_number = -1;
            else
                rtv.tv_number = ml_find_line_or_offset(curbuf, lnum, null);

            if (0 <= rtv.tv_number)
                rtv.tv_number++;
        }
    };

    /*
     * "lispindent(lnum)" function
     */
    /*private*/ static final f_func_C f_lispindent = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            pos_C pos = new pos_C();
            COPY_pos(pos, curwin.w_cursor);
            long lnum = get_tv_lnum(argvars[0]);

            if (1 <= lnum && lnum <= curbuf.b_ml.ml_line_count)
            {
                curwin.w_cursor.lnum = lnum;
                rtv.tv_number = get_lisp_indent.getindent();
                COPY_pos(curwin.w_cursor, pos);
            }
            else
                rtv.tv_number = -1;
        }
    };

    /*
     * "localtime()" function
     */
    /*private*/ static final f_func_C f_localtime = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rtv.tv_number = libC._time();
        }
    };

    /*private*/ static void get_maparg(typval_C[] argvars, typval_C rtv, boolean exact)
    {
        boolean abbr = false;
        boolean get_dict = false;

        /* return empty string for failure */
        rtv.tv_type = VAR_STRING;
        rtv.tv_string = null;

        Bytes keys = get_tv_string(argvars[0]);
        if (keys.at(0) == NUL)
            return;

        Bytes[] which = new Bytes[1];
        if (argvars[1].tv_type != VAR_UNKNOWN)
        {
            which[0] = get_tv_string_chk(argvars[1]);
            if (argvars[2].tv_type != VAR_UNKNOWN)
            {
                abbr = (get_tv_number(argvars[2]) != 0);
                if (argvars[3].tv_type != VAR_UNKNOWN)
                    get_dict = (get_tv_number(argvars[3]) != 0);
            }
        }
        else
            which[0] = u8("");
        if (which[0] == null)
            return;

        int mode = get_map_mode(which, false);

        keys = replace_termcodes(keys, true, true, false);
        mapblock_C[] mp = { null };
        int[] buffer_local = { 0 };
        Bytes rhs = check_map(keys, mode, exact, false, abbr, mp, buffer_local);

        if (!get_dict)
        {
            /* Return a string. */
            if (rhs != null)
                rtv.tv_string = str2special_save(rhs, false);
        }
        else
        {
            rettv_dict_alloc(rtv);

            if (rhs != null)
            {
                /* Return a dictionary. */
                Bytes lhs = str2special_save(mp[0].m_keys, true);
                Bytes mapmode = map_mode_to_chars(mp[0].m_mode);
                dict_C dict = rtv.tv_dict;

                dict_add_nr_str(dict, u8("lhs"),     0L, lhs);
                dict_add_nr_str(dict, u8("rhs"),     0L, mp[0].m_orig_str);
                dict_add_nr_str(dict, u8("noremap"), (mp[0].m_noremap != 0) ? 1L : 0L, null);
                dict_add_nr_str(dict, u8("expr"),    mp[0].m_expr    ? 1L : 0L, null);
                dict_add_nr_str(dict, u8("silent"),  mp[0].m_silent  ? 1L : 0L, null);
                dict_add_nr_str(dict, u8("sid"),     (long)mp[0].m_script_ID, null);
                dict_add_nr_str(dict, u8("buffer"),  (long)buffer_local[0], null);
                dict_add_nr_str(dict, u8("nowait"),  mp[0].m_nowait  ? 1L : 0L, null);
                dict_add_nr_str(dict, u8("mode"),    0L, mapmode);
            }
        }
    }

    /*
     * "map()" function
     */
    /*private*/ static final f_func_C f_map = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            filter_map(argvars, rtv, true);
        }
    };

    /*
     * "maparg()" function
     */
    /*private*/ static final f_func_C f_maparg = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            get_maparg(argvars, rtv, true);
        }
    };

    /*
     * "mapcheck()" function
     */
    /*private*/ static final f_func_C f_mapcheck = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            get_maparg(argvars, rtv, false);
        }
    };

    /*private*/ static void find_some_match(typval_C[] argvars, typval_C rtv, int type)
    {
        int nth = 1;
        int startcol = 0;
        long idx = 0;

        /* Make 'cpoptions' empty, the 'l' flag should not be used here. */
        Bytes save_cpo = p_cpo[0];
        p_cpo[0] = u8("");

        rtv.tv_number = -1;
        if (type == 3)
        {
            /* return empty list when there are no matches */
            rettv_list_alloc(rtv);
        }
        else if (type == 2)
        {
            rtv.tv_type = VAR_STRING;
            rtv.tv_string = null;
        }

        list_C l = null;
        listitem_C li = null;
        Bytes expr = null;
        Bytes str = null;
        int len = 0;

        theend:
        {
            if (argvars[0].tv_type == VAR_LIST)
            {
                l = argvars[0].tv_list;
                if (l == null)
                    break theend;
                li = l.lv_first;
            }
            else
            {
                expr = str = get_tv_string(argvars[0]);
                len = strlen(str);
            }

            Bytes pat = get_tv_string_chk(argvars[1]);
            if (pat == null)
                break theend;

            if (argvars[2].tv_type != VAR_UNKNOWN)
            {
                boolean[] error = { false };

                int start = (int)get_tv_number_chk(argvars[2], error);
                if (error[0])
                    break theend;
                if (l != null)
                {
                    li = list_find(l, start);
                    if (li == null)
                        break theend;
                    idx = l.lv_idx;     /* use the cached index */
                }
                else
                {
                    if (start < 0)
                        start = 0;
                    if (len < start)
                        break theend;
                    /* When "count" argument is there,
                     * ignore matches before "start", otherwise skip part of the string.
                     * Differs when pattern is "^" or "\<". */
                    if (argvars[3].tv_type != VAR_UNKNOWN)
                        startcol = start;
                    else
                    {
                        str = str.plus(start);
                        len -= start;
                    }
                }

                if (argvars[3].tv_type != VAR_UNKNOWN)
                    nth = (int)get_tv_number_chk(argvars[3], error);
                if (error[0])
                    break theend;
            }

            regmatch_C regmatch = new regmatch_C();
            regmatch.regprog = vim_regcomp(pat, RE_MAGIC + RE_STRING);
            if (regmatch.regprog != null)
            {
                regmatch.rm_ic = p_ic[0];

                boolean match = false;
                for ( ; ; )
                {
                    if (l != null)
                    {
                        if (li == null)
                        {
                            match = false;
                            break;
                        }
                        str = echo_string(li.li_tv, 0);
                        if (str == null)
                            break;
                    }

                    match = vim_regexec_nl(regmatch, str, startcol);

                    if (match && --nth <= 0)
                        break;
                    if (l == null && !match)
                        break;

                    /* Advance to just after the match. */
                    if (l != null)
                    {
                        li = li.li_next;
                        idx++;
                    }
                    else
                    {
                        startcol = BDIFF(regmatch.startp[0].plus(us_ptr2len_cc(regmatch.startp[0])), str);
                        if (len < startcol || BLE(str.plus(startcol), regmatch.startp[0]))
                        {
                            match = false;
                            break;
                        }
                    }
                }

                if (match)
                {
                    if (type == 3)
                    {
                        int i;

                        /* return list with matched string and submatches */
                        for (i = 0; i < NSUBEXP; i++)
                        {
                            if (regmatch.endp[i] == null)
                            {
                                if (list_append_string(rtv.tv_list, u8(""), 0) == false)
                                    break;
                            }
                            else if (list_append_string(rtv.tv_list, regmatch.startp[i],
                                        BDIFF(regmatch.endp[i], regmatch.startp[i])) == false)
                                break;
                        }
                    }
                    else if (type == 2)
                    {
                        /* return matched string */
                        if (l != null)
                            copy_tv(li.li_tv, rtv);
                        else
                            rtv.tv_string = STRNDUP(regmatch.startp[0],
                                        BDIFF(regmatch.endp[0], regmatch.startp[0]));
                    }
                    else if (l != null)
                        rtv.tv_number = idx;
                    else
                    {
                        if (type != 0)
                            rtv.tv_number = BDIFF(regmatch.startp[0], str);
                        else
                            rtv.tv_number = BDIFF(regmatch.endp[0], str);
                        rtv.tv_number += BDIFF(str, expr);
                    }
                }
            }
        }

        p_cpo[0] = save_cpo;
    }

    /*
     * "match()" function
     */
    /*private*/ static final f_func_C f_match = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            find_some_match(argvars, rtv, 1);
        }
    };

    /*
     * "matchadd()" function
     */
    /*private*/ static final f_func_C f_matchadd = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = -1;

            Bytes grp = get_tv_string_chk(argvars[0]);    /* group */
            Bytes pat = get_tv_string_chk(argvars[1]);    /* pattern */
            if (grp == null || pat == null)
                return;

            int prio = 10;      /* default priority */
            int id = -1;

            boolean[] error = { false };
            if (argvars[2].tv_type != VAR_UNKNOWN)
            {
                prio = (int)get_tv_number_chk(argvars[2], error);
                if (argvars[3].tv_type != VAR_UNKNOWN)
                    id = (int)get_tv_number_chk(argvars[3], error);
            }
            if (error[0])
                return;

            if (1 <= id && id <= 3)
            {
                emsgn(u8("E798: ID is reserved for \":match\": %ld"), (long)id);
                return;
            }

            rtv.tv_number = match_add(curwin, grp, pat, prio, id, null);
        }
    };

    /*
     * "matchaddpos()" function
     */
    /*private*/ static final f_func_C f_matchaddpos = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = -1;

            Bytes group = get_tv_string_chk(argvars[0]);
            if (group == null)
                return;

            if (argvars[1].tv_type != VAR_LIST)
            {
                emsg2(e_listarg, u8("matchaddpos()"));
                return;
            }
            list_C l = argvars[1].tv_list;
            if (l == null)
                return;

            int prio = 10;
            int id = -1;

            boolean[] error = { false };
            if (argvars[2].tv_type != VAR_UNKNOWN)
            {
                prio = (int)get_tv_number_chk(argvars[2], error);
                if (argvars[3].tv_type != VAR_UNKNOWN)
                    id = (int)get_tv_number_chk(argvars[3], error);
            }
            if (error[0])
                return;

            /* id == 3 is ok because matchaddpos() is supposed to substitute :3match */
            if (id == 1 || id == 2)
            {
                emsgn(u8("E798: ID is reserved for \":match\": %ld"), (long)id);
                return;
            }

            rtv.tv_number = match_add(curwin, group, null, prio, id, l);
        }
    };

    /*
     * "matcharg()" function
     */
    /*private*/ static final f_func_C f_matcharg = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rettv_list_alloc(rtv);

            int id = (int)get_tv_number(argvars[0]);

            if (1 <= id && id <= 3)
            {
                matchitem_C mi = get_match(curwin, id);
                if (mi != null)
                {
                    list_append_string(rtv.tv_list, syn_id2name(mi.hlg_id), -1);
                    list_append_string(rtv.tv_list, mi.pattern, -1);
                }
                else
                {
                    list_append_string(rtv.tv_list, null, -1);
                    list_append_string(rtv.tv_list, null, -1);
                }
            }
        }
    };

    /*
     * "matchdelete()" function
     */
    /*private*/ static final f_func_C f_matchdelete = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = match_delete(curwin, (int)get_tv_number(argvars[0]), true);
        }
    };

    /*
     * "matchend()" function
     */
    /*private*/ static final f_func_C f_matchend = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            find_some_match(argvars, rtv, 0);
        }
    };

    /*
     * "matchlist()" function
     */
    /*private*/ static final f_func_C f_matchlist = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            find_some_match(argvars, rtv, 3);
        }
    };

    /*
     * "matchstr()" function
     */
    /*private*/ static final f_func_C f_matchstr = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            find_some_match(argvars, rtv, 2);
        }
    };

    /*private*/ static void max_min(typval_C varp, typval_C rtv, boolean domax)
    {
        long m = 0;
        boolean[] error = { false };

        if (varp.tv_type == VAR_LIST)
        {
            list_C l = varp.tv_list;
            if (l != null)
            {
                listitem_C li = l.lv_first;
                if (li != null)
                {
                    m = get_tv_number_chk(li.li_tv, error);
                    for ( ; ; )
                    {
                        li = li.li_next;
                        if (li == null)
                            break;
                        long n = get_tv_number_chk(li.li_tv, error);
                        if (domax ? m < n : n < m)
                            m = n;
                    }
                }
            }
        }
        else if (varp.tv_type == VAR_DICT)
        {
            dict_C d = varp.tv_dict;
            if (d != null)
            {
                boolean first = true;

                for (int i = 0, todo = (int)d.dv_hashtab.ht_used; 0 < todo; i++)
                {
                    hashitem_C hi = d.dv_hashtab.ht_buckets[i];
                    if (!hashitem_empty(hi))
                    {
                        long n = get_tv_number_chk(((dictitem_C)hi.hi_data).di_tv, error);
                        if (first)
                        {
                            m = n;
                            first = false;
                        }
                        else if (domax ? m < n : n < m)
                            m = n;
                        --todo;
                    }
                }
            }
        }
        else
            emsg(e_listdictarg);

        rtv.tv_number = (error[0]) ? 0 : m;
    }

    /*
     * "max()" function
     */
    /*private*/ static final f_func_C f_max = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            max_min(argvars[0], rtv, true);
        }
    };

    /*
     * "min()" function
     */
    /*private*/ static final f_func_C f_min = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            max_min(argvars[0], rtv, false);
        }
    };

    /*
     * "mode()" function
     */
    /*private*/ static final f_func_C f_mode = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes buf = new Bytes(3);

            if (VIsual_active)
            {
                if (VIsual_select)
                    buf.be(0, VIsual_mode + 's' - 'v');
                else
                    buf.be(0, VIsual_mode);
            }
            else if (State == HITRETURN || State == ASKMORE || State == SETWSIZE || State == CONFIRM)
            {
                buf.be(0, (byte)'r');
                if (State == ASKMORE)
                    buf.be(1, (byte)'m');
                else if (State == CONFIRM)
                    buf.be(1, (byte)'?');
            }
            else if (State == EXTERNCMD)
                buf.be(0, (byte)'!');
            else if ((State & INSERT) != 0)
            {
                if ((State & VREPLACE_FLAG) != 0)
                {
                    buf.be(0, (byte)'R');
                    buf.be(1, (byte)'v');
                }
                else if ((State & REPLACE_FLAG) != 0)
                    buf.be(0, (byte)'R');
                else
                    buf.be(0, (byte)'i');
            }
            else if ((State & CMDLINE) != 0)
            {
                buf.be(0, (byte)'c');
                if (exmode_active != 0)
                    buf.be(1, (byte)'v');
            }
            else if (exmode_active != 0)
            {
                buf.be(0, (byte)'c');
                buf.be(1, (byte)'e');
            }
            else
            {
                buf.be(0, (byte)'n');
                if (finish_op)
                    buf.be(1, (byte)'o');
            }

            /* Clear out the minor mode when the argument is not a non-zero number or non-empty string. */
            if (!non_zero_arg(argvars[0]))
                buf.be(1, NUL);

            rtv.tv_string = STRDUP(buf);
            rtv.tv_type = VAR_STRING;
        }
    };

    /*
     * "nextnonblank()" function
     */
    /*private*/ static final f_func_C f_nextnonblank = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            long lnum;

            for (lnum = get_tv_lnum(argvars[0]); ; lnum++)
            {
                if (lnum < 0 || curbuf.b_ml.ml_line_count < lnum)
                {
                    lnum = 0;
                    break;
                }
                if (skipwhite(ml_get(lnum)).at(0) != NUL)
                    break;
            }

            rtv.tv_number = lnum;
        }
    };

    /*
     * "nr2char()" function
     */
    /*private*/ static final f_func_C f_nr2char = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes buf = new Bytes(NUMBUFLEN);
            buf.be(utf_char2bytes((int)get_tv_number(argvars[0]), buf), NUL);

            rtv.tv_type = VAR_STRING;
            rtv.tv_string = STRDUP(buf);
        }
    };

    /*
     * "or(expr, expr)" function
     */
    /*private*/ static final f_func_C f_or = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = get_tv_number_chk(argvars[0], null) | get_tv_number_chk(argvars[1], null);
        }
    };

    /*
     * "pathshorten()" function
     */
    /*private*/ static final f_func_C f_pathshorten = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_type = VAR_STRING;

            Bytes p = get_tv_string_chk(argvars[0]);
            if (p == null)
                rtv.tv_string = null;
            else
            {
                p = STRDUP(p);
                rtv.tv_string = p;
                if (p != null)
                    shorten_dir(p);
            }
        }
    };

    /*
     * "prevnonblank()" function
     */
    /*private*/ static final f_func_C f_prevnonblank = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            long lnum = get_tv_lnum(argvars[0]);

            if (lnum < 1 || curbuf.b_ml.ml_line_count < lnum)
                lnum = 0;
            else
                while (1 <= lnum && skipwhite(ml_get(lnum)).at(0) == NUL)
                    --lnum;

            rtv.tv_number = lnum;
        }
    };

    /*
     * "range()" function
     */
    /*private*/ static final f_func_C f_range = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            long stride = 1;
            boolean[] error = { false };

            long start = get_tv_number_chk(argvars[0], error);
            long end;
            if (argvars[1].tv_type == VAR_UNKNOWN)
            {
                end = start - 1;
                start = 0;
            }
            else
            {
                end = get_tv_number_chk(argvars[1], error);
                if (argvars[2].tv_type != VAR_UNKNOWN)
                    stride = get_tv_number_chk(argvars[2], error);
            }

            if (error[0])
                return;         /* type error; errmsg already given */

            if (stride == 0)
                emsg(u8("E726: Stride is zero"));
            else if (0 < stride ? end + 1 < start : start < end - 1)
                emsg(u8("E727: Start past end"));
            else
            {
                rettv_list_alloc(rtv);
                for (long i = start; 0 < stride ? i <= end : end <= i; i += stride)
                    list_append_number(rtv.tv_list, i);
            }
        }
    };

    /*
     * "readfile()" function
     */
    /*private*/ static final f_func_C f_readfile = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes buf = new Bytes((IOSIZE / 256) * 256);    /* rounded to avoid odd + 1 */
            int io_size = buf.size();
            long maxline = MAXLNUM;

            boolean binary = false;
            if (argvars[1].tv_type != VAR_UNKNOWN)
            {
                if (STRCMP(get_tv_string(argvars[1]), u8("b")) == 0)
                    binary = true;
                if (argvars[2].tv_type != VAR_UNKNOWN)
                    maxline = get_tv_number(argvars[2]);
            }

            rettv_list_alloc(rtv);

            /* Always open the file in binary mode,
             * library functions have a mind of their own about CR-LF conversion. */
            Bytes fname = get_tv_string(argvars[0]);
            file_C fd;
            if (fname.at(0) == NUL || (fd = libC.fopen(fname, u8("r"))) == null)
            {
                emsg2(e_notopen, (fname.at(0) == NUL) ? u8("<empty>") : fname);
                return;
            }

            Bytes prev = null;                 /* previously read bytes, if any */
            int prevlen = 0;                   /* length of data in "prev" */
            int prevsize = 0;                  /* size of "prev" buffer */
            long cnt = 0;

            while (cnt < maxline || maxline < 0)
            {
                int readlen = (int)libC.fread(buf, 1, io_size, fd);

                /* This for loop processes what was read, but is also entered at end
                 * of file so that either:
                 * - an incomplete line gets written
                 * - a "binary" file gets an empty line at the end if it ends in a newline.
                 */
                Bytes p;                       /* position in "buf" */
                Bytes start;                   /* start of current line */
                for (p = buf, start = buf; BLT(p, buf.plus(readlen)) || (readlen <= 0 && (0 < prevlen || binary)); p = p.plus(1))
                {
                    if (p.at(0) == (byte)'\n' || readlen <= 0)
                    {
                        int len = BDIFF(p, start);

                        /* Finished a line.  Remove CRs before NL. */
                        if (0 < readlen && !binary)
                        {
                            while (0 < len && start.at(len - 1) == (byte)'\r')
                                --len;
                            /* removal may cross back to the "prev" string */
                            if (len == 0)
                                while (0 < prevlen && prev.at(prevlen - 1) == (byte)'\r')
                                    --prevlen;
                        }

                        Bytes s;
                        if (prevlen == 0)
                            s = STRNDUP(start, len);
                        else
                        {
                            s = new Bytes(prevlen + len + 1);

                            BCOPY(s, prev, prevlen);
                            BCOPY(s, prevlen, start, 0, len);
                            s.be(prevlen + len, NUL);

                            prev = null;
                            prevlen = prevsize = 0;
                        }

                        listitem_C li = new listitem_C();
                        li.li_tv.tv_type = VAR_STRING;
                        li.li_tv.tv_lock = 0;
                        li.li_tv.tv_string = s;
                        list_append(rtv.tv_list, li);

                        start = p.plus(1); /* step over newline */
                        if ((maxline <= ++cnt && 0 <= maxline) || readlen <= 0)
                            break;
                    }
                    else if (p.at(0) == NUL)
                    {
                        p.be(0, (byte)'\n');
                    }
                    /* Check for utf8 "bom"; U+FEFF is encoded as EF BB BF.
                     * Do this when finding the BF and check the previous two bytes. */
                    else if (char_u(p.at(0)) == 0xbf && !binary)
                    {
                        /* Find the two bytes before the 0xbf.
                         * If "p" is at "buf", or "buf + 1", these may be in the "prev" string. */
                        byte back1 = (BLE(buf.plus(1), p)) ? p.at(-1) : (1 <= prevlen) ? prev.at(prevlen - 1) : NUL;
                        byte back2 = (BLE(buf.plus(2), p)) ? p.at(-2) :
                                        (BEQ(p, buf.plus(1)) && 1 <= prevlen) ? prev.at(prevlen - 1) :
                                            (2 <= prevlen) ? prev.at(prevlen - 2) : NUL;

                        if (char_u(back2) == 0xef && char_u(back1) == 0xbb)
                        {
                            Bytes dest = p.minus(2);

                            /* Usually a BOM is at the beginning of a file, and so at
                             * the beginning of a line; then we can just step over it.
                             */
                            if (BEQ(start, dest))
                                start = p.plus(1);
                            else
                            {
                                /* have to shuffle "buf" to close gap */
                                int adjust_prevlen = 0;

                                if (BLT(dest, buf))
                                {
                                    adjust_prevlen = BDIFF(buf, dest); /* must be 1 or 2 */
                                    dest = buf;
                                }
                                if (BDIFF(p, buf) + 1 < readlen)
                                    BCOPY(dest, 0, p, 1, readlen - BDIFF(p, buf) - 1);
                                readlen -= 3 - adjust_prevlen;
                                prevlen -= adjust_prevlen;
                                p = dest.minus(1);
                            }
                        }
                    }
                }

                if ((maxline <= cnt && 0 <= maxline) || readlen <= 0)
                    break;

                if (BLT(start, p))
                {
                    /* There's part of a line in "buf", store it in "prev". */
                    if (prevsize <= BDIFF(p, start) + prevlen)
                    {
                        /* A common use case is ordinary text files and "prev" gets a fragment
                         * of a line, so the first allocation is made small, to avoid
                         * repeatedly 'allocing' large and 'reallocing' small. */
                        if (prevsize == 0)
                            prevsize = BDIFF(p, start);
                        else
                        {
                            int grow50pc = (prevsize * 3) / 2;
                            int growmin = BDIFF(p, start) * 2 + prevlen;
                            prevsize = (growmin < grow50pc) ? grow50pc : growmin;
                        }

                        /* need bigger "prev" buffer */
                        Bytes newprev = new Bytes(prevsize);
                        if (prev != null)
                            BCOPY(newprev, prev, prevlen);
                        prev = newprev;
                    }

                    /* Add the line part to end of "prev". */
                    BCOPY(prev, prevlen, start, 0, BDIFF(p, start));
                    prevlen += BDIFF(p, start);
                }
            }

            /*
             * For a negative line count use only the lines at the end of the file, free the rest.
             */
            if (maxline < 0)
                for ( ; -maxline < cnt; --cnt)
                    listitem_remove(rtv.tv_list, rtv.tv_list.lv_first);

            libc.fclose(fd);
        }
    };

    /*
     * Convert a List to proftime_t.
     * Return false when there is something wrong.
     */
    /*private*/ static boolean list2proftime(typval_C arg, timeval_C tm)
    {
        if (arg.tv_type != VAR_LIST || arg.tv_list == null || arg.tv_list.lv_len != 2)
            return false;

        boolean[] error = { false };

        long n1 = list_find_nr(arg.tv_list, 0L, error);
        long n2 = list_find_nr(arg.tv_list, 1L, error);
        tm.tv_sec(n1);
        tm.tv_usec(n2);

        return !error[0];
    }

    /*
     * "reltime()" function
     */
    /*private*/ static final f_func_C f_reltime = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            timeval_C res = new timeval_C();

            if (argvars[0].tv_type == VAR_UNKNOWN)
            {
                /* No arguments: get current time. */
                profile_start(res);
            }
            else if (argvars[1].tv_type == VAR_UNKNOWN)
            {
                if (list2proftime(argvars[0], res) == false)
                    return;
                profile_end(res);
            }
            else
            {
                timeval_C start = new timeval_C();
                /* Two arguments: compute the difference. */
                if (list2proftime(argvars[0], start) == false || list2proftime(argvars[1], res) == false)
                    return;
                profile_sub(res, start);
            }

            rettv_list_alloc(rtv);

            long n1 = res.tv_sec();
            long n2 = res.tv_usec();
            list_append_number(rtv.tv_list, n1);
            list_append_number(rtv.tv_list, n2);
        }
    };

    /*
     * "reltimestr()" function
     */
    /*private*/ static final f_func_C f_reltimestr = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_type = VAR_STRING;
            rtv.tv_string = null;

            timeval_C tm = new timeval_C();
            if (list2proftime(argvars[0], tm) == true)
                rtv.tv_string = STRDUP(profile_msg(tm));
        }
    };

    /*
     * "remove()" function
     */
    /*private*/ static final f_func_C f_remove = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            list_C l;
            Bytes arg_errmsg = u8("remove() argument");

            if (argvars[0].tv_type == VAR_DICT)
            {
                dict_C d;

                if (argvars[2].tv_type != VAR_UNKNOWN)
                    emsg2(e_toomanyarg, u8("remove()"));
                else if ((d = argvars[0].tv_dict) != null && !tv_check_lock(d.dv_lock, arg_errmsg))
                {
                    Bytes key = get_tv_string_chk(argvars[1]);
                    if (key != null)
                    {
                        dictitem_C di = dict_find(d, key, -1);
                        if (di == null)
                            emsg2(e_dictkey, key);
                        else
                        {
                            COPY_typval(rtv, di.di_tv);
                            ZER0_typval(di.di_tv);
                            dictitem_remove(d, di);
                        }
                    }
                }
            }
            else if (argvars[0].tv_type != VAR_LIST)
                emsg2(e_listdictarg, u8("remove()"));
            else if ((l = argvars[0].tv_list) != null && !tv_check_lock(l.lv_lock, arg_errmsg))
            {
                boolean[] error = { false };
                listitem_C item;

                long idx = get_tv_number_chk(argvars[1], error);
                if (error[0])
                    ;           /* type error: do nothing, errmsg already given */
                else if ((item = list_find(l, idx)) == null)
                    emsgn(e_listidx, idx);
                else
                {
                    if (argvars[2].tv_type == VAR_UNKNOWN)
                    {
                        /* Remove one item, return its value. */
                        vimlist_remove(l, item, item);
                        COPY_typval(rtv, item.li_tv);
                    }
                    else
                    {
                        listitem_C item2;

                        /* Remove range of items, return list with values. */
                        long end = get_tv_number_chk(argvars[2], error);
                        if (error[0])
                            ;           /* type error: do nothing */
                        else if ((item2 = list_find(l, end)) == null)
                            emsgn(e_listidx, end);
                        else
                        {
                            int cnt = 0;

                            listitem_C li;
                            for (li = item; li != null; li = li.li_next)
                            {
                                cnt++;
                                if (li == item2)
                                    break;
                            }
                            if (li == null)     /* didn't find "item2" after "item" */
                                emsg(e_invrange);
                            else
                            {
                                vimlist_remove(l, item, item2);

                                rettv_list_alloc(rtv);

                                l = rtv.tv_list;
                                l.lv_first = item;
                                l.lv_last = item2;
                                item.li_prev = null;
                                item2.li_next = null;
                                l.lv_len = cnt;
                            }
                        }
                    }
                }
            }
        }
    };

    /*
     * "repeat()" function
     */
    /*private*/ static final f_func_C f_repeat = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int n = (int)get_tv_number(argvars[1]);
            if (argvars[0].tv_type == VAR_LIST)
            {
                rettv_list_alloc(rtv);

                if (argvars[0].tv_list != null)
                    while (0 < n--)
                        if (list_extend(rtv.tv_list, argvars[0].tv_list, null) == false)
                            break;
            }
            else
            {
                Bytes p = get_tv_string(argvars[0]);
                rtv.tv_type = VAR_STRING;
                rtv.tv_string = null;

                int slen = strlen(p);
                int len = slen * n;
                if (len <= 0)
                    return;

                Bytes r = new Bytes(len + 1);

                for (int i = 0; i < n; i++)
                    BCOPY(r, i * slen, p, 0, slen);
                r.be(len, NUL);

                rtv.tv_string = r;
            }
        }
    };

    /*
     * "reverse({list})" function
     */
    /*private*/ static final f_func_C f_reverse = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            if (argvars[0].tv_type != VAR_LIST)
                emsg2(e_listarg, u8("reverse()"));
            else
            {
                list_C l = argvars[0].tv_list;

                if (l != null && !tv_check_lock(l.lv_lock, u8("reverse() argument")))
                {
                    listitem_C li = l.lv_last;
                    l.lv_first = l.lv_last = null;
                    l.lv_len = 0;
                    while (li != null)
                    {
                        listitem_C ni = li.li_prev;
                        list_append(l, li);
                        li = ni;
                    }
                    rtv.tv_list = l;
                    rtv.tv_type = VAR_LIST;
                    l.lv_refcount++;
                    l.lv_idx = l.lv_len - l.lv_idx - 1;
                }
            }
        }
    };

    /*private*/ static final int SP_NOMOVE       = 0x01;        /* don't move cursor */
    /*private*/ static final int SP_REPEAT       = 0x02;        /* repeat to find outer pair */
    /*private*/ static final int SP_RETCOUNT     = 0x04;        /* return matchcount */
    /*private*/ static final int SP_SETPCMARK    = 0x08;        /* set previous context mark */
    /*private*/ static final int SP_START        = 0x10;        /* accept match at start position */
    /*private*/ static final int SP_SUBPAT       = 0x20;        /* return nr of matching sub-pattern */
    /*private*/ static final int SP_END          = 0x40;        /* leave cursor at end of match */

    /*
     * Get flags for a search function.
     * Possibly sets "p_ws".
     * Returns BACKWARD, FORWARD or zero (for an error).
     */
    /*private*/ static int get_search_arg(typval_C varp, int[] flagsp)
    {
        int dir = FORWARD;

        if (varp.tv_type != VAR_UNKNOWN)
        {
            Bytes flags = get_tv_string_chk(varp);
            if (flags == null)
                return 0;           /* type error; errmsg already given */

            while (flags.at(0) != NUL)
            {
                switch (flags.at(0))
                {
                    case 'b': dir = BACKWARD; break;
                    case 'w': p_ws[0] = true; break;
                    case 'W': p_ws[0] = false; break;
                    default:
                    {
                        int mask = 0;
                        if (flagsp != null)
                            switch (flags.at(0))
                            {
                                case 'c': mask = SP_START; break;
                                case 'e': mask = SP_END; break;
                                case 'm': mask = SP_RETCOUNT; break;
                                case 'n': mask = SP_NOMOVE; break;
                                case 'p': mask = SP_SUBPAT; break;
                                case 'r': mask = SP_REPEAT; break;
                                case 's': mask = SP_SETPCMARK; break;
                            }
                        if (mask == 0)
                        {
                            emsg2(e_invarg2, flags);
                            dir = 0;
                        }
                        else
                            flagsp[0] |= mask;
                        break;
                    }
                }
                if (dir == 0)
                    break;
                flags = flags.plus(1);
            }
        }

        return dir;
    }

    /*
     * Shared by search() and searchpos() functions
     */
    /*private*/ static long search_cmn(typval_C[] argvars, pos_C match_pos, int[] flagsp)
    {
        long retval = 0;     /* default: FAIL */

        boolean save_p_ws = p_ws[0];
        long time_limit = 0;
        int options = SEARCH_KEEP;

        theend:
        {
            Bytes pat = get_tv_string(argvars[0]);
            int dir = get_search_arg(argvars[1], flagsp);   /* may set "p_ws" */
            if (dir == 0)
                break theend;

            int flags = flagsp[0];
            if ((flags & SP_START) != 0)
                options |= SEARCH_START;
            if ((flags & SP_END) != 0)
                options |= SEARCH_END;

            /* Optional arguments: line number to stop searching and timeout. */
            long lnum_stop = 0;
            if (argvars[1].tv_type != VAR_UNKNOWN && argvars[2].tv_type != VAR_UNKNOWN)
            {
                lnum_stop = get_tv_number_chk(argvars[2], null);
                if (lnum_stop < 0)
                    break theend;

                if (argvars[3].tv_type != VAR_UNKNOWN)
                {
                    time_limit = get_tv_number_chk(argvars[3], null);
                    if (time_limit < 0)
                        break theend;
                }
            }

            /* Set the time limit, if there is one. */
            timeval_C tm = new timeval_C();
            profile_setlimit(time_limit, tm);

            /*
             * This function does not accept SP_REPEAT and SP_RETCOUNT flags.
             * Check to make sure only those flags are set.
             * Also, Only the SP_NOMOVE or the SP_SETPCMARK flag can be set.
             * Both flags cannot be set.  Check for that condition also.
             */
            if (((flags & (SP_REPEAT | SP_RETCOUNT)) != 0) || ((flags & SP_NOMOVE) != 0 && (flags & SP_SETPCMARK) != 0))
            {
                emsg2(e_invarg2, get_tv_string(argvars[1]));
                break theend;
            }

            pos_C save_cursor = new pos_C();
            COPY_pos(save_cursor, curwin.w_cursor);
            pos_C pos = new pos_C();
            COPY_pos(pos, save_cursor);

            int subpatnum = searchit(curwin, curbuf, pos, dir, pat, 1L, options, RE_SEARCH, lnum_stop, tm);
            if (subpatnum != 0)
            {
                if ((flags & SP_SUBPAT) != 0)
                    retval = subpatnum;
                else
                    retval = pos.lnum;
                if ((flags & SP_SETPCMARK) != 0)
                    setpcmark();
                COPY_pos(curwin.w_cursor, pos);
                if (match_pos != null)
                {
                    /* Store the match cursor position. */
                    match_pos.lnum = pos.lnum;
                    match_pos.col = pos.col + 1;
                }
                /* "/$" will put the cursor after the end of the line, may need to correct that here */
                check_cursor();
            }

            /* If 'n' flag is used: restore cursor position. */
            if ((flags & SP_NOMOVE) != 0)
                COPY_pos(curwin.w_cursor, save_cursor);
            else
                curwin.w_set_curswant = true;
        }

        p_ws[0] = save_p_ws;

        return retval;
    }

    /*
     * "screenattr()" function
     */
    /*private*/ static final f_func_C f_screenattr = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int row = (int)get_tv_number_chk(argvars[0], null) - 1;
            int col = (int)get_tv_number_chk(argvars[1], null) - 1;

            int c;
            if (row < 0 || screenRows <= row || col < 0 || screenColumns <= col)
                c = -1;
            else
                c = screenAttrs[lineOffset[row] + col];
            rtv.tv_number = c;
        }
    };

    /*
     * "screenchar()" function
     */
    /*private*/ static final f_func_C f_screenchar = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int row = (int)get_tv_number_chk(argvars[0], null) - 1;
            int col = (int)get_tv_number_chk(argvars[1], null) - 1;

            int c;
            if (row < 0 || screenRows <= row || col < 0 || screenColumns <= col)
                c = -1;
            else
            {
                int off = lineOffset[row] + col;
                if (screenLinesUC[off] != 0)
                    c = screenLinesUC[off];
                else
                    c = screenLines.at(off);
            }
            rtv.tv_number = c;
        }
    };

    /*
     * "screencol()" function
     *
     * First column is 1 to be consistent with virtcol().
     */
    /*private*/ static final f_func_C f_screencol = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rtv.tv_number = screen_screencol() + 1;
        }
    };

    /*
     * "screenrow()" function
     */
    /*private*/ static final f_func_C f_screenrow = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rtv.tv_number = screen_screenrow() + 1;
        }
    };

    /*
     * "search()" function
     */
    /*private*/ static final f_func_C f_search = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int[] flags = { 0 };

            rtv.tv_number = search_cmn(argvars, null, flags);
        }
    };

    /*
     * "searchdecl()" function
     */
    /*private*/ static final f_func_C f_searchdecl = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = 1;        /* default: FAIL */

            boolean locally = true;
            boolean thisblock = false;
            boolean[] error = { false };

            Bytes name = get_tv_string_chk(argvars[0]);
            if (argvars[1].tv_type != VAR_UNKNOWN)
            {
                locally = (get_tv_number_chk(argvars[1], error) == 0);
                if (!error[0] && argvars[2].tv_type != VAR_UNKNOWN)
                    thisblock = (get_tv_number_chk(argvars[2], error) != 0);
            }
            if (!error[0] && name != null)
            {
                boolean found = find_decl(name, strlen(name), locally, thisblock, SEARCH_KEEP);
                rtv.tv_number = (found == false) ? 1 : 0;
            }
        }
    };

    /*
     * Used by searchpair() and searchpairpos()
     */
    /*private*/ static long searchpair_cmn(typval_C[] argvars, pos_C match_pos)
    {
        long retval = 0;            /* default: FAIL */

        boolean save_p_ws = p_ws[0];
        long lnum_stop = 0;
        long time_limit = 0;

        theend:
        {
            /* Get the three pattern arguments: start, middle, end. */
            Bytes spat = get_tv_string_chk(argvars[0]);
            Bytes mpat = get_tv_string_chk(argvars[1]);
            Bytes epat = get_tv_string_chk(argvars[2]);
            if (spat == null || mpat == null || epat == null)
                break theend;        /* type error */

            /* Handle the optional fourth argument: flags. */
            int[] flags = { 0 };
            int dir = get_search_arg(argvars[3], flags); /* may set "p_ws" */
            if (dir == 0)
                break theend;

            /* Don't accept SP_END or SP_SUBPAT.
             * Only one of the SP_NOMOVE or SP_SETPCMARK flags can be set.
             */
            if ((flags[0] & (SP_END | SP_SUBPAT)) != 0
                || ((flags[0] & SP_NOMOVE) != 0 && (flags[0] & SP_SETPCMARK) != 0))
            {
                emsg2(e_invarg2, get_tv_string(argvars[3]));
                break theend;
            }

            /* Using 'r' implies 'W', otherwise it doesn't work. */
            if ((flags[0] & SP_REPEAT) != 0)
                p_ws[0] = false;

            /* Optional fifth argument: skip expression. */
            Bytes skip;
            if (argvars[3].tv_type == VAR_UNKNOWN || argvars[4].tv_type == VAR_UNKNOWN)
                skip = u8("");
            else
            {
                skip = get_tv_string_chk(argvars[4]);
                if (argvars[5].tv_type != VAR_UNKNOWN)
                {
                    lnum_stop = get_tv_number_chk(argvars[5], null);
                    if (lnum_stop < 0)
                        break theend;
                    if (argvars[6].tv_type != VAR_UNKNOWN)
                    {
                        time_limit = get_tv_number_chk(argvars[6], null);
                        if (time_limit < 0)
                            break theend;
                    }
                }
            }
            if (skip == null)
                break theend;        /* type error */

            retval = do_searchpair(spat, mpat, epat, dir, skip, flags[0], match_pos, lnum_stop, time_limit);
        }

        p_ws[0] = save_p_ws;

        return retval;
    }

    /*
     * "searchpair()" function
     */
    /*private*/ static final f_func_C f_searchpair = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = searchpair_cmn(argvars, null);
        }
    };

    /*
     * "searchpairpos()" function
     */
    /*private*/ static final f_func_C f_searchpairpos = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rettv_list_alloc(rtv);

            long lnum = 0;
            int col = 0;

            pos_C match_pos = new pos_C();
            if (0 < searchpair_cmn(argvars, match_pos))
            {
                lnum = match_pos.lnum;
                col = match_pos.col;
            }

            list_append_number(rtv.tv_list, lnum);
            list_append_number(rtv.tv_list, col);
        }
    };

    /*
     * Search for a start/middle/end thing.
     * Used by searchpair(), see its documentation for the details.
     * Returns 0 or -1 for no match,
     */
    /*private*/ static long do_searchpair(Bytes spat, Bytes mpat, Bytes epat, int dir, Bytes skip, int flags, pos_C match_pos, long lnum_stop, long time_limit)
        /* spat: start pattern */
        /* mpat: middle pattern */
        /* epat: end pattern */
        /* dir: BACKWARD or FORWARD */
        /* skip: skip expression */
        /* flags: SP_SETPCMARK and other SP_ values */
        /* lnum_stop: stop at this line if not zero */
        /* time_limit: stop after this many msec */
    {
        long retval = 0;

        int nest = 1;
        int options = SEARCH_KEEP;

        /* Make 'cpoptions' empty, the 'l' flag should not be used here. */
        Bytes save_cpo = p_cpo[0];
        p_cpo[0] = EMPTY_OPTION;

        /* Set the time limit, if there is one. */
        timeval_C tm = new timeval_C();
        profile_setlimit(time_limit, tm);

        /* Make two search patterns: start/end (pat2, for in nested pairs)
         * and start/middle/end (pat3, for the top pair). */
        Bytes pat2 = new Bytes(strlen(spat) + strlen(epat) + 15);
        Bytes pat3 = new Bytes(strlen(spat) + strlen(mpat) + strlen(epat) + 23);

        libC.sprintf(pat2, u8("\\(%s\\m\\)\\|\\(%s\\m\\)"), spat, epat);
        if (mpat.at(0) == NUL)
            STRCPY(pat3, pat2);
        else
            libC.sprintf(pat3, u8("\\(%s\\m\\)\\|\\(%s\\m\\)\\|\\(%s\\m\\)"), spat, epat, mpat);
        if ((flags & SP_START) != 0)
            options |= SEARCH_START;

        pos_C save_cursor = new pos_C();
        COPY_pos(save_cursor, curwin.w_cursor);
        pos_C pos = new pos_C();
        COPY_pos(pos, curwin.w_cursor);

        pos_C firstpos = new pos_C();
        pos_C foundpos = new pos_C();

        for (Bytes pat = pat3; ; )
        {
            int n = searchit(curwin, curbuf, pos, dir, pat, 1L, options, RE_SEARCH, lnum_stop, tm);
            if (n == 0 || (firstpos.lnum != 0 && eqpos(pos, firstpos)))
                /* didn't find it or found the first match again: FAIL */
                break;

            if (firstpos.lnum == 0)
                COPY_pos(firstpos, pos);
            if (eqpos(pos, foundpos))
            {
                /* Found the same position again.  Can happen with a pattern that
                 * has "\zs" at the end and searching backwards.  Advance one
                 * character and try again. */
                if (dir == BACKWARD)
                    decl(pos);
                else
                    incl(pos);
            }
            COPY_pos(foundpos, pos);

            /* clear the start flag to avoid getting stuck here */
            options &= ~SEARCH_START;

            /* If the "skip" pattern matches, ignore this match. */
            if (skip.at(0) != NUL)
            {
                boolean result;
                boolean[] error = new boolean[1];

                pos_C save_pos = new pos_C();
                COPY_pos(save_pos, curwin.w_cursor);
                COPY_pos(curwin.w_cursor, pos);
                result = eval_to_bool(skip, error, null, false);
                COPY_pos(curwin.w_cursor, save_pos);
                if (error[0])
                {
                    /* Evaluating {skip} caused an error, break here. */
                    COPY_pos(curwin.w_cursor, save_cursor);
                    retval = -1;
                    break;
                }
                if (result)
                    continue;
            }

            if ((dir == BACKWARD && n == 3) || (dir == FORWARD && n == 2))
            {
                /* Found end when searching backwards or start when searching forward:
                 * nested pair. */
                nest++;
                pat = pat2;         /* nested, don't search for middle */
            }
            else
            {
                /* Found end when searching forward or start when searching backward:
                 * end of (nested) pair; or found middle in outer pair. */
                if (--nest == 1)
                    pat = pat3;     /* outer level, search for middle */
            }

            if (nest == 0)
            {
                /* Found the match: return matchcount or line number. */
                if ((flags & SP_RETCOUNT) != 0)
                    retval++;
                else
                    retval = pos.lnum;
                if ((flags & SP_SETPCMARK) != 0)
                    setpcmark();
                COPY_pos(curwin.w_cursor, pos);
                if ((flags & SP_REPEAT) == 0)
                    break;
                nest = 1;       /* search for next unmatched */
            }
        }

        if (match_pos != null)
        {
            /* Store the match cursor position. */
            match_pos.lnum = curwin.w_cursor.lnum;
            match_pos.col = curwin.w_cursor.col + 1;
        }

        /* If 'n' flag is used or search failed: restore cursor position. */
        if ((flags & SP_NOMOVE) != 0 || retval == 0)
            COPY_pos(curwin.w_cursor, save_cursor);

        if (p_cpo[0] == EMPTY_OPTION)
            p_cpo[0] = save_cpo;
        else
            ; /* Darn, evaluating {skip} expression changed the value. */

        return retval;
    }

    /*
     * "searchpos()" function
     */
    /*private*/ static final f_func_C f_searchpos = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rettv_list_alloc(rtv);

            long lnum = 0;
            int col = 0;
            int[] flags = { 0 };

            pos_C match_pos = new pos_C();
            long n = search_cmn(argvars, match_pos, flags);
            if (0 < n)
            {
                lnum = match_pos.lnum;
                col = match_pos.col;
            }

            list_append_number(rtv.tv_list, lnum);
            list_append_number(rtv.tv_list, col);
            if ((flags[0] & SP_SUBPAT) != 0)
                list_append_number(rtv.tv_list, n);
        }
    };

    /*
     * "setbufvar()" function
     */
    /*private*/ static final f_func_C f_setbufvar = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C _rettv)
        {
            if (check_restricted() || check_secure())
                return;

            get_tv_number(argvars[0]);      /* issue errmsg if type error */
            Bytes varname = get_tv_string_chk(argvars[1]);
            buffer_C buf = get_buf_tv(argvars[0], false);
            typval_C varp = argvars[2];

            if (buf != null && varname != null && varp != null)
            {
                /* set curbuf to be our buf, temporarily */
                aco_save_C aco = new aco_save_C();
                aucmd_prepbuf(aco, buf);

                if (varname.at(0) == (byte)'&')
                {
                    boolean[] error = { false };

                    varname = varname.plus(1);
                    long numval = get_tv_number_chk(varp, error);
                    Bytes strval = get_tv_string_chk(varp);
                    if (!error[0] && strval != null)
                        set_option_value(varname, numval, strval, OPT_LOCAL);
                }
                else
                {
                    Bytes bufvarname = new Bytes(strlen(varname) + 3);

                    STRCPY(bufvarname, u8("b:"));
                    STRCPY(bufvarname.plus(2), varname);
                    set_var(bufvarname, varp, true);
                }

                /* reset notion of buffer */
                aucmd_restbuf(aco);
            }
        }
    };

    /*
     * "setcmdpos()" function
     */
    /*private*/ static final f_func_C f_setcmdpos = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int pos = (int)get_tv_number(argvars[0]) - 1;
            if (0 <= pos)
                rtv.tv_number = set_cmdline_pos(pos);
        }
    };

    /*
     * "setline()" function
     */
    /*private*/ static final f_func_C f_setline = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes line = null;
            list_C l = null;
            listitem_C li = null;
            long added = 0;
            long lcount = curbuf.b_ml.ml_line_count;

            long lnum = get_tv_lnum(argvars[0]);
            if (argvars[1].tv_type == VAR_LIST)
            {
                l = argvars[1].tv_list;
                li = l.lv_first;
            }
            else
                line = get_tv_string_chk(argvars[1]);

            /* default result is zero == OK */
            for ( ; ; )
            {
                if (l != null)
                {
                    /* list argument, get next string */
                    if (li == null)
                        break;
                    line = get_tv_string_chk(li.li_tv);
                    li = li.li_next;
                }

                rtv.tv_number = 1;            /* FAIL */
                if (line == null || lnum < 1 || curbuf.b_ml.ml_line_count + 1 < lnum)
                    break;

                /* When coming here from Insert mode, sync undo, so that this can be
                 * undone separately from what was previously inserted. */
                if (u_sync_once == 2)
                {
                    u_sync_once = 1; /* notify that u_sync() was called */
                    u_sync(true);
                }

                if (lnum <= curbuf.b_ml.ml_line_count)
                {
                    /* existing line, replace it */
                    if (u_savesub(lnum) == true && ml_replace(lnum, line, true))
                    {
                        changed_bytes(lnum, 0);
                        if (lnum == curwin.w_cursor.lnum)
                            check_cursor_col();
                        rtv.tv_number = 0;    /* OK */
                    }
                }
                else if (0 < added || u_save(lnum - 1, lnum))
                {
                    /* lnum is one past the last line, append the line */
                    added++;
                    if (ml_append(lnum - 1, line, 0, false))
                        rtv.tv_number = 0;    /* OK */
                }

                if (l == null)                  /* only one string argument */
                    break;
                lnum++;
            }

            if (0 < added)
                appended_lines_mark(lcount, added);
        }
    };

    /*
     * "setmatches()" function
     */
    /*private*/ static final f_func_C f_setmatches = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = -1;

            if (argvars[0].tv_type != VAR_LIST)
            {
                emsg(e_listreq);
                return;
            }

            list_C l = argvars[0].tv_list;
            if (l != null)
            {
                /* To some extent make sure that we are dealing with a list from "getmatches()". */
                listitem_C li = l.lv_first;
                while (li != null)
                {
                    dict_C d;
                    if (li.li_tv.tv_type != VAR_DICT || (d = li.li_tv.tv_dict) == null)
                    {
                        emsg(e_invarg);
                        return;
                    }
                    if (!(dict_find(d, u8("group"), -1) != null
                    && dict_find(d, u8("pattern"), -1) != null
                    && dict_find(d, u8("priority"), -1) != null
                    && dict_find(d, u8("id"), -1) != null))
                    {
                        emsg(e_invarg);
                        return;
                    }
                    li = li.li_next;
                }

                clear_matches(curwin);
                li = l.lv_first;
                while (li != null)
                {
                    dict_C d = li.li_tv.tv_dict;
                    match_add(curwin, get_dict_string(d, u8("group"), false),
                            get_dict_string(d, u8("pattern"), false),
                            (int)get_dict_number(d, u8("priority")),
                            (int)get_dict_number(d, u8("id")), null);
                    li = li.li_next;
                }
                rtv.tv_number = 0;
            }
        }
    };

    /*
     * "setpos()" function
     */
    /*private*/ static final f_func_C f_setpos = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = -1;

            int[] curswant = { -1 };

            Bytes name = get_tv_string_chk(argvars[0]);
            if (name != null)
            {
                pos_C pos = new pos_C();
                int[] fnum = new int[1];
                if (list2fpos(argvars[1], pos, fnum, curswant) == true)
                {
                    if (--pos.col < 0)
                        pos.col = 0;
                    if (name.at(0) == (byte)'.' && name.at(1) == NUL)
                    {
                        /* set cursor */
                        if (fnum[0] == curbuf.b_fnum)
                        {
                            COPY_pos(curwin.w_cursor, pos);
                            if (0 <= curswant[0])
                                curwin.w_curswant = curswant[0] - 1;
                            check_cursor();
                            rtv.tv_number = 0;
                        }
                        else
                            emsg(e_invarg);
                    }
                    else if (name.at(0) == (byte)'\'' && name.at(1) != NUL && name.at(2) == NUL)
                    {
                        /* set mark */
                        if (setmark_pos(name.at(1), pos, fnum[0]) == true)
                            rtv.tv_number = 0;
                    }
                    else
                        emsg(e_invarg);
                }
            }
        }
    };

    /*
     * "setreg()" function
     */
    /*private*/ static final f_func_C f_setreg = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = 1;        /* FAIL is default */

            Bytes strregname = get_tv_string_chk(argvars[0]);
            if (strregname == null)
                return;                 /* type error; errmsg already given */

            byte regname = strregname.at(0);
            if (regname == NUL || regname == '@')
                regname = '"';

            int block_len = -1;
            byte yank_type = MAUTO;
            boolean append = false;

            if (argvars[2].tv_type != VAR_UNKNOWN)
            {
                Bytes opts = get_tv_string_chk(argvars[2]);
                if (opts == null)
                    return;             /* type error */

                for ( ; opts.at(0) != NUL; opts = opts.plus(1))
                    switch (opts.at(0))
                    {
                        case 'a': case 'A':     /* append */
                            append = true;
                            break;
                        case 'v': case 'c':     /* character-wise selection */
                            yank_type = MCHAR;
                            break;
                        case 'V': case 'l':     /* line-wise selection */
                            yank_type = MLINE;
                            break;
                        case 'b': case Ctrl_V:  /* block-wise selection */
                            yank_type = MBLOCK;
                            if (asc_isdigit(opts.at(1)))
                            {
                                opts = opts.plus(1);
                                { Bytes[] __ = { opts }; block_len = (int)getdigits(__) - 1; opts = __[0]; }
                                opts = opts.minus(1);
                            }
                            break;
                    }
            }

            if (argvars[1].tv_type == VAR_LIST)
            {
                int len = argvars[1].tv_list.lv_len;

                /* First half: use for pointers to result lines;
                 * second half: use for pointers to allocated copies. */
                Bytes[] lstval = new Bytes[(len + 1) * 2];

                int i = 0;
                for (listitem_C li = argvars[1].tv_list.lv_first; li != null; li = li.li_next)
                {
                    Bytes strval = get_tv_string_chk(li.li_tv);
                    if (strval == null)
                        return;

                    lstval[i++] = strval;
                }
                lstval[i++] = null;

                write_reg_contents_lst(regname, lstval, -1, append, yank_type, block_len);
            }
            else
            {
                Bytes strval = get_tv_string_chk(argvars[1]);
                if (strval == null)
                    return;

                write_reg_contents_ex(regname, strval, -1, append, yank_type, block_len);
            }

            rtv.tv_number = 0;
        }
    };

    /*
     * "settabvar()" function
     */
    /*private*/ static final f_func_C f_settabvar = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = 0;

            if (check_restricted() || check_secure())
                return;

            tabpage_C tp = find_tabpage((int)get_tv_number_chk(argvars[0], null));
            Bytes varname = get_tv_string_chk(argvars[1]);
            typval_C varp = argvars[2];

            if (varname != null && varp != null && tp != null)
            {
                tabpage_C save_curtab = curtab;
                goto_tabpage_tp(tp, false, false);

                Bytes tabvarname = new Bytes(strlen(varname) + 3);

                STRCPY(tabvarname, u8("t:"));
                STRCPY(tabvarname.plus(2), varname);
                set_var(tabvarname, varp, true);

                /* Restore current tabpage. */
                if (valid_tabpage(save_curtab))
                    goto_tabpage_tp(save_curtab, false, false);
            }
        }
    };

    /*
     * "setwinvar()" and "settabwinvar()" functions
     */
    /*private*/ static void setwinvar(typval_C[] argvars, typval_C _rettv, int off)
    {
        if (check_restricted() || check_secure())
            return;

        tabpage_C tp;
        if (off == 1)
            tp = find_tabpage((int)get_tv_number_chk(argvars[0], null));
        else
            tp = curtab;
        window_C win = find_win_by_nr(argvars[off], tp);
        Bytes varname = get_tv_string_chk(argvars[off + 1]);
        typval_C varp = argvars[off + 2];

        if (win != null && varname != null && varp != null)
        {
            window_C[] save_curwin = new window_C[1];
            tabpage_C[] save_curtab = new tabpage_C[1];
            if (switch_win(save_curwin, save_curtab, win, tp, true))
            {
                if (varname.at(0) == (byte)'&')
                {
                    boolean[] error = { false };

                    varname = varname.plus(1);
                    long numval = get_tv_number_chk(varp, error);

                    Bytes strval = get_tv_string_chk(varp);
                    if (!error[0] && strval != null)
                        set_option_value(varname, numval, strval, OPT_LOCAL);
                }
                else
                {
                    Bytes winvarname = new Bytes(strlen(varname) + 3);

                    STRCPY(winvarname, u8("w:"));
                    STRCPY(winvarname.plus(2), varname);
                    set_var(winvarname, varp, true);
                }
            }
            restore_win(save_curwin[0], save_curtab[0], true);
        }
    }

    /*
     * "settabwinvar()" function
     */
    /*private*/ static final f_func_C f_settabwinvar = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            setwinvar(argvars, rtv, 1);
        }
    };

    /*
     * "setwinvar()" function
     */
    /*private*/ static final f_func_C f_setwinvar = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            setwinvar(argvars, rtv, 0);
        }
    };

    /*
     * shiftwidth() function
     */
    /*private*/ static final f_func_C f_shiftwidth = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rtv.tv_number = get_sw_value(curbuf);
        }
    };

    /*
     * "simplify()" function
     */
    /*private*/ static final f_func_C f_simplify = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes p = get_tv_string(argvars[0]);
            rtv.tv_string = STRDUP(p);
            simplify_filename(rtv.tv_string);     /* simplify in place */
            rtv.tv_type = VAR_STRING;
        }
    };

    /* struct used in the array that's given to qsort() */
    /*private*/ static final class sortItem_C
    {
        listitem_C  item;
        int         idx;

        /*private*/ sortItem_C()
        {
        }
    }

    /*private*/ static sortItem_C[] ARRAY_sortItem(int n)
    {
        sortItem_C[] a = new sortItem_C[n];
        for (int i = 0; i < n; i++)
            a[i] = new sortItem_C();
        return a;
    }

    /*private*/ static boolean  item_compare_ic;
    /*private*/ static boolean  item_compare_numeric;
    /*private*/ static Bytes    item_compare_func;
    /*private*/ static dict_C   item_compare_selfdict;
    /*private*/ static boolean  item_compare_func_err;
    /*private*/ static boolean  item_compare_keep_zero;

    /*private*/ static final int ITEM_COMPARE_FAIL = 999;

    /*
     * Compare functions for f_sort() and f_uniq() below.
     */
    /*private*/ static int item__compare1(listitem_C li1, listitem_C li2)
    {
        typval_C tv1 = li1.li_tv;
        typval_C tv2 = li2.li_tv;

        /* tv2string() puts quotes around a string.  Don't do that for string variables.
         * Use a single quote when comparing with a non-string to do what the docs promise. */
        Bytes p1;
        if (tv1.tv_type == VAR_STRING)
            p1 = (tv2.tv_type != VAR_STRING || item_compare_numeric) ? u8("'") : tv1.tv_string;
        else
            p1 = tv2string(tv1, 0);
        if (p1 == null)
            p1 = u8("");

        Bytes p2;
        if (tv2.tv_type == VAR_STRING)
            p2 = (tv1.tv_type != VAR_STRING || item_compare_numeric) ? u8("'") : tv2.tv_string;
        else
            p2 = tv2string(tv2, 0);
        if (p2 == null)
            p2 = u8("");

        int cmp;

        if (!item_compare_numeric)
        {
            cmp = (item_compare_ic) ? STRCASECMP(p1, p2) : STRCMP(p1, p2);
        }
        else
        {
            long n1 = libC.atol(p1), n2 = libC.atol(p2);
            cmp = (n1 == n2) ? 0 : (n2 < n1) ? 1 : -1;
        }

        return cmp;
    }

    /*private*/ static final Comparator<sortItem_C> item_compare1 = new Comparator<sortItem_C>()
    {
        public int compare(sortItem_C si1, sortItem_C si2)
        {
            int cmp = item__compare1(si1.item, si2.item);

            /* When the result would be zero, compare the item indexes.  Makes the sort stable. */
            if (cmp == 0 && !item_compare_keep_zero)
                cmp = (si2.idx < si1.idx) ? 1 : -1;

            return cmp;
        }
    };

    /*private*/ static int item__compare2(listitem_C li1, listitem_C li2)
    {
        /* Copy the values.  This is needed to be able to set tv_lock to VAR_FIXED
         * in the copy without changing the original list items. */
        typval_C[] argv = ARRAY_typval(3);
        copy_tv(li1.li_tv, argv[0]);
        copy_tv(li2.li_tv, argv[1]);

        typval_C rtv = new typval_C();
        rtv.tv_type = VAR_UNKNOWN;    /* clear_tv() uses this */

        boolean[] doesrange = new boolean[1];
        int cmp = call_func(item_compare_func, strlen(item_compare_func), rtv, 2, argv,
                                     0L, 0L, doesrange, true, item_compare_selfdict) ? TRUE : FALSE;
        clear_tv(argv[1]);
        clear_tv(argv[0]);

        if (cmp == 0)
            cmp = ITEM_COMPARE_FAIL;
        else
        {
            boolean[] __ = { item_compare_func_err };
            cmp = (int)get_tv_number_chk(rtv, __);
            item_compare_func_err = __[0];
        }
        if (item_compare_func_err)
            cmp = ITEM_COMPARE_FAIL;    /* return value has wrong type */

        clear_tv(rtv);

        return cmp;
    }

    /*private*/ static final Comparator<sortItem_C> item_compare2 = new Comparator<sortItem_C>()
    {
        public int compare(sortItem_C si1, sortItem_C si2)
        {
            /* shortcut after failure in previous call; compare all items equal */
            if (item_compare_func_err)
                return 0;

            int cmp = item__compare2(si1.item, si2.item);

            /* When the result would be zero, compare the item indexes.  Makes the sort stable. */
            if (cmp == 0 && !item_compare_keep_zero)
                cmp = (si2.idx < si1.idx) ? 1 : -1;

            return cmp;
        }
    };

    /*
     * "sort({list})" function
     */
    /*private*/ static void do_sort_uniq(typval_C[] argvars, typval_C rtv, boolean sort)
    {
        if (argvars[0].tv_type != VAR_LIST)
        {
            emsg2(e_listarg, sort ? u8("sort()") : u8("uniq()"));
            return;
        }

        list_C list = argvars[0].tv_list;
        if (list == null || tv_check_lock(list.lv_lock, sort ? u8("sort() argument") : u8("uniq() argument")))
            return;

        rtv.tv_list = list;
        rtv.tv_type = VAR_LIST;
        list.lv_refcount++;

        int len = list_len(list);
        if (len <= 1)
            return;     /* short list sorts pretty quickly */

        item_compare_ic = false;
        item_compare_numeric = false;
        item_compare_func = null;
        item_compare_selfdict = null;
        if (argvars[1].tv_type != VAR_UNKNOWN)
        {
            /* optional second argument: {func} */
            if (argvars[1].tv_type == VAR_FUNC)
                item_compare_func = argvars[1].tv_string;
            else
            {
                boolean[] error = { false };

                long i = get_tv_number_chk(argvars[1], error);
                if (error[0])
                    return;             /* type error; errmsg already given */
                if (i == 1)
                    item_compare_ic = true;
                else
                    item_compare_func = get_tv_string(argvars[1]);
                if (item_compare_func != null)
                {
                    if (STRCMP(item_compare_func, u8("n")) == 0)
                    {
                        item_compare_func = null;
                        item_compare_numeric = true;
                    }
                    else if (STRCMP(item_compare_func, u8("i")) == 0)
                    {
                        item_compare_func = null;
                        item_compare_ic = true;
                    }
                }
            }

            if (argvars[2].tv_type != VAR_UNKNOWN)
            {
                /* optional third argument: {dict} */
                if (argvars[2].tv_type != VAR_DICT)
                {
                    emsg(e_dictreq);
                    return;
                }
                item_compare_selfdict = argvars[2].tv_dict;
            }
        }

        /* Make an array with each entry pointing to an item in the List. */
        sortItem_C[] ptrs = ARRAY_sortItem(len);

        if (sort)
        {
            int i = 0;
            /* sort(): ptrs will be the list to sort */
            for (listitem_C li = list.lv_first; li != null; li = li.li_next)
            {
                ptrs[i].item = li;
                ptrs[i].idx = i;
                i++;
            }

            item_compare_func_err = false;
            item_compare_keep_zero = false;
            /* test the compare function */
            if (item_compare_func != null && item_compare2.compare(ptrs[0], ptrs[1]) == ITEM_COMPARE_FAIL)
                emsg(u8("E702: Sort compare function failed"));
            else
            {
                /* Sort the array with item pointers. */
                Arrays.sort(ptrs, 0, len, (item_compare_func == null) ? item_compare1 : item_compare2);

                if (!item_compare_func_err)
                {
                    /* Clear the List and append the items in sorted order. */
                    list.lv_first = list.lv_last = list.lv_idx_item = null;
                    list.lv_len = 0;
                    for (i = 0; i < len; i++)
                        list_append(list, ptrs[i].item);
                }
            }
        }
        else
        {
            /* f_uniq(): ptrs will be a stack of items to remove */
            item_compare_func_err = false;
            item_compare_keep_zero = true;

            int i = 0;
            for (listitem_C li = list.lv_first; li != null && li.li_next != null; li = li.li_next)
            {
                int cmp = (item_compare_func == null) ? item__compare1(li, li.li_next) : item__compare2(li, li.li_next);
                if (cmp == 0)
                    ptrs[i++].item = li;
                if (item_compare_func_err)
                {
                    emsg(u8("E882: Uniq compare function failed"));
                    break;
                }
            }

            if (!item_compare_func_err)
            {
                while (0 <= --i)
                {
                    listitem_C li = ptrs[i].item.li_next;
                    ptrs[i].item.li_next = li.li_next;
                    if (li.li_next != null)
                        li.li_next.li_prev = ptrs[i].item;
                    else
                        list.lv_last = ptrs[i].item;
                    list_fix_watch(list, li);
                    listitem_free(li);
                    list.lv_len--;
                }
            }
        }
    }

    /*
     * "sort({list})" function
     */
    /*private*/ static final f_func_C f_sort = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            do_sort_uniq(argvars, rtv, true);
        }
    };

    /*
     * "uniq({list})" function
     */
    /*private*/ static final f_func_C f_uniq = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            do_sort_uniq(argvars, rtv, false);
        }
    };

    /*private*/ static final f_func_C f_split = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int col = 0;
            boolean keepempty = false;
            boolean[] typeerr = { false };

            /* Make 'cpoptions' empty, the 'l' flag should not be used here. */
            Bytes save_cpo = p_cpo[0];
            p_cpo[0] = u8("");

            Bytes pat = null;
            Bytes str = get_tv_string(argvars[0]);
            if (argvars[1].tv_type != VAR_UNKNOWN)
            {
                pat = get_tv_string_chk(argvars[1]);
                if (pat == null)
                    typeerr[0] = true;
                if (argvars[2].tv_type != VAR_UNKNOWN)
                    keepempty = (get_tv_number_chk(argvars[2], typeerr) != 0);
            }
            if (pat == null || pat.at(0) == NUL)
                pat = u8("[\\x01- ]\\+");

            rettv_list_alloc(rtv);
            if (typeerr[0])
                return;

            regmatch_C regmatch = new regmatch_C();
            regmatch.regprog = vim_regcomp(pat, RE_MAGIC + RE_STRING);
            if (regmatch.regprog != null)
            {
                regmatch.rm_ic = false;
                while (str.at(0) != NUL || keepempty)
                {
                    boolean match;
                    if (str.at(0) == NUL)
                        match = false;  /* empty item at the end */
                    else
                        match = vim_regexec_nl(regmatch, str, col);
                    Bytes end;
                    if (match)
                        end = regmatch.startp[0];
                    else
                        end = str.plus(strlen(str));
                    if (keepempty || BLT(str, end)
                        || (0 < rtv.tv_list.lv_len && str.at(0) != NUL && match && BLT(end, regmatch.endp[0])))
                    {
                        if (list_append_string(rtv.tv_list, str, BDIFF(end, str)) == false)
                            break;
                    }
                    if (!match)
                        break;
                    /* Advance to just after the match. */
                    if (BLT(str, regmatch.endp[0]))
                        col = 0;
                    else
                    {
                        /* Don't get stuck at the same match. */
                        col = us_ptr2len_cc(regmatch.endp[0]);
                    }
                    str = regmatch.endp[0];
                }
            }

            p_cpo[0] = save_cpo;
        }
    };

    /*
     * "str2nr()" function
     */
    /*private*/ static final f_func_C f_str2nr = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int base = 10;
            if (argvars[1].tv_type != VAR_UNKNOWN)
            {
                base = (int)get_tv_number(argvars[1]);
                if (base != 8 && base != 10 && base != 16)
                {
                    emsg(e_invarg);
                    return;
                }
            }

            Bytes p = skipwhite(get_tv_string(argvars[0]));
            if (p.at(0) == (byte)'+')
                p = skipwhite(p.plus(1));

            long[] n = new long[1];
            vim_str2nr(p, null, null, (base == 8) ? 2 : 0, (base == 16) ? 2 : 0, n);
            rtv.tv_number = n[0];
        }
    };

    /*
     * "strftime({format}[, {time}])" function
     */
    /*private*/ static final f_func_C f_strftime = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes result_buf = new Bytes(256);

            rtv.tv_type = VAR_STRING;

            Bytes p = get_tv_string(argvars[0]);

            long seconds;
            if (argvars[1].tv_type == VAR_UNKNOWN)
                seconds = libC._time();
            else
                seconds = get_tv_number(argvars[1]);

            tm_C curtime = libC._localtime(seconds);
            /* MSVC returns null for an invalid value of seconds. */
            if (curtime == null)
                rtv.tv_string = STRDUP(u8("(Invalid)"));
            else
            {
                if (p != null)
                    libC.strftime(result_buf, result_buf.size(), p, curtime);
                else
                    result_buf.be(0, NUL);

                rtv.tv_string = STRDUP(result_buf);
            }
        }
    };

    /*
     * "stridx()" function
     */
    /*private*/ static final f_func_C f_stridx = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = -1;

            Bytes needle = get_tv_string_chk(argvars[1]);
            Bytes save_haystack;
            Bytes haystack = save_haystack = get_tv_string_chk(argvars[0]);
            if (needle == null || haystack == null)
                return;         /* type error; errmsg already given */

            if (argvars[2].tv_type != VAR_UNKNOWN)
            {
                boolean[] error = { false };

                int start_idx = (int)get_tv_number_chk(argvars[2], error);
                if (error[0] || strlen(haystack) <= start_idx)
                    return;
                if (0 <= start_idx)
                    haystack = haystack.plus(start_idx);
            }

            Bytes pos = STRSTR(haystack, needle);
            if (pos != null)
                rtv.tv_number = BDIFF(pos, save_haystack);
        }
    };

    /*
     * "string()" function
     */
    /*private*/ static final f_func_C f_string = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_type = VAR_STRING;
            rtv.tv_string = tv2string(argvars[0], 0);
        }
    };

    /*
     * "strlen()" function
     */
    /*private*/ static final f_func_C f_strlen = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = strlen(get_tv_string(argvars[0]));
        }
    };

    /*
     * "strchars()" function
     */
    /*private*/ static final f_func_C f_strchars = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int len = 0;

            Bytes[] s = { get_tv_string(argvars[0]) };
            while (s[0].at(0) != NUL)
            {
                us_ptr2char_adv(s, false);
                len++;
            }

            rtv.tv_number = len;
        }
    };

    /*
     * "strdisplaywidth()" function
     */
    /*private*/ static final f_func_C f_strdisplaywidth = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes s = get_tv_string(argvars[0]);

            int col = 0;
            if (argvars[1].tv_type != VAR_UNKNOWN)
                col = (int)get_tv_number(argvars[1]);

            rtv.tv_number = linetabsize_col(s, col) - col;
        }
    };

    /*
     * "strwidth()" function
     */
    /*private*/ static final f_func_C f_strwidth = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes s = get_tv_string(argvars[0]);

            rtv.tv_number = us_string2cells(s, -1);
        }
    };

    /*
     * "strpart()" function
     */
    /*private*/ static final f_func_C f_strpart = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            boolean[] error = { false };

            Bytes p = get_tv_string(argvars[0]);
            int slen = strlen(p);

            int n = (int)get_tv_number_chk(argvars[1], error);
            int len;
            if (error[0])
                len = 0;
            else if (argvars[2].tv_type != VAR_UNKNOWN)
                len = (int)get_tv_number(argvars[2]);
            else
                len = slen - n;     /* default len: all bytes that are available. */

            /*
             * Only return the overlap between the specified part and the actual string.
             */
            if (n < 0)
            {
                len += n;
                n = 0;
            }
            else if (slen < n)
                n = slen;
            if (len < 0)
                len = 0;
            else if (slen < n + len)
                len = slen - n;

            rtv.tv_type = VAR_STRING;
            rtv.tv_string = STRNDUP(p.plus(n), len);
        }
    };

    /*
     * "strridx()" function
     */
    /*private*/ static final f_func_C f_strridx = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = -1;

            Bytes needle = get_tv_string_chk(argvars[1]);
            Bytes haystack = get_tv_string_chk(argvars[0]);
            if (needle == null || haystack == null)
                return;         /* type error; errmsg already given */

            int haystack_len = strlen(haystack);
            int end_idx;
            if (argvars[2].tv_type != VAR_UNKNOWN)
            {
                /* Third argument: upper limit for index. */
                end_idx = (int)get_tv_number_chk(argvars[2], null);
                if (end_idx < 0)
                    return;     /* can never find a match */
            }
            else
                end_idx = haystack_len;

            Bytes lastmatch = null;
            if (needle.at(0) == NUL)
            {
                /* Empty string matches past the end. */
                lastmatch = haystack.plus(end_idx);
            }
            else
            {
                for (Bytes rest = haystack; rest.at(0) != NUL; rest = rest.plus(1))
                {
                    rest = STRSTR(rest, needle);
                    if (rest == null || BLT(haystack.plus(end_idx), rest))
                        break;
                    lastmatch = rest;
                }
            }

            if (lastmatch == null)
                rtv.tv_number = -1;
            else
                rtv.tv_number = BDIFF(lastmatch, haystack);
        }
    };

    /*
     * "strtrans()" function
     */
    /*private*/ static final f_func_C f_strtrans = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_type = VAR_STRING;
            rtv.tv_string = transstr(get_tv_string(argvars[0]));
        }
    };

    /*
     * "submatch()" function
     */
    /*private*/ static final f_func_C f_submatch = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int retList = 0;

            boolean[] error = { false };
            int no = (int)get_tv_number_chk(argvars[0], error);
            if (error[0])
                return;

            error[0] = false;
            if (argvars[1].tv_type != VAR_UNKNOWN)
                retList = (int)get_tv_number_chk(argvars[1], error);
            if (error[0])
                return;

            if (retList == 0)
            {
                rtv.tv_type = VAR_STRING;
                rtv.tv_string = reg_submatch(no);
            }
            else
            {
                rtv.tv_type = VAR_LIST;
                rtv.tv_list = reg_submatch_list(no);
            }
        }
    };

    /*
     * "substitute()" function
     */
    /*private*/ static final f_func_C f_substitute = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes str = get_tv_string_chk(argvars[0]);
            Bytes pat = get_tv_string_chk(argvars[1]);
            Bytes sub = get_tv_string_chk(argvars[2]);
            Bytes flg = get_tv_string_chk(argvars[3]);

            rtv.tv_type = VAR_STRING;
            if (str == null || pat == null || sub == null || flg == null)
                rtv.tv_string = null;
            else
                rtv.tv_string = do_string_sub(str, pat, sub, flg);
        }
    };

    /*
     * "synID(lnum, col, trans)" function
     */
    /*private*/ static final f_func_C f_synID = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int id = 0;

            long lnum = get_tv_lnum(argvars[0]);            /* -1 on type error */
            int col = (int)(get_tv_number(argvars[1]) - 1);       /* -1 on type error */

            boolean[] error = { false };
            boolean trans = (get_tv_number_chk(argvars[2], error) != 0);

            if (!error[0] && 1 <= lnum && lnum <= curbuf.b_ml.ml_line_count
                        && 0 <= col && col < strlen(ml_get(lnum)))
                id = syn_get_id(curwin, lnum, col, trans, false);

            rtv.tv_number = id;
        }
    };

    /*
     * "synIDattr(id, what [, mode])" function
     */
    /*private*/ static final f_func_C f_synIDattr = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes p = null;
            Bytes mode;
            int modec;

            int id = (int)get_tv_number(argvars[0]);
            Bytes what = get_tv_string(argvars[1]);
            if (argvars[2].tv_type != VAR_UNKNOWN)
            {
                mode = get_tv_string(argvars[2]);
                modec = asc_tolower(mode.at(0));
                if (modec != 't' && modec != 'c' && modec != 'g')
                    modec = 0;                                          /* replace invalid with current */
            }
            else
            {
                if (1 < t_colors)
                    modec = 'c';
                else
                    modec = 't';
            }

            switch (asc_tolower(what.at(0)))
            {
                case 'b':
                    if (asc_tolower(what.at(1)) == 'g')                                /* bg[#] */
                        p = highlight_color(id, what, modec);
                    else                                                            /* bold */
                        p = highlight_has_attr(id, HL_BOLD, modec);
                    break;

                case 'f':                                                           /* fg[#] or font */
                    p = highlight_color(id, what, modec);
                    break;

                case 'i':
                    if (asc_tolower(what.at(1)) == 'n')                                /* inverse */
                        p = highlight_has_attr(id, HL_INVERSE, modec);
                    else                                                            /* italic */
                        p = highlight_has_attr(id, HL_ITALIC, modec);
                    break;

                case 'n':                                                           /* name */
                    p = get_highlight_name.expand(null, id - 1);
                    break;

                case 'r':                                                           /* reverse */
                    p = highlight_has_attr(id, HL_INVERSE, modec);
                    break;

                case 's':
                    if (asc_tolower(what.at(1)) == 'p')                                /* sp[#] */
                        p = highlight_color(id, what, modec);
                    else                                                            /* standout */
                        p = highlight_has_attr(id, HL_STANDOUT, modec);
                    break;

                case 'u':
                    if (strlen(what) <= 5 || asc_tolower(what.at(5)) != 'c')           /* underline */
                        p = highlight_has_attr(id, HL_UNDERLINE, modec);
                    else                                                            /* undercurl */
                        p = highlight_has_attr(id, HL_UNDERCURL, modec);
                    break;
            }

            if (p != null)
                p = STRDUP(p);
            rtv.tv_type = VAR_STRING;
            rtv.tv_string = p;
        }
    };

    /*
     * "synIDtrans(id)" function
     */
    /*private*/ static final f_func_C f_synIDtrans = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int id = (int)get_tv_number(argvars[0]);

            if (0 < id)
                id = syn_get_final_id(id);
            else
                id = 0;

            rtv.tv_number = id;
        }
    };

    /*
     * "synconcealed(lnum, col)" function
     */
    /*private*/ static final f_func_C f_synconcealed = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_type = VAR_LIST;
            rtv.tv_list = null;

            long lnum = get_tv_lnum(argvars[0]);            /* -1 on type error */
            int col = (int)(get_tv_number(argvars[1]) - 1);       /* -1 on type error */

            Bytes str = new Bytes(NUMBUFLEN);

            rettv_list_alloc(rtv);

            int syntax_flags = 0;
            int[] matchid = { 0 };

            if (1 <= lnum && lnum <= curbuf.b_ml.ml_line_count
                && 0 <= col && col <= strlen(ml_get(lnum))
                && 0 < curwin.w_onebuf_opt.wo_cole[0])
            {
                syn_get_id(curwin, lnum, col, false, false);
                syntax_flags = get_syntax_info(matchid);

                /* get the conceal character */
                if ((syntax_flags & HL_CONCEAL) != 0 && curwin.w_onebuf_opt.wo_cole[0] < 3)
                {
                    int cchar = syn_get_sub_char();
                    if (cchar == NUL && curwin.w_onebuf_opt.wo_cole[0] == 1 && lcs_conceal[0] != NUL)
                        cchar = lcs_conceal[0];
                    if (cchar != NUL)
                        utf_char2bytes(cchar, str);
                }
            }

            list_append_number(rtv.tv_list, ((syntax_flags & HL_CONCEAL) != 0) ? TRUE : FALSE);
            /* -1 to auto-determine strlen */
            list_append_string(rtv.tv_list, str, -1);
            list_append_number(rtv.tv_list, matchid[0]);
        }
    };

    /*
     * "synstack(lnum, col)" function
     */
    /*private*/ static final f_func_C f_synstack = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_type = VAR_LIST;
            rtv.tv_list = null;

            long lnum = get_tv_lnum(argvars[0]);            /* -1 on type error */
            int col = (int)(get_tv_number(argvars[1]) - 1);       /* -1 on type error */

            if (1 <= lnum && lnum <= curbuf.b_ml.ml_line_count
                && 0 <= col && col <= strlen(ml_get(lnum)))
            {
                rettv_list_alloc(rtv);

                syn_get_id(curwin, lnum, col, false, true);

                for (int i = 0; ; i++)
                {
                    int id = syn_get_stack_item(i);
                    if (id < 0)
                        break;
                    list_append_number(rtv.tv_list, id);
                }
            }
        }
    };

    /*
     * "tabpagebuflist()" function
     */
    /*private*/ static final f_func_C f_tabpagebuflist = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            window_C wp = null;

            if (argvars[0].tv_type == VAR_UNKNOWN)
                wp = firstwin;
            else
            {
                tabpage_C tp = find_tabpage((int)get_tv_number(argvars[0]));
                if (tp != null)
                    wp = (tp == curtab) ? firstwin : tp.tp_firstwin;
            }

            if (wp != null)
            {
                rettv_list_alloc(rtv);
                for ( ; wp != null; wp = wp.w_next)
                    list_append_number(rtv.tv_list, wp.w_buffer.b_fnum);
            }
        }
    };

    /*
     * "tabpagenr()" function
     */
    /*private*/ static final f_func_C f_tabpagenr = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int nr = 0;

            if (argvars[0].tv_type != VAR_UNKNOWN)
            {
                Bytes arg = get_tv_string_chk(argvars[0]);
                if (arg != null)
                {
                    if (STRCMP(arg, u8("$")) == 0)
                        nr = tabpage_index(null) - 1;
                    else
                        emsg2(e_invexpr2, arg);
                }
            }
            else
                nr = tabpage_index(curtab);

            rtv.tv_number = nr;
        }
    };

    /*
     * Common code for tabpagewinnr() and winnr().
     */
    /*private*/ static int get_winnr(tabpage_C tp, typval_C argvar)
    {
        int nr = 1;

        window_C twin = (tp == curtab) ? curwin : tp.tp_curwin;
        if (argvar.tv_type != VAR_UNKNOWN)
        {
            Bytes arg = get_tv_string_chk(argvar);
            if (arg == null)
                nr = 0;                                 /* type error; errmsg already given */
            else if (STRCMP(arg, u8("$")) == 0)
                twin = (tp == curtab) ? lastwin : tp.tp_lastwin;
            else if (STRCMP(arg, u8("#")) == 0)
            {
                twin = (tp == curtab) ? prevwin : tp.tp_prevwin;
                if (twin == null)
                    nr = 0;
            }
            else
            {
                emsg2(e_invexpr2, arg);
                nr = 0;
            }
        }

        if (0 < nr)
            for (window_C wp = (tp == curtab) ? firstwin : tp.tp_firstwin; wp != twin; wp = wp.w_next)
            {
                if (wp == null)
                {
                    /* didn't find it in this tabpage */
                    nr = 0;
                    break;
                }
                nr++;
            }

        return nr;
    }

    /*
     * "tabpagewinnr()" function
     */
    /*private*/ static final f_func_C f_tabpagewinnr = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int nr = 0;

            tabpage_C tp = find_tabpage((int)get_tv_number(argvars[0]));
            if (tp != null)
                nr = get_winnr(tp, argvars[1]);

            rtv.tv_number = nr;
        }
    };

    /*
     * "tolower(string)" function
     */
    /*private*/ static final f_func_C f_tolower = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes p = STRDUP(get_tv_string(argvars[0]));
            rtv.tv_type = VAR_STRING;
            rtv.tv_string = p;

            if (p != null)
                while (p.at(0) != NUL)
                {
                    int c = us_ptr2char(p);
                    int lc = utf_tolower(c);
                    int l = us_ptr2len(p);

                    /* TODO: reallocate string when byte count changes. */
                    if (utf_char2len(lc) == l)
                        utf_char2bytes(lc, p);
                    p = p.plus(l);
                }
        }
    };

    /*
     * "toupper(string)" function
     */
    /*private*/ static final f_func_C f_toupper = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_type = VAR_STRING;
            rtv.tv_string = strup_save(get_tv_string(argvars[0]));
        }
    };

    /*
     * "tr(string, fromstr, tostr)" function
     */
    /*private*/ static final f_func_C f_tr = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes in_str = get_tv_string(argvars[0]);
            Bytes fromstr = get_tv_string_chk(argvars[1]);
            Bytes tostr = get_tv_string_chk(argvars[2]);

            /* Default return value: empty string. */
            rtv.tv_type = VAR_STRING;
            rtv.tv_string = null;
            if (fromstr == null || tostr == null)
                return;             /* type error; errmsg already given */

            barray_C ba = new barray_C(80);

            boolean first = true;

            /* "fromstr" and "tostr" have to contain the same number of chars */
            while (in_str.at(0) != NUL)
            {
                int inlen = us_ptr2len_cc(in_str);
                Bytes cpstr = in_str;
                int cplen = inlen;
                int idx = 0;

                int fromlen;
                for (Bytes p = fromstr; p.at(0) != NUL; p = p.plus(fromlen))
                {
                    fromlen = us_ptr2len_cc(p);
                    if (fromlen == inlen && STRNCMP(in_str, p, inlen) == 0)
                    {
                        int tolen;
                        for (p = tostr; p.at(0) != NUL; p = p.plus(tolen))
                        {
                            tolen = us_ptr2len_cc(p);
                            if (idx-- == 0)
                            {
                                cplen = tolen;
                                cpstr = p;
                                break;
                            }
                        }
                        if (p.at(0) == NUL)      /* "tostr" is shorter than "fromstr" */
                        {
                            emsg2(e_invarg2, fromstr);
                            ba_clear(ba);
                            return;
                        }
                        break;
                    }
                    idx++;
                }

                if (first && BEQ(cpstr, in_str))
                {
                    /* Check that 'fromstr' and 'tostr' have the same number of (multi-byte) characters.
                     * Done only once when a character of 'in_str' doesn't appear in 'fromstr'. */
                    first = false;
                    int tolen;
                    for (Bytes p = tostr; p.at(0) != NUL; p = p.plus(tolen))
                    {
                        tolen = us_ptr2len_cc(p);
                        --idx;
                    }
                    if (idx != 0)
                    {
                        emsg2(e_invarg2, fromstr);
                        ba_clear(ba);
                        return;
                    }
                }

                ba_grow(ba, cplen);
                ACOPY(ba.ba_data, ba.ba_len, cpstr.array, cpstr.index, cplen);
                ba.ba_len += cplen;

                in_str = in_str.plus(inlen);
            }

            /* add a terminating NUL */
            ba_grow(ba, 1);
            ba_append(ba, NUL);

            rtv.tv_string = new Bytes(ba.ba_data);
        }
    };

    /*
     * "type(expr)" function
     */
    /*private*/ static final f_func_C f_type = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int n;

            switch (argvars[0].tv_type)
            {
                case VAR_NUMBER: n = 0; break;
                case VAR_STRING: n = 1; break;
                case VAR_FUNC:   n = 2; break;
                case VAR_LIST:   n = 3; break;
                case VAR_DICT:   n = 4; break;
                default: emsg2(e_intern2, u8("f_type()")); n = 0; break;
            }

            rtv.tv_number = n;
        }
    };

    /*
     * "undofile(name)" function
     */
    /*private*/ static final f_func_C f_undofile = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_type = VAR_STRING;

            Bytes fname = get_tv_string(argvars[0]);
            if (fname.at(0) == NUL)
            {
                /* If there is no file name there will be no undo file. */
                rtv.tv_string = null;
            }
            else
            {
                Bytes ffname = fullName_save(fname, false);
                if (ffname != null)
                    rtv.tv_string = u_get_undo_file_name(ffname, false);
            }
        }
    };

    /*
     * "undotree()" function
     */
    /*private*/ static final f_func_C f_undotree = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rettv_dict_alloc(rtv);

            dict_C dict = rtv.tv_dict;

            dict_add_nr_str(dict, u8("synced"), curbuf.b_u_synced ? TRUE : FALSE, null);
            dict_add_nr_str(dict, u8("seq_last"), curbuf.b_u_seq_last, null);
            dict_add_nr_str(dict, u8("save_last"), curbuf.b_u_save_nr_last, null);
            dict_add_nr_str(dict, u8("seq_cur"), curbuf.b_u_seq_cur, null);
            dict_add_nr_str(dict, u8("time_cur"), curbuf.b_u_time_cur, null);
            dict_add_nr_str(dict, u8("save_cur"), curbuf.b_u_save_nr_cur, null);

            list_C list = new list_C();

            u_eval_tree(curbuf.b_u_oldhead, list);
            dict_add_list(dict, u8("entries"), list);
        }
    };

    /*
     * "values(dict)" function
     */
    /*private*/ static final f_func_C f_values = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            dict_list(argvars[0], rtv, 1);
        }
    };

    /*
     * "virtcol(string)" function
     */
    /*private*/ static final f_func_C f_virtcol = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            int[] vcol = { 0 };

            int[] fnum = { curbuf.b_fnum };
            pos_C fp = var2fpos(argvars[0], false, fnum);
            if (fp != null && fp.lnum <= curbuf.b_ml.ml_line_count && fnum[0] == curbuf.b_fnum)
            {
                getvvcol(curwin, fp, null, null, vcol);
                vcol[0]++;
            }

            rtv.tv_number = vcol[0];
        }
    };

    /*
     * "visualmode()" function
     */
    /*private*/ static final f_func_C f_visualmode = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            Bytes str = new Bytes(2);

            rtv.tv_type = VAR_STRING;
            str.be(0, curbuf.b_visual_mode_eval);
            str.be(1, NUL);
            rtv.tv_string = STRDUP(str);

            /* A non-zero number or non-empty string argument: reset mode. */
            if (non_zero_arg(argvars[0]))
                curbuf.b_visual_mode_eval = NUL;
        }
    };

    /*
     * "winbufnr(nr)" function
     */
    /*private*/ static final f_func_C f_winbufnr = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            window_C wp = find_win_by_nr(argvars[0], null);

            if (wp == null)
                rtv.tv_number = -1;
            else
                rtv.tv_number = wp.w_buffer.b_fnum;
        }
    };

    /*
     * "wincol()" function
     */
    /*private*/ static final f_func_C f_wincol = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            validate_cursor();

            rtv.tv_number = curwin.w_wcol + 1;
        }
    };

    /*
     * "winheight(nr)" function
     */
    /*private*/ static final f_func_C f_winheight = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            window_C wp = find_win_by_nr(argvars[0], null);

            if (wp == null)
                rtv.tv_number = -1;
            else
                rtv.tv_number = wp.w_height;
        }
    };

    /*
     * "winline()" function
     */
    /*private*/ static final f_func_C f_winline = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            validate_cursor();
            rtv.tv_number = curwin.w_wrow + 1;
        }
    };

    /*
     * "winnr()" function
     */
    /*private*/ static final f_func_C f_winnr = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = get_winnr(curtab, argvars[0]);
        }
    };

    /*
     * "winrestcmd()" function
     */
    /*private*/ static final f_func_C f_winrestcmd = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            barray_C ba = new barray_C(70);

            int winnr = 1;
            for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            {
                Bytes buf = new Bytes(50);
                libC.sprintf(buf, u8("%dresize %d|"), winnr, wp.w_height);
                ba_concat(ba, buf);
                libC.sprintf(buf, u8("vert %dresize %d|"), winnr, wp.w_width);
                ba_concat(ba, buf);
                winnr++;
            }
            ba_append(ba, NUL);

            rtv.tv_string = new Bytes(ba.ba_data);
            rtv.tv_type = VAR_STRING;
        }
    };

    /*
     * "winrestview()" function
     */
    /*private*/ static final f_func_C f_winrestview = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C _rettv)
        {
            dict_C dict;

            if (argvars[0].tv_type != VAR_DICT || (dict = argvars[0].tv_dict) == null)
                emsg(e_invarg);
            else
            {
                if (dict_find(dict, u8("lnum"), -1) != null)
                    curwin.w_cursor.lnum = get_dict_number(dict, u8("lnum"));
                if (dict_find(dict, u8("col"), -1) != null)
                    curwin.w_cursor.col = (int)get_dict_number(dict, u8("col"));
                if (dict_find(dict, u8("coladd"), -1) != null)
                    curwin.w_cursor.coladd = (int)get_dict_number(dict, u8("coladd"));
                if (dict_find(dict, u8("curswant"), -1) != null)
                {
                    curwin.w_curswant = (int)get_dict_number(dict, u8("curswant"));
                    curwin.w_set_curswant = false;
                }

                if (dict_find(dict, u8("topline"), -1) != null)
                    set_topline(curwin, get_dict_number(dict, u8("topline")));
                if (dict_find(dict, u8("leftcol"), -1) != null)
                    curwin.w_leftcol = (int)get_dict_number(dict, u8("leftcol"));
                if (dict_find(dict, u8("skipcol"), -1) != null)
                    curwin.w_skipcol = (int)get_dict_number(dict, u8("skipcol"));

                check_cursor();
                win_new_height(curwin, curwin.w_height);
                win_new_width(curwin, curwin.w_width);
                changed_window_setting();

                if (curwin.w_topline <= 0)
                    curwin.w_topline = 1;
                if (curwin.w_topline > curbuf.b_ml.ml_line_count)
                    curwin.w_topline = curbuf.b_ml.ml_line_count;
            }
        }
    };

    /*
     * "winsaveview()" function
     */
    /*private*/ static final f_func_C f_winsaveview = new f_func_C()
    {
        public void fun(typval_C[] _argvars, typval_C rtv)
        {
            rettv_dict_alloc(rtv);

            dict_C dict = rtv.tv_dict;

            dict_add_nr_str(dict, u8("lnum"), curwin.w_cursor.lnum, null);
            dict_add_nr_str(dict, u8("col"), (long)curwin.w_cursor.col, null);
            dict_add_nr_str(dict, u8("coladd"), (long)curwin.w_cursor.coladd, null);
            update_curswant();
            dict_add_nr_str(dict, u8("curswant"), (long)curwin.w_curswant, null);

            dict_add_nr_str(dict, u8("topline"), curwin.w_topline, null);
            dict_add_nr_str(dict, u8("leftcol"), (long)curwin.w_leftcol, null);
            dict_add_nr_str(dict, u8("skipcol"), (long)curwin.w_skipcol, null);
        }
    };

    /*
     * "winwidth(nr)" function
     */
    /*private*/ static final f_func_C f_winwidth = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            window_C wp = find_win_by_nr(argvars[0], null);

            if (wp == null)
                rtv.tv_number = -1;
            else
                rtv.tv_number = wp.w_width;
        }
    };

    /*
     * Write list of strings to file
     */
    /*private*/ static boolean write_list(file_C fd, list_C list, boolean binary)
    {
        boolean ret = true;

        for (listitem_C li = list.lv_first; li != null; li = li.li_next)
        {
            for (Bytes s = get_tv_string(li.li_tv); s.at(0) != NUL; s = s.plus(1))
            {
                int c;
                if (s.at(0) == (byte)'\n')
                    c = libc.putc(NUL, fd);
                else
                    c = libc.putc(s.at(0), fd);
                if (c == EOF)
                {
                    ret = false;
                    break;
                }
            }
            if (!binary || li.li_next != null)
                if (libc.putc('\n', fd) == EOF)
                {
                    ret = false;
                    break;
                }
            if (ret == false)
            {
                emsg(e_write);
                break;
            }
        }

        return ret;
    }

    /*
     * "writefile()" function
     */
    /*private*/ static final f_func_C f_writefile = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            if (check_restricted() || check_secure())
                return;

            if (argvars[0].tv_type != VAR_LIST)
            {
                emsg2(e_listarg, u8("writefile()"));
                return;
            }
            if (argvars[0].tv_list == null)
                return;

            boolean binary = false;
            boolean append = false;

            if (argvars[2].tv_type != VAR_UNKNOWN)
            {
                if (vim_strchr(get_tv_string(argvars[2]), 'b') != null)
                    binary = true;
                if (vim_strchr(get_tv_string(argvars[2]), 'a') != null)
                    append = true;
            }

            int ret = 0;

            /* Always open the file in binary mode,
             * library functions have a mind of their own about CR-LF conversion. */
            Bytes fname = get_tv_string(argvars[1]);
            file_C fd;
            if (fname.at(0) == NUL || (fd = libC.fopen(fname, append ? u8("a") : u8("w"))) == null)
            {
                emsg2(e_notcreate, (fname.at(0) == NUL) ? u8("<empty>") : fname);
                ret = -1;
            }
            else
            {
                if (write_list(fd, argvars[0].tv_list, binary) == false)
                    ret = -1;
                libc.fclose(fd);
            }

            rtv.tv_number = ret;
        }
    };

    /*
     * "xor(expr, expr)" function
     */
    /*private*/ static final f_func_C f_xor = new f_func_C()
    {
        public void fun(typval_C[] argvars, typval_C rtv)
        {
            rtv.tv_number = get_tv_number_chk(argvars[0], null) ^ get_tv_number_chk(argvars[1], null);
        }
    };

    /*
     * Array with names and number of arguments of all internal functions
     * MUST BE KEPT SORTED IN strcmp() ORDER FOR BINARY SEARCH!
     */
    /*private*/ static fst_C[] functions = new fst_C[]
    {
        new fst_C(u8("abs"),             1,  1, f_abs            ),
        new fst_C(u8("add"),             2,  2, f_add            ),
        new fst_C(u8("and"),             2,  2, f_and            ),
        new fst_C(u8("append"),          2,  2, f_append         ),
        new fst_C(u8("argc"),            0,  0, f_argc           ),
        new fst_C(u8("argidx"),          0,  0, f_argidx         ),
        new fst_C(u8("arglistid"),       0,  2, f_arglistid      ),
        new fst_C(u8("argv"),            0,  1, f_argv           ),
        new fst_C(u8("bufexists"),       1,  1, f_bufexists      ),
        new fst_C(u8("buflisted"),       1,  1, f_buflisted      ),
        new fst_C(u8("bufloaded"),       1,  1, f_bufloaded      ),
        new fst_C(u8("bufname"),         1,  1, f_bufname        ),
        new fst_C(u8("bufnr"),           1,  2, f_bufnr          ),
        new fst_C(u8("bufwinnr"),        1,  1, f_bufwinnr       ),
        new fst_C(u8("byte2line"),       1,  1, f_byte2line      ),
        new fst_C(u8("byteidx"),         2,  2, f_byteidx        ),
        new fst_C(u8("byteidxcomp"),     2,  2, f_byteidxcomp    ),
        new fst_C(u8("call"),            2,  3, f_call           ),
        new fst_C(u8("changenr"),        0,  0, f_changenr       ),
        new fst_C(u8("char2nr"),         1,  2, f_char2nr        ),
        new fst_C(u8("cindent"),         1,  1, f_cindent        ),
        new fst_C(u8("clearmatches"),    0,  0, f_clearmatches   ),
        new fst_C(u8("col"),             1,  1, f_col            ),
        new fst_C(u8("confirm"),         1,  4, f_confirm        ),
        new fst_C(u8("copy"),            1,  1, f_copy           ),
        new fst_C(u8("count"),           2,  4, f_count          ),
        new fst_C(u8("cursor"),          1,  3, f_cursor         ),
        new fst_C(u8("deepcopy"),        1,  2, f_deepcopy       ),
        new fst_C(u8("did_filetype"),    0,  0, f_did_filetype   ),
        new fst_C(u8("empty"),           1,  1, f_empty          ),
        new fst_C(u8("escape"),          2,  2, f_escape         ),
        new fst_C(u8("eval"),            1,  1, f_eval           ),
        new fst_C(u8("eventhandler"),    0,  0, f_eventhandler   ),
        new fst_C(u8("exists"),          1,  1, f_exists         ),
        new fst_C(u8("extend"),          2,  3, f_extend         ),
        new fst_C(u8("feedkeys"),        1,  2, f_feedkeys       ),
        new fst_C(u8("filter"),          2,  2, f_filter         ),
        new fst_C(u8("function"),        1,  1, f_function       ),
        new fst_C(u8("get"),             2,  3, f_get            ),
        new fst_C(u8("getbufline"),      2,  3, f_getbufline     ),
        new fst_C(u8("getbufvar"),       2,  3, f_getbufvar      ),
        new fst_C(u8("getchar"),         0,  1, f_getchar        ),
        new fst_C(u8("getcharmod"),      0,  0, f_getcharmod     ),
        new fst_C(u8("getcmdline"),      0,  0, f_getcmdline     ),
        new fst_C(u8("getcmdpos"),       0,  0, f_getcmdpos      ),
        new fst_C(u8("getcmdtype"),      0,  0, f_getcmdtype     ),
        new fst_C(u8("getcmdwintype"),   0,  0, f_getcmdwintype  ),
        new fst_C(u8("getcurpos"),       0,  0, f_getcurpos      ),
        new fst_C(u8("getline"),         1,  2, f_getline        ),
        new fst_C(u8("getmatches"),      0,  0, f_getmatches     ),
        new fst_C(u8("getpos"),          1,  1, f_getpos         ),
        new fst_C(u8("getreg"),          0,  3, f_getreg         ),
        new fst_C(u8("getregtype"),      0,  1, f_getregtype     ),
        new fst_C(u8("gettabvar"),       2,  3, f_gettabvar      ),
        new fst_C(u8("gettabwinvar"),    3,  4, f_gettabwinvar   ),
        new fst_C(u8("getwinvar"),       2,  3, f_getwinvar      ),
        new fst_C(u8("glob2regpat"),     1,  1, f_glob2regpat    ),
        new fst_C(u8("has_key"),         2,  2, f_has_key        ),
        new fst_C(u8("hasmapto"),        1,  3, f_hasmapto       ),
        new fst_C(u8("histadd"),         2,  2, f_histadd        ),
        new fst_C(u8("histdel"),         1,  2, f_histdel        ),
        new fst_C(u8("histget"),         1,  2, f_histget        ),
        new fst_C(u8("histnr"),          1,  1, f_histnr         ),
        new fst_C(u8("hlID"),            1,  1, f_hlID           ),
        new fst_C(u8("hlexists"),        1,  1, f_hlexists       ),
        new fst_C(u8("indent"),          1,  1, f_indent         ),
        new fst_C(u8("index"),           2,  4, f_index          ),
        new fst_C(u8("input"),           1,  3, f_input          ),
        new fst_C(u8("inputdialog"),     1,  3, f_inputdialog    ),
        new fst_C(u8("inputlist"),       1,  1, f_inputlist      ),
        new fst_C(u8("inputrestore"),    0,  0, f_inputrestore   ),
        new fst_C(u8("inputsave"),       0,  0, f_inputsave      ),
        new fst_C(u8("inputsecret"),     1,  2, f_inputsecret    ),
        new fst_C(u8("insert"),          2,  3, f_insert         ),
        new fst_C(u8("invert"),          1,  1, f_invert         ),
        new fst_C(u8("islocked"),        1,  1, f_islocked       ),
        new fst_C(u8("items"),           1,  1, f_items          ),
        new fst_C(u8("join"),            1,  2, f_join           ),
        new fst_C(u8("keys"),            1,  1, f_keys           ),
        new fst_C(u8("len"),             1,  1, f_len            ),
        new fst_C(u8("line"),            1,  1, f_line           ),
        new fst_C(u8("line2byte"),       1,  1, f_line2byte      ),
        new fst_C(u8("lispindent"),      1,  1, f_lispindent     ),
        new fst_C(u8("localtime"),       0,  0, f_localtime      ),
        new fst_C(u8("map"),             2,  2, f_map            ),
        new fst_C(u8("maparg"),          1,  4, f_maparg         ),
        new fst_C(u8("mapcheck"),        1,  3, f_mapcheck       ),
        new fst_C(u8("match"),           2,  4, f_match          ),
        new fst_C(u8("matchadd"),        2,  4, f_matchadd       ),
        new fst_C(u8("matchaddpos"),     2,  4, f_matchaddpos    ),
        new fst_C(u8("matcharg"),        1,  1, f_matcharg       ),
        new fst_C(u8("matchdelete"),     1,  1, f_matchdelete    ),
        new fst_C(u8("matchend"),        2,  4, f_matchend       ),
        new fst_C(u8("matchlist"),       2,  4, f_matchlist      ),
        new fst_C(u8("matchstr"),        2,  4, f_matchstr       ),
        new fst_C(u8("max"),             1,  1, f_max            ),
        new fst_C(u8("min"),             1,  1, f_min            ),
        new fst_C(u8("mode"),            0,  1, f_mode           ),
        new fst_C(u8("nextnonblank"),    1,  1, f_nextnonblank   ),
        new fst_C(u8("nr2char"),         1,  2, f_nr2char        ),
        new fst_C(u8("or"),              2,  2, f_or             ),
        new fst_C(u8("pathshorten"),     1,  1, f_pathshorten    ),
        new fst_C(u8("prevnonblank"),    1,  1, f_prevnonblank   ),
        new fst_C(u8("range"),           1,  3, f_range          ),
        new fst_C(u8("readfile"),        1,  3, f_readfile       ),
        new fst_C(u8("reltime"),         0,  2, f_reltime        ),
        new fst_C(u8("reltimestr"),      1,  1, f_reltimestr     ),
        new fst_C(u8("remove"),          2,  3, f_remove         ),
        new fst_C(u8("repeat"),          2,  2, f_repeat         ),
        new fst_C(u8("reverse"),         1,  1, f_reverse        ),
        new fst_C(u8("screenattr"),      2,  2, f_screenattr     ),
        new fst_C(u8("screenchar"),      2,  2, f_screenchar     ),
        new fst_C(u8("screencol"),       0,  0, f_screencol      ),
        new fst_C(u8("screenrow"),       0,  0, f_screenrow      ),
        new fst_C(u8("search"),          1,  4, f_search         ),
        new fst_C(u8("searchdecl"),      1,  3, f_searchdecl     ),
        new fst_C(u8("searchpair"),      3,  7, f_searchpair     ),
        new fst_C(u8("searchpairpos"),   3,  7, f_searchpairpos  ),
        new fst_C(u8("searchpos"),       1,  4, f_searchpos      ),
        new fst_C(u8("setbufvar"),       3,  3, f_setbufvar      ),
        new fst_C(u8("setcmdpos"),       1,  1, f_setcmdpos      ),
        new fst_C(u8("setline"),         2,  2, f_setline        ),
        new fst_C(u8("setmatches"),      1,  1, f_setmatches     ),
        new fst_C(u8("setpos"),          2,  2, f_setpos         ),
        new fst_C(u8("setreg"),          2,  3, f_setreg         ),
        new fst_C(u8("settabvar"),       3,  3, f_settabvar      ),
        new fst_C(u8("settabwinvar"),    4,  4, f_settabwinvar   ),
        new fst_C(u8("setwinvar"),       3,  3, f_setwinvar      ),
        new fst_C(u8("shiftwidth"),      0,  0, f_shiftwidth     ),
        new fst_C(u8("simplify"),        1,  1, f_simplify       ),
        new fst_C(u8("sort"),            1,  3, f_sort           ),
        new fst_C(u8("split"),           1,  3, f_split          ),
        new fst_C(u8("str2nr"),          1,  2, f_str2nr         ),
        new fst_C(u8("strchars"),        1,  1, f_strchars       ),
        new fst_C(u8("strdisplaywidth"), 1,  2, f_strdisplaywidth),
        new fst_C(u8("strftime"),        1,  2, f_strftime       ),
        new fst_C(u8("stridx"),          2,  3, f_stridx         ),
        new fst_C(u8("string"),          1,  1, f_string         ),
        new fst_C(u8("strlen"),          1,  1, f_strlen         ),
        new fst_C(u8("strpart"),         2,  3, f_strpart        ),
        new fst_C(u8("strridx"),         2,  3, f_strridx        ),
        new fst_C(u8("strtrans"),        1,  1, f_strtrans       ),
        new fst_C(u8("strwidth"),        1,  1, f_strwidth       ),
        new fst_C(u8("submatch"),        1,  2, f_submatch       ),
        new fst_C(u8("substitute"),      4,  4, f_substitute     ),
        new fst_C(u8("synID"),           3,  3, f_synID          ),
        new fst_C(u8("synIDattr"),       2,  3, f_synIDattr      ),
        new fst_C(u8("synIDtrans"),      1,  1, f_synIDtrans     ),
        new fst_C(u8("synconcealed"),    2,  2, f_synconcealed   ),
        new fst_C(u8("synstack"),        2,  2, f_synstack       ),
        new fst_C(u8("tabpagebuflist"),  0,  1, f_tabpagebuflist ),
        new fst_C(u8("tabpagenr"),       0,  1, f_tabpagenr      ),
        new fst_C(u8("tabpagewinnr"),    1,  2, f_tabpagewinnr   ),
        new fst_C(u8("tolower"),         1,  1, f_tolower        ),
        new fst_C(u8("toupper"),         1,  1, f_toupper        ),
        new fst_C(u8("tr"),              3,  3, f_tr             ),
        new fst_C(u8("type"),            1,  1, f_type           ),
        new fst_C(u8("undofile"),        1,  1, f_undofile       ),
        new fst_C(u8("undotree"),        0,  0, f_undotree       ),
        new fst_C(u8("uniq"),            1,  3, f_uniq           ),
        new fst_C(u8("values"),          1,  1, f_values         ),
        new fst_C(u8("virtcol"),         1,  1, f_virtcol        ),
        new fst_C(u8("visualmode"),      0,  1, f_visualmode     ),
        new fst_C(u8("winbufnr"),        1,  1, f_winbufnr       ),
        new fst_C(u8("wincol"),          0,  0, f_wincol         ),
        new fst_C(u8("winheight"),       1,  1, f_winheight      ),
        new fst_C(u8("winline"),         0,  0, f_winline        ),
        new fst_C(u8("winnr"),           0,  1, f_winnr          ),
        new fst_C(u8("winrestcmd"),      0,  0, f_winrestcmd     ),
        new fst_C(u8("winrestview"),     1,  1, f_winrestview    ),
        new fst_C(u8("winsaveview"),     0,  0, f_winsaveview    ),
        new fst_C(u8("winwidth"),        1,  1, f_winwidth       ),
        new fst_C(u8("writefile"),       2,  3, f_writefile      ),
        new fst_C(u8("xor"),             2,  2, f_xor            ),
    };

    /*private*/ static pos_C _1_pos = new pos_C();

    /*
     * Translate a String variable into a position.
     * Returns null when there is an error.
     */
    /*private*/ static pos_C var2fpos(typval_C varp, boolean dollar_lnum, int[] fnum)
        /* dollar_lnum: true when $ is last line */
        /* fnum: set to fnum for '0, 'A, etc. */
    {
        /* Argument can be [lnum, col, coladd]. */
        if (varp.tv_type == VAR_LIST)
        {
            boolean[] error = { false };

            list_C l = varp.tv_list;
            if (l == null)
                return null;

            /* Get the line number. */
            _1_pos.lnum = list_find_nr(l, 0L, error);
            if (error[0] || _1_pos.lnum <= 0 || curbuf.b_ml.ml_line_count < _1_pos.lnum)
                return null;        /* invalid line number */

            /* Get the column number. */
            _1_pos.col = (int)list_find_nr(l, 1L, error);
            if (error[0])
                return null;

            int len = strlen(ml_get(_1_pos.lnum));

            /* We accept "$" for the column number: last column. */
            listitem_C li = list_find(l, 1L);
            if (li != null && li.li_tv.tv_type == VAR_STRING
                    && li.li_tv.tv_string != null
                    && STRCMP(li.li_tv.tv_string, u8("$")) == 0)
                _1_pos.col = len + 1;

            /* Accept a position up to the NUL after the line. */
            if (_1_pos.col == 0 || len + 1 < _1_pos.col)
                return null;        /* invalid column number */
            --_1_pos.col;

            /* Get the virtual offset.  Defaults to zero. */
            _1_pos.coladd = (int)list_find_nr(l, 2L, error);
            if (error[0])
                _1_pos.coladd = 0;

            return _1_pos;
        }

        Bytes name = get_tv_string_chk(varp);
        if (name == null)
            return null;
        if (name.at(0) == (byte)'.')                         /* cursor */
            return curwin.w_cursor;
        if (name.at(0) == (byte)'v' && name.at(1) == NUL)       /* Visual start */
        {
            if (VIsual_active)
                return VIsual;

            return curwin.w_cursor;
        }
        if (name.at(0) == (byte)'\'')                        /* mark */
        {
            pos_C pp = getmark_buf_fnum(curbuf, name.at(1), false, fnum);
            if (pp == null || pp == NOPOS || pp.lnum <= 0)
                return null;

            return pp;
        }

        _1_pos.coladd = 0;

        if (name.at(0) == (byte)'w' && dollar_lnum)
        {
            _1_pos.col = 0;
            if (name.at(1) == (byte)'0')             /* "w0": first visible line */
            {
                update_topline();
                _1_pos.lnum = curwin.w_topline;
                return _1_pos;
            }
            else if (name.at(1) == (byte)'$')        /* "w$": last visible line */
            {
                validate_botline();
                _1_pos.lnum = curwin.w_botline - 1;
                return _1_pos;
            }
        }
        else if (name.at(0) == (byte)'$')            /* last column or line */
        {
            if (dollar_lnum)
            {
                _1_pos.lnum = curbuf.b_ml.ml_line_count;
                _1_pos.col = 0;
            }
            else
            {
                _1_pos.lnum = curwin.w_cursor.lnum;
                _1_pos.col = strlen(ml_get_curline());
            }
            return _1_pos;
        }

        return null;
    }

    /*
     * Convert list in "arg" into a position and optional file number.
     * When "fnump" is null there is no file number, only 3 items.
     * Note that the column is passed on as-is, the caller may want to decrement
     * it to use 1 for the first column.
     * Return false when conversion is not possible, doesn't check the position for validity.
     */
    /*private*/ static boolean list2fpos(typval_C arg, pos_C posp, int[] fnump, int[] curswantp)
    {
        list_C l = arg.tv_list;

        /* List must be: [fnum, lnum, col, coladd, curswant], where "fnum" is only
         * there when "fnump" isn't null; "coladd" and "curswant" are optional. */
        if (arg.tv_type != VAR_LIST
                || l == null
                || l.lv_len < (fnump == null ? 2 : 3)
                || l.lv_len > (fnump == null ? 4 : 5))
            return false;

        long n, i = 0;

        if (fnump != null)
        {
            n = list_find_nr(l, i++, null); /* fnum */
            if (n < 0)
                return false;
            if (n == 0)
                n = curbuf.b_fnum;          /* current buffer */
            fnump[0] = (int)n;
        }

        n = list_find_nr(l, i++, null);     /* lnum */
        if (n < 0)
            return false;
        posp.lnum = n;

        n = list_find_nr(l, i++, null);     /* col */
        if (n < 0)
            return false;
        posp.col = (int)n;

        n = list_find_nr(l, i, null);       /* off */
        if (n < 0)
            posp.coladd = 0;
        else
            posp.coladd = (int)n;

        if (curswantp != null)
            curswantp[0] = (int)list_find_nr(l, i + 1, null);  /* curswant */

        return true;
    }

    /*
     * Get the length of the name of a function or internal variable.
     * "arg" is advanced to the first non-white character after the name.
     * Return 0 if something is wrong.
     */
    /*private*/ static int get_id_len(Bytes[] arg)
    {
        Bytes p;
        for (p = arg[0]; eval_isnamec(p.at(0)); p = p.plus(1))
            ;
        if (BEQ(p, arg[0]))          /* no name found */
            return 0;

        int len = BDIFF(p, arg[0]);
        arg[0] = skipwhite(p);
        return len;
    }

    /*
     * Get the length of the name of a variable or function.
     * Only the name is recognized, does not handle ".key" or "[idx]".
     * "arg" is advanced to the first non-white character after the name.
     * Return -1 if curly braces expansion failed.
     * Return 0 if something else is wrong.
     * If the name contains 'magic' {}'s, expand them and return the
     * expanded name in an allocated string via 'alias' - caller must free.
     */
    /*private*/ static int get_name_len(Bytes[] arg, Bytes[] alias, boolean evaluate, boolean verbose)
    {
        alias[0] = null;  /* default to no alias */

        if (arg[0].at(0) == KB_SPECIAL && arg[0].at(1) == KS_EXTRA && arg[0].at(2) == KE_SNR)
        {
            /* hard coded <SNR>, already translated */
            arg[0] = arg[0].plus(3);
            return get_id_len(arg) + 3;
        }
        int len = eval_fname_script(arg[0]);
        if (0 < len)
        {
            /* literal "<SID>", "s:" or "<SNR>" */
            arg[0] = arg[0].plus(len);
        }

        /*
         * Find the end of the name; check for {} construction.
         */
        Bytes[] expr_start = new Bytes[1];
        Bytes[] expr_end = new Bytes[1];
        Bytes p = find_name_end(arg[0], expr_start, expr_end, (0 < len) ? 0 : FNE_CHECK_START);
        if (expr_start[0] != null)
        {
            if (!evaluate)
            {
                len += BDIFF(p, arg[0]);
                arg[0] = skipwhite(p);
                return len;
            }

            /*
             * Include any <SID> etc in the expanded string:
             * Thus the -len here.
             */
            Bytes temp_string = make_expanded_name(arg[0].minus(len), expr_start[0], expr_end[0], p);
            if (temp_string == null)
                return -1;
            alias[0] = temp_string;
            arg[0] = skipwhite(p);
            return strlen(temp_string);
        }

        len += get_id_len(arg);
        if (len == 0 && verbose)
            emsg2(e_invexpr2, arg[0]);

        return len;
    }

    /*
     * Find the end of a variable or function name, taking care of magic braces.
     * If "expr_start" is not null then "expr_start" and "expr_end" are set to the
     * start and end of the first magic braces item.
     * "flags" can have FNE_INCL_BR and FNE_CHECK_START.
     * Return a pointer to just after the name.  Equal to "arg" if there is no valid name.
     */
    /*private*/ static Bytes find_name_end(Bytes arg, Bytes[] expr_start, Bytes[] expr_end, int flags)
    {
        if (expr_start != null)
        {
            expr_start[0] = null;
            expr_end[0] = null;
        }

        /* Quick check for valid starting character. */
        if ((flags & FNE_CHECK_START) != 0 && !eval_isnamec1(arg.at(0)) && arg.at(0) != (byte)'{')
            return arg;

        Bytes p;

        int mb_nest = 0;
        int br_nest = 0;

        for (p = arg; p.at(0) != NUL
                        && (eval_isnamec(p.at(0))
                            || p.at(0) == (byte)'{'
                            || ((flags & FNE_INCL_BR) != 0 && (p.at(0) == (byte)'[' || p.at(0) == (byte)'.'))
                            || mb_nest != 0
                            || br_nest != 0); p = p.plus(us_ptr2len_cc(p)))
        {
            if (p.at(0) == (byte)'\'')
            {
                /* skip over 'string' to avoid counting [ and ] inside it. */
                for (p = p.plus(1); p.at(0) != NUL && p.at(0) != (byte)'\''; p = p.plus(us_ptr2len_cc(p)))
                    ;
                if (p.at(0) == NUL)
                    break;
            }
            else if (p.at(0) == (byte)'"')
            {
                /* skip over "str\"ing" to avoid counting [ and ] inside it. */
                for (p = p.plus(1); p.at(0) != NUL && p.at(0) != (byte)'"'; p = p.plus(us_ptr2len_cc(p)))
                    if (p.at(0) == (byte)'\\' && p.at(1) != NUL)
                        p = p.plus(1);
                if (p.at(0) == NUL)
                    break;
            }

            if (mb_nest == 0)
            {
                if (p.at(0) == (byte)'[')
                    br_nest++;
                else if (p.at(0) == (byte)']')
                    --br_nest;
            }

            if (br_nest == 0)
            {
                if (p.at(0) == (byte)'{')
                {
                    mb_nest++;
                    if (expr_start != null && expr_start[0] == null)
                        expr_start[0] = p;
                }
                else if (p.at(0) == (byte)'}')
                {
                    mb_nest--;
                    if (expr_start != null && mb_nest == 0 && expr_end[0] == null)
                        expr_end[0] = p;
                }
            }
        }

        return p;
    }

    /*
     * Expands out the 'magic' {}'s in a variable/function name.
     * Note that this can call itself recursively, to deal with
     * constructs like foo{bar}{baz}{bam}
     * The four pointer arguments point to "foo{expre}ss{ion}bar"
     *                      "in_start"      ^
     *                      "expr_start"       ^
     *                      "expr_end"               ^
     *                      "in_end"                            ^
     *
     * Returns a new allocated string, which the caller must free.
     * Returns null for failure.
     */
    /*private*/ static Bytes make_expanded_name(Bytes in_start, Bytes _expr_start, Bytes _expr_end, Bytes in_end)
    {
        Bytes[] expr_start = { _expr_start };
        Bytes[] expr_end = { _expr_end };

        Bytes retval = null;
        Bytes[] nextcmd = { null };

        if (expr_end[0] == null || in_end == null)
            return null;
        expr_start[0].be(0, NUL);
        expr_end[0].be(0, NUL);

        byte c1 = in_end.at(0);
        in_end.be(0, NUL);

        Bytes s = eval_to_string(expr_start[0].plus(1), nextcmd, false);
        if (s != null && nextcmd[0] == null)
        {
            retval = new Bytes(strlen(s) + BDIFF(expr_start[0], in_start) + BDIFF(in_end, expr_end[0]) + 1);

            STRCPY(retval, in_start);
            STRCAT(retval, s);
            STRCAT(retval, expr_end[0].plus(1));
        }

        in_end.be(0, c1);               /* put char back for error messages */
        expr_start[0].be(0, (byte)'{');
        expr_end[0].be(0, (byte)'}');

        if (retval != null)
        {
            s = find_name_end(retval, expr_start, expr_end, 0);
            if (expr_start[0] != null)
            {
                /* Further expansion! */
                s = make_expanded_name(retval, expr_start[0], expr_end[0], s);
                retval = s;
            }
        }

        return retval;
    }

    /*
     * Return true if character "c" can be used in a variable or function name.
     * Does not include '{' or '}' for magic braces.
     */
    /*private*/ static boolean eval_isnamec(int c)
    {
        return (asc_isalnum(c) || c == '_' || c == ':' || c == AUTOLOAD_CHAR);
    }

    /*
     * Return true if character "c" can be used as the first character in a
     * variable or function name (excluding '{' and '}').
     */
    /*private*/ static boolean eval_isnamec1(int c)
    {
        return (asc_isalpha(c) || c == '_');
    }

    /*
     * Set number v: variable to "val".
     */
    /*private*/ static void set_vim_var_nr(int idx, long val)
    {
        vimvars[idx].vv_di.di_tv.tv_number = val;
    }

    /*
     * Get number v: variable value.
     */
    /*private*/ static long get_vim_var_nr(int idx)
    {
        return vimvars[idx].vv_di.di_tv.tv_number;
    }

    /*
     * Get string v: variable value.  Uses a static buffer, can only be used once.
     */
    /*private*/ static Bytes get_vim_var_str(int idx)
    {
        return get_tv_string(vimvars[idx].vv_di.di_tv);
    }

    /*
     * Get List v: variable value.  Caller must take care of reference count when needed.
     */
    /*private*/ static list_C get_vim_var_list(int idx)
    {
        return vimvars[idx].vv_di.di_tv.tv_list;
    }

    /*
     * Set v:char to character "c".
     */
    /*private*/ static void set_vim_var_char(int c)
    {
        Bytes buf = new Bytes(MB_MAXBYTES + 1);

        buf.be(utf_char2bytes(c, buf), NUL);
        set_vim_var_string(VV_CHAR, buf, -1);
    }

    /*
     * Set v:count to "count" and v:count1 to "count1".
     * When "set_prevcount" is true first set v:prevcount from v:count.
     */
    /*private*/ static void set_vcount(long count, long count1, boolean set_prevcount)
    {
        if (set_prevcount)
            set_vim_var_nr(VV_PREVCOUNT, get_vim_var_nr(VV_COUNT));
        set_vim_var_nr(VV_COUNT, count);
        set_vim_var_nr(VV_COUNT1, count1);
    }

    /*
     * Set string v: variable to a copy of "val".
     */
    /*private*/ static void set_vim_var_string(int idx, Bytes val, int len)
        /* len: length of "val" to use or -1 (whole string) */
    {
        dictitem_C di = vimvars[idx].vv_di;

        if (val == null)
            di.di_tv.tv_string = null;
        else if (len == -1)
            di.di_tv.tv_string = STRDUP(val);
        else
            di.di_tv.tv_string = STRNDUP(val, len);
    }

    /*
     * Set List v: variable to "val".
     */
    /*private*/ static void set_vim_var_list(int idx, list_C val)
    {
        dictitem_C di = vimvars[idx].vv_di;

        list_unref(di.di_tv.tv_list);

        di.di_tv.tv_list = val;
        if (val != null)
            val.lv_refcount++;
    }

    /*
     * Set v:register if needed.
     */
    /*private*/ static void set_reg_var(int c)
    {
        Bytes regname = new Bytes(1).be(0, (c != 0 && c != ' ') ? (byte)c : (byte)'"');

        /* Avoid free/alloc when the value is already right. */
        if (vimvars[VV_REG].vv_di.di_tv.tv_string == null || vimvars[VV_REG].vv_di.di_tv.tv_string.at(0) != c)
            set_vim_var_string(VV_REG, regname, 1);
    }

    /*
     * Get or set v:exception.  If "oldval" == null, return the current value.
     * Otherwise, restore the value to "oldval" and return null.
     * Must always be called in pairs to save and restore v:exception!
     * Does not take care of memory allocations.
     */
    /*private*/ static Bytes v_exception(Bytes oldval)
    {
        if (oldval == null)
            return vimvars[VV_EXCEPTION].vv_di.di_tv.tv_string;

        vimvars[VV_EXCEPTION].vv_di.di_tv.tv_string = oldval;
        return null;
    }

    /*
     * Get or set v:throwpoint.  If "oldval" == null, return the current value.
     * Otherwise, restore the value to "oldval" and return null.
     * Must always be called in pairs to save and restore v:throwpoint!
     * Does not take care of memory allocations.
     */
    /*private*/ static Bytes v_throwpoint(Bytes oldval)
    {
        if (oldval == null)
            return vimvars[VV_THROWPOINT].vv_di.di_tv.tv_string;

        vimvars[VV_THROWPOINT].vv_di.di_tv.tv_string = oldval;
        return null;
    }

    /*
     * Set v:cmdarg.
     * If "eap" != null, use "eap" to generate the value and return the old value.
     * If "oldarg" != null, restore the value to "oldarg" and return null.
     * Must always be called in pairs!
     */
    /*private*/ static Bytes set_cmdarg(exarg_C eap, Bytes oldarg)
    {
        Bytes oldval = vimvars[VV_CMDARG].vv_di.di_tv.tv_string;
        if (eap == null)
        {
            vimvars[VV_CMDARG].vv_di.di_tv.tv_string = oldarg;
            return null;
        }

        int len;
        if (eap.force_bin == FORCE_BIN)
            len = 6;
        else if (eap.force_bin == FORCE_NOBIN)
            len = 8;
        else
            len = 0;

        if (eap.read_edit)
            len += 7;

        if (eap.bad_char != 0)
            len += 7 + 4;       /* " ++bad=" + "keep" or "drop" */

        Bytes newval = new Bytes(len + 1);

        if (eap.force_bin == FORCE_BIN)
            libC.sprintf(newval, u8(" ++bin"));
        else if (eap.force_bin == FORCE_NOBIN)
            libC.sprintf(newval, u8(" ++nobin"));
        else
            newval.be(0, NUL);

        if (eap.read_edit)
            STRCAT(newval, u8(" ++edit"));

        if (eap.bad_char == BAD_KEEP)
            STRCPY(newval.plus(strlen(newval)), u8(" ++bad=keep"));
        else if (eap.bad_char == BAD_DROP)
            STRCPY(newval.plus(strlen(newval)), u8(" ++bad=drop"));
        else if (eap.bad_char != 0)
            libC.sprintf(newval.plus(strlen(newval)), u8(" ++bad=%c"), eap.bad_char);

        vimvars[VV_CMDARG].vv_di.di_tv.tv_string = newval;
        return oldval;
    }

    /*
     * Get the value of internal variable "name".
     * Return true or false.
     */
    /*private*/ static boolean get_var_tv(Bytes name, int len, typval_C rtv, boolean verbose, boolean no_autoload)
        /* len: length of "name" */
        /* rtv: null when only checking existence */
        /* verbose: may give error message */
        /* no_autoload: do not use script autoloading */
    {
        typval_C tv = null;
        typval_C atv = new typval_C();

        /* truncate the name, so that we can use strcmp() */
        int cc = name.at(len);
        name.be(len, NUL);

        /*
         * Check for "b:changedtick".
         */
        if (STRCMP(name, u8("b:changedtick")) == 0)
        {
            atv.tv_type = VAR_NUMBER;
            atv.tv_number = curbuf.b_changedtick;
            tv = atv;
        }
        /*
         * Check for user-defined variables.
         */
        else
        {
            dictitem_C v = find_var(name, null, no_autoload);
            if (v != null)
                tv = v.di_tv;
        }

        boolean ret = true;

        if (tv == null)
        {
            if (rtv != null && verbose)
                emsg2(e_undefvar, name);
            ret = false;
        }
        else if (rtv != null)
            copy_tv(tv, rtv);

        name.be(len, cc);

        return ret;
    }

    /*
     * Handle expr[expr], expr[expr:expr] subscript and .name lookup.
     * Also handle function call with Funcref variable: func(expr)
     * Can all be combined: dict.func(expr)[idx]['func'](expr)
     */
    /*private*/ static boolean handle_subscript(Bytes[] arg, typval_C rtv, boolean evaluate, boolean verbose)
        /* evaluate: do more than finding the end */
        /* verbose: give error messages */
    {
        boolean ret = true;

        dict_C selfdict = null;

        while (ret
                && (arg[0].at(0) == (byte)'['
                    || (arg[0].at(0) == (byte)'.' && rtv.tv_type == VAR_DICT)
                    || (arg[0].at(0) == (byte)'(' && (!evaluate || rtv.tv_type == VAR_FUNC)))
                && !vim_iswhite(arg[0].at(-1)))
        {
            if (arg[0].at(0) == (byte)'(')
            {
                typval_C functv = new typval_C();

                /* need to copy the funcref so that we can clear rtv */
                Bytes s;
                if (evaluate)
                {
                    COPY_typval(functv, rtv);
                    rtv.tv_type = VAR_UNKNOWN;

                    /* Invoke the function.  Recursive! */
                    s = functv.tv_string;
                }
                else
                    s = u8("");

                boolean[] doesrange = new boolean[1];
                ret = get_func_tv(s, strlen(s), rtv, arg,
                            curwin.w_cursor.lnum, curwin.w_cursor.lnum,
                            doesrange, evaluate, selfdict);

                /* Clear the funcref afterwards, so that deleting it while
                 * evaluating the arguments is possible (see test55). */
                if (evaluate)
                    clear_tv(functv);

                /* Stop the expression evaluation when immediately aborting on error,
                 * or when an interrupt occurred or an exception was thrown but not caught. */
                if (aborting())
                {
                    if (ret)
                        clear_tv(rtv);
                    ret = false;
                }
                dict_unref(selfdict);
                selfdict = null;
            }
            else /* arg[0][0] == '[' || arg[0][0] == '.' */
            {
                dict_unref(selfdict);
                if (rtv.tv_type == VAR_DICT)
                {
                    selfdict = rtv.tv_dict;
                    if (selfdict != null)
                        selfdict.dv_refcount++;
                }
                else
                    selfdict = null;
                if (eval_index(arg, rtv, evaluate, verbose) == false)
                {
                    clear_tv(rtv);
                    ret = false;
                }
            }
        }

        dict_unref(selfdict);

        return ret;
    }

    /*
     * Allocate memory for a variable type-value, and assign a string to it.
     * The string "s" must have been allocated, it is consumed.
     * Return null for out of memory, the variable otherwise.
     */
    /*private*/ static typval_C alloc_string_tv(Bytes s)
    {
        typval_C tv = new typval_C();

        tv.tv_type = VAR_STRING;
        tv.tv_string = s;

        return tv;
    }

    /*
     * Free the memory for a variable type-value.
     */
    /*private*/ static void free_tv(typval_C varp)
    {
        if (varp != null)
        {
            switch (varp.tv_type)
            {
                case VAR_FUNC:
                    func_unref(varp.tv_string);
                    /* FALLTHROUGH */
                case VAR_STRING:
                    varp.tv_string = null;
                    break;
                case VAR_LIST:
                    list_unref(varp.tv_list);
                    break;
                case VAR_DICT:
                    dict_unref(varp.tv_dict);
                    break;
                case VAR_NUMBER:
                case VAR_UNKNOWN:
                    break;
                default:
                    emsg2(e_intern2, u8("free_tv()"));
                    break;
            }
        }
    }

    /*
     * Free the memory for a variable value and set the value to null or 0.
     */
    /*private*/ static void clear_tv(typval_C varp)
    {
        if (varp != null)
        {
            switch (varp.tv_type)
            {
                case VAR_FUNC:
                    func_unref(varp.tv_string);
                    /* FALLTHROUGH */
                case VAR_STRING:
                    varp.tv_string = null;
                    break;
                case VAR_LIST:
                    list_unref(varp.tv_list);
                    varp.tv_list = null;
                    break;
                case VAR_DICT:
                    dict_unref(varp.tv_dict);
                    varp.tv_dict = null;
                    break;
                case VAR_NUMBER:
                    varp.tv_number = 0;
                    break;
                case VAR_UNKNOWN:
                    break;
                default:
                    emsg2(e_intern2, u8("clear_tv()"));
            }
            varp.tv_lock = 0;
        }
    }

    /*
     * Get the number value of a variable.
     * If it is a String variable, uses vim_str2nr().
     * For incompatible types, return 0.
     */
    /*private*/ static long get_tv_number(typval_C varp)
    {
        boolean[] error = { false };

        return get_tv_number_chk(varp, error);     /* return 0L on error */
    }

    /*
     * get_tv_number_chk() is similar to get_tv_number(), but informs the caller of incompatible types:
     * it sets "*denote" to true if "denote" is not null or returns -1 otherwise.
     */
    /*private*/ static long get_tv_number_chk(typval_C varp, boolean[] denote)
    {
        long[] n = { 0L };

        switch (varp.tv_type)
        {
            case VAR_NUMBER:
                return varp.tv_number;
            case VAR_FUNC:
                emsg(u8("E703: Using a Funcref as a Number"));
                break;
            case VAR_STRING:
                if (varp.tv_string != null)
                    vim_str2nr(varp.tv_string, null, null, TRUE, TRUE, n);
                return n[0];
            case VAR_LIST:
                emsg(u8("E745: Using a List as a Number"));
                break;
            case VAR_DICT:
                emsg(u8("E728: Using a Dictionary as a Number"));
                break;
            default:
                emsg2(e_intern2, u8("get_tv_number()"));
                break;
        }
        if (denote == null)         /* useful for values that must be unsigned */
            n[0] = -1;
        else
            denote[0] = true;
        return n[0];
    }

    /*
     * Get the lnum from the first argument.
     * Also accepts ".", "$", etc., but that only works for the current buffer.
     * Returns -1 on error.
     */
    /*private*/ static long get_tv_lnum(typval_C varp)
    {
        long lnum = get_tv_number_chk(varp, null);
        if (lnum == 0)  /* no valid number, try using line() */
        {
            int[] fnum = new int[1];
            pos_C fp = var2fpos(varp, true, fnum);
            if (fp != null)
                lnum = fp.lnum;
        }
        return lnum;
    }

    /*
     * Get the lnum from the first argument.
     * Also accepts "$", then "buf" is used.
     * Returns 0 on error.
     */
    /*private*/ static long get_tv_lnum_buf(typval_C varp, buffer_C buf)
    {
        if (varp.tv_type == VAR_STRING && varp.tv_string != null && varp.tv_string.at(0) == (byte)'$' && buf != null)
            return buf.b_ml.ml_line_count;

        return get_tv_number_chk(varp, null);
    }

    /*
     * Get the string value of a variable.
     * If it is a Number variable, the number is converted into a string.
     * If the String variable has never been set, return an empty string.
     * Never returns null.
     * get_tv_string_chk() is similar, but return null on error.
     */
    /*private*/ static Bytes get_tv_string(typval_C varp)
    {
        Bytes res = get_tv_string_chk(varp);

        return (res != null) ? res : u8("");
    }

    /*private*/ static Bytes get_tv_string_chk(typval_C varp)
    {
        switch (varp.tv_type)
        {
            case VAR_NUMBER:
                Bytes buf = new Bytes(NUMBUFLEN);
                libC.sprintf(buf, u8("%ld"), varp.tv_number);
                return buf;
            case VAR_FUNC:
                emsg(u8("E729: using Funcref as a String"));
                break;
            case VAR_LIST:
                emsg(u8("E730: using List as a String"));
                break;
            case VAR_DICT:
                emsg(u8("E731: using Dictionary as a String"));
                break;
            case VAR_STRING:
                if (varp.tv_string != null)
                    return varp.tv_string;
                return u8("");
            default:
                emsg2(e_intern2, u8("get_tv_string()"));
                break;
        }
        return null;
    }

    /*
     * Find variable "name" in the list of variables.
     * Return a pointer to it if found, null if not found.
     * Careful: "a:0" variables don't have a name.
     * When "htp" is not null we are writing to the variable, set "htp" to the hashtab_C used.
     */
    /*private*/ static dictitem_C find_var(Bytes name, hashtab_C[] htp, boolean no_autoload)
    {
        Bytes[] varname = new Bytes[1];

        hashtab_C ht = find_var_ht(name, varname);
        if (htp != null)
            htp[0] = ht;
        if (ht == null)
            return null;

        return find_var_in_ht(ht, name.at(0), varname[0], no_autoload || htp != null);
    }

    /*
     * Find variable "varname" in hashtab "ht" with name "htname".
     * Returns null if not found.
     */
    /*private*/ static dictitem_C find_var_in_ht(hashtab_C ht, int htname, Bytes varname, boolean no_autoload)
    {
        if (varname.at(0) == NUL)
        {
            /* Must be something like "s:", otherwise "ht" would be null. */
            switch (htname)
            {
                case 's':
                {
                    scriptvar_C sv = ga_scripts.ga_data[current_SID - 1];
                    return sv.sv_var;
                }
                case 'g': return globvars_var;
                case 'v': return vimvars_var;
                case 'b': return curbuf.b_bufvar;
                case 'w': return curwin.w_winvar;
                case 't': return curtab.tp_winvar;
                case 'l': return (current_funccal != null) ? current_funccal.l_vars_var : null;
                case 'a': return (current_funccal != null) ? current_funccal.l_avars_var : null;
            }
            return null;
        }

        hashitem_C hi = hash_find(ht, varname);
        if (hashitem_empty(hi))
        {
            /* For global variables we may try auto-loading the script.
             * If it worked, find the variable again.
             * Don't auto-load a script if it was loaded already,
             * otherwise it would be loaded every time when checking
             * if a function name is a Funcref variable. */
            if (ht == globvardict.dv_hashtab && !no_autoload)
            {
                /* Note: script_autoload() may make "hi" invalid.
                 * It must either be obtained again or not used. */
                if (!script_autoload(varname, false) || aborting())
                    return null;
                hi = hash_find(ht, varname);
            }
            if (hashitem_empty(hi))
                return null;
        }
        return (dictitem_C)hi.hi_data;
    }

    /*
     * Find the hashtab used for a variable name.
     * Set "varname" to the start of name without ':'.
     */
    /*private*/ static hashtab_C find_var_ht(Bytes name, Bytes[] varname)
    {
        if (name.at(1) != (byte)':')
        {
            /* The name must not start with a colon or #. */
            if (name.at(0) == (byte)':' || name.at(0) == AUTOLOAD_CHAR)
                return null;
            varname[0] = name;

            if (current_funccal == null)
                return globvardict.dv_hashtab;              /* global variable */

            return current_funccal.l_vars.dv_hashtab;       /* l: variable */
        }
        varname[0] = name.plus(2);
        if (name.at(0) == (byte)'g')                                   /* global variable */
            return globvardict.dv_hashtab;
        /*
         * There must be no ':' or '#' in the rest of the name, unless g: is used.
         */
        if (vim_strchr(name.plus(2), ':') != null || vim_strchr(name.plus(2), AUTOLOAD_CHAR) != null)
            return null;
        if (name.at(0) == (byte)'b')                                   /* buffer variable */
            return curbuf.b_vars.dv_hashtab;
        if (name.at(0) == (byte)'w')                                   /* window variable */
            return curwin.w_vars.dv_hashtab;
        if (name.at(0) == (byte)'t')                                   /* tab page variable */
            return curtab.tp_vars.dv_hashtab;
        if (name.at(0) == (byte)'v')                                   /* v: variable */
            return vimvardict.dv_hashtab;
        if (name.at(0) == (byte)'a' && current_funccal != null)        /* function argument */
            return current_funccal.l_avars.dv_hashtab;
        if (name.at(0) == (byte)'l' && current_funccal != null)        /* local function variable */
            return current_funccal.l_vars.dv_hashtab;
        if (name.at(0) == (byte)'s' && 0 < current_SID && current_SID <= ga_scripts.ga_len) /* script variable */
        {
            scriptvar_C sv = ga_scripts.ga_data[current_SID - 1];
            return sv.sv_dict.dv_hashtab;
        }

        return null;
    }

    /*
     * Get the string value of a (global/local) variable.
     * Note: see get_tv_string() for how long the pointer remains valid.
     * Returns null when it doesn't exist.
     */
    /*private*/ static Bytes get_var_value(Bytes name)
    {
        dictitem_C v = find_var(name, null, false);
        if (v == null)
            return null;

        return get_tv_string(v.di_tv);
    }

    /*
     * Allocate a new hashtab for a sourced script.  It will be used while
     * sourcing this script and when executing functions defined in the script.
     */
    /*private*/ static void new_script_vars(int id)
    {
        scriptvar_C[] spp = ga_scripts.ga_grow(id - ga_scripts.ga_len);

        /* Re-allocating ga_data means that an ht_buckets pointing to ht_small_buckets
         * becomes invalid.  We can recognize this: ht_mask is at its init value.
         * Also reset "tv_dict", it's always the same. */
        for (int i = 0; i < ga_scripts.ga_len; i++)
        {
            scriptvar_C sv = spp[i];
            hashtab_C ht = sv.sv_dict.dv_hashtab;
            if (ht.ht_mask == HT_INIT_SIZE - 1)
                ht.ht_buckets = ARRAY_hashitem(HT_INIT_SIZE);
            sv.sv_var.di_tv.tv_dict = sv.sv_dict;
        }

        for ( ; ga_scripts.ga_len < id; ga_scripts.ga_len++)
        {
            scriptvar_C sv = new scriptvar_C();
            sv.sv_var = new dictitem_C();
            sv.sv_dict = new dict_C();
            spp[ga_scripts.ga_len] = sv;
            init_var_dict(sv.sv_dict, sv.sv_var, VAR_SCOPE);
        }
    }

    /*
     * Initialize dictionary "dict" as a scope and set variable "dict_var" to point to it.
     */
    /*private*/ static void init_var_dict(dict_C dict, dictitem_C dict_var, byte scope)
    {
        hash_init(dict.dv_hashtab);
        dict.dv_lock = 0;
        dict.dv_scope = scope;
        dict.dv_refcount = DO_NOT_FREE_CNT;
        dict.dv_copyID = 0;

        dict_var.di_tv.tv_dict = dict;
        dict_var.di_tv.tv_type = VAR_DICT;
        dict_var.di_tv.tv_lock = VAR_FIXED;
        dict_var.di_flags = DI_FLAGS_RO | DI_FLAGS_FIX;
        dict_var.di_key = STRDUP(u8(""));
    }

    /*
     * Unreference a dictionary initialized by init_var_dict().
     */
    /*private*/ static void unref_var_dict(dict_C dict)
    {
        /* Now the dict needs to be freed if no one else is using it,
         * go back to normal reference counting. */
        dict.dv_refcount -= DO_NOT_FREE_CNT - 1;
        dict_unref(dict);
    }

    /*
     * Clean up a list of internal variables.
     * Frees all allocated variables and the value they contain.
     * Clears hashtab "ht", does not free it.
     */
    /*private*/ static void vars_clear(hashtab_C ht)
    {
        vars_clear_ext(ht, true);
    }

    /*
     * Like vars_clear(), but only free the value if "free_val" is true.
     */
    /*private*/ static void vars_clear_ext(hashtab_C ht, boolean free_val)
    {
        hash_lock(ht);
        for (int i = 0, todo = (int)ht.ht_used; 0 < todo; i++)
        {
            hashitem_C hi = ht.ht_buckets[i];
            if (!hashitem_empty(hi))
            {
                --todo;
                /* Free the variable.  Don't remove it from the hashtab,
                 * ht_buckets might change then.  hash_clear() takes care of it later. */
                dictitem_C di = (dictitem_C)hi.hi_data;
                if (free_val)
                    clear_tv(di.di_tv);
            }
        }
        hash_clear(ht);
        ht.ht_used = 0;
    }

    /*
     * Delete a variable from hashtab "ht" at item "hi".
     * Clear the variable value and free the dictitem.
     */
    /*private*/ static void delete_var(hashtab_C ht, hashitem_C hi)
    {
        dictitem_C di = (dictitem_C)hi.hi_data;

        hash_remove(ht, hi);
        clear_tv(di.di_tv);
    }

    /*
     * List the value of one internal variable.
     */
    /*private*/ static void list_one_var(dictitem_C di, Bytes prefix, boolean[] first)
    {
        current_copyID += COPYID_INC;

        Bytes s = echo_string(di.di_tv, current_copyID);
        list_one_var_a(prefix, di.di_key, di.di_tv.tv_type, s == null ? u8("") : s, first);
    }

    /*private*/ static void list_one_var_a(Bytes prefix, Bytes name, int type, Bytes string, boolean[] first)
        /* first: when true clear rest of screen and set to false */
    {
        /* don't use msg() or msg_attr() to avoid overwriting "v:statusmsg" */
        msg_start();
        msg_puts(prefix);
        if (name != null)   /* "a:" vars don't have a name stored */
            msg_puts(name);
        msg_putchar(' ');
        msg_advance(22);
        if (type == VAR_NUMBER)
            msg_putchar('#');
        else if (type == VAR_FUNC)
            msg_putchar('*');
        else if (type == VAR_LIST)
        {
            msg_putchar('[');
            if (string.at(0) == (byte)'[')
                string = string.plus(1);
        }
        else if (type == VAR_DICT)
        {
            msg_putchar('{');
            if (string.at(0) == (byte)'{')
                string = string.plus(1);
        }
        else
            msg_putchar(' ');

        msg_outtrans(string);

        if (type == VAR_FUNC)
            msg_puts(u8("()"));
        if (first[0])
        {
            msg_clr_eos();
            first[0] = false;
        }
    }

    /*
     * Set variable "name" to value in "tv".
     * If the variable already exists, the value is updated.
     * Otherwise the variable is created.
     */
    /*private*/ static void set_var(Bytes name, typval_C tv, boolean copy)
        /* copy: make copy of value in "tv" */
    {
        Bytes[] varname = new Bytes[1];
        hashtab_C ht = find_var_ht(name, varname);
        if (ht == null || varname[0].at(0) == NUL)
        {
            emsg2(e_illvar, name);
            return;
        }

        dictitem_C di = find_var_in_ht(ht, 0, varname[0], true);
        if (tv.tv_type == VAR_FUNC && var_check_func_name(name, di == null))
            return;

        if (di != null)
        {
            /* existing variable, need to clear the value */
            if (var_check_ro(di.di_flags, name) || tv_check_lock(di.di_tv.tv_lock, name))
                return;

            byte t1 = di.di_tv.tv_type, t2 = tv.tv_type;
            if (t1 != t2
                    && !((t1 == VAR_STRING || t1 == VAR_NUMBER) && (t2 == VAR_STRING || t2 == VAR_NUMBER))
                    && !(t1 == VAR_NUMBER && t2 == VAR_NUMBER))
            {
                emsg2(u8("E706: Variable type mismatch for: %s"), name);
                return;
            }

            /*
             * Handle setting internal v: variables separately: we don't change the type.
             */
            if (ht == vimvardict.dv_hashtab)
            {
                if (t1 == VAR_STRING)
                {
                    if (copy || t2 != VAR_STRING)
                        di.di_tv.tv_string = STRDUP(get_tv_string(tv));
                    else
                    {
                        /* Take over the string to avoid an extra alloc/free. */
                        di.di_tv.tv_string = tv.tv_string;
                        tv.tv_string = null;
                    }
                }
                else if (t1 != VAR_NUMBER)
                    emsg2(e_intern2, u8("set_var()"));
                else
                {
                    di.di_tv.tv_number = get_tv_number(tv);
                    if (STRCMP(varname[0], u8("searchforward")) == 0)
                        set_search_direction((di.di_tv.tv_number != 0) ? (byte)'/' : (byte)'?');
                    else if (STRCMP(varname[0], u8("hlsearch")) == 0)
                    {
                        no_hlsearch = (di.di_tv.tv_number == 0);
                        redraw_all_later(SOME_VALID);
                    }
                }
                return;
            }

            clear_tv(di.di_tv);
        }
        else                    /* add a new variable */
        {
            /* Can't add "v:" variable. */
            if (ht == vimvardict.dv_hashtab)
            {
                emsg2(e_illvar, name);
                return;
            }

            /* Make sure the variable name is valid. */
            if (!valid_varname(varname[0]))
                return;

            di = dictitem_alloc(varname[0]);

            if (!hash_add(ht, di, di.di_key))
                return;
        }

        if (copy || tv.tv_type == VAR_NUMBER)
            copy_tv(tv, di.di_tv);
        else
        {
            COPY_typval(di.di_tv, tv);
            di.di_tv.tv_lock = 0;
            ZER0_typval(tv);
        }
    }

    /*
     * Return true if di_flags "flags" indicates variable "name" is read-only.
     * Also give an error message.
     */
    /*private*/ static boolean var_check_ro(int flags, Bytes name)
    {
        if ((flags & DI_FLAGS_RO) != 0)
        {
            emsg2(e_readonlyvar, name);
            return true;
        }
        if ((flags & DI_FLAGS_RO_SBX) != 0 && sandbox != 0)
        {
            emsg2(e_readonlysbx, name);
            return true;
        }
        return false;
    }

    /*
     * Return true if di_flags "flags" indicates variable "name" is fixed.
     * Also give an error message.
     */
    /*private*/ static boolean var_check_fixed(int flags, Bytes name)
    {
        if ((flags & DI_FLAGS_FIX) != 0)
        {
            emsg2(u8("E795: Cannot delete variable %s"), name);
            return true;
        }
        return false;
    }

    /*
     * Check if a funcref is assigned to a valid variable name.
     * Return true and give an error if not.
     */
    /*private*/ static boolean var_check_func_name(Bytes name, boolean new_var)
        /* name: points to start of variable name */
        /* new_var: true when creating the variable */
    {
        /* Allow for w: b: s: and t:. */
        if (!(vim_strbyte(u8("wbst"), name.at(0)) != null && name.at(1) == (byte)':')
                && !asc_isupper((name.at(0) != NUL && name.at(1) == (byte)':') ? name.at(2) : name.at(0)))
        {
            emsg2(u8("E704: Funcref variable name must start with a capital: %s"), name);
            return true;
        }
        /* Don't allow hiding a function.  When "v" is not null, we might be assigning
         * another function to the same var, the type is checked below. */
        if (new_var && function_exists(name))
        {
            emsg2(u8("E705: Variable name conflicts with existing function: %s"), name);
            return true;
        }
        return false;
    }

    /*
     * Check if a variable name is valid.
     * Return false and give an error if not.
     */
    /*private*/ static boolean valid_varname(Bytes varname)
    {
        for (Bytes p = varname; p.at(0) != NUL; p = p.plus(1))
            if (!eval_isnamec1(p.at(0)) && (BEQ(p, varname) || !asc_isdigit(p.at(0))) && p.at(0) != AUTOLOAD_CHAR)
            {
                emsg2(e_illvar, varname);
                return false;
            }
        return true;
    }

    /*
     * Return true if typeval "tv" is set to be locked (immutable).
     * Also give an error message, using "name".
     */
    /*private*/ static boolean tv_check_lock(int lock, Bytes name)
    {
        if ((lock & VAR_LOCKED) != 0)
        {
            emsg2(u8("E741: Value is locked: %s"), (name == null) ? u8("Unknown") : name);
            return true;
        }
        if ((lock & VAR_FIXED) != 0)
        {
            emsg2(u8("E742: Cannot change value of %s"), (name == null) ? u8("Unknown") : name);
            return true;
        }
        return false;
    }

    /*
     * Copy the values from typval_C "from" to typval_C "to".
     * When needed allocates string or increases reference count.
     * Does not make a copy of a list or dict but copies the reference!
     * It is OK for "from" and "to" to point to the same item.  This is used to make a copy later.
     */
    /*private*/ static void copy_tv(typval_C from, typval_C to)
    {
        to.tv_type = from.tv_type;
        to.tv_lock = 0;

        switch (from.tv_type)
        {
            case VAR_NUMBER:
                to.tv_number = from.tv_number;
                break;

            case VAR_STRING:
            case VAR_FUNC:
                if (from.tv_string == null)
                    to.tv_string = null;
                else
                {
                    to.tv_string = STRDUP(from.tv_string);
                    if (from.tv_type == VAR_FUNC)
                        func_ref(to.tv_string);
                }
                break;

            case VAR_LIST:
                if (from.tv_list == null)
                    to.tv_list = null;
                else
                {
                    to.tv_list = from.tv_list;
                    to.tv_list.lv_refcount++;
                }
                break;

            case VAR_DICT:
                if (from.tv_dict == null)
                    to.tv_dict = null;
                else
                {
                    to.tv_dict = from.tv_dict;
                    to.tv_dict.dv_refcount++;
                }
                break;

            default:
                emsg2(e_intern2, u8("copy_tv()"));
                break;
        }
    }

    /*private*/ static int _3_recurse;

    /*
     * Make a copy of an item.
     * Lists and Dictionaries are also copied.  A deep copy if "deep" is set.
     * For deepcopy() "copyID" is zero for a full copy or the ID for when a
     * reference to an already copied list/dict can be used.
     * Returns false or true.
     */
    /*private*/ static boolean item_copy(typval_C from, typval_C to, boolean deep, int copyID)
    {
        if (DICT_MAXNEST <= _3_recurse)
        {
            emsg(u8("E698: variable nested too deep for making a copy"));
            return false;
        }
        _3_recurse++;

        boolean ret = true;

        switch (from.tv_type)
        {
            case VAR_NUMBER:
            case VAR_STRING:
            case VAR_FUNC:
            {
                copy_tv(from, to);
                break;
            }

            case VAR_LIST:
            {
                to.tv_type = VAR_LIST;
                to.tv_lock = 0;
                if (from.tv_list == null)
                    to.tv_list = null;
                else if (copyID != 0 && from.tv_list.lv_copyID == copyID)
                {
                    /* use the copy made earlier */
                    to.tv_list = from.tv_list.lv_copylist;
                    to.tv_list.lv_refcount++;
                }
                else
                    to.tv_list = list_copy(from.tv_list, deep, copyID);
                if (to.tv_list == null)
                    ret = false;
                break;
            }

            case VAR_DICT:
            {
                to.tv_type = VAR_DICT;
                to.tv_lock = 0;
                if (from.tv_dict == null)
                    to.tv_dict = null;
                else if (copyID != 0 && from.tv_dict.dv_copyID == copyID)
                {
                    /* use the copy made earlier */
                    to.tv_dict = from.tv_dict.dv_copydict;
                    to.tv_dict.dv_refcount++;
                }
                else
                    to.tv_dict = dict_copy(from.tv_dict, deep, copyID);
                if (to.tv_dict == null)
                    ret = false;
                break;
            }

            default:
            {
                emsg2(e_intern2, u8("item_copy()"));
                ret = false;
                break;
            }
        }

        --_3_recurse;
        return ret;
    }

    /*
     * ":echo expr1 ..."    print each argument separated with a space, add a newline at the end.
     * ":echon expr1 ..."   print each argument plain.
     */
    /*private*/ static final ex_func_C ex_echo = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes[] arg = { eap.arg };
            boolean needclr = true;
            boolean atstart = true;

            if (eap.skip)
                emsg_skip++;

            typval_C rtv = new typval_C();

            while (arg[0].at(0) != NUL && arg[0].at(0) != (byte)'|' && arg[0].at(0) != (byte)'\n' && !got_int)
            {
                /* If eval1() causes an error message the text from the command may
                 * still need to be cleared.  E.g., "echo 22,44". */
                need_clr_eos = needclr;

                Bytes p = arg[0];
                if (eval1(arg, rtv, !eap.skip) == false)
                {
                    /*
                     * Report the invalid expression unless the expression evaluation has been
                     * cancelled due to an aborting error, an interrupt, or an exception.
                     */
                    if (!aborting())
                        emsg2(e_invexpr2, p);
                    need_clr_eos = false;
                    break;
                }
                need_clr_eos = false;

                if (!eap.skip)
                {
                    if (atstart)
                    {
                        atstart = false;
                        /* Call msg_start() after eval1(),
                         * evaluating the expression may cause a message to appear. */
                        if (eap.cmdidx == CMD_echo)
                        {
                            /* Mark the saved text as finishing the line, so that what follows
                             * is displayed on a new line when scrolling back at the more prompt. */
                            msg_sb_eol();
                            msg_start();
                        }
                    }
                    else if (eap.cmdidx == CMD_echo)
                        msg_puts_attr(u8(" "), echo_attr);
                    current_copyID += COPYID_INC;
                    p = echo_string(rtv, current_copyID);
                    if (p != null)
                        for ( ; p.at(0) != NUL && !got_int; p = p.plus(1))
                        {
                            if (p.at(0) == (byte)'\n' || p.at(0) == (byte)'\r' || p.at(0) == TAB)
                            {
                                if (p.at(0) != TAB && needclr)
                                {
                                    /* remove any text still there from the command */
                                    msg_clr_eos();
                                    needclr = false;
                                }
                                msg_putchar_attr(p.at(0), echo_attr);
                            }
                            else
                            {
                                int i = us_ptr2len_cc(p);

                                msg_outtrans_len_attr(p, i, echo_attr);
                                p = p.plus(i - 1);
                            }
                        }
                }
                clear_tv(rtv);
                arg[0] = skipwhite(arg[0]);
            }

            eap.nextcmd = check_nextcmd(arg[0]);

            if (eap.skip)
                --emsg_skip;
            else
            {
                /* remove text that may still be there from the command */
                if (needclr)
                    msg_clr_eos();
                if (eap.cmdidx == CMD_echo)
                    msg_end();
            }
        }
    };

    /*
     * ":echohl {name}".
     */
    /*private*/ static final ex_func_C ex_echohl = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int id = syn_name2id(eap.arg);
            if (id == 0)
                echo_attr = 0;
            else
                echo_attr = syn_id2attr(id);
        }
    };

    /*
     * ":execute expr1 ..." execute the result of an expression.
     * ":echomsg expr1 ..." print a message.
     * ":echoerr expr1 ..." print an error.
     * Each gets spaces around each argument and a newline at the end for echo commands.
     */
    /*private*/ static final ex_func_C ex_execute = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes[] arg = { eap.arg };

            if (eap.skip)
                emsg_skip++;

            barray_C ba = new barray_C(80);

            boolean ret = true;

            typval_C rtv = new typval_C();

            while (arg[0].at(0) != NUL && arg[0].at(0) != (byte)'|' && arg[0].at(0) != (byte)'\n')
            {
                Bytes p = arg[0];
                if (eval1(arg, rtv, !eap.skip) == false)
                {
                    /*
                     * Report the invalid expression unless the expression evaluation has been
                     * cancelled due to an aborting error, an interrupt, or an exception.
                     */
                    if (!aborting())
                        emsg2(e_invexpr2, p);
                    ret = false;
                    break;
                }

                if (!eap.skip)
                {
                    p = get_tv_string(rtv);
                    int len = strlen(p);
                    Bytes s = new Bytes(ba_grow(ba, len + 2));
                    if (0 < ba.ba_len)
                        s.be(ba.ba_len++, (byte)' ');
                    STRCPY(s.plus(ba.ba_len), p);
                    ba.ba_len += len;
                }

                clear_tv(rtv);
                arg[0] = skipwhite(arg[0]);
            }

            if (ret && ba.ba_data != null)
            {
                if (eap.cmdidx == CMD_echomsg)
                {
                    msg_attr(new Bytes(ba.ba_data), echo_attr);
                    out_flush();
                }
                else if (eap.cmdidx == CMD_echoerr)
                {
                    /* We don't want to abort following commands, restore did_emsg. */
                    boolean save_did_emsg = did_emsg;
                    emsg(new Bytes(ba.ba_data));
                    if (!force_abort)
                        did_emsg = save_did_emsg;
                }
                else if (eap.cmdidx == CMD_execute)
                    do_cmdline(new Bytes(ba.ba_data), eap.getline, eap.cookie, DOCMD_NOWAIT|DOCMD_VERBOSE);
            }

            ba_clear(ba);

            if (eap.skip)
                --emsg_skip;

            eap.nextcmd = check_nextcmd(arg[0]);
        }
    };

    /*
     * Skip over the name of an option: "&option", "&g:option" or "&l:option".
     * "arg" points to the "&" or '+' when called, to "option" when returning.
     * Returns null when no option name found.  Otherwise pointer to the char
     * after the option name.
     */
    /*private*/ static Bytes find_option_end(Bytes[] arg, int[] opt_flags)
    {
        Bytes p = arg[0];

        p = p.plus(1);
        if (p.at(0) == (byte)'g' && p.at(1) == (byte)':')
        {
            opt_flags[0] = OPT_GLOBAL;
            p = p.plus(2);
        }
        else if (p.at(0) == (byte)'l' && p.at(1) == (byte)':')
        {
            opt_flags[0] = OPT_LOCAL;
            p = p.plus(2);
        }
        else
            opt_flags[0] = 0;

        if (!asc_isalpha(p.at(0)))
            return null;
        arg[0] = p;

        if (p.at(0) == (byte)'t' && p.at(1) == (byte)'_' && p.at(2) != NUL && p.at(3) != NUL)
            p = p.plus(4);     /* termcap option */
        else
            while (asc_isalpha(p.at(0)))
                p = p.plus(1);
        return p;
    }

    /*private*/ static int func_nr;     /* number for nameless function */

    /*
     * ":function"
     */
    /*private*/ static final ex_func_C ex_function = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            boolean saved_wait_return = need_wait_return;

            /*
             * ":function" without argument: list functions.
             */
            if (ends_excmd(eap.arg.at(0)))
            {
                if (!eap.skip)
                {
                    for (int i = 0, todo = (int)func_hashtab.ht_used; 0 < todo && !got_int; i++)
                    {
                        hashitem_C hi = func_hashtab.ht_buckets[i];
                        if (!hashitem_empty(hi))
                        {
                            ufunc_C fp = (ufunc_C)hi.hi_data;
                            if (!asc_isdigit(fp.uf_name.at(0)))
                                list_func_head(fp, false);
                            --todo;
                        }
                    }
                }
                eap.nextcmd = check_nextcmd(eap.arg);
                return;
            }

            /*
             * ":function /pat": list functions matching pattern.
             */
            if (eap.arg.at(0) == (byte)'/')
            {
                Bytes p = skip_regexp(eap.arg.plus(1), (byte)'/', true, null);
                if (!eap.skip)
                {
                    regmatch_C regmatch = new regmatch_C();

                    byte c = p.at(0);
                    p.be(0, NUL);
                    regmatch.regprog = vim_regcomp(eap.arg.plus(1), RE_MAGIC);
                    p.be(0, c);
                    if (regmatch.regprog != null)
                    {
                        regmatch.rm_ic = p_ic[0];

                        for (int i = 0, todo = (int)func_hashtab.ht_used; 0 < todo && !got_int; i++)
                        {
                            hashitem_C hi = func_hashtab.ht_buckets[i];
                            if (!hashitem_empty(hi))
                            {
                                ufunc_C fp = (ufunc_C)hi.hi_data;
                                if (!asc_isdigit(fp.uf_name.at(0)) && vim_regexec(regmatch, fp.uf_name, 0))
                                    list_func_head(fp, false);
                                --todo;
                            }
                        }
                    }
                }
                if (p.at(0) == (byte)'/')
                    p = p.plus(1);
                eap.nextcmd = check_nextcmd(p);
                return;
            }

            /*
             * Get the function name.  There are these situations:
             * func         normal function name
             *              "name" == func, "fudi.fd_dict" == null
             * dict.func    new dictionary entry
             *              "name" == null, "fudi.fd_dict" set,
             *              "fudi.fd_di" == null, "fudi.fd_newkey" == func
             * dict.func    existing dict entry with a Funcref
             *              "name" == func, "fudi.fd_dict" set,
             *              "fudi.fd_di" set, "fudi.fd_newkey" == null
             * dict.func    existing dict entry that's not a Funcref
             *              "name" == null, "fudi.fd_dict" set,
             *              "fudi.fd_di" set, "fudi.fd_newkey" == null
             * s:func       script-local function name
             * g:func       global function name, same as "func"
             */
            Bytes p = eap.arg;
            funcdict_C fudi = new funcdict_C();
            Bytes name;
            { Bytes[] __ = { p }; name = trans_function_name(__, eap.skip, 0, fudi); p = __[0]; }
            boolean paren = (vim_strchr(p, '(') != null);
            if (name == null && (fudi.fd_dict == null || !paren) && !eap.skip)
            {
                /*
                 * Report an invalid expression in braces, unless the expression evaluation
                 * has been cancelled due to an aborting error, an interrupt, or an exception.
                 */
                if (!aborting())
                {
                    if (!eap.skip && fudi.fd_newkey != null)
                        emsg2(e_dictkey, fudi.fd_newkey);
                    return;
                }
                else
                    eap.skip = true;
            }

            /* An error in a function call during evaluation of an expression in
             * magic braces should not cause the function not to be defined. */
            boolean saved_did_emsg = did_emsg;
            did_emsg = false;

            theend:
            {
                /*
                 * ":function func" with only function name: list function.
                 */
                if (!paren)
                {
                    if (!ends_excmd(skipwhite(p).at(0)))
                    {
                        emsg(e_trailing);
                        break theend;
                    }
                    eap.nextcmd = check_nextcmd(p);
                    if (eap.nextcmd != null)
                        p.be(0, NUL);
                    if (!eap.skip && !got_int)
                    {
                        ufunc_C fp = find_func(name);
                        if (fp != null)
                        {
                            list_func_head(fp, true);
                            for (int j = 0; j < fp.uf_lines.ga_len && !got_int; j++)
                            {
                                Bytes line = fp.uf_lines.ga_data[j];
                                if (line == null)
                                    continue;
                                msg_putchar('\n');
                                msg_outnum((long)(j + 1));
                                if (j < 9)
                                    msg_putchar(' ');
                                if (j < 99)
                                    msg_putchar(' ');
                                msg_prt_line(line, false);
                                out_flush();        /* show a line at a time */
                                ui_breakcheck();
                            }
                            if (!got_int)
                            {
                                msg_putchar('\n');
                                msg_puts(u8("   endfunction"));
                            }
                        }
                        else
                            emsg_funcname(u8("E123: Undefined function: %s"), name);
                    }
                    break theend;
                }

                /*
                 * ":function name(arg1, arg2)" Define function.
                 */
                p = skipwhite(p);
                if (p.at(0) != (byte)'(')
                {
                    if (!eap.skip)
                    {
                        emsg2(u8("E124: Missing '(': %s"), eap.arg);
                        break theend;
                    }
                    /* attempt to continue by skipping some text */
                    Bytes q = vim_strchr(p, '(');
                    if (q != null)
                        p = q;
                }
                p = skipwhite(p.plus(1));

                if (!eap.skip)
                {
                    /* Check the name of the function.
                     * Unless it's a dictionary function (that we are overwriting). */
                    Bytes arg = (name != null) ? name : fudi.fd_newkey;
                    if (arg != null && (fudi.fd_di == null || fudi.fd_di.di_tv.tv_type != VAR_FUNC))
                    {
                        int j = (arg.at(0) == KB_SPECIAL) ? 3 : 0;
                        while (arg.at(j) != NUL && (j == 0 ? eval_isnamec1(arg.at(j)) : eval_isnamec(arg.at(j))))
                            j++;
                        if (arg.at(j) != NUL)
                            emsg_funcname(e_invarg2, arg);
                    }
                    /* Disallow using the g: dict. */
                    if (fudi.fd_dict != null && fudi.fd_dict.dv_scope == VAR_DEF_SCOPE)
                        emsg(u8("E862: Cannot use g: here"));
                }

                Bytes skip_until = null;

                Growing<Bytes> newargs = new Growing<Bytes>(Bytes.class, 3);
                Growing<Bytes> newlines = new Growing<Bytes>(Bytes.class, 3);

                boolean varargs = false, mustend = false;
                /*
                 * Isolate the arguments: "arg1, arg2, ...)"
                 */
                while (p.at(0) != (byte)')')
                {
                    if (p.at(0) == (byte)'.' && p.at(1) == (byte)'.' && p.at(2) == (byte)'.')
                    {
                        varargs = true;
                        p = p.plus(3);
                        mustend = true;
                    }
                    else
                    {
                        Bytes arg = p;
                        while (asc_isalnum(p.at(0)) || p.at(0) == (byte)'_')
                            p = p.plus(1);
                        if (BEQ(arg, p) || asc_isdigit(arg.at(0))
                                || (BDIFF(p, arg) == 9 && STRNCMP(arg, u8("firstline"), 9) == 0)
                                || (BDIFF(p, arg) == 8 && STRNCMP(arg, u8("lastline"), 8) == 0))
                        {
                            if (!eap.skip)
                                emsg2(u8("E125: Illegal argument: %s"), arg);
                            break;
                        }

                        byte c = p.at(0);
                        p.be(0, NUL);
                        arg = STRDUP(arg);

                        /* Check for duplicate argument name. */
                        for (int i = 0; i < newargs.ga_len; i++)
                            if (STRCMP(newargs.ga_data[i], arg) == 0)
                            {
                                emsg2(u8("E853: Duplicate argument name: %s"), arg);
                                break theend;
                            }

                        newargs.ga_grow(1);
                        newargs.ga_data[newargs.ga_len++] = arg;

                        p.be(0, c);
                        if (p.at(0) == (byte)',')
                            p = p.plus(1);
                        else
                            mustend = true;
                    }
                    p = skipwhite(p);
                    if (mustend && p.at(0) != (byte)')')
                    {
                        if (!eap.skip)
                            emsg2(e_invarg2, eap.arg);
                        break;
                    }
                }
                p = p.plus(1);        /* skip the ')' */

                int flags = 0;
                /* find extra arguments "range", "dict" and "abort" */
                for ( ; ; )
                {
                    p = skipwhite(p);
                    if (STRNCMP(p, u8("range"), 5) == 0)
                    {
                        flags |= FC_RANGE;
                        p = p.plus(5);
                    }
                    else if (STRNCMP(p, u8("dict"), 4) == 0)
                    {
                        flags |= FC_DICT;
                        p = p.plus(4);
                    }
                    else if (STRNCMP(p, u8("abort"), 5) == 0)
                    {
                        flags |= FC_ABORT;
                        p = p.plus(5);
                    }
                    else
                        break;
                }

                /* When there is a line break use what follows for the function body.
                 * Makes 'exe "func Test()\n...\nendfunc"' work. */
                Bytes line_arg = null;
                if (p.at(0) == (byte)'\n')
                    line_arg = p.plus(1);
                else if (p.at(0) != NUL && p.at(0) != (byte)'"' && !eap.skip && !did_emsg)
                    emsg(e_trailing);

                /*
                 * Read the body of the function, until ":endfunction" is found.
                 */
                if (keyTyped)
                {
                    /* Check if the function already exists, don't let the user type the
                     * whole function before telling him it doesn't work!  For a script we
                     * need to skip the body to be able to find what follows. */
                    if (!eap.skip && !eap.forceit)
                    {
                        if (fudi.fd_dict != null && fudi.fd_newkey == null)
                            emsg(e_funcdict);
                        else if (name != null && find_func(name) != null)
                            emsg_funcname(e_funcexts, name);
                    }

                    if (!eap.skip && did_emsg)
                        break theend;

                    msg_putchar('\n');          /* don't overwrite the function name */
                    cmdline_row = msg_row;
                }

                int indent = 2;
                int nesting = 0;
                for ( ; ; )
                {
                    if (keyTyped)
                    {
                        msg_scroll = true;
                        saved_wait_return = false;
                    }
                    need_wait_return = false;
                    long sourcing_lnum_off = sourcing_lnum;

                    Bytes theline;
                    if (line_arg != null)
                    {
                        /* Use eap.arg, split up in parts by line breaks. */
                        theline = line_arg;
                        p = vim_strchr(theline, '\n');
                        if (p == null)
                            line_arg = line_arg.plus(strlen(line_arg));
                        else
                        {
                            p.be(0, NUL);
                            line_arg = p.plus(1);
                        }
                    }
                    else if (eap.getline == null)
                        theline = getcmdline(':', 0L, indent);
                    else
                        theline = eap.getline.getline(':', eap.cookie, indent);
                    if (keyTyped)
                        lines_left = (int)Rows[0] - 1;
                    if (theline == null)
                    {
                        emsg(u8("E126: Missing :endfunction"));
                        break theend;
                    }

                    /* Detect line continuation: sourcing_lnum increased more than one. */
                    if (sourcing_lnum_off + 1 < sourcing_lnum)
                        sourcing_lnum_off = sourcing_lnum - sourcing_lnum_off - 1;
                    else
                        sourcing_lnum_off = 0;

                    if (skip_until != null)
                    {
                        /* Between ":append" and "." and between ":python <<EOF" and "EOF"
                         * don't check for ":endfunc". */
                        if (STRCMP(theline, skip_until) == 0)
                            skip_until = null;
                    }
                    else
                    {
                        /* skip ':' and blanks */
                        for (p = theline; vim_iswhite(p.at(0)) || p.at(0) == (byte)':'; p = p.plus(1))
                            ;

                        /* Check for "endfunction". */
                        boolean b;
                        { Bytes[] __ = { p }; b = checkforcmd(__, u8("endfunction"), 4); p = __[0]; }
                        if (b && nesting-- == 0)
                            break;

                        /* Increase indent inside "if", "while", "for" and "try", decrease at "end". */
                        if (2 < indent && STRNCMP(p, u8("end"), 3) == 0)
                            indent -= 2;
                        else if (STRNCMP(p, u8("if"), 2) == 0
                            || STRNCMP(p, u8("wh"), 2) == 0
                            || STRNCMP(p, u8("for"), 3) == 0
                            || STRNCMP(p, u8("try"), 3) == 0)
                            indent += 2;

                        /* Check for defining a function inside this function. */
                        { Bytes[] __ = { p }; b = checkforcmd(__, u8("function"), 2); p = __[0]; }
                        if (b)
                        {
                            if (p.at(0) == (byte)'!')
                                p = skipwhite(p.plus(1));
                            p = p.plus(eval_fname_script(p));
                            { Bytes[] __ = { p }; trans_function_name(__, true, 0, null); p = __[0]; }
                            if (skipwhite(p).at(0) == (byte)'(')
                            {
                                nesting++;
                                indent += 2;
                            }
                        }

                        /* Check for ":append" or ":insert". */
                        p = skip_range(p, null);
                        if ((p.at(0) == (byte)'a' && (!asc_isalpha(p.at(1)) || p.at(1) == (byte)'p'))
                                || (p.at(0) == (byte)'i'
                                    && (!asc_isalpha(p.at(1)) || (p.at(1) == (byte)'n'
                                            && (!asc_isalpha(p.at(2)) || (p.at(2) == (byte)'s'))))))
                            skip_until = STRDUP(u8("."));

                        /* Check for ":python <<EOF", ":tcl <<EOF", etc. */
                        Bytes arg = skipwhite(skiptowhite(p));
                        if (arg.at(0) == (byte)'<' && arg.at(1) == (byte)'<'
                                && ((p.at(0) == (byte)'p' && p.at(1) == (byte)'y' && (!asc_isalpha(p.at(2)) || p.at(2) == (byte)'t'))
                                || (p.at(0) == (byte)'p' && p.at(1) == (byte)'e' && (!asc_isalpha(p.at(2)) || p.at(2) == (byte)'r'))
                                || (p.at(0) == (byte)'t' && p.at(1) == (byte)'c' && (!asc_isalpha(p.at(2)) || p.at(2) == (byte)'l'))
                                || (p.at(0) == (byte)'l' && p.at(1) == (byte)'u' && p.at(2) == (byte)'a' && !asc_isalpha(p.at(3)))
                                || (p.at(0) == (byte)'r' && p.at(1) == (byte)'u' && p.at(2) == (byte)'b' && (!asc_isalpha(p.at(3)) || p.at(3) == (byte)'y'))
                                || (p.at(0) == (byte)'m' && p.at(1) == (byte)'z' && (!asc_isalpha(p.at(2)) || p.at(2) == (byte)'s'))))
                        {
                            /* ":python <<" continues until a dot, like ":append" */
                            p = skipwhite(arg.plus(2));
                            if (p.at(0) == NUL)
                                skip_until = STRDUP(u8("."));
                            else
                                skip_until = STRDUP(p);
                        }
                    }

                    /* Copy the line to newly allocated memory.
                     * get_one_sourceline() allocates 250 bytes per line, this saves 80% on average.
                     * The cost is an extra alloc/free. */
                    theline = STRDUP(theline);

                    /* Add the line to the function. */
                    newlines.ga_grow((int)(1 + sourcing_lnum_off));

                    newlines.ga_data[newlines.ga_len++] = theline;

                    /* Add null lines for continuation lines,
                     * so that the line count is equal to the index in the growarray. */
                    while (0 < sourcing_lnum_off--)
                        newlines.ga_data[newlines.ga_len++] = null;

                    /* Check for end of eap.arg. */
                    if (line_arg != null && line_arg.at(0) == NUL)
                        line_arg = null;
                }

                /* Don't define the function when skipping commands or when an error was detected. */
                if (eap.skip || did_emsg)
                    break theend;

                /*
                 * If there are no errors, add the function
                 */
                ufunc_C fp;
                if (fudi.fd_dict == null)
                {
                    hashtab_C[] ht = new hashtab_C[1];
                    dictitem_C v = find_var(name, ht, false);
                    if (v != null && v.di_tv.tv_type == VAR_FUNC)
                    {
                        emsg_funcname(u8("E707: Function name conflicts with variable: %s"), name);
                        break theend;
                    }

                    fp = find_func(name);
                    if (fp != null)
                    {
                        if (!eap.forceit)
                        {
                            emsg_funcname(e_funcexts, name);
                            break theend;
                        }
                        if (0 < fp.uf_calls)
                        {
                            emsg_funcname(u8("E127: Cannot redefine function %s: It is in use"), name);
                            break theend;
                        }
                        /* redefine existing function */
                        fp.uf_lines.ga_clear();
                        fp.uf_args.ga_clear();
                        name = null;
                    }
                }
                else
                {
                    Bytes numbuf = new Bytes(20);

                    fp = null;
                    if (fudi.fd_newkey == null && !eap.forceit)
                    {
                        emsg(e_funcdict);
                        break theend;
                    }
                    if (fudi.fd_di == null)
                    {
                        /* Can't add a function to a locked dictionary. */
                        if (tv_check_lock(fudi.fd_dict.dv_lock, eap.arg))
                            break theend;
                    }
                        /* Can't change an existing function if it is locked. */
                    else if (tv_check_lock(fudi.fd_di.di_tv.tv_lock, eap.arg))
                        break theend;

                    /* Give the function a sequential number.  Can only be used with a Funcref! */
                    libC.sprintf(numbuf, u8("%d"), ++func_nr);
                    name = STRDUP(numbuf);
                }

                if (fp == null)
                {
                    if (fudi.fd_dict == null && vim_strchr(name, AUTOLOAD_CHAR) != null)
                    {
                        /* Check that the autoload name matches the script name. */
                        boolean eq = false;
                        if (sourcing_name != null)
                        {
                            Bytes scriptname = autoload_name(name);
                            if (scriptname != null)
                            {
                                p = vim_strchr(scriptname, '/');
                                int plen = strlen(p), slen = strlen(sourcing_name);
                                if (plen < slen && STRCMP(p, sourcing_name.plus(slen - plen)) == 0)
                                    eq = true;
                            }
                        }
                        if (!eq)
                        {
                            emsg2(u8("E746: Function name does not match script file name: %s"), name);
                            break theend;
                        }
                    }

                    fp = new ufunc_C();
                    fp.uf_name = new Bytes(strlen(name) + 1);

                    if (fudi.fd_dict != null)
                    {
                        if (fudi.fd_di == null)
                        {
                            /* add new dict entry */
                            fudi.fd_di = dictitem_alloc(fudi.fd_newkey);
                            if (!dict_add(fudi.fd_dict, fudi.fd_di))
                                break theend;
                        }
                        else
                            /* overwrite existing dict entry */
                            clear_tv(fudi.fd_di.di_tv);
                        fudi.fd_di.di_tv.tv_type = VAR_FUNC;
                        fudi.fd_di.di_tv.tv_lock = 0;
                        fudi.fd_di.di_tv.tv_string = STRDUP(name);
                        fp.uf_refcount = 1;

                        /* behave like "dict" was used */
                        flags |= FC_DICT;
                    }

                    /* insert the new function in the function list */
                    STRCPY(fp.uf_name, name);
                    hash_add(func_hashtab, fp, fp.uf_name);
                }
                COPY_garray(fp.uf_args, newargs);
                COPY_garray(fp.uf_lines, newlines);
                fp.uf_varargs = varargs;
                fp.uf_flags = flags;
                fp.uf_calls = 0;
                fp.uf_script_ID = current_SID;
            }

            did_emsg |= saved_did_emsg;
            need_wait_return |= saved_wait_return;
        }
    };

    /*
     * Get a function name, translating "<SID>" and "<SNR>".
     * Also handles a Funcref in a List or Dictionary.
     * Returns the function name in allocated memory, or null for failure.
     * flags:
     * TFN_INT:         internal function name OK
     * TFN_QUIET:       be quiet
     * TFN_NO_AUTOLOAD: do not use script autoloading
     * Advances "pp" to just after the function name (if no error).
     */
    /*private*/ static Bytes trans_function_name(Bytes[] pp, boolean skip, int flags, funcdict_C fdp)
        /* skip: only find the end, don't evaluate */
        /* fdp: return: info about dictionary used */
    {
        Bytes name = null;

        if (fdp != null)
            ZER0_funcdict(fdp);

        Bytes start = pp[0];

        /* Check for hard coded <SNR>: already translated function ID (from a user command). */
        if (pp[0].at(0) == KB_SPECIAL && pp[0].at(1) == KS_EXTRA && pp[0].at(2) == KE_SNR)
        {
            pp[0] = pp[0].plus(3);
            int len = get_id_len(pp) + 3;
            return STRNDUP(start, len);
        }

        /* A name starting with "<SID>" or "<SNR>" is local to a script.
         * But don't skip over "s:", get_lval() needs it for "s:dict.func". */
        int lead = eval_fname_script(start);
        if (2 < lead)
            start = start.plus(lead);

        theend:
        {
            /* Note that TFN_ flags use the same values as GLV_ flags. */
            lval_C lv = new lval_C();
            Bytes end = get_lval(start, null, lv, false, skip, flags, (2 < lead) ? 0 : FNE_CHECK_START);
            if (BEQ(end, start))
            {
                if (!skip)
                    emsg(u8("E129: Function name required"));
                break theend;
            }
            if (end == null || (lv.ll_tv != null && (2 < lead || lv.ll_range)))
            {
                /*
                 * Report an invalid expression in braces, unless the expression evaluation
                 * has been cancelled due to an aborting error, an interrupt, or an exception.
                 */
                if (!aborting())
                {
                    if (end != null)
                        emsg2(e_invarg2, start);
                }
                else
                    pp[0] = find_name_end(start, null, null, FNE_INCL_BR);
                break theend;
            }

            if (lv.ll_tv != null)
            {
                if (fdp != null)
                {
                    fdp.fd_dict = lv.ll_dict;
                    fdp.fd_newkey = lv.ll_newkey;
                    lv.ll_newkey = null;
                    fdp.fd_di = lv.ll_di;
                }
                if (lv.ll_tv.tv_type == VAR_FUNC && lv.ll_tv.tv_string != null)
                {
                    name = STRDUP(lv.ll_tv.tv_string);
                    pp[0] = end;
                }
                else
                {
                    if (!skip && (flags & TFN_QUIET) == 0
                            && (fdp == null || lv.ll_dict == null || fdp.fd_newkey == null))
                        emsg(e_funcref);
                    else
                        pp[0] = end;
                    name = null;
                }
                break theend;
            }

            if (lv.ll_name == null)
            {
                /* Error found, but continue after the function name. */
                pp[0] = end;
                break theend;
            }

            /* Check if the name is a Funcref.  If so, use the value. */
            int[] len = new int[1];
            if (lv.ll_exp_name != null)
            {
                len[0] = strlen(lv.ll_exp_name);
                name = deref_func_name(lv.ll_exp_name, len, (flags & TFN_NO_AUTOLOAD) != 0);
                if (BEQ(name, lv.ll_exp_name))
                    name = null;
            }
            else
            {
                len[0] = BDIFF(end, pp[0]);
                name = deref_func_name(pp[0], len, (flags & TFN_NO_AUTOLOAD) != 0);
                if (BEQ(name, pp[0]))
                    name = null;
            }
            if (name != null)
            {
                name = STRDUP(name);
                pp[0] = end;
                if (STRNCMP(name, u8("<SNR>"), 5) == 0)
                {
                    /* Change "<SNR>" to the byte sequence. */
                    name.be(0, KB_SPECIAL);
                    name.be(1, KS_EXTRA);
                    name.be(2, KE_SNR);
                    BCOPY(name, 3, name, 5, strlen(name, 5) + 1);
                }
                break theend;
            }

            if (lv.ll_exp_name != null)
            {
                len[0] = strlen(lv.ll_exp_name);
                if (lead <= 2 && BEQ(lv.ll_name, lv.ll_exp_name) && STRNCMP(lv.ll_name, u8("s:"), 2) == 0)
                {
                    /* When there was "s:" already or the name expanded
                     * to get a leading "s:", then remove it. */
                    lv.ll_name = lv.ll_name.plus(2);
                    len[0] -= 2;
                    lead = 2;
                }
            }
            else
            {
                /* skip over "s:" and "g:" */
                if (lead == 2 || (lv.ll_name.at(0) == (byte)'g' && lv.ll_name.at(1) == (byte)':'))
                    lv.ll_name = lv.ll_name.plus(2);
                len[0] = BDIFF(end, lv.ll_name);
            }

            Bytes sid_buf = new Bytes(20);

            /*
             * Copy the function name to allocated memory.
             * Accept <SID>name() inside a script, translate into <SNR>123_name().
             * Accept <SNR>123_name() outside a script.
             */
            if (skip)
                lead = 0;       /* do nothing */
            else if (0 < lead)
            {
                lead = 3;
                if ((lv.ll_exp_name != null && eval_fname_sid(lv.ll_exp_name)) || eval_fname_sid(pp[0]))
                {
                    /* It's "s:" or "<SID>". */
                    if (current_SID <= 0)
                    {
                        emsg(e_usingsid);
                        break theend;
                    }
                    libC.sprintf(sid_buf, u8("%ld_"), (long)current_SID);
                    lead += strlen(sid_buf);
                }
            }
            else if ((flags & TFN_INT) == 0 && builtin_function(lv.ll_name, len[0]))
            {
                emsg2(u8("E128: Function name must start with a capital or \"s:\": %s"), start);
                break theend;
            }
            if (!skip && (flags & TFN_QUIET) == 0)
            {
                Bytes cp = vim_strchr(lv.ll_name, ':');

                if (cp != null && BLT(cp, end))
                {
                    emsg2(u8("E884: Function name cannot contain a colon: %s"), start);
                    break theend;
                }
            }

            name = new Bytes(len[0] + lead + 1);

            if (0 < lead)
            {
                name.be(0, KB_SPECIAL);
                name.be(1, KS_EXTRA);
                name.be(2, KE_SNR);
                if (3 < lead)                       /* if it's "<SID>" */
                    STRCPY(name.plus(3), sid_buf);
            }
            BCOPY(name, lead, lv.ll_name, 0, len[0]);
            name.be(lead + len[0], NUL);

            pp[0] = end;
            clear_lval(lv);
        }

        return name;
    }

    /*
     * Return 5 if "p" starts with "<SID>" or "<SNR>" (ignoring case).
     * Return 2 if "p" starts with "s:".
     * Return 0 otherwise.
     */
    /*private*/ static int eval_fname_script(Bytes p)
    {
        if (p.at(0) == (byte)'<'
            && (STRNCASECMP(p.plus(1), u8("SID>"), 4) == 0
             || STRNCASECMP(p.plus(1), u8("SNR>"), 4) == 0))
            return 5;
        if (p.at(0) == (byte)'s' && p.at(1) == (byte)':')
            return 2;

        return 0;
    }

    /*
     * Return true if "p" starts with "<SID>" or "s:".
     * Only works if eval_fname_script() returned non-zero for "p"!
     */
    /*private*/ static boolean eval_fname_sid(Bytes p)
    {
        return (p.at(0) == (byte)'s' || asc_toupper(p.at(2)) == 'I');
    }

    /*
     * List the head of the function: "name(arg1, arg2)".
     */
    /*private*/ static void list_func_head(ufunc_C fp, boolean indent)
    {
        msg_start();
        if (indent)
            msg_puts(u8("   "));
        msg_puts(u8("function "));
        if (fp.uf_name.at(0) == KB_SPECIAL)
        {
            msg_puts_attr(u8("<SNR>"), hl_attr(HLF_8));
            msg_puts(fp.uf_name.plus(3));
        }
        else
            msg_puts(fp.uf_name);
        msg_putchar('(');
        int j;
        for (j = 0; j < fp.uf_args.ga_len; j++)
        {
            if (j != 0)
                msg_puts(u8(", "));
            msg_puts(fp.uf_args.ga_data[j]);
        }
        if (fp.uf_varargs)
        {
            if (j != 0)
                msg_puts(u8(", "));
            msg_puts(u8("..."));
        }
        msg_putchar(')');
        if ((fp.uf_flags & FC_ABORT) != 0)
            msg_puts(u8(" abort"));
        if ((fp.uf_flags & FC_RANGE) != 0)
            msg_puts(u8(" range"));
        if ((fp.uf_flags & FC_DICT) != 0)
            msg_puts(u8(" dict"));
        msg_clr_eos();
        if (0 < p_verbose[0])
            last_set_msg(fp.uf_script_ID);
    }

    /*
     * Find a function by name, return pointer to it in ufuncs.
     * Return null for unknown function.
     */
    /*private*/ static ufunc_C find_func(Bytes name)
    {
        hashitem_C hi = hash_find(func_hashtab, name);

        if (!hashitem_empty(hi))
            return (ufunc_C)hi.hi_data;

        return null;
    }

    /*private*/ static boolean translated_function_exists(Bytes name)
    {
        if (builtin_function(name, -1))
            return (0 <= find_internal_func(name));

        return (find_func(name) != null);
    }

    /*
     * Return true if a function "name" exists.
     */
    /*private*/ static boolean function_exists(Bytes name)
    {
        boolean n = false;

        Bytes[] nm = { name };
        Bytes p = trans_function_name(nm, false, TFN_INT|TFN_QUIET|TFN_NO_AUTOLOAD, null);
        nm[0] = skipwhite(nm[0]);

        /* Only accept "funcname", "funcname ", "funcname (..." and
         * "funcname(...", not "funcname!...". */
        if (p != null && (nm[0].at(0) == NUL || nm[0].at(0) == (byte)'('))
            n = translated_function_exists(p);

        return n;
    }

    /*
     * Return true if "name" looks like a builtin function name:
     * starts with a lower case letter and doesn't contain AUTOLOAD_CHAR.
     * "len" is the length of "name", or -1 for NUL terminated.
     */
    /*private*/ static boolean builtin_function(Bytes name, int len)
    {
        if (!asc_islower(name.at(0)))
            return false;

        Bytes p = vim_strchr(name, AUTOLOAD_CHAR);
        return (p == null) || (0 < len && BLT(name.plus(len), p));
    }

    /*
     * If "name" has a package name try autoloading the script for it.
     * Return true if a package was loaded.
     */
    /*private*/ static boolean script_autoload(Bytes name, boolean reload)
        /* reload: load script again when already loaded */
    {
        boolean ret = false;

        /* If there is no '#' after name[0] there is no package name. */
        Bytes p = vim_strchr(name, AUTOLOAD_CHAR);
        if (p == null || BEQ(p, name))
            return false;

        Bytes scriptname = autoload_name(name);
        Bytes tofree = scriptname;

        /* Find the name in the list of previously loaded package names.
         * Skip "autoload/", it's always the same. */
        int i;
        for (i = 0; i < ga_loaded.ga_len; i++)
            if (STRCMP(ga_loaded.ga_data[i].plus(9), scriptname.plus(9)) == 0)
                break;
        if (!reload && i < ga_loaded.ga_len)
            ret = false;        /* was loaded already */
        else
        {
            /* Remember the name if it wasn't loaded already. */
            if (i == ga_loaded.ga_len)
            {
                ga_loaded.ga_grow(1);
                ga_loaded.ga_data[ga_loaded.ga_len++] = scriptname;
                tofree = null;
            }

            /* Try loading the package from autoload/<name>.vim. */
            if (source_runtime(scriptname, false) == true)
                ret = true;
        }

        return ret;
    }

    /*
     * Return the autoload script name for a function or variable name.
     */
    /*private*/ static Bytes autoload_name(Bytes name)
    {
        /* Get the script file name: replace '#' with '/', append ".vim". */
        Bytes scriptname = new Bytes(strlen(name) + 14);

        STRCPY(scriptname, u8("autoload/"));
        STRCAT(scriptname, name);
        vim_strrchr(scriptname, AUTOLOAD_CHAR).be(0, NUL);
        STRCAT(scriptname, u8(".vim"));
        for (Bytes p; (p = vim_strchr(scriptname, AUTOLOAD_CHAR)) != null; )
            p.be(0, (byte)'/');

        return scriptname;
    }

    /*private*/ static long uf__done;
    /*private*/ static hashitem_C[] uf__hi;
    /*private*/ static int uf__i;

    /*
     * Function given to expandGeneric() to obtain the list of user defined function names.
     */
    /*private*/ static final expfun_C get_user_func_name = new expfun_C()
    {
        public Bytes expand(expand_C xp, int idx)
        {
            if (idx == 0)
            {
                uf__done = 0;
                uf__hi = func_hashtab.ht_buckets;
                uf__i = 0;
            }

            if (uf__done < func_hashtab.ht_used)
            {
                if (0 < uf__done++)
                    uf__i++;
                while (hashitem_empty(uf__hi[uf__i]))
                    uf__i++;
                ufunc_C fp = (ufunc_C)uf__hi[uf__i].hi_data;

                if ((fp.uf_flags & FC_DICT) != 0)
                    return u8("");                              /* don't show dict functions */

                if (IOSIZE <= strlen(fp.uf_name) + 4)
                    return fp.uf_name;                          /* prevents overflow */

                cat_func_name(ioBuff, fp);
                if (xp.xp_context != EXPAND_USER_FUNC)
                {
                    STRCAT(ioBuff, u8("("));
                    if (!fp.uf_varargs && fp.uf_args.ga_len == 0)
                        STRCAT(ioBuff, u8(")"));
                }
                return ioBuff;
            }

            return null;
        }
    };

    /*
     * Copy the function name of "fp" to buffer "buf".
     * "buf" must be able to hold the function name plus three bytes.
     * Takes care of script-local function names.
     */
    /*private*/ static void cat_func_name(Bytes buf, ufunc_C fp)
    {
        if (fp.uf_name.at(0) == KB_SPECIAL)
        {
            STRCPY(buf, u8("<SNR>"));
            STRCAT(buf, fp.uf_name.plus(3));
        }
        else
            STRCPY(buf, fp.uf_name);
    }

    /*
     * ":delfunction {name}"
     */
    /*private*/ static final ex_func_C ex_delfunction = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes[] p = { eap.arg };
            funcdict_C fudi = new funcdict_C();
            Bytes name = trans_function_name(p, eap.skip, 0, fudi);
            fudi.fd_newkey = null;
            if (name == null)
            {
                if (fudi.fd_dict != null && !eap.skip)
                    emsg(e_funcref);
                return;
            }
            if (!ends_excmd(skipwhite(p[0]).at(0)))
            {
                emsg(e_trailing);
                return;
            }

            eap.nextcmd = check_nextcmd(p[0]);
            if (eap.nextcmd != null)
                p[0].be(0, NUL);

            ufunc_C fp = null;
            if (!eap.skip)
                fp = find_func(name);

            if (!eap.skip)
            {
                if (fp == null)
                {
                    emsg2(e_nofunc, eap.arg);
                    return;
                }
                if (0 < fp.uf_calls)
                {
                    emsg2(u8("E131: Cannot delete function %s: It is in use"), eap.arg);
                    return;
                }

                if (fudi.fd_dict != null)
                {
                    /* Delete the dict item that refers to the function,
                     * it will invoke func_unref() and possibly delete the function. */
                    dictitem_remove(fudi.fd_dict, fudi.fd_di);
                }
                else
                    func_free(fp);
            }
        }
    };

    /*
     * Free a function and remove it from the list of functions.
     */
    /*private*/ static void func_free(ufunc_C fp)
    {
        /* clear this function */
        fp.uf_args.ga_clear();
        fp.uf_lines.ga_clear();

        /* remove the function from the function hashtable */
        hashitem_C hi = hash_find(func_hashtab, fp.uf_name);

        if (hashitem_empty(hi))
            emsg2(e_intern2, u8("func_free()"));
        else
            hash_remove(func_hashtab, hi);
    }

    /*
     * Unreference a Function: decrement the reference count and free it when it becomes zero.
     * Only for numbered functions.
     */
    /*private*/ static void func_unref(Bytes name)
    {
        if (name != null && asc_isdigit(name.at(0)))
        {
            ufunc_C fp = find_func(name);

            if (fp == null)
                emsg2(e_intern2, u8("func_unref()"));
            else if (--fp.uf_refcount <= 0)
            {
                /* Only delete it when it's not being used.
                 * Otherwise it's done when "uf_calls" becomes zero. */
                if (fp.uf_calls == 0)
                    func_free(fp);
            }
        }
    }

    /*
     * Count a reference to a Function.
     */
    /*private*/ static void func_ref(Bytes name)
    {
        if (name != null && asc_isdigit(name.at(0)))
        {
            ufunc_C fp = find_func(name);
            if (fp == null)
                emsg2(e_intern2, u8("func_ref()"));
            else
                fp.uf_refcount++;
        }
    }

    /*
     * Add a number variable "name" to dict "dp" with value "nr".
     */
    /*private*/ static void add_nr_var(dict_C dp, Bytes name, long nr)
    {
        dictitem_C di = dictitem_alloc(name);

        di.di_flags = DI_FLAGS_RO;
        hash_add(dp.dv_hashtab, di, di.di_key);
        di.di_tv.tv_type = VAR_NUMBER;
        di.di_tv.tv_lock = VAR_FIXED;
        di.di_tv.tv_number = nr;
    }

    /*private*/ static int _1_depth;

    /*
     * Call a user function.
     */
    /*private*/ static void call_user_func(ufunc_C fp, int argcount, typval_C[] argvars, typval_C rtv, long firstline, long lastline, dict_C selfdict)
        /* fp: pointer to function */
        /* argcount: nr of args */
        /* argvars: arguments */
        /* rtv: return value */
        /* firstline: first line of range */
        /* lastline: last line of range */
        /* selfdict: Dictionary for "self" */
    {
        /* If depth of calling is getting too high, don't execute the function. */
        if (p_mfd[0] <= _1_depth)
        {
            emsg(u8("E132: Function call depth is higher than 'maxfuncdepth'"));
            rtv.tv_type = VAR_NUMBER;
            rtv.tv_number = -1;
            return;
        }
        _1_depth++;

        line_breakcheck();                      /* check for CTRL-C hit */

        funccall_C fc = new funccall_C();
        fc.caller = current_funccal;
        current_funccal = fc;
        fc.func = fp;
        fc.rtv = rtv;
        rtv.tv_number = 0;
        fc.linenr = 0;
        fc.returned = false;
        fc.level = ex_nesting_level;
        /* Check if this function has a breakpoint. */
        fc.breakpoint[0] = dbg_find_breakpoint(false, fp.uf_name, 0);
        fc.dbg_tick[0] = debug_tick;

        /*
         * Init l: variables.
         */
        init_var_dict(fc.l_vars, fc.l_vars_var, VAR_DEF_SCOPE);
        if (selfdict != null)
        {
            /* Set l:self to "selfdict". */
            dictitem_C di = dictitem_alloc(u8("self"));
            di.di_flags = DI_FLAGS_RO;
            hash_add(fc.l_vars.dv_hashtab, di, di.di_key);
            di.di_tv.tv_type = VAR_DICT;
            di.di_tv.tv_lock = 0;
            di.di_tv.tv_dict = selfdict;
            selfdict.dv_refcount++;
        }

        /*
         * Init a: variables.
         * Set a:0 to "argcount".
         * Set a:000 to a list with room for the "..." arguments.
         */
        init_var_dict(fc.l_avars, fc.l_avars_var, VAR_SCOPE);
        add_nr_var(fc.l_avars, u8("0"), argcount - fp.uf_args.ga_len);
        {
            dictitem_C di = dictitem_alloc(u8("000"));
            di.di_flags = DI_FLAGS_RO;
            hash_add(fc.l_avars.dv_hashtab, di, di.di_key);
            di.di_tv.tv_type = VAR_LIST;
            di.di_tv.tv_lock = VAR_FIXED;
            di.di_tv.tv_list = fc.l_varlist;
            ZER0_list(fc.l_varlist);
            fc.l_varlist.lv_refcount = DO_NOT_FREE_CNT;
            fc.l_varlist.lv_lock = VAR_FIXED;
        }

        Bytes numbuf = new Bytes(NUMBUFLEN);

        /*
         * Set a:firstline to "firstline" and a:lastline to "lastline".
         * Set a:name to named arguments.
         * Set a:N to the "..." arguments.
         */
        add_nr_var(fc.l_avars, u8("firstline"), firstline);
        add_nr_var(fc.l_avars, u8("lastline"), lastline);
        for (int i = 0; i < argcount; i++)
        {
            Bytes name;
            int ai = i - fp.uf_args.ga_len;
            if (ai < 0)
                /* named argument a:name */
                name = fp.uf_args.ga_data[i];
            else
            {
                /* "..." argument a:1, a:2, etc. */
                libC.sprintf(numbuf, u8("%d"), ai + 1);
                name = numbuf;
            }

            dictitem_C di = dictitem_alloc(name);

            di.di_flags = DI_FLAGS_RO;
            hash_add(fc.l_avars.dv_hashtab, di, di.di_key);

            /* Note: the values are copied directly to avoid alloc/free.
             * "argvars" must have VAR_FIXED for tv_lock. */
            COPY_typval(di.di_tv, argvars[i]);
            di.di_tv.tv_lock = VAR_FIXED;

            if (0 <= ai && ai < MAX_FUNC_ARGS)
            {
                list_append(fc.l_varlist, fc.l_listitems[ai]);
                COPY_typval(fc.l_listitems[ai].li_tv, argvars[i]);
                fc.l_listitems[ai].li_tv.tv_lock = VAR_FIXED;
            }
        }

        /* Don't redraw while executing the function. */
        redrawingDisabled++;

        Bytes save_sourcing_name = sourcing_name;
        long save_sourcing_lnum = sourcing_lnum;
        sourcing_lnum = 1;
        sourcing_name = new Bytes((save_sourcing_name == null ? 0 : strlen(save_sourcing_name)) + strlen(fp.uf_name) + 13);

        if (save_sourcing_name != null && STRNCMP(save_sourcing_name, u8("function "), 9) == 0)
            libC.sprintf(sourcing_name, u8("%s.."), save_sourcing_name);
        else
            STRCPY(sourcing_name, u8("function "));
        cat_func_name(sourcing_name.plus(strlen(sourcing_name)), fp);

        if (12 <= p_verbose[0])
        {
            no_wait_return++;
            verbose_enter_scroll();

            smsg(u8("calling %s"), sourcing_name);
            if (14 <= p_verbose[0])
            {
                Bytes buf = new Bytes(MSG_BUF_LEN);

                msg_puts(u8("("));
                for (int i = 0; i < argcount; i++)
                {
                    if (0 < i)
                        msg_puts(u8(", "));
                    if (argvars[i].tv_type == VAR_NUMBER)
                        msg_outnum(argvars[i].tv_number);
                    else
                    {
                        /* Do not want errors such as E724 here. */
                        emsg_off++;
                        Bytes s = tv2string(argvars[i], 0);
                        --emsg_off;
                        if (s != null)
                        {
                            if (MSG_BUF_CLEN < mb_string2cells(s, -1))
                            {
                                trunc_string(s, buf, MSG_BUF_CLEN, MSG_BUF_LEN);
                                s = buf;
                            }
                            msg_puts(s);
                        }
                    }
                }
                msg_puts(u8(")"));
            }
            msg_puts(u8("\n"));             /* don't overwrite this either */

            verbose_leave_scroll();
            --no_wait_return;
        }

        int save_current_SID = current_SID;
        current_SID = fp.uf_script_ID;
        boolean save_did_emsg = did_emsg;
        did_emsg = false;

        /* call do_cmdline() to execute the lines */
        do_cmdline(null, get_func_line, fc, DOCMD_NOWAIT|DOCMD_VERBOSE|DOCMD_REPEAT);

        --redrawingDisabled;

        /* when the function was aborted because of an error, return -1 */
        if ((did_emsg && (fp.uf_flags & FC_ABORT) != 0) || rtv.tv_type == VAR_UNKNOWN)
        {
            clear_tv(rtv);
            rtv.tv_type = VAR_NUMBER;
            rtv.tv_number = -1;
        }

        /* when being verbose, mention the return value */
        if (12 <= p_verbose[0])
        {
            no_wait_return++;
            verbose_enter_scroll();

            if (aborting())
                smsg(u8("%s aborted"), sourcing_name);
            else if (fc.rtv.tv_type == VAR_NUMBER)
                smsg(u8("%s returning #%ld"), sourcing_name, fc.rtv.tv_number);
            else
            {
                Bytes buf = new Bytes(MSG_BUF_LEN);

                /* The value may be very long.  Skip the middle part, so that we
                 * have some idea how it starts and ends.  smsg() would always
                 * truncate it at the end.  Don't want errors such as E724 here. */
                emsg_off++;
                Bytes s = tv2string(fc.rtv, 0);
                --emsg_off;
                if (s != null)
                {
                    if (MSG_BUF_CLEN < mb_string2cells(s, -1))
                    {
                        trunc_string(s, buf, MSG_BUF_CLEN, MSG_BUF_LEN);
                        s = buf;
                    }
                    smsg(u8("%s returning %s"), sourcing_name, s);
                }
            }
            msg_puts(u8("\n"));             /* don't overwrite this either */

            verbose_leave_scroll();
            --no_wait_return;
        }

        sourcing_name = save_sourcing_name;
        sourcing_lnum = save_sourcing_lnum;
        current_SID = save_current_SID;

        if (12 <= p_verbose[0] && sourcing_name != null)
        {
            no_wait_return++;
            verbose_enter_scroll();

            smsg(u8("continuing in %s"), sourcing_name);
            msg_puts(u8("\n"));             /* don't overwrite this either */

            verbose_leave_scroll();
            --no_wait_return;
        }

        did_emsg |= save_did_emsg;
        current_funccal = fc.caller;
        --_1_depth;

        /* If the a:000 list and the l: and a: dicts are not referenced,
         * we can free the funccall_C and what's in it. */
        if (fc.l_varlist.lv_refcount == DO_NOT_FREE_CNT
            && fc.l_vars.dv_refcount == DO_NOT_FREE_CNT
           && fc.l_avars.dv_refcount == DO_NOT_FREE_CNT)
        {
            free_funccal(fc, false);
        }
        else
        {
            /* "fc" is still in use.
             * This can happen when returning "a:000" or assigning "l:" to a global variable.
             * Link "fc" in the list for garbage collection later. */
            fc.caller = previous_funccal;
            previous_funccal = fc;

            /* Make a copy of the a: variables, since we didn't do that above. */
            for (int i = 0, todo = (int)fc.l_avars.dv_hashtab.ht_used; 0 < todo; i++)
            {
                hashitem_C hi = fc.l_avars.dv_hashtab.ht_buckets[i];
                if (!hashitem_empty(hi))
                {
                    dictitem_C di = (dictitem_C)hi.hi_data;
                    copy_tv(di.di_tv, di.di_tv);
                    --todo;
                }
            }

            /* Make a copy of the a:000 items, since we didn't do that above. */
            for (listitem_C li = fc.l_varlist.lv_first; li != null; li = li.li_next)
                copy_tv(li.li_tv, li.li_tv);
        }
    }

    /*
     * Free "fc" and what it contains.
     */
    /*private*/ static void free_funccal(funccall_C fc, boolean free_val)
        /* free_val: a: vars were allocated */
    {
        /* The a: variables typevals may not have been allocated, only free the allocated variables. */
        vars_clear_ext(fc.l_avars.dv_hashtab, free_val);

        /* free all l: variables */
        vars_clear(fc.l_vars.dv_hashtab);

        /* Free the a:000 variables if they were allocated. */
        if (free_val)
            for (listitem_C li = fc.l_varlist.lv_first; li != null; li = li.li_next)
                clear_tv(li.li_tv);
    }

    /*
     * ":return [expr]"
     */
    /*private*/ static final ex_func_C ex_return = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Bytes arg = eap.arg;

            if (current_funccal == null)
            {
                emsg(u8("E133: :return not inside a function"));
                return;
            }

            if (eap.skip)
                emsg_skip++;

            boolean returning = false;

            typval_C rtv = new typval_C();

            eap.nextcmd = null;
            boolean b;
            { Bytes[] __ = { eap.nextcmd }; b = (arg.at(0) != NUL && arg.at(0) != (byte)'|' && arg.at(0) != (byte)'\n') && eval0(arg, rtv, __, !eap.skip) != false; eap.nextcmd = __[0]; }
            if (b)
            {
                if (!eap.skip)
                    returning = do_return(eap, false, true, rtv);
                else
                    clear_tv(rtv);
            }
            /* It's safer to return also on error. */
            else if (!eap.skip)
            {
                /*
                 * Return unless the expression evaluation has been cancelled
                 * due to an aborting error, an interrupt, or an exception.
                 */
                if (!aborting())
                    returning = do_return(eap, false, true, null);
            }

            /* When skipping or the return gets pending, advance to the next command
             * in this line (!returning).  Otherwise, ignore the rest of the line.
             * Following lines will be ignored by get_func_line(). */
            if (returning)
                eap.nextcmd = null;
            else if (eap.nextcmd == null)           /* no argument */
                eap.nextcmd = check_nextcmd(arg);

            if (eap.skip)
                --emsg_skip;
        }
    };

    /*
     * Return from a function.  Possibly makes the return pending.  Also called
     * for a pending return at the ":endtry" or after returning from an extra
     * do_cmdline().  "reanimate" is used in the latter case.  "is_cmd" is set
     * when called due to a ":return" command.  "rtv" may point to a typval_C
     * with the return rtv.  Returns true when the return can be carried out,
     * false when the return gets pending.
     */
    /*private*/ static boolean do_return(exarg_C eap, boolean reanimate, boolean is_cmd, typval_C rtv)
    {
        condstack_C cstack = eap.cstack;

        if (reanimate)
            /* Undo the return. */
            current_funccal.returned = false;

        /*
         * Cleanup (and inactivate) conditionals, but stop when a try conditional
         * not in its finally clause (which then is to be executed next) is found.
         * In this case, make the ":return" pending for execution at the ":endtry".
         * Otherwise, return normally.
         */
        int idx = cleanup_conditionals(eap.cstack, 0, true);
        if (0 <= idx)
        {
            cstack.cs_pending[idx] = CSTP_RETURN;

            if (!is_cmd && !reanimate)
                /* A pending return again gets pending.  "rtv" points to an
                 * allocated variable with the rtv of the original ":return"'s
                 * argument if present or is null else. */
                cstack.cs_rv_ex[idx] = rtv;
            else
            {
                /* When undoing a return in order to make it pending, get the stored return rtv. */
                if (reanimate)
                    rtv = current_funccal.rtv;

                if (rtv != null)
                {
                    /* Store the value of the pending return. */
                    cstack.cs_rv_ex[idx] = new typval_C();
                    COPY_typval((typval_C)cstack.cs_rv_ex[idx], rtv);
                }
                else
                    cstack.cs_rv_ex[idx] = null;

                if (reanimate)
                {
                    /* The pending return value could be overwritten by a ":return"
                     * without argument in a finally clause; reset the default return value. */
                    current_funccal.rtv.tv_type = VAR_NUMBER;
                    current_funccal.rtv.tv_number = 0;
                }
            }
            report_make_pending(CSTP_RETURN, rtv);
        }
        else
        {
            current_funccal.returned = true;

            /* If the return is carried out now, store the return value.
             * For a return immediately after reanimation, the value is already there. */
            if (!reanimate && rtv != null)
            {
                clear_tv(current_funccal.rtv);
                COPY_typval(current_funccal.rtv, rtv);
            }
        }

        return (idx < 0);
    }

    /*
     * Free the variable with a pending return value.
     */
    /*private*/ static void discard_pending_return(typval_C rtv)
    {
        free_tv(rtv);
    }

    /*
     * Generate a return command for producing the value of "rtv".
     * Used by report_pending() for verbose messages.
     */
    /*private*/ static Bytes get_return_cmd(typval_C rtv)
    {
        Bytes s = null;
        if (rtv != null)
            s = echo_string(rtv, 0);
        if (s == null)
            s = u8("");

        STRCPY(ioBuff, u8(":return "));
        STRNCPY(ioBuff.plus(8), s, IOSIZE - 8);
        if (IOSIZE <= strlen(s) + 8)
            STRCPY(ioBuff.plus(IOSIZE - 4), u8("..."));
        return STRDUP(ioBuff);
    }

    /*
     * Get next function line.
     * Called by do_cmdline() to get the next line.
     * Returns allocated string, or null for end of function.
     */
    /*private*/ static final getline_C get_func_line = new getline_C()
    {
        public Bytes getline(int _c, Object cookie, int _indent)
        {
            Bytes retval;

            funccall_C fcp = (funccall_C)cookie;
            ufunc_C fp = fcp.func;

            /* If breakpoints have been added/deleted need to check for it. */
            if (fcp.dbg_tick[0] != debug_tick)
            {
                fcp.breakpoint[0] = dbg_find_breakpoint(false, fp.uf_name, sourcing_lnum);
                fcp.dbg_tick[0] = debug_tick;
            }

            Growing<Bytes> gap = fp.uf_lines;     /* growarray with function lines */
            if (((fp.uf_flags & FC_ABORT) != 0 && did_emsg && !aborted_in_try()) || fcp.returned)
                retval = null;
            else
            {
                /* Skip null lines (continuation lines). */
                while (fcp.linenr < gap.ga_len && gap.ga_data[fcp.linenr] == null)
                    fcp.linenr++;
                if (gap.ga_len <= fcp.linenr)
                    retval = null;
                else
                {
                    retval = STRDUP(gap.ga_data[fcp.linenr++]);
                    sourcing_lnum = fcp.linenr;
                }
            }

            /* Did we encounter a breakpoint? */
            if (fcp.breakpoint[0] != 0 && fcp.breakpoint[0] <= sourcing_lnum)
            {
                dbg_breakpoint(fp.uf_name, sourcing_lnum);
                /* Find next breakpoint. */
                fcp.breakpoint[0] = dbg_find_breakpoint(false, fp.uf_name, sourcing_lnum);
                fcp.dbg_tick[0] = debug_tick;
            }

            return retval;
        }
    };

    /*
     * Return true if the currently active function should be ended, because
     * a return was encountered or an error occurred.  Used inside a ":while".
     */
    /*private*/ static boolean func_has_ended(funccall_C cookie)
    {
        /* Ignore the "abort" flag if the abortion behavior has been changed
         * due to an error inside a try conditional. */
        return (((cookie.func.uf_flags & FC_ABORT) != 0 && did_emsg && !aborted_in_try()) || cookie.returned);
    }

    /*
     * return true if cookie indicates a function which "abort"s on errors.
     */
    /*private*/ static boolean func_has_abort(funccall_C cookie)
    {
        return ((cookie.func.uf_flags & FC_ABORT) != 0);
    }

    /*
     * Display script name where an item was last set.
     * Should only be invoked when 'verbose' is non-zero.
     */
    /*private*/ static void last_set_msg(int scriptID)
    {
        if (scriptID != 0)
        {
            Bytes name = get_scriptname(scriptID);
            if (name != null)
            {
                verbose_enter();
                msg_puts(u8("\n\tLast set from "));
                msg_puts(name);
                verbose_leave();
            }
        }
    }

    /*
     * Adjust a filename, according to a string of modifiers.
     * *fnamep must be NUL terminated when called.  When returning, the length is determined by *fnamelen.
     * Returns VALID_ flags or -1 for failure.
     * When there is an error, *fnamep is set to null.
     */
    /*private*/ static int modify_fname(Bytes src, int[] usedlen, Bytes[] fnamep, Bytes[] bufp, int[] fnamelen)
        /* src: string with modifiers */
        /* usedlen: characters after src that are used */
        /* fnamep: file name so far */
        /* bufp: buffer for allocated file name or null */
        /* fnamelen: length of fnamep */
    {
        int valid = 0;
        Bytes dirname = new Bytes(MAXPATHL);
        boolean has_fullname = false;

        repeat:
        for ( ; ; )
        {
            /* ":p" - full path/file_name */
            if (src.at(usedlen[0]) == (byte)':' && src.at(usedlen[0] + 1) == (byte)'p')
            {
                has_fullname = true;

                valid |= VALID_PATH;
                usedlen[0] += 2;

                /* When "/." or "/.." is used: force expansion to get rid of it. */
                Bytes p;
                for (p = fnamep[0]; p.at(0) != NUL; p = p.plus(us_ptr2len_cc(p)))
                {
                    if (vim_ispathsep(p.at(0))
                            && p.at(1) == (byte)'.'
                            && (p.at(2) == NUL
                                || vim_ispathsep(p.at(2))
                                || (p.at(2) == (byte)'.'
                                    && (p.at(3) == NUL || vim_ispathsep(p.at(3))))))
                        break;
                }

                /* fullName_save() is slow, don't use it when not needed. */
                if (p.at(0) != NUL || !vim_isAbsName(fnamep[0]))
                {
                    fnamep[0] = fullName_save(fnamep[0], p.at(0) != NUL);
                    bufp[0] = fnamep[0];
                    if (fnamep[0] == null)
                        return -1;
                }

                /* Append a path separator to a directory. */
                if (mch_isdir(fnamep[0]))
                {
                    /* Make room for one or two extra characters. */
                    fnamep[0] = STRNDUP(fnamep[0], strlen(fnamep[0]) + 2);
                    bufp[0] = fnamep[0];
                    if (fnamep[0] == null)
                        return -1;
                    add_pathsep(fnamep[0]);
                }
            }

            /* ":." - path relative to the current directory */
            /* ":~" - path relative to the home directory */
            /* ":8" - shortname path - postponed till after */
            int c;
            while (src.at(usedlen[0]) == (byte)':' && ((c = src.at(usedlen[0] + 1)) == '.' || c == '~' || c == '8'))
            {
                usedlen[0] += 2;
                if (c == '8')
                    continue;

                Bytes p, pbuf = null;
                if (!has_fullname)
                    p = pbuf = fullName_save(fnamep[0], false);
                else
                    p = fnamep[0];

                has_fullname = false;

                if (p != null)
                {
                    if (c == '.')
                    {
                        mch_dirname(dirname, MAXPATHL);
                        Bytes s = shorten_fname(p, dirname);
                        if (s != null)
                        {
                            fnamep[0] = s;
                            if (pbuf != null)
                            {
                                bufp[0] = pbuf;
                                pbuf = null;
                            }
                        }
                    }
                    else
                    {
                        vim_strncpy(dirname, p, MAXPATHL - 1);
                        /* Only replace it when it starts with '~'. */
                        if (dirname.at(0) == (byte)'~')
                            bufp[0] = fnamep[0] = STRDUP(dirname);
                    }
                }
            }

            Bytes tail = gettail(fnamep[0]);
            fnamelen[0] = strlen(fnamep[0]);

            /* ":h" - head, remove "/file_name", can be repeated */
            /* Don't remove the first "/" or "c:\". */
            while (src.at(usedlen[0]) == (byte)':' && src.at(usedlen[0] + 1) == (byte)'h')
            {
                valid |= VALID_HEAD;
                usedlen[0] += 2;
                Bytes s = get_past_head(fnamep[0]);
                while (BLT(s, tail) && after_pathsep(s, tail))
                    tail = tail.minus(us_ptr_back(fnamep[0], tail));
                fnamelen[0] = BDIFF(tail, fnamep[0]);
                if (fnamelen[0] == 0)
                {
                    /* Result is empty.  Turn it into "." to make ":cd %:h" work. */
                    bufp[0] = fnamep[0] = tail = STRDUP(u8("."));
                    fnamelen[0] = 1;
                }
                else
                {
                    while (BLT(s, tail) && !after_pathsep(s, tail))
                        tail = tail.minus(us_ptr_back(fnamep[0], tail));
                }
            }

            /* ":8" - shortname */
            if (src.at(usedlen[0]) == (byte)':' && src.at(usedlen[0] + 1) == (byte)'8')
            {
                usedlen[0] += 2;
            }

            /* ":t" - tail, just the basename */
            if (src.at(usedlen[0]) == (byte)':' && src.at(usedlen[0] + 1) == (byte)'t')
            {
                usedlen[0] += 2;
                fnamelen[0] -= BDIFF(tail, fnamep[0]);
                fnamep[0] = tail;
            }

            /* ":e" - extension, can be repeated */
            /* ":r" - root, without extension, can be repeated */
            while (src.at(usedlen[0]) == (byte)':' && (src.at(usedlen[0] + 1) == (byte)'e' || src.at(usedlen[0] + 1) == (byte)'r'))
            {
                /* find a '.' in the tail:
                 * - for second :e: before the current fname
                 * - otherwise: The last '.'
                 */
                Bytes s;
                if (src.at(usedlen[0] + 1) == (byte)'e' && BLT(tail, fnamep[0]))
                    s = fnamep[0].minus(2);
                else
                    s = fnamep[0].plus(fnamelen[0] - 1);
                for ( ; BLT(tail, s); s = s.minus(1))
                    if (s.at(0) == (byte)'.')
                        break;
                if (src.at(usedlen[0] + 1) == (byte)'e')           /* :e */
                {
                    if (BLT(tail, s))
                    {
                        fnamelen[0] += BDIFF(fnamep[0], s.plus(1));
                        fnamep[0] = s.plus(1);
                    }
                    else if (BLE(fnamep[0], tail))
                        fnamelen[0] = 0;
                }
                else                            /* :r */
                {
                    if (BLT(tail, s))       /* remove one extension */
                        fnamelen[0] = BDIFF(s, fnamep[0]);
                }
                usedlen[0] += 2;
            }

            /* ":s?pat?foo?" - substitute */
            /* ":gs?pat?foo?" - global substitute */
            if (src.at(usedlen[0]) == (byte)':'
                    && (src.at(usedlen[0] + 1) == (byte)'s'
                        || (src.at(usedlen[0] + 1) == (byte)'g' && src.at(usedlen[0] + 2) == (byte)'s')))
            {
                boolean didit = false;

                Bytes flags = u8("");
                Bytes s = src.plus(usedlen[0] + 2);
                if (src.at(usedlen[0] + 1) == (byte)'g')
                {
                    flags = u8("g");
                    s = s.plus(1);
                }

                byte sep = (s = s.plus(1)).at(-1);
                if (sep != NUL)
                {
                    /* find end of pattern */
                    Bytes p = vim_strchr(s, sep);
                    if (p != null)
                    {
                        Bytes pat = STRNDUP(s, BDIFF(p, s));

                        s = p.plus(1);
                        /* find end of substitution */
                        p = vim_strchr(s, sep);
                        if (p != null)
                        {
                            Bytes sub = STRNDUP(s, BDIFF(p, s));
                            Bytes str = STRNDUP(fnamep[0], fnamelen[0]);

                            usedlen[0] = BDIFF(p.plus(1), src);
                            s = do_string_sub(str, pat, sub, flags);
                            if (s != null)
                            {
                                fnamep[0] = s;
                                fnamelen[0] = strlen(s);
                                bufp[0] = s;
                                didit = true;
                            }
                        }
                    }
                    /* after using ":s", repeat all the modifiers */
                    if (didit)
                        continue repeat;
                }
            }

            break;
        }

        if (src.at(usedlen[0]) == (byte)':' && src.at(usedlen[0] + 1) == (byte)'S')
            usedlen[0] += 2;

        return valid;
    }

    /*
     * Perform a substitution on "str" with pattern "pat" and substitute "sub".
     * "flags" can be "g" to do a global substitute.
     * Returns an allocated string, null for error.
     */
    /*private*/ static Bytes do_string_sub(Bytes str, Bytes pat, Bytes sub, Bytes flags)
    {
        /* Make 'cpoptions' empty, so that the 'l' flag doesn't work here. */
        Bytes save_cpo = p_cpo[0];
        p_cpo[0] = EMPTY_OPTION;

        barray_C ba = new barray_C(200);

        boolean do_all = (flags.at(0) == (byte)'g');

        regmatch_C regmatch = new regmatch_C();
        regmatch.rm_ic = p_ic[0];
        regmatch.regprog = vim_regcomp(pat, RE_MAGIC + RE_STRING);
        if (regmatch.regprog != null)
        {
            Bytes zero_width = null;
            Bytes tail = str;
            Bytes end = str.plus(strlen(str));
            while (vim_regexec_nl(regmatch, str, BDIFF(tail, str)))
            {
                /* Skip empty match except for first match. */
                if (BEQ(regmatch.startp[0], regmatch.endp[0]))
                {
                    if (BEQ(zero_width, regmatch.startp[0]))
                    {
                        /* avoid getting stuck on a match with an empty string */
                        int n = us_ptr2len_cc(tail);
                        ACOPY(ba.ba_data, ba.ba_len, tail.array, tail.index, n);
                        ba.ba_len += n;
                        tail = tail.plus(n);
                        continue;
                    }
                    zero_width = regmatch.startp[0];
                }

                /*
                 * Get some space for a temporary buffer to do the substitution into.
                 * It will contain:
                 * - The text up to where the match is.
                 * - The substituted text.
                 * - The text after the match.
                 */
                int sublen = vim_regsub(regmatch, sub, tail, false, true, false);
                ba_grow(ba, BDIFF(end, tail) + sublen - BDIFF(regmatch.endp[0], regmatch.startp[0]));

                /* copy the text up to where the match is */
                int n = BDIFF(regmatch.startp[0], tail);
                ACOPY(ba.ba_data, ba.ba_len, tail.array, tail.index, n);
                /* add the substituted text */
                vim_regsub(regmatch, sub, new Bytes(ba.ba_data, ba.ba_len + n), true, true, false);
                ba.ba_len += n + sublen - 1;
                tail = regmatch.endp[0];
                if (tail.at(0) == NUL)
                    break;
                if (!do_all)
                    break;
            }

            if (ba.ba_data != null)
                STRCPY(new Bytes(ba.ba_data, ba.ba_len), tail);
        }

        Bytes retval = STRDUP(ba.ba_data != null ? new Bytes(ba.ba_data) : str);
        ba_clear(ba);

        if (p_cpo[0] == EMPTY_OPTION)
            p_cpo[0] = save_cpo;
        else
            ; /* Darn, evaluating {sub} expression changed the value. */

        return retval;
    }

    /*
     * Converts a file name into a canonical form.  It simplifies a file name into
     * its simplest form by stripping out unneeded components, if any.
     * The resulting file name is simplified in place and will either be the same
     * length as that supplied, or shorter.
     */
    /*private*/ static void simplify_filename(Bytes filename)
    {
        int components = 0;
        boolean stripping_disabled = false;
        boolean relative = true;

        Bytes p = filename;

        if (vim_ispathsep(p.at(0)))
        {
            relative = false;
            do
            {
                p = p.plus(1);
            } while (vim_ispathsep(p.at(0)));
        }

        Bytes start = p;      /* remember start after "c:/" or "/" or "///" */

        do
        {
            /* At this point "p" is pointing to the char following a single "/"
             * or "p" is at the "start" of the (absolute or relative) path name. */
            if (vim_ispathsep(p.at(0)))
                BCOPY(p, 0, p, 1, strlen(p, 1) + 1); /* remove duplicate "/" */
            else if (p.at(0) == (byte)'.' && (vim_ispathsep(p.at(1)) || p.at(1) == NUL))
            {
                if (BEQ(p, start) && relative)
                    p = p.plus(1 + (p.at(1) != NUL ? 1 : 0)); /* keep single "." or leading "./" */
                else
                {
                    /* Strip "./" or ".///".  If we are at the end of the file name
                     * and there is no trailing path separator, either strip "/." if
                     * we are after "start", or strip "." if we are at the beginning
                     * of an absolute path name . */
                    Bytes tail = p.plus(1);
                    if (p.at(1) != NUL)
                        while (vim_ispathsep(tail.at(0)))
                            tail = tail.plus(us_ptr2len_cc(tail));
                    else if (BLT(start, p))
                        p = p.minus(1);                /* strip preceding path separator */
                    BCOPY(p, tail, strlen(tail) + 1);
                }
            }
            else if (p.at(0) == (byte)'.' && p.at(1) == (byte)'.' && (vim_ispathsep(p.at(2)) || p.at(2) == NUL))
            {
                /* Skip to after ".." or "../" or "..///". */
                Bytes tail = p.plus(2);
                while (vim_ispathsep(tail.at(0)))
                    tail = tail.plus(us_ptr2len_cc(tail));

                if (0 < components)         /* strip one preceding component */
                {
                    boolean do_strip = false;

                    /* Don't strip for an erroneous file name. */
                    if (!stripping_disabled)
                    {
                        stat_C st = new stat_C();
                        /* If the preceding component does not exist in the file system, we strip it.
                         * On Unix, we don't accept a symbolic link that refers to a non-existent file. */
                        byte saved_char = p.at(-1);
                        p.be(-1, NUL);
                        if (libC.lstat(filename, st) < 0)
                            do_strip = true;
                        p.be(-1, saved_char);

                        p = p.minus(1);
                        /* Skip back to after previous '/'. */
                        while (BLT(start, p) && !after_pathsep(start, p))
                            p = p.minus(us_ptr_back(start, p));

                        if (!do_strip)
                        {
                            /* If the component exists in the file system, check that stripping
                             * won't change the meaning of the file name.  First get information
                             * about the unstripped file name.  This may fail if the component
                             * to strip is not a searchable directory (but a regular file, for
                             * instance), since the trailing "/.." cannot be applied then.  We don't
                             * strip it then since we don't want to replace an erroneous file name
                             * by a valid one, and we disable stripping of later components. */
                            saved_char = tail.at(0);
                            tail.be(0, NUL);
                            if (0 <= libC.stat(filename, st))
                                do_strip = true;
                            else
                                stripping_disabled = true;
                            tail.be(0, saved_char);
                            if (do_strip)
                            {
                                stat_C new_st = new stat_C();

                                /* On Unix, the check for the unstripped file name above works
                                 * also for a symbolic link pointing to a searchable directory.
                                 * But then the parent of the directory pointed to by the link
                                 * must be the same as the stripped file name.  (The latter exists
                                 * in the file system, since it is the component's parent directory.) */
                                if (BEQ(p, start) && relative)
                                    libC.stat(u8("."), new_st);
                                else
                                {
                                    saved_char = p.at(0);
                                    p.be(0, NUL);
                                    libC.stat(filename, new_st);
                                    p.be(0, saved_char);
                                }

                                if (new_st.st_ino() != st.st_ino() || new_st.st_dev() != st.st_dev())
                                {
                                    do_strip = false;
                                    /* We don't disable stripping of later components,
                                     * since the unstripped path name is still valid. */
                                }
                            }
                        }
                    }

                    if (!do_strip)
                    {
                        /* Skip the ".." or "../" and reset the counter for
                         * the components that might be stripped later on. */
                        p = tail;
                        components = 0;
                    }
                    else
                    {
                        /* Strip previous component.  If the result would get empty
                         * and there is no trailing path separator, leave a single
                         * "." instead.  If we are at the end of the file name and
                         * there is no trailing path separator and a preceding
                         * component is left after stripping, strip its trailing
                         * path separator as well. */
                        if (BEQ(p, start) && relative && tail.at(-1) == (byte)'.')
                        {
                            (p = p.plus(1)).be(-1, (byte)'.');
                            p.be(0, NUL);
                        }
                        else
                        {
                            if (BLT(start, p) && tail.at(-1) == (byte)'.')
                                p = p.minus(1);
                            BCOPY(p, tail, strlen(tail) + 1); /* strip previous component */
                        }

                        --components;
                    }
                }
                else if (BEQ(p, start) && !relative)               /* leading "/.." or "/../" */
                    BCOPY(p, tail, strlen(tail) + 1);         /* strip ".." or "../" */
                else
                {
                    if (BEQ(p, start.plus(2)) && p.at(-2) == (byte)'.')         /* leading "./../" */
                    {
                        BCOPY(p, -2, p, 0, strlen(p) + 1);       /* strip leading "./" */
                        tail = tail.minus(2);
                    }
                    p = tail;                                   /* skip to char after ".." or "../" */
                }
            }
            else
            {
                components++;                                   /* simple path component */
                p = getnextcomp(p);
            }
        } while (p.at(0) != NUL);
    }

    /* Growarray to store info about already sourced scripts.
     * For Unix also store the dev/ino, so that we don't have to stat() each
     * script when going through the list. */
    /*private*/ static final class scriptitem_C
    {
        Bytes       sn_name;
        boolean     sn_dev_valid;
        long        sn_dev;
        long        sn_ino;

        /*private*/ scriptitem_C()
        {
        }
    }

    /*private*/ static Growing<scriptitem_C> script_items = new Growing<scriptitem_C>(scriptitem_C.class, 4);

    /* batch mode debugging: don't save and restore typeahead */
    /*private*/ static boolean debug_greedy;

    /*private*/ static int last_cmd;
    /*private*/ static final int CMD_CONT       = 1;
    /*private*/ static final int CMD_NEXT       = 2;
    /*private*/ static final int CMD_STEP       = 3;
    /*private*/ static final int CMD_FINISH     = 4;
    /*private*/ static final int CMD_QUIT       = 5;
    /*private*/ static final int CMD_INTERRUPT  = 6;

    /*
     * do_debug(): Debug mode.
     * Repeatedly get Ex commands, until told to continue normal execution.
     */
    /*private*/ static void do_debug(Bytes cmd)
    {
        boolean save_msg_scroll = msg_scroll;

        int save_State = State;
        boolean save_did_emsg = did_emsg;
        boolean save_cmd_silent = cmd_silent;
        int save_msg_silent = msg_silent;
        int save_emsg_silent = emsg_silent;
        boolean save_redir_off = redir_off;

        Bytes tail = null;

        /* Make sure we are in raw mode and start termcap mode.  Might have side effects... */
        settmode(TMODE_RAW);
        starttermcap();

        redrawingDisabled++;        /* don't redisplay the window */
        no_wait_return++;           /* don't wait for return */

        did_emsg = false;           /* don't use error from debugged stuff */
        cmd_silent = false;         /* display commands */
        msg_silent = FALSE;         /* display messages */
        emsg_silent = FALSE;        /* display error messages */
        redir_off = true;           /* don't redirect debug commands */

        State = NORMAL;

        if (!debug_did_msg)
            msg(u8("Entering Debug mode.  Type \"cont\" to continue."));
        if (sourcing_name != null)
            msg(sourcing_name);
        if (sourcing_lnum != 0)
            smsg(u8("line %ld: %s"), sourcing_lnum, cmd);
        else
            smsg(u8("cmd: %s"), cmd);

        boolean save_ignore_script = false;
        tasave_C typeaheadbuf = new_tasave();
        boolean typeahead_saved = false;

        Bytes cmdline = null;
        /*
         * Repeat getting a command and executing it.
         */
        for ( ; ; )
        {
            msg_scroll = true;
            need_wait_return = false;
            /* Save the current typeahead buffer and replace it with an empty one.
             * This makes sure we get input from the user here and don't interfere
             * with the commands being executed.  Reset "ex_normal_busy" to avoid
             * the side effects of using ":normal".  Save the stuff buffer and make
             * it empty.  Set ignore_script to avoid reading from script input. */
            int save_ex_normal_busy = ex_normal_busy;
            ex_normal_busy = 0;
            if (!debug_greedy)
            {
                save_ignore_script = ignore_script;
                ignore_script = true;
                save_typeahead(typeaheadbuf);
                typeahead_saved = true;
            }

            cmdline = getcmdline_prompt('>', null, 0, EXPAND_NOTHING, null);

            if (typeahead_saved)
            {
                restore_typeahead(typeaheadbuf);
                typeahead_saved = false;
                ignore_script = save_ignore_script;
                save_ignore_script = false;
            }
            ex_normal_busy = save_ex_normal_busy;

            cmdline_row = msg_row;
            if (cmdline != null)
            {
                /* If this is a debug command, set "last_cmd".
                 * If not, reset "last_cmd".
                 * For a blank line use previous command. */
                Bytes p = skipwhite(cmdline);
                if (p.at(0) != NUL)
                {
                    switch (p.at(0))
                    {
                        case 'c': last_cmd = CMD_CONT;
                                  tail = u8("ont");
                                  break;
                        case 'n': last_cmd = CMD_NEXT;
                                  tail = u8("ext");
                                  break;
                        case 's': last_cmd = CMD_STEP;
                                  tail = u8("tep");
                                  break;
                        case 'f': last_cmd = CMD_FINISH;
                                  tail = u8("inish");
                                  break;
                        case 'q': last_cmd = CMD_QUIT;
                                  tail = u8("uit");
                                  break;
                        case 'i': last_cmd = CMD_INTERRUPT;
                                  tail = u8("nterrupt");
                                  break;
                        default: last_cmd = 0;
                                 break;
                    }
                    if (last_cmd != 0)
                    {
                        /* Check that the tail matches. */
                        p = p.plus(1);
                        while (p.at(0) != NUL && p.at(0) == tail.at(0))
                        {
                            p = p.plus(1);
                            tail = tail.plus(1);
                        }
                        if (asc_isalpha(p.at(0)))
                            last_cmd = 0;
                    }
                }

                if (last_cmd != 0)
                {
                    /* Execute debug command: decided where to break next and return. */
                    switch (last_cmd)
                    {
                        case CMD_CONT:
                            debug_break_level = -1;
                            break;
                        case CMD_NEXT:
                            debug_break_level = ex_nesting_level;
                            break;
                        case CMD_STEP:
                            debug_break_level = 9999;
                            break;
                        case CMD_FINISH:
                            debug_break_level = ex_nesting_level - 1;
                            break;
                        case CMD_QUIT:
                            got_int = true;
                            debug_break_level = -1;
                            break;
                        case CMD_INTERRUPT:
                            got_int = true;
                            debug_break_level = 9999;
                            /* Do not repeat ">interrupt" cmd, continue stepping. */
                            last_cmd = CMD_STEP;
                            break;
                    }
                    break;
                }

                /* don't debug this command */
                int n = debug_break_level;
                debug_break_level = -1;
                do_cmdline(cmdline, getexline, null, DOCMD_VERBOSE|DOCMD_EXCRESET);
                debug_break_level = n;
            }
            lines_left = (int)Rows[0] - 1;
        }

        --redrawingDisabled;
        --no_wait_return;

        redraw_all_later(NOT_VALID);

        need_wait_return = false;
        msg_scroll = save_msg_scroll;
        lines_left = (int)Rows[0] - 1;

        State = save_State;
        did_emsg = save_did_emsg;
        cmd_silent = save_cmd_silent;
        msg_silent = save_msg_silent;
        emsg_silent = save_emsg_silent;
        redir_off = save_redir_off;

        /* Only print the message again when typing a command before coming back here. */
        debug_did_msg = true;
    }

    /*
     * ":debug".
     */
    /*private*/ static final ex_func_C ex_debug = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int debug_break_level_save = debug_break_level;

            debug_break_level = 9999;
            do_cmdline_cmd(eap.arg);
            debug_break_level = debug_break_level_save;
        }
    };

    /*private*/ static Bytes    debug_breakpoint_name;
    /*private*/ static long     debug_breakpoint_lnum;

    /*
     * When debugging or a breakpoint is set on a skipped command, no debug prompt
     * is shown by do_one_cmd().  This situation is indicated by debug_skipped,
     * and "debug_skipped_name" is then set to the source name in the breakpoint case.
     * If a skipped command decides itself that a debug prompt should be displayed,
     * it can do so by calling dbg_check_skipped().
     */
    /*private*/ static boolean  debug_skipped;
    /*private*/ static Bytes    debug_skipped_name;

    /*
     * Go to debug mode when a breakpoint was encountered or "ex_nesting_level"
     * is at or below the break level.  But only when the line is actually executed.
     * Return true and set breakpoint_name for skipped commands that decide to execute
     * something themselves.  Called from do_one_cmd() before executing a command.
     */
    /*private*/ static void dbg_check_breakpoint(exarg_C eap)
    {
        debug_skipped = false;
        if (debug_breakpoint_name != null)
        {
            if (!eap.skip)
            {
                /* replace K_SNR with "<SNR>" */
                Bytes p;
                if (debug_breakpoint_name.at(0) == KB_SPECIAL
                        && debug_breakpoint_name.at(1) == KS_EXTRA
                        && debug_breakpoint_name.at(2) == KE_SNR)
                    p = u8("<SNR>");
                else
                    p = u8("");
                smsg(u8("Breakpoint in \"%s%s\" line %ld"),
                        p,
                        debug_breakpoint_name.plus((p.at(0) == NUL) ? 0 : 3),
                        debug_breakpoint_lnum);
                debug_breakpoint_name = null;
                do_debug(eap.cmd);
            }
            else
            {
                debug_skipped = true;
                debug_skipped_name = debug_breakpoint_name;
                debug_breakpoint_name = null;
            }
        }
        else if (ex_nesting_level <= debug_break_level)
        {
            if (!eap.skip)
                do_debug(eap.cmd);
            else
            {
                debug_skipped = true;
                debug_skipped_name = null;
            }
        }
    }

    /*
     * Go to debug mode if skipped by dbg_check_breakpoint() because eap.skip was set.
     * Return true when the debug mode is entered this time.
     */
    /*private*/ static boolean dbg_check_skipped(exarg_C eap)
    {
        if (debug_skipped)
        {
            /*
             * Save the value of got_int and reset it.
             * We don't want a previous interruption cause flushing the input buffer.
             */
            boolean prev_got_int = got_int;
            got_int = false;
            debug_breakpoint_name = debug_skipped_name;
            /* eap.skip is true */
            eap.skip = false;
            dbg_check_breakpoint(eap);
            eap.skip = true;
            got_int |= prev_got_int;

            return true;
        }

        return false;
    }

    /*
     * The list of breakpoints: dbg_breakp.
     * This is a grow-array of structs.
     */
    /*private*/ static final class debuggy_C
    {
        int         dbg_nr;             /* breakpoint number */
        int         dbg_type;           /* DBG_FUNC or DBG_FILE */
        Bytes       dbg_name;           /* function or file name */
        regprog_C   dbg_prog;           /* regexp program */
        long        dbg_lnum;           /* line number in function or file */
        boolean     dbg_forceit;        /* ! used */

        /*private*/ debuggy_C()
        {
        }
    }

    /*private*/ static Growing<debuggy_C> dbg_breakp = new Growing<debuggy_C>(debuggy_C.class, 4);

    /*private*/ static int last_breakp;     /* nr of last defined breakpoint */

    /*private*/ static final int DBG_FUNC        = 1;
    /*private*/ static final int DBG_FILE        = 2;

    /*
     * Parse the arguments of ":profile", ":breakadd" or ":breakdel" and put them in the entry
     * just after the last one in dbg_breakp.  Note that "dbg_name" is allocated.
     * Returns false for failure.
     */
    /*private*/ static boolean dbg_parsearg(Bytes arg, Growing<debuggy_C> gap)
    {
        Bytes p = arg;
        boolean here = false;

        gap.ga_grow(1);

        debuggy_C bp = gap.ga_data[gap.ga_len] = new debuggy_C();

        /* Find "func" or "file". */
        if (STRNCMP(p, u8("func"), 4) == 0)
            bp.dbg_type = DBG_FUNC;
        else if (STRNCMP(p, u8("file"), 4) == 0)
            bp.dbg_type = DBG_FILE;
        else if (STRNCMP(p, u8("here"), 4) == 0)
        {
            if (curbuf.b_ffname == null)
            {
                emsg(e_noname);
                return false;
            }
            bp.dbg_type = DBG_FILE;
            here = true;
        }
        else
        {
            emsg2(e_invarg2, p);
            return false;
        }
        p = skipwhite(p.plus(4));

        /* Find optional line number. */
        if (here)
            bp.dbg_lnum = curwin.w_cursor.lnum;
        else if (asc_isdigit(p.at(0)))
        {
            { Bytes[] __ = { p }; bp.dbg_lnum = getdigits(__); p = __[0]; }
            p = skipwhite(p);
        }
        else
            bp.dbg_lnum = 0;

        /* Find the function or file name.  Don't accept a function name with (). */
        if ((!here && p.at(0) == NUL) || (here && p.at(0) != NUL) || (bp.dbg_type == DBG_FUNC && STRSTR(p, u8("()")) != null))
        {
            emsg2(e_invarg2, arg);
            return false;
        }

        if (bp.dbg_type == DBG_FUNC)
            bp.dbg_name = STRDUP(p);
        else if (here)
            bp.dbg_name = STRDUP(curbuf.b_ffname);
        else if (p.at(0) != (byte)'*')
            bp.dbg_name = fullName_save(p, true);
        else
            bp.dbg_name = STRDUP(p);

        if (bp.dbg_name == null)
            return false;

        return true;
    }

    /*
     * ":breakadd".
     */
    /*private*/ static final ex_func_C ex_breakadd = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            Growing<debuggy_C> gap = dbg_breakp;

            if (dbg_parsearg(eap.arg, gap) == true)
            {
                debuggy_C bp = gap.ga_data[gap.ga_len];
                bp.dbg_forceit = eap.forceit;

                Bytes pat = file_pat_to_reg_pat(bp.dbg_name, null, null);
                if (pat != null)
                    bp.dbg_prog = vim_regcomp(pat, RE_MAGIC + RE_STRING);
                if (pat == null || bp.dbg_prog == null)
                    bp.dbg_name = null;
                else
                {
                    if (bp.dbg_lnum == 0)   /* default line number is 1 */
                        bp.dbg_lnum = 1;

                    bp.dbg_nr = ++last_breakp;
                    debug_tick++;

                    gap.ga_len++;
                }
            }
        }
    };

    /*
     * ":debuggreedy".
     */
    /*private*/ static final ex_func_C ex_debuggreedy = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.addr_count == 0 || eap.line2 != 0)
                debug_greedy = true;
            else
                debug_greedy = false;
        }
    };

    /*
     * ":breakdel" and ":profdel".
     */
    /*private*/ static final ex_func_C ex_breakdel = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.cmdidx == CMD_profdel)
            {
                ex_ni.ex(eap);
                return;
            }

            int todel = -1;
            boolean del_all = false;
            long best_lnum = 0;
            Growing<debuggy_C> gap = dbg_breakp;

            if (asc_isdigit(eap.arg.at(0)))
            {
                /* ":breakdel {nr}" */
                int nr = libC.atoi(eap.arg);
                for (int i = 0; i < gap.ga_len; i++)
                    if (gap.ga_data[i].dbg_nr == nr)
                    {
                        todel = i;
                        break;
                    }
            }
            else if (eap.arg.at(0) == (byte)'*')
            {
                todel = 0;
                del_all = true;
            }
            else
            {
                /* ":breakdel {func|file} [lnum] {name}" */
                if (dbg_parsearg(eap.arg, gap) == false)
                    return;

                debuggy_C bp = gap.ga_data[gap.ga_len];

                for (int i = 0; i < gap.ga_len; i++)
                {
                    debuggy_C bpi = gap.ga_data[i];
                    if (bp.dbg_type == bpi.dbg_type
                            && STRCMP(bp.dbg_name, bpi.dbg_name) == 0
                            && (bp.dbg_lnum == bpi.dbg_lnum
                                || (bp.dbg_lnum == 0
                                    && (best_lnum == 0
                                        || bpi.dbg_lnum < best_lnum))))
                    {
                        todel = i;
                        best_lnum = bpi.dbg_lnum;
                    }
                }

                bp.dbg_name = null;
            }

            if (todel < 0)
                emsg2(u8("E161: Breakpoint not found: %s"), eap.arg);
            else
            {
                for (debuggy_C[] dbg = gap.ga_data; 0 < gap.ga_len; )
                {
                    if (todel < --gap.ga_len)
                        for (int i = todel; i < gap.ga_len; i++)
                            dbg[i] = dbg[i + 1];
                    dbg[gap.ga_len] = null;
                    debug_tick++;
                    if (!del_all)
                        break;
                }

                /* If all breakpoints were removed clear the array. */
                if (gap.ga_len == 0)
                    gap.ga_clear();
            }
        }
    };

    /*
     * ":breaklist".
     */
    /*private*/ static final ex_func_C ex_breaklist = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            if (dbg_breakp.ga_len == 0)
                msg(u8("No breakpoints defined"));
            else
                for (int i = 0; i < dbg_breakp.ga_len; i++)
                {
                    debuggy_C bp = dbg_breakp.ga_data[i];

                    smsg(u8("%3d  %s %s  line %ld"),
                            bp.dbg_nr,
                            (bp.dbg_type == DBG_FUNC) ? u8("func") : u8("file"),
                            bp.dbg_name,
                            bp.dbg_lnum);
                }
        }
    };

    /*
     * Find a breakpoint for a function or sourced file.
     * Returns line number at which to break; zero when no matching breakpoint.
     */
    /*private*/ static long dbg_find_breakpoint(boolean file, Bytes fname, long after)
        /* file: true for a file, false for a function */
        /* fname: file or function name */
        /* after: after this line number */
    {
        return debuggy_find(file, fname, after, dbg_breakp, null);
    }

    /*
     * Common code for dbg_find_breakpoint().
     */
    /*private*/ static long debuggy_find(boolean file, Bytes fname, long after, Growing<debuggy_C> gap, boolean[] fp)
        /* file: true for a file, false for a function */
        /* fname: file or function name */
        /* after: after this line number */
        /* fp: if not null: return forceit */
    {
        /* Return quickly when there are no breakpoints. */
        if (gap.ga_len == 0)
            return 0;

        Bytes name = fname;

        /* Replace K_SNR in function name with "<SNR>". */
        if (!file && fname.at(0) == KB_SPECIAL)
        {
            name = new Bytes(strlen(fname) + 3);

            STRCPY(name, u8("<SNR>"));
            STRCPY(name.plus(5), fname.plus(3));
        }

        long lnum = 0;

        for (int i = 0; i < gap.ga_len; i++)
        {
            /* Skip entries that are not useful or are for a line that is beyond an already found breakpoint. */
            debuggy_C bp = gap.ga_data[i];
            if ((bp.dbg_type == DBG_FILE) == file && (after < bp.dbg_lnum && (lnum == 0 || bp.dbg_lnum < lnum)))
            {
                /*
                 * Save the value of got_int and reset it.
                 * We don't want a previous interruption cancel matching,
                 * only hitting CTRL-C while matching should abort it.
                 */
                boolean prev_got_int = got_int;
                got_int = false;
                boolean b;
                { regprog_C[] __ = { bp.dbg_prog }; b = vim_regexec_prog(__, false, name, 0); bp.dbg_prog = __[0]; }
                if (b)
                {
                    lnum = bp.dbg_lnum;
                    if (fp != null)
                        fp[0] = bp.dbg_forceit;
                }
                got_int |= prev_got_int;
            }
        }

        return lnum;
    }

    /*
     * Called when a breakpoint was encountered.
     */
    /*private*/ static void dbg_breakpoint(Bytes name, long lnum)
    {
        /* We need to check if this line is actually executed in do_one_cmd(). */
        debug_breakpoint_name = name;
        debug_breakpoint_lnum = lnum;
    }

    /*
     * Store the current time in "tm".
     */
    /*private*/ static void profile_start(timeval_C tm)
    {
        libC._gettimeofday(tm);
    }

    /*
     * Compute the elapsed time from "tm" till now and store in "tm".
     */
    /*private*/ static void profile_end(timeval_C tm)
    {
        timeval_C now = new timeval_C();
        libC._gettimeofday(now);

        tm.tv_usec(now.tv_usec() - tm.tv_usec());
        tm.tv_sec(now.tv_sec() - tm.tv_sec());
        if (tm.tv_usec() < 0)
        {
            tm.tv_usec(tm.tv_usec() + 1000000);
            tm.tv_sec(tm.tv_sec() - 1);
        }
    }

    /*
     * Subtract the time "tm2" from "tm".
     */
    /*private*/ static void profile_sub(timeval_C tm1, timeval_C tm2)
    {
        tm1.tv_usec(tm1.tv_usec() - tm2.tv_usec());
        tm1.tv_sec(tm1.tv_sec() - tm2.tv_sec());
        if (tm1.tv_usec() < 0)
        {
            tm1.tv_usec(tm1.tv_usec() + 1000000);
            tm1.tv_sec(tm1.tv_sec() - 1);
        }
    }

    /*private*/ static Bytes profile_msg_buf = new Bytes(50);

    /*
     * Return a string that represents the time in "tm".
     * Uses a static buffer!
     */
    /*private*/ static Bytes profile_msg(timeval_C tm)
    {
        libC.sprintf(profile_msg_buf, u8("%3ld.%06ld"), tm.tv_sec(), tm.tv_usec());
        return profile_msg_buf;
    }

    /*
     * Put the time "msec" past now in "tm".
     */
    /*private*/ static void profile_setlimit(long msec, timeval_C tm)
    {
        if (msec <= 0)      /* no limit */
            profile_zero(tm);
        else
        {
            libC._gettimeofday(tm);

            long usec = tm.tv_usec() + msec * 1000;
            tm.tv_usec(usec % 1000000L);
            tm.tv_sec(tm.tv_sec() + usec / 1000000L);
        }
    }

    /*
     * Return true if the current time is past "tm".
     */
    /*private*/ static boolean profile_passed_limit(timeval_C tm)
    {
        if (tm.tv_sec() == 0)     /* timer was not set */
            return false;

        timeval_C now = new timeval_C();

        libC._gettimeofday(now);
        return (tm.tv_sec() < now.tv_sec() || (now.tv_sec() == tm.tv_sec() && tm.tv_usec() < now.tv_usec()));
    }

    /*
     * Set the time in "tm" to zero.
     */
    /*private*/ static void profile_zero(timeval_C tm)
    {
        tm.tv_usec(0);
        tm.tv_sec(0);
    }

    /*
     * If 'autowrite' option set, try to write the file.
     * Careful: autocommands may make "buf" invalid!
     *
     * return false for failure, true otherwise
     */
    /*private*/ static boolean autowrite(buffer_C buf, boolean forceit)
    {
        if (!(p_aw[0] || p_awa[0]) || !p_write[0] || (!forceit && buf.b_p_ro[0]) || buf.b_ffname == null)
            return false;

        boolean r = buf_write_all(buf, forceit);

        /* Writing may succeed but the buffer still changed, e.g. when
         * there is a conversion error.  We do want to return false then. */
        if (buf_valid(buf) && bufIsChanged(buf))
            r = false;

        return r;
    }

    /*
     * flush all buffers, except the ones that are readonly
     */
    /*private*/ static void autowrite_all()
    {
        if (!(p_aw[0] || p_awa[0]) || !p_write[0])
            return;

        for (buffer_C buf = firstbuf; buf != null; buf = buf.b_next)
            if (bufIsChanged(buf) && !buf.b_p_ro[0])
            {
                buf_write_all(buf, false);
                /* an autocommand may have deleted the buffer */
                if (!buf_valid(buf))
                    buf = firstbuf;
            }
    }

    /*
     * Return true if buffer was changed and cannot be abandoned.
     * For flags use the CCGD_ values.
     */
    /*private*/ static boolean check_changed(buffer_C buf, int flags)
    {
        boolean forceit = ((flags & CCGD_FORCEIT) != 0);

        if (!forceit
                && bufIsChanged(buf)
                && ((flags & CCGD_MULTWIN) != 0 || buf.b_nwindows <= 1)
                && ((flags & CCGD_AW) == 0 || autowrite(buf, forceit) == false))
        {
            if ((p_confirm[0] || cmdmod.confirm) && p_write[0])
            {
                int count = 0;

                if ((flags & CCGD_ALLBUF) != 0)
                    for (buffer_C buf2 = firstbuf; buf2 != null; buf2 = buf2.b_next)
                        if (bufIsChanged(buf2) && (buf2.b_ffname != null))
                            count++;
                if (!buf_valid(buf))
                    /* Autocommand deleted buffer, oops!  It's not changed now. */
                    return false;
                dialog_changed(buf, 1 < count);
                if (!buf_valid(buf))
                    /* Autocommand deleted buffer, oops!  It's not changed now. */
                    return false;

                return bufIsChanged(buf);
            }
            if ((flags & CCGD_EXCMD) != 0)
                emsg(e_nowrtmsg);
            else
                emsg(e_nowrtmsg_nobang);

            return true;
        }

        return false;
    }

    /*
     * Ask the user what to do when abandoning a changed buffer.
     * Must check 'write' option first!
     */
    /*private*/ static void dialog_changed(buffer_C buf, boolean checkall)
        /* checkall: may abandon all changed buffers */
    {
        Bytes buff = new Bytes(DIALOG_MSG_SIZE);

        dialog_msg(buff, u8("Save changes to \"%s\"?"), (buf.b_fname != null) ? buf.b_fname : u8("Untitled"));

        int ret;
        if (checkall)
            ret = vim_dialog_yesnoallcancel(buff, 1);
        else
            ret = vim_dialog_yesnocancel(buff, 1);

        exarg_C ea = new exarg_C();
        /* Init ea pseudo-structure, this is needed for the check_overwrite() function. */
        ea.append = ea.forceit = false;

        if (ret == VIM_YES)
        {
            if (buf.b_fname != null && check_overwrite(ea, buf, buf.b_fname, buf.b_ffname, false) == true)
                /* didn't hit Cancel */
                buf_write_all(buf, false);
        }
        else if (ret == VIM_NO)
        {
            unchanged(buf, true);
        }
        else if (ret == VIM_ALL)
        {
            /*
             * Write all modified files that can be written.
             * Skip readonly buffers, these need to be confirmed individually.
             */
            for (buffer_C buf2 = firstbuf; buf2 != null; buf2 = buf2.b_next)
            {
                if (bufIsChanged(buf2) && (buf2.b_ffname != null) && !buf2.b_p_ro[0])
                {
                    if (buf2.b_fname != null
                            && check_overwrite(ea, buf2, buf2.b_fname, buf2.b_ffname, false) == true)
                        /* didn't hit Cancel */
                        buf_write_all(buf2, false);
                    /* an autocommand may have deleted the buffer */
                    if (!buf_valid(buf2))
                        buf2 = firstbuf;
                }
            }
        }
        else if (ret == VIM_DISCARDALL)
        {
            /*
             * mark all buffers as unchanged
             */
            for (buffer_C buf2 = firstbuf; buf2 != null; buf2 = buf2.b_next)
                unchanged(buf2, true);
        }
    }

    /*
     * Return true if the buffer "buf" can be abandoned, either by making it
     * hidden, autowriting it or unloading it.
     */
    /*private*/ static boolean can_abandon(buffer_C buf, boolean forceit)
    {
        return (P_HID(buf)
                    || !bufIsChanged(buf)
                    || 1 < buf.b_nwindows
                    || autowrite(buf, forceit) == true
                    || forceit);
    }

    /*
     * Add a buffer number to "bufnrs", unless it's already there.
     */
    /*private*/ static int add_bufnum(int[] bufnrs, int bufnum, int nr)
    {
        for (int i = 0; i < bufnum; i++)
            if (bufnrs[i] == nr)
                return bufnum;

        bufnrs[bufnum++] = nr;
        return bufnum;
    }

    /*
     * Return true if any buffer was changed and cannot be abandoned.
     * That changed buffer becomes the current buffer.
     */
    /*private*/ static boolean check_changed_any(boolean hidden)
        /* hidden: only check hidden buffers */
    {
        boolean retval = false;

        int bufnum = 0;
        int bufcount = 0;

        buffer_C buf;
        for (buf = firstbuf; buf != null; buf = buf.b_next)
            bufcount++;

        if (bufcount == 0)
            return false;

        int[] bufnrs = new int[bufcount];

        /* curbuf */
        bufnrs[bufnum++] = curbuf.b_fnum;
        /* buf in curtab */
        for (window_C wp = firstwin; wp != null; wp = wp.w_next)
            if (wp.w_buffer != curbuf)
                bufnum = add_bufnum(bufnrs, bufnum, wp.w_buffer.b_fnum);

        /* buf in other tab */
        for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
            if (tp != curtab)
                for (window_C wp = tp.tp_firstwin; wp != null; wp = wp.w_next)
                    bufnum = add_bufnum(bufnrs, bufnum, wp.w_buffer.b_fnum);
        /* any other buf */
        for (buf = firstbuf; buf != null; buf = buf.b_next)
            bufnum = add_bufnum(bufnrs, bufnum, buf.b_fnum);

        int i;
        for (i = 0; i < bufnum; i++)
        {
            buf = buflist_findnr(bufnrs[i]);
            if (buf == null)
                continue;
            if ((!hidden || buf.b_nwindows == 0) && bufIsChanged(buf))
            {
                /* Try auto-writing the buffer.  If this fails,
                 * but the buffer no longer exists, it's not changed, that's OK. */
                if (check_changed(buf, (p_awa[0] ? CCGD_AW : 0)
                                     | CCGD_MULTWIN
                                     | CCGD_ALLBUF) && buf_valid(buf))
                    break;      /* didn't save - still changes */
            }
        }

        if (bufnum <= i)
            return retval;

        retval = true;
        exiting = false;
        /*
         * When ":confirm" used, don't give an error message.
         */
        if (!(p_confirm[0] || cmdmod.confirm))
        {
            /* There must be a wait_return for this message, do_buffer() may cause a redraw.
             * But wait_return() is a no-op when vgetc() is busy (Quit used from window menu),
             * then make sure we don't cause a scroll up. */
            if (0 < vgetc_busy)
            {
                msg_row = cmdline_row;
                msg_col = 0;
                msg_didout = false;
            }
            if (emsg2(u8("E162: No write since last change for buffer \"%s\""), buf_spname(buf, false)))
            {
                int save = no_wait_return;
                no_wait_return = FALSE;
                wait_return(FALSE);
                no_wait_return = save;
            }
        }

        buf_found:
        {
            /* Try to find a window that contains the buffer. */
            if (buf != curbuf)
            {
                for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
                    for (window_C wp = (tp == curtab) ? firstwin : tp.tp_firstwin; wp != null; wp = wp.w_next)
                        if (wp.w_buffer == buf)
                        {
                            goto_tabpage_win(tp, wp);
                            /* Paranoia: did autocms wipe out the buffer with changes? */
                            if (!buf_valid(buf))
                                return retval;
                            break buf_found;
                        }
            }
        }

        /* Open the changed buffer in the current window. */
        if (buf != curbuf)
            set_curbuf(buf, DOBUF_GOTO);

        return retval;
    }

    /*
     * Return false if there is no file name, true if there is one.
     * Give error message for false.
     */
    /*private*/ static boolean check_fname()
    {
        if (curbuf.b_ffname == null)
        {
            emsg(e_noname);
            return false;
        }
        return true;
    }

    /*
     * Flush the contents of a buffer, unless it has no file name.
     * Return false for failure, true otherwise.
     */
    /*private*/ static boolean buf_write_all(buffer_C buf, boolean forceit)
    {
        buffer_C old_curbuf = curbuf;

        boolean retval = buf_write(buf, buf.b_ffname, buf.b_fname,
                                       1, buf.b_ml.ml_line_count, null,
                                                      false, forceit, true, false);
        if (curbuf != old_curbuf)
        {
            msg_source(hl_attr(HLF_W));
            msg(u8("Warning: Entered other buffer unexpectedly (check autocommands)"));
        }

        return retval;
    }

    /*
     * Code to handle the argument list.
     */

    /*private*/ static final int AL_SET  = 1;
    /*private*/ static final int AL_ADD  = 2;
    /*private*/ static final int AL_DEL  = 3;

    /*
     * Isolate one argument, taking backticks.
     * Changes the argument in-place, puts a NUL after it.  Backticks remain.
     * Return a pointer to the start of the next argument.
     */
    /*private*/ static Bytes do_one_arg(Bytes str)
    {
        boolean inbacktick = false;

        Bytes p;
        for (p = str; str.at(0) != NUL; str = str.plus(1))
        {
            /* When the backslash is used for escaping the special meaning
             * of a character, we need to keep it until wildcard expansion. */
            if (rem_backslash(str))
            {
                (p = p.plus(1)).be(-1, (str = str.plus(1)).at(-1));
                (p = p.plus(1)).be(-1, str.at(0));
            }
            else
            {
                /* An item ends at a space not in backticks. */
                if (!inbacktick && vim_isspace(str.at(0)))
                    break;
                if (str.at(0) == (byte)'`')
                    inbacktick ^= true;
                (p = p.plus(1)).be(-1, str.at(0));
            }
        }
        str = skipwhite(str);
        p.be(0, NUL);

        return str;
    }

    /*
     * Separate the arguments in "str" and return a list of pointers in the growarray "gap".
     */
    /*private*/ static Growing<Bytes> get_arglist(Bytes str)
    {
        Growing<Bytes> gap = new Growing<Bytes>(Bytes.class, 20);

        while (str.at(0) != NUL)
        {
            gap.ga_grow(1);
            gap.ga_data[gap.ga_len++] = str;

            /* Isolate one argument, change it in-place, put a NUL after it. */
            str = do_one_arg(str);
        }

        return gap;
    }

    /*
     * "what" == AL_SET: Redefine the argument list to 'str'.
     * "what" == AL_ADD: add files in 'str' to the argument list after "after".
     * "what" == AL_DEL: remove files in 'str' from the argument list.
     *
     * Return false for failure, true otherwise.
     */
    /*private*/ static boolean do_arglist(Bytes str, int what, int after)
        /* after: 0 means before first one */
    {
        /*
         * Collect all file name arguments in "new_ga".
         */
        Growing<Bytes> new_ga = get_arglist(str);

        if (what == AL_DEL)
        {
            regmatch_C regmatch = new regmatch_C();
            regmatch.rm_ic = false;

            /*
             * Delete the items: use each item as a regexp and find a match in the argument list.
             */
            for (int i = 0; i < new_ga.ga_len && !got_int; i++)
            {
                Bytes p = new_ga.ga_data[i];
                p = file_pat_to_reg_pat(p, null, null);
                if (p == null)
                    break;

                regmatch.regprog = vim_regcomp(p, p_magic[0] ? RE_MAGIC : 0);
                if (regmatch.regprog == null)
                    break;

                boolean didone = false;
                for (int match = 0; match < curwin.w_alist.al_ga.ga_len; match++)
                {
                    aentry_C[] waep = curwin.w_alist.al_ga.ga_data;

                    if (vim_regexec(regmatch, alist_name(waep[match]), 0))
                    {
                        didone = true;

                        --curwin.w_alist.al_ga.ga_len;
                        for (int j = match; j < curwin.w_alist.al_ga.ga_len; j++)
                            waep[j] = waep[j + 1];

                        waep[curwin.w_alist.al_ga.ga_len] = null;

                        if (match < curwin.w_arg_idx)
                            --curwin.w_arg_idx;
                        --match;
                    }
                }

                if (!didone)
                    emsg2(e_nomatch2, new_ga.ga_data[i]);
            }

            new_ga.ga_clear();
        }
        else
        {
            int[] exp_count = new int[1];
            Bytes[][] exp_files = new Bytes[1][];
            boolean b = dummy_expand_wildcards(new_ga.ga_len, new_ga.ga_data,
                            exp_count, exp_files, EW_DIR|EW_FILE|EW_ADDSLASH|EW_NOTFOUND);
            new_ga.ga_clear();
            if (!b)
                return false;
            if (exp_count[0] == 0)
            {
                emsg(e_nomatch);
                return false;
            }

            if (what == AL_ADD)
                alist_add_list(exp_count[0], exp_files[0], after);
            else /* what == AL_SET */
                alist_set(curwin.w_alist, exp_count[0], exp_files[0], false, null, 0);
        }

        alist_check_arg_idx();

        return true;
    }

    /*
     * Check the validity of the arg_idx for each other window.
     */
    /*private*/ static void alist_check_arg_idx()
    {
        for (tabpage_C tp = first_tabpage; tp != null; tp = tp.tp_next)
            for (window_C wp = (tp == curtab) ? firstwin : tp.tp_firstwin; wp != null; wp = wp.w_next)
                if (wp.w_alist == curwin.w_alist)
                    check_arg_idx(wp);
    }

    /*
     * Return true if window "win" is editing the file at the current argument index.
     */
    /*private*/ static boolean editing_arg_idx(window_C win)
    {
        return !(win.w_alist.al_ga.ga_len <= win.w_arg_idx
                    || (win.w_buffer.b_fnum != win.w_alist.al_ga.ga_data[win.w_arg_idx].ae_fnum
                        && (win.w_buffer.b_ffname == null
                             || (fullpathcmp(alist_name(win.w_alist.al_ga.ga_data[win.w_arg_idx]), win.w_buffer.b_ffname, true) & FPC_SAME) == 0)));
    }

    /*
     * Check if window "win" is editing the w_arg_idx file in its argument list.
     */
    /*private*/ static void check_arg_idx(window_C win)
    {
        if (1 < win.w_alist.al_ga.ga_len && !editing_arg_idx(win))
        {
            /* We are not editing the current entry in the argument list.
             * Set "arg_had_last" if we are editing the last one. */
            win.w_arg_idx_invalid = true;
            if (win.w_arg_idx != win.w_alist.al_ga.ga_len - 1
                    && arg_had_last == false
                    && win.w_alist == global_alist
                    && 0 < global_alist.al_ga.ga_len
                    && win.w_arg_idx < global_alist.al_ga.ga_len
                    && (win.w_buffer.b_fnum == global_alist.al_ga.ga_data[global_alist.al_ga.ga_len - 1].ae_fnum
                        || (win.w_buffer.b_ffname != null
                            && (fullpathcmp(alist_name(global_alist.al_ga.ga_data[global_alist.al_ga.ga_len - 1]),
                                    win.w_buffer.b_ffname, true) & FPC_SAME) != 0)))
                arg_had_last = true;
        }
        else
        {
            /* We are editing the current entry in the argument list.
             * Set "arg_had_last" if it's also the last one. */
            win.w_arg_idx_invalid = false;
            if (win.w_arg_idx == win.w_alist.al_ga.ga_len - 1 && win.w_alist == global_alist)
                arg_had_last = true;
        }
    }

    /*
     * ":args", ":argslocal" and ":argsglobal".
     */
    /*private*/ static final ex_func_C ex_args = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (eap.cmdidx != CMD_args)
            {
                alist_unlink(curwin.w_alist);
                if (eap.cmdidx == CMD_argglobal)
                    curwin.w_alist = global_alist;
                else /* eap.cmdidx == CMD_arglocal */
                    curwin.w_alist = alist_new();
            }

            if (!ends_excmd(eap.arg.at(0)))
            {
                /*
                 * ":args file ..": define new argument list, handle like ":next"
                 * Also for ":argslocal file .." and ":argsglobal file ..".
                 */
                ex_next.ex(eap);
            }
            else if (eap.cmdidx == CMD_args)
            {
                /*
                 * ":args": list arguments.
                 */
                if (0 < curwin.w_alist.al_ga.ga_len)
                {
                    aentry_C[] waep = curwin.w_alist.al_ga.ga_data;

                    /* Overwrite the command, for a short list
                     * there is no scrolling required and no wait_return(). */
                    gotocmdline(true);
                    for (int i = 0; i < curwin.w_alist.al_ga.ga_len; i++)
                    {
                        if (i == curwin.w_arg_idx)
                            msg_putchar('[');
                        msg_outtrans(alist_name(waep[i]));
                        if (i == curwin.w_arg_idx)
                            msg_putchar(']');
                        msg_putchar(' ');
                    }
                }
            }
            else if (eap.cmdidx == CMD_arglocal)
            {
                /*
                 * ":argslocal": make a local copy of the global argument list.
                 */
                Growing<aentry_C> gga = global_alist.al_ga;
                aentry_C[] gae = gga.ga_data;

                Growing<aentry_C> wga = curwin.w_alist.al_ga;
                aentry_C[] wae = wga.ga_grow(gga.ga_len);

                for (int i = 0; i < gga.ga_len; i++)
                    if (gae[i].ae_fname != null)
                    {
                        wae[wga.ga_len] = new aentry_C();
                        wae[wga.ga_len].ae_fname = STRDUP(gae[i].ae_fname);
                        wae[wga.ga_len].ae_fnum = gae[i].ae_fnum;

                        wga.ga_len++;
                    }
            }
        }
    };

    /*
     * ":previous", ":sprevious", ":Next" and ":sNext".
     */
    /*private*/ static final ex_func_C ex_previous = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            /* If past the last one already, go to the last one. */
            if (curwin.w_alist.al_ga.ga_len <= curwin.w_arg_idx - (int)eap.line2)
                do_argfile(eap, curwin.w_alist.al_ga.ga_len - 1);
            else
                do_argfile(eap, curwin.w_arg_idx - (int)eap.line2);
        }
    };

    /*
     * ":rewind", ":first", ":sfirst" and ":srewind".
     */
    /*private*/ static final ex_func_C ex_rewind = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            do_argfile(eap, 0);
        }
    };

    /*
     * ":last" and ":slast".
     */
    /*private*/ static final ex_func_C ex_last = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            do_argfile(eap, curwin.w_alist.al_ga.ga_len - 1);
        }
    };

    /*
     * ":argument" and ":sargument".
     */
    /*private*/ static final ex_func_C ex_argument = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int i;
            if (0 < eap.addr_count)
                i = (int)(eap.line2 - 1);
            else
                i = curwin.w_arg_idx;

            do_argfile(eap, i);
        }
    };

    /*
     * Edit file "argn" of the argument lists.
     */
    /*private*/ static void do_argfile(exarg_C eap, int argn)
    {
        if (argn < 0 || curwin.w_alist.al_ga.ga_len <= argn)
        {
            if (curwin.w_alist.al_ga.ga_len <= 1)
                emsg(u8("E163: There is only one file to edit"));
            else if (argn < 0)
                emsg(u8("E164: Cannot go before first file"));
            else
                emsg(u8("E165: Cannot go beyond last file"));
        }
        else
        {
            int old_arg_idx = curwin.w_arg_idx;

            setpcmark();

            /* split window or create new tab page first */
            if (eap.cmd.at(0) == (byte)'s' || cmdmod.tab != 0)
            {
                if (win_split(0, 0) == false)
                    return;
                curwin.w_onebuf_opt.wo_scb[0] = false;
                curwin.w_onebuf_opt.wo_crb[0] = false;
            }
            else
            {
                /*
                 * if 'hidden' set, only check for changed file when re-editing the same buffer
                 */
                boolean other = true;
                if (P_HID(curbuf))
                {
                    Bytes p = fullName_save(alist_name(curwin.w_alist.al_ga.ga_data[argn]), true);
                    other = otherfile(p);
                }
                if ((!P_HID(curbuf) || !other)
                      && check_changed(curbuf, CCGD_AW
                                             | (other ? 0 : CCGD_MULTWIN)
                                             | (eap.forceit ? CCGD_FORCEIT : 0)
                                             | CCGD_EXCMD))
                    return;
            }

            curwin.w_arg_idx = argn;
            if (argn == curwin.w_alist.al_ga.ga_len - 1 && curwin.w_alist == global_alist)
                arg_had_last = true;

            /* Edit the file; always use the last known line number.
             * When it fails (e.g. Abort for already edited file), restore the argument index. */
            if (do_ecmd(0, alist_name(curwin.w_alist.al_ga.ga_data[curwin.w_arg_idx]), null,
                          eap, ECMD_LAST,
                          (P_HID(curwin.w_buffer) ? ECMD_HIDE : 0)
                             + (eap.forceit ? ECMD_FORCEIT : 0), curwin) == false)
                curwin.w_arg_idx = old_arg_idx;
            /* Like Vi: set the mark where the cursor is in the file. */
            else if (eap.cmdidx != CMD_argdo)
                setmark('\'');
        }
    }

    /*
     * ":next", and commands that behave like it.
     */
    /*private*/ static final ex_func_C ex_next = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            /*
             * Check for changed buffer now, if this fails the argument list is not redefined.
             */
            if (P_HID(curbuf)
                    || eap.cmdidx == CMD_snext
                    || !check_changed(curbuf, CCGD_AW | (eap.forceit ? CCGD_FORCEIT : 0) | CCGD_EXCMD))
            {
                int i;
                if (eap.arg.at(0) != NUL)    /* redefine file list */
                {
                    if (do_arglist(eap.arg, AL_SET, 0) == false)
                        return;
                    i = 0;
                }
                else
                    i = curwin.w_arg_idx + (int)eap.line2;

                do_argfile(eap, i);
            }
        }
    };

    /*
     * ":argedit"
     */
    /*private*/ static final ex_func_C ex_argedit = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            /* Add the argument to the buffer list and get the buffer number. */
            int fnum = buflist_add(eap.arg, BLN_LISTED);

            /* Check if this argument is already in the argument list. */
            int i;
            for (i = 0; i < curwin.w_alist.al_ga.ga_len; i++)
                if (curwin.w_alist.al_ga.ga_data[i].ae_fnum == fnum)
                    break;
            if (i == curwin.w_alist.al_ga.ga_len)
            {
                /* Can't find it, add it to the argument list. */
                Bytes[] s = { STRDUP(eap.arg) };
                i = alist_add_list(1, s, (0 < eap.addr_count) ? (int)eap.line2 : curwin.w_arg_idx + 1);
                curwin.w_arg_idx = i;
            }

            alist_check_arg_idx();

            /* Edit the argument. */
            do_argfile(eap, i);
        }
    };

    /*
     * ":argadd"
     */
    /*private*/ static final ex_func_C ex_argadd = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            do_arglist(eap.arg, AL_ADD, (0 < eap.addr_count) ? (int)eap.line2 : curwin.w_arg_idx + 1);
        }
    };

    /*
     * ":argdelete"
     */
    /*private*/ static final ex_func_C ex_argdelete = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (0 < eap.addr_count)
            {
                /* ":1,4argdel": Delete all arguments in the range. */
                if (eap.line2 > curwin.w_alist.al_ga.ga_len)
                    eap.line2 = curwin.w_alist.al_ga.ga_len;
                int n = (int)(eap.line2 - eap.line1 + 1);
                if (eap.arg.at(0) != NUL || n <= 0)
                    emsg(e_invarg);
                else
                {
                    aentry_C[] waep = curwin.w_alist.al_ga.ga_data;

                    for (int i = (int)(eap.line1 - 1); i < eap.line2; i++)
                        waep[i] = null;
                    for (int i = (int)eap.line2; i < curwin.w_alist.al_ga.ga_len; i++)
                        waep[i - n] = waep[i];

                    for (int i = 0; i < n; i++)
                        waep[--curwin.w_alist.al_ga.ga_len] = null;

                    if (eap.line2 <= curwin.w_arg_idx)
                        curwin.w_arg_idx -= n;
                    else if (eap.line1 < curwin.w_arg_idx)
                        curwin.w_arg_idx = (int)eap.line1;
                }
            }
            else if (eap.arg.at(0) == NUL)
                emsg(e_argreq);
            else
                do_arglist(eap.arg, AL_DEL, 0);
        }
    };

    /*
     * ":argdo", ":windo", ":bufdo", ":tabdo"
     */
    /*private*/ static final ex_func_C ex_listdo = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            buffer_C buf = curbuf;
            int next_fnum = 0;
            Bytes save_ei = null;

            if (eap.cmdidx != CMD_windo && eap.cmdidx != CMD_tabdo)
                /* Don't do syntax HL autocommands.
                 * Skipping the syntax file is a great speed improvement. */
                save_ei = au_event_disable(u8(",Syntax"));
            start_global_changes();

            if (eap.cmdidx == CMD_windo
                    || eap.cmdidx == CMD_tabdo
                    || P_HID(curbuf)
                    || !check_changed(curbuf, CCGD_AW | (eap.forceit ? CCGD_FORCEIT : 0) | CCGD_EXCMD))
            {
                int i = 0;
                /* start at the eap.line1 argument/window/buffer */
                window_C wp = firstwin;
                tabpage_C tp = first_tabpage;
                switch (eap.cmdidx)
                {
                    case CMD_windo:
                        for ( ; wp != null && i + 1 < eap.line1; wp = wp.w_next)
                            i++;
                        break;
                    case CMD_tabdo:
                        for ( ; tp != null && i + 1 < eap.line1; tp = tp.tp_next)
                            i++;
                        break;
                    case CMD_argdo:
                        i = (int)(eap.line1 - 1);
                        break;
                    default:
                        break;
                }
                /* set pcmark now */
                if (eap.cmdidx == CMD_bufdo)
                {
                    /* Advance to the first listed buffer after "eap.line1". */
                    for (buf = firstbuf; buf != null && (buf.b_fnum < eap.line1 || !buf.b_p_bl[0]); buf = buf.b_next)
                        if (eap.line2 < buf.b_fnum)
                        {
                            buf = null;
                            break;
                        }
                    if (buf != null)
                        goto_buffer(eap, DOBUF_FIRST, FORWARD, buf.b_fnum);
                }
                else
                    setpcmark();
                listcmd_busy = true;        /* avoids setting pcmark below */

                while (!got_int && buf != null)
                {
                    if (eap.cmdidx == CMD_argdo)
                    {
                        /* go to argument "i" */
                        if (i == curwin.w_alist.al_ga.ga_len)
                            break;
                        /* Don't call do_argfile() when already there,
                         * it will try reloading the file. */
                        if (curwin.w_arg_idx != i || !editing_arg_idx(curwin))
                        {
                            /* Clear 'shm' to avoid that the file message
                             * overwrites any output from the command. */
                            Bytes p_shm_save = STRDUP(p_shm[0]);
                            set_option_value(u8("shm"), 0L, u8(""), 0);
                            do_argfile(eap, i);
                            set_option_value(u8("shm"), 0L, p_shm_save, 0);
                        }
                        if (curwin.w_arg_idx != i)
                            break;
                    }
                    else if (eap.cmdidx == CMD_windo)
                    {
                        /* go to window "wp" */
                        if (!win_valid(wp))
                            break;
                        win_goto(wp);
                        if (curwin != wp)
                            break;  /* something must be wrong */
                        wp = curwin.w_next;
                    }
                    else if (eap.cmdidx == CMD_tabdo)
                    {
                        /* go to window "tp" */
                        if (!valid_tabpage(tp))
                            break;
                        goto_tabpage_tp(tp, true, true);
                        tp = tp.tp_next;
                    }
                    else if (eap.cmdidx == CMD_bufdo)
                    {
                        /* Remember the number of the next listed buffer, in case
                         * ":bwipe" is used or autocommands do something strange. */
                        next_fnum = -1;
                        for (buf = curbuf.b_next; buf != null; buf = buf.b_next)
                            if (buf.b_p_bl[0])
                            {
                                next_fnum = buf.b_fnum;
                                break;
                            }
                    }

                    i++;

                    /* execute the command */
                    do_cmdline(eap.arg, eap.getline, eap.cookie, DOCMD_VERBOSE + DOCMD_NOWAIT);

                    if (eap.cmdidx == CMD_bufdo)
                    {
                        /* Done? */
                        if (next_fnum < 0 || eap.line2 < next_fnum)
                            break;
                        /* Check if the buffer still exists. */
                        for (buf = firstbuf; buf != null; buf = buf.b_next)
                            if (buf.b_fnum == next_fnum)
                                break;
                        if (buf == null)
                            break;

                        /* Go to the next buffer.  Clear 'shm' to avoid that
                         * the file message overwrites any output from the command. */
                        Bytes p_shm_save = STRDUP(p_shm[0]);
                        set_option_value(u8("shm"), 0L, u8(""), 0);
                        goto_buffer(eap, DOBUF_FIRST, FORWARD, next_fnum);
                        set_option_value(u8("shm"), 0L, p_shm_save, 0);

                        /* If autocommands took us elsewhere, quit here. */
                        if (curbuf.b_fnum != next_fnum)
                            break;
                    }

                    if (eap.cmdidx == CMD_windo)
                    {
                        validate_cursor();      /* cursor may have moved */
                        /* required when 'scrollbind' has been set */
                        if (curwin.w_onebuf_opt.wo_scb[0])
                            do_check_scrollbind(true);
                    }

                    if (eap.cmdidx == CMD_windo || eap.cmdidx == CMD_tabdo)
                        if (eap.line2 < i + 1)
                            break;
                    if (eap.cmdidx == CMD_argdo && eap.line2 <= i)
                        break;
                }
                listcmd_busy = false;
            }

            if (save_ei != null)
            {
                au_event_restore(save_ei);
                apply_autocmds(EVENT_SYNTAX, curbuf.b_p_syn[0], curbuf.b_fname, true, curbuf);
            }
            end_global_changes();
        }
    };

    /*
     * Add files[count] to the arglist of the current window after arg "after".
     * The file names in files[count] must have been allocated and are taken over.
     * files[] itself is not taken over.
     * Returns index of first added argument.
     */
    /*private*/ static int alist_add_list(int count, Bytes[] files, int after)
        /* after: where to add: 0 = before first one */
    {
        aentry_C[] waep = curwin.w_alist.al_ga.ga_grow(count);
        int n = curwin.w_alist.al_ga.ga_len;

        if (after < 0)
            after = 0;
        if (after > n)
            after = n;

        if (after < n)
            for (int i = n; after <= --i; )
                waep[count + i] = waep[i];
        for (int i = 0; i < count; i++)
        {
            waep[after + i] = new aentry_C();
            waep[after + i].ae_fname = files[i];
            waep[after + i].ae_fnum = buflist_add(files[i], BLN_LISTED);
        }

        curwin.w_alist.al_ga.ga_len += count;
        if (after <= curwin.w_arg_idx)
            curwin.w_arg_idx++;

        return after;
    }

    /*
     * ":runtime {name}"
     */
    /*private*/ static final ex_func_C ex_runtime = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            source_runtime(eap.arg, eap.forceit);
        }
    };

    /*
     * Source the file "name" from all directories in 'runtimepath'.
     * When "all" is true, source all files, otherwise only the first one.
     * return false when no file could be sourced, true otherwise.
     */
    /*private*/ static boolean source_runtime(Bytes name, boolean all)
    {
        boolean did_one = false;

        if (name != null && p_rtp[0] != null)
        {
            if (1 < p_verbose[0])
            {
                verbose_enter();
                smsg(u8("Searching for \"%s\" in \"%s\""), name, p_rtp[0]);
                verbose_leave();
            }

            Bytes buf = new Bytes(MAXPATHL);

            /* Loop over all entries in 'runtimepath'. */
            for (Bytes[] rtp = { p_rtp[0] }; rtp[0].at(0) != NUL && (all || !did_one); )
            {
                /* Copy the path from 'runtimepath' to buf[]. */
                copy_option_part(rtp, buf, MAXPATHL, u8(","));
                if (strlen(buf) + strlen(name) + 2 < MAXPATHL)
                {
                    add_pathsep(buf);
                    Bytes tail = buf.plus(strlen(buf));

                    /* Loop over all patterns in "name". */
                    Bytes[] np = { name };
                    while (np[0].at(0) != NUL && (all || !did_one))
                    {
                        /* Append the pattern from "name" to buf[]. */
                        copy_option_part(np, tail, MAXPATHL - BDIFF(tail, buf), u8("\t "));

                        if (2 < p_verbose[0])
                        {
                            verbose_enter();
                            smsg(u8("Searching for \"%s\""), buf);
                            verbose_leave();
                        }

                        do_source(buf, false);
                        did_one = true;
                        if (!all)
                            break;
                    }
                }
            }

            if (0 < p_verbose[0] && !did_one)
            {
                verbose_enter();
                smsg(u8("not found in 'runtimepath': \"%s\""), name);
                verbose_leave();
            }
        }

        return did_one;
    }

    /*
     * ":source {fname}"
     */
    /*private*/ static final ex_func_C ex_source = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            cmd_source(eap.arg, eap);
        }
    };

    /*private*/ static void cmd_source(Bytes fname, exarg_C eap)
    {
        if (fname.at(0) == NUL)
            emsg(e_argreq);

        else if (eap != null && eap.forceit)
            /* ":source!": read Normal mode commands
             * Need to execute the commands directly.  This is required at least for:
             * - ":g" command busy
             * - after ":argdo", ":windo" or ":bufdo"
             * - another command follows
             * - inside a loop
             */
            openscript(fname, global_busy != 0 || listcmd_busy || eap.nextcmd != null || 0 <= eap.cstack.cs_idx);

        /* ":source" read ex commands */
        else if (do_source(fname, false) == false)
            emsg2(e_notopen, fname);
    }

    /*
     * ":source" and associated commands.
     */
    /*
     * Structure used to store info for each sourced file.
     * It is shared between do_source() and getsourceline().
     * This is required, because it needs to be handed to do_cmdline()
     * and sourcing can be done recursively.
     */
    /*private*/ static final class source_cookie_C
    {
        file_C      fp;            /* opened file for sourcing */
        Bytes       nextline;       /* if not null: line that was read ahead */
        boolean     finished;       /* ":finish" used */
        long[]      breakpoint = new long[1];     /* next line with breakpoint or zero */
        Bytes       fname;          /* name of sourced file */
        int[]       dbg_tick = new int[1];       /* debug_tick when breakpoint was set */
        int         level;          /* top nesting level of sourced file */

        /*private*/ source_cookie_C()
        {
        }
    }

    /*
     * Return the address holding the next breakpoint line for a source cookie.
     */
    /*private*/ static long[] source_breakpoint(source_cookie_C cookie)
    {
        return cookie.breakpoint;
    }

    /*
     * Return the address holding the debug tick for a source cookie.
     */
    /*private*/ static int[] source_dbg_tick(source_cookie_C cookie)
    {
        return cookie.dbg_tick;
    }

    /*
     * Return the nesting level for a source cookie.
     */
    /*private*/ static int source_level(source_cookie_C cookie)
    {
        return cookie.level;
    }

    /*
     * Special function to open a file without handle inheritance.
     * When possible the handle is closed on exec().
     */
    /*private*/ static file_C fopen_noinh_readbin(Bytes filename)
    {
        int fd_tmp = libC.open(filename, O_RDONLY, 0);

        if (fd_tmp == -1)
            return null;

        {
            int fdflags = libc.fcntl(fd_tmp, F_GETFD);
            if (0 <= fdflags && (fdflags & FD_CLOEXEC) == 0)
                libc.fcntl(fd_tmp, F_SETFD, fdflags | FD_CLOEXEC);
        }

        return libC.fdopen(fd_tmp, u8("r"));
    }

    /*private*/ static int last_current_SID;

    /*
     * do_source: Read the file "fname" and execute its lines as EX commands.
     *
     * This function may be called recursively!
     *
     * return false if file could not be opened, true otherwise
     */
    /*private*/ static boolean do_source(Bytes fname, boolean check_other)
        /* check_other: check for .vimrc and _vimrc */
    {
        boolean retval = false;

        int save_debug_break_level = debug_break_level;

        Bytes fname_exp = fullName_save(fname, true);
        if (fname_exp == null)
            return retval;

        if (mch_isdir(fname_exp))
        {
            smsg(u8("Cannot source a directory: \"%s\""), fname);
            return retval;
        }

        /* Apply SourceCmd autocommands, they should get the file and source it. */
        if (has_autocmd(EVENT_SOURCECMD, fname_exp, null)
                && apply_autocmds(EVENT_SOURCECMD, fname_exp, fname_exp, false, curbuf))
        {
            return !aborting();
        }

        /* Apply SourcePre autocommands, they may get the file. */
        apply_autocmds(EVENT_SOURCEPRE, fname_exp, fname_exp, false, curbuf);

        source_cookie_C cookie = new source_cookie_C();

        cookie.fp = fopen_noinh_readbin(fname_exp);
        if (cookie.fp == null && check_other)
        {
            /*
             * Try again, replacing file name ".vimrc" by "_vimrc" or vice versa,
             * and ".exrc" by "_exrc" or vice versa.
             */
            Bytes p = gettail(fname_exp);
            if ((p.at(0) == (byte)'.' || p.at(0) == (byte)'_') && (STRCASECMP(p.plus(1), u8("vimrc")) == 0 || STRCASECMP(p.plus(1), u8("exrc")) == 0))
            {
                if (p.at(0) == (byte)'_')
                    p.be(0, (byte)'.');
                else
                    p.be(0, (byte)'_');
                cookie.fp = fopen_noinh_readbin(fname_exp);
            }
        }

        if (cookie.fp == null)
        {
            if (0 < p_verbose[0])
            {
                verbose_enter();
                if (sourcing_name == null)
                    smsg(u8("could not source \"%s\""), fname);
                else
                    smsg(u8("line %ld: could not source \"%s\""), sourcing_lnum, fname);
                verbose_leave();
            }
            return retval;
        }

        /*
         * The file exists.
         * - In verbose mode, give a message.
         */
        if (1 < p_verbose[0])
        {
            verbose_enter();
            if (sourcing_name == null)
                smsg(u8("sourcing \"%s\""), fname);
            else
                smsg(u8("line %ld: sourcing \"%s\""), sourcing_lnum, fname);
            verbose_leave();
        }

        cookie.nextline = null;
        cookie.finished = false;

        /*
         * Check if this script has a breakpoint.
         */
        cookie.breakpoint[0] = dbg_find_breakpoint(true, fname_exp, 0);
        cookie.fname = fname_exp;
        cookie.dbg_tick[0] = debug_tick;

        cookie.level = ex_nesting_level;

        /*
         * Keep the sourcing name/lnum, for recursive calls.
         */
        Bytes save_sourcing_name = sourcing_name;
        sourcing_name = fname_exp;
        long save_sourcing_lnum = sourcing_lnum;
        sourcing_lnum = 0;

        /* Read the first line so we can check for a UTF-8 BOM. */
        Bytes firstline = getsourceline.getline(0, cookie, 0);
        if (firstline != null && 3 <= strlen(firstline)
            && char_u(firstline.at(0)) == 0xef && char_u(firstline.at(1)) == 0xbb && char_u(firstline.at(2)) == 0xbf)
        {
            /* Found BOM; setup conversion, skip over BOM and recode the line. */
            firstline = STRDUP(firstline.plus(3));
        }

        /* Don't use local function variables, if called from a function. */
        funccall_C save_funccalp = save_funccal();

        /*
         * Check if this script was sourced before to finds its SID.
         * If it's new, generate a new SID.
         */
        int save_current_SID = current_SID;
        stat_C st = new stat_C();
        boolean stat_ok = (0 <= libC.stat(fname_exp, st));
        for (current_SID = script_items.ga_len; 0 < current_SID; --current_SID)
        {
            scriptitem_C si = script_items.ga_data[current_SID - 1];
            if (si.sn_name != null
                    /* Compare dev/ino when possible, it catches symbolic links.
                     * Also compare file names, the inode may change when the file was edited. */
                    && (((stat_ok && si.sn_dev_valid) && (si.sn_dev == st.st_dev() && si.sn_ino == st.st_ino()))
                            || STRCMP(si.sn_name, fname_exp) == 0))
                break;
        }
        if (current_SID == 0)
        {
            current_SID = ++last_current_SID;

            script_items.ga_grow(current_SID - script_items.ga_len);
            while (script_items.ga_len < current_SID)
                script_items.ga_data[script_items.ga_len++] = new scriptitem_C();

            scriptitem_C si = script_items.ga_data[current_SID - 1];
            si.sn_name = fname_exp;
            fname_exp = null;
            if (stat_ok)
            {
                si.sn_dev_valid = true;
                si.sn_dev = st.st_dev();
                si.sn_ino = st.st_ino();
            }
            else
                si.sn_dev_valid = false;

            /* Allocate the local script variables to use for this script. */
            new_script_vars(current_SID);
        }

        /*
         * Call do_cmdline, which will call getsourceline() to get the lines.
         */
        do_cmdline(firstline, getsourceline, cookie, DOCMD_VERBOSE|DOCMD_NOWAIT|DOCMD_REPEAT);
        retval = true;

        if (got_int)
            emsg(e_interr);
        sourcing_name = save_sourcing_name;
        sourcing_lnum = save_sourcing_lnum;
        if (1 < p_verbose[0])
        {
            verbose_enter();
            smsg(u8("finished sourcing %s"), fname);
            if (sourcing_name != null)
                smsg(u8("continuing in %s"), sourcing_name);
            verbose_leave();
        }

        /*
         * After a "finish" in debug mode, need to break at first command of next sourced file.
         */
        if (ex_nesting_level < save_debug_break_level && debug_break_level == ex_nesting_level)
            debug_break_level++;

        current_SID = save_current_SID;
        restore_funccal(save_funccalp);
        libc.fclose(cookie.fp);

        return retval;
    }

    /*
     * ":scriptnames"
     */
    /*private*/ static final ex_func_C ex_scriptnames = new ex_func_C()
    {
        public void ex(exarg_C _eap)
        {
            for (int i = 1; i <= script_items.ga_len && !got_int; i++)
            {
                Bytes name = script_items.ga_data[i - 1].sn_name;
                if (name != null)
                    smsg(u8("%3d: %s"), i, name);
            }
        }
    };

    /*
     * Get a pointer to a script name.  Used for ":verbose set".
     */
    /*private*/ static Bytes get_scriptname(int id)
    {
        if (id == SID_CMDARG)
            return u8("--cmd argument");
        if (id == SID_CARG)
            return u8("-c argument");
        if (id == SID_ENV)
            return u8("environment variable");
        if (id == SID_ERROR)
            return u8("error handler");

        return script_items.ga_data[id - 1].sn_name;
    }

    /*
     * Get one full line from a sourced file.
     * Called by do_cmdline() when it's called from do_source().
     *
     * Return a pointer to the line in allocated memory.
     * Return null for end-of-file or some error.
     */
    /*private*/ static final getline_C getsourceline = new getline_C()
    {
        public Bytes getline(int _c, Object cookie, int _indent)
        {
            source_cookie_C sp = (source_cookie_C)cookie;

            /* If breakpoints have been added/deleted need to check for it. */
            if (sp.dbg_tick[0] < debug_tick)
            {
                sp.breakpoint[0] = dbg_find_breakpoint(true, sp.fname, sourcing_lnum);
                sp.dbg_tick[0] = debug_tick;
            }
            /*
             * Get current line.  If there is a read-ahead line, use it, otherwise get one now.
             */
            Bytes line;
            if (sp.finished)
                line = null;
            else if (sp.nextline == null)
                line = get_one_sourceline(sp);
            else
            {
                line = sp.nextline;
                sp.nextline = null;
                sourcing_lnum++;
            }

            /* Only concatenate lines starting with a \ when 'cpoptions' doesn't contain the 'C' flag. */
            if (line != null && vim_strbyte(p_cpo[0], CPO_CONCAT) == null)
            {
                /* compensate for the one line read-ahead */
                --sourcing_lnum;

                Bytes p;
                /* Get the next line and concatenate it when it starts with a backslash.
                 * We always need to read the next line, keep it in sp.nextline. */
                sp.nextline = get_one_sourceline(sp);
                if (sp.nextline != null && (p = skipwhite(sp.nextline)).at(0) == (byte)'\\')
                {
                    barray_C ba = new barray_C(400);

                    ba_concat(ba, line);
                    ba_concat(ba, p.plus(1));
                    for ( ; ; )
                    {
                        sp.nextline = get_one_sourceline(sp);
                        if (sp.nextline == null)
                            break;
                        p = skipwhite(sp.nextline);
                        if (p.at(0) != (byte)'\\')
                            break;
                        /* Adjust the growsize to the current length to speed up concatenating many lines. */
                        if (400 < ba.ba_len)
                            ba.ba_growsize = (8000 < ba.ba_len) ? 8000 : ba.ba_len;
                        ba_concat(ba, p.plus(1));
                    }
                    ba_append(ba, NUL);
                    line = new Bytes(ba.ba_data);
                }
            }

            /* Did we encounter a breakpoint? */
            if (sp.breakpoint[0] != 0 && sp.breakpoint[0] <= sourcing_lnum)
            {
                dbg_breakpoint(sp.fname, sourcing_lnum);
                /* Find next breakpoint. */
                sp.breakpoint[0] = dbg_find_breakpoint(true, sp.fname, sourcing_lnum);
                sp.dbg_tick[0] = debug_tick;
            }

            return line;
        }
    };

    /*private*/ static Bytes get_one_sourceline(source_cookie_C sp)
    {
        boolean have_read = false;

        /* use a growarray to store the sourced line */
        barray_C ba = new barray_C(250);

        /*
         * Loop until there is a finished line (or end-of-file).
         */
        sourcing_lnum++;
        for ( ; ; )
        {
            /* make room to read at least 120 (more) characters */
            Bytes buf = new Bytes(ba_grow(ba, 120));
            if (!libC._fgets(buf.plus(ba.ba_len), ba.ba_maxlen - ba.ba_len, sp.fp))
                break;

            int len = ba.ba_len + strlen(buf, ba.ba_len);
            have_read = true;
            ba.ba_len = len;

            /* If the line was longer than the buffer, read more. */
            if (ba.ba_maxlen - ba.ba_len == 1 && buf.at(len - 1) != (byte)'\n')
                continue;

            if (1 <= len && buf.at(len - 1) == (byte)'\n')   /* remove trailing NL */
            {
                /* The '\n' is escaped if there is an odd number of ^V's just before it.
                 * First set "c" just before the ^V's and then check "len" and "c" parities;
                 * it is faster than ((len - c) % 2 == 0). */
                int c;
                for (c = len - 2; 0 <= c && buf.at(c) == Ctrl_V; c--)
                    ;
                if ((len & 1) != (c & 1))   /* escaped NL, read more */
                {
                    sourcing_lnum++;
                    continue;
                }

                buf.be(len - 1, NUL);         /* remove the NL */
            }

            /*
             * Check for ^C here now and then, so recursive :so can be broken.
             */
            line_breakcheck();
            break;
        }

        if (have_read)
            return new Bytes(ba.ba_data);

        return null;
    }

    /*
     * ":finish": Mark a sourced file as finished.
     */
    /*private*/ static final ex_func_C ex_finish = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            if (getline_equal(eap.getline, eap.cookie, getsourceline))
                do_finish(eap, false);
            else
                emsg(u8("E168: :finish used outside of a sourced file"));
        }
    };

    /*
     * Mark a sourced file as finished.  Possibly makes the ":finish" pending.
     * Also called for a pending finish at the ":endtry" or after returning from
     * an extra do_cmdline().  "reanimate" is used in the latter case.
     */
    /*private*/ static void do_finish(exarg_C eap, boolean reanimate)
    {
        if (reanimate)
            ((source_cookie_C)getline_cookie(eap.getline, eap.cookie)).finished = false;

        /*
         * Cleanup (and inactivate) conditionals, but stop when a try conditional
         * not in its finally clause (which then is to be executed next) is found.
         * In this case, make the ":finish" pending for execution at the ":endtry".
         * Otherwise, finish normally.
         */
        int idx = cleanup_conditionals(eap.cstack, 0, true);
        if (0 <= idx)
        {
            eap.cstack.cs_pending[idx] = CSTP_FINISH;
            report_make_pending(CSTP_FINISH, null);
        }
        else
            ((source_cookie_C)getline_cookie(eap.getline, eap.cookie)).finished = true;
    }

    /*
     * Return true when a sourced file had the ":finish" command:
     * don't give error message for missing ":endif".
     * Return false when not sourcing a file.
     */
    /*private*/ static boolean source_finished(getline_C fgetline, Object cookie)
    {
        return (getline_equal(fgetline, cookie, getsourceline)
            && ((source_cookie_C)getline_cookie(fgetline, cookie)).finished);
    }

    /*
     * ":checktime [buffer]"
     */
    /*private*/ static final ex_func_C ex_checktime = new ex_func_C()
    {
        public void ex(exarg_C eap)
        {
            int save_no_check_timestamps = no_check_timestamps;

            no_check_timestamps = 0;
            if (eap.addr_count == 0)                            /* default is all buffers */
                check_timestamps(false);
            else
            {
                buffer_C buf = buflist_findnr((int)eap.line2);
                if (buf != null)                                /* cannot happen? */
                    buf_check_timestamp(buf);
            }
            no_check_timestamps = save_no_check_timestamps;
        }
    };

    /*
     * hashtab.c: Handling of a hashtable with Vim-specific properties.
     *
     * Each item in a hashtable has a NUL terminated string key.
     * A key can appear only once in the table.
     *
     * A hash number is computed from the key for quick lookup.
     * When the hashes of two different keys point to the same entry
     * an algorithm is used to iterate over other entries in the table
     * until the right one is found.
     * To make the iteration work removed keys are different
     * from entries where a key was never present.
     *
     * The hashtable grows to accommodate more entries when needed.
     * At least 1/3 of the entries is empty to keep the lookup efficient
     * (at the cost of extra memory).
     */

    /* Magic value for algorithm that walks through the array. */
    /*private*/ static final int PERTURB_SHIFT = 5;

    /*
     * Initialize an empty hash table.
     */
    /*private*/ static void hash_init(hashtab_C ht)
    {
        ZER0_hashtab(ht);
        ht.ht_buckets = ARRAY_hashitem(HT_INIT_SIZE);
        ht.ht_mask = HT_INIT_SIZE - 1;
    }

    /*
     * Free the array of a hash table.  Does not free the items it contains!
     * If "ht" is not freed then you should call hash_init() next!
     */
    /*private*/ static void hash_clear(hashtab_C ht)
    {
        ht.ht_buckets = null;
    }

    /*
     * Find "key" in hashtable "ht".  "key" must not be null.
     * Always returns a pointer to a hashitem.  If the item was not found then
     * hashitem_empty() is true.  The pointer is then the place where the key would be added.
     * WARNING: The returned pointer becomes invalid when the hashtable is changed
     * (adding, setting or removing an item)!
     */
    /*private*/ static hashitem_C hash_find(hashtab_C ht, Bytes key)
    {
        return hash_lookup(ht, key, hash_hash(key));
    }

    /*
     * Like hash_find(), but caller computes "hash".
     */
    /*private*/ static hashitem_C hash_lookup(hashtab_C ht, Bytes key, long hash)
    {
        /*
         * Quickly handle the most common situations:
         * - return if there is no item at all
         * - skip over a removed item
         * - return if the item matches
         */
        long idx = (hash & ht.ht_mask);
        hashitem_C hi = ht.ht_buckets[(int)idx];

        if (hi.hi_key == null)
            return hi;

        hashitem_C freeitem;
        if (hi.hi_key == HASH_REMOVED)
            freeitem = hi;
        else if (hi.hi_hash == hash && STRCMP(hi.hi_key, key) == 0)
            return hi;
        else
            freeitem = null;

        /*
         * Need to search through the table to find the key.  The algorithm to step
         * through the table starts with large steps, gradually becoming smaller down
         * to (1/4 table size + 1).  This means it goes through all table entries in
         * the end.  When we run into a null key, it's clear that the key isn't there.
         * Return the first available slot found (can be a slot of a removed item).
         */
        for (long perturb = hash; ; perturb = perturb >>> PERTURB_SHIFT)
        {
            idx = (idx << 2) + idx + perturb + 1;
            hi = ht.ht_buckets[(int)(idx & ht.ht_mask)];
            if (hi.hi_key == null)
                return (freeitem == null) ? hi : freeitem;
            if (hi.hi_hash == hash && hi.hi_key != HASH_REMOVED && STRCMP(hi.hi_key, key) == 0)
                return hi;
            if (hi.hi_key == HASH_REMOVED && freeitem == null)
                freeitem = hi;
        }
    }

    /*
     * Add item with key "key" to hashtable "ht".
     * Returns false when out of memory or the key is already present.
     */
    /*private*/ static boolean hash_add(hashtab_C ht, Object data, Bytes key)
    {
        long hash = hash_hash(key);
        hashitem_C hi = hash_lookup(ht, key, hash);
        if (!hashitem_empty(hi))
        {
            emsg2(e_intern2, u8("hash_add()"));
            return false;
        }
        return hash_add_item(ht, hi, data, key, hash);
    }

    /*
     * Add item "hi" with "key" to hashtable "ht".  "key" must not be null and
     * "hi" must have been obtained with hash_lookup() and point to an empty item.
     * "hi" is invalid after this!
     * Returns true or false (out of memory).
     */
    /*private*/ static boolean hash_add_item(hashtab_C ht, hashitem_C hi, Object data, Bytes key, long hash)
    {
        ht.ht_used++;
        if (hi.hi_key == null)
            ht.ht_filled++;
        hi.hi_data = data;
        hi.hi_key = key;
        hi.hi_hash = hash;

        /* When the space gets low may resize the array. */
        return hash_may_resize(ht, 0);
    }

    /*
     * Remove item "hi" from  hashtable "ht".  "hi" must have been obtained with hash_lookup().
     * The caller must take care of freeing the item itself.
     */
    /*private*/ static void hash_remove(hashtab_C ht, hashitem_C hi)
    {
        --ht.ht_used;
        hi.hi_data = hi.hi_key = HASH_REMOVED;
        hash_may_resize(ht, 0);
    }

    /*
     * Lock a hashtable: prevent that ht_buckets changes.
     * Don't use this when items are to be added!
     * Must call hash_unlock() later.
     */
    /*private*/ static void hash_lock(hashtab_C ht)
    {
        ht.ht_locked++;
    }

    /*
     * Unlock a hashtable: allow ht_buckets changes again.
     * Table will be resized (shrink) when necessary.
     * This must balance a call to hash_lock().
     */
    /*private*/ static void hash_unlock(hashtab_C ht)
    {
        --ht.ht_locked;
        hash_may_resize(ht, 0);
    }

    /*
     * Shrink a hashtable when there is too much empty space.
     * Grow a hashtable when there is not enough empty space.
     * Returns true or false (out of memory).
     */
    /*private*/ static boolean hash_may_resize(hashtab_C ht, int minitems)
        /* minitems: minimal number of items */
    {
        /* Don't resize a locked table. */
        if (0 < ht.ht_locked)
            return true;

        long minsize;
        if (minitems == 0)
        {
            /*
             * Return quickly for small tables with at least two null items.
             * NULL items are required for the lookup to decide a key isn't there.
             */
            if (ht.ht_filled < HT_INIT_SIZE - 1)
                return true;

            /*
             * Grow or refill the array when it's more than 2/3 full
             * (including removed items, so that they get cleaned up).
             * Shrink the array when it's less than 1/5 full.
             * When growing it is at least 1/4 full
             * (avoids repeated grow-shrink operations).
             */
            long oldsize = ht.ht_mask + 1;
            if (ht.ht_filled * 3 < oldsize * 2 && oldsize / 5 < ht.ht_used)
                return true;

            if (1000 < ht.ht_used)
                minsize = ht.ht_used * 2;   /* it's big, don't make too much room */
            else
                minsize = ht.ht_used * 4;   /* make plenty of room */
        }
        else
        {
            /* Use specified size. */
            if ((long)minitems < ht.ht_used)        /* just in case... */
                minitems = (int)ht.ht_used;
            minsize = minitems * 3 / 2;             /* array is up to 2/3 full */
        }

        long newsize = HT_INIT_SIZE;
        while (newsize < minsize)
        {
            newsize <<= 1;                  /* make sure it's always a power of 2 */
            if (newsize <= 0)
                return false;               /* overflow */
        }

        hashitem_C[] newarray = ARRAY_hashitem((int)newsize), oldarray = ht.ht_buckets;

        /*
         * Move all the items from the old array to the new one, placing them in the right spot.
         * The new array won't have any removed items, thus this is also a cleanup action.
         */
        long newmask = newsize - 1;
        for (int oldi = 0, todo = (int)ht.ht_used; 0 < todo; oldi++)
        {
            hashitem_C olditem = oldarray[oldi];
            if (!hashitem_empty(olditem))
            {
                /*
                 * The algorithm to find the spot to add the item is identical to
                 * the algorithm to find an item in hash_lookup().  But we only
                 * need to search for a null key, thus it's simpler.
                 */
                long newi = (olditem.hi_hash & newmask);
                hashitem_C newitem = newarray[(int)newi];

                if (newitem.hi_key != null)
                    for (long perturb = olditem.hi_hash; ; perturb = perturb >>> PERTURB_SHIFT)
                    {
                        newi = (newi << 2) + newi + perturb + 1;
                        newitem = newarray[(int)(newi & newmask)];
                        if (newitem.hi_key == null)
                            break;
                    }
                COPY_hashitem(newitem, olditem);
                --todo;
            }
        }

        ht.ht_buckets = newarray;
        ht.ht_mask = newmask;
        ht.ht_filled = ht.ht_used;

        return true;
    }

    /*
     * Get the hash number for a key.
     * If you think you know a better hash function: compile with HT_DEBUG set
     * and run a script that uses hashtables a lot.  Vim will then print statistics
     * when exiting.  Try that with the current hash algorithm and yours.
     * The lower the percentage the better.
     */
    /*private*/ static long hash_hash(Bytes key)
    {
        long hash = (key = key.plus(1)).at(-1);

        if (hash != 0)                          /* empty keys are not allowed */
            while (key.at(0) != NUL)                 /* simplistic algorithm that appears to do very well */
                hash = hash * 101 + (key = key.plus(1)).at(-1);

        return hash;
    }
}
