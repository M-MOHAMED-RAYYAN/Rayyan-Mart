package com.shopping.system.controller;

import com.shopping.system.dto.ApiResponse;
import com.shopping.system.dto.ProductRequest;
import com.shopping.system.dto.ProductResponse;
import com.shopping.system.service.ProductService;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> list(@RequestParam(required = false) String search,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) BigDecimal minPrice,
                                      @RequestParam(required = false) BigDecimal maxPrice,
                                      @RequestParam(required = false) Boolean available,
                                      @RequestParam(required = false) String sort) {
        return productService.search(search, category, minPrice, maxPrice, available, sort);
    }

    @GetMapping("/categories")
    public List<String> categories() {
        return productService.categories();
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request, HttpSession session) {
        return productService.create(request, session);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id,
                                  @Valid @RequestBody ProductRequest request,
                                  HttpSession session) {
        return productService.update(id, request, session);
    }

    @DeleteMapping("/{id}")
    public ApiResponse delete(@PathVariable Long id, HttpSession session) {
        productService.delete(id, session);
        return ApiResponse.success("Product deleted successfully", null);
    }
}