package services;

import java.util.ArrayList;
import java.util.List;

public class RecommendationServiceImpl implements RecommendationService {

    @Override
    public List<Recommendation> generateRecommendations(int totalProjects, int numberOfRooms, int slotsPerDay,
                                                        int numberOfDays, int numberOfProfs, PlanningConfig config) {
        List<Recommendation> out = new ArrayList<>();

        if (totalProjects <= 0) {
            out.add(Recommendation.info("Aucun projet a planifier",
                    "Aucune affectation n'a ete enregistree. Lancez d'abord l'affectation des encadrants."));
            return out;
        }

        // ── Capacity check ───────────────────────────────────────────────────
        int dailyCapacity = Math.max(0, numberOfRooms) * Math.max(0, slotsPerDay);
        int totalCapacity = dailyCapacity * Math.max(0, numberOfDays);
        int recommendedDays = dailyCapacity > 0 ? (int) Math.ceil(totalProjects / (double) dailyCapacity) : 0;

        if (numberOfRooms <= 0) {
            out.add(Recommendation.error("Aucune salle selectionnee",
                    "Selectionnez au moins une salle pour pouvoir generer le planning.",
                    "Ajoutez ou selectionnez une salle dans la section 'Salles'."));
        }
        if (slotsPerDay <= 0) {
            out.add(Recommendation.error("Aucun creneau disponible",
                    "Les parametres horaires actuels ne produisent aucun creneau exploitable.",
                    "Verifiez les heures de debut/fin et la duree d'une soutenance."));
        }

        if (totalCapacity > 0) {
            if (totalCapacity < totalProjects) {
                int missing = totalProjects - totalCapacity;
                out.add(Recommendation.error(
                        "Capacite insuffisante",
                        "Avec " + numberOfRooms + " salle(s), " + slotsPerDay + " creneau(x)/jour et "
                                + numberOfDays + " jour(s), vous pouvez planifier au maximum "
                                + totalCapacity + " soutenances. Vous avez " + totalProjects
                                + " projets a planifier (manque " + missing + ").",
                        "Ajoutez des salles, allongez la plage horaire, ou augmentez le nombre de jours a "
                                + recommendedDays + "."));
            } else if (recommendedDays > 0 && numberOfDays > recommendedDays + 1) {
                out.add(Recommendation.info(
                        "Nombre de jours genereux",
                        "Vous pourriez generer le planning en " + recommendedDays + " jour(s) au lieu de "
                                + numberOfDays + " (capacite suffisante)."));
            } else {
                out.add(Recommendation.info(
                        "Capacite suffisante",
                        "Capacite totale: " + totalCapacity + " creneaux. " + totalProjects + " projets a planifier."));
            }
        }

        if (recommendedDays > 0 && numberOfDays > 0 && numberOfDays < recommendedDays) {
            out.add(Recommendation.warning(
                    "Nombre de jours faible",
                    "Vous avez " + totalProjects + " projets et " + slotsPerDay + " creneau(x)/jour pour "
                            + numberOfRooms + " salle(s). Recommandation: au moins " + recommendedDays
                            + " jour(s) de soutenances.",
                    "Augmentez le nombre de jours a " + recommendedDays + " (ou plus)."));
        }

        // ── Professor sufficiency ───────────────────────────────────────────
        if (numberOfProfs > 0 && totalProjects > 0) {
            // Each project needs 3 distinct professors (encadrant + 2 rapporteurs).
            // Average jury participations per professor = 2 * totalProjects / numberOfProfs (encadrant role excluded).
            double avgJury = (2.0 * totalProjects) / numberOfProfs;
            if (numberOfProfs < 3) {
                out.add(Recommendation.error("Pas assez de professeurs",
                        "Il faut au moins 3 professeurs pour former un jury complet. Disponibles: " + numberOfProfs + ".",
                        "Ajoutez des professeurs dans le fichier Excel."));
            } else if (avgJury > 8) {
                out.add(Recommendation.warning(
                        "Charge jury elevee",
                        "Avec " + numberOfProfs + " professeurs, chaque prof participera en moyenne a "
                                + String.format("%.1f", avgJury) + " jurys.",
                        "Reduisez 'Max soutenances/prof/jour' ou ajoutez des professeurs."));
            }

            int maxPerProfPerDay = config.getConstraints().getMaxSoutenancesPerProfPerDay();
            int profCapacityPerDay = numberOfProfs * maxPerProfPerDay;
            int neededProfSlotsPerDay = (int) Math.ceil(totalProjects * 3.0 / Math.max(1, numberOfDays));
            if (profCapacityPerDay > 0 && neededProfSlotsPerDay > profCapacityPerDay) {
                out.add(Recommendation.warning(
                        "Limite par prof/jour serree",
                        "Limite actuelle: " + maxPerProfPerDay + " soutenances/prof/jour x " + numberOfProfs
                                + " profs = " + profCapacityPerDay + " participations/jour. Besoin estime: "
                                + neededProfSlotsPerDay + ".",
                        "Augmentez 'Max soutenances/prof/jour' ou ajoutez des jours."));
            }
        }

        // ── Rest constraint feasibility ─────────────────────────────────────
        int rest = config.getConstraints().getProfRestHours();
        if (rest >= slotsPerDay && slotsPerDay > 0) {
            out.add(Recommendation.warning(
                    "Repos professeur trop strict",
                    "Le repos demande (" + rest + "h) est superieur ou egal au nombre de creneaux/jour ("
                            + slotsPerDay + ").",
                    "Reduisez le repos a " + Math.max(0, slotsPerDay - 1) + "h ou allongez la plage horaire."));
        }

        // ── Time configuration sanity ───────────────────────────────────────
        if (config.getSoutenanceDurationMinutes() <= 0) {
            out.add(Recommendation.error("Duree invalide",
                    "La duree d'une soutenance doit etre superieure a 0.",
                    "Saisissez une duree (en minutes) realiste, ex: 60."));
        }
        if (config.getSoutenanceDurationMinutes() > 0 && slotsPerDay > 0) {
            int totalMinutesPerDay = slotsPerDay * config.getSlotStrideMinutes();
            int hours = totalMinutesPerDay / 60;
            int minutes = totalMinutesPerDay % 60;
            out.add(Recommendation.info("Plage horaire effective",
                    "Vous disposez de " + slotsPerDay + " creneau(x)/jour ("
                            + hours + "h" + (minutes > 0 ? minutes : "") + " utiles)."));
        }

        return out;
    }
}
