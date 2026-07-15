package com.hackathon.service.analytics;

import com.hackathon.dto.analytics.MetricResultDTO;

import java.util.List;

public interface AnalyticsService {

    // 1. Lấy thống kê tiêu chí theo phạm vi
    List<MetricResultDTO> getCriteriaStats(String scope, Integer id);

}