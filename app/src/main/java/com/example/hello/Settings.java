package com.example.hello;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.app.AppCompatDelegate;
public class Settings extends BaseActivity {
private ImageView back;
private LinearLayout profile_settings, update_password;
private Switch switchTheme;
private RadioGroup radioFontSize;
    private Spinner languageSpinner;
   private CardView aboutCard;
    private TextView aboutHeader, aboutContent,logout;
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        languageSpinner = findViewById(R.id.language_spinner);
        saveButton = findViewById(R.id.save_button);

        setupLanguageSpinner();
        setupSaveButton();
back = findViewById(R.id.back);
logout = findViewById(R.id.logout);
radioFontSize = findViewById(R.id.radioFontSize);
switchTheme = findViewById(R.id.switchTheme);
profile_settings = findViewById(R.id.profile_settings);
aboutCard = findViewById(R.id.aboutCard);
aboutHeader = findViewById(R.id.aboutHeader);
aboutContent = findViewById(R.id.aboutContent);

        // Toggle expand/collapse when clicking the header
        aboutHeader.setOnClickListener(v -> {
            if (aboutContent.getVisibility() == View.GONE) {
                aboutContent.setVisibility(View.VISIBLE);
            } else {
                aboutContent.setVisibility(View.GONE);
            }
        });

        update_password = findViewById(R.id.update_password);
        update_password.setOnClickListener(v -> {
            startActivity(new Intent(Settings.this, ChangePasswordActivity.class));
        });
        profile_settings.setOnClickListener(v -> {
            startActivity(new Intent(Settings.this, ProfileActivity.class));
        });
        back.setOnClickListener(v -> {
            startActivity(new Intent(Settings.this, chooseActivity.class));
        });
        logout.setOnClickListener(v -> {
            startActivity(new Intent(Settings.this, MainActivity.class));
        });
        String savedFont = getSharedPreferences("AppSettings", MODE_PRIVATE)
                .getString("FontSize", "Medium");

        switch (savedFont) {
            case "Small":
                radioFontSize.check(R.id.fontSmall);
                setFontScale(0.60f);
                break;
            case "Large":
                radioFontSize.check(R.id.fontLarge);
                setFontScale(1.35f);
                break;
            default:
                radioFontSize.check(R.id.fontMedium);
                setFontScale(1.0f);
                break;
        }

        radioFontSize.setOnCheckedChangeListener((group, checkedId) -> {
            String fontSize = "Medium";
            float scale = 1.0f;

            if (checkedId == R.id.fontSmall) {
                fontSize = "Small";
                scale = 0.60f;
            } else if (checkedId == R.id.fontLarge) {
                fontSize = "Large";
                scale = 1.35f;
            }

            // Apply font scale
            setFontScale(scale);

            // Save preference
            getSharedPreferences("AppSettings", MODE_PRIVATE)
                    .edit()
                    .putString("FontSize", fontSize)
                    .apply();

            recreate(); // refresh activity
        });

        boolean isDarkMode = getSharedPreferences("AppSettings", MODE_PRIVATE)
                .getBoolean("DarkMode", false);

        switchTheme.setChecked(isDarkMode);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }

// Toggle listener
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }

            // Save preference
            getSharedPreferences("AppSettings", MODE_PRIVATE)
                    .edit()
                    .putBoolean("DarkMode", isChecked)
                    .apply();
        });
    }

    private void setFontScale(float scale) {
        android.content.res.Configuration configuration = getResources().getConfiguration();
        configuration.fontScale = scale;
        getResources().updateConfiguration(configuration, getResources().getDisplayMetrics());
    }
    private void setupLanguageSpinner() {
        String[] languages = {getString(R.string.english), getString(R.string.swahili)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        // Set current language as selected
        String currentLanguage = LocaleHelper.getLanguage(this);
        if (currentLanguage.equals("sw")) {
            languageSpinner.setSelection(1);
        } else {
            languageSpinner.setSelection(0);
        }
    }

    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> {
            int selectedPosition = languageSpinner.getSelectedItemPosition();
            String newLanguage = (selectedPosition == 1) ? "sw" : "en";
            String currentLanguage = LocaleHelper.getLanguage(this);

            if (!newLanguage.equals(currentLanguage)) {
                // Change language
                LocaleHelper.setLocale(Settings.this, newLanguage);

                // Restart activity to apply changes
                restartActivity();

                Toast.makeText(Settings.this,
                        getString(R.string.language_changed), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
