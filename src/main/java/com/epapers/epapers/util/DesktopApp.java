package com.epapers.epapers.util;

import java.nio.file.Files;
import java.nio.file.Path;

import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.service.EpaperService;

public class DesktopApp {
    public static void download(String publication, String date) {
        EpaperService service = new EpaperService();
        Epaper pdfDocument = new Epaper();
        String edition;
        try {
            if(publication.equals("HT")){
                edition = "102";
                pdfDocument = (Epaper) service.getHTpdf(edition, date).get("epaper");
            } else {
                edition = "toibgc";
                pdfDocument = (Epaper) service.getTOIpdf(edition, date).get("epaper");
            }
            Files.move(Path.of(pdfDocument.getFile().getAbsolutePath()), Path.of(System.getenv("HOME") + "/epaper.pdf"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
