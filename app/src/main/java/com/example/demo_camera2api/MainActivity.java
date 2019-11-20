package com.example.demo_camera2api;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
    }

    private void init(){
        List<String> sSource = new ArrayList<>(Arrays.asList(Parameter.getSOURCE().get("-1")));
        sSource.add(Parameter.getSOURCE().get("99"));
        sSource.addAll(Camera2API.ScanAllCamera(getApplicationContext()));

        // adapter
        ListAdapter listAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sSource);

        // list view
        ListView listView = findViewById(R.id.listview_allCamera);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(onItemClickListener);
    }

    ListView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            ListView listView = (ListView) parent;
            String selected = listView.getItemAtPosition(position).toString();

            Intent intent = null;
            if(selected.equals(Parameter.getSOURCE().get("-1"))){
                intent = new Intent(MainActivity.this, ImageProcessorActivity.class);
            }
            else if(selected.equals(Parameter.getSOURCE().get("99"))){
                Toast.makeText(MainActivity.this, "No test task", Toast.LENGTH_LONG).show();
                return;
            }
            else{
                int lens = 0;
                for(String key : Parameter.getSOURCE().keySet()){
                    if(selected.equals(Parameter.getSOURCE().get(key))){
                        lens = Integer.valueOf(key);
                        break;
                    }
                }
                intent = new Intent(MainActivity.this, Camera2Activity.class);
                intent.putExtra("lens", lens);
            }
            startActivity(intent);
        }
    };
}
