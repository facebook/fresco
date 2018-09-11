/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.backends.pipeline.info;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.facebook.common.time.MonotonicClock;
import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.backends.pipeline.info.internal.ImagePerfControllerListener;
import com.facebook.drawee.backends.pipeline.info.internal.ImagePerfImageOriginListener;
import com.facebook.imagepipeline.listener.RequestListener;
import org.junit.Before;
import org.junit.Test;

/** Tests {@link ImagePerfMonitor} */
public class ImagePerfMonitorTest {

  private static final String CONTROLLER_ID = "abc";

  private MonotonicClock mMonotonicClock;
  private PipelineDraweeController mController;

  private ImagePerfMonitor mImagePerfMonitor;

  @Before
  public void setUp() {
    mMonotonicClock = mock(MonotonicClock.class);
    mController = mock(PipelineDraweeController.class);
    when(mController.getId()).thenReturn(CONTROLLER_ID);

    mImagePerfMonitor = new ImagePerfMonitor(mMonotonicClock, mController);
  }

  @Test
  public void testSetEnabled() {
    mImagePerfMonitor.setEnabled(true);

    verify(mController).addImageOriginListener(any(ImagePerfImageOriginListener.class));
    verify(mController).addControllerListener(any(ImagePerfControllerListener.class));
    verify(mController).addRequestListener(any(RequestListener.class));
    verify(mController).getId();
    verifyNoMoreInteractions(mController);
  }

  @Test
  public void testEnableDisable() {
    mImagePerfMonitor.setEnabled(true);
    mImagePerfMonitor.setEnabled(false);

    verify(mController).addImageOriginListener(any(ImagePerfImageOriginListener.class));
    verify(mController).addControllerListener(any(ImagePerfControllerListener.class));
    verify(mController).addRequestListener(any(RequestListener.class));
    verify(mController).getId();
    verify(mController).removeImageOriginListener(any(ImagePerfImageOriginListener.class));
    verify(mController).removeControllerListener(any(ImagePerfControllerListener.class));
    verify(mController).removeRequestListener(any(RequestListener.class));
    verifyNoMoreInteractions(mController);
  }

  @Test
  public void testNotifyListeners_whenNoListener_thenDoNothing() {
    ImagePerfState state = mock(ImagePerfState.class);
    mImagePerfMonitor.setEnabled(true);
    mImagePerfMonitor.notifyStatusUpdated(state, ImageLoadStatus.SUCCESS);

    verify(state).setImageLoadStatus(eq(ImageLoadStatus.SUCCESS));
    verifyNoMoreInteractions(state);
  }

  @Test
  public void testNotifyListeners_whenListenerSet_thenNotifyListener() {
    ImagePerfState state = mock(ImagePerfState.class);
    ImagePerfDataListener listener = mock(ImagePerfDataListener.class);

    mImagePerfMonitor.setEnabled(true);
    mImagePerfMonitor.addImagePerfDataListener(listener);
    mImagePerfMonitor.notifyStatusUpdated(state, ImageLoadStatus.SUCCESS);

    verify(state).setImageLoadStatus(eq(ImageLoadStatus.SUCCESS));
    verify(listener).onImageLoadStatusUpdated(any(ImagePerfData.class), eq(ImageLoadStatus.SUCCESS));
  }

  @Test
  public void testNotifyListeners_whenDisabled_thenDoNothing() {
    ImagePerfState state = mock(ImagePerfState.class);
    ImagePerfDataListener listener = mock(ImagePerfDataListener.class);

    mImagePerfMonitor.setEnabled(false);
    mImagePerfMonitor.addImagePerfDataListener(listener);
    mImagePerfMonitor.notifyStatusUpdated(state, ImageLoadStatus.SUCCESS);

    verify(state).setImageLoadStatus(eq(ImageLoadStatus.SUCCESS));
    verifyNoMoreInteractions(listener);
  }
}
