package com.eatnumber1.soapcli;

import com.eatnumber1.soapi.controller.SoapController;
import com.eatnumber1.soapi.controller.SoapLocation;
import com.eatnumber1.soapi.server.Playlist;
import com.eatnumber1.soapi.server.SongFile;
import com.eatnumber1.soapi.server.SongServer;
import com.eatnumber1.util.MessageBundle;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
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
    private ControllerThread controller;

    public static void main( String[] args ) throws Exception {
        Option localHostOption = new Option("l", "local-host", true, messages.getMessage("com.eatnumber1.soapcli.option.help.local-host"));
        Option soapHostOption = new Option("n", "hostname", true, messages.getMessage("com.eatnumber1.soapcli.option.help.hostname"));
        Option southVaderOption = new Option(null, "south-vader", true, messages.getMessage("com.eatnumber1.soapcli.option.help.south-vader"));

        Options options = new Options();
        options.addOption(localHostOption);
        options.addOption(soapHostOption);
        options.addOption(southVaderOption);

        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse(options, args, false);

        SoapCLI cli;
        SoapController controller = new SoapController();

        URI localUri = controller.getLocalUri();
        if( cmd.hasOption(localHostOption.getOpt()) ) {
            controller.setLocalUri(new URI(localUri.getScheme(), localUri.getUserInfo(), cmd.getOptionValue(localHostOption.getOpt()), localUri.getPort(), localUri.getPath(), localUri.getQuery(), localUri.getFragment()));
        }

        URI soapUri = controller.getSoapUri();
        if( cmd.hasOption(soapHostOption.getOpt()) ) {
            controller.setSoapUri(new URI(soapUri.getScheme(), soapUri.getUserInfo(), cmd.getOptionValue(soapHostOption.getOpt()), soapUri.getPort(), soapUri.getPath(), soapUri.getQuery(), soapUri.getFragment()));
        }

        if( cmd.hasOption(southVaderOption.getLongOpt()) ) {
            controller.setLocation(SoapLocation.SOUTH_VADER);
        } else {
            log.error(messages.getMessage("com.eatnumber1.soapcli.error.no-location"));
            System.exit(1);
        }

        cli = new SoapCLI(controller, cmd.getArgs());
        cli.run();
    }

    public SoapCLI( SoapController controller, String... songs ) throws IOException {
        List<SongFile> songList = new ArrayList<SongFile>(songs.length);
        for( String s : songs ) songList.add(new SongFile(new File(s)));
        Playlist playlist = new Playlist(songList);
        server = new SongServer(playlist);
        this.controller = new ControllerThread(controller);
    }

    public void run() throws Exception {
        new Thread(controller).run();
        server.start();
    }

    public class ControllerThread implements Runnable {
        private SoapController controller;

        public ControllerThread( SoapController controller ) throws IOException {
            this.controller = controller;
        }

        @Override
        public void run() {
            try {
                server.join();
                controller.play();
            } catch( Exception e ) {
                if( e instanceof RuntimeException ) throw (RuntimeException) e;
                throw new RuntimeException(e);
            }
        }
    }
}
