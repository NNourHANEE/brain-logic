package com.reflexiongame.controller;

import com.reflexiongame.model.Difficulty;
import com.reflexiongame.model.GameState;
import com.reflexiongame.model.GameType;
import com.reflexiongame.service.GameService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class GameController {
    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping({"/", ""})
    public String home(Model model) {
        model.addAttribute("difficulties", Difficulty.values());
        model.addAttribute("gameTypes", GameType.values());
        model.addAttribute("scores", gameService.scoreBoard());
        return "index";
    }

    @PostMapping("/start")
    public String start(@RequestParam String playerName,
                        @RequestParam Difficulty difficulty,
                        @RequestParam(defaultValue = "MEMORY") GameType gameType) {
        GameState game = gameService.createGame(playerName, difficulty, gameType);
        return "redirect:/game/" + game.getId();
    }

    @GetMapping("/game/{id}")
    public String game(@PathVariable Long id,
                       @RequestParam(name = "just-won", required = false) Boolean justWon,
                       @RequestParam(name = "just-lost", required = false) Boolean justLost,
                       Model model) {
        GameState game = gameService.findGame(id).orElseThrow();

        // If the player came back after the time budget expired, mark the game as lost now.
        boolean justTimedOut = gameService.tickTimeLimit(game);
        if (justTimedOut) {
            justLost = Boolean.TRUE;
        }

        boolean celebrate = Boolean.TRUE.equals(justWon) && game.isWon();
        boolean dommage   = Boolean.TRUE.equals(justLost) && game.isLost();

        model.addAttribute("game", game);
        model.addAttribute("stars", gameService.stars(game));
        model.addAttribute("celebrate", celebrate);
        model.addAttribute("dommage", dommage);

        // No celebration overlay → show the dedicated result page when the game is over.
        if (game.isCompleted() && !celebrate && !dommage) {
            return "result";
        }

        return switch (game.getGameType()) {
            case MEMORY -> "game";
            case SUDOKU -> "sudoku";
            case PICTURE_PUZZLE -> "picture";
            case NUMBER_RUSH -> "numbers";
            case TOWER_OF_HANOI -> "hanoi";
        };
    }

    @PostMapping("/game/{id}/select")
    public String select(@PathVariable Long id, @RequestParam int position) {
        GameState before = gameService.findGame(id).orElseThrow();
        boolean wasCompleted = before.isCompleted();
        GameState game = gameService.play(id, position);
        return redirectAfterAction(id, wasCompleted, game);
    }

    @PostMapping("/game/{id}/sudoku")
    public String sudoku(@PathVariable Long id, @RequestParam int cell, @RequestParam int value) {
        GameState before = gameService.findGame(id).orElseThrow();
        boolean wasCompleted = before.isCompleted();
        GameState game = gameService.sudokuFill(id, cell, value);
        return redirectAfterAction(id, wasCompleted, game);
    }

    @PostMapping("/game/{id}/puzzle")
    public String puzzle(@PathVariable Long id, @RequestParam int slot) {
        GameState before = gameService.findGame(id).orElseThrow();
        boolean wasCompleted = before.isCompleted();
        GameState game = gameService.puzzleClick(id, slot);
        return redirectAfterAction(id, wasCompleted, game);
    }

    @PostMapping("/game/{id}/number")
    public String number(@PathVariable Long id, @RequestParam int cell) {
        GameState before = gameService.findGame(id).orElseThrow();
        boolean wasCompleted = before.isCompleted();
        GameState game = gameService.numberClick(id, cell);
        return redirectAfterAction(id, wasCompleted, game);
    }

    @PostMapping("/game/{id}/hanoi")
    public String hanoi(@PathVariable Long id, @RequestParam int tower) {
        GameState before = gameService.findGame(id).orElseThrow();
        boolean wasCompleted = before.isCompleted();
        GameState game = gameService.hanoiClick(id, tower);
        return redirectAfterAction(id, wasCompleted, game);
    }

    /** Browser-driven keep-alive that triggers the time-out check server-side. */
    @PostMapping("/game/{id}/tick")
    public String tick(@PathVariable Long id) {
        GameState before = gameService.findGame(id).orElseThrow();
        boolean wasCompleted = before.isCompleted();
        boolean justLost = gameService.tickTimeLimit(before);
        if (!wasCompleted && justLost) {
            return "redirect:/game/" + id + "?just-lost=true";
        }
        return "redirect:/game/" + id;
    }

    @PostMapping("/game/{id}/save")
    public String save(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", "Partie sauvegardée avec succès.");
        return "redirect:/saved";
    }

    @GetMapping("/saved")
    public String saved(Model model) {
        model.addAttribute("games", gameService.savedGames());
        return "saved";
    }

    @GetMapping("/scores")
    public String scores(Model model) {
        model.addAttribute("scores", gameService.scoreBoard());
        return "scores";
    }

    private String redirectAfterAction(Long id, boolean wasCompleted, GameState after) {
        if (!wasCompleted && after.isCompleted()) {
            String flag = after.isWon() ? "just-won" : "just-lost";
            return "redirect:/game/" + id + "?" + flag + "=true";
        }
        return "redirect:/game/" + id;
    }
}
