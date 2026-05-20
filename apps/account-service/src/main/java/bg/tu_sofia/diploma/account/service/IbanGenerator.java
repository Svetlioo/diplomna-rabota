package bg.tu_sofia.diploma.account.service;

import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * Generates Bulgarian (BG) IBANs with valid ISO 13616 check digits. The BBAN is
 * a fixed demo bank code ("DIPL") followed by 14 random digits, so the result is
 * a well-formed 22-character IBAN that passes MOD-97 validation.
 */
@Component
public class IbanGenerator {

    private static final String COUNTRY_CODE = "BG";
    private static final String BANK_CODE = "DIPL";
    private static final BigInteger MOD = BigInteger.valueOf(97);

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder bban = new StringBuilder(BANK_CODE);
        for (int i = 0; i < 14; i++) {
            bban.append(random.nextInt(10));
        }
        String checkDigits = checkDigits(bban.toString());
        return COUNTRY_CODE + checkDigits + bban;
    }

    private String checkDigits(String bban) {
        // ISO 13616: append country code + "00", convert letters to numbers,
        // then the check digits are 98 - (number mod 97).
        String rearranged = numeric(bban + COUNTRY_CODE + "00");
        int remainder = new BigInteger(rearranged).mod(MOD).intValue();
        int check = 98 - remainder;
        return String.format("%02d", check);
    }

    private String numeric(String input) {
        StringBuilder sb = new StringBuilder(input.length() * 2);
        for (char c : input.toCharArray()) {
            if (Character.isLetter(c)) {
                sb.append(Character.getNumericValue(c)); // A=10 ... Z=35
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
