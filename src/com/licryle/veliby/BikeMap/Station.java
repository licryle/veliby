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
    PARIS       (1, "Paris",                48.85568,   2.346246),
    ROUEN       (2, "Rouen",                49.443232,  1.099971),
    TOULOUSE    (3, "Toulouse",             43.604652,  1.444209),
    LUXEMBOURG  (4, "Luxembourg",           49.611621,  6.1319346),
    VALENCE     (5, "Valence",              39.4699075, -0.3762881),
    STOCKHOLM   (6, "Stockholm",            59.32893,   18.06491),
    GOTEBORG    (7, "Goteborg",             57.70887,   11.97456),
    SANTANDER   (8, "Santander",            43.4623057, -3.8099803),
    AMIENS      (9, "Amiens",               49.894067,  2.295753),
    LILLESTROM  (10, "Lillestrom",          59.9559696, 11.0503785),
    MULHOUSE    (11, "Mulhouse",            47.750839,  7.335888),
    LYON        (12, "Lyon",                45.764043,  4.835659),
    LJUBLJANA   (13, "Ljubljana",           46.0564509, 14.5080702),
    SEVILLE     (14, "Seville",             37.3880961, -5.9823299),
    NAMUR       (15, "Namur",               50.4673883, 4.8719854),
    NANCY       (16, "Nancy",               48.692054,  6.184417),
    CRETEIL     (17, "Creteil",             48.790367,  2.455572),
    BRUXELLES   (18, "Bruxelles-Capitale",  50.8503463, 4.3517211),
    CERGY       (19, "Cergy-Pontoise",      49.038946,  2.075368),
    VILNIUS     (20, "Vilnius",             54.6871555, 25.2796514),
    TOYANA      (21, "Toyama",              36.6959518, 137.2136768),
    KAZAN       (22, "Kazan",               55.8005556, 49.1055556),
    MARSEILLE   (23, "Marseille",           43.296482,  5.36978),
    NANTES      (24, "Nantes",              47.218371, -1.553621),
    BESANCON    (25, "Besancon",            47.237829,  6.0240539);

		private final int _iId;
		private final String _sName;
		private final LatLng _Position;

		Contract(int iId, String sName, double dLat, double dLng) {
			this._iId = iId;
			this._sName = sName;
			this._Position = new LatLng(dLat, dLng);
		}

		public int getId() { return _iId; }
    public String getName() { return _sName; }
    public LatLng getPosition() { return _Position; }

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
