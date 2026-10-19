"""
Builds app/src/main/res/raw/fdc_tsv.gz, the offline ingredient table, from USDA FoodData Central (public domain).

Sources: Foundation Foods and SR Legacy, the two curated sets (no branded foods). Run from the repo root:

    python -I tools/build_food_table.py

Downloads land in tools/fdc-cache (gitignored) and are reused. Commit the output; the app build needs no network.

Output, one food per line, tab separated:  name  protein_per_100g  kcal_per_100g  portions
where portions is "label=grams|label=grams|..." (may be empty), e.g. "1 cup, chopped=140|1 oz=28.35".
"""
import csv
import gzip
import io
import os
import sys
import urllib.request
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "tools", "fdc-cache")
OUT = os.path.join(ROOT, "app", "src", "main", "res", "raw", "fdc_tsv.gz")
SOURCES = [
    "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_foundation_food_csv_2025-04-24.zip",
    "https://fdc.nal.usda.gov/fdc-datasets/FoodData_Central_sr_legacy_food_csv_2018-04.zip",
]
PROTEIN = "1003"
# Energy in kcal: SR Legacy carries 1008; Foundation often only the Atwater values (specific 2048, general 2047).
KCAL = ("1008", "2048", "2047")


def fetch(url):
    os.makedirs(CACHE, exist_ok=True)
    path = os.path.join(CACHE, url.rsplit("/", 1)[1])
    if not os.path.exists(path):
        print("downloading", url)
        urllib.request.urlretrieve(url, path)
    return path


def read_csv(z, name):
    member = next(n for n in z.namelist() if n.endswith("/" + name) or n == name)
    with z.open(member) as f:
        yield from csv.DictReader(io.TextIOWrapper(f, encoding="utf-8-sig"))


def fmt(x):
    s = f"{x:.1f}".rstrip("0").rstrip(".")
    return s if s else "0"


def load(path):
    foods = {}
    with zipfile.ZipFile(path) as z:
        for r in read_csv(z, "food.csv"):
            if r["data_type"] in ("foundation_food", "sr_legacy_food"):
                foods[r["fdc_id"]] = {"name": r["description"].strip(), "p": None, "k": {}, "portions": []}
        for r in read_csv(z, "food_nutrient.csv"):
            f = foods.get(r["fdc_id"])
            if f is None or not r["amount"]:
                continue
            if r["nutrient_id"] == PROTEIN:
                f["p"] = float(r["amount"])
            elif r["nutrient_id"] in KCAL:
                f["k"][r["nutrient_id"]] = float(r["amount"])
        units = {r["id"]: r["name"] for r in read_csv(z, "measure_unit.csv")}
        for r in read_csv(z, "food_portion.csv"):
            f = foods.get(r["fdc_id"])
            if f is None or not r["gram_weight"]:
                continue
            unit = units.get(r["measure_unit_id"], "")
            if unit == "undetermined":
                unit = ""
            amount = r["amount"].strip()
            label = " ".join(x for x in (fmt(float(amount)) if amount else "", unit, r["modifier"].strip() or r["portion_description"].strip()) if x)
            label = label.replace("|", "/").replace("=", " ")
            if label and len(f["portions"]) < 4:
                f["portions"].append((label, float(r["gram_weight"])))
    return foods


def main():
    rows = {}
    for url in SOURCES:  # Foundation first, so it wins on a duplicate description.
        for f in load(fetch(url)).values():
            kcal = next((f["k"][k] for k in KCAL if k in f["k"]), None)
            if f["p"] is None and kcal is None:
                continue
            key = f["name"].lower()
            if key in rows:
                continue
            rows[key] = (f["name"], f["p"] or 0.0, kcal or 0.0, f["portions"])
    lines = [
        "\t".join((name, fmt(p), fmt(k), "|".join(f"{l}={fmt(g)}" for l, g in portions)))
        for name, p, k, portions in sorted(rows.values(), key=lambda r: r[0].lower())
    ]
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with gzip.open(OUT, "wt", encoding="utf-8", newline="\n", compresslevel=9) as out:
        out.write("\n".join(lines) + "\n")
    print(f"{len(lines)} foods, {os.path.getsize(OUT) / 1024:.0f} KB -> {OUT}")


if __name__ == "__main__":
    sys.exit(main())
