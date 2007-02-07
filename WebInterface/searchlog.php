<?php require("inc/head.php") ?>

  <?php

    require("inc/searchlog.functions.php");

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
      $filename = "../../ChanServ/logs/#main.log";
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