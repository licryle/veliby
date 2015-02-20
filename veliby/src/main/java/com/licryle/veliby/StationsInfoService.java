package com.licryle.veliby;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import com.licryle.veliby.BikeMap.Contract;
import com.licryle.veliby.BikeMap.Contracts;
import com.licryle.veliby.BikeMap.Station;
import com.licryle.veliby.BikeMap.Stations;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.Hashtable;

public class StationsInfoService extends IntentService {
  public static final int UPDATE_PROGRESS = 8344;
  public static final int SUCCESS_STATIONS = 8345;
  public static final int FAILURE_STATIONS_CONNECTION = 8346;
  public static final int FAILURE_STATIONS_GENERIC = 8347;
  public static final int FAILURE_STATIONS_PARSE = 8348;
  public static final int FAILURE_WRONG_CONTRACT = 8354;
  public static final int FINISHED = 8349;
  public static final int SUCCESS_CONTRACTS = 8350;
  public static final int FAILURE_CONTRACTS_CONNECTION = 8351;
  public static final int FAILURE_CONTRACTS_GENERIC = 8352;
  public static final int FAILURE_CONTRACTS_PARSE = 8353;

  protected Hashtable<String, ResultReceiver> _mRequesters;
  protected Stations _mStations;
  protected Contracts _mContracts;

  public StationsInfoService() {
    super("StationsInfoService");
    Log.i("StationsInfoService", "Entering Constructor()");

    _mRequesters = new Hashtable<String, ResultReceiver>();
  }

  protected synchronized boolean _isConcurrent(ResultReceiver mReceiver,
                                               String sRequestor) {
    if (_mRequesters.containsKey(sRequestor)) return true;

    _mRequesters.put(sRequestor, mReceiver);

    // we do not queue the Intent, we just register the requester
    return (_mRequesters.size() > 1);
  }

  public int onStartCommand (Intent intent, int flags, int startId) {
    if (intent != null) {
      ResultReceiver mReceiver = (ResultReceiver)
          intent.getParcelableExtra("receiver");
      String sRequestor = intent.getStringExtra("requestor");

      if (_isConcurrent(mReceiver, sRequestor)) return 0;
    }

    return super.onStartCommand(intent, flags, startId);
  }

  protected void _loadStations(File mStationsFile, int iDlDynamic) {
    if (_mStations == null) {
      _mStations = Stations.loadStationsInfo(mStationsFile, iDlDynamic);
    }
  }

  protected void _loadContracts(File mContractsFile) {
    if (_mContracts == null) {
      _mContracts = Contracts.loadFromFile(mContractsFile);
    }
  }


  private int _downloadContracts(String sUrlContracts) {
    Log.i("StationsInfoService", "Entering _downloadContracts()");
    try {
      ByteArrayOutputStream mOutput = _downloadData(sUrlContracts);

      _parseContractsData(mOutput);
      mOutput.close();
    } catch (ConnectException e) {
      e.printStackTrace();
      return FAILURE_CONTRACTS_CONNECTION;
    } catch (IOException e) {
      e.printStackTrace();
      return FAILURE_CONTRACTS_GENERIC;
    } catch (Exception e) {
      e.printStackTrace();
      return FAILURE_CONTRACTS_PARSE;
    }
    return SUCCESS_CONTRACTS;
  }

  protected int _downloadStations(boolean bFullCycle, Contract mContract) {
    Log.i("StationsInfoService", "Entering _downloadStations()");
    try {
      String sUrl = bFullCycle ? Settings.getURLDownloadFull(mContract) :
          Settings.getURLDownloadDynamic(mContract);

      ByteArrayOutputStream mOutput = _downloadData(sUrl);

      if (bFullCycle) {
        _parseStationsFullData(mOutput, mContract);
      } else {
        _parseStationsDynamicData(mOutput, mContract);
      }
      mOutput.close();

      return SUCCESS_STATIONS;
    } catch (ConnectException e) {
      e.printStackTrace();
      return FAILURE_STATIONS_CONNECTION;
    } catch (IOException e) {
      e.printStackTrace();
      return FAILURE_STATIONS_GENERIC;
    } catch (Exception e) {
      e.printStackTrace();
      return FAILURE_STATIONS_PARSE;
    }
  }

  protected ByteArrayOutputStream _downloadData(String sUrl)
      throws IOException {
    Log.i("StationsInfoService", "Entering _downloadData()");

    URL mUrl = new URL(sUrl);
    URLConnection mConnection = mUrl.openConnection();
    mConnection.connect();

    // For progress report
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
        _dispatchResults(UPDATE_PROGRESS, mResultData);
      }

      mOutput.write(aData, 0, iCount);
    }

    mOutput.flush();
    mInput.close();

    Log.i("StationsInfoService", "Leaving _downloadData()");
    return mOutput;
  }

  protected boolean _updateContracts(int iDlContracts, String sContractsUrl) {
    Log.i("StationsInfoService", "Entering _handleContracts()");
    boolean bExpiredContracts = _mContracts.isStaticExpired(iDlContracts);

    int iSignal = (_mContracts != null && _mContracts.size() > 0) ?
        SUCCESS_CONTRACTS : FAILURE_CONTRACTS_GENERIC;

    if (bExpiredContracts) { // Download contracts if they are expired
      // Send result to process asking, write after, all in background
      iSignal = _downloadContracts(sContractsUrl);
    }

    Bundle mBundle = new Bundle();
    mBundle.putSerializable("contracts", _mContracts);
    _dispatchResults(iSignal, mBundle);

    Log.i("StationsInfoService", "Leaving _handleContracts()");
    return bExpiredContracts && iSignal == SUCCESS_CONTRACTS;
  }

  protected boolean _updateStations(int iDlStatic, int iDlDynamic,
                                    int iContract) {
    Log.i("StationsInfoService", "Entering _handleStations()");
    boolean bStaticExpired = _mStations.isStaticExpired(iDlStatic);
    boolean bDynamicExpired = _mStations.isDynamicExpired(iDlDynamic);

    // check if we changed contract
    boolean bDiffContract = false;
    try {
      bDiffContract = (_mStations.size() == 0) ||
          (iContract != _mStations.entrySet().iterator().
              next().getValue().getContract().getId());
    } catch (Exception e) {
      bDiffContract = true;
    }

    // If stations data is invalidated
    int iSignal = (_mStations != null && _mStations.size() > 0) ?
        SUCCESS_STATIONS : FAILURE_STATIONS_GENERIC;
    if (bStaticExpired || bDynamicExpired || bDiffContract) {
      if (_mContracts == null) {
        iSignal = FAILURE_WRONG_CONTRACT;
      } else {
        Contract mContract = _mContracts.findContractById(iContract);
        if (mContract == null) {
          iSignal = FAILURE_WRONG_CONTRACT;
        } else {
          iSignal = _downloadStations(bStaticExpired || bDiffContract, mContract);
        }
      }
    } else {
      iSignal = SUCCESS_STATIONS;
    }

    if ( iSignal != SUCCESS_STATIONS && _mStations != null) {
      // We send static info for reference
      _mStations.removeDynamicData();
    }

    // Send result to process asking, write after, all in background
    Bundle mBundle = new Bundle();
    mBundle.putSerializable("stations", _mStations);
    _dispatchResults(iSignal, mBundle);

    Log.i("StationsInfoService", "Leaving _handleStations()");
    return (bStaticExpired || bDynamicExpired || bDiffContract) &&
        iSignal == SUCCESS_STATIONS;
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.i("StationsInfoService", "Entering onHandleIntent()");

    if (_mRequesters.size() == 0) return;

    String sStationsFile = intent.getStringExtra("stations_file");
    File mStationsFile = new File(sStationsFile);
    int iDlStatic = intent.getIntExtra("dl_static", 1000);
    int iDlDynamic = intent.getIntExtra("dl_dynamic", 1000);

    int iDlContracts = intent.getIntExtra("dl_contracts", 1000);
    int iContract = intent.getIntExtra("contract_id", 0);
    String sContractsUrl = intent.getStringExtra("contracts_url");
    String sContractsFile = intent.getStringExtra("contracts_file");
    File mContractsFile = new File(sContractsFile);

    _loadContracts(mContractsFile);
    _loadStations(mStationsFile, iDlDynamic);

    if (_updateContracts(iDlContracts, sContractsUrl))
      _mContracts.saveToFile(mContractsFile);

    if (iContract != 0 && _updateStations(iDlStatic, iDlDynamic, iContract))
      _mStations.saveToFile(mStationsFile);

    Bundle mBundle = new Bundle();
    _dispatchResults(FINISHED, mBundle);

    _mRequesters.clear();
    Log.i("StationsInfoService", "Leaving onHandleIntent()");
  }

  private void _parseContractsData(ByteArrayOutputStream mInput)
      throws JSONException {
    Log.i("StationsInfoService", "Entering _parseContractsData()");
    String sInput = new String(mInput.toByteArray());
    JSONArray mJSon = new JSONArray(sInput);

    Contracts mNewContracts = new Contracts();
    for (int i=0; i < mJSon.length(); i++) {
      try {
        Contract mContract = new Contract(mJSon.getJSONObject(i));
        mNewContracts.put(mContract.getId(), mContract);
      } catch (Exception e) {
        Log.i("StationsInfoService", "1 station rejected, JSON invalid. " +
            e.getMessage());
      }
    }
    mNewContracts.setLastUpdate(Calendar.getInstance().getTime());

    _mContracts = mNewContracts;
    Log.i("StationsInfoService", "Leaving _parseContractsData()");
  }

  protected void _parseStationsFullData(ByteArrayOutputStream mInput,
                                Contract mContract) throws JSONException {
    Log.i("StationsInfoService", "Entering _parseStationsFullData()");
    String sInput = new String(mInput.toByteArray());
    JSONArray mJSon = new JSONArray(sInput);

    Stations mNewStations = new Stations();
    for (int i=0; i < mJSon.length(); i++) {
      try {
        Station mStation = new Station(mContract, mJSon.getJSONObject(i));
        mNewStations.put(mStation.getId(), mStation);
      } catch (Exception e) {
        Log.i("StationsInfoService", "1 station rejected, JSON invalid. " +
            e.getMessage());
      }
    }
    mNewStations.setLastUpdate(Calendar.getInstance().getTime());

    _mStations = mNewStations;
    Log.i("StationsInfoService", "Leaving _parseStationsFullData()");
  }

  protected void _parseStationsDynamicData(ByteArrayOutputStream mInput,
                                   Contract mContract)
      throws Exception {
    Log.i("StationsInfoService", "Entering _parseStationsDynamicData()");
    if (mInput.size() % 5 != 0)
      throw new Exception("Not rounded dynamic data");

    byte aData[] = mInput.toByteArray();
    int i = 0;

    while (i < mInput.size()) {
      int iNumber = Util.intToUInt((new Byte(aData[i++])).intValue(), 8) +
          Util.intToUInt((new Byte(aData[i++])).intValue() << 8, 16) +
          Util.intToUInt((new Byte(aData[i++])).intValue() << 16, 24);

      Station mStationToUp = _mStations.get(Station.generateId(mContract.getId(), iNumber));

      if (mStationToUp != null) {
        int iAvBikes      = (new Byte(aData[i++])).intValue();
        int iAvBikeStands = (new Byte(aData[i++])).intValue();
        boolean bOpened   = (iAvBikes > 0) || (iAvBikeStands > 0);

        mStationToUp.update(bOpened, iAvBikes, iAvBikeStands);
      } else {
        i = i +2;
      }
    }
    _mStations.setLastUpdate(Calendar.getInstance().getTime());
    Log.i("StationsInfoService", "Leaving _parseStationsDynamicData()");
  }

  protected void _dispatchResults(int iSignal, Bundle mBundle) {
    for(ResultReceiver mReceiver : _mRequesters.values()) {
      mReceiver.send(iSignal, mBundle);
    }
  }
}
