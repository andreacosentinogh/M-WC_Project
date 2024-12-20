package com.example.foody;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public class FoodScanFragment extends Fragment {

    private static final int SPEECH_REQUEST_CODE = 100;

    private EditText foodNameEditText;
    private Button scanButton, searchButton, speechButton;
    private JSONObject foodData;

    public FoodScanFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        foodNameEditText = view.findViewById(R.id.foodNameEditText);
        scanButton = view.findViewById(R.id.scanButton);
        searchButton = view.findViewById(R.id.searchButton);
        speechButton = view.findViewById(R.id.speechButton);


        foodData = loadFoodData();


        scanButton.setOnClickListener(v -> {
            IntentIntegrator intentIntegrator = IntentIntegrator.forSupportFragment(FoodScanFragment.this);
            intentIntegrator.setPrompt("Scan a barcode or QR Code");
            intentIntegrator.setOrientationLocked(true);
            intentIntegrator.initiateScan();
        });


        searchButton.setOnClickListener(v -> {
            String foodName = foodNameEditText.getText().toString().trim();
            if (!foodName.isEmpty()) {
                searchFood(foodName);
            } else {
                Toast.makeText(getContext(), "Please enter a valid food name", Toast.LENGTH_SHORT).show();
            }
        });


        speechButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak the food name");

            try {
                startActivityForResult(intent, SPEECH_REQUEST_CODE);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Speech-to-Text not supported on this device", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private JSONObject loadFoodData() {
        try {
            InputStream is = getResources().openRawResource(R.raw.food_info);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            return new JSONObject(json);
        } catch (Exception e) {
            Log.e("JsonError", "Error loading JSON data", e);
            Toast.makeText(getContext(), "Error loading food data", Toast.LENGTH_SHORT).show();
            return null;
        }
    }


    private void searchFood(String foodName) {
        if (foodData != null && foodData.has(foodName.toLowerCase())) {
            String foodInfo = foodData.optString(foodName.toLowerCase());
            showFoodDetailsDialog(foodName, foodInfo);
        } else {
            Toast.makeText(getContext(), "Food not found in the database", Toast.LENGTH_SHORT).show();
        }
    }


    private void showFoodDetailsDialog(String foodName, String foodInfo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Food Details: " + foodName);

        try {
            JSONObject json = new JSONObject(foodInfo);


            StringBuilder details = new StringBuilder();
            details.append("<b>Calories:</b> ").append(json.optString("calories", "N/A")).append("<br>");
            details.append("<b>Origin:</b> ").append(json.optString("origin", "N/A")).append("<br>");
            details.append("<b>Carbon Footprint:</b> ").append(json.optString("carbon_footprint", "N/A")).append("<br>");
            details.append("<b>Category:</b> ").append(json.optString("category", "N/A")).append("<br>");
            details.append("<b>Vitamins:</b> ");
            for (int i = 0; i < json.getJSONArray("Vitamins").length(); i++) {
                details.append(json.getJSONArray("Vitamins").getString(i));
                if (i != json.getJSONArray("Vitamins").length() - 1) {
                    details.append(", ");
                }
            }

            builder.setMessage(Html.fromHtml(details.toString(), Html.FROM_HTML_MODE_LEGACY));
        } catch (Exception e) {
            builder.setMessage("Error displaying food details.");
        }


        builder.setPositiveButton("Go Back", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Speech-to-text results
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == getActivity().RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                foodNameEditText.setText(result.get(0));
            }
        }


        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (intentResult != null) {
            if (intentResult.getContents() == null) {
                Toast.makeText(getContext(), "Scan cancelled", Toast.LENGTH_SHORT).show();
            } else {
                String scannedText = intentResult.getContents();
                Log.d("ScannedText", scannedText);

                if (foodData != null && foodData.has(scannedText.toLowerCase())) {
                    String foodInfo = foodData.optString(scannedText.toLowerCase());
                    showFoodDetailsDialog(scannedText, foodInfo);
                } else {
                    Toast.makeText(getContext(), "Food not found in the database", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
