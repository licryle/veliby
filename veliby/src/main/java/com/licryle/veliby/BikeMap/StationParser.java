package com.licryle.veliby.BikeMap;

import android.util.Log;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;

import com.licryle.POICityMap.datastructure.City;
import com.licryle.POICityMap.datastructure.POI;
import com.licryle.POICityMap.datastructure.POIList;
import com.licryle.POICityMap.helpers.POIParser;
import com.licryle.POICityMap.helpers.Util;

/**
 * Created by licryle on 8/23/15.
 */
public class StationParser implements POIParser<BikeStation> {
  @Override
  public void parsePOIListFullData(ByteArrayOutputStream mInput,
                                   POIList mOutput, City mCity)
      throws Exception {
    String sInput = new String(mInput.toByteArray());
    JSONArray mJSon = new JSONArray(sInput);

    POIList mNewPOIList = new POIList();
    for (int i=0; i < mJSon.length(); i++) {
      try {
        POI mStation = new BikeStation(mCity, mJSon.getJSONObject(i));
        mNewPOIList.put(mStation.getId(), mStation);
      } catch (Exception e) {
        Log.i("POIListInfoService", "1 POI rejected, JSON invalid. " +
            e.getMessage());
      }
    }

    mOutput.copyInto(mNewPOIList);
  }

  @Override
  public void parsePOIListDynamicData(ByteArrayOutputStream mInput,
                                      POIList mOutput, City mCity)
      throws Exception {
    {
      if (mInput.size() % 5 != 0)
        throw new Exception("Not rounded dynamic data");

      byte aData[] = mInput.toByteArray();
      int i = 0;

      while (i < mInput.size()) {
        int iNumber = Util.intToUInt((new Byte(aData[i++])).intValue(), 8) +
            Util.intToUInt((new Byte(aData[i++])).intValue() << 8, 16) +
            Util.intToUInt((new Byte(aData[i++])).intValue() << 16, 24);

        BikeStation mStationToUp = (BikeStation) mOutput.get(
            BikeStation.generateId(mCity.getId(), iNumber));

        if (mStationToUp != null) {
          int iAvBikes      = (new Byte(aData[i++])).intValue();
          int iAvBikeStands = (new Byte(aData[i++])).intValue();
          boolean bOpened   = (iAvBikes > 0) || (iAvBikeStands > 0);

          mStationToUp.update(bOpened, iAvBikes, iAvBikeStands);
        } else {
          i = i +2;
        }
      }
    }
  }
}
