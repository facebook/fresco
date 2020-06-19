/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.graphics.drawable.Drawable;
import androidx.annotation.Nullable;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.TransformAwareDrawable;
import com.facebook.drawee.drawable.TransformCallback;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import java.io.Closeable;
import javax.annotation.Nonnull;

public abstract class FrescoDrawable2 extends BaseFrescoDrawable
    implements Drawable.Callback,
        TransformCallback,
        TransformAwareDrawable,
        Closeable,
        DeferredReleaser.Releasable,
        DataSubscriber<CloseableReference<CloseableImage>> {

  @Override
  @Nullable
  public Drawable setImage(
      @Nullable Drawable imageDrawable,
      @Nullable CloseableReference<CloseableImage> imageReference) {
    return super.setImage(imageDrawable, imageReference);
  }

  public abstract ScaleTypeDrawable getActualImageWrapper();

  public abstract void setDataSource(
      @Nullable DataSource<CloseableReference<CloseableImage>> dataSource);

  public abstract void setFetchSubmitted(boolean fetchSubmitted);

  public abstract boolean isFetchSubmitted();

  public abstract void setDrawableDataSubscriber(
      @Nullable DrawableDataSubscriber drawableDataSubscriber);

  @Nullable
  public abstract DrawableDataSubscriber getDrawableDataSubscriber();

  public abstract void setImageRequest(@Nullable VitoImageRequest imageRequest);

  public abstract void setCallerContext(@Nullable Object callerContext);

  @Nullable
  public abstract Object getCallerContext();

  public abstract void setImageListener(@Nullable ImageListener imageListener);

  public abstract void setVitoImageRequestListener(@Nullable VitoImageRequestListener listener);

  public abstract CombinedImageListener getImageListener();

  public abstract RequestListener getImageOriginListener();

  @Nullable
  public abstract VitoImageRequest getImageRequest();

  public abstract void setImageId(long imageId);

  public abstract long getImageId();

  public abstract void setImageOrigin(@ImageOrigin int imageOrigin);

  @ImageOrigin
  public abstract int getImageOrigin();

  @Override
  public abstract void release();

  public abstract void scheduleReleaseDelayed();

  public abstract void cancelReleaseDelayed();

  public abstract void scheduleReleaseNextFrame();

  public abstract void cancelReleaseNextFrame();

  @Override
  public abstract void onNewResult(
      @Nonnull DataSource<CloseableReference<CloseableImage>> dataSource);

  @Override
  public abstract void onFailure(
      @Nonnull DataSource<CloseableReference<CloseableImage>> dataSource);

  @Override
  public abstract void onCancellation(
      @Nonnull DataSource<CloseableReference<CloseableImage>> dataSource);

  @Override
  public abstract void onProgressUpdate(
      @Nonnull DataSource<CloseableReference<CloseableImage>> dataSource);

  public abstract @Nullable Object getExtras();

  public abstract void setExtras(@Nullable Object extras);
}
