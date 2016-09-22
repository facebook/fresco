/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.core;

import android.net.Uri;

import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Some tests for ImagePipelineConfigTest
 */
@RunWith(RobolectricTestRunner.class)
public class ImagePipelineConfigTest {

  private Uri mUri;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mUri = mock(Uri.class);
  }

  @Test
  public void testDefaultConfigIsFalseByDefault() {
    ImagePipelineConfig.resetDefaultRequestConfig();
    assertFalse(ImagePipelineConfig.getDefaultImageRequestConfig().isProgressiveRenderingEnabled());
  }

  @Test
  public void testDefaultConfigIsTrueIfChanged() {
    ImagePipelineConfig.resetDefaultRequestConfig();
    ImagePipelineConfig.getDefaultImageRequestConfig().setProgressiveRenderingEnabled(true);
    assertTrue(ImagePipelineConfig.getDefaultImageRequestConfig().isProgressiveRenderingEnabled());
  }

  @Test
  public void testImageRequestDefault() {
    ImagePipelineConfig.resetDefaultRequestConfig();
    final ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(mUri).build();
    assertFalse(imageRequest.getProgressiveRenderingEnabled());
  }

  @Test
  public void testImageRequestWhenChanged() {
    ImagePipelineConfig.resetDefaultRequestConfig();
    ImagePipelineConfig.getDefaultImageRequestConfig().setProgressiveRenderingEnabled(true);
    final ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(mUri).build();
    assertTrue(imageRequest.getProgressiveRenderingEnabled());
  }

  @Test
  public void testImageRequestWhenChangedAndOverriden() {
    ImagePipelineConfig.resetDefaultRequestConfig();
    final ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(mUri)
        .setProgressiveRenderingEnabled(true)
        .build();
    assertTrue(imageRequest.getProgressiveRenderingEnabled());
    final ImageRequest imageRequest2 = ImageRequestBuilder.newBuilderWithSource(mUri)
        .setProgressiveRenderingEnabled(false)
        .build();
    assertFalse(imageRequest2.getProgressiveRenderingEnabled());
  }
}
