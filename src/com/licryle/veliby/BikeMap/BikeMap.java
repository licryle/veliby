package com.licryle.veliby.BikeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Calendar;
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
import android.os.ResultReceiver;
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
import com.licryle.veliby.Util;

public class BikeMap implements OnMarkerClickListener, OnMapClickListener,
    InfoWindowAdapter {
	private GoogleMap _mMap;

	protected Activity _mContext = null;
	protected File _mStationsDataFile = null;

  protected boolean	_bModeFindBike = true;
  protected boolean	_bDownloading = false;
  protected Hashtable<Integer, Station> _mStations;

  protected ArrayList<BikeMapListener> _mListeners; 


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


	public BikeMap(Activity mContext, File mStationsDataFile, GoogleMap mMap) {
    _mStations = new Hashtable<Integer, Station>();
    _mListeners = new ArrayList<BikeMapListener>();

    _mMap = mMap;
    _mMap.setInfoWindowAdapter(this);
    _mContext = mContext;
    _mStationsDataFile = mStationsDataFile;

    loadStaticInfo();
    setupMap();
	}

  @SuppressWarnings("unchecked")
  public boolean loadStaticInfo() {
    try {
      FileInputStream mInput = new FileInputStream(_mStationsDataFile);
      ObjectInputStream mObjectStream = new ObjectInputStream(mInput);
      _mStations = (Hashtable<Integer, Station>) mObjectStream.readObject();
      mObjectStream.close();
      return true;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (StreamCorruptedException e) {
      _mStationsDataFile.delete();
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      // We changed version, let's delete that file
      _mStationsDataFile.delete();
      e.printStackTrace();
    } catch (Exception e) {
      // In doubt, call the shots
      _mStationsDataFile.delete();
      e.printStackTrace();
    }

    _mStations = new Hashtable<Integer, Station>();
    return false;
  }

  public boolean isDownloading() { return _bDownloading; }
  public boolean isFindBikeMode() { return _bModeFindBike; }
  public void changeBikeMode(boolean bFindBike) {
    _bModeFindBike = bFindBike;
    updateMarkers();
  }

  public boolean downloadMarkers(boolean bFullCycle, String sUrl) {
    if (_bDownloading) return false;
    _bDownloading = true;

    Intent intent = new Intent(_mContext, DownloadStationsService.class);

    intent.putExtra("url", sUrl);
    intent.putExtra("full_cycle", bFullCycle);
    intent.putExtra("receiver", new DownloadStationsReceiver(new Handler()));

    _mContext.startService(intent);
    return true;
  }

  public void updateCamera(CameraUpdate mCameraUpdate) {
    _mMap.animateCamera(mCameraUpdate, 700, null);
  }

  protected void updateMarkers() {
    if (_mMap == null) return;
    
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

    updateMarkers();

    return true;
  }

  @Override
  public boolean onMarkerClick(Marker mMarker) {
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



  private class DownloadStationsReceiver extends ResultReceiver{
    public DownloadStationsReceiver(Handler handler) {
      super(handler);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
      super.onReceiveResult(resultCode, resultData);

      switch (resultCode) {
        case DownloadStationsService.SUCCESS:
          if (resultData.getBoolean("full_cycle")) {
            _mStations.clear();
            Hashtable<Integer, Station> mResult = (Hashtable<Integer, Station>) 
                resultData.getSerializable("stations");
   
            for (Map.Entry<Integer, Station> mStation : mResult.entrySet()) {
              _mStations.put(mStation.getKey(), new Station(mStation.getValue()));
            }
  
            _mStationsDataFile.delete();
            FileOutputStream mOutput;
            try {
              mOutput = new FileOutputStream(_mStationsDataFile);
              ObjectOutputStream mObjectStream = new ObjectOutputStream(mOutput);
   
              // Nulling dynamic data for storage
              Iterator<Map.Entry<Integer, Station>> it = mResult.entrySet().
                  iterator();
  
              while (it.hasNext()) {
                Map.Entry<Integer, Station> entry = it.next();
                Station mStation = entry.getValue();
                mStation.update(false, 0, 0, null);
              }
  
              mObjectStream.writeObject(mResult);
              mObjectStream.close();
            } catch (FileNotFoundException e) {
              e.printStackTrace();
            } catch (IOException e) {
              e.printStackTrace();
              _mStationsDataFile.delete(); // TODO: this isn't reliable
            }
          } else {
            Hashtable<Integer, Station> mResult = (Hashtable<Integer, Station>) 
                resultData.getSerializable("stations");
            for (Map.Entry<Integer, Station> mStation : mResult.entrySet()) {
              Station mStationToUp = _mStations.get(mStation.getValue().getId());
  
              if (mStationToUp != null) {
                mStationToUp.update(mStation.getValue().isOpened(),
                    mStation.getValue().getAvailableBikes(),
                    mStation.getValue().getAvailableBikeStands(),
                    Calendar.getInstance().getTime());
              }
            }
          }
  
          _bDownloading = false;
          updateMarkers();
          dispatchOnDownloadSuccess();
        break;

        case DownloadStationsService.FAILURE_CONNECTION:
        case DownloadStationsService.FAILURE_GENERIC:
        case DownloadStationsService.FAILURE_PARSE:
          _bDownloading = false;
          dispatchOnDownloadFailure();
        break;
      }
    }
  }



  @Override
  public View getInfoContents(Marker mMarker) {
    String sStationId = mMarker.getTitle();
    Station mStation = _mStations.get(Integer.valueOf(sStationId));

    return dispatchOnGetInfoContents(mMarker, mStation);
  }

  @Override
  public View getInfoWindow(Marker mMarker) {
    return null;
  }
}
