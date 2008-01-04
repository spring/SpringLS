<?php
/*
	@ Program	: phpBB CURL Library
	@ Author	: Dean Newman, Afterlife(69)
	@ Purpose	: Remote login and posting in phpBB
	@ Filename	: curl_phpbb.class.php
	@ Class		: curl_phpbb
	@ About		: Function lib for login, posting and logout on remote phpbb's
	@ Licence	: GNU/General Public Licence v2
	@ Created	: 6/22/2006, 7:41pm
	@ Updated	: 9/01/2006, 5:19pm
	@ Updated	: 4/01/2008, 7:39pm
*/

/*
	@ Changelog
	@	9/01/2006: Added ->read() functionality.
  @	4/01/2008: (Betalord) -> modified the script to work with phpbb3 forum. Not all methods were fixed though (only login and post reply)
*/

require_once "HtmlFormParser.php";

class curl_phpbb
{
	/*
		@ Variable	: $curl (Resource)
		@ About		: The cURL object used for the request
		@ Type		: Private
	*/
	var $curl = null;

	/*
		@ Variable	: $cookie_name (String)
		@ About		: The filename of the temp file used for storing cookies
		@ Type		: Private
	*/
	var $cookie_name = array();

	/*
		@ Variable	: $phpbb_url (String)
		@ About		: The address of the remote phpbb that is being connected to
		@ Type		: Private
	*/
	var $phpbb_url = null;

	/*
		@ Variable	: $error (Array)
		@ About		: The array including error code and message on errors
		@ Type		: Public
	*/
	var $error = array();

	/*
		@ Function	: curl_phpbb() - Constructor
		@ About		: Check if CURL is available and the url exists.
		@ Type		: Public
	*/
	function curl_phpbb($phpbb_url, $cookie_name = 'tmpfile.tmp')
	{
		// Check CURL is present
		if ( ! function_exists ( 'curl_init') )
		{
			// Output an error message
			trigger_error('curl_phpbb::error, Sorry but it appears that CURL is not loaded, Please install it to continue.');
			return false;
		}
		if ( empty($phpbb_url) )
		{
			// Output an error message
			trigger_error('curl_phpbb::error, The phpBB location is required to continue, Please edit your script.');
			return false;
		}
		// Set base location
		$this->phpbb_url = $phpbb_url;
		// Create temp file
		$this->cookie_name = $cookie_name;
	}
  
  /* similar to topic_reply, only that it also parses the "post reply" form (needed for phpbb3) */
	function topic_reply_advanced($topic_id, $forum_id, $message, &$errormsg)
	{
		global $_SERVER;
  
    // get the "post a reply" page:
    $postReplyPage = $this->read('posting.php?mode=reply&f=' . $forum_id . '&t=' . $topic_id);
    if (!$postReplyPage) {
      $errormsg = "Unable to retrieve 'post a reply' page";
      return false;
    }
      
    $parser =& new HtmlFormParser($postReplyPage);
    $parsed = $parser->parseForms();
    
    // Generate post string
    $post_fields = $this->array_to_http(array(
      'post'				=> 'Submit',
      
      'f'		=> $forum_id,
      't'		=> $topic_id,
      
//			'mode'				=> 'reply',
      'message'			=> $message,
      
			'disable_smilies'	=> 1,
        
      // parsed:
      'topic_cur_post_id' => $parsed[0]['form_elemets']['topic_cur_post_id']['value'],
      'creation_time' => $parsed[0]['form_elemets']['creation_time']['value'],
      'form_token' => $parsed[0]['form_elemets']['form_token']['value'],
      'subject' => $parsed[0]['form_elemets']['subject'],
    ));

		// Location
		$url_vars = $this->array_to_http2(array(
			'mode'	=> 'reply',
      'f'		=> $forum_id,
			't'		=> $topic_id,
		));
		// Init curl
		$this->curl = curl_init();
		// Set options
		curl_setopt ( $this->curl, CURLOPT_URL, $this->phpbb_url . 'posting.php?' . $url_vars ); // curl_setopt ( $this->curl, CURLOPT_URL, $this->phpbb_url . substr($parsed[0]['form_data']['action'], 2));
		curl_setopt ( $this->curl, CURLOPT_POST, true );
		curl_setopt ( $this->curl, CURLOPT_POSTFIELDS, $post_fields );
		curl_setopt ( $this->curl, CURLOPT_RETURNTRANSFER, true );
		curl_setopt ( $this->curl, CURLOPT_HEADER, false );
		curl_setopt ( $this->curl, CURLOPT_COOKIE, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_COOKIEJAR, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_COOKIEFILE, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_USERAGENT, $_SERVER['HTTP_USER_AGENT'] );
		// Execute request
		$result = curl_exec ( $this->curl );
		// Get the result
		if ( preg_match ( '#This message has been posted successfully.#', $result, $match ) )
		{
			$post_status = 1;
		}
		else if ( preg_match ( '#You cannot make another post so soon after your last; please try again in a short while.#', $result, $match ) )
		{
			$post_status = 0;
		}
		else
		{
			$post_status = 0;
		}
		// Error handling
		if ( curl_errno ( $this->curl ) )
		{
			$this->error = array(
				curl_errno($this->curl),
				curl_error($this->curl),
			);
			curl_close ( $this->curl );
      $errormsg = implode(" - ", $this->error);
			return false;
		}
		// Close connection
		curl_close ( $this->curl );
		// Return result
    if ($post_status == 0) {
      $errormsg = "Unknown or invalid response to post submission (post may still have been written)";
      return false;
    }
    return true;
	}
  
	/*
		@ Function	: login() - Log In
		@ About		: Does a remote login to the target phpBB and stores in cookie
		@ Type		: Public
	*/
	function login($username, $password)
	{
		global $_SERVER;
	
		// Generate post string
		$post_fields = $this->array_to_http(array(
			'username'	=> $username,
			'password'	=> $password,
			'autologin'	=> 1,
			'redirect'	=> 'index.php',
			'login'		=> 'Login',
		));
		// Init curl
		$this->curl = curl_init();
		// Set options
		curl_setopt ( $this->curl, CURLOPT_URL, $this->phpbb_url . 'ucp.php?mode=login' );
		curl_setopt ( $this->curl, CURLOPT_POST, true );
		curl_setopt ( $this->curl, CURLOPT_POSTFIELDS, $post_fields );
		curl_setopt ( $this->curl, CURLOPT_RETURNTRANSFER, true );
		curl_setopt ( $this->curl, CURLOPT_HEADER, false );
		curl_setopt ( $this->curl, CURLOPT_COOKIE, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_COOKIEJAR, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_COOKIEFILE, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_USERAGENT, $_SERVER['HTTP_USER_AGENT'] );
		// Execute request
		$result = curl_exec ( $this->curl );
		// Error handling
		if ( curl_errno ( $this->curl ) )
		{
			$this->error = array(
				curl_errno($this->curl),
				curl_error($this->curl),
			);
			curl_close ( $this->curl );
			return false;
		}
		// Close connection
		curl_close ( $this->curl );
		// Return result
		return true;
	}

	/*
		@ Function	: read() - Read a pages contents
		@ About		: Returns the contents of a url
		@ Type		: Public
	*/
	function read($page_url)
	{
		global $_SERVER;

		// Init curl
		$this->curl = curl_init();
		// Set options
		curl_setopt ( $this->curl, CURLOPT_URL, $this->phpbb_url . $page_url );
		curl_setopt ( $this->curl, CURLOPT_POST, false );
		curl_setopt ( $this->curl, CURLOPT_RETURNTRANSFER, true );
		curl_setopt ( $this->curl, CURLOPT_HEADER, false );
		curl_setopt ( $this->curl, CURLOPT_COOKIE, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_COOKIEJAR, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_COOKIEFILE, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_USERAGENT, $_SERVER['HTTP_USER_AGENT'] );
		// Execute request
		$result = curl_exec ( $this->curl );
		// Error handling
		if ( curl_errno ( $this->curl ) )
		{
			$this->error = array(
				curl_errno($this->curl),
				curl_error($this->curl),
			);
			curl_close ( $this->curl );
			return false;
		}
		// Close connection
		curl_close ( $this->curl );
		// Return result
		return $result;
	}

	/*
		@ Function	: new_topic() - New Topic
		@ About		: Remotely posts a topic to the target phpBB forum.
		@ Type		: Public
	*/
	function new_topic($forum_id, $message, $topic_title)
	{
		global $_SERVER;
	
		// Generate post string
		$post_fields = $this->array_to_http(array(
			'post'				=> 'Submit',
			'mode'				=> 'newtopic',
			'message'			=> $message,
			'f'					=> $forum_id,
			'subject'			=> $topic_title,
			'disable_bbcode'	=> 0,
			'disable_smilies'	=> 0,
			'attach_sig'		=> 1,
			'topictype'			=> 0,
		));
		// Location
		$url_vars = $this->array_to_http(array(
			'mode'	=> 'newtopic',
			'f'		=> $forum_id,
		));
		// Init curl
		$this->curl = curl_init();
		// Set options
		curl_setopt ( $this->curl, CURLOPT_URL, $this->phpbb_url . 'posting.php?' . $url_vars );
		curl_setopt ( $this->curl, CURLOPT_POST, true );
		curl_setopt ( $this->curl, CURLOPT_POSTFIELDS, $post_fields );
		curl_setopt ( $this->curl, CURLOPT_RETURNTRANSFER, true );
		curl_setopt ( $this->curl, CURLOPT_HEADER, false );
		curl_setopt ( $this->curl, CURLOPT_COOKIE, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_COOKIEJAR, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_COOKIEFILE, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_USERAGENT, $_SERVER['HTTP_USER_AGENT'] );
		// Execute request
		$result = curl_exec ( $this->curl );
		// Get the result
		if ( preg_match ( '#<td align="center"><span class="gen">Your message has been entered successfully.<br \/><br \/>#is', $result, $match ) )
		{
			$post_status = 1;
		}
		else if ( preg_match ( '#<td align="center"><span class="gen">You cannot make another post so soon after your last; please try again in a short while.<\/span><\/td>>#is', $result, $match ) )
		{
			$post_status = 0;
		}
		else
		{
			$post_status = 0;
		}
		// Error handling
		if ( curl_errno ( $this->curl ) )
		{
			$this->error = array(
				curl_errno($this->curl),
				curl_error($this->curl),
			);
			curl_close ( $this->curl );
			return false;
		}
		// Close connection
		curl_close ( $this->curl );
		// Return result
		return $post_status;
	}

	/*
		@ Function	: new_pm() - New PM
		@ About		: Remotely sends a pm to a user of a phpbb forum.
		@ Type		: Public
	*/
	function new_pm($username, $message, $topic_title)
	{
		global $_SERVER;
	
		// Generate post string
		$post_fields = $this->array_to_http(array(
			'post'				=> 'Submit',
			'mode'				=> 'newtopic',
			'message'			=> $message,
			'username'			=> $username,
			'subject'			=> $topic_title,
			'disable_bbcode'	=> 0,
			'disable_smilies'	=> 0,
			'attach_sig'		=> 1,
		));
		// Location
		$url_vars = $this->array_to_http(array(
			'mode'	=> 'post',
			'u'		=> $username,
		));
		// Init curl
		$this->curl = curl_init();
		// Set options
		curl_setopt ( $this->curl, CURLOPT_URL, $this->phpbb_url . 'posting.php?' . $url_vars );
		curl_setopt ( $this->curl, CURLOPT_POST, true );
		curl_setopt ( $this->curl, CURLOPT_POSTFIELDS, $post_fields );
		curl_setopt ( $this->curl, CURLOPT_RETURNTRANSFER, true );
		curl_setopt ( $this->curl, CURLOPT_HEADER, false );
		curl_setopt ( $this->curl, CURLOPT_COOKIE, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_COOKIEJAR, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_COOKIEFILE, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_USERAGENT, $_SERVER['HTTP_USER_AGENT'] );
		// Execute request
		$result = curl_exec ( $this->curl );
		// Get the result
		if ( preg_match ( '#<td align="center"><span class="gen">Your message has been sent.<br \/><br \/>#is', $result, $match ) )
		{
			$post_status = 1;
		}
		else if ( preg_match ( '#<td align="center"><span class="gen">You cannot make another post so soon after your last; please try again in a short while.<\/span><\/td>>#is', $result, $match ) )
		{
			$post_status = 0;
		}
		else
		{
			$post_status = 0;
		}
		// Error handling
		if ( curl_errno ( $this->curl ) )
		{
			$this->error = array(
				curl_errno($this->curl),
				curl_error($this->curl),
			);
			curl_close ( $this->curl );
			return false;
		}
		// Close connection
		curl_close ( $this->curl );
		// Return result
		return $post_status;
	}

	/*
		@ Function	: topic_reply() - Topic Reply
		@ About		: Remotely replys a post to a topic to the target phpBB forum.
		@ Type		: Public
	*/
	function topic_reply($topic_id, $message, $topic_title)
	{
		global $_SERVER;
	
		// Generate post string
		$post_fields = $this->array_to_http(array(
			'post'				=> 'Submit',
			'mode'				=> 'reply',
			'message'			=> $message,
			't'					=> $topic_id,
			'subject'			=> $topic_title,
			'disable_bbcode'	=> 0,
			'disable_smilies'	=> 0,
			'attach_sig'		=> 1,
			'topictype'			=> 0,
		));
		// Location
		$url_vars = $this->array_to_http(array(
			'mode'	=> 'reply',
			't'		=> $topic_id,
		));
		// Init curl
		$this->curl = curl_init();
		// Set options
		curl_setopt ( $this->curl, CURLOPT_URL, $this->phpbb_url . 'posting.php?' . $url_vars );
		curl_setopt ( $this->curl, CURLOPT_POST, true );
		curl_setopt ( $this->curl, CURLOPT_POSTFIELDS, $post_fields );
		curl_setopt ( $this->curl, CURLOPT_RETURNTRANSFER, true );
		curl_setopt ( $this->curl, CURLOPT_HEADER, false );
		curl_setopt ( $this->curl, CURLOPT_COOKIE, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_COOKIEJAR, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_COOKIEFILE, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_USERAGENT, $_SERVER['HTTP_USER_AGENT'] );
		// Execute request
		$result = curl_exec ( $this->curl );
		// Get the result
		if ( preg_match ( '#<td align="center"><span class="gen">Your message has been entered successfully.<br \/><br \/>#is', $result, $match ) )
		{
			$post_status = 1;
		}
		else if ( preg_match ( '#<td align="center"><span class="gen">You cannot make another post so soon after your last; please try again in a short while.<\/span><\/td>>#is', $result, $match ) )
		{
			$post_status = 0;
		}
		else
		{
			$post_status = 0;
		}
		// Error handling
		if ( curl_errno ( $this->curl ) )
		{
			$this->error = array(
				curl_errno($this->curl),
				curl_error($this->curl),
			);
			curl_close ( $this->curl );
			return false;
		}
		// Close connection
		curl_close ( $this->curl );
		// Return result
		return $post_status;
	}
  
	/*
		@ Function	: logout() - Log Out
		@ About		: Logs out of the target phpBB properly.
		@ Type		: Public
	*/
	function logout()
	{
		global $_SERVER;
	
		// Generate post string
		$urlopt = $this->array_to_http(array(
			'logout'	=> 'true',
			'mode'		=> 'logout',
		));
		// Init curl
		$this->curl = curl_init();
		// Set options
		curl_setopt ( $this->curl, CURLOPT_URL, $this->phpbb_url . 'ucp.php?' . $urlopt );
		curl_setopt ( $this->curl, CURLOPT_POST, false );
		curl_setopt ( $this->curl, CURLOPT_RETURNTRANSFER, true );
		curl_setopt ( $this->curl, CURLOPT_HEADER, false );
		curl_setopt ( $this->curl, CURLOPT_COOKIE, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_COOKIEJAR, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_COOKIEFILE, $this->cookie_name );
		curl_setopt ( $this->curl, CURLOPT_USERAGENT, $_SERVER['HTTP_USER_AGENT'] );
		// Execute request
		$result = curl_exec ( $this->curl );
		// Error handling
		if ( curl_errno ( $this->curl ) )
		{
			$this->error = array(
				curl_errno($this->curl),
				curl_error($this->curl),
			);
			curl_close ( $this->curl );
			return false;
		}
		// Close connection
		curl_close ( $this->curl );
		// Delete cookie file
		@unlink($this->cookie_name);
		// Return result
		return true;
	}
  
	/*
		@ Function	: getCurl() - return curl object
		@ About		: Returns curl object
		@ Type		: Public
	*/  
  function getCurl() {
    return $this->curl;
  }
	
	/*
		@ Function	: array_to_http() - Converter
		@ About		: Converts data from array to http string
		@ Type		: Private
	*/
	function array_to_http($array)
	{
		$retvar = '';
		while ( list ( $field, $data ) = @each ( $array ) )
		{
			$retvar .= ( empty($retvar) ) ? '' : '&';
			$retvar .= urlencode($field) . '=' . urlencode($data); 
		}
		return $retvar;
	}
  
	function array_to_http2($array)
	{
		$retvar = '';
		while ( list ( $field, $data ) = @each ( $array ) )
		{
			$retvar .= ( empty($retvar) ) ? '' : '&amp;';
			$retvar .= urlencode($field) . '=' . urlencode($data); 
		}
		return $retvar;
	}
  
  
}

?>