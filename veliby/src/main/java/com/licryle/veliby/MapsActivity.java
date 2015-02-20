package com.licryle.veliby;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.licryle.veliby.BikeMap.BikeMap;
import com.licryle.veliby.BikeMap.BikeMapListener;
import com.licryle.veliby.BikeMap.Contract;
import com.licryle.veliby.BikeMap.Contracts;
import com.licryle.veliby.BikeMap.Station;
import com.licryle.veliby.BikeMap.Stations;
import com.licryle.veliby.UI.ExpandableDrawerAdapter;
import com.licryle.veliby.UI.ExpandableDrawerAdapter.ExpandNode;
import com.licryle.veliby.UI.OnSwipeListener;
import com.licryle.veliby.UI.SwipeTouchDectector;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;

public class MapsActivity extends ActionBarActivity implements BikeMapListener,
    android.view.View.OnClickListener, OnSwipeListener, OnChildClickListener, 
    OnGroupClickListener {
  protected static final String FAVSTATION_WIDGET_UPDATE =
      "com.licryle.veliby.favstationwidget.WIDGET_UPDATE";
  protected BikeMap _mBikeMap = null;
  protected View _mStationInfo = null;
  protected View _mStationInfoExt = null;
  protected View _mInfoView = null;

  protected DrawerLayout _mMenu = null;
  protected ExpandableListView _mMenuList = null;
  protected ExpandableDrawerAdapter _mMenuAdapter = null;
  protected ActionBarDrawerToggle _mMenuToggle = null;

  protected Station _mSelectedStation = null;

  protected Settings _mSettings = null;
  protected SwipeTouchDectector _mSwipeDetector;

  protected final static int MENU_MODES = 2;
  protected final static int MENU_HELP = 0;
  protected final static int MENU_CITY = 4;

  protected Tracker _mAnalytics = null;

  //****************************** Analytics *******************************//
  synchronized protected Tracker _getAnalyticsTracker() {
    if (_mAnalytics == null) {
      GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
      return analytics.newTracker(R.xml.analytics_tracker);
    }

    return _mAnalytics;
  }

  protected void _logAnalyticsEvent(String sAction,
                                    String sLabel, int iValue) {
    String sCity = "Unknown";

    if (_mBikeMap != null) {
      Contract mContract = _mBikeMap.getContracts().findContractById(
          _mSettings.getCurrentContractId());

      if (mContract != null) sCity = mContract.getCity();
    }

    _getAnalyticsTracker().send(new HitBuilders.EventBuilder()
        .setCategory(sCity)
        .setAction(sAction)
        .setLabel(sLabel)
        .setValue(iValue)
        .build());
  }

  protected void _logAnalyticsScreenView() {
    _getAnalyticsTracker().setScreenName(this.getLocalClassName());

    // Send a screen view.
    _getAnalyticsTracker().send(new HitBuilders.AppViewBuilder().build());
  }


  //************************* Activity Overrides ***************************//
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);

    _mSettings = Settings.getInstance(this);
    _mSwipeDetector = new SwipeTouchDectector(getApplicationContext(), 100, 100);
    _mSwipeDetector.addListener(this);

    // Start - Create Control Views
    _createMenu();

    _firstStart();

    _mStationInfo = findViewById(R.id.map_stationinfo);
    _mStationInfo.findViewById(R.id.infoview_favorite).setOnClickListener(this);
    _mStationInfo.findViewById(R.id.map_stationinfo_toggle).
        setOnClickListener(this);
    findViewById(R.id.map_stationinfo_main).setOnTouchListener(_mSwipeDetector);

    _mStationInfoExt = findViewById(R.id.map_stationinfo_extended);
    _mStationInfoExt.setOnTouchListener(_mSwipeDetector);
    _mStationInfoExt.findViewById(R.id.map_stationinfo_direction).
        setOnClickListener(this);

    _mInfoView = getLayoutInflater().inflate(R.layout.map_infoview, null);
    // End - Create Control Views

    File mAppDir = Settings.getVelibyPath();
    mAppDir.mkdirs();
 
    _mBikeMap = new BikeMap(this, _mSettings);
    _mBikeMap.registerBikeMapListener(this);

    ((SupportMapFragment) this.getSupportFragmentManager().
        findFragmentById(R.id.map)).getMapAsync(_mBikeMap);

    if (! Util.hasPlayServices(this)) {
      alertBox(getResources().getString(R.string.playservice_issue_title),
          getResources().getString(R.string.playservice_issue_content), null);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    _logAnalyticsScreenView();
    downloadMarkers(false);
  }

  @Override
  protected void onPostCreate(Bundle savedInstanceState) {
    super.onPostCreate(savedInstanceState);
    // Sync the toggle state after onRestoreInstanceState has occurred.
    _mMenuToggle.syncState();
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    _mMenuToggle.onConfigurationChanged(newConfig);
  }


  //************************ ActionBar Overrides ***************************//
	@Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu items for use in the action bar
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.maps, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Pass the event to ActionBarDrawerToggle, if it returns
    // true, then it has handled the app icon touch event
    if (_mMenuToggle.onOptionsItemSelected(item)) {
      return true;
    }

    // Handle presses on the action bar items
    switch (item.getItemId()) {
      case R.id.action_reload:
        downloadMarkers(true);
      return true;

      case R.id.action_mode:
      	boolean bModeFindBike = ! _mBikeMap.isFindBikeMode();

      	int iIcon;
      	int iToastStr;
      	if (bModeFindBike) {
      		iIcon = R.drawable.bikes;
      		iToastStr = R.string.action_mode_find;
      	} else {
      		iIcon = R.drawable.parking;
      		iToastStr = R.string.action_mode_return;
      	}

      	item.setIcon(getResources().getDrawable(iIcon));

      	hideStationInfo();
      	_mBikeMap.changeBikeMode(bModeFindBike);
      	lightMessage(iToastStr, false);
        _logAnalyticsEvent("BikeMode", bModeFindBike ? "Bikes" : "Docks", 0);
      return true;

      default:
        return super.onOptionsItemSelected(item);
	  }
  }

  //************************** LeftMenu Controls ***************************//
  protected void _createMenu() {
    _mMenu = (DrawerLayout) findViewById(R.id.drawer_layout);
    _mMenuList = (ExpandableListView) findViewById(R.id.mapsactivity_menu);

    _mMenuToggle = new ActionBarDrawerToggle(
        this, _mMenu, R.drawable.ic_drawer,
        R.string.menu_open, R.string.menu_close);
    // Set the drawer toggle as the DrawerListener
    _mMenu.setDrawerListener(_mMenuToggle);

    ActionBar actionBar = getSupportActionBar();
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);
 
    // Set the adapter for the list view
    _mMenuAdapter = new ExpandableDrawerAdapter(this);
    _mMenuList.setGroupIndicator(
        getResources().getDrawable(R.drawable.activity_maps_menu_group));
    
    _mMenuList.setAdapter(_mMenuAdapter);
    _mMenuList.setOnChildClickListener(this);
    _mMenuList.setOnGroupClickListener(this);

    // specific controls to this app
    _mMenuList.expandGroup(MENU_HELP); // Trick to get the correct state
    _mMenuList.expandGroup(MENU_MODES); // Modes menu always expanded
    _mMenuAdapter.getChild(MENU_MODES, _mSettings.isFavStationsOnly() ? 1 : 0).
        setSelected(true);
  }

  protected void _updateMenu(Contracts mContracts) {
    if (mContracts == null || mContracts.size() == 0) {
      ExpandNode[] mCities = new ExpandNode[1];
      mCities[0] = _mMenuAdapter.new ExpandNode("No city available",
          new ExpandNode[0], false, 0, null);

      _mMenuAdapter.getGroup(MENU_CITY).setChildren(mCities);
      _mMenuAdapter.notifyDataSetChanged();
      _mMenuList.expandGroup(MENU_CITY);
      _mMenuAdapter.getChild(MENU_CITY, 0).setSelected(false);

      _logAnalyticsEvent("Error", "NoCity", 0);
      return;
    }

    ExpandNode[] mCities = new ExpandNode[mContracts.size()];

    Iterator<Map.Entry<Integer, Contract>> it = mContracts.entrySet().
        iterator();

    int i = 0;
    while (it.hasNext()) {
      Map.Entry<Integer, Contract> entry = it.next();
      Contract mContract = entry.getValue();

      mCities[i] = _mMenuAdapter.new ExpandNode(mContract.getCity(),
          new ExpandNode[0], false, 0, mContract);
      i++;
    }

    Arrays.sort(mCities, new Comparator<ExpandNode>() {
      @Override
      public int compare(ExpandNode lhs, ExpandNode rhs) {
        return ((Contract) lhs.getLinkedObject()).getCity().
            compareToIgnoreCase(((Contract) rhs.getLinkedObject()).getCity());
      }
    });


    int iCurrContractId = _mSettings.getCurrentContractId();
    int iCurrContractPos = -1;
    for(int j = 0; j < mCities.length; j++) {
      if (((Contract) mCities[j].getLinkedObject()).getId() == iCurrContractId) {
        iCurrContractPos = j;
      }
    }

    _mMenuAdapter.getGroup(MENU_CITY).setChildren(mCities);

    if (iCurrContractPos >= 0) {
      _mMenuAdapter.getChild(MENU_CITY, iCurrContractPos).setSelected(true);
    }

    _mMenuAdapter.notifyDataSetChanged();
  }

  protected void _updateAppTitle() {
    Contract mContract = _mBikeMap.getContracts().findContractById(
        _mSettings.getCurrentContractId());

    String sAppName = getResources().getString(R.string.app_name);
    if (mContract != null) {
      setTitle(sAppName + " - " + mContract.getCity());
    } else {
      setTitle(sAppName);
    }
  }

  //********************** OnClickListener Interface ***********************//
  @Override
  public void onClick(View mView) {
    switch (mView.getId()) {
      case R.id.map_stationinfo_direction:
        _mBikeMap.displayDirections(_mSelectedStation.getPosition());
      break;
      
      case R.id.map_stationinfo_toggle:
        if (isExtendedStationInfoShown()) {
          hideExtendedStationInfo();
        } else {
          showExtendedStationInfo();
        }
      break;

      case R.id.infoview_favorite:
        boolean bNowFav = ! _mSettings.isStationFavorite(
            _mSelectedStation.getId());
        _mSettings.setStationFavorite(_mSelectedStation.getId(), bNowFav);
        updateFavoriteView();

        if (bNowFav) {
          lightMessage(R.string.action_faved, true);
          _mBikeMap.refreshStations();
          _logAnalyticsEvent("Favorite", "True", _mSelectedStation.getId());
        } else {
          lightMessage(R.string.action_unfaved, true);

          if (_mSettings.isFavStationsOnly() &&
              _mSettings.getFavStations().size() == 0) {
            hideStationInfo();
            _mSettings.setFavStationsOnly(false);
            _mMenuAdapter.notifyDataSetChanged();
            _mBikeMap.ShowAllStations();
            lightMessage(R.string.map_nofav, true);
          } else {
            _mBikeMap.refreshStations();
          }
          _logAnalyticsEvent("Favorite", "False", _mSelectedStation.getId());
        }

        updateFavStationWidget();
      break;
    }
  }

  //*************************** Misc. Controls *** *************************//
  public void exitApp() {
    Intent intent = new Intent(Intent.ACTION_MAIN);
    intent.addCategory(Intent.CATEGORY_HOME);
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startActivity(intent);
  }
 
  protected void _firstStart() {
    if (_mSettings.isFirstStart()) {
      showHelpDialog();

      _logAnalyticsEvent("FirstStart", null, 0);
      _mSettings.firstStart();
    }
  }

  protected void _inviteChooseCity() {
    _mMenuList.expandGroup(MENU_CITY);
    showMenu();

    lightMessage(R.string.welcome_choosecity, true);
  }

  //********************** Show / Hide control Views ***********************//
  public void showHelpDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    LayoutInflater inflater = getLayoutInflater();

    String sTitle = getResources().getString(R.string.welcome_title);
    try {
      PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
      sTitle = sTitle + " - " + pInfo.versionName;
    } catch (PackageManager.NameNotFoundException e) {
    }

    builder.setView(inflater.inflate(R.layout.dialog_help, null))
           .setPositiveButton(R.string.ok, null)
           .setTitle(sTitle);

    AlertDialog dialog = builder.create();

    if (_mSettings.isFirstStart() ) {
      dialog.setOnDismissListener(new OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
          _inviteChooseCity();
        }
      });
    }

    dialog.show();
    _logAnalyticsEvent("Show", "Help", 0);
  }
 
  protected void lightMessage(int iTextResource, boolean bLong) {
  	String sText = getResources().getString(iTextResource);
  	int iLength = bLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
  	Toast.makeText(getApplicationContext(), sText, iLength).show();
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
    hideStationInfo();
  }

  protected void showExtendedStationInfo() {
    /*if (_mStationInfoExt.getVisibility() == View.VISIBLE) return;
 
    Animation bottomUp = AnimationUtils.loadAnimation(getApplicationContext(),
        R.anim.bottom_up);

    _mStationInfoExt.startAnimation(bottomUp);*/

    ImageView mToggle = (ImageView) _mStationInfo.
        findViewById(R.id.map_stationinfo_toggle);

    mToggle.setImageDrawable(getResources().
        getDrawable(R.drawable.arrow_down_float));

    _mStationInfoExt.setVisibility(View.VISIBLE);
    _logAnalyticsEvent("Show", "Station_Info", _mSelectedStation.getId());
  }

  protected boolean isExtendedStationInfoShown() {
    return _mStationInfoExt.getVisibility() == View.VISIBLE;
  }

  protected void hideExtendedStationInfo() {
    /*if (_mStationInfoExt.getVisibility() == View.GONE) return;

    Animation bottomDown = AnimationUtils.loadAnimation(getApplicationContext(),
        R.anim.bottom_down);

    _mStationInfoExt.startAnimation(bottomDown);*/

    ImageView mToggle = (ImageView) _mStationInfo.
        findViewById(R.id.map_stationinfo_toggle);

    mToggle.setImageDrawable(getResources().
        getDrawable(R.drawable.arrow_up_float));

    _mStationInfoExt.setVisibility(View.GONE);
  }

  protected void showMenu() {
    _mMenu.openDrawer(Gravity.LEFT);
  }

  protected void hideMenu() {
    _mMenu.closeDrawers();
  }

  protected void downloadMarkers(boolean bManual) {
    Log.i("MapsActivity", "Entered downloadMarkers()");
    _logAnalyticsEvent("Update", bManual ? "Manual" : "Auto",
        _mBikeMap.isDownloading() ? 1 : 0);

    if (_mBikeMap.isDownloading()) { return; }

    if (!_mSettings.isSetCurrentContractId()
        && _mBikeMap.getContracts().size() > 0) {
      _inviteChooseCity();
    }

    Stations mStations = _mBikeMap.getStations();
    if (mStations == null ||
        mStations.isStaticExpired(Settings.getStaticDeadLine())) {
      lightMessage(R.string.action_reload_start, true);
      _mBikeMap.downloadMarkers();
    } else {
      if (bManual ||
          mStations.isDynamicExpired(Settings.getDynamicDeadLine())) {
        lightMessage(R.string.action_reload_start, true);
        _mBikeMap.downloadMarkers();
      }
    }
  }

  //********************** BikeMapListener Interface ***********************//
  @Override
  public void onDirectionsFailed(BikeMap mBikeMap) {
    _logAnalyticsEvent("Errors", "Directions",
        (_mSelectedStation == null) ? _mSelectedStation.getId() : 0);

    lightMessage(R.string.map_directionsfailed, false);
  }

  @Override
  public void onMapClick(BikeMap bikeMap, LatLng mLatLng) {
    hideStationInfo();
  }

  @Override
  public void onContractDownloadSuccess(BikeMap mBikeMap) {
    _updateMenu(mBikeMap.getContracts());
    _updateAppTitle();

    if (!_mSettings.isSetCurrentContractId()) _inviteChooseCity();
  }

  @Override
  public void onContractDownloadFailure(BikeMap mBikeMap) {
    onContractDownloadSuccess(mBikeMap);
    lightMessage(R.string.action_reload_contractfailure,
        true);
    _logAnalyticsEvent("Error", "Contracts_Update", 0);
  }

  @Override
  public void onStationsDownloadSuccess(BikeMap mBikeMap) {
    hideStationInfo();

    if (_mBikeMap.isMapLoaded()) {
      if (! mBikeMap.isAnyStationVisible()) {
        mBikeMap.moveCameraOnContract();
      }

      lightMessage(R.string.action_reload_complete, false);
      _logAnalyticsEvent("Success", "Contracts_Update", 0);
    } else {
      lightMessage(R.string.action_reload_nomap, true);
      _logAnalyticsEvent("Error", "Map_NotReady", 0);
    }
  }

  @Override
  public void onStationsDownloadFailure(BikeMap mBikeMap) {
    if (! mBikeMap.isAnyStationVisible()) {
      mBikeMap.moveCameraOnContract();
    }

    lightMessage(R.string.action_reload_failure, true);
    _logAnalyticsEvent("Error", "Stations_Update", 0);
  }

  @Override
  public void onStationClick(BikeMap mBikeMap, Station mStation) {
    _mSelectedStation = mStation;

    // Distance
    LatLng mLastKnownPos = Util.getLastPosition(getApplicationContext());
    TextView mDistance = (TextView) _mStationInfo.findViewById(
        R.id.map_stationinfo_distance);
    if (mLastKnownPos == null) {
      mDistance.setText(getResources().getText(R.string.map_nodistance));
    } else {
      float[] aDistance = new float[3];
      Location.distanceBetween(mLastKnownPos.latitude,
                               mLastKnownPos.longitude,
                               mStation.getPosition().latitude,
                               mStation.getPosition().longitude,
                               aDistance);
      mDistance.setText(String.valueOf((int)(aDistance[0])) + " " +
          getResources().getText(R.string.map_meters));
    }

    // Address
    TextView mAddress = (TextView) _mStationInfo.
        findViewById(R.id.map_stationinfo_address);
    mAddress.setText(mStation.getAddress().toLowerCase());    
    
    showStationInfo();

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
      _mSelectedStation = null;
      return;
    }

    _mStationInfo.findViewById(R.id.infoview_stationclosed).
        setVisibility(View.GONE);
    _mStationInfo.findViewById(R.id.infoview_row_bike).
        setVisibility(View.VISIBLE);

    // isFavorite
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
  }

  //*************************** InfoView Controls **************************//
  @Override
  public View onGetInfoContents(Marker mMarker, Station mStation) {
    // Take
    TextView mTitle = (TextView) _mInfoView.findViewById(R.id.infoview_title);
    mTitle.setText(mStation.getFriendlyName());
    return _mInfoView;
  }

  protected void updateFavoriteView() {
    ImageView mFavImg = (ImageView) _mStationInfo.findViewById(
        R.id.infoview_favorite);
    int iIcon = R.drawable.rate_star_big_off_holo_dark;

    if (_mSettings.isStationFavorite(_mSelectedStation.getId())) {
      iIcon = R.drawable.rate_star_big_on_holo_dark;
    }
    mFavImg.setImageResource(iIcon);
  }

  protected void updateFavStationWidget() {
    Intent intent = new Intent(FAVSTATION_WIDGET_UPDATE);
    getApplicationContext().sendBroadcast(intent);
  }

  //************************* Swipe Touch Interface ************************//
  @Override
  public void onSwipeRight(View mOrigin) {    
  }

  @Override
  public void onSwipeLeft(View mOrigin) {
    if (_mMenu.findViewById(mOrigin.getId()) != null) {
      hideMenu();
    }
  }

  @Override
  public void onSwipeUp(View mOrigin) {
    if (_mStationInfo.findViewById(mOrigin.getId()) != null) {
      showExtendedStationInfo();
    }
  }

  @Override
  public void onSwipeDown(View mOrigin) {
    if (_mStationInfo.findViewById(mOrigin.getId()) != null) {
      hideExtendedStationInfo();
    }
  }

  //*********************** Expandable Drawer Clicks ***********************//
  @Override
  public boolean onChildClick(ExpandableListView parent, View v,
      int groupPosition, int childPosition, long id) {

    ExpandNode mGroup = _mMenuAdapter.getGroup(groupPosition);
    ExpandNode mChild = _mMenuAdapter.getChild(groupPosition, childPosition);

    if (groupPosition == MENU_MODES) {
      switch (childPosition) {
        case 0:
          if (_mSettings.isFavStationsOnly()) {
            _mSettings.setFavStationsOnly(false);
            hideStationInfo();
            _mBikeMap.ShowAllStations();
            _logAnalyticsEvent("Mode", "All_Stations", 0);
          }
          hideMenu();
        break;
   
        case 1:
          if (! _mSettings.isFavStationsOnly()) {
            if (_mSettings.getFavStations().size() == 0) {
              lightMessage(R.string.map_nofav, true);
              hideMenu();
              _mMenuAdapter.notifyDataSetChanged();
              return false;
            } else {
              _mSettings.setFavStationsOnly(true);
              hideStationInfo();
              _mBikeMap.ShowFavStations();
              _logAnalyticsEvent("Mode", "Favorite_Stations", 0);
            }
          }
          hideMenu();
        break;
      }
    } else if (groupPosition == MENU_CITY) {
      Contract mContract = (Contract) _mMenuAdapter.
          getChild(MENU_CITY, childPosition).getLinkedObject();

      if (mContract == null) {
        lightMessage(R.string.menu_no_city, true);
      } else {
        if (mContract.getId() != _mSettings.getCurrentContractId()) {
          _logAnalyticsEvent("ChangeCity", mContract.getCity(), 0);
          _mSettings.setCurrentContract(mContract);
          _updateAppTitle();
          _mBikeMap.moveCameraTo(mContract.getPosition(), 13);
          lightMessage(R.string.action_reload_start, true);
          _mBikeMap.downloadMarkers();
        } else {
          _mBikeMap.moveCameraTo(_mBikeMap.getContracts().findContractById(
              _mSettings.getCurrentContractId()).getPosition(), 13);
        }

        hideMenu();
      }
    }
 
    switch (mGroup.getSelectedMode()) {
      case 0:
      break;

      case 1:
        for (ExpandNode miChild : mGroup.Children()) {
          miChild.setSelected(false);
        }

        mChild.setSelected(_mBikeMap.getContracts().size() > 0);
      break;
     
      case 2:
        mChild.setSelected(! mChild.isSelected());
      break;
    }


    _mMenuAdapter.notifyDataSetChanged();
    return false;
  }

  @Override
  public boolean onGroupClick(ExpandableListView parent, View v,
      int groupPosition, long id) {
    switch (groupPosition) {
      //case MENU_MODES: return true;
      case MENU_HELP: 
        showHelpDialog();
        hideMenu();
        return true;
    }

    return false;
  }
}
