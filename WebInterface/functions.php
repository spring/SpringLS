<?php
  // this function has been copied from: http://www.laughing-buddha.net/jon/php/selectdate/
  // make sure you use checkdate() on the returned values, as date may be invalid!
  function selectDate (
                          $sel_d = -1       // selected day
                        , $sel_m = -1       // selected month
                        , $sel_y = -1       // selected year
                        , $var_d = 'd'     // name for day variable
                        , $var_m = 'm'     // name for month variable
                        , $var_y = 'y'     // name for year variable
                        , $min_y = -1       // minimum year
                        , $max_y = -1       // maximum year
                        , $enabled = true  // enable drop-downs?
                      ) {

    // --------------------------------------------------------------------------
    // First of all, set up some sensible defaults

    // Default day is today
    if ($sel_d == -1)
      $sel_d = date('j');

    // Default month is this month
    if ($sel_m == -1)
      $sel_m = date('n');

    // Default year is this year
    if ($sel_y == -1)
      $sel_y = date('Y');

    // Default minimum year is last year
    if ($min_y == -1)
      $min_y = date('Y') - 1;

    // Default maximum year is two years ahead
    if ($max_y == -1)
      $max_y = ($min_y + 3);


    // --------------------------------------------------------------------------
    // Start off with the drop-down for Days

    // Start opening the select element
    echo '<select name="' . $var_d . '"';

    // Add disabled attribute if necessary
    if (!$enabled)
      echo ' disabled="disabled"';

    // Finish opening the select element
    echo ">\n";

    // Loop round and create an option element for each day (1 - 31)
    for ($i = 1; $i <= 31; $i++) {

      // Start the option element
      echo "\t<option value=\"" . $i . '"';

      // If this is the selected day, add the selected attribute
      if ($i == $sel_d)
        echo ' selected="selected"';

      // Display the value and close the option element
      echo ">" . $i . "</option>\n";

    }

    // Close the select element
    echo "</select>";


    // --------------------------------------------------------------------------
    // Now do the drop-down for Months

    // Start opening the select element
    echo '<select name="' . $var_m . '"';

    // Add disabled attribute if necessary
    if (!$enabled)
      echo ' disabled="disabled"';

    // Finish opening the select element
    echo ">\n";

    // Loop round and create an option element for each month (Jan - Dec)
    for ($i = 1; $i <= 12; $i++) {

      // Start the option element
      echo "\t<option value=\"" . $i . '"';

      // If this is the selected month, add the selected attribute
      if ($i == $sel_m)
        echo ' selected="selected"';

      // Display the value and close the option element
      echo ">" . date('F', mktime(3, 0, 0, $i)) . "</option>\n";

    }

    // Close the select element
    echo "</select>";


    // --------------------------------------------------------------------------
    // Finally, the drop-down for Years

    // Start opening the select element
    echo '<select name="' . $var_y . '"';

    // Add disabled attribute if necessary
    if (!$enabled)
      echo ' disabled="disabled"';

    // Finish opening the select element
    echo ">\n";

    // Loop round and create an option element for each year ($min_y - $max_y)
    for ($i = $min_y; $i <= $max_y; $i++) {

      // Start the option element
      echo "\t<option value=\"" . $i . '"';

      // If this is the selected year, add the selected attribute
      if ($i == $sel_y)
        echo ' selected="selected"';

      // Display the value and close the option element
      echo ">" . $i . "</option>\n";

    }

    // Close the select element
    echo "</select>\n";

  }
  
  // this function has been copied from: http://www.laughing-buddha.net/jon/php/selectdate/
  // I've modified it somewhat though, to add time picking capability.
  function selectTime (
                          $sel_h = -1       // selected hour
                        , $sel_m = -1       // selected minutes
                        , $sel_s = -1       // selected seconds
                        , $var_h = 'h'     // name for hour variable
                        , $var_m = 'm'     // name for minutes variable
                        , $var_s = 's'     // name for seconds variable
                        , $enabled = true  // enable drop-downs?
                      ) {

    // --------------------------------------------------------------------------
    // First of all, set up some sensible defaults

    // Default time is today
    if ($sel_h == -1)
      $sel_h = date('H');

    if ($sel_m == -1)
      $sel_m = date('i');

    if ($sel_s == -1)
      $sel_s = date('s');

    // --------------------------------------------------------------------------
    // Start off with the drop-down for hour

    // Start opening the select element
    echo '<select name="' . $var_h . '"';

    // Add disabled attribute if necessary
    if (!$enabled)
      echo ' disabled="disabled"';

    // Finish opening the select element
    echo ">\n";

    // Loop round and create an option element for each hour (00 - 23)
    for ($i = 0; $i <= 23; $i++) {

      // Start the option element
      echo "\t<option value=\"" . $i . '"';

      // If this is the selected hour, add the selected attribute
      if ($i == $sel_h)
        echo ' selected="selected"';

      // Display the value and close the option element
      printf(">" . "%02d" . "</option>\n", $i);

    }

    // Close the select element
    echo "</select>:";

    // --------------------------------------------------------------------------
    // Now do the drop-down for minutes

    // Start opening the select element
    echo '<select name="' . $var_m . '"';

    // Add disabled attribute if necessary
    if (!$enabled)
      echo ' disabled="disabled"';

    // Finish opening the select element
    echo ">\n";

    // Loop round and create an option element for each minute (00 - 59)
    for ($i = 0; $i <= 59; $i++) {

      // Start the option element
      echo "\t<option value=\"" . $i . '"';

      // If this is the selected minute, add the selected attribute
      if ($i == $sel_m)
        echo ' selected="selected"';

      // Display the value and close the option element
      printf(">" . "%02d" . "</option>\n", $i);

    }

    // Close the select element
    echo "</select>:";

    // --------------------------------------------------------------------------
    // Finally, the drop-down for seconds

    // Start opening the select element
    echo '<select name="' . $var_s . '"';

    // Add disabled attribute if necessary
    if (!$enabled)
      echo ' disabled="disabled"';

    // Finish opening the select element
    echo ">\n";

    // Loop round and create an option element for each second (00 - 59)
    for ($i = 0; $i <= 59; $i++) {

      // Start the option element
      echo "\t<option value=\"" . $i . '"';

      // If this is the selected second, add the selected attribute
      if ($i == $sel_s)
        echo ' selected="selected"';

      // Display the value and close the option element
      printf(">" . "%02d" . "</option>\n", $i);

    }

    // Close the select element
    echo "</select>\n";

  }

  // "minimum date box" - contains all the elements we need in a box shaped table
  function minDateBox () {
    echo '<table cellspacing="5" frame="border">';
    echo '<tr>';
    echo '  <td colspan="4">';
    echo '  <input type="checkbox" name="usemindate" /> Show newer than:';
    echo '  </td>';
    echo '</tr>';
    echo '<tr>';

    echo '  <td>';
    echo "  Pick date:";
    echo '  </td>';

    echo '  <td>';
    selectDate(-1, -1, -1, "min_day", "min_month", "min_year");
    echo '  </td>';

    echo '  <td>';
    echo "  Pick time:";
    echo '  </td>';

    echo '  <td>';
    selectTime(0, 0, 0, "min_hour", "min_min", "min_sec");
    echo '  </td>';

    echo '</tr>';
    echo '</table>';
  }
  
  // "maximum date box" - contains all the elements we need in a box shaped table
  function maxDateBox () {
    echo '<table cellspacing="5" frame="border">';
    echo '<tr>';
    echo '  <td colspan="4">';
    echo '  <input type="checkbox" name="usemaxdate" /> Show older than:';
    echo '  </td>';
    echo '</tr>';
    echo '<tr>';

    echo '  <td>';
    echo "  Pick date:";
    echo '  </td>';

    echo '  <td>';
    selectDate(-1, -1, -1, "max_day", "max_month", "max_year");
    echo '  </td>';

    echo '  <td>';
    echo "  Pick time:";
    echo '  </td>';

    echo '  <td>';
    selectTime(23, 59, 59, "max_hour", "max_min", "max_sec");
    echo '  </td>';

    echo '</tr>';
    echo '</table>';
  }
  
  function keywordBox () {
    echo '<table cellspacing="5" frame="border">';
    echo '<tr>';
    echo '  <td colspan="4">';
    echo '  <input type="checkbox" name="usekeyword" /> Only show lines that contain:';
    echo '  </td>';
    echo '</tr>';
    echo '<tr>';

    echo '  <td>';
    echo "  Keyword: <input type='text' name='keyword' />";
    echo '  </td>';

    echo '</tr>';

    echo '<tr>';
    echo '  <td>';
    echo '  Note: wildcards are not supported!';
    echo '  </td>';
    echo '</tr>';

    echo '</table>';
  }


?>
