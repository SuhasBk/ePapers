package com.epapers.epapers.controller;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.epapers.epapers.model.Edition;
import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.service.EpaperService;
import com.epapers.epapers.util.AppUtils;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class EpaperController {
    
    @Autowired
    EpaperService service;

    @Autowired
    String TODAYS_DATE;

    @GetMapping("/getEditionList")
    public List<Edition> getHTEditionList(@RequestParam("publication") String publication) throws Exception {
        if(publication.equals("TOI")) {
            return service.getTOIEditionList();
        }
        return service.getHTEditionList();
    }

    @GetMapping("/getTOIEditionList")
    public List<Edition> getTOIEditionList() throws Exception {
        return service.getTOIEditionList();
    }

    @GetMapping("/getHTSupplementEditions")
    public List<String> getHTSupplementEditions(@RequestParam("mainEdition") String mainEdition, @RequestParam("editionDate") Optional<String> date) {
        return service.getHTSupplementEditions(mainEdition, date.orElse(TODAYS_DATE));
    }

    @GetMapping("/getHTPages")
    public List<String> getPages(@RequestParam("mainEdition") String mainEdition, @RequestParam("editionDate") Optional<String> date) {
        List<String> pagesLinks = new ArrayList<>();
        List<String> editions = service.getHTSupplementEditions(mainEdition, date.orElse(TODAYS_DATE));
        editions.forEach(edition -> {
            try {
                pagesLinks.addAll(service.getPages(edition, date.orElse(TODAYS_DATE)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return pagesLinks;
    }

    @PostMapping("/getPDF")
    public ResponseEntity<ByteArrayResource> getPDF(@RequestBody Map<String, Object> payload) throws Exception {
        Epaper pdfDocument = new Epaper();
        String emailId = (String) payload.get("userEmail");
        String mainEdition = (String) payload.get("mainEdition");
        String date = (String) Optional.ofNullable(payload.get("date")).orElse(TODAYS_DATE);
        String publication = (String) Optional.ofNullable(payload.get("publication")).orElse(null);       
        
        if(publication == null) {
            throw new ResponseStatusException(400, "Missing 'publication' in payload", null);
        }

        if(publication.equals("HT")) {
            pdfDocument = (Epaper) service.getHTpdf(mainEdition, date).get("epaper");
        } else {
            pdfDocument = (Epaper) service.getTOIpdf(mainEdition, date).get("epaper");
        }

        ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(pdfDocument.getFile().toPath()));

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + pdfDocument.getFile().getName());

        ResponseEntity<ByteArrayResource> response = ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);

        if (emailId != null && !emailId.isEmpty()) {
            service.mailPDF(emailId, mainEdition, date, publication);
        } else {
            AppUtils.deleteFile(pdfDocument.getFile());
        }
        return response;
    }
}
