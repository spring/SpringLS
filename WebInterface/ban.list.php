<?php require("inc/head.php") ?>

<?php
  // useful link: http://www.databasejournal.com/features/mysql/article.php/10897_1469211_2
  
  function deleteButton($id)
  {
    return "<a class='button1' href='ban.delete.php?id={$id}'>Delete</a>";
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
  
  function displayBanEntryAsTable($row)
  {
    if ($row{'IP_start'} == 0) 
      $display_ip = ""; // none
    else if ($row{'IP_start'} == $row{'IP_end'})
      $display_ip = "IP: " . LONG2IP_($row{'IP_start'});
    else
      $display_ip = "IP range: " . LONG2IP_($row{'IP_start'}).' - '.LONG2IP_($row{'IP_end'});

    echo '<table class="table4" width=500px>';
    
    echo '  <tr>';
    echo '    <th>#'.$row{'ID'}.'</th>'; 
    echo '    <th>'.$row{'Owner'}.'</th>'; 
    echo '    <th>Added: '.$row{'Date'}.'</th>'; 
    echo '    <th><div align=right>'.deleteButton($row{'ID'}).'</div></th>'; 
    echo '  </tr>';      

    echo '  <tr>';
    echo '    <td colspan="4" class="font2">Expiration date: '.($row{'ExpirationDate'} != "" ? $row{'ExpirationDate'} : "indefinite").'</td>'; 
    echo '  </tr>';

    if ($row{'Username'} != "") {
      echo '  <tr>';
      echo '    <td colspan="4" class="font1"> Username: '.$row{'Username'}.'</td>';
      echo '  </tr>';
    }
    
    if ($display_ip != "") {
      echo '  <tr>';
      echo '    <td colspan="4" class="font1">'.$display_ip.'</td>';
      echo '  </tr>';
    }

    if ($row{'userID'} != 0) {
      echo '  <tr>';
      echo '    <td colspan="4" class="font1">User ID: '.$row{'userID'}.'</td>';
      echo '  </tr>';    
    }

    echo '  <tr>';
    echo '    <td colspan="4">';
    echo '      Private reason:';
    echo '      <table class="table4" width=470px align=center>';
    echo '        <tr><td>'.'<pre>'.$row{'PrivateReason'}.'</pre>'.'</td></tr>';
    echo '      </table>';
    echo '    </td>';
    echo '  </tr>';

    echo '  <tr>';
    echo '    <td colspan="4">';
    echo '      Public reason:';
    echo '      <table class="table4" width=470px align=center>';
    echo '        <tr><td>'.'<pre>'.$row{'PublicReason'}.'</pre>'.'</td></tr>';
    echo '      </table>';
    echo '    </td>';
    echo '  </tr>';
    
    echo '</table>';
  }  

  if ($_GET['view'] != "basic") {
    echo "<p>Click <a href='". basename($_SERVER['PHP_SELF'] . "?view=basic") . "'>here</a> for basic mode (single table).</p>";
  } else {
    echo "<p>Click <a href='". basename($_SERVER['PHP_SELF']) . "'>here</a> for full mode.</p>";
  }
  
  $select = "SELECT ID, Owner, Date, ExpirationDate, Username, IP_start, IP_end, userID, PrivateReason, PublicReason FROM BanEntries WHERE (Enabled=1 AND (ExpirationDate IS NULL OR ExpirationDate > CURRENT_TIMESTAMP))";
    
  // connect to the database:
  // (Done after this tutorial: http://www.databasejournal.com/features/mysql/article.php/1469211)
  $dbh = mysql_connect($constants['database_url'], $constants['database_username'], $constants['database_password']) 
    or returnError("Unable to connect to the database.");
  $selected = mysql_select_db($constants['database_name'], $dbh) 
    or returnError("Problems connecting to the database.");
 
  // issue a query:
  $result = mysql_query($select);
  
  // display results:
  if ($_GET['view'] == "basic") {
    echo '<table class="table4">';
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
  } else {
    while ($row = mysql_fetch_array($result, MYSQL_ASSOC)) {
      displayBanEntryAsTable($row);
      echo "<br />";
    }   
  }
  
  mysql_close($dbh);
  
  echo "<br />";
  
  goBackButton();
  
?>

<?php require("inc/footer.php") ?>