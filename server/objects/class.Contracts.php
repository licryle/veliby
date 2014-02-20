<?php

require_once(PATH_OBJECTS . 'class.Contract.php');
require_once(PATH_PROVIDERS . 'class.DataProvider_JCDecaux.php');
require_once(PATH_PROVIDERS . 'class.DataProvider_Alta.php');

class Contracts {
  protected $_aContracts = [];

  public function __construct() {
    $this->_aContracts[1] = new Contract(
      1, /* $iId */
      'Veliby', /* $sTag */
      'Veliby', /* $sName */
      'Paris', /* $sCity */
      48856614 / 1E6, /* $dLat */
      2352221 / 1E6, /* $dLng */
      20000, /* $iRadius */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[2] = new Contract(
      2, /* $iId */
      'CityBike', /* $sTag */
      'CityBike', /* $sName */
      'New York', /* $sCity */
      40.7324926, /* $dLat */
      -73.9960737, /* $dLng */
      20000, /* $iRadius */
      'DataProvider_Alta', /* $sProvider */
      'http://www.citibikenyc.com/stations/json' /* $sUrl */
    );

    $this->_aContracts[3] = new Contract(
      3, /* $iId */
      'SanFrancisco', /* $sTag */
      'Bay Area Bike Share', /* $sName */
      'San Francisco', /* $sCity */
      37.7917231, /* $dLat */
      -122.4072589, /* $dLng */
      20000, /* $iRadius */
      'DataProvider_Alta', /* $sProvider */
      'http://www.bayareabikeshare.com/stations/json' /* $sUrl */
    );

    $this->_aContracts[4] = new Contract(
      4, /* $iId */
      'Toronto', /* $sTag */
      'Bixi', /* $sName */
      'Toronto', /* $sCity */
      43653226 / 1E6, /* $dLat */
      -79383184 / 1E6, /* $dLng */
      20000, /* $iRadius */
      'DataProvider_Alta', /* $sProvider */
      'http://www.bikesharetoronto.com/stations/json' /* $sUrl */
    );

    $this->_aContracts[5] = new Contract(
      5, /* $iId */
      'Divvy', /* $sTag */
      'Divvy', /* $sName */
      'Chicago', /* $sCity */
      41878113 / 1E6, /* $dLat */
      -87629798 / 1E6, /* $dLng */
      20000, /* $iRadius */
      'DataProvider_Alta', /* $sProvider */
      'http://www.divvybikes.com/stations/json' /* $sUrl */
    );

//    $this->_aContracts[5] = new Contract(
//      5, /* $iId */
//      'Washington', /* $sTag */
//      'Capital Bike Share', /* $sName */
//      'Washington, DC', /* $sCity */
//      38896758, /* $dLat */
//      -77037016, /* $dLng */
//      20000, /* $iRadius */
//      'DataProvider_AltaXML', /* $sProvider */
//      'url' => 'https://www.capitalbikeshare.com/data/stations/bikeStations.xml' /* $sUrl */
//    );
  }

  public function getContractById($iId) {
    return $this->_aContracts[$iId];
  }

  public function getArray() {
    $aContracts = [];

    foreach ($this->_aContracts AS $mContract) {
      $aContracts[] = $mContract->getArray();
    }

    return $aContracts;
  } 
}

?>