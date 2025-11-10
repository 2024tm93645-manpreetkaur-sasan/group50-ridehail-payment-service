package com.rhf.payment.dataseeder;

import com.rhf.payment.entity.Payment;
import com.rhf.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Configuration
public class CsvDataLoader {

    private static final Logger log = LoggerFactory.getLogger(CsvDataLoader.class);

    // CSV format example: 10/18/2023 22:02
    private static final DateTimeFormatter CSV_DATE_FORMAT =
            DateTimeFormatter.ofPattern("M/d/yyyy HH:mm");

    @Bean
    CommandLineRunner loadCsv(PaymentRepository repo) {
        return args -> {

            log.info("=== CsvDataLoader INITIALIZING ===");

            long existing = repo.count();
            if (existing > 0) {
                log.info("Payments table already has {} rows. Skipping CSV seed.", existing);
                return;
            }

            log.info("Payments table empty. Loading rhfd_payments.csv...");

            try {
                InputStream is =
                        getClass().getClassLoader().getResourceAsStream("rhfd_payments.csv");

                if (is == null) {
                    log.error(" CSV file missing: classpath:rhfd_payments.csv");
                    return;
                }

                log.info(" CSV found. Starting import...");

                try (BufferedReader br =
                             new BufferedReader(
                                     new InputStreamReader(is, StandardCharsets.UTF_8))) {

                    br.lines().skip(1).forEach(line -> {
                        try {
                            String[] t = line.split(",");
                            if (t.length < 7) {
                                log.error("Invalid CSV row: {}", line);
                                return;
                            }

                            Payment p = new Payment();
                            p.setTripId(Long.valueOf(t[1]));
                            p.setAmount(new BigDecimal(t[2]));
                            p.setMethod(t[3]);
                            p.setStatus(t[4]);
                            p.setReference(t[5]);

                            // Parse date
                            String rawDate = t[6].trim().replace(" ", "T") + "Z";
                            p.setCreatedAt(Instant.parse(rawDate));

                            repo.save(p);
                        } catch (Exception e) {
                            log.error("Error processing row: {}", line, e);
                        }
                    });


                    long finalCount = repo.count();
                    log.info(" CSV import completed. Total rows now: {}", finalCount);

                }
            } catch (Exception ex) {
                log.error(" FAILED to seed rhfd_payments.csv", ex);
            }
        };
    }
}
