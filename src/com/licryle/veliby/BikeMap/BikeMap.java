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

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.licryle.veliby.R;
import com.licryle.veliby.Util;
import com.licryle.veliby.UI.Maps_InfoWindowAdapter;

public class BikeMap {
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
    _mContext = mContext;
    _mStationsDataFile = mStationsDataFile;

    loadStaticInfo();
    setupMap();
	}

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
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      // We changed version, let's delete that file
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
    intent.putExtra("receiver", new DownloadReceiver(new Handler()));

    _mContext.startService(intent);
    return true;
  }

  private class DownloadReceiver extends ResultReceiver{
    public DownloadReceiver(Handler handler) {
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
                mStation.update(false, 0, 0);
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
                    mStation.getValue().getAvailableBikeStands());
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

  protected void updateMarkers() {
    if (_mStations == null || _mMap == null) return;
    
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
      mOpts.snippet(String.valueOf(id));

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


  private boolean setupMap() {
    if (_mMap == null) return false;

    _mMap.setMyLocationEnabled(true);
    _mMap.setInfoWindowAdapter(new Maps_InfoWindowAdapter(_mContext, _mStations));

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
      _mMap.animateCamera(cu, 700, null);
    }

    updateMarkers();

    return true;
  }

  public void registerBikeMapListener(BikeMapListener mListener) {
    _mListeners.add(mListener);
  }

  protected void dispatchOnDownloadFailure() {
    for (BikeMapListener mListener: _mListeners) {
      mListener.OnDownloadFailure(this);
    }
  }

  protected void dispatchOnDownloadSuccess() {
    for (BikeMapListener mListener: _mListeners) {
      mListener.OnDownloadSuccess(this);
    }
  }
}
