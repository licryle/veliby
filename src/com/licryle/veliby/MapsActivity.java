package com.licryle.veliby;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.internal.fc;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.licryle.veliby.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

public class MapsActivity extends FragmentActivity {
	private GoogleMap mMap;
  protected String sFileStations = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_maps);

        if (!setUpMapIfNeeded()) {
        	OnClickListener onclick = new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							// exit getApplication()
						}
					};

        	alertBox("Connection issue!",
        					 "It seems that you are having connection issues," +
        							 "please check your Wifi/Data connections",
        					 onclick);
        }

        downloadMarkers();
    }

    @Override
    public void onResume() {
    	super.onResume();
    }
 
    protected void downloadMarkers() {
    	Intent intent = new Intent(this, DownloadService.class);

    	File appDir = new File(
    			Environment.getExternalStorageDirectory().getPath() + "/Veliby/");
    	// have the object build the directory structure, if needed.
    	appDir.mkdirs();
    	sFileStations = appDir.getAbsolutePath() +
					"/com.licryle.veliby.stations.status";

    	intent.putExtra("url", "https://api.jcdecaux.com/vls/v1/stations?contract=Paris&apiKey=718b4e0e0b1f01af842ff54c38bed00eaa63ce3c");
    	intent.putExtra("tempFile", sFileStations);
    	intent.putExtra("receiver", new DownloadReceiver(new Handler()));
    	startService(intent);
    }

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }*/

    protected void alertBox(String title, String mymessage, OnClickListener onclick) {
      new AlertDialog.Builder(this)
      			.setMessage(mymessage)
      			.setTitle(title)
      			.setCancelable(true)
      			.setNeutralButton(android.R.string.cancel, onclick)
      			.show();
    }

    private boolean setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.-
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().
           		     findFragmentById(R.id.map)).getMap();

            if (mMap == null) return false;

            mMap.setMyLocationEnabled(true);

            LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
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
        }
        return true;
    }

    private class DownloadReceiver extends ResultReceiver{
      public DownloadReceiver(Handler handler) {
          super(handler);
      }

      @Override
      protected void onReceiveResult(int resultCode, Bundle resultData) {
        super.onReceiveResult(resultCode, resultData);
        if (resultCode == DownloadService.UPDATE_PROGRESS) {
          int progress = resultData.getInt("progress");
          if (progress == 100) {
          	addMarkers();
          }
        }
      }

      protected void addMarkers() {
      	try {
	        String sInput = Util.readFile(sFileStations);
	      
	        JSONArray mJSon = new JSONArray(sInput);

	        mMap.clear();
	        for (int i=0; i < mJSon.length(); i++) {
	        	JSONObject mStation = mJSon.getJSONObject(i);

	        	JSONObject mPos = mStation.getJSONObject("position");

	        	MarkerOptions mOpts = new MarkerOptions();
	        	mOpts.position(new LatLng(mPos.getDouble("lat"),
	        														mPos.getDouble("lng")));
	        	mOpts.title(mStation.getString("name"));
	        	mOpts.snippet("Status: " + mStation.getString("status") + " " +
	        								"Bikes: " + mStation.getString("available_bikes") + " " +
	        								"Return: " + mStation.getString("available_bike_stands") + " " +
	        								"Bonus: " + mStation.getString("bonus") + " " +
	        								"Address: " + mStation.getString("address"));

	        	float fColor;
	        	if (! mStation.getString("status").equalsIgnoreCase("OPEN")) {
	        		fColor = BitmapDescriptorFactory.HUE_VIOLET;
	        	} else {
		        	switch (mStation.getInt("available_bikes")) {
	        			case 0:
	        				fColor = BitmapDescriptorFactory.HUE_RED;
	        			break;

	        			case 1:
	        			case 2:
	        				fColor = BitmapDescriptorFactory.HUE_ORANGE;
	        			break;

	        			case 3:
	        			case 4:
	        			case 5:
	        				fColor = BitmapDescriptorFactory.HUE_YELLOW;
	        			break;

	        			default:
	        				fColor = BitmapDescriptorFactory.HUE_GREEN;
		        	}
	        	}
	        		
	        	mOpts.icon(BitmapDescriptorFactory.defaultMarker(fColor));
	        	mMap.addMarker(mOpts);
	        }
        } catch (FileNotFoundException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (JSONException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        } catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
        }
      }
    }
}
