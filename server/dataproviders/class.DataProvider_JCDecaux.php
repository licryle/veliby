<?php

require_once(PATH_PROVIDERS . 'class.DataProvider_Abstract.php');

class DataProvider_JCDecaux extends DataProvider_Abstract {
  public function generateStationsUrl() {
    return 'https://api.jcdecaux.com/vls/v1/stations?contract=' .
        $this->getContract()->getTag() .
        '&apiKey=718b4e0e0b1f01af842ff54c38bed00eaa63ce3c';
  }

  public function parseStation(Array $aStation) {
    $iIdLocal = $aStation['number'];
    $sName = $aStation['name'];
    $sAddress = $aStation['address'];
    $iLat = $aStation['position']['lat'];/*  / 1E6)*/
    $iLon = $aStation['position']['lng'];
    $iAvBikes = $aStation['status'] = 'OPEN' ?
        $aStation['available_bikes'] : 0;
    $iAvBikeStands = $aStation['status'] = 'OPEN' ?
        $aStation['available_bike_stands'] : 0;
    $iLastUpdate = $aStation['last_update'];

    return new DynamicStation($this->getContract(), $iIdLocal, $sName,
      $sAddress, $iLat, $iLon, $iAvBikes, $iAvBikeStands, $iLastUpdate);
  }

  public function parseStations($sStations) {
    $aStations = json_decode($sStations, true);
    if (!$aStations) return false;

    $aAllStations = Array();
    foreach ($aStations as $key => $aStation) {
      $mStation = $this->parseStation($aStation);
      $aAllStations[$mStation->getId()] = $mStation;
    }

    return $aAllStations;
  }
}

?>