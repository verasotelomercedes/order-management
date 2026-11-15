package com.verasotelomercedes.ordermanagement.logistics.infrastructure;

import com.verasotelomercedes.ordermanagement.logistics.application.LogisticsRate;
import com.verasotelomercedes.ordermanagement.logistics.application.LogisticsRatePort;
import com.verasotelomercedes.ordermanagement.logistics.application.LogisticsRateRequest;
import org.springframework.stereotype.Component;

@Component
public class LogisticsRestClient implements LogisticsRatePort {

    @Override
    public LogisticsRate requestRate(LogisticsRateRequest request) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
