/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.request;

import static org.junit.Assert.assertEquals;

import android.net.Uri;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;

@RunWith(ParameterizedRobolectricTestRunner.class)
public class ImageRequestBuilderCacheEnabledTest {

  @ParameterizedRobolectricTestRunner.Parameters(name = "URI of scheme \"{0}://\"")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][]{
            {"asset", false},
            {"content", false},
            {"data", false},
            {"file", false},
            {"http", true},
            {"https", true},
            {"res", false},
        });
  }

  private final String mUriScheme;
  private final boolean mExpectedDefaultDiskCacheEnabled;

  public ImageRequestBuilderCacheEnabledTest(
      String uriScheme,
      Boolean expectedDefaultDiskCacheEnabled) {
    mUriScheme = uriScheme;
    mExpectedDefaultDiskCacheEnabled = expectedDefaultDiskCacheEnabled;
  }

  @Test
  public void testIsDiskCacheEnabledByDefault() throws Exception {
    ImageRequestBuilder imageRequestBuilder = createBuilder();
    assertEquals(mExpectedDefaultDiskCacheEnabled, imageRequestBuilder.isDiskCacheEnabled());
  }

  @Test
  public void testIsDiskCacheDisabledIfRequested() throws Exception {
    ImageRequestBuilder imageRequestBuilder = createBuilder();
    imageRequestBuilder.disableDiskCache();
    assertEquals(false, imageRequestBuilder.isDiskCacheEnabled());
  }

  private ImageRequestBuilder createBuilder() {
    return ImageRequestBuilder.newBuilderWithSource(Uri.parse(mUriScheme + "://request"));
  }
}
