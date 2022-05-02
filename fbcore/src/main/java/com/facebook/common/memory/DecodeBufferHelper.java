/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.memory;

import androidx.annotation.Nullable;
import androidx.core.util.Pools;
import com.facebook.infer.annotation.Nullsafe;
import java.nio.ByteBuffer;

@Nullsafe(Nullsafe.Mode.LOCAL)
public class DecodeBufferHelper implements Pools.Pool<ByteBuffer> {

  public static final DecodeBufferHelper INSTANCE = new DecodeBufferHelper();

  private static final int DEFAULT_DECODE_BUFFER_SIZE = 16 * 1024;

  private static int sRecommendedDecodeBufferSize = DEFAULT_DECODE_BUFFER_SIZE;

  public static int getRecommendedDecodeBufferSize() {
    return sRecommendedDecodeBufferSize;
  }

  public static void setRecommendedDecodeBufferSize(int recommendedDecodeBufferSize) {
    sRecommendedDecodeBufferSize = recommendedDecodeBufferSize;
  }

  private static final ThreadLocal<ByteBuffer> sBuffer =
      new ThreadLocal<ByteBuffer>() {
        @Override
        protected ByteBuffer initialValue() {
          return ByteBuffer.allocate(sRecommendedDecodeBufferSize);
        }
      };

  @Override
  public @Nullable ByteBuffer acquire() {
    return sBuffer.get();
  }

  @Override
  public boolean release(ByteBuffer instance) {
    return true;
  }
}
