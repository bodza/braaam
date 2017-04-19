# Makefile for Vim on Unix and Unix-like systems	vim:ts=8:sw=8:tw=78
#
# 1. Edit this Makefile  {{{1
#	- If you want a version of Vim that is small and starts up quickly,
#	  you might want to disable the GUI, X11, Perl, Python and Tcl.
#	- Uncomment the line with --disable-gui if you have Motif, GTK and/or
#	  Athena but don't want to make gvim (the GUI version of Vim with nice
#	  menus and scrollbars, but makes Vim bigger and startup slower).
#	- Uncomment --disable-darwin if on Mac OS X but you want to compile a
#	  Unix version.
#	- Uncomment the line "CONF_OPT_X = --without-x" if you have X11 but
#	  want to disable using X11 libraries.	This speeds up starting Vim,
#	  but the window title will not be set and the X11 selection can not
#	  be used.
#	- Uncomment one or more of these lines to include an interface;
#	  each makes Vim quite a bit bigger:
#		--enable-cscope		for Cscope interface
#	- Uncomment one of the lines with --with-features= to enable a set of
#	  features (but not the interfaces just mentioned).
#	- Uncomment the line with --disable-acl to disable ACL support even
#	  though your system supports it.
#	- Uncomment the line with --disable-gpm to disable gpm support
#	  even though you have gpm libraries and includes.
#	- Uncomment the line with --disable-sysmouse to disable sysmouse
#	  support even though you have /dev/sysmouse and includes.
#	- Uncomment one of the lines with CFLAGS and/or CC if you have
#	  something very special or want to tune the optimizer.
#	- Search for the name of your system to see if it needs anything
#	  special.
#	- A few versions of make use '.include "file"' instead of 'include
#	  file'.  Adjust the include line below if yours does.
#
# 2. Edit feature.h  {{{1
#	Only if you do not agree with the default compile features, e.g.:
#	- you want Vim to be as vi compatible as it can be
#	- you want right-to-left editing (Hebrew)
#	- you want 'langmap' support (Greek)
#	- you want to remove features to make Vim smaller
#
# 3. "make"  {{{1
#	Will first run ./configure with the options in this file. Then it will
#	start make again on this Makefile to do the compiling. You can also do
#	this in two steps with:
#		make config
#		make
#	The configuration phase creates/overwrites auto/config.h and
#	auto/config.mk.
#	The configure script is created with "make autoconf".  It can detect
#	different features of your system and act accordingly.  However, it is
#	not correct for all systems.  Check this:
#	- If you have X windows, but configure could not find it or reported
#	  another include/library directory then you wanted to use, you have
#	  to set CONF_OPT_X below.  You might also check the installation of
#	  xmkmf.
#	- If you have --enable-gui=motif and have Motif on your system, but
#	  configure reports "checking for location of gui... <not found>", you
#	  have to set GUI_INC_LOC and GUI_LIB_LOC below.
#	If you changed something, do this to run configure again:
#		make reconfig
#
#	- If you do not trust the automatic configuration code, then inspect
#	  auto/config.h and auto/config.mk, before starting the actual build
#	  phase. If possible edit this Makefile, rather than auto/config.mk --
#	  especially look at the definition of VIMLOC below. Note that the
#	  configure phase overwrites auto/config.mk and auto/config.h again.
#	- If you get error messages, find out what is wrong and try to correct
#	  it in this Makefile. You may need to do "make reconfig" when you
#	  change anything that configure uses (e.g. switching from an old C
#	  compiler to an ANSI C compiler). Only when auto/configure does
#	  something wrong you may need to change one of the other files. If
#	  you find a clean way to fix the problem, consider sending a note to
#	  the author of autoconf (bug-gnu-utils@prep.ai.mit.edu) or Vim
#	  (Bram@vim.org). Don't bother to do that when you made a hack
#	  solution for a non-standard system.
# }}}

#DO NOT CHANGE the next line, we need it for configure to find the compiler
#instead of using the default from the "make" program.
#Use a line further down to change the value for CC.
CC=

######################## auto/config.mk ######################## {{{1
# At this position auto/config.mk is included. When starting from the
# toplevel Makefile it is almost empty. After running auto/configure it
# contains settings that have been discovered for your system. Settings below
# this include override settings in auto/config.mk!

# Note: If make fails because auto/config.mk does not exist (it is not
# included in the repository), do:
#    cp config.mk.dist auto/config.mk

# (X) How to include auto/config.mk depends on the version of "make" you have,
#     if the current choice doesn't work, try the other one.

include auto/config.mk
CClink = $(CC)

#}}}

# Include the configuration choices first, so we can override everything
# below. As shipped, this file contains a target that causes to run
# configure. Once configure was run, this file contains a list of
# make variables with predefined values instead. Thus any second invocation
# of make, will build Vim.

# CONFIGURE - configure arguments {{{1

# CSCOPE
# Uncomment this when you want to include the Cscope interface.
#CONF_OPT_CSCOPE = --enable-cscope

# MULTIBYTE - To edit multi-byte characters.
# Uncomment this when you want to edit a multibyte language.
# It's automatically enabled with normal features, GTK or IME support.
# Note: Compile on a machine where setlocale() actually works, otherwise the
# configure tests may fail.
#CONF_OPT_MULTIBYTE = --enable-multibyte

# NLS - National Language Support
# Uncomment this when you do not want to support translated messages, even
# though configure can find support for it.
#CONF_OPT_NLS = --disable-nls

# gpm - For mouse support on Linux console via gpm
# Uncomment this when you do not want to include gpm support, even
# though you have gpm libraries and includes.
#CONF_OPT_GPM = --disable-gpm

# sysmouse - For mouse support on FreeBSD and DragonFly console via sysmouse
# Uncomment this when you do not want do include sysmouse support, even
# though you have /dev/sysmouse and includes.
#CONF_OPT_SYSMOUSE = --disable-sysmouse

# FEATURES - For creating Vim with more or less features
# Uncomment one of these lines when you want to include few to many features.
# The default is "normal".
#CONF_OPT_FEAT = --with-features=tiny
#CONF_OPT_FEAT = --with-features=small
#CONF_OPT_FEAT = --with-features=normal
#CONF_OPT_FEAT = --with-features=big
#CONF_OPT_FEAT = --with-features=huge

# Autoconf binary.
AUTOCONF = autoconf

# }}}

### Names of the programs and targets  {{{1
VIMTARGET	= $(VIMNAME)$(EXEEXT)
EXTARGET	= $(EXNAME)$(LNKEXT)
VIEWTARGET	= $(VIEWNAME)$(LNKEXT)
GVIMNAME	= g$(VIMNAME)
GVIMTARGET	= $(GVIMNAME)$(LNKEXT)
GVIEWNAME	= g$(VIEWNAME)
GVIEWTARGET	= $(GVIEWNAME)$(LNKEXT)
RVIMNAME	= r$(VIMNAME)
RVIMTARGET	= $(RVIMNAME)$(LNKEXT)
RVIEWNAME	= r$(VIEWNAME)
RVIEWTARGET	= $(RVIEWNAME)$(LNKEXT)
RGVIMNAME	= r$(GVIMNAME)
RGVIMTARGET	= $(RGVIMNAME)$(LNKEXT)
RGVIEWNAME	= r$(GVIEWNAME)
RGVIEWTARGET	= $(RGVIEWNAME)$(LNKEXT)
VIMDIFFNAME	= $(VIMNAME)diff
GVIMDIFFNAME	= g$(VIMDIFFNAME)
VIMDIFFTARGET	= $(VIMDIFFNAME)$(LNKEXT)
GVIMDIFFTARGET	= $(GVIMDIFFNAME)$(LNKEXT)
EVIMNAME	= e$(VIMNAME)
EVIMTARGET	= $(EVIMNAME)$(LNKEXT)
EVIEWNAME	= e$(VIEWNAME)
EVIEWTARGET	= $(EVIEWNAME)$(LNKEXT)

### Names of the tools that are also made  {{{1
TOOLS =

### Directory of the man pages
MAN1DIR = /man1

### Vim version (adjusted by a script)
VIMMAJOR = 7
VIMMINOR = 4

### Location of Vim files (should not need to be changed, and  {{{1
### some things might not work when they are changed!)
VIMDIR = /vim
VIMRTDIR = /vim$(VIMMAJOR)$(VIMMINOR)
HELPSUBDIR = /doc
COLSUBDIR = /colors
SYNSUBDIR = /syntax
INDSUBDIR = /indent
AUTOSUBDIR = /autoload
PLUGSUBDIR = /plugin
FTPLUGSUBDIR = /ftplugin
LANGSUBDIR = /lang
COMPSUBDIR = /compiler
KMAPSUBDIR = /keymap
MACROSUBDIR = /macros
TOOLSSUBDIR = /tools
TUTORSUBDIR = /tutor
SPELLSUBDIR = /spell
PRINTSUBDIR = /print

### VIMLOC	common root of the Vim files (all versions)
### VIMRTLOC	common root of the runtime Vim files (this version)
### VIMRCLOC	compiled-in location for global [g]vimrc files (all versions)
### VIMRUNTIMEDIR  compiled-in location for runtime files (optional)
### HELPSUBLOC	location for help files
### COLSUBLOC	location for colorscheme files
### SYNSUBLOC	location for syntax files
### INDSUBLOC	location for indent files
### AUTOSUBLOC	location for standard autoload files
### PLUGSUBLOC	location for standard plugin files
### FTPLUGSUBLOC  location for ftplugin files
### LANGSUBLOC	location for language files
### COMPSUBLOC	location for compiler files
### KMAPSUBLOC	location for keymap files
### MACROSUBLOC	location for macro files
### TOOLSSUBLOC	location for tools files
### TUTORSUBLOC	location for tutor files
### SPELLSUBLOC	location for spell files
### PRINTSUBLOC	location for PostScript files (prolog, latin1, ..)
### SCRIPTLOC	location for script files (menu.vim, bugreport.vim, ..)
### You can override these if you want to install them somewhere else.
### Edit feature.h for compile-time settings.
VIMLOC		= $(DATADIR)$(VIMDIR)
VIMRTLOC	= $(DATADIR)$(VIMDIR)$(VIMRTDIR)
VIMRCLOC	= $(VIMLOC)
HELPSUBLOC	= $(VIMRTLOC)$(HELPSUBDIR)
COLSUBLOC	= $(VIMRTLOC)$(COLSUBDIR)
SYNSUBLOC	= $(VIMRTLOC)$(SYNSUBDIR)
INDSUBLOC	= $(VIMRTLOC)$(INDSUBDIR)
AUTOSUBLOC	= $(VIMRTLOC)$(AUTOSUBDIR)
PLUGSUBLOC	= $(VIMRTLOC)$(PLUGSUBDIR)
FTPLUGSUBLOC	= $(VIMRTLOC)$(FTPLUGSUBDIR)
LANGSUBLOC	= $(VIMRTLOC)$(LANGSUBDIR)
COMPSUBLOC	= $(VIMRTLOC)$(COMPSUBDIR)
KMAPSUBLOC	= $(VIMRTLOC)$(KMAPSUBDIR)
MACROSUBLOC	= $(VIMRTLOC)$(MACROSUBDIR)
TOOLSSUBLOC	= $(VIMRTLOC)$(TOOLSSUBDIR)
TUTORSUBLOC	= $(VIMRTLOC)$(TUTORSUBDIR)
SPELLSUBLOC	= $(VIMRTLOC)$(SPELLSUBDIR)
PRINTSUBLOC	= $(VIMRTLOC)$(PRINTSUBDIR)
SCRIPTLOC	= $(VIMRTLOC)

# Program to install the program in the target directory.  Could also be "mv".
INSTALL_PROG	= cp

# Program to install the data in the target directory.	Cannot be "mv"!
INSTALL_DATA	= cp
INSTALL_DATA_R	= cp -r

### Permissions for binaries  {{{1
BINMOD = 755

### Permissions for man page
MANMOD = 644

### Permissions for help files
HELPMOD = 644

### Permissions for Perl and shell scripts
SCRIPTMOD = 755

### Permission for Vim script files (menu.vim, bugreport.vim, ..)
VIMSCRIPTMOD = 644

### Permissions for all directories that are created
DIRMOD = 755

### Permissions for all other files that are created
FILEMOD = 644

# Where to copy the man and help files from
HELPSOURCE = ../runtime/doc

# Where to copy the script files from (menu, bugreport)
SCRIPTSOURCE = ../runtime

# Where to copy the colorscheme files from
COLSOURCE = ../runtime/colors

# Where to copy the syntax files from
SYNSOURCE = ../runtime/syntax

# Where to copy the indent files from
INDSOURCE = ../runtime/indent

# Where to copy the standard plugin files from
AUTOSOURCE = ../runtime/autoload

# Where to copy the standard plugin files from
PLUGSOURCE = ../runtime/plugin

# Where to copy the ftplugin files from
FTPLUGSOURCE = ../runtime/ftplugin

# Where to copy the macro files from
MACROSOURCE = ../runtime/macros

# Where to copy the tools files from
TOOLSSOURCE = ../runtime/tools

# Where to copy the tutor files from
TUTORSOURCE = ../runtime/tutor

# Where to copy the spell files from
SPELLSOURCE = ../runtime/spell

# Where to look for language specific files
LANGSOURCE = ../runtime/lang

# Where to look for compiler files
COMPSOURCE = ../runtime/compiler

# Where to look for keymap files
KMAPSOURCE = ../runtime/keymap

# Where to look for print resource files
PRINTSOURCE = ../runtime/print

# Without a GUI install the normal way.
NONE_INSTALL = install_normal

# }}}

################################################
##   no changes required below this line      ##
################################################

SHELL = /bin/sh

.SUFFIXES:
.SUFFIXES: .c .o .pro

PRE_DEFS = -Iproto $(DEFS) $(CPPFLAGS)

ALL_CFLAGS = $(PRE_DEFS) $(CFLAGS)

OSDEF_CFLAGS = $(PRE_DEFS)

ALL_LIBS = $(LIBS) $(EXTRA_LIBS)

#	     BASIC_SRC: files that are always used
#
#		   SRC: files used for current configuration

BASIC_SRC = \
	blowfish.c \
	buffer.c \
	charset.c \
	crypt.c \
	crypt_zip.c \
	diff.c \
	digraph.c \
	edit.c \
	eval.c \
	ex_cmds.c \
	ex_cmds2.c \
	ex_docmd.c \
	ex_eval.c \
	ex_getln.c \
	fileio.c \
	fold.c \
	getchar.c \
	hardcopy.c \
	hashtab.c \
	if_cscope.c \
	if_xcmdsrv.c \
	main.c \
	mark.c \
	memfile.c \
	memline.c \
	menu.c \
	message.c \
	misc1.c \
	misc2.c \
	move.c \
	mbyte.c \
	normal.c \
	ops.c \
	option.c \
	os_unix.c \
	auto/pathdef.c \
	popupmnu.c \
	quickfix.c \
	regexp.c \
	screen.c \
	search.c \
	sha256.c \
	spell.c \
	syntax.c \
	tag.c \
	term.c \
	ui.c \
	undo.c \
	version.c \
	window.c

SRC =	$(BASIC_SRC)

OBJ_COMMON = \
	objects/buffer.o \
	objects/blowfish.o \
	objects/charset.o \
	objects/crypt.o \
	objects/crypt_zip.o \
	objects/diff.o \
	objects/digraph.o \
	objects/edit.o \
	objects/eval.o \
	objects/ex_cmds.o \
	objects/ex_cmds2.o \
	objects/ex_docmd.o \
	objects/ex_eval.o \
	objects/ex_getln.o \
	objects/fileio.o \
	objects/fold.o \
	objects/getchar.o \
	objects/hardcopy.o \
	objects/hashtab.o \
	objects/if_cscope.o \
	objects/if_xcmdsrv.o \
	objects/mark.o \
        objects/memline.o \
	objects/menu.o \
	objects/message.o \
	objects/misc1.o \
	objects/misc2.o \
	objects/move.o \
	objects/mbyte.o \
	objects/normal.o \
	objects/ops.o \
	objects/option.o \
	objects/os_unix.o \
	objects/pathdef.o \
	objects/popupmnu.o \
	objects/quickfix.o \
	objects/regexp.o \
	objects/screen.o \
	objects/search.o \
	objects/sha256.o \
	objects/spell.o \
	objects/syntax.o \
	objects/tag.o \
	objects/term.o \
	objects/ui.o \
	objects/undo.o \
	objects/version.o \
	objects/window.o

OBJ = $(OBJ_COMMON) \
	objects/main.o \
	objects/memfile.o

# Default target is making the executable and tools
all: $(VIMTARGET)

tools: $(TOOLS)

# Run configure with all the setting from above.
#
# Note: auto/config.h doesn't depend on configure, because running configure
# doesn't always update auto/config.h.  The timestamp isn't changed if the
# file contents didn't change (to avoid recompiling everything).  Including a
# dependency on auto/config.h would cause running configure each time when
# auto/config.h isn't updated.  The dependency on auto/config.mk should make
# sure configure is run when it's needed.
#
# Remove the config.cache every time, once in a while it causes problems that
# are very hard to figure out.
#
config auto/config.mk: auto/configure config.mk.in config.h.in
	-rm -f auto/config.cache
	if test "X$(MAKECMDGOALS)" != "Xclean" \
		-a "X$(MAKECMDGOALS)" != "Xdistclean" \
		-a "X$(MAKECMDGOALS)" != "Xautoconf" \
		-a "X$(MAKECMDGOALS)" != "Xreconfig"; then \
	    GUI_INC_LOC="$(GUI_INC_LOC)" GUI_LIB_LOC="$(GUI_LIB_LOC)" \
		CC="$(CC)" CPPFLAGS="$(CPPFLAGS)" CFLAGS="$(CFLAGS)" \
		LDFLAGS="$(LDFLAGS)" $(CONF_SHELL) srcdir="$(srcdir)" \
		./configure $(CONF_OPT_GUI) $(CONF_OPT_X) \
		$(CONF_OPT_FAIL) \
		$(CONF_OPT_NLS) \
		$(CONF_OPT_CSCOPE) $(CONF_OPT_MULTIBYTE) $(CONF_OPT_INPUT) \
		$(CONF_OPT_OUTPUT) $(CONF_OPT_GPM) \
		$(CONF_OPT_FEAT) $(CONF_TERM_LIB) \
		$(CONF_OPT_COMPBY) $(CONF_OPT_ACL) \
		$(CONF_ARGS) $(CONF_OPT_PLTHOME) \
		$(CONF_OPT_SYSMOUSE); \
	fi

# Use "make reconfig" to rerun configure without cached values.
# When config.h changes, most things will be recompiled automatically.
# Invoke $(MAKE) to run config with the empty auto/config.mk.
# Invoke $(MAKE) to build all with the filled auto/config.mk.
reconfig: scratch clean
	$(MAKE) -f Makefile config
	$(MAKE) -f Makefile all

# Run autoconf to produce auto/configure.
# Note:
# - DO NOT RUN autoconf MANUALLY!  It will overwrite ./configure instead of
#   producing auto/configure.
# - autoconf is not run automatically, because a patch usually changes both
#   configure.in and auto/configure but can't update the timestamps.  People
#   who do not have (the correct version of) autoconf would run into trouble.
#
# Two tricks are required to make autoconf put its output in the "auto" dir:
# - Temporarily move the ./configure script to ./configure.save.  Don't
#   overwrite it, it's probably the result of an aborted autoconf.
# - Use sed to change ./config.log to auto/config.log in the configure script.
# Autoconf 2.5x (2.59 at least) produces a few more files that we need to take
# care of:
# - configure.lineno: has the line numbers replaced with $LINENO.  That
#   improves patches a LOT, thus use it instead (until someone says it doesn't
#   work on some system).
# - autom4te.cache directory is created and not cleaned up.  Delete it.
# - Uses ">config.log" instead of "./config.log".
autoconf:
	if test ! -f configure.save; then mv configure configure.save; fi
	$(AUTOCONF)
	sed -e 's+>config.log+>auto/config.log+' -e 's+\./config.log+auto/config.log+' configure > auto/configure
	chmod 755 auto/configure
	mv -f configure.save configure
	-rm -rf autom4te.cache
	-rm -f auto/config.status auto/config.cache

# Re-execute this Makefile to include the new auto/config.mk produced by
# configure Only used when typing "make" with a fresh auto/config.mk.
myself:
	$(MAKE) -f Makefile all


# The normal command to compile a .c file to its .o file.
CCC = $(CC) -c -I$(srcdir) $(ALL_CFLAGS)


# Link the target for normal use or debugging.
# A shell script is used to try linking without unnecessary libraries.
$(VIMTARGET): auto/config.mk objects $(OBJ) version.c version.h
	$(CCC) version.c -o objects/version.o
	@LINK="$(PURIFY) $(SHRPENV) $(CClink) $(LDFLAGS) \
		-o $(VIMTARGET) $(OBJ) $(ALL_LIBS)" \
		MAKE="$(MAKE)" LINK_AS_NEEDED=$(LINK_AS_NEEDED) \
		sh $(srcdir)/link.sh

# Clean up all the files that have been produced, except configure's.
# We support common typing mistakes for Juergen! :-)
clean:
	-rm -f *.o objects/* core $(VIMTARGET).core $(VIMTARGET) vim
	-rm -f $(TOOLS) auto/osdef.h auto/pathdef.c
	-rm -f conftest* *~ auto/link.sed
	-rmdir objects

# Update the synmenu.vim file with the latest Syntax menu.
# This is only needed when runtime/makemenu.vim was changed.
menu: ./vim ../runtime/makemenu.vim
	./vim -u ../runtime/makemenu.vim

# Start configure from scratch
scrub scratch:
	-rm -f auto/config.status auto/config.cache config.log auto/config.log
	-rm -f auto/config.h auto/link.log auto/link.sed auto/config.mk
	touch auto/config.h
	cp config.mk.dist auto/config.mk

distclean: clean scratch

###########################################################################

.c.o:
	$(CCC) $<

auto/osdef.h: auto/config.h osdef.sh osdef1.h.in osdef2.h.in
	CC="$(CC) $(OSDEF_CFLAGS)" srcdir=$(srcdir) sh $(srcdir)/osdef.sh

auto/pathdef.c: Makefile auto/config.mk
	-@echo creating $@
	-@echo '/* pathdef.c */' > $@
	-@echo '/* This file is automatically created by Makefile' >> $@
	-@echo ' * DO NOT EDIT!  Change Makefile only. */' >> $@
	-@echo '#include "vim.h"' >> $@
	-@echo 'char_u *default_vim_dir = (char_u *)"$(VIMRCLOC)";' | $(QUOTESED) >> $@
	-@echo 'char_u *default_vimruntime_dir = (char_u *)"$(VIMRUNTIMEDIR)";' | $(QUOTESED) >> $@
	-@echo 'char_u *all_cflags = (char_u *)"$(CC) -c -I$(srcdir) $(ALL_CFLAGS)";' | $(QUOTESED) >>  $@
	-@echo 'char_u *all_lflags = (char_u *)"$(CC) $(LDFLAGS) -o $(VIMTARGET) $(ALL_LIBS) ";' | $(QUOTESED) >>  $@
	-@echo 'char_u *compiled_user = (char_u *)"' | tr -d $(NL) >> $@
	-@if test -n "$(COMPILEDBY)"; then \
		echo "$(COMPILEDBY)" | tr -d $(NL) >> $@; \
		else ((logname) 2>/dev/null || whoami) | tr -d $(NL) >> $@; fi
	-@echo '";' >> $@
	-@echo 'char_u *compiled_sys = (char_u *)"' | tr -d $(NL) >> $@
	-@if test -z "$(COMPILEDBY)"; then hostname | tr -d $(NL) >> $@; fi
	-@echo '";' >> $@
	-@sh $(srcdir)/pathdef.sh

# All the object files are put in the "objects" directory.  Since not all make
# commands understand putting object files in another directory, it must be
# specified for each file separately.

objects:
	mkdir objects

objects/blowfish.o: blowfish.c
	$(CCC) -o $@ blowfish.c

objects/buffer.o: buffer.c
	$(CCC) -o $@ buffer.c

objects/charset.o: charset.c
	$(CCC) -o $@ charset.c

objects/crypt.o: crypt.c
	$(CCC) -o $@ crypt.c

objects/crypt_zip.o: crypt_zip.c
	$(CCC) -o $@ crypt_zip.c

objects/diff.o: diff.c
	$(CCC) -o $@ diff.c

objects/digraph.o: digraph.c
	$(CCC) -o $@ digraph.c

objects/edit.o: edit.c
	$(CCC) -o $@ edit.c

objects/eval.o: eval.c
	$(CCC) -o $@ eval.c

objects/ex_cmds.o: ex_cmds.c
	$(CCC) -o $@ ex_cmds.c

objects/ex_cmds2.o: ex_cmds2.c
	$(CCC) -o $@ ex_cmds2.c

objects/ex_docmd.o: ex_docmd.c
	$(CCC) -o $@ ex_docmd.c

objects/ex_eval.o: ex_eval.c
	$(CCC) -o $@ ex_eval.c

objects/ex_getln.o: ex_getln.c
	$(CCC) -o $@ ex_getln.c

objects/fileio.o: fileio.c
	$(CCC) -o $@ fileio.c

objects/fold.o: fold.c
	$(CCC) -o $@ fold.c

objects/getchar.o: getchar.c
	$(CCC) -o $@ getchar.c

objects/hardcopy.o: hardcopy.c
	$(CCC) -o $@ hardcopy.c

objects/hashtab.o: hashtab.c
	$(CCC) -o $@ hashtab.c

objects/if_cscope.o: if_cscope.c
	$(CCC) -o $@ if_cscope.c

objects/if_xcmdsrv.o: if_xcmdsrv.c
	$(CCC) -o $@ if_xcmdsrv.c

objects/main.o: main.c
	$(CCC) -o $@ main.c

objects/mark.o: mark.c
	$(CCC) -o $@ mark.c

objects/memfile.o: memfile.c
	$(CCC) -o $@ memfile.c

objects/memline.o: memline.c
	$(CCC) -o $@ memline.c

objects/menu.o: menu.c
	$(CCC) -o $@ menu.c

objects/message.o: message.c
	$(CCC) -o $@ message.c

objects/misc1.o: misc1.c
	$(CCC) -o $@ misc1.c

objects/misc2.o: misc2.c
	$(CCC) -o $@ misc2.c

objects/move.o: move.c
	$(CCC) -o $@ move.c

objects/mbyte.o: mbyte.c
	$(CCC) -o $@ mbyte.c

objects/normal.o: normal.c
	$(CCC) -o $@ normal.c

objects/ops.o: ops.c
	$(CCC) -o $@ ops.c

objects/option.o: option.c
	$(CCC) -o $@ option.c

objects/os_unix.o: os_unix.c
	$(CCC) -o $@ os_unix.c

objects/pathdef.o: auto/pathdef.c
	$(CCC) -o $@ auto/pathdef.c

objects/pty.o: pty.c
	$(CCC) -o $@ pty.c

objects/popupmnu.o: popupmnu.c
	$(CCC) -o $@ popupmnu.c

objects/quickfix.o: quickfix.c
	$(CCC) -o $@ quickfix.c

objects/regexp.o: regexp.c regexp_nfa.c
	$(CCC) -o $@ regexp.c

objects/screen.o: screen.c
	$(CCC) -o $@ screen.c

objects/search.o: search.c
	$(CCC) -o $@ search.c

objects/sha256.o: sha256.c
	$(CCC) -o $@ sha256.c

objects/spell.o: spell.c
	$(CCC) -o $@ spell.c

objects/syntax.o: syntax.c
	$(CCC) -o $@ syntax.c

objects/tag.o: tag.c
	$(CCC) -o $@ tag.c

objects/term.o: term.c
	$(CCC) -o $@ term.c

objects/ui.o: ui.c
	$(CCC) -o $@ ui.c

objects/undo.o: undo.c
	$(CCC) -o $@ undo.c

objects/window.o: window.c
	$(CCC) -o $@ window.c

Makefile:
	@echo The name of the makefile MUST be "Makefile" (with capital M)!!!!

###############################################################################
### (automatically generated by 'make depend')
### Dependencies:
objects/blowfish.o: blowfish.c vim.h auto/config.h feature.h os_unix.h \
 auto/osdef.h ascii.h keymap.h term.h macros.h option.h structs.h \
 regexp.h gui.h gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h \
 globals.h
objects/buffer.o: buffer.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h version.h
objects/charset.o: charset.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/crypt.o: crypt.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/crypt_zip.o: crypt_zip.c vim.h auto/config.h feature.h os_unix.h \
 auto/osdef.h ascii.h keymap.h term.h macros.h option.h structs.h \
 regexp.h gui.h gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h \
 globals.h
objects/diff.o: diff.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/digraph.o: digraph.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/edit.o: edit.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/eval.o: eval.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h version.h
objects/ex_cmds.o: ex_cmds.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h version.h
objects/ex_cmds2.o: ex_cmds2.c vim.h auto/config.h feature.h os_unix.h \
 auto/osdef.h ascii.h keymap.h term.h macros.h option.h structs.h \
 regexp.h gui.h gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h \
 globals.h version.h
objects/ex_docmd.o: ex_docmd.c vim.h auto/config.h feature.h os_unix.h \
 auto/osdef.h ascii.h keymap.h term.h macros.h option.h structs.h \
 regexp.h gui.h gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h \
 globals.h
objects/ex_eval.o: ex_eval.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/ex_getln.o: ex_getln.c vim.h auto/config.h feature.h os_unix.h \
 auto/osdef.h ascii.h keymap.h term.h macros.h option.h structs.h \
 regexp.h gui.h gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h \
 globals.h
objects/fileio.o: fileio.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/fold.o: fold.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/getchar.o: getchar.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/hardcopy.o: hardcopy.c vim.h auto/config.h feature.h os_unix.h \
 auto/osdef.h ascii.h keymap.h term.h macros.h option.h structs.h \
 regexp.h gui.h gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h \
 globals.h version.h
objects/hashtab.o: hashtab.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/if_cscope.o: if_cscope.c vim.h auto/config.h feature.h os_unix.h \
 auto/osdef.h ascii.h keymap.h term.h macros.h option.h structs.h \
 regexp.h gui.h gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h \
 globals.h if_cscope.h
objects/if_xcmdsrv.o: if_xcmdsrv.c vim.h auto/config.h feature.h os_unix.h \
 auto/osdef.h ascii.h keymap.h term.h macros.h option.h structs.h \
 regexp.h gui.h gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h \
 globals.h version.h
objects/main.o: main.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/mark.o: mark.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/memfile.o: memfile.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/memline.o: memline.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/menu.o: menu.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/message.o: message.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/misc1.o: misc1.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h version.h
objects/misc2.o: misc2.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/move.o: move.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/mbyte.o: mbyte.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/normal.o: normal.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/ops.o: ops.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h ascii.h \
 keymap.h term.h macros.h option.h structs.h regexp.h gui.h gui_beval.h \
 proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/option.o: option.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/os_unix.o: os_unix.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h os_unixx.h
objects/pathdef.o: auto/pathdef.c vim.h auto/config.h feature.h os_unix.h \
 auto/osdef.h ascii.h keymap.h term.h macros.h option.h structs.h \
 regexp.h gui.h gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h \
 globals.h
objects/popupmnu.o: popupmnu.c vim.h auto/config.h feature.h os_unix.h \
 auto/osdef.h ascii.h keymap.h term.h macros.h option.h structs.h \
 regexp.h gui.h gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h \
 globals.h
objects/quickfix.o: quickfix.c vim.h auto/config.h feature.h os_unix.h \
 auto/osdef.h ascii.h keymap.h term.h macros.h option.h structs.h \
 regexp.h gui.h gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h \
 globals.h
objects/regexp.o: regexp.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h regexp_nfa.c
objects/screen.o: screen.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/search.o: search.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/sha256.o: sha256.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/spell.o: spell.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/syntax.o: syntax.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/tag.o: tag.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h ascii.h \
 keymap.h term.h macros.h option.h structs.h regexp.h gui.h gui_beval.h \
 proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/term.o: term.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/ui.o: ui.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h ascii.h \
 keymap.h term.h macros.h option.h structs.h regexp.h gui.h gui_beval.h \
 proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/undo.o: undo.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
objects/version.o: version.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h version.h
objects/window.o: window.c vim.h auto/config.h feature.h os_unix.h auto/osdef.h \
 ascii.h keymap.h term.h macros.h option.h structs.h regexp.h gui.h \
 gui_beval.h proto/gui_beval.pro ex_cmds.h proto.h globals.h
