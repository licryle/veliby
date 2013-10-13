package com.licryle.veliby;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.sql.Date;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.licryle.veliby.BikeMap.BikeMap;
import com.licryle.veliby.BikeMap.BikeMapListener;
import com.licryle.veliby.BikeMap.DownloadStationsService;
import com.licryle.veliby.BikeMap.Station;
import com.licryle.veliby.UI.Maps_InfoWindowAdapter;

public class MapsActivity extends ActionBarActivity implements BikeMapListener {
  protected BikeMap mBikeMap = null;

  protected String sFileStations = null;
  protected File mStationsDataFile;
  protected int iStaticDeadline = 7;
  protected String sUrlFull = "https://api.jcdecaux.com/vls/v1/stations?" +
  		"contract=Paris&apiKey=718b4e0e0b1f01af842ff54c38bed00eaa63ce3c";
  protected String sUrlDynamic = "http://veliby.berliat.fr/?c=1";


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_maps);


    File mAppDir = new File(
        Environment.getExternalStorageDirectory().getPath() + "/Veliby/");

    mAppDir.mkdirs();
    mStationsDataFile = new File(mAppDir.getAbsolutePath() +
        "/stations.comlete");

    firstStart();

    GoogleMap mMap = ((SupportMapFragment) this.getSupportFragmentManager().
        findFragmentById(R.id.map)).getMap();
 
    mBikeMap = new BikeMap(this, mStationsDataFile, mMap);
    mBikeMap.registerBikeMapListener(this);
    
    if (! Util.hasPlayServices(this)) {
      alertBox(getResources().getString(R.string.playservice_issue_title),
          getResources().getString(R.string.playservice_issue_content), null);
    } else {
      downloadMarkers();
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
	  if (mBikeMap.isDownloading()) { return; }

    lightMessage(R.string.action_reload_start, true);
 
    if (isStaticDataExpired()) {
      mBikeMap.downloadMarkers(true, sUrlFull);
    } else {
      mBikeMap.downloadMarkers(false, sUrlDynamic);
    }
	}

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle presses on the action bar items
    switch (item.getItemId()) {
      case R.id.action_reload:
      	downloadMarkers();
      return true;

      case R.id.action_mode:
      	boolean bModeFindBike = ! mBikeMap.isFindBikeMode();

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

      	mBikeMap.changeBikeMode(bModeFindBike);
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
 
  protected void lightMessage(int iTextResource, boolean bLong) {
  	String sText = getResources().getString(iTextResource);
  	int iLength = bLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT;
  	Toast.makeText(getApplicationContext(), sText, iLength).show();  	
  }
 
  protected void firstStart() {
  	SharedPreferences mSettings = getPreferences(MODE_PRIVATE);

  	if (! mSettings.getBoolean("previously_started", false)) {
  		AlertDialog.Builder builder = new AlertDialog.Builder(this);
  		LayoutInflater inflater = getLayoutInflater();

  		builder.setView(inflater.inflate(R.layout.welcome_dialog, null))
  					 .setPositiveButton(R.string.ok, null)
  		       .setTitle(R.string.welcome_title);

  		AlertDialog dialog = builder.create();
  		dialog.show();

  		mSettings.edit().putBoolean("previously_started", true).commit();
  	}
  }

  private boolean isStaticDataExpired() {
		if (!mStationsDataFile.exists()) {
			return true;
		}

	  Date mLastModified = new Date(mStationsDataFile.lastModified());
	  Calendar mDeadline = Calendar.getInstance();
	  mDeadline.add(Calendar.DATE, -iStaticDeadline);

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

  public void exitApp() {
  	Intent intent = new Intent(Intent.ACTION_MAIN);
  	intent.addCategory(Intent.CATEGORY_HOME);
  	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  	startActivity(intent);
  }


  @Override
  public void OnDownloadFailure(BikeMap mBikeMap) {
    lightMessage(R.string.action_reload_failure, true);    
  }


  @Override
  public void OnDownloadSuccess(BikeMap mBikeMap) {
    lightMessage(R.string.action_reload_complete, false);
  }
}
