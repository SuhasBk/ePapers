package com.epapers.epapers.service;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.epapers.epapers.model.Edition;
import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.util.AppUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class EpaperService {

    @Autowired
    EmailService emailService;

    private static final String HT_BASE_URL = "https://epaper.hindustantimes.com";
    private static final String HT_EDITIONS_URL = "https://epaper.hindustantimes.com/Home/GetEditionList";
    private static final String TOI_BASE_URL = "https://asset.harnscloud.com/PublicationData/TOI/";
    private static final String TOI_META_URL = TOI_BASE_URL + "%s/%s/%s/%s/DayIndex/%s_%s.json";
    private static final String KP_BASE_URL = "https://kpepaper.asianetnews.com/t/12222";
    private static final String KP_EDITION_LINK = "https://kpepaper.asianetnews.com/download/fullpdflink/newspaper/12222/%s";
    private static final String EPAPER_KEY_STRING = "epaper";

    public List<Edition> getHTEditionList() throws Exception {
        List<Edition> editions = new ArrayList<>();
        List<Map<String, Object>> json = AppUtils.getHTJsonObject(HT_EDITIONS_URL);
        json.forEach(edition -> {
            Edition editionInfo = new Edition(Double.valueOf(edition.get("EditionId").toString()).intValue()+"", edition.get("EditionDisplayName").toString());
            editions.add(editionInfo);
        });
        log.info("HT Edition list: {}", editions);
        return editions;
    }

    public List<Edition> getTOIEditionList() {
        ObjectMapper objectMapper = new ObjectMapper();
        String editions = AppUtils.TOI_EDITIONS;
        List<Edition> toiEditions = null;
        try {
            toiEditions = Arrays.asList(objectMapper.readValue(editions, Edition[].class));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        log.info("TOI Edition list: {}", toiEditions);
        return toiEditions;
    }

    public List<String> getHTSupplementEditions(String mainEdition, String date) {
        List<String> editions = new ArrayList<>();
        log.info("Called getHTSupplementEditions with edition: {} and date: {}", mainEdition, date);
        String url = String.format("%1$s/Home/GetAllSupplement?edid=%2$s&EditionDate=%3$s", HT_BASE_URL, mainEdition, date.replace("/", "%2F"));
        List<Map<String, Object>> json = AppUtils.getHTJsonObject(url);
        json.forEach(edition -> editions.add(edition.get("EditionId").toString()));
        return editions;
    }

    public Map<String, String> getEditionFromCity(String city) throws Exception {
        Map<String, String> editions = new HashMap<>();

        List<Edition> toiEditions = getTOIEditionList()
                .stream()
                .filter(edition -> edition.getEditionName().toUpperCase().contains(city))
                .collect(Collectors.toList());
        
        if (!toiEditions.isEmpty()) {
            editions.put("TOI", toiEditions.get(0).getEditionId());
        }

        List<Edition> htEditions = getHTEditionList()
                .stream()
                .filter(edition -> edition.getEditionName().toUpperCase().contains(city))
                .collect(Collectors.toList());
        
        if (!htEditions.isEmpty()) {
            editions.put("HT", htEditions.get(0).getEditionId());
        }

        return editions;
    }

    public List<String> getPages(String edition, String date) {
        List<String> links = new ArrayList<>();
        log.info("Called getPages with edition: {} and date: {}", edition, date);
        String url = String.format("%1$s/Home/GetAllpages?editionid=%2$s&editiondate=%3$s", HT_BASE_URL, edition, date.replace("/", "%2F"));
        List<Map<String, Object>> json = AppUtils.getHTJsonObject(url);
        json.forEach(page -> links.add(page.get("HighResolution").toString().replace("_mr", "")));
        return links;
    }

    public Epaper getPDF(List<String> links, String edition, String date) throws Exception {
        log.info("Starting downloads for edition: {}", edition);

        // prepare the File object
        Epaper epaper = new Epaper(date.replace("/", "_"), edition);

        File file = epaper.getFile();

        if(file.exists()) {
            log.info("File already exists, skipping download from servers...");
            return epaper;
        }

        // define what the executor should do and return
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Callable<Image>> callableList = new ArrayList<>();
        List<Future<Image>> futureList;
        Document document = new Document();

        PdfWriter.getInstance(document, new FileOutputStream(file));
        document.open();

        links.forEach(imgLink -> callableList.add(() -> {
            Image image = Image.getInstance(new URL(imgLink));
            // scale factor based on publication:
            if(imgLink.contains("harnscloud")) {
                image.scalePercent(27f);    //TOI
            } else {
                image.scalePercent(21f);    //HT
            }
            return image;
        }));

        // let executor handle stuff including exceptions
        futureList = executor.invokeAll(callableList);

        // process returned results in synchronous manner
        try {
            futureList.forEach(img -> {
                try {
                    document.add(img.get());
                    log.info("Downloaded: {} of {}", futureList.indexOf(img) + 1, futureList.size());
                } catch (DocumentException | ExecutionException e) {
                    log.error("Error in fetching image - ", e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            document.close();
            log.info("Finished collecting pages: {}\n", document);
            if(!executor.shutdownNow().isEmpty()) {
                log.error("Some pages might not be added to PDF! :(");
            }
            log.info("File size in MB: {}\n", (file.length() / (1024*1024)));
        }

        return epaper;
    }

    public Map<String, Object> getHTpdf(String mainEdition, String date) throws Exception {
        Map<String, Object> response = new HashMap<>();
        List<String> pagesLinks = new ArrayList<>();
        log.info("Called getHTpdf with edition: {} and date: {}", mainEdition, date);

        List<String> editions = getHTSupplementEditions(mainEdition, date);
        editions.forEach(edition -> {
            try {
                pagesLinks.addAll(getPages(edition, date));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        final Epaper epaper = getPDF(pagesLinks, mainEdition, date);
        response.put(EPAPER_KEY_STRING, epaper);
        return response;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTOIpdf(String mainEdition, String date) throws Exception {
        Map<String, Object> response = new HashMap<>();
        List<String> pagesLinks = new ArrayList<>();
        log.info("Called getTOIpdf with edition: {} and date: {}", mainEdition, date);

        String[] dateSplit = date.split("/");
        String day = dateSplit[0];
        String month = dateSplit[1];
        String year = dateSplit[2];
        String metaUrl = String.format(TOI_META_URL, mainEdition, year, month, day, date.replace("/","_"), mainEdition);

        Map<String, Object> meta = AppUtils.getTOIJsonObject(metaUrl);
        List<Object> metaData = Arrays.asList(meta.get("DayIndex"));
        List<Object> data = (List<Object>) metaData.get(0);

        data.forEach(page -> {
            Map<String, String> pageData = (Map<String, String>) page;
            String pageUrl = String.format("%s%s/%s/%s/%s/Page/%s_%s_%s.jpg", TOI_BASE_URL, mainEdition, year, month, day, date.replace("/", "_"), pageData.get("DisplayPageNumber"), mainEdition);
            pagesLinks.add(pageUrl);
        });

        final Epaper epaper = getPDF(pagesLinks, mainEdition, date);
        response.put(EPAPER_KEY_STRING, epaper);
        return response;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getKannadaPrabha() throws Exception {
        Map<String, Object> response = new HashMap<>();
        Epaper epaper = new Epaper(AppUtils.getTodaysDate(), "BNG");
        String fileDownloadURL = null;

        if(epaper.getFile().exists()) {
            log.info("File already exists, skipping download from servers...");
            response.put(EPAPER_KEY_STRING, epaper);
            return response;
        }

        Elements allDivs = Jsoup.connect(KP_BASE_URL).get().getElementsByClass("papr-card");
        Element todayDiv = Optional.ofNullable(allDivs.first()).orElseThrow().parent();
        String edition = Optional.ofNullable(todayDiv).orElseThrow().attr("href").split("/r/")[1];

        Map<String, Object> kpResponse = AppUtils.getKPJsonObject(String.format(KP_EDITION_LINK, edition));
        if (!(Boolean) kpResponse.get("status")) {
            return null;
        }
        fileDownloadURL = ((Map<String, String>) kpResponse.get("data")).get("fullpdf");
        
        AppUtils.downloadFileFromUrl(Optional.ofNullable(fileDownloadURL).orElseThrow(), epaper.getFile());

        response.put(EPAPER_KEY_STRING, epaper);
        return response;
    }

    public void mailPDF(String emailId, String mainEdition, String date, String publication) {
        new Thread(() -> {
            Epaper epaper = null;
            try {
                if(publication.equals("HT")) {
                    epaper = (Epaper) getHTpdf(mainEdition, date).get(EPAPER_KEY_STRING);
                } else {
                    epaper = (Epaper) getTOIpdf(mainEdition, date).get(EPAPER_KEY_STRING);
                }
                // AppUtils.compressPDF(epaper);
                emailService.mailPDF(emailId, epaper);
            } catch (Exception e) {
                e.printStackTrace();
                for(int i=0; i<10; i++)
                    log.error("COULD NOT GENERATE, COMPRESS AND MAIL REQUESTED PDF");
            }
        }).start();
    }
}