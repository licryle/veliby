package com.licryle.veliby.UI;

import android.view.View;

public interface OnSwipeListener {
  public void onSwipeRight(View mOrigin);
  public void onSwipeLeft(View mOrigin);
  public void onSwipeUp(View mOrigin);
  public void onSwipeDown(View mOrigin);
}