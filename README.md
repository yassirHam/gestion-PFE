# Gestion Des PFE

Gestion Des PFE is a Java web application for managing PFE students, professors, supervisor assignments, soutenance planning, exports, and PV documents.

The application uses JSP/Servlets for the web layer, Hibernate for persistence, MySQL for storage, Apache POI for Excel and DOCX handling, iText for PDF exports, and an optional NVIDIA NIM/OpenAI-compatible NLP service to help classify project subjects by professor specialty.

## Updates Video link
https://drive.google.com/file/d/1DudSqkPTZCqGqsL695Y4HKcbeB965xlh/view?usp=sharing

## Main Features

- **Unified Excel Import**: Import student lists, professors, and optional rooms from a *single* multi-sheet Excel file.
- **Intelligent Load Balancing**: Generate student-to-professor affectations using a min-heap algorithm, guaranteeing a maximum difference of 1 project between any two professors.
- **Dynamic Configuration**: A dedicated configuration page to define working days, time slots, break durations, and hard/soft constraints for planning generation.
- **Automated Planning Generation**: Generate soutenance schedules respecting professor availability, room constraints, and jury composition. Live recommendations warn of feasibility issues.
- **AI-Powered Matching**: Optional NLP service analyzes project subjects to assign the most relevant "Module Enseigné" for jury selection.
- **Dashboard & Analytics**: View balanced statistics by professor and filiere, with responsive charts.
- **Salle Management**: Add, delete, or bulk-import rooms, with duplicate detection and usage protection.
- **Exports & Documents**: Export affectations and planning to PDF and DOCX. Keep planning export history. Generate and download individual or bulk PV documents.
- **Modern UI**: Clean, responsive interface with full-screen loading animations, dark navbars, and clear visual feedback.

## Tech Stack

- Java 21
- Maven
- Servlet API 4.0.1
- JSP 2.3.3
- JSTL 1.2
- Hibernate ORM 6.4.4.Final
- MySQL Connector/J 8.3.0
- Apache POI 5.2.5
- iText 7.2.5
- Log4j 2.20.0
- LangChain4j 0.36.2
- Tomcat 9 recommended

## Project Structure

```text
projet/
  pom.xml
  README.md
  src/main/java/
    controller/
      FrontController.java
    dao/
      *DAO.java
      *DAOImpl.java
    entities/
      Affectation.java
      Etudiant.java
      FichierListe.java
      Jury.java
      Professeur.java
      Salle.java
      Soutenance.java
      PlanningConfig.java
      Constraint*.java
    services/
      AffectationService*.java
      NlpService*.java
      PfeService*.java
      PlanningService*.java
      VerificationService*.java
      RecommendationService*.java
    util/
      AppListener.java
      ExcelImporter.java
      HibernateUtil.java
  src/main/resources/
    hibernate.cfg.xml
    config.properties.example
    templates/
      template_pv.docx
  src/main/webapp/
    index.jsp
    dashboard.jsp
    affectation.jsp
    planning.jsp
    config.jsp
    pv.jsp
    images/
      logo.png
    WEB-INF/web.xml
```

## Architecture

The app follows a MVC-style organization.

`FrontController.java` is the single servlet mapped to `*.do`. It receives requests, calls service methods, prepares request attributes, and forwards to JSP pages or streams generated files.

The JSP files are responsible for the UI:

- `index.jsp`: landing/home page.
- `dashboard.jsp`: statistics, search, and verification display.
- `affectation.jsp`: unified Excel imports, affectation controls, and exports.
- `config.jsp`: dynamic planning configuration (days, slots, constraints).
- `planning.jsp`: planning generation, room selection, planning exports, and history.
- `pv.jsp`: PV generation and downloads.

The service layer contains the business logic:

- `PfeServiceImpl`: main facade used by the controller, handles balanced affectations.
- `PlanningServiceImpl`: generates soutenance schedules based on dynamic configurations and constraints.
- `NlpServiceImpl`: optional AI subject analysis.
- `VerificationServiceImpl`: checks generated data for issues against constraints.
- `RecommendationServiceImpl`: live feasibility hints and metrics before generating a planning.

The DAO layer wraps Hibernate access for each entity.

## Database

The application uses MySQL. The current Hibernate configuration is in `src/main/resources/hibernate.cfg.xml`.

Default connection:

```properties
url=jdbc:mysql://localhost:3306/GestionPFE?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
username=root
password=
```

Hibernate is configured with `hibernate.hbm2ddl.auto=update`, which creates or updates the `GestionPFE` schema automatically.

## Setup

### 1. Requirements

- JDK 21
- Maven
- MySQL Server
- Tomcat 9

### 2. Configure MySQL

Start MySQL and ensure the credentials match `hibernate.cfg.xml`.

### 3. Configure Optional NLP

The NLP integration helps the planning algorithm match project subjects with professor modules.

Copy `src/main/resources/config.properties.example` to `src/main/resources/config.properties`.

Fill it with:

```properties
nvidia.api.key=YOUR_API_KEY
nvidia.base.url=https://integrate.api.nvidia.com/v1
nvidia.model=meta/llama-3.3-70b-instruct
```

If not configured, the app falls back gracefully.

### 4. Build & Deploy

```bash
mvn clean package
```

Deploy the generated `target/Gestion-Des-PFE.war` to Tomcat 9 `webapps/`. Access at `http://localhost:8080/Gestion-Des-PFE/`.

## Application Workflow

### 1. Import Data

Go to the Affectation page and upload a **single unified Excel file** containing:
- One sheet per filiere (e.g., "GI", "ID").
- A `Professeurs` sheet.
- An optional `Salles` sheet.

Student sheets expect columns:
`A: CNE | B: NOM | C: PRÉNOM | D: EMAIL | E: CNE BINÔME | F: SUJET PFE`

Professor sheet expects:
`A: NOM | B: PRÉNOM | C: DISCIPLINE | D: MODULE ENSEIGNÉ`

### 2. Generate Affectations

The affectation algorithm loads students and professors, then assigns students using a **min-heap load balancer**. This ensures no professor is overloaded, keeping the maximum difference in assigned projects to exactly 1.

### 3. Configure Planning

Go to the Configuration page to set up:
- Planning days and time slots.
- Defense duration and rest periods.
- Hard and soft constraints (e.g., English defenses require English-speaking jury members).

### 4. Generate Planning

The Planning page uses existing affectations, available rooms, and constraints to generate soutenance schedules. Live recommendations highlight feasibility before generation. 

### 5. Export Planning & PVs

Export your data to PDF or DOCX. Use the PVs page to generate individual or bulk (ZIP) PV documents using the included Word template.

## Main Routes

| Route | Purpose |
| --- | --- |
| `/dashboard.do` | Dashboard and statistics |
| `/affectation.do` | Affectation page and unified import |
| `/config.do` | Planning configuration |
| `/lancerAffectation.do` | Generate balanced affectations |
| `/planning.do` | Planning page |
| `/lancerPlanning.do` | Generate planning |
| `/pv.do` | PV generation page |
| `/templateData.do` | Download unified Excel template |
| `/exportPdf.do` | Export affectations as PDF |
| `/exportDocx.do` | Export affectations as DOCX |
| `/planningPdf.do` | Export planning as PDF |
| `/planningDocx.do` | Export planning as DOCX |



---

## Operational Administration Layer (v2)

The platform was extended to behave as a full soutenance management platform
rather than a one-shot generator. Every change is additive: the original
workflow (import → affect → plan → export) keeps working unchanged.

### What's new

| Concern | Implementation |
| --- | --- |
| **Audit actor** | No authentication or login flow is enforced. `AuditContext` supplies a local operator actor so audit records and ownership metadata keep working. |
| **Sessions & versions** | `AcademicSession` ("Session Juin 2026") groups one or more `PlanningVersion`s ("Version 1", "Version finale"). Exactly one session is active and one version per session is current. Frozen / published versions become immutable. |
| **Validation lifecycle** | `LifecycleState` (DRAFT → PENDING_VALIDATION → VALIDATED → PUBLISHED → ARCHIVED, plus REJECTED) on every affectation and planning version. |
| **Approval chain** | `Approval` records every decision (request / approve / reject / publish / archive) with actor, role, timestamp and comment. |
| **Publication freeze** | Publishing a version locks every `Soutenance` it contains (`Soutenance.locked = true`, status `LOCKED`). The next planning regeneration preserves locked soutenances and only recomputes the rest. |
| **Manual overrides** | `ManualOverrideService` exposes lock/unlock soutenance, postpone, cancel, replace salle, swap a single jury member, force an encadrant, lock affectation, exclude/reinstate professor — without re-running the whole engine. |
| **Conflict resolution** | When a hard constraint is violated, the engine still surfaces the violations and unscheduled projects; locked soutenances are preserved so the operator can fix the rest. |
| **Exception management** | `SoutenanceException` records (student/prof absent, room unavailable, jury replacement, delay, cancellation). Reported through the planning grid, resolved with a chosen action (acknowledge / postpone / cancel / mark completed). |
| **Communication** | `Notification` table + `NotificationService` queues student and jury convocations, then dispatches them via SMTP when configured (plain Java SMTP/STARTTLS client, no extra dependency). When SMTP is off, notifications stay queued for review. |
| **Audit log** | `AuditLog` records generation, override, settings, validation and publication events with actor, target type/id, summary and remote IP. |
| **Governance** | `Department` entity owns rooms, professors, filière codes, quotas. Rooms gain `priority`, `available`, `capacity`, `equipment`, `campus`. Professors gain `grade`, `internal/external`, `languages`, `maxSoutenancesPerDay`, `vip`, `excluded`. `ProfesseurAvailability` lets each professor declare per-date AM/PM unavailability. |
| **Resource prioritization** | `Etudiant.priority` is honored by the planner (urgent students scheduled first). VIP professors are taken into account. High-priority salles are filled first. |
| **Multi-campus / multi-department** | Optional `campus` field on rooms and on `AppSettings.defaultCampus`; sessions can be scoped to one department. The single mono-admin model is replaced by per-department ownership. |
| **Richer professor constraints** | New constraint IDs in `ConstraintIds` / `ConstraintSet`: `MIN_PRESIDENT_GRADE`, `REQUIRE_EXTERNAL_MEMBER`, `MAX_SOUTENANCES_PER_HALFDAY`, `AVOID_CONSECUTIVE_DEFENSES`, `MAX_JURY_REPETITION`, `RESPECT_PROF_UNAVAILABILITY`, `RESPECT_LANGUAGE_REQUIREMENT`, `SPECIALTY_COMPATIBILITY`, `RESPECT_SESSION_DEADLINE`, `FORBID_CONSECUTIVE_SLOTS`. Each one is editable in the configuration page (the existing form iterates `ConstraintSet.defaults()`). |

### New routes

```
Sessions / versions           /sessions.do  /createSession.do  /activateSession.do  /closeSession.do
                              /createVersion.do  /setCurrentVersion.do  /deleteVersion.do
Approval chain                /approvals.do  /requestApproval.do  /approve.do  /reject.do
                              /publishVersion.do  /freezeVersion.do  /archiveVersion.do
Manual overrides              /lockSoutenance.do  /unlockSoutenance.do  /cancelSoutenance.do
                              /postponeSoutenance.do  /replanSoutenance.do  /replaceSalle.do
                              /swapJuryMember.do  /lockJury.do
                              /forceAffectation.do  /lockAffectation.do  /transitionAffectation.do
                              /excludeProf.do  /reinstateProf.do
Exceptions                    /exceptions.do  /reportException.do  /resolveException.do
Notifications                 /notifications.do  /sendConvocations.do  /sendReminders.do
                              /dispatchNotifications.do
Governance                    /departments.do  /saveDepartment.do  /deleteDepartment.do
                              /profGovernance.do  /saveProfGovernance.do
                              /profAvailability.do  /saveProfAvailability.do  /deleteProfAvailability.do
                              /salleGovernance.do  /saveSalleGovernance.do
Audit / SMTP                  /audit.do  /saveSmtp.do
```

There is intentionally no authentication filter and no login redirect. Audit-aware
operations use a local operator actor supplied by `AuditContext`.

### First start

1. Build and deploy the WAR as before (`mvn clean package`, copy to Tomcat).
2. Open the application directly in the browser.
3. Create at least one academic session in **Sessions** (the system also
   bootstraps a default session if none exists). Optionally create departments,
   assign rooms/professors, and let professors declare unavailability.
4. The original workflow (import → affect → plan → export) is unchanged. New
   tabs (Sessions, Approbations, Incidents, Convocations, Gouvernance, Audit)
   appear in the sidebar when their JSP/controller routes are available.

### Notes for operators

- **Publishing freezes**: once a planning version is published, every soutenance
  inside is locked. To change anything, unlock the soutenance, swap a jury
  member, or report an exception — never delete the version.
- **Manual overrides leave traces**: every action goes through the audit log
  (`/audit.do`).
- **SMTP is optional**: when not configured, notifications are stored in the
  database and the operator can dispatch them later when SMTP is enabled.
- **Bootstrap data**: a default `DEFAULT-<year>` session is created on first
  use so planning can run without manual session setup.
- **No new dependencies were added**: the SMTP client is written directly on top
  of `java.net.Socket` and `javax.net.ssl.SSLSocket`.
