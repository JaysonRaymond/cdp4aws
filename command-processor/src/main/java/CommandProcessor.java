import com.amazonaws.services.sqs.model.MessageAttributeValue;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.aws.sqs.SqsConstants;
import org.apache.camel.main.Main;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * The CommandProcessor retrieves messages containing Commands from the SQS Commands queue, executes them, and places
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
public class CommandProcessor {
    public static final Logger LOGGER = Logger.getLogger(CommandProcessor.class.getName());
    public static final String CONFIGURATION_PATH = System.getProperty("user.home")+"/.aws/credentials/AwsCredentials.properties";
    private String accessKey, secretKey, endpoint;

    /**
     * The currently available Commands
     */
    protected enum Command {
        TIME {
            @Override
            String execute() {
                DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                return formatter.format(new Date());
            }
        },
        IP {
            @Override
            String execute() {
                try {
                    InetAddress inetAddress = InetAddress.getLocalHost();  // .getLocalAddr()?
                    return inetAddress.getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                return "{IP Undetermined}";
            }
        },
        UNRECOGNIZED { @Override String execute() { return "Command not recognized."; } };

        abstract String execute();
    }

    /**
     * Parse the command from the request, execute it and put it's result in the outgoing message.
     */
    protected static class CommandExecutionProcessor implements Processor {

        public void process(Exchange exchange) throws Exception {
            // Make a copy of the incoming message...
            final Message message = exchange.getIn().copy();
            LOGGER.info("Received: "+message);
            // ... extract the command from the message body
            String body = message.getBody(String.class);
            Command command;
            try { command = Command.valueOf(body.trim().toUpperCase()); }
            catch (RuntimeException e) { command = Command.UNRECOGNIZED; }
            // ...execute the command, ending the results to the message body
            message.setBody(command.execute());
            Map<String, Object> messageAttributes = (Map<String, Object>) message.getHeader(SqsConstants.MESSAGE_ATTRIBUTES, new HashMap<String, String>(), Map.class);
            MessageAttributeValue inResponseToAttribute = new MessageAttributeValue();
            inResponseToAttribute.setStringValue((String) message.getHeader(SqsConstants.MESSAGE_ID));
            inResponseToAttribute.setDataType("String");
            messageAttributes.put("InResponseTo", inResponseToAttribute);
            message.setHeader(SqsConstants.MESSAGE_ATTRIBUTES, messageAttributes);
            //  Send it on again, with original headers in tact and only the body changed.
            exchange.setOut(message);
            LOGGER.info("Sent: "+message);
        }
    }

    /**
     * This class establishes the routes from Commands queue, to CommandExecutionProcessor, then to the Results queue.
     */
    protected class CommandRouteBuilder extends RouteBuilder {

        @Override
        public void configure() throws Exception {
            from("aws-sqs://Commands?amazonSQSEndpoint="+ endpoint +
                    "&accessKey="+accessKey+
                    "&secretKey="+secretKey+
                    // set attributeNames at least twice so it will be parsed as a list:
                    // these are the message header attributes we wish to receive from this endpoint
                    "&messageAttributeNames=ReplyTo&messageAttributeNames=All"+
                    "&attributeNames=ReplyTo&attributeNames=All")
            .process(new CommandExecutionProcessor())
            // Since the receiving queue is determined at runtime, we use recipientList()
            .recipientList(simple("aws-sqs://"+
                                 "${in.header.CamelAwsSqsMessageAttributes[ReplyTo].stringValue}"+
                                 "?amazonSQSEndpoint="+ endpoint+
                                 "&accessKey="+accessKey+
                                 "&secretKey="+secretKey))
            .stopOnException(); // Let us now if this is wrong
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
     * @throws IOException, IllegalArgumentException
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
        main.addRouteBuilder(new CommandRouteBuilder());
        // run until you terminate the JVM
        System.out.println("\r\n Starting Camel. Use ctrl + c to terminate the JVM.\r\n");
        main.run();
    }

    /**
     * Launchable form the command line
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        new CommandProcessor().startup();
    }

}
