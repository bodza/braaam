typedef long unsigned int size_t;

typedef unsigned char __u_char;
typedef unsigned short int __u_short;
typedef unsigned int __u_int;
typedef unsigned long int __u_long;

typedef signed char __int8_t;
typedef unsigned char __uint8_t;
typedef signed short int __int16_t;
typedef unsigned short int __uint16_t;
typedef signed int __int32_t;
typedef unsigned int __uint32_t;

typedef signed long int __int64_t;
typedef unsigned long int __uint64_t;

typedef long int __quad_t;
typedef unsigned long int __u_quad_t;

typedef unsigned long int __dev_t;
typedef unsigned int __uid_t;
typedef unsigned int __gid_t;
typedef unsigned long int __ino_t;
typedef unsigned long int __ino64_t;
typedef unsigned int __mode_t;
typedef unsigned long int __nlink_t;
typedef long int __off_t;
typedef long int __off64_t;
typedef int __pid_t;
typedef struct { int __val[2]; } __fsid_t;
typedef long int __clock_t;
typedef unsigned long int __rlim_t;
typedef unsigned long int __rlim64_t;
typedef unsigned int __id_t;
typedef long int __time_t;
typedef unsigned int __useconds_t;
typedef long int __suseconds_t;

typedef int __daddr_t;
typedef long int __swblk_t;
typedef int __key_t;

typedef int __clockid_t;

typedef void * __timer_t;

typedef long int __blksize_t;

typedef long int __blkcnt_t;
typedef long int __blkcnt64_t;

typedef unsigned long int __fsblkcnt_t;
typedef unsigned long int __fsblkcnt64_t;

typedef unsigned long int __fsfilcnt_t;
typedef unsigned long int __fsfilcnt64_t;

typedef long int __ssize_t;

typedef __off64_t __loff_t;
typedef __quad_t *__qaddr_t;
typedef char *__caddr_t;

typedef long int __intptr_t;

typedef unsigned int __socklen_t;

struct _IO_FILE;

typedef struct _IO_FILE FILE;

typedef struct _IO_FILE __FILE;

typedef struct
{
    int __count;
    union
    {
        unsigned int __wch;

        char __wchb[4];
    } __value;
} __mbstate_t;

typedef struct
{
    __off_t __pos;
    __mbstate_t __state;
} _G_fpos_t;
typedef struct
{
    __off64_t __pos;
    __mbstate_t __state;
} _G_fpos64_t;

typedef int _G_int16_t __attribute__ ((__mode__ (__HI__)));
typedef int _G_int32_t __attribute__ ((__mode__ (__SI__)));
typedef unsigned int _G_uint16_t __attribute__ ((__mode__ (__HI__)));
typedef unsigned int _G_uint32_t __attribute__ ((__mode__ (__SI__)));

typedef __builtin_va_list __gnuc_va_list;

struct _IO_jump_t;
struct _IO_FILE;

typedef void _IO_lock_t;

struct _IO_marker
{
    struct _IO_marker *_next;
    struct _IO_FILE *_sbuf;

    int _pos;
};

enum __codecvt_result
{
    __codecvt_ok,
    __codecvt_partial,
    __codecvt_error,
    __codecvt_noconv
};

struct _IO_FILE
{
    int _flags;

    char* _IO_read_ptr;
    char* _IO_read_end;
    char* _IO_read_base;
    char* _IO_write_base;
    char* _IO_write_ptr;
    char* _IO_write_end;
    char* _IO_buf_base;
    char* _IO_buf_end;

    char *_IO_save_base;
    char *_IO_backup_base;
    char *_IO_save_end;

    struct _IO_marker *_markers;

    struct _IO_FILE *_chain;

    int _fileno;

    int _flags2;

    __off_t _old_offset;

    unsigned short _cur_column;
    signed char _vtable_offset;
    char _shortbuf[1];

    _IO_lock_t *_lock;

    __off64_t _offset;

    void *__pad1;
    void *__pad2;
    void *__pad3;
    void *__pad4;
    size_t __pad5;

    int _mode;

    char _unused2[15 * sizeof (int) - 4 * sizeof (void *) - sizeof (size_t)];
};

typedef struct _IO_FILE _IO_FILE;

struct _IO_FILE_plus;

extern struct _IO_FILE_plus _IO_2_1_stdin_;
extern struct _IO_FILE_plus _IO_2_1_stdout_;
extern struct _IO_FILE_plus _IO_2_1_stderr_;

typedef __ssize_t __io_read_fn (void *__cookie, char *__buf, size_t __nbytes);

typedef __ssize_t __io_write_fn (void *__cookie, __const char *__buf, size_t __n);

typedef int __io_seek_fn (void *__cookie, __off64_t *__pos, int __w);

typedef int __io_close_fn (void *__cookie);

extern int __underflow (_IO_FILE *);
extern int __uflow (_IO_FILE *);
extern int __overflow (_IO_FILE *, int);

extern int _IO_getc (_IO_FILE *__fp);
extern int _IO_putc (int __c, _IO_FILE *__fp);
extern int _IO_feof (_IO_FILE *__fp) __attribute__ ((__nothrow__));
extern int _IO_ferror (_IO_FILE *__fp) __attribute__ ((__nothrow__));

extern int _IO_peekc_locked (_IO_FILE *__fp);

extern void _IO_flockfile (_IO_FILE *) __attribute__ ((__nothrow__));
extern void _IO_funlockfile (_IO_FILE *) __attribute__ ((__nothrow__));
extern int _IO_ftrylockfile (_IO_FILE *) __attribute__ ((__nothrow__));

extern int _IO_vfscanf (_IO_FILE * __restrict, const char * __restrict, __gnuc_va_list, int *__restrict);
extern int _IO_vfprintf (_IO_FILE *__restrict, const char *__restrict, __gnuc_va_list);
extern __ssize_t _IO_padn (_IO_FILE *, int, __ssize_t);
extern size_t _IO_sgetn (_IO_FILE *, void *, size_t);

extern __off64_t _IO_seekoff (_IO_FILE *, __off64_t, int, int);
extern __off64_t _IO_seekpos (_IO_FILE *, __off64_t, int);

extern void _IO_free_backup_area (_IO_FILE *) __attribute__ ((__nothrow__));

typedef __gnuc_va_list va_list;

typedef __off_t off_t;

typedef __ssize_t ssize_t;

typedef _G_fpos_t fpos_t;

extern struct _IO_FILE *stdin;
extern struct _IO_FILE *stdout;
extern struct _IO_FILE *stderr;

extern int remove (__const char *__filename) __attribute__ ((__nothrow__));

extern int rename (__const char *__old, __const char *__new) __attribute__ ((__nothrow__));

extern int renameat (int __oldfd, __const char *__old, int __newfd, __const char *__new) __attribute__ ((__nothrow__));

extern FILE *tmpfile (void) __attribute__ ((__warn_unused_result__));

extern char *tmpnam (char *__s) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern char *tmpnam_r (char *__s) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern char *tempnam (__const char *__dir, __const char *__pfx) __attribute__ ((__nothrow__)) __attribute__ ((__malloc__)) __attribute__ ((__warn_unused_result__));

extern int fclose (FILE *__stream);

extern int fflush (FILE *__stream);

extern int fflush_unlocked (FILE *__stream);

extern FILE *fopen (__const char *__restrict __filename, __const char *__restrict __modes) __attribute__ ((__warn_unused_result__));

extern FILE *freopen (__const char *__restrict __filename, __const char *__restrict __modes, FILE *__restrict __stream) __attribute__ ((__warn_unused_result__));

extern FILE *fdopen (int __fd, __const char *__modes) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern FILE *fmemopen (void *__s, size_t __len, __const char *__modes) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern FILE *open_memstream (char **__bufloc, size_t *__sizeloc) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern void setbuf (FILE *__restrict __stream, char *__restrict __buf) __attribute__ ((__nothrow__));

extern int setvbuf (FILE *__restrict __stream, char *__restrict __buf, int __modes, size_t __n) __attribute__ ((__nothrow__));

extern void setbuffer (FILE *__restrict __stream, char *__restrict __buf, size_t __size) __attribute__ ((__nothrow__));

extern void setlinebuf (FILE *__stream) __attribute__ ((__nothrow__));

extern int fprintf (FILE *__restrict __stream, __const char *__restrict __format, ...);

extern int printf (__const char *__restrict __format, ...);

extern int sprintf (char *__restrict __s, __const char *__restrict __format, ...) __attribute__ ((__nothrow__));

extern int vfprintf (FILE *__restrict __s, __const char *__restrict __format, __gnuc_va_list __arg);

extern int vprintf (__const char *__restrict __format, __gnuc_va_list __arg);

extern int vsprintf (char *__restrict __s, __const char *__restrict __format, __gnuc_va_list __arg) __attribute__ ((__nothrow__));

extern int snprintf (char *__restrict __s, size_t __maxlen, __const char *__restrict __format, ...) __attribute__ ((__nothrow__)) __attribute__ ((__format__ (__printf__, 3, 4)));

extern int vsnprintf (char *__restrict __s, size_t __maxlen, __const char *__restrict __format, __gnuc_va_list __arg) __attribute__ ((__nothrow__)) __attribute__ ((__format__ (__printf__, 3, 0)));

extern int vdprintf (int __fd, __const char *__restrict __fmt, __gnuc_va_list __arg) __attribute__ ((__format__ (__printf__, 2, 0)));
extern int dprintf (int __fd, __const char *__restrict __fmt, ...) __attribute__ ((__format__ (__printf__, 2, 3)));

extern int fscanf (FILE *__restrict __stream, __const char *__restrict __format, ...) __attribute__ ((__warn_unused_result__));

extern int scanf (__const char *__restrict __format, ...) __attribute__ ((__warn_unused_result__));

extern int sscanf (__const char *__restrict __s, __const char *__restrict __format, ...) __attribute__ ((__nothrow__));

extern int fscanf (FILE *__restrict __stream, __const char *__restrict __format, ...) __asm__ ("" "__isoc99_fscanf") __attribute__ ((__warn_unused_result__));
extern int scanf (__const char *__restrict __format, ...) __asm__ ("" "__isoc99_scanf") __attribute__ ((__warn_unused_result__));
extern int sscanf (__const char *__restrict __s, __const char *__restrict __format, ...) __asm__ ("" "__isoc99_sscanf") __attribute__ ((__nothrow__));

extern int vfscanf (FILE *__restrict __s, __const char *__restrict __format, __gnuc_va_list __arg) __attribute__ ((__format__ (__scanf__, 2, 0))) __attribute__ ((__warn_unused_result__));

extern int vscanf (__const char *__restrict __format, __gnuc_va_list __arg) __attribute__ ((__format__ (__scanf__, 1, 0))) __attribute__ ((__warn_unused_result__));

extern int vsscanf (__const char *__restrict __s, __const char *__restrict __format, __gnuc_va_list __arg) __attribute__ ((__nothrow__)) __attribute__ ((__format__ (__scanf__, 2, 0)));

extern int vfscanf (FILE *__restrict __s, __const char *__restrict __format, __gnuc_va_list __arg) __asm__ ("" "__isoc99_vfscanf") __attribute__ ((__format__ (__scanf__, 2, 0))) __attribute__ ((__warn_unused_result__));
extern int vscanf (__const char *__restrict __format, __gnuc_va_list __arg) __asm__ ("" "__isoc99_vscanf") __attribute__ ((__format__ (__scanf__, 1, 0))) __attribute__ ((__warn_unused_result__));
extern int vsscanf (__const char *__restrict __s, __const char *__restrict __format, __gnuc_va_list __arg) __asm__ ("" "__isoc99_vsscanf") __attribute__ ((__nothrow__)) __attribute__ ((__format__ (__scanf__, 2, 0)));

extern int fgetc (FILE *__stream);
extern int getc (FILE *__stream);

extern int getchar (void);

extern int getc_unlocked (FILE *__stream);
extern int getchar_unlocked (void);

extern int fgetc_unlocked (FILE *__stream);

extern int fputc (int __c, FILE *__stream);
extern int putc (int __c, FILE *__stream);

extern int putchar (int __c);

extern int fputc_unlocked (int __c, FILE *__stream);

extern int putc_unlocked (int __c, FILE *__stream);
extern int putchar_unlocked (int __c);

extern int getw (FILE *__stream);

extern int putw (int __w, FILE *__stream);

extern char *fgets (char *__restrict __s, int __n, FILE *__restrict __stream) __attribute__ ((__warn_unused_result__));

extern char *gets (char *__s) __attribute__ ((__warn_unused_result__));

extern __ssize_t __getdelim (char **__restrict __lineptr, size_t *__restrict __n, int __delimiter, FILE *__restrict __stream) __attribute__ ((__warn_unused_result__));
extern __ssize_t getdelim (char **__restrict __lineptr, size_t *__restrict __n, int __delimiter, FILE *__restrict __stream) __attribute__ ((__warn_unused_result__));

extern __ssize_t getline (char **__restrict __lineptr, size_t *__restrict __n, FILE *__restrict __stream) __attribute__ ((__warn_unused_result__));

extern int fputs (__const char *__restrict __s, FILE *__restrict __stream);

extern int puts (__const char *__s);

extern int ungetc (int __c, FILE *__stream);

extern size_t fread (void *__restrict __ptr, size_t __size, size_t __n, FILE *__restrict __stream) __attribute__ ((__warn_unused_result__));

extern size_t fwrite (__const void *__restrict __ptr, size_t __size, size_t __n, FILE *__restrict __s) __attribute__ ((__warn_unused_result__));

extern size_t fread_unlocked (void *__restrict __ptr, size_t __size, size_t __n, FILE *__restrict __stream) __attribute__ ((__warn_unused_result__));
extern size_t fwrite_unlocked (__const void *__restrict __ptr, size_t __size, size_t __n, FILE *__restrict __stream) __attribute__ ((__warn_unused_result__));

extern int fseek (FILE *__stream, long int __off, int __whence);

extern long int ftell (FILE *__stream) __attribute__ ((__warn_unused_result__));

extern void rewind (FILE *__stream);

extern int fseeko (FILE *__stream, __off_t __off, int __whence);

extern __off_t ftello (FILE *__stream) __attribute__ ((__warn_unused_result__));

extern int fgetpos (FILE *__restrict __stream, fpos_t *__restrict __pos);

extern int fsetpos (FILE *__stream, __const fpos_t *__pos);

extern void clearerr (FILE *__stream) __attribute__ ((__nothrow__));

extern int feof (FILE *__stream) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern int ferror (FILE *__stream) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern void clearerr_unlocked (FILE *__stream) __attribute__ ((__nothrow__));
extern int feof_unlocked (FILE *__stream) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));
extern int ferror_unlocked (FILE *__stream) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern void perror (__const char *__s);

extern int sys_nerr;
extern __const char *__const sys_errlist[];

extern int fileno (FILE *__stream) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern int fileno_unlocked (FILE *__stream) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern FILE *popen (__const char *__command, __const char *__modes) __attribute__ ((__warn_unused_result__));

extern int pclose (FILE *__stream);

extern char *ctermid (char *__s) __attribute__ ((__nothrow__));

extern void flockfile (FILE *__stream) __attribute__ ((__nothrow__));

extern int ftrylockfile (FILE *__stream) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern void funlockfile (FILE *__stream) __attribute__ ((__nothrow__));

extern __inline __attribute__ ((__gnu_inline__)) int getchar (void)
{
    return _IO_getc (stdin);
}

extern __inline __attribute__ ((__gnu_inline__)) int fgetc_unlocked (FILE *__fp)
{
    return (__builtin_expect (((__fp)->_IO_read_ptr >= (__fp)->_IO_read_end), 0) ? __uflow (__fp) : *(unsigned char *) (__fp)->_IO_read_ptr++);
}

extern __inline __attribute__ ((__gnu_inline__)) int getc_unlocked (FILE *__fp)
{
    return (__builtin_expect (((__fp)->_IO_read_ptr >= (__fp)->_IO_read_end), 0) ? __uflow (__fp) : *(unsigned char *) (__fp)->_IO_read_ptr++);
}

extern __inline __attribute__ ((__gnu_inline__)) int getchar_unlocked (void)
{
    return (__builtin_expect (((stdin)->_IO_read_ptr >= (stdin)->_IO_read_end), 0) ? __uflow (stdin) : *(unsigned char *) (stdin)->_IO_read_ptr++);
}

extern __inline __attribute__ ((__gnu_inline__)) int putchar (int __c)
{
    return _IO_putc (__c, stdout);
}

extern __inline __attribute__ ((__gnu_inline__)) int fputc_unlocked (int __c, FILE *__stream)
{
    return (__builtin_expect (((__stream)->_IO_write_ptr >= (__stream)->_IO_write_end), 0) ? __overflow (__stream, (unsigned char) (__c)) : (unsigned char) (*(__stream)->_IO_write_ptr++ = (__c)));
}

extern __inline __attribute__ ((__gnu_inline__)) int putc_unlocked (int __c, FILE *__stream)
{
    return (__builtin_expect (((__stream)->_IO_write_ptr >= (__stream)->_IO_write_end), 0) ? __overflow (__stream, (unsigned char) (__c)) : (unsigned char) (*(__stream)->_IO_write_ptr++ = (__c)));
}

extern __inline __attribute__ ((__gnu_inline__)) int putchar_unlocked (int __c)
{
    return (__builtin_expect (((stdout)->_IO_write_ptr >= (stdout)->_IO_write_end), 0) ? __overflow (stdout, (unsigned char) (__c)) : (unsigned char) (*(stdout)->_IO_write_ptr++ = (__c)));
}

extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) feof_unlocked (FILE *__stream)
{
    return (((__stream)->_flags & 0x10) != 0);
}

extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) ferror_unlocked (FILE *__stream)
{
    return (((__stream)->_flags & 0x20) != 0);
}

extern int __sprintf_chk (char *__restrict __s, int __flag, size_t __slen, __const char *__restrict __format, ...) __attribute__ ((__nothrow__));
extern int __vsprintf_chk (char *__restrict __s, int __flag, size_t __slen, __const char *__restrict __format, __gnuc_va_list __ap) __attribute__ ((__nothrow__));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) int __attribute__ ((__nothrow__)) sprintf (char *__restrict __s, __const char *__restrict __fmt, ...)
{
    return __builtin___sprintf_chk (__s, 1 - 1, __builtin_object_size (__s, 1 > 1), __fmt, __builtin_va_arg_pack ());
}

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) int __attribute__ ((__nothrow__)) vsprintf (char *__restrict __s, __const char *__restrict __fmt, __gnuc_va_list __ap)
{
    return __builtin___vsprintf_chk (__s, 1 - 1, __builtin_object_size (__s, 1 > 1), __fmt, __ap);
}

extern int __snprintf_chk (char *__restrict __s, size_t __n, int __flag, size_t __slen, __const char *__restrict __format, ...) __attribute__ ((__nothrow__));
extern int __vsnprintf_chk (char *__restrict __s, size_t __n, int __flag, size_t __slen, __const char *__restrict __format, __gnuc_va_list __ap) __attribute__ ((__nothrow__));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) int __attribute__ ((__nothrow__)) snprintf (char *__restrict __s, size_t __n, __const char *__restrict __fmt, ...)
{
    return __builtin___snprintf_chk (__s, __n, 1 - 1, __builtin_object_size (__s, 1 > 1), __fmt, __builtin_va_arg_pack ());
}

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) int __attribute__ ((__nothrow__)) vsnprintf (char *__restrict __s, size_t __n, __const char *__restrict __fmt, __gnuc_va_list __ap)
{
    return __builtin___vsnprintf_chk (__s, __n, 1 - 1, __builtin_object_size (__s, 1 > 1), __fmt, __ap);
}

extern char *__gets_chk (char *__str, size_t) __attribute__ ((__warn_unused_result__));
extern char *__gets_warn (char *__str) __asm__ ("" "gets") __attribute__ ((__warn_unused_result__)) __attribute__((__warning__ ("please use fgets or getline instead, gets can't " "specify buffer size")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) __attribute__ ((__warn_unused_result__)) char * gets (char *__str)
{
    if (__builtin_object_size (__str, 1 > 1) != (size_t) -1)
        return __gets_chk (__str, __builtin_object_size (__str, 1 > 1));
    return __gets_warn (__str);
}

extern char *__fgets_chk (char *__restrict __s, size_t __size, int __n, FILE *__restrict __stream) __attribute__ ((__warn_unused_result__));
extern char *__fgets_alias (char *__restrict __s, int __n, FILE *__restrict __stream) __asm__ ("" "fgets") __attribute__ ((__warn_unused_result__));
extern char *__fgets_chk_warn (char *__restrict __s, size_t __size, int __n, FILE *__restrict __stream) __asm__ ("" "__fgets_chk") __attribute__ ((__warn_unused_result__)) __attribute__((__warning__ ("fgets called with bigger size than length " "of destination buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) __attribute__ ((__warn_unused_result__)) char * fgets (char *__restrict __s, int __n, FILE *__restrict __stream)
{
    if (__builtin_object_size (__s, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__n) || __n <= 0)
            return __fgets_chk (__s, __builtin_object_size (__s, 1 > 1), __n, __stream);

        if ((size_t) __n > __builtin_object_size (__s, 1 > 1))
            return __fgets_chk_warn (__s, __builtin_object_size (__s, 1 > 1), __n, __stream);
    }
    return __fgets_alias (__s, __n, __stream);
}

extern size_t __fread_chk (void *__restrict __ptr, size_t __ptrlen, size_t __size, size_t __n, FILE *__restrict __stream) __attribute__ ((__warn_unused_result__));
extern size_t __fread_alias (void *__restrict __ptr, size_t __size, size_t __n, FILE *__restrict __stream) __asm__ ("" "fread") __attribute__ ((__warn_unused_result__));
extern size_t __fread_chk_warn (void *__restrict __ptr, size_t __ptrlen, size_t __size, size_t __n, FILE *__restrict __stream) __asm__ ("" "__fread_chk") __attribute__ ((__warn_unused_result__)) __attribute__((__warning__ ("fread called with bigger size * nmemb than length " "of destination buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) __attribute__ ((__warn_unused_result__)) size_t fread (void *__restrict __ptr, size_t __size, size_t __n, FILE *__restrict __stream)
{
    if (__builtin_object_size (__ptr, 0) != (size_t) -1)
    {
        if (!__builtin_constant_p (__size) || !__builtin_constant_p (__n) || (__size | __n) >= (((size_t) 1) << (8 * sizeof (size_t) / 2)))
            return __fread_chk (__ptr, __builtin_object_size (__ptr, 0), __size, __n, __stream);

        if (__size * __n > __builtin_object_size (__ptr, 0))
            return __fread_chk_warn (__ptr, __builtin_object_size (__ptr, 0), __size, __n, __stream);
    }
    return __fread_alias (__ptr, __size, __n, __stream);
}

extern size_t __fread_unlocked_chk (void *__restrict __ptr, size_t __ptrlen, size_t __size, size_t __n, FILE *__restrict __stream) __attribute__ ((__warn_unused_result__));
extern size_t __fread_unlocked_alias (void *__restrict __ptr, size_t __size, size_t __n, FILE *__restrict __stream) __asm__ ("" "fread_unlocked") __attribute__ ((__warn_unused_result__));
extern size_t __fread_unlocked_chk_warn (void *__restrict __ptr, size_t __ptrlen, size_t __size, size_t __n, FILE *__restrict __stream) __asm__ ("" "__fread_unlocked_chk") __attribute__ ((__warn_unused_result__)) __attribute__((__warning__ ("fread_unlocked called with bigger size * nmemb than " "length of destination buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) __attribute__ ((__warn_unused_result__)) size_t fread_unlocked (void *__restrict __ptr, size_t __size, size_t __n, FILE *__restrict __stream)
{
    if (__builtin_object_size (__ptr, 0) != (size_t) -1)
    {
        if (!__builtin_constant_p (__size) || !__builtin_constant_p (__n) || (__size | __n) >= (((size_t) 1) << (8 * sizeof (size_t) / 2)))
            return __fread_unlocked_chk (__ptr, __builtin_object_size (__ptr, 0), __size, __n, __stream);

        if (__size * __n > __builtin_object_size (__ptr, 0))
            return __fread_unlocked_chk_warn (__ptr, __builtin_object_size (__ptr, 0), __size, __n, __stream);
    }

    if (__builtin_constant_p (__size) && __builtin_constant_p (__n) && (__size | __n) < (((size_t) 1) << (8 * sizeof (size_t) / 2)) && __size * __n <= 8)
    {
        size_t __cnt = __size * __n;
        char *__cptr = (char *) __ptr;
        if (__cnt == 0)
            return 0;

        for ( ; __cnt > 0; --__cnt)
        {
            int __c = (__builtin_expect (((__stream)->_IO_read_ptr >= (__stream)->_IO_read_end), 0) ? __uflow (__stream) : *(unsigned char *) (__stream)->_IO_read_ptr++);
            if (__c == (-1))
                break;
            *__cptr++ = __c;
        }
        return (__cptr - (char *) __ptr) / __size;
    }

    return __fread_unlocked_alias (__ptr, __size, __n, __stream);
}

enum
{
    _ISupper = ((0) < 8 ? ((1 << (0)) << 8) : ((1 << (0)) >> 8)),
    _ISlower = ((1) < 8 ? ((1 << (1)) << 8) : ((1 << (1)) >> 8)),
    _ISalpha = ((2) < 8 ? ((1 << (2)) << 8) : ((1 << (2)) >> 8)),
    _ISdigit = ((3) < 8 ? ((1 << (3)) << 8) : ((1 << (3)) >> 8)),
    _ISxdigit = ((4) < 8 ? ((1 << (4)) << 8) : ((1 << (4)) >> 8)),
    _ISspace = ((5) < 8 ? ((1 << (5)) << 8) : ((1 << (5)) >> 8)),
    _ISprint = ((6) < 8 ? ((1 << (6)) << 8) : ((1 << (6)) >> 8)),
    _ISgraph = ((7) < 8 ? ((1 << (7)) << 8) : ((1 << (7)) >> 8)),
    _ISblank = ((8) < 8 ? ((1 << (8)) << 8) : ((1 << (8)) >> 8)),
    _IScntrl = ((9) < 8 ? ((1 << (9)) << 8) : ((1 << (9)) >> 8)),
    _ISpunct = ((10) < 8 ? ((1 << (10)) << 8) : ((1 << (10)) >> 8)),
    _ISalnum = ((11) < 8 ? ((1 << (11)) << 8) : ((1 << (11)) >> 8))
};

extern __const unsigned short int **__ctype_b_loc (void) __attribute__ ((__nothrow__)) __attribute__ ((__const));
extern __const __int32_t **__ctype_tolower_loc (void) __attribute__ ((__nothrow__)) __attribute__ ((__const));
extern __const __int32_t **__ctype_toupper_loc (void) __attribute__ ((__nothrow__)) __attribute__ ((__const));

extern int isalnum (int) __attribute__ ((__nothrow__));
extern int isalpha (int) __attribute__ ((__nothrow__));
extern int iscntrl (int) __attribute__ ((__nothrow__));
extern int isdigit (int) __attribute__ ((__nothrow__));
extern int islower (int) __attribute__ ((__nothrow__));
extern int isgraph (int) __attribute__ ((__nothrow__));
extern int isprint (int) __attribute__ ((__nothrow__));
extern int ispunct (int) __attribute__ ((__nothrow__));
extern int isspace (int) __attribute__ ((__nothrow__));
extern int isupper (int) __attribute__ ((__nothrow__));
extern int isxdigit (int) __attribute__ ((__nothrow__));

extern int tolower (int __c) __attribute__ ((__nothrow__));

extern int toupper (int __c) __attribute__ ((__nothrow__));

extern int isblank (int) __attribute__ ((__nothrow__));

extern int isascii (int __c) __attribute__ ((__nothrow__));

extern int toascii (int __c) __attribute__ ((__nothrow__));

extern int _toupper (int) __attribute__ ((__nothrow__));
extern int _tolower (int) __attribute__ ((__nothrow__));

extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) tolower (int __c)
{
    return __c >= -128 && __c < 256 ? (*__ctype_tolower_loc ())[__c] : __c;
}

extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) toupper (int __c)
{
    return __c >= -128 && __c < 256 ? (*__ctype_toupper_loc ())[__c] : __c;
}

typedef struct __locale_struct
{
    struct __locale_data *__locales[13];

    const unsigned short int *__ctype_b;
    const int *__ctype_tolower;
    const int *__ctype_toupper;

    const char *__names[13];
} *__locale_t;

typedef __locale_t locale_t;

extern int isalnum_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int isalpha_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int iscntrl_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int isdigit_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int islower_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int isgraph_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int isprint_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int ispunct_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int isspace_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int isupper_l (int, __locale_t) __attribute__ ((__nothrow__));
extern int isxdigit_l (int, __locale_t) __attribute__ ((__nothrow__));

extern int isblank_l (int, __locale_t) __attribute__ ((__nothrow__));

extern int __tolower_l (int __c, __locale_t __l) __attribute__ ((__nothrow__));
extern int tolower_l (int __c, __locale_t __l) __attribute__ ((__nothrow__));

extern int __toupper_l (int __c, __locale_t __l) __attribute__ ((__nothrow__));
extern int toupper_l (int __c, __locale_t __l) __attribute__ ((__nothrow__));

typedef int wchar_t;

union wait
{
    int w_status;
    struct
    {
        unsigned int __w_termsig:7;
        unsigned int __w_coredump:1;
        unsigned int __w_retcode:8;
        unsigned int:16;

    } __wait_terminated;
    struct
    {
        unsigned int __w_stopval:8;
        unsigned int __w_stopsig:8;
        unsigned int:16;

    } __wait_stopped;
};

typedef union
{
    union wait *__uptr;
    int *__iptr;
} __WAIT_STATUS __attribute__ ((__transparent_union__));

typedef struct
{
    int quot;
    int rem;
} div_t;

typedef struct
{
    long int quot;
    long int rem;
} ldiv_t;

__extension__ typedef struct
{
    long long int quot;
    long long int rem;
} lldiv_t;

extern size_t __ctype_get_mb_cur_max (void) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern double atof (__const char *__nptr) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern int atoi (__const char *__nptr) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern long int atol (__const char *__nptr) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

__extension__ extern long long int atoll (__const char *__nptr) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern double strtod (__const char *__restrict __nptr, char **__restrict __endptr) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern float strtof (__const char *__restrict __nptr, char **__restrict __endptr) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern long double strtold (__const char *__restrict __nptr, char **__restrict __endptr) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern long int strtol (__const char *__restrict __nptr, char **__restrict __endptr, int __base) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern unsigned long int strtoul (__const char *__restrict __nptr, char **__restrict __endptr, int __base) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

__extension__ extern long long int strtoq (__const char *__restrict __nptr, char **__restrict __endptr, int __base) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

__extension__ extern unsigned long long int strtouq (__const char *__restrict __nptr, char **__restrict __endptr, int __base) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

__extension__ extern long long int strtoll (__const char *__restrict __nptr, char **__restrict __endptr, int __base) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

__extension__ extern unsigned long long int strtoull (__const char *__restrict __nptr, char **__restrict __endptr, int __base) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern __inline __attribute__ ((__gnu_inline__)) double __attribute__ ((__nothrow__)) atof (__const char *__nptr)
{
    return strtod (__nptr, (char **) ((void *)0));
}
extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) atoi (__const char *__nptr)
{
    return (int) strtol (__nptr, (char **) ((void *)0), 10);
}
extern __inline __attribute__ ((__gnu_inline__)) long int __attribute__ ((__nothrow__)) atol (__const char *__nptr)
{
    return strtol (__nptr, (char **) ((void *)0), 10);
}

__extension__ extern __inline __attribute__ ((__gnu_inline__)) long long int __attribute__ ((__nothrow__)) atoll (__const char *__nptr)
{
    return strtoll (__nptr, (char **) ((void *)0), 10);
}

extern char *l64a (long int __n) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern long int a64l (__const char *__s) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

typedef __u_char u_char;
typedef __u_short u_short;
typedef __u_int u_int;
typedef __u_long u_long;
typedef __quad_t quad_t;
typedef __u_quad_t u_quad_t;
typedef __fsid_t fsid_t;
typedef __loff_t loff_t;
typedef __ino_t ino_t;
typedef __dev_t dev_t;
typedef __gid_t gid_t;
typedef __mode_t mode_t;
typedef __nlink_t nlink_t;
typedef __uid_t uid_t;
typedef __pid_t pid_t;
typedef __id_t id_t;
typedef __daddr_t daddr_t;
typedef __caddr_t caddr_t;
typedef __key_t key_t;
typedef __clock_t clock_t;
typedef __time_t time_t;
typedef __clockid_t clockid_t;
typedef __timer_t timer_t;

typedef unsigned long int ulong;
typedef unsigned short int ushort;
typedef unsigned int uint;

typedef int int8_t __attribute__ ((__mode__ (__QI__)));
typedef int int16_t __attribute__ ((__mode__ (__HI__)));
typedef int int32_t __attribute__ ((__mode__ (__SI__)));
typedef int int64_t __attribute__ ((__mode__ (__DI__)));

typedef unsigned int u_int8_t __attribute__ ((__mode__ (__QI__)));
typedef unsigned int u_int16_t __attribute__ ((__mode__ (__HI__)));
typedef unsigned int u_int32_t __attribute__ ((__mode__ (__SI__)));
typedef unsigned int u_int64_t __attribute__ ((__mode__ (__DI__)));

typedef int register_t __attribute__ ((__mode__ (__word__)));

typedef int __sig_atomic_t;

typedef struct
{
    unsigned long int __val[(1024 / (8 * sizeof (unsigned long int)))];
} __sigset_t;

typedef __sigset_t sigset_t;

struct timespec
{
    __time_t tv_sec;
    long int tv_nsec;
};

struct timeval
{
    __time_t tv_sec;
    __suseconds_t tv_usec;
};

typedef __suseconds_t suseconds_t;

typedef long int __fd_mask;

typedef struct
{
    __fd_mask __fds_bits[1024 / (8 * (int) sizeof (__fd_mask))];

} fd_set;

typedef __fd_mask fd_mask;

extern int select (int __nfds, fd_set *__restrict __readfds, fd_set *__restrict __writefds, fd_set *__restrict __exceptfds, struct timeval *__restrict __timeout);

extern int pselect (int __nfds, fd_set *__restrict __readfds, fd_set *__restrict __writefds, fd_set *__restrict __exceptfds, const struct timespec *__restrict __timeout, const __sigset_t *__restrict __sigmask);

__extension__ extern unsigned int gnu_dev_major (unsigned long long int __dev) __attribute__ ((__nothrow__));
__extension__ extern unsigned int gnu_dev_minor (unsigned long long int __dev) __attribute__ ((__nothrow__));
__extension__ extern unsigned long long int gnu_dev_makedev (unsigned int __major, unsigned int __minor) __attribute__ ((__nothrow__));

__extension__ extern __inline __attribute__ ((__gnu_inline__)) unsigned int __attribute__ ((__nothrow__)) gnu_dev_major (unsigned long long int __dev)
{
    return ((__dev >> 8) & 0xfff) | ((unsigned int) (__dev >> 32) & ~0xfff);
}

__extension__ extern __inline __attribute__ ((__gnu_inline__)) unsigned int __attribute__ ((__nothrow__)) gnu_dev_minor (unsigned long long int __dev)
{
    return (__dev & 0xff) | ((unsigned int) (__dev >> 12) & ~0xff);
}

__extension__ extern __inline __attribute__ ((__gnu_inline__)) unsigned long long int __attribute__ ((__nothrow__)) gnu_dev_makedev (unsigned int __major, unsigned int __minor)
{
    return ((__minor & 0xff) | ((__major & 0xfff) << 8) | (((unsigned long long int) (__minor & ~0xff)) << 12) | (((unsigned long long int) (__major & ~0xfff)) << 32));
}

typedef __blksize_t blksize_t;
typedef __blkcnt_t blkcnt_t;
typedef __fsblkcnt_t fsblkcnt_t;
typedef __fsfilcnt_t fsfilcnt_t;

typedef unsigned long int pthread_t;

typedef union
{
    char __size[56];
    long int __align;
} pthread_attr_t;

typedef struct __pthread_internal_list
{
    struct __pthread_internal_list *__prev;
    struct __pthread_internal_list *__next;
} __pthread_list_t;

typedef union
{
    struct __pthread_mutex_s
    {
        int __lock;
        unsigned int __count;
        int __owner;

        unsigned int __nusers;

        int __kind;

        int __spins;
        __pthread_list_t __list;

    } __data;
    char __size[40];
    long int __align;
} pthread_mutex_t;

typedef union
{
    char __size[4];
    int __align;
} pthread_mutexattr_t;

typedef union
{
    struct
    {
        int __lock;
        unsigned int __futex;
        __extension__ unsigned long long int __total_seq;
        __extension__ unsigned long long int __wakeup_seq;
        __extension__ unsigned long long int __woken_seq;
        void *__mutex;
        unsigned int __nwaiters;
        unsigned int __broadcast_seq;
    } __data;
    char __size[48];
    __extension__ long long int __align;
} pthread_cond_t;

typedef union
{
    char __size[4];
    int __align;
} pthread_condattr_t;

typedef unsigned int pthread_key_t;

typedef int pthread_once_t;

typedef union
{
    struct
    {
        int __lock;
        unsigned int __nr_readers;
        unsigned int __readers_wakeup;
        unsigned int __writer_wakeup;
        unsigned int __nr_readers_queued;
        unsigned int __nr_writers_queued;
        int __writer;
        int __shared;
        unsigned long int __pad1;
        unsigned long int __pad2;

        unsigned int __flags;
    } __data;

    char __size[56];
    long int __align;
} pthread_rwlock_t;

typedef union
{
    char __size[8];
    long int __align;
} pthread_rwlockattr_t;

typedef volatile int pthread_spinlock_t;

typedef union
{
    char __size[32];
    long int __align;
} pthread_barrier_t;

typedef union
{
    char __size[4];
    int __align;
} pthread_barrierattr_t;

extern long int random (void) __attribute__ ((__nothrow__));

extern void srandom (unsigned int __seed) __attribute__ ((__nothrow__));

extern char *initstate (unsigned int __seed, char *__statebuf, size_t __statelen) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern char *setstate (char *__statebuf) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

struct random_data
{
    int32_t *fptr;
    int32_t *rptr;
    int32_t *state;
    int rand_type;
    int rand_deg;
    int rand_sep;
    int32_t *end_ptr;
};

extern int random_r (struct random_data *__restrict __buf, int32_t *__restrict __result) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int srandom_r (unsigned int __seed, struct random_data *__buf) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern int initstate_r (unsigned int __seed, char *__restrict __statebuf, size_t __statelen, struct random_data *__restrict __buf) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 4)));

extern int setstate_r (char *__restrict __statebuf, struct random_data *__restrict __buf) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int rand (void) __attribute__ ((__nothrow__));

extern void srand (unsigned int __seed) __attribute__ ((__nothrow__));

extern int rand_r (unsigned int *__seed) __attribute__ ((__nothrow__));

extern double drand48 (void) __attribute__ ((__nothrow__));
extern double erand48 (unsigned short int __xsubi[3]) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern long int lrand48 (void) __attribute__ ((__nothrow__));
extern long int nrand48 (unsigned short int __xsubi[3]) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern long int mrand48 (void) __attribute__ ((__nothrow__));
extern long int jrand48 (unsigned short int __xsubi[3]) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern void srand48 (long int __seedval) __attribute__ ((__nothrow__));
extern unsigned short int *seed48 (unsigned short int __seed16v[3]) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
extern void lcong48 (unsigned short int __param[7]) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

struct drand48_data
{
    unsigned short int __x[3];
    unsigned short int __old_x[3];
    unsigned short int __c;
    unsigned short int __init;
    unsigned long long int __a;
};

extern int drand48_r (struct drand48_data *__restrict __buffer, double *__restrict __result) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));
extern int erand48_r (unsigned short int __xsubi[3], struct drand48_data *__restrict __buffer, double *__restrict __result) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int lrand48_r (struct drand48_data *__restrict __buffer, long int *__restrict __result) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));
extern int nrand48_r (unsigned short int __xsubi[3], struct drand48_data *__restrict __buffer, long int *__restrict __result) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int mrand48_r (struct drand48_data *__restrict __buffer, long int *__restrict __result) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));
extern int jrand48_r (unsigned short int __xsubi[3], struct drand48_data *__restrict __buffer, long int *__restrict __result) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int srand48_r (long int __seedval, struct drand48_data *__buffer) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern int seed48_r (unsigned short int __seed16v[3], struct drand48_data *__buffer) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int lcong48_r (unsigned short int __param[7], struct drand48_data *__buffer) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern void *malloc (size_t __size) __attribute__ ((__nothrow__)) __attribute__ ((__malloc__)) __attribute__ ((__warn_unused_result__));

extern void *calloc (size_t __nmemb, size_t __size) __attribute__ ((__nothrow__)) __attribute__ ((__malloc__)) __attribute__ ((__warn_unused_result__));

extern void *realloc (void *__ptr, size_t __size) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern void free (void *__ptr) __attribute__ ((__nothrow__));

extern void cfree (void *__ptr) __attribute__ ((__nothrow__));

extern void *alloca (size_t __size) __attribute__ ((__nothrow__));

extern void *valloc (size_t __size) __attribute__ ((__nothrow__)) __attribute__ ((__malloc__)) __attribute__ ((__warn_unused_result__));

extern int posix_memalign (void **__memptr, size_t __alignment, size_t __size) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern void abort (void) __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));

extern int atexit (void (*__func) (void)) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int on_exit (void (*__func) (int __status, void *__arg), void *__arg) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern void exit (int __status) __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));

extern void _Exit (int __status) __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));

extern char *getenv (__const char *__name) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern char *__secure_getenv (__const char *__name) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern int putenv (char *__string) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int setenv (__const char *__name, __const char *__value, int __replace) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern int unsetenv (__const char *__name) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int clearenv (void) __attribute__ ((__nothrow__));

extern char *mktemp (char *__template) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern int mkstemp (char *__template) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern int mkstemps (char *__template, int __suffixlen) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern char *mkdtemp (char *__template) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern int system (__const char *__command) __attribute__ ((__warn_unused_result__));

extern char *realpath (__const char *__restrict __name, char *__restrict __resolved) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

typedef int (*__compar_fn_t) (__const void *, __const void *);

extern void *bsearch (__const void *__key, __const void *__base, size_t __nmemb, size_t __size, __compar_fn_t __compar) __attribute__ ((__nonnull__ (1, 2, 5))) __attribute__ ((__warn_unused_result__));

extern void qsort (void *__base, size_t __nmemb, size_t __size, __compar_fn_t __compar) __attribute__ ((__nonnull__ (1, 4)));

extern int abs (int __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)) __attribute__ ((__warn_unused_result__));
extern long int labs (long int __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)) __attribute__ ((__warn_unused_result__));

__extension__ extern long long int llabs (long long int __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)) __attribute__ ((__warn_unused_result__));

extern div_t div (int __numer, int __denom) __attribute__ ((__nothrow__)) __attribute__ ((__const__)) __attribute__ ((__warn_unused_result__));
extern ldiv_t ldiv (long int __numer, long int __denom) __attribute__ ((__nothrow__)) __attribute__ ((__const__)) __attribute__ ((__warn_unused_result__));

__extension__ extern lldiv_t lldiv (long long int __numer, long long int __denom) __attribute__ ((__nothrow__)) __attribute__ ((__const__)) __attribute__ ((__warn_unused_result__));

extern char *ecvt (double __value, int __ndigit, int *__restrict __decpt, int *__restrict __sign) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4))) __attribute__ ((__warn_unused_result__));

extern char *fcvt (double __value, int __ndigit, int *__restrict __decpt, int *__restrict __sign) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4))) __attribute__ ((__warn_unused_result__));

extern char *gcvt (double __value, int __ndigit, char *__buf) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3))) __attribute__ ((__warn_unused_result__));

extern char *qecvt (long double __value, int __ndigit, int *__restrict __decpt, int *__restrict __sign) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4))) __attribute__ ((__warn_unused_result__));
extern char *qfcvt (long double __value, int __ndigit, int *__restrict __decpt, int *__restrict __sign) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4))) __attribute__ ((__warn_unused_result__));
extern char *qgcvt (long double __value, int __ndigit, char *__buf) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3))) __attribute__ ((__warn_unused_result__));

extern int ecvt_r (double __value, int __ndigit, int *__restrict __decpt, int *__restrict __sign, char *__restrict __buf, size_t __len) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4, 5)));
extern int fcvt_r (double __value, int __ndigit, int *__restrict __decpt, int *__restrict __sign, char *__restrict __buf, size_t __len) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4, 5)));

extern int qecvt_r (long double __value, int __ndigit, int *__restrict __decpt, int *__restrict __sign, char *__restrict __buf, size_t __len) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4, 5)));
extern int qfcvt_r (long double __value, int __ndigit, int *__restrict __decpt, int *__restrict __sign, char *__restrict __buf, size_t __len) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4, 5)));

extern int mblen (__const char *__s, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern int mbtowc (wchar_t *__restrict __pwc, __const char *__restrict __s, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern int wctomb (char *__s, wchar_t __wchar) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern size_t mbstowcs (wchar_t *__restrict __pwcs, __const char *__restrict __s, size_t __n) __attribute__ ((__nothrow__));

extern size_t wcstombs (char *__restrict __s, __const wchar_t *__restrict __pwcs, size_t __n) __attribute__ ((__nothrow__));

extern int rpmatch (__const char *__response) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern int getsubopt (char **__restrict __optionp, char *__const *__restrict __tokens, char **__restrict __valuep) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2, 3))) __attribute__ ((__warn_unused_result__));

extern int getloadavg (double __loadavg[], int __nelem) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern char *__realpath_chk (__const char *__restrict __name, char *__restrict __resolved, size_t __resolvedlen) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));
extern char *__realpath_alias (__const char *__restrict __name, char *__restrict __resolved) __asm__ ("" "realpath") __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));
extern char *__realpath_chk_warn (__const char *__restrict __name, char *__restrict __resolved, size_t __resolvedlen) __asm__ ("" "__realpath_chk") __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__)) __attribute__((__warning__ ("second argument of realpath must be either NULL or at " "least PATH_MAX bytes long buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) __attribute__ ((__warn_unused_result__)) char * __attribute__ ((__nothrow__)) realpath (__const char *__restrict __name, char *__restrict __resolved)
{
    if (__builtin_object_size (__resolved, 1 > 1) != (size_t) -1)
    {
        return __realpath_chk (__name, __resolved, __builtin_object_size (__resolved, 1 > 1));
    }

    return __realpath_alias (__name, __resolved);
}

extern int __ptsname_r_chk (int __fd, char *__buf, size_t __buflen, size_t __nreal) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));
extern int __ptsname_r_alias (int __fd, char *__buf, size_t __buflen) __asm__ ("" "ptsname_r") __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));
extern int __ptsname_r_chk_warn (int __fd, char *__buf, size_t __buflen, size_t __nreal) __asm__ ("" "__ptsname_r_chk") __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2))) __attribute__((__warning__ ("ptsname_r called with buflen bigger than " "size of buf")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) int __attribute__ ((__nothrow__)) ptsname_r (int __fd, char *__buf, size_t __buflen)
{
    if (__builtin_object_size (__buf, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__buflen))
            return __ptsname_r_chk (__fd, __buf, __buflen, __builtin_object_size (__buf, 1 > 1));
        if (__buflen > __builtin_object_size (__buf, 1 > 1))
            return __ptsname_r_chk_warn (__fd, __buf, __buflen, __builtin_object_size (__buf, 1 > 1));
    }
    return __ptsname_r_alias (__fd, __buf, __buflen);
}

extern int __wctomb_chk (char *__s, wchar_t __wchar, size_t __buflen) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));
extern int __wctomb_alias (char *__s, wchar_t __wchar) __asm__ ("" "wctomb") __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) __attribute__ ((__warn_unused_result__)) int __attribute__ ((__nothrow__)) wctomb (char *__s, wchar_t __wchar)
{
    if (__builtin_object_size (__s, 1 > 1) != (size_t) -1 && 16 > __builtin_object_size (__s, 1 > 1))
        return __wctomb_chk (__s, __wchar, __builtin_object_size (__s, 1 > 1));
    return __wctomb_alias (__s, __wchar);
}

extern size_t __mbstowcs_chk (wchar_t *__restrict __dst, __const char *__restrict __src, size_t __len, size_t __dstlen) __attribute__ ((__nothrow__));
extern size_t __mbstowcs_alias (wchar_t *__restrict __dst, __const char *__restrict __src, size_t __len) __asm__ ("" "mbstowcs") __attribute__ ((__nothrow__));
extern size_t __mbstowcs_chk_warn (wchar_t *__restrict __dst, __const char *__restrict __src, size_t __len, size_t __dstlen) __asm__ ("" "__mbstowcs_chk") __attribute__ ((__nothrow__)) __attribute__((__warning__ ("mbstowcs called with dst buffer smaller than len " "* sizeof (wchar_t)")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) size_t __attribute__ ((__nothrow__)) mbstowcs (wchar_t *__restrict __dst, __const char *__restrict __src, size_t __len)
{
    if (__builtin_object_size (__dst, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__len))
            return __mbstowcs_chk (__dst, __src, __len, __builtin_object_size (__dst, 1 > 1) / sizeof (wchar_t));

        if (__len > __builtin_object_size (__dst, 1 > 1) / sizeof (wchar_t))
            return __mbstowcs_chk_warn (__dst, __src, __len, __builtin_object_size (__dst, 1 > 1) / sizeof (wchar_t));
    }
    return __mbstowcs_alias (__dst, __src, __len);
}

extern size_t __wcstombs_chk (char *__restrict __dst, __const wchar_t *__restrict __src, size_t __len, size_t __dstlen) __attribute__ ((__nothrow__));
extern size_t __wcstombs_alias (char *__restrict __dst, __const wchar_t *__restrict __src, size_t __len) __asm__ ("" "wcstombs") __attribute__ ((__nothrow__));
extern size_t __wcstombs_chk_warn (char *__restrict __dst, __const wchar_t *__restrict __src, size_t __len, size_t __dstlen) __asm__ ("" "__wcstombs_chk") __attribute__ ((__nothrow__)) __attribute__((__warning__ ("wcstombs called with dst buffer smaller than len")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) size_t __attribute__ ((__nothrow__)) wcstombs (char *__restrict __dst, __const wchar_t *__restrict __src, size_t __len)
{
    if (__builtin_object_size (__dst, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__len))
            return __wcstombs_chk (__dst, __src, __len, __builtin_object_size (__dst, 1 > 1));
        if (__len > __builtin_object_size (__dst, 1 > 1))
            return __wcstombs_chk_warn (__dst, __src, __len, __builtin_object_size (__dst, 1 > 1));
    }
    return __wcstombs_alias (__dst, __src, __len);
}

typedef __useconds_t useconds_t;

typedef __intptr_t intptr_t;

typedef __socklen_t socklen_t;

extern int access (__const char *__name, int __type) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int faccessat (int __fd, __const char *__file, int __type, int __flag) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2))) __attribute__ ((__warn_unused_result__));

extern __off_t lseek (int __fd, __off_t __offset, int __whence) __attribute__ ((__nothrow__));

extern int close (int __fd);

extern ssize_t read (int __fd, void *__buf, size_t __nbytes) __attribute__ ((__warn_unused_result__));

extern ssize_t write (int __fd, __const void *__buf, size_t __n) __attribute__ ((__warn_unused_result__));

extern ssize_t pread (int __fd, void *__buf, size_t __nbytes, __off_t __offset) __attribute__ ((__warn_unused_result__));

extern ssize_t pwrite (int __fd, __const void *__buf, size_t __n, __off_t __offset) __attribute__ ((__warn_unused_result__));

extern int pipe (int __pipedes[2]) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern unsigned int alarm (unsigned int __seconds) __attribute__ ((__nothrow__));

extern unsigned int sleep (unsigned int __seconds);

extern __useconds_t ualarm (__useconds_t __value, __useconds_t __interval) __attribute__ ((__nothrow__));

extern int usleep (__useconds_t __useconds);

extern int pause (void);

extern int chown (__const char *__file, __uid_t __owner, __gid_t __group) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern int fchown (int __fd, __uid_t __owner, __gid_t __group) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern int lchown (__const char *__file, __uid_t __owner, __gid_t __group) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern int fchownat (int __fd, __const char *__file, __uid_t __owner, __gid_t __group, int __flag) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2))) __attribute__ ((__warn_unused_result__));

extern int chdir (__const char *__path) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern int fchdir (int __fd) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern char *getcwd (char *__buf, size_t __size) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern char *getwd (char *__buf) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__deprecated__)) __attribute__ ((__warn_unused_result__));

extern int dup (int __fd) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern int dup2 (int __fd, int __fd2) __attribute__ ((__nothrow__));

extern char **__environ;

extern int execve (__const char *__path, char *__const __argv[], char *__const __envp[]) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int fexecve (int __fd, char *__const __argv[], char *__const __envp[]) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern int execv (__const char *__path, char *__const __argv[]) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int execle (__const char *__path, __const char *__arg, ...) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int execl (__const char *__path, __const char *__arg, ...) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int execvp (__const char *__file, char *__const __argv[]) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int execlp (__const char *__file, __const char *__arg, ...) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int nice (int __inc) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern void _exit (int __status) __attribute__ ((__noreturn__));

enum
{
    _PC_LINK_MAX,
    _PC_MAX_CANON,
    _PC_MAX_INPUT,
    _PC_NAME_MAX,
    _PC_PATH_MAX,
    _PC_PIPE_BUF,
    _PC_CHOWN_RESTRICTED,
    _PC_NO_TRUNC,
    _PC_VDISABLE,
    _PC_SYNC_IO,
    _PC_ASYNC_IO,
    _PC_PRIO_IO,
    _PC_SOCK_MAXBUF,
    _PC_FILESIZEBITS,
    _PC_REC_INCR_XFER_SIZE,
    _PC_REC_MAX_XFER_SIZE,
    _PC_REC_MIN_XFER_SIZE,
    _PC_REC_XFER_ALIGN,
    _PC_ALLOC_SIZE_MIN,
    _PC_SYMLINK_MAX,
    _PC_2_SYMLINKS
};

enum
{
    _SC_ARG_MAX,
    _SC_CHILD_MAX,
    _SC_CLK_TCK,
    _SC_NGROUPS_MAX,
    _SC_OPEN_MAX,
    _SC_STREAM_MAX,
    _SC_TZNAME_MAX,
    _SC_JOB_CONTROL,
    _SC_SAVED_IDS,
    _SC_REALTIME_SIGNALS,
    _SC_PRIORITY_SCHEDULING,
    _SC_TIMERS,
    _SC_ASYNCHRONOUS_IO,
    _SC_PRIORITIZED_IO,
    _SC_SYNCHRONIZED_IO,
    _SC_FSYNC,
    _SC_MAPPED_FILES,
    _SC_MEMLOCK,
    _SC_MEMLOCK_RANGE,
    _SC_MEMORY_PROTECTION,
    _SC_MESSAGE_PASSING,
    _SC_SEMAPHORES,
    _SC_SHARED_MEMORY_OBJECTS,
    _SC_AIO_LISTIO_MAX,
    _SC_AIO_MAX,
    _SC_AIO_PRIO_DELTA_MAX,
    _SC_DELAYTIMER_MAX,
    _SC_MQ_OPEN_MAX,
    _SC_MQ_PRIO_MAX,
    _SC_VERSION,
    _SC_PAGESIZE,
    _SC_RTSIG_MAX,
    _SC_SEM_NSEMS_MAX,
    _SC_SEM_VALUE_MAX,
    _SC_SIGQUEUE_MAX,
    _SC_TIMER_MAX,
    _SC_BC_BASE_MAX,
    _SC_BC_DIM_MAX,
    _SC_BC_SCALE_MAX,
    _SC_BC_STRING_MAX,
    _SC_COLL_WEIGHTS_MAX,
    _SC_EQUIV_CLASS_MAX,
    _SC_EXPR_NEST_MAX,
    _SC_LINE_MAX,
    _SC_RE_DUP_MAX,
    _SC_CHARCLASS_NAME_MAX,
    _SC_2_VERSION,
    _SC_2_C_BIND,
    _SC_2_C_DEV,
    _SC_2_FORT_DEV,
    _SC_2_FORT_RUN,
    _SC_2_SW_DEV,
    _SC_2_LOCALEDEF,
    _SC_PII,
    _SC_PII_XTI,
    _SC_PII_SOCKET,
    _SC_PII_INTERNET,
    _SC_PII_OSI,
    _SC_POLL,
    _SC_SELECT,
    _SC_UIO_MAXIOV,
    _SC_IOV_MAX = _SC_UIO_MAXIOV,
    _SC_PII_INTERNET_STREAM,
    _SC_PII_INTERNET_DGRAM,
    _SC_PII_OSI_COTS,
    _SC_PII_OSI_CLTS,
    _SC_PII_OSI_M,
    _SC_T_IOV_MAX,
    _SC_THREADS,
    _SC_THREAD_SAFE_FUNCTIONS,
    _SC_GETGR_R_SIZE_MAX,
    _SC_GETPW_R_SIZE_MAX,
    _SC_LOGIN_NAME_MAX,
    _SC_TTY_NAME_MAX,
    _SC_THREAD_DESTRUCTOR_ITERATIONS,
    _SC_THREAD_KEYS_MAX,
    _SC_THREAD_STACK_MIN,
    _SC_THREAD_THREADS_MAX,
    _SC_THREAD_ATTR_STACKADDR,
    _SC_THREAD_ATTR_STACKSIZE,
    _SC_THREAD_PRIORITY_SCHEDULING,
    _SC_THREAD_PRIO_INHERIT,
    _SC_THREAD_PRIO_PROTECT,
    _SC_THREAD_PROCESS_SHARED,
    _SC_NPROCESSORS_CONF,
    _SC_NPROCESSORS_ONLN,
    _SC_PHYS_PAGES,
    _SC_AVPHYS_PAGES,
    _SC_ATEXIT_MAX,
    _SC_PASS_MAX,
    _SC_XOPEN_VERSION,
    _SC_XOPEN_XCU_VERSION,
    _SC_XOPEN_UNIX,
    _SC_XOPEN_CRYPT,
    _SC_XOPEN_ENH_I18N,
    _SC_XOPEN_SHM,
    _SC_2_CHAR_TERM,
    _SC_2_C_VERSION,
    _SC_2_UPE,
    _SC_XOPEN_XPG2,
    _SC_XOPEN_XPG3,
    _SC_XOPEN_XPG4,
    _SC_CHAR_BIT,
    _SC_CHAR_MAX,
    _SC_CHAR_MIN,
    _SC_INT_MAX,
    _SC_INT_MIN,
    _SC_LONG_BIT,
    _SC_WORD_BIT,
    _SC_MB_LEN_MAX,
    _SC_NZERO,
    _SC_SSIZE_MAX,
    _SC_SCHAR_MAX,
    _SC_SCHAR_MIN,
    _SC_SHRT_MAX,
    _SC_SHRT_MIN,
    _SC_UCHAR_MAX,
    _SC_UINT_MAX,
    _SC_ULONG_MAX,
    _SC_USHRT_MAX,
    _SC_NL_ARGMAX,
    _SC_NL_LANGMAX,
    _SC_NL_MSGMAX,
    _SC_NL_NMAX,
    _SC_NL_SETMAX,
    _SC_NL_TEXTMAX,
    _SC_XBS5_ILP32_OFF32,
    _SC_XBS5_ILP32_OFFBIG,
    _SC_XBS5_LP64_OFF64,
    _SC_XBS5_LPBIG_OFFBIG,
    _SC_XOPEN_LEGACY,
    _SC_XOPEN_REALTIME,
    _SC_XOPEN_REALTIME_THREADS,
    _SC_ADVISORY_INFO,
    _SC_BARRIERS,
    _SC_BASE,
    _SC_C_LANG_SUPPORT,
    _SC_C_LANG_SUPPORT_R,
    _SC_CLOCK_SELECTION,
    _SC_CPUTIME,
    _SC_THREAD_CPUTIME,
    _SC_DEVICE_IO,
    _SC_DEVICE_SPECIFIC,
    _SC_DEVICE_SPECIFIC_R,
    _SC_FD_MGMT,
    _SC_FIFO,
    _SC_PIPE,
    _SC_FILE_ATTRIBUTES,
    _SC_FILE_LOCKING,
    _SC_FILE_SYSTEM,
    _SC_MONOTONIC_CLOCK,
    _SC_MULTI_PROCESS,
    _SC_SINGLE_PROCESS,
    _SC_NETWORKING,
    _SC_READER_WRITER_LOCKS,
    _SC_SPIN_LOCKS,
    _SC_REGEXP,
    _SC_REGEX_VERSION,
    _SC_SHELL,
    _SC_SIGNALS,
    _SC_SPAWN,
    _SC_SPORADIC_SERVER,
    _SC_THREAD_SPORADIC_SERVER,
    _SC_SYSTEM_DATABASE,
    _SC_SYSTEM_DATABASE_R,
    _SC_TIMEOUTS,
    _SC_TYPED_MEMORY_OBJECTS,
    _SC_USER_GROUPS,
    _SC_USER_GROUPS_R,
    _SC_2_PBS,
    _SC_2_PBS_ACCOUNTING,
    _SC_2_PBS_LOCATE,
    _SC_2_PBS_MESSAGE,
    _SC_2_PBS_TRACK,
    _SC_SYMLOOP_MAX,
    _SC_STREAMS,
    _SC_2_PBS_CHECKPOINT,
    _SC_V6_ILP32_OFF32,
    _SC_V6_ILP32_OFFBIG,
    _SC_V6_LP64_OFF64,
    _SC_V6_LPBIG_OFFBIG,
    _SC_HOST_NAME_MAX,
    _SC_TRACE,
    _SC_TRACE_EVENT_FILTER,
    _SC_TRACE_INHERIT,
    _SC_TRACE_LOG,
    _SC_LEVEL1_ICACHE_SIZE,
    _SC_LEVEL1_ICACHE_ASSOC,
    _SC_LEVEL1_ICACHE_LINESIZE,
    _SC_LEVEL1_DCACHE_SIZE,
    _SC_LEVEL1_DCACHE_ASSOC,
    _SC_LEVEL1_DCACHE_LINESIZE,
    _SC_LEVEL2_CACHE_SIZE,
    _SC_LEVEL2_CACHE_ASSOC,
    _SC_LEVEL2_CACHE_LINESIZE,
    _SC_LEVEL3_CACHE_SIZE,
    _SC_LEVEL3_CACHE_ASSOC,
    _SC_LEVEL3_CACHE_LINESIZE,
    _SC_LEVEL4_CACHE_SIZE,
    _SC_LEVEL4_CACHE_ASSOC,
    _SC_LEVEL4_CACHE_LINESIZE,
    _SC_IPV6 = _SC_LEVEL1_ICACHE_SIZE + 50,
    _SC_RAW_SOCKETS,
    _SC_V7_ILP32_OFF32,
    _SC_V7_ILP32_OFFBIG,
    _SC_V7_LP64_OFF64,
    _SC_V7_LPBIG_OFFBIG,
    _SC_SS_REPL_MAX,
    _SC_TRACE_EVENT_NAME_MAX,
    _SC_TRACE_NAME_MAX,
    _SC_TRACE_SYS_MAX,
    _SC_TRACE_USER_EVENT_MAX,
    _SC_XOPEN_STREAMS,
    _SC_THREAD_ROBUST_PRIO_INHERIT,
    _SC_THREAD_ROBUST_PRIO_PROTECT
};

enum
{
    _CS_PATH,
    _CS_V6_WIDTH_RESTRICTED_ENVS,
    _CS_GNU_LIBC_VERSION,
    _CS_GNU_LIBPTHREAD_VERSION,
    _CS_V5_WIDTH_RESTRICTED_ENVS,
    _CS_V7_WIDTH_RESTRICTED_ENVS,
    _CS_LFS_CFLAGS = 1000,
    _CS_LFS_LDFLAGS,
    _CS_LFS_LIBS,
    _CS_LFS_LINTFLAGS,
    _CS_LFS64_CFLAGS,
    _CS_LFS64_LDFLAGS,
    _CS_LFS64_LIBS,
    _CS_LFS64_LINTFLAGS,
    _CS_XBS5_ILP32_OFF32_CFLAGS = 1100,
    _CS_XBS5_ILP32_OFF32_LDFLAGS,
    _CS_XBS5_ILP32_OFF32_LIBS,
    _CS_XBS5_ILP32_OFF32_LINTFLAGS,
    _CS_XBS5_ILP32_OFFBIG_CFLAGS,
    _CS_XBS5_ILP32_OFFBIG_LDFLAGS,
    _CS_XBS5_ILP32_OFFBIG_LIBS,
    _CS_XBS5_ILP32_OFFBIG_LINTFLAGS,
    _CS_XBS5_LP64_OFF64_CFLAGS,
    _CS_XBS5_LP64_OFF64_LDFLAGS,
    _CS_XBS5_LP64_OFF64_LIBS,
    _CS_XBS5_LP64_OFF64_LINTFLAGS,
    _CS_XBS5_LPBIG_OFFBIG_CFLAGS,
    _CS_XBS5_LPBIG_OFFBIG_LDFLAGS,
    _CS_XBS5_LPBIG_OFFBIG_LIBS,
    _CS_XBS5_LPBIG_OFFBIG_LINTFLAGS,
    _CS_POSIX_V6_ILP32_OFF32_CFLAGS,
    _CS_POSIX_V6_ILP32_OFF32_LDFLAGS,
    _CS_POSIX_V6_ILP32_OFF32_LIBS,
    _CS_POSIX_V6_ILP32_OFF32_LINTFLAGS,
    _CS_POSIX_V6_ILP32_OFFBIG_CFLAGS,
    _CS_POSIX_V6_ILP32_OFFBIG_LDFLAGS,
    _CS_POSIX_V6_ILP32_OFFBIG_LIBS,
    _CS_POSIX_V6_ILP32_OFFBIG_LINTFLAGS,
    _CS_POSIX_V6_LP64_OFF64_CFLAGS,
    _CS_POSIX_V6_LP64_OFF64_LDFLAGS,
    _CS_POSIX_V6_LP64_OFF64_LIBS,
    _CS_POSIX_V6_LP64_OFF64_LINTFLAGS,
    _CS_POSIX_V6_LPBIG_OFFBIG_CFLAGS,
    _CS_POSIX_V6_LPBIG_OFFBIG_LDFLAGS,
    _CS_POSIX_V6_LPBIG_OFFBIG_LIBS,
    _CS_POSIX_V6_LPBIG_OFFBIG_LINTFLAGS,
    _CS_POSIX_V7_ILP32_OFF32_CFLAGS,
    _CS_POSIX_V7_ILP32_OFF32_LDFLAGS,
    _CS_POSIX_V7_ILP32_OFF32_LIBS,
    _CS_POSIX_V7_ILP32_OFF32_LINTFLAGS,
    _CS_POSIX_V7_ILP32_OFFBIG_CFLAGS,
    _CS_POSIX_V7_ILP32_OFFBIG_LDFLAGS,
    _CS_POSIX_V7_ILP32_OFFBIG_LIBS,
    _CS_POSIX_V7_ILP32_OFFBIG_LINTFLAGS,
    _CS_POSIX_V7_LP64_OFF64_CFLAGS,
    _CS_POSIX_V7_LP64_OFF64_LDFLAGS,
    _CS_POSIX_V7_LP64_OFF64_LIBS,
    _CS_POSIX_V7_LP64_OFF64_LINTFLAGS,
    _CS_POSIX_V7_LPBIG_OFFBIG_CFLAGS,
    _CS_POSIX_V7_LPBIG_OFFBIG_LDFLAGS,
    _CS_POSIX_V7_LPBIG_OFFBIG_LIBS,
    _CS_POSIX_V7_LPBIG_OFFBIG_LINTFLAGS,
    _CS_V6_ENV,
    _CS_V7_ENV
};

extern long int pathconf (__const char *__path, int __name) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern long int fpathconf (int __fd, int __name) __attribute__ ((__nothrow__));

extern long int sysconf (int __name) __attribute__ ((__nothrow__));

extern size_t confstr (int __name, char *__buf, size_t __len) __attribute__ ((__nothrow__));

extern __pid_t getpid (void) __attribute__ ((__nothrow__));

extern __pid_t getppid (void) __attribute__ ((__nothrow__));

extern __pid_t getpgrp (void) __attribute__ ((__nothrow__));

extern __pid_t __getpgid (__pid_t __pid) __attribute__ ((__nothrow__));

extern __pid_t getpgid (__pid_t __pid) __attribute__ ((__nothrow__));

extern int setpgid (__pid_t __pid, __pid_t __pgid) __attribute__ ((__nothrow__));

extern int setpgrp (void) __attribute__ ((__nothrow__));

extern __pid_t setsid (void) __attribute__ ((__nothrow__));

extern __pid_t getsid (__pid_t __pid) __attribute__ ((__nothrow__));

extern __uid_t getuid (void) __attribute__ ((__nothrow__));

extern __uid_t geteuid (void) __attribute__ ((__nothrow__));

extern __gid_t getgid (void) __attribute__ ((__nothrow__));

extern __gid_t getegid (void) __attribute__ ((__nothrow__));

extern int getgroups (int __size, __gid_t __list[]) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern int setuid (__uid_t __uid) __attribute__ ((__nothrow__));

extern int setreuid (__uid_t __ruid, __uid_t __euid) __attribute__ ((__nothrow__));

extern int seteuid (__uid_t __uid) __attribute__ ((__nothrow__));

extern int setgid (__gid_t __gid) __attribute__ ((__nothrow__));

extern int setregid (__gid_t __rgid, __gid_t __egid) __attribute__ ((__nothrow__));

extern int setegid (__gid_t __gid) __attribute__ ((__nothrow__));

extern __pid_t fork (void) __attribute__ ((__nothrow__));

extern __pid_t vfork (void) __attribute__ ((__nothrow__));

extern char *ttyname (int __fd) __attribute__ ((__nothrow__));

extern int ttyname_r (int __fd, char *__buf, size_t __buflen) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2))) __attribute__ ((__warn_unused_result__));

extern int isatty (int __fd) __attribute__ ((__nothrow__));

extern int ttyslot (void) __attribute__ ((__nothrow__));

extern int link (__const char *__from, __const char *__to) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2))) __attribute__ ((__warn_unused_result__));

extern int linkat (int __fromfd, __const char *__from, int __tofd, __const char *__to, int __flags) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 4))) __attribute__ ((__warn_unused_result__));

extern int symlink (__const char *__from, __const char *__to) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2))) __attribute__ ((__warn_unused_result__));

extern ssize_t readlink (__const char *__restrict __path, char *__restrict __buf, size_t __len) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2))) __attribute__ ((__warn_unused_result__));

extern int symlinkat (__const char *__from, int __tofd, __const char *__to) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 3))) __attribute__ ((__warn_unused_result__));

extern ssize_t readlinkat (int __fd, __const char *__restrict __path, char *__restrict __buf, size_t __len) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 3))) __attribute__ ((__warn_unused_result__));

extern int unlink (__const char *__name) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int unlinkat (int __fd, __const char *__name, int __flag) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern int rmdir (__const char *__path) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern __pid_t tcgetpgrp (int __fd) __attribute__ ((__nothrow__));

extern int tcsetpgrp (int __fd, __pid_t __pgrp_id) __attribute__ ((__nothrow__));

extern char *getlogin (void);

extern int getlogin_r (char *__name, size_t __name_len) __attribute__ ((__nonnull__ (1)));

extern int setlogin (__const char *__name) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern char *optarg;

extern int optind;

extern int opterr;

extern int optopt;

extern int getopt (int ___argc, char *const *___argv, const char *__shortopts) __attribute__ ((__nothrow__));

extern int gethostname (char *__name, size_t __len) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int sethostname (__const char *__name, size_t __len) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern int sethostid (long int __id) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern int getdomainname (char *__name, size_t __len) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));
extern int setdomainname (__const char *__name, size_t __len) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern int vhangup (void) __attribute__ ((__nothrow__));

extern int revoke (__const char *__file) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern int profil (unsigned short int *__sample_buffer, size_t __size, size_t __offset, unsigned int __scale) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int acct (__const char *__name) __attribute__ ((__nothrow__));

extern char *getusershell (void) __attribute__ ((__nothrow__));
extern void endusershell (void) __attribute__ ((__nothrow__));
extern void setusershell (void) __attribute__ ((__nothrow__));

extern int daemon (int __nochdir, int __noclose) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern int chroot (__const char *__path) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern char *getpass (__const char *__prompt) __attribute__ ((__nonnull__ (1)));

extern int fsync (int __fd);

extern long int gethostid (void);

extern void sync (void) __attribute__ ((__nothrow__));

extern int getpagesize (void) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern int getdtablesize (void) __attribute__ ((__nothrow__));

extern int truncate (__const char *__file, __off_t __length) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern int ftruncate (int __fd, __off_t __length) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern int brk (void *__addr) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern void *sbrk (intptr_t __delta) __attribute__ ((__nothrow__));

extern long int syscall (long int __sysno, ...) __attribute__ ((__nothrow__));

extern int lockf (int __fd, int __cmd, __off_t __len) __attribute__ ((__warn_unused_result__));

extern int fdatasync (int __fildes);

extern char *ctermid (char *__s) __attribute__ ((__nothrow__));

extern ssize_t __read_chk (int __fd, void *__buf, size_t __nbytes, size_t __buflen) __attribute__ ((__warn_unused_result__));
extern ssize_t __read_alias (int __fd, void *__buf, size_t __nbytes) __asm__ ("" "read") __attribute__ ((__warn_unused_result__));
extern ssize_t __read_chk_warn (int __fd, void *__buf, size_t __nbytes, size_t __buflen) __asm__ ("" "__read_chk") __attribute__ ((__warn_unused_result__)) __attribute__((__warning__ ("read called with bigger length than size of " "the destination buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) __attribute__ ((__warn_unused_result__)) ssize_t read (int __fd, void *__buf, size_t __nbytes)
{
    if (__builtin_object_size (__buf, 0) != (size_t) -1)
    {
        if (!__builtin_constant_p (__nbytes))
            return __read_chk (__fd, __buf, __nbytes, __builtin_object_size (__buf, 0));

        if (__nbytes > __builtin_object_size (__buf, 0))
            return __read_chk_warn (__fd, __buf, __nbytes, __builtin_object_size (__buf, 0));
    }
    return __read_alias (__fd, __buf, __nbytes);
}

extern ssize_t __readlink_chk (__const char *__restrict __path, char *__restrict __buf, size_t __len, size_t __buflen) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2))) __attribute__ ((__warn_unused_result__));
extern ssize_t __readlink_alias (__const char *__restrict __path, char *__restrict __buf, size_t __len) __asm__ ("" "readlink") __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2))) __attribute__ ((__warn_unused_result__));
extern ssize_t __readlink_chk_warn (__const char *__restrict __path, char *__restrict __buf, size_t __len, size_t __buflen) __asm__ ("" "__readlink_chk") __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2))) __attribute__ ((__warn_unused_result__)) __attribute__((__warning__ ("readlink called with bigger length " "than size of destination buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) __attribute__ ((__nonnull__ (1, 2))) __attribute__ ((__warn_unused_result__)) ssize_t __attribute__ ((__nothrow__)) readlink (__const char *__restrict __path, char *__restrict __buf, size_t __len)
{
    if (__builtin_object_size (__buf, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__len))
            return __readlink_chk (__path, __buf, __len, __builtin_object_size (__buf, 1 > 1));

        if ( __len > __builtin_object_size (__buf, 1 > 1))
            return __readlink_chk_warn (__path, __buf, __len, __builtin_object_size (__buf, 1 > 1));
    }
    return __readlink_alias (__path, __buf, __len);
}

extern ssize_t __readlinkat_chk (int __fd, __const char *__restrict __path, char *__restrict __buf, size_t __len, size_t __buflen) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 3))) __attribute__ ((__warn_unused_result__));
extern ssize_t __readlinkat_alias (int __fd, __const char *__restrict __path, char *__restrict __buf, size_t __len) __asm__ ("" "readlinkat") __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 3))) __attribute__ ((__warn_unused_result__));
extern ssize_t __readlinkat_chk_warn (int __fd, __const char *__restrict __path, char *__restrict __buf, size_t __len, size_t __buflen) __asm__ ("" "__readlinkat_chk") __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 3))) __attribute__ ((__warn_unused_result__)) __attribute__((__warning__ ("readlinkat called with bigger " "length than size of destination " "buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) __attribute__ ((__nonnull__ (2, 3))) __attribute__ ((__warn_unused_result__)) ssize_t __attribute__ ((__nothrow__)) readlinkat (int __fd, __const char *__restrict __path, char *__restrict __buf, size_t __len)
{
    if (__builtin_object_size (__buf, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__len))
            return __readlinkat_chk (__fd, __path, __buf, __len, __builtin_object_size (__buf, 1 > 1));

        if (__len > __builtin_object_size (__buf, 1 > 1))
            return __readlinkat_chk_warn (__fd, __path, __buf, __len, __builtin_object_size (__buf, 1 > 1));
    }
    return __readlinkat_alias (__fd, __path, __buf, __len);
}

extern char *__getcwd_chk (char *__buf, size_t __size, size_t __buflen) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));
extern char *__getcwd_alias (char *__buf, size_t __size) __asm__ ("" "getcwd") __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));
extern char *__getcwd_chk_warn (char *__buf, size_t __size, size_t __buflen) __asm__ ("" "__getcwd_chk") __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__)) __attribute__((__warning__ ("getcwd caller with bigger length than size of " "destination buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) __attribute__ ((__warn_unused_result__)) char * __attribute__ ((__nothrow__)) getcwd (char *__buf, size_t __size)
{
    if (__builtin_object_size (__buf, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__size))
            return __getcwd_chk (__buf, __size, __builtin_object_size (__buf, 1 > 1));

        if (__size > __builtin_object_size (__buf, 1 > 1))
            return __getcwd_chk_warn (__buf, __size, __builtin_object_size (__buf, 1 > 1));
    }
    return __getcwd_alias (__buf, __size);
}

extern char *__getwd_chk (char *__buf, size_t buflen) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));
extern char *__getwd_warn (char *__buf) __asm__ ("" "getwd") __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__)) __attribute__((__warning__ ("please use getcwd instead, as getwd " "doesn't specify buffer size")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__deprecated__)) __attribute__ ((__warn_unused_result__)) char * __attribute__ ((__nothrow__)) getwd (char *__buf)
{
    if (__builtin_object_size (__buf, 1 > 1) != (size_t) -1)
        return __getwd_chk (__buf, __builtin_object_size (__buf, 1 > 1));
    return __getwd_warn (__buf);
}

extern size_t __confstr_chk (int __name, char *__buf, size_t __len, size_t __buflen) __attribute__ ((__nothrow__));
extern size_t __confstr_alias (int __name, char *__buf, size_t __len) __asm__ ("" "confstr") __attribute__ ((__nothrow__));
extern size_t __confstr_chk_warn (int __name, char *__buf, size_t __len, size_t __buflen) __asm__ ("" "__confstr_chk") __attribute__ ((__nothrow__)) __attribute__((__warning__ ("confstr called with bigger length than size of destination " "buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) size_t __attribute__ ((__nothrow__)) confstr (int __name, char *__buf, size_t __len)
{
    if (__builtin_object_size (__buf, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__len))
            return __confstr_chk (__name, __buf, __len, __builtin_object_size (__buf, 1 > 1));

        if (__builtin_object_size (__buf, 1 > 1) < __len)
            return __confstr_chk_warn (__name, __buf, __len, __builtin_object_size (__buf, 1 > 1));
    }
    return __confstr_alias (__name, __buf, __len);
}

extern int __getgroups_chk (int __size, __gid_t __list[], size_t __listlen) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));
extern int __getgroups_alias (int __size, __gid_t __list[]) __asm__ ("" "getgroups") __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));
extern int __getgroups_chk_warn (int __size, __gid_t __list[], size_t __listlen) __asm__ ("" "__getgroups_chk") __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__)) __attribute__((__warning__ ("getgroups called with bigger group count than what " "can fit into destination buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) int __attribute__ ((__nothrow__)) getgroups (int __size, __gid_t __list[])
{
    if (__builtin_object_size (__list, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__size) || __size < 0)
            return __getgroups_chk (__size, __list, __builtin_object_size (__list, 1 > 1));

        if (__size * sizeof (__gid_t) > __builtin_object_size (__list, 1 > 1))
            return __getgroups_chk_warn (__size, __list, __builtin_object_size (__list, 1 > 1));
    }
    return __getgroups_alias (__size, __list);
}

extern int __ttyname_r_chk (int __fd, char *__buf, size_t __buflen, size_t __nreal) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));
extern int __ttyname_r_alias (int __fd, char *__buf, size_t __buflen) __asm__ ("" "ttyname_r") __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));
extern int __ttyname_r_chk_warn (int __fd, char *__buf, size_t __buflen, size_t __nreal) __asm__ ("" "__ttyname_r_chk") __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2))) __attribute__((__warning__ ("ttyname_r called with bigger buflen than " "size of destination buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) int __attribute__ ((__nothrow__)) ttyname_r (int __fd, char *__buf, size_t __buflen)
{
    if (__builtin_object_size (__buf, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__buflen))
            return __ttyname_r_chk (__fd, __buf, __buflen, __builtin_object_size (__buf, 1 > 1));

        if (__buflen > __builtin_object_size (__buf, 1 > 1))
            return __ttyname_r_chk_warn (__fd, __buf, __buflen, __builtin_object_size (__buf, 1 > 1));
    }
    return __ttyname_r_alias (__fd, __buf, __buflen);
}

extern int __getlogin_r_chk (char *__buf, size_t __buflen, size_t __nreal) __attribute__ ((__nonnull__ (1)));
extern int __getlogin_r_alias (char *__buf, size_t __buflen) __asm__ ("" "getlogin_r") __attribute__ ((__nonnull__ (1)));
extern int __getlogin_r_chk_warn (char *__buf, size_t __buflen, size_t __nreal) __asm__ ("" "__getlogin_r_chk") __attribute__ ((__nonnull__ (1))) __attribute__((__warning__ ("getlogin_r called with bigger buflen than " "size of destination buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) int getlogin_r (char *__buf, size_t __buflen)
{
    if (__builtin_object_size (__buf, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__buflen))
            return __getlogin_r_chk (__buf, __buflen, __builtin_object_size (__buf, 1 > 1));

        if (__buflen > __builtin_object_size (__buf, 1 > 1))
            return __getlogin_r_chk_warn (__buf, __buflen, __builtin_object_size (__buf, 1 > 1));
    }
    return __getlogin_r_alias (__buf, __buflen);
}

extern int __gethostname_chk (char *__buf, size_t __buflen, size_t __nreal) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
extern int __gethostname_alias (char *__buf, size_t __buflen) __asm__ ("" "gethostname") __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));
extern int __gethostname_chk_warn (char *__buf, size_t __buflen, size_t __nreal) __asm__ ("" "__gethostname_chk") __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__((__warning__ ("gethostname called with bigger buflen than " "size of destination buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) int __attribute__ ((__nothrow__)) gethostname (char *__buf, size_t __buflen)
{
    if (__builtin_object_size (__buf, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__buflen))
            return __gethostname_chk (__buf, __buflen, __builtin_object_size (__buf, 1 > 1));

        if (__buflen > __builtin_object_size (__buf, 1 > 1))
            return __gethostname_chk_warn (__buf, __buflen, __builtin_object_size (__buf, 1 > 1));
    }
    return __gethostname_alias (__buf, __buflen);
}

extern int __getdomainname_chk (char *__buf, size_t __buflen, size_t __nreal) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));
extern int __getdomainname_alias (char *__buf, size_t __buflen) __asm__ ("" "getdomainname") __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));
extern int __getdomainname_chk_warn (char *__buf, size_t __buflen, size_t __nreal) __asm__ ("" "__getdomainname_chk") __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__)) __attribute__((__warning__ ("getdomainname called with bigger " "buflen than size of destination " "buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) int __attribute__ ((__nothrow__)) getdomainname (char *__buf, size_t __buflen)
{
    if (__builtin_object_size (__buf, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__buflen))
            return __getdomainname_chk (__buf, __buflen, __builtin_object_size (__buf, 1 > 1));

        if (__buflen > __builtin_object_size (__buf, 1 > 1))
            return __getdomainname_chk_warn (__buf, __buflen, __builtin_object_size (__buf, 1 > 1));
    }
    return __getdomainname_alias (__buf, __buflen);
}

struct dirent
{
    __ino_t d_ino;
    __off_t d_off;

    unsigned short int d_reclen;
    unsigned char d_type;
    char d_name[256];
};

enum
{
    DT_UNKNOWN = 0,
    DT_FIFO = 1,
    DT_CHR = 2,
    DT_DIR = 4,
    DT_BLK = 6,
    DT_REG = 8,
    DT_LNK = 10,
    DT_SOCK = 12,
    DT_WHT = 14
};

typedef struct __dirstream DIR;

extern DIR *opendir (__const char *__name) __attribute__ ((__nonnull__ (1)));

extern DIR *fdopendir (int __fd);

extern int closedir (DIR *__dirp) __attribute__ ((__nonnull__ (1)));

extern struct dirent *readdir (DIR *__dirp) __attribute__ ((__nonnull__ (1)));

extern int readdir_r (DIR *__restrict __dirp, struct dirent *__restrict __entry, struct dirent **__restrict __result) __attribute__ ((__nonnull__ (1, 2, 3)));

extern void rewinddir (DIR *__dirp) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern void seekdir (DIR *__dirp, long int __pos) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern long int telldir (DIR *__dirp) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int dirfd (DIR *__dirp) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int scandir (__const char *__restrict __dir, struct dirent ***__restrict __namelist, int (*__selector) (__const struct dirent *), int (*__cmp) (__const struct dirent **, __const struct dirent **)) __attribute__ ((__nonnull__ (1, 2)));

extern int alphasort (__const struct dirent **__e1, __const struct dirent **__e2) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));

extern __ssize_t getdirentries (int __fd, char *__restrict __buf, size_t __nbytes, __off_t *__restrict __basep) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 4)));

struct tm
{
    int tm_sec;
    int tm_min;
    int tm_hour;
    int tm_mday;
    int tm_mon;
    int tm_year;
    int tm_wday;
    int tm_yday;
    int tm_isdst;

    long int tm_gmtoff;
    __const char *tm_zone;
};

struct itimerspec
{
    struct timespec it_interval;
    struct timespec it_value;
};

struct sigevent;

extern clock_t clock (void) __attribute__ ((__nothrow__));

extern time_t time (time_t *__timer) __attribute__ ((__nothrow__));

extern double difftime (time_t __time1, time_t __time0) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern time_t mktime (struct tm *__tp) __attribute__ ((__nothrow__));

extern size_t strftime (char *__restrict __s, size_t __maxsize, __const char *__restrict __format, __const struct tm *__restrict __tp) __attribute__ ((__nothrow__));

extern size_t strftime_l (char *__restrict __s, size_t __maxsize, __const char *__restrict __format, __const struct tm *__restrict __tp, __locale_t __loc) __attribute__ ((__nothrow__));

extern struct tm *gmtime (__const time_t *__timer) __attribute__ ((__nothrow__));

extern struct tm *localtime (__const time_t *__timer) __attribute__ ((__nothrow__));

extern struct tm *gmtime_r (__const time_t *__restrict __timer, struct tm *__restrict __tp) __attribute__ ((__nothrow__));

extern struct tm *localtime_r (__const time_t *__restrict __timer, struct tm *__restrict __tp) __attribute__ ((__nothrow__));

extern char *asctime (__const struct tm *__tp) __attribute__ ((__nothrow__));

extern char *ctime (__const time_t *__timer) __attribute__ ((__nothrow__));

extern char *asctime_r (__const struct tm *__restrict __tp, char *__restrict __buf) __attribute__ ((__nothrow__));

extern char *ctime_r (__const time_t *__restrict __timer, char *__restrict __buf) __attribute__ ((__nothrow__));

extern char *__tzname[2];
extern int __daylight;
extern long int __timezone;

extern char *tzname[2];

extern void tzset (void) __attribute__ ((__nothrow__));

extern int daylight;
extern long int timezone;

extern int stime (__const time_t *__when) __attribute__ ((__nothrow__));

extern time_t timegm (struct tm *__tp) __attribute__ ((__nothrow__));

extern time_t timelocal (struct tm *__tp) __attribute__ ((__nothrow__));

extern int dysize (int __year) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern int nanosleep (__const struct timespec *__requested_time, struct timespec *__remaining);

extern int clock_getres (clockid_t __clock_id, struct timespec *__res) __attribute__ ((__nothrow__));

extern int clock_gettime (clockid_t __clock_id, struct timespec *__tp) __attribute__ ((__nothrow__));

extern int clock_settime (clockid_t __clock_id, __const struct timespec *__tp) __attribute__ ((__nothrow__));

extern int clock_nanosleep (clockid_t __clock_id, int __flags, __const struct timespec *__req, struct timespec *__rem);

extern int clock_getcpuclockid (pid_t __pid, clockid_t *__clock_id) __attribute__ ((__nothrow__));

extern int timer_create (clockid_t __clock_id, struct sigevent *__restrict __evp, timer_t *__restrict __timerid) __attribute__ ((__nothrow__));

extern int timer_delete (timer_t __timerid) __attribute__ ((__nothrow__));

extern int timer_settime (timer_t __timerid, int __flags, __const struct itimerspec *__restrict __value, struct itimerspec *__restrict __ovalue) __attribute__ ((__nothrow__));

extern int timer_gettime (timer_t __timerid, struct itimerspec *__value) __attribute__ ((__nothrow__));

extern int timer_getoverrun (timer_t __timerid) __attribute__ ((__nothrow__));

extern int __sigismember (__const __sigset_t *, int);
extern int __sigaddset (__sigset_t *, int);
extern int __sigdelset (__sigset_t *, int);

extern __inline __attribute__ ((__gnu_inline__)) int __sigismember (__const __sigset_t *__set, int __sig)
{
    unsigned long int __mask = (((unsigned long int) 1) << (((__sig) - 1) % (8 * sizeof (unsigned long int))));
    unsigned long int __word = (((__sig) - 1) / (8 * sizeof (unsigned long int)));
    return (__set->__val[__word] & __mask) ? 1 : 0;
}
extern __inline __attribute__ ((__gnu_inline__)) int __sigaddset ( __sigset_t *__set, int __sig)
{
    unsigned long int __mask = (((unsigned long int) 1) << (((__sig) - 1) % (8 * sizeof (unsigned long int))));
    unsigned long int __word = (((__sig) - 1) / (8 * sizeof (unsigned long int)));
    return ((__set->__val[__word] |= __mask), 0);
}
extern __inline __attribute__ ((__gnu_inline__)) int __sigdelset ( __sigset_t *__set, int __sig)
{
    unsigned long int __mask = (((unsigned long int) 1) << (((__sig) - 1) % (8 * sizeof (unsigned long int))));
    unsigned long int __word = (((__sig) - 1) / (8 * sizeof (unsigned long int)));
    return ((__set->__val[__word] &= ~__mask), 0);
}

typedef __sig_atomic_t sig_atomic_t;

typedef union sigval
{
    int sival_int;
    void *sival_ptr;
} sigval_t;

typedef struct siginfo
{
    int si_signo;
    int si_errno;

    int si_code;

    union
    {
        int _pad[((128 / sizeof (int)) - 4)];

        struct
        {
            __pid_t si_pid;
            __uid_t si_uid;
        } _kill;

        struct
        {
            int si_tid;
            int si_overrun;
            sigval_t si_sigval;
        } _timer;

        struct
        {
            __pid_t si_pid;
            __uid_t si_uid;
            sigval_t si_sigval;
        } _rt;

        struct
        {
            __pid_t si_pid;
            __uid_t si_uid;
            int si_status;
            __clock_t si_utime;
            __clock_t si_stime;
        } _sigchld;

        struct
        {
            void *si_addr;
        } _sigfault;

        struct
        {
            long int si_band;
            int si_fd;
        } _sigpoll;
    } _sifields;
} siginfo_t;

enum
{
    SI_ASYNCNL = -60,
    SI_TKILL = -6,
    SI_SIGIO,
    SI_ASYNCIO,
    SI_MESGQ,
    SI_TIMER,
    SI_QUEUE,
    SI_USER,
    SI_KERNEL = 0x80
};

enum
{
    ILL_ILLOPC = 1,
    ILL_ILLOPN,
    ILL_ILLADR,
    ILL_ILLTRP,
    ILL_PRVOPC,
    ILL_PRVREG,
    ILL_COPROC,
    ILL_BADSTK
};

enum
{
    FPE_INTDIV = 1,
    FPE_INTOVF,
    FPE_FLTDIV,
    FPE_FLTOVF,
    FPE_FLTUND,
    FPE_FLTRES,
    FPE_FLTINV,
    FPE_FLTSUB
};

enum
{
    SEGV_MAPERR = 1,
    SEGV_ACCERR
};

enum
{
    BUS_ADRALN = 1,
    BUS_ADRERR,
    BUS_OBJERR
};

enum
{
    TRAP_BRKPT = 1,
    TRAP_TRACE
};

enum
{
    CLD_EXITED = 1,
    CLD_KILLED,
    CLD_DUMPED,
    CLD_TRAPPED,
    CLD_STOPPED,
    CLD_CONTINUED
};

enum
{
    POLL_IN = 1,
    POLL_OUT,
    POLL_MSG,
    POLL_ERR,
    POLL_PRI,
    POLL_HUP
};

typedef struct sigevent
{
    sigval_t sigev_value;
    int sigev_signo;
    int sigev_notify;

    union
    {
        int _pad[((64 / sizeof (int)) - 4)];

        __pid_t _tid;

        struct
        {
            void (*_function) (sigval_t);
            void *_attribute;
        } _sigev_thread;
    } _sigev_un;
} sigevent_t;

enum
{
    SIGEV_SIGNAL = 0,
    SIGEV_NONE,
    SIGEV_THREAD,
    SIGEV_THREAD_ID = 4
};

typedef void (*__sighandler_t) (int);

extern __sighandler_t __sysv_signal (int __sig, __sighandler_t __handler) __attribute__ ((__nothrow__));

extern __sighandler_t signal (int __sig, __sighandler_t __handler) __attribute__ ((__nothrow__));

extern int kill (__pid_t __pid, int __sig) __attribute__ ((__nothrow__));

extern int killpg (__pid_t __pgrp, int __sig) __attribute__ ((__nothrow__));

extern int raise (int __sig) __attribute__ ((__nothrow__));

extern __sighandler_t ssignal (int __sig, __sighandler_t __handler) __attribute__ ((__nothrow__));
extern int gsignal (int __sig) __attribute__ ((__nothrow__));

extern void psignal (int __sig, __const char *__s);

extern void psiginfo (__const siginfo_t *__pinfo, __const char *__s);

extern int __sigpause (int __sig_or_mask, int __is_sig);

extern int sigblock (int __mask) __attribute__ ((__nothrow__)) __attribute__ ((__deprecated__));

extern int sigsetmask (int __mask) __attribute__ ((__nothrow__)) __attribute__ ((__deprecated__));

extern int siggetmask (void) __attribute__ ((__nothrow__)) __attribute__ ((__deprecated__));

typedef __sighandler_t sig_t;

extern int sigemptyset (sigset_t *__set) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int sigfillset (sigset_t *__set) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int sigaddset (sigset_t *__set, int __signo) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int sigdelset (sigset_t *__set, int __signo) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int sigismember (__const sigset_t *__set, int __signo) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

struct sigaction
{
    union
    {
        __sighandler_t sa_handler;

        void (*sa_sigaction) (int, siginfo_t *, void *);
    }
    __sigaction_handler;

    __sigset_t sa_mask;

    int sa_flags;

    void (*sa_restorer) (void);
};

extern int sigprocmask (int __how, __const sigset_t *__restrict __set, sigset_t *__restrict __oset) __attribute__ ((__nothrow__));

extern int sigsuspend (__const sigset_t *__set) __attribute__ ((__nonnull__ (1)));

extern int sigaction (int __sig, __const struct sigaction *__restrict __act, struct sigaction *__restrict __oact) __attribute__ ((__nothrow__));

extern int sigpending (sigset_t *__set) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int sigwait (__const sigset_t *__restrict __set, int *__restrict __sig) __attribute__ ((__nonnull__ (1, 2)));

extern int sigwaitinfo (__const sigset_t *__restrict __set, siginfo_t *__restrict __info) __attribute__ ((__nonnull__ (1)));

extern int sigtimedwait (__const sigset_t *__restrict __set, siginfo_t *__restrict __info, __const struct timespec *__restrict __timeout) __attribute__ ((__nonnull__ (1)));

extern int sigqueue (__pid_t __pid, int __sig, __const union sigval __val) __attribute__ ((__nothrow__));

extern __const char *__const _sys_siglist[65];
extern __const char *__const sys_siglist[65];

struct sigvec
{
    __sighandler_t sv_handler;
    int sv_mask;

    int sv_flags;
};

extern int sigvec (int __sig, __const struct sigvec *__vec, struct sigvec *__ovec) __attribute__ ((__nothrow__));

struct _fpreg
{
    unsigned short significand[4];
    unsigned short exponent;
};

struct _fpxreg
{
    unsigned short significand[4];
    unsigned short exponent;
    unsigned short padding[3];
};

struct _xmmreg
{
    __uint32_t element[4];
};

struct _fpstate
{
    __uint16_t cwd;
    __uint16_t swd;
    __uint16_t ftw;
    __uint16_t fop;
    __uint64_t rip;
    __uint64_t rdp;
    __uint32_t mxcsr;
    __uint32_t mxcr_mask;
    struct _fpxreg _st[8];
    struct _xmmreg _xmm[16];
    __uint32_t padding[24];
};

struct sigcontext
{
    unsigned long r8;
    unsigned long r9;
    unsigned long r10;
    unsigned long r11;
    unsigned long r12;
    unsigned long r13;
    unsigned long r14;
    unsigned long r15;
    unsigned long rdi;
    unsigned long rsi;
    unsigned long rbp;
    unsigned long rbx;
    unsigned long rdx;
    unsigned long rax;
    unsigned long rcx;
    unsigned long rsp;
    unsigned long rip;
    unsigned long eflags;
    unsigned short cs;
    unsigned short gs;
    unsigned short fs;
    unsigned short __pad0;
    unsigned long err;
    unsigned long trapno;
    unsigned long oldmask;
    unsigned long cr2;
    struct _fpstate * fpstate;
    unsigned long __reserved1 [8];
};

extern int sigreturn (struct sigcontext *__scp) __attribute__ ((__nothrow__));

extern int siginterrupt (int __sig, int __interrupt) __attribute__ ((__nothrow__));

struct sigstack
{
    void *ss_sp;
    int ss_onstack;
};

enum
{
    SS_ONSTACK = 1,
    SS_DISABLE
};

typedef struct sigaltstack
{
    void *ss_sp;
    int ss_flags;
    size_t ss_size;
} stack_t;

typedef long int greg_t;

typedef greg_t gregset_t[23];

struct _libc_fpxreg
{
    unsigned short int significand[4];
    unsigned short int exponent;
    unsigned short int padding[3];
};

struct _libc_xmmreg
{
    __uint32_t element[4];
};

struct _libc_fpstate
{
    __uint16_t cwd;
    __uint16_t swd;
    __uint16_t ftw;
    __uint16_t fop;
    __uint64_t rip;
    __uint64_t rdp;
    __uint32_t mxcsr;
    __uint32_t mxcr_mask;
    struct _libc_fpxreg _st[8];
    struct _libc_xmmreg _xmm[16];
    __uint32_t padding[24];
};

typedef struct _libc_fpstate *fpregset_t;

typedef struct
{
    gregset_t gregs;

    fpregset_t fpregs;
    unsigned long __reserved1 [8];
} mcontext_t;

typedef struct ucontext
{
    unsigned long int uc_flags;
    struct ucontext *uc_link;
    stack_t uc_stack;
    mcontext_t uc_mcontext;
    __sigset_t uc_sigmask;
    struct _libc_fpstate __fpregs_mem;
} ucontext_t;

extern int sigstack (struct sigstack *__ss, struct sigstack *__oss) __attribute__ ((__nothrow__)) __attribute__ ((__deprecated__));

extern int sigaltstack (__const struct sigaltstack *__restrict __ss, struct sigaltstack *__restrict __oss) __attribute__ ((__nothrow__));

extern int pthread_sigmask (int __how, __const __sigset_t *__restrict __newmask, __sigset_t *__restrict __oldmask)__attribute__ ((__nothrow__));

extern int pthread_kill (pthread_t __threadid, int __signo) __attribute__ ((__nothrow__));

extern int __libc_current_sigrtmin (void) __attribute__ ((__nothrow__));

extern int __libc_current_sigrtmax (void) __attribute__ ((__nothrow__));

struct passwd
{
    char *pw_name;
    char *pw_passwd;
    __uid_t pw_uid;
    __gid_t pw_gid;
    char *pw_gecos;
    char *pw_dir;
    char *pw_shell;
};

extern void setpwent (void);

extern void endpwent (void);

extern struct passwd *getpwent (void);

extern struct passwd *fgetpwent (FILE *__stream);

extern int putpwent (__const struct passwd *__restrict __p, FILE *__restrict __f);

extern struct passwd *getpwuid (__uid_t __uid);

extern struct passwd *getpwnam (__const char *__name);

extern int getpwent_r (struct passwd *__restrict __resultbuf, char *__restrict __buffer, size_t __buflen, struct passwd **__restrict __result);

extern int getpwuid_r (__uid_t __uid, struct passwd *__restrict __resultbuf, char *__restrict __buffer, size_t __buflen, struct passwd **__restrict __result);

extern int getpwnam_r (__const char *__restrict __name, struct passwd *__restrict __resultbuf, char *__restrict __buffer, size_t __buflen, struct passwd **__restrict __result);

extern int fgetpwent_r (FILE *__restrict __stream, struct passwd *__restrict __resultbuf, char *__restrict __buffer, size_t __buflen, struct passwd **__restrict __result);

extern void *memcpy (void *__restrict __dest, __const void *__restrict __src, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern void *memmove (void *__dest, __const void *__src, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern void *memccpy (void *__restrict __dest, __const void *__restrict __src, int __c, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern void *memset (void *__s, int __c, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int memcmp (__const void *__s1, __const void *__s2, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));

extern void *memchr (__const void *__s, int __c, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1)));

extern char *strcpy (char *__restrict __dest, __const char *__restrict __src) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern char *strncpy (char *__restrict __dest, __const char *__restrict __src, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern char *strcat (char *__restrict __dest, __const char *__restrict __src) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern char *strncat (char *__restrict __dest, __const char *__restrict __src, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int strcmp (__const char *__s1, __const char *__s2) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));

extern int strncmp (__const char *__s1, __const char *__s2, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));

extern int strcoll (__const char *__s1, __const char *__s2) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));

extern size_t strxfrm (char *__restrict __dest, __const char *__restrict __src, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern int strcoll_l (__const char *__s1, __const char *__s2, __locale_t __l) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2, 3)));

extern size_t strxfrm_l (char *__dest, __const char *__src, size_t __n, __locale_t __l) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 4)));

extern char *strdup (__const char *__s) __attribute__ ((__nothrow__)) __attribute__ ((__malloc__)) __attribute__ ((__nonnull__ (1)));

extern char *strndup (__const char *__string, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__malloc__)) __attribute__ ((__nonnull__ (1)));

extern char *strchr (__const char *__s, int __c) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1)));

extern char *strrchr (__const char *__s, int __c) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1)));

extern size_t strcspn (__const char *__s, __const char *__reject) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));

extern size_t strspn (__const char *__s, __const char *__accept) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));

extern char *strpbrk (__const char *__s, __const char *__accept) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));

extern char *strstr (__const char *__haystack, __const char *__needle) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));

extern char *strtok (char *__restrict __s, __const char *__restrict __delim) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern char *__strtok_r (char *__restrict __s, __const char *__restrict __delim, char **__restrict __save_ptr) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 3)));

extern char *strtok_r (char *__restrict __s, __const char *__restrict __delim, char **__restrict __save_ptr) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 3)));

extern size_t strlen (__const char *__s) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1)));

extern size_t strnlen (__const char *__string, size_t __maxlen) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1)));

extern char *strerror (int __errnum) __attribute__ ((__nothrow__));

extern int strerror_r (int __errnum, char *__buf, size_t __buflen) __asm__ ("" "__xpg_strerror_r") __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern char *strerror_l (int __errnum, __locale_t __l) __attribute__ ((__nothrow__));

extern void __bzero (void *__s, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern void bcopy (__const void *__src, void *__dest, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern void bzero (void *__s, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int bcmp (__const void *__s1, __const void *__s2, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));

extern char *index (__const char *__s, int __c) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1)));

extern char *rindex (__const char *__s, int __c) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1)));

extern int ffs (int __i) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern int strcasecmp (__const char *__s1, __const char *__s2) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));

extern int strncasecmp (__const char *__s1, __const char *__s2, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__pure__)) __attribute__ ((__nonnull__ (1, 2)));

extern char *strsep (char **__restrict __stringp, __const char *__restrict __delim) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern char *strsignal (int __sig) __attribute__ ((__nothrow__));

extern char *__stpcpy (char *__restrict __dest, __const char *__restrict __src) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));
extern char *stpcpy (char *__restrict __dest, __const char *__restrict __src) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern char *__stpncpy (char *__restrict __dest, __const char *__restrict __src, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));
extern char *stpncpy (char *__restrict __dest, __const char *__restrict __src, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern void *__rawmemchr (const void *__s, int __c);

extern __inline __attribute__ ((__gnu_inline__)) size_t __strcspn_c1 (__const char *__s, int __reject);
extern __inline __attribute__ ((__gnu_inline__)) size_t __strcspn_c1 (__const char *__s, int __reject)
{
    register size_t __result = 0;
    while (__s[__result] != '\0' && __s[__result] != __reject)
        ++__result;
    return __result;
}

extern __inline __attribute__ ((__gnu_inline__)) size_t __strcspn_c2 (__const char *__s, int __reject1, int __reject2);
extern __inline __attribute__ ((__gnu_inline__)) size_t __strcspn_c2 (__const char *__s, int __reject1, int __reject2)
{
    register size_t __result = 0;
    while (__s[__result] != '\0' && __s[__result] != __reject1 && __s[__result] != __reject2)
        ++__result;
    return __result;
}

extern __inline __attribute__ ((__gnu_inline__)) size_t __strcspn_c3 (__const char *__s, int __reject1, int __reject2, int __reject3);
extern __inline __attribute__ ((__gnu_inline__)) size_t __strcspn_c3 (__const char *__s, int __reject1, int __reject2, int __reject3)
{
    register size_t __result = 0;
    while (__s[__result] != '\0' && __s[__result] != __reject1 && __s[__result] != __reject2 && __s[__result] != __reject3)
        ++__result;
    return __result;
}

extern __inline __attribute__ ((__gnu_inline__)) size_t __strspn_c1 (__const char *__s, int __accept);
extern __inline __attribute__ ((__gnu_inline__)) size_t __strspn_c1 (__const char *__s, int __accept)
{
    register size_t __result = 0;

    while (__s[__result] == __accept)
        ++__result;
    return __result;
}

extern __inline __attribute__ ((__gnu_inline__)) size_t __strspn_c2 (__const char *__s, int __accept1, int __accept2);
extern __inline __attribute__ ((__gnu_inline__)) size_t __strspn_c2 (__const char *__s, int __accept1, int __accept2)
{
    register size_t __result = 0;

    while (__s[__result] == __accept1 || __s[__result] == __accept2)
        ++__result;
    return __result;
}

extern __inline __attribute__ ((__gnu_inline__)) size_t __strspn_c3 (__const char *__s, int __accept1, int __accept2, int __accept3);
extern __inline __attribute__ ((__gnu_inline__)) size_t __strspn_c3 (__const char *__s, int __accept1, int __accept2, int __accept3)
{
    register size_t __result = 0;

    while (__s[__result] == __accept1 || __s[__result] == __accept2 || __s[__result] == __accept3)
        ++__result;
    return __result;
}

extern __inline __attribute__ ((__gnu_inline__)) char *__strpbrk_c2 (__const char *__s, int __accept1, int __accept2);
extern __inline __attribute__ ((__gnu_inline__)) char * __strpbrk_c2 (__const char *__s, int __accept1, int __accept2)
{
    while (*__s != '\0' && *__s != __accept1 && *__s != __accept2)
        ++__s;
    return *__s == '\0' ? ((void *)0) : (char *) (size_t) __s;
}

extern __inline __attribute__ ((__gnu_inline__)) char *__strpbrk_c3 (__const char *__s, int __accept1, int __accept2, int __accept3);
extern __inline __attribute__ ((__gnu_inline__)) char * __strpbrk_c3 (__const char *__s, int __accept1, int __accept2, int __accept3)
{
    while (*__s != '\0' && *__s != __accept1 && *__s != __accept2 && *__s != __accept3)
        ++__s;
    return *__s == '\0' ? ((void *)0) : (char *) (size_t) __s;
}

extern __inline __attribute__ ((__gnu_inline__)) char *__strtok_r_1c (char *__s, char __sep, char **__nextp);
extern __inline __attribute__ ((__gnu_inline__)) char * __strtok_r_1c (char *__s, char __sep, char **__nextp)
{
    char *__result;
    if (__s == ((void *)0))
        __s = *__nextp;
    while (*__s == __sep)
        ++__s;
    __result = ((void *)0);
    if (*__s != '\0')
    {
        __result = __s++;
        while (*__s != '\0')
            if (*__s++ == __sep)
            {
                __s[-1] = '\0';
                break;
            }
    }
    *__nextp = __s;
    return __result;
}

extern char *__strsep_g (char **__stringp, __const char *__delim);

extern __inline __attribute__ ((__gnu_inline__)) char *__strsep_1c (char **__s, char __reject);
extern __inline __attribute__ ((__gnu_inline__)) char * __strsep_1c (char **__s, char __reject)
{
    register char *__retval = *__s;
    if (__retval != ((void *)0) && (*__s = (__extension__ (__builtin_constant_p (__reject) && !__builtin_constant_p (__retval) && (__reject) == '\0' ? (char *) __rawmemchr (__retval, __reject) : __builtin_strchr (__retval, __reject)))) != ((void *)0))
        *(*__s)++ = '\0';
    return __retval;
}

extern __inline __attribute__ ((__gnu_inline__)) char *__strsep_2c (char **__s, char __reject1, char __reject2);
extern __inline __attribute__ ((__gnu_inline__)) char * __strsep_2c (char **__s, char __reject1, char __reject2)
{
    register char *__retval = *__s;
    if (__retval != ((void *)0))
    {
        register char *__cp = __retval;
        while (1)
        {
            if (*__cp == '\0')
            {
                __cp = ((void *)0);
                break;
            }
            if (*__cp == __reject1 || *__cp == __reject2)
            {
                *__cp++ = '\0';
                break;
            }
            ++__cp;
        }
        *__s = __cp;
    }
    return __retval;
}

extern __inline __attribute__ ((__gnu_inline__)) char *__strsep_3c (char **__s, char __reject1, char __reject2, char __reject3);
extern __inline __attribute__ ((__gnu_inline__)) char * __strsep_3c (char **__s, char __reject1, char __reject2, char __reject3)
{
    register char *__retval = *__s;
    if (__retval != ((void *)0))
    {
        register char *__cp = __retval;
        while (1)
        {
            if (*__cp == '\0')
            {
                __cp = ((void *)0);
                break;
            }
            if (*__cp == __reject1 || *__cp == __reject2 || *__cp == __reject3)
            {
                *__cp++ = '\0';
                break;
            }
            ++__cp;
        }
        *__s = __cp;
    }
    return __retval;
}

extern char *__strdup (__const char *__string) __attribute__ ((__nothrow__)) __attribute__ ((__malloc__));

extern char *__strndup (__const char *__string, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__malloc__));

extern void __warn_memset_zero_len (void) __attribute__((__warning__ ("memset used with constant zero length parameter; this could be due to transposed parameters")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) void * __attribute__ ((__nothrow__)) memcpy (void *__restrict __dest, __const void *__restrict __src, size_t __len)
{
    return __builtin___memcpy_chk (__dest, __src, __len, __builtin_object_size (__dest, 0));
}

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) void * __attribute__ ((__nothrow__)) memmove (void *__dest, __const void *__src, size_t __len)
{
    return __builtin___memmove_chk (__dest, __src, __len, __builtin_object_size (__dest, 0));
}

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) void * __attribute__ ((__nothrow__)) memset (void *__dest, int __ch, size_t __len)
{
    if (__builtin_constant_p (__len) && __len == 0 && (!__builtin_constant_p (__ch) || __ch != 0))
    {
        __warn_memset_zero_len ();
        return __dest;
    }
    return __builtin___memset_chk (__dest, __ch, __len, __builtin_object_size (__dest, 0));
}

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) void __attribute__ ((__nothrow__)) bcopy (__const void *__src, void *__dest, size_t __len)
{
    (void) __builtin___memmove_chk (__dest, __src, __len, __builtin_object_size (__dest, 0));
}

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) void __attribute__ ((__nothrow__)) bzero (void *__dest, size_t __len)
{
    (void) __builtin___memset_chk (__dest, '\0', __len, __builtin_object_size (__dest, 0));
}

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) char * __attribute__ ((__nothrow__)) strcpy (char *__restrict __dest, __const char *__restrict __src)
{
    return __builtin___strcpy_chk (__dest, __src, __builtin_object_size (__dest, 1 > 1));
}

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) char * __attribute__ ((__nothrow__)) strncpy (char *__restrict __dest, __const char *__restrict __src, size_t __len)
{
    return __builtin___strncpy_chk (__dest, __src, __len, __builtin_object_size (__dest, 1 > 1));
}

extern char *__stpncpy_chk (char *__dest, __const char *__src, size_t __n, size_t __destlen) __attribute__ ((__nothrow__));
extern char *__stpncpy_alias (char *__dest, __const char *__src, size_t __n) __asm__ ("" "stpncpy") __attribute__ ((__nothrow__));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) char * __attribute__ ((__nothrow__)) stpncpy (char *__dest, __const char *__src, size_t __n)
{
    if (__builtin_object_size (__dest, 1 > 1) != (size_t) -1 && (!__builtin_constant_p (__n) || __n <= __builtin_object_size (__dest, 1 > 1)))
        return __stpncpy_chk (__dest, __src, __n, __builtin_object_size (__dest, 1 > 1));
    return __stpncpy_alias (__dest, __src, __n);
}

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) char * __attribute__ ((__nothrow__)) strcat (char *__restrict __dest, __const char *__restrict __src)
{
    return __builtin___strcat_chk (__dest, __src, __builtin_object_size (__dest, 1 > 1));
}

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) char * __attribute__ ((__nothrow__)) strncat (char *__restrict __dest, __const char *__restrict __src, size_t __len)
{
    return __builtin___strncat_chk (__dest, __src, __len, __builtin_object_size (__dest, 1 > 1));
}

struct stat
{
    __dev_t st_dev;
    __ino_t st_ino;
    __nlink_t st_nlink;
    __mode_t st_mode;
    __uid_t st_uid;
    __gid_t st_gid;

    int __pad0;

    __dev_t st_rdev;
    __off_t st_size;
    __blksize_t st_blksize;
    __blkcnt_t st_blocks;

    struct timespec st_atim;
    struct timespec st_mtim;
    struct timespec st_ctim;

    long int __unused[3];
};

extern int stat (__const char *__restrict __file, struct stat *__restrict __buf) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int fstat (int __fd, struct stat *__buf) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern int fstatat (int __fd, __const char *__restrict __file, struct stat *__restrict __buf, int __flag) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 3)));

extern int lstat (__const char *__restrict __file, struct stat *__restrict __buf) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1, 2)));

extern int chmod (__const char *__file, __mode_t __mode) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int lchmod (__const char *__file, __mode_t __mode) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int fchmod (int __fd, __mode_t __mode) __attribute__ ((__nothrow__));

extern int fchmodat (int __fd, __const char *__file, __mode_t __mode, int __flag) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2))) __attribute__ ((__warn_unused_result__));

extern __mode_t umask (__mode_t __mask) __attribute__ ((__nothrow__));

extern int mkdir (__const char *__path, __mode_t __mode) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int mkdirat (int __fd, __const char *__path, __mode_t __mode) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern int mknod (__const char *__path, __mode_t __mode, __dev_t __dev) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int mknodat (int __fd, __const char *__path, __mode_t __mode, __dev_t __dev) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern int mkfifo (__const char *__path, __mode_t __mode) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int mkfifoat (int __fd, __const char *__path, __mode_t __mode) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern int utimensat (int __fd, __const char *__path, __const struct timespec __times[2], int __flags) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2)));

extern int futimens (int __fd, __const struct timespec __times[2]) __attribute__ ((__nothrow__));

extern int __fxstat (int __ver, int __fildes, struct stat *__stat_buf) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3)));
extern int __xstat (int __ver, __const char *__filename, struct stat *__stat_buf) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 3)));
extern int __lxstat (int __ver, __const char *__filename, struct stat *__stat_buf) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 3)));
extern int __fxstatat (int __ver, int __fildes, __const char *__filename, struct stat *__stat_buf, int __flag) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 4)));

extern int __xmknod (int __ver, __const char *__path, __mode_t __mode, __dev_t *__dev) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (2, 4)));

extern int __xmknodat (int __ver, int __fd, __const char *__path, __mode_t __mode, __dev_t *__dev) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (3, 5)));

extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) stat (__const char *__path, struct stat *__statbuf)
{
    return __xstat (1, __path, __statbuf);
}

extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) lstat (__const char *__path, struct stat *__statbuf)
{
    return __lxstat (1, __path, __statbuf);
}

extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) fstat (int __fd, struct stat *__statbuf)
{
    return __fxstat (1, __fd, __statbuf);
}

extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) fstatat (int __fd, __const char *__filename, struct stat *__statbuf, int __flag)
{
    return __fxstatat (1, __fd, __filename, __statbuf, __flag);
}

extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) mknod (__const char *__path, __mode_t __mode, __dev_t __dev)
{
    return __xmknod (0, __path, __mode, &__dev);
}

extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) mknodat (int __fd, __const char *__path, __mode_t __mode, __dev_t __dev)
{
    return __xmknodat (0, __fd, __path, __mode, &__dev);
}

struct timezone
{
    int tz_minuteswest;
    int tz_dsttime;
};

typedef struct timezone *__restrict __timezone_ptr_t;

extern int gettimeofday (struct timeval *__restrict __tv, __timezone_ptr_t __tz) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int settimeofday (__const struct timeval *__tv, __const struct timezone *__tz) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int adjtime (__const struct timeval *__delta, struct timeval *__olddelta) __attribute__ ((__nothrow__));

enum __itimer_which
{
    ITIMER_REAL = 0,
    ITIMER_VIRTUAL = 1,
    ITIMER_PROF = 2
};

struct itimerval
{
    struct timeval it_interval;
    struct timeval it_value;
};

typedef int __itimer_which_t;

extern int getitimer (__itimer_which_t __which, struct itimerval *__value) __attribute__ ((__nothrow__));

extern int setitimer (__itimer_which_t __which, __const struct itimerval *__restrict __new, struct itimerval *__restrict __old) __attribute__ ((__nothrow__));

extern int utimes (__const char *__file, __const struct timeval __tvp[2]) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int lutimes (__const char *__file, __const struct timeval __tvp[2]) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

extern int futimes (int __fd, __const struct timeval __tvp[2]) __attribute__ ((__nothrow__));

extern int *__errno_location (void) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

typedef unsigned char uint8_t;
typedef unsigned short int uint16_t;

typedef unsigned int uint32_t;

typedef unsigned long int uint64_t;

typedef signed char int_least8_t;
typedef short int int_least16_t;
typedef int int_least32_t;

typedef long int int_least64_t;

typedef unsigned char uint_least8_t;
typedef unsigned short int uint_least16_t;
typedef unsigned int uint_least32_t;

typedef unsigned long int uint_least64_t;

typedef signed char int_fast8_t;

typedef long int int_fast16_t;
typedef long int int_fast32_t;
typedef long int int_fast64_t;

typedef unsigned char uint_fast8_t;

typedef unsigned long int uint_fast16_t;
typedef unsigned long int uint_fast32_t;
typedef unsigned long int uint_fast64_t;

typedef unsigned long int uintptr_t;

typedef long int intmax_t;
typedef unsigned long int uintmax_t;

typedef int __gwchar_t;

typedef struct
{
    long int quot;
    long int rem;
} imaxdiv_t;

extern intmax_t imaxabs (intmax_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern imaxdiv_t imaxdiv (intmax_t __numer, intmax_t __denom) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern intmax_t strtoimax (__const char *__restrict __nptr, char **__restrict __endptr, int __base) __attribute__ ((__nothrow__));

extern uintmax_t strtoumax (__const char *__restrict __nptr, char ** __restrict __endptr, int __base) __attribute__ ((__nothrow__));

extern intmax_t wcstoimax (__const __gwchar_t *__restrict __nptr, __gwchar_t **__restrict __endptr, int __base) __attribute__ ((__nothrow__));

extern uintmax_t wcstoumax (__const __gwchar_t *__restrict __nptr, __gwchar_t ** __restrict __endptr, int __base) __attribute__ ((__nothrow__));

extern long int __strtol_internal (__const char *__restrict __nptr, char **__restrict __endptr, int __base, int __group) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern __inline __attribute__ ((__gnu_inline__)) intmax_t __attribute__ ((__nothrow__)) strtoimax (__const char *__restrict nptr, char **__restrict endptr, int base)
{
    return __strtol_internal (nptr, endptr, base, 0);
}

extern unsigned long int __strtoul_internal (__const char * __restrict __nptr, char ** __restrict __endptr, int __base, int __group) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern __inline __attribute__ ((__gnu_inline__)) uintmax_t __attribute__ ((__nothrow__)) strtoumax (__const char *__restrict nptr, char **__restrict endptr, int base)
{
    return __strtoul_internal (nptr, endptr, base, 0);
}

extern long int __wcstol_internal (__const __gwchar_t * __restrict __nptr, __gwchar_t **__restrict __endptr, int __base, int __group) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern __inline __attribute__ ((__gnu_inline__)) intmax_t __attribute__ ((__nothrow__)) wcstoimax (__const __gwchar_t *__restrict nptr, __gwchar_t **__restrict endptr, int base)
{
    return __wcstol_internal (nptr, endptr, base, 0);
}

extern unsigned long int __wcstoul_internal (__const __gwchar_t * __restrict __nptr, __gwchar_t ** __restrict __endptr, int __base, int __group) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1))) __attribute__ ((__warn_unused_result__));

extern __inline __attribute__ ((__gnu_inline__)) uintmax_t __attribute__ ((__nothrow__)) wcstoumax (__const __gwchar_t *__restrict nptr, __gwchar_t **__restrict endptr, int base)
{
    return __wcstoul_internal (nptr, endptr, base, 0);
}

typedef unsigned int wint_t;

typedef __mbstate_t mbstate_t;

struct tm;

extern wchar_t *wcscpy (wchar_t *__restrict __dest, __const wchar_t *__restrict __src) __attribute__ ((__nothrow__));

extern wchar_t *wcsncpy (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __n) __attribute__ ((__nothrow__));

extern wchar_t *wcscat (wchar_t *__restrict __dest, __const wchar_t *__restrict __src) __attribute__ ((__nothrow__));

extern wchar_t *wcsncat (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __n) __attribute__ ((__nothrow__));

extern int wcscmp (__const wchar_t *__s1, __const wchar_t *__s2) __attribute__ ((__nothrow__)) __attribute__ ((__pure__));

extern int wcsncmp (__const wchar_t *__s1, __const wchar_t *__s2, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__pure__));

extern int wcscasecmp (__const wchar_t *__s1, __const wchar_t *__s2) __attribute__ ((__nothrow__));

extern int wcsncasecmp (__const wchar_t *__s1, __const wchar_t *__s2, size_t __n) __attribute__ ((__nothrow__));

extern int wcscasecmp_l (__const wchar_t *__s1, __const wchar_t *__s2, __locale_t __loc) __attribute__ ((__nothrow__));

extern int wcsncasecmp_l (__const wchar_t *__s1, __const wchar_t *__s2, size_t __n, __locale_t __loc) __attribute__ ((__nothrow__));

extern int wcscoll (__const wchar_t *__s1, __const wchar_t *__s2) __attribute__ ((__nothrow__));

extern size_t wcsxfrm (wchar_t *__restrict __s1, __const wchar_t *__restrict __s2, size_t __n) __attribute__ ((__nothrow__));

extern int wcscoll_l (__const wchar_t *__s1, __const wchar_t *__s2, __locale_t __loc) __attribute__ ((__nothrow__));

extern size_t wcsxfrm_l (wchar_t *__s1, __const wchar_t *__s2, size_t __n, __locale_t __loc) __attribute__ ((__nothrow__));

extern wchar_t *wcsdup (__const wchar_t *__s) __attribute__ ((__nothrow__)) __attribute__ ((__malloc__));

extern wchar_t *wcschr (__const wchar_t *__wcs, wchar_t __wc) __attribute__ ((__nothrow__)) __attribute__ ((__pure__));

extern wchar_t *wcsrchr (__const wchar_t *__wcs, wchar_t __wc) __attribute__ ((__nothrow__)) __attribute__ ((__pure__));

extern size_t wcscspn (__const wchar_t *__wcs, __const wchar_t *__reject) __attribute__ ((__nothrow__)) __attribute__ ((__pure__));

extern size_t wcsspn (__const wchar_t *__wcs, __const wchar_t *__accept) __attribute__ ((__nothrow__)) __attribute__ ((__pure__));

extern wchar_t *wcspbrk (__const wchar_t *__wcs, __const wchar_t *__accept) __attribute__ ((__nothrow__)) __attribute__ ((__pure__));

extern wchar_t *wcsstr (__const wchar_t *__haystack, __const wchar_t *__needle) __attribute__ ((__nothrow__)) __attribute__ ((__pure__));

extern wchar_t *wcstok (wchar_t *__restrict __s, __const wchar_t *__restrict __delim, wchar_t **__restrict __ptr) __attribute__ ((__nothrow__));

extern size_t wcslen (__const wchar_t *__s) __attribute__ ((__nothrow__)) __attribute__ ((__pure__));

extern size_t wcsnlen (__const wchar_t *__s, size_t __maxlen) __attribute__ ((__nothrow__)) __attribute__ ((__pure__));

extern wchar_t *wmemchr (__const wchar_t *__s, wchar_t __c, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__pure__));

extern int wmemcmp (__const wchar_t *__restrict __s1, __const wchar_t *__restrict __s2, size_t __n) __attribute__ ((__nothrow__)) __attribute__ ((__pure__));

extern wchar_t *wmemcpy (wchar_t *__restrict __s1, __const wchar_t *__restrict __s2, size_t __n) __attribute__ ((__nothrow__));

extern wchar_t *wmemmove (wchar_t *__s1, __const wchar_t *__s2, size_t __n) __attribute__ ((__nothrow__));

extern wchar_t *wmemset (wchar_t *__s, wchar_t __c, size_t __n) __attribute__ ((__nothrow__));

extern wint_t btowc (int __c) __attribute__ ((__nothrow__));

extern int wctob (wint_t __c) __attribute__ ((__nothrow__));

extern int mbsinit (__const mbstate_t *__ps) __attribute__ ((__nothrow__)) __attribute__ ((__pure__));

extern size_t mbrtowc (wchar_t *__restrict __pwc, __const char *__restrict __s, size_t __n, mbstate_t *__p) __attribute__ ((__nothrow__));

extern size_t wcrtomb (char *__restrict __s, wchar_t __wc, mbstate_t *__restrict __ps) __attribute__ ((__nothrow__));

extern size_t __mbrlen (__const char *__restrict __s, size_t __n, mbstate_t *__restrict __ps) __attribute__ ((__nothrow__));
extern size_t mbrlen (__const char *__restrict __s, size_t __n, mbstate_t *__restrict __ps) __attribute__ ((__nothrow__));

extern wint_t __btowc_alias (int __c) __asm ("btowc");
extern __inline __attribute__ ((__gnu_inline__)) wint_t __attribute__ ((__nothrow__)) btowc (int __c)
{
    return (__builtin_constant_p (__c) && __c >= '\0' && __c <= '\x7f' ? (wint_t) __c : __btowc_alias (__c));
}

extern int __wctob_alias (wint_t __c) __asm ("wctob");
extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) wctob (wint_t __wc)
{
    return (__builtin_constant_p (__wc) && __wc >= L'\0' && __wc <= L'\x7f' ? (int) __wc : __wctob_alias (__wc));
}

extern __inline __attribute__ ((__gnu_inline__)) size_t __attribute__ ((__nothrow__)) mbrlen (__const char *__restrict __s, size_t __n, mbstate_t *__restrict __ps)
{
    return (__ps != ((void *)0) ? mbrtowc (((void *)0), __s, __n, __ps) : __mbrlen (__s, __n, ((void *)0)));
}

extern size_t mbsrtowcs (wchar_t *__restrict __dst, __const char **__restrict __src, size_t __len, mbstate_t *__restrict __ps) __attribute__ ((__nothrow__));
extern size_t wcsrtombs (char *__restrict __dst, __const wchar_t **__restrict __src, size_t __len, mbstate_t *__restrict __ps) __attribute__ ((__nothrow__));

extern size_t mbsnrtowcs (wchar_t *__restrict __dst, __const char **__restrict __src, size_t __nmc, size_t __len, mbstate_t *__restrict __ps) __attribute__ ((__nothrow__));
extern size_t wcsnrtombs (char *__restrict __dst, __const wchar_t **__restrict __src, size_t __nwc, size_t __len, mbstate_t *__restrict __ps) __attribute__ ((__nothrow__));

extern double wcstod (__const wchar_t *__restrict __nptr, wchar_t **__restrict __endptr) __attribute__ ((__nothrow__));

extern float wcstof (__const wchar_t *__restrict __nptr, wchar_t **__restrict __endptr) __attribute__ ((__nothrow__));
extern long double wcstold (__const wchar_t *__restrict __nptr, wchar_t **__restrict __endptr) __attribute__ ((__nothrow__));

extern long int wcstol (__const wchar_t *__restrict __nptr, wchar_t **__restrict __endptr, int __base) __attribute__ ((__nothrow__));

extern unsigned long int wcstoul (__const wchar_t *__restrict __nptr, wchar_t **__restrict __endptr, int __base) __attribute__ ((__nothrow__));

__extension__ extern long long int wcstoll (__const wchar_t *__restrict __nptr, wchar_t **__restrict __endptr, int __base) __attribute__ ((__nothrow__));

__extension__ extern unsigned long long int wcstoull (__const wchar_t *__restrict __nptr, wchar_t **__restrict __endptr, int __base) __attribute__ ((__nothrow__));

extern __FILE *open_wmemstream (wchar_t **__bufloc, size_t *__sizeloc) __attribute__ ((__nothrow__));

extern int fwide (__FILE *__fp, int __mode) __attribute__ ((__nothrow__));

extern int fwprintf (__FILE *__restrict __stream, __const wchar_t *__restrict __format, ...);

extern int wprintf (__const wchar_t *__restrict __format, ...);

extern int swprintf (wchar_t *__restrict __s, size_t __n, __const wchar_t *__restrict __format, ...) __attribute__ ((__nothrow__));

extern int vfwprintf (__FILE *__restrict __s, __const wchar_t *__restrict __format, __gnuc_va_list __arg);

extern int vwprintf (__const wchar_t *__restrict __format, __gnuc_va_list __arg);

extern int vswprintf (wchar_t *__restrict __s, size_t __n, __const wchar_t *__restrict __format, __gnuc_va_list __arg) __attribute__ ((__nothrow__));

extern int fwscanf (__FILE *__restrict __stream, __const wchar_t *__restrict __format, ...);

extern int wscanf (__const wchar_t *__restrict __format, ...);

extern int swscanf (__const wchar_t *__restrict __s, __const wchar_t *__restrict __format, ...) __attribute__ ((__nothrow__));

extern int fwscanf (__FILE *__restrict __stream, __const wchar_t *__restrict __format, ...) __asm__ ("" "__isoc99_fwscanf");
extern int wscanf (__const wchar_t *__restrict __format, ...) __asm__ ("" "__isoc99_wscanf");
extern int swscanf (__const wchar_t *__restrict __s, __const wchar_t *__restrict __format, ...) __asm__ ("" "__isoc99_swscanf") __attribute__ ((__nothrow__));

extern int vfwscanf (__FILE *__restrict __s, __const wchar_t *__restrict __format, __gnuc_va_list __arg);

extern int vwscanf (__const wchar_t *__restrict __format, __gnuc_va_list __arg);

extern int vswscanf (__const wchar_t *__restrict __s, __const wchar_t *__restrict __format, __gnuc_va_list __arg) __attribute__ ((__nothrow__));

extern int vfwscanf (__FILE *__restrict __s, __const wchar_t *__restrict __format, __gnuc_va_list __arg) __asm__ ("" "__isoc99_vfwscanf");
extern int vwscanf (__const wchar_t *__restrict __format, __gnuc_va_list __arg) __asm__ ("" "__isoc99_vwscanf");
extern int vswscanf (__const wchar_t *__restrict __s, __const wchar_t *__restrict __format, __gnuc_va_list __arg) __asm__ ("" "__isoc99_vswscanf") __attribute__ ((__nothrow__));

extern wint_t fgetwc (__FILE *__stream);
extern wint_t getwc (__FILE *__stream);

extern wint_t getwchar (void);

extern wint_t fputwc (wchar_t __wc, __FILE *__stream);
extern wint_t putwc (wchar_t __wc, __FILE *__stream);

extern wint_t putwchar (wchar_t __wc);

extern wchar_t *fgetws (wchar_t *__restrict __ws, int __n, __FILE *__restrict __stream);

extern int fputws (__const wchar_t *__restrict __ws, __FILE *__restrict __stream);

extern wint_t ungetwc (wint_t __wc, __FILE *__stream);

extern size_t wcsftime (wchar_t *__restrict __s, size_t __maxsize, __const wchar_t *__restrict __format, __const struct tm *__restrict __tp) __attribute__ ((__nothrow__));

extern wchar_t *__wmemcpy_chk (wchar_t *__restrict __s1, __const wchar_t *__restrict __s2, size_t __n, size_t __ns1) __attribute__ ((__nothrow__));
extern wchar_t *__wmemcpy_alias (wchar_t *__restrict __s1, __const wchar_t *__restrict __s2, size_t __n) __asm__ ("" "wmemcpy") __attribute__ ((__nothrow__));
extern wchar_t *__wmemcpy_chk_warn (wchar_t *__restrict __s1, __const wchar_t *__restrict __s2, size_t __n, size_t __ns1) __asm__ ("" "__wmemcpy_chk") __attribute__ ((__nothrow__)) __attribute__((__warning__ ("wmemcpy called with length bigger than size of destination " "buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) wchar_t * __attribute__ ((__nothrow__)) wmemcpy (wchar_t *__restrict __s1, __const wchar_t *__restrict __s2, size_t __n)
{
    if (__builtin_object_size (__s1, 0) != (size_t) -1)
    {
        if (!__builtin_constant_p (__n))
            return __wmemcpy_chk (__s1, __s2, __n, __builtin_object_size (__s1, 0) / sizeof (wchar_t));

        if (__n > __builtin_object_size (__s1, 0) / sizeof (wchar_t))
            return __wmemcpy_chk_warn (__s1, __s2, __n, __builtin_object_size (__s1, 0) / sizeof (wchar_t));
    }
    return __wmemcpy_alias (__s1, __s2, __n);
}

extern wchar_t *__wmemmove_chk (wchar_t *__s1, __const wchar_t *__s2, size_t __n, size_t __ns1) __attribute__ ((__nothrow__));
extern wchar_t *__wmemmove_alias (wchar_t *__s1, __const wchar_t *__s2, size_t __n) __asm__ ("" "wmemmove") __attribute__ ((__nothrow__));
extern wchar_t *__wmemmove_chk_warn (wchar_t *__s1, __const wchar_t *__s2, size_t __n, size_t __ns1) __asm__ ("" "__wmemmove_chk") __attribute__ ((__nothrow__)) __attribute__((__warning__ ("wmemmove called with length bigger than size of destination " "buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) wchar_t * __attribute__ ((__nothrow__)) wmemmove (wchar_t *__s1, __const wchar_t *__s2, size_t __n)
{
    if (__builtin_object_size (__s1, 0) != (size_t) -1)
    {
        if (!__builtin_constant_p (__n))
            return __wmemmove_chk (__s1, __s2, __n, __builtin_object_size (__s1, 0) / sizeof (wchar_t));

        if (__n > __builtin_object_size (__s1, 0) / sizeof (wchar_t))
            return __wmemmove_chk_warn (__s1, __s2, __n, __builtin_object_size (__s1, 0) / sizeof (wchar_t));
    }
    return __wmemmove_alias (__s1, __s2, __n);
}

extern wchar_t *__wmemset_chk (wchar_t *__s, wchar_t __c, size_t __n, size_t __ns) __attribute__ ((__nothrow__));
extern wchar_t *__wmemset_alias (wchar_t *__s, wchar_t __c, size_t __n) __asm__ ("" "wmemset") __attribute__ ((__nothrow__));
extern wchar_t *__wmemset_chk_warn (wchar_t *__s, wchar_t __c, size_t __n, size_t __ns) __asm__ ("" "__wmemset_chk") __attribute__ ((__nothrow__)) __attribute__((__warning__ ("wmemset called with length bigger than size of destination " "buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) wchar_t * __attribute__ ((__nothrow__)) wmemset (wchar_t *__s, wchar_t __c, size_t __n)
{
    if (__builtin_object_size (__s, 0) != (size_t) -1)
    {
        if (!__builtin_constant_p (__n))
            return __wmemset_chk (__s, __c, __n, __builtin_object_size (__s, 0) / sizeof (wchar_t));

        if (__n > __builtin_object_size (__s, 0) / sizeof (wchar_t))
            return __wmemset_chk_warn (__s, __c, __n, __builtin_object_size (__s, 0) / sizeof (wchar_t));
    }
    return __wmemset_alias (__s, __c, __n);
}

extern wchar_t *__wcscpy_chk (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __n) __attribute__ ((__nothrow__));
extern wchar_t *__wcscpy_alias (wchar_t *__restrict __dest, __const wchar_t *__restrict __src) __asm__ ("" "wcscpy") __attribute__ ((__nothrow__));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) wchar_t * __attribute__ ((__nothrow__)) wcscpy (wchar_t *__restrict __dest, __const wchar_t *__restrict __src)
{
    if (__builtin_object_size (__dest, 1 > 1) != (size_t) -1)
        return __wcscpy_chk (__dest, __src, __builtin_object_size (__dest, 1 > 1) / sizeof (wchar_t));
    return __wcscpy_alias (__dest, __src);
}

extern wchar_t *__wcpcpy_chk (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __destlen) __attribute__ ((__nothrow__));
extern wchar_t *__wcpcpy_alias (wchar_t *__restrict __dest, __const wchar_t *__restrict __src) __asm__ ("" "wcpcpy") __attribute__ ((__nothrow__));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) wchar_t * __attribute__ ((__nothrow__)) wcpcpy (wchar_t *__restrict __dest, __const wchar_t *__restrict __src)
{
    if (__builtin_object_size (__dest, 1 > 1) != (size_t) -1)
        return __wcpcpy_chk (__dest, __src, __builtin_object_size (__dest, 1 > 1) / sizeof (wchar_t));
    return __wcpcpy_alias (__dest, __src);
}

extern wchar_t *__wcsncpy_chk (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __n, size_t __destlen) __attribute__ ((__nothrow__));
extern wchar_t *__wcsncpy_alias (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __n) __asm__ ("" "wcsncpy") __attribute__ ((__nothrow__));
extern wchar_t *__wcsncpy_chk_warn (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __n, size_t __destlen) __asm__ ("" "__wcsncpy_chk") __attribute__ ((__nothrow__)) __attribute__((__warning__ ("wcsncpy called with length bigger than size of destination " "buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) wchar_t * __attribute__ ((__nothrow__)) wcsncpy (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __n)
{
    if (__builtin_object_size (__dest, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__n))
            return __wcsncpy_chk (__dest, __src, __n, __builtin_object_size (__dest, 1 > 1) / sizeof (wchar_t));
        if (__n > __builtin_object_size (__dest, 1 > 1) / sizeof (wchar_t))
            return __wcsncpy_chk_warn (__dest, __src, __n, __builtin_object_size (__dest, 1 > 1) / sizeof (wchar_t));
    }
    return __wcsncpy_alias (__dest, __src, __n);
}

extern wchar_t *__wcpncpy_chk (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __n, size_t __destlen) __attribute__ ((__nothrow__));
extern wchar_t *__wcpncpy_alias (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __n) __asm__ ("" "wcpncpy") __attribute__ ((__nothrow__));
extern wchar_t *__wcpncpy_chk_warn (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __n, size_t __destlen) __asm__ ("" "__wcpncpy_chk") __attribute__ ((__nothrow__)) __attribute__((__warning__ ("wcpncpy called with length bigger than size of destination " "buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) wchar_t * __attribute__ ((__nothrow__)) wcpncpy (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __n)
{
    if (__builtin_object_size (__dest, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__n))
            return __wcpncpy_chk (__dest, __src, __n, __builtin_object_size (__dest, 1 > 1) / sizeof (wchar_t));
        if (__n > __builtin_object_size (__dest, 1 > 1) / sizeof (wchar_t))
            return __wcpncpy_chk_warn (__dest, __src, __n, __builtin_object_size (__dest, 1 > 1) / sizeof (wchar_t));
    }
    return __wcpncpy_alias (__dest, __src, __n);
}

extern wchar_t *__wcscat_chk (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __destlen) __attribute__ ((__nothrow__));
extern wchar_t *__wcscat_alias (wchar_t *__restrict __dest, __const wchar_t *__restrict __src) __asm__ ("" "wcscat") __attribute__ ((__nothrow__));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) wchar_t * __attribute__ ((__nothrow__)) wcscat (wchar_t *__restrict __dest, __const wchar_t *__restrict __src)
{
    if (__builtin_object_size (__dest, 1 > 1) != (size_t) -1)
        return __wcscat_chk (__dest, __src, __builtin_object_size (__dest, 1 > 1) / sizeof (wchar_t));
    return __wcscat_alias (__dest, __src);
}

extern wchar_t *__wcsncat_chk (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __n, size_t __destlen) __attribute__ ((__nothrow__));
extern wchar_t *__wcsncat_alias (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __n) __asm__ ("" "wcsncat") __attribute__ ((__nothrow__));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) wchar_t * __attribute__ ((__nothrow__)) wcsncat (wchar_t *__restrict __dest, __const wchar_t *__restrict __src, size_t __n)
{
    if (__builtin_object_size (__dest, 1 > 1) != (size_t) -1)
        return __wcsncat_chk (__dest, __src, __n, __builtin_object_size (__dest, 1 > 1) / sizeof (wchar_t));
    return __wcsncat_alias (__dest, __src, __n);
}

extern int __swprintf_chk (wchar_t *__restrict __s, size_t __n, int __flag, size_t __s_len, __const wchar_t *__restrict __format, ...) __attribute__ ((__nothrow__));

extern int __swprintf_alias (wchar_t *__restrict __s, size_t __n, __const wchar_t *__restrict __fmt, ...) __asm__ ("" "swprintf") __attribute__ ((__nothrow__));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) int __attribute__ ((__nothrow__)) swprintf (wchar_t *__restrict __s, size_t __n, __const wchar_t *__restrict __fmt, ...)
{
    if (__builtin_object_size (__s, 1 > 1) != (size_t) -1 || 1 > 1)
        return __swprintf_chk (__s, __n, 1 - 1, __builtin_object_size (__s, 1 > 1) / sizeof (wchar_t), __fmt, __builtin_va_arg_pack ());
    return __swprintf_alias (__s, __n, __fmt, __builtin_va_arg_pack ());
}

extern int __vswprintf_chk (wchar_t *__restrict __s, size_t __n, int __flag, size_t __s_len, __const wchar_t *__restrict __format, __gnuc_va_list __arg) __attribute__ ((__nothrow__));

extern int __vswprintf_alias (wchar_t *__restrict __s, size_t __n, __const wchar_t *__restrict __fmt, __gnuc_va_list __ap) __asm__ ("" "vswprintf") __attribute__ ((__nothrow__));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) int __attribute__ ((__nothrow__)) vswprintf (wchar_t *__restrict __s, size_t __n, __const wchar_t *__restrict __fmt, __gnuc_va_list __ap)
{
    if (__builtin_object_size (__s, 1 > 1) != (size_t) -1 || 1 > 1)
        return __vswprintf_chk (__s, __n, 1 - 1, __builtin_object_size (__s, 1 > 1) / sizeof (wchar_t), __fmt, __ap);
    return __vswprintf_alias (__s, __n, __fmt, __ap);
}

extern wchar_t *__fgetws_chk (wchar_t *__restrict __s, size_t __size, int __n, __FILE *__restrict __stream) __attribute__ ((__warn_unused_result__));
extern wchar_t *__fgetws_alias (wchar_t *__restrict __s, int __n, __FILE *__restrict __stream) __asm__ ("" "fgetws") __attribute__ ((__warn_unused_result__));
extern wchar_t *__fgetws_chk_warn (wchar_t *__restrict __s, size_t __size, int __n, __FILE *__restrict __stream) __asm__ ("" "__fgetws_chk") __attribute__ ((__warn_unused_result__)) __attribute__((__warning__ ("fgetws called with bigger size than length " "of destination buffer")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) __attribute__ ((__warn_unused_result__)) wchar_t * fgetws (wchar_t *__restrict __s, int __n, __FILE *__restrict __stream)
{
    if (__builtin_object_size (__s, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__n) || __n <= 0)
            return __fgetws_chk (__s, __builtin_object_size (__s, 1 > 1) / sizeof (wchar_t), __n, __stream);

        if ((size_t) __n > __builtin_object_size (__s, 1 > 1) / sizeof (wchar_t))
            return __fgetws_chk_warn (__s, __builtin_object_size (__s, 1 > 1) / sizeof (wchar_t), __n, __stream);
    }
    return __fgetws_alias (__s, __n, __stream);
}

extern size_t __wcrtomb_chk (char *__restrict __s, wchar_t __wchar, mbstate_t *__restrict __p, size_t __buflen) __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));
extern size_t __wcrtomb_alias (char *__restrict __s, wchar_t __wchar, mbstate_t *__restrict __ps) __asm__ ("" "wcrtomb") __attribute__ ((__nothrow__)) __attribute__ ((__warn_unused_result__));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) __attribute__ ((__warn_unused_result__)) size_t __attribute__ ((__nothrow__)) wcrtomb (char *__restrict __s, wchar_t __wchar, mbstate_t *__restrict __ps)
{
    if (__builtin_object_size (__s, 1 > 1) != (size_t) -1 && 16 > __builtin_object_size (__s, 1 > 1))
        return __wcrtomb_chk (__s, __wchar, __ps, __builtin_object_size (__s, 1 > 1));
    return __wcrtomb_alias (__s, __wchar, __ps);
}

extern size_t __mbsrtowcs_chk (wchar_t *__restrict __dst, __const char **__restrict __src, size_t __len, mbstate_t *__restrict __ps, size_t __dstlen) __attribute__ ((__nothrow__));
extern size_t __mbsrtowcs_alias (wchar_t *__restrict __dst, __const char **__restrict __src, size_t __len, mbstate_t *__restrict __ps) __asm__ ("" "mbsrtowcs") __attribute__ ((__nothrow__));
extern size_t __mbsrtowcs_chk_warn (wchar_t *__restrict __dst, __const char **__restrict __src, size_t __len, mbstate_t *__restrict __ps, size_t __dstlen) __asm__ ("" "__mbsrtowcs_chk") __attribute__ ((__nothrow__)) __attribute__((__warning__ ("mbsrtowcs called with dst buffer smaller than len " "* sizeof (wchar_t)")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) size_t __attribute__ ((__nothrow__)) mbsrtowcs (wchar_t *__restrict __dst, __const char **__restrict __src, size_t __len, mbstate_t *__restrict __ps)
{
    if (__builtin_object_size (__dst, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__len))
            return __mbsrtowcs_chk (__dst, __src, __len, __ps, __builtin_object_size (__dst, 1 > 1) / sizeof (wchar_t));

        if (__len > __builtin_object_size (__dst, 1 > 1) / sizeof (wchar_t))
            return __mbsrtowcs_chk_warn (__dst, __src, __len, __ps, __builtin_object_size (__dst, 1 > 1) / sizeof (wchar_t));
    }
    return __mbsrtowcs_alias (__dst, __src, __len, __ps);
}

extern size_t __wcsrtombs_chk (char *__restrict __dst, __const wchar_t **__restrict __src, size_t __len, mbstate_t *__restrict __ps, size_t __dstlen) __attribute__ ((__nothrow__));
extern size_t __wcsrtombs_alias (char *__restrict __dst, __const wchar_t **__restrict __src, size_t __len, mbstate_t *__restrict __ps) __asm__ ("" "wcsrtombs") __attribute__ ((__nothrow__));
extern size_t __wcsrtombs_chk_warn (char *__restrict __dst, __const wchar_t **__restrict __src, size_t __len, mbstate_t *__restrict __ps, size_t __dstlen) __asm__ ("" "__wcsrtombs_chk") __attribute__ ((__nothrow__)) __attribute__((__warning__ ("wcsrtombs called with dst buffer smaller than len")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) size_t __attribute__ ((__nothrow__)) wcsrtombs (char *__restrict __dst, __const wchar_t **__restrict __src, size_t __len, mbstate_t *__restrict __ps)
{
    if (__builtin_object_size (__dst, 1 > 1) != (size_t) -1)
    {
        if (!__builtin_constant_p (__len))
            return __wcsrtombs_chk (__dst, __src, __len, __ps, __builtin_object_size (__dst, 1 > 1));

        if (__len > __builtin_object_size (__dst, 1 > 1))
            return __wcsrtombs_chk_warn (__dst, __src, __len, __ps, __builtin_object_size (__dst, 1 > 1));
    }
    return __wcsrtombs_alias (__dst, __src, __len, __ps);
}

typedef unsigned long int wctype_t;

enum
{
    __ISwupper = 0,
    __ISwlower = 1,
    __ISwalpha = 2,
    __ISwdigit = 3,
    __ISwxdigit = 4,
    __ISwspace = 5,
    __ISwprint = 6,
    __ISwgraph = 7,
    __ISwblank = 8,
    __ISwcntrl = 9,
    __ISwpunct = 10,
    __ISwalnum = 11,

    _ISwupper = ((__ISwupper) < 8 ? (int) ((1UL << (__ISwupper)) << 24) : ((__ISwupper) < 16 ? (int) ((1UL << (__ISwupper)) << 8) : ((__ISwupper) < 24 ? (int) ((1UL << (__ISwupper)) >> 8) : (int) ((1UL << (__ISwupper)) >> 24)))),
    _ISwlower = ((__ISwlower) < 8 ? (int) ((1UL << (__ISwlower)) << 24) : ((__ISwlower) < 16 ? (int) ((1UL << (__ISwlower)) << 8) : ((__ISwlower) < 24 ? (int) ((1UL << (__ISwlower)) >> 8) : (int) ((1UL << (__ISwlower)) >> 24)))),
    _ISwalpha = ((__ISwalpha) < 8 ? (int) ((1UL << (__ISwalpha)) << 24) : ((__ISwalpha) < 16 ? (int) ((1UL << (__ISwalpha)) << 8) : ((__ISwalpha) < 24 ? (int) ((1UL << (__ISwalpha)) >> 8) : (int) ((1UL << (__ISwalpha)) >> 24)))),
    _ISwdigit = ((__ISwdigit) < 8 ? (int) ((1UL << (__ISwdigit)) << 24) : ((__ISwdigit) < 16 ? (int) ((1UL << (__ISwdigit)) << 8) : ((__ISwdigit) < 24 ? (int) ((1UL << (__ISwdigit)) >> 8) : (int) ((1UL << (__ISwdigit)) >> 24)))),
    _ISwxdigit = ((__ISwxdigit) < 8 ? (int) ((1UL << (__ISwxdigit)) << 24) : ((__ISwxdigit) < 16 ? (int) ((1UL << (__ISwxdigit)) << 8) : ((__ISwxdigit) < 24 ? (int) ((1UL << (__ISwxdigit)) >> 8) : (int) ((1UL << (__ISwxdigit)) >> 24)))),
    _ISwspace = ((__ISwspace) < 8 ? (int) ((1UL << (__ISwspace)) << 24) : ((__ISwspace) < 16 ? (int) ((1UL << (__ISwspace)) << 8) : ((__ISwspace) < 24 ? (int) ((1UL << (__ISwspace)) >> 8) : (int) ((1UL << (__ISwspace)) >> 24)))),
    _ISwprint = ((__ISwprint) < 8 ? (int) ((1UL << (__ISwprint)) << 24) : ((__ISwprint) < 16 ? (int) ((1UL << (__ISwprint)) << 8) : ((__ISwprint) < 24 ? (int) ((1UL << (__ISwprint)) >> 8) : (int) ((1UL << (__ISwprint)) >> 24)))),
    _ISwgraph = ((__ISwgraph) < 8 ? (int) ((1UL << (__ISwgraph)) << 24) : ((__ISwgraph) < 16 ? (int) ((1UL << (__ISwgraph)) << 8) : ((__ISwgraph) < 24 ? (int) ((1UL << (__ISwgraph)) >> 8) : (int) ((1UL << (__ISwgraph)) >> 24)))),
    _ISwblank = ((__ISwblank) < 8 ? (int) ((1UL << (__ISwblank)) << 24) : ((__ISwblank) < 16 ? (int) ((1UL << (__ISwblank)) << 8) : ((__ISwblank) < 24 ? (int) ((1UL << (__ISwblank)) >> 8) : (int) ((1UL << (__ISwblank)) >> 24)))),
    _ISwcntrl = ((__ISwcntrl) < 8 ? (int) ((1UL << (__ISwcntrl)) << 24) : ((__ISwcntrl) < 16 ? (int) ((1UL << (__ISwcntrl)) << 8) : ((__ISwcntrl) < 24 ? (int) ((1UL << (__ISwcntrl)) >> 8) : (int) ((1UL << (__ISwcntrl)) >> 24)))),
    _ISwpunct = ((__ISwpunct) < 8 ? (int) ((1UL << (__ISwpunct)) << 24) : ((__ISwpunct) < 16 ? (int) ((1UL << (__ISwpunct)) << 8) : ((__ISwpunct) < 24 ? (int) ((1UL << (__ISwpunct)) >> 8) : (int) ((1UL << (__ISwpunct)) >> 24)))),
    _ISwalnum = ((__ISwalnum) < 8 ? (int) ((1UL << (__ISwalnum)) << 24) : ((__ISwalnum) < 16 ? (int) ((1UL << (__ISwalnum)) << 8) : ((__ISwalnum) < 24 ? (int) ((1UL << (__ISwalnum)) >> 8) : (int) ((1UL << (__ISwalnum)) >> 24))))
};

extern int iswalnum (wint_t __wc) __attribute__ ((__nothrow__));
extern int iswalpha (wint_t __wc) __attribute__ ((__nothrow__));
extern int iswcntrl (wint_t __wc) __attribute__ ((__nothrow__));
extern int iswdigit (wint_t __wc) __attribute__ ((__nothrow__));
extern int iswgraph (wint_t __wc) __attribute__ ((__nothrow__));
extern int iswlower (wint_t __wc) __attribute__ ((__nothrow__));
extern int iswprint (wint_t __wc) __attribute__ ((__nothrow__));
extern int iswpunct (wint_t __wc) __attribute__ ((__nothrow__));
extern int iswspace (wint_t __wc) __attribute__ ((__nothrow__));
extern int iswupper (wint_t __wc) __attribute__ ((__nothrow__));
extern int iswxdigit (wint_t __wc) __attribute__ ((__nothrow__));
extern int iswblank (wint_t __wc) __attribute__ ((__nothrow__));

extern wctype_t wctype (__const char *__property) __attribute__ ((__nothrow__));

extern int iswctype (wint_t __wc, wctype_t __desc) __attribute__ ((__nothrow__));

typedef __const __int32_t *wctrans_t;

extern wint_t towlower (wint_t __wc) __attribute__ ((__nothrow__));
extern wint_t towupper (wint_t __wc) __attribute__ ((__nothrow__));

extern wctrans_t wctrans (__const char *__property) __attribute__ ((__nothrow__));

extern wint_t towctrans (wint_t __wc, wctrans_t __desc) __attribute__ ((__nothrow__));

extern int iswalnum_l (wint_t __wc, __locale_t __locale) __attribute__ ((__nothrow__));
extern int iswalpha_l (wint_t __wc, __locale_t __locale) __attribute__ ((__nothrow__));
extern int iswcntrl_l (wint_t __wc, __locale_t __locale) __attribute__ ((__nothrow__));
extern int iswdigit_l (wint_t __wc, __locale_t __locale) __attribute__ ((__nothrow__));
extern int iswgraph_l (wint_t __wc, __locale_t __locale) __attribute__ ((__nothrow__));
extern int iswlower_l (wint_t __wc, __locale_t __locale) __attribute__ ((__nothrow__));
extern int iswprint_l (wint_t __wc, __locale_t __locale) __attribute__ ((__nothrow__));
extern int iswpunct_l (wint_t __wc, __locale_t __locale) __attribute__ ((__nothrow__));
extern int iswspace_l (wint_t __wc, __locale_t __locale) __attribute__ ((__nothrow__));
extern int iswupper_l (wint_t __wc, __locale_t __locale) __attribute__ ((__nothrow__));
extern int iswxdigit_l (wint_t __wc, __locale_t __locale) __attribute__ ((__nothrow__));
extern int iswblank_l (wint_t __wc, __locale_t __locale) __attribute__ ((__nothrow__));

extern wctype_t wctype_l (__const char *__property, __locale_t __locale) __attribute__ ((__nothrow__));

extern int iswctype_l (wint_t __wc, wctype_t __desc, __locale_t __locale) __attribute__ ((__nothrow__));

extern wint_t towlower_l (wint_t __wc, __locale_t __locale) __attribute__ ((__nothrow__));
extern wint_t towupper_l (wint_t __wc, __locale_t __locale) __attribute__ ((__nothrow__));

extern wctrans_t wctrans_l (__const char *__property, __locale_t __locale) __attribute__ ((__nothrow__));

extern wint_t towctrans_l (wint_t __wc, wctrans_t __desc, __locale_t __locale) __attribute__ ((__nothrow__));

typedef float float_t;
typedef double double_t;

extern double acos (double __x) __attribute__ ((__nothrow__)); extern double __acos (double __x) __attribute__ ((__nothrow__));
extern double asin (double __x) __attribute__ ((__nothrow__)); extern double __asin (double __x) __attribute__ ((__nothrow__));
extern double atan (double __x) __attribute__ ((__nothrow__)); extern double __atan (double __x) __attribute__ ((__nothrow__));
extern double atan2 (double __y, double __x) __attribute__ ((__nothrow__)); extern double __atan2 (double __y, double __x) __attribute__ ((__nothrow__));
extern double cos (double __x) __attribute__ ((__nothrow__)); extern double __cos (double __x) __attribute__ ((__nothrow__));
extern double sin (double __x) __attribute__ ((__nothrow__)); extern double __sin (double __x) __attribute__ ((__nothrow__));
extern double tan (double __x) __attribute__ ((__nothrow__)); extern double __tan (double __x) __attribute__ ((__nothrow__));
extern double cosh (double __x) __attribute__ ((__nothrow__)); extern double __cosh (double __x) __attribute__ ((__nothrow__));
extern double sinh (double __x) __attribute__ ((__nothrow__)); extern double __sinh (double __x) __attribute__ ((__nothrow__));
extern double tanh (double __x) __attribute__ ((__nothrow__)); extern double __tanh (double __x) __attribute__ ((__nothrow__));
extern double acosh (double __x) __attribute__ ((__nothrow__)); extern double __acosh (double __x) __attribute__ ((__nothrow__));
extern double asinh (double __x) __attribute__ ((__nothrow__)); extern double __asinh (double __x) __attribute__ ((__nothrow__));
extern double atanh (double __x) __attribute__ ((__nothrow__)); extern double __atanh (double __x) __attribute__ ((__nothrow__));
extern double exp (double __x) __attribute__ ((__nothrow__)); extern double __exp (double __x) __attribute__ ((__nothrow__));
extern double frexp (double __x, int *__exponent) __attribute__ ((__nothrow__)); extern double __frexp (double __x, int *__exponent) __attribute__ ((__nothrow__));
extern double ldexp (double __x, int __exponent) __attribute__ ((__nothrow__)); extern double __ldexp (double __x, int __exponent) __attribute__ ((__nothrow__));
extern double log (double __x) __attribute__ ((__nothrow__)); extern double __log (double __x) __attribute__ ((__nothrow__));
extern double log10 (double __x) __attribute__ ((__nothrow__)); extern double __log10 (double __x) __attribute__ ((__nothrow__));
extern double modf (double __x, double *__iptr) __attribute__ ((__nothrow__)); extern double __modf (double __x, double *__iptr) __attribute__ ((__nothrow__));
extern double expm1 (double __x) __attribute__ ((__nothrow__)); extern double __expm1 (double __x) __attribute__ ((__nothrow__));
extern double log1p (double __x) __attribute__ ((__nothrow__)); extern double __log1p (double __x) __attribute__ ((__nothrow__));
extern double logb (double __x) __attribute__ ((__nothrow__)); extern double __logb (double __x) __attribute__ ((__nothrow__));
extern double exp2 (double __x) __attribute__ ((__nothrow__)); extern double __exp2 (double __x) __attribute__ ((__nothrow__));
extern double log2 (double __x) __attribute__ ((__nothrow__)); extern double __log2 (double __x) __attribute__ ((__nothrow__));
extern double pow (double __x, double __y) __attribute__ ((__nothrow__)); extern double __pow (double __x, double __y) __attribute__ ((__nothrow__));
extern double sqrt (double __x) __attribute__ ((__nothrow__)); extern double __sqrt (double __x) __attribute__ ((__nothrow__));
extern double hypot (double __x, double __y) __attribute__ ((__nothrow__)); extern double __hypot (double __x, double __y) __attribute__ ((__nothrow__));
extern double cbrt (double __x) __attribute__ ((__nothrow__)); extern double __cbrt (double __x) __attribute__ ((__nothrow__));
extern double ceil (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __ceil (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern double fabs (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __fabs (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern double floor (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __floor (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern double fmod (double __x, double __y) __attribute__ ((__nothrow__)); extern double __fmod (double __x, double __y) __attribute__ ((__nothrow__));
extern int __isinf (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int __finite (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int isinf (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int finite (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern double drem (double __x, double __y) __attribute__ ((__nothrow__)); extern double __drem (double __x, double __y) __attribute__ ((__nothrow__));
extern double significand (double __x) __attribute__ ((__nothrow__)); extern double __significand (double __x) __attribute__ ((__nothrow__));
extern double copysign (double __x, double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __copysign (double __x, double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern double nan (__const char *__tagb) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __nan (__const char *__tagb) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int __isnan (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int isnan (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern double j0 (double) __attribute__ ((__nothrow__)); extern double __j0 (double) __attribute__ ((__nothrow__));
extern double j1 (double) __attribute__ ((__nothrow__)); extern double __j1 (double) __attribute__ ((__nothrow__));
extern double jn (int, double) __attribute__ ((__nothrow__)); extern double __jn (int, double) __attribute__ ((__nothrow__));
extern double y0 (double) __attribute__ ((__nothrow__)); extern double __y0 (double) __attribute__ ((__nothrow__));
extern double y1 (double) __attribute__ ((__nothrow__)); extern double __y1 (double) __attribute__ ((__nothrow__));
extern double yn (int, double) __attribute__ ((__nothrow__)); extern double __yn (int, double) __attribute__ ((__nothrow__));

extern double erf (double) __attribute__ ((__nothrow__)); extern double __erf (double) __attribute__ ((__nothrow__));
extern double erfc (double) __attribute__ ((__nothrow__)); extern double __erfc (double) __attribute__ ((__nothrow__));
extern double lgamma (double) __attribute__ ((__nothrow__)); extern double __lgamma (double) __attribute__ ((__nothrow__));
extern double tgamma (double) __attribute__ ((__nothrow__)); extern double __tgamma (double) __attribute__ ((__nothrow__));
extern double gamma (double) __attribute__ ((__nothrow__)); extern double __gamma (double) __attribute__ ((__nothrow__));
extern double lgamma_r (double, int *__signgamp) __attribute__ ((__nothrow__)); extern double __lgamma_r (double, int *__signgamp) __attribute__ ((__nothrow__));
extern double rint (double __x) __attribute__ ((__nothrow__)); extern double __rint (double __x) __attribute__ ((__nothrow__));
extern double nextafter (double __x, double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __nextafter (double __x, double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern double nexttoward (double __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __nexttoward (double __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern double remainder (double __x, double __y) __attribute__ ((__nothrow__)); extern double __remainder (double __x, double __y) __attribute__ ((__nothrow__));
extern double scalbn (double __x, int __n) __attribute__ ((__nothrow__)); extern double __scalbn (double __x, int __n) __attribute__ ((__nothrow__));
extern int ilogb (double __x) __attribute__ ((__nothrow__)); extern int __ilogb (double __x) __attribute__ ((__nothrow__));
extern double scalbln (double __x, long int __n) __attribute__ ((__nothrow__)); extern double __scalbln (double __x, long int __n) __attribute__ ((__nothrow__));
extern double nearbyint (double __x) __attribute__ ((__nothrow__)); extern double __nearbyint (double __x) __attribute__ ((__nothrow__));
extern double round (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __round (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern double trunc (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern double __trunc (double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern double remquo (double __x, double __y, int *__quo) __attribute__ ((__nothrow__)); extern double __remquo (double __x, double __y, int *__quo) __attribute__ ((__nothrow__));
extern long int lrint (double __x) __attribute__ ((__nothrow__)); extern long int __lrint (double __x) __attribute__ ((__nothrow__));
extern long long int llrint (double __x) __attribute__ ((__nothrow__)); extern long long int __llrint (double __x) __attribute__ ((__nothrow__));
extern long int lround (double __x) __attribute__ ((__nothrow__)); extern long int __lround (double __x) __attribute__ ((__nothrow__));
extern long long int llround (double __x) __attribute__ ((__nothrow__)); extern long long int __llround (double __x) __attribute__ ((__nothrow__));
extern double fdim (double __x, double __y) __attribute__ ((__nothrow__)); extern double __fdim (double __x, double __y) __attribute__ ((__nothrow__));
extern double fmax (double __x, double __y) __attribute__ ((__nothrow__)); extern double __fmax (double __x, double __y) __attribute__ ((__nothrow__));
extern double fmin (double __x, double __y) __attribute__ ((__nothrow__)); extern double __fmin (double __x, double __y) __attribute__ ((__nothrow__));

extern int __fpclassify (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern int __signbit (double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern double fma (double __x, double __y, double __z) __attribute__ ((__nothrow__)); extern double __fma (double __x, double __y, double __z) __attribute__ ((__nothrow__));

extern double scalb (double __x, double __n) __attribute__ ((__nothrow__)); extern double __scalb (double __x, double __n) __attribute__ ((__nothrow__));

extern float acosf (float __x) __attribute__ ((__nothrow__)); extern float __acosf (float __x) __attribute__ ((__nothrow__));
extern float asinf (float __x) __attribute__ ((__nothrow__)); extern float __asinf (float __x) __attribute__ ((__nothrow__));
extern float atanf (float __x) __attribute__ ((__nothrow__)); extern float __atanf (float __x) __attribute__ ((__nothrow__));
extern float atan2f (float __y, float __x) __attribute__ ((__nothrow__)); extern float __atan2f (float __y, float __x) __attribute__ ((__nothrow__));
extern float cosf (float __x) __attribute__ ((__nothrow__)); extern float __cosf (float __x) __attribute__ ((__nothrow__));
extern float sinf (float __x) __attribute__ ((__nothrow__)); extern float __sinf (float __x) __attribute__ ((__nothrow__));
extern float tanf (float __x) __attribute__ ((__nothrow__)); extern float __tanf (float __x) __attribute__ ((__nothrow__));
extern float coshf (float __x) __attribute__ ((__nothrow__)); extern float __coshf (float __x) __attribute__ ((__nothrow__));
extern float sinhf (float __x) __attribute__ ((__nothrow__)); extern float __sinhf (float __x) __attribute__ ((__nothrow__));
extern float tanhf (float __x) __attribute__ ((__nothrow__)); extern float __tanhf (float __x) __attribute__ ((__nothrow__));
extern float acoshf (float __x) __attribute__ ((__nothrow__)); extern float __acoshf (float __x) __attribute__ ((__nothrow__));
extern float asinhf (float __x) __attribute__ ((__nothrow__)); extern float __asinhf (float __x) __attribute__ ((__nothrow__));
extern float atanhf (float __x) __attribute__ ((__nothrow__)); extern float __atanhf (float __x) __attribute__ ((__nothrow__));
extern float expf (float __x) __attribute__ ((__nothrow__)); extern float __expf (float __x) __attribute__ ((__nothrow__));
extern float frexpf (float __x, int *__exponent) __attribute__ ((__nothrow__)); extern float __frexpf (float __x, int *__exponent) __attribute__ ((__nothrow__));
extern float ldexpf (float __x, int __exponent) __attribute__ ((__nothrow__)); extern float __ldexpf (float __x, int __exponent) __attribute__ ((__nothrow__));
extern float logf (float __x) __attribute__ ((__nothrow__)); extern float __logf (float __x) __attribute__ ((__nothrow__));
extern float log10f (float __x) __attribute__ ((__nothrow__)); extern float __log10f (float __x) __attribute__ ((__nothrow__));
extern float modff (float __x, float *__iptr) __attribute__ ((__nothrow__)); extern float __modff (float __x, float *__iptr) __attribute__ ((__nothrow__));
extern float expm1f (float __x) __attribute__ ((__nothrow__)); extern float __expm1f (float __x) __attribute__ ((__nothrow__));
extern float log1pf (float __x) __attribute__ ((__nothrow__)); extern float __log1pf (float __x) __attribute__ ((__nothrow__));
extern float logbf (float __x) __attribute__ ((__nothrow__)); extern float __logbf (float __x) __attribute__ ((__nothrow__));
extern float exp2f (float __x) __attribute__ ((__nothrow__)); extern float __exp2f (float __x) __attribute__ ((__nothrow__));
extern float log2f (float __x) __attribute__ ((__nothrow__)); extern float __log2f (float __x) __attribute__ ((__nothrow__));
extern float powf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __powf (float __x, float __y) __attribute__ ((__nothrow__));
extern float sqrtf (float __x) __attribute__ ((__nothrow__)); extern float __sqrtf (float __x) __attribute__ ((__nothrow__));
extern float hypotf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __hypotf (float __x, float __y) __attribute__ ((__nothrow__));
extern float cbrtf (float __x) __attribute__ ((__nothrow__)); extern float __cbrtf (float __x) __attribute__ ((__nothrow__));
extern float ceilf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __ceilf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern float fabsf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __fabsf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern float floorf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __floorf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern float fmodf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __fmodf (float __x, float __y) __attribute__ ((__nothrow__));
extern int __isinff (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int __finitef (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int isinff (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int finitef (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern float dremf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __dremf (float __x, float __y) __attribute__ ((__nothrow__));
extern float significandf (float __x) __attribute__ ((__nothrow__)); extern float __significandf (float __x) __attribute__ ((__nothrow__));
extern float copysignf (float __x, float __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __copysignf (float __x, float __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern float nanf (__const char *__tagb) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __nanf (__const char *__tagb) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int __isnanf (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int isnanf (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern float j0f (float) __attribute__ ((__nothrow__)); extern float __j0f (float) __attribute__ ((__nothrow__));
extern float j1f (float) __attribute__ ((__nothrow__)); extern float __j1f (float) __attribute__ ((__nothrow__));
extern float jnf (int, float) __attribute__ ((__nothrow__)); extern float __jnf (int, float) __attribute__ ((__nothrow__));
extern float y0f (float) __attribute__ ((__nothrow__)); extern float __y0f (float) __attribute__ ((__nothrow__));
extern float y1f (float) __attribute__ ((__nothrow__)); extern float __y1f (float) __attribute__ ((__nothrow__));
extern float ynf (int, float) __attribute__ ((__nothrow__)); extern float __ynf (int, float) __attribute__ ((__nothrow__));

extern float erff (float) __attribute__ ((__nothrow__)); extern float __erff (float) __attribute__ ((__nothrow__));
extern float erfcf (float) __attribute__ ((__nothrow__)); extern float __erfcf (float) __attribute__ ((__nothrow__));
extern float lgammaf (float) __attribute__ ((__nothrow__)); extern float __lgammaf (float) __attribute__ ((__nothrow__));
extern float tgammaf (float) __attribute__ ((__nothrow__)); extern float __tgammaf (float) __attribute__ ((__nothrow__));
extern float gammaf (float) __attribute__ ((__nothrow__)); extern float __gammaf (float) __attribute__ ((__nothrow__));
extern float lgammaf_r (float, int *__signgamp) __attribute__ ((__nothrow__)); extern float __lgammaf_r (float, int *__signgamp) __attribute__ ((__nothrow__));
extern float rintf (float __x) __attribute__ ((__nothrow__)); extern float __rintf (float __x) __attribute__ ((__nothrow__));
extern float nextafterf (float __x, float __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __nextafterf (float __x, float __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern float nexttowardf (float __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __nexttowardf (float __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern float remainderf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __remainderf (float __x, float __y) __attribute__ ((__nothrow__));
extern float scalbnf (float __x, int __n) __attribute__ ((__nothrow__)); extern float __scalbnf (float __x, int __n) __attribute__ ((__nothrow__));
extern int ilogbf (float __x) __attribute__ ((__nothrow__)); extern int __ilogbf (float __x) __attribute__ ((__nothrow__));
extern float scalblnf (float __x, long int __n) __attribute__ ((__nothrow__)); extern float __scalblnf (float __x, long int __n) __attribute__ ((__nothrow__));
extern float nearbyintf (float __x) __attribute__ ((__nothrow__)); extern float __nearbyintf (float __x) __attribute__ ((__nothrow__));
extern float roundf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __roundf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern float truncf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern float __truncf (float __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern float remquof (float __x, float __y, int *__quo) __attribute__ ((__nothrow__)); extern float __remquof (float __x, float __y, int *__quo) __attribute__ ((__nothrow__));

extern long int lrintf (float __x) __attribute__ ((__nothrow__)); extern long int __lrintf (float __x) __attribute__ ((__nothrow__));
extern long long int llrintf (float __x) __attribute__ ((__nothrow__)); extern long long int __llrintf (float __x) __attribute__ ((__nothrow__));

extern long int lroundf (float __x) __attribute__ ((__nothrow__)); extern long int __lroundf (float __x) __attribute__ ((__nothrow__));
extern long long int llroundf (float __x) __attribute__ ((__nothrow__)); extern long long int __llroundf (float __x) __attribute__ ((__nothrow__));

extern float fdimf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __fdimf (float __x, float __y) __attribute__ ((__nothrow__));
extern float fmaxf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __fmaxf (float __x, float __y) __attribute__ ((__nothrow__));
extern float fminf (float __x, float __y) __attribute__ ((__nothrow__)); extern float __fminf (float __x, float __y) __attribute__ ((__nothrow__));

extern int __fpclassifyf (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern int __signbitf (float __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern float fmaf (float __x, float __y, float __z) __attribute__ ((__nothrow__)); extern float __fmaf (float __x, float __y, float __z) __attribute__ ((__nothrow__));

extern float scalbf (float __x, float __n) __attribute__ ((__nothrow__)); extern float __scalbf (float __x, float __n) __attribute__ ((__nothrow__));

extern long double acosl (long double __x) __attribute__ ((__nothrow__)); extern long double __acosl (long double __x) __attribute__ ((__nothrow__));
extern long double asinl (long double __x) __attribute__ ((__nothrow__)); extern long double __asinl (long double __x) __attribute__ ((__nothrow__));
extern long double atanl (long double __x) __attribute__ ((__nothrow__)); extern long double __atanl (long double __x) __attribute__ ((__nothrow__));
extern long double atan2l (long double __y, long double __x) __attribute__ ((__nothrow__)); extern long double __atan2l (long double __y, long double __x) __attribute__ ((__nothrow__));
extern long double cosl (long double __x) __attribute__ ((__nothrow__)); extern long double __cosl (long double __x) __attribute__ ((__nothrow__));
extern long double sinl (long double __x) __attribute__ ((__nothrow__)); extern long double __sinl (long double __x) __attribute__ ((__nothrow__));
extern long double tanl (long double __x) __attribute__ ((__nothrow__)); extern long double __tanl (long double __x) __attribute__ ((__nothrow__));
extern long double coshl (long double __x) __attribute__ ((__nothrow__)); extern long double __coshl (long double __x) __attribute__ ((__nothrow__));
extern long double sinhl (long double __x) __attribute__ ((__nothrow__)); extern long double __sinhl (long double __x) __attribute__ ((__nothrow__));
extern long double tanhl (long double __x) __attribute__ ((__nothrow__)); extern long double __tanhl (long double __x) __attribute__ ((__nothrow__));
extern long double acoshl (long double __x) __attribute__ ((__nothrow__)); extern long double __acoshl (long double __x) __attribute__ ((__nothrow__));
extern long double asinhl (long double __x) __attribute__ ((__nothrow__)); extern long double __asinhl (long double __x) __attribute__ ((__nothrow__));
extern long double atanhl (long double __x) __attribute__ ((__nothrow__)); extern long double __atanhl (long double __x) __attribute__ ((__nothrow__));
extern long double expl (long double __x) __attribute__ ((__nothrow__)); extern long double __expl (long double __x) __attribute__ ((__nothrow__));
extern long double frexpl (long double __x, int *__exponent) __attribute__ ((__nothrow__)); extern long double __frexpl (long double __x, int *__exponent) __attribute__ ((__nothrow__));
extern long double ldexpl (long double __x, int __exponent) __attribute__ ((__nothrow__)); extern long double __ldexpl (long double __x, int __exponent) __attribute__ ((__nothrow__));
extern long double logl (long double __x) __attribute__ ((__nothrow__)); extern long double __logl (long double __x) __attribute__ ((__nothrow__));
extern long double log10l (long double __x) __attribute__ ((__nothrow__)); extern long double __log10l (long double __x) __attribute__ ((__nothrow__));
extern long double modfl (long double __x, long double *__iptr) __attribute__ ((__nothrow__)); extern long double __modfl (long double __x, long double *__iptr) __attribute__ ((__nothrow__));
extern long double expm1l (long double __x) __attribute__ ((__nothrow__)); extern long double __expm1l (long double __x) __attribute__ ((__nothrow__));
extern long double log1pl (long double __x) __attribute__ ((__nothrow__)); extern long double __log1pl (long double __x) __attribute__ ((__nothrow__));
extern long double logbl (long double __x) __attribute__ ((__nothrow__)); extern long double __logbl (long double __x) __attribute__ ((__nothrow__));
extern long double exp2l (long double __x) __attribute__ ((__nothrow__)); extern long double __exp2l (long double __x) __attribute__ ((__nothrow__));
extern long double log2l (long double __x) __attribute__ ((__nothrow__)); extern long double __log2l (long double __x) __attribute__ ((__nothrow__));
extern long double powl (long double __x, long double __y) __attribute__ ((__nothrow__)); extern long double __powl (long double __x, long double __y) __attribute__ ((__nothrow__));
extern long double sqrtl (long double __x) __attribute__ ((__nothrow__)); extern long double __sqrtl (long double __x) __attribute__ ((__nothrow__));
extern long double hypotl (long double __x, long double __y) __attribute__ ((__nothrow__)); extern long double __hypotl (long double __x, long double __y) __attribute__ ((__nothrow__));
extern long double cbrtl (long double __x) __attribute__ ((__nothrow__)); extern long double __cbrtl (long double __x) __attribute__ ((__nothrow__));
extern long double ceill (long double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __ceill (long double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern long double fabsl (long double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __fabsl (long double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern long double floorl (long double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __floorl (long double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern long double fmodl (long double __x, long double __y) __attribute__ ((__nothrow__)); extern long double __fmodl (long double __x, long double __y) __attribute__ ((__nothrow__));
extern int __isinfl (long double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int __finitel (long double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int isinfl (long double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int finitel (long double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern long double dreml (long double __x, long double __y) __attribute__ ((__nothrow__)); extern long double __dreml (long double __x, long double __y) __attribute__ ((__nothrow__));
extern long double significandl (long double __x) __attribute__ ((__nothrow__)); extern long double __significandl (long double __x) __attribute__ ((__nothrow__));
extern long double copysignl (long double __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __copysignl (long double __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern long double nanl (__const char *__tagb) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __nanl (__const char *__tagb) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int __isnanl (long double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern int isnanl (long double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern long double j0l (long double) __attribute__ ((__nothrow__)); extern long double __j0l (long double) __attribute__ ((__nothrow__));
extern long double j1l (long double) __attribute__ ((__nothrow__)); extern long double __j1l (long double) __attribute__ ((__nothrow__));
extern long double jnl (int, long double) __attribute__ ((__nothrow__)); extern long double __jnl (int, long double) __attribute__ ((__nothrow__));
extern long double y0l (long double) __attribute__ ((__nothrow__)); extern long double __y0l (long double) __attribute__ ((__nothrow__));
extern long double y1l (long double) __attribute__ ((__nothrow__)); extern long double __y1l (long double) __attribute__ ((__nothrow__));
extern long double ynl (int, long double) __attribute__ ((__nothrow__)); extern long double __ynl (int, long double) __attribute__ ((__nothrow__));

extern long double erfl (long double) __attribute__ ((__nothrow__)); extern long double __erfl (long double) __attribute__ ((__nothrow__));
extern long double erfcl (long double) __attribute__ ((__nothrow__)); extern long double __erfcl (long double) __attribute__ ((__nothrow__));
extern long double lgammal (long double) __attribute__ ((__nothrow__)); extern long double __lgammal (long double) __attribute__ ((__nothrow__));
extern long double tgammal (long double) __attribute__ ((__nothrow__)); extern long double __tgammal (long double) __attribute__ ((__nothrow__));
extern long double gammal (long double) __attribute__ ((__nothrow__)); extern long double __gammal (long double) __attribute__ ((__nothrow__));
extern long double lgammal_r (long double, int *__signgamp) __attribute__ ((__nothrow__)); extern long double __lgammal_r (long double, int *__signgamp) __attribute__ ((__nothrow__));
extern long double rintl (long double __x) __attribute__ ((__nothrow__)); extern long double __rintl (long double __x) __attribute__ ((__nothrow__));
extern long double nextafterl (long double __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __nextafterl (long double __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern long double nexttowardl (long double __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __nexttowardl (long double __x, long double __y) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern long double remainderl (long double __x, long double __y) __attribute__ ((__nothrow__)); extern long double __remainderl (long double __x, long double __y) __attribute__ ((__nothrow__));
extern long double scalbnl (long double __x, int __n) __attribute__ ((__nothrow__)); extern long double __scalbnl (long double __x, int __n) __attribute__ ((__nothrow__));
extern int ilogbl (long double __x) __attribute__ ((__nothrow__)); extern int __ilogbl (long double __x) __attribute__ ((__nothrow__));
extern long double scalblnl (long double __x, long int __n) __attribute__ ((__nothrow__)); extern long double __scalblnl (long double __x, long int __n) __attribute__ ((__nothrow__));
extern long double nearbyintl (long double __x) __attribute__ ((__nothrow__)); extern long double __nearbyintl (long double __x) __attribute__ ((__nothrow__));
extern long double roundl (long double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __roundl (long double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern long double truncl (long double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__)); extern long double __truncl (long double __x) __attribute__ ((__nothrow__)) __attribute__ ((__const__));
extern long double remquol (long double __x, long double __y, int *__quo) __attribute__ ((__nothrow__)); extern long double __remquol (long double __x, long double __y, int *__quo) __attribute__ ((__nothrow__));

extern long int lrintl (long double __x) __attribute__ ((__nothrow__)); extern long int __lrintl (long double __x) __attribute__ ((__nothrow__));
extern long long int llrintl (long double __x) __attribute__ ((__nothrow__)); extern long long int __llrintl (long double __x) __attribute__ ((__nothrow__));

extern long int lroundl (long double __x) __attribute__ ((__nothrow__)); extern long int __lroundl (long double __x) __attribute__ ((__nothrow__));
extern long long int llroundl (long double __x) __attribute__ ((__nothrow__)); extern long long int __llroundl (long double __x) __attribute__ ((__nothrow__));

extern long double fdiml (long double __x, long double __y) __attribute__ ((__nothrow__)); extern long double __fdiml (long double __x, long double __y) __attribute__ ((__nothrow__));
extern long double fmaxl (long double __x, long double __y) __attribute__ ((__nothrow__)); extern long double __fmaxl (long double __x, long double __y) __attribute__ ((__nothrow__));
extern long double fminl (long double __x, long double __y) __attribute__ ((__nothrow__)); extern long double __fminl (long double __x, long double __y) __attribute__ ((__nothrow__));

extern int __fpclassifyl (long double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern int __signbitl (long double __value) __attribute__ ((__nothrow__)) __attribute__ ((__const__));

extern long double fmal (long double __x, long double __y, long double __z) __attribute__ ((__nothrow__)); extern long double __fmal (long double __x, long double __y, long double __z) __attribute__ ((__nothrow__));

extern long double scalbl (long double __x, long double __n) __attribute__ ((__nothrow__)); extern long double __scalbl (long double __x, long double __n) __attribute__ ((__nothrow__));

extern int signgam;

enum
{
    FP_NAN,
    FP_INFINITE,
    FP_ZERO,
    FP_SUBNORMAL,
    FP_NORMAL
};

typedef enum
{
    _IEEE_ = -1,
    _SVID_,
    _XOPEN_,
    _POSIX_,
    _ISOC_
} _LIB_VERSION_TYPE;

extern _LIB_VERSION_TYPE _LIB_VERSION;

struct exception
{
    int type;
    char *name;
    double arg1;
    double arg2;
    double retval;
};

extern int matherr (struct exception *__exc);

extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) __signbitf (float __x)
{
    int __m;
    __asm ("pmovmskb %1, %0" : "=r" (__m) : "x" (__x));
    return __m & 0x8;
}
extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) __signbit (double __x)
{
    int __m;
    __asm ("pmovmskb %1, %0" : "=r" (__m) : "x" (__x));
    return __m & 0x80;
}
extern __inline __attribute__ ((__gnu_inline__)) int __attribute__ ((__nothrow__)) __signbitl (long double __x)
{
    __extension__ union { long double __l; int __i[3]; } __u = { __l: __x };
    return (__u.__i[2] & 0x8000) != 0;
}

typedef unsigned char cc_t;
typedef unsigned int speed_t;
typedef unsigned int tcflag_t;

struct termios
{
    tcflag_t c_iflag;
    tcflag_t c_oflag;
    tcflag_t c_cflag;
    tcflag_t c_lflag;
    cc_t c_line;
    cc_t c_cc[32];
    speed_t c_ispeed;
    speed_t c_ospeed;
};

extern speed_t cfgetospeed (__const struct termios *__termios_p) __attribute__ ((__nothrow__));
extern speed_t cfgetispeed (__const struct termios *__termios_p) __attribute__ ((__nothrow__));

extern int cfsetospeed (struct termios *__termios_p, speed_t __speed) __attribute__ ((__nothrow__));
extern int cfsetispeed (struct termios *__termios_p, speed_t __speed) __attribute__ ((__nothrow__));
extern int cfsetspeed (struct termios *__termios_p, speed_t __speed) __attribute__ ((__nothrow__));

extern int tcgetattr (int __fd, struct termios *__termios_p) __attribute__ ((__nothrow__));
extern int tcsetattr (int __fd, int __optional_actions, __const struct termios *__termios_p) __attribute__ ((__nothrow__));

extern void cfmakeraw (struct termios *__termios_p) __attribute__ ((__nothrow__));

extern int tcsendbreak (int __fd, int __duration) __attribute__ ((__nothrow__));

extern int tcdrain (int __fd);

extern int tcflush (int __fd, int __queue_selector) __attribute__ ((__nothrow__));

extern int tcflow (int __fd, int __action) __attribute__ ((__nothrow__));

extern char PC;
extern char * UP;
extern char * BC;
extern unsigned ospeed;

extern char * tgetstr ( char *, char **);
extern char * tgoto (const char *, int, int);
extern int tgetent (char *, const char *);
extern int tgetflag ( char *);
extern int tgetnum ( char *);
extern int tputs (const char *, int, int (*)(int));

enum __rlimit_resource
{
    RLIMIT_CPU = 0,
    RLIMIT_FSIZE = 1,
    RLIMIT_DATA = 2,
    RLIMIT_STACK = 3,
    RLIMIT_CORE = 4,
    __RLIMIT_RSS = 5,
    RLIMIT_NOFILE = 7,
    __RLIMIT_OFILE = RLIMIT_NOFILE,
    RLIMIT_AS = 9,
    __RLIMIT_NPROC = 6,
    __RLIMIT_MEMLOCK = 8,
    __RLIMIT_LOCKS = 10,
    __RLIMIT_SIGPENDING = 11,
    __RLIMIT_MSGQUEUE = 12,
    __RLIMIT_NICE = 13,
    __RLIMIT_RTPRIO = 14,
    __RLIMIT_NLIMITS = 15,
    __RLIM_NLIMITS = __RLIMIT_NLIMITS
};

typedef __rlim_t rlim_t;

struct rlimit
{
    rlim_t rlim_cur;
    rlim_t rlim_max;
};

enum __rusage_who
{
    RUSAGE_SELF = 0,
    RUSAGE_CHILDREN = -1
};

struct rusage
{
    struct timeval ru_utime;
    struct timeval ru_stime;

    long int ru_maxrss;
    long int ru_ixrss;
    long int ru_idrss;
    long int ru_isrss;
    long int ru_minflt;
    long int ru_majflt;
    long int ru_nswap;
    long int ru_inblock;
    long int ru_oublock;
    long int ru_msgsnd;
    long int ru_msgrcv;
    long int ru_nsignals;
    long int ru_nvcsw;
    long int ru_nivcsw;
};

enum __priority_which
{
    PRIO_PROCESS = 0,
    PRIO_PGRP = 1,
    PRIO_USER = 2
};

typedef int __rlimit_resource_t;
typedef int __rusage_who_t;
typedef int __priority_which_t;

extern int getrlimit (__rlimit_resource_t __resource, struct rlimit *__rlimits) __attribute__ ((__nothrow__));

extern int setrlimit (__rlimit_resource_t __resource, __const struct rlimit *__rlimits) __attribute__ ((__nothrow__));

extern int getrusage (__rusage_who_t __who, struct rusage *__usage) __attribute__ ((__nothrow__));

extern int getpriority (__priority_which_t __which, id_t __who) __attribute__ ((__nothrow__));

extern int setpriority (__priority_which_t __which, id_t __who, int __prio) __attribute__ ((__nothrow__));

struct sysinfo
{
    long uptime;
    unsigned long loads[3];
    unsigned long totalram;
    unsigned long freeram;
    unsigned long sharedram;
    unsigned long bufferram;
    unsigned long totalswap;
    unsigned long freeswap;
    unsigned short procs;
    unsigned short pad;
    unsigned long totalhigh;
    unsigned long freehigh;
    unsigned int mem_unit;
    char _f[20-2*sizeof(long)-sizeof(int)];
};

struct module;

void mark_hardware_unsupported(const char *msg);
void mark_tech_preview(const char *msg, struct module *mod);

extern int sysinfo (struct sysinfo *__info) __attribute__ ((__nothrow__));

extern int get_nprocs_conf (void) __attribute__ ((__nothrow__));

extern int get_nprocs (void) __attribute__ ((__nothrow__));

extern long int get_phys_pages (void) __attribute__ ((__nothrow__));

extern long int get_avphys_pages (void) __attribute__ ((__nothrow__));

struct utsname
{
    char sysname[65];
    char nodename[65];
    char release[65];
    char version[65];
    char machine[65];
    char __domainname[65];
};

extern int uname (struct utsname *__name) __attribute__ ((__nothrow__));

typedef long int __jmp_buf[8];

struct __jmp_buf_tag
{
    __jmp_buf __jmpbuf;
    int __mask_was_saved;
    __sigset_t __saved_mask;
};

typedef struct __jmp_buf_tag jmp_buf[1];

extern int setjmp (jmp_buf __env) __attribute__ ((__nothrow__));

extern int __sigsetjmp (struct __jmp_buf_tag __env[1], int __savemask) __attribute__ ((__nothrow__));

extern int _setjmp (struct __jmp_buf_tag __env[1]) __attribute__ ((__nothrow__));

extern void longjmp (struct __jmp_buf_tag __env[1], int __val) __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));

extern void _longjmp (struct __jmp_buf_tag __env[1], int __val) __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));

typedef struct __jmp_buf_tag sigjmp_buf[1];

extern void siglongjmp (sigjmp_buf __env, int __val) __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));

extern void longjmp (struct __jmp_buf_tag __env[1], int __val) __asm__ ("" "__longjmp_chk") __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));
extern void _longjmp (struct __jmp_buf_tag __env[1], int __val) __asm__ ("" "__longjmp_chk") __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));
extern void siglongjmp (struct __jmp_buf_tag __env[1], int __val) __asm__ ("" "__longjmp_chk") __attribute__ ((__nothrow__)) __attribute__ ((__noreturn__));

struct winsize
{
    unsigned short int ws_row;
    unsigned short int ws_col;
    unsigned short int ws_xpixel;
    unsigned short int ws_ypixel;
};

struct termio
{
    unsigned short int c_iflag;
    unsigned short int c_oflag;
    unsigned short int c_cflag;
    unsigned short int c_lflag;
    unsigned char c_line;
    unsigned char c_cc[8];
};

extern int ioctl (int __fd, unsigned long int __request, ...) __attribute__ ((__nothrow__));

typedef enum
{
    P_ALL,
    P_PID,
    P_PGID
} idtype_t;

extern __pid_t wait (__WAIT_STATUS __stat_loc);

extern __pid_t waitpid (__pid_t __pid, int *__stat_loc, int __options);

extern int waitid (idtype_t __idtype, __id_t __id, siginfo_t *__infop, int __options);

struct rusage;

extern __pid_t wait3 (__WAIT_STATUS __stat_loc, int __options, struct rusage * __usage) __attribute__ ((__nothrow__));

extern __pid_t wait4 (__pid_t __pid, __WAIT_STATUS __stat_loc, int __options, struct rusage *__usage) __attribute__ ((__nothrow__));

struct flock
{
    short int l_type;
    short int l_whence;

    __off_t l_start;
    __off_t l_len;

    __pid_t l_pid;
};

extern int fcntl (int __fd, int __cmd, ...);

extern int open (__const char *__file, int __oflag, ...) __attribute__ ((__nonnull__ (1)));

extern int openat (int __fd, __const char *__file, int __oflag, ...) __attribute__ ((__nonnull__ (2)));

extern int creat (__const char *__file, __mode_t __mode) __attribute__ ((__nonnull__ (1)));

extern int posix_fadvise (int __fd, __off_t __offset, __off_t __len, int __advise) __attribute__ ((__nothrow__));

extern int posix_fallocate (int __fd, __off_t __offset, __off_t __len);

extern int __open_2 (__const char *__path, int __oflag) __attribute__ ((__nonnull__ (1)));
extern int __open_alias (__const char *__path, int __oflag, ...) __asm__ ("" "open") __attribute__ ((__nonnull__ (1)));

extern void __open_too_many_args (void) __attribute__((__error__ ("open can be called either with 2 or 3 arguments, not more")));
extern void __open_missing_mode (void) __attribute__((__error__ ("open with O_CREAT in second argument needs 3 arguments")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) int open (__const char *__path, int __oflag, ...)
{
    if (__builtin_va_arg_pack_len () > 1)
        __open_too_many_args ();

    if (__builtin_constant_p (__oflag))
    {
        if ((__oflag & 0100) != 0 && __builtin_va_arg_pack_len () < 1)
        {
            __open_missing_mode ();
            return __open_2 (__path, __oflag);
        }
        return __open_alias (__path, __oflag, __builtin_va_arg_pack ());
    }

    if (__builtin_va_arg_pack_len () < 1)
        return __open_2 (__path, __oflag);

    return __open_alias (__path, __oflag, __builtin_va_arg_pack ());
}

extern int __openat_2 (int __fd, __const char *__path, int __oflag) __attribute__ ((__nonnull__ (2)));
extern int __openat_alias (int __fd, __const char *__path, int __oflag, ...) __asm__ ("" "openat") __attribute__ ((__nonnull__ (2)));

extern void __openat_too_many_args (void) __attribute__((__error__ ("openat can be called either with 3 or 4 arguments, not more")));
extern void __openat_missing_mode (void) __attribute__((__error__ ("openat with O_CREAT in third argument needs 4 arguments")));

extern __inline __attribute__ ((__always_inline__)) __attribute__ ((__gnu_inline__, __artificial__)) int openat (int __fd, __const char *__path, int __oflag, ...)
{
    if (__builtin_va_arg_pack_len () > 1)
        __openat_too_many_args ();

    if (__builtin_constant_p (__oflag))
    {
        if ((__oflag & 0100) != 0 && __builtin_va_arg_pack_len () < 1)
        {
            __openat_missing_mode ();
            return __openat_2 (__fd, __path, __oflag);
        }
        return __openat_alias (__fd, __path, __oflag, __builtin_va_arg_pack ());
    }

    if (__builtin_va_arg_pack_len () < 1)
        return __openat_2 (__fd, __path, __oflag);

    return __openat_alias (__fd, __path, __oflag, __builtin_va_arg_pack ());
}

struct utimbuf
{
    __time_t actime;
    __time_t modtime;
};

extern int utime (__const char *__file, __const struct utimbuf *__file_times) __attribute__ ((__nothrow__)) __attribute__ ((__nonnull__ (1)));

