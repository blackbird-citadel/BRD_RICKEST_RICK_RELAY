package com.brd.relay.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.brd.relay.R;
import com.brd.relay.config.PreferencesManager;
import com.brd.relay.receivers.SmsInterceptReceiver;
import com.brd.relay.services.RelayForegroundService;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_CODE = 101;
    private PreferencesManager prefs;

    private TextView tvBusinessName;
    private TextView tvDailySales;
    private TextView tvSalesCount;
    private TextView tvMonthlySales;
    private EditText etEmployeeName;
    private EditText etWorkerNumber;
    private TextView tvSmsBalance;
    private MaterialSwitch switchRelay;
    private Button btnEnroll;
    private Button btnBuySms;
    private LinearLayout llEmployeeListContainer;
    private TextView tvRickMotto;

    // Real-time Refresh Receiver
    private final BroadcastReceiver metricsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateDashboardMetrics();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new PreferencesManager(this);

        tvBusinessName = findViewById(R.id.tvBusinessName);
        tvDailySales = findViewById(R.id.tvDailySales);
        tvSalesCount = findViewById(R.id.tvSalesCount);
        tvMonthlySales = findViewById(R.id.tvMonthlySales);

        etEmployeeName = findViewById(R.id.etEmployeeName);
        etWorkerNumber = findViewById(R.id.etWorkerNumber);
        tvSmsBalance = findViewById(R.id.tvSmsBalance);
        switchRelay = findViewById(R.id.switchRelay);
        btnEnroll = findViewById(R.id.btnEnroll);
        btnBuySms = findViewById(R.id.btnBuySms);
        llEmployeeListContainer = findViewById(R.id.llEmployeeListContainer);
        tvRickMotto = findViewById(R.id.tvRickMotto);

        if (switchRelay != null) {
            switchRelay.setText("");
            switchRelay.setTextOn("");
            switchRelay.setTextOff("");
            switchRelay.setChecked(prefs.isRelayEnabled());
        }

        String currentShop = prefs.getSelectedShop();
        tvBusinessName.setText((currentShop != null && !currentShop.trim().isEmpty()) ? currentShop : "Main Branch");

        updateDashboardMetrics();
        requestPermissionsIfNecessary();
        renderEnrolledEmployees();

        btnEnroll.setOnClickListener(v -> {
            String name = etEmployeeName.getText().toString().trim();
            String phone = etWorkerNumber.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Please enter both worker name and phone number", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.addWorker(name, phone);
            etEmployeeName.setText("");
            etWorkerNumber.setText("");
            renderEnrolledEmployees();
            Toast.makeText(this, "Worker " + name + " enrolled successfully!", Toast.LENGTH_SHORT).show();
        });

        btnBuySms.setOnClickListener(v -> triggerSmsPurchaseUssd());

        switchRelay.setOnCheckedChangeListener((buttonView, isChecked) -> {
            List<PreferencesManager.Worker> workers = prefs.getWorkers();
            if (isChecked && workers.isEmpty()) {
                Toast.makeText(this, "Enroll at least one worker phone number first", Toast.LENGTH_SHORT).show();
                switchRelay.setChecked(false);
                return;
            }

            prefs.setRelayEnabled(isChecked);
            toggleRelayService(isChecked);
        });

        if (tvRickMotto != null) {
            tvRickMotto.setOnClickListener(v -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=gRYuuF4zT2o"));
                startActivity(browserIntent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboardMetrics();
        renderEnrolledEmployees();

        // Register Dynamic BroadcastReceiver for Instant UI Auto-Refresh
        IntentFilter filter = new IntentFilter(SmsInterceptReceiver.ACTION_REFRESH_METRICS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(metricsReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(metricsReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(metricsReceiver);
        } catch (IllegalArgumentException ignored) {}
    }

    private void updateDashboardMetrics() {
        double dailySales = prefs.getDailySales();
        int salesCount = prefs.getDailyCount();
        double monthlySales = prefs.getMonthlySales();

        tvDailySales.setText(String.format(Locale.US, "Ksh %.2f", dailySales));
        tvSalesCount.setText(salesCount + " Transactions");
        tvMonthlySales.setText(String.format(Locale.US, "Ksh %.2f", monthlySales));

        int balance = prefs.getSmsBalance();
        tvSmsBalance.setText(balance + " SMS remaining");
        if (balance < 15) {
            tvSmsBalance.setTextColor(Color.parseColor("#DC2626"));
        } else {
            tvSmsBalance.setTextColor(Color.parseColor("#059669"));
        }
    }

    private void toggleRelayService(boolean enable) {
        Intent serviceIntent = new Intent(this, RelayForegroundService.class);
        try {
            if (enable) {
                ContextCompat.startForegroundService(this, serviceIntent);
            } else {
                stopService(serviceIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to toggle relay service", Toast.LENGTH_SHORT).show();
            switchRelay.setChecked(false);
            prefs.setRelayEnabled(false);
        }
    }

    private void renderEnrolledEmployees() {
        llEmployeeListContainer.removeAllViews();
        List<PreferencesManager.Worker> workers = prefs.getWorkers();

        if (workers.isEmpty()) {
            TextView emptyTv = new TextView(this);
            emptyTv.setText("No workers enrolled yet.");
            emptyTv.setTextColor(Color.parseColor("#9CA3AF"));
            emptyTv.setPadding(0, 16, 0, 16);
            llEmployeeListContainer.addView(emptyTv);
            return;
        }

        for (PreferencesManager.Worker worker : workers) {
            View itemView = LayoutInflater.from(this).inflate(R.layout.item_employee, llEmployeeListContainer, false);
            TextView tvName = itemView.findViewById(R.id.tvItemEmpName);
            TextView tvPhone = itemView.findViewById(R.id.tvItemEmpPhone);
            TextView tvStatus = itemView.findViewById(R.id.tvStatusLabel);

            tvName.setText(worker.name);
            tvPhone.setText(worker.number);
            tvStatus.setText("ACTIVE");
            tvStatus.setTextColor(Color.parseColor("#10B981"));

            // Long click to remove worker
            itemView.setOnLongClickListener(v -> {
                prefs.removeWorker(worker.number);
                renderEnrolledEmployees();
                Toast.makeText(MainActivity.this, "Removed worker " + worker.name, Toast.LENGTH_SHORT).show();
                return true;
            });

            llEmployeeListContainer.addView(itemView);
        }
    }

    private void triggerSmsPurchaseUssd() {
        String ussdCode = "*180" + Uri.encode("#");
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + ussdCode));
        startActivity(intent);
        Toast.makeText(this, "Select your SMS bundle option from your SIM dialer", Toast.LENGTH_LONG).show();
    }

    private void requestPermissionsIfNecessary() {
        List<String> permissionsNeeded = new ArrayList<>();
        permissionsNeeded.add(Manifest.permission.RECEIVE_SMS);
        permissionsNeeded.add(Manifest.permission.SEND_SMS);
        permissionsNeeded.add(Manifest.permission.READ_SMS);
        permissionsNeeded.add(Manifest.permission.READ_PHONE_STATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsNeeded.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        List<String> permissionsToRequest = new ArrayList<>();
        for (String perm : permissionsNeeded) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(perm);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    PERMISSION_REQ_CODE
            );
        }
    }
}