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

import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeControllerBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider.ImageSize;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import java.util.List;
import java.util.Random;

/**
 * Simple drawee recycler view fragment that displays a grid of images.
 */
public class DraweeRecyclerViewFragment extends BaseShowcaseFragment {

  /** How many images to display */
  private static final int NUM_ENTRIES = 256;

  /**
   * Number of recycler view spans
   */
  private static final int SPAN_COUNT = 3;

  private @Nullable ResizeOptions mResizeOptions;

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_drawee_recycler, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    final RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
    recyclerView.addOnLayoutChangeListener(
        new View.OnLayoutChangeListener() {
          @Override
          public void onLayoutChange(
              View view,
              int left,
              int top,
              int right,
              int bottom,
              int oldLeft,
              int oldTop,
              int oldRight,
              int oldBottom) {
            final int imageSize = (right - left) / SPAN_COUNT;
            mResizeOptions = new ResizeOptions(imageSize, imageSize);
          }
        });

    GridLayoutManager layoutManager = new GridLayoutManager(getContext(), SPAN_COUNT);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setHasFixedSize(true);

    final ImageUriProvider uriProvider = ImageUriProvider.getInstance(getContext());
    final List<Uri> smallDummyData = uriProvider.getRandomSampleUris(ImageSize.S, NUM_ENTRIES);
    final List<Uri> bigDummyData = uriProvider.getRandomSampleUris(ImageSize.M, NUM_ENTRIES);
    final SimpleAdapter adapter = new SimpleAdapter(smallDummyData);
    recyclerView.setAdapter(adapter);

    final Switch bigImagesSwitch = view.findViewById(R.id.switch_big_images);
    bigImagesSwitch.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton compoundButton, boolean enabled) {
            adapter.setData(enabled ? bigDummyData : smallDummyData);
          }
        });
  }

  @Override
  public int getTitleId() {
    return R.string.drawee_recycler_title;
  }

  public class SimpleAdapter extends RecyclerView.Adapter<SimpleViewHolder> {

    private final PipelineDraweeControllerBuilder mControllerBuilder =
        Fresco.newDraweeControllerBuilder();
    private List<Uri> mUris;

    SimpleAdapter(List<Uri> uris) {
      mUris = uris;
      setHasStableIds(true);
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
      final ImageRequest imageRequest =
          ImageRequestBuilder.newBuilderWithSource(mUris.get(position))
              .setResizeOptions(mResizeOptions)
              .build();
      holder.mSimpleDraweeView.setImageRequest(imageRequest);
    }

    @Override
    public int getItemCount() {
      return mUris.size();
    }

    @Override
    public long getItemId(int position) {
      return mUris.get(position).hashCode();
    }

    public void setData(List<Uri> uris) {
      mUris = uris;
      notifyDataSetChanged();
    }
  }

  static class SimpleViewHolder extends RecyclerView.ViewHolder {

    private static final Random sRandom = new Random();

    private final SimpleDraweeView mSimpleDraweeView;

    SimpleViewHolder(View itemView) {
      super(itemView);
      mSimpleDraweeView = itemView.findViewById(R.id.drawee_view);
      mSimpleDraweeView.getHierarchy().setPlaceholderImage(new ColorDrawable(sRandom.nextInt()));
    }
  }
}
