package com.sarisari.storepos;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;

import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

/** Scan a barcode or pick an item from the list to see its price. */
public class PriceCheckActivity extends AppCompatActivity {

    private DbHelper db;
    private EditText barcode, search;
    private View resultCard;
    private TextView resultName, resultPrice, resultDetail, notFound;
    private Button btnEdit, btnRegisterNew;
    private ItemAdapter adapter;
    private String lastCode;

    private final ActivityResultLauncher<ScanOptions> scanLauncher =
            registerForActivityResult(new ScanContract(), result -> {
                if (result.getContents() != null) lookup(result.getContents());
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_price_check);
        Util.setupScreen(this, true);
        db = DbHelper.get(this);

        barcode = findViewById(R.id.fieldBarcode);
        search = findViewById(R.id.fieldSearch);
        resultCard = findViewById(R.id.resultCard);
        resultName = findViewById(R.id.resultName);
        resultPrice = findViewById(R.id.resultPrice);
        resultDetail = findViewById(R.id.resultDetail);
        notFound = findViewById(R.id.notFound);
        btnEdit = findViewById(R.id.btnEdit);
        btnRegisterNew = findViewById(R.id.btnRegisterNew);

        ListView list = findViewById(R.id.list);
        adapter = new ItemAdapter(this);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            Util.hideKeyboard(this);
            show(adapter.getItem(pos));
        });

        Util.onEnter(barcode, this::lookup);
        findViewById(R.id.btnScan).setOnClickListener(v ->
                scanLauncher.launch(Util.scanOptions(getString(R.string.scan_prompt_price))));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { refreshList(); }
        });

        btnEdit.setOnClickListener(v -> openRegister(lastCode));
        btnRegisterNew.setOnClickListener(v -> openRegister(lastCode));

        resultCard.setVisibility(View.GONE);
        barcode.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
        if (lastCode != null) {
            Item it = db.getItem(lastCode);
            if (it != null) show(it);
        }
    }

    private void refreshList() {
        adapter.setItems(db.search(search.getText().toString()));
    }

    private void lookup(String code) {
        code = code.trim();
        Item it = db.getItem(code);
        if (it != null) {
            show(it);
        } else {
            lastCode = code;
            resultCard.setVisibility(View.VISIBLE);
            resultName.setVisibility(View.GONE);
            resultPrice.setVisibility(View.GONE);
            resultDetail.setVisibility(View.GONE);
            btnEdit.setVisibility(View.GONE);
            notFound.setVisibility(View.VISIBLE);
            notFound.setText(getString(R.string.not_found, code));
            btnRegisterNew.setVisibility(View.VISIBLE);
        }
        barcode.setText("");      // ready for the next scan
        barcode.requestFocus();
    }

    private void show(Item it) {
        lastCode = it.barcode;
        resultCard.setVisibility(View.VISIBLE);
        resultName.setVisibility(View.VISIBLE);
        resultPrice.setVisibility(View.VISIBLE);
        resultDetail.setVisibility(View.VISIBLE);
        btnEdit.setVisibility(View.VISIBLE);
        notFound.setVisibility(View.GONE);
        btnRegisterNew.setVisibility(View.GONE);
        resultName.setText(it.name);
        resultPrice.setText(Util.peso(it.priceCents));
        resultDetail.setText(getString(R.string.row_detail, it.barcode, it.stock));
    }

    private void openRegister(String code) {
        Intent i = new Intent(this, RegisterActivity.class);
        if (code != null) i.putExtra(RegisterActivity.EXTRA_BARCODE, code);
        startActivity(i);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
