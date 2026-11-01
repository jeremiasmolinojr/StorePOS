package com.sarisari.storepos;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private DbHelper db;
    private TextView itemCount;

    private final ActivityResultLauncher<String> exportLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/csv"), this::doExport);

    private final ActivityResultLauncher<String[]> importLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), this::doImport);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        db = DbHelper.get(this);
        itemCount = findViewById(R.id.itemCount);

        findViewById(R.id.btnRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        findViewById(R.id.btnPriceCheck).setOnClickListener(v ->
                startActivity(new Intent(this, PriceCheckActivity.class)));
        findViewById(R.id.btnItems).setOnClickListener(v ->
                startActivity(new Intent(this, ItemListActivity.class)));
        findViewById(R.id.btnExport).setOnClickListener(v -> {
            String stamp = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
            exportLauncher.launch("storepos-backup-" + stamp + ".csv");
        });
        findViewById(R.id.btnImport).setOnClickListener(v ->
                importLauncher.launch(new String[]{"text/*", "application/csv",
                        "application/vnd.ms-excel", "application/octet-stream"}));
    }

    @Override
    protected void onResume() {
        super.onResume();
        itemCount.setText(getString(R.string.item_count, db.count()));
    }

    private void doExport(Uri uri) {
        if (uri == null) return;
        try (OutputStream os = getContentResolver().openOutputStream(uri)) {
            if (os == null) throw new IllegalStateException("Cannot open file");
            int n = CsvBackup.export(db.search(""), os);
            Toast.makeText(this, getString(R.string.export_done, n), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_prefix, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private void doImport(Uri uri) {
        if (uri == null) return;
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) throw new IllegalStateException("Cannot open file");
            List<Item> items = CsvBackup.read(is);
            for (Item it : items) db.upsert(it);
            Toast.makeText(this, getString(R.string.import_done, items.size()), Toast.LENGTH_LONG).show();
            onResume();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.error_prefix, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }
}
