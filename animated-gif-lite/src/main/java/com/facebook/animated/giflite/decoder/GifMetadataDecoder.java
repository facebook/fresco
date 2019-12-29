/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.giflite.decoder;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;

public class GifMetadataDecoder {

  private static final int MAX_BLOCK_SIZE = 256; // blocks sizes are defined by a single byte
  private static final char[] NETSCAPE =
      new char[] {'N', 'E', 'T', 'S', 'C', 'A', 'P', 'E', '2', '.', '0'};
  private static final int CONTROL_INDEX_DISPOSE = 0;
  private static final int CONTROL_INDEX_DELAY = 1;
  private static final int DEFAULT_FRAME_DURATION_MS = 100;

  private final byte[] block = new byte[MAX_BLOCK_SIZE];
  private final InputStream mInputStream;
  @Nullable private final OutputStream mOutputStream;
  private boolean shouldFixStream;
  private final List<int[]> mFrameControls = new ArrayList<>();
  private int mLoopCount = 1; // default loop count is 1
  private boolean mDecoded = false;
  private int mCurrentOffset = 0;

  public static GifMetadataDecoder create(InputStream is, @Nullable OutputStream os)
      throws IOException {
    GifMetadataDecoder decoder = new GifMetadataDecoder(is, os);
    decoder.decode();
    return decoder;
  }

  /**
   * @param is InputStream to decode
   * @param os OutputStream to write fixed version of gif, if needed. (optional)
   */
  private GifMetadataDecoder(InputStream is, @Nullable OutputStream os) {
    mInputStream = is;
    mOutputStream = os;
  }

  public void decode() throws IOException {
    if (mDecoded) {
      throw new IllegalStateException("decode called multiple times");
    }
    mDecoded = true;
    readGifInfo();
  }

  public int getFrameCount() {
    if (!mDecoded) {
      throw new IllegalStateException("getFrameCount called before decode");
    }
    return mFrameControls.size();
  }

  public int getLoopCount() {
    if (!mDecoded) {
      throw new IllegalStateException("getLoopCount called before decode");
    }
    return mLoopCount;
  }

  public int getFrameDisposal(int frameNumber) {
    if (!mDecoded) {
      throw new IllegalStateException("getFrameDisposal called before decode");
    }
    return mFrameControls.get(frameNumber)[CONTROL_INDEX_DISPOSE];
  }

  public int getFrameDurationMs(int frameNumber) {
    if (!mDecoded) {
      throw new IllegalStateException("getFrameDurationMs called before decode");
    }

    // For frame number higher than frame count, returning 1 ms ensures that animation backend can
    // fetch loop duration of a frame correctly in case when some frames do not have correct delay.
    if (frameNumber >= getFrameCount()) {
      return 1;
    }

    return mFrameControls.get(frameNumber)[CONTROL_INDEX_DELAY];
  }

  private void readGifInfo() throws IOException {
    validateAndIgnoreHeader();

    final int[] control = new int[] {0, 0};

    boolean done = false;
    while (!done) {
      int code = readAndWriteNextByte();
      switch (code) {
        case 0x21: // extension
          int extCode = readAndWriteNextByte();
          switch (extCode) {
            case 0xff: // application extension
              readBlock();
              if (isNetscape()) {
                readNetscapeExtension();
              } else {
                skipExtension();
              }
              break;
            case 0xf9: // graphics control extension
              readGraphicsControlExtension(control);
              break;
            case 0x01: // plain text extension, counts as a frame
              addFrame(control);
              skipExtension();
              break;
            default:
              skipExtension();
          }
          break;
        case 0x2C: // image
          addFrame(control);
          skipImage();
          // count as a frame
          break;
        case 0x3b: // terminator
          done = true;
          break;
        default:
          throw new IOException("Unknown block header [" + Integer.toHexString(code) + "]");
      }
    }
  }

  private void addFrame(int[] control) {
    mFrameControls.add(Arrays.copyOf(control, control.length));
  }

  private void validateAndIgnoreHeader() throws IOException {
    readIntoBlock(0 /* offset */, 6 /* length */);
    boolean valid =
        'G' == (char) block[0]
            && 'I' == (char) block[1]
            && 'F' == (char) block[2]
            && '8' == (char) block[3]
            && ('7' == (char) block[4] || '9' == (char) block[4])
            && 'a' == (char) block[5];
    if (!valid) {
      throw new IOException("Illegal header for gif");
    }

    skipAndWriteBytes(4); // width, height

    int fields = readAndWriteNextByte();
    boolean hasGlobalColorTable = (fields & 0x80) != 0;
    int globalColorTableSize = 2 << (fields & 7);

    skipAndWriteBytes(2); // bgc index, aspect ratio

    if (hasGlobalColorTable) {
      ignoreColorTable(globalColorTableSize);
    }
  }

  private void ignoreColorTable(int numColors) throws IOException {
    skipAndWriteBytes(3 * numColors);
  }

  private int readBlock() throws IOException {
    int blockSize = readAndWriteNextByte();
    int n = 0;
    if (blockSize > 0) {
      while (n < blockSize) {
        n += readIntoBlock(n, blockSize - n);
      }
    }
    return n;
  }

  private void skipExtension() throws IOException {
    int size;
    do {
      size = readBlock();
    } while (size > 0);
  }

  private void skipImage() throws IOException {
    skipAndWriteBytes(8);

    int flags = readAndWriteNextByte();
    boolean hasLct = (flags & 0x80) != 0;
    if (hasLct) {
      int lctSize = 2 << (flags & 7);
      ignoreColorTable(lctSize);
    }
    skipAndWriteBytes(1);
    skipExtension();
  }

  private boolean isNetscape() {
    if (block.length < NETSCAPE.length) {
      return false;
    }

    for (int i = 0, N = NETSCAPE.length; i < N; i++) {
      if (NETSCAPE[i] != (char) block[i]) {
        return false;
      }
    }
    return true;
  }

  private void readNetscapeExtension() throws IOException {
    int size;
    do {
      size = readBlock();
      if (block[0] == 1) {
        mLoopCount = ((((int) block[2]) & 0xff) << 8) | (((int) block[1]) & 0xff);
      }
    } while (size > 0);
  }

  private void readGraphicsControlExtension(int[] control) throws IOException {
    skipAndWriteBytes(1);
    int flags = readAndWriteNextByte();
    control[CONTROL_INDEX_DISPOSE] = (flags & 0x1c) >> 2; // dispose
    control[CONTROL_INDEX_DELAY] = readTwoByteInt() * 10; // delay
    if (control[CONTROL_INDEX_DELAY] == 0) {
      control[CONTROL_INDEX_DELAY] = DEFAULT_FRAME_DURATION_MS;
      initFixedOutputStream();
    }
    writeTwoByteInt(control[CONTROL_INDEX_DELAY] / 10);
    skipAndWriteBytes(2);
  }

  private int readNextByte() throws IOException {
    int read = mInputStream.read();
    mCurrentOffset++;
    if (read == -1) {
      throw new EOFException("Unexpected end of gif file");
    }
    return read;
  }

  private int readTwoByteInt() throws IOException {
    return readNextByte() | (readNextByte() << 8);
  }

  private int readIntoBlock(int offset, int length) throws IOException {
    int count = mInputStream.read(block, offset, length);
    mCurrentOffset += length;
    if (shouldFixStream) {
      mOutputStream.write(block, offset, length);
    }
    if (count == -1) {
      throw new EOFException("Unexpected end of gif file");
    }
    return count;
  }

  private int readAndWriteNextByte() throws IOException {
    int read = readNextByte();
    writeNextByte(read);
    return read;
  }

  private void writeNextByte(int b) throws IOException {
    if (shouldFixStream) {
      mOutputStream.write(b);
    }
  }

  private void writeTwoByteInt(int content) throws IOException {
    writeNextByte(content);
    writeNextByte(content >> 8);
  }

  private void skipAndWriteBytes(int length) throws IOException {
    if (shouldFixStream) {
      copyFromIsToOs(mInputStream, mOutputStream, length);
    } else {
      mInputStream.skip(length);
    }
    mCurrentOffset += length;
  }

  private void initFixedOutputStream() throws IOException {
    if (shouldFixStream || mOutputStream == null) {
      return;
    }
    shouldFixStream = true;
    mInputStream.reset();
    copyFromIsToOs(mInputStream, mOutputStream, mCurrentOffset - 2);
    mInputStream.skip(2);
  }

  private void copyFromIsToOs(InputStream in, OutputStream out, int length) throws IOException {
    while (length > 0) {
      int bytesRead = in.read(block, 0, Math.min(MAX_BLOCK_SIZE, length));
      length -= MAX_BLOCK_SIZE;
      out.write(block, 0, bytesRead);
    }
  }
}
