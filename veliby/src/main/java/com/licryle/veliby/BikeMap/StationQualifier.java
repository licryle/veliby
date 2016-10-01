package com.licryle.veliby.BikeMap;

import com.licryle.veliby.R;
import com.licryle.veliby.Settings;
import com.licryle.veliby.Util;

import java.util.Hashtable;

import com.licryle.POICityMap.helpers.POIQualifier;

/**
 * Created by licryle on 8/23/15.
 */
public class StationQualifier implements POIQualifier<BikeStation> {
  protected Settings _mSettings = null;

  public StationQualifier(Settings mSettings) {
    _mSettings = mSettings;
  }

  protected static Hashtable<Integer, Integer> _mBikeResources =
      new Hashtable<Integer, Integer>() {
        private static final long serialVersionUID = -276505145697466182L;
        {
          put(0, R.drawable.presence_invisible);
          put(2, R.drawable.presence_busy);
          put(4, R.drawable.presence_away);
          put(1000, R.drawable.presence_online);
        }
      };

  protected static Hashtable<Integer, Integer> _mBikeFavResources =
      new Hashtable<Integer, Integer>() {
        private static final long serialVersionUID = -276505145697466182L;
        {
          put(0, R.drawable.favorite_none);
          put(2, R.drawable.favorite_few);
          put(4, R.drawable.favorite_some);
          put(1000, R.drawable.favorite_plenty);
        }
      };

  public int getIcon(BikeStation mPOI) {
    int iIcon;
    if (! mPOI.isOpened()) {
      iIcon = R.drawable.presence_offline;
    } else {
      int iBikes = (_mSettings.isBikeFindMode()) ?
          mPOI.getAvailableBikes() :
          mPOI.getAvailableBikeStands();

      if (isFavorite(mPOI)) {
        iIcon = Util.resolveResourceFromNumber(_mBikeFavResources, iBikes);
      } else {
        iIcon = Util.resolveResourceFromNumber(_mBikeResources, iBikes);
      }
    }

    return iIcon;
  }

  public boolean isFavorite(BikeStation mPOI) {
    return _mSettings.isStationFavorite(mPOI.getId());
  }

  public boolean isFiltered(BikeStation mPOI) {
    return _mSettings.isFavStationsOnly() && ! isFavorite(mPOI);
  }
}
