package com.epapers.epapers.controller;

import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.service.EpaperService;
import com.epapers.epapers.telegram.EpapersBot;
import com.epapers.epapers.util.AppUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class EpaperController {
    
    @Autowired
    EpaperService ePaperService;

    @Autowired
    EpapersBot telegramBot;

    @GetMapping("/getEditionList")
    public List<Object> getHTEditionList(@RequestParam("publication") String publication) throws Exception {
        return switch (publication) {
            case "TOI" ->
                    Collections.singletonList(ePaperService.getTOIEditionList());
            case "HT" ->
                    Collections.singletonList(ePaperService.getHTEditionList());
            default ->
                    Collections.singletonList(ePaperService.getAllEditions());
        };
    }

    @PostMapping("/getPDF")
    public String getPDF(@RequestBody Map<String, Object> payload) throws Exception {
        String todaysDate = AppUtils.getTodaysDate();
        Epaper pdfDocument;
        String emailId = (String) payload.get("userEmail");
        String mainEdition = (String) payload.get("mainEdition");
        String date = (String) Optional.ofNullable(payload.get("date")).orElse(todaysDate);
        String publication = (String) payload.get("publication");
        
        if(publication == null) {
            throw new ResponseStatusException(HttpStatus.SC_UNPROCESSABLE_ENTITY, "Missing 'publication' in payload", null);
        }

        if(publication.equals("HT")) {
            pdfDocument = (Epaper) ePaperService.getHTpdf(mainEdition, date).get("epaper");
        } else {
            pdfDocument = (Epaper) ePaperService.getTOIpdf(mainEdition, date).get("epaper");
        }
        
        if (emailId != null && !emailId.isEmpty()) {
            ePaperService.mailPDF(emailId, mainEdition, date, publication);
        }
        return pdfDocument.getFile().getName();
    }

    @GetMapping("/file")
    public ResponseEntity<FileSystemResource> getFile(@RequestParam("name") String fileName, HttpServletRequest request) {
        log.info("{} is trying to access the resource: {}", AppUtils.getIPAddr(request), fileName);
        FileSystemResource resource = ePaperService.getFile(fileName);

        if(!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + resource.getFile().getName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/trigger")
    public ResponseEntity<String> trigger() {
        telegramBot.triggerSubscriptions(false);
        return ResponseEntity.ok().body("triggered!");
    }
}
