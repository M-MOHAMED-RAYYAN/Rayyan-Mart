package com.shopping.system.service.chat;

import com.shopping.system.model.Product;
import com.shopping.system.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Deterministic, product-aware fallback provider. Requires no API key, so the
 * assistant always works out of the box and can be tested without any external
 * call. Answers are restricted to the RayyanMart shopping/product domain.
 */
@Component
public class FAQChatProvider implements ChatProvider {

    /** Stored prices are USD; the store front displays INR, so do the same. */
    private static final BigDecimal USD_TO_INR = new BigDecimal("83.00");

    private static final Pattern GREETING =
            Pattern.compile("\\b(hi|hello|hey|namaste|good (morning|afternoon|evening))\\b");
    private static final Pattern THANKS = Pattern.compile("\\b(thank|thanks|thankyou)\\b");
    private static final Pattern WELCOME_INTRO = Pattern.compile("\\b(help|what can you|who are you)\\b");
    private static final Pattern PRICE = Pattern.compile("\\b(price|prices|cost|cheap|cheapest|expensive|how much)\\b");
    private static final Pattern CATEGORY = Pattern.compile("\\b(categor(y|ies))\\b");
    private static final Pattern STOCK = Pattern.compile("\\b(stock|in stock|available|out of stock|availability)\\b");
    private static final Pattern RECOMMEND = Pattern.compile("\\b(recommend|best|popular|favourite|favorite|suggest|top)\\b");
    private static final Pattern PRODUCTS = Pattern.compile("\\b(products?|what do you sell|list)\\b");
    private static final Pattern ORDER_HOW = Pattern.compile("\\b(how (do|can).*(buy|order)|place an? order|place order|checkout|add to cart|buy)\\b");
    private static final Pattern DELIVERY = Pattern.compile("\\b(deliver|delivery|shipping|ship|arrive|arrival)\\b");
    private static final Pattern RETURN = Pattern.compile("\\b(return|refund|exchange|cancel order)\\b");
    private static final Pattern PAYMENT = Pattern.compile("\\b(pay|payment|payments|card|cash on delivery|cod)\\b");
    private static final Pattern REVIEW = Pattern.compile("\\b(review|rating|rate|reviews)\\b");
    private static final Pattern SELLER = Pattern.compile("\\b(be(come)? a seller|seller account|sell|selling)\\b");
    private static final Pattern ACCOUNT = Pattern.compile("\\b(register|sign up|create an? account|login|log in|logout)\\b");
    private static final Pattern CONTACT = Pattern.compile("\\b(contact|support|complaint|help desk)\\b");

    private final ProductRepository productRepository;

    public FAQChatProvider(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public String answer(String message) {
        String q = message.trim().toLowerCase();

        if (GREETING.matcher(q).find()) {
            return "Hello! I am the RayyanMart shopping assistant \uD83D\uDC4B. You can ask me about our products, "
                    + "prices, categories, stock, how to place an order, delivery, returns and payments. "
                    + "Type a question to get started.";
        }
        if (THANKS.matcher(q).find()) {
            return "You're welcome! If you need anything else about RayyanMart, just ask.";
        }
        if (WELCOME_INTRO.matcher(q).find()) {
            return "I'm the RayyanMart assistant. I can help you find products, check prices and stock, and explain "
                    + "how ordering, delivery, payments and returns work on RayyanMart.";
        }
        if (PRICE.matcher(q).find()) {
            return answerPrices();
        }
        if (CATEGORY.matcher(q).find()) {
            return answerCategories();
        }
        if (PRODUCTS.matcher(q).find()) {
            return answerProductList();
        }
        if (PAYMENT.matcher(q).find()) {
            return "RayyanMart supports Cash on Delivery (COD) as the payment method. No payment is taken online "
                    + "and no money is transferred at the time of ordering \u2014 there is no payment gateway. When you "
                    + "click \u201CPlace Order\u201D a COD order is created with a PENDING status and the product stock is "
                    + "reduced immediately; you pay in cash when the order arrives. The seller then reviews the order "
                    + "and updates its status (Confirmed \u2192 Processing \u2192 Shipped \u2192 Delivered) on the Orders page. "
                    + "All prices are shown in Indian rupees (\u20B9).";
        }
        if (STOCK.matcher(q).find()) {
            return answerStock();
        }
        if (RECOMMEND.matcher(q).find()) {
            return answerRecommendations();
        }
        if (ORDER_HOW.matcher(q).find()) {
            return "Placing an order on RayyanMart is easy: 1) create an account (or log in), 2) browse the Products "
                    + "page and open a product you like, 3) click \u201CAdd to Cart\u201D, 4) open your cart to update "
                    + "quantities, and 5) go to Checkout and click \u201CPlace Order\u201D. Stock is validated automatically "
                    + "and your order appears under your order history.";
        }
        if (DELIVERY.matcher(q).find()) {
            return "RayyanMart processes orders within 1\u20132 business days. After the seller confirms the order, "
                    + "shipping typically takes 3\u20137 business days depending on your location. You can follow the "
                    + "status on the Order History page (Pending \u2192 Confirmed \u2192 Processing \u2192 Shipped \u2192 Delivered).";
        }
        if (RETURN.matcher(q).find()) {
            return "You can request a return or refund while an order is still PENDING or CONFIRMED. Contact RayyanMart "
                    + "support with your order number and reason, and the seller will review it. Once an order is "
                    + "Shipped or Delivered, returns are subject to the seller's policy.";
        }
        if (REVIEW.matcher(q).find()) {
            return "After you receive an order you can rate the products (1\u20135 stars) and leave a written review on "
                    + "the product page. You can only review products you have purchased, and you can edit or delete "
                    + "your own review at any time.";
        }
        if (SELLER.matcher(q).find()) {
            return "To sell on RayyanMart, register an account and choose the \u201CSeller\u201D role. Sellers get a "
                    + "dashboard where they can add products, manage stock and view orders that contain their products.";
        }
        if (ACCOUNT.matcher(q).find()) {
            return "You can register on the Register page with your name, email and a password of at least 6 "
                    + "characters. RayyanMart uses the same account to shop, order and (if you choose the Seller role) "
                    + "sell products.";
        }
        if (CONTACT.matcher(q).find()) {
            return "For help and complaints, contact RayyanMart support via the contact details published on the "
                    + "project website, quoting your order number if applicable.";
        }

        String productAnswer = answerMatchingProduct(q);
        if (productAnswer != null) {
            return productAnswer;
        }

        return "I can only help with questions about RayyanMart shopping \u2014 products, prices, categories, stock, "
                + "orders, delivery, payments and returns. Try asking something like \u201Cshow me the products\u201D, "
                + "\u201Cwhat is the price?\u201D or \u201Chow do I place an order?\u201D.";
    }

    private String answerMatchingProduct(String q) {
        List<Product> products = productRepository.findAll(Sort.by("name"));
        for (Product product : products) {
            if (product.getName() != null && q.contains(product.getName().toLowerCase())) {
                String availability = Boolean.TRUE.equals(product.getAvailable()) && product.getQuantity() > 0
                        ? "In stock (" + product.getQuantity() + " units available)"
                        : "Currently out of stock";
                return product.getName() + " \u2014 " + money(product.getPrice()) + ". " + availability + "."
                        + (product.getDescription() == null || product.getDescription().isBlank()
                        ? "" : " Description: " + product.getDescription().trim());
            }
        }
        return null;
    }

    private String answerPrices() {
        List<Product> products = availableProducts();
        if (products.isEmpty()) {
            return "There are no products to show right now. Please check back soon!";
        }
        List<Product> cheapest = products.stream()
                .sorted(Comparator.comparing(Product::getPrice))
                .limit(5)
                .toList();
        return "Here are some of our products with their prices:\n"
                + cheapest.stream()
                .map(p -> "  \u2022 " + p.getName() + " \u2014 " + money(p.getPrice()))
                .collect(Collectors.joining("\n"))
                + "\nPrices on the store front are displayed in Indian rupees (\u20B9).";
    }

    private String answerCategories() {
        List<String> categories = productRepository.findAll(Sort.by("category")).stream()
                .map(Product::getCategory)
                .filter(c -> c != null && !c.isBlank())
                .distinct()
                .sorted()
                .toList();
        if (categories.isEmpty()) {
            return "There are no product categories yet.";
        }
        return "RayyanMart currently has these product categories: " + String.join(", ", categories) + ".";
    }

    private String answerStock() {
        List<Product> products = productRepository.findAll();
        long available = products.stream().filter(p -> Boolean.TRUE.equals(p.getAvailable()) && p.getQuantity() > 0).count();
        long outOfStock = products.size() - available;
        return "Currently " + available + " product" + (available == 1 ? " is" : "s are") + " in stock"
                + " and " + outOfStock + " product" + (outOfStock == 1 ? " is" : "s are") + " out of stock."
                + " Stock is updated automatically whenever an order is placed.";
    }

    private String answerRecommendations() {
        List<Product> products = availableProducts();
        if (products.isEmpty()) {
            return "No products are available right now. Please check back soon!";
        }
        List<Product> top = products.stream().limit(5).toList();
        return "Here are a few products worth checking out on RayyanMart:\n"
                + top.stream()
                .map(p -> "  \u2022 " + p.getName() + " \u2014 " + money(p.getPrice()))
                .collect(Collectors.joining("\n"))
                + "\nOpen the Products page to see the full catalogue.";
    }

    private String answerProductList() {
        List<Product> products = availableProducts();
        if (products.isEmpty()) {
            return "There are no products available right now. Please check back soon!";
        }
        List<String> names = products.stream().map(Product::getName).toList();
        return "We sell " + products.size() + " product" + (products.size() == 1 ? "" : "s")
                + (names.isEmpty() ? "." : " right now: " + String.join(", ", names) + ".");
    }

    private List<Product> availableProducts() {
        return productRepository.findAll(Sort.by("name")).stream()
                .filter(p -> Boolean.TRUE.equals(p.getAvailable()) && p.getQuantity() > 0)
                .toList();
    }

    private static String money(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value.multiply(USD_TO_INR);
        return "\u20B9" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}