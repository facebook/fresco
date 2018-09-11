/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.common;

import static org.fest.assertions.api.Assertions.assertThat;

import android.graphics.Bitmap;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/** Tests {@link ImageDecodeOptions} */
@RunWith(RobolectricTestRunner.class)
public class ImageDecodeOptionsTest {

  private static final int MIN_DECODE_INTERVAL_MS = 123;

  public Bitmap.Config mBitmapConfig = Bitmap.Config.ARGB_8888;
  @Mock public ImageDecoder mImageDecoder;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void testSetFrom_whenUnchanged_thenEqual() throws Exception {
    ImageDecodeOptions originalOptions = createSampleDecodeOptions();

    ImageDecodeOptions newOptions =
        ImageDecodeOptions.newBuilder().setFrom(originalOptions).build();

    assertThat(newOptions).isEqualTo(originalOptions);
  }

  @Test
  public void testSetFrom_whenBooleanChanged_thenNotEqual() throws Exception {
    ImageDecodeOptions originalOptions = createSampleDecodeOptions();

    ImageDecodeOptions newOptions =
        ImageDecodeOptions.newBuilder().setFrom(originalOptions).setForceStaticImage(false).build();

    assertThat(newOptions).isNotEqualTo(originalOptions);
  }

  @Test
  public void testSetFrom_whenObjectChanged_thenNotEqual() throws Exception {
    ImageDecodeOptions originalOptions = createSampleDecodeOptions();

    ImageDecodeOptions newOptions =
        ImageDecodeOptions.newBuilder()
            .setFrom(originalOptions)
            .setCustomImageDecoder(null)
            .build();

    assertThat(newOptions).isNotEqualTo(originalOptions);
  }

  private ImageDecodeOptions createSampleDecodeOptions() {
    return ImageDecodeOptions.newBuilder()
        .setBitmapConfig(mBitmapConfig)
        .setCustomImageDecoder(mImageDecoder)
        .setDecodeAllFrames(true)
        .setDecodePreviewFrame(true)
        .setForceStaticImage(true)
        .setMinDecodeIntervalMs(MIN_DECODE_INTERVAL_MS)
        .setUseLastFrameForPreview(true)
        .build();
  }
}
