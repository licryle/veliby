package com.licryle.veliby.BikeMap;

import android.os.Bundle;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;

import com.licryle.POICityMap.datastructure.City;
import com.licryle.POICityMap.datastructure.POI;

/**
 * Created by licryle on 8/25/15.
 */
public class BikeStation extends POI implements Serializable {
  protected static int _iNumber;
  /*protected static boolean _bBanking;
  protected static boolean _bBonus;
  protected static int _iBikeStands;*/

  // Dynamic data
  protected static String OPENED = "open";
  protected static String BIKES = "bikes";
  protected static String SPOTS = "free";

  public BikeStation(City mCity, JSONObject mStation)
      throws JSONException {
    super(mCity, mStation);

    _iNumber = _iId;
    _iId = generateId(_mCity.getId(), _iNumber);

    /*_bBanking = false; // mStation.getBoolean("banking");
    _bBonus = false; //mStation.getBoolean("bonus");
    _iBikeStands = -1;//mStation.getInt("bike_stands");*/

    Bundle mNewData = new Bundle();
    mNewData.putInt(BIKES, mStation.getInt(BIKES));
    mNewData.putInt(SPOTS, mStation.getInt(SPOTS));

    mNewData.putBoolean(OPENED,
        (mStation.getInt(BIKES) > 0) || (mStation.getInt(SPOTS) > 0));

    _updateDynamicData(mNewData);
  }

  public BikeStation(BikeStation mOriginal) {
    super(mOriginal);

    _iNumber = mOriginal.getNumber();

    Bundle mNewData = new Bundle();
    mNewData.putInt(BIKES, mOriginal.getAvailableBikes());
    mNewData.putInt(SPOTS, mOriginal.getAvailableBikeStands());
    /*
    _bBanking = mOriginal.hasBanking();
    _bBonus = mOriginal.hasBonus();
    _iBikeStands = mOriginal._iBikeStands;
    */
  }

  public int getNumber() { return _iNumber; }
  /*public boolean hasBanking() { return _bBanking; }
  public boolean hasBonus() { return _bBonus; }*/

  public boolean isOpened() { return getDynamicData().getBoolean(OPENED); }
  public int getAvailableBikes() { return getDynamicData().getInt(BIKES); }
  public int getAvailableBikeStands() {
    return getDynamicData().getInt(SPOTS);
  }

  public void update(boolean bStatus, int iAvBikes, int iAvBikeStands) {
    Bundle mNewData = new Bundle();
    mNewData.putBoolean(OPENED, bStatus);
    mNewData.putInt(BIKES, iAvBikes);
    mNewData.putInt(SPOTS, iAvBikeStands);

    _updateDynamicData(mNewData);
  }

  public static int generateId(int iCityId, int iPOINumber) {
    return iCityId * 1000000 + iPOINumber;
  }
}
