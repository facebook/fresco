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
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.time.MonotonicClock;
import com.facebook.drawee.backends.pipeline.info.ImageLoadStatus;
import com.facebook.drawee.backends.pipeline.info.ImagePerfNotifier;
import com.facebook.drawee.backends.pipeline.info.ImagePerfState;
import com.facebook.drawee.backends.pipeline.info.VisibilityState;
import com.facebook.fresco.ui.common.BaseControllerListener2;
import com.facebook.fresco.ui.common.DimensionsInfo;
import com.facebook.fresco.ui.common.OnDrawControllerListener;
import com.facebook.imagepipeline.image.ImageInfo;
import javax.annotation.Nullable;

public class ImagePerfControllerListener2 extends BaseControllerListener2<ImageInfo>
    implements OnDrawControllerListener<ImageInfo> {

  private static final String TAG = "ImagePerfControllerListener2";

  private static final int WHAT_STATUS = 1;
  private static final int WHAT_VISIBILITY = 2;

  private final MonotonicClock mClock;
  private final ImagePerfState mImagePerfState;
  private final ImagePerfNotifier mImagePerfNotifier;
  private final Supplier<Boolean> mAsyncLogging;

  private @Nullable Handler mHandler;

  static class LogHandler extends Handler {

    private final ImagePerfNotifier mNotifier;

    public LogHandler(@NonNull Looper looper, @NonNull ImagePerfNotifier notifier) {
      super(looper);
      mNotifier = notifier;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
      switch (msg.what) {
        case WHAT_STATUS:
          mNotifier.notifyStatusUpdated((ImagePerfState) msg.obj, msg.arg1);
          break;
        case WHAT_VISIBILITY:
          mNotifier.notifyListenersOfVisibilityStateUpdate((ImagePerfState) msg.obj, msg.arg1);
          break;
      }
    }
  };

  public ImagePerfControllerListener2(
      MonotonicClock clock,
      ImagePerfState imagePerfState,
      ImagePerfNotifier imagePerfNotifier,
      Supplier<Boolean> asyncLogging) {
    mClock = clock;
    mImagePerfState = imagePerfState;
    mImagePerfNotifier = imagePerfNotifier;

    mAsyncLogging = asyncLogging;
  }

  @Override
  public void onSubmit(String id, @Nullable Object callerContext, @Nullable Extras extraData) {
    final long now = mClock.now();

    mImagePerfState.resetPointsTimestamps();

    mImagePerfState.setControllerSubmitTimeMs(now);
    mImagePerfState.setControllerId(id);
    mImagePerfState.setCallerContext(callerContext);

    mImagePerfState.setExtraData(extraData);

    updateStatus(ImageLoadStatus.REQUESTED);
    reportViewVisible(now);
  }

  @Override
  public void onIntermediateImageSet(String id, @Nullable ImageInfo imageInfo) {
    final long now = mClock.now();

    mImagePerfState.setControllerIntermediateImageSetTimeMs(now);
    mImagePerfState.setControllerId(id);
    mImagePerfState.setImageInfo(imageInfo);

    updateStatus(ImageLoadStatus.INTERMEDIATE_AVAILABLE);
  }

  @Override
  public void onFinalImageSet(
      String id, @Nullable ImageInfo imageInfo, @Nullable Extras extraData) {
    final long now = mClock.now();

    mImagePerfState.setExtraData(extraData);

    mImagePerfState.setControllerFinalImageSetTimeMs(now);
    mImagePerfState.setImageRequestEndTimeMs(now);
    mImagePerfState.setControllerId(id);
    mImagePerfState.setImageInfo(imageInfo);

    updateStatus(ImageLoadStatus.SUCCESS);
  }

  @Override
  public void onFailure(String id, Throwable throwable, @Nullable Extras extras) {
    final long now = mClock.now();

    mImagePerfState.setExtraData(extras);

    mImagePerfState.setControllerFailureTimeMs(now);
    mImagePerfState.setControllerId(id);
    mImagePerfState.setErrorThrowable(throwable);

    updateStatus(ImageLoadStatus.ERROR);

    reportViewInvisible(now);
  }

  @Override
  public void onRelease(String id, Extras extras) {
    final long now = mClock.now();

    mImagePerfState.setExtraData(extras);

    int lastImageLoadStatus = mImagePerfState.getImageLoadStatus();
    if (lastImageLoadStatus != ImageLoadStatus.SUCCESS
        && lastImageLoadStatus != ImageLoadStatus.ERROR
        && lastImageLoadStatus != ImageLoadStatus.DRAW) {
      mImagePerfState.setControllerCancelTimeMs(now);
      mImagePerfState.setControllerId(id);
      // The image request was canceled
      updateStatus(ImageLoadStatus.CANCELED);
    }

    reportViewInvisible(now);
  }

  @Override
  public void onImageDrawn(String id, ImageInfo info, DimensionsInfo dimensionsInfo) {
    mImagePerfState.setImageDrawTimeMs(mClock.now());
    mImagePerfState.setDimensionsInfo(dimensionsInfo);
    updateStatus(ImageLoadStatus.DRAW);
  }

  @VisibleForTesting
  public void reportViewVisible(long now) {
    mImagePerfState.setVisible(true);
    mImagePerfState.setVisibilityEventTimeMs(now);

    updateVisibility(VisibilityState.VISIBLE);
  }

  @VisibleForTesting
  private void reportViewInvisible(long time) {
    mImagePerfState.setVisible(false);
    mImagePerfState.setInvisibilityEventTimeMs(time);

    updateVisibility(VisibilityState.INVISIBLE);
  }

  private void updateStatus(@ImageLoadStatus int imageLoadStatus) {
    if (shouldDispatchAsync()) {
      Message msg = mHandler.obtainMessage();
      msg.what = WHAT_STATUS;
      msg.arg1 = imageLoadStatus;
      msg.obj = mImagePerfState;
      mHandler.sendMessage(msg);
    } else {
      mImagePerfNotifier.notifyStatusUpdated(mImagePerfState, imageLoadStatus);
    }
  }

  private void updateVisibility(@VisibilityState int visibilityState) {
    if (shouldDispatchAsync()) {
      Message msg = mHandler.obtainMessage();
      msg.what = WHAT_VISIBILITY;
      msg.arg1 = visibilityState;
      msg.obj = mImagePerfState;
      mHandler.sendMessage(msg);
    } else {
      // sync
      mImagePerfNotifier.notifyListenersOfVisibilityStateUpdate(mImagePerfState, visibilityState);
    }
  }

  private synchronized void initHandler() {
    if (mHandler != null) {
      return;
    }
    HandlerThread handlerThread = new HandlerThread("ImagePerfControllerListener2Thread");
    handlerThread.start();
    mHandler = new LogHandler(handlerThread.getLooper(), mImagePerfNotifier);
  }

  private boolean shouldDispatchAsync() {
    boolean enabled = mAsyncLogging.get();
    if (enabled && mHandler == null) {
      initHandler();
    }
    return enabled;
  }
}
