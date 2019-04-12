/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.backends.pipeline.info.internal;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.facebook.drawee.backends.pipeline.info.ImageLoadStatus;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.ImagePerfMonitor;
import com.facebook.drawee.backends.pipeline.info.ImagePerfState;
import org.junit.Before;
import org.junit.Test;

/** Tests {@link ImagePerfImageOriginListener} */
public class ImagePerfImageOriginListenerTest {

  private static final String CONTROLLER_ID = "abc";

  private ImagePerfMonitor mImagePerfMonitor;
  private ImagePerfState mImagePerfState;

  private ImagePerfImageOriginListener mListener;

  @Before
  public void setUp() {
    mImagePerfMonitor = mock(ImagePerfMonitor.class);
    mImagePerfState = mock(ImagePerfState.class);
    mListener = new ImagePerfImageOriginListener(mImagePerfState, mImagePerfMonitor);
  }

  @Test
  public void testOnImageLoaded() {

    mListener.onImageLoaded(CONTROLLER_ID, ImageOrigin.NETWORK, true, null);

    verify(mImagePerfState).setImageOrigin(eq(ImageOrigin.NETWORK));
    verify(mImagePerfMonitor)
        .notifyStatusUpdated(eq(mImagePerfState), eq(ImageLoadStatus.ORIGIN_AVAILABLE));
    verifyNoMoreInteractions(mImagePerfMonitor);
  }
}
