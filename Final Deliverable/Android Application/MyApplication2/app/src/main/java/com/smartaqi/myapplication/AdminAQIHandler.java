package com.smartaqi.myapplication;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

// Color Picker
import yuku.ambilwarna.AmbilWarnaDialog;
import petrov.kristiyan.colorpicker.ColorPicker;

import java.util.ArrayList;
import java.util.Locale;

public class AdminAQIHandler extends AppCompatActivity {

    TextView txtValMin, txtValMax, txtValCategory, txtValImpact, txtValAction, txtValColor, txtColored;

    Button addAQIBtn;
    CardView aqiCard;
    ListView listViewAQICol;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    ArrayList<DocumentSnapshot> list;
    ArrayAdapter<DocumentSnapshot> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_aqihandler);

        listViewAQICol = findViewById(R.id.listViewAQICol);
        addAQIBtn = findViewById(R.id.addAQIBtn);
        addAQIBtn.setOnClickListener(v -> rangeHandler());

        list = new ArrayList<>();
        adapter = new ArrayAdapter<DocumentSnapshot>(this, 0, list) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext())
                            .inflate(R.layout.aqi_collection, parent, false);
                }

                // ✅ 1. Use local variables and find views inside convertView
                txtValMin = convertView.findViewById(R.id.txtValMin);
                txtValMax = convertView.findViewById(R.id.txtValMax);
                txtValCategory = convertView.findViewById(R.id.txtValCategory);
                txtValColor = convertView.findViewById(R.id.txtValColor);
                txtColored = convertView.findViewById(R.id.txtColored);
                txtValImpact = convertView.findViewById(R.id.txtValImpact);
                txtValAction = convertView.findViewById(R.id.txtValAction);
                aqiCard = findViewById(R.id.aqiCard);

                DocumentSnapshot doc = list.get(position);

                // Fetch data
                int minimum = doc.getDouble("minimum") != null ? doc.getDouble("minimum").intValue() : 0;
                int maximum = doc.getDouble("maximum") != null ? doc.getDouble("maximum").intValue() : 0;
                String color = doc.getString("color") != null ? doc.getString("color") : "#888888"; // Default color
                String category = doc.getString("category") != null ? doc.getString("category") : "N/A";
                String impact = doc.getString("health_impact") != null ? doc.getString("health_impact") : "N/A";
                String rec = doc.getString("recommendation") != null ? doc.getString("recommendation") : "N/A";

                // ✅ 2. Use String.valueOf() to prevent ResourceNotFoundException
                txtValMin.setText(String.valueOf(minimum));
                txtValMax.setText(String.valueOf(maximum));
                txtValColor.setText(color);
                txtColored.setBackgroundColor(Color.parseColor(color));
                txtValCategory.setText(category);
                txtValCategory.setTextColor(Color.parseColor(color));
                txtValColor.setTextColor(Color.DKGRAY);
                txtValImpact.setText(impact);
                txtValAction.setText(rec);

                /*// ✅ 3. Safely parse color
                try {

                } catch (Exception e) {
                    txtColored.setBackgroundColor(Color.GRAY);
                }*/

                return convertView;
            }
        };

        listViewAQICol.setAdapter(adapter);

        loadData();

        /*// FILTER
        filterBtn.setOnClickListener(v -> {
            String getDate = filterInput.getText().toString().trim();

            if (!getDate.isEmpty()) {
                try {
                    *//*int maxAQI = Integer.parseInt(value);*//*
                    filterData(getDate);
                } catch (Exception e) {
                    Toast.makeText(this, "Date not found", Toast.LENGTH_SHORT).show();
                }
            }
        });*/

        // Card Swip Right
       /* GestureDetector gestureDetector = new GestureDetector(this,
                new GestureDetector.SimpleOnGestureListener() {

                    private static final int SWIPE_THRESHOLD = 100;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {

                        float diffX = e2.getX() - e1.getX();

                        // ✅ RIGHT SWIPE
                        if (diffX > SWIPE_THRESHOLD &&
                                Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                            return true;
                        }

                        return false;
                    }
                });*/

        /*// DELETE
        listViewAQICol.setOnTouchListener(new View.OnTouchListener() {

            private int position = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                // Detect touched item position
                position = listViewAQICol.pointToPosition(
                        (int) event.getX(),
                        (int) event.getY()
                );

                if (gestureDetector.onTouchEvent(event)) {

                    if (position != ListView.INVALID_POSITION) {

                        String docId = list.get(position).getId();

                        // ✅ Animate card swipe
                        View itemView = listViewAQICol.getChildAt(
                                position - listViewAQICol.getFirstVisiblePosition()
                        );

                        if (itemView != null) {

                            itemView.animate()
                                    .translationX(1000)
                                    .alpha(0)
                                    .setDuration(300)
                                    .withEndAction(() -> {

                                        // ✅ DELETE DIALOG
                                        new android.app.AlertDialog.Builder(AdminAQIHandler.this)
                                                .setTitle("Delete Record?")
                                                .setMessage("Delete this AQI record?")
                                                .setPositiveButton("Yes", (d, w) -> {
                                                    deleteRecord(docId);
                                                })
                                                .setNegativeButton("No", (d, w) -> {

                                                    // restore card if cancel
                                                    itemView.setTranslationX(0);
                                                    itemView.setAlpha(1);

                                                })
                                                .show();

                                    })
                                    .start();
                        }
                    }

                    return true;
                }

                return false;
            }
        });*/

        // Delete
        listViewAQICol.setOnItemLongClickListener((parent, view, position, id) -> {

            String docId = list.get(position).getId();

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Delete Record?")
                    .setMessage("Are you sure you want to delete this AQI record?")
                    .setPositiveButton("Yes", (d, w) -> deleteRecord(docId))
                    .setNegativeButton("No", null)
                    .show();

            return true;
        });

        // Update
        listViewAQICol.setOnItemClickListener((parent, view, position, id) -> {

            String docId = list.get(position).getId();

            updateHandler(docId);

            Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show();

        });
    }

    private void loadData() {

        db.collection("AQI_Collection")
                .document("AQI_Doc")
                .collection("Catalog")
                .orderBy("maximum", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {

                    list.clear();

                    for (DocumentSnapshot doc : query) {
                        list.add(doc);
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Log.e("ERROR", e.getMessage())
                );
    }

    private void rangeHandler() {
        /*FirebaseFirestore db = FirebaseFirestore.getInstance();*/
        db.collection("AQI_Collection")
                .document("AQI_Doc")
                .collection("Catalog")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int minVal = 0;
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        Long maxValLong = doc.getLong("maximum");
                        minVal = maxValLong != null ? maxValLong.intValue() + 1 : 0;
                    }
                    addNewAQI(minVal);
                });
    }

    private void addNewAQI(int minVal) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.HORIZONTAL);

        String colorVal = "#888888";
        String txtColor = "#333333";

        TextView minTxt = new TextView(this);
        minTxt.setText("Minimum AQI Value:");
        minTxt.setTextColor(Color.parseColor(txtColor));
        EditText minAQIValue = new EditText(this);
        minAQIValue.setText(String.valueOf(minVal));
        minAQIValue.setTextColor(Color.parseColor(txtColor));

        TextView maxTxt = new TextView(this);
        maxTxt.setText("Maximum AQI Value:");
        maxTxt.setTextColor(Color.parseColor(txtColor));
        EditText maxAQIValue = new EditText(this);
        maxAQIValue.setTextColor(Color.parseColor(txtColor));

        TextView colorTxt = new TextView(this);
        colorTxt.setText("Color (Click box to pick):");
        colorTxt.setTextColor(Color.parseColor(txtColor));

        EditText colorCode = new EditText(this);
        colorCode.setText(colorVal);
        colorCode.setTextColor(Color.parseColor(txtColor));

        // Create the square preview box
        TextView coloredBox = new TextView(this);
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(50, 50);
        boxParams.setMargins(120, 0, 0, 0);
        coloredBox.setLayoutParams(boxParams);
        coloredBox.setBackgroundColor(Color.parseColor(colorVal));

        // --- THE COLOR PICKER LOGIC ---
        coloredBox.setOnClickListener(v -> {
            ColorPicker colorPicker = new ColorPicker(AdminAQIHandler.this);

            // 1. Prepare the custom button for the palette icon
            Button btnCustom = new Button(this);
            String btnBg = "#FFFFFF";
            String btnTxtCol = "#000011";
            btnCustom.setBackgroundColor(Color.parseColor(btnBg));
            btnCustom.setTextColor(Color.parseColor(btnTxtCol));

            // 2. Setup the picker
            colorPicker.setRoundColorButton(true)
                    .setColumns(4)
                    .setTitle("Pick Color")
                    .setOnChooseColorListener(new ColorPicker.OnChooseColorListener() {
                        @Override
                        public void onChooseColor(int position, int color) {
                            String hexColor = String.format("#%06X", (0xFFFFFF & color));
                            colorCode.setText(hexColor);
                            coloredBox.setBackgroundColor(color);
                        }

                        @Override
                        public void onCancel() {}
                    })
                    .addListenerButton("Custom", btnCustom, new ColorPicker.OnButtonListener() {
                        @Override
                        public void onClick(View v, int position, int color) {
                            colorPicker.dismissDialog();
                            openAmbilWarna(colorCode, coloredBox);
                        }
                    });
                    colorPicker.show();
                    if (colorPicker.getmDialog() != null &&
                        colorPicker.getmDialog().getWindow() != null) {

                        colorPicker.getmDialog()
                                .getWindow()
                                .setBackgroundDrawableResource(
                                R.drawable.dialog_white_bg
                        );
            }
        });

        innerLayout.addView(colorCode);
        innerLayout.addView(coloredBox);

        TextView categoryTxt = new TextView(this);
        categoryTxt.setText("Category:");
        categoryTxt.setTextColor(Color.parseColor(txtColor));
        EditText aqiCategory = new EditText(this);
        aqiCategory.setTextColor(Color.parseColor(txtColor));

        TextView impactTxt = new TextView(this);
        impactTxt.setText("Health Impact:");
        impactTxt.setTextColor(Color.parseColor(txtColor));
        EditText healthImpact = new EditText(this);
        healthImpact.setTextColor(Color.parseColor(txtColor));

        TextView recommendationTxt = new TextView(this);
        recommendationTxt.setText("Recommend Action:");
        recommendationTxt.setTextColor(Color.parseColor(txtColor));
        EditText recommendedAction = new EditText(this);
        recommendedAction.setTextColor(Color.parseColor(txtColor));

        // Add all views to the main layout
        layout.addView(minTxt);
        layout.addView(minAQIValue);
        layout.addView(maxTxt);
        layout.addView(maxAQIValue);
        layout.addView(colorTxt);
        layout.addView(innerLayout);
        layout.addView(categoryTxt);
        layout.addView(aqiCategory);
        layout.addView(impactTxt);
        layout.addView(healthImpact);
        layout.addView(recommendationTxt);
        layout.addView(recommendedAction);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add New AQI Range")
                .setView(layout)
                .setPositiveButton("Add", (d, w) -> {
                    String minStr = minAQIValue.getText().toString().trim();
                    String maxStr = maxAQIValue.getText().toString().trim();
                    String category = aqiCategory.getText().toString().trim();
                    String color = colorCode.getText().toString().trim();
                    String impact = healthImpact.getText().toString().trim();
                    String rec = recommendedAction.getText().toString().trim();

                    // 1. Basic Empty Validation
                    if (minStr.isEmpty() || maxStr.isEmpty() || category.isEmpty() || color.isEmpty()) {
                        Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        // 2. Validate Hexadecimal Color format
                        // This line will throw an Exception if color is not like "#FF0000"
                        android.graphics.Color.parseColor(color);

                        // 3. Validate Numbers
                        int min = Integer.parseInt(minStr);
                        int max = Integer.parseInt(maxStr);

                        // 4. TRIGGER THE HANDLER if everything is valid
                        RangeHandler handler = new RangeHandler();
                        handler.addAqiRange(min, max, category, color, impact, rec, new RangeHandler.RangeCheckCallback() {
                            @Override
                            public void onSuccess(String message) {
                                Toast.makeText(AdminAQIHandler.this, message, Toast.LENGTH_SHORT).show();
                                loadData(); // Refresh the list
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(AdminAQIHandler.this, "Error: " + error, Toast.LENGTH_LONG).show();
                            }
                        });

                    } catch (IllegalArgumentException e) {
                        // ✅ This handles the Hex Color Error
                        Toast.makeText(this, "Invalid Color! Please use Hex format (e.g. #FF0000)", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
                dialog.show();

                dialog.getWindow().setBackgroundDrawableResource(
                        R.drawable.dialog_white_bg
                );

    }

    // ================= UPDATE =================
    private void updateHandler(String docId) {
        /*FirebaseFirestore db = FirebaseFirestore.getInstance();*/
        db.collection("AQI_Collection")
                .document("AQI_Doc")
                .collection("Catalog")
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (doc.exists()) {

                        int minimum = doc.getDouble("minimum") != null ? doc.getDouble("minimum").intValue() : 0;
                        int maximum = doc.getDouble("maximum") != null ? doc.getDouble("maximum").intValue() : 0;
                        String color = doc.getString("color") != null ? doc.getString("color") : "#888888"; // Default color
                        String category = doc.getString("category") != null ? doc.getString("category") : "N/A";
                        String impact = doc.getString("health_impact") != null ? doc.getString("health_impact") : "N/A";
                        String rec = doc.getString("recommendation") != null ? doc.getString("recommendation") : "N/A";

                        updateAQI(docId, minimum, maximum, color, category, impact, rec);
                    }

                });
    }

    // ================= UPDATE METHOD =================
    private void updateAQI(String docID, int minVal, int maxVal, String colorVal, String categoryVal, String impactVal, String recommendationVal) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 40);

        LinearLayout innerLayout = new LinearLayout(this);
        innerLayout.setOrientation(LinearLayout.HORIZONTAL);

        String txtColor = "#333333";

        TextView minTxt = new TextView(this);
        minTxt.setText("Minimum AQI Value:");
        minTxt.setTextColor(Color.parseColor(txtColor));
        EditText minAQIValue = new EditText(this);
        minAQIValue.setText(String.valueOf(minVal));//new
        minAQIValue.setTextColor(Color.parseColor(txtColor));

        TextView maxTxt = new TextView(this);
        maxTxt.setText("Maximum AQI Value:");
        maxTxt.setTextColor(Color.parseColor(txtColor));
        EditText maxAQIValue = new EditText(this);
        maxAQIValue.setText(String.valueOf(maxVal));//new
        maxAQIValue.setTextColor(Color.parseColor(txtColor));

        TextView colorTxt = new TextView(this);
        colorTxt.setText("Color (Click box to pick):");
        colorTxt.setTextColor(Color.parseColor(txtColor));

        EditText colorCode = new EditText(this);
        colorCode.setText(colorVal);
        colorCode.setTextColor(Color.parseColor(txtColor));

        // Create the square preview box
        TextView coloredBox = new TextView(this);
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(50, 50);
        boxParams.setMargins(120, 0, 0, 0);
        coloredBox.setLayoutParams(boxParams);
        coloredBox.setBackgroundColor(Color.parseColor(colorVal));

        // --- THE COLOR PICKER LOGIC ---
        coloredBox.setOnClickListener(v -> {
            ColorPicker colorPicker = new ColorPicker(AdminAQIHandler.this);

            // 1. Prepare the custom button for the palette icon
            Button btnCustom = new Button(this);
            String btnBg = "#FFFFFF";
            String btnTxtCol = "#000011";
            btnCustom.setBackgroundColor(Color.parseColor(btnBg));
            btnCustom.setTextColor(Color.parseColor(btnTxtCol));

            // 2. Setup the picker
            colorPicker.setRoundColorButton(true)
                    .setColumns(4)
                    .setTitle("Pick Color")
                    .setOnChooseColorListener(new ColorPicker.OnChooseColorListener() {
                        @Override
                        public void onChooseColor(int position, int color) {
                            String hexColor = String.format("#%06X", (0xFFFFFF & color));
                            colorCode.setText(hexColor);
                            coloredBox.setBackgroundColor(color);
                        }

                        @Override
                        public void onCancel() {}
                    })
                    .addListenerButton("Custom", btnCustom, new ColorPicker.OnButtonListener() {
                        @Override
                        public void onClick(View v, int position, int color) {
                            colorPicker.dismissDialog();
                            openAmbilWarna(colorCode, coloredBox);
                        }
                    });
            colorPicker.show();
            if (colorPicker.getmDialog() != null &&
                    colorPicker.getmDialog().getWindow() != null) {

                colorPicker.getmDialog()
                        .getWindow()
                        .setBackgroundDrawableResource(
                                R.drawable.dialog_white_bg
                        );
            }
        });

        innerLayout.addView(colorCode);
        innerLayout.addView(coloredBox);

        TextView categoryTxt = new TextView(this);
        categoryTxt.setText("Category:");
        categoryTxt.setTextColor(Color.parseColor(txtColor));
        EditText aqiCategory = new EditText(this);
        aqiCategory.setText(categoryVal);//new
        aqiCategory.setTextColor(Color.parseColor(txtColor));

        TextView impactTxt = new TextView(this);
        impactTxt.setText("Health Impact:");
        impactTxt.setTextColor(Color.parseColor(txtColor));
        EditText healthImpact = new EditText(this);
        healthImpact.setText(impactVal);//new
        healthImpact.setTextColor(Color.parseColor(txtColor));

        TextView recommendationTxt = new TextView(this);
        recommendationTxt.setText("Recommend Action:");
        recommendationTxt.setTextColor(Color.parseColor(txtColor));
        EditText recommendedAction = new EditText(this);
        recommendedAction.setText(recommendationVal);//new
        recommendedAction.setTextColor(Color.parseColor(txtColor));

        // Add all views to the main layout
        layout.addView(minTxt);
        layout.addView(minAQIValue);
        layout.addView(maxTxt);
        layout.addView(maxAQIValue);
        layout.addView(colorTxt);
        layout.addView(innerLayout);
        layout.addView(categoryTxt);
        layout.addView(aqiCategory);
        layout.addView(impactTxt);
        layout.addView(healthImpact);
        layout.addView(recommendationTxt);
        layout.addView(recommendedAction);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Update AQI")
                .setView(layout)
                .setPositiveButton("Update", (d, w) -> {
                    String color = colorCode.getText().toString().trim();
                    // ... (Keep your existing validation and Firebase logic here) ...
                    try {
                        int min = Integer.parseInt(minAQIValue.getText().toString().trim());
                        int max = Integer.parseInt(maxAQIValue.getText().toString().trim());
                        Color.parseColor(color);

                        ExistingRangeHandler exhandler = new ExistingRangeHandler(docID);
                        exhandler.updateAQI(min, max, aqiCategory.getText().toString(), color, healthImpact.getText().toString(), recommendedAction.getText().toString(), new ExistingRangeHandler.RangeCheckCallback() {
                            @Override
                            public void onSuccess(String message) {
                                Toast.makeText(AdminAQIHandler.this, message, Toast.LENGTH_SHORT).show();
                                loadData();
                            }
                            @Override
                            public void onError(String error) {
                                Toast.makeText(AdminAQIHandler.this, "Error: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (Exception e) {
                        Toast.makeText(this, "Invalid Data!", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
                dialog.show();

                dialog.getWindow().setBackgroundDrawableResource(
                        R.drawable.dialog_white_bg
                );
    }

// ================= COLOR WHEEL PICKER (AMBIL WARNA) ===========

    private void openAmbilWarna(EditText targetEditText, TextView targetPreviewBox) {
        int initialColor = Color.WHITE;
        try {
            initialColor = Color.parseColor(targetEditText.getText().toString());
        } catch (Exception e) { /* default to white */ }

        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, initialColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                String hexColor = String.format("#%06X", (0xFFFFFF & color));
                targetEditText.setText(hexColor);
                targetPreviewBox.setBackgroundColor(color);
            }

            @Override
            public void onCancel(AmbilWarnaDialog dialog) {}
        });
        dialog.show();
    }

    // ================= DELETE =================
    private void deleteRecord(String docId) {

        db.collection("AQI_Collection")
                .document("AQI_Doc")
                .collection("Catalog")
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadData();
                });
    }
}
