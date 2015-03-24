/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.objectpool;

import java.util.HashMap;

import com.facebook.common.time.MonotonicClock;

/**
 * Manager for storing and retrieving object pools
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class ObjectPoolManager {
  private final HashMap<Class, ObjectPool> mObjectPools;
  private final MonotonicClock mClock;

  public ObjectPoolManager(MonotonicClock clock) {
    mObjectPools = new HashMap<Class, ObjectPool>();
    mClock = clock;
  }

  /**
   * Create a new pool builder.  Note that the pool is not actually constructed until
   * ObjectPoolBuilder.build() is called
   * @param clazz the class object for the type which is going to be stored in the pool
   * @param <T> type of object to pool
   * @return a builder object to configure and build the pool
   */
  public <T> ObjectPoolBuilder<T> createPoolBuilder(Class<T> clazz) {
    return new ObjectPoolBuilder<T>(this, clazz, mClock);
  }

  /* package */ <T> void addPool(Class<T> clazz, ObjectPool<T> pool) {
    mObjectPools.put(clazz, pool);
  }

  /**
   * Retrieve the ObjectPool created for the specified class type
   * @param clazz the class object for the type which is pooled
   * @param <T> type of object which is pooled
   * @return an object pool for the given type
   */
  public <T> ObjectPool<T> getPool(Class<T> clazz) {
    return (ObjectPool<T>) mObjectPools.get(clazz);
  }

  /**
   * Convenience method which retrieves the pool and requests an allocation from the pool, returning
   * the allocated object
   * @param clazz the class object for the type which is pooled
   * @param <T> type of object which is pooled
   * @return an instance of the type from the pooler, if it exists
   */
  public <T> T allocate(Class<T> clazz) {
    ObjectPool<T> pool = getPool(clazz);
    if (pool != null) {
      return pool.allocate();
    } else {
      return null;
    }
  }

  /**
   * Convenience method to release an object back to the corresponding pool
   * @param clazz the class object for the type which is pooled
   * @param obj an instance of the type to return to the pooler
   * @param <T> type of the object which is pooled
   */
  public <T> void release(Class<T> clazz, T obj) {
    ObjectPool<T> pool = getPool(clazz);
    if (pool != null) {
      pool.release(obj);
    }
  }
}
