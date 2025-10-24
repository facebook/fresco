/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.file;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import org.junit.Test;

/** Unit tests for {@link FileUtils} */
public class FileUtilsTest {

  @Test
  public void testMkDirsNoWorkRequired() throws Exception {
    File directory = mock(File.class);
    when(directory.exists()).thenReturn(true);
    when(directory.isDirectory()).thenReturn(true);
    FileUtils.mkdirs(directory);
  }

  @Test
  public void testMkDirsSuccessfulCreate() throws Exception {
    File directory = mock(File.class);
    when(directory.exists()).thenReturn(false);
    when(directory.mkdirs()).thenReturn(true);
    when(directory.isDirectory()).thenReturn(true);
    FileUtils.mkdirs(directory);
  }

  @Test
  public void testMkDirsCantDeleteExisting() {
    File directory = mock(File.class);
    when(directory.exists()).thenReturn(true);
    when(directory.isDirectory()).thenReturn(false);
    when(directory.delete()).thenReturn(false);
    assertThatThrownBy(() -> FileUtils.mkdirs(directory))
        .isInstanceOf(FileUtils.CreateDirectoryException.class)
        .hasCauseInstanceOf(FileUtils.FileDeleteException.class);
  }

  @Test
  public void testRenameSuccessful() throws Exception {
    File sourceFile = mock(File.class);
    File targetFile = mock(File.class);

    when(sourceFile.renameTo(targetFile)).thenReturn(true);

    FileUtils.rename(sourceFile, targetFile);
  }

  @Test
  public void testParentDirNotFoundExceptionIsThrown() {
    File parentFile = mock(File.class);
    File sourceFile = mock(File.class);
    File targetFile = mock(File.class);

    when(sourceFile.getParentFile()).thenReturn(parentFile);
    when(sourceFile.getAbsolutePath()).thenReturn("<source>");
    when(targetFile.getAbsolutePath()).thenReturn("<destination>");

    assertThatThrownBy(() -> FileUtils.rename(sourceFile, targetFile))
        .isInstanceOf(FileUtils.RenameException.class)
        .hasCauseInstanceOf(FileUtils.ParentDirNotFoundException.class);
  }
}
