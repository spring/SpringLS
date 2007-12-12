<?php
  // this page is used (included) with head.php page. Don't include it with any other page!

  global $restrictions;

  $restricted_page = basename($_SERVER['PHP_SELF']);
  $access = $restrictions[$restricted_page];
  $append = "";

  switch ($access) {
  case 0:
     // This should not really happen - page is not restricted at all (we probably forgot to specify access level for this page)! 
     // If we came here that must mean that global $restrict_default is set to true, hence we completely restrict any access to this page.
     $append = " (if you are an administrator, please set up access level for this page)";
     break;
  case 1:
     $append = " (you need to be <span style='color: blue; font-weight: bold'>logged in</span>)";
     break;
  case 2:
     $append = " (you need to be logged in as a server <span style='color: blue; font-weight: bold'>moderator</span>)";
     break;
  case 3:
     $append = " (you need to be logged in as a server <span style='color: blue; font-weight: bold'>administrator</span>)";
     break;
  }

  echo "<p>Access to this page (<span style='color: black; font-weight: bold'>" . $restricted_page . "</span>) is <span style='color: red; font-weight: bold'>restricted</span>" . $append . ".</p>";
?>