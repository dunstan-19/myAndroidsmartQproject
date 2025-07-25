package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterrActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etCPassword;
    private Button btnSignup;
    private TextView tvback;
    private ImageView ivTogglePassword, ivToggleCPassword;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private boolean passwordShowing = false;
    private boolean cPasswordShowing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_registerr);

        // Initialize views
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etemail);
        etPassword = findViewById(R.id.etPassword);
        etCPassword = findViewById(R.id.etCPassword);
        btnSignup = findViewById(R.id.btnSignup);
        tvback = findViewById(R.id.tvback);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleCPassword = findViewById(R.id.ivToggleCPassword);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        // Back to login text click listener
        tvback.setOnClickListener(v -> {
            startActivity(new Intent(RegisterrActivity.this, MainActivity.class));
            finish();
        });

        // Toggle password visibility for main password field
        ivTogglePassword.setOnClickListener(v -> togglePasswordVisibility(etPassword, ivTogglePassword, true));

        // Toggle password visibility for confirm password field
        ivToggleCPassword.setOnClickListener(v -> togglePasswordVisibility(etCPassword, ivToggleCPassword, false));

        // Sign up button click listener
        btnSignup.setOnClickListener(v -> validateAndRegister());
    }

    private void togglePasswordVisibility(EditText editText, ImageView imageView, boolean isMainPassword) {
        if (isMainPassword ? passwordShowing : cPasswordShowing) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            imageView.setImageResource(R.drawable.eyeclosed);
            if (isMainPassword) passwordShowing = false;
            else cPasswordShowing = false;
        } else {
            editText.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            imageView.setImageResource(R.drawable.eyeopen);
            if (isMainPassword) passwordShowing = true;
            else cPasswordShowing = true;
        }
        editText.setSelection(editText.length());
    }

    private void showPassword() {
        etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        etCPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        ivTogglePassword.setImageResource(R.drawable.eyeopen);
        ivToggleCPassword.setImageResource(R.drawable.eyeopen);
        passwordShowing = true;
        cPasswordShowing = true;
        etPassword.setSelection(etPassword.length());
        etCPassword.setSelection(etCPassword.length());
    }

    private void hidePassword() {
        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etCPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        ivTogglePassword.setImageResource(R.drawable.eyeclosed);
        ivToggleCPassword.setImageResource(R.drawable.eyeclosed);
        passwordShowing = false;
        cPasswordShowing = false;
        etPassword.setSelection(etPassword.length());
        etCPassword.setSelection(etCPassword.length());
    }

    private void validateAndRegister() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etCPassword.getText().toString().trim();

        if (!isValidUsername(username)) {
            Toast.makeText(this, "Username should not contain uppercase letters", Toast.LENGTH_SHORT).show();
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
        } else if (!isValidPassword(password)) {
            Toast.makeText(this, "Password must be at least 5 characters long and contain both letters and numbers", Toast.LENGTH_SHORT).show();
        } else if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
        } else {
            registerUser(username, email, password);
        }
    }

    private void registerUser(String username, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        User newUser = new User(username, email, "user");

                        mDatabase.child(userId).setValue(newUser)
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        Toast.makeText(RegisterrActivity.this, "Registration Successful", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(RegisterrActivity.this, MainActivity.class));
                                        finish();
                                    } else {
                                        Toast.makeText(RegisterrActivity.this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(RegisterrActivity.this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isValidUsername(String username) {
        for (char c : username.toCharArray()) {
            if (Character.isUpperCase(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 5) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        return hasLetter && hasDigit;
    }
}

class User {
    public String username, email, role;

    public User() {
        // Default constructor required for Firebase
    }

    public User(String username, String email, String role) {
        this.username = username;
        this.email = email;
        this.role = role;
    }
}