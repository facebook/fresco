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
import java.util.ArrayList;
import java.util.List;

/**
 * Simple drawee recycler view fragment that displays a grid of images.
 */
public class DraweeRecyclerViewFragment extends BaseShowcaseFragment {

  /**
   * lorempixel.com image categories.
   */
  private static final String[] CATEGORIES = {
     "animals",
     "sports",
     "nature",
     "city",
     "food",
     "people",
     "nightlife",
     "fashion",
     "transport",
     "cats",
     "business",
     "technics",
  };

  /**
   * How many images per each category.
   */
  private static final int NUM_ENTRIES_PER_CATEGORY = 10;

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
    for (int i = 0; i < NUM_ENTRIES_PER_CATEGORY; i++) {
      for (int j = 0; j < CATEGORIES.length; j++) {
        data.add(Uri.parse(String.format(
            "http://lorempixel.com/400/400/%s/%d", CATEGORIES[j], i + 1)));
      }
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
