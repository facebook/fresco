package com.facebook.samples.demo;

import android.app.Activity;
import android.os.Bundle;

import com.facebook.drawee.drawable.ScalingUtils;
import com.meetyou.frescopainter.FrescoImageView;
import com.meetyou.frescopainter.FrescoPainter;
import com.meetyou.frescopainter.FrescoPainterPen;

/**
 * Created by Linhh on 16/9/10.
 */
public class PainterActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_painter);
        final FrescoImageView frescoImageView = (FrescoImageView)findViewById(R.id.frescoimage);
//      final FrescoZoomView frescoImageView2 = (FrescoZoomView)findViewById(R.id.frescoimage2);
//      frescoImageView.setOnLongDraweeClickListener(new View.OnLongClickListener() {
//        @Override
//        public boolean onLongClick(View v) {
//          Toast.makeText(PainterActivity.this,"onLongClick",Toast.LENGTH_SHORT).show();
//          return false;
//        }
//      });
////      TextView textView = new TextView(this);
////      textView.setText("ssssssss");
//      RichDrawable gifDrawable = new RichDrawable(getDrawable(R.mipmap.apk_gif));
//      gifDrawable.setPosition(RichDrawable.POS_CENTER);
//      gifDrawable.setMargin(10,10,10,10);
//      frescoImageView.setRichDrawable(gifDrawable);
//      frescoImageView2.setRichDrawable(gifDrawable);
//      FrescoPainterPen pen = FrescoPainterPen.newBuilderWithURL("http://sc.seeyouyima.com/avatar_52319959?imageView/1/w/120/h/120/q/100/&rx=34153&imageView2/1/w/80/h/80/format/webp")
//          .setFailHolder(R.mipmap.ic_launcher).setFrescoPainterDraweeInterceptor(new FrescoPainterDraweeInterceptor() {
//            @Override
//            public Drawable onSetPlaceholderImage(int resourceId) {
//              return null;
//            }
//
//            @Override
//            public Drawable onSetRetryImage(int resourceId) {
//              return null;
//            }
//
//            @Override
//            public Drawable onSetFailureImage(int resourceId) {
//              return null;
//            }
//
//            @Override
//            public Drawable onSetProgressBarImage(int resourceId) {
//              return null;
//            }
//
//            @Override
//            public Drawable onSetBackground(int resourceId) {
//              return null;
//            }
//          }).setScaleType(ScalingUtils.ScaleType.FIT_XY);
//      FrescoPainter.workspace().paint(frescoImageView2, pen);
//      frescoImageView.setOnDraweeClickListener(new View.OnClickListener() {
//        @Override
//        public void onClick(View v) {
//          Toast.makeText(PainterActivity.this,"onClick",Toast.LENGTH_SHORT).show();
////          FrescoPainter.workspace().removeFromDiskCache("http://img1.jiemian.com/101/original/20160926/147486461127815600");
//        }
//      });
//        FrescoPainter.workspace().loadImageCallBackUi(
//            "http://sc.seeyouyima.com/advertise/20160726/5796d7899b2fa_147_196.gif",
//            new PainterCallBack() {
//                @Override
//                public void onSuccess(String url, Bitmap bitmap) {
//                    FrescoPainterPen pen = FrescoPainterPen.newBuilderWithURL("http://sc.seeyouyima.com/advertise/20160726/5796d7899b2fa_147_196.gif")
//                        .setScaleType(ScalingUtils.ScaleType.FIT_XY);
//                    FrescoPainter.workspace().paint(frescoImageView, pen);
//                }
//
//                @Override
//                public void onFailure(String url, Throwable throwable) {
//
//                }
//            });

        FrescoPainterPen pen = FrescoPainterPen.
                newBuilderWithURL("http://cdn.seeyouyima.com/taobao/web_brand_area_notice58febef3cf4f5_500_500.png")
            .setScaleType(ScalingUtils.ScaleType.CENTER)
                ;
//        FrescoPainterPen pen = FrescoPainterPen.
//            newBuilderWithAsset("/gif_test.gif")
////            .setRadius(60)
////            .setHeight(30)
////            .setWidth(30)
//            .setAnim(true)
//            .setScaleType(ScalingUtils.ScaleType.FIT_XY)
//            .setDefaultHolder(R.drawable.apk_music_off_line)
//            .setFailHolder(R.drawable.apk_music_off_line);
        FrescoPainter.workspace().paint(frescoImageView,pen);
//        .setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        ImagePipeline imagePipeline = Fresco.getImagePipeline();
//                        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(
//                                Uri.parse("http://pic.duowan.com/newgame/1109/179919961792/179920848656.jpg"))
//                                .setSync(true)
//                                .build();
//                        try {
//                            final Bitmap bitmap = imagePipeline.fetchBitmap(request, this);
//                            new Handler(Looper.getMainLooper()).post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    ((ImageView)findViewById(R.id.ivtest)).setImageBitmap(bitmap);
//                                }
//                            });
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }).start();
//            }
//        });


    }

}

