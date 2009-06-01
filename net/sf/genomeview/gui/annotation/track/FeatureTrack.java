/**
 * %HEADER%
 */
package net.sf.genomeview.gui.annotation.track;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import net.sf.genomeview.core.Configuration;
import net.sf.genomeview.data.Model;
import net.sf.genomeview.gui.Convert;
import net.sf.genomeview.gui.Mouse;
import net.sf.genomeview.gui.annotation.GeneEvidenceLabel.FillMode;
import net.sf.genomeview.gui.components.CollisionMap;
import net.sf.jannot.Entry;
import net.sf.jannot.Feature;
import net.sf.jannot.Location;
import net.sf.jannot.Qualifier;
import net.sf.jannot.Type;

public class FeatureTrack extends Track {
	/* Type that is represented by this track */
	private Type type;
	private CollisionMap hitmap;

	/**
	 * Returns the type this track represents
	 * 
	 * @return type represented by this track
	 */
	public Type getType() {
		return type;
	}

	public FeatureTrack(Model model, Type type) {
		this(model,type,true);
	}

	public FeatureTrack(Model model, Type type, boolean visible) {
		super(model,visible);
		hitmap=new CollisionMap(model);
		this.type = type;
		collapsible=true;
	}

	@Override
	public int paint(Graphics gg, Entry entry, int yOffset, double width) {
		boolean collision = false;
		hitmap.clear();

		Graphics2D g = (Graphics2D) gg;
		List<Feature> types = entry.annotation.getByType(
				type, model.getAnnotationLocationVisible());
		if (types.size() > Configuration
				.getInt("annotationview:maximumNoVisibleFeatures")) {
			g.setColor(Color.BLACK);
			g
					.drawString(
							type
									+ ": Too many features to display, zoom in to see features",
							10, yOffset + 10);
			collapsible = false;
			return 20+5;
		} else {
			CollisionMap fullBlockMap = new CollisionMap(model);
			

			int lineThickness = Configuration.getInt("evidenceLineHeight");
//			if (model.isShowTextOnStructure(type)) {
//				lineThickness += 10;
//			}
			int lines = 0;
			for (Feature rf : types) {
				if (!model.isSourceVisible(rf.getSource()))
					continue;
				// the line on which to paint this feature
				int thisLine = 0;

				Color c = Configuration.getColor("TYPE_" + rf.type());
				if (Configuration.getBoolean("useColorQualifierTag")) {
					List<Qualifier> notes = rf.qualifier("colour");
					if (notes.size() > 0) {
						String[] arr = notes.get(0).getValue().split(" ");
						if (arr.length == 3)
							c = new Color(Integer.parseInt(arr[0]), Integer
									.parseInt(arr[1]), Integer.parseInt(arr[2]));

					}
				}
				g.setColor(c);
				int x1 = Convert.translateGenomeToScreen(rf.start(), model
						.getAnnotationLocationVisible(), width);
				int x2 = Convert.translateGenomeToScreen(rf.end() + 1, model
						.getAnnotationLocationVisible(), width);

				// TODO is this not always the case?
				if (x2 > 0) {

					Qualifier name = rf.singleQualifier("gene");

					int maxX = x2;

//					// modify collision box only when the names will be
//					// displayed.
//					if (model.isShowTextOnStructure(type) && name != null) {
//
//						Rectangle2D stringSize = g.getFontMetrics()
//								.getStringBounds(name.getValue(), g);
//						if (x1 + stringSize.getMaxX() > maxX)
//							maxX = x1 + (int) stringSize.getMaxX() + 1;
//
//					}
					/*
					 * How close can items be together before they are
					 * considered overlapping?
					 */
					int closenessOverlap = Configuration
							.getInt("closenessOverlap");
					Rectangle r = new Rectangle(x1 - closenessOverlap, thisLine
							* lineThickness + yOffset, maxX - x1 + 2
							* closenessOverlap, lineThickness);
					// only when the blocks should be tiled, do we need to
					// determine an empty place.
					if (!collision)
						collision = fullBlockMap.collision(r);
					if (!isCollapsed()) {

						while (fullBlockMap.collision(r)) {
							thisLine++;

							if (thisLine > lines)
								lines = thisLine;
							r = new Rectangle(x1 - closenessOverlap, thisLine
									* lineThickness + yOffset, maxX - x1 + 2
									* closenessOverlap, lineThickness);
						}
					}
					fullBlockMap.addLocation(r, null);
					/*
					 * Create one or more rectangles, in order not to have to
					 * reproduce them on every drawing occasion. Make sure they
					 * are ordered from left to right.
					 */
					SortedSet<Location> loc = rf.location();
					ArrayList<Rectangle> rectList = new ArrayList<Rectangle>();
					for (Location l : loc) {

						int subX1 = Convert.translateGenomeToScreen(l.start(),
								model.getAnnotationLocationVisible(), width);
						int subX2 = Convert.translateGenomeToScreen(
								l.end() + 1, model
										.getAnnotationLocationVisible(), width);
						Rectangle rec = new Rectangle(subX1, thisLine
								* lineThickness + yOffset, subX2 - subX1,
								lineThickness - 5);
						/* Add this rectangle to the location hits */
						hitmap.addLocation(rec, l);
						rectList.add(rec);

					}

					if (model.getHighlightedFeatures() != null
							&& model.getHighlightedFeatures().contains(rf)) {
						Color backupColor = g.getColor();
						Stroke backupStroke = g.getStroke();
						float[] dashes = { 5f, 2f };
						Stroke dashedStroke = new BasicStroke(2f,
								BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
								10f, dashes, 0f);
						// g.setStroke(new BasicStroke(2));
						g.setStroke(dashedStroke);
						g.setColor(Color.ORANGE);
						drawRects(g, rectList, FillMode.DRAW);
						g.setColor(backupColor);
						g.setStroke(backupStroke);

					}
					if (!model.isFeatureVisible(rf))
						g.setColor(Color.LIGHT_GRAY);
					drawRects(g, rectList, FillMode.FILL);
					Color backColor = g.getColor();
					g.setColor(g.getColor().darker());
					drawRects(g, rectList, FillMode.DRAW);

					/* Put triangle */
					int trianglehalf = (lineThickness - 5) / 2;
					switch (rf.strand()) {
					case REVERSE:// reverse arrow
						g.drawLine(x1, thisLine * lineThickness + yOffset, x1
								- trianglehalf, thisLine * lineThickness
								+ yOffset + trianglehalf);
						g.drawLine(x1 - trianglehalf, thisLine * lineThickness
								+ yOffset + trianglehalf, x1, thisLine
								* lineThickness + yOffset + lineThickness - 5);
						break;
					case FORWARD:// forward arrow
						g.drawLine(x2, thisLine * lineThickness + yOffset, x2
								+ trianglehalf, thisLine * lineThickness
								+ yOffset + trianglehalf);
						g.drawLine(x2 + trianglehalf, thisLine * lineThickness
								+ yOffset + trianglehalf, x2, thisLine
								* lineThickness + yOffset + lineThickness - 5);
						break;
					default:// do nothing
						break;

					}
//					if (model.isShowTextOnStructure(type) && name != null) {
//						g.drawString(name.getValue(), x1, thisLine
//								* lineThickness + yOffset + 20);
//					}
					g.setColor(backColor);

					// Set<Feature> selected = model.getFeatureSelection();
					Set<Location> intersection = new HashSet<Location>(loc);
					intersection.retainAll(model.getLocationSelection());

					if (intersection.size() > 0) {
						g.setColor(Color.BLACK);
						drawRects(g, rectList, FillMode.DRAW);

					}
				}
			}
			if (Configuration.getBoolean("showTrackName")) {
				g.setColor(Color.black);
				g.drawString(type.toString(), 10, yOffset + lineThickness);
			}
			return (lines + 1) * lineThickness+5;

		}
	}

	/**
	 * A double click on the evidence label wants us to zoom to the double
	 * clicked feature. The chromosome view should pan to the correct location,
	 * but should not change zoom level.
	 * 
	 * @param e
	 *            the MouseEvent
	 */
	private boolean doubleClick(MouseEvent e) {
		Location locationHit = hitmap.uniqueLocation(e.getX(), e.getY());
		if (locationHit != null) {
			Feature featHit = locationHit.getParent();
			model.setLocationSelection(featHit);
			int l = featHit.length();
			model.setAnnotationLocationVisible(new Location(featHit.start()
					- (l / 20), featHit.end() + (l / 20)));

			if (!featHit.overlaps(model.getChromosomeLocationVisible())) {
				int featCenter = featHit.start() + featHit.length() / 2;
				model.centerChromView(featCenter);
			}
			return true;
		}
		return false;
	}

	private void drawRects(Graphics g, ArrayList<Rectangle> rectList,
			FillMode fm) {
		Point lastPoint = null;

		for (Rectangle rect : rectList) {
			switch (fm) {
			case FILL:
				g.fillRect(rect.x, rect.y, rect.width, rect.height);
				break;
			case DRAW:
				g.drawRect(rect.x, rect.y, rect.width, rect.height);
				break;
			case BACKDROP:
				// TODO some nice indication that something is highlighted?
				break;
			}
			if (lastPoint != null) {
				g.drawLine(rect.x, rect.y + rect.height / 2, lastPoint.x,
						lastPoint.y);
			}
			lastPoint = new Point(rect.x + rect.width, rect.y + rect.height / 2);
		}

	}

	@Override
	public boolean mouseClicked(int x, int y, MouseEvent e) {
//		System.out.println("feature track click: "+x+"\t"+y);
		// catch double clicks
		if (e.getClickCount() == 2) {
			doubleClick(e);
		}

		/* try selecting something */
		Location locationHit = hitmap.uniqueLocation(e.getX(), e.getY());
		if (Mouse.button1(e)) {
			if (locationHit == null && !Mouse.modifier(e)) {
				model.clearLocationSelection();
			} else if (locationHit != null && e.isShiftDown()) {
				if (model.getLocationSelection().contains(locationHit)) {
					model.removeLocationSelection(locationHit);
				} else {
					model.addLocationSelection(locationHit);
				}
			} else if (locationHit != null && !Mouse.modifier(e)) {
				model.setLocationSelection(locationHit);

			}
			return true;
		}
		return false;

	}

	@Override
	public String displayName() {
		return type.toString();
	}

}