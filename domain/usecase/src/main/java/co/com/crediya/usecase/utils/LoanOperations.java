package co.com.crediya.usecase.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;


public class LoanOperations {

    public static BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal annualRate, int termMonths) {
        if (annualRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(termMonths), 2, RoundingMode.HALF_UP);
        }
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        BigDecimal numerator = monthlyRate.multiply((monthlyRate.add(BigDecimal.ONE)).pow(termMonths));
        BigDecimal denominator = ((monthlyRate.add(BigDecimal.ONE)).pow(termMonths)).subtract(BigDecimal.ONE);

        return principal.multiply(numerator.divide(denominator, 10, RoundingMode.HALF_UP))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
