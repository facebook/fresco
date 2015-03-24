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

/**
 * Utility class to visit a file tree.
 * There's similar functionality in Java 7's Files.walkFileTree method.
 * Its methods could be merge into FileUtil (although it seems to have a lot of
 * crazy redundant methods, maybe for testing, but crazy anyway).
 */
public class FileTree {

  /**
   * Iterates over the file tree of a directory. It receives a visitor and will call its methods
   * for each file in the directory.
   * preVisitDirectory (directory)
   * visitFile (file)
   * - recursively the same for every subdirectory
   * postVisitDirectory (directory)
   * @param directory the directory to iterate
   * @param visitor the visitor that will be invoked for each directory/file in the tree
   */
  public static void walkFileTree(File directory, FileTreeVisitor visitor) {
    visitor.preVisitDirectory(directory);
    File[] files = directory.listFiles();
    if (files != null) {
      for (File file: files) {
        if (file.isDirectory()) {
          walkFileTree(file, visitor);
        } else {
          visitor.visitFile(file);
        }
      }
    }
    visitor.postVisitDirectory(directory);
  }

  /**
   * Deletes all files and subdirectories in directory (doesn't delete the directory
   * passed as parameter).
   */
  public static boolean deleteContents(File directory) {
    File[] files = directory.listFiles();
    boolean success = true;
    if (files != null) {
      for (File file : files) {
        success &= deleteRecursively(file);
      }
    }
    return success;
  }

  /**
   * Deletes the file and if it's a directory deletes also any content in it
   * @param file a file or directory
   * @return true if the file/directory could be deleted
   */
  public static boolean deleteRecursively(File file) {
    if (file.isDirectory()) {
      deleteContents(file);
    }
    // if I can delete directory then I know everything was deleted
    return file.delete();
  }

}
