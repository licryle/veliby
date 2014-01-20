package com.licryle.veliby.BikeMap;

import java.io.Serializable;
import java.util.Comparator;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;

public class Contract implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = -3698242294551745027L;

  protected final int _iId;
  protected final String _sName;
  protected final String _sTag;
  protected final String _sCity;
  protected Double _dLng;
  protected Double _dLat;
  protected final int _iRadius;
  protected final String _sUrl;

  public class CityNameComparator implements Comparator<Contract> {
      @Override
      public int compare(Contract lhs, Contract rhs) {
        return lhs.getCity().compareToIgnoreCase(rhs.getCity());
      }
  }

  public Contract(JSONObject mContract)
      throws JSONException {
    _iId = mContract.getInt("id");
    _sName = mContract.getString("name");
    _sTag = mContract.getString("tag");
    _sCity = mContract.getString("city");

    _dLat = mContract.getDouble("lat") / 1E6;
    _dLng = mContract.getDouble("lng") / 1E6;

    _sUrl = mContract.getString("url");
    _iRadius = mContract.getInt("radius");
  }

  public int getId() { return _iId; }
  public String getName() { return _sName; }
  public String getTag() { return _sTag; }
  public String getCity() { return _sCity; }
  public LatLng getPosition() { return new LatLng(_dLat, _dLng); }
  public String getUrl() { return _sUrl; }
  public int getRadius() { return _iRadius; }
}