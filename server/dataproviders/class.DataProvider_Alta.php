<?php

require_once(PATH_PROVIDERS . 'class.DataProvider_Abstract.php');

class DataProvider_Alta extends DataProvider_Abstract {
  public function generateStationsUrl() {
    return $this->getContract()->getUrl();
  }

  public function parseStation(Array $aStation) {
    $iIdLocal = $aStation['id'];
    $sName = $aStation['stationName'];
    $sAddress = $aStation['stAddress1'];
    $iLat = $aStation['latitude'];
    $iLon = $aStation['longitude'];
    $iAvBikes = $aStation['statusValue'] = 'In Service' ?
        $aStation['availableBikes'] : 0;
    $iAvBikeStands = $aStation['statusValue'] = 'In Service' ?
        $aStation['availableDocks'] : 0;
    $iLastUpdate = $aStation['lastCommunicationTime'];

    return new DynamicStation($this->getContract(), $iIdLocal, $sName,
      $sAddress, $iLat, $iLon, $iAvBikes, $iAvBikeStands, $iLastUpdate);
  }

  public function parseStations($sStations) {
    $aStations = json_decode($sStations, true);
    if (!$aStations) return false;

    $aAllStations = Array();
    foreach ($aStations['stationBeanList'] as $key => $aStation) {
      if ($aStation['testStation'] == 'true') continue;
      $mStation = $this->parseStation($aStation);
      $aAllStations[$mStation->getId()] = $mStation;
    }

    return $aAllStations;
  }
}

?>