# Privacy Policy

**EV Charged Reminder** is a free, open-source Android application. This privacy policy explains how the app handles your data.

## Data Collection

EV Charged Reminder does **not** collect, transmit, or share any personal data. There are no accounts, no analytics, no ads, and no telemetry.

## Location Data

The app uses your device's location to detect proximity to your saved charger locations. All location data is processed entirely on your device and is never sent to any server or third party.

## Network Requests

The app makes the following network requests, all initiated by explicit user actions:

- **OpenStreetMap tile servers** — to display map tiles when you use the map picker to place a charger.
- **Nominatim (OpenStreetMap)** — to reverse-geocode coordinates into a human-readable address when adding a charger.
- **OpenChargeMap** — to suggest nearby public chargers and their specifications.

No personally identifiable information is included in these requests. No network requests are made in the background.

## Data Storage

All app data (cars, chargers, charging sessions) is stored locally on your device using an on-device database. No data is backed up to any cloud service by the app.

## Third-Party Services

The app uses Google Play Services for geofencing (detecting arrival at charger locations). Geofence registrations are handled locally by Google Play Services on your device. The app does not send location data to Google beyond what the Android operating system requires for geofencing to function.

## Children's Privacy

This app is not directed at children under 13 and does not knowingly collect any data from children.

## Changes to This Policy

Any changes to this privacy policy will be posted in this repository. The policy is effective as of the date of the last commit that modified this file.

## Contact

If you have questions about this privacy policy, please open an issue on the [GitHub repository](https://github.com/heikkitoivonen/ev-charged-reminder).
