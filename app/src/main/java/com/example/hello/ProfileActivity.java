package com.example.hello;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {
    private EditText edtfullName, edtusername, edtemail, edtphoneNumber;
    private ShapeableImageView imgprofile;
    private FloatingActionButton fabChangePhoto;
    private Button BtnSave, BtnSignOut;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    private static final int PICK_IMAGE_REQUEST = 1;
    private Bitmap selectedBitmap;
    private String originalFullName, originalUsername, originalPhone, originalProfileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        usersRef = FirebaseDatabase.getInstance().getReference("Users").child(user.getUid());

        edtfullName = findViewById(R.id.edtfullName);
        edtusername = findViewById(R.id.edtusername);
        edtemail = findViewById(R.id.edtemail);
        edtphoneNumber = findViewById(R.id.edtphoneNumber);
        imgprofile = findViewById(R.id.imgprofile);
        fabChangePhoto = findViewById(R.id.fabChangePhoto); // FloatingActionButton for profile changes
        BtnSave = findViewById(R.id.BtnSave);
        BtnSignOut = findViewById(R.id.BtnSignOut);

        BtnSave.setEnabled(false); // Initially disable save button

        if (user != null) {
            edtemail.setText(user.getEmail());
            usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        originalUsername = snapshot.child("username").getValue(String.class);
                        originalFullName = snapshot.child("fullName").getValue(String.class);
                        originalPhone = snapshot.child("phone").getValue(String.class);
                        originalProfileImage = snapshot.child("profileImage").getValue(String.class);

                        edtusername.setText(originalUsername);
                        edtfullName.setText(originalFullName);
                        edtphoneNumber.setText(originalPhone);

                        if (originalProfileImage != null && !originalProfileImage.isEmpty()) {
                            byte[] decodedString = Base64.decode(originalProfileImage, Base64.DEFAULT);
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            imgprofile.setImageBitmap(decodedBitmap);
                        }
                    }

                    addTextWatchers();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            });
        }

        fabChangePhoto.setOnClickListener(v -> showPhotoOptions());
        BtnSave.setOnClickListener(v -> saveChanges());
        BtnSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
            finish();
        });
    }

    private void showPhotoOptions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Profile Photo")
                .setItems(new CharSequence[]{"Choose from Gallery", "Remove Photo"}, (dialog, which) -> {
                    if (which == 0) {
                        openFileChooser();
                    } else {
                        removeProfilePhoto();
                    }
                })
                .show();
    }

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void removeProfilePhoto() {
        imgprofile.setImageResource(R.drawable.baseline_account_circle_24);
        selectedBitmap = null;
        usersRef.child("profileImage").removeValue();
        BtnSave.setEnabled(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                InputStream imageStream = getContentResolver().openInputStream(imageUri);
                selectedBitmap = BitmapFactory.decodeStream(imageStream);
                imgprofile.setImageBitmap(selectedBitmap);
                checkForChanges();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void saveChanges() {
        String fullName = edtfullName.getText().toString().trim();
        String username = edtusername.getText().toString().trim();
        String phone = edtphoneNumber.getText().toString().trim();

        usersRef.child("fullName").setValue(fullName);
        usersRef.child("username").setValue(username);
        usersRef.child("phone").setValue(phone);

        if (selectedBitmap != null) {
            ProgressDialog progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("Saving...");
            progressDialog.show();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos);
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            usersRef.child("profileImage").setValue(encodedImage).addOnCompleteListener(task -> {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    originalProfileImage = encodedImage;
                    Toast.makeText(ProfileActivity.this, "Profile Updated", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ProfileActivity.this, "Failed to update profile", Toast.LENGTH_SHORT).show();
                }
            });
        }

        originalFullName = fullName;
        originalUsername = username;
        originalPhone = phone;
        BtnSave.setEnabled(false);
    }

    private void addTextWatchers() {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                checkForChanges();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        };

        edtfullName.addTextChangedListener(textWatcher);
        edtusername.addTextChangedListener(textWatcher);
        edtphoneNumber.addTextChangedListener(textWatcher);
    }

    private void checkForChanges() {
        String fullName = edtfullName.getText().toString().trim();
        String username = edtusername.getText().toString().trim();
        String phone = edtphoneNumber.getText().toString().trim();

        boolean nameChanged = !fullName.equals(originalFullName);
        boolean usernameChanged = !username.equals(originalUsername);
        boolean phoneChanged = !phone.equals(originalPhone);
        boolean imageChanged = selectedBitmap != null;

        BtnSave.setEnabled(nameChanged || usernameChanged || phoneChanged || imageChanged);
    }
}
