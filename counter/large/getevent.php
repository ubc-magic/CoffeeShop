<?php
// Set your return content type
header('Content-type: application/xml');

//The minimum timeout allowed by the broker's rest API is 1 second. For most large-display-to-mobile
//applications a second latency is enough (e.g. accelerometer data needs to be interpolated and averaged)
$daurl = 'http://localhost:8800/osgibroker/event?topic=counter&clientID=counter_large&timeOut=1';

// Get that website's content
$handle = fopen($daurl, "r");


// If there is something, read and return
if ($handle) {
    while (!feof($handle)) {
        $buffer = fgets($handle, 4096);
        echo $buffer;
    }
    fclose($handle);
}

?>
