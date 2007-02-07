<?php
  // As an argument you should specify the "goback" string, which must be a filename of the
  // page to which it should redirect (withouth any "/" characters, just the filename, like "index.php")

  include("inc/functions.php");
  
  logout();

  $goback = $_GET['goback'];
  if ($goback == "") $goback = "index.php";
  redirect($goback);
?>