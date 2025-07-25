package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etemail;
    private Button btnresetPassword;
    private TextView tvstatus, tvback;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_forgot_password);

        etemail = findViewById(R.id.etemail);
        btnresetPassword = findViewById(R.id.btnresetPassword);
        tvstatus = findViewById(R.id.tvstatus);
        tvback = findViewById(R.id.tvback);
        mAuth = FirebaseAuth.getInstance();
        tvback.setOnClickListener(v -> {
            Intent intent = new Intent(ForgotPasswordActivity.this,MainActivity.class);
           finish();
            startActivity(intent);

    });
        btnresetPassword.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email = etemail.getText().toString().trim();

        if (email.isEmpty()) {
            etemail.setError("Email is required");
            etemail.requestFocus();
            return;
        }

        // Send password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            tvstatus.setText("Password reset email sent. Check your inbox.");
                            tvstatus.setVisibility(View.VISIBLE);
                        } else {
                            Toast.makeText(ForgotPasswordActivity.this, "Failed to send reset email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}