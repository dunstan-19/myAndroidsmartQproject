package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private EditText etemail, etPassword;
    private Button btnlogin;
    private TextView tvsign, tvForgotPassword;
    private LottieAnimationView lLoading;
    private ImageView ivTogglePassword;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private boolean passwordShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        etemail = findViewById(R.id.etemail);
        etPassword = findViewById(R.id.etPassword);
        btnlogin = findViewById(R.id.btnlogin);
        tvsign = findViewById(R.id.tvsign);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        lLoading = findViewById(R.id.lLoading);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference("Users");

        // Eye icon toggle for password visibility
        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        // Login button click listener
        btnlogin.setOnClickListener(v -> {
            String email = etemail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(MainActivity.this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            } else if (password.isEmpty()) {
                Toast.makeText(MainActivity.this, "Please enter your password", Toast.LENGTH_SHORT).show();
            } else {
                loginUser(email, password);
            }
        });

        // Sign up text click listener
        tvsign.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RegisterrActivity.class));
        });

        // Forgot password text click listener
        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ForgotPasswordActivity.class));
        });
    }

    private void togglePasswordVisibility() {
        if (passwordShowing) {
            hidePassword();
        } else {
            showPassword();
        }
    }

    private void showPassword() {
        etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        ivTogglePassword.setImageResource(R.drawable.eyeopen);
        passwordShowing = true;
        etPassword.setSelection(etPassword.length());
    }

    private void hidePassword() {
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        ivTogglePassword.setImageResource(R.drawable.eyeclosed);
        passwordShowing = false;
        etPassword.setSelection(etPassword.length());
    }

    private void loginUser(String email, String password) {
        lLoading.setVisibility(View.VISIBLE);
        lLoading.playAnimation();
        btnlogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            checkUserRole(user.getUid());
                        }
                    } else {
                        lLoading.setVisibility(View.GONE);
                        btnlogin.setEnabled(true);
                        Toast.makeText(MainActivity.this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkUserRole(String userId) {
        userRef.child(userId).child("role").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                lLoading.setVisibility(View.GONE);
                btnlogin.setEnabled(true);

                if (snapshot.exists()) {
                    String role = snapshot.getValue(String.class);
                    if ("admin".equals(role)) {
                        Toast.makeText(MainActivity.this, "Welcome, Admin!", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, AdminActivity.class));
                    } else {
                        Toast.makeText(MainActivity.this, "Login Successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, chooseActivity.class));
                    }
                    finish();
                } else {
                    Toast.makeText(MainActivity.this, "User role not found!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                lLoading.setVisibility(View.GONE);
                btnlogin.setEnabled(true);
                Toast.makeText(MainActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}