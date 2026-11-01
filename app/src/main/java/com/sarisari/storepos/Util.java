package com.sarisari.storepos;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.journeyapps.barcodescanner.ScanOptions;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Util {
    private Util() {}

    /** 1250 -> "₱12.50" */
    public static String peso(long cents) {
        return String.format(Locale.US, "₱%,.2f", cents / 100.0);
    }

    /** 1250 -> "12.50" (for editing) */
    public static String plain(long cents) {
        return BigDecimal.valueOf(cents).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    /** "12.5" -> 1250. Returns -1 if the text is not a valid price. */
    public static long parsePrice(String text) {
        if (text == null) return -1;
        String s = text.trim().replace(",", "").replace("₱", "");
        if (s.isEmpty()) return -1;
        try {
            BigDecimal bd = new BigDecimal(s);
            if (bd.signum() < 0) return -1;
            return bd.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
        } catch (Exception e) {
            return -1;
        }
    }

    /** Camera scanner settings shared by every screen. */
    public static ScanOptions scanOptions(String prompt) {
        ScanOptions o = new ScanOptions();
        o.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
        o.setPrompt(prompt);
        o.setBeepEnabled(true);
        o.setOrientationLocked(false);
        return o;
    }

    public interface OnCode { void onCode(String code); }

    /**
     * Makes an EditText react to "Enter" — both the on-screen keyboard's Done
     * key and USB/Bluetooth barcode scanners (which type the code then press Enter).
     */
    public static void onEnter(EditText field, OnCode cb) {
        field.setOnEditorActionListener((v, actionId, event) -> {
            boolean enterDown = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            boolean enterUp = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_UP;
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_GO
                    || enterDown) {
                String code = field.getText().toString().trim();
                if (!code.isEmpty()) cb.onCode(code);
                return true;
            }
            return enterUp; // swallow the key-up so focus doesn't jump
        });
    }

    public static void hideKeyboard(Activity a) {
        View v = a.getCurrentFocus();
        if (v == null) v = a.getWindow().getDecorView();
        InputMethodManager imm = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    public static void showKeyboard(Activity a, View field) {
        field.requestFocus();
        InputMethodManager imm = (InputMethodManager) a.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
    }
}
