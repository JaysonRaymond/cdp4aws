package cdp4aws.core;

import com.amazonaws.services.sqs.model.MessageAttributeValue;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.apache.camel.main.Main;
import org.apache.camel.model.RouteDefinition;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Logger;

/**
 * The RequestQueueingProcessor retrieves messages containing Requests from the SQS Requests queue, executes them, and places
 * the results on to the Results queue specified in the message header attribute 'ReplyTo'.
 * <p/>
 * We leverage Apache Camel here as an ideal tool to handle the plumbing, including:
 * - the flexibility and power of a Enterprise Integration Pattern Domain Specific Language
 * - auto-deletion of processed messages
 * - auto queue creation if accessed before creation
 * - graceful shutdown of routes, assuring messages run to completion before exiting
 * - JMX management support
 * - thread pool support
 * - easy to add IoC support (via Guice or Spring)
 *
 * @author jraymond
 *         Date: 6/17/14
 *         Time: 6:09 AM
 */
// TODO Add Response Queue Reaper
public class RequestQueueing {
    public static final Logger LOGGER = Logger.getLogger(RequestQueueing.class.getName());
    public static final String CONFIGURATION_PATH = System.getProperty("user.home")+"/.aws/credentials/AwsCredentials.properties";
    private String accessKey, secretKey, endpoint;

    /**
     * Pass the the request on to the RequestFulfillmentProvider, put the result in the outgoing message body, and assure
     * the CorrelationId has been placed in the header attributes of the response.
     */
    public static class FulfillmentProcessor implements org.apache.camel.Processor {
        private RequestFulfillmentProvider requestFulfiller = RequestFulfillmentProvider.getProvider();

        public final void process(Exchange exchange) throws Exception {
            // Make a copy of the incoming message...
            final Message message = exchange.getIn().copy();
            LOGGER.info("Received: "+message);
            // ... extract the command from the message body
            String request = message.getBody(String.class);
            // ...fulfill the request, putting the results into the response message body
            message.setBody(requestFulfiller.fulfill(request));
            Map<String, Object> messageAttributes = (Map<String, Object>) message.getHeader(SqsConstants.MESSAGE_ATTRIBUTES, new HashMap<String, String>(), Map.class);
            MessageAttributeValue correlationIdAttribute = new MessageAttributeValue();
            correlationIdAttribute.setStringValue((String) message.getHeader(SqsConstants.MESSAGE_ID));
            correlationIdAttribute.setDataType("String");
            messageAttributes.put("CorrelationId", correlationIdAttribute);
            message.setHeader(SqsConstants.MESSAGE_ATTRIBUTES, messageAttributes);
            //  Send it on again, with original headers in tact and only the body changed.
            exchange.setOut(message);
            LOGGER.info("Sent: "+message);
        }
    }

    /**
     * This class establishes the routes from Requests queue, to FulfillmentProcessor, then to the appropriate Results
     * queue.
     *
     * Override this class when you want to delegate fulfilling the request to an external service.
     * To override the default RouteBuilder specify the fully qualified class name of your implementation in the text
     * file META-INF/services/cdp4aws.core.RequestQueueing.RouteBuilder on the classpath.
     */
    public static class RouteBuilder extends org.apache.camel.builder.RouteBuilder {
        private static ServiceLoader<RouteBuilder> serviceLoader =
                ServiceLoader.load(RouteBuilder.class);

        private static RouteBuilder builder;

        public static RouteBuilder getBuilder(String endpoint, String accessKey, String secretKey) {
            if (builder == null) {
                for (RouteBuilder aBuilder : serviceLoader) {
                    if (aBuilder != null) {
                        builder = aBuilder;
                        builder.withEndpoint(endpoint).withAccessKey(accessKey).withSecretKey(secretKey);
                        return builder;
                    }
                }
                // default
                builder = new RouteBuilder().withEndpoint(endpoint).withAccessKey(accessKey).withSecretKey(secretKey);
            }
            return builder;
        }

        private String endpoint;
        private String accessKey;
        private String secretKey;

        /**
         * If needed, users can override this and route through an external service endpoint to resolve this command.
         * For simpler cases, we process the request inline using a FulfillmentProcessor.
         */
        public RouteDefinition addRequestResolverToRoute(RouteDefinition routeDefinition) {
            return routeDefinition.process(new FulfillmentProcessor());
        }

        @Override
        public final void configure() throws Exception {
            addRequestResolverToRoute(from("aws-sqs://Requests?amazonSQSEndpoint=" + endpoint +
                    "&accessKey=" + accessKey +
                    "&secretKey=" + secretKey +
                    // set attributeNames at least twice so it will be parsed as a list:
                    // these are the message header attributes we wish to receive from this endpoint
                    "&messageAttributeNames=ReplyTo&messageAttributeNames=All" +
                    "&attributeNames=ReplyTo&attributeNames=All"))
            // Since the receiving queue is determined at runtime, we use recipientList()
            .recipientList(simple("aws-sqs://" +
                    "${in.header.CamelAwsSqsMessageAttributes[ReplyTo].stringValue}" +
                    "?amazonSQSEndpoint=" + endpoint +
                    "&accessKey=" + accessKey +
                    "&secretKey=" + secretKey))
            .stopOnException(); // Let us now if this is wrong
        }

        public RouteBuilder withSecretKey(String secretKey) {
            this.secretKey = secretKey;
            return this;
        }

        public RouteBuilder withEndpoint(String endpoint) {
            this.endpoint = endpoint;
            return this;
        }

        public RouteBuilder withAccessKey(String accessKey) {
            this.accessKey = accessKey;
            return this;
        }
    }

    /**
     * Extract a specific property, throwing an exception if not found to support fail fast.
     *
     * @param properties
     * @param name
     * @return
     * @throws IllegalArgumentException
     */
    protected static String extractProperty(Properties properties, String name) throws IllegalArgumentException {
        String value = properties.getProperty(name);
        if (value == null) throw new IllegalArgumentException("'"+name+"' missing from "+ CONFIGURATION_PATH);
        return value.trim();
    }

    /**
     * Assure external configuration.
     *
     * @throws java.io.IOException, IllegalArgumentException
     */
    protected void loadConfiguration() throws IOException, IllegalArgumentException {
        Properties properties = new Properties();
       	InputStream input = new FileInputStream(CONFIGURATION_PATH);
        properties.load(input);
        accessKey = extractProperty(properties, "accessKey");
        secretKey = extractProperty(properties, "secretKey");
        endpoint = extractProperty(properties, "endpoint");
    }

    /**
     * Leverage Camel Main for standalone: http://camel.apache.org/running-camel-standalone-and-have-it-keep-running.html
     * @throws Exception
     */
    protected void startup() throws Exception {
        // Fail fast if configuration isn't properly supplied
        loadConfiguration();
        // create a Main instance
        Main main = new Main();
        // enable hangup support so you can press ctrl + c to terminate the JVM
        main.enableHangupSupport();
        // add routes
        main.addRouteBuilder(RouteBuilder.getBuilder(endpoint, accessKey, secretKey));
        // run until you terminate the JVM
        System.out.println("\r\n Starting Camel. Use ctrl + c to terminate the JVM.\r\n");
        main.run();
    }

    /**
     * Launchable from the command line
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        new RequestQueueing().startup();
    }

}
