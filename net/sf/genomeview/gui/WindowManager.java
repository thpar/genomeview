/**
 * %HEADER%
 */
package net.sf.genomeview.gui;

import jargs.gnu.CmdLineParser.IllegalOptionValueException;
import jargs.gnu.CmdLineParser.Option;
import jargs.gnu.CmdLineParser.UnknownOptionException;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.sf.genomeview.core.Configuration;
import net.sf.genomeview.core.Icons;
import net.sf.genomeview.data.Model;
import net.sf.genomeview.gui.menu.MainMenu;
import net.sf.genomeview.plugin.PluginLoader;
import net.sf.jannot.Cleaner;
import net.sf.jannot.Data;
import net.sf.jannot.DataKey;
import net.sf.jannot.Entry;
import net.sf.jannot.EntrySet;
import be.abeel.jargs.AutoHelpCmdLineParser;

/**
 * MainWindow is the container for a single GenomeView instance.
 * 
 * @author Thomas Abeel
 * 
 */
public class WindowManager extends WindowAdapter implements Observer {
	private static Logger logger = Logger.getLogger(WindowManager.class.getCanonicalName());

	private GenomeViewWindow window = null;

	private GenomeViewWindow helper = null;

	private Model model = null;

	/**
	 * Adds an observer to the model, which will only check if the instance is
	 * still running.
	 * 
	 * @param o
	 */
	void addInstanceObserver(Observer o) {
		model.addObserver(o);
	}

	public WindowManager(String args[], Splash splash) throws InterruptedException, ExecutionException {
		running++;
		logger.info("Started running instance" + running);
		init(args, splash);
	}

	private boolean parse(AutoHelpCmdLineParser parser, String[] args) {
		try {
			parser.parse(args);
			return true;
		} catch (IllegalOptionValueException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			CrashHandler.showErrorMessage("Error while parsing command line arguments: " + e.getMessage()
					+ "\n\nWill continue without command line arguments.", e);
		} catch (UnknownOptionException e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			CrashHandler.showErrorMessage("Error while parsing command line arguments: " + e.getMessage()
					+ "\n\nWill continue without command line arguments.", e);
		}
		return false;

	}

	public void dispose() {
		window.dispose();
		if (helper != null)
			helper.dispose();

	}


	/**
	 * Just see if the user tries to close the window directly and tell the
	 * model if this would happen.
	 */
	@Override
	public void windowClosing(WindowEvent e) {
		this.model.exit();
	}

	private static int running = 0;

	/**
	 * Keeps an eye on the model (used to detect exitRequested) and to detect
	 * whether the used has not loaded any data.
	 * 
	 * 
	 * 
	 * 
	 * @param o
	 * @param arg
	 */
	@Override
	public void update(Observable o, Object arg) {

		if (model.isExitRequested()) {
			model.deleteObserver(this);
			logger.info("Disposing the window in MainWindow.update()");
			dispose();
			try {
				Configuration.save();
			} catch (IOException e) {
				logger.log(Level.SEVERE, "Problem saving configuration", e);
			}
			running--;
			logger.info("Instances still running: " + running);
			if (running < 1) {
				logger.info("No instances left, exiting VM");
				Cleaner.exit();
				for(Frame frame:Frame.getFrames()){
					frame.dispose();
				}
				
				System.exit(0);
			}
		}
		
		
		
		

	}

	public void init(String[] args, Splash splash) throws InterruptedException, ExecutionException {
		// FIXME special handling if this is not the first time the application
		// is initialized

		splash.setText("Parsing parameters...");
		/* Initialize the command line options */
		AutoHelpCmdLineParser parser = new AutoHelpCmdLineParser();
		Option urlO = parser.addHelp(parser.addStringOption("url"), "Start GenomeView with data loaded from the URL");

		Option fileO = parser.addHelp(parser.addStringOption("file"), "Start GenomeView with data loaded from a file.");

		Option configurationO = parser.addHelp(parser.addStringOption("config"),
				"Provide additional configuration to load.");

		Option positionO = parser.addHelp(parser.addStringOption("position"),
				"Provide the initial region that should be visible.");

		Option sessionO = parser.addHelp(parser.addStringOption("session"),
				"Provide a session file that contains all the files that have to be loaded.");

		Option idO = parser.addHelp(parser.addStringOption("id"),
		"Instance ID for this GenomeView instance, useful to control multiple GVs at once.");
				
		boolean goodParse = parse(parser, args);

		if (parser.checkHelp()) {
			System.exit(0);
		}

		splash.setText("Creating windows...");
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		boolean freshwindow = false;
		
		if (model == null) {
			model = new Model((String) parser.getOptionValue(idO));
			model.addObserver(this);
			KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new Hotkeys(model));
		}
		
		if (window == null) {
			freshwindow = true;
			logger.info("Creating new window");
			window = new GenomeViewWindow(model,"GenomeView :: " + Configuration.version(), gs[0].getDefaultConfiguration());
			model.getGUIManager().registerMainWindow(window);
			window.setIconImage(Icons.MINILOGO);
			window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			window.addWindowListener(this);
			window.getRootPane().setTransferHandler(new DropTransferHandler(model));

		}
	
		/* Make sure the model is empty */
		model.setSilent(true);
		model.clearEntries();

		if (freshwindow) {
			JPanel[] content = MainContent.createContent(model, Configuration.getBoolean("dualscreen") ? gs.length : 1);
			window.setContentPane(content[0]);
			window.setJMenuBar(new MainMenu(model));

			window.pack();
			Rectangle rec = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
			window.setSize(rec.width, rec.height);

			if (content.length > 1) {
				for (int i = 1; i < content.length; i++) {
					helper = new GenomeViewWindow(model,"GenomeView :: " + Configuration.version(), gs[i].getDefaultConfiguration());
					helper.setJMenuBar(new MainMenu(model));
					helper.setIconImage(new ImageIcon(this.getClass().getResource("/images/gv2.png")).getImage());
					helper.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
					helper.setContentPane(content[i]);
					helper.setVisible(true);
					helper.pack();
				}
			}
			window.setVisible(true);
			PluginLoader.load(model);

		}
		splash.setText("Loading data...");
		/* Data specified on command line */
		InitDataLoader idl = new InitDataLoader(model);
		if (goodParse) {
			String cmdUrl = (String) parser.getOptionValue(urlO);
			String cmdFile = (String) parser.getOptionValue(fileO);
			String session = (String) parser.getOptionValue(sessionO);
			String[] remArgs = parser.getRemainingArgs();
			String initialLocation = (String) parser.getOptionValue(positionO);
			/* Load the additional configuration */
			String config = (String) parser.getOptionValue(configurationO);
			idl.init(config, cmdUrl, cmdFile, remArgs, initialLocation, session);
		} else {
			idl.init(null, null, null, new String[0], null, null);
		}

		ReferenceMissingMonitor rmm=new ReferenceMissingMonitor(model);
		/* Start acting */
		model.setSilent(false);

	}

}