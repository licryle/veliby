<?php

class Station {
  protected $_iNumber;
  protected $_iContract;
  protected $_iId;

  protected $_sName;
  protected $_sAddress;
  protected $_aPos;
  protected $_bBanking;
  protected $_bBonus;
  protected $_iBikeStands;

  static public $aContracts = [
      1 => "Paris",
  ];

  public function __construct(Array $aStation) {
    $this->_iNumber = intval($aStation['number']);
    $this->_iContract = array_search($aStation['contract_name'], self::$aContracts);
    $this->_iId = 1000000 * $this->_iContract + $this->_iNumber;

    $this->_sName = $aStation['name'];
    $this->_sAddress = $aStation['address'];

    $this->_aPos = $aStation['position'];
    $this->_bBanking = ($aStation['banking'] === true);
    $this->_bBonus = ($aStation['bonus'] === true);
    $this->_iBikeStands = intval($aStation['bike_stands']);
  }

  public function getNumber() { return $this->_iNumber; }
  public function getContract() { return $this->_iContract; }
  public function getId() { return $this->_iId; }
  public function getName() { return $this->_sName; }
  public function getAddress() { return $this->_sAddress; }
  public function getPosition() { return $this->_aPos; }
  public function hasBanking() { return $this->_bBanking; }
  public function hasBonus() { return $this->_bBonus; }
  public function getBikeStands() { return $this->_iBikeStands; }

  public function getArray() {
    $aArray = [];
    $aArray['id'] = $this->_iId;
    $aArray['number'] = $this->_iNumber;
    $aArray['contract'] = $this->_iContract;

    $aArray['name'] = $this->_sName;
    $aArray['address'] = $this->_sAddress;

    $aArray['position'] = $this->_aPos;
    $aArray['banking'] = $this->_bBanking;
    $aArray['bonus'] = $this->_bBonus;
    $aArray['bike_stands'] = $this->_iBikeStands;

    return $aArray;
  }
}

?>