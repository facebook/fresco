package com.facebook.fresco.samples.showcase.imageformat.webplist.controller;

import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;

/**
 * The Switch to change Normal/Drop
 * Normal : the webp playback is more complete, but it may too cause stuck.
 * Drop : the playback may drop some frame, but no stuck.
 */
public class SwitchController implements CompoundButton.OnCheckedChangeListener {

  private boolean mEnable;
  private OnEnableChangeListener mChangeListener;

  public SwitchController(SwitchCompat switchCompat) {
    switchCompat.setOnCheckedChangeListener(this);
  }

  public void setDropChangeListener(OnEnableChangeListener listener) {
    mChangeListener = listener;
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    if (mEnable != isChecked && mChangeListener != null) {
      mChangeListener.onDropChanged(isChecked);
    }
    mEnable = isChecked;
  }

  public interface OnEnableChangeListener {
    void onDropChanged(boolean enableDropped);
  }
}
