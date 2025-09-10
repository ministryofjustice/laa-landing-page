package uk.gov.justice.laa.portal.landingpage.validation;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Component
public class BlocklistedEmailDomains {

    private static final Set<String> DEFAULT_BLOCKLIST = BlocklistedEmailDomainsData.ALL;

    private final Set<String> domains;

    public BlocklistedEmailDomains() {
        this.domains = new HashSet<>(DEFAULT_BLOCKLIST);
    }

    public BlocklistedEmailDomains(Set<String> predefinedDomains) {
        this.domains = new HashSet<>();
        if (predefinedDomains != null) {
            for (String d : predefinedDomains) {
                if (d != null) {
                    this.domains.add(d.trim().toLowerCase());
                }
            }
        }
    }

    public boolean isBlocklisted(String domain) {
        if (domain == null) {
            return false;
        }
        return domains.contains(domain.trim().toLowerCase());
    }

    public Set<String> getDomains() {
        return Collections.unmodifiableSet(domains);
    }
}


