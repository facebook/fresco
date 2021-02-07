/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.misc;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.facebook.common.internal.Supplier;
import com.facebook.fresco.samples.showcase.settings.SettingsFragment;

public class DebugOverlaySupplierSingleton implements Supplier<Boolean> {

  private static final String KEY_DEBUG_OVERLAY = SettingsFragment.KEY_DEBUG_OVERLAY;

  private static DebugOverlaySupplierSingleton sInstance;

  private final SharedPreferences mSharedPreferences;

  private DebugOverlaySupplierSingleton(Context context) {
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public static DebugOverlaySupplierSingleton getInstance(Context context) {
    synchronized (DebugOverlaySupplierSingleton.class) {
      if (sInstance == null) {
        sInstance = new DebugOverlaySupplierSingleton(context.getApplicationContext());
      }
      return sInstance;
    }
  }

  @Override
  public Boolean get() {
    return mSharedPreferences.getBoolean(KEY_DEBUG_OVERLAY, false);
  }
}
