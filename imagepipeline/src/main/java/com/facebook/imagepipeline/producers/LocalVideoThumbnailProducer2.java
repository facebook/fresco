/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteBufferOutputStream;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.BitmapPool;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * A producer that creates video thumbnails.
 *
 * <p> Those thumbnails will be transformed into EncodedImage and put into disk cache if need
 *
 */
public class LocalVideoThumbnailProducer2 implements
    Producer<EncodedImage> {

  public static final String PRODUCER_NAME = "VideoThumbnailProducer";
  @VisibleForTesting static final String CREATED_THUMBNAIL = "createdThumbnail";

  private final Executor mExecutor;
  private final ContentResolver mContentResolver;
  private final PooledByteBufferFactory mPooledByteBufferFactory;
  private final BitmapPool mBitmapPool;

  public LocalVideoThumbnailProducer2(
      PooledByteBufferFactory pooledByteBufferFactory,
      BitmapPool bitmapPool,
      Executor executor,
      ContentResolver contentResolver) {
    mPooledByteBufferFactory = pooledByteBufferFactory;
    mBitmapPool = bitmapPool;
    mExecutor = executor;
    mContentResolver = contentResolver;
  }

  @Override
  public void produceResults(
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext) {

    final ProducerListener listener = producerContext.getListener();
    final String requestId = producerContext.getId();
    final ImageRequest imageRequest = producerContext.getImageRequest();
    final StatefulProducerRunnable cancellableProducerRunnable =
        new StatefulProducerRunnable<EncodedImage>(
            consumer,
            listener,
            PRODUCER_NAME,
            requestId) {
          @Override
          protected void onSuccess(EncodedImage result) {
            super.onSuccess(result);
            listener.onUltimateProducerReached(requestId, PRODUCER_NAME, result != null);
          }

          @Override
          protected void onFailure(Exception e) {
            super.onFailure(e);
            listener.onUltimateProducerReached(requestId, PRODUCER_NAME, false);
          }

          @Override
          protected EncodedImage getResult() throws Exception {
            EncodedImage encodedImage = getEncodedImage(imageRequest);
            if (encodedImage == null) {
              listener.onUltimateProducerReached(requestId, PRODUCER_NAME, false);
              return null;
            }
            encodedImage.parseMetaData();
            listener.onUltimateProducerReached(requestId, PRODUCER_NAME, true);
            return encodedImage;
          }

          @Override
          protected Map<String, String> getExtraMapOnSuccess(
              final EncodedImage result) {
            return ImmutableMap.of(CREATED_THUMBNAIL, String.valueOf(result != null));
          }

          @Override
          protected void disposeResult(EncodedImage result) {
            EncodedImage.closeSafely(result);
          }
        };
    producerContext.addCallbacks(
        new BaseProducerContextCallbacks() {
          @Override
          public void onCancellationRequested() {
            cancellableProducerRunnable.cancel();
          }
        });
    mExecutor.execute(cancellableProducerRunnable);
  }


  protected EncodedImage getEncodedImage(ImageRequest imageRequest) {
    String path = getLocalFilePath(imageRequest);
    if (path == null) {
      return null;
    }
    Bitmap thumbnailBitmap = ThumbnailUtils.createVideoThumbnail(
        path,
        calculateKind(imageRequest));
    if (thumbnailBitmap == null) {
      return null;
    }

    PooledByteBufferOutputStream pooledOutputStream = mPooledByteBufferFactory.newOutputStream();
    try {
      thumbnailBitmap.compress(thumbnailBitmap.hasAlpha() ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG,
          100, pooledOutputStream);
      return getByteBufferBackedEncodedImage(pooledOutputStream);
    } finally {
      mBitmapPool.release(thumbnailBitmap);
      pooledOutputStream.close();
    }
  }

  private EncodedImage getByteBufferBackedEncodedImage(
      PooledByteBufferOutputStream pooledOutputStream) {
    CloseableReference<PooledByteBuffer> result = null;
    try {
      result = CloseableReference.of(pooledOutputStream.toByteBuffer());
      return new EncodedImage(result);
    } finally {
      CloseableReference.closeSafely(result);
    }
  }

  private static int calculateKind(ImageRequest imageRequest) {
    if (imageRequest.getPreferredWidth() > 96 || imageRequest.getPreferredHeight() > 96) {
      return MediaStore.Images.Thumbnails.MINI_KIND;
    }
    return MediaStore.Images.Thumbnails.MICRO_KIND;
  }

  @Nullable private String getLocalFilePath(ImageRequest imageRequest) {
    Uri uri = imageRequest.getSourceUri();
    if (UriUtil.isLocalFileUri(uri)) {
      return imageRequest.getSourceFile().getPath();
    } else if (UriUtil.isLocalContentUri(uri)) {
      String selection = null;
      String[] selectionArgs = null;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
          && "com.android.providers.media.documents".equals(uri.getAuthority())) {
        String documentId = DocumentsContract.getDocumentId(uri);
        uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        selection = MediaStore.Video.Media._ID + "=?";
        selectionArgs = new String[] {documentId.split(":")[1]};
      }
      Cursor cursor =
          mContentResolver.query(
              uri, new String[] {MediaStore.Video.Media.DATA}, selection, selectionArgs, null);
      try {
        if (cursor != null && cursor.moveToFirst()) {
          return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
        }
      } finally {
        if (cursor != null) {
          cursor.close();
        }
      }
    }
    return null;
  }
}
