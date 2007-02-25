<?php require("inc/head.php") ?>

<?php
  global $constants;

  function displayEditForm() {
    global $constants;

    echo "<p>Through this page you can edit the MOTD (Message of the day) displayed by the server when
          user logs in. The standard info fields are static and cannot be edited here.</p>";
    echo "<p>Make sure lines are not too long or they will word wrap in the lobby client. Message length
          is unlimited.</p>";

    // load the file:
    $lines = file($constants['motd_file']);
    if ($lines === FALSE) {
      echo "<span style='color: red; font-weight: bold;'>MOTD file was not found. New MOTD file will be created once you click SUBMIT (PHP must have rights to create new files set!).</span>";
      echo "<br /> <br />";
    }

    echo "<form method='post' action='{$PHP_SELF}'>";
    echo 'Edit MOTD:<br>';
    echo '<textarea name="motd" cols=60 rows=15 wrap=off>';
    if ($lines !== FALSE) {
      foreach ($lines as $line_num => $line) {
         echo rtrim($line, "\r\n") . "\n"; // note that rtrim's second argument is a list of chars, not the exact keyword to be removed from the end of the line
      }
    }
    echo '</textarea>';
    echo '<p><input type=SUBMIT value="Submit"></p>';
    echo '</FORM>';

    echo "<br />";
    echo "<p>Click <a href='editmotd.apply.php?goback=" . basename($_SERVER['PHP_SELF']) . "'>here</a> to force server-side update. This will force server to reload motd.txt file from the disk.</p>";
  }

  function applyAndDisplayOKForm() {
    global $constants;

    if (!file_exists($constants['motd_file'])) {
      $fh = fopen($constants['motd_file'], 'w');

      if ($fh === FALSE) {
        printError("Unable to create file \"" . $constants['motd_file'] . "\". Make sure PHP has neccessary rights to create files.");
        return ;
      }

      fwrite($fh, $_POST['motd']);
      fclose($fh);
      echo "MOTD file has been successfully created and written to! <br /> You must now force server to reload the file by issuing UPDATEMOTD command. You can do this from this page (after clicking OK).";
    } else {
      $fh = fopen($constants['motd_file'], 'w');

      if ($fh === FALSE) {
        printError("Unable to open file for writing: \"" . $constants['motd_file'] . "\". Make sure PHP has neccessary rights to write files.");
        return ;
      }

      fwrite($fh, $_POST['motd']);
      fclose($fh);
      echo "MOTD file has been successfully written to! <br /> You must now force server to reload the file by issuing UPDATEMOTD command. You can do this from this page (after clicking OK).";
    }
  }

  if (isset($_POST['motd'])) {
    applyAndDisplayOKForm();
    echo "<br /> <br />";
    echo "<a class='button1' href='{$PHP_SELF}'>OK</a>";
  } else {
    displayEditForm();
  }

?>

<?php require("inc/footer.php") ?>