#include "syscall.h"

#define NULL 0
#define BUFFSIZE 26
char buf[BUFFSIZE];
int
main()
{
	char*  file = "testfile1.txt";
	open(file);
	int i;
	unlink(file);
	close(2);
	unlink(file);
    /* not reached */
	return 1;
}
