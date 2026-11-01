package com.sarisari.storepos;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** Local SQLite database stored on the phone itself (no internet, no server). */
public class DbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "storepos.db";
    private static final int DB_VERSION = 1;
    private static final String T = "items";

    private static DbHelper instance;

    public static synchronized DbHelper get(Context ctx) {
        if (instance == null) instance = new DbHelper(ctx.getApplicationContext());
        return instance;
    }

    private DbHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T + " ("
                + "barcode TEXT PRIMARY KEY NOT NULL,"
                + "name TEXT NOT NULL,"
                + "price_cents INTEGER NOT NULL DEFAULT 0,"
                + "stock INTEGER NOT NULL DEFAULT 0,"
                + "updated_at INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_items_name ON " + T + "(name COLLATE NOCASE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // Future schema changes go here. Never drop the table: it holds the store's data.
    }

    public Item getItem(String barcode) {
        try (Cursor c = getReadableDatabase().query(T, null, "barcode = ?",
                new String[]{barcode}, null, null, null)) {
            return c.moveToFirst() ? read(c) : null;
        }
    }

    /** Insert a new item, or update it if the barcode already exists. */
    public void upsert(Item it) {
        ContentValues v = new ContentValues();
        v.put("barcode", it.barcode);
        v.put("name", it.name);
        v.put("price_cents", it.priceCents);
        v.put("stock", it.stock);
        v.put("updated_at", System.currentTimeMillis());
        getWritableDatabase().insertWithOnConflict(T, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void delete(String barcode) {
        getWritableDatabase().delete(T, "barcode = ?", new String[]{barcode});
    }

    /** Search by name or barcode. Empty query returns everything, A to Z. */
    public List<Item> search(String query) {
        String q = query == null ? "" : query.trim();
        String where = null;
        String[] args = null;
        if (!q.isEmpty()) {
            String like = "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
            where = "name LIKE ? ESCAPE '\\' OR barcode LIKE ? ESCAPE '\\'";
            args = new String[]{like, like};
        }
        List<Item> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query(T, null, where, args, null, null,
                "name COLLATE NOCASE ASC")) {
            while (c.moveToNext()) out.add(read(c));
        }
        return out;
    }

    public long count() {
        return DatabaseUtils.queryNumEntries(getReadableDatabase(), T);
    }

    private static Item read(Cursor c) {
        Item it = new Item();
        it.barcode = c.getString(c.getColumnIndexOrThrow("barcode"));
        it.name = c.getString(c.getColumnIndexOrThrow("name"));
        it.priceCents = c.getLong(c.getColumnIndexOrThrow("price_cents"));
        it.stock = c.getInt(c.getColumnIndexOrThrow("stock"));
        it.updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at"));
        return it;
    }
}
