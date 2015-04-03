package com.github.lookout.whoas;

import io.dropwizard.Configuration;

/**
 * The interface to create the whoas factory based on configuration
 * The client of whoas will call with the configuration to create the whoas factory.
 * @param < T > Configuration class that extends dropwizard configuration
 */
public interface WhoasConfiguration<T extends Configuration> {
    public WhoasFactory getWhoasFactory(T configuration);
}
