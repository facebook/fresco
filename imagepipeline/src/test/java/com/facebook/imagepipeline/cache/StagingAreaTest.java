/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;

import org.junit.*;
import org.junit.runner.*;
import org.robolectric.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
public class StagingAreaTest {
  private StagingArea mStagingArea;
  private CloseableReference<PooledByteBuffer> mCloseableReference;
  private CloseableReference<PooledByteBuffer> mCloseableReference2;
  private EncodedImage mEncodedImage;
  private EncodedImage mSecondEncodedImage;
  private CacheKey mCacheKey;

  @Before
  public void setUp() {
    mStagingArea = StagingArea.getInstance();
    mCloseableReference = CloseableReference.of(mock(PooledByteBuffer.class));
    mCloseableReference2 = CloseableReference.of(mock(PooledByteBuffer.class));
    mEncodedImage = new EncodedImage(mCloseableReference);
    mSecondEncodedImage = new EncodedImage(mCloseableReference2);
    mCacheKey = new SimpleCacheKey("http://this.is/uri");
    mStagingArea.put(mCacheKey, mEncodedImage);
  }

  @Test
  public void testContains() {
    assertTrue(mStagingArea.containsKey(mCacheKey));
    assertFalse(mStagingArea.containsKey(new SimpleCacheKey("http://this.is.not.uri")));
  }

  @Test
  public void testDoesntContainInvalid() {
    mEncodedImage.close();
    assertTrue(mStagingArea.containsKey(mCacheKey));
    assertTrue(EncodedImage.isValid(mStagingArea.get(mCacheKey)));
  }

  @Test
  public void testGetValue() {
    assertSame(
        mCloseableReference.getUnderlyingReferenceTestOnly(),
        mStagingArea.get(mCacheKey).getByteBufferRef().getUnderlyingReferenceTestOnly());
  }

  @Test
  public void testBumpsRefCountOnGet() {
    mStagingArea.get(mCacheKey);
    assertEquals(4, mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
  }

  @Test
  public void testAnotherPut() {
    mStagingArea.put(
        mCacheKey,
        mSecondEncodedImage);
    assertEquals(2, mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertSame(
        mCloseableReference2.getUnderlyingReferenceTestOnly(),
        mStagingArea.get(mCacheKey).getByteBufferRef().getUnderlyingReferenceTestOnly());
  }

  @Test
  public void testSamePut() {
    assertEquals(3, mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    mStagingArea.put(mCacheKey, mEncodedImage);
    assertEquals(3, mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertSame(
        mCloseableReference.getUnderlyingReferenceTestOnly(),
        mStagingArea.get(mCacheKey).getByteBufferRef().getUnderlyingReferenceTestOnly());
  }

  @Test
  public void testRemove() {
    assertTrue(mStagingArea.remove(mCacheKey, mEncodedImage));
    assertEquals(2, mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertFalse(mStagingArea.remove(mCacheKey, mEncodedImage));
  }

  @Test
  public void testRemoveWithBadRef() {
    assertFalse(mStagingArea.remove(mCacheKey, mSecondEncodedImage));
    assertTrue(CloseableReference.isValid(mCloseableReference));
    assertTrue(CloseableReference.isValid(mCloseableReference2));
  }

  @Test
  public void testRemoveWithoutValueCheck() {
    assertTrue(mStagingArea.remove(mCacheKey));
    assertEquals(2, mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertFalse(mStagingArea.remove(mCacheKey));
  }

  @Test
  public void testClearAll() {
    mStagingArea.put(new SimpleCacheKey("second"), mSecondEncodedImage);
    mStagingArea.clearAll();
    assertEquals(2, mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertEquals(2, mCloseableReference2.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertFalse(mStagingArea.remove(mCacheKey));
  }
}
