<?php
// Set your return content type
//header('Content-type: application/xml');

// We are going to send a dummy name-value pair. If you are sending discrete data (e.g. accelerometer data)
// you would send an event containing a direction, or a coordinate.
$daurl = 'http://localhost:8800/osgibroker/event?topic=counter&clientID=counter_mobile&_method=POST&eventName=eventValue';

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
