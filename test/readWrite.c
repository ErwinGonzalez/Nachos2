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
	for(i =0;i<=25;i++){
	    buf[i]=i+65;}
	read(2,buf,26);
	write(1,buf,26);
	close(2);
	return 1;
}
