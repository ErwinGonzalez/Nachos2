#include "syscall.h"

#define NULL 0
int
main()
{
	char *executable, *prog2;
	int exitS=1;
	executable="matmult.coff";
	char *arg[1];
	arg[0]=executable;
	int id;
	id = exec(executable,1,arg);
	int jId;
	jId = join(id,&exitS);
	if(jId==0){
		printf("joined process %d\n",id);}
	exit(0);
	return 1;
}
