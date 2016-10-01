<?php

require_once(PATH_OBJECTS . 'class.Station.php');

class DynamicStation extends Station {
  protected $_iAvBikes;
  protected $_iAvBikeStands;
  protected $_iLastUpdate;

  function __construct($mCity, $iIdLocal, $sName, $sAddress,
      $iLat, $iLon, $iAvBikes, $iAvBikeStands, $iLastUpdate) {
    parent::__construct($mCity, $iIdLocal, $sName, $sAddress,
      $iLat, $iLon);

    $this->_iAvBikes = intval($iAvBikes);
    $this->_iAvBikeStands = intval($iAvBikeStands);
    $this->_iLastUpdate = intval($iLastUpdate);
  }

  public function getAvailableBikes() { return $this->_iAvBikes; }
  public function getAvailableBikeStands() { return $this->_iAvBikeStands; }
  public function getLastUpdate() { return $this->_iLastUpdate; }

  public function getMinimizedDynamic() {
    $sOutput = pack("L", $this->getNumber());

    return $sOutput[0] . $sOutput[1] . $sOutput[2] . pack("C*",
        $this->_iAvBikes, $this->_iAvBikeStands);
  }

  public function getArray() {
    $aArray = parent::getArray();

    $aArray['bikes']     = $this->_iAvBikes;
    $aArray['free']      = $this->_iAvBikeStands;
    $aArray['timestamp'] = $this->_iLastUpdate;

    return $aArray;
  }
}

?>