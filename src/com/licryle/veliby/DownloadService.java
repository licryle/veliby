package com.licryle.veliby;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

public class DownloadService extends IntentService {
  public static final int UPDATE_PROGRESS = 8344;
  public static final int SUCCESS = 8345;
  public static final int FAILURE_CONNECTION = 8346;
  public static final int FAILURE_GENERIC = 8347;
  public DownloadService() {
    super("DownloadService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    String sUrlToDownload = intent.getStringExtra("url");
    ResultReceiver mReceiver = (ResultReceiver) intent.
    		getParcelableExtra("receiver");
    boolean bFullCycle = intent.getBooleanExtra("full_cycle", true);

    try {
      URL mUrl = new URL(sUrlToDownload);
      URLConnection mConnection = mUrl.openConnection();
      mConnection.connect();
      // this will be useful so that you can show a typical 0-100% progress bar
      int fileLength = mConnection.getContentLength();

      // download the file
      InputStream mInput = new BufferedInputStream(mUrl.openStream());
      ByteArrayOutputStream mOutput = new ByteArrayOutputStream();

      byte aData[] = new byte[1024];
      long lTotal = 0;
      int iCount;
      while ((iCount = mInput.read(aData)) != -1) {
        lTotal += iCount;
        // publishing the progress....
        Bundle mResultData = new Bundle();

        if (fileLength > 0) {
        	mResultData.putInt("progress", (int) (lTotal * 100 / fileLength));
        }

        mReceiver.send(UPDATE_PROGRESS, mResultData);
        mOutput.write(aData, 0, iCount);
      }

      mOutput.flush();
      mInput.close();

      Bundle mResultData = new Bundle();
      if (bFullCycle) {
        mResultData.putSerializable("stations", parseFullData(mOutput));
      } else {
        mResultData.putSerializable("stations", parseDynamicData(mOutput));
      }
      mOutput.close();

      mResultData.putBoolean("full_cycle", bFullCycle);
      mReceiver.send(SUCCESS, mResultData);
    } catch (ConnectException e) {
      Bundle mResultData = new Bundle();
      mReceiver.send(FAILURE_CONNECTION, mResultData);
      return;
    } catch (IOException e) {
      Bundle mResultData = new Bundle();
      mReceiver.send(FAILURE_GENERIC, mResultData);
      e.printStackTrace();
    }
  }

  protected Serializable parseFullData(ByteArrayOutputStream mInput) {
    String sInput = new String(mInput.toByteArray());
    JSONArray mJSon;
    try {
	    mJSon = new JSONArray(sInput);
    
	    Hashtable<Integer, Station> mNewStations =
	    		new Hashtable<Integer, Station>();
	    for (int i=0; i < mJSon.length(); i++) {
	    	Station mStation = new Station(mJSon.getJSONObject(i));
	    	mNewStations.put(mStation.getId(), mStation);
	    }

	    return mNewStations;
    } catch (JSONException e) {
	    e.printStackTrace();
	    return null;
    }
  }

  protected Serializable parseDynamicData(ByteArrayOutputStream mInput) {
  	if (mInput.size() % 6 != 0) return null;

  	byte aData[] = mInput.toByteArray();
  	int i = 0;

  	while (i < mInput.size()) {
  		int iId = Util.intToUInt((new Byte(aData[i++])).intValue(), 8) +
  							Util.intToUInt((new Byte(aData[i++])).intValue() << 8, 16) +
  							Util.intToUInt((new Byte(aData[i++])).intValue() << 16, 32);

  		int iAvBikes			= (new Byte(aData[i++])).intValue();
  		int iAvBikeStands = (new Byte(aData[i++])).intValue();
  		boolean bOpened 	= (new Byte(aData[i++])).intValue() == 1;
  		
  		new Station(iId, iAvBikes, iAvBikeStands, bOpened);
  	}

    return null;
  }
}
