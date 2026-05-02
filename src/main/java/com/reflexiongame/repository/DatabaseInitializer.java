package com.reflexiongame.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class DatabaseInitializer {
    private final JdbcTemplate jdbcTemplate;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostConstruct
    public void init() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS players (
                id BIGSERIAL PRIMARY KEY,
                name VARCHAR(80) NOT NULL UNIQUE,
                best_score INT NOT NULL DEFAULT 0,
                progression INT NOT NULL DEFAULT 0
            )
            """);

        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS games (
                id BIGSERIAL PRIMARY KEY,
                player_id BIGINT NOT NULL,
                game_type VARCHAR(20) NOT NULL DEFAULT 'MEMORY',
                difficulty VARCHAR(20) NOT NULL,
                cards_state TEXT NOT NULL,
                moves INT NOT NULL DEFAULT 0,
                matched_pairs INT NOT NULL DEFAULT 0,
                score INT NOT NULL DEFAULT 0,
                completed BOOLEAN NOT NULL DEFAULT FALSE,
                started_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                completed_at TIMESTAMP,
                CONSTRAINT fk_games_player FOREIGN KEY (player_id) REFERENCES players(id)
            )
            """);

        jdbcTemplate.execute("ALTER TABLE games ADD COLUMN IF NOT EXISTS game_type VARCHAR(20) NOT NULL DEFAULT 'MEMORY'");
        // Remove rows whose game_type is no longer supported.
        jdbcTemplate.execute(
                "DELETE FROM games WHERE game_type NOT IN " +
                "('MEMORY','SUDOKU','PICTURE_PUZZLE','NUMBER_RUSH','TOWER_OF_HANOI')"
        );
    }
}
