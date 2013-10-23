package com.licryle.veliby;

import java.io.File;
import java.sql.Date;
import java.util.Calendar;
import java.util.Hashtable;

import android.app.AlertDialog;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.licryle.veliby.BikeMap.BikeMap;
import com.licryle.veliby.BikeMap.BikeMapListener;
import com.licryle.veliby.BikeMap.Station;

public class MapsActivity extends ActionBarActivity implements BikeMapListener,
    android.view.View.OnClickListener {
  protected BikeMap _mBikeMap = null;
  protected View _mStationInfo = null;
  protected View _mInfoView = null;
  protected int _iStationShownId;
  

  protected String _sFileStations = null;
  protected File _mStationsDataFile;
  protected SharedPreferences _mSettings;
  protected int _iStaticDeadline = 7;
  public final String URL_FULL = "https://api.jcdecaux.com/vls/v1/stations?" +
  		"contract=Paris&apiKey=718b4e0e0b1f01af842ff54c38bed00eaa63ce3c";
  public final String URL_DYNAMIC = "http://veliby.berliat.fr/?c=1";
  public final static Hashtable<Integer, Integer> mBikeResources = 
      new Hashtable<Integer, Integer>() {
        private static final long serialVersionUID = -6956564905991202734L;
        {
          put(0,R.color.infoview_nobike);
          put(2,R.color.infoview_fewbikes);
          put(4,R.color.infoview_somebikes);
          put(1000,R.color.infoview_plentybikes);
        }
      };

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);
    _mStationInfo = findViewById(R.id.map_infoview);
    _mStationInfo.findViewById(R.id.infoview_favorite).setOnClickListener(this);
    _mInfoView = getLayoutInflater().inflate(R.layout.map_infoview, null);
    _mSettings = getPreferences(MODE_PRIVATE);

    firstStart();

    File mAppDir = new File(
        Environment.getExternalStorageDirectory().getPath() + "/Veliby/");
    mAppDir.mkdirs();
    _mStationsDataFile = new File(mAppDir.getAbsolutePath() +
        "/stations.comlete");

    GoogleMap mMap = ((SupportMapFragment) this.getSupportFragmentManager().
        findFragmentById(R.id.map)).getMap();
 
    _mBikeMap = new BikeMap(this, _mStationsDataFile, mMap);
    _mBikeMap.registerBikeMapListener(this);
    
    if (! Util.hasPlayServices(this)) {
      alertBox(getResources().getString(R.string.playservice_issue_title),
          getResources().getString(R.string.playservice_issue_content), null);
    }
  }


	@Override
  public boolean onCreateOptionsMenu(Menu menu) {
      // Inflate the menu items for use in the action bar
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.maps, menu);
      return super.onCreateOptionsMenu(menu);
  }

	protected void downloadMarkers() {
	  if (_mBikeMap.isDownloading()) { return; }

    lightMessage(R.string.action_reload_start, true);
 
    if (isStaticDataExpired()) {
      _mBikeMap.downloadMarkers(true, URL_FULL);
    } else {
      _mBikeMap.downloadMarkers(false, URL_DYNAMIC);
    }
	}

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle presses on the action bar items
    switch (item.getItemId()) {
    case R.id.action_reload:
      downloadMarkers();
    return true;

    case R.id.action_help:
      showHelpDialog();
    return true;

      case R.id.action_mode:
      	boolean bModeFindBike = ! _mBikeMap.isFindBikeMode();

      	int iIcon;
      	int iToastStr;
      	if (bModeFindBike) {
      		iIcon = R.drawable.ic_menu_upload;
      		iToastStr = R.string.action_mode_find;
      	} else {
      		iIcon = R.drawable.ic_menu_goto;
      		iToastStr = R.string.action_mode_return;
      	}

      	item.setIcon(getResources().getDrawable(iIcon));

      	hideStationInfo();
      	_mBikeMap.changeBikeMode(bModeFindBike);
      	lightMessage(iToastStr, false);
      return true;

      default:
      return super.onOptionsItemSelected(item);
	  }
  }

  @Override
  public void onResume() {
  	super.onResume();
  	downloadMarkers();
  }

  public void exitApp() {
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_HOME);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }

  public void showHelpDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    LayoutInflater inflater = getLayoutInflater();

    builder.setView(inflater.inflate(R.layout.dialog_help, null))
           .setPositiveButton(R.string.ok, null)
           .setTitle(R.string.welcome_title);

    AlertDialog dialog = builder.create();
    dialog.show();
  }
 
  protected void lightMessage(int iTextResource, boolean bLong) {
  	String sText = getResources().getString(iTextResource);
  	int iLength = bLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
  	Toast.makeText(getApplicationContext(), sText, iLength).show();  	
  }
 
  protected void firstStart() {
  	if (! _mSettings.getBoolean("previously_started", false)) {
  	  showHelpDialog();

  		_mSettings.edit().putBoolean("previously_started", true).commit();
  	}
  }

  private boolean isStaticDataExpired() {
		if (!_mStationsDataFile.exists()) {
			return true;
		}

	  Date mLastModified = new Date(_mStationsDataFile.lastModified());
	  Calendar mDeadline = Calendar.getInstance();
	  mDeadline.add(Calendar.DATE, -_iStaticDeadline);

	  return mLastModified.before(mDeadline.getTime());
  }

	protected void alertBox(String title, String mymessage,
  		OnClickListener onclick) {
    new AlertDialog.Builder(this)
    			.setMessage(mymessage)
    			.setTitle(title)
    			.setNeutralButton(R.string.ok, onclick)
    			.show();
  }

  protected void showStationInfo() {
    if (_mStationInfo.getVisibility() == View.VISIBLE) return;
 
    Animation bottomUp = AnimationUtils.loadAnimation(getApplicationContext(),
        R.anim.bottom_up);

    _mStationInfo.startAnimation(bottomUp);
    _mStationInfo.setVisibility(View.VISIBLE);
  }

  protected void hideStationInfo() {
    if (_mStationInfo.getVisibility() == View.GONE) return;

    Animation bottomDown = AnimationUtils.loadAnimation(getApplicationContext(),
        R.anim.bottom_down);

    _mStationInfo.startAnimation(bottomDown);
    _mStationInfo.setVisibility(View.GONE);
  }

  protected boolean isStationFavorite(int iStationId) {
    return _mSettings.getBoolean("favstation_" + iStationId, false);
  }

  protected void setStationFavorite(int iStationId, boolean bFavorite) {
    String sKey = "favstation_" + iStationId;

    if (!bFavorite) {
      _mSettings.edit().remove(sKey).commit();
    } else {
      _mSettings.edit().putBoolean(sKey, true).commit();
    }
  }

  @Override
  public void onDownloadFailure(BikeMap mBikeMap) {
    lightMessage(R.string.action_reload_failure, true);    
  }

  @Override
  public void onDownloadSuccess(BikeMap mBikeMap) {
    hideStationInfo();
    lightMessage(R.string.action_reload_complete, false);
  }

  @Override
  public void onMapClick(BikeMap bikeMap, LatLng mLatLng) {
    hideStationInfo();
  }

  @Override
  public void onStationClick(BikeMap mBikeMap, Station mStation) {
    if (mStation.getLastUpdate() == null) {
      _mStationInfo.findViewById(R.id.infoview_noinfo).
          setVisibility(View.VISIBLE);

      _mStationInfo.findViewById(R.id.infoview_stationclosed).
          setVisibility(View.GONE);
      _mStationInfo.findViewById(R.id.infoview_row_bike).
          setVisibility(View.GONE);

      showStationInfo();
      return;
    }

    _mStationInfo.findViewById(R.id.infoview_noinfo).
        setVisibility(View.GONE);

    if (!mStation.isOpened()) {
      _mStationInfo.findViewById(R.id.infoview_stationclosed).
          setVisibility(View.VISIBLE);
      _mStationInfo.findViewById(R.id.infoview_row_bike).
          setVisibility(View.GONE);

      showStationInfo();
      return;
    }

    _mStationInfo.findViewById(R.id.infoview_stationclosed).
        setVisibility(View.GONE);
    _mStationInfo.findViewById(R.id.infoview_row_bike).
        setVisibility(View.VISIBLE);

    // isFavorite
    _iStationShownId = mStation.getId();
    updateFavoriteView();

    // Take
    int iNbBikes = mStation.isOpened() ? mStation.getAvailableBikes() : 0;

    TextView mBikes = (TextView) _mStationInfo.findViewById(R.id.infoview_bikes);
    mBikes.setText("" + iNbBikes);

    int color = Util.resolveResourceFromNumber(mBikeResources, iNbBikes);
    mBikes.setTextColor(getResources().getColor(color));

    // Return
    iNbBikes = mStation.isOpened() ? mStation.getAvailableBikeStands() : 0;

    mBikes = (TextView) _mStationInfo.findViewById(R.id.infoview_stands);
    mBikes.setText("" + iNbBikes);

    color = Util.resolveResourceFromNumber(mBikeResources, iNbBikes);
    mBikes.setTextColor(getResources().getColor(color));

    showStationInfo();
  }

  protected void updateFavoriteView() {
    ImageView mFavImg = (ImageView) _mStationInfo.findViewById(
        R.id.infoview_favorite);
    int iIcon = R.drawable.rate_star_big_off_holo_dark;

    if (isStationFavorite(_iStationShownId)) {
      iIcon = R.drawable.rate_star_big_on_holo_dark;
    }
    mFavImg.setImageResource(iIcon);
  }


  @Override
  public void onClick(View mView) {
    switch (mView.getId()) {
      case R.id.infoview_favorite:
        boolean bNowFav = !isStationFavorite(_iStationShownId);
        setStationFavorite(_iStationShownId, bNowFav);
        updateFavoriteView();

        if (bNowFav) {
          lightMessage(R.string.action_faved, true);
        } else {
          lightMessage(R.string.action_unfaved, true);
        }
      break;
    }
  }


  @Override
  public View onGetInfoContents(Marker mMarker, Station mStation) {
    // Take
    String sTitle = mStation.getName();
    sTitle = sTitle.replaceAll("[0-9]+ - (.*)", "$1").toLowerCase();
    sTitle = Character.toUpperCase(sTitle.charAt(0)) + sTitle.substring(1);

    TextView mTitle = (TextView) _mInfoView.findViewById(R.id.infoview_title);
    mTitle.setText(sTitle);

    // Banking?
    int iVisibility = mStation.hasBanking() ? View.VISIBLE : View.GONE;
    _mInfoView.findViewById(R.id.infoview_bankimg).
        setVisibility(iVisibility);

    // Bonus ?
    iVisibility = mStation.hasBonus() ? View.VISIBLE : View.GONE;
    _mInfoView.findViewById(R.id.infoview_bonusimg).
        setVisibility(iVisibility);

    return _mInfoView;
  }
}
