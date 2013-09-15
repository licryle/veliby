package com.licryle.veliby;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
  public DownloadService() {
    super("DownloadService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    String urlToDownload = intent.getStringExtra("url");
    ResultReceiver receiver = (ResultReceiver) intent.
    		getParcelableExtra("receiver");

    try {
      URL url = new URL(urlToDownload);
      URLConnection connection = url.openConnection();
      connection.connect();
      // this will be useful so that you can show a typical 0-100% progress bar
      int fileLength = connection.getContentLength();

      // download the file
      InputStream input = new BufferedInputStream(url.openStream());
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      //	FileOutputStream(intent.getStringExtra("tempFile"));

      byte data[] = new byte[1024];
      long total = 0;
      int count;
      while ((count = input.read(data)) != -1) {
        total += count;
        // publishing the progress....
        Bundle resultData = new Bundle();
        resultData.putInt("progress", (int) (total * 100 / fileLength));
        receiver.send(UPDATE_PROGRESS, resultData);
        output.write(data, 0, count);
      }

      output.flush();

      String sInput = new String( output.toByteArray() );

      output.close();
      input.close();

      JSONArray mJSon = new JSONArray(sInput);
      
      Hashtable<Integer, Station> mNewStations =
      		new Hashtable<Integer, Station>();
      for (int i=0; i < mJSon.length(); i++) {
      	Station mStation = new Station(mJSon.getJSONObject(i));
      	mNewStations.put(mStation.getId(), mStation);
      }

      Bundle resultData = new Bundle();
      resultData.putSerializable("stations", mNewStations);
      receiver.send(SUCCESS, resultData);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (JSONException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
    }

    Bundle resultData = new Bundle();
    resultData.putInt("progress", 100);
    receiver.send(UPDATE_PROGRESS, resultData);
  }
}
