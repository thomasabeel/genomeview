/**
 * %HEADER%
 */
package net.sf.genomeview.gui.information;

import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import net.sf.genomeview.data.Model;
import net.sf.genomeview.gui.StaticUtils;
import net.sf.jannot.Feature;
import net.sf.jannot.Qualifier;
import be.abeel.gui.GridBagPanel;

/**
 * Panel with detailed information about a single Feature. (bottom -right in
 * GUI)
 * 
 * @author Thomas Abeel
 * 
 */
public class FeatureDetailPanel extends GridBagPanel implements Observer {

	/**
     * 
     */
	private static final long serialVersionUID = 1214531303733670258L;

	private JEditorPane name = new JEditorPane();

	private Model model;

	public FeatureDetailPanel(Model model) {
		this.model = model;
		name.setEditable(false);
		name.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showPopUp(e);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showPopUp(e);
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.isPopupTrigger()) {
					showPopUp(e);
				}
			}

			/**
			 * Class representing a website query.
			 * 
			 * @author Thomas
			 * 
			 */
			class Query extends AbstractAction {

				/**
				 * 
				 */
				private static final long serialVersionUID = 5252902483799067615L;
				private String queryURL;

				// private String label;

				Query(String label, String queryURL) {
					super(label);
					// this.label = label;
					this.queryURL = queryURL;
				}

				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						String query = queryURL
								+ URLEncoder.encode(name.getSelectedText(),
										"UTF-8");

						Desktop.getDesktop().browse(new URI(query));
					} catch (MalformedURLException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (IOException f) {
						// TODO Auto-generated catch block
						f.printStackTrace();
					} catch (URISyntaxException g) {
						// TODO Auto-generated catch block
						g.printStackTrace();
					}

				}

			}

			private JPopupMenu popupMenu = null;

			private void showPopUp(MouseEvent e) {
				if (popupMenu == null) {
					popupMenu = new JPopupMenu();
					JMenuItem ncbiQuery = new JMenuItem(new Query(
							"Query at NCBI Entrez",
							"http://www.ncbi.nlm.nih.gov/sites/gquery?term="));
					JMenuItem ensemblQuery = new JMenuItem(
							new Query("Query at Ensembl",
									"http://www.ensembl.org/Homo_sapiens/Search/Summary?species=all;idx=;q="));
					JMenuItem ebi = new JMenuItem(
							new Query("Query at EMBL-EBI",
									"http://www.ebi.ac.uk/ebisearch/search.ebi?db=allebi&query="));

					JMenuItem google = new JMenuItem(new Query(
							"Query at Google",
							"http://www.google.com/search?q="));

					/* Check whether in Broad domain */
					boolean inBroadNetwork = false;
					try {

						Enumeration<NetworkInterface> eni = NetworkInterface
								.getNetworkInterfaces();
						while (eni.hasMoreElements()) {
							NetworkInterface ni = eni.nextElement();
							// System.out.println(ni.getInetAddresses());
							Enumeration<InetAddress> eia = ni
									.getInetAddresses();
							while (eia.hasMoreElements()) {
								InetAddress ia = eia.nextElement();
								System.out.println("Host  : "
										+ ia.getCanonicalHostName());
								if (ia.getCanonicalHostName().contains(
										"broad.mit.edu"))
									inBroadNetwork = true;
							}
						}

					} catch (Exception t) {
						t.printStackTrace();
					}
					JMenuItem calhoun = new JMenuItem(
							new Query("View feature in Calhoun",
									"http://calhoun/calhoun/app?page=InspectFeature&service=external&sp=S"));

					popupMenu.add(ncbiQuery);
					popupMenu.add(ensemblQuery);
					popupMenu.add(ebi);
					popupMenu.add(google);
					if (inBroadNetwork)
						popupMenu.add(calhoun);
				}
				popupMenu.show(e.getComponent(), e.getX(), e.getY());

			}

		});
		name.setEditorKit(new HTMLEditorKit());
		name.setDocument(new HTMLDocument());
		name.addHyperlinkListener(new HyperlinkListener() {

			@Override
			public void hyperlinkUpdate(HyperlinkEvent e) {
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {

					System.out.println(e.getDescription());
					URL url = e.getURL();
					System.out.println("Hyperlink: " + url);
					// try {
					// Desktop.getDesktop().browse(url.toURI());
					// } catch (IOException e1) {
					// // TODO Auto-generated catch block
					// e1.printStackTrace();
					// } catch (URISyntaxException e1) {
					// // TODO Auto-generated catch block
					// e1.printStackTrace();
					// }
					//                
				}
			}

		});
		;

		model.addObserver(this);
		gc.fill = GridBagConstraints.BOTH;
		gc.weightx = 1;
		gc.weighty = 0;
		add(name, gc);
		gc.gridy++;
		gc.weighty = 1;
		add(new JLabel(), gc);
	}

	private int lastHash = 0;

	@Override
	public void update(Observable o, Object arg) {
		Set<Feature> set = model.getFeatureSelection();
		String text = "";

		if (set != null && set.size() > 0) {
			for (Feature rf : set) {
				text += "Data origin: " + rf.getSource() + "<br/>";
				text += "Location: "
						+ StaticUtils.escapeHTML(rf.location().toString())
						+ "<br/>";
				text += "Strand: " + rf.strand() + "<br/>";
				text += "Score: " + rf.getScore() + "<br/>";
				Set<String> qks = rf.getQualifiersKeys();
				for (String key : qks) {
					for (Qualifier q : rf.qualifier(key)) {
						text += q + "<br/>";
					}
				}

			}

			text += "---------------------------------------<br/>";
			int hash = text.hashCode();
			if (hash != lastHash) {
				System.out.println("FDP: " + text + "\t" + name);
				name.setText("<html>" + text + "</html>");
				name.scrollRectToVisible(new Rectangle(0, 0, 1, 1));
				lastHash = hash;
			}

		} else {
			name.setText("");
		}

	}
}
