<?php require("inc/head.php") ?>

<?php 
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
  if ($_POST["C2"] == 1) $insert .= ', "' . $ip_start . '", "' . $ip_end . '"';
  if ($_POST["C3"] == 1) $insert .= ', "' . $user_id . '"';
  $insert .= ', "' . $priv_reason . '"';
  $insert .= ', "' . $pub_reason . '"';
  $insert .= ")";

  
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
  
  echo "<p>New ban entry has been successfully created.</p>";
  echo "<a class='button1' href='ban.php'>OK</a>";
  
?>

<?php require("inc/footer.php") ?>