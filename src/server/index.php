<?php

error_reporting(E_ALL);

define('PATH_BASE', dirname(__FILE__) . '/');
define('PATH_TMP', PATH_BASE . 'tmp/');
define('PATH_OBJECTS', PATH_BASE . 'objects/');

require_once(PATH_OBJECTS . 'class.DynamicStation.php');

$sUrl = "https://api.jcdecaux.com/vls/v1/stations?" .
  			"contract=Paris&apiKey=718b4e0e0b1f01af842ff54c38bed00eaa63ce3c";

$sFilePath = PATH_TMP . 'stations';
$iDataTimeout = 120; // seconds

$iContract = intval($_GET['c']);

if (!isset(Station::$aContracts[$iContract])) {
  // Quit if not a valid contract
  die('BAD_CONTRACT');
}

function isBeingWritten($sFilePath) {
  $mFile = fopen($sFilePath, 'r');
  $bLock = flock($mFile, LOCK_EX | LOCK_NB);
  flock($mFile, LOCK_UN);
  fclose($mFile);

  return $mFile && $bLock;
}

function openLockedFileTimeOut($sFilePath, $sMode, $iTimeOut, $bRead) {
  $iTime = 0;
  while ($iTime < $iTimeOut && isBeingWritten($sFilePath)) {
    $iTime += 100;
    usleep(100);
  }

  $mFile = fopen($sFilePath, $sMode);

  $iLock = $bRead ? LOCK_SH : LOCK_EX;
  $bLock = flock($mFile, $iLock | LOCK_NB);

  return $bLock ? $mFile : false;
}

$sFilePath .= '_' . $iContract;
if (!file_exists($sFilePath) || (filemtime($sFilePath) +  $iContract)> time()) {
  // If data not cached or outdated
  if (!isBeingWritten($sFilePath)) {
    // Not being written, take the stab at writing it
    $mFile = openLockedFileTimeOut($sFilePath, 'wb', 500, false);
    flock($mFile, LOCK_EX);

    $sStations = file_get_contents($sUrl);
    $aStations = json_decode($sStations, true);
    foreach ($aStations as $key => $aStation) {
      $mStation = new DynamicStation($aStation);

      fwrite($mFile, $mStation->getMinimizedDynamic());
    }

    fflush($mFile);            // flush output before releasing the lock
    flock($mFile, LOCK_UN);    // release the lock
  }
}


// Now file is written or being written for sure
$mFile = openLockedFileTimeOut($sFilePath, 'rb', 500, true);
if (!$mFile) die('CANNOT_READ');

$iFileSize = filesize($sFilePath);
header('Content-Type: binary/octet-stream');
header('Content-Length: ' . $iFileSize);
echo fread($mFile, $iFileSize);
fclose($mFile);
flock($mFile, LOCK_UN);

?>