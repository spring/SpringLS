#include <stdio.h>

int main(int argc, char **argv)
{
  int bytes_read;
  int nbytes = 256;
  char *line;
  FILE* file;

  if (argc < 3)
  {
  	printf("Error: two arguments required\n");
    exit(1);
  }

  file = fopen (argv[1], "rt");
  if (file==NULL)
  {
  	printf("Error: cannot open file\n");
    exit(1);
  }

  line = (char *) malloc (nbytes + 1);

  while (!feof(file))
  {
    if (bytes_read = getline (&line, &nbytes, file) == -1) break; // would work with fgets as well. What works faster though?
    if (strstr(line, argv[2]) != NULL)
/*      puts(line); -> prints out EOF too, which we don't want */
      printf("%s", line);
  }

  fclose(file);

  return 0;
}
