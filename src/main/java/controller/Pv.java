package controller;

public class Pv {
    private String id;
    private String professorId;
    private String professorName;
    private String studentName;
    private String filiere;
    private String date;
    private String heure;
    private String salle;
    private String fileName;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getProfessorId() { return professorId; }
    public void setProfessorId(String professorId) { this.professorId = professorId; }
    
    public String getProfessorName() { return professorName; }
    public void setProfessorName(String professorName) { this.professorName = professorName; }
    
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    
    public String getFiliere() { return filiere; }
    public void setFiliere(String filiere) { this.filiere = filiere; }
    
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    
    public String getHeure() { return heure; }
    public void setHeure(String heure) { this.heure = heure; }
    
    public String getSalle() { return salle; }
    public void setSalle(String salle) { this.salle = salle; }
    
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
}
