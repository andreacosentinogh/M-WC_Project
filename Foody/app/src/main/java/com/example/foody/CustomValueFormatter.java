package com.example.foody;

import com.github.mikephil.charting.formatter.ValueFormatter;

public class CustomValueFormatter extends ValueFormatter {
    @Override
    public String getFormattedValue(float value) {
        return String.valueOf((int) value);
    }
}
