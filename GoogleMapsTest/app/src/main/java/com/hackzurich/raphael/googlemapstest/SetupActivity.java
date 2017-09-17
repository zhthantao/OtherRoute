package com.hackzurich.raphael.googlemapstest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class SetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
    }

    public void openMap(View view){
        Intent intent = new Intent(this, MapsTest.class);
        startActivity(intent);
    }
}
