package com.example.szabi.fertestapp.view;

import android.graphics.Typeface;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import com.example.szabi.fertestapp.R;
import com.example.szabi.fertestapp.model.face.LabelsType;
import com.example.szabi.fertestapp.model.messages.Feedback;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.example.szabi.fertestapp.Configs.DB_TESTS;

public class FeedbackResultsActivity extends AppCompatActivity {

    private static final int CLASSES = 7;

    private static final String[] labels = {"AF", "AN", "DI", "HA", "NE", "SA", "SU"};


    private DatabaseReference databaseReference;
    private List<Feedback> feedbackList;
    private Map<LabelsType, Integer> intMap;
    private TextView[] trueLabels;
    private TextView[] predictedLabels;
    private TextView[][] confusionMatrixItems;

    private double[][] confusionMatrix;
    private int[] elementCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_results);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Feedback results");

        initElements();
        feedbackList = new ArrayList<>();
        elementCount = new int[CLASSES];
        confusionMatrix = new double[CLASSES][CLASSES];

        databaseReference = FirebaseDatabase.getInstance().getReference(DB_TESTS);
        databaseReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Feedback feedback = dataSnapshot.getValue(Feedback.class);
                feedbackList.add(feedback);
                updateChart(feedback);
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                super.onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    private void updateChart(Feedback feedback) {
        elementCount[intMap.get(feedback.getActual())]++;
        confusionMatrix[intMap.get(feedback.getActual())][intMap.get(feedback.getPredicted())]++;

        for (int i = 0; i < CLASSES; i++) {
            for (int j = 0; j < CLASSES; j++) {
                confusionMatrixItems[i][j].setText(String.valueOf(confusionMatrix[i][j]));
            }
        }
    }

    private void initElements() {
        intMap = new HashMap<>();
        intMap.put(LabelsType.FEAR, 0);
        intMap.put(LabelsType.ANGRY, 1);
        intMap.put(LabelsType.DISGUST, 2);
        intMap.put(LabelsType.HAPPY, 3);
        intMap.put(LabelsType.NEUTRAL, 4);
        intMap.put(LabelsType.SAD, 5);
        intMap.put(LabelsType.SURPRISED, 6);

        trueLabels = new TextView[CLASSES];
        predictedLabels = new TextView[CLASSES];
        confusionMatrixItems = new TextView[CLASSES][CLASSES];

        trueLabels[0] = findViewById(R.id.true_label_0);
        trueLabels[1] = findViewById(R.id.true_label_1);
        trueLabels[2] = findViewById(R.id.true_label_2);
        trueLabels[3] = findViewById(R.id.true_label_3);
        trueLabels[4] = findViewById(R.id.true_label_4);
        trueLabels[5] = findViewById(R.id.true_label_5);
        trueLabels[6] = findViewById(R.id.true_label_6);

        predictedLabels[0] = findViewById(R.id.predicted_label_0);
        predictedLabels[1] = findViewById(R.id.predicted_label_1);
        predictedLabels[2] = findViewById(R.id.predicted_label_2);
        predictedLabels[3] = findViewById(R.id.predicted_label_3);
        predictedLabels[4] = findViewById(R.id.predicted_label_4);
        predictedLabels[5] = findViewById(R.id.predicted_label_5);
        predictedLabels[6] = findViewById(R.id.predicted_label_6);

        confusionMatrixItems[0][0] = findViewById(R.id.matrix_item_0_0);
        confusionMatrixItems[0][1] = findViewById(R.id.matrix_item_0_1);
        confusionMatrixItems[0][2] = findViewById(R.id.matrix_item_0_2);
        confusionMatrixItems[0][3] = findViewById(R.id.matrix_item_0_3);
        confusionMatrixItems[0][4] = findViewById(R.id.matrix_item_0_4);
        confusionMatrixItems[0][5] = findViewById(R.id.matrix_item_0_5);
        confusionMatrixItems[0][6] = findViewById(R.id.matrix_item_0_6);

        confusionMatrixItems[1][0] = findViewById(R.id.matrix_item_1_0);
        confusionMatrixItems[1][1] = findViewById(R.id.matrix_item_1_1);
        confusionMatrixItems[1][2] = findViewById(R.id.matrix_item_1_2);
        confusionMatrixItems[1][3] = findViewById(R.id.matrix_item_1_3);
        confusionMatrixItems[1][4] = findViewById(R.id.matrix_item_1_4);
        confusionMatrixItems[1][5] = findViewById(R.id.matrix_item_1_5);
        confusionMatrixItems[1][6] = findViewById(R.id.matrix_item_1_6);

        confusionMatrixItems[2][0] = findViewById(R.id.matrix_item_2_0);
        confusionMatrixItems[2][1] = findViewById(R.id.matrix_item_2_1);
        confusionMatrixItems[2][2] = findViewById(R.id.matrix_item_2_2);
        confusionMatrixItems[2][3] = findViewById(R.id.matrix_item_2_3);
        confusionMatrixItems[2][4] = findViewById(R.id.matrix_item_2_4);
        confusionMatrixItems[2][5] = findViewById(R.id.matrix_item_2_5);
        confusionMatrixItems[2][6] = findViewById(R.id.matrix_item_2_6);

        confusionMatrixItems[3][0] = findViewById(R.id.matrix_item_3_0);
        confusionMatrixItems[3][1] = findViewById(R.id.matrix_item_3_1);
        confusionMatrixItems[3][2] = findViewById(R.id.matrix_item_3_2);
        confusionMatrixItems[3][3] = findViewById(R.id.matrix_item_3_3);
        confusionMatrixItems[3][4] = findViewById(R.id.matrix_item_3_4);
        confusionMatrixItems[3][5] = findViewById(R.id.matrix_item_3_5);
        confusionMatrixItems[3][6] = findViewById(R.id.matrix_item_3_6);

        confusionMatrixItems[4][0] = findViewById(R.id.matrix_item_4_0);
        confusionMatrixItems[4][1] = findViewById(R.id.matrix_item_4_1);
        confusionMatrixItems[4][2] = findViewById(R.id.matrix_item_4_2);
        confusionMatrixItems[4][3] = findViewById(R.id.matrix_item_4_3);
        confusionMatrixItems[4][4] = findViewById(R.id.matrix_item_4_4);
        confusionMatrixItems[4][5] = findViewById(R.id.matrix_item_4_5);
        confusionMatrixItems[4][6] = findViewById(R.id.matrix_item_4_6);

        confusionMatrixItems[5][0] = findViewById(R.id.matrix_item_5_0);
        confusionMatrixItems[5][1] = findViewById(R.id.matrix_item_5_1);
        confusionMatrixItems[5][2] = findViewById(R.id.matrix_item_5_2);
        confusionMatrixItems[5][3] = findViewById(R.id.matrix_item_5_3);
        confusionMatrixItems[5][4] = findViewById(R.id.matrix_item_5_4);
        confusionMatrixItems[5][5] = findViewById(R.id.matrix_item_5_5);
        confusionMatrixItems[5][6] = findViewById(R.id.matrix_item_5_6);

        confusionMatrixItems[6][0] = findViewById(R.id.matrix_item_6_0);
        confusionMatrixItems[6][1] = findViewById(R.id.matrix_item_6_1);
        confusionMatrixItems[6][2] = findViewById(R.id.matrix_item_6_2);
        confusionMatrixItems[6][3] = findViewById(R.id.matrix_item_6_3);
        confusionMatrixItems[6][4] = findViewById(R.id.matrix_item_6_4);
        confusionMatrixItems[6][5] = findViewById(R.id.matrix_item_6_5);
        confusionMatrixItems[6][6] = findViewById(R.id.matrix_item_6_6);

        for (int i = 0; i < CLASSES; i++) {
            trueLabels[i].setText(labels[i]);
            predictedLabels[i].setText(labels[i]);

            confusionMatrixItems[i][i].setTypeface(null, Typeface.BOLD);
        }
    }
}
