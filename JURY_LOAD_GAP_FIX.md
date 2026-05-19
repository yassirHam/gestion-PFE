# Correction de la Repartition Jury - Documentation Detaillee

## Probleme Initial

Lors de la generation du planning pour 105+ soutenances, le dashboard affichait :

```
ALERTE Planning - Repartition jury non equitable
L'ecart entre le minimum (5) et le maximum (10) de participations jury est de 5,
ce qui depasse le seuil autorise de 3.

ALERTE Planning - Surcharge jury professeur
ABAKOUY Redouan participe a 10 jury(s), alors que le minimum est 5 (ecart autorise: 3).

ALERTE Planning - Surcharge jury professeur
LAHJOUJIELIDRISSI Ahmed participe a 10 jury(s), alors que le minimum est 5 (ecart autorise: 3).
```

Le `maxJuryLoadGap` (configure a 3 dans `PlanningConfig`) n'etait pas correctement applique par l'algorithme de generation.

---

## Causes Racines Identifiees

### Cause 1 : `minLoad` calcule localement par slot

**Ancien code** dans `DefaultJurySelectionStrategy.selectJury()` :

```java
candidates.sort(Comparator.comparingInt(
    (Professeur p) -> profJuryCount.getOrDefault(p.getIdp(), 0)));
int minLoad = candidates.isEmpty() ? 0 
    : profJuryCount.getOrDefault(candidates.get(0).getIdp(), 0);
int maxLoadGap = config.getMaxJuryLoadGap();
```

**Probleme** : `candidates` ne contient que les professeurs **disponibles au creneau courant**. Si seuls des profs surcharges sont libres a un creneau, `minLoad` monte (ex: 7), rendant `minLoad + gap = 10`. Un prof avec 10 participations passe alors le filtre car `10 <= 10`.

**Impact** : La reference de comparaison derive localement a chaque creneau au lieu d'etre ancree sur le minimum global de l'ensemble du planning.

---

### Cause 2 : Chemin NLP sans filtre sur le second membre

**Ancien code** dans `DefaultJurySelectionStrategy.selectNlpAwareJury()` :

```java
// Cas "techProf trouve, pas besoin d'anglais" :
if (techProf != null && !needEnglish) {
    Professeur r2 = candidates.stream()
            .filter(p -> !p.getIdp().equals(techProf.getIdp()))
            .findFirst()  // AUCUN filtre de charge !
            .orElse(null);
    if (r2 != null) {
        return new Professeur[]{techProf, r2};
    }
}
```

**Probleme** : Le second rapporteur (`r2`) etait selectionne sans aucune verification de sa charge jury. Un prof avec 10 participations pouvait etre choisi simplement car il etait le premier dans la liste.

---

### Cause 3 : Le chemin "fallback" (non-NLP) ignorait completement la charge

**Ancien code** dans `DefaultJurySelectionStrategy.selectJury()` (apres le chemin NLP) :

```java
for (int i = 0; i < candidates.size(); i++) {
    for (int j = i + 1; j < candidates.size(); j++) {
        Professeur p1 = candidates.get(i);
        Professeur p2 = candidates.get(j);
        int infoCount = (encadrantIsInfo ? 1 : 0) + (isInfo(p1) ? 1 : 0) + (isInfo(p2) ? 1 : 0);
        if (infoCount >= 2) {
            return new Professeur[]{p1, p2};  // PAS DE FILTRE DE CHARGE
        }
    }
}
```

**Probleme** : Le seul critere etait la regle "2 informaticiens". Aucune verification de la charge jury.

---

### Cause 4 : La boucle `findBestPlanningChoice` court-circuitait sur le mauvais critere

**Ancien code** dans `PlanningServiceImpl.findBestPlanningChoice()` :

```java
for (int dayIdx = 0; dayIdx < planningDates.validDates.size(); dayIdx++) {
    String dateStr = planningDates.validDates.get(dayIdx);
    int encadrantDailyLoad = profDailyCount.get(encadrant.getIdp()).getOrDefault(dateStr, 0);
    if (encadrantDailyLoad > minDailyLoad) {
        continue;  // SKIP : le jour est rejete AVANT de verifier le jury
    }
    // ...
}
```

**Probleme** : Un jour avec un meilleur jury (plus equilibre) pouvait etre rejete simplement car le `encadrantDailyLoad` etait legerement plus haut. Le critere d'equilibre jury n'intervenait jamais dans le choix du meilleur slot.

---

## Corrections Apportees

---

### 1. Nouveau parametre `globalMinLoad` dans l'interface `JurySelectionStrategy`

**Fichier** : `services/JurySelectionStrategy.java`

```java
public interface JurySelectionStrategy {
    /**
     * @param encadrant       le superviseur (president du jury, exclu de la selection)
     * @param available       professeurs disponibles au creneau considere
     * @param profJuryCount   map globale de tous les profs -> nombre de participations jury
     * @param globalMinLoad   le minimum de participations jury parmi TOUS les professeurs
     *                        eligibles (pas seulement ceux disponibles au creneau).
     *                        Utilise pour ancrer le plafond de charge de maniere consistante.
     * @param nlp             analyse NLP optionnelle du sujet
     * @param config          configuration du planning
     * @return tableau de 2 professeurs, ou null si aucune paire valide
     */
    Professeur[] selectJury(Professeur encadrant, List<Professeur> available, 
                            Map<Long, Integer> profJuryCount,
                            int globalMinLoad, SujetAnalysis nlp, PlanningConfig config);
}
```

**Pourquoi** : Le `globalMinLoad` est calcule une seule fois par projet a partir de l'ensemble du jury pool (tous les profs sauf l'encadrant). Cela elimine la derive locale qui causait le bug.

---

### 2. Calcul du `globalMinLoad` dans `PlanningServiceImpl`

**Fichier** : `services/PlanningServiceImpl.java`

**Nouvelle methode** :

```java
private int computeGlobalMinLoad(List<Professeur> juryPool, Map<Long, Integer> profJuryCount) {
    int min = Integer.MAX_VALUE;
    for (Professeur p : juryPool) {
        int load = profJuryCount.getOrDefault(p.getIdp(), 0);
        if (load < min) min = load;
    }
    return min == Integer.MAX_VALUE ? 0 : min;
}
```

**Utilisation** dans la boucle principale :

```java
for (List<Affectation> project : projects) {
    // ...
    List<Professeur> juryPool = new ArrayList<>(allProfs);
    juryPool.removeIf(p -> p.getIdp().equals(encadrant.getIdp()));

    // Calcule le VRAI minimum global, pas le min local du creneau
    int globalMinLoad = computeGlobalMinLoad(juryPool, profJuryCount);

    PlanningChoice bestChoice = findBestPlanningChoice(
        encadrant, juryPool, planningDates, slots, salles,
        profBusyAtSlot, profSchedule, profJuryCount, profDailyCount, 
        roomBusy, nlpResult, globalMinLoad);
    // ...
}
```

**Pourquoi** : Le minimum est recalcule avant chaque projet (car il peut augmenter au fur et a mesure des affectations), mais il est toujours base sur l'ensemble du pool, pas sur un sous-ensemble de creneau.

---

### 3. Reecriture de `DefaultJurySelectionStrategy` avec `loadCeiling`

**Fichier** : `services/DefaultJurySelectionStrategy.java`

#### 3.1 Signature mise a jour

```java
@Override
public Professeur[] selectJury(Professeur encadrant, List<Professeur> available, 
                               Map<Long, Integer> profJuryCount,
                               int globalMinLoad, SujetAnalysis nlp, PlanningConfig config) {
    // ...
    int loadCeiling = globalMinLoad + config.getMaxJuryLoadGap();
    // Tout professeur avec profJuryCount > loadCeiling est EXCLU
}
```

#### 3.2 Nouvelle methode utilitaire `pickPair`

```java
/**
 * Itere les paires par charge croissante et retourne la premiere valide.
 * Prend toujours la paire la moins chargee qui satisfait les contraintes,
 * minimisant l'ecart a chaque etape plutot que de simplement le borner.
 */
private Professeur[] pickPair(List<Professeur> candidates, Map<Long, Integer> profJuryCount,
                              int loadCeiling, boolean encadrantIsInfo, boolean enforceInfoRule) {
    for (int i = 0; i < candidates.size(); i++) {
        Professeur p1 = candidates.get(i);
        if (load(profJuryCount, p1) > loadCeiling) continue;  // FILTRE
        for (int j = i + 1; j < candidates.size(); j++) {
            Professeur p2 = candidates.get(j);
            if (load(profJuryCount, p2) > loadCeiling) continue;  // FILTRE
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

**Pourquoi** : Factorise la logique de selection en un seul endroit. Le `loadCeiling` est applique sur les DEUX membres de la paire, sans exception.

#### 3.3 Logique de selection en 5 passes degradantes

```java
@Override
public Professeur[] selectJury(...) {
    // ...
    int loadCeiling = globalMinLoad + maxLoadGap;

    // 1) NLP-aware avec plafond strict
    if (nlp != null) {
        Professeur[] nlpJury = selectNlpAwareJury(candidates, profJuryCount, 
                                                   loadCeiling, encadrantIsInfo, nlp);
        if (nlpJury != null) return nlpJury;
    }

    // 2) Regle "2 informaticiens" + plafond de charge
    Professeur[] pair = pickPair(candidates, profJuryCount, loadCeiling, encadrantIsInfo, true);
    if (pair != null) return pair;

    // 3) Plafond de charge seul (relache la regle informaticiens)
    pair = pickPair(candidates, profJuryCount, loadCeiling, encadrantIsInfo, false);
    if (pair != null) return pair;

    // 4) Dernier recours : ignore le plafond mais tente la regle informaticiens
    pair = pickPair(candidates, profJuryCount, Integer.MAX_VALUE, encadrantIsInfo, true);
    if (pair != null) return pair;

    // 5) Fallback absolu : les 2 moins charges
    return new Professeur[]{candidates.get(0), candidates.get(1)};
}
```

**Pourquoi** : La degradation progressive assure que le planning est toujours genere (pas de blocage), mais que le chemin heureux respecte le plafond. Si les passes 4/5 sont declenchees, la verification sur le dashboard le signalera.

#### 3.4 `selectNlpAwareJury` corrige

```java
private Professeur[] selectNlpAwareJury(List<Professeur> candidates, 
                                        Map<Long, Integer> profJuryCount,
                                        int loadCeiling, boolean encadrantIsInfo, 
                                        SujetAnalysis nlp) {
    // techProf : filtrage par specialite ET plafond de charge
    Professeur techProf = candidates.stream()
            .filter(p -> load(profJuryCount, p) <= loadCeiling)   // <-- NOUVEAU
            .filter(p -> containsIgnoreCase(p.getSpecialite(), targetSpec))
            .findFirst().orElse(null);

    // englishProf : filtrage par langue ET plafond de charge
    // ...

    // Cas "techProf seul" : le second membre respecte AUSSI le plafond
    if (techProf != null && !needEnglish) {
        Professeur r2 = candidates.stream()
                .filter(p -> !p.getIdp().equals(selectedTechProf.getIdp()))
                .filter(p -> load(profJuryCount, p) <= loadCeiling)  // <-- NOUVEAU
                .filter(p -> infoSoFar >= 2 || isInfo(p))
                .findFirst().orElse(null);
        // Fallback si pas de prof info sous le plafond :
        if (r2 == null) {
            r2 = candidates.stream()
                    .filter(p -> !p.getIdp().equals(selectedTechProf.getIdp()))
                    .filter(p -> load(profJuryCount, p) <= loadCeiling)  // <-- NOUVEAU
                    .findFirst().orElse(null);
        }
        if (r2 != null) return new Professeur[]{techProf, r2};
    }
    // Meme chose pour le cas "englishProf seul"...
    return null;
}
```

**Pourquoi** : Chaque branche de selection (techProf, englishProf, r2, r1) applique maintenant le `loadCeiling`. Plus aucune fuite n'est possible par ce chemin.

#### 3.5 Methode utilitaire `load`

```java
private int load(Map<Long, Integer> profJuryCount, Professeur p) {
    return profJuryCount.getOrDefault(p.getIdp(), 0);
}
```

**Pourquoi** : Evite la repetition de `profJuryCount.getOrDefault(p.getIdp(), 0)` partout.

---

### 4. `findBestPlanningChoice` avec scoring par charge jury

**Fichier** : `services/PlanningServiceImpl.java`

**Ancien comportement** : Premier creneau valide retenu, en privilegiant le `encadrantDailyLoad` minimum.

**Nouveau comportement** : Explore TOUS les creneaux et retient celui qui produit le jury le plus equilibre.

```java
private PlanningChoice findBestPlanningChoice(Professeur encadrant,
        List<Professeur> juryPool, PlanningDates planningDates, int[] slots,
        List<Salle> salles, Map<String, Set<Long>> profBusyAtSlot,
        Map<Long, List<String>> profSchedule, Map<Long, Integer> profJuryCount,
        Map<Long, Map<String, Integer>> profDailyCount,
        Map<String, Boolean> roomBusy, SujetAnalysis nlpResult,
        int globalMinLoad) {
    
    PlanningChoice bestChoice = null;
    int bestJuryMaxLoad = Integer.MAX_VALUE;  // <-- NOUVEAU critere dominant
    int minDailyLoad = Integer.MAX_VALUE;
    int minSlotLoad = Integer.MAX_VALUE;

    for (int dayIdx = 0; dayIdx < planningDates.validDates.size(); dayIdx++) {
        String dateStr = planningDates.validDates.get(dayIdx);
        int encadrantDailyLoad = profDailyCount.get(encadrant.getIdp())
                                               .getOrDefault(dateStr, 0);
        // SUPPRIME : plus de "if (encadrantDailyLoad > minDailyLoad) continue;"
        // On ne court-circuite plus avant d'avoir evalue le jury.

        for (int slot : slots) {
            // ... verification disponibilite encadrant, salle libre, etc.

            Professeur[] pickedJury = jurySelectionStrategy.selectJury(
                    encadrant, available, profJuryCount, globalMinLoad, nlpResult, config);
            if (pickedJury == null) continue;

            // Score du jury : la charge max parmi les 2 rapporteurs choisis
            int juryMaxLoad = Math.max(
                    profJuryCount.getOrDefault(pickedJury[0].getIdp(), 0),
                    profJuryCount.getOrDefault(pickedJury[1].getIdp(), 0)
            );

            // Selection du meilleur creneau par ordre lexicographique :
            // 1. Jury le moins charge  2. Encadrant daily load  3. Slot occupancy
            if (isBetterChoice(juryMaxLoad, encadrantDailyLoad, slotLoad,
                    bestJuryMaxLoad, minDailyLoad, minSlotLoad)) {
                bestChoice = new PlanningChoice(dayIdx, slot, freeSalle, 
                                               pickedJury[0], pickedJury[1]);
                bestJuryMaxLoad = juryMaxLoad;
                minDailyLoad = encadrantDailyLoad;
                minSlotLoad = slotLoad;
            }
        }
    }
    return bestChoice;
}
```

#### Nouvelle methode `isBetterChoice`

```java
/**
 * Preference lexicographique : choisir le creneau dont le jury a la charge
 * maximale la plus basse. C'est le critere dominant pour l'equite.
 * En cas d'egalite : charge quotidienne de l'encadrant, puis occupation du slot.
 */
private boolean isBetterChoice(int juryMaxLoad, int encadrantDailyLoad, int slotLoad,
                               int bestJuryMaxLoad, int minDailyLoad, int minSlotLoad) {
    if (juryMaxLoad != bestJuryMaxLoad) {
        return juryMaxLoad < bestJuryMaxLoad;   // PRIORITE 1 : equite jury
    }
    if (encadrantDailyLoad != minDailyLoad) {
        return encadrantDailyLoad < minDailyLoad;  // PRIORITE 2
    }
    return slotLoad < minSlotLoad;  // PRIORITE 3
}
```

**Pourquoi** : L'equite jury est maintenant le **critere dominant**. Le planning prefere activement un creneau ou le jury sera compose de profs peu charges, meme si l'encadrant a un jour legerement plus occupe.

---

### 5. Tri des projets par charge d'encadrement

**Fichier** : `services/PlanningServiceImpl.java`

**Nouvelles methodes** :

```java
private Long encadrantIdOf(List<Affectation> project) {
    if (project == null || project.isEmpty()) return null;
    Affectation main = project.get(0);
    if (main.getEncadrant() == null) return null;
    return main.getEncadrant().getIdp();
}

private Map<Long, Integer> countEncadrantProjects(List<List<Affectation>> projects) {
    Map<Long, Integer> count = new HashMap<>();
    for (List<Affectation> p : projects) {
        Long id = encadrantIdOf(p);
        if (id != null) {
            count.merge(id, 1, Integer::sum);
        }
    }
    return count;
}
```

**Utilisation** :

```java
// Tri : les encadrants avec le plus de projets sont planifies en premier
Map<Long, Integer> encadrantProjectCount = countEncadrantProjects(projects);
projects.sort(Comparator.comparingInt(
    (List<Affectation> proj) -> -encadrantProjectCount.getOrDefault(encadrantIdOf(proj), 0)));
```

**Pourquoi** : Les encadrants tres charges ont le moins de disponibilites (ils sont "president" a chaque soutenance de leurs etudiants). En les planifiant en premier, on leur reserve les meilleurs creneaux et on laisse plus de flexibilite pour le balancement jury des projets suivants.

---

### 6. Verification post-generation dans `VerificationServiceImpl`

**Fichier** : `services/VerificationServiceImpl.java`

**Nouvelle methode** :

```java
private void verifyJuryLoadDistribution(List<Soutenance> soutenances,
                                        Map<Long, Affectation> affectationByStudent,
                                        VerificationReport report) {
    int maxJuryLoadGap = PlanningConfig.defaults().getMaxJuryLoadGap();

    // Compter les participations jury (role rapporteur uniquement)
    Map<Long, Integer> juryCountByProfessor = new LinkedHashMap<>();
    Map<Long, String> namesByProfessor = new HashMap<>();

    for (Professeur professeur : professeurDAO.findAll()) {
        if (professeur.getIdp() == null) continue;
        juryCountByProfessor.put(professeur.getIdp(), 0);
        namesByProfessor.put(professeur.getIdp(), professorName(professeur));
    }

    // Deduplique les binomes (meme jury pour les 2 etudiants d'un meme projet)
    Set<String> countedProjects = new HashSet<>();

    for (Soutenance soutenance : soutenances) {
        Jury jury = soutenance.getJury();
        if (jury == null) continue;

        Etudiant etudiant = soutenance.getEtudiant();
        String projKey = projectKey(etudiant);
        if (!countedProjects.add(projKey)) continue;

        if (jury.getRapporteur1() != null && jury.getRapporteur1().getIdp() != null) {
            juryCountByProfessor.merge(jury.getRapporteur1().getIdp(), 1, Integer::sum);
        }
        if (jury.getRapporteur2() != null && jury.getRapporteur2().getIdp() != null) {
            juryCountByProfessor.merge(jury.getRapporteur2().getIdp(), 1, Integer::sum);
        }
    }

    // Trouver min et max parmi les profs qui participent au moins une fois
    int minLoad = Integer.MAX_VALUE;
    int maxLoad = 0;
    for (Map.Entry<Long, Integer> entry : juryCountByProfessor.entrySet()) {
        int count = entry.getValue();
        if (count > 0 && count < minLoad) minLoad = count;
        if (count > maxLoad) maxLoad = count;
    }

    if (minLoad == Integer.MAX_VALUE) return;  // pas de donnees jury

    int actualGap = maxLoad - minLoad;

    // Alerte globale si l'ecart depasse le seuil
    if (actualGap > maxJuryLoadGap) {
        report.addIssue("ALERTE", "Planning",
                "Repartition jury non equitable",
                "L'ecart entre le minimum (" + minLoad + ") et le maximum (" + maxLoad
                        + ") de participations jury est de " + actualGap
                        + ", ce qui depasse le seuil autorise de " + maxJuryLoadGap + ".");
    }

    // Alertes individuelles pour chaque prof surcharge
    for (Map.Entry<Long, Integer> entry : juryCountByProfessor.entrySet()) {
        int count = entry.getValue();
        if (count > minLoad + maxJuryLoadGap) {
            report.addIssue("ALERTE", "Planning",
                    "Surcharge jury professeur",
                    namesByProfessor.get(entry.getKey()) + " participe a " + count
                            + " jury(s), alors que le minimum est " + minLoad
                            + " (ecart autorise: " + maxJuryLoadGap + ").");
        }
    }
}
```

**Appel** ajoute dans `verifyPlanning()` :

```java
private void verifyPlanning(List<Affectation> affectations, List<Soutenance> soutenances, 
                            VerificationReport report) {
    // ... verifications existantes ...
    verifyMissingProjects(expectedProjects, defensesByProject.keySet(), report);
    verifyMissingStudents(expectedStudentNames, scheduledStudentIds, report);
    verifyRoomOverlaps(projectsByRoomSlot, defensesByProject, report);
    verifyProfessorOverlaps(projectsByProfessorSlot, defensesByProject, report);
    verifyProfessorRest(scheduleByProfessorAndDate, defensesByProject, report);
    verifyJuryLoadDistribution(soutenances, affectationByStudent, report);  // <-- NOUVEAU
}
```

**Pourquoi** : Meme si l'algorithme est maintenant correct, la verification agit comme un filet de securite. Si une future modification ou un cas limite produit un depassement, l'utilisateur est immediatement alerte via le dashboard.

---

## Resultat Attendu

| Metrique | Avant | Apres |
|----------|-------|-------|
| Ecart min-max jury | 5 (min=5, max=10) | <= 3 (le `maxJuryLoadGap` configure) |
| Alertes dashboard | 3 alertes | 0 alerte |
| Nombre de soutenances planifiees | Identique | Identique |
| Autres contraintes (salle, repos, etc.) | Respectees | Toujours respectees |

## Fichiers Modifies

| Fichier | Nature du changement |
|---------|---------------------|
| `services/JurySelectionStrategy.java` | Ajout parametre `globalMinLoad` dans l'interface |
| `services/DefaultJurySelectionStrategy.java` | Reecriture avec `loadCeiling`, `pickPair`, NLP corrige |
| `services/PlanningServiceImpl.java` | `computeGlobalMinLoad`, scoring par charge jury, tri projets |
| `services/VerificationServiceImpl.java` | `verifyJuryLoadDistribution` pour le dashboard |

## Configuration

Le seuil est defini dans `PlanningConfig.defaults()` :

```java
public static PlanningConfig defaults() {
    return new PlanningConfig(
        new int[]{9, 10, 11, 14, 15, 16, 17},  // slots
        2026, Calendar.JUNE, 23,                 // date debut
        4,                                       // maxDays
        List.of("S3 AB", "S4 AB", "S3 NB", "S2 NB", "AMPHI A"),
        /* palette couleurs... */,
        3  // <-- maxJuryLoadGap : ecart max tolere
    );
}
```

Pour ajuster la tolerance, modifier la derniere valeur. Un gap de 2 = distribution tres stricte. Un gap de 4 = plus de flexibilite pour le NLP matching.
