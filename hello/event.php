<?php
// Set your return content type
//header('Content-type: application/xml');

$daurl = 'http://localhost:8800/osgibroker/event?topic=hello&clientID=hello&timeOut=1';

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
