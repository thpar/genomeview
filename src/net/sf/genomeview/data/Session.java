/**
 * %HEADER%
 */
package net.sf.genomeview.data;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import be.abeel.io.LineIterator;
import be.abeel.net.URIFactory;
import net.sf.genomeview.core.Configuration;
import net.sf.genomeview.gui.CrashHandler;
import net.sf.genomeview.gui.MessageManager;
import net.sf.genomeview.gui.dialog.TryAgainHandler;
import net.sf.genomeview.gui.external.ExternalHelper;
import net.sf.genomeview.plugin.PluginLoader;
import net.sf.jannot.Location;
import net.sf.jannot.source.DataSource;
import net.sf.jannot.source.Locator;
import net.sf.nameservice.NameService;

/**
 * 
 * @author Thomas Abeel
 * 
 */
public class Session {

	public static Thread loadSession(Model model, String in) throws IOException {
		log.debug("Loading session from String: " + in);
		if (in.startsWith("http://") || in.startsWith("https://")) {
			try {
				return loadSession(model, URIFactory.url(in));
			} catch (MalformedURLException e) {
				CrashHandler.showErrorMessage("Failed to load from URL: " + in, e);
				return null;
			} catch (URISyntaxException e) {
				CrashHandler.showErrorMessage("Failed to load from URL: " + in, e);
				return null;
			}
		} else {
			return loadSession(model, new File(in));
		}
	}

	public static Thread loadSession(Model model, File selectedFile) throws FileNotFoundException {
		log.debug("Loading session from File: " + selectedFile);
		return loadSession(model, new FileInputStream(selectedFile));

	}

	public static Thread loadSession(Model model, URL url) throws IOException {
		log.debug("Loading session from URL: " + url);
		return loadSession(model, url.openStream());
	}

	private static Logger log = LoggerFactory.getLogger(Session.class.getCanonicalName());

	enum SessionInstruction {
		PREFIX, CONFIG, DATA, OPTION, LOCATION, PLUGIN, ALIAS, C, U, F,EXTRA;
	}

	/**
	 * Asynchronous loading of a session file
	 * 
	 * @param model
	 *            model to load the session into
	 * @param is
	 *            inputstream that contains the session
	 * @return thread loading the session
	 */
	private static Thread loadSession(final Model model, final InputStream is) {
		model.messageModel().setStatusBarMessage(MessageManager.getString("session.preparing_load_session"));

		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				LineIterator it = new LineIterator(is, false, true);

				try {
					String key = it.next();
					String lcKey = key.toLowerCase();
					if (!(lcKey.contains("genomeview") && lcKey.contains("session"))) {
						JOptionPane.showMessageDialog(model.getGUIManager().getParent(), MessageManager.getString("session.not_genome_view_session"));
					} else {

						model.clearEntries();
						String prefix = "";
						for (String line : it) {
							try {
								if (line.trim().startsWith("#") || line.trim().isEmpty())
									continue;
								char firstchar = line.toUpperCase().charAt(0);

								String[] arr = line.split("[: \t]", 2);
								
								model.messageModel().setStatusBarMessage(
										MessageManager.formatMessage("session.loading_session_current_file_line", new Object[] { line }));
								SessionInstruction si = null;
								try {
									si = SessionInstruction.valueOf(arr[0].toUpperCase());
								} catch (Exception e) {
									log.warn("Could not parse: " + arr[0] + "\n Unknown instruction.\nCould not load session line: " + line, e);
								}

								if (si != null) {
									try {
										switch (si) {
										case PREFIX:
											if(arr.length==1)
												prefix="";
											else
												prefix = arr[1].trim();
											break;
										case EXTRA:
											model.getExtraSessionFiles().addElement(arr[1]);
											log.warn("Extra file: "+arr[1]);
											break;
										case U:
										case F:
										case DATA:
											final Locator loc = new Locator(prefix + arr[1].trim());
											try {
												DataSourceHelper.load(model, loc);
											} catch (RuntimeException re) {
												TryAgainHandler.ask(model, "Something went wrong while loading line: " + line
														+ "\n\tfrom the session file.\n\tTo recover GenomeView skipped this file.", new Runnable() {
													public void run() {
														try {
															DataSourceHelper.load(model, loc);
														} catch (Exception e) {
															throw new RuntimeException(e);
														}
													}
												});
												log.error("Something went wrong while loading line: " + line
														+ "\n\tfrom the session file.\n\tAsked the user to try again.", re);
											}
											break;
										case C:
										case CONFIG:
											Configuration.loadExtra(new Locator(prefix + arr[1].trim()).stream());
											// Configuration.loadExtra(URIFactory.url(arr[1]).openStream());
											break;
										case OPTION:
											String[] ap = arr[1].trim().split("=", 2);
											Configuration.set(ap[0].trim(), ap[1].trim());
											break;
										case ALIAS:
											String[] al = arr[1].trim().split("=", 2);
											NameService.addSynonym(al[1].trim(), al[0].trim());
											break;
										case PLUGIN:
											PluginLoader.installPlugin(new Locator(prefix + arr[1].trim()), Configuration.getSessionPluginDirectory());
											break;
										case LOCATION:
											ExternalHelper.setPosition(arr[1].trim(), model);

										}
									} catch (Exception e) {
										CrashHandler.showErrorMessage("Problem while executing this instruction: " + line
												+ "\nSkipping this line and continuing.", e);

									}
								}
							} catch (Exception e) {
								CrashHandler.showErrorMessage("Problem while parsing this line: " + line + "\nSkipping this line and continuing.", e);

							}

						}
					}
				} catch (Exception ex) {
					CrashHandler.crash(MessageManager.getString("crashhandler.couldnt_load_session"), ex);
				}
				it.close();
				model.messageModel().setStatusBarMessage(null);

			}
		});
		t.start();
		return t;

	}

	public static void save(File f, Model model) throws IOException {
		PrintWriter out = new PrintWriter(f);
		log.info("Saving session for:" + model.loadedSources());

		out.println("##GenomeView session       ##");
		out.println("##Do not remove header lines##");
		for (DataSource ds : model.loadedSources()) {
			Locator l = ds.getLocator();
			out.println("DATA:" + l);
		}
		for (String key : Configuration.keySet()) {
			out.println("OPTION:" + key + "=" + Configuration.get(key));

		}

		String e = model.vlm.getSelectedEntry().getID();
		Location l = model.vlm.getAnnotationLocationVisible();
		out.println("LOCATION:" + e + ":" + l.start + ":" + l.end);

		out.close();

	}

}
