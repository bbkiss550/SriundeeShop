"""Regression checks for independently reading poster columns, without loading OCR models."""
import importlib.util
from pathlib import Path
import unittest
import sys

sys.dont_write_bytecode = True

spec = importlib.util.spec_from_file_location('local_ocr', Path(__file__).resolve().parents[2] / 'src/main/resources/ocr/read_product_image.py')
ocr = importlib.util.module_from_spec(spec)
spec.loader.exec_module(ocr)


def box(x, right, text, confidence=0.9):
    return ([[x, 10], [right, 10], [right, 30], [x, 30]], text, confidence)


class PosterLayoutTest(unittest.TestCase):
    def test_low_confidence_version_heading_can_be_reread_from_its_own_pixels(self):
        from PIL import Image
        class Reader:
            def readtext(self, pixels, **kwargs):
                return [box(0, 150, 'Light ver.', 0.95)]
        original = [box(10, 110, 'lignt ver.', 0.5)]
        revised = ocr.retry_conflicting_prices(Reader(), original, Image.new('RGB', (200, 100)), 2)
        self.assertEqual(revised[0][1], 'Light ver.')

    def test_independent_price_columns_are_read_in_column_order(self):
        def at(x, right, y, text):
            return ([[x, y], [right, y], [right, y + 20], [x, y + 20]], text, 0.9)
        boxes = [at(10, 200, 10, 'Applemusic'), at(500, 850, 10, 'FANS SHOP / YES24 / Musicplant'),
                 at(10, 350, 45, 'Standard ver.'), at(500, 850, 45, 'Standard ver.'),
                 at(10, 350, 80, 'single 490 (deposit 250)'), at(500, 850, 80, 'single 550 (deposit 250)'),
                 at(10, 350, 115, 'SET(2) 950 (deposit 500)'), at(500, 850, 115, 'SET(2) 1050 (deposit 500)')]
        lines = ocr.lines_from_boxes(boxes)
        self.assertEqual([line['text'] for line in lines], [boxes[i][1] for i in [0, 2, 4, 6, 1, 3, 5, 7]])

    def test_gift_column_does_not_pollute_price_heading(self):
        lines = ocr.lines_from_boxes([box(40, 300, 'special gift'), box(640, 950, 'Weverse Albums ver.')])
        self.assertEqual([line['text'] for line in lines], ['special gift', 'Weverse Albums ver.'])

    def test_price_and_deposit_words_stay_together(self):
        lines = ocr.lines_from_boxes([box(640, 760, 'SET(3)'), box(775, 900, '1,750'), box(910, 1100, '(deposit 900)')])
        self.assertEqual([line['text'] for line in lines], ['SET(3) 1,750 (deposit 900)'])

    def test_low_confidence_price_is_preserved_for_review(self):
        self.assertEqual(ocr.lines_from_boxes([box(640, 1100, 'SET(3) 1,750 (deposit 900)', 0.1)])[0]['text'], 'SET(3) 1,750 (deposit 900)')

    def test_conflicting_price_is_read_again_from_uploaded_pixels(self):
        from PIL import Image
        class Reader:
            def readtext(self, pixels, **kwargs):
                self.shape = pixels.shape
                return [box(0, 100, 'single 590 (deposit 300)')]
        reader = Reader()
        original = [box(10, 110, 'single 90 (deposit 300)', 0.2)]
        revised = ocr.retry_conflicting_prices(reader, original, Image.new('RGB', (200, 100)), 2)
        self.assertEqual(revised[0][1], 'single 590 (deposit 300)')
        self.assertLess(reader.shape[1], 200, 'retry is confined to the conflicting line')

    def test_retry_cannot_replace_price_with_a_different_deposit(self):
        from PIL import Image
        class Reader:
            def readtext(self, pixels, **kwargs):
                return [box(0, 100, 'single 590 (deposit 200)')]
        original = [box(10, 110, 'single 90 (deposit 300)', 0.2)]
        self.assertEqual(ocr.retry_conflicting_prices(Reader(), original, Image.new('RGB', (200, 100)), 2), original)


if __name__ == '__main__':
    unittest.main()
