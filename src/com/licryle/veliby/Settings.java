package com.licryle.veliby;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import com.google.android.gms.maps.model.PolylineOptions;
import com.licryle.veliby.BikeMap.Contract;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

public class Settings implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = 7918647910557505183L;
  private static Settings _mInstance;
  protected Context _mContext;
  protected SharedPreferences _mPrefs;

  protected final static int _iStaticDeadline = 7;
  protected final static int _iDynamicDeadline = 3;
  protected final static int _iContractsDeadline = 10;
  protected final static int _iWidgetUpdateFrequency = 10;

  protected final static String URL_CONTRACTS =
      "http://api.citybik.es/networks.json";
  protected final static String URL_DYNAMIC =
      "http://veliby.berliat.fr/v2/?c=%d";

  protected final static Hashtable<Integer, Integer> _mBikeResources = 
  new Hashtable<Integer, Integer>() {
    private static final long serialVersionUID = -6956564905991202734L;
    {
      put(0, R.color.infoview_nobike);
      put(2, R.color.infoview_fewbikes);
      put(4, R.color.infoview_somebikes);
      put(1000, R.color.infoview_plentybikes);
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

  public static int getStaticDeadLine() {
    return _iStaticDeadline;
  }

  public static int getDynamicDeadLine() {
    return _iDynamicDeadline;
  }

  public static int getContractsDeadLine() {
    return _iContractsDeadline;
  }

  public static int getWidgetUpdateFrequency() {
    return _iWidgetUpdateFrequency;
  }

  public int getCurrentContractId() {
    return _mPrefs.getInt("current_contract", 1);    
  }

  public void setCurrentContract(Contract mContract) {
    _mPrefs.edit().putInt("current_contract", mContract.getId()).commit();
  }

  @SuppressLint("DefaultLocale")
  public static String getURLDownloadDynamic(Contract mContract) {
    return String.format(URL_DYNAMIC, mContract.getId());
  }

  public static String getURLDownloadFull(Contract mContract) {
    return mContract.getUrl();
  }

  public static String getURLContracts() {
    return URL_CONTRACTS;
  }

  public static File getVelibyPath() {
    return new File(Environment.getExternalStorageDirectory().getPath() +
        "/Veliby/");
  }

  public static File getStationsFile() {
    return new File(getVelibyPath().getAbsolutePath() + "/stations.comlete");
  }

  public static File getContractsFile() {
    return new File(getVelibyPath().getAbsolutePath() + "/contracts");
  }

  public boolean isFavStationsOnly() {
    return _mPrefs.getBoolean("favorite_only", false);
  }


  public void setFavStationsOnly(boolean bFavOnly) {
    _mPrefs.edit().putBoolean("favorite_only", bFavOnly).commit();
  }

  public PolylineOptions getDirectionsStyle() {
    PolylineOptions mOptions = new PolylineOptions();

    mOptions.color(
        _mContext.getResources().getColor(R.color.veliby_purple_light));
    mOptions.width(10);

    return mOptions;
  }
}