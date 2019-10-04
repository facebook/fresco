/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf.data.impl;

import android.net.Uri;
import com.facebook.samples.scrollperf.data.Decorator;
import com.facebook.samples.scrollperf.data.SimpleAdapter;

/** This decorates a Uri adding a distinct parameter */
public enum DistinctUriDecorator implements Decorator<Uri> {
  SINGLETON;

  @Override
  public Uri decorate(SimpleAdapter<Uri> decoratee, int position) {
    final int pos = position % decoratee.getSize();
    final Uri srcUri = decoratee.get(position);
    return Uri.parse(srcUri.toString() + "?param=" + pos);
  }
}
