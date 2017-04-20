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
public class VimB
{
    /*private*/ static @interface fd_set_ptr_t   { }
    /*private*/ static @interface sighandler_t   { }
    /*private*/ static @interface sigset_ptr_t   { }
    /*private*/ static @interface time_ptr_t     { }
    /*private*/ static @interface timezone_ptr_t { }

    /*private*/ static final int
        SIGHUP    =  1,
        SIGINT    =  2,
        SIGQUIT   =  3,
        SIGILL    =  4,
        SIGTRAP   =  5,
        SIGABRT   =  6,
        SIGBUS    =  7,
        SIGFPE    =  8,
        SIGKILL   =  9,
        SIGUSR1   = 10,
        SIGSEGV   = 11,
        SIGUSR2   = 12,
        SIGPIPE   = 13,
        SIGALRM   = 14,
        SIGTERM   = 15,
        SIGTSTP   = 20,
        SIGXCPU   = 24,
        SIGXFSZ   = 25,
        SIGVTALRM = 26,
        SIGPROF   = 27,
        SIGWINCH  = 28,
        SIGPWR    = 30,
        SIGSYS    = 31;

    /*private*/ static final @sighandler_t long
        SIG_DFL = 0,
        SIG_IGN = 1,
        SIG_ERR = -1;
    /*private*/ static final int SA_ONSTACK = 0x08000000;

    /*private*/ static final int W_OK = 2;

    /*private*/ static final int
        F_GETFD = 1,
        F_SETFD = 2;

    /*private*/ static final int FD_CLOEXEC = 1;

    /*private*/ static final int
        SEEK_SET = 0,
        SEEK_END = 2;

    /*private*/ static final int
        O_RDONLY   = 00,
        O_WRONLY   = 01,
        O_RDWR     = 02,
        O_CREAT    = 0100,
        O_EXCL     = 0200,
        O_TRUNC    = 01000,
        O_APPEND   = 02000,
        O_NONBLOCK = 04000,
        O_NOFOLLOW = 0400000;

    /*private*/ static final int
        __S_IFIFO  = 0010000,
        __S_IFDIR  = 0040000,
        __S_IFBLK  = 0060000,
        __S_IFREG  = 0100000,
        __S_IFSOCK = 0140000,
        __S_IFMT   = 0170000;

    /*private*/ static final boolean __S_ISTYPE(int mode, int mask)
    {
        return ((mode & __S_IFMT) == mask);
    }

    /*private*/ static final boolean S_ISFIFO(int mode) { return __S_ISTYPE(mode, __S_IFIFO);  }
    /*private*/ static final boolean S_ISDIR(int mode)  { return __S_ISTYPE(mode, __S_IFDIR);  }
    /*private*/ static final boolean S_ISBLK(int mode)  { return __S_ISTYPE(mode, __S_IFBLK);  }
    /*private*/ static final boolean S_ISREG(int mode)  { return __S_ISTYPE(mode, __S_IFREG);  }
    /*private*/ static final boolean S_ISSOCK(int mode) { return __S_ISTYPE(mode, __S_IFSOCK); }

    /*private*/ static final int EOF = -1;

    /*private*/ static final int
        ISIG   = 0000001,
        ICANON = 0000002,
        ONLCR  = 0000004,
        ECHO   = 0000010,
        ECHOE  = 0000020,
        ICRNL  = 0000400,
        IEXTEN = 0100000;

    /*private*/ static final int TCSANOW = 0;
    /*private*/ static final int TIOCGWINSZ = 0x5413;
    /*private*/ static final int TCIFLUSH = 0;

    /*private*/ static final int
        ENOENT    = 2,
        EINTR     = 4,
        EINVAL    = 22,
        EFBIG     = 27,
        EOVERFLOW = 75;

    /*private*/ static final int MAXNAMLEN = 255;

    /* ----------------------------------------------------------------------- */

    /*private*/ static abstract class struct_C
    {
        /*private*/ static abstract class Bridge<T extends struct_C> implements DataConverter<T, Pointer>
        {
            protected abstract T wrap(Pointer pointer);

            public T fromNative(Pointer pointer, FromNativeContext _context)
            {
                return (pointer != null) ? wrap(pointer) : null;
            }

            public Pointer toNative(T struct, ToNativeContext _context)
            {
                return (struct != null) ? struct.memory : null;
            }

            public Class<Pointer> nativeType()
            {
                return Pointer.class;
            }
        }

        protected final Pointer memory;

        protected struct_C(StructLayout layout)
        {
            memory = Memory.allocate(jnr.ffi.Runtime.getRuntime(libc), layout.size());
        }

        protected struct_C(long address)
        {
            memory = Pointer.wrap(jnr.ffi.Runtime.getRuntime(libc), address);
        }

        protected struct_C(Pointer pointer)
        {
            memory = pointer;
        }
    }

    /*private*/ static final class stat_C extends struct_C
    {
        /*private*/ static final int VERSION = 1;

        /*private*/ static final Bridge<stat_C> BRIDGE = new Bridge<stat_C>()
        {
            protected stat_C wrap(Pointer pointer)
            {
                return new stat_C(pointer);
            }
        };

        /*private*/ static final class Layout extends StructLayout
        {
            /*private*/ Layout(jnr.ffi.Runtime runtime)
            {
                super(runtime);
            }

            final Signed64 st_dev = new Signed64();         // @dev_t long st_dev
            final Signed64 st_ino = new Signed64();         // @ino_t long st_ino
            final Signed64 st_nlink = new Signed64();       // @nlink_t long st_nlink
            final Signed32 st_mode = new Signed32();        // @mode_t int st_mode
            final Signed32 st_uid = new Signed32();         // @uid_t int st_uid
            final Signed32 st_gid = new Signed32();         // @gid_t int st_gid
            final Signed32 __pad0 = new Signed32();         // int __pad0
            final Signed64 st_rdev = new Signed64();        // @dev_t long st_rdev
            final Signed64 st_size = new Signed64();        // @off_t long st_size
            final Signed64 st_blksize = new Signed64();     // @blksize_t long st_blksize
            final Signed64 st_blocks = new Signed64();      // @blkcnt_t long st_blocks
            final Signed64 st_atim_sec = new Signed64();    // @time_t long st_atim.tv_sec
            final Signed64 st_atim_nsec = new Signed64();   // long st_atim.tv_nsec
            final Signed64 st_mtim_sec = new Signed64();    // @time_t long st_mtim.tv_sec
            final Signed64 st_mtim_nsec = new Signed64();   // long st_mtim.tv_nsec
            final Signed64 st_ctim_sec = new Signed64();    // @time_t long st_ctim.tv_sec
            final Signed64 st_ctim_nsec = new Signed64();   // long st_ctim.tv_nsec
            final Signed64 __unused1 = new Signed64();      // long __unused1
            final Signed64 __unused2 = new Signed64();      // long __unused2
            final Signed64 __unused3 = new Signed64();      // long __unused3
        }

        /*private*/ static final Layout layout = new Layout(jnr.ffi.Runtime.getSystemRuntime());

        /*private*/ stat_C()
        {
            super(layout);
        }

        /*private*/ stat_C(Pointer pointer)
        {
            super(pointer);
        }

        long st_dev()   { return layout.st_dev.get(memory); }
        void st_dev(long dev)  { layout.st_dev.set(memory, dev); }
        long st_ino()   { return layout.st_ino.get(memory); }
        void st_ino(long ino)  { layout.st_ino.set(memory, ino); }
        long st_nlink() { return layout.st_nlink.get(memory); }
        int st_mode()   { return layout.st_mode.get(memory); }
        int st_uid()    { return layout.st_uid.get(memory); }
        int st_gid()    { return layout.st_gid.get(memory); }
        long st_size()  { return layout.st_size.get(memory); }
        long st_atime() { return layout.st_atim_sec.get(memory); }
        long st_mtime() { return layout.st_mtim_sec.get(memory); }
    }

    /*private*/ static final class timeval_C extends struct_C
    {
        /*private*/ static final Bridge<timeval_C> BRIDGE = new Bridge<timeval_C>()
        {
            protected timeval_C wrap(Pointer pointer)
            {
                return new timeval_C(pointer);
            }
        };

        /*private*/ static final class Layout extends StructLayout
        {
            /*private*/ Layout(jnr.ffi.Runtime runtime)
            {
                super(runtime);
            }

            final Signed64 tv_sec = new Signed64();     // @time_t long tv_sec
            final Signed64 tv_usec = new Signed64();    // @suseconds_t long tv_usec
        }

        /*private*/ static final Layout layout = new Layout(jnr.ffi.Runtime.getSystemRuntime());

        /*private*/ timeval_C()
        {
            super(layout);
        }

        /*private*/ timeval_C(Pointer pointer)
        {
            super(pointer);
        }

        long tv_sec()    { return layout.tv_sec.get(memory); }
        void tv_sec(long sec)   { layout.tv_sec.set(memory, sec); }
        long tv_usec()   { return layout.tv_usec.get(memory); }
        void tv_usec(long usec) { layout.tv_usec.set(memory, usec); }
    }

    /*private*/ static void COPY_timeval(timeval_C tv1, timeval_C tv0)
    {
        tv1.tv_sec(tv0.tv_sec());
        tv1.tv_usec(tv0.tv_usec());
    }

    /*private*/ static final class timespec_C extends struct_C
    {
        /*private*/ static final Bridge<timespec_C> BRIDGE = new Bridge<timespec_C>()
        {
            protected timespec_C wrap(Pointer pointer)
            {
                return new timespec_C(pointer);
            }
        };

        /*private*/ static final class Layout extends StructLayout
        {
            /*private*/ Layout(jnr.ffi.Runtime runtime)
            {
                super(runtime);
            }

            final Signed64 tv_sec = new Signed64();     // @time_t long tv_sec
            final Signed64 tv_nsec = new Signed64();    // long tv_nsec
        }

        /*private*/ static final Layout layout = new Layout(jnr.ffi.Runtime.getSystemRuntime());

        /*private*/ timespec_C()
        {
            super(layout);
        }

        /*private*/ timespec_C(Pointer pointer)
        {
            super(pointer);
        }

        void tv_sec(long sec)   { layout.tv_sec.set(memory, sec); }
        void tv_nsec(long nsec) { layout.tv_nsec.set(memory, nsec); }
    }

    /*private*/ static final class tm_C extends struct_C
    {
        /*private*/ static final Bridge<tm_C> BRIDGE = new Bridge<tm_C>()
        {
            protected tm_C wrap(Pointer pointer)
            {
                return new tm_C(pointer);
            }
        };

        /*private*/ static final class Layout extends StructLayout
        {
            /*private*/ Layout(jnr.ffi.Runtime runtime)
            {
                super(runtime);
            }

            final Signed32 tm_sec = new Signed32();     // int tm_sec
            final Signed32 tm_min = new Signed32();     // int tm_min
            final Signed32 tm_hour = new Signed32();    // int tm_hour
            final Signed32 tm_mday = new Signed32();    // int tm_mday
            final Signed32 tm_mon = new Signed32();     // int tm_mon
            final Signed32 tm_year = new Signed32();    // int tm_year
            final Signed32 tm_wday = new Signed32();    // int tm_wday
            final Signed32 tm_yday = new Signed32();    // int tm_yday
            final Signed32 tm_isdst = new Signed32();   // int tm_isdst
            final Signed64 tm_gmtoff = new Signed64();  // long tm_gmtoff
            final AsciiStringRef tm_zone = new AsciiStringRef(); // byte *tm_zone
        }

        /*private*/ static final Layout layout = new Layout(jnr.ffi.Runtime.getSystemRuntime());

        /*private*/ tm_C()
        {
            super(layout);
        }

        /*private*/ tm_C(Pointer pointer)
        {
            super(pointer);
        }
    }

    /*private*/ static final class winsize_C extends struct_C
    {
        /*private*/ static final Bridge<winsize_C> BRIDGE = new Bridge<winsize_C>()
        {
            protected winsize_C wrap(Pointer pointer)
            {
                return new winsize_C(pointer);
            }
        };

        /*private*/ static final class Layout extends StructLayout
        {
            /*private*/ Layout(jnr.ffi.Runtime runtime)
            {
                super(runtime);
            }

            final Unsigned16 ws_row = new Unsigned16();     // unsigned short ws_row
            final Unsigned16 ws_col = new Unsigned16();     // unsigned short ws_col
            final Unsigned16 ws_xpixel = new Unsigned16();  // unsigned short ws_xpixel
            final Unsigned16 ws_ypixel = new Unsigned16();  // unsigned short ws_ypixel
        }

        /*private*/ static final Layout layout = new Layout(jnr.ffi.Runtime.getSystemRuntime());

        /*private*/ winsize_C()
        {
            super(layout);
        }

        /*private*/ winsize_C(Pointer pointer)
        {
            super(pointer);
        }

        int ws_row() { return layout.ws_row.get(memory); }
        int ws_col() { return layout.ws_col.get(memory); }
    }

    /*private*/ static final class termios_C extends struct_C
    {
        /*private*/ static final Bridge<termios_C> BRIDGE = new Bridge<termios_C>()
        {
            protected termios_C wrap(Pointer pointer)
            {
                return new termios_C(pointer);
            }
        };

        /*private*/ static final class Layout extends StructLayout
        {
            /*private*/ Layout(jnr.ffi.Runtime runtime)
            {
                super(runtime);
            }

            final Unsigned32 c_iflag = new Unsigned32();    // unsigned int c_iflag
            final Unsigned32 c_oflag = new Unsigned32();    // unsigned int c_oflag
            final Unsigned32 c_cflag = new Unsigned32();    // unsigned int c_cflag
            final Unsigned32 c_lflag = new Unsigned32();    // unsigned int c_lflag
            final Unsigned8 c_line = new Unsigned8();       // unsigned byte c_line
            final Unsigned8 c_vintr = new Unsigned8();      // unsigned byte c_vintr
            final Unsigned8 c_cc1 = new Unsigned8();        // unsigned byte c_cc[1]
            final Unsigned8 c_verase = new Unsigned8();     // unsigned byte c_verase
            final Unsigned8 c_cc3 = new Unsigned8();        // unsigned byte c_cc[3]
            final Unsigned8 c_cc4 = new Unsigned8();        // unsigned byte c_cc[4]
            final Unsigned8 c_vtime = new Unsigned8();      // unsigned byte c_vtime
            final Unsigned8 c_vmin = new Unsigned8();       // unsigned byte c_vmin
            final Unsigned8 c_cc7 = new Unsigned8();        // unsigned byte c_cc[7]
            final Unsigned8 c_cc8 = new Unsigned8();        // unsigned byte c_cc[8]
            final Unsigned8 c_cc9 = new Unsigned8();        // unsigned byte c_cc[9]
            final Unsigned8 c_cc10 = new Unsigned8();       // unsigned byte c_cc[10]
            final Unsigned8 c_cc11 = new Unsigned8();       // unsigned byte c_cc[11]
            final Unsigned8 c_cc12 = new Unsigned8();       // unsigned byte c_cc[12]
            final Unsigned8 c_cc13 = new Unsigned8();       // unsigned byte c_cc[13]
            final Unsigned8 c_cc14 = new Unsigned8();       // unsigned byte c_cc[14]
            final Unsigned8 c_cc15 = new Unsigned8();       // unsigned byte c_cc[15]
            final Unsigned8 c_cc16 = new Unsigned8();       // unsigned byte c_cc[16]
            final Unsigned8 c_cc17 = new Unsigned8();       // unsigned byte c_cc[17]
            final Unsigned8 c_cc18 = new Unsigned8();       // unsigned byte c_cc[18]
            final Unsigned8 c_cc19 = new Unsigned8();       // unsigned byte c_cc[19]
            final Unsigned8 c_cc20 = new Unsigned8();       // unsigned byte c_cc[20]
            final Unsigned8 c_cc21 = new Unsigned8();       // unsigned byte c_cc[21]
            final Unsigned8 c_cc22 = new Unsigned8();       // unsigned byte c_cc[22]
            final Unsigned8 c_cc23 = new Unsigned8();       // unsigned byte c_cc[23]
            final Unsigned8 c_cc24 = new Unsigned8();       // unsigned byte c_cc[24]
            final Unsigned8 c_cc25 = new Unsigned8();       // unsigned byte c_cc[25]
            final Unsigned8 c_cc26 = new Unsigned8();       // unsigned byte c_cc[26]
            final Unsigned8 c_cc27 = new Unsigned8();       // unsigned byte c_cc[27]
            final Unsigned8 c_cc28 = new Unsigned8();       // unsigned byte c_cc[28]
            final Unsigned8 c_cc29 = new Unsigned8();       // unsigned byte c_cc[29]
            final Unsigned8 c_cc30 = new Unsigned8();       // unsigned byte c_cc[30]
            final Unsigned8 c_cc31 = new Unsigned8();       // unsigned byte c_cc[31]
            final Unsigned32 c_ispeed = new Unsigned32();   // unsigned int c_ispeed
            final Unsigned32 c_ospeed = new Unsigned32();   // unsigned int c_ospeed
        }

        /*private*/ static final Layout layout = new Layout(jnr.ffi.Runtime.getSystemRuntime());

        /*private*/ termios_C()
        {
            super(layout);
        }

        /*private*/ termios_C(Pointer pointer)
        {
            super(pointer);
        }

        long c_iflag()     { return layout.c_iflag.get(memory); }
        void c_iflag(long iflag)  { layout.c_iflag.set(memory, iflag); }
        long c_lflag()     { return layout.c_lflag.get(memory); }
        void c_lflag(long lflag)  { layout.c_lflag.set(memory, lflag); }
        long c_oflag()     { return layout.c_oflag.get(memory); }
        void c_oflag(long oflag)  { layout.c_oflag.set(memory, oflag); }
        short c_verase()   { return layout.c_verase.get(memory); }
        short c_vintr()    { return layout.c_vintr.get(memory); }
        void c_vmin(short vmin)   { layout.c_vmin.set(memory, vmin); }
        void c_vtime(short vtime) { layout.c_vtime.set(memory, vtime); }
    }

    /*private*/ static void COPY_termios(termios_C ti1, termios_C ti0)
    {
        ti1.memory.transferFrom(0, ti0.memory, 0, termios_C.layout.size());
    }

    /*private*/ static final int FD_SET_LENGTH = 1024 / 64;

    /*private*/ static final void FD_ZERO(long[] fds)
    {
        Arrays.fill(fds, 0);
    }

    /*private*/ static final void FD_SET(int fd, long[] fds)
    {
        fds[fd / 64] |= (1L << (fd % 64));
    }

    /*private*/ static final class file_C extends struct_C
    {
        /*private*/ static final Bridge<file_C> BRIDGE = new Bridge<file_C>()
        {
            protected file_C wrap(Pointer pointer)
            {
                return new file_C(pointer);
            }
        };

        /*private*/ static final class Layout extends StructLayout
        {
            /*private*/ Layout(jnr.ffi.Runtime runtime)
            {
                super(runtime);
            }

            final Padding _IO_FILE = new Padding(NativeType.SCHAR, 216);
        }

        /*private*/ static final Layout layout = new Layout(jnr.ffi.Runtime.getSystemRuntime());

        /*private*/ file_C()
        {
            super(layout);
        }

        /*private*/ file_C(long address)
        {
            super(address);
        }

        /*private*/ file_C(Pointer pointer)
        {
            super(pointer);
        }
    }

    public static interface Libc
    {
        int access(ByteBuffer name, int type);
        int atoi(ByteBuffer nptr);
        long atol(ByteBuffer nptr);
        int chdir(ByteBuffer path);
        int chmod(ByteBuffer file, @mode_t int mode);
        int close(int fd);
        int dup(int fd);
        Pointer /*int **/__errno_location(/*void*/);
        void _exit(int status);
        void exit(int status);
        int fchdir(int fd);
        int fchown(int fd, @uid_t int owner, @gid_t int group);
        int fclose(file_C stream);
     // int fcntl(int fd, int cmd, ...);
        int fcntl(int fd, int cmd);
        int fcntl(int fd, int cmd, int arg);
        file_C fdopen(int fd, ByteBuffer modes);
        int fflush(file_C stream);
        Pointer /*byte **/fgets(ByteBuffer s, int n, file_C stream);
        int fileno(file_C stream);
        file_C fopen(ByteBuffer filename, ByteBuffer modes);

     // int fprintf(file_C stream, ByteBuffer format, ...);
     // int fprintf(file_C stream, ByteBuffer format, ByteBuffer... args);

        int fprintf(file_C stream, ByteBuffer format);
        int fprintf(file_C stream, ByteBuffer format, ByteBuffer arg1);
        int fprintf(file_C stream, ByteBuffer format, ByteBuffer arg1, ByteBuffer arg2);

        int fprintf(file_C stream, ByteBuffer format, int arg);

        int fputs(ByteBuffer s, file_C stream);
        @size_t long fread(ByteBuffer /*void **/ptr, @size_t long size, @size_t long n, file_C stream);
        int fsync(int fd);
        @size_t long fwrite(ByteBuffer /*void **/ptr, @size_t long size, @size_t long n, file_C stream);
        int getc(file_C stream);
        Pointer /*byte **/getcwd(ByteBuffer buf, @size_t long size);
        /*Pointer*/String /*byte **/getenv(ByteBuffer name);
        @gid_t int getgid(/*void*/);
        @pid_t int getpid(/*void*/);
        int gettimeofday(timeval_C tv, @timezone_ptr_t Pointer tz);
        @uid_t int getuid(/*void*/);
     // int ioctl(int fd, /*unsigned */long request, ...);
        int ioctl(int fd, /*unsigned */long request, winsize_C ws);
        int isatty(int fd);
        int kill(@pid_t int pid, int sig);
        tm_C localtime(@time_ptr_t long[] timer);
        @off_t long lseek(int fd, @off_t long offset, int whence);
     // int lstat(ByteBuffer file, stat_C buf);
        int __lxstat64(int version, ByteBuffer file, stat_C buf);
        int nanosleep(timespec_C requested_time, timespec_C remaining);
     // int open(ByteBuffer file, int oflag, ...);
        int open(ByteBuffer file, int oflag, int perm);
        int putc(int c, file_C stream);
        @ssize_t long read(int fd, ByteBuffer /*void **/buf, @size_t long nbytes);
        @ssize_t long readlink(ByteBuffer path, ByteBuffer buf, @size_t long len);
        int select(int nfds, @fd_set_ptr_t long[] readfds, @fd_set_ptr_t long[] writefds, @fd_set_ptr_t long[] exceptfds, timeval_C timeout);
        int sigaction(int sig, /*sigaction_C*/Pointer act, /*sigaction_C*/Pointer oact);
        int sigemptyset(@sigset_ptr_t Pointer set);
        Pointer /*void (**/sigset(int sig, Pointer /*void (**/func/*)(int)*/)/*)(int)*/;

     // int sprintf(ByteBuffer s, ByteBuffer format, ...);
     // int sprintf(ByteBuffer s, ByteBuffer format, ByteBuffer... args);

        int sprintf(ByteBuffer s, ByteBuffer format);
        int sprintf(ByteBuffer s, ByteBuffer format, ByteBuffer arg1);
        int sprintf(ByteBuffer s, ByteBuffer format, ByteBuffer arg1, ByteBuffer arg2);
        int sprintf(ByteBuffer s, ByteBuffer format, ByteBuffer arg1, ByteBuffer arg2, ByteBuffer arg3);

        int sprintf(ByteBuffer s, ByteBuffer format, byte arg1);
        int sprintf(ByteBuffer s, ByteBuffer format, byte arg1, byte arg2);
        int sprintf(ByteBuffer s, ByteBuffer format, byte arg1, int arg2);
        int sprintf(ByteBuffer s, ByteBuffer format, byte arg1, int arg2, long arg3, int arg4);
        int sprintf(ByteBuffer s, ByteBuffer format, ByteBuffer arg1, int arg2);
        int sprintf(ByteBuffer s, ByteBuffer format, int arg1);
        int sprintf(ByteBuffer s, ByteBuffer format, int arg1, ByteBuffer arg2);
        int sprintf(ByteBuffer s, ByteBuffer format, int arg1, int arg2);
        int sprintf(ByteBuffer s, ByteBuffer format, int arg1, long arg2);
        int sprintf(ByteBuffer s, ByteBuffer format, int arg1, long arg2, int arg3);
        int sprintf(ByteBuffer s, ByteBuffer format, long arg1);
        int sprintf(ByteBuffer s, ByteBuffer format, long arg1, ByteBuffer arg2);
        int sprintf(ByteBuffer s, ByteBuffer format, long arg1, ByteBuffer arg2, int arg3);
        int sprintf(ByteBuffer s, ByteBuffer format, long arg1, long arg2);

     // int stat(ByteBuffer file, stat_C buf);
        int __xstat64(int version, ByteBuffer file, stat_C buf);
        /*Pointer*/String /*byte **/strerror(int errnum);
        @size_t long strftime(ByteBuffer s, @size_t long maxsize, ByteBuffer format, tm_C tp);
        int tcflush(int fd, int queue_selector);
        int tcgetattr(int fd, termios_C termios_p);
        int tcsetattr(int fd, int optional_actions, termios_C termios_p);
        @time_t long time(@time_ptr_t long[] timer);
        int unlink(ByteBuffer name);
        @ssize_t long write(int fd, ByteBuffer /*void **/buf, @size_t long n);

        Variable<Long> stdin();
        Variable<Long> stdout();
        Variable<Long> stderr();
    }

    /*private*/ static final Libc libc;
    static
    {
        LibraryLoader<Libc> loader = LibraryLoader.create(Libc.class);

        loader.map(stat_C.class, stat_C.BRIDGE);
        loader.map(timeval_C.class, timeval_C.BRIDGE);
        loader.map(timespec_C.class, timespec_C.BRIDGE);
        loader.map(tm_C.class, tm_C.BRIDGE);
        loader.map(winsize_C.class, winsize_C.BRIDGE);
        loader.map(termios_C.class, termios_C.BRIDGE);
        loader.map(file_C.class, file_C.BRIDGE);

        libc = loader.load("c");
    }

    /*private*/ static final class LibC
    {
        int access(Bytes name, int type)
        {
            return libc.access(name.buf(), type);
        }

        int atoi(Bytes nptr)
        {
            return libc.atoi(nptr.buf());
        }

        long atol(Bytes nptr)
        {
            return libc.atol(nptr.buf());
        }

        int chdir(Bytes path)
        {
            return libc.chdir(path.buf());
        }

        int chmod(Bytes file, int mode)
        {
            return libc.chmod(file.buf(), mode);
        }

        int errno()
        {
            return libc.__errno_location().getInt(0);
        }

        file_C fdopen(int fd, Bytes modes)
        {
            return libc.fdopen(fd, modes.buf());
        }

        boolean _fgets(Bytes s, int n, file_C stream)
        {
            return (libc.fgets(s.buf(), n, stream) != null);
        }

        file_C fopen(Bytes filename, Bytes modes)
        {
            return libc.fopen(filename.buf(), modes.buf());
        }

     /* int fprintf(file_C stream, Bytes format, Bytes... args)
        {
            ByteBuffer[] bufs = new ByteBuffer[args.length];

            for (int i = 0; i < bufs.length; i++)
                bufs[i] = args[i].buf();

            return libc.fprintf(stream, format.buf(), bufs);
        } */

        int fprintf(file_C stream, Bytes format)
        {
            return libc.fprintf(stream, format.buf());
        }

        int fprintf(file_C stream, Bytes format, Bytes arg1)
        {
            return libc.fprintf(stream, format.buf(), arg1.buf());
        }

        int fprintf(file_C stream, Bytes format, Bytes arg1, Bytes arg2)
        {
            return libc.fprintf(stream, format.buf(), arg1.buf(), arg2.buf());
        }

        int fprintf(file_C stream, Bytes format, int arg)
        {
            return libc.fprintf(stream, format.buf(), arg);
        }

        int fputs(Bytes s, file_C stream)
        {
            return libc.fputs(s.buf(), stream);
        }

        long fread(Bytes ptr, long size, long n, file_C stream)
        {
            return libc.fread(ptr.buf(), size, n, stream);
        }

        long fwrite(Bytes ptr, long size, long n, file_C stream)
        {
            return libc.fwrite(ptr.buf(), size, n, stream);
        }

        boolean _getcwd(Bytes buf, long size)
        {
            return (libc.getcwd(buf.buf(), size) != null);
        }

        Bytes getenv(Bytes name)
        {
            return u8(libc.getenv(name.buf()));
        }

        int _gettimeofday(timeval_C tv)
        {
            return libc.gettimeofday(tv, null);
        }

        tm_C _localtime(long seconds)
        {
            long[] timer = { seconds };
            return libc.localtime(timer);
        }

        int lstat(Bytes file, stat_C buf)
        {
            return libc.__lxstat64(stat_C.VERSION, file.buf(), buf);
        }

        int open(Bytes file, int oflag, int perm)
        {
            return libc.open(file.buf(), oflag, perm);
        }

        long read(int fd, Bytes buf, long nbytes)
        {
            return libc.read(fd, buf.buf(), nbytes);
        }

        long readlink(Bytes path, Bytes buf, long len)
        {
            return libc.readlink(path.buf(), buf.buf(), len);
        }

        /*int*/void sigaction(int _sig, Pointer _act, Pointer _oact)
        {
            // %% not yet, maybe nor ever
        }

        /*int*/void sigemptyset(Pointer _set)
        {
            // %% not yet, maybe nor ever
        }

        /*Pointer*/void sigset(int _sig, Pointer _func)
        {
            // %% not yet, maybe nor ever
        }

     /* int sprintf(Bytes s, Bytes format, Bytes... args)
        {
            ByteBuffer[] bufs = new ByteBuffer[args.length];

            for (int i = 0; i < bufs.length; i++)
                bufs[i] = args[i].buf();

            return libc.sprintf(s.buf(), format.buf(), bufs);
        } */

        int sprintf(Bytes s, Bytes format)
        {
            return libc.sprintf(s.buf(), format.buf());
        }

        int sprintf(Bytes s, Bytes format, Bytes arg1)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1.buf());
        }

        int sprintf(Bytes s, Bytes format, Bytes arg1, Bytes arg2)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1.buf(), arg2.buf());
        }

        int sprintf(Bytes s, Bytes format, Bytes arg1, Bytes arg2, Bytes arg3)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1.buf(), arg2.buf(), arg3.buf());
        }

        int sprintf(Bytes s, Bytes format, byte arg1)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1);
        }

        int sprintf(Bytes s, Bytes format, byte arg1, byte arg2)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1, arg2);
        }

        int sprintf(Bytes s, Bytes format, byte arg1, int arg2)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1, arg2);
        }

        int sprintf(Bytes s, Bytes format, byte arg1, int arg2, long arg3, int arg4)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1, arg2, arg3, arg4);
        }

        int sprintf(Bytes s, Bytes format, Bytes arg1, int arg2)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1.buf(), arg2);
        }

        int sprintf(Bytes s, Bytes format, int arg1)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1);
        }

        int sprintf(Bytes s, Bytes format, int arg1, Bytes arg2)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1, arg2.buf());
        }

        int sprintf(Bytes s, Bytes format, int arg1, int arg2)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1, arg2);
        }

        int sprintf(Bytes s, Bytes format, int arg1, long arg2)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1, arg2);
        }

        int sprintf(Bytes s, Bytes format, int arg1, long arg2, int arg3)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1, arg2, arg3);
        }

        int sprintf(Bytes s, Bytes format, long arg1)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1);
        }

        int sprintf(Bytes s, Bytes format, long arg1, Bytes arg2)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1, arg2.buf());
        }

        int sprintf(Bytes s, Bytes format, long arg1, Bytes arg2, int arg3)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1, arg2.buf(), arg3);
        }

        int sprintf(Bytes s, Bytes format, long arg1, long arg2)
        {
            return libc.sprintf(s.buf(), format.buf(), arg1, arg2);
        }

        int stat(Bytes file, stat_C buf)
        {
            return libc.__xstat64(stat_C.VERSION, file.buf(), buf);
        }

        Bytes strerror(int errnum)
        {
            return u8(libc.strerror(errnum));
        }

        long strftime(Bytes s, long maxsize, Bytes format, tm_C tp)
        {
            return libc.strftime(s.buf(), maxsize, format.buf(), tp);
        }

        long _time()
        {
            return libc.time(null);
        }

        int unlink(Bytes name)
        {
            return libc.unlink(name.buf());
        }

        long write(int fd, Bytes buf, long n)
        {
            return libc.write(fd, buf.buf(), n);
        }

        file_C stdin()  { return new file_C(libc.stdin().get()); }
        file_C stdout() { return new file_C(libc.stdout().get()); }
        file_C stderr() { return new file_C(libc.stderr().get()); }
    }

    /*private*/ static final LibC libC = new LibC();

    /*private*/ static final file_C stdin = libC.stdin(), stdout = libC.stdout(), stderr = libC.stderr();
}
