package com.sarisari.storepos;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

/** Register a new item (or edit an existing one) by scanning its barcode. */
public class RegisterActivity extends AppCompatActivity {

    public static final String EXTRA_BARCODE = "barcode";

    private DbHelper db;
    private EditText barcode, name, price, stock;
    private TextView status;
    private Button btnDelete;
    private CheckBox autoScan;

    private final ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) loadBarcode(result.getContents());
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        db = DbHelper.get(this);

        barcode = findViewById(R.id.fieldBarcode);
        name = findViewById(R.id.fieldName);
        price = findViewById(R.id.fieldPrice);
        stock = findViewById(R.id.fieldStock);
        status = findViewById(R.id.status);
        btnDelete = findViewById(R.id.btnDelete);
        autoScan = findViewById(R.id.checkAutoScan);

        Util.onEnter(barcode, this::loadBarcode);
        findViewById(R.id.btnScan).setOnClickListener(v -> openCamera());
        findViewById(R.id.btnGenerate).setOnClickListener(v -> generateCode());
        findViewById(R.id.btnSave).setOnClickListener(v -> save());
        findViewById(R.id.btnClear).setOnClickListener(v -> clearForm());
        btnDelete.setOnClickListener(v -> confirmDelete());

        String preset = getIntent().getStringExtra(EXTRA_BARCODE);
        if (!TextUtils.isEmpty(preset)) loadBarcode(preset);
        else clearForm();
    }

    private void openCamera() {
        scanLauncher.launch(Util.scanOptions(getString(R.string.scan_prompt_register)));
    }

    /** Called after a barcode is scanned or typed: load the item if we already have it. */
    private void loadBarcode(String code) {
        code = code.trim();
        barcode.setText(code);
        Item it = db.getItem(code);
        if (it != null) {
            name.setText(it.name);
            price.setText(Util.plain(it.priceCents));
            stock.setText(String.valueOf(it.stock));
            status.setText(getString(R.string.status_existing, Util.peso(it.priceCents)));
            btnDelete.setVisibility(View.VISIBLE);
            Util.showKeyboard(this, price);
            price.selectAll();
        } else {
            name.setText("");
            price.setText("");
            stock.setText("");
            status.setText(R.string.status_new);
            btnDelete.setVisibility(View.GONE);
            Util.showKeyboard(this, name);
        }
    }

    /** For loose items with no barcode (e.g. candies sold per piece, ice, yelo). */
    private void generateCode() {
        String code;
        do {
            code = "SS" + (System.currentTimeMillis() % 10_000_000_000L);
        } while (db.getItem(code) != null);
        loadBarcode(code);
    }

    private void save() {
        String code = barcode.getText().toString().trim();
        String n = name.getText().toString().trim();
        long cents = Util.parsePrice(price.getText().toString());
        String stockText = stock.getText().toString().trim();

        if (code.isEmpty()) { barcode.setError(getString(R.string.err_barcode)); barcode.requestFocus(); return; }
        if (n.isEmpty()) { name.setError(getString(R.string.err_name)); name.requestFocus(); return; }
        if (cents < 0) { price.setError(getString(R.string.err_price)); price.requestFocus(); return; }
        int qty = 0;
        if (!stockText.isEmpty()) {
            try { qty = Integer.parseInt(stockText); }
            catch (NumberFormatException e) { stock.setError(getString(R.string.err_stock)); stock.requestFocus(); return; }
        }

        db.upsert(new Item(code, n, cents, qty));
        Toast.makeText(this, getString(R.string.saved, n, Util.peso(cents)), Toast.LENGTH_SHORT).show();
        clearForm();
        if (autoScan.isChecked()) openCamera();
    }

    private void confirmDelete() {
        String code = barcode.getText().toString().trim();
        Item it = db.getItem(code);
        if (it == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, it.name))
                .setPositiveButton(R.string.delete, (d, w) -> {
                    db.delete(code);
                    Toast.makeText(this, R.string.deleted, Toast.LENGTH_SHORT).show();
                    clearForm();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void clearForm() {
        barcode.setText("");
        name.setText("");
        price.setText("");
        stock.setText("");
        barcode.setError(null); name.setError(null); price.setError(null); stock.setError(null);
        status.setText(R.string.status_ready);
        btnDelete.setVisibility(View.GONE);
        Util.hideKeyboard(this);
        barcode.requestFocus(); // ready for a USB/Bluetooth scanner
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
