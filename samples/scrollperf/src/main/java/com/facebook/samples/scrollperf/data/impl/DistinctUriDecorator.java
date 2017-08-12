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

import android.net.Uri;
import com.facebook.samples.scrollperf.data.Decorator;
import com.facebook.samples.scrollperf.data.SimpleAdapter;

/**
 * This decorates a Uri adding a distinct parameter
 */
public enum DistinctUriDecorator implements Decorator<Uri> {

  SINGLETON;

  @Override
  public Uri decorate(SimpleAdapter<Uri> decoratee, int position) {
    final int pos = position % decoratee.getSize();
    final Uri srcUri = decoratee.get(position);
    return Uri.parse(srcUri.toString() + "?param=" + pos);
  }
}
