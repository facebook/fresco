package com.meetyou.frescopainter;

import android.content.Context;
import android.util.AttributeSet;

import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.RichDrawable;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Created by Linhh on 16/9/10.
 */
public class FrescoImageView extends SimpleDraweeView implements IFrescoImageView{

    private FrescoPainterPen mPen;
    private ImageRequest mImageRequest;
    private ImageRequest mLowImageRequest;
    private ControllerListener mControllerListener;

    public FrescoImageView(Context context, GenericDraweeHierarchy hierarchy) {
        super(context, hierarchy);
    }

    public FrescoImageView(Context context) {
        super(context);
    }

    public FrescoImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FrescoImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public FrescoImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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
