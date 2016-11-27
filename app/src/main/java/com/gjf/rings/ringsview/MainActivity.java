package com.gjf.rings.ringsview;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((RingsView) findViewById(R.id.rv_circle)).start();
        ((RingsView) findViewById(R.id.rv_oval)).start();
        ((RingsView) findViewById(R.id.rv_rings_oval)).start();
        ((RingsView) findViewById(R.id.rv_rings_circle)).start();
    }
}
