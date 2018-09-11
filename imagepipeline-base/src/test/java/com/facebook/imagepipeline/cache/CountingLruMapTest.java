/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import static org.junit.Assert.*;

import com.facebook.common.internal.Predicate;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.*;
import org.junit.runner.*;
import org.robolectric.*;

@RunWith(RobolectricTestRunner.class)
public class CountingLruMapTest {

  private CountingLruMap<String, Integer> mCountingLruMap;

  @Before
  public void setUp() {
    ValueDescriptor<Integer> valueDescriptor =
        new ValueDescriptor<Integer>() {
      @Override
      public int getSizeInBytes(Integer value) {
        return value;
      }
    };
    mCountingLruMap = new CountingLruMap<>(valueDescriptor);
  }

  @Test
  public void testInitialState() {
    assertEquals(0, mCountingLruMap.getCount());
    assertEquals(0, mCountingLruMap.getSizeInBytes());
  }

  @Test
  public void testPut() {
    // last inserted element should be last in the queue
    mCountingLruMap.put("key1", 110);
    assertEquals(1, mCountingLruMap.getCount());
    assertEquals(110, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1");
    assertValueOrder(110);

    mCountingLruMap.put("key2", 120);
    assertEquals(2, mCountingLruMap.getCount());
    assertEquals(230, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1", "key2");
    assertValueOrder(110, 120);

    mCountingLruMap.put("key3", 130);
    assertEquals(3, mCountingLruMap.getCount());
    assertEquals(360, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1", "key2", "key3");
    assertValueOrder(110, 120, 130);
  }

  @Test
  public void testPut_SameKeyTwice() {
    mCountingLruMap.put("key1", 110);
    mCountingLruMap.put("key2", 120);
    mCountingLruMap.put("key3", 130);

    // last inserted element should be last in the queue
    mCountingLruMap.put("key2", 150);
    assertEquals(3, mCountingLruMap.getCount());
    assertEquals(390, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1", "key3", "key2");
    assertValueOrder(110, 130, 150);
  }

  @Test
  public void testGet() {
    mCountingLruMap.put("key1", 110);
    mCountingLruMap.put("key2", 120);
    mCountingLruMap.put("key3", 130);

    // get shouldn't affect the ordering, nor the size
    assertEquals(120, (Object) mCountingLruMap.get("key2"));
    assertEquals(3, mCountingLruMap.getCount());
    assertEquals(360, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1", "key2", "key3");
    assertValueOrder(110, 120, 130);

    assertEquals(110, (Object) mCountingLruMap.get("key1"));
    assertEquals(3, mCountingLruMap.getCount());
    assertEquals(360, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1", "key2", "key3");
    assertValueOrder(110, 120, 130);

    assertEquals(null, (Object) mCountingLruMap.get("key4"));
    assertEquals(3, mCountingLruMap.getCount());
    assertEquals(360, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1", "key2", "key3");
    assertValueOrder(110, 120, 130);

    assertEquals(130, (Object) mCountingLruMap.get("key3"));
    assertEquals(3, mCountingLruMap.getCount());
    assertEquals(360, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1", "key2", "key3");
    assertValueOrder(110, 120, 130);
  }

  @Test
  public void testContains() {
    mCountingLruMap.put("key1", 110);
    mCountingLruMap.put("key2", 120);
    mCountingLruMap.put("key3", 130);

    // contains shouldn't affect the ordering, nor the size
    assertTrue(mCountingLruMap.contains("key2"));
    assertEquals(3, mCountingLruMap.getCount());
    assertEquals(360, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1", "key2", "key3");
    assertValueOrder(110, 120, 130);

    assertTrue(mCountingLruMap.contains("key1"));
    assertEquals(3, mCountingLruMap.getCount());
    assertEquals(360, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1", "key2", "key3");
    assertValueOrder(110, 120, 130);

    assertFalse(mCountingLruMap.contains("key4"));
    assertEquals(3, mCountingLruMap.getCount());
    assertEquals(360, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1", "key2", "key3");
    assertValueOrder(110, 120, 130);

    assertTrue(mCountingLruMap.contains("key3"));
    assertEquals(3, mCountingLruMap.getCount());
    assertEquals(360, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1", "key2", "key3");
    assertValueOrder(110, 120, 130);
  }

  @Test
  public void testRemove() {
    mCountingLruMap.put("key1", 110);
    mCountingLruMap.put("key2", 120);
    mCountingLruMap.put("key3", 130);

    assertEquals(120, (Object) mCountingLruMap.remove("key2"));
    assertEquals(2, mCountingLruMap.getCount());
    assertEquals(240, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1", "key3");
    assertValueOrder(110, 130);

    assertEquals(130, (Object) mCountingLruMap.remove("key3"));
    assertEquals(1, mCountingLruMap.getCount());
    assertEquals(110, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1");
    assertValueOrder(110);

    assertEquals(null, (Object) mCountingLruMap.remove("key4"));
    assertEquals(1, mCountingLruMap.getCount());
    assertEquals(110, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1");
    assertValueOrder(110);

    assertEquals(110, (Object) mCountingLruMap.remove("key1"));
    assertEquals(0, mCountingLruMap.getCount());
    assertEquals(0, mCountingLruMap.getSizeInBytes());
    assertKeyOrder();
    assertValueOrder();
  }

  @Test
  public void testRemoveAll() {
    mCountingLruMap.put("key1", 110);
    mCountingLruMap.put("key2", 120);
    mCountingLruMap.put("key3", 130);
    mCountingLruMap.put("key4", 140);

    mCountingLruMap.removeAll(
        new Predicate<String>() {
          @Override
          public boolean apply(String key) {
            return key.equals("key2") || key.equals("key3");
          }
        });
    assertEquals(2, mCountingLruMap.getCount());
    assertEquals(250, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1", "key4");
    assertValueOrder(110, 140);
  }

  @Test
  public void testClear() {
    mCountingLruMap.put("key1", 110);
    mCountingLruMap.put("key2", 120);
    mCountingLruMap.put("key3", 130);
    mCountingLruMap.put("key4", 140);

    mCountingLruMap.clear();
    assertEquals(0, mCountingLruMap.getCount());
    assertEquals(0, mCountingLruMap.getSizeInBytes());
    assertKeyOrder();
    assertValueOrder();
  }

  @Test
  public void testGetMatchingEntries() {
    mCountingLruMap.put("key1", 110);
    mCountingLruMap.put("key2", 120);
    mCountingLruMap.put("key3", 130);
    mCountingLruMap.put("key4", 140);

    List<LinkedHashMap.Entry<String, Integer>> entries =  mCountingLruMap.getMatchingEntries(
        new Predicate<String>() {
          @Override
          public boolean apply(String key) {
            return key.equals("key2") || key.equals("key3");
          }
        });
    assertNotNull(entries);
    assertEquals(2, entries.size());
    assertEquals("key2", entries.get(0).getKey());
    assertEquals(120, (int) entries.get(0).getValue());
    assertEquals("key3", entries.get(1).getKey());
    assertEquals(130, (int) entries.get(1).getValue());
    // getting entries should not affect the order nor the size
    assertEquals(4, mCountingLruMap.getCount());
    assertEquals(500, mCountingLruMap.getSizeInBytes());
    assertKeyOrder("key1", "key2", "key3", "key4");
    assertValueOrder(110, 120, 130, 140);
  }

  @Test
  public void testGetFirstKey() {
    mCountingLruMap.put("key1", 110);
    assertKeyOrder("key1");
    assertValueOrder(110);
    assertEquals("key1", mCountingLruMap.getFirstKey());

    mCountingLruMap.put("key2", 120);
    assertKeyOrder("key1", "key2");
    assertValueOrder(110, 120);
    assertEquals("key1", mCountingLruMap.getFirstKey());

    mCountingLruMap.put("key3", 130);
    assertKeyOrder("key1", "key2", "key3");
    assertValueOrder(110, 120, 130);
    assertEquals("key1", mCountingLruMap.getFirstKey());

    mCountingLruMap.put("key1", 140);
    assertKeyOrder("key2", "key3", "key1");
    assertValueOrder(120, 130, 140);
    assertEquals("key2", mCountingLruMap.getFirstKey());

    mCountingLruMap.remove("key3");
    assertKeyOrder("key2", "key1");
    assertValueOrder(120, 140);
    assertEquals("key2", mCountingLruMap.getFirstKey());

    mCountingLruMap.remove("key2");
    assertKeyOrder("key1");
    assertValueOrder(140);
    assertEquals("key1", mCountingLruMap.getFirstKey());

    mCountingLruMap.remove("key1");
    assertKeyOrder();
    assertValueOrder();
    assertEquals(null, mCountingLruMap.getFirstKey());
  }

  private void assertKeyOrder(String... expectedKeys) {
    assertArrayEquals(expectedKeys, mCountingLruMap.getKeys().toArray());
  }

  private void assertValueOrder(Integer... expectedVlues) {
    assertArrayEquals(expectedVlues, mCountingLruMap.getValues().toArray());
  }
}
