(function () {
    'use strict';

    // Constants
    var ACTION_COOLDOWN_MS = 500;

    // DOM references
    var locationName = document.querySelector('.journey-progress__city--current');
    var healthRow = document.querySelectorAll('.stat-row')[0];
    var energyRow = document.querySelectorAll('.stat-row')[1];
    var moraleRow = document.querySelectorAll('.stat-row')[2];
    var cashVal = document.querySelector('.cash-val');
    var foodVal = document.querySelector('.food-val');
    var computeVal = document.querySelector('.compute-val');
    var actionsContainer = document.querySelector('.actions');

    // State tracking
    var isProcessing = false;
    var waitingForChoice = false;

    // Capture initial state from DOM for diff comparison
    var turnEl = document.querySelector('.top-bar__turn');
    var turnText = turnEl ? turnEl.textContent : '';
    var previousState = {
        turn: parseInt(turnText.replace(/\D/g, '')) || 1,
        teamState: {
            health: parseInt(healthRow.querySelector('.stat-row__val').textContent) || 0,
            energy: parseInt(energyRow.querySelector('.stat-row__val').textContent) || 0,
            morale: parseInt(moraleRow.querySelector('.stat-row__val').textContent) || 0
        }, resourceState: {
            cash: parseInt(cashVal.textContent) || 0,
            food: parseInt(foodVal.textContent) || 0,
            computeCredits: parseInt(computeVal.textContent) || 0
        }, journeyState: {
            currentLocation: {name: locationName.textContent}
        }
    };

    // Initialize resource bars on load
    window.GameStats.updateResourceBars(previousState.resourceState);

    // Disable actions the player can't afford on load
    updateActionAvailability(previousState.teamState.energy);

    // Modal DOM references
    var choiceModal = document.getElementById('choice-modal');
    var choiceModalTitle = document.getElementById('choice-modal-title');
    var choiceModalDesc = document.getElementById('choice-modal-desc');
    var choiceModalOptions = document.getElementById('choice-modal-options');

    /**
     * Create a ripple effect on button click.
     */
    function createRipple(btn, e) {
        var rect = btn.getBoundingClientRect();
        var x = e.clientX - rect.left;
        var y = e.clientY - rect.top;

        var ripple = document.createElement('span');
        ripple.className = 'btn-ripple';
        ripple.style.left = x + 'px';
        ripple.style.top = y + 'px';
        btn.appendChild(ripple);

        setTimeout(function () {
            if (ripple.parentNode) ripple.parentNode.removeChild(ripple);
        }, 550);
    }

    // Choice-pending state: disable/enable action buttons
    function setChoicePending(pending) {
        waitingForChoice = pending;
        if (actionsContainer) {
            if (pending) {
                actionsContainer.classList.add('actions--choice-pending');
            } else {
                actionsContainer.classList.remove('actions--choice-pending');
            }
        }
    }

    // Build stat change tag HTML for a choice outcome
    function buildChangeTags(outcome) {
        var html = '';
        var fields = [
            ['Health', outcome.healthChange],
            ['Energy', outcome.energyChange],
            ['Morale', outcome.moraleChange],
            ['Cash', outcome.cashChange],
            ['Food', outcome.foodChange],
            ['Compute', outcome.computeCreditsChange]
        ];
        fields.forEach(function (pair) {
            if (pair[1] === 0) return;
            var cls = pair[1] > 0 ? 'choice-modal__tag--positive' : 'choice-modal__tag--negative';
            var sign = pair[1] > 0 ? '+' : '';
            html += '<span class="choice-modal__tag ' + cls + '">' + pair[0] + ' ' + sign + pair[1] + '</span>';
        });
        return html;
    }

    // Show the choice modal with event data
    function showChoiceModal(evt) {
        if (!choiceModal || !evt) return;

        var choices = evt.choices || evt.outcomes || [];
        if (choices.length === 0) return;

        choiceModalTitle.textContent = evt.title || 'Event';
        choiceModalDesc.textContent = evt.description || '';
        choiceModalOptions.innerHTML = '';

        // show current stats so player can evaluate the impact
        var statsHtml = '<div class="choice-modal__stats">' +
            '<span>HP ' + previousState.teamState.health + '</span>' +
            '<span>Energy ' + previousState.teamState.energy + '</span>' +
            '<span>Morale ' + previousState.teamState.morale + '</span>' +
            '<span>Cash $' + previousState.resourceState.cash + '</span>' +
            '<span>Food ' + previousState.resourceState.food + '</span>' +
            '<span>CPU ' + previousState.resourceState.computeCredits + '</span>' +
            '</div>';
        choiceModalOptions.insertAdjacentHTML('beforebegin', '');
        var statsContainer = document.getElementById('choice-modal-stats');
        if (!statsContainer) {
            statsContainer = document.createElement('div');
            statsContainer.id = 'choice-modal-stats';
            choiceModalOptions.parentNode.insertBefore(statsContainer, choiceModalOptions);
        }
        statsContainer.innerHTML = statsHtml;

        choices.forEach(function (outcome, idx) {
            var btn = document.createElement('button');
            btn.className = 'choice-modal__btn';
            btn.setAttribute('data-index', idx);

            var label = document.createElement('span');
            label.className = 'choice-modal__btn-label';
            label.textContent = outcome.description;
            btn.appendChild(label);

            var tagsSpan = document.createElement('span');
            tagsSpan.className = 'choice-modal__btn-tags';
            tagsSpan.innerHTML = buildChangeTags(outcome);
            btn.appendChild(tagsSpan);

            btn.addEventListener('click', function (e) {
                createRipple(btn, e);
                handleModalChoice(idx, outcome.description || 'Option ' + (idx + 1));
            });

            choiceModalOptions.appendChild(btn);
        });

        choiceModal.classList.remove('choice-modal--closing');
        choiceModal.style.display = 'flex';
        setChoicePending(true);
    }

    // Hide the choice modal with closing animation
    function hideChoiceModal() {
        if (!choiceModal) return;
        choiceModal.classList.add('choice-modal--closing');
        setTimeout(function () {
            choiceModal.style.display = 'none';
            choiceModal.classList.remove('choice-modal--closing');
            choiceModalOptions.innerHTML = '';
        }, 250);
    }

    // Handle a choice pick from the modal
    function handleModalChoice(choiceIndex, chosenText) {
        if (isProcessing) return;

        // Disable all modal buttons immediately
        var allBtns = choiceModalOptions.querySelectorAll('.choice-modal__btn');
        allBtns.forEach(function (b) {
            b.disabled = true;
        });

        isProcessing = true;

        fetch('/api/choice', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: 'choiceIndex=' + encodeURIComponent(choiceIndex)
        })
            .then(function (response) {
                if (!response.ok) throw new Error('Choice request failed: ' + response.status);
                return response.json();
            })
            .then(function (state) {
                if (state.gameOver) {
                    window.location.href = '/end';
                    return;
                }

                var oldState = previousState;

                // Hide modal and clear choice-pending
                hideChoiceModal();
                setChoicePending(false);

                applyStateUpdate(state, oldState);

                // Update team status animations
                if (window.GameAnimations) {
                    window.GameAnimations.updateTeamStatus(state.teamState);
                }

                // Show toast with what the player chose
                var toastHtml = '<div class="toast__title">Chose: ' + window.GameToast.escapeHtml(chosenText) + '</div>';
                var parts = [];
                var diffs = [
                    {label: 'Health', old: oldState.teamState.health, cur: state.teamState.health},
                    {label: 'Energy', old: oldState.teamState.energy, cur: state.teamState.energy},
                    {label: 'Morale', old: oldState.teamState.morale, cur: state.teamState.morale},
                    {label: 'Cash', old: oldState.resourceState.cash, cur: state.resourceState.cash},
                    {label: 'Food', old: oldState.resourceState.food, cur: state.resourceState.food},
                    {
                        label: 'Compute',
                        old: oldState.resourceState.computeCredits,
                        cur: state.resourceState.computeCredits
                    }
                ];
                var net = 0;
                diffs.forEach(function (d) {
                    var delta = d.cur - d.old;
                    if (delta !== 0) {
                        var sign = delta > 0 ? '+' : '';
                        var color = delta > 0 ? 'color:var(--green)' : 'color:var(--red)';
                        parts.push('<span style="' + color + '">' + d.label + ' ' + sign + delta + '</span>');
                        net += delta;
                    }
                });
                if (parts.length > 0) {
                    toastHtml += '<div class="toast__body">' + parts.join(' &middot; ') + '</div>';
                }
                var type = net >= 0 ? 'positive' : 'negative';
                window.GameToast.show(toastHtml, type, window.GameToast.TOAST_DURATION);
            })
            .catch(function (err) {
                console.error('Choice failed, reloading:', err);
                window.location.href = '/game';
            })
            .finally(function () {
                isProcessing = false;
                applyCooldown();
            });
    }

    // On page load, check if we are already waiting for a choice (server-rendered state)
    if (window.__waitingEventChoice && window.__lastEvent) {
        showChoiceModal(window.__lastEvent);
    }

    // Show fallback modal if routing API was unavailable - let player retry or accept
    if (window.__gameConfig && window.__gameConfig.usedFallback) {
        var fallbackModal = document.getElementById('fallback-modal');
        if (fallbackModal) {
            fallbackModal.style.display = 'flex';
            setChoicePending(true);

            var retryBtn = document.getElementById('fallback-retry');
            var acceptBtn = document.getElementById('fallback-accept');
            var fallbackDesc = document.getElementById('fallback-desc');
            var originalMode = fallbackModal.getAttribute('data-game-mode');

            retryBtn.addEventListener('click', function () {
                retryBtn.disabled = true;
                acceptBtn.disabled = true;
                retryBtn.querySelector('.choice-modal__btn-label').textContent = 'Connecting...';
                if (fallbackDesc) fallbackDesc.textContent = 'This may take a few seconds.';

                fetch('/api/retry-distances', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/x-www-form-urlencoded'},
                    body: 'gameMode=' + encodeURIComponent(originalMode)
                })
                .then(function (res) { return res.json(); })
                .then(function (data) {
                    if (data.success) {
                        window.location.href = '/game';
                    } else {
                        retryBtn.disabled = false;
                        acceptBtn.disabled = false;
                        retryBtn.querySelector('.choice-modal__btn-label').textContent = 'Retry';
                        if (fallbackDesc) fallbackDesc.textContent = 'Still unavailable. Try again or play with estimated distances.';
                    }
                })
                .catch(function () {
                    retryBtn.disabled = false;
                    acceptBtn.disabled = false;
                    retryBtn.querySelector('.choice-modal__btn-label').textContent = 'Retry';
                    if (fallbackDesc) fallbackDesc.textContent = 'Connection failed. Try again or play with estimated distances.';
                });
            });

            document.getElementById('fallback-close').addEventListener('click', function () {
                window.location.href = '/';
            });

            acceptBtn.addEventListener('click', function () {
                fallbackModal.style.display = 'none';
                setChoicePending(false);
            });
        }
    }

    // Set buttons processing state
    function setButtonsProcessing(processing) {
        var buttons = document.querySelectorAll('.action-card button');
        buttons.forEach(function (btn) {
            btn.disabled = processing;
            if (processing) {
                btn.classList.add('processing');
            } else {
                btn.classList.remove('processing');
            }
        });
    }

    // Brief cooldown after action to prevent spam
    function applyCooldown() {
        var buttons = document.querySelectorAll('.action-card button');
        buttons.forEach(function (btn) {
            btn.disabled = true;
            btn.classList.add('cooldown');
        });
        setTimeout(function () {
            buttons.forEach(function (btn) {
                btn.classList.remove('cooldown');
                // only re-enable if not blocked by energy gate
                if (!btn.classList.contains('action-card__btn--no-energy')) {
                    btn.disabled = false;
                }
            });
        }, ACTION_COOLDOWN_MS);
    }

    // Disable action buttons when player lacks the energy to use them
    function updateActionAvailability(energy) {
        var forms = document.querySelectorAll('.action-card');
        forms.forEach(function (form) {
            var btn = form.querySelector('button');
            if (!btn) return;
            var cost = parseInt(btn.getAttribute('data-energy-cost')) || 0;
            if (cost > 0 && energy < cost) {
                btn.disabled = true;
                btn.classList.add('action-card__btn--no-energy');
            } else {
                btn.classList.remove('action-card__btn--no-energy');
            }
        });
    }

    // Common state update after any response (action or choice)
    function applyStateUpdate(state, oldState, toastOverrideHtml) {
        previousState = {
            turn: state.turn,
            teamState: {
                health: state.teamState.health, energy: state.teamState.energy, morale: state.teamState.morale
            }, resourceState: {
                cash: state.resourceState.cash,
                food: state.resourceState.food,
                computeCredits: state.resourceState.computeCredits
            }, journeyState: {
                currentLocation: {name: state.journeyState.currentLocation.name}
            }
        };

        window.GameStats.renderState(state, oldState);

        // Update journey progress map
        if (window.GameJourney) {
            window.GameJourney.update(
                state.journeyState.currentLocationIndex,
                state.journeyState.distanceToNextLocation
            );
        }

        // Update weather display
        if (window.GameWeather && state.lastWeather) {
            window.GameWeather.update(state.lastWeather, state.lastWeatherTemp);
        }

        // Handle choice-pending state via modal — delay so player sees action result first
        if (state.waitingEventChoice && state.lastEvent) {
            setTimeout(function () {
                showChoiceModal(state.lastEvent);
            }, 800);
        } else {
            setChoicePending(false);
        }

        // Disable actions the player can't afford
        updateActionAvailability(state.teamState.energy);
    }

    // Sequenced turn resolution: story beat -> pause -> stats -> re-enable
    var RESOLVE_DELAY_STORY = 80;   // ms before story text appears
    var RESOLVE_DELAY_STATS = 350;  // ms after story before stats animate
    var RESOLVE_DELAY_ENABLE = 250; // ms after stats before buttons re-enable

    // AJAX action handling
    function handleAction(actionValue, clickEvent) {
        if (isProcessing || waitingForChoice) return;
        isProcessing = true;
        setButtonsProcessing(true);

        // Add resolving class to story beat for anticipation
        var storyContainer = document.getElementById('story-beat-container');
        if (storyContainer) {
            storyContainer.classList.add('story-beat--resolving');
        }

        fetch('/api/action', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: 'action=' + encodeURIComponent(actionValue)
        })
            .then(function (response) {
                if (!response.ok) throw new Error('Request failed: ' + response.status);
                return response.json();
            })
            .then(function (state) {
                if (state.gameOver) {
                    window.location.href = '/end';
                    return;
                }

                var oldState = previousState;

                // Phase 1: Update story beat text first (with typewriter entrance)
                setTimeout(function () {
                    if (storyContainer) {
                        storyContainer.classList.remove('story-beat--resolving');
                        storyContainer.classList.add('story-beat--revealing');
                    }

                    // Update story beat only (not stats yet)
                    window.GameStats.renderStoryBeat(state);

                    // Update journey city label + distance text immediately for context
                    var turnLabel = document.querySelector('.top-bar__turn');
                    if (turnLabel) {
                        turnLabel.textContent = 'Turn ' + state.turn;
                        if (oldState && state.turn !== oldState.turn) {
                            turnLabel.classList.remove('turn-tick');
                            void turnLabel.offsetWidth;
                            turnLabel.classList.add('turn-tick');
                            setTimeout(function () { turnLabel.classList.remove('turn-tick'); }, 450);
                        }
                    }

                    // Play action animation
                    if (window.GameAnimations) {
                        window.GameAnimations.playAction(actionValue, state);
                    }

                    // Phase 2: Animate stats after a beat
                    setTimeout(function () {
                        if (storyContainer) {
                            storyContainer.classList.remove('story-beat--revealing');
                        }

                        // Now update stats with animation
                        applyStateUpdate(state, oldState);

                        if (window.GameAnimations) {
                            window.GameAnimations.updateTeamStatus(state.teamState);
                        }

                        // Show action result toast (even if choice event is pending)
                        window.GameStats.buildCombinedToast(oldState, state);

                        // Phase 3: Re-enable buttons after stats settle
                        setTimeout(function () {
                            isProcessing = false;
                            setButtonsProcessing(false);
                            if (!waitingForChoice) {
                                applyCooldown();
                            }
                        }, RESOLVE_DELAY_ENABLE);

                    }, RESOLVE_DELAY_STATS);

                }, RESOLVE_DELAY_STORY);
            })
            .catch(function (err) {
                console.error('Action failed, falling back to form submit:', err);
                if (storyContainer) {
                    storyContainer.classList.remove('story-beat--resolving', 'story-beat--revealing');
                }
                isProcessing = false;
                setButtonsProcessing(false);
                window.location.href = '/game';
            });
    }

    // Expose choice handler globally (kept for backward compatibility with any remaining references)
    window.GameChoices = {
        pick: function (btnElement) {
            // Modal handles choices now; this is a no-op fallback
            var idx = parseInt(btnElement.getAttribute('data-index'));
            var label = btnElement.querySelector('.event-choice__btn-label, .choice-modal__btn-label');
            var text = label ? label.textContent : 'Option ' + (idx + 1);
            handleModalChoice(idx, text);
        }
    };

    // Intercept form submissions and add ripple effects
    var forms = document.querySelectorAll('.action-card');
    forms.forEach(function (form) {
        var btn = form.querySelector('button');

        // Add ripple on click
        if (btn) {
            btn.addEventListener('mousedown', function (e) {
                if (!btn.disabled) {
                    createRipple(btn, e);
                }
            });
        }

        form.addEventListener('submit', function (e) {
            e.preventDefault();
            var actionInput = form.querySelector('input[name="action"]');
            if (actionInput) {
                handleAction(actionInput.value, e);
            }
        });
    });

    // Add ripple to market button
    var marketBtnEl = document.getElementById('market-btn');
    if (marketBtnEl) {
        marketBtnEl.addEventListener('mousedown', function (e) {
            if (!marketBtnEl.disabled) {
                createRipple(marketBtnEl, e);
            }
        });
    }

    // ---- City Market modal ----
    var marketModal = document.getElementById('market-modal');
    var marketModalTitle = document.getElementById('market-modal-title');
    var marketModalDesc = document.getElementById('market-modal-desc');
    var marketModalOptions = document.getElementById('market-modal-options');
    var marketModalStats = document.getElementById('market-modal-stats');
    var marketBtn = document.getElementById('market-btn');
    var marketBackdrop = document.getElementById('market-backdrop');
    var marketPurchased = [];

    function showMarketModal(evt, purchased) {
        if (!marketModal || !evt) return;
        var choices = evt.outcomes || [];
        if (choices.length === 0) return;
        marketPurchased = purchased || [];

        marketModalTitle.textContent = evt.title || 'City Market';
        marketModalDesc.textContent = evt.description || '';
        marketModalOptions.innerHTML = '';

        // show current stats
        marketModalStats.innerHTML = '<div class="choice-modal__stats">' +
            '<span>Cash $' + previousState.resourceState.cash + '</span>' +
            '<span>Food ' + previousState.resourceState.food + '</span>' +
            '<span>Energy ' + previousState.teamState.energy + '</span>' +
            '<span>Morale ' + previousState.teamState.morale + '</span>' +
            '<span>CPU ' + previousState.resourceState.computeCredits + '</span>' +
            '</div>';

        var isLastIdx = choices.length - 1;
        choices.forEach(function (outcome, idx) {
            var btn = document.createElement('button');
            btn.className = 'choice-modal__btn';
            btn.setAttribute('data-index', idx);

            // disable already purchased options (skip is never disabled)
            var alreadyBought = idx !== isLastIdx && marketPurchased.indexOf(idx) !== -1;
            if (alreadyBought) {
                btn.disabled = true;
                btn.classList.add('choice-modal__btn--sold');
            }

            var label = document.createElement('span');
            label.className = 'choice-modal__btn-label';
            label.textContent = alreadyBought ? outcome.description + ' (SOLD OUT)' : outcome.description;
            btn.appendChild(label);

            var tagsSpan = document.createElement('span');
            tagsSpan.className = 'choice-modal__btn-tags';
            tagsSpan.innerHTML = buildChangeTags(outcome);
            btn.appendChild(tagsSpan);

            if (!alreadyBought) {
                btn.addEventListener('click', function (e) {
                    createRipple(btn, e);
                    handleMarketChoice(idx, outcome.description || 'Option ' + (idx + 1));
                });
            }

            marketModalOptions.appendChild(btn);
        });

        marketModal.classList.remove('choice-modal--closing');
        marketModal.style.display = 'flex';
    }

    function hideMarketModal() {
        if (!marketModal) return;
        marketModal.classList.add('choice-modal--closing');
        setTimeout(function () {
            marketModal.style.display = 'none';
            marketModal.classList.remove('choice-modal--closing');
            marketModalOptions.innerHTML = '';
        }, 250);
    }

    function handleMarketChoice(choiceIndex, chosenText) {
        if (isProcessing) return;
        isProcessing = true;

        var allBtns = marketModalOptions.querySelectorAll('.choice-modal__btn');
        allBtns.forEach(function (b) { b.disabled = true; });

        fetch('/api/market', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body: 'choiceIndex=' + encodeURIComponent(choiceIndex)
        })
            .then(function (response) {
                if (!response.ok) throw new Error('Market request failed: ' + response.status);
                return response.json();
            })
            .then(function (data) {
                var state = data.state;
                var purchased = data.purchased || [];
                if (data.error) {
                    hideMarketModal();
                    window.GameToast.show('<div class="toast__title">' + data.error + '</div>', 'negative', window.GameToast.TOAST_DURATION);
                    return;
                }

                var oldState = previousState;
                hideMarketModal();
                applyStateUpdate(state, oldState);

                if (window.GameAnimations) {
                    window.GameAnimations.updateTeamStatus(state.teamState);
                }

                // toast
                var toastHtml = '<div class="toast__title">Market: ' + window.GameToast.escapeHtml(chosenText) + '</div>';
                var parts = [];
                var diffs = [
                    {label: 'Cash', old: oldState.resourceState.cash, cur: state.resourceState.cash},
                    {label: 'Food', old: oldState.resourceState.food, cur: state.resourceState.food},
                    {label: 'Energy', old: oldState.teamState.energy, cur: state.teamState.energy},
                    {label: 'Morale', old: oldState.teamState.morale, cur: state.teamState.morale},
                    {label: 'Compute', old: oldState.resourceState.computeCredits, cur: state.resourceState.computeCredits}
                ];
                var net = 0;
                diffs.forEach(function (d) {
                    var delta = d.cur - d.old;
                    if (delta !== 0) {
                        var sign = delta > 0 ? '+' : '';
                        var color = delta > 0 ? 'color:var(--green)' : 'color:var(--red)';
                        parts.push('<span style="' + color + '">' + d.label + ' ' + sign + delta + '</span>');
                        net += delta;
                    }
                });
                if (parts.length > 0) {
                    toastHtml += '<div class="toast__body">' + parts.join(' &middot; ') + '</div>';
                }
                window.GameToast.show(toastHtml, net >= 0 ? 'positive' : 'negative', window.GameToast.TOAST_DURATION);
            })
            .catch(function (err) {
                console.error('Market failed:', err);
                hideMarketModal();
            })
            .finally(function () {
                isProcessing = false;
            });
    }

    // Open market button
    if (marketBtn) {
        marketBtn.addEventListener('click', function () {
            if (isProcessing || waitingForChoice) return;
            isProcessing = true;

            fetch('/api/market')
                .then(function (response) {
                    if (!response.ok) throw new Error('Market fetch failed: ' + response.status);
                    return response.json();
                })
                .then(function (data) {
                    showMarketModal(data.event, data.purchased);
                })
                .catch(function (err) {
                    console.error('Failed to open market:', err);
                })
                .finally(function () {
                    isProcessing = false;
                });
        });
    }

    // Close market modal on backdrop click
    if (marketBackdrop) {
        marketBackdrop.addEventListener('click', function () {
            hideMarketModal();
        });
    }

})();
