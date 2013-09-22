<?php

require_once(PATH_OBJECTS . 'class.Station.php');

class DynamicStation extends Station {
  protected $_bOpened;
  protected $_iAvBikes;
  protected $_iAvBikeStands;
  protected $_iLastUpdate;

  function __construct(Array $aStation) {
    parent::__construct($aStation);

    $this->_bOpened = ($aStation['status'] === 'OPEN');
    $this->_iAvBikes = intval($aStation['available_bikes']);
    $this->_iAvBikeStands = intval($aStation['available_bike_stands']);
    $this->_iLastUpdate = intval($aStation['last_update']);
  }

  public function isOpened() { return $this->_bOpened; }
  public function getAvailableBikes() { return $this->_iAvBikes; }
  public function getAvailableBikeStands() { return $this->_iAvBikeStands; }
  public function getLastUpdate() { return $this->_iLastUpdate; }

  public function getMinimizedDynamic() {
    $sOutput = pack("L", $this->getId());


    return $sOutput[0] . $sOutput[1] . $sOutput[2] . pack("C*",
      $this->_iAvBikes, $this->_iAvBikeStands, intval($this->_bOpened));
  }

  public function getArray() {
    $aArray = parent::getArray();

    $aArray['status'] = $this->_bOpened;
  }
}

?>