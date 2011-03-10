/**
 * %HEADER%
 */
package net.sf.genomeview.data.provider;

import net.sf.jannot.Data;

/**
 * Data provides for visualization tracks. Methods should return immediately,
 * data fetching should be done in other threads.
 * 
 * @author Thomas Abeel
 * 
 * @param <T>
 */
public interface DataProvider<T> {

	/**
	 * Gets data. The selected data should cover [start,end[. The coordinates
	 * are one based.
	 * 
	 * @param start
	 *            the start coordinate, this one will be included. This is a
	 *            one-based coordinate.
	 * @param end
	 *            the end coordinate, this one will not be included. This is a
	 *            one-based coordinate.
	 * @return the selected data.
	 */
	public Iterable<T> get(int start, int end);

	public Iterable<Status> getStatus(int start, int end);
	
	public Data<T> getSourceData();

}
