package com.licryle.veliby.BikeMap;

import android.view.View;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public interface BikeMapListener {
  void onDownloadFailure(BikeMap mBikeMap);
  void onDownloadSuccess(BikeMap mBikeMap);
  void onStationClick(BikeMap mBikeMap, Station mStation);
  void onMapClick(BikeMap bikeMap, LatLng mLatLng);
  View onGetInfoContents(Marker mMarker, Station mStation);
}
