package com.epapers.epapers.service.downloader;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;

import java.net.http.HttpClient;
import java.util.Map;

public interface DownloadStrategy {
    void initialize(OkHttpClient httpClient, ObjectMapper objectMapper);
    Map<String, Object> downloadPDF(String mainEdition, String date);
}
