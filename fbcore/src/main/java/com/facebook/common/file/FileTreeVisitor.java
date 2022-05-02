/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.file;

import java.io.File;

/**
 * An instance of this interface must be passed to FileTree.walkFileTree method in order to execute
 * some logic while iterating over the directory descendants. Java 7 provides a FileVisitor
 * interface and a Files.walkFileTree method that does this same thing (with more options).
 */
public interface FileTreeVisitor {

  /** Called before iterating over a directory (including the root directory of the iteration) */
  void preVisitDirectory(File directory);

  /** Called for each file contained in a directory (after preVisitDirectory) */
  void visitFile(File file);

  /** Called after iterating over a directory (including the root directory of the iteration) */
  void postVisitDirectory(File directory);
}
