/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.statfs;

import java.io.File;

import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link StatFsHelper}.
 */
@RunWith(RobolectricTestRunner.class)
@PrepareForTest({Environment.class, StatFsHelper.class, SystemClock.class})
@Ignore("t6344387")
public class StatFsHelperTest {

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  private File mMockFileInternal;
  private File mMockFileExternal;
  private StatFs mMockStatFsInternal;
  private StatFs mMockStatFsExternal;

  private static final String INTERNAL_PATH = "/data";
  private static final String EXTERNAL_PATH = "/mnt/sdcard/data";

  private static final int INTERNAL_BLOCK_SIZE = 512;
  private static final int EXTERNAL_BLOCK_SIZE = 2048;

  private static final int INTERNAL_BLOCKS_FREE = 16;
  private static final int EXTERNAL_BLOCKS_FREE = 32;

  @Before
  public void setUp() {
    PowerMockito.mockStatic(Environment.class);
    PowerMockito.mockStatic(StatFsHelper.class);
    PowerMockito.mockStatic(SystemClock.class);
    mMockFileInternal = mock(File.class);
    mMockFileExternal = mock(File.class);
    mMockStatFsInternal = mock(StatFs.class);
    mMockStatFsExternal = mock(StatFs.class);
    PowerMockito.when(SystemClock.elapsedRealtime()).thenReturn(System.currentTimeMillis());
  }

  private void expectInternalSetup() {
    PowerMockito.when(Environment.getDataDirectory()).thenReturn(mMockFileInternal);
    when(mMockFileInternal.getAbsolutePath()).thenReturn(INTERNAL_PATH);
    when(mMockFileInternal.exists()).thenReturn(true);
    PowerMockito.when(StatFsHelper.createStatFs(INTERNAL_PATH)).thenReturn(mMockStatFsInternal);
    when(mMockStatFsInternal.getBlockSize()).thenReturn(INTERNAL_BLOCK_SIZE);
    when(mMockStatFsInternal.getAvailableBlocks()).thenReturn(INTERNAL_BLOCKS_FREE);
  }

  private void expectExternalSetup() {
    PowerMockito.when(Environment.getExternalStorageDirectory()).thenReturn(mMockFileExternal);
    when(mMockFileExternal.getAbsolutePath()).thenReturn(EXTERNAL_PATH);
    when(mMockFileExternal.exists()).thenReturn(true);
    PowerMockito.when(StatFsHelper.createStatFs(EXTERNAL_PATH)).thenReturn(mMockStatFsExternal);
    when(mMockStatFsExternal.getBlockSize()).thenReturn(EXTERNAL_BLOCK_SIZE);
    when(mMockStatFsExternal.getAvailableBlocks()).thenReturn(EXTERNAL_BLOCKS_FREE);
  }

  @Test
  public void testShouldCreateStatFsForInternalAndExternalStorage() {

    expectInternalSetup();
    expectExternalSetup();

    StatFsHelper statFsHelper = new StatFsHelper();

    long freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.INTERNAL);
    assertEquals(INTERNAL_BLOCK_SIZE * INTERNAL_BLOCKS_FREE, freeBytes);

    freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.EXTERNAL);
    assertEquals(EXTERNAL_BLOCK_SIZE * EXTERNAL_BLOCKS_FREE, freeBytes);

    statFsHelper.resetStats();

    verify(mMockStatFsInternal).restat(INTERNAL_PATH);
    verify(mMockStatFsExternal).restat(EXTERNAL_PATH);
  }

  @Test
  public void testShouldCreateStatFsForInternalStorageOnly() {

    expectInternalSetup();
    // Configure external storage to be absent.
    PowerMockito.when(Environment.getExternalStorageDirectory()).thenReturn(null);
    StatFsHelper statFsHelper = new StatFsHelper();

    long freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.INTERNAL);
    assertEquals(INTERNAL_BLOCK_SIZE * INTERNAL_BLOCKS_FREE, freeBytes);

    freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.EXTERNAL);
    assertEquals(0, freeBytes);

    statFsHelper.resetStats();

    verify(mMockStatFsInternal).restat(INTERNAL_PATH);
  }

  @Test
  public void testShouldHandleNoInternalStorage() {
    // Configure internal storage to be absent.
    PowerMockito.when(Environment.getDataDirectory()).thenReturn(null);
    // Configure external storage to be absent.
    PowerMockito.when(Environment.getExternalStorageDirectory()).thenReturn(null);

    StatFsHelper statFsHelper = new StatFsHelper();

    long freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.INTERNAL);
    assertEquals(0, freeBytes);

    freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.EXTERNAL);
    assertEquals(0, freeBytes);

    statFsHelper.resetStats();
  }


    @Test
  public void testShouldHandleExceptionOnExternalCacheCreate() {

    expectInternalSetup();

    // Configure external storage to be present but to throw an exception while instantiating
    // a new StatFs object for external storage.
    when(mMockFileExternal.getAbsolutePath()).thenReturn(EXTERNAL_PATH);
    when(mMockFileExternal.exists()).thenReturn(true);
    PowerMockito.when(StatFsHelper.createStatFs(EXTERNAL_PATH))
        .thenThrow(new IllegalArgumentException());

    StatFsHelper statFsHelper = new StatFsHelper();

    long freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.INTERNAL);
    assertEquals(INTERNAL_BLOCK_SIZE * INTERNAL_BLOCKS_FREE, freeBytes);

    freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.EXTERNAL);
    assertEquals(0, freeBytes);
  }

  @Test
  public void testShouldHandleExceptionOnExternalCacheRestat() {

    expectInternalSetup();
    expectExternalSetup();
    doThrow(new IllegalArgumentException()).when(mMockStatFsExternal).restat(EXTERNAL_PATH);

    StatFsHelper statFsHelper = new StatFsHelper();
    statFsHelper.resetStats();

    long freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.INTERNAL);
    assertEquals(INTERNAL_BLOCK_SIZE * INTERNAL_BLOCKS_FREE, freeBytes);

    freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.EXTERNAL);
    assertEquals(0, freeBytes);

    verify(mMockStatFsInternal).restat(INTERNAL_PATH);
  }

  @Test
  public void testShouldHandleExternalStorageRemoved() {

    expectInternalSetup();
    expectExternalSetup();

    // External dir is present on creation and missing on subsequent resetStatus() calls.
    when(mMockFileExternal.exists()).thenReturn(true).thenReturn(false);

    StatFsHelper statFsHelper = new StatFsHelper();
    statFsHelper.resetStats();

    long freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.INTERNAL);
    assertEquals(INTERNAL_BLOCK_SIZE * INTERNAL_BLOCKS_FREE, freeBytes);

    freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.EXTERNAL);
    assertEquals(0, freeBytes);

    verify(mMockStatFsInternal).restat(INTERNAL_PATH);
  }

  @Test
  public void testShouldHandleExternalStorageReinserted() {

    expectInternalSetup();
    expectExternalSetup();

    // External dir is present on creation, missing on first resetStatus() call, and back on
    // subsequent resetStatus() calls.
    when(mMockFileExternal.exists()).thenReturn(true).thenReturn(false).thenReturn(true);

    StatFsHelper statFsHelper = new StatFsHelper();
    statFsHelper.resetStats();

    long freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.INTERNAL);
    assertEquals(INTERNAL_BLOCK_SIZE * INTERNAL_BLOCKS_FREE, freeBytes);

    freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.EXTERNAL);
    assertEquals(0, freeBytes);

    statFsHelper.resetStats();

    freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.EXTERNAL);
    assertEquals(EXTERNAL_BLOCK_SIZE * EXTERNAL_BLOCKS_FREE, freeBytes);

    statFsHelper.resetStats();

    freeBytes = statFsHelper.getAvailableStorageSpace(StatFsHelper.StorageType.EXTERNAL);
    assertEquals(EXTERNAL_BLOCK_SIZE * EXTERNAL_BLOCKS_FREE, freeBytes);

    verify(mMockStatFsInternal, times(3)).restat(INTERNAL_PATH);
    verify(mMockStatFsExternal).restat(EXTERNAL_PATH);
  }
}
