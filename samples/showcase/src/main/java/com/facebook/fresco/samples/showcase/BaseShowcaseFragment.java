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
package com.facebook.fresco.samples.showcase;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;

/**
 * A base classe for ShowcaseFragment
 */

public abstract class BaseShowcaseFragment extends Fragment implements ShowcaseFragment {

  @Nullable
  @Override
  public String getBackstackTag() {
    return null;
  }

  public ImageUriProvider sampleUris() {
    return ImageUriProvider.getInstance(getContext());
  }
}
