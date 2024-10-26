package com.epapers.epapers.util;

import com.epapers.epapers.config.AppConfig;
import com.epapers.epapers.model.Epaper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import com.itextpdf.text.Image;

@Slf4j
public class AppUtils {

    private AppUtils() {}

    public static final String TOI_EDITIONS = "[ { \"EditionDisplayName\": \"Ahmedabad\", \"EditionId\": \"toiac\" }, { \"EditionDisplayName\": \"Bengaluru\", \"EditionId\": \"toibgc\" }, { \"EditionDisplayName\": \"Bhopal\", \"EditionId\": \"toibhoc\" }, { \"EditionDisplayName\": \"Chandigarh\", \"EditionId\": \"toicgct\" }, { \"EditionDisplayName\": \"Chennai\", \"EditionId\": \"toich\" }, { \"EditionDisplayName\": \"Delhi\", \"EditionId\": \"cap\" }, { \"EditionDisplayName\": \"Goa\", \"EditionId\": \"toigo\" }, { \"EditionDisplayName\": \"Hyderabad\", \"EditionId\": \"toih\" }, { \"EditionDisplayName\": \"Jaipur\", \"EditionId\": \"toijc\" }, { \"EditionDisplayName\": \"Kochi\", \"EditionId\": \"toikrko\" }, { \"EditionDisplayName\": \"Kolkata\", \"EditionId\": \"toikc\" }, { \"EditionDisplayName\": \"Lucknow\", \"EditionId\": \"toilc\" }, { \"EditionDisplayName\": \"Mumbai\", \"EditionId\": \"toim\" }, { \"EditionDisplayName\": \"Pune\", \"EditionId\": \"toipuc\" } ]";

    public static String getTodaysDate() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String todaysDate = dtf.format(ZonedDateTime.now(ZoneId.of("Asia/Kolkata")));
        log.info("Today's date (default) is : {}", todaysDate);
        return todaysDate;
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
        result = switch (serviceName) {
            case "TELEGRAM" ->
                    fileSizeMB > 50;
            case "GMAIL" ->
                    fileSizeMB > 25;
            default ->
                    true;
        };
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
            byte[] buffer = new byte[AppConfig.INPUT_BUFFER_SIZE];
            int count = 0;
            while ((count = bis.read(buffer, 0, AppConfig.INPUT_BUFFER_SIZE)) != -1) {
                fis.write(buffer, 0, count);
            }
        };
    }

    public static String getIPAddr(HttpServletRequest request) {
        return request.getHeader("X-FORWARDED-FOR") != null ? request.getHeader("X-FORWARDED-FOR") : request.getRemoteAddr();
    }

    public static <T> T fetchHttpResponse(OkHttpClient client, ObjectMapper objectMapper, String url, Class<T> returnType) {
        T finalResponseBody = null;
        Request request = null;
        Request.Builder reqBuilder = new Request.Builder()
                .url(url)
                .get();

        if (url.contains("harnscloud")) {
            request = reqBuilder.header("User-Agent", "myagent")
                    .header("Referer", "https://epaper.indiatimes.com/")
                    .build();
        } else {
            request = reqBuilder.build();
        }

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                finalResponseBody = objectMapper.readValue(response.body().string(), returnType);
            } else {
                throw new RuntimeException("HTTP connection failed with status code: " + response.code());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return finalResponseBody;
    }

    private static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int nRead;
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        return buffer.toByteArray();
    }

    public static Image fetchAndScaleImage(OkHttpClient client, String imgLink, float scaleFactor) {
        Request request = new Request.Builder()
                .url(imgLink)
                .header("User-Agent", "myagent")
                .header("Referer", "https://epaper.indiatimes.com/")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new RuntimeException("Failed to download image: " + response);
            }

            // Retrieve the image data as an InputStream
            InputStream inputStream = response.body().byteStream();

            // Create the Image instance from the InputStream
            Image image = Image.getInstance(toByteArray(inputStream));
            image.scalePercent(scaleFactor); // Scale the image as required
            return image;

        } catch (Exception e) {
            throw new RuntimeException("Error fetching or scaling image", e);
        }
    }
}
