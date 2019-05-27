package org.openlumify.core.http;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.openlumify.core.config.Configuration;

@Singleton
public class DefaultHttpRepository extends HttpRepository {
    @Inject
    public DefaultHttpRepository(Configuration configuration) {
        super(configuration);
    }
}
