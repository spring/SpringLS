<?php

/**
 * HTML Form Parser
 * This will extract all forms and his elemets in an
 * big assoc Array.
 *
 * @package HtmlFormParser
 * @version $Id 1.0
 * @author Peter Valicek <Sonny2@gmx.DE>
 * @copyright 2004 Peter Valicek Peter Valicek <Sonny2@gmx.DE>
 */

class HtmlFormParser {
	
	/**
	 * Core HTML Data
	 * @access public
	 * @var string
	 * @see HtmlFormParser()
	 */
	var $html_data = '';
	
	/**
	 * Param Array of all Elemets
	 * @access private
	 * @var array
	 */
	var $_return = array();
	
	/**
	 * Form counter
	 * @access private
	 * @var int
	 */
	var $_counter = '';
	
	/**
	 * Form button Counter
	 * @access private
	 * @var int
	 * @see parseForms()
	 */
	var $button_counter = '';
	
	/**
	 * unique identifiert for parsing
	 * @access private
	 * @var string
	 * @see HtmlFormParser()
	 */
	var $_unique_id = '';
	 
	/**
	 * HtmlFormParser Constructor
	 * @access public
	 * @param mixed $html_data Could be either an big array or an string
	 */
	function HtmlFormParser( $html_data ) {
		if ( is_array($html_data) ) {
			$this->html_data = join('', $html_data);
		} else {
			$this->html_data = $html_data;
		}
		$this->_return = array();
		$this->_counter = 0;
		$this->button_counter = 0;
		$this->_unique_id = md5(time());
	}
	
	/**
	 * Parse all Forms in given Data
	 * @access public
	 * @return array
	 */
	function parseForms() {
		if ( preg_match_all("/<form.*>.+<\/form>/isU", $this->html_data, $forms) ) {
			foreach ( $forms[0] as $form ) {
				/*
				 * Form Details like method, action ..
				 */
				preg_match("/<form.*name=[\"']?([\w\s]*)[\"']?[\s>]/i", $form, $form_name);
				$this->_return[$this->_counter]['form_data']['name'] = preg_replace("/[\"'<>]/", "", $form_name[1]);
				preg_match("/<form.*action=(\"([^\"]*)\"|'([^']*)'|[^>\s]*)([^>]*)?>/is", $form, $action);
				$this->_return[$this->_counter]['form_data']['action'] = preg_replace("/[\"'<>]/", "", $action[1]);
				preg_match("/<form.*method=[\"']?([\w\s]*)[\"']?[\s>]/i", $form, $method);
				$this->_return[$this->_counter]['form_data']['method'] = preg_replace("/[\"'<>]/", "", $method[1]);
				preg_match("/<form.*enctype=(\"([^\"]*)\"|'([^']*)'|[^>\s]*)([^>]*)?>/is", $form, $enctype);
				$this->_return[$this->_counter]['form_data']['enctype'] = preg_replace("/[\"'<>]/", "", $enctype[1]);
				
				/*
				 * <input type=hidden entries
				 */
				if ( preg_match_all("/<input.*type=[\"']?hidden[\"']?.*>$/im", $form, $hiddens) ) {					
					foreach ( $hiddens[0] as $hidden ) {
						$this->_return[$this->_counter]['form_elemets'][$this->_getName($hidden)] = array(
																							'type'	=> 'hidden',
																							'value'	=> $this->_getValue($hidden)
																							);
					}
				}
				
				/*
				 * <input type=text entries
				 */
				if ( preg_match_all("/<input.*type=[\"']?text[\"']?.*>/iU", $form, $texts) ) { 
					foreach ( $texts[0] as $text ) {
						$this->_return[$this->_counter]['form_elemets'][$this->_getName($text)] = array(
																							'type'	=> 'text',
																							'value'	=> $this->_getValue($text)
																							);
					}
				}
				
				/*
				 * <input type=password entries
				 */
				if ( preg_match_all("/<input.*type=[\"']?password[\"']?.*>/iU", $form, $passwords) ) { 
					foreach ( $passwords[0] as $password ) {
						$this->_return[$this->_counter]['form_elemets'][$this->_getName($password)] = array(
																							'type'	=> 'password',
																							'value'	=> $this->_getValue($password)
																							);
					}
				}
				
				/*
				 * <textarea entries
				 */
				if ( preg_match_all("/<textarea.*>.*<\/textarea>/isU", $form, $textareas) ) {
					foreach ( $textareas[0] as $textarea ) {
						preg_match("/<textarea.*>(.*)<\/textarea>/isU", $textarea, $textarea_value);
						$this->_return[$this->_counter]['form_elemets'][$this->_getName($textarea)] = array(
																							'type'	=> 'textarea',
																							'value'	=> $textarea_value[1]
																							);
					}
				}
				
				/*
				 * <input type=checkbox entries
				 */
				if ( preg_match_all("/<input.*type=[\"']?checkbox[\"']?.*>/iU", $form, $checkboxes) ) {
					foreach ( $checkboxes[0] as $checkbox ) {
						if ( preg_match("/checked/i", $checkbox) ) {
							$this->_return[$this->_counter]['form_elemets'][$this->_getName($checkbox)] = array(
																							'type'	=> 'checkbox',
																							'value'	=> 'on'
																							);
						} else {
							$this->_return[$this->_counter]['form_elemets'][$this->_getName($checkbox)] = array(
																							'type'	=> 'checkbox',
																							'value'	=> ''
																							);
						}
					}
				}
				
				/*
				 * <input type=radio entries
				 */
				if ( preg_match_all("/<input.*type=[\"']?radio[\"']?.*>/iU", $form, $radios) ) {
					foreach ( $radios[0] as $radio ) {
						if ( preg_match("/checked/i", $radio) ) {
							$this->_return[$this->_counter]['form_elemets'][$this->_getName($radio)] = array(
																							'type'	=> 'radio',
																							'value'	=> $this->_getValue($radio)
																							);
						}
					}		
				}
				
				/*
				 * <input type=submit entries
				 */
				if ( preg_match_all("/<input.*type=[\"']?submit[\"']?.*>/iU", $form, $submits) ) {
					foreach ( $submits[0] as $submit ) {
						$this->_return[$this->_counter]['buttons'][$this->button_counter] = array(
																							'type'	=> 'submit',
																							'name'	=> $this->_getName($submit),
																							'value'	=> $this->_getValue($submit)
																							);
						$this->button_counter++;
					}
				}
				
				/*
				 * <input type=button entries
				 */
				if ( preg_match_all("/<input.*type=[\"']?button[\"']?.*>/iU", $form, $buttons) ) {
					foreach ( $buttons[0] as $button ) {
						$this->_return[$this->_counter]['buttons'][$this->button_counter] = array(
																							'type'	=> 'button',
																							'name'	=> $this->_getName($button),
																							'value'	=> $this->_getValue($button)
																							);
						$this->button_counter++;
					}
				}
				
				/*
				 * <input type=reset entries
				 */
				if ( preg_match_all("/<input.*type=[\"']?reset[\"']?.*>/iU", $form, $resets) ) {
					foreach ( $resets[0] as $reset ) {
						$this->_return[$this->_counter]['buttons'][$this->button_counter] = array(
																							'type'	=> 'reset',
																							'name'	=> $this->_getName($reset),
																							'value'	=> $this->_getValue($reset)
																							);
						$this->button_counter++;
					}
				}
				
				/*
				 * <input type=image entries
				 */
				if ( preg_match_all("/<input.*type=[\"']?image[\"']?.*>/iU", $form, $images) ) {
					foreach ( $images[0] as $image ) {
						$this->_return[$this->_counter]['buttons'][$this->button_counter] = array(
																							'type'	=> 'reset',
																							'name'	=> $this->_getName($image),
																							'value'	=> $this->_getValue($image)
																							);
						$this->button_counter++;
					}
				}
				
				/*
				 * <input type=select entries
				 * Here I have to go on step around to grep at first all select names and then
				 * the content. Seems not to work in an other way
				 */
				if ( preg_match_all("/<select.*>.+<\/select>/isU", $form, $selects) ) {
					foreach ( $selects[0] as $select ) {
						if ( preg_match_all("/<option.*>.+<\/option>/isU", $select, $all_options) ) {
							foreach ( $all_options[0] as $option ) {
								if ( preg_match("/selected/i", $option) ) {
									if ( preg_match("/value=[\"'](.*)[\"']\s/iU", $option, $option_value) ) {
										$option_value = $option_value[1];
										$found_selected = 1;
									} else {
										preg_match("/<option.*>(.*)<\/option>/isU", $option, $option_value);
										$option_value = $option_value[1];
										$found_selected = 1;
									}
								}
							}
							if ( !isset($found_selected) ) {
								if ( preg_match("/value=[\"'](.*)[\"']/iU", $all_options[0][0], $option_value) ) {
									$option_value = $option_value[1];
								} else {
									preg_match("/<option>(.*)<\/option>/iU", $all_options[0][0], $option_value);
									$option_value = $option_value[1];
								}
							} else {
								unset($found_selected);
							}
							$this->_return[$this->_counter]['form_elemets'][$this->_getName($select)] = array(
																									'type'	=> 'select',
																									'value'	=> trim($option_value)
																									);
						}
					}
				}

				/*
				 * Update the form counter if we have more then 1 form in the HTML table
				 */
				$this->_counter++;
			}
		}
		return $this->_return;
	}
	
	/**
	 * Get Name from string
	 * @access private
	 * @param string
	 * @return string
	 */
	function _getName( $string ) {
		if ( preg_match("/name=[\"']?([\w\s]*)[\"']?[\s>]/i", $string, $match) ) {
			$val_match = preg_replace("/\"'/", "", trim($match[1]));
			
			unset($string);
			return $val_match;
		}
	}
	
	/**
	 * Get Value from string
	 * @access private
	 * @param string
	 * @return string
	 */
	function _getValue( $string ) {
		if ( preg_match("/value=(\"([^\"]*)\"|'([^']*)'|[^>\s]*)([^>]*)?>/is", $string, $match) ) {
			$val_match = trim($match[1]);
			
			if ( strstr($val_match, '"') ) {
				$val_match = str_replace('"', '', $val_match);
			}
			
			unset($string);
			return $val_match;
		}
	}

}

?>