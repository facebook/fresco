/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.net.Uri;

import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.MediaVariations;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class MediaVariationsIndexDatabaseTest {

  private static final String MEDIA_ID = "id";

  private static final Uri URI_1 = Uri.parse("https://frescolib.org/variant1.jpg");
  private static final SimpleCacheKey CACHE_KEY_1 = new SimpleCacheKey(URI_1.toString());
  private static final int WIDTH_1 = 100;
  private static final int HEIGHT_1 = 200;
  private static final ImageRequest.CacheChoice CACHE_CHOICE_1 = ImageRequest.CacheChoice.DEFAULT;

  private static final Uri URI_2 = Uri.parse("https://frescolib.org/variant2.jpg");
  private static final SimpleCacheKey CACHE_KEY_2 = new SimpleCacheKey(URI_2.toString());
  private static final int WIDTH_2 = 50;
  private static final int HEIGHT_2 = 100;
  private static final ImageRequest.CacheChoice CACHE_CHOICE_2 = ImageRequest.CacheChoice.SMALL;

  private static final String DIFFERENT_MEDIA_ID = "different";

  private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

  @Mock public EncodedImage mEncodedImage1;
  @Mock public EncodedImage mEncodedImage2;
  private MediaVariationsIndexDatabase mMediaVariationsIndexDatabase;

  @Before
  public void setup() {
    mMediaVariationsIndexDatabase =
        new MediaVariationsIndexDatabase(RuntimeEnvironment.application, EXECUTOR, EXECUTOR);

    MockitoAnnotations.initMocks(this);
    when(mEncodedImage1.getWidth()).thenReturn(WIDTH_1);
    when(mEncodedImage1.getHeight()).thenReturn(HEIGHT_1);
    when(mEncodedImage2.getWidth()).thenReturn(WIDTH_2);
    when(mEncodedImage2.getHeight()).thenReturn(HEIGHT_2);
  }

  @Test
  public void testGetsNoCachedVariantsIfNothingStored() {
    List<MediaVariations.Variant> cachedVariants =
        mMediaVariationsIndexDatabase.getCachedVariantsSync(MEDIA_ID);

    assertThat(cachedVariants).isEmpty();
  }

  @Test
  public void testGetsNoCachedVariantsIfNonMatchingItemStored() {
    whenNonMatchingItemInIndex();

    List<MediaVariations.Variant> cachedVariants =
        mMediaVariationsIndexDatabase.getCachedVariantsSync(MEDIA_ID);

    assertThat(cachedVariants).isEmpty();
  }

  @Test
  public void testGetsSameCachedVariantAfterBeingSavedIfOneSaved() {
    mMediaVariationsIndexDatabase.saveCachedVariantSync(
        MEDIA_ID,
        ImageRequest.CacheChoice.DEFAULT,
        CACHE_KEY_1,
        mEncodedImage1);

    List<MediaVariations.Variant> cachedVariants =
        mMediaVariationsIndexDatabase.getCachedVariantsSync(MEDIA_ID);

    assertThat(cachedVariants).hasSize(1);
    assertVariantIsEqualTo(cachedVariants.get(0), URI_1, WIDTH_1, HEIGHT_1, CACHE_CHOICE_1);
  }

  @Test
  public void testGetsSameCachedVariantAfterBeingSavedIfTwoSaved() {
    whenNonMatchingItemInIndex();
    mMediaVariationsIndexDatabase
        .saveCachedVariantSync(MEDIA_ID, CACHE_CHOICE_1, CACHE_KEY_1, mEncodedImage1);
    mMediaVariationsIndexDatabase
        .saveCachedVariantSync(MEDIA_ID, CACHE_CHOICE_2, CACHE_KEY_2, mEncodedImage2);

    List<MediaVariations.Variant> cachedVariants =
        mMediaVariationsIndexDatabase.getCachedVariantsSync(MEDIA_ID);

    assertThat(cachedVariants).hasSize(2);

    assertVariantIsEqualTo(cachedVariants.get(0), URI_1, WIDTH_1, HEIGHT_1, CACHE_CHOICE_1);
    assertVariantIsEqualTo(cachedVariants.get(1), URI_2, WIDTH_2, HEIGHT_2, CACHE_CHOICE_2);
  }

  private void whenNonMatchingItemInIndex() {
    mMediaVariationsIndexDatabase.saveCachedVariantSync(
        DIFFERENT_MEDIA_ID,
        ImageRequest.CacheChoice.DEFAULT,
        CACHE_KEY_1,
        mEncodedImage1);
  }

  private static void assertVariantIsEqualTo(
      MediaVariations.Variant variant,
      Uri uri,
      int width,
      int height,
      ImageRequest.CacheChoice cacheChoice) {
    assertThat(variant.getUri()).isEqualTo(uri);
    assertThat(variant.getWidth()).isEqualTo(width);
    assertThat(variant.getHeight()).isEqualTo(height);
    assertThat(variant.getCacheChoice()).isEqualTo(cacheChoice);
  }
}
