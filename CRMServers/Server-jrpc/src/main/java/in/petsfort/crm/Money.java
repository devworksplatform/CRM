package in.petsfort.crm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Decimal-only financial arithmetic. Binary floating point is never used. */
final class Money {
    static final BigDecimal ZERO = BigDecimal.ZERO;
    static final BigDecimal HUNDRED = new BigDecimal("100");
    static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private Money() {}

    static BigDecimal of(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value == null || value.isJsonNull() ? ZERO : new BigDecimal(value.getAsString());
    }

    static BigDecimal of(JsonElement value) {
        return value == null || value.isJsonNull() ? ZERO : new BigDecimal(value.getAsString());
    }

    static BigDecimal rateAfterDiscount(BigDecimal mrp, BigDecimal discountPercent) {
        return mrp.subtract(mrp.multiply(discountPercent).divide(HUNDRED, 12, ROUNDING));
    }

    static BigDecimal percent(BigDecimal amount, BigDecimal percent) {
        return amount.multiply(percent).divide(HUNDRED, 12, ROUNDING);
    }

    static BigDecimal output(BigDecimal value) { return value.setScale(2, ROUNDING); }
    static BigDecimal database(BigDecimal value) { return value.setScale(3, ROUNDING); }

    static void add(JsonObject object, String field, BigDecimal value) {
        object.addProperty(field, output(value));
    }
}
