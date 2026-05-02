# Rapport de Projet — Jeu de Réflexion Web JEE
**Module :** Architecture Web JEE — M1 ISII 2025/2026

---

## 1. Description du jeu

### Présentation générale

**Brain Logic** est une application web de jeux de réflexion mono-joueur. Elle propose cinq jeux distincts, chacun mobilisant une compétence cognitive différente : mémoire, logique, perception spatiale, rapidité et résolution de problèmes. Le joueur choisit son jeu et son niveau de difficulté avant de commencer, et peut sauvegarder sa partie à tout moment pour la reprendre plus tard.

---

### Les cinq jeux disponibles

#### 1.1 Mémoire (Memory)
**Objectif :** Retrouver toutes les paires de cartes identiques.

**Règles :**
- Un plateau de cartes retournées face cachée est affiché.
- Le joueur retourne deux cartes à la fois.
- Si les deux cartes affichent le même symbole, elles restent visibles (paire trouvée).
- Sinon, elles se retournent face cachée après un bref délai.
- La partie est gagnée quand toutes les paires sont trouvées avant l'expiration du temps.

**Éléments visuels :** Chaque carte affiche une icône SVG (soleil, lune, étoile, nuage, arc-en-ciel, éclair, flocon, flamme).

---

#### 1.2 Sudoku 4×4
**Objectif :** Remplir une grille 4×4 avec les chiffres 1 à 4, sans répétition sur chaque ligne, colonne et bloc 2×2.

**Règles :**
- Certaines cases sont pré-remplies (indices fixes, en bleu).
- Le joueur clique sur une case vide, puis choisit un chiffre (1–4).
- Une case incorrecte est signalée en rouge.
- Aux niveaux Expert et Maître, les deux diagonales principales doivent également contenir les chiffres 1 à 4 (variante X-Sudoku).

---

#### 1.3 Puzzle Photo
**Objectif :** Reconstituer une photographie découpée en pièces mélangées.

**Règles :**
- Une image est découpée en grille (3×3, 4×4 ou 5×5 selon le niveau).
- Le joueur clique sur une pièce pour la sélectionner, puis clique sur une autre pour les échanger.
- Au niveau Maître, seuls les échanges entre pièces adjacentes (horizontalement ou verticalement) sont autorisés.
- La partie est gagnée quand toutes les pièces sont à leur position d'origine.

**Images disponibles :** jardin, coucher de soleil, paysage urbain, océan (images SVG).

---

#### 1.4 Grille des Nombres (Number Rush)
**Objectif :** Cliquer sur les nombres d'une grille dans l'ordre croissant (ou décroissant au niveau Maître), le plus rapidement possible.

**Règles :**
- Une grille mélangée de nombres (de 1 à N²) est affichée.
- Le joueur doit taper les nombres dans l'ordre attendu.
- Aux niveaux Expert et Maître, un mauvais clic redistribue aléatoirement tous les nombres restants.
- Au niveau Maître, l'ordre est décroissant (de N² à 1).

---

#### 1.5 Tours de Hanoï
**Objectif :** Déplacer tous les disques de la tour de gauche vers la tour de droite, en respectant les règles du jeu classique.

**Règles :**
- On ne peut déplacer qu'un disque à la fois.
- On ne peut jamais poser un disque plus grand sur un disque plus petit.
- Le joueur clique sur une tour pour sélectionner le disque du dessus, puis clique sur la tour de destination.
- Au niveau Maître, on ne peut déplacer un disque que vers une tour adjacente (gauche ↔ centre ↔ droite).

---

### Niveaux de difficulté

Cinq niveaux progressifs sont disponibles pour tous les jeux :

| Niveau | Paires Mémoire | Grille Sudoku/Puzzle/Nombres | Disques Hanoï | Budget temps (Mémoire) |
|--------|---------------|------------------------------|----------------|------------------------|
| Facile | 4 paires | 3×3 | 3 disques | 3 min |
| Moyen | 6 paires | 4×4 | 4 disques | 4 min |
| Difficile | 8 paires | 5×5 | 5 disques | 5 min |
| Expert | 10 paires | 6×6 | 5 disques | 6 min + règles spéciales |
| Maître | 12 paires | 6×6 | 6 disques | 8 min + règles spéciales |

Chaque jeu a son propre multiplicateur de temps appliqué au budget de base.

---

### Système de score

- Le score est calculé à la fin de chaque partie gagnée.
- Il est basé sur le nombre de mouvements effectués, le multiplicateur du niveau de difficulté, et le multiplicateur propre à chaque type de jeu.
- Une partie perdue par dépassement du temps donne un score de 0.
- Un classement général (`/scores`) affiche les 20 meilleures performances tous jeux confondus.
- Le profil joueur conserve le meilleur score et la progression maximale atteints.

---

### Gestion des parties

- **Sauvegarder :** À tout moment, le joueur peut sauvegarder sa partie en cours (bouton "Sauvegarder"). La partie est conservée en base de données avec `completed = false`.
- **Reprendre :** La page "Reprendre" (`/saved`) liste toutes les parties sauvegardées non terminées. Le joueur peut reprendre n'importe laquelle en cliquant dessus.
- **Limite de temps :** Chaque partie a un budget de temps. Un mécanisme de tick côté serveur (POST `/game/{id}/tick` déclenché par le navigateur) vérifie l'expiration et marque la partie comme perdue le cas échéant.

---

## 2. Architecture de l'application

### 2.1 Vue d'ensemble

L'application suit une architecture **MVC (Modèle-Vue-Contrôleur)** en trois couches, déployée sur un serveur Tomcat embarqué démarré manuellement depuis le `main()` Java, sans aucune dépendance à Spring Boot.

```
Navigateur
    │  HTTP (GET / POST)
    ▼
DispatcherServlet (Spring MVC)
    │
    ├── GameController        ← Couche Contrôleur
    │       │
    │       ├── GameService   ← Couche Service (logique métier)
    │       │       │
    │       │       └── GameRepository  ← Couche Accès aux données (JDBC)
    │       │               │
    │       │               └── PostgreSQL (Base de données)
    │       │
    │       └── Thymeleaf (templates HTML)  ← Couche Vue
    │
AppConfig (@Configuration)   ← Configuration Spring manuelle
```

---

### 2.2 Technologies utilisées

| Composant | Technologie | Version |
|-----------|-------------|---------|
| Langage | Java | 17 |
| Framework web | Spring MVC | 6.1.14 |
| Moteur de templates | Thymeleaf (spring6) | 3.1.2 |
| Serveur HTTP | Apache Tomcat embarqué | 10.1.25 |
| Accès aux données | Spring JDBC / JdbcTemplate | 6.1.14 |
| Pool de connexions | HikariCP | 5.1.0 |
| Base de données | PostgreSQL | — |
| Build | Apache Maven | 3.8.6 |
| CI/CD | GitHub Actions | — |

---

### 2.3 Description des principaux composants

#### `Application.java` — Point d'entrée
Démarre manuellement un serveur Tomcat embarqué. Crée un `AnnotationConfigWebApplicationContext` pointant vers `AppConfig`, instancie le `DispatcherServlet` Spring MVC, l'enregistre sur le chemin `/*`, puis appelle `tomcat.start()`. Le port d'écoute est lu depuis la variable d'environnement `PORT` (défaut : 8080).

```
main()
  └─ new Tomcat()
       └─ addContext("")
            └─ addServlet("dispatcher", new DispatcherServlet(springContext))
                 └─ tomcat.start()
```

---

#### `AppConfig.java` — Configuration Spring
Classe annotée `@Configuration @EnableWebMvc @ComponentScan`. Déclare manuellement tous les beans qui seraient normalement auto-configurés par Spring Boot :

| Bean | Rôle |
|------|------|
| `DataSource` (HikariCP) | Pool de connexions vers PostgreSQL, paramétré via les variables d'environnement `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` |
| `JdbcTemplate` | Utilitaire Spring pour exécuter des requêtes SQL sans boilerplate |
| `ClassLoaderTemplateResolver` | Localise les templates Thymeleaf dans `classpath:/templates/` |
| `SpringTemplateEngine` | Moteur Thymeleaf intégré à Spring |
| `ThymeleafViewResolver` | Résout les noms de vues retournés par les contrôleurs vers les fichiers `.html` |
| `addResourceHandlers` | Sert les fichiers statiques (`/css/**`, `/js/**`, `/images/**`) depuis `classpath:/static/` |

---

#### `GameController.java` — Contrôleur HTTP
Unique contrôleur Spring MVC (`@Controller`). Gère toutes les routes de l'application :

| Méthode HTTP | Route | Action |
|-------------|-------|--------|
| GET | `/` | Page d'accueil + classement |
| POST | `/start` | Créer une nouvelle partie |
| GET | `/game/{id}` | Afficher l'état d'une partie |
| POST | `/game/{id}/select` | Action Mémoire (retourner une carte) |
| POST | `/game/{id}/sudoku` | Action Sudoku (remplir une case) |
| POST | `/game/{id}/puzzle` | Action Puzzle (sélectionner/échanger une pièce) |
| POST | `/game/{id}/number` | Action Grille des nombres (cliquer un nombre) |
| POST | `/game/{id}/hanoi` | Action Hanoï (sélectionner/déplacer un disque) |
| POST | `/game/{id}/tick` | Vérification du temps (keep-alive navigateur) |
| POST | `/game/{id}/save` | Sauvegarder la partie |
| GET | `/saved` | Lister les parties sauvegardées |
| GET | `/scores` | Classement général |

Toutes les actions POST suivent le pattern **PRG (Post-Redirect-Get)** pour éviter les doubles soumissions.

---

#### `GameService.java` — Logique métier
Contient l'intégralité des règles de jeu pour les cinq types de jeux. Responsabilités principales :
- Initialisation des états de jeu (génération aléatoire des cartes, de la grille Sudoku, du puzzle, etc.)
- Traitement des actions joueur (vérification des paires, validation du Sudoku, déplacement des disques Hanoï, etc.)
- Calcul du score en fin de partie
- Vérification et application de la limite de temps
- Application des règles spéciales des niveaux Expert et Maître

---

#### `GameRepository.java` — Accès aux données
Utilise `JdbcTemplate` pour toutes les opérations SQL. L'état de chaque jeu est sérialisé en une chaîne compacte stockée dans la colonne `cards_state` de la table `games` :

| Préfixe | Format |
|---------|--------|
| `M:` | Mémoire : liste des cartes encodées |
| `S:` | Sudoku : solution, grille actuelle, cases fixes |
| `P:` | Puzzle : nom de l'image, taille, ordre des pièces |
| `N:` | Grille des nombres : taille, grille, prochain attendu |
| `H:` | Hanoï : nombre de disques, état des 3 tours |

---

#### `DatabaseInitializer.java` — Initialisation de la base
Composant Spring (`@Component`) exécuté au démarrage (`@PostConstruct`). Crée les tables `players` et `games` si elles n'existent pas encore, et applique les migrations de schéma nécessaires.

---

### 2.4 Schéma de la base de données

```sql
-- Joueurs
CREATE TABLE players (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(80) NOT NULL UNIQUE,
    best_score  INT NOT NULL DEFAULT 0,
    progression INT NOT NULL DEFAULT 0
);

-- Parties
CREATE TABLE games (
    id            BIGSERIAL PRIMARY KEY,
    player_id     BIGINT NOT NULL REFERENCES players(id),
    game_type     VARCHAR(20) NOT NULL,   -- MEMORY | SUDOKU | PICTURE_PUZZLE | NUMBER_RUSH | TOWER_OF_HANOI
    difficulty    VARCHAR(20) NOT NULL,   -- EASY | MEDIUM | HARD | EXPERT | MASTER
    cards_state   TEXT NOT NULL,          -- état sérialisé du jeu
    moves         INT NOT NULL DEFAULT 0,
    matched_pairs INT NOT NULL DEFAULT 0,
    score         INT NOT NULL DEFAULT 0,
    completed     BOOLEAN NOT NULL DEFAULT FALSE,
    started_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    completed_at  TIMESTAMP
);
```

---

### 2.5 Structure du projet

```
mobile/
├── pom.xml                          ← Dépendances Maven (sans Spring Boot)
└── src/main/
    ├── java/com/reflexiongame/
    │   ├── Application.java         ← main() : démarrage Tomcat embarqué
    │   ├── AppConfig.java           ← Configuration Spring MVC manuelle
    │   ├── controller/
    │   │   └── GameController.java  ← Routes HTTP
    │   ├── service/
    │   │   └── GameService.java     ← Logique métier des 5 jeux
    │   ├── repository/
    │   │   ├── GameRepository.java  ← Accès PostgreSQL via JdbcTemplate
    │   │   └── DatabaseInitializer.java ← Création des tables au démarrage
    │   └── model/
    │       ├── GameState.java       ← État complet d'une partie
    │       ├── GameType.java        ← Enum des 5 types de jeux
    │       ├── Difficulty.java      ← Enum des 5 niveaux
    │       ├── Card.java            ← Carte Mémoire
    │       ├── Player.java          ← Entité joueur
    │       └── GameSummary.java     ← Résumé pour les listes
    └── resources/
        ├── templates/               ← Vues Thymeleaf (.html)
        │   ├── index.html           ← Accueil
        │   ├── game.html            ← Mémoire
        │   ├── sudoku.html          ← Sudoku
        │   ├── picture.html         ← Puzzle photo
        │   ├── numbers.html         ← Grille des nombres
        │   ├── hanoi.html           ← Tours de Hanoï
        │   ├── result.html          ← Résultat de partie
        │   ├── saved.html           ← Parties sauvegardées
        │   ├── scores.html          ← Classement
        │   └── fragments.html       ← Fragments Thymeleaf réutilisables
        └── static/
            ├── css/style.css        ← Styles de l'interface
            ├── js/effects.js        ← Effets visuels côté client
            └── images/              ← Icônes SVG et photos des puzzles
```

---

### 2.6 Démarrage de l'application

```bash
# Compilation et packaging
cd mobile
mvn package -DskipTests

# Lancement
PORT=8080 java -jar target/jeu-reflexion-jee-1.0.0.jar
```

Variables d'environnement utilisées : `PORT`, `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`.
