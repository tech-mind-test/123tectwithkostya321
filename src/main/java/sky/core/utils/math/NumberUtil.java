package sky.core.utils.math;

import sky.core.utils.Wrapper;

import java.util.Locale;

public class NumberUtil implements Wrapper {

    public static String toRomanNumeral(int number) {
        if (number < 1 || number > 3999) {
            return String.valueOf(number);
        }

        String[] thousands = {"", "M", "MM", "MMM"};
        String[] hundreds = {"", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM"};
        String[] tens = {"", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC"};
        String[] ones = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX"};

        return thousands[number / 1000] + hundreds[(number % 1000) / 100] + tens[(number % 100) / 10] + ones[number % 10];
    }

    public static String formatThousands(String input) {
        if (input == null || input.isEmpty()) return "";
        String digitsOnly = input.replaceAll("[^0-9]", "");
        digitsOnly = digitsOnly.replaceFirst("^0+(?!$)", "");
        if (digitsOnly.isEmpty()) return "";
        try {
            long value = Long.parseLong(digitsOnly);
            return String.format(Locale.US, "%,d", value);
        } catch (NumberFormatException e) {
            return "";
        }
    }

    public static int countDigits(String input) {
        if (input == null || input.isEmpty()) return 0;
        int cnt = 0;
        for (int i = 0; i < input.length(); i++) {
            if (Character.isDigit(input.charAt(i))) cnt++;
        }
        return cnt;
    }
}
