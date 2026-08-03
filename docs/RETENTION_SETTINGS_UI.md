# Retention Settings — UI Design Guide

Covers the admin page for configuring the cleanup daemon.
Backend integration is not yet wired — this document describes the screen,
its components, the integration checklist, and how the page connects to
the daemon classes in `backend/daemon/`.

---

## Files Created

| File | Purpose |
|---|---|
| `frontend/pages/retention-settings.fxml` | Page layout |
| `css/retention-settings.css` | All styles for this page |
| `pages/RetentionSettingsController.java` | Controller — spinners + stub handlers |

---

## Screen Layout

```
┌─────────────────────────────────────────────────────────────────────────┐
│  NAVBAR                                                                 │
├──────────┬──────────────────────────────────────────────────────────────┤
│          │  🔧 Retention & Cleanup Settings      [Reset] [Run Now] [Save]│
│          │  ─────────────────────────────────────────────────────────── │
│ SIDEBAR  │  ℹ Daemon is running. Next: 2026-07-31 08:00    Last: never  │
│          │  ─────────────────────────────────────────────────────────── │
│          │                                                               │
│          │  LEFT COLUMN (settings)    RIGHT COLUMN (preview + log)       │
│          │                                                               │
│          │  ┌─ User Inactivity ─────┐  ┌─ Preview Impact ─────────────┐ │
│          │  │ Days: [  90 ]  days   │  │ Users to deactivate:      —  │ │
│          │  │ Will deactivate: —    │  │ System log rows to delete: — │ │
│          │  └───────────────────────┘  │ Audit log rows to delete:  — │ │
│          │                             │ Log files to archive:      — │ │
│          │  ┌─ DB Log Retention ────┐  │ Stale archives to delete:  — │ │
│          │  │ Delete after: [90] d  │  └──────────────────────────────┘ │
│          │  │ Will delete: — rows   │                                   │
│          │  └───────────────────────┘  ┌─ Last Run Results ───────────┐ │
│          │                             │ > monospace dark log area    │ │
│          │  ┌─ File Log Archiving ──┐  │                              │ │
│          │  │ Max size: [10] MB     │  │                              │ │
│          │  │ Delete after: [15] d  │  │                              │ │
│          │  │ Dir: ~/.hms/logs/     │  └──────────────────────────────┘ │
│          │  └───────────────────────┘                                   │
│          │                                                               │
│          │  ┌─ Schedule ────────────┐                                   │
│          │  │ Every: [24] hours     │                                   │
│          │  │ Next run: —           │                                   │
│          │  └───────────────────────┘                                   │
│          │                                                               │
├──────────┴──────────────────────────────────────────────────────────────┤
│  FOOTER                                                                 │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## Components

### Status Banner
Full-width bar below the header. Shows daemon state, next scheduled run time,
and time of last run. Changes colour when a run is in progress (blue → amber).

### Settings Cards (left column)

Each card has:
- A coloured icon + title + description
- One or two `Spinner<Integer>` inputs with a unit label (days / MB / hours)
- An **impact chip** beside each spinner showing the count of items that will
  be affected (populated by the Preview query — stub for now)

| Card | Spinner(s) | Impact chip |
|---|---|---|
| User Inactivity | `inactiveUserDaysSpinner` (1–3650 days) | Users to deactivate |
| DB Log Retention | `dbLogRetentionSpinner` (1–3650 days) | Log rows to delete |
| File Log Archiving | `fileLogMaxSizeSpinner` (1–1024 MB) + `archiveRetentionSpinner` (1–365 days) | Archives on disk count |
| Schedule | `intervalHoursSpinner` (1–168 hours) | Next run time |

### Preview Impact Panel (right column, top)

Shows five row counters (populated by clicking **Refresh Preview**):

| Row | Data source |
|---|---|
| Users to deactivate | `UserInactivityCleaner.previewCount(policy)` |
| System log rows | `SELECT COUNT(*) FROM system_logs WHERE created_at < NOW() - interval` |
| Audit log rows | `SELECT COUNT(*) FROM audit_log WHERE created_at < NOW() - interval` |
| Log files to archive | Scan `~/.hms/logs/` for `.log` files exceeding size threshold |
| Stale archives to delete | Scan `~/.hms/logs/` for `.log.gz` files beyond retention days |

### Last Run Results (right column, bottom)

Dark monospace `TextArea` (non-editable). Displays the summaries returned by
each `CleanupTask.run()`. New lines are appended at the bottom so the most
recent run always shows at the end.

---

## Action Buttons

| Button | Style | Action |
|---|---|---|
| Reset Defaults | Outline | Sets all spinners back to `RetentionPolicy.DEFAULT_*` constants |
| Run Now | Warning (amber) | Triggers `DatabaseCleanupDaemon.runNow()` — disables itself while running |
| Save Settings | Primary (blue) | Saves to `RetentionPolicyStore` + restarts daemon with new interval |
| Refresh Preview | Ghost (small) | Runs preview queries and fills the five count labels |
| Clear | Ghost (small) | Clears the last run log `TextArea` |

---

## Spinner Behaviour

All spinners are `editable = true`. A `focusedProperty` listener commits
typed values on focus-lost (calls `spinner.increment(0)`). This is necessary
because JavaFX editable spinners do not auto-commit typed values without this.

Range constraints prevent invalid settings at the UI level:

| Spinner | Min | Max |
|---|---|---|
| Inactive user days | 1 | 3 650 (~10 years) |
| DB log retention | 1 | 3 650 |
| File log max size | 1 | 1 024 MB |
| Archive retention | 1 | 365 |
| Cleanup interval | 1 | 168 (one week) |

---

## Integration Checklist

All integration points are marked with `// TODO` in the controller.
Wire these when the services are ready:

```
initialize()
  ├── RetentionPolicyStore.load()            → populate all spinners
  ├── DatabaseCleanupDaemon status           → populate statusLabel + nextRunLabel
  └── EventBus.subscribe(DATA_CLEANING_*)   → update log area + banner + runNowBtn

onSave()
  ├── RetentionPolicyStore.save(policy)
  └── DatabaseCleanupDaemon.restart()

onRunNow()
  ├── DatabaseCleanupDaemon.runNow()
  └── disable runNowBtn until DATA_CLEANING_COMPLETED fires

onPreview()
  ├── UserInactivityCleaner.previewCount(policy)  → previewUserCount
  ├── COUNT query on system_logs                  → previewSysLogCount
  ├── COUNT query on audit_log                    → previewAuditLogCount
  └── Files.list(LOG_DIR) scan                    → previewArchiveCount, previewDeleteCount

EventBus listener — DATA_CLEANING_COMPLETED
  ├── call onCleaningCompleted(summaries)
  ├── update lastRunLabel
  └── re-enable runNowBtn
```

---

## Adding to Sidebar Navigation

To make the page reachable from the sidebar (admin-only):

1. Add to `PageRoute` enum:
   ```java
   RETENTION("retention", "Cleanup Settings",
       "/hospital/management/frontend/pages/retention-settings.fxml")
   ```

2. Add a sidebar entry guarded by role:
   ```java
   if (AccessControl.isAdmin()) {
       // show the Cleanup Settings menu item
   }
   ```

3. In `RetentionSettingsController.initialize()`:
   ```java
   sidebarController.setActiveItem(PageRoute.RETENTION);
   ```

---

## CSS Design Tokens Used

| Token | Hex | Used for |
|---|---|---|
| Primary `#3498DB` | Blue | Save button, preview border, info icons |
| Warning `#F39C12` | Amber | Run Now button, user inactivity impact chip |
| Danger `#E74C3C` | Red | Delete impact counts |
| Success `#27AE60` | Green | Schedule card icon, next run chip |
| Secondary `#2C3E50` | Dark | Card titles, page title |
| Muted `#7F8C8D` | Grey | Descriptions, unit labels |
| Light `#F8F9F9` | Off-white | Card backgrounds, impact rows |