/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;

import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
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
  @VisibleForTesting static final String PRODUCER_NAME = "LocalContentUriFetchProducer";
  private static final String DISPLAY_PHOTO_PATH =
      Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "display_photo").getPath();
  private static final String[] PROJECTION = new String[] { MediaStore.Images.ImageColumns.DATA };

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
    if (isContactUri(uri)) {
      // If a Contact URI is provided, use the special helper to open that contact's photo.
      return getByteBufferBackedEncodedImage(
          ContactsContract.Contacts.openContactPhotoInputStream(mContentResolver, uri),
          getLength(imageRequest));
    }

    if (isCameraUri(uri)) {
      final String pathname = getCameraPath(uri);
      if (pathname != null) {
        Supplier<FileInputStream> sup = new Supplier<FileInputStream>() {
          @Override
          public FileInputStream get() {
            try {
              return new FileInputStream(pathname);
            } catch (IOException ioe) {
              throw new RuntimeException(ioe);
            }
          }
        };
        return new EncodedImage(sup, getLength(imageRequest));
      }
    }

    return getByteBufferBackedEncodedImage(
        mContentResolver.openInputStream(uri),
        getLength(imageRequest));
  }

  /**
   * Checks if the given URI is a general Contact URI, and not a specific display photo.
   * @param uri the URI to check
   * @return true if the uri is a a Contact URI, and is not already specifying a display photo.
   */
  private static boolean isContactUri(Uri uri) {
    return ContactsContract.AUTHORITY.equals(uri.getAuthority()) &&
        !uri.getPath().startsWith(DISPLAY_PHOTO_PATH);
  }

  private static boolean isCameraUri(Uri uri) {
    String uriString = uri.toString();
    return uriString.startsWith(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString()) ||
        uriString.startsWith(MediaStore.Images.Media.INTERNAL_CONTENT_URI.toString());
  }

  private @Nullable String getCameraPath(Uri uri) {
    Cursor cursor = mContentResolver.query(uri, PROJECTION, null, null, null);
    if (cursor == null) {
      return null;
    }
    try {
      cursor.moveToFirst();
      return cursor.getString(0);
    } finally {
      cursor.close();
    }
  }

  private int getLength(ImageRequest imageRequest) {
    Uri uri = imageRequest.getSourceUri();
    if (isCameraUri(uri)) {
      String pathname = getCameraPath(uri);
      return pathname == null ? -1 : (int) new File(pathname).length();
    } else {
      return -1;
    }
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}
