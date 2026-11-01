package com.sarisari.storepos;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/** All registered items, searchable. Tap an item to edit it. */
public class ItemListActivity extends AppCompatActivity {

    private DbHelper db;
    private EditText search;
    private TextView summary;
    private ItemAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_list);
        Util.setupScreen(this, true);
        db = DbHelper.get(this);

        search = findViewById(R.id.fieldSearch);
        summary = findViewById(R.id.summary);
        ListView list = findViewById(R.id.list);
        list.setEmptyView(findViewById(R.id.empty));
        adapter = new ItemAdapter(this);
        list.setAdapter(adapter);
        list.setOnItemClickListener((p, v, pos, id) -> {
            Intent i = new Intent(this, RegisterActivity.class);
            i.putExtra(RegisterActivity.EXTRA_BARCODE, adapter.getItem(pos).barcode);
            startActivity(i);
        });

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { refresh(); }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<Item> items = db.search(search.getText().toString());
        adapter.setItems(items);
        summary.setText(getString(R.string.list_summary, items.size(), db.count()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
