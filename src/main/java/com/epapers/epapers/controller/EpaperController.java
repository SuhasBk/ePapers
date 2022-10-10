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

import com.epapers.epapers.EpapersApplication;
import com.epapers.epapers.model.Edition;
import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.model.EpapersUser;
import com.epapers.epapers.service.EpaperService;
import com.epapers.epapers.service.UserService;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class EpaperController {
    
    @Autowired
    EpaperService ePaperService;

    @Autowired
    UserService userService;

    @GetMapping("/getEditionList")
    public List<Edition> getHTEditionList(@RequestParam("publication") String publication) throws Exception {
        if(publication.equals("TOI")) {
            return ePaperService.getTOIEditionList();
        }
        return ePaperService.getHTEditionList();
    }

    @GetMapping("/getTOIEditionList")
    public List<Edition> getTOIEditionList() throws Exception {
        return ePaperService.getTOIEditionList();
    }

    @GetMapping("/getHTSupplementEditions")
    public List<String> getHTSupplementEditions(@RequestParam("mainEdition") String mainEdition, @RequestParam("editionDate") Optional<String> date) {
        String TODAYS_DATE = EpapersApplication.getTodaysDate();
        return ePaperService.getHTSupplementEditions(mainEdition, date.orElse(TODAYS_DATE));
    }

    @GetMapping("/getHTPages")
    public List<String> getPages(@RequestParam("mainEdition") String mainEdition, @RequestParam("editionDate") Optional<String> date) {
        List<String> pagesLinks = new ArrayList<>();
        String TODAYS_DATE = EpapersApplication.getTodaysDate();
        List<String> editions = ePaperService.getHTSupplementEditions(mainEdition, date.orElse(TODAYS_DATE));
        editions.forEach(edition -> {
            try {
                pagesLinks.addAll(ePaperService.getPages(edition, date.orElse(TODAYS_DATE)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return pagesLinks;
    }

    @PostMapping("/getPDF")
    public String getPDF(@RequestBody Map<String, Object> payload, HttpServletRequest request) throws Exception {
        String TODAYS_DATE = EpapersApplication.getTodaysDate();
        Epaper pdfDocument = new Epaper();
        String emailId = (String) payload.get("userEmail");
        String mainEdition = (String) payload.get("mainEdition");
        String date = (String) Optional.ofNullable(payload.get("date")).orElse(TODAYS_DATE);
        String publication = (String) Optional.ofNullable(payload.get("publication")).orElse(null);
        String IP = request.getHeader("X-FORWARDED-FOR") != null ? request.getHeader("X-FORWARDED-FOR") : request.getRemoteAddr();

        EpapersUser epapersUser = new EpapersUser(IP.split(",")[0], null, mainEdition, mainEdition, TODAYS_DATE + new Date().getTime(), 1);
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

        // ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(pdfDocument.getFile().toPath()));
        // HttpHeaders headers = new HttpHeaders();
        // headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + pdfDocument.getFile().getName());
        // ResponseEntity<ByteArrayResource> response = ResponseEntity.ok()
        //         .headers(headers)
        //         .contentType(MediaType.APPLICATION_OCTET_STREAM)
        //         .body(resource);
        
        if (emailId != null && !emailId.isEmpty()) {
            ePaperService.mailPDF(emailId, mainEdition, date, publication);
        }
        return pdfDocument.getFile().getName();
    }

    @GetMapping("/file")
    public ResponseEntity<FileSystemResource> getFile(@RequestParam("name") String fileName) throws Exception{
        File requestedFile = new File(fileName);

        if (!requestedFile.exists() || !requestedFile.getName().endsWith("pdf")) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(requestedFile);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=" + requestedFile.getName());

        ResponseEntity<FileSystemResource> response = ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);

        return response;
    }

    @GetMapping("/epapers/refreshDB")
    public String refreshDB() {
        userService.refreshDB();
        return "done";
    }
}
