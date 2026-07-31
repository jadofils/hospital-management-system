# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Run

```bash
# Run the application (opens auth/login page at 1280×800)
./mvnw clean javafx:run

# Compile only
./mvnw clean compile

# Run tests (JUnit 5 configured, no tests written yet)
./mvnw test
```

Java 25 and JavaFX 21.0.6 are required. The Maven wrapper (`mvnw`) pins the correct version.

## Architecture

JavaFX desktop application using FXML + MVC. The app launches into `auth-pages.fxml`; successful login navigates to `dashboard.fxml`. Navigation is handled by `NavbarController` and `SidebarController`, which load new page FXML files and replace the current scene.

**Module:** `net.amalitech.hospitalmanagementsystem` (defined in `module-info.java`). Any new package that needs FXML injection must be added to `opens ... to javafx.fxml` there. Model classes also need `opens ... to javafx.base` for `PropertyValueFactory` reflection.

**Entry point:** `Main.java` extends `Application`, loads `auth-pages.fxml`, and attaches `global.css`.

### Package layout

```
hospital.management/
├── Main.java                          # JavaFX entry point
├── model/                             # JavaFX-property POJOs (Patient, Doctor, Appointment, Bill)
├── components/                        # Reusable UI component controllers
│   ├── navbar/NavbarController        # Top navbar — handles page navigation
│   ├── sidebar/SidebarController      # Left sidebar — handles page navigation + active highlight
│   ├── footer/FooterController
│   ├── breadcrumbs/BreadcrumbsController
│   ├── buttons/{Button,LoadingButton}Controller
│   ├── search/{SearchBar,AdvancedSearch}Controller
│   ├── table/PatientTableController   # FilteredList + Pagination
│   ├── form/PatientFormController     # 3-step multi-step form
│   ├── toast/ToastController          # Fade-out toast notifications
│   ├── modal/ModalController          # Overlay confirmation dialog
│   ├── calendar/CalendarController    # Month-grid calendar, fires onDateSelected
│   ├── stats/StatsWidgetController    # 4-card stats row
│   ├── profile/UserProfileController
│   ├── notification/NotificationController
│   └── searchdropdown/SearchableDropdownController
└── backend/
    └── pages/                         # Full-page controllers (one per route)
        ├── DashboardController
        ├── PatientsPageController
        ├── AppointmentsPageController
        ├── BillingPageController
        └── AuthPageController
```

### Resource layout

```
src/main/resources/hospital/management/
├── css/                               # All CSS lives here (global.css + per-component files)
└── frontend/
    ├── components/                    # One subfolder per component: <comp>/<comp>.fxml
    │   ├── navbar/, footer/, sidebar/, breadcrumbs/
    │   ├── buttons/  (button.fxml, loading-button.fxml)
    │   ├── search/   (search-bar.fxml, advanced-search.fxml)
    │   ├── table/    (patient-table.fxml)
    │   ├── form/     (patient-form.fxml)
    │   ├── card/     (patient-card.fxml, doctor-card.fxml, appointment-card.fxml)
    │   ├── spinner/, toast/, modal/
    │   ├── tabs/, accordion/, calendar/
    │   ├── stats/, profile/, notification/, searchdropdown/
    └── pages/                         # Full-page layouts (included by Main.java or navigation)
        ├── dashboard.fxml
        ├── patients-page.fxml
        ├── appointments-page.fxml
        ├── billing-page.fxml
        └── auth-pages.fxml
```

### Component pattern

1. FXML file declares layout, sets `fx:controller`, and includes its own CSS via `<stylesheets>`.
2. Controller uses `@FXML` injection; `initialize()` wires data and events.
3. Pages include reusable components with `<fx:include fx:id="sidebar" source="..."/>`. The injected controller is available as `@FXML private SidebarController sidebarController;` (convention: `fx:id + "Controller"`).

### Navigation

`NavbarController` and `SidebarController` both call a private `navigate(fxmlPath)` helper that loads the target FXML and replaces the current `Scene`. Each page controller calls `sidebarController.setActiveItem("key")` in `initialize()` to highlight the correct menu item.

### CSS design tokens (global.css)

Primary `#3498DB` · Secondary `#2C3E50` · Accent `#F1C40F` · Success `#27AE60` · Warning `#F39C12` · Danger `#E74C3C` · Light `#F8F9F9` · Muted `#E5E7E9`. Use the `.text-*`, `.bg-*` utility classes from `global.css` instead of hardcoding hex values in component CSS.

### Models

All models (`Patient`, `Doctor`, `Appointment`, `Bill`) use JavaFX `SimpleXxxProperty` fields with `getXxx()` / `xxxProperty()` accessors so `PropertyValueFactory` and `TableView` observe changes without extra wiring.

## Dependencies of note

| Library | Version | Purpose |
|---|---|---|
| JavaFX | 21.0.6 | GUI framework |
| ControlsFX | 11.2.1 | Enhanced controls |
| FormsFX | 11.6.0 | Form building |
| ValidatorFX | 0.6.1 | Form validation |
| Ikonli | 12.3.1 | Icon set |
| TilesFX | 21.0.9 | Dashboard tiles |
| JUnit 5 | 5.12.1 | Testing (not yet used) |