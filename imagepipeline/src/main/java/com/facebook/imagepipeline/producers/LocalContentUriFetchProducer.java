/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/**
 * Represents a local content Uri fetch producer.
 */
public class LocalContentUriFetchProducer extends LocalFetchProducer {

  public static final String PRODUCER_NAME = "LocalContentUriFetchProducer";

  private static final String[] PROJECTION = new String[] {
      MediaStore.Images.Media._ID,
      MediaStore.Images.ImageColumns.DATA
  };

  private final ContentResolver mContentResolver;

  public LocalContentUriFetchProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      ContentResolver contentResolver) {
    super(executor, pooledByteBufferFactory);
    mContentResolver = contentResolver;
  }

  @Override
  protected EncodedImage getEncodedImage(ImageRequest imageRequest) throws IOException {
    Uri uri = imageRequest.getSourceUri();
    if (UriUtil.isLocalContactUri(uri)) {
      final InputStream inputStream;
      if (uri.toString().endsWith("/photo")) {
        inputStream =  mContentResolver.openInputStream(uri);
      } else if (uri.toString().endsWith("/display_photo")) {
        try {
          AssetFileDescriptor fd = mContentResolver.openAssetFileDescriptor(uri, "r");
          inputStream = fd.createInputStream();
        } catch (IOException e) {
          throw new IOException("Contact photo does not exist: " + uri);
        }
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
