package com.example.alumnos;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        View mainContainer = findViewById(R.id.main_container);
        View imgLogo = findViewById(R.id.imgLogo);
        View txtTitulo = findViewById(R.id.txtTitulo);
        View txtSubtitulo = findViewById(R.id.txtSubtitulo);
        MaterialButton btnIniciarRegistro = findViewById(R.id.btnIniciarRegistro);

        // Cargar animación
        Animation fadeInUp = AnimationUtils.loadAnimation(this, R.anim.fade_in_up);
        
        // Aplicar animación a los elementos
        imgLogo.startAnimation(fadeInUp);
        txtTitulo.startAnimation(fadeInUp);
        txtSubtitulo.startAnimation(fadeInUp);
        btnIniciarRegistro.startAnimation(fadeInUp);

        btnIniciarRegistro.setOnClickListener(v ->
                startActivity(new Intent(this, RegistroActivity.class)));
    }
}