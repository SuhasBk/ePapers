package com.epapers.epapers.service.downloader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.itextpdf.text.pdf.PdfReader;
import org.springframework.web.reactive.function.client.WebClient;

import com.epapers.epapers.config.AppConfig;
import com.epapers.epapers.model.Edition;
import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.model.HTPage;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
public class HTDownload implements DownloadStrategy {

    private static final String HT_BASE_URL = "https://epaper.hindustantimes.com";
    private static final String EPAPER_KEY_STRING = "epaper";
    private final WebClient webClient;

    public HTDownload(WebClient webClient) {
        this.webClient = webClient;
    }

    public List<String> getHTSupplementEditions(String mainEdition, String date) {
        List<String> editions = new ArrayList<>();
        log.info("Called getHTSupplementEditions with edition: {} and date: {}", mainEdition, date);

        String url = String.format("%1$s/Home/GetAllSupplement?edid=%2$s&EditionDate=%3$s", HT_BASE_URL, mainEdition, date);
        Edition[] htSupplements = webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(Edition[].class)
                .block();

        if(htSupplements != null && htSupplements.length != 0) {
            editions = Arrays.stream(htSupplements).toList()
                    .stream()
                    .map(Edition::getEditionId)
                    .toList();
        }
        return editions;
    }

    public List<String> getPages(String edition, String date) {
        List<String> links = new ArrayList<>();
        log.info("Called getPages with edition: {} and date: {}", edition, date);
        String url = String.format("%1$s/Home/GetAllpages?editionid=%2$s&editiondate=%3$s", HT_BASE_URL, edition, date);

         HTPage[] htPages = webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(HTPage[].class)
                .block();

         if(htPages != null && htPages.length != 0) {
             Arrays.stream(htPages).toList().forEach(page -> links.add(page.getHighResolution().replace("_mr", "")));
         }

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
            final float scaleFactor = AppConfig.HT_SCALE_PERCENT;
            Image image = Image.getInstance(Objects.requireNonNull(Mono.from(webClient.get()
                            .uri(imgLink)
                            .retrieve()
                            .bodyToMono(byte[].class))
                            .block()));
            image.scalePercent(scaleFactor);
            return image;
        }));

        // let executor handle stuff including exceptions
        futureList = executor.invokeAll(callableList);

        // process returned results in synchronous manner
        try {
            futureList.forEach(img -> {
                try {
                    document.add(img.get());
                    log.info("{} Downloaded: {} of {}", edition, futureList.indexOf(img) + 1, futureList.size());
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

    @Override
    public Map<String, Object> downloadPDF(String mainEdition, String date) {
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

        Epaper epaper;
        try {
            epaper = getPDF(pagesLinks, mainEdition, date);
            response.put(EPAPER_KEY_STRING, epaper);
        } catch (Exception e) {
            log.error("Error occurred: {}", e);
        }
        return response;
    }
    
}
