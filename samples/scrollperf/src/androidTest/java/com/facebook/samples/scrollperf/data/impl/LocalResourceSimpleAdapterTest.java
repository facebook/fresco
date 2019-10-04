/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.data.impl;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.test.AndroidTestCase;
import com.facebook.samples.scrollperf.R;
import com.facebook.samples.scrollperf.data.SimpleAdapter;
import org.junit.Assert;

/** This is the Unit Test class for the LocalResourceSimpleAdapterTest */
public class LocalResourceSimpleAdapterTest extends AndroidTestCase {

  public void testEagerAdapter() {
    final Context context = getContext();
    final Resources res = context.getResources();
    final String[] uris = res.getStringArray(R.array.local_uri_test);
    Assert.assertNotNull(uris);
    SimpleAdapter<Uri> simpleAdapter =
        LocalResourceSimpleAdapter.getEagerAdapter(context, R.array.local_uri_test);
    checkSimpleAdapterData(simpleAdapter, uris.length);
    Assert.assertFalse(simpleAdapter.isLazy());
  }

  public void testLazyAdapter() {
    final Context context = getContext();
    final Resources res = context.getResources();
    final String[] uris = res.getStringArray(R.array.local_uri_test);
    Assert.assertNotNull(uris);
    SimpleAdapter<Uri> simpleAdapter =
        LocalResourceSimpleAdapter.getEagerAdapter(context, R.array.local_uri_test);
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
