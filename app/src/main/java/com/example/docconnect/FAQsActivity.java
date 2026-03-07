package com.example.docconnect;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FAQsActivity extends AppCompatActivity {

    private RecyclerView rvFaq;
    private FaqAdapter adapter;
    private List<FaqModel> faqList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_faqs);

        rvFaq = findViewById(R.id.rvFaq);
        findViewById(R.id.btnback).setOnClickListener(v -> finish());

        setupList();
    }

    private void setupList() {
        faqList = new ArrayList<>();
        faqList.add(new FaqModel("How do I book a new appointment?", "Search for your doctor, choose a consultation type, select an available time slot, and confirm."));
        faqList.add(new FaqModel("Can I cancel my consultation?", "Yes, cancellations are allowed up to 24 hours before the appointment through the 'Appointments' tab."));
        faqList.add(new FaqModel("How to reach my doctor?", "Once booked, you can use the built-in chat feature or visit the clinic address provided in your receipt."));
        faqList.add(new FaqModel("Is my medical data secure?", "Absolutely. DocConnect uses end-to-end encryption for all health records and chat messages."));
        faqList.add(new FaqModel("Payment and refund policy?", "Payments are processed securely. Refunds for cancelled appointments take 3-5 business days."));

        adapter = new FaqAdapter(faqList);
        rvFaq.setLayoutManager(new LinearLayoutManager(this));
        rvFaq.setAdapter(adapter);
    }
}