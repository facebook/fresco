/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.controller;

import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import android.content.Context;
import android.graphics.drawable.Animatable;

import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSources;
import com.facebook.datasource.FirstAvailableDataSourceSupplier;
import com.facebook.datasource.IncreasingQualityDataSourceSupplier;
import com.facebook.drawee.components.RetryManager;
import com.facebook.drawee.gestures.GestureDetector;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.SimpleDraweeControllerBuilder;

/**
 * Base implementation for Drawee controller builders.
 */
public abstract class AbstractDraweeControllerBuilder <
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

  // builder parameters
  private @Nullable Object mCallerContext;
  private @Nullable REQUEST mImageRequest;
  private @Nullable REQUEST mLowResImageRequest;
  private @Nullable REQUEST[] mMultiImageRequests;
  private @Nullable Supplier<DataSource<IMAGE>> mDataSourceSupplier;
  private @Nullable ControllerListener<? super INFO> mControllerListener;
  private boolean mTapToRetryEnabled;
  private boolean mAutoPlayAnimations;
  // old controller to reuse
  private @Nullable DraweeController mOldController;

  private static final AtomicLong sIdCounter = new AtomicLong();

  protected AbstractDraweeControllerBuilder(
      Context context,
      Set<ControllerListener> boundControllerListeners) {
    mContext = context;
    mBoundControllerListeners = boundControllerListeners;
    init();
  }

  /** Initializes this builder. */
  private void init() {
    mCallerContext = null;
    mImageRequest = null;
    mLowResImageRequest = null;
    mMultiImageRequests = null;
    mControllerListener = null;
    mTapToRetryEnabled = false;
    mAutoPlayAnimations = false;
    mOldController = null;
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
   * <p> For performance reasons, the array is not deep-copied, but only stored by reference.
   * Please don't modify once submitted.
   */
  public BUILDER setFirstAvailableImageRequests(REQUEST[] firstAvailableImageRequests) {
    mMultiImageRequests = firstAvailableImageRequests;
    return getThis();
  }

  /**
   * Gets the array of first-available image requests.
   * <p> For performance reasons, the array is not deep-copied, but only stored by reference.
   * Please don't modify.
   */
  @Nullable
  public REQUEST[] getFirstAvailableImageRequests() {
    return mMultiImageRequests;
  }

  /**
   *  Sets the data source supplier to be used.
   *
   *  <p/> Note: This is mutually exclusive with other image request setters.
   */
  public void setDataSourceSupplier(@Nullable Supplier<DataSource<IMAGE>> dataSourceSupplier) {
    mDataSourceSupplier = dataSourceSupplier;
  }

  /**
   * Gets the data source supplier if set.
   *
   * <p/>Important: this only returns the externally set data source (if any). Subclasses should
   * use {#code obtainDataSourceSupplier()} to obtain a data source to be passed to the controller.
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
  public BUILDER setControllerListener(ControllerListener<? super INFO> controllerListener) {
    mControllerListener = controllerListener;
    return getThis();
  }

  /** Gets the controller listener */
  @Nullable
  public ControllerListener<? super INFO> getControllerListener() {
    return mControllerListener;
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
        (mDataSourceSupplier == null) ||
            (mMultiImageRequests == null && mImageRequest == null && mLowResImageRequest == null),
        "Cannot specify DataSourceSupplier with other ImageRequests! Use one or the other.");
  }

  /** Builds a regular controller. */
  protected AbstractDraweeController buildController() {
    AbstractDraweeController controller = obtainController();
    maybeBuildAndSetRetryManager(controller);
    maybeAttachListeners(controller);
    return controller;
  }

  /** Generates unique controller id. */
  protected static String generateUniqueControllerId() {
    return String.valueOf(sIdCounter.getAndIncrement());
  }

  /** Gets the top-level data source supplier to be used by a controller. */
  protected Supplier<DataSource<IMAGE>> obtainDataSourceSupplier() {
    if (mDataSourceSupplier != null) {
      return mDataSourceSupplier;
    }

    Supplier<DataSource<IMAGE>> supplier = null;

    // final image supplier;
    if (mImageRequest != null) {
      supplier = getDataSourceSupplierForRequest(mImageRequest);
    } else if (mMultiImageRequests != null) {
      supplier = getFirstAvailableDataSourceSupplier(mMultiImageRequests);
    }

    // increasing-quality supplier; highest-quality supplier goes first
    if (supplier != null && mLowResImageRequest != null) {
      List<Supplier<DataSource<IMAGE>>> suppliers = new ArrayList<>(2);
      suppliers.add(supplier);
      suppliers.add(getDataSourceSupplierForRequest(mLowResImageRequest));
      supplier = IncreasingQualityDataSourceSupplier.create(suppliers);
    }

    // no image requests; use null data source supplier
    if (supplier == null) {
      supplier = DataSources.getFailedDataSourceSupplier(NO_REQUEST_EXCEPTION);
    }

    return supplier;
  }

  protected Supplier<DataSource<IMAGE>> getFirstAvailableDataSourceSupplier(
      REQUEST[] imageRequests) {
    List<Supplier<DataSource<IMAGE>>> suppliers = new ArrayList<>(imageRequests.length * 2);
    // we first add cache-only suppliers, then the full-fetch ones
    for (int i = 0; i < imageRequests.length; i++) {
      suppliers.add(getDataSourceSupplierForRequest(imageRequests[i], /*cacheOnly */ true));
    }
    for (int i = 0; i < imageRequests.length; i++) {
      suppliers.add(getDataSourceSupplierForRequest(imageRequests[i]));
    }
    return FirstAvailableDataSourceSupplier.create(suppliers);
  }

  /** Creates a data source supplier for the given image request. */
  protected Supplier<DataSource<IMAGE>> getDataSourceSupplierForRequest(REQUEST imageRequest) {
    return getDataSourceSupplierForRequest(imageRequest, /* bitmapCacheOnly */ false);
  }

  /** Creates a data source supplier for the given image request. */
  protected Supplier<DataSource<IMAGE>> getDataSourceSupplierForRequest(
      final REQUEST imageRequest,
      final boolean bitmapCacheOnly) {
    final Object callerContext = getCallerContext();
    return new Supplier<DataSource<IMAGE>>() {
      @Override
      public DataSource<IMAGE> get() {
        return getDataSourceForRequest(imageRequest, callerContext, bitmapCacheOnly);
      }
      @Override
      public String toString() {
        return Objects.toStringHelper(this)
            .add("request", imageRequest.toString())
            .toString();
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
    RetryManager retryManager = controller.getRetryManager();
    if (retryManager == null) {
      retryManager = new RetryManager();
      controller.setRetryManager(retryManager);
    }
    retryManager.setTapToRetryEnabled(mTapToRetryEnabled);
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
  protected abstract AbstractDraweeController obtainController();

  /**
   * Concrete builder classes should override this method to return a data source for the request.
   *
   * <p/>IMPORTANT: Do NOT ever call this method directly. This method is only to be called from
   * a supplier created in {#code getDataSourceSupplierForRequest(REQUEST, boolean)}.
   *
   * <p/>IMPORTANT: Make sure that you do NOT use any non-final field from this method, as the field
   * may change if the instance of this builder gets reused. If any such field is required, override
   * {#code getDataSourceSupplierForRequest(REQUEST, boolean)}, and store the field in a final
   * variable (same as it is done for callerContext).
   */
  protected abstract DataSource<IMAGE> getDataSourceForRequest(
      final REQUEST imageRequest,
      final Object callerContext,
      final boolean bitmapCacheOnly);

  /** Concrete builder classes should override this method to return {#code this}. */
  protected abstract BUILDER getThis();
}
