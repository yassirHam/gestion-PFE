import pandas as pd
import random

file = r"c:\Users\yassi\OneDrive\Desktop\rapport_tp2-3_jee\Liste des Profs.xlsx"
df = pd.read_excel(file, header=None)

specialites_info = [
    "Développement Web", "Génie Logiciel", "Intelligence Artificielle", 
    "Réseaux et Sécurité", "Data Science", "Bases de données", "Cloud Computing"
]

new_specialites = []

for index, row in df.iterrows():
    if index == 0:
        new_specialites.append("Spécialité")
        continue
    if index == 1:
        new_specialites.append("Spécialité")
        continue
    
    disc = str(row.iloc[2]).strip()
    
    if "info" in disc.lower():
        new_specialites.append(random.choice(specialites_info))
    elif pd.isna(row.iloc[2]) or disc.lower() == "nan":
        new_specialites.append("")
    else:
        new_specialites.append(disc)

# Create a new column
df[3] = new_specialites

df.to_excel(file, index=False, header=False)
print("Successfully added Spécialité to Liste des Profs.xlsx without modifying other columns.")
