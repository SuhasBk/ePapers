package com.epapers.epapers.service;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.epapers.epapers.config.AppConfig;
import com.epapers.epapers.model.Edition;
import com.epapers.epapers.model.Epaper;
import com.epapers.epapers.model.HTPage;
import com.epapers.epapers.model.KPPageNew;
import com.epapers.epapers.service.downloader.HTDownload;
import com.epapers.epapers.service.downloader.PDFDownloader;
import com.epapers.epapers.service.downloader.TOIDownload;
import com.epapers.epapers.util.AppUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfCopy;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class EpaperService {

    @Autowired
    EmailService emailService;

    @Autowired
    WebClient webClient;

    private static final String HT_BASE_URL = "https://epaper.hindustantimes.com";
    private static final String HT_EDITIONS_URL = "https://epaper.hindustantimes.com/Home/GetEditionList";
    private static final String KP_BNG_PAGES_LINK = "https://www.enewspapr.com/OutSourcingDataChanged.php?operation=getPageArticleDetails&selectedIssueId=KANPRABHA_BG_%s&data=0";
    private static final String KP_IMAGE_BASE_URL = "https://www.enewspapr.com/News/KANPRABHA/BG/%s/%s/%s/%s";
    private static final String EPAPER_KEY_STRING = "epaper";

    public List<Edition> getHTEditionList() {
        log.info("Accessing HT url : {}", HT_EDITIONS_URL);
        Edition[] editions = webClient
                .get()
                .uri(HT_EDITIONS_URL)
                .retrieve()
                .bodyToMono(Edition[].class)
                .doOnError(err -> {throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "HT is down " + err.toString());})
                .block();

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
            e.printStackTrace();
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
            // scale factor based on publication:
            final float scaleFactor;
            if(imgLink.contains("harnscloud")) {
                scaleFactor = AppConfig.TOI_SCALE_PERCENT;
            } else if (imgLink.contains("enewspapr")) {
                scaleFactor = 70f;
            } else {
                scaleFactor = AppConfig.HT_SCALE_PERCENT;
            }

            Image image = Image.getInstance(new URL(imgLink));
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

    // public Map<String, Object> getHTpdf(String mainEdition, String date) throws Exception {
    //     Map<String, Object> response = new HashMap<>();
    //     List<String> pagesLinks = new ArrayList<>();
    //     log.info("Called getHTpdf with edition: {} and date: {}", mainEdition, date);

    //     List<String> editions = getHTSupplementEditions(mainEdition, date);
    //     editions.forEach(edition -> {
    //         try {
    //             pagesLinks.addAll(getPages(edition, date));
    //         } catch (Exception e) {
    //             e.printStackTrace();
    //         }
    //     });

    //     final Epaper epaper = getPDF(pagesLinks, mainEdition, date);
    //     response.put(EPAPER_KEY_STRING, epaper);
    //     return response;
    // }

    // public Map<String, Object> getTOIpdf(String mainEdition, String date) throws Exception {
    //     Map<String, Object> response = new HashMap<>();
    //     List<String> pagesLinks = new ArrayList<>();
    //     log.info("Called getTOIpdf with edition: {} and date: {}", mainEdition, date);

    //     String[] dateSplit = date.split("/");
    //     String day = dateSplit[0];
    //     String month = dateSplit[1];
    //     String year = dateSplit[2];
    //     String metaUrl = String.format(TOI_META_URL, mainEdition, year, month, day, date.replace("/","_"), mainEdition);

    //     TOIPages pages = webClient
    //             .get()
    //             .uri(metaUrl)
    //             .retrieve()
    //             .bodyToMono(TOIPages.class)
    //             .block();

    //     if(pages != null) {
    //         pages.getDayIndex().forEach(pageData -> {
    //             String pageUrl = String.format("%s%s/%s/%s/%s/Page/%s.jpg", TOI_BASE_URL, mainEdition, year, month, day, pageData.get("PageName"));
    //             pagesLinks.add(pageUrl);
    //         });
    //         final Epaper epaper = getPDF(pagesLinks, mainEdition, date);
    //         response.put(EPAPER_KEY_STRING, epaper);
    //     }
    //     return response;
    // }

    public Map<String, Object> getKannadaPrabha() throws Exception {
        String date = AppUtils.getTodaysDate();
        Map<String, Object> response = new HashMap<>();
        Epaper epaper = new Epaper(date, "BNG");
        if (epaper.getFile().exists()) {
            response.put(EPAPER_KEY_STRING, epaper);
            return response;
        }
        KPPageNew[] pages = null;

        log.info("Called getKP with date: {}", date);
        
        String[] dateSplit = date.split("/");
        String day = dateSplit[0];
        String month = dateSplit[1];
        String year = dateSplit[2];
        String dateString = year + month + day;

        String metaUrl = String.format(KP_BNG_PAGES_LINK, dateString);

        String links = webClient
            .get()
            .uri(metaUrl)
            .retrieve()
            .toEntity(String.class)
            .timeout(Duration.ofSeconds(5))
            .block()
            .getBody();
        
        pages = new ObjectMapper().readValue(links, KPPageNew[].class);
        
        if (pages != null) {
            ExecutorService executor = Executors.newCachedThreadPool();
            List<Callable<PdfReader>> callables = new ArrayList<>();
            final Document doc = new Document();
            final PdfCopy copy = new PdfCopy(doc, new FileOutputStream(epaper.getFile().getName()));
            doc.open();

            for(int i = 1; i <= pages.length; i++) {
                String fileLoc = dateString + "_" + String.format("%02d", i);
                String pageUrl = String.format(KP_IMAGE_BASE_URL, year, month, day, fileLoc);
                
                callables.add(() -> {
                    PdfReader reader = null;
                    try {
                        reader = new PdfReader(new URL(pageUrl).openStream());
                    } catch(Exception e) {
                        log.error("KP Download Error: {}", e);
                    }
                    return reader;
                });
            }
            
            List<Future<PdfReader>> results = executor.invokeAll(callables);
            results.forEach(res -> {
                try {
                    log.info("KP Downloading: Page {} of {}", results.indexOf(res) + 1, results.size());
                    PdfReader reader = res.get();
                    copy.addDocument(reader);
                    reader.close();
                } catch (Exception e) {
                    log.error("KP Merge Error: {}", e);
                }
            });
            doc.close();
            response.put(EPAPER_KEY_STRING, epaper);
        }        
        return response;
    }

    public void mailPDF(String emailId, String mainEdition, String date, String publication) {
        new Thread(() -> {
            Epaper epaper;
            try {
                PDFDownloader downloader = new PDFDownloader();
                if(publication.equals("HT")) {
                    downloader.setDownloadStrategy(new HTDownload(webClient));
                } else {
                    downloader.setDownloadStrategy(new TOIDownload(webClient));
                }
                epaper = (Epaper) downloader.getPDF(mainEdition, date).get("epaper");
                // AppUtils.compressPDF(epaper);
                emailService.mailPDF(emailId, epaper);
            } catch (Exception e) {
                e.printStackTrace();
                for(int i=0; i<10; i++)
                    log.error("COULD NOT GENERATE, COMPRESS AND MAIL REQUESTED PDF");
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