# Gestion Des PFE

Gestion Des PFE is a Java web application for managing PFE students, professors, supervisor assignments, soutenance planning, exports, and PV documents.

The application uses JSP/Servlets for the web layer, Hibernate for persistence, MySQL for storage, Apache POI for Excel and DOCX handling, iText for PDF exports, and an optional NVIDIA NIM/OpenAI-compatible NLP service to help classify project subjects by professor specialty.

## Main Features

- Import student lists from Excel files by filiere.
- Import professor lists from Excel files.
- Generate balanced student-to-professor affectations.
- View dashboard statistics by professor and filiere.
- Search dashboard data.
- Restore affectations from a backup file.
- Generate soutenance planning with rooms, dates, time slots, juries, and professor availability constraints.
- Add custom salles from the planning configuration panel.
- Prevent duplicate salles, including variants like `AMPHI A` vs `amphi A` and `S6 NB` vs `salle 6 NB`.
- Export affectations and planning to PDF and DOCX.
- Keep planning export history.
- Generate and download PV documents.
- Download Excel templates for students and professors.

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
      Session.java
      Soutenance.java
    services/
      AffectationService*.java
      NlpService*.java
      PfeService*.java
      PlanningService*.java
      VerificationService*.java
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
    pv.jsp
    WEB-INF/web.xml
```

## Architecture

The app follows a simple MVC-style organization.

`FrontController.java` is the single servlet mapped to `*.do`. It receives requests, calls service methods, prepares request attributes, and forwards to JSP pages or streams generated files.

The JSP files are responsible for the UI:

- `index.jsp`: landing/home page.
- `dashboard.jsp`: statistics, search, and verification display.
- `affectation.jsp`: imports, affectation controls, and affectation exports.
- `planning.jsp`: planning generation, room selection, planning exports, and history.
- `pv.jsp`: PV generation and downloads.

The service layer contains the business logic:

- `PfeServiceImpl`: main facade used by the controller.
- `AffectationServiceImpl`: assigns students to professors with balanced load.
- `PlanningServiceImpl`: generates soutenance schedules.
- `NlpServiceImpl`: optional AI subject analysis.
- `VerificationServiceImpl`: checks generated data for issues.

The DAO layer wraps Hibernate access for each entity.

## Database

The application uses MySQL. The current Hibernate configuration is in:

```text
src/main/resources/hibernate.cfg.xml
```

Default connection:

```properties
url=jdbc:mysql://localhost:3306/GestionPFE?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
username=root
password=
```

Hibernate is configured with:

```xml
<property name="hibernate.hbm2ddl.auto">update</property>
```

That means Hibernate updates the schema automatically based on the entity classes. The database `GestionPFE` is created if it does not already exist.

## Entity Overview

- `Etudiant`: student data imported from Excel, including CNE, name, email, filiere, binome CNE, subject, and language.
- `Professeur`: professor data, including name, discipline, and specialty.
- `Affectation`: relation between a student and an assigned supervisor.
- `Salle`: room used for soutenance planning.
- `Jury`: president and two rapporteurs.
- `Soutenance`: scheduled defense with date, hour, room, student, and jury.
- `FichierListe`: uploaded list metadata.
- `Session`: session-related persistence entity.

## Setup

### 1. Requirements

Install:

- JDK 21
- Maven
- MySQL Server
- Tomcat 9

Make sure `java` and `mvn` are available in your terminal:

```bash
java -version
mvn -version
```

### 2. Configure MySQL

Start MySQL and make sure the configured user can create and update the `GestionPFE` database.

If your local MySQL credentials are different, edit:

```text
src/main/resources/hibernate.cfg.xml
```

Update:

```xml
<property name="connection.username">root</property>
<property name="connection.password"></property>
```

### 3. Configure Optional NLP

The NLP integration is optional. It helps the planning algorithm match project subjects with professor specialties.

Copy:

```text
src/main/resources/config.properties.example
```

to:

```text
src/main/resources/config.properties
```

Then fill:

```properties
nvidia.api.key=YOUR_API_KEY
nvidia.base.url=https://integrate.api.nvidia.com/v1
nvidia.model=meta/llama-3.3-70b-instruct
```

If `config.properties` is missing or the key is not configured, the app still runs. NLP calls fall back gracefully in the service code.

### 4. Build

From the `projet` directory:

```bash
mvn clean package
```

The WAR file is generated under:

```text
target/Gestion-Des-PFE.war
```

### 5. Deploy

Deploy the WAR to Tomcat 9.

Typical options:

- Copy `target/Gestion-Des-PFE.war` to Tomcat `webapps/`.
- Or deploy it from the Tomcat Manager UI.

After deployment, open:

```text
http://localhost:8080/Gestion-Des-PFE/
```

The exact URL can change if your Tomcat context path is different.

## Application Workflow

### 1. Import Data

Go to the affectation page and upload:

- student Excel files, one per filiere.
- professor Excel file.

Student import expects columns:

```text
A: CNE
B: Nom
C: Prenom
D: Email
E: CNE Binome
F: Sujet de stage
```

Professor import expects columns:

```text
A: Nom
B: Prenom
C: Discipline
D: Specialite
```

Template downloads are available through the application.

### 2. Generate Affectations

The affectation algorithm loads the selected students and all professors, then assigns students to the least-loaded professor at each step.

The generated affectations are saved in the database and can be exported to PDF or DOCX.

### 3. Generate Planning

The planning page uses existing affectations to generate soutenance schedules.

Important rules in `PlanningServiceImpl`:

- At least 3 professors are required.
- Default time slots are `9h`, `10h`, `11h`, `14h`, `15h`, `16h`, and `17h`.
- The default start date is June 23, 2026.
- Planning uses up to 4 working days.
- Weekends are skipped.
- A professor cannot be scheduled in overlapping slots.
- A professor needs at least one hour of rest between consecutive soutenances.
- Rooms cannot be double-booked in the same slot.
- Jury participation is balanced across professors.
- Each jury tries to include at least 2 professors related to informatique.
- If NLP is configured, subjects are analyzed in batch to improve jury specialty matching.

When the database has no rooms, the default rooms are inserted:

```text
S4A
S5A
S16A
S17A
AMPHI A
```

### 4. Manage Salles

From the planning configuration panel, you can add a new salle.

Duplicate room names are blocked. The comparison ignores:

- case differences: `AMPHI A` equals `amphi A`.
- repeated spaces: `S6  NB` equals `S6 NB`.
- `Salle` vs `S` prefixes: `salle 6 NB`, `Salle6 NB`, `S 6 NB`, and `S6 NB` are considered the same.

When a duplicate is submitted, the app redirects back to planning and displays a Bootstrap warning:

```text
Cette salle existe deja.
```

### 5. Export Planning

Planning can be exported as:

- PDF
- DOCX

Exports are also kept in planning history and can be downloaded later.

### 6. Generate PV Documents

The PV page uses soutenance data and the template:

```text
src/main/resources/templates/template_pv.docx
```

The app can generate PV DOCX files and download them individually or as a ZIP.

## Main Routes

All routes are handled by `FrontController.java`.

| Route | Purpose |
| --- | --- |
| `/dashboard.do` | Dashboard and statistics |
| `/affectation.do` | Affectation page |
| `/uploadEtudiants.do` | Upload student Excel file |
| `/uploadProfs.do` | Upload professor Excel file |
| `/lancerAffectation.do` | Generate affectations |
| `/supprimerListes.do` | Delete uploaded lists/data |
| `/exportPdf.do` | Export affectations as PDF |
| `/exportDocx.do` | Export affectations as DOCX |
| `/restoreAffectation.do` | Restore affectation backup |
| `/planning.do` | Planning page |
| `/lancerPlanning.do` | Generate planning |
| `/addSalle.do` | Add a salle |
| `/downloadHistory.do` | Download planning export from history |
| `/clearHistory.do` | Clear planning export history |
| `/planningPdf.do` | Export planning as PDF |
| `/planningDocx.do` | Export planning as DOCX |
| `/pv.do` | PV page |
| `/downloadPvDocx.do` | Download PV DOCX |
| `/downloadPvZip.do` | Download PV ZIP |
| `/templateEtudiants.do` | Download student Excel template |
| `/templateProfs.do` | Download professor Excel template |

## Important Files

| File | Role |
| --- | --- |
| `pom.xml` | Maven project configuration |
| `hibernate.cfg.xml` | MySQL and Hibernate configuration |
| `config.properties.example` | Optional NLP configuration template |
| `FrontController.java` | Main servlet and route dispatcher |
| `PfeServiceImpl.java` | Main application service facade |
| `PlanningServiceImpl.java` | Planning generation algorithm |
| `AffectationServiceImpl.java` | Affectation generation algorithm |
| `NlpServiceImpl.java` | NVIDIA NIM/OpenAI-compatible NLP integration |
| `ExcelImporter.java` | Excel parsing for students and professors |
| `planning.jsp` | Planning UI |
| `affectation.jsp` | Import and affectation UI |
| `dashboard.jsp` | Dashboard UI |
| `pv.jsp` | PV UI |

## Build and Troubleshooting

### `mvn` is not recognized

Maven is not installed or not in your `PATH`.

Install Maven, then verify:

```bash
mvn -version
```

### Tomcat deployment fails with Java version errors

This project targets Java 21. Use JDK 21 to build and run it.

### Database connection fails

Check:

- MySQL is running.
- The username and password in `hibernate.cfg.xml` are correct.
- The MySQL user can create the `GestionPFE` database.
- Port `3306` is available.

### NLP warning about missing `config.properties`

This is expected if you did not configure NVIDIA NLP. Copy `config.properties.example` to `config.properties` and add a valid key, or ignore the warning if you do not need NLP.

### Duplicate salles still appear from old data

The duplicate guard prevents new duplicates from being inserted. If duplicates already exist in the database from before the fix, remove them manually from the `salle` table or through a cleanup script.

## Development Notes

- The servlet layer uses `javax.servlet`, so Tomcat 9 is the safest target. Tomcat 10 uses `jakarta.servlet` and would require code/package migration.
- Hibernate schema mode is `update`, which is convenient for development but should be reviewed before production use.
- Generated planning deletes old `Soutenance` and `Jury` records before saving the new planning.
- The planning algorithm groups binomes so both students share the same soutenance slot, room, and jury.
- Export code is currently in `FrontController.java`, so route and document-generation logic are tightly coupled.

## Suggested Future Improvements

- Add a database unique constraint or normalized column for `Salle` names.
- Move export generation out of `FrontController.java` into dedicated services.
- Add automated tests for salle normalization, planning constraints, and Excel import.
- Add user-facing success messages after adding a new salle.
- Add authentication if the app is used by multiple roles.
- Externalize database credentials with environment variables.

