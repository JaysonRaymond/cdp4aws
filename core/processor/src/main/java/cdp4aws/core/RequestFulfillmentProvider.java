package cdp4aws.core;

import java.util.ServiceLoader;

/**
 * @author jraymond
 *         Date: 7/11/14
 *         Time: 10:27 AM
 */
public abstract class RequestFulfillmentProvider {

        private static ServiceLoader<RequestFulfillmentProvider> serviceLoader =
                ServiceLoader.load(RequestFulfillmentProvider.class);

        private static RequestFulfillmentProvider provider;

    /**
     * @return the singleton instance of the RequestFulfillmentProvider as defined in the file
     *         META-INF/RequestFulfillmentProvider
     */
    public static RequestFulfillmentProvider getProvider() {
            if (provider == null) {
                for (RequestFulfillmentProvider aProvider : serviceLoader) {
                    if (aProvider != null) {
                        provider = aProvider;
                        return provider;
                    }
                }
            }
            return provider;
        }

    /**
     * The Request sent from the client is passed in as a String (typically a JSON object) and returns the results as a String.
     * Exceptions can be thrown during fulfillment and will be handled according to the Route definition.
     *
     * @param request The Request String to be forwarded
     * @return A String representing the result of the request
     * @throws Exception to be handled according to the Route definition
     */
    public abstract String fulfill(String request) throws Exception;

}
