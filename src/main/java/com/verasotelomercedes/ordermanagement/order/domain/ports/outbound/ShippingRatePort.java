package com.verasotelomercedes.ordermanagement.order.domain.ports.outbound;

import com.verasotelomercedes.ordermanagement.order.domain.ShippingRate;
import com.verasotelomercedes.ordermanagement.order.domain.ShippingRateRequest;

public interface ShippingRatePort {

    ShippingRate requestRate(ShippingRateRequest request);
}
