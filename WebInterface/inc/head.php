<?php
  require("functions.php");
  include("maintenance.inc");

  global $MAINTENANCE_MODE;
  if ($MAINTENANCE_MODE) 
  {
    displayMaintenancePage();
    die();
  }
  
  startSessionProperly();
?>


<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title>TASServer Web Interface</title>
    <link rel="stylesheet" type="text/css" href="style.css" />
  </head>
  <body class="waterbody">

  <table width="100%" border="0" cellspacing="0" cellpadding="10" style='margin-left: 10px'>
    <tr>
      <!-- top-left space: -->
      <td class="leftcell">
      </td>

      <!-- top-right space: -->
      <td>
     	  <?php
     	    echo "<h1>TASServer Web Interface</h1>";
          echo "<span style='color: green; font-weight: bold;'>
                Important notice: until Spring 0.76 release, this site will not be connected to actual lobby accounts
                but will rather be using temporary accounts that were transfered to a database from TASServer on 28.12.2007.
                This will remain so until next release as this site is being updated to use new account database.
                </span>
                <hr>
                ";
    	  ?>
      </td>
    </tr>

    <tr>
      <!-- left-side menu: -->
      <td class="leftcell" align="center" valign="top">
      <!-- start menu -->

        <div id="left">

          <ul id="leftmenu">
            <?php
              function page_link($link, $title)
              {
                $base = basename($_SERVER['PHP_SELF']);
                if (substr($base, 0, strpos($base, '.')) == substr($link, 0, strpos($base, '.')))
                  echo '<li style="font-weight: bold"><a href="' . $link . '" title="' . $title . '">' . $title . '</a></li>';
                else
                  echo '<li><a href="' . $link . '" title="' . $title . '">' . $title . '</a></li>';
              }

              page_link("index.php", "Home");
              page_link("stats.php", "Server stats");
              page_link("notifs.php", "Server notifications");
              page_link("searchlog.php", "Search logs");
              page_link("ban.php", "Ban control");
              page_link("userinfo.php", "User information");
              page_link("updating.php", "Update manager");
              page_link("editmotd.php", "Edit MOTD");
              page_link("phpinfo.php", "phpinfo()");
              page_link("http://spring.clan-sy.com", "Spring web site");

            ?>
          </ul>

          <?php
            displayLogin();
            print "<br />";
            print "Current time: " . date("G:i:s (O)", time()) . "<br />";
          ?>

        </div>

      <!-- end menu -->
      </td>
      <td class="content" valign="top">

      <!-- start content -->

      <?php

        /* before actually starting the content, we must here first check if user
        is authorized to view this page at all. If not, we will stop loading this page
        and display some information regarding this problem. */

        if (checkAccess() == false)
        {
          require("restricted.inc");
          require("footer.php");
          exit;
        }

      ?>