/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.imagepipeline;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.backends.pipeline.info.ImageOriginListener;
import com.facebook.drawee.backends.pipeline.info.ImageOriginUtils;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.fresco.samples.showcase.BaseShowcaseFragment;
import com.facebook.fresco.samples.showcase.R;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider.ImageSize;
import com.facebook.fresco.samples.showcase.misc.ImageUriProvider.Orientation;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.Locale;

/**
 * Fragment that illustrates how to prefetch images to disk cache so that they load faster when
 * finally displayed.
 */
public class ImagePipelinePrefetchFragment extends BaseShowcaseFragment {

  private Uri[] mUris;

  private Button mPrefetchDiskButton;
  private Button mPrefetchEncodedButton;
  private Button mPrefetchBitmapButton;
  private TextView mPrefetchStatus;
  private ViewGroup mDraweesHolder;
  private final Handler mHandler = new Handler();

  private final ImageOriginListener mImageOriginListener =
      new ImageOriginListener() {
        @Override
        public void onImageLoaded(
            final String controllerId,
            final @ImageOrigin int imageOrigin,
            final boolean successful,
            final @Nullable String ultimateProducerName) {
          mHandler.post(
              new Runnable() {
                @Override
                public void run() {
                  Toast.makeText(
                          getContext(),
                          String.format(
                              (Locale) null,
                              "Image loaded: controllerId=%s, origin=%s, successful=%b",
                              controllerId,
                              ImageOriginUtils.toString(imageOrigin),
                              successful),
                          Toast.LENGTH_SHORT)
                      .show();
                }
              });
        }
      };

  @Override
  public int getTitleId() {
    return R.string.imagepipeline_prefetch_title;
  }

  private class PrefetchSubscriber extends BaseDataSubscriber<Void> {

    private int mSuccessful = 0;
    private int mFailed = 0;

    @Override
    protected void onNewResultImpl(DataSource<Void> dataSource) {
      mSuccessful++;
      updateDisplay();
    }

    @Override
    protected void onFailureImpl(DataSource<Void> dataSource) {
      mFailed++;
      updateDisplay();
    }

    private void updateDisplay() {
      if (mSuccessful + mFailed == mUris.length) {
        mPrefetchDiskButton.setEnabled(true);
        mPrefetchEncodedButton.setEnabled(true);
        mPrefetchBitmapButton.setEnabled(true);
      }
      mPrefetchStatus.setText(
          getString(R.string.prefetch_status, mSuccessful, mUris.length, mFailed));
    }
  }

  public ImagePipelinePrefetchFragment() {
    // Required empty public constructor
  }

  @Override
  public @Nullable View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_imagepipeline_prefetch, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    mUris =
        new Uri[] {
          sampleUris().createSampleUri(ImageSize.L, Orientation.LANDSCAPE),
          sampleUris().createSampleUri(ImageSize.L, Orientation.PORTRAIT),
        };

    final Button clearCacheButton = (Button) view.findViewById(R.id.clear_cache);
    clearCacheButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            for (Uri uri : mUris) {
              Fresco.getImagePipeline().evictFromCache(uri);
            }
          }
        });

    mPrefetchStatus = (TextView) view.findViewById(R.id.prefetch_status);
    mPrefetchDiskButton = (Button) view.findViewById(R.id.prefetch_disk_now);
    mPrefetchDiskButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            mPrefetchDiskButton.setEnabled(false);
            final PrefetchSubscriber subscriber = new PrefetchSubscriber();
            for (Uri uri : mUris) {
              final DataSource<Void> ds =
                  Fresco.getImagePipeline().prefetchToDiskCache(ImageRequest.fromUri(uri), null);
              ds.subscribe(subscriber, UiThreadImmediateExecutorService.getInstance());
            }
          }
        });

    mPrefetchEncodedButton = (Button) view.findViewById(R.id.prefetch_encoded_now);
    mPrefetchEncodedButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            mPrefetchEncodedButton.setEnabled(false);
            final PrefetchSubscriber subscriber = new PrefetchSubscriber();
            for (Uri uri : mUris) {
              final DataSource<Void> ds =
                  Fresco.getImagePipeline().prefetchToEncodedCache(ImageRequest.fromUri(uri), null);
              ds.subscribe(subscriber, UiThreadImmediateExecutorService.getInstance());
            }
          }
        });

    mPrefetchBitmapButton = (Button) view.findViewById(R.id.prefetch_bitmap_now);
    mPrefetchBitmapButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            mPrefetchBitmapButton.setEnabled(false);
            final PrefetchSubscriber subscriber = new PrefetchSubscriber();
            for (Uri uri : mUris) {
              final DataSource<Void> ds =
                  Fresco.getImagePipeline().prefetchToBitmapCache(ImageRequest.fromUri(uri), null);
              ds.subscribe(subscriber, UiThreadImmediateExecutorService.getInstance());
            }
          }
        });

    mDraweesHolder = (ViewGroup) view.findViewById(R.id.drawees);
    Button toggleImages = (Button) view.findViewById(R.id.toggle_images);
    toggleImages.setOnClickListener(
        new View.OnClickListener() {
          private boolean mShowing = false;

          @Override
          public void onClick(View v) {
            if (!mShowing) {
              for (int i = 0; i < mDraweesHolder.getChildCount(); i++) {
                SimpleDraweeView draweeView = (SimpleDraweeView) mDraweesHolder.getChildAt(i);
                draweeView.setController(
                    Fresco.newDraweeControllerBuilder()
                        .setOldController(draweeView.getController())
                        .setImageOriginListener(mImageOriginListener)
                        .setUri(mUris[i])
                        .build());
              }
            } else {
              for (int i = 0; i < mDraweesHolder.getChildCount(); i++) {
                ((SimpleDraweeView) mDraweesHolder.getChildAt(i)).setController(null);
              }
            }
            mShowing = !mShowing;
          }
        });
  }
}
