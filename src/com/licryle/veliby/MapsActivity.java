package com.licryle.veliby;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity
		implements OnItemSelectedListener, View.OnClickListener,
							 InfoWindowAdapter {
	private GoogleMap mMap;
	private View mInfoView;

  protected String sFileStations = null;
  protected Hashtable<Integer, Station> mStations = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      setContentView(R.layout.activity_maps);

      Spinner spinner = (Spinner) findViewById(R.id.color_mode);
		  // Create an ArrayAdapter using the string array and a default spinner layout
		  ArrayAdapter<CharSequence> adapter =
		  		ArrayAdapter.createFromResource(this,
		  																		R.array.color_mode_array,
		  																		android.R.layout.simple_spinner_item);
		  // Specify the layout to use when the list of choices appears
		  adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		  // Apply the adapter to the spinner
		  spinner.setAdapter(adapter);
		  spinner.setOnItemSelectedListener(this);

		  ((ImageButton) findViewById(R.id.refresh)).setOnClickListener(this);

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

    /*@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }*/

    protected void alertBox(String title, String mymessage,
    		OnClickListener onclick) {
      new AlertDialog.Builder(this)
      			.setMessage(mymessage)
      			.setTitle(title)
      			.setCancelable(true)
      			.setNeutralButton(android.R.string.cancel, onclick)
      			.show();
    }

    private boolean setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().
           		     findFragmentById(R.id.map)).getMap();

            if (mMap == null) return false;

            mMap.setMyLocationEnabled(true);
            mMap.setInfoWindowAdapter(this);

            mInfoView = getLayoutInflater().inflate(R.layout.map_infoview,
            																			  null);

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
          mStations = (Hashtable<Integer, Station>) resultData.
          		getSerializable("stations");

          TextView mText = (TextView) findViewById(R.id.refresh_date);
          mText.setText("Mis à jour: " +
          							DateFormat.getDateTimeInstance().format(
          									Calendar.getInstance().getTime()));

        	Spinner spinner = (Spinner) findViewById(R.id.color_mode);
        	addMarkers(spinner.getSelectedItemId() == 1);
        }
      }
    }

    protected void addMarkers(boolean bReturnBike) {
    	if (mStations == null) return;
    	
      mMap.clear();
      Iterator<Map.Entry<Integer, Station>> it = mStations.entrySet().iterator();

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

        	switch (iBikes) {
      			case 0:
      				iIcon = R.drawable.presence_invisible;
      			break;

      			case 1:
      			case 2:
      				iIcon = R.drawable.presence_busy;
      			break;

      			case 3:
      			case 4:
      			case 5:
      				iIcon = R.drawable.presence_away;
      			break;

      			default:
      				iIcon = R.drawable.presence_online;
        	}
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
			addMarkers(id == 1);
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

		@Override
    public View getInfoContents(Marker marker) {
			String sStationId = marker.getSnippet();
			Station mStation = mStations.get(Integer.valueOf(sStationId));

			// Title
			TextView mTitle = (TextView) mInfoView.findViewById(R.id.infoview_title);
			mTitle.setText(mStation.getName());

			// Take
			int iNbBikes = mStation.getAvailableBikes();

			TextView mBikes = (TextView) mInfoView.findViewById(R.id.infoview_bikes);
			mBikes.setText("" + iNbBikes);

			int color;
			switch (iNbBikes) {
				case 0:
					color = getResources().getColor(R.color.infoview_nobike);
				break;
	
				case 1:
				case 2:
					color = getResources().getColor(R.color.infoview_fewbikes);
				break;
	
				case 3:
				case 4:
				case 5:
					color = getResources().getColor(R.color.infoview_somebikes);
				break;
	
				default:
					color = getResources().getColor(R.color.infoview_plentybikes);
			}

			mBikes.setTextColor(color);

			// Return
			iNbBikes = mStation.getAvailableBikeStands();

			mBikes = (TextView) mInfoView.findViewById(R.id.infoview_stands);
			mBikes.setText("" + iNbBikes);

			switch (iNbBikes) {
				case 0:
					color = getResources().getColor(R.color.infoview_nobike);
				break;
	
				case 1:
				case 2:
					color = getResources().getColor(R.color.infoview_fewbikes);
				break;
	
				case 3:
				case 4:
				case 5:
					color = getResources().getColor(R.color.infoview_somebikes);
				break;
	
				default:
					color = getResources().getColor(R.color.infoview_plentybikes);
			}

			mBikes.setTextColor(color);

			int iVisibility = mStation.hasBonus() ? View.VISIBLE : View.GONE;
			mInfoView.findViewById(R.id.map_infoview_row_bonus).
					setVisibility(iVisibility);

			iVisibility = mStation.hasBanking() ? View.VISIBLE : View.GONE;
			mInfoView.findViewById(R.id.map_infoview_row_bank).
					setVisibility(iVisibility);

			return mInfoView;
    }

		@Override
    public View getInfoWindow(Marker marker) {
	    // TODO Auto-generated method stub
	    return null;
    }
}
