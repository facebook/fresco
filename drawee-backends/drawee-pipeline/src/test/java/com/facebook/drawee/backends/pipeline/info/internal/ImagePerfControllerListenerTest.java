/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.backends.pipeline.info.internal;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.facebook.common.time.MonotonicClock;
import com.facebook.drawee.backends.pipeline.info.ImageLoadStatus;
import com.facebook.drawee.backends.pipeline.info.ImagePerfMonitor;
import com.facebook.drawee.backends.pipeline.info.ImagePerfState;
import org.junit.Before;
import org.junit.Test;

/** Tests {@link ImagePerfControllerListener} */
public class ImagePerfControllerListenerTest {

  private static final String CONTROLLER_ID = "abc";
  private static final Object CALLER_CONTEXT = "callerContext";

  private MonotonicClock mMonotonicClock;
  private ImagePerfMonitor mImagePerfMonitor;
  private ImagePerfState mImagePerfState;

  private ImagePerfControllerListener mListener;

  @Before
  public void setUp() {
    mMonotonicClock = mock(MonotonicClock.class);
    mImagePerfMonitor = mock(ImagePerfMonitor.class);
    mImagePerfState = mock(ImagePerfState.class);
    mListener =
        new ImagePerfControllerListener(mMonotonicClock, mImagePerfState, mImagePerfMonitor);
    when(mImagePerfState.getImageLoadStatus()).thenReturn(ImageLoadStatus.UNKNOWN);
  }

  @Test
  public void testSuccess() {
    final long startTime = 10L;
    final long imageLoadTime = 200L;
    final long imageReleaseTime = 345L;

    when(mMonotonicClock.now()).thenReturn(startTime);
    mListener.onSubmit(CONTROLLER_ID, CALLER_CONTEXT);
    when(mImagePerfState.getImageLoadStatus()).thenReturn(ImageLoadStatus.REQUESTED);

    when(mMonotonicClock.now()).thenReturn(imageLoadTime);
    mListener.onFinalImageSet(CONTROLLER_ID, null, null);
    when(mImagePerfState.getImageLoadStatus()).thenReturn(ImageLoadStatus.SUCCESS);

    when(mMonotonicClock.now()).thenReturn(imageReleaseTime);
    mListener.onRelease(CONTROLLER_ID);

    verify(mMonotonicClock, times(2)).now();
    verify(mImagePerfMonitor).notifyListeners(eq(mImagePerfState), eq(ImageLoadStatus.REQUESTED));
    verify(mImagePerfMonitor).notifyListeners(eq(mImagePerfState), eq(ImageLoadStatus.SUCCESS));
    verify(mImagePerfState).setControllerSubmitTimeMs(startTime);
    verify(mImagePerfState).setControllerFinalImageSetTimeMs(imageLoadTime);
    verifyNoMoreInteractions(mImagePerfMonitor);
  }

  @Test
  public void testFailure() {
    final long startTime = 10L;
    final long imageLoadTime = 200L;
    final long imageReleaseTime = 345L;

    when(mMonotonicClock.now()).thenReturn(startTime);
    mListener.onSubmit(CONTROLLER_ID, CALLER_CONTEXT);
    when(mImagePerfState.getImageLoadStatus()).thenReturn(ImageLoadStatus.REQUESTED);

    when(mMonotonicClock.now()).thenReturn(imageLoadTime);
    mListener.onFailure(CONTROLLER_ID, new Throwable("Error"));
    when(mImagePerfState.getImageLoadStatus()).thenReturn(ImageLoadStatus.ERROR);

    when(mMonotonicClock.now()).thenReturn(imageReleaseTime);
    mListener.onRelease(CONTROLLER_ID);

    verify(mMonotonicClock, times(2)).now();
    verify(mImagePerfMonitor).notifyListeners(eq(mImagePerfState), eq(ImageLoadStatus.REQUESTED));
    verify(mImagePerfMonitor).notifyListeners(eq(mImagePerfState), eq(ImageLoadStatus.ERROR));
    verify(mImagePerfState).setControllerSubmitTimeMs(startTime);
    verify(mImagePerfState).setControllerFailureTimeMs(imageLoadTime);
  }

  @Test
  public void testCancellation() {
    final long startTime = 10L;
    final long imageReleaseTime = 123L;

    when(mMonotonicClock.now()).thenReturn(startTime);
    mListener.onSubmit(CONTROLLER_ID, CALLER_CONTEXT);
    when(mImagePerfState.getImageLoadStatus()).thenReturn(ImageLoadStatus.REQUESTED);

    when(mMonotonicClock.now()).thenReturn(imageReleaseTime);
    mListener.onRelease(CONTROLLER_ID);

    verify(mMonotonicClock, times(2)).now();
    verify(mImagePerfMonitor).notifyListeners(eq(mImagePerfState), eq(ImageLoadStatus.REQUESTED));
    verify(mImagePerfMonitor).notifyListeners(eq(mImagePerfState), eq(ImageLoadStatus.CANCELED));
    verify(mImagePerfState).setControllerSubmitTimeMs(startTime);
    verify(mImagePerfState).setControllerCancelTimeMs(imageReleaseTime);
  }

  @Test
  public void testIntermediateImage() {
    final long startTime = 10L;
    final long intermediateImageTime = 123L;
    final long imageLoadTime = 200L;
    final long imageReleaseTime = 345L;

    when(mMonotonicClock.now()).thenReturn(startTime);
    mListener.onSubmit(CONTROLLER_ID, CALLER_CONTEXT);
    when(mImagePerfState.getImageLoadStatus()).thenReturn(ImageLoadStatus.REQUESTED);

    when(mMonotonicClock.now()).thenReturn(intermediateImageTime);
    mListener.onIntermediateImageSet(CONTROLLER_ID, null);
    when(mImagePerfState.getImageLoadStatus()).thenReturn(ImageLoadStatus.INTERMEDIATE_AVAILABLE);

    when(mMonotonicClock.now()).thenReturn(imageLoadTime);
    mListener.onFinalImageSet(CONTROLLER_ID, null, null);
    when(mImagePerfState.getImageLoadStatus()).thenReturn(ImageLoadStatus.SUCCESS);

    when(mMonotonicClock.now()).thenReturn(imageReleaseTime);
    mListener.onRelease(CONTROLLER_ID);

    verify(mMonotonicClock, times(3)).now();
    verify(mImagePerfMonitor).notifyListeners(eq(mImagePerfState), eq(ImageLoadStatus.REQUESTED));
    verify(mImagePerfMonitor)
        .notifyListeners(eq(mImagePerfState), eq(ImageLoadStatus.INTERMEDIATE_AVAILABLE));
    verify(mImagePerfMonitor).notifyListeners(eq(mImagePerfState), eq(ImageLoadStatus.SUCCESS));
    verify(mImagePerfState).setControllerSubmitTimeMs(startTime);
    verify(mImagePerfState).setControllerIntermediateImageSetTimeMs(intermediateImageTime);
    verify(mImagePerfState).setControllerFinalImageSetTimeMs(imageLoadTime);
    verifyNoMoreInteractions(mImagePerfMonitor);
  }
}
