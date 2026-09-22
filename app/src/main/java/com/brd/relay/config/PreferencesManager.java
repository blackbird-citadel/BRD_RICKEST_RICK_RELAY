package com.brd.relay.config;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PreferencesManager {

    private static final String PREF_NAME = "BRD_Relay_Prefs";
    private static final String KEY_SELECTED_SHOP = "selected_shop";
    private static final String KEY_SHOP_PIN = "shop_pin";
    private static final String KEY_RELAY_ENABLED = "relay_enabled";
    private static final String KEY_SMS_BALANCE = "sms_balance";
    private static final String KEY_WORKERS_JSON = "workers_json";

    // Sales Tracking Keys
    private static final String KEY_DAILY_SALES = "daily_sales_amount";
    private static final String KEY_DAILY_COUNT = "daily_sales_count";
    private static final String KEY_MONTHLY_SALES = "monthly_sales_amount";
    private static final String KEY_LAST_SALE_DATE = "last_sale_date";
    private static final String KEY_LAST_SALE_MONTH = "last_sale_month";

    private final SharedPreferences prefs;

    public static class Worker {
        public String name;
        public String number;

        public Worker(String name, String number) {
            this.name = name;
            this.number = number;
        }
    }

    public PreferencesManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // --- Shop & Security Credentials ---

    public void saveSelectedShop(String shopName) {
        prefs.edit().putString(KEY_SELECTED_SHOP, shopName).apply();
    }

    public String getSelectedShop() {
        return prefs.getString(KEY_SELECTED_SHOP, "Main Branch");
    }

    public void saveShopPin(String pin) {
        prefs.edit().putString(KEY_SHOP_PIN, pin).apply();
    }

    public String getShopPin() {
        return prefs.getString(KEY_SHOP_PIN, null);
    }

    // --- Relay State & SMS Balance ---

    public void setRelayEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_RELAY_ENABLED, enabled).apply();
    }

    public boolean isRelayEnabled() {
        return prefs.getBoolean(KEY_RELAY_ENABLED, false);
    }

    public void setSmsBalance(int balance) {
        prefs.edit().putInt(KEY_SMS_BALANCE, Math.max(0, balance)).apply();
    }

    public int getSmsBalance() {
        return prefs.getInt(KEY_SMS_BALANCE, 100);
    }

    public synchronized void decrementSmsBalance(int count) {
        int current = getSmsBalance();
        setSmsBalance(current - count);
    }

    // --- Multi-Worker Storage Logic ---

    public synchronized void addWorker(String name, String number) {
        List<Worker> currentList = getWorkers();
        for (Worker w : currentList) {
            if (w.number.equals(number)) return;
        }
        currentList.add(new Worker(name, number));
        saveWorkersList(currentList);
    }

    public synchronized void removeWorker(String number) {
        List<Worker> currentList = getWorkers();
        List<Worker> updated = new ArrayList<>();
        for (Worker w : currentList) {
            if (!w.number.equals(number)) {
                updated.add(w);
            }
        }
        saveWorkersList(updated);
    }

    public List<Worker> getWorkers() {
        List<Worker> list = new ArrayList<>();
        String jsonStr = prefs.getString(KEY_WORKERS_JSON, "[]");
        try {
            JSONArray array = new JSONArray(jsonStr);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                list.add(new Worker(obj.getString("name"), obj.getString("number")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void saveWorkersList(List<Worker> list) {
        JSONArray array = new JSONArray();
        for (Worker w : list) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("name", w.name);
                obj.put("number", w.number);
                array.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        prefs.edit().putString(KEY_WORKERS_JSON, array.toString()).apply();
    }

    // --- Sales Tracking Logic ---

    public synchronized void recordSale(double amount) {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());

        String lastDate = prefs.getString(KEY_LAST_SALE_DATE, "");
        String lastMonth = prefs.getString(KEY_LAST_SALE_MONTH, "");

        double dailyTotal = getDailySales();
        int dailyCount = getDailyCount();
        double monthlyTotal = getMonthlySales();

        if (!today.equals(lastDate)) {
            dailyTotal = 0.0;
            dailyCount = 0;
            prefs.edit().putString(KEY_LAST_SALE_DATE, today).apply();
        }

        if (!currentMonth.equals(lastMonth)) {
            monthlyTotal = 0.0;
            prefs.edit().putString(KEY_LAST_SALE_MONTH, currentMonth).apply();
        }

        dailyTotal += amount;
        dailyCount += 1;
        monthlyTotal += amount;

        prefs.edit()
                .putFloat(KEY_DAILY_SALES, (float) dailyTotal)
                .putInt(KEY_DAILY_COUNT, dailyCount)
                .putFloat(KEY_MONTHLY_SALES, (float) monthlyTotal)
                .apply();
    }

    public double getDailySales() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String lastDate = prefs.getString(KEY_LAST_SALE_DATE, "");
        if (!today.equals(lastDate)) return 0.0;
        return prefs.getFloat(KEY_DAILY_SALES, 0.0f);
    }

    public int getDailyCount() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        String lastDate = prefs.getString(KEY_LAST_SALE_DATE, "");
        if (!today.equals(lastDate)) return 0;
        return prefs.getInt(KEY_DAILY_COUNT, 0);
    }

    public double getMonthlySales() {
        String currentMonth = new SimpleDateFormat("yyyy-MM", Locale.US).format(new Date());
        String lastMonth = prefs.getString(KEY_LAST_SALE_MONTH, "");
        if (!currentMonth.equals(lastMonth)) return 0.0;
        return prefs.getFloat(KEY_MONTHLY_SALES, 0.0f);
    }
}