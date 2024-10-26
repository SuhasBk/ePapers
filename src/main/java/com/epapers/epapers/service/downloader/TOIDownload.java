package com.epapers.epapers.service.downloader;

import com.epapers.epapers.config.AppConfig;
import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.model.TOIPages;
import com.epapers.epapers.util.AppUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class TOIDownload implements DownloadStrategy{

    private static final String TOI_BASE_URL = "https://asset.harnscloud.com/PublicationData/TOI/";
    private static final String TOI_META_URL = TOI_BASE_URL + "%s/%s/%s/%s/DayIndex/%s_%s.json";
    private static final String EPAPER_KEY_STRING = "epaper";
    private OkHttpClient httpClient;
    private ObjectMapper objectMapper;

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
            final float scaleFactor = AppConfig.TOI_SCALE_PERCENT;
            Image image = AppUtils.fetchAndScaleImage(this.httpClient, imgLink, scaleFactor); //Image.getInstance(new URL(imgLink));
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
            log.error("Something horribly went wrong! - {}", e.getMessage());
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
    public void initialize(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, Object> downloadPDF(String mainEdition, String date) {
        Map<String, Object> response = new HashMap<>();
        List<String> pagesLinks = new ArrayList<>();
        log.info("Called getTOIpdf with edition: {} and date: {}", mainEdition, date);

        String[] dateSplit = date.split("/");
        String day = dateSplit[0];
        String month = dateSplit[1];
        String year = dateSplit[2];
        String metaUrl = String.format(TOI_META_URL, mainEdition, year, month, day, date.replace("/","_"), mainEdition);
        TOIPages pages = AppUtils.fetchHttpResponse(httpClient, objectMapper, metaUrl, TOIPages.class);

        if(pages != null) {
            pages.getDayIndex().forEach(pageData -> {
                String pageUrl = String.format("%s%s/%s/%s/%s/Page/%s.jpg", TOI_BASE_URL, mainEdition, year, month, day, pageData.get("PageName"));
                pagesLinks.add(pageUrl);
            });
            Epaper epaper;
            try {
                epaper = getPDF(pagesLinks, mainEdition, date);
                response.put(EPAPER_KEY_STRING, epaper);
            } catch (Exception e) {
                log.error("Something horribly went wrong! - {}", e.getMessage());
            }
        }
        return response;
    }
}
