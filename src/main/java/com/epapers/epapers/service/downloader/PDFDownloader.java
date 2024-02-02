package com.epapers.epapers.service.downloader;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class PDFDownloader {
    private DownloadStrategy strategy;

    public void setDownloadStrategy(DownloadStrategy strategy) {
        this.strategy = strategy;
    }

    public Map<String, Object> getPDF(String mainEdition, String date) {
        return strategy.downloadPDF(mainEdition, date);
    }
}
