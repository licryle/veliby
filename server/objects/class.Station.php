<?php

class Station {
  protected $_iNumber;
  protected $_iId;
  protected $_mContract;

  protected $_sName;
  protected $_sAddress;
  protected $_aPos;

  public function __construct($mCity, $iIdLocal, $sName, $sAddress,
      $iLat, $iLon) {
    $this->_iNumber = intval($iIdLocal);
    $this->_mContract = $mCity;
    $this->_iId = 1000000 * $this->_mContract->getId() + $this->_iNumber;

    $this->_sName = $sName;
    $this->_sAddress = $sAddress;

    $this->_aPos = array('lat' => floatval($iLat),
        'lng' => floatval($iLon));
  }

  public function getNumber() { return $this->_iNumber; }
  public function getId() { return $this->_iId; }
  public function getContract() { return $this->_mContract; }
  public function getName() { return $this->_sName; }
  public function getAddress() { return $this->_sAddress; }
  public function getPosition() { return $this->_aPos; }

  public function getArray() {
    $aArray = Array();
    $aArray['number'] = $this->_iNumber;
    $aArray['contractId'] = $this->_mContract->getId();

    $aArray['name'] = $this->_sName;
    $aArray['address'] = $this->_sAddress;

    $aArray['position'] = $this->_aPos;

    return $aArray;
  }
}

?>