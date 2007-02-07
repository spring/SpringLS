<?php require("inc/head.php") ?>

<?php
  echo "<h2>Update manager</h2>";

  function displayEntry($trigger, $action)
  {
    $hash = md5($trigger . "|" . $action);

    print "<table class='table2' width='80%'>";
    print "  <th class='nopad' width='20%'>";
    print "  Trigger";
    print "  </th>";
    print "  <th class='nopad' width='80%'>";
    print "  Action";
    print "  </th>";
    print "  <th class='nopad' width='100'>";
    print "  <a class='button1' href='updating.delete.php?goback=" . basename($_SERVER['PHP_SELF']) . "&del=$hash'>Delete</a>";

/*
    echo "  <form action='{$PHP_SELF}' method='post'>";
//      echo "<input type='submit' value='Delete' name='submit' />";

    echo '<BUTTON name="submit" value="submit" type="submit">';
    echo 'Send<IMG src="/icons/wow.gif" alt="wow"></BUTTON>';

    echo "  </form>";
*/
/*
    echo '<FORM action="http://somesite.com/prog/adduser" method="post">';
    echo '   <P>';
    echo '   <LABEL for="firstname">First name: </LABEL>';
    echo '             <INPUT type="text" id="firstname"><BR>';
    echo '   <LABEL for="lastname">Last name: </LABEL>';
    echo '             <INPUT type="text" id="lastname"><BR>';
    echo '   <LABEL for="email">email: </LABEL>';
    echo '             <INPUT type="text" id="email"><BR>';
    echo '   <INPUT type="radio" name="sex" value="Male"> Male<BR>';
    echo '   <INPUT type="radio" name="sex" value="Female"> Female<BR>';
    echo '   <INPUT type="submit" value="Send"> <INPUT type="reset">';
    echo '   </P>';
    echo '</FORM>';
*/
    print "  </th>";
    print "  <tr>";
    print "    <td width='150' valign='top'>";
    print "    {$trigger}";
    print "    </td>";
    print "    <td colspan='2'>";
    print "    {$action}";
    print "    </td>";
    print "  </tr>";
    print "</table>";
  }
  
  function displayNewEntryForm()
  {
    echo "<form action='updating.append.php?goback=" . basename($_SERVER['PHP_SELF']) . "' method=post>";
    echo "<table cellspacing='5' frame='box' rules=none style='border-collapse: collapse; background-color: #ccccff'>";
    echo "  <tr>";
    echo "    <td colspan='2'>";
    echo "      <b>Add new entry:</b>";
    echo "    </td>";
    echo "  </tr>";
    echo "  <tr>";
    echo "    <td colspan='2'>";
    echo "      <font face='verdana, arial, helvetica' size='2'>Trigger:</font>";
    echo "    </td>";
    echo "    <td colspan='2'>";
    echo "      <input type='text' name='trigger'>";
    echo "    </td>";
    echo "  </tr>";
    echo "  <tr>";
    echo "    <td colspan='2'>";
    echo "      <font face='verdana, arial, helvetica' size='2'>Action:</font>";
    echo "    </td>";
    echo "    <td colspan='2'>";
    echo "      <input type='text' name='action'>";
    echo "    </td>";
    echo "  </tr>";
    echo "  <tr>";
    echo "    <td colspan='2'>";
    echo "      <input type='submit' value='Add entry'>";
    echo "    </td>";
    echo "  </tr>";
    echo "</table>";
    echo "</form>";
  }

  // XML DOM examples: http://chregu.tv/domxml/

  global $constants;

  // read xml file into memory
  if (!$doc = domxml_open_file($constants['updates_file'])) {
    printError("Unable to find 'updates.xml'. Action cancelled.");
    exit;
  }

  //create new context
  $ctx = $doc->xpath_new_context();

  //get all 'entry' nodes
  $nodes = $ctx->xpath_eval("//entry");

  echo "<br />";
  foreach ($nodes->nodeset as $node) {
    $key = $node->attributes();
    $key = $key[0];

    displayEntry($key->value(), $node->get_content());
    echo "<br />";
  }

  echo "<br />";
  displayNewEntryForm();

  echo "<br />";
  echo "<p>Click <a href='updating.apply.php?goback=" . basename($_SERVER['PHP_SELF']) . "'>here</a> to force server-side update. This will force server to reload updates.xml file from the disk.</p>";
  echo "<p>To download updates.xml, click <a href='" . $constants['updates_file'] . "'>here</a>.</p>";

?>

<?php require("inc/footer.php") ?>