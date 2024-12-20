package com.example.foody;

import android.os.Bundle;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class ShoppingCartFragment extends Fragment {

    private ArrayList<String> purchasedItems;
    private ArrayAdapter<String> cartAdapter;
    private ListView cartListView;
    private Button confirmButton, clearButton;
    private DBHelper dbHelper;

    private GestureDetector gestureDetector;

    public ShoppingCartFragment(ArrayList<String> purchasedItems) {
        this.purchasedItems = purchasedItems;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_shopping_cart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dbHelper = new DBHelper(requireContext());


        cartListView = view.findViewById(R.id.cartListView);
        confirmButton = view.findViewById(R.id.confirmButton);
        clearButton = view.findViewById(R.id.clearButton);


        cartAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, purchasedItems);
        cartListView.setAdapter(cartAdapter);


        gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                int position = cartListView.pointToPosition((int) e.getX(), (int) e.getY());
                if (position != ListView.INVALID_POSITION) {
                    String itemToRemove = purchasedItems.get(position);
                    purchasedItems.remove(position);
                    cartAdapter.notifyDataSetChanged();
                    Toast.makeText(getContext(), itemToRemove + " removed from cart.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });


        cartListView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));


        confirmButton.setOnClickListener(v -> {
            if (!purchasedItems.isEmpty()) {
                String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                for (String item : purchasedItems) {
                    dbHelper.addUserData(currentDate, item, 150.0f, 0.5f);
                }

                Toast.makeText(getContext(), "Items confirmed!", Toast.LENGTH_SHORT).show();
                purchasedItems.clear();
                cartAdapter.notifyDataSetChanged();
            } else {
                Toast.makeText(getContext(), "Cart is empty. Nothing to confirm.", Toast.LENGTH_SHORT).show();
            }
        });


        clearButton.setOnClickListener(v -> {
            if (!purchasedItems.isEmpty()) {
                purchasedItems.clear();
                cartAdapter.notifyDataSetChanged();
                Toast.makeText(getContext(), "Cart cleared.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Cart is already empty.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
