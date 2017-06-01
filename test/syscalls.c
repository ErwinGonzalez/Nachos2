#include "syscall.h"

#define NULL 0
int
main()
{
	//char*  file = "new.txt";
	//creat(file);
	//open(file);
	//close(2);
	char *executable, *prog2;
	int exitS;
	executable="matmult.coff";
	prog2="sort.coff";
	char *arg[1];
	arg[0]=executable;
	int id,id2;
	id = exec(executable,1,arg);
	//exit(0);
	arg[0]=prog2;
	int jId;
	id2=exec(prog2,1,arg);
	jId = join(id,&exitS);
	if(jId==0){
		printf("joined process %d\n",id);}
	exit(0);
	//unlink(file);
    /* not reached */
	return 1;
}
