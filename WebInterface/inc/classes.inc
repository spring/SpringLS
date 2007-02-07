<?php
//  require("constants.inc"); --> must be included before including classes.inc file!

  /* some examples:
     http://www.devshed.com/c/a/PHP/Socket-Programming-With-PHP/4/       (see pages 5 and 6)
   */

  class ServerConnection {

    var $socket;

    function ServerConnection()
    {
      $this->socket = socket_create(AF_INET, SOCK_STREAM, SOL_TCP);
    }

    // returns TRUE if successful, or error string otherwise
    function connect()
    {
      global $constants;

      $result = socket_connect($this->socket, $constants['remote_address'], $constants['remote_port']);
      if ($result === false) {
        return "socket_connect() failed.\nReason: ($result) " . socket_strerror(socket_last_error($this->socket));
      }

      return true;
    }

    // tries to identify to the remote access server. Returns TRUE or FALSE based on success.
    function identify()
    {
      global $constants;

      if ($this->sendLine("identify " . $constants['access_key']) !== true) return false;
      if (($res = $this->readLine()) === false) return false;
      if ($res == 'FAILED') return false;
      if ($res != 'PROCEED') return false;
      return true;
    }

    // returns TRUE if successful, or error string otherwise
    function sendLine($line)
    {
      $line .= "\n";
      return (socket_write($this->socket, $line, strlen($line))) ? true : "Could not send data to server";
    }

    // returns the line read, or FALSE if unsuccessful
    function readLine()
    {
      $out = socket_read($this->socket, 2048);
      if ($out === false) return false;
      return trim($out);
    }

    // closes the connection
    function close()
    {
      socket_close($this->socket);
    }

  } // end of class ServerConnection

?>