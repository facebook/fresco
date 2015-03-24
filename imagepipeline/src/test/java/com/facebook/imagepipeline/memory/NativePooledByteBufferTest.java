/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import java.io.InputStream;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.testing.FakeNativeMemoryChunk;
import com.facebook.imagepipeline.testing.FakeNativeMemoryChunkPool;
import com.facebook.testing.robolectric.v2.WithTestDefaultsRunner;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Basic tests for {@link NativePooledByteBuffer}
 */
@RunWith(WithTestDefaultsRunner.class)
public class NativePooledByteBufferTest {
  private FakeNativeMemoryChunkPool mPool;
  private PoolStats mStats;

  @Before
  public void setup() {
    mPool = new FakeNativeMemoryChunkPool();
    mStats = new PoolStats(mPool);
  }

  @Test
  public void testBasic() throws Exception {
    byte[] b = new byte[] {1, 4, 5, 0, 100, 34, 0, 0};
    NativeMemoryChunk chunk = new FakeNativeMemoryChunk(b.length);
    chunk.write(0, b, 0, b.length);
    NativePooledByteBuffer sb = new NativePooledByteBuffer(CloseableReference.of(chunk, mPool), 3);
    Assert.assertFalse(sb.isClosed());
    Assert.assertSame(chunk, sb.mBufRef.get());
    Assert.assertEquals(3, sb.size());
    InputStream is = sb.getStream();
    byte[] tmp = new byte[100];
    int bytesRead = is.read(tmp, 0, tmp.length);
    Assert.assertEquals(3, bytesRead);
    for (int i = 0; i < bytesRead; i++) {
      Assert.assertEquals(b[i], tmp[i]);
    }

    // try a reset
    sb.close();
    Assert.assertTrue(sb.isClosed());
    Assert.assertNull(sb.mBufRef);
    // getting the size should fail
    try {
      sb.size();
      Assert.fail();
    } catch (PooledByteBuffer.ClosedException e) {
      // ignore
    }
    // getting the stream should fail
    try {
      sb.getStream();
      Assert.fail();
    } catch (PooledByteBuffer.ClosedException e) {
      // ignore
    }
  }
}
