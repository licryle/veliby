package com.licryle.veliby.UI;

import java.util.Hashtable;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;
import com.licryle.veliby.R;
import com.licryle.veliby.Station;
import com.licryle.veliby.Util;

public class Maps_InfoWindowAdapter implements InfoWindowAdapter {
	private View mInfoView;
	protected Activity mActivity;
	protected Hashtable<Integer, Station> mStations;

	protected static Hashtable<Integer, Integer> mBikeResources = 
			new Hashtable<Integer, Integer>() {
        private static final long serialVersionUID = -6956564905991202734L;
				{
					put(0,R.color.infoview_nobike);
					put(2,R.color.infoview_fewbikes);
					put(4,R.color.infoview_somebikes);
					put(1000,R.color.infoview_plentybikes);
				}
			};

	public Maps_InfoWindowAdapter(Activity mActivity,
			Hashtable<Integer, Station> mStations) {
		this.mActivity = mActivity;
		this.mStations = mStations;

    mInfoView = mActivity.getLayoutInflater().inflate(R.layout.map_infoview,
			  null);
	}

	@Override
  public View getInfoContents(Marker marker) {
		String sStationId = marker.getSnippet();
		Station mStation = mStations.get(Integer.valueOf(sStationId));

		// Title
		TextView mTitle = (TextView) mInfoView.findViewById(R.id.infoview_title);
		mTitle.setText(mStation.getName());

		// Take
		int iNbBikes = mStation.isOpened() ? mStation.getAvailableBikes() : 0;

		TextView mBikes = (TextView) mInfoView.findViewById(R.id.infoview_bikes);
		mBikes.setText("" + iNbBikes);

		int color = Util.resolveResourceFromNumber(mBikeResources, iNbBikes);
		mBikes.setTextColor(mActivity.getResources().getColor(color));

		// Return
	  iNbBikes = mStation.isOpened() ? mStation.getAvailableBikeStands() : 0;

		mBikes = (TextView) mInfoView.findViewById(R.id.infoview_stands);
		mBikes.setText("" + iNbBikes);

		color = Util.resolveResourceFromNumber(mBikeResources, iNbBikes);
		mBikes.setTextColor(mActivity.getResources().getColor(color));

		// Is it a bonus station?
		int iVisibility = mStation.hasBonus() ? View.VISIBLE : View.GONE;
		mInfoView.findViewById(R.id.infoview_row_bonus).
				setVisibility(iVisibility);

		// Does the station has a bank?
		iVisibility = mStation.hasBanking() ? View.VISIBLE : View.GONE;
		mInfoView.findViewById(R.id.infoview_row_bank).
				setVisibility(iVisibility);

		// Is the station closed?
		iVisibility = mStation.isOpened() ? View.GONE : View.VISIBLE;
		mInfoView.findViewById(R.id.infoview_stationclosed).
				setVisibility(iVisibility);
		return mInfoView;
  }

	@Override
  public View getInfoWindow(Marker marker) {
    // TODO Auto-generated method stub
    return null;
  }
}
