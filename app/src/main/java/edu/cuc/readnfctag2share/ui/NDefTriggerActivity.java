package edu.cuc.readnfctag2share.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import edu.cuc.readnfctag2share.R;

public class NDefTriggerActivity extends AppCompatActivity {

    private static final String TAG = NDefTriggerActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ndef_trigger);

        Intent intent = getIntent();
        Log.i(TAG, "onCreate: " + intent);
    }

}
