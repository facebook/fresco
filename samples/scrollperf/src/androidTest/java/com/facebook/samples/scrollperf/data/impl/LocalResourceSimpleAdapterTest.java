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

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.test.AndroidTestCase;
import com.facebook.samples.scrollperf.R;
import com.facebook.samples.scrollperf.data.SimpleAdapter;
import org.junit.Assert;

/**
 * This is the Unit Test class for the LocalResourceSimpleAdapterTest
 */
public class LocalResourceSimpleAdapterTest extends AndroidTestCase {

  public void testEagerAdapter() {
    final Context context = getContext();
    final Resources res = context.getResources();
    final String[] uris = res.getStringArray(R.array.local_uri_test);
    Assert.assertNotNull(uris);
    SimpleAdapter<Uri> simpleAdapter = LocalResourceSimpleAdapter
        .getEagerAdapter(context, R.array.local_uri_test);
    checkSimpleAdapterData(simpleAdapter, uris.length);
    Assert.assertFalse(simpleAdapter.isLazy());
  }

  public void testLazyAdapter() {
    final Context context = getContext();
    final Resources res = context.getResources();
    final String[] uris = res.getStringArray(R.array.local_uri_test);
    Assert.assertNotNull(uris);
    SimpleAdapter<Uri> simpleAdapter = LocalResourceSimpleAdapter
        .getEagerAdapter(context, R.array.local_uri_test);
    checkSimpleAdapterData(simpleAdapter, uris.length);
    Assert.assertTrue(simpleAdapter.isLazy());
  }

  private void checkSimpleAdapterData(SimpleAdapter<Uri> simpleAdapter, int requestedSize) {
    Assert.assertNotNull(simpleAdapter);
    Assert.assertEquals(10, requestedSize);
    Assert.assertEquals(simpleAdapter.getSize(), requestedSize);
    Uri firstUri = simpleAdapter.get(0);
    assertNotNull(firstUri);
    assertEquals(Uri.parse("http://myserver.com/file1"), firstUri);
    Uri lastUri = simpleAdapter.get(simpleAdapter.getSize() - 1);
    assertNotNull(lastUri);
    assertEquals(Uri.parse("http://myserver.com/file10"), lastUri);
  }
}
