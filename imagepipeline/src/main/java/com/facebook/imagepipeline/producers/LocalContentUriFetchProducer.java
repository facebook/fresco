/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.ContactsContract;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Represents a local content Uri fetch producer.
 */
public class LocalContentUriFetchProducer extends LocalFetchProducer {
  @VisibleForTesting static final String PRODUCER_NAME = "LocalContentUriFetchProducer";
  private static final String DISPLAY_PHOTO_PATH =
      Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "display_photo").getPath();

  private final ContentResolver mContentResolver;

  public LocalContentUriFetchProducer(
      Executor executor,
      PooledByteBufferFactory pooledByteBufferFactory,
      ContentResolver contentResolver) {
    super(executor, pooledByteBufferFactory);
    mContentResolver = contentResolver;
  }

  @Override
  protected InputStream getInputStream(ImageRequest imageRequest) throws IOException {
    Uri uri = imageRequest.getSourceUri();
    if (isContactUri(uri)) {
      // If a Contact URI is provided, use the special helper to open that contact's photo.
      return ContactsContract.Contacts.openContactPhotoInputStream(mContentResolver, uri);
    }
    return mContentResolver.openInputStream(uri);
  }

  /**
   * Checks if the given URI is a general Contact URI, and not a specific display photo.
   * @param uri the URI to check
   * @return true if the uri is a a Contact URI, and is not already specifying a display photo.
   */
  private boolean isContactUri(Uri uri) {
    return ContactsContract.AUTHORITY.equals(uri.getAuthority()) &&
        !uri.getPath().startsWith(DISPLAY_PHOTO_PATH);
  }

  @Override
  protected int getLength(ImageRequest imageRequest) {
    return -1;
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}
