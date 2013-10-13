package com.licryle.veliby.BikeMap;

public interface BikeMapListener {
  void OnDownloadFailure(BikeMap mBikeMap);
  void OnDownloadSuccess(BikeMap mBikeMap);
}
