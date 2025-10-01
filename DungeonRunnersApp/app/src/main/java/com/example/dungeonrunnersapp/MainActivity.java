package com.example.dungeonrunnersapp;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private TextView txt;

    private Button btn_1,btn_2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        txt = findViewById(R.id.TXT);
        btn_1 = findViewById(R.id.btn1);
        btn_2 = findViewById(R.id.btn2);

        btn_1.setOnClickListener( new View.OnClickListener() {
            @Override
                public void onClick(View v) {
                    txt.setText("TEXTO 1");
                }
            }
        );

        btn_2.setOnClickListener( new View.OnClickListener() {
            @Override
                public void onClick(View v) {
                    txt.setText("TEXTO 2");
                }
            }
        );

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}