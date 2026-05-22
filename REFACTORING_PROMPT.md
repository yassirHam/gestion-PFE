# Comprehensive Refactoring Prompt — Gestion PFE Application

> **Give this entire document to an AI agent to implement ALL the changes described below.**

---

## 🎯 Project Context

This is a Java EE web application (WAR deployed on Tomcat 9) for managing **Projet de Fin d'Études** (end-of-study project defenses) at ENSAH. The tech stack is:

- **Java 21** / Servlet 4.0 / JSP / JSTL / Bootstrap 5
- **Hibernate 6.4.4** (ORM) with **MySQL**
- **Apache POI 5.2.5** (Excel import/export + DOCX generation)
- **iText 7.2.5** (PDF generation)
- **LangChain4j 0.36.2** with NVIDIA NIM (Llama 3.3 70B) for NLP subject classification
- Single `FrontController` servlet routing all `*.do` requests
- DAO pattern (interfaces + Hibernate implementations)
- Service layer (`PfeService`, `PlanningService`, `VerificationService`)

---

## 📋 CHANGE 1: Unified Single Excel Import (Multi-Sheet Workbook)

### Current Behavior (to remove)
- Students are uploaded as **separate Excel files per filière** (e.g., `GI.xlsx`, `ID.xlsx`, `TDIA.xlsx`). The filière is deduced from the filename.
- Professors are uploaded as a **separate Excel file**.
- These are two independent upload forms with two separate endpoints (`uploadEtudiants.do`, `uploadProfs.do`).

### New Behavior (to implement)
- There is **ONE single Excel file** (`GestionPFE.xlsx` or whatever the user names it) containing **multiple sheets** (feuilles):
  - **One sheet per filière** for students (sheet name = filière code, e.g., sheet named `GI`, `ID`, `TDIA`, `GEER`, etc.). The sheet name IS the filière.
  - **One sheet named `Professeurs`** (or `PROFESSEURS` / `Profs` — be case-insensitive) for the professor list.
  - Optionally, a sheet named `Salles` for room definitions.
- The system reads ALL sheets from the single uploaded workbook:
  - Any sheet named "Professeurs" (case-insensitive) → import as professors
  - Any sheet named "Salles" (case-insensitive) → import as rooms
  - All other sheets → import as student lists where the sheet name = filière code
- The student sheet format stays the same: CNE | NOM | PRÉNOM | EMAIL | CNE BINÔME | SUJET PFE (first row = header, skip it)
- The professor sheet format stays the same: NOM | PRÉNOM | DISCIPLINE | SPÉCIALITÉ (first row = decorative header, second row = real header, data starts row 3)
- Optional `Salles` sheet: NUM_SALLE | BLOCK (first row = header)

### Implementation Details
1. **Remove** the separate `uploadEtudiants.do` and `uploadProfs.do` endpoints.
2. **Create** a single `uploadData.do` endpoint that accepts one `.xlsx` file.
3. **Modify `ExcelImporter`** to add a new method:
   ```java
   public static ImportResult importWorkbook(InputStream is)
   ```
   This method returns a result object containing:
   - `Map<String, List<Etudiant>> studentsByFiliere` (key = sheet name = filière)
   - `List<Professeur> professeurs`
   - `List<Salle> salles` (optional, may be empty)
   - `List<String> warnings` (any parsing issues)
4. **Update the UI** (`affectation.jsp`): Replace the two upload forms with a single form:
   - One file input: "Fichier Excel (toutes les données)"
   - One upload button
   - Update the format preview section to explain the multi-sheet structure
5. **Update the template download** (`templateEtudiants.do` → replace with `templateData.do`) to generate a sample workbook with:
   - A sheet named "GI" with student headers
   - A sheet named "ID" with student headers
   - A sheet named "TDIA" with student headers
   - A sheet named "Professeurs" with professor headers
   - A sheet named "Salles" with room headers
6. **Remove** `templateProfs.do` (merged into single template).
7. The controller should iterate over all filières found, call `service.saveEtudiants(list, filiere, originalFileName)` for each, then call `service.saveProfesseurs(profs)`, then import salles if present.

---

## 📋 CHANGE 2: Full User-Configurable Planning Parameters

### Current Behavior (to remove/modify)
- `PlanningConfig` is hardcoded with:
  - Time slots: `{9, 10, 11, 14, 15, 16, 17}`
  - Max days: `4`
  - Default start date: June 23, 2026
- The user can only pick: start date + which salles to include.
- No soutenance duration concept (implicitly 1 hour per slot).

### New Behavior (to implement)
The user should be able to configure ALL of the following **before** generating the planning, via a **dedicated "Configuration" tab/section** in the planning page (or a separate `configuration.jsp` page accessible before planning generation):

#### Time Configuration
| Parameter | Description | Example |
|-----------|-------------|---------|
| `numberOfDays` | How many working days for soutenances | 4 |
| `startDate` | First day of soutenances | 2026-06-23 |
| `startHourMorning` | Morning session start hour | 9 |
| `endHourMorning` | Morning session end hour | 12 |
| `startHourAfternoon` | Afternoon session start hour | 14 |
| `endHourAfternoon` | Afternoon session end hour | 18 |
| `soutenanceDuration` | Duration of one soutenance in minutes | 60 |
| `breakBetweenSoutenances` | Break between two consecutive soutenances in same room (minutes) | 0 |
| `lunchBreakStart` | Lunch break start hour | 12 |
| `lunchBreakEnd` | Lunch break end hour | 14 |

From these parameters, the system **dynamically computes the time slots** instead of hardcoding them. For example:
- Morning: 9h, 10h, 11h (if duration=60min, start=9, end=12)
- Afternoon: 14h, 15h, 16h, 17h (if duration=60min, start=14, end=18)

#### Constraint Configuration (see CHANGE 3)
These are in the same configuration section.

### Implementation Details
1. **Modify `PlanningConfig`** to accept all these parameters dynamically (constructor or builder pattern). Remove hardcoded `defaults()` or make it truly default values that the user can override.
2. **Add a method `computeSlots()`** that generates the `int[]` slots array from the hour/duration parameters.
3. **Update `PlanningServiceImpl`** to use the dynamically-computed slots.
4. **Update the planning form** (either in `planning.jsp` or a new `configuration.jsp`) to let the user set all these values via form inputs with sensible defaults.
5. **Pass these parameters** from the controller to the service when calling `genererPlanning()`. The signature should become:
   ```java
   List<Soutenance> genererPlanning(List<String> filieres, List<String> debugLog, 
                                     List<Long> selectedSalles, PlanningConfig config);
   ```
   (Remove the `String startDate` parameter; it's now inside `PlanningConfig`.)

---

## 📋 CHANGE 3: Fully User-Configurable Constraints System

### Current Behavior (hardcoded, to make configurable)
- Professor rest: consecutive slots forbidden (hardcoded `Math.abs(existSlot - slot) == 1`)
- Jury composition: at least 2 informatique professors (hardcoded in `DefaultJurySelectionStrategy`)
- Max jury load gap: 2 (hardcoded in `PlanningConfig`)
- No explicit room capacity concept
- No user-facing constraint configuration

### New Behavior (to implement)
Create a **constraint configuration system** where the user defines each constraint with:
- **Constraint name** (human-readable label)
- **Constraint type** (enumerated)
- **Constraint value** (numeric, text, or boolean parameter)
- **Constraint priority**: `HARD` (must be satisfied — blocks planning) or `SOFT` (best-effort — generates warning if violated)

#### Constraint Types to Support

| Constraint ID | Label | Default Value | Type | Description |
|---------------|-------|---------------|------|-------------|
| `PROF_REST_HOURS` | Repos entre 2 soutenances (même prof) | 1 slot (= 1 hour) | HARD | Minimum gap in hours between two soutenances for the same professor. If set to 2, a prof at 9h cannot be assigned at 10h or 11h. |
| `MIN_JURY_SPECIALITY_MATCH` | Minimum de jurys par spécialité principale | 2 | SOFT | Minimum number of jury members whose discipline matches the "dominant" discipline (e.g., 2 Informatique profs). |
| `DOMINANT_DISCIPLINE` | Discipline dominante requise | "Informatique" | SOFT | Which discipline must have the minimum count in each jury. User can change to "Mathématiques" etc. |
| `MAX_JURY_LOAD_GAP` | Écart max participations jury | 2 | SOFT | Maximum difference in jury participation count between most-active and least-active professor. |
| `MAX_SOUTENANCES_PER_PROF_PER_DAY` | Max soutenances/prof/jour | 4 | HARD | A professor cannot be in more than N soutenances on the same day (in any role). |
| `MAX_SOUTENANCES_PER_ROOM_PER_DAY` | Max soutenances/salle/jour | 7 | HARD | Maximum soutenances scheduled in one room in one day. |
| `REQUIRE_ENCADRANT_AS_PRESIDENT` | Encadrant = Président du jury | true | HARD | The supervisor (encadrant) must be the president of the jury. |
| `FORBID_ENCADRANT_AS_RAPPORTEUR` | Encadrant ≠ Rapporteur | true | HARD | The supervisor cannot be a rapporteur in their own student's jury. |
| `MIN_DISTINCT_JURY_MEMBERS` | Membres jury distincts | 3 | HARD | All 3 jury members must be different people. |
| `RESPECT_ENGLISH_PROF` | Inclure prof d'anglais si sujet en anglais | true | SOFT | If NLP detects subject is in English, include an English-discipline professor in jury. |
| `EXCLUDE_WEEKENDS` | Exclure samedi/dimanche | true | HARD | Never schedule on Saturday/Sunday. |
| `CUSTOM_EXCLUDED_DATES` | Dates exclues manuellement | [] | HARD | User-specified dates to exclude (holidays, etc.). |

#### UI for Constraints
In the **configuration section** (before the "Generate" button), display a table/form with all constraints:
- Each row: constraint label | current value (editable input) | priority toggle (HARD/SOFT)
- Allow the user to modify values and priorities
- HARD constraints → if violated, planning generation **STOPS** and shows an error
- SOFT constraints → if violated, planning still generates but shows **warnings** listing which soft constraints were broken

### Implementation Details
1. **Create a `Constraint` class**:
   ```java
   public class Constraint {
       private String id;
       private String label;
       private String value; // stored as String, parsed as needed
       private ConstraintPriority priority; // HARD or SOFT
   }
   ```
2. **Create a `ConstraintSet` class** that holds all constraints with methods:
   ```java
   public int getProfRestSlots();
   public int getMinSpecialityMatch();
   public String getDominantDiscipline();
   public int getMaxJuryLoadGap();
   public int getMaxSoutenancesPerProfPerDay();
   public boolean isEncadrantPresident();
   // etc.
   ```
3. **Modify `PlanningConfig`** to include a `ConstraintSet`.
4. **Modify `PlanningServiceImpl`**:
   - `isProfAvailable()` → use `constraint.getProfRestSlots()` instead of hardcoded `1`
   - `DefaultJurySelectionStrategy` → use constraint values for discipline matching
   - After planning generation, **validate all HARD constraints**. If any HARD constraint is violated, **delete the generated planning** and return errors.
   - Collect all SOFT constraint violations as warnings.
5. **Modify `VerificationServiceImpl`** to use the constraint set for its checks.
6. **Return constraint violation results to the UI** as structured data:
   ```java
   public class PlanningResult {
       private List<Soutenance> soutenances;
       private List<ConstraintViolation> hardViolations; // if non-empty, planning is rejected
       private List<ConstraintViolation> softViolations; // warnings
   }
   ```

---

## 📋 CHANGE 4: Intelligent Recommendations (Application Communicates with User)

### Current Behavior
- No recommendations or suggestions to the user.
- User just picks options and clicks generate.

### New Behavior (to implement)
The application should **proactively communicate** with the user by analyzing the current data and providing **intelligent recommendations** and **feasibility feedback** BEFORE and AFTER planning generation.

#### Pre-Generation Recommendations (shown in the configuration panel)
When the user navigates to the planning/configuration page, the system should analyze the current state and display recommendations:

1. **Recommended number of days**:
   - Formula: `ceil(totalProjects / (slotsPerDay * numberOfRooms))`
   - Example: "Vous avez **105 soutenances** à planifier. Avec 5 salles et 7 créneaux/jour, je recommande **au minimum 3 jours** de soutenances."
   - Show warning if user picks fewer days than recommended.

2. **Room adequacy check**:
   - "Avec 2 salles sélectionnées et 4 jours, vous pouvez planifier maximum 56 soutenances. Vous en avez 105. **Sélectionnez plus de salles ou ajoutez des jours.**"

3. **Professor sufficiency check**:
   - "Vous avez **12 professeurs** disponibles. Avec 105 soutenances et un jury de 3, chaque professeur participera en moyenne à **26 jurys**. C'est élevé. Recommandation: limiter à 4 soutenances/prof/jour max."

4. **Time slot adequacy**:
   - "Avec des soutenances de 90 minutes et la plage 9h-12h + 14h-18h, vous disposez de **5 créneaux/jour**."

5. **Conflict prediction**:
   - "Attention: avec seulement 8 professeurs et un repos de 2h entre soutenances, il est possible que le planning ne puisse pas être complété. Réduisez le repos à 1h ou ajoutez des jours."

#### Post-Generation Feedback
After planning generation (whether it succeeds or fails):

1. **If planning succeeds** → Show summary:
   - "Planning généré avec succès: 105 soutenances sur 4 jours."
   - "⚠️ 3 contraintes souples non respectées:" (list them)
   - "✅ Toutes les contraintes dures sont satisfaites."

2. **If planning fails (HARD constraint violated)** → Show **specific blocking error**:
   - "❌ **Impossible de générer le planning** avec cette configuration."
   - "**Contrainte violée**: Repos professeur (2h) — Le professeur AMRANI ne peut pas être planifié sans conflit les jours 3 et 4."
   - "**Suggestion**: Réduisez le repos à 1h, ajoutez un jour supplémentaire, ou ajoutez plus de professeurs."

3. **If partially generated** (some students couldn't be scheduled):
   - "⚠️ **12 soutenances n'ont pas pu être planifiées** (pas assez de créneaux disponibles)."
   - "Étudiants non planifiés: [list]"
   - "Suggestion: ajoutez 1 jour supplémentaire ou réduisez la contrainte de repos."

#### Implementation Details
1. **Create a `RecommendationService`** (or add to `PfeService`):
   ```java
   public class RecommendationService {
       public List<Recommendation> generateRecommendations(
           int totalProjects, int numberOfRooms, int slotsPerDay, 
           int numberOfDays, int numberOfProfs, ConstraintSet constraints);
   }
   ```
2. Each `Recommendation` has: `type` (INFO/WARNING/ERROR), `message`, `suggestion`.
3. **Add an AJAX endpoint** (`recommendations.do`) that returns JSON recommendations based on current configuration values. The frontend calls this dynamically as the user adjusts parameters.
4. **Display recommendations** in a dedicated panel/alert area on the configuration page, updating in real-time (or on-blur of inputs).
5. **After generation**, if there are HARD violations, show a **modal or prominent alert** listing each violated constraint with:
   - Which constraint
   - Which specific entities are affected (which professor, which student, which time slot)
   - A suggested fix

---

## 📋 CHANGE 5: Constraint Violation Blocking (Planning Not Generated if HARD Constraint Fails)

### Current Behavior
- Planning is always generated regardless of whether constraints are met.
- Verification happens AFTER (in VerificationService) as a passive report.

### New Behavior
- **HARD constraints are checked DURING generation.** If the algorithm cannot satisfy a HARD constraint for a specific soutenance, it:
  1. Logs the issue
  2. Continues trying other soutenances
  3. At the end, if ANY student couldn't be scheduled OR any HARD constraint is violated in the final result:
     - **The planning is NOT saved to the database**
     - A clear error message is returned to the user explaining exactly which constraints failed
     - The user must adjust configuration and retry
- **SOFT constraints**: The planning is generated, saved, but violations are reported as warnings in the UI.

### Implementation Details
1. **Modify `PlanningServiceImpl.genererPlanning()`** to return a `PlanningResult` instead of `List<Soutenance>`:
   ```java
   public class PlanningResult {
       private boolean success;
       private List<Soutenance> soutenances;
       private List<String> unscheduledStudents;
       private List<ConstraintViolation> hardViolations;
       private List<ConstraintViolation> softViolations;
       private List<Recommendation> suggestions;
   }
   ```
2. **After the scheduling loop**, run a **validation pass** on the complete result checking all HARD constraints.
3. **If `hardViolations` is non-empty OR `unscheduledStudents` is non-empty**:
   - Do NOT call `soutDao.saveAll()`
   - Return the result with `success = false`
4. **In the controller**, if `!result.isSuccess()`:
   - Set the violations as request attributes
   - Display a prominent error UI with the specific failing constraints
   - Show suggestions for how to fix (e.g., "increase days", "reduce rest time", "add more salles")
5. **Pop-up/Modal in UI**: When planning fails, show a Bootstrap modal or alert:
   ```
   ❌ Le planning ne peut pas être généré avec cette configuration.
   
   Contraintes non satisfaites:
   • [SALLE] Contrainte de salle: pas assez de salles pour planifier tous les étudiants en 3 jours.
   • [REPOS] Contrainte de repos: impossible de respecter 2h de repos avec 8 professeurs sur 3 jours.
   
   Suggestions:
   • Ajoutez 1 jour supplémentaire (passez de 3 à 4 jours)
   • Ou réduisez le repos professeur de 2h à 1h
   • Ou ajoutez 2 salles supplémentaires
   ```

---

## 📋 CHANGE 6: Responsive Design

### Current State
- The app uses Bootstrap 5 but is not fully optimized for mobile/tablet.
- Tables overflow on small screens.
- Configuration panels don't stack well on mobile.

### Requirements
1. **All pages must be fully responsive** (mobile-first approach).
2. **Tables**: Use `table-responsive` wrapper + consider card-based layouts for mobile view of planning data.
3. **Forms**: Stack vertically on mobile, use `col-12` breakpoints properly.
4. **Navigation**: The navbar collapse is already there (hamburger) — verify it works properly.
5. **Configuration panel**: On mobile, the sticky sidebar should become a collapsible accordion above the main content.
6. **Charts** (dashboard): Must resize properly on mobile.
7. **Modals/alerts**: Must not overflow on small screens.
8. Use CSS media queries where Bootstrap utilities aren't enough.
9. Test at breakpoints: 320px, 576px, 768px, 992px, 1200px.

---

## 📋 CHANGE 7: Updated Application Flow

### New Page Flow
```
1. Upload Page (affectation.jsp)
   └── Single Excel upload → processes all sheets
   └── Shows imported filières + professor count + salle count
   └── "Lancer Affectation" button

2. Configuration Page (NEW: configuration.jsp or section in planning.jsp)
   └── Time parameters (days, hours, duration, breaks)
   └── Constraint table (all constraints editable)
   └── Recommendations panel (dynamic, updates as user changes values)
   └── "Générer le Planning" button

3. Planning Result Page (planning.jsp)
   └── If success: show planning + soft constraint warnings
   └── If failure: show error modal + constraint violations + suggestions
   └── Export buttons (PDF, DOCX)
   └── "Reconfigurer" button → back to configuration

4. PV Page (pv.jsp) — unchanged
5. Dashboard (dashboard.jsp) — unchanged but shows constraint status

### Salle Management Panel (inside Configuration)
Visible alongside the time/constraint configuration:
- Bulk-add textarea
- Salle table with checkbox + delete button per row
- Delete-all button (with confirmation + protection if used by a planning)
```

---

## 📋 CHANGE 8: Salle (Room) Management — Hybrid Mode

### Current Behavior
- Salles can only be added one-by-one via a small form on the planning page (`addSalle.do`).
- No way to delete a salle.
- No bulk select/unselect (only "Tout sélectionner" / "Tout désélectionner" buttons for the active selection).

### New Behavior
Salles can be sourced from **two interchangeable channels**, both editable in the UI:

1. **From the unified Excel workbook** (optional sheet named `Salles`).
2. **Manually managed in the application** (CRUD).

In the UI (planning configuration section), provide a complete salle management panel:

- **List**: show every salle with its `num_salle`, `block`, current selection state.
- **Add** (single): existing form (`addSalle.do`) — keep as is.
- **Add (bulk)**: textarea with one salle per line → creates them all at once.
- **Delete**: trash icon next to each salle → calls `deleteSalle.do?id=...` (with confirmation). Must refuse to delete a salle currently referenced by a saved soutenance and show a clear message.
- **Delete all**: button "Supprimer toutes les salles" with double-confirmation. Same protection as above.
- **Select / Unselect**: per-row checkbox. The set of checked salles is what the planning generation uses.
- **Select all / Unselect all**: toggle buttons (already present, keep).

### Implementation Details
1. **Add to `SalleDAO`**:
   ```java
   void deleteById(Long id);
   boolean isUsedInPlanning(Long id); // returns true if any soutenance references this salle
   ```
2. **Add to `PfeService`**:
   ```java
   boolean deleteSalle(Long id); // returns false if blocked because salle is in use
   int addSalleBulk(String multilineNames); // returns number of salles actually added
   ```
3. **New endpoints in `FrontController`**:
   - `deleteSalle.do?id=...` → calls service, redirects with success/error flash message
   - `addSalleBulk.do` → reads `salleNames` textarea
4. **Excel import**: when the unified workbook contains a `Salles` sheet, each row creates a new `Salle` (skipping duplicates by normalized `num_salle`). Existing salles are preserved.
5. **UI**: In the configuration section of `planning.jsp`, replace the simple list of salles with a table:
   | Sélection | N° Salle | Block | Actions |
   |-----------|----------|-------|---------|
   | ☑ | S3 AB | Bloc Principal | 🗑 |

---

## 📋 CHANGE 9: Database & Entity Changes

### New Entity: `PlanningConfiguration`
```java
@Entity
@Table(name = "planning_configuration")
public class PlanningConfiguration {
    @Id @GeneratedValue
    private Long id;
    
    private int numberOfDays;
    private String startDate; // YYYY-MM-DD
    private int startHourMorning;
    private int endHourMorning;
    private int startHourAfternoon;
    private int endHourAfternoon;
    private int soutenanceDurationMinutes;
    private int breakBetweenMinutes;
    
    // Constraints stored as JSON or separate table
    @Column(columnDefinition = "TEXT")
    private String constraintsJson;
    
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;
}
```

This entity **optionally** persists the last-used configuration so the user doesn't have to re-enter it each time. On page load, the last configuration is loaded as defaults.

---

## 📋 Summary of Files to Modify

| File | Changes |
|------|---------|
| `ExcelImporter.java` | Add `importWorkbook()` method, keep backward compat |
| `FrontController.java` | Remove `uploadEtudiants.do`/`uploadProfs.do`, add `uploadData.do`, modify `lancerPlanning.do` to pass full config, add `recommendations.do` |
| `PlanningConfig.java` | Make fully dynamic, add `computeSlots()`, integrate `ConstraintSet` |
| `PlanningServiceImpl.java` | Use dynamic constraints, return `PlanningResult`, validate HARD constraints before saving |
| `DefaultJurySelectionStrategy.java` | Use constraint values instead of hardcoded logic |
| `VerificationServiceImpl.java` | Integrate with constraint system |
| `PfeService.java` + `PfeServiceImpl.java` | Update `genererPlanning()` signature, add recommendation methods |
| `ServiceFactory.java` | Update factory methods |
| `affectation.jsp` | Single upload form, updated format preview |
| `planning.jsp` | Full configuration section, recommendations display, error modals, responsive |
| `dashboard.jsp` | Show constraint compliance status |
| All JSPs | Responsive CSS fixes |

### New Files to Create
| File | Purpose |
|------|---------|
| `services/Constraint.java` | Constraint data class |
| `services/ConstraintSet.java` | Collection of constraints with accessor methods |
| `services/ConstraintViolation.java` | Violation data class |
| `services/PlanningResult.java` | Result wrapper (success/failures/warnings) |
| `services/Recommendation.java` | Recommendation data class |
| `services/RecommendationService.java` | Interface for recommendations |
| `services/RecommendationServiceImpl.java` | Implementation |
| `entities/PlanningConfiguration.java` | (Optional) Persisted config entity |
| `dao/PlanningConfigurationDAO.java` | (Optional) DAO for config |

---

## ⚠️ Critical Rules

1. **Do NOT break existing functionality** that isn't mentioned. PV generation, PDF/DOCX export, dashboard search, history system must continue working.
2. **Keep the same dependencies** (pom.xml) — no new frameworks unless absolutely necessary.
3. **Keep Hibernate auto schema update** (`hbm2ddl.auto=update`) so new entities are auto-created.
4. **Keep the NLP service** working as-is (NVIDIA NIM integration).
5. **All UI text should be in French** (matching the existing app language).
6. **Use Bootstrap 5 components** for all new UI elements (modals, accordions, cards, badges, alerts).
7. **No JavaScript framework** (keep vanilla JS + Bootstrap JS). You may use fetch() for the AJAX recommendations endpoint.
8. **Maintain the DAO pattern** — create DAOs for any new entities.
9. **Test edge cases**: 0 students, 0 professors, 1 salle, impossible configurations.
10. **Ensure backward compatibility**: If an old-format Excel file (single sheet) is uploaded, detect it gracefully and either handle it (treat as single filière) or show a clear error message explaining the new expected format.
