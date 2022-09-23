import { useLocation } from "react-router";

function PdfViewer() {
    const location = useLocation();

    if (!location?.state && !location?.state?.src) {
        return (
            <>
                Um, this is not the place you want to be right now 🫤🫠<br></br>
                <a href="/">Go Home?</a>
            </>
        )
    }

    return (
        <>
            <a style={{display: "block"}} href={location.state.src}>View fullscreen</a>
            <iframe
                id="pdfViewer"
                name="pdfViewer"
                style={{width: "70vw", height: "80vh"}}
                src={location.state.src}>
            </iframe>
        </>
    );
}

export default PdfViewer;