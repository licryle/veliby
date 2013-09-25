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
import com.licryle.veliby.UI.Maps_InfoWindowAdapter;

public class MapsActivity extends ActionBarActivity {
	private GoogleMap mMap;

  protected String sFileStations = null;
  protected Hashtable<Integer, Station> mStations;
  protected boolean	bModeFindBike = true;
  protected boolean	bDownloading = false;
  protected File mStationsDataFile;
  protected int iStaticDeadline = 7;
  protected String sUrlFull = "https://api.jcdecaux.com/vls/v1/stations?" +
  		"contract=Paris&apiKey=718b4e0e0b1f01af842ff54c38bed00eaa63ce3c";
  protected String sUrlDynamic = "http://veliby.berliat.fr/?c=1";

	protected static Hashtable<Integer, Integer> mBikeResources = 
			new Hashtable<Integer, Integer>() {
        private static final long serialVersionUID = -276505145697466182L;
				{
					put(0,R.drawable.presence_invisible);
					put(2,R.drawable.presence_busy);
					put(4,R.drawable.presence_away);
					put(1000,R.drawable.presence_online);
				}
			}; 


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_maps);
    mStations = new Hashtable<Integer, Station>();

    loadStaticInfo();

    setupMap();

    if (hasPlayServices()) {
    	downloadMarkers();

    	firstStart();
    }
  }

  private void loadStaticInfo() {
  	File mAppDir = new File(
  			Environment.getExternalStorageDirectory().getPath() + "/Veliby/");

   	mAppDir.mkdirs();
  	mStationsDataFile = new File(mAppDir.getAbsolutePath() +
				"/stations.comlete");

		try {
	    FileInputStream mInput = new FileInputStream(mStationsDataFile);
	    ObjectInputStream mObjectStream = new ObjectInputStream(mInput);
	    mStations = (Hashtable<Integer, Station>) mObjectStream.readObject();
	    mObjectStream.close();
	    return;
    } catch (FileNotFoundException e) {
	    e.printStackTrace();
    } catch (StreamCorruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
    } catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
    } catch (ClassNotFoundException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
    }

		mStations = new Hashtable<Integer, Station>();
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

        updateMarkers(bModeFindBike);
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

  protected void downloadMarkers() {
  	if (bDownloading) return;
  	bDownloading = true;

  	lightMessage(R.string.action_reload_start, true);

  	Intent intent = new Intent(this, DownloadService.class);

  	if (isStaticDataExpired()) {
    	intent.putExtra("url", sUrlFull);
    	intent.putExtra("full_cycle", true);
  	} else {
    	intent.putExtra("url", sUrlDynamic);
    	intent.putExtra("full_cycle", false);
  	}
  	intent.putExtra("receiver", new DownloadReceiver(new Handler()));
  	startService(intent);
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

  private boolean hasPlayServices() {
  	int iStatus = GooglePlayServicesUtil.
  			isGooglePlayServicesAvailable(getApplicationContext());
  	if (iStatus != ConnectionResult.SUCCESS) {
      alertBox(getResources().getString(R.string.playservice_issue_title),
      		getResources().getString(R.string.playservice_issue_content), null);
      return false;
  	}

  	return true;
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

    updateMarkers(bModeFindBike);

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

      switch (resultCode) {
      	case DownloadService.SUCCESS:
	        if (resultData.getBoolean("full_cycle")) {
	        	mStations.clear();
	          Hashtable<Integer, Station> mResult = (Hashtable<Integer, Station>) 
	          		resultData.getSerializable("stations");
	 
		        for (Map.Entry<Integer, Station> mStation : mResult.entrySet()) {
		        	mStations.put(mStation.getKey(), new Station(mStation.getValue()));
		        }
	
	          mStationsDataFile.delete();
		        FileOutputStream mOutput;
	          try {
		          mOutput = new FileOutputStream(mStationsDataFile);
			        ObjectOutputStream mObjectStream = new ObjectOutputStream(mOutput);
	 
			        // Nulling dynamic data for storage
			        Iterator<Map.Entry<Integer, Station>> it = mResult.entrySet().
			        		iterator();
	
			        while (it.hasNext()) {
			        	Map.Entry<Integer, Station> entry = it.next();
			        	Station mStation = entry.getValue();
			        	mStation.update(false, 0, 0);
			        }
	
			        mObjectStream.writeObject(mResult);
			        mObjectStream.close();
	          } catch (FileNotFoundException e) {
		          e.printStackTrace();
	          } catch (IOException e) {
		          e.printStackTrace();
		          mStationsDataFile.delete(); // TODO: this isn't reliable
	          }
	        } else {
	          Hashtable<Integer, Station> mResult = (Hashtable<Integer, Station>) 
	          		resultData.getSerializable("stations");
		        for (Map.Entry<Integer, Station> mStation : mResult.entrySet()) {
		        	Station mStationToUp = mStations.get(mStation.getValue().getId());
	
		        	if (mStationToUp != null) {
		        		mStationToUp.update(mStation.getValue().isOpened(),
		        				mStation.getValue().getAvailableBikes(),
		        				mStation.getValue().getAvailableBikeStands());
		        	}
		        }
	        }
	
	        bDownloading = false;
	      	updateMarkers(bModeFindBike);
	      	lightMessage(R.string.action_reload_complete, false);
	      break;

      	case DownloadService.FAILURE_CONNECTION:
      	case DownloadService.FAILURE_GENERIC:
      	case DownloadService.FAILURE_PARSE:
	      	lightMessage(R.string.action_reload_failure, true);
	      break;
      }
    }
  }

  protected void updateMarkers(boolean bFindBike) {
  	if (mStations == null || mMap == null) return;
  	
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
