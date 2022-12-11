import {Button, Dropdown} from 'react-bootstrap';
import Form from "react-bootstrap/Form";
import {BACKEND_URL, FIRST_DAY, LAST_DAY, IS_MOBILE} from '../constants';
import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router';
import Loading from '../spinner/Spinner';

function Home() {

    const [spinnerHidden, setSpinnerHidden] = useState(true);
    const [publication, setPublication] = useState("Select your publication");
    const [cities, setCities] = useState([]);
    const [selectedEdition, setEdition] = useState("");
    const [dropDownTitle, setDropDownTitle] = useState("Select your city");
    
    // const navigator = useNavigate();

    let choosePublication = (e) => {
        let publication = e.target.getAttribute("value");
        if (publication === "" || publication === null) return;
        setPublication(publication);
        axios.get(`${BACKEND_URL}/api/getEditionList?&publication=${publication}`)
        .then((response) => {
            setCities(response.data);
            setEdition(publication === "TOI" ? "toibgc" : "102");
            setDropDownTitle('Bengaluru');
        });
    }

    let chooseEdition = (e) => {
        let mainEdition = e.target.getAttribute("value");
        if(mainEdition === "" || mainEdition === null) return;
        setEdition(publication === "HT" ? Number.parseInt(mainEdition).toString() : mainEdition);
        setDropDownTitle(e.target.innerHTML);
    }

    let download = () => {
        if(!selectedEdition) {
            alert("Choose your city!");
            return;
        }

        let selectedDate = document.getElementById("editionDate").value;
        let userEmail = document.getElementById("userEmail").value;

        if(selectedDate === "") {
            alert("Please pick a proper date");
            return;
        } else {
            selectedDate = selectedDate.split("-").reverse().join("-").replaceAll("-", "/");
        }

        setSpinnerHidden(false);
        document.getElementById("downloadButton").setAttribute("disabled", true);

        axios.post(`${BACKEND_URL}/api/getPDF`, {
            userEmail: userEmail,
            mainEdition: selectedEdition,
            date: selectedDate,
            publication: publication
        }, 
        // { responseType: 'arraybuffer' }
        ).then(response => {
                document.getElementById("downloadButton").removeAttribute("disabled");
                setSpinnerHidden(true);
                window.location.href = `${BACKEND_URL}/api/file?name=${response.data}`;
                // var file = new Blob([response.data], { type: 'application/pdf' });
                // var fileURL = URL.createObjectURL(file);
                // if(IS_MOBILE) {
                //     var anchor = document.createElement('a');
                //     anchor.href = fileURL;
                //     anchor.click();
                // } else {
                //     navigator("/view", { state: { src: fileURL } });
                // }
            }, err => {
                let errMessage = err.response.data?.message;
                document.getElementById("downloadButton").removeAttribute("disabled");
                console.log(err.response);
                setSpinnerHidden(true);
                alert(errMessage ? errMessage : "Something went wrong. Try again later.");
            });
    }

    return ( 
        <>
            <h1>Times Epaper</h1>
            <div id="editionPicker">
                <p>Pick publication</p>

                <Dropdown>
                    <Dropdown.Toggle id="dropdown-cities" variant="secondary" >
                        {publication}
                    </Dropdown.Toggle>

                    <Dropdown.Menu>
                        <Dropdown.Item key="0" value="HT" onClick={(e) => choosePublication(e)}>Hindustan Times</Dropdown.Item>
                        <Dropdown.Item key="1" value="TOI" onClick={(e) => choosePublication(e)}>Times Of India</Dropdown.Item>
                    </Dropdown.Menu>
                </Dropdown>

                <p style={{ marginTop: '2%' }}>Pick your edition:</p>
                <Dropdown>
                    <Dropdown.Toggle id="dropdown-cities" variant="secondary" >
                        {dropDownTitle}
                    </Dropdown.Toggle>

                    <Dropdown.Menu>
                        {cities.map((edition) => (
                            <Dropdown.Item key={edition.editionId} value={edition.editionId} onClick={(e) => chooseEdition(e)}>{edition.editionName}</Dropdown.Item>
                        ))}
                    </Dropdown.Menu>
                </Dropdown>
                
                <div style={{marginTop: '2%'}}>
                    <Form.Control
                        type='date' 
                        id='editionDate' 
                        min={FIRST_DAY} max={LAST_DAY} 
                        defaultValue={LAST_DAY}>
                    </Form.Control>
                </div>

                <div style={{marginTop: '2%'}}>
                    <Form.Control
                        type='email'
                        id='userEmail'
                        placeholder='Attachment Email (optional)'>
                    </Form.Control>
                </div>

                <Button id="downloadButton" style={{ border: "none", marginTop: "2%" }} variant="primary" onClick={() => download()}>
                    Download&nbsp;
                    {!spinnerHidden && <Loading></Loading>}
                </Button>
            </div>
        </>
    );
}

export default Home;