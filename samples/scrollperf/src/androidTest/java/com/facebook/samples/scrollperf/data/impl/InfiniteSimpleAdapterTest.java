/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.facebook.samples.scrollperf.data.impl;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.test.AndroidTestCase;
import com.facebook.samples.scrollperf.R;
import com.facebook.samples.scrollperf.data.SimpleAdapter;
import org.mockito.Mock;

/**
 * We test the InfiniteSimpleAdapter
 */
public class InfiniteSimpleAdapterTest extends AndroidTestCase {

  @Mock
  private SimpleAdapter mSimpleAdapter;

  @Mock
  private Uri mUri;

  public void testInfiniteAdapterWhichIsEmpty() {
    final Context context = getContext();
    final Resources res = context.getResources();
    final String[] uris = new String[]{};
    when(mSimpleAdapter.getSize()).thenReturn(0);
    when(mSimpleAdapter.isLazy()).thenReturn(true);
    final SimpleAdapter infinite = SimpleAdapter.Util.makeItInfinite(mSimpleAdapter);
    assertEquals(0, infinite.getSize());
    assertTrue(infinite.isLazy());
  }

  public void testInfiniteAdapter() {
    final Context context = getContext();
    final Resources res = context.getResources();
    final String[] uris = res.getStringArray(R.array.local_uri_test);
    when(mSimpleAdapter.getSize()).thenReturn(10);
    when(mSimpleAdapter.isLazy()).thenReturn(true);
    when(mSimpleAdapter.get(0)).thenReturn(mUri);
    final SimpleAdapter infinite = SimpleAdapter.Util.makeItInfinite(mSimpleAdapter);
    assertEquals(Integer.MAX_VALUE, infinite.getSize());
    assertTrue(infinite.isLazy());
    assertSame(mUri, infinite.get(0));
    assertSame(mUri, infinite.get(10));
  }

  public void testAlreadyInfiniteAdapter() {
    final Context context = getContext();
    final Resources res = context.getResources();
    final String[] uris = res.getStringArray(R.array.local_uri_test);
    when(mSimpleAdapter.getSize()).thenReturn(Integer.MAX_VALUE);
    when(mSimpleAdapter.isLazy()).thenReturn(true);
    when(mSimpleAdapter.get(0)).thenReturn(mUri);
    final SimpleAdapter infinite = SimpleAdapter.Util.makeItInfinite(mSimpleAdapter);
    assertEquals(Integer.MAX_VALUE, infinite.getSize());
    assertTrue(infinite.isLazy());
    assertSame(mUri, infinite.get(0));
    assertSame(mUri, infinite.get(10));
    assertSame(infinite, mSimpleAdapter);
  }
}
