package com.sarisari.storepos;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Export / import the item list as a CSV file (opens in Excel or Google Sheets). */
public final class CsvBackup {
    private CsvBackup() {}

    public static int export(List<Item> items, OutputStream os) throws IOException {
        Writer w = new OutputStreamWriter(os, StandardCharsets.UTF_8);
        w.write("barcode,name,price,stock\n");
        for (Item it : items) {
            w.write(esc(it.barcode) + "," + esc(it.name) + ","
                    + Util.plain(it.priceCents) + "," + it.stock + "\n");
        }
        w.flush();
        return items.size();
    }

    /** Returns items parsed from the file. Rows that can't be read are skipped. */
    public static List<Item> read(InputStream is) throws IOException {
        List<Item> out = new ArrayList<>();
        BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        String line;
        boolean first = true;
        while ((line = r.readLine()) != null) {
            if (first) {
                first = false;
                if (line.startsWith("﻿")) line = line.substring(1);
                if (line.toLowerCase().startsWith("barcode")) continue; // header
            }
            if (line.trim().isEmpty()) continue;
            List<String> f = split(line);
            if (f.size() < 3) continue;
            String code = f.get(0).trim();
            String name = f.get(1).trim();
            long price = Util.parsePrice(f.get(2));
            int stock = 0;
            if (f.size() > 3) {
                try { stock = Integer.parseInt(f.get(3).trim()); } catch (NumberFormatException ignored) {}
            }
            if (code.isEmpty() || name.isEmpty() || price < 0) continue;
            out.add(new Item(code, name, price, stock));
        }
        return out;
    }

    private static String esc(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static List<String> split(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') { cur.append('"'); i++; }
                    else quoted = false;
                } else cur.append(ch);
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                out.add(cur.toString()); cur.setLength(0);
            } else cur.append(ch);
        }
        out.add(cur.toString());
        return out;
    }
}
