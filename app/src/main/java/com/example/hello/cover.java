package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;
import java.util.Timer;
import java.util.TimerTask;

public class cover extends AppCompatActivity {

    private ViewPager2 viewPager;
    private DotsIndicator dotsIndicator;
    private ImageAdapter imageAdapter;
    private int[] imageResources = {R.drawable.f, R.drawable.mannn, R.drawable.cartoongirl};
    private Timer timer;
    private int currentPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Switch to the main theme before calling super.onCreate()
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_cover);

        // Initialize ViewPager2 and DotsIndicator
        viewPager = findViewById(R.id.viewPager);
        dotsIndicator = findViewById(R.id.dotsIndicator);

        // Set up the adapter
        imageAdapter = new ImageAdapter(imageResources);
        viewPager.setAdapter(imageAdapter);

        // Connect DotsIndicator to ViewPager2
        dotsIndicator.setViewPager2(viewPager);

        // Auto-scroll images every 2 seconds
        startAutoScroll();

        // Handle "Get started" button click
        TextView textViewB = findViewById(R.id.textViewB);
        textViewB.setOnClickListener(v -> {
            Intent intent = new Intent(cover.this, MainActivity.class);
            startActivity(intent);
            finish(); // Close the cover activity
        });
    }

    private void startAutoScroll() {
        final Handler handler = new Handler();
        final Runnable update = new Runnable() {
            public void run() {
                if (currentPage == imageResources.length) {
                    currentPage = 0;
                }
                viewPager.setCurrentItem(currentPage++, true);
            }
        };

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(update);
            }
        }, 2000, 2000); // Delay 2 seconds, repeat every 2 seconds
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel(); // Stop the timer when the activity is destroyed
        }
    }
}