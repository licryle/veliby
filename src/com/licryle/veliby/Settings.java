package com.licryle.veliby;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

public class Settings {
  private static Settings _mInstance;
  protected Context _mContext;
  protected SharedPreferences _mPrefs;

  protected int _iStaticDeadline = 7;
  protected int _iDyanmicDeadline = 3;

  protected final String URL_FULL = "https://api.jcdecaux.com/vls/v1/stations" +
      "?contract=Paris&apiKey=718b4e0e0b1f01af842ff54c38bed00eaa63ce3c";
  protected final String URL_DYNAMIC = "http://veliby.berliat.fr/?c=1";

  protected final static Hashtable<Integer, Integer> _mBikeResources = 
  new Hashtable<Integer, Integer>() {
    private static final long serialVersionUID = -6956564905991202734L;
    {
      put(0,R.color.infoview_nobike);
      put(2,R.color.infoview_fewbikes);
      put(4,R.color.infoview_somebikes);
      put(1000,R.color.infoview_plentybikes);
    }
  };

  private Settings(Context mContext) {
    _mContext = mContext;
    _mPrefs = _mContext.getSharedPreferences(_mContext.getPackageName(),
        Context.MODE_PRIVATE);

    /*try
{
    String app_ver = this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName;
}
catch (NameNotFoundException e) {}*/
  }

  public static Settings getInstance(Context mContext) {
    if (_mInstance == null) {
      _mInstance = new Settings(mContext);
    }

    return _mInstance;
  }

  public Hashtable<Integer, Integer> getBikeColors() {
    return _mBikeResources;
  }
  public boolean isFirstStart() {
    return ! _mPrefs.getBoolean("previously_started", false);
  }

  public void firstStart() {
    _mPrefs.edit().putBoolean("previously_started", true).commit();
  }

  protected boolean isStationFavorite(int iStationId) {
    return _mPrefs.getBoolean("favstation_" + iStationId, false);
  }

  protected void setStationFavorite(int iStationId, boolean bFavorite) {
    String sKey = "favstation_" + iStationId;

    if (!bFavorite) {
      _mPrefs.edit().remove(sKey).commit();
    } else {
      _mPrefs.edit().putBoolean(sKey, true).commit();
    }
  }

  public ArrayList<Integer> getFavStations() {
    ArrayList<Integer> aResults = new ArrayList<Integer>();

    Map<String, ?> items = _mPrefs.getAll();
    for(String s : items.keySet()){
      if (s.matches("favstation_[0-9]+")) {
        int iStationId = Integer.parseInt(
            s.replaceAll("favstation_([0-9]+)", "$1"));

        aResults.add(iStationId);
      }
    }

    return aResults;
  }

  public int getStaticDeadLine() {
    return _iStaticDeadline;
  }

  public int getDynamicDeadLine() {
    return _iDyanmicDeadline;
  }

  public String getURLDownloadDynamic() {
    return URL_DYNAMIC;
  }

  public String getURLDownloadFull() {
    return URL_FULL;
  }

  public File getVelibyPath() {
    return new File(Environment.getExternalStorageDirectory().getPath() +
        "/Veliby/");
  }

  public File getStationsFile() {
    return new File(getVelibyPath().getAbsolutePath() + "/stations.comlete");
  }
}