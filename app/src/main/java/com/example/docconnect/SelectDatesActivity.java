package com.example.docconnect;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SelectDatesActivity extends AppCompatActivity {

    private ShiftConfiguration incomingConfig;

    // UI Components
    private TextView tvMonthTitle, tvSelectedCount, tvDateRange;
    private ImageView btnPrevMonth, btnNextMonth;
    private GridLayout calendarGrid;
    private AppCompatButton btnGenerateSlots;
    private ImageView btnBack;

    // Chips
    private TextView chipNext10, chipNext30, chipFullMonth;

    // Logic Variables
    private Calendar currentCalendarMonth;
    private List<Long> selectedDatesMillis = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_dates);

        if (getIntent().hasExtra("SHIFT_CONFIG")) {
            incomingConfig = (ShiftConfiguration) getIntent().getSerializableExtra("SHIFT_CONFIG");
        }

        // Initialize Calendar to current month, day 1
        currentCalendarMonth = Calendar.getInstance();
        currentCalendarMonth.set(Calendar.DAY_OF_MONTH, 1);

        initializeViews();
        setupClickListeners();
        updateCalendarGrid();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btn_back);
        calendarGrid = findViewById(R.id.calendarGrid);
        tvMonthTitle = findViewById(R.id.tvMonthTitle);
        btnPrevMonth = findViewById(R.id.btnPrevMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);

        tvSelectedCount = findViewById(R.id.tvSelectedCount);
        tvDateRange = findViewById(R.id.tvDateRange);
        btnGenerateSlots = findViewById(R.id.btnGenerateSlots);

        chipNext10 = findViewById(R.id.chipNext10);
        chipNext30 = findViewById(R.id.chipNext30);
        chipFullMonth = findViewById(R.id.chipFullMonth);
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());

        // Month Navigation
        btnPrevMonth.setOnClickListener(v -> {
            currentCalendarMonth.add(Calendar.MONTH, -1);
            updateCalendarGrid();
        });

        btnNextMonth.setOnClickListener(v -> {
            currentCalendarMonth.add(Calendar.MONTH, 1);
            updateCalendarGrid();
        });

        // ---------------------------------------------------------
        //  DROPDOWN: Month & Year Picker
        // ---------------------------------------------------------
        tvMonthTitle.setOnClickListener(v -> showMonthYearPicker());


        // Chips
        chipNext10.setOnClickListener(v -> selectRangeFromToday(10));
        chipNext30.setOnClickListener(v -> selectRangeFromToday(30));
        chipFullMonth.setOnClickListener(v -> selectFullMonth());

        // Inside setupClickListeners()
        btnGenerateSlots.setOnClickListener(v -> {
            if (selectedDatesMillis.isEmpty()) {
                Toast.makeText(this, "Please select at least one date", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Create Intent
            Intent intent = new Intent(SelectDatesActivity.this, GeneratedSlotsPreviewActivity.class);

            // 2. Pass the Configuration (Times, Breaks, Duration)
            intent.putExtra("SHIFT_CONFIG", incomingConfig);

            // 3. Pass the Selected Dates (Convert List<Long> to long[])
            long[] datesArray = new long[selectedDatesMillis.size()];
            for (int i = 0; i < selectedDatesMillis.size(); i++)
                datesArray[i] = selectedDatesMillis.get(i);
            intent.putExtra("SELECTED_DATES", datesArray);

            // 4. Go!
            startActivity(intent);
        });
    }

    /**
     * SHOW CUSTOM MONTH/YEAR PICKER DIALOG
     */
    private void showMonthYearPicker() {
        // Create a horizontal layout to hold two NumberPickers
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER);
        container.setPadding(50, 50, 50, 50);

        // 1. Month Picker
        final NumberPicker monthPicker = new NumberPicker(this);
        monthPicker.setMinValue(0);
        monthPicker.setMaxValue(11);
        monthPicker.setDisplayedValues(new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"});
        monthPicker.setValue(currentCalendarMonth.get(Calendar.MONTH));
        // Remove focus to fix UI glitch in some android versions
        monthPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        // 2. Year Picker
        final NumberPicker yearPicker = new NumberPicker(this);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        yearPicker.setMinValue(currentYear);
        yearPicker.setMaxValue(currentYear + 10); // Allow picking up to 10 years in future
        yearPicker.setValue(currentCalendarMonth.get(Calendar.YEAR));
        yearPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        // Add views to container
        container.addView(monthPicker);
        container.addView(yearPicker);

        // Build Dialog
        new AlertDialog.Builder(this)
                .setTitle("Select Month & Year")
                .setView(container)
                .setPositiveButton("OK", (dialog, which) -> {
                    // Update the calendar based on selection
                    currentCalendarMonth.set(Calendar.MONTH, monthPicker.getValue());
                    currentCalendarMonth.set(Calendar.YEAR, yearPicker.getValue());
                    currentCalendarMonth.set(Calendar.DAY_OF_MONTH, 1);

                    // Refresh UI
                    updateCalendarGrid();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateCalendarGrid() {
        // Update Title
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
        tvMonthTitle.setText(sdf.format(currentCalendarMonth.getTime()));

        calendarGrid.removeAllViews();

        Calendar iterator = (Calendar) currentCalendarMonth.clone();
        iterator.set(Calendar.DAY_OF_MONTH, 1);

        int firstDayOfWeek = iterator.get(Calendar.DAY_OF_WEEK);
        int daysInMonth = iterator.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Add Empty Spaces
        for (int i = 1; i < firstDayOfWeek; i++) {
            addEmptyDayView();
        }

        // Add Days
        for (int day = 1; day <= daysInMonth; day++) {
            addDayView(day, iterator);
            iterator.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private void addEmptyDayView() {
        TextView emptyView = new TextView(this);
        // Height increased to 50dp (was 40dp)
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dpToPx(50);
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        emptyView.setLayoutParams(params);
        calendarGrid.addView(emptyView);
    }

    private void addDayView(int dayNumber, Calendar calDate) {
        TextView dayView = new TextView(this);
        dayView.setText(String.valueOf(dayNumber));
        dayView.setGravity(Gravity.CENTER);
        dayView.setTextSize(14);

        // --- UPDATED LAYOUT PARAMS ---
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = dpToPx(50); // Increased Height by 25% (40dp -> 50dp)
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);

        // Added Margins for equal spacing
        int margin = dpToPx(4);
        params.setMargins(margin, margin, margin, margin);

        dayView.setLayoutParams(params);

        long dateMillis = calDate.getTimeInMillis();
        int dayOfWeek = calDate.get(Calendar.DAY_OF_WEEK);

        boolean isSunday = (dayOfWeek == Calendar.SUNDAY);
        boolean isSelected = isDateSelected(dateMillis);

        if (isSelected) {
            dayView.setBackgroundResource(R.drawable.bg_date_selected);
            dayView.setTextColor(Color.WHITE);
            dayView.setTypeface(null, android.graphics.Typeface.BOLD);
        } else if (isSunday) {
            dayView.setBackgroundResource(0);
            dayView.setTextColor(Color.parseColor("#BDBDBD"));
        } else {
            dayView.setBackgroundResource(0);
            dayView.setTextColor(ContextCompat.getColor(this, R.color.doc_text_heading));
        }

        dayView.setOnClickListener(v -> toggleDateSelection(dateMillis));
        calendarGrid.addView(dayView);
    }

    private void toggleDateSelection(long dateMillis) {
        long normalizedDate = getStartOfDay(dateMillis);
        if (selectedDatesMillis.contains(normalizedDate)) {
            selectedDatesMillis.remove(normalizedDate);
        } else {
            selectedDatesMillis.add(normalizedDate);
        }
        updateCalendarGrid();
        updateSummaryCard();
    }

    private boolean isDateSelected(long dateMillis) {
        return selectedDatesMillis.contains(getStartOfDay(dateMillis));
    }

    private void updateSummaryCard() {
        tvSelectedCount.setText(selectedDatesMillis.size() + " Dates Selected");
        if (selectedDatesMillis.isEmpty()) {
            tvDateRange.setText("Select dates above");
            return;
        }
        Collections.sort(selectedDatesMillis);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM d", Locale.getDefault());
        String start = sdf.format(new Date(selectedDatesMillis.get(0)));
        String end = sdf.format(new Date(selectedDatesMillis.get(selectedDatesMillis.size() - 1)));
        tvDateRange.setText(start.toUpperCase() + " - " + end.toUpperCase());
    }

    private void selectRangeFromToday(int daysCount) {
        selectedDatesMillis.clear();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < daysCount; i++) {
            if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                selectedDatesMillis.add(getStartOfDay(cal.getTimeInMillis()));
            }
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }
        currentCalendarMonth = Calendar.getInstance();
        currentCalendarMonth.set(Calendar.DAY_OF_MONTH, 1);

        updateCalendarGrid();
        updateSummaryCard();
    }

    private void selectFullMonth() {
        Calendar iter = (Calendar) currentCalendarMonth.clone();
        int max = iter.getActualMaximum(Calendar.DAY_OF_MONTH);

        selectedDatesMillis.clear();

        for(int i=1; i<=max; i++) {
            iter.set(Calendar.DAY_OF_MONTH, i);
            if(iter.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                selectedDatesMillis.add(getStartOfDay(iter.getTimeInMillis()));
            }
        }
        updateCalendarGrid();
        updateSummaryCard();
    }

    private long getStartOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}