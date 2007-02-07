<?php require("inc/head.php") ?>

<?php

  function goBackButton()
  {
    $goback = $_GET['goback'];
    if ($goback == "") $goback = "updating.php";
    echo "<a class='button1' href='$goback'>OK</a>";
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

    if (($res = $conn->sendLine("queryserver RELOADUPDATEPROPERTIES")) !== TRUE)
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
    echo "<p><span style='color: blue; font-weight: bold'>TASServer response:</span> $res</p>";

    // close connection with server:
    $conn->close();
  }

  forceUpdate();
  goBackButton();
?>

<?php require("inc/footer.php") ?>