"""Read exactly one supplied local image. Runtime model/network downloads are disabled."""
import argparse
import json
import os
import re
from pathlib import Path
import socket
import sys


def price_pair(text):
    """Only explicit price/deposit rows can be reconciled; never infer missing digits."""
    marker = re.search(r'มั?ดจ(?:ำ|ํา|า)|deposit', text.replace(' ', ''), re.I)
    compact = text.replace(' ', '')
    if not marker:
        return None
    before = re.findall(r'\d+(?:,\d{3})*(?:\.\d{1,2})?', compact[:marker.start()])
    after = re.findall(r'\d+(?:,\d{3})*(?:\.\d{1,2})?', compact[marker.end():])
    if not before or not after:
        return None
    return tuple(float(value.replace(',', '')) for value in (before[-1], after[0]))


def retry_conflicting_prices(reader, result, source, scale):
    """Re-read only conflicting rows at a different size, using supplied pixels only."""
    import numpy as np
    from PIL import Image
    revised = list(result)
    for index, (box, text, confidence) in enumerate(result):
        amounts = price_pair(text)
        uncertain_heading = confidence < 0.65 and re.search(r'\bver\.?\s*$', text, re.I)
        if not uncertain_heading and (not amounts or amounts[0] >= amounts[1]):
            continue
        left = max(0, int(min(point[0] for point in box) / scale) - 8)
        top = max(0, int(min(point[1] for point in box) / scale) - 8)
        right = min(source.width, int(max(point[0] for point in box) / scale) + 9)
        bottom = min(source.height, int(max(point[1] for point in box) / scale) + 9)
        crop = source.crop((left, top, right, bottom))
        enlargement = 3 if uncertain_heading else 1.5
        crop = crop.resize((round(crop.width * enlargement), round(crop.height * enlargement)), Image.Resampling.LANCZOS)
        candidates = lines_from_boxes(reader.readtext(np.asarray(crop), detail=1, paragraph=False, canvas_size=1800))
        if uncertain_heading:
            valid = [row for row in candidates if re.search(r'\bver\.?\s*$', row['text'], re.I)
                     and row['confidence'] > confidence + 0.1]
            if len(valid) == 1:
                revised[index] = (box, valid[0]['text'], valid[0]['confidence'])
            continue
        valid = [row for row in candidates if (pair := price_pair(row['text']))
                 and pair[1] == amounts[1] and pair[0] >= pair[1]]
        if len(valid) == 1:
            revised[index] = (box, valid[0]['text'], valid[0]['confidence'])
    return revised


def lines_from_boxes(boxes):
    rows = []
    for box, text, confidence in sorted(boxes, key=lambda item: min(p[1] for p in item[0])):
        # Mixed Thai/English price rows often have low aggregate confidence despite usable
        # numbers. Keep the text for conservative parsing and the user's editable review.
        if not text.strip():
            continue
        top, bottom = min(p[1] for p in box), max(p[1] for p in box)
        center, height = (top + bottom) / 2, max(bottom - top, 1)
        row = next((r for r in reversed(rows)
                    if abs(r['center'] - center) <= 0.45 * min(r['height'], height)), None)
        word = (min(p[0] for p in box), str(text).strip(), float(confidence), max(p[0] for p in box), height)
        if row is None:
            rows.append({'center': center, 'height': height, 'words': [word]})
        else:
            row['words'].append(word)
    lines = []
    for row in sorted(rows, key=lambda r: r['center']):
        segments = []
        for word in sorted(row['words']):
            # Separate poster columns instead of joining gift descriptions on the left
            # into version headings / prices on the right at the same vertical position.
            if not segments or word[0] - segments[-1][-1][3] > max(28, 1.5 * min(word[4], segments[-1][-1][4])):
                segments.append([])
            segments[-1].append(word)
        for segment in segments:
            lines.append({'text': ' '.join(word[1] for word in segment),
                          'confidence': min(word[2] for word in segment),
                          'left': segment[0][0], 'right': segment[-1][3], 'center': row['center']})
    # Two independent price columns must be read top-to-bottom per column,
    # rather than interleaving shops and prices at the same y coordinate.
    occupied = []
    for line in sorted(lines, key=lambda line: line['left']):
        if occupied and line['left'] <= occupied[-1][1]:
            occupied[-1][1] = max(occupied[-1][1], line['right'])
        else:
            occupied.append([line['left'], line['right']])
    gutters = []
    for left, right in zip(occupied, occupied[1:]):
        gap = right[0] - left[1]
        if gap < 28:
            continue
        cut = (left[1] + right[0]) / 2
        before = sum(price_pair(line['text']) is not None for line in lines if line['right'] < cut)
        after = sum(price_pair(line['text']) is not None for line in lines if line['left'] > cut)
        if before >= 2 and after >= 2:
            gutters.append((gap, cut))
    if gutters:
        cut = max(gutters)[1]
        lines.sort(key=lambda line: (line['left'] > cut, line['center'], line['left']))
    return [{'text': line['text'], 'confidence': line['confidence']} for line in lines]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--input', required=True)
    parser.add_argument('--output', required=True)
    parser.add_argument('--model-dir', default='')
    parser.add_argument('--progress', default='')
    args = parser.parse_args()
    destination = Path(args.output)
    def progress(value):
        if args.progress:
            Path(args.progress).write_text(str(value), encoding='utf-8')
    try:
        import easyocr
        import numpy as np
        from PIL import Image
        import torch
        progress(30)
    except ImportError:
        destination.write_text(json.dumps({'error': 'not_ready'}), encoding='utf-8')
        return 2

    # Enforce offline processing even if a dependency unexpectedly attempts a network call.
    def offline(*_args, **_kwargs):
        raise OSError('Network access is disabled for local OCR')
    socket.socket.connect = offline
    socket.socket.connect_ex = offline
    socket.create_connection = offline

    try:
        torch.set_num_threads(min(4, os.cpu_count() or 1))
        options = {'gpu': False, 'download_enabled': False, 'verbose': False}
        if args.model_dir:
            options['model_storage_directory'] = args.model_dir
        reader = easyocr.Reader(['th', 'en'], **options)
        progress(40)
    except (FileNotFoundError, RuntimeError, ValueError, OSError):
        destination.write_text(json.dumps({'error': 'not_ready'}), encoding='utf-8')
        return 2

    try:
        with Image.open(Path(args.input)) as image:
            image = image.convert('RGB')
            source = image.copy()
            # Small crops otherwise keep text too small for reliable detection. Enlarge only
            # the supplied pixels; never restore any content outside the selected region.
            scale = min(3.0, 1200 / min(image.size), 1800 / max(image.size))
            if scale > 1:
                image = image.resize((round(image.width * scale), round(image.height * scale)), Image.Resampling.LANCZOS)
            pixels = np.asarray(image)
        progress(50)
        result = reader.readtext(pixels, detail=1, paragraph=False, canvas_size=1800)
        progress(80)
        result = retry_conflicting_prices(reader, result, source, max(scale, 1))
        progress(90)
        rows = lines_from_boxes(result)
        destination.write_text(json.dumps({'lines': rows}, ensure_ascii=False), encoding='utf-8')
        return 0
    except Exception:
        destination.write_text(json.dumps({'error': 'unreadable'}), encoding='utf-8')
        return 3


if __name__ == '__main__':
    sys.exit(main())
