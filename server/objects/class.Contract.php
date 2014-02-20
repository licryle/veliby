<?php

require_once(PATH_OBJECTS . 'class.DynamicStation.php');

class Contract {
  protected $_iId;
  protected $_sTag;
  protected $_sName;
  protected $_sCity;

  protected $_aPos;
  protected $_iRadius;

  protected $_sProvider;
  protected $_sUrl;

  public function __construct($iId, $sTag, $sName, $sCity, $iLat, $iLng,
      $iRadius, $sProvider, $sUrl) {
    $this->_iId = intval($iId);
    $this->_sTag = $sTag;

    $this->_sName = $sName;
    $this->_sCity = $sCity;

    $this->_aPos = array('lat' => floatval($iLat),
        'lng' => floatval($iLng));
    $this->_iRadius = intval($iRadius);
    $this->_sProvider = $sProvider;
    $this->_sUrl = $sUrl;
  }

  public function getId() { return $this->_iId; }
  public function getTag() { return $this->_sTag; }
  public function getName() { return $this->_sName; }
  public function getCity() { return $this->_sCity; }
  public function getPosition() { return $this->_aPos; }
  public function getRadius() { return $this->_iRadius; }
  public function getProvider() { return $this->_sProvider; }
  public function getUrl() { return $this->_sUrl; }

  public function getArray() {
    $aArray = Array();
    $aArray['id'] = $this->_iId;
    $aArray['tag'] = $this->_sTag;
    $aArray['name'] = $this->_sName;
    $aArray['city'] = $this->_sCity;

    $aArray['position'] = $this->_aPos;
    $aArray['radius'] = $this->_iRadius;
    $aArray['provider'] = $this->_sProvider;
    $aArray['url'] = $this->_sUrl;

    return $aArray;
  }

  public function buildStationsFilePath($sFileBase, $bFullData) {
    return $sFileBase . '_' . ($bFullData ? 'full_' : '') . $this->getId();
  }

  public function updateDynamicStations($sFileBase, $iTimeout) {
    if (!$this->writeStations($sFileBase, $iTimeout))
      return false;

    return true;
  }

  protected function fetchStations() {
    $sDataProvider = $this->getProvider();
    $mDataProvider = new $sDataProvider($this);

    $sUrl = $mDataProvider->generateStationsURL($this);
    $sStations = file_get_contents($sUrl);

    return $mDataProvider->parseStations($sStations);
  }

  protected function writeStations($sFileBase, $iTimeout){
    $bResult = true;
    $sFullFile    = $this->buildStationsFilePath($sFileBase, true);
    $sDynamicFile = $this->buildStationsFilePath($sFileBase, false);

    if (!file_exists($sFullFile) ||
        (filemtime($sFullFile) +  $iTimeout) < time() ||
        !file_exists($sDynamicFile) ||
        (filemtime($sDynamicFile) +  $iTimeout) < time()) {


      $bConcurrent = FileUtils::isBeingWritten($sFullFile) || 
          FileUtils::isBeingWritten($sDynamicFile);

      if (!$bConcurrent) {
        // None of the files being writen, take the stab at writing it
        $mFullFile = FileUtils::openLockedFileTimeOut($sFullFile, 'wb', 500, false);
        flock($mFullFile, LOCK_EX);

        $mDynamicFile = FileUtils::openLockedFileTimeOut($sDynamicFile, 'wb', 500, false);
        flock($mDynamicFile, LOCK_EX);

        $aStations = $this->fetchStations();

        if (!$aStations) {
          $bResult = false;
        } else {
          $aAllStations = [];

          foreach ($aStations as $key => $mStation) {
            fwrite($mDynamicFile, $mStation->getMinimizedDynamic());
            $aAllStations[] = $mStation->getArray();
          }

          fwrite($mFullFile, json_encode($aAllStations));
        }

        fflush($mFullFile);            // flush output before releasing the lock
        touch($sFullFile);
        flock($mFullFile, LOCK_UN);    // release the lock

        fflush($mDynamicFile);            // flush output before releasing the lock
        touch($sDynamicFile);
        flock($mDynamicFile, LOCK_UN);    // release the lock
      }
    }

    return $bResult;
  }
}

?>