package com.shopping.system.dto;

import java.util.List;

public record SellerDashboardResponse(
        String sellerName,
        long totalProducts,
        long availableProducts,
        long outOfStockProducts,
        long totalStock,
        List<ProductResponse> recentProducts
) {
}
