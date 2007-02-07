<?php require("inc/head.php") ?>

  <?php

    $notifs_folder = "../notifs/";

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
      
        $lines = file($filename);
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
        print "    <td width=\"150\" align='left' valign='top' id='notif_leftcell'>";
        // left-cell content
        print "      Time: " . date("G:i:s (O)", $ltime) . "<br><br>";
//        print "      Date: " . date("Y-m-d", $ltime) . "<br><br>";
        print "      Author: " . $lines[1] . "<br>";
        // end left-cell content
        print "    </td>";
        print "    <td id='notif_rightcell'>";
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
      global $notifs_folder;

      $counter = 1;
      while (file_exists($notifs_folder . $basename . "_" . $counter))
      {
        displayNotif($notifs_folder . $basename . "_" . $counter);
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
    $count['sunday'] = countNotifs($notifs_folder . date("Ymd", $linktime['sunday']));
    $count['monday'] = countNotifs($notifs_folder . date("Ymd", $linktime['monday']));
    $count['tuesday'] = countNotifs($notifs_folder . date("Ymd", $linktime['tuesday']));
    $count['wednesday'] = countNotifs($notifs_folder . date("Ymd", $linktime['wednesday']));
    $count['thursday'] = countNotifs($notifs_folder . date("Ymd", $linktime['thursday']));
    $count['friday'] = countNotifs($notifs_folder . date("Ymd", $linktime['friday']));
    $count['saturday'] = countNotifs($notifs_folder . date("Ymd", $linktime['saturday']));

  ?>


  <table width="100%" border="0" cellspacing="0" cellpadding="10">
    <tr>
      <td colspan="2">
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

      <td align="center" valign="top" style="width: 200px">
      <!-- start menu -->

        <div id="left">

          <ul id="leftmenu">
          	<li><a href="notifs.php" title="Today">Today</a></li>
          	<?php print "<li><a href=\"notifs.php?date=" . ($time - 24 * 60 * 60) . "\" title=\"Previous day\">Current day - 1</a></li>" ?>
          	<?php print "<li><a href=\"notifs.php?date=" . ($time + 24 * 60 * 60) . "\" title=\"Next day\">Current day + 1</a></li>" ?>
          </ul>

        </div>

      <!-- end menu -->
      </td>
    </tr>
  </table>


<?php require("inc/footer.php") ?>