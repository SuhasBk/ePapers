package com.epapers.epapers.service;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.epapers.epapers.model.Edition;
import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.util.AppUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EpaperService {

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    EmailService emailService;

    private final String HT_BASE_URL = "https://epaper.hindustantimes.com";
    private final String TOI_BASE_URL = "https://asset.harnscloud.com/PublicationData/TOI/";

    private final String TOI_META_URL = TOI_BASE_URL + "%s/%s/%s/%s/DayIndex/%s_%s.json";

    public List<Edition> getHTEditionList() throws Exception {
        List<Edition> editions = new ArrayList<>();
        String url = "https://epaper.hindustantimes.com/Home/GetEditionList";
        List<Map<String, Object>> json = AppUtils.getHTJsonObject(url);
        json.forEach(edition -> {
            Edition editionInfo = new Edition(edition.get("EditionId").toString(), edition.get("EditionDisplayName").toString());
            editions.add(editionInfo);
        });
        log.info("Edition list: {}", editions);
        return editions;
    }

    public List<Edition> getTOIEditionList() throws Exception {
        String editions = AppUtils.getTOIEditions();
        return Arrays.asList(objectMapper.readValue(editions, Edition[].class));
    }

    public List<String> getHTSupplementEditions(String mainEdition, String date) {
        List<String> editions = new ArrayList<>();
        log.info("Called getHTSupplementEditions with edition: {} and date: {}", mainEdition, date);
        String url = String.format(HT_BASE_URL + "/Home/GetAllSupplement?edid=%1$s&EditionDate=%2$s", mainEdition, date.replaceAll("/", "%2F"));
        List<Map<String, Object>> json = AppUtils.getHTJsonObject(url);
        json.forEach(edition -> editions.add(edition.get("EditionId").toString()));
        return editions;
    }

    public List<String> getPages(String edition, String date) {
        List<String> links = new ArrayList<>();
        log.info("Called getPages with edition: {} and date: {}", edition, date);
        String url = String.format(HT_BASE_URL + "/Home/GetAllpages?editionid=%1$s&editiondate=%2$s", edition,
                date.replaceAll("/", "%2F"));
        List<Map<String, Object>> json = AppUtils.getHTJsonObject(url);
        json.forEach(page -> links.add(page.get("HighResolution").toString().replaceAll("_mr", "")));
        return links;
    }

    public Epaper getPDF(List<String> links, String edition, String date) throws Exception {
        log.info("Starting downloads for edition: {}", edition);
        Epaper epaper = new Epaper(date.replaceAll("/", "_"), edition);

        File file = epaper.getFile();

        if(file.exists()) {
            log.info("File already exists, skipping download from HT servers...");
            return epaper;
        }

//        ExecutorService executor = Executors.newFixedThreadPool(30);
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

        futureList = executor.invokeAll(callableList);

        try {
            futureList.forEach((img) -> {
                try {
                    document.add(img.get());
                    log.info("Downloaded: {} of {}", futureList.indexOf(img) + 1, futureList.size());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            document.close();
            log.info("Finished collecting pages: {}\n", document);
            if(executor.shutdownNow().size() > 0) {
                log.error("Some pages might not be added to PDF! :(");
            }
            System.gc();
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
        response.put("epaper", epaper);
        return response;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTOIpdf(String mainEdition, String date) throws Exception {
        Map<String, Object> response = new HashMap<>();
        List<String> pagesLinks = new ArrayList<>();
        log.info("Called getTOIpdf with edition: {} and date: {}", mainEdition, date);

        String[] dateSplit = date.split("/");
        String DAY = dateSplit[0];
        String MONTH = dateSplit[1];
        String YEAR = dateSplit[2];
        String metaUrl = String.format(TOI_META_URL, mainEdition, YEAR, MONTH, DAY, date.replaceAll("/",
"_"), mainEdition);

        Map<String, Object> meta = AppUtils.getTOIJsonObject(metaUrl);
        List<Object> metaData = Arrays.asList(meta.get("DayIndex"));
        List<Object> data = (List<Object>) metaData.get(0);

        data.forEach(page -> {
            Map<String, String> pageData = (Map<String, String>) page;
            String PAGE_URL = String.format(TOI_BASE_URL + "%s/%s/%s/%s/Page/%s_%s_%s.jpg", mainEdition, YEAR, MONTH, DAY, date.replaceAll("/", "_"), pageData.get("DisplayPageNumber"), mainEdition);
            pagesLinks.add(PAGE_URL);
        });

        final Epaper epaper = getPDF(pagesLinks, mainEdition, date);
        response.put("epaper", epaper);
        return response;
    }

    public void mailPDF(String emailId, String mainEdition, String date, String publication) {
        new Thread(() -> {
            Epaper epaper = null;
            try {
                if(publication.equals("HT")) {
                    epaper = (Epaper) getHTpdf(mainEdition, date).get("epaper");
                } else {
                    epaper = (Epaper) getTOIpdf(mainEdition, date).get("epaper");
                }
                System.gc();
                AppUtils.compressPDF(epaper);
                emailService.mailPDF(emailId, epaper);
            } catch (Exception e) {
                e.printStackTrace();
                for(int i=0; i<10; i++)
                    log.error("COULD NOT GENERATE, COMPRESS AND MAIL REQUESTED PDF");
            }
        }).start();
    }
}