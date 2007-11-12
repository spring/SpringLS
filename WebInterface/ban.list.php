<?php require("inc/head.php") ?>

<?php
  // useful link: http://www.databasejournal.com/features/mysql/article.php/10897_1469211_2
  
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
  
  function displayBanEntry($row)
  {
    if ($row{'IP_start'} == 0) 
      $display_ip = "none";
    else
      $display_ip = LONG2IP_($row{'IP_start'}).' - '.LONG2IP_($row{'IP_end'});

    echo '<tr>';
    echo '<td>'.$row{'ID'}.'</td>'; 
    echo '<td>'.$row{'Owner'}.'</td>'; 
    echo '<td>'.$row{'Date'}.'</td>'; 
    echo '<td>'.$row{'ExpirationDate'}.'</td>'; 
    echo '<td>'.$row{'Username'}.'</td>';
    echo '<td>'.$display_ip.'</td>'; 
    echo '<td>'.$row{'userID'}.'</td>';
    echo '<td>'.$row{'PrivateReason'}.'</td>';
    echo '<td>'.$row{'PublicReason'}.'</td>';
    echo '</tr>';
  }

  $select = "SELECT ID, Owner, Date, ExpirationDate, Username, IP_start, IP_end, userID, PrivateReason, PublicReason FROM BanEntries WHERE Enabled=1";
    
  // connect to the database:
  // (Done after this tutorial: http://www.databasejournal.com/features/mysql/article.php/1469211)
  $dbh = mysql_connect($constants['database_url'], $constants['database_username'], $constants['database_password']) 
    or returnError("Unable to connect to the database.");
  $selected = mysql_select_db($constants['database_name'], $dbh) 
    or returnError("Problems connecting to the database.");
 
  // issue a query:
  $result = mysql_query($select);
  
  // display results:
  echo '<table class="table4" id="T1">';
  echo '<tr>';
  echo '<th>ID</th>'; 
  echo '<th>Owner</th>'; 
  echo '<th>Date added</th>'; 
  echo '<th>Expiration date</th>'; 
  echo '<th>Username</th>'; 
  echo '<th>IP</th>';
  echo '<th>userID</th>';
  echo '<th>Private reason</th>';
  echo '<th>Public reason</th>';
  echo '</tr>';  
  while ($row = mysql_fetch_array($result, MYSQL_ASSOC)) {
    displayBanEntry($row);
  } 
  echo "</table>";
  
  mysql_close($dbh);
  
  echo "<br />";
  
  goBackButton();
  
?>

<?php require("inc/footer.php") ?>