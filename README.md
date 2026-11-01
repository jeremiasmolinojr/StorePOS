# StorePOS

Offline point-of-sale for a sari-sari store, on one Android phone. No server, no account,
no internet needed.

| Tab | What it does |
| --- | --- |
| **Sell** | Quick-tap grid with item photos, camera or USB/Bluetooth barcode scanning, cart, hold a sale, pay by cash (shows the sukli), GCash/Maya, or utang |
| **Items** | Add items by scanning, take a photo of each, cost price with live margin, reorder level, supplier, tingi (sold per piece from a pack). **Check price** by camera scan or search |
| **Stock** | Low-stock list by supplier, receive deliveries (fare spread into cost, expiry dates), spoiled / taken for the house / shelf count, open a pack, expiry list, movement log. Stock never goes below 0 |
| **Utang** | Who owes what and for how long, statements, record bayad, copy a statement to send |
| **Money** | Open and close the day, what the drawer should hold, paid out, over/short, capital, expenses, net profit |
| **Reports** | Sales and profit by day / 7 / 30 days, busiest hours, what earns, dead stock, cancel a sale |

Staff switch with a 4-digit PIN; helpers cannot see costs or profit. First-launch PINs:
Owner **1234**, Helper **0000** — change them in ⋯ → Settings.

## How the app is built

- `app/src/main/assets/www/index.html` — **all store screens**: one HTML/CSS/JavaScript
  file, the same code as the web Prototype (kept outside this repo, in `../Prototype`).
- `MainActivity.java` — shows that page full-screen and gives it what a page cannot do
  alone: the ZXing camera scanner, item photos from the camera, the data file, backup files
  through Android's file picker, and a one-time import of items from StorePOS 1.x.
- Data lives in the app's private storage: `files/state.json` (written atomically, with
  `state.previous.json` kept from the last launch) and item photos in `files/photos/`.
- `DbHelper.java` / `Item.java` — only read once, to import items saved by version 1.x.

## Build the APK

**GitHub (no installs):** every push builds `StorePOS.apk`. Open the run under Actions,
download the **StorePOS** artifact, unzip.

**Your Mac:** install Android Studio or the command-line SDK, then `./build-apk.sh`.
`StorePOS.apk` appears in this folder.

Keep `keystore/storepos.jks` — every update must be signed with it, or Android refuses to
install it over the existing app.

## Backups

⋯ → Settings → **Save backup file**, at least weekly, to Google Drive or Files. Restore
with **Restore from file**. Backups hold all data; item photos stay on the phone that took them.
