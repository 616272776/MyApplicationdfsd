package com.example.myapplicationdfsd.software.activity;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplicationdfsd.R;

import org.webrtc.EglBase;
import org.webrtc.SurfaceViewRenderer;

public class PoliceAffairsActivity extends AppCompatActivity {

    SurfaceViewRenderer[] remoteViews;
    private SurfaceViewRenderer localView;
    EglBase.Context eglBaseContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_police_affairs);

        remoteViews = new SurfaceViewRenderer[]{
                findViewById(R.id.remoteView),
                findViewById(R.id.remoteView2),
                findViewById(R.id.remoteView3),
        };
        for (SurfaceViewRenderer remoteView : remoteViews) {
            remoteView.setMirror(false);
            remoteView.init(eglBaseContext, null);
        }

        localView = findViewById(R.id.localView);
        localView.setMirror(true);
        localView.init(eglBaseContext, null);



    }
}
