/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.controller;

import static com.facebook.drawee.components.DraweeEventTracker.Event;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import com.facebook.common.internal.Objects;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.drawee.components.DraweeEventTracker;
import com.facebook.drawee.components.RetryManager;
import com.facebook.drawee.gestures.GestureDetector;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.drawee.interfaces.SettableDraweeHierarchy;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.ReturnsOwnership;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Abstract Drawee controller that implements common functionality
 * regardless of the backend used to fetch the image.
 *
 * All methods should be called on the main UI thread.
 *
 * @param <T> image type (e.g. Bitmap)
 * @param <INFO> image info type (can be same as T)
 */
@NotThreadSafe
public abstract class AbstractDraweeController<T, INFO> implements
    DraweeController,
    DeferredReleaser.Releasable,
    GestureDetector.ClickListener {

  /**
   * This class is used to allow an optimization of not creating a ForwardingControllerListener
   * when there is only a single controller listener.
   */
  private static class InternalForwardingListener<INFO> extends ForwardingControllerListener<INFO> {
    public static <INFO> InternalForwardingListener<INFO> createInternal(
        ControllerListener<? super INFO> listener1, ControllerListener<? super INFO> listener2) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("AbstractDraweeController#createInternal");
      }
      InternalForwardingListener<INFO> forwarder = new InternalForwardingListener<INFO>();
      forwarder.addListener(listener1);
      forwarder.addListener(listener2);
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
      return forwarder;
    }
  }

  private static final Class<?> TAG = AbstractDraweeController.class;

  // Components
  private final DraweeEventTracker mEventTracker = DraweeEventTracker.newInstance();
  private final DeferredReleaser mDeferredReleaser;
  private final Executor mUiThreadImmediateExecutor;

  // Optional components
  private @Nullable RetryManager mRetryManager;
  private @Nullable GestureDetector mGestureDetector;
  private @Nullable ControllerViewportVisibilityListener mControllerViewportVisibilityListener;
  protected @Nullable ControllerListener<INFO> mControllerListener;

  // Hierarchy
  private @Nullable SettableDraweeHierarchy mSettableDraweeHierarchy;
  private @Nullable Drawable mControllerOverlay;

  // Constant state (non-final because controllers can be reused)
  private String mId;
  private Object mCallerContext;

  // Mutable state
  private boolean mIsAttached;
  private boolean mIsRequestSubmitted;
  private boolean mIsVisibleInViewportHint;
  private boolean mHasFetchFailed;
  private boolean mRetainImageOnFailure;
  private @Nullable String mContentDescription;
  private @Nullable DataSource<T> mDataSource;
  private @Nullable T mFetchedImage;
  private @Nullable Drawable mDrawable;
  private boolean mJustConstructed = true;

  public AbstractDraweeController(
      DeferredReleaser deferredReleaser,
      Executor uiThreadImmediateExecutor,
      String id,
      Object callerContext) {
    mDeferredReleaser = deferredReleaser;
    mUiThreadImmediateExecutor = uiThreadImmediateExecutor;
    init(id, callerContext);
  }

  /**
   * Initializes this controller with the new id and caller context.
   * This allows for reusing of the existing controller instead of instantiating a new one.
   * This method should be called when the controller is in detached state.
   * @param id unique id for this controller
   * @param callerContext tag and context for this controller
   */
  protected void initialize(String id, Object callerContext) {
    init(id, callerContext);
    mJustConstructed = false;
  }

  private synchronized void init(String id, Object callerContext) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("AbstractDraweeController#init");
    }
    mEventTracker.recordEvent(Event.ON_INIT_CONTROLLER);
    // cancel deferred release
    if (!mJustConstructed && mDeferredReleaser != null) {
      mDeferredReleaser.cancelDeferredRelease(this);
    }
    // reinitialize mutable state (fetch state)
    mIsAttached = false;
    mIsVisibleInViewportHint = false;
    releaseFetch();
    mRetainImageOnFailure = false;
    // reinitialize optional components
    if (mRetryManager != null) {
      mRetryManager.init();
    }
    if (mGestureDetector != null) {
      mGestureDetector.init();
      mGestureDetector.setClickListener(this);
    }
    if (mControllerListener instanceof InternalForwardingListener) {
      ((InternalForwardingListener) mControllerListener).clearListeners();
    } else {
      mControllerListener = null;
    }
    mControllerViewportVisibilityListener = null;
    // clear hierarchy and controller overlay
    if (mSettableDraweeHierarchy != null) {
      mSettableDraweeHierarchy.reset();
      mSettableDraweeHierarchy.setControllerOverlay(null);
      mSettableDraweeHierarchy = null;
    }
    mControllerOverlay = null;
    // reinitialize constant state
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(TAG, "controller %x %s -> %s: initialize", System.identityHashCode(this), mId, id);
    }
    mId = id;
    mCallerContext = callerContext;
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  @Override
  public void release() {
    mEventTracker.recordEvent(Event.ON_RELEASE_CONTROLLER);
    if (mRetryManager != null) {
      mRetryManager.reset();
    }
    if (mGestureDetector != null) {
      mGestureDetector.reset();
    }
    if (mSettableDraweeHierarchy != null) {
      mSettableDraweeHierarchy.reset();
    }
    releaseFetch();
  }

  private void releaseFetch() {
    boolean wasRequestSubmitted = mIsRequestSubmitted;
    mIsRequestSubmitted = false;
    mHasFetchFailed = false;
    if (mDataSource != null) {
      mDataSource.close();
      mDataSource = null;
    }
    if (mDrawable != null) {
      releaseDrawable(mDrawable);
    }
    if (mContentDescription != null) {
      mContentDescription = null;
    }
    mDrawable = null;
    if (mFetchedImage != null) {
      logMessageAndImage("release", mFetchedImage);
      releaseImage(mFetchedImage);
      mFetchedImage = null;
    }
    if (wasRequestSubmitted) {
      getControllerListener().onRelease(mId);
    }
  }

  /** Gets the controller id. */
  public String getId() {
    return mId;
  }

  /** Gets the analytic tag & caller context */
  public Object getCallerContext() {
    return mCallerContext;
  }

  /** Gets retry manager. */
  @ReturnsOwnership protected RetryManager getRetryManager() {
    if (mRetryManager == null) {
      mRetryManager = new RetryManager();
    }
    return mRetryManager;
  }

  /** Gets gesture detector. */
  protected @Nullable GestureDetector getGestureDetector() {
    return mGestureDetector;
  }

  /** Sets gesture detector. */
  protected void setGestureDetector(@Nullable GestureDetector gestureDetector) {
    mGestureDetector = gestureDetector;
    if (mGestureDetector != null) {
      mGestureDetector.setClickListener(this);
    }
  }

  /** Sets whether to display last available image in case of failure. */
  protected void setRetainImageOnFailure(boolean enabled) {
    mRetainImageOnFailure = enabled;
  }

  /** Gets accessibility content description. */
  @Override
  public @Nullable String getContentDescription() {
    return mContentDescription;
  }

  /** Sets accessibility content description. */
  @Override
  public void setContentDescription(@Nullable String contentDescription) {
    mContentDescription = contentDescription;
  }

  /** Adds controller listener. */
  public void addControllerListener(ControllerListener<? super INFO> controllerListener) {
    Preconditions.checkNotNull(controllerListener);
    if (mControllerListener instanceof InternalForwardingListener) {
      ((InternalForwardingListener<INFO>) mControllerListener).addListener(controllerListener);
      return;
    }
    if (mControllerListener != null) {
      mControllerListener = InternalForwardingListener.createInternal(
          mControllerListener,
          controllerListener);
      return;
    }
    // Listener only receives <INFO>, it never produces one.
    // That means if it can accept <? super INFO>, it can very well accept <INFO>.
    mControllerListener = (ControllerListener<INFO>) controllerListener;
  }

  /** Removes controller listener. */
  public void removeControllerListener(ControllerListener<? super INFO> controllerListener) {
    Preconditions.checkNotNull(controllerListener);
    if (mControllerListener instanceof InternalForwardingListener) {
      ((InternalForwardingListener<INFO>) mControllerListener).removeListener(controllerListener);
      return;
    }
    if (mControllerListener == controllerListener) {
      mControllerListener = null;
    }
  }

  /** Gets controller listener for internal use. */
  protected ControllerListener<INFO> getControllerListener() {
    if (mControllerListener == null) {
      return BaseControllerListener.getNoOpListener();
    }
    return mControllerListener;
  }

  /** Sets the controller viewport visibility listener */
  public void setControllerViewportVisibilityListener(
      @Nullable ControllerViewportVisibilityListener controllerViewportVisibilityListener) {
    mControllerViewportVisibilityListener = controllerViewportVisibilityListener;
  }

  /** Gets the hierarchy */
  @Override
  public @Nullable
  DraweeHierarchy getHierarchy() {
    return mSettableDraweeHierarchy;
  }

  /**
   * Sets the hierarchy.
   *
   * <p>The controller should be detached when this method is called.
   * @param hierarchy This must be an instance of {@link SettableDraweeHierarchy}
   */
  @Override
  public void setHierarchy(@Nullable DraweeHierarchy hierarchy) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(
          TAG,
          "controller %x %s: setHierarchy: %s",
          System.identityHashCode(this),
          mId,
          hierarchy);
    }
    mEventTracker.recordEvent(
        (hierarchy != null) ? Event.ON_SET_HIERARCHY : Event.ON_CLEAR_HIERARCHY);
    // force release in case request was submitted
    if (mIsRequestSubmitted) {
      mDeferredReleaser.cancelDeferredRelease(this);
      release();
    }
    // clear the existing hierarchy
    if (mSettableDraweeHierarchy != null) {
      mSettableDraweeHierarchy.setControllerOverlay(null);
      mSettableDraweeHierarchy = null;
    }
    // set the new hierarchy
    if (hierarchy != null) {
      Preconditions.checkArgument(hierarchy instanceof SettableDraweeHierarchy);
      mSettableDraweeHierarchy = (SettableDraweeHierarchy) hierarchy;
      mSettableDraweeHierarchy.setControllerOverlay(mControllerOverlay);
    }
  }

  /** Sets the controller overlay */
  protected void setControllerOverlay(@Nullable Drawable controllerOverlay) {
    mControllerOverlay = controllerOverlay;
    if (mSettableDraweeHierarchy != null) {
      mSettableDraweeHierarchy.setControllerOverlay(mControllerOverlay);
    }
  }

  /** Gets the controller overlay */
  protected @Nullable Drawable getControllerOverlay() {
    return mControllerOverlay;
  }

  @Override
  public void onAttach() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("AbstractDraweeController#onAttach");
    }
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(
          TAG,
          "controller %x %s: onAttach: %s",
          System.identityHashCode(this),
          mId,
          mIsRequestSubmitted ? "request already submitted" : "request needs submit");
    }
    mEventTracker.recordEvent(Event.ON_ATTACH_CONTROLLER);
    Preconditions.checkNotNull(mSettableDraweeHierarchy);
    mDeferredReleaser.cancelDeferredRelease(this);
    mIsAttached = true;
    if (!mIsRequestSubmitted) {
      submitRequest();
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  @Override
  public void onDetach() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("AbstractDraweeController#onDetach");
    }
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(TAG, "controller %x %s: onDetach", System.identityHashCode(this), mId);
    }
    mEventTracker.recordEvent(Event.ON_DETACH_CONTROLLER);
    mIsAttached = false;
    mDeferredReleaser.scheduleDeferredRelease(this);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  @Override
  public void onViewportVisibilityHint(boolean isVisibleInViewportHint) {
    final ControllerViewportVisibilityListener listener = mControllerViewportVisibilityListener;
    if (listener != null) {
      if (isVisibleInViewportHint && !mIsVisibleInViewportHint) {
        listener.onDraweeViewportEntry(mId);
      } else if (!isVisibleInViewportHint && mIsVisibleInViewportHint) {
        listener.onDraweeViewportExit(mId);
      }
    }
    mIsVisibleInViewportHint = isVisibleInViewportHint;
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(TAG, "controller %x %s: onTouchEvent %s", System.identityHashCode(this), mId, event);
    }
    if (mGestureDetector == null) {
      return false;
    }
    if (mGestureDetector.isCapturingGesture() || shouldHandleGesture()) {
      mGestureDetector.onTouchEvent(event);
      return true;
    }
    return false;
  }

  /** Returns whether the gesture should be handled by the controller */
  protected boolean shouldHandleGesture() {
    return shouldRetryOnTap();
  }

  private boolean shouldRetryOnTap() {
    // We should only handle touch event if we are expecting some gesture.
    // For example, we expect click when fetch fails and tap-to-retry is enabled.
    return mHasFetchFailed && mRetryManager != null && mRetryManager.shouldRetryOnTap();
  }

  @Override
  public boolean onClick() {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(TAG, "controller %x %s: onClick", System.identityHashCode(this), mId);
    }
    if (shouldRetryOnTap()) {
      mRetryManager.notifyTapToRetry();
      mSettableDraweeHierarchy.reset();
      submitRequest();
      return true;
    }
    return false;
  }

  protected void submitRequest() {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("AbstractDraweeController#submitRequest");
    }
    final T closeableImage = getCachedImage();
    if (closeableImage != null) {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("AbstractDraweeController#submitRequest->cache");
      }
      mDataSource = null;
      mIsRequestSubmitted = true;
      mHasFetchFailed = false;
      mEventTracker.recordEvent(Event.ON_SUBMIT_CACHE_HIT);
      getControllerListener().onSubmit(mId, mCallerContext);
      onImageLoadedFromCacheImmediately(mId, closeableImage);
      onNewResultInternal(mId, mDataSource, closeableImage, 1.0f, true, true, true);
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
      return;
    }
    mEventTracker.recordEvent(Event.ON_DATASOURCE_SUBMIT);
    getControllerListener().onSubmit(mId, mCallerContext);
    mSettableDraweeHierarchy.setProgress(0, true);
    mIsRequestSubmitted = true;
    mHasFetchFailed = false;
    mDataSource = getDataSource();
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(
          TAG,
          "controller %x %s: submitRequest: dataSource: %x",
          System.identityHashCode(this),
          mId,
          System.identityHashCode(mDataSource));
    }
    final String id = mId;
    final boolean wasImmediate = mDataSource.hasResult();
    final DataSubscriber<T> dataSubscriber =
        new BaseDataSubscriber<T>() {
          @Override
          public void onNewResultImpl(DataSource<T> dataSource) {
            // isFinished must be obtained before image, otherwise we might set intermediate result
            // as final image.
            boolean isFinished = dataSource.isFinished();
            boolean hasMultipleResults = dataSource.hasMultipleResults();
            float progress = dataSource.getProgress();
            T image = dataSource.getResult();
            if (image != null) {
              onNewResultInternal(
                  id, dataSource, image, progress, isFinished, wasImmediate, hasMultipleResults);
            } else if (isFinished) {
              onFailureInternal(id, dataSource, new NullPointerException(), /* isFinished */ true);
            }
          }

          @Override
          public void onFailureImpl(DataSource<T> dataSource) {
            onFailureInternal(id, dataSource, dataSource.getFailureCause(), /* isFinished */ true);
          }

          @Override
          public void onProgressUpdate(DataSource<T> dataSource) {
            boolean isFinished = dataSource.isFinished();
            float progress = dataSource.getProgress();
            onProgressUpdateInternal(id, dataSource, progress, isFinished);
          }
        };
    mDataSource.subscribe(dataSubscriber, mUiThreadImmediateExecutor);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  private void onNewResultInternal(
      String id,
      DataSource<T> dataSource,
      @Nullable T image,
      float progress,
      boolean isFinished,
      boolean wasImmediate,
      boolean deliverTempResult) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("AbstractDraweeController#onNewResultInternal");
      }
      // ignore late callbacks (data source that returned the new result is not the one we expected)
      if (!isExpectedDataSource(id, dataSource)) {
        logMessageAndImage("ignore_old_datasource @ onNewResult", image);
        releaseImage(image);
        dataSource.close();
        return;
      }
      mEventTracker.recordEvent(
          isFinished ? Event.ON_DATASOURCE_RESULT : Event.ON_DATASOURCE_RESULT_INT);
      // create drawable
      Drawable drawable;
      try {
        drawable = createDrawable(image);
      } catch (Exception exception) {
        logMessageAndImage("drawable_failed @ onNewResult", image);
        releaseImage(image);
        onFailureInternal(id, dataSource, exception, isFinished);
        return;
      }
      T previousImage = mFetchedImage;
      Drawable previousDrawable = mDrawable;
      mFetchedImage = image;
      mDrawable = drawable;
      try {
        // set the new image
        if (isFinished) {
          logMessageAndImage("set_final_result @ onNewResult", image);
          mDataSource = null;
          mSettableDraweeHierarchy.setImage(drawable, 1f, wasImmediate);
          getControllerListener().onFinalImageSet(id, getImageInfo(image), getAnimatable());
        } else if (deliverTempResult) {
          logMessageAndImage("set_temporary_result @ onNewResult", image);
          mSettableDraweeHierarchy.setImage(drawable, 1f, wasImmediate);
          getControllerListener().onFinalImageSet(id, getImageInfo(image), getAnimatable());
          // IMPORTANT: do not execute any instance-specific code after this point
        } else {
          logMessageAndImage("set_intermediate_result @ onNewResult", image);
          mSettableDraweeHierarchy.setImage(drawable, progress, wasImmediate);
          getControllerListener().onIntermediateImageSet(id, getImageInfo(image));
          // IMPORTANT: do not execute any instance-specific code after this point
        }
      } finally {
        if (previousDrawable != null && previousDrawable != drawable) {
          releaseDrawable(previousDrawable);
        }
        if (previousImage != null && previousImage != image) {
          logMessageAndImage("release_previous_result @ onNewResult", previousImage);
          releaseImage(previousImage);
        }
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  private void onFailureInternal(
      String id, DataSource<T> dataSource, Throwable throwable, boolean isFinished) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("AbstractDraweeController#onFailureInternal");
    }
    // ignore late callbacks (data source that failed is not the one we expected)
    if (!isExpectedDataSource(id, dataSource)) {
      logMessageAndFailure("ignore_old_datasource @ onFailure", throwable);
      dataSource.close();
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
      return;
    }
    mEventTracker.recordEvent(
        isFinished ? Event.ON_DATASOURCE_FAILURE : Event.ON_DATASOURCE_FAILURE_INT);
    // fail only if the data source is finished
    if (isFinished) {
      logMessageAndFailure("final_failed @ onFailure", throwable);
      mDataSource = null;
      mHasFetchFailed = true;
      // Set the previously available image if available.
      if (mRetainImageOnFailure && mDrawable != null) {
        mSettableDraweeHierarchy.setImage(mDrawable, 1f, true);
      } else if (shouldRetryOnTap()) {
        mSettableDraweeHierarchy.setRetry(throwable);
      } else {
        mSettableDraweeHierarchy.setFailure(throwable);
      }
      getControllerListener().onFailure(mId, throwable);
      // IMPORTANT: do not execute any instance-specific code after this point
    } else {
      logMessageAndFailure("intermediate_failed @ onFailure", throwable);
      getControllerListener().onIntermediateImageFailed(mId, throwable);
      // IMPORTANT: do not execute any instance-specific code after this point
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  private void onProgressUpdateInternal(
      String id,
      DataSource<T> dataSource,
      float progress,
      boolean isFinished) {
    // ignore late callbacks (data source that failed is not the one we expected)
    if (!isExpectedDataSource(id, dataSource)) {
      logMessageAndFailure("ignore_old_datasource @ onProgress", null);
      dataSource.close();
      return;
    }
    if (!isFinished) {
      mSettableDraweeHierarchy.setProgress(progress, false);
    }
  }

  private boolean isExpectedDataSource(String id, DataSource<T> dataSource) {
    if (dataSource == null && mDataSource == null) {
      // DataSource is null when we use directly the Bitmap from the MemoryCache. In this case
      // we don't have to close the DataSource.
      return true;
    }
    // There are several situations in which an old data source might return a result that we are no
    // longer interested in. To verify that the result is indeed expected, we check several things:
    return id.equals(mId) && dataSource == mDataSource && mIsRequestSubmitted;
  }

  private void logMessageAndImage(String messageAndMethod, T image) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(
          TAG,
          "controller %x %s: %s: image: %s %x",
          System.identityHashCode(this),
          mId,
          messageAndMethod,
          getImageClass(image),
          getImageHash(image));
    }
  }

  private void logMessageAndFailure(String messageAndMethod, Throwable throwable) {
    if (FLog.isLoggable(FLog.VERBOSE)) {
      FLog.v(
          TAG,
          "controller %x %s: %s: failure: %s",
          System.identityHashCode(this),
          mId,
          messageAndMethod,
          throwable);
    }
  }

  @Override
  public @Nullable Animatable getAnimatable() {
    return (mDrawable instanceof Animatable) ? (Animatable) mDrawable : null;
  }

  protected abstract DataSource<T> getDataSource();

  protected abstract Drawable createDrawable(T image);

  protected abstract @Nullable INFO getImageInfo(T image);

  protected String getImageClass(@Nullable T image) {
    return (image != null) ? image.getClass().getSimpleName() : "<null>";
  }

  protected int getImageHash(@Nullable T image) {
    return System.identityHashCode(image);
  }

  protected abstract void releaseImage(@Nullable T image);

  protected abstract void releaseDrawable(@Nullable Drawable drawable);

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("isAttached", mIsAttached)
        .add("isRequestSubmitted", mIsRequestSubmitted)
        .add("hasFetchFailed", mHasFetchFailed)
        .add("fetchedImage", getImageHash(mFetchedImage))
        .add("events", mEventTracker.toString())
        .toString();
  }

  protected @Nullable T getCachedImage() {
    return null;
  }

  protected void onImageLoadedFromCacheImmediately(String id, T cachedImage) {}
}
