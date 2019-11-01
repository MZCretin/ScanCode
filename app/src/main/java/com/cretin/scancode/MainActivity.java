package com.cretin.scancode;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_scan_in_activity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //在Activity中使用
                startActivity(new Intent(MainActivity.this, DemoInActivity.class));
            }
        });

        findViewById(R.id.btn_scan_in_fragment).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //在Fragment中使用
                startActivity(new Intent(MainActivity.this, DemoInFragment.class));
            }
        });
    }
}

