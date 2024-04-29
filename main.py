import json
import math
import re
import time
import urllib.request
import concurrent.futures
import zlib
import chardet
import requests
from shapely.geometry import Point, Polygon

BUS_ROUTE = set()
MTR_BUS_STOP_ALIAS = {}
DATA_SHEET = {}
KMB_SUBSIDIARY_ROUTES = {"LWB": set(), "SUNB": set()}
MTR_DATA = {}
MTR_BARRIER_FREE_MAPPING = {}
LRT_DATA = {}

DATA_SHEET_FILE_NAME = "data.json"
DATA_SHEET_FORMATTED_FILE_NAME = "data_formatted.json"
DATA_SHEET_FULL_FILE_NAME = "data_full.json"
DATA_SHEET_FULL_FORMATTED_FILE_NAME = "data_full_formatted.json"
CHECKSUM_FILE_NAME = "checksum.md5"
LAST_UPDATED_FILE = "last_updated.txt"

RECAPITALIZE_KEYWORDS = [
    "BBI",
    "MTR",
    "STK",
    "FCA",
    "LMC",
    "SL",
    "apm",
    "II",
    "GTC",
    "HK"
]

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


def get_web_json(url):
    with urllib.request.urlopen(url) as data:
        return json.load(data)


def get_web_text(url, gzip=True):
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


def download_and_process_kmb_route():
    global BUS_ROUTE
    url = "https://data.etabus.gov.hk/v1/transport/kmb/route/"
    response = requests.get(url)
    data = response.json()
    kmb_route = data.get("data", [])
    for route_data in kmb_route:
        route = route_data.get("route", "")
        BUS_ROUTE.add(route)


def download_and_process_ctb_route():
    global BUS_ROUTE
    url = "https://rt.data.gov.hk/v2/transport/citybus/route/ctb"
    response = requests.get(url)
    data = response.json()
    ctb_route = data.get("data", [])
    for route_data in ctb_route:
        route = route_data.get("route", "")
        BUS_ROUTE.add(route)


def download_and_process_nlb_route():
    global BUS_ROUTE
    url = "https://rt.data.gov.hk/v2/transport/nlb/route.php?action=list"
    response = requests.get(url)
    data = response.json()
    nlb_route = data.get("routes", [])
    for route_data in nlb_route:
        route = route_data.get("routeNo", "")
        BUS_ROUTE.add(route)


def download_and_process_mtr_bus_data():
    global BUS_ROUTE
    global DATA_SHEET
    global MTR_BUS_STOP_ALIAS

    stop_id_pattern = re.compile("^[A-Z]?[0-9]{1,3}[A-Z]?-[A-Z][0-9]{3}$")
    stops_alias_result = {}

    for stop_id in DATA_SHEET["stopList"].keys():
        if stop_id_pattern.match(stop_id):
            stops_alias_result[stop_id] = [stop_id]

    for key, data in DATA_SHEET["routeList"].items():
        bound = data["bound"]
        if "lrtfeeder" in bound:
            BUS_ROUTE.add(data["route"])
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
            BUS_ROUTE.add(route)


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
        if "freq" in route_data:
            del route_data["freq"]
    if "serviceDayMap" in data:
        del data["serviceDayMap"]


def download_and_process_data_sheet():
    global DATA_SHEET
    global BUS_ROUTE
    url = "https://data.hkbus.app/routeFareList.min.json"
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

    lrt_705_1_stops = DATA_SHEET["routeList"]["705+1+Tin Shui Wai+Tin Wing"]["stops"]["lightRail"]
    lrt_705_2_stops = DATA_SHEET["routeList"]["705+1+Tin Wing+Tin Shui Wai"]["stops"]["lightRail"]
    lrt_705_2_stops.pop(0)
    DATA_SHEET["routeList"]["705+1+Tin Shui Wai+Tin Shui Wai"] = DATA_SHEET["routeList"]["705+1+Tin Shui Wai+Tin Wing"]
    DATA_SHEET["routeList"]["705+1+Tin Shui Wai+Tin Shui Wai"]["dest"] = DATA_SHEET["routeList"]["705+1+Tin Wing+Tin Shui Wai"]["dest"]
    DATA_SHEET["routeList"]["705+1+Tin Shui Wai+Tin Shui Wai"]["stops"]["lightRail"] = lrt_705_1_stops + lrt_705_2_stops
    DATA_SHEET["routeList"]["705+1+Tin Shui Wai+Tin Shui Wai"]["lrtCircular"] = {"en": "TSW Circular", "zh": "天水圍循環綫"}
    del DATA_SHEET["routeList"]["705+1+Tin Shui Wai+Tin Wing"]
    del DATA_SHEET["routeList"]["705+1+Tin Wing+Tin Shui Wai"]

    lrt_706_1_stops = DATA_SHEET["routeList"]["706+1+Tin Shui Wai+Tin Wing"]["stops"]["lightRail"]
    lrt_706_2_stops = DATA_SHEET["routeList"]["706+1+Tin Wing+Tin Shui Wai"]["stops"]["lightRail"]
    lrt_706_2_stops.pop(0)
    DATA_SHEET["routeList"]["706+1+Tin Shui Wai+Tin Shui Wai"] = DATA_SHEET["routeList"]["706+1+Tin Wing+Tin Shui Wai"]
    DATA_SHEET["routeList"]["706+1+Tin Shui Wai+Tin Shui Wai"]["orig"] = DATA_SHEET["routeList"]["706+1+Tin Shui Wai+Tin Wing"]["orig"]
    DATA_SHEET["routeList"]["706+1+Tin Shui Wai+Tin Shui Wai"]["stops"]["lightRail"] = lrt_706_1_stops + lrt_706_2_stops
    DATA_SHEET["routeList"]["706+1+Tin Shui Wai+Tin Shui Wai"]["lrtCircular"] = {"en": "TSW Circular", "zh": "天水圍循環綫"}
    del DATA_SHEET["routeList"]["706+1+Tin Shui Wai+Tin Wing"]
    del DATA_SHEET["routeList"]["706+1+Tin Wing+Tin Shui Wai"]

    kmb_ops = {}
    ctb_circular = {}
    mtr_orig = {}
    mtr_dest = {}
    mtr_stops_lists = {}

    def list_index_of(li, o):
        try:
            return li.index(o)
        except ValueError:
            return -1

    for key, data in DATA_SHEET["routeList"].items():
        bounds = data.get("bound")

        cos = data.get("co")
        if "hkkf" in bounds and "hkkf" not in cos:
            data["co"] = ["hkkf"]
        if "sunferry" in bounds and "sunferry" not in cos:
            data["co"] = ["sunferry"]
        if "fortuneferry" in bounds and "fortuneferry" not in cos:
            data["co"] = ["fortuneferry"]

        if "lightRail" in bounds:
            BUS_ROUTE.add(data["route"])
        elif "mtr" in bounds:
            line_name = data["route"]
            BUS_ROUTE.add(line_name)
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

        if "lrtfeeder" in bounds:
            keys_to_remove.append(key)
        elif "mtr" in bounds:
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
                keys_to_remove.append(key)
            elif route_number in ctb_circular:
                if ctb_circular[route_number] < 0:
                    if len(bounds["ctb"]) >= 2:
                        bounds["ctb"] = bounds["ctb"][0:1]
                elif len(bounds["ctb"]) < 2:
                    data["ctbIsCircular"] = True
                else:
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
                        if haversine(kmb_stop_location["lat"], kmb_stop_location["lng"], ctb_stop_location["lat"], ctb_stop_location["lng"]) >= 0.1:
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
        elif "gmb" in bound:
            co = "gmb"
        elif "lightRail" in bound:
            co = "lightRail"
        elif "mtr" in bound:
            co = "mtr"
        else:
            keys_to_remove.append(key)
            break
        if co not in data["stops"]:
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
    global NORMALIZE_CHARS
    for keyword, replacement in NORMALIZE_CHARS.items():
        input_str = input_str.replace(keyword, replacement)
    return input_str


def capitalize(input_str, lower=True):
    if lower:
        input_str = input_str.lower()
    return re.sub(r"(?:^|\s|[\"'(\[{/\-])+\S", lambda m: m.group().upper(), input_str)


def apply_recapitalize_keywords(input_str):
    global RECAPITALIZE_KEYWORDS
    for keyword in RECAPITALIZE_KEYWORDS:
        input_str = re.sub(r"(?i)(?<![0-9a-zA-Z])" + keyword + "(?![0-9a-zA-Z])", keyword, input_str)
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
        stop["name"]["zh"] = normalize(stop["name"]["zh"])
        stop["name"]["en"] = normalize(stop["name"]["en"])


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
    global KMB_SUBSIDIARY_ROUTE
    global LWB_AREA
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
            if route_number in lwb_routes or LWB_AREA.contains(Point(first_stop["lat"], first_stop["lng"])) or LWB_AREA.contains(Point(last_stop["lat"], last_stop["lng"])):
                KMB_SUBSIDIARY_ROUTES["LWB"].add(route_number)


def download_and_process_mtr_data():
    global MTR_DATA
    global MTR_BARRIER_FREE_MAPPING
    global LRT_DATA
    MTR_BARRIER_FREE_MAPPING["categories"] = {}
    MTR_BARRIER_FREE_MAPPING["items"] = {}
    all_mtr_stations = get_web_text("https://opendata.mtr.com.hk/data/mtr_lines_and_stations.csv").splitlines()[1:]
    station_id_to_code = {70: "RAC"}
    MTR_DATA["RAC"] = {"fares": {}, "barrier_free": {}}
    for line in all_mtr_stations:
        try:
            row = line.split(",")
            station_id = int(row[3].strip('"'))
            station_code = row[2].strip('"')
            station_id_to_code[station_id] = station_code
            MTR_DATA[station_code] = {
                "fares": {},
                "barrier_free": {}
            }
        except ValueError:
            pass
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
print("Capitalizing KMB English Names")
capitalize_english_names()
print("Listing KMB Subsidiary Routes")
list_kmb_subsidiary_routes()
print("Normalizing Names")
normalize_names()
print("Searching & Injecting GMB Region")
inject_gmb_region()

output = {
    "dataSheet": DATA_SHEET,
    "mtrBusStopAlias": MTR_BUS_STOP_ALIAS,
    "busRoute": sorted(BUS_ROUTE),
    "kmbSubsidiary": {key: sorted(value) for key, value in KMB_SUBSIDIARY_ROUTES.items()},
    "mtrData": MTR_DATA,
    "mtrBarrierFreeMapping": MTR_BARRIER_FREE_MAPPING,
    "lrtData": LRT_DATA
}

with open('mtr_data.json', 'r') as file:
    mtr_data = json.load(file)
    merge(output, mtr_data)

with open(DATA_SHEET_FULL_FILE_NAME, "w", encoding="utf-8") as f:
    json.dump(output, f, sort_keys=True, ensure_ascii=False, separators=(',', ':'))

with open(DATA_SHEET_FULL_FORMATTED_FILE_NAME, "w", encoding="utf-8") as f:
    json.dump(output, f, sort_keys=True, ensure_ascii=False, separators=(',', ':'), indent=4)

strip_data_sheet(DATA_SHEET)
del output["mtrData"]
del output["mtrBarrierFreeMapping"]
del output["lrtData"]

with open(DATA_SHEET_FILE_NAME, "w", encoding="utf-8") as f:
    json.dump(output, f, sort_keys=True, ensure_ascii=False, separators=(',', ':'))

with open(DATA_SHEET_FORMATTED_FILE_NAME, "w", encoding="utf-8") as f:
    json.dump(output, f, sort_keys=True, ensure_ascii=False, separators=(',', ':'), indent=4)

with open(LAST_UPDATED_FILE, 'w') as f:
    f.write(str(int(time.time() * 1000)))
