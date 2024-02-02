package com.epapers.epapers.service.downloader;

import java.util.Map;

public interface DownloadStrategy {
    public Map<String, Object> downloadPDF(String mainEdition, String date);
}
