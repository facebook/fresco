/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.facebook.common.internal.Preconditions;

/**
 * Static operations on {@link File}s
 */
public class FileUtils {

  /**
   * Creates the specified directory, along with all parent paths if necessary
   * @param directory directory to be created
   * @throws CreateDirectoryException
   */
  public static void mkdirs(File directory) throws CreateDirectoryException {
    if (directory.exists()) {
      // file exists and *is* a directory
      if (directory.isDirectory()) {
        return;
      }

      // file exists, but is not a directory - delete it
      if (!directory.delete()) {
        throw new CreateDirectoryException(
            directory.getAbsolutePath(),
            new FileDeleteException(directory.getAbsolutePath()));
      }
    }

    // doesn't exist. Create one
    if (!directory.mkdirs() && !directory.isDirectory()) {
      throw new CreateDirectoryException(directory.getAbsolutePath());
    }
  }

  /**
   * Renames the source file to the target file. If the target file exists, then we attempt to
   * delete it. If the delete or the rename operation fails, then we raise an exception
   * @param source the source file
   * @param target the new 'name' for the source file
   * @throws IOException
   */
  public static void rename(File source, File target) throws RenameException {
    Preconditions.checkNotNull(source);
    Preconditions.checkNotNull(target);

    // delete the target first - but ignore the result
    target.delete();

    if (source.renameTo(target)) {
      return;
    }

    Throwable innerException = null;
    if (target.exists()) {
      innerException = new FileDeleteException(target.getAbsolutePath());
    } else if (!source.getParentFile().exists()) {
      innerException = new ParentDirNotFoundException(source.getAbsolutePath());
    } else if (!source.exists()) {
      innerException = new FileNotFoundException(source.getAbsolutePath());
    }

    throw new RenameException(
        "Unknown error renaming " + source.getAbsolutePath() + " to " + target.getAbsolutePath(),
        innerException);
  }

  /**
   * Represents an exception during directory creation
   */
  public static class CreateDirectoryException extends IOException {
    public CreateDirectoryException(String message) {
      super(message);
    }

    public CreateDirectoryException(String message, Throwable innerException) {
      super(message);
      initCause(innerException);
    }
  }

  /**
   * A specialization of FileNotFoundException when the parent-dir doesn't exist
   */
  public static class ParentDirNotFoundException extends FileNotFoundException {
    public ParentDirNotFoundException(String message) {
      super(message);
    }
  }

  /**
   * Represents an exception when the target file/directory cannot be deleted
   */
  public static class FileDeleteException extends IOException {
    public FileDeleteException(String message) {
      super(message);
    }
  }

  /**
   * Represents an unknown rename exception
   */
  public static class RenameException extends IOException {
    public RenameException(String message) {
      super(message);
    }

    public RenameException(String message, Throwable innerException) {
      super(message);
      initCause(innerException);

    }
  }
}
