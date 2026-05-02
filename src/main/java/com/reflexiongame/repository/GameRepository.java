package com.reflexiongame.repository;

import com.reflexiongame.model.Card;
import com.reflexiongame.model.Difficulty;
import com.reflexiongame.model.GameState;
import com.reflexiongame.model.GameSummary;
import com.reflexiongame.model.GameType;
import com.reflexiongame.model.Player;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class GameRepository {
    private final JdbcTemplate jdbcTemplate;

    public GameRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Player findOrCreatePlayer(String rawName) {
        String name = rawName == null || rawName.trim().isBlank() ? "Joueur" : rawName.trim();
        Optional<Player> existing = findPlayerByName(name);
        if (existing.isPresent()) return existing.get();

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO players(name, best_score, progression) VALUES (?, 0, 0)",
                    new String[]{"id"}
            );
            ps.setString(1, name);
            return ps;
        }, keyHolder);

        Player player = new Player();
        player.setId(keyHolder.getKey().longValue());
        player.setName(name);
        return player;
    }

    public Optional<Player> findPlayerByName(String name) {
        List<Player> players = jdbcTemplate.query(
                "SELECT id, name, best_score, progression FROM players WHERE LOWER(name) = LOWER(?)",
                playerMapper(), name
        );
        return players.stream().findFirst();
    }

    public Long insertGame(GameState game) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    """
                    INSERT INTO games(player_id, game_type, difficulty, cards_state, moves, matched_pairs, score, completed, started_at, updated_at, completed_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """,
                    new String[]{"id"}
            );
            ps.setLong(1, game.getPlayerId());
            ps.setString(2, game.getGameType().name());
            ps.setString(3, game.getDifficulty().name());
            ps.setString(4, encodeState(game));
            ps.setInt(5, game.getMoves());
            ps.setInt(6, game.getMatchedPairs());
            ps.setInt(7, game.getScore());
            ps.setBoolean(8, game.isCompleted());
            ps.setTimestamp(9, Timestamp.valueOf(game.getStartedAt()));
            ps.setTimestamp(10, Timestamp.valueOf(game.getUpdatedAt()));
            ps.setTimestamp(11, game.getCompletedAt() == null ? null : Timestamp.valueOf(game.getCompletedAt()));
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    public void updateGame(GameState game) {
        jdbcTemplate.update(
                """
                UPDATE games SET game_type = ?, cards_state = ?, moves = ?, matched_pairs = ?, score = ?, completed = ?, updated_at = ?, completed_at = ?
                WHERE id = ?
                """,
                game.getGameType().name(), encodeState(game), game.getMoves(), game.getMatchedPairs(), game.getScore(), game.isCompleted(),
                Timestamp.valueOf(game.getUpdatedAt()), game.getCompletedAt() == null ? null : Timestamp.valueOf(game.getCompletedAt()),
                game.getId()
        );
    }

    public Optional<GameState> findGame(Long id) {
        List<GameState> games = jdbcTemplate.query(
                """
                SELECT g.*, p.name AS player_name FROM games g
                JOIN players p ON p.id = g.player_id
                WHERE g.id = ?
                """,
                gameMapper(), id
        );
        return games.stream().findFirst();
    }

    public List<GameSummary> findSavedGames() {
        return jdbcTemplate.query(
                """
                SELECT g.id, p.name AS player_name, g.game_type, g.difficulty, g.moves, g.matched_pairs, g.score, g.completed, g.updated_at
                FROM games g JOIN players p ON p.id = g.player_id
                WHERE g.completed = FALSE
                ORDER BY g.updated_at DESC
                """,
                summaryMapper()
        );
    }

    public List<GameSummary> findScoreBoard() {
        return jdbcTemplate.query(
                """
                SELECT g.id, p.name AS player_name, g.game_type, g.difficulty, g.moves, g.matched_pairs, g.score, g.completed, g.updated_at
                FROM games g JOIN players p ON p.id = g.player_id
                WHERE g.completed = TRUE AND g.score > 0
                ORDER BY g.score DESC, g.moves ASC
                LIMIT 20
                """,
                summaryMapper()
        );
    }

    public void updatePlayerProgress(Long playerId, int score, int progression) {
        jdbcTemplate.update(
                "UPDATE players SET best_score = GREATEST(best_score, ?), progression = GREATEST(progression, ?) WHERE id = ?",
                score, progression, playerId
        );
    }

    private RowMapper<Player> playerMapper() {
        return (rs, rowNum) -> {
            Player player = new Player();
            player.setId(rs.getLong("id"));
            player.setName(rs.getString("name"));
            player.setBestScore(rs.getInt("best_score"));
            player.setProgression(rs.getInt("progression"));
            return player;
        };
    }

    private RowMapper<GameState> gameMapper() {
        return (rs, rowNum) -> {
            GameState game = new GameState();
            game.setId(rs.getLong("id"));
            game.setPlayerId(rs.getLong("player_id"));
            game.setPlayerName(rs.getString("player_name"));
            game.setGameType(GameType.valueOf(rs.getString("game_type")));
            game.setDifficulty(Difficulty.valueOf(rs.getString("difficulty")));
            decodeState(game, rs.getString("cards_state"));
            game.setMoves(rs.getInt("moves"));
            game.setMatchedPairs(rs.getInt("matched_pairs"));
            game.setScore(rs.getInt("score"));
            game.setCompleted(rs.getBoolean("completed"));
            game.setStartedAt(rs.getTimestamp("started_at").toLocalDateTime());
            game.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            Timestamp completedAt = rs.getTimestamp("completed_at");
            game.setCompletedAt(completedAt == null ? null : completedAt.toLocalDateTime());
            return game;
        };
    }

    private RowMapper<GameSummary> summaryMapper() {
        return (rs, rowNum) -> {
            GameSummary summary = new GameSummary();
            Difficulty difficulty = Difficulty.valueOf(rs.getString("difficulty"));
            GameType gameType = GameType.valueOf(rs.getString("game_type"));
            summary.setId(rs.getLong("id"));
            summary.setPlayerName(rs.getString("player_name"));
            summary.setDifficulty(difficulty);
            summary.setGameType(gameType);
            summary.setMoves(rs.getInt("moves"));
            summary.setMatchedPairs(rs.getInt("matched_pairs"));
            summary.setTotalPairs(gameType.getTarget(difficulty));
            summary.setScore(rs.getInt("score"));
            summary.setCompleted(rs.getBoolean("completed"));
            summary.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
            return summary;
        };
    }

    // ----- state encoding -----

    private String encodeState(GameState g) {
        return switch (g.getGameType()) {
            case MEMORY -> "M:" + encodeCards(g.getCards());
            case SUDOKU -> "S:" + intsToStr(g.getSudokuSolution()) + "|" + intsToStr(g.getSudokuGrid())
                    + "|" + boolsToStr(g.getSudokuFixed());
            case PICTURE_PUZZLE -> "P:" + g.getPuzzleImage() + "|" + g.getPuzzleSize() + "|"
                    + intArrayToCsv(g.getPuzzleOrder()) + "|" + g.getPuzzleSelected();
            case NUMBER_RUSH -> "N:" + g.getNumbersSize() + "|" + intArrayToCsv(g.getNumbersGrid()) + "|" + g.getNumbersNext();
            case TOWER_OF_HANOI -> "H:" + g.getHanoiDisks() + "|" + encodeTowers(g.getHanoiTowers())
                    + "|" + g.getHanoiSelected();
        };
    }

    private void decodeState(GameState g, String value) {
        if (value == null || value.length() < 2) return;
        String body = value.substring(2);
        switch (g.getGameType()) {
            case MEMORY -> g.setCards(decodeCards(body));
            case SUDOKU -> {
                String[] p = body.split("\\|", -1);
                g.setSudokuSolution(strToInts(p[0]));
                g.setSudokuGrid(strToInts(p[1]));
                g.setSudokuFixed(strToBools(p[2]));
            }
            case PICTURE_PUZZLE -> {
                String[] p = body.split("\\|", -1);
                g.setPuzzleImage(p[0]);
                g.setPuzzleSize(Integer.parseInt(p[1]));
                g.setPuzzleOrder(csvToIntArray(p[2]));
                g.setPuzzleSelected(Integer.parseInt(p[3]));
            }
            case NUMBER_RUSH -> {
                String[] p = body.split("\\|", -1);
                g.setNumbersSize(Integer.parseInt(p[0]));
                g.setNumbersGrid(csvToIntArray(p[1]));
                g.setNumbersNext(Integer.parseInt(p[2]));
            }
            case TOWER_OF_HANOI -> {
                String[] p = body.split("\\|", -1);
                g.setHanoiDisks(Integer.parseInt(p[0]));
                g.setHanoiTowers(decodeTowers(p[1]));
                g.setHanoiSelected(Integer.parseInt(p[2]));
            }
        }
    }

    private String intsToStr(int[] arr) {
        if (arr == null) return "";
        StringBuilder sb = new StringBuilder(arr.length);
        for (int v : arr) sb.append(v);
        return sb.toString();
    }

    private int[] strToInts(String s) {
        if (s == null || s.isEmpty()) return new int[0];
        int[] r = new int[s.length()];
        for (int i = 0; i < s.length(); i++) r[i] = s.charAt(i) - '0';
        return r;
    }

    private String boolsToStr(boolean[] arr) {
        if (arr == null) return "";
        StringBuilder sb = new StringBuilder(arr.length);
        for (boolean b : arr) sb.append(b ? '1' : '0');
        return sb.toString();
    }

    private boolean[] strToBools(String s) {
        if (s == null || s.isEmpty()) return new boolean[0];
        boolean[] r = new boolean[s.length()];
        for (int i = 0; i < s.length(); i++) r[i] = s.charAt(i) == '1';
        return r;
    }

    private String intArrayToCsv(int[] arr) {
        if (arr == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(arr[i]);
        }
        return sb.toString();
    }

    private int[] csvToIntArray(String s) {
        if (s == null || s.isEmpty()) return new int[0];
        String[] parts = s.split(",");
        int[] r = new int[parts.length];
        for (int i = 0; i < parts.length; i++) r[i] = Integer.parseInt(parts[i]);
        return r;
    }

    private String encodeTowers(List<List<Integer>> towers) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < towers.size(); i++) {
            if (i > 0) sb.append('/');
            List<Integer> t = towers.get(i);
            for (int j = 0; j < t.size(); j++) {
                if (j > 0) sb.append(',');
                sb.append(t.get(j));
            }
        }
        return sb.toString();
    }

    private List<List<Integer>> decodeTowers(String s) {
        List<List<Integer>> result = new ArrayList<>();
        String[] parts = s.split("/", -1);
        for (String part : parts) {
            List<Integer> tower = new ArrayList<>();
            if (!part.isEmpty()) {
                for (String d : part.split(",")) tower.add(Integer.parseInt(d));
            }
            result.add(tower);
        }
        while (result.size() < 3) result.add(new ArrayList<>());
        return result;
    }

    public static String encodeCards(List<Card> cards) {
        if (cards == null) return "";
        List<String> parts = new ArrayList<>();
        for (Card card : cards) {
            parts.add(card.getPosition() + "," + card.getSymbol() + "," + card.getImage() + "," + card.isRevealed() + "," + card.isMatched());
        }
        return String.join(";", parts);
    }

    public static List<Card> decodeCards(String value) {
        List<Card> cards = new ArrayList<>();
        if (value == null || value.isBlank()) return cards;
        for (String part : value.split(";")) {
            String[] fields = part.split(",");
            cards.add(new Card(
                    Integer.parseInt(fields[0]), fields[1], fields[2],
                    Boolean.parseBoolean(fields[3]), Boolean.parseBoolean(fields[4])
            ));
        }
        return cards;
    }
}
