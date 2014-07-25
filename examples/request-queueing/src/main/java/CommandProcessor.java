import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The CommandProcessor receives a String request, interprets it into a command and executes it.
 * <p/>
 */
public class CommandProcessor extends RequestFulfillmentProvider {

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
                    InetAddress inetAddress = InetAddress.getLocalHost();
                    return inetAddress.getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                return "{IP Undetermined}";
            }
        },
        UNRECOGNIZED {
            @Override
            String execute() {
                return "Command not recognized.";
            }
        };

        abstract String execute();
    }

    /**
     * Parse the command from the request, execute it and return it's result.
     */
    public String fulfill(String request) {
        Command command;
        try {
            command = Command.valueOf(request.trim().toUpperCase());
        } catch (RuntimeException e) {
            command = Command.UNRECOGNIZED;
        }
        // ...execute the command, ending the results to the message body
        return command.execute();
    }

}
