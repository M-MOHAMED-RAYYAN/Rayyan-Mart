package com.shopping.system.controller;

import com.shopping.system.dto.ApiResponse;
import com.shopping.system.dto.ProductRequest;
import com.shopping.system.dto.ProductResponse;
import com.shopping.system.dto.SellerDashboardResponse;
import com.shopping.system.dto.SellerOrderResponse;
import com.shopping.system.dto.StockUpdateRequest;
import com.shopping.system.service.SellerService;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

    private final SellerService sellerService;

    public SellerController(SellerService sellerService) {
        this.sellerService = sellerService;
    }

    @GetMapping("/dashboard")
    public SellerDashboardResponse dashboard(HttpSession session) {
        return sellerService.dashboard(session);
    }

    @GetMapping("/products")
    public List<ProductResponse> myProducts(HttpSession session) {
        return sellerService.myProducts(session);
    }

    @GetMapping("/orders")
    public List<SellerOrderResponse> myOrders(HttpSession session) {
        return sellerService.myOrders(session);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request, HttpSession session) {
        return sellerService.create(request, session);
    }

    @PutMapping("/products/{id}")
    public ProductResponse update(@PathVariable Long id,
                                  @Valid @RequestBody ProductRequest request,
                                  HttpSession session) {
        return sellerService.update(id, request, session);
    }

    @DeleteMapping("/products/{id}")
    public ApiResponse delete(@PathVariable Long id, HttpSession session) {
        sellerService.delete(id, session);
        return ApiResponse.success("Product deleted successfully", null);
    }

    @PatchMapping("/products/{id}/stock")
    public ProductResponse updateStock(@PathVariable Long id,
                                       @Valid @RequestBody StockUpdateRequest request,
                                       HttpSession session) {
        return sellerService.updateStock(id, request.quantity(), session);
    }
}
