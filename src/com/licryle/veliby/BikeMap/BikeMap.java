package com.licryle.veliby.BikeMap;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.View;

import com.directions.route.Routing;
import com.directions.route.RoutingListener;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.licryle.veliby.R;
import com.licryle.veliby.Settings;
import com.licryle.veliby.Util;


public class BikeMap implements OnMarkerClickListener, OnMapClickListener,
    InfoWindowAdapter, RoutingListener {
	private GoogleMap _mMap;

	protected Activity _mContext = null;

  protected boolean	_bModeFindBike = true;
  protected boolean	_bDownloading = false;

  protected ArrayList<BikeMapListener> _aListeners;
  protected Stations _mStations = null;
  protected Settings _mSettings = null;

  protected Polyline _mCurrentDirections = null;

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


	public BikeMap(Activity mContext, GoogleMap mMap, Settings mSettings) {
    _aListeners = new ArrayList<BikeMapListener>();
    _mSettings = mSettings;

    _mMap = mMap;
    _mMap.setInfoWindowAdapter(this);
    _mContext = mContext;

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
    Intent intent = new Intent(_mContext, StationsInfoService.class);

    intent.putExtra("receiver",
        (Parcelable) new DownloadStationsReceiver(new Handler()));
    intent.putExtra("requestor", this.toString());
    intent.putExtra("url_full",
        mSettings.getURLDownloadFull(mSettings.getCurrentContract()));
    intent.putExtra("url_dynamic",
        mSettings.getURLDownloadDynamic(mSettings.getCurrentContract()));
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

    boolean bShowfavOnly = _mSettings.isFavStationsOnly();
    ArrayList<Integer> aFavStations = _mSettings.getFavStations();
    while (it.hasNext()) {
      Map.Entry<Integer, Station> entry = it.next();
      Station mStation = entry.getValue();

      if (! bShowfavOnly ||
          (bShowfavOnly && aFavStations.contains(mStation.getId()))) {
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
  }


  protected boolean setupMap() {
    if (_mMap == null) return false;

    _mMap.setMyLocationEnabled(true);
    _mMap.setOnMarkerClickListener(this);
    _mMap.setOnMapClickListener(this);

    LatLng mlastKnownPos = Util.getLastPosition(_mContext);
    if (mlastKnownPos != null) {
      CameraUpdate cu = CameraUpdateFactory.newCameraPosition(
          new CameraPosition(mlastKnownPos, 16, 0, 0));
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
    _aListeners.add(mListener);
  }

  protected void dispatchOnStationClick(Station mStation) {
    for (BikeMapListener mListener: _aListeners) {
      mListener.onStationClick(this, mStation);
    }
  }

  protected void dispatchOnMapClick(LatLng mLatLng) {
    for (BikeMapListener mListener: _aListeners) {
      mListener.onMapClick(this, mLatLng);
    }
  }

  protected void dispatchOnDownloadFailure() {
    for (BikeMapListener mListener: _aListeners) {
      mListener.onDownloadFailure(this);
    }
  }

  protected void dispatchOnDownloadSuccess() {
    for (BikeMapListener mListener: _aListeners) {
      mListener.onDownloadSuccess(this);
    }
  }

  protected void dispatchOnDirectionsFailed() {
    for (BikeMapListener mListener: _aListeners) {
      mListener.onDirectionsFailed(this);
    }
  }

  protected View dispatchOnGetInfoContents(Marker mMarker, Station mStation) {
    View mResult = null;
    for (BikeMapListener mListener: _aListeners) {
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
        case StationsInfoService.SUCCESS:
          Log.i("BikeMap", "onReceiveResult() SUCCESS");
          // date doesn't matter since it was just generated
          _mStations = (Stations) resultData.getSerializable("stations");
  
          updateMarkers();
          dispatchOnDownloadSuccess();
          _bDownloading = false;
        break;

        case StationsInfoService.FAILURE_CONNECTION:
        case StationsInfoService.FAILURE_GENERIC:
        case StationsInfoService.FAILURE_PARSE:
          Log.i("BikeMap", "onReceiveResult() FAILURE_xxxx");

          _mStations = (Stations) resultData.getSerializable("stations");

          // we need this to display at least static data
          updateMarkers();

          dispatchOnDownloadFailure();
          _bDownloading = false;
        break;

        case StationsInfoService.FINISHED:
          Log.i("BikeMap", "onReceiveResult() FINISHED");
          _bDownloading = false;
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

  public void ShowAllStations() {
    _mSettings.setFavStationsOnly(false);
    updateMarkers();
  }

  public void ShowFavStations() {
    _mSettings.setFavStationsOnly(true);
    updateMarkers();
  }

  public void displayDirections(LatLng _mPos) {
    Routing mRouting = new Routing(_mContext, Routing.TravelMode.WALKING);
    mRouting.registerListener(this);
    mRouting.execute(Util.getLastPosition(_mContext), _mPos);
  }

  public void clearDirections() {
    if (_mCurrentDirections != null) _mCurrentDirections.remove();
  }

  @Override
  public void onRoutingFailure() {
    clearDirections();
    dispatchOnDirectionsFailed();
  }

  @Override
  public void onRoutingStart() {
  }

  @Override
  public void onRoutingSuccess(PolylineOptions mPolyOptions) {
    PolylineOptions mOptions = _mSettings.getDirectionsStyle();
    mOptions.addAll(mPolyOptions.getPoints());

    clearDirections();
    _mCurrentDirections = _mMap.addPolyline(mOptions);
  }
}
