package com.licryle.veliby.FavStationWidget;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

import com.licryle.veliby.MapsActivity;
import com.licryle.veliby.R;
import com.licryle.veliby.Settings;
import com.licryle.veliby.Util;
import com.licryle.veliby.BikeMap.DownloadStationsService;
import com.licryle.veliby.BikeMap.Station;
import com.licryle.veliby.BikeMap.Stations;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.os.ResultReceiver;
import android.util.Log;
import android.widget.RemoteViews;

public class Provider extends AppWidgetProvider {
  protected static final String FAVSTATION_WIDGET_UPDATE =
      "com.licryle.veliby.favstationwidget.WIDGET_UPDATE";
  protected boolean _bDownloading = false;
  protected Context _mContext = null; // TODO: make it less hacky

  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);
    _mContext = context;

    if (FAVSTATION_WIDGET_UPDATE.equals(intent.getAction())) {
      ComponentName mAppWidget = new ComponentName(
          context.getPackageName(), getClass().getName());
      AppWidgetManager mWdgMgr = AppWidgetManager.getInstance(context);
      int aIds[] = mWdgMgr.getAppWidgetIds(mAppWidget);

      onUpdate(context, mWdgMgr, aIds);
    }
  }

  public void onEnabled(Context context) {
    super.onEnabled(context);
    Log.i("Provider", "Entering onEnabled()");
    _mContext = context;

    Settings mSettings = Settings.getInstance(context);
    File mAppDir = mSettings.getVelibyPath();
    mAppDir.mkdirs();

    AlarmManager alarmManager = (AlarmManager) context.getSystemService(
        Context.ALARM_SERVICE);
    alarmManager.setRepeating(AlarmManager.RTC,
        System.currentTimeMillis() + 1000,
        Settings.getInstance(context).getDynamicDeadLine() * 60000,
        _createDataUpdateIntent(context));
  }

  public void onDisabled(Context context) {
    Log.i("Provider", "Entering onDisabled()");
    super.onDisabled(context);
    _mContext = context;
 
    AlarmManager alarmManager = (AlarmManager)context.getSystemService(
        Context.ALARM_SERVICE);
    alarmManager.cancel(_createDataUpdateIntent(context));
  }

  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
      int[] appWidgetIds) {
    super.onUpdate(context, appWidgetManager, appWidgetIds);
    _mContext = context;

    Settings mSettings = Settings.getInstance(context);

    ArrayList<Integer> aFavStations = mSettings.getFavStations();

    if (aFavStations.size() > 0) {
      _downloadMarkers();
    }
  }

  protected RemoteViews _buildStationView(Station mStation) {
    RemoteViews mStationView = new RemoteViews(_mContext.getPackageName(),
        R.layout.widget_favstations_item);
    Settings mSettings = Settings.getInstance(_mContext);

    // Take
    int iNbBikes = mStation.isOpened() ? mStation.getAvailableBikes() : 0;
    mStationView.setTextViewText(R.id.widget_station_bikes, "" + iNbBikes);

    int color = Util.resolveResourceFromNumber(mSettings.getBikeColors(),
        iNbBikes);
    mStationView.setTextColor(R.id.widget_station_bikes,
        _mContext.getResources().getColor(color));

    // Return
    iNbBikes = mStation.isOpened() ? mStation.getAvailableBikeStands() : 0;
    mStationView.setTextViewText(R.id.widget_station_stands, "" + iNbBikes);

    color = Util.resolveResourceFromNumber(mSettings.getBikeColors(),
        iNbBikes);
    mStationView.setTextColor(R.id.widget_station_stands,
        _mContext.getResources().getColor(color));

    mStationView.setTextViewText(R.id.widget_station_title,
        mStation.getFriendlyName());

    return mStationView;
  }

  protected void _updateViews(Stations mStations) {
    if (_mContext == null) return;

    ComponentName mAppWidget = new ComponentName(
        _mContext.getPackageName(), getClass().getName());

    //which layout to show on widget
    RemoteViews remoteViews = new RemoteViews(_mContext.getPackageName(),
        R.layout.widget_favstations);
    remoteViews.removeAllViews(R.id.widget_main);

    Settings mSettings = Settings.getInstance(_mContext);
    ArrayList<Integer> aFavStations = mSettings.getFavStations();
    for(Integer i : aFavStations) {
      Station mStation = mStations.get(i);

      Intent mIntent = new Intent(_mContext, MapsActivity.class);
      mIntent.putExtra("fav_station", i);
      PendingIntent mPendingIntent = PendingIntent.getActivity(_mContext, 0,
          mIntent, 0);

      RemoteViews mStationView = _buildStationView(mStation);
      mStationView.setOnClickPendingIntent(R.id.widget_station_item, mPendingIntent);

      remoteViews.addView(R.id.widget_main, mStationView);
      /*Intent svcIntent = new Intent(context, Service.class);
      remoteViews.setRemoteAdapter(R.id.widget_stationslist, svcIntent);*/
    }

    AppWidgetManager mWdgMgr = AppWidgetManager.getInstance(_mContext);
    int aIds[] = mWdgMgr.getAppWidgetIds(mAppWidget);
    // Perform this loop procedure for each App Widget for this provider
    final int N = aIds.length;
    for (int i=0; i<N; i++) {
      int appWidgetId = aIds[i];

      // Tell AppWidgetManager to perform an update on the current app widget
      mWdgMgr.updateAppWidget(appWidgetId, remoteViews);
    }
  }

  private PendingIntent _createDataUpdateIntent(Context context) {
    Intent intent = new Intent(FAVSTATION_WIDGET_UPDATE);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT);
    return pendingIntent;
  }

  private boolean _downloadMarkers() {
    Log.i("Provider", "Entering _downloadMarkers()");
    if (_bDownloading) return false;
    _bDownloading = true;

    Settings mSettings = Settings.getInstance(_mContext);
    Intent intent = new Intent(_mContext, DownloadStationsService.class);

    intent.putExtra("receiver",
        (Parcelable) new DownloadStationsReceiver(new Handler()));
    intent.putExtra("url_full", mSettings.getURLDownloadFull());
    intent.putExtra("url_dynamic", mSettings.getURLDownloadDynamic());
    intent.putExtra("dl_static", mSettings.getStaticDeadLine());
    intent.putExtra("dl_dynamic", mSettings.getDynamicDeadLine());
    intent.putExtra("stations_file",
        mSettings.getStationsFile().getAbsolutePath());

    Log.i("Provider", "Starting download intent");
    _mContext.startService(intent);

    return true;
  }

  private class DownloadStationsReceiver extends ResultReceiver implements Serializable{
    /**
     * 
     */
    private static final long serialVersionUID = 8768846450342188871L;

    public DownloadStationsReceiver(Handler handler) {
      super(handler);
    }

    @Override
    protected void onReceiveResult(int resultCode, Bundle resultData) {
      super.onReceiveResult(resultCode, resultData);

      switch (resultCode) {
        case DownloadStationsService.SUCCESS:
          Log.i("Provider", "onReceiveResult SUCCESS");
          // date doesn't matter since it was just generated
          Stations mStations = (Stations) resultData.getSerializable("stations");

          _updateViews(mStations);
          _bDownloading = false;
        break;

        case DownloadStationsService.FAILURE_CONNECTION:
        case DownloadStationsService.FAILURE_GENERIC:
        case DownloadStationsService.FAILURE_PARSE:
          Log.i("Provider", "onReceiveResult FAILURE_Xxxxx");
        break;

        case DownloadStationsService.FINISHED:
          Log.i("Provider", "onReceiveResult FINISHED");
        break;
      }
    }
  }
}