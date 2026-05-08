import pandas as pd
import random
import os

# Define some random topics
sujets_gi = [
    "Development of a web application for project management",
    "Conception d'une application mobile de gestion de stock",
    "Mise en place d'une architecture microservices",
    "Building an e-commerce platform with Spring Boot and React",
    "Optimisation des performances d'une base de données",
    "Création d'un ERP pour la gestion RH"
]

sujets_id = [
    "Predictive modeling for customer churn",
    "Analyse de données et création de dashboard BI",
    "Implémentation d'un data lake avec Hadoop",
    "Real-time data streaming pipeline using Kafka",
    "Machine learning pour la détection de fraudes",
    "Web scraping et analyse de sentiments"
]

sujets_tdia = [
    "Deep learning for medical image segmentation",
    "Création d'un chatbot intelligent avec NLP",
    "Reconnaissance faciale et sécurité",
    "Implementation of a recommendation system",
    "Transformation digitale des processus d'une entreprise",
    "Génération de texte avec des modèles LLM"
]

specialites_profs = [
    "Développement Web", "Génie Logiciel", "Intelligence Artificielle", 
    "Réseaux et Sécurité", "Data Science", "Bases de données",
    "Cloud Computing", "Anglais"
]

# Ensure at least one English teacher
specialites_profs_extended = specialites_profs * 3 + ["Anglais", "Anglais"]

def update_students(filename, sujets_list):
    if not os.path.exists(filename):
        print(f"File not found: {filename}")
        return
    
    df = pd.read_excel(filename)
    
    # Check if we need to add the column F (which is index 5)
    # The columns should be: 0: CNE, 1: Nom, 2: Prenom, 3: Email, 4: Binome, 5: Sujet
    
    # Let's see the current columns
    # We'll just assign a random subject to each row
    new_sujets = [random.choice(sujets_list) for _ in range(len(df))]
    
    # Name the column 'Sujet_Stage'
    df['Sujet_Stage'] = new_sujets
    
    df.to_excel(filename, index=False)
    print(f"Updated {filename}")

def update_profs(filename):
    if not os.path.exists(filename):
        print(f"File not found: {filename}")
        return
        
    df = pd.read_excel(filename)
    
    # Assign random specialities
    # Wait, the structure in ExcelImporter: 0: Nom, 1: Prenom, 2: Specialite
    new_specs = [random.choice(specialites_profs_extended) for _ in range(len(df))]
    
    # If the column already exists, update it. Otherwise create it.
    if len(df.columns) > 2:
        df.iloc[:, 2] = new_specs
    else:
        df['Spécialité'] = new_specs
        
    df.to_excel(filename, index=False)
    print(f"Updated {filename}")

base_dir = r"c:\Users\yassi\OneDrive\Desktop\rapport_tp2-3_jee"

import glob

# Because of encoding issues with accents, use glob
gi_file = glob.glob(os.path.join(base_dir, "G*nie Informatique 3 Option GL_Email.xlsx"))
id_file = glob.glob(os.path.join(base_dir, "Ing*nierie des donn*es 3_Email.xlsx"))
tdia_file = glob.glob(os.path.join(base_dir, "Transformation Digitale & Intelligence Artificielle 3_Email.xlsx"))
prof_file = glob.glob(os.path.join(base_dir, "Liste des Profs.xlsx"))

if gi_file: update_students(gi_file[0], sujets_gi)
if id_file: update_students(id_file[0], sujets_id)
if tdia_file: update_students(tdia_file[0], sujets_tdia)
if prof_file: update_profs(prof_file[0])

