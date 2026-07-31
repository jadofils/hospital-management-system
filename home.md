# Hospital Management System – Home/Landing Page Redesign

## Goal
Transform `test-page.fxml` into a **professional home/landing page** that serves as the entry point for the Hospital Management System.
The page must be **responsive**, visually appealing, and structured with clear sections.

---

## Layout Structure

### Navigation (Top)
- **Navbar (left-aligned)**: hospital logo + system name
- **Navbar (center)**: main navigation links (Dashboard, Patients, Doctors, Appointments, Billing)
- **Navbar (right-aligned)**: user profile avatar + dropdown (Settings, Logout)

### Hero Section (First Section)
- Full-width **background image or video** (hospital theme, healthcare visuals)
- Overlay with:
  - Headline: “Welcome to Hospital Management System”
  - Subheadline: “Efficient, modern, and reliable healthcare management”
  - Call-to-action buttons: **Login** (primary), **Register** (secondary)

### Core Features Section
- Three or four **cards** horizontally aligned:
  - Patient Management
  - Doctor Scheduling
  - Appointment Booking
  - Billing & Reports
- Each card includes an icon, title, and short description.

### Dashboard Preview Section
- Split layout:
  - Left: **Statistics widgets** (patients today, appointments scheduled, revenue)
  - Right: **Charts** (bar/pie/line for analytics)

### Interactive Section
- Tabs or Accordions:
  - Patient details (history, prescriptions, billing)
  - Doctor availability
  - FAQs

### Footer
- Left: © 2026 Hospital Management System
- Center: Quick links (Privacy Policy, Terms of Service)
- Right: Contact info (email, phone, social icons)

---

## Additional Pages

### Login Page
- Centered card with:
  - Logo + system name
  - Username/email field
  - Password field
  - Login button (primary)
  - “Forgot password?” link
  - “Register” link for new users
- Background: subtle hospital-themed image or gradient

### Registration Page
- Multi-step form:
  - Step 1: Personal info (name, email, phone)
  - Step 2: Role selection (Doctor, Admin, Patient)
  - Step 3: Credentials (username, password)
- Progress indicator at top

### Dashboard Page
- Sidebar navigation (Patients, Doctors, Appointments, Billing, Reports)
- Main content area with:
  - Statistics widgets
  - Charts
  - Tables (recent patients, upcoming appointments)

---

## Styling Guidelines
- Use **global.css** for:
  - Color palette (primary blue, secondary dark gray, accent yellow, danger red, success green)
  - Typography (headings, body text, labels)
  - Spacing, border radius, shadows
- Each component (navbar, footer, cards, tables, forms) has its own scoped CSS file.
- Ensure responsiveness:
  - Navbar collapses into hamburger menu on small screens
  - Cards stack vertically on mobile
  - Charts resize dynamically
  - Footer adapts to single-column layout on mobile

---

## Deliverables
- Updated `test-page.fxml` → redesigned as home/landing page
- `login.fxml` → professional login page
- `register.fxml` → multi-step registration page
- `dashboard.fxml` → main dashboard with widgets, charts, tables
- Scoped CSS files for each component + shared `global.css`
