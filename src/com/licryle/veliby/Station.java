package com.licryle.veliby;

import java.sql.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.maps.model.LatLng;

public class Station {
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
	}

	// Static data
	protected int _iNumber;
	protected Contract _iContract;
	protected int _iId;

	protected String _sName;
	protected String _sAddress;
	protected LatLng _mPos;
	protected boolean _bBanking;
	protected boolean _bBonus;
	protected int _iBikeStands;

	// Dynamic data
	protected boolean _bOpened;
	protected int _iAvBikes;
	protected int _iAvBikeStands;
	protected Date _mLastUpdate;

	public Station(JSONObject mStation) throws JSONException {
		_iNumber = mStation.getInt("number");
		_iContract = Contract.findContractByName(mStation.getString("contract_name"));
		_iId = _iContract.getId() * 10000000 + _iNumber;

		_sName = mStation.getString("name");
		_sAddress = mStation.getString("address");

		JSONObject mPos = mStation.getJSONObject("position");
		_mPos = new LatLng(mPos.getDouble("lat"),
											 mPos.getDouble("lng"));

		_bBanking = mStation.getBoolean("banking");
		_bBonus = mStation.getBoolean("bonus");
		_iBikeStands = mStation.getInt("bike_stands");

		_bOpened = mStation.getString("status").equalsIgnoreCase("OPEN");
		_iAvBikes = mStation.getInt("available_bikes");
		_iAvBikeStands = mStation.getInt("available_bike_stands");
		_mLastUpdate = new Date(mStation.getLong("last_update"));
	}

	public int getNumber() { return _iNumber; }
	public Contract getContract() { return _iContract; }
	public int getId() { return _iId; }
	public String getName() { return _sName; }
	public String getAddress() { return _sAddress; }
	public LatLng getPosition() { return _mPos; }
	public boolean hasBanking() { return _bBanking; }
	public boolean hasBonus() { return _bBonus; }

	public boolean isOpened() { return _bOpened; }
	public int getAvailableBikes() { return _iAvBikes; }
	public int getAvailableBikeStands() { return _iAvBikeStands; }
	public Date getLastUpdate() { return _mLastUpdate; }
}
