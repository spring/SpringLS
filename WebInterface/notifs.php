<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title>TASServer system notifications</title>
    <link rel="stylesheet" type="text/css" href="style.css" />
  </head>
  <body class="waterbody">


  <?php

    // will count how many notifications exist for specific day (basename should be left part of filename for that specific day):
    function countNotifs($basename)
    {
      $counter = 1;
      while (file_exists($basename . "_" . $counter)) $counter += 1;
      return $counter - 1;
    }

    function displayNotif($filename)
    {
      /*
        $lines = file('./' . $filename);
        $ltime = $lines[3];
        print "Title: " . $lines[2] . "<br>";
        print "Time: " . date("G:i:s (O)", $ltime / 1000) . "<br>";
        print "Author: " . $lines[1] . "<br>";
        for($i = 4; $i < count($lines); $i += 1) {
          //echo "Line #<b>" . ($i-2) . "</b> : " . htmlspecialchars(rtrim($lines[$i])) . "<br />\n";
          echo htmlspecialchars(rtrim($lines[$i])) . "<br />\n";
        }
        print "<br />";
      */
      
        $lines = file('./' . $filename);
        $ltime = $lines[3] / 1000;
        global $titles;

        print "<table class='notification' frame='box' cellpadding='5'>";
        print "  <tr>";
        print "    <th class='headercell nopad' width=\"150\">";
        // left-header content
//        print "      header left";
        // end left-header content
        print "    </th>";
        print "    <th class='headercell nopad'>";
        // right-header content
        print $lines[2]; // title
        $titles[$lines[2]] += 1; // keep track of title count
        // end right-header content
        print "    </th>";
        print "  </tr>";
        print "  <tr>";
        print "    <td width=\"150\" align='left' valign='top'>";
        // left-cell content
        print "      Time: " . date("G:i:s (O)", $ltime) . "<br><br>";
//        print "      Date: " . date("Y-m-d", $ltime) . "<br><br>";
        print "      Author: " . $lines[1] . "<br>";
        // end left-cell content
        print "    </td>";
        print "    <td>";
        // right-cell content
        for($i = 4; $i < count($lines); $i += 1) {
          //echo "Line #<b>" . ($i-2) . "</b> : " . htmlspecialchars(rtrim($lines[$i])) . "<br />\n";
          echo htmlspecialchars(rtrim($lines[$i])) . "<br />\n";
        }
        // end right-cell content
        print "    </td>";
        print "  </tr>";
        print "</table>";
        print "<br />";
    }

    function displayAllNotifs($basename)
    {
      $counter = 1;
      while (file_exists($basename . "_" . $counter))
      {
        displayNotif($basename . "_" . $counter);
        $counter += 1;
      }
      
      if ($counter == 1) print "<p>There are no notifications logged for this day.</p>";
    }


    $time = time();
    if ($_GET['date']) $time = $_GET['date'];

    $day = date("w", $time); // "w" returns day of the week (0-6)

    $nextWeek = $time + (7 * 24 * 60 * 60);
    $prevWeek = $time - (7 * 24 * 60 * 60);

    $linktime['sunday'] = $time - 24 * 60 * 60 * $day;
    $linktime['monday'] = $time - 24 * 60 * 60 * ($day - 1);
    $linktime['tuesday'] = $time - 24 * 60 * 60 * ($day - 2);
    $linktime['wednesday'] = $time - 24 * 60 * 60 * ($day - 3);
    $linktime['thursday'] = $time - 24 * 60 * 60 * ($day - 4);
    $linktime['friday'] = $time - 24 * 60 * 60 * ($day - 5);
    $linktime['saturday'] = $time - 24 * 60 * 60 * ($day - 6);

    // figure out how many notifications exist per certain day in the week (for selected week):
    $count['sunday'] = countNotifs(date("Ymd", $linktime['sunday']));
    $count['monday'] = countNotifs(date("Ymd", $linktime['monday']));
    $count['tuesday'] = countNotifs(date("Ymd", $linktime['tuesday']));
    $count['wednesday'] = countNotifs(date("Ymd", $linktime['wednesday']));
    $count['thursday'] = countNotifs(date("Ymd", $linktime['thursday']));
    $count['friday'] = countNotifs(date("Ymd", $linktime['friday']));
    $count['saturday'] = countNotifs(date("Ymd", $linktime['saturday']));



    /*
    //Includes setup and function stuff
    include ('include/start.php');

    //Gets what page we are trying to view
    $page = 'index';
    if ($_GET['time'])
    {
      if (strpos($_GET['p'], ".") === FALSE)
      {
        if (file_exists("pages/".$_GET['p'].".php"))
        {
          $page = $_GET['p'];
        }
      }
    }
    */
  ?>


<!--
  <table id="navigation">
  	<tr>
    	<td class="tnav">
    	  <?php
    	    print "<a class=\"navlink_prev_next\" href=\"notifs.php?date=" . $prevNext . ">&nbsp;&#60;&#151; Prev. week&nbsp;</a>"
    	  ?>

    	  |
    		<a class="navlink" href="notifs.php?date=x">&nbsp;Mon&nbsp;</a>
    		|
    		<a class="navlink_selected" href="notifs.php?date=x">&nbsp;Tue&nbsp;</a>
    		|
    		<a class="navlink" href="notifs.php?date=x">&nbsp;Wed&nbsp;</a>
    		|
    		<a class="navlink" href="notifs.php?date=x">&nbsp;Thu&nbsp;</a>
    		|
    		<a class="navlink" href="notifs.php?date=x">&nbsp;Fri&nbsp;</a>
    		|
    		<a class="navlink" href="notifs.php?date=x">&nbsp;Sat&nbsp;</a>
    		|
    		<a class="navlink" href="notifs.php?date=x">&nbsp;Sun&nbsp;</a>
    		|
    		<a class="navlink_prev_next" href="notifs.php?date=x">Next week &#151;&#62;&nbsp;</a>
  		</td>
  	</tr>
	</table>

	<div id="topmenu">
    <ul id="mainlevel-nav">
      <li><a href="http://demo.opensourcecms.com/mambo/" class="mainlevel-nav" >Contact Us</a></li>
      <li><a href="http://demo.opensourcecms.com/mambo/" class="mainlevel-nav" >Home</a></li>
    </ul>
  </div>

-->


<!--
<div>
  <script type='text/javascript' src='/e107/e107_files/nav_menu_alt.js'></script>
  <div class='menuBar' style='width:100%; white-space: nowrap'>
    <a class='menuButton' href='/e107/index.php' style='background-image: url(/e107/e107_themes/jayya/images/arrow.png); background-repeat: no-repeat; background-position: 3px 1px; white-space: nowrap' >Home</a>
    <a class='menuButton' href='/e107/download.php' style='background-image: url(/e107/e107_themes/jayya/images/arrow.png); background-repeat: no-repeat; background-position: 3px 1px; white-space: nowrap' >Downloads</a>
    <a class='menuButton' href='/e107/user.php' style='background-image: url(/e107/e107_themes/jayya/images/arrow.png); background-repeat: no-repeat; background-position: 3px 1px; white-space: nowrap' >Members</a>
    <a class='menuButton' href='/e107/submitnews.php' style='background-image: url(/e107/e107_themes/jayya/images/arrow.png); background-repeat: no-repeat; background-position: 3px 1px; white-space: nowrap' >Submit News</a>
  </div>
</div>

  <div id="bittopbar"><a accesskey="h" href="/">Home</a><a href="/articles/45">Download</a><a href="/wiki/documentation">Documentation</a><a href="/wiki/support">Support</a><a href="/wiki/developer+center">Community</a><a href="http://www.opensourcecms.com/index.php?option=com_content&amp;task=view&amp;id=2155&amp;PHPSESSID=c7c8f321cab4f67b9dca147abcee97b3">Demo</a></div>

-->


  <table width="100%" border="0" cellspacing="0" cellpadding="10">
    <tr>
    <td id="leftcell">
    </td>

    <td>
    	<div id="topmenu">
        <ul id="mainlevel-nav">
      	  <?php
      	    print "<li><a href=\"notifs.php?date={$prevWeek}\" style=\"color: #0099FF;\" >Prev. week</a></li>";
      	    print "<li><a href=\"notifs.php?date={$linktime['sunday']}\"" . ($day == 0 ? "style=\"color: #99FF66;\"" : "") . "> Sunday ({$count['sunday']})</a></li>";
      	    print "<li><a href=\"notifs.php?date={$linktime['monday']}\"" . ($day == 1 ? "style=\"color: #99FF66;\"" : "") . "> Monday ({$count['monday']})</a></li>";
      	    print "<li><a href=\"notifs.php?date={$linktime['tuesday']}\"" . ($day == 2 ? "style=\"color: #99FF66;\"" : "") . "> Tuesday ({$count['tuesday']})</a></li>";
      	    print "<li><a href=\"notifs.php?date={$linktime['wednesday']}\"" . ($day == 3 ? "style=\"color: #99FF66;\"" : "") . "> Wednesday ({$count['wednesday']})</a></li>";
      	    print "<li><a href=\"notifs.php?date={$linktime['thursday']}\"" . ($day == 4 ? "style=\"color: #99FF66;\"" : "") . "> Thursday ({$count['thursday']})</a></li>";
      	    print "<li><a href=\"notifs.php?date={$linktime['friday']}\"" . ($day == 5 ? "style=\"color: #99FF66;\"" : "") . "> Friday ({$count['friday']})</a></li>";
      	    print "<li><a href=\"notifs.php?date={$linktime['saturday']}\"" . ($day == 6 ? "style=\"color: #99FF66;\"" : "") . "> Saturday ({$count['saturday']})</a></li>";
      	    print "<li><a href=\"notifs.php?date={$nextWeek}\" style=\"color: #0099FF;\" >Next week</a></li>";
      	  ?>
        </ul>
      </div>
    </td>
    </tr>

    <tr>
      <td id="leftcell" align="center" valign="top">
      <!-- start menu -->

        <div id="left">

          <ul id="leftmenu">
          	<li><a href="notifs.php" title="Today">Today</a></li>
          	<?php print "<li><a href=\"notifs.php?date=" . ($time - 24 * 60 * 60) . "\" title=\"Previous day\">Current day - 1</a></li>" ?>
          	<?php print "<li><a href=\"notifs.php?date=" . ($time + 24 * 60 * 60) . "\" title=\"Next day\">Current day + 1</a></li>" ?>
          	<li><a href="http://taspring.clan-sy.com/stats" title="Server stats">Server stats</a></li>
          	<li><a href="http://taspring.clan-sy.com:8202/notifs/searchlog.php" title="Search chat logs">Search chat logs</a></li>
          	<li><a href="http://taspring.clan-sy.com" title="Spring web site">Spring web site</a></li>
          </ul>

          <?php
            print "<br />";
            print "Current time: " . date("G:i:s (O)", time()) . "<br />";
          ?>

        </div>

      <!-- end menu -->
      </td>
      <td class="content" valign="top">

      <!-- start content -->

      <?php
        print "<h2>Notifications for " . date("Y-m-d", $time) . "</h1>";
        print "<br>";

        displayAllNotifs(date("Ymd", $time));

        print "<br /> <hr /> <br />";
        print "List of notifications by type: <br><br>";

        if (count($titles) == 0)
          print "-- no notifications listed --";
        else
        {
          foreach ($titles as $title => $c)
          {
            print "<b>{$title} ({$c})</b><br />";
          }
        }

      ?>

      <!-- end content -->
      </td>
    </tr>
  </table>

  </body>
</html>