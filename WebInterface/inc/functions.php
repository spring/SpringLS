<?php
  require("constants.inc");
  require("classes.inc");

  /* check if user is authorized to view current page based on his access status.
     Returns true if user is authorized to view it, or false otherwise. */
  function checkAccess()
  {
    global $restrictions;
    return (intval($restrictions[basename($_SERVER['PHP_SELF'])]) > intval($_SESSION['access'])) ? false : true;
  }

  // copied from http://www.php.net/header. Note that the script that calls this function shouldn't output any data before it calls it!
  function redirect($page) {
    /* Redirect to a different page in the current directory that was requested */
    $host  = $_SERVER['HTTP_HOST'];
    $uri  = rtrim(dirname($_SERVER['PHP_SELF']), '/\\');
    header("Location: http://$host$uri/$page");
    exit; /* Make sure that code below does not get executed when we redirect. */
  }

  // copied from http://www.php.net/header
  function refreshCurrentPage() {
    header( "Location: http" .
    (isset($_SERVER['HTTPS']) && $_SERVER['HTTPS']
    == "on" ? "s" : "") . "://" .
    $_SERVER['SERVER_NAME'] . ":" . $_SERVER['SERVER_PORT'] .
    ( isset($_SERVER['REQUEST_URI']) ? $_SERVER
    ['REQUEST_URI'] .
    ( $_SERVER['QUERY_STRING'] ? "?" . $_SERVER
    ['QUERY_STRING'] : "" ) : "" ) );
  }

  // copied from: http://www.php.net/manual/en/function.md5.php
  function md5_base64($data)
  {
     return base64_encode(pack('H*',md5($data)));
  }

  //copied from http://www.php.net/strings
  function beginsWith( $str, $sub ) {
    return ( substr( $str, 0, strlen( $sub ) ) === $sub );
  }

  //copied from http://www.php.net/strings
  function endsWith( $str, $sub ) {
    return ( substr( $str, strlen( $str ) - strlen( $sub ) ) === $sub );
  }
  
  // removes a beginning of a string. If 'beginning' doesn't match the actual beginning of the string, it will simply cut off length(beginning) chars
  function removeBeginning($string, $beginning)
  {
    return substr($string, strlen($beginning));
  }

  function isValidIP($ip)
  {
    // pattern copied from: http://smart-pad.blogspot.com/2006/07/regular-expression-for-valid-ip.html
    $ipPattern = 
     '/\b(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.' .
     '(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.' .
     '(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.' .
     '(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\b/';

     return preg_match($ipPattern, $ip) == 1;
  }
  
  /* this will try to login current visitor assigning all needed session variables etc. 
     Some tutorials / sample code:
     * http://www.litfuel.net/tutorials/sockets/sockets.php
     * http://www.php.net/sockets
  */
  function login($username, $password)
  {
    global $constants;

    $socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
    $result = socket_connect($socket, $constants['remote_address'], $constants['remote_port']);
    if ($result === false) {
      $_SESSION['error'] = "socket_connect() failed.\nReason: ($result) " . socket_strerror(socket_last_error($socket));
      return;
    }

    $in = "identify " . $constants['access_key'] . "\n";
    socket_write($socket, $in, strlen($in));

    // read the response:
//    while ($out = socket_read($socket, 2048)) {
//      echo $out;
//    }
    $out = trim(socket_read($socket, 2048));
    if ($out == 'FAILED') {
      $_SESSION['error'] = "Unable to acquire link with the server";
      return;
    } else if ($out != 'PROCEED')
    {
      $_SESSION['error'] = "Error while trying to connect to server";
      return;
    }

    $in = "TESTLOGIN " . $username . " " . md5_base64($password) . "\n";
    socket_write($socket, $in, strlen($in));
    $out = trim(socket_read($socket, 2048));
    if ($out == 'LOGINBAD')
    {
      $_SESSION['error'] = "Bad username/password";
      return;
    } else if ($out != 'LOGINOK')
    {
      $_SESSION['error'] = "Error in communication with server";
      return;
    }

    // get access level:
    $in = "GETACCESS " . $username . "\n";
    socket_write($socket, $in, strlen($in));
    $out = trim(socket_read($socket, 2048));
    if ($out == '0')
    {
      $_SESSION['error'] = "Error in communication with server (#2)";
      return;
    }
    if ((intval($out) < 1) || (intval($out) > 3))
    {
      $_SESSION['error'] = "Error in communication with server (#3)";
      return;
    }
    $access = $out;

    socket_close($socket);

    // ok everything seems fine, log the user in (create session data etc.):
    unset($_SESSION['error']);
    $_SESSION['username'] = $username;
    $_SESSION['access'] = $access;
    $_SESSION['last_timestamp'] = time(); // last time when user accessed some page. Used with manual timeout checking
  }

  // see http://www.captain.at/howto-php-sessions.php
  function startSessionProperly()
  {
/*
    ini_set('session.gc_maxlifetime', 10); // in seconds
    ini_set('session.gc_probability',1);
    ini_set('session.gc_divisor',1);
    Note: code above causes some ill side-effects, that is why I implemented custom timeout handling.
*/
    global $constants;

    session_start();
    if (time() - $_SESSION['last_timestamp'] > $constants['session_timeout_time'])
    {
      // recreate session
      session_destroy();
      session_start();
      $_SESSION['last_timestamp'] = time();
    }
    else
    {
      $_SESSION['last_timestamp'] = time();
    }
  }

  function logout()
  {
    //session_start(); // is this needed at all? Judging from here it is: http://www.tizag.com/phpT/phpsessions.php
    startSessionProperly();
    session_unset();
    session_destroy();
  }

  function loggedIn()
  {
    return isset($_SESSION['username']);
  }

  function displayLogin()
  {
    echo "<br>";
    if (!loggedIn())
    {
      if (isset($_SESSION['error']))
      {
        echo "<font face='verdana, arial, helvetica' size='2' color='red'>" . $_SESSION['error'] . "</font><br>";
      }

      echo "<form action='login.php?goback=" . basename($_SERVER['PHP_SELF']) . "' method=post>";
      echo "  <table class='logintable'>";
      echo "    <tr><td>";
      echo "      <font face='verdana, arial, helvetica' size='2'>Username:</font>";
      echo "    </td></tr>";
      echo "    <tr><td>";
      echo "      <input type ='username' name='username' style='width: 120px;'>";
      echo "    </td></tr>";
      echo "    <tr><td>";
      echo "      <font face='verdana, arial, helvetica' size='2'>Password:</font>";
      echo "    </td></tr>";
      echo "    <tr><td>";
      echo "      <input type ='password' name='password' style='width: 120px;'>";
      echo "    </td></tr>";
      echo "    <tr><td>";
      echo "      <input type='submit' value='Login'>";
      echo "    </td></tr>";
//      echo "    <tr><td style='text-align: left; margin-top: 20px'>";
//      echo "      <font face='verdana, arial, helvetica' size='2'>(Note: Use your lobby account to log in)</font>";
//      echo "    </td></tr>";
      echo "  </table>";
      echo "</form>";

      echo "<table style='text-align: left; margin-top: 20px; border: 1px solid #000000;'>";
      echo "  <tr><td>";
      echo "    <font size='2'>(Note: Use your lobby account to log in)</font>";
      echo "  </td></tr>";
      echo "</table>";

    }
    else
    {
      echo "<table class='logintable'>";
      echo "  <tr><td>";
      echo "    Welcome, " . $_SESSION['username'] . "!<br>";
      echo "  </td></tr>";
      echo "  <tr><td>";
      echo "    <form action='logout.php?goback=" . basename($_SERVER['PHP_SELF']) . "' method=post>";
      echo "    <input type='submit' value='Logout'><br>";
      echo "    </form>";
      echo "  </td></tr>";
      echo "</table>";

    }
  }
  
  function displayMaintenancePage() {
    echo '<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">';
    echo '<html>';
    echo '  <head>';
    echo '    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />';
    echo '    <title>TASServer Web Interface</title>';
    echo '  </head>';
    echo '  <body>';
    echo '   <h1 style="color: blue">Maintenance mode</h1>';
    echo '   <h3>The site is currently running in maintenance mode and is not accessible.</h3>';
    echo '   <h3>Please check again later!</h3>';
    echo '  </body>';
    echo '</html>';
  }

  function printError($error) {
    echo "<span style='color: red; font-weight: bold;'>$error</span>";
  }

?>
