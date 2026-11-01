package com.sarisari.storepos;

/** One product in the store. Price is kept in centavos to avoid rounding errors. */
public class Item {
    public String barcode;
    public String name;
    public long priceCents;
    public int stock;
    public long updatedAt;

    public Item() {}

    public Item(String barcode, String name, long priceCents, int stock) {
        this.barcode = barcode;
        this.name = name;
        this.priceCents = priceCents;
        this.stock = stock;
    }
}
