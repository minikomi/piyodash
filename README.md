# PiyoDash

This Clojure/Datastar app is an unofficial, read-only client for the private sync
protocol used by PiyoLog Android 9.2.7. It updates SQLite and serves the family
dashboard in one process. PiyoLog does not publish this API, so an app update may
require changes here.

## Enroll the remote machine

On an **admin/owner phone**, open PiyoLog and create a **Share Code** (not a
Transfer Code). PiyoLog hides this option on phones that originally joined the
account through sharing:

1. Open **Settings → Share / Transfer**.
2. Choose the sharing option and create a one-time Share Code.
3. Note the PiyoLog ID shown beside it.

On the remote machine, copy this repository and run:

```sh
clojure -M:enroll
```

Enter the ID and one-time code at the prompts. The collector creates its own
revocable PiyoLog client, saves its credential in `.piyolog.json` with mode
`0600`, and downloads the initial dataset into `piyolog.sqlite3`.

Do not commit or copy `.piyolog.json`. Anyone with that file can read the shared
PiyoLog data. You can revoke the collector later from PiyoLog's shared-client
settings.

The enrollment command saves `.piyolog.json` with owner-only permissions and
performs the first full SQLite sync. The dashboard's Clojure `go-loop` performs
each later incremental sync; there is no separate Python process or scheduler.

## Data model

`sync_runs` keeps every raw API response for recovery and protocol debugging.
`records` contains the latest version of each object, including:

- `baby`
- `baby_event`
- `day_log`
- `food_record`
- `calendar_event`
- singleton documents such as `custom_event_info`

The original object is in `payload_json`; common fields are also extracted for
queries. Deleted records are retained with `deleted = 1` so dashboards can
exclude them without losing history.

Example event query:

```sql
SELECT
  baby_id,
  event_time,
  json_extract(payload_json, '$.type') AS event_type,
  json_extract(payload_json, '$.amount') AS amount,
  json_extract(payload_json, '$.memo') AS memo
FROM records
WHERE entity = 'baby_event' AND deleted = 0
ORDER BY event_time DESC;
```

## Run the iPad dashboard

The Clojure/Datastar app runs a `core.async` sync loop itself and reads the
SQLite database it updates. It reuses the credentials saved during `enroll`:

```sh
PIYOLOG_DB=/absolute/path/piyolog.sqlite3 \
TZ=Asia/Tokyo \
clojure -M:run
```

By default it uses `.piyolog.json`, `piyolog.sqlite3`, and a 60-second sync
interval. Override them when running elsewhere:

```sh
PIYOLOG_CONFIG=/secure/path/piyolog.json \
PIYOLOG_DB=/var/lib/piyodash/piyolog.sqlite3 \
PIYOLOG_SYNC_INTERVAL=60 \
clojure -M:run
```

Open `http://REMOTE-MACHINE-IP:3000` in Safari on the iPad. Use Safari's
**Share → Add to Home Screen** for a full-screen app-like display.

Every panel is bilingual: Japanese is prominent, with English directly below.
Timers themselves stay compact and language-neutral (`2h 10m`, `45m 12s`).
On an iPad the dashboard remains a 2×2 grid in either orientation. On a phone
in portrait orientation, the four panels become a vertically scrolling stack.

The screen is always split into four panels. Sleep/wake and diaper are on top;
milk and breastfeeding are on the bottom:

- current **awake** or **asleep** duration, determined by the newest sleep/wake event;
- time since bottle milk;
- time since breastfeeding;
- time since the newest pee or poop, including which kind it was.

Open `/solidfoods` for the solid-food history. It recognizes ingredients in
type-9 meal notes across
Japanese commas, spaces, newlines, and common recipe phrases. The page shows
the first and latest matching dates, meal count, foods not yet found in the
notes, and a separate allergen checklist. “Not recorded” is not a diagnosis.

Datastar requests a fresh server-rendered dashboard every second. The database
remains the source of truth; there is no browser-side application model.

If the account has multiple babies, select one with its `baby_id`:

```sh
sqlite3 piyolog.sqlite3 \
  "select record_key, json_extract(payload_json, '$.nickname') from records where entity='baby';"

BABY_ID=the-id PIYOLOG_DB=/absolute/path/piyolog.sqlite3 clojure -M:run
```

The server listens on all network interfaces. Keep it on a trusted LAN or
behind a private VPN/reverse proxy because the dashboard contains personal
care data and currently has no login screen. Set `PORT` to change port 3000.

## Privacy before committing

The repository ignores `.piyolog.json`, SQLite databases and their WAL/SHM
files, `.env` files, private keys, caches, and build output. Do not add real
PiyoLog IDs, hostnames, credentials, database snapshots, or meal data to Git.
The values in tests are synthetic placeholders.
