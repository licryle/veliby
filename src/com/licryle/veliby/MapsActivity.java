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
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.licryle.veliby.UI.Maps_InfoWindowAdapter;

public class MapsActivity extends FragmentActivity
		implements OnItemSelectedListener, View.OnClickListener {
	private GoogleMap mMap;

  protected String sFileStations = null;
  protected Hashtable<Integer, Station> mStations;


	protected static Hashtable<Integer, Integer> mBikeResources = 
			new Hashtable<Integer, Integer>() {{
				put(0,R.drawable.presence_invisible);
				put(2,R.drawable.presence_busy);
				put(5,R.drawable.presence_away);
				put(1000,R.drawable.presence_online);
			}}; 


    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      setContentView(R.layout.activity_maps);

      mStations = new Hashtable<Integer, Station>();
      // ToDo: load static info from file

      setupControls();
      setupMap();

      downloadMarkers();
    }

    @Override
    public void onResume() {
    	super.onResume();
    }
 
    protected void setupControls() {
		  ((ImageButton) findViewById(R.id.refresh)).setOnClickListener(this);

      Spinner spinner = (Spinner) findViewById(R.id.color_mode);
		  // Create an ArrayAdapter using the string array and a default spinner 
    //   layout.
	  ArrayAdapter<CharSequence> adapter =
	  		ArrayAdapter.createFromResource(this,
	  																		R.array.color_mode_array,
	  																		android.R.layout.simple_spinner_item);
	  // Specify the layout to use when the list of choices appears
	  adapter.setDropDownViewResource(
	  		android.R.layout.simple_spinner_dropdown_item);
	  // Apply the adapter to the spinner
		  spinner.setAdapter(adapter);
		  spinner.setOnItemSelectedListener(this);    	
    }
 
    protected void downloadMarkers() {
			((ImageButton) findViewById(R.id.refresh)).setClickable(false);
			((TextView) findViewById(R.id.refresh_date)).
					setText("Téléchargement en cours...");

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

        TextView mText = (TextView) findViewById(R.id.refresh_date);
        mText.setText("Mis à jour: " +
        							DateFormat.getDateTimeInstance().format(
        									Calendar.getInstance().getTime()));

      	Spinner spinner = (Spinner) findViewById(R.id.color_mode);
      	updateMarkers(spinner.getSelectedItemId() == 1);
      }
    }
  }

  protected void updateMarkers(boolean bReturnBike) {
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
      	int iBikes = (bReturnBike) ?
      			 mStation.getAvailableBikeStands() :
      			 mStation.getAvailableBikes();

      	iIcon = Util.resolveResourceFromNumber(mBikeResources, iBikes);
    	}

    	mOpts.icon(BitmapDescriptorFactory.fromResource(iIcon));
    	mMap.addMarker(mOpts);
    }

    ImageButton mButton = (ImageButton) findViewById(R.id.refresh);
    mButton.setClickable(true);
  }

	@Override
  public void onItemSelected(AdapterView<?> parent, View view, int position,
      long id) {
		updateMarkers(id == 1);
  }

	@Override
  public void onNothingSelected(AdapterView<?> parent) {
		((Spinner) parent).setSelection(0);    
  }

	@Override
  public void onClick(View v) {
    // Refresh button
		switch(v.getId()) {
			case R.id.refresh:
				downloadMarkers();
			break;
		}
  }
}
