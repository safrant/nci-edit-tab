package gov.nih.nci.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.event.ListSelectionListener;

import org.protege.editor.core.ui.list.MListButton;
import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.util.OWLAxiomInstance;
import org.protege.editor.owl.ui.UIHelper;
import org.protege.editor.owl.ui.axiom.AxiomAnnotationPanel;
import org.protege.editor.owl.ui.frame.OWLFrame;
import org.protege.editor.owl.ui.frame.OWLFrameSectionRow;
import org.protege.editor.owl.ui.framelist.OWLFrameList;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nih.nci.ui.dialog.PropertyEditingDialog;

public class NCIOWLFrameList<R> extends OWLFrameList {

	private static final long serialVersionUID = 1L;
	
	private final Logger logger = LoggerFactory.getLogger(NCIOWLFrameList.class);
	
	private boolean read_only = false;
	
	private OWLEditorKit editorKit;
	
	private List<OWLAnnotationProperty> annotationProps;
	
	private List<OWLAnnotation> annotations;
	
	OWLAnnotationAssertionAxiom axiom;
	
	private MListIconButton addButton;
    
    private MListIconButton editButton;
    
    private MListIconButton deleteButton;
    
    private ListSelectionListener selListener = event -> handleSelectionEvent(event);
    
	public NCIOWLFrameList(OWLEditorKit editorKit, OWLFrame<R> frame, boolean read_only) {
		this(editorKit, frame);
		this.read_only = read_only;
	}
	
	public NCIOWLFrameList(OWLEditorKit editorKit, OWLFrame<R> frame) {
		super(editorKit, frame);
		this.editorKit = editorKit;
		//this.frame = frame;
		//setupFrameListener();
		addListSelectionListener(selListener);
	}
	
	protected List<MListButton> getButtons(Object value) {
		List<MListButton> buttons = new ArrayList<MListButton>();
		if (read_only) {

    	} else {
    		buttons = new ArrayList<>();
    		if (value instanceof OWLFrameSectionRow) {
    			if (((OWLFrameSectionRow) value).isEditable()) {
    				
    				buttons = createButtons();
    				
    			}
    		}
    	}
		return buttons;
	}
	
	private List<MListButton> createButtons() {
		List<MListButton> buttons = new ArrayList<MListButton>();
    	addButton = new MListIconButton(NCIEditTabConstants.ADD, "ButtonAddIcon.png", NCIEditTabConstants.ADD, event -> handleAdd());
    	editButton = new MListIconButton(NCIEditTabConstants.EDIT, "ButtonEditIcon.png", NCIEditTabConstants.EDIT, event -> handleEdit());
    	deleteButton = new MListIconButton(NCIEditTabConstants.DELETE, "ButtonDeleteIcon.png", NCIEditTabConstants.DELETE, event -> handleDelete());
		
    	buttons.add(deleteButton);
    	buttons.add(editButton);
    	buttons.add(addButton);
    	return buttons;
    }
	
	public void handleAdd() {
		if(isComplexProperty()) {
			loadAnnotationsAndProperties();
			PropertyEditingDialog add = new	PropertyEditingDialog(NCIEditTabConstants.ADD, 
					PropertyUtil.getSelectedPropertyType(annotationProps), 
					PropertyUtil.getDefaultPropertyValues(annotationProps), 
					PropertyUtil.getSelectedPropertyOptions(annotationProps), 
					PropertyUtil.getDefaultSelectedPropertyLabel(annotationProps));
			
			HashMap<String, String> data = 	add.showDialog(this.editorKit, "Adding Properties");
			
			String codeStr = ((IRI)axiom.getSubject()).getShortForm();
			OWLClass cls = NCIEditTab.currentTab().getClass(codeStr);
			if (data != null) {
				if (NCIEditTab.currentTab().complexPropOp(NCIEditTabConstants.ADD, cls,
						axiom.getProperty(), axiom, data)) {
				}				
	
			} 
		} else {
			Object obj = getSelectedValue();
	        if (!(obj instanceof OWLFrameSectionRow)) {
	            return;
	        }
	        OWLFrameSectionRow<?,?,?> row = (OWLFrameSectionRow<?,?,?>) obj;
	        OWLAxiom ax = row.getAxiom();

	        AxiomAnnotationPanel axiomAnnotationPanel;
	            axiomAnnotationPanel = new AxiomAnnotationPanel(editorKit);

	        OWLOntology ontology = row.getOntology();
	        final OWLAxiomInstance axiomInstance;
	        if(ontology != null) {
	            axiomInstance = new OWLAxiomInstance(ax, ontology);
	        }
	        else {
	            OWLOntology activeOntology = editorKit.getOWLModelManager().getActiveOntology();
	            axiomInstance = new OWLAxiomInstance(ax, activeOntology);
	        }
	        axiomAnnotationPanel.setAxiomInstance(axiomInstance);
	        new UIHelper(editorKit).showDialog("Annotations for " + ax.getAxiomType().toString(), axiomAnnotationPanel, JOptionPane.CLOSED_OPTION);
	        axiomAnnotationPanel.dispose();
		}
		
	}
	
	public void handleEdit() {
		if(isComplexProperty()) {
			loadAnnotationsAndProperties();
			if(annotationProps == null) return;
			PropertyEditingDialog edit = new	PropertyEditingDialog(NCIEditTabConstants.EDIT, 
					PropertyUtil.getSelectedPropertyType(annotationProps), 
					PropertyUtil.getSelectedPropertyValues(annotations), 
					PropertyUtil.getSelectedPropertyOptions(annotationProps), 
					PropertyUtil.getDefaultSelectedPropertyLabel(annotationProps));
			
			HashMap<String, String> data = 	edit.showDialog(this.editorKit, "Editing Properties");
			
			String codeStr = ((IRI)axiom.getSubject()).getShortForm();
			OWLClass cls = NCIEditTab.currentTab().getClass(codeStr);
			if (data != null) {
				if (NCIEditTab.currentTab().complexPropOp(NCIEditTabConstants.EDIT, cls,
						axiom.getProperty(), axiom, data)) {
					if (axiom.getProperty().equals(NCIEditTab.currentTab().getPreferredName())) {
						NCIEditTab.currentTab().syncPrefName(data.get("Value"));
					}
				}				
	
			} 
		} else {
			super.handleEdit();
		}
	}
	
	private boolean isComplexProperty() {
		final Object val = getSelectedValue();
		if (val instanceof NCIOWLAnnotationsFrameSectionRow) {
			axiom = ((NCIOWLAnnotationsFrameSectionRow)val).getAxiom();
			OWLAnnotationProperty annoProp = axiom.getProperty();
			Set<OWLAnnotationProperty> complexProps = NCIEditTab.currentTab().getComplexProperties();
			if(complexProps.contains(annoProp)) {
				return true;
			}
		}
		return false;
	}
	
	private void loadAnnotationsAndProperties() {
		final Object val = getSelectedValue();
		if (val instanceof NCIOWLAnnotationsFrameSectionRow) {
			axiom = ((NCIOWLAnnotationsFrameSectionRow)val).getAxiom();
			annotations = new ArrayList<OWLAnnotation>(axiom.getAnnotations());
			annotationProps = new ArrayList<OWLAnnotationProperty>();
			for(OWLAnnotation annotation : annotations) {
				annotationProps.add(annotation.getProperty());
			}
		}
	}
	
	public void dispose() {
		removeListSelectionListener(selListener);
	}
}
