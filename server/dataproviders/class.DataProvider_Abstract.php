<?php

abstract class DataProvider_Abstract {
  protected $_mContract = null;

  public function __construct(Contract $mCity) {
  	$this->_mContract = $mCity;
  }

  protected function getContract() { return $this->_mContract; }

  abstract public function generateStationsUrl();
  abstract public function parseStation(Array $aStation);
  abstract public function parseStations($sStations);
}

?>