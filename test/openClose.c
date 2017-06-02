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
	close(2);
	for(i=0;i<26;i++){
		buf[i]=65;}
	write(2,buf,26);
	return 1;
}
