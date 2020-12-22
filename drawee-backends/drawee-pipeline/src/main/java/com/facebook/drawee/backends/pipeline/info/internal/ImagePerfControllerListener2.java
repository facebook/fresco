/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline.info.internal;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.time.MonotonicClock;
import com.facebook.drawee.backends.pipeline.info.ImageLoadStatus;
import com.facebook.drawee.backends.pipeline.info.ImagePerfNotifier;
import com.facebook.drawee.backends.pipeline.info.ImagePerfState;
import com.facebook.drawee.backends.pipeline.info.VisibilityState;
import com.facebook.fresco.ui.common.BaseControllerListener2;
import com.facebook.fresco.ui.common.ControllerListener2;
import com.facebook.fresco.ui.common.DimensionsInfo;
import com.facebook.fresco.ui.common.OnDrawControllerListener;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class ImagePerfControllerListener2 extends BaseControllerListener2<ImageInfo>
    implements OnDrawControllerListener<ImageInfo> {

  private static final String TAG = "ImagePerfControllerListener2";

  private static final int WHAT_STATUS = 1;
  private static final int WHAT_VISIBILITY = 2;

  private final MonotonicClock mClock;
  private final ImagePerfState mImagePerfState;
  private final ImagePerfNotifier mImagePerfNotifier;
  private final Supplier<Boolean> mAsyncLogging;
  private final Supplier<Boolean> mUseNewState;

  private @Nullable Handler mHandler;

  static class LogHandler extends Handler {

    private final ImagePerfNotifier mNotifier;

    public LogHandler(@NonNull Looper looper, @NonNull ImagePerfNotifier notifier) {
      super(looper);
      mNotifier = notifier;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
      ImagePerfState state = (ImagePerfState) Preconditions.checkNotNull(msg.obj);
      switch (msg.what) {
        case WHAT_STATUS:
          mNotifier.notifyStatusUpdated(state, msg.arg1);
          break;
        case WHAT_VISIBILITY:
          mNotifier.notifyListenersOfVisibilityStateUpdate(state, msg.arg1);
          break;
      }
    }
  };

  public ImagePerfControllerListener2(
      MonotonicClock clock,
      ImagePerfState imagePerfState,
      ImagePerfNotifier imagePerfNotifier,
      Supplier<Boolean> asyncLogging,
      Supplier<Boolean> useNewState) {
    mClock = clock;
    mImagePerfState = imagePerfState;
    mImagePerfNotifier = imagePerfNotifier;

    mAsyncLogging = asyncLogging;
    mUseNewState = useNewState;
  }

  @Override
  public void onSubmit(
      String id, @Nullable Object callerContext, @Nullable ControllerListener2.Extras extraData) {
    final long now = mClock.now();

    ImagePerfState state = obtainState();
    state.resetPointsTimestamps();

    state.setControllerSubmitTimeMs(now);
    state.setControllerId(id);
    state.setCallerContext(callerContext);

    state.setExtraData(extraData);

    updateStatus(state, ImageLoadStatus.REQUESTED);
    reportViewVisible(state, now);
  }

  @Override
  public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
    final long now = mClock.now();

    ImagePerfState state = obtainState();

    state.setControllerIntermediateImageSetTimeMs(now);
    state.setControllerId(id);
    state.setImageInfo(imageInfo);

    updateStatus(state, ImageLoadStatus.INTERMEDIATE_AVAILABLE);
  }

  @Override
  public void onFinalImageSet(
      String id, @Nullable ImageInfo imageInfo, @Nullable ControllerListener2.Extras extraData) {
    final long now = mClock.now();

    ImagePerfState state = obtainState();

    state.setExtraData(extraData);

    state.setControllerFinalImageSetTimeMs(now);
    state.setImageRequestEndTimeMs(now);
    state.setControllerId(id);
    state.setImageInfo(imageInfo);

    updateStatus(state, ImageLoadStatus.SUCCESS);
  }

  @Override
  public void onFailure(
      String id, Throwable throwable, @Nullable ControllerListener2.Extras extras) {
    final long now = mClock.now();

    ImagePerfState state = obtainState();

    state.setExtraData(extras);

    state.setControllerFailureTimeMs(now);
    state.setControllerId(id);
    state.setErrorThrowable(throwable);

    updateStatus(state, ImageLoadStatus.ERROR);

    reportViewInvisible(state, now);
  }

  @Override
  public void onRelease(String id, @Nullable ControllerListener2.Extras extras) {
    final long now = mClock.now();

    ImagePerfState state = obtainState();

    state.setExtraData(extras);
    state.setControllerId(id);

    int lastImageLoadStatus = state.getImageLoadStatus();
    if (lastImageLoadStatus != ImageLoadStatus.SUCCESS
        && lastImageLoadStatus != ImageLoadStatus.ERROR
        && lastImageLoadStatus != ImageLoadStatus.DRAW) {
      state.setControllerCancelTimeMs(now);
      // The image request was canceled
      updateStatus(state, ImageLoadStatus.CANCELED);
    }

    reportViewInvisible(state, now);
  }

  @Override
  public void onImageDrawn(String id, ImageInfo info, DimensionsInfo dimensionsInfo) {
    ImagePerfState state = obtainState();

    state.setControllerId(id);
    state.setImageDrawTimeMs(mClock.now());
    state.setDimensionsInfo(dimensionsInfo);
    updateStatus(state, ImageLoadStatus.DRAW);
  }

  @VisibleForTesting
  public void reportViewVisible(ImagePerfState state, long now) {
    state.setVisible(true);
    state.setVisibilityEventTimeMs(now);

    updateVisibility(state, VisibilityState.VISIBLE);
  }

  public void resetState() {
    obtainState().reset();
  }

  @VisibleForTesting
  private void reportViewInvisible(ImagePerfState state, long time) {
    state.setVisible(false);
    state.setInvisibilityEventTimeMs(time);

    updateVisibility(state, VisibilityState.INVISIBLE);
  }

  private void updateStatus(ImagePerfState state, @ImageLoadStatus int imageLoadStatus) {
    if (shouldDispatchAsync()) {
      Message msg = Preconditions.checkNotNull(mHandler).obtainMessage();
      msg.what = WHAT_STATUS;
      msg.arg1 = imageLoadStatus;
      msg.obj = state;
      mHandler.sendMessage(msg);
    } else {
      mImagePerfNotifier.notifyStatusUpdated(state, imageLoadStatus);
    }
  }

  private void updateVisibility(ImagePerfState state, @VisibilityState int visibilityState) {
    if (shouldDispatchAsync()) {
      Message msg = Preconditions.checkNotNull(mHandler).obtainMessage();
      msg.what = WHAT_VISIBILITY;
      msg.arg1 = visibilityState;
      msg.obj = state;
      mHandler.sendMessage(msg);
    } else {
      // sync
      mImagePerfNotifier.notifyListenersOfVisibilityStateUpdate(state, visibilityState);
    }
  }

  private synchronized void initHandler() {
    if (mHandler != null) {
      return;
    }
    HandlerThread handlerThread = new HandlerThread("ImagePerfControllerListener2Thread");
    handlerThread.start();
    Looper looper = Preconditions.checkNotNull(handlerThread.getLooper());
    mHandler = new LogHandler(looper, mImagePerfNotifier);
  }

  private boolean shouldDispatchAsync() {
    boolean enabled = mAsyncLogging.get();
    if (enabled && mHandler == null) {
      initHandler();
    }
    return enabled;
  }

  private ImagePerfState obtainState() {
    return mUseNewState.get() ? new ImagePerfState() : mImagePerfState;
  }
}
