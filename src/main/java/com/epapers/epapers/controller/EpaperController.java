package com.epapers.epapers.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
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
import com.epapers.epapers.model.EpapersUser;
import com.epapers.epapers.service.EpaperService;
import com.epapers.epapers.service.UserService;
import com.epapers.epapers.telegram.EpapersBot;
import com.epapers.epapers.util.AppUtils;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class EpaperController {
    
    @Autowired
    EpaperService ePaperService;

    @Autowired
    UserService userService;

    @Autowired
    EpapersBot telegramBot;

    @GetMapping("/getEditionList")
    public List<Edition> getHTEditionList(@RequestParam("publication") String publication) throws Exception {
        if(publication.equals("TOI")) {
            return ePaperService.getTOIEditionList();
        }
        return ePaperService.getHTEditionList();
    }

    @GetMapping("/getTOIEditionList")
    public List<Edition> getTOIEditionList() {
        return ePaperService.getTOIEditionList();
    }

    @GetMapping("/getHTSupplementEditions")
    public List<String> getHTSupplementEditions(@RequestParam("mainEdition") String mainEdition, @RequestParam("editionDate") Optional<String> date) {
        return ePaperService.getHTSupplementEditions(mainEdition, date.orElse(AppUtils.getTodaysDate()));
    }

    @GetMapping("/getHTPages")
    public List<String> getPages(@RequestParam("mainEdition") String mainEdition, @RequestParam("editionDate") Optional<String> date) {
        List<String> pagesLinks = new ArrayList<>();
        String todayDate = AppUtils.getTodaysDate();
        List<String> editions = ePaperService.getHTSupplementEditions(mainEdition, date.orElse(todayDate));
        editions.forEach(edition -> {
            try {
                pagesLinks.addAll(ePaperService.getPages(edition, date.orElse(todayDate)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return pagesLinks;
    }

    @PostMapping("/getPDF")
    public String getPDF(@RequestBody Map<String, Object> payload, HttpServletRequest request) throws Exception {
        String todaysDate = AppUtils.getTodaysDate();
        Epaper pdfDocument;
        String emailId = (String) payload.get("userEmail");
        String mainEdition = (String) payload.get("mainEdition");
        String date = (String) Optional.ofNullable(payload.get("date")).orElse(todaysDate);
        String publication = (String) Optional.ofNullable(payload.get("publication")).orElse(null);
        String ipAddress = request.getHeader("X-FORWARDED-FOR") != null ? request.getHeader("X-FORWARDED-FOR") : request.getRemoteAddr();

        EpapersUser epapersUser = new EpapersUser(ipAddress.split(",")[0], null, mainEdition, mainEdition, todaysDate + new Date().getTime(), 1);
        if (!userService.canAccess(epapersUser)) {
            throw new ResponseStatusException(401,"Access denied ❌. Quota Exceeded.", null);
        }
        
        if(publication == null) {
            throw new ResponseStatusException(400, "Missing 'publication' in payload", null);
        }

        if(publication.equals("HT")) {
            pdfDocument = (Epaper) ePaperService.getHTpdf(mainEdition, date).get("epaper");
        } else {
            pdfDocument = (Epaper) ePaperService.getTOIpdf(mainEdition, date).get("epaper");
        }

        /* ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(pdfDocument.getFile().toPath()));
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + pdfDocument.getFile().getName());
        ResponseEntity<ByteArrayResource> response = ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource); */
        
        if (emailId != null && !emailId.isEmpty()) {
            ePaperService.mailPDF(emailId, mainEdition, date, publication);
        }
        return pdfDocument.getFile().getName();
    }

    @GetMapping("/file")
    public ResponseEntity<FileSystemResource> getFile(@RequestParam("name") String fileName) {
        File requestedFile = new File(fileName);

        if (!requestedFile.exists() || !requestedFile.getName().endsWith("pdf")) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(requestedFile);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + requestedFile.getName());

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @GetMapping("/epapers/refreshDB")
    public String refreshDB() {
        userService.refreshDB();
        return "done";
    }

    @GetMapping("/trigger")
    public ResponseEntity<String> trigger() {
        telegramBot.triggerSubscriptions();
        return ResponseEntity.ok().body("triggered!");
    }

    // @GetMapping("/internal_testing")
    // public String test() throws Exception {
    //     Epaper pdfDocument = (Epaper) ePaperService.getKannadaPrabha().get("epaper");
    //     return pdfDocument.getFile().getName();
    // }
}
