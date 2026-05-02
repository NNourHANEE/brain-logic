package com.reflexiongame.service;

import com.reflexiongame.model.Card;
import com.reflexiongame.model.Difficulty;
import com.reflexiongame.model.GameState;
import com.reflexiongame.model.GameSummary;
import com.reflexiongame.model.GameType;
import com.reflexiongame.model.Player;
import com.reflexiongame.repository.GameRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class GameService {
    private static final List<String[]> SYMBOLS = List.of(
            new String[]{"sun", "sun.svg"},
            new String[]{"moon", "moon.svg"},
            new String[]{"star", "star.svg"},
            new String[]{"cloud", "cloud.svg"},
            new String[]{"rainbow", "rainbow.svg"},
            new String[]{"lightning", "lightning.svg"},
            new String[]{"snowflake", "snowflake.svg"},
            new String[]{"flame", "flame.svg"}
    );

    public static final String[] PUZZLE_IMAGES = {
            "garden.svg", "sunset.svg", "cityscape.svg", "ocean.svg"
    };

    private final GameRepository repository;
    private final Random random = new Random();

    public GameService(GameRepository repository) {
        this.repository = repository;
    }

    public GameState createGame(String playerName, Difficulty difficulty, GameType gameType) {
        Player player = repository.findOrCreatePlayer(playerName);
        GameState game = new GameState();
        game.setPlayerId(player.getId());
        game.setPlayerName(player.getName());
        game.setDifficulty(difficulty);
        game.setGameType(gameType);
        game.setMoves(0);
        game.setMatchedPairs(0);
        game.setScore(0);
        game.setCompleted(false);
        game.setStartedAt(LocalDateTime.now());
        game.setUpdatedAt(LocalDateTime.now());
        switch (gameType) {
            case MEMORY -> game.setCards(createCards(difficulty));
            case SUDOKU -> initSudoku(game);
            case PICTURE_PUZZLE -> initPicturePuzzle(game);
            case NUMBER_RUSH -> initNumberRush(game);
            case TOWER_OF_HANOI -> initHanoi(game);
        }
        game.setId(repository.insertGame(game));
        return game;
    }

    public Optional<GameState> findGame(Long id) {
        return repository.findGame(id);
    }

    /**
     * Check the game's time budget. If exhausted and not yet finished, mark it as lost
     * (completed with score 0) and persist it. Returns true if the game just transitioned to lost,
     * false otherwise.
     */
    public boolean tickTimeLimit(GameState game) {
        if (game == null || game.isCompleted()) return false;
        if (game.getRemainingSeconds() > 0) return false;
        loseByTimeout(game);
        repository.updateGame(game);
        return true;
    }

    /** Same as {@link #tickTimeLimit(GameState)} but loads the game from the database first. */
    public Optional<GameState> tickTimeLimit(Long gameId) {
        Optional<GameState> opt = repository.findGame(gameId);
        opt.ifPresent(this::tickTimeLimit);
        return opt;
    }

    private void loseByTimeout(GameState game) {
        game.setCompleted(true);
        game.setCompletedAt(LocalDateTime.now());
        game.setScore(0);
        game.setMessage("Temps écoulé ! Partie perdue.");
        game.setOutcome("fail");
        game.setUpdatedAt(LocalDateTime.now());
    }

    // ---------------------- MEMORY ----------------------

    public GameState play(Long gameId, int position) {
        GameState game = repository.findGame(gameId).orElseThrow();
        if (tickTimeLimit(game)) return game;
        if (game.isCompleted() || game.getGameType() != GameType.MEMORY) return game;

        hidePreviousMismatch(game);
        Card selected = game.getCards().stream()
                .filter(card -> card.getPosition() == position)
                .findFirst()
                .orElseThrow();

        if (selected.isMatched() || selected.isRevealed()) {
            game.setMessage("Choisissez une autre carte.");
            game.setOutcome("info");
            return game;
        }

        selected.setRevealed(true);
        List<Card> openCards = openUnmatchedCards(game);
        if (openCards.size() == 1) {
            game.setMessage("Trouvez la carte correspondante.");
            game.setOutcome("info");
        } else if (openCards.size() == 2) {
            game.setMoves(game.getMoves() + 1);
            Card first = openCards.get(0);
            Card second = openCards.get(1);
            if (first.getSymbol().equals(second.getSymbol())) {
                first.setMatched(true);
                second.setMatched(true);
                game.setMatchedPairs(game.getMatchedPairs() + 1);
                game.setMessage("Bonne paire !");
                game.setOutcome("success");
            } else {
                game.setMessage("Pas identiques : mémorisez-les, puis continuez.");
                game.setOutcome("fail");
            }
        }

        if (game.getMatchedPairs() == game.getTotalPairs()) {
            finish(game);
        }

        game.setUpdatedAt(LocalDateTime.now());
        repository.updateGame(game);
        return game;
    }

    // ---------------------- SUDOKU ----------------------

    public GameState sudokuFill(Long gameId, int cell, int value) {
        GameState game = repository.findGame(gameId).orElseThrow();
        if (tickTimeLimit(game)) return game;
        if (game.isCompleted() || game.getGameType() != GameType.SUDOKU) return game;
        if (cell < 0 || cell >= 16) return game;
        if (game.getSudokuFixed()[cell]) {
            game.setMessage("Cette case est verrouillée.");
            game.setOutcome("info");
            return game;
        }
        if (value < 0 || value > 4) return game;

        game.setMoves(game.getMoves() + 1);
        game.getSudokuGrid()[cell] = value;
        if (value == 0) {
            game.setMessage("Case effacée.");
            game.setOutcome("info");
        } else if (value == game.getSudokuSolution()[cell]) {
            game.setMessage("Bonne valeur !");
            game.setOutcome("success");
        } else {
            game.setMessage("Mauvaise valeur, réessayez.");
            game.setOutcome("fail");
        }
        int correct = 0;
        for (int i = 0; i < 16; i++) {
            if (game.getSudokuGrid()[i] == game.getSudokuSolution()[i]) correct++;
        }
        game.setMatchedPairs(correct);
        if (correct == 16) finish(game);
        game.setUpdatedAt(LocalDateTime.now());
        repository.updateGame(game);
        return game;
    }

    // ---------------------- PICTURE PUZZLE ----------------------

    public GameState puzzleClick(Long gameId, int slot) {
        GameState game = repository.findGame(gameId).orElseThrow();
        if (tickTimeLimit(game)) return game;
        if (game.isCompleted() || game.getGameType() != GameType.PICTURE_PUZZLE) return game;
        int total = game.getPuzzleSize() * game.getPuzzleSize();
        if (slot < 0 || slot >= total) return game;

        int sel = game.getPuzzleSelected();
        int n = game.getPuzzleSize();
        if (sel == -1) {
            game.setPuzzleSelected(slot);
            game.setMessage("Sélectionnez une seconde pièce pour l'échanger.");
            game.setOutcome("info");
        } else if (sel == slot) {
            game.setPuzzleSelected(-1);
            game.setMessage("Sélection annulée.");
            game.setOutcome("info");
        } else if (game.isPuzzleSlideOnly() && !areAdjacent(sel, slot, n)) {
            game.setPuzzleSelected(-1);
            game.setMessage("Règle Maître : seules les pièces voisines (haut/bas/gauche/droite) peuvent s'échanger.");
            game.setOutcome("fail");
        } else {
            int[] order = game.getPuzzleOrder();
            int tmp = order[sel];
            order[sel] = order[slot];
            order[slot] = tmp;
            game.setPuzzleSelected(-1);
            game.setMoves(game.getMoves() + 1);
            int placed = 0;
            for (int i = 0; i < total; i++) if (order[i] == i) placed++;
            game.setMatchedPairs(placed);
            boolean justPlaced = order[slot] == slot || order[sel] == sel;
            game.setMessage(placed == total ? "Image reconstituée !" : "Pièces échangées.");
            game.setOutcome(placed == total ? "success" : (justPlaced ? "success" : "info"));
            if (placed == total) finish(game);
        }
        game.setUpdatedAt(LocalDateTime.now());
        repository.updateGame(game);
        return game;
    }

    // ---------------------- NUMBER RUSH (Schulte table) ----------------------

    public GameState numberClick(Long gameId, int cell) {
        GameState game = repository.findGame(gameId).orElseThrow();
        if (tickTimeLimit(game)) return game;
        if (game.isCompleted() || game.getGameType() != GameType.NUMBER_RUSH) return game;
        int n = game.getNumbersSize();
        int total = n * n;
        if (cell < 0 || cell >= total) return game;
        int[] grid = game.getNumbersGrid();
        int value = grid[cell];
        if (value == 0) {
            game.setMessage("Cette case est déjà validée.");
            game.setOutcome("info");
        } else if (value == game.getNumbersNext()) {
            grid[cell] = 0;
            game.setNumbersNext(game.isNumbersDescending()
                    ? game.getNumbersNext() - 1
                    : game.getNumbersNext() + 1);
            game.setMoves(game.getMoves() + 1);
            int placed = 0;
            for (int v : grid) if (v == 0) placed++;
            game.setMatchedPairs(placed);
            if (placed == total) {
                game.setMessage("Bravo, grille terminée !");
                game.setOutcome("success");
                finish(game);
            } else {
                game.setMessage("Bien ! Cherchez le " + game.getNumbersNext() + ".");
                game.setOutcome("success");
            }
        } else {
            game.setMoves(game.getMoves() + 1);
            game.setMessage("Mauvais nombre. Cherchez le " + game.getNumbersNext() + ".");
            game.setOutcome("fail");
            if (game.isNumbersShuffleOnMiss()) {
                shuffleRemainingNumbers(grid);
                game.setMessage("Pénalité : la grille est mélangée. Cherchez le " + game.getNumbersNext() + ".");
            }
        }
        game.setUpdatedAt(LocalDateTime.now());
        repository.updateGame(game);
        return game;
    }

    private void shuffleRemainingNumbers(int[] grid) {
        List<Integer> idx = new ArrayList<>();
        List<Integer> vals = new ArrayList<>();
        for (int i = 0; i < grid.length; i++) {
            if (grid[i] != 0) { idx.add(i); vals.add(grid[i]); }
        }
        Collections.shuffle(vals, random);
        for (int i = 0; i < idx.size(); i++) grid[idx.get(i)] = vals.get(i);
    }

    // ---------------------- TOWER OF HANOI ----------------------

    public GameState hanoiClick(Long gameId, int tower) {
        GameState game = repository.findGame(gameId).orElseThrow();
        if (tickTimeLimit(game)) return game;
        if (game.isCompleted() || game.getGameType() != GameType.TOWER_OF_HANOI) return game;
        if (tower < 0 || tower > 2) return game;

        int sel = game.getHanoiSelected();
        List<List<Integer>> towers = game.getHanoiTowers();
        if (sel == -1) {
            if (towers.get(tower).isEmpty()) {
                game.setMessage("Tour vide : choisissez une tour avec un disque.");
                game.setOutcome("info");
            } else {
                game.setHanoiSelected(tower);
                game.setMessage("Tour sélectionnée. Choisissez la destination.");
                game.setOutcome("info");
            }
        } else if (sel == tower) {
            game.setHanoiSelected(-1);
            game.setMessage("Sélection annulée.");
            game.setOutcome("info");
        } else {
            List<Integer> from = towers.get(sel);
            List<Integer> to = towers.get(tower);
            int disk = from.get(from.size() - 1);
            if (game.isHanoiAdjacentOnly() && Math.abs(sel - tower) != 1) {
                game.setMessage("Règle Maître : déplacement uniquement vers une tour voisine.");
                game.setOutcome("fail");
            } else if (!to.isEmpty() && to.get(to.size() - 1) < disk) {
                game.setMessage("Mouvement interdit : disque plus grand sur plus petit.");
                game.setOutcome("fail");
            } else {
                from.remove(from.size() - 1);
                to.add(disk);
                game.setMoves(game.getMoves() + 1);
                game.setMessage("Disque déplacé.");
                game.setOutcome(tower == 2 ? "success" : "info");
            }
            game.setHanoiSelected(-1);
            int onTarget = towers.get(2).size();
            game.setMatchedPairs(onTarget);
            if (onTarget == game.getHanoiDisks() && isSorted(towers.get(2))) {
                game.setOutcome("success");
                finish(game);
            }
        }
        game.setUpdatedAt(LocalDateTime.now());
        repository.updateGame(game);
        return game;
    }

    private boolean isSorted(List<Integer> tower) {
        for (int i = 1; i < tower.size(); i++) {
            if (tower.get(i) >= tower.get(i - 1)) return false;
        }
        return true;
    }

    // ---------------------- shared helpers ----------------------

    public List<GameSummary> savedGames() { return repository.findSavedGames(); }
    public List<GameSummary> scoreBoard() { return repository.findScoreBoard(); }

    public int stars(GameState game) {
        int target = game.getTotalPairs();
        if (target <= 0) return 1;
        double ratio = (double) game.getMoves() / target;
        double threshold3 = switch (game.getGameType()) {
            case MEMORY -> 1.5;
            case SUDOKU -> 1.2;
            case PICTURE_PUZZLE -> 1.5;
            case NUMBER_RUSH -> 1.6;
            case TOWER_OF_HANOI -> 2.0;
        };
        if (ratio <= threshold3) return 3;
        if (ratio <= threshold3 * 1.7) return 2;
        return 1;
    }

    private void finish(GameState game) {
        game.setCompleted(true);
        game.setCompletedAt(LocalDateTime.now());
        game.setScore(calculateScore(game));
        repository.updatePlayerProgress(game.getPlayerId(), game.getScore(), progressionFor(game.getDifficulty()));
        if (game.getMessage() == null || game.getMessage().isBlank()) {
            game.setMessage("Partie terminée !");
        }
    }

    private List<Card> createCards(Difficulty difficulty) {
        List<String[]> selectedSymbols = new ArrayList<>(SYMBOLS.subList(0, difficulty.getPairs()));
        List<Card> cards = new ArrayList<>();
        int position = 0;
        for (String[] symbol : selectedSymbols) {
            cards.add(new Card(position++, symbol[0], symbol[1], false, false));
            cards.add(new Card(position++, symbol[0], symbol[1], false, false));
        }
        Collections.shuffle(cards);
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            cards.set(i, new Card(i, card.getSymbol(), card.getImage(), false, false));
        }
        return cards;
    }

    private void hidePreviousMismatch(GameState game) {
        List<Card> open = openUnmatchedCards(game);
        if (open.size() >= 2) {
            for (Card card : open) card.setRevealed(false);
        }
    }

    private List<Card> openUnmatchedCards(GameState game) {
        return game.getCards().stream()
                .filter(card -> card.isRevealed() && !card.isMatched())
                .toList();
    }

    private int calculateScore(GameState game) {
        long elapsed = Math.max(1, game.getElapsedSeconds());
        int base = game.getTotalPairs() * game.getDifficulty().getMultiplier();
        int movePenalty = game.getMoves() * 8;
        int timePenalty = (int) Math.min(300, elapsed * 2);
        return Math.max(50, base + 600 - movePenalty - timePenalty);
    }

    private int progressionFor(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 1;
            case MEDIUM -> 2;
            case HARD -> 3;
            case EXPERT -> 4;
            case MASTER -> 5;
        };
    }

    private boolean areAdjacent(int a, int b, int n) {
        int ra = a / n, ca = a % n;
        int rb = b / n, cb = b % n;
        return (ra == rb && Math.abs(ca - cb) == 1) || (ca == cb && Math.abs(ra - rb) == 1);
    }

    // ---------------------- initializers ----------------------

    private void initSudoku(GameState game) {
        int[] solution = generateSudoku4(game.isXSudoku());
        boolean[] fixed = new boolean[16];
        Arrays.fill(fixed, true);
        // Hide N cells.
        int hide = game.getDifficulty().getSudokuHidden();
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < 16; i++) idx.add(i);
        Collections.shuffle(idx, random);
        int[] grid = solution.clone();
        for (int i = 0; i < hide; i++) {
            int c = idx.get(i);
            grid[c] = 0;
            fixed[c] = false;
        }
        game.setSudokuSolution(solution);
        game.setSudokuGrid(grid);
        game.setSudokuFixed(fixed);
        // initial progress = correctly placed cells (i.e. clues, since empty != solution)
        int correct = 0;
        for (int i = 0; i < 16; i++) if (grid[i] == solution[i]) correct++;
        game.setMatchedPairs(correct);
    }

    private int[] generateSudoku4(boolean xSudoku) {
        int[] grid = new int[16];
        if (!fillSudoku(grid, 0, xSudoku)) {
            int[] base = {1,2,3,4, 3,4,1,2, 2,1,4,3, 4,3,2,1};
            return base;
        }
        return grid;
    }

    private boolean fillSudoku(int[] g, int pos, boolean xSudoku) {
        if (pos == 16) return true;
        int r = pos / 4, c = pos % 4;
        List<Integer> values = new ArrayList<>(List.of(1, 2, 3, 4));
        Collections.shuffle(values, random);
        for (int v : values) {
            if (sudokuFits(g, r, c, v, xSudoku)) {
                g[pos] = v;
                if (fillSudoku(g, pos + 1, xSudoku)) return true;
                g[pos] = 0;
            }
        }
        return false;
    }

    private boolean sudokuFits(int[] g, int r, int c, int v, boolean xSudoku) {
        for (int i = 0; i < 4; i++) {
            if (g[r * 4 + i] == v) return false;
            if (g[i * 4 + c] == v) return false;
        }
        int br = (r / 2) * 2, bc = (c / 2) * 2;
        for (int i = 0; i < 2; i++)
            for (int j = 0; j < 2; j++)
                if (g[(br + i) * 4 + (bc + j)] == v) return false;
        if (xSudoku) {
            if (r == c) {
                for (int i = 0; i < 4; i++) if (i != r && g[i * 4 + i] == v) return false;
            }
            if (r + c == 3) {
                for (int i = 0; i < 4; i++) {
                    int j = 3 - i;
                    if (i != r && g[i * 4 + j] == v) return false;
                }
            }
        }
        return true;
    }

    private void initPicturePuzzle(GameState game) {
        int size = game.getDifficulty().getGridSize();
        int total = size * size;
        game.setPuzzleSize(size);
        game.setPuzzleImage(PUZZLE_IMAGES[random.nextInt(PUZZLE_IMAGES.length)]);
        int[] order = new int[total];
        for (int i = 0; i < total; i++) order[i] = i;
        // Shuffle, then ensure not already solved.
        do {
            shuffleInts(order);
        } while (isPuzzleSolved(order));
        game.setPuzzleOrder(order);
        game.setPuzzleSelected(-1);
        int placed = 0;
        for (int i = 0; i < total; i++) if (order[i] == i) placed++;
        game.setMatchedPairs(placed);
    }

    private boolean isPuzzleSolved(int[] order) {
        for (int i = 0; i < order.length; i++) if (order[i] != i) return false;
        return true;
    }

    private void shuffleInts(int[] a) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int tmp = a[i]; a[i] = a[j]; a[j] = tmp;
        }
    }

    private void initNumberRush(GameState game) {
        int n = game.getDifficulty().getGridSize();
        int total = n * n;
        int[] grid = new int[total];
        List<Integer> values = new ArrayList<>(total);
        for (int i = 1; i <= total; i++) values.add(i);
        Collections.shuffle(values, random);
        for (int i = 0; i < total; i++) grid[i] = values.get(i);
        game.setNumbersSize(n);
        game.setNumbersGrid(grid);
        game.setNumbersNext(game.isNumbersDescending() ? total : 1);
        game.setMatchedPairs(0);
    }

    private void initHanoi(GameState game) {
        int disks = game.getDifficulty().getDisks();
        game.setHanoiDisks(disks);
        List<List<Integer>> towers = new ArrayList<>();
        List<Integer> a = new ArrayList<>();
        for (int i = disks; i >= 1; i--) a.add(i);
        towers.add(a);
        towers.add(new ArrayList<>());
        towers.add(new ArrayList<>());
        game.setHanoiTowers(towers);
        game.setHanoiSelected(-1);
        game.setMatchedPairs(0);
    }
}
