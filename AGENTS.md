# AGENTS.md — EV Charged Reminder

## Build & Commit Rules
- **Unit tests must be run and pass before every git commit.** No exceptions.
- Run tests with `./gradlew test` from the project root.
- AI agents must add a `Co-Authored-By` trailer to the commit message using the agent's own identity, e.g.:
  - Claude: `Co-Authored-By: Claude <noreply@anthropic.com>`
  - Amp: `Co-Authored-By: Amp <amp@ampcode.com>`

## Project Intent
- This is a **free, open-source** app (MIT License).
- Will be published on **GitHub** and the **Google Play Store**.
- **No monetization, no ads** — do not introduce any ad SDKs, analytics trackers, or payment libraries.

## API Usage Constraints
- **Nominatim (OSM)**: must set `User-Agent: EVChargedReminder/1.0` on all requests; rate-limit to **1 request per second** max.
- **OpenChargeMap**: free tier; be conservative with request frequency.
- **OSM map tiles (osmdroid)**: must display **OpenStreetMap attribution** on all map views.

## Design Principles
- Keep the app **local-only** — no cloud accounts, no sign-in, no telemetry.
- Prefer **privacy-respecting** behavior: location data stays on-device, no background network calls unless the user explicitly triggered an action.
- Do not add dependencies without checking they are compatible with the MIT license.
