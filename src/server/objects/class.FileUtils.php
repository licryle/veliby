<?php

class FileUtils {
  public static function isBeingWritten($sFilePath) {
    $mFile = @fopen($sFilePath, 'r');
    if ($mFile == false) return false;

    $bLock = flock($mFile, LOCK_EX | LOCK_NB);

    flock($mFile, LOCK_UN);
    fclose($mFile);

    return $mFile && !$bLock;
  }

  public static function openLockedFileTimeOut($sFilePath, $sMode, $iTimeOut,
      $bRead) {
    $iTime = 0;
    while ($iTime < $iTimeOut && FileUtils::isBeingWritten($sFilePath)) {
      $iTime += 100;
      usleep(100);
    }

    $mFile = fopen($sFilePath, $sMode);
    if (!$mFile) return false;

    $iLock = $bRead ? LOCK_SH : LOCK_EX;
    $bLock = flock($mFile, $iLock | LOCK_NB);
    if (!$bLock) return false;

    return $mFile;
  }
}

?>