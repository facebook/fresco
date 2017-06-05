package com.meetyou.frescopainter;

import android.text.TextUtils;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.RoundingParams;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * 画室
 * Created by Linhh on 16/9/10.
 */
class PainterWorksapce implements PainterWork{

    /**
     * main paint method. don't change or edit code,
     * if you want to edit code or improve logic, contact linhonghong please.
     * 3Q
     * @param draweeView
     * @param pen
     */
    @Override
    public void paint(IFrescoImageView draweeView, FrescoPainterPen pen) {
        //设置监听
        setFrescoPainterInterceptor(draweeView,pen);
        draweeView.setFrescoPainterPen(pen);
        //设置圆形
        setRoundingParmas(draweeView, pen);
        //设置缩放
        setActualImageScaleType(draweeView, pen);
        //设置基本数据
        setImageHolder(draweeView, pen);
        //设置描边
        setBorder(draweeView, pen);
        try {
            if (pen.getSourceUri() != null) {
                paintNetwork(draweeView, pen);
            } else {
                paintLocal(draweeView, pen);
            }
        }catch (OutOfMemoryError error){
            error.printStackTrace();
        }
    }

    /**
     * 获得一个RoundingParams
     * @param draweeView
     * @return
     */
    private RoundingParams getRoundingParams(IFrescoImageView draweeView) {
        RoundingParams roundingParams = getHierarchy(draweeView).getRoundingParams();
        if(roundingParams == null){
            roundingParams = new RoundingParams();
        }
        return roundingParams;
    }

    /**
     * Hierarchy
     * @param draweeView
     * @return
     */
    private GenericDraweeHierarchy getHierarchy(IFrescoImageView draweeView){
        return draweeView.getHierarchy();
    }

    /**
     * 获得缓存的rounding参数
     * @param draweeView
     * @param roundingParams
     */
    private void setRoundingParams(IFrescoImageView draweeView, RoundingParams roundingParams){
        getHierarchy(draweeView).setRoundingParams(roundingParams);
    }

    /**
     * 设置图片缩放
     * @param draweeView
     * @param pen
     */
    public void setActualImageScaleType(IFrescoImageView draweeView, FrescoPainterPen pen) {
        //从XML读取的缩放
        ScalingUtils.ScaleType xmlScaling = getHierarchy(draweeView).getActualImageScaleType();
        getHierarchy(draweeView).setActualImageScaleType(pen.getScaleType() == null ? xmlScaling : pen.getScaleType());
    }

    /**
     * setBorder
     * @param draweeView
     * @param pen
     */
    private void setBorder(IFrescoImageView draweeView, FrescoPainterPen pen){
        RoundingParams roundingParams = getRoundingParams(draweeView);
        if(pen.getBorderColor() != 0) {
            roundingParams.setBorderColor(pen.getBorderColor());
        }

        if(pen.getBorderWidth() != 0){
            roundingParams.setBorderWidth(pen.getBorderWidth());
        }

        if(pen.getRoundePadding() != 0){
            roundingParams.setPadding(pen.getRoundePadding());
        }

        setRoundingParams(draweeView, roundingParams);
    }

    /**
     * 设置圆形或者圆角
     * @param draweeView
     */
    private void setRoundingParmas(IFrescoImageView draweeView, FrescoPainterPen pen){
        PenRadius penRadius = pen.getPenRadius();
        if(penRadius == null){
            return;
        }
        RoundingParams roundingParams = getRoundingParams(draweeView);
        if(pen.isCircle()){
            //如果是圆形
            roundingParams.setRoundAsCircle(true);
            if(pen.getOverColor() != 0){
                roundingParams.setRoundingMethod(RoundingParams.RoundingMethod.OVERLAY_COLOR).
                        setOverlayColor(pen.getOverColor());
            }
            setRoundingParams(draweeView, roundingParams);

        }else if(penRadius.getTopRightRadius() != 0 ||
                penRadius.getBottomRightRadius() != 0||
                penRadius.getTopLeftRadius() !=0 ||
                penRadius.getBottomLeftRadius() != 0){
            //设置圆角
            roundingParams.setCornersRadii(penRadius.getTopLeftRadius(),
                    penRadius.getTopRightRadius(),
                    penRadius.getBottomRightRadius(),
                    penRadius.getBottomLeftRadius());
            if(pen.getOverColor() != 0){
                roundingParams.setRoundingMethod(RoundingParams.RoundingMethod.OVERLAY_COLOR).
                        setOverlayColor(pen.getOverColor());
            }
            setRoundingParams(draweeView, roundingParams);
        }
    }

    /**
     * ResourceController
     * @param frescoImageView
     */
    private void setResourceController(IFrescoImageView frescoImageView){

        ImageRequest request = PainterFactory.buildImageRequestWithResource(frescoImageView.getFrescoPainterPen());
        frescoImageView.setImageRequest(request);

        DraweeController controller = PainterFactory.buildDraweeController(frescoImageView);
        frescoImageView.setController(controller);
    }

    /**
     * setSourceController
     * @param frescoImageView
     */
    private void setSourceController(IFrescoImageView frescoImageView){

        ImageRequest request = PainterFactory.buildImageRequestWithSource(frescoImageView.getFrescoPainterPen());
        frescoImageView.setImageRequest(request);

        ImageRequest low_request = PainterFactory.buildLowImageRequest(frescoImageView.getFrescoPainterPen());
        frescoImageView.setLowImageRequest(low_request);

        DraweeController controller = PainterFactory.buildDraweeController(frescoImageView);
        frescoImageView.setController(controller);
    }

  /**
   * 设置监听器
   * @param draweeView
   * @param pen
   */
  private void setFrescoPainterInterceptor(IFrescoImageView draweeView, FrescoPainterPen pen){
        getHierarchy(draweeView).setFrescoPainterDraweeInterceptor(pen.getFrescoPainterDraweeInterceptor());
    }

    /**
     * setImageHolder
     * @param draweeView
     * @param pen
     */
    private void setImageHolder(IFrescoImageView draweeView, FrescoPainterPen pen){
        int defaultResID = pen.getDefaultHolder();
        ScalingUtils.ScaleType defaultScaleType = pen.getDefaultHolderScaleType();
        ScalingUtils.ScaleType failScaleType = pen.getFailHolderScaleType();
        ScalingUtils.ScaleType progressScaleType = pen.getProgressHolderScaleType();
        ScalingUtils.ScaleType retryScaleType = pen.getRetryHolderScaleType();
        int failResID = pen.getFailHolder();
        int retryResID = pen.getRetryHolder();
        int bgResID = pen.getBgHolder();
        int progressResID = pen.getProgressHolder();

        if(defaultResID > 0) {
            if(defaultScaleType == null) {
                getHierarchy(draweeView).setPlaceholderImage(defaultResID);
            }else{
                getHierarchy(draweeView).setPlaceholderImage(defaultResID, defaultScaleType);
            }
        }
        if(failResID > 0) {
            if(failScaleType == null) {
                getHierarchy(draweeView).setFailureImage(failResID);
            }else{
                getHierarchy(draweeView).setFailureImage(failResID, failScaleType);
            }
        }
        if(retryResID > 0){
            if(retryScaleType == null) {
                getHierarchy(draweeView).setRetryImage(retryResID);
            }else{
                getHierarchy(draweeView).setRetryImage(retryResID, retryScaleType);
            }
        }
        if(bgResID > 0){
            getHierarchy(draweeView).setBackgroundImage(bgResID);
        }
        if(progressResID > 0){
            if(progressScaleType == null) {
                getHierarchy(draweeView).setProgressBarImage(progressResID);
            }else{
                getHierarchy(draweeView).setProgressBarImage(progressResID, progressScaleType);
            }
        }
        getHierarchy(draweeView).setFadeDuration(pen.getFadeDuration());


    }

    /**
     * 绘制网络图片，以及磁盘
     * @param draweeView
     * @param pen
     */
    public void paintNetwork(IFrescoImageView draweeView, FrescoPainterPen pen){
        String url = pen.getSourceUri();
        if(TextUtils.isEmpty(url)){
            //如果url为空就显示默认图
            setResourceController(draweeView);
            return;
        }
        setSourceController(draweeView);
    }

    /**
     * 加载本地，res
     * @param draweeView
     * @param pen
     */
    public void paintLocal(IFrescoImageView draweeView, FrescoPainterPen pen){
        setResourceController(draweeView);
    }
}
