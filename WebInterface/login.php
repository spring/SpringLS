<?php
  // As an argument you should specify the "goback" string, which must be a filename of the
  // page to which it should redirect (withouth any "/" characters, just the filename, like "index.php")

  require("inc/functions.php");

  startSessionProperly();

  if (!loggedIn())
  {
    login($_POST['username'], $_POST['password']);
  }

  $goback = $_GET['goback'];
  if ($goback == "") $goback = "index.php";
  redirect($goback);

?>