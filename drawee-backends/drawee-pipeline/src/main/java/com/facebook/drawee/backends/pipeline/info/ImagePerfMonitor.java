/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline.info;

import android.graphics.Rect;
import com.facebook.common.internal.Supplier;
import com.facebook.common.time.MonotonicClock;
import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.backends.pipeline.info.internal.ImagePerfControllerListener2;
import com.facebook.drawee.backends.pipeline.info.internal.ImagePerfRequestListener;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.fresco.ui.common.ImageLoadStatus;
import com.facebook.fresco.ui.common.ImagePerfData;
import com.facebook.fresco.ui.common.ImagePerfDataListener;
import com.facebook.fresco.ui.common.ImagePerfNotifier;
import com.facebook.fresco.ui.common.ImagePerfState;
import com.facebook.fresco.ui.common.VisibilityState;
import com.facebook.imagepipeline.listener.ForwardingRequestListener;
import com.facebook.infer.annotation.Nullsafe;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class ImagePerfMonitor implements ImagePerfNotifier {

  private final PipelineDraweeController mPipelineDraweeController;
  private final MonotonicClock mMonotonicClock;
  private final ImagePerfState mImagePerfState;
  private final Supplier<Boolean> mAsyncLogging;

  private @Nullable ImagePerfRequestListener mImagePerfRequestListener;
  private @Nullable ImagePerfControllerListener2 mImagePerfControllerListener2;
  private @Nullable ForwardingRequestListener mForwardingRequestListener;

  private @Nullable List<ImagePerfDataListener> mImagePerfDataListeners;

  private boolean mEnabled;

  public ImagePerfMonitor(
      MonotonicClock monotonicClock,
      PipelineDraweeController pipelineDraweeController,
      Supplier<Boolean> asyncLogging) {
    mMonotonicClock = monotonicClock;
    mPipelineDraweeController = pipelineDraweeController;
    mImagePerfState = new ImagePerfState();
    mAsyncLogging = asyncLogging;
  }

  public void setEnabled(boolean enabled) {
    mEnabled = enabled;
    if (enabled) {
      setupListeners();
      if (mImagePerfControllerListener2 != null) {
        mPipelineDraweeController.addControllerListener2(mImagePerfControllerListener2);
      }
      if (mForwardingRequestListener != null) {
        mPipelineDraweeController.addRequestListener(mForwardingRequestListener);
      }
    } else {
      if (mImagePerfControllerListener2 != null) {
        mPipelineDraweeController.removeControllerListener2(mImagePerfControllerListener2);
      }
      if (mForwardingRequestListener != null) {
        mPipelineDraweeController.removeRequestListener(mForwardingRequestListener);
      }
    }
  }

  public void addImagePerfDataListener(@Nullable ImagePerfDataListener imagePerfDataListener) {
    if (imagePerfDataListener == null) {
      return;
    }
    if (mImagePerfDataListeners == null) {
      mImagePerfDataListeners = new CopyOnWriteArrayList<>();
    }
    mImagePerfDataListeners.add(imagePerfDataListener);
  }

  public void removeImagePerfDataListener(ImagePerfDataListener imagePerfDataListener) {
    if (mImagePerfDataListeners == null) {
      return;
    }
    mImagePerfDataListeners.remove(imagePerfDataListener);
  }

  public void clearImagePerfDataListeners() {
    if (mImagePerfDataListeners != null) {
      mImagePerfDataListeners.clear();
    }
  }

  @Override
  public void notifyStatusUpdated(ImagePerfState state, ImageLoadStatus imageLoadStatus) {
    state.setImageLoadStatus(imageLoadStatus);
    if (!mEnabled || mImagePerfDataListeners == null || mImagePerfDataListeners.isEmpty()) {
      return;
    }
    if (imageLoadStatus == ImageLoadStatus.SUCCESS) {
      addViewportData();
    }
    ImagePerfData data = state.snapshot();
    for (ImagePerfDataListener listener : mImagePerfDataListeners) {
      listener.onImageLoadStatusUpdated(data, imageLoadStatus);
    }
  }

  @Override
  public void notifyListenersOfVisibilityStateUpdate(
      ImagePerfState state, VisibilityState visibilityState) {
    if (!mEnabled || mImagePerfDataListeners == null || mImagePerfDataListeners.isEmpty()) {
      return;
    }

    ImagePerfData data = state.snapshot();
    for (ImagePerfDataListener listener : mImagePerfDataListeners) {
      listener.onImageVisibilityUpdated(data, visibilityState);
    }
  }

  public void addViewportData() {
    DraweeHierarchy hierarchy = mPipelineDraweeController.getHierarchy();
    if (hierarchy != null && hierarchy.getTopLevelDrawable() != null) {
      Rect bounds = hierarchy.getTopLevelDrawable().getBounds();
      mImagePerfState.setOnScreenWidth(bounds.width());
      mImagePerfState.setOnScreenHeight(bounds.height());
    }
  }

  private void setupListeners() {
    if (mImagePerfControllerListener2 == null) {
      mImagePerfControllerListener2 =
          new ImagePerfControllerListener2(mMonotonicClock, mImagePerfState, this, mAsyncLogging);
    }
    if (mImagePerfRequestListener == null) {
      mImagePerfRequestListener = new ImagePerfRequestListener(mMonotonicClock, mImagePerfState);
    }
    if (mForwardingRequestListener == null) {
      mForwardingRequestListener = new ForwardingRequestListener(mImagePerfRequestListener);
    }
  }

  public void reset() {
    clearImagePerfDataListeners();
    setEnabled(false);
    mImagePerfState.reset();
  }
}
