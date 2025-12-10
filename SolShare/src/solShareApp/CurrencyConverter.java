package solShareApp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurrencyConverter {

    private static final String API_URL = "https://open.er-api.com/v6/latest/";
    private static final Map<String, Map<String, BigDecimal>> cache = new HashMap<>();
    private static final long CACHE_DURATION_MS = 3600000; // 1 hour
    private static long lastCacheTime = 0;

    public static BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        try {
            BigDecimal rate = getRate(fromCurrency, toCurrency);
            if (rate != null) {
                return amount.multiply(rate); // Keep precision, caller handles rounding
            }
        } catch (Exception e) {
            System.err.println("Currency conversion failed: " + e.getMessage());
        }
        
        // Fallback: if conversion fails, return null to signal error.
        return null;
    }

    private static BigDecimal getRate(String from, String to) throws Exception {
        // Check cache first
        if (System.currentTimeMillis() - lastCacheTime < CACHE_DURATION_MS && cache.containsKey(from)) {
            Map<String, BigDecimal> rates = cache.get(from);
            if (rates != null && rates.containsKey(to)) {
                return rates.get(to);
            }
        }

        // Fetch from API
        String urlStr = API_URL + from.toUpperCase();
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int status = conn.getResponseCode();
        if (status != 200) {
            throw new RuntimeException("API returned code " + status);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();
        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
        }
        in.close();
        conn.disconnect();

        String json = content.toString();
        
        // Parse JSON simply using Regex to find rates.
        // This is a naive parser but sufficient for this API structure
        Map<String, BigDecimal> rates = new HashMap<>();
        
        // The API returns something like: "rates":{"USD":1,"EUR":0.92,...}
        // We look for the "rates" object
        int ratesIndex = json.indexOf("\"rates\"");
        if (ratesIndex != -1) {
            String ratesPart = json.substring(ratesIndex);
            int endRates = ratesPart.indexOf("}"); // End of rates object
            if (endRates != -1) {
                ratesPart = ratesPart.substring(0, endRates + 1);
                
                Pattern pattern = Pattern.compile("\"([A-Z]{3})\":([0-9.]+)");
                Matcher matcher = pattern.matcher(ratesPart);
                while (matcher.find()) {
                    String currency = matcher.group(1);
                    String value = matcher.group(2);
                    rates.put(currency, new BigDecimal(value));
                }
            }
        }

        cache.put(from, rates);
        lastCacheTime = System.currentTimeMillis();

        if (rates.containsKey(to)) {
            return rates.get(to);
        } else {
            throw new RuntimeException("Currency " + to + " not found in rates.");
        }
    }
}
