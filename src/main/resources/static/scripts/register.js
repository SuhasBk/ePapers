fetch(`${BACKEND_URL}/api/getEditionList?&publication=ALL`)
.then((response) => response.json())
.then(data => {
    data[0].forEach((city) => {
        let select = document.querySelector("#city");
        let option = document.createElement("option");
        option.text = city;
        option.value = city;
        select.appendChild(option);
    });
});

document.querySelector("#sign-up").addEventListener('click', () => {
    let username = document.querySelector("#username").value;
    let password = document.querySelector("#password").value;
    let email = document.querySelector("#email").value;
    let city = document.querySelector("#city").value;

    if(!username || !password || !email || !city) {
        return;
    }

    fetch(`${BACKEND_URL}/api/register`, {
        headers: {
            'Content-Type': 'application/json'
        },
        method: 'POST',
        body: JSON.stringify({
            username: username,
            password: password,
            email: email,
            city: city
        })
    })
    .then(response => {
        if(response.ok) {
            return response.json()
        } else {
            return Promise.reject(response);
        }
    })
    .then(response => {
        if(response.status == 'true') {
            window.location.href = `${BACKEND_URL}/signin.html`;
        } else {
            showErrors(response.error);
        }
    })
    .catch(response => {
        console.log(response);
        response.json().then(err => {
            showErrors("Something went wrong. Try again later.");
        });
    });
});