package com.shopping.system.web;

import com.shopping.system.dto.CartItemResponse;
import com.shopping.system.dto.CartRequest;
import com.shopping.system.dto.CartResponse;
import com.shopping.system.dto.LoginRequest;
import com.shopping.system.dto.LoginResponse;
import com.shopping.system.dto.OrderResponse;
import com.shopping.system.dto.ProductRequest;
import com.shopping.system.dto.ProductResponse;
import com.shopping.system.dto.RatingCount;
import com.shopping.system.dto.RegisterRequest;
import com.shopping.system.dto.ReviewRequest;
import com.shopping.system.dto.ReviewResponse;
import com.shopping.system.dto.SellerDashboardResponse;
import com.shopping.system.dto.SellerOrderResponse;
import com.shopping.system.dto.StockUpdateRequest;
import com.shopping.system.dto.UserAdminResponse;
import com.shopping.system.dto.UserResponse;
import com.shopping.system.model.OrderStatus;
import com.shopping.system.model.PaymentMethod;
import com.shopping.system.model.Role;
import com.shopping.system.model.User;
import com.shopping.system.service.AdminService;
import com.shopping.system.service.AuthService;
import com.shopping.system.service.CartService;
import com.shopping.system.service.OrderService;
import com.shopping.system.service.ProductService;
import com.shopping.system.service.ReviewService;
import com.shopping.system.service.SellerService;
import com.shopping.system.service.SessionService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Server-side page controller. Renders every page with Thymeleaf and handles all
 * form submissions. All business logic lives in the existing services â€” no
 * client-side JavaScript is used.
 */
@Controller
public class WebController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final ProductService productService;
    private final CartService cartService;
    private final OrderService orderService;
    private final SellerService sellerService;
    private final ReviewService reviewService;
    private final AdminService adminService;
    private final Ui ui;

    public WebController(AuthService authService,
                         SessionService sessionService,
                         ProductService productService,
                         CartService cartService,
                         OrderService orderService,
                         SellerService sellerService,
                         ReviewService reviewService,
                         AdminService adminService,
                         Ui ui) {
        this.authService = authService;
        this.sessionService = sessionService;
        this.productService = productService;
        this.cartService = cartService;
        this.orderService = orderService;
        this.sellerService = sellerService;
        this.reviewService = reviewService;
        this.adminService = adminService;
        this.ui = ui;
    }

    /* ------------------------------------------------------------------ *
     *  Shared view helpers
     * ------------------------------------------------------------------ */

    /** Adds the nav (current user + cart badge) and the Ui helper to every model. */
    private void addNav(Model model, HttpSession session) {
        User current = currentUser(session);
        model.addAttribute("ui", ui);
        model.addAttribute("navUser", current == null ? null : UserResponse.from(current));
        int cartCount = 0;
        if (current != null) {
            try {
                CartResponse cart = cartService.getCart(session);
                cartCount = cart.items().stream().mapToInt(CartItemResponse::quantity).sum();
            } catch (RuntimeException ignored) {
                // badge is decorative
            }
        }
        model.addAttribute("cartCount", cartCount);
    }

    private User currentUser(HttpSession session) {
        if (!sessionService.isLoggedIn(session)) {
            return null;
        }
        try {
            return sessionService.getCurrentUser(session);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Redirect target after a form post, keeping the user on the same page. */
    private String referer(HttpServletRequest request) {
        String ref = request.getHeader("Referer");
        if (ref != null) {
            int scheme = ref.indexOf("://");
            if (scheme >= 0) {
                int slash = ref.indexOf('/', scheme + 3);
                if (slash >= 0) {
                    return ref.substring(slash);
                }
            }
        }
        return "/products";
    }

    private boolean isSafePath(String path) {
        return path != null && !path.isBlank()
                && !path.startsWith("\\")
                && !path.contains("://")
                && path.matches("[A-Za-z0-9_./?=&%+-]+");
    }

    /* ------------------------------------------------------------------ *
     *  Public pages
     * ------------------------------------------------------------------ */

    @GetMapping("/")
    public String index(Model model, HttpSession session) {
        addNav(model, session);
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(Model model, HttpSession session,
                            @RequestParam(required = false) String redirect) {
        addNav(model, session);
        model.addAttribute("redirect", isSafePath(redirect) ? redirect : null);
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        @RequestParam(required = false) String redirect,
                        HttpSession session,
                        RedirectAttributes ra) {
        try {
            LoginResponse response = authService.login(new LoginRequest(email, password), session);
            if ("ADMIN".equals(response.role())) {
                return "redirect:/admin";
            }
            if ("SELLER".equals(response.role())) {
                return "redirect:/seller";
            }
            if (isSafePath(redirect)) {
                return "redirect:" + redirect;
            }
            return "redirect:/products";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
            if (isSafePath(redirect)) {
                ra.addFlashAttribute("redirect", redirect);
            }
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model, HttpSession session) {
        addNav(model, session);
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam(required = false) String password2,
                           @RequestParam(required = false) String role,
                           RedirectAttributes ra) {
        try {
            if (email == null || email.isBlank() || !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                throw new IllegalArgumentException("Email must be valid");
            }
            if (password == null || password.length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters");
            }
            if (password2 != null && !password2.isBlank() && !password.equals(password2)) {
                throw new IllegalArgumentException("Passwords do not match");
            }
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Name is required");
            }
            authService.register(new RegisterRequest(name, email, password, role));
            ra.addFlashAttribute("msg", "Registration successful! Please log in.");
            return "redirect:/login";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
            ra.addFlashAttribute("name", name);
            ra.addFlashAttribute("email", email);
            ra.addFlashAttribute("role", role);
            return "redirect:/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        sessionService.logout(session);
        return "redirect:/login";
    }

    /* ------------------------------------------------------------------ *
     *  Products
     * ------------------------------------------------------------------ */

    @GetMapping("/products")
    public String products(@RequestParam(required = false) String search,
                           @RequestParam(required = false) String category,
                           @RequestParam(required = false) String minPrice,
                           @RequestParam(required = false) String maxPrice,
                           @RequestParam(required = false) String available,
                           @RequestParam(required = false) String sort,
                           Model model,
                           HttpSession session) {
        addNav(model, session);
        BigDecimal min = parseMoney(minPrice);
        BigDecimal max = parseMoney(maxPrice);
        Boolean avail = parseAvailable(available);
        List<ProductResponse> products = productService.search(
                blankToNull(search), blankToNull(category), min, max, avail, sort);
        model.addAttribute("products", products);
        model.addAttribute("categories", productService.categories());
        model.addAttribute("search", search);
        model.addAttribute("category", category);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("available", available);
        model.addAttribute("sort", sort == null || sort.isBlank() ? "name" : sort);
        return "products";
    }

    @GetMapping("/products/{id}")
    public String product(@PathVariable Long id,
                          Model model,
                          HttpSession session,
                          RedirectAttributes ra) {
        ProductResponse product;
        try {
            product = productService.getById(id);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", "Product not found");
            return "redirect:/products";
        }
        List<ReviewResponse> reviews = reviewService.listByProduct(id);
        User current = currentUser(session);
        ReviewResponse myReview = null;
        if (current != null) {
            myReview = reviews.stream()
                    .filter(r -> r.userId() != null && r.userId().equals(current.getId()))
                    .findFirst().orElse(null);
        }
        addNav(model, session);
        model.addAttribute("product", product);
        model.addAttribute("reviews", reviews);
        model.addAttribute("myReview", myReview);
        List<RatingCount> dist = product.ratingDistribution();
        if (dist != null) {
            List<RatingCount> reversed = new ArrayList<>(dist.size());
            for (int i = dist.size() - 1; i >= 0; i--) {
                reversed.add(dist.get(i));
            }
            dist = reversed;
        }
        model.addAttribute("dist", dist);
        model.addAttribute("distMax", Math.max(1,
                product.ratingDistribution() == null ? 0 :
                        product.ratingDistribution().stream().mapToLong(
                                d -> d.count() == null ? 0 : d.count()).max().orElse(0)));
        return "product";
    }

    @PostMapping("/products/{productId}/reviews")
    public String submitReview(@PathVariable Long productId,
                               @RequestParam(required = false) Long reviewId,
                               @RequestParam Integer rating,
                               @RequestParam(required = false) String comment,
                               HttpSession session,
                               RedirectAttributes ra) {
        try {
            ReviewRequest request = new ReviewRequest(rating, comment);
            if (reviewId != null) {
                reviewService.update(session, reviewId, request);
                ra.addFlashAttribute("msg", "Review updated!");
            } else {
                reviewService.create(session, productId, request);
                ra.addFlashAttribute("msg", "Thanks for your review!");
            }
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products/" + productId;
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id,
                               @RequestParam Long productId,
                               HttpSession session,
                               RedirectAttributes ra) {
        try {
            reviewService.delete(session, id);
            ra.addFlashAttribute("msg", "Review deleted");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/products/" + productId;
    }

    /* ------------------------------------------------------------------ *
     *  Cart & checkout
     * ------------------------------------------------------------------ */

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam(defaultValue = "1") int quantity,
                            HttpSession session,
                            HttpServletRequest request,
                            RedirectAttributes ra) {
        if (!sessionService.isLoggedIn(session)) {
            return "redirect:/login?redirect=" + relativePath(request);
        }
        try {
            cartService.add(session, new CartRequest(productId, quantity));
            ra.addFlashAttribute("msg", "Added to cart!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + referer(request);
    }

    @GetMapping("/cart")
    public String cart(Model model, HttpSession session) {
        if (!sessionService.isLoggedIn(session)) {
            return "redirect:/login?redirect=cart";
        }
        addNav(model, session);
        model.addAttribute("cart", cartService.getCart(session));
        return "cart";
    }

    @PostMapping("/cart/item/{id}/update")
    public String updateCartItem(@PathVariable Long id,
                                 @RequestParam int quantity,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        try {
            int safe = Math.max(1, quantity);
            cartService.updateQuantity(session, id, safe);
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/item/{id}/remove")
    public String removeCartItem(@PathVariable Long id, HttpSession session) {
        cartService.remove(session, id);
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(HttpSession session) {
        cartService.clear(session);
        return "redirect:/cart";
    }

    @GetMapping("/checkout")
    public String checkout(Model model, HttpSession session) {
        if (!sessionService.isLoggedIn(session)) {
            return "redirect:/login?redirect=checkout";
        }
        addNav(model, session);
        model.addAttribute("cart", cartService.getCart(session));
        return "checkout";
    }

    @PostMapping("/checkout/place")
    public String placeOrder(@RequestParam(name = "paymentMethod", required = false) String paymentMethod,
                             HttpSession session,
                             RedirectAttributes ra) {
        try {
            OrderResponse order = orderService.placeOrder(session, resolvePaymentMethod(paymentMethod));
            ra.addFlashAttribute("msg", "Order #" + order.id() + " has been placed. Total: "
                    + ui.money(order.totalAmount()) + " (" + ui.title(order.paymentMethod().name()) + ").");
            return "redirect:/orders";
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/cart";
        }
    }

    private static PaymentMethod resolvePaymentMethod(String raw) {
        if (raw == null || raw.isBlank()) {
            return PaymentMethod.CASH_ON_DELIVERY;
        }
        try {
            return PaymentMethod.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PaymentMethod.CASH_ON_DELIVERY;
        }
    }

    /* ------------------------------------------------------------------ *
     *  Orders
     * ------------------------------------------------------------------ */

    @GetMapping("/orders")
    public String orders(Model model, HttpSession session) {
        if (!sessionService.isLoggedIn(session)) {
            return "redirect:/login?redirect=orders";
        }
        addNav(model, session);
        model.addAttribute("orders", orderService.getMyOrders(session));
        return "orders";
    }

    /* ------------------------------------------------------------------ *
     *  Seller dashboard
     * ------------------------------------------------------------------ */

    @GetMapping("/seller")
    public String seller(@RequestParam(required = false) String search,
                         @RequestParam(required = false) String available,
                         @RequestParam(required = false) Long edit,
                         Model model,
                         HttpSession session) {
        User user = currentUser(session);
        if (user == null) {
            return "redirect:/login?redirect=seller";
        }
        if (user.getRole() != Role.SELLER) {
            addNav(model, session);
            model.addAttribute("accessDenied", true);
            model.addAttribute("accessDeniedMsg",
                    "Only sellers can access this page. Your account role is " + user.getRole() + ".");
            return "seller";
        }
        addNav(model, session);
        SellerDashboardResponse dashboard = sellerService.dashboard(session);
        List<ProductResponse> products = sellerService.myProducts(session);
        if (search != null && !search.isBlank()) {
            String term = search.trim().toLowerCase();
            products = products.stream()
                    .filter(p -> (p.name() == null ? "" : p.name().toLowerCase()).contains(term)
                            || (p.description() == null ? "" : p.description().toLowerCase()).contains(term))
                    .toList();
        }
        Boolean avail = parseAvailable(available);
        if (avail != null) {
            products = products.stream()
                    .filter(p -> Boolean.TRUE.equals(p.available()) == avail)
                    .toList();
        }
        List<SellerOrderResponse> orders = sellerService.myOrders(session);

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("products", products);
        model.addAttribute("orders", orders);
        model.addAttribute("categories", productService.categories());
        model.addAttribute("search", search);
        model.addAttribute("available", available);
        model.addAttribute("editProduct", edit == null ? null : productService.getById(edit));
        return "seller";
    }

    @PostMapping("/seller/products")
    public String sellerCreateProduct(@RequestParam String name,
                                      @RequestParam(required = false) String description,
                                      @RequestParam BigDecimal price,
                                      @RequestParam int quantity,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) String imageUrl,
                                      @RequestParam(required = false) boolean available,
                                      HttpSession session,
                                      RedirectAttributes ra) {
        try {
            sellerService.create(new ProductRequest(name, description, price, quantity, imageUrl, category, available),
                    session);
            ra.addFlashAttribute("msg", "Product created!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller";
    }

    @PostMapping("/seller/products/{id}/update")
    public String sellerUpdateProduct(@PathVariable Long id,
                                      @RequestParam String name,
                                      @RequestParam(required = false) String description,
                                      @RequestParam BigDecimal price,
                                      @RequestParam int quantity,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) String imageUrl,
                                      @RequestParam(required = false) boolean available,
                                      HttpSession session,
                                      RedirectAttributes ra) {
        try {
            sellerService.update(id,
                    new ProductRequest(name, description, price, quantity, imageUrl, category, available), session);
            ra.addFlashAttribute("msg", "Product updated!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller";
    }

    @PostMapping("/seller/products/{id}/delete")
    public String sellerDeleteProduct(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        try {
            sellerService.delete(id, session);
            ra.addFlashAttribute("msg", "Product deleted.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller";
    }

    @PostMapping("/seller/products/{id}/stock")
    public String sellerUpdateStock(@PathVariable Long id,
                                    @RequestParam int quantity,
                                    HttpSession session,
                                    RedirectAttributes ra) {
        try {
            sellerService.updateStock(id, quantity, session);
            ra.addFlashAttribute("msg", "Stock updated!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/seller";
    }

    /* ------------------------------------------------------------------ *
     *  Admin dashboard
     * ------------------------------------------------------------------ */

    @GetMapping("/admin")
    public String admin(@RequestParam(required = false) String search,
                        @RequestParam(required = false) Long edit,
                        Model model,
                        HttpSession session,
                        RedirectAttributes ra) {
        User user = currentUser(session);
        if (user == null) {
            return "redirect:/login?redirect=admin";
        }
        if (user.getRole() != Role.ADMIN) {
            ra.addFlashAttribute("error", "Admin access required.");
            return "redirect:/";
        }
        addNav(model, session);
        List<ProductResponse> products = productService.search(
                blankToNull(search), null, null, null, null, null);
        model.addAttribute("stats", adminService.dashboard(session));
        model.addAttribute("products", products);
        model.addAttribute("orders", orderService.getAllOrders(session));
        model.addAttribute("users", adminService.users(session));
        model.addAttribute("reviews", reviewService.adminList(session));
        model.addAttribute("categories", productService.categories());
        model.addAttribute("productNames", products.stream()
                .collect(java.util.stream.Collectors.toMap(ProductResponse::id, ProductResponse::name)));
        model.addAttribute("search", search);
        model.addAttribute("editProduct", edit == null ? null : productService.getById(edit));
        return "admin";
    }

    @PostMapping("/admin/products")
    public String adminCreateProduct(@RequestParam String name,
                                     @RequestParam(required = false) String description,
                                     @RequestParam BigDecimal price,
                                     @RequestParam int quantity,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) String imageUrl,
                                     @RequestParam(required = false) boolean available,
                                     HttpSession session,
                                     RedirectAttributes ra) {
        try {
            productService.create(new ProductRequest(name, description, price, quantity, imageUrl, category, available),
                    session);
            ra.addFlashAttribute("msg", "Product created!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/products/{id}/update")
    public String adminUpdateProduct(@PathVariable Long id,
                                     @RequestParam String name,
                                     @RequestParam(required = false) String description,
                                     @RequestParam BigDecimal price,
                                     @RequestParam int quantity,
                                     @RequestParam(required = false) String category,
                                     @RequestParam(required = false) String imageUrl,
                                     @RequestParam(required = false) boolean available,
                                     HttpSession session,
                                     RedirectAttributes ra) {
        try {
            productService.update(id,
                    new ProductRequest(name, description, price, quantity, imageUrl, category, available), session);
            ra.addFlashAttribute("msg", "Product updated!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/products/{id}/delete")
    public String adminDeleteProduct(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        try {
            productService.delete(id, session);
            ra.addFlashAttribute("msg", "Product deleted.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/orders/{id}/status")
    public String adminUpdateOrderStatus(@PathVariable Long id,
                                         @RequestParam String status,
                                         HttpSession session,
                                         RedirectAttributes ra) {
        try {
            OrderStatus next = OrderStatus.valueOf(status.toUpperCase());
            orderService.updateStatus(session, id, next);
            ra.addFlashAttribute("msg", "Order #" + id + " status updated to " + status.toUpperCase());
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    @PostMapping("/admin/reviews/{id}/delete")
    public String adminDeleteReview(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        try {
            reviewService.adminDelete(session, id);
            ra.addFlashAttribute("msg", "Review deleted.");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin";
    }

    /* ------------------------------------------------------------------ *
     *  Parsing helpers
     * ------------------------------------------------------------------ */

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean parseAvailable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Boolean.valueOf(value);
    }

    private static String relativePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null ? "" : uri.replaceAll("^/", "");
    }
}