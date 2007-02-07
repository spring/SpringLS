<?php require("inc/head.php") ?>

<?php

  function goBackButton()
  {
    $goback = $_GET['goback'];
    if ($goback == "") $goback = "updating.php";
    echo "<a class='button1' href='$goback'>OK</a>";
  }

  // returns true if successful, or false otherwise
  function deleteEntry($entryHash)
  {
    global $constants;
    
    // load xml file and parse it:

    // read xml file into memory
    if (!$doc = domxml_open_file($constants['updates_file'])) {
      printError("<p>Unable to find 'updates.xml'. Action cancelled.</p>");
      goBackButton();
      return false;
    }

    //create new context
    $ctx = $doc->xpath_new_context();

    //get root node of 'entry' elements:
    $nodes = $ctx->xpath_eval("/properties");
    $nodes = $nodes->nodeset[0];
    foreach ($nodes->child_nodes() as $node) {
      if ($node->node_type() != XML_ELEMENT_NODE) continue;
      $key = $node->attributes();
      $key = $key[0];

      $trigger = $key->value();
      $action = $node->get_content();
      $hash = md5($trigger . "|" . $action);

      if ($hash == $entryHash) {
        // we found the entry which we must delete, lets remove it now and save the xml back to disk:

        $nodes->remove_child($node->next_sibling());
        $nodes->remove_child($node);

        // now save the modified xml document:
        $res = $doc->dump_file($constants['updates_file'], false, false);
        if ($res == 0) {
          printError("<p>Unable to write to 'updates.xml'. Make sure file has the permission to write set to true.</p>");
          goBackButton();
          return false;
        }

        return true;
      }


    }

    printError("<p>Unable to find entry. No entry has been deleted.<p>");
    goBackButton();
    return false;
  } // deleteEntry()

  // page begins here:

  $del = $_GET['del'];
  $res = deleteEntry($del);

  if ($res == true) {
    echo "<p>Entry has been successfully deleted.</p>";
    goBackButton();
  } else {
    // do nothing - error msg has already been printed by deleteEntry() function!
  }

?>

<?php require("inc/footer.php") ?>