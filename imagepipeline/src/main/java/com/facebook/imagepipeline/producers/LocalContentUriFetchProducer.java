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
import android.os.ParcelFileDescriptor;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/** Represents a local content Uri fetch producer. */
public class LocalContentUriFetchProducer extends LocalFetchProducer {

  public static final String PRODUCER_NAME = "LocalContentUriFetchProducer";

  private static final String[] PROJECTION =
      new String[] {MediaStore.Images.Media._ID};

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
        inputStream = mContentResolver.openInputStream(uri);
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
      return getEncodedImage(inputStream, EncodedImage.UNKNOWN_STREAM_SIZE);
    }
    return getEncodedImage(mContentResolver.openInputStream(uri), EncodedImage.UNKNOWN_STREAM_SIZE);
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}
