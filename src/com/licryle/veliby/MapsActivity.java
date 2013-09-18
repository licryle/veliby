package com.licryle.veliby;

import java.io.File;
import java.text.DateFormat;
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
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.licryle.veliby.UI.Maps_InfoWindowAdapter;

public class MapsActivity extends ActionBarActivity {
	private GoogleMap mMap;

  protected String sFileStations = null;
  protected Hashtable<Integer, Station> mStations;
  protected boolean	bModeFindBike = true;
  protected boolean	bDownloading = false;

	protected static Hashtable<Integer, Integer> mBikeResources = 
			new Hashtable<Integer, Integer>() {{
				put(0,R.drawable.presence_invisible);
				put(2,R.drawable.presence_busy);
				put(4,R.drawable.presence_away);
				put(1000,R.drawable.presence_online);
			}}; 


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_maps);

    mStations = new Hashtable<Integer, Station>();
    // ToDo: load static info from file

    setupMap();

    downloadMarkers();

    firstStart();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
      // Inflate the menu items for use in the action bar
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.maps, menu);
      return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle presses on the action bar items
    switch (item.getItemId()) {
      case R.id.action_reload:
      	downloadMarkers();
      return true;

      case R.id.action_mode:
      	bModeFindBike = ! bModeFindBike;

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
      	lightMessage(iToastStr);

        updateMarkers(bModeFindBike);
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
 
  protected void lightMessage(int iTextResource) {
  	String sText = getResources().getString(iTextResource);
  	Toast.makeText(getApplicationContext(), sText, Toast.LENGTH_SHORT).show();  	
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

  protected void downloadMarkers() {
  	if (bDownloading) return;
  	bDownloading = true;

  	lightMessage(R.string.action_reload_start);

  	Intent intent = new Intent(this, DownloadService.class);

  	File appDir = new File(
  			Environment.getExternalStorageDirectory().getPath() + "/Veliby/");
  	// have the object build the directory structure, if needed.
  	appDir.mkdirs();
  	sFileStations = appDir.getAbsolutePath() +
				"/com.licryle.veliby.stations.status";

  	intent.putExtra("url", "https://api.jcdecaux.com/vls/v1/stations?" +
  			"contract=Paris&apiKey=718b4e0e0b1f01af842ff54c38bed00eaa63ce3c");
  	intent.putExtra("tempFile", sFileStations);
  	intent.putExtra("receiver", new DownloadReceiver(new Handler()));
  	startService(intent);
  }

  protected void alertBox(String title, String mymessage,
  		OnClickListener onclick) {
    new AlertDialog.Builder(this)
    			.setMessage(mymessage)
    			.setTitle(title)
    			.setCancelable(true)
    			.setNeutralButton(android.R.string.cancel, onclick)
    			.show();
  }

  private boolean setupMap() {
    mMap = ((SupportMapFragment) getSupportFragmentManager().
   		     findFragmentById(R.id.map)).getMap();

    if (mMap == null) return false;

    mMap.setMyLocationEnabled(true);
    mMap.setInfoWindowAdapter(new Maps_InfoWindowAdapter(this, mStations));

    LocationManager lm = (LocationManager) getSystemService(
    		Context.LOCATION_SERVICE);
    Criteria crit = new Criteria();
    crit.setAccuracy(Criteria.ACCURACY_FINE);
    String provider = lm.getBestProvider(crit, true);
    Location lastKnownLocation = lm.getLastKnownLocation(provider);
    if (lastKnownLocation != null) {
    	LatLng ll = new LatLng(lastKnownLocation.getLatitude(),
    			lastKnownLocation.getLongitude());

   	  CameraUpdate cu = CameraUpdateFactory.newCameraPosition(
   		  	new CameraPosition(ll, 16, 0, 0));
   	  mMap.animateCamera(cu, 700, null);
    }

    return true;
  }

  private class DownloadReceiver extends ResultReceiver{
    public DownloadReceiver(Handler handler) {
      super(handler);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
      super.onReceiveResult(resultCode, resultData);

      if (resultCode == DownloadService.SUCCESS) {
      	mStations.clear();
        Hashtable<Integer, Station> result = (Hashtable<Integer, Station>) 
        		resultData.getSerializable("stations");
        // ToDo: update only the dynamic data once server will be ready
        for (Map.Entry<Integer, Station> mStation : result.entrySet()) {
        	mStations.put(mStation.getKey(), mStation.getValue());
        }

        bDownloading = false;
      	lightMessage(R.string.action_reload_complete);
      	updateMarkers(bModeFindBike);
      }
    }
  }

  protected void updateMarkers(boolean bFindBike) {
  	if (mStations == null) return;
  	
    mMap.clear();
    Iterator<Map.Entry<Integer, Station>> it = mStations.entrySet().
    		iterator();

    while (it.hasNext()) {
    	Map.Entry<Integer, Station> entry = it.next();
    	Station mStation = entry.getValue();

    	MarkerOptions mOpts = new MarkerOptions();
    	mOpts.position(mStation.getPosition());
    	mOpts.title(mStation.getName());
    	
    	int id = mStation.getId();
    	mOpts.snippet(String.valueOf(id));

    	int iIcon;
			if (! mStation.isOpened()) {
    		iIcon = R.drawable.presence_offline;
    	} else {
      	int iBikes = (bFindBike) ?
      			 mStation.getAvailableBikes() :
      			 mStation.getAvailableBikeStands();

      	iIcon = Util.resolveResourceFromNumber(mBikeResources, iBikes);
    	}

    	mOpts.icon(BitmapDescriptorFactory.fromResource(iIcon));
    	mMap.addMarker(mOpts);
    }
  }
}
