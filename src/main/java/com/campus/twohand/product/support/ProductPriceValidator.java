package com.campus.twohand.product.support;

import java.math.BigDecimal;

public final class ProductPriceValidator {

    private static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99");

    private ProductPriceValidator() {
    }

    public static BigDecimal validate(Object value) {
        String text = value == null ? null : String.valueOf(value).trim();
        if (text == null || text.isEmpty()) {
            throw new RuntimeException("商品价格不能为空");
        }

        BigDecimal price;
        try {
            price = new BigDecimal(text);
        } catch (NumberFormatException ignored) {
            throw new RuntimeException("商品价格格式不正确");
        }

        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("商品价格必须大于 0");
        }
        if (price.scale() > 2) {
            throw new RuntimeException("商品价格最多保留两位小数");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new RuntimeException("商品价格不能超过 99999999.99");
        }
        return price;
    }
}
