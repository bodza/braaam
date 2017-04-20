VIM = vim

CC = gcc
CFLAGS = -std=gnu99 -Wall -Wextra -U_FORTIFY_SOURCE -D_FORTIFY_SOURCE=1 -g
RM = rm

SRC = vim.c
OBJ = vim.o

all: $(VIM)

$(VIM): $(OBJ)
	$(CC) -o $(VIM) $(OBJ)

check: vim.c
	$(CC) -fsyntax-only $(CFLAGS) vim.c

vim.o: vim.c
	$(CC) -c $(CFLAGS) -o $@ vim.c

clean:
	$(RM) -fv $(OBJ) $(VIM)
