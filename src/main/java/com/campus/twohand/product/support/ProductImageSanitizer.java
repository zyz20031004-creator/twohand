package com.campus.twohand.product.support;

import java.util.List;
import java.util.Locale;

/**
 * 商品图片清洗工具：
 * 1. 过滤明显无效的图片协议
 * 2. 过滤项目里历史遗留的游戏截图/无关测试图
 * 3. 给商品场景提供统一占位图路径
 */
public final class ProductImageSanitizer {

    public static final String DEFAULT_PRODUCT_PLACEHOLDER = "/upload/product/product_placeholder.svg";

    private static final List<String> BLOCKED_FILE_MARKERS = List.of(
            "1772470139664_iphone1.png",
            "1772470150961_iphone2.png",
            "1772470223564_iphone1.png",
            "1772541103200_iphone2.png",
            "1772608234000_book1.png",
            "1772608241751_1772470139664_iphone1.png",
            "1772610361936_1772608241751_1772470139664_iphone1.png",
            "1772610646218_iphone2.png",
            "1772612730195_1772608234000_book1.png",
            "7rhgx5ux5fljh82e.jpg",
            "b1867326e1ec34eba04d7b407d6ec2ddee609171a6feec3ecd32fc87d016c0b4.jpg",
            "1688521133714449.png",
            "r.jpg",
            "r (1).jpg"
    );

    private ProductImageSanitizer() {
    }

    public static String sanitize(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }

        String lowerText = text.toLowerCase(Locale.ROOT);
        if (lowerText.startsWith("blob:")
                || lowerText.startsWith("data:")
                || lowerText.startsWith("file:")) {
            return null;
        }

        for (String marker : BLOCKED_FILE_MARKERS) {
            if (lowerText.contains(marker)) {
                return null;
            }
        }
        return text;
    }

    public static String coverOrPlaceholder(Object value) {
        String sanitized = sanitize(value);
        return sanitized != null ? sanitized : DEFAULT_PRODUCT_PLACEHOLDER;
    }
}
