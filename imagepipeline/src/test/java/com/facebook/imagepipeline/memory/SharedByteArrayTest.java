/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.facebook.common.memory.MemoryTrimType;
import com.facebook.common.memory.MemoryTrimmableRegistry;
import com.facebook.common.references.CloseableReference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link SharedByteArray} */
@RunWith(RobolectricTestRunner.class)
public class SharedByteArrayTest {

  private SharedByteArray mArray;

  @Before
  public void setup() {
    mArray =
        new SharedByteArray(mock(MemoryTrimmableRegistry.class), new PoolParams(null, 4, 16, 1));
  }

  @Test
  public void testBasic() throws Exception {
    assertThat(mArray.mMinByteArraySize).isEqualTo(4);
    assertThat(mArray.mMaxByteArraySize).isEqualTo(16);
    assertThat(mArray.mByteArraySoftRef.get()).isNull();
    assertThat(mArray.mSemaphore.availablePermits()).isEqualTo(1);
  }

  @Test
  public void testGet() throws Exception {
    CloseableReference<byte[]> arrayRef = mArray.get(1);
    assertThat(arrayRef.get()).isSameAs(mArray.mByteArraySoftRef.get());
    assertThat(arrayRef.get().length).isEqualTo(4);
    assertThat(mArray.mSemaphore.availablePermits()).isEqualTo(0);
  }

  @Test
  public void testGetTooBigArray() {
    assertThatThrownBy(() -> mArray.get(32)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void testRelease() throws Exception {
    mArray.get(4).close();
    assertThat(mArray.mSemaphore.availablePermits()).isEqualTo(1);
  }

  @Test
  public void testGet_Realloc() {
    CloseableReference<byte[]> arrayRef = mArray.get(1);
    final byte[] smallArray = arrayRef.get();
    arrayRef.close();

    arrayRef = mArray.get(7);
    assertThat(arrayRef.get().length).isEqualTo(8);
    assertThat(arrayRef.get()).isSameAs(mArray.mByteArraySoftRef.get());
    assertThat(arrayRef.get()).isNotSameAs(smallArray);
  }

  @Test
  public void testTrim() {
    mArray.get(7).close();
    assertThat(mArray.mByteArraySoftRef.get().length).isEqualTo(8);

    // now trim, and verify again
    mArray.trim(MemoryTrimType.OnCloseToDalvikHeapLimit);
    assertThat(mArray.mByteArraySoftRef.get()).isNull();
    assertThat(mArray.mSemaphore.availablePermits()).isEqualTo(1);
  }

  @Test
  public void testTrimUnsuccessful() {
    CloseableReference<byte[]> arrayRef = mArray.get(7);
    mArray.trim(MemoryTrimType.OnCloseToDalvikHeapLimit);
    assertThat(mArray.mByteArraySoftRef.get()).isSameAs(arrayRef.get());
    assertThat(mArray.mSemaphore.availablePermits()).isEqualTo(0);
  }

  @Test
  public void testGetBucketedSize() throws Exception {
    assertThat(mArray.getBucketedSize(1)).isEqualTo(4);
    assertThat(mArray.getBucketedSize(2)).isEqualTo(4);
    assertThat(mArray.getBucketedSize(3)).isEqualTo(4);
    assertThat(mArray.getBucketedSize(4)).isEqualTo(4);
    assertThat(mArray.getBucketedSize(5)).isEqualTo(8);
    assertThat(mArray.getBucketedSize(6)).isEqualTo(8);
    assertThat(mArray.getBucketedSize(7)).isEqualTo(8);
    assertThat(mArray.getBucketedSize(8)).isEqualTo(8);
    assertThat(mArray.getBucketedSize(9)).isEqualTo(16);
  }
}
