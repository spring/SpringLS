<?php
  $constants = array();
  $constants['remote_address'] = "89.212.76.147";
  $constants['remote_port'] = "8205";
  $constants['access_key'] = "phpscript:pass"; // remote access key
  $constants['session_timeout_time'] = 10 * 60; // in seconds. After so many seconds of inactivity, user will be automatically logged out
  $constants['updates_file'] = "../updates.xml"; // file name (and path) to the updates.xml file
  $constants['motd_file'] = "../motd.txt"; // file name (and path) to the updates.xml file
  $constants['database_url'] = "127.0.0.1"; // database where ban entries are stored etc.
  $constants['database_name'] = "spring";
  $constants['database_username'] = "anonymous";
  $constants['database_password'] = "anonymous";

  /* restrictions for various pages (0 means no restrictions (default), 1 means "normal user" (that is user must be logged in),
     2 means moderator and 3 means administrator. Pages not listed in this array have default restriction of 0.
  */
  $restrictions = array();
  $restrictions['notifs.php'] = 2;
  $restrictions['searchlog.php'] = 2;
  $restrictions['ban.php'] = 2;
  $restrictions['ban.add.php'] = 2;
  $restrictions['ban.process.php'] = 2;
  $restrictions['ban.list.php'] = 2;
  $restrictions['ban.delete.php'] = 2;
  $restrictions['userinfo.php'] = 2;
  $restrictions['editmotd.php'] = 3;
  $restrictions['updating.php'] = 3;
  $restrictions['updating.delete.php'] = 3;
  $restrictions['updating.append.php'] = 3;
  $restrictions['updating.apply.php'] = 3;
  $restrictions['phpinfo.php'] = 3;
?>
