import docx
import pandas as pd
import re

docx_file = r"C:\Users\yassi\Downloads\planning_soutenances (1).docx"
profs_file = r"c:\Users\yassi\OneDrive\Desktop\rapport_tp2-3_jee\Liste des Profs.xlsx"

# 1. Read Profs file to get Specialite mapping
profs_df = pd.read_excel(profs_file, header=None)
profs_map = {}
for index, row in profs_df.iterrows():
    if index < 2: continue # skip headers
    nom = str(row[0]).strip().upper()
    specialite = str(row[3]).strip()
    profs_map[nom] = specialite

print("---- PLANNING EXTRACTION ----")
doc = docx.Document(docx_file)

for table in doc.tables:
    headers = [cell.text.strip() for cell in table.rows[0].cells]
    
    if "Encadrant" in headers and "Membre de jury 1" in headers:
        
        col_enc = headers.index("Encadrant")
        col_m1 = headers.index("Membre de jury 1")
        col_m2 = headers.index("Membre de jury 2")
        col_nom_etu = headers.index("Nom d'étudiant")
        col_prenom_etu = headers.index("Prénom d'étudiant")
        
        for i in range(1, min(len(table.rows), 15)): # Check first 15 for brevity
            row = table.rows[i]
            if len(row.cells) <= max(col_enc, col_m1, col_m2, col_nom_etu): continue
            
            etu_nom = row.cells[col_nom_etu].text.strip()
            etu_prenom = row.cells[col_prenom_etu].text.strip()
            
            enc = row.cells[col_enc].text.strip().split(" ")[0].upper()
            m1 = row.cells[col_m1].text.strip().split(" ")[0].upper()
            m2 = row.cells[col_m2].text.strip().split(" ")[0].upper()
            
            spec_enc = profs_map.get(enc, "N/A")
            spec_m1 = profs_map.get(m1, "N/A")
            spec_m2 = profs_map.get(m2, "N/A")
            
            print(f"Etudiant: {etu_nom} {etu_prenom}")
            print(f"  - Encadrant: {enc} ({spec_enc})")
            print(f"  - Rapporteur 1: {m1} ({spec_m1})")
            print(f"  - Rapporteur 2: {m2} ({spec_m2})")
            print("-" * 30)
        break
