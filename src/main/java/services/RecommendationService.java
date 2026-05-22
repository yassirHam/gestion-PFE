package services;

import java.util.List;

/**
 * Computes intelligent recommendations to help the user pick a feasible
 * planning configuration before generation.
 */
public interface RecommendationService {

    /**
     * @param totalProjects  number of projects (not students) to schedule (binomes count once)
     * @param numberOfRooms  number of rooms the user has selected
     * @param slotsPerDay    number of time slots/day given the user's hours+duration
     * @param numberOfDays   number of working days the user has selected
     * @param numberOfProfs  total professors available
     * @param config         current planning configuration
     */
    List<Recommendation> generateRecommendations(int totalProjects, int numberOfRooms, int slotsPerDay,
                                                 int numberOfDays, int numberOfProfs, PlanningConfig config);
}
