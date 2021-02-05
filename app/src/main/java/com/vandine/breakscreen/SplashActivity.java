package com.vandine.breakscreen;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.util.Log;

import com.seagazer.animlogoview.AnimLogoView;

public class SplashActivity extends AppCompatActivity {
    private AnimSplashView animLogoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        initView();
        initListener();

        animLogoView.startAnimation();
    }

    private void initView(){
        animLogoView = findViewById(R.id.anim);
    }

    private void initListener(){
        animLogoView.addOffsetAnimListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                Log.d("MainActivity", "Offset anim end");
            }
        });
        animLogoView.addGradientAnimListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                Log.d("MainActivity", "Gradient anim end");
            }
        });
    }
}