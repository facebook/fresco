/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.controller;

import android.content.Context;
import android.graphics.drawable.Animatable;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSources;
import com.facebook.datasource.FirstAvailableDataSourceSupplier;
import com.facebook.datasource.IncreasingQualityDataSourceSupplier;
import com.facebook.drawee.gestures.GestureDetector;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.SimpleDraweeControllerBuilder;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.ui.common.LoggingListener;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.ReturnsOwnership;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nullable;

/** Base implementation for Drawee controller builders. */
public abstract class AbstractDraweeControllerBuilder<
        BUILDER extends AbstractDraweeControllerBuilder<BUILDER, REQUEST, IMAGE, INFO>,
        REQUEST,
        IMAGE,
        INFO>
    implements SimpleDraweeControllerBuilder {

  private static final ControllerListener<Object> sAutoPlayAnimationsListener =
      new BaseControllerListener<Object>() {
        @Override
        public void onFinalImageSet(String id, @Nullable Object info, @Nullable Animatable anim) {
          if (anim != null) {
            anim.start();
          }
        }
      };

  private static final NullPointerException NO_REQUEST_EXCEPTION =
      new NullPointerException("No image request was specified!");

  // components
  private final Context mContext;
  private final Set<ControllerListener> mBoundControllerListeners;
  private final Set<ControllerListener2> mBoundControllerListeners2;

  // builder parameters
  private @Nullable Object mCallerContext;
  private @Nullable REQUEST mImageRequest;
  private @Nullable REQUEST mLowResImageRequest;
  private @Nullable REQUEST[] mMultiImageRequests;
  private boolean mTryCacheOnlyFirst;
  private @Nullable Supplier<DataSource<IMAGE>> mDataSourceSupplier;
  private @Nullable ControllerListener<? super INFO> mControllerListener;
  private @Nullable LoggingListener mLoggingListener;
  private @Nullable ControllerViewportVisibilityListener mControllerViewportVisibilityListener;
  private boolean mTapToRetryEnabled;
  private boolean mAutoPlayAnimations;
  private boolean mRetainImageOnFailure;
  private String mContentDescription;
  // old controller to reuse
  private @Nullable DraweeController mOldController;

  private static final AtomicLong sIdCounter = new AtomicLong();

  protected AbstractDraweeControllerBuilder(
      Context context,
      Set<ControllerListener> boundControllerListeners,
      Set<ControllerListener2> boundControllerListeners2) {
    mContext = context;
    mBoundControllerListeners = boundControllerListeners;
    mBoundControllerListeners2 = boundControllerListeners2;
    init();
  }

  /** Initializes this builder. */
  private void init() {
    mCallerContext = null;
    mImageRequest = null;
    mLowResImageRequest = null;
    mMultiImageRequests = null;
    mTryCacheOnlyFirst = true;
    mControllerListener = null;
    mLoggingListener = null;
    mControllerViewportVisibilityListener = null;
    mTapToRetryEnabled = false;
    mAutoPlayAnimations = false;
    mOldController = null;
    mContentDescription = null;
  }

  /** Resets this builder to its initial values making it reusable. */
  public BUILDER reset() {
    init();
    return getThis();
  }

  /** Sets the caller context. */
  @Override
  public BUILDER setCallerContext(Object callerContext) {
    mCallerContext = callerContext;
    return getThis();
  }

  /** Gets the caller context. */
  @Nullable
  public Object getCallerContext() {
    return mCallerContext;
  }

  /** Sets the image request. */
  public BUILDER setImageRequest(REQUEST imageRequest) {
    mImageRequest = imageRequest;
    return getThis();
  }

  /** Gets the image request. */
  @Nullable
  public REQUEST getImageRequest() {
    return mImageRequest;
  }

  /** Sets the low-res image request. */
  public BUILDER setLowResImageRequest(REQUEST lowResImageRequest) {
    mLowResImageRequest = lowResImageRequest;
    return getThis();
  }

  /** Gets the low-res image request. */
  @Nullable
  public REQUEST getLowResImageRequest() {
    return mLowResImageRequest;
  }

  /**
   * Sets the array of first-available image requests that will be probed in order.
   *
   * <p>For performance reasons, the array is not deep-copied, but only stored by reference. Please
   * don't modify once submitted.
   */
  public BUILDER setFirstAvailableImageRequests(REQUEST[] firstAvailableImageRequests) {
    return setFirstAvailableImageRequests(firstAvailableImageRequests, true);
  }

  /**
   * Sets the array of first-available image requests that will be probed in order.
   *
   * <p>For performance reasons, the array is not deep-copied, but only stored by reference. Please
   * don't modify once submitted.
   *
   * @param tryCacheOnlyFirst if set, bitmap cache only requests will be tried in order before the
   *     supplied requests.
   */
  public BUILDER setFirstAvailableImageRequests(
      REQUEST[] firstAvailableImageRequests, boolean tryCacheOnlyFirst) {
    Preconditions.checkArgument(
        firstAvailableImageRequests == null || firstAvailableImageRequests.length > 0,
        "No requests specified!");
    mMultiImageRequests = firstAvailableImageRequests;
    mTryCacheOnlyFirst = tryCacheOnlyFirst;
    return getThis();
  }

  /**
   * Gets the array of first-available image requests.
   *
   * <p>For performance reasons, the array is not deep-copied, but only stored by reference. Please
   * don't modify.
   */
  @Nullable
  public REQUEST[] getFirstAvailableImageRequests() {
    return mMultiImageRequests;
  }

  /**
   * Sets the data source supplier to be used.
   *
   * <p>Note: This is mutually exclusive with other image request setters.
   */
  public BUILDER setDataSourceSupplier(@Nullable Supplier<DataSource<IMAGE>> dataSourceSupplier) {
    mDataSourceSupplier = dataSourceSupplier;
    return getThis();
  }

  /**
   * Gets the data source supplier if set.
   *
   * <p>Important: this only returns the externally set data source (if any). Subclasses should use
   * {#code obtainDataSourceSupplier()} to obtain a data source to be passed to the controller.
   */
  @Nullable
  public Supplier<DataSource<IMAGE>> getDataSourceSupplier() {
    return mDataSourceSupplier;
  }

  /** Sets whether tap-to-retry is enabled. */
  public BUILDER setTapToRetryEnabled(boolean enabled) {
    mTapToRetryEnabled = enabled;
    return getThis();
  }

  /** Gets whether tap-to-retry is enabled. */
  public boolean getTapToRetryEnabled() {
    return mTapToRetryEnabled;
  }

  /** Sets whether to display last available image in case of failure. */
  public BUILDER setRetainImageOnFailure(boolean enabled) {
    mRetainImageOnFailure = enabled;
    return getThis();
  }

  /** Gets whether to retain image on failure. */
  public boolean getRetainImageOnFailure() {
    return mRetainImageOnFailure;
  }

  /** Sets whether to auto play animations. */
  public BUILDER setAutoPlayAnimations(boolean enabled) {
    mAutoPlayAnimations = enabled;
    return getThis();
  }

  /** Gets whether to auto play animations. */
  public boolean getAutoPlayAnimations() {
    return mAutoPlayAnimations;
  }

  /** Sets the controller listener. */
  public BUILDER setControllerListener(
      @Nullable ControllerListener<? super INFO> controllerListener) {
    mControllerListener = controllerListener;
    return getThis();
  }

  public BUILDER setLoggingListener(@Nullable LoggingListener loggingListener) {
    mLoggingListener = loggingListener;
    return getThis();
  }

  @Nullable
  public LoggingListener getLoggingListener() {
    return mLoggingListener;
  }

  /** Gets the controller listener */
  @Nullable
  public ControllerListener<? super INFO> getControllerListener() {
    return mControllerListener;
  }

  /** Sets the controller viewport visibility listener. */
  public BUILDER setControllerViewportVisibilityListener(
      @Nullable ControllerViewportVisibilityListener controllerViewportVisibilityListener) {
    mControllerViewportVisibilityListener = controllerViewportVisibilityListener;
    return getThis();
  }

  /** Gets the controller viewport visibility listener. */
  @Nullable
  public ControllerViewportVisibilityListener getControllerViewportVisibilityListener() {
    return mControllerViewportVisibilityListener;
  }

  /** Sets the accessibility content description. */
  public BUILDER setContentDescription(String contentDescription) {
    mContentDescription = contentDescription;
    return getThis();
  }

  /** Gets the accessibility content description. */
  @Nullable
  public String getContentDescription() {
    return mContentDescription;
  }

  /** Sets the old controller to be reused if possible. */
  @Override
  public BUILDER setOldController(@Nullable DraweeController oldController) {
    mOldController = oldController;
    return getThis();
  }

  /** Gets the old controller to be reused. */
  @Nullable
  public DraweeController getOldController() {
    return mOldController;
  }

  /** Builds the specified controller. */
  @Override
  public AbstractDraweeController build() {
    validate();

    // if only a low-res request is specified, treat it as a final request.
    if (mImageRequest == null && mMultiImageRequests == null && mLowResImageRequest != null) {
      mImageRequest = mLowResImageRequest;
      mLowResImageRequest = null;
    }

    return buildController();
  }

  /** Validates the parameters before building a controller. */
  protected void validate() {
    Preconditions.checkState(
        (mMultiImageRequests == null) || (mImageRequest == null),
        "Cannot specify both ImageRequest and FirstAvailableImageRequests!");
    Preconditions.checkState(
        (mDataSourceSupplier == null)
            || (mMultiImageRequests == null
                && mImageRequest == null
                && mLowResImageRequest == null),
        "Cannot specify DataSourceSupplier with other ImageRequests! Use one or the other.");
  }

  /** Builds a regular controller. */
  protected AbstractDraweeController buildController() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("AbstractDraweeControllerBuilder#buildController");
    }
    AbstractDraweeController controller = obtainController();
    controller.setRetainImageOnFailure(getRetainImageOnFailure());
    controller.setContentDescription(getContentDescription());
    controller.setControllerViewportVisibilityListener(getControllerViewportVisibilityListener());
    maybeBuildAndSetRetryManager(controller);
    maybeAttachListeners(controller);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return controller;
  }

  /** Generates unique controller id. */
  protected static String generateUniqueControllerId() {
    return String.valueOf(sIdCounter.getAndIncrement());
  }

  /** Gets the top-level data source supplier to be used by a controller. */
  protected Supplier<DataSource<IMAGE>> obtainDataSourceSupplier(
      final DraweeController controller, final String controllerId) {
    if (mDataSourceSupplier != null) {
      return mDataSourceSupplier;
    }

    Supplier<DataSource<IMAGE>> supplier = null;

    // final image supplier;
    if (mImageRequest != null) {
      supplier = getDataSourceSupplierForRequest(controller, controllerId, mImageRequest);
    } else if (mMultiImageRequests != null) {
      supplier =
          getFirstAvailableDataSourceSupplier(
              controller, controllerId, mMultiImageRequests, mTryCacheOnlyFirst);
    }

    // increasing-quality supplier; highest-quality supplier goes first
    if (supplier != null && mLowResImageRequest != null) {
      List<Supplier<DataSource<IMAGE>>> suppliers = new ArrayList<>(2);
      suppliers.add(supplier);
      suppliers.add(getDataSourceSupplierForRequest(controller, controllerId, mLowResImageRequest));
      supplier = IncreasingQualityDataSourceSupplier.create(suppliers, false);
    }

    // no image requests; use null data source supplier
    if (supplier == null) {
      supplier = DataSources.getFailedDataSourceSupplier(NO_REQUEST_EXCEPTION);
    }

    return supplier;
  }

  protected Supplier<DataSource<IMAGE>> getFirstAvailableDataSourceSupplier(
      final DraweeController controller,
      String controllerId,
      REQUEST[] imageRequests,
      boolean tryBitmapCacheOnlyFirst) {
    List<Supplier<DataSource<IMAGE>>> suppliers = new ArrayList<>(imageRequests.length * 2);
    if (tryBitmapCacheOnlyFirst) {
      // we first add bitmap-cache-only suppliers, then the full-fetch ones
      for (int i = 0; i < imageRequests.length; i++) {
        suppliers.add(
            getDataSourceSupplierForRequest(
                controller, controllerId, imageRequests[i], CacheLevel.BITMAP_MEMORY_CACHE));
      }
    }
    for (int i = 0; i < imageRequests.length; i++) {
      suppliers.add(getDataSourceSupplierForRequest(controller, controllerId, imageRequests[i]));
    }
    return FirstAvailableDataSourceSupplier.create(suppliers);
  }

  /** Creates a data source supplier for the given image request. */
  protected Supplier<DataSource<IMAGE>> getDataSourceSupplierForRequest(
      final DraweeController controller, String controllerId, REQUEST imageRequest) {
    return getDataSourceSupplierForRequest(
        controller, controllerId, imageRequest, CacheLevel.FULL_FETCH);
  }

  /** Creates a data source supplier for the given image request. */
  protected Supplier<DataSource<IMAGE>> getDataSourceSupplierForRequest(
      final DraweeController controller,
      final String controllerId,
      final REQUEST imageRequest,
      final CacheLevel cacheLevel) {
    final Object callerContext = getCallerContext();
    return new Supplier<DataSource<IMAGE>>() {
      @Override
      public DataSource<IMAGE> get() {
        return getDataSourceForRequest(
            controller, controllerId, imageRequest, callerContext, cacheLevel);
      }

      @Override
      public String toString() {
        return Objects.toStringHelper(this).add("request", imageRequest.toString()).toString();
      }
    };
  }

  /** Attaches listeners (if specified) to the given controller. */
  protected void maybeAttachListeners(AbstractDraweeController controller) {
    if (mBoundControllerListeners != null) {
      for (ControllerListener<? super INFO> listener : mBoundControllerListeners) {
        controller.addControllerListener(listener);
      }
    }
    if (mBoundControllerListeners2 != null) {
      for (ControllerListener2<INFO> listener : mBoundControllerListeners2) {
        controller.addControllerListener2(listener);
      }
    }
    if (mControllerListener != null) {
      controller.addControllerListener(mControllerListener);
    }
    if (mAutoPlayAnimations) {
      controller.addControllerListener(sAutoPlayAnimationsListener);
    }
  }

  /** Installs a retry manager (if specified) to the given controller. */
  protected void maybeBuildAndSetRetryManager(AbstractDraweeController controller) {
    if (!mTapToRetryEnabled) {
      return;
    }
    controller.getRetryManager().setTapToRetryEnabled(mTapToRetryEnabled);
    maybeBuildAndSetGestureDetector(controller);
  }

  /** Installs a gesture detector to the given controller. */
  protected void maybeBuildAndSetGestureDetector(AbstractDraweeController controller) {
    GestureDetector gestureDetector = controller.getGestureDetector();
    if (gestureDetector == null) {
      gestureDetector = GestureDetector.newInstance(mContext);
      controller.setGestureDetector(gestureDetector);
    }
  }

  /* Gets the context. */
  protected Context getContext() {
    return mContext;
  }

  /** Concrete builder classes should override this method to return a new controller. */
  @ReturnsOwnership
  protected abstract AbstractDraweeController obtainController();

  /**
   * Concrete builder classes should override this method to return a data source for the request.
   *
   * <p>IMPORTANT: Do NOT ever call this method directly. This method is only to be called from a
   * supplier created in {#code getDataSourceSupplierForRequest(REQUEST, boolean)}.
   *
   * <p>IMPORTANT: Make sure that you do NOT use any non-final field from this method, as the field
   * may change if the instance of this builder gets reused. If any such field is required, override
   * {#code getDataSourceSupplierForRequest(REQUEST, boolean)}, and store the field in a final
   * variable (same as it is done for callerContext).
   */
  protected abstract DataSource<IMAGE> getDataSourceForRequest(
      final DraweeController controller,
      final String controllerId,
      final REQUEST imageRequest,
      final Object callerContext,
      final CacheLevel cacheLevel);

  protected final BUILDER getThis() {
    return (BUILDER) this;
  }

  public enum CacheLevel {
    /* Fetch (from the network or local storage) */
    FULL_FETCH,

    /* Disk caching */
    DISK_CACHE,

    /* Bitmap caching */
    BITMAP_MEMORY_CACHE;
  }
}
