package com.licryle.veliby.BikeMap;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.licryle.veliby.R;
import com.licryle.veliby.Settings;
import com.licryle.veliby.Util;

public class BikeMap implements OnMarkerClickListener, OnMapClickListener,
    InfoWindowAdapter {
	private GoogleMap _mMap;

	protected Activity _mContext = null;
	protected File _mStationsDataFile = null;

  protected boolean	_bModeFindBike = true;
  protected boolean	_bDownloading = false;

  protected ArrayList<BikeMapListener> _mListeners;
  protected Stations _mStations = null;


	protected static Hashtable<Integer, Integer> _mBikeResources = 
			new Hashtable<Integer, Integer>() {
        private static final long serialVersionUID = -276505145697466182L;
				{
					put(0, R.drawable.presence_invisible);
					put(2, R.drawable.presence_busy);
					put(4, R.drawable.presence_away);
					put(1000, R.drawable.presence_online);
				}
			};


	public BikeMap(Activity mContext, File mStationsDataFile, GoogleMap mMap,
	    int iDeadLine) {
    _mListeners = new ArrayList<BikeMapListener>();

    _mMap = mMap;
    _mMap.setInfoWindowAdapter(this);
    _mContext = mContext;
    _mStationsDataFile = mStationsDataFile;

    setupMap();
	}

  public boolean isDownloading() { return _bDownloading; }
  public boolean isFindBikeMode() { return _bModeFindBike; }
  public Stations stations() { return _mStations; }

  public void changeBikeMode(boolean bFindBike) {
    _bModeFindBike = bFindBike;
    updateMarkers();
  }

  public boolean downloadMarkers() {
    Log.i("BikeMap", "Entered downloadMarkers()");
    if (_bDownloading) return false;
    _bDownloading = true;

    Settings mSettings = Settings.getInstance(_mContext);
    Intent intent = new Intent(_mContext, DownloadStationsService.class);

    intent.putExtra("receiver",
        (Parcelable) new DownloadStationsReceiver(new Handler()));
    intent.putExtra("url_full", mSettings.getURLDownloadFull());
    intent.putExtra("url_dynamic", mSettings.getURLDownloadDynamic());
    intent.putExtra("dl_static", mSettings.getStaticDeadLine());
    intent.putExtra("dl_dynamic", mSettings.getDynamicDeadLine());
    intent.putExtra("stations_file",
        mSettings.getStationsFile().getAbsolutePath());

    Log.i("BikeMap", "Starting Intent in downloadMarkers()");
    _mContext.startService(intent);
    return true;
  }

  public void updateCamera(CameraUpdate mCameraUpdate) {
    _mMap.animateCamera(mCameraUpdate, 700, null);
  }

  protected void updateMarkers() {
    if (_mMap == null || _mStations == null) return;
    
    _mMap.clear();
    Iterator<Map.Entry<Integer, Station>> it = _mStations.entrySet().
        iterator();

    while (it.hasNext()) {
      Map.Entry<Integer, Station> entry = it.next();
      Station mStation = entry.getValue();

      MarkerOptions mOpts = new MarkerOptions();
      mOpts.position(mStation.getPosition());
      mOpts.title(mStation.getName());
      
      int id = mStation.getId();
      mOpts.title(String.valueOf(id));

      int iIcon;
      if (! mStation.isOpened()) {
        iIcon = R.drawable.presence_offline;
      } else {
        int iBikes = (_bModeFindBike) ?
             mStation.getAvailableBikes() :
             mStation.getAvailableBikeStands();

        iIcon = Util.resolveResourceFromNumber(_mBikeResources, iBikes);
      }

      mOpts.icon(BitmapDescriptorFactory.fromResource(iIcon));
      _mMap.addMarker(mOpts);
    }
  }


  protected boolean setupMap() {
    if (_mMap == null) return false;

    _mMap.setMyLocationEnabled(true);
    _mMap.setOnMarkerClickListener(this);
    _mMap.setOnMapClickListener(this);

    LocationManager lm = (LocationManager) _mContext.getSystemService(
        Context.LOCATION_SERVICE);
    Criteria crit = new Criteria();
    crit.setAccuracy(Criteria.ACCURACY_FINE);
    String provider = lm.getBestProvider(crit, true);
    Location lastKnownLocation = lm.getLastKnownLocation(provider);
    if (lastKnownLocation != null) {
      LatLng ll = new LatLng(lastKnownLocation.getLatitude(),
          lastKnownLocation.getLongitude());

      CameraUpdate cu = CameraUpdateFactory.newCameraPosition(
          new CameraPosition(ll, 16, 0, 0));
      updateCamera(cu);
    }

    return true;
  }

  @Override
  public boolean onMarkerClick(Marker mMarker) {
    if (_mStations == null) return false;

    String sStationId = mMarker.getTitle();
    Station mStation = _mStations.get(Integer.valueOf(sStationId));

    dispatchOnStationClick(mStation);
    return false;
  }

  @Override
  public void onMapClick(LatLng mLatLng) {
    dispatchOnMapClick(mLatLng); 
  }

  public void registerBikeMapListener(BikeMapListener mListener) {
    _mListeners.add(mListener);
  }

  protected void dispatchOnStationClick(Station mStation) {
    for (BikeMapListener mListener: _mListeners) {
      mListener.onStationClick(this, mStation);
    }
  }

  protected void dispatchOnMapClick(LatLng mLatLng) {
    for (BikeMapListener mListener: _mListeners) {
      mListener.onMapClick(this, mLatLng);
    }
  }

  protected void dispatchOnDownloadFailure() {
    for (BikeMapListener mListener: _mListeners) {
      mListener.onDownloadFailure(this);
    }
  }

  protected void dispatchOnDownloadSuccess() {
    for (BikeMapListener mListener: _mListeners) {
      mListener.onDownloadSuccess(this);
    }
  }

  protected View dispatchOnGetInfoContents(Marker mMarker, Station mStation) {
    View mResult = null;
    for (BikeMapListener mListener: _mListeners) {
      mResult = mListener.onGetInfoContents(mMarker, mStation);
    }

    return mResult;
  }



  private class DownloadStationsReceiver extends ResultReceiver
      implements Serializable{
    /**
     * 
     */
    private static final long serialVersionUID = 5529673172741409950L;

    public DownloadStationsReceiver(Handler handler) {
      super(handler);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
      super.onReceiveResult(resultCode, resultData);

      switch (resultCode) {
        case DownloadStationsService.SUCCESS:
          Log.i("BikeMap", "onReceiveResult() SUCCESS");
          // date doesn't matter since it was just generated
          _mStations = (Stations) resultData.getSerializable("stations");
  
          updateMarkers();
          dispatchOnDownloadSuccess();
          _bDownloading = false;
        break;

        case DownloadStationsService.FAILURE_CONNECTION:
        case DownloadStationsService.FAILURE_GENERIC:
        case DownloadStationsService.FAILURE_PARSE:
          Log.i("BikeMap", "onReceiveResult() FAILURE_xxxx");
          dispatchOnDownloadFailure();
        break;

        case DownloadStationsService.FINISHED:
          Log.i("BikeMap", "onReceiveResult() FINISHED");
        break;
      }
    }
  }



  @Override
  public View getInfoContents(Marker mMarker) {
    if (_mStations == null) return null;

    String sStationId = mMarker.getTitle();
    Station mStation = _mStations.get(Integer.valueOf(sStationId));

    return dispatchOnGetInfoContents(mMarker, mStation);
  }

  @Override
  public View getInfoWindow(Marker mMarker) {
    return null;
  }
}
