<?php require("inc/head.php") ?>

<!--

Links:

http://www.pageresource.com/dhtml/jtut6.htm
(how to change background colors with javascript)

http://en.wikipedia.org/wiki/Image:Sample_web_form.png
(example form with radio buttons, checkboxes and text area)
-->
<Script Language="JavaScript">
 
function switchEnabled(what) {
  switch(what) {
    case(1):
    if (document.getElementById('C1').checked) {
      document.getElementById('T1').style.backgroundColor=originalBackgroundColor;
      document.getElementById('username').disabled=false;
    } else {
      document.getElementById('T1').style.backgroundColor='#E0E0E0';
      document.getElementById('username').disabled=true;
    }
    break;
    case(2):
    if (document.getElementById('C2').checked) {
      document.getElementById('T2').style.backgroundColor=originalBackgroundColor;
      document.getElementById('ip_start').disabled=false;
      document.getElementById('ip_end').disabled=false;
    } else {
      document.getElementById('T2').style.backgroundColor='#E0E0E0';
      document.getElementById('ip_start').disabled=true;
      document.getElementById('ip_end').disabled=true;
    }
    break;
    case(3):
    if (document.getElementById('C3').checked) {
      document.getElementById('T3').style.backgroundColor=originalBackgroundColor;
      document.getElementById('userid').disabled=false;
    } else {
      document.getElementById('T3').style.backgroundColor='#E0E0E0';
      document.getElementById('userid').disabled=true;
    }
    
    break;
  }
}

function checkCheckBoxes() {
  if (document.getElementById('C1').checked == false &&
      document.getElementById('C2').checked == false &&
      document.getElementById('C3').checked == false)
  {
    alert ('You must choose at least one criterion!');
    return false;
  }
  else
  {
    return true;
  }
}

</Script>

<FORM method="post" onsubmit="return checkCheckBoxes();" action="ban.process.php">
 
  <table class="table5" cellspacing="0">
    <tr>
      <!-- top-left space: -->
      <td>
      
        <p>1. General fields:</p>
        
        <!-- general info box --> 
        <table class="table4" width="300">
        <tr>
          <TD>Ban duration</TD>
          <TD>
           <input type="radio" name="R_bandDuration" value="limited" onclick="document.getElementById('dur_day').disabled=false; document.getElementById('dur_hours').disabled=false" checked> Ban for limited time
           <br />
           <input type="text" name="dur_days" id="dur_day" size=3 style="margin-left: 30px"> days
           <br />
           <input type="text" name="dur_hours" id="dur_hours" size=3 style="margin-left: 30px"> hours
           <br />
           <input type="radio" name="R_bandDuration" value="unlimited" onclick="document.getElementById('dur_day').disabled=true; document.getElementById('dur_hours').disabled=true"> Ban for indefinite time
          </TD>
        </tr>
        <tr>
          <TD colspan="2">Private reason (seen only by other mods):<BR>
           <textarea name="privatereason" cols="50" rows="5"></textarea>
          </TD>
        </tr>
        <tr>
          <TD colspan="2">Public reason (seen by the banned user):<BR>
           <textarea name="publicreason" cols="50" rows="2"></textarea>
          </TD>
        </tr>
        </table>   
      
      </td> <!-- top-left space -->

      <!-- top-right space: -->
      <td>
      
        <p>2. Select one or more ban criteria:</p>

        <!-- USERNAME box --> 
        <table class="table4" id="T1" width="300">
        <tr>
          <td colspan="4">
            <input type="checkbox" id="C1" name="C1" value="1" onclick="switchEnabled(1);">Ban by username</input>
            <hr width="100%" border='1' noshade color="#C5C5C5" />
          </td>
        </tr>
        <tr>
          <td>
            Username: <input type="text" name="username" id="username">
          </td>
        </tr>
        </table>
        
        <br />
        
        <!-- IP RANGE box --> 
        <table class="table4" id="T2" width="300">
        <tr>
          <td colspan="2">
            <input type="checkbox" id="C2" name="C2" value="1" onclick="switchEnabled(2);">Ban by IP or IP range</input>
            <hr width="100%" border='1' noshade color="#C5C5C5" />
          </td>
        </tr>
        <tr>
          <td>
           First IP:<br /> <input type="text" name="ip_start" id="ip_start" size="15">
          </td>
          <td>
           Last IP:<br /> <input type="text" name="ip_end" id="ip_end" size="15">
          </td>
        </tr>
        <tr>
          <td colspan="2">
            Note: To ban by IP, simply enter same IP in both boxes
          </td>
        </tr>
        </table>   
         
        <br />
         
        <!-- USER ID box --> 
        <table class="table4" id="T3" width="300">
        <tr>
          <td colspan="4">
            <input type="checkbox" id="C3" name="C3" value="1" onclick="switchEnabled(3);">Ban by user ID</input>
            <hr width="100%" border='1' noshade color="#C5C5C5" />
          </td>
        </tr>
        <tr>
          <td>
           User ID: <input type="text" name="userid" id="userid">
          </td>
        </tr>
        </table>   
      
      </td> <!-- top-right space -->

    </tr>
    <tr>
      <td colspan="2" style="text-align: left;">
        <p>3. Finaly, add new ban entry:</p>
        <input type="submit" value="--->     Add ban entry     <---" style="width: 200px">
      </td>
    </tr>
  </table>  
  
</FORM>
 
<Script Language="JavaScript">
  var originalBackgroundColor = document.getElementById('T1').style.backgroundColor;
  // set all to false:
  switchEnabled(1);
  switchEnabled(2);
  switchEnabled(3);
</Script>

 
<?php require("inc/footer.php") ?>
