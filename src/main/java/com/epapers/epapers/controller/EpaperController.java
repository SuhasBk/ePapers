package com.epapers.epapers.controller;

import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.service.EpaperService;
import com.epapers.epapers.service.downloader.HTDownload;
import com.epapers.epapers.service.downloader.PDFDownloader;
import com.epapers.epapers.service.downloader.TOIDownload;
import com.epapers.epapers.telegram.EpapersBot;
import com.epapers.epapers.util.AppUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpStatus;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class EpaperController {

    private final EpaperService ePaperService;
    private final EpapersBot telegramBot;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EpaperController(EpaperService epaperService, EpapersBot epapersBot, HttpClient httpClient, ObjectMapper objectMapper) {
        this.ePaperService = epaperService;
        this.telegramBot = epapersBot;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

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
        
        PDFDownloader downloader = new PDFDownloader(httpClient, objectMapper);
        if(publication.equals("HT")) {
            downloader.setDownloadStrategy(new HTDownload());
        } else {
            downloader.setDownloadStrategy(new TOIDownload());
        }
        
        if (emailId != null && !emailId.isEmpty()) {
            ePaperService.mailPDF(emailId, mainEdition, date, publication);
        }
        
        pdfDocument = (Epaper) downloader.getPDF(mainEdition, date).get("epaper");
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

    @GetMapping("/triggerCache")
    public ResponseEntity<String> triggerCache() {
        telegramBot.triggerSubscriptions(true);
        return ResponseEntity.ok().body("caching triggered!");
    }

    @GetMapping("/kp")
    public ResponseEntity<FileSystemResource> getKP() throws Exception {
        Map<String, Object> res = ePaperService.getKannadaPrabha();
        Epaper paper = (Epaper) res.get("epaper");
        FileSystemResource resource = ePaperService.getFile(paper.getFile().getName());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + resource.getFile().getName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
}
