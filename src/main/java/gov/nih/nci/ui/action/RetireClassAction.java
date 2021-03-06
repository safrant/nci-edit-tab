package gov.nih.nci.ui.action;

import java.awt.event.ActionEvent;

import org.protege.editor.owl.ui.action.FocusedComponentAction;

//import org.protege.editor.owl.ui.action.FocusedComponentAction;

public class RetireClassAction extends FocusedComponentAction<RetireClassTarget> {


	/**
	 * 
	 */
	private static final long serialVersionUID = 2497276618178521312L;


	protected Class<RetireClassTarget> initialiseAction() {
        return RetireClassTarget.class;
    }
	
	protected boolean canPerform() {
    	return (getCurrentTarget() != null && getCurrentTarget().canRetireClass());
    }
    
    public void actionPerformed(ActionEvent e) {
    	// if focus has been lost and popup not updated, target will be null
    	if (getCurrentTarget() != null) {
    		getCurrentTarget().retireClass();
    	}
    }
}
