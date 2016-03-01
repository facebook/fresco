/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.references;

import java.io.Closeable;
import java.io.IOException;

import com.facebook.common.internal.Closeables;
import org.robolectric.RobolectricTestRunner;

import junit.framework.Assert;
import org.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

/**
 * Basic tests for shared references
 */
@RunWith(RobolectricTestRunner.class)
public class SharedReferenceTest {

  /**
   * Tests out the basic operations (isn't everything a basic operation?)
   */
  @Test
  public void testBasic() {

    // ref count = 1 after creation
    SharedReference<Thing> tRef = new SharedReference<Thing>(new Thing("abc"), THING_RELEASER);
    Assert.assertTrue(SharedReference.isValid(tRef));
    Assert.assertEquals(1, tRef.getRefCountTestOnly());
    Thing t = tRef.get();
    Assert.assertEquals("abc", t.get());

    // adding a reference increases the ref count
    tRef.addReference();
    Assert.assertTrue(SharedReference.isValid(tRef));
    Assert.assertEquals(2, tRef.getRefCountTestOnly());
    Assert.assertEquals(t, tRef.get());
    Assert.assertEquals("abc", t.get());

    // deleting a reference drops the reference count
    tRef.deleteReference();
    Assert.assertTrue(SharedReference.isValid(tRef));
    Assert.assertEquals(1, tRef.getRefCountTestOnly());
    Assert.assertEquals(t, tRef.get());
    Assert.assertEquals("abc", t.get());

    // when the last reference is gone, the underlying object is disposed
    tRef.deleteReference();
    Assert.assertFalse(SharedReference.isValid(tRef));
    Assert.assertEquals(0, tRef.getRefCountTestOnly());

    // adding a reference now should fail
    try {
      tRef.addReference();
      Assert.fail();
    } catch (SharedReference.NullReferenceException e) {
      // do nothing
    }

    // so should deleting a reference
    try {
      tRef.deleteReference();
      Assert.fail();
    } catch (SharedReference.NullReferenceException e) {
      // do nothing
    }

    // null shared references are not 'valid'
    Assert.assertFalse(SharedReference.isValid(null));

    // test out exceptions during a close
    SharedReference<Thing> t2Ref = new SharedReference<Thing>(new Thing2("abc"), THING_RELEASER);
    // this should not throw
    t2Ref.deleteReference();
  }

  @Test
  public void testNewSharedReference() {
    final Thing thing = new Thing("abc");
    Assert.assertSame(thing, new SharedReference(thing, THING_RELEASER).get());
  }

  @Test
  public void testCustomReleaser() {
    final Thing thing = new Thing("abc");
    final ResourceReleaser releaser = Mockito.mock(ResourceReleaser.class);
    final SharedReference<Thing> tRef = new SharedReference<Thing>(thing, releaser);
    tRef.deleteReference();
    Mockito.verify(releaser, Mockito.times(1)).release(thing);
  }

  public static class Thing implements Closeable {
    private String mValue;

    public Thing(String value) {
      mValue = value;
    }

    public String get() {
      return mValue;
    }

    public void close() throws IOException {
      mValue = null;
    }
  }

  /**
   * A subclass of Thing that throws an exception on close
   */
  public static class Thing2 extends Thing {
    private String mValue;

    public Thing2(String value) {
      super(value);
    }

    public void close() throws IOException {
      throw new IOException("");
    }
  }

  public final ResourceReleaser<Thing> THING_RELEASER = new ResourceReleaser<Thing>() {
    @Override
    public void release(Thing value) {
      try {
        Closeables.close(value, true);
      } catch (IOException ioe) {
        // this should not happen
        Assert.fail();
      }
    }
  };
}
