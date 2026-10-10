package com.shopping.system.controller;

import com.shopping.system.dto.CartRequest;
import com.shopping.system.dto.CartResponse;
import com.shopping.system.dto.QuantityRequest;
import com.shopping.system.service.CartService;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(HttpSession session) {
        return cartService.getCart(session);
    }

    @PostMapping("/add")
    public CartResponse add(@Valid @RequestBody CartRequest request, HttpSession session) {
        return cartService.add(session, request);
    }

    @PutMapping("/item/{id}")
    public CartResponse update(@PathVariable Long id,
                               @Valid @RequestBody QuantityRequest request,
                               HttpSession session) {
        return cartService.updateQuantity(session, id, request.quantity());
    }

    @DeleteMapping("/item/{id}")
    public CartResponse remove(@PathVariable Long id, HttpSession session) {
        return cartService.remove(session, id);
    }

    @DeleteMapping("/clear")
    public CartResponse clear(HttpSession session) {
        return cartService.clear(session);
    }
}