/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;

import android.content.res.Resources;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class VitoImageRequestTest {

  @Test
  public void testEquals() {
    Resources res = mock(Resources.class);
    ImageSource is = mock(ImageSource.class);
    ImageOptions options = mock(ImageOptions.class);
    VitoImageRequest a = new VitoImageRequest(res, is, options, null, null);
    VitoImageRequest b = new VitoImageRequest(res, is, options, null, null);
    assertThat(a).isEqualTo(b);
  }

  @Test
  public void testHashCode() {
    Resources res = mock(Resources.class);
    ImageSource is = mock(ImageSource.class);
    ImageOptions options = mock(ImageOptions.class);
    VitoImageRequest a = new VitoImageRequest(res, is, options, null, null);
    VitoImageRequest b = new VitoImageRequest(res, is, options, null, null);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
  }
}
