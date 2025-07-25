package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        LottieAnimationView lottieAnimation = findViewById(R.id.lottieAnimation);
        lottieAnimation.setAnimation(R.raw.splashh);
        lottieAnimation.playAnimation();

        // Delay for 3 seconds then go to MainActivity
        new Handler().postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, cover.class));
            finish();
        }, 3000);
    }
}
