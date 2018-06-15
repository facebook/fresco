/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

import com.facebook.common.time.MonotonicClock;
import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.backends.pipeline.info.internal.ImagePerfControllerListener;
import com.facebook.drawee.backends.pipeline.info.internal.ImagePerfImageOriginListener;
import com.facebook.drawee.backends.pipeline.info.internal.ImagePerfRequestListener;
import com.facebook.imagepipeline.listener.BaseRequestListener;
import com.facebook.imagepipeline.listener.ForwardingRequestListener;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;

public class ImagePerfMonitor extends BaseRequestListener {

  private final PipelineDraweeController mPipelineDraweeController;
  private final MonotonicClock mMonotonicClock;
  private final ImagePerfState mImagePerfState;

  private @Nullable ImageOriginRequestListener mImageOriginRequestListener;
  private @Nullable ImageOriginListener mImageOriginListener;
  private @Nullable ImagePerfRequestListener mImagePerfRequestListener;
  private @Nullable ImagePerfControllerListener mImagePerfControllerListener;
  private @Nullable ForwardingRequestListener mForwardingRequestListener;

  private @Nullable List<ImagePerfDataListener> mImagePerfDataListeners;

  private boolean mEnabled;

  public ImagePerfMonitor(
      MonotonicClock monotonicClock, PipelineDraweeController pipelineDraweeController) {
    mMonotonicClock = monotonicClock;
    mPipelineDraweeController = pipelineDraweeController;
    mImagePerfState = new ImagePerfState();
  }

  public void setEnabled(boolean enabled) {
    mEnabled = enabled;
    if (enabled) {
      setupListeners();
      if (mImageOriginListener != null) {
        mPipelineDraweeController.addImageOriginListener(mImageOriginListener);
      }
      if (mImagePerfControllerListener != null) {
        mPipelineDraweeController.addControllerListener(mImagePerfControllerListener);
      }
      if (mForwardingRequestListener != null) {
        mPipelineDraweeController.addRequestListener(mForwardingRequestListener);
      }
    } else {
      if (mImageOriginListener != null) {
        mPipelineDraweeController.removeImageOriginListener(mImageOriginListener);
      }
      if (mImagePerfControllerListener != null) {
        mPipelineDraweeController.removeControllerListener(mImagePerfControllerListener);
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
      mImagePerfDataListeners = new LinkedList<>();
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

  public void notifyListeners(ImagePerfState state, @ImageLoadStatus int imageLoadStatus) {
    mImagePerfState.setImageLoadStatus(imageLoadStatus);
    if (!mEnabled || mImagePerfDataListeners == null || mImagePerfDataListeners.isEmpty()) {
      return;
    }
    ImagePerfData data = state.snapshot();
    for (ImagePerfDataListener listener : mImagePerfDataListeners) {
      listener.onImagePerfDataUpdated(data, imageLoadStatus);
    }
  }

  private void setupListeners() {
    if (mImagePerfControllerListener == null) {
      mImagePerfControllerListener =
          new ImagePerfControllerListener(mMonotonicClock, mImagePerfState, this);
    }
    if (mImagePerfRequestListener == null) {
      mImagePerfRequestListener = new ImagePerfRequestListener(mMonotonicClock, mImagePerfState);
    }
    if (mImageOriginListener == null) {
      mImageOriginListener = new ImagePerfImageOriginListener(mImagePerfState, this);
    }
    if (mImageOriginRequestListener == null) {
      mImageOriginRequestListener =
          new ImageOriginRequestListener(mPipelineDraweeController.getId(), mImageOriginListener);
    } else {
      // The ID could have changed
      mImageOriginRequestListener.init(mPipelineDraweeController.getId());
    }
    if (mForwardingRequestListener == null) {
      mForwardingRequestListener =
          new ForwardingRequestListener(mImagePerfRequestListener, mImageOriginRequestListener);
    }
  }

  public void reset() {
    clearImagePerfDataListeners();
    setEnabled(false);
    mImagePerfState.reset();
  }
}
