let getFormattedDate = (date) => {
    return date.getFullYear() + '-' + (date.getMonth() + 1).toString().padStart(2, '0') + '-' + (date.getDate().toString().padStart(2, '0'));
}

let BACKEND_URL = window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':8000' : '');

let today = new Date();
let firstDay = new Date(today.getFullYear(), today.getMonth() - 1, 1);
let FIRST_DAY = getFormattedDate(firstDay);
let LAST_DAY = getFormattedDate(today);

let IS_MOBILE = /iPhone|iPad|iPod|Android|Opera Mini/i.test(navigator.userAgent);

export {BACKEND_URL, FIRST_DAY, LAST_DAY, IS_MOBILE};