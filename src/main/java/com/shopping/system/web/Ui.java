package com.shopping.system.web;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * View helpers used by the Thymeleaf templates. Keeps formatting logic in Java
 * instead of JavaScript.
 */
@Component
public class Ui {

    private static final BigDecimal USD_TO_INR = new BigDecimal("83.00");

    private static final String DEFAULT_IMAGE = "https://placehold.co/400x300/cccccc/333?text=No+Image";

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Display price in Indian rupees, converted from the stored USD amount. */
    public String money(BigDecimal value) {
        BigDecimal amount = value == null
                ? BigDecimal.ZERO
                : value.multiply(USD_TO_INR).setScale(2, RoundingMode.HALF_UP);
        return "\u20B9" + amount.toPlainString();
    }

    /** Renders a 5-star rating as filled/empty stars. */
    public String stars(Double rating) {
        int rounded = rating == null ? 0 : (int) Math.round(rating);
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i <= rounded) {
                sb.append("\u2605");
            } else {
                sb.append("<span class=\"off\">\u2605</span>");
            }
        }
        return sb.toString();
    }

    /** Renders stars from an integer rating (reviews). */
    public String stars(Integer rating) {
        return stars(rating == null ? null : rating.doubleValue());
    }

    /** Average rating formatted to one decimal place. */
    public String rating(Double average) {
        return average == null ? "0.0" : String.format("%.1f", average);
    }

    /** Enum-ish label, e.g. "PENDING" -> "Pending". */
    public String title(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String norm = value.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(norm.charAt(0)) + norm.substring(1);
    }

    public String dateTime(LocalDateTime value) {
        return value == null ? "" : DATE_TIME.format(value);
    }

    public String date(LocalDateTime value) {
        return value == null ? "" : DATE.format(value);
    }

    /** Placeholder image when a product has none. */
    public String image(String url) {
        return (url == null || url.isBlank()) ? DEFAULT_IMAGE : url;
    }

    /** CSS status badge class, e.g. "status-pending". */
    public String statusClass(String status) {
        return "status-" + (status == null ? "" : status.toLowerCase());
    }

    /** Truncated product description for product cards. */
    public String desc(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 90 ? text.substring(0, 90) + "..." : text;
    }
}