/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.file;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import org.junit.Test;

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
      fail();
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
      fail();
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
      fail();
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
      fail();
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
      fail();
    } catch (FileUtils.RenameException re) {
      assertTrue(re.getCause() instanceof FileUtils.ParentDirNotFoundException);
    }
  }
}
