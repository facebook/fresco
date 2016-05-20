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
package com.facebook.samples.scrollperf.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.samples.scrollperf.R;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.util.UI;

public class MainFragment extends Fragment {

  public static final String TAG = MainFragment.class.getSimpleName();

  private Config mConfig;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setHasOptionsMenu(true);
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater,
      ViewGroup container,
      Bundle savedInstanceState) {
    mConfig = Config.load(getContext());
    // We use a different layout based on the type of output
    final View layout;
    switch (mConfig.mRecyclerLayoutType) {
      case "recyclerview_recycler_layout":
        layout = inflater.inflate(R.layout.content_recyclerview, container, false);
        initializeRecyclerView(layout);
        break;
      case "listview_recycler_layout":
        layout = inflater.inflate(R.layout.content_listview, container, false);
        initializeListView(layout);
        break;
      default:
        throw new IllegalStateException("Recycler Layout not supported");
    }
    return layout;
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.menu, menu);
    super.onCreateOptionsMenu(menu, inflater);
  }

  private void initializeRecyclerView(final View layout) {

  }

  private void initializeListView(final View layout) {

  }
}
