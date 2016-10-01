<?php

error_reporting(E_ALL);

define('PATH_BASE', dirname(__FILE__) . '/');
define('PATH_TMP', PATH_BASE . 'tmp/');
define('PATH_OBJECTS', PATH_BASE . 'objects/');
define('PATH_PROVIDERS', PATH_BASE . 'dataproviders/');

require_once(PATH_OBJECTS . 'class.FileUtils.php');
require_once(PATH_OBJECTS . 'class.Contracts.php');


$sFilePath_Stations = PATH_TMP . 'stations';
$iDataTimeout_Stations = 120; // seconds

$sFilePath_Contracts = PATH_TMP . 'contracts';
$sUrl_Contracts = "http://api.citybik.es/networks.json";
$iDataTimeout_Contracts = 36000; // seconds

$mCityList = new Contracts();
$iContract = intval(@$_GET['c']);
if (! $iContract) {
  die(json_encode($mCityList->getArray()));
}

$mCity = $mCityList->getContractById($iContract);
if (!$mCity) {
  // Quit if not a valid contract
  die('CONTRACT_BAD_OR_CANNOT_LOAD');
}

$bFull = boolval(@$_GET['f']);
if (!$mCity->updateDynamicStations($sFilePath_Stations,
    $iDataTimeout_Stations)) {
  // Quit if we can't update stations for some reason
  die('STATIONS_CANNOT_UPDATE');
}

$sFilePath = $mCity->buildStationsFilePath($sFilePath_Stations, $bFull);
$mFile = FileUtils::openLockedFileTimeOut($sFilePath, 'rb', 500, true);
if (!$mFile) {
  // Quit if not a valid contract
  die('STATIONS_CANNOT_LOAD_FILE');
}

$iFileSize = filesize($sFilePath);

if (!$bFull) {
  header('Content-Type: binary/octet-stream');
  header('Content-Length: ' . $iFileSize);
}

echo fread($mFile, $iFileSize);

flock($mFile, LOCK_UN);
fclose($mFile);

?>