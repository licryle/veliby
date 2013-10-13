package com.licryle.veliby;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import android.content.Context;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

public abstract class Util {
	public static String readFile( String file ) throws IOException {
    BufferedReader reader = new BufferedReader( new FileReader (file));
    String         line = null;
    StringBuilder  stringBuilder = new StringBuilder();
    String         ls = System.getProperty("line.separator");

    while( ( line = reader.readLine() ) != null ) {
        stringBuilder.append( line );
        stringBuilder.append( ls );
    }

    reader.close();
    return stringBuilder.toString();
  }

	public static int resolveResourceFromNumber(
			Hashtable<Integer, Integer> mResources, int iNumber) {
		if (mResources.size() == 0) {
			return -1;
		}

		Enumeration<Integer> enumInts = mResources.keys();
		List<Integer> threeSholds = Collections.list(enumInts);
		Collections.sort(threeSholds);

		int iResource;
		int iThreeshold;
		int i = 0;
		do {
			iThreeshold = threeSholds.get(i);
			iResource = mResources.get(iThreeshold);
			i++;
		} while (iNumber > iThreeshold);

    return iResource;
	}

	public static int intToUInt(int i, int iPow) {
		long iMax = (Double.valueOf(Math.pow(2, iPow))).longValue();

		return (int) ((i < 0) ? (iMax + i) : i);
	}

  public static boolean hasPlayServices(Context mContext) {
    int iStatus = GooglePlayServicesUtil.
        isGooglePlayServicesAvailable(mContext.getApplicationContext());
    if (iStatus != ConnectionResult.SUCCESS) {
      return false;
    }

    return true;
  }
}
