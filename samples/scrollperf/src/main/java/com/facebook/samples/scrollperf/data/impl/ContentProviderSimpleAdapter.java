/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.data.impl;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.samples.scrollperf.data.SimpleAdapter;

/** This is a SimpleAdapter which which uses a set of elements from a ContentProvider */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class ContentProviderSimpleAdapter implements SimpleAdapter<Uri> {

  private final Uri[] mUris;

  private ContentProviderSimpleAdapter(final Uri baseProvider, Context context) {
    String[] projection = {MediaStore.Images.Media._ID};
    Cursor cursor = context.getContentResolver().query(baseProvider, projection, null, null, null);
    // NULLSAFE_FIXME[Nullable Dereference]
    final int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
    // NULLSAFE_FIXME[Nullable Dereference]
    mUris = new Uri[cursor.getCount()];
    int i = 0;
    // NULLSAFE_FIXME[Nullable Dereference]
    while (cursor.moveToNext()) {
      // NULLSAFE_FIXME[Nullable Dereference]
      final String imageId = cursor.getString(columnIndex);
      mUris[i++] = Uri.withAppendedPath(baseProvider, imageId);
    }
    // NULLSAFE_FIXME[Nullable Dereference]
    cursor.close();
  }

  /**
   * Creates and returns a SimpleAdapter for Internal Photos
   *
   * @param context The Context
   * @return The SimpleAdapter for local photo
   */
  public static ContentProviderSimpleAdapter getInternalPhotoSimpleAdapter(Context context) {
    return new ContentProviderSimpleAdapter(MediaStore.Images.Media.INTERNAL_CONTENT_URI, context);
  }

  /**
   * Creates and returns a SimpleAdapter for External Photos
   *
   * @param context The Context
   * @return The SimpleAdapter for local photo
   */
  public static ContentProviderSimpleAdapter getExternalPhotoSimpleAdapter(Context context) {
    return new ContentProviderSimpleAdapter(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, context);
  }

  @Override
  public int getSize() {
    return mUris.length;
  }

  @Override
  public Uri get(int position) {
    return mUris[position];
  }

  @Override
  public boolean isLazy() {
    return false;
  }
}
