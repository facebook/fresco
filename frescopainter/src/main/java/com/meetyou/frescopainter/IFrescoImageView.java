package com.meetyou.frescopainter;

import android.content.Context;

import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.drawable.RichDrawable;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Created by Linhh on 16/9/10.
 */
public interface IFrescoImageView {

    public Context getContext();
    /**
     * 获得当前使用的ImageRequest
     * @return
     */
    public ImageRequest getImageRequest();

    public void setImageRequest(ImageRequest imageRequest);

    public ImageRequest getLowImageRequest();

    public void setLowImageRequest(ImageRequest lowImageRequest);

    public void setFrescoPainterPen(FrescoPainterPen painterPen);

    public FrescoPainterPen getFrescoPainterPen();

    public void setControllerListener(ControllerListener controllerListener);

    public ControllerListener getControllerListener();

    public GenericDraweeHierarchy getHierarchy();

    public void setController(DraweeController controller);

    public DraweeController getController();

    public void setRichDrawable(RichDrawable hongHongDrawable);

    public RichDrawable getRichDrawable();

}
