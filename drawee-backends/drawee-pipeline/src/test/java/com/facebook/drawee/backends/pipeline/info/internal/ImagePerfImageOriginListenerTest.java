/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.pipeline.info.internal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.ImagePerfExtra;
import com.facebook.fresco.ui.common.ImagePerfState;
import org.junit.Before;
import org.junit.Test;

/** Tests {@link ImagePerfImageOriginListener} */
public class ImagePerfImageOriginListenerTest {

  private static final String CONTROLLER_ID = "abc";

  private ImagePerfState mImagePerfState;

  private ImagePerfImageOriginListener mListener;

  @Before
  public void setUp() {
    mImagePerfState = mock(ImagePerfState.class);
    mListener = new ImagePerfImageOriginListener(mImagePerfState);
  }

  @Test
  public void testOnImageLoaded() {

    mListener.onImageLoaded(CONTROLLER_ID, ImageOrigin.NETWORK, true, null);

    verify(mImagePerfState)
        .setPipelineExtra(eq(ImagePerfExtra.IMAGE_ORIGIN), eq(ImageOrigin.NETWORK));
  }
}
