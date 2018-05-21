package com.facebook.fresco.samples.showcase.imageformat.webplist.adapter;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

/**
 * The Adapter for the webp list;
 * In this example, only one webp is playing. For privacy reasons, the webp list I use cannot
 * be uploaded. You can add more webp items to show the problem of Stuck.
 */
public class WebpAdapter extends RecyclerView.Adapter<WebpAdapter.WebpViewHolder> {

  private static final int MAX_SIZE = 10000;

  private volatile boolean mEnableDropFrame = false;

  @NonNull
  @Override
  public WebpViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    return new WebpViewHolder(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_rcy_webp, parent, false),
            mEnableDropFrame);
  }

  @Override
  public void onBindViewHolder(@NonNull WebpViewHolder holder, int position) {
    holder.bind(position);
  }

  @Override
  public int getItemCount() {
    return MAX_SIZE;
  }

  public void setEnableDropFrame(boolean enableDropped) {
    mEnableDropFrame = enableDropped;
  }

  static class WebpViewHolder extends RecyclerView.ViewHolder {

    private final ImageUriProvider imageUriProvider;

    private SimpleDraweeView mWebpIv;
    private TextView mTitleTv;
    private String mShowText;
    private boolean mEnableDrop;

    WebpViewHolder(View itemView, boolean enableDrop) {
      super(itemView);
      mTitleTv = itemView.findViewById(R.id.tv_webp_title);
      mWebpIv = itemView.findViewById(R.id.iv_webp);
      imageUriProvider = ImageUriProvider.getInstance(itemView.getContext());
      mEnableDrop = enableDrop;
    }

    void bind(int position) {
      mShowText = "Pos_" + position;
      mTitleTv.setText(mShowText);

      Uri uri = imageUriProvider.createWebpAnimatedUri();
      DraweeController controller = Fresco.newDraweeControllerBuilder()
              .setImageRequest(
                    ImageRequestBuilder.newBuilderWithSource(uri)
                        .setImageDecodeOptions(
                              ImageDecodeOptions.newBuilder()
                              .setEnableDropFrame(mEnableDrop)
                              .build())
                    .build())
              .setOldController(mWebpIv.getController())
              .setAutoPlayAnimations(true)
              .build();
      mWebpIv.setController(controller);
    }
  }
}
