package org.example.service.support;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.example.enums.Currency.getCurrencyCodes;

@Slf4j
@Component
public class ExchangeRateService {

    public static final int COEFFICIENT = 1;
    public static final double ZERO_DOUBLE = 0.0;

    public static final String SET_METHOD = "GET";
    public static final int SUCCESS_CODE = 200;
    public static final String REQUEST = "/latest/";
    public static final String CONVERSION_RATES = "conversion_rates";

    public static final String LINE_BREAK = "\n";
    public static final String FIRST_SPACE = "%-15s";
    public static final String TABLE_SPACE = "%-10s";
    public static final String SPACE_WITH_FORMAT = "%-10.2f";

    @SuppressWarnings("unused")
    @Value("${exchange.rate.api.url}")
    private String apiUrl;

    private static LocalDateTime lastUpdate = null;
    private static final Map<String, Map<String, Double>> currencyTable = new HashMap<>();

    public String getExchangeRatesTable(Set<String> currentCurrencies) {
        log.info("Start generating a currency exchange rate table with currencies: {}", currentCurrencies);
        if (isDataOutdated()) {
            fetchAndUpdateCurrencyTable();
        }
        String result = generateCurrencyTableString(currentCurrencies);

        log.info("Finished generating exchange rate table");
        return result;
    }

    public Double getConvertCurrency(double amount, String fromCurrency, String toCurrency) {
        log.info("Start converting {} {} to {}", amount, fromCurrency, toCurrency);
        if (isDataOutdated()) {
            fetchAndUpdateCurrencyTable();
        }
        Double result = convertCurrency(amount, fromCurrency, toCurrency);

        log.info("Converted {} {} to {} {}", amount, fromCurrency, result, toCurrency);
        return result;
    }

    private static boolean isDataOutdated() {
        boolean outdated = lastUpdate == null || lastUpdate.plusDays(1).isBefore(LocalDateTime.now());
        log.debug("Currency data is outdated: {}", outdated);
        return outdated;
    }

    private void fetchAndUpdateCurrencyTable() {
        log.info("Starts updating currency rates");
        currencyTable.clear();
        for (String baseCurrency : getCurrencyCodes()) {
            try {
                Map<String, Double> exchangeRates = fetchExchangeRates(baseCurrency);
                if (exchangeRates != null) {
                    currencyTable.put(baseCurrency, exchangeRates);
                    log.debug("Fetched and stored rates for {}: {}", baseCurrency, exchangeRates);
                } else {
                    log.error("Failed to fetch rates for {}", baseCurrency);
                }
            } catch (Exception e) {
                log.error("Error fetching rates for {}: {}", baseCurrency, e.getMessage(), e);
            }
        }
        lastUpdate = LocalDateTime.now();
        log.info("Finished updating currency rates at {}", lastUpdate);
    }

    private Map<String, Double> fetchExchangeRates(String baseCurrency) {
        String requestUrl = apiUrl + REQUEST + baseCurrency;
        log.debug("Sending request to URL: {}", requestUrl);

        try {
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(SET_METHOD);

            int responseCode = connection.getResponseCode();
            log.debug("Received HTTP response code: {}", responseCode);

            if (responseCode == SUCCESS_CODE) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                return parseExchangeRates(response.toString());
            } else {
                log.error("Error: Received HTTP response code {} not 200", responseCode);
            }
        } catch (Exception e) {
            log.error("Error requesting exchange rates for {}: {}", baseCurrency, e.getMessage(), e);
        }

        return null;
    }

    private static Map<String, Double> parseExchangeRates(String jsonResponse) {
        JSONObject jsonObject = new JSONObject(jsonResponse);

        if (jsonObject.has(CONVERSION_RATES)) {
            JSONObject rates = jsonObject.getJSONObject(CONVERSION_RATES);
            Map<String, Double> exchangeRates = new HashMap<>();

            for (String key : rates.keySet()) {
                exchangeRates.put(key, rates.getDouble(key) * COEFFICIENT);
            }
            log.debug("Parsed exchange rates: {}", exchangeRates);
            return exchangeRates;
        } else {
            log.error("Error: Invalid API response structure.");
        }

        return null;
    }

    private static String generateCurrencyTableString(Set<String> currentCurrencies) {
        StringBuilder table = new StringBuilder();
        table.append(String.format(FIRST_SPACE, ""));
        for (String currency : currentCurrencies) {
            table.append(String.format(TABLE_SPACE, currency));
        }
        table.append(LINE_BREAK);

        for (String baseCurrency : currentCurrencies) {
            table.append(String.format(TABLE_SPACE, baseCurrency));
            for (String targetCurrency : currentCurrencies) {
                Double rate = currencyTable.getOrDefault(baseCurrency, new HashMap<>()).get(targetCurrency);
                table.append(String.format(SPACE_WITH_FORMAT, rate != null ? rate : ZERO_DOUBLE));
            }
            table.append(LINE_BREAK);
        }

        log.debug("Generated currency exchange rate table:\n{}", table);
        return table.toString();
    }

    private static Double convertCurrency(double amount, String fromCurrency, String toCurrency) {
        if (currencyTable.containsKey(fromCurrency) && currencyTable.get(fromCurrency).containsKey(toCurrency)) {
            double rate = currencyTable.get(fromCurrency).get(toCurrency) / COEFFICIENT;
            return amount * rate;
        } else {
            log.error("Error: Conversion rate not available for {} to {}", fromCurrency, toCurrency);
            return null;
        }
    }
}