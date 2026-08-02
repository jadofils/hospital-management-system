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

JavaFX desktop application using FXML + MVC. The app launches into `home-page.fxml` (sign-in); successful login navigates to `dashboard.fxml`. Navigation is handled by `NavbarController` and `SidebarController`, which load new page FXML files and replace the current scene. There is no `module-info.java` in this project — it's a classic classpath app, not a JPMS module, so no `opens` directives are needed for new packages/reflection.

**Entry point:** `Main.java` extends `Application`, loads `home-page.fxml` (via `AppConfig.HOME_FXML_PATH`), and attaches `global.css`.

Both the page-controller and component-controller packages are organized into **domain subfolders that mirror `backend/dto/`'s grouping** (`auth`, `patient`, `doctor`, `clinical`, `lab`, `pharmacy`, `finance`, `log`), plus a few UI-specific groups (`admin`, `analytics`, `dashboard` for pages; `shared/*` for cross-domain reusable components). Shared abstract bases (`BasePageController`, `PaginatedTableController`) live at the root of their respective package, one level above the domain subfolders — every domain-specific controller extends one of these and must explicitly `import` it (they're no longer same-package).

### Package layout

```
hospital.management/
├── Main.java                              # JavaFX entry point
├── enums/PageRoute.java                   # Every navigable route: key, label, fxml path, allowed roles
├── model/                                 # Domain POJOs (Patient, Doctor, Appointment, Invoice, ...) — mostly
│                                           #   plain getter/setter POJOs; Invoice uses JavaFX properties
├── pages/                                 # Full-page controllers, one subfolder per domain
│   ├── BasePageController                 # Shared base: injects sidebar/toast/confirm-modal/form-dialog,
│   │                                       #   exposes toast()/toastSuccess()/toastError()/confirm() helpers
│   ├── auth/        AuthPageController, UsersPageController, ProfilePageController
│   ├── patient/     PatientsPageController, PatientDetailController
│   ├── doctor/      DoctorsPageController, DepartmentsPageController, ScheduleController, ReferralsController
│   ├── clinical/    AppointmentsPageController, MedicalRecordsController
│   ├── lab/         LabOrdersController
│   ├── pharmacy/    PrescriptionsController, PharmacyController
│   ├── finance/     InvoicePageController
│   ├── log/         AuditLogsController, SystemLogsController
│   ├── admin/       RetentionSettingsController
│   ├── analytics/   AnalyticsController
│   ├── dashboard/   DashboardController
│   └── components/                        # Reusable UI component controllers
│       ├── PaginatedTableController        # Shared generic base: pagination + filter + row-action wiring
│       │                                   #   for every entity table controller below
│       ├── patient/    PatientTableController, VitalSignTableController, PatientAllergyTableController
│       ├── auth/       UserTableController, UserSessionTableController
│       ├── doctor/     DoctorTableController, DepartmentTableController, DoctorScheduleTableController,
│       │               ReferralTableController
│       ├── clinical/   AppointmentTableController, MedicalRecordTableController
│       ├── lab/        LabOrderTableController
│       ├── pharmacy/   PrescriptionTableController, MedicalInventoryTableController
│       ├── finance/    InvoiceTableController
│       ├── log/        AuditLogTableController, SystemLogTableController
│       └── shared/                         # Cross-domain reusable components, grouped by concern
│           ├── layout/    NavbarController, SidebarController, RightSidebarController, FooterController,
│           │              BreadcrumbsController
│           ├── feedback/  ToastController, ModalController, FormDialogController (Add/Edit dialog)
│           ├── search/    AdvancedSearchController, SearchableDropdownController
│           └── widgets/   CalendarController, StatsWidgetController
└── backend/
    ├── dto/                                # Same domain grouping as pages/ above (auth, patient, doctor, ...)
    ├── mapper/                             # Same domain grouping — one mapper subfolder per dto/ subfolder
    ├── model/, service/, dao/, config/, exceptions/, daemon/, cache/, utils/
```

### Resource layout

```
src/main/resources/hospital/management/
├── css/                               # All CSS lives here (global.css + per-page/per-component files)
└── frontend/
    ├── components/                    # One subfolder per component: <comp>/<comp>.fxml
    │   ├── navbar/, footer/, sidebar/, right-sidebar/, breadcrumbs/
    │   ├── search/     (advanced-search.fxml), searchdropdown/
    │   ├── table/      (one <entity>-table.fxml per entity table controller — patient-table.fxml,
    │   │               doctor-table.fxml, invoice-table.fxml, ...)
    │   ├── formdialog/ (form-dialog.fxml — shared Add/Edit modal used by every page)
    │   ├── modal/      (modal.fxml — shared confirm dialog)
    │   ├── toast/      (toast.fxml — shared floating notification)
    │   ├── spinner/, calendar/, stats/, card/, tabs/, accordion/
    └── pages/                         # Full-page layouts. Root is a StackPane wrapping the page's
                                        #   BorderPane content plus form-dialog/confirm-modal/toast includes
                                        #   (see patients-page.fxml as the reference shape).
        ├── home-page.fxml              # Sign-in (fx:controller = AuthPageController)
        ├── dashboard.fxml
        ├── patients-page.fxml, patient-detail-page.fxml
        ├── appointments-page.fxml, ...  # one -page.fxml per PageRoute
```

### Component pattern

1. FXML file declares layout, sets `fx:controller` to the fully-qualified controller class (including its domain subfolder, e.g. `hospital.management.pages.components.patient.PatientTableController`), and includes its own CSS via `<stylesheets>`.
2. Controller uses `@FXML` injection; `initialize()` wires data and events.
3. Pages include reusable components with `<fx:include fx:id="sidebar" source="..."/>`. The injected controller is available as `@FXML private SidebarController sidebarController;` (convention: `fx:id + "Controller"`) — inherited for free from `BasePageController` for the sidebar/toast/modal/form-dialog quartet; page-specific includes (e.g. an entity table) are injected directly on the page controller.
4. Every CRUD page's Add/Edit flow reuses the shared `FormDialogController` (`open()`/`addField()`/`setError()`/`close()`) rather than a bespoke dialog per page. Every entity table controller extends `PaginatedTableController<T>`, implementing only `configureColumns()` and `matches()` — pagination, filtering, and row-action wiring (`wireActionsColumn`) live in the base class.

### Navigation

`NavbarController` and `SidebarController` both call a private `navigate(fxmlPath)` helper that loads the target FXML and replaces the current `Scene`. `SidebarController.navigate` also swaps the clicked nav button's icon for a small spinner (`ProgressIndicator`) until the new scene has loaded. Each page controller calls `sidebarController.setActiveItem(PageRoute.X)` in `initialize()` to highlight the correct menu item.

### CSS design tokens (global.css)

Primary `#3498DB` · Secondary `#2C3E50` · Accent `#F1C40F` · Success `#27AE60` · Warning `#F39C12` · Danger `#E74C3C` · Light `#F8F9F9` · Muted `#E5E7E9`. Use the `.text-*`, `.bg-*` utility classes from `global.css`, and the shared `.primary-button`/`.secondary-button`/`.danger-button`/`.row-action-btn` classes from `buttons.css`, instead of hardcoding hex values or one-off button styles in a page's own CSS.

### Models

Model shape is mixed: most (`Patient`, `Doctor`, `Department`, `Appointment`, `LabOrder`, ...) are plain getter/setter POJOs; `Invoice` (in `backend/model/finance`) uses JavaFX `SimpleXxxProperty` fields instead. `PropertyValueFactory` works with either style. Note: the backend service/DAO layer (`backend/service/**/*ServiceImpl.java`) is currently entirely stubbed (`throw new UnsupportedOperationException("Not implemented yet")`) — every page's Add/Edit/Delete flow operates on an in-memory `List<T>` inside the page controller until that layer is implemented.

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