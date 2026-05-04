package entities;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "fichier_liste")
public class FichierListe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nomFichier;
    private String filiere;
    private int nbEtudiants;

    @Temporal(TemporalType.TIMESTAMP)
    private Date dateUpload;

    public FichierListe() {}

    public FichierListe(String nomFichier, String filiere, int nbEtudiants) {
        this.nomFichier = nomFichier;
        this.filiere = filiere;
        this.nbEtudiants = nbEtudiants;
        this.dateUpload = new Date();
    }

    // GETTERS SETTERS
    public Long getId() { return id; }

    public String getNomFichier() { return nomFichier; }
    public void setNomFichier(String nomFichier) { this.nomFichier = nomFichier; }

    public String getFiliere() { return filiere; }
    public void setFiliere(String filiere) { this.filiere = filiere; }

    public int getNbEtudiants() { return nbEtudiants; }
    public void setNbEtudiants(int nbEtudiants) { this.nbEtudiants = nbEtudiants; }

    public Date getDateUpload() { return dateUpload; }
}