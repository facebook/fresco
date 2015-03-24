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

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FileUtils}
 */
public class FileUtilsTest {

  @Test
  public void testMkDirsNoWorkRequired() {
    File directory = mock(File.class);
    when(directory.exists()).thenReturn(true);
    when(directory.isDirectory()).thenReturn(true);
    try {
      FileUtils.mkdirs(directory);
    } catch (FileUtils.CreateDirectoryException cde) {
      assertTrue(false);
    }
  }

  @Test
  public void testMkDirsSuccessfulCreate() {
    File directory = mock(File.class);
    when(directory.exists()).thenReturn(false);
    when(directory.mkdirs()).thenReturn(true);
    when(directory.isDirectory()).thenReturn(true);
    try {
      FileUtils.mkdirs(directory);
    } catch (FileUtils.CreateDirectoryException cde) {
      assertTrue(false);
    }
  }

  @Test
  public void testMkDirsCantDeleteExisting() {
    File directory = mock(File.class);
    when(directory.exists()).thenReturn(true);
    when(directory.isDirectory()).thenReturn(false);
    when(directory.delete()).thenReturn(false);
    try {
      FileUtils.mkdirs(directory);
      assertTrue(false);
    } catch (FileUtils.CreateDirectoryException cde) {
      assertTrue(cde.getCause() instanceof FileUtils.FileDeleteException);
    }
  }

  @Test
  public void testRenameSuccessful() {
    File sourceFile = mock(File.class);
    File targetFile = mock(File.class);

    when(sourceFile.renameTo(targetFile)).thenReturn(true);

    try {
      FileUtils.rename(sourceFile, targetFile);
    } catch (FileUtils.RenameException re) {
      assertTrue(false);
    }
  }

  @Test
  public void testParentDirNotFoundExceptionIsThrown() {
    File parentFile = mock(File.class);
    File sourceFile = mock(File.class);
    File targetFile = mock(File.class);

    when(sourceFile.getParentFile()).thenReturn(parentFile);
    when(sourceFile.getAbsolutePath()).thenReturn("<source>");
    when(targetFile.getAbsolutePath()).thenReturn("<destination>");

    try {
      FileUtils.rename(sourceFile, targetFile);
      assertTrue(false);
    } catch (FileUtils.RenameException re) {
      assertTrue(re.getCause() instanceof FileUtils.ParentDirNotFoundException);
    }
  }
}
