#!/usr/bin/env python3
import io
import os
import re
from time import sleep
from typing import Iterable, List, Optional

import requests
from ddgs import DDGS
from PIL import Image

DEFAULT_TIMEOUT = 10
DEFAULT_DELAY = 1.5
DEFAULT_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36"

WORKSPACE_ROOT = os.path.abspath(os.path.dirname(__file__))

OUTPUT_DIRS = {
    "food": os.path.join(WORKSPACE_ROOT, "images", "food-images"),
    "eatery": os.path.join(WORKSPACE_ROOT, "images", "eatery-images"),
}

FOOD_LABELS = [
    "Chicken Rice",
    "Hokkien Mee",
    "Char Kway Teow",
    "Laksa",
    "Nasi Lemak",
    "Roti Prata",
    "Satay",
    "Beef Rendang",
    "Chili Crab",
    "Popiah",
    "Bak Kut Teh",
    "Mee Goreng",
    "Prawn Noodle",
    "Fish Soup",
    "Biryani",
    "Ramen",
    "Sushi",
    "Tacos",
    "Burgers",
    "Pasta",
    "Paella",
    "Ceviche",
    "Falafel",
    "Quiche",
    "Dim Sum",
    "Peking Duck",
    "Kway Teow",
    "Oyster Omelette",
    "Gado Gado",
    "Fried Chicken",
    "Chow Mein",
    "Mango Sticky Rice",
    "Kimchi",
    "Curry Laksa",
    "Spaghetti Bolognese",
    "Tiramisu",
    "Chocolate Cake",
    "Apple Pie",
    "Crepes",
    "Shakshuka",
    "Thai Green Curry",
    "Banh Mi",
    "Pho",
    "Baba Ganoush",
    "Bratwurst",
    "Samosas",
    "Chowder",
    "Jambalaya",
    "Buffalo Wings",
    "Eggs Benedict",
    "Caesar Salad",
    "Gelato",
    "Pavlova",
    "Baklava",
    "Donuts",
    "Croissants",
    "Bagels",
    "Fettuccine Alfredo",
    "Coconut Curry",
    "Moussaka",
    "Risotto",
    "Tandoori Chicken",
]

EATERY_LABELS = [
    "Maxwell Food Centre",
    "Amoy Street Food Centre",
    "Lau Pa Sat",
    "Common Man Coffee Roasters",
    "Tiong Bahru Bakery",
    "PS.Cafe at Ann Siang Hill",
    "Ya Kun Kaya Toast (Far East Square)",
    "The Daily Cut (Tanjong Pagar)",
    "Heytea (ION Orchard)",
    "Din Tai Fung (Paragon)",
    "Old Airport Road Food Centre",
    "East Coast Lagoon Food Village",
    "Penny University",
    "328 Katong Laksa",
    "Beach Road Prawn Noodle",
    "Birds of Paradise Gelato",
    "Changi Village Hawker Centre",
    "Bedok 85 (Fengshan Market)",
    "Simpang Bedok",
    "Jewel Changi Food Court",
    "Ghim Moh Market",
    "Clementi 448 Market",
    "Creamier Handcrafted Ice Cream",
    "The Workbench Bistro",
    "Holland Village Food Centre",
    "Boon Lay Power Nasi Lemak",
    "Waku Waku Burger",
    "Jurong Point Kopitiam",
    "Atlas Coffeehouse",
    "Burnt Cones (Sunset Way)",
    "Chomp Chomp Food Centre",
    "Serangoon Garden Market",
    "Wheeler's Estate",
    "Sembawang Eating House",
    "Chong Pang Market",
    "Nakhon Kitchen (Kovan)",
    "Lola's Cafe",
    "Punggol Settlement Seafood",
    "Whampoa Drive Food Centre",
    "Upper Thomson Prata House",
    "McDonald's (Ang Mo Kio)",
    "KFC (Tampines Hub)",
    "Koi The (Plaza Singapura)",
    "LiHo (VivoCity)",
    "Starbucks (Rochester Park)",
    "Toast Box (Nex)",
    "BreadTalk (Bugis Junction)",
    "Cedele (Raffles City)",
    "Haidilao (Clarke Quay)",
    "Food Republic (Wisma Atria)",
    "Circuit Road Food Centre",
    "Geylang Bahru Market",
    "Kim Keat Hokkien Mee",
    "Toa Payoh Lucky Pisang Raja",
    "Sin Ming Roti Prata",
    "Heavens (Ghim Moh)",
    "Zam Zam Singapore",
    "Victory Restaurant",
    "Swee Choon Tim Sum",
    "The Whale Tea (Lot One)",
    "Jollibean (Toa Payoh)",
    "Old Hen Coffee Bar",
    "Chye Seng Huat Hardware",
    "Brunetti (Tanglin Mall)",
    "Cedele Bakery Cafe",
    "Plain Vanilla Bakery",
    "Tiong Bahru Galicier Pastry",
    "Brotherbird Bakehouse",
    "Two Men Bagel House",
    "Wild Honey",
    "Genki Sushi (Ngee Ann City)",
    "Sushi Tei (Holland V)",
    "PastaMania (Clementi Mall)",
    "Saizeriya (Liang Court)",
    "Marche Movenpick",
    "Encik Tan (Bedok Mall)",
    "Stuff'd (Bugis Junction)",
    "Irvins Salted Egg (Orchard)",
    "Nine Fresh",
    "Milksha (Suntec)",
]


def normalize_filename(label: str) -> str:
    normalized = re.sub(r"[^a-z0-9]+", "_", (label or "").strip().lower())
    normalized = re.sub(r"^_+|_+$", "", normalized)
    if not normalized:
        normalized = "unknown"
    return f"{normalized}.jpg"


def fetch_first_ddg_image(query: str) -> Optional[str]:
    ddg = DDGS()
    try:
        results = ddg.images(
            query=query, region="wt-wt", safesearch="on", max_results=1
        )
        if results:
            url = results[0].get("image")
            if url:
                return url
    except Exception:
        pass
    return None


def download_image(url: str, timeout: int, user_agent: str) -> Optional[Image.Image]:
    headers = {"User-Agent": user_agent}
    response = requests.get(url, headers=headers, timeout=timeout)
    if response.status_code != 200:
        return None
    img_bytes = io.BytesIO(response.content)
    with Image.open(img_bytes) as img:
        return img.convert("RGB")


def ensure_dir(path: str) -> None:
    os.makedirs(path, exist_ok=True)


def iter_labels(labels: Iterable[str], limit: Optional[int]) -> Iterable[str]:
    count = 0
    for label in labels:
        if limit is not None and count >= limit:
            return
        yield label
        count += 1


def scrape_images(
    image_type: str,
    labels: List[str],
    output_dir: str,
    search_suffix: str,
    timeout: int,
    delay: float,
    user_agent: str,
    dry_run: bool,
    limit: Optional[int],
) -> List[str]:
    if not labels:
        raise RuntimeError(f"No labels found for {image_type} images")

    ensure_dir(output_dir)
    suffix = f" {search_suffix}" if search_suffix else ""
    failed_items = []

    for label in iter_labels(labels, limit):
        filename = normalize_filename(label)
        save_path = os.path.join(output_dir, filename)

        if os.path.exists(save_path):
            print(f"[-] Skip '{label}': {filename} already exists")
            continue

        query = f"{label}{suffix}"
        print(f"[*] Searching '{query}'")

        if dry_run:
            print("    [~] Dry run mode enabled, skipping download")
            continue

        try:
            image_url = fetch_first_ddg_image(query)
            if not image_url:
                print(f"    [!] No results for '{label}'")
                failed_items.append(label)
                sleep(delay)
                continue

            img = download_image(image_url, timeout, user_agent)
            if not img:
                print(f"    [!] Download failed for '{label}'")
                failed_items.append(label)
                sleep(delay)
                continue

            img.save(save_path, "JPEG", quality=95)
            print(f"    [+] Saved {filename}")
            sleep(delay)
        except Exception as exc:
            print(f"    [!] Error for '{label}': {exc}")
            failed_items.append(label)
            sleep(max(delay, 5))

    return failed_items


def main() -> None:
    print("\n" + "=" * 60)
    print("Starting image scraping process...")
    print("=" * 60 + "\n")

    failed_food = scrape_images(
        image_type="food",
        labels=FOOD_LABELS,
        output_dir=OUTPUT_DIRS["food"],
        search_suffix="food",
        timeout=DEFAULT_TIMEOUT,
        delay=DEFAULT_DELAY,
        user_agent=DEFAULT_USER_AGENT,
        dry_run=False,
        limit=None,
    )

    print("\n")

    failed_eatery = scrape_images(
        image_type="eatery",
        labels=EATERY_LABELS,
        output_dir=OUTPUT_DIRS["eatery"],
        search_suffix="restaurant",
        timeout=DEFAULT_TIMEOUT,
        delay=DEFAULT_DELAY,
        user_agent=DEFAULT_USER_AGENT,
        dry_run=False,
        limit=None,
    )

    # Print final report
    print("\n" + "=" * 60)
    print("FINAL REPORT")
    print("=" * 60)

    if failed_food or failed_eatery:
        if failed_food:
            print(f"\n[!] Failed to find images for {len(failed_food)} food items:")
            for item in failed_food:
                print(f"    - {item}")

        if failed_eatery:
            print(f"\n[!] Failed to find images for {len(failed_eatery)} eatery items:")
            for item in failed_eatery:
                print(f"    - {item}")
    else:
        print("\n[+] All images downloaded successfully!")

    print("\n" + "=" * 60)


if __name__ == "__main__":
    main()
