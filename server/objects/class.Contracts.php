<?php

require_once(PATH_OBJECTS . 'class.Contract.php');
require_once(PATH_PROVIDERS . 'class.DataProvider_JCDecaux.php');
require_once(PATH_PROVIDERS . 'class.DataProvider_Alta.php');

class Contracts {
  protected $_aContracts = [];

  public function __construct() {
    $this->_aContracts[] = new Contract(
      1, /* $iId */
      'Paris', /* $sTag */
      'Veliby\'', /* $sName */
      'Paris', /* $sCity */
      48856614 / 1E6, /* $dLat */
      2352221 / 1E6, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      2, /* $iId */
      'Rouen', /* $sTag */
      'Cy\'clic', /* $sName */
      'Rouen', /* $sCity */
      49.443232, /* $dLat */
      1.099971, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      3, /* $iId */
      'Toulouse', /* $sTag */
      'VelôTOulouse', /* $sName */
      'Toulouse', /* $sCity */
      43.604652, /* $dLat */
      1.444209, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      4, /* $iId */
      'Luxembourg', /* $sTag */
      'Veloh', /* $sName */
      'Luxembourg', /* $sCity */
      49.611621, /* $dLat */
      6.1319346, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      5, /* $iId */
      'Valence', /* $sTag */
      'Valenbisi', /* $sName */
      'Valencia', /* $sCity */
      39.4699075, /* $dLat */
      -0.3762881, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      6, /* $iId */
      'Stockholm', /* $sTag */
      'Cyclocity', /* $sName */
      'Stockholm', /* $sCity */
      59.32893, /* $dLat */
      18.06491, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      7, /* $iId */
      'Goteborg', /* $sTag */
      'Goteborg', /* $sName */
      'Goteborg', /* $sCity */
      57.70887, /* $dLat */
      11.97456, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      8, /* $iId */
      'Santander', /* $sTag */
      'Tusbic', /* $sName */
      'Santander', /* $sCity */
      43.4623057, /* $dLat */
      -3.8099803, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      9, /* $iId */
      'Amiens', /* $sTag */
      'Velam', /* $sName */
      'Amiens', /* $sCity */
      49.894067, /* $dLat */
      2.295753, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      10, /* $iId */
      'Lillestrom', /* $sTag */
      'Bysykkel', /* $sName */
      'Lillestrom', /* $sCity */
      59.9559696, /* $dLat */
      11.0503785, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      11, /* $iId */
      'Mulhouse', /* $sTag */
      'Vélocité', /* $sName */
      'Mulhouse', /* $sCity */
      47.750839, /* $dLat */
      7.335888, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      12, /* $iId */
      'Lyon', /* $sTag */
      'Vélo\'v', /* $sName */
      'Lyon', /* $sCity */
      45.764043, /* $dLat */
      4.835659, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      13, /* $iId */
      'Ljubljana', /* $sTag */
      'Bicikelj', /* $sName */
      'Ljubljana', /* $sCity */
      46.0564509, /* $dLat */
      14.5080702, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      14, /* $iId */
      'Seville', /* $sTag */
      'Sevici', /* $sName */
      'Sevilla', /* $sCity */
      37.3880961, /* $dLat */
      -5.9823299, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      15, /* $iId */
      'Namur', /* $sTag */
      'Li bia velo', /* $sName */
      'Namur', /* $sCity */
      50.4673883, /* $dLat */
      4.8719854, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      16, /* $iId */
      'Nancy', /* $sTag */
      'vélOstan\'lib', /* $sName */
      'Nancy', /* $sCity */
      48.692054, /* $dLat */
      6.184417, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      17, /* $iId */
      'Creteil', /* $sTag */
      'Cristolib', /* $sName */
      'Creteil', /* $sCity */
      48.790367, /* $dLat */
      2.455572, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      18, /* $iId */
      'Bruxelles-Capitale', /* $sTag */
      'villo', /* $sName */
      'Bruxelles', /* $sCity */
      50.8503463, /* $dLat */
      4.3517211, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      19, /* $iId */
      'Cergy-Pontoise', /* $sTag */
      'Velo2', /* $sName */
      'Cergy-Pontoise', /* $sCity */
      49.038946, /* $dLat */
      2.075368, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      20, /* $iId */
      'Vilnius', /* $sTag */
      'Cyclocity', /* $sName */
      'Vilnius', /* $sCity */
      54.6871555, /* $dLat */
      25.2796514, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      21, /* $iId */
      'Toyama', /* $sTag */
      'Cyclocity', /* $sName */
      'Toyama', /* $sCity */
      36.6959518, /* $dLat */
      137.2136768, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      22, /* $iId */
      'Kazan', /* $sTag */
      'Veli\'k', /* $sName */
      'Kazan', /* $sCity */
      55.8005556, /* $dLat */
      49.1055556, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      23, /* $iId */
      'Marseille', /* $sTag */
      'Le vélo', /* $sName */
      'Marseille', /* $sCity */
      43.296482, /* $dLat */
      5.36978, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      24, /* $iId */
      'Nantes', /* $sTag */
      'Bicloo', /* $sName */
      'Nantes', /* $sCity */
      47.218371, /* $dLat */
      -1.553621, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      24, /* $iId */
      'Besancon', /* $sTag */
      'Vélocié', /* $sName */
      'Besancon', /* $sCity */
      47.237829, /* $dLat */
      6.0240539, /* $dLng */
      13, /* $iZoom */
      'DataProvider_JCDecaux', /* $sProvider */
      '' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      25, /* $iId */
      'CityBike', /* $sTag */
      'CityBike', /* $sName */
      'New York', /* $sCity */
      40.7324926, /* $dLat */
      -73.9960737, /* $dLng */
      13, /* $iZoom */
      'DataProvider_Alta', /* $sProvider */
      'http://www.citibikenyc.com/stations/json' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      26, /* $iId */
      'SanFrancisco', /* $sTag */
      'Bay Area Bike Share', /* $sName */
      'San Francisco', /* $sCity */
      37.7917231, /* $dLat */
      -122.4072589, /* $dLng */
      13, /* $iZoom */
      'DataProvider_Alta', /* $sProvider */
      'http://www.bayareabikeshare.com/stations/json' /* $sUrl */
    );

    $this->_aContracts[] = new Contract(
      27, /* $iId */
      'Toronto', /* $sTag */
      'Bixi', /* $sName */
      'Toronto', /* $sCity */
      43653226 / 1E6, /* $dLat */
      -79383184 / 1E6, /* $dLng */
      13, /* $iZoom */
      'DataProvider_Alta', /* $sProvider */
      'http://www.bikesharetoronto.com/stations/json' /* $sUrl */
    );

    $this->_aContracts[28] = new Contract(
      28, /* $iId */
      'Divvy', /* $sTag */
      'Divvy', /* $sName */
      'Chicago', /* $sCity */
      41878113 / 1E6, /* $dLat */
      -87629798 / 1E6, /* $dLng */
      13, /* $iZoom */
      'DataProvider_Alta', /* $sProvider */
      'http://www.divvybikes.com/stations/json' /* $sUrl */
    );

    $aContracts = [];
    foreach ($this->_aContracts AS $aContract) {
      $aContracts[$aContract->getId()] = $aContract;
    }

    $this->_aContracts = $aContracts;
//    $this->_aContracts[5] = new Contract(
//      5, /* $iId */
//      'Washington', /* $sTag */
//      'Capital Bike Share', /* $sName */
//      'Washington, DC', /* $sCity */
//      38896758, /* $dLat */
//      -77037016, /* $dLng */
//      13, /* $iZoom */
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