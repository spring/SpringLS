<?php require("inc/head.php") ?>

  <?php

    require("inc/searchlog.functions.php");

    function goBackButton()
    {
      echo "<a class='button1' href='javascript:history.go(-1)'>Go back</a>";
    }
    
    function returnError($err_msg) 
    {
      printError($err_msg);
      echo "<br />";
      echo "<br />";
      goBackButton();
      exit();
    }
  
    function displaySearchForm()
    {
      print "<span style='color: blue; font-weight: bold;'>Important notice: On 2007/12/26 older logs were moved to an archive and new logs are available only through a database, so any logs from before that date are unavailable.</span>";
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
      $logname = "#main";
    
      print "<p>Using channel log {$logname}.</p>";
      print "<p>Time stamps are relative to CET - Central European Time.</p>";
      print "<br>";
      print "Search results:";
      print '<div style="font-family: Fixedsys, "Lucida Console", monospace">';
      print "<hr />";

      $timer = microtime_float();
      
      $count = 0;

      $dbh = mysql_connect($constants['database_url'], $constants['database_username'], $constants['database_password']) 
        or returnError("Unable to connect to the database.");
      $selected = mysql_select_db("ChanServLogs", $dbh) 
        or returnError("Problems connecting to the database. Error: " . mysql_error());
     
      $select = "SELECT stamp, line FROM `" . $logname . "`";
      if (($mindate != 0) && ($maxdate != 0)) {
        $select .= " WHERE stamp > " . $mindate . " AND stamp < " . $maxdate;
      } elseif (($mindate == 0) && ($maxdate != 0)) {
        $select .= " WHERE stamp < " . $maxdate;
      } elseif (($mindate != 0) && ($maxdate == 0)) {
        $select .= " WHERE stamp > " . $mindate;
      }
      
      $result = mysql_query($select);
      while($row = mysql_fetch_row($result))
      {
        if (strlen($keyword) > 0) 
          if (strpos($row[1], $keyword) === false) 
            continue;

        echo "[" . date('Y-m-d H:i:s', $row[0]) . "] " . htmlspecialchars(rtrim($row[1])) . "<br>";
        $count += 1;
      }     
      
      mysql_close($dbh);
        
      if ($count == 0)
      {
        echo "No mathing lines have been found! Try searching using different keyword.";
        return false;
      }

      print "<hr />";
      print "</div>";
      $timer = microtime_float() - $timer;
      printf("<br> %d lines mathing search criteria. Query took %01.2f seconds <br> End of file.", $count, $timer);
    }

    // this is where execution of this script starts:

    // first check if user's ip is not in the ban list:
    $ip = $_SERVER['REMOTE_ADDR'];
    $iparray = file("ipblock.txt");
    $i = 0;
    while ($i < count($iparray)) {
      $line = $iparray[$i];
      $line = trim(substr($line, 0, strpos($line, '#')));
      if ($line == "") {
        $i++;
        continue;
      }
      if ($_GET['debug']) echo "<$line><br>";
      if ($line == $ip)
        exit("You have been blocked from accessing this script. Contact server administrator for more info.");
      $i++;
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

<?php require("inc/footer.php") ?>