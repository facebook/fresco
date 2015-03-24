/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.objectpool;

import java.util.Vector;

import com.facebook.common.testing.FakeClock;
import com.facebook.testing.robolectric.v2.WithTestDefaultsRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.*;

/**
 * Test for {@link ObjectPool}, {@link ObjectPoolManager}.
 */
@RunWith(WithTestDefaultsRunner.class)
public class ObjectPoolTest {

  private FakeClock fakeClock;

  @Before
  public void setUp() throws Exception {
    this.fakeClock = new FakeClock();
  }

  @Test
  public void testPoolConstruction() {
    ObjectPoolManager manager = new ObjectPoolManager(fakeClock);
    TestAllocator alloc = new TestAllocator();
    ObjectPool<String> pool = manager.createPoolBuilder(String.class)
        .setMinimumSize(101)
        .setMaximumSize(303)
        .setIncrementSize(18)
        .setCompactionDelay(805)
        .setAllocator(alloc)
        .build();

    assertEquals(101, pool.getMinimumSize());
    assertEquals(303, pool.getMaximumSize());
    assertEquals(805, pool.getCompactionDelayMs());
    assertEquals(18, pool.getIncrementSize());

    // make sure the allocator we specified is actually the one attached to the pool
    String s = pool.allocate();
    assertEquals(1, alloc.allocateCallCount);
    pool.release(s);
    assertEquals(1, alloc.releaseCallCount);
  }

  @Test
  public void testPool() {
    ObjectPoolManager manager = new ObjectPoolManager(fakeClock);
    TestAllocator alloc = new TestAllocator();
    ObjectPool<String> pool = manager.createPoolBuilder(String.class)
        .setMinimumSize(16)
        .setMaximumSize(100)
        .setIncrementSize(16)
        .setCompactionDelay(100)
        .setAllocator(alloc)
        .build();

    // The new pool we got should be the same pool we get from the manager afterwards
    assertEquals(pool, manager.getPool(String.class));

    String s = pool.allocate();
    // Only check that create must be called at least this many times, to account for the pool
    // possibly eagerly creating objects to fill the pool
    assertTrue(alloc.createCallCount >= 1);
    // Our allocator should be notified whenever there is an allocation
    assertEquals(1, alloc.allocateCallCount);
    assertTrue(s != null);
    // Allocating one object should not have changed the backing size of the array
    assertEquals(pool.getMinimumSize(), pool.getPoolSize());
    int poolSize = pool.getPooledObjectCount();
    pool.release(s);
    // ... or a release
    assertEquals(1, alloc.releaseCallCount);
    // The object should have returned to the pool
    assertEquals(poolSize + 1, pool.getPooledObjectCount());
    // Our minimum size was enough that we should not have had to increase the pool size
    assertEquals(pool.getMinimumSize(), pool.getPoolSize());
    int prevCallCount = alloc.createCallCount;
    s = pool.allocate();
    // Reallocating from the pool should call our allocation function again even though it should
    // be serviced from the pool
    assertEquals(prevCallCount, alloc.createCallCount);
    assertEquals(2, alloc.allocateCallCount);


    alloc.resetCallCounts();
    Vector<String> store = new Vector<String>();
    for (int i = 0; i < pool.getMinimumSize() + 1; ++i) {
      store.add(pool.allocate());
    }
    // We shouldn't have increased the pool size during allocation
    assertEquals(pool.getMinimumSize(), pool.getPoolSize());
    for (String str : store) {
      pool.release(str);
    }
    store.clear();

    // ... but we should have upon releasing
    assertEquals(pool.getMinimumSize() + pool.getIncrementSize(), pool.getPoolSize());

    alloc.resetCallCounts();
    for (int i = 0; i < pool.getIncrementSize() * 2 + 1; ++i) {
      store.add(pool.allocate());
    }
    for (String str : store) {
      pool.release(str);
    }
    store.clear();
    pool.checkUsage();
    // We should not have resized our pool yet because the clock has not advanced
    assertEquals(pool.getMinimumSize() + pool.getIncrementSize() * 2, pool.getPoolSize());

    fakeClock.incrementBy(pool.getCompactionDelayMs() + 1);
    pool.checkUsage();
    // We should have lopped off incrementSize number of elements in our pool
    assertEquals(pool.getMinimumSize() + pool.getIncrementSize(), pool.getPoolSize());

    // If we force a compaction, then it must shrink
    pool.compactUsage();
    assertEquals(pool.getMinimumSize(), pool.getPoolSize());

    for (int i = 0; i < 2 * pool.getMaximumSize(); ++i) {
      store.add(pool.allocate());
    }
    for (String str : store) {
      pool.release(str);
    }
    store.clear();
    // Regardless of how many objects we allocate, we should only have up to the maximum
    assertEquals(pool.getMaximumSize(), pool.getPoolSize());
  }

  public class TestAllocator extends ObjectPool.BasicAllocator<String> {
    public int createCallCount;
    public int allocateCallCount;
    public int releaseCallCount;

    public TestAllocator() {
      super(String.class);
      resetCallCounts();
    }

    public void resetCallCounts() {
      createCallCount = 0;
      allocateCallCount = 0;
      releaseCallCount = 0;
    }

    @Override
    public String create() {
      ++createCallCount;
      String s = super.create();
      assertNotNull(s);
      return s;
    }

    @Override
    public void onAllocate(String obj) {
      ++allocateCallCount;
      assertNotNull(obj);
      super.onAllocate(obj);
    }

    @Override
    public void onRelease(String obj) {
      ++releaseCallCount;
      assertNotNull(obj);
      super.onRelease(obj);
    }
  }
}
