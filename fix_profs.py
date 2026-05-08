import pandas as pd
import random

file = r"c:\Users\yassi\OneDrive\Desktop\rapport_tp2-3_jee\Liste des Profs.xlsx"
df = pd.read_excel(file)

# Let's recreate Discipline and Specialite based on the user rules
disciplines = ["Informatique"] * 15 + ["Anglais", "Anglais", "Français", "Math", "Gestion"]
specialites_info = [
    "Développement Web", "Génie Logiciel", "Intelligence Artificielle", 
    "Réseaux et Sécurité", "Data Science", "Bases de données", "Cloud Computing"
]

new_disciplines = []
new_specialites = []

# First row in the data seems to be "Nom", "Prénom", "Discpline" (a header row that was shifted?)
# Let's check df.iloc[0]
for index, row in df.iterrows():
    # Ignore if it's the header row that got shifted
    if str(row.iloc[0]) == "Nom" or pd.isna(row.iloc[0]):
        new_disciplines.append("Discipline")
        new_specialites.append("Spécialité")
        continue

    # Pick a random discipline
    d = random.choice(disciplines)
    new_disciplines.append(d)
    
    if d == "Informatique":
        new_specialites.append(random.choice(specialites_info))
    else:
        new_specialites.append(d)

df.iloc[:, 2] = new_disciplines
if len(df.columns) > 3:
    df.iloc[:, 3] = new_specialites
else:
    df['Spécialité'] = new_specialites

# Clean up column names if needed
df.columns = ['Nom', 'Prénom', 'Discipline', 'Spécialité']

df.to_excel(file, index=False)
print("Liste des Profs updated with Discipline and Specialité.")
