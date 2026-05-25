package services;

import dao.AffectationDAO;
import dao.AffectationDAOImpl;
import dao.ProfesseurDAO;
import dao.ProfesseurDAOImpl;
import dao.SoutenanceDAO;
import dao.SoutenanceDAOImpl;
import entities.Affectation;
import entities.Etudiant;
import entities.Jury;
import entities.Professeur;
import entities.Salle;
import entities.Soutenance;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public class VerificationServiceImpl implements VerificationService {

    private final AffectationDAO affectationDAO;
    private final ProfesseurDAO professeurDAO;
    private final SoutenanceDAO soutenanceDAO;

    public VerificationServiceImpl() {
        this(new AffectationDAOImpl(), new ProfesseurDAOImpl(), new SoutenanceDAOImpl());
    }

    public VerificationServiceImpl(AffectationDAO affectationDAO,
                                   ProfesseurDAO professeurDAO,
                                   SoutenanceDAO soutenanceDAO) {
        this.affectationDAO = Objects.requireNonNull(affectationDAO);
        this.professeurDAO = Objects.requireNonNull(professeurDAO);
        this.soutenanceDAO = Objects.requireNonNull(soutenanceDAO);
    }

    @Override
    public VerificationReport verifyGeneratedFiles(List<String> filieresFiltre) {
        return verifyGeneratedFiles(filieresFiltre, ConstraintSet.defaults());
    }

    @Override
    public VerificationReport verifyGeneratedFiles(List<String> filieresFiltre, ConstraintSet constraints) {
        ConstraintSet c = constraints == null ? ConstraintSet.defaults() : constraints;

        VerificationReport report = new VerificationReport();
        List<Affectation> affectations = filterAffectations(affectationDAO.findAllWithDetails(), filieresFiltre);
        List<Soutenance> soutenances = filterSoutenances(soutenanceDAO.findAllWithDetails(), filieresFiltre);

        report.setAffectationDataAvailable(!affectations.isEmpty());
        report.setPlanningDataAvailable(!soutenances.isEmpty());

        verifyAffectations(affectations, report);
        verifyPlanning(affectations, soutenances, report, c);

        if (report.isPlanningDataAvailable()) {
            report.setNlpSummary(buildLocalSummary(report));
        }

        return report;
    }

    // ─── Filtering ──────────────────────────────────────────────────────────

    private List<Affectation> filterAffectations(List<Affectation> source, List<String> filieresFiltre) {
        if (filieresFiltre == null || filieresFiltre.isEmpty()) return source;
        List<Affectation> result = new ArrayList<>();
        for (Affectation affectation : source) {
            Etudiant etudiant = affectation.getEtudiant();
            if (etudiant != null && filieresFiltre.contains(etudiant.getFiliere())) {
                result.add(affectation);
            }
        }
        return result;
    }

    private List<Soutenance> filterSoutenances(List<Soutenance> source, List<String> filieresFiltre) {
        if (filieresFiltre == null || filieresFiltre.isEmpty()) return source;
        List<Soutenance> result = new ArrayList<>();
        for (Soutenance soutenance : source) {
            Etudiant etudiant = soutenance.getEtudiant();
            if (etudiant != null && filieresFiltre.contains(etudiant.getFiliere())) {
                result.add(soutenance);
            }
        }
        return result;
    }

    // ─── Affectations ───────────────────────────────────────────────────────

    private void verifyAffectations(List<Affectation> affectations, VerificationReport report) {
        if (affectations.isEmpty()) return;

        Map<Long, Integer> loadByProfessor = new LinkedHashMap<>();
        Map<Long, String> namesByProfessor = new HashMap<>();
        Set<Long> affectedStudents = new HashSet<>();
        Set<Long> professorIds = new HashSet<>();

        for (Professeur professeur : professeurDAO.findAll()) {
            if (professeur.getIdp() == null) continue;
            loadByProfessor.put(professeur.getIdp(), 0);
            namesByProfessor.put(professeur.getIdp(), professorName(professeur));
            professorIds.add(professeur.getIdp());
        }

        for (Affectation affectation : affectations) {
            Etudiant etudiant = affectation.getEtudiant();
            Professeur encadrant = affectation.getEncadrant();

            if (etudiant == null) {
                report.addIssue("CRITIQUE", "Affectation", "Affectation sans etudiant",
                        "Une ligne d'affectation ne contient aucun etudiant rattache.");
                continue;
            }
            if (!affectedStudents.add(etudiant.getIde())) {
                report.addIssue("CRITIQUE", "Affectation", "Etudiant affecte plusieurs fois",
                        studentName(etudiant) + " apparait dans plusieurs affectations.");
            }
            if (encadrant == null) {
                report.addIssue("CRITIQUE", "Affectation", "Encadrant manquant",
                        studentName(etudiant) + " n'a pas d'encadrant.");
                continue;
            }
            if (encadrant.getIdp() == null) {
                report.addIssue("CRITIQUE", "Affectation", "Encadrant invalide",
                        studentName(etudiant) + " pointe vers un professeur sans identifiant.");
                continue;
            }

            loadByProfessor.putIfAbsent(encadrant.getIdp(), 0);
            loadByProfessor.merge(encadrant.getIdp(), 1, Integer::sum);
            namesByProfessor.put(encadrant.getIdp(), professorName(encadrant));
        }

        if (loadByProfessor.isEmpty()) return;

        double average = affectations.size() / (double) loadByProfessor.size();
        int expectedMin = average >= 3.0 && average <= 4.0 ? 3 : Math.max(0, (int) Math.floor(average));
        int expectedMax = average >= 3.0 && average <= 4.0 ? 4 : Math.max(expectedMin, (int) Math.ceil(average));
        report.setEncadrementAverage(average);
        report.setEncadrementMinExpected(expectedMin);
        report.setEncadrementMaxExpected(expectedMax);

        for (Map.Entry<Long, Integer> entry : loadByProfessor.entrySet()) {
            int load = entry.getValue();
            if (load < expectedMin || load > expectedMax) {
                report.addIssue("ALERTE", "Affectation", "Repartition non equitable",
                        namesByProfessor.get(entry.getKey()) + " encadre " + load
                                + " etudiant(s), alors que la plage attendue est "
                                + expectedMin + "-" + expectedMax + " (moyenne "
                                + String.format(java.util.Locale.US, "%.2f", average) + ").");
            }
        }

        for (Long idp : loadByProfessor.keySet()) {
            if (!professorIds.contains(idp)) {
                report.addIssue("CRITIQUE", "Affectation", "Professeur introuvable",
                        namesByProfessor.get(idp)
                                + " est utilise dans une affectation mais n'existe plus dans la liste des professeurs.");
            }
        }
    }

    // ─── Planning ───────────────────────────────────────────────────────────

    private void verifyPlanning(List<Affectation> affectations, List<Soutenance> soutenances,
                                VerificationReport report, ConstraintSet c) {
        if (soutenances.isEmpty()) return;

        Map<Long, Affectation> affectationByStudent = new HashMap<>();
        Set<String> expectedProjects = new LinkedHashSet<>();
        Map<Long, String> expectedStudentNames = new LinkedHashMap<>();
        Set<Long> scheduledStudentIds = new HashSet<>();
        for (Affectation affectation : affectations) {
            if (affectation.getEtudiant() == null) continue;
            affectationByStudent.put(affectation.getEtudiant().getIde(), affectation);
            expectedProjects.add(projectKey(affectation.getEtudiant()));
            expectedStudentNames.put(affectation.getEtudiant().getIde(), studentName(affectation.getEtudiant()));
        }

        Map<String, DefenseView> defensesByProject = new LinkedHashMap<>();
        Map<String, Set<String>> projectsByRoomSlot = new HashMap<>();
        Map<String, Set<String>> projectsByProfessorSlot = new HashMap<>();
        Map<Long, Map<String, List<DefenseView>>> scheduleByProfessorAndDate = new HashMap<>();
        Map<Long, Integer> juryParticipations = new HashMap<>();

        for (Soutenance soutenance : soutenances) {
            Etudiant etudiant = soutenance.getEtudiant();
            if (etudiant != null && etudiant.getIde() != null) {
                scheduledStudentIds.add(etudiant.getIde());
            }
            String projectKey = projectKey(etudiant);
            DefenseView defense = defensesByProject.computeIfAbsent(projectKey,
                    key -> new DefenseView(projectKey, soutenance));
            if (!defense.matches(soutenance)) {
                report.addIssue("CRITIQUE", "Planning", "Projet planifie sur plusieurs creneaux",
                        "Le projet " + projectKey + " apparait avec des dates, heures ou salles differentes.");
            }
            defense.addSoutenance(soutenance);

            verifySoutenanceBasics(soutenance, affectationByStudent, report, c);

            String dateKey = dateKey(soutenance);
            String hourKey = safe(soutenance.getHeure());
            String roomKey = salleKey(soutenance.getSalle());
            if (!dateKey.isEmpty() && !hourKey.isEmpty() && !roomKey.isEmpty()) {
                projectsByRoomSlot.computeIfAbsent(dateKey + "|" + hourKey + "|" + roomKey,
                        key -> new TreeSet<>()).add(projectKey);
            }

            Jury jury = soutenance.getJury();
            if (jury != null) {
                if (jury.getRapporteur1() != null) {
                    juryParticipations.merge(jury.getRapporteur1().getIdp(), 1, Integer::sum);
                }
                if (jury.getRapporteur2() != null) {
                    juryParticipations.merge(jury.getRapporteur2().getIdp(), 1, Integer::sum);
                }
            }

            for (Professeur professeur : professorsInDefense(soutenance, affectationByStudent)) {
                String professorSlotKey = dateKey + "|" + hourKey + "|" + professeur.getIdp();
                projectsByProfessorSlot.computeIfAbsent(professorSlotKey,
                        key -> new TreeSet<>()).add(projectKey);
                scheduleByProfessorAndDate
                        .computeIfAbsent(professeur.getIdp(), key -> new HashMap<>())
                        .computeIfAbsent(dateKey, key -> new ArrayList<>())
                        .add(defense);
            }
        }

        verifyMissingProjects(expectedProjects, defensesByProject.keySet(), report);
        verifyMissingStudents(expectedStudentNames, scheduledStudentIds, report);
        verifyRoomOverlaps(projectsByRoomSlot, defensesByProject, report);
        verifyProfessorOverlaps(projectsByProfessorSlot, defensesByProject, report);
        verifyProfessorRest(scheduleByProfessorAndDate, defensesByProject, report, c);
        verifyJuryLoadGap(juryParticipations, report, c);
    }

    private void verifySoutenanceBasics(Soutenance soutenance, Map<Long, Affectation> affectationByStudent,
                                        VerificationReport report, ConstraintSet c) {
        Etudiant etudiant = soutenance.getEtudiant();
        if (etudiant == null) {
            report.addIssue("CRITIQUE", "Planning", "Soutenance sans etudiant",
                    "Une ligne du planning n'a aucun etudiant rattache.");
            return;
        }
        if (!affectationByStudent.containsKey(etudiant.getIde())) {
            report.addIssue("CRITIQUE", "Planning", "Etudiant non affecte planifie",
                    studentName(etudiant) + " est planifie sans affectation correspondante.");
        }
        if (soutenance.getDate() == null || soutenance.getHeure() == null
                || soutenance.getHeure().trim().isEmpty()) {
            report.addIssue("CRITIQUE", "Planning", "Creneau incomplet",
                    studentName(etudiant) + " n'a pas de date ou d'heure valide.");
        }
        if (soutenance.getSalle() == null) {
            report.addIssue("CRITIQUE", "Planning", "Salle manquante",
                    studentName(etudiant) + " n'a pas de salle assignee.");
        }

        Jury jury = soutenance.getJury();
        if (jury == null || jury.getPresident() == null
                || jury.getRapporteur1() == null || jury.getRapporteur2() == null) {
            report.addIssue("CRITIQUE", "Planning", "Jury incomplet",
                    studentName(etudiant) + " n'a pas un jury complet (president + 2 rapporteurs).");
            return;
        }

        Set<Long> uniqueProfessors = new HashSet<>();
        uniqueProfessors.add(jury.getPresident().getIdp());
        uniqueProfessors.add(jury.getRapporteur1().getIdp());
        uniqueProfessors.add(jury.getRapporteur2().getIdp());
        if (uniqueProfessors.size() < c.getMinDistinctJuryMembers()) {
            report.addIssue("CRITIQUE", "Planning", "Professeur duplique dans le jury",
                    studentName(etudiant) + " a le meme professeur dans plusieurs roles du jury.");
        }

        Affectation affectation = affectationByStudent.get(etudiant.getIde());
        if (affectation != null && affectation.getEncadrant() != null) {
            Long encadrantId = affectation.getEncadrant().getIdp();
            if (c.isEncadrantPresidentRequired() && !encadrantId.equals(jury.getPresident().getIdp())) {
                String severity = c.isEncadrantPresidentHard() ? "CRITIQUE" : "ALERTE";
                report.addIssue(severity, "Planning", "President different de l'encadrant",
                        studentName(etudiant) + " a pour encadrant " + professorName(affectation.getEncadrant())
                                + ", mais le president du jury est " + professorName(jury.getPresident()) + ".");
            }
            if (c.isEncadrantRapporteurForbidden()
                    && (encadrantId.equals(jury.getRapporteur1().getIdp())
                        || encadrantId.equals(jury.getRapporteur2().getIdp()))) {
                report.addIssue("CRITIQUE", "Planning", "Encadrant aussi rapporteur",
                        studentName(etudiant) + " a son encadrant affecte comme rapporteur.");
            }
        }
    }

    private void verifyMissingProjects(Set<String> expectedProjects, Set<String> scheduledProjects,
                                       VerificationReport report) {
        for (String expected : expectedProjects) {
            if (!scheduledProjects.contains(expected)) {
                report.addIssue("ALERTE", "Planning", "Projet affecte non planifie",
                        "Le projet " + expected + " existe dans les affectations mais pas dans le planning.");
            }
        }
    }

    private void verifyMissingStudents(Map<Long, String> expectedStudentNames, Set<Long> scheduledStudentIds,
                                       VerificationReport report) {
        for (Map.Entry<Long, String> expected : expectedStudentNames.entrySet()) {
            if (!scheduledStudentIds.contains(expected.getKey())) {
                report.addIssue("ALERTE", "Planning", "Etudiant affecte non planifie",
                        expected.getValue() + " existe dans les affectations mais pas dans le planning.");
            }
        }
    }

    private void verifyRoomOverlaps(Map<String, Set<String>> projectsByRoomSlot,
                                    Map<String, DefenseView> defensesByProject, VerificationReport report) {
        for (Map.Entry<String, Set<String>> entry : projectsByRoomSlot.entrySet()) {
            if (entry.getValue().size() > 1) {
                report.addIssue("CRITIQUE", "Planning", "Chevauchement de salle",
                        slotLabel(entry.getKey()) + " contient plusieurs projets: "
                                + projectNames(entry.getValue(), defensesByProject) + ".");
            }
        }
    }

    private void verifyProfessorOverlaps(Map<String, Set<String>> projectsByProfessorSlot,
                                         Map<String, DefenseView> defensesByProject, VerificationReport report) {
        for (Map.Entry<String, Set<String>> entry : projectsByProfessorSlot.entrySet()) {
            if (entry.getValue().size() > 1) {
                report.addIssue("CRITIQUE", "Planning", "Professeur affecte au meme horaire",
                        slotLabel(entry.getKey()) + " concerne plusieurs projets: "
                                + projectNames(entry.getValue(), defensesByProject) + ".");
            }
        }
    }

    private void verifyProfessorRest(Map<Long, Map<String, List<DefenseView>>> scheduleByProfessorAndDate,
                                     Map<String, DefenseView> defensesByProject, VerificationReport report,
                                     ConstraintSet c) {
        int requiredRestHours = c.getProfRestHours();
        if (requiredRestHours <= 0) return;
        String severity = c.isProfRestHard() ? "CRITIQUE" : "ALERTE";

        Set<String> reportedPairs = new HashSet<>();
        for (Map.Entry<Long, Map<String, List<DefenseView>>> professorEntry : scheduleByProfessorAndDate.entrySet()) {
            for (Map.Entry<String, List<DefenseView>> dateEntry : professorEntry.getValue().entrySet()) {
                List<DefenseView> defenses = new ArrayList<>(new LinkedHashSet<>(dateEntry.getValue()));
                for (int i = 0; i < defenses.size(); i++) {
                    for (int j = i + 1; j < defenses.size(); j++) {
                        DefenseView first = defenses.get(i);
                        DefenseView second = defenses.get(j);
                        if (first.projectKey.equals(second.projectKey)) continue;
                        int firstSlot = parseHour(first.hour);
                        int secondSlot = parseHour(second.hour);
                        if (firstSlot < 0 || secondSlot < 0) continue;
                        int gapHours = Math.abs(firstSlot - secondSlot);
                        if (gapHours > 0 && gapHours <= requiredRestHours) {
                            String pairKey = professorEntry.getKey() + "|" + dateEntry.getKey()
                                    + "|" + Math.min(firstSlot, secondSlot)
                                    + "|" + Math.max(firstSlot, secondSlot);
                            if (reportedPairs.add(pairKey)) {
                                List<String> conflicts = new ArrayList<>();
                                conflicts.add(first.projectKey);
                                conflicts.add(second.projectKey);
                                report.addIssue(severity, "Planning", "Repos professeur insuffisant",
                                        dateEntry.getKey() + " : un professeur enchaine deux soutenances avec un ecart de "
                                                + gapHours + "h (repos requis: " + requiredRestHours + "h) entre "
                                                + first.hour + " et " + second.hour + " ("
                                                + projectNames(conflicts, defensesByProject) + ").");
                            }
                        }
                    }
                }
            }
        }
    }

    private void verifyJuryLoadGap(Map<Long, Integer> juryParticipations, VerificationReport report,
                                   ConstraintSet c) {
        if (juryParticipations.isEmpty()) return;
        int min = Integer.MAX_VALUE, max = Integer.MIN_VALUE;
        for (int v : juryParticipations.values()) { min = Math.min(min, v); max = Math.max(max, v); }
        int gap = max - min;
        if (gap > c.getMaxJuryLoadGap()) {
            String severity = c.isMaxJuryLoadGapHard() ? "CRITIQUE" : "ALERTE";
            report.addIssue(severity, "Planning", "Charge jury inegale",
                    "Ecart de participations entre profs: " + gap
                            + " (max attendu: " + c.getMaxJuryLoadGap() + ").");
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private List<Professeur> professorsInDefense(Soutenance soutenance,
                                                 Map<Long, Affectation> affectationByStudent) {
        Map<Long, Professeur> result = new LinkedHashMap<>();
        if (soutenance.getEtudiant() != null) {
            Affectation affectation = affectationByStudent.get(soutenance.getEtudiant().getIde());
            if (affectation != null && affectation.getEncadrant() != null) {
                result.put(affectation.getEncadrant().getIdp(), affectation.getEncadrant());
            }
        }
        Jury jury = soutenance.getJury();
        if (jury != null) {
            addProfessor(result, jury.getPresident());
            addProfessor(result, jury.getRapporteur1());
            addProfessor(result, jury.getRapporteur2());
        }
        return new ArrayList<>(result.values());
    }

    private void addProfessor(Map<Long, Professeur> map, Professeur professeur) {
        if (professeur != null && professeur.getIdp() != null) {
            map.put(professeur.getIdp(), professeur);
        }
    }

    private String buildLocalSummary(VerificationReport report) {
        if (report.getCriticalCount() > 0) {
            return "Des anomalies critiques ont ete detectees. Corrigez d'abord les conflits de salles, "
                    + "de professeurs ou les donnees manquantes avant d'utiliser le planning.";
        }
        if (report.getWarningCount() > 0) {
            return "Le planning est exploitable, mais certaines contraintes doivent etre verifiees. "
                    + "Priorite aux ecarts d'equite et aux temps de repos des professeurs.";
        }
        return "Aucune anomalie bloquante detectee. Les fichiers generes semblent conformes aux contraintes principales.";
    }

    private String projectNames(Iterable<String> projectKeys, Map<String, DefenseView> defensesByProject) {
        List<String> names = new ArrayList<>();
        for (String key : projectKeys) {
            DefenseView view = defensesByProject.get(key);
            names.add(view != null && !view.studentNames.isEmpty()
                    ? String.join(" & ", view.studentNames) : key);
        }
        return String.join(", ", names);
    }

    private String slotLabel(String compositeKey) {
        String[] parts = compositeKey.split("\\|");
        if (parts.length >= 2) return parts[0] + " a " + parts[1];
        return compositeKey;
    }

    private String projectKey(Etudiant etudiant) {
        if (etudiant == null) return "PROJET_INCONNU";
        String cne = safe(etudiant.getCne());
        if (cne.isEmpty()) cne = "IDE_" + etudiant.getIde();
        String binome = safe(etudiant.getBinome_cne());
        if (!binome.isEmpty()) {
            List<String> cnes = new ArrayList<>();
            cnes.add(cne);
            cnes.add(binome);
            Collections.sort(cnes);
            return cnes.get(0) + "_" + cnes.get(1);
        }
        return cne;
    }

    private String dateKey(Soutenance soutenance) {
        if (soutenance == null || soutenance.getDate() == null) return "";
        return new SimpleDateFormat("yyyy-MM-dd").format(soutenance.getDate());
    }

    private String salleKey(Salle salle) {
        if (salle == null) return "";
        if (salle.getId_salle() != null) return String.valueOf(salle.getId_salle());
        return safe(salle.getNum_salle());
    }

    private int parseHour(String heure) {
        try {
            String h = safe(heure).replace("h", "").trim();
            if (h.isEmpty()) return -1;
            if (h.contains(":")) return Integer.parseInt(h.split(":")[0]);
            if (h.length() > 2) return Integer.parseInt(h.substring(0, h.length() - 2));
            return Integer.parseInt(h);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String studentName(Etudiant etudiant) {
        if (etudiant == null) return "Etudiant inconnu";
        return (safe(etudiant.getNomE()) + " " + safe(etudiant.getPrenomE())).trim();
    }

    private String professorName(Professeur professeur) {
        if (professeur == null) return "Professeur inconnu";
        return (safe(professeur.getNom()) + " " + safe(professeur.getPrenom())).trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private class DefenseView {
        private final String projectKey;
        private final String date;
        private final String hour;
        private final String room;
        private final Set<String> studentNames = new LinkedHashSet<>();

        private DefenseView(String projectKey, Soutenance firstSoutenance) {
            this.projectKey = projectKey;
            this.date = dateKey(firstSoutenance);
            this.hour = safe(firstSoutenance.getHeure());
            this.room = salleKey(firstSoutenance.getSalle());
        }

        private void addSoutenance(Soutenance soutenance) {
            studentNames.add(studentName(soutenance.getEtudiant()));
        }

        private boolean matches(Soutenance soutenance) {
            return date.equals(dateKey(soutenance))
                    && hour.equals(safe(soutenance.getHeure()))
                    && room.equals(salleKey(soutenance.getSalle()));
        }
    }
}
