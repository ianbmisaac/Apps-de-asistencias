package com.example.profesor;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

/** P-PF01: Pantalla principal del profesor (CU-PF01). */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View mainContainer = findViewById(R.id.main_container);
        MaterialButton btnNuevaSesion = findViewById(R.id.btnNuevaSesion);

        Animation fadeInUp = AnimationUtils.loadAnimation(this, R.anim.fade_in_up);

        mainContainer.startAnimation(fadeInUp);

        btnNuevaSesion.setOnClickListener(v ->
                startActivity(new Intent(this, NuevaSesionActivity.class)));
    }
}