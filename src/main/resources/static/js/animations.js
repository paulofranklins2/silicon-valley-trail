(function () {
    'use strict';

    var stage = document.getElementById('action-stage');
    var scene = document.getElementById('action-scene');
    var SCENE_DURATION = 2500;
    var activeTimeout = null;

    // Helper: create an img element
    function img(src, classes) {
        var el = document.createElement('img');
        el.src = src;
        el.alt = '';
        el.className = classes || '';
        return el;
    }

    // Helper: create a text span
    function span(text, classes) {
        var el = document.createElement('span');
        el.textContent = text;
        el.className = classes || '';
        return el;
    }

    // Helper: create a div
    function div(classes) {
        var el = document.createElement('div');
        el.className = classes || '';
        return el;
    }

    // Team character image paths
    var TEAM = [
        '/img/team-member-1.png',
        '/img/team-member-2.png',
        '/img/team-member-3.png',
        '/img/team-member-4.png'
    ];

    var VC_INVESTORS = [
        '/img/vc-investor-1.png',
        '/img/vc-investor-2.png'
    ];

    // -- Scene builders --

    function buildTravelScene(result) {
        // City labels: current location → next location
        var fromCity = '???';
        var toCity = '???';

        if (result && result.journeyState) {
            var js = result.journeyState;
            var locations = js.locations || [];
            var idx = js.currentLocationIndex || 0;
            fromCity = locations[idx]
                ? (locations[idx].name || locations[idx]) : fromCity;
            toCity = (locations[idx + 1])
                ? (locations[idx + 1].name || locations[idx + 1]) : toCity;
        }

        var labelFrom = span(fromCity, 'scene-label scene-label--left');
        var labelTo = span(toCity, 'scene-label scene-label--right');

        // Road
        var road = div('scene-road');

        // Train group with riders
        var group = div('scene-travel-group');
        TEAM.forEach(function (src) {
            group.appendChild(img(src, 'scene-sprite scene-sprite--char'));
        });
        group.appendChild(img('/img/train.png', 'scene-sprite scene-sprite--train'));

        scene.appendChild(labelFrom);
        scene.appendChild(labelTo);
        scene.appendChild(road);
        scene.appendChild(group);
    }

    function buildRestScene() {
        var label = span('Break Time', 'scene-label scene-label--center');
        scene.appendChild(label);

        // Characters sitting around
        var positions = [18, 34, 50, 66];
        TEAM.forEach(function (src, i) {
            var c = img(src, 'scene-sprite scene-sprite--char scene-rest-char');
            c.style.left = positions[i] + '%';
            c.style.bottom = '20px';
            c.style.top = 'auto';
            c.style.transform = 'none';
            scene.appendChild(c);
        });

        // Coffee cups and pizza
        var coffee1 = img('/img/cup-coffee.png', 'scene-sprite scene-sprite--item scene-rest-item');
        coffee1.style.left = '26%';
        coffee1.style.bottom = '14px';
        coffee1.style.top = 'auto';
        coffee1.style.transform = 'none';

        var coffee2 = img('/img/cup-coffee.png', 'scene-sprite scene-sprite--item scene-rest-item');
        coffee2.style.left = '58%';
        coffee2.style.bottom = '14px';
        coffee2.style.top = 'auto';
        coffee2.style.transform = 'none';

        var pizza = img('/img/pizza.png', 'scene-sprite scene-sprite--item scene-rest-item');
        pizza.style.left = '42%';
        pizza.style.bottom = '10px';
        pizza.style.top = 'auto';
        pizza.style.transform = 'none';

        scene.appendChild(coffee1);
        scene.appendChild(coffee2);
        scene.appendChild(pizza);

        // Floating zzz and + symbols
        var symbols = ['zzz', '+', 'zzz', '+'];
        var symPositions = [22, 38, 54, 70];
        symbols.forEach(function (text, i) {
            var s = span(text, 'scene-float-symbol');
            s.style.left = symPositions[i] + '%';
            s.style.bottom = '60px';
            s.style.top = 'auto';
            s.style.animationDelay = (i * 0.5) + 's';
            scene.appendChild(s);
        });
    }

    function buildScavengeScene(result) {
        var label = span('Scavenging...', 'scene-label scene-label--center');
        scene.appendChild(label);

        // Single searcher character in center
        var searcher = img(TEAM[0], 'scene-sprite scene-sprite--char scene-scavenge-searcher');
        searcher.style.left = '45%';
        searcher.style.width = '42px';
        searcher.style.height = '42px';
        scene.appendChild(searcher);

        // Determine result: food or cash
        var gotFood = false;
        var gotCash = false;
        var outcome = result && result.lastTurnResult && result.lastTurnResult.actionOutcome;
        if (outcome === 'FOOD') gotFood = true;
        if (outcome === 'CASH') gotCash = true;

        // Show result item
        var resultItem;
        if (gotFood) {
            resultItem = img('/img/apple.png', 'scene-sprite scene-sprite--item scene-scavenge-result');
        } else if (gotCash) {
            resultItem = span('$', 'scene-sprite scene-scavenge-result');
            resultItem.style.fontFamily = 'var(--font-arcade)';
            resultItem.style.fontSize = '1.4rem';
            resultItem.style.color = 'var(--green)';
            resultItem.style.textShadow = '0 0 8px rgba(34,197,94,0.5)';
        } else {
            resultItem = img('/img/food-icon.png', 'scene-sprite scene-sprite--item scene-scavenge-result');
        }
        if (resultItem) {
            resultItem.style.left = '58%';
            scene.appendChild(resultItem);
        }

        // Sparkles around result
        setTimeout(function () {
            if (!scene.parentNode) return;
            var sparklePositions = [
                { left: '55%', top: '25%' },
                { left: '62%', top: '35%' },
                { left: '52%', top: '40%' },
                { left: '65%', top: '28%' }
            ];
            sparklePositions.forEach(function (pos, i) {
                var s = div('scene-sparkle');
                s.style.left = pos.left;
                s.style.top = pos.top;
                s.style.animationDelay = (i * 0.15) + 's';
                if (gotCash) s.style.background = 'var(--green)';
                scene.appendChild(s);
            });
        }, 1200);
    }

    function buildHackathonScene() {
        var label = span('Hackathon!', 'scene-label scene-label--center');
        scene.appendChild(label);

        // Characters hunched and typing
        var positions = [15, 33, 51, 69];
        TEAM.forEach(function (src, i) {
            var c = img(src, 'scene-sprite scene-sprite--char scene-hack-char');
            c.style.left = positions[i] + '%';
            scene.appendChild(c);
        });

        // Floating code symbols
        var codeSymbols = ['{}', '</>', '01', 'fn', '{}', '01', '</>', 'fn'];
        var codePositions = [18, 30, 42, 55, 65, 75, 25, 50];
        codeSymbols.forEach(function (text, i) {
            var s = span(text, 'scene-code-symbol');
            s.style.left = codePositions[i] + '%';
            s.style.top = '65%';
            s.style.animationDelay = (i * 0.25) + 's';
            scene.appendChild(s);
        });
    }

    function buildPitchScene(result) {
        var label = span('Pitch Meeting', 'scene-label scene-label--center');
        scene.appendChild(label);

        // Team on the left
        var teamGroup = div('scene-pitch-team');
        TEAM.forEach(function (src) {
            teamGroup.appendChild(img(src, 'scene-sprite scene-sprite--char'));
        });
        scene.appendChild(teamGroup);

        // VS label in the middle
        var vs = span('VS', 'scene-pitch-vs');
        scene.appendChild(vs);

        // VC investors on the right
        var vcGroup = div('scene-pitch-vcs');
        VC_INVESTORS.forEach(function (src) {
            vcGroup.appendChild(img(src, 'scene-sprite scene-sprite--char'));
        });
        scene.appendChild(vcGroup);

        // Determine success/failure after entrance animation
        var pitchSuccess = result && result.lastTurnResult && result.lastTurnResult.actionOutcome === 'PITCH_SUCCESS';

        setTimeout(function () {
            if (!scene.parentNode) return;

            if (pitchSuccess) {
                scene.classList.add('scene-pitch-success');
                vs.textContent = 'DEAL!';

                // Green sparkles
                for (var i = 0; i < 8; i++) {
                    var s = div('scene-pitch-sparkle');
                    s.style.left = (20 + Math.random() * 60) + '%';
                    s.style.top = (20 + Math.random() * 50) + '%';
                    s.style.animation = 'sparkle 0.6s ease-out ' + (i * 0.1) + 's forwards';
                    s.style.background = 'var(--green)';
                    scene.appendChild(s);
                }
            } else {
                scene.classList.add('scene-pitch-fail');
                vs.textContent = 'PASS';
            }
        }, 900);
    }

    // -- Scene dispatcher --

    var SCENE_BUILDERS = {
        'TRAVEL': buildTravelScene,
        'REST': buildRestScene,
        'SCAVENGE': buildScavengeScene,
        'HACKATHON': buildHackathonScene,
        'PITCH_VCS': buildPitchScene
    };

    function playAction(action, result) {
        if (!stage || !scene) return;

        // Clear any running scene
        if (activeTimeout) {
            clearTimeout(activeTimeout);
            cleanup();
        }

        var builder = SCENE_BUILDERS[action];
        if (!builder) return;

        // Clear scene contents and build new scene
        scene.innerHTML = '';
        scene.className = 'action-stage__scene';
        stage.className = 'action-stage action-stage--active';

        builder(result);

        // Fade out after duration
        activeTimeout = setTimeout(function () {
            stage.style.opacity = '0';
            setTimeout(function () {
                cleanup();
                stage.style.opacity = '';
            }, 350);
        }, SCENE_DURATION);
    }

    function cleanup() {
        if (stage) {
            stage.className = 'action-stage';
        }
        if (scene) {
            scene.innerHTML = '';
            scene.className = 'action-stage__scene';
        }
        activeTimeout = null;
    }

    /**
     * Update team member visual states based on current stats.
     */
    function updateTeamStatus(teamState) {
        if (!teamState) return;

        var members = document.querySelectorAll('.team-display__member');
        members.forEach(function (member) {
            member.classList.remove(
                'team-display__member--low-health',
                'team-display__member--low-energy',
                'team-display__member--high-morale'
            );

            if (teamState.health <= 25) {
                member.classList.add('team-display__member--low-health');
            }
            if (teamState.energy <= 25) {
                member.classList.add('team-display__member--low-energy');
            }
            if (teamState.morale >= 80) {
                member.classList.add('team-display__member--high-morale');
            }
        });
    }

    // Initialize team status from DOM on load
    function initTeamStatus() {
        var healthVal = document.querySelector('[data-stat="health"] .stat-row__val');
        var energyVal = document.querySelector('[data-stat="energy"] .stat-row__val');
        var moraleVal = document.querySelector('[data-stat="morale"] .stat-row__val');

        if (healthVal && energyVal && moraleVal) {
            updateTeamStatus({
                health: parseInt(healthVal.textContent) || 0,
                energy: parseInt(energyVal.textContent) || 0,
                morale: parseInt(moraleVal.textContent) || 0
            });
        }
    }

    initTeamStatus();

    // Expose globally
    window.GameAnimations = {
        playAction: playAction,
        updateTeamStatus: updateTeamStatus
    };

})();
