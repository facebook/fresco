/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.util.Map;
import java.util.concurrent.Executor;

import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;

import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.bitmaps.SimpleBitmapReleaser;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * A producer that creates video thumbnails.
 *
 * <p>At present, these thumbnails are created on the java heap rather than being pinned
 * purgeables. This is deemed okay as the thumbnails are only very small.
 */
public class LocalVideoThumbnailProducer implements
    Producer<CloseableReference<CloseableImage>> {

  public static final String PRODUCER_NAME = "VideoThumbnailProducer";
  @VisibleForTesting static final String CREATED_THUMBNAIL = "createdThumbnail";

  private final Executor mExecutor;
  private final ContentResolver mContentResolver;

  public LocalVideoThumbnailProducer(Executor executor, ContentResolver contentResolver) {
    mExecutor = executor;
    mContentResolver = contentResolver;
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      final ProducerContext producerContext) {

    final ProducerListener listener = producerContext.getListener();
    final String requestId = producerContext.getId();
    final ImageRequest imageRequest = producerContext.getImageRequest();
    final StatefulProducerRunnable cancellableProducerRunnable =
        new StatefulProducerRunnable<CloseableReference<CloseableImage>>(
            consumer,
            listener,
            PRODUCER_NAME,
            requestId) {
          @Override
          protected void onSuccess(CloseableReference<CloseableImage> result) {
            super.onSuccess(result);
            listener.onUltimateProducerReached(requestId, PRODUCER_NAME, result != null);
          }

          @Override
          protected void onFailure(Exception e) {
            super.onFailure(e);
            listener.onUltimateProducerReached(requestId, PRODUCER_NAME, false);
          }

          @Override
          protected CloseableReference<CloseableImage> getResult() throws Exception {
            String path = getLocalFilePath(imageRequest);
            if (path == null) {
              return null;
            }
            Bitmap thumbnailBitmap = ThumbnailUtils.createVideoThumbnail(path,
                calculateKind(imageRequest));
            if (thumbnailBitmap == null) {
              return null;
            }

            return CloseableReference.<CloseableImage>of(
                new CloseableStaticBitmap(
                    thumbnailBitmap,
                    SimpleBitmapReleaser.getInstance(),
                    ImmutableQualityInfo.FULL_QUALITY,
                    0));
          }

          @Override
          protected Map<String, String> getExtraMapOnSuccess(
              final CloseableReference<CloseableImage> result) {
            return ImmutableMap.of(CREATED_THUMBNAIL, String.valueOf(result != null));
          }

          @Override
          protected void disposeResult(CloseableReference<CloseableImage> result) {
            CloseableReference.closeSafely(result);
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
      Cursor cursor = mContentResolver.query(uri, new String[] {MediaStore.Video.Media.DATA},
        null, null, null);
      try {
        if (cursor.moveToFirst()) {
          return cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
        }
      } finally {
        cursor.close();
      }
    }
    return null;
  }
}
