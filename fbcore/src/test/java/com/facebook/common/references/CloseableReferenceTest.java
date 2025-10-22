/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.references;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Closeable;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

/** Basic tests for shared references */
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
    assertThat(mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly())
        .isEqualTo(1);
  }

  @Test
  public void testClone() {
    CloseableReference<Closeable> copy = mCloseableReference.clone();
    assertThat(mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly())
        .isEqualTo(2);
    assertThat(copy.getUnderlyingReferenceTestOnly())
        .isSameAs(mCloseableReference.getUnderlyingReferenceTestOnly());
  }

  @Test
  public void testCloseReference() {
    CloseableReference<Closeable> copy = mCloseableReference.clone();
    assertThat(mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly())
        .isEqualTo(2);
    copy.close();
    assertThat(mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly())
        .isEqualTo(1);
  }

  @Test
  public void testCloseWhenRefcount0() throws IOException {
    mCloseableReference.close();
    assertThat(mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly())
        .isEqualTo(0);
    Mockito.verify(mMockCloseable).close();
  }
}
