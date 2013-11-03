package com.licryle.veliby;

import java.io.File;

import android.app.AlertDialog;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
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
import com.licryle.veliby.BikeMap.Stations;

public class MapsActivity extends ActionBarActivity implements BikeMapListener,
    android.view.View.OnClickListener {
  protected static final String FAVSTATION_WIDGET_UPDATE =
      "com.licryle.veliby.favstationwidget.WIDGET_UPDATE";
  protected BikeMap _mBikeMap = null;
  protected View _mStationInfo = null;
  protected View _mInfoView = null;
  protected int _iStationShownId;
  

  protected String _sFileStations = null;
  protected File _mStationsDataFile;
  protected Settings _mSettings;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);
    _mStationInfo = findViewById(R.id.map_infoview);
    _mStationInfo.findViewById(R.id.infoview_favorite).setOnClickListener(this);
    _mInfoView = getLayoutInflater().inflate(R.layout.map_infoview, null);
    _mSettings = Settings.getInstance(this);

    firstStart();

    File mAppDir = _mSettings.getVelibyPath();
    mAppDir.mkdirs();
    _mStationsDataFile = _mSettings.getStationsFile();

    GoogleMap mMap = ((SupportMapFragment) this.getSupportFragmentManager().
        findFragmentById(R.id.map)).getMap();
 
    _mBikeMap = new BikeMap(this, _mStationsDataFile, mMap,
        _mSettings.getDynamicDeadLine());
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

	protected void downloadMarkers(boolean bManual) {
	  if (_mBikeMap.isDownloading()) { return; }
 
    Stations mStations = _mBikeMap.stations();
    if (mStations == null ||
        mStations.isStaticExpired(_mSettings.getStaticDeadLine())) {
      lightMessage(R.string.action_reload_start, true);
      _mBikeMap.downloadMarkers(true, _mSettings.getURLDownloadFull());
    } else {
      if (bManual ||
          mStations.isDynamicExpired(_mSettings.getDynamicDeadLine())) {
        lightMessage(R.string.action_reload_start, true);
        _mBikeMap.downloadMarkers(false, _mSettings.getURLDownloadDynamic());
      }
    }
	}

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle presses on the action bar items
    switch (item.getItemId()) {
    case R.id.action_reload:
      downloadMarkers(true);
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
  	downloadMarkers(false);
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
  	if (_mSettings.isFirstStart()) {
  	  showHelpDialog();

  		_mSettings.firstStart();
  	}
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
    if (mStation.isStaticOnly()) {
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

    int color = Util.resolveResourceFromNumber(_mSettings.getBikeColors(),
        iNbBikes);
    mBikes.setTextColor(getResources().getColor(color));

    // Return
    iNbBikes = mStation.isOpened() ? mStation.getAvailableBikeStands() : 0;

    mBikes = (TextView) _mStationInfo.findViewById(R.id.infoview_stands);
    mBikes.setText("" + iNbBikes);

    color = Util.resolveResourceFromNumber(_mSettings.getBikeColors(),
        iNbBikes);
    mBikes.setTextColor(getResources().getColor(color));

    showStationInfo();
  }

  protected void updateFavoriteView() {
    ImageView mFavImg = (ImageView) _mStationInfo.findViewById(
        R.id.infoview_favorite);
    int iIcon = R.drawable.rate_star_big_off_holo_dark;

    if (_mSettings.isStationFavorite(_iStationShownId)) {
      iIcon = R.drawable.rate_star_big_on_holo_dark;
    }
    mFavImg.setImageResource(iIcon);
  }

  protected void updateFavStationWidget() {
    Intent intent = new Intent(FAVSTATION_WIDGET_UPDATE);
    getApplicationContext().sendBroadcast(intent);
  }


  @Override
  public void onClick(View mView) {
    switch (mView.getId()) {
      case R.id.infoview_favorite:
        boolean bNowFav = ! _mSettings.isStationFavorite(_iStationShownId);
        _mSettings.setStationFavorite(_iStationShownId, bNowFav);
        updateFavoriteView();

        if (bNowFav) {
          lightMessage(R.string.action_faved, true);
        } else {
          lightMessage(R.string.action_unfaved, true);
        }

        updateFavStationWidget();
      break;
    }
  }

  @Override
  public View onGetInfoContents(Marker mMarker, Station mStation) {
    // Take
    TextView mTitle = (TextView) _mInfoView.findViewById(R.id.infoview_title);
    mTitle.setText(mStation.getFriendlyName());

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
