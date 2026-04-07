(function () {
    'use strict';

    // DOM references
    var statRows = document.querySelectorAll('.stat-row');
    var healthRow = statRows[0];
    var energyRow = statRows[1];
    var moraleRow = statRows[2];

    var cashVal = document.querySelector('.cash-val');
    var foodVal = document.querySelector('.food-val');
    var computeVal = document.querySelector('.compute-val');

    // Resource bar references
    var cashFill = document.querySelector('.res-item__fill--cash');
    var foodFill = document.querySelector('.res-item__fill--food');
    var computeFill = document.querySelector('.res-item__fill--compute');

    // Helpers

    function barClass(value) {
        if (value <= 25) return 'danger';
        if (value <= 50) return 'warning';
        return 'good';
    }

    /**
     * Animate a number counting from old to new value.
     */
    function animateNumber(el, from, to, duration) {
        if (from === to) {
            el.textContent = to;
            return;
        }
        duration = duration || 500;
        var start = performance.now();
        var diff = to - from;

        function tick(now) {
            var elapsed = now - start;
            var progress = Math.min(elapsed / duration, 1);
            // Ease out cubic
            var eased = 1 - Math.pow(1 - progress, 3);
            var current = Math.round(from + diff * eased);
            el.textContent = current;
            if (progress < 1) {
                requestAnimationFrame(tick);
            }
        }

        requestAnimationFrame(tick);
    }

    /**
     * Create a floating stat change indicator (e.g., "+15" or "-10")
     * that floats upward and fades out.
     */
    function createFloatingIndicator(targetEl, delta) {
        var rect = targetEl.getBoundingClientRect();
        var indicator = document.createElement('div');
        indicator.className = 'stat-change-float ' +
            (delta > 0 ? 'stat-change-float--positive' : 'stat-change-float--negative');
        indicator.textContent = (delta > 0 ? '+' : '') + delta;

        // Position near the stat value
        indicator.style.position = 'fixed';
        indicator.style.left = rect.left + 'px';
        indicator.style.top = (rect.top - 4) + 'px';

        document.body.appendChild(indicator);

        // Remove after animation
        setTimeout(function () {
            if (indicator.parentNode) indicator.parentNode.removeChild(indicator);
        }, 850);
    }

    /**
     * Trigger screen shake for big damage events.
     */
    function triggerScreenShake() {
        var container = document.querySelector('.container');
        if (!container) return;
        container.classList.remove('screen-shake');
        // Force reflow
        void container.offsetWidth;
        container.classList.add('screen-shake');
        setTimeout(function () {
            container.classList.remove('screen-shake');
        }, 450);
    }

    /**
     * Flash the screen border with damage/gain color.
     */
    function triggerScreenFlash(type) {
        var body = document.body;
        var cls = type === 'damage' ? 'damage-flash' : 'gain-flash';
        body.classList.remove('damage-flash', 'gain-flash');
        void body.offsetWidth;
        body.classList.add(cls);
        setTimeout(function () {
            body.classList.remove(cls);
        }, 550);
    }

    function updateBar(row, newValue, oldValue) {
        var fill = row.querySelector('.stat-row__fill');
        var val = row.querySelector('.stat-row__val');
        var bar = row.querySelector('.stat-row__bar');

        fill.className = 'stat-row__fill ' + barClass(newValue);
        fill.style.width = newValue + '%';

        // Animate the number counting
        if (oldValue !== undefined && oldValue !== newValue) {
            animateNumber(val, oldValue, newValue, 450);

            var flashClass = newValue > oldValue ? 'flash-up' : 'flash-down';
            val.classList.add(flashClass);

            var pulseClass = newValue > oldValue ? 'pulse-up' : 'pulse-down';
            bar.classList.add(pulseClass);

            // Floating indicator
            var delta = newValue - oldValue;
            createFloatingIndicator(val, delta);

            setTimeout(function () {
                val.classList.remove(flashClass);
                bar.classList.remove(pulseClass);
            }, 800);
        } else {
            val.textContent = newValue;
        }
    }

    function updateResource(el, newValue, oldValue) {
        if (oldValue !== undefined && oldValue !== newValue) {
            animateNumber(el, oldValue, newValue, 400);

            var flashClass = newValue > oldValue ? 'flash-up' : 'flash-down';
            el.classList.add(flashClass);

            // Floating indicator
            var delta = newValue - oldValue;
            createFloatingIndicator(el, delta);

            setTimeout(function () {
                el.classList.remove(flashClass);
            }, 800);
        } else {
            el.textContent = newValue;
        }
    }

    // Resource bars use a relative scale: the bar fills proportionally,
    // capped so that the "full" point feels reasonable for each resource.
    function resourceBarPercent(value, refMax) {
        if (value <= 0) return 0;
        var pct = (value / refMax) * 100;
        return Math.min(pct, 100);
    }

    function updateResourceBars(res) {
        // Reference maxes: these give a sense of "full" for each bar.
        // Cash 200 = full bar, Food 20 = full bar, Compute 20 = full bar
        if (cashFill) cashFill.style.width = resourceBarPercent(res.cash, 200) + '%';
        if (foodFill) foodFill.style.width = resourceBarPercent(res.food, 20) + '%';
        if (computeFill) computeFill.style.width = resourceBarPercent(res.computeCredits, 20) + '%';
    }

    function buildCombinedToast(oldState, newState) {
        if (!oldState) return;

        var parts = [];
        var net = 0;

        var diffs = [
            {label: 'Health', old: oldState.teamState.health, cur: newState.teamState.health},
            {label: 'Energy', old: oldState.teamState.energy, cur: newState.teamState.energy},
            {label: 'Morale', old: oldState.teamState.morale, cur: newState.teamState.morale},
            {label: 'Cash', old: oldState.resourceState.cash, cur: newState.resourceState.cash},
            {label: 'Food', old: oldState.resourceState.food, cur: newState.resourceState.food},
            {label: 'Compute', old: oldState.resourceState.computeCredits, cur: newState.resourceState.computeCredits}
        ];

        var totalNegative = 0;
        var totalPositive = 0;

        diffs.forEach(function (d) {
            var delta = d.cur - d.old;
            if (delta !== 0) {
                var sign = delta > 0 ? '+' : '';
                var color = delta > 0 ? 'color:var(--green)' : 'color:var(--red)';
                parts.push('<span style="' + color + '">' + d.label + ' ' + sign + delta + '</span>');
                net += delta;
                if (delta < 0) totalNegative += Math.abs(delta);
                if (delta > 0) totalPositive += delta;
            }
        });

        // Screen effects based on severity
        if (totalNegative >= 15) {
            triggerScreenShake();
            triggerScreenFlash('damage');
        } else if (totalNegative >= 8) {
            triggerScreenFlash('damage');
        }

        if (totalPositive >= 15) {
            triggerScreenFlash('gain');
        }

        var newTr = newState.lastTurnResult || {};
        var actionName = (newTr.gameAction || '').replace(/_/g, ' ');
        var evt = newTr.gameEvent;
        var type = net >= 0 ? 'positive' : 'negative';

        var html = '<div class="toast__title">' + window.GameToast.escapeHtml(actionName);
        if (evt && evt.title) {
            html += ' &mdash; ' + window.GameToast.escapeHtml(evt.title);
        }
        html += '</div>';

        if (parts.length > 0) {
            html += '<div class="toast__body">' + parts.join(' &middot; ') + '</div>';
        }

        // Check for location change
        var oldLoc = oldState.journeyState.currentLocation.name;
        var newLoc = newState.journeyState.currentLocation.name;
        if (oldLoc !== newLoc) {
            type = 'location';
            html = '<div class="toast__title">Arrived at ' + window.GameToast.escapeHtml(newLoc) + '!</div>' +
                '<div class="toast__body">' + parts.join(' &middot; ') + '</div>';
        }

        window.GameToast.show(html, type, window.GameToast.TOAST_DURATION);
    }

    function renderStoryBeat(state) {
        var container = document.getElementById('story-beat-container');
        if (!container) return;

        var tr = state.lastTurnResult || {};

        // If waiting for a choice, show waiting state
        if (tr.waitingEventChoice && tr.gameEvent) {
            container.innerHTML = '<div class="story-beat__empty"><span class="story-beat__action-label">A decision awaits...</span></div>';
            return;
        }

        if (!tr.gameAction) {
            container.innerHTML = '<div class="story-beat__empty"><span class="story-beat__action-label">Waiting for orders...</span></div>';
            return;
        }

        var html = '<div class="story-beat__action"><span class="story-beat__action-label">' +
            tr.gameAction.replace(/_/g, ' ') + '</span></div>';

        var evt = tr.gameEvent;
        if (evt) {
            html += '<div class="story-beat__event">';
            html += '<p class="story-beat__title">' + escapeHtml(evt.title) + '</p>';
            html += '<p class="story-beat__desc">' + escapeHtml(evt.description) + '</p>';
            html += '<div class="story-beat__impacts">';
            var impacts = [
                ['Health', evt.healthChange],
                ['Energy', evt.energyChange],
                ['Morale', evt.moraleChange],
                ['Cash', evt.cashChange],
                ['Food', evt.foodChange],
                ['Compute', evt.computeCreditsChange]
            ];
            impacts.forEach(function (pair) {
                if (pair[1] === 0) return;
                var cls = pair[1] > 0 ? 'positive' : 'negative';
                var sign = pair[1] > 0 ? '+' : '';
                html += '<span class="' + cls + '">' + pair[0] + ' ' + sign + pair[1] + '</span>';
            });
            html += '</div></div>';
        }

        container.innerHTML = html;
    }

    function escapeHtml(text) {
        var div = document.createElement('div');
        div.appendChild(document.createTextNode(text));
        return div.innerHTML;
    }

    var foodWarn = document.getElementById('food-warn');
    var cashWarn = document.getElementById('cash-warn');
    var computeWarn = document.getElementById('compute-warn');
    var config = window.__gameConfig || {};
    var FOOD_GRACE = config.foodGraceTurns || 2;
    var CASH_GRACE = config.cashGraceTurns || 3;

    function updateGraceWarnings(state) {
        var foodTurns = state.turnWithoutFood || 0;
        var cashTurns = state.turnWithoutCash || 0;

        if (foodWarn) {
            foodWarn.style.display = 'inline';
            if (foodTurns > 0) {
                foodWarn.textContent = foodTurns + '/' + FOOD_GRACE;
                foodWarn.className = 'res-item__warn res-item__warn--danger';
            } else {
                foodWarn.textContent = '0/' + FOOD_GRACE;
                foodWarn.className = 'res-item__warn res-item__warn--safe';
            }
        }

        if (cashWarn) {
            cashWarn.style.display = 'inline';
            if (cashTurns > 0) {
                cashWarn.textContent = cashTurns + '/' + CASH_GRACE;
                cashWarn.className = 'res-item__warn res-item__warn--danger';
            } else {
                cashWarn.textContent = '0/' + CASH_GRACE;
                cashWarn.className = 'res-item__warn res-item__warn--safe';
            }
        }
    }

    function renderState(state, oldState) {
        // Turn counter with tick animation
        var turnLabel = document.querySelector('.top-bar__turn');
        var locationName = document.querySelector('.journey-progress__city--current');
        var distanceText = document.querySelector('.journey-progress__dist');

        if (turnLabel) {
            turnLabel.textContent = 'Turn ' + state.turn;
            // Animate the turn counter
            if (oldState && state.turn !== oldState.turn) {
                turnLabel.classList.remove('turn-tick');
                void turnLabel.offsetWidth;
                turnLabel.classList.add('turn-tick');
                setTimeout(function () {
                    turnLabel.classList.remove('turn-tick');
                }, 450);
            }
        }
        if (locationName) locationName.textContent = state.journeyState.currentLocation.name;
        if (distanceText) distanceText.textContent = 'Next stop: ' + state.journeyState.distanceToNextLocation.toFixed(1) + ' km';

        // Team stats
        var oh = oldState ? oldState.teamState.health : undefined;
        var oe = oldState ? oldState.teamState.energy : undefined;
        var om = oldState ? oldState.teamState.morale : undefined;
        updateBar(healthRow, state.teamState.health, oh);
        updateBar(energyRow, state.teamState.energy, oe);
        updateBar(moraleRow, state.teamState.morale, om);

        // Resources
        var oc = oldState ? oldState.resourceState.cash : undefined;
        var of_ = oldState ? oldState.resourceState.food : undefined;
        var occ = oldState ? oldState.resourceState.computeCredits : undefined;
        updateResource(cashVal, state.resourceState.cash, oc);
        updateResource(foodVal, state.resourceState.food, of_);
        updateResource(computeVal, state.resourceState.computeCredits, occ);

        // Update resource bars
        updateResourceBars(state.resourceState);

        // Warnings
        updateGraceWarnings(state);
        updateStatWarnings(state);

        // Story beat section
        renderStoryBeat(state);
    }

    function updateStatWarnings(state) {
        if (computeWarn) {
            computeWarn.style.display = 'inline';
            if (state.resourceState.computeCredits <= 0) {
                computeWarn.textContent = 'slow';
                computeWarn.className = 'res-item__warn res-item__warn--danger';
            } else {
                computeWarn.textContent = 'slow';
                computeWarn.className = 'res-item__warn res-item__warn--disabled';
            }
        }
    }

    // Show warnings on initial page load
    updateGraceWarnings({turnWithoutFood: 0, turnWithoutCash: 0});
    updateStatWarnings({resourceState: {computeCredits: parseInt(document.querySelector('.compute-val').textContent) || 0}});

    // Expose globally for other modules
    window.GameStats = {
        renderState: renderState,
        renderStoryBeat: renderStoryBeat,
        updateResourceBars: updateResourceBars,
        buildCombinedToast: buildCombinedToast
    };

})();
