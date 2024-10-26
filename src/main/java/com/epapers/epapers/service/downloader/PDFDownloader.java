package com.epapers.epapers.service.downloader;

import java.net.http.HttpClient;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Service;

@Service
public class PDFDownloader {
    private DownloadStrategy strategy;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public PDFDownloader(OkHttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public void setDownloadStrategy(DownloadStrategy strategy) {
        this.strategy = strategy;
        this.strategy.initialize(httpClient, objectMapper);
    }

    public Map<String, Object> getPDF(String mainEdition, String date) {
        return strategy.downloadPDF(mainEdition, date);
    }
}
