package com.licryle.veliby.BikeMap;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import com.google.android.gms.maps.model.LatLng;

@SuppressLint("DefaultLocale")
public class Station implements Serializable {
  private static final long serialVersionUID = -967068502674805726L;

	public enum Contract {
		PARIS (1, "Paris");

		private final int _iId;
		private final String _sName;

		Contract(int iId, String sName) {
			this._iId = iId;
			this._sName = sName;
		}

		private int getId() { return _iId; }
		private String getName() { return _sName; }

		public static Contract findContractByName(String sName) {
			for (Contract c: Contract.values()) {
				if (c.getName().equalsIgnoreCase(sName) ) {
					return c;
				}
			}

			return null;
		}

		public static Contract findContractById(int iId) {
			for (Contract c: Contract.values()) {
				if (c.getId() == iId) {
					return c;
				}
			}

			return null;
    }
	}

	// Static data
	protected int _iNumber;
	protected Contract _iContract;
	protected int _iId;

	protected String _sName;
	protected String _sAddress;
	protected Double _dLng;
	protected Double _dLat;
	protected boolean _bBanking;
	protected boolean _bBonus;
	protected int _iBikeStands;

	// Dynamic data
	protected boolean _bOpened;
	protected int _iAvBikes;
	protected int _iAvBikeStands;

	// Shallow: only contains dynamic data
  protected boolean _bShallow;
  protected boolean _bStaticOnly;

	public Station(JSONObject mStation) throws JSONException {
		_iNumber = mStation.getInt("number");
		_iContract = Contract.findContractByName(mStation.getString("contract_name"));
		_iId = _iContract.getId() * 1000000 + _iNumber;

		_sName = mStation.getString("name");
		_sAddress = mStation.getString("address");

		JSONObject mPos = mStation.getJSONObject("position");
		_dLat = mPos.getDouble("lat");
		_dLng = mPos.getDouble("lng");

		_bBanking = mStation.getBoolean("banking");
		_bBonus = mStation.getBoolean("bonus");
		_iBikeStands = mStation.getInt("bike_stands");

		_bOpened = mStation.getString("status").equalsIgnoreCase("OPEN");
		_iAvBikes = mStation.getInt("available_bikes");
		_iAvBikeStands = mStation.getInt("available_bike_stands");

		_bShallow = false;
		_bStaticOnly = false;
	}

	public Station(Station mOriginal) {
		_bShallow = mOriginal.isShallow();
		_bStaticOnly = mOriginal.isStaticOnly();

		_iNumber = mOriginal.getNumber();
		_iContract = mOriginal.getContract();
		_iId = mOriginal.getId();

		_bOpened = mOriginal.isOpened();
		_iAvBikes = mOriginal.getAvailableBikes();
		_iAvBikeStands = mOriginal.getAvailableBikeStands();

		if (!isShallow()) {	
			_sName = mOriginal.getName();
			_sAddress = mOriginal.getAddress();
	
			_dLat = mOriginal.getPosition().latitude;
			_dLng = mOriginal.getPosition().longitude;
	
			_bBanking = mOriginal.hasBanking();
			_bBonus = mOriginal.hasBonus();
			_iBikeStands = mOriginal._iBikeStands;
		}
	}

	public Station(int iId, int iAvBikes, int iAvBikeStands, boolean bOpened) {
		_iId = iId;
		_iContract = Contract.findContractById(iId / 1000000);
		_iNumber = iId - _iContract.getId() * 1000000;

		_bOpened = bOpened;
		_iAvBikes = iAvBikes;
		_iAvBikeStands = iAvBikeStands;

		_bShallow = true;
		_bStaticOnly = false;
	}

	public int getNumber() { return _iNumber; }
	public Contract getContract() { return _iContract; }
	public int getId() { return _iId; }
	public String getName() { return _sName; }
	public String getFriendlyName() {
	  String sTitle = this.getName();

	  sTitle = sTitle.replaceAll("[0-9]+ - (.*)", "$1").toLowerCase();
    sTitle = Character.toUpperCase(sTitle.charAt(0)) + sTitle.substring(1);

    return sTitle;
	}
	public String getAddress() { return _sAddress; }
	public LatLng getPosition() { return new LatLng(_dLat, _dLng); }
	public boolean hasBanking() { return _bBanking; }
	public boolean hasBonus() { return _bBonus; }

	public boolean isOpened() { return _bOpened; }
	public int getAvailableBikes() { return _iAvBikes; }
	public int getAvailableBikeStands() { return _iAvBikeStands; }

  public boolean isShallow() { return _bShallow; }
  public boolean isStaticOnly() { return _bStaticOnly; }

  public void removeDynamicData() {
    update(false, 0, 0);
    _bStaticOnly = true;
  }

	public void update(boolean bStatus, int iAvBikes, int iAvBikeStands) {
		_bOpened = bStatus;
		_iAvBikes = iAvBikes;
		_iAvBikeStands = iAvBikeStands;
		_bStaticOnly = false;
	}
}
