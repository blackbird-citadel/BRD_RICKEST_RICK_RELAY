package com.brd.relay.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.brd.relay.R;
import com.brd.relay.config.PreferencesManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private PreferencesManager prefs;
    private TextInputLayout tilShopName;
    private TextInputEditText etCustomShopName;
    private TextInputEditText etShopPin;
    private Button btnEnterPortal;
    private TextView tvSubtitle;
    private TextView tvRickQuote;

    private boolean isSetupMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        prefs = new PreferencesManager(this);

        tilShopName = findViewById(R.id.tilShopName);
        etCustomShopName = findViewById(R.id.etCustomShopName);
        etShopPin = findViewById(R.id.etShopPin);
        btnEnterPortal = findViewById(R.id.btnEnterPortal);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvRickQuote = findViewById(R.id.tvRickQuote);

        String existingPin = prefs.getShopPin();
        isSetupMode = (existingPin == null || existingPin.trim().isEmpty());

        if (isSetupMode) {
            tvSubtitle.setText("Initialize Dimension Settings");
            tilShopName.setVisibility(View.VISIBLE);
            btnEnterPortal.setText("SET UP DEVICE");
            tvRickQuote.setText("\"Listen to me, Morty. Just enter a shop name and a 4-digit PIN. Don't overthink it.\"");
        } else {
            tvSubtitle.setText("Gateway Locked. Authenticate.");
            tilShopName.setVisibility(View.GONE);
            btnEnterPortal.setText("OPEN PORTAL");
            tvRickQuote.setText("\"I'm not going to tell you the PIN, Morty. That defeats the whole purpose of a PIN!\"");
        }

        btnEnterPortal.setOnClickListener(v -> handlePortalEntry());
    }

    private void handlePortalEntry() {
        if (etShopPin.getText() == null) {
            Toast.makeText(this, "Please enter a PIN", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String pinInput = etShopPin.getText().toString().trim();

        if (pinInput.length() < 4) {
            Toast.makeText(this, "Four digits, Morty! Are you even trying?!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSetupMode) {
            if (etCustomShopName.getText() == null) {
                Toast.makeText(this, "Please enter a shop name", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String shopName = etCustomShopName.getText().toString().trim();
            if (shopName.isEmpty()) {
                Toast.makeText(this, "The shop needs a name! We can't just call it 'Dimension Null'!", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.saveSelectedShop(shopName);
            prefs.saveShopPin(pinInput);

            Toast.makeText(this, "Portal configured. *burp*", Toast.LENGTH_SHORT).show();
            launchMain();
        } else {
            String savedPin = prefs.getShopPin();
            if (savedPin != null && pinInput.equals(savedPin.trim())) {
                launchMain();
            } else {
                Toast.makeText(this, "Wrong PIN! Are you a Galactic Federation spy?!", Toast.LENGTH_LONG).show();
                etShopPin.setText("");
            }
        }
    }

    private void launchMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}