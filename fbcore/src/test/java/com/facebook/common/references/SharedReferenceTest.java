/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.references;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.facebook.common.internal.Closeables;
import java.io.Closeable;
import java.io.IOException;
import javax.annotation.Nullable;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;

/** Basic tests for shared references */
@RunWith(RobolectricTestRunner.class)
public class SharedReferenceTest {

  /** Tests out the basic operations (isn't everything a basic operation?) */
  @Test
  public void testBasic() {

    // ref count = 1 after creation
    SharedReference<Thing> tRef = new SharedReference<Thing>(new Thing("abc"), THING_RELEASER);
    assertThat(SharedReference.isValid(tRef)).isTrue();
    assertThat(tRef.getRefCountTestOnly()).isEqualTo(1);
    Thing t = tRef.get();
    assertThat(t.get()).isEqualTo("abc");

    // adding a reference increases the ref count
    tRef.addReference();
    assertThat(SharedReference.isValid(tRef)).isTrue();
    assertThat(tRef.getRefCountTestOnly()).isEqualTo(2);
    assertThat(tRef.get()).isEqualTo(t);
    assertThat(t.get()).isEqualTo("abc");

    // deleting a reference drops the reference count
    tRef.deleteReference();
    assertThat(SharedReference.isValid(tRef)).isTrue();
    assertThat(tRef.getRefCountTestOnly()).isEqualTo(1);
    assertThat(tRef.get()).isEqualTo(t);
    assertThat(t.get()).isEqualTo("abc");

    // when the last reference is gone, the underlying object is disposed
    tRef.deleteReference();
    assertThat(SharedReference.isValid(tRef)).isFalse();
    assertThat(tRef.getRefCountTestOnly()).isEqualTo(0);

    // adding a reference now should fail
    try {
      tRef.addReference();
      fail("Expected NullReferenceException");
    } catch (SharedReference.NullReferenceException e) {
      // do nothing
    }

    // so should deleting a reference
    try {
      tRef.deleteReference();
      fail("Expected NullReferenceException");
    } catch (SharedReference.NullReferenceException e) {
      // do nothing
    }

    // null shared references are not 'valid'
    assertThat(SharedReference.isValid(null)).isFalse();

    // test out exceptions during a close
    SharedReference<Thing> t2Ref = new SharedReference<Thing>(new Thing2("abc"), THING_RELEASER);
    // this should not throw
    t2Ref.deleteReference();
  }

  @Test
  public void testNewSharedReference() {
    final Thing thing = new Thing("abc");
    assertThat(new SharedReference(thing, THING_RELEASER).get()).isSameAs(thing);
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
    @Nullable private String mValue;

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

  /** A subclass of Thing that throws an exception on close */
  public static class Thing2 extends Thing {
    private String mValue;

    public Thing2(String value) {
      super(value);
    }

    public void close() throws IOException {
      throw new IOException("");
    }
  }

  public final ResourceReleaser<Thing> THING_RELEASER =
      new ResourceReleaser<Thing>() {
        @Override
        public void release(Thing value) {
          try {
            Closeables.close(value, true);
          } catch (IOException ioe) {
            // this should not happen
            fail("this should not happen");
          }
        }
      };
}
