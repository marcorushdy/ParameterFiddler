package parameterfiddler;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.apache.commons.cli.*;

public class ParameterFiddler implements Observer {

    private static String brutaltester;
    private static String refereeCommand;
    private static Bot toImprove;
    private static ArrayList<Bot> opponents = new ArrayList<>();
    private static int rounds = 1;
    private static int threads = 1;
    private static double delta = 0.3;
    private static int players = 2;
    private static boolean swap = false;
    private static boolean verbose = false;
    private static boolean veryVerbose = false;

    public ParameterFiddler() throws IOException {
        ArrayList<Parameter> parameters = new ArrayList<>();
        List<String> lines = Files.readAllLines(Paths.get(toImprove.getParamFile()), Charset.forName("UTF-8"));
        for (String line : lines) {
            parameters.add(new Parameter(line));
        }
        Runner runner = new Runner(toImprove, opponents,
                parameters, brutaltester, refereeCommand,
                rounds, threads, delta, veryVerbose, swap, players);
        runner.addObserver(this);
        Thread thread = new Thread(runner);
        thread.start();
    }

    public static void main(String[] args) throws IOException {
        try {
            Options options = new Options();
            options.addOption("h", false, "help")
                    .addOption("gui", false, "run in GUI mode")
                    .addOption("brutaltester", true, "path to brutaltester .jar file")
                    .addOption("r", true, "referee run command, e.g. -r \"java -jar cotc-referee.jar\"")
                    .addOption("bot", true, "runcommand:paramfile, e.g. -bot \"./bot.exe:params.txt\"")
                    .addOption("opponents", true, "bots to play against, seperated by ;")
                    .addOption("n", true, "games to play")
                    .addOption("t", true, "threads to use")
                    .addOption("p", true, "number of players")
                    .addOption("s", false, "swap players")
                    .addOption("v", false, "print game results")
                    .addOption("vv", false, "print everything")
                    .addOption("delta", true, "parameter mutation range");
            CommandLine cmd = new DefaultParser().parse(options, args);

            if (cmd.hasOption("gui")) {
                FiddlerGUI gui = new FiddlerGUI();
                gui.setVisible(true);
                return;
            }
            if (cmd.hasOption("h") || !cmd.hasOption("brutaltester") || !cmd.hasOption("r") || !cmd.hasOption("bot") || !cmd.hasOption("opponents")) {
                new HelpFormatter().printHelp("-brutaltester <path to brutaltester> "
                        + "-r <referee command line> "
                        + "-bot <bot run command:params.txt path> "
                        + "-opponents <bot1:param1;bot2:param2;...> "
                        + "[-n <games> -t <thread> -delta <parameter mutation range> -s -p <players>]", options);
                System.exit(0);
            }

            brutaltester = cmd.getOptionValue("brutaltester");
            refereeCommand = cmd.getOptionValue("r");
            String[] parts = cmd.getOptionValue("bot").split(":");
            toImprove = new Bot(parts[0], parts[1]);
            for (String opp : cmd.getOptionValue("opponents").split(";")) {
                parts = opp.split(":");
                opponents.add(new Bot(parts[0], parts[1]));
            }
            if (cmd.hasOption("s")) {
                swap = true;
            }
            if (cmd.hasOption("v")) {
                verbose = true;
            }
            if (cmd.hasOption("vv")) {
                veryVerbose = true;
            }

            try {
                rounds = Integer.valueOf(cmd.getOptionValue("n"));
            } catch (Exception exception) {
                //use default number of rounds
            }
            try {
                threads = Integer.valueOf(cmd.getOptionValue("t"));
            } catch (Exception exception) {
                //use default number of threads
            }
            try {
                players = Integer.valueOf(cmd.getOptionValue("p"));
            } catch (Exception exception) {
                //use default number of threads
            }
            try {
                delta = Double.valueOf(cmd.getOptionValue("delta"));
            } catch (Exception exception) {
                //use default value of delta
            }
            new ParameterFiddler();

        } catch (ParseException ex) {
            System.out.println("fail: " + ex);
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        Runner r = (Runner) o;

        ConcurrentLinkedQueue<String> brutaltesterQueue = r.getBrutaltesterQueue();
        while (brutaltesterQueue.size() > 0) {
            String line = brutaltesterQueue.poll();
            if (verbose) {
                System.out.println(line);
            }
        }

        ConcurrentLinkedQueue<String> parameterFiddlerQueue = r.getParameterFiddlerQueue();
        while (parameterFiddlerQueue.size() > 0) {
            System.out.println("#####  " + parameterFiddlerQueue.poll());
        }
    }
}
