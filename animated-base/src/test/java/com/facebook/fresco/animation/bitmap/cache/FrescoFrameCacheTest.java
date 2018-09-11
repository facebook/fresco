/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.fresco.animation.bitmap.cache;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Tests {@link FrescoFrameCache}.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(CloseableReference.class)
public class FrescoFrameCacheTest {

  @Mock public CloseableReference<CloseableImage> mImageReference;
  @Mock public CloseableStaticBitmap mCloseableStaticBitmap;
  @Mock public CloseableReference<Bitmap> mBitmapReference;
  @Mock public CloseableReference<Bitmap> mBitmapReferenceClone;
  @Mock public Bitmap mUnderlyingBitmap;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(mBitmapReference.isValid()).thenReturn(true);
    when(mBitmapReference.get()).thenReturn(mUnderlyingBitmap);

    when(mBitmapReferenceClone.isValid()).thenReturn(true);
    when(mBitmapReferenceClone.get()).thenReturn(mUnderlyingBitmap);

    when(mCloseableStaticBitmap.isClosed()).thenReturn(false);
    when(mCloseableStaticBitmap.getUnderlyingBitmap()).thenReturn(mUnderlyingBitmap);
    when(mCloseableStaticBitmap.convertToBitmapReference())
        .thenReturn(mBitmapReference);
    when(mCloseableStaticBitmap.cloneUnderlyingBitmapReference()).thenReturn(mBitmapReferenceClone);

    when(mImageReference.isValid()).thenReturn(true);
    when(mImageReference.get()).thenReturn(mCloseableStaticBitmap);
  }

  @Test
  public void testExtractAndClose() throws Exception {
    CloseableReference<Bitmap> extractedReference =
        FrescoFrameCache.convertToBitmapReferenceAndClose(mImageReference);

    assertThat(extractedReference).isNotNull();
    assertThat(extractedReference.get()).isEqualTo(mUnderlyingBitmap);
    verify(mImageReference).close();

    extractedReference.close();
  }

  @Test
  public void testExtractAndClose_whenBitmapRecycled_thenReturnReference() throws Exception {
    when(mUnderlyingBitmap.isRecycled()).thenReturn(true);

    CloseableReference<Bitmap> extractedReference =
        FrescoFrameCache.convertToBitmapReferenceAndClose(mImageReference);

    // We only detach the reference and do not care if the bitmap is valid
    assertThat(extractedReference).isNotNull();
    assertThat(extractedReference.get()).isEqualTo(mUnderlyingBitmap);
    verify(mImageReference).close();

    extractedReference.close();
  }

  @Test
  public void testExtractAndClose_whenBitmapReferenceInvalid_thenReturnReference()
      throws Exception {
    when(mBitmapReference.isValid()).thenReturn(false);

    CloseableReference<Bitmap> extractedReference =
        FrescoFrameCache.convertToBitmapReferenceAndClose(mImageReference);

    // We only detach the reference and do not care if the bitmap reference is valid
    assertThat(extractedReference).isNotNull();
    assertThat(extractedReference.get()).isEqualTo(mUnderlyingBitmap);

    extractedReference.close();

    verify(mImageReference).close();
  }

  @Test
  public void testExtractAndClose_whenCloseableStaticBitmapClosed_thenReturnNull()
      throws Exception {
    when(mCloseableStaticBitmap.isClosed()).thenReturn(true);
    when(mCloseableStaticBitmap.cloneUnderlyingBitmapReference()).thenReturn(null);

    CloseableReference<Bitmap> extractedReference =
        FrescoFrameCache.convertToBitmapReferenceAndClose(mImageReference);

    // We only detach the reference and do not care if the bitmap is valid
    assertThat(extractedReference).isNull();
    verify(mImageReference).close();
  }

  @Test
  public void testExtractAndClose_whenImageReferenceInvalid_thenReturnNull() throws Exception {
    when(mImageReference.isValid()).thenReturn(false);

    CloseableReference<Bitmap> extractedReference =
        FrescoFrameCache.convertToBitmapReferenceAndClose(mImageReference);

    // We only detach the reference and do not care if the bitmap is valid
    assertThat(extractedReference).isNull();
    verify(mImageReference).close();
  }

  @Test
  public void testExtractAndClose_whenInputNull_thenReturnNull() throws Exception {
    CloseableReference<Bitmap> extractedReference =
        FrescoFrameCache.convertToBitmapReferenceAndClose(null);

    assertThat(extractedReference).isNull();
    verifyZeroInteractions(mImageReference);
  }

  @Test
  public void testExtractAndClose_whenCloseableStaticBitmapNull_thenReturnNull() throws Exception {
    when(mImageReference.get()).thenReturn(null);

    CloseableReference<Bitmap> extractedReference =
        FrescoFrameCache.convertToBitmapReferenceAndClose(mImageReference);

    assertThat(extractedReference).isNull();
    verify(mImageReference).close();
  }
}
