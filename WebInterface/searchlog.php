<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <title>TASServer system notifications</title>
    <link rel="stylesheet" type="text/css" href="style.css" />
  </head>
  <body>
  
  <!--
    <label for="search_input_id">Type in username or part of username you want to retrieve log for:</label>
    <input type="text" style="font-size: 11px" name="search_input" id="search_input_id" size="10" accesskey="s" tabindex="101" value="User Name" onfocus="if (this.value == 'User Name') this.value = '';" />
  -->

  <?php
    function displayLog($username)
    {
//      $filename = "/home/betalord/ChanServ/logs/#main.log";
      $filename = "/home/betalord/#main.log";
      print "<p>Log for: \"{$username}\"</p>";
      print "<p>Using file {$filename}.</p>";
      print "<br>";

/*
      if (!file_exists($filename))
      {
        echo "Error: $filename does not exist! Unable to retrieve log.";
        return false;
      }

      $count = 0;
      $handle = @fopen($filename, "r");
      if ($handle)
      {
         while (!feof($handle)) {
           $buffer = fgets($handle, 1024);

           if (strpos($buffer, $username) !== false)
           {
             $count += 1;
             echo htmlspecialchars(rtrim($buffer)) . "<br>\n";
             if ($count % 100 == 0)
             {
               ob_flush();
               flush();
             }
           }
         }
      }
      fclose($handle);

      if ($count == 0)
      {
        echo "No mathing lines have been found! Try searching using different keyword.";
        return false;
      }

      echo "<br> $count lines mathing search criteria. <br> End of file.";
*/


//      passthru("./a.out " . $filename . " " . $username);
      $handle = popen("./searchlog " . $filename . " \"" . $username . "\"", "r");
      while (!feof($handle))
      {
        $read = fgets($handle, 1024);
        echo htmlspecialchars(rtrim($read)) . "<br>";
      }
      pclose($handle);

      echo "<br> End of stream!";

      //*** Trenutno ne dela z velikimi fajli, recimo #main,
      //*** prav tako moram naštimat zadevo da sproducira utf-8, ker se šumnikov ne vidi.
      //*** dodaj "hint" na koncu, naj ljudje uporabijo "nickname>" convention!
      //*** v notifs.php poštimi, ker frame='box' ne dela. Primer je tu: http://www.w3schools.com/html/tryit.asp?filename=tryhtml_table_frame,
      //***   sicer pa za združevanje celic (kul pomoje, lahko uporabim): http://www.w3schools.com/html/tryit.asp?filename=tryhtml_table_span
    }

    if ($_POST['username']) {
      displayLog($_POST['username']);
    } else {
      print "<form action='test.php' method='post'>";
      print "Username (or part of): <input type='text' name='username' />";
      print "<input type='submit' />";
      print "</form>";
    }

  ?>


  </body>
</html>