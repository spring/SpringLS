#include <stdio.h>
#include <stdlib.h>

int main(int argc, char **argv)
{
  char c;

  if (argc < 2)
  {
  	printf("Error: argument required\n");
    exit(1);
  }

  /* Append to a file. The file is created if it doesn't exist. */
  FILE* file = fopen (argv[1], "a");
  if (file==NULL)
  {
  	printf("Error: cannot open file\n");
    exit(1);
  }

	while ((c = (char)getchar()) && (!feof(stdin))) 
  {
		fputc(c, stdout);
		fputc(c, file);
	}

  fclose(file);

  return 0;
}
