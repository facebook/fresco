/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.binaryresource;

import com.facebook.common.internal.Files;
import com.facebook.common.internal.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

/*
 * Implementation of BinaryResource based on a real file. @see BinaryResource for more details.
 */
public class FileBinaryResource implements BinaryResource {
  private final File mFile;

  private FileBinaryResource(File file) {
    mFile = Preconditions.checkNotNull(file);
  }

  public File getFile() {
    return mFile;
  }

  @Override
  public InputStream openStream() throws IOException {
    return new FileInputStream(mFile);
  }

  @Override
  public long size() {
    return mFile.length(); // 0L if file doesn't exist
  }

  @Override
  public byte[] read() throws IOException {
    return Files.toByteArray(mFile);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof FileBinaryResource)) {
      return false;
    }
    FileBinaryResource that = (FileBinaryResource)obj;
    return mFile.equals(that.mFile);
  }

  @Override
  public int hashCode() {
    return mFile.hashCode();
  }

  /*
   * Factory method to create a wrapping BinaryResource without explicitly taking care of null.
   * If the supplied file is null, instead of BinaryResource, null is returned.
   */
  public static @Nullable FileBinaryResource createOrNull(File file) {
    return (file != null) ? new FileBinaryResource(file) : null;
  }
}
