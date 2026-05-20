# Changelog - All Planning Algorithm Fixes

## Summary of All Issues Fixed

| # | Problem | Root Cause | Fix Applied |
|---|---------|-----------|-------------|
| 1 | Jury load gap not enforced | `minLoad` computed locally per slot, not globally | Pass `globalMinLoad` from full jury pool |
| 2 | NLP path leaked overloaded profs | Second jury member (`r2`) had no load filter | Apply `loadCeiling` on every selection branch |
| 3 | Fallback path ignored load entirely | "2 info" rule was the only criterion | Multi-pass `pickPair` with load ceiling |
| 4 | Slot search short-circuited | `encadrantDailyLoad` rejected good slots before checking jury | Score by jury max load first |
| 5 | Dashboard chart vs algorithm mismatch | Algorithm counted rapporteurs only; chart counted all roles | Pre-populate with president counts |
| 6 | Verification silent on violations | Counted rapporteurs only, deduplicated binomes | Count all roles per soutenance |
| 7 | No verification for "2 informaticiens" | Rule was never checked post-generation | Added `verifyJuryInformatique()` |
| 8 | "2 info" rule violated at runtime | Fallback dropped the rule too early within same slot | Hard constraint: reject slot if < 2 info (Option B) |

---

## Fix 1: Global Min Load (not local per slot)

**Problem:** `minLoad` was computed from professors available at the current slot only. When only overloaded profs were free, `minLoad` rose (e.g., 7), making `minLoad + gap = 10`, and profs at 10 passed the filter.

**Fix:** Added `globalMinLoad` parameter to `JurySelectionStrategy` interface, computed once per project from the **entire** jury pool (all profs except encadrant).

**Files changed:**
- `JurySelectionStrategy.java` — new `int globalMinLoad` parameter
- `PlanningServiceImpl.java` — `computeGlobalMinLoad()` method
- `DefaultJurySelectionStrategy.java` — uses `globalMinLoad` instead of local `candidates.get(0)`

**Code:**
```java
// PlanningServiceImpl - computed before each project
int globalMinLoad = computeGlobalMinLoad(juryPool, profJuryCount);

private int computeGlobalMinLoad(List<Professeur> juryPool, Map<Long, Integer> profJuryCount) {
    int min = Integer.MAX_VALUE;
    for (Professeur p : juryPool) {
        int load = profJuryCount.getOrDefault(p.getIdp(), 0);
        if (load < min) min = load;
    }
    return min == Integer.MAX_VALUE ? 0 : min;
}
```

---

## Fix 2: Load Ceiling on Every NLP Selection Branch

**Problem:** In `selectNlpAwareJury`, the "techProf only" and "englishProf only" fallbacks picked the second member without any load filter:
```java
// OLD - no load check on r2!
Professeur r2 = candidates.stream()
    .filter(p -> !p.getIdp().equals(techProf.getIdp()))
    .findFirst().orElse(null);
```

**Fix:** Every `.stream()` selection now filters by `load(profJuryCount, p) <= loadCeiling`:
```java
// NEW - load ceiling enforced
Professeur r2 = candidates.stream()
    .filter(p -> !p.getIdp().equals(selectedTechProf.getIdp()))
    .filter(p -> load(profJuryCount, p) <= loadCeiling)  // ADDED
    .filter(p -> infoSoFar >= 2 || isInfo(p))
    .findFirst().orElse(null);
```

---

## Fix 3: Multi-Pass pickPair with Graceful Degradation

**Problem:** The old fallback had a single loop that only checked the "2 informaticiens" rule, ignoring load entirely.

**Fix:** Factored into `pickPair()` helper with parameterized `loadCeiling` and `enforceInfoRule`:

```java
private Professeur[] pickPair(List<Professeur> candidates, Map<Long, Integer> profJuryCount,
                              int loadCeiling, boolean encadrantIsInfo, boolean enforceInfoRule) {
    for (int i = 0; i < candidates.size(); i++) {
        Professeur p1 = candidates.get(i);
        if (load(profJuryCount, p1) > loadCeiling) continue;
        for (int j = i + 1; j < candidates.size(); j++) {
            Professeur p2 = candidates.get(j);
            if (load(profJuryCount, p2) > loadCeiling) continue;
            if (enforceInfoRule) {
                int infoCount = (encadrantIsInfo ? 1 : 0) + (isInfo(p1) ? 1 : 0) + (isInfo(p2) ? 1 : 0);
                if (infoCount < 2) continue;
            }
            return new Professeur[]{p1, p2};
        }
    }
    return null;
}
```

---

## Fix 4: Slot Scoring by Jury Fairness First

**Problem:** `findBestPlanningChoice` short-circuited with `if (encadrantDailyLoad > minDailyLoad) continue;` — rejecting days before evaluating the jury.

**Fix:** Removed the early exit. Now scores ALL valid slots by `(juryMaxLoad, encadrantDailyLoad, slotLoad)` in lexicographic order:

```java
private boolean isBetterChoice(int juryMaxLoad, int encadrantDailyLoad, int slotLoad,
                               int bestJuryMaxLoad, int minDailyLoad, int minSlotLoad) {
    if (juryMaxLoad != bestJuryMaxLoad) return juryMaxLoad < bestJuryMaxLoad;  // DOMINANT
    if (encadrantDailyLoad != minDailyLoad) return encadrantDailyLoad < minDailyLoad;
    return slotLoad < minSlotLoad;
}
```

---

## Fix 5: Pre-Populate profJuryCount with President Roles

**Problem:** The dashboard chart (`getSoutenancesParProf`) counts president + rapporteur1 + rapporteur2 per soutenance. But `profJuryCount` started at 0 for everyone, only incrementing for rapporteur picks. An encadrant of 5 students would show as 5 (president) + rapporteur picks in the chart, but the algorithm thought they were at 0.

**Fix:** Before the planning loop, pre-load each encadrant's future presidency count:

```java
// Pre-populate: each student their encadrant supervises = 1 future presidency
for (List<Affectation> project : projects) {
    for (Affectation aff : project) {
        if (aff.getEncadrant() != null && aff.getEncadrant().getIdp() != null) {
            profJuryCount.merge(aff.getEncadrant().getIdp(), 1, Integer::sum);
        }
    }
}
```

Also increment rapporteur count by `project.size()` (binomes = 2) to match the chart:
```java
int projectSize = project.size();
profJuryCount.merge(choice.rapporteur1.getIdp(), projectSize, Integer::sum);
profJuryCount.merge(choice.rapporteur2.getIdp(), projectSize, Integer::sum);
```

---

## Fix 6: Verification Counts All Roles Per Soutenance

**Problem:** `verifyJuryLoadDistribution` only counted rapporteurs, and deduplicated binomes. It measured a completely different metric than the chart, so violations were invisible.

**Fix:** Rewrote to count **president + rapporteur1 + rapporteur2** for every soutenance (no deduplication):

```java
for (Soutenance soutenance : soutenances) {
    Jury jury = soutenance.getJury();
    if (jury == null) continue;
    countRole(jury.getPresident(), participationByProfessor, namesByProfessor);
    countRole(jury.getRapporteur1(), participationByProfessor, namesByProfessor);
    countRole(jury.getRapporteur2(), participationByProfessor, namesByProfessor);
}
```

Now the verification fires alerts using the **exact same numbers** the user sees in the chart.

---

## Fix 7: Added "Jury sans 2 informaticiens" Verification

**Problem:** The "2 informaticiens" rule was only enforced during generation (best-effort). No post-generation check existed, so violations were invisible on the dashboard.

**Fix:** Added `verifyJuryInformatique()` in `VerificationServiceImpl`:

```java
private void verifyJuryInformatique(List<Soutenance> soutenances, ...) {
    for (Soutenance soutenance : soutenances) {
        // Check once per project (binomes share jury)
        Jury jury = soutenance.getJury();
        int infoCount = 0;
        if (isInfoProfesseur(jury.getPresident())) infoCount++;
        if (isInfoProfesseur(jury.getRapporteur1())) infoCount++;
        if (isInfoProfesseur(jury.getRapporteur2())) infoCount++;

        if (infoCount < 2) {
            report.addIssue("ALERTE", "Planning", "Jury sans 2 informaticiens", ...);
        }
    }
}
```

Uses same `isInfo` logic as `DefaultJurySelectionStrategy`: checks if `discipline` or `specialite` contains "info" (case-insensitive).

---

## Fix 8: "2 Informaticiens" as Hard Constraint (Option B)

**Problem:** Even after prioritizing the info rule (Option A), the algorithm still violated it because `selectJury` accepted the current slot and fell back to non-info profs. It never tried other slots where info profs might be available.

**Fix (Option B):** Made the strategy **reject the slot entirely** if it can't satisfy the "2 info" rule:

```java
// DefaultJurySelectionStrategy.selectJury():
// Pass 1: NLP + load ceiling
// Pass 2: "2 info" + load ceiling
// Pass 3: "2 info" WITHOUT load ceiling (ignore balance, keep discipline)
// Pass 4: return null → REJECT THIS SLOT
return null;  // forces caller to try another slot
```

In `PlanningServiceImpl`, if `findBestPlanningChoice` returns null (all slots rejected):

```java
if (bestChoice == null) {
    // Last resort: findBestPlanningChoiceRelaxed picks any 2 least-loaded profs
    bestChoice = findBestPlanningChoiceRelaxed(...);
    if (bestChoice != null) {
        log.add("ATTENTION: " + studentNames + " planifie sans 2 informaticiens");
    }
}
```

**New method `findBestPlanningChoiceRelaxed`:** Same as the normal version but picks the 2 least-loaded profs without any discipline constraint. Only called when the entire planning has no viable slot with 2 info profs.

---

## Fix: Project Ordering by Encadrant Load

**Problem:** Projects processed in random order. Encadrants with many students had their slots consumed early, leaving no room for balanced jury selection later.

**Fix:** Sort projects by encadrant frequency (descending) before the planning loop:

```java
Map<Long, Integer> encadrantProjectCount = countEncadrantProjects(projects);
projects.sort(Comparator.comparingInt(
    (List<Affectation> proj) -> -encadrantProjectCount.getOrDefault(encadrantIdOf(proj), 0)));
```

---

## Files Modified (Complete List)

| File | Changes |
|------|---------|
| `services/JurySelectionStrategy.java` | Added `int globalMinLoad` parameter |
| `services/DefaultJurySelectionStrategy.java` | Complete rewrite: `pickPair`, 3-pass logic, NLP fixes, hard constraint |
| `services/PlanningServiceImpl.java` | `computeGlobalMinLoad`, pre-populate presidents, `findBestPlanningChoiceRelaxed`, project sorting, scoring by jury load |
| `services/VerificationServiceImpl.java` | `verifyJuryLoadDistribution` (all roles), `verifyJuryInformatique` (2 info check) |
| `services/PlanningConfig.java` | `maxJuryLoadGap` field (already existed, now properly used) |

---

## Configuration

All thresholds are in `PlanningConfig.defaults()`:

```java
public static PlanningConfig defaults() {
    return new PlanningConfig(
        new int[]{9, 10, 11, 14, 15, 16, 17},  // time slots
        2026, Calendar.JUNE, 23,                 // start date
        4,                                       // maxDays
        List.of("S3 AB", "S4 AB", "S3 NB", "S2 NB", "AMPHI A"),  // rooms
        /* color palette */,
        2  // maxJuryLoadGap: max allowed difference in total participations
    );
}
```

---

## Constraint Priority Order (Final)

| Priority | Constraint | Behavior |
|----------|-----------|----------|
| 1 (hard) | No time conflicts | Prof can't be in 2 places at once |
| 2 (hard) | Back-to-back rest | Prof can't have consecutive slots |
| 3 (hard) | Room availability | One soutenance per room per slot |
| 4 (hard) | 2 informaticiens | Reject slot if not satisfiable; try all others first |
| 5 (soft) | Jury load gap | Prefer balanced, but allow violation to satisfy #4 |
| 6 (soft) | NLP specialty match | Best-effort, never blocks scheduling |
| 7 (soft) | Encadrant daily spread | Tie-breaker when jury load is equal |

---

## Dashboard Alerts Summary

| Alert | Severity | When it fires |
|-------|----------|---------------|
| Repartition jury non equitable | ALERTE | Gap between min and max participations > maxJuryLoadGap |
| Surcharge jury professeur | ALERTE | Individual prof exceeds minLoad + maxJuryLoadGap |
| Jury sans 2 informaticiens | ALERTE | Jury has < 2 profs with "info" in discipline/specialite |
| Repartition non equitable (affectation) | ALERTE | Encadrement count outside expected range |
| Chevauchement de salle | CRITIQUE | Two projects in same room at same time |
| Professeur affecte au meme horaire | CRITIQUE | Prof in two juries simultaneously |
| Repos professeur insuffisant | ALERTE | Prof has back-to-back soutenances |
| Projet affecte non planifie | ALERTE | Student has affectation but no soutenance |
