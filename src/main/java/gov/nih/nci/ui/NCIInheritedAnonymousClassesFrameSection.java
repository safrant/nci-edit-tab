package gov.nih.nci.ui;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.protege.editor.owl.OWLEditorKit;
import org.protege.editor.owl.model.inference.ReasonerPreferences.OptionalInferenceTask;
import org.protege.editor.owl.ui.editor.OWLObjectEditor;
import org.protege.editor.owl.ui.frame.AbstractOWLFrameSection;
import org.protege.editor.owl.ui.frame.OWLFrame;
import org.protege.editor.owl.ui.frame.OWLFrameSectionRow;
import org.protege.editor.owl.ui.frame.cls.InheritedAnonymousClassesFrameSectionRow;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class NCIInheritedAnonymousClassesFrameSection extends AbstractOWLFrameSection<OWLClass, OWLClassAxiom, OWLClassExpression> {
	private static final String LABEL = "SubClass Of (Anonymous Ancestor)";

    private Set<OWLClass> processedClasses = new HashSet<>();


    public NCIInheritedAnonymousClassesFrameSection(OWLEditorKit editorKit, OWLFrame<? extends OWLClass> frame) {
        super(editorKit, LABEL, "Anonymous Ancestor Class", frame);
    }


    protected OWLSubClassOfAxiom createAxiom(OWLClassExpression object) {
        return null; // canAdd() = false
    }


    public OWLObjectEditor<OWLClassExpression> getObjectEditor() {
        return null; // canAdd() = false
    }


    protected void refill(OWLOntology ontology) {
        Set<OWLClass> clses = getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider().getAncestors(getRootObject());
        clses.remove(getRootObject());
        for (OWLClass cls : clses) {
            for (OWLSubClassOfAxiom ax : ontology.getSubClassAxiomsForSubClass(cls)) {
                if (ax.getSuperClass().isAnonymous()) {
                    addRow(new NCIInheritedAnonymousClassesFrameSectionRow(getOWLEditorKit(), this, ontology, cls, ax, false));
                }
            }
            for (OWLEquivalentClassesAxiom ax : ontology.getEquivalentClassesAxioms(cls)) {
                    addRow(new NCIInheritedAnonymousClassesFrameSectionRow(getOWLEditorKit(), this, ontology, cls, ax, false));
            }
            processedClasses.add(cls);
        }
    }


    protected void refillInferred() {
        getOWLModelManager().getReasonerPreferences().executeTask(OptionalInferenceTask.SHOW_INFERRED_SUPER_CLASSES,
                () -> {
                    refillInferredDoIt();
                });
    }
    
    private void refillInferredDoIt() {
        OWLReasoner reasoner = getOWLModelManager().getReasoner();
        if (!reasoner.isConsistent()) {
            return;
        }
        if(!reasoner.isSatisfiable(getRootObject())) {
            return;
        }
        Set<OWLClass> clses = getReasoner().getSuperClasses(getRootObject(), true).getFlattened();
        clses.remove(getRootObject());
        for (OWLClass cls : clses) {
            if (!processedClasses.contains(cls)) {
                for (OWLOntology ontology : getOWLModelManager().getActiveOntology().getImportsClosure()) {
                    for (OWLSubClassOfAxiom ax : ontology.getSubClassAxiomsForSubClass(cls)) {
                        OWLClassExpression superClass = ax.getSuperClass();
                        if (superClass.isAnonymous()) {
                            OWLSubClassOfAxiom entailedAxiom = getOWLDataFactory().getOWLSubClassOfAxiom(getRootObject(), superClass);
                            addRow(new NCIInheritedAnonymousClassesFrameSectionRow(getOWLEditorKit(),
                                                                                this,
                                                                                null,
                                                                                cls,
                                                                                entailedAxiom,
                                                                                false));
                        }
                    }
                    for (OWLEquivalentClassesAxiom ax : ontology.getEquivalentClassesAxioms(cls)) {
                        Set<OWLClassExpression> descs = new HashSet<>(ax.getClassExpressions());
                        descs.remove(getRootObject());
                        for (OWLClassExpression superCls : descs) {
                        	if (superCls.isAnonymous()) {
                                OWLSubClassOfAxiom entailedAxiom = getOWLDataFactory().getOWLSubClassOfAxiom(getRootObject(), superCls);
                                addRow(new NCIInheritedAnonymousClassesFrameSectionRow(getOWLEditorKit(),
                        				this,
                        				null,
                        				cls, entailedAxiom,
                        				false));
                        	}
                        }
                    }
                }
            }
        }
    }


    public boolean canAdd() {
        return false;
    }
    
    @Override
    protected boolean isResettingChange(OWLOntologyChange change) {
    	return change.isAxiomChange() && (change.getAxiom() instanceof OWLSubClassOfAxiom || change.getAxiom() instanceof OWLEquivalentClassesAxiom);
    }


    protected void clear() {
        processedClasses.clear();
    }


    public Comparator<OWLFrameSectionRow<OWLClass, OWLClassAxiom, OWLClassExpression>> getRowComparator() {
        return null;
    }
}
