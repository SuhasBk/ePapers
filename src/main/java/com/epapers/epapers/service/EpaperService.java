package com.epapers.epapers.service;

import com.epapers.epapers.model.Edition;
import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.model.HTPage;
import com.epapers.epapers.service.downloader.HTDownload;
import com.epapers.epapers.service.downloader.KPDownload;
import com.epapers.epapers.service.downloader.PDFDownloader;
import com.epapers.epapers.service.downloader.TOIDownload;
import com.epapers.epapers.util.AppUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.net.http.HttpClient;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class EpaperService {
    private final EmailService emailService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EpaperService(EmailService emailService, HttpClient httpClient, ObjectMapper objectMapper) {
        this.emailService = emailService;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    private static final String HT_BASE_URL = "https://epaper.hindustantimes.com";
    private static final String HT_EDITIONS_URL = "https://epaper.hindustantimes.com/Home/GetEditionList";

    public List<Edition> getHTEditionList() {
        log.info("Accessing HT url : {}", HT_EDITIONS_URL);
        Edition[] editions = AppUtils.fetchHttpResponse(this.httpClient, this.objectMapper, HT_EDITIONS_URL, Edition[].class);

        if(editions == null || editions.length == 0) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "I am guessing yesterday was a holiday for journalists, they must be chilling today, try tomorrow!\n\nIf it still ain't working, then adiós, we are done here...");
        }

        return Arrays.stream(editions).toList();
    }

    public List<Edition> getTOIEditionList() {
        ObjectMapper objectMapper = new ObjectMapper();
        String editions = AppUtils.TOI_EDITIONS;
        List<Edition> toiEditions = null;
        try {
            toiEditions = Arrays.asList(objectMapper.readValue(editions, Edition[].class));
        } catch (JsonProcessingException e) {
            log.error("JSON Parsing failed - {}", e.getMessage());
        }
        log.info("TOI Edition list: {}", toiEditions);
        return toiEditions;
    }

    public List<String> getAllEditions() {
        List<String> ht = getHTEditionList().stream().map(Edition::getEditionName).collect(Collectors.toList());
        List<String> toi = getTOIEditionList().stream().map(Edition::getEditionName).toList();

        toi.forEach(edition -> {
            if (!ht.contains(edition)) {
                ht.add(edition);
            }
        });
        return ht;
    }

    public List<String> getHTSupplementEditions(String mainEdition, String date) {
        List<String> editions = new ArrayList<>();
        log.info("Called getHTSupplementEditions with edition: {} and date: {}", mainEdition, date);

        String url = String.format("%1$s/Home/GetAllSupplement?edid=%2$s&EditionDate=%3$s", HT_BASE_URL, mainEdition, date);
        Edition[] htSupplements = AppUtils.fetchHttpResponse(httpClient, objectMapper, url, Edition[].class);

        if(htSupplements != null && htSupplements.length != 0) {
            editions = Arrays.stream(htSupplements).toList()
                    .stream()
                    .map(Edition::getEditionId)
                    .toList();
        }
        return editions;
    }

    public Map<String, String> getEditionFromCity(String city) {
        Map<String, String> editions = new HashMap<>();
        List<Edition> toiEditions = getTOIEditionList()
                .stream()
                .filter(edition -> edition.getEditionName().toUpperCase().contains(city))
                .toList();
        
        if (!toiEditions.isEmpty()) {
            editions.put("TOI", toiEditions.get(0).getEditionId());
        }

        List<Edition> htEditions = getHTEditionList()
                .stream()
                .filter(edition -> edition.getEditionName().toUpperCase().contains(city))
                .toList();
        
        if (!htEditions.isEmpty()) {
            editions.put("HT", htEditions.get(0).getEditionId());
        }

        return editions;
    }

    public List<String> getPages(String edition, String date) {
        List<String> links = new ArrayList<>();
        log.info("Called getPages with edition: {} and date: {}", edition, date);
        String url = String.format("%1$s/Home/GetAllpages?editionid=%2$s&editiondate=%3$s", HT_BASE_URL, edition, date);
        HTPage[] htPages = AppUtils.fetchHttpResponse(httpClient, objectMapper, url, HTPage[].class);

         if(htPages != null && htPages.length != 0) {
             Arrays.stream(htPages).toList().forEach(page -> links.add(page.getHighResolution().replace("_mr", "")));
         }

        return links;
    }

    public Map<String, Object> getKannadaPrabha() throws Exception {
        PDFDownloader downloader = new PDFDownloader(httpClient, objectMapper);
        downloader.setDownloadStrategy(new KPDownload());
        return downloader.getPDF("", "");
    }

    public void mailPDF(String emailId, String mainEdition, String date, String publication) {
        new Thread(() -> {
            Epaper epaper;
            try {
                PDFDownloader downloader = new PDFDownloader(httpClient, objectMapper);
                if(publication.equals("HT")) {
                    downloader.setDownloadStrategy(new HTDownload());
                } else {
                    downloader.setDownloadStrategy(new TOIDownload());
                }
                epaper = (Epaper) downloader.getPDF(mainEdition, date).get("epaper");
                // AppUtils.compressPDF(epaper);
                emailService.mailPDF(emailId, epaper);
            } catch (Exception e) {
                log.error("EMAIL OPERATION FAILED - {}", e.getMessage());
            }
        }).start();
    }

    public FileSystemResource getFile(String fileName) {
        File requestedFile = new File(fileName);
        if (!requestedFile.exists() || !requestedFile.getName().endsWith("pdf")) {
            return new FileSystemResource("null");
        }
        return new FileSystemResource(requestedFile);
    }
}