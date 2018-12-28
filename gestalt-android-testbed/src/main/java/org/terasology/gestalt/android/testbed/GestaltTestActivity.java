package org.terasology.gestalt.android.testbed;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.terasology.util.collection.KahnSorter;

public class GestaltTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestalt_test);
        TextView text = findViewById(R.id.textDisplay);

        KahnSorter<String> sorter = new KahnSorter<>();
        sorter.addNode("E");
        sorter.addNode("A");
        sorter.addNode("C");
        sorter.addNode("B");
        sorter.addNode("D");
        sorter.addEdge("B", "A");
        sorter.addEdge("C", "B");
        sorter.addEdge("D", "C");
        sorter.addEdge("E", "D");

        text.setText("Test: " + sorter.sort());
    }
}
