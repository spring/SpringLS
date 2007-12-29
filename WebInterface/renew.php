<?php require("inc/head.php") ?>

<?php

  $explanation = "Since Spring 0.76 (lobby server version 0.35), all accounts have been transferred
                  to a database and need to be renewed in order to be reactivated again.
                  This is required because certain new restrictions have been applied to accounts.
                  After you will renew your account you won't be able to rename it anymore, however you will 
                  be able to set a nickname based on your username (username prefixed and/or postfixed with a 
                  clan tag or some other text) from within the lobby (or this page). This will ease tracking and 
                  uniquely identifying accounts making it easier to bind them with other external entities
                  (such as ladder sites, statistics tracking etc.).
                  When you renew your account your settings will be preserved.
                  <br />
                  <br />";

  function returnError($err_msg, $back = 1) 
  {
    printError($err_msg);
    echo "<br />";
    echo "<br />";
    goBackButton($back);
    exit();
  }

  function keywordBox () {
    echo '<table class="standard-table">';
    echo '<tr>';
    echo '  <td colspan="4">';
    echo '  <input type="checkbox" name="usekeyword" /> Only show lines that contain:';
    echo '  </td>';
    echo '</tr>';
    echo '<tr>';

    echo '  <td>';
    echo "  Keyword: <input type='text' name='keyword' />";
    echo '  </td>';

    echo '</tr>';

    echo '<tr>';
    echo '  <td>';
    echo '  Note: wildcards are not supported!';
    echo '  </td>';
    echo '</tr>';

    echo '</table>';
  }

  function displayRenewalForm()
  {
    echo "<h2>Account renewal</h2>";

    echo "<div style='width: 80%'>";

    global $explanation;
    echo $explanation;
    echo "<hr>";
    echo "<h2>Account renewal form</h2>";
    
    echo "<form action='{$PHP_SELF}' method='post'>";

    echo '<table class="table4">';

    echo '<tr>';
    echo '  <td>';
    echo "    Your old username: ";
    echo '  </td>';
    echo '  <td>';
    echo "    <span style='font-weight: bold'>";
    echo $_SESSION['username'];
    echo "    </span>";
    echo '  </td>';
    echo '</tr>';

    echo '<tr>';
    echo '  <td>';
    echo "    Your new username: ";
    echo '  </td>';
    echo '  <td>';
    echo "    <input type='text' name='new_username' value='" . $_SESSION['username'] . "' />";
    echo '  </td>';
    echo '</tr>';
    
    echo '<tr>';
    echo '  <td>';
    echo "    Your new nickname: ";
    echo '  </td>';
    echo '  <td>';
    echo "    <input type='text' name='new_nickname' value='" . $_SESSION['username'] . "' />";
    echo '  </td>';
    echo '</tr>';
    
    echo '</table>';

    echo "<br />";

    echo "Note: You will be able to change your nickname within the lobby client using /nick command.";

    echo "<br />";
    echo "<br />";

    echo "<span style='color: red; font-weight: bold'>";
    echo "IMPORTANT: Once you renew your account, you won't be able to rename it anymore. You will be
          able to choose a different nickname though, which will have to contain your username (optionally 
          prefixed and/or postfixed with some text)";
    echo "</span>";

    echo "<br />";
    echo "<br />";
    echo "<input type='submit' value='Renew my account' name='submit' />";
    echo "</form>";

    echo "</div>";
  }

  function displayConfirmationForm()
  {
    echo "<h2>Account renewal</h2>";

    echo "<div style='width: 80%'>";
    
    echo "Please confirm the changes:";

    echo "<form action='{$PHP_SELF}' method='post'>";

    echo '<table class="table4">';
    
    echo '<tr>';
    echo '  <td>';
    echo "    Your old username: ";
    echo '  </td>';
    echo '  <td>';
    echo "    <span style='font-weight: bold'>";
    echo $_SESSION['username'];
    echo "    </span>";
    echo '  </td>';
    echo '</tr>';

    echo '<tr>';
    echo '  <td>';
    echo "    Your new username: ";
    echo '  </td>';
    echo '  <td>';
    echo "    <span style='font-weight: bold; color: blue'>";
    echo $_POST['new_username'];
    echo "    </span>";
    echo '  </td>';
    echo '</tr>';
    
    echo '<tr>';
    echo '  <td>';
    echo "    Your new nickname: ";
    echo '  </td>';
    echo '  <td>';
    echo "    <span style='font-weight: bold; color: blue'>";
    echo $_POST['new_nickname'];
    echo "    </span>";
    echo '  </td>';
    echo '</tr>';
    
    echo '</table>';

    echo "<br />";
    echo "<input type='hidden' name='new_username_confirmed' value='" . $_POST['new_username'] . "'>";
    echo "<input type='submit' value='Renew my account' name='submit' />";
    echo "</form>";

    echo "</div>";
  }

  function displayCompletionForm()
  {
    echo "<h2>Account renewal</h2>";

    echo "<div style='width: 80%'>";

    if (!isset($_SESSION['verified_new_username'])) {
      printError("Error occured: new username is not defined in the session array. Please report this error!");
      echo "<a class='button1' href='javascript:history.go(-2)'>Go back</a>";
      return ;
    }

    if ($_POST['new_username_confirmed'] != $_SESSION['verified_new_username']) {
      // perhaps user is trying to forge a custom POST request, we must deny it
      printError("Error occured: new username doesn't match the one submited in POST form. Please try again or else report this error!");
      echo "<a class='button1' href='javascript:history.go(-2)'>Go back</a>";
      return ;
    }

    
    $dbh = mysql_connect($constants['database_url'], $constants['database_username'], $constants['database_password']) 
      or returnError("Unable to connect to the database. Error: " . mysql_error());
    $selected = mysql_select_db("spring", $dbh) 
      or returnError("Problems connecting to the database. Error: " . mysql_error());
   
    $select = "SELECT * FROM `OldAccounts` WHERE Username = '" . $_SESSION['username'] . "'";
    $result = mysql_query($select);
    if (!$result) {
      returnError("Problems accessing the database. Error: " . mysql_error());
    }
    $row = mysql_fetch_row($result);
    if ($row === false) {
      returnError("Problems accessing the database. Your username could not be found (this should not happen). Please report this error!");
    }
    
    if ($row[2] == 1) {
      returnError("Error: this account has already been renewed. Cancelling operation ...");
    }
    
    // really good introduction to mysql transactions: http://www.devshed.com/c/a/MySQL/Using-Transactions-In-MySQL-Part-1/
    
    transaction_begin();

    // check if such username already exists in Accounts table:
    $select = "SELECT * FROM Accounts WHERE Username='" . $_SESSION['verified_new_username'] . "'";
    $result = mysql_query($select);
    if (!$result) {
      returnError("Error while trying to query the database. MySQL error: " . mysql_error());
    }
    $row = mysql_fetch_row($result);
    if ($row !== false) {
      returnError("Error: account with username '" . $_SESSION['verified_new_username'] . "' already exists - please choose another username. Operation cancelled.", 2);
    }

    // check that user is not renaming to some other account (which is not his):
    $select = "SELECT * FROM OldAccounts WHERE Username='" . $_SESSION['verified_new_username'] . "'";
    $result = mysql_query($select);
    if (!$result) {
      returnError("Error while trying to query the database. MySQL error: " . mysql_error());
    }
    $row = mysql_fetch_row($result);
    if (($row !== false) && ($row[1] == 0 /* not renewed yet */) && ($_SESSION['verified_new_username'] != $_SESSION['username'])) {
      returnError("Error: account with username '" . $_SESSION['verified_new_username'] . "' already exists (but hasn't been renewed yet) - please choose another username. Operation cancelled.", 2);
    }

    $insert = "INSERT INTO Accounts (Username, Nickname, Password, AccessBits, RegistrationDate) values (";
    $insert .= "'" . $_SESSION['verified_new_username'] . "', '" . $_SESSION['verified_new_nickname'] . "', '" . $row[4] . "', " . $row[5] . ", " . $row[6] . ");";
    $update = "UPDATE OldAccounts SET Transferred=1, NewUsername='" . $_SESSION['verified_new_username'] . "' WHERE Username='" . $_SESSION['username'] . "';";
    
    if (!mysql_query($insert)) {
      returnError("Error while trying to modify the database. MySQL error: " . mysql_error());
    }
    
    if (!mysql_query($update)) {
      returnError("Error while trying to modify the database. MySQL error: " . mysql_error());
    }
    
    transaction_commit();
    
    mysql_close($dbh);
  
    logout();
  
    
    echo "Your account has been successfully updated to ";
    echo "<span style='font-weight: bold; color: blue'>";
    echo $_POST['new_username_confirmed'];
    echo "</span>";

    echo "<br />";
    echo "<br />";
    echo "Your password remains the same. You may now login via lobby client using this account. <br />";
    echo "Note: you have now been logged out from this site. You should login again if you want to perform any additional tasks.";

    echo "</div>";
  }

  function displayGeneralInfo()
  {
    global $explanation;
    
    echo "<h2>Account renewal</h2>";

    $text = $explanation . "<span style='color: blue; font-weight: bold;'>
                            In order to renew your account now, log in using your old lobby username and password at the menu
                            on the left.
                            </span>
                            ";
                           
    echo "<div style='width: 80%; color: black; font-weight: normal;'>$text</div>";
  }
  
  function displayAlreadyRenewedPage()
  {
    global $explanation;
    
    echo "<h2>Account renewal</h2>";

    $text = $explanation . "<span style='color: blue; font-weight: bold;'>
                            The account you are currently logged in has already been renewed.
                            </span>
                            ";
                           
    echo "<div style='width: 80%; color: black; font-weight: normal;'>$text</div>";
  }
  
  /* if validation fails it returns error description in &$error */
  function verifyNewUsernameAndNickname(&$error)
  {
    $user = $_POST['new_username'];
    $nick = $_POST['new_nickname'];

    if (strlen($user) < 2) {
      $error = "Username too short (minimum: 2 characters)";
      return false;
    } elseif (strlen($user) > 20) {
      $error = "Username too long (maximum: 20 characters)";
      return false;
    }

    if (strlen($nick) < 2) {
      $error = "Nickname too short (minimum: 2 characters)";
      return false;
    } elseif (strlen($nick) > 25) {
      $error = "Nickname too long (maximum: 25 characters)";
      return false;
    }

  	if (!preg_match('/^[A-Za-z0-9_]+$/', $user)) {
      $error = "Username contains invalid characters (valid regex is: '^[A-Za-z0-9_]+$')";
      return false;
  	} 
    
  	if (!preg_match('/^[A-Za-z0-9_\[\]\|]+$/', $nick)) {
      $error = "Nickname contains invalid characters (valid regex is: '^[A-Za-z0-9_\[\]\|]+$')";
      return false;
  	} 
    
		// check if prefix is valid:
  	if (!preg_match('/^([A-Za-z0-9\[\]\|]+[\|\]])?' . $user . '/', $nick)) {
      $error = "Invalid prefix found in nickname: embed your prefix in [] brackets or separate it by a | character";
      return false;
  	} 

		// check if postfix is valid:
  	if (!preg_match('/' . $user . '([\|\[][A-Za-z0-9\[\]\|]+)?$/', $nick)) {
      $error = "Invalid postfix found in nickname: embed your postfix in [] brackets or separate it by a | character";
      return false;
  	} 

		// check if prefix and postfix are both valid in one shot:
  	if (!preg_match('/^([A-Za-z0-9\[\]\|]+[\|\]])?' . $user . '([\|\[][A-Za-z0-9\[\]\|]+)?$/', $nick)) {
      $error = "Nickname contains invalid prefix/postfix. Your username should be contained in your nickname.";
      return false;
  	} 
    
    return true;
  }

  // page contents begin here:

  if (!loggedIn())
    displayGeneralInfo();
  else if ($_SESSION['renewed'] == 1) 
    displayAlreadyRenewedPage();
  else if (!isset($_POST['new_username']) && !isset($_POST['new_username_confirmed']))
    displayRenewalForm();
  else if (isset($_POST['new_username'])) {
    if (!verifyNewUsernameAndNickname(&$error)) {
      echo "<span style='color: red; font-weight: bold'> Error: " . $error . "</span>";
      echo "<br /><br />";
      echo "Some examples of (in)valid nicknames: <br />";
      echo "<span style='color: blue'>Username: Johnny</span> <br />";
      echo "Nickname: <span style='color: green'>Johnny</span> <br />";
      echo "Nickname: <span style='color: green'>[ClaN]Johnny</span> <br />";
      echo "Nickname: <span style='color: green'>MyClan|Johnny</span> <br />";
      echo "Nickname: <span style='color: green'>MyClan|Johnny[I_rule]</span> <br />";
      echo "Nickname: <span style='color: red'>Lucy</span> <br />";
      echo "Nickname: <span style='color: red'>_Johnny_</span> <br />";
      echo "Nickname: <span style='color: red'>somethingJohnny</span> <br />";
      echo "<br />";
      goBackButton();
    } else {
      $_SESSION['verified_new_username'] = $_POST['new_username'];
      $_SESSION['verified_new_nickname'] = $_POST['new_nickname'];
      
      displayConfirmationForm();
    }
  }
  else if (isset($_POST['new_username_confirmed'])) {
    displayCompletionForm();
    unset($_SESSION['verified_new_username']);
    unset($_SESSION['verified_new_nickname']);
  }
  else printError("Error occured on the page. Please report this error!");

?>

<?php require("inc/footer.php") ?>