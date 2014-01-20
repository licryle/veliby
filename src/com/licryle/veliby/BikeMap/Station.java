package com.licryle.veliby.BikeMap;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import com.google.android.gms.maps.model.LatLng;

@SuppressLint("DefaultLocale")
public class Station implements Serializable {
  private static final long serialVersionUID = -967068502674805726L;

	// Static data
	protected int _iNumber;
	protected int _iId;
	protected Contract _mContract;

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

	public Station(Contract mContract, JSONObject mStation)
	    throws JSONException {
    _mContract = mContract;
		_iNumber = mStation.getInt("id");
		_iId = _mContract.getId() * 1000 + _iNumber;

		_sName = mStation.getString("name");
		_sAddress = ""; //mStation.getString("address");

		_dLat = mStation.getDouble("lat") / 1E6;
		_dLng = mStation.getDouble("lng") / 1E6;

		_bBanking = false; // mStation.getBoolean("banking");
		_bBonus = false; //mStation.getBoolean("bonus");
		_iBikeStands = -1;//mStation.getInt("bike_stands");

		_iAvBikes = mStation.getInt("bikes");
		_iAvBikeStands = mStation.getInt("free");
    _bOpened = (_iAvBikes > 0) || (_iAvBikeStands > 0);

		_bShallow = false;
		_bStaticOnly = false;
	}

	public Station(Station mOriginal) {
		_bShallow = mOriginal.isShallow();
		_bStaticOnly = mOriginal.isStaticOnly();

		_iNumber = mOriginal.getNumber();
		_mContract = mOriginal.getContract();
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

	/*public Station(int iId, int iAvBikes, int iAvBikeStands, boolean bOpened) {
		_iId = iId;
		_mContract = Contracts.findContractById(iId / 1000000);
		_iNumber = iId - _mContract.getId() * 1000000;

		_bOpened = bOpened;
		_iAvBikes = iAvBikes;
		_iAvBikeStands = iAvBikeStands;

		_bShallow = true;
		_bStaticOnly = false;
	}*/

	public int getNumber() { return _iNumber; }
	public Contract getContract() { return _mContract; }
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
