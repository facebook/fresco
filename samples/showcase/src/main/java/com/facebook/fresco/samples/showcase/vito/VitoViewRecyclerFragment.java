/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.vito;

import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageSourceSpinner;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.view.VitoView;
import com.facebook.imagepipeline.common.ResizeOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

/** Simple Vito recycler view fragment that displays a grid of images. */
public class VitoViewRecyclerFragment extends BaseShowcaseFragment {

  /** Number of recycler view spans */
  private static final int SPAN_COUNT = 3;

  private @Nullable ResizeOptions mResizeOptions;
  private ImageOptions.Builder mOptionsBuilder = ImageOptions.create().fadeDurationMs(500);

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
            mOptionsBuilder = mOptionsBuilder.resize(mResizeOptions);
          }
        });

    GridLayoutManager layoutManager = new GridLayoutManager(getContext(), SPAN_COUNT);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.setHasFixedSize(true);

    final SimpleAdapter adapter = new SimpleAdapter(new ArrayList<Uri>());
    recyclerView.setAdapter(adapter);

    final Spinner imageSource = view.findViewById(R.id.spinner_image_source);
    ImageSourceSpinner.INSTANCE.setup(
        imageSource,
        sampleUris(),
        new Function1<List<Uri>, Unit>() {
          @Override
          public Unit invoke(List<Uri> uris) {
            adapter.setData(uris);
            return null;
          }
        });
  }

  @Override
  public int getTitleId() {
    return R.string.vito_view_view_recycler;
  }

  public class SimpleAdapter extends RecyclerView.Adapter<SimpleViewHolder> {

    private final Random mRandom = new Random();

    private List<Uri> mUris;

    SimpleAdapter(List<Uri> uris) {
      mUris = uris;
      setHasStableIds(true);
    }

    @Override
    public SimpleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View itemView =
          LayoutInflater.from(parent.getContext())
              .inflate(R.layout.vito_recycler_item, parent, false);
      return new SimpleViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(SimpleViewHolder holder, int position) {
      final ImageOptions imageOptions =
          mOptionsBuilder.placeholder(new ColorDrawable(mRandom.nextInt())).build();
      VitoView.show(mUris.get(position), imageOptions, holder.mView);
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

    private final View mView;

    SimpleViewHolder(View itemView) {
      super(itemView);
      mView = itemView.findViewById(R.id.view);
    }
  }
}
