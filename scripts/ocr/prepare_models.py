"""One-time dependency/model preparation. No user image is read or uploaded."""
import argparse
import easyocr

parser = argparse.ArgumentParser()
parser.add_argument('--model-dir', default=None)
args = parser.parse_args()
easyocr.Reader(['th', 'en'], gpu=False, model_storage_directory=args.model_dir,
               download_enabled=True, verbose=True)
print('Thai/English OCR models are ready. Runtime image reading uses offline mode.')
