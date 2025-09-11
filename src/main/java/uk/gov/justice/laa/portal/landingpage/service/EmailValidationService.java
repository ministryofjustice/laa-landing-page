package uk.gov.justice.laa.portal.landingpage.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.portal.landingpage.validation.BlocklistedEmailDomains;

import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailValidationService {

    private static final ExecutorService executor = new ThreadPoolExecutor(10, 100, 3600,
            TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000));

    private final BlocklistedEmailDomains blocklistedEmailDomains;

    public boolean isValidEmailDomain(String email) {

        if (email == null || !email.contains("@")) {
            return false;
        }

        String domain = email.substring(email.lastIndexOf('@') + 1).toLowerCase(Locale.ROOT).trim();
        if (domain.isEmpty()) {
            return false;
        }

        if (blocklistedEmailDomains.isBlocklisted(domain)) {
            log.debug("Email domain '{}' is blocklisted", domain);
            return false;
        }

        Future<Boolean> future = executor.submit(() -> hasMxRecords(email));

        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (TimeoutException timeoutEx) {
            log.error("The email domain validation took longer than expected. Possibly the email domain is invalid!", timeoutEx);
            throw new RuntimeException("The email domain validation took longer than expected. Possibly the email domain is invalid!");
        } catch (Exception ex) {
            log.error("Error while performing email domain validation. Possibly the email domain is invalid!", ex);
            throw new RuntimeException("Error while performing email domain validation. Possibly the email domain is invalid!");
        }

    }

    /**
     * Checks if the domain of a given email address has valid MX records.
     *
     * @param email The email address to validate.
     * @return true if MX records are found, false otherwise.
     */
    protected boolean hasMxRecords(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }

        String domain = email.substring(email.lastIndexOf('@') + 1);

        try {
            log.debug("Performing MX record lookup for domain: {}", domain);
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns:");

            DirContext dirContext = new InitialDirContext(env);
            Attributes attrs = dirContext.getAttributes(domain, new String[]{"MX"});
            Attribute mxAttr = attrs.get("MX");

            boolean recordsFound = mxAttr != null && mxAttr.size() > 0;
            if (recordsFound) {
                log.debug("Successfully found MX records for domain: {}", domain);
            } else {
                log.debug("No MX records found for domain: {}", domain);
            }
            return recordsFound;

        } catch (NameNotFoundException e) {
            // Non-existent domain
            log.debug("DNS name not found (response code 3) for domain: {}", domain, e);
            return false;
        } catch (NamingException e) {
            // Other DNS issues
            log.debug("DNS lookup failed for domain: {}", domain, e);
            return false;
        }
    }
}