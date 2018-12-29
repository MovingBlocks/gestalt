package org.terasology.gestalt.android.testbed;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class GestaltTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestalt_test);
        TextView text = findViewById(R.id.textDisplay);
        //text.setText("Test: " + map.valueFor(Locale.JAPANESE));
    }

}
