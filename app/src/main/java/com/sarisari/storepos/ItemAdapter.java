package com.sarisari.storepos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter extends BaseAdapter {
    private final LayoutInflater inflater;
    private final Context ctx;
    private List<Item> items = new ArrayList<>();

    public ItemAdapter(Context ctx) {
        this.ctx = ctx;
        this.inflater = LayoutInflater.from(ctx);
    }

    public void setItems(List<Item> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override public int getCount() { return items.size(); }
    @Override public Item getItem(int position) { return items.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView != null ? convertView
                : inflater.inflate(R.layout.list_item, parent, false);
        Item it = items.get(position);
        ((TextView) v.findViewById(R.id.rowName)).setText(it.name);
        ((TextView) v.findViewById(R.id.rowDetail)).setText(
                ctx.getString(R.string.row_detail, it.barcode, it.stock));
        ((TextView) v.findViewById(R.id.rowPrice)).setText(Util.peso(it.priceCents));
        return v;
    }
}
