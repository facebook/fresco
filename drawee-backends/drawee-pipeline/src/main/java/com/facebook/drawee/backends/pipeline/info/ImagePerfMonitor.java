/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline.info;

import android.graphics.Rect;
import com.facebook.common.internal.Supplier;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.time.MonotonicClock;
import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.backends.pipeline.info.internal.ImagePerfControllerListener2;
import com.facebook.drawee.backends.pipeline.info.internal.ImagePerfImageOriginListener;
import com.facebook.drawee.backends.pipeline.info.internal.ImagePerfRequestListener;
import com.facebook.drawee.controller.AbstractDraweeControllerBuilder;
import com.facebook.drawee.interfaces.DraweeHierarchy;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.listener.ForwardingRequestListener;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.Nullable;

public class ImagePerfMonitor implements ImagePerfNotifier {

  private final PipelineDraweeController mPipelineDraweeController;
  private final MonotonicClock mMonotonicClock;
  private final ImagePerfState mImagePerfState;
  private final Supplier<Boolean> mAsyncLogging;

  private @Nullable ImageOriginRequestListener mImageOriginRequestListener;
  private @Nullable ImageOriginListener mImageOriginListener;
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

  public void updateImageRequestData(
      AbstractDraweeControllerBuilder<
              PipelineDraweeControllerBuilder,
              ImageRequest,
              CloseableReference<CloseableImage>,
              ImageInfo>
          pipelineDraweeControllerBuilder) {
    mImagePerfState.setControllerImageRequests(
        pipelineDraweeControllerBuilder.getImageRequest(),
        pipelineDraweeControllerBuilder.getLowResImageRequest(),
        pipelineDraweeControllerBuilder.getFirstAvailableImageRequests());
  }

  public void setEnabled(boolean enabled) {
    mEnabled = enabled;
    if (enabled) {
      setupListeners();
      if (mImageOriginListener != null) {
        mPipelineDraweeController.addImageOriginListener(mImageOriginListener);
      }
      if (mImagePerfControllerListener2 != null) {
        mPipelineDraweeController.addControllerListener2(mImagePerfControllerListener2);
      }
      if (mForwardingRequestListener != null) {
        mPipelineDraweeController.addRequestListener(mForwardingRequestListener);
      }
    } else {
      if (mImageOriginListener != null) {
        mPipelineDraweeController.removeImageOriginListener(mImageOriginListener);
      }
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
  public void notifyStatusUpdated(ImagePerfState state, @ImageLoadStatus int imageLoadStatus) {
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
      ImagePerfState state, @VisibilityState int visibilityState) {
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
