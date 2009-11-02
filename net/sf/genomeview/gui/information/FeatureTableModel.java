/**
 * %HEADER%
 */
package net.sf.genomeview.gui.information;

import java.util.Observable;
import java.util.Observer;

import javax.swing.Icon;
import javax.swing.table.AbstractTableModel;

import net.sf.genomeview.core.Icons;
import net.sf.genomeview.data.Model;
import net.sf.genomeview.data.NotificationTypes;
import net.sf.jannot.Feature;
import net.sf.jannot.Sequence;
import net.sf.jannot.Type;
import net.sf.jannot.utils.SequenceTools;

/**
 * Wraps an instance of the Model interface and let it act like a ListModel.
 * 
 * @author Thomas Abeel
 * 
 */
public class FeatureTableModel extends AbstractTableModel implements Observer {

	/**
     * 
     */
	private static final long serialVersionUID = 320228141380099074L;

	private String[] columns = { "Name", "Start", "Stop", "Internal stop", "Splice sites"};

	@Override
	public String getColumnName(int column) {
		return columns[column];
	}

	private Model model;

	public FeatureTableModel(Model model) {
		this.model = model;
		model.addObserver(this);

	}

	public void update(Observable o, Object arg) {
		if (arg == NotificationTypes.GENERAL || arg == NotificationTypes.TRANSLATIONTABLECHANGE || arg == NotificationTypes.ENTRYCHANGED || arg == NotificationTypes.JANNOTCHANGE) {
			fireTableDataChanged();
		}

	}

	@Override
	public int getColumnCount() {
		return columns.length;
	}

	@Override
	public int getRowCount() {
		return model.getSelectedEntry().annotation.noFeatures(type);
	}

	@Override
	public Class<?> getColumnClass(int col) {
		switch (col) {
		case 0:
			return String.class;
		case 1:
		case 2:
		case 3:
		case 4:
			return Icon.class;
		default:
			return String.class;

		}

	}

	public Feature getFeature(int row) {
		return model.getSelectedEntry().annotation.get(type, row);
	}

	@Override
	public Object getValueAt(int row, int col) {
		Feature f = getFeature(row);
		Sequence seq = model.getSelectedEntry().sequence;
		switch (col) {
		case 0:
			return f;
		case 1:
			if (SequenceTools.hasMissingStartCodon(seq, f, model.getAAMapping())) {
				return Icons.NO;
			} else {
				return Icons.YES;
			}
		case 2:
			if (SequenceTools.hasMissingStopCodon(seq, f, model.getAAMapping())) {
				return Icons.NO;
			} else {
				return Icons.YES;
			}
		case 3:
			if (SequenceTools.hasInternalStopCodon(seq, f, model.getAAMapping())) {
				return Icons.NO;
			} else {
				return Icons.YES;
			}
		case 4:
			if (SequenceTools.hasWrongSpliceSite(seq, f)) {
				return Icons.NO;
			} else {
				return Icons.YES;
			}
		default:
			return null;
		}
	}

	public int getRow(Feature first) {
		return model.getSelectedEntry().annotation.getByType(type).indexOf(first);

	}

	private Type type = Type.get("CDS");

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
		model.refresh(NotificationTypes.GENERAL);

	}

}