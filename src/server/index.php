<?php

error_reporting(E_ALL);

define('PATH_BASE', dirname(__FILE__) . '/');
define('PATH_OBJECTS', PATH_BASE . 'objects/');

require_once(PATH_OBJECTS . 'class.DynamicStation.php');

$sUrl = "https://api.jcdecaux.com/vls/v1/stations?" .
  			"contract=Paris&apiKey=718b4e0e0b1f01af842ff54c38bed00eaa63ce3c";

$sStations = file_get_contents($sUrl);

$bWantStatic = isset($_GET['static']);

if ($bWantStatic) {
/*  $aStaticStations;
  foreach ($aStations as $key => $aStation) {
    $mStation = new DynamicStation($aStation);
  }
*/
  echo $sStations;
} else {
  header('Content-Type: binary/octet-stream');

  $aStations = json_decode($sStations, true);
  foreach ($aStations as $key => $aStation) {
    $mStation = new DynamicStation($aStation);

    echo $mStation->getMinimizedDynamic();
  }
}

?>