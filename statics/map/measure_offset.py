"""
How far the scheme of OpenStreetMap lies from the satellite imagery of Esri on the campus of MPEI.

The coordinates in markers.json fit the satellite imagery. The scheme of OpenStreetMap puts the same buildings a few meters
aside, so the markers stand at the edge of their buildings, or beside them, on it. The app draws the scheme moved back by
the distance that this script prints (OSM_FEATURES_EAST_METERS and OSM_FEATURES_NORTH_METERS in
modules/ui/map/.../TileLayer.kt).

The markers are the measure: the tiles of zoom 18 that cover them are downloaded, the buildings of the scheme are cut out
of the picture (they have one fill color), and the shift of the buildings against the markers is found in two ways:
  1. the shift at which the markers of the buildings and of the food places lie deepest inside the buildings;
  2. the median vector from a marker to the center of the building it stands at (for every type of the markers).
The two ways agree on the east and are a few meters apart on the north-south direction (the markers are put by hand, not
exactly in the middle), so the result is good to 2 meters or so. The edges of the scheme against the edges of the imagery
were tried too and were dropped: the two pictures are too different for the correlation to be stable.

    pip install pillow numpy scipy
    python3 measure_offset.py [markers.json] [cache-dir]

Tiles are downloaded once into the cache directory (about 500 requests, with pauses; it is a one-off measurement).
"""
import json
import math
import os
import statistics
import sys
import time
import urllib.request

import numpy as np
from numpy.fft import fft2, fftshift, ifft2
from PIL import Image
from scipy import ndimage as ndi

Z = 18
TILE = 256
USER_AGENT = "mpei-hub-offset-check/1.0 (one-off measurement of the map offset)"
OSM_BUILDING_FILL = np.array([217, 208, 201])  # the fill of the buildings of the standard OpenStreetMap style
BLOCK = 1024
RADIUS = 100  # the largest shift that is looked for, in pixels (34 m)


def tile_position(lat, lng):
    n = 2 ** Z
    x = (lng + 180) / 360 * n
    y = (1 - math.log(math.tan(math.radians(lat)) + 1 / math.cos(math.radians(lat))) / math.pi) / 2 * n
    return x, y


def download(url, path):
    if os.path.exists(path):
        return
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with open(path, "wb") as out:
        out.write(urllib.request.urlopen(request, timeout=30).read())
    time.sleep(0.3)


def mosaic(markers, cache):
    os.makedirs(cache, exist_ok=True)
    lats = [m["location"]["lat"] for m in markers]
    lngs = [m["location"]["lng"] for m in markers]
    x0, y1 = tile_position(min(lats), min(lngs))
    x1, y0 = tile_position(max(lats), max(lngs))
    first_x, last_x, first_y, last_y = int(x0), int(x1), int(y0), int(y1)
    width = (last_x - first_x + 1) * TILE
    height = (last_y - first_y + 1) * TILE
    osm = np.zeros((height, width, 3), np.uint8)
    esri = np.zeros((height, width, 3), np.uint8)
    for x in range(first_x, last_x + 1):
        for y in range(first_y, last_y + 1):
            sources = (
                ("osm", osm, f"https://tile.openstreetmap.org/{Z}/{x}/{y}.png"),
                (
                    "esri",
                    esri,
                    f"https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{Z}/{y}/{x}",
                ),
            )
            for name, target, url in sources:
                path = os.path.join(cache, f"{name}_{Z}_{x}_{y}")
                download(url, path)
                target[(y - first_y) * TILE:(y - first_y + 1) * TILE, (x - first_x) * TILE:(x - first_x + 1) * TILE] = (
                    np.array(Image.open(path).convert("RGB"))
                )
    return osm, esri, first_x, first_y


def building_mask(osm):
    close = np.abs(osm.astype(int) - OSM_BUILDING_FILL).sum(2) <= 12
    return ndi.binary_closing(close, iterations=3)  # the labels and the icons make holes in the fill


def deepest_shift(markers, osm, first_x, first_y, meters_per_pixel, types):
    """The shift (east, north) in meters of the markers of [types] at which they lie deepest inside the buildings."""
    filled = building_mask(osm)
    inside = ndi.distance_transform_edt(filled)
    outside = ndi.distance_transform_edt(~filled)
    points = []
    for marker in markers:
        if marker["type"] in types:
            px, py = tile_position(marker["location"]["lat"], marker["location"]["lng"])
            points.append(((px - first_x) * TILE, (py - first_y) * TILE))
    best = None
    for north in np.arange(-10, 10.01, 0.5):
        for east in np.arange(-10, 12.01, 0.5):
            score = 0.0
            for x, y in points:
                xi = int(round(x + east / meters_per_pixel))
                yi = int(round(y - north / meters_per_pixel))
                # being deep inside a building is good (up to 8 m), being outside of it is bad
                score += min(inside[yi, xi] * meters_per_pixel, 8.0) - outside[yi, xi] * meters_per_pixel
            if best is None or score > best[0]:
                best = (score, float(east), float(north))
    return best[1], best[2]


def markers_shift(markers, osm, first_x, first_y, meters_per_pixel):
    """Where the buildings of the scheme are against the markers: (east, north) in meters, by the types of the markers."""
    filled = building_mask(osm)
    labels, count = ndi.label(filled)
    centers = ndi.center_of_mass(filled, labels, range(1, count + 1))
    areas = ndi.sum(filled, labels, range(1, count + 1))
    _, nearest = ndi.distance_transform_edt(~filled, return_indices=True)
    by_type = {}
    for marker in markers:
        px, py = tile_position(marker["location"]["lat"], marker["location"]["lng"])
        x, y = (px - first_x) * TILE, (py - first_y) * TILE
        label = labels[nearest[0][int(y), int(x)], nearest[1][int(y), int(x)]]
        if label == 0 or not 400 <= areas[label - 1] <= 200_000:
            continue
        cy, cx = centers[label - 1]
        by_type.setdefault(marker["type"], []).append(((cx - x) * meters_per_pixel, -(cy - y) * meters_per_pixel))
    return by_type


def main():
    markers_path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "markers.json")
    cache = sys.argv[2] if len(sys.argv) > 2 else "tiles-cache"
    markers = json.load(open(markers_path, encoding="utf-8"))
    osm, _, first_x, first_y = mosaic(markers, cache)
    latitude = statistics.mean(m["location"]["lat"] for m in markers)
    meters_per_pixel = 156543.03392 * math.cos(math.radians(latitude)) / 2 ** Z

    results = []
    for title, types in (("buildings and food", ("BUILDING", "FOOD")), ("all the markers", None)):
        wanted = types or {m["type"] for m in markers}
        east, north = deepest_shift(markers, osm, first_x, first_y, meters_per_pixel, wanted)
        results.append((east, north))
        print(f"1. {title:20}: the buildings of the scheme are {east:+.1f} m east, {north:+.1f} m north of the markers")

    vectors = markers_shift(markers, osm, first_x, first_y, meters_per_pixel)
    for kind, found in sorted(vectors.items()):
        print(
            f"2. {kind:9} ({len(found):2} markers): the center of the building is "
            f"{statistics.median(v[0] for v in found):+.1f} m east, {statistics.median(v[1] for v in found):+.1f} m north of the marker"
        )
    everything = [v for found in vectors.values() for v in found]
    results.append((statistics.median(v[0] for v in everything), statistics.median(v[1] for v in everything)))
    print(f"2. {'all':9} ({len(everything):2} markers): {results[-1][0]:+.1f} m east, {results[-1][1]:+.1f} m north")

    east = statistics.mean(r[0] for r in results)
    north = statistics.mean(r[1] for r in results)
    print(f"\nthe scheme lies {east:+.1f} m east, {north:+.1f} m north of the imagery: "
          f"OSM_FEATURES_EAST_METERS = {east:.1f}, OSM_FEATURES_NORTH_METERS = {north:.1f}")


if __name__ == "__main__":
    main()
