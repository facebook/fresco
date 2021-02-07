/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.data.impl;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.ArrayRes;
import com.facebook.samples.scrollperf.data.SimpleAdapter;

/** This is the implementation of a SimpleAdapter which reads data from an array resource */
public class LocalResourceSimpleAdapter implements SimpleAdapter<Uri> {

  private Uri[] mUris;

  private final String[] mSrcArray;

  private final boolean mLazy;

  public static LocalResourceSimpleAdapter getLazyAdapter(
      final Context context, @ArrayRes int arrayId) {
    return new LocalResourceSimpleAdapter(context, arrayId, true);
  }

  public static LocalResourceSimpleAdapter getEagerAdapter(
      final Context context, @ArrayRes int arrayId) {
    return new LocalResourceSimpleAdapter(context, arrayId, false);
  }

  private LocalResourceSimpleAdapter(final Context context, @ArrayRes int arrayId, boolean lazy) {
    mSrcArray = context.getResources().getStringArray(arrayId);
    mLazy = lazy;
    mUris = new Uri[mSrcArray.length];
    if (!lazy) {
      for (int i = 0; i < mSrcArray.length; i++) {
        mUris[i] = Uri.parse(mSrcArray[i]);
      }
    }
  }

  @Override
  public int getSize() {
    return mSrcArray.length;
  }

  @Override
  public Uri get(int position) {
    if (mLazy && mUris[position] == null) {
      mUris[position] = Uri.parse(mSrcArray[position]);
    }
    return mUris[position];
  }

  @Override
  public boolean isLazy() {
    return mLazy;
  }
}
