VIM = vim

CC = gcc
CFLAGS = -O2 -fno-strength-reduce -Wall -Wextra -U_FORTIFY_SOURCE -D_FORTIFY_SOURCE=1 -std=gnu99
LDFLAGS = -Wl,--as-needed
LIBS = -lm -ltinfo

SRC = vim.c
OBJ = vim.o

all: $(VIM)

$(VIM): $(OBJ)
	$(CC) $(LDFLAGS) -o $(VIM) $(OBJ) $(LIBS)

vim.o: vim.c
	$(CC) -c -I. $(CFLAGS) -o $@ vim.c

clean:
	-rm -fv $(OBJ) $(VIM)
