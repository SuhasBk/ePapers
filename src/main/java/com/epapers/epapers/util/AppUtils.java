package com.epapers.epapers.util;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.epapers.epapers.model.Epaper;
import com.google.gson.Gson;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfStream;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SuppressWarnings("unchecked")
public class AppUtils {

    private AppUtils() {}

    private static final Gson gson = new Gson();

    public static final String TOI_EDITIONS = "[ { \"editionName\": \"Ahmedabad\", \"editionId\": \"toiac\" }, { \"editionName\": \"Bengaluru\", \"editionId\": \"toibgc\" }, { \"editionName\": \"Bhopal\", \"editionId\": \"toibhoc\" }, { \"editionName\": \"Chandigarh\", \"editionId\": \"toicgct\" }, { \"editionName\": \"Chennai\", \"editionId\": \"toich\" }, { \"editionName\": \"Delhi\", \"editionId\": \"cap\" }, { \"editionName\": \"Goa\", \"editionId\": \"toigo\" }, { \"editionName\": \"Hyderabad\", \"editionId\": \"toih\" }, { \"editionName\": \"Jaipur\", \"editionId\": \"toijc\" }, { \"editionName\": \"Kochi\", \"editionId\": \"toikrko\" }, { \"editionName\": \"Kolkata\", \"editionId\": \"toikc\" }, { \"editionName\": \"Lucknow\", \"editionId\": \"toilc\" }, { \"editionName\": \"Mumbai\", \"editionId\": \"toim\" }, { \"editionName\": \"Pune\", \"editionId\": \"toipuc\" } ]";

    public static String getTodaysDate() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String todaysDate = dtf.format(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")));
        log.info("Today's date (default) is : {}", todaysDate);
        return todaysDate;
    }

    public static List<Map<String, Object>> getHTJsonObject(String urlString) {
        List<Map<String, Object>> response = new ArrayList<>();
        log.info("Accessing HT url : {}", urlString);
        try (InputStreamReader reader = new InputStreamReader(new URL(urlString).openStream())) {
            response = gson.fromJson(reader, List.class);
        } catch (Exception e) {
            log.error("HT IS DOWN:\n{}\nURL: {}", e, urlString);
        }

        if(response.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "I am guessing yesterday was a holiday for journalists, they must be chilling today, try tomorrow!\n\nIf it still ain't working, then adiós, we are done here...");
        }
        return response;
    }

    public static Map<String, Object> getTOIJsonObject(String urlString) throws Exception {
        Map<String, Object> response = new HashMap<>();
        try (InputStreamReader reader = new InputStreamReader(new URL(urlString).openStream())) {
            response = gson.fromJson(reader, Map.class);
        } catch (Exception e) {
            log.error("Oops, something is wrong!", e);
        }

        if (response.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "I am guessing yesterday was a holiday for journalists, they must be chilling today, try tomorrow!\n\nIf it still ain't working, then adiós, we are done here...");
        }
        return response;
    }

    public static Map<String, Object> getKPJsonObject(String urlString) throws Exception {
        Map<String, Object> response = new HashMap<>();
        try (InputStreamReader reader = new InputStreamReader(new URL(urlString).openStream())) {
            response = gson.fromJson(reader, Map.class);
        } catch (Exception e) {
            log.error("Oops, something is wrong!", e);
        }

        if (response.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "I am guessing yesterday was a holiday for journalists, they must be chilling today, try tomorrow!\n\nIf it still ain't working, then adiós, we are done here...");
        }
        return response;
    }

    public static void compressPDF(Epaper epaper) throws Exception {
        String src = epaper.getFile().getAbsolutePath();
        String dest = src.replace(".pdf", "_tmp.pdf");
        float resizeFactor = 1.0f;
        try (PdfDocument pdfDoc = new PdfDocument(new PdfReader(src), new PdfWriter(dest))) {
            int pages = pdfDoc.getNumberOfPages();

            // Iterate over all pages to get all images.
            for (int i = 1; i <= pages; i++) {
                PdfPage page = pdfDoc.getPage(i);
                PdfDictionary pageDict = page.getPdfObject();
                PdfDictionary resources = pageDict.getAsDictionary(PdfName.Resources);
                // Get images
                PdfDictionary xObjects = resources.getAsDictionary(PdfName.XObject);
                for (PdfName imgRef : xObjects.keySet()) {
                    // Get image
                    PdfStream stream = xObjects.getAsStream(imgRef);
                    PdfImageXObject image = new PdfImageXObject(stream);
                    BufferedImage bi = image.getBufferedImage();
                    if (bi == null)
                        continue;

                    // Create new image
                    int width = (int) (bi.getWidth() * resizeFactor);
                    int height = (int) (bi.getHeight() * resizeFactor);
                    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
                    AffineTransform at = AffineTransform.getScaleInstance(resizeFactor, resizeFactor);
                    Graphics2D g = img.createGraphics();
                    g.drawRenderedImage(bi, at);
                    ByteArrayOutputStream imgBytes = new ByteArrayOutputStream();

                    // Write new image
                    ImageIO.write(img, "JPG", imgBytes);
                    com.itextpdf.layout.element.Image imgNew = new com.itextpdf.layout.element.Image(ImageDataFactory.create(imgBytes.toByteArray()));

                    // Replace the original image with the resized image
                    xObjects.put(imgRef, imgNew.getXObject().getPdfObject());
                }
                log.info("Compressed {} of {} pages", i, pages);
            }
        }
        
        deleteFile(epaper.getFile());
        Path source = Paths.get(dest);
        Files.move(source, source.resolveSibling(src));
        File newFile = new File(src);
        epaper.setFile(newFile);
    }

    public static boolean isLargeFile(File file, String serviceName) {
        boolean result = false;
        long fileSizeMB = file.length() / (1024*1024);
        switch(serviceName) {
            case "TELEGRAM":
                result = fileSizeMB > 50;
                break;
            case "GMAIL":
                result = fileSizeMB > 25;
                break;
            default:
                result = true;
        }
        return result;
    }

    public static void deleteFile(File file) {
        try {
            Files.delete(file.toPath());
        } catch(IOException e) {
            log.error("Oops! Failed to delete tmp file: {}", file.getAbsolutePath());
        }
    }

    public static void downloadFileFromUrl(String fileUrl, File destFile) throws Exception {
        URL url = new URL(fileUrl);
        try(BufferedInputStream bis = new BufferedInputStream(url.openStream());
            FileOutputStream fis = new FileOutputStream(destFile);) {
            byte[] buffer = new byte[1024];
            int count = 0;
            while ((count = bis.read(buffer, 0, 1024)) != -1) {
                fis.write(buffer, 0, count);
            }
        };
    }

    public static String getIPAddr(HttpServletRequest request) {
        return request.getHeader("X-FORWARDED-FOR") != null ? request.getHeader("X-FORWARDED-FOR") : request.getRemoteAddr();
    }
}
