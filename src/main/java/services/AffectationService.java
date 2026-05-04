package services;

import java.util.List;

import entities.Affectation;

public interface AffectationService {

	List<Affectation> lancerAffectation(List<String> filieres, List<String> debug);

}
