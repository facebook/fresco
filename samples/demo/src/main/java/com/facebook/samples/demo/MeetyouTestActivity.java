package com.facebook.samples.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.meetyou.frescopainter.FrescoPainter;
import com.meetyou.frescopainter.PainterCallBack;

/**
 * Created by Linhh on 16/9/10.
 */
public class MeetyouTestActivity  extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

      findViewById(R.id.ivtest).setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
          FrescoPainter.workspace().evictFromPainterCache("http://sc.seeyouyima.com/advertise/20160726/5796d7899b2fa_147_196.gif");
        }
      });

        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              FrescoPainter.workspace().loadImageCallBackUi(
                    "http://sc.seeyouyima.com/advertise/20160726/5796d7899b2fa_147_196.gif",
                    new PainterCallBack() {
                      @Override
                      public void onSuccess(String url, Bitmap bitmap) {
                        ((ImageView)findViewById(R.id.ivtest)).setImageBitmap(bitmap);

                      }

                      @Override
                      public void onFailure(String url, Throwable throwable) {

                      }
                    });
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
////                        FrescoPainterPen pen = FrescoPainterPen.newBuilderWithURL("http://img30.360buyimg.com/jgsq-productsoa/jfs/t3295/3/1848509641/48600/5a858445/57d61644N67602289.jpg")
////                                .asCircle(true)
////                                .setRadius(new PenRadius(1,20,30,40))
////                                .setBorderWidth(5);
//                        final Bitmap bitmap = FrescoPainter.workspace().loadBitmapSync(R.mipmap.ic_launcher,new ResizeOptions(20,20));
//                        new Handler(Looper.getMainLooper()).post(new Runnable() {
//                            @Override
//                            public void run() {
//                                ((ImageView)findViewById(R.id.ivtest)).setImageBitmap(bitmap);
//                            }
//                        });
//                    }
//                }).start();
            }
        });


    }

}
