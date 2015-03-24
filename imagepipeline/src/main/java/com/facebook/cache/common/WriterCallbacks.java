/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.facebook.common.internal.ByteStreams;

/**
 * Utility class to create typical {@link WriterCallback}s.
 */
public class WriterCallbacks {

  /**
   * Creates a writer callback that copies all the content read from an {@link InputStream} into
   * the target stream.
   *
   * <p>This writer can be used only once.
   * @param is the source
   * @return the writer callback
   */
  public static WriterCallback from(final InputStream is) {
   return new WriterCallback() {
     @Override
     public void write(OutputStream os) throws IOException {
       ByteStreams.copy(is, os);
     }
   };
  }

  /**
   * Creates a writer callback that writes some byte array to the target stream.
   *
   * <p>This writer can be used many times.
   * @param data the bytes to write
   * @return the writer callback
   */
  public static WriterCallback from(final byte[] data) {
    return new WriterCallback() {
      @Override
      public void write(OutputStream os) throws IOException {
        os.write(data);
      }
    };
  }
}
