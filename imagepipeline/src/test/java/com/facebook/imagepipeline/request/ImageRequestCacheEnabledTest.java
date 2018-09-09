/*
 * Copyright (c) 2015-present, Facebook, Inc.
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
public class ImageRequestCacheEnabledTest {

  @ParameterizedRobolectricTestRunner.Parameters(name = "URI of scheme \"{0}://\"")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][]{
            {"asset", false, false, false},
            {"content", false, false, false},
            {"content", true, true, true},
            {"data", false, false, false},
            {"file", false, false, false},
            {"file", true, true, true},
            {"http", false, true, false},
            {"https", false, true, false},
            {"res", false, false, false},
        });
  }

  private final String mUriScheme;
  private final boolean mIsNetworkUrl;
  private final boolean mIsLocalVideoUrl;
  private final boolean mExpectedDefaultDiskCacheEnabled;
  private final boolean mExpectedDefaultLocalVideoDiskCacheEnabled;

  public ImageRequestCacheEnabledTest(
      String uriScheme,
      Boolean isLocalVideoUrl,
      Boolean expectedDefaultDiskCacheEnabled,
      Boolean expectedDefaultLocalVideoDiskCacheEnabled) {
    mUriScheme = uriScheme;
    mIsNetworkUrl = "http".equals(mUriScheme) || "https".equals(mUriScheme);
    mIsLocalVideoUrl = isLocalVideoUrl;
    mExpectedDefaultDiskCacheEnabled = expectedDefaultDiskCacheEnabled;
    mExpectedDefaultLocalVideoDiskCacheEnabled = expectedDefaultLocalVideoDiskCacheEnabled;
  }

  @Test
  public void testIsDiskCacheEnabledByDefault() throws Exception {
    ImageRequest imageRequest = createRequest(false, false);
    assertEquals(mExpectedDefaultDiskCacheEnabled, imageRequest.isDiskCacheEnabled());
    assertEquals(mExpectedDefaultLocalVideoDiskCacheEnabled, imageRequest.isVideoThumbnailDiskCacheEnabled());
  }

  @Test
  public void testIsDiskCacheDisabledIfRequested() throws Exception {
    ImageRequest imageRequest = createRequest(true, false);
    assertEquals(false, imageRequest.isDiskCacheEnabled());
    assertEquals(false, imageRequest.isVideoThumbnailDiskCacheEnabled());
    // disable local video disk cache
    imageRequest = createRequest(true, true);
    assertEquals(false, imageRequest.isDiskCacheEnabled());
    assertEquals(false, imageRequest.isVideoThumbnailDiskCacheEnabled());
  }

  @Test
  public void testIsLocalVideoCacheDisabledIfRequested() throws Exception {
    ImageRequest imageRequest = createRequest(false, true);
    assertEquals(mExpectedDefaultDiskCacheEnabled && mIsNetworkUrl, imageRequest.isDiskCacheEnabled());
    assertEquals(false, imageRequest.isVideoThumbnailDiskCacheEnabled());
  }


  private ImageRequest createRequest(
      boolean disableDiskCache,
      boolean disableLocalVideoDiskCache) {
    ImageRequestBuilder builder = ImageRequestBuilder.newBuilderWithSource(Uri.parse(mUriScheme + "://request/12345"));
    if (disableDiskCache) {
      builder = builder.disableDiskCache();
    }
    if (disableLocalVideoDiskCache) {
      builder.disableVideoThumbnailDiskCache();
    }
    ImageRequest request = builder.build();
    request.setIsLocalVideoUri(mIsLocalVideoUrl);
    return request;
  }
}
