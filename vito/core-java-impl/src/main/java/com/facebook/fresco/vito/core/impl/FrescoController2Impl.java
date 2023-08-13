/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.facebook.common.callercontext.ContextChain;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Supplier;
import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.fresco.middleware.HasExtraData;
import com.facebook.fresco.middleware.MiddlewareUtils;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.ui.common.ControllerListener2.Extras;
import com.facebook.fresco.ui.common.ImagePerfDataListener;
import com.facebook.fresco.ui.common.ImagePerfDataNotifier;
import com.facebook.fresco.ui.common.ImagePerfNotifier;
import com.facebook.fresco.ui.common.OnFadeListener;
import com.facebook.fresco.ui.common.VitoUtils;
import com.facebook.fresco.vito.core.FrescoController2;
import com.facebook.fresco.vito.core.FrescoDrawableInterface;
import com.facebook.fresco.vito.core.FrescoVitoConfig;
import com.facebook.fresco.vito.core.NopDrawable;
import com.facebook.fresco.vito.core.VitoImagePerfListener;
import com.facebook.fresco.vito.core.VitoImagePipeline;
import com.facebook.fresco.vito.core.VitoImageRequest;
import com.facebook.fresco.vito.core.VitoImageRequestListener;
import com.facebook.fresco.vito.core.impl.debug.DebugOverlayFactory2;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.source.BitmapImageSource;
import com.facebook.fresco.vito.source.DrawableImageSource;
import com.facebook.fresco.vito.source.EmptyImageSource;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.image.ImageInfoImpl;
import com.facebook.imagepipeline.image.ImmutableQualityInfo;
import com.facebook.infer.annotation.Nullsafe;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class FrescoController2Impl implements DrawableDataSubscriber, FrescoController2 {

  private static final Map<String, Object> COMPONENT_EXTRAS =
      ImmutableMap.<String, Object>of("component_tag", "vito2");
  private static final Map<String, Object> SHORTCUT_EXTRAS =
      ImmutableMap.<String, Object>of("origin", "memory_bitmap", "origin_sub", "shortcut");
  private static final String TAG = "FrescoController2Impl";

  private final FrescoVitoConfig mConfig;
  private final Hierarcher mHierarcher;
  private final Executor mLightweightBackgroundThreadExecutor;
  private final Executor mUiThreadExecutor;
  private final VitoImagePipeline mImagePipeline;
  private final @Nullable VitoImageRequestListener mGlobalImageListener;
  private final DebugOverlayFactory2 mDebugOverlayFactory;
  private final @Nullable Supplier<ControllerListener2<ImageInfo>> mImagePerfListenerSupplier;
  private final VitoImagePerfListener mVitoImagePerfListener;

  private static final long EMPTY_IMAGE_ID = Long.MAX_VALUE;

  public FrescoController2Impl(
      FrescoVitoConfig config,
      Hierarcher hierarcher,
      Executor lightweightBackgroundThreadExecutor,
      Executor uiThreadExecutor,
      VitoImagePipeline imagePipeline,
      @Nullable VitoImageRequestListener globalImageListener,
      DebugOverlayFactory2 debugOverlayFactory,
      @Nullable Supplier<ControllerListener2<ImageInfo>> imagePerfListenerSupplier,
      VitoImagePerfListener vitoImagePerfListener) {
    mConfig = config;
    mHierarcher = hierarcher;
    mLightweightBackgroundThreadExecutor = lightweightBackgroundThreadExecutor;
    mUiThreadExecutor = uiThreadExecutor;
    mImagePipeline = imagePipeline;
    mGlobalImageListener = globalImageListener;
    mDebugOverlayFactory = debugOverlayFactory;
    mImagePerfListenerSupplier = imagePerfListenerSupplier;
    mVitoImagePerfListener = vitoImagePerfListener;
  }

  @Override
  public FrescoDrawable2 createDrawable() {
    return new FrescoDrawable2Impl(
        mConfig.useNewReleaseCallback(),
        mImagePerfListenerSupplier == null ? null : mImagePerfListenerSupplier.get(),
        mVitoImagePerfListener);
  }

  @Override
  public boolean fetch(
      final FrescoDrawableInterface drawable,
      final VitoImageRequest imageRequest,
      final @Nullable Object callerContext,
      final @Nullable ContextChain contextChain,
      final @Nullable ImageListener listener,
      final @Nullable ImagePerfDataListener perfDataListener,
      final @Nullable OnFadeListener onFadeListener,
      final @Nullable Rect viewportDimensions) {
    if (!(drawable instanceof FrescoDrawable2Impl)) {
      FLog.e(TAG, "Drawable not supported " + drawable);
      return false;
    }
    final FrescoDrawable2Impl frescoDrawable = (FrescoDrawable2Impl) drawable;

    // Fast path for null-URIs
    if (mConfig.fastPathForEmptyRequests()
        && imageRequest.imageSource instanceof EmptyImageSource) {
      emptyRequestFastPath(frescoDrawable, imageRequest, callerContext);
      return true;
    }

    // Save viewport dimension for future use
    frescoDrawable.setViewportDimensions(viewportDimensions);

    // Check if we already fetched the image
    if (isAlreadyLoadingImage(frescoDrawable, imageRequest)) {
      frescoDrawable.cancelReleaseNextFrame();
      frescoDrawable.cancelReleaseDelayed();
      return true; // already set
    }

    if (frescoDrawable.isFetchSubmitted()) {
      frescoDrawable.getImagePerfListener().onDrawableReconfigured(frescoDrawable);
    }
    // We didn't -> Reset everything
    frescoDrawable.close();
    // Basic setup
    frescoDrawable.setDrawableDataSubscriber(this);
    frescoDrawable.setImageRequest(imageRequest);
    frescoDrawable.setCallerContext(callerContext);
    frescoDrawable.setImageListener(listener);

    frescoDrawable.setVitoImageRequestListener(mGlobalImageListener);

    // Setup local perf data listener
    if (perfDataListener != null) {
      ImagePerfNotifier localPerfStateListener = new ImagePerfDataNotifier(perfDataListener);
      frescoDrawable.getInternalListener().setLocalImagePerfStateListener(localPerfStateListener);
    } else {
      frescoDrawable.getInternalListener().setLocalImagePerfStateListener(null);
    }

    frescoDrawable.setOnFadeListener(onFadeListener);

    // Set layers that are always visible
    frescoDrawable.setOverlayDrawable(
        mHierarcher.buildOverlayDrawable(imageRequest.resources, imageRequest.imageOptions));

    // We're fetching a new image, so we're updating the ID
    final long imageId = VitoUtils.generateIdentifier();
    frescoDrawable.setImageId(imageId);

    Extras extras = obtainExtras(null, null, frescoDrawable);

    // Notify listeners that we're about to fetch an image
    frescoDrawable.getInternalListener().onSubmit(imageId, imageRequest, callerContext, extras);
    frescoDrawable.getImagePerfListener().onImageFetch(frescoDrawable);

    // Direct bitmap available
    if (imageRequest.imageSource instanceof BitmapImageSource) {
      Bitmap bitmap = ((BitmapImageSource) imageRequest.imageSource).getBitmap();
      CloseableBitmap closeableBitmap =
          CloseableStaticBitmap.of(
              bitmap, noOpReleaser -> {}, ImmutableQualityInfo.FULL_QUALITY, 0);

      CloseableReference<CloseableImage> bitmapRef = CloseableReference.of(closeableBitmap);
      try {
        frescoDrawable.setImageOrigin(ImageOrigin.MEMORY_BITMAP);
        // Immediately display the actual image.
        setActualImage(frescoDrawable, imageRequest, bitmapRef, true, null);
        frescoDrawable.setFetchSubmitted(true);
        mDebugOverlayFactory.update(frescoDrawable, extras);
        return true;
      } finally {
        CloseableReference.closeSafely(bitmapRef);
      }
    } else if (imageRequest.imageSource instanceof DrawableImageSource) {
      Drawable actualImageDrawable = ((DrawableImageSource) imageRequest.imageSource).getDrawable();

      ScaleTypeDrawable actualImageWrapperDrawable = frescoDrawable.getActualImageWrapper();
      mHierarcher.setupActualImageWrapper(
          actualImageWrapperDrawable, imageRequest.imageOptions, drawable.getCallerContext());
      Drawable actualDrawable =
          mHierarcher.applyRoundingOptions(
              imageRequest.resources, actualImageDrawable, imageRequest.imageOptions);
      actualImageWrapperDrawable.setCurrent(actualDrawable);

      frescoDrawable.setImage(actualImageWrapperDrawable, null);
      frescoDrawable.showImageImmediately();

      frescoDrawable.setImageOrigin(ImageOrigin.LOCAL);
      frescoDrawable.setFetchSubmitted(true);

      if (imageRequest.imageOptions.shouldAutoPlay() && actualImageDrawable instanceof Animatable) {
        ((Animatable) actualDrawable).start();
      }

      Map<String, Object> imageInfoExtras;
      if (extras.imageExtras != null) {
        imageInfoExtras = extras.imageExtras;
      } else {
        imageInfoExtras = new HashMap<>();
      }

      frescoDrawable
          .getInternalListener()
          .onFinalImageSet(
              drawable.getImageId(),
              imageRequest,
              ImageOrigin.LOCAL,
              new ImageInfoImpl(
                  actualImageDrawable.getIntrinsicWidth(),
                  actualImageDrawable.getIntrinsicHeight(),
                  0,
                  ImmutableQualityInfo.FULL_QUALITY,
                  imageInfoExtras),
              extras,
              actualDrawable);
      drawable.getImagePerfListener().onImageSuccess(drawable, true);
      mDebugOverlayFactory.update(frescoDrawable, extras);
      return true;
    }

    // Check if the image is in cache
    CloseableReference<CloseableImage> cachedImage = mImagePipeline.getCachedImage(imageRequest);
    try {
      if (CloseableReference.isValid(cachedImage)) {
        frescoDrawable.setImageOrigin(ImageOrigin.MEMORY_BITMAP_SHORTCUT);
        // Immediately display the actual image.
        setActualImage(frescoDrawable, imageRequest, cachedImage, true, null);
        frescoDrawable.setFetchSubmitted(true);
        mDebugOverlayFactory.update(frescoDrawable, extras);
        return true;
      }
    } finally {
      CloseableReference.closeSafely(cachedImage);
    }

    // The image is not in cache -> Set up layers visible until the image is available
    frescoDrawable.setProgressDrawable(
        mHierarcher.buildProgressDrawable(imageRequest.resources, imageRequest.imageOptions));
    // Immediately show the progress image and set progress to 0
    frescoDrawable.setProgress(0f);
    frescoDrawable.showProgressImmediately();

    setUpPlaceholder(frescoDrawable, imageRequest, imageId);

    // Fetch the image
    final Runnable fetchRunnable =
        new Runnable() {
          @Override
          public void run() {
            if (imageId != frescoDrawable.getImageId()) {
              return; // We're trying to load a different image -> ignore
            }
            DataSource<CloseableReference<CloseableImage>> dataSource =
                mImagePipeline.fetchDecodedImage(
                    imageRequest, callerContext, frescoDrawable.getImageOriginListener(), imageId);
            frescoDrawable.setDataSource(imageId, dataSource);
            dataSource.subscribe(frescoDrawable, mUiThreadExecutor);
          }
        };

    if (mConfig.submitFetchOnBgThread()) {
      mLightweightBackgroundThreadExecutor.execute(fetchRunnable);
    } else {
      fetchRunnable.run();
    }
    frescoDrawable.setFetchSubmitted(true);

    mDebugOverlayFactory.update(frescoDrawable, null);

    return false;
  }

  private void emptyRequestFastPath(
      FrescoDrawable2Impl frescoDrawable,
      VitoImageRequest imageRequest,
      @Nullable Object callerContext) {
    frescoDrawable.close();
    frescoDrawable.setVitoImageRequestListener(mGlobalImageListener);

    frescoDrawable.getInternalListener().onEmptyEvent(callerContext);

    frescoDrawable.setOverlayDrawable(
        mHierarcher.buildOverlayDrawable(imageRequest.resources, imageRequest.imageOptions));
    setUpPlaceholder(frescoDrawable, imageRequest, EMPTY_IMAGE_ID);
  }

  private void setUpPlaceholder(
      FrescoDrawable2Impl frescoDrawable, VitoImageRequest imageRequest, long imageId) {
    Drawable placeholder =
        mHierarcher.buildPlaceholderDrawable(imageRequest.resources, imageRequest.imageOptions);
    frescoDrawable.setPlaceholderDrawable(placeholder);
    frescoDrawable.setImageDrawable(null);
    frescoDrawable.getInternalListener().onPlaceholderSet(imageId, imageRequest, placeholder);
  }

  @Override
  public void releaseDelayed(final FrescoDrawableInterface drawable) {
    if (!(drawable instanceof FrescoDrawable2Impl)) {
      FLog.e(TAG, "Drawable not supported " + drawable);
      return;
    }
    FrescoDrawable2Impl frescoDrawable = (FrescoDrawable2Impl) drawable;
    frescoDrawable.getImagePerfListener().onScheduleReleaseDelayed(frescoDrawable);
    frescoDrawable.scheduleReleaseDelayed();
  }

  @Override
  public void release(final FrescoDrawableInterface drawable) {
    if (!(drawable instanceof FrescoDrawable2Impl)) {
      FLog.e(TAG, "Drawable not supported " + drawable);
      return;
    }
    FrescoDrawable2Impl frescoDrawable = (FrescoDrawable2Impl) drawable;
    frescoDrawable.getImagePerfListener().onScheduleReleaseNextFrame(frescoDrawable);
    frescoDrawable.scheduleReleaseNextFrame();
  }

  @Override
  public void releaseImmediately(FrescoDrawableInterface drawable) {
    if (!(drawable instanceof FrescoDrawable2Impl)) {
      FLog.e(TAG, "Drawable not supported " + drawable);
      return;
    }
    FrescoDrawable2Impl frescoDrawable = (FrescoDrawable2Impl) drawable;
    frescoDrawable.getImagePerfListener().onReleaseImmediately(frescoDrawable);
    frescoDrawable.releaseImmediately();
  }

  private void setActualImage(
      FrescoDrawable2Impl drawable,
      VitoImageRequest imageRequest,
      CloseableReference<CloseableImage> image,
      boolean isImmediate,
      @Nullable DataSource<CloseableReference<CloseableImage>> dataSource) {

    ScaleTypeDrawable actualImageWrapperDrawable = drawable.getActualImageWrapper();
    mHierarcher.setupActualImageWrapper(
        actualImageWrapperDrawable, imageRequest.imageOptions, drawable.getCallerContext());
    Drawable actualDrawable =
        mHierarcher.buildActualImageDrawable(
            imageRequest.resources, imageRequest.imageOptions, image);
    actualImageWrapperDrawable.setCurrent(
        actualDrawable != null ? actualDrawable : NopDrawable.INSTANCE);

    drawable.setImage(actualImageWrapperDrawable, image);

    if (isImmediate || imageRequest.imageOptions.getFadeDurationMs() <= 0) {
      drawable.showImageImmediately();
    } else {
      drawable.fadeInImage(imageRequest.imageOptions.getFadeDurationMs());
    }
    if (imageRequest.imageOptions.shouldAutoPlay() && actualDrawable instanceof Animatable) {
      ((Animatable) actualDrawable).start();
    }
    Extras extras = obtainExtras(dataSource, image, drawable);
    ImageInfo imageInfo = image.get().getImageInfo();
    if (notifyFinalResult(dataSource)) {
      drawable
          .getInternalListener()
          .onFinalImageSet(
              drawable.getImageId(),
              imageRequest,
              drawable.getImageOrigin(),
              imageInfo,
              extras,
              actualDrawable);
    } else {
      drawable
          .getInternalListener()
          .onIntermediateImageSet(drawable.getImageId(), imageRequest, imageInfo);
    }
    drawable.getImagePerfListener().onImageSuccess(drawable, isImmediate);
    float progress = 1f;
    if (dataSource != null && !dataSource.isFinished()) {
      progress = dataSource.getProgress();
    }
    drawable.setProgress(progress);
    mDebugOverlayFactory.update(drawable, extras);
  }

  @Override
  public void onNewResult(
      FrescoDrawable2Impl drawable,
      VitoImageRequest imageRequest,
      @Nullable DataSource<CloseableReference<CloseableImage>> dataSource) {
    if (dataSource == null || !dataSource.hasResult()) {
      return;
    }

    CloseableReference<CloseableImage> image = dataSource.getResult();
    try {
      if (!CloseableReference.isValid(image)) {
        onFailure(drawable, imageRequest, dataSource);
      } else {
        setActualImage(drawable, imageRequest, image, false, dataSource);
      }
    } finally {
      CloseableReference.closeSafely(image);
    }
  }

  @Override
  public void onFailure(
      FrescoDrawable2Impl drawable,
      VitoImageRequest imageRequest,
      DataSource<CloseableReference<CloseableImage>> dataSource) {
    Drawable errorDrawable =
        mHierarcher.buildErrorDrawable(imageRequest.resources, imageRequest.imageOptions);
    drawable.setProgress(1f);
    drawable.setImageDrawable(errorDrawable);
    if (!drawable.isDefaultLayerIsOn()) {
      if (imageRequest.imageOptions.getFadeDurationMs() <= 0) {
        drawable.showImageImmediately();
      } else {
        drawable.fadeInImage(imageRequest.imageOptions.getFadeDurationMs());
      }
    } else {
      drawable.setPlaceholderDrawable(null);
      drawable.setProgressDrawable(null);
    }
    Extras extras = obtainExtras(dataSource, dataSource.getResult(), drawable);
    if (notifyFinalResult(dataSource)) {
      drawable
          .getInternalListener()
          .onFailure(
              drawable.getImageId(),
              imageRequest,
              errorDrawable,
              dataSource.getFailureCause(),
              extras);
    } else {
      drawable
          .getInternalListener()
          .onIntermediateImageFailed(
              drawable.getImageId(), imageRequest, dataSource.getFailureCause());
    }

    drawable.getImagePerfListener().onImageError(drawable);
    mDebugOverlayFactory.update(drawable, extras);
  }

  @Override
  public void onProgressUpdate(
      FrescoDrawable2Impl drawable,
      VitoImageRequest imageRequest,
      DataSource<CloseableReference<CloseableImage>> dataSource) {
    boolean isFinished = dataSource.isFinished();
    float progress = dataSource.getProgress();
    if (!isFinished) {
      drawable.setProgress(progress);
    }
  }

  @Override
  public void onRelease(final FrescoDrawable2Impl drawable) {
    final VitoImageRequest imageRequest = drawable.getImageRequest();
    if (imageRequest != null) {
      // Notify listeners
      drawable
          .getInternalListener()
          .onRelease(drawable.getImageId(), imageRequest, obtainExtras(null, null, drawable));
      if (mConfig.stopAnimationInOnRelease()) {
        // We automatically stop the animation if it was automatically started
        if ((!mConfig.onlyStopAnimationWhenAutoPlayEnabled()
                || imageRequest.imageOptions.shouldAutoPlay())
            && drawable.getActualImageDrawable() instanceof Animatable) {
          ((Animatable) drawable.getActualImageDrawable()).stop();
        }
      }
    }
    drawable.getImagePerfListener().onImageRelease(drawable);
  }

  private boolean isAlreadyLoadingImage(
      FrescoDrawable2Impl drawable, VitoImageRequest imageRequest) {
    if (drawable.getDrawableDataSubscriber() != this || !drawable.isFetchSubmitted()) {
      return false;
    }
    if (mConfig.useSmartPropertyDiffing()) {
      return imageRequest.equalsIfHasImage(drawable.getImageRequest(), drawable.hasImage());
    } else {
      return imageRequest.equals(drawable.getImageRequest());
    }
  }

  private static Extras obtainExtras(
      @Nullable DataSource<CloseableReference<CloseableImage>> dataSource,
      @Nullable CloseableReference<CloseableImage> image,
      FrescoDrawable2 drawable) {
    Map<String, Object> imageExtras = null;
    if (image != null) {
      imageExtras = image.get().getExtras();
    }

    Uri sourceUri = null;
    VitoImageRequest vitoImageRequest = drawable.getImageRequest();
    Map<String, Object> imageSourceExtras = null;
    boolean logWithHighSamplingRate = false;
    if (vitoImageRequest != null) {
      logWithHighSamplingRate = vitoImageRequest.logWithHighSamplingRate;
      if (vitoImageRequest.finalImageRequest != null) {
        sourceUri = vitoImageRequest.finalImageRequest.getSourceUri();
      }
      imageSourceExtras =
          (Map<String, Object>) vitoImageRequest.extras.get(HasExtraData.KEY_IMAGE_SOURCE_EXTRAS);
    }

    return MiddlewareUtils.obtainExtras(
        COMPONENT_EXTRAS,
        SHORTCUT_EXTRAS,
        dataSource == null ? null : dataSource.getExtras(),
        imageSourceExtras,
        drawable.getViewportDimensions(),
        String.valueOf(drawable.getActualImageScaleType()),
        drawable.getActualImageFocusPoint(),
        imageExtras,
        drawable.getCallerContext(),
        logWithHighSamplingRate,
        sourceUri);
  }

  private static boolean notifyFinalResult(
      @Nullable DataSource<CloseableReference<CloseableImage>> dataSource) {
    return dataSource == null || dataSource.isFinished() || dataSource.hasMultipleResults();
  }
}
