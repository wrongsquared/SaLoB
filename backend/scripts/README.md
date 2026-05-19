# Seed Image Script

This script downloads seed images **before** running the app. The Java seeders now expect
images to exist on disk or in MinIO and will fail fast if they are missing.

## Setup

```bash
python -m venv .venv
source .venv/bin/activate
pip install -r scripts/requirements.txt
```

## Usage

Scrape both food and eatery images:

```bash
python scripts/seed_images.py
```

## Output locations

- Food images: `src/main/resources/static/food-images/`
- Eatery images: `src/main/resources/static/eatery-images/`
