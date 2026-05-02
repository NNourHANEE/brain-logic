(() => {
  // ----------------------------- SOUND MANAGER -----------------------------
  const SoundManager = (() => {
    let audioContext = null;
    let soundsEnabled = true;
    let pendingTones = [];

    // Try to load user preference
    try {
      const saved = localStorage.getItem('brainlogic_sounds');
      if (saved !== null) soundsEnabled = saved === 'true';
    } catch (e) { /* ignore */ }

    const getAudioContext = () => {
      if (!audioContext) {
        audioContext = new (window.AudioContext || window.webkitAudioContext)();
      }
      return audioContext;
    };

    // Actually play a tone (internal)
    const playTone = (frequency, duration, type = 'sine', volume = 0.08, delay = 0) => {
      try {
        const ctx = getAudioContext();
        const start = ctx.currentTime + delay;
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = type;
        osc.frequency.setValueAtTime(frequency, start);
        gain.gain.setValueAtTime(0.0001, start);
        gain.gain.exponentialRampToValueAtTime(volume, start + 0.01);
        gain.gain.exponentialRampToValueAtTime(0.0001, start + duration);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(start);
        osc.stop(start + duration + 0.02);
      } catch (err) {
        // fail silently
      }
    };

    // Public: play a named sound
    const play = (name) => {
      if (!soundsEnabled) return;
      // Ensure audio context is running (start on user gesture)
      const ctx = getAudioContext();
      if (ctx.state !== 'running') return;

      switch (name) {
        case 'flip':
          playTone(420, 0.08, 'triangle', 0.05);
          playTone(640, 0.09, 'triangle', 0.04, 0.05);
          break;
        case 'correct':
          playTone(540, 0.09, 'sine', 0.07);
          playTone(760, 0.12, 'sine', 0.08, 0.08);
          break;
        case 'wrong':
          playTone(190, 0.11, 'sawtooth', 0.04);
          playTone(150, 0.12, 'sawtooth', 0.035, 0.08);
          break;
        case 'win':
          playTone(523, 0.12, 'sine', 0.08);
          playTone(659, 0.12, 'sine', 0.08, 0.1);
          playTone(784, 0.16, 'sine', 0.09, 0.2);
          playTone(1047, 0.2, 'sine', 0.08, 0.34);
          break;
        case 'lose':
          playTone(330, 0.18, 'sawtooth', 0.06);
          playTone(220, 0.22, 'sawtooth', 0.06, 0.18);
          playTone(150, 0.3, 'sawtooth', 0.07, 0.4);
          break;
        default:
          playTone(360, 0.06, 'triangle', 0.045);
      }
    };

    // Enable/disable sounds
    const setEnabled = (enabled) => {
      soundsEnabled = enabled;
      try { localStorage.setItem('brainlogic_sounds', enabled); } catch(e) {}
      // Also update the toggle button UI if present
      const toggleBtn = document.getElementById('soundToggle');
      if (toggleBtn) {
        toggleBtn.textContent = enabled ? '🔊' : '🔈';
        toggleBtn.setAttribute('aria-label', enabled ? 'Désactiver les sons' : 'Activer les sons');
      }
    };

    const isEnabled = () => soundsEnabled;

    // Must be called after a user gesture to unlock audio
    const unlock = () => {
      const ctx = getAudioContext();
      if (ctx.state === 'suspended') {
        ctx.resume();
      }
    };

    // Auto-unlock on any user click/tap
    document.body.addEventListener('click', unlock, { once: true });
    document.body.addEventListener('touchstart', unlock, { once: true });

    return { play, setEnabled, isEnabled, unlock };
  })();

  // ----------------------------- HELPER FUNCTIONS -----------------------------
  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60).toString().padStart(2, '0');
    const secs = Math.floor(seconds % 60).toString().padStart(2, '0');
    return `${mins}:${secs}`;
  };

  // Close all open popups
  const closeAllPopups = () => {
    document.querySelectorAll('[data-popup]').forEach(popup => {
      popup.classList.remove('is-open');
      popup.setAttribute('aria-hidden', 'true');
    });
  };

  // Update the selection preview on home screen
  const updateSelectionPreview = () => {
    const title = document.querySelector('[data-selection-title]');
    const desc = document.querySelector('[data-selection-description]');
    const gameLabel = document.querySelector('[data-game-choice-label]');
    const levelLabel = document.querySelector('[data-level-choice-label]');
    const selectedGame = document.querySelector('[data-game-card]:has(input:checked)');
    const selectedLevel = document.querySelector('[data-level-card]:has(input:checked)');

    if (title && desc && selectedGame && selectedLevel) {
      title.textContent = `${selectedGame.dataset.gameTitle} · ${selectedLevel.dataset.levelTitle}`;
      desc.textContent = selectedGame.dataset.gameDescription;
    }
    if (gameLabel && selectedGame) gameLabel.textContent = selectedGame.dataset.gameTitle;
    if (levelLabel && selectedLevel) levelLabel.textContent = selectedLevel.dataset.levelTitle;
  };

  // Flash body background on success/failure
  const flashBody = (cls) => {
    document.body.classList.remove('flash-success', 'flash-fail');
    // force reflow
    void document.body.offsetWidth;
    document.body.classList.add(cls);
    setTimeout(() => document.body.classList.remove(cls), 480);
  };

  // ----------------------------- TIMER MANAGER -----------------------------
  class TimerManager {
    constructor() {
      this.intervals = [];
      this.visibilityHandler = this.handleVisibilityChange.bind(this);
      document.addEventListener('visibilitychange', this.visibilityHandler);
    }

    registerTimer(timerElement) {
      const output = timerElement.querySelector('strong');
      const label = timerElement.querySelector('span');
      if (!output) return;

      let elapsed = Number(timerElement.dataset.elapsedSeconds || 0);
      const limit = Number(timerElement.dataset.timeLimitSeconds || 0);
      const tickFormId = timerElement.dataset.tickFormId || '';
      const tickForm = tickFormId ? document.getElementById(tickFormId) : null;

      if (limit > 0 && label) label.textContent = 'Restant';

      let active = true;
      let lastTick = Date.now() / 1000;
      let animationFrame = null;

      const updateDisplay = () => {
        if (!active) return;
        if (limit > 0) {
          const remaining = Math.max(0, limit - elapsed);
          output.textContent = formatTime(remaining);
          timerElement.classList.toggle('timer-low', remaining <= 30 && remaining > 0);
          timerElement.classList.toggle('timer-out', remaining <= 0);
          if (remaining <= 0 && tickForm && !tickForm.dataset.submitted) {
            tickForm.dataset.submitted = '1';
            tickForm.submit();
            active = false; // stop counting
          }
        } else {
          output.textContent = formatTime(elapsed);
        }
      };

      const tick = () => {
        if (!active) return;
        const now = Date.now() / 1000;
        const delta = Math.floor(now - lastTick);
        if (delta >= 1) {
          elapsed += delta;
          lastTick = now;
          updateDisplay();
        }
        animationFrame = requestAnimationFrame(() => tick());
      };

      const stop = () => {
        active = false;
        if (animationFrame) cancelAnimationFrame(animationFrame);
      };

      // start ticking
      lastTick = Date.now() / 1000;
      tick();

      // store stop function to clean up later
      timerElement._stopTimer = stop;
      this.intervals.push(stop);
    }

    handleVisibilityChange() {
      // When tab becomes visible again, we rely on the next tick to correct elapsed time
      // (the timer adjusts using delta). No extra action needed.
    }

    stopAll() {
      this.intervals.forEach(stop => stop());
      this.intervals = [];
      document.removeEventListener('visibilitychange', this.visibilityHandler);
    }
  }

  // ----------------------------- POPUP / UI INIT -----------------------------
  const initPopups = () => {
    // Open popup buttons
    document.querySelectorAll('[data-open-popup]').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.preventDefault();
        closeAllPopups();
        const popupId = btn.dataset.openPopup;
        const popup = document.getElementById(popupId);
        if (popup) {
          popup.classList.add('is-open');
          popup.setAttribute('aria-hidden', 'false');
        }
      });
    });

    // Close buttons
    document.querySelectorAll('[data-close-popup]').forEach(btn => {
      btn.addEventListener('click', closeAllPopups);
    });

    // Click outside popup
    document.querySelectorAll('[data-popup]').forEach(popup => {
      popup.addEventListener('click', (e) => {
        if (e.target === popup) closeAllPopups();
      });
    });

    // Escape key
    document.addEventListener('keydown', (e) => {
      if (e.key === 'Escape') closeAllPopups();
    });
  };

  // ----------------------------- RADIO BUTTON HANDLING -----------------------------
  const initRadioButtons = () => {
    document.querySelectorAll('input[type="radio"]').forEach(radio => {
      radio.addEventListener('change', () => {
        SoundManager.play('tap');
        updateSelectionPreview();
        const label = radio.closest('label');
        if (label) {
          label.classList.add('choice-pulse');
          setTimeout(() => label.classList.remove('choice-pulse'), 360);
        }
        setTimeout(closeAllPopups, 260);
      });
    });
    updateSelectionPreview();
  };

  // ----------------------------- SOUND ON DATA-SOUND CLICKS -----------------------------
  const initSoundTriggers = () => {
    document.addEventListener('click', (e) => {
      const trigger = e.target.closest('[data-sound]');
      if (!trigger) return;
      const sound = trigger.dataset.sound;
      if (sound === 'answer') {
        const form = trigger.closest('[data-correct-answer]');
        const isCorrect = (trigger.value === form?.dataset.correctAnswer);
        SoundManager.play(isCorrect ? 'correct' : 'wrong');
        if (!isCorrect) flashBody('flash-fail');
        else flashBody('flash-success');
        return;
      }
      SoundManager.play(sound);
    });
  };

  // ----------------------------- RESULT PAGE / OUTCOME HANDLING -----------------------------
  const handleResultOutcome = () => {
    const outcome = document.body.dataset.outcome || '';
    if (outcome === 'success') {
      setTimeout(() => { SoundManager.play('correct'); flashBody('flash-success'); }, 120);
    } else if (outcome === 'fail') {
      setTimeout(() => { SoundManager.play('wrong'); flashBody('flash-fail'); }, 120);
    } else {
      const msg = document.body.dataset.resultSound || '';
      if (msg.includes('Bonne') || msg.includes('terminé') || msg.includes('reconstituée')) {
        setTimeout(() => SoundManager.play('correct'), 220);
      }
    }

    // Win/lose music on result page
    if (document.body.dataset.page === 'result') {
      const state = document.body.dataset.gameState || '';
      if (state === 'won') setTimeout(() => SoundManager.play('win'), 280);
      else if (state === 'lost') setTimeout(() => SoundManager.play('lose'), 280);
    }
  };

  // ----------------------------- AUTO-OPEN OVERLAYS (CELEBRATE / DOMMAGE) -----------------------------
  const handleAutoOpenOverlays = () => {
    const celebrate = document.querySelector('[data-auto-open="celebrate"]');
    const dommage = document.querySelector('[data-auto-open="dommage"]');

    if (celebrate) {
      setTimeout(() => SoundManager.play('win'), 280);
      setTimeout(() => {
        celebrate.classList.add('is-open');
        celebrate.setAttribute('aria-hidden', 'false');
      }, 420);
    }

    if (dommage) {
      setTimeout(() => SoundManager.play('lose'), 220);
      setTimeout(() => {
        dommage.classList.add('is-open');
        dommage.setAttribute('aria-hidden', 'false');
      }, 360);
    }
  };

  // ----------------------------- FIRST-TIME TUTORIAL (RULES) -----------------------------
  const initTutorial = () => {
    const rulesKey = document.body.dataset.rulesKey;
    const alreadyCelebrating = document.querySelector('[data-auto-open="celebrate"]');
    const alreadyLosing = document.querySelector('[data-auto-open="dommage"]');
    if (rulesKey && !alreadyCelebrating && !alreadyLosing) {
      try {
        const seenKey = `rules-seen:${rulesKey}`;
        if (!localStorage.getItem(seenKey)) {
          const rulesPopup = document.getElementById('rulesPopup');
          if (rulesPopup) {
            rulesPopup.classList.add('is-open');
            rulesPopup.setAttribute('aria-hidden', 'false');
            localStorage.setItem(seenKey, '1');
          }
        }
      } catch (e) { /* localStorage not available */ }
    }
  };

  // ----------------------------- SOUND TOGGLE BUTTON -----------------------------
  const createSoundToggleButton = () => {
    // Look for existing button, or create one in the header if possible
    let toggleBtn = document.getElementById('soundToggle');
    if (!toggleBtn) {
      const header = document.querySelector('.game-header, .header-actions');
      if (header) {
        toggleBtn = document.createElement('button');
        toggleBtn.id = 'soundToggle';
        toggleBtn.className = 'icon-button sound-toggle';
        toggleBtn.setAttribute('aria-label', 'Activer/désactiver les sons');
        toggleBtn.style.fontSize = '1.4rem';
        header.appendChild(toggleBtn);
      } else {
        // fallback: create a floating button? Better to just attach to body
        toggleBtn = document.createElement('button');
        toggleBtn.id = 'soundToggle';
        toggleBtn.textContent = '🔊';
        toggleBtn.style.position = 'fixed';
        toggleBtn.style.bottom = '20px';
        toggleBtn.style.right = '20px';
        toggleBtn.style.zIndex = '999';
        toggleBtn.style.background = 'rgba(0,0,0,0.6)';
        toggleBtn.style.border = 'none';
        toggleBtn.style.borderRadius = '50%';
        toggleBtn.style.width = '48px';
        toggleBtn.style.height = '48px';
        toggleBtn.style.fontSize = '24px';
        toggleBtn.style.cursor = 'pointer';
        document.body.appendChild(toggleBtn);
      }
    }
    if (toggleBtn) {
      toggleBtn.textContent = SoundManager.isEnabled() ? '🔊' : '🔈';
      toggleBtn.addEventListener('click', () => {
        const newState = !SoundManager.isEnabled();
        SoundManager.setEnabled(newState);
        toggleBtn.textContent = newState ? '🔊' : '🔈';
        if (newState) SoundManager.play('tap');
      });
    }
  };

  // ----------------------------- INITIALIZE EVERYTHING -----------------------------
  const init = () => {
    // First, unlock audio on any user gesture (already handled by SoundManager)
    // Set up timers
    const timerManager = new TimerManager();
    document.querySelectorAll('[data-timer]').forEach(timer => timerManager.registerTimer(timer));

    initPopups();
    initRadioButtons();
    initSoundTriggers();
    handleResultOutcome();
    handleAutoOpenOverlays();
    initTutorial();
    createSoundToggleButton();

    // Cleanup on page unload (optional)
    window.addEventListener('beforeunload', () => {
      timerManager.stopAll();
    });
  };

  // Start everything once DOM is ready
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();