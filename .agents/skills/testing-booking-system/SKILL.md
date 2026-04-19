# Testing the BookMyShow booking-system app

Reference for running and testing the `mishraachandan/booking-system` stack end-to-end (Spring Boot + Angular + Keycloak + Postgres).

## Devin Secrets Needed
None for dev-only local spin-up. All ports are local and the DB password is dev-only (`booking`).
Razorpay keys are intentionally left as the placeholder `RAZORPAY_KEY_NOT_SET` so the backend takes the dummy-payment bypass. Do NOT request real Razorpay credentials for testing — the UI already skips the Razorpay popup when it sees the placeholder key.

## Stack & ports
- Backend: Spring Boot on `localhost:8080` (`./gradlew bootRun` from repo root, reads `.env.dev`).
- Frontend: Angular 18 dev server on `localhost:4200` (`cd booking-system-ui && npm start`, proxies `/api` to `:8080`).
- Keycloak: `localhost:8180`, realm `booking-system` (docker-compose service `keycloak`).
- Keycloak DB: docker-compose service `keycloak-db`.
- App DB (Postgres): `localhost:5433`, db/user/password all `booking` (docker-compose service `app-db`).

Start order: `docker compose up -d keycloak-db keycloak app-db` → wait for Keycloak realm import (log line `Realm 'booking-system' created`) → backend → frontend.

## Test users
- Keycloak admin: `admin@bookmyshow.dev / Admin@1234` (realm admin; can create realm users).
- Typical browser test user: any realm user (the UI shows the `given_name` claim, e.g. "Super").
- A user's bookings live under their Keycloak `sub` claim; expect different users to see different booking lists.

## Seed data quick reference
- `src/main/resources/seed_data.sql` seeds cities/cinemas/screens/movies/shows (idempotent via `ON CONFLICT`). Triggered by Spring Boot on first boot when `HIBERNATE_DDL_AUTO=update` (default in `.env.dev`).
- Add-ons and movie metadata (trailer, cast, rating) may be missing after a fresh DDL. If `GET /api/v1/addons` returns `[]` or the movie detail page shows no rating/cast/trailer, re-apply the add-ons/metadata seed:
  1. Write the SQL to a file on the VM (NOT via heredoc — `docker exec ... <<SQL` has been flaky; prefer file + `docker cp`).
  2. `docker cp /tmp/seed_addons.sql app-db:/tmp/seed_addons.sql`
  3. `docker exec app-db psql -U booking -d booking -f /tmp/seed_addons.sql`
- Expected add-on names used in tests: `Salted Popcorn (Large)` (₹250, FOOD), `Coke (500ml)` (₹150, BEVERAGE), `Combo: Popcorn + Pepsi` (₹350, COMBO).

## Golden-path E2E flow (covers all three PR #12 features in one pass)
1. Home (`/`) → pick city "Mumbai" in the dropdown.
2. Click a movie tile → URL should change to `/movies/<id>` (not inline expand). Verify rating chip, cast chips, YouTube iframe, future showtimes.
3. Click a future showtime → log in via Keycloak if prompted.
4. Seat selection: pick any green (REGULAR) seat in rows C or D (₹200). Expand "Add food & beverages" panel, `+` one Salted Popcorn and one Coke. Footer must read `₹600` (200 + 250 + 150).
5. Click `Book Now →` → booking summary shows three rows: `Seats ₹200`, `Add-ons 🍿 ₹400`, `Total Amount ₹600`.
6. Click `💳 Pay Now ₹600`. No Razorpay popup should open (dummy path). `POST /api/payments/create-order` returns `keyId=RAZORPAY_KEY_NOT_SET` and `razorpayOrderId` prefixed `order_dummy_`; `POST /api/payments/verify` returns `status:"CONFIRMED"`. UI shows `Booking Confirmed! · Booking #<n> · Total Paid: ₹600`.
7. Navigate to `/my-bookings`. Tabs render as `Upcoming (N) / Past (M)` with live counts. The new booking appears under Upcoming with a 🍿 Add-ons list showing both line items with snapshotted prices.
8. Click `Cancel Booking` → an in-app modal (NOT `window.confirm`) opens with the refund policy bullets and a T&C checkbox. The confirm button must be disabled until the checkbox is ticked.
9. Tick T&C → confirm → booking status flips to `Cancelled` and the card moves Upcoming → Past; counts update (N→N-1, M→M+1).

## Known gotchas
- **Legacy JJWT filter vs Keycloak RS256**: `JwtUtil.validateJwtToken` used to throw `DecodingException` on Keycloak base64url tokens, which bubbled up as `401` before OAuth2 resource server could validate the token. Fix landed in commit `157cb34` (wide catch, DEBUG log). If authenticated calls start returning 401 again, check this filter first.
- **Dummy-payment detection**: booking-summary component treats any of `RAZORPAY_KEY_NOT_SET`, `REPLACE_ME`, or a `razorpayOrderId` starting with `order_dummy_` as the dummy path. Don't regress this set when editing `booking-summary.component.ts`.
- **`OpenSessionInView` is OFF**: any serialization of a lazy-loaded JPA relation outside the transaction throws `LazyInitializationException`. When extending `BookingService`/`PaymentService`, initialize proxies inside the `@Transactional` method (there's already a `findBookingInitialized(...)` + `initializeBookingProxies(...)` pattern to reuse).
- **`seed_data.sql` does NOT populate add-ons or the new movie columns (trailer/cast/rating)**. If a test depends on them, re-seed explicitly (see above).
- **Fresh DDL after pulling a branch with new entities**: `HIBERNATE_DDL_AUTO=update` creates new tables but does not re-run `seed_data.sql`. Restart the backend process cleanly (kill the stale PID first) so Hibernate picks up new entity classes; then re-seed.
- **Razorpay key placeholders vary**: `.env.dev` may say `RAZORPAY_KEY_NOT_SET` or `REPLACE_ME`. Both trigger the dummy path — don't "fix" them to real keys for testing.

## Recording tips
- Maximize the browser before recording: `wmctrl -r :ACTIVE: -b add,maximized_vert,maximized_horz`.
- Use `computer(action="record_annotate")` with structured types:
  - `type="setup"` for navigation / pre-conditions.
  - `type="test_start"` with `test="It should ..."` (Jest style) at the start of each named test.
  - `type="assertion"` with `test="It should ..."`, `test_result="passed|failed|untested"`, `assertion="<what was verified>"` (consolidated, under ~80 chars).
- Keep the walkthrough under ~2 minutes if possible — single continuous flow that proves all three features beats three separate clips.

## Useful one-liners
- Check backend is listening: `ss -tlnp 2>/dev/null | grep 8080`.
- Tail backend log for lazy-init regressions: `grep -c LazyInitializationException /tmp/backend.log`.
- Peek at add-ons: `curl -s localhost:8080/api/v1/addons | jq '.[].name'`.
- Peek at movie detail (public): `curl -s localhost:8080/api/v1/movies/1 | jq`.
