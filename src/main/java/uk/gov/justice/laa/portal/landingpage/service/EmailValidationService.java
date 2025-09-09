package uk.gov.justice.laa.portal.landingpage.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

@Service
public class EmailValidationService {

    private static final Logger log = LoggerFactory.getLogger(EmailValidationService.class);

    /**
     * Checks if the domain of a given email address has valid MX records.
     *
     * @param email The email address to validate.
     * @return true if MX records are found, false otherwise.
     */
    public boolean hasMxRecords(String email) {
        if (email == null || !email.contains("@")) {
            return false;
        }

        String domain = email.substring(email.lastIndexOf('@') + 1);

        try {
            log.info("Performing MX record lookup for domain: {}", domain);
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns:");

            DirContext dirContext = new InitialDirContext(env);
            Attributes attrs = dirContext.getAttributes(domain, new String[]{"MX"});
            Attribute mxAttr = attrs.get("MX");

            boolean recordsFound = mxAttr != null && mxAttr.size() > 0;
            if (recordsFound) {
                log.info("Successfully found MX records for domain: {}", domain);
            } else {
                log.warn("No MX records found for domain: {}", domain);
            }
            return recordsFound;

        } catch (NamingException e) {
            log.error("DNS lookup failed for domain: {}", domain, e);
            return false;
        }
    }
}