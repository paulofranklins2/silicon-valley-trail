(function () {
    'use strict';

    var TOAST_DURATION = 2500;
    var toastContainer = document.getElementById('toast-container');
    var currentToast = null;
    var currentToastTimer = null;

    function dismissCurrentToast(callback) {
        if (currentToastTimer) {
            clearTimeout(currentToastTimer);
            currentToastTimer = null;
        }
        if (currentToast && currentToast.parentNode) {
            currentToast.classList.remove('toast--visible');
            currentToast.classList.add('toast--exiting');
            var old = currentToast;
            setTimeout(function () {
                if (old.parentNode) old.parentNode.removeChild(old);
                if (callback) callback();
            }, 260);
            currentToast = null;
        } else {
            currentToast = null;
            if (callback) callback();
        }
    }

    function showToast(html, type, duration) {
        type = type || 'neutral';
        duration = duration || TOAST_DURATION;

        // Immediately remove any existing toast
        if (currentToast && currentToast.parentNode) {
            if (currentToastTimer) {
                clearTimeout(currentToastTimer);
                currentToastTimer = null;
            }
            currentToast.parentNode.removeChild(currentToast);
            currentToast = null;
        }

        var toast = document.createElement('div');
        toast.className = 'toast toast--' + type;
        toast.innerHTML = html;
        toastContainer.appendChild(toast);
        currentToast = toast;

        // Trigger enter animation with slight delay for smoothness
        requestAnimationFrame(function () {
            requestAnimationFrame(function () {
                toast.classList.add('toast--visible');
            });
        });

        // Auto-remove after duration
        currentToastTimer = setTimeout(function () {
            if (toast.parentNode) {
                toast.classList.remove('toast--visible');
                toast.classList.add('toast--exiting');
                setTimeout(function () {
                    if (toast.parentNode) toast.parentNode.removeChild(toast);
                    if (currentToast === toast) currentToast = null;
                }, 300);
            }
            currentToastTimer = null;
        }, duration);
    }

    function escapeHtml(str) {
        var div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    // Expose globally for other modules
    window.GameToast = {
        show: showToast,
        dismiss: dismissCurrentToast,
        escapeHtml: escapeHtml,
        TOAST_DURATION: TOAST_DURATION
    };

})();
