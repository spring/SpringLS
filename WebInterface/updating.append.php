<?php require("inc/head.php") ?>

<?php

  function goBackButton()
  {
    $goback = $_GET['goback'];
    if ($goback == "") $goback = "updating.php";
    echo "<a class='button1' href='$goback'>OK</a>";
  }

  // returns true if successful, or false otherwise
  function addEntry($trigger, $action)
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

    $parent_node = $ctx->xpath_eval("/properties");
    $parent_node = $parent_node->nodeset[0];

    /* see examples at: 
       http://www.php.net/manual/en/function.domnode-append-child.php 
       http://www.php.net/manual/en/function.domdocument-create-text-node.php
    */
    $new_element = $doc->create_element("entry");
//    $new_node = $last_node->append_child($new_element);
    $new_node = $parent_node->append_child($new_element);
    $new_node->set_attribute("key", $trigger);
    $new_node->set_content(htmlspecialchars($action));
    
    $new_text_node = $doc->create_text_node("\n");
    $parent_node->append_child($new_text_node);


    // now save the modified xml document:
    $res = $doc->dump_file($constants['updates_file'], false, false);
    if ($res == 0) {
      printError("<p>Unable to write to 'updates.xml'. Make sure file has the permission to write set to true.</p>");
      goBackButton();
      return false;
    }

    return true;
  } // addEntry()


  // page begins here:

  $res = addEntry($_POST['trigger'], $_POST['action']);

  if ($res == true) {
    echo "<p>Entry has been successfully added.</p>";
    goBackButton();
  } else {
    // do nothing - error msg has already been printed by addEntry() function!
  }

?>

<?php require("inc/footer.php") ?>