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
import java.util.List;

public class Contracts extends Hashtable<Integer, Contract> {
  /**
   * 
   */
  private static final long serialVersionUID = -3698242295251745027L;

  protected Date _mLastUpdate = new Date(0);

  public Date getLastUpdate() { return _mLastUpdate; }
  public void setLastUpdate(Date mLastUpdate) { _mLastUpdate = mLastUpdate; }

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

  public Contract findContractById(int iId) {
    return this.get(iId);
  }

  public static Contracts loadFromFile(File mFile) {
    Contracts mContracts;

    try {
      FileInputStream mInput = new FileInputStream(mFile);
      ObjectInputStream mObjectStream = new ObjectInputStream(mInput);
      mContracts = (Contracts) mObjectStream.readObject();

      mObjectStream.close();
      return mContracts;
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

    return new Contracts();
  }

  public boolean saveToFile(File mFile) {
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
