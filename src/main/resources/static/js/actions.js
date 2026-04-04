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
    var previousState = {
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

            btn.addEventListener('click', function () {
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

        // Handle choice-pending state via modal
        if (state.waitingEventChoice && state.lastEvent) {
            showChoiceModal(state.lastEvent);
        } else {
            setChoicePending(false);
        }

        // Disable actions the player can't afford
        updateActionAvailability(state.teamState.energy);
    }

    // AJAX action handling
    function handleAction(actionValue) {
        if (isProcessing || waitingForChoice) return;
        isProcessing = true;
        setButtonsProcessing(true);

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
                applyStateUpdate(state, oldState);

                // Play action animation
                if (window.GameAnimations) {
                    window.GameAnimations.playAction(actionValue, state);
                    window.GameAnimations.updateTeamStatus(state.teamState);
                }

                // Single combined toast (only if not waiting for choice)
                if (!state.waitingEventChoice) {
                    window.GameStats.buildCombinedToast(oldState, state);
                }
            })
            .catch(function (err) {
                console.error('Action failed, falling back to form submit:', err);
                window.location.href = '/game';
            })
            .finally(function () {
                isProcessing = false;
                setButtonsProcessing(false);
                // Apply brief cooldown after action completes (only if not waiting for choice)
                if (!waitingForChoice) {
                    applyCooldown();
                }
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

    // Intercept form submissions
    var forms = document.querySelectorAll('.action-card');
    forms.forEach(function (form) {
        form.addEventListener('submit', function (e) {
            e.preventDefault();
            var actionInput = form.querySelector('input[name="action"]');
            if (actionInput) {
                handleAction(actionInput.value);
            }
        });
    });

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
                btn.addEventListener('click', function () {
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
