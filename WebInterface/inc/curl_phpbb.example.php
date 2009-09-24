<?php
/*

// Init class
include('curl_phpbb.class.php');

// The ending backslash is required.
$phpbb = new curl_phpbb('http://www.phpbb.com/phpBB/');

// Log in
$phpbb->login('username', 'password');

// Send random_user a pm
$r = $phpbb->new_pm('random_user', print_r($_SERVER, true), 'Hello user...');
echo ( $r ) ? 'Posted' : 'Failed';

// Post a topic
$r = $phpbb->new_topic('16', 'This is just a test post!', 'Topic subject');
echo ( $r ) ? 'Posted' : 'Failed';

// Post a topic
$r = $phpbb->topic_reply('343298', 'This is just a test post!', 'Post subject');
echo ( $r ) ? 'Posted' : 'Failed';

// Read index
echo $phpbb->read('index.php');

// Log out
$phpbb->logout();

*/
?>