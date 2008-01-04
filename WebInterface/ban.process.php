<?php require("inc/head.php") ?>

<?php 
  require_once('inc/curl_phpbb.class.php');
  
  function returnError($err_msg) 
  {
    printError($err_msg);
    echo "<br />";
    echo "<br />";
    goBackButton();
    exit();
  }

  function forceUpdate()
  {
    $conn = new ServerConnection();
    if (($res = $conn->connect()) !== true)
    {
      printError("<p style='color: black; font-weight: bold'>" . "Error: " . $res . "</p>");
      return;
    }
    if (($conn->identify()) == false)
    {
      printError("<p style='color: black; font-weight: bold'>" . "Error while trying to authenticate with the server" . "</p>");
      return;
    }

    if (($res = $conn->sendLine("queryserver RETRIEVELATESTBANLIST")) !== TRUE)
    {
      printError("<p style='color: black; font-weight: bold'>" . "Error while communicating with the server" . "</p>");
      return;
    }
    if (($res = $conn->readLine()) === FALSE)
    {
      printError("<p style='color: black; font-weight: bold'>" . "Error while communicating with the server" . "</p>");
      return;
    }

    $res = removeBeginning($res, 'SERVERMSG ');
    echo "<p>Server has successfully updated ban records from the database.</p>";

    // close connection with server:
    $conn->close();
  }

  // will try to make a post in the private moderation forum to notify other moderators about this new ban entry
  function postOnForum($message, &$errormsg) {
    global $constants;
  
    $forum_id = 17;
    $topic_id = 4091;
    
    // The ending backslash is required.
    $phpbb = new curl_phpbb('http://spring.clan-sy.com/phpbb/');
    
    // Log in
    $r = $phpbb->login($constants['forumposter_username'], $constants['forumposter_password']);
    if (!$r) {
      $errormsg = 'ErrCode: ' . implode(" - ", $phpbb->error);
      return false;
    }

    // Post a topic reply
    $r = $phpbb->topic_reply_advanced($topic_id, $forum_id, $message, &$errormsg);
    if (!$r) {
      return false;
    }

    // Log out
    $r = $phpbb->logout();
    if (!$r) {
      // ignore! We were still successful
    }
    
    return true;
  }
    
  //$bandur; // ban duration. If 1, then we banned for limited time, if 2, we banned for indefinite time
  if ($_POST["R_bandDuration"] == "limited") $bandur = 1;
  else if ($_POST["R_bandDuration"] == "unlimited") $bandur = 2;
  else returnError("Ban duration must be specified!");
  
  $dur_days = $_POST["dur_days"];
  if ($dur_days == "") $dur_days = 0;
  else if ((string)(int)$dur_days === $dur_days) $dur_days = (int)$dur_days;
  else returnError("Error: Duration days not an integer!");
  
  $dur_hours = $_POST["dur_hours"];
  if ($dur_hours == "") $dur_hours = 0;
  else if ((string)(int)$dur_hours === $dur_hours) $dur_hours = (int)$dur_hours;
  else returnError("Error: Duration hours not an integer!");
  
  if (($bandur == 1) && ($dur_days == 0) && ($dur_hours == 0)) returnError("Error: At least one of duration arguments must be set!");
  
  $priv_reason = $_POST["privatereason"];
  if ($priv_reason == "") returnError("No private reason was set!");
  
  $pub_reason = $_POST["publicreason"];
  if ($pub_reason == "") returnError("No public reason was set!");
  
  if (($_POST["C1"] != 1) && ($_POST["C2"] != 1) && ($_POST["C3"] != 1)) returnError("At least one criterion must be selected!");
  
  // check USERNAME criterion:
  $username = $_POST["username"];
  if ($_POST["C1"] == 1) {
    if (!isset($username) || ($username == "")) returnError("Username must be set!");
  }
  
  // check IP RANGE criterion:
  $ip_start = $_POST["ip_start"];
  $ip_end = $_POST["ip_end"];
  if ($_POST["C2"] == 1) {
    if (!isset($ip_start) || !isset($ip_end)) returnError("Both ip_start and ip_end must be specified!");
    if (($ip_start == "") || ($ip_end == "")) returnError("Both ip_start and ip_end must be specified!");
    if (!isValidIP($ip_start)) returnError("Start IP is not valid!");
    if (!isValidIP($ip_end)) returnError("End IP is not valid!");
  }
  
  // check USER ID criterion:
  $user_id = $_POST["userid"];
  if ($_POST["C3"] == 1) {
    if (!isset($user_id) || ($user_id == "")) returnError("User ID must be set!");
    if ((string)(int)$user_id === $user_id) $user_id = (int)$user_id;
    else returnError("User ID must be an integer!");
  }
  
  
  // construct the sql INSERT command:
  $insert = "INSERT INTO BanEntries (Owner, ";
  if ($bandur == 1) $insert .= "ExpirationDate, ";
  if ($_POST["C1"] == 1) $insert .= "Username, ";
  if ($_POST["C2"] == 1) $insert .= "IP_start, IP_end, ";
  if ($_POST["C3"] == 1) $insert .= "userID, ";
  $insert .= "PrivateReason, PublicReason) values (";
  $insert .= '"' . $_SESSION['username'] . '"';
  if ($bandur == 1) {
    $enddate = time() + $dur_days * 24 * 3600 + $dur_hours * 3600;
    $insert .= ', FROM_UNIXTIME(' . $enddate . ')';
  }
  if ($_POST["C1"] == 1) $insert .= ', "' . $username . '"';
  if ($_POST["C2"] == 1) $insert .= ', "' . IP2LONG_($ip_start) . '", "' . IP2LONG_($ip_end) . '"';
  if ($_POST["C3"] == 1) $insert .= ', "' . $user_id . '"';
  $insert .= ', "' . $priv_reason . '"';
  $insert .= ', "' . $pub_reason . '"';
  $insert .= ")";
  
  // construct $forumpost, which is what we'll post on the forum:
  $forumpost = "<" . $_SESSION['username'] . "> has just added new ban entry:\n\n[list]\n";
  if ($_POST["C1"] == 1) $forumpost .= "[*]Banned username: [color=#0000FF]" . $username . "[/color]\n";
  if ($bandur == 1) {
    $forumpost .= "[*]Banned until " . date("Y-m-d, H:i:s", $enddate) . "\n";
  } else {
  $forumpost .= "[*]Banned indefinitely\n";
  }
  if ($_POST["C2"] == 1) $forumpost .= "[*]Banned IP: " . ($ip_start == $ip_end ? $ip_start : $ip_start . " - " . $ip_end) . "\n";
  if ($_POST["C3"] == 1) $forumpost .= "[*]Banned User ID: " . $user_id . "\n";
  $forumpost .= "[/list]\n\n";
  $forumpost .= "When user tries to connect, he will see:\n";
  $forumpost .= "[quote]".$pub_reason."[/quote]\n";
  $forumpost .= "<" . $_SESSION['username'] . "> says this about it:\n";
  $forumpost .= "[quote]".$priv_reason."[/quote]\n";

  
  // now connect to the database and add new ban entry:
  // (Done after this tutorial: http://www.databasejournal.com/features/mysql/article.php/1469211)
  $dbh = mysql_connect($constants['database_url'], $constants['database_username'], $constants['database_password']) 
    or returnError("Unable to connect to the database.");
  $selected = mysql_select_db($constants['database_name'], $dbh) 
    or returnError("Problems connecting to the database.");
 
  // insert ban entry into the database: 
  if (!mysql_query($insert)) {
    // DEBUG: print "Failed to insert record. Insert command was: <begin>" . $insert ."<end> Also, error is this: " . mysql_error();
    returnError("Error while trying to access the database.");
  }
  
  mysql_close($dbh);
  
  echo "<p>New ban entry has been successfully created. Now forcing TASServer to fetch new ban entries from the database ... </p>";
  forceUpdate(); 
  if (!postOnForum($forumpost, &$errormsg)) {
    echo "<p style='color: red'>Unable to post on the forum, you will have to post it manually. (error: " . $errormsg . "</p>";
  } else {
    echo "<p>Post has been added to forum successfully.</p>";
  }  
  echo "<a class='button1' href='ban.php'>OK</a>";
  
?>

<?php require("inc/footer.php") ?>