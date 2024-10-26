package com.epapers.epapers.service.downloader;

import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.model.KPPageNew;
import com.epapers.epapers.util.AppUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;

import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class KPDownload implements DownloadStrategy {

    private static final String KP_BNG_PAGES_LINK = "https://www.enewspapr.com/OutSourcingDataChanged.php?operation=getPageArticleDetails&selectedIssueId=KANPRABHA_BG_%s&data=0";
    private static final String KP_IMAGE_BASE_URL = "https://www.enewspapr.com/News/KANPRABHA/BG/%s/%s/%s/%s";
    private OkHttpClient httpClient;

    private static final String EPAPER_KEY_STRING = "epaper";

    @Override
    public void initialize(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
    }

    @Override
    @Retry(name = "")
    public Map<String, Object> downloadPDF(String mainEdition, String date) {
        date = AppUtils.getTodaysDate();
        Map<String, Object> response = new HashMap<>();
        Epaper epaper = new Epaper(date, "BNG");
        if (epaper.getFile().exists()) {
            response.put(EPAPER_KEY_STRING, epaper);
            return response;
        }

        log.info("Called getKP with date: {}", date);

        String[] dateSplit = date.split("/");
        String day = dateSplit[0];
        String month = dateSplit[1];
        String year = dateSplit[2];
        String dateString = year + month + day;

        String metaUrl = String.format(KP_BNG_PAGES_LINK, dateString);

        Request request = new Request.Builder()
                .url(metaUrl)
                .build();

        String links = "";

        try (Response resp = this.httpClient.newCall(request).execute()) {
            if (resp.isSuccessful() && resp.body() != null) {
                links = resp.body().string();
            } else {
                throw new IOException("Unexpected code " + resp);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        KPPageNew[] pages;
        try {
            pages = new ObjectMapper().readValue(links, KPPageNew[].class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (pages != null) {
            ExecutorService executor = Executors.newCachedThreadPool();
            final Document doc = new Document();
            final PdfCopy copy;
            try {
                copy = new PdfCopy(doc, new FileOutputStream(epaper.getFile().getName()));
                doc.open();
            } catch (DocumentException | FileNotFoundException e) {
                throw new RuntimeException(e);
            }

            List<CompletableFuture<PdfReader>> futures = new ArrayList<>();
            for (int i = 1; i <= pages.length; i++) {
                String fileLoc = dateString + "_" + String.format("%02d", i);
                String pageUrl = String.format(KP_IMAGE_BASE_URL, year, month, day, fileLoc);
                CompletableFuture<PdfReader> future = CompletableFuture.supplyAsync(() -> {
                    try {
                        return new PdfReader(new URL(pageUrl));
                    } catch (
                            IOException e) {
                        throw new RuntimeException(e);
                    }
                }, executor);
                futures.add(future);
            }

            futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .forEach(reader -> {
                        try {
                            log.info("Processing : {}", copy.getPageNumber());
                            copy.addDocument(reader);
                            reader.close();
                        } catch (Exception e) {
                            log.error("KP Merge Error: {}", e.getMessage());
                        }
                    });

            doc.close();
            response.put(EPAPER_KEY_STRING, epaper);
        }
        return response;
    }
}
