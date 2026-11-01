# Local image import

The product form lets the user draw, move and resize a crop over the selected PNG/JPEG.
The crop can also be adjusted using pixel fields or the keyboard (arrows move, Shift +
arrows resize). A preview shows the exact region to be read; "use full image" resets it.
The browser encodes only the selected pixels and uploads that cropped image, never the
original file or pixels outside the crop. Changing the crop invalidates the old OCR draft.
EasyOCR runs as a local
Python process on the machine hosting the application. It reads Thai/English text,
then local rules identify version headings, website groups, selling prices and deposits.
The image and OCR output are temporary and removed after the request. The reader has
no database access, external API, API key or network connection. Uploading never saves
or publishes product data; the user reviews the draft and saves through the existing form.

## Prepare the machine once

Use Python 3.11 or a version supported by PyTorch/EasyOCR:

```powershell
python -m pip install -r scripts/ocr/requirements.txt
python scripts/ocr/prepare_models.py
```

The preparation step downloads open source packages and model weights only. No image is
sent anywhere. To install on a fully offline server, prepare the Python environment and
copy the two models (`craft_mlt_25k.pth`, `thai.pth`) from a prepared machine.

The runtime uses `download_enabled=False` and disables Python socket connections. Missing
dependencies/models produce a readable error; the application does not silently download
anything or fall back to an external OCR service.

## Configuration

The defaults use `python` on PATH and EasyOCR's local model directory (`~/.EasyOCR/model`).
Spring Boot configuration can override:

```properties
shop.ocr.python=C:/path/to/python.exe
shop.ocr.model-directory=C:/path/to/ocr-models
shop.ocr.timeout-seconds=120
```

Environment variables: `SHOP_OCR_PYTHON`, `SHOP_OCR_MODEL_DIRECTORY`, `SHOP_OCR_TIMEOUT_SECONDS`.
For a Linux/container deployment install the same CPU Python dependencies/models in that
environment and set `shop.ocr.python=python3` if needed. The Java-only Docker image does
not install Python or model weights automatically.

## Input and review

- PNG/JPEG, at most 8 MB and 16 megapixels. Reading is limited to one request at a time.
- Crop dimensions are at least 20 × 20 pixels. Coordinates remain tied to the original
  image when the screen is resized. Choose a new file to start with a fresh full-image crop.
- Clear version headings such as `T-SHIRT VER.` or `DESK VER.` work best.
- A crop without a version heading uses `Standard` and retains its price groups for review;
  a heading is not required. A missing website is flagged rather than invented.
- Small crops are enlarged locally before OCR to improve small-text detection. This uses
  only the selected crop; no pixels from outside the crop are read. Explicit "ไม่มี POB"
  conditions are shown as review warnings rather than silently treated as a selectable cover.
- Website names should appear before the version/price. Supported common shop aliases
  include Ktown4u, YES24, FNC STORE, Aladin, Makestar, Plus Chat and Mnet Plus Merch.
- Missing websites/deposits stay empty. A new version does not inherit a previous
  version's websites. Gift quantities are not expanded into sellable variants.
- The user can correct OCR text and parse again without uploading or running OCR again.
- Imported offers are appended; existing website/cover offers and their IDs/prices are
  preserved. Unknown website names have an **Add website** button both in the OCR review
  and in imported price groups. The name is prefilled and editable; saving adds/reuses an
  active website immediately without saving the product. All matching pending groups
  select that website automatically, including when an OCR typo is corrected before saving.
  Name comparison ignores case, spacing and punctuation to avoid creating duplicates.
  Errors keep both the website name and the unsaved product form in place.
- The upload is for reading only. It is not stored as the product photo; that field still
  uses the product image URL.

## Checks

```powershell
.\mvnw.cmd '-Dtest=ProductImageTextParserTest,ProductImageControllerTest,LocalProductImageReaderTest,ProductSetupServiceTest,ProductSetupControllerTest' test
node scripts/test/product-setup-ui.cjs
```

Parser/controller tests do not require Python models or a shop database. An optional
real local OCR smoke test accepts the current selected sample image as an explicit path.

References: [EasyOCR](https://github.com/JaidedAI/EasyOCR),
[Reader options and offline model configuration](https://www.jaided.ai/easyocr/documentation/).
