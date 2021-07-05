/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/** Represents a local content Uri fetch producer. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LocalContentUriFetchProducer extends LocalFetchProducer {

  public static final String PRODUCER_NAME = "LocalContentUriFetchProducer";

  private static final String[] PROJECTION =
      new String[] {MediaStore.Images.Media._ID, MediaStore.Images.ImageColumns.DATA};

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
          Preconditions.checkNotNull(fd);
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
      Preconditions.checkNotNull(inputStream);
      // If a Contact URI is provided, use the special helper to open that contact's photo.
      return getEncodedImage(inputStream, EncodedImage.UNKNOWN_STREAM_SIZE);
    }

    if (UriUtil.isLocalCameraUri(uri)) {
      EncodedImage cameraImage = getCameraImage(uri);
      if (cameraImage != null) {
        return cameraImage;
      }
    }

    return getEncodedImage(
        Preconditions.checkNotNull(mContentResolver.openInputStream(uri)),
        EncodedImage.UNKNOWN_STREAM_SIZE);
  }

  private @Nullable EncodedImage getCameraImage(Uri uri) throws IOException {
    try {
      ParcelFileDescriptor parcelFileDescriptor = mContentResolver.openFileDescriptor(uri, "r");
      Preconditions.checkNotNull(parcelFileDescriptor);
      FileDescriptor fd = parcelFileDescriptor.getFileDescriptor();
      return getEncodedImage(new FileInputStream(fd), (int) parcelFileDescriptor.getStatSize());
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }
}
