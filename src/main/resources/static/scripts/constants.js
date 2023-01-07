var getFormattedDate = (date) => {
    return date.getFullYear() + '-' + (date.getMonth() + 1).toString().padStart(2, '0') + '-' + (date.getDate().toString().padStart(2, '0'));
}

var BACKEND_URL = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':8000' : '');

var today = new Date();
var firstDay = new Date(today.getFullYear(), today.getMonth() - 1, 1);
var FIRST_DAY = getFormattedDate(firstDay);
var LAST_DAY = getFormattedDate(today);

var IS_MOBILE = /iPhone|iPad|iPod|Android|Opera Mini/i.test(navigator.userAgent);

var showErrors = (errorMessage) => {
    let errorDiv = document.querySelector("#error-alert");
    errorDiv.removeAttribute("hidden", false);
    errorDiv.textContent = errorMessage;
    setTimeout(() => {
        errorDiv.setAttribute("hidden", true);
    }, 5000);
}