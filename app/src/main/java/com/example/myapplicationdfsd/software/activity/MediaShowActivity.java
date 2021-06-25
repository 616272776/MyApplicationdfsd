package com.example.myapplicationdfsd.software.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.abby.xbanner.XBanner;
import com.example.myapplicationdfsd.R;

public class MediaShowActivity extends AppCompatActivity {

    // ui控件
    private BannerShower mBannerController;
    private XBanner mXBanner;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_show);

        mXBanner = findViewById(R.id.banner);

        // 轮播图
        mBannerController = new BannerShower();
        mBannerController.prepare(mXBanner, 3000);
        mBannerController.start();
    }
}
