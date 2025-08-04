import difflib
import json
import re
import requests
import pytesseract
from pdf2image import convert_from_bytes
import os

os.environ["OMP_THREAD_LIMIT"] = "1"

def read_json(file_path):
    with open(file_path, 'r') as file:
        return json.load(file)

def download_station_pdf(station_code):
    url = f"https://www.mtr.com.hk/archive/ch/services/layouts/{station_code.lower()}.pdf"
    response = requests.get(url)
    if response.status_code == 200:
        return response.content
    else:
        print(f"Failed to download {station_code}.pdf (HTTP {response.status_code})")
        return None

def extract_text_from_pdf_bytes(pdf_bytes):
    images = convert_from_bytes(pdf_bytes, dpi=421)
    full_text = ""
    for i, image in enumerate(images):
        text = pytesseract.image_to_string(image, lang="eng")
        full_text += f"\n--- Page {i+1} ---\n{text}"
    return full_text

def parse_station_info(text, facilities, threshold=0.85):
    bits = 0
    text = re.sub(r"[^a-zA-Z0-9\s]", "", text.lower())  # remove punctuation
    words = text.split()
    max_len = max(len(fac.split()) for fac in facilities)
    ngrams = [" ".join(words[i:i + n]) for n in range(1, max_len + 1) for i in range(len(words) - n + 1)]
    for index, facility in enumerate(facilities):
        facility_lower = facility.lower()
        for ngram in ngrams:
            similarity = difflib.SequenceMatcher(None, facility_lower, ngram).ratio()
            if similarity >= threshold:
                bits |= (1 << index)
                break
    return bits

def collect_mtr_station_facilities(station_code, facilities):
    print(f"Extracting station facilities for {station_code}")
    pdf_bytes = download_station_pdf(station_code)
    if not pdf_bytes:
        return 0
    text = extract_text_from_pdf_bytes(pdf_bytes)
    station_info = parse_station_info(text, facilities)
    print(f"Result for {station_code}: {station_info}")
    return station_info

def collect_mtr_station_facilities_mapping():
    return read_json("mtr_station_facilities.json")["Facilities"]

def decode_facility_mask(bits, facilities):
    return [facility for idx, facility in enumerate(facilities) if (bits >> idx) & 1]

if __name__ == "__main__":
    facilities_list = collect_mtr_station_facilities_mapping()
    print(decode_facility_mask(collect_mtr_station_facilities("TSY", facilities_list), facilities_list))