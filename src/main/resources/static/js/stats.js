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

    function updateBar(row, newValue, oldValue) {
        var fill = row.querySelector('.stat-row__fill');
        var val = row.querySelector('.stat-row__val');
        var bar = row.querySelector('.stat-row__bar');

        fill.className = 'stat-row__fill ' + barClass(newValue);
        fill.style.width = newValue + '%';
        val.textContent = newValue;

        // Animate value flash
        if (oldValue !== undefined && oldValue !== newValue) {
            var flashClass = newValue > oldValue ? 'flash-up' : 'flash-down';
            val.classList.add(flashClass);

            var pulseClass = newValue > oldValue ? 'pulse-up' : 'pulse-down';
            bar.classList.add(pulseClass);

            setTimeout(function () {
                val.classList.remove(flashClass);
                bar.classList.remove(pulseClass);
            }, 700);
        }
    }

    function updateResource(el, newValue, oldValue) {
        el.textContent = newValue;
        if (oldValue !== undefined && oldValue !== newValue) {
            var flashClass = newValue > oldValue ? 'flash-up' : 'flash-down';
            el.classList.add(flashClass);
            setTimeout(function () {
                el.classList.remove(flashClass);
            }, 700);
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

        diffs.forEach(function (d) {
            var delta = d.cur - d.old;
            if (delta !== 0) {
                var sign = delta > 0 ? '+' : '';
                var color = delta > 0 ? 'color:var(--green)' : 'color:var(--red)';
                parts.push('<span style="' + color + '">' + d.label + ' ' + sign + delta + '</span>');
                net += delta;
            }
        });

        var actionName = (newState.lastAction || '').replace(/_/g, ' ');
        var evt = newState.lastEvent;
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
        // Remove existing story beat
        var existing = document.querySelector('.story-beat');
        if (existing) existing.remove();

        // If waiting for a choice, don't render the story beat (modal handles it)
        if (state.waitingEventChoice && state.lastEvent) {
            return;
        }

        if (!state.lastAction) return;

        var section = document.createElement('div');
        section.className = 'story-beat story-beat--entering';

        // Action label
        var actionDiv = document.createElement('div');
        actionDiv.className = 'story-beat__action';
        var actionSpan = document.createElement('span');
        actionSpan.className = 'story-beat__action-label';
        actionSpan.textContent = state.lastAction.replace(/_/g, ' ');
        actionDiv.appendChild(actionSpan);
        section.appendChild(actionDiv);

        // Event
        var evt = state.lastEvent;
        if (evt) {
            var eventDiv = document.createElement('div');
            eventDiv.className = 'story-beat__event';

            var titleP = document.createElement('p');
            titleP.className = 'story-beat__title';
            titleP.textContent = evt.title;
            eventDiv.appendChild(titleP);

            var descP = document.createElement('p');
            descP.className = 'story-beat__desc';
            descP.textContent = evt.description;
            eventDiv.appendChild(descP);

            var impactDiv = document.createElement('div');
            impactDiv.className = 'story-beat__impacts';
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
                var span = document.createElement('span');
                span.className = pair[1] > 0 ? 'positive' : 'negative';
                span.textContent = pair[0] + ' ' + (pair[1] > 0 ? '+' : '') + pair[1];
                impactDiv.appendChild(span);
            });
            eventDiv.appendChild(impactDiv);
            section.appendChild(eventDiv);
        }

        // Insert into the stats column (game-body__stats)
        var statsCol = document.querySelector('.game-body__stats');
        if (statsCol) {
            statsCol.appendChild(section);
        }

        // Animate in
        requestAnimationFrame(function () {
            requestAnimationFrame(function () {
                section.classList.remove('story-beat--entering');
            });
        });
    }

    function renderState(state, oldState) {
        // Turn and location
        var turnLabel = document.querySelector('.top-bar__turn');
        var locationName = document.querySelector('.journey-progress__city--current');
        var distanceText = document.querySelector('.journey-progress__dist');

        if (turnLabel) turnLabel.textContent = 'Turn ' + state.turn;
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

        // Story beat section
        renderStoryBeat(state);
    }

    // Expose globally for other modules
    window.GameStats = {
        renderState: renderState,
        updateResourceBars: updateResourceBars,
        buildCombinedToast: buildCombinedToast
    };

})();
