/**
 * %HEADER%
 */
package net.sf.genomeview.gui.menu.selection;

import java.awt.event.ActionEvent;
import java.util.Observable;

import net.sf.genomeview.BufferSeq;
import net.sf.genomeview.data.Blast;
import net.sf.genomeview.data.Model;
import net.sf.genomeview.gui.menu.AbstractModelAction;
import net.sf.jannot.Feature;
import net.sf.jannot.Location;
import net.sf.jannot.utils.SequenceTools;

/**
 * Opens a NCBI blast window with the sequence filled in in the boxes.
 * 
 * @author Thomas Abeel
 * 
 */
public class NCBIdnaBlastAction extends AbstractModelAction {

	
    private static final long serialVersionUID = -10339770203425228L;

    public NCBIdnaBlastAction(Model model) {
        super("Blast DNA at NCBI", model);
    }

    @Override
    public void update(Observable x, Object y) {
        setEnabled((model.selectionModel().getFeatureSelection() != null && model.selectionModel().getFeatureSelection().size() == 1) || 
        			model.getSelectedRegion() != null);
    }

    @Override
    public void actionPerformed(ActionEvent arg0) {
    	if (model.selectionModel().getLocationSelection() != null && model.selectionModel().getFeatureSelection().size() == 1){
    		Feature rf = model.selectionModel().getLocationSelection().iterator().next().getParent();
    		String seq = SequenceTools.extractSequence(model.getSelectedEntry().sequence(), rf);
    		Blast.nucleotideBlast(""+rf.toString().hashCode(),seq);
    	} else if (model.getSelectedRegion() != null){
    		 Location l = model.getSelectedRegion();
    	     String seq = //model.getSelectedEntry().sequence().getSubSequence(l.start(), l.end()+1);
    	    new BufferSeq(model.getSelectedEntry().sequence(),l).toString();
				
    	     Blast.nucleotideBlast("selection",seq);
    	}
    }

}
