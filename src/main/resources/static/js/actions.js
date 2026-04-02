(function () {
    'use strict';

    // Constants
    var ACTION_COOLDOWN_MS = 500;

    // DOM references
    var locationName = document.querySelector('.location-strip__name');
    var healthRow = document.querySelectorAll('.stat-row')[0];
    var energyRow = document.querySelectorAll('.stat-row')[1];
    var moraleRow = document.querySelectorAll('.stat-row')[2];
    var cashVal = document.querySelector('.cash-val');
    var foodVal = document.querySelector('.food-val');
    var computeVal = document.querySelector('.compute-val');

    // State tracking
    var isProcessing = false;

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
                btn.disabled = false;
                btn.classList.remove('cooldown');
            });
        }, ACTION_COOLDOWN_MS);
    }

    // AJAX action handling

    function handleAction(actionValue) {
        if (isProcessing) return;
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

                // Single combined toast (replaces any existing one)
                window.GameStats.buildCombinedToast(oldState, state);
            })
            .catch(function (err) {
                console.error('Action failed, falling back to form submit:', err);
                window.location.href = '/game';
            })
            .finally(function () {
                isProcessing = false;
                setButtonsProcessing(false);
                // Apply brief cooldown after action completes
                applyCooldown();
            });
    }

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

})();
