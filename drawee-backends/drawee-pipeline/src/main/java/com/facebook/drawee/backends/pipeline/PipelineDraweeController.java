/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableList;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.time.AwakeTimeSinceBootClock;
import com.facebook.datasource.DataSource;
import com.facebook.drawable.base.DrawableWithCaches;
import com.facebook.drawee.backends.pipeline.info.ForwardingImageOriginListener;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.ImageOriginListener;
import com.facebook.drawee.backends.pipeline.info.ImageOriginRequestListener;
import com.facebook.drawee.backends.pipeline.info.ImagePerfMonitor;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.controller.AbstractDraweeController;
import com.facebook.drawee.controller.AbstractDraweeControllerBuilder;
import com.facebook.drawee.debug.DebugControllerOverlayDrawable;
import com.facebook.drawee.debug.listener.ImageLoadingTimeControllerListener;
import com.facebook.drawee.drawable.ArrayDrawable;
import com.facebook.drawee.drawable.DrawableParent;
import com.facebook.drawee.drawable.ScaleTypeDrawable;
import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.drawee.interfaces.SettableDraweeHierarchy;
import com.facebook.fresco.ui.common.ImagePerfDataListener;
import com.facebook.fresco.ui.common.MultiUriHelper;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.listener.ForwardingRequestListener;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.Nullsafe;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * Drawee controller that bridges the image pipeline with {@link SettableDraweeHierarchy}.
 *
 * <p>The hierarchy's actual image is set to the image(s) obtained by the provided data source. The
 * data source is automatically obtained and closed based on attach / detach calls.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class PipelineDraweeController
    extends AbstractDraweeController<CloseableReference<CloseableImage>, ImageInfo> {

  private static final Class<?> TAG = PipelineDraweeController.class;

  // Components
  private final Resources mResources;
  private final DrawableFactory mDefaultDrawableFactory;
  // Global drawable factories that are set when Fresco is initialized
  @Nullable private final ImmutableList<DrawableFactory> mGlobalDrawableFactories;

  private final @Nullable MemoryCache<CacheKey, CloseableImage> mMemoryCache;

  // NULLSAFE_FIXME[Field Not Initialized]
  private CacheKey mCacheKey;

  // Constant state (non-final because controllers can be reused)
  // NULLSAFE_FIXME[Field Not Initialized]
  private Supplier<DataSource<CloseableReference<CloseableImage>>> mDataSourceSupplier;

  private boolean mDrawDebugOverlay;

  // Drawable factories that are unique for a given image request
  private @Nullable ImmutableList<DrawableFactory> mCustomDrawableFactories;

  private @Nullable ImagePerfMonitor mImagePerfMonitor;

  @GuardedBy("this")
  @Nullable
  private Set<RequestListener> mRequestListeners;

  @GuardedBy("this")
  @Nullable
  private ImageOriginListener mImageOriginListener;

  private @Nullable ImageRequest mImageRequest;
  private @Nullable ImageRequest[] mFirstAvailableImageRequests;
  private @Nullable ImageRequest mLowResImageRequest;

  public PipelineDraweeController(
      Resources resources,
      DeferredReleaser deferredReleaser,
      @Nullable DrawableFactory animatedDrawableFactory,
      @Nullable DrawableFactory xmlDrawableFactory,
      Executor uiThreadExecutor,
      @Nullable MemoryCache<CacheKey, CloseableImage> memoryCache,
      @Nullable ImmutableList<DrawableFactory> globalDrawableFactories) {
    // NULLSAFE_FIXME[Parameter Not Nullable]
    super(deferredReleaser, uiThreadExecutor, null, null);
    mResources = resources;
    mDefaultDrawableFactory =
        new DefaultDrawableFactory(resources, animatedDrawableFactory, xmlDrawableFactory);
    mGlobalDrawableFactories = globalDrawableFactories;
    mMemoryCache = memoryCache;
  }

  /**
   * Initializes this controller with the new data source supplier, id and caller context. This
   * allows for reusing of the existing controller instead of instantiating a new one. This method
   * should be called when the controller is in detached state.
   *
   * @param dataSourceSupplier data source supplier
   * @param id unique id for this controller
   * @param callerContext tag and context for this controller
   */
  public void initialize(
      Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier,
      String id,
      CacheKey cacheKey,
      Object callerContext,
      @Nullable ImmutableList<DrawableFactory> customDrawableFactories) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("PipelineDraweeController#initialize");
    }
    super.initialize(id, callerContext);
    init(dataSourceSupplier);
    mCacheKey = cacheKey;
    setCustomDrawableFactories(customDrawableFactories);
    maybeUpdateDebugOverlay(null);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  protected synchronized void initializePerformanceMonitoring(
      @Nullable ImagePerfDataListener imagePerfDataListener,
      AbstractDraweeControllerBuilder<
              PipelineDraweeControllerBuilder,
              ImageRequest,
              CloseableReference<CloseableImage>,
              ImageInfo>
          builder) {
    if (mImagePerfMonitor != null) {
      mImagePerfMonitor.reset();
    }
    if (imagePerfDataListener != null) {
      if (mImagePerfMonitor == null) {
        mImagePerfMonitor = new ImagePerfMonitor(AwakeTimeSinceBootClock.get(), this);
      }
      mImagePerfMonitor.addImagePerfDataListener(imagePerfDataListener);
      mImagePerfMonitor.setEnabled(true);
    }

    mImageRequest = builder.getImageRequest();
    mFirstAvailableImageRequests = builder.getFirstAvailableImageRequests();
    mLowResImageRequest = builder.getLowResImageRequest();
  }

  public void setDrawDebugOverlay(boolean drawDebugOverlay) {
    mDrawDebugOverlay = drawDebugOverlay;
  }

  public void setCustomDrawableFactories(
      @Nullable ImmutableList<DrawableFactory> customDrawableFactories) {
    mCustomDrawableFactories = customDrawableFactories;
  }

  public synchronized void addRequestListener(RequestListener requestListener) {
    if (mRequestListeners == null) {
      mRequestListeners = new HashSet<>();
    }
    mRequestListeners.add(requestListener);
  }

  public synchronized void removeRequestListener(RequestListener requestListener) {
    if (mRequestListeners == null) {
      return;
    }
    mRequestListeners.remove(requestListener);
  }

  public synchronized void removeImageOriginListener(ImageOriginListener imageOriginListener) {
    if (mImageOriginListener instanceof ForwardingImageOriginListener) {
      ((ForwardingImageOriginListener) mImageOriginListener)
          .removeImageOriginListener(imageOriginListener);
      return;
    }
    if (mImageOriginListener == imageOriginListener) {
      mImageOriginListener = null;
    }
  }

  private void init(Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier) {
    mDataSourceSupplier = dataSourceSupplier;

    maybeUpdateDebugOverlay(null);
  }

  protected Resources getResources() {
    return mResources;
  }

  protected CacheKey getCacheKey() {
    return mCacheKey;
  }

  @Nullable
  public synchronized RequestListener getRequestListener() {
    RequestListener imageOriginRequestListener = null;
    if (mImageOriginListener != null) {
      imageOriginRequestListener = new ImageOriginRequestListener(getId(), mImageOriginListener);
    }
    if (mRequestListeners != null) {
      ForwardingRequestListener requestListener = new ForwardingRequestListener(mRequestListeners);
      if (imageOriginRequestListener != null) {
        requestListener.addRequestListener(imageOriginRequestListener);
      }
      return requestListener;
    }
    return imageOriginRequestListener;
  }

  @Override
  protected DataSource<CloseableReference<CloseableImage>> getDataSource() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("PipelineDraweeController#getDataSource");
    }
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(TAG, "controller %x: getDataSource", System.identityHashCode(this));
    }
    DataSource<CloseableReference<CloseableImage>> result = mDataSourceSupplier.get();
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return result;
  }

  @Override
  protected Drawable createDrawable(CloseableReference<CloseableImage> image) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("PipelineDraweeController#createDrawable");
      }
      Preconditions.checkState(CloseableReference.isValid(image));
      CloseableImage closeableImage = image.get();

      maybeUpdateDebugOverlay(closeableImage);

      Drawable drawable =
          maybeCreateDrawableFromFactories(mCustomDrawableFactories, closeableImage);
      if (drawable != null) {
        return drawable;
      }

      drawable = maybeCreateDrawableFromFactories(mGlobalDrawableFactories, closeableImage);
      if (drawable != null) {
        return drawable;
      }

      drawable = mDefaultDrawableFactory.createDrawable(closeableImage);
      if (drawable != null) {
        return drawable;
      }
      throw new UnsupportedOperationException("Unrecognized image class: " + closeableImage);
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  private @Nullable Drawable maybeCreateDrawableFromFactories(
      @Nullable ImmutableList<DrawableFactory> drawableFactories, CloseableImage closeableImage) {
    if (drawableFactories == null) {
      return null;
    }
    for (DrawableFactory factory : drawableFactories) {
      if (factory.supportsImageType(closeableImage)) {
        Drawable drawable = factory.createDrawable(closeableImage);
        if (drawable != null) {
          return drawable;
        }
      }
    }
    return null;
  }

  @Override
  public void setHierarchy(@Nullable DraweeHierarchy hierarchy) {
    super.setHierarchy(hierarchy);
    maybeUpdateDebugOverlay(null);
  }

  @Override
  public boolean isSameImageRequest(@Nullable DraweeController other) {
    if (mCacheKey != null && other instanceof PipelineDraweeController) {
      return Objects.equal(mCacheKey, ((PipelineDraweeController) other).getCacheKey());
    }
    return false;
  }

  private void maybeUpdateDebugOverlay(@Nullable CloseableImage image) {
    if (!mDrawDebugOverlay) {
      return;
    }

    if (getControllerOverlay() == null) {
      final DebugControllerOverlayDrawable controllerOverlay = new DebugControllerOverlayDrawable();
      ImageLoadingTimeControllerListener overlayImageLoadListener =
          new ImageLoadingTimeControllerListener(controllerOverlay);
      addControllerListener(overlayImageLoadListener);
      setControllerOverlay(controllerOverlay);
    }

    if (getControllerOverlay() instanceof DebugControllerOverlayDrawable) {
      updateDebugOverlay(image, (DebugControllerOverlayDrawable) getControllerOverlay());
    }
  }

  /**
   * updateDebugOverlay updates the debug overlay. Subclasses of {@link PipelineDraweeController}
   * can override this method (and call <code>super</code>) to provide additional debug information.
   */
  protected void updateDebugOverlay(
      @Nullable CloseableImage image, DebugControllerOverlayDrawable debugOverlay) {
    debugOverlay.setControllerId(getId());

    final DraweeHierarchy draweeHierarchy = getHierarchy();
    ScaleType scaleType = null;
    if (draweeHierarchy != null) {
      final ScaleTypeDrawable scaleTypeDrawable =
          getActiveScaleTypeDrawable(draweeHierarchy.getTopLevelDrawable());
      scaleType = scaleTypeDrawable != null ? scaleTypeDrawable.getScaleType() : null;
    }
    debugOverlay.setScaleType(scaleType);

    String callerContextString = getCallerContextString();
    if (callerContextString != null) {
      debugOverlay.addAdditionalData("cc", callerContextString);
    }

    if (image != null) {
      debugOverlay.setDimensions(image.getWidth(), image.getHeight());
      debugOverlay.setImageSize(image.getSizeInBytes());
    } else {
      debugOverlay.reset();
    }
  }

  @Override
  protected ImageInfo getImageInfo(CloseableReference<CloseableImage> image) {
    Preconditions.checkState(CloseableReference.isValid(image));
    return image.get().getImageInfo();
  }

  @Override
  protected int getImageHash(@Nullable CloseableReference<CloseableImage> image) {
    return (image != null) ? image.getValueHash() : 0;
  }

  @Override
  protected void releaseImage(@Nullable CloseableReference<CloseableImage> image) {
    CloseableReference.closeSafely(image);
  }

  @Override
  protected void releaseDrawable(@Nullable Drawable drawable) {
    if (drawable instanceof DrawableWithCaches) {
      ((DrawableWithCaches) drawable).dropCaches();
    }
  }

  @Override
  protected @Nullable CloseableReference<CloseableImage> getCachedImage() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("PipelineDraweeController#getCachedImage");
    }
    try {
      if (mMemoryCache == null || mCacheKey == null) {
        return null;
      }
      // We get the CacheKey
      CloseableReference<CloseableImage> closeableImage = mMemoryCache.get(mCacheKey);
      if (closeableImage != null && !closeableImage.get().getQualityInfo().isOfFullQuality()) {
        closeableImage.close();
        return null;
      }
      return closeableImage;
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @Override
  protected void onImageLoadedFromCacheImmediately(
      String id, CloseableReference<CloseableImage> cachedImage) {
    super.onImageLoadedFromCacheImmediately(id, cachedImage);
    synchronized (this) {
      if (mImageOriginListener != null) {
        mImageOriginListener.onImageLoaded(
            id, ImageOrigin.MEMORY_BITMAP_SHORTCUT, true, "PipelineDraweeController");
      }
    }
  }

  protected Supplier<DataSource<CloseableReference<CloseableImage>>> getDataSourceSupplier() {
    return mDataSourceSupplier;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("super", super.toString())
        .add("dataSourceSupplier", mDataSourceSupplier)
        .toString();
  }

  @Override
  public @Nullable Map<String, Object> obtainExtrasFromImage(ImageInfo info) {
    if (info == null) return null;
    return info.getExtras();
  }

  @Override
  protected @Nullable Uri getMainUri() {
    return MultiUriHelper.getMainUri(
        mImageRequest,
        mLowResImageRequest,
        mFirstAvailableImageRequests,
        ImageRequest.REQUEST_TO_URI_FN);
  }

  protected @Nullable String getCallerContextString() {
    Object cc = getCallerContext();
    if (cc == null) {
      return null;
    }
    return cc.toString();
  }

  @Nullable
  public static ScaleTypeDrawable getActiveScaleTypeDrawable(@Nullable Drawable drawable) {
    if (drawable == null) {
      return null;
    } else if (drawable instanceof ScaleTypeDrawable) {
      return (ScaleTypeDrawable) drawable;
    } else if (drawable instanceof DrawableParent) {
      final Drawable childDrawable = ((DrawableParent) drawable).getDrawable();
      return getActiveScaleTypeDrawable(childDrawable);
    } else if (drawable instanceof ArrayDrawable) {
      final ArrayDrawable fadeDrawable = (ArrayDrawable) drawable;
      final int numLayers = fadeDrawable.getNumberOfLayers();

      for (int i = 0; i < numLayers; i++) {
        final Drawable childDrawable = fadeDrawable.getDrawable(i);
        final ScaleTypeDrawable result = getActiveScaleTypeDrawable(childDrawable);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }
}
