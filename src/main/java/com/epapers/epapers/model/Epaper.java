package com.epapers.epapers.model;

import java.io.File;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString
@NoArgsConstructor
public class Epaper {
    String edition;
    File file;
    String date;
    
    public Epaper(String date, String edition) {
        this.date = date.replaceAll("/", "_");
        this.edition = edition;
        this.file = new File(date.replaceAll("/", "_") + "_" + edition + ".pdf");
    }
}
