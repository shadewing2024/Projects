package com.smartaqi.myapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Locale;

public class FeedbckHistories extends AppCompatActivity {

    ListView listFeedView;
    TextView txtName, txtFeedback, txtTime;
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    ArrayList<DocumentSnapshot> list;
    ArrayAdapter<DocumentSnapshot> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feedbck_histories);


        listFeedView = findViewById(R.id.listFeedView);

        String docUID = getIntent().getStringExtra("docId");

        list = new ArrayList<>();
        adapter = new ArrayAdapter<DocumentSnapshot>(this, 0, list){

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {

                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext())
                            .inflate(R.layout.feedcard, parent, false);
                }

                txtName = convertView.findViewById(R.id.txtName);
                txtFeedback = convertView.findViewById(R.id.txtFeedback);
                txtTime = convertView.findViewById(R.id.txtTime);

                DocumentSnapshot doc = list.get(position);

                String name = doc.getString("sender") != null ? doc.getString("sender") : null;
                String text = doc.getString("text") != null ? doc.getString("text") : null;
                String time = doc.getString("time") != null ? doc.getString("time") : null;

                txtName.setText(name);
                txtFeedback.setText(text);
                txtTime.setText(time);

                return convertView;
            }
        };

        listFeedView.setAdapter(adapter);

        loadData(docUID);

        // Delete
        listFeedView.setOnItemLongClickListener((parent, view, position, id) -> {

            String docId = list.get(position).getId();

            new android.app.AlertDialog.Builder(this)
                    .setTitle("Delete Record?")
                    .setMessage("Are you sure you want to delete this record?")
                    .setPositiveButton("Yes", (d, w) -> deleteRecord(docId, docUID))
                    .setNegativeButton("No", null)
                    .show();

            return true;
        });

    }

    public void loadData(String docID) {

        db.collection("Feedback")
                .document(docID)
                .collection("messages")
                .orderBy("millisTime", Query.Direction.DESCENDING)
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

    // ================= DELETE =================
    private void deleteRecord(String docId, String docUId) {

        db.collection("Feedback")
                .document(docUId)
                .collection("messages")
                .document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                    loadData(docUId);
                });
    }
}