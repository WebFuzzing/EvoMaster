package com.foo.rest.emb.json.signalserver;

import org.apache.commons.lang3.StringUtils;
import org.evomaster.client.java.utils.SimpleLogger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

public class AccountsManager {

//    private static final Logger logger = LoggerFactory.getLogger(AccountsManager.class);
    private static final SimpleLogger logger = new SimpleLogger();

    static Optional<Account> parseAccountJson(@Nullable final String accountJson, final UUID uuid) {
        try {
            if (StringUtils.isNotBlank(accountJson)) {
                Account account = SystemMapper.jsonMapper().readValue(accountJson, Account.class);
                account.setUuid(uuid);

                if (account.getPhoneNumberIdentifier() == null) {
//                    logger.warn("Account {} loaded from Redis is missing a PNI", uuid);
                    logger.warn("Account " + uuid + " loaded from Redis is missing a PNI");
                }

                return Optional.of(account);
            }

            return Optional.empty();
        } catch (final IOException e) {
            logger.warn("Deserialization error", e);
            return Optional.empty();
        }
    }
}
