var homeModule = {
    dom: {
        'publication': document.querySelector("#publication"),
        'city': document.querySelector("#city"),
        'date': document.querySelector("#editionDate"),
        'userEmail': document.querySelector("#userEmail"),
        'downloadButton': document.querySelector("#downloadButton"),
        'spinner': document.querySelector("#spinner")
    },

    init: function() {
        this.dom.publication.addEventListener('change', (event) => {
            choosePublication(event);
        });

        this.dom.city.addEventListener('change', (event) => {
            chooseEdition(event);
        });

        this.dom.date.setAttribute("min", FIRST_DAY)
        this.dom.date.setAttribute("max", LAST_DAY)
        this.dom.date.setAttribute("value", LAST_DAY);
    },
}

var choosePublication = (e) => {
    homeModule.dom.city.innerHTML = "";
    let publication = e.target.value;
    if (publication === "" || publication === null) return;
    homeModule['publication'] = publication;
    fetch(`${BACKEND_URL}/api/getEditionList?&publication=${publication}`)
    .then((response) => response.json())
    .then(data => {
        data[0].forEach((city) => {
            let select = homeModule.dom.city;
            let option = document.createElement("option");
            option.text = city.EditionDisplayName;
            option.value = city.EditionId;

            // default Namma Bengaluru!
            if (option.value == '102' || option.value == 'toibgc') {
                option.selected = true;
                homeModule['edition'] = homeModule['publication'] === "HT" ? Number.parseInt(option.value).toString() : option.value;
            }

            select.appendChild(option);
        });
    });
}

var chooseEdition = (e) => {
    let mainEdition = e.target.value;
    if(mainEdition === "" || mainEdition === null) return;
    homeModule['edition'] = homeModule['publication'] === "HT" ? Number.parseInt(mainEdition).toString() : mainEdition;
}

var download = () => {
    if(!homeModule['edition']) {
        alert("Choose your city!");
        return;
    }

    let selectedDate = homeModule.dom.date.value;
    let userEmail = homeModule.dom.userEmail.value;

    if(selectedDate === "") {
        alert("Please pick a proper date");
        return;
    } else {
        selectedDate = selectedDate.split("-").reverse().join("-").replaceAll("-", "/");
    }

    homeModule.dom.downloadButton.setAttribute("disabled", true);
    homeModule.dom.spinner.style.display = 'inline-block';

    fetch(`${BACKEND_URL}/api/getPDF`, {
        headers: {
            'Content-Type': 'application/json'
        },
        method: 'POST',
        body: JSON.stringify({
            userEmail: userEmail,
            mainEdition: homeModule['edition'],
            date: selectedDate,
            publication: homeModule['publication']
        })
    })
    .then(response => {
        if(response.ok) {
            return response.text()
        } else {
            return Promise.reject(response);
        }
    })
    .then(response => {
            homeModule.dom.downloadButton.removeAttribute("disabled");
            window.location.href = `${BACKEND_URL}/api/file?name=${response}`;
    })
    .catch(response => {
        homeModule.dom.downloadButton.removeAttribute("disabled");
        console.log(response);
        response.json().then(err => {
            alert(err.message.includes('Access denied') ? "Access denied ❌. Quota Exceeded." : "Something went wrong. Try again later.");
        });
    });
}

homeModule.init();