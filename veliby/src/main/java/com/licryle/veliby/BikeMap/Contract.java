package com.licryle.veliby.BikeMap;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Comparator;

public class Contract implements Serializable {
  /**
   * 
   */
  private static final long serialVersionUID = -3698242294551745027L;

  protected final int _iId;
  protected final String _sName;
  protected final String _sCity;
  protected Double _dLng;
  protected Double _dLat;
  protected final int _iZoom;

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
    _sCity = mContract.getString("city");

    _dLat = mContract.getJSONObject("position").getDouble("lat");
    _dLng = mContract.getJSONObject("position").getDouble("lng");

    _iZoom = mContract.getInt("zoom");
  }

  public int getId() { return _iId; }
  public String getName() { return _sName; }
  public String getCity() { return _sCity; }
  public LatLng getPosition() { return new LatLng(_dLat, _dLng); }
  public int getZoom() { return _iZoom; }
}