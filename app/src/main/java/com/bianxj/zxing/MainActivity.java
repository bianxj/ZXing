package com.bianxj.zxing;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    private Button bt_scan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findView();
    }

    private void findView(){
        bt_scan = findViewById(R.id.bt_scan);
        bt_scan.setOnClickListener(listener);
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int viewId = v.getId();
            if ( viewId == R.id.bt_scan ){
                Intent intent = new Intent(MainActivity.this,ScanActivity.class);
                startActivity(intent);
            }
        }
    };

}
