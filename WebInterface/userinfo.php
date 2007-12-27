<?php require("inc/head.php") ?>

<?php

  function displayInputForm()
  {
    echo "<form action='{$PHP_SELF}' method='post'>";
    echo '<table class="table4">';
    echo '<tr>';
    echo '  <td>';
    echo "  Info for (type in username): <input type='text' name='username' />";
    echo "  <input type='submit' value='Submit' name='submit' />";
    echo '  </td>';
    echo '</tr>';
    echo '</table>';
    echo "</form>";
  }

  function displayResultForm($username)
  {
    displayUserInfo($username);
  }

  function displayUserInfo($username)
  {
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

    if (($res = $conn->sendLine("queryserver GETREGISTRATIONDATE " . $username)) !== TRUE)
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
    // an example line: "Registration timestamp for <Betalord> is 0 (1 jan 1970 01:00:00 CET)"
    // an example line: "User <test> not found!"
    if ($res == "User <" . $username . "> not found!") {
      echo "<p style='color: black; font-weight: bold'>" . "User &#60;" . $username . "&#62; not found" . "</p>";
      return;
    }
    $res = removeBeginning($res, "Registration timestamp for <" . $username . "> is ");
    $temp = explode(" ", $res);
    if ($temp[0] == 0)
      $registrationDate = 'before 1st Jan 2006';
    else
      $registrationDate = date("Y-m-d D H:i:s", $temp[0] / 1000); // java timestamp (unix timestamp + milliseconds)

    if (($res = $conn->sendLine("queryserver GETINGAMETIME " . $username)) !== TRUE)
    {
      echo "<p style='color: black; font-weight: bold'>" . "Error while communicating with the server" . "</p>";
      return;
    }
    if (($res = $conn->readLine()) === FALSE)
    {
      echo "<p style='color: black; font-weight: bold'>" . "Error while communicating with the server" . "</p>";
      return;
    }

    // an example line: "Betalord's in-game time is 7362 minutes."
    $res = removeBeginning($res, 'SERVERMSG ');
    $res = removeBeginning($res, $username . "'s in-game time is ");
    $temp = explode(" ", $res);
    $ingame = $temp[0]; // in minutes

    if (($res = $conn->sendLine("queryserver GETLASTIP " . $username)) !== TRUE)
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
    // an example line: "Betalord's last IP was 192.168.190.1 (online)"
    $res = removeBeginning($res, $username . "'s last IP was ");
    $temp = explode(" ", $res);
    $lastip = $temp[0];
    
    if (($res = $conn->sendLine("queryserver GETLASTLOGINTIME " . $username)) !== TRUE)
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
    // an example line: "<Betalord> is currently online"
    // an example line: "<test>'s last login was on 30 jan 2007 23:13:10 CET"
    if ($res == "<" . $username . "> is currently online")
      $lastLogin = "Currently online";
    else {
      $res = removeBeginning($res, "<" . $username . ">'s last login was on ");
      $lastLogin = $res;
    }
    
    if (($res = $conn->sendLine("GETACCESS " . $username)) !== TRUE)
    {
      echo "<p style='color: black; font-weight: bold'>" . "Error while communicating with the server" . "</p>";
      return;
    }
    if (($res = $conn->readLine()) === FALSE)
    {
      echo "<p style='color: black; font-weight: bold'>" . "Error while communicating with the server" . "</p>";
      return;
    }
    switch ($res) {
    case '1':
       $access = 'normal user';
       break;
    case '2':
       $access = 'moderator';
       break;
    case '3':
       $access = 'administrator';
       break;
    default:
      $access = 'unknown (error?)';
    }

    // close connection with server:
    $conn->close();

    // now display all acquired information:
    echo "<table class='table1' border='0' cellpadding='3' width='400'>";
    echo "  <tr>";
    echo "    <td>";
    echo "      User: ";
    echo "    </td>";
    echo "    <td>";
    echo "      " . $username;
    echo "    </td>";
    echo "  </tr>";
    echo "  <tr>";
    echo "    <td>";
    echo "      Access type: ";
    echo "    </td>";
    echo "    <td>";
    echo "      " . $access;
    echo "    </td>";
    echo "  </tr>";
    echo "  <tr>";
    echo "    <td>";
    echo "      Registration date: ";
    echo "    </td>";
    echo "    <td>";
    echo "      " . $registrationDate;
    echo "    </td>";
    echo "  </tr>";
    echo "  <tr>";
    echo "    <td>";
    echo "      In-game time: ";
    echo "    </td>";
    echo "    <td>";
    echo "      " . $ingame . " minutes (" . (floor(($ingame / 60) * 100 + .5) * .01) . " hours)";
    echo "    </td>";
    echo "  </tr>";
    echo "  <tr>";
    echo "    <td>";
    echo "      Last ip: ";
    echo "    </td>";
    echo "    <td>";
    echo "      " . $lastip;
    echo "    </td>";
    echo "  </tr>";
    echo "  <tr>";
    echo "    <td>";
    echo "      Last login time: ";
    echo "    </td>";
    echo "    <td>";
    echo "      " . $lastLogin;
    echo "    </td>";
    echo "  </tr>";
    echo "</table>";
  }


  // page contents begin here:

  $username = $_POST['username'];
  if ($username == "") $username = $_GET['username'];
  if ($username == "")
    displayInputForm();
  else {
    displayInputForm();
    echo "<br><br>";
    displayResultForm($username);
  }


?>

<?php require("inc/footer.php") ?>