/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static com.facebook.common.util.UriUtil.getRealPathFromUri;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.middleware.HasExtraData;
import com.facebook.imagepipeline.bitmaps.SimpleBitmapReleaser;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

/**
 * A producer that creates video thumbnails.
 *
 * <p>At present, these thumbnails are created on the java heap rather than being pinned purgeables.
 * This is deemed okay as the thumbnails are only very small.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LocalVideoThumbnailProducer implements Producer<CloseableReference<CloseableImage>> {

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

    final ProducerListener2 listener = producerContext.getProducerListener();
    final ImageRequest imageRequest = producerContext.getImageRequest();
    producerContext.putOriginExtra("local", "video");
    final StatefulProducerRunnable<CloseableReference<CloseableImage>> cancellableProducerRunnable =
        new StatefulProducerRunnable<CloseableReference<CloseableImage>>(
            consumer, listener, producerContext, PRODUCER_NAME) {
          @Override
          protected void onSuccess(@Nullable CloseableReference<CloseableImage> result) {
            super.onSuccess(result);
            listener.onUltimateProducerReached(producerContext, PRODUCER_NAME, result != null);
            producerContext.putOriginExtra("local", "video");
          }

          @Override
          protected void onFailure(Exception e) {
            super.onFailure(e);
            listener.onUltimateProducerReached(producerContext, PRODUCER_NAME, false);
            producerContext.putOriginExtra("local", "video");
          }

          @Override
          protected @Nullable CloseableReference<CloseableImage> getResult() throws Exception {
            Bitmap thumbnailBitmap = null;
            String path;
            try {
              path = getLocalFilePath(imageRequest);
            } catch (IllegalArgumentException e) {
              path = null;
            }

            if (path != null) {
              thumbnailBitmap =
                  ThumbnailUtils.createVideoThumbnail(path, calculateKind(imageRequest));
            }

            if (thumbnailBitmap == null) {
              thumbnailBitmap =
                  createThumbnailFromContentProvider(mContentResolver, imageRequest.getSourceUri());
            }

            if (thumbnailBitmap == null) {
              return null;
            }

            CloseableStaticBitmap closeableStaticBitmap =
                CloseableStaticBitmap.of(
                    thumbnailBitmap,
                    SimpleBitmapReleaser.getInstance(),
                    ImmutableQualityInfo.FULL_QUALITY,
                    0);
            producerContext.putExtra(HasExtraData.KEY_IMAGE_FORMAT, "thumbnail");
            closeableStaticBitmap.putExtras(producerContext.getExtras());
            return CloseableReference.<CloseableImage>of(closeableStaticBitmap);
          }

          @Override
          protected Map<String, String> getExtraMapOnSuccess(
              final @Nullable CloseableReference<CloseableImage> result) {
            return ImmutableMap.of(CREATED_THUMBNAIL, String.valueOf(result != null));
          }

          @Override
          protected void disposeResult(@Nullable CloseableReference<CloseableImage> result) {
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

  @Nullable
  private String getLocalFilePath(ImageRequest imageRequest) {
    Uri uri = imageRequest.getSourceUri();
    return getRealPathFromUri(mContentResolver, uri);
  }

  @Nullable
  private static Bitmap createThumbnailFromContentProvider(
      ContentResolver contentResolver, Uri uri) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1) {
      MediaMetadataRetriever mediaMetadataRetriever = null;
      try {
        ParcelFileDescriptor videoFile = contentResolver.openFileDescriptor(uri, "r");
        Preconditions.checkNotNull(videoFile);
        mediaMetadataRetriever = new MediaMetadataRetriever();
        mediaMetadataRetriever.setDataSource(videoFile.getFileDescriptor());
        return mediaMetadataRetriever.getFrameAtTime(-1);
      } catch (FileNotFoundException e) {
        return null;
      } finally {
        if (mediaMetadataRetriever != null) {
          try {
            mediaMetadataRetriever.release();
          } catch (IOException ex) {
            // Nothing to do.
          }
        }
      }
    } else {
      return null;
    }
  }
}
