package com.licryle.veliby.UI;

import java.util.ArrayList;

import android.content.Context;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class SwipeTouchDectector implements OnTouchListener {
  protected ArrayList<OnSwipeListener> _aListeners;
  protected final GestureDetector _mGestureDetector;
  protected View _mCurrentView = null;

  public SwipeTouchDectector(Context mContext, int iSwipeThreshold,
      int iSwipeVelovity) {
    _mGestureDetector = new GestureDetector(mContext, new GestureListener(
        iSwipeThreshold, iSwipeVelovity));
  }

  public void addListener(OnSwipeListener mListener) {
    _aListeners.add(mListener);
  }

  public boolean onTouch(final View view, final MotionEvent motionEvent) {
    _mCurrentView = view;
    return _mGestureDetector.onTouchEvent(motionEvent);
  }

  protected class GestureListener extends SimpleOnGestureListener {
    protected int _iSwipe_Threshold;
    protected int _iSwipeVelocityThreshold;
  
    public GestureListener(int iSwipeThreshold, int iSwipeVelovity) {
      _aListeners = new ArrayList<OnSwipeListener>();
      _iSwipe_Threshold = iSwipeThreshold;
      _iSwipeVelocityThreshold = iSwipeVelovity;
    }
  
    @Override
    public boolean onDown(MotionEvent e) {
      return true;
    }
  
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
      boolean result = false;
      try {
        float diffY = e2.getY() - e1.getY();
        float diffX = e2.getX() - e1.getX();
        if (Math.abs(diffX) > Math.abs(diffY)) {
          if (Math.abs(diffX) > _iSwipe_Threshold && Math.abs(velocityX) > _iSwipeVelocityThreshold) {
            if (diffX > 0) {
              dispathOnSwipeRight();
            } else {
              dispathOnSwipeLeft();
            }
          }
        } else {
          if (Math.abs(diffY) > _iSwipe_Threshold && Math.abs(velocityY) > _iSwipeVelocityThreshold) {
            if (diffY > 0) {
              dispathOnSwipeDown();
            } else {
              dispathOnSwipeUp();
            }
          }
        }
      } catch (Exception exception) {
        exception.printStackTrace();
      }
      return result;
    }
  }

  protected void dispathOnSwipeRight() {
    for(OnSwipeListener mGL : _aListeners) {
      mGL.onSwipeRight(_mCurrentView);
    }
  }

  protected void dispathOnSwipeLeft() {
    for(OnSwipeListener mGL : _aListeners) {
      mGL.onSwipeLeft(_mCurrentView);
    }
  }

  protected void dispathOnSwipeUp() {
    for(OnSwipeListener mGL : _aListeners) {
      mGL.onSwipeUp(_mCurrentView);
    }
  }

  protected void dispathOnSwipeDown() {
    for(OnSwipeListener mGL : _aListeners) {
      mGL.onSwipeDown(_mCurrentView);
    }
  }
}