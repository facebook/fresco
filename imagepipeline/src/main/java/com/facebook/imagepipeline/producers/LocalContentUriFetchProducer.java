/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import javax.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Represents a local content Uri fetch producer.
 */
public class LocalContentUriFetchProducer extends LocalFetchProducer {

  @VisibleForTesting static final String PRODUCER_NAME = "LocalContentUriFetchProducer";
  private static final String[] PROJECTION = new String[] {
      MediaStore.Images.Media._ID,
      MediaStore.Images.ImageColumns.DATA
  };

  private final ContentResolver mContentResolver;

  public LocalContentUriFetchProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      ContentResolver contentResolver,
      boolean decodeFileDescriptorEnabled) {
    super(executor, pooledByteBufferFactory,decodeFileDescriptorEnabled);
    mContentResolver = contentResolver;
  }

  @Override
  protected EncodedImage getEncodedImage(ImageRequest imageRequest) throws IOException {
    Uri uri = imageRequest.getSourceUri();
    if (UriUtil.isLocalContactUri(uri)) {
      final InputStream inputStream;
      if (uri.toString().endsWith("/photo")) {
        inputStream =  mContentResolver.openInputStream(uri);
      } else {
        inputStream = ContactsContract.Contacts.openContactPhotoInputStream(mContentResolver, uri);
        if (inputStream == null) {
          throw new IOException("Contact photo does not exist: " + uri);
        }
      }
      // If a Contact URI is provided, use the special helper to open that contact's photo.
      return getEncodedImage(
          inputStream,
          EncodedImage.UNKNOWN_STREAM_SIZE);
    }

    if (UriUtil.isLocalCameraUri(uri)) {
      EncodedImage cameraImage = getCameraImage(uri);
      if (cameraImage != null) {
        return cameraImage;
      }
    }

    return getEncodedImage(
        mContentResolver.openInputStream(uri),
        EncodedImage.UNKNOWN_STREAM_SIZE);
  }

  private @Nullable EncodedImage getCameraImage(Uri uri) throws IOException {
    Cursor cursor = mContentResolver.query(uri, PROJECTION, null, null, null);
    if (cursor == null) {
      return null;
    }
    try {
      if (cursor.getCount() == 0) {
        return null;
      }
      cursor.moveToFirst();
      final String pathname =
          cursor.getString(cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA));
      if (pathname != null) {
        return getEncodedImage(new FileInputStream(pathname), getLength(pathname));
      }
    } finally {
      cursor.close();
    }
    return null;
  }

  private static int getLength(String pathname) {
    return pathname == null ? -1 : (int) new File(pathname).length();
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}
