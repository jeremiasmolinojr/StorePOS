package com.sarisari.storepos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * StorePOS 2.
 *
 * The store screens (Sell, Items, Stock, Utang, Money, Reports) are a web app bundled in
 * assets/www/index.html — the same code as the Prototype — shown full-screen in a WebView.
 * This activity gives that page the things a web page cannot do by itself:
 *
 *   scan()        camera barcode scanning (ZXing, works offline)
 *   takePhoto()   item photos from the camera, saved privately in the app
 *   save/load     all store data in one file, files/state.json, written atomically
 *   backups       save and open backup files through Android's file picker
 *   legacyItems() items registered in StorePOS 1.x, imported once on first launch
 *
 * The page calls these through window.StorePOSBridge. Results come back through
 * window.StorePOSNative.onScan / onPhoto / onBackupLoaded / onBackupSaved.
 */
public class MainActivity extends AppCompatActivity {

    private static final String STATE_FILE = "state.json";
    private static final String PREVIOUS_FILE = "state.previous.json";

    private WebView web;
    private View topBar;
    private View bottomBar;

    private String scanRequest;
    private String photoRequest;
    private String restoreRequest;
    private String backupJson;

    private final ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                String id = scanRequest;
                scanRequest = null;
                if (id != null) callJs("onScan", id, result.getContents());
            });

    private final ActivityResultLauncher<Void> photoLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicturePreview(), bitmap -> {
                String id = photoRequest;
                photoRequest = null;
                if (id != null) callJs("onPhoto", id, bitmap == null ? null : savePhoto(bitmap));
            });

    private final ActivityResultLauncher<String> cameraPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (Boolean.TRUE.equals(granted)) {
                    launchPhoto();
                } else {
                    String id = photoRequest;
                    photoRequest = null;
                    if (id != null) callJs("onPhoto", id, null);
                    toast("Allow camera access to take item photos.");
                }
            });

    private final ActivityResultLauncher<String> backupSaver =
            registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                String json = backupJson;
                backupJson = null;
                if (uri == null) return; // cancelled
                boolean ok = false;
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    if (os != null && json != null) {
                        os.write(json.getBytes(StandardCharsets.UTF_8));
                        ok = true;
                    }
                } catch (Exception e) {
                    ok = false;
                }
                callJsRaw("window.StorePOSNative&&StorePOSNative.onBackupSaved(" + ok + ")");
            });

    private final ActivityResultLauncher<String[]> backupOpener =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                String id = restoreRequest;
                restoreRequest = null;
                if (id == null) return;
                String text = null;
                if (uri != null) {
                    try (InputStream is = getContentResolver().openInputStream(uri)) {
                        if (is != null) text = readAll(is);
                    } catch (Exception e) {
                        toast("Could not read that file.");
                    }
                }
                callJs("onBackupLoaded", id, text);
            });

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Draw edge-to-edge on every Android version, then pad for the system bars ourselves.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
        setContentView(R.layout.activity_main);

        topBar = findViewById(R.id.topBar);
        bottomBar = findViewById(R.id.bottomBar);
        web = findViewById(R.id.web);

        boolean night = (getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        WindowInsetsControllerCompat bars =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        bars.setAppearanceLightStatusBars(night);     // light green bar in dark mode -> dark icons
        bars.setAppearanceLightNavigationBars(!night); // white tab bar in light mode -> dark icons

        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets sys = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            int ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            setHeight(topBar, sys.top);
            // When the keyboard is open, shrink the page above it instead of covering fields.
            setHeight(bottomBar, Math.max(sys.bottom, ime));
            v.setPadding(sys.left, 0, sys.right, 0);
            return WindowInsetsCompat.CONSUMED;
        });

        keepPreviousCopy();

        web.setBackgroundColor(ContextCompat.getColor(this, R.color.page_bg));
        WebSettings ws = web.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true); // item photos are files in this app's private storage
        ws.setAllowContentAccess(false);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return true; // the store screens never navigate away
            }
        });
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(), "StorePOSBridge");
        web.loadUrl("file:///android_asset/www/index.html");

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Back closes an open panel, then returns to Sell, then leaves the app.
                web.evaluateJavascript("(window.StorePOSBack&&window.StorePOSBack())?'1':'0'", value -> {
                    if (value == null || !value.contains("1")) finish();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.removeJavascriptInterface("StorePOSBridge");
            web.destroy();
            web = null;
        }
        super.onDestroy();
    }

    /** Methods the page can call. They run on a background thread, never the UI thread. */
    private class Bridge {

        @JavascriptInterface
        public String loadState() {
            return readState();
        }

        @JavascriptInterface
        public void saveState(String json) {
            writeState(json);
        }

        @JavascriptInterface
        public String legacyItems() {
            JSONArray arr = new JSONArray();
            try {
                if (!getDatabasePath("storepos.db").exists()) return "[]";
                for (Item it : DbHelper.get(MainActivity.this).search("")) {
                    JSONObject o = new JSONObject();
                    o.put("barcode", it.barcode);
                    o.put("name", it.name);
                    o.put("price", it.priceCents);
                    o.put("stock", it.stock);
                    arr.put(o);
                }
            } catch (Exception e) {
                return "[]";
            }
            return arr.toString();
        }

        @JavascriptInterface
        public void scan(String id, String prompt) {
            runOnUiThread(() -> {
                scanRequest = id;
                ScanOptions o = new ScanOptions();
                o.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES);
                o.setPrompt(prompt == null ? "" : prompt);
                o.setBeepEnabled(true);
                o.setOrientationLocked(false);
                try {
                    scanLauncher.launch(o);
                } catch (ActivityNotFoundException e) {
                    scanRequest = null;
                    callJs("onScan", id, null);
                    toast("The scanner could not start.");
                }
            });
        }

        @JavascriptInterface
        public void takePhoto(String id) {
            runOnUiThread(() -> {
                photoRequest = id;
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
                    launchPhoto();
                } else {
                    cameraPermission.launch(Manifest.permission.CAMERA);
                }
            });
        }

        @JavascriptInterface
        public void deletePhoto(String url) {
            try {
                File dir = photoDir().getCanonicalFile();
                String path = Uri.parse(url).getPath();
                if (path == null) return;
                File f = new File(path).getCanonicalFile();
                if (dir.equals(f.getParentFile())) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            } catch (Exception ignored) {
                // a photo that cannot be removed is harmless
            }
        }

        @JavascriptInterface
        public void saveBackup(String json, String fileName) {
            runOnUiThread(() -> {
                backupJson = json;
                try {
                    backupSaver.launch(fileName);
                } catch (ActivityNotFoundException e) {
                    backupJson = null;
                    toast("No app is available to save files.");
                }
            });
        }

        @JavascriptInterface
        public void openBackup(String id) {
            runOnUiThread(() -> {
                restoreRequest = id;
                try {
                    backupOpener.launch(new String[]{"application/json", "text/plain", "application/octet-stream"});
                } catch (ActivityNotFoundException e) {
                    restoreRequest = null;
                    toast("No app is available to open files.");
                }
            });
        }
    }

    private void launchPhoto() {
        try {
            photoLauncher.launch(null);
        } catch (ActivityNotFoundException e) {
            String id = photoRequest;
            photoRequest = null;
            if (id != null) callJs("onPhoto", id, null);
            toast("This phone has no camera app for photos.");
        }
    }

    // ---------- storage ----------

    private synchronized String readState() {
        File f = new File(getFilesDir(), STATE_FILE);
        if (!f.exists()) return "";
        try (FileInputStream in = new FileInputStream(f)) {
            return readAll(in);
        } catch (IOException e) {
            return "";
        }
    }

    /** Write to a temporary file, then rename over the old one, so a crash never leaves half a file. */
    private synchronized void writeState(String json) {
        if (json == null || json.isEmpty()) return;
        File dir = getFilesDir();
        File tmp = new File(dir, STATE_FILE + ".tmp");
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(json.getBytes(StandardCharsets.UTF_8));
            out.getFD().sync();
        } catch (IOException e) {
            toast("Could not save. Is the phone storage full?");
            return;
        }
        if (!tmp.renameTo(new File(dir, STATE_FILE))) {
            toast("Could not save the store data.");
        }
    }

    /** Keep a copy of the data as it was when the app last opened, as a safety net. */
    private void keepPreviousCopy() {
        File src = new File(getFilesDir(), STATE_FILE);
        if (!src.exists()) return;
        File dst = new File(getFilesDir(), PREVIOUS_FILE);
        try (FileInputStream in = new FileInputStream(src);
             FileOutputStream out = new FileOutputStream(dst)) {
            byte[] b = new byte[16384];
            int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
        } catch (IOException ignored) {
            // the main file is untouched either way
        }
    }

    private File photoDir() {
        File d = new File(getFilesDir(), "photos");
        if (!d.exists()) {
            //noinspection ResultOfMethodCallIgnored
            d.mkdirs();
        }
        return d;
    }

    private String savePhoto(Bitmap bitmap) {
        File f = new File(photoDir(), UUID.randomUUID() + ".jpg");
        try (FileOutputStream out = new FileOutputStream(f)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (IOException e) {
            toast("Could not save the photo.");
            return null;
        }
        return Uri.fromFile(f).toString();
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] b = new byte[16384];
        int n;
        while ((n = in.read(b)) != -1) buf.write(b, 0, n);
        return new String(buf.toByteArray(), StandardCharsets.UTF_8);
    }

    // ---------- helpers ----------

    /** Calls StorePOSNative.fn(arg1, arg2, ...) in the page; null arguments become JavaScript null. */
    private void callJs(String fn, String... args) {
        StringBuilder js = new StringBuilder("window.StorePOSNative&&StorePOSNative.").append(fn).append('(');
        for (int i = 0; i < args.length; i++) {
            if (i > 0) js.append(',');
            js.append(args[i] == null ? "null" : JSONObject.quote(args[i]));
        }
        js.append(')');
        callJsRaw(js.toString());
    }

    private void callJsRaw(String script) {
        runOnUiThread(() -> {
            if (web != null) web.evaluateJavascript(script, null);
        });
    }

    private static void setHeight(View v, int h) {
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp.height != h) {
            lp.height = h;
            v.setLayoutParams(lp);
        }
    }

    private void toast(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
    }
}
