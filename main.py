import concurrent.futures
import copy
import json
import math
import random
import re
import string
import sys
import time
import urllib.request
import zlib

import chardet
import requests
from bs4 import BeautifulSoup
from shapely.geometry import Point, Polygon


def merge(a, b, path=None):
    if path is None:
        path = []
    for key in b:
        if key in a:
            if isinstance(a[key], dict) and isinstance(b[key], dict):
                merge(a[key], b[key], path + [str(key)])
            elif a[key] != b[key]:
                raise Exception('Conflict at ' + '.'.join(path + [str(key)]))
        else:
            a[key] = b[key]


def get_web_json(url, max_retries=1000, delay=5):
    for attempt in range(max_retries):
        try:
            with urllib.request.urlopen(url) as data:
                return json.load(data)
        except Exception as e:
            if attempt < max_retries - 1:
                time.sleep(max(delay * (attempt + 1), delay * 10))
            else:
                raise e
    return None


def get_web_text(url, gzip=True, max_retries=1000, delay=5):
    for attempt in range(max_retries):
        try:
            req = urllib.request.Request(url)
            req.add_header('User-Agent', 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36')
            if gzip:
                req.add_header('Accept-Encoding', 'gzip')
            req.add_header('Accept', 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7')
            req.add_header('Connection', 'keep-alive')
            response = urllib.request.urlopen(req)
            text = response.read()
            encoding = chardet.detect(text)
            if encoding['encoding'] is None:
                decompressed_data = zlib.decompress(text, 16 + zlib.MAX_WBITS)
                return str(decompressed_data)
            else:
                return text.decode(encoding['encoding'])
        except Exception as e:
            if attempt < max_retries - 1:
                time.sleep(max(delay * (attempt + 1), delay * 10))
            else:
                raise e
    return None


def read_json(file_path):
    with open(file_path, 'r') as file:
        return json.load(file)


def haversine(lat1, lon1, lat2, lon2):
    lat1_rad = math.radians(lat1)
    lon1_rad = math.radians(lon1)
    lat2_rad = math.radians(lat2)
    lon2_rad = math.radians(lon2)
    dlat = lat2_rad - lat1_rad
    dlon = lon2_rad - lon1_rad
    a = math.sin(dlat / 2) ** 2 + math.cos(lat1_rad) * math.cos(lat2_rad) * math.sin(dlon / 2) ** 2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1 - a))
    return 6371.0 * c


# ===========================


IS_EXPERIMENTAL = "--experimental" in sys.argv[1:]

DATA_PREFIX = "experimental_" if IS_EXPERIMENTAL else ""

BUS_ROUTE = {}
MTR_BUS_STOP_ALIAS = {}
DATA_SHEET = {}
KMB_SUBSIDIARY_ROUTES = {"LWB": set(), "SUNB": set()}
ROUTE_REMARKS = {}
MTR_DATA = {}
MTR_BARRIER_FREE_MAPPING = {}
LRT_DATA = {}
TRAFFIC_SNAPSHOTS = []
CTB_ETA_STOPS = {}

MISSING_ROUTES = {}

DATA_SHEET_FILE_NAME = DATA_PREFIX + "data.json"
DATA_SHEET_FORMATTED_FILE_NAME = DATA_PREFIX + "data_formatted.json"
DATA_SHEET_FULL_FILE_NAME = DATA_PREFIX + "data_full.json"
DATA_SHEET_FULL_FORMATTED_FILE_NAME = DATA_PREFIX + "data_full_formatted.json"
CHECKSUM_FILE_NAME = DATA_PREFIX + "checksum.md5"
LAST_UPDATED_FILE = DATA_PREFIX + "last_updated.txt"

RECAPITALIZE_KEYWORDS = read_json("recapitalize_keywords.json")

NORMALIZE_CHARS = {
    "／": "/",
    "∕": "/"
}

SUN_BUS_ROUTES = {"331", "331S", "917", "918", "945"}
LWB_AREA = Polygon([
    (22.353035101615237, 114.04468147886547),
    (22.33535147194486, 114.06412468869647),
    (22.188100849108828, 114.06444874219365),
    (22.18870096192713, 113.82594536826676),
    (22.35273539776945, 113.83145427771888)
])
NOT_LWB_PREFIXES = ["PB", "PN"]


# ===========================


def download_and_process_kmb_route():
    global BUS_ROUTE
    url = "https://data.etabus.gov.hk/v1/transport/kmb/route/"
    response = requests.get(url)
    data = response.json()
    kmb_route = data.get("data", [])
    for route_data in kmb_route:
        route = route_data.get("route", "")
        if route not in BUS_ROUTE:
            BUS_ROUTE[route] = {}
        BUS_ROUTE[route]["kmb"] = {
            "orig": {
                "zh": route_data.get("orig_tc"),
                "en": route_data.get("orig_en")
            },
            "dest": {
                "zh": route_data.get("dest_tc"),
                "en": route_data.get("dest_en")
            }
        }


def download_and_process_ctb_route():
    global BUS_ROUTE
    url = "https://rt.data.gov.hk/v2/transport/citybus/route/ctb"
    response = requests.get(url)
    data = response.json()
    ctb_route = data.get("data", [])
    for route_data in ctb_route:
        route = route_data.get("route", "")
        if route not in BUS_ROUTE:
            BUS_ROUTE[route] = {}
        BUS_ROUTE[route]["ctb"] = {
            "orig": {
                "zh": route_data.get("orig_tc"),
                "en": route_data.get("orig_en")
            },
            "dest": {
                "zh": route_data.get("dest_tc"),
                "en": route_data.get("dest_en")
            }
        }


def download_and_process_nlb_route():
    global BUS_ROUTE
    url = "https://rt.data.gov.hk/v2/transport/nlb/route.php?action=list"
    response = requests.get(url)
    data = response.json()
    nlb_route = data.get("routes", [])
    for route_data in nlb_route:
        route = route_data.get("routeNo", "")
        nlb_id = route_data.get("routeId", "")
        name_zh = route_data.get("routeName_c", " \u003E ").split("\u003E")
        name_en = route_data.get("routeName_e", " \u003E ").split("\u003E")
        if route not in BUS_ROUTE:
            BUS_ROUTE[route] = {}
        if "nlb" in BUS_ROUTE[route]:
            if nlb_id not in BUS_ROUTE[route]["nlb"]["nlb_ids"]:
                BUS_ROUTE[route]["nlb"]["nlb_ids"].append(nlb_id)
        else:
            BUS_ROUTE[route]["nlb"] = {
                "nlb_ids": [nlb_id],
                "orig": {
                    "zh": name_zh[0].strip(),
                    "en": name_en[0].strip()
                },
                "dest": {
                    "zh": name_zh[1].strip(),
                    "en": name_en[1].strip()
                }
            }


def download_and_process_mtr_bus_data():
    global BUS_ROUTE
    global DATA_SHEET
    global MTR_BUS_STOP_ALIAS

    stop_id_pattern = re.compile("^[A-Z]?[0-9]{1,3}[A-Z]?-[a-zA-Z]+[0-9]{3}$")
    stops_alias_result = {}

    for stop_id in DATA_SHEET["stopList"].keys():
        if stop_id_pattern.match(stop_id):
            stops_alias_result[stop_id] = [stop_id]

    for key, data in DATA_SHEET["routeList"].items():
        bound = data["bound"]
        if "lrtfeeder" in bound:
            if data["route"] not in BUS_ROUTE:
                BUS_ROUTE[data["route"]] = {}
            BUS_ROUTE[data["route"]]["mtr-bus"] = {
                "orig": data["orig"].copy(),
                "dest": data["dest"].copy()
            }
            direction = bound["lrtfeeder"]
            del bound["lrtfeeder"]
            bound["mtr-bus"] = direction
            if "lrtfeeder" in data["co"]:
                data["co"].remove("lrtfeeder")
                data["co"].append("mtr-bus")
            if "lrtfeeder" in data["stops"]:
                stops = data["stops"]["lrtfeeder"]
                del data["stops"]["lrtfeeder"]
                data["stops"]["mtr-bus"] = stops

    MTR_BUS_STOP_ALIAS = stops_alias_result


def download_and_process_gmb_route():
    global BUS_ROUTE
    url = "https://data.etagmb.gov.hk/route"
    response = requests.get(url)
    data = response.json()
    gmb_route = data.get("data", {}).get("routes", {})
    for region, route_list in gmb_route.items():
        for route in route_list:
            if route not in BUS_ROUTE:
                BUS_ROUTE[route] = {}
            BUS_ROUTE[route]["gmb"] = {
                "orig": {
                    "zh": "未知地點",
                    "en": "Unknown Place"
                },
                "dest": {
                    "zh": "未知地點",
                    "en": "Unknown Place"
                }
            }


def clean_data_sheet(data):
    route_list = data["routeList"]
    route_keys = list(route_list.keys())
    for key in route_keys:
        route_data = route_list[key]
        if "seq" in route_data:
            del route_data["seq"]


def strip_data_sheet(data):
    route_list = data["routeList"]
    route_keys = list(route_list.keys())
    for key in route_keys:
        route_data = route_list[key]
        if "fares" in route_data:
            del route_data["fares"]
        if "faresHoliday" in route_data:
            del route_data["faresHoliday"]


def stop_matches(ctb_stop_id, kmb_stop_id):
    if ctb_stop_id in DATA_SHEET["stopMap"]:
        if ctb_stop_id in DATA_SHEET["stopMap"] and any(
                array[1] == kmb_stop_id for array in
                DATA_SHEET["stopMap"][ctb_stop_id]):
            return True, 0
        if kmb_stop_id in DATA_SHEET["stopMap"] and any(
                array[1] == ctb_stop_id for array in
                DATA_SHEET["stopMap"][kmb_stop_id]):
            return True, 0
    ctb_stop_location = DATA_SHEET["stopList"][ctb_stop_id]["location"]
    kmb_stop_location = DATA_SHEET["stopList"][kmb_stop_id]["location"]
    distance = haversine(ctb_stop_location["lat"], ctb_stop_location["lng"],
                         kmb_stop_location["lat"], kmb_stop_location["lng"])
    return distance <= 0.3, distance


def generate_ctb_route_with_more_stops(ctb_route):
    route_number = ctb_route["route"]
    ctb_stop_ids = ctb_route["stops"]["ctb"]
    route_specific_stop_map = {}
    matching_route = None
    for key, data in DATA_SHEET["routeList"].items():
        if route_number != data["route"] or "kmb" not in data["bound"] or "kmb" not in data["stops"]:
            continue
        first_kmb_stop_id = data["stops"]["kmb"][0]
        last_kmb_stop_id = data["stops"]["kmb"][-1]
        first_stop_matches = False
        last_stop_matches = False
        for ctb_stop_id in ctb_stop_ids[0:min(6, len(ctb_stop_ids) - 1)]:
            if stop_matches(ctb_stop_id, first_kmb_stop_id)[0]:
                first_stop_matches = True
                break
        for ctb_stop_id in reversed(ctb_stop_ids[max(1, len(ctb_stop_ids) - 6):len(ctb_stop_ids)]):
            if stop_matches(ctb_stop_id, last_kmb_stop_id)[0]:
                last_stop_matches = True
                break
        priority = False
        if first_stop_matches and last_stop_matches:
            if matching_route is None or (len(matching_route["stops"]["kmb"]) < len(data["stops"]["kmb"]) or (len(matching_route["stops"]["kmb"]) == len(data["stops"]["kmb"]) and "ctb" in data["stops"])):
                matching_route = data
                priority = True
        if "ctb" in data["stops"]:
            for i in range(min(len(data["stops"]["kmb"]), len(data["stops"]["ctb"]))):
                ctb_stop_id = data["stops"]["ctb"][i]
                kmb_stop_id = data["stops"]["kmb"][i]
                if not re.fullmatch(r'ZZ[A-Z0-9]{8}[0-9]{6}', kmb_stop_id):
                    if ctb_stop_id in route_specific_stop_map:
                        if priority:
                            route_specific_stop_map[ctb_stop_id] = kmb_stop_id
                    else:
                        route_specific_stop_map[ctb_stop_id] = kmb_stop_id
    if matching_route is None:
        return False
    kmb_stop_index = 0
    ctb_stop_index = -1
    generated_kmb_stops = []
    new_stop_maps = []
    new_stop_entries = {}
    while ctb_stop_index < len(ctb_stop_ids) - 1:
        ctb_stop_index += 1
        ctb_stop_id = ctb_stop_ids[ctb_stop_index]
        ctb_next_stop_id = ctb_stop_ids[ctb_stop_index + 1] if ctb_stop_index + 1 < len(ctb_stop_ids) else None
        kmb_stop_id = matching_route["stops"]["kmb"][kmb_stop_index] if kmb_stop_index < len(matching_route["stops"]["kmb"]) else None
        actual_ctb_stop_id = matching_route["stops"]["ctb"][kmb_stop_index] if "ctb" in matching_route["stops"] and kmb_stop_index < len(matching_route["stops"]["ctb"]) else None
        definitely_does_not_match = False if actual_ctb_stop_id is None else actual_ctb_stop_id != ctb_stop_id
        matches, distance = stop_matches(ctb_stop_id, kmb_stop_id) if kmb_stop_id is not None else (False, 0)
        if definitely_does_not_match:
            matches = False
        kmb_stop_offset = 1
        if not matches:
            for offset in range(1, 5):
                offset_kmb_stop_id = matching_route["stops"]["kmb"][kmb_stop_index + offset] if kmb_stop_index + offset < len(matching_route["stops"]["kmb"]) else None
                offset_actual_ctb_stop_id = matching_route["stops"]["ctb"][kmb_stop_index + offset] if "ctb" in matching_route["stops"] and kmb_stop_index + offset < len(matching_route["stops"]["ctb"]) else None
                offset_definitely_does_not_match = False if offset_actual_ctb_stop_id is None else offset_actual_ctb_stop_id != ctb_stop_id
                offset_matches, offset_distance = stop_matches(ctb_stop_id, offset_kmb_stop_id) if offset_kmb_stop_id is not None else (False, 0)
                if offset_definitely_does_not_match:
                    offset_matches = False
                if offset_matches and offset_distance < 0.05 and (ctb_next_stop_id is None or offset_distance < stop_matches(ctb_next_stop_id, kmb_stop_id)[1]):
                    matches, distance = offset_matches, offset_distance
                    kmb_stop_offset += offset
                    break
        if kmb_stop_offset > 1 or (matches and (ctb_next_stop_id is None or distance < stop_matches(ctb_next_stop_id, kmb_stop_id)[1])):
            generated_kmb_stops.append(kmb_stop_id)
            kmb_stop_index += kmb_stop_offset
        else:
            ctb_stop = DATA_SHEET["stopList"][ctb_stop_id]
            if ctb_stop_id in route_specific_stop_map:
                kmb_stop_id = route_specific_stop_map[ctb_stop_id]
            else:
                kmb_stop_id = "ZZ" + generate_numeric_string(ctb_stop_id, 8, "alphanumeric", lambda s: s not in DATA_SHEET["stopList"]) + ctb_stop_id
                new_stop_entries[kmb_stop_id] = ctb_stop.copy()
            generated_kmb_stops.append(kmb_stop_id)
            new_stop_maps.append((ctb_stop_id, kmb_stop_id))
    if kmb_stop_index < len(matching_route["stops"]["kmb"]) - 1:
        return False
    ctb_route["stops"]["kmb"] = generated_kmb_stops
    ctb_route["fakeRoute"] = True
    ctb_route["kmbCtbJoint"] = True
    if "kmb" not in ctb_route["co"]:
        ctb_route["co"].append("kmb")
    if "kmb" not in ctb_route["bound"]:
        ctb_route["bound"]["kmb"] = matching_route["bound"]["kmb"]
    for ctb_stop_id, kmb_stop_id in new_stop_maps:
        ctb_stop_map_entry = ["kmb", kmb_stop_id]
        if ctb_stop_id in DATA_SHEET["stopMap"]:
            if ctb_stop_map_entry not in DATA_SHEET["stopMap"][ctb_stop_id]:
                DATA_SHEET["stopMap"][ctb_stop_id].append(ctb_stop_map_entry)
        else:
            DATA_SHEET["stopMap"][ctb_stop_id] = [ctb_stop_map_entry]
        kmb_stop_map_entry = ["ctb", ctb_stop_id]
        if kmb_stop_id in DATA_SHEET["stopMap"]:
            if kmb_stop_map_entry not in DATA_SHEET["stopMap"][kmb_stop_id]:
                DATA_SHEET["stopMap"][kmb_stop_id].append(kmb_stop_map_entry)
        else:
            DATA_SHEET["stopMap"][kmb_stop_id] = [kmb_stop_map_entry]
    for kmb_stop_id, stop in new_stop_entries.items():
        DATA_SHEET["stopList"][kmb_stop_id] = stop
        DATA_SHEET["stopList"][kmb_stop_id]["remark"] = {
            "en": "(Citybus)",
            "zh": "(城巴)"
        }
    return True


def generate_numeric_string(seed_string, length, charset, predicate):
    random.seed(seed_string)
    if charset == "numeric":
        characters = string.digits
    elif charset == "alphanumeric":
        characters = string.ascii_uppercase + string.digits
    else:
        raise ValueError("Unsupported charset. Use 'numeric' or 'alphanumeric'.")
    while True:
        result = ''.join(random.choice(characters) for _ in range(length))
        if predicate(result):
            return result


def contains(small, big):
    for i in range(len(big)-len(small)+1):
        for j in range(len(small)):
            if big[i+j] != small[j]:
                break
        else:
            return True
    return False


def download_and_process_data_sheet():
    global DATA_SHEET
    global BUS_ROUTE
    url = "https://crawling-data.hkbuseta.com/routeFareList.min.json"
    response = requests.get(url)
    DATA_SHEET = response.json()
    clean_data_sheet(DATA_SHEET)

    DATA_SHEET["stopList"]["AC1FD9BDD09D1DD6"]["remark"] = {"en": "(STK FCA - Closed Area Permit Required)", "zh": "(沙頭角邊境禁區 - 需持邊境禁區許可證)"}
    DATA_SHEET["stopList"]["20001477"]["remark"] = {"en": "(STK FCA - Closed Area Permit Required)", "zh": "(沙頭角邊境禁區 - 需持邊境禁區許可證)"}
    DATA_SHEET["stopList"]["152"]["remark"] = {"en": "(S-Bay Control Point - Border Crossing Passengers Only)", "zh": "(深圳灣管制站 - 僅限過境旅客)"}
    DATA_SHEET["stopList"]["20015453"]["remark"] = {"en": "(S-Bay Control Point - Border Crossing Passengers Only)", "zh": "(深圳灣管制站 - 僅限過境旅客)"}
    DATA_SHEET["stopList"]["003208"]["remark"] = {"en": "(S-Bay Control Point - Border Crossing Passengers Only)", "zh": "(深圳灣管制站 - 僅限過境旅客)"}
    DATA_SHEET["stopList"]["81567ACCCF40DD4B"]["remark"] = {"en": "(LMC SL Immigration Control Point - Border Crossing Passengers Only)", "zh": "(落馬洲支線出入境管制站 - 僅限過境旅客)"}
    DATA_SHEET["stopList"]["20015420"]["remark"] = {"en": "(LMC SL Immigration Control Point - Border Crossing Passengers Only)", "zh": "(落馬洲支線出入境管制站 - 僅限過境旅客)"}
    DATA_SHEET["stopList"]["20011698"]["remark"] = {"en": "(LMC Control Point - Border Crossing Passengers Only)", "zh": "(落馬洲管制站 - 僅限過境旅客)"}
    DATA_SHEET["stopList"]["20015598"]["remark"] = {"en": "(LMC Control Point - Border Crossing Passengers Only)", "zh": "(落馬洲管制站 - 僅限過境旅客)"}
    DATA_SHEET["stopList"]["RAC"] = {
        "location": {"lat": 22.4003487, "lng": 114.2030287},
        "name": {"en": "Racecourse", "zh": "馬場"},
        "remark": {"en": "(Race Days Only)", "zh": "(只限賽馬日)"}
    }
    DATA_SHEET["routeList"]["AEL+1+AsiaWorld-Expo+Hong Kong"]["orig"]["zh"] = "機場及博覽館"
    DATA_SHEET["routeList"]["AEL+1+AsiaWorld-Expo+Hong Kong"]["orig"]["en"] = "Airport & AsiaWorld-Expo"
    DATA_SHEET["routeList"]["AEL+1+AsiaWorld-Expo+Hong Kong"]["dest"]["zh"] = "市區"
    DATA_SHEET["routeList"]["AEL+1+AsiaWorld-Expo+Hong Kong"]["dest"]["en"] = "city"
    DATA_SHEET["routeList"]["AEL+1+Hong Kong+AsiaWorld-Expo"]["orig"]["zh"] = "市區"
    DATA_SHEET["routeList"]["AEL+1+Hong Kong+AsiaWorld-Expo"]["orig"]["en"] = "city"
    DATA_SHEET["routeList"]["AEL+1+Hong Kong+AsiaWorld-Expo"]["dest"]["zh"] = "機場及博覽館"
    DATA_SHEET["routeList"]["AEL+1+Hong Kong+AsiaWorld-Expo"]["dest"]["en"] = "Airport & AsiaWorld-Expo"

    lrt_705_start = DATA_SHEET["routeList"]["705+1+Tin Shui Wai+Tin Shui Wai (Circular)"]["stops"]["lightRail"][0]
    DATA_SHEET["routeList"]["705+1+Tin Shui Wai+Tin Shui Wai (Circular)"]["stops"]["lightRail"].append(lrt_705_start)
    DATA_SHEET["routeList"]["705+1+Tin Shui Wai+Tin Shui Wai (Circular)"]["lrtCircular"] = {"en": "TSW Circular", "zh": "天水圍循環綫"}

    lrt_706_start = DATA_SHEET["routeList"]["706+1+Tin Shui Wai+Tin Shui Wai (Circular)"]["stops"]["lightRail"][0]
    DATA_SHEET["routeList"]["706+1+Tin Shui Wai+Tin Shui Wai (Circular)"]["stops"]["lightRail"].append(lrt_706_start)
    DATA_SHEET["routeList"]["706+1+Tin Shui Wai+Tin Shui Wai (Circular)"]["lrtCircular"] = {"en": "TSW Circular", "zh": "天水圍循環綫"}

    kmb_ops = {}
    ctb_circular = {}
    ctb_circular_ref = {}
    mtr_orig = {}
    mtr_dest = {}
    mtr_stops_lists = {}

    kmb_only_routes = {}
    ctb_only_routes = {}
    for key, data in DATA_SHEET["routeList"].items():
        route_number = data["route"]
        bounds = data["bound"]
        if "kmb" in bounds and "ctb" not in bounds:
            if route_number in kmb_only_routes:
                kmb_only_routes[route_number].append(key)
            else:
                kmb_only_routes[route_number] = [key]
        elif "ctb" in bounds and "kmb" not in bounds:
            if route_number in ctb_only_routes:
                ctb_only_routes[route_number].append(key)
            else:
                ctb_only_routes[route_number] = [key]

    keys_to_remove = []
    for route_number, kmb_route_keys in kmb_only_routes.items():
        if route_number in ctb_only_routes:
            for kmb_route_key in kmb_route_keys:
                kmb_route = DATA_SHEET["routeList"][kmb_route_key]
                for ctb_route_key in ctb_only_routes[route_number]:
                    if ctb_route_key not in keys_to_remove:
                        ctb_route = DATA_SHEET["routeList"][ctb_route_key]
                        if "kmb" in kmb_route["stops"]:
                            if len(kmb_route["stops"]["kmb"]) == len(ctb_route["stops"]["ctb"]):
                                matches = True
                                for i in range(len(ctb_route["stops"]["ctb"])):
                                    ctb_stop_id = ctb_route["stops"]["ctb"][i]
                                    kmb_stop_id = kmb_route["stops"]["kmb"][i]
                                    if ctb_stop_id in DATA_SHEET["stopMap"]:
                                        if ctb_stop_id in DATA_SHEET["stopMap"] and any(
                                                array[1] == kmb_stop_id for array in
                                                DATA_SHEET["stopMap"][ctb_stop_id]):
                                            continue
                                        if kmb_stop_id in DATA_SHEET["stopMap"] and any(
                                                array[1] == ctb_stop_id for array in
                                                DATA_SHEET["stopMap"][kmb_stop_id]):
                                            continue
                                    ctb_stop_location = DATA_SHEET["stopList"][ctb_stop_id]["location"]
                                    kmb_stop_location = DATA_SHEET["stopList"][kmb_stop_id]["location"]
                                    distance = haversine(ctb_stop_location["lat"], ctb_stop_location["lng"],
                                                         kmb_stop_location["lat"], kmb_stop_location["lng"])
                                    if distance > 0.3:
                                        matches = False
                                        break
                                if matches:
                                    keys_to_remove.append(ctb_route_key)
                                    kmb_route["bound"]["ctb"] = ctb_route["bound"]["ctb"]
                                    if "ctb" not in kmb_route["co"]:
                                        kmb_route["co"].append("ctb")
                                    kmb_route["stops"]["ctb"] = ctb_route["stops"]["ctb"]
                                    if "fares" not in kmb_route or kmb_route["fares"] is None:
                                        kmb_route["fares"] = ctb_route["fares"]
                                    if "faresHoliday" not in kmb_route or kmb_route["faresHoliday"] is None:
                                        kmb_route["faresHoliday"] = ctb_route["faresHoliday"]
                                    if "freq" not in kmb_route or kmb_route["freq"] is None:
                                        kmb_route["freq"] = ctb_route["freq"]
                                    if "gtfsId" not in kmb_route or kmb_route["gtfsId"] is None:
                                        kmb_route["gtfsId"] = ctb_route["gtfsId"]
                                    if "jt" not in kmb_route or kmb_route["jt"] is None:
                                        kmb_route["jt"] = ctb_route["jt"]

    for route_key in keys_to_remove:
        if route_key in DATA_SHEET["routeList"]:
            del DATA_SHEET["routeList"][route_key]

    def list_index_of(li, o):
        try:
            return li.index(o)
        except ValueError:
            return -1

    for key, data in DATA_SHEET["routeList"].items():
        bounds = data.get("bound")

        if "jt" in data and data["jt"] is None:
            del data["jt"]

        cos = data.get("co")
        if "hkkf" in bounds and "hkkf" not in cos:
            data["co"] = ["hkkf"]
        if "sunferry" in bounds and "sunferry" not in cos:
            data["co"] = ["sunferry"]
        if "fortuneferry" in bounds and "fortuneferry" not in cos:
            data["co"] = ["fortuneferry"]

        if "lightRail" in bounds:
            if data["route"] not in BUS_ROUTE:
                BUS_ROUTE[data["route"]] = {}
            BUS_ROUTE[data["route"]]["lightRail"] = {
                "orig": data["orig"].copy(),
                "dest": data["dest"].copy()
            }
        elif "mtr" in bounds:
            line_name = data["route"]
            if line_name not in BUS_ROUTE:
                BUS_ROUTE[line_name] = {}
            BUS_ROUTE[line_name]["mtr"] = {
                "orig": data["orig"].copy(),
                "dest": data["dest"].copy()
            }
            bound = bounds.get("mtr")
            index = bound.find("-")
            stops = data["stops"]["mtr"]
            if index >= 0:
                if bound.startswith("LMC-"):
                    stops[stops.index("FOT")] = "RAC"
                bounds["mtr"] = bound = bound[index + 1:]
                data["serviceType"] = "2"
                mtr_orig.setdefault(line_name + "_" + bound, []).append(data.get("orig"))
                mtr_dest.setdefault(line_name + "_" + bound, []).append(data.get("dest"))
            else:
                mtr_orig.setdefault(line_name + "_" + bound, []).insert(0, data.get("orig"))
                mtr_dest.setdefault(line_name + "_" + bound, []).insert(0, data.get("dest"))
            mtr_stops_lists.setdefault(line_name + "_" + bound, []).append([DATA_SHEET["stopList"][s]["name"]["zh"] for s in stops])
        elif "kmb" in bounds:
            if "ctb" in data["co"]:
                data["kmbCtbJoint"] = True
                if data["route"] not in kmb_ops:
                    kmb_ops[data["route"]] = [data]
                else:
                    kmb_ops[data["route"]].append(data)
        elif "ctb" in bounds:
            route_number = data["route"]
            service_type = int(data["serviceType"])
            if len(bounds["ctb"]) > 1:
                stops = data["stops"].get("ctb")
                if stops is not None:
                    if route_number in ctb_circular_ref:
                        ctb_circular_ref[route_number].append(stops)
                    else:
                        ctb_circular_ref[route_number] = [stops]
                    first_stop_id = stops[0]
                    last_stop_id = stops[-1]
                    if first_stop_id != last_stop_id:
                        first_stop = DATA_SHEET["stopList"][first_stop_id]["location"]
                        last_stop = DATA_SHEET["stopList"][last_stop_id]["location"]
                        if haversine(first_stop["lat"], first_stop["lng"], last_stop["lat"],
                                     last_stop["lng"]) > 0.3:
                            data["dest"]["zh"] = data["dest"]["zh"].replace("(循環線)", "").strip()
                            data["dest"]["en"] = data["dest"]["en"].replace("(Circular)", "").strip()
                            bounds["ctb"] = bounds["ctb"][-1]
                            continue
                if route_number in ctb_circular:
                    if abs(ctb_circular[route_number]) >= service_type:
                        ctb_circular[route_number] = service_type
                else:
                    ctb_circular[route_number] = service_type
            elif route_number in ctb_circular:
                if abs(ctb_circular[route_number]) > service_type:
                    ctb_circular[route_number] = -service_type
            else:
                ctb_circular[route_number] = -service_type

    mtr_joined_orig = {}
    for key, values in mtr_orig.items():
        if len(values) > 1:
            orig_zh = []
            orig_en = []
            for orig in values:
                stop_name_zh = orig["zh"]
                if not any(0 < list_index_of(x, stop_name_zh) < len(x) - 1 for x in mtr_stops_lists[key]):
                    orig_zh.append(stop_name_zh)
                    orig_en.append(orig["en"])
            mtr_joined_orig[key] = ("/".join(list(dict.fromkeys(orig_zh))), "/".join(list(dict.fromkeys(orig_en))))

    mtr_joined_dest = {}
    for key, values in mtr_dest.items():
        if len(values) > 1:
            dest_zh = []
            dest_en = []
            for orig in values:
                stop_name_zh = orig.get("zh")
                if not any(0 < list_index_of(x, stop_name_zh) < len(x) - 1 for x in mtr_stops_lists[key]):
                    dest_zh.append(stop_name_zh)
                    dest_en.append(orig["en"])
            mtr_joined_dest[key] = ("/".join(list(dict.fromkeys(dest_zh))), "/".join(list(dict.fromkeys(dest_en))))

    keys_to_remove = []
    for key, data in DATA_SHEET["routeList"].items():
        route_number = data["route"]
        bounds = data["bound"]

        if "mtr" in bounds:
            line_name = route_number
            bound = bounds["mtr"]

            joint_ori = mtr_joined_orig.get(line_name + "_" + bound)
            if joint_ori:
                orig = data["orig"]
                orig["zh"], orig["en"] = joint_ori

            joint_dest = mtr_joined_dest.get(line_name + "_" + bound)
            if joint_dest:
                dest = data["dest"]
                dest["zh"], dest["en"] = joint_dest
        elif "ctb" in bounds:
            if route_number in kmb_ops and "kmb" not in bounds:
                for kmb_data in kmb_ops[route_number]:
                    if kmb_data["serviceType"] == data["serviceType"]:
                        if kmb_data["freq"] is None and data["freq"] is not None:
                            kmb_data["freq"] = data["freq"]
                        if kmb_data["fares"] is None and data["fares"] is not None:
                            kmb_data["fares"] = data["fares"]
                if not generate_ctb_route_with_more_stops(data):
                    keys_to_remove.append(key)
            elif route_number in ctb_circular:
                if ctb_circular[route_number] < 0:
                    if len(bounds["ctb"]) >= 2:
                        bounds["ctb"] = bounds["ctb"][0:1]
                elif len(bounds["ctb"]) < 2:
                    if route_number in ctb_circular_ref and any(contains(data["stops"]["ctb"], s) for s in ctb_circular_ref[route_number]):
                        data["ctbIsCircular"] = True
                    else:
                        data["bound"]["ctb"] = "OI"
                else:
                    data["bound"]["ctb"] = "OI"
                    data["dest"]["zh"] += " (循環線)"
                    data["dest"]["en"] += " (Circular)"
        elif "kmb" in bounds:
            if route_number in kmb_ops and not data.get("kmbCtbJoint"):
                data["kmbCtbJoint"] = True
                kmb_ops[route_number].append(data)
    for key in keys_to_remove:
        ctb_data = DATA_SHEET["routeList"][key]
        ctb_stops = ctb_data["stops"].get("ctb")
        if ctb_stops is not None:
            for kmb_data in kmb_ops[ctb_data["route"]]:
                kmb_stops = kmb_data["stops"]["kmb"]
                if len(ctb_stops) == len(kmb_stops):
                    match = True
                    for i in range(0, len(ctb_stops)):
                        kmb_stop_id = kmb_stops[i]
                        ctb_stop_id = ctb_stops[i]
                        stop_map = DATA_SHEET["stopMap"].get(kmb_stop_id)
                        if stop_map is not None and any(x[1] == ctb_stop_id for x in stop_map):
                            continue
                        kmb_stop_location = DATA_SHEET["stopList"][kmb_stop_id]["location"]
                        ctb_stop_location = DATA_SHEET["stopList"][ctb_stop_id]["location"]
                        if haversine(kmb_stop_location["lat"], kmb_stop_location["lng"], ctb_stop_location["lat"], ctb_stop_location["lng"]) >= 0.3:
                            match = False
                            break
                    if not match:
                        continue
                    for data_key in ctb_data:
                        if data_key not in kmb_data or kmb_data[data_key] is None:
                            kmb_data[data_key] = ctb_data[data_key]
                    break
        del DATA_SHEET["routeList"][key]

    keys_to_remove = []
    for key, data in DATA_SHEET["routeList"].items():
        bound = data["bound"]
        if "kmb" in bound:
            co = "kmb"
        elif "ctb" in bound:
            co = "ctb"
        elif "nlb" in bound:
            co = "nlb"
        elif "mtr-bus" in bound:
            co = "mtr-bus"
        elif "lrtfeeder" in bound:
            co = "lrtfeeder"
        elif "gmb" in bound:
            co = "gmb"
        elif "lightRail" in bound:
            co = "lightRail"
        elif "mtr" in bound:
            co = "mtr"
        elif "hkkf" in bound:
            co = "hkkf"
        elif "sunferry" in bound:
            co = "sunferry"
        elif "fortuneferry" in bound:
            co = "fortuneferry"
        else:
            keys_to_remove.append(key)
            continue
        if co not in data["stops"] or len(data["stops"][co]) <= 0:
            keys_to_remove.append(key)

    for key in keys_to_remove:
        del DATA_SHEET["routeList"][key]

    kmb_bbi_map = {
        "0028": ["上水"],
        "0007": ["粉嶺站"],
        "0027": ["華明"],
        "0009": ["粉嶺公路"],
        "0030": ["廣福道"],
        "0006": ["石門"],
        "0025": ["大欖隧道"],
        "0018": ["大圍"],
        "0013": ["城門隧道"],
        "0023": ["青沙公路"],
        "0024": ["大老山隧道"],
        "0020": ["寶田"],
        "0001": ["黃大仙"],
        "0005": ["屯門公路"],
        "0021": ["屯赤隧道"],
        "0016": ["彩虹"],
        "0029": ["寶達"],
        "0017": ["牛池灣"],
        "0011": ["大窩口"],
        "0003": ["美孚"],
        "0031": ["青衣"],
        "0022": ["青嶼幹線"],
        "0008": ["九龍城"],
        "0026": ["觀塘", "牛頭角站", "apm創紀之城5期"],
        "0004": ["將軍澳隧道"],
        "0002": ["啟德隧道"],
        "0012": ["將藍隧道"],
        "0019": ["尖沙咀"],
        "0015": ["西隧"],
        "0014": ["紅隧"],
        "0010": ["東隧"]
    }

    bbi_regex = "(?<!,)(?:^|[> ;]){name}(?:轉車站(?:[ ]*-?[ ]*[^(\\n<]+)?(?:[ ]*\([A-Z0-9]+\))?)|{name}(?:[ ]*\([A-Z0-9]+\))?(?=[ \\n<]|$)(?! \()"
    for key, stop in DATA_SHEET["stopList"].items():
        if re.match("^[0-9A-Z]{16}$", key):
            found = False
            stop_name_zh = stop["name"]["zh"]
            for bbi_id, bbi_names in kmb_bbi_map.items():
                for bbi_name in bbi_names:
                    if re.match(bbi_regex.format(name=bbi_name), stop_name_zh):
                        found = True
                        stop["kmbBbiId"] = bbi_id
                        break
                if found:
                    break


def normalize(input_str):
    return normalize_extract(input_str)[0]


def normalize_extract(input_str):
    global NORMALIZE_CHARS
    for keyword, replacement in NORMALIZE_CHARS.items():
        input_str = input_str.replace(keyword, replacement)
    match = re.search(r"[ 　]*(\([A-Z]{2}[0-9]{3}\))[ 　]*", input_str)
    if match:
        modified_string = input_str.replace(match.group(), "", 1)
        return modified_string, match.group(1)
    else:
        return input_str, None


def capitalize(input_str, lower=True):
    if lower:
        input_str = input_str.lower()
    return re.sub(r"(?:^|\s|[\"(\[{/\-]|'(?!s))+\S", lambda m: m.group().upper(), input_str)


def apply_recapitalize_keywords(input_str):
    global RECAPITALIZE_KEYWORDS
    for entry in RECAPITALIZE_KEYWORDS["recapitalize_regex"]:
        upper = entry["case"].casefold() == "UPPER".casefold()
        input_str = re.sub(r"(?i)" + entry["regex"], lambda m: m.group(0).upper() if upper else m.group(0).lower(), input_str)
    for keyword in RECAPITALIZE_KEYWORDS["recapitalize"]:
        input_str = re.sub(r"(?i)(?<![0-9a-zA-Z])" + re.escape(keyword) + "(?![0-9a-zA-Z])", keyword, input_str)
    return input_str


def capitalize_english_names():
    global DATA_SHEET
    for route in DATA_SHEET["routeList"].values():
        if "kmb" in route["bound"] or "sunferry" in route["bound"] or "fortuneferry" in route["bound"]:
            route["dest"]["en"] = apply_recapitalize_keywords(capitalize(route["dest"]["en"]))
            route["orig"]["en"] = apply_recapitalize_keywords(capitalize(route["orig"]["en"]))

    for stopId, stop in DATA_SHEET["stopList"].items():
        if len(stopId) == 16:
            stop["name"]["en"] = apply_recapitalize_keywords(capitalize(stop["name"]["en"]))


def normalize_names():
    global DATA_SHEET
    for route in DATA_SHEET["routeList"].values():
        route["dest"]["zh"] = normalize(route["dest"]["zh"])
        route["dest"]["en"] = normalize(route["dest"]["en"])
        route["orig"]["zh"] = normalize(route["orig"]["zh"])
        route["orig"]["en"] = normalize(route["orig"]["en"])
    for stopId, stop in DATA_SHEET["stopList"].items():
        stop["name"]["zh"], extracted_zh = normalize_extract(stop["name"]["zh"])
        stop["name"]["en"], extracted_en = normalize_extract(stop["name"]["en"])
        if extracted_zh is not None or extracted_en is not None:
            if "remark" not in stop:
                stop["remark"] = {
                    "zh": "" if extracted_zh is None else extracted_zh,
                    "en": "" if extracted_en is None else extracted_en
                }
            else:
                if extracted_zh is not None:
                    stop["remark"]["zh"] += " " + extracted_zh
                if extracted_en is not None:
                    stop["remark"]["en"] += " " + extracted_en


def inject_gmb_region():
    global DATA_SHEET
    regions = ["HKI", "KLN", "NT"]
    lookup_url = "https://data.etagmb.gov.hk/route/{region}/{route}"
    cached_lookups = {}
    route_numbers = set()
    for key, route in DATA_SHEET["routeList"].items():
        if "gmb" in route["bound"]:
            route_numbers.add(route["route"])

    def lookup(route_number, region):
        print("Downloading GMB data for " + route_number + " in " + region)
        cached_lookups[route_number + "_" + region] = get_web_json(lookup_url.format(region=region, route=route_number))

    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        futures = []
        for route_number in route_numbers:
            for region in regions:
                futures.append(executor.submit(lookup, route_number=route_number, region=region))
    for _ in concurrent.futures.as_completed(futures):
        pass

    keys_to_remove = []
    for key, route in DATA_SHEET["routeList"].items():
        if "gmb" in route["bound"]:
            route_number = route["route"]
            gtfs_id = route["gtfsId"]
            found = False
            for region in regions:
                lookup_key = route_number + "_" + region
                for gmb_data in cached_lookups[lookup_key]["data"]:
                    if gtfs_id == str(gmb_data["route_id"]):
                        route["gmbRegion"] = gmb_data["region"]
                        found = True
                        break
                if found:
                    break
            if found:
                continue
            keys_to_remove.append(key)
    for key in keys_to_remove:
        del DATA_SHEET["routeList"][key]


def list_kmb_subsidiary_routes():
    global DATA_SHEET
    global SUN_BUS_ROUTES
    global KMB_SUBSIDIARY_ROUTES
    global LWB_AREA
    global NOT_LWB_PREFIXES
    gtfs = get_web_text("https://static.data.gov.hk/td/pt-headway-tc/routes.txt").splitlines()[1:]
    lwb_routes = set()
    for line in gtfs:
        row = line.split(",")
        if "LWB" in row[1]:
            lwb_routes.add(row[2])
    for key, route in DATA_SHEET["routeList"].items():
        if "kmb" in route["bound"]:
            route_number = route["route"]
            if route_number in SUN_BUS_ROUTES:
                KMB_SUBSIDIARY_ROUTES["SUNB"].add(route_number)
                continue
            stops = route["stops"]["kmb"]
            first_stop = DATA_SHEET["stopList"][stops[0]]["location"]
            last_stop = DATA_SHEET["stopList"][stops[-1]]["location"]
            if route_number in lwb_routes or (not any(route_number.startswith(p) for p in NOT_LWB_PREFIXES) and (LWB_AREA.contains(Point(first_stop["lat"], first_stop["lng"])) or LWB_AREA.contains(Point(last_stop["lat"], last_stop["lng"])))):
                KMB_SUBSIDIARY_ROUTES["LWB"].add(route_number)


def download_and_process_mtr_data():
    global MTR_DATA
    global MTR_BARRIER_FREE_MAPPING
    global LRT_DATA
    MTR_BARRIER_FREE_MAPPING["categories"] = {}
    MTR_BARRIER_FREE_MAPPING["items"] = {}
    all_mtr_stations = get_web_text("https://opendata.mtr.com.hk/data/mtr_lines_and_stations.csv").splitlines()[1:]
    station_id_to_code = {70: "RAC"}
    station_code_to_id = {"RAC": {70}}
    MTR_DATA["RAC"] = {"fares": {}, "barrier_free": {}}
    for line in all_mtr_stations:
        try:
            row = line.split(",")
            station_id = int(row[3].strip('"'))
            station_code = row[2].strip('"')
            station_id_to_code[station_id] = station_code
            if station_code not in station_code_to_id:
                station_code_to_id[station_code] = {station_id}
            else:
                station_code_to_id[station_code].add(station_id)
            MTR_DATA[station_code] = {
                "fares": {},
                "barrier_free": {}
            }
        except ValueError:
            pass
    for key, stop in DATA_SHEET["stopList"].items():
        if key in station_code_to_id:
            station_ids = station_code_to_id[key]
            stop["mtrIds"] = sorted(station_ids)
    mtr_fares = get_web_text("https://opendata.mtr.com.hk/data/mtr_lines_fares.csv").splitlines()[1:]
    for line in mtr_fares:
        row = line.split(",")
        src_station = station_id_to_code[int(row[1].strip('"'))]
        dst_station = station_id_to_code[int(row[3].strip('"'))]
        if src_station != dst_station:
            MTR_DATA[src_station]["fares"][dst_station] = {
                "octo_adult": float(row[4].strip('"')),
                "octo_student": float(row[5].strip('"')),
                "octo_joyyou_sixty": float(row[6].strip('"')),
                "single_adult": float(row[7].strip('"')),
                "octo_child": float(row[8].strip('"')),
                "octo_elderly": float(row[9].strip('"')),
                "octo_pwd": float(row[10].strip('"')),
                "single_child": float(row[11].strip('"')),
                "single_elderly": float(row[12].strip('"'))
            }
    airport_express_fares = get_web_text("https://opendata.mtr.com.hk/data/airport_express_fares.csv").splitlines()[1:]
    for line in airport_express_fares:
        row = line.split(",")
        src_station = station_id_to_code[int(row[1].strip('"'))]
        dst_station = station_id_to_code[int(row[3].strip('"'))]
        if src_station != dst_station:
            MTR_DATA[src_station]["fares"][dst_station] = {
                "octo_adult": float(row[4].strip('"')),
                "octo_child": float(row[5].strip('"')),
                "single_adult": float(row[6].strip('"')),
                "single_child": float(row[7].strip('"'))
            }
    station_barrier_free_facilities = get_web_text("https://opendata.mtr.com.hk/data/barrier_free_facilities.csv").splitlines()[1:]
    for line in station_barrier_free_facilities:
        row = line.split(",")
        if len(row) > 2 and row[2].strip('"') == "Y":
            station = station_id_to_code[int(row[0].strip('"'))]
            facility_key = row[1].strip('"')
            facility_exit_zh = row[4].strip('"') if 4 < len(row) else ""
            if facility_exit_zh == "":
                MTR_DATA[station]["barrier_free"][facility_key] = {}
            else:
                MTR_DATA[station]["barrier_free"][facility_key] = {
                    "location": {
                        "zh": facility_exit_zh,
                        "en": row[3].strip('"')
                    }
                }
    barrier_free_details = get_web_text("https://opendata.mtr.com.hk/data/barrier_free_facility_category.csv").splitlines()[1:]
    for line in barrier_free_details:
        row = line.replace("&#32171;", "綫").split(",")
        code = row[0].strip('"')
        category = row[1].strip('"')
        MTR_BARRIER_FREE_MAPPING["categories"][category] = {
            "name": {
                "zh": row[3].strip('"'),
                "en": row[2].strip('"')
            }
        }
        MTR_BARRIER_FREE_MAPPING["items"][code] = {
            "category": category,
            "name": {
                "zh": row[5].strip('"'),
                "en": row[4].strip('"')
            }
        }
    lrt_fares = get_web_text("https://opendata.mtr.com.hk/data/light_rail_fares.csv").splitlines()[1:]
    for line in lrt_fares:
        row = line.split(",")
        src_id = int(row[0].strip('"'))
        dst_id = int(row[1].strip('"'))
        if src_id != dst_id:
            src_station = f"LR{src_id:03}"
            dst_station = f"LR{dst_id:03}"
            if src_station not in LRT_DATA:
                LRT_DATA[src_station] = {
                    "fares": {}
                }
            LRT_DATA[src_station]["fares"][dst_station] = {
                "octo_adult": float(row[2].strip('"')),
                "octo_child": float(row[3].strip('"')),
                "octo_elderly": float(row[4].strip('"')),
                "octo_pwd": float(row[5].strip('"')),
                "octo_student": float(row[6].strip('"')),
                "octo_joyyou_sixty": float(row[7].strip('"')),
                "single_adult": float(row[8].strip('"')),
                "single_child": float(row[9].strip('"')),
                "single_elderly": float(row[10].strip('"'))
            }


def download_and_process_traffic_snapshot():
    global TRAFFIC_SNAPSHOTS
    traffic_snapshots_zh = get_web_text("https://static.data.gov.hk/td/traffic-snapshot-images/code/Traffic_Camera_Locations_Tc.csv").splitlines()[1:]
    traffic_snapshots_en = get_web_text("https://static.data.gov.hk/td/traffic-snapshot-images/code/Traffic_Camera_Locations_En.csv").splitlines()[1:]
    en_names = {}
    for entry in traffic_snapshots_en:
        row = entry.split("\t")
        key = row[0].strip('"')
        name_en = row[3].strip('"')
        en_names[key] = name_en
    for entry in traffic_snapshots_zh:
        row = entry.split("\t")
        key = row[0].strip('"')
        name_zh = row[3].strip('"')
        name_en = en_names[key] if key in en_names else ""
        lat = float(row[6].strip('"'))
        lng = float(row[7].strip('"'))
        TRAFFIC_SNAPSHOTS.append({
            "key": key,
            "name": {
                "zh": name_zh,
                "en": name_en
            },
            "location": {
                "lat": lat,
                "lng": lng
            }
        })

def create_missing_routes():
    global DATA_SHEET
    global BUS_ROUTE
    global ROUTE_REMARKS
    global MISSING_ROUTES

    MISSING_ROUTES = copy.deepcopy(BUS_ROUTE)
    joint_operated = set()

    for key, data in DATA_SHEET["routeList"].items():
        route_number = data["route"]
        operators = data["co"]
        for co in operators:
            if route_number in MISSING_ROUTES and co in MISSING_ROUTES[route_number]:
                if data.get("kmbCtbJoint", False):
                    joint_operated.add(route_number)
                del MISSING_ROUTES[route_number][co]
                if len(MISSING_ROUTES[route_number]) <= 0:
                    del MISSING_ROUTES[route_number]
                break

    for route_number in joint_operated:
        if route_number in MISSING_ROUTES:
            if "kmb" in MISSING_ROUTES[route_number]:
                del MISSING_ROUTES[route_number]["kmb"]
            if "ctb" in MISSING_ROUTES[route_number]:
                del MISSING_ROUTES[route_number]["ctb"]
            if len(MISSING_ROUTES[route_number]) <= 0:
                del MISSING_ROUTES[route_number]

    unknown_stop_ids = {
        "kmb": ["ZZZZZZZZZZZZZZZY", "ZZZZZZZZZZZZZZZZ"],
        "ctb": ["999998", "999999"],
        "nlb": ["9998", "9999"],
        "mtr-bus": ["Z99-Z998", "Z99-Z999"],
        "gmb": ["99999998", "99999999"],
        "lightRail": ["LR99998", "LR99999"],
        "mtr": ["ZZY", "ZZZ"]
    }

    for stop_ids in unknown_stop_ids.values():
        for stop_id in stop_ids:
            DATA_SHEET["stopList"][stop_id] = {
                "location": {
                    "lat": 22.203615,
                    "lng": 114.415195
                },
                "name": {
                    "zh": "未有路線資訊",
                    "en": "Route Details TBD"
                },
                "remark": {
                    "zh": "(資訊通常會在數日後更新出現)",
                    "en": "(Usually details will be updated in a few days)"
                }
            }

    for route_number, operator_data in MISSING_ROUTES.items():
        for co, data in operator_data.items():
            key = f"{route_number}+99+{data['orig']['en']}+{data['dest']['en']}"
            DATA_SHEET["routeList"][key] = {
                "bound": {co: "O"},
                "co": [co],
                "dest": data['dest'].copy(),
                "fares": None,
                "faresHoliday": None,
                "freq": None,
                "gtfsId": None,
                "jt": None,
                "nlbId": None,
                "orig": data['orig'].copy(),
                "route": route_number,
                "serviceType": "99",
                "stops": {co: unknown_stop_ids[co].copy()}
            }


def add_route_remarks():
    global ROUTE_REMARKS
    global BUS_ROUTE

    kmb = {
        "HK1": {
            "zh": "九龍旅遊路線",
            "en": "Kowloon Bus Tour"
        }
    }
    ctb = {
        "H1": {
            "zh": "過海旅遊路線",
            "en": "Cross Harbour Bus Tour"
        },
        "H1S": {
            "zh": "過海旅遊路線",
            "en": "Cross Harbour Bus Tour"
        },
        "H2": {
            "zh": "過海旅遊路線",
            "en": "Cross Harbour Bus Tour"
        },
        "H2K": {
            "zh": "過海旅遊路線",
            "en": "Cross Harbour Bus Tour"
        },
        "H3": {
            "zh": "港島旅遊路線",
            "en": "Hong Kong Island Bus Tour"
        },
        "H4": {
            "zh": "港島旅遊路線",
            "en": "Hong Kong Island Bus Tour"
        }
    }
    nlb = {}

    kmb_routes_data_with_timetables = {}
    ctb_routes_data_with_timetables = {}
    nlb_routes_data_with_timetables = {}
    for key, data in DATA_SHEET["routeList"].items():
        route_number = data["route"]
        operators = data["co"]
        if "kmb" in operators and data["freq"] is not None:
            if route_number not in kmb_routes_data_with_timetables:
                kmb_routes_data_with_timetables[route_number] = []
            kmb_routes_data_with_timetables[route_number].append(data)
        if "ctb" in operators and data["freq"] is not None:
            if route_number not in ctb_routes_data_with_timetables:
                ctb_routes_data_with_timetables[route_number] = []
            ctb_routes_data_with_timetables[route_number].append(data)
        if "nlb" in operators and data["freq"] is not None:
            if route_number not in nlb_routes_data_with_timetables:
                nlb_routes_data_with_timetables[route_number] = []
            nlb_routes_data_with_timetables[route_number].append(data)

    for bus_route, operator_data in BUS_ROUTE.items():
        if "kmb" in operator_data and len(kmb_routes_data_with_timetables.get(bus_route, [])) <= 0:
            bound1 = get_web_json(f"https://search.kmb.hk/KMBWebSite/Function/FunctionRequest.ashx?action=getSpecialRoute&route={bus_route}&bound=1")["data"]
            bound2 = get_web_json(f"https://search.kmb.hk/KMBWebSite/Function/FunctionRequest.ashx?action=getSpecialRoute&route={bus_route}&bound=2")["data"]
            bound1_text = None
            bound2_text = None
            if len(bound1["routes"]) > 0 and len(bound1["routes"][0]["Desc_CHI"].strip()) > 0:
                bound1_text = bound1["routes"][0]["Desc_CHI"].strip()
            if len(bound2["routes"]) > 0 and len(bound2["routes"][0]["Desc_CHI"].strip()) > 0:
                bound2_text = bound2["routes"][0]["Desc_CHI"].strip()
            if bound1_text == bound2_text or bound1_text is None or bound2_text is None:
                if bound1_text is not None:
                    kmb[bus_route] = {
                        "zh": bound1["routes"][0]["Desc_CHI"].strip(),
                        "en": bound1["routes"][0]["Desc_ENG"].strip()
                    }
                    continue
                elif bound2_text is not None:
                    kmb[bus_route] = {
                        "zh": bound2["routes"][0]["Desc_CHI"].strip(),
                        "en": bound2["routes"][0]["Desc_ENG"].strip()
                    }
                    continue
        if bus_route.startswith("PB"):
            kmb[bus_route] = {
                "zh": "寵物巴士團 🐾",
                "en": "Pet Bus 🐾"
            }
        elif bus_route.startswith("PN"):
            kmb[bus_route] = {
                "zh": "夜間寵物巴士團 🐾",
                "en": "Evening Pet Bus 🐾"
            }
        elif bus_route.startswith("LB"):
            kmb[bus_route] = {
                "zh": "郊遊遠足路線 🏞",
                "en": "Leisure Bus 🏞"
            }

    ctb_soup_zh = BeautifulSoup(get_web_text("https://mobile.citybus.com.hk/nwp3/printout1.php?l=0", False), "html.parser")
    ctb_soup_en = BeautifulSoup(get_web_text("https://mobile.citybus.com.hk/nwp3/printout1.php?l=1", False), "html.parser")

    def parse_routes(soup):
        route_notes = {}
        rows = soup.find_all("tr")
        for row in rows:
            cells = row.find_all("td")
            if len(cells) >= 4:
                route = cells[0].get_text(strip=True)
                remark = cells[3].get_text(strip=True)
                if route not in route_notes:
                    route_notes[route] = remark
                elif route_notes[route] != remark:
                    route_notes[route] = None
        return route_notes

    route_notes_zh = parse_routes(ctb_soup_zh)
    route_notes_en = parse_routes(ctb_soup_en)

    for route_number in route_notes_zh:
        if route_number in BUS_ROUTE and "ctb" in BUS_ROUTE[route_number] and len(ctb_routes_data_with_timetables.get(route_number, [])) <= 0:
            zh_remark = route_notes_zh.get(route_number, "").strip()
            en_remark = route_notes_en.get(route_number, "").strip()
            if len(zh_remark) > 0:
                ctb[route_number] = {
                    "zh": zh_remark,
                    "en": en_remark
                }

    def extract_nlb_timetable_date(html_content):
        soup = BeautifulSoup(html_content, 'html.parser')
        widgets = soup.find_all('div', class_='widget')
        if not widgets:
            return None
        timetable_widget = None
        for widget in widgets:
            widget_title = widget.find('div', class_='widget-title')
            if widgets is not None and "時間表" in widget_title.text:
                timetable_widget = widget
                break
        if not timetable_widget:
            return None
        date_paragraph = timetable_widget.find('div', class_='widget-content')
        if date_paragraph and date_paragraph.text.strip():
            raw_text = date_paragraph.text.strip()
            dates_text = re.findall("([0-9]+)年([0-9]+)月([0-9]+)日", raw_text)
            if len(dates_text) <= 0:
                if len(re.findall("只於指定日子提供服務", raw_text)) > 0:
                    return []
                else:
                    return None
            dates_parsed = []
            for group in dates_text:
                year, month, day = group
                dates_parsed.append(f"{day}/{month}/{year}")
            return dates_parsed
        return None

    def format_dates_list(strings, separator, last_separator):
        if not strings:
            return ""
        elif len(strings) == 1:
            return strings[0]
        elif len(strings) == 2:
            return f"{strings[0]}{last_separator}{strings[1]}"
        else:
            return f"{separator.join(strings[:-1])}{last_separator}{strings[-1]}"

    for bus_route, operator_data in BUS_ROUTE.items():
        if "nlb" in operator_data and len(nlb_routes_data_with_timetables.get(bus_route, [])) <= 0:
            nlb_ids = operator_data["nlb"]["nlb_ids"]
            dates_list = {}
            for nlb_id in nlb_ids:
                page = get_web_text(f"https://nlb.com.hk/route/detail/{nlb_id}", False)
                dates = extract_nlb_timetable_date(page)
                if dates is not None:
                    dates_list[",".join(dates)] = dates
            if len(dates_list) == 1:
                dates = next(iter(dates_list.values()))
                if len(dates) == 0:
                    nlb[bus_route] = {
                        "zh": f"只於指定日子提供服務",
                        "en": f"Service on specified days only"
                    }
                else:
                    nlb[bus_route] = {
                        "zh": f"只於 {format_dates_list(dates, ', ', '及')} 提供服務",
                        "en": f"Service on {format_dates_list(dates, ', ', ' & ')} only"
                    }

    ROUTE_REMARKS["kmb"] = kmb
    ROUTE_REMARKS["ctb"] = ctb
    ROUTE_REMARKS["nlb"] = nlb

    for route_number, operator_data in MISSING_ROUTES.items():
        for co, data in operator_data.items():
            if co not in ROUTE_REMARKS:
                ROUTE_REMARKS[co] = {}
            original_remark = ROUTE_REMARKS[co].get(route_number)
            original_remark_zh = f"{original_remark['zh']} - " if original_remark is not None else ""
            original_remark_en = f"{original_remark['en']} - " if original_remark is not None else ""
            ROUTE_REMARKS[co][route_number] = {
                "zh": f"{original_remark_zh}未有路線資訊",
                "en": f"{original_remark_en}Route Details TBD"
            }

def add_ctb_stops_that_does_not_belong_to_any_route():
    route_to_stops = {}
    joint_routes = set()
    for key, data in DATA_SHEET["routeList"].items():
        if "ctb" not in data["co"]:
            continue
        route_number = data["route"]
        if "kmb" in data["bound"] or "kmb" in data["co"]:
            joint_routes.add(route_number)
        bounds = []
        if "ctb" in data["bound"]:
            bound = data["bound"]["ctb"]
            if len(bound) > 1:
                bounds.append("O")
                bounds.append("I")
            else:
                bounds.append(bound)
        else:
            bounds.append("O")
        if "ctb" in data["stops"]:
            if route_number in route_to_stops:
                for stop_id in data["stops"]["ctb"]:
                    route_to_stops[route_number]["stops"].add(stop_id)
            else:
                route_to_stops[route_number] = {
                    "bounds": {},
                    "stops": set(data["stops"]["ctb"])
                }
            for bound in bounds:
                if bound in route_to_stops[route_number]["bounds"]:
                    if len(data["stops"]["ctb"]) > route_to_stops[route_number]["bounds"][bound]["stop_count"]:
                        route_to_stops[route_number]["bounds"][bound]["stop_count"] = len(data["stops"]["ctb"])
                        route_to_stops[route_number]["bounds"][bound]["orig"] = data['orig']
                        route_to_stops[route_number]["bounds"][bound]["dest"] = data['dest']
                else:
                    route_to_stops[route_number]["bounds"][bound] = {
                        "stop_count": len(data["stops"]["ctb"]),
                        "orig": data['orig'],
                        "dest": data['dest'],
                    }

    def process_route(route_number, data):
        known_stop_ids = data["stops"]

        def handle_bound(bound_key):
            if bound_key in data["bounds"]:
                try:
                    url = f"https://rt.data.gov.hk/v2/transport/citybus/route-stop/CTB/{route_number}/{'in' if bound_key == 'I' else 'out'}bound"
                    stops_json = get_web_json(url)
                    stops = []
                    any_missing = False
                    for entry in stops_json["data"]:
                        stop_id = entry["stop"]
                        if stop_id not in known_stop_ids:
                            any_missing = True
                        stops.append(stop_id)

                    if any_missing:
                        bound_info = data["bounds"][bound_key]
                        key = f"{route_number}+98+{bound_info['orig']['en']}+{bound_info['dest']['en']}"
                        DATA_SHEET["routeList"][key] = {
                            "bound": {"ctb": bound_key},
                            "co": ["ctb"],
                            "dest": bound_info['dest'].copy(),
                            "fares": None,
                            "faresHoliday": None,
                            "freq": None,
                            "gtfsId": None,
                            "jt": None,
                            "nlbId": None,
                            "fakeRoute": True,
                            "orig": bound_info['orig'].copy(),
                            "route": route_number,
                            "serviceType": "98",
                            "stops": {"ctb": stops}
                        }
                        if route_number in joint_routes:
                            if not generate_ctb_route_with_more_stops(DATA_SHEET["routeList"][key]):
                                del DATA_SHEET["routeList"][key]
                except Exception as e:
                    print(e)

        handle_bound("O")
        handle_bound("I")

    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        futures = [executor.submit(process_route, route_number, data) for route_number, data in route_to_stops.items()]
    for _ in concurrent.futures.as_completed(futures):
        pass

    added_route_keys_by_route_number = {}
    has_fake_route = set()
    is_circular_route = {}
    for key, data in DATA_SHEET["routeList"].items():
        route_number = data["route"]
        if "ctb" in data["bound"] and "ctb" in data["stops"]:
            if "fakeRoute" in data and data["fakeRoute"]:
                has_fake_route.add(route_number)
            if len(data["bound"]["ctb"]) > 1:
                if route_number in is_circular_route:
                    is_circular_route[route_number]["length"] = min(len(data["stops"]["ctb"]), is_circular_route[route_number]["length"])
                    is_circular_route[route_number]["orig"].add(data["stops"]["ctb"][0])
                    is_circular_route[route_number]["dest"].add(data["stops"]["ctb"][-1])
                else:
                    is_circular_route[route_number] = {
                        "bound": data["bound"]["ctb"],
                        "length": len(data["stops"]["ctb"]),
                        "orig": {data["stops"]["ctb"][0]},
                        "dest": {data["stops"]["ctb"][-1]}
                    }
            if route_number in added_route_keys_by_route_number:
                added_route_keys_by_route_number[route_number].add(key)
            else:
                added_route_keys_by_route_number[route_number] = {key}

    for route_number, keys in added_route_keys_by_route_number.items():
        if route_number in has_fake_route:
            for key in keys:
                route = DATA_SHEET["routeList"][key]
                if route_number in is_circular_route:
                    if "fakeRoute" in route and route["fakeRoute"] and len(route["bound"]["ctb"]) <= 1:
                        data = is_circular_route[route_number]
                        if len(route["stops"]["ctb"]) >= data["length"]:
                            route["bound"]["ctb"] = data["bound"]
                        else:
                            first_stop_id = route["stops"]["ctb"][0]
                            last_stop_id = route["stops"]["ctb"][-1]
                            is_circular = False
                            if first_stop_id in DATA_SHEET["stopList"] and last_stop_id in DATA_SHEET["stopList"]:
                                first_stop = DATA_SHEET["stopList"][first_stop_id]["location"]
                                last_stop = DATA_SHEET["stopList"][last_stop_id]["location"]
                                if any(haversine(first_stop["lat"], first_stop["lng"],
                                                 DATA_SHEET["stopList"][a]["location"]["lat"],
                                                 DATA_SHEET["stopList"][a]["location"]["lng"]) < 0.1 for a in
                                       data["orig"]) and any(haversine(last_stop["lat"], last_stop["lng"],
                                                                       DATA_SHEET["stopList"][a]["location"]["lat"],
                                                                       DATA_SHEET["stopList"][a]["location"][
                                                                           "lng"]) < 0.1 for a in data["dest"]):
                                    is_circular = True
                            if is_circular:
                                route["bound"]["ctb"] = data["bound"]
                            else:
                                route["ctbIsCircular"] = True


def fix_missing_stops():
    missing_stops = set()
    for data in DATA_SHEET["routeList"].values():
        if "stops" in data and data["stops"] is not None:
            for stops in data["stops"].values():
                for stop_id in stops:
                    if stop_id not in DATA_SHEET["stopList"]:
                        missing_stops.add(stop_id)
                        DATA_SHEET["stopList"][stop_id] = {
                            "location": {
                                "lat": 22.203615,
                                "lng": 114.415195
                            },
                            "name": {
                                "zh": f"未有車站資訊",
                                "en": f"Stop Details TBD"
                            },
                            "remark": {
                                "zh": "(資訊通常會在數日後更新出現)",
                                "en": "(Usually details will be updated in a few days)"
                            }
                        }
    entry_to_remove = set()
    for key, stop_map in DATA_SHEET["stopMap"].items():
        for i in range(len(stop_map) - 1, -1, -1):
            array = stop_map[i]
            if array[1] in missing_stops:
                del stop_map[i]
        if len(stop_map) <= 0:
            entry_to_remove.add(key)
    for key in entry_to_remove:
        if key in DATA_SHEET["stopMap"]:
            del DATA_SHEET["stopMap"][key]
    for stop_id in missing_stops:
        if stop_id in DATA_SHEET["stopMap"]:
            del DATA_SHEET["stopMap"][stop_id]

def fix_ctb_route_bounds():
    ctb_route_numbers = set()
    ctb_circular_route_numbers = set()
    for data in DATA_SHEET["routeList"].values():
        if "stops" in data and data["stops"] is not None and "ctb" in data["stops"]:
            route_number = data["route"]
            if "ctbIsCircular" not in data and len(data["bound"]["ctb"]) == 1:
                ctb_route_numbers.add(route_number)
            else:
                ctb_circular_route_numbers.add(route_number)

    ctb_stops_by_bound = {}

    def process(route_number):
        for bound_key in ['O', 'I']:
            url = f"https://rt.data.gov.hk/v2/transport/citybus/route-stop/CTB/{route_number}/{'in' if bound_key == 'I' else 'out'}bound"
            stops_json = get_web_json(url)
            stops = []
            for stop_json in stops_json["data"]:
                stops.append(stop_json["stop"])
            ctb_stops_by_bound[f"{route_number}_{bound_key}"] = stops

    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        futures = [executor.submit(process, route_number) for route_number in (ctb_route_numbers | ctb_circular_route_numbers)]
    for _ in concurrent.futures.as_completed(futures):
        pass

    for key, data in DATA_SHEET["routeList"].items():
        if "stops" in data and data["stops"] is not None and "ctb" in data["stops"]:
            route_number = data["route"]
            if route_number in ctb_route_numbers:
                original_bound = data["bound"]["ctb"]
                reverse_bound = 'I' if original_bound == 'O' else 'O'
                ctb_stops = data["stops"]["ctb"]
                right_bound_stops = ctb_stops_by_bound[f"{route_number}_{original_bound}"]
                wrong_bound_stops = ctb_stops_by_bound[f"{route_number}_{reverse_bound}"]
                right_bound_count = 0
                wrong_bound_count = 0
                for stop in ctb_stops:
                    if stop in right_bound_stops:
                        right_bound_count += 1
                    if stop in wrong_bound_stops:
                        wrong_bound_count += 1
                if wrong_bound_count > right_bound_count:
                    data["bound"]["ctb"] = reverse_bound
                    print(f"Flipped {key} from {original_bound} (Matched {right_bound_count}) to {reverse_bound} (Matched {wrong_bound_count})")
            elif route_number in ctb_circular_route_numbers:
                ctb_stops = data["stops"]["ctb"]
                if ctb_stops[0] != ctb_stops[-1]:
                    outbound_stops = set(ctb_stops_by_bound[f"{route_number}_O"])
                    inbound_stops = set(ctb_stops_by_bound[f"{route_number}_I"])
                    circular_stops = set(outbound_stops) | set(inbound_stops)
                    outbound_count = 0
                    inbound_count = 0
                    circular_count = 0
                    for stop in ctb_stops:
                        if stop in outbound_stops:
                            outbound_count += 1
                        if stop in inbound_stops:
                            inbound_count += 1
                        if stop in circular_stops:
                            circular_count += 1
                    outbound_percentage = outbound_count / len(outbound_stops)
                    inbound_percentage = inbound_count / len(inbound_stops)
                    circular_percentage = circular_count / len(circular_stops)
                    if circular_percentage > 0.75 and outbound_percentage > 0.75 and inbound_percentage > 0.75:
                        original_bound = data["bound"]["ctb"]
                        if len(original_bound) == 1:
                            data["bound"]["ctb"] = "OI"
                            if "ctbIsCircular" in data:
                                del data["ctbIsCircular"]
                            if "循環線" not in data["dest"]["zh"]:
                                data["dest"]["zh"] += " (循環線)"
                                data["dest"]["en"] += " (Circular)"
                            print(f"Flipped {key} from {original_bound} to {data['bound']['ctb']} (Matched C: {circular_percentage} O: {outbound_percentage} I: {inbound_percentage})")
                    else:
                        original_bound = data["bound"]["ctb"]
                        data["bound"]["ctb"] = "O" if outbound_count > inbound_count else "I"
                        data["ctbIsCircular"] = True
                        data["dest"]["zh"] = re.sub(r" *\(?循環線\)?$", "", data["dest"]["zh"])
                        data["dest"]["en"] = re.sub(r" *\(?Circular\)?$", "", data["dest"]["en"])
                        if original_bound != data["bound"]["ctb"]:
                            print(f"Flipped {key} (Circular) from {original_bound} to {data['bound']['ctb']} (Matched O: {outbound_count} I: {inbound_count})")

    if IS_EXPERIMENTAL:
        ctb_circular_stripped = set()

        for key, data in DATA_SHEET["routeList"].items():
            if "ctb" in data["co"]:
                route_number = data["route"]
                if route_number in ctb_circular_route_numbers and len(data["bound"]["ctb"]) > 1:
                    if len(ctb_stops_by_bound[f"{route_number}_O"]) > 0 and len(ctb_stops_by_bound[f"{route_number}_I"]) > 0:
                        if "循環線" not in data["dest"]["zh"]:
                            data["dest"]["zh"] += " (循環線)"
                            data["dest"]["en"] += " (Circular)"
                        data["bound"]["ctb"] = "O"
                        data["ctbIsCircular"] = True
                        data["ctbCircularStripped"] = True
                        ctb_circular_stripped.add(route_number)
                        print(f"Updated {key} (Circular) to O for CTB display as bi-direction")

        for key, data in DATA_SHEET["routeList"].items():
            route_number = data["route"]
            if "ctb" in data["co"] and route_number in ctb_circular_stripped:
                if "ctbIsCircular" in data and "ctbCircularStripped" not in data:
                    del data["ctbIsCircular"]
                    if "freq" not in data or data["freq"] is None:
                        data["fakeRoute"] = True
                if "ctbCircularStripped" in data:
                    del data["ctbCircularStripped"]


def add_ctb_eta_stops():
    global CTB_ETA_STOPS

    ctb_route_numbers = set()
    for data in DATA_SHEET["routeList"].values():
        if "ctb" in data["co"]:
            ctb_route_numbers.add(data["route"])

    def process(route_number):
        entry = {}
        for bound_key in ['O', 'I']:
            url = f"https://rt.data.gov.hk/v2/transport/citybus/route-stop/CTB/{route_number}/{'in' if bound_key == 'I' else 'out'}bound"
            stops_json = get_web_json(url)
            stops = []
            for stop_json in stops_json["data"]:
                stops.append(stop_json["stop"])
            entry[bound_key] = stops
        CTB_ETA_STOPS[route_number] = entry

    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as executor:
        futures = [executor.submit(process, route_number) for route_number in ctb_route_numbers]
    for _ in concurrent.futures.as_completed(futures):
        pass

if IS_EXPERIMENTAL:
    print("============================")
    print("Running in Experimental Mode")
    print("============================")

print("Downloading & Processing KMB Routes")
download_and_process_kmb_route()
print("Downloading & Processing CTB Routes")
download_and_process_ctb_route()
print("Downloading & Processing NLB Routes")
download_and_process_nlb_route()
print("Downloading & Processing GMB Routes")
download_and_process_gmb_route()
print("Downloading & Processing Data Sheet")
download_and_process_data_sheet()
print("Downloading & Processing MTR-Bus Data")
download_and_process_mtr_bus_data()
print("Downloading & Processing MTR & LRT Data")
download_and_process_mtr_data()
print("Downloading & Processing Traffic Snapshots")
download_and_process_traffic_snapshot()
print("Fix CTB Route Bounds")
fix_ctb_route_bounds()
print("Creating Missing Routes")
create_missing_routes()
print("Adding Route Remarks")
add_route_remarks()
print("Add CTB Stops that does not Belong to Any Route")
add_ctb_stops_that_does_not_belong_to_any_route()
print("Fix Missing Stops")
fix_missing_stops()
print("Capitalizing KMB English Names")
capitalize_english_names()
print("Listing KMB Subsidiary Routes")
list_kmb_subsidiary_routes()
print("Normalizing Names")
normalize_names()
print("Searching & Injecting GMB Region")
inject_gmb_region()
print("Added CTB ETA Stops")
add_ctb_eta_stops()

output = {
    "dataSheet": DATA_SHEET,
    "mtrBusStopAlias": MTR_BUS_STOP_ALIAS,
    "busRoute": sorted(BUS_ROUTE.keys()),
    "routeRemarks": ROUTE_REMARKS,
    "kmbSubsidiary": {key: sorted(value) for key, value in KMB_SUBSIDIARY_ROUTES.items()},
    "mtrData": MTR_DATA,
    "mtrBarrierFreeMapping": MTR_BARRIER_FREE_MAPPING,
    "lrtData": LRT_DATA,
    "trafficSnapshot": TRAFFIC_SNAPSHOTS,
    "ctbEtaStops": CTB_ETA_STOPS
}

mtr_data = requests.get("https://mtrdata.hkbuseta.com/mtr_data.json").json()
merge(output, mtr_data)

lrt_data = requests.get("https://mtrdata.hkbuseta.com/lrt_data.json").json()
merge(output, lrt_data)

splash_data = requests.get("https://splash.hkbuseta.com/splash.json").json()
merge(output, splash_data)

with open(DATA_SHEET_FULL_FILE_NAME, "w", encoding="utf-8") as f:
    json.dump(output, f, sort_keys=True, ensure_ascii=False, separators=(',', ':'))

with open(DATA_SHEET_FULL_FORMATTED_FILE_NAME, "w", encoding="utf-8") as f:
    json.dump(output, f, sort_keys=True, ensure_ascii=False, separators=(',', ':'), indent=4)

strip_data_sheet(DATA_SHEET)
del output["mtrData"]
del output["mtrBarrierFreeMapping"]
del output["lrtData"]
del output["splashEntries"]
del output["trafficSnapshot"]

with open(DATA_SHEET_FILE_NAME, "w", encoding="utf-8") as f:
    json.dump(output, f, sort_keys=True, ensure_ascii=False, separators=(',', ':'))

with open(DATA_SHEET_FORMATTED_FILE_NAME, "w", encoding="utf-8") as f:
    json.dump(output, f, sort_keys=True, ensure_ascii=False, separators=(',', ':'), indent=4)

with open(LAST_UPDATED_FILE, 'w', encoding="utf-8") as f:
    f.write(str(int(time.time() * 1000)))
