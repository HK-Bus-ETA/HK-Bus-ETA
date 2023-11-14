import json
import re
import urllib.request
import concurrent.futures
import zlib
import chardet
import requests

BUS_ROUTE = set()
MTR_BUS_STOP_ALIAS = {}
DATA_SHEET = {}

DATA_SHEET_FILE_NAME = "data.json"
CHECKSUM_FILE_NAME = "checksum.md5"

RECAPITALIZE_KEYWORDS = [
    "BBI",
    "MTR",
    "STK",
    "FCA",
    "LMC",
    "SL",
    "apm"
]


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

    mtr_bus_route_list = get_web_text("https://opendata.mtr.com.hk/data/mtr_bus_routes.csv")
    mtr_bus_stop_list = get_web_text("https://opendata.mtr.com.hk/data/mtr_bus_stops.csv")

    routes_result = {}
    stops_result = {}
    stops_alias_result = {}
    stop_entries = [[y[1:len(y) - 1] if y.startswith("\"") else y for y in x.split(",")] for x in mtr_bus_stop_list.splitlines()[1:]]
    route_entries = [[y[1:len(y) - 1] if y.startswith("\"") else y for y in x.split(",")] for x in mtr_bus_route_list.splitlines()[1:]]

    stops_map = {}
    stops_by_route_bound = {}
    for stop_entry in stop_entries:
        position = stop_entry[4] + " " + stop_entry[5]
        if position not in stops_map:
            stops_map[position] = [stop_entry[3], stop_entry]
            stops_alias_result[stop_entry[3]] = [stop_entry[3]]
        else:
            stops_alias_result[stops_map[position][0]].append(stop_entry[3])
        route_number = stop_entry[0]
        bound = stop_entry[1]
        key = route_number + "_" + bound
        if key in stops_by_route_bound:
            stops_by_route_bound[key].append(stop_entry)
        else:
            stops_by_route_bound[key] = [stop_entry]

    for key, stop_details in stops_map.items():
        position = [float(stop_details[1][4]), float(stop_details[1][5])]
        result = {
            "location": {
                "lat": position[0],
                "lng": position[1]
            },
            "name": {
                "en": stop_details[1][7],
                "zh": stop_details[1][6]
            }
        }
        stops_result[stop_details[1][3]] = result

    for route_entry in route_entries:
        route_number = route_entry[0]
        for bound in ["O", "I"]:
            key = route_number + "_" + bound
            if key in stops_by_route_bound:
                stop_list = stops_by_route_bound[key]
                stop_list.sort(key=lambda x: float(x[2]))
                stop_ids = []
                for stop in stop_list:
                    position = stop[4] + " " + stop[5]
                    stop_ids.append(stops_map[position][0])
                result = {
                    "bound": {"mtr-bus": bound},
                    "co": ["mtr-bus"],
                    "dest": {
                        "en": route_entry[2].split(" to ")[1] if bound == "O" else stop_list[-1][7],
                        "zh": route_entry[1].split("至")[1] if bound == "O" else stop_list[-1][6]
                    },
                    "gtfsId": None,
                    "nlbId": None,
                    "orig": {
                        "en": stop_list[0][7],
                        "zh": stop_list[0][6]
                    },
                    "route": route_number,
                    "serviceType": 1,
                    "stops": {"mtr-bus": stop_ids}
                }
                key = route_number + "+1+" + stop_list[0][7] + "+" + stop_list[-1][7]
                routes_result[key] = result

    for key, data in routes_result.items():
        route = data.get("route", "")
        DATA_SHEET["routeList"][key] = data
        BUS_ROUTE.add(route)

    for key, stop_result in stops_result.items():
        DATA_SHEET["stopList"][key] = stop_result

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


def download_and_process_data_sheet():
    global DATA_SHEET
    global BUS_ROUTE
    url = "https://raw.githubusercontent.com/LOOHP/hk-bus-crawling/gh-pages/routeFareList.strip.json"
    response = requests.get(url)
    DATA_SHEET = response.json()

    DATA_SHEET["stopList"]["AC1FD9BDD09D1DD6"]["remark"] = {"remark": {"en": "(STK FCA - Closed Area Permit Required)", "zh": "(沙頭角邊境禁區 - 需持邊境禁區許可證)"}}
    DATA_SHEET["stopList"]["20001477"] = {"remark": {"en": "(STK FCA - Closed Area Permit Required)", "zh": "(沙頭角邊境禁區 - 需持邊境禁區許可證)"}}
    DATA_SHEET["stopList"]["152"]["remark"] = {"remark": {"en": "(S-Bay Control Point - Border Crossing Passengers Only)", "zh": "(深圳灣管制站 - 僅限過境旅客)"}}
    DATA_SHEET["stopList"]["20015453"]["remark"] = {"remark": {"en": "(S-Bay Control Point - Border Crossing Passengers Only)", "zh": "(深圳灣管制站 - 僅限過境旅客)"}}
    DATA_SHEET["stopList"]["003208"]["remark"] = {"remark": {"en": "(S-Bay Control Point - Border Crossing Passengers Only)", "zh": "(深圳灣管制站 - 僅限過境旅客)"}}
    DATA_SHEET["stopList"]["81567ACCCF40DD4B"]["remark"] = {"remark": {"en": "(LMC SL Immigration Control Point - Border Crossing Passengers Only)", "zh": "(落馬洲支線出入境管制站 - 僅限過境旅客)"}}
    DATA_SHEET["stopList"]["20015420"]["remark"] = {"remark": {"en": "(LMC SL Immigration Control Point - Border Crossing Passengers Only)", "zh": "(落馬洲支線出入境管制站 - 僅限過境旅客)"}}
    DATA_SHEET["stopList"]["20011698"]["remark"] = {"remark": {"en": "(LMC Control Point - Border Crossing Passengers Only)", "zh": "(落馬洲管制站 - 僅限過境旅客)"}}
    DATA_SHEET["stopList"]["20015598"]["remark"] = {"remark": {"en": "(LMC Control Point - Border Crossing Passengers Only)", "zh": "(落馬洲管制站 - 僅限過境旅客)"}}
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

    kmb_ops = set()
    ctb_circular = set()
    mtr_orig = {}
    mtr_dest = {}
    mtr_stops_lists = {}

    def list_index_of(li, o):
        try:
            return li.index(o)
        except ValueError:
            return -1

    for key in DATA_SHEET["routeList"].keys():
        data = DATA_SHEET["routeList"][key]
        bounds = data.get("bound")

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
            mtr_stops_lists.setdefault(line_name + "_" + bound, []).append(
                [DATA_SHEET["stopList"][s]["name"]["zh"] for s in stops])
        elif "kmb" in bounds:
            if "ctb" in data["co"]:
                data["kmbCtbJoint"] = True
                kmb_ops.add(data["route"])
        elif "ctb" in bounds and len(bounds.get("ctb")) > 1:
            ctb_circular.add(data["route"])
            dest = data["dest"]
            dest["zh"] += " (循環線)"
            dest["en"] += " (Circular)"

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
            elif route_number in ctb_circular and len(bounds.get("ctb")) < 2:
                data["ctbIsCircular"] = True
    for key in keys_to_remove:
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


def capitalize(input_str, lower=True):
    if lower:
        input_str = input_str.lower()
    return re.sub(r"(?:^|\s|[\"'(\[{/\-])+\S", lambda m: m.group().upper(), input_str)


def apply_recapitalize_keywords(input_str):
    global RECAPITALIZE_KEYWORDS
    for keyword in RECAPITALIZE_KEYWORDS:
        input_str = re.sub(r"(?i)(?<![0-9a-zA-Z])" + keyword + "(?![0-9a-zA-Z])", keyword, input_str)
    return input_str


def capitalize_kmb_english_names():
    global DATA_SHEET
    for route in DATA_SHEET["routeList"].values():
        if "kmb" in route["bound"]:
            route["dest"]["en"] = apply_recapitalize_keywords(capitalize(route["dest"]["en"]))
            route["orig"]["en"] = apply_recapitalize_keywords(capitalize(route["orig"]["en"]))

    for stopId, stop in DATA_SHEET["stopList"].items():
        if len(stopId) == 16:
            stop["name"]["en"] = apply_recapitalize_keywords(capitalize(stop["name"]["en"]))


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
print("Capitalizing KMB English Names")
capitalize_kmb_english_names()
print("Searching & Injecting GMB Region")
inject_gmb_region()

output = {
    "dataSheet": DATA_SHEET,
    "mtrBusStopAlias": MTR_BUS_STOP_ALIAS,
    "busRoute": sorted(BUS_ROUTE)
}

with open(DATA_SHEET_FILE_NAME, "w", encoding="utf-8") as f:
    json.dump(output, f, sort_keys=True, ensure_ascii=False, separators=(',', ':'))
