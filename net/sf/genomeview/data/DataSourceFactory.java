/**
 * %HEADER%
 */
package net.sf.genomeview.data;

import java.awt.Button;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import net.sf.genomeview.core.Configuration;
import net.sf.genomeview.data.das.DAS;
import net.sf.genomeview.data.das.DAS.EntryPoint;
import net.sf.jannot.source.DataSource;
import net.sf.jannot.source.FileSource;
import net.sf.jannot.source.MultiFileSource;
import net.sf.jannot.source.SAMDataSource;
import net.sf.jannot.source.URLSource;

public class DataSourceFactory {
	static class MyAuthenticator extends Authenticator {
//		private String username = null, password = null;

		public MyAuthenticator() {
			// username = user;
			// password = pass;
		}

		protected PasswordAuthentication getPasswordAuthentication() {
			final JDialog jd = new JDialog();
			jd.setTitle("Enter password");
			jd.setModal(true);
			jd.setLayout(new GridLayout(0, 1));
			Label jl = new Label(getRequestingPrompt());
			jd.add(jl);
			TextField username = new TextField();
			username.setBackground(Color.lightGray);
			jd.add(username);
			TextField password = new TextField();
			password.setEchoChar('*');
			password.setBackground(Color.lightGray);
			jd.add(password);
			Button jb = new Button("OK");
			jd.add(jb);
			jb.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					jd.dispose();
				}
			});
			System.out.println("Requesting Host  : " + getRequestingHost());
			System.out.println("Requesting Port  : " + getRequestingPort());
			System.out.println("Requesting Prompt : " + getRequestingPrompt());
			System.out.println("Requesting Protocol: " + getRequestingProtocol());
			System.out.println("Requesting Scheme : " + getRequestingScheme());
			System.out.println("Requesting Site  : " + getRequestingSite());
			return new PasswordAuthentication(username.getText(), password.getText().toCharArray());
		}

//		private void get() {
//			// TODO Auto-generated method stub
//
//		}
	}

	static {
		Authenticator.setDefault(new MyAuthenticator());
	}

	public enum Sources {
		LOCALFILE, URL, DIRECTORY, DAS;
		@Override
		public String toString() {
			switch (this) {
			case URL:
				return "URL";
			case LOCALFILE:
				return "Local file";
			case DIRECTORY:
				return "Directory";
			case DAS:
				return "DAS server";
			}
			return null;
		}
	}

	public static DataSource[] create(Sources source, Model model, final String[] extensions) {
		switch (source) {
		case DIRECTORY:
			try {
				JFileChooser chooser = new JFileChooser(Configuration.getFile("lastDirectory"));
				chooser.setMultiSelectionEnabled(false);
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				chooser.setFileFilter(new FileFilter() {

					@Override
					public boolean accept(File f) {
						return f.isDirectory();
					}

					@Override
					public String getDescription() {
						return "Directories";
					}

				});
				int returnVal = chooser.showOpenDialog(model.getParent());
				if (returnVal == JFileChooser.APPROVE_OPTION) {

					File file = chooser.getSelectedFile();

					DataSource[] out = new DataSource[1];
					out[0] = new MultiFileSource(file);
					Configuration.set("lastDirectory", file.getParentFile());
					return out;
				} else {
					return null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case URL:

			try {
				URL url = new URI(JOptionPane.showInputDialog(model.getParent(), "Give the URL of the data").trim()).toURL();
				return new DataSource[] { new URLSource(url) };
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case DAS:
			try {
				String url = JOptionPane.showInputDialog(model.getParent(), "Give the URL of the data").trim();
				DAS das = new DAS(url);
				List<String> refs = das.getReferences();
				Collections.sort(refs);
				String ref = (String) JOptionPane.showInputDialog(model.getParent(), "Select reference genome", "Reference selection", JOptionPane.INFORMATION_MESSAGE, null, refs.toArray(), refs.get(0));
				List<EntryPoint> eps = das.getEntryPoints(ref);
				EntryPoint ep = (EntryPoint) JOptionPane.showInputDialog(model.getParent(), "Select entry point", "Entry point selection", JOptionPane.INFORMATION_MESSAGE, null, eps.toArray(), eps.get(0));
				das.setEntryPoint(ep);
				das.setReference(ref);
				return new DataSource[] { das };
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		case LOCALFILE:
			try {
				JFileChooser chooser = new JFileChooser(Configuration.getFile("lastDirectory"));
				chooser.resetChoosableFileFilters();
				if (extensions != null) {

					for (final String ext : extensions) {
						chooser.addChoosableFileFilter(new FileFilter() {

							@Override
							public boolean accept(File f) {
								if (f.isDirectory())
									return true;
								return f.getName().endsWith(ext) || f.getName().endsWith(ext + ".gz");
							}

							@Override
							public String getDescription() {
								return ext + " files";
							}

						});
					}
					chooser.addChoosableFileFilter(new FileFilter() {

						@Override
						public boolean accept(File f) {
							if (f.isDirectory())
								return true;
							for (String ext : extensions) {

								if (f.getName().endsWith(ext) || f.getName().endsWith(ext + ".gz")) {
									return true;
								}
							}
							return false;
						}

						@Override
						public String getDescription() {
							return Arrays.toString(extensions);
						}

					});
				}

				chooser.setMultiSelectionEnabled(true);
				int returnVal = chooser.showOpenDialog(model.getParent());
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File[] files = chooser.getSelectedFiles();
					DataSource[] out = new DataSource[files.length];
					for (int i = 0; i < files.length; i++) {
						if (files[i].getName().toLowerCase().endsWith("bai")) {
							String fileName = files[i].toString();
							out[i] = new SAMDataSource(new File(fileName.substring(0, fileName.length() - 4)));
						} else
							out[i] = new FileSource(files[i]);

					}
					Configuration.set("lastDirectory", files[0].getParentFile());
					return out;
				} else {
					return null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			break;
		}
		return null;
	}
}
