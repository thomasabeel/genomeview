/**
 * %HEADER%
 */
package net.sf.genomeview.data.provider;

import java.util.ArrayList;

import net.sf.genomeview.data.GenomeViewScheduler;
import net.sf.genomeview.data.Model;
import net.sf.genomeview.data.Task;
import net.sf.jannot.Data;
import net.sf.jannot.Entry;
import net.sf.jannot.Location;
import net.sf.jannot.pileup.Pile;
import net.sf.jannot.tdf.TDFData;

import org.broad.igv.track.WindowFunction;

/**
 * 
 * @author Thomas Abeel
 * 
 */
public class TDFProvider extends PileProvider {

	private TDFData source;

	private Model model;

	public TDFProvider(Entry e, TDFData source, Model model) {
		this.source = source;
		this.model = model;
	}

	private ArrayList<Pile> buffer = new ArrayList<Pile>();
	private ArrayList<Status> status = new ArrayList<Status>();
	private int lastStart = -1;
	private int lastEnd = -1;
	// privFate float maxSummary;
	private float maxPile;

	@Override
	public Iterable<Pile> get(final int start, final int end) {
		/* Check whether request can be fulfilled by buffer */
		if (start >= lastStart && end <= lastEnd
				&& (lastEnd - lastStart) <= 2 * (end - start))
			return buffer;

		/* New request */

		// reset status
		lastStart = start;
		lastEnd = end;

		buffer.clear();
		status.clear();
		status.add(new Status(false, true, false, start, end));
		// queue up retrieval
		Task t = new Task(new Location(start, end)) {

			@Override
			public void run() {
				// When actually running, check again whether we actually need
				// this data
				if (!(start >= lastStart && end <= lastEnd && (lastEnd - lastStart) <= 2 * (end - start)))
					return;
				status.get(0).setRunning();
				Iterable<Pile> fresh = source.get(start, end);

				for (Pile p : fresh) {
					float val = p.getCoverage();

					if (val > maxPile)
						maxPile = val;

					buffer.add(p);
				}
				status.get(0).setFinished();
				notifyListeners();
			}

		};
		GenomeViewScheduler.submit(t);

		// System.out.println("\tServing new request from provider");

		return buffer;

	}

	
	private void notifyListeners(){
		setChanged();
		notifyObservers();
	}
	@Override
	public double getMaxPile() {
		return maxPile;
	}

	public Iterable<Status> getStatus(int start, int end) {
		return status;
	}

	@Override
	public Data<Pile> getSourceData() {
		return source;
	}

	@Override
	public WindowFunction[] getWindowFunctions() {
		return source.availableWindowFunctions().toArray(new WindowFunction[0]);
	}

	@Override
	public void requestWindowFunction(WindowFunction wf) {
		// System.out.println("WF in TDF: "+wf);
		if (source.availableWindowFunctions().contains(wf)) {
			// System.out.println("\tWe are nwo using WF: "+wf);
			source.requestWindowFunction(wf);
			lastStart = -1;
			lastEnd = -1;
			maxPile = 0;
			buffer.clear();
			setChanged();
			notifyObservers();
		}

	}

	@Override
	public boolean isCurrentWindowFunction(WindowFunction wf) {
		return source.isCurrentWindowFunction(wf);
	}

}