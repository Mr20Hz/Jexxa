package io.jexxa.tutorials.timeservice;

import io.jexxa.core.JexxaMain;
import io.jexxa.infrastructure.drivingadapter.jmx.JMXAdapter;
import io.jexxa.infrastructure.drivingadapter.rest.RESTfulRPCAdapter;
import io.jexxa.tutorials.timeservice.applicationservice.TimeService;
import io.jexxa.utils.JexxaLogger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public final class TimeServiceApplication
{
    //Declare the packages that should be used by Jexxa
    private static final String JMS_DRIVEN_ADAPTER      = TimeServiceApplication.class.getPackageName() + ".infrastructure.drivenadapter.messaging";
    private static final String CONSOLE_DRIVEN_ADAPTER  = TimeServiceApplication.class.getPackageName() + ".infrastructure.drivenadapter.console";
    private static final String OUTBOUND_PORTS          = TimeServiceApplication.class.getPackageName() + ".domainservice";

    public static void main(String[] args)
    {
        JexxaMain jexxaMain = new JexxaMain("TimeService");

        jexxaMain
                //Define which outbound ports should be managed by Jexxa
                .addToApplicationCore(OUTBOUND_PORTS)
                
                //Define which driven adapter should be used by Jexxa
                //Note: We can only register one driven adapter for the
                .addToInfrastructure(getDrivenAdapter(args))

                // Bind a REST and JMX adapter to the TimeService
                // It allows to access the public methods of the TimeService via RMI over REST or Jconsole
                .bind(RESTfulRPCAdapter.class).to(TimeService.class)
                .bind(JMXAdapter.class).to(TimeService.class)

                .bind(JMXAdapter.class).to(jexxaMain.getBoundedContext())
                .bind(RESTfulRPCAdapter.class).to(jexxaMain.getBoundedContext())

                .start()

                .waitForShutdown()

                .stop();
    }

    private static String getDrivenAdapter(String[] args)
    {
        Options options = new Options();
        options.addOption("j", "jms", false, "jms driven adapter");

        CommandLineParser parser = new DefaultParser();
        try
        {
            CommandLine line = parser.parse( options, args );

            if (line.hasOption("jms"))
            {
                return JMS_DRIVEN_ADAPTER;
            }
        }
        catch( ParseException exp ) {
            JexxaLogger.getLogger(TimeServiceApplication.class)
                    .error( "Parsing failed.  Reason: {}", exp.getMessage() );
        }
        return CONSOLE_DRIVEN_ADAPTER;
    }

    private TimeServiceApplication()
    {
        //Private constructor since we only offer main 
    }
}
