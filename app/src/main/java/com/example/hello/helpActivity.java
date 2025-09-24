package com.example.hello;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class helpActivity extends BaseActivity {

    private RecyclerView faqRecyclerView;
    private FAQAdapter faqAdapter;
    private List<FAQItem> faqList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_help);

        faqRecyclerView = findViewById(R.id.faqRecyclerView);
        faqRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Add FAQ data
        faqList = new ArrayList<>();
        faqList.add(new FAQItem("How do I join a queue?",
                "After logging in, select a service you would like to visit and click done."));
        faqList.add(new FAQItem("How do I know my queue position?",
                "Your position is shown as your Username initial letter or Profile Picture."));
        faqList.add(new FAQItem("What happens if I leave a queue?",
                "You will lose your current position and need to rejoin at the end if needed."));
        faqList.add(new FAQItem("Does the app require an internet connection?",
                "Yes, as it updates queue positions in real-time."));

        faqAdapter = new FAQAdapter(faqList);
        faqRecyclerView.setAdapter(faqAdapter);
    }
}
