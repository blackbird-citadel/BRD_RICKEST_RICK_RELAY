package com.brd.relay.receivers;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.brd.relay.config.PreferencesManager;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsInterceptReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsInterceptReceiver";
    public static final String ACTION_REFRESH_METRICS = "com.brd.relay.REFRESH_METRICS";

    @Override
    public void onReceive(Context context, Intent intent) {
        PreferencesManager prefs = new PreferencesManager(context);

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        String format = bundle.getString("format");

        for (Object pdu : pdus) {
            SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu, format);
            if (message == null) continue;

            String sender = message.getDisplayOriginatingAddress();
            String body = message.getMessageBody();

            if (sender == null || body == null) continue;

            String upperBody = body.toUpperCase();
            String upperSender = sender.toUpperCase();

            // 1. Process SMS Bundle Top-up Confirmations from Safaricom / Carrier
            if (upperSender.contains("SAFARICOM") || upperSender.contains("180") || 
                upperSender.contains("456") || upperSender.contains("MPESA") ||
                upperBody.contains("SMS") || upperBody.contains("BUNDLE")) {
                processBundleTopUp(context, prefs, body);
            }

            // 2. Intercept Pochi la Biashara & M-PESA Payment Messages
            if (prefs.isRelayEnabled()) {
                if (upperSender.contains("MPESA") || upperBody.contains("POCHI") || 
                    upperBody.contains("RECEIVED KSH") || upperBody.contains("KSH") && upperBody.contains("RECEIVED")) {
                    processAndForwardPayment(context, prefs, body);
                }
            }
        }
    }

    private void processAndForwardPayment(Context context, PreferencesManager prefs, String body) {
        List<PreferencesManager.Worker> workers = prefs.getWorkers();
        if (workers.isEmpty()) {
            Log.w(TAG, "No workers enrolled to relay SMS to.");
            return;
        }

        // Check remaining balance
        if (prefs.getSmsBalance() <= 0) {
            Log.e(TAG, "SMS Balance depleted. Cannot relay message.");
            return;
        }

        // --- Pochi la Biashara & M-PESA Extraction Patterns ---
        String txId = extractPattern(body, "^([A-Z0-9]{10,12})\\s+Confirmed");
        
        // Match both: "received Ksh1,500.00" AND "Ksh1,500.00 received"
        String rawAmount = extractPattern(body, "received\\s+Ksh\\s*([0-9,]+\\.[0-9]{2})");
        if (rawAmount.isEmpty()) {
            rawAmount = extractPattern(body, "Ksh\\s*([0-9,]+\\.[0-9]{2})\\s+received");
        }
        if (rawAmount.isEmpty()) {
            rawAmount = extractPattern(body, "Ksh\\s*([0-9,]+\\.[0-9]{2})");
        }

        // Extract sender name from Pochi / Paybill / Till formats
        String senderName = extractPattern(body, "from\\s+([A-Za-z0-9\\s]+?)\\s+on\\s+");
        if (senderName.isEmpty()) {
            senderName = extractPattern(body, "from\\s+([A-Za-z0-9\\s]+?)\\.\\s*");
        }

        // Record Sale Amount
        if (!rawAmount.isEmpty()) {
            try {
                double parsedAmount = Double.parseDouble(rawAmount.replace(",", ""));
                prefs.recordSale(parsedAmount);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Failed parsing sale amount: " + rawAmount, e);
            }
        }

        String relayPayload;
        if (!txId.isEmpty() && !rawAmount.isEmpty()) {
            relayPayload = "[POCHI/MPESA] TX: " + txId + " | Amt: Ksh " + rawAmount + " | From: " + senderName.trim();
        } else {
            relayPayload = "[BRD RELAY] " + (body.length() > 140 ? body.substring(0, 140) : body);
        }

        // Dispatch SMS to ALL enrolled workers
        int successfulDispatches = 0;
        for (PreferencesManager.Worker worker : workers) {
            boolean sent = sendSmsWithMultiSimFailover(context, worker.number, relayPayload);
            if (sent) {
                successfulDispatches++;
            }
        }

        if (successfulDispatches > 0) {
            prefs.decrementSmsBalance(successfulDispatches);
            notifyUiToRefresh(context);
        }
    }

    private void processBundleTopUp(Context context, PreferencesManager prefs, String body) {
        // Regex matches formats like: "1000 SMS", "200 SMS", "received 500 SMS"
        Pattern pattern = Pattern.compile("(?:RECEIVED|BOUGHT|CREDITED|YOU HAVE)?\\s*(\\d+)\\s*SMS", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(body);
        if (matcher.find()) {
            try {
                int addedSms = Integer.parseInt(matcher.group(1));
                if (addedSms > 0) {
                    prefs.setSmsBalance(prefs.getSmsBalance() + addedSms);
                    notifyUiToRefresh(context);
                    Log.d(TAG, "Top-up detected! Added " + addedSms + " SMS to balance.");
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private void notifyUiToRefresh(Context context) {
        Intent intent = new Intent(ACTION_REFRESH_METRICS);
        intent.setPackage(context.getPackageName());
        context.sendBroadcast(intent);
    }

    @SuppressLint("MissingPermission")
    private boolean sendSmsWithMultiSimFailover(Context context, String destination, String text) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                SubscriptionManager subManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
                if (subManager != null) {
                    List<SubscriptionInfo> activeSubscriptions = subManager.getActiveSubscriptionInfoList();

                    if (activeSubscriptions != null && !activeSubscriptions.isEmpty()) {
                        for (SubscriptionInfo info : activeSubscriptions) {
                            try {
                                int subId = info.getSubscriptionId();
                                SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(subId);
                                smsManager.sendTextMessage(destination, null, text, null, null);
                                Log.d(TAG, "Relayed via SIM Slot: " + info.getSimSlotIndex() + " to " + destination);
                                return true;
                            } catch (Exception e) {
                                Log.e(TAG, "Failed sending on SIM Slot: " + info.getSimSlotIndex() + ", trying next SIM...", e);
                            }
                        }
                    }
                }
            }

            SmsManager defaultSmsManager = SmsManager.getDefault();
            defaultSmsManager.sendTextMessage(destination, null, text, null, null);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "All SIM dispatch attempts failed for destination: " + destination, e);
            return false;
        }
    }

    private String extractPattern(String text, String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }
}