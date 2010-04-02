package com.eatnumber1.soapcli;

import com.eatnumber1.soapi.controller.SoapController;
import com.eatnumber1.soapi.controller.SoapControllerFactory;
import com.eatnumber1.soapi.controller.SoapLocation;
import com.eatnumber1.soapi.server.Playlist;
import com.eatnumber1.soapi.server.SongFile;
import com.eatnumber1.soapi.server.SongServer;
import com.eatnumber1.util.MessageBundle;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Russell Harmon
 * @since Mar 29, 2010
 */
public class SoapCLI {
    private static Logger log = LoggerFactory.getLogger(SoapCLI.class);
    private static MessageBundle messages = MessageBundle.getMessageBundle(SoapCLI.class);
    private SongServer server;
    private SoapController controller;

    public static void main( String[] args ) throws ParseException, URISyntaxException, IOException {
        BasicConfigurator.configure();

        Option verboseOption = new Option("v", "verbose", false, "Enable verbose (INFO) messages");
        Option debugOption = new Option("d", "debug", false, "Enable debugging messages");
        Option helpOption = new Option("h", "help", false, "Print this message");
        Option localHostOption = new Option(null, "local-host", true, messages.getMessage("com.eatnumber1.soapcli.option.help.local-host"));
        Option localPortOption = new Option(null, "local-port", true, messages.getMessage("com.eatnumber1.soapcli.option.help.local-port"));
        Option soapHostOption = new Option(null, "soap-host", true, messages.getMessage("com.eatnumber1.soapcli.option.help.soap-host"));
        Option soapPortOption = new Option(null, "soap-port", true, messages.getMessage("com.eatnumber1.soapcli.option.help.soap-port"));
        Option southVaderOption = new Option("sv", "south-vader", false, messages.getMessage("com.eatnumber1.soapcli.option.help.south-vader"));
        Option northVaderOption = new Option("nv", "north-vader", false, messages.getMessage("com.eatnumber1.soapcli.option.help.north-vader"));
        Option lOption = new Option("l", "the-l", false, messages.getMessage("com.eatnumber1.soapcli.option.help.l"));
        Option southStairsOption = new Option("ss", "south-stairs", false, messages.getMessage("com.eatnumber1.soapcli.option.help.south-stairs"));
        Option northStairsOption = new Option("ns", "north-stairs", false, messages.getMessage("com.eatnumber1.soapcli.option.help.north-stairs"));

        Options options = new Options();
        options.addOption(verboseOption);
        options.addOption(debugOption);
        options.addOption(helpOption);
        options.addOption(localHostOption);
        options.addOption(localPortOption);
        options.addOption(soapHostOption);
        options.addOption(soapPortOption);
        options.addOption(southVaderOption);
        options.addOption(northVaderOption);
        options.addOption(lOption);
        options.addOption(southStairsOption);
        options.addOption(northStairsOption);

        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse(options, args, false);

        SoapCLI cli;
        SoapControllerFactory controllerFactory = new SoapControllerFactory();

        org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getLogger("com.eatnumber1");
        if( cmd.hasOption(debugOption.getLongOpt()) ) {
            rootLogger.setLevel(Level.DEBUG);
        } else {
            org.apache.log4j.Logger.getRootLogger().setLevel(Level.WARN);
            if( cmd.hasOption(verboseOption.getLongOpt()) ) {
                rootLogger.setLevel(Level.INFO);
            } else {
                rootLogger.setLevel(Level.WARN);
            }
        }
        if( cmd.hasOption(helpOption.getLongOpt()) ) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("soapcli [options] (location option) songs...", options);
            System.exit(0);
        }
        if( cmd.hasOption(localHostOption.getLongOpt()) )
            controllerFactory.setLocalHost(cmd.getOptionValue(localHostOption.getLongOpt()));
        if( cmd.hasOption(localPortOption.getLongOpt()) )
            controllerFactory.setLocalPort(Integer.valueOf(cmd.getOptionValue(localPortOption.getLongOpt())));
        if( cmd.hasOption(soapHostOption.getLongOpt()) )
            controllerFactory.setSoapHost(cmd.getOptionValue(soapHostOption.getLongOpt()));
        if( cmd.hasOption(soapPortOption.getLongOpt()) )
            controllerFactory.setSoapPort(Integer.valueOf(cmd.getOptionValue(soapPortOption.getLongOpt())));
        if( cmd.hasOption(southVaderOption.getLongOpt()) ) {
            controllerFactory.setLocation(SoapLocation.SOUTH_VADER);
        } else if( cmd.hasOption(northVaderOption.getLongOpt()) ) {
            controllerFactory.setLocation(SoapLocation.NORTH_VADER);
        } else if( cmd.hasOption(lOption.getLongOpt()) ) {
            controllerFactory.setLocation(SoapLocation.THE_L);
        } else if( cmd.hasOption(southStairsOption.getLongOpt()) ) {
            controllerFactory.setLocation(SoapLocation.SOUTH_STAIRS);
        } else if( cmd.hasOption(northStairsOption.getLongOpt()) ) {
            controllerFactory.setLocation(SoapLocation.NORTH_STAIRS);
        } else {
            log.error(messages.getMessage("com.eatnumber1.soapcli.error.no-location"));
            System.exit(1);
        }

        cli = new SoapCLI(controllerFactory.produce(), cmd.getArgs());
        cli.run();
    }

    public SoapCLI( SoapController controller, String... songs ) throws IOException {
        List<SongFile> songList = new ArrayList<SongFile>(songs.length);
        for( String s : songs ) songList.add(new SongFile(new File(s)));
        Playlist playlist = new Playlist(songList);
        server = new SongServer(playlist);
        this.controller = controller;
    }

    public void run() {
        ExecutorService threadPool = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread( Runnable r ) {
                return new Thread(r, "SoapCLI Thread");
            }
        });
        List<Future<Void>> futures = new ArrayList<Future<Void>>(2);
        try {
            futures.add(threadPool.submit(new ControllerCallable()));
            futures.add(threadPool.submit(new ServerCallable()));
            for( Future<Void> f : futures ) f.get();
        } catch( Exception e ) {
            threadPool.shutdownNow();
        } finally {
            threadPool.shutdown();
        }
    }

    public class ServerCallable implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            server.start();
            return null;
        }
    }

    public class ControllerCallable implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            server.waitForStart();
            controller.play();
            return null;
        }
    }
}
