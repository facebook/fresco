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
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.util.Size;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.media.MediaUtils;
import com.facebook.common.references.CloseableReference;
import com.facebook.fresco.middleware.HasExtraData;
import com.facebook.imagepipeline.bitmaps.SimpleBitmapReleaser;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

@RequiresApi(Build.VERSION_CODES.Q)
@Nullsafe(Nullsafe.Mode.LOCAL)
public class LocalThumbnailBitmapSdk29Producer
    implements Producer<CloseableReference<CloseableImage>> {

  public static final String PRODUCER_NAME = "LocalThumbnailBitmapSdk29Producer";
  @VisibleForTesting static final String CREATED_THUMBNAIL = "createdThumbnail";

  private final Executor mExecutor;
  private final ContentResolver mContentResolver;

  public LocalThumbnailBitmapSdk29Producer(Executor executor, ContentResolver contentResolver) {
    mExecutor = executor;
    mContentResolver = contentResolver;
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<CloseableImage>> consumer, final ProducerContext context) {

    final ProducerListener2 listener = context.getProducerListener();
    final ImageRequest imageRequest = context.getImageRequest();
    context.putOriginExtra("local", "thumbnail_bitmap");
    final CancellationSignal cancellationSignal = new CancellationSignal();
    final StatefulProducerRunnable<CloseableReference<CloseableImage>> cancellableProducerRunnable =
        new StatefulProducerRunnable<CloseableReference<CloseableImage>>(
            consumer, listener, context, PRODUCER_NAME) {
          @Override
          protected void onSuccess(@Nullable CloseableReference<CloseableImage> result) {
            super.onSuccess(result);
            listener.onUltimateProducerReached(context, PRODUCER_NAME, result != null);
            context.putOriginExtra("local", "thumbnail_bitmap");
          }

          @Override
          protected void onFailure(Exception e) {
            super.onFailure(e);
            listener.onUltimateProducerReached(context, PRODUCER_NAME, false);
            context.putOriginExtra("local", "thumbnail_bitmap");
          }

          @Override
          protected @Nullable CloseableReference<CloseableImage> getResult() throws IOException {
            Bitmap thumbnailBitmap = null;
            String path;
            Size size =
                new Size(imageRequest.getPreferredWidth(), imageRequest.getPreferredHeight());
            try {
              path = getLocalFilePath(imageRequest);
            } catch (IllegalArgumentException e) {
              path = null;
            }

            if (path != null) {
              thumbnailBitmap =
                  MediaUtils.isVideo(MediaUtils.extractMime(path))
                      ? ThumbnailUtils.createVideoThumbnail(
                          new File(path), size, cancellationSignal)
                      : ThumbnailUtils.createImageThumbnail(
                          new File(path), size, cancellationSignal);
            }

            if (thumbnailBitmap == null) {
              thumbnailBitmap =
                  mContentResolver.loadThumbnail(
                      imageRequest.getSourceUri(), size, cancellationSignal);
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
            context.putExtra(HasExtraData.KEY_IMAGE_FORMAT, "thumbnail");
            closeableStaticBitmap.putExtras(context.getExtras());
            return CloseableReference.<CloseableImage>of(closeableStaticBitmap);
          }

          @Override
          protected void onCancellation() {
            super.onCancellation();
            cancellationSignal.cancel();
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
    context.addCallbacks(
        new BaseProducerContextCallbacks() {
          @Override
          public void onCancellationRequested() {
            cancellableProducerRunnable.cancel();
          }
        });
    mExecutor.execute(cancellableProducerRunnable);
  }

  @Nullable
  private String getLocalFilePath(ImageRequest imageRequest) {
    Uri uri = imageRequest.getSourceUri();
    return getRealPathFromUri(mContentResolver, uri);
  }
}
