package com.licryle.veliby;

import java.io.File;

import org.apache.http.MethodNotSupportedException;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
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
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
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
import com.licryle.veliby.BikeMap.Station.Contract;
import com.licryle.veliby.BikeMap.Stations;
import com.licryle.veliby.UI.ExpandableDrawerAdapter;
import com.licryle.veliby.UI.ExpandableDrawerAdapter.ExpandNode;
import com.licryle.veliby.UI.OnSwipeListener;
import com.licryle.veliby.UI.SwipeTouchDectector;

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

  protected String _sFileStations = null;
  protected Settings _mSettings = null;
  protected SwipeTouchDectector _mSwipeDetector;

  protected final static int MENU_MODES = 2;
  protected final static int MENU_HELP = 0;
  protected final static int MENU_CITY = 4;

  //************************* Activity Overrides ***************************//
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_maps);

    _mSettings = Settings.getInstance(this);
    _mSwipeDetector = new SwipeTouchDectector(getApplicationContext(), 100, 100);
    _mSwipeDetector.addListener(this);
    _updateAppTitle();

    // Start - Create Control Views
    createMenu();

    firstStart();

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

    File mAppDir = _mSettings.getVelibyPath();
    mAppDir.mkdirs();

    GoogleMap mMap = ((SupportMapFragment) this.getSupportFragmentManager().
        findFragmentById(R.id.map)).getMap();
 
    _mBikeMap = new BikeMap(this, mMap, _mSettings);
    _mBikeMap.registerBikeMapListener(this);
    
    if (! Util.hasPlayServices(this)) {
      alertBox(getResources().getString(R.string.playservice_issue_title),
          getResources().getString(R.string.playservice_issue_content), null);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
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

  //************************** LeftMenu Controls ***************************//
  public void createMenu() {
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
    _mMenuAdapter.getChild(MENU_CITY, 
        _mSettings.getCurrentContract().getId() - 1).setSelected(true);
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
        } else {
          lightMessage(R.string.action_unfaved, true);

          if (_mSettings.isFavStationsOnly() &&
              _mSettings.getFavStations().size() == 0) {
            hideStationInfo();
            _mSettings.setFavStationsOnly(false);
            _mMenuAdapter.notifyDataSetChanged();
            _mBikeMap.ShowAllStations();
            lightMessage(R.string.map_nofav, true);
          }
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
 
  protected void firstStart() {
    if (_mSettings.isFirstStart()) {
      showHelpDialog();

      _mSettings.firstStart();
    }
  }

  //********************** Show / Hide control Views ***********************//
  public void showHelpDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    LayoutInflater inflater = getLayoutInflater();

    builder.setView(inflater.inflate(R.layout.dialog_help, null))
           .setPositiveButton(R.string.ok, null)
           .setTitle(R.string.welcome_title);

    AlertDialog dialog = builder.create();

    if (_mSettings.isFirstStart() ) {
      dialog.setOnDismissListener(new OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
          _mMenuList.expandGroup(MENU_CITY);
          showMenu();
  
          lightMessage(R.string.welcome_choosecity, true);
        }
      });
    }

    dialog.show();
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
    if (_mBikeMap.isDownloading()) { return; }
 
    Stations mStations = _mBikeMap.stations();
    if (mStations == null ||
        mStations.isStaticExpired(_mSettings.getStaticDeadLine())) {
      lightMessage(R.string.action_reload_start, true);
      _mBikeMap.downloadMarkers();
    } else {
      if (bManual ||
          mStations.isDynamicExpired(_mSettings.getDynamicDeadLine())) {
        lightMessage(R.string.action_reload_start, true);
        _mBikeMap.downloadMarkers();
      }
    }
  }

  //********************** BikeMapListener Interface ***********************//
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
  public void onDirectionsFailed(BikeMap mBikeMap) {
    lightMessage(R.string.map_directionsfailed, false);
  }

  @Override
  public void onMapClick(BikeMap bikeMap, LatLng mLatLng) {
    hideStationInfo();
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
      mDistance.setText((int)(aDistance[0]) + " mètres");
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
            }
          }
          hideMenu();
        break;
      }
    } else if (groupPosition == MENU_CITY) {
      int iNewContract = childPosition + 1;
      if (iNewContract != _mSettings.getCurrentContract().getId()) {
        Contract mContract = Contract.findContractById(childPosition + 1);
        _mSettings.setCurrentContract(mContract);
        _updateAppTitle();
        _mBikeMap.moveCameraTo(mContract.getPosition(), 13);
        lightMessage(R.string.action_reload_start, true);
        _mBikeMap.downloadMarkers();
      } else {
        _mBikeMap.moveCameraTo(
            _mSettings.getCurrentContract().getPosition(),13);
      }

      hideMenu();
    }
 
    switch (mGroup.getSelectedMode()) {
      case 0:
      break;

      case 1:
        for (ExpandNode miChild : mGroup.Children()) {
          miChild.setSelected(false);
        }
        mChild.setSelected(true);
      break;
     
      case 2:
        mChild.setSelected(! mChild.isSelected());
      break;
    }


    _mMenuAdapter.notifyDataSetChanged();
    return false;
  }

  private void _updateAppTitle() {
    Contract mContract = _mSettings.getCurrentContract();
    String sAppName = getResources().getString(R.string.app_name);
    setTitle(sAppName + " - " + mContract.getName());
  }

  @Override
  public boolean onGroupClick(ExpandableListView parent, View v,
      int groupPosition, long id) {
    switch (groupPosition) {
      //case MENU_MODES: return true;
      case MENU_HELP: 
        showHelpDialog();
        hideMenu();
      break;
    }

    return false;
  }
}
