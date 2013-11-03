package com.licryle.veliby.BikeMap;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONException;

import com.licryle.veliby.Util;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

public class DownloadStationsService extends IntentService {
  public static final int UPDATE_PROGRESS = 8344;
  public static final int SUCCESS = 8345;
  public static final int FAILURE_CONNECTION = 8346;
  public static final int FAILURE_GENERIC = 8347;
  public static final int FAILURE_PARSE = 8348;
  public static final int FINISHED = 8349;
  public DownloadStationsService() {
    super("DownloadService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    String sUrlToDownload = intent.getStringExtra("url");
    ResultReceiver mReceiver = (ResultReceiver) intent.
    		getParcelableExtra("receiver");
    boolean bFullCycle = intent.getBooleanExtra("full_cycle", true);
    String sFile = intent.getStringExtra("file");
    File mFile = new File(sFile);

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
      Stations mStations;
      if (bFullCycle) {
        mStations = parseFullData(mOutput);
      } else {
        mStations = parseDynamicData(mOutput, mFile);
      }
      mOutput.close();

      // Send result to process asking, write in background
      mResultData.putSerializable("stations", mStations);
      mReceiver.send(SUCCESS, mResultData);
      mStations.saveStationsInfo(mFile);
    } catch (ConnectException e) {
      mReceiver.send(FAILURE_CONNECTION, new Bundle());
      e.printStackTrace();
    } catch (IOException e) {
      mReceiver.send(FAILURE_GENERIC, new Bundle());
      e.printStackTrace();
    } catch (Exception e) {
      mReceiver.send(FAILURE_PARSE, new Bundle());
      e.printStackTrace();
    }
    mReceiver.send(FINISHED, new Bundle());
    stopSelf();
  }

  protected Stations parseFullData(ByteArrayOutputStream mInput)
      throws JSONException {
    String sInput = new String(mInput.toByteArray());
    JSONArray mJSon = new JSONArray(sInput);
  
    Stations mNewStations = new Stations();
    for (int i=0; i < mJSon.length(); i++) {
    	Station mStation = new Station(mJSon.getJSONObject(i));
    	mNewStations.put(mStation.getId(), mStation);
    }
    mNewStations.setLastUpdate(Calendar.getInstance().getTime());

    return mNewStations;
  }

  protected Stations parseDynamicData(ByteArrayOutputStream mInput, File mFile)
      throws Exception {
  	if (mInput.size() % 6 != 0) throw new Exception("Not rounded dynamic data");

  	byte aData[] = mInput.toByteArray();
  	int i = 0;

    Stations mStations = Stations.loadStationsInfo(mFile, 10000);
  	while (i < mInput.size()) {
  		int iId = Util.intToUInt((new Byte(aData[i++])).intValue(), 8) +
  							Util.intToUInt((new Byte(aData[i++])).intValue() << 8, 16) +
  							Util.intToUInt((new Byte(aData[i++])).intValue() << 16, 32);

      Station mStationToUp = mStations.get(iId);

      if (mStationToUp != null) {
        int iAvBikes      = (new Byte(aData[i++])).intValue();
        int iAvBikeStands = (new Byte(aData[i++])).intValue();
        boolean bOpened   = (new Byte(aData[i++])).intValue() == 1;
        
        mStationToUp.update(bOpened, iAvBikes, iAvBikeStands);
      }
  	}
  	mStations.setLastUpdate(Calendar.getInstance().getTime());

    return mStations;
  }
}
