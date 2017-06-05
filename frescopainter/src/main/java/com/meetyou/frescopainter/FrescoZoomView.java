package com.meetyou.frescopainter;

import android.content.Context;
import android.util.AttributeSet;

import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.RichDrawable;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.imagepipeline.request.ImageRequest;
import com.meetyou.frescopainter.zoom.zoomable.ZoomableDraweeView;

/**
 * Created by Linhh on 16/9/10.
 */
public class FrescoZoomView extends ZoomableDraweeView implements IFrescoImageView{
    private FrescoPainterPen mPen;
    private ImageRequest mImageRequest;
    private ImageRequest mLowImageRequest;
    private ControllerListener mControllerListener;

    public FrescoZoomView(Context context) {
        super(context);
        init();
    }

    public FrescoZoomView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FrescoZoomView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public void init(){
        GenericDraweeHierarchy hierarchy = new GenericDraweeHierarchyBuilder(getResources())
            .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
//                .setProgressBarImage(new ProgressBarDrawable())
            .build();

//        view.setController(ctrl);
        this.setHierarchy(hierarchy);
    }


    @Override
    public ImageRequest getImageRequest() {
        return mImageRequest;
    }

    @Override
    public void setImageRequest(ImageRequest imageRequest) {
        mImageRequest = imageRequest;
    }

    @Override
    public ImageRequest getLowImageRequest() {
        return mLowImageRequest;
    }

    @Override
    public void setLowImageRequest(ImageRequest lowImageRequest) {
        mLowImageRequest = lowImageRequest;
    }

    @Override
    public void setFrescoPainterPen(FrescoPainterPen painterPen){
        mPen = painterPen;
    }

    @Override
    public FrescoPainterPen getFrescoPainterPen(){
        return mPen;
    }

    @Override
    public void setControllerListener(ControllerListener controllerListener) {
        mControllerListener = controllerListener;
    }

    @Override
    public ControllerListener getControllerListener() {
        return mControllerListener;
    }

    @Override
    public void setRichDrawable(RichDrawable hongHongDrawable) {
        getHierarchy().setHongHongDrawable(hongHongDrawable);
    }

    @Override
    public RichDrawable getRichDrawable() {
        return getHierarchy().getHongHongDrawable();
    }
}
