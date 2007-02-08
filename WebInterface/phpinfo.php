<?php
  /* this page is a bit different from the rest of the site. It doesn't not include
     header or footer, so we must call session_start() ourselves. This is so because
     a call to phpinfo() outputs complete html document, not just the tables. */

  require("inc/functions.php");
  startSessionProperly();
  if (checkAccess() == false)
  {
    require("inc/restricted.inc");
    exit;
  }


  phpinfo();
?>