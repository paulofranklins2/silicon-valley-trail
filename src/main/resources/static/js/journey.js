(function () {
    'use strict';

    var dotsContainer = document.getElementById('journey-dots');
    var trainEl = document.getElementById('journey-train');
    var distLabel = document.querySelector('.journey-progress__dist');
    var currentCityLabel = document.querySelector('.journey-progress__city--current');
    var rail = document.querySelector('.journey-progress__rail');

    var data = window.__journeyData || {};
    var locations = data.locations || [];
    var distances = data.distances || [];
    var totalLocations = locations.length;

    // Compute the total journey distance (sum of all segment distances)
    function totalJourneyDistance() {
        var sum = 0;
        for (var i = 0; i < distances.length; i++) {
            sum += distances[i];
        }
        return sum;
    }

    // Compute cumulative distance to start of each location
    function cumulativeDistances() {
        var cum = [0];
        for (var i = 0; i < distances.length; i++) {
            cum.push(cum[i] + distances[i]);
        }
        return cum;
    }

    // Get the proportional percent position for a given location index
    function locationPercent(index) {
        var totalDist = totalJourneyDistance();
        if (totalDist === 0) return 0;
        var cumDist = cumulativeDistances();
        var dist = index < cumDist.length ? cumDist[index] : totalDist;
        return (dist / totalDist) * 100;
    }

    // Build dot markers for each city, positioned proportionally
    function buildDots() {
        if (!dotsContainer || totalLocations === 0) return;
        dotsContainer.innerHTML = '';

        for (var i = 0; i < totalLocations; i++) {
            var dot = document.createElement('div');
            dot.className = 'journey-dot';
            // Position based on real cumulative distances
            var pct = locationPercent(i);
            dot.style.left = pct + '%';

            // Label
            var label = document.createElement('span');
            label.className = 'journey-dot__label';
            var name = locations[i].name || locations[i];
            // Abbreviate long names
            if (name.length > 10) {
                name = name.substring(0, 8) + '..';
            }
            label.textContent = name;
            dot.appendChild(label);

            dot.setAttribute('data-index', i);
            dotsContainer.appendChild(dot);
        }
    }

    // Update the visual state: dots, train position, labels
    function updateJourney(currentIndex, distRemaining) {
        if (totalLocations === 0) return;

        var cumDist = cumulativeDistances();
        var totalDist = totalJourneyDistance();

        // Distance already traveled: cumulative to current segment start + how far into this segment
        var segmentDist = currentIndex < distances.length ? distances[currentIndex] : 0;
        var traveledInSegment = segmentDist - Math.max(0, distRemaining);
        var totalTraveled = cumDist[currentIndex] + traveledInSegment;
        var progressPct = totalDist > 0 ? (totalTraveled / totalDist) * 100 : 0;
        progressPct = Math.max(0, Math.min(100, progressPct));

        // Update rail fill
        if (rail) {
            rail.style.setProperty('--rail-progress', progressPct + '%');
        }

        // Update train position
        if (trainEl) {
            trainEl.style.left = progressPct + '%';
        }

        // Update dots - a dot is "visited" if its index < currentIndex, "current" if equal
        var dots = dotsContainer ? dotsContainer.querySelectorAll('.journey-dot') : [];
        for (var i = 0; i < dots.length; i++) {
            dots[i].classList.remove('journey-dot--visited', 'journey-dot--current');
            if (i < currentIndex) {
                dots[i].classList.add('journey-dot--visited');
            } else if (i === currentIndex) {
                dots[i].classList.add('journey-dot--current');
            }
        }

        // Update city label
        if (currentCityLabel && locations[currentIndex]) {
            currentCityLabel.textContent = locations[currentIndex].name || locations[currentIndex];
        }

        // Update distance text
        if (distLabel) {
            if (currentIndex >= totalLocations - 1) {
                distLabel.textContent = 'Destination reached!';
            } else {
                distLabel.textContent = 'Next stop: ' + distRemaining.toFixed(1) + ' km';
            }
        }
    }

    // Initialize
    buildDots();
    updateJourney(data.currentIndex || 0, data.distanceRemaining || 0);

    // Expose for stats.js / actions.js to call after AJAX
    window.GameJourney = {
        update: updateJourney,
        setData: function (newLocations, newDistances) {
            if (newLocations) locations = newLocations;
            if (newDistances) distances = newDistances;
        }
    };

})();
