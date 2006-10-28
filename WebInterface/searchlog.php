<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title>TASServer system notifications</title>
    <link rel="stylesheet" type="text/css" href="style.css" />
  </head>
  <body>
  
  <!--
    <label for="search_input_id">Type in username or part of username you want to retrieve log for:</label>
    <input type="text" style="font-size: 11px" name="search_input" id="search_input_id" size="10" accesskey="s" tabindex="101" value="User Name" onfocus="if (this.value == 'User Name') this.value = '';" />
  -->

  <?php

    include ('functions.php');

    function displaySearchForm() 
    {
      print "<p> Select one or more criteria and click Submit: </p>";

      echo "  <form action='{$PHP_SELF}' method='post'>";
      keywordBox();
      echo "<br />";
      minDateBox();
      echo "<br />";
      maxDateBox();
      echo "<br />";
      echo "<input type='submit' value='Submit' name='submit' />";
      echo "  </form>";
      
      echo "<br><br>";
      echo "Note 1: to search for all entries made by user Joe, use \"Joe>\" as keyword criterion.<br>";
      echo "Note 2: to specify time interval, select both min. and max. date criteria.";

    }

    function displayLog($keyword, $mindate, $maxdate)
    {
      $filename = "/home/betalord/ChanServ/logs/#main.log";
//      $filename = "/home/betalord/#main.log";
//      $filename = "/home/betalord/ChanServ/logs/#slo.log";
      print "<p>Using file {$filename}.</p>";
      print "<p>Time stamps are relative to CET - Central European Time.</p>";
      print "<br>";
      print "Search results:";
      print '<div style="font-family: Fixedsys, "Lucida Console", monospace">';
      print "<hr />";

      $count = 0;

      $command = "./searchlog " . $filename;
      if (strlen($keyword) > 0) $command = $command . " k " . '"' . $keyword . '"';
      if ($mindate != 0) $command = $command . " m " . $mindate;
      if ($maxdate != 0) $command = $command . " M " . $maxdate;

      $handle = popen($command, "r");
      while (!feof($handle))
      {
        $read = fgets($handle, 1024);
        if (feof($handle)) continue;
//        echo "<i>" . htmlspecialchars(rtrim($read)) . "</i><br>";
        echo htmlspecialchars(rtrim($read)) . "<br>";
        $count += 1;
      }
      pclose($handle);

      if ($count == 0)
      {
        echo "No mathing lines have been found! Try searching using different keyword.";
        return false;
      }

      print "<hr />";
      print "</div>";
      echo "<br> $count lines mathing search criteria. <br> End of file.";
    }

    if ($_POST['submit']) {
      if ((!isset($_POST['usekeyword'])) && (!isset($_POST['usemindate'])) && (!isset($_POST['usemaxdate'])))
      {
        echo '<font color="#FF0000"><b>Error: at least one search criterion must be selected!</b></font><br>';
        displaySearchForm();
      } else if ((isset($_POST['usemindate'])) && (!checkDate($_POST['min_month'], $_POST['min_day'], $_POST['min_year']))) {
        echo '<font color="#FF0000"><b>Error: minimum date is not valid!</b></font><br>';
        displaySearchForm();
      } else if ((isset($_POST['usemaxdate'])) && (!checkDate($_POST['max_month'], $_POST['max_day'], $_POST['max_year']))) {
        echo '<font color="#FF0000"><b>Error: maximum date is not valid!</b></font><br>';
        displaySearchForm();
      } else if ((isset($_POST['usekeyword'])) && (strlen($_POST['keyword']) > 255)) {
        echo '<font color="#FF0000"><b>Error: search keyword too long!</b></font><br>';
        displaySearchForm();
      } else {
        // everything is fine, display the results now:
        $keyword = (isset($_POST['usekeyword']) ? $_POST['keyword'] : "");
        $mindate = (isset($_POST['usemindate']) ? gmmktime($_POST['min_hour'], $_POST['min_min'], $_POST['min_sec'], $_POST['min_month'], $_POST['min_day'], $_POST['min_year']) : 0);
        $maxdate = (isset($_POST['usemaxdate']) ? gmmktime($_POST['max_hour'], $_POST['max_min'], $_POST['max_sec'], $_POST['max_month'], $_POST['max_day'], $_POST['max_year']) : 0);

        if (isset($_POST['usekeyword'])) {
          echo "Keyword: " . $_POST['keyword'] . "<br>";
        }

        if (isset($_POST['usemindate'])) {
          echo "Min. date: " . date("Y-F-d, H:i:s", mktime($_POST['min_hour'], $_POST['min_min'], $_POST['min_sec'], $_POST['min_month'], $_POST['min_day'], $_POST['min_year'])) . "<br>";
        }

        if (isset($_POST['usemaxdate'])) {
          echo "Max. date: " . date("Y-F-d, H:i:s", mktime($_POST['max_hour'], $_POST['max_min'], $_POST['max_sec'], $_POST['max_month'], $_POST['max_day'], $_POST['max_year'])) . "<br>";
        }

        displayLog($keyword, $mindate, $maxdate);
      }
    } else {
      displaySearchForm();
    }

  ?>

  </body>
</html>