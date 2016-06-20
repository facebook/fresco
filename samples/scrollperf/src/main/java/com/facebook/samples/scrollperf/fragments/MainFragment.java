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

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.facebook.samples.scrollperf.R;
import com.facebook.samples.scrollperf.conf.Config;
import com.facebook.samples.scrollperf.data.SimpleAdapter;
import com.facebook.samples.scrollperf.data.impl.ContentProviderSimpleAdapter;
import com.facebook.samples.scrollperf.data.impl.DistinctUriDecorator;
import com.facebook.samples.scrollperf.data.impl.LocalResourceSimpleAdapter;
import com.facebook.samples.scrollperf.fragments.recycler.DraweeViewAdapter;
import com.facebook.samples.scrollperf.fragments.recycler.DraweeViewListAdapter;
import com.facebook.samples.scrollperf.util.UI;

public class MainFragment extends Fragment {

  public static final String TAG = MainFragment.class.getSimpleName();

  private RecyclerView mRecyclerView;

  private ListView mListView;

  private DraweeViewAdapter mDraweeViewAdapter;

  private ListAdapter mListAdapter;

  private SimpleAdapter<Uri> mSimpleAdapter;

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
    // Initialize the SimpleAdapter
    mSimpleAdapter = initializeSimpleAdapter(mConfig);
    // We use a different layout based on the type of output
    final View layout;
    switch (mConfig.recyclerLayoutType) {
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
    // Get RecyclerView
    mRecyclerView = UI.findViewById(layout, R.id.recycler_view);
    // Choose the LayoutManager
    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
    layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
    layoutManager.scrollToPosition(0);
    mRecyclerView.setLayoutManager(layoutManager);
    // Create the Adapter
    mDraweeViewAdapter = new DraweeViewAdapter(getContext(), mSimpleAdapter, mConfig);
    mRecyclerView.setAdapter(mDraweeViewAdapter);
  }

  private void initializeListView(final View layout) {
    // get the ListView
    mListView = UI.findViewById(layout, R.id.list_view);
    // Create the Adapter
    mListAdapter = new DraweeViewListAdapter(mSimpleAdapter, mConfig);
    // Set the adapter
    mListView.setAdapter(mListAdapter);
  }

  private SimpleAdapter<Uri> initializeSimpleAdapter(final Config config) {
    boolean distinctUriCompatible = true;
    SimpleAdapter<Uri> simpleAdapter = null;
    switch (config.dataSourceType) {
      case "local_resource_uris":
        simpleAdapter = LocalResourceSimpleAdapter
                .getEagerAdapter(getContext(), R.array.example_uris);
        break;
      case "local_internal_photo_uris":
        simpleAdapter = ContentProviderSimpleAdapter.getInternalPhotoSimpleAdapter(getContext());
        distinctUriCompatible = false;
        break;
      case "local_external_photo_uris":
        simpleAdapter = ContentProviderSimpleAdapter.getExternalPhotoSimpleAdapter(getContext());
        distinctUriCompatible = false;
        break;
    }
    if (config.infiniteDataSource) {
      simpleAdapter = SimpleAdapter.Util.makeItInfinite(simpleAdapter);
      if (distinctUriCompatible && config.distinctUriDataSource) {
        simpleAdapter = SimpleAdapter.Util
                .decorate(simpleAdapter, DistinctUriDecorator.SINGLETON);
      }
    }
    return simpleAdapter;
  }
}
