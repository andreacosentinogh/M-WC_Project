package com.example.foody;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class HomeFragment extends Fragment {

    private static ArrayList<String> purchasedItems = new ArrayList<>();
    private JSONObject foodData;

    private EditText foodNameEditText;
    private Button searchButton, scanButton, speechButton;

    private AlertDialog currentDialog;

    private final ActivityResultLauncher<Intent> scanLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        IntentResult intentResult = IntentIntegrator.parseActivityResult(IntentIntegrator.REQUEST_CODE, result.getResultCode(), data);
                        if (intentResult != null && intentResult.getContents() != null) {
                            String scannedText = intentResult.getContents();
                            Toast.makeText(getContext(), "Scan successful: " + scannedText, Toast.LENGTH_SHORT).show();


                            vibrate();


                            searchFood(scannedText);
                        } else {
                            Toast.makeText(getContext(), "Scan cancelled", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    private final ActivityResultLauncher<Intent> speechLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    ArrayList<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (results != null && !results.isEmpty()) {
                        foodNameEditText.setText(results.get(0));
                    }
                }
            });

    public static ArrayList<String> getPurchasedItems() {
        return purchasedItems;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        foodNameEditText = view.findViewById(R.id.foodNameEditText);
        searchButton = view.findViewById(R.id.searchButton);
        scanButton = view.findViewById(R.id.scanButton);
        speechButton = view.findViewById(R.id.speechButton);


        searchButton.setOnClickListener(v -> {
            String foodName = foodNameEditText.getText().toString().trim();
            if (!foodName.isEmpty()) {
                searchFood(foodName);
                foodNameEditText.setText("");
                foodNameEditText.setHint("Enter food name");
            } else {
                Toast.makeText(getContext(), "Please enter a valid food name", Toast.LENGTH_SHORT).show();
            }
        });


        scanButton.setOnClickListener(v -> {
            IntentIntegrator intentIntegrator = new IntentIntegrator(requireActivity());
            intentIntegrator.setPrompt("Scan a barcode or QR Code");
            intentIntegrator.setOrientationLocked(true);
            intentIntegrator.setBeepEnabled(false);
            scanLauncher.launch(intentIntegrator.createScanIntent());
        });


        speechButton.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak the food name");

            try {
                speechLauncher.launch(intent);
            } catch (Exception e) {
                Toast.makeText(getContext(), "Speech-to-Text not supported on this device", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void searchFood(String foodName) {

        showFoodDetailsDialog(foodName);


        vibrate();
    }


    private void showFoodDetailsDialog(String foodName) {

        String foodInfo = "{\"calories\": 150, \"origin\": \"USA\", \"category\": \"Fruit\", \"Vitamins\": [\"Vitamin C\", \"Vitamin A\"]}";

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Food Details: " + foodName);

        try {
            JSONObject json = new JSONObject(foodInfo);


            StringBuilder details = new StringBuilder();
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String label = key.replace("_", " ");
                Object value = json.get(key);

                if (value instanceof String || value instanceof Number) {
                    details.append("<b>").append(label).append(":</b> ").append(value).append("<br>");
                } else if (value instanceof org.json.JSONArray) {
                    details.append("<b>").append(label).append(":</b> ");
                    org.json.JSONArray array = (org.json.JSONArray) value;
                    for (int i = 0; i < array.length(); i++) {
                        details.append(array.getString(i));
                        if (i != array.length() - 1) {
                            details.append(", ");
                        }
                    }
                    details.append("<br>");
                }
            }

            builder.setMessage(Html.fromHtml(details.toString(), Html.FROM_HTML_MODE_LEGACY));
        } catch (Exception e) {
            builder.setMessage("Error displaying food details.");
        }


        builder.setPositiveButton("Add to Cart", (dialog, which) -> {
            purchasedItems.add(foodName);
            Toast.makeText(getContext(), foodName + " added to cart.", Toast.LENGTH_SHORT).show();
        });


        builder.setNegativeButton("Go Back", (dialog, which) -> dialog.dismiss());


        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }

        currentDialog = builder.create();
        currentDialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (currentDialog != null && currentDialog.isShowing()) {
            currentDialog.dismiss();
        }
    }


    private void vibrate() {
        Vibrator vibrator = (Vibrator) requireContext().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}
