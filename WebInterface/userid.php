<?php require("inc/head.php") ?>

<?php

  $username = $_GET['username'];
  if ($username == "") {
    printError("No username specified. Cancelling operation.");
    return;
  }
    
  $conn = new ServerConnection();
  if (($res = $conn->connect()) !== true)
  {
    echo "<p style='color: black; font-weight: bold'>" . "Error: " . $res . "</p>";
    return;
  }

  if (($conn->identify()) == false)
  {
    echo "<p style='color: black; font-weight: bold'>" . "Error while trying to authenticate with the server" . "</p>";
    return;
  }

  if (($res = $conn->sendLine("queryserver GETUSERID " . $username)) !== TRUE)
  {
    echo "<p style='color: black; font-weight: bold'>" . "Error while communicating with the server" . "</p>";
    return;
  }
  if (($res = $conn->readLine()) === FALSE)
  {
    echo "<p style='color: black; font-weight: bold'>" . "Error while communicating with the server" . "</p>";
    return;
  }
  $res = removeBeginning($res, 'SERVERMSG ');
  $temp = strpos($res, "Last user ID for <" . $username . "> was ");
  if ($temp === FALSE) {
    echo "<p style='color: black; font-weight: bold'>" . "Error while communicating with the server" . "</p>";
    return;
  } else
    $userid = removeBeginning($res, "Last user ID for <" . $username . "> was ");

  if ($userid != "0") {
    printError("User has already an ID associated, installing it again would override the old one. Operation cancelled. (note: if you want to override the current ID anyway, you can still do it manually)");
    echo "<br /><br />";
    goBackButton();
    return;
  }

  if (($res = $conn->sendLine("ISONLINE " . $username)) !== TRUE)
  {
    echo "<p style='color: black; font-weight: bold'>" . "Error while communicating with the server" . "</p>";
    return;
  }
  if (($res = $conn->readLine()) === FALSE)
  {
    echo "<p style='color: black; font-weight: bold'>" . "Error while communicating with the server" . "</p>";
    return;
  }

  if ($res != "OK")
  {
    echo "<p style='color: black; font-weight: bold'>" . "User is currently offline. Please try again when user is online. Operation cancelled." . "</p>";
    goBackButton();
    return;
  }
  
  if (($res = $conn->sendLine("GENERATEUSERID " . $username)) !== TRUE)
  {
    echo "<p style='color: black; font-weight: bold'>" . "Error while communicating with the server" . "</p>";
    return;
  }
  if (($res = $conn->readLine()) === FALSE)
  {
    echo "<p style='color: black; font-weight: bold'>" . "Error while communicating with the server" . "</p>";
    return;
  }

  if ($res != "OK")
  {
    echo "<p style='color: black; font-weight: bold'>" . "Error while communicating with the server" . "</p>";
    return;
  }
  
  // close connection with server:
  $conn->close();

  echo "<p style='color: black; font-weight: bold'>" . "Request to generate User ID for user <" . $username . "> has been dispatched. You will be notified of success via server notification system." . "</p>";
  goBackButton();

?>

<?php require("inc/footer.php") ?>