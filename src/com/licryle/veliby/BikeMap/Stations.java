package com.licryle.veliby.BikeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

public class Stations extends Hashtable<Integer, Station> {
  /**
   * 
   */
  private static final long serialVersionUID = -2878489681537529799L;

  protected Date _mLastUpdate;

  public Date getLastUpdate() { return _mLastUpdate; }
  public void setLastUpdate(Date mLastUpdate) { _mLastUpdate = mLastUpdate; }

  public boolean isDynamicExpired(int iMinutes) {
    return isOverTime(Calendar.MINUTE, iMinutes);
  }

  public boolean isStaticExpired(int iDays) {
    return isOverTime(Calendar.DATE, iDays);
  }

  protected boolean isOverTime(int iType, int iLength) {
    Date mLastModified = getLastUpdate();
    if (mLastModified == null) return true;

    Calendar mDeadline = Calendar.getInstance();
    mDeadline.add(iType, -iLength);

    return mLastModified.before(mDeadline.getTime());    
  }

  public void removeDynamicData() {
    Iterator<Map.Entry<Integer, Station>> it = this.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<Integer, Station> entry = it.next();
      Station mStation = entry.getValue();
      mStation.removeDynamicData();
    }
  }

  public static Stations loadStationsInfo(File mFile, int iDeadLine) {
    Stations mStations;

    try {
      FileInputStream mInput = new FileInputStream(mFile);
      ObjectInputStream mObjectStream = new ObjectInputStream(mInput);
      mStations = (Stations) mObjectStream.readObject();

      if (mStations.isDynamicExpired(iDeadLine)) {
        mStations.removeDynamicData();
      }

      mObjectStream.close();
      return mStations;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (StreamCorruptedException e) {
      mFile.delete();
      e.printStackTrace();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ClassNotFoundException e) {
      // We changed version, let's delete that file
      mFile.delete();
      e.printStackTrace();
    } catch (Exception e) {
      // In doubt, call the shots
      mFile.delete();
      e.printStackTrace();
    }

    return new Stations();
  }

  public boolean saveStationsInfo(File mFile) {
    mFile.delete();
    FileOutputStream mOutput;
    try {
      mOutput = new FileOutputStream(mFile);
      ObjectOutputStream mObjectStream = new ObjectOutputStream(mOutput);

      mObjectStream.writeObject(this);
      mObjectStream.close();
      return true;
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
      mFile.delete(); // TODO: this isn't reliable
    }
 
    return false;
  }
}
