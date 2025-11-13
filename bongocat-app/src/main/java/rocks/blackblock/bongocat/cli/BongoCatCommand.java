package rocks.blackblock.bongocat.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Main CLI command for Bongo Cat application.
 */
@Command(
    name = "bongocat",
    description = "Bongo Cat Wayland Overlay - An animated cat that reacts to your keyboard input",
    mixinStandardHelpOptions = true,
    version = "Bongo Cat Overlay v1.0.0-SNAPSHOT"
)
public class BongoCatCommand implements Callable<Integer> {

    @Option(
        names = {"-c", "--config"},
        description = "Specify configuration file (default: bongocat.json or bongocat.yaml)",
        paramLabel = "<file>"
    )
    private Path configFile;

    @Option(
        names = {"-t", "--toggle"},
        description = "Toggle bongocat on/off (start if not running, stop if running)"
    )
    private boolean toggle;

    @Option(
        names = {"-d", "--debug"},
        description = "Enable debug logging"
    )
    private boolean debug;

    public Path getConfigFile() {
        return configFile;
    }

    public boolean isToggle() {
        return toggle;
    }

    public boolean isDebug() {
        return debug;
    }

    @Override
    public Integer call() throws Exception {
        // This method will be implemented by the Application class
        // For now, it just serves as a data holder for parsed arguments
        return 0;
    }

    /**
     * Parse command line arguments and return the command instance
     *
     * @param args command line arguments
     * @return parsed command or null if help/version was shown
     */
    public static BongoCatCommand parse(String[] args) {
        BongoCatCommand command = new BongoCatCommand();
        CommandLine cmd = new CommandLine(command);

        try {
            CommandLine.ParseResult parseResult = cmd.parseArgs(args);

            // If help or version was requested, print and return null
            if (cmd.isUsageHelpRequested()) {
                cmd.usage(System.out);
                return null;
            }
            if (cmd.isVersionHelpRequested()) {
                cmd.printVersionHelp(System.out);
                return null;
            }

            return command;

        } catch (CommandLine.ParameterException e) {
            System.err.println(e.getMessage());
            cmd.usage(System.err);
            System.exit(1);
            return null;
        }
    }
}
