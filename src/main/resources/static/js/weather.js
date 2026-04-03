(function () {
    'use strict';

    // Weather configuration: icons, labels, effect descriptions, CSS classes
    var WEATHER_CONFIG = {
        CLEAR: {
            icon: '\u2600\uFE0F',
            label: 'Clear',
            effects: '+2 health, +5 energy',
            effectClass: 'weather-effects--positive',
            overlayClass: 'weather-overlay--clear'
        },
        RAINY: {
            icon: '\uD83C\uDF27\uFE0F',
            label: 'Rainy',
            effects: '-5 health, -2 energy',
            effectClass: 'weather-effects--negative',
            overlayClass: 'weather-overlay--rainy'
        },
        STORMY: {
            icon: '\u26C8\uFE0F',
            label: 'Stormy',
            effects: '-10 energy, -1 food',
            effectClass: 'weather-effects--negative',
            overlayClass: 'weather-overlay--stormy'
        },
        HEATWAVE: {
            icon: '\uD83D\uDD25',
            label: 'Heatwave',
            effects: '-5 morale, -3 health',
            effectClass: 'weather-effects--negative',
            overlayClass: 'weather-overlay--heatwave'
        }
    };

    // DOM references
    var weatherIcon = document.getElementById('weather-icon');
    var weatherLabel = document.getElementById('weather-label');
    var weatherEffects = document.getElementById('weather-effects');
    var weatherOverlay = document.getElementById('weather-overlay');
    var weatherParticles = document.getElementById('weather-particles');

    // Current weather tracking for transitions
    var currentWeather = null;

    /**
     * Remove all particle elements from the particles container.
     */
    function clearParticles() {
        if (!weatherParticles) return;
        weatherParticles.innerHTML = '';
    }

    /**
     * Create rain DOM particles for RAINY weather.
     * Three layers at different speeds/sizes for depth.
     */
    function createRainParticles() {
        if (!weatherParticles) return;
        var dropCount = 80;
        var fragment = document.createDocumentFragment();

        for (var i = 0; i < dropCount; i++) {
            var drop = document.createElement('div');
            drop.className = 'rain-drop';

            // Distribute across three layers
            var layer = i % 3;
            if (layer === 1) {
                drop.className += ' rain-drop--medium';
            } else if (layer === 2) {
                drop.className += ' rain-drop--large';
            }

            // Random horizontal position
            var left = Math.random() * 100;
            drop.style.left = left + '%';

            // Vary speed per layer: back(slow) -> mid -> front(fast)
            var baseSpeed;
            var opacity;
            if (layer === 0) {
                baseSpeed = 1.0 + Math.random() * 0.4;
                opacity = 0.4;
            } else if (layer === 1) {
                baseSpeed = 0.7 + Math.random() * 0.3;
                opacity = 0.6;
            } else {
                baseSpeed = 0.5 + Math.random() * 0.25;
                opacity = 0.8;
            }
            drop.style.setProperty('--drop-speed', baseSpeed + 's');
            drop.style.setProperty('--drop-delay', '-' + (Math.random() * baseSpeed).toFixed(2) + 's');
            drop.style.setProperty('--drop-opacity', opacity.toString());

            fragment.appendChild(drop);
        }

        weatherParticles.appendChild(fragment);
    }

    /**
     * Create heavier, faster rain particles for STORMY weather.
     */
    function createStormParticles() {
        if (!weatherParticles) return;
        var dropCount = 120;
        var fragment = document.createDocumentFragment();

        for (var i = 0; i < dropCount; i++) {
            var drop = document.createElement('div');
            var isHeavy = i % 3 === 0;
            drop.className = 'rain-drop ' + (isHeavy ? 'rain-drop--storm-heavy' : 'rain-drop--storm');

            var left = Math.random() * 110 - 5; // wider spread for angled rain
            drop.style.left = left + '%';

            var speed = isHeavy
                ? 0.35 + Math.random() * 0.15
                : 0.4 + Math.random() * 0.2;
            var opacity = isHeavy ? 0.9 : 0.7;

            drop.style.setProperty('--drop-speed', speed + 's');
            drop.style.setProperty('--drop-delay', '-' + (Math.random() * speed).toFixed(2) + 's');
            drop.style.setProperty('--drop-opacity', opacity.toString());

            fragment.appendChild(drop);
        }

        weatherParticles.appendChild(fragment);
    }

    /**
     * Create heat haze floating lines for HEATWAVE weather.
     */
    function createHeatParticles() {
        if (!weatherParticles) return;
        var lineCount = 8;
        var fragment = document.createDocumentFragment();

        for (var i = 0; i < lineCount; i++) {
            var line = document.createElement('div');
            line.className = 'heat-haze-line';

            // Spread lines vertically across the viewport
            var top = 10 + (i / lineCount) * 80;
            line.style.top = top + '%';

            var speed = 2.5 + Math.random() * 2;
            var delay = Math.random() * speed;
            line.style.setProperty('--haze-speed', speed + 's');
            line.style.setProperty('--haze-delay', '-' + delay.toFixed(2) + 's');

            fragment.appendChild(line);
        }

        weatherParticles.appendChild(fragment);
    }

    /**
     * Apply particle effects for the given weather type.
     */
    function applyParticles(weather) {
        clearParticles();
        switch (weather) {
            case 'RAINY':
                createRainParticles();
                break;
            case 'STORMY':
                createStormParticles();
                break;
            case 'HEATWAVE':
                createHeatParticles();
                break;
            // CLEAR: no particles needed
        }
    }

    /**
     * Update the weather display, effects indicator, and overlay animation.
     * Handles fade transitions when weather changes.
     */
    function updateWeather(weather, temp) {
        if (!weather) return;

        var config = WEATHER_CONFIG[weather];
        if (!config) return;

        var previousWeather = currentWeather;
        currentWeather = weather;

        // Update text display
        var tempStr = temp ? ' ' + temp.toFixed(1) + '\u00B0C' : '';
        if (weatherIcon) weatherIcon.textContent = config.icon;
        if (weatherLabel) weatherLabel.textContent = config.label + tempStr;

        // Update effects indicator
        if (weatherEffects) {
            weatherEffects.textContent = config.effects;
            weatherEffects.className = 'weather-display__effects ' + config.effectClass;
        }

        // Transition overlay
        if (weatherOverlay) {
            if (previousWeather) {
                // Fade out old, then fade in new
                weatherOverlay.classList.add('weather-overlay--transitioning');
                setTimeout(function () {
                    applyOverlayClass(config.overlayClass);
                    applyParticles(weather);
                    weatherOverlay.classList.remove('weather-overlay--transitioning');
                }, 400);
            } else {
                // First load, just apply
                applyOverlayClass(config.overlayClass);
                applyParticles(weather);
            }
        }
    }

    function applyOverlayClass(newClass) {
        if (!weatherOverlay) return;
        weatherOverlay.className = 'weather-overlay ' + newClass;
    }

    /**
     * Initialize weather from server-rendered data attribute.
     */
    function init() {
        var displayEl = document.querySelector('.weather-display');
        if (displayEl && displayEl.dataset.weather) {
            updateWeather(displayEl.dataset.weather);
        }
    }

    init();

    // Expose globally
    window.GameWeather = {
        update: updateWeather
    };

})();
