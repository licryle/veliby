package com.licryle.veliby.BikeMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.View;

import com.directions.route.Route;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.licryle.veliby.R;
import com.licryle.veliby.Settings;
import com.licryle.veliby.StationsInfoService;
import com.licryle.veliby.Util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;


public class BikeMap implements OnMarkerClickListener, OnMapClickListener,
    InfoWindowAdapter, RoutingListener, OnMapReadyCallback {
	private GoogleMap _mMap;

	protected Activity _mContext = null;

  protected boolean	_bModeFindBike = true;
  protected boolean	_bDownloading = false;

  protected ArrayList<BikeMapListener> _aListeners;
  protected Stations _mStations = null;
  protected Contracts _mContracts = new Contracts();
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

  protected static Hashtable<Integer, Integer> _mBikeFavResources =
      new Hashtable<Integer, Integer>() {
        private static final long serialVersionUID = -276505145697466182L;
        {
          put(0, R.drawable.favorite_none);
          put(2, R.drawable.favorite_few);
          put(4, R.drawable.favorite_some);
          put(1000, R.drawable.favorite_plenty);
        }
      };


	public BikeMap(Activity mContext, Settings mSettings) {
    _aListeners = new ArrayList<BikeMapListener>();
    _mSettings = mSettings;

    _mContext = mContext;
	}

  @Override
  public void onMapReady(GoogleMap mMap) {
    _mMap = mMap;
    _mMap.setInfoWindowAdapter(this);
    _setupMap();
  }

  public boolean isMapLoaded() { return _mMap != null; }
  public boolean isDownloading() { return _bDownloading; }
  public boolean isFindBikeMode() { return _bModeFindBike; }
  public Stations getStations() { return _mStations; }
  public Contracts getContracts() { return _mContracts; }

  public void changeBikeMode(boolean bFindBike) {
    _bModeFindBike = bFindBike;
    _updateMarkers();
  }

  public boolean downloadMarkers() {
    Log.i("BikeMap", "Entered downloadMarkers()");
    if (_bDownloading) return false;
    _bDownloading = true;

    Settings mSettings = Settings.getInstance(_mContext);
    Intent intent = new Intent(_mContext, StationsInfoService.class);

    intent.putExtra("receiver",
        (Parcelable) new _DownloadStationsReceiver(new Handler()));
    intent.putExtra("requestor", this.toString());
    intent.putExtra("dl_static", Settings.getStaticDeadLine());
    intent.putExtra("dl_dynamic", Settings.getDynamicDeadLine());
    intent.putExtra("stations_file",
        Settings.getStationsFile().getAbsolutePath());

    intent.putExtra("dl_contracts", Settings.getContractsDeadLine());
    intent.putExtra("contract_id", mSettings.getCurrentContractId());
    intent.putExtra("contracts_url", Settings.getURLContracts());
    intent.putExtra("contracts_file",
        Settings.getContractsFile().getAbsolutePath());

    Log.i("BikeMap", "Starting Intent in downloadMarkers()");
    _mContext.startService(intent);
    return true;
  }

  public void moveCameraOnContract() {
    Contract mContract = _mContracts.findContractById(
        _mSettings.getCurrentContractId());

    if (mContract != null) {
      moveCameraTo(mContract.getPosition(), mContract.getZoom());
    }
  }

  public void moveCameraTo(LatLng mPosition, int iZoom) {
    if (mPosition == null) return;

    CameraUpdate cu = CameraUpdateFactory.newCameraPosition(
        new CameraPosition(mPosition, iZoom, 0, 0));

    updateCamera(cu);
  }

  public void updateCamera(CameraUpdate mCameraUpdate) {
    if (!isMapLoaded()) return;

    _mMap.animateCamera(mCameraUpdate, 700, null);
  }

  protected void _updateMarker(Station mStation, boolean bFavorite) {
    if (!isMapLoaded() || mStation == null) return;

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

      if (bFavorite) {
        iIcon = Util.resolveResourceFromNumber(_mBikeFavResources, iBikes);
      } else {
        iIcon = Util.resolveResourceFromNumber(_mBikeResources, iBikes);
      }
    }

    mOpts.icon(BitmapDescriptorFactory.fromResource(iIcon));
    _mMap.addMarker(mOpts);
  }

  protected void _updateMarkers() {
    if (!isMapLoaded() || _mStations == null) return;
    
    _mMap.clear();

    ArrayList<Integer> aFavStations = _mSettings.getFavStations();
    for(Integer iStationId: aFavStations)
      _updateMarker(_mStations.get(iStationId), true);

    if (! _mSettings.isFavStationsOnly()) {
      Iterator<Map.Entry<Integer, Station>> it = _mStations.entrySet().
          iterator();

      while (it.hasNext()) {
        Map.Entry<Integer, Station> entry = it.next();
        Station mStation = entry.getValue();

        if (! aFavStations.contains(mStation.getId()))
          _updateMarker(mStation, false);
      }
    }
  }

  public void refreshStations() {
    _updateMarkers();
  }

  protected boolean _setupMap() {
    if (!isMapLoaded()) return false;

    _mMap.setMyLocationEnabled(true);
    _mMap.setOnMarkerClickListener(this);
    _mMap.setOnMapClickListener(this);
    _mMap.getUiSettings().setMapToolbarEnabled(false);

    moveCameraTo(Util.getLastPosition(_mContext), 13);

    return true;
  }

  @Override
  public boolean onMarkerClick(Marker mMarker) {
    if (_mStations == null) return false;

    String sStationId = mMarker.getTitle();
    Station mStation = _mStations.get(Integer.valueOf(sStationId));

    _dispatchOnStationClick(mStation);
    return false;
  }

  @Override
  public void onMapClick(LatLng mLatLng) {
    _dispatchOnMapClick(mLatLng);
  }

  public void registerBikeMapListener(BikeMapListener mListener) {
    _aListeners.add(mListener);
  }

  protected void _dispatchOnStationClick(Station mStation) {
    for (BikeMapListener mListener: _aListeners) {
      mListener.onStationClick(this, mStation);
    }
  }

  protected void _dispatchOnMapClick(LatLng mLatLng) {
    for (BikeMapListener mListener: _aListeners) {
      mListener.onMapClick(this, mLatLng);
    }
  }

  protected void _dispatchOnStationsDownloadFailure() {
    for (BikeMapListener mListener: _aListeners) {
      mListener.onStationsDownloadFailure(this);
    }
  }

  protected void _dispatchOnStationsDownloadSuccess() {
    for (BikeMapListener mListener: _aListeners) {
      mListener.onStationsDownloadSuccess(this);
    }
  }

  protected void _dispatchOnContractDownloadFailure() {
    for (BikeMapListener mListener: _aListeners) {
      mListener.onContractDownloadFailure(this);
    }
  }

  protected void _dispatchOnContractDownloadSuccess() {
    for (BikeMapListener mListener: _aListeners) {
      mListener.onContractDownloadSuccess(this);
    }
  }

  protected void _dispatchOnDirectionsFailed() {
    for (BikeMapListener mListener: _aListeners) {
      mListener.onDirectionsFailed(this);
    }
  }

  protected View _dispatchOnGetInfoContents(Marker mMarker, Station mStation) {
    View mResult = null;
    for (BikeMapListener mListener: _aListeners) {
      mResult = mListener.onGetInfoContents(mMarker, mStation);
    }

    return mResult;
  }

  public boolean isAnyStationVisible() {
    Iterator<Map.Entry<Integer, Station>> it = _mStations.entrySet().
        iterator();

    while (it.hasNext()) {
      Map.Entry<Integer, Station> entry = it.next();
      Station mStation = entry.getValue();

      if (_isStationVisible(mStation)) return true;
    }

    return false;
  }

  private boolean _isStationVisible(Station mStation) {
    if (!isMapLoaded()) return false;

    return _mMap.getProjection().getVisibleRegion().latLngBounds.contains(
        mStation.getPosition()
    );
  }


  private class _DownloadStationsReceiver extends ResultReceiver
      implements Serializable{
    /**
     * 
     */
    private static final long serialVersionUID = 5529673172741409950L;

    public _DownloadStationsReceiver(Handler handler) {
      super(handler);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
      super.onReceiveResult(resultCode, resultData);

      switch (resultCode) {
        case StationsInfoService.FAILURE_STATIONS_CONNECTION:
        case StationsInfoService.FAILURE_STATIONS_GENERIC:
        case StationsInfoService.FAILURE_STATIONS_PARSE:
        case StationsInfoService.SUCCESS_STATIONS:
          _mStations = (Stations) resultData.getSerializable("stations");

          // we need this to display at least static data
          if (_mStations != null) {
            _updateMarkers();
          }

          if (resultCode == StationsInfoService.SUCCESS_STATIONS) {
            Log.i("BikeMap", "onReceiveResult() SUCCESS_STATIONS_xxxx");
            _dispatchOnStationsDownloadSuccess();
          } else {
            Log.i("BikeMap", "onReceiveResult() FAILURE_STATIONS_xxxx");
            _dispatchOnStationsDownloadFailure();
          }
        break;

        case StationsInfoService.FAILURE_CONTRACTS_CONNECTION:
        case StationsInfoService.FAILURE_CONTRACTS_GENERIC:
        case StationsInfoService.FAILURE_CONTRACTS_PARSE:
        case StationsInfoService.SUCCESS_CONTRACTS:

          _mContracts = (Contracts) resultData.getSerializable("contracts");

          if (resultCode == StationsInfoService.SUCCESS_CONTRACTS) {
            Log.i("BikeMap", "onReceiveResult() SUCCESS_CONTRACTS_xxxx");
            _dispatchOnContractDownloadSuccess();
          } else {
            Log.i("BikeMap", "onReceiveResult() FAILURE_CONTRACTS_xxxx");
            _dispatchOnContractDownloadFailure();
          }
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

    return _dispatchOnGetInfoContents(mMarker, mStation);
  }

  @Override
  public View getInfoWindow(Marker mMarker) {
    return null;
  }

  public void ShowAllStations() {
    _mSettings.setFavStationsOnly(false);
    _updateMarkers();
  }

  public void ShowFavStations() {
    _mSettings.setFavStationsOnly(true);
    _updateMarkers();
  }

  public void displayDirections(LatLng _mPos) {
    Routing mRouting = new Routing(Routing.TravelMode.WALKING);
    mRouting.registerListener(this);
    mRouting.execute(Util.getLastPosition(_mContext), _mPos);
  }

  public void clearDirections() {
    if (_mCurrentDirections != null) _mCurrentDirections.remove();
  }

  public void onRoutingFailure() {
    clearDirections();
    _dispatchOnDirectionsFailed();
  }

  public void onRoutingStart() {
  }

  public void onRoutingSuccess(PolylineOptions mPolyOptions, Route mRoute) {
    if (!isMapLoaded()) return;

    PolylineOptions mOptions = _mSettings.getDirectionsStyle();
    mOptions.addAll(mPolyOptions.getPoints());

    clearDirections();
    _mCurrentDirections = _mMap.addPolyline(mOptions);
  }
}
