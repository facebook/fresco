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
package com.facebook.fresco.samples.showcase.drawee;

import java.util.ArrayList;
import java.util.List;

import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;

/**
 * Simple drawee recycler view fragment that displays a grid of images.
 */
public class DraweeRecyclerViewFragment extends BaseShowcaseFragment {

  /**
   * Total number of images displayed
   */
  private static final int TOTAL_NUM_ENTRIES = 200;

  /**
   * URIs that will be repeated
   */
  private static final Uri[] URIS = {
      Uri.parse("http://frescolib.org/static/sample-images/animal_a_s.jpg"),
      Uri.parse("http://frescolib.org/static/sample-images/animal_b_s.jpg"),
      Uri.parse("http://frescolib.org/static/sample-images/animal_c_s.jpg"),
      Uri.parse("http://frescolib.org/static/sample-images/animal_d_s.jpg"),
      Uri.parse("http://frescolib.org/static/sample-images/animal_e_s.jpg"),
      Uri.parse("http://frescolib.org/static/sample-images/animal_f_s.jpg"),
      Uri.parse("http://frescolib.org/static/sample-images/animal_g_s.jpg"),
  };

  /**
   * Number of recycler view spans
   */
  private static final int SPAN_COUNT = 3;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_drawee_recycler, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
    GridLayoutManager layoutManager = new GridLayoutManager(getContext(), SPAN_COUNT);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setAdapter(new SimpleAdapter(createDummyData()));
  }

  @Override
  public int getTitleId() {
    return R.string.drawee_recycler_title;
  }

  private List<Uri> createDummyData() {
    List<Uri> data = new ArrayList<>();
    for (int i = 0; i < TOTAL_NUM_ENTRIES; i++) {
      // We add the URI and append a query parameter (?cache_busting=123) so that each image
      // will be treated as a separate network image.
      data.add(
          URIS[i % URIS.length]
              .buildUpon()
              .appendQueryParameter("cache_busting", Integer.toString(i))
              .build());
    }
    return data;
  }

  public static class SimpleAdapter extends RecyclerView.Adapter<SimpleViewHolder> {

    private final List<Uri> mUris;

    public SimpleAdapter(List<Uri> uris) {
      mUris = uris;
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(
        ViewGroup parent,
        int viewType) {
      View itemView = LayoutInflater.from(
          parent.getContext()).inflate(R.layout.drawee_recycler_item, parent, false);
      return new SimpleViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {
      holder.mSimpleDraweeView.setImageURI(mUris.get(position));
    }

    @Override
    public int getItemCount() {
      return mUris.size();
    }
  }

  public static class SimpleViewHolder extends RecyclerView.ViewHolder {

    private final SimpleDraweeView mSimpleDraweeView;

    public SimpleViewHolder(View itemView) {
      super(itemView);
      mSimpleDraweeView = (SimpleDraweeView) itemView.findViewById(R.id.drawee_view);
    }
  }
}
