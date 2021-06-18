package com.example.myapplicationdfsd.activity;

import com.app.abby.xbanner.XBanner;
import com.example.myapplicationdfsd.R;

public class BannerShower {
    private XBanner xbanner;

    public void prepare(XBanner xbanner, Integer delay) {
        int[] images = {R.drawable.advertisement1,
                R.drawable.advertisement2,
                R.drawable.advertisement3,
                R.drawable.advertisement4};

        // 轮播图
        this.xbanner = xbanner;
        xbanner.isAutoPlay(true)
                .setBannerTypes(XBanner.CIRCLE_INDICATOR)
                .setUpIndicatorSize(0, 0)
                .setTitlebgAlpha()
                .setDelay(delay)
                .setImageRes(images);
    }

    public void start() {
        xbanner.start();
    }

}
