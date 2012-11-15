/**
 * %HEADER%
 */
package net.sf.genomeview.data;

import java.awt.Color;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.genomeview.core.Configuration;
import net.sf.genomeview.gui.external.JavaScriptHandler;
import net.sf.genomeview.gui.viztracks.TickmarkTrack;
import net.sf.genomeview.gui.viztracks.Track;
import net.sf.genomeview.gui.viztracks.annotation.StructureTrack;
import net.sf.genomeview.plugin.GUIManager;
import net.sf.jannot.AminoAcidMapping;
import net.sf.jannot.Entry;
import net.sf.jannot.EntrySet;
import net.sf.jannot.Location;
import net.sf.jannot.Strand;
import net.sf.jannot.event.ChangeEvent;
import net.sf.jannot.exception.ReadFailedException;
import net.sf.jannot.source.DataSource;
import be.abeel.net.URIFactory;
import be.abeel.util.DefaultHashMap;

/**
 * The Model.
 * 
 * @author Thomas Abeel
 * 
 */
public class Model extends Observable implements IModel {
	private Logger logger = Logger.getLogger(Model.class.getCanonicalName());

	private EntrySet entries = new EntrySet();

	private SelectionModel selectionModel = new SelectionModel();
	private MouseModel mouseModel = new MouseModel();
	private MessageModel messageModel = new MessageModel(this);

	public MessageModel messageModel(){
		return messageModel;
	}
	
	public MouseModel mouseModel() {
		return mouseModel;
	}

	public Model(String id) {
		

		guimanager = new GUIManager();

		/* JavaScriptInputHandler */
		if (Configuration.getBoolean("integration:monitorJavaScript")) {
			new JavaScriptHandler(this, id);
			logger.info("JavaScriptHandler started");
		} else {
			logger.info("JavaScriptHandler NOT started");
		}

		GenomeViewScheduler.start(this);

		selectionModel.addObserver(this);
		messageModel.addObserver(this);
		this.trackList = new TrackList(this);
		// entries.addObserver(this);

		Configuration.getTypeSet("visibleTypes");
		updateTracks();

	}

	public int noEntries() {
		return entries.size();
	}



	public void update(Observable arg0, Object arg) {
		System.out.println("Model update "+arg0+"\t"+arg );
		if (arg instanceof ChangeEvent) {
			undoStack.push((ChangeEvent) arg);
			redoStack.clear();
			while (undoStack.size() > 100)
				undoStack.remove(0);
			refresh(NotificationTypes.JANNOTCHANGE);
		} else {
			refresh(arg);
		}

	}

	public void clearEntries() {
		selectionModel.clear();
		visible=new Location(0,0);
		loadedSources.clear();
		entries.clear();
		undoStack.clear();
		redoStack.clear();
		trackList.clear();
		refresh(NotificationTypes.GENERAL);
	}

	// private void clearTrackList(TrackList tracklist) {
	// List<Track> remove = new ArrayList<Track>();
	// for (Track t : tracklist) {
	//
	// if (!(t instanceof FeatureTrack || t instanceof StructureTrack || t
	// instanceof TickmarkTrack))
	// remove.add(t);
	// }
	// tracklist.removeAll(remove);
	// refresh();
	//
	// }

	public EntrySet entries() {
		return entries;

	}

	private boolean silent;

	/**
	 * Set the mode of the model. In silent mode, the model does not pass on
	 * notifications from its observables to its observers.
	 * 
	 * This can be useful to limit the number of repaints in events that there
	 * are a lot of changes in the data. For instance when loading new data.
	 * 
	 * @param silent
	 */
	public void setSilent(boolean silent) {
		this.silent = silent;
		refresh(NotificationTypes.GENERAL);
	}

	public void refresh(Object arg) {
		if (!silent) {
			setChanged();
			notifyObservers(arg == null ? NotificationTypes.GENERAL : arg);
		}
	}

	/**
	 * Checked way to notify all model observers.
	 */
	@Deprecated
	public void refresh() {
		refresh(NotificationTypes.GENERAL);

	}

	private boolean exitRequested = false;

	public void exit() {
		this.exitRequested = true;

		loadedSources.clear();
		refresh();

	}

	public Location getAnnotationLocationVisible() {
		return visible;
	}

	private Location visible=new Location(0,0);
	//private int annotationStart = 0, annotationEnd = 0;

	public void setAnnotationLocationVisible(Location r, boolean mayExpand) {
		int modStart = -1;
		int modEnd = -1;
		if (r.start > 1) {
			modStart = r.start;
		} else {
			modStart = 1;
			modEnd = r.length();
		}
		int chromLength = getSelectedEntry().getMaximumLength();
		if (r.end < chromLength || chromLength == 0) {
			modEnd = r.end;
		} else {
			modEnd = chromLength;
			modStart = modEnd - r.length();
			if (modStart < 1)
				modStart = 1;
		}
		Location newZoom = new Location(modStart, modEnd);
		/* When trying to zoom to something really small */
		if (newZoom.length() < 50 && mayExpand) {
			setAnnotationLocationVisible(new Location(modStart - 25, modEnd + 25));
		}
		if (newZoom.length() != visible.end - visible.start+ 1 && newZoom.length() < 50)
			return;
		// if (newZoom.length() != annotationEnd - annotationStart + 1
		// && newZoom.length() > Configuration.getInt("general:zoomout"))
		// return;
		if (newZoom.start < 1 || newZoom.end < 1)
			return;

		ZoomChange zc = new ZoomChange(visible, newZoom);
		zc.doChange();
		refresh();

	}

	/**
	 * Set the visible area in the evidence and structure frame to the given
	 * Location.
	 * 
	 * start and end one-based [start,end]
	 * 
	 * @param start
	 * @param annotationEnd
	 */

	public void setAnnotationLocationVisible(Location r) {
		setAnnotationLocationVisible(r, false);

	}

	/**
	 * Provides implementation to do/undo zoom changes.
	 * 
	 * @author Thomas Abeel
	 * 
	 */
	class ZoomChange implements ChangeEvent {
		/* The original zoom */
		private Location orig;

		/* The new zoom */
		private Location neww;

		public ZoomChange(Location location, Location newZoom) {
			this.orig = location;
			this.neww = newZoom;
		}

		@Override
		public void doChange() {
//			annotationStart = neww.start();
//			annotationEnd = neww.end();
			visible=neww;

		}

		@Override
		public void undoChange() {
			assert (visible.start == neww.start());
			assert (visible.end == neww.end());
			visible=orig;
			

		}

	}

	public boolean isExitRequested() {
		return exitRequested;
	}

	private ConcurrentLinkedQueue<Highlight> highlights = new ConcurrentLinkedQueue<Highlight>();

	public class Highlight {
		final public Location location;

		final public Strand strand;

		public Highlight(Location location, Color color, Strand strand) {
			super();
			this.color = color;
			this.location = location;
			this.strand = strand;
		}

		final public Color color;
	}

	public List<Highlight> getHighlight(Location region) {
		ArrayList<Highlight> out = new ArrayList<Highlight>();
		for (Highlight f : highlights) {
			if (f.location.end() > region.start() && f.location.start() < region.end())
				out.add(f);
		}
		return Collections.unmodifiableList(out);
	}

	public void clearHighlights() {
		highlights.clear();
		refresh();
	}

	public void addHighlight(Location l, Color c, Strand s) {
		highlights.add(new Highlight(l, c, s));
		refresh();
	}

	/**
	 * Center the model on a certain position. This will cause the nucleotide
	 * start, end and the normal start and end to change.
	 * 
	 * @param genomePosition
	 *            the position to center on
	 */
	public void center(int genomePosition) {
		int length = (visible.end - visible.start) / 2;
		setAnnotationLocationVisible(new Location(genomePosition - length, genomePosition + length));

	}

	/**
	 * Load new entries from a data source.
	 * 
	 * 
	 * This should only be done by a ReadWorker.
	 * 
	 * @param f
	 *            data source to load data from
	 * @throws ReadFailedException
	 * 
	 * FIXME move to read worker
	 */
	void addData(DataSource f) throws ReadFailedException {
		if (entries.size() == 0)
			setAnnotationLocationVisible(new Location(1, 51));
		logger.info("Reading source:" + f);
		try {
			f.read(entries);
		} catch (Exception e) {
			throw new ReadFailedException(e);
		}
		logger.info("Entries: " + entries.size());
		logger.info("Model adding data done!");
		loadedSources.add(f);
		updateTracks();
		refresh(NotificationTypes.GENERAL);

	}

	private HashMap<Entry, AminoAcidMapping> aamapping = new DefaultHashMap<Entry, AminoAcidMapping>(
			AminoAcidMapping.valueOf(Configuration.get("translationTable:default")));

	// private Configuration trackMap;

	public AminoAcidMapping getAAMapping(Entry e) {
		return aamapping.get(e);
	}

	public AminoAcidMapping getAAMapping() {
		return aamapping.get(getSelectedEntry());
	}

	public void setAAMapping(Entry e, AminoAcidMapping aamapping) {
		logger.info("setting amino acid mapping: " + aamapping);
		this.aamapping.put(e, aamapping);
		refresh(NotificationTypes.TRANSLATIONTABLECHANGE);

	}

	private final TrackList trackList;

	

	

	/**
	 * Returns a list of all tracks. This method creates a copy to make it safe
	 * to iterate the returned list.
	 * 
	 * @return list of tracks
	 */
	public TrackList getTrackList() {
		return trackList;
	}

	
	/**
	 * This method keeps the track list up to date when adding new data to the
	 * entry from outside the model.
	 * 
	 * All types and graphs loaded should have a corresponding track.
	 */
	public synchronized void updateTracks() {
		try {
			
			Entry e = this.getSelectedEntry();
			boolean changed=trackList.update(e);
			
			if(changed)
				refresh(NotificationTypes.UPDATETRACKS);
		} catch (ConcurrentModificationException e) {
			logger.log(Level.INFO, "Update tracks interrupted, tracks already changed", e);
		}

	}

	private Stack<ChangeEvent> undoStack = new Stack<ChangeEvent>();

	private Stack<ChangeEvent> redoStack = new Stack<ChangeEvent>();

	public boolean hasRedo() {
		return redoStack.size() > 0;
	}

	public boolean hasUndo() {
		return undoStack.size() > 0;
	}

	public void undo() {
		ChangeEvent e = undoStack.pop();
		e.undoChange();
		redoStack.push(e);
		refresh();
	}

	public void redo() {
		ChangeEvent e = redoStack.pop();
		e.doChange();
		undoStack.push(e);
		refresh();
	}

	public String getUndoDescription() {
		if (hasUndo())
			return "Undo: " + undoStack.peek();
		else
			return "";
	}

	public String getRedoDescription() {
		if (hasRedo())
			return "Redo: " + redoStack.peek();
		else
			return "";
	}

	/* Cache of the sources that are currently loaded */
	private ConcurrentSkipListSet<DataSource> loadedSources = new ConcurrentSkipListSet<DataSource>();

	private int pressTrack;

	public Set<DataSource> loadedSources() {
		return loadedSources;

	}

	public int getPressTrack() {
		return pressTrack;
	}

	/**
	 * Keeps track of which track was used for selecting a region. <code>
	 * 4 -> AA
	 * 3 -> AA
	 * 2 -> AA
	 * 1 -> forward nucleotides
	 * 0 ->tick marks
	 * -1 -> reverse nucleotides
	 * -2 -> AA
	 * -3 -> AA
	 * -4 -> AA
	 * </code>
	 * 
	 * @param pressTrack
	 */
	public void setSelectedTrack(int pressTrack) {
		this.pressTrack = pressTrack;
	}

	private final GUIManager guimanager;

	public GUIManager getGUIManager() {
		return guimanager;
	}

	@Override
	public Location getSelectedRegion() {
		return selectionModel.getSelectedRegion();
	}

	public SelectionModel selectionModel() {
		return selectionModel;
	}

	public Entry getSelectedEntry() {
		if (entries.size() == 0)
			return DummyEntry.dummy;
		return entries.getEntry();
	}

	public synchronized void setSelectedEntry(Entry entry) {
		entries.setDefault(entry);
		selectionModel.clear();

		setAnnotationLocationVisible(getAnnotationLocationVisible());
		trackList.clear();
		updateTracks();
		refresh(NotificationTypes.ENTRYCHANGED);

	}

	/**
	 * Removes a datakey from the visualization.
	 * 
	 * @param track
	 */
	public void remove(Track track) {
		if (!(track instanceof StructureTrack) && !(track instanceof TickmarkTrack)) {
			for(Entry e:entries){
				e.remove(track.getDataKey());
			}
			trackList.remove(track.getDataKey());
		}
		
		GenomeViewScheduler.submit(Task.GC);
		setChanged();
		notifyObservers(NotificationTypes.UPDATETRACKS);
	}

	public void change(ChangeEvent change) {
		undoStack.push(change);

	}

	private WorkerManager wm = new WorkerManager();

	public WorkerManager getWorkerManager() {
		return wm;

	}

	
	
	public synchronized Throwable processException(){
		if(!exceptionStack.isEmpty())
			return exceptionStack.pop();
		return null;
	}
	
	private Stack<Throwable>exceptionStack=new Stack<Throwable>();
	/**
	 * Method to register daemon exceptions to the model.
	 * @param e
	 */
	public synchronized void daemonException(Throwable e) {
		exceptionStack.push(e);
		logger.log(Level.SEVERE,"Exception in daemon thread",e);
		setChanged();
		notifyObservers(NotificationTypes.EXCEPTION);
		
	}
}