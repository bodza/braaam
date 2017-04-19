VIM = vim

CC = gcc
CFLAGS = -O2 -fno-strength-reduce -Wall -U_FORTIFY_SOURCE -D_FORTIFY_SOURCE=1 -std=gnu99
LDFLAGS = -Wl,--as-needed
LIBS = -lm -ltinfo
CPP = gcc -E

VIMMAJOR = 7
VIMMINOR = 4

SHELL = /bin/sh

.SUFFIXES:
.SUFFIXES: .c .o

ALL_CFLAGS = $(CFLAGS)
ALL_LIBS = $(LIBS)

BASIC_SRC = buffer.c charset.c digraph.c \
	edit.c eval.c ex_cmds.c ex_cmds2.c ex_docmd.c ex_eval.c ex_getln.c \
	fileio.c getchar.c hashtab.c \
	main.c mark.c memfile.c memline.c message.c misc1.c misc2.c \
	move.c mbyte.c normal.c ops.c option.c os_unix.c \
	regexp.c screen.c search.c sha256.c syntax.c \
	term.c ui.c undo.c version.c window.c

SRC = $(BASIC_SRC)

OBJ_COMMON = \
	objects/buffer.o \
	objects/charset.o \
	objects/digraph.o \
	objects/edit.o \
	objects/eval.o \
	objects/ex_cmds.o \
	objects/ex_cmds2.o \
	objects/ex_docmd.o \
	objects/ex_eval.o \
	objects/ex_getln.o \
	objects/fileio.o \
	objects/getchar.o \
	objects/hashtab.o \
	objects/mark.o \
        objects/memline.o \
	objects/message.o \
	objects/misc1.o \
	objects/misc2.o \
	objects/move.o \
	objects/mbyte.o \
	objects/normal.o \
	objects/ops.o \
	objects/option.o \
	objects/os_unix.o \
	objects/regexp.o \
	objects/screen.o \
	objects/search.o \
	objects/sha256.o \
	objects/syntax.o \
	objects/term.o \
	objects/ui.o \
	objects/undo.o \
	objects/version.o \
	objects/window.o

OBJ = $(OBJ_COMMON) \
	objects/main.o \
	objects/memfile.o

all: $(VIM)

# The normal command to compile a .c file to its .o file.
CCC = $(CC) -c -I. $(ALL_CFLAGS)

# Link the target for normal use or debugging.
# A shell script is used to try linking without unnecessary libraries.
$(VIM): objects $(OBJ) version.c version.h
	$(CCC) version.c -o objects/version.o
	$(CC) $(LDFLAGS) -o $(VIM) $(OBJ) $(ALL_LIBS)

clean:
	-rm -fr objects $(VIM)

###########################################################################

.c.o:
	$(CCC) $<

objects:
	mkdir objects

objects/buffer.o: buffer.c
	$(CCC) -o $@ buffer.c

objects/charset.o: charset.c
	$(CCC) -o $@ charset.c

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

objects/getchar.o: getchar.c
	$(CCC) -o $@ getchar.c

objects/hashtab.o: hashtab.c
	$(CCC) -o $@ hashtab.c

objects/main.o: main.c
	$(CCC) -o $@ main.c

objects/mark.o: mark.c
	$(CCC) -o $@ mark.c

objects/memfile.o: memfile.c
	$(CCC) -o $@ memfile.c

objects/memline.o: memline.c
	$(CCC) -o $@ memline.c

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

objects/regexp.o: regexp.c
	$(CCC) -o $@ regexp.c

objects/screen.o: screen.c
	$(CCC) -o $@ screen.c

objects/search.o: search.c
	$(CCC) -o $@ search.c

objects/sha256.o: sha256.c
	$(CCC) -o $@ sha256.c

objects/syntax.o: syntax.c
	$(CCC) -o $@ syntax.c

objects/term.o: term.c
	$(CCC) -o $@ term.c

objects/ui.o: ui.c
	$(CCC) -o $@ ui.c

objects/undo.o: undo.c
	$(CCC) -o $@ undo.c

objects/window.o: window.c
	$(CCC) -o $@ window.c

###############################################################################

objects/buffer.o: buffer.c vim.h ex_cmds.h proto.h version.h
objects/charset.o: charset.c vim.h ex_cmds.h proto.h
objects/digraph.o: digraph.c vim.h ex_cmds.h proto.h
objects/edit.o: edit.c vim.h ex_cmds.h proto.h
objects/eval.o: eval.c vim.h ex_cmds.h proto.h version.h
objects/ex_cmds.o: ex_cmds.c vim.h ex_cmds.h proto.h
objects/ex_cmds2.o: ex_cmds2.c vim.h ex_cmds.h proto.h
objects/ex_docmd.o: ex_docmd.c vim.h ex_cmds.h proto.h
objects/ex_eval.o: ex_eval.c vim.h ex_cmds.h proto.h
objects/ex_getln.o: ex_getln.c vim.h ex_cmds.h proto.h
objects/fileio.o: fileio.c vim.h ex_cmds.h proto.h
objects/getchar.o: getchar.c vim.h ex_cmds.h proto.h
objects/hashtab.o: hashtab.c vim.h ex_cmds.h proto.h
objects/main.o: main.c vim.h ex_cmds.h proto.h
objects/mark.o: mark.c vim.h ex_cmds.h proto.h
objects/memfile.o: memfile.c vim.h ex_cmds.h proto.h
objects/memline.o: memline.c vim.h ex_cmds.h proto.h
objects/message.o: message.c vim.h ex_cmds.h proto.h
objects/misc1.o: misc1.c vim.h ex_cmds.h proto.h version.h
objects/misc2.o: misc2.c vim.h ex_cmds.h proto.h
objects/move.o: move.c vim.h ex_cmds.h proto.h
objects/mbyte.o: mbyte.c vim.h ex_cmds.h proto.h
objects/normal.o: normal.c vim.h ex_cmds.h proto.h
objects/ops.o: ops.c vim.h ex_cmds.h proto.h
objects/option.o: option.c vim.h ex_cmds.h proto.h
objects/os_unix.o: os_unix.c vim.h ex_cmds.h proto.h
objects/regexp.o: regexp.c vim.h ex_cmds.h proto.h
objects/screen.o: screen.c vim.h ex_cmds.h proto.h
objects/search.o: search.c vim.h ex_cmds.h proto.h
objects/sha256.o: sha256.c vim.h ex_cmds.h proto.h
objects/syntax.o: syntax.c vim.h ex_cmds.h proto.h
objects/term.o: term.c vim.h ex_cmds.h proto.h
objects/ui.o: ui.c vim.h ex_cmds.h proto.h
objects/undo.o: undo.c vim.h ex_cmds.h proto.h
objects/version.o: version.c vim.h ex_cmds.h proto.h version.h
objects/window.o: window.c vim.h ex_cmds.h proto.h
