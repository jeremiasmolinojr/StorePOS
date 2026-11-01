# StorePOS

Offline Android price list / POS for a sari-sari store.

- **Register / Edit Item** – scan a barcode, enter name, price (₱) and stock.
- **Price Check** – scan a barcode or search and pick an item to see its price.
- **All Items** – searchable list, tap to edit.
- **Backup** – export/import CSV.
- Works with the phone camera or a USB/Bluetooth barcode scanner. No internet or server needed.

## Build the APK

**Cloud (no installs):** push this folder to a private GitHub repo → Actions tab →
"Build StorePOS APK" → download the `StorePOS` artifact → `StorePOS.apk`.

**Your PC:** install Android Studio, open this folder once, then run `build-apk.bat`
(Windows) or `./build-apk.sh` (Mac/Linux). `StorePOS.apk` appears in this folder.

Keep `keystore/storepos.jks` — every update must be signed with it.
See the full documentation for usage, backup and troubleshooting.
