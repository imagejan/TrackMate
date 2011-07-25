package fiji.plugin.trackmate.visualization.trackscheme;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPopupMenu;

import org.jgrapht.graph.DefaultWeightedEdge;

import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;

import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.SpotFeature;
import fiji.plugin.trackmate.TrackMateModel;

public class TrackSchemePopupMenu extends JPopupMenu {

	private static final long serialVersionUID = -5168784267411318961L;
	private static final boolean DEBUG = true; 
	private Object cell;
	private TrackSchemeFrame frame;
	//	private Point point;
	private TrackMateModel model;
	private JGraphXAdapter graph;

	public TrackSchemePopupMenu(final TrackSchemeFrame frame, final Object cell, final TrackMateModel model, final JGraphXAdapter graph) {
		this.frame = frame;
		this.cell = cell;
		this.model = model;
		this.graph = graph;
		init();
	}


	@SuppressWarnings("serial")
	private void init() {

		// Build selection categories
		final Object[] selection = frame.getGraph().getSelectionCells();
		final ArrayList<mxCell> vertices = new ArrayList<mxCell>();
		final ArrayList<mxCell> edges = new ArrayList<mxCell>();
		for(Object obj : selection) {
			mxCell cell = (mxCell) obj;
			if (cell.isVertex()) 
				vertices.add(cell);
			else if (cell.isEdge()) 
				edges.add(cell);
		}

		if (cell != null) {
			// Edit
			add(new AbstractAction("Edit spot name") {
				public void actionPerformed(ActionEvent e) {
					frame.getGraphComponent().startEditingAtCell(cell);
				}
			});

			// Fold
			add(new AbstractAction("Fold/Unfold branch") {
				public void actionPerformed(ActionEvent e) {
					Object parent;
					if (frame.getGraph().isCellFoldable(cell, true))
						parent = cell;
					else
						parent = frame.getGraph().getModel().getParent(cell);
					frame.getGraph().foldCells(!frame.getGraph().isCellCollapsed(parent), false, new Object[] { parent });
				}
			});

		} else { 

			if (vertices.size() > 1) {

				// Multi edit
				add(new AbstractAction("Edit " + vertices.size() +" spot names") {

					public void actionPerformed(ActionEvent e) {
						final mxCell firstCell = vertices.remove(0);
						frame.getGraphComponent().startEditingAtCell(firstCell, e);
						frame.getGraphComponent().addListener(mxEvent.LABEL_CHANGED, new mxIEventListener() {

							@Override
							public void invoke(Object sender, mxEventObject evt) {
								for (mxCell cell : vertices) {
									cell.setValue(firstCell.getValue());
									frame.getGraph().getCellToVertexMap().get(cell).setName(firstCell.getValue().toString());
								}
								frame.getGraphComponent().refresh();
								frame.getGraphComponent().removeListener(this);
							}
						});
					}
				});
			}

			// Link

			Action linkAction = new AbstractAction("Link " + model.getSpotSelection().size() +" spots") {

				@Override
				public void actionPerformed(ActionEvent e) {

					// Sort spots by time
					TreeMap<Float, Spot> spotsInTime = new TreeMap<Float, Spot>();
					for (Spot spot : model.getSpotSelection()) {
						spotsInTime.put(spot.getFeature(SpotFeature.POSITION_T), spot);
					}

					// Find adequate column
					int targetColumn = 0;
					for (int i = 0; i < frame.getGraphComponent().getColumnWidths().length; i++) {
						targetColumn += frame.getGraphComponent().getColumnWidths()[i];
					}

					// Then link them in this order
					model.beginUpdate();
					try {
						graph.getModel().beginUpdate();
						Iterator<Float> it = spotsInTime.keySet().iterator();
						Float previousTime = it.next();
						Spot previousSpot = spotsInTime.get(previousTime);
						while(it.hasNext()) {
							Float currentTime = it.next();
							Spot currentSpot = spotsInTime.get(currentTime);
							// Link if not linked already
							if (model.containsEdge(previousSpot, currentSpot))
								continue;
							// Check that the cells matching the 2 spots exist in the graph
							mxCell currentCell = graph.getVertexToCellMap().get(currentSpot);
							if (null == currentCell) {
								currentCell = frame.insertSpotInGraph(currentSpot, targetColumn);
								if (DEBUG) {
									System.out.println("[TrackSchemePopupMenu] linkSpots: creating cell "+currentCell+" for spot "+currentSpot);
								}
							}
							mxCell previousCell = graph.getVertexToCellMap().get(previousSpot);
							if (null == previousCell) {
								previousCell = frame.insertSpotInGraph(previousSpot, targetColumn);
								if (DEBUG) {
									System.out.println("[TrackSchemePopupMenu] linkSpots: creating cell "+previousCell+" for spot "+previousSpot);
								}
							}
							// This will update the mxGraph view.
							// Check if the model does not have already a edge for these 2 spots (that is 
							// the case if the 2 spot are in an invisible track, which track scheme does not
							// know of).
							DefaultWeightedEdge edge = model.getEdge(previousSpot, currentSpot); 
							if (null == edge) {
								edge = model.addEdge(previousSpot, currentSpot, -1);
								mxCell cell = graph.addJGraphTEdge(edge);
								cell.setValue("New");
							} else {
								mxCell cell = graph.addJGraphTEdge(edge);
								cell.setValue(String.format("%.1f", model.getEdgeWeight(edge)));
							}
							previousSpot = currentSpot;
						}
					} finally {
						graph.getModel().endUpdate();
						model.endUpdate();
					}
				}
			};
			if (model.getSpotSelection().size() > 1) {
				add(linkAction);
			}
		}

		// Remove
		if (selection.length > 0) {
			Action removeAction = new AbstractAction("Remove spots and links") {
				public void actionPerformed(ActionEvent e) {
					try {
						frame.getGraph().getModel().beginUpdate();
						frame.getGraph().removeCells(selection);
					} finally {
						frame.getGraph().getModel().endUpdate();
					}
				}
			};
			add(removeAction);
		}

		// Fold


	}

}
