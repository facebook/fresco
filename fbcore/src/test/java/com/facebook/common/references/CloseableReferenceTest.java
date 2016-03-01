/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.references;

import java.io.Closeable;
import java.io.IOException;

import org.robolectric.RobolectricTestRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Basic tests for shared references
 */
@RunWith(RobolectricTestRunner.class)
public class CloseableReferenceTest {

  private Closeable mMockCloseable;
  private CloseableReference<Closeable> mCloseableReference;

  @Before
  public void setup() {
    mMockCloseable = Mockito.mock(Closeable.class);
    mCloseableReference = CloseableReference.of(mMockCloseable);
  }

  @Test
  public void testCreation() {
    Assert.assertEquals(1,
        mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
  }

  @Test
  public void testClone() {
    CloseableReference<Closeable> copy = mCloseableReference.clone();
    Assert.assertEquals(2,
        mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    Assert.assertSame(mCloseableReference.getUnderlyingReferenceTestOnly(),
        copy.getUnderlyingReferenceTestOnly());
  }

  @Test
  public void testCloseReference() {
    CloseableReference<Closeable> copy = mCloseableReference.clone();
    Assert.assertEquals(2,
        mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    copy.close();
    Assert.assertEquals(1,
        mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
  }

  @Test
  public void testCloseWhenRefcount0() throws IOException {
    mCloseableReference.close();
    Assert.assertEquals(0,
        mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    Mockito.verify(mMockCloseable).close();
  }
}
