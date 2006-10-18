/*
  Started on: 2006-10-09
  - Betalord

  This is part of "search log" functionality used in TASServer web interface.
*/

//#include <string.h>
#include <time.h>
//#include <iostream>
//#include <cstdlib>
#include <string>


//using namespace std;

//using std::string; //*** tako bo bolje morda? Da ne pride do naming konfliktov

//typedef std::basic_string<unsigned char> MyString;

time_t lastLogStamp; // unix time stamp value of last marker processed which has form of "Log started on dd/mm/yy". Note that this marker get automatically increased by 1 day each time we pass midnight when processing lines.
time_t lastLineStamp; // unix time stamp of the last line processed
bool lastLineWasLogStamp;
char lastLineHourStamp[3]; // hour ("xx") from last line that has been processed

/* searches for time markers ("Log started on dd/mm/yy"), returns timestamp of this line.
   This method assumes serial (consecutive) line processing (we process line 1 first, then
   line 2, 3, and so on.) */
void processLine(char* line)
{
  if (strncmp(line, "Log started on", 14) == 0)
  {
//    temp = std::string(line);

    // extract day, month and year from the line:
    char day[3], month[3], year[3];
    day[2] = 0;
    month[2] = 0;
    year[2] = 0;        
    memcpy(day, line+15, 2);
    memcpy(month, line+18, 2);
    memcpy(year, line+21, 2);

//***    printf("DEBUG: %s %s %s \r\n", day, month, year);

    tm stamp;
    stamp.tm_isdst = -1; // daylight savings unknown
    stamp.tm_year = 100 + atoi(year);
    stamp.tm_mon = atoi(month)-1;
    stamp.tm_mday = atoi(day);
    stamp.tm_hour = 0;
    stamp.tm_min = 0;
    stamp.tm_sec = 0;
    // tm_wday and tm_yday fields are ignored by mktime() function

/*
    stamp.tm_sec; // seconds after the minute (0-61)
    stamp.tm_min; // minutes after the hour (0-59)
    stamp.tm_hour; // hours since midnight (0-23)
    stamp.tm_mday; // day of the month (1-31)
    stamp.tm_mon; // months since January  (0-11)
    stamp.tm_year; // elapsed years since 1900
    stamp.tm_wday; // days since Sunday (0-6)
    stamp.tm_yday; // days since January 1st  (0-365)
    stamp.tm_isdst; // 1 if daylight savings is on, zero if not, -1 if unknown
*/
    lastLogStamp = mktime(&stamp);
    lastLineStamp = lastLogStamp;

    lastLineWasLogStamp = true;

  }
  else if ((strncmp(line, "\n", 1) == 0) || (strncmp(line, "\r", 1) == 0) || (strncmp(line, "\r\n", 2) == 0))
  {
    // do nothing!
  }
  else
  {
    // normal line, e.g. "[15:23:29] * the_duke has joined #main"

    // extract hour, minutes and seconds from the line header:
    char hour[3], min[3], sec[3];
    hour[2] = 0;
    min[2] = 0;
    sec[2] = 0;    
    memcpy(hour, line+1, 2);
    memcpy(min, line+4, 2);
    memcpy(sec, line+7, 2);
    
//***    printf("DEBUG: %s %s %s \r\n", hour, min, sec);

    // process day turns (when we get over 23:59:59):
    if ((strncmp(hour, "00", 2) == 0) && (!lastLineWasLogStamp) && ((strncmp(lastLineHourStamp, "23", 2) == 0))) // we assume there was at least one line added between 00:00:00 and 00:59:59, and at least one line between 23:00:00 and 23:59:59. This is not perfect of course, but there is no explicit information present in the log regarding that
    {
      // ok we went from 23:xx:xx to 00:xx:xx, let's increase current date:
      lastLogStamp += 24*60*60;
    }

    lastLineStamp = lastLogStamp + atoi(hour)*60*60 + atoi(min)*60 + atoi(sec);
    strncpy(lastLineHourStamp, hour, 2);
    lastLineWasLogStamp = false;
  }

}

char* timeToString(time_t *stamp, const char *format)
{
  char *s = (char *)malloc(51);
  tm *temp = gmtime(stamp); // don't ever free *temp, it's a pointer to some static, internal buffer!
  strftime(s, 50, format, temp);
  return s;
}

/* possible values (at least one!):
   * first argument is always log file name!
   * k [keywoard]
   * m [min date unix timestamp]
   * M [max date unix timestamp]
 */
int main(int argc, char **argv)
{
  int bytes_read;
  int nbytes = 2048;
  char *line, *truncline;
  FILE* file;
  
  // search criteria:
  bool usekeyword = false;
  bool usemindate = false;
  bool usemaxdate = false;
  char keyword[256];
  int mindate = 0;
  int maxdate = 0;

  lastLineHourStamp[2] = 0;

  if (argc < 4)
  {
  	printf("Error: at least 3 rguments required!\n");
    exit(1);
  }

  file = fopen (argv[1], "rt");
  if (file==NULL)
  {
  	printf("Error: cannot open file\n");
    exit(1);
  }
  
  int i = 2;
  while (i < argc)
  {
    if (argv[i][0] == 'k')
    {
      keyword[0] = 0;
      strncpy(keyword, argv[i+1], 255);
      i += 2;
      usekeyword = true;
      continue;
    }
    else if (argv[i][0] == 'm')
    {
      mindate = atol(argv[i+1]);
      i += 2;
      usemindate = true;
      continue;
    }
    else if (argv[i][0] == 'M')
    {
      maxdate = atol(argv[i+1]);
      i += 2;
      usemaxdate = true;
      continue;
    }
    else
    {
    	printf("Error: invalid parameters!");
      exit(1);
    }
  }

  line = (char *) malloc (nbytes + 1);
  truncline = (char *) malloc (nbytes + 1);
  time_t currentStamp;

  while (!feof(file))
  {
    line = fgets(line, nbytes, file);
    if (line == NULL) break; // end of file

    processLine(line);

    if (
    ((!usekeyword) || (strstr(line, keyword) != NULL)) &&
    ((!usemindate) || (lastLineStamp > mindate)) &&
    ((!usemaxdate) || (lastLineStamp < maxdate))
    )
    {
      if (lastLineWasLogStamp) printf("%s", line);
      else 
      {
        strcpy(truncline, line+11);   
        char *timestr = timeToString(&lastLineStamp, "%Y-%m-%d, %X"); // use this format: http://www-ccs.ucsd.edu/c/time.html#strftime
        printf("[%s] %s", timestr, truncline);
//        printf("%ld [%s] %s", lastLineStamp, timestr, line);
        free(timestr);
      }
    }
  }

  fclose(file);

  return 0;
}
