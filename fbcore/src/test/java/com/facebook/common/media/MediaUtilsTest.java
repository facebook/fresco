/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.media;

import static org.fest.assertions.api.Assertions.assertThat;

import android.webkit.MimeTypeMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowMimeTypeMap;

@RunWith(RobolectricTestRunner.class)
public class MediaUtilsTest {

  @Test
  public void testIsPhotoMimeTypeNull() {
    assertThat(MediaUtils.isPhoto(null)).isFalse();
  }

  @Test
  public void testIsPhotoMimeTypeImage() {
    assertThat(MediaUtils.isPhoto("image/jpg")).isTrue();
  }

  @Test
  public void testIsPhotoMimeTypeVideo() {
    assertThat(MediaUtils.isPhoto("video/mp4")).isFalse();
  }

  @Test
  public void testIsVideoMimeTypeNull() {
    assertThat(MediaUtils.isVideo(null)).isFalse();
  }

  @Test
  public void testIsVideoMimeTypeImage() {
    assertThat(MediaUtils.isVideo("image/jpg")).isFalse();
  }

  @Test
  public void testIsVideoMimeTypeVideo() {
    assertThat(MediaUtils.isVideo("video/mp4")).isTrue();
  }

  @Test
  public void testExtractMimeNoFileExtension() {
    String path = "file/with/no/extension";
    assertThat(MediaUtils.extractMime(path)).isNull();
  }

  @Test
  public void testExtractMimeNativelySupportedFileExtension() {
    ShadowMimeTypeMap mimeTypeMap = Shadows.shadowOf(MimeTypeMap.getSingleton());
    mimeTypeMap.addExtensionMimeTypMapping("jpg", "image/jpg");

    String path = "file/with/natively/supported/extension.jpg";
    assertThat(MediaUtils.extractMime(path)).isEqualTo("image/jpg");
  }

  @Test
  public void testExtractMimeNonNativelySupportedFileExtension() {
    String path = "file/with/non/natively/supported/extension.mkv";
    assertThat(MediaUtils.extractMime(path)).isEqualTo("video/x-matroska");
  }

  @Test
  public void testExtractMimeUnsupportedFileExtension() {
    String path = "file/with/unsupported/extension.zip";
    assertThat(MediaUtils.extractMime(path)).isNull();
  }

  @Test
   public void testIsNonNativeSupportedMimeTypeNative() {
    assertThat(MediaUtils.isNonNativeSupportedMimeType("image/jpg")).isFalse();
  }

  @Test
  public void testIsNonNativeSupportedMimeTypeNonNative() {
    assertThat(MediaUtils.isNonNativeSupportedMimeType("video/x-matroska")).isTrue();
  }
}
