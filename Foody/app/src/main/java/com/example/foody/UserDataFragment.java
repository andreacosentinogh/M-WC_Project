package com.example.foody;

import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class UserDataFragment extends Fragment {

    private DBHelper dbHelper;

    private BarChart dailyCaloriesChart, dailyCO2Chart, monthlyCaloriesChart, monthlyCO2Chart;
    private TextView totalProductsTextView, totalCaloriesTextView, totalCO2TextView, itemsListLabel;
    private ListView dailyItemsListView;

    private ArrayAdapter<String> dailyListAdapter;
    private ArrayList<String> dailyItems = new ArrayList<>();

    private ArrayList<String> dayLabels = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_user_data, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DBHelper(getContext());


        dailyCaloriesChart = view.findViewById(R.id.dailyCaloriesChart);
        dailyCO2Chart = view.findViewById(R.id.dailyCO2Chart);
        monthlyCaloriesChart = view.findViewById(R.id.monthlyCaloriesChart);
        monthlyCO2Chart = view.findViewById(R.id.monthlyCO2Chart);

        totalProductsTextView = view.findViewById(R.id.totalProducts);
        totalCaloriesTextView = view.findViewById(R.id.totalCalories);
        totalCO2TextView = view.findViewById(R.id.totalCO2);
        itemsListLabel = view.findViewById(R.id.itemsListLabel);
        dailyItemsListView = view.findViewById(R.id.dailyItemsList);

        dailyListAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, dailyItems);
        dailyItemsListView.setAdapter(dailyListAdapter);


        itemsListLabel.setVisibility(View.GONE);
        dailyItemsListView.setVisibility(View.GONE);

        loadChartData();
    }

    private void loadChartData() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());


        loadDailyData(today);


        loadMonthlyData();
    }

    private void loadDailyData(String date) {
        Cursor dailyCursor = dbHelper.getDataByDate(date);

        float dailyCalories = 0f, dailyCO2 = 0f;
        while (dailyCursor.moveToNext()) {
            dailyCalories += dailyCursor.getFloat(dailyCursor.getColumnIndex("calories"));
            dailyCO2 += dailyCursor.getFloat(dailyCursor.getColumnIndex("co2"));
        }
        dailyCursor.close();


        populateSingleChart(dailyCaloriesChart, dailyCalories, "Today's Calories");
        populateSingleChart(dailyCO2Chart, dailyCO2, "Today's CO₂ Emissions");
    }

    private void loadMonthlyData() {
        Cursor monthlyCursor = dbHelper.getMonthlyData();
        HashMap<String, Float> monthlyCalories = new HashMap<>();
        HashMap<String, Float> monthlyCO2 = new HashMap<>();
        dayLabels.clear();

        while (monthlyCursor.moveToNext()) {
            String date = monthlyCursor.getString(monthlyCursor.getColumnIndex("date"));
            float calories = monthlyCursor.getFloat(monthlyCursor.getColumnIndex("total_calories"));
            float co2 = monthlyCursor.getFloat(monthlyCursor.getColumnIndex("total_co2"));

            monthlyCalories.put(date, calories);
            monthlyCO2.put(date, co2);
            dayLabels.add(date);
        }
        monthlyCursor.close();

        populateChart(monthlyCaloriesChart, monthlyCalories, "Monthly Calories");
        populateChart(monthlyCO2Chart, monthlyCO2, "Monthly CO₂");

        setupMonthlyChartInteraction(monthlyCaloriesChart);
        setupMonthlyChartInteraction(monthlyCO2Chart);
    }

    private void populateSingleChart(BarChart chart, float value, String label) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, value));

        BarDataSet dataSet = new BarDataSet(entries, label);
        BarData barData = new BarData(dataSet);
        chart.setData(barData);

        chart.getXAxis().setEnabled(false);
        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setGranularity(1f);
        chart.setDescription(null);
        chart.invalidate();
    }

    private void populateChart(BarChart chart, HashMap<String, Float> dataMap, String label) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        int index = 0;

        for (String date : dayLabels) {
            float value = dataMap.getOrDefault(date, 0f);
            entries.add(new BarEntry(index++, value));
        }

        BarDataSet dataSet = new BarDataSet(entries, label);
        BarData barData = new BarData(dataSet);
        chart.setData(barData);

        XAxis xAxis = chart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dayLabels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        chart.getAxisRight().setEnabled(false);
        chart.getAxisLeft().setGranularity(1f);
        chart.setDescription(null);
        chart.invalidate();
    }

    private void setupMonthlyChartInteraction(BarChart chart) {
        chart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, com.github.mikephil.charting.highlight.Highlight h) {
                int index = (int) e.getX();
                String selectedDate = dayLabels.get(index);
                loadItemsAndTotalsForDate(selectedDate);
            }

            @Override
            public void onNothingSelected() {
                dailyItems.clear();
                dailyListAdapter.notifyDataSetChanged();
                itemsListLabel.setVisibility(View.GONE);
                dailyItemsListView.setVisibility(View.GONE);
                resetTotals();
            }
        });
    }

    private void loadItemsAndTotalsForDate(String date) {
        Cursor cursor = dbHelper.getDataByDate(date);
        dailyItems.clear();

        float totalCalories = 0f, totalCO2 = 0f;
        int totalProducts = 0;

        while (cursor.moveToNext()) {
            dailyItems.add(cursor.getString(cursor.getColumnIndex("item")));
            totalCalories += cursor.getFloat(cursor.getColumnIndex("calories"));
            totalCO2 += cursor.getFloat(cursor.getColumnIndex("co2"));
            totalProducts++;
        }
        cursor.close();


        totalProductsTextView.setText("Total Products: " + totalProducts);
        totalCaloriesTextView.setText("Total Calories: " + totalCalories);
        totalCO2TextView.setText(String.format(Locale.getDefault(), "Total CO₂: %.2f kg", totalCO2));


        itemsListLabel.setVisibility(View.VISIBLE);
        dailyItemsListView.setVisibility(View.VISIBLE);
        dailyListAdapter.notifyDataSetChanged();
    }

    private void resetTotals() {
        totalProductsTextView.setText("Total Products: 0");
        totalCaloriesTextView.setText("Total Calories: 0");
        totalCO2TextView.setText("Total CO₂: 0 kg");
    }
}

